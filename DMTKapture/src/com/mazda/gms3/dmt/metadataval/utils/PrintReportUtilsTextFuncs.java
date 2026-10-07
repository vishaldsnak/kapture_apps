package com.mazda.gms3.dmt.metadataval.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class PrintReportUtilsTextFuncs {

	private Logger logger = LogManager.getLogger(PrintReportUtilsTextFuncs.class);
	
	public void printMCMMEVinTextReport(String scheduleCode , ArrayList<VinDetails> vinList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printMCMMEVinTextReport :: Method Starts.");
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
					
					String headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||MANUAL TYPE||FACELIFT FOLDER||";
					headers+="MATERIAL FOLDER||CARLINE CODE||WMI CODE||VDS CODE||VIS START RANGE||VIS END RANGE||VIN EXISTS IN MDM||";
					headers+="VIN REF KEY||VIN EXISTS IN IM||DISPLAY ORDER FILE||ERROR MESSAGE";
					
					StringBuilder str=  new StringBuilder();
					str.append(headers);
					str.append("\n");

					for (int a = 0; a < vinList.size(); a++) {
						VinDetails details = (VinDetails) vinList.get(a);
						
						str.append(ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"||");
						if(null!=details.getVinSourceFileName() && !"".equals(details.getVinSourceFileName()))
						{
							str.append(details.getVinSourceFileName()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinSorceFilePath() && !"".equals(details.getVinSorceFilePath()))
						{
							str.append(details.getVinSorceFilePath()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
						{
							str.append(details.getProcessingStatus()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if (null != locale && !"".equals(locale)) 
						{
							str.append(locale+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if (null != details.getManualType() && !"".equals(details.getManualType())) 
						{
							str.append(details.getManualType()+"||");
						}
						else
						{
							str.append(""+"||");
						}

						if (null != details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder())) 
						{
							str.append(details.getFaceLiftFolder()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if (null != details.getMaterialFolder() && !"".equals(details.getMaterialFolder())) 
						{
							str.append(details.getMaterialFolder()+"||");
						}
						else
						{
							str.append(""+"||");
						}

						if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
						{
							str.append(details.getCarlineCode()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							str.append(details.getWmiCode()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
						{
							str.append(details.getVdsCode()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
						{
							str.append(details.getVisStartRange()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
						{
							str.append(details.getVisEndRange()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinFoundInMDM() && !"".equals(details.getVinFoundInMDM()))
						{
							str.append(details.getVinFoundInMDM()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinRefKey() && !"".equals(details.getVinRefKey()))
						{
							str.append(details.getVinRefKey()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinFoundInIM() && !"".equals(details.getVinFoundInIM()))
						{
							str.append(details.getVinFoundInIM()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						
						if(null!=details.getDisplayOrderTextFileName() && !"".equals(details.getDisplayOrderTextFileName()))
						{
							str.append(details.getDisplayOrderTextFileName()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
						{
							str.append(details.getErrorMessage()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						str.append("\n");
						details = null;
					}

					headers=null;
					
					
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("VIN METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMCMMEVinTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printMCMMEVinTextReport :: Method Ends.");
	}

	public void printDisplayOrderTextReport(String scheduleCode , ArrayList<DisplayOrderDetails> displayOrderList, String locale) 
	{

		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderTextReport :: Method Starts.");
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
					 * Add Header Row
					 */
					String headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||MODEL FOLDER||MANUAL TYPE FOLDER||FACELIFT FOLDER||MATERIAL FOLDER||PROCESSING FOLDER||"
							+ "CONTENT FILE NAME||TITLE||DISP ORD CODE 1||DISP ORD NAME 1||DISP ORD CODE 2||DISP ORD NAME 2||DISP ORD CODE 3||DISP ORD NAME 3||"
							+ "DISP ORD CODE 4||DISP ORD NAME 4||DISP ORD CODE 5||DISP ORD NAME 5||DISP ORD CODE 6||DISP ORD NAME 6||SEQUENCE NO||ERROR MESSAGE||"
							+ "ENGINE TYPE||TRANSMISSION TYPE||"
							+ "DRIVEAXLE TYPE||BODY TYPE||MDM-FAILED ENGINE TYPE||IM-FAILED ENGINE TYPE||MDM-FAILED TRANSMISSION TYPE||IM-FAILED TRANSMISSION TYPE||"
							+ "MDM-FAILED DRIVEAXLE TYPE||IM-FAILED DRIVEAXLE TYPE||MDM-FAILED BODY TYPE||IM-FAILED BODY TYPE";
					
					StringBuilder str = new StringBuilder();
					str.append(headers);
					str.append("\n");
					String dispData="";
					DisplayOrderDetails details=null;
					for (int a = 0; a < displayOrderList.size(); a++) {
						details = (DisplayOrderDetails) displayOrderList.get(a);
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getDisplayOrderSourceFileName()+
								"<TOK_SEPARATOR>"+details.getDisplayOrderSourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>"+details.getModelFolderName()+"<TOK_SEPARATOR>"+
								details.getManualType()+"<TOK_SEPARATOR>";
						dispData+=details.getFaceLiftFolderName()+"<TOK_SEPARATOR>"+details.getMaterialFolderName()+"<TOK_SEPARATOR>"+details.getProcessingFolderName()+
								"<TOK_SEPARATOR>"+details.getFileName()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel1Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel2Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel3Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel4Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel4Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel5Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel5Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel6Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel6Name()+"<TOK_SEPARATOR>";
						dispData+=details.getSequenceNo()+"<TOK_SEPARATOR>"+details.getErrorMessage()+"<TOK_SEPARATOR>";
						dispData+=details.getEngineType()+"<TOK_SEPARATOR>"+details.getMissionType()+"<TOK_SEPARATOR>";
						dispData+=details.getDriveAxleType()+"<TOK_SEPARATOR>"+details.getBodyType()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedEngineTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedEngineTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedMissionTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedMissionTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedDriveAxleTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedDriveAxleTypeCodesIM()+"<TOK_SEPARATOR>";
						dispData+=details.getFailedBodyTypeCodesMDM()+"<TOK_SEPARATOR>"+details.getFailedBodyTypeCodesIM()+"<TOK_SEPARATOR>";
						
						
						dispData = dispData.replace("NULL", "");
						dispData = dispData.replace("null", "");
						dispData = dispData.replace("<TOK_SEPARATOR>", "||");
						str.append(dispData);
						str.append("\n");
						
						details = null;
						dispData=null;
					}
					details = null;
					dispData=null;
					
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("DISPLAY ORDER METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printDisplayOrderTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printDisplayOrderTextReport :: Method Ends.");
	}

	public void printCDProcessingTextReport(String scheduleCode , ArrayList<CDProcessingDetails> cdProcessingList, String locale) {

		if (logger.isInfoEnabled())
			logger.info("printCDProcessingTextReport :: Method Starts.");
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
					 * Add Header Row
					 */
					String headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||MODEL FOLDER||MANUAL TYPE FOLDER||FACELIFT FOLDER||MATERIAL FOLDER||PROCESSING FOLDER||"
							+ "CONTENT FILE NAME||TITLE||DISP ORD CODE 1||DISP ORD NAME 1||DISP ORD CODE 2||DISP ORD NAME 2||DISP ORD CODE 3||DISP ORD NAME 3||"
							+ "DISP ORD CODE 4||DISP ORD NAME 4||DISP ORD CODE 5||DISP ORD NAME 5||DISP ORD CODE 6||DISP ORD NAME 6||SEQUENCE NO||ERROR MESSAGE||"
							+ "ENGINE TYPE||TRANSMISSION TYPE||"
							+ "DRIVEAXLE TYPE||BODY TYPE";
					StringBuilder str = new StringBuilder();
					str.append(headers);
					str.append("\n");
					
					String dispData=null;
					CDProcessingDetails details=null;
					for (int a = 0; a < cdProcessingList.size(); a++) {
						details = (CDProcessingDetails) cdProcessingList.get(a);
						
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getCdProcessingSourceFileName()+
								"<TOK_SEPARATOR>"+details.getCdProcessingSourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>"+details.getModelFolderName()+"<TOK_SEPARATOR>"+
								details.getManualType()+"<TOK_SEPARATOR>";
						dispData+=details.getFaceLiftFolderName()+"<TOK_SEPARATOR>"+details.getMaterialFolderName()+"<TOK_SEPARATOR>"+details.getProcessingFolderName()+
								"<TOK_SEPARATOR>"+details.getFileName()+"<TOK_SEPARATOR>"+details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel1Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel2Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel3Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel4Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel4Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel5Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel5Name()+"<TOK_SEPARATOR>";
						dispData+=details.getDisplayOrderLevel6Code()+"<TOK_SEPARATOR>"+details.getDisplayOrderLevel6Name()+"<TOK_SEPARATOR>";
						dispData+=details.getSequenceNo()+"<TOK_SEPARATOR>"+details.getErrorMessage()+"<TOK_SEPARATOR>";
						dispData+=details.getEngineType()+"<TOK_SEPARATOR>"+details.getMissionType()+"<TOK_SEPARATOR>";
						dispData+=details.getDriveAxleType()+"<TOK_SEPARATOR>"+details.getBodyType();
						
						dispData = dispData.replace("NULL", "");
						dispData = dispData.replace("null", "");
						dispData=dispData.replace("<TOK_SEPARATOR>", "||");
						str.append(dispData);
						str.append("\n");
						details = null;
						dispData=null;
					}
					details = null;
					dispData=null;
					
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("CD PROCESSING METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printCDProcessingTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printCDProcessingTextReport :: Method Ends.");
	}
	
	public void printESICategoryTextReport(String scheduleCode , ArrayList<ESICategoryDetails> esiCategoryList, String locale, String metaDataType) {

		if (logger.isInfoEnabled())
			logger.info("printESICategoryTextReport :: Method Starts.");
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
					 * Add Header Row
					 */
					String headers="";
					headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||FACELIFT FOLDER||MATERIAL FOLDER||PROCESSING FOLDER||"
							+ "CONTENT FILE NAME||TITLE||LEVEL1-CODE||LEVEL1-NAME||LEVEL1-EXISTS_MDM||LEVEL1-REFKEY||LEVEL1-EXISTS_IM||";
					headers+="LEVEL2-CODE||LEVEL2-NAME||LEVEL2-EXISTS_MDM||LEVEL2-REFKEY||LEVEL2-EXISTS_IM||";
					headers+="LEVEL3-CODE||LEVEL3-NAME||LEVEL3-EXISTS_MDM||LEVEL3-REFKEY||LEVEL3-EXISTS_IM||";
					if(metaDataType.equals("ESICATEGORY_WD"))
					{
						headers+="STEERING TYPE||IM-FAILED_STEERING TYPE||";
					}
					headers+="ERROR MESSAGE";
					
					StringBuilder str = new StringBuilder();
					str.append(headers);
					str.append("\n");
					
					String dispData=null;
					ESICategoryDetails details=null;
					for (int a = 0; a < esiCategoryList.size(); a++) {
						details = (ESICategoryDetails) esiCategoryList.get(a);
						
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
						
						if(metaDataType.equals("ESICATEGORY_WD"))
						{
							dispData+=details.getSteeringTypeInfoText()+"<TOK_SEPARATOR>"+details.getFailedSteeringTypeInIM()+"<TOK_SEPARATOR>";
						}
						
						dispData+=details.getErrorMessage();
						dispData = dispData.replace("NULL", "");
						dispData = dispData.replace("null", "");
						dispData = dispData.replace("<TOK_SEPARATOR>", "||");
						str.append(dispData);
						str.append("\n");
						details = null;
						dispData=null;
					}
					details = null;
					dispData=null;
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("ESI CATEGORY METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printESICategoryTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printESICategoryTextReport :: Method Ends.");
	}
	

	public void printLeftMenuTextReport(String scheduleCode , ArrayList<LeftMenuFileDetails> leftMenuList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printLeftMenuTextReport :: Method Starts.");
		try {
			if (null != leftMenuList && leftMenuList.size() > 0) 
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
					 * Add Header Row
					 */
					String headers="";
					headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||CONTENT FILE NAME||VTOC FILE NAME||"
							+ "TITLE||LEVEL1-CODE||LEVEL1-NAME||LEVEL1-EXISTS_MDM||LEVEL1-REFKEY||LEVEL1-EXISTS_IM||";
					headers+="LEVEL2-CODE||LEVEL2-NAME||LEVEL2-EXISTS_MDM||LEVEL2-REFKEY||LEVEL2-EXISTS_IM||";
					headers+="LEVEL3-CODE||LEVEL3-NAME||LEVEL3-EXISTS_MDM||LEVEL3-REFKEY||LEVEL3-EXISTS_IM||";
					headers+="ERROR MESSAGE";
					
					StringBuilder str = new StringBuilder();
					str.append(headers);
					str.append("\n");
					
					String dispData=null;
					LeftMenuFileDetails details=null;
					for (int a = 0; a < leftMenuList.size(); a++) {
						details = (LeftMenuFileDetails) leftMenuList.get(a);
						
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getLeftMenuSourceFileName()+
								"<TOK_SEPARATOR>"+details.getLeftMenuSourceFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>";
						dispData+=details.getFileName()+"<TOK_SEPARATOR>"+details.getVtocFileNameAttribute()+"<TOK_SEPARATOR>"+ details.getTitle()+"<TOK_SEPARATOR>";
						dispData+=details.getCategoryCode()+"<TOK_SEPARATOR>"+details.getCategoryLevel1Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode1ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode1RefKey()+"<TOK_SEPARATOR>"+details.getCode1ExistsInIM()+"<TOK_SEPARATOR>";
						dispData+=details.getSubCategoryCode()+"<TOK_SEPARATOR>"+details.getCategoryLevel2Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode2ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode2RefKey()+"<TOK_SEPARATOR>"+details.getCode2ExistsInIM()+"<TOK_SEPARATOR>";
						dispData+=details.getSubSubCategoryCode()+"<TOK_SEPARATOR>"+details.getCategoryLevel3Name()+"<TOK_SEPARATOR>";
						dispData+=details.getCode3ExistsInMDM()+"<TOK_SEPARATOR>"+details.getCode3RefKey()+"<TOK_SEPARATOR>"+details.getCode3ExistsInIM()+"<TOK_SEPARATOR>";
						dispData+=details.getErrorMessage();
						dispData = dispData.replace("NULL", "");
						dispData = dispData.replace("null", "");
						dispData = dispData.replace("<TOK_SEPARATOR>", "||");
						str.append(dispData);
						str.append("\n");
						details = null;
						dispData=null;
					}
					details = null;
					dispData=null;
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("LEFTMENU METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printLeftMenuTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printLeftMenuTextReport :: Method Ends.");
	}

	public void printMNAOVinTextReport(String scheduleCode , ArrayList<VINEntFileDetails> vinList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printMNAOVinTextReport :: Method Starts.");
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
					
					String headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||";
					headers+="CARLINE CODE||WMI CODE||VDS CODE||VIS START RANGE||VIS END RANGE||VIN EXISTS IN MDM||";
					headers+="VIN REF KEY||VIN EXISTS IN IM||MODEL YEAR||ERROR MESSAGE";
					
					StringBuilder str=  new StringBuilder();
					str.append(headers);
					str.append("\n");

					for (int a = 0; a < vinList.size(); a++) {
						VINEntFileDetails details = (VINEntFileDetails) vinList.get(a);
						
						str.append(ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"||");

						if(null!=details.getFileName() && !"".equals(details.getFileName()))
						{
							str.append(details.getFileName()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
						{
							str.append(details.getFilePath()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getProcessingStatus() && !"".equals(details.getProcessingStatus()))
						{
							str.append(details.getProcessingStatus()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if (null != locale && !"".equals(locale)) 
						{
							str.append(locale+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if (null != details.getVinCarline() && !"".equals(details.getVinCarline())) 
						{
							str.append(details.getVinCarline()+"||");
						}
						else
						{
							str.append(""+"||");
						}

						if(null!=details.getVinWMI() && !"".equals(details.getVinWMI()))
						{
							str.append(details.getVinWMI()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinVDS() && !"".equals(details.getVinVDS()))
						{
							str.append(details.getVinVDS()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinStartRange() && !"".equals(details.getVinStartRange()))
						{
							str.append(details.getVinStartRange()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()))
						{
							str.append(details.getVinEndRange()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinFoundInMDM() && !"".equals(details.getVinFoundInMDM()))
						{
							str.append(details.getVinFoundInMDM()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinRefKey() && !"".equals(details.getVinRefKey()))
						{
							str.append(details.getVinRefKey()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getVinFoundInIM() && !"".equals(details.getVinFoundInIM()))
						{
							str.append(details.getVinFoundInIM()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						
						if(null!=details.getModelYear() && !"".equals(details.getModelYear()))
						{
							str.append(details.getModelYear()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
						{
							str.append(details.getErrorMessage()+"||");
						}
						else
						{
							str.append(""+"||");
						}
						
						str.append("\n");
						details = null;
					}

					headers=null;
					
					
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("VIN METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printMNAOVinTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printMNAOVinTextReport :: Method Ends.");
	}

	public void printVinAttributeTextReport(String scheduleCode , ArrayList<VinAttributeEntFileDetails> vinAttributeList, String locale) {
		if (logger.isInfoEnabled())
			logger.info("printVinAttributeTextReport :: Method Starts.");
		try {
			if (null != vinAttributeList && vinAttributeList.size() > 0) 
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
					 * Add Header Row
					 */
					String headers="SCHEDULE NAME||FILE NAME||FILE PATH||PROCESSING STATUS||LOCALE||"
							+ "ENGINE TYPE||TRANSMISSION TYPE||"
							+ "DRIVEAXLE TYPE||BODY TYPE||ERROR MESSAGE";
					
					StringBuilder str = new StringBuilder();
					str.append(headers);
					str.append("\n");
					String dispData="";
					VinAttributeEntFileDetails details=null;
					for (int a = 0; a < vinAttributeList.size(); a++) {
						details = (VinAttributeEntFileDetails) vinAttributeList.get(a);
						dispData=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode+"<TOK_SEPARATOR>"+details.getFileName()+
								"<TOK_SEPARATOR>"+details.getFilePath()+"<TOK_SEPARATOR>";
						dispData+=details.getProcessingStatus()+"<TOK_SEPARATOR>"+locale+"<TOK_SEPARATOR>";
						dispData+=details.getVinEngineType()+"<TOK_SEPARATOR>"+details.getVinTransType()+"<TOK_SEPARATOR>";
						dispData+=details.getVinAxleType()+"<TOK_SEPARATOR>"+details.getVinBodyType()+"<TOK_SEPARATOR>";
						dispData+=details.getErrorMessage();
						
						
						dispData = dispData.replace("NULL", "");
						dispData = dispData.replace("null", "");
						dispData = dispData.replace("<TOK_SEPARATOR>", "||");
						str.append(dispData);
						str.append("\n");
						
						details = null;
						dispData=null;
					}
					details = null;
					dispData=null;
					
					/*
					 * iterate Map and write the Excel File for each Index
					 */
					String fName = "/" + scheduleCode+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
					File myFile = PathUtil.file(path + fName);
					fName = null;
					FileOutputStream os = PathUtil.fileOutputStream(myFile);
					os.write(str.toString().getBytes("utf-8"));
					logger.info("VIN ATTRIBUTE METADATA FAILURE REPORT TEXT file Finished ...");
					os.flush();
					os.close();

					// set path to null
					path = null;
					// set myFile to null
					myFile = null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PrintReportsUtil.class.getName(), "printVinAttributeTextReport()", e);
		}
		if (logger.isInfoEnabled())
			logger.info("printVinAttributeTextReport :: Method Ends.");
	}

}
