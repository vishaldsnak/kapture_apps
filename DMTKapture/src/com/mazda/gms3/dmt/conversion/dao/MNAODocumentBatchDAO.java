package com.mazda.gms3.dmt.conversion.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

/**
 * SAVES THE DATABASE DETAILS OF MANY MNAO DOCUMENTS AT ONCE - the batch form of
 * MNAODocumentManagementDAO.saveDocumentDetails() / updateDocumentDetails().
 *
 * Same design as MMEDocumentBatchDAO; the differences are MNAO's: one set of tables per locale
 * (gms3_dmt_<locale>_*, no model type), the document row keeps DC_IM_DOC_FLAG (active / deleted)
 * apart from DC_IM_DOC_STATUS (Kapture status), the manual folder is DC_MANUAL_TYPE, the ESI codes
 * are DC_IM_DOC_CAT/SUBCAT/SUBSUBCAT_TYPE, every document has a navigation row (_nav), the VIN row
 * comes from the vin.txt (VINEntFileDetails) and the VIN attribute row from the d01.txt, and the
 * view content lives in gms3_vc_vin_details / gms3_vc_model_year_details (one locale column each).
 *
 * The documents of one Kapture bulk request are written together: one transaction, and per
 * table ONE delete (IN list) and ONE batched insert for all the documents. UPDATE statements are
 * sent one per row, never batched (MySQL Router on MC Dev).
 *
 * When a batch fails it is rolled back and the caller saves those documents one by one with the
 * per-document methods, so a failure is still reported for the document that caused it.
 */
public class MNAODocumentBatchDAO {

	private static Logger logger = LogManager.getLogger(MNAODocumentBatchDAO.class);

	/** Tables of one locale. */
	private static class Tables {
		String doc, innerLinks, esiCat, vin, vinAttr, category, masterVin, cvc, nav, displayOrder, locale;
	}

	/** The tables of one locale; locale as en-US or en_US. */
	private static Tables tables(String locale) {
		Tables t = new Tables();
		t.locale = tableLocale(locale);
		String p = "gms3_dmt_" + t.locale.toLowerCase() + "_";
		t.doc = p + "imdoc";
		t.innerLinks = p + "inrlks";
		t.esiCat = p + "esicat";
		t.vin = p + "vin";
		t.vinAttr = p + "vinattr";
		t.category = p + "cat";
		t.masterVin = p + "vinmaster";
		t.cvc = p + "cvc";
		t.nav = p + "nav";
		t.displayOrder = p + "dispord";
		return t;
	}

	// ---- lookup -------------------------------------------------------------

