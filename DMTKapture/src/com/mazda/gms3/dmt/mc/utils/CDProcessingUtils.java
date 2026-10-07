package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.dao.MCDocumentManagementDAO;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.utils.Utilities;

public class CDProcessingUtils {

	private static Logger logger = LogManager.getLogger(CDProcessingUtils.class);
	
	/**
	 * Function will read the CD Processing Data Identified in the FACE LIFT FOLDER
	 * @param cdProcessingFile
	 * @param itemDetails
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<CDProcessingDetails> readCDProcessingTextFile(File cdProcessingFile, String modelType, String identifyName) throws IOException
	{
		BufferedReader br = null;
		ArrayList<CDProcessingDetails> cdProcessingDataList = null;
		try
		{
			/*
			 * RULES FOR DISPLAY ORDER
			 * WHEN NOT ENGINE AT / MT (FOR ALL MODEL TYPES & CHANNELS)
			 * 
			 * 	THEN LINE WILL BE - 
			 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
			 * 	EngineType||Transmission Type||BodyType||DriveAxleType||Navigation LevelCode1||Navigation Level Code 2||
			 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
			 */
			
			if(null!=cdProcessingFile && cdProcessingFile.exists() && cdProcessingFile.isFile())
			{
				String localeFolderName = cdProcessingFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(cdProcessingFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    CDProcessingDetails details = new CDProcessingDetails();
			    String[] tokens=null;
			    String sequenceNo=null;
			    String filePath=null;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
//			    		content = content.replace("?", "１");
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}


			    		// IDENTIFY SEQUENCE NO, IT WILL ALWAYS BE THE VALUE AFTER LAST INDEX OF ||
			    		sequenceNo="";
			    		if(line.lastIndexOf("||")!=-1)
			    		{
			    			sequenceNo = line.substring(line.lastIndexOf("||")+2, line.length());
			    			// Now remove the last Index from LINE, since it is already read
			    			line = line.substring(0, line.lastIndexOf("||"));
			    		}

			    		details = new CDProcessingDetails();
			    		if(null!=sequenceNo && !"".equals(sequenceNo))
			    		{
			    			details.setSequenceNo(sequenceNo);
			    		}
			    		// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
			    		//	<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||EngineType||Transmission Type||BodyType||DriveAxleType||
			    		//	<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
			    		// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
			    		if(null!=line && !"".equals(line))
			    		{
			    			tokens = line.split("\\|\\|");

			    			// CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
			    			if(null!=tokens && tokens.length>0)
			    			{
			    				try
			    				{
			    					if(tokens.length>=0)
			    					{
			    						if(null!=tokens[0] && !"".equals(tokens[0]))
			    						{
			    							// set MODEL FOLDER NAME
			    							details.setModelFolderName(String.valueOf(tokens[0]));
			    						}
			    					}
			    				}
			    				catch (Exception e) {
			    				}

			    				try
			    				{
			    					if(tokens.length>=1)
			    					{
			    						if(null!=tokens[1] && !"".equals(tokens[1]))
			    						{
			    							// set MANUAL TYPE FOLDER NAME
			    							details.setManualType(String.valueOf(tokens[1]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=2)
			    					{
			    						if(null!=tokens[2] && !"".equals(tokens[2]))
			    						{
			    							// set FACE LIFT FOLDER NAME
			    							details.setFaceLiftFolderName(String.valueOf(tokens[2]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=3)
			    					{
			    						if(null!=tokens[3] && !"".equals(tokens[3]))
			    						{
			    							// set MATERIAL FOLDER NAME
			    							details.setMaterialFolderName(String.valueOf(tokens[3]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=4)
			    					{
			    						if(null!=tokens[4] && !"".equals(tokens[4]))
			    						{
			    							// set PROCESSING FOLDER NAME
			    							details.setProcessingFolderName(String.valueOf(tokens[4]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=5)
			    					{
			    						if(null!=tokens[5] && !"".equals(tokens[5]))
			    						{
			    							// set FILE NAME
			    							details.setFileName(String.valueOf(tokens[5]));
			    							/*
				    			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
				    			    		 * DO THIS ONLY FOR MODEL 
				    			    		 */
				    			    		details.setFileName(Utilities.replaceJunkCharacterToFullbyte(details.getFileName(), details.getModelFolderName(), details.getManualType(), 
				    			    				details.getFaceLiftFolderName(), details.getMaterialFolderName(), localeFolderName));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}
			    				
			    				try{
			    					if(tokens.length>=6)
			    					{
			    						if(null!=tokens[6] && !"".equals(tokens[6]))
			    						{
			    							// set TITLE
			    							details.setTitle(String.valueOf(tokens[6]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=7)
			    					{
			    						if(null!=tokens[7] && !"".equals(tokens[7]))
			    						{
			    							// set ENGINE TYPE
			    							details.setEngineType(String.valueOf(tokens[7]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=8)
			    					{
			    						if(null!=tokens[8] && !"".equals(tokens[8]))
			    						{
			    							// set MISSION TYPE
			    							details.setMissionType(String.valueOf(tokens[8]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=9)
			    					{
			    						if(null!=tokens[9] && !"".equals(tokens[9]))
			    						{
			    							// set BODY TYPE
			    							details.setBodyType(String.valueOf(tokens[9]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=10)
			    					{
			    						if(null!=tokens[10] && !"".equals(tokens[10]))
			    						{
			    							// set DRIVE AXLE TYPE
			    							details.setDriveAxleType(String.valueOf(tokens[10]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=11)
			    					{
			    						if(null!=tokens[11] && !"".equals(tokens[11]))
			    						{
			    							// set DISPLAY ORDER LEVEL 1 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel1Name(String.valueOf(tokens[11]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=12)
			    					{
			    						if(null!=tokens[12] && !"".equals(tokens[12]))
			    						{
			    							// set DISPLAY ORDER LEVEL 2 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel2Name(String.valueOf(tokens[12]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=13)
			    					{
			    						if(null!=tokens[13] && !"".equals(tokens[13]))
			    						{
			    							// set DISPLAY ORDER LEVEL 3 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel3Name(String.valueOf(tokens[13]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=14)
			    					{
			    						if(null!=tokens[14] && !"".equals(tokens[14]))
			    						{
			    							// set DISPLAY ORDER LEVEL 4 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel4Name(String.valueOf(tokens[14]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=15)
			    					{
			    						if(null!=tokens[15] && !"".equals(tokens[15]))
			    						{
			    							// set DISPLAY ORDER LEVEL 5 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel5Name(String.valueOf(tokens[15]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=16)
			    					{
			    						if(null!=tokens[16] && !"".equals(tokens[16]))
			    						{
			    							// set DISPLAY ORDER LEVEL 6 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel6Name(String.valueOf(tokens[16]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}
			    			}
			    			tokens=  null;
			    		}

			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName))
			    		{
			    			if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
			    					&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
			    					null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+details.getModelFolderName()+"\\"+details.getManualType()+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    			}
			    		}

			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(ConversionUtils.LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(ConversionUtils.LINE_TYPE_INVALID);
			    		}

			    		details.setCdProcessingSourceFileName(cdProcessingFile.getName());
			    		details.setCdProcessingSourceFilePath(PathUtil.winPath(cdProcessingFile));
			    		if(null==cdProcessingDataList || cdProcessingDataList.size()<=0)
			    		{
			    			cdProcessingDataList = new ArrayList<CDProcessingDetails>();
			    		}
			    		// add details to ESI CATEGORY LIST
			    		cdProcessingDataList.add(details);
			    		details= null;
			    		filePath=null;
			    	}
			    }
			    line = null;
			    filePath = null;
			    details= null;
			    tokens = null;
			    sequenceNo = null;
				/*
				 * GET DISPLAY ORDER NAMES FOR ALL DISPLAY ORDER CODES
				 */
			    if(null!=cdProcessingDataList && cdProcessingDataList.size()>0)
			    {
			    	cdProcessingDataList = MCDocumentManagementDAO.getDisplayOrderNameForCDProcessing(cdProcessingDataList, modelType, localeFolderName, identifyName);
			    }
			    localeFolderName = null;
			}
			else
			{
				logger.info("readCDProcessingTextFile :: CD PROCESSING Text file is either Corrupted or Does not Exist.");
				cdProcessingDataList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDProcessingUtils.class.getName(), "readCDProcessingTextFile()", e);
//			cdProcessingDataList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return cdProcessingDataList;
	}

	/**
	 * Function will read the CD Processing Data Identified in the FACE LIFT FOLDER FOR ENGINE / AT / MT
	 * @param cdProcessingFile
	 * @param modelType
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<CDProcessingDetails> readCDProcessingTextFileForEngineATMT(File cdProcessingFile,String modelType, String identifyName) throws IOException
	{
		BufferedReader br = null;
		ArrayList<CDProcessingDetails> cdProcessingDataList = null;
		try
		{
			/*
			 * DISPLAY ORDER FORMAT
			 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
			 * 	Navigation LevelCode1||Navigation Level Code 2||
			 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
			 */
			
			if(null!=cdProcessingFile && cdProcessingFile.exists() && cdProcessingFile.isFile())
			{
				String localeFolderName = cdProcessingFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(cdProcessingFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    String sequenceNo=null;
			    CDProcessingDetails details = new CDProcessingDetails();
			    String[] tokens=null;
			    String filePath=null;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}


			    		// IDENTIFY SEQUENCE NO, IT WILL ALWAYS BE THE VALUE AFTER LAST INDEX OF ||
			    		sequenceNo="";
			    		if(line.lastIndexOf("||")!=-1)
			    		{
			    			sequenceNo = line.substring(line.lastIndexOf("||")+2, line.length());
			    			// Now remove the last Index from LINE, since it is already read
			    			line = line.substring(0, line.lastIndexOf("||"));
			    		}

			    		details = new CDProcessingDetails();
			    		if(null!=sequenceNo && !"".equals(sequenceNo))
			    		{
			    			details.setSequenceNo(sequenceNo);
			    		}
			    		// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
			    		//	<PROCESSING FOLDERNAME>||<FILENAME>||<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
			    		// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
			    		if(null!=line && !"".equals(line))
			    		{
			    			tokens = line.split("\\|\\|");
			    			if(null!=tokens && tokens.length>0)
			    			{
			    				// WITHOUT ENGINE TYPE, MISSION TYPE, DRIVE AXLE AND BODY TYPE
			    				if(null!=tokens && tokens.length>0)
			    				{
			    					try
				    				{
				    					if(tokens.length>=0)
				    					{
				    						if(null!=tokens[0] && !"".equals(tokens[0]))
				    						{
				    							// set MODEL FOLDER NAME
				    							details.setModelFolderName(String.valueOf(tokens[0]));
				    						}
				    					}
				    				}
				    				catch (Exception e) {
				    				}

				    				try
				    				{
				    					if(tokens.length>=1)
				    					{
				    						if(null!=tokens[1] && !"".equals(tokens[1]))
				    						{
				    							// set MANUAL TYPE FOLDER NAME
				    							details.setManualType(String.valueOf(tokens[1]));
				    						}
				    					}
				    				}
				    				catch(Exception e){
				    				}

			    					try
			    					{
			    						if(tokens.length>=2)
			    						{
			    							if(null!=tokens[2] && !"".equals(tokens[2]))
			    							{
			    								// set FACE LIFT FOLDER NAME
			    								details.setFaceLiftFolderName(String.valueOf(tokens[2]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try
			    					{
			    						if(tokens.length>=3)
			    						{
			    							if(null!=tokens[3] && !"".equals(tokens[3]))
			    							{
			    								// set MATERIAL FOLDER NAME
			    								details.setMaterialFolderName(String.valueOf(tokens[3]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try
			    					{
			    						if(tokens.length>=4)
			    						{
			    							if(null!=tokens[4] && !"".equals(tokens[4]))
			    							{
			    								// set PROCESSING FOLDER NAME
			    								details.setProcessingFolderName(String.valueOf(tokens[4]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=5)
			    						{
			    							if(null!=tokens[5] && !"".equals(tokens[5]))
			    							{
			    								// set FILE NAME
			    								details.setFileName(String.valueOf(tokens[5]));
			    								/*
					    			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
					    			    		 * DO THIS ONLY FOR MODEL 
					    			    		 */
					    			    		details.setFileName(Utilities.replaceJunkCharacterToFullbyte(details.getFileName(), details.getModelFolderName(), details.getManualType(), 
					    			    				details.getFaceLiftFolderName(), details.getMaterialFolderName(), localeFolderName));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}
			    					
			    					try{
			    						if(tokens.length>=6)
			    						{
			    							if(null!=tokens[6] && !"".equals(tokens[6]))
			    							{
			    								// set TITLE
			    								details.setTitle(String.valueOf(tokens[6]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=7)
			    						{
			    							if(null!=tokens[7] && !"".equals(tokens[7]))
			    							{
			    								// set DISPLAY ORDER LEVEL 1 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel1Name(String.valueOf(tokens[7]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=8)
			    						{
			    							if(null!=tokens[8] && !"".equals(tokens[8]))
			    							{
			    								// set DISPLAY ORDER LEVEL 2 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel2Name(String.valueOf(tokens[8]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=9)
			    						{
			    							if(null!=tokens[9] && !"".equals(tokens[9]))
			    							{
			    								// set DISPLAY ORDER LEVEL 3 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel3Name(String.valueOf(tokens[9]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=10)
			    						{
			    							if(null!=tokens[10] && !"".equals(tokens[10]))
			    							{
			    								// set DISPLAY ORDER LEVEL 4 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel4Name(String.valueOf(tokens[10]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=11)
			    						{
			    							if(null!=tokens[11] && !"".equals(tokens[11]))
			    							{
			    								// set DISPLAY ORDER LEVEL 5 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel5Name(String.valueOf(tokens[11]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=12)
			    						{
			    							if(null!=tokens[12] && !"".equals(tokens[12]))
			    							{
			    								// set DISPLAY ORDER LEVEL 6 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel6Name(String.valueOf(tokens[12]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}
			    				}
			    			}
			    			tokens=  null;
			    		}

			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName))
			    		{
			    			if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
			    					&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
			    					null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+details.getModelFolderName()+"\\"+details.getManualType()+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    			}
			    		}

			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(ConversionUtils.LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(ConversionUtils.LINE_TYPE_INVALID);
			    		}

			    		details.setCdProcessingSourceFileName(cdProcessingFile.getName());
			    		details.setCdProcessingSourceFilePath(PathUtil.winPath(cdProcessingFile));
			    		if(null==cdProcessingDataList || cdProcessingDataList.size()<=0)
			    		{
			    			cdProcessingDataList = new ArrayList<CDProcessingDetails>();
			    		}
			    		// add details to ESI CATEGORY LIST
			    		cdProcessingDataList.add(details);
			    		details= null;
			    		filePath = null;
			    	}
			    }
			    line = null;
			    details=null;
			    tokens=null;
			    filePath = null;
			    sequenceNo = null;
				/*
				 * GET DISPLAY ORDER NAMES FOR ALL DISPLAY ORDER CODES
				 */
			    if(null!=cdProcessingDataList && cdProcessingDataList.size()>0)
			    {
			    	cdProcessingDataList = MCDocumentManagementDAO.getDisplayOrderNameForCDProcessing(cdProcessingDataList, modelType, localeFolderName, identifyName);
			    }
			   localeFolderName= null;
			}
			else
			{
				logger.info("readCDProcessingTextFileForEngineATMT :: CD PROCESSING Text file is either Corrupted or Does not Exist.");
				cdProcessingDataList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDProcessingDetails.class.getName(), "readCDProcessingTextFileForEngineATMT()", e);
//			cdProcessingDataList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return cdProcessingDataList;
	}

}
