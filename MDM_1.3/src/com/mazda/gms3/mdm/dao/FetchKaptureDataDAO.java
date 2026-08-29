package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.kapture.KaptureApiResult;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.utils.KaptureContentXmlReader;
import java.text.SimpleDateFormat;
import com.mazda.gms3.mdm.vo.SIVinDetails;

/**
 * KAPTURE COUNTERPART OF FetchIMDataDAO.
 *
 * FetchIMDataDAO reads the Oracle InfoManager schema (OK_IM.CONTENTTEXT / TAG / TAGRESOURCE /
 * CONTENTTEXTCATEGORY) and is DELIBERATELY LEFT UNTOUCHED. This class is its replacement,
 * reading the Kapture CMS schema instead, so both can coexist while the SI VIN screens are
 * migrated market by market.
 *
 * Method signatures mirror FetchIMDataDAO so the call sites change as little as possible.
 *
 * SOURCE TABLES (all in kapture_cms_db, reached through DBConnectionHelper.getCMSConnection()):
 *   k_article           - one row PER VERSION of a document. latest_version='Y' picks the current
 *                         one. NOTE there is no RECORDID and no CONTENTID-per-document: contentid
 *                         identifies the CHANNEL, and 602 articles share only 19 contentids.
 *   k_article_category  - categories mapped onto a document, filtered by mastercategoryrefkey
 *                         ('VIN' for the SI VIN screens) instead of the old VIN object-id prefix.
 *   k_article_checkout  - checkout state.
 *   k_categories        - the category master, used to resolve display labels and to walk the
 *                         parent chain when the mapped row alone does not carry them.
 *
 * MAPPING FROM THE OLD OK_IM COLUMNS:
 *   DOCUMENTID              -> article_id
 *   LOCALEID                -> article_primary_locale
 *   PUBLISHED               -> article_state   ('Published' / 'Unpublished')
 *   INDEXMASTERIDENTIFIERS  -> article_title
 *   CREATEDATE              -> created_date
 *   LASTMODIFIEDDATE        -> date_modified
 *   PUBLISHDATE             -> published_date
 *   CHECKEDOUT              -> k_article_checkout.checkoutoption
 *   RECORDID / CONTENTID / DISPLAYENDDATE -> no longer needed
 */
public class FetchKaptureDataDAO extends DBConnectionHelper {

	static Logger logger = LogManager.getLogger(FetchKaptureDataDAO.class);

	/** mastercategoryrefkey used by the SI VIN screens - replaces the old VIN object-id prefix. */
	public static final String MASTER_KEY_VIN = "VIN";
	/** master keys used by the MNAO View Content popup. */
	public static final String MASTER_KEY_ESI = "ESI";
	public static final String MASTER_KEY_SERVICE_INFORMATION = "SERVICE_INFORMATION_TYPE";
	public static final String MASTER_KEY_SERVICE_MANUAL = "SERVICE_MANUAL_TYPE";
	public static final String MASTER_KEY_MODEL_YEAR = "MODEL_YEAR";

	private static final String T_ARTICLE = "kapture_cms_db.k_article";
	private static final String T_ARTICLE_CATEGORY = "kapture_cms_db.k_article_category";
	private static final String T_ARTICLE_CHECKOUT = "kapture_cms_db.k_article_checkout";
	private static final String T_CATEGORIES = "kapture_cms_db.k_categories";
	private static final String T_ESI_ARTICLE_CATEGORIES = "kapture_cms_db.k_esi_article_categories";
	private static final String T_CONTENT_TYPE = "kapture_cms_db.k_content_type";

	/**
	 * THE SERVICE INFORMATION CHANNEL, IDENTIFIED BY ITS DOCUMENT ID PREFIX.
	 *
	 * k_content_type keys the channel on a UUID that DIFFERS PER ENVIRONMENT, so it can never be
	 * hardcoded or put in a properties file - dev and VDI would need different values and the
	 * wrong one addresses another channel silently. The prefix is the part that is stable: it is
	 * what every SI document id starts with (SI1123, SI1125...), and it is what Kapture stamps new
	 * documents with. content_name is NOT used as the key - it is a display label ("Service
	 * Information") and free to be renamed.
	 */
	private static final String SI_DOCUMENT_ID_PREFIX = "SI";

	/**
	 * Resolved once per JVM - the channel row does not change while the application is running.
	 * volatile because the SI Data Load screen resolves it on the request thread and then USES it
	 * on the schedule thread it starts.
	 */
	private static volatile String siChannelContentId = null;

	/** Guard against a malformed parent chain looping forever. */
	private static final int MAX_HIERARCHY_DEPTH = 20;

	/*
	 * FIELDS ON latest-article's articlelistcategory. NOTE parentCategroryRefKey - Kapture
	 * misspells it identically in the API and in k_article_category, so the name is correct
	 * as written and must not be "fixed".
	 */
	private static final String API_CATEGORY_ARRAY = "articlelistcategory";
	private static final String API_REF_KEY = "categoryRefKey";
	private static final String API_CATEGORY_NAME = "categoryName";
	private static final String API_MASTER_KEY = "masterCategoryRefKey";
	private static final String API_PARENT_KEY = "parentCategroryRefKey";
	private static final String API_SELECTED = "selected";

	/** Set on SIVinDetails when the mapped categories could not be read at all. */
	public static final String ERROR_CATEGORY_READ_FAILED = "CATEGORY_READ_FAILED";

	/**
	 * THE ONE ROW FOR ONE LOCALE - MASTER OR TRANSLATION ALIKE. Bind the locale TWICE.
	 *
	 * InfoManager had a single CONTENTTEXT.LOCALEID, so a lookup was documentid + localeid and
	 * that was the whole story. Kapture splits it across two columns and the pair CHANGES SHAPE
	 * over a document's life:
	 *   primary = L, translated = L      - a master version
	 *   primary = L, translated = NULL   - a master version minted by update-article-tab, which
	 *                                      does NOT persist translatedLocale even though we send it
	 *   primary = M, translated = L      - an UNPUBLISHED TRANSLATION DRAFT of the master M
	 *
	 * So the locale a row ACTUALLY BELONGS TO is COALESCE(translated, primary), and that is what
	 * this predicate matches. Verified over every SI document: exactly one latest_version='Y' row
	 * per document per effective locale, no duplicates.
	 *
	 * WHY THE PLAIN article_primary_locale MATCH WAS A DEFECT, NOT A SIMPLIFICATION: a translation
	 * KEEPS THE MASTER'S primary locale, so SI1070 / en_US matched BOTH the en_US master (id 62152)
	 * and its ja_JP translation (id 64505), and ORDER BY id DESC handed back the translation. Since
	 * id is the row update-article-tab rewrites, an en_US request wrote the ja_JP translation.
	 * SI1070 is an MNAO document - this was never MME-only.
	 *
	 * THE "translated IS NULL" HALF IS NOT OPTIONAL. Every version update-article-tab creates
	 * leaves the column NULL (SI1092 rows 64489/64497/64501/64503), so matching the translated
	 * column alone would find nothing at all for an ordinary master document.
	 */
	private static final String LOCALE_PREDICATE =
			" AND (article_translated_locale = ?"
			+ " OR (article_translated_locale IS NULL AND article_primary_locale = ?))";

	/**
	 * KAPTURE REPLACEMENT FOR FetchIMDataDAO.getDocumentsData().
	 *
	 * Reads the latest version of a document for a locale and attaches its mapped VIN
	 * categories. The old signature took a vinParentRefKey used to resolve an InfoManager
	 * object id; here that same value IS the mastercategoryrefkey, so it is passed straight
	 * through to the category query.
	 *
	 * @param documentId       article_id, e.g. "SI1123"
	 * @param localeId         document locale, e.g. "en_US" (hyphens are normalised to underscore)
	 * @param masterCategoryKey mastercategoryrefkey to filter categories by - MASTER_KEY_VIN
	 * @param requestType      "MNAO" / "MC" / "MME" - decides the label fallback locale
	 * @param conn             an open CMS connection, or null to open one
	 * @param closeConnection  null means this method closes the connection it was given
	 */
	public static SIVinDetails getDocumentsData(String documentId, String localeId,
			String masterCategoryKey, String requestType, Connection conn, String closeConnection)
			throws SQLException {
		return getDocumentsData(documentId, localeId, masterCategoryKey, requestType, conn,
				closeConnection, true);
	}

