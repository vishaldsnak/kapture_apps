package com.mazda.gms3.dmt.automation.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.PrintReportsUtil;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class PrintReportsUtils {
	
	private Logger logger = LogManager.getLogger(PrintReportsUtils.class);
	
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

	
	
	@SuppressWarnings({ "resource", "unchecked" })
	public void printOtherChannelReports(ItemDetails itemDetails, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=itemDetails && null!=itemDetails.getImpactedDocumentsList() 
					&& itemDetails.getImpactedDocumentsList().size()>0)
			{
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
				 * Prepare File name
				 * ITEMCODE_LOCALE_CHANNEL_INITIALS_<MANUAL_TYPE_IF AVAILABLE>_MODEL.XLSX
				 */
				String channelInitials=itemDetails.getChannelRefKey();
				if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE))
				{
					channelInitials="SI";
				}
				String fName = "/" + itemDetails.getItemId()+"_"+itemDetails.getLocale().trim()+"_"+channelInitials;
				if(null!=itemDetails.getDocumentTypeRefKey() && !"".equals(itemDetails.getDocumentTypeRefKey()))
				{
					// add manualType
					fName=fName+"_"+itemDetails.getDocumentTypeRefKey().trim();
				}
				if(null!=itemDetails.getCarlineInfo() && !"".equals(itemDetails.getCarlineInfo()))
				{
					// add carline
					fName=fName+"_"+itemDetails.getCarlineInfo().trim();
				}
				fName = fName.replace("-", "_");
				fName = fName.trim().toUpperCase()+".xlsx";
				
				File myFile = PathUtil.file(path + fName);
				fName = null;
				channelInitials = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="LOCALE,CHANNEL,DOCUMENT_TYPE,MODEL,DOCUMENT_ID,OLD_VIN_REF_KEY,NEW_VIN_REF_KEY,"
						+ "OLD_CARLINE,OLD_WMI,OLD_VDS,OLD_VIS_START,OLD_VIS_END,NEW_CARLINE,NEW_WMI,NEW_VDS,NEW_VIS_START,NEW_VIS_END";
				String[] tokens=headers.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(tokens[a].replace("_", " "));
						headerCell  = null;
					}
				}
				tokens = null;
				headerCell=null;
				headerRow=null;
				headers=null;
				
				int rowCount = 0;
				/*
				 * GENERATE MULTIPLE ROWS SUCH THAT
				 * item data + Document Id + multiple Rows for Applicable VIN List
				 */
				String dataRow="";
				Map<String, Object> documentMap = null;
				ArrayList<ExcelRowDetails> appvinList = new ArrayList<ExcelRowDetails>();
				ExcelRowDetails vinDetails = new ExcelRowDetails();
				
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < itemDetails.getImpactedDocumentsList().size(); a++) {
					documentMap = (Map<String, Object>) itemDetails.getImpactedDocumentsList().get(a);

					if(null!=documentMap.get("VIN_LIST"))
					{
						appvinList = (ArrayList<ExcelRowDetails>)documentMap.get("VIN_LIST");
					}
					if(null!=appvinList && appvinList.size()>0)
					{
						vinDetails = new ExcelRowDetails();
						for(int b=0;b<appvinList.size();b++)
						{
							vinDetails = (ExcelRowDetails)appvinList.get(b);
							dataRow=itemDetails.getLocale()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getChannelLabel()+"<TOK_SEPARATOR>"+itemDetails.getDocumentTypeLabel()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getCarlineInfo()+"<TOK_SEPARATOR>"+documentMap.get("DOCUMENTID")+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldRefKey()+"<TOK_SEPARATOR>"+vinDetails.getNewRefKey()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldCarlineCode()+"<TOK_SEPARATOR>"+vinDetails.getOldWmiCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVdsCode()+"<TOK_SEPARATOR>"+vinDetails.getOldVisStartRange()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVisEndRange()+"<TOK_SEPARATOR>"+vinDetails.getNewCarlineCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewWmiCode()+"<TOK_SEPARATOR>"+vinDetails.getNewVdsCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewVisStartRange()+"<TOK_SEPARATOR>"+vinDetails.getNewVisEndRange();
							
							// increment rowCount by 1
							rowCount++;
							// Create a new Row
							row = mySheet.createRow(rowCount);
							tokens = dataRow.split("<TOK_SEPARATOR>");
							if(null!=tokens && tokens.length>0)
							{
								for(int e=0;e<tokens.length;e++)
								{
									dataCell = row.createCell(e);
									dataCell.setCellValue("");
									if(null!=tokens[e] && !"".equals(tokens[e]) && !"null".equals(tokens[e].trim().toLowerCase()))
									{
										dataCell.setCellValue(tokens[e].trim());
									}
									dataCell =null;
								}
							}
							tokens = null;
							row=null;
							dataRow = null;
							dataCell = null;
							vinDetails = null;
						}
						vinDetails = null;
					}
					
					appvinList = null;
					documentMap = null;
				}

				headerRow = null;
				
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on OTHER CHANNEL REPORT XLSX file Finished ...");
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
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printOtherChannelReports()", e);
		}
	}

	@SuppressWarnings({ "resource", "unchecked" })
	public void printSMWDChannelReports(ItemDetails itemDetails, String scheduleCode, ArrayList<Map<String, Object>> transactionList)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=itemDetails && null!=transactionList)
			{
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
				 * Prepare File name
				 * ITEMCODE_LOCALE_CHANNEL_INITIALS_<MANUAL_TYPE_IF AVAILABLE>_MODEL.XLSX
				 */
				String channelInitials=itemDetails.getChannelRefKey();
				if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE))
				{
					channelInitials="SM";
				}
				else if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
				{
					channelInitials="OSM";
				}
				
				String fName = "/" + itemDetails.getItemId()+"_"+itemDetails.getLocale().trim()+"_"+channelInitials;
				if(null!=itemDetails.getDocumentTypeRefKey() && !"".equals(itemDetails.getDocumentTypeRefKey()))
				{
					// add manualType
					fName=fName+"_"+itemDetails.getDocumentTypeRefKey().trim();
				}
				if(null!=itemDetails.getCarlineInfo() && !"".equals(itemDetails.getCarlineInfo()))
				{
					// add carline
					fName=fName+"_"+itemDetails.getCarlineInfo().trim();
				}
				fName = fName.replace("-", "_");
				fName = fName.trim().toUpperCase()+".xlsx";
				
				File myFile = PathUtil.file(path + fName);
				fName = null;
				channelInitials = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="LOCALE,CHANNEL,DOCUMENT_TYPE,MODEL,DOCUMENT_ID,OLD_VIN_REF_KEY,NEW_VIN_REF_KEY,"
						+ "OLD_CARLINE,OLD_WMI,OLD_VDS,OLD_VIS_START,OLD_VIS_END,NEW_CARLINE,NEW_WMI,NEW_VDS,NEW_VIS_START,NEW_VIS_END,PROCESSING_STATUS,DOCUMENT_VERSION, DOCUMENT_STATUS,REMARKS";
				String[] tokens=headers.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(tokens[a].replace("_", " "));
						headerCell  = null;
					}
				}
				tokens = null;
				headerCell=null;
				headerRow=null;
				headers=null;
				
				
				Map<String, Object> documentMap = null;
				int rowCount = 0;
				/*
				 * GENERATE MULTIPLE ROWS SUCH THAT
				 * item data + Document Id + multiple Rows for Applicable VIN List
				 */
				String dataRow="";
				ArrayList<ExcelRowDetails> appvinList = new ArrayList<ExcelRowDetails>();
				ExcelRowDetails vinDetails = new ExcelRowDetails();
				Row row=null;
				Cell dataCell=null;
				
				for(int a=0;a<transactionList.size();a++)
				{
					documentMap = (Map<String, Object>)transactionList.get(a);
					if(null!=documentMap.get(AutomationConstants.VIN_LIST))
					{
						appvinList = (ArrayList<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST);
					}
					
					if(null!=appvinList && appvinList.size()>0)
					{
						vinDetails = new ExcelRowDetails();
						for(int b=0;b<appvinList.size();b++)
						{
							vinDetails = (ExcelRowDetails)appvinList.get(b);
							dataRow=itemDetails.getLocale()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getChannelLabel()+"<TOK_SEPARATOR>"+itemDetails.getDocumentTypeLabel()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getCarlineInfo()+"<TOK_SEPARATOR>"+documentMap.get(AutomationConstants.DOCUMENTID)+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldRefKey()+"<TOK_SEPARATOR>"+vinDetails.getNewRefKey()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldCarlineCode()+"<TOK_SEPARATOR>"+vinDetails.getOldWmiCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVdsCode()+"<TOK_SEPARATOR>"+vinDetails.getOldVisStartRange()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVisEndRange()+"<TOK_SEPARATOR>"+vinDetails.getNewCarlineCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewWmiCode()+"<TOK_SEPARATOR>"+vinDetails.getNewVdsCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewVisStartRange()+"<TOK_SEPARATOR>"+vinDetails.getNewVisEndRange()+"<TOK_SEPARATOR>";
							dataRow+=documentMap.get(AutomationConstants.PROCESSING_STATUS)+"<TOK_SEPARATOR>"+documentMap.get(AutomationConstants.DOCUMENT_VERSION)+"<TOK_SEPARATOR>";
							dataRow+=documentMap.get(AutomationConstants.DOCUMENT_STATUS)+"<TOK_SEPARATOR>"+documentMap.get(AutomationConstants.ERROR_MESSAGE);
							
							// increment rowCount by 1
							rowCount++;
							// Create a new Row
							row = mySheet.createRow(rowCount);
							tokens = dataRow.split("<TOK_SEPARATOR>");
							if(null!=tokens && tokens.length>0)
							{
								for(int e=0;e<tokens.length;e++)
								{
									dataCell = row.createCell(e);
									dataCell.setCellValue("");
									if(null!=tokens[e] && !"".equals(tokens[e]) && !"null".equals(tokens[e].trim().toLowerCase()))
									{
										dataCell.setCellValue(tokens[e].trim());
									}
									dataCell =null;
								}
							}
							tokens = null;
							row=null;
							dataRow = null;
							dataCell = null;
							vinDetails = null;
						}
						vinDetails = null;
					}
				}
			
				headerRow = null;
				
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on SM / WD CHANNEL REPORT XLSX file Finished ...");
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
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printSMWDChannelReports()", e);
		}
	}

	@SuppressWarnings("resource")
	public void printErrorReports(ItemDetails itemDetails, String scheduleCode, ArrayList<Map<String, Object>> errorsList)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=itemDetails && null!=errorsList)
			{
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
				 * Prepare File name
				 * ITEMCODE_LOCALE_CHANNEL_INITIALS_<MANUAL_TYPE_IF AVAILABLE>_MODEL.XLSX
				 */
				String channelInitials=itemDetails.getChannelRefKey();
				if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE))
				{
					channelInitials="SM";
				}
				else if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
				{
					channelInitials="OSM";
				}
				String fName = "/FAILURE_" + itemDetails.getItemId()+"_"+itemDetails.getLocale().trim()+"_"+channelInitials;
				if(null!=itemDetails.getDocumentTypeRefKey() && !"".equals(itemDetails.getDocumentTypeRefKey()))
				{
					// add manualType
					fName=fName+"_"+itemDetails.getDocumentTypeRefKey().trim();
				}
				if(null!=itemDetails.getCarlineInfo() && !"".equals(itemDetails.getCarlineInfo()))
				{
					// add carline
					fName=fName+"_"+itemDetails.getCarlineInfo().trim();
				}
				fName = fName.replace("-", "_");
				fName = fName.trim().toUpperCase()+".xlsx";
				
				File myFile = PathUtil.file(path + fName);
				fName = null;
				channelInitials = null;
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="LOCALE,CHANNEL,DOCUMENT_TYPE,MODEL,DOCUMENT_ID,MISSING_REF_KEY,"
						+ "ERROR_CODE,ERROR_MESSAGE";
				String[] tokens=headers.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(tokens[a].replace("_", " "));
						headerCell  = null;
					}
				}
				tokens = null;
				headerCell=null;
				headerRow=null;
				headers=null;
				
				
				Map<String, Object> documentMap = null;
				int rowCount = 0;
				/*
				 * GENERATE MULTIPLE ROWS SUCH THAT
				 * item data + Document Id + multiple Rows for Applicable VIN List
				 */
				String dataRow="";
				Row row=null;
				Cell dataCell=null;
				
				for(int a=0;a<errorsList.size();a++)
				{
					documentMap = (Map<String, Object>)errorsList.get(a);
					dataRow=itemDetails.getLocale()+"<TOK_SEPARATOR>";
					dataRow+=itemDetails.getChannelLabel()+"<TOK_SEPARATOR>"+itemDetails.getDocumentTypeLabel()+"<TOK_SEPARATOR>";
					dataRow+=itemDetails.getCarlineInfo()+"<TOK_SEPARATOR>"+documentMap.get(AutomationConstants.DOCUMENTID)+"<TOK_SEPARATOR>";
					dataRow+=documentMap.get(AutomationConstants.MISSING_REFKEY)+"<TOK_SEPARATOR>"+documentMap.get(AutomationConstants.ERROR_CODE)+"<TOK_SEPARATOR>";
					dataRow+=documentMap.get(AutomationConstants.ERROR_MESSAGE);
					
					// increment rowCount by 1
					rowCount++;
					// Create a new Row
					row = mySheet.createRow(rowCount);
					tokens = dataRow.split("<TOK_SEPARATOR>");
					if(null!=tokens && tokens.length>0)
					{
						for(int e=0;e<tokens.length;e++)
						{
							dataCell = row.createCell(e);
							dataCell.setCellValue("");
							if(null!=tokens[e] && !"".equals(tokens[e]) && !"null".equals(tokens[e].trim().toLowerCase()))
							{
								dataCell.setCellValue(tokens[e].trim());
							}
							dataCell =null;
						}
					}
					tokens = null;
					row=null;
					dataRow = null;
					dataCell = null;
					documentMap = null;
				}
			
				headerRow = null;
				
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on SM / WD CHANNEL ERROR REPORT XLSX file Finished ...");
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
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printErrorReports()", e);
		}
	}
	
	@SuppressWarnings("resource")
	public void printVINManualMappingReport(String scheduleCode, ArrayList<ExcelRowDetails> inputDataList)
	{

		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=inputDataList)
			{
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
				 * Prepare File name
				 * ITEMCODE_LOCALE_CHANNEL_INITIALS_<MANUAL_TYPE_IF AVAILABLE>_MODEL.XLSX
				 */
				
				
				String fName =  "/VIN_MANUAL_TYPE_MAPPING_REPORT"+".xlsx";
				
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
				Cell headerCell = null;
				
				String headers="LOCALE,OLD CARLINE CODE,OLD WMI,OLD VDS,OLD VIS START RANGE,OLD VIS END RANGE,OLD WD MAPPING,OLD WM MAPPING,OLD BSM MAPPING,OLD MC MAPPING,";
				headers+="OLD AT MAPPING,OLD MT MAPPING,OLD ENGINE MAPPING,OLD TG MAPPING,OLD TQG MAPPING,NEW CARLINE CODE,NEW WMI,NEW VDS,NEW VIS START,NEW VIS END,";
				headers+="NEW WD MAPPING,NEW WM MAPPING,NEW BSM MAPPING,NEW MC MAPPING,NEW AT MAPPING,NEW MT MAPPING,NEW ENGINE MAPPING,NEW TG MAPPING,NEW TQG MAPPING,PROCESSING STATUS,REMARKS";
				
				String[] tokens=headers.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(tokens[a].replace("_", " "));
						headerCell  = null;
					}
				}
				tokens = null;
				headerCell=null;
				headerRow=null;
				headers=null;
				
				
				int rowCount = 0;
				/*
				 * GENERATE MULTIPLE ROWS SUCH THAT
				 * item data + Document Id + multiple Rows for Applicable VIN List
				 */
				String dataRow="";
				ExcelRowDetails vinDetails = new ExcelRowDetails();
				VinMLMappingDetails oldies=new VinMLMappingDetails();
				VinMLMappingDetails current = new VinMLMappingDetails();
				Row row=null;
				Cell dataCell=null;
				
				for(int a=0;a<inputDataList.size();a++)
				{
					vinDetails = (ExcelRowDetails)inputDataList.get(a);
					if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(vinDetails.getLocale().replace("-", "_").trim().toLowerCase())>-1)
					{
						oldies= new VinMLMappingDetails();
						current=  new VinMLMappingDetails();
						if(null!=vinDetails.getOldDetails())
						{
							oldies=  vinDetails.getOldDetails();
						}
						if(null!=vinDetails.getNewDetails())
						{
							current=  vinDetails.getNewDetails();
						}

						dataRow=vinDetails.getLocale()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getOldCarlineCode()+"<TOK_SEPARATOR>"+vinDetails.getOldWmiCode()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getOldVdsCode()+"<TOK_SEPARATOR>"+vinDetails.getOldVisStartRange()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getOldVisEndRange()+"<TOK_SEPARATOR>"+oldies.getWdMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=oldies.getWmMappingLocale()+"<TOK_SEPARATOR>"+oldies.getBsmMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=oldies.getMcMappingLocale()+"<TOK_SEPARATOR>"+oldies.getAtMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=oldies.getMtMappingLocale()+"<TOK_SEPARATOR>"+oldies.getEngineMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=oldies.getTgMappingLocale()+"<TOK_SEPARATOR>"+oldies.getTqgMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getNewCarlineCode()+"<TOK_SEPARATOR>"+vinDetails.getNewWmiCode()+"<TOK_SEPARATOR>"+vinDetails.getNewVdsCode()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getNewVisStartRange()+"<TOK_SEPARATOR>"+vinDetails.getNewVisEndRange()+"<TOK_SEPARATOR>";
						dataRow+=current.getWdMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=current.getWmMappingLocale()+"<TOK_SEPARATOR>"+current.getBsmMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=current.getMcMappingLocale()+"<TOK_SEPARATOR>"+current.getAtMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=current.getMtMappingLocale()+"<TOK_SEPARATOR>"+current.getEngineMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=current.getTgMappingLocale()+"<TOK_SEPARATOR>"+current.getTqgMappingLocale()+"<TOK_SEPARATOR>";
						dataRow+=vinDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+vinDetails.getRemarks()+"<TOK_SEPARATOR>";
						
						// increment rowCount by 1
						rowCount++;
						// Create a new Row
						row = mySheet.createRow(rowCount);
						tokens = dataRow.split("<TOK_SEPARATOR>");
						if(null!=tokens && tokens.length>0)
						{
							for(int e=0;e<tokens.length;e++)
							{
								dataCell = row.createCell(e);
								dataCell.setCellValue("");
								if(null!=tokens[e] && !"".equals(tokens[e]) && !"null".equals(tokens[e].trim().toLowerCase()))
								{
									dataCell.setCellValue(tokens[e].trim());
								}
								dataCell =null;
							}
						}
						tokens = null;
						row=null;
						dataRow = null;
						dataCell = null;
						oldies=  null;
						current=  null;
					}
					vinDetails=  null;
				}
			
				headerRow = null;
				
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
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printVINManualMappingReport()", e);
		}
	
	}

}