	/**
	 * The documents already in the database, by source path - getDocumentDetails() for many
	 * paths in one query per db.lookup.batch.size paths.
	 *
	 * @return TRIM(LOWER(path)) -> {document id, content id}; a path missing from the map is new
	 */
	public static Map<String, String[]> findExistingDocuments(List<String> filePaths, String locale) throws Exception {
		Map<String, String[]> found = new HashMap<String, String[]>();
		if (null == filePaths || filePaths.isEmpty()) {
			return found;
		}
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.lookup.batch.size"));
		Tables t = tables(locale);
		long started = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = connection();
			for (int from = 0; from < filePaths.size(); from += chunk) {
				List<String> part = filePaths.subList(from, Math.min(from + chunk, filePaths.size()));
				pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_IM_DOC_CONTENT_ID, DC_SOURCE_NETWORK_LOC FROM "
						+ t.doc + " WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN (" + marks(part.size()) + ")");
				int p = 1;
				for (String path : part) {
					pstmt.setString(p++, key(path));
				}
				rs = pstmt.executeQuery();
				while (rs.next()) {
					String k = key(rs.getString("DC_SOURCE_NETWORK_LOC"));
					// the first row wins, as in getDocumentDetails()
					if (!found.containsKey(k)) {
						found.put(k, new String[] { rs.getString("DC_IM_DOC_ID"), rs.getString("DC_IM_DOC_CONTENT_ID") });
					}
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
			}
			logger.info("findExistingDocuments :: paths=" + filePaths.size() + " already in " + t.doc + "=" + found.size()
					+ " :: " + (System.currentTimeMillis() - started) + " ms");
		} finally {
			close(rs, pstmt, conn);
		}
		return found;
	}

	/** Same normalisation as the SQL: TRIM(LOWER(path)). */
	public static String key(String path) {
		return null == path ? "" : path.trim().toLowerCase();
	}

	// ---- save new documents ----------------------------------------------------

	/**
	 * saveDocumentDetails() for many NEW documents in one transaction.
	 *
	 * @throws Exception when the batch could not be written; nothing of it is kept (rolled back)
	 */
	public static void saveNewDocuments(List<ContentDetails> docs) throws Exception {
		for (Map.Entry<String, List<ContentDetails>> e : byTables(docs).entrySet()) {
			List<ContentDetails> list = e.getValue();
			Tables t = tables(list.get(0).getLocale());
			long started = System.currentTimeMillis();
			Connection conn = null;
			try {
				conn = connection();
				conn.setAutoCommit(false);
				insertDocuments(conn, t, list);
				writeChildren(conn, t, list);
				conn.commit();
				logger.info("saveNewDocuments :: " + list.size() + " documents saved in " + t.doc + " in "
						+ (System.currentTimeMillis() - started) + " ms");
			} catch (Exception ex) {
				rollback(conn);
				Utilities.printStackTraceToLogs(MNAODocumentBatchDAO.class.getName(), "saveNewDocuments()", ex);
				throw ex;
			} finally {
				release(conn);
			}
		}
	}

	// ---- update existing documents ---------------------------------------------

	/**
	 * updateDocumentDetails() for many documents in one transaction.
	 *
	 * @param performAllOps true = a normal update (every detail rewritten), false = the inner
	 *                      links update (document row and inner links only)
	 * @throws Exception when the batch could not be written; nothing of it is kept (rolled back)
	 */
	public static void updateDocuments(List<ContentDetails> docs, boolean performAllOps) throws Exception {
		for (Map.Entry<String, List<ContentDetails>> e : byTables(docs).entrySet()) {
			Tables t = tables(e.getValue().get(0).getLocale());
			long started = System.currentTimeMillis();
			Connection conn = null;
			try {
				conn = connection();
				conn.setAutoCommit(false);
				// the document row per id - updateDocumentDetails() changes nothing for a document without one
				Map<String, Long> rowIds = documentRowIds(conn, t, e.getValue());
				List<ContentDetails> list = new ArrayList<ContentDetails>();
				for (ContentDetails cd : e.getValue()) {
					if (rowIds.containsKey(cd.getImDocumentId())) {
						list.add(cd);
					} else {
						logger.info("updateDocuments :: no row in " + t.doc + " for document " + cd.getImDocumentId()
								+ " - nothing updated, as in updateDocumentDetails()");
					}
				}
				if (performAllOps) {
					// cleanUpOldDataForDocument(): every detail of the documents goes first
					deleteByDocumentIds(conn, t.displayOrder, list);
					deleteViewContent(conn, t, list);
				}
				updateDocumentRows(conn, t, list, rowIds);
				if (performAllOps) {
					writeChildren(conn, t, list);
				} else {
					deleteByDocumentIds(conn, t.innerLinks, list);
					insertInnerLinks(conn, t.innerLinks, list);
				}
				conn.commit();
				logger.info("updateDocuments :: " + list.size() + " documents updated in " + t.doc + " (all details="
						+ performAllOps + ") in " + (System.currentTimeMillis() - started) + " ms");
			} catch (Exception ex) {
				rollback(conn);
				Utilities.printStackTraceToLogs(MNAODocumentBatchDAO.class.getName(), "updateDocuments()", ex);
				throw ex;
			} finally {
				release(conn);
			}
		}
	}

	/** Every child table of the documents: the old rows go, the new ones are written. */
	private static void writeChildren(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		deleteByDocumentIds(conn, t.nav, list);
		insertNavigation(conn, t.nav, list);
		deleteByDocumentIds(conn, t.innerLinks, list);
		insertInnerLinks(conn, t.innerLinks, list);
		deleteByDocumentIds(conn, t.esiCat, list);
		insertEsiCategories(conn, t.esiCat, list);
		deleteByDocumentIds(conn, t.category, list);
		insertCategories(conn, t.category, list);
		deleteByDocumentIds(conn, t.vin, list);
		insertVins(conn, t.vin, list);
		deleteByDocumentIds(conn, t.vinAttr, list);
		insertVinAttributes(conn, t.vinAttr, list);
		deleteByDocumentIds(conn, t.masterVin, list);
		insertMasterVins(conn, t.masterVin, list);
		deleteByDocumentIds(conn, t.cvc, list);
		insertCvc(conn, t.cvc, list);
	}

	// ---- schedule counts -------------------------------------------------------

	/** The schedule tables are market neutral: MCDocumentBatchDAO.addDocumentCounts(). */
	public static void addDocumentCounts(String scheduleId, String itemId, int processed, int failed) {
		com.mazda.gms3.dmt.mc.dao.MCDocumentBatchDAO.addDocumentCounts(scheduleId, itemId, processed, failed);
	}

	// ---- statements ------------------------------------------------------------

	private static final String DOC_COLUMNS = "DC_LOCALE_NAME,DC_SOURCE_FILE_NAME,DC_SOURCE_NETWORK_LOC,DC_FILE_NAME_ATTRIBUTE,"
			+ "DC_IM_CHANNEL_NAME,DC_IM_DOC_CONTENT_ID,DC_IM_DOC_VERSION,DC_IM_DOC_RESOURCE_PATH,DC_IM_DOC_STATUS,DC_MARKET_NAME,"
			+ "DC_MODEL_NAME,DC_MANUAL_TYPE,DC_IM_DOC_CAT_TYPE,DC_IM_DOC_SUBCAT_TYPE,DC_IM_DOC_FLAG,DC_MODEL_FOLDER_NAME,"
			+ "DC_FACELIFT_FOLDER_NAME,DC_MATERIAL_FOLDER_NAME,DC_IM_DOC_SUBSUBCAT_TYPE,DC_ESI_CATEGORY_MAPPED,"
			+ "DC_ESI_CATEGORY_MAPPED_LEVEL,DC_ESI_CATEGORY_MAPPED_REFKEY,DC_VIN_MAPPED,DC_IM_DOC_TITLE,DC_INRLNK_FOUND,"
			+ "DC_INRLNK_ALL_MAPPED,DC_INRLNK_FAILURE_REASON";
	private static final int DOC_COLUMN_COUNT = 27;

	private static void insertDocuments(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		// saveDocumentDetails() inserts the row and then sets these columns - here in one statement
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + t.doc + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
				+ DOC_COLUMNS + ",DC_CREATED_TMSTP) VALUES(" + marks(DOC_COLUMN_COUNT + 3) + ")");
		try {
			for (ContentDetails cd : list) {
				pstmt.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
				pstmt.setString(2, cd.getImDocumentId());
				setDocumentColumns(pstmt, cd, 3);
				pstmt.setTimestamp(DOC_COLUMN_COUNT + 3, now());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			pstmt.close();
		}
	}

	private static void updateDocumentRows(Connection conn, Tables t, List<ContentDetails> list, Map<String, Long> rowIds)
			throws Exception {
		if (list.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = conn.prepareStatement("UPDATE " + t.doc + " SET " + DOC_COLUMNS.replace(",", "=?,")
				+ "=?,DC_UPDATED_TMSTP=? WHERE DC_DD_ID=?");
		try {
			for (ContentDetails cd : list) {
				setDocumentColumns(pstmt, cd, 1);
				pstmt.setTimestamp(DOC_COLUMN_COUNT + 1, now());
				pstmt.setLong(DOC_COLUMN_COUNT + 2, rowIds.get(cd.getImDocumentId()));
				// one statement per row, never a batched UPDATE: MySQL Router (connection sharing) on
				// MC Dev refuses a batch sent as a multi-statement unless the session is already pinned
				pstmt.executeUpdate();
			}
		} finally {
			pstmt.close();
		}
	}

	/** The DOC_COLUMNS values, in their order, starting at parameter first. */
	private static void setDocumentColumns(PreparedStatement pstmt, ContentDetails cd, int first) throws Exception {
		int p = first;
		pstmt.setString(p++, cd.getLocale());
		pstmt.setString(p++, cd.getFileName());
		pstmt.setString(p++, cd.getFilePath());
		pstmt.setString(p++, cd.getFileNameAttribute());
		pstmt.setString(p++, cd.getChannelName());
		pstmt.setString(p++, cd.getImContentType());
		pstmt.setString(p++, cd.getImVersion());
		pstmt.setString(p++, cd.getImResourcePath());
		pstmt.setString(p++, cd.getImDocStatus());
		pstmt.setString(p++, cd.getMarket());
		pstmt.setString(p++, cd.getModel());
		pstmt.setString(p++, cd.getManualType());
		pstmt.setString(p++, cd.getCategoryCode());
		pstmt.setString(p++, cd.getSubCategoryCode());
		pstmt.setString(p++, ApplicationProperties.getProperty("flag.value.active"));
		pstmt.setString(p++, cd.getModelFolderName());
		pstmt.setString(p++, cd.getFaceLiftFolderName());
		pstmt.setString(p++, cd.getMaterialName());
		pstmt.setString(p++, cd.getSubSubCategoryCode());
		pstmt.setString(p++, cd.getEsiCategoryMapped());
		pstmt.setString(p++, cd.getEsiCategoryMappedLevel());
		pstmt.setString(p++, cd.getEsiCategoryMappedRefKey());
		pstmt.setString(p++, cd.getVinMapped());
		pstmt.setString(p++, cd.getTitle());
		pstmt.setString(p++, cd.getInnerLinkFound());
		pstmt.setString(p++, cd.getAllInnerLinksMapped());
		pstmt.setString(p, cd.getInnerLinkMappingReason());
	}

	/** DC_DD_ID per document id - the lowest one, as there is normally exactly one. */
	private static Map<String, Long> documentRowIds(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		Map<String, Long> ids = new HashMap<String, Long>();
		List<String> docIds = documentIds(list);
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.lookup.batch.size"));
		for (int from = 0; from < docIds.size(); from += chunk) {
			List<String> part = docIds.subList(from, Math.min(from + chunk, docIds.size()));
			PreparedStatement pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, MIN(DC_DD_ID) AS DC_DD_ID FROM " + t.doc
					+ " WHERE DC_IM_DOC_ID IN (" + marks(part.size()) + ") GROUP BY DC_IM_DOC_ID");
			ResultSet rs = null;
			try {
				int p = 1;
				for (String id : part) {
					pstmt.setString(p++, id);
				}
				rs = pstmt.executeQuery();
				while (rs.next()) {
					ids.put(rs.getString("DC_IM_DOC_ID"), rs.getLong("DC_DD_ID"));
				}
			} finally {
				close(rs, pstmt, null);
			}
		}
		// the match is case-insensitive in MySQL: map every document by the id it carries
		Map<String, Long> byOwnId = new HashMap<String, Long>();
		for (ContentDetails cd : list) {
			for (Map.Entry<String, Long> e : ids.entrySet()) {
				if (e.getKey().equalsIgnoreCase(cd.getImDocumentId())) {
					byOwnId.put(cd.getImDocumentId(), e.getValue());
					break;
				}
			}
		}
		return byOwnId;
	}

	private static void deleteByDocumentIds(Connection conn, String table, List<ContentDetails> list) throws Exception {
		List<String> docIds = documentIds(list);
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
		for (int from = 0; from < docIds.size(); from += chunk) {
			List<String> part = docIds.subList(from, Math.min(from + chunk, docIds.size()));
			PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + table + " WHERE DC_IM_DOC_ID IN ("
					+ marks(part.size()) + ")");
			try {
				int p = 1;
				for (String id : part) {
					pstmt.setString(p++, id);
				}
				pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		}
	}

	/** View content rows of the documents (both MNAO view content tables), as cleanUpOldDataForDocument(). */
	private static void deleteViewContent(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		List<String> docIds = new ArrayList<String>();
		for (String id : documentIds(list)) {
			docIds.add(id.trim());
		}
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
		String[][] targets = { { "gms3_vc_model_year_details", "VC_MY_LOCALE", "VC_MY_DOCUMENT_ID" },
				{ "gms3_vc_vin_details", "VC_VIN_LOCALE", "VC_VIN_DOCUMENT_ID" } };
		for (String[] vc : targets) {
			for (int from = 0; from < docIds.size(); from += chunk) {
				List<String> part = docIds.subList(from, Math.min(from + chunk, docIds.size()));
				PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + vc[0] + " WHERE " + vc[1] + " = ? AND "
						+ vc[2] + " IN (" + marks(part.size()) + ")");
				try {
					pstmt.setString(1, t.locale);
					int p = 2;
					for (String id : part) {
						pstmt.setString(p++, id);
					}
					pstmt.executeUpdate();
				} finally {
					pstmt.close();
				}
			}
		}
	}

	private static void insertNavigation(Connection conn, String table, List<ContentDetails> list) throws Exception {
		// one row per document, always - as saveDocumentDetails()
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_IM_DOC_ID,DC_IM_DOC_NEXT_LINK_FILE_NAME,"
				+ "DC_IM_DOC_NEXT_LINK_FILE_PATH,DC_IM_DOC_NEXT_IMDOC_ID,DC_IM_DOC_PREV_LINK_FILE_NAME,"
				+ "DC_IM_DOC_PREV_LINK_FILE_PATH,DC_IM_DOC_PREV_IMDOC_ID,DC_NAV_CREATED_TMSTP) VALUES(" + marks(8) + ")");
		try {
			for (ContentDetails cd : list) {
				b.ps.setString(1, cd.getImDocumentId());
				b.ps.setString(2, fileName(cd.getNextLink()));
				b.ps.setString(3, cd.getNextLink());
				b.ps.setString(4, cd.getNextLinkDocId());
				b.ps.setString(5, fileName(cd.getPreviousLink()));
				b.ps.setString(6, cd.getPreviousLink());
				b.ps.setString(7, cd.getPreviousLinkDocId());
				b.ps.setTimestamp(8, now());
				b.add();
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertInnerLinks(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_INNER_DOC_LINK_PATH,DC_INNER_IM_DOC_ID,DC_INNER_LINK_UPDATED_STATUS,DC_CREATED_TMSTP,"
				+ "DC_INNER_DOC_VTOC_NAME) VALUES(" + marks(8) + ")");
		try {
			for (ContentDetails cd : list) {
				List<Object> links = new ArrayList<Object>();
				if (null != cd.getInnerLinksList()) {
					links.addAll(cd.getInnerLinksList());
				}
				if (null != cd.getAdditionalInnerLinks()) {
					links.addAll(cd.getAdditionalInnerLinks());
				}
				for (Object o : links) {
					LinkDetails l = (LinkDetails) o;
					b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
					b.ps.setString(2, cd.getImDocumentId());
					b.ps.setString(3, cd.getFilePath());
					b.ps.setString(4, l.getInnerLinkPath());
					b.ps.setString(5, l.getInnnerLinkDocumentId());
					b.ps.setString(6, l.getMapStatus());
					b.ps.setTimestamp(7, now());
					b.ps.setString(8, l.getVtocFileName());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertEsiCategories(Connection conn, String table, List<ContentDetails> list) throws Exception {
		// one row per document, always - as the MME / MC esicat tables
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_ESICAT_CODE_LEVEL_1,DC_ESICAT_CODE_LEVEL_2,DC_ESICAT_CODE_LEVEL_3,DC_ESICAT_MAPPED_LEVEL,"
				+ "DC_ESICAT_MAPPED_REFKEY,DC_ESICAT_TXT_FILE_NAME,DC_ESICAT_TXT_FILE_PATH,DC_ESICAT_CREATED_TMSTP,"
				+ "DC_STEERING_TYPE_INFO) VALUES(" + marks(12) + ")");
		try {
			for (ContentDetails cd : list) {
				b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
				b.ps.setString(2, cd.getImDocumentId());
				b.ps.setString(3, cd.getFilePath());
				b.ps.setString(4, cd.getCategoryCode());
				b.ps.setString(5, cd.getSubCategoryCode());
				b.ps.setString(6, cd.getSubSubCategoryCode());
				b.ps.setString(7, cd.getEsiCategoryMappedLevel());
				b.ps.setString(8, cd.getEsiCategoryMappedRefKey());
				b.ps.setString(9, cd.getEsiCategoryMappedFileName());
				b.ps.setString(10, cd.getEsiCategoryMappedFilePath());
				b.ps.setTimestamp(11, now());
				b.ps.setString(12, cd.getEsiSteeringTypeInfo());
				b.add();
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertCategories(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_IM_DOC_CATEGORY_NAME,DC_IM_DOC_CATEGORY_REF_KEY,DC_CREATED_TMSTP,DC_IM_DOC_CATEGORY_TYPE)"
				+ " VALUES(" + marks(7) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getCategoryList()) {
					continue;
				}
				for (Object o : cd.getCategoryList()) {
					CategoryDetails c = (CategoryDetails) o;
					b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
					b.ps.setString(2, cd.getImDocumentId());
					b.ps.setString(3, cd.getFilePath());
					b.ps.setString(4, c.getCategoryName());
					b.ps.setString(5, c.getCategoryRefKey());
					b.ps.setTimestamp(6, now());
					b.ps.setString(7, c.getCategoryType());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertVins(Connection conn, String table, List<ContentDetails> list) throws Exception {
		// processVINENTInformation() - every entry once (the legacy method re-read the first 100 per partition)
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_IM_DOC_ID,DC_VIN_FILE_NAME,DC_VIN_MODEL_YEAR,DC_VIN_WMI,"
				+ "DC_VIN_CARLINE,DC_VIN_VDS,DC_VIN_START,DC_VIN_END,DC_VIN_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_VIN_REFKEY)"
				+ " VALUES(" + marks(11) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getVinEntDetailsList()) {
					continue;
				}
				for (Object o : cd.getVinEntDetailsList()) {
					VINEntFileDetails v = (VINEntFileDetails) o;
					b.ps.setString(1, cd.getImDocumentId());
					b.ps.setString(2, v.getFileName());
					b.ps.setString(3, v.getModelYear());
					b.ps.setString(4, v.getVinWMI());
					b.ps.setString(5, v.getVinCarline());
					b.ps.setString(6, v.getVinVDS());
					b.ps.setString(7, v.getVinStartRange());
					b.ps.setString(8, v.getVinEndRange());
					b.ps.setTimestamp(9, now());
					b.ps.setString(10, cd.getFilePath());
					b.ps.setString(11, v.getVinRefKey());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertVinAttributes(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_IM_DOC_ID,DC_VIN_ATTR_FILE_NAME,DC_VIN_ATTR_TRANS_TYPE,"
				+ "DC_VIN_ATTR_ENGINE_TYPE,DC_VIN_ATTR_BODY_TYPE,DC_VIN_ATTR_BODY_TYPE_VAL,DC_VIN_ATTR_AXLE_TYPE,"
				+ "DC_VIN_ATTR_AXLE_TYPE_VAL,DC_VIN_ATTR_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC) VALUES(" + marks(10) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getVinAttributeEntDetailsList()) {
					continue;
				}
				for (Object o : cd.getVinAttributeEntDetailsList()) {
					VinAttributeEntFileDetails v = (VinAttributeEntFileDetails) o;
					b.ps.setString(1, cd.getImDocumentId());
					b.ps.setString(2, v.getFileName());
					b.ps.setString(3, v.getVinTransType());
					b.ps.setString(4, v.getVinEngineType());
					b.ps.setString(5, v.getVinBodyType());
					b.ps.setString(6, v.getVinBodyTypeValue());
					b.ps.setString(7, v.getVinAxleType());
					b.ps.setString(8, v.getVinAxleTypeValue());
					b.ps.setTimestamp(9, now());
					b.ps.setString(10, cd.getFilePath());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertMasterVins(Connection conn, String table, List<ContentDetails> list) throws Exception {
		// processVINMasterInformation(): DC_VM_MANUAL_TYPE carries the model, DC_VM_MANUAL_TYPE_CODE the manual folder
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_IM_DOC_ID,DC_VM_MANUAL_TYPE,DC_VM_MANUAL_TYPE_CODE,"
				+ "DC_VM_CARLINE_CODE,DC_VM_WMI_CODE,DC_VM_VDS_CODE,DC_VM_VIS_START_RANGE,DC_VM_VIS_END_RANGE,"
				+ "DC_VM_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_VM_REFKEY) VALUES(" + marks(11) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getMasterVinList()) {
					continue;
				}
				for (Object o : cd.getMasterVinList()) {
					VINEntFileDetails v = (VINEntFileDetails) o;
					b.ps.setString(1, cd.getImDocumentId());
					b.ps.setString(2, cd.getModel());
					b.ps.setString(3, cd.getManualType());
					b.ps.setString(4, v.getVinCarline());
					b.ps.setString(5, v.getVinWMI());
					b.ps.setString(6, v.getVinVDS());
					b.ps.setString(7, v.getVinStartRange());
					b.ps.setString(8, v.getVinEndRange());
					b.ps.setTimestamp(9, now());
					b.ps.setString(10, cd.getFilePath());
					b.ps.setString(11, v.getVinRefKey());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertCvc(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_IM_DOC_ID,DC_CVC_CAT_CODE,DC_CVC_SYM_CODE,"
				+ "DC_CVC_SUBSYM_CODE,DC_CVC_CON_CODE,DC_CVC_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC) VALUES(" + marks(7) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getCvcCategoryList()) {
					continue;
				}
				for (Object o : cd.getCvcCategoryList()) {
					CVCCategoryDetails c = (CVCCategoryDetails) o;
					b.ps.setString(1, cd.getImDocumentId());
					b.ps.setString(2, c.getCategoryCode());
					b.ps.setString(3, c.getSymptomCode());
					b.ps.setString(4, c.getSubSymptomCode());
					b.ps.setString(5, c.getConditionCode());
					b.ps.setTimestamp(6, now());
					b.ps.setString(7, cd.getFilePath());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	// ---- helpers -----------------------------------------------------------

	/** A batched insert sent every db.write.batch.size rows (one statement each, rewriteBatchedStatements). */
	private static class Batch {
		final PreparedStatement ps;
		final int size;
		int pending = 0;

		Batch(Connection conn, String sql) throws Exception {
			ps = conn.prepareStatement(sql);
			size = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
		}

		void add() throws Exception {
			ps.addBatch();
			if (++pending >= size) {
				flush();
			}
		}

		void flush() throws Exception {
			if (pending > 0) {
				ps.executeBatch();
				pending = 0;
			}
		}

		void close() {
			try {
				ps.close();
			} catch (Exception e) {
				// nothing more to do
			}
		}
	}

	/** The documents grouped by the table set they are written to: the locale. */
	private static Map<String, List<ContentDetails>> byTables(List<ContentDetails> docs) {
		Map<String, List<ContentDetails>> m = new LinkedHashMap<String, List<ContentDetails>>();
		for (ContentDetails cd : docs) {
			String k = tableLocale(cd.getLocale()).toLowerCase();
			List<ContentDetails> l = m.get(k);
			if (null == l) {
				l = new ArrayList<ContentDetails>();
				m.put(k, l);
			}
			l.add(cd);
		}
		return m;
	}

	/** en-US -> en_US: the VC_VIN_LOCALE / VC_MY_LOCALE value, lowercased for the table names. */
	private static String tableLocale(String locale) {
		if (null == locale || "".equals(locale.trim())) {
			throw new IllegalArgumentException("no locale for the MNAO tables");
		}
		return locale.replace("-", "_").trim();
	}

	/** The file name of a backslash path ("" when none), as saveDocumentDetails() writes it. */
	private static String fileName(String path) {
		if (null == path || path.lastIndexOf("\\") == -1) {
			return "";
		}
		return path.substring(path.lastIndexOf("\\") + 1);
	}

	private static List<String> documentIds(List<ContentDetails> list) {
		List<String> ids = new ArrayList<String>();
		for (ContentDetails cd : list) {
			if (null != cd.getImDocumentId() && !ids.contains(cd.getImDocumentId())) {
				ids.add(cd.getImDocumentId());
			}
		}
		return ids;
	}

	private static String marks(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i > 0 ? ",?" : "?");
		}
		return sb.toString();
	}

	private static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}

	private static Connection connection() {
		Connection conn = DBConnectionHelper.getConnection();
		if (null == conn) {
			throw new IllegalStateException("no database connection");
		}
		return conn;
	}

	private static void rollback(Connection conn) {
		try {
			if (null != conn) {
				conn.rollback();
			}
		} catch (Exception e) {
			// nothing more to do
		}
	}

	/** Back to the pool - never in a half-open transaction. */
	private static void release(Connection conn) {
		if (null == conn) {
			return;
		}
		try {
			if (!conn.getAutoCommit()) {
				conn.rollback();
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			// nothing more to do
		}
		try {
			conn.close();
		} catch (Exception e) {
			// nothing more to do
		}
	}

	private static void close(ResultSet rs, PreparedStatement pstmt, Connection conn) {
		try {
			if (null != rs) {
				rs.close();
			}
		} catch (Exception e) {
			// nothing more to do
		}
		try {
			if (null != pstmt) {
				pstmt.close();
			}
		} catch (Exception e) {
			// nothing more to do
		}
		try {
			if (null != conn) {
				conn.close();
			}
		} catch (Exception e) {
			// nothing more to do
		}
	}
}
