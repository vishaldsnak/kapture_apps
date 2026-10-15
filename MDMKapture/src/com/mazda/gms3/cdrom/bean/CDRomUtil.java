package com.mazda.gms3.cdrom.bean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.cdrom.bean.impl.CDRomStartConversionImpl;
import com.mazda.gms3.cdrom.bean.impl.ContentComparator;
import com.mazda.gms3.cdrom.bean.impl.ESICategoryComparator;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.cdrom.dao.CDRomManualsDAO;
import com.mazda.gms3.mdm.dao.CategoryDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.CategoryDetails;
import com.mazda.gms3.mdm.utils.AbortCheck;

public class CDRomUtil {

	/*
	 * SET BY THE CD CREATION SCHEDULE SO THIS ORCHESTRATOR CAN STOP ON ABORT.
	 *
	 * getDocumentData() is the OUTER loop of the data extract: it walks the selected manual
	 * books and calls startConversion() for each one. A guard inside the conversion alone is
	 * not enough - it would stop the current book and this loop would start the next.
	 * Null for any caller with no schedule to abort, and then nothing changes.
	 */
	private AbortCheck abortCheck = null;

	public void setAbortCheck(AbortCheck abortCheck)
	{
		this.abortCheck = abortCheck;
	}

	static Logger logger = LogManager.getLogger(CDRomUtil.class);
	static MessageProperties msgProps = null;

	public static void manageModelsList(CDRomSearchBean searchBean) {
		searchBean.setModels(new ArrayList<LabelBean>());
		try {
			if(null!=searchBean.getSelectedCountry() && !"".equals(searchBean.getSelectedCountry()) && 
					null!=searchBean.getSelectedLanguage() && !"".equals(searchBean.getSelectedLanguage()))
			{
				searchBean.setModels(CDRomDAO.getModelsList(searchBean.getSelectedLanguage()));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(),
					"manageModelsList()", e);
		}
	}

	public static void manageWmiList(CDRomSearchBean searchBean) {
		searchBean.setWmi(new ArrayList<LabelBean>());
		try {
			if(null!=searchBean.getSelectedCountry() && !"".equals(searchBean.getSelectedCountry()) && 
					null!=searchBean.getSelectedLanguage() && !"".equals(searchBean.getSelectedLanguage()) && 
					null!=searchBean.getSelectedModel() && !"".equals(searchBean.getSelectedModel()))
			{
				searchBean.setWmi(CDRomDAO.getWmiList(searchBean.getSelectedLanguage(),
						searchBean.getSelectedModel()));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(),
					"manageWmiList()", e);
		}
	}

	public static void manageVdsList(CDRomSearchBean searchBean) {
		searchBean.setVds(new ArrayList<LabelBean>());
		try {
			if(null!=searchBean.getSelectedCountry() && !"".equals(searchBean.getSelectedCountry()) && 
					null!=searchBean.getSelectedLanguage() && !"".equals(searchBean.getSelectedLanguage()) && 
					null!=searchBean.getSelectedModel() && !"".equals(searchBean.getSelectedModel()) && 
					null!=searchBean.getSelectedWmi() && !"".equals(searchBean.getSelectedWmi()))
			{
				searchBean
				.setVds(CDRomDAO.getVdsList(searchBean.getSelectedLanguage(),
						searchBean.getSelectedModel(),
						searchBean.getSelectedWmi()));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(),
					"manageVdsList()", e);
		}
	}

