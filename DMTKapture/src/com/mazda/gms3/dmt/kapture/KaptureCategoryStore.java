package com.mazda.gms3.dmt.kapture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;

/**
 * HOW DMT WRITES CATEGORIES INTO KAPTURE - ONE SWITCH FOR THE WHOLE APPLICATION.
 *
 *   kapture.category.write.mode = API   the Kapture category API (add-categoryMgt / updateCategoryByCatRef)
 *                               = DB    straight into the Kapture categories table (kapture.category.db.table),
 *                                       the way Job 8 loaded every locale; needs insert/update on that table
 *                                       for the CMS database user (jdbc.cms.datasource)
 *
 * Used by every category write of DMT: the Master Data Syncing tab and "Master Data with Content" of the MC,
 * MME and MNAO conversion jobs. DB mode writes what the API writes and nothing more:
 *   - a new category: reference key, name, description (= name), locale, parent, date added / modified - in the
 *     locale it was created for AND in every other Kapture default locale (kapture.default.locales.table) with
 *     the same name, as the API create does; a locale that already has the category is left as it is;
 *   - fillMissingLocales: a category that is there for some locales only is written into the missing default
 *     locales the same way (DB mode only - the callers use it for every category they process); a locale
 *     without the parent is skipped;
 *   - an existing category: name, description and date modified only, in its own locale only - the parent is
 *     never changed (the API cannot change it either);
 *   - the callers keep every other rule (missing parent fails, top category handling, read back from the table).
 * Rows are written in multi-row INSERT ... ON DUPLICATE KEY UPDATE statements on the unique key (reference key,
 * locale): one statement each, so the MySQL Router accepts them (no multi-statement batches).
 */
public class KaptureCategoryStore {

	private static Logger logger = LogManager.getLogger(KaptureCategoryStore.class);

	public static final String MODE_API = "API";
	public static final String MODE_DB = "DB";

	/**
	 * One category statement at a time in this application: parallel locale workers write the same reference keys
	 * in neighbouring locales, and MySQL's gap locks on the unique key then deadlock concurrent INSERT ... ON
	 * DUPLICATE KEY UPDATE statements. Reads stay parallel; a deadlock with another application is retried.
	 */
	private static final Object WRITE_LOCK = new Object();

	/** One category to write: reference key, name, parent reference key ("" = top of the tree). */
	public static class Row {
		public final String ref;
		public final String name;
		public final String parent;

		public Row(String ref, String name, String parent) {
			this.ref = ref;
			this.name = name;
			this.parent = parent;
		}
	}

	/** One row of the statement: a category in one locale. */
	private static class LocaleRow {
		final Row row;
		final String locale;

		LocaleRow(Row row, String locale) {
			this.row = row;
			this.locale = locale;
		}
	}

	/** API or DB - required key, anything else is a configuration error. */
	public static String mode() {
		String mode = KaptureApiClient.require("kapture.category.write.mode").trim().toUpperCase();
		if (!MODE_API.equals(mode) && !MODE_DB.equals(mode)) {
			throw new IllegalStateException("kapture.category.write.mode must be API or DB, is '" + mode + "'");
		}
		return mode;
	}

	public static boolean dbMode() {
		return MODE_DB.equals(mode());
	}

	/**
	 * Adds categories that are not in Kapture: in the given locale and then, with the same name, in every other Kapture
	 * default locale that does not have them yet (fillMissingLocales) - what the API create does. A row that is there
	 * already (another spelling of the reference key) is left exactly as it is - as the API answered 409 and changed nothing.
	 */
	public static void insertNew(List<Row> rows, String locale) {
		insertNew(rows, locale, null);
	}

	/** insertNew; claimed = reference keys already filled into the other locales in this job (shared by parallel workers). */
	public static void insertNew(List<Row> rows, String locale, Set<String> claimed) {
		if (null == rows || rows.isEmpty()) {
			return;
		}
		List<LocaleRow> own = new ArrayList<LocaleRow>();
		for (Row r : rows) {
			own.add(new LocaleRow(r, locale));
		}
		write(own, "referencekey = referencekey", "insertNew", locale, rows.size());
		fillMissingLocales(rows, claimed);
	}

	/**
	 * Writes categories into every Kapture default locale that does not have them yet, with the name given (the
	 * label of the locale being processed). A locale that has the category keeps it as it is. A locale that does not
	 * have the parent is skipped - no category without its parent; rows are taken in the order given, so a parent
	 * earlier in the list counts as there. claimed (may be null): a reference key in it is in every default locale
	 * already (filled by a worker of the same job) and is skipped; a key is added to it once it is written into every
	 * locale - not when a locale was skipped for its parent, so a later worker tries that locale again.
	 * @return rows written
	 */
	public static int fillMissingLocales(List<Row> rows, Set<String> claimed) {
		if (null == rows || rows.isEmpty()) {
			return 0;
		}
		if (null == claimed) {
			return fill(rows, null);
		}
		// parallel locale workers of one job: one fill at a time - the next one finds the keys done and skips them
		// (overlapping multi-row inserts of the same keys from several workers deadlock in MySQL)
		synchronized (claimed) {
			return fill(rows, claimed);
		}
	}

