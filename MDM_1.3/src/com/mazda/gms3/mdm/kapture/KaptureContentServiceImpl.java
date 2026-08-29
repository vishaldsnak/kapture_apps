package com.mazda.gms3.mdm.kapture;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.dao.KaptureCategoryDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;
import com.mazda.gms3.mdm.vo.SIVinDetails;

/**
 * KAPTURE REPLACEMENT FOR InfoManagerServiceImpl.modifyContent() AND .deleteContent().
 *
 * Both of those did the same thing to InfoManager - fetch the content record, change which
 * categories are on it, write it back - so they are ONE implementation here, differing only in
 * whether the reference keys are added or removed. There is no delete API involved: removing a
 * VIN range is an update with that category taken off the document.
 *
 * READ, CHANGE, WRITE - BUT THE READ AND THE WRITE ARE DIFFERENT SHAPES:
 *   1. GET  /kauthor-api/display-Article    -> the article as Kapture holds it today (35 fields)
 *   2. change the mapped categories
 *   3. PUT  /kauthor-api/update-article-tab -> a SMALLER, FIXED payload built from that response
 *   4. POST /kauthor-api/publish-article    -> ONLY when the version updated was Published
 *
 * The display response CANNOT simply be sent back. The update endpoint accepts its own field set
 * and answers 400 "Validation failed. Missing or invalid mandatory fields" to anything else - it
 * needs k_article's id (which display never returns) and rejects the extra read-side fields.
 * buildUpdatePayload() therefore constructs the documented write shape, carrying every value it
 * does not own straight over from the read so nothing is invented.
 *
 * WHAT ACTUALLY CHANGES: articlelistcategory, and nothing else. THE CSV FIELDS DO NOT TAG THE
 * DOCUMENT - categoriesTreeRefKeys / categoriesESITreeRefKeys are accepted by the API but the
 * mapping is taken from the ARRAYS, so a payload carrying only the CSVs succeeds and maps nothing.
 * articleesicategory is carried over untouched: the SI VIN screens do not manage ESI categories,
 * but the array has to be sent or the update drops the document's existing ESI mapping.
 *
 * ONLY THE SELECTED ENTRIES ARE SENT. display-Article returns the whole tree with each mapped leaf
 * at selected=true and its ancestors at selected=false; only the leaves are the mapping.
 *
 * EVERY REFERENCE KEY TRAVELS WITH ITS DISPLAY NAME - categories, user groups and views alike.
 * Omitting a name is accepted by the API but KAuthor then shows "undefined" for that entry, so the
 * call looks successful while leaving the document visibly broken.
 *
 * PRIMARY-LOCALE DOCUMENTS ONLY, FOR NOW. The version row is matched on article_primary_locale.
 * A TRANSLATION carries the wanted locale in article_translated_locale instead, with a different
 * article_primary_locale - that fallback is a deliberate follow-up, to be added and tested once
 * the primary-locale flow is signed off.
 *
 * PUBLISHED DOCUMENTS: an update to a published article leaves a NEW UNPUBLISHED version behind,
 * which is then promoted with publish-article. An UNPUBLISHED document is deliberately LEFT
 * unpublished - this screen maps VIN ranges, it does not decide a working draft is ready to go
 * live. When the promotion fails, ERROR_NOT_REPUBLISHED is set on the returned SIVinDetails rather
 * than reporting a success that did not happen.
 */
public class KaptureContentServiceImpl {

	static Logger logger = LogManager.getLogger(KaptureContentServiceImpl.class);

	/** k_article's primary key - NOT articleId. See KaptureArticleIdentity. */
	/*
	 * THE FORMAT THE WRITE ENDPOINTS TAKE, and the ones the read endpoint hands out that they
	 * do not - see toWriteFormat().
	 */
	private static final String WRITE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String[] SLASH_DATE_FORMATS = { "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy" };

	private static final String F_ID = "id";

	/*
	 * THE CATEGORY ARRAYS ARE THE WRITE CONTRACT. categoriesTreeRefKeys / categoriesESITreeRefKeys
	 * are NOT sent: the API accepts them but does not tag the document from them, so a payload
	 * carrying only the CSVs reports success and maps nothing.
	 */
	private static final String F_CATEGORY_ARRAY = "articlelistcategory";
	private static final String F_ESI_CATEGORY_ARRAY = "articleesicategory";
	private static final String F_CATEGORY_REF_KEY = "categoryRefKey";
	private static final String F_CATEGORY_NAME = "categoryName";
	private static final String F_SELECTED = "selected";

	/*
	 * FIELDS THAT ONLY THE TRANSLATION PAYLOAD CARRIES.
	 *
	 * NOTE THE MISSPELLING IN parentCategroryRefKey - "Categrory". THAT IS KAPTURE'S SPELLING, IN
	 * THE API AND IN k_article_category ALIKE. IT IS NOT A TYPO HERE; CORRECTING IT BREAKS BOTH.
	 */
	private static final String F_PARENT_REF_KEY = "parentCategroryRefKey";
	private static final String F_MASTER_CATEGORY_REF_KEY = "masterCategoryRefKey";
	private static final String F_USER_GROUP = "articlelistusergroup";
	private static final String F_LOCALE = "locale";
	private static final String F_ARTICLE_ID = "articleId";
	private static final String F_CONTENT_ID = "contentId";

	/*
	 * THE save-translate-article FIELDS CARRIED STRAIGHT OVER FROM THE READ.
	 *
	 * Kapture's own working sample holds 28 fields and the read returns 27 of them, in the same
	 * shapes - so the payload is that sample, populated from the response. The ones NOT listed
	 * here are set explicitly in buildTranslationPayload() because the read cannot supply them or
	 * supplies them wrongly for a translation.
	 */
	private static final String[] TRANSLATE_ECHO_FIELDS = { "articleOwner", "ISARTICLE",
			"articleCreator", "contentId", "contentid", "emailId", "type", "title", "DOCUMENTID",
			"crawlType", "userLocale", "articleId", "articlelistview", "createdDate",
			"contentTranslateList", "articleCreatorUserID", "status" };

	/** Set on the returned VO when the update succeeded but the document could not be re-published. */
	public static final String ERROR_NOT_REPUBLISHED = "NOT_REPUBLISHED";

	/*
	 * WHY A FAILED CALL STILL RETURNS AN OBJECT.
	 *
	 * Callers decide success by whether a DOCUMENT ID came back - servlets and batches
	 * alike - so an object carrying only an error code is still read as a failure, and
	 * the reason survives the return instead of living solely in the log. The batches
	 * copy it into the modification report, which used to print Failure with the
	 * ERROR_CODE and ERROR_MESSAGE columns empty.
	 */
	public static final String ERROR_DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";

