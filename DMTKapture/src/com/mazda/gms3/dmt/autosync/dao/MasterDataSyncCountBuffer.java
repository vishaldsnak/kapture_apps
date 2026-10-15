package com.mazda.gms3.dmt.autosync.dao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROCESSED / FAILED COUNTS OF A MASTER DATA SYNCING JOB, WRITTEN IN BULK.
 *
 * Legacy updateProcessingCount ran 4 statements per category row (SELECT + UPDATE for the schedule and for the
 * item), ~250 ms each through the MySQL Router on MC Dev. The counts are now kept here and written as one
 * increment per schedule and item every few seconds, and at the end of every item (same totals as legacy).
 * Shared by the locale workers of one job.
 */
public class MasterDataSyncCountBuffer {

	private static final long FLUSH_MS = 5000L;

	private final long scheduleId;
	private final MasterDataSyncTransactionDAO dao = new MasterDataSyncTransactionDAO();
	/** item id -> { processed, failed } not written yet */
	private final Map<Long, int[]> pending = new LinkedHashMap<Long, int[]>();
	private long lastFlush = System.currentTimeMillis();

	public MasterDataSyncCountBuffer(long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public void add(long itemId, boolean success) {
		boolean due;
		synchronized (this) {
			int[] c = pending.get(Long.valueOf(itemId));
			if (null == c) {
				c = new int[2];
				pending.put(Long.valueOf(itemId), c);
			}
			c[success ? 0 : 1]++;
			due = System.currentTimeMillis() - lastFlush >= FLUSH_MS;
		}
		if (due) {
			flush();
		}
	}

	public synchronized void flush() {
		lastFlush = System.currentTimeMillis();
		for (Map.Entry<Long, int[]> e : pending.entrySet()) {
			dao.addProcessingCounts(scheduleId, e.getKey().longValue(), e.getValue()[0], e.getValue()[1]);
		}
		pending.clear();
	}
}
