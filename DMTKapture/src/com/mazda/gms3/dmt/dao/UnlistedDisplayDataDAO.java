package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;

/**
 * VIEW CONTENT AND CD DATA OF DOCUMENTS NO LONGER LISTED - the statements (all markets).
 *
 * The display order step rewrites the view content of the documents its files list, the CD step the
 * CD data of the documents its files list; a document of the same material folder that is no longer
 * listed kept its old rows. These statements find and remove them. A material folder is matched on
 * the start of DC_SOURCE_NETWORK_LOC (locale\model\manual type\facelift\material\). The tables are
 * the caller's (each market has its own).
 *
 * Every statement is a single statement (IN lists) - the MC Dev Router refuses batched UPDATE / DELETE.
 */
public class UnlistedDisplayDataDAO {

	/** One view content table: name, locale column (null when the table is of one locale), document id column. */
	public static class ViewContentTable {
		public final String table;
		public final String localeColumn;
		public final String documentColumn;

		public ViewContentTable(String table, String localeColumn, String documentColumn) {
			this.table = table;
			this.localeColumn = localeColumn;
			this.documentColumn = documentColumn;
		}
	}

	/** One document found under a material folder. */
	public static class FolderDocument {
		public String id;
		public String path;
		public String title;
		/** CD only: the CD text file(s) the old rows came from */
		public String cdFileName;
	}

