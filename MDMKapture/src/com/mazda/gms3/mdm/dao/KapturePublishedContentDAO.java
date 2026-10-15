package com.mazda.gms3.mdm.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.KaptureContentXmlReader;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * FINDS THE PUBLISHED CONTENT XML FOR A DOCUMENT - the shared half of "read a document off disk".
 *
 * WHY THIS IS SHARED. Two screens have to do exactly the same thing and must not disagree about
 * it: CD Creation wants the CHANNEL NODE out of the file, RMI Export wants the WHOLE FILE, but
 * both first have to answer the same two questions - which version is published, and where is its
 * XML. Both used to get this for free from a single system: CD Creation joined
 * OK_IM.CONTENTTEXT/CONTENTDATA, RMI walked an InfoManager folder tree that had no version level
 * at all. Kapture splits it, so the answer is now a database read plus a path.
 *
 *   1. kapture_cms_db.k_article  - WHICH VERSION. One row per version; the newest PUBLISHED one.
 *   2. the generated XML on disk  - WHERE. Laid out by KaptureContentXmlReader, which owns the
 *                                   folder convention.
 *
 * Callers take it from there:
 *   - findContentXmlFile()  -> the File, for anything that wants the document as Kapture wrote it
 *   - findContentFolder()   -> the folder, for a caller that will open a reader on it
 *
 * LIVE, NOT STAGING - checked against the data, not assumed. Across every sample document live
 * holds only x.0 folders and staging only x.y with y>=1, so a PUBLISHED version is always an x.0
 * that exists only under live; staging holds the working drafts between publishes. Reading a
 * published document out of staging would either find nothing or export an unreviewed draft.
 * Staging is still tried as a FALLBACK, because a version folder under live can exist while its
 * file has not been written there (SM1027 1.0 in the sample data is an empty folder), and using
 * the right content from the wrong tree beats dropping the document silently.
 */
public class KapturePublishedContentDAO extends DBConnectionHelper {

	static Logger logger = LogManager.getLogger(KapturePublishedContentDAO.class);

	private static final String T_ARTICLE = "kapture_cms_db.k_article";

	/** k_article.article_state for a published version. */
	private static final String STATE_PUBLISHED = "Published";

	/**
	 * THE ONE ROW FOR ONE LOCALE. Carried over verbatim from FetchKaptureDataDAO - the locale a row
	 * belongs to is COALESCE(article_translated_locale, article_primary_locale), because a master
	 * minted by update-article-tab leaves article_translated_locale NULL while a translation draft
	 * carries the MASTER's primary locale. Bind the locale TWICE.
	 */
	private static final String LOCALE_PREDICATE =
			" AND (article_translated_locale = ?"
			+ " OR (article_translated_locale IS NULL AND article_primary_locale = ?))";

	/**
	 * THE MASTER IDENTIFIER, NOT A TRANSLATION OF ONE.
	 *
	 * master_locale IS THE ONLY THING THAT CAN TELL THEM APART ONCE PUBLISHED, which is why this is
	 * written as it is rather than by comparing the primary and translated columns. PUBLISHING
	 * PROMOTES A TRANSLATION: as a draft it carries the MASTER's primary locale, but publishing
	 * rewrites primary to the translation's OWN locale - so a published translation has
	 * primary = translated = its own locale and is, on those two columns alone, indistinguishable
	 * from a master. Since only PUBLISHED versions are read here, that is the state always met.
	 *
	 * THE COALESCE ON master_locale IS FOR MASTERS, NOT TRANSLATIONS. A master leaves the column
	 * NULL, so it falls back to its own primary locale and matches itself; a translation always
	 * carries a value. Do NOT "simplify" this to a plain master_locale comparison - every ordinary
	 * master would fail the test and no document would be found at all.
	 */
	private static final String MASTER_PREDICATE =
			" AND COALESCE(article_translated_locale, article_primary_locale)"
			+ " = COALESCE(master_locale, article_primary_locale)";

