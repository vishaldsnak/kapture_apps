package com.mazda.gms3.dmt.metadataval.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;

public class PrintReportsUtil {

	private Logger logger = LogManager.getLogger(PrintReportsUtil.class);
	
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

	public void printMCMMEVinReport(String scheduleCode , ArrayList<VinDetails> vinList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printMCMMEVinReport :: Method Starts.");
		try {
			if (null != vinList && vinList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File reportDirectory = PathUtil.file(path);
					if (!reportDirectory.exists() || !reportDirectory.isDirectory()) {
						// create new directory
						reportDirectory.mkdir();
					}
					reportDirectory = null;
					
					if(!path.endsWith("/"))
					{
						path = path+"/";
					}
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path  + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+".xlsx";
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
					Cell fileCell = headerRow.createCell(0);
					fileCell.setCellValue("SCHEDULE NAME");
					Cell fileNameCell = headerRow.createCell(1);
					fileNameCell.setCellValue("FILE NAME");
					Cell imDocumentIdCell = headerRow.createCell(2);
					imDocumentIdCell.setCellValue("FILE PATH");
					Cell operationTypeCell = headerRow.createCell(3);
					operationTypeCell.setCellValue("PROCESSING STATUS");
					Cell localeCell = headerRow.createCell(4);
					localeCell.setCellValue("LOCALE");
					Cell catNameCell = headerRow.createCell(5);
					catNameCell.setCellValue("MANUAL TYPE");
					Cell catRefKeyCell = headerRow.createCell(6);
					catRefKeyCell.setCellValue("FACELIFT FOLDER");
					Cell catTypeCell = headerRow.createCell(7);
					catTypeCell.setCellValue("MATERIAL FOLDER");
					Cell carlineCodeCell = headerRow.createCell(8);
					carlineCodeCell.setCellValue("CARLINE CODE");
					Cell wmiCodeCell = headerRow.createCell(9);
					wmiCodeCell.setCellValue("WMI CODE");
					Cell vdsCodeCell = headerRow.createCell(10);
					vdsCodeCell.setCellValue("VDS CODE");
					Cell visStartCell = headerRow.createCell(11);
					visStartCell.setCellValue("VIS START RANGE");
					Cell visEndCell =headerRow.createCell(12);
					visEndCell.setCellValue("VIS END RANGE");
					Cell vinFoundInMDMCell=headerRow.createCell(13);
					vinFoundInMDMCell.setCellValue("VIN EXISTS IN MDM");
					Cell vinRefKeyCell = headerRow.createCell(14);
					vinRefKeyCell.setCellValue("VIN REF KEY");
					Cell vinFoundInIMCell = headerRow.createCell(15);
					vinFoundInIMCell.setCellValue("VIN EXISTS IN KAPTURE");
					Cell displayOrderCell = headerRow.createCell(16);
					displayOrderCell.setCellValue("DISPLAY ORDER FILE");
					Cell errorMessageCell = headerRow.createCell(17);
					errorMessageCell.setCellValue("ERROR MESSAGE");

					int rowCount = 0;
					for (int a = 0; a < vinList.size(); a++) {
						VinDetails details = (VinDetails) vinList.get(a);
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						cell0.setCellValue(ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode);

						Cell cell1 = row.createCell(1);
						cell1.setCellValue("");
						if(null!=details.getVinSourceFileName() && !"".equals(details.getVinSourceFileName()))
						{
							cell1.setCellValue(details.getVinSourceFileName());
						}
						
						Cell cell2 = row.createCell(2);
						cell2.setCellValue("");
						if(null!=details.getVinSorceFilePath() && !"".equals(details.getVinSorceFilePath()))
						{
							cell2.setCellValue(details.getVinSorceFilePath());
						}
						
						Cell cell3 = row.createCell(3);
						cell3.setCellValue("");
						if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
						{
							cell3.setCellValue(details.getProcessingStatus());
						}
						
						Cell cell4 = row.createCell(4);
						cell4.setCellValue("");
						if (null != locale && !"".equals(locale)) 
						{
							cell4.setCellValue(locale);
						}
						
						Cell cell5 = row.createCell(5);
						cell5.setCellValue("");
						if (null != details.getManualType() && !"".equals(details.getManualType())) 
						{
							cell5.setCellValue(details.getManualType());
						}

						Cell cell6 = row.createCell(6);
						cell6.setCellValue("");
						if (null != details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder())) 
						{
							cell6.setCellValue(details.getFaceLiftFolder());
						}
						
						Cell cell7 = row.createCell(7);
						cell7.setCellValue("");
						if (null != details.getMaterialFolder() && !"".equals(details.getMaterialFolder())) 
						{
							cell7.setCellValue(details.getMaterialFolder());
						}

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
						
						if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
						{
							cell8.setCellValue(details.getCarlineCode());
						}
						
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							cell9.setCellValue(details.getWmiCode());
						}
						
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
						{
							cell10.setCellValue(details.getVdsCode());
						}
						
						if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
						{
							cell11.setCellValue(details.getVisStartRange());
						}
						
						if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
						{
							cell12.setCellValue(details.getVisEndRange());
						}
						
						if(null!=details.getVinFoundInMDM() && !"".equals(details.getVinFoundInMDM()))
						{
							cell13.setCellValue(details.getVinFoundInMDM());
						}
						
						if(null!=details.getVinRefKey() && !"".equals(details.getVinRefKey()))
						{
							cell14.setCellValue(details.getVinRefKey());
						}
						
						if(null!=details.getVinFoundInIM() && !"".equals(details.getVinFoundInIM()))
						{
							cell15.setCellValue(details.getVinFoundInIM());
						}
						
						if(null!=details.getDisplayOrderTextFileName() && !"".equals(details.getDisplayOrderTextFileName()))
						{
							cell16.setCellValue(details.getDisplayOrderTextFileName());
						}
						
						if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
						{
							cell17.setCellValue(details.getErrorMessage());
						}
						
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
						details = null;
					}

					headerRow = null;
					catNameCell = null;
					catRefKeyCell = null;
					fileCell = null;
					fileNameCell = null;
					imDocumentIdCell= null;
					catTypeCell=  null;
					operationTypeCell=null;
					localeCell=null;
					carlineCodeCell=null;
					wmiCodeCell=null;
					vdsCodeCell=null;
					visStartCell=null;
					visEndCell=null;
					vinFoundInIMCell=null;
					vinFoundInMDMCell=null;
					vinRefKeyCell=null;
					displayOrderCell=null;
					errorMessageCell=null;
					
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("VIN METADATA FAILURE REPORT XLSX file Finished ...");
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
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMCMMEVinReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printMCMMEVinReport :: Method Ends.");
	}