	/** The ACTIVE documents of the DMT document table under the material folder (statusColumn: DC_DOC_STATUS, MNAO DC_IM_DOC_FLAG). */
	public static List<FolderDocument> documentsUnder(String documentTable, String statusColumn, String folderPrefix) throws Exception {
		List<FolderDocument> docs = new ArrayList<FolderDocument>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC, DC_IM_DOC_TITLE FROM " + documentTable
					+ " WHERE LOWER(DC_SOURCE_NETWORK_LOC) LIKE ? ESCAPE '|' AND " + statusColumn + " = ?");
			ps.setString(1, likePrefix(folderPrefix));
			ps.setString(2, ApplicationProperties.getProperty("flag.value.active"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (null == rs.getString(1) || "".equals(rs.getString(1).trim())) {
					continue;
				}
				FolderDocument d = new FolderDocument();
				d.id = rs.getString(1).trim();
				d.path = rs.getString(2);
				d.title = rs.getString(3);
				docs.add(d);
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return docs;
	}

	/** Of the document ids, the ones that have rows in any of the view content tables (in the locale). */
	public static Set<String> withViewContent(List<ViewContentTable> tables, String locale, Collection<String> ids) throws Exception {
		Set<String> found = new LinkedHashSet<String>();
		if (null == ids || ids.isEmpty()) {
			return found;
		}
		List<String> all = new ArrayList<String>(ids);
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		Connection conn = DBConnectionHelper.getConnection();
		try {
			for (ViewContentTable t : tables) {
				for (int i = 0; i < all.size(); i += chunk) {
					List<String> part = all.subList(i, Math.min(i + chunk, all.size()));
					PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT " + t.documentColumn + " FROM " + t.table + " WHERE "
							+ (null == t.localeColumn ? "" : t.localeColumn + " = ? AND ") + t.documentColumn + " IN (" + placeholders(part.size()) + ")");
					int p = 1;
					if (null != t.localeColumn) {
						ps.setString(p++, vcLocale(locale));
					}
					for (String id : part) {
						ps.setString(p++, id);
					}
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						found.add(rs.getString(1).trim());
					}
					rs.close();
					ps.close();
				}
			}
		} finally {
			conn.close();
		}
		return found;
	}

	/** Deletes the view content rows of the documents (in the locale) from every table. @return rows deleted */
	public static int deleteViewContent(List<ViewContentTable> tables, String locale, Collection<String> ids) throws Exception {
		if (null == ids || ids.isEmpty()) {
			return 0;
		}
		List<String> all = new ArrayList<String>(ids);
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		int rows = 0;
		Connection conn = DBConnectionHelper.getConnection();
		try {
			for (ViewContentTable t : tables) {
				for (int i = 0; i < all.size(); i += chunk) {
					List<String> part = all.subList(i, Math.min(i + chunk, all.size()));
					PreparedStatement ps = conn.prepareStatement("DELETE FROM " + t.table + " WHERE "
							+ (null == t.localeColumn ? "" : t.localeColumn + " = ? AND ") + t.documentColumn + " IN (" + placeholders(part.size()) + ")");
					int p = 1;
					if (null != t.localeColumn) {
						ps.setString(p++, vcLocale(locale));
					}
					for (String id : part) {
						ps.setString(p++, id);
					}
					rows += ps.executeUpdate();
					ps.close();
				}
			}
		} finally {
			conn.close();
		}
		return rows;
	}

	/** The documents that have CD data rows under the material folder (one entry per document). */
	public static List<FolderDocument> cdDocumentsUnder(String cdTable, String folderPrefix) throws Exception {
		Map<String, FolderDocument> docs = new LinkedHashMap<String, FolderDocument>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC, DC_TITLE, DC_CD_TXT_FILE_NAME FROM " + cdTable
					+ " WHERE LOWER(DC_SOURCE_NETWORK_LOC) LIKE ? ESCAPE '|'");
			ps.setString(1, likePrefix(folderPrefix));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (null == rs.getString(1) || "".equals(rs.getString(1).trim())) {
					continue;
				}
				String id = rs.getString(1).trim();
				FolderDocument d = docs.get(id);
				if (null == d) {
					d = new FolderDocument();
					d.id = id;
					d.path = rs.getString(2);
					d.title = rs.getString(3);
					docs.put(id, d);
				}
				String file = rs.getString(4);
				if (null != file && !"".equals(file.trim())) {
					if (null == d.cdFileName) {
						d.cdFileName = file.trim();
					} else if (!("," + d.cdFileName + ",").contains("," + file.trim() + ",")) {
						d.cdFileName = d.cdFileName + "," + file.trim();
					}
				}
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return new ArrayList<FolderDocument>(docs.values());
	}

	/** Deletes the CD data rows of the documents under the material folder. @return rows deleted */
	public static int deleteCDData(String cdTable, String folderPrefix, Collection<String> ids) throws Exception {
		if (null == ids || ids.isEmpty()) {
			return 0;
		}
		List<String> all = new ArrayList<String>(ids);
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		int rows = 0;
		Connection conn = DBConnectionHelper.getConnection();
		try {
			for (int i = 0; i < all.size(); i += chunk) {
				List<String> part = all.subList(i, Math.min(i + chunk, all.size()));
				PreparedStatement ps = conn.prepareStatement("DELETE FROM " + cdTable
						+ " WHERE DC_IM_DOC_ID IN (" + placeholders(part.size()) + ") AND LOWER(DC_SOURCE_NETWORK_LOC) LIKE ? ESCAPE '|'");
				int p = 1;
				for (String id : part) {
					ps.setString(p++, id);
				}
				ps.setString(p, likePrefix(folderPrefix));
				rows += ps.executeUpdate();
				ps.close();
			}
		} finally {
			conn.close();
		}
		return rows;
	}

	/** View content keeps the locale with an underscore (ja_JP, en_US). */
	private static String vcLocale(String locale) {
		return null == locale ? "" : locale.trim().replace("-", "_");
	}

	/** Lower-case LIKE pattern "prefix%" - the backslashes of the path stay literal ('|' is the escape). */
	private static String likePrefix(String folderPrefix) {
		String p = folderPrefix.trim().toLowerCase();
		if (!p.endsWith("\\")) {
			p = p + "\\";
		}
		return p.replace("|", "||").replace("%", "|%").replace("_", "|_") + "%";
	}

	private static String placeholders(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "?" : ",?");
		}
		return sb.toString();
	}
}
