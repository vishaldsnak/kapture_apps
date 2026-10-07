package com.mazda.gms3.dmt.kapture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * READ-ONLY lookups on the Kapture CMS schema (kapture_cms_db). Nothing here writes: every
 * change to Kapture goes through the REST API.
 *
 * These replace the InfoManager lookups the conversion made before it could write a document
 * (channel, view, user group and category by reference key, and the latest content record).
 */
public class KaptureLookupDAO {

	private static Logger logger = LogManager.getLogger(KaptureLookupDAO.class);

	/** The current state of a document in Kapture. */
	public static class ArticleIdentity {
		public long id;
		public String articleVersion;
		public String publishedVersion;
		/** "Published" or "Unpublished" - must be echoed on an update. */
		public String articleState;
	}

	/**
	 * contentId of a channel, by its document id prefix (SM, WD, OSM). It differs per
	 * environment, so it is always read, never configured.
	 *
	 * @return null when the channel is not defined in Kapture
	 */
	public static String getChannelContentId(String documentIdPrefix) {
		String sql = "SELECT CONTENTID FROM kapture_cms_db.k_content_type WHERE UPPER(TRIM(DOCUMENTID_PREFIX)) = ?";
		List<String> rows = queryStrings(sql, new String[] { upper(documentIdPrefix) }, "getChannelContentId()");
		return rows.isEmpty() ? null : rows.get(0);
	}

	/** Display name of a user group, by its Kapture reference key; null when unknown. */
	public static String getUserGroupName(String refKey) {
		String sql = "SELECT usergroup_role_name FROM kapture_cms_db.k_user_group_ref_name"
				+ " WHERE UPPER(TRIM(usergroup_role_ref)) = ?";
		List<String> rows = queryStrings(sql, new String[] { upper(refKey) }, "getUserGroupName()");
		return rows.isEmpty() ? null : rows.get(0);
	}

	/** Display name of a view, by its Kapture reference key; null when unknown. */
	public static String getViewName(String refKey) {
		String sql = "SELECT userview_name FROM kapture_cms_db.k_userview_ref_name"
				+ " WHERE UPPER(TRIM(userview_ref_key)) = ?";
		List<String> rows = queryStrings(sql, new String[] { upper(refKey) }, "getViewName()");
		return rows.isEmpty() ? null : rows.get(0);
	}

