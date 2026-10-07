package com.mazda.gms3.dmt.autosync.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
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

import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;

public class PrintReportUtils {

	private Logger logger = LogManager.getLogger(PrintReportUtils.class);
	
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
			Utilities.printStackTraceToLogs(PrintReportUtils.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(PrintReportUtils.class.getName(), "createReportsZip()", e);
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

	
	@SuppressWarnings("resource")
	public void printTransactionReport(List<AutoSyncCategoryDetails> categoryList, String scheduleId, int listCount)
	{
		try
		{
			if(null!=categoryList && categoryList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");

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
				path = path + "/" + scheduleId;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				
				String fName = "/CATEGORY_TRANSACTIONS_"+listCount+".xlsx";
				
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
				
				String headers="Market,Master Data Type,Locale,Processing Status,Level 1 Name,Level 1 RefKey,Level 2 Name,Level 2 RefKey,Level 3 Name,Level 3RefKey,Level 4 Name,Level 4RefKey,Level 5 Name,Level 5 RefKey,Remarks";
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
				 */
				String dataRow="";
				AutoSyncCategoryDetails catDetails = null;
				
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < categoryList.size(); a++) {
					catDetails = (AutoSyncCategoryDetails) categoryList.get(a);

//					dataRow=catDetails.getMarket()+"<TOK_SEPARATOR>"+catDetails.getItemType()+"<TOK_SEPARATOR>";
//					dataRow+=catDetails.getLocale()+"<TOK_SEPARATOR>"+catDetails.getCategoryRefKey()+"<TOK_SEPARATOR>";
//					dataRow+=catDetails.getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getParentRefKey()+"<TOK_SEPARATOR>"+catDetails.getProcessingStatus();
					
					dataRow=catDetails.getMarket()+"<TOK_SEPARATOR>"+catDetails.getItemType()+"<TOK_SEPARATOR>";
					dataRow+=catDetails.getLocale()+"<TOK_SEPARATOR>"+catDetails.getProcessingStatus()+"<TOK_SEPARATOR>";
					
					if(null!=catDetails.getLevel1Details())
					{
						dataRow+=catDetails.getLevel1Details().getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getLevel1Details().getCategoryRefKey()+"<TOK_SEPARATOR>";
					}
					else
					{
						dataRow+="<TOK_SEPARATOR><TOK_SEPARATOR>";
					}
					
					if(null!=catDetails.getLevel2Details())
					{
						dataRow+=catDetails.getLevel2Details().getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getLevel2Details().getCategoryRefKey()+"<TOK_SEPARATOR>";
					}
					else
					{
						dataRow+="<TOK_SEPARATOR><TOK_SEPARATOR>";
					}
					
					if(null!=catDetails.getLevel3Details())
					{
						dataRow+=catDetails.getLevel3Details().getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getLevel3Details().getCategoryRefKey()+"<TOK_SEPARATOR>";
					}
					else
					{
						dataRow+="<TOK_SEPARATOR><TOK_SEPARATOR>";
					}
					
					if(null!=catDetails.getLevel4Details())
					{
						dataRow+=catDetails.getLevel4Details().getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getLevel4Details().getCategoryRefKey()+"<TOK_SEPARATOR>";
					}
					else
					{
						dataRow+="<TOK_SEPARATOR><TOK_SEPARATOR>";
					}
					
					if(null!=catDetails.getLevel5Details())
					{
						dataRow+=catDetails.getLevel5Details().getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getLevel5Details().getCategoryRefKey()+"<TOK_SEPARATOR>";
					}
					else
					{
						dataRow+="<TOK_SEPARATOR><TOK_SEPARATOR>";
					}
					
					dataRow+=catDetails.getErrorMessage();
					
					
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
					
					catDetails = null;
				}

				headerRow = null;
				
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on CATEGORY TRANSACTION {"+listCount+"} REPORT XLSX file Finished ...");
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
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(PrintReportUtils.class.getName(), "printTransactionReport()", e);
		}
	}

	@SuppressWarnings("resource")
	public void printFailureReport(List<AutoSyncCategoryDetails> categoryList, String scheduleId, int listCount)
	{
		try
		{
			if(null!=categoryList && categoryList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");

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
				path = path + "/" + scheduleId;
				File scheduleDirectory = PathUtil.file(path);
				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
				{
					scheduleDirectory.mkdir();
				}
				scheduleDirectory = null;

				
				String fName = "/CATEGORY_FAILURES_"+listCount+".xlsx";
				
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
				
				String headers="Market,Master Data Type,Locale,Category Level,Category Ref Key,Category Name,Parent Ref Key,Operation Type,Error Code,Error Message";
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
				 */
				String dataRow="";
				AutoSyncCategoryDetails catDetails = null;
				
				Row row=null;
				Cell dataCell=null;
				
				for (int a = 0; a < categoryList.size(); a++) {
					catDetails = (AutoSyncCategoryDetails) categoryList.get(a);

					dataRow=catDetails.getMarket()+"<TOK_SEPARATOR>"+catDetails.getItemType()+"<TOK_SEPARATOR>";
					dataRow+=catDetails.getLocale()+"<TOK_SEPARATOR>"+catDetails.getCategoryLevel()+"<TOK_SEPARATOR>"+catDetails.getCategoryRefKey()+"<TOK_SEPARATOR>";
					dataRow+=catDetails.getCategoryName()+"<TOK_SEPARATOR>"+catDetails.getParentRefKey()+"<TOK_SEPARATOR>"+catDetails.getOperationType()+"<TOK_SEPARATOR>";
					dataRow+=catDetails.getErrorCode()+"<TOK_SEPARATOR>"+catDetails.getErrorMessage();
					
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
					
					catDetails = null;
				}

				headerRow = null;
				
				FileOutputStream os = PathUtil.fileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on CATEGORY FAILURE {"+listCount+"} REPORT XLSX file Finished ...");
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
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(PrintReportUtils.class.getName(), "printFailureReport()", e);
		}
	}

}
