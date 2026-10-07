package com.mazda.gms3.dmt.conversion.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;


public class ArchiveUtils {

	Logger logger = LogManager.getLogger(ArchiveUtils.class);
	
	String archiveFlag=ApplicationProperties.getProperty("ARCHIVE_FLAG");
	
	
	public void archiveRepository(String serverPath, ArrayList<ScheduleItemDetails> itemsList, String scheduleId, XcopyUtil xcopyUtils)
	{
		try
		{
			if(null!=archiveFlag  && !"".equals(archiveFlag) && archiveFlag.equals("TRUE"))
			{
				String gms3ContentFolderName = ApplicationProperties.getProperty("ARCHIVE_GMS3_CONTENT_DIECTORY_NAME");
				
				if(null!=itemsList && itemsList.size()>0)
				{
					for(int a=0;a<itemsList.size();a++)
					{
						ScheduleItemDetails itemDetails = (ScheduleItemDetails)itemsList.get(a);
						/*
						 * Fetch Market, Locale , Model and Manual Type from the Item Details
						 * check for each whether the directory exists or not
						 * 
						 * NOT MODEL, IT HAS TO BE MODEL FOLDER NAME + MATERIANL NAME
						 * 
						 * The data will move at 2 locations -
						 *   FULL CONTENT - SERVER PATH + MARKET + LOCALE + MODEL + MANUAL TYPR _ MATERIAL FOLDER NAME
						 *   HISTORY CONTENT - SERVER PATH + MARKET + LOCALE + MODEL + MANUAL TYPR _ MATERIAL FOLDER NAME_TIMESTAMP
						 * 
						 */
						
						/*
						 * BEFORE MOVING DATA TO FULL CONTENT DIRECTORY
						 * CHECK IT MATERIAL FOLDER ALREADY EXISTS - THEN DELETE IT FIRST
						 * 
						 * CHANGE - 25 JAN 2017
						 * ADD GMS3_Content Folder Name to Both ARCHIVE_FULL_CONTENT_DIRECTORY & ARCHIVE_HISTORY_CONTENT_DIRECTORY
						 * 
						 * 
						 * 
						 */
						String checkArcFullPath=ApplicationProperties.getProperty("ARCHIVE_FULL_CONTENT_DIRECTORY");
						if(null!=checkArcFullPath && !"".equals(checkArcFullPath))
						{
							if(!checkArcFullPath.endsWith("/"))
							{
								checkArcFullPath = checkArcFullPath+"/";
							}
							// add gms3ContentFolderName/Market/Locale/Model/ManualType/MaterialFolder
							checkArcFullPath = checkArcFullPath+gms3ContentFolderName+"/"+itemDetails.getMarket()+"/"+itemDetails.getLocale()+"/"+
							itemDetails.getModelFolderName()+"/"+itemDetails.getManualType()+"/"+itemDetails.getMaterialFolderName();
							File existArchMatFolder = PathUtil.file(checkArcFullPath);
							if(existArchMatFolder.exists() && existArchMatFolder.isDirectory())
							{
								logger.info("startConversionProcess :: Proceed for Deleting {"+itemDetails.getMaterialFolderName()+"} Exists in Full Content at Path :: > "+ PathUtil.winPath(existArchMatFolder));
								deleteSourceDirectory(existArchMatFolder);
							}
							existArchMatFolder=  null;
							checkArcFullPath= null;
						}
						
						
						String sourcePath=serverPath+ itemDetails.getMarket()+"\\"+itemDetails.getLocale()+"\\"+
						 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType()+"\\"+ itemDetails.getMaterialFolderName();
					
						logger.info("startConversionProcess :: Proceed for Archiving  Directory :: > " + sourcePath);
						/*
						 * call function to archive Data to Server
						 */
						// add destinationDirectory to Archive Path
						/*
						 * also Add MARKET, LOCALE, MODEL NAME TO THE PATH
						 * AND CHECK FOR EACH IF DIRECOTRY DOESNOT EXISTS THEN CREATE IT
						 */
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
						String displayDate = sdf.format(new Date());
						String archivePath=ApplicationProperties.getProperty("ARCHIVE_FULL_CONTENT_DIRECTORY");
						String archiveLocationPathHistoryContent=ApplicationProperties.getProperty("ARCHIVE_HISTORY_CONTENT_DIRECTORY");
						
						
						// ARCHIVE A FULL CONTENT COPY
						File archFile = PathUtil.file(archivePath);
						if(!archFile.exists() || !archFile.isDirectory())
						{
							archFile.mkdir();
						}
						if(!archivePath.endsWith("/"))
						{
							archivePath = archivePath+"/";
						}
						// ADD GMS3_Content Directory
						archivePath = archivePath+gms3ContentFolderName;
						File gms3Dir = PathUtil.file(archivePath);
						if(!gms3Dir.exists() || !gms3Dir.isDirectory())
						{
							gms3Dir.mkdir();
						}
						if(!archivePath.endsWith("/"))
						{
							archivePath = archivePath+"/";
						}
						
						// NOW ADD MARKET
						archivePath = archivePath+itemDetails.getMarket();
						archFile = PathUtil.file(archivePath);
						if(!archFile.exists() || !archFile.isDirectory())
						{
							archFile.mkdir();
						}
						// NOW ADD LOCALE
						archivePath = archivePath+"/"+itemDetails.getLocale();
						archFile = PathUtil.file(archivePath);
						if(!archFile.exists() || !archFile.isDirectory())
						{
							archFile.mkdir();
						}
						// NOW ADD MODEL FOLDER NAME
						archivePath = archivePath+"/"+ itemDetails.getModelFolderName();
						archFile = PathUtil.file(archivePath);
						if(!archFile.exists() || !archFile.isDirectory())
						{
							archFile.mkdir();
						}
						// NOW ADD MANUAL TYPE FOLDER
						archivePath = archivePath+"/"+ itemDetails.getManualType();
						archFile = PathUtil.file(archivePath);
						if(!archFile.exists() || !archFile.isDirectory())
						{
							archFile.mkdir();
						}
						ContentDetails con = new ContentDetails();
						con.setScheduleId(scheduleId);
						con.setThreadId(itemDetails.getThreadId());
						archiveDirectory(sourcePath, archivePath, con, "", "FULL", "", xcopyUtils);
						archivePath = null;
						con = null;
						
						// NOW ARCHIVE A HISTORY COPY
						File archHistoryFile = PathUtil.file(archiveLocationPathHistoryContent);
						if(!archHistoryFile.exists() || !archHistoryFile.isDirectory())
						{
							archHistoryFile.mkdir();
						}
						if(!archiveLocationPathHistoryContent.endsWith("/"))
						{
							archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/";
						}
						
						// ADD DATE FOLDER NAME
						archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+displayDate;
						File dateFol = PathUtil.file(archiveLocationPathHistoryContent);
						if(!dateFol.exists() || !dateFol.isDirectory())
						{
							dateFol.mkdir();
						}
						
						if(!archiveLocationPathHistoryContent.endsWith("/"))
						{
							archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/";
						}
						
						// ADD GMS3_Content folder Name
						archiveLocationPathHistoryContent=archiveLocationPathHistoryContent+gms3ContentFolderName;
						gms3Dir = PathUtil.file(archiveLocationPathHistoryContent);
						if(!gms3Dir.exists() || !gms3Dir.isDirectory())
						{
							gms3Dir.mkdir();
						}
						
						if(!archiveLocationPathHistoryContent.endsWith("/"))
						{
							archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/";
						}
						
						// NOW ADD MARKET
						archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+itemDetails.getMarket();
						archHistoryFile = PathUtil.file(archiveLocationPathHistoryContent);
						if(!archHistoryFile.exists() || !archHistoryFile.isDirectory())
						{
							archHistoryFile.mkdir();
						}
						// NOW ADD LOCALE
						archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/"+itemDetails.getLocale();
						archHistoryFile = PathUtil.file(archiveLocationPathHistoryContent);
						if(!archHistoryFile.exists() || !archHistoryFile.isDirectory())
						{
							archHistoryFile.mkdir();
						}
						// NOW ADD MODEL FOLDER NAME
						archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/"+ itemDetails.getModelFolderName();
						archHistoryFile = PathUtil.file(archiveLocationPathHistoryContent);
						if(!archHistoryFile.exists() || !archHistoryFile.isDirectory())
						{
							archHistoryFile.mkdir();
						}
						// NOW ADD MANUAL TYPE FOLDER
						archiveLocationPathHistoryContent = archiveLocationPathHistoryContent+"/"+ itemDetails.getManualType();
						archHistoryFile = PathUtil.file(archiveLocationPathHistoryContent);
						if(!archHistoryFile.exists() || !archHistoryFile.isDirectory())
						{
							archHistoryFile.mkdir();
						}
						con = new ContentDetails();
						con.setScheduleId(scheduleId);
						con.setThreadId(itemDetails.getThreadId());
						archiveDirectory(sourcePath, archiveLocationPathHistoryContent, con, displayDate, "HISTORY", itemDetails.getMaterialFolderName(), xcopyUtils);
						archiveLocationPathHistoryContent = null;
						con = null;
						
						/*
						 * Now, call function to archive Data to User Location which
						 * will be ServerPath + Archive Director
						 */

						if (!serverPath.endsWith("\\")) 
						{
							serverPath = serverPath + "\\";
						}
						
						/*
						 * call function to Delete Source Directory
						 */
						File sourceDirectory = PathUtil.file(sourcePath);
						if (sourceDirectory.exists() && sourceDirectory.isDirectory()) 
						{
							logger.info("startConversionProcess :: Calling Function to delete the Source Directory :: > " + PathUtil.winPath(sourceDirectory));
							deleteSourceDirectory(sourceDirectory);
							// now delete the sourceDirectory as well
							sourceDirectory.deleteOnExit();
						}
						sourceDirectory = null;
						itemDetails = null;
						sourcePath = null;
					}
				}
				gms3ContentFolderName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ArchiveUtils.class.getName(), "archiveRepository()", e);
		}
	}
	
	
	/**
	 * Function will Archive the Source Directory
	 * 
	 * @param sourcePath
	 */
	private void archiveDirectory(String sourcePath, String archivePath, ContentDetails contentDetails, String displayDate, String archiveMovementType, String materialFolderName, XcopyUtil xcopyUtils) 
	{
		try 
		{
			if (null != sourcePath && !"".equals(sourcePath)) 
			{
				logger.info("archiveDirectory :: Start Archiving Directory for :: > "+ sourcePath);
				// Check whether the directory exists at source Directory or not
				File sourceDirectory = PathUtil.file(sourcePath);
				if (sourceDirectory.exists() && sourceDirectory.isDirectory()) 
				{
					/*
					 * Here, the sourcePath will be some thing like
					 * D:/XMLConversion_WD
					 * /SourceContent/Content_Delta_20140101_achoragu, so to
					 * archive the directory the final Archive Path will -
					 * 
					 * D:/XMLConversion_WD/Archive/ (this will be fetched from
					 * application.properties PLUS Content_Delta_20140101 (THE
					 * VALUE OF THE DELTA / DELETE directory Name before last
					 * Index of _
					 */
					String sourceDirectoryName = sourceDirectory.getName();
					if (null != sourceDirectoryName && !"".equals(sourceDirectoryName)) 
					{
						
						String destinationDirectoryName = sourceDirectoryName;
						
						/*
						 * CHANGE DATE 25 JAN 2017 - DO NOT ADD DATE TO MATERIAL FOLDER NAME
						 */
//						if(null!=archiveMovementType && !"".equals(archiveMovementType))
//						{
//							if(archiveMovementType.equals("HISTORY"))
//							{
//								// ADD DISPLAY DATE IF DESTIONATION DIRECTORY NAME IS MATERIAL NAME
//								if(destinationDirectoryName.equals(materialFolderName))
//								{
//									destinationDirectoryName = destinationDirectoryName+"_"+displayDate;
//								}
//							}
//						}
						
						logger.info("archiveDirectory :: Destination Directory Name retrieved is  :: > "	+ destinationDirectoryName);
						if (null != destinationDirectoryName	&& !"".equals(destinationDirectoryName)) 
						{
							/*
							 * check for Archive Directory, if Exists then
							 * Ok, else create Archive Folder
							 */
							File aPath = PathUtil.file(archivePath);
							if (aPath.exists() && aPath.isDirectory()) 
							{
								logger.info("archiveDirectory :: Archive Directory Exists at Path :: >  "+ archivePath);
							} 
							else 
							{
								// create new directory.
								aPath.mkdir();
							}
							aPath = null;
							/*
							 * now check if the destinationDirectoryName
							 * does not exists then create it and add to
							 * Archive Path
							 */
							String finalArchivePath = xcopyUtils.createFolderStructure(destinationDirectoryName,	archivePath);
							if (null != finalArchivePath 	&& !"".equals(finalArchivePath)) 
							{
								logger.info("archiveDirectory ::  Final Archive Path :: > "	+ finalArchivePath);
								/*
								 * Now start for checking Child Files and
								 * Start Moving
								 */
								File[] childFilesList = sourceDirectory.listFiles();
								if (null != childFilesList	&& childFilesList.length > 0) 
								{
									for (int a = 0; a < childFilesList.length; a++) 
									{
										File childFile = childFilesList[a];
										processArchivingFiles(childFile,sourcePath,	finalArchivePath, contentDetails, xcopyUtils);
										childFile = null;
									}
								}
							} else {
								logger.info("archiveDirectory :: Failed to Create Destination Directory {"
										+ destinationDirectoryName
										+ "} at Archive Path :: >"
										+ archivePath);
							}
							archivePath = null;
							finalArchivePath = null;
						} else {
							logger.info("archiveDirectory :: Destination Directory Name is null. Exiting.");
						}
					
					} else {
						logger.info("archiveDirectory :: Source Directory Name is null. Exiting.");
					}
				} else {
					logger.info("archiveDirectory :: No directory Exists at Path {"
							+ sourcePath + "} . Exiting.");
				}
				sourceDirectory = null;
			} else {
				logger.info("archiveDirectory :: Source Path is passed as null in parameter.");
			}

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(ArchiveUtils.class.getName(), "archiveDirectory()", e);
		}
	}

