package com.mazda.gms3.mdm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;

public class SIVINBatchProcessingReportsUtil {

	private Logger logger = LogManager.getLogger(SIVINBatchProcessingReportsUtil.class);
	

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
				String zipPath = reportsDirectoryPath	+ scheduleCode + "_" + ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
				File file = new File(reportsDirectoryPath);
				if (file.exists() && file.isDirectory()) 
				{
					FileOutputStream fos = new FileOutputStream(zipPath);
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
				File zip = new File(zipPath);
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
					File excelFiles = new File(reportsDirectoryPath);
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
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "createReportsZip()", e);
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
		FileInputStream fis = new FileInputStream(childFile);
		zos.putNextEntry(new ZipEntry(childFile.getName()));
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	public void printCategoryTransactionReport(List<IMCategoryDetails> categoryProcessingList, String scheduleCode, String market)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=categoryProcessingList  
					&& categoryProcessingList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = new File(path);
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
				File scheduleDirectory = new File(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * Prepare File name
				 * scheduleCode_CATEGORY_CREATION_REPORT
				 */
				String fName = "/" + scheduleCode+"_CATEGORY_CREATION_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="";
				if(market.equals("MNAO"))
				{
					headers="LOCALE,MODEL,YEAR,CARLINE_CODE,WMI_CODE,VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,CATEGORY_NAME,CATEGORY_REFKEY,PARENT_REFKEY,OPERATION_TYPE,"
							+ "CATEGORY_LEVEL,PROCESSING_STATUS";
				}
				else
				{
					headers="LOCALE,MODEL,CARLINE_CODE,WMI_CODE,VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,CATEGORY_NAME,CATEGORY_REFKEY,PARENT_REFKEY,OPERATION_TYPE,"
							+ "CATEGORY_LEVEL,PROCESSING_STATUS";
				}
				
				
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
				IMCategoryDetails details = null;
				SIVinDetails itemDetails = new SIVinDetails();
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < categoryProcessingList.size(); a++) 
				{
					details = (IMCategoryDetails)categoryProcessingList.get(a);
					itemDetails = new SIVinDetails();
					if(null!=details.getItemDetails())
					{
						itemDetails = details.getItemDetails();
					}
					
					dataRow=details.getLocale()+"<TOK_SEPARATOR>"+itemDetails.getModel();
					if(market.equals("MNAO"))
					{
						dataRow+="<TOK_SEPARATOR>"+itemDetails.getYear();
					}
					dataRow+="<TOK_SEPARATOR>"+itemDetails.getCarlineCode()+"<TOK_SEPARATOR>"+itemDetails.getWmiCode()+"<TOK_SEPARATOR>"+itemDetails.getVdsCode()+
							"<TOK_SEPARATOR>"+itemDetails.getVinStartRange()+"<TOK_SEPARATOR>"+itemDetails.getVinEndRange()+"<TOK_SEPARATOR>"
							+details.getCategoryName()+"<TOK_SEPARATOR>"+details.getCategoryRefKey()+"<TOK_SEPARATOR>"+
							details.getParentRefKey()+"<TOK_SEPARATOR>"+details.getOperationType()+"<TOK_SEPARATOR>"+details.getLevel()+
							"<TOK_SEPARATOR>"+details.getProcessingStatus();
					
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
					
					itemDetails  =null;
					details = null;
				}
				headerRow = null;
				
				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
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
				// set fileName to null
				fName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "printCategoryTransactionReport()", e);
		}
	}

	public void printCategoryFailureReport(List<IMCategoryDetails> categoryProcessingList, String scheduleCode, String market)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=categoryProcessingList  
					&& categoryProcessingList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = new File(path);
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
				File scheduleDirectory = new File(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * Prepare File name
				 * scheduleCode_CATEGORY_CREATION_REPORT
				 */
				String fName = "/" + scheduleCode+"_FAILURE_CATEGORY_CREATION_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="";
				if(market.equals("MNAO"))
				{
					headers="LOCALE,MODEL,YEAR,CARLINE_CODE,WMI_CODE,VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,CATEGORY_NAME,CATEGORY_REFKEY,PARENT_REFKEY,OPERATION_TYPE,"
							+ "CATEGORY_LEVEL,PROCESSING_STATUS,ERROR_CODE,ERROR_MESSAGE";
				}
				else
				{
					headers="LOCALE,MODEL,CARLINE_CODE,WMI_CODE,VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,CATEGORY_NAME,CATEGORY_REFKEY,PARENT_REFKEY,OPERATION_TYPE,"
							+ "CATEGORY_LEVEL,PROCESSING_STATUS,ERROR_CODE,ERROR_MESSAGE";
				}
				
				
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
				IMCategoryDetails details = null;
				SIVinDetails itemDetails = new SIVinDetails();
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < categoryProcessingList.size(); a++) 
				{
					details = (IMCategoryDetails)categoryProcessingList.get(a);
					
					itemDetails = new SIVinDetails();
					if(null!=details.getItemDetails())
					{
						itemDetails = details.getItemDetails();
					}
					
					dataRow=details.getLocale()+"<TOK_SEPARATOR>"+itemDetails.getModel();
					if(market.equals("MNAO"))
					{
						dataRow+="<TOK_SEPARATOR>"+itemDetails.getYear();
					}
					dataRow+="<TOK_SEPARATOR>"+itemDetails.getCarlineCode()+"<TOK_SEPARATOR>"+itemDetails.getWmiCode()+"<TOK_SEPARATOR>"+itemDetails.getVdsCode()+
							"<TOK_SEPARATOR>"+itemDetails.getVinStartRange()+"<TOK_SEPARATOR>"+itemDetails.getVinEndRange()+"<TOK_SEPARATOR>"
							+details.getCategoryName()+"<TOK_SEPARATOR>"+details.getCategoryRefKey()+"<TOK_SEPARATOR>"+
							details.getParentRefKey()+"<TOK_SEPARATOR>"+details.getOperationType()+"<TOK_SEPARATOR>"+details.getLevel()+
							"<TOK_SEPARATOR>"+details.getProcessingStatus()+"<TOK_SEPARATOR>"+details.getErrorCode()+"<TOK_SEPARATOR>"+details.getErrorMessage();
					
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
					
					itemDetails  =null;
					details = null;
				}
				headerRow = null;
				
				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
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
				// set fileName to null
				fName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "printCategoryFailureReport()", e);
		}
	}

	public void printDocumentTransactionReport(SIVINScheduleDetails documentDetails, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=documentDetails)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = new File(path);
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
				File scheduleDirectory = new File(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * Prepare File name
				 * scheduleCode_CATEGORY_CREATION_REPORT
				 */
				String fName = "/" + documentDetails.getDocumentId()+"_MODIFICATION_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="DOCUMENT_ID,LOCALE,FETCHED_VERSION,MODIFIED_VERSION,PUBLISHED,KAPTURE_PROCESSING_STATUS,DB_PROCESSING_STATUS,CATEGORY,MAPPING_STATUS,ERROR_CODE,ERROR_MESSAGE";
				
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
				IMCategoryDetails details = null;
				Row row=null;
				Cell dataCell=null;
				String errorCode="";
				String errorMessage="";
				
				if(null!=documentDetails.getCategoryList() && documentDetails.getCategoryList().size()>0)
				{
					for (int a = 0; a < documentDetails.getCategoryList().size(); a++)
					{
						details = (IMCategoryDetails)documentDetails.getCategoryList().get(a);
						
						dataRow=documentDetails.getDocumentId()+"<TOK_SEPARATOR>"+documentDetails.getLocale();
						
						dataRow+="<TOK_SEPARATOR>"+documentDetails.getFetchedVersion()+"<TOK_SEPARATOR>"+documentDetails.getUpdatedVersion();
						if(documentDetails.isDocumentPublishedStatus()==true)
						{
							dataRow+="<TOK_SEPARATOR>"+"Y";
						}
						else
						{
							dataRow+="<TOK_SEPARATOR>"+"";
						}
						dataRow+="<TOK_SEPARATOR>"+documentDetails.getImProcessingStatus()+"<TOK_SEPARATOR>"+documentDetails.getDbprocessingStatus()+
								"<TOK_SEPARATOR>"+details.getCategoryName()+"<TOK_SEPARATOR>"+details.getProcessingStatus();
						
						errorCode = "";
						if(null!=documentDetails.getErrorCode() && !"".equals(documentDetails.getErrorCode()))
						{
							errorCode = documentDetails.getErrorCode()+",";
						}
						if(null!=details.getErrorCode() && !"".equals(details.getErrorCode()))
						{
							errorCode+=details.getErrorCode();
						}
						if(null!=errorCode && !"".equals(errorCode))
						{
							if(errorCode.endsWith(","))
							{
								errorCode=  errorCode.substring(0, errorCode.length()-1);
							}
						}
						dataRow+="<TOK_SEPARATOR>"+errorCode;
						errorCode = null;
						
						errorMessage = "";
						if(null!=documentDetails.getErrorMessage() && !"".equals(documentDetails.getErrorMessage()))
						{
							errorMessage = documentDetails.getErrorMessage()+"\r\n";
						}
						if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
						{
							errorMessage+=details.getErrorMessage();
						}
						dataRow+="<TOK_SEPARATOR>"+errorMessage;
						errorMessage = null;
						
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
						
						details = null;
					}
				}
				headerRow = null;
				
				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
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
				// set fileName to null
				fName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "printDocumentTransactionReport()", e);
		}
	}

	public void printTranslationTransactionReport(List<SIVINScheduleDetails> documentsList, String scheduleCode,String documentId)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=documentsList && documentsList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY");

				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
				File wslDirectory = new File(path);
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
				File scheduleDirectory = new File(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				/*
				 * Prepare File name
				 * scheduleCode_CATEGORY_CREATION_REPORT
				 */
				String fName = "/" + documentId+"_TRANSLATIONS_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="DOCUMENT_ID,LOCALE,FETCHED_VERSION,MODIFIED_VERSION,PUBLISHED,KAPTURE_PROCESSING_STATUS,"
						+ "CATEGORY,MAPPING_STATUS,ERROR_CODE,ERROR_MESSAGE";
				
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
				IMCategoryDetails details = null;
				Row row=null;
				Cell dataCell=null;
				String errorCode="";
				String errorMessage="";
				SIVINScheduleDetails documentDetails = null;
				
				
				for(int b=0;b<documentsList.size();b++)
				{
					documentDetails = (SIVINScheduleDetails)documentsList.get(b);
					if(null!=documentDetails.getCategoryList() && documentDetails.getCategoryList().size()>0)
					{
						for (int a = 0; a < documentDetails.getCategoryList().size(); a++)
						{
							details = (IMCategoryDetails)documentDetails.getCategoryList().get(a);
							
							dataRow=documentDetails.getDocumentId()+"<TOK_SEPARATOR>"+documentDetails.getLocale();
							
							dataRow+="<TOK_SEPARATOR>"+documentDetails.getFetchedVersion()+"<TOK_SEPARATOR>"+documentDetails.getUpdatedVersion();
							if(documentDetails.isDocumentPublishedStatus()==true)
							{
								dataRow+="<TOK_SEPARATOR>"+"Y";
							}
							else
							{
								dataRow+="<TOK_SEPARATOR>"+"";
							}
							dataRow+="<TOK_SEPARATOR>"+documentDetails.getImProcessingStatus()+"<TOK_SEPARATOR>"+details.getCategoryName()+"<TOK_SEPARATOR>"+details.getProcessingStatus();
							
							errorCode = "";
							if(null!=documentDetails.getErrorCode() && !"".equals(documentDetails.getErrorCode()))
							{
								errorCode = documentDetails.getErrorCode()+",";
							}
							if(null!=details.getErrorCode() && !"".equals(details.getErrorCode()))
							{
								errorCode+=details.getErrorCode();
							}
							if(null!=errorCode && !"".equals(errorCode))
							{
								if(errorCode.endsWith(","))
								{
									errorCode=  errorCode.substring(0, errorCode.length()-1);
								}
							}
							dataRow+="<TOK_SEPARATOR>"+errorCode;
							errorCode = null;
							
							errorMessage = "";
							if(null!=documentDetails.getErrorMessage() && !"".equals(documentDetails.getErrorMessage()))
							{
								errorMessage = documentDetails.getErrorMessage()+"\r\n";
							}
							if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
							{
								errorMessage+=details.getErrorMessage();
							}
							dataRow+="<TOK_SEPARATOR>"+errorMessage;
							errorMessage = null;
							
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
							
							details = null;
						}
					}
					documentDetails = null;
				}
				
				headerRow = null;
				
				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
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
				// set fileName to null
				fName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "printTranslationTransactionReport()", e);
		}
	}

}