	/**
	 * As above, but able to SKIP the mapped-category read.
	 *
	 * That read is an API call now (see getCategoriesList), so a caller that only wants the
	 * header - the checked-out flag, the state, the version - should not pay for it.
	 * KaptureContentServiceImpl is exactly that caller: it needs the version row and nothing
	 * else, and it was passing MASTER_KEY_VIN on MC/MME documents whose key is CARLINE, which
	 * also explains the misleading "NO CATEGORIES FOUND" line it used to log on every write.
	 *
	 * @param loadCategories false to leave the category list empty and make no API call
	 */
	public static SIVinDetails getDocumentsData(String documentId, String localeId,
			String masterCategoryKey, String requestType, Connection conn, String closeConnection,
			boolean loadCategories) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		SIVinDetails details = null;
		try {
			if (null != documentId && !"".equals(documentId) && null != localeId
					&& !"".equals(localeId) && null != masterCategoryKey
					&& !"".equals(masterCategoryKey)) {
				if (null == conn || conn.isClosed() == true) {
					conn = getCMSConnection();
				}

				localeId = localeId.replace("-", "_");

				/*
				 * ONE ROW PER VERSION LIVES IN k_article - latest_version='Y' is what makes this
				 * single-row. Ordering by id descending as well, so a data glitch that leaves two
				 * rows flagged 'Y' still yields the newest rather than an arbitrary one.
				 */
				String sql = "SELECT article_id, contentid, article_primary_locale, article_state,"
						+ " article_version, published_version, article_title, created_date,"
						+ " date_modified, published_date,"
						+ " COALESCE(article_translated_locale, article_primary_locale)"
						+ " AS effective_locale"
						+ " FROM " + T_ARTICLE
						+ " WHERE article_id = ?" + LOCALE_PREDICATE
						+ " AND latest_version = 'Y' ORDER BY id DESC";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, documentId.trim());
				pstmt.setString(2, localeId.trim());
				pstmt.setString(3, localeId.trim());
				rs = pstmt.executeQuery();
				String contentId = null;
				if (rs.next()) {
					details = new SIVinDetails();
					details.setDocumentId(rs.getString("article_id"));
					// NEEDED BY THE CATEGORY READ - latest-article is keyed article + content + locale.
					contentId = safeTrim(rs.getString("contentid"));
					/*
					 * THE EFFECTIVE LOCALE, NOT THE PRIMARY ONE. This value is what the follow-up
					 * reads bind: k_article_category and k_article_view key a translation's rows
					 * on the locale it IS (SI1070's Japanese categories sit under ja_JP), while
					 * that row's article_primary_locale still says en_US. Storing the primary here
					 * would find the right row and then list the WRONG locale's categories.
					 */
					details.setLocale(safeTrim(rs.getString("effective_locale")));

					/*
					 * OLD: PUBLISHED = 'Y' / 'N'. NEW: article_state = 'Published' / 'Unpublished'.
					 */
					String articleState = rs.getString("article_state");
					details.setDocumentPublished(null != articleState
							&& "Published".equalsIgnoreCase(articleState.trim()));

					/*
					 * THE VERSION THE SCREEN WILL LATER SEND BACK TO THE KAPTURE API. A published
					 * document is addressed by its published_version, an unpublished one by its
					 * working article_version.
					 */
					String version = details.isDocumentPublished()
							? rs.getString("published_version") : rs.getString("article_version");
					if (null == version || "".equals(version.trim())
							|| "NaN".equalsIgnoreCase(version.trim())) {
						// published_version is sometimes literally "NaN" or null in the data
						version = rs.getString("article_version");
					}
					details.setFetchedVersion(safeTrim(version));
				}
				rs.close(); rs = null;
				pstmt.close(); pstmt = null;

				if (null != details && null != details.getDocumentId()
						&& !"".equals(details.getDocumentId())) {
					// CHECKED OUT?
					details.setCheckedOut(isDocumentCheckedOut(details.getDocumentId(), conn));
					// MAPPED CATEGORIES
					if (loadCategories) {
						details = getCategoriesList(details, conn, contentId, masterCategoryKey,
								requestType);
					} else {
						details.setCategoryList(new ArrayList<IMCategoryDetails>());
					}
				} else {
					logger.info("getDocumentsData :: No Data Found for Document Id {" + documentId
							+ "} for Locale :: > " + localeId);
				}

				if (null == closeConnection) {
					if (null != conn) {
						conn.close();
					}
				}
			} else {
				logger.info("getDocumentsData :: Document id, Locale & Master Category Key as"
						+ " Parameter are null.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getDocumentsData()", e);
			details = null;
		} finally {
			if (null != rs)
				rs.close();
			if (null != pstmt)
				pstmt.close();
			documentId = null;
			localeId = null;
		}
		return details;
	}

	/**
	 * KAPTURE REPLACEMENT FOR FetchIMDataDAO.getCategoriesList().
	 *
	 * READS THE API, NOT k_article_category, AND THIS IS DELIBERATE.
	 *
	 * save-translate-article APPENDS to k_article_category without deleting the locale's
	 * previous rows, so a translated document collects one duplicate set per edit and a
	 * category that has been REMOVED still has its old row sitting in the table. SI1125/en_EU
	 * ended up with six copies of one VIN range that had already been removed, while
	 * update-article-tab (the master path) replaces its rows correctly - across the whole
	 * table the only duplicated groups were on translation rows.
	 *
	 * latest-article reports the real mapping: de-duplicated, and with the removed range gone.
	 * Reading it is both simpler and more reliable than trying to reconstruct the truth from
	 * the accumulated rows, so it is the source of record here regardless of document type.
	 *
	 * The old version matched categories whose InfoManager OBJECTID started with the VIN
	 * hierarchy's object id. Kapture stores the master key on the row itself, so the filter is
	 * simply mastercategoryrefkey = 'VIN'.
	 *
	 * The mapped row usually carries its own categoryname; when it does not, the label is
	 * resolved from k_categories using the market's fallback locale.
	 */
	private static SIVinDetails getCategoriesList(SIVinDetails details, Connection conn,
			String contentId, String masterCategoryKey, String requestType) {
		try {
			details.setCategoryList(new ArrayList<IMCategoryDetails>());

			if (null == contentId || "".equals(contentId)) {
				logger.info("getCategoriesList :: NO CONTENT ID FOR {" + details.getDocumentId()
						+ "} OF {" + details.getLocale() + "} - cannot read its categories.");
				details.setErrorCode(ERROR_CATEGORY_READ_FAILED);
				details.setErrorMessage("The mapped categories for {" + details.getDocumentId()
						+ "} could not be read - the document has no content id.");
				return details;
			}

			KaptureApiClient client = new KaptureApiClient();
			KaptureApiResult read = client.latestArticle(details.getDocumentId(), contentId,
					details.getLocale(), "category list");
			if (!read.isOk() || null == read.body || "".equals(read.body.trim())) {
				/*
				 * NO SILENT EMPTY LIST. An empty list is indistinguishable from "this document
				 * has no VIN ranges mapped", and the screens would then offer to ADD a range that
				 * is already there. Say so instead.
				 */
				logger.info("getCategoriesList :: READ FAILED FOR {" + details.getDocumentId()
						+ "} OF {" + details.getLocale() + "} - HTTP " + read.httpStatus + " "
						+ read.message);
				details.setErrorCode(ERROR_CATEGORY_READ_FAILED);
				details.setErrorMessage("The mapped categories for {" + details.getDocumentId()
						+ "} could not be read from Kapture. Please try again.");
				return details;
			}

			JsonObject article = new JsonParser().parse(read.body).getAsJsonObject();
			JsonArray tree = null;
			if (article.has(API_CATEGORY_ARRAY) && article.get(API_CATEGORY_ARRAY).isJsonArray()) {
				tree = article.getAsJsonArray(API_CATEGORY_ARRAY);
			}

			/*
			 * THE WHOLE TREE COMES BACK, NOT JUST THE MAPPING. Every node on the path to a mapped
			 * category is present with selected=false; only the mapped ones are selected=true.
			 * Taking selected=true under the wanted master key gives the mapped leaves, and the
			 * screens trace each one's hierarchy through getCategoryHierarchy as before.
			 */
			LinkedHashMap<String, IMCategoryDetails> selected =
					new LinkedHashMap<String, IMCategoryDetails>();
			for (int i = 0; null != tree && i < tree.size(); i++) {
				JsonElement element = tree.get(i);
				if (null == element || !element.isJsonObject()) {
					continue;
				}
				JsonObject node = element.getAsJsonObject();
				if (!jsonFlag(node, API_SELECTED)) {
					continue;
				}
				if (!masterCategoryKey.trim().equalsIgnoreCase(jsonText(node, API_MASTER_KEY))) {
					continue;
				}
				String refKey = jsonText(node, API_REF_KEY);
				if ("".equals(refKey) || refKey.equalsIgnoreCase(masterCategoryKey.trim())) {
					/*
					 * SKIP THE MASTER ROW ITSELF. The mapping can include the root ("VIN")
					 * alongside the actual leaf; the screens only care about the mapped ranges.
					 */
					continue;
				}
				IMCategoryDetails catDetails = new IMCategoryDetails();
				catDetails.setCategoryRefKey(refKey);
				catDetails.setCategoryName(jsonText(node, API_CATEGORY_NAME));
				catDetails.setParentRefKey(jsonText(node, API_PARENT_KEY));
				catDetails.setLocale(details.getLocale());
				// KEYED BY REFERENCE KEY so a repeated node can never become a repeated range.
				selected.put(refKey, catDetails);
				catDetails = null;
			}
			details.getCategoryList().addAll(selected.values());

			if (null != details.getCategoryList() && details.getCategoryList().size() > 0) {
				String labelLocale = resolveLabelLocale(details.getLocale(), requestType);
				for (IMCategoryDetails catDetails : details.getCategoryList()) {
					if ("".equals(safeTrim(catDetails.getCategoryName()))) {
						catDetails.setCategoryName(getCategoryLabel(catDetails.getCategoryRefKey(),
								details.getLocale(), labelLocale, conn));
					}
				}
				labelLocale = null;
			} else {
				logger.info("getCategoriesList :: NO CATEGORIES FOUND FOR {"
						+ details.getDocumentId() + "} OF {" + details.getLocale() + "}.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getCategoriesList()", e);
			details.setErrorCode(ERROR_CATEGORY_READ_FAILED);
			details.setErrorMessage("The mapped categories for {" + details.getDocumentId()
					+ "} could not be read from Kapture. Please try again.");
		}
		return details;
	}

	/** Reference-key / name / parent / master-key / flag fields on articlelistcategory. */
	private static String jsonText(JsonObject node, String field) {
		if (null == node || !node.has(field) || node.get(field).isJsonNull()) {
			return "";
		}
		return safeTrim(node.get(field).getAsString());
	}

	private static boolean jsonFlag(JsonObject node, String field) {
		if (null == node || !node.has(field) || node.get(field).isJsonNull()) {
			return false;
		}
		try {
			return node.get(field).getAsBoolean();
		} catch (Exception e) {
			return "true".equalsIgnoreCase(safeTrim(node.get(field).getAsString()));
		}
	}

	/**
	 * Walks a category's parent chain UPWARDS in k_categories and returns it root-first, so the
	 * screen can show every level's label.
	 *
	 * Depth is NOT fixed: MNAO hangs VIN ranges off the VIN root (4 levels) while MC and MME hang
	 * them off CARLINE (6 levels), so this follows parentrefkey until it runs out rather than
	 * assuming a shape.
	 *
	 * @return root-first list; empty when the reference key is not in the master
	 */
	public static ArrayList<IMCategoryDetails> getCategoryHierarchy(String categoryRefKey,
			String locale, String labelLocale, Connection conn) {
		ArrayList<IMCategoryDetails> chain = new ArrayList<IMCategoryDetails>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean closeHere = false;
		try {
			if (null == categoryRefKey || "".equals(categoryRefKey.trim())) {
				return chain;
			}
			if (null == conn || conn.isClosed() == true) {
				conn = getCMSConnection();
				closeHere = true;
			}

			/*
			 * A reference key exists once PER LOCALE. Prefer the document's own locale and fall
			 * back to the market's label locale, because a row can exist for the key without a
			 * name in the wanted locale.
			 */
			String sql = "SELECT referencekey, categoryname, parentrefkey, locale FROM " + T_CATEGORIES
					+ " WHERE referencekey = ? AND locale IN (?, ?)"
					+ " ORDER BY CASE WHEN locale = ? THEN 0 ELSE 1 END LIMIT 1";

			String currentKey = categoryRefKey.trim();
			int depth = 0;
			while (null != currentKey && !"".equals(currentKey) && depth < MAX_HIERARCHY_DEPTH) {
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, currentKey);
				pstmt.setString(2, locale);
				pstmt.setString(3, labelLocale);
				pstmt.setString(4, locale);
				rs = pstmt.executeQuery();
				String parentKey = null;
				if (rs.next()) {
					IMCategoryDetails level = new IMCategoryDetails();
					level.setCategoryRefKey(safeTrim(rs.getString("referencekey")));
					level.setCategoryName(safeTrim(rs.getString("categoryname")));
					level.setParentRefKey(safeTrim(rs.getString("parentrefkey")));
					level.setLocale(safeTrim(rs.getString("locale")));
					// build root-first
					chain.add(0, level);
					parentKey = level.getParentRefKey();
					level = null;
				} else {
					logger.info("getCategoryHierarchy :: {" + currentKey + "} not present in "
							+ T_CATEGORIES + " for {" + locale + "} or {" + labelLocale + "}.");
				}
				rs.close(); rs = null;
				pstmt.close(); pstmt = null;

				currentKey = ("".equals(safeTrim(parentKey))) ? null : parentKey.trim();
				depth++;
			}
			if (depth >= MAX_HIERARCHY_DEPTH) {
				logger.info("getCategoryHierarchy :: depth guard hit for {" + categoryRefKey
						+ "} - parent chain may be circular.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getCategoryHierarchy()", e);
		} finally {
			closeQuietly(rs, pstmt);
			if (closeHere && null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
		return chain;
	}

	/**
	 * Display label for a single category reference key, preferring the document's own locale and
	 * falling back to the market's locale when the key carries no name there.
	 */
	public static String getCategoryLabel(String categoryRefKey, String locale, String labelLocale,
			Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String name = "";
		try {
			if (null == categoryRefKey || "".equals(categoryRefKey.trim())) {
				return name;
			}
			String sql = "SELECT categoryname FROM " + T_CATEGORIES
					+ " WHERE referencekey = ? AND locale IN (?, ?)"
					+ " AND categoryname IS NOT NULL AND categoryname <> ''"
					+ " ORDER BY CASE WHEN locale = ? THEN 0 ELSE 1 END LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, categoryRefKey.trim());
			pstmt.setString(2, locale);
			pstmt.setString(3, labelLocale);
			pstmt.setString(4, locale);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				name = safeTrim(rs.getString("categoryname"));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getCategoryLabel()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return name;
	}

	/**
	 * The contentid a document belongs to.
	 *
	 * NOTE THIS IS A CHANNEL IDENTIFIER, NOT A DOCUMENT ONE - 602 articles share 19 contentids,
	 * one per channel (all SI documents carry the same). The Kapture REST calls still require it
	 * as a query parameter, which is the only reason it is read at all.
	 */
	public static String getContentId(String documentId, String locale, Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String contentId = "";
		try {
			String sql = "SELECT contentid FROM " + T_ARTICLE
					+ " WHERE article_id = ?" + LOCALE_PREDICATE
					+ " ORDER BY id DESC LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			pstmt.setString(2, locale.replace("-", "_").trim());
			pstmt.setString(3, locale.replace("-", "_").trim());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				contentId = safeTrim(rs.getString("contentid"));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(), "getContentId()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return contentId;
	}

	/**
	 * THE k_article ROW FOR THE VERSION BEING UPDATED - id, version, published version and state.
	 *
	 * Every one of these has to come from the table rather than from display-Article; see
	 * KaptureArticleIdentity for why each.
	 *
	 * MATCHES ON THE EFFECTIVE LOCALE - see LOCALE_PREDICATE, which is the same predicate
	 * getDocumentsData and getContentId use, so the id always belongs to the row they reported on.
	 *
	 * TRANSLATIONS ARE COVERED BY THAT PREDICATE AND NEED NO SEPARATE PATH. A translation carries
	 * the wanted locale in article_translated_locale with a DIFFERENT article_primary_locale, and
	 * COALESCE resolves it. There is no primary-then-translated fallback and there should not be
	 * one: a fallback would still return the master when BOTH exist, which is the case that used
	 * to be wrong.
	 *
	 * WHAT COMES BACK FOR A TRANSLATION IS THE TRANSLATION'S OWN id / version / state, which is
	 * exactly right - the publish decision is per locale, so a published master and its unpublished
	 * translation draft are handled independently.
	 */
	/**
	 * The k_article identity when the caller has no CMS connection of its own.
	 *
	 * The SI Data Load schedule threads run outside any request and hold no connection, so they
	 * take one for the length of this read and give it straight back.
	 */
	public static KaptureArticleIdentity getArticleIdentity(String documentId, String locale) {
		Connection conn = null;
		try {
			conn = getCMSConnection();
			if (null == conn) {
				logger.info("getArticleIdentity :: no CMS connection - cannot identify {" + documentId
						+ "} / {" + locale + "}.");
				return new KaptureArticleIdentity();
			}
			return getArticleIdentity(documentId, locale, conn);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getArticleIdentity()", e);
			return new KaptureArticleIdentity();
		} finally {
			if (null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
	}

	public static KaptureArticleIdentity getArticleIdentity(String documentId, String locale,
			Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		KaptureArticleIdentity identity = new KaptureArticleIdentity();
		try {
			String sql = "SELECT id, article_version, published_version, article_state,"
					+ " COALESCE(master_locale, article_primary_locale) AS master_locale,"
					+ " COALESCE(article_translated_locale, article_primary_locale)"
					+ " AS effective_locale FROM "
					+ T_ARTICLE
					+ " WHERE article_id = ?" + LOCALE_PREDICATE
					+ " AND latest_version = 'Y' ORDER BY id DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			pstmt.setString(2, locale.replace("-", "_").trim());
			pstmt.setString(3, locale.replace("-", "_").trim());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				identity.setId(safeTrim(rs.getString("id")));
				identity.setArticleVersion(safeTrim(rs.getString("article_version")));
				identity.setPublishedVersion(safeTrim(rs.getString("published_version")));
				identity.setArticleState(safeTrim(rs.getString("article_state")));
				identity.setMasterLocale(safeTrim(rs.getString("master_locale")));
				identity.setEffectiveLocale(safeTrim(rs.getString("effective_locale")));
			}
			if (!identity.isUsable()) {
				logger.info("getArticleIdentity :: no latest k_article row for {" + documentId + "} / {"
						+ locale + "} - update-article-tab would reject the payload.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getArticleIdentity()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return identity;
	}

	/**
	 * KAPTURE REPLACEMENT FOR THE OLD CONTENTTEXT.CHECKEDOUT COLUMN.
	 *
	 * checkoutoption is bit(1) NULLABLE, so it is read with getInt/wasNull rather than as a String:
	 * different Connector/J versions surface bit(1) as Boolean, Integer or a raw byte, and
	 * comparing its toString() is not portable.
	 *
	 * POLARITY - checkoutoption = 1 MEANS THE DOCUMENT IS CHECKED OUT.
	 * The migration note said b'0' meant checked out. The data says the opposite, and the two
	 * readings are mutually exclusive, so this follows the data:
	 *   - checkoutoption and check_inoption are a perfect XOR over every row in the table -
	 *     exactly one of the pair is 1, never both and never neither.
	 *   - the checkoutoption=1 rows are working drafts (SI1075 Unpublished 0.7, SI1085
	 *     Unpublished 0.1), i.e. documents somebody currently has open.
	 *   - the checkoutoption=0 / check_inoption=1 rows are finished documents (SI1079
	 *     Published 3.0), i.e. checked back IN.
	 * Reading b'0' as "checked out" would make it synonymous with check_inoption=1, which is the
	 * opposite state. If this ever has to be flipped, it is the single comparison below.
	 *
	 * NO ROW AT ALL, OR A NULL, MEANS NOT CHECKED OUT - a document nobody has opened has no row.
	 */
	/**
	 * DOES THIS DOCUMENT HAVE A LIVE ROW FOR THIS LOCALE?
	 *
	 * Used by the MNAO French Canada feature before it writes to fr_CA, and by the MNAO batch for
	 * the same reason. A document that was never translated has nothing to update, and that is not
	 * an error - the en_CA write still stands.
	 *
	 * MATCHES ON THE EFFECTIVE LOCALE, the same rule as everywhere else in this class, so a
	 * translation is found under the locale it IS rather than its master's. **latest_version='Y'
	 * IS THE POINT OF THE CHECK**: SI1125's de_DE and cs_CZ rows still exist and latest-article
	 * will still serve them, but their lineage ended and nothing should be written to them.
	 */
	public static boolean hasTranslationForLocale(String documentId, String locale) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;
		boolean found = false;
		try {
			if (null == documentId || "".equals(documentId.trim()) || null == locale
					|| "".equals(locale.trim())) {
				return false;
			}
			conn = getCMSConnection();
			String sql = "SELECT 1 FROM " + T_ARTICLE
					+ " WHERE article_id = ?" + LOCALE_PREDICATE
					+ " AND latest_version = 'Y' LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			pstmt.setString(2, locale.trim());
			pstmt.setString(3, locale.trim());
			rs = pstmt.executeQuery();
			found = rs.next();
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"hasTranslationForLocale()", e);
			found = false;
		} finally {
			closeQuietly(rs, pstmt);
			if (null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
		return found;
	}

	/**
	 * Checkout state when the caller has no CMS connection of its own - opens and closes one.
	 *
	 * WHY THIS STILL MATTERS UNDER KAPTURE: checkout is not a leftover from InfoManager. The
	 * SERVICE_INFORMATION channel has check_out_check_in_option = 1 in k_content_type and
	 * k_article_checkout carries real checked-out SI documents, so writing to one is as wrong here
	 * as it was there.
	 */
	public static boolean isDocumentCheckedOut(String documentId) {
		Connection conn = null;
		try {
			conn = getCMSConnection();
			if (null == conn) {
				logger.info("isDocumentCheckedOut :: no CMS connection - cannot check {" + documentId
						+ "}.");
				return false;
			}
			return isDocumentCheckedOut(documentId, conn);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"isDocumentCheckedOut()", e);
			return false;
		} finally {
			if (null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
	}

	public static boolean isDocumentCheckedOut(String documentId, Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean checkedOut = false;
		try {
			String sql = "SELECT checkoutoption FROM " + T_ARTICLE_CHECKOUT
					+ " WHERE article_id = ? ORDER BY id DESC LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				int value = rs.getInt("checkoutoption");
				if (!rs.wasNull()) {
					checkedOut = (value == 1);
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"isDocumentCheckedOut()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return checkedOut;
	}

	/**
	 * Categories mapped onto a document for an arbitrary master key - used by the MNAO View
	 * Content popup for SERVICE_INFORMATION_TYPE, SERVICE_MANUAL_TYPE and MODEL_YEAR.
	 *
	 * ESI categories live in their OWN table and are served by getESICategories() instead.
	 *
	 * @return reference key -> display name, in the order the rows come back
	 */
	public static LinkedHashMap<String, String> getMappedCategories(String documentId,
			String locale, String masterCategoryKey, Connection conn) {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT categoryrefkey, categoryname FROM " + T_ARTICLE_CATEGORY
					+ " WHERE article_id = ? AND locale = ? AND mastercategoryrefkey = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			pstmt.setString(2, locale.trim());
			pstmt.setString(3, masterCategoryKey.trim());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String refKey = safeTrim(rs.getString("categoryrefkey"));
				if (!"".equals(refKey)) {
					map.put(refKey, safeTrim(rs.getString("categoryname")));
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getMappedCategories()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return map;
	}

	/**
	 * ESI categories mapped onto a document. These are NOT in k_article_category - Kapture keeps
	 * them in their own table, keyed by article and locale.
	 */
	public static LinkedHashMap<String, String> getESICategories(String documentId, String locale,
			Connection conn) {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT referencekey, categoryname FROM " + T_ESI_ARTICLE_CATEGORIES
					+ " WHERE article_id = ? AND locale = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			pstmt.setString(2, locale.trim());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String refKey = safeTrim(rs.getString("referencekey"));
				if (!"".equals(refKey)) {
					map.put(refKey, safeTrim(rs.getString("categoryname")));
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getESICategories()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return map;
	}

	/**
	 * KAPTURE REPLACEMENT FOR FetchIMDataDAO.prepareViewContentData() - the MNAO View Content
	 * popup. Applicable to MNAO locales only, as before.
	 *
	 * EVERY FIELD THE OLD VERSION POPULATED IS POPULATED HERE. The differences are only in where
	 * the values come from:
	 *
	 *   OK_IM.CONTENTTEXT                     -> kapture_cms_db.k_article
	 *   OK_IM.CONTENTTEXTCATEGORY + TAG       -> kapture_cms_db.k_article_category, filtered by
	 *                                            mastercategoryrefkey instead of an object-id prefix
	 *   ESI categories                        -> kapture_cms_db.k_esi_article_categories
	 *   OK_IM.CONTENTVALUE (XPath lookups)    -> the generated document XML, same XPaths
	 *                                            (KaptureContentXmlReader)
	 *   object-id token depth                 -> depth in the parentrefkey chain
	 *
	 * THREE THINGS TO KNOW:
	 *
	 * 1. THE EXPIRY RULE IS ACTIVE, BUT COMPARED WITH NOW RATHER THAN WITH THE PUBLISH DATE.
	 *    article_expire_date is the Kapture equivalent of DISPLAYENDDATE and is read into the
	 *    same field. A published document is expired only when it HAS an end date and that date
	 *    has passed; no end date means it never expires. The original condition is kept inline
	 *    as a comment with a marker explaining the swap - see below.
	 *
	 * 2. ONLY "UNPUBLISH_CONTENT" DROPS A DOCUMENT. The InfoManager version also matched
	 *    "UBPUBLISH_CONTENT" (a typo) and "EXPIRE_CONTENT"; neither is used as a fallback.
	 *
	 * 3. ESI CATEGORIES START AT LEVEL 1. A document carries level 1, optionally level 2 and
	 *    level 3 - never the ESI root. A document mapped only to the root key itself counts as
	 *    having NO ESI category.
	 */
	public static MNAOViewContentDetails prepareViewContentData(String documentId, String localeId)
			throws SQLException {
		MNAOViewContentDetails docDetails = new MNAOViewContentDetails();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null == documentId || "".equals(documentId) || null == localeId
					|| "".equals(localeId)) {
				return docDetails;
			}
			conn = getCMSConnection();
			String locale = localeId.replace("-", "_").trim();

			/*
			 * DOCUMENT HEADER
			 */
			String version = null;
			boolean published = false;
			/*
			 * THE EFFECTIVE LOCALE, like every other read in this class - see LOCALE_PREDICATE.
			 *
			 * This was the last query still matching on article_primary_locale alone. A TRANSLATION
			 * row carries its MASTER's primary locale (fr_CA translations of an en_CA document hold
			 * article_primary_locale = en_CA), so asking for fr_CA matched nothing, this method
			 * returned an empty object, and the caller's null check skipped
			 * performViewContentOperation WITHOUT LOGGING ANYTHING. The View Content refresh looked
			 * like it was never invoked when in fact it could not find the document.
			 */
			String sql = "SELECT article_id, article_primary_locale, article_state, article_version,"
					+ " published_version, article_title, created_date, date_modified, published_date,"
					+ " article_expire_date,"
					+ " COALESCE(article_translated_locale, article_primary_locale) AS effective_locale"
					+ " FROM " + T_ARTICLE
					+ " WHERE article_id = ?" + LOCALE_PREDICATE
					+ " AND latest_version = 'Y' ORDER BY id DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId.trim());
			// bound TWICE - the predicate tests the translated column and then the primary one
			pstmt.setString(2, locale);
			pstmt.setString(3, locale);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				docDetails.setDocumentId(safeTrim(rs.getString("article_id")));
				/*
				 * THE LOCALE THIS ROW IS, not its master's. gms3_vc_* rows are keyed by locale, so
				 * storing the primary here would have written the translation's View Content under
				 * en_CA and overwritten the master's.
				 */
				docDetails.setLocale(safeTrim(rs.getString("effective_locale")));

				String articleState = rs.getString("article_state");
				published = (null != articleState && "Published".equalsIgnoreCase(articleState.trim()));
				// OLD: PUBLISHED column was 'Y' / 'N' - the rest of the screen still reads it that way
				docDetails.setDocumentStatus(published ? "Y" : "N");

				docDetails.setTitle(rs.getString("article_title"));
				docDetails.setPublishDate(rs.getTimestamp("published_date"));
				/*
				 * article_expire_date IS THE KAPTURE EQUIVALENT OF THE OLD DISPLAYENDDATE and is
				 * read into the same field. Two formats occur in this column - MM/dd/yyyy HH:mm:ss
				 * from the API and yyyy-MM-dd HH:mm:ss from the Authoring Tool - and toTimestamp()
				 * accepts both. An unparseable value yields null, i.e. NEVER EXPIRES, which is the
				 * safe direction: a document stays visible rather than silently disappearing.
				 */
				docDetails.setDisplayEndDate(toTimestamp(rs.getString("article_expire_date")));
				docDetails.setDocumentCreateDate(toTimestamp(rs.getString("created_date")));
				docDetails.setDocumentLastModifiedDate(toTimestamp(rs.getString("date_modified")));

				/*
				 * EXPIRY CHECK.
				 *
				 * AN UNPUBLISHED VERSION IS ALREADY HANDLED and needs nothing here: documentStatus
				 * was set to "N" above from article_state, and SIVinDAO inserts only when it is
				 * "Y", so an unpublished document has its View Content rows deleted and none
				 * written back.
				 *
				 * A PUBLISHED VERSION IS EXPIRED only when it HAS an end date and that date has
				 * passed. No end date means it never expires.
				 *
				 * WHY THIS RULE EXISTS AT ALL: it is INHERITED FROM INFOMANAGER, where a document
				 * past its display end date was dropped from View Content. It is carried forward
				 * because the legacy behaviour is the specification for this screen.
				 *
				 * THE AUTHORING TOOL DOES NOT APPLY IT, AND THAT DIVERGENCE IS DELIBERATE.
				 * SI1069 carries an end date of 04 Jun 2026 and KAuthor still lists it as a live
				 * version, so whatever KAuthor does with article_expire_date, it is not this. MDM
				 * View Content keeps the InfoManager semantics regardless - a document past its
				 * end date is not shown here. Do NOT "fix" this to match KAuthor without a
				 * business decision to drop the legacy rule.
				 *
				 * *** CONDITION SWAPPED: PUBLISH DATE -> CURRENT TIME ***
				 * Based on how kapture_cms_db and the Authoring Tool actually behave, the end date
				 * is compared with NOW instead of with the publish date. The InfoManager rule
				 * compared the two dates and treated a missing value on EITHER side as expired,
				 * which does not survive the move:
				 *   - published_date is NULL on most published rows the Authoring Tool creates
				 *   - article_expire_date is empty on the majority of published documents
				 * so the original condition would expire virtually every document. Nothing in the
				 * data carries an end date EARLIER than its publish date either, so the old
				 * comparison would never fire on a document that still has both.
				 *
				 * ORIGINAL CONDITION, KEPT VERBATIM FOR REFERENCE:
				 *
				 * if(published == true)
				 * {
				 *     if(null == docDetails.getPublishDate() || null == docDetails.getDisplayEndDate()
				 *             || docDetails.getDisplayEndDate().getTime() <= docDetails.getPublishDate().getTime())
				 *     {
				 *         // DOCUMENT EXPIRED - DELETE ITS DATA FROM THE VIEW CONTENT TABLES
				 *         docDetails.setDocumentStatus("N");
				 *         return docDetails;
				 *     }
				 * }
				 */
				if (published && null != docDetails.getDisplayEndDate()
						&& docDetails.getDisplayEndDate().getTime() <= System.currentTimeMillis()) {
					// DOCUMENT EXPIRED - DELETE ITS DATA FROM THE VIEW CONTENT TABLES
					logger.info("prepareViewContentData :: {" + docDetails.getDocumentId() + "} / {"
							+ docDetails.getLocale() + "} EXPIRED on {" + docDetails.getDisplayEndDate()
							+ "} - its View Content data is removed.");
					docDetails.setDocumentStatus("N");
					return docDetails;
				}

				version = published ? rs.getString("published_version") : rs.getString("article_version");
				if (null == version || "".equals(version.trim()) || "NaN".equalsIgnoreCase(version.trim())) {
					version = rs.getString("article_version");
				}
			} else {
				// NO DOCUMENT - DELETE ITS DATA FROM THE VIEW CONTENT TABLES
				docDetails.setDocumentStatus("N");
				return docDetails;
			}
			rs.close(); rs = null;
			pstmt.close(); pstmt = null;

			/*
			 * ALL MAPPED CATEGORIES, WHATEVER THE MASTER KEY. Read once and grouped, rather than
			 * one query per master key, because the old version also worked off a single list.
			 */
			ArrayList<IMCategoryDetails> categoriesList = new ArrayList<IMCategoryDetails>();
			sql = "SELECT categoryrefkey, categoryname, mastercategoryrefkey,"
					+ " COALESCE(parentcategroryrefkey, parentcategoryrefkey) AS parentrefkey"
					+ " FROM " + T_ARTICLE_CATEGORY + " WHERE article_id = ? AND locale = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, docDetails.getDocumentId());
			pstmt.setString(2, docDetails.getLocale());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				IMCategoryDetails cat = new IMCategoryDetails();
				cat.setCategoryRefKey(safeTrim(rs.getString("categoryrefkey")));
				cat.setCategoryName(safeTrim(rs.getString("categoryname")));
				cat.setParentRefKey(safeTrim(rs.getString("parentrefkey")));
				// the master key is carried in the unused-for-Kapture objectId slot so the
				// grouping below needs no extra VO field
				cat.setObjectId(safeTrim(rs.getString("mastercategoryrefkey")));
				categoriesList.add(cat);
				cat = null;
			}
			rs.close(); rs = null;
			pstmt.close(); pstmt = null;

			if (categoriesList.size() <= 0) {
				// NO CATEGORIES FETCHED - DELETE DOCUMENT DATA FROM VIEW CONTENT TABLES
				docDetails.setDocumentStatus("N");
				return docDetails;
			}

			/*
			 * UNPUBLISH_CONTENT MAPPED? -> remove the document from view content.
			 *
			 * Only this exact reference key is tested. The InfoManager version also matched
			 * "UBPUBLISH_CONTENT" (a typo, missing the N) and "EXPIRE_CONTENT"; neither is used as
			 * a fallback here - if the exact key is found the document is dropped, otherwise
			 * processing continues.
			 */
			for (IMCategoryDetails cat : categoriesList) {
				if ("UNPUBLISH_CONTENT".equals(safeTrim(cat.getCategoryRefKey()))) {
					docDetails.setDocumentStatus("N");
					return docDetails;
				}
			}

			// ---------------------------------------------------------------- ESI CATEGORIES
			applyESICategories(docDetails, conn);

			// ------------------------------------------------- DOCUMENT TYPE AND SUB TYPE
			String labelLocale = resolveLabelLocale(docDetails.getLocale(), "MNAO");
			String docId = docDetails.getDocumentId().trim();
			if (docId.startsWith("WD")) {
				docDetails.setDocumentType("WIRING_DIAGRAMS");
				docDetails.setDocumentTypeName(getCategoryLabel("WIRING_DIAGRAMS",
						docDetails.getLocale(), labelLocale, conn));
			} else if (docId.startsWith("AC")) {
				docDetails.setDocumentType("ACCESSORIES");
				docDetails.setDocumentTypeName(getCategoryLabel("ACCESSORIES",
						docDetails.getLocale(), labelLocale, conn));
			} else if (docId.startsWith("SM")) {
				for (IMCategoryDetails cat : categoriesList) {
					if (MASTER_KEY_SERVICE_MANUAL.equalsIgnoreCase(safeTrim(cat.getObjectId()))
							&& !MASTER_KEY_SERVICE_MANUAL.equalsIgnoreCase(cat.getCategoryRefKey())) {
						docDetails.setDocumentType(cat.getCategoryRefKey());
						docDetails.setDocumentTypeName(cat.getCategoryName());
						break;
					}
				}
			} else if (docId.startsWith("SI")) {
				/*
				 * SI TYPE AND SUB TYPE. The old version decided type vs sub-type from object-id
				 * depth; here a row whose parent is the master key itself is the TYPE, and one
				 * nested below that is the SUB TYPE (with its parent supplying the type).
				 */
				for (IMCategoryDetails cat : categoriesList) {
					if (!MASTER_KEY_SERVICE_INFORMATION.equalsIgnoreCase(safeTrim(cat.getObjectId()))) {
						continue;
					}
					String refKey = cat.getCategoryRefKey();
					if (MASTER_KEY_SERVICE_INFORMATION.equalsIgnoreCase(refKey)) {
						// the master row itself - not a type
						continue;
					}
					String parent = safeTrim(cat.getParentRefKey());
					if ("".equals(parent) || MASTER_KEY_SERVICE_INFORMATION.equalsIgnoreCase(parent)) {
						docDetails.setDocumentType(refKey);
						docDetails.setDocumentTypeName(cat.getCategoryName());
						// NO SUB TYPE
					} else {
						docDetails.setDocumentSubType(refKey);
						docDetails.setDocumentSubTypeName(cat.getCategoryName());
						docDetails.setDocumentType(parent);
						docDetails.setDocumentTypeName(getCategoryLabel(parent,
								docDetails.getLocale(), labelLocale, conn));
					}
					break;
				}

				/*
				 * SI ATTRIBUTES - description, SI number, issue date, first publication date.
				 * These used to be XPath lookups against OK_IM.CONTENTVALUE; the same XPaths are
				 * now evaluated against the generated document XML. A missing attribute is not an
				 * error - the field simply stays blank.
				 */
				KaptureContentXmlReader xml = KaptureContentXmlReader.open(docDetails.getDocumentId(),
						docDetails.getLocale(), version, published);
				String description = xml.getDescription();
				if (null != description && !"".equals(description)) {
					// TRUNCATION RULE CARRIED OVER VERBATIM
					if (description.length() > 100) {
						docDetails.setDescription(description.substring(0, 99));
					} else {
						docDetails.setDescription(description);
					}
				}
				docDetails.setSiNumber(xml.getSINumber(docDetails.getDocumentType()));
				docDetails.setIssueDate(xml.getIssueDate());
				docDetails.setFirstPublicationDate(xml.getFirstPublicationDate());
				if (!xml.isAvailable()) {
					logger.info("prepareViewContentData :: no content XML for {"
							+ docDetails.getDocumentId() + "} {" + docDetails.getLocale() + "} v"
							+ version + " - SI attributes left blank.");
				}
				xml = null;
				description = null;
			}

			// ------------------------------------------------------- MODEL YEAR CATEGORIES
			for (IMCategoryDetails cat : categoriesList) {
				if (!MASTER_KEY_MODEL_YEAR.equalsIgnoreCase(safeTrim(cat.getObjectId()))
						|| MASTER_KEY_MODEL_YEAR.equalsIgnoreCase(cat.getCategoryRefKey())) {
					continue;
				}
				MNAOViewContentDetails modelYearData = copyHeader(docDetails);
				String refKey = safeTrim(cat.getCategoryRefKey());
				String parent = safeTrim(cat.getParentRefKey());
				if (!"".equals(parent) && !MASTER_KEY_MODEL_YEAR.equalsIgnoreCase(parent)) {
					// MODEL_YEAR - the year is the tail of the reference key, the model its parent
					if (refKey.lastIndexOf("_") != -1) {
						modelYearData.setYear(refKey.substring(refKey.lastIndexOf("_") + 1));
					}
					modelYearData.setModel(getCategoryLabel(parent, docDetails.getLocale(),
							labelLocale, conn));
				} else {
					// MODEL ONLY, NO YEAR AVAILABLE
					modelYearData.setModel(cat.getCategoryName());
				}
				if (null == docDetails.getModelYearList()) {
					docDetails.setModelYearList(new ArrayList<MNAOViewContentDetails>());
				}
				docDetails.getModelYearList().add(modelYearData);
				modelYearData = null;
			}

			// ------------------------------------------------------------- VIN CATEGORIES
			for (IMCategoryDetails cat : categoriesList) {
				if (!MASTER_KEY_VIN.equalsIgnoreCase(safeTrim(cat.getObjectId()))
						|| MASTER_KEY_VIN.equalsIgnoreCase(cat.getCategoryRefKey())) {
					continue;
				}
				MNAOViewContentDetails vinData = copyHeader(docDetails);

				/*
				 * WMI / VDS / VIS - the old version needed a 4-part object id; the equivalent here
				 * is a 4-level chain VIN -> WMI -> VDS -> VIS range.
				 */
				ArrayList<IMCategoryDetails> chain = getCategoryHierarchy(cat.getCategoryRefKey(),
						docDetails.getLocale(), labelLocale, conn);

				/*
				 * EACH LEVEL IS FILLED IN AS FAR AS THE CHAIN ACTUALLY GOES.
				 *
				 * A mapping made ABOVE VIS level still produces a row - that is what the
				 * InfoManager version did (it added the row outside its tokens.length==4
				 * check) and it is kept on purpose. What it does NOT do any more is leave that
				 * row completely blank: a VDS-level mapping now carries its WMI and VDS codes
				 * and only the VIS range stays empty.
				 */
				if (null != chain && chain.size() >= 2) {
					vinData.setWmiCode(chain.get(1).getCategoryName());
				}
				if (null != chain && chain.size() >= 3) {
					vinData.setVdsCode(chain.get(2).getCategoryName());
				}

				/*
				 * ONLY A FULL CHAIN CARRIES A VIS RANGE - the leaf category name with the WMI
				 * and VDS codes stripped off the front.
				 */
				if (null != chain && chain.size() == 4) {
					String keyToBeReplaced = "";
					if (null != vinData.getWmiCode() && !"".equals(vinData.getWmiCode())
							&& null != vinData.getVdsCode() && !"".equals(vinData.getVdsCode())) {
						if (!vinData.getWmiCode().trim().equals("-")) {
							keyToBeReplaced = keyToBeReplaced + vinData.getWmiCode().trim();
						}
						keyToBeReplaced = (keyToBeReplaced + vinData.getVdsCode().trim()).trim();

						// REMOVE IT FROM CATEGORY NAME
						String visRangeName = safeTrim(cat.getCategoryName()).replace(keyToBeReplaced, "");
						if (!"".equals(visRangeName)) {
							visRangeName = visRangeName.trim();
							// FIRST 6 CHARS ARE VIS START RANGE AND REMAINING ARE VIS END RANGE
							if (visRangeName.length() >= 7) {
								vinData.setVisStartRange(visRangeName.substring(0, 6));
								vinData.setVisEndRange(visRangeName.substring(6));
							} else {
								vinData.setVisStartRange(visRangeName);
							}
						}
						visRangeName = null;
					}
					keyToBeReplaced = null;
				}
				chain = null;

				if (null == docDetails.getVinList()) {
					docDetails.setVinList(new ArrayList<MNAOViewContentDetails>());
				}
				docDetails.getVinList().add(vinData);
				vinData = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"prepareViewContentData()", e);
		} finally {
			closeQuietly(rs, pstmt);
			if (null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
		return docDetails;
	}

	/**
	 * ESI levels 1 to 3. Kapture keeps ESI mappings in their own table, so the level a category
	 * sits at is derived from its parentrefkey chain instead of object-id token depth.
	 *
	 * A DOCUMENT'S ESI MAPPING ALWAYS STARTS AT LEVEL 1 and may go to level 2 or level 3. The ESI
	 * ROOT IS NEVER ONE OF THOSE LEVELS: a document mapped only to the root key counts as having
	 * NO ESI category, so the root row is dropped up front and an otherwise empty set simply
	 * leaves all three levels unset.
	 *
	 * The root row may or may not even be stored - SI1123 has exactly one row, ESI01 with
	 * parentrefkey ESI, and no row for ESI itself - so levels are counted relative to the root
	 * KEY rather than to whatever the shallowest returned row happens to be. Indexing off the
	 * returned set would yield no levels at all for a document like that.
	 *
	 * The level-1 code keeps the old shape: the reference key with a leading "ESI" stripped.
	 * The level-3 code keeps the old rule too: characters 7-8 of an 8-character reference key.
	 */
	private static void applyESICategories(MNAOViewContentDetails docDetails, Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			LinkedHashMap<String, IMCategoryDetails> byRefKey = new LinkedHashMap<String, IMCategoryDetails>();
			String sql = "SELECT referencekey, categoryname, parentrefkey FROM "
					+ T_ESI_ARTICLE_CATEGORIES + " WHERE article_id = ? AND locale = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, docDetails.getDocumentId());
			pstmt.setString(2, docDetails.getLocale());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				IMCategoryDetails cat = new IMCategoryDetails();
				cat.setCategoryRefKey(safeTrim(rs.getString("referencekey")));
				cat.setCategoryName(safeTrim(rs.getString("categoryname")));
				cat.setParentRefKey(safeTrim(rs.getString("parentrefkey")));
				// DROP THE ROOT ROW IF IT IS PRESENT - levels are counted BELOW it, and it is not
				// always stored (see the note above)
				if (!"".equals(cat.getCategoryRefKey())
						&& !MASTER_KEY_ESI.equalsIgnoreCase(cat.getCategoryRefKey())) {
					byRefKey.put(cat.getCategoryRefKey(), cat);
				}
				cat = null;
			}
			rs.close(); rs = null;
			pstmt.close(); pstmt = null;

			if (byRefKey.isEmpty()) {
				return;
			}

			// the deepest row is the one nobody names as a parent
			IMCategoryDetails deepest = null;
			for (IMCategoryDetails cat : byRefKey.values()) {
				boolean isParentOfSomething = false;
				for (IMCategoryDetails other : byRefKey.values()) {
					if (cat.getCategoryRefKey().equals(other.getParentRefKey())) {
						isParentOfSomething = true;
						break;
					}
				}
				if (!isParentOfSomething) {
					deepest = cat;
					break;
				}
			}
			if (null == deepest) {
				return;
			}

			// build the chain root-first
			ArrayList<IMCategoryDetails> chain = new ArrayList<IMCategoryDetails>();
			IMCategoryDetails current = deepest;
			int guard = 0;
			while (null != current && guard < MAX_HIERARCHY_DEPTH) {
				chain.add(0, current);
				current = byRefKey.get(current.getParentRefKey());
				guard++;
			}

			/*
			 * The root is excluded from the map, so chain[0] IS level 1.
			 */
			if (chain.size() > 0) {
				IMCategoryDetails l1 = chain.get(0);
				String rKey = safeTrim(l1.getCategoryRefKey()).toLowerCase().replace("esi", "");
				docDetails.setCatCodeLevel1(rKey.trim().toUpperCase());
				docDetails.setCatNameLevel1(l1.getCategoryName());
			}
			if (chain.size() > 1) {
				// 2ND LEVEL CODE IS THE REFERENCE KEY ITSELF
				docDetails.setCatCodeLevel2(chain.get(1).getCategoryRefKey());
				docDetails.setCatNameLevel2(chain.get(1).getCategoryName());
			}
			if (chain.size() > 2) {
				IMCategoryDetails l3 = chain.get(2);
				String thirdLevelCode = "";
				if (null != l3.getCategoryRefKey() && l3.getCategoryRefKey().trim().length() == 8) {
					thirdLevelCode = l3.getCategoryRefKey().trim().substring(6, 8);
				}
				docDetails.setCatCodeLevel3(thirdLevelCode);
				docDetails.setCatNameLevel3(l3.getCategoryName());
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"applyESICategories()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
	}

	/**
	 * Copies the document-level fields onto a per-row entry, exactly the set the old version
	 * copied onto each model-year / VIN row.
	 */
	private static MNAOViewContentDetails copyHeader(MNAOViewContentDetails source) {
		MNAOViewContentDetails copy = new MNAOViewContentDetails();
		copy.setDocumentId(source.getDocumentId());
		copy.setLocale(source.getLocale());
		copy.setDocumentStatus(source.getDocumentStatus());
		copy.setPublishDate(source.getPublishDate());
		copy.setDisplayEndDate(source.getDisplayEndDate());
		copy.setTitle(source.getTitle());
		copy.setDocumentCreateDate(source.getDocumentCreateDate());
		copy.setDocumentLastModifiedDate(source.getDocumentLastModifiedDate());
		copy.setContentId(source.getContentId());
		copy.setRecordId(source.getRecordId());
		copy.setDescription(source.getDescription());

		copy.setCatCodeLevel1(source.getCatCodeLevel1());
		copy.setCatCodeLevel2(source.getCatCodeLevel2());
		copy.setCatCodeLevel3(source.getCatCodeLevel3());
		copy.setCatNameLevel1(source.getCatNameLevel1());
		copy.setCatNameLevel2(source.getCatNameLevel2());
		copy.setCatNameLevel3(source.getCatNameLevel3());

		copy.setDocumentType(source.getDocumentType());
		copy.setDocumentTypeName(source.getDocumentTypeName());
		copy.setDocumentSubType(source.getDocumentSubType());
		copy.setDocumentSubTypeName(source.getDocumentSubTypeName());

		copy.setFirstPublicationDate(source.getFirstPublicationDate());
		copy.setSiNumber(source.getSiNumber());
		copy.setIssueDate(source.getIssueDate());
		return copy;
	}

	/**
	 * k_article stores created_date / date_modified as VARCHAR in MM/dd/yyyy HH:mm:ss, whereas the
	 * old CONTENTTEXT columns were real timestamps and the VO still expects Timestamp.
	 * An unparseable or empty value yields null rather than an exception.
	 */
	private static Timestamp toTimestamp(String value) {
		if (null == value || "".equals(value.trim())) {
			return null;
		}
		String text = value.trim();
		String[] patterns = new String[] { "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy", "yyyy-MM-dd HH:mm:ss" };
		for (int i = 0; i < patterns.length; i++) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(patterns[i]);
				sdf.setLenient(false);
				return new Timestamp(sdf.parse(text).getTime());
			} catch (Exception ignore) {
				// try the next pattern
			}
		}
		logger.info("toTimestamp :: could not parse {" + text + "}");
		return null;
	}

	/**
	 * Locale to fall back to when a category carries no name in the document's own locale.
	 * MNAO -> en_US, MC -> the selected locale itself, MME -> en_UK (NOT en_GB - k_categories
	 * carries both as separate locales). Configured in application.properties so it can be
	 * corrected without a rebuild.
	 *
	 * ALSO THE SOURCE LOCALE FOR HIERARCHY REPLICATION. Where the two uses meet: a category
	 * whose row is missing for the wanted locale is copied from this locale instead, so the
	 * locale trusted to carry a readable name is the same one trusted to carry the tree shape.
	 */
	public static String resolveLabelLocale(String documentLocale, String requestType) {
		String fallback = documentLocale;
		try {
			if (null != requestType && "MME".equalsIgnoreCase(requestType.trim())) {
				fallback = ApplicationProperties
						.getProperty("kapture.category.label.fallback.mme");
			} else if (null != requestType && "MC".equalsIgnoreCase(requestType.trim())) {
				// MC deliberately uses the selected locale - no fallback
				fallback = documentLocale;
			} else {
				fallback = ApplicationProperties
						.getProperty("kapture.category.label.fallback.mnao");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"resolveLabelLocale()", e);
		}
		if (null == fallback || "".equals(fallback.trim())) {
			fallback = documentLocale;
		}
		return fallback.trim();
	}

	/**
	 * THE contentid OF THE SERVICE INFORMATION CHANNEL, read once and remembered.
	 *
	 * WHY THIS EXISTS ALONGSIDE getContentId(documentId, locale, conn): that one answers "which
	 * channel does THIS DOCUMENT belong to" and needs a document to ask about. The SI Data Load
	 * screen has no document at all on the New Document path - it is about to create the first
	 * version - so the channel has to be identified directly.
	 *
	 * ONE ROW, ONE CHANNEL. k_content_type holds one row per channel (19 of them) and the SI row is
	 * the one whose documentid_prefix is SI. Returns "" and logs when it cannot be resolved, which
	 * the caller must treat as "cannot talk to Kapture" rather than carrying on with a blank id -
	 * a blank contentid on a REST call does not fail loudly, it addresses nothing.
	 *
	 * CACHED DELIBERATELY. Called on every doGet of the screen, and again by the schedule thread
	 * for every operation; the row is static configuration, so re-reading it would be one CMS round
	 * trip per page load for a value that cannot change.
	 */
	public static String getSIChannelContentId() {
		return getChannelContentId(SI_DOCUMENT_ID_PREFIX);
	}

	/** Channel content ids already resolved, keyed by document id prefix. */
	private static final java.util.Map<String, String> CHANNEL_CONTENT_IDS =
			new java.util.HashMap<String, String>();

	/**
	 * THE CONTENT ID OF A CHANNEL, BY DOCUMENT ID PREFIX - "SI", "SM", "OSM" ...
	 *
	 * Every write endpoint is addressed ?contentId=<channel>, and the value is per CHANNEL, not per
	 * document. Translation Update handles SI and SM in one screen, which is why this is keyed by
	 * prefix rather than fixed to SI as getSIChannelContentId() was.
	 *
	 * @return the content id, or "" when the channel has no row - callers must treat "" as
	 *         "cannot proceed", never as a usable value
	 */
	public static String getChannelContentId(String documentIdPrefix) {
		if (null == documentIdPrefix || "".equals(documentIdPrefix.trim())) {
			logger.info("getChannelContentId :: no document id prefix given.");
			return "";
		}
		String prefix = documentIdPrefix.trim().toUpperCase();
		synchronized (CHANNEL_CONTENT_IDS) {
			String cached = CHANNEL_CONTENT_IDS.get(prefix);
			if (null != cached && !"".equals(cached)) {
				return cached;
			}
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;
		String contentId = "";
		try {
			conn = getCMSConnection();
			String sql = "SELECT contentid FROM " + T_CONTENT_TYPE
					+ " WHERE TRIM(documentid_prefix) = ? LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, prefix);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				contentId = safeTrim(rs.getString("contentid"));
			}

			if (!"".equals(contentId)) {
				synchronized (CHANNEL_CONTENT_IDS) {
					CHANNEL_CONTENT_IDS.put(prefix, contentId);
				}
				if (SI_DOCUMENT_ID_PREFIX.equals(prefix)) {
					// kept so the SI screens' existing cache field still warms as it always did
					siChannelContentId = contentId;
				}
				logger.info("getChannelContentId :: channel {" + prefix + "} resolved to {"
						+ contentId + "}.");
			} else {
				logger.info("getChannelContentId :: NO ROW in " + T_CONTENT_TYPE
						+ " with documentid_prefix {" + prefix
						+ "}. Kapture operations for that channel cannot proceed.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FetchKaptureDataDAO.class.getName(),
					"getChannelContentId()", e);
			contentId = "";
		} finally {
			closeQuietly(rs, pstmt);
			if (null != conn) {
				try {
					conn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
		return contentId;
	}

	private static String safeTrim(String value) {
		return null == value ? "" : value.trim();
	}

	private static void closeQuietly(ResultSet rs, PreparedStatement pstmt) {
		try {
			if (null != rs)
				rs.close();
		} catch (Exception ignore) {
			// nothing useful to do here
		}
		try {
			if (null != pstmt)
				pstmt.close();
		} catch (Exception ignore) {
			// nothing useful to do here
		}
	}
}