	public static void manageVinRangeList(CDRomSearchBean searchBean) {
		searchBean.setVinRange(new ArrayList<LabelBean>());
		try {
			if(null!=searchBean.getSelectedCountry() && !"".equals(searchBean.getSelectedCountry()) && 
					null!=searchBean.getSelectedLanguage() && !"".equals(searchBean.getSelectedLanguage()) && 
					null!=searchBean.getSelectedModel() && !"".equals(searchBean.getSelectedModel()) && 
					null!=searchBean.getSelectedWmi() && !"".equals(searchBean.getSelectedWmi()) && 
					null!=searchBean.getSelectedVds() && !"".equals(searchBean.getSelectedVds()))
			{
				searchBean.setVinRange(CDRomDAO.getVinRangeList(
						searchBean.getSelectedLanguage(),
						searchBean.getSelectedModel(), searchBean.getSelectedWmi(),
						searchBean.getSelectedVds()));
			}
		} catch (SQLException e) {
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(),
					"manageVinRangeList()", e);
		}
	}

	public static List<String> getAsList(String str) {
		return new ArrayList<String>(Arrays.asList(str.split(",")));
	}

	public void getDocumentData(
			String locale,
			String carLineCode, String vdsCode, String vinStartRange,
			String wmiCode, String timeStamp, String modelName, String wslId,
			CDRomStartConversionImpl startConvImpl, String networkPath,
			List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList, CDRomSearchBean searchBean,
			List<OKAssetReportBean> okAssetReportBean,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			Long scheduleID, ArrayList<CDRomScheduleItemDetails> itemsList, CDRomDAO cdRomDao) {
		/*
		 *  UPDATE SCHDEULE STATUS TO PROCESSING
		 */
		try
		{
			cdRomDao.updateScheduleStatus(String.valueOf(scheduleID), ApplicationProperties.getProperty("schedule.status.processing.value"));
		}
		catch(Exception e1)
		{
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
		}
		
		try
		{
			if(null!=itemsList && itemsList.size()>0)
			{
				for(CDRomScheduleItemDetails itemDetails : itemsList)
				{
					if(null!=abortCheck && abortCheck.isAborted()) { break; }
					// UPDATE ITEM STATUS TO DOCUMENT PROCESSING.
					try
					{
						// UPDATE SCHEDULE ID PROCESSING STATUS
						cdRomDao
						.updateCurrentProcessingStatus(
								String.valueOf(scheduleID),
								ApplicationProperties
										.getProperty("schedule.current.status.document.value"),
								itemDetails.getManualTypeCode());
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
					}
					
					if(null!=itemDetails.getDocumentIdsList() && itemDetails.getDocumentIdsList().size()>0)
					{
						try
						{
							// START PROCESSING DOC TYPE FOR SCHDEULE.
							List<LabelBean> xmlDocumentList = CDRomManualsDAO
									.getDocumentRecordIdAndXMLForManual(itemDetails.getDocumentIdsList(), locale);
							loadModelData(itemDetails.getManualTypeCode(), xmlDocumentList, locale,
									carLineCode, vdsCode, vinStartRange, timeStamp,
									modelName, wslId, String.valueOf(scheduleID),
									startConvImpl, wmiCode, networkPath, 
									keywordSearchList, viewContentList,
									firstLevelCatList,
									itemDetails.getManualTypeName(),
									okAssetReportBean, innerLinkReportBean,
									transactionReportBean, documentFailureReportBean,
									"",cdRomDao);
							
							// HERE AFTER EXTRACTION , ITEM STATUS IS GETTING UPDATED AS COMPLETED INSIDE LOAD MODEL DATA FUNCTION.
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
						}
					}
					else
					{
						// UPDATE ITEM STATUS TO COMPLETED.
						try
						{
							cdRomDao.updateCurrentProcessingStatus(
									String.valueOf(scheduleID),
									ApplicationProperties
											.getProperty("schedule.current.status.complete.value"),
									itemDetails.getManualTypeCode());
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
						}
					}
				}
			}
			else
			{
				/*
				 *  UPDATE SCHDEULE STATUS TO FAILURE
				 */
				try
				{
					cdRomDao.updateScheduleStatus(String.valueOf(scheduleID), ApplicationProperties.getProperty("schedule.status.failure.value"));
				}
				catch(Exception e1)
				{
					Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e);
			/*
			 *  UPDATE SCHDEULE STATUS TO FAILURE
			 */
			try
			{
				cdRomDao.updateScheduleStatus(String.valueOf(scheduleID), ApplicationProperties.getProperty("schedule.status.failure.value"));
				if(null!=itemsList && itemsList.size()>0)
				{
					for(CDRomScheduleItemDetails it : itemsList)
					{
						if(null!=abortCheck && abortCheck.isAborted()) { break; }
						cdRomDao.updateCurrentProcessingStatus(
								String.valueOf(scheduleID),
								ApplicationProperties
										.getProperty("schedule.current.status.complete.value"),
								it.getManualTypeCode());
						it = null;
					}
				}
			}
			catch(Exception e1)
			{
				Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "getDocumentData()", e1);
			}
		}
	}
	
	public static void loadModelData(String docType,
			List<LabelBean> xmlDocumentList, String locale, String carLineCode,
			String vdsCode, String vinStartRange, String timeStamp,
			String modelName, String wslId, String scheduleID,
			CDRomStartConversionImpl startConvImpl, String wmiCode,
			String networkPath, 
			List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList, String manualTypeName,
			List<OKAssetReportBean> okAssetReportBean,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String jobStatus,CDRomDAO cdRomDao) {
//		logger.info("Start loadModelData for model type: " + modelType);
		logger.info("Start loadModelData for manual type: " + docType);
		try {
			
			if (xmlDocumentList != null && xmlDocumentList.size() > 0) {
				if (null != scheduleID) {
					startConvImpl.startConversion(xmlDocumentList,
							String.valueOf(scheduleID), timeStamp, locale,
							carLineCode, docType, vdsCode, vinStartRange,
							modelName, networkPath, wmiCode, keywordSearchList,
							viewContentList, firstLevelCatList, 
							manualTypeName, okAssetReportBean,
							innerLinkReportBean, transactionReportBean,
							documentFailureReportBean, jobStatus,cdRomDao);
				}
			}
			try
			{
				cdRomDao.updateCurrentProcessingStatus(
						String.valueOf(scheduleID),
						ApplicationProperties
								.getProperty("schedule.current.status.complete.value"),
						docType);
			}
			catch(Exception e1)
			{
				Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "loadModelData()", e1);
			}
		} catch (Exception e) {
			logger.info("loadModelData :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"loadModelData()", e);
			logger.info("loadModelData :: ################ Exception ################");
		}
		logger.info("End loadModelData for manual type: " + docType);

	}

	public void copyHTML(String timeStamp, String networkPath, ArrayList<ApplicableVINList> applicableVINList,String locale) {
		logger.info("copyHTML starts here");
		String root = networkPath;
		String folderPath = root + "/CD_ROM_" + timeStamp;
		String sourcePath="";
		if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
				locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
		{
			// MC MARKET LOCALE
			sourcePath = ApplicationProperties.getProperty("cdrom.japan.html.folder.path");
		}
		else
		{
			// MME MARKET LOCALE
			sourcePath = ApplicationProperties.getProperty("cdrom.mme.html.folder.path");
		}
		File source = new File(sourcePath);
		File dest = new File(folderPath);
		try {
			FileUtils.copyDirectory(source, dest);
			
			/*
			 * DO THIS FOR GEENRATING APPLICABLE VIN DATA IN INDEX.HTML IN DESTIONATION DIRECTORY
			 */
			if(dest.isDirectory() && dest.exists()==true)
			{
				File[] destFiles = dest.listFiles();
				if(null!=destFiles && destFiles.length>0)
				{
					for(File file : destFiles)
					{
						if(null!=abortCheck && abortCheck.isAborted()) { break; }
						if(file.exists() && file.isFile())
						{
							if(file.getName().equals("index.html"))
							{
								/*
								 *  READ CONTENT OF THE HTML FILE AND REPALCE APP_TOK_VIN_DATA
								 */
								String data = FileUtils.readFileToString(file, "utf-8");
								if(null!=data && !"".equals(data))
								{
									String replaceToken="";
									if(null!=applicableVINList && applicableVINList.size()>0)
									{
										int co=0;
										for(ApplicableVINList appVIN : applicableVINList)
										{
											if(null!=abortCheck && abortCheck.isAborted()) { break; }
											co++;
											replaceToken = replaceToken+"<tr>";
											replaceToken=replaceToken+"<td>"+co+"</td>";
											if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
													locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
											{
												// MC MARKET - NO WMI CODE
											}
											else
											{
												// MME MARKET-  ADD WMI CODE
												replaceToken=replaceToken+"<td>"+appVIN.getWmiCode()+"</td>";
											}
											replaceToken=replaceToken+"<td>"+appVIN.getVdsCode()+"</td>";
											replaceToken=replaceToken+"<td align=\"left\">"+appVIN.getVisStartRange()+"</td>";
											replaceToken = replaceToken+"</tr>";
											appVIN=  null;
										}
									}
									
									data = data.replace("APP_TOK_VIN_DATA", replaceToken);
									
									// OVER WRITE INDEX.HTML AT DESTIONATION LOCATION
									FileUtils.writeStringToFile(file, data, "utf-8");
									replaceToken = null;
								}
								data = null;
								break;
							}
						}
					}
				}
			}
			
			
		} catch (IOException e) {
			logger.info("copyHTML :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"copyHTML()", e);
			logger.info("copyHTML :: ################ Exception ################");
		}
		logger.info("copyHTML Ends here");

	}

	public void createReports(String networkPath, String timeStamp,
			List<OKAssetReportBean> okAssetReportBean,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean, String wslId,
			String scheduleName, String locale, String modelName,
			String carLineCode, String wmiCode, String vdsCode,
			String vinStartRange,
			List<DocumentFailureReportBean> documentFailureReportBean) {
		logger.info("createReports starts here");
		String reportsPath = networkPath + "/CD_ROM_" + timeStamp + "_Reports";
		File path = new File(reportsPath);
		if (!path.exists()) {
			if (!path.mkdirs()) {
				logger.info("getFolderPath :: Failed to create directory");

			}
		}
		if (okAssetReportBean != null && okAssetReportBean.size() > 0) {
			generateOkAssetReport(reportsPath, okAssetReportBean, wslId,
					scheduleName, locale, modelName, carLineCode, wmiCode,
					vdsCode, vinStartRange);
		}
		if (innerLinkReportBean != null && innerLinkReportBean.size() > 0) {
			generateInnerLinkReport(reportsPath, innerLinkReportBean, wslId,
					scheduleName, locale, modelName, carLineCode, wmiCode,
					vdsCode, vinStartRange);
		}
		if (transactionReportBean != null && transactionReportBean.size() > 0) {
			generateTransactionReport(reportsPath, transactionReportBean,
					wslId, scheduleName, locale, modelName, carLineCode,
					wmiCode, vdsCode, vinStartRange);
		}
		if (documentFailureReportBean != null
				&& documentFailureReportBean.size() > 0) {
			generateFailureReport(reportsPath, documentFailureReportBean,
					wslId, scheduleName, locale, modelName, carLineCode,
					wmiCode, vdsCode, vinStartRange);
		}
		logger.info("createReports Ends here");

	}
	
	private static void generateFailureReport(String reportsPath,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String wslId, String scheduleName, String locale, String modelName,
			String carLineCode, String wmiCode, String vdsCode,
			String vinStartRange) {
		try {
			File myFile = new File(reportsPath + "/DocumentFailureReport.xlsx");
			
			// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
			SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

			// Create a new sheet
			Sheet mySheet = myWorkBook.createSheet("Failure Details");
			/*
			 * Add Header Row
			 */
			Row headerRow = mySheet.createRow(0);
			Cell wslCell = headerRow.createCell(0);
			wslCell.setCellValue("WSL ID");
			Cell schNameCell = headerRow.createCell(1);
			schNameCell.setCellValue("Schedule Name");
			Cell localeCell = headerRow.createCell(2);
			localeCell.setCellValue("Locale");
			Cell modelCell = headerRow.createCell(3);
			modelCell.setCellValue("Model");
			Cell carCodeCell = headerRow.createCell(4);
			carCodeCell.setCellValue("Carline Code");
			Cell wmiCell = headerRow.createCell(5);
			wmiCell.setCellValue("WMI");
			Cell vdsCell = headerRow.createCell(6);
			vdsCell.setCellValue("VDS");
			Cell visStartCell = headerRow.createCell(7);
			visStartCell.setCellValue("Vin Start Range");
			Cell manualTypeCell = headerRow.createCell(8);
			manualTypeCell.setCellValue("Manual Type");
			Cell docIdCell = headerRow.createCell(9);
			docIdCell.setCellValue("Document Id");
			Cell errorCodeCell = headerRow.createCell(10);
			errorCodeCell.setCellValue("Error Code");
			Cell errorMsgCell = headerRow.createCell(11);
			errorMsgCell.setCellValue("Error Message / Details");
			Cell errorTimeCell = headerRow.createCell(12);
			errorTimeCell.setCellValue("Error Time");
			int rowCount = 0;
			for (int i = 0; i < documentFailureReportBean.size(); i++) 
			{
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
				
				if(null!=wslId && !"".equals(wslId))
				{
					cell0.setCellValue(wslId);
				}
				if(null!=scheduleName && !"".equals(scheduleName))
				{
					cell1.setCellValue(scheduleName);
				}
				if(null!=locale && !"".equals(locale))
				{
					cell2.setCellValue(locale);
				}
				if(null!=modelName && !"".equals(modelName))
				{
					cell3.setCellValue(modelName);
				}
				if(null!=carLineCode && !"".equals(carLineCode))
				{
					cell4.setCellValue(carLineCode);
				}
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					cell5.setCellValue(wmiCode);
				}
				if(null!=vdsCode && !"".equals(vdsCode))
				{
					cell6.setCellValue(vdsCode);
				}
				if(null!=vinStartRange && !"".equals(vinStartRange))
				{
					cell7.setCellValue(vinStartRange);
				}
				if(null!=documentFailureReportBean.get(i).getManualType())
				{
					cell8.setCellValue(documentFailureReportBean.get(i).getManualType());
				}
				if(null!=documentFailureReportBean.get(i).getDocumentId())
				{
					cell9.setCellValue(documentFailureReportBean.get(i).getDocumentId());
				}
				if(null!=documentFailureReportBean.get(i).getErrorCode())
				{
					cell10.setCellValue(documentFailureReportBean.get(i).getErrorCode());
				}
				if(null!=documentFailureReportBean.get(i).getErrorMessage())
				{
					cell11.setCellValue(documentFailureReportBean.get(i).getErrorMessage());
				}
				if(null!=documentFailureReportBean.get(i).getErrorTime())
				{
					cell12.setCellValue(documentFailureReportBean.get(i).getErrorTime());
				}
				
				cell4 = null;
				cell3 = null;
				cell2 = null;
				cell1 = null;
				cell0 = null;
				cell5=null;
				cell6=null;
				cell7=null;
				cell8=null;
				cell9=null;
				cell10=null;
				cell11=null;
				cell12=null;
				row = null;
			}
			
			headerRow = null;
			wslCell = null;
			schNameCell = null;
			localeCell = null;
			modelCell =null;
			wmiCell = null;
			vdsCell = null;
			visStartCell = null;
			manualTypeCell = null;
			docIdCell = null;
			errorCodeCell = null;
			errorMsgCell=null;
			errorTimeCell = null;
			
			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on FAILURE REPORT XLSX file Finished ...");
			os.flush();
			os.close();

			// set mySheet to null
			mySheet = null;
			// set myWorkBook to null
			myWorkBook = null;
			// set myFile to null
			myFile = null;
		} catch (Exception e) {
			logger.info("generateFailureReport :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"generateFailureReport()", e);
			logger.info("generateFailureReport :: ################ Exception ################");
		}

	}

	private static void generateTransactionReport(String reportsPath,
			List<TransactionReportBean> transactionReportBean, String wslId,
			String scheduleName, String locale, String modelName,
			String carLineCode, String wmiCode, String vdsCode,
			String vinStartRange) {
		try {
			File myFile = new File(reportsPath + "/TransactionReport.xlsx");
			
			// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
			SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

			// Create a new sheet
			Sheet mySheet = myWorkBook.createSheet("Details");
			/*
			 * Add Header Row
			 */
			Row headerRow = mySheet.createRow(0);
			Cell wslCell = headerRow.createCell(0);
			wslCell.setCellValue("WSL ID");
			Cell schNameCell = headerRow.createCell(1);
			schNameCell.setCellValue("Schedule Name");
			Cell localeCell = headerRow.createCell(2);
			localeCell.setCellValue("Locale");
			Cell modelCell = headerRow.createCell(3);
			modelCell.setCellValue("Model");
			Cell carCodeCell = headerRow.createCell(4);
			carCodeCell.setCellValue("Carline Code");
			Cell wmiCell = headerRow.createCell(5);
			wmiCell.setCellValue("WMI");
			Cell vdsCell = headerRow.createCell(6);
			vdsCell.setCellValue("VDS");
			Cell visStartCell = headerRow.createCell(7);
			visStartCell.setCellValue("Vin Start Range");
			Cell manualTypeCell = headerRow.createCell(8);
			manualTypeCell.setCellValue("Manual Type");
			Cell docIdCell = headerRow.createCell(9);
			docIdCell.setCellValue("Document Id");
			Cell destLocCell = headerRow.createCell(10);
			destLocCell.setCellValue("Destination Location");
			Cell inrlkCountCell = headerRow.createCell(11);
			inrlkCountCell.setCellValue("Inner Links Count"); 
			Cell allInrMapCountCell = headerRow.createCell(12);
			allInrMapCountCell.setCellValue("All Inner Links Mapped");
			Cell okastCountCell = headerRow.createCell(13);
			okastCountCell.setCellValue("OKAssets Count");
			Cell allOkAstLocCell = headerRow.createCell(14);
			allOkAstLocCell.setCellValue("All OKAssets Location");
			Cell opTypeCell = headerRow.createCell(15);
			opTypeCell.setCellValue("Operation Type");
			Cell procStatusCell = headerRow.createCell(16);
			procStatusCell.setCellValue("Processing Status");
			Cell reasonCell = headerRow.createCell(17);
			reasonCell.setCellValue("Reason For Failure");
			
			int rowCount = 0;
			for (int i = 0; i < transactionReportBean.size(); i++) 
			{
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
				
				if(null!=wslId && !"".equals(wslId))
				{
					cell0.setCellValue(wslId);
				}
				if(null!=scheduleName && !"".equals(scheduleName))
				{
					cell1.setCellValue(scheduleName);
				}
				if(null!=locale && !"".equals(locale))
				{
					cell2.setCellValue(locale);
				}
				if(null!=modelName && !"".equals(modelName))
				{
					cell3.setCellValue(modelName);
				}
				if(null!=carLineCode && !"".equals(carLineCode))
				{
					cell4.setCellValue(carLineCode);
				}
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					cell5.setCellValue(wmiCode);
				}
				if(null!=vdsCode && !"".equals(vdsCode))
				{
					cell6.setCellValue(vdsCode);
				}
				if(null!=vinStartRange && !"".equals(vinStartRange))
				{
					cell7.setCellValue(vinStartRange);
				}
				if(null!=transactionReportBean.get(i).getManualType())
				{
					cell8.setCellValue(transactionReportBean.get(i).getManualType());
				}
				if(null!=transactionReportBean.get(i).getDocumentId())
				{
					cell9.setCellValue(transactionReportBean.get(i).getDocumentId());
				}
				if(null!=transactionReportBean.get(i).getDestinationLocation())
				{
					cell10.setCellValue(transactionReportBean.get(i).getDestinationLocation());
				}
				
				cell11.setCellValue(String.valueOf(transactionReportBean.get(i).getInnerLinkCount()));
				
				if(null!=transactionReportBean.get(i).getAllInnerLinkMapped())
				{
					cell12.setCellValue(transactionReportBean.get(i).getAllInnerLinkMapped());
				}
				
				cell13.setCellValue(String.valueOf(transactionReportBean.get(i).getOkAssetCount()));

				if(null!=transactionReportBean.get(i).getAllOkAssetMapped())
				{
					cell14.setCellValue(transactionReportBean.get(i).getAllOkAssetMapped());
				}
				
				if(null!=transactionReportBean.get(i).getStatus())
				{
					cell16.setCellValue(transactionReportBean.get(i).getStatus());
				}
				
				if(null!=transactionReportBean.get(i).getFailureReason())
				{
					cell17.setCellValue(transactionReportBean.get(i).getFailureReason());
				}
				
				cell4 = null;
				cell3 = null;
				cell2 = null;
				cell1 = null;
				cell0 = null;
				cell5=null;
				cell6=null;
				cell7=null;
				cell8=null;
				cell9=null;
				cell10=null;
				cell11=null;
				cell12=null;
				cell13=null;
				cell14=null;
				cell15=null;
				cell16=null;
				cell17 = null;
				row = null;
			}
			
			headerRow = null;
			wslCell = null;
			schNameCell = null;
			localeCell = null;
			modelCell =null;
			wmiCell = null;
			vdsCell = null;
			visStartCell = null;
			manualTypeCell = null;
			docIdCell = null;
			destLocCell = null;
			inrlkCountCell = null;
			allInrMapCountCell = null;
			okastCountCell = null;
			allOkAstLocCell = null;
			opTypeCell = null;
			procStatusCell = null;
			reasonCell = null;
			
			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on TRANSACTION REPORT XLSX file Finished ...");
			os.flush();
			os.close();

			// set mySheet to null
			mySheet = null;
			// set myWorkBook to null
			myWorkBook = null;
			// set myFile to null
			myFile = null;
		} catch (IOException e) {
			logger.info("generateTransactionReport :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"generateTransactionReport()", e);
			logger.info("generateTransactionReport :: ################ Exception ################");
		}

	}

	private static void generateInnerLinkReport(String reportsPath,
			List<InnerLinkReportBean> innerLinkReportBean, String wslId,
			String scheduleName, String locale, String modelName,
			String carLineCode, String wmiCode, String vdsCode,
			String vinStartRange) {
		try {
			File myFile = new File(reportsPath + "/InnerLinkReport.xlsx");
			
			// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
			SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

			// Create a new sheet
			Sheet mySheet = myWorkBook.createSheet("InnerLink Details");
			/*
			 * Add Header Row
			 */
			Row headerRow = mySheet.createRow(0);
			Cell wslCell = headerRow.createCell(0);
			wslCell.setCellValue("WSL ID");
			Cell schNameCell = headerRow.createCell(1);
			schNameCell.setCellValue("Schedule Name");
			Cell localeCell = headerRow.createCell(2);
			localeCell.setCellValue("Locale");
			Cell modelCell = headerRow.createCell(3);
			modelCell.setCellValue("Model");
			Cell carCodeCell = headerRow.createCell(4);
			carCodeCell.setCellValue("Carline Code");
			Cell wmiCell = headerRow.createCell(5);
			wmiCell.setCellValue("WMI");
			Cell vdsCell = headerRow.createCell(6);
			vdsCell.setCellValue("VDS");
			Cell visStartCell = headerRow.createCell(7);
			visStartCell.setCellValue("Vin Start Range");
			Cell manualTypeCell = headerRow.createCell(8);
			manualTypeCell.setCellValue("Manual Type");
			Cell docIdCell = headerRow.createCell(9);
			docIdCell.setCellValue("Document Id");
			Cell inrSourceDocIdCell = headerRow.createCell(10);
			inrSourceDocIdCell.setCellValue("Inner Link Source Document id");
			Cell htmlFileNameCell = headerRow.createCell(11);
			htmlFileNameCell.setCellValue("Inner Link Mapped Document HTML File Name"); 
			Cell locPathCell = headerRow.createCell(12);
			locPathCell.setCellValue("Inner Link Mapped Document HTML File Location Path");
			Cell statusCell = headerRow.createCell(13);
			statusCell.setCellValue("Mapping Status");
			
			int rowCount = 0;
			for (int i = 0; i < innerLinkReportBean.size(); i++) 
			{
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
				
				if(null!=wslId && !"".equals(wslId))
				{
					cell0.setCellValue(wslId);
				}
				if(null!=scheduleName && !"".equals(scheduleName))
				{
					cell1.setCellValue(scheduleName);
				}
				if(null!=locale && !"".equals(locale))
				{
					cell2.setCellValue(locale);
				}
				if(null!=modelName && !"".equals(modelName))
				{
					cell3.setCellValue(modelName);
				}
				if(null!=carLineCode && !"".equals(carLineCode))
				{
					cell4.setCellValue(carLineCode);
				}
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					cell5.setCellValue(wmiCode);
				}
				if(null!=vdsCode && !"".equals(vdsCode))
				{
					cell6.setCellValue(vdsCode);
				}
				if(null!=vinStartRange && !"".equals(vinStartRange))
				{
					cell7.setCellValue(vinStartRange);
				}
				if(null!=innerLinkReportBean.get(i).getManualType())
				{
					cell8.setCellValue(innerLinkReportBean.get(i).getManualType());
				}
				if(null!=innerLinkReportBean.get(i).getDocumentId())
				{
					cell9.setCellValue(innerLinkReportBean.get(i).getDocumentId());
				}
				if(null!=innerLinkReportBean.get(i).getSourceDocumentId())
				{
					cell10.setCellValue(innerLinkReportBean.get(i).getSourceDocumentId());
				}
				if(null!=innerLinkReportBean.get(i).getMappedDocumentHTMLFileName())
				{
					cell11.setCellValue(innerLinkReportBean.get(i).getMappedDocumentHTMLFileName());
				}
				if(null!=innerLinkReportBean.get(i).getMappedDocumentHTMLFilePath())
				{
					cell12.setCellValue(innerLinkReportBean.get(i).getMappedDocumentHTMLFilePath());
				}
				if(null!=innerLinkReportBean.get(i).getMappingStatus())
				{
					cell13.setCellValue(innerLinkReportBean.get(i).getMappingStatus());
				}
				
				
				cell4 = null;
				cell3 = null;
				cell2 = null;
				cell1 = null;
				cell0 = null;
				cell5=null;
				cell6=null;
				cell7=null;
				cell8=null;
				cell9=null;
				cell10=null;
				cell11=null;
				cell12=null;
				cell13=null;
				
				row = null;
			}
			
			headerRow = null;
			wslCell = null;
			schNameCell = null;
			localeCell = null;
			modelCell =null;
			wmiCell = null;
			vdsCell = null;
			visStartCell = null;
			manualTypeCell = null;
			docIdCell = null;
			inrSourceDocIdCell = null;
			htmlFileNameCell = null;
			locPathCell = null;
			statusCell = null;
			
			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on INNERLINK REPORT XLSX file Finished ...");
			os.flush();
			os.close();

			// set mySheet to null
			mySheet = null;
			// set myWorkBook to null
			myWorkBook = null;
			// set myFile to null
			myFile = null;
		} catch (Exception e) {
			logger.info("generateInnerLinkReport :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"generateInnerLinkReport()", e);
			logger.info("generateInnerLinkReport :: ################ Exception ################");
		}
	}

	private static void generateOkAssetReport(String reportsPath,
			List<OKAssetReportBean> okAssetReportBean, String wslId,
			String scheduleName, String locale, String modelName,
			String carLineCode, String wmiCode, String vdsCode,
			String vinStartRange) {
		try {
			
			File myFile = new File(reportsPath + "/OkAssetReport.xlsx");
			
			// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
			SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

			// Create a new sheet
			Sheet mySheet = myWorkBook.createSheet("OKAssets Details");
			/*
			 * Add Header Row
			 */
			Row headerRow = mySheet.createRow(0);
			Cell wslCell = headerRow.createCell(0);
			wslCell.setCellValue("WSL ID");
			Cell schNameCell = headerRow.createCell(1);
			schNameCell.setCellValue("Schedule Name");
			Cell localeCell = headerRow.createCell(2);
			localeCell.setCellValue("Locale");
			Cell modelCell = headerRow.createCell(3);
			modelCell.setCellValue("Model");
			Cell carCodeCell = headerRow.createCell(4);
			carCodeCell.setCellValue("Carline Code");
			Cell wmiCell = headerRow.createCell(5);
			wmiCell.setCellValue("WMI");
			Cell vdsCell = headerRow.createCell(6);
			vdsCell.setCellValue("VDS");
			Cell visStartCell = headerRow.createCell(7);
			visStartCell.setCellValue("Vin Start Range");
			Cell manualTypeCell = headerRow.createCell(8);
			manualTypeCell.setCellValue("Manual Type");
			Cell docIdCell = headerRow.createCell(9);
			docIdCell.setCellValue("Document Id");
			Cell okAssetNameCell = headerRow.createCell(10);
			okAssetNameCell.setCellValue("OKAsset Name");
			Cell okAssetSourceLocCell = headerRow.createCell(11);
			okAssetSourceLocCell.setCellValue("OKAsset Source Location"); 
			Cell okAssetDestLocCell = headerRow.createCell(12);
			okAssetDestLocCell.setCellValue("OKAsset Destination Location");
			Cell statusCell = headerRow.createCell(13);
			statusCell.setCellValue("Status");
			Cell errorCodeCell = headerRow.createCell(14);
			errorCodeCell.setCellValue("Error Code");
			Cell errorMsgCell = headerRow.createCell(15);
			errorMsgCell.setCellValue("Error Message / Details");
			Cell errorTimeCell = headerRow.createCell(16);
			errorTimeCell.setCellValue("Error Time");
			int rowCount = 0;
			for (int i = 0; i < okAssetReportBean.size(); i++) 
			{
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
				
				if(null!=wslId && !"".equals(wslId))
				{
					cell0.setCellValue(wslId);
				}
				if(null!=scheduleName && !"".equals(scheduleName))
				{
					cell1.setCellValue(scheduleName);
				}
				if(null!=locale && !"".equals(locale))
				{
					cell2.setCellValue(locale);
				}
				if(null!=modelName && !"".equals(modelName))
				{
					cell3.setCellValue(modelName);
				}
				if(null!=carLineCode && !"".equals(carLineCode))
				{
					cell4.setCellValue(carLineCode);
				}
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					cell5.setCellValue(wmiCode);
				}
				if(null!=vdsCode && !"".equals(vdsCode))
				{
					cell6.setCellValue(vdsCode);
				}
				if(null!=vinStartRange && !"".equals(vinStartRange))
				{
					cell7.setCellValue(vinStartRange);
				}
				if(null!=okAssetReportBean.get(i).getManualType())
				{
					cell8.setCellValue(okAssetReportBean.get(i).getManualType());
				}
				if(null!=okAssetReportBean.get(i).getDocumentId())
				{
					cell9.setCellValue(okAssetReportBean.get(i).getDocumentId());
				}
				if(null!=okAssetReportBean.get(i).getOkAssetName())
				{
					cell10.setCellValue(okAssetReportBean.get(i).getOkAssetName());
				}
				if(null!=okAssetReportBean.get(i).getOkAssetSourceLocation())
				{
					cell11.setCellValue(okAssetReportBean.get(i).getOkAssetSourceLocation());
				}
				if(null!=okAssetReportBean.get(i)
						.getOkAssetDestinationLocation())
				{
					cell12.setCellValue(okAssetReportBean.get(i)
						.getOkAssetDestinationLocation());
				}
				if(null!=okAssetReportBean.get(i).getStatus())
				{
					cell13.setCellValue(okAssetReportBean.get(i).getStatus());
				}
				if(null!=okAssetReportBean.get(i).getErrorCode())
				{
					cell14.setCellValue(okAssetReportBean.get(i).getErrorCode());
				}
				if(null!=okAssetReportBean.get(i).getErrorMessage())
				{
					cell15.setCellValue(okAssetReportBean.get(i).getErrorMessage());
				}
				if(null!=okAssetReportBean.get(i).getErrorTime())
				{
					cell16.setCellValue(okAssetReportBean.get(i).getErrorTime());
				}
				
				cell4 = null;
				cell3 = null;
				cell2 = null;
				cell1 = null;
				cell0 = null;
				cell5=null;
				cell6=null;
				cell7=null;
				cell8=null;
				cell9=null;
				cell10=null;
				cell11=null;
				cell12=null;
				cell13=null;
				cell14=null;
				cell15=null;
				cell16=null;
				
				row = null;
			}
			
			headerRow = null;
			wslCell = null;
			schNameCell = null;
			localeCell = null;
			modelCell =null;
			wmiCell = null;
			vdsCell = null;
			visStartCell = null;
			manualTypeCell = null;
			docIdCell = null;
			okAssetNameCell = null;
			okAssetSourceLocCell = null;
			okAssetDestLocCell = null;
			statusCell = null;
			errorCodeCell = null;
			errorMsgCell=null;
			errorTimeCell = null;
			
			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on OKASSETS REPORT XLSX file Finished ...");
			os.flush();
			os.close();

			// set mySheet to null
			mySheet = null;
			// set myWorkBook to null
			myWorkBook = null;
			// set myFile to null
			myFile = null;
		} catch (Exception e) {
			logger.info("generateOkAssetReport :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"generateOkAssetReport()", e);
			logger.info("generateOkAssetReport :: ################ Exception ################");
		}
	}
		
	
	public void createJson(List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList, String timeStamp,
			Set<String> firstLevelCatList, String networkPath,
			String modelName, String carLineCode, String vdsCode,
			String vinStartRange, 
			CDRomSearchBean searchBean, ArrayList<CDRomScheduleItemDetails> itemsList,String langCode,String wmiCode) {
		
		// 1. Java object to JSON, and save into a file
		/*
		 * var thirdLevelUnique= new Array(); thirdLevelUnique[0]="null";
		 * thirdLevelUnique[1]="Engine";
		 */
		ArrayList<CategoryDetails> sortedFirstLevelCatList= new ArrayList<CategoryDetails>();
		try
		{
			// getSortedCatList on the basis of CAT Codes
			ArrayList<CategoryDetails> sortedCatList = CategoryDAO.getSortedCategoryList(langCode);
			if(null!=sortedCatList && sortedCatList.size()>0)
			{
				for(CategoryDetails cat : sortedCatList)
				{
					if(null!=abortCheck && abortCheck.isAborted()) { break; }
					if(null!=cat.getCategoryNameEng() && !"".equals(cat.getCategoryNameEng()))
					{
						for (String s : firstLevelCatList) {
							if(null!=abortCheck && abortCheck.isAborted()) { break; }
							if(null!=s && !"".equals(s))
							{
								if(s.trim().toLowerCase().equals(cat.getCategoryNameEng().trim().toLowerCase()))
								{
									// add to List
									sortedFirstLevelCatList.add(cat);
									break;
								}
							}
							s = null;
						}
					}
					cat = null;
				}
			}
			sortedCatList = null;
		}catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "createJson()", e);
		}
		
		if(null==sortedFirstLevelCatList || sortedFirstLevelCatList.size()<=0)
		{
			sortedFirstLevelCatList = new ArrayList<CategoryDetails>();
			// set the data from firstLevelSet to the List as it is
			for(String s : firstLevelCatList)
			{
				if(null!=abortCheck && abortCheck.isAborted()) { break; }
				CategoryDetails cDetails = new CategoryDetails();
				cDetails.setCategoryNameEng(s);
				sortedFirstLevelCatList.add(cDetails);
				cDetails= null;
			}
		}
		else
		{
			// sort the List by CAT Code
			ESICategoryComparator esiComp = new ESICategoryComparator();
			Collections.sort(sortedFirstLevelCatList, esiComp);
			esiComp=  null;
		}
		
		logger.info(" Create json starts");
		String root = networkPath;
		String folderPath = root + "/CD_ROM_" + timeStamp;
		String keywordjs = folderPath + "/js/keyword.js";
		String contentjs = folderPath + "/js/content.js";
		// String keywordJson = folderPath+"/js/keywordJson.js";
		String labelTitle="";
		if(langCode.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
				langCode.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
		{
			// MC MARKET
			labelTitle = modelName + "(" + carLineCode + ")" + " " + vdsCode
					+ " " + vinStartRange;
		}
		else
		{
			// MME MARKET
			labelTitle = modelName + "(" + carLineCode + ")" + " " + wmiCode +" "+ vdsCode
					+ " " + vinStartRange;
		}
			
		String wdLabel="";
		String ebLabel="";
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				wdLabel = CDRomManualsDAO.getManualTypeNameOnCode("WD", langCode);
				
				if(langCode.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
				langCode.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET - Use Engine Code as ENG
					ebLabel = CDRomManualsDAO.getManualTypeNameOnCode("ENG", langCode);
				}
				else
				{
					// MME MARKET - Use Engine Code as Engine
					ebLabel = CDRomManualsDAO.getManualTypeNameOnCode("ENGINE", langCode);
				}
			}
		}
		catch(Exception eq)
		{
			Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "createJson()", eq);
		}
		
		try {
			// keyword.js
			BufferedWriter fileWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(keywordjs),
							"UTF-8"));
			fileWriter.write("var labelTitle = " + '"' + labelTitle + '"'+";");
			fileWriter.newLine();
			fileWriter.write("var firstLevelUnique = new Array();");
			int k = 0;
