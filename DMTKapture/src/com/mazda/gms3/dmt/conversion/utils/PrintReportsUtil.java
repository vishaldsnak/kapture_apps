package com.mazda.gms3.dmt.conversion.utils;

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

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.WindowJSDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.ErrorDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.MNAOModelYearViewContentDetails;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;

public class PrintReportsUtil {

	Logger logger = LogManager.getLogger(PrintReportsUtil.class);
	private int partitionSize=700000;
	private SXSSFWorkbook myWorkBook;
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
									excelReports.delete();
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
	public void printSingleFailureReport(String scheduleCode, ArrayList<ErrorDetails> imErrorDetailsList, ArrayList<Map<Object, Object>> databaseFailureList, 
			ArrayList<Map<Object, Object>> xcopyFailureList, ArrayList<ErrorDetails> imErrorDeleteDetailsList, ArrayList<Map<Object, Object>> databaseFailureListForDelete) 
	{
		if (logger.isInfoEnabled())
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
					Cell modelCell = headerRow.createCell(5);
					modelCell.setCellValue("MODEL");
					Cell carlineCodeCell = headerRow.createCell(6);
					carlineCodeCell.setCellValue("MODEL_FOLDER_NAME");
					Cell manualTypeCell = headerRow.createCell(7);
					manualTypeCell.setCellValue("MANUAL_TYPE");
					Cell operationTypeCell = headerRow.createCell(8);
					operationTypeCell.setCellValue("OPERATION_TYPE");
					Cell errorCodeCell = headerRow.createCell(9);
					errorCodeCell.setCellValue("ERROR_CODE");
					Cell errorMessageCell = headerRow.createCell(10);
					errorMessageCell.setCellValue("ERROR_MESSAGE");
					Cell imDocumentIdCell = headerRow.createCell(11);
					imDocumentIdCell.setCellValue("IM_DOCUMENT_ID");
					Cell destinationPathCell = headerRow.createCell(12);
					destinationPathCell.setCellValue("DESTINATION_PATH");
					Cell dateTimeStampCell = headerRow.createCell(13);
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
						// set totalCount
						totalCount = imErrorList.size();
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
							if (null != con.getModel()  && !"".equals(con.getModel())) 
							{
								cell5.setCellValue(String.valueOf(con.getModel()));
							} 
							else 
							{
								cell5.setCellValue("");
							}
							
							Cell cell6 = row.createCell(6);
							if (null != con.getModelFolderName()  && !"".equals(con.getModelFolderName())) 
							{
								cell6.setCellValue(String.valueOf(con.getModelFolderName()));
							} 
							else 
							{
								cell6.setCellValue("");
							}
							
							Cell cell7 = row.createCell(7);
							if (null != con.getManualType() && !"".equals(con.getManualType()))
							{
								cell7.setCellValue(con.getManualType());
							} 
							else 
							{
								cell7.setCellValue("");
							}
							
							Cell cell8 = row.createCell(8);
							if (null != errorDetails.getOperationType()  && !"".equals(errorDetails.getOperationType()))
							{
								cell8.setCellValue(String.valueOf(errorDetails.getOperationType()));
							} 
							else 
							{
								cell8.setCellValue("");
							}

							
							Cell cell9 = row.createCell(9);
							if (null != errorDetails.getErrorCode()  && !"".equals(errorDetails.getErrorCode())) 
							{
								cell9.setCellValue(String.valueOf(errorDetails.getErrorCode()));
							} 
							else 
							{
								cell9.setCellValue("");
							}

							Cell cell10 = row.createCell(10);
							if (null != errorDetails.getErrorMessage() 	&& !"".equals(errorDetails.getErrorMessage())) 
							{
								cell10.setCellValue(String.valueOf(errorDetails.getErrorMessage()));
							} 
							else 
							{
								cell10.setCellValue("");
							}

							Cell cell11 = row.createCell(11);
							if (null != con.getImDocumentId()	&& !"".equals(con.getImDocumentId())) 
							{
								cell11.setCellValue(con.getImDocumentId());
							}
							else 
							{
								cell11.setCellValue("");
							}

							
							Cell cell13 = row.createCell(13);
							if (null != errorDetails.getDateTime()) 
							{
								SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
								cell13.setCellValue(String.valueOf(sdf.format(errorDetails.getDateTime())));
								sdf = null;
							}
							else 
							{
								cell13.setCellValue("");
							}
							
							
							
							cell11 = null;
							cell13 = null;
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
						totalCount= totalCount+ dbErrorList.size();
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
							Cell cell13 = row.createCell(13);
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
							cell13.setCellValue("");

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
							if(null!=con.getModel() && !"".equals(con.getModel()))
							{
								cell5.setCellValue(con.getModel());
							}
							if(null!=con.getModelFolderName() && !"".equals(con.getModelFolderName()))
							{
								cell6.setCellValue(con.getModelFolderName());
							}
							if(null!=con.getManualType() && !"".equals(con.getManualType()))
							{
								cell7.setCellValue(con.getManualType());
							}
							
							if (null != dataMap.get("OPERATION_TYPE")) {
								cell8.setCellValue((String) dataMap.get("OPERATION_TYPE"));
							}
							if (null != dataMap.get("ERROR_CODE")) {
								cell9.setCellValue((String) dataMap.get("ERROR_CODE"));
							}
							if (null != dataMap.get("ERROR_MESSAGE")) {
								cell10.setCellValue((String) dataMap.get("ERROR_MESSAGE"));
							}

							if (null != dataMap.get("IM_DOCUMENT_ID")) {
								cell11.setCellValue((String) dataMap.get("IM_DOCUMENT_ID"));
							}

							if (null != dataMap.get("DATE_TIME")) {
								cell13.setCellValue((String) dataMap.get("DATE_TIME"));
							}

							// set all used variables to null
							cell13 = null;
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
						totalCount= totalCount + xcopyFailureList.size();
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
							Cell cell10 = row.createCell(10);
							Cell cell11 = row.createCell(11);
							Cell cell12 = row.createCell(12);
							Cell cell13 = row.createCell(13);
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
							cell10.setCellValue("");
							cell11.setCellValue("");
							cell12.setCellValue("");
							cell13.setCellValue("");

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
							if(null!=con.getModel() && !"".equals(con.getModel()))
							{
								cell5.setCellValue(con.getModel());
							}
							if(null!=con.getModelFolderName() && !"".equals(con.getModelFolderName()))
							{
								cell6.setCellValue(con.getModelFolderName());
							}
							if(null!=con.getManualType() && !"".equals(con.getManualType()))
							{
								cell7.setCellValue(con.getManualType());
							}
							
							if (null != dataMap.get("ERROR_MESSAGE")) {
								cell10.setCellValue((String) dataMap.get("ERROR_MESSAGE"));
							}

							if(null!=con.getImDocumentId() && !"".equals(con.getImDocumentId()))
							{
								cell11.setCellValue(con.getImDocumentId());
							}
							if (null != dataMap.get("DESTINATION_PATH")) {
								cell12.setCellValue((String) dataMap.get("DESTINATION_PATH"));
							}

							if (null != dataMap.get("DATE_TIME")) {
								cell13.setCellValue((String) dataMap.get("DATE_TIME"));
							}

							// set all used variables to null
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
							cell10 = null;
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
					
					// set totalCount
					totalCount = missingCategoryList.size();

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/"+"MISSING_CATEGORIES.xlsx";
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
					Cell modelNameCell = headerRow.createCell(7);
					modelNameCell.setCellValue("MODEL");
					Cell carlineCodeCell = headerRow.createCell(8);
					carlineCodeCell.setCellValue("CARLINE CODE");
					Cell wmiCodeCell = headerRow.createCell(9);
					wmiCodeCell.setCellValue("WMI CODE");
					Cell vdsCodeCell = headerRow.createCell(10);
					vdsCodeCell.setCellValue("VDS CODE");
					Cell remarksCell = headerRow.createCell(11);
					remarksCell.setCellValue("REMARKS");
					Cell errorCodeCell = headerRow.createCell(12);
					errorCodeCell.setCellValue("ERROR CODES");
					Cell errorMessagesCell = headerRow.createCell(13);
					errorMessagesCell.setCellValue("ERROR MESSAGES");
					
					int rowCount = 0;
					for (int a = 0; a < missingCategoryList.size(); a++) {
						CategoryDetails catDetails = (CategoryDetails) missingCategoryList.get(a);

						ContentDetails con = new ContentDetails();
						if(null!=catDetails.getContentDetails())
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
						Cell cell8 = row.createCell(8);
						Cell cell9 = row.createCell(9);
						Cell cell10 = row.createCell(10);
						Cell cell11 = row.createCell(11);
						Cell cell12 = row.createCell(12);
						Cell cell13 = row.createCell(13);

						cell7.setCellValue("");
						cell8.setCellValue("");
						cell9.setCellValue("");
						cell10.setCellValue("");
						cell11.setCellValue("");
						cell12.setCellValue("");
						cell13.setCellValue("");
						
						if(null!=catDetails.getModel() && !"".equals(catDetails.getModel()))
						{
							cell7.setCellValue(catDetails.getModel());
						}
						if(null!=catDetails.getCarlineCode() && !"".equals(catDetails.getCarlineCode()))
						{
							cell8.setCellValue(catDetails.getCarlineCode());
						}
						if(null!=catDetails.getWmiCode() && !"".equals(catDetails.getWmiCode()))
						{
							cell9.setCellValue(catDetails.getWmiCode());
						}
						if(null!=catDetails.getVdsCode() && !"".equals(catDetails.getVdsCode()))
						{
							cell10.setCellValue(catDetails.getVdsCode());
						}
						if(null!=catDetails.getRemarks() && !"".equals(catDetails.getRemarks()))
						{
							cell11.setCellValue(catDetails.getRemarks());
						}
						if (null != catDetails.getErrorCodes() && !"".equals(catDetails.getErrorCodes())) 
						{
							cell12.setCellValue(catDetails.getErrorCodes());
						}
						
						if (null != catDetails.getErrorMessage() && !"".equals(catDetails.getErrorMessage())) 
						{
							cell13.setCellValue(catDetails.getErrorMessage());
						}
						
						cell13=null;
						cell12=null;
						cell11=null;
						cell10=null;
						cell9=null;
						cell8=null;
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
					modelNameCell = null;
					carlineCodeCell = null;
					wmiCodeCell=null;
					vdsCodeCell = null;
					remarksCell = null;
					errorCodeCell = null;
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
	public void printProcessingFilesReport(String scheduleCode, ArrayList<ContentDetails> processingFileDetailsList, ArrayList<Map<Object, Object>> xcopyFileProcessingList, ArrayList<ContentDetails> processingFileDetailsListForDelete)
	{
		if (logger.isInfoEnabled())
			logger.info("printProcessingFilesReport :: Method Starts.");
		try {
			if (null != scheduleCode && !"".equals(scheduleCode)) 
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
				
				Cell modelCell = headerRow.createCell(4);
				modelCell.setCellValue("MODEL");
				Cell modelFolderCell = headerRow.createCell(5);
				modelFolderCell.setCellValue("MODEL FOLDER NAME");

				Cell manualTypeCell = headerRow.createCell(6);
				manualTypeCell.setCellValue("MANUAL TYPE");
				Cell materialNameCell = headerRow.createCell(7);
				materialNameCell.setCellValue("MATERIAL NAME");
				
				Cell channelCell = headerRow.createCell(8);
				channelCell.setCellValue("CHANNEL");
				Cell imDocumentIdCell = headerRow.createCell(9);
				imDocumentIdCell.setCellValue("DOCUMENT ID");
				Cell currentVersionCell = headerRow.createCell(10);
				currentVersionCell.setCellValue("DOCUMENT VERSION");
				Cell sourceFileLocCell = headerRow.createCell(11);
				sourceFileLocCell.setCellValue("SOURCE FILE LOCATION");
				Cell sourceFileNameCell = headerRow.createCell(12);
				sourceFileNameCell.setCellValue("SOURCE FILE NAME");
				Cell processingStatusCell = headerRow.createCell(13);
				processingStatusCell.setCellValue("PROCESSING STATUS");
				Cell xcopyprocessingStatusCell = headerRow.createCell(14);
				xcopyprocessingStatusCell.setCellValue("XCOPY STATUS");
				Cell okAssetsDestinationCell = headerRow.createCell(15);
				okAssetsDestinationCell.setCellValue("DESTINATINATION DIRECTORY");
				Cell remarksCell = headerRow.createCell(16);
				remarksCell.setCellValue("REMARKS");
				Cell dateTimeCell = headerRow.createCell(17);
				dateTimeCell.setCellValue("TIME");
				
				Cell esiCategoryMappedCell = headerRow.createCell(18);
				esiCategoryMappedCell.setCellValue("ESI MAPPED");
				Cell esiCategoryTaxonomyCell = headerRow.createCell(19);
				esiCategoryTaxonomyCell.setCellValue("ESI TAXANOMY");
				Cell esiCategoryMappedLevelCell = headerRow.createCell(20);
				esiCategoryMappedLevelCell.setCellValue("ESI MAPPED LEVEL");
				Cell esiCategoryMappedRefKey = headerRow.createCell(21);
				esiCategoryMappedRefKey.setCellValue("ESI MAPPED REFKEY");
				
				Cell vinMappedCell = headerRow.createCell(22);
				vinMappedCell.setCellValue("VIN MAPPED");
				
				Cell titleCell = headerRow.createCell(23);
				titleCell.setCellValue("TITLE");
				
				Cell innerLinkFoundCell = headerRow.createCell(24);
				innerLinkFoundCell.setCellValue("INNER LINK FOUND");
				Cell allInnerLinksMappedCell = headerRow.createCell(25);
				allInnerLinksMappedCell.setCellValue("ALL INNER LINKS MAPPED");
				Cell innerLinkNotMappedReasonCell = headerRow.createCell(26);
				innerLinkNotMappedReasonCell.setCellValue("INNER LINK MAPPING FAILURE REASON");
				Cell operationTypeCell = headerRow.createCell(27);
				operationTypeCell.setCellValue("OPERATION TYPE");
				int rowCount = 0;
				// ADD INFO
				
				
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
						if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
						{
							cell4.setCellValue(contentDetails.getModel());
						}
						
						if(null!=contentDetails.getModelFolderName() && !"".equals(contentDetails.getModelFolderName()))
						{
							cell5.setCellValue(contentDetails.getModelFolderName());
						}
						if(null!=contentDetails.getManualType() && !"".equals(contentDetails.getManualType()))
						{
							cell6.setCellValue(contentDetails.getManualType());
						}
						if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
						{
							cell7.setCellValue(contentDetails.getMaterialName());
						}
						if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
						{
							cell8.setCellValue(contentDetails.getChannelName());
						}
						if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
						{
							cell9.setCellValue(contentDetails.getImDocumentId());
						}
						if(null!=contentDetails.getImVersion() && !"".equals(contentDetails.getImVersion()))
						{
							cell10.setCellValue(contentDetails.getImVersion());
						}
						if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
						{
							cell11.setCellValue(contentDetails.getFileAbsolutePath());
						}
						if(null!=contentDetails.getFileName() && !"".equals(contentDetails.getFileName()))
						{
							cell12.setCellValue(contentDetails.getFileName());
						}
						if(null!=contentDetails.getProcessingOperationStatus() && !"".equals(contentDetails.getProcessingOperationStatus()))
						{
							cell13.setCellValue(contentDetails.getProcessingOperationStatus());
							if(contentDetails.getProcessingOperationStatus().trim().toLowerCase().equals("success"))
							{
								// increment successCount
								successCount++;
							}
						}
						
						if(null!=contentDetails.getErrorComments() && !"".equals(contentDetails.getErrorComments()))
						{
							cell16.setCellValue(contentDetails.getErrorComments());
						}
						if(null!=contentDetails.getProcessingTime())
						{
							String time = sdf.format(contentDetails.getProcessingTime());
							cell17.setCellValue(time);
							time=null;
						}
						
						if(null!=contentDetails.getEsiCategoryMapped() && !"".equals(contentDetails.getEsiCategoryMapped()))
						{
							cell18.setCellValue(contentDetails.getEsiCategoryMapped());
						}
						if(null!=contentDetails.getMappedTaxonomy() && !"".equals(contentDetails.getMappedTaxonomy()))
						{
							cell19.setCellValue(contentDetails.getMappedTaxonomy());
						}
						
						if(null!=contentDetails.getEsiCategoryMappedLevel() && !"".equals(contentDetails.getEsiCategoryMappedLevel()))
						{
							cell20.setCellValue(contentDetails.getEsiCategoryMappedLevel());
						}
						
						if(null!=contentDetails.getEsiCategoryMappedRefKey() && !"".equals(contentDetails.getEsiCategoryMappedRefKey()))
						{
							cell21.setCellValue(contentDetails.getEsiCategoryMappedRefKey());
						}
						
						if(null!=contentDetails.getVinMapped() && !"".equals(contentDetails.getVinMapped()))
						{
							cell22.setCellValue(contentDetails.getVinMapped());
						}
						
						if(null!=contentDetails.getTitle() && !"".equals(contentDetails.getTitle()))
						{
							cell23.setCellValue(contentDetails.getTitle());
						}
						
						if(null!=contentDetails.getInnerLinkFound() && !"".equals(contentDetails.getInnerLinkFound()))
						{
							cell24.setCellValue(contentDetails.getInnerLinkFound());
						}
						if(null!=contentDetails.getAllInnerLinksMapped() && !"".equals(contentDetails.getAllInnerLinksMapped()))
						{
							cell25.setCellValue(contentDetails.getAllInnerLinksMapped());
						}
						if(null!=contentDetails.getInnerLinkMappingReason() && !"".equals(contentDetails.getInnerLinkMappingReason()))
						{
							cell26.setCellValue(contentDetails.getInnerLinkMappingReason());
						}
						if(null!=contentDetails.getOperationType() && !"".equals(contentDetails.getOperationType()))
						{
							cell27.setCellValue(contentDetails.getOperationType());
						}
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
						if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
						{
							cell4.setCellValue(contentDetails.getModel());
						}
						
						if(null!=contentDetails.getModelFolderName() && !"".equals(contentDetails.getModelFolderName()))
						{
							cell5.setCellValue(contentDetails.getModelFolderName());
						}
						if(null!=contentDetails.getManualType() && !"".equals(contentDetails.getManualType()))
						{
							cell6.setCellValue(contentDetails.getManualType());
						}
						if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
						{
							cell7.setCellValue(contentDetails.getMaterialName());
						}
						if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
						{
							cell8.setCellValue(contentDetails.getChannelName());
						}
						if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
						{
							cell9.setCellValue(contentDetails.getImDocumentId());
						}
						if(null!=contentDetails.getImVersion() && !"".equals(contentDetails.getImVersion()))
						{
							cell10.setCellValue(contentDetails.getImVersion());
						}
						if(null!=dataMap.get("SOURCE_PATH") && !"".equals(dataMap.get("SOURCE_PATH")))
						{
							cell11.setCellValue(String.valueOf(dataMap.get("SOURCE_PATH")));
						}
						if(null!=dataMap.get("FILE_NAME") && !"".equals(dataMap.get("FILE_NAME")))
						{
							cell12.setCellValue(String.valueOf(dataMap.get("FILE_NAME")));
						}
						
						if(null!=dataMap.get("PROCESSING_STATUS") && !"".equals(dataMap.get("PROCESSING_STATUS")))
						{
							cell14.setCellValue(String.valueOf(dataMap.get("PROCESSING_STATUS")));
							if(String.valueOf(dataMap.get("PROCESSING_STATUS")).trim().toLowerCase().equals("success"))
							{
								// increment successCount
								successCount++;
							}
						}
					
						if(null!=dataMap.get("DESTINATION_PATH") && !"".equals(dataMap.get("DESTINATION_PATH")))
						{
							cell15.setCellValue(String.valueOf(dataMap.get("DESTINATION_PATH")));
						}
						
						if(null!=dataMap.get("ERROR_COMMENTS") && !"".equals(dataMap.get("ERROR_COMMENTS")))
						{
							cell16.setCellValue(String.valueOf(dataMap.get("ERROR_COMMENTS")));
						}
						
						if(null!=dataMap.get("DATE_TIME") && !"".equals(dataMap.get("DATE_TIME")))
						{
							cell17.setCellValue(String.valueOf(dataMap.get("DATE_TIME")));
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
				processingStatusCell=null;
				esiCategoryMappedCell=null;
				esiCategoryMappedLevelCell=null;
				esiCategoryMappedRefKey=null;
				esiCategoryTaxonomyCell=null;
				vinMappedCell=null;
						
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
				
				// calculate failure Count
				if(totalCount>0)
				{
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
				Cell innerLinkVTOCNameCell = headerRow.createCell(7);
				innerLinkVTOCNameCell.setCellValue("INNER LINK VTOC NAME");
				Cell innerLinkDocumentIdCell = headerRow.createCell(8);
				innerLinkDocumentIdCell.setCellValue("INNER LINK DOC ID");
				Cell innerLinkMappingStatusCell = headerRow.createCell(9);
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
								Cell cell9 = row.createCell(9);

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
								if(null!=linkDetails.getVtocFileName() && !"".equals(linkDetails.getVtocFileName()))
								{
									cell7.setCellValue(linkDetails.getVtocFileName());
								}
								if (null != linkDetails.getInnnerLinkDocumentId() && !"".equals(linkDetails.getInnnerLinkDocumentId())) 
								{
									cell8.setCellValue(linkDetails.getInnnerLinkDocumentId());
								}
								if (null != linkDetails.getMapStatus() 	&& !"".equals(linkDetails.getMapStatus())) 
								{
									cell9.setCellValue(linkDetails.getMapStatus());
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
								cell9 = null;

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
				innerLinkVTOCNameCell = null;

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
				myWorkBook = new SXSSFWorkbook(100);

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
	/** The display order columns of the MNAO view content reports (the MC view content report labels), after the legacy ones. */
	/**
	 * The display order columns of the MNAO view content reports (the MC view content report labels), after the legacy
	 * ones. The level CODES are not reported - they are no longer supplied (the database keeps its columns).
	 */
	private static final String[] DISPLAY_ORDER_HEADERS = { "DISP ORD NAME LEVEL 1", "DISP ORD NAME LEVEL 2", "DISP ORD NAME LEVEL 3",
			"DISP ORD NAME LEVEL 4", "DISP ORD NAME LEVEL 5", "DISP ORD NAME LEVEL 6", "SEQUENCE NO" };

	private static void addDisplayOrderHeaders(Row headerRow, int from)
	{
		for(int i=0; i<DISPLAY_ORDER_HEADERS.length; i++)
		{
			headerRow.createCell(from+i).setCellValue(DISPLAY_ORDER_HEADERS[i]);
		}
	}

	/** The display order row the view content row was written for (the values in VC_*_DISP_* / VC_*_DISPORD_SEQ_NO). */
	private static void addDisplayOrderCells(Row row, int from, com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d)
	{
		String[] values = null==d ? new String[DISPLAY_ORDER_HEADERS.length] : new String[] {
				d.getDisplayOrderLevel1Name(), d.getDisplayOrderLevel2Name(), d.getDisplayOrderLevel3Name(),
				d.getDisplayOrderLevel4Name(), d.getDisplayOrderLevel5Name(), d.getDisplayOrderLevel6Name(),
				d.getSequenceNo() };
		for(int i=0; i<values.length; i++)
		{
			row.createCell(from+i).setCellValue(null==values[i] ? "" : values[i]);
		}
	}

	public void printViewContentModelYearDetailsReport(String scheduleCode , ArrayList<MNAOModelYearViewContentDetails> viewContentList) {
		if (logger.isInfoEnabled())
			logger.info("printViewContentModelYearDetailsReport :: Method Starts.");
		try {
			if (null != viewContentList && viewContentList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					long totalCount=0;
					long successCount = 0;
					long failureCount= 0;
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
					
					ArrayList<List<MNAOModelYearViewContentDetails>> partitions = new ArrayList<List<MNAOModelYearViewContentDetails>>();
					for (int i=0; i<viewContentList.size(); i += partitionSize) {
						partitions.add(viewContentList.subList(i, Math.min(i + partitionSize, viewContentList.size())));
					}
					
					if(null!=partitions && partitions.size()>0)
					{
						int listCount=0;
						for(List<MNAOModelYearViewContentDetails> subsetList : partitions)
						{
							if(null!=subsetList && subsetList.size()>0)
							{
								/*
								 * iterate Map and write the Excel File for each Index
								 */
								String fName = "/";
								if(listCount==0)
								{
									fName+="VIEW_CONTENT_MODEL_YEAR_REPORT.xlsx";
								}
								else 
								{
									fName+="VIEW_CONTENT_MODEL_YEAR_REPORT_"+listCount+".xlsx";
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
								
								Cell catTypeCell = headerRow.createCell(3);
								catTypeCell.setCellValue("MODEL");
								
								Cell dispOrdCodeLevel1 = headerRow.createCell(4);
								dispOrdCodeLevel1.setCellValue("YEAR");
								
								Cell dispOrdCodeLevel4 = headerRow.createCell(5);
								dispOrdCodeLevel4.setCellValue("DOCUMENT TYPE");
								
								Cell manualTypeLabel = headerRow.createCell(6);
								manualTypeLabel.setCellValue("DOCUMENT TYPE LABEL");
								
								Cell dispOrdNameLevel4 = headerRow.createCell(7);
								dispOrdNameLevel4.setCellValue("ESI CAT LEVEL 1 CODE");
								
								Cell dispOrdCodeLevel5 = headerRow.createCell(8);
								dispOrdCodeLevel5.setCellValue("ESI CAT LEVEL 1 NAME");
								
								Cell dispOrdNameLevel5 = headerRow.createCell(9);
								dispOrdNameLevel5.setCellValue("ESI CAT LEVEL 2 CODE");
								
								Cell dispOrdCodeLevel6 = headerRow.createCell(10);
								dispOrdCodeLevel6.setCellValue("ESI CAT LEVEL 2 NAME");
								
								Cell dispOrdNameLevel6 = headerRow.createCell(11);
								dispOrdNameLevel6.setCellValue("ESI CAT LEVEL 3 CODE");
								
								Cell seqNoCell = headerRow.createCell(12);
								seqNoCell.setCellValue("ESI CAT LEVEL 3 NAME");
								
								Cell manualTypeCell = headerRow.createCell(13);
								manualTypeCell.setCellValue("TITLE");
								
								Cell errorCodeCellb = headerRow.createCell(14);
								errorCodeCellb.setCellValue("DOC CREATED DATE");
								
								Cell errorMessageCellc = headerRow.createCell(15);
								errorMessageCellc.setCellValue("DOC MODIFIED DATE");
								
								Cell errorCodeCelld = headerRow.createCell(16);
								errorCodeCelld.setCellValue("STATUS");
								
								Cell errorCodesCell = headerRow.createCell(17);
								errorCodesCell.setCellValue("ERROR CODES");
								
								Cell errorMessagesCell = headerRow.createCell(18);
								errorMessagesCell.setCellValue("ERROR MESSAGES");
								addDisplayOrderHeaders(headerRow, 19);
								int rowCount = 0;
								for (int a = 0; a < subsetList.size(); a++) {
									MNAOModelYearViewContentDetails details = (MNAOModelYearViewContentDetails) subsetList.get(a);

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
									
									if(null!=details.getModel() && !"".equals(details.getModel()))
									{
										cell3.setCellValue(details.getModel().trim().toUpperCase());
									}
									
									if(null!=details.getYear() && !"".equals(details.getYear()))
									{
										cell4.setCellValue(details.getYear());
									}
									
									if(null!=details.getManualType() && !"".equals(details.getManualType()))
									{
										cell5.setCellValue(details.getManualType());
									}
									
									if(null!=details.getManualTypeLabel() && !"".equals(details.getManualTypeLabel()))
									{
										cell6.setCellValue(details.getManualTypeLabel());
									}
									
									if(null!=details.getEsiCatLevel1Code() && !"".equals(details.getEsiCatLevel1Code()))
									{
										cell7.setCellValue(details.getEsiCatLevel1Code());
									}
									
									if(null!=details.getEsiCatLevel1Name() && !"".equals(details.getEsiCatLevel1Name()))
									{
										cell8.setCellValue(details.getEsiCatLevel1Name());
									}
									
									if(null!=details.getEsiCatLevel2Code() && !"".equals(details.getEsiCatLevel2Code()))
									{
										cell9.setCellValue(details.getEsiCatLevel2Code());
									}
									
									if(null!=details.getEsiCatLevel2Name() && !"".equals(details.getEsiCatLevel2Name()))
									{
										cell10.setCellValue(details.getEsiCatLevel2Name());
									}
									
									if(null!=details.getEsiCatLevel3Code() && !"".equals(details.getEsiCatLevel3Code()))
									{
										cell11.setCellValue(details.getEsiCatLevel3Code());
									}
									
									if(null!=details.getEsiCatLevel3Name() && !"".equals(details.getEsiCatLevel3Name()))
									{
										cell12.setCellValue(details.getEsiCatLevel3Name());
									}
									
									if(null!=details.getTitle() && !"".equals(details.getTitle()))
									{
										cell13.setCellValue(details.getTitle());
									}
									
									if(null!=details.getImDocCreateDate())
									{
										cell14.setCellValue(sdf.format(details.getImDocCreateDate()));
									}
									
									if(null!=details.getImDocModifiedDate())
									{
										cell15.setCellValue(sdf.format(details.getImDocModifiedDate()));
									}
									
									if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
									{
										cell16.setCellValue(details.getProcessingStatus());
										if(details.getProcessingStatus().trim().toLowerCase().equals("success"))
										{
											// increment successCount
											successCount++;
										}
									}
									
									if(null!=details.getErrorCodes() && !"".equals(details.getErrorCodes()))
									{
										cell17.setCellValue(details.getErrorCodes());
									}
									
									if(null!=details.getErrorMessages() && !"".equals(details.getErrorMessages()))
									{
										cell18.setCellValue(details.getErrorMessages());
									}
									
									addDisplayOrderCells(row, 19, details.getDisplayOrder());

									cell18 = null;
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
								dispOrdFileNameCell = null;
								catTypeCell = null;
								dispOrdCodeLevel1 =null;
								dispOrdCodeLevel4 =null;
								dispOrdCodeLevel5 =null;
								dispOrdCodeLevel6 =null;
								dispOrdNameLevel4 =null;
								dispOrdNameLevel5 =null;
								dispOrdNameLevel6 =null;
								seqNoCell=  null;
								manualTypeCell= null;
								errorCodeCellb =null;
								errorCodesCell = null;
								errorMessagesCell = null;
								errorCodeCellb = null;
								errorCodeCelld = null;
								errorMessageCellc = null;
								
								FileOutputStream os = PathUtil.fileOutputStream(myFile);
								myWorkBook.write(os);
								logger.info("Writing on VIEW CONTENT MODEL YEAR DETAILS REPORT XLSX file Finished ...");
								os.flush();
								os.close();

								// set mySheet to null
								mySheet = null;
								// set myWorkBook to null
								myWorkBook = null;
								// set myFile to null
								myFile = null;
								
							}
							subsetList =null;
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
						rsd.setReportName(ReportsSummaryDetails.REPORT_VIEWCONTENT_MODEL_YEAR);
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
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentModelYearDetailsReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					successCount= 0;
					failureCount = 0;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentModelYearDetailsReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printViewContentModelYearDetailsReport :: Method Ends.");
	}
	@SuppressWarnings("resource")
	public void printViewContentVINDetailsReport(String scheduleCode , ArrayList<MNAOModelYearViewContentDetails> viewContentList) {
		if (logger.isInfoEnabled())
			logger.info("printViewContentVINDetailsReport :: Method Starts.");
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

					
					// SET TOTAL COUNT
					totalCount= viewContentList.size();
					
					ArrayList<List<MNAOModelYearViewContentDetails>> partitions = new ArrayList<List<MNAOModelYearViewContentDetails>>();
					for (int i=0; i<viewContentList.size(); i += partitionSize) {
						partitions.add(viewContentList.subList(i, Math.min(i + partitionSize, viewContentList.size())));
					}
					
					int listCount=0;
					if(null!=partitions && partitions.size()>0)
					{
						for(List<MNAOModelYearViewContentDetails> subsetList: partitions)
						{
							if(null!=subsetList && subsetList.size()>0)
							{
								/*
								 * iterate Map and write the Excel File for each Index
								 */
								String fName = "/";
								if(listCount ==0 )
								{
									fName+="VIEW_CONTENT_VIN_REPORT.xlsx";
								}
								else
								{
									fName+="VIEW_CONTENT_VIN_REPORT_"+listCount+".xlsx";
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
								
								Cell modelCodeCell = headerRow.createCell(3);
								modelCodeCell.setCellValue("MODEL");
								
								Cell yearCodeCell = headerRow.createCell(4);
								yearCodeCell.setCellValue("YEAR");
								
								Cell dispOrdCodeLevel2 = headerRow.createCell(5);
								dispOrdCodeLevel2.setCellValue("WMI CODE");
								
								Cell dispOrdNameLevel2 = headerRow.createCell(6);
								dispOrdNameLevel2.setCellValue("VDS CODE");
								
								Cell dispOrdCodeLevel3 = headerRow.createCell(7);
								dispOrdCodeLevel3.setCellValue("VIS START RANGE");
								
								Cell dispOrdNameLevel3 = headerRow.createCell(8);
								dispOrdNameLevel3.setCellValue("VIS END RANGE");
								
								Cell dispOrdCodeLevel4 = headerRow.createCell(9);
								dispOrdCodeLevel4.setCellValue("DOCUMENT TYPE");
								
								Cell manualTypeLabel = headerRow.createCell(10);
								manualTypeLabel.setCellValue("DOCUMENT TYPE LABEL");
								
								Cell dispOrdNameLevel4 = headerRow.createCell(11);
								dispOrdNameLevel4.setCellValue("ESI CAT LEVEL 1 CODE");
								
								Cell dispOrdCodeLevel5 = headerRow.createCell(12);
								dispOrdCodeLevel5.setCellValue("ESI CAT LEVEL 1 NAME");
								
								Cell dispOrdNameLevel5 = headerRow.createCell(13);
								dispOrdNameLevel5.setCellValue("ESI CAT LEVEL 2 CODE");
								
								Cell dispOrdCodeLevel6 = headerRow.createCell(14);
								dispOrdCodeLevel6.setCellValue("ESI CAT LEVEL 2 NAME");
								
								Cell dispOrdNameLevel6 = headerRow.createCell(15);
								dispOrdNameLevel6.setCellValue("ESI CAT LEVEL 3 CODE");
								
								Cell seqNoCell = headerRow.createCell(16);
								seqNoCell.setCellValue("ESI CAT LEVEL 3 NAME");
								
								Cell manualTypeCell = headerRow.createCell(17);
								manualTypeCell.setCellValue("TITLE");
								
								Cell errorCodeCellb = headerRow.createCell(18);
								errorCodeCellb.setCellValue("DOC CREATED DATE");
								
								Cell errorMessageCellc = headerRow.createCell(19);
								errorMessageCellc.setCellValue("DOC MODIFIED DATE");
								
								Cell errorCodeCelld = headerRow.createCell(20);
								errorCodeCelld.setCellValue("STATUS");
								
								Cell processingStatusCell = headerRow.createCell(21);
								processingStatusCell.setCellValue("ERROR CODES");
								
								Cell errorMessagesCall = headerRow.createCell(22);
								errorMessagesCall.setCellValue("ERROR MESSAGES");
								addDisplayOrderHeaders(headerRow, 23);
								
								
								int rowCount = 0;
								for (int a = 0; a < subsetList.size(); a++) {
									MNAOModelYearViewContentDetails details = (MNAOModelYearViewContentDetails) subsetList.get(a);

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
									
									if(null!=details.getModel() && !"".equals(details.getModel()))
									{
										cell3.setCellValue(details.getModel().trim().toUpperCase());
									}
									
									if(null!=details.getYear() && !"".equals(details.getYear()))
									{
										cell4.setCellValue(details.getYear());
									}
									
									if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
									{
										cell5.setCellValue(details.getWmiCode());
									}
									
									if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
									{
										cell6.setCellValue(details.getVdsCode());
									}
									
									if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
									{
										cell7.setCellValue(details.getVisStartRange());
									}
									
									if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
									{
										cell8.setCellValue(details.getVisEndRange());
									}
									
									if(null!=details.getManualType() && !"".equals(details.getManualType()))
									{
										cell9.setCellValue(details.getManualType());
									}
									
									if(null!=details.getManualTypeLabel() && !"".equals(details.getManualTypeLabel()))
									{
										cell10.setCellValue(details.getManualTypeLabel());
									}
									
									if(null!=details.getEsiCatLevel1Code() && !"".equals(details.getEsiCatLevel1Code()))
									{
										cell11.setCellValue(details.getEsiCatLevel1Code());
									}
									
									if(null!=details.getEsiCatLevel1Name() && !"".equals(details.getEsiCatLevel1Name()))
									{
										cell12.setCellValue(details.getEsiCatLevel1Name());
									}
									
									if(null!=details.getEsiCatLevel2Code() && !"".equals(details.getEsiCatLevel2Code()))
									{
										cell13.setCellValue(details.getEsiCatLevel2Code());
									}
									
									if(null!=details.getEsiCatLevel2Name() && !"".equals(details.getEsiCatLevel2Name()))
									{
										cell14.setCellValue(details.getEsiCatLevel2Name());
									}
									
									if(null!=details.getEsiCatLevel3Code() && !"".equals(details.getEsiCatLevel3Code()))
									{
										cell15.setCellValue(details.getEsiCatLevel3Code());
									}
									
									if(null!=details.getEsiCatLevel3Name() && !"".equals(details.getEsiCatLevel3Name()))
									{
										cell16.setCellValue(details.getEsiCatLevel3Name());
									}
									
									if(null!=details.getTitle() && !"".equals(details.getTitle()))
									{
										cell17.setCellValue(details.getTitle());
									}
									
									if(null!=details.getImDocCreateDate())
									{
										cell18.setCellValue(sdf.format(details.getImDocCreateDate()));
									}
									
									if(null!=details.getImDocModifiedDate())
									{
										cell19.setCellValue(sdf.format(details.getImDocModifiedDate()));
									}
									
									if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
									{
										cell20.setCellValue(details.getProcessingStatus());
										if(details.getProcessingStatus().trim().toLowerCase().equals("success"))
										{
											// increment successCount
											successCount++;
										}
									}
									
									if(null!=details.getErrorCodes() && !"".equals(details.getErrorCodes()))
									{
										cell21.setCellValue(details.getErrorCodes());
									}
									
									if(null!=details.getErrorMessages() && !"".equals(details.getErrorMessages()))
									{
										cell22.setCellValue(details.getErrorMessages());
									}
									
									addDisplayOrderCells(row, 23, details.getDisplayOrder());

									cell22 = null;
									
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
								modelCodeCell = null;
								yearCodeCell = null;
								documentIdCell = null;
								dispOrdFileNameCell = null;
								dispOrdCodeLevel2 =null;
								dispOrdCodeLevel3 =null;
								dispOrdCodeLevel4 =null;
								dispOrdCodeLevel5 =null;
								dispOrdCodeLevel6 =null;
								dispOrdNameLevel2 =null;
								dispOrdNameLevel3 =null;
								dispOrdNameLevel4 =null;
								dispOrdNameLevel5 =null;
								dispOrdNameLevel6 =null;
								seqNoCell=  null;
								manualTypeCell= null;
								processingStatusCell= null;
								errorCodeCellb = null;
								errorCodeCelld = null;
								errorMessageCellc = null;
								
								FileOutputStream os = PathUtil.fileOutputStream(myFile);
								myWorkBook.write(os);
								logger.info("Writing on VIEW CONTENT VIN DETAILS REPORT XLSX file Finished ...");
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
					partitions=  null;
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
						rsd.setReportName(ReportsSummaryDetails.REPORT_VIEWCONTENT_VIN);
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
							Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentVINDetailsReport()", e);
						}
						rsd = null;
					}
					totalCount = 0;
					successCount= 0;
					failureCount = 0;
				}
			}
		
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printViewContentVINDetailsReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printViewContentVINDetailsReport :: Method Ends.");
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
					String headers="FILE PATH,FILE NAME,LOCALE,MODEL FOLDER NAME,MANUALTYPE,MATERIAL FOLDER,"
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
						dataString+=con.getManualType()+"<TOK_SPERATOR>";
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
					String headers="FILE PATH,FILE NAME,LOCALE,MODEL FOLDER NAME,MANUALTYPE,MATERIAL FOLDER,"
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
						dataString+=con.getManualType()+"<TOK_SPERATOR>";
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

	
}