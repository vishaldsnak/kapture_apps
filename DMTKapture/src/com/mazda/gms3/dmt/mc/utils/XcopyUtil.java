package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.mazda.gms3.dmt.conversion.utils.WiringDiagramUtils;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DateFormatter;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;

public class XcopyUtil {
	Logger logger = LogManager.getLogger(XcopyUtil.class);

	String XCOPY_FLAG = ApplicationProperties.getProperty("XCOPY_FLAG");

	private ArrayList<Map<Object, Object>> fileProcessingList = new ArrayList<Map<Object, Object>>();

	private ArrayList<Map<Object, Object>> fileUploadFailureList = new ArrayList<Map<Object, Object>>();

	public ArrayList<Map<Object, Object>> getFileProcessingList() {
		return fileProcessingList;
	}

	public void setFileProcessingList(
			ArrayList<Map<Object, Object>> fileProcessingList) {
		this.fileProcessingList = fileProcessingList;
	}

	public ArrayList<Map<Object, Object>> getFileUploadFailureList() {
		return fileUploadFailureList;
	}

	public void setFileUploadFailureList(
			ArrayList<Map<Object, Object>> fileUploadFailureList) {
		this.fileUploadFailureList = fileUploadFailureList;
	}

	/**
	 * Function will move inputFile from sourceLocation to destinationLocation
	 * 
	 * @param inputFileDetailsVO
	 */
	public void copyFilesToServer(File sourceFile, String localeFolderName,
			String channelFolderName, String modelFolderName,
			String manualTypeFolderName, String faceLiftFolderName,
			String materialFolderName, String movementType,
			ContentDetails contentDetails, WiringDiagramUtils wiringDiagramUtils) {
		boolean updateFailureCount = false;
		try {
			boolean proceedFurther = false;
			if (null != XCOPY_FLAG && !"".equals(XCOPY_FLAG)
					&& XCOPY_FLAG.toLowerCase().equals("y")) {
				// set proceed to true
				proceedFurther = true;
			}

			if (proceedFurther == true) {
				if (null != sourceFile && sourceFile.exists()
						&& null != localeFolderName
						&& !"".equals(localeFolderName)
						&& null != channelFolderName
						&& !"".equals(channelFolderName)) {

					logger.info("copyFilesToServer :: Source File, Locale and Channel Folders Names are not null as Parameter. Proceed for Uploading File.");
					/*
					 * Prepare Destination Path /library/MAZDAESI/Service
					 * Information/en_us/image/ac5uuw00000002.gif
					 */

					// PREPARE CHANNEL REF KEY
					channelFolderName = channelFolderName.replace(" ", "_");

					String destinationPath = "";
					destinationPath = ApplicationProperties
							.getProperty("SERVER_LIBRARY_DIRECTORY");
					// add repository
					destinationPath = destinationPath
							+ ApplicationProperties.getProperty("REPOSITORY")
									.toUpperCase() + "/";
					// add channel Name
					destinationPath = destinationPath
							+ channelFolderName.toUpperCase() + "/";
					// add locale Folder Name in lowercase
					destinationPath = destinationPath
							+ ApplicationProperties
									.getProperty(
											localeFolderName.trim()
													.toLowerCase()).trim()
									.toLowerCase() + "/";

					if (null != modelFolderName && !"".equals(modelFolderName)) {
						// add model Folder Name in lowercase
						destinationPath = destinationPath
								+ modelFolderName.toLowerCase() + "/";
					}

					if (null != manualTypeFolderName
							&& !"".equals(manualTypeFolderName)) {
						// add manualType Folder Name in lowercase
						destinationPath = destinationPath
								+ manualTypeFolderName.toLowerCase() + "/";
					}

					if (null != faceLiftFolderName
							&& !"".equals(faceLiftFolderName)) {
						// add faceLift Folder Name in lowercase
						destinationPath = destinationPath
								+ faceLiftFolderName.toLowerCase() + "/";
					}
					if (null != materialFolderName
							&& !"".equals(materialFolderName)) {
						// add materialFolderName in as in lowercase
						destinationPath = destinationPath
								+ materialFolderName.toLowerCase() + "/";
					}

					// add image/html/pdf directory name
					if (movementType.equals("image")) {
						destinationPath = destinationPath + "image";
					} else if (movementType.equals("djvu")) {
						destinationPath = destinationPath + "djvu";
					}
					else if (movementType.equals("ent.gms3")) {
						destinationPath = destinationPath + "ent.gms3";
					}
					else if (movementType.equals("html")
							|| movementType.equals("html5")) {
						destinationPath = destinationPath + movementType.trim();
						/*
						 * check here it the sourceFile folder path has conn
						 * directory, if yes, then add the conn directory to
						 * destination path - CSS / JS / IMG / PRING
						 */
						if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.conn")
												+ "\\")) {
							// add conn to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.conn");
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.css")
												+ "\\")) {
							// add css to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.css");
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.js"))) {
							/*
							 * ONLY WHEN MOVEMENT TYPE IS HTML5
							 */
							if (movementType.equals("html5")) {
								// add js to destination Path
								destinationPath = destinationPath
										+ "/"
										+ ApplicationProperties
												.getProperty("directory.js");
							}
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.img")
												+ "\\")) {
							// add css to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.img");
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.print")
												+ "\\")) {
							// add css to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.print");
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.images")
												+ "\\")) {
							// add images to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.images");
						} else if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.image")
												+ "\\")) {
							// add image to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.image");
						}
					} else if (movementType.equals("conn")) {
						// THIS SITUATION WILL COME WHEN CONN DIRECOTRY IS
						// PARALLEL TO HTML FOLDER - PROCESSING HTML5 DOCUMENTS
						destinationPath = destinationPath + "conn";
					} else if (movementType.equals("pdf")) {
						destinationPath = destinationPath + "pdf";
						/*
						 * check here if source File path has conn then add conn
						 * folder to the destination e.g. = /pdf/conn/abc.pdf
						 */
						if (PathUtil.winPath(sourceFile)
								.trim()
								.toLowerCase()
								.contains(
										"\\"
												+ ApplicationProperties
														.getProperty("directory.conn")
												+ "\\")) {
							// add conn to destination Path
							destinationPath = destinationPath
									+ "/"
									+ ApplicationProperties
											.getProperty("directory.conn");
						}
					} else if (movementType.equals("xml")) {
						destinationPath = destinationPath + "xml";
					} else if (movementType.equals("xls")) {
						destinationPath = destinationPath + "xls";
					} else if (movementType.equals("zip")) {
						destinationPath = destinationPath + "zip";
					} else if (movementType.equals("xlsx")) {
						destinationPath = destinationPath + "xlsx";
					} else if (movementType.equals("doc")) {
						destinationPath = destinationPath + "doc";
					}

					String tempDestinationPathForJS = destinationPath;

					// Now check for all the folders whether they exist or not
					// in
					// the destination path
					destinationPath = createFolderStructure(
							destinationPath,
							ApplicationProperties
									.getProperty("SERVER_OKASSETS_PHYSICAL_PATH"));

					/*
					 * call function to add File to Processing List
					 */
					captureFileProcessingeDetails(sourceFile.getName(),
							PathUtil.winPath(sourceFile), destinationPath,
							contentDetails);

					if (null != destinationPath && !"".equals(destinationPath)) {
						logger.info("copyFilesToServer :: Moving Input File {"
								+ sourceFile.getName() + "} at Path :: >"
								+ destinationPath);

						File destinationFilePath = PathUtil.file(destinationPath);

						PathUtil.copyFileToDirectory(PathUtil.winPath(sourceFile), destinationFilePath);

						String testDestFilePath = destinationPath + "/"
								+ sourceFile.getName();
						File testCheck = PathUtil.file(testDestFilePath);
						if (testCheck.isFile() && testCheck.exists()) {
							logger.info("copyFilesToServer :: Input File {"
									+ sourceFile.getName()
									+ "}. Moved Successfully at Path :: >"
									+ destinationPath);

							System.out
									.println("copyFilesToServer :: Input File {"
											+ sourceFile.getName()
											+ "}. Moved Successfully at Path :: >"
											+ destinationPath);

							/*
							 * UPDATE OK ASSETS PROCESSING COUNT TO 1
							 */
							ScheduleDAO.updateProcessingOKAssetsCount(
									contentDetails.getScheduleId(),
									contentDetails.getItemId());
							/*
							 * WINDOW.JS OPERATION ONLY WHEN MOVEMENT TYPE IS
							 * HTML
							 */
							if (movementType.equals("html")) {
								/*
								 * Check here, if source File Name is window.js
								 * then update the path of conn folder in each
								 * js tag search for conn and append destination
								 * path of html folder
								 */
								if (testCheck.getName().toLowerCase().equals("window.js")) 
								{
									logger.info("copyFilesToServer :: Input File is WINDOW.JS. Update Conn Path.");

									if (!tempDestinationPathForJS.endsWith("/")) {
										tempDestinationPathForJS = tempDestinationPathForJS
												+ "/";
									}

									/*
									 * call function to perform Windows
									 * Operations
									 */
									String jsContent = wiringDiagramUtils
											.performWindowsJSOperationForMC(
													testCheck,
													tempDestinationPathForJS,
													localeFolderName, contentDetails);
									if (null != jsContent && !"".equals(jsContent)) {
										// if
										// (!tempDestinationPathForJS.endsWith("/"))
										// {
										// tempDestinationPathForJS =
										// tempDestinationPathForJS
										// + "/";
										// }
										// jsContent = jsContent
										// .replace(
										// ConfigurationPropertiesUtil
										// .getProperty("directory.conn"),
										// tempDestinationPathForJS
										// + ConfigurationPropertiesUtil
										// .getProperty("directory.conn"));

										/*
										 * update JSContent in the destination
										 * File
										 */

										File jsFile = PathUtil.file(testDestFilePath);
										jsFile.createNewFile();
										FileOutputStream fos = PathUtil.fileOutputStream(
												jsFile);
										fos.write(jsContent.getBytes());
										fos.flush();
										fos.close();
										fos = null;
										jsFile = null;
									}
									jsContent = null;

									/*
									 * KAPTURE: the html and pdf addresses window.js opens are /library/...;
									 * the browser reaches OKAssets at /content/library/... - updated in place
									 */
									wiringDiagramUtils.prefixOkAssetsContextInWindowJS(testCheck);
								}
							}

							if (movementType.equals("html5")) {
								/*
								 * CHECK WHETHER THE PATH CONTAINS - JS
								 * DIRECTORY OR NOT
								 */
								if (PathUtil.winPath(testCheck).trim().toLowerCase().contains("\\"+ ApplicationProperties.getProperty("directory.js")+ "\\"))
								{
									/*
									 * NOW CHECK IF FILE IS NOT COMMON.JS
									 */
									if (!testCheck.getName().trim().toLowerCase().equals("common.js")) 
									{
										logger.info("copyFilesToServer :: Input File is {"
												+ testCheck.getName()
												+ "}. Update Conn Path.");

										if (!tempDestinationPathForJS
												.endsWith("/")) {
											tempDestinationPathForJS = tempDestinationPathForJS
													+ "/";
										}

										// UPDATE THE / CONN PATH WITH THE TEMP
										// FOLDER PATH
										String jsContent = wiringDiagramUtils
												.updateJSContentForHTML5(
														testCheck,
														tempDestinationPathForJS);
										if (null != jsContent
												&& !"".equals(jsContent)) {

											/*
											 * update JSContent in the
											 * destination File
											 */

											File jsFile = PathUtil.file(
													testDestFilePath);
											jsFile.createNewFile();
											FileOutputStream fos = PathUtil.fileOutputStream(
													jsFile);
											fos.write(jsContent.getBytes());
											fos.flush();
											fos.close();
											fos = null;
											jsFile = null;
										}
										jsContent = null;
									}
								}
								
								/*
								 * CHECK FOR VOLTAGE MAP & VOLTAGE LINK MAP JS
								 */
								if(null!=contentDetails && null!=contentDetails.getMarket() && contentDetails.getMarket().toLowerCase().equals("mc"))
								{
									// MC MARKET
									if(testCheck.getName().trim().toLowerCase().equals("voltageMAP.js".toLowerCase()))
									{
										logger.info("copyFilesToServer :: Input File is voltageMAP.JS. Update Inner Links Path.");
										/*
										 * call function to perform Windows
										 * Operations
										 */
										String jsContent = wiringDiagramUtils.performVoltageMapJSOperation(testCheck, localeFolderName, contentDetails);
										if (null != jsContent && !"".equals(jsContent)) 
										{
											/*
											 * update JSContent in the destination
											 * File
											 */
											File jsFile = PathUtil.file(testDestFilePath);
											jsFile.createNewFile();
											FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
											fos.write(jsContent.getBytes());
											fos.flush();
											fos.close();
											fos = null;
											jsFile = null;
										}
										jsContent = null;
									}
									
									/*
									 * CHECK FOR VOLTAGE LINK MAP JS
									 */
									if(testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP.js".toLowerCase()))
									{
										logger.info("copyFilesToServer :: Input File is voltagelinkMAP.JS. Update Inner Links Path.");
										/*
										 * call function to perform Windows
										 * Operations
										 */
										String jsContent = wiringDiagramUtils.performVoltageMapLinkJSOperation(testCheck, localeFolderName, contentDetails);
										if (null != jsContent && !"".equals(jsContent)) 
										{
											/*
											 * update JSContent in the destination
											 * File
											 */
											File jsFile = PathUtil.file(testDestFilePath);
											jsFile.createNewFile();
											FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
											fos.write(jsContent.getBytes());
											fos.flush();
											fos.close();
											fos = null;
											jsFile = null;
										}
										jsContent = null;
									}
								}
								else
								{
									// MME CONDITION 
									if(testCheck.getName().trim().toLowerCase().equals("voltageMAP.js".toLowerCase()) || 
											testCheck.getName().trim().toLowerCase().equals("voltageMAP_ECE.js".toLowerCase()) || 
											testCheck.getName().trim().toLowerCase().equals("voltageMAP_UK.js".toLowerCase()))
									{
										logger.info("copyFilesToServer :: Input File is "+testCheck.getName()+". Update Inner Links Path.");
										/*
										 * call function to perform Windows
										 * Operations
										 */
										String jsContent = wiringDiagramUtils.performVoltageMapJSOperation(testCheck, localeFolderName, contentDetails);
										if (null != jsContent && !"".equals(jsContent)) 
										{
											/*
											 * update JSContent in the destination
											 * File
											 */
											File jsFile = PathUtil.file(testDestFilePath);
											jsFile.createNewFile();
											FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
											fos.write(jsContent.getBytes());
											fos.flush();
											fos.close();
											fos = null;
											jsFile = null;
										}
										jsContent = null;
									}
									
									/*
									 * CHECK FOR VOLTAGE LINK MAP JS
									 */
									if(testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP.js".toLowerCase()) || 
											testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP_ECE.js".toLowerCase()) || 
											testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP_UK.js".toLowerCase()))
									{
										logger.info("copyFilesToServer :: Input File is "+testCheck.getName()+". Update Inner Links Path.");
										/*
										 * call function to perform Windows
										 * Operations
										 */
										String jsContent = wiringDiagramUtils.performVoltageMapLinkJSOperation(testCheck, localeFolderName, contentDetails);
										if (null != jsContent && !"".equals(jsContent)) 
										{
											/*
											 * update JSContent in the destination
											 * File
											 */
											File jsFile = PathUtil.file(testDestFilePath);
											jsFile.createNewFile();
											FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
											fos.write(jsContent.getBytes());
											fos.flush();
											fos.close();
											fos = null;
											jsFile = null;
										}
										jsContent = null;
									}
								}
								
								
							}

						} else {
							logger.info("copyFilesToServer :: Failed to move Input File {"
									+ sourceFile.getName()
									+ "} at Path :: >"
									+ destinationPath);

							System.err
									.println("copyFilesToServer :: Failed to move Input File {"
											+ sourceFile.getName()
											+ "} at Path :: >"
											+ destinationPath);

							/*
							 * Track Error
							 */
							// UPDATE FAILURE COUNT AS WELL
							updateFailureCount = true;
							String errorMessage = "Failed to move File at the Destination Path.";
							captureFileUploadFailureDetails(
									sourceFile.getName(),
									PathUtil.winPath(sourceFile),
									destinationPath, errorMessage,
									contentDetails);
							// set errorMessage to null
							errorMessage = null;
						}
						// set testCheck to null
						testCheck = null;
						// set testDestFilePath to null
						testDestFilePath = null;
						// set destionationFilePath to null
						destinationFilePath = null;
					} else {
						logger.info("copyFilesToServer :: Failed to Read Desired Directory Structure at Destination. Path :: >"
								+ destinationPath);
						System.err
								.println("copyFilesToServer :: Failed to Read Desired Directory Structure at Destination. Path :: >"
										+ destinationPath);
						/*
						 * Track Error
						 */
						// UPDATE FAILURE COUNT AS WELL
						updateFailureCount = true;
						String errorMessage = "Failed to Read Directory Structure at the Destination Path.";
						captureFileUploadFailureDetails(sourceFile.getName(),
								PathUtil.winPath(sourceFile), destinationPath,
								errorMessage, contentDetails);
						// set errorMessage to null
						errorMessage = null;
					}
					// set destinationPath to null
					destinationPath = null;
					// set tempDestinationPathForJS to null
					tempDestinationPathForJS = null;
				} else {
					logger.info("copyFilesToServer :: Source File Details, Locale and Channel Folder Names are as Parameter. Skip File.");
					System.err
							.println("copyFilesToServer :: Source File Details, Locale and Channel Folder Names are as Parameter. Skip File.");
					/*
					 * Track Error
					 */
					// UPDATE FAILURE COUNT AS WELL
					updateFailureCount = true;
					String errorMessage = "Source File Details is not a Valid File.";
					captureFileUploadFailureDetails("", "", "", errorMessage,
							contentDetails);
					// set errorMessage to null
					errorMessage = null;
				}
			} else {
				logger.info("copyFilesToServer :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
				System.out
						.println("copyFilesToServer :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),
					"copyFilesToServer()", e);
		}

		if (updateFailureCount == true) {
			try {
				/*
				 * UPDATE FAILURE OKASSETS COUNT BY 1
				 */
				ScheduleDAO.updateFailureOKAssetsCount(
						contentDetails.getScheduleId(),
						contentDetails.getItemId());
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),
						"copyFilesToServer()", e);
			}
		}
	}

	public void copyFilesToServerForHTML5(File sourceFile, String localeFolderName,
			String channelFolderName, String modelFolderName,
			String manualTypeFolderName, String faceLiftFolderName,
			String materialFolderName, String movementType,
			ContentDetails contentDetails, WiringDiagramUtils wiringDiagramUtils) {
		try {
			boolean proceedFurther = false;
			if (null != XCOPY_FLAG && !"".equals(XCOPY_FLAG)
					&& XCOPY_FLAG.toLowerCase().equals("y")) {
				// set proceed to true
				proceedFurther = true;
			}

			if (proceedFurther == true) {
				if (null != sourceFile && sourceFile.exists()
						&& null != localeFolderName
						&& !"".equals(localeFolderName)
						&& null != channelFolderName
						&& !"".equals(channelFolderName)) {

					logger.info("copyFilesToServerForHTML5 :: Source File, Locale and Channel Folders Names are not null as Parameter. Proceed for Uploading File.");
					/*
					 * Prepare Destination Path /library/MAZDAESI/Service
					 * Information/en_us/image/ac5uuw00000002.gif
					 */

					// PREPARE CHANNEL REF KEY
					channelFolderName = channelFolderName.replace(" ", "_");

					String destinationPath = "";
					destinationPath = ApplicationProperties
							.getProperty("SERVER_LIBRARY_DIRECTORY");
					// add repository
					destinationPath = destinationPath
							+ ApplicationProperties.getProperty("REPOSITORY")
									.toUpperCase() + "/";
					// add channel Name
					destinationPath = destinationPath
							+ channelFolderName.toUpperCase() + "/";
					// add locale Folder Name in lowercase
					destinationPath = destinationPath
							+ ApplicationProperties
									.getProperty(
											localeFolderName.trim()
													.toLowerCase()).trim()
									.toLowerCase() + "/";

					if (null != modelFolderName && !"".equals(modelFolderName)) {
						// add model Folder Name in lowercase
						destinationPath = destinationPath
								+ modelFolderName.toLowerCase() + "/";
					}

					if (null != manualTypeFolderName
							&& !"".equals(manualTypeFolderName)) {
						// add manualType Folder Name in lowercase
						destinationPath = destinationPath
								+ manualTypeFolderName.toLowerCase() + "/";
					}

					if (null != faceLiftFolderName
							&& !"".equals(faceLiftFolderName)) {
						// add faceLift Folder Name in lowercase
						destinationPath = destinationPath
								+ faceLiftFolderName.toLowerCase() + "/";
					}
					if (null != materialFolderName
							&& !"".equals(materialFolderName)) {
						// add materialFolderName in as in lowercase
						destinationPath = destinationPath
								+ materialFolderName.toLowerCase() + "/";
					}

					// add html5 folder
					destinationPath = destinationPath+"html5";

					// Now check for all the folders whether they exist or not
					// in
					// the destination path
					destinationPath = createFolderStructure(
							destinationPath,
							ApplicationProperties
									.getProperty("SERVER_OKASSETS_PHYSICAL_PATH"));

					moveHTML5Files(destinationPath, sourceFile, contentDetails, wiringDiagramUtils);
					
					// set destinationPath to null
					destinationPath = null;
				} else {
					logger.info("copyFilesToServerForHTML5 :: Source File Details, Locale and Channel Folder Names are as Parameter. Skip File.");
					/*
					 * Track Error
					 */
					String errorMessage = "Source HTML5 directory Details are not Valid.";
					captureFileUploadFailureDetails("", "", "", errorMessage,
							contentDetails);
					// set errorMessage to null
					errorMessage = null;
				}
			} else {
				logger.info("copyFilesToServerForHTML5 :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),
					"copyFilesToServerForHTML5()", e);
		}
	}
	
	private void moveHTML5Files(String destinationPath, File htmlFolder, ContentDetails contentDetails, WiringDiagramUtils  wiringDiagramUtils)
	{
		boolean updateFailureCount=false;
		try
		{
			File[] htmlFiles = htmlFolder.listFiles();
			File destinationFilePath=null;
			String testDestFilePath=null;
			File testCheck=null;
			if(null!=htmlFiles && htmlFiles.length>0)
			{
				// first process only Files
				for(int a=0;a<htmlFiles.length;a++)
				{
					if(htmlFiles[a].isFile())
					{
						if(!htmlFiles[a].getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
						{
							/*
							 * call function to add File to Processing List
							 */
							captureFileProcessingeDetails(htmlFiles[a].getName(), PathUtil.winPath(htmlFiles[a]), destinationPath, contentDetails);
							
							if(null!=destinationPath && !"".equals(destinationPath))
							{
								logger.info("copyFilesToServerForHTML5 :: Moving Input File {"+ htmlFiles[a].getName() + "} at Path :: >"	+ destinationPath);

								destinationFilePath = PathUtil.file(destinationPath);

								PathUtil.copyFileToDirectory(PathUtil.winPath(htmlFiles[a]), destinationFilePath);
								
								testDestFilePath = destinationPath + "/"
										+ htmlFiles[a].getName();
								testCheck = PathUtil.file(testDestFilePath);
								if (testCheck.isFile() && testCheck.exists()) 
								{
									logger.info("copyFilesToServerForHTML5 :: Input File {"	+ htmlFiles[a].getName()+ "}. Moved Successfully at Path :: >"+ destinationPath);
									/*
									 * CHECK FOR VOLTAGE MAP & VOLTAGE LINK MAP JS
									 * voltageMAP_ECE.js & voltageMAP_UK.js ARE ONLY APPLICABLE FOR MME MARKET
									 */
									if(null!=contentDetails && null!=contentDetails.getMarket() && contentDetails.getMarket().toLowerCase().equals("mc"))
									{
										// MC CONDITION
										if(testCheck.getName().trim().toLowerCase().equals("voltageMAP.js".toLowerCase()))
										{
											logger.info("copyFilesToServerForHTML5 :: Input File is voltageMAP.JS. Update Inner Links Path.");
											/*
											 * call function to perform Windows
											 * Operations
											 */
											String jsContent = wiringDiagramUtils.performVoltageMapJSOperation(testCheck, contentDetails.getLocale(), contentDetails);
											if (null != jsContent && !"".equals(jsContent)) 
											{
												/*
												 * update JSContent in the destination
												 * File
												 */
												File jsFile = PathUtil.file(testDestFilePath);
												jsFile.createNewFile();
												FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
												fos.write(jsContent.getBytes());
												fos.flush();
												fos.close();
												fos = null;
												jsFile = null;
											}
											jsContent = null;
										}
										
										/*
										 * CHECK FOR VOLTAGE LINK MAP JS
										 */
										if(testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP.js".toLowerCase()))
										{
											logger.info("copyFilesToServerForHTML5 :: Input File is voltagelinkMAP.JS. Update Inner Links Path.");
											/*
											 * call function to perform Windows
											 * Operations
											 */
											String jsContent = wiringDiagramUtils.performVoltageMapLinkJSOperation(testCheck, contentDetails.getLocale(), contentDetails);
											if (null != jsContent && !"".equals(jsContent)) 
											{
												/*
												 * update JSContent in the destination
												 * File
												 */
												File jsFile = PathUtil.file(testDestFilePath);
												jsFile.createNewFile();
												FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
												fos.write(jsContent.getBytes());
												fos.flush();
												fos.close();
												fos = null;
												jsFile = null;
											}
											jsContent = null;
										}
										
									}
									else
									{
										// MME CONDITION
										if(testCheck.getName().trim().toLowerCase().equals("voltageMAP.js".toLowerCase()) || 
												testCheck.getName().trim().toLowerCase().equals("voltageMAP_ECE.js".toLowerCase()) || 
												testCheck.getName().trim().toLowerCase().equals("voltageMAP_UK.js".toLowerCase()))
										{
											logger.info("copyFilesToServerForHTML5 :: Input File is "+testCheck.getName()+". Update Inner Links Path.");
											/*
											 * call function to perform Windows
											 * Operations
											 */
											String jsContent = wiringDiagramUtils.performVoltageMapJSOperation(testCheck, contentDetails.getLocale(), contentDetails);
											if (null != jsContent && !"".equals(jsContent)) 
											{
												/*
												 * update JSContent in the destination
												 * File
												 */
												File jsFile = PathUtil.file(testDestFilePath);
												jsFile.createNewFile();
												FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
												fos.write(jsContent.getBytes());
												fos.flush();
												fos.close();
												fos = null;
												jsFile = null;
											}
											jsContent = null;
										}
										
										/*
										 * CHECK FOR VOLTAGE LINK MAP JS
										 */
										if(testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP.js".toLowerCase()) || 
												testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP_ECE.js".toLowerCase()) || 
												testCheck.getName().trim().toLowerCase().equals("voltagelinkMAP_UK.js".toLowerCase()))
										{
											logger.info("copyFilesToServerForHTML5 :: Input File is "+testCheck.getName()+". Update Inner Links Path.");
											/*
											 * call function to perform Windows
											 * Operations
											 */
											String jsContent = wiringDiagramUtils.performVoltageMapLinkJSOperation(testCheck, contentDetails.getLocale(), contentDetails);
											if (null != jsContent && !"".equals(jsContent)) 
											{
												/*
												 * update JSContent in the destination
												 * File
												 */
												File jsFile = PathUtil.file(testDestFilePath);
												jsFile.createNewFile();
												FileOutputStream fos = PathUtil.fileOutputStream(jsFile);
												fos.write(jsContent.getBytes());
												fos.flush();
												fos.close();
												fos = null;
												jsFile = null;
											}
											jsContent = null;
										}
										
									}
									
									/*
									 * UPDATE OK ASSETS PROCESSING COUNT TO 1
									 */
									ScheduleDAO.updateProcessingOKAssetsCount(contentDetails.getScheduleId() , contentDetails.getItemId());
								}
								else
								{
									logger.info("copyFilesToServerForHTML5 :: Failed to move Input File {"+ htmlFiles[a].getName()+ "} at Path :: >"
											+ destinationPath);

									/*
									 * Track Error
									 */
									updateFailureCount=true;
									String errorMessage = "Failed to move File at the Destination Path.";
									captureFileUploadFailureDetails(htmlFiles[a].getName(),PathUtil.winPath(htmlFiles[a]),destinationPath, errorMessage, contentDetails);
									// set errorMessage to null
									errorMessage = null;
								}
								destinationFilePath=null;
								testCheck=null;
								testDestFilePath=null;
							}
							else
							{
								logger.info("copyFilesToServerForHTML5 :: Failed to Read Desired Directory Structure at Destination. Path :: >"
										+ destinationPath);
								/*
								 * Track Error
								 */
								updateFailureCount=true;
								String errorMessage = "Failed to Read Directory Structure at the Destination Path.";
								captureFileUploadFailureDetails(htmlFiles[a].getName(),PathUtil.winPath(htmlFiles[a]), destinationPath,errorMessage, contentDetails);
								// set errorMessage to null
								errorMessage = null;
							}
							
							if(updateFailureCount==true)
							{
								try {
									/*
									 * UPDATE FAILURE OKASSETS COUNT BY 1
									 */
									ScheduleDAO.updateFailureOKAssetsCount(
											contentDetails.getScheduleId(),
											contentDetails.getItemId());
								} catch (Exception e) {
									Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),
											"copyFilesToServer()", e);
								}
							}
						}
					}
				}
				
				// now process only Directories
				for(int a=0;a<htmlFiles.length;a++)
				{
					if(htmlFiles[a].isDirectory())
					{
						String tempDesPath="";
						try
						{
							tempDesPath = destinationPath+"/"+htmlFiles[a].getName();
							destinationFilePath = PathUtil.file(tempDesPath);
							if(!destinationFilePath.exists() || !destinationFilePath.isDirectory())
							{
								// create DIR
								destinationFilePath.mkdir();
							}
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(XcopyUtil.class.getName(), "copyFilesToServerForHTML5", e);
							tempDesPath=null;
						}
						// call recursive function
						moveHTML5Files(tempDesPath, htmlFiles[a], contentDetails, wiringDiagramUtils);
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(), "copyFilesToServerForHTML5", e);
		}
	}
	

	/**
	 * Copies one file into the given directory of OKAssets - the directory is used exactly as
	 * given (no change of case, no fixed sub folder) and is created when missing.
	 * Tracked and counted like every other OKAssets file.
	 *
	 * @param destinationDirectory physical directory, with forward slashes
	 */
	/** OKAssets folders already made by copyFileKeepingSourcePath - a folder check on the S3 mount is a remote call. */
	private static final java.util.Set<String> MADE_FOLDERS = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

	/**
	 * Copies one file into the given OKAssets folder (OEM content). Called by several copy threads
	 * at once. A failed copy is known from the copy itself - no second look at the S3 mount.
	 */
	public void copyFileKeepingSourcePath(File sourceFile, File destinationDirectory, ContentDetails contentDetails) {
		boolean copied = false;
		String destination = PathUtil.winPath(destinationDirectory);
		try {
			if (null != XCOPY_FLAG && XCOPY_FLAG.toLowerCase().equals("y")) {
				captureFileProcessingeDetails(sourceFile.getName(), PathUtil.winPath(sourceFile), destination, contentDetails);
				try {
					String key = destinationDirectory.getPath();
					if (!MADE_FOLDERS.contains(key)) {
						java.nio.file.Files.createDirectories(destinationDirectory.toPath());
						MADE_FOLDERS.add(key);
					}
					java.nio.file.Files.copy(sourceFile.toPath(), new File(destinationDirectory, sourceFile.getName()).toPath(),
							java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					copied = true;
				} catch (java.io.IOException ce) {
					logger.info("copyFileKeepingSourcePath :: Failed to move Input File {" + sourceFile.getName() + "} at Path :: >" + destination + " :: " + ce);
					captureFileUploadFailureDetails(sourceFile.getName(), PathUtil.winPath(sourceFile), destination,
							"Failed to move File at the Destination Path.", contentDetails);
				}
				if (copied) {
					ScheduleDAO.updateProcessingOKAssetsCount(contentDetails.getScheduleId(), contentDetails.getItemId());
				}
			} else {
				logger.info("copyFileKeepingSourcePath :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
				return;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(), "copyFileKeepingSourcePath()", e);
		}
		if (copied == false) {
			try {
				ScheduleDAO.updateFailureOKAssetsCount(contentDetails.getScheduleId(), contentDetails.getItemId());
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(XcopyUtil.class.getName(), "copyFileKeepingSourcePath()", e);
			}
		}
	}

	/**
	 * Function will create desired directory structure in the destination path
	 * 
	 * @param tempPath
	 * @return
	 */
	public String createFolderStructure(String tempPath, String serverPath) {
		StringTokenizer str = new StringTokenizer(tempPath, "/");
		int i = 0;
		File file;
		String token;
		String path = serverPath;
		while (str.hasMoreTokens()) {
			i = i + 1;
			if (i > 2) {
				token = str.nextToken();
				if (path.substring(path.length() - 1, path.length())
						.equalsIgnoreCase("/")) {
					path = path + token;
				} else {
					path = path + "/" + token;
				}
				file = PathUtil.file(path);
				if (null != file && file.isDirectory()) {
					continue;
				} else {
					file.mkdir();
				}
			}
		}
		return path;
	}

	/**
	 * Function will be used to track and store File processing details
	 * 
	 * @param fileName
	 * @param sourcePath
	 * @param destinationPath
	 */
	private synchronized void captureFileProcessingeDetails(String fileName,
			String sourcePath, String destinationPath,
			ContentDetails contentDetails) {
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
			dataMap.put("DATE_TIME",
					DateFormatter.converTimeStampToString(new Timestamp(
							new Date().getTime())));

			/*
			 * check here getFileUploadFailureList is null then initialize it
			 */
			if (null == getFileProcessingList()
					|| getFileProcessingList().size() <= 0) {
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
	private synchronized void captureFileUploadFailureDetails(String fileName,
			String sourcePath, String destinationPath, String errorMessage,
			ContentDetails contentDetails) {
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

	/**
	 * Function will move inputFile from sourceLocation to destinationLocation
	 * 
	 * @param inputFileDetailsVO
	 */
	public void copyPDFFilesToLiveStagingFolder(ContentDetails contentVO) {
		try {
			boolean proceedFurther = false;
			if (null != XCOPY_FLAG && !"".equals(XCOPY_FLAG)
					&& XCOPY_FLAG.toLowerCase().equals("y")) {
				// set proceed to true
				proceedFurther = true;
			}

			if (proceedFurther == true) {
				if (null != contentVO && null != contentVO.getImDocumentId()
						&& !"".equals(contentVO.getImDocumentId())) {
					logger.info("copyPDFFilesToLiveStagingFolder :: Content Details VO is not null as Parameter. Proceed for Copying File.");

					String sourcePath = contentVO.getPdfFilePathAsAttachment();
					if (null != sourcePath && !"".equals(sourcePath)
							&& null != contentVO.getImResourcePath()
							&& !"".equals(contentVO.getImResourcePath())) {

						/*
						 * Prepare Destination Path
						 */
						String destinationPath = ApplicationProperties
								.getProperty("SERVER_RESOURCES_PATH")
								+ contentVO.getImResourcePath();
						/*
						 * Also move File to LIVE Folder parallel to Stating
						 * Folder, here, replace the staging with live and also
						 * remove the version no from the resourcePath
						 */
						String temp = "";
						if (null != contentVO.getImResourcePath()
								&& !"".equals(contentVO.getImResourcePath())) {
							temp = contentVO.getImResourcePath().replace(
									"staging", "live");
							/*
							 * now check if last character is / then remove it
							 * else keep the url as it is
							 */
							if (temp.substring(temp.length() - 1, temp.length())
									.equals("/")) {
								temp = temp.substring(0, temp.length() - 1);
							}
							/*
							 * now get the last index of / and remove the
							 * version no
							 */
							temp = temp.substring(0, temp.lastIndexOf("/") + 1);

						}
						String anotherDestinationPath = ApplicationProperties
								.getProperty("SERVER_RESOURCES_PATH") + temp;
						// set temp to null

						temp = null;
						if (null != destinationPath
								&& !"".equals(destinationPath)
								&& null != anotherDestinationPath
								&& !"".equals(anotherDestinationPath)) {
							logger.info("copyPDFFilesToLiveStagingFolder :: Desired Destination Directory Structure Created. Path :: >"
									+ destinationPath);
							logger.info("copyPDFFilesToLiveStagingFolder :: Desired Destination Directory Structure Created. Path :: >"
									+ anotherDestinationPath);

							/*
							 * proceed for moving file
							 */
							File sourceFilePath = PathUtil.file(sourcePath);
							File destinationFilePath = PathUtil.file(destinationPath);

							PathUtil.copyFileToDirectory(sourceFilePath, destinationFilePath);

							File anotherDestFilePath = PathUtil.file(
									anotherDestinationPath);
							PathUtil.copyFileToDirectory(sourceFilePath, anotherDestFilePath);

							/*
							 * Check whether the File exists or not at
							 * Destination add fileName to destinationPath and
							 * check whether exists or not
							 */
							String testDestFilePath = destinationPath + "/"
									+ contentVO.getPdfFileNameAsAttachment();

							String anotherTestDestFilePath = anotherDestinationPath
									+ "/"
									+ contentVO.getPdfFileNameAsAttachment();

							File testCheck = PathUtil.file(testDestFilePath);
							File anotherTestCheck = PathUtil.file(
									anotherTestDestFilePath);
							if (testCheck.isFile() && testCheck.exists()
									&& anotherTestCheck.isFile()
									&& anotherTestCheck.exists()) {
								logger.info("copyPDFFilesToLiveStagingFolder :: Input File {"
										+ contentVO
												.getPdfFileNameAsAttachment()
										+ "}. Moved Successfully at Path :: >"
										+ destinationPath);

								logger.info("copyPDFFilesToLiveStagingFolder :: Input File {"
										+ contentVO
												.getPdfFileNameAsAttachment()
										+ "}. Moved Successfully at Path :: >"
										+ anotherDestinationPath);
								System.out
										.println("copyPDFFilesToLiveStagingFolder :: Input File {"
												+ contentVO
														.getPdfFileNameAsAttachment()
												+ "}. Moved Successfully at Path :: >"
												+ destinationPath);

								System.out
										.println("copyPDFFilesToLiveStagingFolder :: Input File {"
												+ contentVO
														.getPdfFileNameAsAttachment()
												+ "}. Moved Successfully at Path :: >"
												+ anotherDestinationPath);
							} else {
								logger.info("copyPDFFilesToLiveStagingFolder :: Failed to move Input File {"
										+ contentVO
												.getPdfFileNameAsAttachment()
										+ "} at Path :: >" + destinationPath);

								logger.info("copyPDFFilesToLiveStagingFolder :: Failed to move Input File {"
										+ contentVO
												.getPdfFileNameAsAttachment()
										+ "} at Path :: >"
										+ anotherDestinationPath);
								System.err
										.println("copyPDFFilesToLiveStagingFolder :: Failed to move Input File {"
												+ contentVO
														.getPdfFileNameAsAttachment()
												+ "} at Path :: >"
												+ destinationPath);

								System.err
										.println("copyPDFFilesToLiveStagingFolder :: Failed to move Input File {"
												+ contentVO
														.getPdfFileNameAsAttachment()
												+ "} at Path :: >"
												+ anotherDestinationPath);
								/*
								 * Track Error
								 */
								String errorMessage = "Failed to move File at the Destination Path.";
								captureFileUploadFailureDetails(
										contentVO.getPdfFileNameAsAttachment(),
										sourcePath, destinationPath + " & "
												+ anotherDestinationPath,
										errorMessage, contentVO);
								// set errorMessage to null
								errorMessage = null;
							}
							// set testCheck to null
							testCheck = null;
							// set testDestFilePath to null
							testDestFilePath = null;
							// set anotherTestCheck to null
							anotherTestCheck = null;
							// set anotherTestDestFilePath to null
							anotherTestDestFilePath = null;
							// set sourceFilePath to null
							sourceFilePath = null;
							// set destionationFilePath to null
							destinationFilePath = null;
							// set anotherDestinationFilePath to null
							anotherDestFilePath = null;
						} else {
							logger.info("copyPDFFilesToLiveStagingFolder :: Failed to Read Desired Directory Structure at Destination. Path :: >"
									+ destinationPath);
							System.err
									.println("copyPDFFilesToLiveStagingFolder :: Failed to Read Desired Directory Structure at Destination. Path :: >"
											+ destinationPath);
							/*
							 * Track Error
							 */
							String errorMessage = "Failed to Read Directory Structure at the Destination Path.";
							captureFileUploadFailureDetails(
									contentVO.getPdfFileNameAsAttachment(),
									sourcePath, destinationPath + " & "
											+ anotherDestinationPath,
									errorMessage, contentVO);
							// set errorMessage to null
							errorMessage = null;
						}
						// set destinationPath to null
						destinationPath = null;
						// set anotherDestinationPath to null
						anotherDestinationPath = null;

					} else {
						logger.info("copyPDFFilesToLiveStagingFolder :: Source Path For File is null. Skip File.");
						System.err
								.println("copyPDFFilesToLiveStagingFolder :: Source Path For File is null. Skip File.");
						/*
						 * Track Error
						 */
						String errorMessage = "Source Path for the Input File is null.";
						captureFileUploadFailureDetails(
								contentVO.getPdfFileNameAsAttachment(), "", "",
								errorMessage, contentVO);
						// set errorMessage to null
						errorMessage = null;
					}
					// set sourcePath to null
					sourcePath = null;
				} else {
					logger.info("copyPDFFilesToLiveStagingFolder :: Input File Details VO is null as Parameter. Skip File.");
					System.err
							.println("copyPDFFilesToLiveStagingFolder :: Input File Details VO is null as Parameter. Skip File.");
					/*
					 * Track Error
					 */
					String errorMessage = "Input File Details for the File are null.";
					captureFileUploadFailureDetails(
							contentVO.getPdfFileNameAsAttachment(), "", "",
							errorMessage, contentVO);
					// set errorMessage to null
					errorMessage = null;
				}
			} else {

				logger.info("copyPDFFilesToLiveStagingFolder :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
				System.out
						.println("copyPDFFilesToLiveStagingFolder :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function will copy the source directories and files to the Arhive
	 * Directory
	 * 
	 * @param sourceFile
	 * @param sourcePath
	 * @param archivePath
	 */
	public void archiveFiles(File sourceFile, String sourcePath,
			String archivePath, ContentDetails contentDetails) {
		try {
			boolean proceedFurther = false;
			if (null != XCOPY_FLAG && !"".equals(XCOPY_FLAG)
					&& XCOPY_FLAG.toLowerCase().equals("y")) {
				// set proceed to true
				proceedFurther = true;
			}

			if (proceedFurther == true) {
				/*
				 * Here sourceFile will be the File that needs to be moved
				 * sourcePath will be the source Directory Name, e.g. -
				 * D:/XMLConversion_WD/SourceContent/Content_Delta_2014_acoragu
				 * archivePath will be the
				 * D:/XMLConversion_WD/Archive/Content_Delta_20140101
				 */
				if (null != sourceFile && sourceFile.exists()
						&& null != sourcePath && !"".equals(sourcePath)
						&& null != archivePath && !"".equals(archivePath)) {
					archivePath = archivePath.replace("\\", "/");
					/*
					 * check here if archivePath doesn't end with / then add it.
					 */
					if (!archivePath.endsWith("/")) {
						archivePath = archivePath + "/";
					}

					/*
					 * replace / in source Path by \\
					 */
					sourcePath = sourcePath.replace("/", "\\");

					/*
					 * now from the source file Path, get the INDEX OF
					 * sourcePath and get the Path after it
					 */
					if (PathUtil.winPath(sourceFile).indexOf(sourcePath) != -1) {
						String remainPath = PathUtil.winPath(sourceFile)
								.substring(
										PathUtil.winPath(sourceFile).indexOf(
												sourcePath)
												+ sourcePath.length(),
										PathUtil.winPath(sourceFile).length());
						/*
						 * now remove the file name from the remain path
						 */
						remainPath = remainPath.replace(sourceFile.getName(),
								"");

						// add remainPath to destination Path
						remainPath = remainPath.replace("\\", "/");

						/*
						 * check here if remainPath starts with / then remove
						 * it.
						 */
						if (remainPath.startsWith("/")) {
							remainPath = remainPath.substring(1,
									remainPath.length());
						}
						// add destination Path
						// remainPath = archivePath + remainPath;
						// call function to check and create folder structure
						String finalDestinationPath = createFolderStructure(
								remainPath, archivePath);

						if (null != finalDestinationPath
								&& !"".equals(finalDestinationPath)) {
							File destinationFilePath = PathUtil.file(
									finalDestinationPath);

							PathUtil.copyFileToDirectory(PathUtil.winPath(sourceFile), destinationFilePath);

							String testDestFilePath = finalDestinationPath
									+ "/" + sourceFile.getName();
							File testCheck = PathUtil.file(testDestFilePath);
							if (testCheck.isFile() && testCheck.exists()) {
								logger.info("archiveFiles :: Input File {"
										+ sourceFile.getName()
										+ "}. Moved Successfully at Path :: >"
										+ finalDestinationPath);

								System.out
										.println("archiveFiles :: Input File {"
												+ sourceFile.getName()
												+ "}. Moved Successfully at Path :: >"
												+ finalDestinationPath);
							} else {
								logger.info("archiveFiles :: Failed to move Input File {"
										+ sourceFile.getName()
										+ "} at Path :: >"
										+ finalDestinationPath);

								System.err
										.println("archiveFiles :: Failed to move Input File {"
												+ sourceFile.getName()
												+ "} at Path :: >"
												+ finalDestinationPath);

								/*
								 * Track Error
								 */
								String errorMessage = "Failed to move File at the Destination Path.";
								captureFileUploadFailureDetails(
										sourceFile.getName(),
										PathUtil.winPath(sourceFile),
										finalDestinationPath, errorMessage,
										contentDetails);
								// set errorMessage to null
								errorMessage = null;
							}
							// set testCheck to null
							testCheck = null;
							// set testDestFilePath to null
							testDestFilePath = null;
							// set destionationFilePath to null
							destinationFilePath = null;
						} else {
							logger.info("archiveFiles :: Failed to Read Desired Directory Structure at Destination. Path :: >"
									+ remainPath);
							System.err
									.println("archiveFiles :: Failed to Read Desired Directory Structure at Destination. Path :: >"
											+ remainPath);
							/*
							 * Track Error
							 */
							String errorMessage = "Failed to Read Directory Structure at the Destination Path.";
							captureFileUploadFailureDetails(
									sourceFile.getName(),
									PathUtil.winPath(sourceFile), remainPath,
									errorMessage, contentDetails);
							// set errorMessage to null
							errorMessage = null;
						}
					}
				} else {
					logger.info("archiveFiles :: Input Parameters are passed as null. Exiting.");
				}
			} else {
				logger.info("archiveFiles :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
				System.out
						.println("archiveFiles :: XCOPY Flag is set to FALSE (N). Exiting without Moving file.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),
					"archiveFiles()", e);
		}
	}
}