	public static final String ERROR_NO_ARTICLE_ROW = "NO_ARTICLE_ROW";

	public static final String ERROR_READ_FAILED = "DISPLAY_ARTICLE_FAILED";

	public static final String ERROR_UPDATE_FAILED = "UPDATE_ARTICLE_FAILED";

	public static final String ERROR_UNEXPECTED = "UNEXPECTED_ERROR";

	public static final String ERROR_CATEGORY_NOT_FOUND = "CATEGORY_NOT_IN_KAPTURE";

	/**
	 * ADD VIN categories to a document. Mirrors InfoManagerServiceImpl.modifyContent().
	 *
	 * @param documentId                 article id, e.g. "SI1123"
	 * @param locale                     document locale
	 * @param wslId                      the acting user, for logging
	 * @param categoriesToBeAddedToDocument reference keys of the VIN ranges to map
	 * @param documentDetails            carried through and returned, as before
	 */
	public static SIVinDetails modifyContent(String documentId, String locale, String wslId,
			ArrayList<String> categoriesToBeAddedToDocument, SIVinDetails documentDetails) {
		return applyCategoryChange(documentId, locale, wslId, categoriesToBeAddedToDocument,
				documentDetails, true);
	}

	/**
	 * REMOVE VIN categories from a document. Mirrors InfoManagerServiceImpl.deleteContent().
	 *
	 * Despite the name nothing is deleted - the document is updated with those categories taken
	 * off it, which is exactly what the InfoManager version did.
	 */
	public static SIVinDetails deleteContent(String documentId, String locale, String wslId,
			ArrayList<String> categoriesToBeDeleted, SIVinDetails documentDetails) {
		return applyCategoryChange(documentId, locale, wslId, categoriesToBeDeleted,
				documentDetails, false);
	}