	private static int fill(List<Row> rows, Set<String> claimed) {
		List<Row> todo = new ArrayList<Row>();
		for (Row r : rows) {
			if (null == claimed || !claimed.contains(matchKey(r.ref))) {
				todo.add(r);
			}
		}
		if (todo.isEmpty()) {
			return 0;
		}
		List<String> locales = defaultLocales();
		if (locales.isEmpty()) {
			return 0;
		}
		Set<String> keys = new LinkedHashSet<String>();
		for (Row r : todo) {
			keys.add(r.ref);
			if (null != r.parent && !"".equals(r.parent)) {
				keys.add(r.parent);
			}
		}
		Map<String, Set<String>> present = presentLocales(new ArrayList<String>(keys));
		List<LocaleRow> add = new ArrayList<LocaleRow>();
		List<String> complete = new ArrayList<String>();
		int noParent = 0;
		for (Row r : todo) {
			int skipped = noParent;
			Set<String> have = present.get(matchKey(r.ref));
			if (null == have) {
				have = new java.util.HashSet<String>();
				present.put(matchKey(r.ref), have);
			}
			Set<String> parentHas = null == r.parent || "".equals(r.parent) ? null : present.get(matchKey(r.parent));
			for (String l : locales) {
				if (have.contains(l)) {
					continue;
				}
				if (null != r.parent && !"".equals(r.parent) && (null == parentHas || !parentHas.contains(l))) {
					noParent++;
					continue;
				}
				add.add(new LocaleRow(r, l));
				have.add(l);
			}
			if (skipped == noParent) {
				complete.add(matchKey(r.ref));
			}
		}
		if (noParent > 0) {
			logger.info("fillMissingLocales :: categories " + todo.size() + " :: locales skipped, parent not there :: " + noParent);
		}
		if (!add.isEmpty()) {
			write(add, "referencekey = referencekey", "fillMissingLocales", locales.size() + " default locales", todo.size());
		}
		if (null != claimed) {
			claimed.addAll(complete);
		}
		return add.size();
	}

	/**
	 * The matching form of a reference key: the categories table compares keys like utf8mb4_0900_ai_ci (case and
	 * width ignored), so a key with a full-width letter and the same key in normal letters are the same row.
	 */
	public static String matchKey(String refKey) {
		return null == refKey ? "" : java.text.Normalizer.normalize(refKey.trim(), java.text.Normalizer.Form.NFKC).toUpperCase(java.util.Locale.ROOT);
	}

