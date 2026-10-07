package com.mazda.gms3.dmt.publish;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.PrintReportsUtil;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;

/**
 * THE REPORTS OF A PUBLISH CONTENT JOB - the DMT pattern: PUBLISH_REPORT.xlsx in
 * REPORTS_DIRECTORY/<schedule id>/, its Job Summary row, then the folder zipped (the loose files
 * are removed) so History offers the zip.
 */
public class PublishReport {

	private static Logger logger = LogManager.getLogger(PublishReport.class);

	private static final String[] HEADER = { "WSL ID", "SCHEDULE ID", "SCHEDULED FROM JOB", "DOCUMENT ID", "TITLE", "CHANNEL", "LOCALE",
			"MODEL", "MODEL FOLDER", "MANUAL TYPE", "MATERIAL FOLDER", "VERSION BEFORE", "VERSION AFTER", "STATUS", "MESSAGE",
			"OTHER JOBS PREVIEW UPDATED" };

	public static void write(String publishScheduleId, String wslId, String sourceName, List<PublishDocument> docs) {
		try {
			String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY") + "/" + publishScheduleId.trim();
			File dir = PathUtil.file(path);
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}
			SXSSFWorkbook book = new SXSSFWorkbook(100);
			Sheet sheet = book.createSheet("Details");
			Row header = sheet.createRow(0);
			for (int i = 0; i < HEADER.length; i++) {
				header.createCell(i).setCellValue(HEADER[i]);
			}
			long success = 0;
			long failure = 0;
			int n = 0;
			for (PublishDocument d : docs) {
				if (null == d.result) {
					continue;
				}
				if (PublishDocument.RESULT_SUCCESS.equals(d.result)) {
					success++;
				} else if (PublishDocument.RESULT_FAILURE.equals(d.result)) {
					failure++;
				}
				Row row = sheet.createRow(++n);
				String[] v = { wslId, publishScheduleId, sourceName, d.documentId, d.title, d.channel, d.locale, d.model, d.modelFolder,
						d.manualType, d.materialFolder, d.version, d.publishedVersion, d.result, d.message, String.join(", ", d.otherJobs) };
				for (int i = 0; i < v.length; i++) {
					row.createCell(i).setCellValue(null == v[i] ? "" : v[i]);
				}
			}
			FileOutputStream os = PathUtil.fileOutputStream(PathUtil.file(path + "/PUBLISH_REPORT.xlsx"));
			try {
				book.write(os);
			} finally {
				os.close();
				book.dispose();
			}
			logger.info("write :: PUBLISH_REPORT.xlsx written :: rows=" + n + " published=" + success + " failed=" + failure);

			ReportsSummaryDetails rsd = new ReportsSummaryDetails();
			rsd.setSchduleCode(publishScheduleId);
			rsd.setReportName(ReportsSummaryDetails.REPORT_PUBLISH);
			rsd.setTotalCount((long) n);
			rsd.setSuccessCount(success);
			rsd.setFailureCount(failure);
			rsd.setReportStatus(failure > 0 ? ReportsSummaryDetails.STATUS_FAILURE : ReportsSummaryDetails.STATUS_SUCCESS);
			ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);

			new PrintReportsUtil().createReportsZip(path + "/", publishScheduleId);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PublishReport.class.getName(), "write()", e);
		}
	}
}