	private static SIVinDetails applyCategoryChange(String documentId, String locale, String wslId,
			ArrayList<String> referenceKeys, SIVinDetails documentDetails, boolean add) {
		Connection cmsConn = null;
		try {
			if (null == documentId || "".equals(documentId.trim()) || null == locale
					|| "".equals(locale.trim())) {
				logger.info("applyCategoryChange :: documentId / locale missing - nothing done.");
				return null;
			}
			if (null == referenceKeys || referenceKeys.size() <= 0) {
				logger.info("applyCategoryChange :: no categories supplied for {" + documentId
						+ "} - nothing to " + (add ? "add" : "remove") + ".");
				return documentDetails;
			}

			cmsConn = DBConnectionHelper.getCMSConnection();

			/*
			 * THE DOCUMENT AS KAPTURE HOLDS IT. contentid identifies the CHANNEL, and the version
			 * to address depends on whether the document is published - both come from k_article.
			 */
			/*
			 * HEADER ONLY - the last argument. Nothing below reads the category list, and loading
			 * it would now cost an extra latest-article call on every write.
			 */
			SIVinDetails current = FetchKaptureDataDAO.getDocumentsData(documentId, locale,
					FetchKaptureDataDAO.MASTER_KEY_VIN, "MNAO", cmsConn, "N", false);
			if (null == current || null == current.getDocumentId()
					|| "".equals(current.getDocumentId())) {
				logger.info("applyCategoryChange :: {" + documentId + "} / {" + locale
						+ "} not found in Kapture.");
				return failed(documentDetails, ERROR_DOCUMENT_NOT_FOUND,
						"Document {" + documentId + "} was not found in Kapture for locale {"
								+ locale + "}.");
			}
			String contentId = FetchKaptureDataDAO.getContentId(documentId, locale, cmsConn);

			/*
			 * THE VERSION ROW ITSELF, FROM k_article. display-Article cannot supply id at all and
			 * returns articleVersion as a JSON NUMBER (1.0 arrives as 1), so both come from the
			 * table - see KaptureArticleIdentity.
			 */
			KaptureArticleIdentity identity = FetchKaptureDataDAO.getArticleIdentity(documentId,
					locale, cmsConn);
			if (!identity.isUsable()) {
				logger.info("applyCategoryChange :: no k_article identity for {" + documentId + "} / {"
						+ locale + "} - cannot update.");
				return failed(documentDetails, ERROR_NO_ARTICLE_ROW,
						"No k_article row for {" + documentId + "} / {" + locale
								+ "} - the update payload could not be identified.");
			}
			String version = identity.getArticleVersion();
			boolean published = identity.isPublished();

			KaptureApiClient client = new KaptureApiClient();
			/*
			 * latest-article RESOLVES THE VERSION AND THE STATE ITSELF, so neither is passed. It
			 * returns byte-identical content to display-Article for a master and a translation
			 * alike - see KaptureApiClient.latestArticle().
			 *
			 * AN EMPTY BODY ON A 200 IS ITS "NOT FOUND", so that is reported as DOCUMENT_NOT_FOUND
			 * rather than as a failed read - it means the document has no version in this locale.
			 */
			/*
			 * ONE LABEL, ON EVERY LINE THIS OPERATION WRITES. A single MME run walks the master
			 * and several translations, and without it the log is a wall of identical-looking
			 * entries where only the locale differs and nothing says which ENDPOINT was used or
			 * why. Built once, here, because this is the first point where the identity is known.
			 */
			String context = describe(identity, locale);
			logger.info("applyCategoryChange :: GET {" + documentId + "} :: " + context
					+ " - reading the latest version for this locale.");
			KaptureApiResult read = client.latestArticle(documentId, contentId, locale, context);
			if (read.isOk() && (null == read.body || "".equals(read.body.trim()))) {
				logger.info("applyCategoryChange :: latest-article returned an empty body for {"
						+ documentId + "} / {" + locale + "} - no version in that locale.");
				return failed(documentDetails, ERROR_DOCUMENT_NOT_FOUND,
						"Document {" + documentId + "} has no version in locale {" + locale + "}.");
			}
			if (!read.isOk() || null == read.body || "".equals(read.body.trim())) {
				logger.info("applyCategoryChange :: latest-article failed for {" + documentId
						+ "} :: " + read.message);
				return failed(documentDetails, ERROR_READ_FAILED, read.message);
			}

			JsonObject article = new JsonParser().parse(read.body).getAsJsonObject();

			/*
			 * THE CARRIED-OVER FIELDS, RESOLVED ONCE AND SENT ON BOTH CALLS.
			 *
			 * ONE RULE, NO EXCEPTIONS: WHATEVER display-Article RETURNS IS SENT BACK AS IS.
			 * No database fallback, no correction, no substitution. Both endpoints rewrite the
			 * whole row, so anything left out is stored empty - carrying the response forward
			 * unchanged is what keeps the document intact, and it means any discrepancy that
			 * survives is a discrepancy in what the API served us, which is answerable.
			 *
			 * dateAdded IS THE ONLY FIELD NOT IN THE RESPONSE, so createdDate is used for it -
			 * Kapture sets date_added and created_date to the same value on every row it
			 * populates itself.
			 *
			 * WHAT IS DELIBERATELY NOT CARRIED OVER: modifiedBy and userEmail. Those describe WHO
			 * IS WRITING, so they come from the account the token was issued for.
			 *
			 * articleCreatorUserID USED TO BE IN THAT LIST AND NO LONGER IS. Kapture's answer to
			 * A7 (2026-08-07) established that it is part of the document's AUTHORSHIP, not of the
			 * current edit, and must be echoed like articleOwner and articleCreator.
			 */
			String expireDate = stringOf(article, "expireDate");
			String reviewDate = stringOf(article, "reviewDate");
			String createdBy = stringOf(article, "createdBy");
			String createdDate = stringOf(article, "createdDate");
			String dateAdded = createdDate;

			/*
			 * THE MAPPED CATEGORIES, i.e. the selected=true entries only. The tree Kapture returns
			 * carries every ancestor at selected=false purely for rendering; sending those back
			 * would map the document to whole WMI and VDS levels.
			 */
			/*
			 * THE TWO ENDPOINTS WANT DIFFERENT CATEGORY SHAPES.
			 *
			 * update-article-tab takes ONLY the mapped leaves and rebuilds the ancestry itself.
			 * save-translate-article is sent the WHOLE TREE with its selected flags, which is what
			 * Kapture's own working sample carries - so for a translation the array is echoed
			 * complete and edited in place rather than reduced.
			 */
			boolean translation = identity.isTranslation();
			JsonArray categories = translation
					? asArray(article, F_CATEGORY_ARRAY)
					: selectedCategories(article, F_CATEGORY_ARRAY);
			int changed = 0;
			int missing = 0;
			if (add) {
				int[] result = translation
						? addTranslationCategories(categories, referenceKeys, locale, documentId,
								contentId, cmsConn)
						: addCategories(categories, referenceKeys, locale, cmsConn);
				changed = result[0];
				missing = result[1];
			} else {
				JsonArray kept = new JsonArray();
				changed = removeCategories(categories, referenceKeys, kept);
				categories = kept;
			}

			/*
			 * A CATEGORY THAT IS NOT IN KAPTURE STOPS THE WHOLE CALL, BEFORE ANYTHING IS
			 * WRITTEN. It means its creation failed earlier in this run, so mapping the rest
			 * would leave the document half done while reporting success - and a partial
			 * mapping is harder to spot afterwards than a clean failure.
			 */
			if (missing > 0) {
				logger.info("applyCategoryChange :: " + missing + " of " + referenceKeys.size()
						+ " categor(y/ies) are not in Kapture for {" + locale
						+ "} - {" + documentId + "} NOT updated.");
				return failed(documentDetails, ERROR_CATEGORY_NOT_FOUND, missing + " of "
						+ referenceKeys.size() + " categor(y/ies) could not be found in Kapture for"
						+ " locale " + locale + " - they were not created, so the document was"
						+ " left untouched.");
			}
			if (changed <= 0) {
				/*
				 * NOTHING TO DO IS A SUCCESS, NOT A FAILURE.
				 *
				 * Every category asked for is already mapped exactly as asked - re-running the
				 * same spreadsheet, or re-removing a VIN already removed - so the document is
				 * ALREADY IN THE REQUESTED STATE and an update would only mint a pointless new
				 * version. Same reading as a 409 already-exists on a category create.
				 *
				 * THE IDENTITY IS STAMPED BEFORE RETURNING because callers tell success from
				 * failure by whether a document id came back - MCSIVINBatchProcessingImpl line
				 * 1191 and its MNAO/MME twins. Returning the bare object made a clean no-op
				 * indistinguishable from a failed call, and schedule 25383 was reported Failure
				 * with all 6 of 6 VIN ranges mapped and no error anywhere in the run.
				 */
				logger.info("applyCategoryChange :: nothing actually changed on {" + documentId
						+ "} - already in the requested state, update skipped.");
				if (null == documentDetails) {
					documentDetails = new SIVinDetails();
				}
				documentDetails.setDocumentId(documentId);
				documentDetails.setLocale(locale);
				documentDetails.setFetchedVersion(version);
				return documentDetails;
			}

			/*
			 * A TRANSLATION IS NOT WRITTEN THROUGH update-article-tab.
			 *
			 * That endpoint handles the ORIGINAL ARTICLE only - confirmed by the Kapture API team
			 * 2026-08-05 - and does not persist article_translated_locale, so a translation sent
			 * through it comes back with the column NULL, collapses onto the master's locale, and
			 * STOPS EXISTING as a locale anyone can address. It reports 200 while doing it.
			 */
			String payload;
			KaptureApiResult write;
			logger.info("applyCategoryChange :: UPDATE {" + documentId + "} :: " + context + " - "
					+ changed + " categor(y/ies) to " + (add ? "add" : "remove") + ", via "
					+ (translation ? "save-translate-article" : "update-article-tab") + ".");
			if (translation) {
				payload = buildTranslationPayload(article, identity, documentId, contentId, locale,
						categories);
				write = client.saveTranslateArticle(contentId, payload, context);
			} else {
				payload = buildUpdatePayload(article, identity, documentId, locale, categories,
						client, expireDate, reviewDate, createdBy, createdDate, dateAdded);
				write = client.updateArticle(contentId, payload, context);
			}
			if (!write.isOk()) {
				logger.info("applyCategoryChange :: "
						+ (translation ? "save-translate-article" : "update-article-tab")
						+ " FAILED for {" + documentId + "} :: " + write.message);
				return failed(documentDetails, ERROR_UPDATE_FAILED, write.message);
			}
			logger.info("applyCategoryChange :: SUCCESS {" + documentId + "} :: " + context
					+ " - updated by {" + wslId + "}, " + changed + " categor(y/ies) "
					+ (add ? "added" : "removed") + ".");

			if (null == documentDetails) {
				documentDetails = new SIVinDetails();
			}
			documentDetails.setDocumentId(documentId);
			documentDetails.setLocale(locale);
			documentDetails.setFetchedVersion(version);

			/*
			 * UPDATING A PUBLISHED DOCUMENT LEAVES A NEW UNPUBLISHED VERSION, so it has to be
			 * published again. AN UNPUBLISHED DOCUMENT IS LEFT UNPUBLISHED - this screen maps VIN
			 * ranges, it does not decide that a working draft is ready to go live.
			 */
			if (published) {
				/*
				 * THE VERSION TO PUBLISH IS THE ONE THE UPDATE JUST CREATED, not the one we read.
				 * Re-reading k_article is what identifies it: the new row is now latest_version='Y'
				 * and carries its own id and version number.
				 */
				KaptureArticleIdentity created = FetchKaptureDataDAO.getArticleIdentity(documentId,
						locale, cmsConn);
				if (!created.isUsable() || created.getId().equals(identity.getId())) {
					documentDetails.setErrorCode(ERROR_NOT_REPUBLISHED);
					logger.info("applyCategoryChange :: {" + documentId + "} updated but the new"
							+ " version could not be identified - NOT re-published.");
					return documentDetails;
				}
				String publishPayload = buildPublishPayload(article, created, contentId,
						documentId, locale, client, expireDate, reviewDate, createdBy,
						createdDate, dateAdded);
				logger.info("applyCategoryChange :: publishing {" + documentId + "} id="
						+ created.getId() + " v" + created.getArticleVersion() + " ("
						+ created.getArticleState() + " -> Published)");
					KaptureApiResult republish = client.publishArticle(documentId, publishPayload,
						describe(created, locale));
				if (!republish.isOk()) {
					documentDetails.setErrorCode(ERROR_NOT_REPUBLISHED);
					logger.info("applyCategoryChange :: {" + documentId + "} v"
							+ created.getArticleVersion() + " could NOT be re-published :: "
							+ republish.message);
				} else {
					logger.info("applyCategoryChange :: {" + documentId + "} v"
							+ created.getArticleVersion() + " re-published.");
				}
			} else {
				logger.info("applyCategoryChange :: {" + documentId
						+ "} was unpublished - left unpublished.");
			}
			return documentDetails;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentServiceImpl.class.getName(),
					"applyCategoryChange()", e);
			return failed(documentDetails, ERROR_UNEXPECTED, String.valueOf(e.getMessage()));
		} finally {
			if (null != cmsConn) {
				try {
					cmsConn.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
	}

	/**
	 * THE PUBLISH REQUEST.
	 *
	 * publish-article REWRITES THE WHOLE ROW, so every column that should survive has to be in
	 * the request. A 7-field publish left article_title, article_owner, created_by and the rest
	 * NULL on the version it created - visible as a blank title in the View Content tables.
	 *
	 * WHERE EACH VALUE COMES FROM:
	 *   id / articleVersion / articleState - the version the UPDATE just created, from k_article
	 *   title, type, owner, creator, createdBy, createdDate - the document as display-Article
	 *                                         returned it, so nothing is invented here
	 *   userEmail / modifiedBy             - the account the token belongs to
	 *   expireDate / reviewDate            - null, and pubFlag true: this IS the publish
	 *
	 * THE CREATION BLOCK IS RESOLVED BY THE CALLER and passed in, so the publish writes exactly
	 * what the update wrote - straight from display-Article, see applyCategoryChange.
	 */
	private static String buildPublishPayload(JsonObject article, KaptureArticleIdentity created,
			String contentId, String documentId, String locale, KaptureApiClient client,
			String expireDate, String reviewDate, String createdBy, String createdDate,
			String dateAdded) {
		JsonObject payload = new JsonObject();
		payload.addProperty(F_ID, created.getId());
		payload.addProperty("contentId", contentId);
		payload.addProperty("articleId", documentId);
		/*
		 * BOTH LOCALES ECHOED FROM display-Article, exactly as the update payload does it -
		 * primaryLocale is the MASTER's locale, translatedLocale the one being processed.
		 *
		 * This changes nothing on any path that runs today: publish is only ever called when the
		 * version we read was PUBLISHED, and Kapture PROMOTES a translation to a full master row
		 * when it is published (SI1084's ja_JP draft 62691 primary=en_US became 62693
		 * primary=ja_JP), so a published translation already has primary == translated == its own
		 * locale. Sent this way so the payload stays correct if that promotion ever changes, and
		 * so the two payloads cannot drift apart.
		 */
		if (created.isTranslation()) {
			/*
			 * A TRANSLATION NAMES BOTH LOCALES, AND primaryLocale IS THE MASTER'S - NOT ECHOED,
			 * because publishing PROMOTES the row to primary = its own locale, so the response
			 * would hand back en_EU where Kapture expects en_UK. translateLocale has NO "d" -
			 * that is the spelling in Kapture's own publish sample.
			 */
			payload.addProperty("primaryLocale", created.getMasterLocale());
			payload.addProperty("translateLocale", locale);
		} else {
			payload.addProperty("primaryLocale",
					defaulted(stringOf(article, "primaryLocale"), locale));
			payload.addProperty("translatedLocale",
					defaulted(stringOf(article, "translatedLocale"), locale));
		}
		payload.addProperty("articleState", created.getArticleState());
		payload.addProperty("articleLatestState", "Published");
		payload.addProperty("articleVersion", created.getArticleVersion());
		addDate(payload, "expireDate", expireDate);
		addDate(payload, "reviewDate", reviewDate);
		payload.addProperty("pubFlag", Boolean.TRUE);
		payload.addProperty("type", stringOf(article, "type"));
		payload.addProperty("title", stringOf(article, "title"));
		payload.addProperty("articleOwner", stringOf(article, "articleOwner"));
		payload.addProperty("articleCreator", stringOf(article, "articleCreator"));
		/*
		 * A7 - ORIGINAL AUTHORSHIP IS PRESERVED, NOT REPLACED WITH THE EDITOR.
		 *
		 * Kapture confirmed (2026-08-07) that the backend stores this EXACTLY AS SENT and does not
		 * overwrite it afterwards - so whoever sends the editing user here is the one destroying
		 * the original author, and for KAuthor's own updates that is KAuthor. The instruction was
		 * "send the existing values and use modifiedBy for the editor".
		 *
		 * THE READ WINS; the token user is a fallback ONLY for a document that carries no value.
		 * This was the other way round until A7 was answered, which silently replaced the author
		 * on every update and every publish.
		 */
		payload.addProperty("articleCreatorUserID", defaulted(
				stringOf(article, "articleCreatorUserID"), client.getUserPkId()));
		payload.addProperty("userEmail", client.getUserEmail());
		payload.addProperty("createdBy", createdBy);
		payload.addProperty("modifiedBy", defaulted(client.getUserDisplayName(),
				stringOf(article, "modifiedBy")));
		payload.addProperty("createdDate", createdDate);
		payload.addProperty("dateAdded", dateAdded);
		return payload.toString();
	}

	/**
	 * BUILDS THE update-article-tab PAYLOAD.
	 *
	 * NOT the display-Article response sent back. The read and the write are different contracts:
	 * display-Article returns 35 fields for rendering, while the update endpoint accepts a much
	 * SMALLER, FIXED set and rejects anything else as "Validation failed. Missing or invalid
	 * mandatory fields".
	 *
	 * TWO SHAPES, ONE DIFFERENCE. A published document additionally carries isUpdateArticle=true
	 * and publishedVersion; an unpublished one carries NEITHER - not blank versions of them, but
	 * the keys absent entirely.
	 *
	 * NAMES ARE MANDATORY, NOT DECORATION. Categories, user groups and views must each carry their
	 * DISPLAY NAME alongside the reference key. Sending the key alone is accepted by the API but
	 * KAuthor then renders the document with "undefined" wherever the name should be, so a payload
	 * that looks like it worked leaves the document visibly broken.
	 *
	 * WHERE EACH VALUE COMES FROM:
	 *   id / articleVersion / publishedVersion / articleState - k_article, never display-Article
	 *     (it has no id, and returns the version as a number so 1.0 would go back as "1")
	 *   articlelistcategory  - the SELECTED categories, with this screen's change applied
	 *   everything else      - carried over from display-Article unchanged
	 */
	private static String buildUpdatePayload(JsonObject article, KaptureArticleIdentity identity,
			String documentId, String locale, JsonArray categories, KaptureApiClient client,
			String expireDate, String reviewDate, String createdBy, String createdDate,
			String dateAdded) {
		JsonObject payload = new JsonObject();

		payload.addProperty(F_ID, identity.getId());
		payload.addProperty("primaryLocale", stringOf(article, "primaryLocale"));
		payload.addProperty("title", stringOf(article, "title"));
		payload.addProperty("articleState", identity.getArticleState());
		payload.addProperty("articleOwner", stringOf(article, "articleOwner"));
		payload.addProperty("articleCreator", stringOf(article, "articleCreator"));
		/*
		 * WHO IS WRITING, NOT WHO WROTE LAST. modifiedBy and userEmail identify the account
		 * this call is authenticated as, so the row records the change honestly instead of
		 * repeating the previous editor's name back at the server.
		 */
		payload.addProperty("modifiedBy", defaulted(client.getUserDisplayName(),
				stringOf(article, "modifiedBy")));
		payload.addProperty("userEmail", client.getUserEmail());
		/*
		 * THE CREATION AUDIT BLOCK, resolved by the caller and taken from display-Article as is.
		 *
		 * createdBy IS SENT BUT NOT PERSISTED BY THIS ENDPOINT. The same key on publish-article
		 * stores it correctly, so the value and the shape are right and update-article-tab is
		 * simply dropping it: SI1077 v1.1 and v2.1 both came back with created_by NULL while the
		 * versions publish created carry it. It is sent anyway so that the payload is correct the
		 * day the endpoint is fixed - RAISE WITH THE API TEAM, do not delete this line.
		 */
		payload.addProperty("createdBy", createdBy);
		payload.addProperty("createdDate", createdDate);
		payload.addProperty("dateAdded", dateAdded);
		payload.addProperty("articleVersion", identity.getArticleVersion());
		payload.addProperty("articleId", documentId);
		payload.addProperty("pubFlag", Boolean.FALSE);
		/*
		 * BLANK IS SENT AS JSON null, NOT AS "" - an empty string is a value and would
		 * overwrite a date set elsewhere between the read and this write.
		 */
		addDate(payload, "expireDate", expireDate);
		addDate(payload, "reviewDate", reviewDate);

		if (identity.isPublished()) {
			payload.addProperty("isUpdateArticle", Boolean.TRUE);
			payload.addProperty("publishedVersion", identity.getPublishedVersion());
		}

		/*
		 * CARRIED OVER COMPLETE, NAMES INCLUDED. display-Article returns articlelistusergroup as a
		 * SINGLE OBJECT when there is one group rather than a one-element array, so the shape is
		 * normalised - but every field inside is kept, because dropping userGroupName / viewName
		 * makes KAuthor show "undefined".
		 */
		payload.add("articlelistusergroup", asArray(article, "articlelistusergroup"));
		payload.add("articlelistview", asArray(article, "articlelistview"));

		/*
		 * A7 - ORIGINAL AUTHORSHIP IS PRESERVED, NOT REPLACED WITH THE EDITOR.
		 *
		 * Kapture confirmed (2026-08-07) that the backend stores this EXACTLY AS SENT and does not
		 * overwrite it afterwards - so whoever sends the editing user here is the one destroying
		 * the original author, and for KAuthor's own updates that is KAuthor. The instruction was
		 * "send the existing values and use modifiedBy for the editor".
		 *
		 * THE READ WINS; the token user is a fallback ONLY for a document that carries no value.
		 * This was the other way round until A7 was answered, which silently replaced the author
		 * on every update and every publish.
		 */
		payload.addProperty("articleCreatorUserID", defaulted(
				stringOf(article, "articleCreatorUserID"), client.getUserPkId()));

		String type = stringOf(article, "type");
		payload.addProperty("type", type);
		payload.addProperty("ISARTICLE", defaulted(stringOf(article, "ISARTICLE"), "KAPTURE"));
		/*
		 * ECHOED FROM display-Article, FALLING BACK TO THE LOCALE BEING PROCESSED.
		 *
		 * THE FALLBACK IS LOAD BEARING, NOT DEFENSIVE PADDING. The response OMITS
		 * translatedLocale on an UNPUBLISHED TRANSLATION - precisely the case the field exists
		 * for - so echoing it alone would send JSON null and blank the column on every
		 * translation. Where the response DOES carry it, it always equals the locale we asked
		 * for, so the echo and the fallback can never disagree.
		 */
		payload.addProperty("translatedLocale",
				defaulted(stringOf(article, "translatedLocale"), locale));
		payload.add("facetsSchemaFields", new JsonArray());
		payload.addProperty("crawlType", defaulted(stringOf(article, "crawlType"), "KAPTURE"));

		/*
		 * ESI IS CARRIED OVER UNCHANGED, minus its unselected levels. The SI VIN screens do not
		 * manage ESI categories, but the array has to be sent or the update drops the document's
		 * existing ESI mapping.
		 */
		payload.add(F_ESI_CATEGORY_ARRAY, selectedCategories(article, F_ESI_CATEGORY_ARRAY));
		payload.add(F_CATEGORY_ARRAY, categories);

		/*
		 * THE CHANNEL ATTRIBUTE BLOCK, KEYED BY THE CHANNEL NAME ("SERVICE_INFORMATION": {...}).
		 * Copied across verbatim: it holds the document's actual content, and omitting it would
		 * blank the article's attributes.
		 */
		if (null != type && !"".equals(type) && article.has(type)) {
			payload.add(type, article.get(type));
		} else {
			logger.info("buildUpdatePayload :: {" + documentId + "} has no attribute block for type {"
					+ type + "} - sending payload without it.");
		}
		return payload.toString();
	}

	/**
	 * BUILDS THE save-translate-article PAYLOAD - THE WRITE PATH FOR A TRANSLATION.
	 *
	 * IT MIRRORS KAPTURE'S OWN WORKING SAMPLE FIELD FOR FIELD, and every value except the ones
	 * below is ECHOED FROM THE READ, which returns 27 of the sample's 28 fields in the same
	 * shapes. Same rule as the update payload: send back what we were given.
	 *
	 *   id              - k_article. The read never returns it.
	 *   articleVersion  - k_article, as TEXT. The read returns a NUMBER, so 1.0 arrives as 1.
	 *   primaryLocale   - THE MASTER'S locale, from k_article.master_locale. NOT echoed:
	 *                     publishing promotes a translation to primary = its own locale, so the
	 *                     response would say en_EU where Kapture expects en_UK.
	 *   translateLocale - the locale being processed. NOTE THE SPELLING, no "d" - that is the key
	 *                     Kapture uses here AND on publish-article.
	 *   the category array - the tree with this screen's change applied.
	 *   categoriesTreeLabels - rebuilt from that tree, so the CSV cannot contradict the array.
	 *
	 * USER GROUPS ARE ECHOED WHEN PRESENT AND THE KEY IS OMITTED WHEN ABSENT - NEVER SENT EMPTY.
	 * save-translate-article DISCARDS the groups it is handed (verified 2026-08-05), so a document
	 * whose groups were lost that way reads back without them. Sending [] would then look like an
	 * instruction to clear what is still sitting in k_article_usergroup.
	 */
	private static String buildTranslationPayload(JsonObject article,
			KaptureArticleIdentity identity, String documentId, String contentId, String locale,
			JsonArray categories) {
		JsonObject payload = new JsonObject();

		for (int i = 0; i < TRANSLATE_ECHO_FIELDS.length; i++) {
			String field = TRANSLATE_ECHO_FIELDS[i];
			if (article.has(field) && !article.get(field).isJsonNull()) {
				payload.add(field, article.get(field));
			}
		}

		// THE CHANNEL ATTRIBUTE BLOCK, KEYED BY THE CHANNEL NAME - it holds the document's content
		String type = stringOf(article, "type");
		if (null != type && !"".equals(type) && article.has(type)) {
			payload.add(type, article.get(type));
		}

		if (article.has(F_USER_GROUP) && !article.get(F_USER_GROUP).isJsonNull()) {
			payload.add(F_USER_GROUP, article.get(F_USER_GROUP));
		} else {
			logger.info("buildTranslationPayload :: {" + documentId + "} / {" + locale + "} - the"
					+ " read carried no user groups, so the key is OMITTED rather than sent empty.");
		}

		payload.addProperty(F_ID, identity.getId());
		payload.addProperty("articleVersion", identity.getArticleVersion());
		payload.addProperty("primaryLocale", identity.getMasterLocale());
		payload.addProperty("translateLocale", locale);
		payload.addProperty("isAutoPublish", Boolean.FALSE);
		payload.addProperty("pubFlag", Boolean.FALSE);

		// FILLED ONLY IF THE READ DID NOT CARRY THEM - we know these independently
		if (!payload.has(F_CONTENT_ID)) {
			payload.addProperty(F_CONTENT_ID, contentId);
		}
		if (!payload.has(F_ARTICLE_ID)) {
			payload.addProperty(F_ARTICLE_ID, documentId);
		}
		if (!payload.has("DOCUMENTID")) {
			payload.addProperty("DOCUMENTID", documentId);
		}
		if (!payload.has("userLocale")) {
			payload.addProperty("userLocale", locale);
		}

		payload.add(F_CATEGORY_ARRAY, categories);
		payload.add(F_ESI_CATEGORY_ARRAY, asArray(article, F_ESI_CATEGORY_ARRAY));
		payload.addProperty("categoriesTreeLabels", labelsOf(categories));
		return payload.toString();
	}

	/**
	 * THE LABEL CSV KAPTURE'S SAMPLE CARRIES - EVERY entry's name in order, mapped or not.
	 *
	 * Rebuilt rather than echoed so that adding or removing a range cannot leave the CSV
	 * describing a tree that is no longer in the array beside it.
	 */
	private static String labelsOf(JsonArray categories) {
		StringBuffer labels = new StringBuffer();
		for (int i = 0; i < categories.size(); i++) {
			JsonElement element = categories.get(i);
			if (null == element || !element.isJsonObject()) {
				continue;
			}
			String name = stringOf(element.getAsJsonObject(), F_CATEGORY_NAME);
			if (labels.length() > 0) {
				labels.append(",");
			}
			labels.append(null == name ? "" : name);
		}
		return labels.toString();
	}

	/**
	 * ADDS VIN RANGES TO A TRANSLATION'S CATEGORY TREE.
	 *
	 * DIFFERENT FROM addCategories() IN TWO WAYS. The array here is the WHOLE TREE, so a range
	 * already in it is switched on rather than appended twice; and a genuinely new entry has to
	 * carry the fields Kapture's sample carries - locale, contentId, articleId, the parent key and
	 * the master key - which come from the k_categories parent chain rather than being invented.
	 *
	 * @return { how many were mapped, how many DO NOT EXIST in Kapture }
	 */
	private static int[] addTranslationCategories(JsonArray categories,
			ArrayList<String> referenceKeys, String locale, String documentId, String contentId,
			Connection cmsConn) {
		LinkedHashSet<String> present = refKeysIn(categories);
		int added = 0;
		int missing = 0;

		for (String referenceKey : referenceKeys) {
			if (null == referenceKey || "".equals(referenceKey.trim())) {
				continue;
			}
			String key = referenceKey.trim();
			if (present.contains(key)) {
				// ALREADY IN THE TREE - it may be sitting there unselected as an ancestor
				if (markSelected(categories, key)) {
					added++;
				}
				continue;
			}

			ArrayList<IMCategoryDetails> chain = FetchKaptureDataDAO.getCategoryHierarchy(key,
					locale, locale, cmsConn);
			if (null == chain || chain.size() <= 0) {
				logger.info("addTranslationCategories :: {" + key + "} does not exist in {" + locale
						+ "} - it cannot be mapped.");
				missing++;
				continue;
			}
			IMCategoryDetails leaf = chain.get(chain.size() - 1);
			String name = leaf.getCategoryName();
			if (null == name || "".equals(name.trim())) {
				// same fallback the category writer uses, so the two can never disagree
				name = key;
			}

			JsonObject node = new JsonObject();
			node.addProperty(F_CONTENT_ID, contentId);
			node.addProperty(F_ARTICLE_ID, documentId);
			node.addProperty(F_CATEGORY_REF_KEY, key);
			node.addProperty(F_LOCALE, locale);
			node.addProperty(F_CATEGORY_NAME, name);
			node.addProperty(F_PARENT_REF_KEY, leaf.getParentRefKey());
			// THE ROOT OF THE CHAIN IS THE MASTER KEY - VIN for MNAO, CARLINE for MC and MME
			node.addProperty(F_MASTER_CATEGORY_REF_KEY, chain.get(0).getCategoryRefKey());
			node.addProperty(F_SELECTED, Boolean.TRUE);
			categories.add(node);
			present.add(key);
			added++;
		}
		return new int[] { added, missing };
	}

	/** Switches an entry already in the tree to mapped. Returns false when it already was. */
	private static boolean markSelected(JsonArray categories, String refKey) {
		for (int i = 0; i < categories.size(); i++) {
			JsonElement element = categories.get(i);
			if (null == element || !element.isJsonObject()) {
				continue;
			}
			JsonObject entry = element.getAsJsonObject();
			if (refKey.equals(stringOf(entry, F_CATEGORY_REF_KEY))) {
				if (isSelected(entry)) {
					return false;
				}
				entry.addProperty(F_SELECTED, Boolean.TRUE);
				return true;
			}
		}
		return false;
	}

	/**
	 * THE CATEGORIES ACTUALLY MAPPED ON THE DOCUMENT - i.e. the entries flagged selected=true.
	 *
	 * display-Article returns the WHOLE TREE: the mapped leaf flagged selected=true and every
	 * ancestor above it flagged selected=false. Only the leaves are the mapping - the ancestors
	 * are context for rendering - so only they are sent back. Kapture rebuilds the ancestry itself
	 * from each leaf's own parent chain.
	 *
	 * Each entry is reduced to the three fields the write contract carries: categoryName,
	 * categoryRefKey, selected. The name is NOT optional - see buildUpdatePayload().
	 */
	private static JsonArray selectedCategories(JsonObject article, String field) {
		JsonArray out = new JsonArray();
		JsonArray source = asArray(article, field);
		for (int i = 0; i < source.size(); i++) {
			JsonElement element = source.get(i);
			if (null == element || !element.isJsonObject()) {
				continue;
			}
			JsonObject entry = element.getAsJsonObject();
			if (!isSelected(entry)) {
				continue;
			}
			String key = stringOf(entry, F_CATEGORY_REF_KEY);
			if (null == key || "".equals(key)) {
				continue;
			}
			out.add(categoryNode(key, stringOf(entry, F_CATEGORY_NAME)));
		}
		return out;
	}

	/** One category entry in the write shape. */
	private static JsonObject categoryNode(String refKey, String name) {
		JsonObject node = new JsonObject();
		node.addProperty(F_CATEGORY_NAME, null == name ? "" : name);
		node.addProperty(F_CATEGORY_REF_KEY, refKey);
		node.addProperty(F_SELECTED, Boolean.TRUE);
		return node;
	}

	private static boolean isSelected(JsonObject entry) {
		if (null == entry || !entry.has(F_SELECTED) || entry.get(F_SELECTED).isJsonNull()) {
			return false;
		}
		try {
			return entry.get(F_SELECTED).getAsBoolean();
		} catch (Exception e) {
			// a non-boolean here means the entry is not a mapping we should send back
			return false;
		}
	}

	/**
	 * Reads a field that should be an array but may arrive as a single object.
	 *
	 * display-Article collapses a one-element list into a bare object (articlelistusergroup does
	 * this whenever a document has exactly one group), and sending that through as-is is a 400.
	 * Entries are copied WHOLE - no field is dropped.
	 */
	private static JsonArray asArray(JsonObject article, String field) {
		JsonArray out = new JsonArray();
		if (null == article || !article.has(field) || article.get(field).isJsonNull()) {
			return out;
		}
		JsonElement element = article.get(field);
		if (element.isJsonArray()) {
			return element.getAsJsonArray();
		}
		if (element.isJsonObject()) {
			out.add(element);
		}
		return out;
	}

	/**
	 * Adds each VIN range to the document's category list.
	 *
	 * ONLY THE RANGE ITSELF IS ADDED, not its ancestors: the write contract carries just the
	 * mapped leaves and Kapture derives the tree from each leaf's parent chain. The hierarchy is
	 * still walked elsewhere - KaptureCategoryServiceImpl makes sure every level EXISTS for the
	 * locale before we get here - but it does not belong in this payload.
	 *
	 * The display NAME is read from k_categories rather than derived from the key, because sending
	 * a category without its name makes KAuthor render it as "undefined".
	 *
	 * @return how many categories were actually added
	 */
	/**
	 * RETURNS { how many were added, how many DO NOT EXIST in Kapture }.
	 *
	 * The second number is what tells a genuine no-op apart from a broken run. A category
	 * that could not be created earlier is simply absent from k_categories, so it used to
	 * be skipped, leave the added count at zero, and be reported to the user as
	 * "Document updated successfully" - a mapping that never happened.
	 */
	private static int[] addCategories(JsonArray categories, ArrayList<String> referenceKeys,
			String locale, Connection cmsConn) {
		LinkedHashSet<String> present = refKeysIn(categories);
		int added = 0;
		int missing = 0;

		for (String referenceKey : referenceKeys) {
			if (null == referenceKey || "".equals(referenceKey.trim())) {
				continue;
			}
			String key = referenceKey.trim();
			if (present.contains(key)) {
				// already mapped - re-mapping the same range is not a change
				continue;
			}
			IMCategoryDetails category = KaptureCategoryDAO.getCategory(key, locale, cmsConn);
			if (null == category) {
				logger.info("addCategories :: {" + key + "} does not exist in {" + locale
						+ "} - it cannot be mapped.");
				missing++;
				continue;
			}
			String name = category.getCategoryName();
			if (null == name || "".equals(name.trim())) {
				// same fallback the category writer uses, so the two can never disagree
				name = key;
			}
			categories.add(categoryNode(key, name));
			present.add(key);
			added++;
		}
		return new int[] { added, missing };
	}

	/**
	 * Removes the given VIN ranges from the document's category list.
	 *
	 * Removing a mapping is simply dropping its entry - there is no delete call, which is why
	 * deleteContent() lands here too. Ancestors are not touched: they are not in the payload at
	 * all, and one may still be shared with another mapped range.
	 *
	 * @return how many categories were actually removed
	 */
	private static int removeCategories(JsonArray categories, ArrayList<String> referenceKeys,
			JsonArray kept) {
		LinkedHashSet<String> toRemove = new LinkedHashSet<String>();
		for (String referenceKey : referenceKeys) {
			if (null != referenceKey && !"".equals(referenceKey.trim())) {
				toRemove.add(referenceKey.trim());
			}
		}
		int removed = 0;
		for (int i = 0; i < categories.size(); i++) {
			JsonElement element = categories.get(i);
			String key = null;
			if (null != element && element.isJsonObject()) {
				key = stringOf(element.getAsJsonObject(), F_CATEGORY_REF_KEY);
			}
			if (null != key && toRemove.contains(key)) {
				removed++;
				continue;
			}
			kept.add(element);
		}
		return removed;
	}

	private static LinkedHashSet<String> refKeysIn(JsonArray categories) {
		LinkedHashSet<String> keys = new LinkedHashSet<String>();
		for (int i = 0; i < categories.size(); i++) {
			JsonElement element = categories.get(i);
			if (null != element && element.isJsonObject()) {
				String key = stringOf(element.getAsJsonObject(), F_CATEGORY_REF_KEY);
				if (null != key && !"".equals(key)) {
					keys.add(key);
				}
			}
		}
		return keys;
	}

	/**
	 * A DATE FIELD, OR AN EXPLICIT JSON null WHEN THERE IS NO DATE.
	 *
	 * The field is always present - omitting it and sending "" both clear the column, and
	 * null is what KAuthor itself sends for a document with no expiry.
	 */
	/**
	 * A FAILURE THE CALLER CAN REPORT: no document id, so it still reads as a failure, but
	 * the code and message travel with it into the modification report.
	 */
	/**
	 * WHAT IS BEING TOUCHED, IN ONE READABLE PHRASE, FOR THE LOG.
	 *
	 * Reads as either
	 *     MASTER IDENTIFIER [primaryLocale=en_UK]
	 * or
	 *     TRANSLATION [primaryLocale=en_UK, translatedLocale=en_EU]
	 *
	 * so a run can be followed locale by locale, and it is obvious from the line alone which
	 * endpoint the write went to and why - the two are decided by exactly this distinction.
	 */
	private static String describe(KaptureArticleIdentity identity, String locale) {
		if (null == identity) {
			return "UNKNOWN DOCUMENT TYPE [locale=" + locale + "]";
		}
		String master = defaulted(identity.getMasterLocale(), locale);
		String effective = defaulted(identity.getEffectiveLocale(), locale);
		if (identity.isTranslation()) {
			return "TRANSLATION [primaryLocale=" + master + ", translatedLocale=" + effective + "]";
		}
		return "MASTER IDENTIFIER [primaryLocale=" + master + "]";
	}

	private static SIVinDetails failed(SIVinDetails documentDetails, String code,
			String message) {
		SIVinDetails details = (null == documentDetails) ? new SIVinDetails() : documentDetails;
		details.setDocumentId(null);
		details.setErrorCode(code);
		details.setErrorMessage(null == message ? "" : message);
		return details;
	}

	private static void addDate(JsonObject payload, String field, String value) {
		if (null == value || "".equals(value.trim())) {
			payload.add(field, JsonNull.INSTANCE);
		} else {
			payload.addProperty(field, toWriteFormat(value.trim()));
		}
	}

	/**
	 * THE ONE PLACE A VALUE FROM display-Article IS NOT ECHOED BACK VERBATIM.
	 *
	 * THE READ API EMITS A FORMAT ITS OWN WRITE API REJECTS. display-Article returns
	 * expireDate and reviewDate as MM/dd/yyyy HH:mm:ss for en_US, and sending that straight
	 * back fails the whole call:
	 *
	 *   400 Validation failed. Missing or invalid mandatory fields.
	 *   validationErrors: [{field: expireDate, message: "Expire Date must be valid for
	 *                       locale en_US."}, {field: reviewDate, ...}]
	 *
	 * The SAME payload with only those two values as yyyy-MM-dd HH:mm:ss is accepted -
	 * verified by hand against the live API on SI1123 / en_US, and by MC succeeding all
	 * along because its ja_JP documents happen to hold the hyphenated form already
	 * (SI1077: 2026-06-30 00:00:00).
	 *
	 * SCOPE IS DELIBERATELY THESE TWO FIELDS ONLY. createdDate and dateAdded went out in the
	 * SAME request, in the SAME slash format, and were NOT among the validation errors - the
	 * server accepts them either way. Everything else still goes back exactly as received;
	 * this is the documented exception, not a licence to start correcting the response.
	 *
	 * ANYTHING NOT IN THE SLASH FORM IS PASSED THROUGH UNTOUCHED, including a value already
	 * hyphenated and anything unrecognisable - the server is the authority on what it takes,
	 * and a value we cannot parse is not one we should be inventing a shape for.
	 *
	 * RAISE WITH THE API TEAM: a read endpoint whose output its own write endpoint refuses.
	 */
	private static String toWriteFormat(String value) {
		for (int i = 0; i < SLASH_DATE_FORMATS.length; i++) {
			SimpleDateFormat in = new SimpleDateFormat(SLASH_DATE_FORMATS[i]);
			in.setLenient(false);
			try {
				Date parsed = in.parse(value);
				return new SimpleDateFormat(WRITE_DATE_FORMAT).format(parsed);
			} catch (ParseException ignore) {
				// not this pattern - try the next, then give up and send it as received
			}
		}
		return value;
	}

	private static String defaulted(String value, String fallback) {
		return (null == value || "".equals(value.trim())) ? fallback : value;
	}

	private static String stringOf(JsonObject object, String field) {
		JsonElement element = object.get(field);
		if (null == element || element.isJsonNull()) {
			return null;
		}
		try {
			return element.getAsString();
		} catch (Exception e) {
			return null;
		}
	}
}