	/**
	 * One published version of one document.
	 *
	 * THE LOCALE IS PART OF THE ANSWER, NOT JUST THE QUESTION. Asked without a locale, the row is
	 * chosen on article id and state alone - but the locale it belongs to comes back with it,
	 * because locale is a mandatory folder level and the path cannot be built without it. It is
	 * never guessed.
	 */
	public static class PublishedVersion {
		private String recordId = "";
		private String version = "";
		private String locale = "";

		/** k_article.id - what the old code knew as CONTENTTEXT.RECORDID. */
		public String getRecordId() {
			return recordId;
		}

		public String getVersion() {
			return version;
		}

		public String getLocale() {
			return locale;
		}

		public boolean isUsable() {
			return null != version && !"".equals(version) && null != locale && !"".equals(locale);
		}
	}

	/**
	 * THE PUBLISHED CONTENT XML FOR A DOCUMENT.
	 *
	 * @param documentId article id, e.g. "SM1027"
	 * @param locale     the locale wanted, or null/blank to take the newest published version in
	 *                   ANY locale
	 * @return the file, or null when the document has no published version or no XML on disk
	 */
	public static File findContentXmlFile(String documentId, String locale) {
		String folder = findContentFolder(documentId, locale);
		return null == folder ? null : findContentXml(new File(folder));
	}

	/** As findContentXmlFile(), but stopping at the folder - for callers that open a reader on it. */
	public static String findContentFolder(String documentId, String locale) {
		if (null == documentId || "".equals(documentId.trim())) {
			logger.info("findContentFolder :: document id as parameter is null.");
			return null;
		}
		String docId = documentId.trim();
		PublishedVersion version = findLatestPublished(docId, locale);
		if (null == version) {
			logger.info("findContentFolder :: no PUBLISHED version in k_article for {" + docId
					+ "} / {" + locale + "}.");
			return null;
		}
		String folder = resolveContentFolder(docId, version.getLocale(), version.getVersion());
		if (null == folder) {
			logger.info("findContentFolder :: no content XML on disk for {" + docId + "} / {"
					+ version.getLocale() + "} / version {" + version.getVersion() + "}.");
		}
		return folder;
	}