//			for (String s : firstLevelCatList) {
//				fileWriter.newLine();
//				fileWriter.write("firstLevelUnique[" + k + "]=" + '"'
//						+ nullCheck(s) + '"' + ";");
//				k++;
//			}
			for(CategoryDetails catData : sortedFirstLevelCatList)
			{
				if(null!=abortCheck && abortCheck.isAborted()) { break; }
				fileWriter.newLine();
				fileWriter.write("firstLevelUnique[" + k + "]=" + '"'
						+ nullCheck(catData.getCategoryNameEng()) + '"' + ";");
				k++;
			}
			fileWriter.newLine();
			fileWriter.write("var manualTypes = new Array();");
			int m = 0;
			
			for(CDRomScheduleItemDetails itemDetails : itemsList)
			{
				if(null!=abortCheck && abortCheck.isAborted()) { break; }
				fileWriter.newLine();
				fileWriter.write("manualTypes[" + m + "]=" + '"'
						+ itemDetails.getManualTypeName() + '"' + ";");
				m++;
			}
//			for (String s : selectedManualTypesAll) {
//				fileWriter.newLine();
//				fileWriter.write("manualTypes[" + m + "]=" + '"'
//						+ getManualTypeName(searchBean, s) + '"' + ";");
//				m++;
//			}
			logger.info("Keyword json starts here");
			fileWriter.newLine();
			fileWriter.write("var keywords = new Array();");
			logger.info("-------- keywordSearchList :: >"+ keywordSearchList.size());
			for (int i = 0; i < keywordSearchList.size(); i++) {
				if(null!=abortCheck && abortCheck.isAborted()) { break; }
				
				String manualTypeConstantForBreadCrumb="SM";
				String manualTypeLabelForBreadCrumb="";
				if(null!=keywordSearchList.get(i).getManualType() && !"".equals(keywordSearchList.get(i).getManualType()))
				{
					if(keywordSearchList.get(i).getManualType().trim().toLowerCase().equals("wd")|| keywordSearchList.get(i).getManualType().trim().toLowerCase().equals("e-wd"))
					{
						// CHANGE MANUAL TYPE CONSTANT TO OSM
						manualTypeConstantForBreadCrumb = "OSM";
						if(null!=wdLabel && !"".equals(wdLabel))
						{
							manualTypeLabelForBreadCrumb = wdLabel;
						}
					}
					else
					{
						/*
						 * IDENTIFY THE MANUAL TYPE FOR DOCUMENT ID & LOCALE, WHY IT IS REQUIRED BECAUSE FOR ENG, AT & MT
						 * NOT SURE WHICH ONE IS WHAT
						 * SO IF MANUAL TYPE STARTS WITH EB OR MB, LOOK UP FOR DOCUMENT ID
						 * WHETHER ITS ENGINE WORKSHOP MANUAL OR AT MANUAL OR MT MANUAL
						 */
						if(keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("eb") || 
								keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("mb") || 
								keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("dm") || 
								keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("mcm") || 
								keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("rq"))
						{
							if(keywordSearchList.get(i).getManualType().trim().toLowerCase().startsWith("eb"))
							{
								// set engine workshop manual
								if(null!=ebLabel && !"".equals(ebLabel))
								{
									manualTypeLabelForBreadCrumb = ebLabel;
								}
							}
							else
							{
								// its either AT or MT OR DM / MCM / RQ
								try
								{
									manualTypeLabelForBreadCrumb = CDRomManualsDAO.getManualTypeOnDocumentIdAndLocale(keywordSearchList.get(i).getDocumentId(), langCode);
								}
								catch(Exception e1)
								{
									Utilities.printStackTraceToLogs(CDRomUtil.class.getName(), "createJSON()", e1);
								}
							}
						}
						else
						{
							manualTypeLabelForBreadCrumb = keywordSearchList.get(i).getManualTypeName();
						}
					}
				}
				fileWriter.newLine();
				fileWriter.write("keywords["
						+ i
						+ "] = new Array ("
						+ '"'
						+ quotesCheck(nullCheck(keywordSearchList.get(i)
								.getTitle())) + '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getDocumentId())
						+ '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getESICat1())
						+ '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getESICat2())
						+ '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getESICat3())
						+ '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getManualType())
						+ '"' + "," + '"'
						+ nullCheck(keywordSearchList.get(i).getFilePath())
						+ '"' + "," + '"'
						+ keywordSearchList.get(i).getManualTypeName() + '"'
						+ "," + '"'
						+ keywordSearchList.get(i).getGenerateDocument() + '"'
						+ "," + '"'
						+ nullCheck(manualTypeConstantForBreadCrumb) + '"'
						+ "," + '"'
						+ nullCheck(manualTypeLabelForBreadCrumb) + '"'
						+ ")");
				
				manualTypeConstantForBreadCrumb = null;
				manualTypeLabelForBreadCrumb = null;
			}
			fileWriter.flush();
			fileWriter.close();
			wdLabel = null;
			ebLabel = null;
			logger.info("Keyword json Ends here");

			Collections.sort(viewContentList, new ContentComparator());
			// content.js
			BufferedWriter fileWriter1 = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(contentjs),
							"UTF-8"));
			logger.info("Content json starts here");

			fileWriter1.write("var labelTitle = " + '"' + labelTitle + '"');
			fileWriter1.newLine();
			fileWriter1.write("var contents = new Array();");
			logger.info("-------- viewContentList :: >"+ viewContentList.size());
			for (int i = 0; i < viewContentList.size(); i++) {
				if(null!=abortCheck && abortCheck.isAborted()) { break; }
				fileWriter1.newLine();
				fileWriter1.write("contents["
						+ i
						+ "] = new Array ("
						+ '"'
						+ quotesCheck(nullCheck(viewContentList.get(i)
								.getTitle())) + '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDocumentId())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getSequencenumber())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getManualTypeName())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel1())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel2())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel3())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel4())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel5())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel6())
						+ '"' + "," + '"'
						+ nullCheck(viewContentList.get(i).getDisplayLevel7())
						+ '"' + "," + '"'
						+ viewContentList.get(i).getFilePath() + '"' + ","
						+ '"' + viewContentList.get(i).getGenerateDocument()
						+ '"' + ")");
			}
			fileWriter1.flush();
			fileWriter1.close();
			logger.info("Content json Ends here");
			// code for keyword json.
			/*
			 * BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new
			 * FileOutputStream(keywordJson), "UTF-8")); Gson gson = new Gson();
			 * String json = gson.toJson(keywordSearchList); fw.newLine();
			 * fw.write(json); fw.flush(); fw.close();
			 */

		} catch (Exception e) {
			logger.info("createJson :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"createJson()", e);
			logger.info("createJson :: ################ Exception ################");
		}
		sortedFirstLevelCatList = null;
	}

	public static String quotesCheck(String str) {
		// sstr = str.replaceAll("^.|.$", "");
		str = str.replaceAll("\"", "");
		return str;

	}

	public static String nullCheck(String str) {
		if (str != null && StringUtils.isBlank(str)) {
			return '"' + " " + '"';
		} else if (StringUtils.isBlank(str)) {
			return "";
		}
		return str;

	}
	
	
	public void generateZipFileErrorReport(String networkPath, String timeStamp, List<CDRomErrorDetails> errorsList) {
		try {
			
			String reportsPath = networkPath + "/CD_ROM_" + timeStamp + "_Reports";
			File path = new File(reportsPath);
			if (!path.exists()) {
				if (!path.mkdirs()) {
					logger.info("generateZipFileErrorReport :: Failed to create directory");

				}
			}
			
			File myFile = new File(reportsPath + "/ContentZipFailure.xlsx");
			
			// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
			SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);

			// Create a new sheet
			Sheet mySheet = myWorkBook.createSheet("Failure Details");
			/*
			 * Add Header Row
			 */
			
			if(null!=errorsList && errorsList.size()>0)
			{
				Row headerRow = mySheet.createRow(0);
				Cell wslCell = headerRow.createCell(0);
				wslCell.setCellValue("FILE_NAME");
				wslCell = null;
				wslCell = headerRow.createCell(1);
				wslCell.setCellValue("STATUS");
				wslCell = null;
				wslCell = headerRow.createCell(2);
				wslCell.setCellValue("ERROR_CODE");
				wslCell = null;
				wslCell = headerRow.createCell(3);
				wslCell.setCellValue("ERROR_MESSAGE");
				wslCell = null;
				
				int count=1;
				Row row=null;
				Cell dataCell = null;
				CDRomErrorDetails details = null;
				for(int a=0;a<errorsList.size();a++)
				{
					if(null!=abortCheck && abortCheck.isAborted()) { break; }
					details = (CDRomErrorDetails)errorsList.get(a);
					row = mySheet.createRow(count);
					// increment count by 1 
					count++;
					dataCell = row.createCell(0);
					dataCell.setCellValue("");
					if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
					{
						dataCell.setCellValue(details.getFilePath());
					}
					dataCell = null;
					dataCell = row.createCell(1);
					dataCell.setCellValue("FAILURE");
					
					dataCell = null;
					dataCell = row.createCell(2);
					dataCell.setCellValue("");
					if(null!=details.getErrorCode() && !"".equals(details.getErrorCode()))
					{
						dataCell.setCellValue(details.getErrorCode());
					}
					dataCell = null;
					dataCell = row.createCell(3);
					dataCell.setCellValue("");
					if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
					{
						dataCell.setCellValue(details.getErrorMessage());
					}
					dataCell = null;
					
					row = null;
					details = null;
				}
				headerRow = null;
				wslCell = null;
				row = null;
				dataCell = null;
			}
			else
			{
				Row headerRow = mySheet.createRow(0);
				Cell wslCell = headerRow.createCell(0);
				wslCell.setCellValue("Details");
				
				Row row = mySheet.createRow(1);
				Cell cell0 = row.createCell(0);
				cell0.setCellValue("Failed to Generate Content Zip File. Please reach out to IT Support Team for looking into the issue.");
				cell0 = null;
				row = null;
				headerRow = null;
				wslCell = null;
				
			}
			
			FileOutputStream os = new FileOutputStream(myFile);
			myWorkBook.write(os);
			logger.info("Writing on CONTENT ZIP FAILURE REPORT XLSX file Finished ...");
			os.flush();
			os.close();

			// set mySheet to null
			mySheet = null;
			// set myWorkBook to null
			myWorkBook = null;
			// set myFile to null
			myFile = null;
			reportsPath =  null;
		} catch (Exception e) {
			logger.info("generateZipFileErrorReport :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"generateZipFileErrorReport()", e);
			logger.info("generateZipFileErrorReport :: ################ Exception ################");
		}

	}
	
}