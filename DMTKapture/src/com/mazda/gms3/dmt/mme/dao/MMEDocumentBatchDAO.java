package com.mazda.gms3.dmt.mme.dao;

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
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;

/**
 * SAVES THE DATABASE DETAILS OF MANY MME DOCUMENTS AT ONCE - the batch form of
 * MMEDocumentManagementDAO.saveDocumentDetails() / updateDocumentDetails().
 *
 * Same design as MCDocumentBatchDAO; the differences are MME's: one set of tables per locale
 * (gms3_dmt_<locale>_[nm_]*, view content gms3_vc_mme_vin_dtl_<locale>), the master VIN table is
 * "vinmast", the ESI row carries DC_STEERING_TYPE_INFO and the VIN row DC_VIN_WMI_CODE.
 *
 * The documents of one Kapture bulk request are written together: one transaction, and per
 * table ONE delete (IN list) and ONE batched insert for all the documents, instead of a dozen
 * statements per document. Every statement is a network round trip, which on the MC Dev cluster
 * costs about 250 ms.
 *
 * The rows written are exactly the ones the per-document methods write (same tables, columns
 * and values). When a batch fails it is rolled back and the caller saves those documents one by
 * one with the per-document methods, so a failure is still reported for the document that
 * caused it.
 */
public class MMEDocumentBatchDAO {

	private static Logger logger = LogManager.getLogger(MMEDocumentBatchDAO.class);

	/** Tables of one model type. */
	private static class Tables {
		String doc, innerLinks, esiCat, vin, category, masterVin, cvc, displayOrder, cdData, viewContent, locale;
	}

