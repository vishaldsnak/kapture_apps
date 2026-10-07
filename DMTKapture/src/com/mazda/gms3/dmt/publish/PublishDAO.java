package com.mazda.gms3.dmt.publish;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.preview.PreviewDAO;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.DateFormatter;

/**
 * THE DATABASE STATEMENTS OF THE PUBLISH CONTENT JOB (MC, MME and MNAO markets - see PublishMarket).
 *
 * The job publishes the documents a DMT job left in gms3_dmt_preview_docs. Its schedule is an
 * ordinary DMT schedule row, told apart by its name only:
 *   MGSS_PUB_MC_<yyyyMMdd>_<id of the DMT job it was scheduled from>   (schedule.name.publish.mc.key)
 *   MGSS_PUB_MME_<yyyyMMdd>_<id of the DMT job it was scheduled from>  (schedule.name.publish.mme.key)
 *   MGSS_PUB_MNAO_<yyyyMMdd>_<id of the DMT job it was scheduled from> (schedule.name.publish.mnao.key)
 * and thread <prefix><own schedule id> (History aborts a job by its thread name).
 * One criteria row (item) per model type / model folder / manual type / material folder of the
 * documents, counted from gms3_dmt_preview_docs - a document another job published already has
 * no row there any more, so it is in no total.
 *
 * Version, row id and state of a document come from the DMT document table, which always holds
 * the latest version written to Kapture, whichever job wrote it.
 */
public class PublishDAO {

	private static Logger logger = LogManager.getLogger(PublishDAO.class);

	/** The busy-items check and the schedule insert run under this lock (two clicks at once). */
	public static final Object SCHEDULE_LOCK = new Object();

	// ---- scheduling ----------------------------------------------------------------------------

	/** Why a publish job cannot be scheduled: a message, and the items in use by other jobs (if that is the reason). */
	public static class Refusal {
		public final String message;
		/** {model folder, manual type, job name, CONVERSION / PUBLISH} */
		public final List<String[]> busy = new ArrayList<String[]>();

		Refusal(String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(message);
			for (String[] b : busy) {
				sb.append("\n").append(String.join(" | ", b));
			}
			return sb.toString();
		}
	}

