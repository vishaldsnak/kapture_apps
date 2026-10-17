package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;

/**
 * WHICH JOBS MAY RUN AT THE SAME TIME (2026-10-16). Jobs run in parallel, each in its own thread:
 *
 *   - CONTENT jobs (Conversion MC / MME / MNAO, Publish Content) run side by side, but an item
 *     (locale + model folder + manual type + facelift folder + material folder) is in at most one
 *     Pending / Processing content job. An empty facelift / material folder covers all of them.
 *   - MASTER jobs (IPM Facelift VIN Update, Document Identification, Master Data Syncing) run side by
 *     side too, but never together with a content job - in either direction.
 *   - At most schedule.parallel.jobs.max jobs are Pending / Processing in all (Meta Data Validation
 *     is not counted: it only validates text files).
 *
 * The check and the insert of the new schedule run under SCHEDULE_LOCK (two clicks at once).
 */
public class JobRules {

	private static Logger logger = LogManager.getLogger(JobRules.class);

	/** Every check + schedule insert of a Conversion, Publish, Facelift, Document Identification or Master Data Syncing job. */
	public static final Object SCHEDULE_LOCK = new Object();

	/** Why a job cannot be scheduled: a message key of dmtresource_en.properties and its arguments. */
	public static class Refusal {
		public final String key;
		public final String[] args;

		Refusal(String key, String... args) {
			this.key = key;
			this.args = args;
		}
	}

	/** The Pending / Processing jobs, by kind (schedule names). */
	public static class ActiveJobs {
		public final List<String> content = new ArrayList<String>();
		public final List<String> master = new ArrayList<String>();

		public int total() {
			return content.size() + master.size();
		}
	}

	/** One item of a content job. */
	public static class Item {
		public final String locale;
		public final String modelFolder;
		public final String manualType;
		public final String faceliftFolder;
		public final String materialFolder;
		/** the job it belongs to (running items only) */
		public final String scheduleName;

		public Item(String locale, String modelFolder, String manualType, String faceliftFolder, String materialFolder, String scheduleName) {
			this.locale = null == locale ? "" : locale.trim().replace('_', '-');
			this.modelFolder = t(modelFolder);
			this.manualType = t(manualType);
			this.faceliftFolder = t(faceliftFolder);
			this.materialFolder = t(materialFolder);
			this.scheduleName = scheduleName;
		}

		/** The same item, or one of them covers the other (an empty facelift / material folder covers all). */
		public boolean overlaps(Item o) {
			return same(locale, o.locale) && same(modelFolder, o.modelFolder) && same(manualType, o.manualType)
					&& covers(faceliftFolder, o.faceliftFolder) && covers(materialFolder, o.materialFolder);
		}

		@Override
		public String toString() {
			return locale + " | " + modelFolder + " | " + manualType + " | " + faceliftFolder + " | " + materialFolder;
		}
	}

	// ---- the rules -----------------------------------------------------------------------------

	/** A Conversion or Publish job: refused while a master job runs, or when the cap is reached; null when allowed. */
	public static Refusal contentRefusal(ActiveJobs active) {
		if (!active.master.isEmpty()) {
			return new Refusal("error.message.job.refused.master.running", String.join(" / ", active.master));
		}
		return capRefusal(active);
	}

	/** A Facelift, Document Identification or Master Data Syncing job: refused while a content job runs, or when the cap is reached. */
	public static Refusal masterRefusal(ActiveJobs active) {
		if (!active.content.isEmpty()) {
			return new Refusal("error.message.job.refused.content.running", String.join(" / ", active.content));
		}
		return capRefusal(active);
	}

	private static Refusal capRefusal(ActiveJobs active) {
		int max = KaptureApiClient.requireInt("schedule.parallel.jobs.max");
		if (active.total() >= max) {
			List<String> all = new ArrayList<String>(active.content);
			all.addAll(active.master);
			return new Refusal("error.message.job.limit.reached", String.valueOf(max), String.join(" / ", all));
		}
		return null;
	}

	/** The running content item the item overlaps, null when none. */
	public static Item busy(List<Item> running, Item item) {
		for (Item r : running) {
			if (r.overlaps(item)) {
				return r;
			}
		}
		return null;
	}

	// ---- reads ---------------------------------------------------------------------------------

