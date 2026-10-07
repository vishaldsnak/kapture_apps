package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.MCViewContentDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.mc.vo.WindowJSDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.ErrorDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;
import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class PrintReportsUtil {

	Logger logger = LogManager.getLogger(PrintReportsUtil.class);

	private int partitionSize=700000;
	
	/**
	 * Function will proceed for generating reports.
	 * 
	 * @param wslId
	 * @param scheduleCode
	 */


	public boolean createReportsZip(String reportsDirectoryPath,String scheduleCode) 
	{
		try
		{
			/*
			 * Path for the ZIP File will be same as reportsDirectoryPath and
			 * name will be - schDate_REPOROTS.ZIP
			 */
			if (null != reportsDirectoryPath && !"".equals(reportsDirectoryPath) && null != scheduleCode
					&& !"".equals(scheduleCode)) 
			{
				String zipPath = reportsDirectoryPath	+ scheduleCode	+ "_"	+ ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
				File file = PathUtil.file(reportsDirectoryPath);
				if (file.exists() && file.isDirectory()) 
				{
					FileOutputStream fos = PathUtil.fileOutputStream(zipPath);
					ZipOutputStream zos = new ZipOutputStream(fos);
					File[] childFiles = file.listFiles();
					for (int a = 0; a < childFiles.length; a++) {
						File reports = childFiles[a];
						if (reports.exists()
								&& reports.isFile()
								&& !reports
								.getName()
								.contains(
										ApplicationProperties
										.getProperty("REPORTS_ZIP_SUFFIX"))) {
							addToZipFile(reports, zos);
						}
					}
					childFiles = null;
					zos.close();
					fos.close();
				}
				file = null;

				/*
				 * Now check here, if the Zip file is generated, then check for
				 * all the other files and delete them inside the directory
				 */
				File zip = PathUtil.file(zipPath);
				if (zip.exists() && zip.length() > 0) 
				{
					logger.info("createReportsZip :: Zip file {"
							+ scheduleCode
							+ "_"
							+ ApplicationProperties
							.getProperty("REPORTS_ZIP_SUFFIX")
							+ "} Generated Successfully");
					/*
					 * Proceed for deleting all the other files
					 */
					File excelFiles = PathUtil.file(reportsDirectoryPath);
					if (excelFiles.exists() && excelFiles.isDirectory()) {
						File[] childFiles = excelFiles.listFiles();
						for (int a = 0; a < childFiles.length; a++) {
							File excelReports = childFiles[a];
							if (excelReports.exists() && excelReports.isFile()) 
							{
								if (!excelReports
										.getName()
										.contains(
												ApplicationProperties
												.getProperty("REPORTS_ZIP_SUFFIX"))) {
									// a report left behind shows next to the zip in the Reports dialog - say why
									try {
										java.nio.file.Files.delete(excelReports.toPath());
										logger.info("createReportsZip :: Deleted after zipping :: > " + excelReports.getName());
									} catch (Exception de) {
										logger.info("createReportsZip :: COULD NOT DELETE after zipping :: > " + excelReports.getName() + " :: " + de);
									}
								}
								excelReports = null;
							}
						}
						childFiles = null;
					}
					excelFiles = null;
				} else {
					logger.info("createReportsZip :: Failed to generate Zip file {"
							+ scheduleCode
							+ "_"
							+ ApplicationProperties
							.getProperty("REPORTS_ZIP_SUFFIX")
							+ "}. Download the Excel reports manually.");
					return false;
				}
				zipPath = null;
			} else {
				logger.info("createReportsZip :: Reports Directory Path and Scheduled Date is passed as null in parameters. Return false");
				return false;
			}
		} catch (FileNotFoundException e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: Input Output Found Exception :: >"
					+ e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Function will add each Child file to the Zip file
	 * 
	 * @param childFile
	 * @param zos
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void addToZipFile(File childFile, ZipOutputStream zos)
			throws FileNotFoundException, IOException {
		logger.info("addToZipFile :: Writing '" + childFile.getName()
		+ "' to zip file");
		FileInputStream fis = PathUtil.fileInputStream(childFile);
		zos.putNextEntry(new ZipEntry(childFile.getName()));
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	/**
	 * Function will print one single report for all the errors generated
	 * 
	 * @param wslId
	 * @param scheduleCode
	 */
	@SuppressWarnings("resource")
	public void printSingleFailureReport(String scheduleCode, ArrayList<ErrorDetails> imErrorDetailsList, ArrayList<Map<Object, Object>> databaseFailureList, ArrayList<Map<Object, Object>> xcopyFailureList, ArrayList<ErrorDetails> imErrorDeleteDetailsList, ArrayList<Map<Object, Object>> databaseFailureListForDelete) 
	{if (logger.isInfoEnabled())
		logger.info("printSingleFailureReport :: Method Starts.");
	try {
		if (null != scheduleCode && !"".equals(scheduleCode)) 
		{
			boolean proceedForErrorReport = false;

			if ((null != imErrorDetailsList && imErrorDetailsList.size() > 0) || (null != databaseFailureList && databaseFailureList.size() > 0)
					|| (null != xcopyFailureList && xcopyFailureList.size() > 0) || (null != imErrorDeleteDetailsList && imErrorDeleteDetailsList.size() > 0) || 
					(null != databaseFailureListForDelete && databaseFailureListForDelete.size() > 0))
			{
				/*
				 * Either of the Operations Failed.
				 */
				proceedForErrorReport = true;
			}

			if (proceedForErrorReport == true) 
			{
				long totalCount=0;
				long failureCount=0;
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = PathUtil.file(path);
				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) 
				{
					// create new directory
					wslDirectory.mkdir();
				}
				wslDirectory = null;
				/*
				 * Now check, whether the schedule code directory exists or
				 * not
				 */
				path = path + "/" + scheduleCode;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * iterate Map and write the Excel File for each Index
				 */
				String fName = "/" +  "FAILURE_REPORT.xlsx";
				File myFile = PathUtil.file(path + fName);
				fName = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Failure Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell fileCell = headerRow.createCell(0);
				fileCell.setCellValue("FILE_LOCATION");
				Cell fileNameCell = headerRow.createCell(1);
				fileNameCell.setCellValue("FILE_NAME");
				Cell failureType = headerRow.createCell(2);
				failureType.setCellValue("FAILURE_TYPE");
				Cell localeNameCell = headerRow.createCell(3);
				localeNameCell.setCellValue("LOCALE");
				Cell channelNameCell = headerRow.createCell(4);
				channelNameCell.setCellValue("CHANNEL");
				Cell modelTypeCell = headerRow.createCell(5);
				modelTypeCell.setCellValue("MODEL_TYPE");
				Cell modelCell = headerRow.createCell(6);
				modelCell.setCellValue("MODEL");
				Cell carlineCodeCell = headerRow.createCell(7);
				carlineCodeCell.setCellValue("CARLINE_CODE");
				Cell manualTypeCell = headerRow.createCell(8);
				manualTypeCell.setCellValue("MANUAL_TYPE");
				Cell operationTypeCell = headerRow.createCell(9);
				operationTypeCell.setCellValue("OPERATION_TYPE");
				Cell errorCodeCell = headerRow.createCell(10);
				errorCodeCell.setCellValue("ERROR_CODE");
				Cell errorMessageCell = headerRow.createCell(11);
				errorMessageCell.setCellValue("ERROR_MESSAGE");
				Cell imDocumentIdCell = headerRow.createCell(12);
				imDocumentIdCell.setCellValue("IM_DOCUMENT_ID");
				Cell destinationPathCell = headerRow.createCell(13);
				destinationPathCell.setCellValue("DESTINATION_PATH");
				Cell dateTimeStampCell = headerRow.createCell(14);
				dateTimeStampCell.setCellValue("DATE_TIME");
				ArrayList<ErrorDetails> imErrorList = new ArrayList<ErrorDetails>();
				if (null != imErrorDetailsList && imErrorDetailsList.size() > 0) 
				{
					imErrorList.addAll(imErrorDetailsList);
				}
				if (null != imErrorDeleteDetailsList && imErrorDeleteDetailsList.size() > 0) 
				{
					imErrorList.addAll(imErrorDeleteDetailsList);
				}
				int rowCount = 0;
				// ADD INFO MANAGER FAILURE REPORT
				if (null != imErrorList && imErrorList.size() > 0) 
				{
					// add to totalCount
					totalCount =imErrorList.size();
					for (int i = 0; i < imErrorList.size(); i++) 
					{
						ErrorDetails errorDetails = (ErrorDetails) imErrorList.get(i);
						ContentDetails con = new ContentDetails();
						if (null != errorDetails.getContentDetails()) 
						{
							con = errorDetails.getContentDetails();
						}


						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						if (null != con.getFileAbsolutePath()  && !"".equals(con.getFileAbsolutePath())) 
						{
							cell0.setCellValue(String.valueOf(con.getFileAbsolutePath()));
						}
						else 
						{
							cell0.setCellValue("");
						}
						Cell cell1 = row.createCell(1);
						if (null != con.getFileName()  && !"".equals(con.getFileName())) 
						{
							cell1.setCellValue(String.valueOf(con.getFileName()));
						}
						else 
						{
							cell1.setCellValue("");
						}

						Cell cell2 = row.createCell(2);
						cell2.setCellValue("INFOMANAGER_FAILURE");

						Cell cell3 = row.createCell(3);
						if (null != con.getLocale() && !"".equals(con.getLocale())) 
						{
							cell3.setCellValue(ApplicationProperties.getProperty(con.getLocale().trim().toLowerCase()));
						} 
						else 
						{
							cell3.setCellValue("");
						}

						Cell cell4 = row.createCell(4);
						if (null != con.getChannelName()  && !"".equals(con.getChannelName())) 
						{
							cell4.setCellValue(String.valueOf(con.getChannelName()));
						} 
						else 
						{
							cell4.setCellValue("");
						}

						Cell cell5 = row.createCell(5);
						if (null != con.getModelType()  && !"".equals(con.getModelType())) 
						{
							cell5.setCellValue(String.valueOf(con.getModelType()));
						} 
						else 
						{
							cell5.setCellValue("");
						}
						Cell cell6 = row.createCell(6);
						if (null != con.getModel()  && !"".equals(con.getModel())) 
						{
							cell6.setCellValue(String.valueOf(con.getModel()));
						} 
						else 
						{
							cell6.setCellValue("");
						}
						Cell cell7 = row.createCell(7);
						if (null != con.getCarlineCode() && !"".equals(con.getCarlineCode()))
						{
							cell7.setCellValue(con.getCarlineCode());
						} 
						else 
						{
							cell7.setCellValue("");
						}
						Cell cell8 = row.createCell(8);
						if (null != con.getManualType() && !"".equals(con.getManualType()))
						{
							cell8.setCellValue(con.getManualType());
						} 
						else 
						{
							cell8.setCellValue("");
						}

						Cell cell9 = row.createCell(9);
						if (null != errorDetails.getOperationType()  && !"".equals(errorDetails.getOperationType())) 
						{
							cell9.setCellValue(String.valueOf(errorDetails.getOperationType()));
						} 
						else 
						{
							cell9.setCellValue("");
						}

						Cell cell10 = row.createCell(10);
						if (null != errorDetails.getErrorCode()  && !"".equals(errorDetails.getErrorCode())) 
						{
							cell10.setCellValue(String.valueOf(errorDetails.getErrorCode()));
						} 
						else 
						{
							cell10.setCellValue("");
						}

						Cell cell11 = row.createCell(11);
						if (null != errorDetails.getErrorMessage() && !"".equals(errorDetails.getErrorMessage())) 
						{
							cell11.setCellValue(String.valueOf(errorDetails.getErrorMessage()));
						}
						else 
						{
							cell11.setCellValue("");
						}

						Cell cell12 = row.createCell(12);
						if (null != con.getImDocumentId() && !"".equals(con.getImDocumentId())) 
						{
							cell12.setCellValue(con.getImDocumentId());
						}
						else 
						{
							cell12.setCellValue("");
						}

						Cell cell14 = row.createCell(14);
						if (null != errorDetails.getDateTime()) 
						{
							SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
							cell14.setCellValue(String.valueOf(sdf.format(errorDetails.getDateTime())));
							sdf = null;
						}
						else 
						{
							cell14.setCellValue("");
						}
						cell11 = null;
						cell12 = null;
						cell14 = null;
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						cell7=null;
						cell8=null;
						cell9 = null;
						cell10 = null;
						row = null;
						errorDetails = null;
						con = null;
					}
				}
				imErrorList=  null;
				// WRITE DATABASE FAILURE REPORT
				ArrayList<Map<Object, Object>> dbErrorList = new ArrayList<Map<Object, Object>>();
				if (null != databaseFailureList && databaseFailureList.size() > 0) 
				{
					dbErrorList.addAll(databaseFailureList);
				}
				if(null!=databaseFailureListForDelete && databaseFailureListForDelete.size()>0)
				{
					dbErrorList.addAll(databaseFailureListForDelete);
				}
				if (null != dbErrorList && dbErrorList.size() > 0) 
				{
					// add to totalCount
					totalCount = totalCount + dbErrorList.size();
					for (int i = 0; i < dbErrorList.size(); i++) 
					{
						Map<Object, Object> dataMap = (HashMap<Object, Object>) dbErrorList.get(i);
						ContentDetails con = new ContentDetails();
						if(null!=dataMap.get("CONTENT_DETAILS") && !"".equals(dataMap.get("CONTENT_DETAILS")))
						{
							con =(ContentDetails) dataMap.get("CONTENT_DETAILS");
						}

						// increment rowCount by 1
						rowCount++;

						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						Cell cell7 = row.createCell(7);
						Cell cell8 = row.createCell(8);
						Cell cell9 = row.createCell(9);
						Cell cell10 = row.createCell(10);
						Cell cell11 = row.createCell(11);
						Cell cell12 = row.createCell(12);
						Cell cell14 = row.createCell(14);
						// set all cell values to default

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						cell7.setCellValue("");
						cell8.setCellValue("");
						cell9.setCellValue("");
						cell10.setCellValue("");
						cell11.setCellValue("");
						cell12.setCellValue("");
						cell14.setCellValue("");

						if (null != con.getFileAbsolutePath() && !"".equals(con.getFileAbsolutePath())) 
						{
							cell0.setCellValue(con.getFileAbsolutePath());
						}
						if(null!=con.getFileName() && !"".equals(con.getFileName()))
						{
							cell1.setCellValue(con.getFileName());
						}
						// set DATABASE_FAILURE
						cell2.setCellValue("DATABASE_FAILURE");

						if (null != dataMap.get("LOCALE")) {
							cell3.setCellValue((String) dataMap.get("LOCALE"));
						}

						if (null != dataMap.get("CHANNEL")) {
							cell4.setCellValue((String) dataMap.get("CHANNEL"));
						}
						if(null!=con.getModelType() && !"".equals(con.getModelType()))
						{
							cell5.setCellValue(con.getModelType());
						}
						if(null!=con.getModel() && !"".equals(con.getModel()))
						{
							cell6.setCellValue(con.getModel());
						}
						if(null!=con.getCarlineCode() && !"".equals(con.getCarlineCode()))
						{
							cell7.setCellValue(con.getCarlineCode());
						}
						if(null!=con.getManualType() && !"".equals(con.getManualType()))
						{
							cell8.setCellValue(con.getManualType());
						}
						if (null != dataMap.get("OPERATION_TYPE")) {
							cell9.setCellValue((String) dataMap.get("OPERATION_TYPE"));
						}
						if (null != dataMap.get("ERROR_CODE")) {
							cell10.setCellValue((String) dataMap.get("ERROR_CODE"));
						}
						if (null != dataMap.get("ERROR_MESSAGE")) {
							cell11.setCellValue((String) dataMap.get("ERROR_MESSAGE"));
						}

						if (null != dataMap.get("IM_DOCUMENT_ID")) {
							cell12.setCellValue((String) dataMap.get("IM_DOCUMENT_ID"));
						}

						if (null != dataMap.get("DATE_TIME")) {
							cell14.setCellValue((String) dataMap.get("DATE_TIME"));
						}

						// set all used variables to null
						cell14 = null;
						cell12 = null;
						cell11 = null;
						cell10 = null;
						cell9 = null;
						cell7 = null;
						cell6 = null;
						cell5 = null;
						cell4 = null;
						cell3 = null;
						cell2 = null;
						cell1 = null;
						cell0 = null;
						row = null;
						con = null;
						dataMap = null;
					}
				}
				dbErrorList = null;

				// WRITE XCOPY FAILURE REPORT

				if (null != xcopyFailureList && xcopyFailureList.size() > 0) 
				{
					// add to totalCount
					totalCount  =totalCount + xcopyFailureList.size();
					for (int i = 0; i < xcopyFailureList.size(); i++) 
					{
						Map<Object, Object> dataMap = (HashMap<Object, Object>) xcopyFailureList.get(i);
						ContentDetails con = new ContentDetails();
						if(null!=dataMap.get("CONTENT_DETAILS") && !"".equals(dataMap.get("CONTENT_DETAILS")))
						{
							con =(ContentDetails) dataMap.get("CONTENT_DETAILS");
						}

						// increment rowCount by 1
						rowCount++;

						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						Cell cell7 = row.createCell(7);
						Cell cell8 = row.createCell(8);
						Cell cell11 = row.createCell(11);
						Cell cell12 = row.createCell(12);
						Cell cell13 = row.createCell(13);
						Cell cell14 = row.createCell(14);
						// set all cell values to default

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						cell7.setCellValue("");
						cell8.setCellValue("");
						cell11.setCellValue("");
						cell12.setCellValue("");
						cell13.setCellValue("");
						cell14.setCellValue("");

						if (null != dataMap.get("SOURCE_PATH")) 
						{
							cell0.setCellValue((String) dataMap.get("SOURCE_PATH"));
						}
						if (null != dataMap.get("FILE_NAME")) 
						{
							cell1.setCellValue((String) dataMap.get("FILE_NAME"));
						}
						// SET XCOPY FAILURE
						cell2.setCellValue("XCOPY_FAILURE");
						if (null != con.getLocale() && !"".equals(con.getLocale())) {
							cell3.setCellValue(con.getLocale());
						}
						if (null != con.getChannelName() && !"".equals(con.getChannelName())) {
							cell4.setCellValue(con.getChannelName());
						}
						if(null!=con.getModelType() && !"".equals(con.getModelType()))
						{
							cell5.setCellValue(con.getModelType());
						}
						if(null!=con.getModel() && !"".equals(con.getModel()))
						{
							cell6.setCellValue(con.getModel());
						}
						if(null!=con.getCarlineCode() && !"".equals(con.getCarlineCode()))
						{
							cell7.setCellValue(con.getCarlineCode());
						}
						if(null!=con.getManualType() && !"".equals(con.getManualType()))
						{
							cell8.setCellValue(con.getManualType());
						}
						if (null != dataMap.get("ERROR_MESSAGE")) {
							cell11.setCellValue((String) dataMap.get("ERROR_MESSAGE"));
						}

						if(null!=con.getImDocumentId() && !"".equals(con.getImDocumentId()))
						{
							cell12.setCellValue(con.getImDocumentId());
						}
						if (null != dataMap.get("DESTINATION_PATH")) {
							cell13.setCellValue((String) dataMap.get("DESTINATION_PATH"));
						}

						if (null != dataMap.get("DATE_TIME")) {
							cell14.setCellValue((String) dataMap.get("DATE_TIME"));
						}

						// set all used variables to null
						cell14 = null;
						cell13 = null;
						cell12 = null;
						cell11 = null;
						cell7 = null;
						cell6 = null;
						cell5 = null;
						cell4 = null;
						cell3 = null;
						cell2 = null;
						cell1 = null;
						cell0 = null;
						row = null;
						con = null;
						dataMap = null;
					}
				}

				headerRow = null;
				localeNameCell = null;
				channelNameCell = null;
				operationTypeCell = null;
				errorCodeCell = null;
				errorMessageCell = null;
				fileCell = null;

				/*
				 * Before Writing check for size if equals to or more than
				 * 10 MB then generate a file with a extension to it.
				 */

				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on FAILURE REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				// set mySheet to null
				mySheet = null;
				// set myWorkBook to null
				myWorkBook = null;
				// set path to null
				path = null;
				// set myFile to null
				myFile = null;
				/*
				 * PROCEED FOR SAVING SUMMARY REPORT
				 */
				if(totalCount>0)
				{
					// calculate failure Count
					failureCount =totalCount;
					// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
					ReportsSummaryDetails rsd = new ReportsSummaryDetails();
					rsd.setSchduleCode(scheduleCode);
					rsd.setFailureCount(failureCount);
					rsd.setTotalCount(totalCount);
					// set report Name as Transaction Report
					rsd.setReportName(ReportsSummaryDetails.REPORT_FAILURE);
					// set status, if failure Count is more than 0 - then failure
					if(failureCount>0)
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
					}
					else
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
					}
					// UPDATE SUMMARY DETAILS
					try
					{
						ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printSingleFailureReport()", e);
					}
					rsd = null;
				}
				totalCount = 0;
				failureCount = 0;
			}
		}
	} catch (Exception e) {
		Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printSingleFailureReport()", e);
	}
	if (logger.isInfoEnabled())
		logger.info("printSingleFailureReport :: Method Ends.");
	}

	/**
	 * Function will write the report for Missing Categories in Source Content
	 * 
	 * @param wslId
	 * @param scheduleCode
	 * @param schDate
	 */
	@SuppressWarnings("resource")
	public void printMissingCategoriesReport(String scheduleCode , ArrayList<CategoryDetails> missingCategoryList) {
		if (logger.isInfoEnabled())
			logger.info("printMissingCategoriesReport :: Method Starts.");
		try {
			if (null != missingCategoryList && missingCategoryList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "MISSING_CATEGORIES.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Failure Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell fileCell = headerRow.createCell(0);
					fileCell.setCellValue("FILE_LOCATION");
					Cell fileNameCell = headerRow.createCell(1);
					fileNameCell.setCellValue("FILE_NAME");
					Cell imDocumentIdCell = headerRow.createCell(2);
					imDocumentIdCell.setCellValue("DOCUMENT ID");
					Cell operationTypeCell = headerRow.createCell(3);
					operationTypeCell.setCellValue("OPERATION TYPE");
					Cell catNameCell = headerRow.createCell(4);
					catNameCell.setCellValue("CATEGORY_NAME");
					Cell catRefKeyCell = headerRow.createCell(5);
					catRefKeyCell.setCellValue("REF KEY");
					Cell catTypeCell = headerRow.createCell(6);
					catTypeCell.setCellValue("CATEGORY TYPE");
					Cell errorCodeCell = headerRow.createCell(7);
					errorCodeCell.setCellValue("ERROR CODES");
					Cell errorMessagesCell = headerRow.createCell(8);
					errorMessagesCell.setCellValue("ERROR MESSAGES");
					// set totalCount
					totalCount = missingCategoryList.size();
					int rowCount = 0;
					for (int a = 0; a < missingCategoryList.size(); a++) {
						CategoryDetails catDetails = (CategoryDetails) missingCategoryList.get(a);

						ContentDetails con = new ContentDetails();
						if(null!=catDetails.getContentDetails() && !"".equals(catDetails.getContentDetails()))
						{
							con = catDetails.getContentDetails();
						}

						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						cell0.setCellValue("");
						if (null != con.getFileAbsolutePath() && !"".equals(con.getFileAbsolutePath())) 
						{
							cell0.setCellValue(con.getFileAbsolutePath());
						}

						Cell cell1 = row.createCell(1);
						cell1.setCellValue("");
						if(null!=con.getFileName() && !"".equals(con.getFileName()))
						{
							cell1.setCellValue(con.getFileName());
						}

						Cell cell2 = row.createCell(2);
						cell2.setCellValue("");
						if(null!=con.getImDocumentId() && !"".equals(con.getImDocumentId()))
						{
							cell2.setCellValue(con.getImDocumentId());
						}

						Cell cell3 = row.createCell(3);
						cell3.setCellValue("");
						if(null!=catDetails.getOperationType() && !"".equals(catDetails.getOperationType()))
						{
							cell3.setCellValue(catDetails.getOperationType());
						}

						Cell cell4 = row.createCell(4);
						cell4.setCellValue("");
						if (null != catDetails.getCategoryName() && !"".equals(catDetails.getCategoryName())) 
						{
							cell4.setCellValue(catDetails.getCategoryName());
						}

						Cell cell5 = row.createCell(5);
						cell5.setCellValue("");
						if (null != catDetails.getCategoryRefKey() 	&& !"".equals(catDetails.getCategoryRefKey())) 
						{
							cell5.setCellValue(catDetails.getCategoryRefKey());
						}

						Cell cell6 = row.createCell(6);
						cell6.setCellValue("");
						if (null != catDetails.getCategoryType() && !"".equals(catDetails.getCategoryType())) 
						{
							cell6.setCellValue(catDetails.getCategoryType());
						}

						Cell cell7 = row.createCell(7);
						cell7.setCellValue("");
						if (null != catDetails.getErrorCodes() && !"".equals(catDetails.getErrorCodes())) 
						{
							cell7.setCellValue(catDetails.getErrorCodes());
						}

						Cell cell8 = row.createCell(8);
						cell8.setCellValue("");
						if (null != catDetails.getErrorMessage() && !"".equals(catDetails.getErrorMessage())) 
						{
							cell8.setCellValue(catDetails.getErrorMessage());
						}

						cell8=null;
						cell7=null;
						cell6 = null;
						cell5 = null;
						cell4 = null;
						cell3 = null;
						cell2 = null;
						cell1 = null;
						cell0 = null;
						row = null;
						con = null;
						catDetails = null;
					}

					headerRow = null;
					catNameCell = null;
					catRefKeyCell = null;
					fileCell = null;
					fileNameCell = null;
					imDocumentIdCell= null;
					catTypeCell=  null;
					operationTypeCell=null;
					errorCodeCell=null;
					errorMessagesCell = null;

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on MISSING CATEGORIES REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_MISSINGCATEGORIES);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMissingCategoriesReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMissingCategoriesReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printMissingCategoriesReport :: Method Ends.");
	}

	/**
	 * Function will print the report of all the processing files with
	 * processing status
	 * 
	 * @param wslId
	 * @param scheduleCode
	 */
	@SuppressWarnings("resource")
	public void printProcessingFilesReport(String scheduleCode, ArrayList<ContentDetails> processingFileDetailsList, ArrayList<Map<Object, Object>> xcopyFileProcessingList,  ArrayList<ContentDetails> processingFileDetailsListForDelete)
	{
		if (logger.isInfoEnabled())
			logger.info("printProcessingFilesReport :: Method Starts.");
		try {
			boolean proceedForReport = true;
			if((null==processingFileDetailsList || processingFileDetailsList.size()<=0) && (null==xcopyFileProcessingList || xcopyFileProcessingList.size()<=0) && 
					(null==processingFileDetailsListForDelete || processingFileDetailsListForDelete.size()<=0))
			{
				proceedForReport = false;
			}
			if (proceedForReport==true && null != scheduleCode && !"".equals(scheduleCode)) 
			{
				long totalCount=0;
				long successCount=0;
				long failureCount=0;
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
				/*
				 * write the string Builder Text to a separate Log file parallel
				 * to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = PathUtil.file(path);
				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) 
				{
					// create new directory
					wslDirectory.mkdir();
				}
				wslDirectory = null;
				/*
				 * Now check, whether the schedule code directory exists or not
				 */
				path = path + "/" + scheduleCode;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * iterate Map and write the Excel File for each Index
				 */
				String fName = "/" + "TRANSACTION_REPORT.xlsx";
				File myFile = PathUtil.file(path + fName);
				fName = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Processing Details");

				/*
				 * Add Header Row
				 * WSL_ID
				 * SCHEDULE CODE
				 * MARKET
				 * LOCALE
				 * MODEL TYPE
				 * MODEL
				 * CARLINE CODE
				 * MODEL FOLDER NAME
				 * MANUAL TYPE
				 * FACELIFT FOLDER NAME
				 * MATERIAL NAME
				 * CHANNEL
				 * DOCUMENT ID
				 * DOCUMENT VERSION
				 * RESOURCE PATH
				 * SOURCE FILE LOCATION
				 * FILE NAME
				 * PROCESSING STATUS - SUCCESS / FAILURE
				 * OKASSETS DIRECTORY LOCATION
				 * REMARKS
				 * DATE TIME
				 * ESI CATEGORY MAPPED
				 * ESI CATEGORY TAXANOMY
				 * ESI CATEGORY MAPPED LEVEL
				 * ESI CATEGORY  MAPPED REFKEY
				 * ESI CATEGOTRY TEXT FILE NAME
				 * VIN MAPPED 
				 * VIN MAPPED TEXT FILE NAME
				 * DISPLAY ORDER MAPPED
				 * DISPLAY ORDER MAPPED FILE NAME
				 * TITLE
				 */
				Row headerRow = mySheet.createRow(0);

				Cell wslIdCell = headerRow.createCell(0);
				wslIdCell.setCellValue("WSL ID");
				Cell scheduleCodeCell = headerRow.createCell(1);
				scheduleCodeCell.setCellValue("SCHEDULE ID");
				Cell marketCell = headerRow.createCell(2);
				marketCell.setCellValue("MARKET");
				Cell localeCell = headerRow.createCell(3);
				localeCell.setCellValue("LOCALE");
				Cell modelTypeCell = headerRow.createCell(4);
				modelTypeCell.setCellValue("MODEL TYPE");
				Cell modelCell = headerRow.createCell(5);
				modelCell.setCellValue("MODEL");
				Cell carlineCodeCell = headerRow.createCell(6);
				carlineCodeCell.setCellValue("CARLINE CODE");
				Cell modelFolderCell = headerRow.createCell(7);
				modelFolderCell.setCellValue("MODEL FOLDER NAME");
				Cell manualTypeCell = headerRow.createCell(8);
				manualTypeCell.setCellValue("MANUAL TYPE");
				Cell faceLiftFolderNameCell = headerRow.createCell(9);
				faceLiftFolderNameCell.setCellValue("FACELIFT FOLDER NAME");
				Cell materialNameCell = headerRow.createCell(10);
				materialNameCell.setCellValue("MATERIAL NAME");
				Cell channelCell = headerRow.createCell(11);
				channelCell.setCellValue("CHANNEL");
				Cell imDocumentIdCell = headerRow.createCell(12);
				imDocumentIdCell.setCellValue("DOCUMENT ID");
				Cell currentVersionCell = headerRow.createCell(13);
				currentVersionCell.setCellValue("DOCUMENT VERSION");
				Cell sourceFileLocCell = headerRow.createCell(14);
				sourceFileLocCell.setCellValue("SOURCE FILE LOCATION");
				Cell sourceFileNameCell = headerRow.createCell(15);
				sourceFileNameCell.setCellValue("SOURCE FILE NAME");
				Cell processingStatusCell = headerRow.createCell(16);
				processingStatusCell.setCellValue("PROCESSING STATUS");
				Cell xcopyprocessingStatusCell = headerRow.createCell(17);
				xcopyprocessingStatusCell.setCellValue("XCOPY STATUS");
				Cell okAssetsDestinationCell = headerRow.createCell(18);
				okAssetsDestinationCell.setCellValue("DESTINATINATION DIRECTORY");
				Cell remarksCell = headerRow.createCell(19);
				remarksCell.setCellValue("REMARKS");
				Cell dateTimeCell = headerRow.createCell(20);
				dateTimeCell.setCellValue("TIME");

				Cell esiCategoryMappedCell = headerRow.createCell(21);
				esiCategoryMappedCell.setCellValue("ESI MAPPED");
				Cell esiCategoryTaxonomyCell = headerRow.createCell(22);
				esiCategoryTaxonomyCell.setCellValue("ESI TAXANOMY");
				Cell esiCategoryMappedLevelCell = headerRow.createCell(23);
				esiCategoryMappedLevelCell.setCellValue("ESI MAPPED LEVEL");
				Cell esiCategoryMappedRefKey = headerRow.createCell(24);
				esiCategoryMappedRefKey.setCellValue("ESI MAPPED REFKEY");
				Cell esiCategoryFileNameCell = headerRow.createCell(25);
				esiCategoryFileNameCell.setCellValue("ESI TEXT FILE NAME");

				Cell vinMappedCell = headerRow.createCell(26);
				vinMappedCell.setCellValue("VIN MAPPED");
				Cell vinMappedFileNameCell = headerRow.createCell(27);
				vinMappedFileNameCell.setCellValue("VIN TEXT FILE NAME");
				Cell dispOrdMappedCell = headerRow.createCell(28);
				dispOrdMappedCell.setCellValue("DISPLAY ORDER MAPPED");
				Cell dispOrdMappedFileNameCell = headerRow.createCell(29);
				dispOrdMappedFileNameCell.setCellValue("DISPLAY ORDER TEXT FILE NAME");
				Cell titleCell = headerRow.createCell(30);
				titleCell.setCellValue("TITLE");

				Cell innerLinkFoundCell = headerRow.createCell(31);
				innerLinkFoundCell.setCellValue("INNER LINK FOUND");
				Cell allInnerLinksMappedCell = headerRow.createCell(32);
				allInnerLinksMappedCell.setCellValue("ALL INNER LINKS MAPPED");
				Cell innerLinkNotMappedReasonCell = headerRow.createCell(33);
				innerLinkNotMappedReasonCell.setCellValue("INNER LINK MAPPING FAILURE REASON");

				int rowCount = 0;
				// ADD INFO

				boolean mmeMarket=false;
				ArrayList<ContentDetails> transactionsReportsList = new ArrayList<ContentDetails>();
				if (null != processingFileDetailsList && processingFileDetailsList.size() > 0) 
				{
					transactionsReportsList.addAll(processingFileDetailsList);
				}

				if(null!=processingFileDetailsListForDelete && processingFileDetailsListForDelete.size()>0)
				{
					transactionsReportsList.addAll(processingFileDetailsListForDelete);
				}

				if (null != transactionsReportsList && transactionsReportsList.size() > 0) 
				{
					// set transactionsReportsList as totalCount
					totalCount= transactionsReportsList.size();

					for (int i = 0; i < transactionsReportsList.size(); i++) 
					{
						ContentDetails contentDetails = (ContentDetails) transactionsReportsList.get(i);

						if(null!=contentDetails.getMarket() && contentDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
						{
							mmeMarket=true;
							break;
						}
					}

					if(mmeMarket==true)
					{
						Cell steeringTypeCell=headerRow.createCell(34);
						steeringTypeCell.setCellValue("STEERING TYPE");
					}
					else
					{
						// MC Market
						Cell metaTagDescCell = headerRow.createCell(34);
						metaTagDescCell.setCellValue("META TAG - DESCRIPTION");
					}


					for (int i = 0; i < transactionsReportsList.size(); i++) 
					{
						ContentDetails contentDetails = (ContentDetails) transactionsReportsList.get(i);

						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						// START ADDING DATA
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						Cell cell7 = row.createCell(7);
						Cell cell8 = row.createCell(8);
						Cell cell9 = row.createCell(9);
						Cell cell10 = row.createCell(10);
						Cell cell11 = row.createCell(11);
						Cell cell12 = row.createCell(12);
						Cell cell13 = row.createCell(13);
						Cell cell14 = row.createCell(14);
						Cell cell15 = row.createCell(15);
						Cell cell16 = row.createCell(16);
						Cell cell17 = row.createCell(17);
						Cell cell18 = row.createCell(18);
						Cell cell19 = row.createCell(19);
						Cell cell20 = row.createCell(20);
						Cell cell21 = row.createCell(21);
						Cell cell22 = row.createCell(22);
						Cell cell23 = row.createCell(23);
						Cell cell24 = row.createCell(24);
						Cell cell25 = row.createCell(25);
						Cell cell26 = row.createCell(26);
						Cell cell27 = row.createCell(27);
						Cell cell28 = row.createCell(28);
						Cell cell29 = row.createCell(29);
						Cell cell30 = row.createCell(30);
						Cell cell31 = row.createCell(31);
						Cell cell32 = row.createCell(32);
						Cell cell33 = row.createCell(33);
						Cell cell34 = row.createCell(34);

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						cell7.setCellValue("");
						cell8.setCellValue("");
						cell9.setCellValue("");
						cell10.setCellValue("");
						cell11.setCellValue("");
						cell12.setCellValue("");
						cell13.setCellValue("");
						cell14.setCellValue("");
						cell15.setCellValue("");
						cell16.setCellValue("");
						cell17.setCellValue("");
						cell18.setCellValue("");
						cell19.setCellValue("");
						cell20.setCellValue("");
						cell21.setCellValue("");
						cell22.setCellValue("");
						cell23.setCellValue("");
						cell24.setCellValue("");
						cell25.setCellValue("");
						cell26.setCellValue("");
						cell27.setCellValue("");
						cell28.setCellValue("");
						cell29.setCellValue("");
						cell30.setCellValue("");
						cell31.setCellValue("");
						cell32.setCellValue("");
						cell33.setCellValue("");
						cell34.setCellValue("");
						if(null!=contentDetails.getWslId() && !"".equals(contentDetails.getWslId()))
						{
							cell0.setCellValue(contentDetails.getWslId());
						}
						if(null!=scheduleCode && !"".equals(scheduleCode))
						{
							cell1.setCellValue(scheduleCode);
						}
						if(null!=contentDetails.getMarket() && !"".equals(contentDetails.getMarket()))
						{
							cell2.setCellValue(contentDetails.getMarket());
						}
						if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
						{
							cell3.setCellValue(contentDetails.getLocale());
						}
						if(null!=contentDetails.getModelType() && !"".equals(contentDetails.getModelType()))
						{
							cell4.setCellValue(contentDetails.getModelType());
						}
						if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
						{
							cell5.setCellValue(contentDetails.getModel());
						}
						if(null!=contentDetails.getCarlineCode() && !"".equals(contentDetails.getCarlineCode()))
						{
							cell6.setCellValue(contentDetails.getCarlineCode());
						}
						if(null!=contentDetails.getModelFolderName() && !"".equals(contentDetails.getModelFolderName()))
						{
							cell7.setCellValue(contentDetails.getModelFolderName());
						}
						if(null!=contentDetails.getManualType() && !"".equals(contentDetails.getManualType()))
						{
							cell8.setCellValue(contentDetails.getManualType());
						}
						if(null!=contentDetails.getFaceLiftFolderName() && !"".equals(contentDetails.getFaceLiftFolderName()))
						{
							cell9.setCellValue(contentDetails.getFaceLiftFolderName());
						}
						if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
						{
							cell10.setCellValue(contentDetails.getMaterialName());
						}
						if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
						{
							cell11.setCellValue(contentDetails.getChannelName());
						}
						if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
						{
							cell12.setCellValue(contentDetails.getImDocumentId());
						}
						if(null!=contentDetails.getImVersion() && !"".equals(contentDetails.getImVersion()))
						{
							cell13.setCellValue(contentDetails.getImVersion());
						}
						if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
						{
							cell14.setCellValue(contentDetails.getFileAbsolutePath());
						}
						if(null!=contentDetails.getFileName() && !"".equals(contentDetails.getFileName()))
						{
							cell15.setCellValue(contentDetails.getFileName());
						}
						if(null!=contentDetails.getProcessingOperationStatus() && !"".equals(contentDetails.getProcessingOperationStatus()))
						{
							cell16.setCellValue(contentDetails.getProcessingOperationStatus());
							if(contentDetails.getProcessingOperationStatus().trim().toLowerCase().equals("success"))
							{
								// increment successCount
								successCount++;
							}
						}

						if(null!=contentDetails.getErrorComments() && !"".equals(contentDetails.getErrorComments()))
						{
							cell19.setCellValue(contentDetails.getErrorComments());
						}
						if(null!=contentDetails.getProcessingTime())
						{
							String time = sdf.format(contentDetails.getProcessingTime());
							cell20.setCellValue(time);
							time=null;
						}

						if(null!=contentDetails.getEsiCategoryMapped() && !"".equals(contentDetails.getEsiCategoryMapped()))
						{
							cell21.setCellValue(contentDetails.getEsiCategoryMapped());
						}
						if(null!=contentDetails.getMappedTaxonomy() && !"".equals(contentDetails.getMappedTaxonomy()))
						{
							cell22.setCellValue(contentDetails.getMappedTaxonomy());
						}

						if(null!=contentDetails.getEsiCategoryMappedLevel() && !"".equals(contentDetails.getEsiCategoryMappedLevel()))
						{
							cell23.setCellValue(contentDetails.getEsiCategoryMappedLevel());
						}

						if(null!=contentDetails.getEsiCategoryMappedRefKey() && !"".equals(contentDetails.getEsiCategoryMappedRefKey()))
						{
							cell24.setCellValue(contentDetails.getEsiCategoryMappedRefKey());
						}

						if(null!=contentDetails.getEsiCategoryMappedFileName() && !"".equals(contentDetails.getEsiCategoryMappedFileName()))
						{
							cell25.setCellValue(contentDetails.getEsiCategoryMappedFileName());
						}
						if(null!=contentDetails.getVinMapped() && !"".equals(contentDetails.getVinMapped()))
						{
							cell26.setCellValue(contentDetails.getVinMapped());
						}
						if(null!=contentDetails.getVinMappedFileName() && !"".equals(contentDetails.getVinMappedFileName()))
						{
							cell27.setCellValue(contentDetails.getVinMappedFileName());
						}
						if(null!=contentDetails.getDisplayOrderMapped() && !"".equals(contentDetails.getDisplayOrderMapped()))
						{
							cell28.setCellValue(contentDetails.getDisplayOrderMapped());
						}
						if(null!=contentDetails.getDisplayOrderMappedFileName() && !"".equals(contentDetails.getDisplayOrderMappedFileName()))
						{
							cell29.setCellValue(contentDetails.getDisplayOrderMappedFileName());
						}
						if(null!=contentDetails.getTitle() && !"".equals(contentDetails.getTitle()))
						{
							cell30.setCellValue(contentDetails.getTitle());
						}

						if(null!=contentDetails.getInnerLinkFound() && !"".equals(contentDetails.getInnerLinkFound()))
						{
							cell31.setCellValue(contentDetails.getInnerLinkFound());
						}
						if(null!=contentDetails.getAllInnerLinksMapped() && !"".equals(contentDetails.getAllInnerLinksMapped()))
						{
							cell32.setCellValue(contentDetails.getAllInnerLinksMapped());
						}
						if(null!=contentDetails.getInnerLinkMappingReason() && !"".equals(contentDetails.getInnerLinkMappingReason()))
						{
							cell33.setCellValue(contentDetails.getInnerLinkMappingReason());
						}

						if(mmeMarket==true)
						{
							if(null!=contentDetails.getEsiSteeringTypeInfo() && !"".equals(contentDetails.getEsiSteeringTypeInfo()))
							{
								cell34.setCellValue(contentDetails.getEsiSteeringTypeInfo());
							}
						}
						else
						{
							// MC MARKTE
							if(null!=contentDetails.getMetaTagDescription() && !"".equals(contentDetails.getMetaTagDescription()))
							{
								cell34.setCellValue(contentDetails.getMetaTagDescription());
							}
						}
						cell34 = null;
						cell33 = null;
						cell32 = null;
						cell31 = null;
						cell30 = null;
						cell29 = null;
						cell28 = null;
						cell27 = null;
						cell26 = null;
						cell25 = null;
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						cell7 = null;
						cell8 = null;
						cell9 = null;
						cell10 = null;
						cell11 = null;
						cell12 = null;
						cell13 = null;
						cell14 = null;
						cell15 = null;
						cell16 = null;
						cell17 = null;
						cell18 = null;
						cell19 = null;
						cell20 = null;
						cell21 = null;
						cell22 = null;
						cell23 = null;
						cell24 = null;
						row = null;
						contentDetails = null;
					}
				}

				transactionsReportsList = null;
				/*
				 * NOW ADD XCOPY PROCESSING LIST
				 */

				ArrayList<Map<Object, Object>> fileProcessingList = new ArrayList<Map<Object, Object>>();
				if(null!=xcopyFileProcessingList && xcopyFileProcessingList.size()>0)
				{
					fileProcessingList.addAll(xcopyFileProcessingList);
				}


				if(null!=fileProcessingList && fileProcessingList.size()>0)
				{
					// add fileProcessingList to totalCount
					totalCount= totalCount+fileProcessingList.size();

					for(int i=0;i<fileProcessingList.size();i++)
					{
						Map<Object, Object> dataMap = (HashMap<Object, Object>)fileProcessingList.get(i);
						ContentDetails contentDetails  = new ContentDetails();
						if(null!=dataMap.get("CONTENT_DETAILS"))
						{
							contentDetails = (ContentDetails)dataMap.get("CONTENT_DETAILS");
						}

						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						// START ADDING DATA
						// START ADDING DATA
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						Cell cell7 = row.createCell(7);
						Cell cell8 = row.createCell(8);
						Cell cell9 = row.createCell(9);
						Cell cell10 = row.createCell(10);
						Cell cell11 = row.createCell(11);
						Cell cell12 = row.createCell(12);
						Cell cell13 = row.createCell(13);
						Cell cell14 = row.createCell(14);
						Cell cell15 = row.createCell(15);
						Cell cell16 = row.createCell(16);
						Cell cell17 = row.createCell(17);
						Cell cell18 = row.createCell(18);
						Cell cell19 = row.createCell(19);
						Cell cell20 = row.createCell(20);

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						cell7.setCellValue("");
						cell8.setCellValue("");
						cell9.setCellValue("");
						cell10.setCellValue("");
						cell11.setCellValue("");
						cell12.setCellValue("");
						cell13.setCellValue("");
						cell14.setCellValue("");
						cell15.setCellValue("");
						cell16.setCellValue("");
						cell17.setCellValue("");
						cell18.setCellValue("");
						cell19.setCellValue("");
						cell20.setCellValue("");

						if(null!=contentDetails.getWslId() && !"".equals(contentDetails.getWslId()))
						{
							cell0.setCellValue(contentDetails.getWslId());
						}
						if(null!=scheduleCode && !"".equals(scheduleCode))
						{
							cell1.setCellValue(scheduleCode);
						}
						if(null!=contentDetails.getMarket() && !"".equals(contentDetails.getMarket()))
						{
							cell2.setCellValue(contentDetails.getMarket());
						}
						if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
						{
							cell3.setCellValue(contentDetails.getLocale());
						}
						if(null!=contentDetails.getModelType() && !"".equals(contentDetails.getModelType()))
						{
							cell4.setCellValue(contentDetails.getModelType());
						}
						if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
						{
							cell5.setCellValue(contentDetails.getModel());
						}
						if(null!=contentDetails.getCarlineCode() && !"".equals(contentDetails.getCarlineCode()))
						{
							cell6.setCellValue(contentDetails.getCarlineCode());
						}
						if(null!=contentDetails.getModelFolderName() && !"".equals(contentDetails.getModelFolderName()))
						{
							cell7.setCellValue(contentDetails.getModelFolderName());
						}
						if(null!=contentDetails.getManualType() && !"".equals(contentDetails.getManualType()))
						{
							cell8.setCellValue(contentDetails.getManualType());
						}
						if(null!=contentDetails.getFaceLiftFolderName() && !"".equals(contentDetails.getFaceLiftFolderName()))
						{
							cell9.setCellValue(contentDetails.getFaceLiftFolderName());
						}
						if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
						{
							cell10.setCellValue(contentDetails.getMaterialName());
						}
						if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
						{
							cell11.setCellValue(contentDetails.getChannelName());
						}
						if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
						{
							cell12.setCellValue(contentDetails.getImDocumentId());
						}
						if(null!=contentDetails.getImVersion() && !"".equals(contentDetails.getImVersion()))
						{
							cell13.setCellValue(contentDetails.getImVersion());
						}
						if(null!=dataMap.get("SOURCE_PATH") && !"".equals(dataMap.get("SOURCE_PATH")))
						{
							cell14.setCellValue(String.valueOf(dataMap.get("SOURCE_PATH")));
						}
						if(null!=dataMap.get("FILE_NAME") && !"".equals(dataMap.get("FILE_NAME")))
						{
							cell15.setCellValue(String.valueOf(dataMap.get("FILE_NAME")));
						}

						if(null!=dataMap.get("PROCESSING_STATUS") && !"".equals(dataMap.get("PROCESSING_STATUS")))
						{
							cell17.setCellValue(String.valueOf(dataMap.get("PROCESSING_STATUS")));
							if(String.valueOf(dataMap.get("PROCESSING_STATUS")).trim().toLowerCase().equals("success"))
							{
								// increment successCount
								successCount++;
							}
						}

						if(null!=dataMap.get("DESTINATION_PATH") && !"".equals(dataMap.get("DESTINATION_PATH")))
						{
							cell18.setCellValue(String.valueOf(dataMap.get("DESTINATION_PATH")));
						}

						if(null!=dataMap.get("ERROR_COMMENTS") && !"".equals(dataMap.get("ERROR_COMMENTS")))
						{
							cell19.setCellValue(String.valueOf(dataMap.get("ERROR_COMMENTS")));
						}

						if(null!=dataMap.get("DATE_TIME") && !"".equals(dataMap.get("DATE_TIME")))
						{
							cell20.setCellValue(String.valueOf(dataMap.get("DATE_TIME")));
						}

						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						cell7 = null;
						cell8 = null;
						cell9 = null;
						cell10 = null;
						cell11 = null;
						cell12 = null;
						cell13 = null;
						cell14 = null;
						cell15 = null;
						cell16 = null;
						cell17 = null;
						cell18 = null;
						cell19 = null;
						cell20 = null;
						row = null;
						contentDetails = null;
						dataMap = null;
					}
				}

				headerRow = null;
				wslIdCell = null;
				scheduleCodeCell = null;
				marketCell = null;
				localeCell=  null;
				modelCell = null;
				modelFolderCell = null;
				manualTypeCell = null;
				materialNameCell = null;
				channelCell = null;
				imDocumentIdCell = null;
				currentVersionCell = null;
				sourceFileLocCell = null;
				sourceFileNameCell= null;
				xcopyprocessingStatusCell = null;
				okAssetsDestinationCell = null;
				dateTimeCell = null;
				remarksCell = null;
				titleCell=  null;
				modelTypeCell=null;
				carlineCodeCell=null;
				faceLiftFolderNameCell=null;
				processingStatusCell=null;
				esiCategoryMappedCell=null;
				esiCategoryFileNameCell=null;
				esiCategoryMappedLevelCell=null;
				esiCategoryMappedRefKey=null;
				esiCategoryTaxonomyCell=null;
				vinMappedCell=null;
				vinMappedFileNameCell=null;
				dispOrdMappedCell=null;
				dispOrdMappedFileNameCell = null;

				/*
				 * Before Writing check for size if equals to or more than 10 MB
				 * then generate a file with a extension to it.
				 */

				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on PROCESSING REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				// set mySheet to null
				mySheet = null;
				// set myWorkBook to null
				myWorkBook = null;
				// set path to null
				path = null;
				// set myFile to null
				myFile = null;

				sdf = null;


				/*
				 * PROCEED FOR SAVING SUMMARY REPORT
				 */
				if(totalCount>0)
				{
					// calculate failure Count
					failureCount =totalCount - successCount;

					// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
					ReportsSummaryDetails rsd = new ReportsSummaryDetails();
					rsd.setSchduleCode(scheduleCode);
					rsd.setFailureCount(failureCount);
					rsd.setTotalCount(totalCount);
					rsd.setSuccessCount(successCount);
					// set report Name as Transaction Report
					rsd.setReportName(ReportsSummaryDetails.REPORT_TRANSACTION);
					// set status, if failure Count is more than 0 - then failure
					if(failureCount>0)
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
					}
					else
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
					}
					// UPDATE SUMMARY DETAILS
					try
					{
						ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printProcessingFilesReport()", e);
					}
					rsd = null;
				}
				totalCount = 0;
				successCount= 0;
				failureCount = 0;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printProcessingFilesReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printProcessingFilesReport :: Method Ends.");

	}
	@SuppressWarnings("resource")
	public void printInnerLinksReport(String scheduleCode, ArrayList<ContentDetails> innerLinksList) {
		if (logger.isInfoEnabled())
			logger.info("printInnerLinksReport :: Method Starts.");
		try {
			if (null != scheduleCode && !"".equals(scheduleCode) && null!=innerLinksList && innerLinksList.size()>0)
			{
				long totalCount=0;
				long successCount=0;
				long failureCount=0;
				/*
				 * write the string Builder Text to a separate Log file parallel
				 * to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = PathUtil.file(path);
				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
					// create new directory
					wslDirectory.mkdir();
				}
				wslDirectory = null;
				/*
				 * Now check, whether the schedule code directory exists or not
				 */
				path = path + "/" + scheduleCode;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists()
						|| !scheduleDirectory.isDirectory()) {
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * iterate Map and write the Excel File for each Index
				 */
				String fName = "/" + "INNER_LINKS_REPORT.xlsx";
				File myFile = PathUtil.file(path + fName);
				fName = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN
				// MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");

				/*
				 * Add Header Row WSL_ID SCHEDULE CODE CHANNEL DOCUMENT ID
				 * SOURCE FILE LOCATION FILE NAME VTOC FILE NAME
				 * 
				 * INNER LINK LOCATION INNER LINK DOC ID INNER LINK VTOC FILE
				 * NAME MAPPING STATUS
				 */
				Row headerRow = mySheet.createRow(0);

				Cell wslIdCell = headerRow.createCell(0);
				wslIdCell.setCellValue("WSL ID");
				Cell scheduleCodeCell = headerRow.createCell(1);
				scheduleCodeCell.setCellValue("SCHEDULE ID");
				Cell channelCell = headerRow.createCell(2);
				channelCell.setCellValue("CHANNEL");
				Cell imDocumentIdCell = headerRow.createCell(3);
				imDocumentIdCell.setCellValue("DOCUMENT ID");
				Cell sourceFileLocCell = headerRow.createCell(4);
				sourceFileLocCell.setCellValue("SOURCE FILE LOCATION");
				Cell sourceFileNameCell = headerRow.createCell(5);
				sourceFileNameCell.setCellValue("SOURCE FILE NAME");
				Cell innerLinkSourcePathCell = headerRow.createCell(6);
				innerLinkSourcePathCell.setCellValue("INNER LINK SOURCE PATH");
				Cell innerLinkDocumentIdCell = headerRow.createCell(7);
				innerLinkDocumentIdCell.setCellValue("INNER LINK DOC ID");
				Cell innerLinkMappingStatusCell = headerRow.createCell(8);
				innerLinkMappingStatusCell.setCellValue("MAPPING STATUS");

				int rowCount = 0;
				// ADD INFO
				ArrayList<ContentDetails> transactionsReportsList = new ArrayList<ContentDetails>();
				if (null != innerLinksList 	&& innerLinksList.size() > 0) 
				{
					transactionsReportsList.addAll(innerLinksList);
				}

				if (null != transactionsReportsList && transactionsReportsList.size() > 0) 
				{
					for (int i = 0; i < transactionsReportsList.size(); i++) {
						ContentDetails contentDetails = (ContentDetails) transactionsReportsList.get(i);

						if (null != contentDetails.getInnerLinksList() 	&& contentDetails.getInnerLinksList().size() > 0) 
						{
							// set totalCount
							totalCount= totalCount+ contentDetails.getInnerLinksList().size();
							for (int j = 0; j < contentDetails.getInnerLinksList().size(); j++) 
							{
								LinkDetails linkDetails = (LinkDetails) contentDetails.getInnerLinksList().get(j);
								// increment rowCount by 1
								rowCount++;
								// Create a new Row
								Row row = mySheet.createRow(rowCount);

								// START ADDING DATA
								Cell cell0 = row.createCell(0);
								Cell cell1 = row.createCell(1);
								Cell cell2 = row.createCell(2);
								Cell cell3 = row.createCell(3);
								Cell cell4 = row.createCell(4);
								Cell cell5 = row.createCell(5);
								Cell cell6 = row.createCell(6);
								Cell cell7 = row.createCell(7);
								Cell cell8 = row.createCell(8);

								cell0.setCellValue("");
								cell1.setCellValue("");
								cell2.setCellValue("");
								cell3.setCellValue("");
								cell4.setCellValue("");
								cell5.setCellValue("");
								cell6.setCellValue("");
								cell7.setCellValue("");
								cell8.setCellValue("");

								if (null != contentDetails.getWslId() && !"".equals(contentDetails.getWslId())) 
								{
									cell0.setCellValue(contentDetails.getWslId());
								}
								if (null != scheduleCode && !"".equals(scheduleCode)) 
								{
									cell1.setCellValue(scheduleCode);
								}
								if (null != contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName())) 
								{
									cell2.setCellValue(contentDetails.getChannelName());
								}
								if (null != contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId())) 
								{
									cell3.setCellValue(contentDetails.getImDocumentId());
								}
								if (null != contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath())) 
								{
									cell4.setCellValue(contentDetails.getFileAbsolutePath());
								}
								if (null != contentDetails.getFileName() && !"".equals(contentDetails.getFileName())) 
								{
									cell5.setCellValue(contentDetails.getFileName());
								}

								if (null != linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath())) 
								{
									cell6.setCellValue(linkDetails.getInnerLinkPath());
								}
								if (null != linkDetails.getInnnerLinkDocumentId() && !"".equals(linkDetails.getInnnerLinkDocumentId())) 
								{
									cell7.setCellValue(linkDetails.getInnnerLinkDocumentId());
								}
								if (null != linkDetails.getMapStatus() 	&& !"".equals(linkDetails.getMapStatus())) 
								{
									cell8.setCellValue(linkDetails.getMapStatus());
									if(linkDetails.getMapStatus().trim().toLowerCase().equals("y"))
									{
										// increment successCount
										successCount++;
									}
								}

								cell0 = null;
								cell1 = null;
								cell2 = null;
								cell3 = null;
								cell4 = null;
								cell5 = null;
								cell6 = null;
								cell7 = null;
								cell8 = null;
								row = null;
								linkDetails = null;
							}
						}
						contentDetails = null;
					}
				}

				transactionsReportsList = null;

				headerRow = null;
				wslIdCell = null;
				scheduleCodeCell = null;
				channelCell = null;
				imDocumentIdCell = null;
				sourceFileLocCell = null;
				sourceFileNameCell = null;
				innerLinkDocumentIdCell = null;
				innerLinkMappingStatusCell = null;
				innerLinkSourcePathCell = null;

				/*
				 * Before Writing check for size if equals to or more than 10 MB
				 * then generate a file with a extension to it.
				 */

				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on INNER LINKS REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				// set mySheet to null
				mySheet = null;
				// set myWorkBook to null
				myWorkBook = null;
				// set path to null
				path = null;
				// set myFile to null
				myFile = null;

				/*
				 * PROCEED FOR SAVING SUMMARY REPORT
				 */
				if(totalCount>0)
				{
					// calculate failure Count
					failureCount =totalCount - successCount;

					// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
					ReportsSummaryDetails rsd = new ReportsSummaryDetails();
					rsd.setSchduleCode(scheduleCode);
					rsd.setFailureCount(failureCount);
					rsd.setTotalCount(totalCount);
					rsd.setSuccessCount(successCount);
					// set report Name as Transaction Report
					rsd.setReportName(ReportsSummaryDetails.REPORT_INNERLINKS);
					// set status, if failure Count is more than 0 - then failure
					if(failureCount>0)
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
					}
					else
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
					}
					// UPDATE SUMMARY DETAILS
					try
					{
						ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printInnerLinksReport()", e);
					}
					rsd = null;
				}
				totalCount = 0;
				successCount= 0;
				failureCount = 0;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(),"printInnerLinksReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printInnerLinksReport :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printOtherInnerLinksReport(String scheduleCode, ArrayList<ContentDetails> innerLinksList) {
		if (logger.isInfoEnabled())
			logger.info("printOtherInnerLinksReport :: Method Starts.");
		try {
			if (null != scheduleCode && !"".equals(scheduleCode) && null!=innerLinksList && innerLinksList.size()>0)
			{
				long totalCount=0;
				long successCount=0;
				long failureCount=0;
				/*
				 * write the string Builder Text to a separate Log file parallel
				 * to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = PathUtil.file(path);
				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
					// create new directory
					wslDirectory.mkdir();
				}
				wslDirectory = null;
				/*
				 * Now check, whether the schedule code directory exists or not
				 */
				path = path + "/" + scheduleCode;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists()
						|| !scheduleDirectory.isDirectory()) {
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * iterate Map and write the Excel File for each Index
				 */
				String fName = "/" + "OTHER_MANUAL_LINKS_REPORT.xlsx";
				File myFile = PathUtil.file(path + fName);
				fName = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN
				// MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");

				/*
				 * Add Header Row WSL_ID SCHEDULE CODE CHANNEL DOCUMENT ID
				 * SOURCE FILE LOCATION FILE NAME VTOC FILE NAME
				 * 
				 * INNER LINK LOCATION INNER LINK DOC ID INNER LINK VTOC FILE
				 * NAME MAPPING STATUS
				 */
				Row headerRow = mySheet.createRow(0);

				Cell wslIdCell = headerRow.createCell(0);
				wslIdCell.setCellValue("WSL ID");
				Cell scheduleCodeCell = headerRow.createCell(1);
				scheduleCodeCell.setCellValue("SCHEDULE ID");
				Cell channelCell = headerRow.createCell(2);
				channelCell.setCellValue("CHANNEL");
				Cell imDocumentIdCell = headerRow.createCell(3);
				imDocumentIdCell.setCellValue("DOCUMENT ID");
				Cell sourceFileLocCell = headerRow.createCell(4);
				sourceFileLocCell.setCellValue("SOURCE FILE LOCATION");
				Cell sourceFileNameCell = headerRow.createCell(5);
				sourceFileNameCell.setCellValue("SOURCE FILE NAME");
				Cell innerLinkSourcePathCell = headerRow.createCell(6);
				innerLinkSourcePathCell.setCellValue("INNER LINK SOURCE PATH");
				// the link as sent to Kapture (OtherManualLinkRules)
				headerRow.createCell(7).setCellValue("KAPTURE LINK");

				int rowCount = 0;
				// ADD INFO
				ArrayList<ContentDetails> transactionsReportsList = new ArrayList<ContentDetails>();
				if (null != innerLinksList 	&& innerLinksList.size() > 0) 
				{
					transactionsReportsList.addAll(innerLinksList);
				}

				if (null != transactionsReportsList && transactionsReportsList.size() > 0) 
				{
					for (int i = 0; i < transactionsReportsList.size(); i++) {
						ContentDetails contentDetails = (ContentDetails) transactionsReportsList.get(i);

						if (null != contentDetails.getAdditionalInnerLinks() 	&& contentDetails.getAdditionalInnerLinks().size() > 0) 
						{
							// set totalCount
							totalCount= totalCount+ contentDetails.getAdditionalInnerLinks().size();
							for (int j = 0; j < contentDetails.getAdditionalInnerLinks().size(); j++) 
							{
								LinkDetails linkDetails = (LinkDetails) contentDetails.getAdditionalInnerLinks().get(j);
								// increment rowCount by 1
								rowCount++;
								// Create a new Row
								Row row = mySheet.createRow(rowCount);

								// START ADDING DATA
								Cell cell0 = row.createCell(0);
								Cell cell1 = row.createCell(1);
								Cell cell2 = row.createCell(2);
								Cell cell3 = row.createCell(3);
								Cell cell4 = row.createCell(4);
								Cell cell5 = row.createCell(5);
								Cell cell6 = row.createCell(6);

								cell0.setCellValue("");
								cell1.setCellValue("");
								cell2.setCellValue("");
								cell3.setCellValue("");
								cell4.setCellValue("");
								cell5.setCellValue("");
								cell6.setCellValue("");

								if (null != contentDetails.getWslId() && !"".equals(contentDetails.getWslId())) 
								{
									cell0.setCellValue(contentDetails.getWslId());
								}
								if (null != scheduleCode && !"".equals(scheduleCode)) 
								{
									cell1.setCellValue(scheduleCode);
								}
								if (null != contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName())) 
								{
									cell2.setCellValue(contentDetails.getChannelName());
								}
								if (null != contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId())) 
								{
									cell3.setCellValue(contentDetails.getImDocumentId());
								}
								if (null != contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath())) 
								{
									cell4.setCellValue(contentDetails.getFileAbsolutePath());
								}
								if (null != contentDetails.getFileName() && !"".equals(contentDetails.getFileName())) 
								{
									cell5.setCellValue(contentDetails.getFileName());
								}

								if (null != linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath())) 
								{
									cell6.setCellValue(linkDetails.getInnerLinkPath());
								}
								row.createCell(7).setCellValue(null == linkDetails.getKaptureLinkPath() ? "" : linkDetails.getKaptureLinkPath());

								cell0 = null;
								cell1 = null;
								cell2 = null;
								cell3 = null;
								cell4 = null;
								cell5 = null;
								cell6 = null;
								row = null;
								linkDetails = null;
							}
						}
						contentDetails = null;
					}
				}

				transactionsReportsList = null;

				headerRow = null;
				wslIdCell = null;
				scheduleCodeCell = null;
				channelCell = null;
				imDocumentIdCell = null;
				sourceFileLocCell = null;
				sourceFileNameCell = null;
				innerLinkSourcePathCell = null;

				/*
				 * Before Writing check for size if equals to or more than 10 MB
				 * then generate a file with a extension to it.
				 */

				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on OTHER MANUAL LINKS REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				// set mySheet to null
				mySheet = null;
				// set myWorkBook to null
				myWorkBook = null;
				// set path to null
				path = null;
				// set myFile to null
				myFile = null;

				/*
				 * PROCEED FOR SAVING SUMMARY REPORT
				 */
				if(totalCount>0)
				{
					// SET failure Count
					failureCount = 0;
					// set successCount = total Count
					successCount = totalCount;

					// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
					ReportsSummaryDetails rsd = new ReportsSummaryDetails();
					rsd.setSchduleCode(scheduleCode);
					rsd.setFailureCount(failureCount);
					rsd.setTotalCount(totalCount);
					rsd.setSuccessCount(successCount);
					// set report Name as Transaction Report
					rsd.setReportName(ReportsSummaryDetails.REPORT_OTHER_MANUALLINKS);
					// set status, if failure Count is more than 0 - then failure
					if(failureCount>0)
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
					}
					else
					{
						rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
					}
					// UPDATE SUMMARY DETAILS
					try
					{
						ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printOtherInnerLinksReport()", e);
					}
					rsd = null;
				}
				totalCount = 0;
				successCount= 0;
				failureCount = 0;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(),"printOtherInnerLinksReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printOtherInnerLinksReport :: Method Ends.");
	}

	
	@SuppressWarnings("resource")
	public void printDisplayOrderDetails(String scheduleCode , ArrayList<DisplayOrderDetails> displayOrderList) {
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderDetails :: Method Starts.");
		try {
			if (null != displayOrderList && displayOrderList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long successCount=0;
					long failureCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					// set totalCount
					totalCount=  displayOrderList.size();

					ArrayList<List<DisplayOrderDetails>> partitions = new ArrayList<List<DisplayOrderDetails>>();
					for (int i=0; i<displayOrderList.size(); i += partitionSize) {
						partitions.add(displayOrderList.subList(i, Math.min(i + partitionSize, displayOrderList.size())));
					}
					
					if(null!=partitions && partitions.size()>0)
					{
						int listCount=0;
						for(List<DisplayOrderDetails> subsetList : partitions)
						{
							if(null!=subsetList && subsetList.size()>0)
							{
								/*
								 * iterate Map and write the Excel File for each Index
								 */
								String fName = "/";
								if(listCount==0)
								{
									fName+="DISPLAY_ORDER.xlsx";
								}
								else
								{
									fName+="DISPLAY_ORDER_"+listCount+".xlsx";
								}
								// increment listCount
								listCount++;
								File myFile = PathUtil.file(path + fName);
								fName = null;
								// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
								SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

								// Create a new sheet
								Sheet mySheet = myWorkBook.createSheet("Details");
								/*
								 * Add Header Row
								 */
								Row headerRow = mySheet.createRow(0);
								Cell sourceFileNameCell = headerRow.createCell(0);
								sourceFileNameCell.setCellValue("SOURCE FILE");

								Cell documentIdCell = headerRow.createCell(1);
								documentIdCell.setCellValue("DOCUMENT ID");

								Cell dispOrdFileNameCell = headerRow.createCell(2);
								dispOrdFileNameCell.setCellValue("DISP ORD FILE NAME");

								Cell lineTypeCell = headerRow.createCell(3);
								lineTypeCell.setCellValue("LINE TYPE");

								Cell catTypeCell = headerRow.createCell(4);
								catTypeCell.setCellValue("STATUS");

								Cell dispOrdNameLevel1 = headerRow.createCell(5);
								dispOrdNameLevel1.setCellValue("DISP ORD LEVEL 1 NAME");

								Cell dispOrdNameLevel2 = headerRow.createCell(6);
								dispOrdNameLevel2.setCellValue("DISP ORD LEVEL 2 NAME");

								Cell dispOrdNameLevel3 = headerRow.createCell(7);
								dispOrdNameLevel3.setCellValue("DISP ORD LEVEL 3 NAME");

								Cell dispOrdNameLevel4 = headerRow.createCell(8);
								dispOrdNameLevel4.setCellValue("DISP ORD LEVEL 4 NAME");

								Cell dispOrdNameLevel5 = headerRow.createCell(9);
								dispOrdNameLevel5.setCellValue("DISP ORD LEVEL 5 NAME");

								Cell dispOrdNameLevel6 = headerRow.createCell(10);
								dispOrdNameLevel6.setCellValue("DISP ORD LEVEL 6 NAME");

								Cell seqNoCell = headerRow.createCell(11);
								seqNoCell.setCellValue("SEQUENCE NO");

								Cell manualTypeCell = headerRow.createCell(12);
								manualTypeCell.setCellValue("MANUAL TYPE");

								Cell titleCell = headerRow.createCell(13);
								titleCell.setCellValue("TITLE");

								Cell engineTypeCell = headerRow.createCell(14);
								engineTypeCell.setCellValue("ENGINE TYPE");

								Cell missionTypeCell = headerRow.createCell(15);
								missionTypeCell.setCellValue("MISSION TYPE");

								Cell axleTypeCell = headerRow.createCell(16);
								axleTypeCell.setCellValue("DRIVE AXLE TYPE");

								Cell bodyTypeCell = headerRow.createCell(17);
								bodyTypeCell.setCellValue("BODY TYPE");

								Cell errorCodeCell = headerRow.createCell(18);
								errorCodeCell.setCellValue("ERROR CODE");

								Cell errorMessageCell = headerRow.createCell(19);
								errorMessageCell.setCellValue("ERROR MESSAGE");
								
								int rowCount = 0;
								for (int a = 0; a < subsetList.size(); a++) {

									DisplayOrderDetails details = (DisplayOrderDetails) subsetList.get(a);

									// increment rowCount by 1
									rowCount++;
									// Create a new Row
									Row row = mySheet.createRow(rowCount);
									Cell cell0 = row.createCell(0);
									Cell cell1 = row.createCell(1);
									Cell cell2 = row.createCell(2);
									Cell cell3 = row.createCell(3);
									Cell cell4 = row.createCell(4);
									Cell cell6 = row.createCell(5);
									Cell cell8 = row.createCell(6);
									Cell cell10 = row.createCell(7);
									Cell cell12 = row.createCell(8);
									Cell cell14 = row.createCell(9);
									Cell cell16 = row.createCell(10);
									Cell cell17 = row.createCell(11);
									Cell cell18 = row.createCell(12);
									Cell cell19 = row.createCell(13);
									Cell cell20 = row.createCell(14);
									Cell cell21 = row.createCell(15);
									Cell cell22 = row.createCell(16);
									Cell cell23 = row.createCell(17);
									Cell cell24 = row.createCell(18);
									Cell cell25 = row.createCell(19);

									cell0.setCellValue("");
									cell1.setCellValue("");
									cell2.setCellValue("");
									cell3.setCellValue("");
									cell4.setCellValue("");
									cell6.setCellValue("");
									cell8.setCellValue("");
									cell10.setCellValue("");
									cell12.setCellValue("");
									cell14.setCellValue("");
									cell16.setCellValue("");
									cell17.setCellValue("");
									cell18.setCellValue("");
									cell19.setCellValue("");
									cell20.setCellValue("");
									cell21.setCellValue("");
									cell22.setCellValue("");
									cell23.setCellValue("");
									cell24.setCellValue("");
									cell25.setCellValue("");

									if (null != details.getFilePath() && !"".equals(details.getFilePath())) 
									{
										cell0.setCellValue(details.getFilePath());
									}

									if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
									{
										cell1.setCellValue(details.getDocumentId());
									}

									if(null!=details.getDisplayOrderSourceFileName() && !"".equals(details.getDisplayOrderSourceFileName()))
									{
										cell2.setCellValue(details.getDisplayOrderSourceFileName());
									}

									if(null!=details.getLineType() && !"".equals(details.getLineType()))
									{
										cell3.setCellValue(details.getLineType());
									}

									if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
									{
										cell4.setCellValue(details.getProcessingStatus());
										if(details.getProcessingStatus().trim().toLowerCase().equals("success"))
										{
											// increment successCount
											successCount++;
										}
									}

									if(null!=details.getDisplayOrderLevel1Name() && !"".equals(details.getDisplayOrderLevel1Name()))
									{
										cell6.setCellValue(details.getDisplayOrderLevel1Name());
									}

									if(null!=details.getDisplayOrderLevel2Name() && !"".equals(details.getDisplayOrderLevel2Name()))
									{
										cell8.setCellValue(details.getDisplayOrderLevel2Name());
									}

									if(null!=details.getDisplayOrderLevel3Name() && !"".equals(details.getDisplayOrderLevel3Name()))
									{
										cell10.setCellValue(details.getDisplayOrderLevel3Name());
									}

									if(null!=details.getDisplayOrderLevel4Name() && !"".equals(details.getDisplayOrderLevel4Name()))
									{
										cell12.setCellValue(details.getDisplayOrderLevel4Name());
									}

									if(null!=details.getDisplayOrderLevel5Name() && !"".equals(details.getDisplayOrderLevel5Name()))
									{
										cell14.setCellValue(details.getDisplayOrderLevel5Name());
									}

									if(null!=details.getDisplayOrderLevel6Name() && !"".equals(details.getDisplayOrderLevel6Name()))
									{
										cell16.setCellValue(details.getDisplayOrderLevel6Name());
									}

									if(null!=details.getSequenceNo() && !"".equals(details.getSequenceNo()))
									{
										cell17.setCellValue(details.getSequenceNo());
									}

									if(null!=details.getManualTypeRefKey() && !"".equals(details.getManualTypeRefKey()))
									{
										cell18.setCellValue(details.getManualTypeRefKey());
									}

									if(null!=details.getTitle() && !"".equals(details.getTitle()))
									{
										cell19.setCellValue(details.getTitle());
									}

									if(null!=details.getEngineType() && !"".equals(details.getEngineType()))
									{
										cell20.setCellValue(details.getEngineType());
									}

									if(null!=details.getMissionType() && !"".equals(details.getMissionType()))
									{
										cell21.setCellValue(details.getMissionType());
									}

									if(null!=details.getDriveAxleType() && !"".equals(details.getDriveAxleType()))
									{
										cell22.setCellValue(details.getDriveAxleType());
									}

									if(null!=details.getBodyType() && !"".equals(details.getBodyType()))
									{
										cell23.setCellValue(details.getBodyType());
									}

									if(null!=details.getErrorCode() && !"".equals(details.getErrorCode()))
									{
										cell24.setCellValue(details.getErrorCode());
									}

									if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
									{
										cell25.setCellValue(details.getErrorMessage());
									}

									cell25=null;
									cell24 =null;
									cell23=null;
									cell22=null;
									cell21=null;
									cell20=null;
									cell19=null;
									cell18=null;
									cell17=null;
									cell16=null;
									cell14=null;
									cell12=null;
									cell10=null;
									cell8=null;
									cell6 = null;
									cell4 = null;
									cell3 = null;
									cell2 = null;
									cell1 = null;
									cell0 = null;
									row = null;
									details=  null;
								}

								headerRow = null;
								sourceFileNameCell = null;
								documentIdCell = null;
								lineTypeCell= null;
								dispOrdFileNameCell = null;
								catTypeCell = null;
								dispOrdNameLevel1 =null;
								dispOrdNameLevel2 =null;
								dispOrdNameLevel3 =null;
								dispOrdNameLevel4 =null;
								dispOrdNameLevel5 =null;
								dispOrdNameLevel6 =null;
								seqNoCell=  null;
								titleCell= null;
								engineTypeCell= null;
								missionTypeCell= null;
								axleTypeCell= null;
								bodyTypeCell= null;
								errorCodeCell= null;
								errorMessageCell = null;
								manualTypeCell= null;

								FileOutputStream os = PathUtil.fileOutputStream(myFile);
								myWorkBook.write(os);
								logger.info("Writing on DISPLAY ORDER DETAILS REPORT XLSX file Finished ...");
								os.flush();
								os.close();

								// set mySheet to null
								mySheet = null;
								// set myWorkBook to null
								myWorkBook = null;
								// set myFile to null
								myFile = null;
							}
							subsetList = null;
						}
					}
					partitions = null;
					// set path to null
					path = null;
					
					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - successCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(successCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_DISPLAYORDER);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printDisplayOrderDetails()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					successCount= 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printDisplayOrderDetails()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderDetails :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printCDDataDetails(String scheduleCode , ArrayList<CDProcessingDetails> cdProcessingList) {
		if (logger.isInfoEnabled())
			logger.info("printCDDataDetails :: Method Starts.");
		try {
			if (null != cdProcessingList && cdProcessingList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long successCount=0;
					long failureCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					// set totalCount
					totalCount=  cdProcessingList.size();

					ArrayList<List<CDProcessingDetails>> partitions = new ArrayList<List<CDProcessingDetails>>();
					for (int i=0; i<cdProcessingList.size(); i += partitionSize) {
						partitions.add(cdProcessingList.subList(i, Math.min(i + partitionSize, cdProcessingList.size())));
					}
					
					if(null!=partitions && partitions.size()>0)
					{
						int listCount=0;
						for(List<CDProcessingDetails> subsetList: partitions)
						{
							if(null!=subsetList && subsetList.size()>0)
							{
								/*
								 * iterate Map and write the Excel File for each Index
								 */
								String fName = "/";
								if(listCount==0)
								{
									fName+="CD_PROCESSING_DATA.xlsx";
								}
								else
								{
									fName+="CD_PROCESSING_DATA_"+listCount+".xlsx";
								}
								// increment listCount
								listCount++;
								File myFile = PathUtil.file(path + fName);
								fName = null;
								// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
								SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

								// Create a new sheet
								Sheet mySheet = myWorkBook.createSheet("Details");
								/*
								 * Add Header Row
								 */
								Row headerRow = mySheet.createRow(0);
								Cell sourceFileNameCell = headerRow.createCell(0);
								sourceFileNameCell.setCellValue("SOURCE FILE");

								Cell documentIdCell = headerRow.createCell(1);
								documentIdCell.setCellValue("DOCUMENT ID");

								Cell dispOrdFileNameCell = headerRow.createCell(2);
								dispOrdFileNameCell.setCellValue("CD FILE NAME");

								Cell lineTypeCell = headerRow.createCell(3);
								lineTypeCell.setCellValue("LINE TYPE");

								Cell catTypeCell = headerRow.createCell(4);
								catTypeCell.setCellValue("STATUS");

								Cell dispOrdNameLevel1 = headerRow.createCell(5);
								dispOrdNameLevel1.setCellValue("DISP ORD LEVEL 1 NAME");

								Cell dispOrdNameLevel2 = headerRow.createCell(6);
								dispOrdNameLevel2.setCellValue("DISP ORD LEVEL 2 NAME");

								Cell dispOrdNameLevel3 = headerRow.createCell(7);
								dispOrdNameLevel3.setCellValue("DISP ORD LEVEL 3 NAME");

								Cell dispOrdNameLevel4 = headerRow.createCell(8);
								dispOrdNameLevel4.setCellValue("DISP ORD LEVEL 4 NAME");

								Cell dispOrdNameLevel5 = headerRow.createCell(9);
								dispOrdNameLevel5.setCellValue("DISP ORD LEVEL 5 NAME");

								Cell dispOrdNameLevel6 = headerRow.createCell(10);
								dispOrdNameLevel6.setCellValue("DISP ORD LEVEL 6 NAME");

								Cell seqNoCell = headerRow.createCell(11);
								seqNoCell.setCellValue("SEQUENCE NO");

								Cell manualTypeCell = headerRow.createCell(12);
								manualTypeCell.setCellValue("MANUAL TYPE");

								Cell titleCell = headerRow.createCell(13);
								titleCell.setCellValue("TITLE");

								Cell engineTypeCell = headerRow.createCell(14);
								engineTypeCell.setCellValue("ENGINE TYPE");

								Cell missionTypeCell = headerRow.createCell(15);
								missionTypeCell.setCellValue("MISSION TYPE");

								Cell axleTypeCell = headerRow.createCell(16);
								axleTypeCell.setCellValue("DRIVE AXLE TYPE");

								Cell bodyTypeCell = headerRow.createCell(17);
								bodyTypeCell.setCellValue("BODY TYPE");

								Cell errorCodeCell = headerRow.createCell(18);
								errorCodeCell.setCellValue("ERROR CODE");

								Cell errorMessageCell = headerRow.createCell(19);
								errorMessageCell.setCellValue("ERROR MESSAGE");
								
								int rowCount = 0;
								for (int a = 0; a < subsetList.size(); a++) {

									CDProcessingDetails details = (CDProcessingDetails) subsetList.get(a);

									// increment rowCount by 1
									rowCount++;
									// Create a new Row
									Row row = mySheet.createRow(rowCount);
									Cell cell0 = row.createCell(0);
									Cell cell1 = row.createCell(1);
									Cell cell2 = row.createCell(2);
									Cell cell3 = row.createCell(3);
									Cell cell4 = row.createCell(4);
									Cell cell6 = row.createCell(5);
									Cell cell8 = row.createCell(6);
									Cell cell10 = row.createCell(7);
									Cell cell12 = row.createCell(8);
									Cell cell14 = row.createCell(9);
									Cell cell16 = row.createCell(10);
									Cell cell17 = row.createCell(11);
									Cell cell18 = row.createCell(12);
									Cell cell19 = row.createCell(13);
									Cell cell20 = row.createCell(14);
									Cell cell21 = row.createCell(15);
									Cell cell22 = row.createCell(16);
									Cell cell23 = row.createCell(17);
									Cell cell24 = row.createCell(18);
									Cell cell25 = row.createCell(19);

									cell0.setCellValue("");
									cell1.setCellValue("");
									cell2.setCellValue("");
									cell3.setCellValue("");
									cell4.setCellValue("");
									cell6.setCellValue("");
									cell8.setCellValue("");
									cell10.setCellValue("");
									cell12.setCellValue("");
									cell14.setCellValue("");
									cell16.setCellValue("");
									cell17.setCellValue("");
									cell18.setCellValue("");
									cell19.setCellValue("");
									cell20.setCellValue("");
									cell21.setCellValue("");
									cell22.setCellValue("");
									cell23.setCellValue("");
									cell24.setCellValue("");
									cell25.setCellValue("");

									if (null != details.getFilePath() && !"".equals(details.getFilePath())) 
									{
										cell0.setCellValue(details.getFilePath());
									}

									if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
									{
										cell1.setCellValue(details.getDocumentId());
									}

									if(null!=details.getCdProcessingSourceFileName() && !"".equals(details.getCdProcessingSourceFileName()))
									{
										cell2.setCellValue(details.getCdProcessingSourceFileName());
									}

									if(null!=details.getLineType() && !"".equals(details.getLineType()))
									{
										cell3.setCellValue(details.getLineType());
									}

									if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
									{
										cell4.setCellValue(details.getProcessingStatus());
										if(details.getProcessingStatus().trim().toLowerCase().equals("success"))
										{
											// increment successCount
											successCount++;
										}
									}

									if(null!=details.getDisplayOrderLevel1Name() && !"".equals(details.getDisplayOrderLevel1Name()))
									{
										cell6.setCellValue(details.getDisplayOrderLevel1Name());
									}

									if(null!=details.getDisplayOrderLevel2Name() && !"".equals(details.getDisplayOrderLevel2Name()))
									{
										cell8.setCellValue(details.getDisplayOrderLevel2Name());
									}

									if(null!=details.getDisplayOrderLevel3Name() && !"".equals(details.getDisplayOrderLevel3Name()))
									{
										cell10.setCellValue(details.getDisplayOrderLevel3Name());
									}

									if(null!=details.getDisplayOrderLevel4Name() && !"".equals(details.getDisplayOrderLevel4Name()))
									{
										cell12.setCellValue(details.getDisplayOrderLevel4Name());
									}

									if(null!=details.getDisplayOrderLevel5Name() && !"".equals(details.getDisplayOrderLevel5Name()))
									{
										cell14.setCellValue(details.getDisplayOrderLevel5Name());
									}

									if(null!=details.getDisplayOrderLevel6Name() && !"".equals(details.getDisplayOrderLevel6Name()))
									{
										cell16.setCellValue(details.getDisplayOrderLevel6Name());
									}

									if(null!=details.getSequenceNo() && !"".equals(details.getSequenceNo()))
									{
										cell17.setCellValue(details.getSequenceNo());
									}

									if(null!=details.getManualTypeRefKey() && !"".equals(details.getManualTypeRefKey()))
									{
										cell18.setCellValue(details.getManualTypeRefKey());
									}

									if(null!=details.getTitle() && !"".equals(details.getTitle()))
									{
										cell19.setCellValue(details.getTitle());
									}

									if(null!=details.getEngineType() && !"".equals(details.getEngineType()))
									{
										cell20.setCellValue(details.getEngineType());
									}

									if(null!=details.getMissionType() && !"".equals(details.getMissionType()))
									{
										cell21.setCellValue(details.getMissionType());
									}

									if(null!=details.getDriveAxleType() && !"".equals(details.getDriveAxleType()))
									{
										cell22.setCellValue(details.getDriveAxleType());
									}

									if(null!=details.getBodyType() && !"".equals(details.getBodyType()))
									{
										cell23.setCellValue(details.getBodyType());
									}

									if(null!=details.getErrorCode() && !"".equals(details.getErrorCode()))
									{
										cell24.setCellValue(details.getErrorCode());
									}

									if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
									{
										cell25.setCellValue(details.getErrorMessage());
									}

									cell25=null;
									cell24 =null;
									cell23=null;
									cell22=null;
									cell21=null;
									cell20=null;
									cell19=null;
									cell18=null;
									cell17=null;
									cell16=null;
									cell14=null;
									cell12=null;
									cell10=null;
									cell8=null;
									cell6 = null;
									cell4 = null;
									cell3 = null;
									cell2 = null;
									cell1 = null;
									cell0 = null;
									row = null;
									details=  null;
								}
								
								headerRow = null;
								sourceFileNameCell = null;
								documentIdCell = null;
								lineTypeCell= null;
								dispOrdFileNameCell = null;
								catTypeCell = null;
								dispOrdNameLevel1 =null;
								dispOrdNameLevel2 =null;
								dispOrdNameLevel3 =null;
								dispOrdNameLevel4 =null;
								dispOrdNameLevel5 =null;
								dispOrdNameLevel6 =null;
								seqNoCell=  null;
								titleCell= null;
								engineTypeCell= null;
								missionTypeCell= null;
								axleTypeCell= null;
								bodyTypeCell= null;
								errorCodeCell= null;
								errorMessageCell = null;
								manualTypeCell= null;

								FileOutputStream os = PathUtil.fileOutputStream(myFile);
								myWorkBook.write(os);
								logger.info("Writing on CD PROCESSING DATA DETAILS REPORT XLSX file Finished ...");
								os.flush();
								os.close();

								// set mySheet to null
								mySheet = null;
								// set myWorkBook to null
								myWorkBook = null;
								// set myFile to null
								myFile = null;
							}
							subsetList = null;
						}
					}
					partitions = null;
					// set path to null
					path = null;
					

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - successCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(successCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_CD_PROCESSING);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printCDDataDetails()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					successCount= 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printDisplayOrderDetails()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderDetails :: Method Ends.");
	}


	@SuppressWarnings("resource")
	public void printViewContentDetailsReport(String scheduleCode , ArrayList<MCViewContentDetails> viewContentList) {
		if (logger.isInfoEnabled())
			logger.info("printViewContentDetailsReport :: Method Starts.");
		try {
			if (null != viewContentList && viewContentList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long successCount=0;
					long failureCount=0;
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
					
					// set totalCount
					totalCount= viewContentList.size();
					
					ArrayList<List<MCViewContentDetails>> partitions = new ArrayList<List<MCViewContentDetails>>();
					for (int i=0; i<viewContentList.size(); i += partitionSize) {
						partitions.add(viewContentList.subList(i, Math.min(i + partitionSize, viewContentList.size())));
					}
					
					if(null!=partitions && partitions.size()>0)
					{
						int listCount=0;
						for(List<MCViewContentDetails> subsetList : partitions)
						{
							if(null!=subsetList && subsetList.size()>0)
							{
								/*
								 * iterate Map and write the Excel File for each Index
								 */
								String fName = "/";
								if(listCount==0)
								{
									fName+="VIEW_CONTENT_REPORT.xlsx";
								}
								else
								{
									fName+="VIEW_CONTENT_REPORT_"+listCount+".xlsx";
								}
								// increment listCount
								listCount++;
								File myFile = PathUtil.file(path + fName);
								fName = null;
								// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
								SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

								// Create a new sheet
								Sheet mySheet = myWorkBook.createSheet("Details");
								/*
								 * Add Header Row
								 */
								Row headerRow = mySheet.createRow(0);
								Cell sourceFileNameCell = headerRow.createCell(0);
								sourceFileNameCell.setCellValue("SOURCE FILE");

								Cell documentIdCell = headerRow.createCell(1);
								documentIdCell.setCellValue("DOCUMENT ID");

								Cell dispOrdFileNameCell = headerRow.createCell(2);
								dispOrdFileNameCell.setCellValue("LOCALE");

								Cell lineTypeCell = headerRow.createCell(3);
								lineTypeCell.setCellValue("MODEL TYPE");

								Cell catTypeCell = headerRow.createCell(4);
								catTypeCell.setCellValue("MODEL");

								Cell dispOrdCodeLevel1 = headerRow.createCell(5);
								dispOrdCodeLevel1.setCellValue("CARLINE NAME");

								Cell dispOrdNameLevel1 = headerRow.createCell(6);
								dispOrdNameLevel1.setCellValue("CARLINE CODE");

								Cell dispOrdCodeLevel2 = headerRow.createCell(7);
								dispOrdCodeLevel2.setCellValue("WMI CODE");

								Cell dispOrdNameLevel2 = headerRow.createCell(8);
								dispOrdNameLevel2.setCellValue("VDS CODE");

								Cell dispOrdCodeLevel3 = headerRow.createCell(9);
								dispOrdCodeLevel3.setCellValue("VIS START RANGE");

								Cell dispOrdNameLevel3 = headerRow.createCell(10);
								dispOrdNameLevel3.setCellValue("VIS END RANGE");

								Cell dispOrdCodeLevel4 = headerRow.createCell(11);
								dispOrdCodeLevel4.setCellValue("DOCUMENT TYPE");

								Cell manualTypeLabel = headerRow.createCell(12);
								manualTypeLabel.setCellValue("DOCUMENT TYPE LABEL");

								Cell dispOrdNameLevel4 = headerRow.createCell(13);
								dispOrdNameLevel4.setCellValue("ESI CAT LEVEL 1 CODE");

								Cell dispOrdCodeLevel5 = headerRow.createCell(14);
								dispOrdCodeLevel5.setCellValue("ESI CAT LEVEL 1 NAME");

								Cell dispOrdNameLevel5 = headerRow.createCell(15);
								dispOrdNameLevel5.setCellValue("ESI CAT LEVEL 2 CODE");

								Cell dispOrdCodeLevel6 = headerRow.createCell(16);
								dispOrdCodeLevel6.setCellValue("ESI CAT LEVEL 2 NAME");

								Cell dispOrdNameLevel6 = headerRow.createCell(17);
								dispOrdNameLevel6.setCellValue("ESI CAT LEVEL 3 CODE");

								Cell seqNoCell = headerRow.createCell(18);
								seqNoCell.setCellValue("ESI CAT LEVEL 3 NAME");

								Cell manualTypeCell = headerRow.createCell(19);
								manualTypeCell.setCellValue("TITLE");

								Cell titleCell = headerRow.createCell(20);
								titleCell.setCellValue("ENGINE BOOK CODE");

								Cell engineTypeCell = headerRow.createCell(21);
								engineTypeCell.setCellValue("ENGINE BOOK NAME");

								Cell missionTypeCell = headerRow.createCell(22);
								missionTypeCell.setCellValue("MISSION BOOK CODE");

								Cell axleTypeCell = headerRow.createCell(23);
								axleTypeCell.setCellValue("MISSION BOOK NAME");

								Cell errorCodeCell = headerRow.createCell(24);
								errorCodeCell.setCellValue("DISP ORD NAME LEVEL 1");

								Cell errorCodeCell1 = headerRow.createCell(25);
								errorCodeCell1.setCellValue("DISP ORD NAME LEVEL 2");

								Cell errorCodeCell3 = headerRow.createCell(26);
								errorCodeCell3.setCellValue("DISP ORD NAME LEVEL 3");

								Cell errorCodeCell5 = headerRow.createCell(27);
								errorCodeCell5.setCellValue("DISP ORD NAME LEVEL 4");

								Cell errorCodeCell7 = headerRow.createCell(28);
								errorCodeCell7.setCellValue("DISP ORD NAME LEVEL 5");

								Cell errorCodeCell9 = headerRow.createCell(29);
								errorCodeCell9.setCellValue("DISP ORD NAME LEVEL 6");

								Cell errorMessageCella = headerRow.createCell(30);
								errorMessageCella.setCellValue("SEQUENCE NO");

								Cell errorCodeCellb = headerRow.createCell(31);
								errorCodeCellb.setCellValue("DOC CREATED DATE");

								Cell errorMessageCellc = headerRow.createCell(32);
								errorMessageCellc.setCellValue("DOC MODIFIED DATE");

								Cell errorCodeCelld = headerRow.createCell(33);
								errorCodeCelld.setCellValue("STATUS");

								Cell processingStatusCell = headerRow.createCell(34);
								processingStatusCell.setCellValue("REMARKS");

								int rowCount = 0;
								for (int a = 0; a < subsetList.size(); a++) {

									MCViewContentDetails details = (MCViewContentDetails) subsetList.get(a);
									// increment rowCount by 1
									rowCount++;
									// Create a new Row
									Row row = mySheet.createRow(rowCount);
									Cell cell0 = row.createCell(0);
									Cell cell1 = row.createCell(1);
									Cell cell2 = row.createCell(2);
									Cell cell3 = row.createCell(3);
									Cell cell4 = row.createCell(4);
									Cell cell5 = row.createCell(5);
									Cell cell6 = row.createCell(6);
									Cell cell7 = row.createCell(7);
									Cell cell8 = row.createCell(8);
									Cell cell9 = row.createCell(9);
									Cell cell10 = row.createCell(10);
									Cell cell11 = row.createCell(11);
									Cell cell12 = row.createCell(12);
									Cell cell13 = row.createCell(13);
									Cell cell14 = row.createCell(14);
									Cell cell15 = row.createCell(15);
									Cell cell16 = row.createCell(16);
									Cell cell17 = row.createCell(17);
									Cell cell18 = row.createCell(18);
									Cell cell19 = row.createCell(19);
									Cell cell20 = row.createCell(20);
									Cell cell21 = row.createCell(21);
									Cell cell22 = row.createCell(22);
									Cell cell23 = row.createCell(23);
									Cell cell25 = row.createCell(24);
									Cell cell27 = row.createCell(25);
									Cell cell29 = row.createCell(26);
									Cell cell31 = row.createCell(27);
									Cell cell33 = row.createCell(28);
									Cell cell35 = row.createCell(29);
									Cell cell36 = row.createCell(30);
									Cell cell37 = row.createCell(31);
									Cell cell38 = row.createCell(32);
									Cell cell39 = row.createCell(33);
									Cell cell40 = row.createCell(34);

									cell0.setCellValue("");
									cell1.setCellValue("");
									cell2.setCellValue("");
									cell3.setCellValue("");
									cell4.setCellValue("");
									cell5.setCellValue("");
									cell6.setCellValue("");
									cell7.setCellValue("");
									cell8.setCellValue("");
									cell9.setCellValue("");
									cell10.setCellValue("");
									cell11.setCellValue("");
									cell12.setCellValue("");
									cell13.setCellValue("");
									cell14.setCellValue("");
									cell15.setCellValue("");
									cell16.setCellValue("");
									cell17.setCellValue("");
									cell18.setCellValue("");
									cell19.setCellValue("");
									cell20.setCellValue("");
									cell21.setCellValue("");
									cell22.setCellValue("");
									cell23.setCellValue("");
									cell25.setCellValue("");
									cell27.setCellValue("");
									cell29.setCellValue("");
									cell31.setCellValue("");
									cell33.setCellValue("");
									cell35.setCellValue("");
									cell36.setCellValue("");
									cell37.setCellValue("");
									cell38.setCellValue("");
									cell39.setCellValue("");
									cell40.setCellValue("");

									if (null != details.getSourceFilePath() && !"".equals(details.getSourceFilePath())) 
									{
										cell0.setCellValue(details.getSourceFilePath());
									}

									if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
									{
										cell1.setCellValue(details.getDocumentId());
									}

									if(null!=details.getLocale() && !"".equals(details.getLocale()))
									{
										cell2.setCellValue(details.getLocale());
									}

									if(null!=details.getModelType() && !"".equals(details.getModelType()))
									{
										cell3.setCellValue(details.getModelType());
									}

									if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
									{
										cell4.setCellValue(details.getCarlineNameEng());
									}

									if(null!=details.getCarlineNameReg() && !"".equals(details.getCarlineNameReg()))
									{
										cell5.setCellValue(details.getCarlineNameReg());
									}

									if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
									{
										cell6.setCellValue(details.getCarlineCode());
									}

									if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
									{
										cell7.setCellValue(details.getWmiCode());
									}

									if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
									{
										cell8.setCellValue(details.getVdsCode());
									}

									if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
									{
										cell9.setCellValue(details.getVisStartRange());
									}

									if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
									{
										cell10.setCellValue(details.getVisEndRange());
									}

									if(null!=details.getManualType() && !"".equals(details.getManualType()))
									{
										cell11.setCellValue(details.getManualType());
									}

									if(null!=details.getManualTypeLabel() && !"".equals(details.getManualTypeLabel()))
									{
										cell12.setCellValue(details.getManualTypeLabel());
									}

									if(null!=details.getEsiCatLevel1Code() && !"".equals(details.getEsiCatLevel1Code()))
									{
										cell13.setCellValue(details.getEsiCatLevel1Code());
									}

									if(null!=details.getEsiCatLevel1Name() && !"".equals(details.getEsiCatLevel1Name()))
									{
										cell14.setCellValue(details.getEsiCatLevel1Name());
									}

									if(null!=details.getEsiCatLevel2Code() && !"".equals(details.getEsiCatLevel2Code()))
									{
										cell15.setCellValue(details.getEsiCatLevel2Code());
									}

									if(null!=details.getEsiCatLevel2Name() && !"".equals(details.getEsiCatLevel2Name()))
									{
										cell16.setCellValue(details.getEsiCatLevel2Name());
									}

									if(null!=details.getEsiCatLevel3Code() && !"".equals(details.getEsiCatLevel3Code()))
									{
										cell17.setCellValue(details.getEsiCatLevel3Code());
									}

									if(null!=details.getEsiCatLevel3Name() && !"".equals(details.getEsiCatLevel3Name()))
									{
										cell18.setCellValue(details.getEsiCatLevel3Name());
									}

									if(null!=details.getTitle() && !"".equals(details.getTitle()))
									{
										cell19.setCellValue(details.getTitle());
									}

									if(null!=details.getEngineBookCode() && !"".equals(details.getEngineBookCode()))
									{
										cell20.setCellValue(details.getEngineBookCode());
									}

									if(null!=details.getEngineBookName() && !"".equals(details.getEngineBookName()))
									{
										cell21.setCellValue(details.getEngineBookName());
									}

									if(null!=details.getMissionBookCode() && !"".equals(details.getMissionBookCode()))
									{
										cell22.setCellValue(details.getMissionBookCode());
									}

									if(null!=details.getMissionBookName() && !"".equals(details.getMissionBookName()))
									{
										cell23.setCellValue(details.getMissionBookName());
									}

									if(null!=details.getDisplayOrderNameLevel1() && !"".equals(details.getDisplayOrderNameLevel1()))
									{
										cell25.setCellValue(details.getDisplayOrderNameLevel1());
									}

									if(null!=details.getDisplayOrderNameLevel2() && !"".equals(details.getDisplayOrderNameLevel2()))
									{
										cell27.setCellValue(details.getDisplayOrderNameLevel2());
									}

									if(null!=details.getDisplayOrderNameLevel3() && !"".equals(details.getDisplayOrderNameLevel3()))
									{
										cell29.setCellValue(details.getDisplayOrderNameLevel3());
									}

									if(null!=details.getDisplayOrderNameLevel4() && !"".equals(details.getDisplayOrderNameLevel4()))
									{
										cell31.setCellValue(details.getDisplayOrderNameLevel4());
									}

									if(null!=details.getDisplayOrderNameLevel5() && !"".equals(details.getDisplayOrderNameLevel5()))
									{
										cell33.setCellValue(details.getDisplayOrderNameLevel5());
									}

									if(null!=details.getDisplayOrderNameLevel6() && !"".equals(details.getDisplayOrderNameLevel6()))
									{
										cell35.setCellValue(details.getDisplayOrderNameLevel6());
									}

									if(null!=details.getDisplayOrderSequenceNo() && !"".equals(details.getDisplayOrderSequenceNo()))
									{
										cell36.setCellValue(details.getDisplayOrderSequenceNo());
									}

									if(null!=details.getImDocCreateDate())
									{
										cell37.setCellValue(sdf.format(details.getImDocCreateDate()));
									}

									if(null!=details.getImDocModifiedDate())
									{
										cell38.setCellValue(sdf.format(details.getImDocModifiedDate()));
									}

									if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
									{
										cell39.setCellValue(details.getProcessingStatus());
										if(details.getProcessingStatus().trim().toLowerCase().equals("success"))
										{
											// increment successCount
											successCount++;
										}
									}

									if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
									{
										cell40.setCellValue(details.getRemarks());
									}

									cell40=null;
									cell39 = null;
									cell38 = null;
									cell37 = null;
									cell36 = null;
									cell35 = null;
									cell33 = null;
									cell31 = null;
									cell29 = null;
									cell27 = null;
									cell25 = null;
									cell25=null;
									cell23=null;
									cell22=null;
									cell21=null;
									cell20=null;
									cell19=null;
									cell18=null;
									cell17=null;
									cell16=null;
									cell15=null;
									cell14=null;
									cell13=null;
									cell12=null;
									cell11=null;
									cell10=null;
									cell9=null;
									cell8=null;
									cell7=null;
									cell6 = null;
									cell5 = null;
									cell4 = null;
									cell3 = null;
									cell2 = null;
									cell1 = null;
									cell0 = null;
									row = null;
									details=  null;
								}
								
								headerRow = null;
								sourceFileNameCell = null;
								documentIdCell = null;
								lineTypeCell= null;
								dispOrdFileNameCell = null;
								catTypeCell = null;
								dispOrdCodeLevel1 =null;
								dispOrdCodeLevel2 =null;
								dispOrdCodeLevel3 =null;
								dispOrdCodeLevel4 =null;
								dispOrdCodeLevel5 =null;
								dispOrdCodeLevel6 =null;
								dispOrdNameLevel1 =null;
								dispOrdNameLevel2 =null;
								dispOrdNameLevel3 =null;
								dispOrdNameLevel4 =null;
								dispOrdNameLevel5 =null;
								dispOrdNameLevel6 =null;
								seqNoCell=  null;
								titleCell= null;
								engineTypeCell= null;
								missionTypeCell= null;
								axleTypeCell= null;
								errorCodeCell= null;
								manualTypeCell= null;
								processingStatusCell= null;
								errorCodeCell1 = null;
								errorCodeCell3 = null;
								errorCodeCell5 = null;
								errorCodeCell7 = null;
								errorCodeCell9 = null;
								errorCodeCellb = null;
								errorCodeCelld = null;
								errorMessageCella = null;
								errorMessageCellc = null;

								FileOutputStream os = PathUtil.fileOutputStream(myFile);
								myWorkBook.write(os);
								logger.info("Writing on VIEW CONTENT DETAILS REPORT XLSX file Finished ...");
								os.flush();
								os.close();

								// set mySheet to null
								mySheet = null;
								// set myWorkBook to null
								myWorkBook = null;
								// set myFile to null
								myFile = null;

							}
							subsetList  =null;
						}
					}
					partitions = null;
					// set path to null
					path = null;
					
					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - successCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(successCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_VIEWCONTENT_MC);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentDetailsReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					successCount= 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentDetailsReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printViewContentDetailsReport :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printWindowJSReport(String scheduleCode , ArrayList<WindowJSDetails> windowJSList) {
		if (logger.isInfoEnabled())
			logger.info("printWindowJSReport :: Method Starts.");
		try {
			if (null != windowJSList && windowJSList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					long processedCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "WINDOW_JS_REPORT.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell headCell=null;
					String headers="FILE PATH,FILE NAME,LOCALE,MODEL FOLDER NAME,MANUALTYPE,FACELIFT FOLDER,MATERIAL FOLDER,"
							+ "INNERLINK PATH,IM DOCUMENT ID,IC PATH,STATUS,ERROR MESSAGE";
					String[] tokens=headers.split(",");
					for(int a=0;a<tokens.length;a++)
					{
						headCell = headerRow.createCell(a);
						headCell.setCellValue(tokens[a]);
						headCell = null;
					}
					tokens = null;
					headers = null;headerRow=null;

					// set totalCount
					totalCount = windowJSList.size();
					int rowCount = 0;
					Row row=null;
					Cell dataCell = null;
					String dataString="";
					for (int a = 0; a < windowJSList.size(); a++) {
						WindowJSDetails jsDetails = (WindowJSDetails) windowJSList.get(a);

						ContentDetails con = new ContentDetails();
						if(null!=jsDetails.getContentDetails() && !"".equals(jsDetails.getContentDetails()))
						{
							con = jsDetails.getContentDetails();
						}

						if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
						{
							jsDetails.setErrorMessage("Failed to identify IM Document Id.");
						}
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dataString=jsDetails.getFilePath()+"<TOK_SPERATOR>"+jsDetails.getFileName()+"<TOK_SPERATOR>";
						dataString+=con.getLocale()+"<TOK_SPERATOR>"+con.getModelFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getManualType()+"<TOK_SPERATOR>"+con.getFaceLiftFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getMaterialName()+"<TOK_SPERATOR>"+jsDetails.getInnerLinkPath()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getInnerLinkDocumentId()+"<TOK_SPERATOR>"+jsDetails.getPreapredInnerLinkURLForIC()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getProcessingStatus()+"<TOK_SPERATOR>"+jsDetails.getErrorMessage();

						if(null!=jsDetails.getProcessingStatus() && jsDetails.getProcessingStatus().equals("SUCCESS"))
						{
							processedCount++;
						}

						tokens = dataString.split("<TOK_SPERATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int r=0;r<tokens.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=tokens[r] && !"".equals(tokens[r]) && !"null".equals(tokens[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[r]);
								}
								dataCell=null;
							}
						}
						tokens=null;
						dataCell = null;
						row=null;
						dataString = null;


						con = null;
						jsDetails = null;
					}

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on WINDOW JS REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - processedCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(processedCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_WINDOW_JS);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printWindowJSReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printWindowJSReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printWindowJSReport :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printVoltageMapJSReport(String scheduleCode , ArrayList<WindowJSDetails> voltageMapJSList) 
	{
		if (logger.isInfoEnabled())
			logger.info("printVoltageMapJSReport :: Method Starts.");
		try 
		{
			if (null != voltageMapJSList && voltageMapJSList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					long processedCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "VOLTAGE_MAP_JS_REPORT.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell headCell=null;
					String headers="FILE PATH,FILE NAME,LOCALE,MODEL FOLDER NAME,MANUALTYPE,FACELIFT FOLDER,MATERIAL FOLDER,"
							+ "INNERLINK PATH,IM DOCUMENT ID,IC PATH,STATUS,ERROR MESSAGE";
					String[] tokens=headers.split(",");
					for(int a=0;a<tokens.length;a++)
					{
						headCell = headerRow.createCell(a);
						headCell.setCellValue(tokens[a]);
						headCell = null;
					}
					tokens = null;
					headers = null;headerRow=null;

					// set totalCount
					totalCount = voltageMapJSList.size();
					int rowCount = 0;
					Row row=null;
					Cell dataCell = null;
					String dataString="";
					WindowJSDetails jsDetails = null;
					ContentDetails con = null;
					for (int a = 0; a < voltageMapJSList.size(); a++) {
						jsDetails = (WindowJSDetails) voltageMapJSList.get(a);

						con = new ContentDetails();
						if(null!=jsDetails.getContentDetails() && !"".equals(jsDetails.getContentDetails()))
						{
							con = jsDetails.getContentDetails();
						}

						if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
						{
							jsDetails.setErrorMessage("Failed to identify IM Document Id.");
						}
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dataString=jsDetails.getFilePath()+"<TOK_SPERATOR>"+jsDetails.getFileName()+"<TOK_SPERATOR>";
						dataString+=con.getLocale()+"<TOK_SPERATOR>"+con.getModelFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getManualType()+"<TOK_SPERATOR>"+con.getFaceLiftFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getMaterialName()+"<TOK_SPERATOR>"+jsDetails.getInnerLinkPath()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getInnerLinkDocumentId()+"<TOK_SPERATOR>"+jsDetails.getPreapredInnerLinkURLForIC()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getProcessingStatus()+"<TOK_SPERATOR>"+jsDetails.getErrorMessage();

						if(null!=jsDetails.getProcessingStatus() && jsDetails.getProcessingStatus().equals("SUCCESS"))
						{
							processedCount++;
						}

						tokens = dataString.split("<TOK_SPERATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int r=0;r<tokens.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=tokens[r] && !"".equals(tokens[r]) && !"null".equals(tokens[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[r]);
								}
								dataCell=null;
							}
						}
						tokens=null;
						dataCell = null;
						row=null;
						dataString = null;


						con = null;
						jsDetails = null;
					}

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on VOLTAGE MAP JS REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - processedCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(processedCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_VOLTAGE_MAP_JS);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVoltageMapJSReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVoltageMapJSReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printVoltageMapJSReport :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printVoltageLinkMapJSReport(String scheduleCode , ArrayList<WindowJSDetails> voltageLinkMapJSList) 
	{
		if (logger.isInfoEnabled())
			logger.info("printVoltageLinkMapJSReport :: Method Starts.");
		try 
		{
			if (null != voltageLinkMapJSList && voltageLinkMapJSList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					long processedCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "VOLTAGE_LINK_MAP_JS_REPORT.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell headCell=null;
					String headers="FILE PATH,FILE NAME,LOCALE,MODEL FOLDER NAME,MANUALTYPE,FACELIFT FOLDER,MATERIAL FOLDER,"
							+ "INNERLINK PATH,IM DOCUMENT ID,IC PATH,STATUS,ERROR MESSAGE";
					String[] tokens=headers.split(",");
					for(int a=0;a<tokens.length;a++)
					{
						headCell = headerRow.createCell(a);
						headCell.setCellValue(tokens[a]);
						headCell = null;
					}
					tokens = null;
					headers = null;headerRow=null;

					// set totalCount
					totalCount = voltageLinkMapJSList.size();
					int rowCount = 0;
					Row row=null;
					Cell dataCell = null;
					String dataString="";
					WindowJSDetails jsDetails = null;
					ContentDetails con = null;
					for (int a = 0; a < voltageLinkMapJSList.size(); a++) {
						jsDetails = (WindowJSDetails) voltageLinkMapJSList.get(a);

						con = new ContentDetails();
						if(null!=jsDetails.getContentDetails())
						{
							con = jsDetails.getContentDetails();
						}

						if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
						{
							jsDetails.setErrorMessage("Failed to identify IM Document Id.");
						}
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dataString=jsDetails.getFilePath()+"<TOK_SPERATOR>"+jsDetails.getFileName()+"<TOK_SPERATOR>";
						dataString+=con.getLocale()+"<TOK_SPERATOR>"+con.getModelFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getManualType()+"<TOK_SPERATOR>"+con.getFaceLiftFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getMaterialName()+"<TOK_SPERATOR>"+jsDetails.getInnerLinkPath()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getInnerLinkDocumentId()+"<TOK_SPERATOR>"+jsDetails.getPreapredInnerLinkURLForIC()+"<TOK_SPERATOR>";
						dataString+=jsDetails.getProcessingStatus()+"<TOK_SPERATOR>"+jsDetails.getErrorMessage();

						if(null!=jsDetails.getProcessingStatus() && jsDetails.getProcessingStatus().equals("SUCCESS"))
						{
							processedCount++;
						}

						tokens = dataString.split("<TOK_SPERATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int r=0;r<tokens.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=tokens[r] && !"".equals(tokens[r]) && !"null".equals(tokens[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[r]);
								}
								dataCell=null;
							}
						}
						tokens=null;
						dataCell = null;
						row=null;
						dataString = null;


						con = null;
						jsDetails = null;
					}

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on VOLTAGE LINK MAP JS REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - processedCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(processedCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_VOLTAGE_LINK_MAP_JS);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVoltageLinkMapJSReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVoltageLinkMapJSReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printVoltageLinkMapJSReport :: Method Ends.");
	}

	
	@SuppressWarnings("resource")
	public void printVinMLMappingReport(String scheduleCode , ArrayList<VinMLMappingDetails> vinMLMappingList) {
		if (logger.isInfoEnabled())
			logger.info("printVinMLMappingReport :: Method Starts.");
		try {
			if (null != vinMLMappingList && vinMLMappingList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					long processedCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties
							.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "VIN_MANUAL_TYPE_MAPPING_REPORT.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell headCell=null;
					String headers="LOCALE FOLDER,MODEL FOLDER,MANUAL TYPE FOLDER,FACELIFT FOLDER,MATERIAL FOLDER,PROCESSING STATUS,LOCALE,CARLINE,WMI CODE,VDS CODE,"
							+ "VIS START RANGE,VIS END RANGE,WM_MAPPING,BSM_MAPPING,MC_MAPPING,AT_MAPPING,MT_MAPPING,ENG_MAPPING,TG_MAPPING,TQG_MAPPING,"
							+ "WD_MAPPING,DM_MAPPING,MCM_MAPPING,RQ_MAPPING,ERROR CODE,ERROR MESSAGE";
					String[] tokens=headers.split(",");
					for(int a=0;a<tokens.length;a++)
					{
						headCell = headerRow.createCell(a);
						headCell.setCellValue(tokens[a]);
						headCell = null;
					}
					tokens = null;
					headers = null;headerRow=null;

					// set totalCount
					totalCount = vinMLMappingList.size();
					int rowCount = 0;
					Row row=null;
					Cell dataCell = null;
					String dataString="";
					VinMLMappingDetails mappingDetails = null;
					ScheduleItemDetails con = new ScheduleItemDetails();
					for (int a = 0; a < vinMLMappingList.size(); a++) {
						mappingDetails = (VinMLMappingDetails) vinMLMappingList.get(a);

						con = new ScheduleItemDetails();
						if(null!=mappingDetails.getItemDetails())
						{
							con = mappingDetails.getItemDetails();
						}
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dataString=con.getLocale()+"<TOK_SPERATOR>"+con.getModelFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getManualType()+"<TOK_SPERATOR>"+con.getFaceLiftFolderName()+"<TOK_SPERATOR>";
						dataString+=con.getMaterialFolderName()+"<TOK_SPERATOR>"+mappingDetails.getProcessingStatus()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getLocaleCode()+"<TOK_SPERATOR>"+mappingDetails.getCarlineCode()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getWmiCode()+"<TOK_SPERATOR>"+mappingDetails.getVdsCode()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getVisStartRange()+"<TOK_SPERATOR>"+mappingDetails.getVisEndRange()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getWmMappingLocale()+"<TOK_SPERATOR>"+mappingDetails.getBsmMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getMcMappingLocale()+"<TOK_SPERATOR>"+mappingDetails.getAtMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getMtMappingLocale()+"<TOK_SPERATOR>"+mappingDetails.getEngineMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getTgMappingLocale()+"<TOK_SPERATOR>"+mappingDetails.getTqgMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getWdMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getDmMappingLocale()+"<TOK_SPERATOR>"+mappingDetails.getMcmMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getRqMappingLocale()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getErrorCode()+"<TOK_SPERATOR>"+mappingDetails.getErrorMessage()+"<TOK_SPERATOR>";

						if(null!=mappingDetails.getProcessingStatus() && mappingDetails.getProcessingStatus().equals(AutomationConstants.STATUS_SUCCESS))
						{
							processedCount++;
						}

						tokens = dataString.split("<TOK_SPERATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int r=0;r<tokens.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=tokens[r] && !"".equals(tokens[r]) && !"null".equals(tokens[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[r]);
								}
								dataCell=null;
							}
						}
						tokens=null;
						dataCell = null;
						row=null;
						dataString = null;


						con = null;
						mappingDetails = null;
					}
					con = null;
					mappingDetails=  null;

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on VIN MANUAL TYPE MAPPING REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - processedCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(processedCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_VIN_ML_MAPPING);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVinMLMappingReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVinMLMappingReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printVinMLMappingReport :: Method Ends.");
	}

	@SuppressWarnings("resource")
	public void printSCMVINMappingReport(String scheduleCode , ArrayList<VinDetails> scmVINList) {
		if (logger.isInfoEnabled())
			logger.info("printSCMVINMappingReport :: Method Starts.");
		try {
			if (null != scmVINList && scmVINList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long failureCount=0;
					long processedCount=0;
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File wslDirectory = PathUtil.file(path);
					if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
						// create new directory
						wslDirectory.mkdir();
					}
					wslDirectory = null;
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path + "/" + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + "SCM_VIN_MAPPING_REPORT.xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					Row headerRow = mySheet.createRow(0);
					Cell headCell=null;
					String headers="MANUAL TYPE FOLDER,FACELIFT FOLDER,MATERIAL FOLDER,PROCESSING STATUS,LOCALE,CARLINE CODE,MODEL NAME,WMI CODE,VDS CODE,"
							+ "VIS START RANGE,VIS END RANGE,IM_DOCUMENT_ID,DOC_SOURCE_LOC,DOC_FILE_NAME,DOC_TITLE,ERROR CODE,ERROR MESSAGE";
					String[] tokens=headers.split(",");
					for(int a=0;a<tokens.length;a++)
					{
						headCell = headerRow.createCell(a);
						headCell.setCellValue(tokens[a]);
						headCell = null;
					}
					tokens = null;
					headers = null;headerRow=null;

					// set totalCount
					totalCount = scmVINList.size();
					int rowCount = 0;
					Row row=null;
					Cell dataCell = null;
					String dataString="";
					VinDetails mappingDetails = null;
					for (int a = 0; a < scmVINList.size(); a++) {
						mappingDetails = (VinDetails) scmVINList.get(a);
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dataString=mappingDetails.getManualType()+"<TOK_SPERATOR>"+mappingDetails.getFaceLiftFolder()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getMaterialFolder()+"<TOK_SPERATOR>"+mappingDetails.getProcessingStatus()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getLocale()+"<TOK_SPERATOR>"+mappingDetails.getCarlineCode()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getCarlineNameEng()+"<TOK_SPERATOR>"+mappingDetails.getWmiCode()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getVdsCode()+"<TOK_SPERATOR>"+mappingDetails.getVisStartRange()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getVisEndRange()+"<TOK_SPERATOR>"+mappingDetails.getDocumentId()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getDocSourceFilePath()+"<TOK_SPERATOR>"+mappingDetails.getDocSourceFileName()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getDocTitle()+"<TOK_SPERATOR>";
						dataString+=mappingDetails.getErrorCode()+"<TOK_SPERATOR>"+mappingDetails.getErrorMessage()+"<TOK_SPERATOR>";

						if(null!=mappingDetails.getProcessingStatus() && mappingDetails.getProcessingStatus().equals(AutomationConstants.STATUS_SUCCESS))
						{
							processedCount++;
						}

						tokens = dataString.split("<TOK_SPERATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int r=0;r<tokens.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=tokens[r] && !"".equals(tokens[r]) && !"null".equals(tokens[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[r]);
								}
								dataCell=null;
							}
						}
						tokens=null;
						dataCell = null;
						row=null;
						dataString = null;
						mappingDetails = null;
					}
					mappingDetails=  null;

					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("Writing on SCM VIN MAPPING REPORT XLSX file Finished ...");
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set myFile to null
					myFile = null;

					/*
					 * PROCEED FOR SAVING SUMMARY REPORT
					 */
					if(totalCount>0)
					{
						// calculate failure Count
						failureCount =totalCount - processedCount;

						// PREAPRE REPORTS SUMMARY VO AND SAVE DATA FOR THE SCHEDULE AS REPORT SUMMARY
						ReportsSummaryDetails rsd = new ReportsSummaryDetails();
						rsd.setSchduleCode(scheduleCode);
						rsd.setFailureCount(failureCount);
						rsd.setTotalCount(totalCount);
						rsd.setSuccessCount(processedCount);
						// set report Name as Transaction Report
						rsd.setReportName(ReportsSummaryDetails.REPORT_SCM_VIN_MAPPING);
						// set status, if failure Count is more than 0 - then failure
						if(failureCount>0)
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_FAILURE);
						}
						else
						{
							rsd.setReportStatus(ReportsSummaryDetails.STATUS_SUCCESS);
						}
						// UPDATE SUMMARY DETAILS
						try
						{
							ScheduleDAO.saveReportSummaryDetailsForSchedule(rsd);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printSCMVINMappingReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printSCMVINMappingReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printSCMVINMappingReport :: Method Ends.");
	}


}