	/** Matching key -> locales that have the category, for the given reference keys. */
	private static Map<String, Set<String>> presentLocales(List<String> refs) {
		String table = KaptureApiClient.require("kapture.category.db.table").trim();
		int chunk = KaptureApiClient.requireInt("db.write.batch.size");
		Map<String, Set<String>> present = new HashMap<String, Set<String>>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			for (int from = 0; from < refs.size(); from += chunk) {
				int to = Math.min(from + chunk, refs.size());
				StringBuilder sql = new StringBuilder("SELECT referencekey, locale FROM ").append(table).append(" WHERE referencekey IN (");
				for (int i = from; i < to; i++) {
					sql.append(i > from ? ",?" : "?");
				}
				sql.append(")");
				pstmt = conn.prepareStatement(sql.toString());
				int p = 1;
				for (int i = from; i < to; i++) {
					pstmt.setString(p++, refs.get(i));
				}
				rs = pstmt.executeQuery();
				while (rs.next()) {
					String k = matchKey(rs.getString(1));
					Set<String> s = present.get(k);
					if (null == s) {
						s = new java.util.HashSet<String>();
						present.put(k, s);
					}
					s.add(rs.getString(2));
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Reading the locales of categories from " + table + " failed: " + e.getMessage(), e);
		} finally {
			try {
				if (null != rs) {
					rs.close();
				}
				if (null != pstmt) {
					pstmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				logger.info("presentLocales :: close failed :: " + e.getMessage());
			}
		}
		return present;
	}

	/** Gives existing categories a new name in the given locale. Only name, description and date modified change. */
	public static void updateNames(List<Row> rows, String locale) {
		if (null == rows || rows.isEmpty()) {
			return;
		}
		List<LocaleRow> all = new ArrayList<LocaleRow>();
		for (Row r : rows) {
			all.add(new LocaleRow(r, locale));
		}
		write(all, "categoryname = VALUES(categoryname), categorydescription = VALUES(categorydescription), date_modified = NOW()",
				"updateNames", locale, rows.size());
	}

	/** The locales Kapture creates every category in (read on every create, so a locale added in Kapture is picked up). */
	private static List<String> defaultLocales() {
		String table = KaptureApiClient.require("kapture.default.locales.table").trim();
		List<String> locales = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			pstmt = conn.prepareStatement("SELECT DISTINCT locale FROM " + table + " WHERE locale IS NOT NULL ORDER BY locale");
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String l = rs.getString(1).trim();
				if (!"".equals(l)) {
					locales.add(l);
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Reading the Kapture default locales from " + table + " failed: " + e.getMessage(), e);
		} finally {
			try {
				if (null != rs) {
					rs.close();
				}
				if (null != pstmt) {
					pstmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				logger.info("defaultLocales :: close failed :: " + e.getMessage());
			}
		}
		if (locales.isEmpty()) {
			// Kapture itself would create the category in no other locale either - nothing to copy into
			logger.info("defaultLocales :: " + table + " is empty :: new categories are written in their own locale only");
		}
		return locales;
	}

	private static void write(List<LocaleRow> rows, String onDuplicate, String what, String forLocale, int categories) {
		String table = KaptureApiClient.require("kapture.category.db.table").trim();
		int chunk = KaptureApiClient.requireInt("db.write.batch.size");
		int deadlockRetries = KaptureApiClient.requireInt("kapture.category.db.deadlock.retries");
		// every statement takes its rows in the same order (key, locale) - writers running side by side then lock in
		// the same order and do not deadlock each other
		rows = new ArrayList<LocaleRow>(rows);
		java.util.Collections.sort(rows, new java.util.Comparator<LocaleRow>() {
			public int compare(LocaleRow a, LocaleRow b) {
				int c = matchKey(a.row.ref).compareTo(matchKey(b.row.ref));
				return 0 != c ? c : a.locale.compareTo(b.locale);
			}
		});
		long startedAt = System.currentTimeMillis();
		int affected = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBConnectionHelper.getCMSConnection();
			if (null == conn) {
				throw new IllegalStateException("no Kapture CMS connection");
			}
			for (int from = 0; from < rows.size(); from += chunk) {
				int to = Math.min(from + chunk, rows.size());
				StringBuilder sql = new StringBuilder("INSERT INTO ").append(table)
						.append(" (referencekey, categoryname, categorydescription, locale, parentrefkey, date_added, date_modified) VALUES ");
				for (int i = from; i < to; i++) {
					sql.append(i > from ? ",(?,?,?,?,?,NOW(),NOW())" : "(?,?,?,?,?,NOW(),NOW())");
				}
				sql.append(" ON DUPLICATE KEY UPDATE ").append(onDuplicate);
				pstmt = conn.prepareStatement(sql.toString());
				int p = 1;
				for (int i = from; i < to; i++) {
					LocaleRow lr = rows.get(i);
					Row r = lr.row;
					pstmt.setString(p++, r.ref);
					pstmt.setString(p++, r.name);
					pstmt.setString(p++, r.name);
					pstmt.setString(p++, lr.locale);
					if (null == r.parent || "".equals(r.parent)) {
						// a top category has no parent (NULL, as the existing roots in the table)
						pstmt.setNull(p++, java.sql.Types.VARCHAR);
					} else {
						pstmt.setString(p++, r.parent);
					}
				}
				for (int attempt = 0;; attempt++) {
					try {
						synchronized (WRITE_LOCK) {
							affected += pstmt.executeUpdate();
						}
						break;
					} catch (java.sql.SQLException e) {
						// MySQL chose this statement as the deadlock victim and rolled it back - it is safe to send again
						if (1213 != e.getErrorCode() || attempt >= deadlockRetries) {
							throw e;
						}
						logger.info(what + " :: " + forLocale + " :: deadlock, statement sent again (" + (attempt + 1) + " of " + deadlockRetries + ")");
					}
				}
				pstmt.close();
				pstmt = null;
			}
			logger.info(what + " :: " + table + " :: " + forLocale + " :: categories " + categories + " :: rows " + rows.size()
					+ " :: rows matched or written (driver count) " + affected + " :: " + (System.currentTimeMillis() - startedAt) + " ms");
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Category " + what + " into " + table + " failed for " + forLocale + ": " + e.getMessage(), e);
		} finally {
			try {
				if (null != pstmt) {
					pstmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				logger.info(what + " :: close failed :: " + e.getMessage());
			}
		}
	}
}
