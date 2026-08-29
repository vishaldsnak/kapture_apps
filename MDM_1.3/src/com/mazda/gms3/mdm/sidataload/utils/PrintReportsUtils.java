package com.mazda.gms3.mdm.sidataload.utils;

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
import com.mazda.gms3.mdm.sidataload.vo.SIChannelErrorsDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelImageDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelSchemaDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;

public class PrintReportsUtils {

	private Logger logger = LogManager.getLogger(PrintReportsUtils.class);
	
	public void printTransactionReport(SIChannelSchemaDetails contentDetails, String scheduleId)
	{
		try
		{
			String reportsDirPath = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			File repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add reports folder
			reportsDirPath+=ApplicationProperties.getProperty("sichannel.data.load.reports.dir");
			repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add schedule id Dir
			reportsDirPath+=scheduleId;
			repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// ADD File Name
			String fName=scheduleId+"_TRANSACTION_REPORT.xlsx";
			File myFile = new File(reportsDirPath + fName);
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
			
			String headers="Locale,DocumentId,DocumentType,Fetched Version,Modified Version,Document Status,Processing Status,Error Codes,Error Messages";
			String[] tokens= headers.split(",");
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
			Row row=null;
			Cell dataCell=null;
			if(null!=contentDetails.getErrorsList() && contentDetails.getErrorsList().size()>0)
			{
				// FAILURES
				SIChannelErrorsDetails errorDetails = null;
				for(int a=0;a<contentDetails.getErrorsList().size();a++)
				{
					errorDetails = (SIChannelErrorsDetails)contentDetails.getErrorsList().get(a);
					dataRow=contentDetails.getLocale()+"<TOK_SEPARATOR>"+contentDetails.getDocumentId()+"<TOK_SEPARATOR>"+contentDetails.getDocumentType();
					dataRow+="<TOK_SEPARATOR>"+contentDetails.getFetchedVersion()+"<TOK_SEPARATOR>"+contentDetails.getModifiedVersion()+"<TOK_SEPARATOR>";
					dataRow+=contentDetails.getPublishStatus()+"<TOK_SEPARATOR>"+contentDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+errorDetails.getErrorCode();
					dataRow+="<TOK_SEPARATOR>"+errorDetails.getErrorMessage();
					
					
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
								setCellValue(dataCell, tokens[e].trim());
							}
							dataCell =null;
						}
					}
					tokens = null;
					row=null;
					dataRow = null;
					dataCell = null;
					
					errorDetails = null;
				}
			}
			else
			{
				// ALL SUCCESS 
				
				dataRow=contentDetails.getLocale()+"<TOK_SEPARATOR>"+contentDetails.getDocumentId()+"<TOK_SEPARATOR>"+contentDetails.getDocumentType();
				dataRow+="<TOK_SEPARATOR>"+contentDetails.getFetchedVersion()+"<TOK_SEPARATOR>"+contentDetails.getModifiedVersion()+"<TOK_SEPARATOR>";
				dataRow+=contentDetails.getPublishStatus()+"<TOK_SEPARATOR>"+contentDetails.getProcessingStatus();
				
				
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
							setCellValue(dataCell, tokens[e].trim());
						}
						dataCell =null;
					}
				}
				tokens = null;
				row=null;
				dataRow = null;
				dataCell = null;
			}
			
			headerRow = null;

			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
			os.flush();
			os.close();
			
			dataCell = null;
			row = null;
			dataRow = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printTransactionReport()", e);
		}
	}
	
	public void printFileProcessingReport(List<SIChannelImageDetails> imagesList, String scheduleId)
	{
		try
		{
			String reportsDirPath = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			File repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add reports folder
			reportsDirPath+=ApplicationProperties.getProperty("sichannel.data.load.reports.dir");
			repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add schedule id Dir
			reportsDirPath+=scheduleId;
			repDir = new File(reportsDirPath);
			if(repDir.exists()==false || repDir.isDirectory()==false)
			{
				repDir.mkdir();
			}
			repDir = null;
			
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// ADD File Name
			String fName=scheduleId+"_OKASSETS_REPORT.xlsx";
			File myFile = new File(reportsDirPath + fName);
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
			
			String headers="Image Name,Image Tag Src Value,Source Physical Path,Destination Relative Path,Destination Physical Path,Processing Status,Error Codes,Error Messages";
			String[] tokens= headers.split(",");
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
			Row row=null;
			Cell dataCell=null;
			if(null!=imagesList && imagesList.size()>0)
			{
				// FAILURES
				SIChannelImageDetails imageDetails = null;
				for(int a=0;a<imagesList.size();a++)
				{
					imageDetails = (SIChannelImageDetails)imagesList.get(a);
					dataRow=imageDetails.getImageName()+"<TOK_SEPARATOR>"+imageDetails.getSrcValue()+"<TOK_SEPARATOR>"+imageDetails.getImageSourcePath();
					dataRow+="<TOK_SEPARATOR>"+imageDetails.getDestinationRelativePath()+"<TOK_SEPARATOR>"+imageDetails.getDestinationPath()+"<TOK_SEPARATOR>";
					dataRow+=imageDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+imageDetails.getErrorCode()+"<TOK_SEPARATOR>"+imageDetails.getErrorMessage();
					
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
								setCellValue(dataCell, tokens[e].trim());
							}
							dataCell =null;
						}
					}
					tokens = null;
					row=null;
					dataRow = null;
					dataCell = null;
					
					imageDetails = null;
				}
			}
			
			headerRow = null;

			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
			os.flush();
			os.close();
			
			dataCell = null;
			row = null;
			dataRow = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "printFileProcessingReport()", e);
		}
	}
	
	
	/**
	 * WRITES A CELL, CUT TO WHAT A SPREADSHEET CELL CAN ACTUALLY HOLD.
	 *
	 * THE ERROR MESSAGE COLUMN CARRIES WHOLE API RESPONSES. When a Kapture call fails, the message
	 * tracked against the schedule is "http <status> :: <the entire response body>" - deliberately
	 * not trimmed, because a validation failure names the offending field a long way into the body.
	 * Those run to thousands of characters.
	 *
	 * WHY THIS MATTERS MORE THAN A TIDY CELL: Excel's hard limit is 32767 characters and POI THROWS
	 * on a longer value. The throw lands in printTransactionReport's catch, so the workbook is never
	 * written at all - one oversized message would have cost the user EVERY error in the report,
	 * and the report is the only place the API's reason survives for them to read.
	 *
	 * The value is cut rather than dropped, and says so, so nobody reads a truncated message as the
	 * whole story. The full text is in the log either way.
	 */
	private void setCellValue(Cell cell, String value)
	{
		if(null==cell)
		{
			return;
		}
		if(null==value)
		{
			cell.setCellValue("");
			return;
		}
		if(value.length()<=EXCEL_MAX_CELL_LENGTH)
		{
			cell.setCellValue(value);
			return;
		}
		String notice = " ... [truncated - see the application log for the full message]";
		cell.setCellValue(value.substring(0, EXCEL_MAX_CELL_LENGTH - notice.length()) + notice);
		logger.info("setCellValue :: a value of "+ value.length()
				+" characters was cut to the spreadsheet cell limit.");
	}

	/** Excel's hard maximum for the characters in one cell - POI throws above it. */
	private static final int EXCEL_MAX_CELL_LENGTH = 32767;

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
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(PrintReportsUtils.class.getName(), "createReportsZip()", e);
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
	private void addToZipFile(File childFile, ZipOutputStream zos) throws FileNotFoundException, IOException 
	{
		logger.info("addToZipFile :: Writing '" + childFile.getName() + "' to zip file");
		FileInputStream fis = new FileInputStream(childFile);
		zos.putNextEntry(new ZipEntry(childFile.getName()));
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) 
		{
			zos.write(bytes, 0, length);
		}
		zos.closeEntry();
		fis.close();
	}
}
