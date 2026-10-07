package com.mazda.gms3.dmt.preview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;

/**
 * THE FEW DATABASE STATEMENTS OF THE CONTENT PREVIEW - all run once at the END of a job, never per
 * document and never when the Preview page is opened (the page reads only the job's files).
 *
 *   saveDocuments        one batched insert: the documents of the job (read by the Publish job)
 *   withdraw             the documents this job unpublished: marked DELETED in the rows of EARLIER jobs,
 *                        which jobs those are is returned so their preview files are corrected
 *   manualTypeNames      only when a manual type had no display order row to take its name from
 */
public class PreviewDAO {

	private static Logger logger = LogManager.getLogger(PreviewDAO.class);

	public static final String STATUS_ACTIVE = "ACTIVE";
	/** unpublished by the delete processing of a later job (pv_deleted_schedule_id) - never published */
	public static final String STATUS_DELETED = "DELETED";

	/** Writes (or rewrites) the rows of the job's documents in one transaction. */
	public static void saveDocuments(String scheduleId, String locale, Collection<PreviewDocument> docs) throws Exception {
		if (null == docs || docs.isEmpty()) {
			return;
		}
		long started = System.currentTimeMillis();
		int batchSize = Integer.parseInt(KaptureApiClient.require("db.write.batch.size"));
		Connection conn = DBConnectionHelper.getConnection();
		boolean auto = conn.getAutoCommit();
		PreparedStatement ps = null;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("INSERT INTO gms3_dmt_preview_docs (pv_schedule_id, pv_locale, pv_document_id, "
					+ "pv_document_version, pv_channel, pv_document_type, pv_model_type, pv_model_folder, pv_manual_type, pv_title, "
					+ "pv_source_network_loc, pv_status, pv_created_tmstp, pv_updated_tmstp, pv_row_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
					+ "ON DUPLICATE KEY UPDATE pv_document_version=VALUES(pv_document_version), pv_row_id=VALUES(pv_row_id), pv_channel=VALUES(pv_channel), "
					+ "pv_document_type=VALUES(pv_document_type), pv_title=VALUES(pv_title), pv_status=VALUES(pv_status), "
					+ "pv_updated_tmstp=VALUES(pv_updated_tmstp)");
			Timestamp now = new Timestamp(System.currentTimeMillis());
			int n = 0;
			for (PreviewDocument d : docs) {
				ps.setLong(1, Long.parseLong(scheduleId.trim()));
				ps.setString(2, locale);
				ps.setString(3, d.id);
				ps.setString(4, d.version);
				ps.setString(5, d.channel);
				ps.setString(6, d.type);
				ps.setString(7, d.modelType);
				ps.setString(8, d.modelFolder);
				ps.setString(9, d.manualTypeFolder);
				ps.setString(10, d.title);
				ps.setString(11, d.sourcePath);
				ps.setString(12, STATUS_ACTIVE);
				ps.setTimestamp(13, now);
				ps.setTimestamp(14, now);
				ps.setString(15, d.rowId);
				ps.addBatch();
				if (++n % batchSize == 0) {
					ps.executeBatch();
				}
			}
			ps.executeBatch();
			conn.commit();
			logger.info("preview :: database :: " + docs.size() + " document rows saved :: " + (System.currentTimeMillis() - started) + " ms");
		} catch (Exception e) {
			conn.rollback();
			throw e;
		} finally {
			if (null != ps) {
				ps.close();
			}
			conn.setAutoCommit(auto);
			conn.close();
		}
	}

	/**
	 * Marks the documents this job UNPUBLISHED as DELETED (by this job) in the rows of the other
	 * (earlier) jobs - the Publish job reports them with the name of this job.
	 *
	 * @return the other jobs that listed any of them -> the document ids each listed
	 */
	public static Map<String, Set<String>> withdraw(String scheduleId, Collection<String> documentIds) throws Exception {
		Map<String, Set<String>> byJob = new HashMap<String, Set<String>>();
		if (null == documentIds || documentIds.isEmpty()) {
			return byJob;
		}
		long started = System.currentTimeMillis();
		List<String> ids = new ArrayList<String>(documentIds);
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		Connection conn = DBConnectionHelper.getConnection();
		try {
			for (int i = 0; i < ids.size(); i += chunk) {
				List<String> part = ids.subList(i, Math.min(i + chunk, ids.size()));
				String in = placeholders(part.size());
				PreparedStatement ps = conn.prepareStatement("SELECT pv_schedule_id, pv_document_id FROM gms3_dmt_preview_docs "
						+ "WHERE pv_document_id IN (" + in + ") AND pv_schedule_id <> ? AND pv_status = ?");
				int p = 1;
				for (String id : part) {
					ps.setString(p++, id);
				}
				ps.setLong(p++, Long.parseLong(scheduleId.trim()));
				ps.setString(p, STATUS_ACTIVE);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					String job = String.valueOf(rs.getLong(1));
					if (!byJob.containsKey(job)) {
						byJob.put(job, new LinkedHashSet<String>());
					}
					byJob.get(job).add(rs.getString(2));
				}
				rs.close();
				ps.close();
				if (byJob.isEmpty()) {
					continue;
				}
				ps = conn.prepareStatement("UPDATE gms3_dmt_preview_docs SET pv_status = ?, pv_deleted_schedule_id = ?, pv_updated_tmstp = ? "
						+ "WHERE pv_document_id IN (" + in + ") AND pv_schedule_id <> ? AND pv_status = ?");
				p = 1;
				ps.setString(p++, STATUS_DELETED);
				ps.setLong(p++, Long.parseLong(scheduleId.trim()));
				ps.setTimestamp(p++, new Timestamp(System.currentTimeMillis()));
				for (String id : part) {
					ps.setString(p++, id);
				}
				ps.setLong(p++, Long.parseLong(scheduleId.trim()));
				ps.setString(p, STATUS_ACTIVE);
				ps.executeUpdate();
				ps.close();
			}
		} finally {
			conn.close();
		}
		logger.info("preview :: database :: " + ids.size() + " unpublished document(s) looked up in earlier jobs :: found in "
				+ byJob.size() + " job(s) :: " + (System.currentTimeMillis() - started) + " ms");
		return byJob;
	}

	/**
	 * The jobs among scheduleIds that still have an unpublished document (an ACTIVE row) - the
	 * database decides whether a job has a preview, not the preview.json file: on the S3 mount a
	 * deleted preview folder can come back, and its documents are published already.
	 */
	public static Set<String> jobsWithUnpublished(Collection<String> scheduleIds) throws Exception {
		Set<String> jobs = new LinkedHashSet<String>();
		if (null == scheduleIds || scheduleIds.isEmpty()) {
			return jobs;
		}
		List<String> ids = new ArrayList<String>(scheduleIds);
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		Connection conn = DBConnectionHelper.getConnection();
		try {
			for (int i = 0; i < ids.size(); i += chunk) {
				List<String> part = ids.subList(i, Math.min(i + chunk, ids.size()));
				PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT pv_schedule_id FROM gms3_dmt_preview_docs "
						+ "WHERE pv_schedule_id IN (" + placeholders(part.size()) + ") AND pv_status = ?");
				int p = 1;
				for (String id : part) {
					ps.setLong(p++, Long.parseLong(id.trim()));
				}
				ps.setString(p, STATUS_ACTIVE);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					jobs.add(String.valueOf(rs.getLong(1)));
				}
				rs.close();
				ps.close();
			}
		} finally {
			conn.close();
		}
		return jobs;
	}

	/**
	 * Manual type names of the locale (ja-JP form), by manual code AND by reference key - the two
	 * keys the display order processing looks a manual type name up by.
	 */
	public static Map<String, String> manualTypeNames(String langCode) throws Exception {
		Map<String, String> names = new HashMap<String, String>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT dc_manual_code, dc_manual_ref_key, dc_manual_name FROM gms3_dmt_conv_manual_type "
					+ "WHERE TRIM(LOWER(mdm_ml_lang_code)) = ? AND dc_manual_flag NOT IN (?, ?)");
			ps.setString(1, langCode.trim().toLowerCase());
			ps.setString(2, ApplicationProperties.getProperty("flag.value.draft"));
			ps.setString(3, ApplicationProperties.getProperty("flag.value.delete"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String name = rs.getString(3);
				if (null == name) {
					continue;
				}
				if (null != rs.getString(1)) {
					names.put("CODE:" + rs.getString(1).trim().toLowerCase(), name);
				}
				if (null != rs.getString(2)) {
					names.put("REF:" + rs.getString(2).trim().toLowerCase(), name);
				}
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return names;
	}

	private static String placeholders(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "?" : ",?");
		}
		return sb.toString();
	}
}