	/**
	 * THE HIGHEST PUBLISHED VERSION OF A DOCUMENT.
	 *
	 * A document keeps one k_article row per version, so the same article id has several rows with
	 * article_state='Published' - 1.0, 2.0, 3.0 - and the newest is wanted.
	 *
	 * VERSIONS ARE ORDERED NUMERICALLY IN JAVA, NOT AS STRINGS IN SQL. "10.0" sorts BEFORE "9.0"
	 * lexically, so a string ORDER BY would quietly return an old version once a document passes
	 * its tenth publish. Doing it here also keeps the SQL free of MySQL-only string functions.
	 *
	 * THE MASTER FILTER IS PREFERRED, NOT ENFORCED. Every document these screens read is a master
	 * identifier for its locale - confirmed against the source data, and it follows from how the
	 * content was imported, one master per locale rather than one master with translations hanging
	 * off it. It is not made a hard requirement because the failure mode of enforcing it is the
	 * worse one: a document shaped differently would vanish from the export with nothing but a
	 * count to show for it. The fallback still yields the right content, and it is logged loudly
	 * because it means the data is not shaped as expected.
	 *
	 * WITHOUT A LOCALE, THE WINNER MAY COME FROM ANY LOCALE, and ties break on the highest
	 * k_article id - deterministic, where the queries this replaced were merely arbitrary.
	 */
	public static PublishedVersion findLatestPublished(String documentId, String locale) {
		Connection conn = null;
		try {
			conn = getCMSConnection();
			if (null == conn) {
				logger.info("findLatestPublished :: no CMS connection - cannot resolve {" + documentId
						+ "}.");
				return null;
			}
			PublishedVersion best = pickHighest(readPublishedVersions(documentId, locale, true, conn));
			if (null != best) {
				return best;
			}
			best = pickHighest(readPublishedVersions(documentId, locale, false, conn));
			if (null != best) {
				logger.info("findLatestPublished :: {" + documentId + "} / {" + locale + "} has NO"
						+ " published MASTER row - falling back to a translation row, locale {"
						+ best.getLocale() + "} version {" + best.getVersion() + "}.");
			}
			return best;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KapturePublishedContentDAO.class.getName(),
					"findLatestPublished()", e);
			return null;
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

	/** Every published row of a document, optionally restricted to a locale and to masters. */
	private static List<PublishedVersion> readPublishedVersions(String documentId, String locale,
			boolean mastersOnly, Connection conn) {
		List<PublishedVersion> rows = new ArrayList<PublishedVersion>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean useLocale = (null != locale && !"".equals(locale.trim()));
		try {
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT id, article_version, published_version,");
			sql.append(" COALESCE(article_translated_locale, article_primary_locale)");
			sql.append(" AS effective_locale FROM ").append(T_ARTICLE);
			sql.append(" WHERE article_id = ? AND article_state = ?");
			if (useLocale) {
				sql.append(LOCALE_PREDICATE);
			}
			if (mastersOnly) {
				sql.append(MASTER_PREDICATE);
			}
			sql.append(" ORDER BY id DESC");

			pstmt = conn.prepareStatement(sql.toString());
			int index = 1;
			pstmt.setString(index++, documentId.trim());
			pstmt.setString(index++, STATE_PUBLISHED);
			if (useLocale) {
				String normalised = locale.replace("-", "_").trim();
				pstmt.setString(index++, normalised);
				pstmt.setString(index++, normalised);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				PublishedVersion row = new PublishedVersion();
				row.recordId = safeTrim(rs.getString("id"));
				row.locale = safeTrim(rs.getString("effective_locale"));
				/*
				 * A PUBLISHED ROW IS ADDRESSED BY ITS published_version. That column is sometimes
				 * literally the string "NaN" or null in this data, and article_version is the
				 * WORKING version - so it is a last resort, never a preference.
				 */
				String published = safeTrim(rs.getString("published_version"));
				if ("".equals(published) || "NaN".equalsIgnoreCase(published)) {
					published = safeTrim(rs.getString("article_version"));
				}
				row.version = published;
				if (row.isUsable()) {
					rows.add(row);
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KapturePublishedContentDAO.class.getName(),
					"readPublishedVersions()", e);
		} finally {
			closeQuietly(rs, pstmt);
		}
		return rows;
	}

	/** The row with the numerically highest version; ties break on the later k_article id. */
	private static PublishedVersion pickHighest(List<PublishedVersion> rows) {
		PublishedVersion best = null;
		if (null == rows) {
			return null;
		}
		for (int i = 0; i < rows.size(); i++) {
			PublishedVersion candidate = rows.get(i);
			if (null == best || compareVersions(candidate.getVersion(), best.getVersion()) > 0) {
				best = candidate;
			}
		}
		return best;
	}

	/**
	 * Compares two dotted version strings NUMERICALLY, segment by segment - so 10.0 &gt; 9.0, which
	 * a string comparison gets wrong. A non-numeric segment counts as 0 rather than throwing.
	 */
	public static int compareVersions(String left, String right) {
		String[] l = (null == left ? "" : left.trim()).split("\\.");
		String[] r = (null == right ? "" : right.trim()).split("\\.");
		int length = Math.max(l.length, r.length);
		for (int i = 0; i < length; i++) {
			long lv = segment(l, i);
			long rv = segment(r, i);
			if (lv != rv) {
				return lv > rv ? 1 : -1;
			}
		}
		return 0;
	}

	private static long segment(String[] parts, int index) {
		if (null == parts || index >= parts.length) {
			return 0L;
		}
		try {
			String digits = parts[index].replaceAll("[^0-9]", "");
			return "".equals(digits) ? 0L : Long.parseLong(digits);
		} catch (Exception e) {
			return 0L;
		}
	}

	/**
	 * FINDS THE FOLDER THAT ACTUALLY HOLDS THE XML, live first and staging second - see the class
	 * comment for why that order and not the other one.
	 *
	 * The last resort SCANS the whole channel for the document. KaptureContentXmlReader.bucketFor()
	 * already finds the right BUCKET by searching, so this is not about the bucket - it covers the
	 * case where the bucket is right but the VERSION folder is not, and takes the highest version
	 * folder that really contains a content_*.xml. Costs one directory listing, only on the miss
	 * path.
	 */
	public static String resolveContentFolder(String documentId, String locale, String version) {
		String path = KaptureContentXmlReader.buildFolderPath(documentId, locale, version, true);
		if (hasContentXml(path)) {
			return path;
		}
		String staging = KaptureContentXmlReader.buildFolderPath(documentId, locale, version, false);
		if (hasContentXml(staging)) {
			logger.info("resolveContentFolder :: {" + documentId + "} version {" + version
					+ "} was not under live - reading it from staging {" + staging + "}.");
			return staging;
		}
		String scanned = scanForDocumentFolder(documentId, locale, true);
		if (null == scanned) {
			scanned = scanForDocumentFolder(documentId, locale, false);
		}
		if (null != scanned) {
			logger.info("resolveContentFolder :: {" + documentId + "} not found at the expected"
					+ " version {" + version + "} - using the highest version folder present, {"
					+ scanned + "}.");
		}
		return scanned;
	}

	/** Searches every bucket under the channel for the document, then takes its newest version. */
	private static String scanForDocumentFolder(String documentId, String locale, boolean live) {
		try {
			String root = ApplicationProperties.getProperty("kapture.content.xml.root");
			String channel = KaptureContentXmlReader.getChannel(documentId);
			if (null == root || "".equals(root.trim()) || null == channel || null == locale
					|| "".equals(locale.trim())) {
				return null;
			}
			File channelFolder = new File(new File(root.trim(), live ? "live" : "staging"), channel);
			File[] buckets = channelFolder.listFiles();
			if (null == buckets) {
				return null;
			}
			for (int i = 0; i < buckets.length; i++) {
				if (!buckets[i].isDirectory()) {
					continue;
				}
				File localeFolder = new File(new File(buckets[i], documentId.trim()),
						locale.trim().replace("-", "_"));
				String newest = newestVersionFolder(localeFolder);
				if (null != newest) {
					return newest;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KapturePublishedContentDAO.class.getName(),
					"scanForDocumentFolder()", e);
		}
		return null;
	}

	/** The numerically highest version folder under a locale that CONTAINS a content_*.xml. */
	private static String newestVersionFolder(File localeFolder) {
		if (null == localeFolder || !localeFolder.isDirectory()) {
			return null;
		}
		File[] versions = localeFolder.listFiles();
		if (null == versions) {
			return null;
		}
		File best = null;
		for (int i = 0; i < versions.length; i++) {
			if (!versions[i].isDirectory() || !hasContentXml(versions[i].getAbsolutePath())) {
				continue;
			}
			if (null == best || compareVersions(versions[i].getName(), best.getName()) > 0) {
				best = versions[i];
			}
		}
		return null == best ? null : best.getAbsolutePath();
	}

	/** True when the folder exists and holds a content_*.xml - an empty version folder is a miss. */
	private static boolean hasContentXml(String path) {
		if (null == path || "".equals(path.trim())) {
			return false;
		}
		return null != findContentXml(new File(path));
	}

	/**
	 * The content XML inside a version folder.
	 *
	 * MATCHED BY PATTERN, NOT BY NAME: the file name carries the CHANNEL's contentid, not the
	 * document's, so every SM document's file has the same name. The FOLDER identifies the
	 * document.
	 */
	private static File findContentXml(File folder) {
		if (null == folder || !folder.isDirectory()) {
			return null;
		}
		File[] files = folder.listFiles();
		if (null == files) {
			return null;
		}
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			if (files[i].isFile() && null != name && name.toLowerCase().startsWith("content_")
					&& name.toLowerCase().endsWith(".xml")) {
				return files[i];
			}
		}
		return null;
	}

	private static String safeTrim(String value) {
		return null == value ? "" : value.trim();
	}

	private static void closeQuietly(ResultSet rs, PreparedStatement pstmt) {
		if (null != rs) {
			try {
				rs.close();
			} catch (Exception ignore) {
				// nothing useful to do here
			}
		}
		if (null != pstmt) {
			try {
				pstmt.close();
			} catch (Exception ignore) {
				// nothing useful to do here
			}
		}
	}
}