	/**
	 * Why a publish job cannot be scheduled for this DMT job now; null when it can.
	 */
	public static Refusal refusal(String sourceScheduleId) throws Exception {
		long source = Long.parseLong(sourceScheduleId.trim());
		String pending = KaptureApiClient.require("schedule.status.pending.value");
		String processing = KaptureApiClient.require("schedule.status.processing.value");
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT DC_SCHEDULE_NAME, DC_STATUS FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID = ?");
			ps.setLong(1, source);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return new Refusal("Job " + source + " does not exist.");
			}
			String name = rs.getString(1);
			String status = rs.getString(2);
			rs.close();
			ps.close();
			PublishMarket market = PublishMarket.ofConversionJob(name);
			if (null == market) {
				return new Refusal("Job " + source + " is not an MC, MME or MNAO conversion job.");
			}
			if (pending.equals(status) || processing.equals(status)) {
				return new Refusal("Job " + name + " is still running.");
			}
			// the model + manual type folders of the job's documents
			Map<String, String[]> mine = new LinkedHashMap<String, String[]>();
			ps = conn.prepareStatement("SELECT DISTINCT pv_locale, pv_model_folder, pv_manual_type FROM gms3_dmt_preview_docs WHERE pv_schedule_id = ?"
					+ " ORDER BY pv_model_folder, pv_manual_type");
			ps.setLong(1, source);
			rs = ps.executeQuery();
			while (rs.next()) {
				mine.put(itemKey(rs.getString(1), rs.getString(2), rs.getString(3)), new String[] { t(rs.getString(2)), t(rs.getString(3)) });
			}
			rs.close();
			ps.close();
			if (mine.isEmpty()) {
				return new Refusal("Job " + name + " has no document left to publish.");
			}
			// none of them may be in a conversion or publish job that is pending / running: the same
			// documents would be written and published at the same time
			Map<String, String> conversions = runningItems(market.conversionPrefix());
			Map<String, String> publishes = runningItems(market.publishPrefix());
			Refusal refusal = new Refusal("This job cannot be published yet. Some of its content is being processed by other jobs."
					+ " Publish again when those jobs have finished.");
			for (Map.Entry<String, String[]> e : mine.entrySet()) {
				if (publishes.containsKey(e.getKey())) {
					refusal.busy.add(new String[] { e.getValue()[0], e.getValue()[1], publishes.get(e.getKey()), "PUBLISH" });
				} else if (conversions.containsKey(e.getKey())) {
					refusal.busy.add(new String[] { e.getValue()[0], e.getValue()[1], conversions.get(e.getKey()), "CONVERSION" });
				}
			}
			if (!refusal.busy.isEmpty()) {
				return refusal;
			}
			return null;
		} finally {
			conn.close();
		}
	}

	/**
	 * The items (model folder + manual type, per locale) of the jobs that are pending or running
	 * and whose thread starts with threadPrefix: itemKey -> schedule name.
	 */
	public static Map<String, String> runningItems(String threadPrefix) throws Exception {
		Map<String, String> items = new HashMap<String, String>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT c.DC_LOCALE, c.DC_MODEL_FOLDER_NAME, c.DC_MANUAL_TYPE, s.DC_SCHEDULE_NAME"
					+ " FROM gms3_dmt_conv_sch_criteria c JOIN gms3_dmt_conv_schedule s ON s.DC_SCHEDULE_ID = c.DC_SCHEDULE_ID"
					+ " WHERE LOCATE(?, s.DC_SCH_THREAD_ID) = 1 AND s.DC_STATUS IN (?, ?)");
			ps.setString(1, threadPrefix);
			ps.setString(2, KaptureApiClient.require("schedule.status.pending.value"));
			ps.setString(3, KaptureApiClient.require("schedule.status.processing.value"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				items.put(itemKey(rs.getString(1), rs.getString(2), rs.getString(3)), rs.getString(4));
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return items;
	}

	/**
	 * The market of the DMT conversion job a publish job is scheduled from.
	 *
	 * @throws IllegalStateException when the job is not an MC, MME or MNAO conversion job
	 */
	public static PublishMarket marketOf(String sourceScheduleId) {
		String name = scheduleName(Long.valueOf(sourceScheduleId.trim()));
		PublishMarket market = PublishMarket.ofConversionJob(name);
		if (null == market) {
			throw new IllegalStateException("Job " + sourceScheduleId + " (" + name + ") is not an MC, MME or MNAO conversion job");
		}
		return market;
	}

	/** locale (ja-JP / ja_JP alike) + model folder + manual type, case-insensitive */
	public static String itemKey(String locale, String modelFolder, String manualType) {
		return (t(locale).replace('_', '-') + "|" + t(modelFolder) + "|" + t(manualType)).toLowerCase();
	}

	private static String t(String s) {
		return null == s ? "" : s.trim();
	}

	/**
	 * The schedule row and one criteria row per item, in one transaction.
	 *
	 * @return the new schedule id
	 */
	public static long createSchedule(String sourceScheduleId, String wslId, List<PublishDocument> docs) throws Exception {
		PublishMarket market = marketOf(sourceScheduleId);
		String prefix = market.publishPrefix();
		String name = prefix + DateFormatter.parseDateToString_YYYY_MM_DD(new java.util.Date()) + "_" + sourceScheduleId.trim();
		Map<String, List<PublishDocument>> items = byItem(docs);
		long toPublish = 0;
		long deleted = 0;
		for (PublishDocument d : docs) {
			if (isDeleted(d)) {
				deleted++;
			} else {
				toPublish++;
			}
		}
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Connection conn = DBConnectionHelper.getConnection();
		boolean auto = conn.getAutoCommit();
		try {
			conn.setAutoCommit(false);
			PreparedStatement ps = conn.prepareStatement("INSERT INTO gms3_dmt_conv_schedule (DC_SCHEDULE_NAME, DC_USER_ID, DC_RECORD_STATUS,"
					+ " DC_IM_PROCESSING_STATUS, DC_STATUS, DC_TOTAL_DOCS_COUNT, DC_PROCESSED_DOCS_COUNT, DC_FAILED_DOCS_COUNT,"
					+ " DC_TOTAL_DOCS_COUNT_DEL, DC_PROCESSED_DOCS_COUNT_DEL, DC_FAILED_DOCS_COUNT_DEL, DC_OKASSETS_COUNT, DC_OKASSETS_COUNT_DEL,"
					+ " DC_SCH_THREAD_ID, DC_SCHEDULE_TMSTP, DC_TOTAL_METADOCS_COUNT, DC_TOTAL_DISPORD_COUNT, DC_TOTAL_CD_COUNT, DC_TOTAL_SCM_COUNT)"
					+ " VALUES (?,?,?,?,?,?,0,0,?,0,0,0,0,?,?,0,0,0,0)", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setString(2, wslId);
			ps.setString(3, AutoSyncConstants.STATUS_ACTIVE);
			ps.setString(4, KaptureApiClient.require("flag.value.publish"));
			ps.setString(5, KaptureApiClient.require("schedule.status.pending.value"));
			ps.setLong(6, toPublish);
			ps.setLong(7, deleted);
			ps.setString(8, prefix);
			ps.setTimestamp(9, now);
			ps.executeUpdate();
			ResultSet keys = ps.getGeneratedKeys();
			keys.next();
			long scheduleId = keys.getLong(1);
			keys.close();
			ps.close();

			ps = conn.prepareStatement("UPDATE gms3_dmt_conv_schedule SET DC_SCH_THREAD_ID = ? WHERE DC_SCHEDULE_ID = ?");
			ps.setString(1, prefix + scheduleId);
			ps.setLong(2, scheduleId);
			ps.executeUpdate();
			ps.close();

			ps = conn.prepareStatement("INSERT INTO gms3_dmt_conv_sch_criteria (DC_SCHEDULE_ID, DC_MARKET, DC_LOCALE, DC_MODEL, DC_MANUAL_TYPE,"
					+ " DC_TOTAL_DOCS_COUNT, DC_PROCESSED_DOCS_COUNT, DC_FAILED_DOCS_COUNT, DC_TOTAL_DOCS_COUNT_DEL, DC_PROCESSED_DOCS_COUNT_DEL,"
					+ " DC_FAILED_DOCS_COUNT_DEL, DC_OKASSETS_COUNT, DC_OKASSETS_COUNT_DEL, DC_CRITERIA_CREATED_TMSTP, DC_MATERIAL_NAME, DC_METADOCS_COUNT,"
					+ " DC_MODEL_FOLDER_NAME, DC_MODEL_TYPE, DC_CARLINE_CODE, DC_FACELIFT_FOLDER_NAME, DC_TOTAL_DISPORD_COUNT, DC_TOTAL_CD_COUNT,"
					+ " DC_TOTAL_SCM_COUNT) VALUES (?,?,?,?,?,?,0,0,?,0,0,0,0,?,?,0,?,?,?,?,0,0,0)");
			for (List<PublishDocument> item : items.values()) {
				PublishDocument first = item.get(0);
				long itemPublish = 0;
				long itemDeleted = 0;
				for (PublishDocument d : item) {
					if (isDeleted(d)) {
						itemDeleted++;
					} else {
						itemPublish++;
					}
				}
				ps.setLong(1, scheduleId);
				ps.setString(2, market.market());
				ps.setString(3, null == first.locale ? null : first.locale.replace('_', '-'));
				ps.setString(4, first.model);
				ps.setString(5, first.manualType);
				ps.setLong(6, itemPublish);
				ps.setLong(7, itemDeleted);
				ps.setTimestamp(8, now);
				ps.setString(9, first.materialFolder);
				ps.setString(10, first.modelFolder);
				ps.setString(11, first.modelType);
				ps.setString(12, first.carlineCode);
				ps.setString(13, first.faceliftFolder);
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
			conn.commit();
			logger.info("createSchedule :: publish job " + name + " (schedule " + scheduleId + ") :: to publish=" + toPublish
					+ " deleted=" + deleted + " items=" + items.size());
			return scheduleId;
		} catch (Exception e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(auto);
			conn.close();
		}
	}

	/** The documents grouped by item, in a stable order. */
	public static Map<String, List<PublishDocument>> byItem(List<PublishDocument> docs) {
		Map<String, List<PublishDocument>> items = new LinkedHashMap<String, List<PublishDocument>>();
		for (PublishDocument d : docs) {
			List<PublishDocument> l = items.get(d.itemKey());
			if (null == l) {
				l = new ArrayList<PublishDocument>();
				items.put(d.itemKey(), l);
			}
			l.add(d);
		}
		return items;
	}

	/** item key -> criteria id of a publish job */
	public static Map<String, Long> items(String publishScheduleId) throws Exception {
		Map<String, Long> items = new HashMap<String, Long>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT DC_CRITERIA_ID, DC_MODEL_TYPE, DC_MODEL_FOLDER_NAME, DC_MANUAL_TYPE, DC_MATERIAL_NAME"
					+ " FROM gms3_dmt_conv_sch_criteria WHERE DC_SCHEDULE_ID = ?");
			ps.setLong(1, Long.parseLong(publishScheduleId.trim()));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PublishDocument key = new PublishDocument();
				key.modelType = rs.getString(2);
				key.modelFolder = rs.getString(3);
				key.manualType = rs.getString(4);
				key.materialFolder = rs.getString(5);
				items.put(key.itemKey(), rs.getLong(1));
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return items;
	}

	/** The item ids the documents are counted under. */
	public static java.util.Set<Long> byItemIds(List<PublishDocument> docs) {
		java.util.Set<Long> ids = new java.util.LinkedHashSet<Long>();
		for (PublishDocument d : docs) {
			if (null != d.itemId) {
				ids.add(d.itemId);
			}
		}
		return ids;
	}

	public static boolean isDeleted(PublishDocument d) {
		return PreviewDAO.STATUS_DELETED.equals(d.previewStatus)
				|| (d.inDmtTables && KaptureApiClient.require("flag.value.delete").equals(PublishDocument.t(d.docStatus)));
	}

	// ---- the documents -------------------------------------------------------------------------

	/**
	 * Every row the DMT job still has in gms3_dmt_preview_docs, with what the DMT document table
	 * holds for the document now.
	 */
	public static List<PublishDocument> loadDocuments(String sourceScheduleId) throws Exception {
		List<PublishDocument> docs = new ArrayList<PublishDocument>();
		Map<String, PublishDocument> byId = new HashMap<String, PublishDocument>();
		PublishMarket market = marketOf(sourceScheduleId);
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT pv_document_id, pv_locale, pv_channel, pv_title, pv_model_type, pv_model_folder,"
					+ " pv_manual_type, pv_status, pv_deleted_schedule_id FROM gms3_dmt_preview_docs WHERE pv_schedule_id = ? ORDER BY pv_id");
			ps.setLong(1, Long.parseLong(sourceScheduleId.trim()));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PublishDocument d = new PublishDocument();
				d.documentId = rs.getString(1).trim();
				d.locale = rs.getString(2);
				d.channel = rs.getString(3);
				d.title = rs.getString(4);
				d.modelType = rs.getString(5);
				d.modelFolder = rs.getString(6);
				d.manualType = rs.getString(7);
				d.previewStatus = rs.getString(8);
				long del = rs.getLong(9);
				d.deletedByScheduleId = rs.wasNull() ? null : Long.valueOf(del);
				docs.add(d);
				byId.put(d.documentId.toUpperCase(), d);
			}
			rs.close();
			ps.close();

			// the latest row of each document in the DMT document table of its model type
			Map<String, List<String>> idsByTable = new HashMap<String, List<String>>();
			for (PublishDocument d : docs) {
				d.dmtTable = market.documentTable(d.modelType, d.locale);
				List<String> ids = idsByTable.get(d.dmtTable);
				if (null == ids) {
					ids = new ArrayList<String>();
					idsByTable.put(d.dmtTable, ids);
				}
				ids.add(d.documentId);
			}
			int chunk = KaptureApiClient.requireInt("db.lookup.batch.size");
			for (Map.Entry<String, List<String>> e : idsByTable.entrySet()) {
				List<String> ids = e.getValue();
				for (int from = 0; from < ids.size(); from += chunk) {
					List<String> part = ids.subList(from, Math.min(from + chunk, ids.size()));
					ps = conn.prepareStatement("SELECT DC_IM_DOC_ID, " + market.activeFlagColumn() + ", DC_IM_DOC_VERSION, DC_IM_DOC_CONTENT_ID, "
							+ market.publishedFlagColumn() + ", DC_MODEL_NAME, DC_MATERIAL_FOLDER_NAME, " + market.carlineColumn()
							+ ", DC_FACELIFT_FOLDER_NAME FROM " + e.getKey()
							+ " WHERE DC_IM_DOC_ID IN (" + placeholders(part.size()) + ") ORDER BY DC_DD_ID DESC");
					int p = 1;
					for (String id : part) {
						ps.setString(p++, id);
					}
					rs = ps.executeQuery();
					while (rs.next()) {
						PublishDocument d = byId.get(rs.getString(1).trim().toUpperCase());
						if (null == d || d.inDmtTables || !e.getKey().equals(d.dmtTable)) {
							continue;
						}
						d.inDmtTables = true;
						d.docStatus = rs.getString(2);
						d.version = rs.getString(3);
						d.rowId = rs.getString(4);
						d.publishedFlag = rs.getString(5);
						d.model = rs.getString(6);
						d.materialFolder = rs.getString(7);
						d.carlineCode = rs.getString(8);
						d.faceliftFolder = rs.getString(9);
					}
					rs.close();
					ps.close();
				}
			}
		} finally {
			conn.close();
		}
		return docs;
	}

