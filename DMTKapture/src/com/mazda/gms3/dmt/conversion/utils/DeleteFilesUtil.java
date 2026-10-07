package com.mazda.gms3.dmt.conversion.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DateFormatter;
import com.mazda.gms3.dmt.vo.ContentDetails;


public class DeleteFilesUtil {

	private static Logger logger = LogManager.getLogger(DeleteFilesUtil.class);

	private static ArrayList<Map<Object, Object>> fileUploadFailureList = new ArrayList<Map<Object, Object>>();
	
	private static ArrayList<Map<Object, Object>> fileProcessingList = new ArrayList<Map<Object, Object>>();
	
	public static ArrayList<Map<Object, Object>> getFileProcessingList() {
		return fileProcessingList;
	}

	public static void setFileProcessingList(
			ArrayList<Map<Object, Object>> fileProcessingList) {
		DeleteFilesUtil.fileProcessingList = fileProcessingList;
	}

	public static ArrayList<Map<Object, Object>> getFileUploadFailureList() {
		return fileUploadFailureList;
	}

	public static void setFileUploadFailureList(
			ArrayList<Map<Object, Object>> fileUploadFailureList) {
		DeleteFilesUtil.fileUploadFailureList = fileUploadFailureList;
	}

	/**
	 * Function will Delete the Files from OK ASSETS Directory
	 * 
	 * @param sourceFile
	 * @param localeFolderName
	 * @param channelFolderName
	 * @param yearFolderName
	 * @param modelFolderName
	 * @param movementType
	 */
	public static void deleteFilesFromServer(String sourceFileName, String localeFolderName, String channelFolderName, String modelFolderName, String manualTypeFolderName, 
			String yearFolderName,  String movementType, ContentDetails contentDetails) {
		try {
			if (null!=sourceFileName && !"".equals(sourceFileName) && null != localeFolderName && !"".equals(localeFolderName)
					&& null != channelFolderName
					&& !"".equals(channelFolderName)) {
				logger.info("deleteFilesFromServer :: Source File, Locale and Channel Folders Names are not null as Parameter. Proceed for Deleting File.");
				/*
				 * Prepare Destination Path /library/MAZDAESI/Service
				 * Manuals/en_us/image/ac5uuw00000002.gif
				 */
				// PREPARE CHANNEL REF KEY
				channelFolderName = channelFolderName.replace(" ", "_");
				
				
				String destinationPath = "";
				destinationPath = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				// add repository
				destinationPath = destinationPath+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
				// add channel Name
				destinationPath = destinationPath+ channelFolderName.toUpperCase() + "/";
				// add locale Folder Name in lowercase
				destinationPath = destinationPath + ApplicationProperties.getProperty(localeFolderName.trim().toLowerCase()).trim().toLowerCase() + "/";
				
				if (null != modelFolderName && !"".equals(modelFolderName)) {
					// add model Folder Name in lowercase
					destinationPath = destinationPath+ modelFolderName.toLowerCase() + "/";
				}
				
				if(null!=manualTypeFolderName && !"".equals(manualTypeFolderName))
				{
					// add manualType Folder Name in lowercase
					destinationPath = destinationPath + manualTypeFolderName.toLowerCase()+"/";
				}
				
				if(null!=yearFolderName && !"".equals(yearFolderName))
				{
					// add yearFolderName in as in lowercase
					destinationPath = destinationPath+yearFolderName.toLowerCase()+"/";
				}
				
				// add image/html/pdf directory name
				if (movementType.equals("image")) 
				{
					destinationPath = destinationPath + "image";
				} 
				else if (movementType.equals("html") || movementType.equals("html5")) 
				{
					destinationPath = destinationPath + "html";
				} 
				else if(movementType.equals("conn"))
				{
					// THIS SITUATION WILL COME WHEN CONN DIRECOTRY IS PARALLEL TO HTML FOLDER - PROCESSING HTML5 DOCUMENTS
					destinationPath = destinationPath+"conn";
				}
				else if (movementType.equals("pdf")) 
				{
					destinationPath = destinationPath + "pdf";
				}
				else if (movementType.equals("xml")) 
				{
					destinationPath = destinationPath + "xml";
				} 
				else if (movementType.equals("xls")) 
				{
					destinationPath = destinationPath + "xls";
				}
				else if (movementType.equals("zip")) 
				{
					destinationPath = destinationPath + "zip";
				}
				else if (movementType.equals("xlsx")) 
				{
					destinationPath = destinationPath + "xlsx";
				} 
				else if (movementType.equals("doc")) 
				{
					destinationPath = destinationPath + "doc";
				}
				// Now check for all the folders whether they exist or not in
				// the destination path

				if (destinationPath.startsWith("/")) {
					destinationPath = destinationPath.substring(1,
							destinationPath.length());
				}
				String spPath = ApplicationProperties.getProperty("SERVER_OKASSETS_PHYSICAL_PATH");
				if(!spPath.startsWith("/"))
				{
					spPath = spPath+"/";
				}
				destinationPath = spPath+destinationPath;
				// add sourceFile Name to the destinationPath

				if (!destinationPath.endsWith("/")) {
					destinationPath = destinationPath + "/";
				}
				destinationPath = destinationPath + sourceFileName;

				/*
				 * call function to add File to Processing List
				 */
				captureFileProcessingeDetails(sourceFileName, "", destinationPath, contentDetails);
				
				
				if (null != destinationPath && !"".equals(destinationPath)) {
					logger.info("deleteFilesFromServer :: Checking Input File {"
							+ sourceFileName
							+ "} at Path :: >"
							+ destinationPath);

					File destinationFilePath = PathUtil.file(destinationPath);
					if (destinationFilePath.exists()) {
						// delete Directory
						boolean delete = destinationFilePath.delete();
						if (delete == true) {
							logger.info("deleteFilesFromServer :: Input File {"
									+ sourceFileName
									+ "}. Deleted Successfully at Path :: >"
									+ destinationPath);

							System.out
									.println("deleteFilesFromServer :: Input File {"
											+ sourceFileName
											+ "}. Deleted Successfully at Path :: >"
											+ destinationPath);
							
							/*
							 * update OKAssets Delete Count
							 */
							ScheduleDAO.updateProcessingOKAssetsDeleteCount(contentDetails.getScheduleId());
							
						} else {
							logger.info("deleteFilesFromServer :: Failed to delete Input File {"
									+ sourceFileName
									+ "} at Path :: >"
									+ destinationPath);

							System.err
									.println("deleteFilesFromServer :: Failed to delete Input File {"
											+ sourceFileName
											+ "} at Path :: >"
											+ destinationPath);

							/*
							 * Track Error
							 */
							String errorMessage = "Failed to delete File at the Destination Path.";
							captureFileUploadFailureDetails(
									sourceFileName,"",destinationPath, errorMessage, contentDetails);
							// set errorMessage to null
							errorMessage = null;
						}
					}
				}
				// set destinationPath to null
				destinationPath = null;
			} else {
				logger.info("deleteFilesFromServer :: Source File Details, Locale and Channel Folder Names are null as Parameter. Skip File.");
				System.err
						.println("deleteFilesFromServer :: Source File Details, Locale and Channel Folder Names are null as Parameter. Skip File.");
				/*
				 * Track Error
				 */
				String errorMessage = "Source File Details is not a Valid File.";
				captureFileUploadFailureDetails("", "", "", errorMessage, contentDetails);
				// set errorMessage to null
				errorMessage = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Function will be used to track and store File processing details
	 * @param fileName
	 * @param sourcePath
	 * @param destinationPath
	 */
	private static void captureFileProcessingeDetails(String fileName,String sourcePath, String destinationPath, ContentDetails contentDetails) 
	{
		try {
			Map<Object, Object> dataMap = new HashMap<Object, Object>();
			// add sourceFilePath
			dataMap.put("SOURCE_PATH", sourcePath);
			// add destinationPath
			dataMap.put("DESTINATION_PATH", destinationPath);
			// add fileName
			dataMap.put("FILE_NAME", fileName);
			// add contentDetails
			dataMap.put("CONTENT_DETAILS", contentDetails);
			dataMap.put("DATE_TIME",DateFormatter.converTimeStampToString(new Timestamp(new Date().getTime())));
			
			/*
			 * check here getFileUploadFailureList is null then initialize it
			 */
			if (null == getFileProcessingList() 	|| getFileProcessingList().size() <= 0) 
			{
				// initialize the list
				setFileProcessingList(new ArrayList<Map<Object, Object>>());
			}
			// add dataMap to getFileProcessingList
			getFileProcessingList().add(dataMap);
			// set dataMap to null
			dataMap = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function will capture all the error details while moving files to network
	 * location in and arrayList
	 * 
	 * @param inputFileDetailsVO
	 * @param sourcePath
	 * @param destinationPath
	 * @param errorMessage
	 */
	private static void captureFileUploadFailureDetails(String fileName,
			String sourcePath, String destinationPath, String errorMessage, ContentDetails contentDetails) {
		try {
			Map<Object, Object> errorMap = new HashMap<Object, Object>();
			// add sourceFilePath
			errorMap.put("SOURCE_PATH", sourcePath);
			// add destinationPath
			errorMap.put("DESTINATION_PATH", destinationPath);
			// add fileName
			errorMap.put("FILE_NAME", fileName);
			errorMap.put("ERROR_MESSAGE", errorMessage);
			errorMap.put("CONTENT_DETAILS", contentDetails);
			errorMap.put("DATE_TIME",
					DateFormatter.converTimeStampToString(new Timestamp(
							new Date().getTime())));

			/*
			 * check here getFileUploadFailureList is null then initialize it
			 */
			if (null == getFileUploadFailureList()
					|| getFileUploadFailureList().size() <= 0) {
				// initialize the list
				setFileUploadFailureList(new ArrayList<Map<Object, Object>>());
			}
			// add errorMap to getFileUploadFailureList
			getFileUploadFailureList().add(errorMap);
			// set errorMap to null
			errorMap = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