	/** The jobs that are Pending / Processing now. */
	public static ActiveJobs active() throws Exception {
		ActiveJobs active = new ActiveJobs();
		String pending = KaptureApiClient.require("schedule.status.pending.value");
		String processing = KaptureApiClient.require("schedule.status.processing.value");
		Connection conn = DBConnectionHelper.getConnection();
		try {
			// Conversion, Publish and IPM Facelift VIN Update - told apart by the thread name prefix
			PreparedStatement ps = conn.prepareStatement("SELECT DC_SCHEDULE_NAME, DC_SCH_THREAD_ID FROM gms3_dmt_conv_schedule WHERE DC_STATUS IN (?, ?)");
			ps.setString(1, pending);
			ps.setString(2, processing);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String name = rs.getString(1);
				String thread = null == rs.getString(2) ? name : rs.getString(2);
				(isContentJob(thread) ? active.content : active.master).add(name);
			}
			rs.close();
			ps.close();
			// Document Identification (IPM Facelift step A)
			ps = conn.prepareStatement("SELECT fm_sch_name FROM gms3_dmt_fm_sch WHERE fm_processing_status IN (?, ?)");
			ps.setString(1, pending);
			ps.setString(2, processing);
			rs = ps.executeQuery();
			while (rs.next()) {
				active.master.add(rs.getString(1));
			}
			rs.close();
			ps.close();
			// Master Data Syncing
			ps = conn.prepareStatement("SELECT AS_SCHEDULE_NAME FROM gms3_auto_sync_sch WHERE AS_JOB_STATUS IN (?, ?)");
			ps.setString(1, AutoSyncConstants.STATUS_PENDING);
			ps.setString(2, AutoSyncConstants.STATUS_PROCESSING);
			rs = ps.executeQuery();
			while (rs.next()) {
				active.master.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		logger.info("active :: content jobs " + active.content + " :: facelift / identification / master data jobs " + active.master);
		return active;
	}

	/** The items of the Pending / Processing Conversion and Publish jobs. */
	public static List<Item> runningContentItems() throws Exception {
		List<Item> items = new ArrayList<Item>();
		Connection conn = DBConnectionHelper.getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT s.DC_SCHEDULE_NAME, s.DC_SCH_THREAD_ID, c.DC_LOCALE, c.DC_MODEL_FOLDER_NAME, c.DC_MANUAL_TYPE,"
					+ " c.DC_FACELIFT_FOLDER_NAME, c.DC_MATERIAL_NAME FROM gms3_dmt_conv_sch_criteria c JOIN gms3_dmt_conv_schedule s"
					+ " ON s.DC_SCHEDULE_ID = c.DC_SCHEDULE_ID WHERE s.DC_STATUS IN (?, ?)");
			ps.setString(1, KaptureApiClient.require("schedule.status.pending.value"));
			ps.setString(2, KaptureApiClient.require("schedule.status.processing.value"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String name = rs.getString(1);
				String thread = null == rs.getString(2) ? name : rs.getString(2);
				// a Facelift job's criteria columns hold other values (carline, channel, document type)
				if (!isContentJob(thread)) {
					continue;
				}
				Item item = new Item(rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), name);
				// an item without locale, model folder or manual type matches nothing (as before)
				if ("".equals(item.locale) || "".equals(item.modelFolder) || "".equals(item.manualType)) {
					continue;
				}
				items.add(item);
			}
			rs.close();
			ps.close();
		} finally {
			conn.close();
		}
		return items;
	}

	/** A Conversion (MC / MME / MNAO) or Publish job, by its thread name (or schedule name). */
	public static boolean isContentJob(String threadOrName) {
		if (null == threadOrName) {
			return false;
		}
		String n = threadOrName.trim();
		for (String key : new String[] { "schedule.name.key", "schedule.name.mc.key", "schedule.name.mme.key", "schedule.name.publish.mc.key",
				"schedule.name.publish.mme.key", "schedule.name.publish.mnao.key" }) {
			if (n.startsWith(KaptureApiClient.require(key).trim())) {
				return true;
			}
		}
		return false;
	}

	/** A Publish Content job (MC / MME / MNAO), by its thread name or schedule name. */
	public static boolean isPublishJob(String threadOrName) {
		if (null == threadOrName) {
			return false;
		}
		String n = threadOrName.trim();
		for (String key : new String[] { "schedule.name.publish.mc.key", "schedule.name.publish.mme.key", "schedule.name.publish.mnao.key" }) {
			if (n.startsWith(KaptureApiClient.require(key).trim())) {
				return true;
			}
		}
		return false;
	}

	private static boolean same(String a, String b) {
		return a.equalsIgnoreCase(b);
	}

	private static boolean covers(String a, String b) {
		return "".equals(a) || "".equals(b) || a.equalsIgnoreCase(b);
	}

	private static String t(String s) {
		return null == s ? "" : s.trim();
	}
}
