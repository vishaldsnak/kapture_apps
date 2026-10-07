package com.mazda.gms3.dmt.masterdata;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.SourceContentPaths;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;

/**
 * THE "MASTER DATA WITH CONTENT" STEP OF A SCHEDULE - runs once, before any content is processed.
 *
 *   1. the master data Excel files of each locale of the schedule are loaded into the MDM
 *      tables (MdmExcelLoader) - new and changed rows get sync status N
 *   2. the rows waiting with sync status N are sent to Kapture as categories
 *      (KaptureCategorySync) - sync status Y on success
 *   3. two reports are written into the reports folder of the schedule and one line for each
 *      is added to the schedule summary - ONLY the rows that changed something (added, updated,
 *      deleted) or failed; a report with no such row is not written and has no summary line
 *
 * A failure here never stops the schedule: the content is processed afterwards, and a document
 * whose category is still missing is reported by the content processing itself. A failed row in
 * either report does mark the job as a FAILURE at the end, like a failure in the other reports.
 */
public class MasterDataLoad {

	private static Logger logger = LogManager.getLogger(MasterDataLoad.class);

	public static final String REPORT_FILE_LOAD = "MASTER_DATA_LOAD_REPORT.xlsx";
	public static final String REPORT_FILE_CATEGORIES = "MASTER_DATA_CATEGORY_REPORT.xlsx";

	/**
	 * @param settingsPath the source content path of the Settings page
	 * @param locales      the locales of the schedule items, e.g. ja-JP
	 * @return true when a row of either report FAILED - the job is then marked as a failure
	 */
	public static boolean run(String scheduleId, String settingsPath, String market, Collection<String> locales) {
		List<MasterDataRecord> loadRecords = new ArrayList<MasterDataRecord>();
		List<MasterDataRecord> categoryRecords = new ArrayList<MasterDataRecord>();
		try {
			ScheduleDAO.updateCurrentJobStatus(scheduleId, null, "MASTER DATA LOAD");
			File folder = SourceContentPaths.masterDataFolder(settingsPath);
			logger.info("run :: MASTER DATA LOAD for Schedule {" + scheduleId + "} from :: > " + PathUtil.winPath(folder));
			for (String locale : locales) {
				MdmExcelLoader loader = new MdmExcelLoader();
				if (loader.load(folder, locale)) {
					markBooksSynced(loader.getManualLanguageId());
				}
				loadRecords.addAll(loader.records);

				// THE ROWS WAITING FOR KAPTURE ARE SENT EVEN WHEN NO FILE WAS FOUND - rows left
				// over from an earlier run that could not reach Kapture are completed here
				ScheduleDAO.updateCurrentJobStatus(scheduleId, null, "MASTER DATA CATEGORY PROCESSING");
				KaptureCategorySync categories = new KaptureCategorySync();
				categories.sync(market, locale, ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim());
				categoryRecords.addAll(categories.records);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "run()", e);
			MasterDataRecord rec = new MasterDataRecord();
			rec.result = MasterDataRecord.RESULT_FAILED;
			rec.message = "Master data load stopped: " + e.getMessage();
			loadRecords.add(rec);
		}

		long failed = 0;
		for (MasterDataRecord rec : loadRecords) {
			failed += rec.failed() ? 1 : 0;
		}
		for (MasterDataRecord rec : categoryRecords) {
			failed += rec.failed() ? 1 : 0;
		}
		// the two reports are WRITTEN at the end of the job (writeReports), next to the other reports:
		// written here, an hour before the zip, they stayed visible on the S3 mount after being deleted
		PENDING_REPORTS.put(scheduleId.trim(), new List[] { loadRecords, categoryRecords });
		logger.info("run :: MASTER DATA LOAD for Schedule {" + scheduleId + "} finished :: failed rows :: > " + failed);
		return failed > 0;
	}

	/** schedule id -> {load records, category records} until the end of the job */
	@SuppressWarnings("rawtypes")
	private static final java.util.Map<String, List[]> PENDING_REPORTS = new java.util.concurrent.ConcurrentHashMap<String, List[]>();

	/**
	 * Writes the two master data reports of the job (and their Job Summary rows) - called with the
	 * other reports, just before they are zipped. Nothing to do when the job loaded no master data.
	 */
	/** End of every job: rows still held (the job stopped before its reports) are dropped. */
	public static void discardReports(String scheduleId) {
		if (null != scheduleId && null != PENDING_REPORTS.remove(scheduleId.trim())) {
			logger.info("discardReports :: master data report rows of Schedule {" + scheduleId + "} dropped - the job ended before its reports");
		}
	}

	@SuppressWarnings("unchecked")
	public static void writeReports(String scheduleId) {
		List<MasterDataRecord>[] r = PENDING_REPORTS.remove(scheduleId.trim());
		if (null == r) {
			return;
		}
		report(scheduleId, REPORT_FILE_LOAD, ReportsSummaryDetails.REPORT_MASTER_DATA_LOAD, r[0], false);
		report(scheduleId, REPORT_FILE_CATEGORIES, ReportsSummaryDetails.REPORT_MASTER_DATA_CATEGORIES, r[1], true);
	}