	/** The tables of one locale + model type; locale as en-UK or en_UK. */
	private static Tables tables(String locale, String modelType) {
		Tables t = new Tables();
		boolean newModel = null != modelType && modelType.trim().toLowerCase()
				.equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase());
		t.locale = tableLocale(locale);
		String p = "gms3_dmt_" + t.locale.toLowerCase() + "_" + (newModel ? "nm_" : "");
		t.viewContent = "gms3_vc_mme_vin_dtl_" + t.locale.toLowerCase();
		t.doc = p + "imdoc";
		t.innerLinks = p + "inrlks";
		t.esiCat = p + "esicat";
		t.vin = p + "vin";
		t.category = p + "cat";
		t.masterVin = p + "vinmast";
		t.cvc = p + "cvc";
		t.displayOrder = p + "dispord";
		t.cdData = p + "cd_data";
		return t;
	}

	// ---- lookup -------------------------------------------------------------

	/**
	 * The documents already in the database, by source path - getDocumentDetails() for many
	 * paths in one query per db.lookup.batch.size paths.
	 *
	 * @return TRIM(LOWER(path)) -> {document id, content id}; a path missing from the map is new
	 */
	public static Map<String, String[]> findExistingDocuments(List<String> filePaths, String modelType, String locale) throws Exception {
		Map<String, String[]> found = new HashMap<String, String[]>();
		if (null == filePaths || filePaths.isEmpty()) {
			return found;
		}
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.lookup.batch.size"));
		Tables t = tables(locale, modelType);
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
			Tables t = tables(list.get(0).getLocale(), list.get(0).getModelType());
			long started = System.currentTimeMillis();
			Connection conn = null;
			try {
				conn = connection();
				conn.setAutoCommit(false);
				insertDocuments(conn, t, list);

				// the children, as saveDocumentDetails() writes them
				List<ContentDetails> withInnerLinks = new ArrayList<ContentDetails>();
				for (ContentDetails cd : list) {
					if (null != cd.getInnerLinksList() && cd.getInnerLinksList().size() > 0) {
						withInnerLinks.add(cd);
					}
				}
				deleteByDocumentIds(conn, t.innerLinks, withInnerLinks);
				insertInnerLinks(conn, t.innerLinks, withInnerLinks);
				deleteByDocumentIds(conn, t.esiCat, list);
				insertEsiCategories(conn, t.esiCat, list);
				deleteByDocumentIds(conn, t.category, list);
				insertCategories(conn, t.category, list);
				deleteByDocumentIds(conn, t.vin, list);
				insertVins(conn, t.vin, list);
				deleteByDocumentIds(conn, t.masterVin, list);
				insertMasterVins(conn, t.masterVin, list);
				deleteByDocumentIds(conn, t.cvc, list);
				insertCvc(conn, t.cvc, list);
				conn.commit();
				logger.info("saveNewDocuments :: " + list.size() + " documents saved in " + t.doc + " in "
						+ (System.currentTimeMillis() - started) + " ms");
			} catch (Exception ex) {
				rollback(conn);
				Utilities.printStackTraceToLogs(MMEDocumentBatchDAO.class.getName(), "saveNewDocuments()", ex);
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
			Tables t = tables(e.getValue().get(0).getLocale(), e.getValue().get(0).getModelType());
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
					deleteByDocumentIds(conn, t.innerLinks, list);
					deleteByDocumentIds(conn, t.category, list);
					deleteByDocumentIds(conn, t.esiCat, list);
					deleteByDocumentIds(conn, t.vin, list);
					deleteByDocumentIds(conn, t.masterVin, list);
					deleteByDocumentIds(conn, t.displayOrder, list);
					deleteByDocumentIds(conn, t.cvc, list);
					deleteByDocumentIds(conn, t.cdData, list);
					deleteViewContent(conn, t, list);
				}
				updateDocumentRows(conn, t, list, rowIds);
				if (!performAllOps) {
					deleteByDocumentIds(conn, t.innerLinks, list);
				}
				insertInnerLinks(conn, t.innerLinks, list);
				if (performAllOps) {
					insertEsiCategories(conn, t.esiCat, list);
					insertCategories(conn, t.category, list);
					insertVins(conn, t.vin, list);
					insertMasterVins(conn, t.masterVin, list);
					insertCvc(conn, t.cvc, list);
				}
				conn.commit();
				logger.info("updateDocuments :: " + list.size() + " documents updated in " + t.doc + " (all details="
						+ performAllOps + ") in " + (System.currentTimeMillis() - started) + " ms");
			} catch (Exception ex) {
				rollback(conn);
				Utilities.printStackTraceToLogs(MMEDocumentBatchDAO.class.getName(), "updateDocuments()", ex);
				throw ex;
			} finally {
				release(conn);
			}
		}
	}

	// ---- schedule counts -------------------------------------------------------

	/** The schedule tables are market neutral: MCDocumentBatchDAO.addDocumentCounts(). */
	public static void addDocumentCounts(String scheduleId, String itemId, int processed, int failed) {
		com.mazda.gms3.dmt.mc.dao.MCDocumentBatchDAO.addDocumentCounts(scheduleId, itemId, processed, failed);
	}

	// ---- statements ------------------------------------------------------------

	private static void insertDocuments(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		// saveDocumentDetails() inserts the row and then sets these columns - here in one statement
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + t.doc + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
				+ "DC_IM_CHANNEL_NAME,DC_IM_DOC_CONTENT_ID,DC_IM_DOC_VERSION,DC_IM_DOC_RESOURCE_PATH,DC_IM_DOC_PUBLISHED_STATUS,"
				+ "DC_MARKET_NAME,DC_LOCALE_NAME,DC_MODEL_NAME,DC_MODEL_TYPE,DC_CARLINE_CODE,DC_MODEL_FOLDER_NAME,"
				+ "DC_MANUAL_TYPE_FOLDER_NAME,DC_FACELIFT_FOLDER_NAME,DC_MATERIAL_FOLDER_NAME,DC_SOURCE_FILE_NAME,"
				+ "DC_SOURCE_NETWORK_LOC,DC_DOC_STATUS,DC_ESI_CATEGORY_MAPPED,DC_ESI_CATEGORY_MAPPED_LEVEL,"
				+ "DC_ESI_CATEGORY_MAPPED_REFKEY,DC_ESI_MAPPED_FILE_NAME,DC_VIN_MAPPED,DC_VIN_MAPPED_FILE_NAME,"
				+ "DC_DISPLAY_ORDER_MAPPED,DC_DISPORD_MAPPED_FILE_NAME,DC_IM_DOC_TITLE,DC_INRLNK_FOUND,"
				+ "DC_INRLNK_ALL_MAPPED,DC_INRLNK_FAILURE_REASON,DC_CREATED_TMSTP) VALUES(" + marks(32) + ")");
		try {
			for (ContentDetails cd : list) {
				pstmt.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
				pstmt.setString(2, cd.getImDocumentId());
				setDocumentColumns(pstmt, cd, 3);
				pstmt.setTimestamp(32, now());
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
		PreparedStatement pstmt = conn.prepareStatement("UPDATE " + t.doc + " SET DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
				+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_PUBLISHED_STATUS=?,DC_MARKET_NAME=?,"
				+ "DC_LOCALE_NAME=?,DC_MODEL_NAME=?,DC_MODEL_TYPE=?,DC_CARLINE_CODE=?,DC_MODEL_FOLDER_NAME=?,"
				+ "DC_MANUAL_TYPE_FOLDER_NAME=?,DC_FACELIFT_FOLDER_NAME=?,DC_MATERIAL_FOLDER_NAME=?,DC_SOURCE_FILE_NAME=?,"
				+ "DC_SOURCE_NETWORK_LOC=?,DC_DOC_STATUS=?,DC_ESI_CATEGORY_MAPPED=?,DC_ESI_CATEGORY_MAPPED_LEVEL=?,"
				+ "DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_ESI_MAPPED_FILE_NAME=?,DC_VIN_MAPPED=?,DC_VIN_MAPPED_FILE_NAME=?,"
				+ "DC_DISPLAY_ORDER_MAPPED=?,DC_DISPORD_MAPPED_FILE_NAME=?,DC_IM_DOC_TITLE=?,DC_INRLNK_FOUND=?,"
				+ "DC_INRLNK_ALL_MAPPED=?,DC_INRLNK_FAILURE_REASON=?,DC_UPDATED_TMSTP=? WHERE DC_DD_ID=?");
		try {
			for (ContentDetails cd : list) {
				setDocumentColumns(pstmt, cd, 1);
				pstmt.setTimestamp(30, now());
				pstmt.setLong(31, rowIds.get(cd.getImDocumentId()));
				// one statement per row, never a batched UPDATE: MySQL Router (connection sharing) on
				// MC Dev refuses a batch sent as a multi-statement unless the session is already pinned
				pstmt.executeUpdate();
			}
		} finally {
			pstmt.close();
		}
	}

	/** The 29 document columns, in the order of both statements above, starting at parameter first. */
	private static void setDocumentColumns(PreparedStatement pstmt, ContentDetails cd, int first) throws Exception {
		int p = first;
		pstmt.setString(p++, cd.getChannelName());
		pstmt.setString(p++, cd.getImContentType());
		pstmt.setString(p++, cd.getImVersion());
		pstmt.setString(p++, cd.getImResourcePath());
		pstmt.setString(p++, cd.getImDocStatus());
		pstmt.setString(p++, cd.getMarket());
		pstmt.setString(p++, cd.getLocale());
		pstmt.setString(p++, cd.getModel());
		pstmt.setString(p++, cd.getModelType());
		pstmt.setString(p++, cd.getCarlineCode());
		pstmt.setString(p++, cd.getModelFolderName());
		pstmt.setString(p++, cd.getManualType());
		pstmt.setString(p++, cd.getFaceLiftFolderName());
		pstmt.setString(p++, cd.getMaterialName());
		pstmt.setString(p++, cd.getFileName());
		pstmt.setString(p++, cd.getFilePath());
		pstmt.setString(p++, ApplicationProperties.getProperty("flag.value.active"));
		pstmt.setString(p++, cd.getEsiCategoryMapped());
		pstmt.setString(p++, cd.getEsiCategoryMappedLevel());
		pstmt.setString(p++, cd.getEsiCategoryMappedRefKey());
		pstmt.setString(p++, cd.getEsiCategoryMappedFileName());
		pstmt.setString(p++, cd.getVinMapped());
		pstmt.setString(p++, cd.getVinMappedFileName());
		pstmt.setString(p++, cd.getDisplayOrderMapped());
		pstmt.setString(p++, cd.getDisplayOrderMappedFileName());
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

	/** View content rows of the documents (gms3_vc_mme_vin_dtl_<locale>), as cleanUpOldDataForDocument(). */
	private static void deleteViewContent(Connection conn, Tables t, List<ContentDetails> list) throws Exception {
		List<String> docIds = new ArrayList<String>();
		for (String id : documentIds(list)) {
			docIds.add(id.trim());
		}
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
		for (int from = 0; from < docIds.size(); from += chunk) {
			List<String> part = docIds.subList(from, Math.min(from + chunk, docIds.size()));
			PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + t.viewContent + " WHERE VC_VIN_LOCALE = ?"
					+ " AND VC_VIN_DOCUMENT_ID IN (" + marks(part.size()) + ")");
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

	private static void insertInnerLinks(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_INNER_DOC_LINK_PATH,DC_INNER_IM_DOC_ID,DC_INNER_LINK_UPDATED_STATUS,DC_CREATED_TMSTP) VALUES(" + marks(7) + ")");
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
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertEsiCategories(Connection conn, String table, List<ContentDetails> list) throws Exception {
		// one row per document, always - as processESICategoryInformation()
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
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_VIN_CARLINE_CODE,DC_VIN_VDS_CODE,DC_VIN_VIS_START_RANGE,DC_VIN_VIS_END_RANGE,"
				+ "DC_VIN_DISPLAYORDER_ENTRY,DC_VIN_REFKEY,DC_VIN_TXT_FILE_NAME,DC_VIN_TXT_FILE_PATH,DC_VIN_CREATED_TMSTP,"
				+ "DC_VIN_WMI_CODE) VALUES(" + marks(13) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getApplicableVINList()) {
					continue;
				}
				for (Object o : cd.getApplicableVINList()) {
					VinDetails v = (VinDetails) o;
					b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
					b.ps.setString(2, cd.getImDocumentId());
					b.ps.setString(3, cd.getFilePath());
					b.ps.setString(4, v.getCarlineCode());
					b.ps.setString(5, v.getVdsCode());
					b.ps.setString(6, v.getVisStartRange());
					b.ps.setString(7, v.getVisEndRange());
					b.ps.setString(8, v.getDisplayOrderTextFileName());
					b.ps.setString(9, v.getVinRefKey());
					b.ps.setString(10, v.getVinSourceFileName());
					b.ps.setString(11, v.getVinSorceFilePath());
					b.ps.setTimestamp(12, now());
					b.ps.setString(13, v.getWmiCode());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertMasterVins(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC,"
				+ "DC_VM_CARLINE_CODE,DC_VM_VDS_CODE,DC_VM_VIS_START_RANGE,DC_VM_VIS_END_RANGE,DC_VM_WMI_CODE,"
				+ "DC_VM_REFKEY,DC_VM_CREATED_TMSTP) VALUES(" + marks(10) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getMasterVinList()) {
					continue;
				}
				for (Object o : cd.getMasterVinList()) {
					VINEntFileDetails v = (VINEntFileDetails) o;
					b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
					b.ps.setString(2, cd.getImDocumentId());
					b.ps.setString(3, cd.getFilePath());
					b.ps.setString(4, v.getVinCarline());
					b.ps.setString(5, v.getVinVDS());
					b.ps.setString(6, v.getVinStartRange());
					b.ps.setString(7, v.getVinEndRange());
					b.ps.setString(8, v.getVinWMI());
					b.ps.setString(9, v.getVinRefKey());
					b.ps.setTimestamp(10, now());
					b.add();
				}
			}
			b.flush();
		} finally {
			b.close();
		}
	}

	private static void insertCvc(Connection conn, String table, List<ContentDetails> list) throws Exception {
		Batch b = new Batch(conn, "INSERT INTO " + table + " (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_CVC_CAT_CODE,"
				+ "DC_CVC_SYM_CODE,DC_CVC_SUBSYM_CODE,DC_CVC_CON_CODE,DC_CVC_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_CVC_REFKEY)"
				+ " VALUES(" + marks(9) + ")");
		try {
			for (ContentDetails cd : list) {
				if (null == cd.getCvcCategoryList()) {
					continue;
				}
				for (Object o : cd.getCvcCategoryList()) {
					CVCCategoryDetails c = (CVCCategoryDetails) o;
					b.ps.setLong(1, Long.parseLong(cd.getScheduleId().trim()));
					b.ps.setString(2, cd.getImDocumentId());
					b.ps.setString(3, c.getCategoryCode());
					b.ps.setString(4, c.getSymptomCode());
					b.ps.setString(5, c.getSubSymptomCode());
					b.ps.setString(6, c.getConditionCode());
					b.ps.setTimestamp(7, now());
					b.ps.setString(8, cd.getFilePath());
					b.ps.setString(9, c.getCvcRefKey());
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

	/** The documents grouped by the table set they are written to: locale + model type. */
	private static Map<String, List<ContentDetails>> byTables(List<ContentDetails> docs) {
		Map<String, List<ContentDetails>> m = new LinkedHashMap<String, List<ContentDetails>>();
		for (ContentDetails cd : docs) {
			String k = tableLocale(cd.getLocale()).toLowerCase() + "|" + (null == cd.getModelType() ? "" : cd.getModelType());
			List<ContentDetails> l = m.get(k);
			if (null == l) {
				l = new ArrayList<ContentDetails>();
				m.put(k, l);
			}
			l.add(cd);
		}
		return m;
	}

	/** en-UK -> en_UK: the VC_VIN_LOCALE value, lowercased for the table names. */
	private static String tableLocale(String locale) {
		if (null == locale || "".equals(locale.trim())) {
			throw new IllegalArgumentException("no locale for the MME tables");
		}
		return locale.replace("-", "_").trim();
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
