package com.mazda.gms3.dmt.metadataval.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.ParseXMLDoc;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class MetaDataValidationUtils {

	private static Logger logger = LogManager.getLogger(MetaDataValidationUtils.class);
	
	public static final String LINE_TYPE_VALID="VALID";
	public static final String LINE_TYPE_INVALID="INVALID";
	
	
	/**
	 * Function will read all the Information from ESI CATEGORY TEXT FILE FOR MC AS WELL AS MME MARKET
	 * @param esiCategoryFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ESICategoryDetails> readESICategoryTextFile(File esiCategoryFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<ESICategoryDetails> esiCategoryDetailsList = null;

		if(null!=esiCategoryFile && esiCategoryFile.exists() && esiCategoryFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(esiCategoryFile.getAbsoluteFile()), "UTF-8"));

			ESICategoryDetails details = new ESICategoryDetails();
			String[] tokens = null;
			String line;
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
					if(line.endsWith(";"))
					{
						line = line.substring(0, line.length()-1);
					}
					details = new ESICategoryDetails();
					// <FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<CATEGORYLEVEL 1 CODE>||<CATEGORYLEVEL 3 CODE>||<CATEGORYLEVEL 2 CODE>
					tokens = line.split("\\|\\|");
					if(null!=tokens && tokens.length>0)
					{
						try
						{
							if(tokens.length>=0)
							{
								if(null!=tokens[0] && !"".equals(tokens[0]))
								{
									// set FACELIFT FOLDER NAME
									details.setFaceLiftFolderName(String.valueOf(tokens[0]));
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
									// set MATERIAL FOLDER NAME
									details.setMaterialFolderName(String.valueOf(tokens[1]));
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
									// set PROCESSING FOLDER NAME
									details.setProcessingFolderName(String.valueOf(tokens[2]));
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
									// set FILE NAME
									details.setFileName(String.valueOf(tokens[3]));
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
									// set TITLE
									details.setTitle(String.valueOf(tokens[4]));
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
									// set CATEGORY LEVLE 1 CODE
									details.setCategoryLevel1Code(String.valueOf(tokens[5]));
									details.setCategoryLevel1Code(details.getCategoryLevel1Code().trim());
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
									// set CATEGORY LEVEL 2 CODE
									details.setCategoryLevel2Code(String.valueOf(tokens[6]));
									details.setCategoryLevel2Code(details.getCategoryLevel2Code().trim());
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
									// set CATEGORY LEVEL 3 CODE
									details.setCategoryLevel3Code(String.valueOf(tokens[7]));
									if(null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code()))
									{
										details.setCategoryLevel3Code(details.getCategoryLevel3Code().trim());
										if(details.getCategoryLevel3Code().endsWith(";"))
										{
											details.setCategoryLevel3Code(details.getCategoryLevel3Code().substring(0, details.getCategoryLevel3Code().length()-1));
										}
									}
								}
							}
						}
						catch(Exception e){
						}

					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */
					if(null!=details && null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
							&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
							&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
							&& null!=details.getFileName() && !"".equals(details.getFileName()))
					{
						details.setLineType(LINE_TYPE_VALID);
					}
					else
					{
						details.setLineType(LINE_TYPE_INVALID);
					}

					details.setEsiCategorySourceFileName(esiCategoryFile.getName());
					details.setEsiCategorySourceFilePath(PathUtil.winPath(esiCategoryFile));
					if(null==esiCategoryDetailsList || esiCategoryDetailsList.size()<=0)
					{
						esiCategoryDetailsList = new ArrayList<ESICategoryDetails>();
					}
					// add details to ESI CATEGORY LIST
					esiCategoryDetailsList.add(details);
					tokens=  null;
					details= null;
				}
			}
			line = null;
			details=null;
			tokens=null;
		}
		else
		{
			logger.info("readESICategoryTextFile :: ESI Category Text file is either Corrupted or Does not Exist.");
			esiCategoryDetailsList = null;
		}

		if(null!=br)
		{
			br.close();
		}
		return esiCategoryDetailsList;
	}

	/**
	 * Function will read the ESI CATEGORY TEXT FILE SPECIFICALLY FOR MME MARKET WHEN PROCESSING WIRING DIAGRAMS
	 * @param esiCategoryFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ESICategoryDetails> readESICategoryTextFileForMME_WD(File esiCategoryFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<ESICategoryDetails> esiCategoryDetailsList = null;

		if(null!=esiCategoryFile && esiCategoryFile.exists() && esiCategoryFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(esiCategoryFile.getAbsoluteFile()), "UTF-8"));
			ESICategoryDetails details = new ESICategoryDetails();
			String[] tokens= null;
			String line;
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
					if(line.endsWith(";"))
					{
						line = line.substring(0, line.length()-1);
					}
					details = new ESICategoryDetails();
					// <FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<STEERING TYPE1, STEERING TYPE 2>||<CATEGORYLEVEL 1 CODE>||<CATEGORYLEVEL 3 CODE>||<CATEGORYLEVEL 2 CODE>
					tokens = line.split("\\|\\|");
					if(null!=tokens && tokens.length>0)
					{
						try
						{
							if(tokens.length>=0)
							{
								if(null!=tokens[0] && !"".equals(tokens[0]))
								{
									// set FACELIFT FOLDER NAME
									details.setFaceLiftFolderName(String.valueOf(tokens[0]));
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
									// set MATERIAL FOLDER NAME
									details.setMaterialFolderName(String.valueOf(tokens[1]));
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
									// set PROCESSING FOLDER NAME
									details.setProcessingFolderName(String.valueOf(tokens[2]));
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
									// set FILE NAME
									details.setFileName(String.valueOf(tokens[3]));
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
									// set TITLE
									details.setTitle(String.valueOf(tokens[4]));
								}
							}
						}
						catch(Exception e){
						}

						try
						{
							if(tokens.length>=5)
							{
								if(null!=tokens[5] && !"".equals(tokens[5]))
								{
									// set STEERING TYPES
									details.setSteeringTypeInfoText(String.valueOf(tokens[5]));
									details.setSteeringTypeInfoText(details.getSteeringTypeInfoText().trim());
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
									// set CATEGORY LEVLE 1 CODE
									details.setCategoryLevel1Code(String.valueOf(tokens[6]));
									details.setCategoryLevel1Code(details.getCategoryLevel1Code().trim());
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
									// set CATEGORY LEVEL 2 CODE
									details.setCategoryLevel2Code(String.valueOf(tokens[7]));
									details.setCategoryLevel2Code(details.getCategoryLevel2Code().trim());
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
									// set CATEGORY LEVEL 3 CODE
									details.setCategoryLevel3Code(String.valueOf(tokens[8]));
									if(null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code()))
									{
										details.setCategoryLevel3Code(details.getCategoryLevel3Code().trim());
										if(details.getCategoryLevel3Code().endsWith(";"))
										{
											details.setCategoryLevel3Code(details.getCategoryLevel3Code().substring(0, details.getCategoryLevel3Code().length()-1));
										}
									}
								}
							}
						}
						catch(Exception e){
						}

					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */
					if(null!=details && null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
							&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
							&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
							&& null!=details.getFileName() && !"".equals(details.getFileName()))
					{
						details.setLineType(LINE_TYPE_VALID);
					}
					else
					{
						details.setLineType(LINE_TYPE_INVALID);
					}
					details.setEsiCategorySourceFileName(esiCategoryFile.getName());
					details.setEsiCategorySourceFilePath(PathUtil.winPath(esiCategoryFile));
					if(null==esiCategoryDetailsList || esiCategoryDetailsList.size()<=0)
					{
						esiCategoryDetailsList = new ArrayList<ESICategoryDetails>();
					}
					// add details to ESI CATEGORY LIST
					esiCategoryDetailsList.add(details);
					tokens=  null;
					details= null;
				}
			}
			line = null;
			details = null;
			tokens = null;
		}
		else
		{
			logger.info("readESICategoryTextFileForMME_WD :: ESI Category Text file is either Corrupted or Does not Exist.");
			esiCategoryDetailsList = null;
		}

		if(null!=br)
		{
			br.close();
		}
		return esiCategoryDetailsList;
	}

	
	/**
	 * Function will read all the Information from VIN TEXT FILE for MC Market
	 * @param vinFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<VinDetails> readVINTextFile(File vinFile) throws IOException 
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;

		if(null!=vinFile && vinFile.exists() && vinFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));

			VinDetails details = new VinDetails();
			String[] tokens = null;
			String line;
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
					if(line.endsWith(";"))
					{
						line = line.substring(0, line.length()-1);
					}
					details = new VinDetails();
					// <MANUAL TYPE FOLDER NAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<CARLINE CODE>||<VDS CODE>||<VIS START RANGE>||<VIS END RANGE>||<DISPLAY ORDER TEXT FILE>
					tokens = line.split("\\|\\|");
					if(null!=tokens && tokens.length>0)
					{
						try
						{
							if(tokens.length>=0)
							{
								if(null!=tokens[0] && !"".equals(tokens[0]))
								{
									// set MANUAL TYPE FOLDER NAME
									details.setManualType(String.valueOf(tokens[0]));
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
									// set FACE LIFT FOLDER NAME
									details.setFaceLiftFolder(String.valueOf(tokens[1]));
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
									// set MATERIAL FOLDER NAME
									details.setMaterialFolder(String.valueOf(tokens[2]));
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
									// set CARLINE CODE
									details.setCarlineCode(String.valueOf(tokens[3]));
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
									// set VDS CODE
									details.setVdsCode(String.valueOf(tokens[4]));
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
									// set VIS START RANGE
									details.setVisStartRange(String.valueOf(tokens[5]));
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
									// set VIS END RANGE
									details.setVisEndRange(String.valueOf(tokens[6]));
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
									// set DISPLAY ORDER FILE
									details.setDisplayOrderTextFileName(String.valueOf(tokens[7]));
								}
							}
						}
						catch(Exception e){
						}

					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */
					if(null!=details && null!=details.getManualType() && !"".equals(details.getManualType())
							&& null!=details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder()) 
							&& null!=details.getMaterialFolder() && !"".equals(details.getMaterialFolder()) 
							&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
							&& null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
							&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
							&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange())) 
					{
						/*
						 * NO NEED OF ADDING ANY FILE PATH
						 * SET LINE TYPE AS VALID
						 */
						details.setLineType(LINE_TYPE_VALID);
					}
					else
					{
						details.setLineType(LINE_TYPE_INVALID);
					}

					details.setVinSourceFileName(vinFile.getName());
					details.setVinSorceFilePath(PathUtil.winPath(vinFile));
					if(null==vinDetailsList || vinDetailsList.size()<=0)
					{
						vinDetailsList = new ArrayList<VinDetails>();
					}
					// add details to VIN LIST
					vinDetailsList.add(details);

					tokens=  null;
					details= null;
				}
			}
			line = null;
			details = null;
			tokens= null;
		}
		else
		{
			logger.info("readVINTextFile :: VIN Text file is either Corrupted or Does not Exist.");
			vinDetailsList = null;
		}
		// close buffered reader
		if(null!=br)
		{
			br.close();
		}
		return vinDetailsList;
	}

	/**
	 * File will read all the Information from VIN TEXT FILE for MME Market
	 * @param vinFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<VinDetails> readVINTextFileForMME(File vinFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;

		if(null!=vinFile && vinFile.exists() && vinFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));

			String line;
			String[] tokens=null;
			VinDetails details = new VinDetails();
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
					if(line.endsWith(";"))
					{
						line = line.substring(0, line.length()-1);
					}
					details = new VinDetails();
					// <MANUAL TYPE FOLDER NAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<CARLINE CODE>||<WMI_CODE>||<VDS CODE>||<VIS START RANGE>||<VIS END RANGE>||<DISPLAY ORDER TEXT FILE>
					tokens = line.split("\\|\\|");
					if(null!=tokens && tokens.length>0)
					{
						try
						{
							if(tokens.length>=0)
							{
								if(null!=tokens[0] && !"".equals(tokens[0]))
								{
									// set MANUAL TYPE FOLDER NAME
									details.setManualType(String.valueOf(tokens[0]));
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
									// set FACE LIFT FOLDER NAME
									details.setFaceLiftFolder(String.valueOf(tokens[1]));
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
									// set MATERIAL FOLDER NAME
									details.setMaterialFolder(String.valueOf(tokens[2]));
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
									// set CARLINE CODE
									details.setCarlineCode(String.valueOf(tokens[3]));
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
									// set WMI CODE
									details.setWmiCode(String.valueOf(tokens[4]));
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
									// set VDS CODE
									details.setVdsCode(String.valueOf(tokens[5]));
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
									// set VIS START RANGE
									details.setVisStartRange(String.valueOf(tokens[6]));
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
									// set VIS END RANGE
									details.setVisEndRange(String.valueOf(tokens[7]));
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
									// set DISPLAY ORDER FILE
									details.setDisplayOrderTextFileName(String.valueOf(tokens[8]));
								}
							}
						}
						catch(Exception e){
						}

					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */
					//		    		String filePath=null;

					if(null!=details && null!=details.getManualType() && !"".equals(details.getManualType())
							&& null!=details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder()) 
							&& null!=details.getMaterialFolder() && !"".equals(details.getMaterialFolder()) 
							&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
							&& null!=details.getWmiCode() && !"".equals(details.getWmiCode())
							&& null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
							&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
							&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange())) 
					{
						/*
						 * NO NEED OF ADDING ANY FILE PATH
						 * SET LINE TYPE AS VALID
						 */
						details.setLineType(LINE_TYPE_VALID);
						//	    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+details.getManualType()+"\\"+
						//	    						details.getFaceLiftFolder()+"\\"+details.getMaterialFolder();
					}
					else
					{
						details.setLineType(LINE_TYPE_INVALID);
					}


					details.setVinSourceFileName(vinFile.getName());
					details.setVinSorceFilePath(PathUtil.winPath(vinFile));
					if(null==vinDetailsList || vinDetailsList.size()<=0)
					{
						vinDetailsList = new ArrayList<VinDetails>();
					}
					// add details to VIN LIST
					vinDetailsList.add(details);

					tokens=  null;
					details= null;
				}
			}
			line = null;
			details = null;
			tokens=null;
		}
		else
		{
			logger.info("readVINTextFileForMME :: VIN Text file is either Corrupted or Does not Exist.");
			vinDetailsList = null;
		}

		// close buffered reader
		if(null!=br)
		{
			br.close();
		}
		return vinDetailsList;
	}

	
	/**
	 * Function will read all the information from DISPLAY ORDER TEXT FILE
	 * @param displayOrderFile
	 * @param itemDetails
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<DisplayOrderDetails> readDisplayOrderTextFile(File displayOrderFile, String locale) throws IOException
	{
		BufferedReader br = null;
		ArrayList<DisplayOrderDetails> displayOrderList = null;

		/*
		 * RULES FOR DISPLAY ORDER
		 * WHEN NOT ENGINE AT / MT (FOR ALL MODEL TYPES & CHANNELS)
		 * 
		 * 	THEN LINE WILL BE - 
		 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
		 * 	EngineType||Transmission Type||BodyType||DriveAxleType||Navigation LevelCode1||Navigation Level Code 2||
		 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
		 * 
		 * WHEN ENGINE AT / MT (FOR ALL MODEL TYPES & CHANNELS)
		 * 
		 * DISPLAY ORDER FORMAT
		 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
		 * 	Navigation LevelCode1||Navigation Level Code 2||
		 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
		 */

		if(null!=displayOrderFile && displayOrderFile.exists() && displayOrderFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(displayOrderFile.getAbsoluteFile()), "UTF-8"));

			String line;
			String sequenceNo=null;
			DisplayOrderDetails details = new DisplayOrderDetails();
			String[] tokens=null;
			String checkEngMissionCriteriaValue=null;
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
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

					details = new DisplayOrderDetails();
					if(null!=sequenceNo && !"".equals(sequenceNo))
					{
						details.setSequenceNo(sequenceNo);
					}
					// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
					//	<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||EngineType||Transmission Type||BodyType||DriveAxleType||
					//	<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
					// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>

					// OR 

					// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
					//	<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
					// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
					if(null!=line && !"".equals(line))
					{
						tokens = line.split("\\|\\|");

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
										/*
										 * if Model Folder Name is not null identify Model Type from it
										 * value before first index of _ is modelType
										 */
										if(null!=details.getModelFolderName() && !"".equals(details.getModelFolderName()))
										{
											// identify Model Type
											if(details.getModelFolderName().indexOf("_")>-1)
											{
												details.setModelType(details.getModelFolderName().substring(0, details.getModelFolderName().indexOf("_")));

												if(null!=details.getModelType() && !"".equals(details.getModelType()))
												{
													if(details.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
													{
														details.setModelType("New");
													}
													else if(details.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.old").trim().toLowerCase()))
													{
														details.setModelType("Old");
													}
													else
													{
														details.setModelType(details.getModelType().trim());
													}
												}
											}

											// identify checkEngMissionCriteriaValue
											if(details.getModelFolderName().lastIndexOf("_")>-1)
											{
												checkEngMissionCriteriaValue = details.getModelFolderName().substring(details.getModelFolderName().lastIndexOf("_")+1, details.getModelFolderName().length());
											}
										}
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


							if(null!=checkEngMissionCriteriaValue && !"".equals(checkEngMissionCriteriaValue))
							{
								if(checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
										checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
								{
									// ENGINE / AT / MT - DOES NOT CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
									details = readDisplayOrderEngATMTTokens(tokens, details);
									details.setEngineATMT(true);
								}
								else
								{
									// NORMAL MODEL - CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
									details = readDisplayOrderNormalModelTokens(tokens, details);
								}
							}
							else
							{
								// PROCEED WITH NORMAL MODEL (DEFAULT CASE) - CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
								details = readDisplayOrderNormalModelTokens(tokens, details);
							}
						}
						tokens=  null;
						checkEngMissionCriteriaValue = null;
					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */

					if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
							&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
							null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
							&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
							&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
							&& null!=details.getFileName() && !"".equals(details.getFileName()))
					{
						details.setLineType(LINE_TYPE_VALID);
					}
					else
					{
						// MAKE LINE TYPE AS INVALIDA
						details.setLineType(LINE_TYPE_INVALID);
					}


					details.setDisplayOrderSourceFileName(displayOrderFile.getName());
					details.setDisplayOrderSourceFilePath(PathUtil.winPath(displayOrderFile));
					if(null==displayOrderList || displayOrderList.size()<=0)
					{
						displayOrderList = new ArrayList<DisplayOrderDetails>();
					}
					// add details to ESI CATEGORY LIST
					displayOrderList.add(details);
					details= null;
				}
			}
			line = null;
			details =null;
			tokens=null;
			sequenceNo = null;
		}
		else
		{
			logger.info("readDisplayOrderTextFile :: DISPLAY ORDER Text file is either Corrupted or Does not Exist.");
			displayOrderList = null;
		}

		if(null!=br)
		{
			br.close();
		}
		return displayOrderList;
	}
	
	/**
	 * Function will read the CD Processing Data Identified in the FACE LIFT FOLDER
	 * @param cdProcessingFile
	 * @param itemDetails
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<CDProcessingDetails> readCDProcessingTextFile(File cdProcessingFile, String locale) throws IOException
	{
		BufferedReader br = null;
		ArrayList<CDProcessingDetails> cdProcessingDataList = null;

		/*
		 * RULES FOR DISPLAY ORDER
		 * WHEN NOT ENGINE AT / MT (FOR ALL MODEL TYPES & CHANNELS)
		 * 
		 * 	THEN LINE WILL BE - 
		 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
		 * 	EngineType||Transmission Type||BodyType||DriveAxleType||Navigation LevelCode1||Navigation Level Code 2||
		 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
		 * 
		 * WHEN ENGINE AT / MT (FOR ALL MODELS & CHANNELS)
		 * 
		 * LINE WILL BE - 
		 * Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
		 * 	Navigation LevelCode1||Navigation Level Code 2||
		 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
		 * 
		 */

		if(null!=cdProcessingFile && cdProcessingFile.exists() && cdProcessingFile.isFile())
		{
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(cdProcessingFile.getAbsoluteFile()), "UTF-8"));

			String line;
			CDProcessingDetails details = new CDProcessingDetails();
			String[] tokens=null;
			String sequenceNo=null;
			String checkEngMissionCriteriaValue=null;
			while ((line = br.readLine()) != null) {
				if(null!=line && !"".equals(line))
				{
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

					// OR 

					// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
					//	<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
					// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
					if(null!=line && !"".equals(line))
					{
						tokens = line.split("\\|\\|");

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
										/*
										 * if Model Folder Name is not null identify Model Type from it
										 * value before first index of _ is modelType
										 */
										if(null!=details.getModelFolderName() && !"".equals(details.getModelFolderName()))
										{
											// identify Model Type
											if(details.getModelFolderName().indexOf("_")>-1)
											{
												details.setModelType(details.getModelFolderName().substring(0, details.getModelFolderName().indexOf("_")));

												if(null!=details.getModelType() && !"".equals(details.getModelType()))
												{
													if(details.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
													{
														details.setModelType("New");
													}
													else if(details.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.old").trim().toLowerCase()))
													{
														details.setModelType("Old");
													}
													else
													{
														details.setModelType(details.getModelType().trim());
													}
												}
											}

											// identify checkEngMissionCriteriaValue
											if(details.getModelFolderName().lastIndexOf("_")>-1)
											{
												checkEngMissionCriteriaValue = details.getModelFolderName().substring(details.getModelFolderName().lastIndexOf("_")+1, details.getModelFolderName().length());
											}
										}
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

							if(null!=checkEngMissionCriteriaValue && !"".equals(checkEngMissionCriteriaValue))
							{
								if(checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
										checkEngMissionCriteriaValue.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
								{
									// ENGINE / AT / MT - DOES NOT CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
									details = readCDProcessingDataEngATMTTokens(tokens, details);
									details.setEngineATMT(true);
								}
								else
								{
									// NORMAL MODEL - CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
									details = readCDProcessingDataNormalModelTokens(tokens, details);
								}
							}
							else
							{
								// PROCEED WITH NORMAL MODEL (DEFAULT CASE) - CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
								details = readCDProcessingDataNormalModelTokens(tokens, details);
							}
						}
						tokens=  null;
						checkEngMissionCriteriaValue = null;
					}

					/*
					 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
					 * STARTING FROM LOCALE
					 */

					if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
							&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
							null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
							&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
							&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
							&& null!=details.getFileName() && !"".equals(details.getFileName()))
					{
						details.setLineType(MetaDataValidationUtils.LINE_TYPE_VALID);
					}
					else
					{
						details.setLineType(MetaDataValidationUtils.LINE_TYPE_INVALID);
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
				}
			}
			line = null;
			details= null;
			tokens = null;
			sequenceNo = null;
		}
		else
		{
			logger.info("readCDProcessingTextFile :: CD PROCESSING Text file is either Corrupted or Does not Exist.");
			cdProcessingDataList = null;
		}

		if(null!=br)
		{
			br.close();
		}
		return cdProcessingDataList;
	}
	
	/**
	 * Function will read Values from Line Tokens for Normal Model from Display Order File
	 * @param tokens
	 * @param details
	 * @return
	 */
	private static DisplayOrderDetails readDisplayOrderNormalModelTokens(String[] tokens, DisplayOrderDetails details)
	{
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
					// set DISPLAY ORDER LEVEL 1 CODE
					details.setDisplayOrderLevel1Code(String.valueOf(tokens[11]));
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
					// set DISPLAY ORDER LEVEL 2 CODE
					details.setDisplayOrderLevel2Code(String.valueOf(tokens[12]));
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
					// set DISPLAY ORDER LEVEL 3 CODE
					details.setDisplayOrderLevel3Code(String.valueOf(tokens[13]));
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
					// set DISPLAY ORDER LEVEL 4 CODE
					details.setDisplayOrderLevel4Code(String.valueOf(tokens[14]));
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
					// set DISPLAY ORDER LEVEL 5 CODE
					details.setDisplayOrderLevel5Code(String.valueOf(tokens[15]));
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
					// set DISPLAY ORDER LEVEL 6 CODE
					details.setDisplayOrderLevel6Code(String.valueOf(tokens[16]));
				}
			}
		}
		catch(Exception e){
		}
		
		return details;
	}
	
	
	/**
	 * Function will read Values from Line Tokens for Engine / AT / MT from Display Order File
	 * @param tokens
	 * @param details
	 * @return
	 */
	private static DisplayOrderDetails readDisplayOrderEngATMTTokens(String[] tokens, DisplayOrderDetails details)
	{
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
					// set DISPLAY ORDER LEVEL 1 CODE
					details.setDisplayOrderLevel1Code(String.valueOf(tokens[7]));
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
					// set DISPLAY ORDER LEVEL 2 CODE
					details.setDisplayOrderLevel2Code(String.valueOf(tokens[8]));
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
					// set DISPLAY ORDER LEVEL 3 CODE
					details.setDisplayOrderLevel3Code(String.valueOf(tokens[9]));
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
					// set DISPLAY ORDER LEVEL 4 CODE
					details.setDisplayOrderLevel4Code(String.valueOf(tokens[10]));
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
					// set DISPLAY ORDER LEVEL 5 CODE
					details.setDisplayOrderLevel5Code(String.valueOf(tokens[11]));
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
					// set DISPLAY ORDER LEVEL 6 CODE
					details.setDisplayOrderLevel6Code(String.valueOf(tokens[12]));
				}
			}
		}
		catch(Exception e){
		}
		return details;
	}

	/**
	 * Function will read Values from Line Tokens for Normal Model from CD Processing File
	 * @param tokens
	 * @param details
	 * @return
	 */
	private static CDProcessingDetails readCDProcessingDataNormalModelTokens(String[] tokens, CDProcessingDetails details)
	{
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
					// set DISPLAY ORDER LEVEL 1 CODE
					details.setDisplayOrderLevel1Code(String.valueOf(tokens[11]));
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
					// set DISPLAY ORDER LEVEL 2 CODE
					details.setDisplayOrderLevel2Code(String.valueOf(tokens[12]));
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
					// set DISPLAY ORDER LEVEL 3 CODE
					details.setDisplayOrderLevel3Code(String.valueOf(tokens[13]));
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
					// set DISPLAY ORDER LEVEL 4 CODE
					details.setDisplayOrderLevel4Code(String.valueOf(tokens[14]));
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
					// set DISPLAY ORDER LEVEL 5 CODE
					details.setDisplayOrderLevel5Code(String.valueOf(tokens[15]));
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
					// set DISPLAY ORDER LEVEL 6 CODE
					details.setDisplayOrderLevel6Code(String.valueOf(tokens[16]));
				}
			}
		}
		catch(Exception e){
		}
		
		return details;
	}
	
	
	/**
	 * Function will read Values from Line Tokens for Engine / AT / MT from CD Processing File
	 * @param tokens
	 * @param details
	 * @return
	 */
	private static CDProcessingDetails readCDProcessingDataEngATMTTokens(String[] tokens, CDProcessingDetails details)
	{
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
					// set DISPLAY ORDER LEVEL 1 CODE
					details.setDisplayOrderLevel1Code(String.valueOf(tokens[7]));
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
					// set DISPLAY ORDER LEVEL 2 CODE
					details.setDisplayOrderLevel2Code(String.valueOf(tokens[8]));
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
					// set DISPLAY ORDER LEVEL 3 CODE
					details.setDisplayOrderLevel3Code(String.valueOf(tokens[9]));
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
					// set DISPLAY ORDER LEVEL 4 CODE
					details.setDisplayOrderLevel4Code(String.valueOf(tokens[10]));
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
					// set DISPLAY ORDER LEVEL 5 CODE
					details.setDisplayOrderLevel5Code(String.valueOf(tokens[11]));
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
					// set DISPLAY ORDER LEVEL 6 CODE
					details.setDisplayOrderLevel6Code(String.valueOf(tokens[12]));
				}
			}
		}
		catch(Exception e){
		}
		return details;
	}

	
	public static ArrayList<LeftMenuFileDetails> readLeftMenuTextFile(File leftMenuFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<LeftMenuFileDetails> leftMenuDetailsList = null;


		if(null!=leftMenuFile && leftMenuFile.exists() && leftMenuFile.isFile())
		{
			String categoryCode=null;
			String subCategoryCode=null;
			String subSubCategoryCode=null;
			String title=null;
			String fileName=null;
			String vtocFileName=null;
			br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(leftMenuFile.getAbsoluteFile()), "UTF-8"));

			String line;
			while ((line = br.readLine()) != null) 
			{
				if(null!=line && !"".equals(line))
				{
					if(line.indexOf("SIE=")!=-1)
					{
						String beforeData = line.substring(0,line.indexOf("SIE="));

						String dataFromSIE = line.substring(line.indexOf("SIE="), line.length());
						if(null!=dataFromSIE && !"".equals(dataFromSIE))
						{
							dataFromSIE = dataFromSIE.trim();
							/*
							 * CHECK HERE FOR FILENAME ATTRIBUTE (e.g. VTOC File Name)
							 * if found, then set VTOC File Name in LEFT MENU
							 */
							if(dataFromSIE.indexOf("FILENAME=")!=-1)
							{
								fileName = dataFromSIE.substring(0, dataFromSIE.indexOf("FILENAME="));
								if(null!=fileName && !"".equals(fileName))
								{
									fileName= fileName.replace("SIE=", "");
									fileName= fileName.trim();
								}
								vtocFileName = dataFromSIE.substring(dataFromSIE.indexOf("FILENAME="), dataFromSIE.length());
								if(null!=vtocFileName && !"".equals(vtocFileName))
								{
									vtocFileName= vtocFileName.replace("FILENAME=", "");
									vtocFileName = vtocFileName.trim();
								}
							}
							else
							{
								// FILE NAME ATTRIBUTE DOES NOT EXISTS - THE DATA IS SIE ID
								fileName= dataFromSIE.trim();
								if(null!=fileName && !"".equals(fileName))
								{
									fileName= fileName.replace("SIE=", "");
									fileName= fileName.trim();
								}
							}
						}
						dataFromSIE = null;
						if(null!=beforeData && !"".equals(beforeData))
						{
							beforeData = beforeData.trim();
							int catCheckLength=0;
							int subCatCheckLength=0;
							int subSubCatCheckLength=0;
							if(beforeData.trim().toLowerCase().startsWith("d"))
							{
								catCheckLength = 4;
								subCatCheckLength = 7;
								subSubCatCheckLength= 2;
							}
							else
							{
								catCheckLength = 3;
								subCatCheckLength = 6;
								subSubCatCheckLength= 2;
							}
							if(beforeData.length()>catCheckLength)
							{
								categoryCode = beforeData.substring(0,catCheckLength);
								String afterCatCode = beforeData.substring(catCheckLength,beforeData.length());
								if(null!=afterCatCode && !"".equals(afterCatCode))
								{
									afterCatCode = afterCatCode.trim();
									if(afterCatCode.length()>subCatCheckLength)
									{
										subCategoryCode = afterCatCode.substring(0,subCatCheckLength);
										String afterSubCatCode = afterCatCode.substring(subCatCheckLength, afterCatCode.length());
										if(null!=afterSubCatCode && !"".equals(afterSubCatCode))
										{
											afterSubCatCode = afterSubCatCode.trim();
											if(afterSubCatCode.length()>subSubCatCheckLength)
											{
												subSubCategoryCode = afterSubCatCode.substring(0, subSubCatCheckLength);
												title = afterSubCatCode.substring(subSubCatCheckLength, afterSubCatCode.length());
											}
										}
										afterSubCatCode = null;
									}
								}
								afterCatCode = null;
							}
						}
						beforeData = null;


						/*
						 * Check here whether the record has to be skipped or not.
						 * if subCategoryCode endsWith - C00 - these are CONN HTMLs 
						 * skip these entries
						 */
						if(null!=categoryCode && !"".equals(categoryCode) && 
								null!=subCategoryCode && !"".equals(subCategoryCode) && null!=subSubCategoryCode && !"".equals(subSubCategoryCode) 
								&& null!=title && !"".equals(title) && null!=fileName && !"".equals(fileName))
						{
							if(!subCategoryCode.trim().toLowerCase().endsWith("c00"))
							{
								categoryCode = categoryCode.trim();
								subCategoryCode = subCategoryCode.trim();
								subSubCategoryCode = subSubCategoryCode.trim();
								/*
								 * add to Left Menu List - not for CONN HTMLs
								 */
								LeftMenuFileDetails lfDetails = new LeftMenuFileDetails();
								if(categoryCode.trim().toLowerCase().startsWith("d"))
								{
									categoryCode = categoryCode.substring(1,categoryCode.length());
								}
								if(subCategoryCode.trim().toLowerCase().startsWith("d"))
								{
									subCategoryCode = subCategoryCode.substring(1, subCategoryCode.length());
								}

								//		    					logger.info("readLeftMenuTextFile :: CAT {"+categoryCode.trim()+"}  SUB CAT {"+subCategoryCode+"}  TITLE {"+title+"}  FILE NAME {"+fileName+"}");
								lfDetails.setCategoryCode(categoryCode.trim());
								lfDetails.setSubCategoryCode(subCategoryCode.trim());
								lfDetails.setSubSubCategoryCode(subSubCategoryCode.trim());
								lfDetails.setTitle(title.trim());
								lfDetails.setFileName(fileName.trim());
								if(null!=vtocFileName && !"".equals(vtocFileName))
								{
									lfDetails.setVtocFileNameAttribute(vtocFileName.trim());
								}
								
								lfDetails.setLeftMenuSourceFileName(leftMenuFile.getName());
								lfDetails.setLeftMenuSourceFilePath(PathUtil.winPath(leftMenuFile));
								if(null==leftMenuDetailsList || leftMenuDetailsList.size()<=0)
								{
									leftMenuDetailsList = new ArrayList<LeftMenuFileDetails>();
								}
								// add lfDetails to leftMenuDetailsList
								leftMenuDetailsList.add(lfDetails);
								lfDetails=null;
							}
						}
					}
				}
			}
			line = null;
		}
		else
		{
			logger.info("readLeftMenuTextFile :: Left Menu Text file is either Corrupted or Does not Exist.");
			leftMenuDetailsList = null;
		}

		if(null!=br)
		{
			br.close();
		}
		return leftMenuDetailsList;
	}
	
	public static ArrayList<VINEntFileDetails> readVINENTFile(File vinEntFile) 
	{
		ArrayList<VINEntFileDetails> vinEntDetailsList = new ArrayList<VINEntFileDetails>();
		try
		{
			if(null!=vinEntFile && vinEntFile.isFile() && vinEntFile.exists())
			{
				Document document = ParseXMLDoc.parseFile(PathUtil.winPath(vinEntFile));
				if(null!=document)
				{
					NodeList vinInfoNodeList = document.getElementsByTagName("VININFO");
					if(null!=vinInfoNodeList && vinInfoNodeList.getLength()>0)
					{
						/*
						 * Parse Strcuture
						 * 	<VININFO>
								<VINDATA>
								<Modelyear>2016MY</Modelyear>
								<VINWMI>JM1</VINWMI>
								<VINcarline>ND</VINcarline>
								<VINVDS>NDAA7*G#</VINVDS>
								<VINStart>100001</VINStart>
								<VINEnd>zzzzzz</VINEnd>
								</VINDATA>
							</VININFO>
						*/
						NodeList vinDataNodesList = vinInfoNodeList.item(0).getChildNodes();
						if(null!=vinDataNodesList && vinDataNodesList.getLength()>0)
						{
							for(int i=0;i<vinDataNodesList.getLength();i++)
							{
								Node vinDataNode = (Node)vinDataNodesList.item(i);
								if (!"#text".equalsIgnoreCase(vinDataNode.getNodeName()))
								{
									if(vinDataNode.getNodeName().equals("VINDATA"))
									{
										
										VINEntFileDetails details = new VINEntFileDetails();
										
										
										NodeList childNodesList = vinDataNode.getChildNodes();
										if(null!=childNodesList && childNodesList.getLength()>0)
										{
											for(int j=0;j<childNodesList.getLength();j++)
											{
												Node childNode = (Node)childNodesList.item(j);
												if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
												{
													if(childNode.getNodeName().equals("Modelyear"))
													{
														details.setModelYear(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINWMI"))
													{
														details.setVinWMI(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINcarline"))
													{
														details.setVinCarline(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINVDS"))
													{
														details.setVinVDS(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINStart"))
													{
														details.setVinStartRange(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINEnd"))
													{
														details.setVinEndRange(ParseXMLDoc.getElementValue(childNode));
													}
												}
												childNode=  null;
											}
										}
										
										/*
										 * add details to vinENtList
										 */
										// ADD FILE NAME & PATH AS WELL TO THE LIST
										details.setFileName(vinEntFile.getName());
										details.setFilePath(PathUtil.winPath(vinEntFile));
										vinEntDetailsList.add(details);
										details =null;
										
										childNodesList=  null;
									}
								}
								vinDataNode=  null;
							}
						}
						vinDataNodesList = null;
					}
					vinInfoNodeList = null;
				}
				document = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationUtils.class.getName(), "readVINENTFile()", e);
			vinEntDetailsList = null;
		}
		return vinEntDetailsList;
	}

	public static ArrayList<VinAttributeEntFileDetails> readVINAttributeENTFile(File vinAttributeEntFile) 
	{
		ArrayList<VinAttributeEntFileDetails> vinAttributeEntDetailsList = new ArrayList<VinAttributeEntFileDetails>();
		try
		{
			if(null!=vinAttributeEntFile && vinAttributeEntFile.isFile() && vinAttributeEntFile.exists())
			{
				Document document = ParseXMLDoc.parseFile(PathUtil.winPath(vinAttributeEntFile));
				if(null!=document)
				{
					NodeList vinAttrNodeList = document.getElementsByTagName("VINATTR");
					if(null!=vinAttrNodeList && vinAttrNodeList.getLength()>0)
					{
						/*
						 * Parse Structure
						 * 	<VINATTR>
								<VINATTRDATA>
								<VINtranstype>B48</VINtranstype>
								<VINenginetype>A27</VINenginetype>
								<VINbodytype>3</VINbodytype>
								<VINbodytypevalue>OPEN</VINbodytypevalue>
								<VINdriveaxle>2</VINdriveaxle>
								<VINdriveaxlevakue>2WD</VINdriveaxlevakue>
								</VINATTRDATA>
							</VINATTR>
						*/
						NodeList vinAttrDataNodeList = vinAttrNodeList.item(0).getChildNodes();
						if(null!=vinAttrDataNodeList && vinAttrDataNodeList.getLength()>0)
						{
							for(int i=0;i<vinAttrDataNodeList.getLength();i++)
							{
								Node vinAttrDataNode = (Node)vinAttrDataNodeList.item(i);
								if (!"#text".equalsIgnoreCase(vinAttrDataNode.getNodeName()))
								{
									if(vinAttrDataNode.getNodeName().equals("VINATTRDATA"))
									{
										VinAttributeEntFileDetails details = new VinAttributeEntFileDetails();
										
										NodeList childNodesList = vinAttrDataNode.getChildNodes();
										if(null!=childNodesList && childNodesList.getLength()>0)
										{
											for(int j=0;j<childNodesList.getLength();j++)
											{
												Node childNode = (Node)childNodesList.item(j);
												if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
												{
													if(childNode.getNodeName().equals("VINtranstype"))
													{
														details.setVinTransType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINenginetype"))
													{
														details.setVinEngineType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINbodytype"))
													{
														details.setVinBodyType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINbodytypevalue"))
													{
														details.setVinBodyTypeValue(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINdriveaxle"))
													{
														details.setVinAxleType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINdriveaxlevakue"))
													{
														details.setVinAxleTypeValue(ParseXMLDoc.getElementValue(childNode));
													}
												}
												childNode=  null;
											}
										}
										/*
										 * add details to vinAttribnuteENtList
										 */
										// ADD FILE NAME & PATH AS WELL TO THE LIST
										details.setFileName(vinAttributeEntFile.getName());
										details.setFilePath(PathUtil.winPath(vinAttributeEntFile));
										vinAttributeEntDetailsList.add(details);
										details =null;
										
										childNodesList=  null;
									}
								}
								vinAttrDataNode=  null;
							}
						}
						vinAttrDataNodeList = null;
					}
					vinAttrNodeList = null;
				}
				document = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationUtils.class.getName(), "readVINAttributeENTFile()", e);
			vinAttributeEntDetailsList = null;
		}
		return vinAttributeEntDetailsList;
	}
	
}