public static String scheduleName(Long scheduleId) {
		if (null == scheduleId) {
			return null;
		}
		try {
			Connection conn = DBConnectionHelper.getConnection();
			try {
				PreparedStatement ps = conn.prepareStatement("SELECT DC_SCHEDULE_NAME FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID = ?");
				ps.setLong(1, scheduleId);
				ResultSet rs = ps.executeQuery();
				String name = rs.next() ? rs.getString(1) : null;
				rs.close();
				ps.close();
				return name;
			} finally {
				conn.close();
			}
		} catch (Exception e) {
			logger.info("scheduleName :: could not read the name of schedule " + scheduleId + " :: " + e);
			return null;
		}
	}

	// ---- after publishing ----------------------------------------------------------------------

	/**
	 * Documents Kapture published: in ONE transaction the DMT document table (published status,
	 * version, row id), the view content rows (Published) and the document's rows of EVERY job in
	 * gms3_dmt_preview_docs (so no other job publishes it again). The other jobs that listed a
	 * document are put in its otherJobs.
	 */
	public static void published(String sourceScheduleId, List<PublishDocument> docs) throws Exception {
		if (docs.isEmpty()) {
			return;
		}
		long source = Long.parseLong(sourceScheduleId.trim());
		PublishMarket market = marketOf(sourceScheduleId);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Connection conn = DBConnectionHelper.getConnection();
		boolean auto = conn.getAutoCommit();
		try {
			conn.setAutoCommit(false);
			// NO BATCHED UPDATE / DELETE: on MC Dev the database is reached through MySQL Router with
			// connection sharing, which refuses a batch sent as one multi-statement ("Multi-Statements
			// are forbidden if connection-sharing is enabled"). One statement per row, or one per IN list.
			Map<String, PublishDocument> byKey = new HashMap<String, PublishDocument>();
			Map<String, List<String>> idsByLocale = new LinkedHashMap<String, List<String>>();
			for (PublishDocument d : docs) {
				byKey.put(d.documentId.toUpperCase() + "|" + d.locale, d);
				List<String> ids = idsByLocale.get(d.locale);
				if (null == ids) {
					ids = new ArrayList<String>();
					idsByLocale.put(d.locale, ids);
				}
				ids.add(d.documentId);
			}
			int chunk = KaptureApiClient.requireInt("db.lookup.batch.size");
			for (Map.Entry<String, List<String>> e : idsByLocale.entrySet()) {
				List<String> ids = e.getValue();
				for (int from = 0; from < ids.size(); from += chunk) {
					List<String> part = ids.subList(from, Math.min(from + chunk, ids.size()));
					// the other jobs that list the documents (their previews are marked afterwards)
					PreparedStatement ps = conn.prepareStatement("SELECT pv_schedule_id, pv_document_id FROM gms3_dmt_preview_docs"
							+ " WHERE pv_locale = ? AND pv_schedule_id <> ? AND pv_document_id IN (" + placeholders(part.size()) + ")");
					int p = 1;
					ps.setString(p++, e.getKey());
					ps.setLong(p++, source);
					for (String id : part) {
						ps.setString(p++, id);
					}
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						PublishDocument d = byKey.get(rs.getString(2).trim().toUpperCase() + "|" + e.getKey());
						if (null != d) {
							d.otherJobs.add(String.valueOf(rs.getLong(1)));
						}
					}
					rs.close();
					ps.close();

					// MNAO: the model year and the VIN view content tables
					for (String[] vc : market.viewContentTables(e.getKey())) {
						ps = conn.prepareStatement("UPDATE " + vc[0] + " SET VC_CONTENT_STATUS = ? WHERE " + vc[1] + "LOCALE = ?"
								+ " AND " + vc[1] + "DOCUMENT_ID IN (" + placeholders(part.size()) + ")");
						p = 1;
						ps.setString(p++, KaptureApiClient.require("view.content.status.published"));
						ps.setString(p++, null == e.getKey() ? null : e.getKey().replace('-', '_'));
						for (String id : part) {
							ps.setString(p++, id);
						}
						ps.executeUpdate();
						ps.close();
					}

					ps = conn.prepareStatement("DELETE FROM gms3_dmt_preview_docs WHERE pv_locale = ? AND pv_document_id IN ("
							+ placeholders(part.size()) + ")");
					p = 1;
					ps.setString(p++, e.getKey());
					for (String id : part) {
						ps.setString(p++, id);
					}
					ps.executeUpdate();
					ps.close();
				}
			}
			// the DMT document table: version and row id differ per document - ONE statement per table and
			// IN list, the values picked per document with CASE (one statement per row cost ~170 ms each
			// on MC Dev: 826 documents = 2.5 minutes)
			Map<String, List<PublishDocument>> byTable = new LinkedHashMap<String, List<PublishDocument>>();
			for (PublishDocument d : docs) {
				List<PublishDocument> l = byTable.get(d.dmtTable);
				if (null == l) {
					l = new ArrayList<PublishDocument>();
					byTable.put(d.dmtTable, l);
				}
				l.add(d);
			}
			for (Map.Entry<String, List<PublishDocument>> e : byTable.entrySet()) {
				List<PublishDocument> all = e.getValue();
				for (int from = 0; from < all.size(); from += chunk) {
					List<PublishDocument> part = all.subList(from, Math.min(from + chunk, all.size()));
					StringBuilder version = new StringBuilder("CASE DC_IM_DOC_ID");
					StringBuilder rowId = new StringBuilder("CASE DC_IM_DOC_ID");
					for (int i = 0; i < part.size(); i++) {
						version.append(" WHEN ? THEN ?");
						rowId.append(" WHEN ? THEN ?");
					}
					version.append(" ELSE DC_IM_DOC_VERSION END");
					rowId.append(" ELSE DC_IM_DOC_CONTENT_ID END");
					PreparedStatement ps = conn.prepareStatement("UPDATE " + e.getKey() + " SET " + market.publishedFlagColumn() + " = ?, DC_IM_DOC_VERSION = "
							+ version + ", DC_IM_DOC_CONTENT_ID = " + rowId + ", DC_UPDATED_TMSTP = ? WHERE " + market.activeFlagColumn()
							+ " = ? AND DC_IM_DOC_ID IN ("
							+ placeholders(part.size()) + ")");
					int p = 1;
					ps.setString(p++, KaptureApiClient.require("flag.value.publish"));
					for (PublishDocument d : part) {
						ps.setString(p++, d.documentId);
						ps.setString(p++, d.publishedVersion);
					}
					for (PublishDocument d : part) {
						ps.setString(p++, d.documentId);
						ps.setString(p++, d.publishedRowId);
					}
					ps.setTimestamp(p++, now);
					ps.setString(p++, KaptureApiClient.require("flag.value.active"));
					for (PublishDocument d : part) {
						ps.setString(p++, d.documentId);
					}
					ps.executeUpdate();
					ps.close();
				}
			}
			conn.commit();
			logger.info("published :: DMT tables, view content and preview rows updated for " + docs.size() + " document(s) :: "
					+ (System.currentTimeMillis() - now.getTime()) + " ms");
		} catch (Exception e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(auto);
			conn.close();
		}
	}

	/** The DELETED documents of the job leave gms3_dmt_preview_docs once they are reported. */
	public static void removeRows(String sourceScheduleId, List<PublishDocument> docs) throws Exception {
		if (docs.isEmpty()) {
			return;
		}
		Connection conn = DBConnectionHelper.getConnection();
		try {
			// one statement per IN list - no batched DELETE (see published())
			int chunk = KaptureApiClient.requireInt("db.lookup.batch.size");
			for (int from = 0; from < docs.size(); from += chunk) {
				List<PublishDocument> part = docs.subList(from, Math.min(from + chunk, docs.size()));
				PreparedStatement ps = conn.prepareStatement("DELETE FROM gms3_dmt_preview_docs WHERE pv_schedule_id = ? AND pv_document_id IN ("
						+ placeholders(part.size()) + ")");
				int p = 1;
				ps.setLong(p++, Long.parseLong(sourceScheduleId.trim()));
				for (PublishDocument d : part) {
					ps.setString(p++, d.documentId);
				}
				ps.executeUpdate();
				ps.close();
			}
		} finally {
			conn.close();
		}
	}

	/** Raises the counts of the schedule and of one item by what a batch did. */
	public static void addCounts(String publishScheduleId, Long itemId, int published, int failed, int deleted) {
		if (published <= 0 && failed <= 0 && deleted <= 0) {
			return;
		}
		try {
			Connection conn = DBConnectionHelper.getConnection();
			try {
				String set = " SET DC_PROCESSED_DOCS_COUNT = IFNULL(DC_PROCESSED_DOCS_COUNT,0) + ?, DC_FAILED_DOCS_COUNT = IFNULL(DC_FAILED_DOCS_COUNT,0) + ?,"
						+ " DC_PROCESSED_DOCS_COUNT_DEL = IFNULL(DC_PROCESSED_DOCS_COUNT_DEL,0) + ?";
				PreparedStatement ps = conn.prepareStatement("UPDATE gms3_dmt_conv_schedule" + set + " WHERE DC_SCHEDULE_ID = ?");
				ps.setLong(1, published);
				ps.setLong(2, failed);
				ps.setLong(3, deleted);
				ps.setLong(4, Long.parseLong(publishScheduleId.trim()));
				ps.executeUpdate();
				ps.close();
				if (null != itemId) {
					ps = conn.prepareStatement("UPDATE gms3_dmt_conv_sch_criteria" + set + " WHERE DC_CRITERIA_ID = ?");
					ps.setLong(1, published);
					ps.setLong(2, failed);
					ps.setLong(3, deleted);
					ps.setLong(4, itemId);
					ps.executeUpdate();
					ps.close();
				}
			} finally {
				conn.close();
			}
		} catch (Exception e) {
			logger.info("addCounts :: the counts of schedule " + publishScheduleId + " could not be raised :: " + e);
		}
	}

	private static String placeholders(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "?" : ",?");
		}
		return sb.toString();
	}
}