	/**
	 * Function will Delete the Source Directories.
	 * 
	 * @param dir
	 * @return
	 */
	public boolean deleteSourceDirectory(File dir) 
	{
		try 
		{
			if (dir.isDirectory()) 
			{
				String[] children = dir.list();
				if (null != children && children.length > 0) 
				{
					for (int i = 0; i < children.length; i++) 
					{
						/*
						 * call recursive function
						 */
						deleteSourceDirectory(PathUtil.file(dir, children[i]));
					}
					children = null;
				}
			} 
			else 
			{
				dir.delete();
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(ArchiveUtils.class.getName(), "deleteSourceDirectory()", e);
		}
		return dir.delete();
	}


	
	/**
	 * Function will start Archiving The processed Files
	 * 
	 * @param childFile
	 * @param sourcePath
	 * @param archivePath
	 */
	private void processArchivingFiles(File childFile,String sourcePath, String archivePath, ContentDetails contentDetails, XcopyUtil xcopyUtils) 
	{
		try 
		{
			if (childFile.exists()) 
			{
				if (childFile.isDirectory()) 
				{
					/*
					 * CALL Recursive function
					 */
					File[] filesList = childFile.listFiles();
					if (null != filesList && filesList.length > 0) 
					{
						for (int a = 0; a < filesList.length; a++) 
						{
							File file = filesList[a];
							if (file.exists()) 
							{
								if (file.isDirectory()) 
								{
									processArchivingFiles(file, sourcePath,archivePath, contentDetails, xcopyUtils);
								}
								else if (file.isFile()) 
								{
									logger.info("processArchivingFiles :: XCOPYING File :: > "+ PathUtil.winPath(file));
									/*
									 * XCOPY File
									 */
									xcopyUtils.archiveFiles(file, sourcePath,	archivePath, contentDetails);
								}
							}
						}
					}
				} 
				else if (childFile.isFile()) 
				{
					logger.info("processArchivingFiles :: XCOPYING File :: > "+ PathUtil.winPath(childFile));
					/*
					 * XCOPY File
					 */
					xcopyUtils.archiveFiles(childFile, sourcePath, archivePath, contentDetails);
				}
			}

		} catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(ArchiveUtils.class.getName(), "processArchivingFiles()", e);
		}
	}

}