	/** Engine and transmission books are not categories - loading them into MDM is all there is to do. */
	private static void markBooksSynced(long manualLanguageId) {
		Connection conn = null;
		PreparedStatement ps = null;
		String[][] books = { { "gms3_mdm_engine_book", "mdm_eb_flag" }, { "gms3_mdm_trans_book", "mdm_transbk_flag" } };
		try {
			conn = DBConnectionHelper.getConnection();
			for (int b = 0; b < books.length; b++) {
				ps = conn.prepareStatement("UPDATE " + books[b][0] + " SET mdm_sync_status = 'Y' WHERE mdm_ml_id = ?"
						+ " AND mdm_sync_status = 'N' AND " + books[b][1] + " = ?");
				ps.setLong(1, manualLanguageId);
				ps.setString(2, ApplicationProperties.getProperty("flag.value.active"));
				ps.executeUpdate();
				ps.close();
				ps = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "markBooksSynced()", e);
		} finally {
			try {
				if (null != ps) {
					ps.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "markBooksSynced()", e);
			}
		}
	}

	/** @return the number of FAILED rows */
	private static long report(String scheduleId, String fileName, String reportName, List<MasterDataRecord> all, boolean categories) {
		// only what changed something or failed - a NO CHANGE row tells the business nothing
		List<MasterDataRecord> records = new ArrayList<MasterDataRecord>();
		for (MasterDataRecord rec : all) {
			if (!MasterDataRecord.RESULT_NO_CHANGE.equals(rec.result)) {
				records.add(rec);
			}
		}
		if (records.isEmpty()) {
			logger.info("report :: " + fileName + " NOT written for Schedule {" + scheduleId + "} :: no row was added, updated, deleted"
					+ " or failed (" + all.size() + " without change)");
			return 0;
		}
		long failures = 0;
		for (MasterDataRecord rec : records) {
			if (rec.failed()) {
				failures++;
			}
		}
		SXSSFWorkbook workbook = null;
		FileOutputStream out = null;
		try {
			String directory = ApplicationProperties.getProperty("REPORTS_DIRECTORY").trim();
			if (!directory.endsWith("/")) {
				directory = directory + "/";
			}
			directory = directory + scheduleId;
			File dir = PathUtil.file(directory);
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}

			workbook = new SXSSFWorkbook(100);
			Sheet sheet = workbook.createSheet("Details");
			CellStyle bold = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			bold.setFont(font);
			String[] headers = categories
					? new String[] { "Master Data Type", "Locale", "Category Level", "Category Ref Key", "Category Name", "Parent Ref Key", "Action", "Status", "Failure Reason" }
					: new String[] { "Master Data Type", "File Name", "Locale", "Row No", "Record", "Indicator", "Action", "Status", "Failure Reason" };
			Row row = sheet.createRow(0);
			for (int c = 0; c < headers.length; c++) {
				Cell cell = row.createCell(c);
				cell.setCellValue(headers[c]);
				cell.setCellStyle(bold);
				sheet.setColumnWidth(c, 6000);
			}
			int r = 1;
			for (MasterDataRecord rec : records) {
				// Action = what was done (ADDED / UPDATED / DELETED; blank for a failed row), Status = SUCCESS / FAILURE,
				// Failure Reason only for a failed row
				String action = rec.failed() ? "" : rec.result;
				String status = rec.failed() ? ReportsSummaryDetails.STATUS_FAILURE : ReportsSummaryDetails.STATUS_SUCCESS;
				String reason = rec.failed() ? rec.message : "";
				String[] v = categories
						? new String[] { rec.type, rec.locale, rec.categoryLevel, rec.categoryRefKey, rec.categoryName, rec.parentRefKey, action, status, reason }
						: new String[] { rec.type, rec.fileName, rec.locale, rec.rowNumber > 0 ? String.valueOf(rec.rowNumber) : "", rec.key, rec.indicator, action, status, reason };
				row = sheet.createRow(r++);
				for (int c = 0; c < v.length; c++) {
					row.createCell(c).setCellValue(null == v[c] ? "" : v[c]);
				}
			}
			out = PathUtil.fileOutputStream(directory + "/" + fileName);
			workbook.write(out);
			logger.info("report :: " + fileName + " written for Schedule {" + scheduleId + "} :: rows " + records.size() + " :: failures " + failures);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "report()", e);
		} finally {
			try {
				if (null != out) {
					out.close();
				}
				if (null != workbook) {
					workbook.dispose();
					workbook.close();
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "report()", e);
			}
		}

		try {
			ReportsSummaryDetails rsd = new ReportsSummaryDetails();
			rsd.setSchduleCode(scheduleId);
			rsd.setReportName(reportName);
			rsd.setTotalCount(Long.valueOf(records.size()));
			rsd.setFailureCount(Long.valueOf(failures));
			rsd.setSuccessCount(Long.valueOf(records.size() - failures));
			rsd.setReportStatus(failures > 0 ? ReportsSummaryDetails.STATUS_FAILURE : ReportsSummaryDetails.STATUS_SUCCESS);
			ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MasterDataLoad.class.getName(), "report()", e);
		}
		return failures;
	}
}