	/**
	 * Which of the given category reference keys exist in Kapture for the locale, with their
	 * names. A key missing from the returned map does not exist.
	 *
	 * @return reference key as stored in Kapture -> category name; keyed case-insensitively
	 *         through the UPPER-CASED reference key
	 */
	public static Map<String, String[]> getCategories(List<String> refKeys, String locale) {
		Map<String, String[]> found = new HashMap<String, String[]>();
		if (null == refKeys || refKeys.isEmpty()) {
			return found;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			// IN lists are sent in chunks so that a large material folder stays one cheap query each
			int chunk = 500;
			for (int from = 0; from < refKeys.size(); from += chunk) {
				int to = Math.min(from + chunk, refKeys.size());
				StringBuilder in = new StringBuilder();
				for (int i = from; i < to; i++) {
					in.append(i > from ? ",?" : "?");
				}
				pstmt = conn.prepareStatement("SELECT referencekey, categoryname FROM kapture_cms_db.k_categories"
						+ " WHERE locale = ? AND UPPER(referencekey) IN (" + in + ")");
				pstmt.setString(1, locale);
				for (int i = from; i < to; i++) {
					pstmt.setString(2 + (i - from), upper(refKeys.get(i)));
				}
				rs = pstmt.executeQuery();
				while (rs.next()) {
					String ref = rs.getString(1);
					if (null != ref) {
						found.put(ref.trim().toUpperCase(), new String[] { ref.trim(), rs.getString(2) });
					}
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureLookupDAO.class.getName(), "getCategories()", e);
			// A FAILED LOOKUP IS NOT "NO SUCH CATEGORY" - the caller must not treat it as one
			throw new IllegalStateException("Kapture category lookup failed: " + e.getMessage());
		} finally {
			close(rs, pstmt, conn);
		}
		return found;
	}

	/**
	 * The latest version of a document in Kapture for a locale; null when the document does
	 * not exist there.
	 *
	 * The locale predicate matters: a master and its translation are both latest_version = 'Y'
	 * under the same article_id. A master has article_translated_locale NULL.
	 */
	public static ArticleIdentity getArticleIdentity(String articleId, String locale) {
		ArticleIdentity identity = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			pstmt = conn.prepareStatement("SELECT id, article_version, published_version, article_state"
					+ " FROM kapture_cms_db.k_article WHERE article_id = ?"
					+ " AND (article_translated_locale = ? OR (article_translated_locale IS NULL"
					+ " AND article_primary_locale = ?))"
					+ " AND latest_version = 'Y' ORDER BY id DESC");
			pstmt.setString(1, articleId);
			pstmt.setString(2, locale);
			pstmt.setString(3, locale);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				identity = new ArticleIdentity();
				identity.id = rs.getLong("id");
				identity.articleVersion = rs.getString("article_version");
				identity.publishedVersion = rs.getString("published_version");
				identity.articleState = rs.getString("article_state");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureLookupDAO.class.getName(), "getArticleIdentity()", e);
			// never guess a state - the caller fails the document instead
			throw new IllegalStateException("Kapture article lookup failed: " + e.getMessage());
		} finally {
			close(rs, pstmt, conn);
		}
		return identity;
	}

	/**
	 * published_version holds a version - not empty and not 'NONE', which Kapture writes there when
	 * nothing is published (k_article, seen on MC Dev after an unpublish).
	 */
	public static boolean hasPublishedVersion(String publishedVersion) {
		return null != publishedVersion && !"".equals(publishedVersion.trim()) && !NO_VERSION.equalsIgnoreCase(publishedVersion.trim());
	}

	/** what Kapture writes into published_version when nothing is published */
	public static final String NO_VERSION = "NONE";

	/** Key of getArticleIdentities(): document id + locale. */
	public static String identityKey(String articleId, String locale) {
		// upper-cased: the IN match is case-insensitive, so the key must be as well
		return (null == articleId ? "" : articleId.trim().toUpperCase()) + "|"
				+ (null == locale ? "" : locale.trim().toUpperCase());
	}

	/**
	 * getArticleIdentity() for every document of a bulk update, in one query per locale and
	 * db.lookup.batch.size documents instead of one query per document.
	 *
	 * @return identityKey(articleId, locale) -> identity; a document missing from the map is not
	 *         present in Kapture
	 */
	public static Map<String, ArticleIdentity> getArticleIdentities(List<KaptureContentService.BatchItem> items) {
		Map<String, List<String>> idsByLocale = new HashMap<String, List<String>>();
		for (KaptureContentService.BatchItem it : items) {
			List<String> ids = idsByLocale.get(it.article.locale);
			if (null == ids) {
				ids = new ArrayList<String>();
				idsByLocale.put(it.article.locale, ids);
			}
			if (null != it.article.documentId && !ids.contains(it.article.documentId)) {
				ids.add(it.article.documentId);
			}
		}
		return getArticleIdentities(idsByLocale);
	}

	/**
	 * The same for plain document ids.
	 *
	 * @param idsByLocale Kapture locale -> document ids
	 */
	public static Map<String, ArticleIdentity> getArticleIdentities(Map<String, List<String>> idsByLocale) {
		Map<String, ArticleIdentity> found = new HashMap<String, ArticleIdentity>();
		int chunk = KaptureApiClient.requireInt("db.lookup.batch.size");
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			for (Map.Entry<String, List<String>> e : idsByLocale.entrySet()) {
				List<String> ids = e.getValue();
				for (int from = 0; from < ids.size(); from += chunk) {
					List<String> part = ids.subList(from, Math.min(from + chunk, ids.size()));
					StringBuilder in = new StringBuilder();
					for (int i = 0; i < part.size(); i++) {
						in.append(i > 0 ? ",?" : "?");
					}
					// ORDER BY id DESC: the first row seen per document is the one getArticleIdentity() takes
					pstmt = conn.prepareStatement("SELECT article_id, id, article_version, published_version, article_state"
							+ " FROM kapture_cms_db.k_article WHERE article_id IN (" + in + ")"
							+ " AND (article_translated_locale = ? OR (article_translated_locale IS NULL"
							+ " AND article_primary_locale = ?))"
							+ " AND latest_version = 'Y' ORDER BY id DESC");
					int p = 1;
					for (String id : part) {
						pstmt.setString(p++, id);
					}
					pstmt.setString(p++, e.getKey());
					pstmt.setString(p, e.getKey());
					rs = pstmt.executeQuery();
					while (rs.next()) {
						String key = identityKey(rs.getString("article_id"), e.getKey());
						if (found.containsKey(key)) {
							continue;
						}
						ArticleIdentity identity = new ArticleIdentity();
						identity.id = rs.getLong("id");
						identity.articleVersion = rs.getString("article_version");
						identity.publishedVersion = rs.getString("published_version");
						identity.articleState = rs.getString("article_state");
						found.put(key, identity);
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureLookupDAO.class.getName(), "getArticleIdentities()", e);
			// never guess a state - the caller fails the documents instead
			throw new IllegalStateException("Kapture article lookup failed: " + e.getMessage());
		} finally {
			close(rs, pstmt, conn);
		}
		return found;
	}

	private static List<String> queryStrings(String sql, String[] binds, String method) {
		List<String> rows = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			pstmt = conn.prepareStatement(sql);
			for (int i = 0; i < binds.length; i++) {
				pstmt.setString(i + 1, binds[i]);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				rows.add(rs.getString(1));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureLookupDAO.class.getName(), method, e);
			throw new IllegalStateException("Kapture lookup failed in " + method + ": " + e.getMessage());
		} finally {
			close(rs, pstmt, conn);
		}
		if (rows.isEmpty()) {
			logger.info(method + " :: no row for " + java.util.Arrays.asList(binds));
		}
		return rows;
	}

	private static String upper(String s) {
		return null == s ? "" : s.trim().toUpperCase();
	}

	private static void close(ResultSet rs, PreparedStatement pstmt, Connection conn) {
		try { if (null != rs) rs.close(); } catch (Exception e) { }
		try { if (null != pstmt) pstmt.close(); } catch (Exception e) { }
		try { if (null != conn) conn.close(); } catch (Exception e) { }
	}
}
