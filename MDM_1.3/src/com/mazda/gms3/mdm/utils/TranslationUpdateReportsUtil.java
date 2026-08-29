package com.mazda.gms3.mdm.utils;

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

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ContentDetails;

public class TranslationUpdateReportsUtil {
	
	private Logger logger = LogManager.getLogger(TranslationUpdateReportsUtil.class);
	
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
			if (null != reportsDirectoryPath && !"".equals(reportsDirectoryPath) && null != scheduleCode && !"".equals(scheduleCode)) 
			{
				String zipPath = reportsDirectoryPath	+ scheduleCode+"_XMLS"	+ ApplicationProperties.getProperty("ZIP_SUFFIX");
				File file = new File(reportsDirectoryPath);
				if (file.exists() && file.isDirectory()) 
				{
					FileOutputStream fos = new FileOutputStream(zipPath);
					ZipOutputStream zos = new ZipOutputStream(fos);
					File[] childFiles = file.listFiles();
					for (int a = 0; a < childFiles.length; a++) 
					{
						File reports = childFiles[a];
						if (reports.exists()
								&& reports.isFile()
								&& !reports
										.getName()
										.contains(
												ApplicationProperties
														.getProperty("ZIP_SUFFIX"))) {
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
									.getProperty("ZIP_SUFFIX")
							+ "} Generated Successfully");
					/*
					 * Proceed for deleting all the other files
					 */
					File xmlFiles = new File(reportsDirectoryPath);
					if (xmlFiles.exists() && xmlFiles.isDirectory()) {
						File[] childFiles = xmlFiles.listFiles();
						for (int a = 0; a < childFiles.length; a++) {
							File excelReports = childFiles[a];
							if (excelReports.exists() && excelReports.isFile()) 
							{
								if (!excelReports
										.getName()
										.contains(
												ApplicationProperties
														.getProperty("ZIP_SUFFIX"))) {
									excelReports.delete();
								}
								excelReports = null;
							}
						}
						childFiles = null;
					}
					xmlFiles = null;
				} else {
					logger.info("createReportsZip :: Failed to generate Zip file {"
							+ scheduleCode
							+ "_"
							+ ApplicationProperties
									.getProperty("ZIP_SUFFIX")
							+ "}. Download the XML files manually.");
					return false;
				}
				zipPath = null;
			} else {
				logger.info("createReportsZip :: Reports Directory Path and Scheduled Date is passed as null in parameters. Return false");
				return false;
			}
		} catch (FileNotFoundException e) {
			Utilities.printStackTraceToLogs(TranslationUpdateReportsUtil.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(TranslationUpdateReportsUtil.class.getName(), "createReportsZip()", e);
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

	public void printExportTransactionReport(ArrayList<ContentDetails> transactionList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=transactionList  
					&& transactionList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("translation.update.export.physical.path");

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
				if(!path.endsWith("/"))
				{
					path = path+"/";
				}
				path = path + scheduleCode;
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
				String fName = "/" + scheduleCode+"_EXPORT_TRANSACTION_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				@SuppressWarnings("resource")
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="";
				headers="LOCALE,DOCUMENT_ID,OPERATION_TYPE,PROCESSING_STATUS,ERROR_CODES,ERROR_MESSAGES";
				
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
				ContentDetails details = null;
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < transactionList.size(); a++) 
				{
					details = (ContentDetails)transactionList.get(a);
					
					dataRow=details.getLocale()+"<TOK_SEPARATOR>"+details.getDocumentId();
					dataRow+="<TOK_SEPARATOR>"+AccessManagementInterface.OPERATION_TYPE_EXPORT+"<TOK_SEPARATOR>"+details.getProcessingStatus()+"<TOK_SEPARATOR>"+details.getErrorCode()+
							"<TOK_SEPARATOR>"+details.getErrorMessage();
					
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
			Utilities.printStackTraceToLogs(TranslationUpdateReportsUtil.class.getName(), "printExportTransactionReport()", e);
		}
	}

	public void printImportTransactionReport(ArrayList<ContentDetails> transactionList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && 
					null!=transactionList  
					&& transactionList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("translation.update.import.physical.path");

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
				if(!path.endsWith("/"))
				{
					path = path+"/";
				}
				path = path + scheduleCode;
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
				String fName = "/" + scheduleCode+"_IMPORT_TRANSACTION_REPORT.xlsx";
				
				File myFile = new File(path + fName);
				
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				@SuppressWarnings("resource")
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				Row headerRow = mySheet.createRow(0);
				Cell headerCell = null;
				
				String headers="";
				headers="LOCALE,DOCUMENT_ID,OPERATION_TYPE,PROCESSING_STATUS,FETCHED_VERSION,MODIFIED_VERSION,PUBLISHED_STATUS,ERROR_CODES,ERROR_MESSAGES";
				
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
				ContentDetails details = null;
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < transactionList.size(); a++) 
				{
					details = (ContentDetails)transactionList.get(a);
					
					dataRow=details.getLocale()+"<TOK_SEPARATOR>"+details.getDocumentId();
					dataRow+="<TOK_SEPARATOR>"+AccessManagementInterface.OPERATION_TYPE_IMPORT+"<TOK_SEPARATOR>"+details.getProcessingStatus()+"<TOK_SEPARATOR>"+details.getFetchedVersion()+
							"<TOK_SEPARATOR>"+details.getModifiedVersion()+"<TOK_SEPARATOR>"+details.getPublishStatus()+"<TOK_SEPARATOR>"+details.getErrorCode()+"<TOK_SEPARATOR>"+details.getErrorMessage();
					
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
			Utilities.printStackTraceToLogs(TranslationUpdateReportsUtil.class.getName(), "printImportTransactionReport()", e);
		}
	}


}
