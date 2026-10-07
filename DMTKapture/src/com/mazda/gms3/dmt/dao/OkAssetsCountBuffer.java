package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.ThreadAbortUtil;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * OKASSETS PROCESSED / FAILED COUNTS OF A SCHEDULE, WRITTEN IN BULK.
 *
 * ScheduleDAO.updateProcessingOKAssetsCount / updateFailureOKAssetsCount ran four statements and
 * two commits for EVERY copied file. While a schedule's buffer is open (begin .. end) they only add
 * to this buffer, and the totals are written with one statement per table every FLUSH_FILES files
 * or FLUSH_MS milliseconds, and when the buffer is closed - so the History screen still moves.
 * Without begin() the DAO methods behave as before (the flows that do not open a buffer).
 * Safe for the copy threads of one schedule.
 */
public final class OkAssetsCountBuffer {

	private static Logger logger = LogManager.getLogger(OkAssetsCountBuffer.class);

	private static final int FLUSH_FILES = 100;
	private static final long FLUSH_MS = 5000L;

	private static final Map<String, OkAssetsCountBuffer> OPEN = new ConcurrentHashMap<String, OkAssetsCountBuffer>();

	private final String scheduleId;
	/** item id -> { processed, failed } not yet written */
	private final Map<String, long[]> pending = new LinkedHashMap<String, long[]>();
	private int pendingFiles = 0;
	private long lastFlush = System.currentTimeMillis();

	private OkAssetsCountBuffer(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	/** Opens the buffer of a schedule (the OKAssets step of a material folder starts). */
	public static void begin(String scheduleId) {
		if (null != scheduleId && !"".equals(scheduleId.trim())) {
			OPEN.putIfAbsent(scheduleId.trim(), new OkAssetsCountBuffer(scheduleId.trim()));
		}
	}

	/** Writes what is left and closes the buffer. */
	public static void end(String scheduleId) {
		if (null == scheduleId) {
			return;
		}
		OkAssetsCountBuffer b = OPEN.remove(scheduleId.trim());
		if (null != b) {
			b.flush();
		}
	}

	/**
	 * Counts one file into the open buffer of the schedule.
	 * @return false when the schedule has no open buffer - the caller then writes the count itself
	 */
	static boolean add(String scheduleId, String itemId, boolean processed) {
		if (OPEN.isEmpty() || null == scheduleId) {
			return false;
		}
		OkAssetsCountBuffer b = OPEN.get(scheduleId.trim());
		if (null == b) {
			return false;
		}
		// the per-file database call used to be the job's abort checkpoint
		ThreadAbortUtil.checkpoint();
		boolean flushNow;
		synchronized (b) {
			String key = null == itemId ? "" : itemId.trim();
			long[] c = b.pending.get(key);
			if (null == c) {
				c = new long[2];
				b.pending.put(key, c);
			}
			c[processed ? 0 : 1]++;
			b.pendingFiles++;
			flushNow = b.pendingFiles >= FLUSH_FILES || System.currentTimeMillis() - b.lastFlush >= FLUSH_MS;
		}
		if (flushNow) {
			b.flush();
		}
		return true;
	}

	private void flush() {
		Map<String, long[]> toWrite;
		synchronized (this) {
			if (pendingFiles == 0) {
				lastFlush = System.currentTimeMillis();
				return;
			}
			toWrite = new LinkedHashMap<String, long[]>(pending);
			pending.clear();
			pendingFiles = 0;
			lastFlush = System.currentTimeMillis();
		}
		long processed = 0, failed = 0;
		for (long[] c : toWrite.values()) {
			processed += c[0];
			failed += c[1];
		}
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DBConnectionHelper.getConnection();
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_OKAST_COUNT = COALESCE(DC_PROCESSED_OKAST_COUNT,0) + ?,"
					+ " DC_FAILED_OKAST_COUNT = COALESCE(DC_FAILED_OKAST_COUNT,0) + ? WHERE DC_SCHEDULE_ID = ?");
			ps.setLong(1, processed);
			ps.setLong(2, failed);
			ps.setLong(3, Long.parseLong(scheduleId));
			ps.executeUpdate();
			ps.close();
			ps = conn.prepareStatement("UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_OKAST_COUNT = COALESCE(DC_PROCESSED_OKAST_COUNT,0) + ?,"
					+ " DC_FAILED_OKAST_COUNT = COALESCE(DC_FAILED_OKAST_COUNT,0) + ? WHERE DC_CRITERIA_ID = ?");
			for (Map.Entry<String, long[]> e : toWrite.entrySet()) {
				if ("".equals(e.getKey())) {
					continue;
				}
				ps.setLong(1, e.getValue()[0]);
				ps.setLong(2, e.getValue()[1]);
				ps.setLong(3, Long.parseLong(e.getKey()));
				// one statement per item, never a batch: MySQL Router (connection sharing) on MC Dev
				// refuses a batch sent as a multi-statement
				ps.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			logger.info("flush :: OKAssets counts of Schedule {" + scheduleId + "} not written (processed +" + processed
					+ ", failed +" + failed + ") :: " + e);
			Utilities.printStackTraceToLogs(OkAssetsCountBuffer.class.getName(), "flush()", e);
			try {
				if (null != conn) {
					conn.rollback();
				}
			} catch (Exception re) {
				// nothing more to do
			}
		} finally {
			try {
				if (null != ps) {
					ps.close();
				}
				if (null != conn) {
					conn.setAutoCommit(true);
					conn.close();
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(OkAssetsCountBuffer.class.getName(), "flush()", e);
			}
		}
	}
}
