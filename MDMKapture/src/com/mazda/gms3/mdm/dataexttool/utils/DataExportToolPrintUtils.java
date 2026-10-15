package com.mazda.gms3.mdm.dataexttool.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportTransactionDAO;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolAttachmentDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolInnerLinkDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class DataExportToolPrintUtils {

	private Logger logger = LogManager.getLogger(DataExportToolPrintUtils.class);

	List<String> filesListInDir = new ArrayList<String>();

	/**
	 * This method zips the directory
	 * @param dir
	 * @param zipDirName
	 */
	private void zipDirectory(File dir, String zipDirName) 
	{
		try 
		{
			populateFilesList(dir);
			//now zip files one by one
			//create ZipOutputStream to write to the zip file
			FileOutputStream fos = new FileOutputStream(zipDirName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			for(String filePath : filesListInDir){
				//				System.out.println("Zipping "+filePath);
				//for ZipEntry we need to keep only relative file path, so we used substring on absolute path
				ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
				zos.putNextEntry(ze);
				//read the file and write to ZipOutputStream
				FileInputStream fis = new FileInputStream(filePath);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				fis.close();
				filePath = null;
			}
			zos.close();
			fos.close();
		} 
		catch (IOException e) {
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "zipDirectory()", e);
		}
		catch (Exception e) {
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "zipDirectory()", e);
		}
	}

	/**
	 * This method populates all the files in a directory to a List
	 * @param dir
	 * @throws IOException
	 */
	private void populateFilesList(File dir) throws IOException {
		File[] files = dir.listFiles();
		for(File file : files)
		{
			if(file.isFile()) 
			{
				filesListInDir.add(file.getAbsolutePath());
			}
			else 
			{
				populateFilesList(file);
			}
		}
	}


	/**
	 * Function will proceed for generating reports.
	 * 
	 * @param wslId
	 * @param scheduleCode
	 */
	public boolean createWorkingDirZip(String workingDirectoryPath,String scheduleCode,String scheduleName) 
	{
		try
		{
			/*
			 * Path for the ZIP File will be same as workingDirectoryPath and
			 * name will be - schDate_REPOROTS.ZIP
			 */
			if (null != workingDirectoryPath && !"".equals(workingDirectoryPath) && null != scheduleCode && !"".equals(scheduleCode)) 
			{
				String zipPath = workingDirectoryPath	+ scheduleName+"_XMLS"	+ ApplicationProperties.getProperty("ZIP_SUFFIX");

				File file = new File(workingDirectoryPath);
				if (file.exists() && file.isDirectory()) 
				{
					zipDirectory(file, zipPath);
				}
				file = null;

				/*
				 * Now check here, if the Zip file is generated, then check for
				 * all the other files and delete them inside the directory
				 */
				File zip = new File(zipPath);
				if (zip.exists() && zip.length() > 0) 
				{
					logger.info("createWorkingDirZip :: Zip file {"+ scheduleName+ "_XMLS"+ ApplicationProperties.getProperty("ZIP_SUFFIX")+ "} Generated Successfully");
					/*
					 * Proceed for deleting all the other files
					 */
					File xmlFiles = new File(workingDirectoryPath);
					if (xmlFiles.exists() && xmlFiles.isDirectory()) 
					{
						File[] childFiles = xmlFiles.listFiles();
						for (int a = 0; a < childFiles.length; a++) 
						{
							File excelReports = childFiles[a];
							// file or directory delete all
							if (excelReports.exists() && !excelReports.getName().contains(ApplicationProperties.getProperty("ZIP_SUFFIX"))) 
							{
								deleteDir(excelReports);
							}
							excelReports = null;
						}
						childFiles = null;
					}
					xmlFiles = null;
				} 
				else 
				{
					logger.info("createWorkingDirZip :: Failed to generate Zip file {"+ scheduleName+ "_XMLS"+ ApplicationProperties.getProperty("ZIP_SUFFIX")+ "}. Download the XML files manually.");
					return false;
				}
				zipPath = null;
			} 
			else 
			{
				logger.info("createWorkingDirZip :: Reports Directory Path and Scheduled Name is passed as null in parameters. Return false");
				return false;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "createWorkingDirZip()", e);
			logger.info("createWorkingDirZip :: Exception :: >"	+ e.getMessage());
			return false;
		} 
		return true;
	}


	private void deleteDir(File directory)
	{
		try
		{
			if(directory.isDirectory())
			{
				if(directory.listFiles().length==0)
				{
					// delete dir
					directory.delete();
				}
				else
				{
					File[] files = directory.listFiles();
					if(null!=files && files.length>0)
					{
						for(int a=0;a<files.length;a++)
						{
							if(files[a].isDirectory())
							{
								deleteDir(files[a]);
							}
							else
							{
								// delete file
								files[a].delete();
							}
						}
					}
					files = null;

					//check the directory again, if empty then delete it
					if(directory.list().length==0){
						directory.delete();
					}
				}
			}
			else
			{
				// if file
				directory.delete();
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "createWorkingDirZip()", e);
		}
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

	public void printTransactionReport(ArrayList<DataExportToolItemDetails> transactionList, String scheduleCode, DataExportTransactionDAO transactionDAO)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=transactionList	&& transactionList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("dataexttool.reports.folder.path");

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
				String fName = "/" + scheduleCode+"_DOCUMENT_EXTRACTION_REPORT.xlsx";

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
//				headers="SCHEDULE_ID,LOCALE,MODEL,CARLINE CODE,CHANNEL,DOCUMENT TYPE,DOCUMENT ID,CHANNEL SOURCE DIR,DESTINATION DOCUMENT DIR,PROCESSING STATUS,ERROR MESSAGE";
				headers="SCHEDULE_ID,LOCALE,MODEL,CHANNEL,DOCUMENT TYPE,DOCUMENT ID,CHANNEL SOURCE DIR,DESTINATION DOCUMENT DIR,PROCESSING STATUS,ERROR MESSAGE";
				
				
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
				DataExportToolItemDetails details = null;
				Row row=null;
				Cell dataCell=null;
				String modelName=null;
				long totalCount=transactionList.size();
				long successCount=0;
				long failureCount=0;
				String reportStatus=ScheduleConstants.STATUS_SUCCESS;
				for (int a = 0; a < transactionList.size(); a++) 
				{
					details = (DataExportToolItemDetails)transactionList.get(a);

					modelName= details.getModel();
					/*
					 * restore _ in ModelName by /
					 * for printing correct name in reports
					 */
					modelName = modelName.replace("_", "/");
					dataRow=String.valueOf(details.getScheduleId())+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>"+modelName;
//					dataRow+="<TOK_SEPARATOR>"+details.getCarlineCode()+"<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
//							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+details.getChannelFolderPath()+"<TOK_SEPARATOR>"+details.getDocumentDirPath()+"<TOK_SEPARATOR>"+details.getProcessingStatus()+"<TOK_SEPARATOR>"+details.getErrorMessage();
					
					dataRow+="<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+details.getChannelFolderPath()+"<TOK_SEPARATOR>"+details.getDocumentDirPath()+"<TOK_SEPARATOR>"+details.getProcessingStatus()+"<TOK_SEPARATOR>"+details.getErrorMessage();

					if(null!=details.getProcessingStatus() && details.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
					{
						// increment successCount
						successCount++;
					}
					else
					{
						// increment failureCount
						failureCount++;
						// set reportStatus as Failure
						reportStatus  = ScheduleConstants.STATUS_FAILURE;
					}
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
					modelName=  null;
				}
				headerRow = null;

				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				/*
				 * CREATE REPORT SCHEDULE DETAILS & SAVE IN DATABASE
				 */
				DataExportToolReportSummaryDetails summaryDetails = new DataExportToolReportSummaryDetails();
				summaryDetails.setScheduleId(scheduleCode);
				summaryDetails.setReportName("DOCUMENT EXTRACTION REPORT");
				summaryDetails.setTotalCount(totalCount);
				summaryDetails.setSuccessCount(successCount);
				summaryDetails.setFailureCount(failureCount);
				summaryDetails.setReportStatus(reportStatus);
				// INSERT REPORT SUMMARY
				transactionDAO.createReportSummary(summaryDetails);

				summaryDetails = null;
				totalCount = 0;
				successCount=0;
				failureCount=0;
				reportStatus =null;
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
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printTransactionReport()", e);
		}
	}

	public void printFilesTransactionReport(ArrayList<DataExportToolAttachmentDetails> transactionList, String scheduleCode, DataExportTransactionDAO transactionDAO)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=transactionList	&& transactionList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("dataexttool.reports.folder.path");

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
				String fName = "/" + scheduleCode+"_FILES_TRANSACTION_REPORT.xlsx";

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
//				headers="SCHEDULE_ID,LOCALE,MODEL,CARLINE CODE,CHANNEL,DOCUMENT TYPE,DOCUMENT ID,FILE NAME,FILE TYPE,SOURCE PATH,DESTINATION DIRECTORY,PROCESSING STATUS,ERROR MESSAGE";
				headers="SCHEDULE_ID,LOCALE,MODEL,CHANNEL,DOCUMENT TYPE,DOCUMENT ID,FILE NAME,FILE TYPE,SOURCE PATH,DESTINATION DIRECTORY,PROCESSING STATUS,ERROR MESSAGE";

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
				DataExportToolItemDetails details = null;
				Row row=null;
				Cell dataCell=null;
				DataExportToolAttachmentDetails attachmentDetails = null;
				String modelName=null;
				long totalCount=transactionList.size();
				long successCount=0;
				long failureCount=0;
				String reportStatus=ScheduleConstants.STATUS_SUCCESS;
				for (int a = 0; a < transactionList.size(); a++) 
				{
					attachmentDetails = (DataExportToolAttachmentDetails)transactionList.get(a);
					details = new DataExportToolItemDetails();
					if(null!=attachmentDetails.getItemDetails())
					{
						details = attachmentDetails.getItemDetails();
					}

					modelName= details.getModel();
					/*
					 * restore _ in ModelName by /
					 * for printing correct name in reports
					 */
					modelName = modelName.replace("_", "/");

					dataRow=String.valueOf(details.getScheduleId())+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>"+modelName;
//					dataRow+="<TOK_SEPARATOR>"+details.getCarlineCode()+"<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
//							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+attachmentDetails.getName()+"<TOK_SEPARATOR>"+attachmentDetails.getAttachmentType()+"<TOK_SEPARATOR>"+attachmentDetails.getSourcePath()+"<TOK_SEPARATOR>"+attachmentDetails.getDestinationPath()+"<TOK_SEPARATOR>"+attachmentDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+attachmentDetails.getErrorMessage();
					dataRow+="<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+attachmentDetails.getName()+"<TOK_SEPARATOR>"+attachmentDetails.getAttachmentType()+"<TOK_SEPARATOR>"+attachmentDetails.getSourcePath()+"<TOK_SEPARATOR>"+attachmentDetails.getDestinationPath()+"<TOK_SEPARATOR>"+attachmentDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+attachmentDetails.getErrorMessage();

					if(null!=attachmentDetails.getProcessingStatus() && attachmentDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
					{
						// increment successCount
						successCount++;
					}
					else
					{
						// increment failureCount
						failureCount++;
						// set reportStatus as Failure
						reportStatus  = ScheduleConstants.STATUS_FAILURE;
					}

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
					modelName = null;
				}
				headerRow = null;

				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				/*
				 * CREATE REPORT SCHEDULE DETAILS & SAVE IN DATABASE
				 */
				DataExportToolReportSummaryDetails summaryDetails = new DataExportToolReportSummaryDetails();
				summaryDetails.setScheduleId(scheduleCode);
				summaryDetails.setReportName("FILES TRANSACTION REPORT");
				summaryDetails.setTotalCount(totalCount);
				summaryDetails.setSuccessCount(successCount);
				summaryDetails.setFailureCount(failureCount);
				summaryDetails.setReportStatus(reportStatus);
				// INSERT REPORT SUMMARY
				transactionDAO.createReportSummary(summaryDetails);

				summaryDetails = null;
				totalCount = 0;
				successCount=0;
				failureCount=0;
				reportStatus =null;


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
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printFilesTransactionReport()", e);
		}
	}

	public void printInnerLinkTransactionReport(ArrayList<DataExportToolInnerLinkDetails> innerLinkTransactionList, String scheduleCode, DataExportTransactionDAO transactionDAO)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=innerLinkTransactionList	&& innerLinkTransactionList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				String path = ApplicationProperties.getProperty("dataexttool.reports.folder.path");

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
				String fName = "/" + scheduleCode+"_INNERLINKS_TRANSACTION_REPORT.xlsx";

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
//				headers="SCHEDULE_ID,LOCALE,MODEL,CARLINE CODE,CHANNEL,DOCUMENT TYPE,DOCUMENT id,DOCUMENT PATH IN EXPORTED DIR,INNERLINK DOCUMENT id,INNERLINK SOURCE PATH,INNERLINK DESTINATION PATH,PROCESSING STATUS,ERROR MESSAGE";
				headers="SCHEDULE_ID,LOCALE,MODEL,CHANNEL,DOCUMENT TYPE,DOCUMENT id,DOCUMENT PATH IN EXPORTED DIR,INNERLINK DOCUMENT id,INNERLINK SOURCE PATH,INNERLINK DESTINATION PATH,PROCESSING STATUS,ERROR MESSAGE";
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
				DataExportToolItemDetails details = null;
				DataExportToolInnerLinkDetails innerLinkDetails = null;
				Row row=null;
				Cell dataCell=null;
				String documentPath=null;
				String localeDir = null;
				String modelName=null;
				long totalCount=innerLinkTransactionList.size();
				long successCount=0;
				long failureCount=0;
				String reportStatus=ScheduleConstants.STATUS_SUCCESS;
				for (int a = 0; a < innerLinkTransactionList.size(); a++) 
				{
					innerLinkDetails = (DataExportToolInnerLinkDetails)innerLinkTransactionList.get(a);
					details = new DataExportToolItemDetails();
					if(null!=innerLinkDetails.getItemDetails())
					{
						details = innerLinkDetails.getItemDetails();
					}

					localeDir= details.getLocale();
					localeDir = localeDir.replace("-", "_");
					documentPath = details.getDocumentDirPath();
					if(documentPath.indexOf(localeDir)!=-1)
					{
						documentPath = documentPath.substring(documentPath.indexOf(localeDir),documentPath.length());
					}

					modelName= details.getModel();
					/*
					 * restore _ in ModelName by /
					 * for printing correct name in reports
					 */
					modelName = modelName.replace("_", "/");

					dataRow=String.valueOf(details.getScheduleId())+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>"+modelName;
//					dataRow+="<TOK_SEPARATOR>"+details.getCarlineCode()+"<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
//							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+documentPath+"<TOK_SEPARATOR>"+innerLinkDetails.getInnerLinkDocumentId()
//							+"<TOK_SEPARATOR>"+innerLinkDetails.getSrcHrefPath()+"<TOK_SEPARATOR>"+innerLinkDetails.getPathToBeReplaced()
//							+"<TOK_SEPARATOR>"+innerLinkDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+innerLinkDetails.getErrorMessage();
					
					dataRow+="<TOK_SEPARATOR>"+details.getChannelName()+"<TOK_SEPARATOR>"+details.getDocumentType()+
							"<TOK_SEPARATOR>"+details.getDocumentId()+"<TOK_SEPARATOR>"+documentPath+"<TOK_SEPARATOR>"+innerLinkDetails.getInnerLinkDocumentId()
							+"<TOK_SEPARATOR>"+innerLinkDetails.getSrcHrefPath()+"<TOK_SEPARATOR>"+innerLinkDetails.getPathToBeReplaced()
							+"<TOK_SEPARATOR>"+innerLinkDetails.getProcessingStatus()+"<TOK_SEPARATOR>"+innerLinkDetails.getErrorMessage();

					if(null!=innerLinkDetails.getProcessingStatus() && 
							(innerLinkDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS) || 
									innerLinkDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SKIPPED)))
					{
						// increment successCount
						successCount++;
					}
					else
					{
						// increment failureCount
						failureCount++;
						// set reportStatus as Failure
						reportStatus  = ScheduleConstants.STATUS_FAILURE;
					}

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
					innerLinkDetails = null;
					documentPath = null;
					localeDir = null;
					modelName= null;
				}
				headerRow = null;

				FileOutputStream os = new FileOutputStream(myFile);
				myWorkBook.write(os);
				logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
				os.flush();
				os.close();

				/*
				 * CREATE REPORT SCHEDULE DETAILS & SAVE IN DATABASE
				 */
				DataExportToolReportSummaryDetails summaryDetails = new DataExportToolReportSummaryDetails();
				summaryDetails.setScheduleId(scheduleCode);
				summaryDetails.setReportName("INNERLINKS TRANSACTION REPORT");
				summaryDetails.setTotalCount(totalCount);
				summaryDetails.setSuccessCount(successCount);
				summaryDetails.setFailureCount(failureCount);
				summaryDetails.setReportStatus(reportStatus);
				// INSERT REPORT SUMMARY
				transactionDAO.createReportSummary(summaryDetails);

				summaryDetails = null;
				totalCount = 0;
				successCount=0;
				failureCount=0;
				reportStatus =null;

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
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printInnerLinkTransactionReport()", e);
		}
	}


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
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: Input Output Found Exception :: >"
					+ e.getMessage());
			return false;
		}
		return true;
	}

	public void printSIChannelViewContentVINReport(List<MNAOViewContentDetails> viewContentList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=viewContentList	&& viewContentList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
//				File wslDirectory = new File(path);
//				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
//					// create new directory
//					wslDirectory.mkdir();
//				}
//				wslDirectory = null;
//				/*
//				 * Now check, whether the schedule code directory exists or
//				 * not
//				 */
//				if(!path.endsWith("/"))
//				{
//					path = path+"/";
//				}
//				path = path + scheduleCode;
//				File scheduleDirectory = new File(path);
//				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
//				{
//					scheduleDirectory.mkdir();
//				}
//				scheduleDirectory = null;

				File scheduleDirectory  = null;
				String tPath = null;
				MNAOViewContentDetails hierarchyDetails = null;
				MNAOViewContentDetails details = null;
				String fName = null;
				File myFile = null;
				SXSSFWorkbook myWorkBook = null;
				Sheet mySheet = null;
				Row headerRow = null;
				Cell headerCell = null;
				String headers="";
				String[] tokens=null;
				int rowCount = 0;
				FileOutputStream os = null;
				for(int Z=0;Z<viewContentList.size();Z++)
				{
					hierarchyDetails = (MNAOViewContentDetails)viewContentList.get(Z);
					tPath = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//					tPath = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					if(!tPath.endsWith("/"))
					{
						tPath = tPath+"/";
					}
					tPath = tPath + scheduleCode;
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//					// add Locale
					tPath=tPath+"/"+hierarchyDetails.getLocale();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//
//					// add model 
//					tPath=tPath+"/"+hierarchyDetails.getModelName()+" "+hierarchyDetails.getCarlineCode();
					tPath=tPath+"/"+hierarchyDetails.getProcessingModelForMNAODataExport();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * Prepare File name
					 * scheduleCode_LOCALE+MODEL_SI_VC_VIN_DETAILS.xlsx
					 */
//					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getModelName().toUpperCase()+"_"+hierarchyDetails.getCarlineCode().toUpperCase()+"_SI_VC_DETAILS.xlsx";
					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getProcessingModelForMNAODataExport().toUpperCase()+"_SI_VC_VIN_DETAILS.xlsx";

					myFile = new File(tPath+"/" +fName);
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					myWorkBook = new SXSSFWorkbook(100);
					// Create a new sheet
					mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					headerRow = mySheet.createRow(0);
					headerCell = null;
					headers="DOCUMENT_ID,LOCALE,CHANNEL_NAME,DOCUMENT_TYPE,DOCUMENT_TYPE_NAME,DOCUMENT_SUB_TYPE,DOCUMENT_SUB_TYPE_NAME,WMI_CODE,"
							+ "VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,ESI_CATEGORY_NAME_1,ESI_CATEGORY_NAME_2,ESI_CATEGORY_NAME_3,TITLE,IM_DOC_MODIFIED_DATE,SI_NUMBER";

					tokens=headers.split(",");
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

					rowCount = 0;
					os = null;
					if(null!=hierarchyDetails.getViewContentList() && hierarchyDetails.getViewContentList().size()>0)
					{
						details = null;
						String dataRow="";
						Row row=null;
						Cell dataCell=null;
						tokens = null;
						for(int b=0;b<hierarchyDetails.getViewContentList().size();b++)
						{
							details = (MNAOViewContentDetails)hierarchyDetails.getViewContentList().get(b);
							if(details.getViewContentType().equals("VIN"))
							{
								dataRow=details.getDocumentId()+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>";
								if(details.getDocumentId().startsWith("SI"))
								{
									dataRow+="SERVICE_INFORMATION<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("TR"))
								{
									dataRow+="TRAINING<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("VI"))
								{
									dataRow+="VIDEOS<TOK_SEPARATOR>";
								}
								dataRow+=details.getDocumentType();
								dataRow+="<TOK_SEPARATOR>"+details.getDocumentTypeName()+"<TOK_SEPARATOR>"+details.getDocumentSubType()+"<TOK_SEPARATOR>"+details.getDocumentSubTypeName();
								dataRow+="<TOK_SEPARATOR>"+details.getWmiCode()+
										"<TOK_SEPARATOR>"+details.getVdsCode()+"<TOK_SEPARATOR>"+details.getVisStartRange()+"<TOK_SEPARATOR>"+details.getVisEndRange()+
										"<TOK_SEPARATOR>"+details.getCatNameLevel1()+"<TOK_SEPARATOR>"+details.getCatNameLevel2();
								dataRow+="<TOK_SEPARATOR>"+details.getCatNameLevel3()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>"+details.getImDocLastModifiedDate()+
										"<TOK_SEPARATOR>"+details.getSiNumber();

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
							}
							tokens = null;
							details = null;
							dataRow = null;
							dataCell = null;
							row = null;
						}
						os = new FileOutputStream(myFile);
						myWorkBook.write(os);
						logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
						os.flush();
						os.close();
						os = null;
					}
					hierarchyDetails = null;
					// restore tPath Value
					tPath = null;
					fName = null;
					myFile = null;
					mySheet = null;
					myWorkBook = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printSIChannelViewContentModelYearReport()", e);
		}
	}

	public void printSIChannelViewContentModelYearReport(List<MNAOViewContentDetails> viewContentList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=viewContentList	&& viewContentList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
				/*
				 * Now check inside the reports directory does a directory
				 * exists for wslId
				 */
//				File wslDirectory = new File(path);
//				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
//					// create new directory
//					wslDirectory.mkdir();
//				}
//				wslDirectory = null;
//				/*
//				 * Now check, whether the schedule code directory exists or
//				 * not
//				 */
//				if(!path.endsWith("/"))
//				{
//					path = path+"/";
//				}
//				path = path + scheduleCode;
//				File scheduleDirectory = new File(path);
//				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
//				{
//					scheduleDirectory.mkdir();
//				}
//				scheduleDirectory = null;

				File scheduleDirectory  = null;
				String tPath = null;
				MNAOViewContentDetails hierarchyDetails = null;
				MNAOViewContentDetails details = null;
				String fName = null;
				File myFile = null;
				SXSSFWorkbook myWorkBook = null;
				Sheet mySheet = null;
				Row headerRow = null;
				Cell headerCell = null;
				String headers="";
				String[] tokens=null;
				int rowCount = 0;
				FileOutputStream os = null;
				for(int Z=0;Z<viewContentList.size();Z++)
				{
					hierarchyDetails = (MNAOViewContentDetails)viewContentList.get(Z);
					tPath = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//					tPath = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
					/*
					 * Now check, whether the schedule code directory exists or
					 * not
					 */
					if(!tPath.endsWith("/"))
					{
						tPath = tPath+"/";
					}
					tPath = tPath + scheduleCode;
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//					// add Locale
					tPath=tPath+"/"+hierarchyDetails.getLocale();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//
//					// add model 
//					tPath=tPath+"/"+hierarchyDetails.getModelName()+" "+hierarchyDetails.getCarlineCode();
					tPath=tPath+"/"+hierarchyDetails.getProcessingModelForMNAODataExport();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * Prepare File name
					 * scheduleCode_LOCALE+MODEL_SI_VC_MODEL_YEAR_DETAILS.xlsx
					 */
//					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getModelName().toUpperCase()+"_"+hierarchyDetails.getCarlineCode().toUpperCase()+"_SI_VC_DETAILS.xlsx";
					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getProcessingModelForMNAODataExport().toUpperCase()+"_SI_VC_MODEL_YEAR_DETAILS.xlsx";

					myFile = new File(tPath+"/" +fName);
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					myWorkBook = new SXSSFWorkbook(100);
					// Create a new sheet
					mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					headerRow = mySheet.createRow(0);
					headerCell = null;
					headers="DOCUMENT_ID,LOCALE,CHANNEL_NAME,DOCUMENT_TYPE,DOCUMENT_TYPE_NAME,DOCUMENT_SUB_TYPE,DOCUMENT_SUB_TYPE_NAME,MODEL,YEAR,"
							+ "ESI_CATEGORY_NAME_1,ESI_CATEGORY_NAME_2,ESI_CATEGORY_NAME_3,TITLE,IM_DOC_MODIFIED_DATE,SI_NUMBER";

					tokens=headers.split(",");
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

					rowCount = 0;
					os = null;
					if(null!=hierarchyDetails.getViewContentList() && hierarchyDetails.getViewContentList().size()>0)
					{
						details = null;
						String dataRow="";
						Row row=null;
						Cell dataCell=null;
						String modelName=null;
						tokens = null;
						for(int b=0;b<hierarchyDetails.getViewContentList().size();b++)
						{
							details = (MNAOViewContentDetails)hierarchyDetails.getViewContentList().get(b);
							if(details.getViewContentType().equals("MODEL_YEAR"))
							{
								modelName= details.getModel();
								/*
								 * restore _ in ModelName by /
								 * for printing correct name in reports
								 */
								modelName = modelName.replace("_", "/");
								dataRow=details.getDocumentId()+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>";
								if(details.getDocumentId().startsWith("SI"))
								{
									dataRow+="SERVICE_INFORMATION<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("TR"))
								{
									dataRow+="TRAINING<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("VI"))
								{
									dataRow+="VIDEOS<TOK_SEPARATOR>";
								}
								dataRow+=details.getDocumentType();
								dataRow+="<TOK_SEPARATOR>"+details.getDocumentTypeName()+"<TOK_SEPARATOR>"+details.getDocumentSubType()+"<TOK_SEPARATOR>"+details.getDocumentSubTypeName();
								dataRow+="<TOK_SEPARATOR>"+modelName+"<TOK_SEPARATOR>"+details.getYear()+"<TOK_SEPARATOR>"+details.getCatNameLevel1()+"<TOK_SEPARATOR>"+details.getCatNameLevel2();
								dataRow+="<TOK_SEPARATOR>"+details.getCatNameLevel3()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>"+details.getImDocLastModifiedDate()+
										"<TOK_SEPARATOR>"+details.getSiNumber();

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
							}
							tokens = null;
							modelName = null;
							details = null;
							dataRow = null;
							dataCell = null;
							row = null;
						}
						os = new FileOutputStream(myFile);
						myWorkBook.write(os);
						logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
						os.flush();
						os.close();
						os = null;
					}
					hierarchyDetails = null;
					// restore tPath Value
					tPath = null;
					fName = null;
					myFile = null;
					mySheet = null;
					myWorkBook = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printSIChannelViewContentModelYearReport()", e);
		}
	}

	
	public void printSMChannelViewContentModelYearReport(List<MNAOViewContentDetails> viewContentList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=viewContentList	&& viewContentList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
//				String path = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//
//				/*
//				 * Now check inside the reports directory does a directory
//				 * exists for wslId
//				 */
//				File wslDirectory = new File(path);
//				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
//					// create new directory
//					wslDirectory.mkdir();
//				}
//				wslDirectory = null;
//				/*
//				 * Now check, whether the schedule code directory exists or
//				 * not
//				 */
//				if(!path.endsWith("/"))
//				{
//					path = path+"/";
//				}
//				path = path + scheduleCode;
//				File scheduleDirectory = new File(path);
//				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
//				{
//					scheduleDirectory.mkdir();
//				}
				File scheduleDirectory = null;
				String tPath = null;
				MNAOViewContentDetails hierarchyDetails = null;
				MNAOViewContentDetails details = null;
				String fName = null;
				File myFile = null;
				SXSSFWorkbook myWorkBook = null;
				Sheet mySheet = null;
				Row headerRow = null;
				Cell headerCell = null;
				String headers="";
				String[] tokens=null;
				int rowCount = 0;
				FileOutputStream os = null;
				for(int Z=0;Z<viewContentList.size();Z++)
				{
					hierarchyDetails = (MNAOViewContentDetails)viewContentList.get(Z);
					tPath = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//					tPath = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
					if(!tPath.endsWith("/"))
					{
						tPath = tPath+"/";
					}
					// add scheduleCode
					tPath+=scheduleCode;
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//					// add Locale
					tPath=tPath+"/"+hierarchyDetails.getLocale();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//
//					// add model 
//					tPath=tPath+"/"+hierarchyDetails.getModelName()+" "+hierarchyDetails.getCarlineCode();
					tPath=tPath+"/"+hierarchyDetails.getProcessingModelForMNAODataExport();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * Prepare File name
					 * scheduleCode_LOCALE+MODEL_SM_VC_MODEL_YEAR_DETAILS.xlsx
					 */
//					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getModelName().toUpperCase()+"_"+hierarchyDetails.getCarlineCode().toUpperCase()+"_SM_VC_DETAILS.xlsx";
					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getProcessingModelForMNAODataExport().toUpperCase()+"_SM_VC_MODEL_YEAR_DETAILS.xlsx";


					myFile = new File(tPath +"/"+ fName);
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					myWorkBook = new SXSSFWorkbook(100);
					// Create a new sheet
					mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					headerRow = mySheet.createRow(0);
					headerCell = null;
					headers="DOCUMENT_ID,LOCALE,CHANNEL_NAME,DOCUMENT_TYPE,DOCUMENT_TYPE_NAME,DOCUMENT_SUB_TYPE,DOCUMENT_SUB_TYPE_NAME,MODEL,YEAR,"
							+ "ESI_CATEGORY_NAME_1,ESI_CATEGORY_NAME_2,ESI_CATEGORY_NAME_3,TITLE,IM_DOC_MODIFIED_DATE";

					tokens=headers.split(",");
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

					rowCount = 0;
					os = null;
					if(null!=hierarchyDetails.getViewContentList() && hierarchyDetails.getViewContentList().size()>0)
					{
						details = null;
						String dataRow="";
						Row row=null;
						Cell dataCell=null;
						String modelName=null;
						tokens = null;
						for(int b=0;b<hierarchyDetails.getViewContentList().size();b++)
						{
							details = (MNAOViewContentDetails)hierarchyDetails.getViewContentList().get(b);
							if(details.getViewContentType().equals("MODEL_YEAR"))
							{
								modelName= details.getModel();
								/*
								 * restore _ in ModelName by /
								 * for printing correct name in reports
								 */
								modelName = modelName.replace("_", "/");
								dataRow=details.getDocumentId()+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>";
								if(details.getDocumentId().startsWith("SM"))
								{
									dataRow+="SERVICE_MANUALS<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("OSM"))
								{
									dataRow+="OTHER_SERVICE_MANUALS<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("WD"))
								{
									dataRow+="WIRING_DIAGRAMS<TOK_SEPARATOR>";
								}
								dataRow+=details.getDocumentType();

								dataRow+="<TOK_SEPARATOR>"+details.getDocumentTypeName()+"<TOK_SEPARATOR>"+details.getDocumentSubType()+"<TOK_SEPARATOR>"+details.getDocumentSubTypeName();
								dataRow+="<TOK_SEPARATOR>"+modelName+"<TOK_SEPARATOR>"+details.getYear()+"<TOK_SEPARATOR>"+details.getCatNameLevel1()+"<TOK_SEPARATOR>"+details.getCatNameLevel2();
								dataRow+="<TOK_SEPARATOR>"+details.getCatNameLevel3()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>"+details.getImDocLastModifiedDate();


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
							}
							tokens = null;
							modelName = null;
							details = null;
							dataRow = null;
							dataCell = null;
							row = null;
						}
						os = new FileOutputStream(myFile);
						myWorkBook.write(os);
						logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
						os.flush();
						os.close();
						os = null;
					}
					hierarchyDetails = null;
					// restore tPath Value
					tPath = null;
					fName = null;
					myFile = null;
					mySheet = null;
					myWorkBook = null;
					scheduleDirectory = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printSMChannelViewContentModelYearReport()", e);
		}
	}

	public void printSMChannelViewContentVINReport(List<MNAOViewContentDetails> viewContentList, String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode) && null!=viewContentList	&& viewContentList.size()>0)
			{
				/*
				 * write the string Builder Text to a separate Log file
				 * parallel to the other Logs Generation
				 */
//				String path = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//
//				/*
//				 * Now check inside the reports directory does a directory
//				 * exists for wslId
//				 */
//				File wslDirectory = new File(path);
//				if (!wslDirectory.exists() || !wslDirectory.isDirectory()) {
//					// create new directory
//					wslDirectory.mkdir();
//				}
//				wslDirectory = null;
//				/*
//				 * Now check, whether the schedule code directory exists or
//				 * not
//				 */
//				if(!path.endsWith("/"))
//				{
//					path = path+"/";
//				}
//				path = path + scheduleCode;
//				File scheduleDirectory = new File(path);
//				if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
//				{
//					scheduleDirectory.mkdir();
//				}
				File scheduleDirectory = null;
				String tPath = null;
				MNAOViewContentDetails hierarchyDetails = null;
				MNAOViewContentDetails details = null;
				String fName = null;
				File myFile = null;
				SXSSFWorkbook myWorkBook = null;
				Sheet mySheet = null;
				Row headerRow = null;
				Cell headerCell = null;
				String headers="";
				String[] tokens=null;
				int rowCount = 0;
				FileOutputStream os = null;
				for(int Z=0;Z<viewContentList.size();Z++)
				{
					hierarchyDetails = (MNAOViewContentDetails)viewContentList.get(Z);
					tPath = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
//					tPath = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
					if(!tPath.endsWith("/"))
					{
						tPath = tPath+"/";
					}
					// add scheduleCode
					tPath+=scheduleCode;
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//					// add Locale
					tPath=tPath+"/"+hierarchyDetails.getLocale();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;
//
//					// add model 
//					tPath=tPath+"/"+hierarchyDetails.getModelName()+" "+hierarchyDetails.getCarlineCode();
					tPath=tPath+"/"+hierarchyDetails.getProcessingModelForMNAODataExport();
					scheduleDirectory = new File(tPath);
					if (!scheduleDirectory.exists() || !scheduleDirectory.isDirectory()) 
					{
						scheduleDirectory.mkdir();
					}
					scheduleDirectory = null;

					/*
					 * Prepare File name
					 * scheduleCode_LOCALE+MODEL_SM_VC_VIN_DETAILS.xlsx
					 */
//					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getModelName().toUpperCase()+"_"+hierarchyDetails.getCarlineCode().toUpperCase()+"_SM_VC_DETAILS.xlsx";
					fName =  scheduleCode+"_"+hierarchyDetails.getLocale().toUpperCase()+"_"+hierarchyDetails.getProcessingModelForMNAODataExport().toUpperCase()+"_SM_VC_VIN_DETAILS.xlsx";


					myFile = new File(tPath +"/"+ fName);
					// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
					myWorkBook = new SXSSFWorkbook(100);
					// Create a new sheet
					mySheet = myWorkBook.createSheet("Details");
					/*
					 * Add Header Row
					 */
					headerRow = mySheet.createRow(0);
					headerCell = null;
					headers="DOCUMENT_ID,LOCALE,CHANNEL_NAME,DOCUMENT_TYPE,DOCUMENT_TYPE_NAME,DOCUMENT_SUB_TYPE,DOCUMENT_SUB_TYPE_NAME,WMI_CODE,"
							+ "VDS_CODE,VIS_START_RANGE,VIS_END_RANGE,ESI_CATEGORY_NAME_1,ESI_CATEGORY_NAME_2,ESI_CATEGORY_NAME_3,TITLE,IM_DOC_MODIFIED_DATE";

					tokens=headers.split(",");
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

					rowCount = 0;
					os = null;
					if(null!=hierarchyDetails.getViewContentList() && hierarchyDetails.getViewContentList().size()>0)
					{
						details = null;
						String dataRow="";
						Row row=null;
						Cell dataCell=null;
						tokens = null;
						for(int b=0;b<hierarchyDetails.getViewContentList().size();b++)
						{
							details = (MNAOViewContentDetails)hierarchyDetails.getViewContentList().get(b);
							if(details.getViewContentType().equals("VIN"))
							{
								/*
								 * restore _ in ModelName by /
								 * for printing correct name in reports
								 */
								dataRow=details.getDocumentId()+"<TOK_SEPARATOR>"+details.getLocale()+"<TOK_SEPARATOR>";
								if(details.getDocumentId().startsWith("SM"))
								{
									dataRow+="SERVICE_MANUALS<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("OSM"))
								{
									dataRow+="OTHER_SERVICE_MANUALS<TOK_SEPARATOR>";
								}
								else if(details.getDocumentId().startsWith("WD"))
								{
									dataRow+="WIRING_DIAGRAMS<TOK_SEPARATOR>";
								}
								dataRow+=details.getDocumentType();
								dataRow+="<TOK_SEPARATOR>"+details.getDocumentTypeName()+"<TOK_SEPARATOR>"+details.getDocumentSubType()+"<TOK_SEPARATOR>"+details.getDocumentSubTypeName();
								dataRow+="<TOK_SEPARATOR>"+details.getWmiCode()+
										"<TOK_SEPARATOR>"+details.getVdsCode()+"<TOK_SEPARATOR>"+details.getVisStartRange()+"<TOK_SEPARATOR>"+details.getVisEndRange()+
										"<TOK_SEPARATOR>"+details.getCatNameLevel1()+"<TOK_SEPARATOR>"+details.getCatNameLevel2();
								dataRow+="<TOK_SEPARATOR>"+details.getCatNameLevel3()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>"+details.getImDocLastModifiedDate();

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
							}
							tokens = null;
							details = null;
							dataRow = null;
							dataCell = null;
							row = null;
						}
						os = new FileOutputStream(myFile);
						myWorkBook.write(os);
						logger.info("Writing on "+fName+" REPORT XLSX file Finished ...");
						os.flush();
						os.close();
						os = null;
					}
					hierarchyDetails = null;
					// restore tPath Value
					tPath = null;
					fName = null;
					myFile = null;
					mySheet = null;
					myWorkBook = null;
					scheduleDirectory = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolPrintUtils.class.getName(), "printSMChannelViewContentVINReport()", e);
		}
	}


}