	public void printDisplayOrderReport(String scheduleCode , ArrayList<DisplayOrderDetails> displayOrderList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderReport :: Method Starts.");
		try {
			if (null != displayOrderList && displayOrderList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File reportDirectory = PathUtil.file(path);
					if (!reportDirectory.exists() || !reportDirectory.isDirectory()) {
						// create new directory
						reportDirectory.mkdir();
					}
					reportDirectory = null;
					
					if(!path.endsWith("/"))
					{
						path = path+"/";
					}
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path  + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+".xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					String headers="SCHEDULE NAME,FILE NAME,FILE PATH,PROCESSING STATUS,LOCALE,MODEL FOLDER,MANUAL TYPE FOLDER,FACELIFT FOLDER,MATERIAL FOLDER,PROCESSING FOLDER,"
							+ "CONTENT FILE NAME,TITLE,DISP ORD NAME 1,DISP ORD NAME 2,DISP ORD NAME 3,"
							+ "DISP ORD NAME 4,DISP ORD NAME 5,DISP ORD NAME 6,SEQUENCE NO,ERROR MESSAGE,"
							+ "ENGINE TYPE,TRANSMISSION TYPE,"
							+ "DRIVEAXLE TYPE,BODY TYPE,MDM-FAILED ENGINE TYPE,KAPTURE-FAILED ENGINE TYPE,MDM-FAILED TRANSMISSION TYPE,KAPTURE-FAILED TRANSMISSION TYPE,"
							+ "MDM-FAILED DRIVEAXLE TYPE,KAPTURE-FAILED DRIVEAXLE TYPE,MDM-FAILED BODY TYPE,KAPTURE-FAILED BODY TYPE";
					String[] headerTokens=headers.split(",");
					Row headerRow = mySheet.createRow(0);
					Cell headerCell=null;
					for(int a=0;a<headerTokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(headerTokens[a].toString().trim());
					}
					headerCell=null;
					headers=null;
					headerTokens=null;
					
					int rowCount = 0;
					String[] dispToken=null;
					String dispData=null;
					Cell dataCell=null;
					Row row=null;
					DisplayOrderDetails details=null;
					for (int a = 0; a < displayOrderList.size(); a++) {
						details = (DisplayOrderDetails) displayOrderList.get(a);
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getDisplayOrderSourceFileName()+
								"<TOK_SEPARATOR>"+details.getDisplayOrderSourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>"+details.getModelFolderName()+"<TOK_SEPARATOR>"+
								details.getManualType()+"<TOK_SEPARATOR>";
						dispData+=details.getFaceLiftFolderName()+"<TOK_SEPARATOR>"+details.getMaterialFolderName()+"<TOK_SEPARATOR>"+details.getProcessingFolderName()+
								"<TOK_SEPARATOR>"+details.getFileName()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel4Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel5Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel6Name()+"<TOK_SEPARATOR>";
						dispData+=details.getSequenceNo()+"<TOK_SEPARATOR>"+details.getErrorMessage()+"<TOK_SEPARATOR>";
						dispData+=details.getEngineType()+"<TOK_SEPARATOR>"+details.getMissionType()+"<TOK_SEPARATOR>";
						dispData+=details.getDriveAxleType()+"<TOK_SEPARATOR>"+details.getBodyType()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedEngineTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedEngineTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedMissionTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedMissionTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedDriveAxleTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedDriveAxleTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedBodyTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedBodyTypeCodesIM()+"<TOK_SEPARATOR>";
						
						dispToken = dispData.split("<TOK_SEPARATOR>");
						if(null!=dispToken && dispToken.length>0)
						{
							for(int r=0;r<dispToken.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=dispToken[r] && !"".equals(dispToken[r]) && !"null".equals(dispToken[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(dispToken[r].trim());
								}
								dataCell=null;
							}
						}
						dataCell=null;
						row = null;
						details = null;
						dispData=null;
						dispToken=null;
					}
					dataCell=null;
					row = null;
					details = null;
					dispData=null;
					dispToken=null;
					headerRow = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("DISPLAY ORDER METADATA FAILURE REPORT XLSX file Finished ...");
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
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printDisplayOrderReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderReport :: Method Ends.");
	}

	public void printCDProcessingReport(String scheduleCode , ArrayList<CDProcessingDetails> cdProcessingList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printCDProcessingReport :: Method Starts.");
		try {
			if (null != cdProcessingList && cdProcessingList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File reportDirectory = PathUtil.file(path);
					if (!reportDirectory.exists() || !reportDirectory.isDirectory()) {
						// create new directory
						reportDirectory.mkdir();
					}
					reportDirectory = null;
					
					if(!path.endsWith("/"))
					{
						path = path+"/";
					}
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path  + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+".xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					String headers="SCHEDULE NAME,FILE NAME,FILE PATH,PROCESSING STATUS,LOCALE,MODEL FOLDER,MANUAL TYPE FOLDER,FACELIFT FOLDER,MATERIAL FOLDER,PROCESSING FOLDER,"
							+ "CONTENT FILE NAME,TITLE,DISP ORD NAME 1,DISP ORD NAME 2,DISP ORD NAME 3,"
							+ "DISP ORD NAME 4,DISP ORD NAME 5,DISP ORD NAME 6,SEQUENCE NO,ERROR MESSAGE,"
							+ "ENGINE TYPE,TRANSMISSION TYPE,"
							+ "DRIVEAXLE TYPE,BODY TYPE";
					String[] headerTokens=headers.split(",");
					Row headerRow = mySheet.createRow(0);
					Cell headerCell=null;
					for(int a=0;a<headerTokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(headerTokens[a].toString().trim());
					}
					headerCell=null;
					headers=null;
					headerTokens=null;
					
					int rowCount = 0;
					String[] dispToken=null;
					String dispData=null;
					Cell dataCell=null;
					Row row=null;
					CDProcessingDetails details=null;
					for (int a = 0; a < cdProcessingList.size(); a++) {
						details = (CDProcessingDetails) cdProcessingList.get(a);
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getCdProcessingSourceFileName()+
								"<TOK_SEPARATOR>"+details.getCdProcessingSourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>"+details.getModelFolderName()+"<TOK_SEPARATOR>"+
								details.getManualType()+"<TOK_SEPARATOR>";
						dispData+=details.getFaceLiftFolderName()+"<TOK_SEPARATOR>"+details.getMaterialFolderName()+"<TOK_SEPARATOR>"+details.getProcessingFolderName()+
								"<TOK_SEPARATOR>"+details.getFileName()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel4Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel5Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel6Name()+"<TOK_SEPARATOR>";
						dispData+=details.getSequenceNo()+"<TOK_SEPARATOR>"+details.getErrorMessage()+"<TOK_SEPARATOR>";
						dispData+=details.getEngineType()+"<TOK_SEPARATOR>"+details.getMissionType()+"<TOK_SEPARATOR>";
						dispData+=details.getDriveAxleType()+"<TOK_SEPARATOR>"+details.getBodyType();
						
						dispToken = dispData.split("<TOK_SEPARATOR>");
						if(null!=dispToken && dispToken.length>0)
						{
							for(int r=0;r<dispToken.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=dispToken[r] && !"".equals(dispToken[r]) && !"null".equals(dispToken[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(dispToken[r].trim());
								}
								dataCell=null;
							}
						}
						dataCell=null;
						row = null;
						details = null;
						dispData=null;
						dispToken=null;
					}
					dataCell=null;
					row = null;
					details = null;
					dispData=null;
					dispToken=null;
					headerRow = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("CD PROCESSING METADATA FAILURE REPORT XLSX file Finished ...");
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
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printCDProcessingReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printCDProcessingReport :: Method Ends.");
	}
	
	public void printESICategoryReport(String scheduleCode , ArrayList<ESICategoryDetails> esiCategoryList, String locale, String metaDataType) {
		if (logger.isInfoEnabled())
			logger.info("printESICategoryReport :: Method Starts.");
		try {
			if (null != esiCategoryList && esiCategoryList.size() > 0) 
			{
				if ( null != scheduleCode && !"".equals(scheduleCode)) 
				{
					/*
					 * write the string Builder Text to a separate Log file
					 * parallel to the other Logs Generation
					 */
					String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");

					/*
					 * Now check inside the reports directory does a directory
					 * exists for wslId
					 */
					File reportDirectory = PathUtil.file(path);
					if (!reportDirectory.exists() || !reportDirectory.isDirectory()) {
						// create new directory
						reportDirectory.mkdir();
					}
					reportDirectory = null;
					
					if(!path.endsWith("/"))
					{
						path = path+"/";
					}
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					path = path  + scheduleCode;
					File scheduleDirectory = PathUtil.file(path);
					if (!scheduleDirectory.exists()
							|| !scheduleDirectory.isDirectory()) {
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+".xlsx";
					File myFile = PathUtil.file(path + fName);
					fName = null;
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

					// Create a new sheet
					Sheet mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					String headers="";
					headers="SCHEDULE NAME,FILE NAME,FILE PATH,PROCESSING STATUS,LOCALE,FACELIFT FOLDER,MATERIAL FOLDER,PROCESSING FOLDER,"
							+ "CONTENT FILE NAME,TITLE,LEVEL1-CODE,LEVEL1-NAME,LEVEL1-EXISTS_MDM,LEVEL1-REFKEY,LEVEL1-EXISTS_KAPTURE,";
					headers+="LEVEL2-CODE,LEVEL2-NAME,LEVEL2-EXISTS_MDM,LEVEL2-REFKEY,LEVEL2-EXISTS_KAPTURE,";
					headers+="LEVEL3-CODE,LEVEL3-NAME,LEVEL3-EXISTS_MDM,LEVEL3-REFKEY,LEVEL3-EXISTS_KAPTURE,";
					if(metaDataType.startsWith("ESICATEGORY_WD"))
					{
						headers+="STEERING TYPE,KAPTURE-FAILED_STEERING TYPE,";
					}
					headers+="ERROR MESSAGE";
					String[] headerTokens=headers.split(",");
					Row headerRow = mySheet.createRow(0);
					Cell headerCell=null;
					for(int a=0;a<headerTokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(headerTokens[a].toString().trim());
					}
					headerCell=null;
					headers=null;
					headerTokens=null;
					
					int rowCount = 0;
					String[] dispToken=null;
					String dispData=null;
					Cell dataCell=null;
					Row row=null;
					ESICategoryDetails details=null;
					for (int a = 0; a < esiCategoryList.size(); a++) {
						details = (ESICategoryDetails) esiCategoryList.get(a);
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getEsiCategorySourceFileName()+
								"<TOK_SEPARATOR>"+details.getEsiCategorySourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>";
						dispData+=details.getFaceLiftFolderName()+"<TOK_SEPARATOR>"+details.getMaterialFolderName()+"<TOK_SEPARATOR>"+details.getProcessingFolderName()+
								"<TOK_SEPARATOR>"+details.getFileName()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getCategoryLevel1Code()+"<TOK_SEPARATOR>"+details.getCategoryLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode1ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode1RefKey()+"<TOK_SEPARATOR>"+details.getCode1ExistsInIM()+"<TOK_SEPARATOR>";
						dispData+=details.getCategoryLevel2Code()+"<TOK_SEPARATOR>"+details.getCategoryLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode2ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode2RefKey()+"<TOK_SEPARATOR>"+details.getCode2ExistsInIM()+"<TOK_SEPARATOR>";
						dispData+=details.getCategoryLevel3Code()+"<TOK_SEPARATOR>"+details.getCategoryLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode3ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode3RefKey()+"<TOK_SEPARATOR>"+details.getCode3ExistsInIM()+"<TOK_SEPARATOR>";
						
						if(metaDataType.startsWith("ESICATEGORY_WD"))
						{
							dispData+=details.getSteeringTypeInfoText()+"<TOK_SEPARATOR>"+details.getFailedSteeringTypeInIM()+"<TOK_SEPARATOR>";
						}
						
						dispData+=details.getErrorMessage();
						
						dispToken = dispData.split("<TOK_SEPARATOR>");
						if(null!=dispToken && dispToken.length>0)
						{
							for(int r=0;r<dispToken.length;r++)
							{
								dataCell = row.createCell(r);
								dataCell.setCellValue("");
								if(null!=dispToken[r] && !"".equals(dispToken[r]) && !"null".equals(dispToken[r].trim().toLowerCase()))
								{
									dataCell.setCellValue(dispToken[r].trim());
								}
								dataCell=null;
							}
						}
						dataCell=null;
						row = null;
						details = null;
						dispData=null;
						dispToken=null;
					}
					dataCell=null;
					row = null;
					details = null;
					dispData=null;
					dispToken=null;
					headerRow = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					myWorkBook.write(os);
					logger.info("ESI CATEGORY METADATA FAILURE REPORT XLSX file Finished ...");
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
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printESICategoryReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printESICategoryReport :: Method Ends.");
	}

	/**
	 * Failure report of an MNAO vin.txt: the VIN columns of the MC / MME report plus the model, the model year and its
	 * category.
	 */
	public void printMNAOVinReport(String scheduleCode , ArrayList<VinDetails> vinList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printMNAOVinReport :: Method Starts.");
		try {
			if (null != vinList && vinList.size() > 0 && null != scheduleCode && !"".equals(scheduleCode))
			{
				String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");
				File reportDirectory = PathUtil.file(path);
				if (!reportDirectory.exists() || !reportDirectory.isDirectory()) {
					reportDirectory.mkdir();
				}
				if(!path.endsWith("/"))
				{
					path = path+"/";
				}
				path = path + scheduleCode;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) {
					scheduleDirectory.mkdir();
				}
				File myFile = PathUtil.file(path + "/" + scheduleCode + ".xlsx");
				// KEEP 100 ROWS IN MEMORY AND REST ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);
				Sheet mySheet = myWorkBook.createSheet("Details");
				String[] headers = { "SCHEDULE NAME", "FILE NAME", "FILE PATH", "PROCESSING STATUS", "LOCALE", "MANUAL TYPE",
						"FACELIFT FOLDER", "MATERIAL FOLDER", "CARLINE CODE", "WMI CODE", "VDS CODE", "VIS START RANGE", "VIS END RANGE",
						"VIN EXISTS IN MDM", "VIN REF KEY", "VIN EXISTS IN KAPTURE", "MODEL", "MODEL YEAR", "MODEL YEAR REF KEY",
						"MODEL YEAR EXISTS IN KAPTURE", "DISPLAY ORDER FILE", "ERROR MESSAGE" };
				Row headerRow = mySheet.createRow(0);
				for (int c = 0; c < headers.length; c++) {
					headerRow.createCell(c).setCellValue(headers[c]);
				}
				int rowCount = 0;
				for (VinDetails details : vinList) {
					rowCount++;
					String[] values = { ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode, details.getVinSourceFileName(),
							details.getVinSorceFilePath(), details.getProcessingStatus(), locale, details.getManualType(), details.getFaceLiftFolder(),
							details.getMaterialFolder(), details.getCarlineCode(), details.getWmiCode(), details.getVdsCode(), details.getVisStartRange(),
							details.getVisEndRange(), details.getVinFoundInMDM(), details.getVinRefKey(), details.getVinFoundInIM(), details.getModelName(),
							details.getModelYear(), details.getModelYearRefKey(), details.getModelYearFoundInKapture(),
							details.getDisplayOrderTextFileName(), details.getErrorMessage() };
					Row row = mySheet.createRow(rowCount);
					for (int c = 0; c < values.length; c++) {
						row.createCell(c).setCellValue(null == values[c] ? "" : values[c]);
					}
				}
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				try {
					myWorkBook.write(os);
					os.flush();
				} finally {
					os.close();
					myWorkBook.dispose();
				}
				logger.info("MNAO VIN METADATA FAILURE REPORT XLSX file Finished ...");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMNAOVinReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printMNAOVinReport :: Method Ends.");
	}

}