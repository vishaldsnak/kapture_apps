package com.mazda.gms3.dmt.conversion.impl;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.mazda.gms3.dmt.kapture.KaptureArticle;
import com.mazda.gms3.dmt.kapture.KaptureContentService;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.kapture.KaptureRecordResult;
import com.mazda.gms3.dmt.mc.utils.KaptureArticleBuilder;
import com.mazda.gms3.dmt.mc.utils.WithdrawnDocumentsFinder;
import com.mazda.gms3.dmt.mme.dao.MMEDocumentBatchDAO;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.conversion.dao.EntParsing;
import com.mazda.gms3.dmt.conversion.utils.WiringDiagramUtils;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.dao.UserProfileDAO;
import com.mazda.gms3.dmt.dao.SettingsDAO;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.CDProcessingUtils;
import com.mazda.gms3.dmt.mc.utils.CategoryUtils;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.utils.PrintReportsUtil;
import com.mazda.gms3.dmt.mc.utils.XcopyUtil;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.MCViewContentDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.mc.vo.WindowJSDetails;
import com.mazda.gms3.dmt.mme.dao.MMEDocumentManagementDAO;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.DeleteFileDetails;
import com.mazda.gms3.dmt.vo.ErrorDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.SettingDetails;
import com.mazda.gms3.dmt.vo.UserGroupDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class StartMMEConversionImpl {

	private Logger logger = LogManager.getLogger(StartMMEConversionImpl.class);
	
	private String serverPath="";
	
	/** Kapture session of this job - the replacement for the InfoManager client. */
	private KaptureContentService kapture = null;
	/** Why there is no Kapture session, when there is none. */
	private String kaptureUnavailableReason = null;
	/** Kapture locale of the schedule item being processed, e.g. en_UK. */
	private String kaptureLocale = null;
	/** Offline preview of the documents this job writes (Preview page of the History screen). */
	private com.mazda.gms3.dmt.preview.PreviewBuilder preview = null;
	/** a row of the master data load or category report failed - the job is a FAILURE */
	private boolean masterDataFailed = false;

	private int authTokenGenerationCount=0;
	
	private int authCheckCount = 0;

	private ArrayList<ContentDetails> processingFileDetailsList = new ArrayList<ContentDetails>();
	
	private ArrayList<ContentDetails> processingDeleteFileDetailsList = new ArrayList<ContentDetails>();
	
	private ArrayList<ErrorDetails> imErrorDetailsList = new ArrayList<ErrorDetails>();
	
	private ArrayList<ErrorDetails> imErrorDeleteDetailsList = new ArrayList<ErrorDetails>();

	private ArrayList<CategoryDetails> missingCategoriesList = new ArrayList<CategoryDetails>();
	
	private ArrayList<ContentDetails> otherInnerLinksList = new ArrayList<ContentDetails>();

	private MMEDocumentManagementDAO mmeDocumentManagementDAO = null;
	
	private XcopyUtil xcopyUtils = null; 
	
	private PrintReportsUtil printReportsUtils=null;
	
	private boolean failureStatus = false;
	
	private ArrayList<ContentDetails> mateialFolderDocumentsListForInnerLinks = null;
	
	private ArrayList<ContentDetails> allInnerLinksList = null;
	
	private ArrayList<DisplayOrderDetails> finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
	
	private ArrayList<CDProcessingDetails> finalCDProcessingList = new ArrayList<CDProcessingDetails>();
	
	private ArrayList<VinDetails> finalSCMVINProcessingList = new ArrayList<VinDetails>();
	
	private WiringDiagramUtils wiringDiagramUtils=null;
	
	private ArrayList<VinMLMappingDetails> vinMLMappingList = new ArrayList<VinMLMappingDetails>();
	
	private ArrayList<UserGroupDetails> groupsListForContent= null;

	private CategoryDetails unpublishedCategoryDetails=null;
	
	private List<CategoryDetails> categoriesListForMaterialFolder=null;
	
	private ArrayList<ContentDetails> materialFolderContentList = null;
	
	private long totalDocsCount=0;
	
	private long dataPreparationCount=0;
	
	private long mfDataPrepSuccessCount=0;
	
	private long mfDataPrepfailureCount=0;
	
	public void startConversion(String scheduleId,String wslId)
	{
		logger.info("StartMMEConversionImpl :: Method Starts.");
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=wslId && !"".equals(wslId))
			{
				mmeDocumentManagementDAO = new MMEDocumentManagementDAO();
				xcopyUtils = new XcopyUtil();
				printReportsUtils = new PrintReportsUtil();
				allInnerLinksList = new ArrayList<ContentDetails>();
				otherInnerLinksList = new ArrayList<ContentDetails>();
				finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
				processingFileDetailsList = new ArrayList<ContentDetails>();
				processingDeleteFileDetailsList = new ArrayList<ContentDetails>();
				finalCDProcessingList = new ArrayList<CDProcessingDetails>();
				wiringDiagramUtils = new WiringDiagramUtils();
				vinMLMappingList = new ArrayList<VinMLMappingDetails>(); 
				finalSCMVINProcessingList = new ArrayList<VinDetails>();
				
				// SET WSL ID TO LOWERCASE
				wslId = wslId.toLowerCase();
				/*
				 * call function to set serverPath from the settings dao
				 */
				SettingDetails sDetails = SettingsDAO.getSettingsData(wslId);
				if(null!=sDetails && null!=sDetails.getNetworkPath() && !"".equals(sDetails.getNetworkPath()))
				{
					serverPath = com.mazda.gms3.dmt.utils.SourceContentPaths.contentRoot(sDetails.getNetworkPath());
				}
				sDetails = null;
//				serverPath=ApplicationProperties.getProperty("GMS3_SOURCE_CONTENT_PATH");
				if(null!=serverPath && !"".equals(serverPath))
				{
					serverPath = serverPath.replace("/", "\\");
				}
				if(!serverPath.endsWith("\\"))
				{
					serverPath = serverPath+"\\";
				}
				
				/*
				 * Update the Status of the Schedule Id to Processing
				 */
				// EVERYTHING THIS RUN DEPENDS ON, IN ONE PLACE OF THE LOG
				logger.info("startConversion :: JOB ENVIRONMENT :: schedule=" + scheduleId + " user=" + wslId + " os=" + System.getProperty("os.name")
						+ " java=" + System.getProperty("java.version") + " contentRoot=" + serverPath
						+ " okAssets=" + ApplicationProperties.getProperty("SERVER_OKASSETS_PHYSICAL_PATH") + " reports=" + ApplicationProperties.getProperty("REPORTS_DIRECTORY")
						+ " kaptureApi=" + ApplicationProperties.getProperty("kapture.api.base") + " masterDataWithContent=" + ScheduleDAO.isMasterDataWithContent(scheduleId));
				boolean updateProcessingStatus = ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.processing.value"));
				if(updateProcessingStatus==true)
				{
					preview = new com.mazda.gms3.dmt.preview.PreviewBuilder(scheduleId, true);
					masterDataFailed = false;
					/*
					 * call function to get Schedule Document Status
					 */
					String imProcessingStatus = ScheduleDAO.getScheduleIMProcessingStatus(scheduleId);
					/*
					 * Fetch all the Schedule Details on the basis of Code; e.g.
					 * Market, Locale and Models, Model Type, Material Folder Selected for Conversion
					 */
					ArrayList<ScheduleItemDetails> itemsList = ScheduleDAO.getScheduleItemDetails(scheduleId);
					if(null!=itemsList && itemsList.size()>0)
					{
						ScheduleItemDetails itemDetails = null;
						// get value at 0 index and identify Market
						itemDetails = (ScheduleItemDetails)itemsList.get(0);
						if(null!=itemDetails)
						{
							// OPEN THE KAPTURE SESSION FOR THIS JOB
							connectToKapture();
						}
						itemDetails = null;

						/*
						 * "MASTER DATA WITH CONTENT" - LOAD THE MASTER DATA EXCEL FILES INTO MDM AND SEND THEM TO
						 * KAPTURE AS CATEGORIES BEFORE ANY CONTENT IS PROCESSED. Its outcome is in its own two
						 * reports; the content is processed whatever the outcome.
						 */
						if(ScheduleDAO.isMasterDataWithContent(scheduleId))
						{
							java.util.Set<String> masterDataLocales = new java.util.LinkedHashSet<String>();
							for(int i=0;i<itemsList.size();i++)
							{
								masterDataLocales.add(((ScheduleItemDetails)itemsList.get(i)).getLocale());
							}
							// a failed row in either master data report fails the job, like the other reports
							masterDataFailed = com.mazda.gms3.dmt.masterdata.MasterDataLoad.run(scheduleId, serverPath, ((ScheduleItemDetails)itemsList.get(0)).getMarket(), masterDataLocales);
						}

						// identify totalProcessingDocsCount
						for(int i=0;i<itemsList.size();i++)
						{
							itemDetails = (ScheduleItemDetails)itemsList.get(i);
							totalDocsCount=totalDocsCount+itemDetails.getTotalDocsForProcessing();
							itemDetails=  null;
						}
						itemDetails=  null;
						/*
						 * Start Iterating List and Proceed for Updating Conversion
						 * Here, mandatory data - Market, Locale, Model, Model Type, Model Folder Name, Manual Type,
						 * Face Lift Folder Name, Material Folder Name, 
						 */
						String channelFolderName=null;
						ArrayList<VINEntFileDetails> masterVINList = null;
						ArrayList<String> applicableEngineMissonTypeList = null;
						ArrayList<VinDetails> allVINList = null;
						ArrayList<VinDetails> applicableVINListForMaterialFolder = null;
						ArrayList<VinDetails> scmVINList = null;
						ArrayList<VinDetails> applicableSCMVINListForMaterialFolder = null;
						
						ArrayList<DisplayOrderDetails> allDisplayOrderList = null;
						ArrayList<CDProcessingDetails> allCDProcessingList = null;
						
						for(int i=0;i<itemsList.size();i++)
						{
							itemDetails = (ScheduleItemDetails)itemsList.get(i);
							if(null!=itemDetails.getMarket() && !"".equals(itemDetails.getMarket())
									&& null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale())
									&& null!=itemDetails.getModel() && !"".equals(itemDetails.getModel())
									&& null!=itemDetails.getModelType() && !"".equals(itemDetails.getModelType())
									&& null!=itemDetails.getModelFolderName() && !"".equals(itemDetails.getModelFolderName())
									&& null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType())
									&& null!=itemDetails.getFaceLiftFolderName() && !"".equals(itemDetails.getFaceLiftFolderName())
									&& null!=itemDetails.getMaterialFolderName() && !"".equals(itemDetails.getMaterialFolderName()))
							{
								itemDetails.setMarket(itemDetails.getMarket().trim());
								itemDetails.setLocale(itemDetails.getLocale().trim());
								itemDetails.setModel(itemDetails.getModel().trim());
								itemDetails.setModelType(itemDetails.getModelType().trim());
								if(null!=itemDetails.getCarlineCode() && !"".equals(itemDetails.getCarlineCode()))
								{
									itemDetails.setCarlineCode(itemDetails.getCarlineCode().trim());
								}
								itemDetails.setModelFolderName(itemDetails.getModelFolderName().trim());
								itemDetails.setManualType(itemDetails.getManualType().trim());
								itemDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName().trim());
								itemDetails.setMaterialFolderName(itemDetails.getMaterialFolderName().trim());
								
								/*
								 * identify Channel FolderName & Process fetch Locale / Channel Details from InfoManager
								 * which is going to common to for all documents processing for this item
								 */
								kaptureLocale = ApplicationProperties.getProperty(itemDetails.getLocale().toLowerCase());
								channelFolderName = ConversionUtils.identifyChannelFolderName(itemDetails.getManualType(), itemDetails.getModelType());
								/*
								 * CHECK FOR THE DISPLAY ORDER FILE PARALLEL TO FACELIFT FOLDER
								 * NOT APPLICABLE NOW FOR ALL DISPLAY ORDER WILL BE INSIDE FACELIFT FOLDER ONLY
								 * DATE = 13 MAY 2017
								 */
								masterVINList = new ArrayList<VINEntFileDetails>();
								applicableEngineMissonTypeList = new ArrayList<String>();
								/*
								 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
								 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
								 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
								 */
								if(null!=itemDetails.getModel() && (itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
								{
									/*
									 * fetch Master VINList
									 * NO NEED OF SENDING MODEL TYPE WHILE IDENTIFYING MASTER VIN.
									 */
									masterVINList = mmeDocumentManagementDAO.getMasterVinDetailsList(itemDetails.getLocale(), itemDetails.getManualType(), itemDetails.getModel(), null,"Y");
									/*
									 * fetch applicable Engine / Mission Type List
									 */
									applicableEngineMissonTypeList = mmeDocumentManagementDAO.getEngineMissionTypeList(itemDetails.getLocale(), itemDetails.getManualType(), itemDetails.getModel());
								}
								
								/*
								 * Fetch Market, Locale , ModelFolderName and Manual Type + FaceLiftFolderName + 
								 * MATERIAL FOLDER NAME
								 * from the Item Details
								 * check for each whether the directory exists or not
								 */
								String materialDirPath=serverPath+ itemDetails.getMarket()+"\\"+itemDetails.getLocale()+"\\"+
								 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType() +"\\"+ 
										itemDetails.getFaceLiftFolderName() +"\\"+ itemDetails.getMaterialFolderName();
								
								File materialDir = PathUtil.file(materialDirPath);
								if(materialDir.exists() && materialDir.isDirectory())
								{
									allVINList = new ArrayList<VinDetails>();
									applicableVINListForMaterialFolder = new ArrayList<VinDetails>();
									
									allDisplayOrderList = new ArrayList<DisplayOrderDetails>();
									allCDProcessingList = new ArrayList<CDProcessingDetails>();
									
									File faceLiftFolder = materialDir.getParentFile();
									if(faceLiftFolder.exists() && faceLiftFolder.isDirectory())
									{
										File[] fcChildsList = faceLiftFolder.listFiles();
										if(null!=fcChildsList && fcChildsList.length>0)
										{
											/*
											 * identify All the VIN & DISPLAY ORDER FILES PLACED HERE.
											 * FOR VIN IDENTIFY THE VINS APPLICABLE FOR THE PRCESSING MATERIAL FOLDER AND ADD IT WITH EACH 
											 * DOCUMENT.
											 */
											for(int m=0;m<fcChildsList.length;m++)
											{
												File chF = (File)fcChildsList[m];
												if(chF.exists() && chF.isFile())
												{
													if(chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.vin.ent")) && 
															!chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.scm.vin.ent")) &&  
															chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
													{
														// VIN TEXT FILE FOUND.
														ArrayList<VinDetails> tempList = ConversionUtils.readVINTextFileForMME(chF,"Y");
														if(null!=tempList && tempList.size()>0)
														{
															allVINList.addAll(tempList);
														}
														tempList= null;
													}
													// SCM VIN TEXT FILE FOUND
													else if(chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.scm.vin.ent")) && 
															chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
													{
														/*
														 * only for WM Channel
														 */
														ArrayList<VinDetails> tempList = new ArrayList<VinDetails>();
														if(null!=itemDetails.getManualType() && itemDetails.getManualType().trim().toLowerCase().equals("wm"))
														{
															// MME Market
															tempList = ConversionUtils.readSCMVINTextFileForMME(chF,"Y"); 
														}
														if(null!=tempList && tempList.size()>0)
														{
															if(null==scmVINList || scmVINList.size()<=0)
															{
																scmVINList = new ArrayList<VinDetails>();
															}
															scmVINList.addAll(tempList);
														}
														tempList= null;
													}
													else if(chF.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.displayorder")) 
															&& !chF.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("file.name.delete")) &&  
															chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
													{
														// DISPLAY ORDER TEXT FILE FOUND.
														
														/*
														 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
														 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
														 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
														 */
														if(null!=itemDetails.getModel() && (itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
														{
															ArrayList<DisplayOrderDetails> tempList = ConversionUtils.readDisplayOrderTextFileForEngineATMT(chF, itemDetails.getModelType(),"Y");
															if(null!=tempList && tempList.size()>0)
															{
																allDisplayOrderList.addAll(tempList);
															}
															tempList= null;
														}
														else
														{
															ArrayList<DisplayOrderDetails> tempList = ConversionUtils.readDisplayOrderTextFile(chF, itemDetails,"Y");
															if(null!=tempList && tempList.size()>0)
															{
																allDisplayOrderList.addAll(tempList);
															}
															tempList= null;
														}
													}
													else if(chF.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.cdprocessing")) && 
															chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
													{
														// CD PROCESSING TEXT FILE FOUND.
														
														/*
														 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
														 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
														 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
														 */
														if(null!=itemDetails.getModel() && (itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
																itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
														{
															ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFileForEngineATMT(chF, itemDetails.getModelType(),"Y");
															if(null!=tempList && tempList.size()>0)
															{
																allCDProcessingList.addAll(tempList);
															}
															tempList= null;
														}
														else
														{
															ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFile(chF, itemDetails.getModelType(),"Y");
															if(null!=tempList && tempList.size()>0)
															{
																allCDProcessingList.addAll(tempList);
															}
															tempList= null;
														}
													}
												}
												chF = null;
											}
											fcChildsList = null;
										}
									}
									faceLiftFolder=null;
									
									if(null!=allVINList && allVINList.size()>0)
									{
										/*
										 * IDENTIFY THE VINS APPLICABLE FOR CURRENT PROCESSING MATERIAL FOLDER
										 */
										for(int y=0;y<allVINList.size();y++)
										{
											VinDetails details = (VinDetails)allVINList.get(y);
											// ONLY FOR VALID LINES
											if(null!=details.getLineType() && details.getLineType().equals(ConversionUtils.LINE_TYPE_VALID))
											{
												if(itemDetails.getManualType().trim().toLowerCase().equals(details.getManualType().trim().toLowerCase()) 
														&& itemDetails.getFaceLiftFolderName().trim().toLowerCase().equals(details.getFaceLiftFolder().trim().toLowerCase())
														&& itemDetails.getMaterialFolderName().trim().toLowerCase().equals(details.getMaterialFolder().trim().toLowerCase()))
												{
													if(null==applicableVINListForMaterialFolder || applicableVINListForMaterialFolder.size()<=0)
													{
														applicableVINListForMaterialFolder = new ArrayList<VinDetails>();
													}
													// add the VINS to CURRENT PROCESSING LIST
													applicableVINListForMaterialFolder.add(details);
												}
											}
											details= null;
										}
									}
									allVINList=  null;
									
									if(null!=scmVINList && scmVINList.size()>0)
									{
										/*
										 * IDENTIFY THE SCM VINS APPLICABLE FOR CURRENT PROCESSING MATERIAL FOLDER
										 */
										for(int y=0;y<scmVINList.size();y++)
										{
											VinDetails details = (VinDetails)scmVINList.get(y);
											// ONLY FOR VALID LINES
											if(null!=details.getLineType() && details.getLineType().equals(ConversionUtils.LINE_TYPE_VALID))
											{
												if(itemDetails.getManualType().trim().toLowerCase().equals(details.getManualType().trim().toLowerCase()) 
														&& itemDetails.getFaceLiftFolderName().trim().toLowerCase().equals(details.getFaceLiftFolder().trim().toLowerCase())
														&& itemDetails.getMaterialFolderName().trim().toLowerCase().equals(details.getMaterialFolder().trim().toLowerCase()))
												{
													if(null==applicableSCMVINListForMaterialFolder || applicableSCMVINListForMaterialFolder.size()<=0)
													{
														applicableSCMVINListForMaterialFolder = new ArrayList<VinDetails>();
													}
													// add the SCM VINS to CURRENT PROCESSING LIST
													applicableSCMVINListForMaterialFolder.add(details);
												}
											}
											details= null;
										}
									}
									scmVINList=  null;
									
									/*
									 * INSERT DATA FOR ALL DISPLAY ORDER IN TEMP TABLE
									 */
									if(null!=allDisplayOrderList && allDisplayOrderList.size()>0)
									{
										logger.info("startConversion :: Start Saving All Display Order in TEMP TABLE.");
										ScheduleDAO.saveDisplayOrderDetailsTemp(allDisplayOrderList, scheduleId);
										// the same rows, indexed by document path - read per document instead of a SELECT each
										displayOrderIndex = indexDisplayOrder(allDisplayOrderList);
										logger.info("startConversion :: END Saving All Display Order in TEMP TABLE.");
									}
									allDisplayOrderList = null;
									// set mateialFolderDocumentsListForInnerLinks to new ArrayList
									mateialFolderDocumentsListForInnerLinks = new ArrayList<ContentDetails>();
									/*
									 * FIRST DELETE
									 * FIRST PROCESS ALL THE CONTENT WITH ESI CATEGORY DATA WITH VIN DATA & DISPLAY ORDER DATA FOR THE MATERIAL FOLDER
									 * THEN PROCESS INNER LINKS FOR THE MATERIAL FOLDER
									 * THEN PROCESS OKASSETS FOR THE MATERIAL FOLDER
									 */
									
									logger.info("startConversion :: Start DELETING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: DELETE PROCESSING STARTED AT :: >"+ new Date());
									
									// DELETE PROCESSING
									startDeleteProcessing(materialDir, itemDetails, wslId,channelFolderName);
									
									logger.info("startConversion :: DELETE PROCESSING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start DATA PREPARATION for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: DATA PREPARATION STARTED AT :: >"+ new Date());
									
									// DATA PREPARATION
									startDataPreparation(materialDir, itemDetails, imProcessingStatus, wslId, applicableVINListForMaterialFolder, allDisplayOrderList, masterVINList, applicableEngineMissonTypeList,channelFolderName);
									applicableEngineMissonTypeList = null; 
									logger.info("startConversion :: DATA PREPARATION ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: CATEGORY PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: CATEGORY PROCESSING STARTED AT :: >"+ new Date());
									
									// CATEGORY PROCESSING
									startCategoryProcessing(itemDetails, wslId);
									
									logger.info("startConversion :: CATEGORY PROCESSING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start DOCUMENT PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: DOCUMENT PROCESSING STARTED AT :: >"+ new Date());
									
									// DOCUMENT PROCESSING
									startDocumentProcessing(itemDetails);
									
									logger.info("startConversion :: DOCUMENT PROCESSING ENDED AT :: >"+ new Date());

									// SET THESE VARIABLES TO NULL AS THEY ARE NOT USED FURTHER ANYMORE. RELEASE FROM MEMORY
									materialFolderContentList = null;
									categoriesListForMaterialFolder = null;
									mfDataPrepSuccessCount = 0;
									mfDataPrepfailureCount = 0;
									
									
									logger.info("startConversion :: Start INNERLINKS PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: INNERLINKS PROCESSING STARTED AT :: >"+ new Date());

									// INNERLINKS PROCESSING
									startProcessingInnerLinks(scheduleId, itemDetails);
									// immediately set innerLinks list to null as Inner Link Processing done - release memory
									mateialFolderDocumentsListForInnerLinks = null;
									
									logger.info("startConversion :: INNERLINKS PROCESSING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start OKASSETS PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: OKASSETS PROCESSING STARTED AT :: >"+ new Date());

									// OKASSETS PROCESSING - the processed / failed counts are written in bulk meanwhile
									com.mazda.gms3.dmt.dao.OkAssetsCountBuffer.begin(String.valueOf(itemDetails.getScheduleId()));
									try
									{
										startProcessingOKAssets(materialDir, itemDetails, wslId, channelFolderName);
									}
									finally
									{
										com.mazda.gms3.dmt.dao.OkAssetsCountBuffer.end(String.valueOf(itemDetails.getScheduleId()));
									}

									logger.info("startConversion :: OKASSETS PROCESSING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start DISPLAY OPDER PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: DISPLAY ORDER PROCESSING STARTED AT :: >"+ new Date());

									// DISPLAY ORDER PROCESSING
									startProcessingDisplayOrder(materialDir, itemDetails, wslId, allDisplayOrderList, applicableVINListForMaterialFolder, channelFolderName);
									// SET IT TO NULL - RELEASE FROM MEMORY
									allDisplayOrderList = null;
									
									logger.info("startConversion :: DISPLAY ORDER PROCESSING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start CD DATA PROCESSING for Material Directory at Path :: > " + materialDirPath);
									logger.info("startConversion :: CD PROCESSING STARTED AT :: >"+ new Date());
									
									// CD DATA PROCESSING
									startCDProcessing(materialDir, itemDetails, wslId, allCDProcessingList, channelFolderName);
									// SET IT TO NULL - RELEASE FROM MEMORY
									allCDProcessingList = null;
									logger.info("startConversion :: CD PROCESSING ENDED AT :: >"+ new Date());
																		

									logger.info("startConversion :: Start SCM VIN MAPPING FOR Material Directory at Path :: > "+ materialDirPath);
									logger.info("startConversion :: SCM VIN MAPPING STARTED AT :: >"+ new Date());
									
									// SCM VIN PROCESSING
									startSCMVINProcessing(materialDir, itemDetails, wslId, applicableSCMVINListForMaterialFolder);
									
									logger.info("startConversion :: SCM VIN MAPPING ENDED AT :: >"+ new Date());
									
									logger.info("startConversion :: Start VIN - MANUAL TYPE MAPPING FOR Material Directory at Path :: > "+ materialDirPath);
									logger.info("startConversion :: VIN MANUAL MAPPING STARTED AT :: >"+ new Date());
									// VIN MANUAL TYPE MAPPING
									startVINManualTypeMapping(applicableVINListForMaterialFolder, masterVINList, itemDetails);
									
									logger.info("startConversion :: VIN MANUAL MAPPING ENDED AT :: >"+ new Date());
									/*
									 * HERE, UPDATE THE CURRENT PROCESSING STATUS FOR THE MATERIAL FOLDER TO COMPLETE
									 */
									ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(itemDetails.getItemId()), "COMPLETED");
									
									applicableVINListForMaterialFolder = null;
									applicableSCMVINListForMaterialFolder = null;
									allDisplayOrderList= null;
								}
								else
								{
									logger.info("startConversion :: Material Directory Does not Exists at Path :: > " + materialDirPath);
								}
								materialDir = null;
								materialDirPath = null;
								channelFolderName = null;
								masterVINList = null;
								/*
								 * delete display order data from temp table
								 */
								ScheduleDAO.deleteDisplayOrderTemp();
								displayOrderIndex = null;
							}
							else if(null!=itemDetails.getMarket() && !"".equals(itemDetails.getMarket())
									&& null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale())
									&& null!=itemDetails.getModel() && !"".equals(itemDetails.getModel())
									&& null!=itemDetails.getModelType() && !"".equals(itemDetails.getModelType())
									&& null!=itemDetails.getModelFolderName() && !"".equals(itemDetails.getModelFolderName())
									&& null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType())
									&& null!=itemDetails.getFaceLiftFolderName() && !"".equals(itemDetails.getFaceLiftFolderName()))
							{
								itemDetails.setMarket(itemDetails.getMarket().trim());
								itemDetails.setLocale(itemDetails.getLocale().trim());
								itemDetails.setModel(itemDetails.getModel().trim());
								itemDetails.setModelType(itemDetails.getModelType().trim());
								if(null!=itemDetails.getCarlineCode() && !"".equals(itemDetails.getCarlineCode()))
								{
									itemDetails.setCarlineCode(itemDetails.getCarlineCode().trim());
								}
								itemDetails.setModelFolderName(itemDetails.getModelFolderName().trim());
								itemDetails.setManualType(itemDetails.getManualType().trim());
								itemDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName().trim());
								/*
								 * identify Channel FolderName & Process fetch Locale / Channel Details from InfoManager
								 * which is going to common to for all documents processing for this item
								 */
								kaptureLocale = ApplicationProperties.getProperty(itemDetails.getLocale().toLowerCase());
								channelFolderName = ConversionUtils.identifyChannelFolderName(itemDetails.getManualType(), itemDetails.getModelType());
								/*
								 * Fetch Market, Locale , ModelFolderName and Manual Type + FaceLiftFolderName  
								 * from the Item Details
								 * check for each whether the directory exists or not
								 */
								String faceLiftDirPath=serverPath+ itemDetails.getMarket()+"\\"+itemDetails.getLocale()+"\\"+
								 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType() +"\\"+ 
										itemDetails.getFaceLiftFolderName();
								
								File faceLiftDir = PathUtil.file(faceLiftDirPath);
								if(faceLiftDir.exists() && faceLiftDir.isDirectory())
								{
									allDisplayOrderList = new ArrayList<DisplayOrderDetails>();
									allCDProcessingList = new ArrayList<CDProcessingDetails>();
									allVINList = new ArrayList<VinDetails>();
									scmVINList = new ArrayList<VinDetails>();
									
									File[] fcChildsList = faceLiftDir.listFiles();
									if(null!=fcChildsList && fcChildsList.length>0)
									{
										/*
										 * identify All the VIN & DISPLAY ORDER FILES PLACED HERE.
										 * FOR VIN IDENTIFY THE VINS APPLICABLE FOR THE PRCESSING MATERIAL FOLDER AND ADD IT WITH EACH 
										 * DOCUMENT.
										 */
										for(int m=0;m<fcChildsList.length;m++)
										{
											File chF = (File)fcChildsList[m];
											if(chF.exists() && chF.isFile())
											{
												if(chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.vin.ent")) && 
														!chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.scm.vin.ent")) && 
														chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
												{
													// VIN TEXT FILE FOUND.
													ArrayList<VinDetails> tempList = ConversionUtils.readVINTextFileForMME(chF,"Y");
													if(null!=tempList && tempList.size()>0)
													{
														allVINList.addAll(tempList);
													}
													tempList= null;
												}
												// SCM VIN TEXT FILE FOUND
												else if(chF.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.scm.vin.ent")) && 
														chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
												{
													/*
													 * only for WM Channel
													 */
													ArrayList<VinDetails> tempList = new ArrayList<VinDetails>();
													if(null!=itemDetails.getManualType() && itemDetails.getManualType().trim().toLowerCase().equals("wm"))
													{
														// MME Market
														tempList = ConversionUtils.readSCMVINTextFileForMME(chF,"Y"); 
													}
													if(null!=tempList && tempList.size()>0)
													{
														if(null==scmVINList || scmVINList.size()<=0)
														{
															scmVINList = new ArrayList<VinDetails>();
														}
														scmVINList.addAll(tempList);
													}
													tempList= null;
												}
												else if(chF.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.displayorder")) 
														&& !chF.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("file.name.delete")) &&  
														chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
												{
													// DISPLAY ORDER TEXT FILE FOUND.
													
													/*
													 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
													 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
													 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
													 */
													if(null!=itemDetails.getModel() && (itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
													{
														ArrayList<DisplayOrderDetails> tempList = ConversionUtils.readDisplayOrderTextFileForEngineATMT(chF, itemDetails.getModelType(),"Y");
														if(null!=tempList && tempList.size()>0)
														{
															allDisplayOrderList.addAll(tempList);
														}
														tempList= null;
													}
													else
													{
														ArrayList<DisplayOrderDetails> tempList = ConversionUtils.readDisplayOrderTextFile(chF, itemDetails,"Y");
														if(null!=tempList && tempList.size()>0)
														{
															allDisplayOrderList.addAll(tempList);
														}
														tempList= null;
													}
												}
												else if(chF.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.cdprocessing")) && 
														chF.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
												{
													// CD PROCESSING TEXT FILE FOUND.
													
													/*
													 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
													 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
													 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
													 */
													if(null!=itemDetails.getModel() && (itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
															itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
													{
														ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFileForEngineATMT(chF, itemDetails.getModelType(),"Y");
														if(null!=tempList && tempList.size()>0)
														{
															allCDProcessingList.addAll(tempList);
														}
														tempList= null;
													}
													else
													{
														ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFile(chF, itemDetails.getModelType(),"Y");
														if(null!=tempList && tempList.size()>0)
														{
															allCDProcessingList.addAll(tempList);
														}
														tempList= null;
													}
												}
											}
											chF = null;
										}
										fcChildsList = null;
									}
									
									/*
									 * PROCESS DISPLAY ORDER FOR THE FACE LIFT FOLDER
									 * PROCESS CD PROCESSING DATA FOR THE FACE LIFT FOLDER
									 */
									
									// DISPLAY ORDER PROCESSING
									logger.info("startConversion :: Start DISPLAY OPDER PROCESSING for FaceLift Directory at Path :: > " + faceLiftDirPath);
									logger.info("startConversion :: DISPLAY ORDER PROCESSING STARTED AT :: >"+ new Date());
									
									startProcessingDisplayOrderForFaceLift(faceLiftDir, itemDetails, wslId, allDisplayOrderList, allVINList, channelFolderName);
									allDisplayOrderList= null;
									allVINList = null;
									logger.info("startConversion :: DISPLAY ORDER PROCESSING ENDED AT :: >"+ new Date());
									
									// CD DATA PROCESSING
									logger.info("startConversion :: Start CD DATA PROCESSING for FaceLift Directory at Path :: > " + faceLiftDirPath);
									logger.info("startConversion :: CD PROCESSING STARTED AT :: >"+ new Date());
									
									startCDProcessingForFaceLift(faceLiftDir, itemDetails, wslId, allCDProcessingList, channelFolderName);
									allCDProcessingList = null;
									logger.info("startConversion :: CD PROCESSING ENDED AT :: >"+ new Date());
									
									// SCM VIN DATA PROCESSING
									logger.info("startConversion :: Start SCM VIN MAPPING DATA PROCESSING for FaceLift Directory at Path :: > " + faceLiftDirPath);
									logger.info("startConversion :: SCM VIN MAPPING DATA PROCESSING STARTED AT :: >"+ new Date());
									
									startSCMVINProcessingForFaceLift(faceLiftDir, itemDetails, wslId, scmVINList);
									scmVINList = null;
									logger.info("startConversion :: SCM VIN MAPPING DATA PROCESSING ENDED AT :: >"+ new Date());
									
									/*
									 * HERE, UPDATE THE CURRENT PROCESSING STATUS FOR THE MATERIAL FOLDER TO COMPLETE
									 */
									ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(itemDetails.getItemId()), "COMPLETED");
								}
								else
								{
									logger.info("startConversion :: FaceLift Directory Does not Exists at Path :: > " + faceLiftDirPath);
								}
								faceLiftDir = null;
								faceLiftDirPath = null;
								channelFolderName = null;
							}
							itemDetails = null;
						}

						/*
						 * OFFLINE PREVIEW: tree, filters and the job's rows - from what the job holds in memory
						 */
						preview.finish(mmeDocumentManagementDAO.getViewContentDataList());
						
						/**
						 * PROCEED FOR PRINTING REPORTS.
						 */
						printReports(scheduleId);
						
						if(failureStatus==true)
						{
							logger.info("###############################################################################");
							logger.info("startConversion :: Errors found in the conversion, Updating Status as Failure. Exit Conversion.");
							logger.info("###############################################################################");
							
							/*
							 * IDENTIFY WHAT ALL ITEMS FAILED AND UPDATE STATUS FOR EACH ACCORDINGLY
							 */
							if(null!=itemsList && itemsList.size()>0)
							{
								itemDetails=null;
								String pathToCheck="";
								String status="";
								for(int u=0;u<itemsList.size();u++)
								{
									itemDetails = (ScheduleItemDetails)itemsList.get(u);
									if(null!=itemDetails.getMarket() && !"".equals(itemDetails.getMarket())
											&& null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale())
											&& null!=itemDetails.getModel() && !"".equals(itemDetails.getModel())
											&& null!=itemDetails.getModelType() && !"".equals(itemDetails.getModelType())
											&& null!=itemDetails.getModelFolderName() && !"".equals(itemDetails.getModelFolderName())
											&& null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType())
											&& null!=itemDetails.getFaceLiftFolderName() && !"".equals(itemDetails.getFaceLiftFolderName())
											&& null!=itemDetails.getMaterialFolderName() && !"".equals(itemDetails.getMaterialFolderName()))
									{
										itemDetails.setMarket(itemDetails.getMarket().trim());
										itemDetails.setLocale(itemDetails.getLocale().trim());
										itemDetails.setModel(itemDetails.getModel().trim());
										itemDetails.setModelType(itemDetails.getModelType().trim());
										if(null!=itemDetails.getCarlineCode() && !"".equals(itemDetails.getCarlineCode()))
										{
											itemDetails.setCarlineCode(itemDetails.getCarlineCode().trim());
										}
										itemDetails.setModelFolderName(itemDetails.getModelFolderName().trim());
										itemDetails.setManualType(itemDetails.getManualType().trim());
										itemDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName().trim());
										itemDetails.setMaterialFolderName(itemDetails.getMaterialFolderName().trim());
										
										pathToCheck = itemDetails.getLocale()+"\\"+
												 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType() +"\\"+ 
													itemDetails.getFaceLiftFolderName() +"\\"+ itemDetails.getMaterialFolderName();
										status = identifyStatusForMaterialFolder(pathToCheck);
										
										if(null!=status && !"".equals(status))
										{
											// update shceduleItemStatus
											ScheduleDAO.updateScheduleItemStatus(String.valueOf(itemDetails.getItemId()), status);
										}
										status = null;
										pathToCheck = null;
									}
									else if(null!=itemDetails.getMarket() && !"".equals(itemDetails.getMarket())
											&& null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale())
											&& null!=itemDetails.getModel() && !"".equals(itemDetails.getModel())
											&& null!=itemDetails.getModelType() && !"".equals(itemDetails.getModelType())
											&& null!=itemDetails.getModelFolderName() && !"".equals(itemDetails.getModelFolderName())
											&& null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType())
											&& null!=itemDetails.getFaceLiftFolderName() && !"".equals(itemDetails.getFaceLiftFolderName()))
									{
										itemDetails.setMarket(itemDetails.getMarket().trim());
										itemDetails.setLocale(itemDetails.getLocale().trim());
										itemDetails.setModel(itemDetails.getModel().trim());
										itemDetails.setModelType(itemDetails.getModelType().trim());
										if(null!=itemDetails.getCarlineCode() && !"".equals(itemDetails.getCarlineCode()))
										{
											itemDetails.setCarlineCode(itemDetails.getCarlineCode().trim());
										}
										itemDetails.setModelFolderName(itemDetails.getModelFolderName().trim());
										itemDetails.setManualType(itemDetails.getManualType().trim());
										itemDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName().trim());
										
										pathToCheck = itemDetails.getLocale()+"\\"+
												 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType() +"\\"+ 
													itemDetails.getFaceLiftFolderName();
										status = identifyStatusForMaterialFolder(pathToCheck);
										
										if(null!=status && !"".equals(status))
										{
											// update shceduleItemStatus
											ScheduleDAO.updateScheduleItemStatus(String.valueOf(itemDetails.getItemId()), status);
										}
										status = null;
										pathToCheck = null;
									}
									itemDetails = null;
								}
							}
							
							/*
							 * Update Status to Failure
							 */
							ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
						}
						else
						{
							logger.info("###############################################################################");
							logger.info("startConversion :: No errors found in the conversion, Updating Status as Success. Exit Conversion.");
							logger.info("###############################################################################");
							/*
							 * Update the Status as Success for the Schedule
							 */
							ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.success.value"));
						}
					}
					else
					{
						logger.info("###############################################################################");
						logger.info("startConversion :: No Items Found, Updating Status as Success . Exit Conversion.");
						logger.info("###############################################################################");
						/*
						 * Update the Status as Success for the Schedule
						 */
						ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.success.value"));
					}
					itemsList = null;
				}
				else
				{
					logger.info("###############################################################################");
					logger.info("startConversion :: Some issue in Updating Processing Status . Exit Conversion.");
					logger.info("###############################################################################");
					/*
					 * Update Status to Failure
					 */
					ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
					ScheduleDAO.updateScheduleAllItemStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
				}
				
				/*
				 * FETCH USER PROFILE ON THE BASIS OF WSL ID
				 * TO GET EMAIL ID
				 */
				String emailUserId=null;
				try
				{
					emailUserId = UserProfileDAO.getUserEmail(wslId);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startConversion()", e);
				}
				
				logger.info("startConversion :: Email Id read from Kapture for WSL ID >> " + wslId+" >> is >> "+ emailUserId);
				
				
				/*
				 * SEND A NOTIFICATION EMAIL
				 */
				NotificationEmailHelper.generateNotificationEmail(scheduleId, emailUserId);
				
				emailUserId  =null;
			}
			else
			{
				logger.info("####################################################################");
				logger.info("startConversion :: Schedule Id is null as parameter. Exit Conversion.");
				logger.info("####################################################################");
				/*
				 * Update Status to Failure
				 */
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
				ScheduleDAO.updateScheduleAllItemStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startConversion()", e);
			/*
			 * Update Status to Failure
			 */
			try {
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
				ScheduleDAO.updateScheduleAllItemStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
			} catch (Exception e1) {
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startConversion()", e1);
			}
		}
		finally
		{
			logger.info(" ******************** StartMMEConversionImpl :: FOR SCHEDULE ID {"+scheduleId+"}. GENERATED AUTH TOKEN COUNTS ARE :: > "+ authTokenGenerationCount);
			logger.info(" ******************** StartMMEConversionImpl :: FOR SCHEDULE ID {"+scheduleId+"}. TOTAL OPERATION COUNTS ARE :: > "+ authCheckCount);

			// set all Used Lists to null
			processingFileDetailsList = null;
			missingCategoriesList= null;
			imErrorDetailsList= null;
			allInnerLinksList  =null;
			mateialFolderDocumentsListForInnerLinks=  null;
			mmeDocumentManagementDAO.setFailedDatabaseSaveDocumentDetails(null);
			xcopyUtils.setFileProcessingList(null);
			xcopyUtils.setFileUploadFailureList(null);
			wiringDiagramUtils.setWindowJSList(null);
			wiringDiagramUtils = null;
			
			kapture = null;
			preview = null;
			authTokenGenerationCount=0;
			authCheckCount=0;
			mmeDocumentManagementDAO=  null;
			xcopyUtils  =null;
			printReportsUtils= null;
			failureStatus = false;
			finalCDProcessingList = null;
			finalDisplayOrderList = null;
			vinMLMappingList = null;
			otherInnerLinksList = null;
			
			groupsListForContent = null;
			categoriesListForMaterialFolder = null;
			materialFolderContentList = null;
			unpublishedCategoryDetails = null;
			totalDocsCount=  0;
			dataPreparationCount=0;
			mfDataPrepSuccessCount=0;
			mfDataPrepfailureCount=0;
			// master data report rows of a job that stopped before its reports were written
			com.mazda.gms3.dmt.masterdata.MasterDataLoad.discardReports(scheduleId);
		}
		
		// call function to delete from temp display ordet table safe side
		ScheduleDAO.deleteDisplayOrderTemp();
		
		// call stopCurrentThread 
		stopActiveThead(scheduleId);
	}
	
	/**
	 * DELETE PROCESSING - DRIVEN BY THE ESI CATEGORY TEXT FILE (same as the MC market).
	 *
	 * There is no delete text file any more. The ESI category text file of the material folder
	 * lists every document the folder holds now; a document the database still has as ACTIVE
	 * for this folder and that the file no longer lists has been withdrawn, and is deleted
	 * (unpublished in Kapture, see deleteContent).
	 *
	 * NOTHING IS DELETED when the folder has no ESI category text file, or when the file has
	 * no valid row - an empty or unreadable file must never unpublish a whole folder.
	 */
	private void startDeleteProcessing(File materialDirFolder, ScheduleItemDetails itemDetails, String wslId, String channelFolderName)
	{
		try
		{
			// LOCATE THE ESI CATEGORY TEXT FILE OF THE MATERIAL FOLDER
			File esiCategoryFile = null;
			File[] childModelFilesList = materialDirFolder.listFiles();
			if(null!=childModelFilesList && childModelFilesList.length>0)
			{
				for(int k=0;k<childModelFilesList.length;k++)
				{
					File chModelFile = childModelFilesList[k];
					if(chModelFile.exists() && chModelFile.isFile()
							&& chModelFile.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.name.esicat"))
							&& chModelFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
					{
						esiCategoryFile = chModelFile;
						break;
					}
				}
			}
			childModelFilesList = null;
			if(null==esiCategoryFile)
			{
				logger.info("startDeleteProcessing :: No ESI CATEGORY text file in Material Folder {"+itemDetails.getMaterialFolderName()+"}. Delete Processing skipped.");
				return;
			}

			// THE ESI CATEGORY TEXT FILE IS READ AS DATA PREPARATION READS IT (WD: STEERING TYPES AFTER THE TITLE)
			ArrayList<ESICategoryDetails> esiCategoryDetailsList = null;
			if(null!=channelFolderName && channelFolderName.trim().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim()))
			{
				esiCategoryDetailsList = ConversionUtils.readESICategoryTextFileForMME_WD(esiCategoryFile);
			}
			else
			{
				esiCategoryDetailsList = ConversionUtils.readESICategoryTextFile(esiCategoryFile);
			}

			// THE DOCUMENTS THE DATABASE STILL HAS AS ACTIVE FOR THIS FOLDER AND THE FILE NO LONGER LISTS
			List<String[]> withdrawnDocuments = WithdrawnDocumentsFinder.find(esiCategoryDetailsList, itemDetails.getLocale(), itemDetails.getModelFolderName(),
					itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), itemDetails.getMaterialFolderName(), itemDetails.getModelType(), true);
			esiCategoryDetailsList = null;

			boolean statusUpdated = false;
			for(int a=0;a<withdrawnDocuments.size();a++)
			{
				String[] document = (String[])withdrawnDocuments.get(a);
				if(statusUpdated==false)
				{
					/*
					 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DOCUMENT DELETION
					 */
					ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "DOCUMENT DELETION");
					statusUpdated = true;
				}

				logger.info("startDeleteProcessing :: Document {"+document[0]+"} is no longer listed in the ESI CATEGORY text file. Processing Delete Operation for :: > " + document[1]);
				DeleteFileDetails deleteDetails = new DeleteFileDetails();
				deleteDetails.setFileType(DeleteFileDetails.TYPE_DOCUMENT);
				deleteDetails.setFileToBeDeletedPath(document[1]);
				deleteDetails.setFileToBeDeletedName(document[1].substring(document[1].lastIndexOf("\\")+1));
				processDataForDocumentDeletion(deleteDetails, itemDetails, wslId, channelFolderName);
				deleteDetails = null;
			}
			withdrawnDocuments = null;
			channelFolderName=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startDeleteProcessing()", e);
		}
	}
	
	private void startDataPreparation(File materialDirFolder, ScheduleItemDetails itemDetails, String imProcessingStatus,String wslId, 
				ArrayList<VinDetails> applicableVINList, ArrayList<DisplayOrderDetails> displayOrderList, 
				ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList,String channelFolderName)
	{
		try
		{
			logger.info("startDataPreparation :: Processing MaterialDirFolder Directory at Path :: > " + PathUtil.winPath(materialDirFolder));
			
			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DATA PREPARATION
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "DATA PREPARATION");
			
			
			/*
			 * Start Processing Child Dirs for each of the SubModel Directory.
			 */
			File[] processingFoldersList = materialDirFolder.listFiles();
			if(null!=processingFoldersList && processingFoldersList.length>0)
			{
				ArrayList<ESICategoryDetails> esiCategoryDetailsList = new ArrayList<ESICategoryDetails>();
				/*
				 * Start Processing Files Here
				 */
				for(int k=0;k<processingFoldersList.length;k++)
				{
					File processingFolder = processingFoldersList[k];
					if(processingFolder.exists())
					{
						if(processingFolder.isFile())
						{
							/*
							 * check for following files
							 * ESI CAT TEXT PLACED HERE.
							 * 
							 */
							if(processingFolder.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.name.esicat"))
									&& processingFolder.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
							{
								logger.info("startDataPreparation :: Processing ESI CATEGORY.txt File");
								
								/*
								 * call function to read LEFT MENU TXT DATA
								 * FOR MME MARKET, THE ESI CAT FILE HAS STEERING TYPES WHEN PROCESSING WD DATA AFTER TITLE IN THE FILE
								 * ELSE FOR SM - NO CHANGE AS PER MC STRUCTURE
								 */
								if(null!=channelFolderName && channelFolderName.trim().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim()))
								{
									esiCategoryDetailsList = ConversionUtils.readESICategoryTextFileForMME_WD(processingFolder);
								}
								else
								{
									esiCategoryDetailsList = ConversionUtils.readESICategoryTextFile(processingFolder);
								}
							}
						}
					}
					processingFolder = null;
				}
				
				
				/*
				 * Start Processing Directory
				 */
				for(int k=0;k<processingFoldersList.length;k++)
				{
					File processingFolder = processingFoldersList[k];
					if(processingFolder.exists())
					{
						if(processingFolder.isDirectory())
						{
							if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")))
							{
								if(null!=channelFolderName && channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
								{
									processHTMLDirectoryForWD(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);	
								}
								else
								{
									processHTMLDirectory(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);
								}
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5").trim().toLowerCase()))
							{
								/*
								 * This is Going to Be Wiring Diagram
								 * HTML5 DIRECTORY
								 * WHICH MAY HAVE - CONN FOLDER / CSS FOLDER / IMG FOLDER / PRINT FOLDER / HTMLS
								 */
								processHTML5DirectoryForWD(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")))
							{
								processPDFDirectory(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.ent.dir")))
							{
								processENTDirectory(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
							{
								processDJVUDirectory(processingFolder, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, displayOrderList, channelFolderName, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
			else
			{
				logger.info("startDataPreparation :: No Processing File or Directory exists in Material Diretory at Path :: > " + PathUtil.winPath(materialDirFolder));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startDataPreparation()", e);
		}
	}
	
	private void startCategoryProcessing(ScheduleItemDetails itemDetails,String wslId)
	{
		try
		{
			/*
			 * 
			 * NOW SINCE DATA PREPARATION IS DONE
			 * START PERFORMING LOOKUPS FOR ALL UNIQUE CATEGORIES AND EXISTENCE IN INFO MANAGER
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "CATEGORY PROCESSING");
			if(null!=materialFolderContentList && materialFolderContentList.size()>0)
			{
				logger.info("startCategoryProcessing :: For Material Folder :: > " + itemDetails.getMaterialFolderName()+". Total Documents found after Data Preparation are :: >"+ materialFolderContentList.size());
				/*
				 * IDENTIFY ALL THE UNIQUE CATEGORIES FROM ALL THE DOCUMENTS FOR THE PROCESSING MATERIAL FOLDER
				 * LOOK UP FOR THEM IN THE INFOMANAGER AND IDENTIFY WHICH ONE EXISTS AND WHICH ONES DOES NOT
				 */
				List<String> uniqueCategoriesList = null;
				ContentDetails contentDetails = null;
				CategoryDetails categoryDetails = null;
				boolean addToList = true;
				for(int a=0;a<materialFolderContentList.size();a++)
				{
					contentDetails = (ContentDetails)materialFolderContentList.get(a);
					if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
					{
						categoryDetails = null;
						for(int b=0;b<contentDetails.getCategoryList().size();b++)
						{
							categoryDetails = (CategoryDetails)contentDetails.getCategoryList().get(b);
							if(null!=categoryDetails && null!=categoryDetails.getCategoryRefKey() && !"".equals(categoryDetails.getCategoryRefKey()))
							{
								addToList = true;
								if(null!=uniqueCategoriesList && uniqueCategoriesList.size()>0)
								{
									for(int c=0;c<uniqueCategoriesList.size();c++)
									{
										if(String.valueOf(uniqueCategoriesList.get(c)).trim().toLowerCase().equals(categoryDetails.getCategoryRefKey().trim().toLowerCase()))
										{
											// category already added to uniqueList
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									if(null==uniqueCategoriesList || uniqueCategoriesList.size()<=0)
									{
										uniqueCategoriesList = new ArrayList<String>();
									}
									// add this category to uniqueList
									uniqueCategoriesList.add(categoryDetails.getCategoryRefKey());
								}
							}
							categoryDetails = null;
						}
					}
					contentDetails = null;
				}
				
				if(null!=uniqueCategoriesList && uniqueCategoriesList.size()>0)
				{
					logger.info("startCategoryProcessing :: Total Unique Categories Found to be Looked Up in InfoManager for Material Folder {"+itemDetails.getMaterialFolderName()+"} are :: >"+ uniqueCategoriesList.size());
					/*
					 * IDENTIFY WHICH OF THE UNIQUE CATEGORIES EXIST IN KAPTURE AND WHICH ONES DO NOT
					 */
					locateCategoriesInKapture(uniqueCategoriesList);
}
				else
				{
					logger.info("startCategoryProcessing :: No Unique Categories Found to be Looked Up in InfoManager for Material Folder {"+itemDetails.getMaterialFolderName()+"}.");
				}
				uniqueCategoriesList = null;
			}
			else
			{
				logger.info("startCategoryProcessing :: No Documents found after Data Preparation for Material Folder :: > " + itemDetails.getMaterialFolderName());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startCategoryProcessing()", e);
		}
	}

	private void startDocumentProcessing(ScheduleItemDetails itemDetails)
	{
		try
		{
			logger.info("--------- For Material Folder {"+itemDetails.getMaterialFolderName()+"}. Data Preparation Reset Success Count Found are :: "+ mfDataPrepSuccessCount);
			logger.info("--------- For Material Folder {"+itemDetails.getMaterialFolderName()+"}. Data Preparation Reset Failure Count Found are :: "+ mfDataPrepfailureCount);
			// the buffered progress counts must be in the tables before they are taken back off
			flushDataPreparationCount();
			// RESET DATA PREPARATION COUNTBOTH SUCCESS & FAILURE FOR THE PROCESSING ITEM
			ScheduleDAO.resetDataPreparationCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), mfDataPrepSuccessCount, mfDataPrepfailureCount);
			// reset mfDataPrepSuccessCount & mfDataPrepfailureCount
			mfDataPrepSuccessCount = 0;
			mfDataPrepfailureCount = 0;
			
			/*
			 * 
			 * NOW SINCE DATA PREPARATION IS DONE
			 * START PERFORMING LOOKUPS FOR ALL UNIQUE CATEGORIES AND EXISTENCE IN INFO MANAGER
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "DOCUMENT PROCESSING");
			if(null!=materialFolderContentList && materialFolderContentList.size()>0)
			{
				logger.info("startDocumentProcessing :: For Material Folder :: > " + itemDetails.getMaterialFolderName()+". Total Documents found after Data Preparation are :: >"+ materialFolderContentList.size());
				// documents in batches - see processDocumentsInBatches()
				processDocumentsInBatches(materialFolderContentList, itemDetails);
			}
			else
			{
				logger.info("startDocumentProcessing :: No Documents found after Data Preparation for Material Folder :: > " + itemDetails.getMaterialFolderName());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startDocumentProcessing()", e);
		}
	}
	
	private void startProcessingOKAssets(File materialDirFolder, ScheduleItemDetails itemDetails, String wslId,String channelFolderName)
	{
		try
		{
			logger.info("startProcessingOKAssets :: Processing MaterialDirFolder Directory at Path :: > " + PathUtil.winPath(materialDirFolder));

			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS OKASSETS PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "OKASSETS PROCESSING");


			/*
			 * Start Processing Child Dir's for each of the SubModel Directory.
			 */
			File[] processingFoldersList = materialDirFolder.listFiles();
			if(null!=processingFoldersList && processingFoldersList.length>0)
			{
				/*
				 * Start Processing Directory
				 */
				for(int k=0;k<processingFoldersList.length;k++)
				{
					File processingFolder = processingFoldersList[k];
					if(processingFolder.exists())
					{
						if(processingFolder.isDirectory())
						{
							if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")))
							{
								// ONLY FOR WD
								if(null!=channelFolderName && channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
								{
									/*
									 * HERE FOLDER TYPE IS ALWAYS GOING TO BE HTML
									 */
									processFolderForOkAssets(processingFolder, itemDetails, "html", wslId, channelFolderName);
								}
								else
								{
									processHTMLDirectoryForOkAssets(processingFolder, itemDetails, wslId, channelFolderName);
								}
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5")))
							{
								/*
								 * This is Going to Be Wiring Diagram
								 * HTML5 DIRECTORY
								 * WHICH MAY HAVE - CONN FOLDER / CSS FOLDER / IMG FOLDER / PRINT FOLDER / HTMLS
								 */
								processFolderForHTML5OkAssets(processingFolder, itemDetails, "html5", wslId, channelFolderName);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")))
							{
								/*
								 * CHECK IF WIRING DIAGRAM AND THE MATERIAL FOLDER DOESN'T CONAINTS ANY XML / DJVU / HTML / PDF
								 */
								if(null!=channelFolderName && channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
								{
									if(null!=itemDetails && null!=itemDetails.getMaterialFolderName() && !"".equals(itemDetails.getMaterialFolderName()))
									{
										String tok="";
										String mtName = itemDetails.getMaterialFolderName().trim();
										if(mtName.lastIndexOf("_")!=-1)
										{
											tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
										}
										if(null==tok)
										{
											tok="";
										}
										// DO NOT CHECK FOR NULL
										if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
										{
											/*
											 * PROCESS PDF FOLDER TO OKASSETS
											 */
											processFolderForOkAssets(processingFolder, itemDetails, "pdf", wslId, channelFolderName);
										}
										tok = null;
										mtName=null;
									}
								}
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
							{
								/*
								 * HERE FOLDER TYPE IS ALWAYS GOING TO BE DJVU
								 */
								processFolderForOkAssets(processingFolder, itemDetails, "djvu", wslId, channelFolderName);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.ent.dir")))
							{
								/*
								 * HERE ITERATE ENT FOLDER AND CHECK FOR XML FILE
								 * IF FOUND - PROCCESS THE FILE AS OK ASSET FILE
								 */
								processENTDirectoryForOkAssets(processingFolder, itemDetails, wslId, channelFolderName);
							}
							else if(processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.img.dir")) || 
									processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.figures")) || 
									processingFolder.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.figure")))
							{
								if(null!=channelFolderName && !"".equals(channelFolderName))
								{
									File[] images = processingFolder.listFiles();
									if(null!=images && images.length>0)
									{
										for(int e=0;e<images.length;e++)
										{
											File imageFile = images[e];
											if(imageFile.isFile() && imageFile.exists())
											{
												if(!imageFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
												{
													/*
													 * XCOPY IMAGES TO THE OKASSETS DIRECTORY 
													 * APPEND IMMEDIATE PARENT NAME AS WELL IN THE PATH e..g. YEAR FOLDER NAME PATH
													 */
													ContentDetails xcopyContentDetails = new ContentDetails();
													xcopyContentDetails.setWslId(wslId);
													xcopyContentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
													xcopyContentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
													xcopyContentDetails.setMarket(itemDetails.getMarket());
													xcopyContentDetails.setLocale(itemDetails.getLocale());
													xcopyContentDetails.setModel(itemDetails.getModel());
													xcopyContentDetails.setModelType(itemDetails.getModelType());
													xcopyContentDetails.setCarlineCode(itemDetails.getCarlineCode());
													xcopyContentDetails.setModelFolderName(itemDetails.getModelFolderName());
													xcopyContentDetails.setManualType(itemDetails.getManualType());
													xcopyContentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
													xcopyContentDetails.setMaterialName(itemDetails.getMaterialFolderName());
													xcopyContentDetails.setChannelName(channelFolderName.toUpperCase());
													xcopyContentDetails.setThreadId(itemDetails.getThreadId());

													/*
													 * IF CHANNEL FOLDER IS NOT WIRING DIAGRAMS, THEN NO MODEL
													 *  AND MATERIAL VALUES WILL BE PASSED
													 */
													if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
													{
														xcopyUtils.copyFilesToServer(imageFile, itemDetails.getLocale(), channelFolderName, itemDetails.getModelFolderName(), itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), itemDetails.getMaterialFolderName(), "image", xcopyContentDetails,wiringDiagramUtils);
													}
													else
													{
														// SM / OSM
														xcopyUtils.copyFilesToServer(imageFile, itemDetails.getLocale(), channelFolderName, "", "", "", "", "image", xcopyContentDetails,wiringDiagramUtils);
													}
													xcopyContentDetails = null;
												}
											}
											imageFile = null;
										}
									}
									images = null;
								}
							}
						}
					}
				}
			}
			else
			{
				logger.info("startProcessingOKAssets :: No Processing File or Directory exists in Material Diretory at Path :: > " + PathUtil.winPath(materialDirFolder));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startProcessingOKAssets()", e);
		}
	}
	
	private void processFolderForHTML5OkAssets(File processingFolder, ScheduleItemDetails itemDetails, String folderType, String wslId, String channelFolderName)
	{
		try
		{
			ContentDetails xcopyContentDetails1 = new ContentDetails();
			xcopyContentDetails1.setWslId(wslId);
			xcopyContentDetails1.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			xcopyContentDetails1.setItemId(String.valueOf(itemDetails.getItemId()));
			xcopyContentDetails1.setMarket(itemDetails.getMarket());
			xcopyContentDetails1.setLocale(itemDetails.getLocale());
			xcopyContentDetails1.setModel(itemDetails.getModel());
			xcopyContentDetails1.setModelType(itemDetails.getModelType());
			xcopyContentDetails1.setCarlineCode(itemDetails.getCarlineCode());
			xcopyContentDetails1.setModelFolderName(itemDetails.getModelFolderName());
			xcopyContentDetails1.setManualType(itemDetails.getManualType());
			xcopyContentDetails1.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			xcopyContentDetails1.setMaterialName(itemDetails.getMaterialFolderName());
			xcopyContentDetails1.setChannelName(channelFolderName.toUpperCase());
			xcopyContentDetails1.setThreadId(itemDetails.getThreadId());
			/*
			 * HERE, COMPLETE PATH FOR MOVING THE FILES WILL BE USED.
			 *e.g. CHANNEL/LOCALE/MODEL_FOLDER/MANUAL_TYPE/FACE_LIFT/MATERIAL_NAME/PROCESSING_FOLDER/FILENAME
			 */
			xcopyUtils.copyFilesToServerForHTML5(processingFolder, itemDetails.getLocale(), channelFolderName, itemDetails.getModelFolderName(),itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), 
					itemDetails.getMaterialFolderName(), "html5", xcopyContentDetails1, wiringDiagramUtils);
			
			xcopyContentDetails1 = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processFolderForHTML5OkAssets()", e);
		}
	}

	private void processFolderForOkAssets(File processingFolder, ScheduleItemDetails itemDetails, String folderType, String wslId, String channelFolderName)
	{
		try
		{
			ContentDetails xcopyContentDetails1 = new ContentDetails();
			xcopyContentDetails1.setWslId(wslId);
			xcopyContentDetails1.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			xcopyContentDetails1.setItemId(String.valueOf(itemDetails.getItemId()));
			xcopyContentDetails1.setMarket(itemDetails.getMarket());
			xcopyContentDetails1.setLocale(itemDetails.getLocale());
			xcopyContentDetails1.setModel(itemDetails.getModel());
			xcopyContentDetails1.setModelType(itemDetails.getModelType());
			xcopyContentDetails1.setCarlineCode(itemDetails.getCarlineCode());
			xcopyContentDetails1.setModelFolderName(itemDetails.getModelFolderName());
			xcopyContentDetails1.setManualType(itemDetails.getManualType());
			xcopyContentDetails1.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			xcopyContentDetails1.setMaterialName(itemDetails.getMaterialFolderName());
			xcopyContentDetails1.setChannelName(channelFolderName.toUpperCase());
			xcopyContentDetails1.setThreadId(itemDetails.getThreadId());
			/*
			 * HERE, COMPLETE PATH FOR MOVING THE FILES WILL BE USED.
			 *e.g. CHANNEL/LOCALE/MODEL_FOLDER/MANUAL_TYPE/FACE_LIFT/MATERIAL_NAME/PROCESSING_FOLDER/FILENAME
			 */
			xCopyCompleteDirectory(processingFolder, itemDetails.getLocale(), channelFolderName, itemDetails.getModelFolderName(), itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), 
					itemDetails.getMaterialFolderName(), folderType, xcopyContentDetails1);
			xcopyContentDetails1 = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processFolderForOkAsssets()", e);
		}
	}
	
	private void processPDFDirectory(File pdfDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(pdfDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = pdfDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
							{
								/*
								 * CALL FUNCTION TO PROCESS PDF FILE
								 */
								processAPDFFile(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName,displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processPDFDirectory()", e);
		}
	}
	
	private void processAPDFFile(File pdfFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName,
			ArrayList<DisplayOrderDetails> allDisplayOrderList, ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(pdfFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(pdfFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(pdfFile);
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
				
				/*
				 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
				 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
				 * THEN REMOVE EXTENSION FROM THE FILE PATH
				 */
				if(null!=contentDetails.getChannelName() && 
						contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					String tok="";
					String mtName = contentDetails.getMaterialName();
					if(mtName.lastIndexOf("_")!=-1)
					{
						tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
					}
					if(null==tok)
					{
						tok="";
					}
					// DO NOT CHECK FOR NULL
					if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
					{
						// set replaceByPDFTO HTML applicable for this case only
						contentDetails.setReplacePDFBYHTML("YES");
						// REMOVE EXTENSION
						if(filePath.lastIndexOf(".")!=-1)
						{
							filePath = filePath.substring(0, filePath.lastIndexOf("."));
						}
					}
					tok = null;
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			contentDetails.setFileData(pdfFile.length());
			contentDetails.setPdfFileNameAsAttachment(pdfFile.getName());
			contentDetails.setPdfFilePathAsAttachment(PathUtil.winPath(pdfFile));
			
			// SET DOCUMENT TYPE AS PDF
			contentDetails.setDocumentType(ContentDetails.PDF_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}
				
				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}
				
				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
				
				/*
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
				
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAPDFFile()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}

	private void processDJVUDirectory(File djvuDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,
			ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(djvuDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = djvuDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING DJVU FILES
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.djvu")))
							{
								/*
								 * CALL FUNCTION TO PROCESS DJVU FILE
								 */
								processADJVUFile(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName, displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}

			/*
			 * ITERATE ESI CATEGORY LIST AND PROCESS ALL THE ENTRIES AS DOCUMENT 
			 */
//			if(null!=esiCategoryDetailsList && esiCategoryDetailsList.size()>0)
//			{
//				/*
//				 * ITERATE AND START PROCESSING HTML FILES IN ESICATEGORY
//				 */
//				for(int a=0;a<esiCategoryDetailsList.size();a++)
//				{
//					ESICategoryDetails sourceDataDetails = (ESICategoryDetails)esiCategoryDetailsList.get(a);
//					if(null!=sourceDataDetails.getLineType() && sourceDataDetails.getLineType().equals(ConversionUtils.LINE_TYPE_VALID))
//					{
//						/*
//						 * CALL FUNCTION TO PROCESS DJVU FILE
//						 */
//						processADJVUFile(sourceDataDetails, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName, displayOrderList, masterVINList, applicableEngineMissionTypeList);
//					}
//				}
//			}
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processPDFDirectory()", e);
		}
	}
	
	private void processADJVUFile(File djvuFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName, 
			ArrayList<DisplayOrderDetails> allDisplayOrderList, ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
//			contentDetails.setFileName(sourceDataDetails.getFileName());
//			contentDetails.setFileAbsolutePath(sourceDataDetails.getFileAbsolutePath());
//			contentDetails.setFilePath(sourceDataDetails.getFilePath());
			contentDetails.setFileName(djvuFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(djvuFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(djvuFile);
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			// SET DOCUMENT TYPE AS DJVU
			contentDetails.setDocumentType(ContentDetails.DJVU_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				/*
				 * SET HERE UPLOAD RIECTORY PATH 
				 * WHICH WILL BE /LIBRARY/MAZDA/<CHANNEL FOLDER NANE >/LOCALE.TOLOWERCASE/MODEL.TOLOWERCASE /MANUALTYPEFOLDERNAME/FACELIFTFOLDERNAME/YEAR FOLDER NAME TO LOWERCASE
				 */
				String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
				String uploadDirPath=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				if(!uploadDirPath.startsWith("/"))
				{
					uploadDirPath = "/"+uploadDirPath;
				}
				if(!uploadDirPath.endsWith("/"))
				{
					uploadDirPath = uploadDirPath+"/";
				}
				
				String channelRefKey=channelFolderName.toUpperCase().trim();
				channelRefKey = channelRefKey.replace(" ", "_");
				
				// add repository
				uploadDirPath = uploadDirPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
				// add channel Name
				uploadDirPath = uploadDirPath + channelRefKey.toUpperCase() + "/";
				// add locale Folder Name
				uploadDirPath = uploadDirPath + localeFolderName.toLowerCase() + "/";
				// add Model folder name
				uploadDirPath = uploadDirPath+ contentDetails.getModelFolderName().trim().toLowerCase()+"/";
				// add Manual Type folder Name
				uploadDirPath = uploadDirPath+contentDetails.getManualType().trim().toLowerCase()+"/";
				// add facelift folder Name
				uploadDirPath = uploadDirPath + contentDetails.getFaceLiftFolderName().trim().toLowerCase()+"/";
				// add material folder Name
				uploadDirPath = uploadDirPath+contentDetails.getMaterialName().trim().toLowerCase()+"/";
				// add processing folder Name, parent of processing DJVU File
				uploadDirPath = uploadDirPath+djvuFile.getParentFile().getName().trim().toLowerCase()+"/";
				// add djvu html file name
				String fName = djvuFile.getName();
				// replace extension by .html
				if(fName.lastIndexOf(".")!=-1)
				{
					fName= fName.substring(0, fName.lastIndexOf("."));
				}
				// add extension as .html
				fName  = fName+ApplicationProperties.getProperty("extension.html");
				uploadDirPath = uploadDirPath+fName;
						
				// set uploadDirPath in contentDetails
				contentDetails.setUploadDirectoryPath(uploadDirPath);
				uploadDirPath = null;
				localeFolderName = null;
				fName=null;
				channelRefKey = null;
				
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}
				
				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
							
				/*
				 * 
				 * 
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processADJVUFile()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}
	
	
	private void processHTML5DirectoryForWD(File htmlDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(htmlDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = htmlDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")) 
									|| childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm")))
							{
								/*
								 * CALL FUNCTION TO PROCESS HTML5 FILE
								 */
								processAHTML5FileForWD(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName, displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processHTML5DirectoryForWD()", e);
		}
	}
	
	
	private void processHTMLDirectoryForWD(File htmlDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(htmlDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = htmlDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")) 
									|| childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm")))
							{
								/*
								 * CALL FUNCTION TO PROCESS HTML FILE
								 */
								processAHTMLFileForWD(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName, displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processPDFDirectory()", e);
		}
	}
	
	
	private void processAHTML5FileForWD(File htmlFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName, 
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(htmlFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(htmlFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(htmlFile);
			
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
				if(null!=filePath && !"".equals(filePath))
				{
					/*
					 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
					 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
					 * THEN REMOVE EXTENSION FROM THE FILE PATH
					 */
					if(null!=contentDetails.getChannelName() && contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
					{
						/*
						 * ALSO SINCE IT'S A WIRING DIAGRAM AND MATERIAL FOLDER CONTAINS BOTH HTML & PDF
						 * HERE, WE ARE PROCESSING HTML FILE, SO IDENTIFY THE FONT CONTENT 
						 * FROM THE FILE AND SET IT CONTENT DETAILS
						 */
						if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
						{
							try
							{
								/*
								 * CALL FILE READ FUNCTION FOR READING THIS HTML, SINCE IN E-WD
								 * BECAUSE OF FRAMESET, GETSTRINGFROMHTML WAS NOT READING BODY TAG AND ITS CONTENT.
								 */
//								String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromHTML(htmlFile);
								String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromXML(htmlFile);
								
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									// SET FONT CONTENT
									contentDetails.setFontContent(ConversionUtils.readFontDataFromHTML(htmlContent));
								}
								htmlContent=  null;
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(StartMCConversionImpl.class.getName(), "processAHTML5FileForWD()", e);
							}
						}
					}
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			
			// SET DOCUMENT TYPE AS HTML5
			contentDetails.setDocumentType(ContentDetails.HTML5_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList, ApplicationProperties.getProperty("directory.html5"));
			
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				/*
				 * SET HERE UPLOAD RIECTORY PATH 
				 * WHICH WILL BE /LIBRARY/MAZDA/<CHANNEL FOLDER NANE >/LOCALE.TOLOWERCASE/MODEL.TOLOWERCASE /MANUALTYPEFOLDERNAME/FACELIFTFOLDERNAME/YEAR FOLDER NAME 
				 * TO LOWERCASE
				 */
				String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
				String uploadDirPath=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				if(!uploadDirPath.startsWith("/"))
				{
					uploadDirPath = "/"+uploadDirPath;
				}
				if(!uploadDirPath.endsWith("/"))
				{
					uploadDirPath = uploadDirPath+"/";
				}
				
				String channelRefKey=channelFolderName.toUpperCase().trim();
				channelRefKey = channelRefKey.replace(" ", "_");
				
				
				// add repository
				uploadDirPath = uploadDirPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
				// add channel Name
				uploadDirPath = uploadDirPath + channelRefKey.toUpperCase() + "/";
				// add locale Folder Name
				uploadDirPath = uploadDirPath + localeFolderName.toLowerCase() + "/";
				// add Model folder name
				uploadDirPath = uploadDirPath+ contentDetails.getModelFolderName().trim().toLowerCase()+"/";
				// add Manual Type folder Name
				uploadDirPath = uploadDirPath+contentDetails.getManualType().trim().toLowerCase()+"/";
				// add facelift folder Name
				uploadDirPath = uploadDirPath + contentDetails.getFaceLiftFolderName().trim().toLowerCase()+"/";
				// add material folder Name
				uploadDirPath = uploadDirPath+contentDetails.getMaterialName().trim().toLowerCase()+"/";
				
				// set to uploadDirectpryPath in contentDetails
				contentDetails.setUploadDirectoryPath(uploadDirPath);
				
				// add processing folder Name, parent of processing HTML5 File
				uploadDirPath = uploadDirPath+htmlFile.getParentFile().getName().trim().toLowerCase()+"/";
				// add html5 file name
				String fName = htmlFile.getName();
				uploadDirPath = uploadDirPath+fName;
						
				// set uploadDirPath in contentDetails.setHtml5FileSourcePath
				contentDetails.setHtml5FileSourcePath(uploadDirPath);
				uploadDirPath = null;
				localeFolderName = null;
				fName=null;
				channelRefKey = null;
				
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}
				
				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList, ApplicationProperties.getProperty("directory.html5"));
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
							
				/*
				 * 
				 * 
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				* add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
				
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTML5FileForWD()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}

	
	private void processAHTMLFileForWD(File htmlFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName, 
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(htmlFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(htmlFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(htmlFile);
			
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
				if(null!=filePath && !"".equals(filePath))
				{
					/*
					 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
					 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
					 * THEN REMOVE EXTENSION FROM THE FILE PATH
					 */
					if(null!=contentDetails.getChannelName() && contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
					{
						String tok="";
						String mtName = contentDetails.getMaterialName();
						if(mtName.lastIndexOf("_")!=-1)
						{
							tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
						}
						if(null==tok)
						{
							tok="";
						}

						// DO NOT CHECK FOR NULL
						if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
						{
							// set replaceByPDFTO HTML applicable for this case only
							contentDetails.setReplacePDFBYHTML("YES");
							// REMOVE EXTENSION
							if(filePath.lastIndexOf(".")!=-1)
							{
								filePath = filePath.substring(0, filePath.lastIndexOf("."));
							}
							
							
							/*
							 * ALSO SINCE IT'S A WIRING DIAGRAM AND MATERIAL FOLDER CONTAINS BOTH HTML & PDF
							 * HERE, WE ARE PROCESSING HTML FILE, SO IDENTIFY THE FONT CONTENT 
							 * FROM THE FILE AND SET IT CONTENT DETAILS
							 */
							if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
							{
								try
								{
									/*
									 * CALL FILE READ FUNCTION FOR READING THIS HTML, SINCE IN E-WD
									 * BECAUSE OF FRAMESET, GETSTRINGFROMHTML WAS NOT READING BODY TAG AND ITS CONTENT.
									 */
//									String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromHTML(htmlFile);
									String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromXML(htmlFile);
									
									if(null!=htmlContent && !"".equals(htmlContent))
									{
										// SET FONT CONTENT
										contentDetails.setFontContent(ConversionUtils.readFontDataFromHTML(htmlContent));
									}
									htmlContent=  null;
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFileForWD()", e);
								}
							}
						}
						tok = null;
					}
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			
			// SET DOCUMENT TYPE AS HTML_MC
			contentDetails.setDocumentType(ContentDetails.HTML_MC_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				/*
				 * SET HERE UPLOAD RIECTORY PATH 
				 * WHICH WILL BE /LIBRARY/MAZDA/<CHANNEL FOLDER NANE >/LOCALE.TOLOWERCASE/MODEL.TOLOWERCASE /MANUALTYPEFOLDERNAME/FACELIFTFOLDERNAME/YEAR FOLDER NAME TO LOWERCASE
				 */
				String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
				String uploadDirPath=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				if(!uploadDirPath.startsWith("/"))
				{
					uploadDirPath = "/"+uploadDirPath;
				}
				if(!uploadDirPath.endsWith("/"))
				{
					uploadDirPath = uploadDirPath+"/";
				}
				
				String channelRefKey=channelFolderName.toUpperCase().trim();
				channelRefKey = channelRefKey.replace(" ", "_");
				
				
				// add repository
				uploadDirPath = uploadDirPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
				// add channel Name
				uploadDirPath = uploadDirPath + channelRefKey.toUpperCase() + "/";
				// add locale Folder Name
				uploadDirPath = uploadDirPath + localeFolderName.toLowerCase() + "/";
				// add Model folder name
				uploadDirPath = uploadDirPath+ contentDetails.getModelFolderName().trim().toLowerCase()+"/";
				// add Manual Type folder Name
				uploadDirPath = uploadDirPath+contentDetails.getManualType().trim().toLowerCase()+"/";
				// add facelift folder Name
				uploadDirPath = uploadDirPath + contentDetails.getFaceLiftFolderName().trim().toLowerCase()+"/";
				// add material folder Name
				uploadDirPath = uploadDirPath+contentDetails.getMaterialName().trim().toLowerCase()+"/";
				// add processing folder Name, parent of processing DJVU File
				uploadDirPath = uploadDirPath+htmlFile.getParentFile().getName().trim().toLowerCase()+"/";
				// add html file name
				String fName = htmlFile.getName();
				uploadDirPath = uploadDirPath+fName;
						
				// set uploadDirPath in contentDetails
				contentDetails.setUploadDirectoryPath(uploadDirPath);
				uploadDirPath = null;
				localeFolderName = null;
				fName=null;
				channelRefKey = null;
				
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}
				
				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
							
				/*
				 * 
				 * 
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFileForWD()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}
	
	public void processAHTMLFileForWD_BackUp(ESICategoryDetails sourceContentDetails, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName, 
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(sourceContentDetails.getFileName());
			contentDetails.setFileAbsolutePath(sourceContentDetails.getFileAbsolutePath());
			contentDetails.setFilePath(sourceContentDetails.getFilePath());
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=sourceContentDetails.getFilePath();
			if(null!=filePath && !"".equals(filePath))
			{
				/*
				 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
				 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
				 * THEN REMOVE EXTENSION FROM THE FILE PATH
				 */
				if(null!=contentDetails.getChannelName() && contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					String tok="";
					String mtName = contentDetails.getMaterialName();
					if(mtName.lastIndexOf("_")!=-1)
					{
						tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
					}
					if(null==tok)
					{
						tok="";
					}

					// DO NOT CHECK FOR NULL
					if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
							!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
					{
						// set replaceByPDFTO HTML applicable for this case only
						contentDetails.setReplacePDFBYHTML("YES");
						// REMOVE EXTENSION
						if(filePath.lastIndexOf(".")!=-1)
						{
							filePath = filePath.substring(0, filePath.lastIndexOf("."));
						}
						
						
						/*
						 * ALSO SINCE IT'S A WIRING DIAGRAM AND MATERIAL FOLDER CONTAINS BOTH HTML & PDF
						 * HERE, WE ARE PROCESSING HTML FILE, SO IDENTIFY THE FONT CONTENT 
						 * FROM THE FILE AND SET IT CONTENT DETAILS
						 */
						if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
						{
							/*
							 * CHECK HERE IF FILE PATH DOESN'T ENDS WITH .html, then add it
							 */
							String tempPath = contentDetails.getFileAbsolutePath();
							if(!tempPath.endsWith(ApplicationProperties.getProperty("extension.html")))
							{
								tempPath = tempPath+ApplicationProperties.getProperty("extension.html");
							}
							try
							{
								File htmlFile = PathUtil.file(tempPath);
								String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromHTML(htmlFile);
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									// SET FONT CONTENT
									contentDetails.setFontContent(ConversionUtils.readFontDataFromHTML(htmlContent));
								}
								htmlContent=  null;
								htmlFile= null;
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFileForWD()", e);
								logger.info("processAHTMLFileForWD :: "+tempPath+" Does not Exists, check for .htm file.");
								// CHECK WITH .HTM EXTENSION
								String htmTempPath  =contentDetails.getFileAbsolutePath();
								if(!htmTempPath.endsWith(ApplicationProperties.getProperty("extension.htm")))
								{
									htmTempPath = htmTempPath+ApplicationProperties.getProperty("extension.htm");
								}
								try
								{
									File htmlFile = PathUtil.file(htmTempPath);
									String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromHTML(htmlFile);
									if(null!=htmlContent && !"".equals(htmlContent))
									{
										// SET FONT CONTENT
										contentDetails.setFontContent(ConversionUtils.readFontDataFromHTML(htmlContent));
									}
									htmlContent=  null;
									htmlFile= null;
								}
								catch(Exception eq)
								{
									Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFileForWD()", eq);
								}
							}
						}
					}
				
					tok = null;
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			
			// SET DOCUMENT TYPE AS HTML_MC
			contentDetails.setDocumentType(ContentDetails.HTML_MC_DOCUMENT);
			
			// SET USERGROUPS FOR DOCUMENT
//			contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
			contentDetails.setUserGroupsList(groupsListForContent);
			/*
			 * SET HERE UPLOAD RIECTORY PATH 
			 * WHICH WILL BE /LIBRARY/MAZDA/<CHANNEL FOLDER NANE >/LOCALE.TOLOWERCASE/MODEL.TOLOWERCASE /MANUALTYPEFOLDERNAME/FACELIFTFOLDERNAME/YEAR FOLDER NAME TO LOWERCASE
			 */
			String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
			String uploadDirPath=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
			if(!uploadDirPath.startsWith("/"))
			{
				uploadDirPath = "/"+uploadDirPath;
			}
			if(!uploadDirPath.endsWith("/"))
			{
				uploadDirPath = uploadDirPath+"/";
			}
			
			String channelRefKey=channelFolderName.toUpperCase().trim();
			channelRefKey = channelRefKey.replace(" ", "_");
			
			
			// add repository
			uploadDirPath = uploadDirPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
			// add channel Name
			uploadDirPath = uploadDirPath + channelRefKey.toUpperCase() + "/";
			// add locale Folder Name
			uploadDirPath = uploadDirPath + localeFolderName.toLowerCase() + "/";
			// add Model folder name
			uploadDirPath = uploadDirPath+ contentDetails.getModelFolderName().trim().toLowerCase()+"/";
			// add Manual Type folder Name
			uploadDirPath = uploadDirPath+contentDetails.getManualType().trim().toLowerCase()+"/";
			// add facelift folder Name
			uploadDirPath = uploadDirPath + contentDetails.getFaceLiftFolderName().trim().toLowerCase()+"/";
			// add material folder Name
			uploadDirPath = uploadDirPath+contentDetails.getMaterialName().trim().toLowerCase()+"/";
			// add processing folder Name, parent of processing DJVU File
			uploadDirPath = uploadDirPath+sourceContentDetails.getProcessingFolderName().trim().toLowerCase()+"/";
			// add html file name
			String fName = sourceContentDetails.getFileName();
			// check extension by .html, if not ends with it then add it
			if(!fName.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")))
			{
				fName= fName+ApplicationProperties.getProperty("extension.html");
			}
			uploadDirPath = uploadDirPath+fName;
					
			// set uploadDirPath in contentDetails
			contentDetails.setUploadDirectoryPath(uploadDirPath);
			uploadDirPath = null;
			localeFolderName = null;
			fName=null;
			channelRefKey = null;
			
			// SET MASTER VIN LIST
			if(null!=masterVINList && masterVINList.size()>0)
			{
				contentDetails.setMasterVinList(masterVINList);
			}

			// SET ENGINE / MISSION TYPE LIST
			if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
			{
				contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
			}
			
			// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
			if(null!=applicableVINList && applicableVINList.size()>0)
			{
				contentDetails.setApplicableVINList(applicableVINList);
			}
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			
			// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//			contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
			contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
			
			// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
			contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
						
			/*
			 * 
			 * 
			 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
			 * add this Document to materialFolderContentList
			 */
			if(null==materialFolderContentList || materialFolderContentList.size()<=0)
			{
				materialFolderContentList = new ArrayList<ContentDetails>();
			}
			materialFolderContentList.add(contentDetails);
			// increment successCount
			mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
			//performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFileForWD()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}
	
	
	private void processHTMLDirectory(File htmlDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(htmlDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = htmlDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")))
							{
								/*
								 * CALL FUNCTION TO PROCESS HTML FILE
								 */
								processAHTMLFile(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName,displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processHTMLDirectory()", e);
		}
	}
	
	private void processAHTMLFile(File htmlFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName, 
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(htmlFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(htmlFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(htmlFile);
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			// SET DOCUMENT TYPE AS PDF
			contentDetails.setDocumentType(ContentDetails.HTML_MC_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				String styleContent = "";
				String linkCssContent="";
				
				// READ CONTENT OF THE HTML FILE
				String htmlContent = com.mazda.gms3.dmt.conversion.utils.ConversionUtils.getStringFromHTML(htmlFile);
				if(null!=htmlContent && !"".equals(htmlContent))
				{
					/*
					 * TOYOTA CHANGE - ADD LINK ATTRIBUTE HAVING CSS - CONTENT.CSS OR CONTENTS.CSS ONLY
					 * COPIED FORM MC - DATE CHANGE = 21 MAY 2021
					 * READ STYLE CSS CONTENT - LINK CSS , FOLDER MAY CONTAIN CSS FILES AS WELL WHICH NEEDS TO NBE MOVED TO OKASSETS 
					 * AND THESE CSS NEEDS TO BE APPENDED TO CONTENT BEFORE MOVING TO IM.
					 */
					linkCssContent = ConversionUtils.readLinkCSSContent(htmlContent, contentDetails);
					
					/*
					 * DATE - 02 JULY 2018
					 * READ META TAG FROM HTML CONTENT TO IDENTIFY WHETHER THE PROCESSING MODEL CONTENT IS 
					 * TOYOTA CONTENT OR NORMAL MODEL CONTENT
					 */
					contentDetails.setMetaTagDescription(ConversionUtils.readMetaTagDescriptionValue(htmlContent));
					if(null!=contentDetails.getMetaTagDescription() && !"".equals(contentDetails.getMetaTagDescription()))
					{
						if(contentDetails.getMetaTagDescription().trim().toLowerCase().equals("toyota"))
						{
							contentDetails.setToyotaContent(true);
						}
					}
					
					// READ STYLE CONTENT FROM THE HTML
					styleContent = ConversionUtils.readStyleTagContent(htmlContent);
					
					// REPALCE IMAGES PATH IN THE CONTENT
					htmlContent=  ConversionUtils.replaceOkAssetsImagesSrcContent(htmlContent, contentDetails);
					
					// set HTML CONTENT IN CONTENT DETAILS
					if(null!=htmlContent && !"".equals(htmlContent))
					{
						contentDetails.setDocumentContent(htmlContent);
					}
					
					// READ ALL THE INNER LINKS FROM CONTENT AND PREPARE INNER LINKS PATHS FOR THEM,
					contentDetails = ConversionUtils.prepareInnerLinkPaths(contentDetails);
					
					if(null!=contentDetails.getAdditionalInnerLinks() && contentDetails.getAdditionalInnerLinks().size()>0)
					{
						// add to all OtherLinks List for reporting purpose
						if(null==otherInnerLinksList || otherInnerLinksList.size()<=0)
						{
							  otherInnerLinksList = new ArrayList<ContentDetails>();
						}
						otherInnerLinksList.add(contentDetails);
					}
					
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						/*
						 * THE DOCUMENT NEEDS TO BE PROCESSED FOR INNER LINKS
						 */
						mateialFolderDocumentsListForInnerLinks.add(contentDetails);
					}
				}
				
				/*
				 * SINCE ALL THE OPERATIONS ARE COMPLETED. NOW SET THE BODY PART OF THE HTML DATA.
				 */
				if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
				{
					htmlContent= contentDetails.getDocumentContent();
				}
				// READ BODY CONTENT OF THE HTML FILE AND SET AS DOCUMENT CONTENT
				htmlContent = wiringDiagramUtils.readBodyContent(htmlContent);
				if(null!=htmlContent && !"".equals(htmlContent))
				{
					if(null!=linkCssContent && !"".equals(linkCssContent))
					{
						// add LINK CSS CONTENT TO HTML DATA
						htmlContent = linkCssContent+htmlContent;
					}
					if(null!=styleContent && !"".equals(styleContent))
					{
						// add STYLE COTNENT TO HTML DATA
						htmlContent= styleContent+ htmlContent;
					}
					contentDetails.setDocumentContent(htmlContent);
				}
				htmlContent= null;
				styleContent = null;
				linkCssContent = null;
				
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}

				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
				
				/*
				 * 
				 * 
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAHTMLFile()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}

	private void processENTDirectory(File entDiretory, ArrayList<VinDetails> applicableVINList, ArrayList<ESICategoryDetails> esiCategoryDetailsList ,
			ScheduleItemDetails itemDetails , String imProcessingStatus, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, String channelFolderName,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		try
		{
			if(entDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE ENT FILES
				 */
				File[] childFiles = entDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING ENT as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent")))
							{
								/*
								 * CALL FUNCTION TO PROCESS ENT FILE
								 */
								processAENTFile(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName,displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
							else if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.xml")))
							{
								/*
								 * CALL FUNCTION TO PROCESS RDF FILE WHICH IS PROVIDED AS AN XML FILE
								 */
								processARDFFile(childFile, applicableVINList, esiCategoryDetailsList, itemDetails, imProcessingStatus, wslId, channelFolderName, displayOrderList, masterVINList, applicableEngineMissionTypeList);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processENTDirectory()", e);
		}
	}

	private void processAENTFile(File entFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName,
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(entFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(entFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(entFile);
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			// SET DOCUMENT TYPE AS PDF
			contentDetails.setDocumentType(ContentDetails.ENT_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				// READ CONTENT OF THE ENT FILE
				String htmlContent="";
				contentDetails = EntParsing.parseEntFile(entFile, ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()), contentDetails);
				if(null!=contentDetails && null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
				{
					htmlContent = contentDetails.getDocumentContent();
				}
				if(null!=htmlContent && !"".equals(htmlContent))
				{
					// REPALCE IMAGES PATH IN THE CONTENT
					htmlContent=  ConversionUtils.replaceOkAssetsImagesSrcContent(htmlContent, contentDetails);
					// REPLACE PDFS PATH IN THE CONTENT
					htmlContent= ConversionUtils.replaceOKAssetsPdfPathsInContent(htmlContent, contentDetails);
					// set HTML CONTENT IN CONTENT DETAILS
					if(null!=htmlContent && !"".equals(htmlContent))
					{
						contentDetails.setDocumentContent(htmlContent);
					}
					
					// READ ALL THE INNER LINKS FROM CONTENT AND PREPARE INNER LINKS PATHS FOR THEM,
					contentDetails = ConversionUtils.prepareInnerLinkPaths(contentDetails);
					
					if(null!=contentDetails.getAdditionalInnerLinks() && contentDetails.getAdditionalInnerLinks().size()>0)
					{
						// add to all OtherLinks List for reporting purpose
						if(null==otherInnerLinksList || otherInnerLinksList.size()<=0)
						{
							  otherInnerLinksList = new ArrayList<ContentDetails>();
						}
						otherInnerLinksList.add(contentDetails);
					}
					
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						/*
						 * THE DOCUMENT NEEDS TO BE PROCESSED FOR INNER LINKS
						 */
						mateialFolderDocumentsListForInnerLinks.add(contentDetails);
					}
				}
				
				/*
				 * SINCE ALL THE OPERATIONS ARE COMPLETED. NOW SET THE BODY PART OF THE HTML DATA.
				 */
				if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
				{
					htmlContent= contentDetails.getDocumentContent();
				}
				// CALL FUNCTION TO APPEND CUSTOM CSS TO ENT DOCUMENT
				htmlContent= ConversionUtils.addStyleLinkForEntDocumentsForMMEMarket(htmlContent);
				// READ BODY CONTENT OF THE HTML FILE AND SET AS DOCUMENT CONTENT
				htmlContent = wiringDiagramUtils.readBodyContent(htmlContent);
				if(null!=htmlContent && !"".equals(htmlContent))
				{
					contentDetails.setDocumentContent(htmlContent);
				}
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}

				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
				/*
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processAENTFile()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}

	private void processARDFFile(File rdfFile, ArrayList<VinDetails> applicableVINList, 
			ArrayList<ESICategoryDetails> esiCategoryDetailsList , ScheduleItemDetails itemDetails ,  
			String imProcessingStatus, String wslId, String channelFolderName,
			ArrayList<DisplayOrderDetails> allDisplayOrderList,ArrayList<VINEntFileDetails> masterVINList, ArrayList<String> applicableEngineMissionTypeList)
	{
		// increment data Preparation Count
		dataPreparationCount++;
		try
		{
			/*
			 * it is a document
			 * All the PARENT VIN ENT, VIN ATTRIBUTE ENT
			 * CHILD VIN ENT, CHILD VIN ATTRIBUTE ENT
			 * CATEGORY / SUB CATEGORY TITLE WILL BE SET HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
			// SET DATA FROM ITEM DETAILS
			contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
			contentDetails.setMarket(itemDetails.getMarket());
			contentDetails.setLocale(itemDetails.getLocale());
			contentDetails.setModel(itemDetails.getModel());
			contentDetails.setModelType(itemDetails.getModelType());
			contentDetails.setCarlineCode(itemDetails.getCarlineCode());
			contentDetails.setModelFolderName(itemDetails.getModelFolderName());
			contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
			contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
			// SET WSL ID
			contentDetails.setWslId(wslId);
			// SET THREAD ID
			contentDetails.setThreadId(itemDetails.getThreadId());
			/*
			 * IF MODEL IS ENGINE / MISSION - THEN MANUAL TYPE IS BOOK CODE
			 */
			contentDetails.setManualType(itemDetails.getManualType());
			
			// SET IM PROCESSING STATUS FOR SCHEDULE
			contentDetails.setImProcessingStatus(imProcessingStatus);
			
			// SET FILE NAME AND FILE PATH
			contentDetails.setFileName(rdfFile.getName());
			contentDetails.setFileAbsolutePath(PathUtil.winPath(rdfFile));
			
			if(null!=channelFolderName && !"".equals(channelFolderName))
			{
				if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
				}
				else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// SET CHANNEL NAME - OTHER SERVICE MANUALS
					contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
				}
			}
			
			/*
			 * FILE PATHS WILL CONTAIN THE RELATIVE PATHS HERE. APPLICABLE FOR ALL FILES AND ABSOLUTE PATH
			 * WILL CONTAIN THE COMPLETE PATH FROM NETWORK LOCATION
			 */
			String filePath=PathUtil.winPath(rdfFile);
			if(null!=contentDetails.getLocale() && !"".equals(contentDetails.getLocale()))
			{
				if(filePath.lastIndexOf(contentDetails.getLocale())!=-1)
				{
					filePath = filePath.substring(filePath.lastIndexOf(contentDetails.getLocale()), filePath.length());
				}
			}
			contentDetails.setFilePath(filePath);
			filePath = null;
			
			// SET DOCUMENT TYPE AS XML
			contentDetails.setDocumentType(ContentDetails.XML_DOCUMENT);
			
			/*
			 * IDENTIFY TITLE & ESI CATEGORY CODES FOR THE DOCUMENT
			 */
			contentDetails = ConversionUtils.identifyAttributesFromESICategoryDetails(contentDetails, esiCategoryDetailsList,"");
			
			if(null!=contentDetails.getEntryFoundInEsiCat() && contentDetails.getEntryFoundInEsiCat().equals("YES"))
			{
				/*
				 * SET HERE UPLOAD RIECTORY PATH 
				 * WHICH WILL BE /LIBRARY/MAZDA/<CHANNEL FOLDER NANE >/LOCALE.TOLOWERCASE/MODEL.TOLOWERCASE /MANUALTYPEFOLDERNAME/FACELIFTFOLDERNAME/YEAR FOLDER NAME TO LOWERCASE
				 */
				String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
				String uploadDirPath=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				if(!uploadDirPath.startsWith("/"))
				{
					uploadDirPath = "/"+uploadDirPath;
				}
				if(!uploadDirPath.endsWith("/"))
				{
					uploadDirPath = uploadDirPath+"/";
				}
				
				String channelRefKey=channelFolderName.toUpperCase().trim();
				channelRefKey = channelRefKey.replace(" ", "_");
				
				// add repository
				uploadDirPath = uploadDirPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
				// add channel Name
				uploadDirPath = uploadDirPath + channelRefKey.toUpperCase() + "/";
				// add locale Folder Name
				uploadDirPath = uploadDirPath + localeFolderName.toLowerCase() + "/";
				// add Model folder name
				uploadDirPath = uploadDirPath+ contentDetails.getModelFolderName().trim().toLowerCase()+"/";
				// add Manual Type folder Name
				uploadDirPath = uploadDirPath+contentDetails.getManualType().trim().toLowerCase()+"/";
				// add facelift folder Name
				uploadDirPath = uploadDirPath + contentDetails.getFaceLiftFolderName().trim().toLowerCase()+"/";
				// add material folder Name
				uploadDirPath = uploadDirPath+contentDetails.getMaterialName().trim().toLowerCase()+"/";
				// add processing folder Name, parent of processing XML File
				uploadDirPath = uploadDirPath+rdfFile.getParentFile().getName().trim().toLowerCase()+"/";
				// add XML file name
				uploadDirPath = uploadDirPath+rdfFile.getName();
						
				// set uploadDirPath in contentDetails
				contentDetails.setOasisDirectoryPath(uploadDirPath);
				uploadDirPath = null;
				localeFolderName = null;
				channelRefKey = null;
				
				// SET USERGROUPS FOR DOCUMENT
//				contentDetails  = ConversionUtils.addUserGroupsToContent(contentDetails);
				contentDetails.setUserGroupsList(groupsListForContent);
				// SET MASTER VIN LIST
				if(null!=masterVINList && masterVINList.size()>0)
				{
					contentDetails.setMasterVinList(masterVINList);
				}

				// SET ENGINE / MISSION TYPE LIST
				if(null!=applicableEngineMissionTypeList && applicableEngineMissionTypeList.size()>0)
				{
					contentDetails.setApplicableEngineMissionTypeList(applicableEngineMissionTypeList);
				}

				// SET APPLICABLE VIN LIST FOR THE MATERIAL FOLDER
//				if(null!=applicableVINList && applicableVINList.size()>0)
//				{
//					contentDetails.setApplicableVINList(applicableVINList);
//				}
				
				// SET APPLICABLE DISPLAY ORDER FOR THE PROCESSING FILE
//				contentDetails = ConversionUtils.identifyAttributesFromDisplayOrderDetails(contentDetails, allDisplayOrderList,"");
				contentDetails.setApplicableDisplayOrderList(applicableDisplayOrderForDocument(contentDetails.getFilePath()));
				
				/*
				* CHANGE IN IDENTIFYING VIN FOR THE DOCUMENT
				* IDENTIFY THE APPLICABLE DISPLAY ORDER FOR THE PROCESSING DOCUMENT AND 
				* THEN FILTER APPLICABLE VIN MAPPED WITH THE APPLICABLE DISPLAY ORDER (ALL VINS WILL NOT BE MAPPED NOW)
				* DATE - 10 JAN 2018
				*/
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=applicableVINList && applicableVINList.size()>0)
				{
					contentDetails = ConversionUtils.identifyApplicableVINForDocument(contentDetails, applicableVINList);
				}
				
				// CALL FUNCTION TO ADD CATEGORIES TO DOCUMENT
				contentDetails = CategoryUtils.performCategoriesOperation(contentDetails,"MME");
				
				/*
				 * CHECK WHETHER DOCUMENT NEEDS TO BE CREATED / UPDATED DOCUMENT In INFO MANAGER
				 * add this Document to materialFolderContentList
				 */
				if(null==materialFolderContentList || materialFolderContentList.size()<=0)
				{
					materialFolderContentList = new ArrayList<ContentDetails>();
				}
				materialFolderContentList.add(contentDetails);
				// increment successCount
				mfIncrementDataPreparationSuccessCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
//				performDocumentOperations(contentDetails, String.valueOf(itemDetails.getScheduleId()),false);
			}
			contentDetails= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processARDFFile()", e);
			// increment failureCount
			mfIncrementDataPreparationFailureCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
		}
		logger.info("------------- DATA PREARATION FINISHED FOR DOCUMENT "+ dataPreparationCount+" / "+ totalDocsCount+"." );
	}
	
	private void processHTMLDirectoryForOkAssets(File htmlDiretory,ScheduleItemDetails itemDetails, String wslId, String channelFolderName)
	{
		try
		{
			if(htmlDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE PDF FILES
				 */
				File[] childFiles = htmlDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isDirectory())
						{
							if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.image")) || 
									childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.images")))
							{
								/*
								 * XCOPY IMAGES FOLDER
								 */
								File[] imageFilesList = childFile.listFiles();
								if(null!=imageFilesList && imageFilesList.length>0)
								{
									for(int r=0;r<imageFilesList.length;r++)
									{
										File imageFile = imageFilesList[r];
										if(!imageFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
										{
											ContentDetails xcopyContentDetails = new ContentDetails();
											xcopyContentDetails.setWslId(wslId);
											xcopyContentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
											xcopyContentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
											xcopyContentDetails.setMarket(itemDetails.getMarket());
											xcopyContentDetails.setLocale(itemDetails.getLocale());
											xcopyContentDetails.setModel(itemDetails.getModel());
											xcopyContentDetails.setModelType(itemDetails.getModelType());
											xcopyContentDetails.setCarlineCode(itemDetails.getCarlineCode());
											xcopyContentDetails.setModelFolderName(itemDetails.getModelFolderName());
											xcopyContentDetails.setManualType(itemDetails.getManualType());
											xcopyContentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
											xcopyContentDetails.setMaterialName(itemDetails.getMaterialFolderName());
											xcopyContentDetails.setChannelName(channelFolderName.toUpperCase());
											xcopyContentDetails.setThreadId(itemDetails.getThreadId());
											
											// SM / OSM
											xcopyUtils.copyFilesToServer(imageFile, itemDetails.getLocale(), channelFolderName, "", "", "", "", "image", xcopyContentDetails,wiringDiagramUtils);
											xcopyContentDetails = null;
										}
										imageFile = null;
									}
								}
								imageFilesList = null;
							}
						}
						else if(childFile.isFile())
						{
							// CHECK FOR CSS FILES
							if(childFile.getName().trim().toLowerCase().endsWith("."+ApplicationProperties.getProperty("directory.css")))
							{
								// MOVE THIS FILE TO OKASSETS
								ContentDetails xcopyContentDetails = new ContentDetails();
								xcopyContentDetails.setWslId(wslId);
								xcopyContentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
								xcopyContentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
								xcopyContentDetails.setMarket(itemDetails.getMarket());
								xcopyContentDetails.setLocale(itemDetails.getLocale());
								xcopyContentDetails.setModel(itemDetails.getModel());
								xcopyContentDetails.setModelType(itemDetails.getModelType());
								xcopyContentDetails.setCarlineCode(itemDetails.getCarlineCode());
								xcopyContentDetails.setModelFolderName(itemDetails.getModelFolderName());
								xcopyContentDetails.setManualType(itemDetails.getManualType());
								xcopyContentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
								xcopyContentDetails.setMaterialName(itemDetails.getMaterialFolderName());
								xcopyContentDetails.setChannelName(channelFolderName.toUpperCase());
								xcopyContentDetails.setThreadId(itemDetails.getThreadId());

								// SM / OSM  - FOLDER WILL ALWYAS BE - HTML
								xcopyUtils.copyFilesToServer(childFile, itemDetails.getLocale(), channelFolderName, itemDetails.getModelFolderName(), 
										itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), itemDetails.getMaterialFolderName(), 
										"html", xcopyContentDetails,wiringDiagramUtils);
								xcopyContentDetails = null;
							}
						}
						childFile = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processHTMLDirectoryForOkAssets()", e);
		}
	}

	private void processENTDirectoryForOkAssets(File entDiretory,ScheduleItemDetails itemDetails, String wslId, String channelFolderName)
	{
		try
		{
			if(entDiretory.isDirectory())
			{
				/*
				 * ITERATE FOLDER AND IDENTIFY ALL THE XML FILES
				 */
				File[] childFiles = entDiretory.listFiles();
				if(null!=childFiles && childFiles.length>0)
				{
					/*
					 * ITERATE AND START PROCESSING PDF as Documents
					 */
					for(int a=0;a<childFiles.length;a++)
					{
						File childFile = childFiles[a];
						if(childFile.isFile())
						{
							if(childFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.xml")))
							{
								ContentDetails xcopyContentDetails = new ContentDetails();
								xcopyContentDetails.setWslId(wslId);
								xcopyContentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
								xcopyContentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
								xcopyContentDetails.setMarket(itemDetails.getMarket());
								xcopyContentDetails.setLocale(itemDetails.getLocale());
								xcopyContentDetails.setModel(itemDetails.getModel());
								xcopyContentDetails.setModelType(itemDetails.getModelType());
								xcopyContentDetails.setCarlineCode(itemDetails.getCarlineCode());
								xcopyContentDetails.setModelFolderName(itemDetails.getModelFolderName());
								xcopyContentDetails.setManualType(itemDetails.getManualType());
								xcopyContentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
								xcopyContentDetails.setMaterialName(itemDetails.getMaterialFolderName());
								xcopyContentDetails.setChannelName(channelFolderName.toUpperCase());
								xcopyContentDetails.setThreadId(itemDetails.getThreadId());
								
								// SM / OSM
								xcopyUtils.copyFilesToServer(childFile, itemDetails.getLocale(), channelFolderName, itemDetails.getModelFolderName(), itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), itemDetails.getMaterialFolderName(), "ent.gms3", xcopyContentDetails,wiringDiagramUtils);
								
								xcopyContentDetails = null;
							}
						}
						childFile = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processENTDirectoryForOkAssets()", e);
		}
	}

	
	/**
	 * Function will stop the Active Thread in Web Container once the Job Finishes
	 * @param scheduleCode
	 */
	@SuppressWarnings("deprecation")
	private void stopActiveThead(String scheduleCode)
	{
		try
		{
			String threadId=ApplicationProperties.getProperty("schedule.name.mme.key")+scheduleCode;
			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
			Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
			if (null != threadArray && threadArray.length > 0) 
			{
				for (int i = 0; i < threadArray.length; i++) 
				{
					Thread at = threadArray[i];
					if (null != at.getName() && !"".equals(at.getName())) {
						if (at.getName().equals(threadId)) 
						{
							try 
							{
								if (at.isAlive()) 
								{
									logger.info(" ################################### STOPPING THREAD ID :: > " + threadId);
									com.mazda.gms3.dmt.utils.ThreadAbortUtil.abort(at);
								}
							}
							catch (Exception e) 
							{
								Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "stopActiveThead()", e);
		}
	}
	
	/*
	 * =====================================================================================
	 * DOCUMENTS IN BATCHES
	 *
	 * The documents of the material folder are written kapture.bulk.max.articles.per.request at
	 * a time: ONE lookup for which of them exist, ONE bulk request (or as few as the byte limit
	 * allows) for the new ones and one for the changed ones, then ONE database transaction for
	 * all their details and ONE count update. Per document the outcome is the same as
	 * performDocumentOperations(): the same Kapture write, the same rows, the same errors in the
	 * reports. kapture.bulk.max.articles.per.request=1 gives the one-by-one behaviour back.
	 * =====================================================================================
	 */
	private void processDocumentsInBatches(List<ContentDetails> documents, ScheduleItemDetails itemDetails)
	{
		int window = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("kapture.bulk.max.articles.per.request"));
		for (int from = 0; from < documents.size(); from += window)
		{
			List<ContentDetails> part = new ArrayList<ContentDetails>(documents.subList(from, Math.min(from + window, documents.size())));
			long started = System.currentTimeMillis();
			logger.info("processDocumentsInBatches :: documents " + (from + 1) + " - " + (from + part.size()) + " of " + documents.size());
			try
			{
				processDocumentBatch(part);
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDocumentsInBatches()", e);
			}
			finally
			{
				// every document of the batch is reported, whatever happened to it
				for (ContentDetails cd : part)
				{
					if(null==processingFileDetailsList)
					{
						processingFileDetailsList = new ArrayList<ContentDetails>();
					}
					processingFileDetailsList.add(cd);
				}
			}
			logger.info("processDocumentsInBatches :: documents " + (from + 1) + " - " + (from + part.size()) + " done in "
					+ (System.currentTimeMillis() - started) + " ms");
		}
	}

	private static boolean hasText(String s)
	{
		return null != s && !"".equals(s.trim());
	}

	/** "locale|model type" - the MME tables a document is written to (gms3_dmt_<locale>_[nm_]*). */
	private static String tableSetKey(ContentDetails cd)
	{
		return (null == cd.getLocale() ? "" : cd.getLocale()) + "|" + (null == cd.getModelType() ? "" : cd.getModelType());
	}

	private void processDocumentBatch(List<ContentDetails> part) throws Exception
	{
		long phaseStarted = System.currentTimeMillis();
		/*
		 * 1. NEW OR EXISTING - one query per model type; WD documents fed by both PDF and HTML
		 *    keep their own lookup (either file name may be the one stored).
		 */
		// MME: one table set per locale + model type - the key is "locale|model type"
		Map<String, Map<String, String[]>> existingByModelType = new HashMap<String, Map<String, String[]>>();
		Map<String, List<String>> pathsByModelType = new HashMap<String, List<String>>();
		for (ContentDetails cd : part)
		{
			if (!"YES".equals(cd.getReplacePDFBYHTML()))
			{
				String mt = tableSetKey(cd);
				if (!pathsByModelType.containsKey(mt))
				{
					pathsByModelType.put(mt, new ArrayList<String>());
				}
				pathsByModelType.get(mt).add(cd.getFilePath());
			}
		}
		for (Map.Entry<String, List<String>> e : pathsByModelType.entrySet())
		{
			try
			{
				String[] k = e.getKey().split("\\|", -1);
				existingByModelType.put(e.getKey(), MMEDocumentBatchDAO.findExistingDocuments(e.getValue(), k[1], k[0]));
			}
			catch(Exception ex)
			{
				// looked up one by one below
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDocumentBatch() lookup", ex);
			}
		}

		List<ContentDetails> creates = new ArrayList<ContentDetails>();
		List<ContentDetails> updates = new ArrayList<ContentDetails>();
		List<ContentDetails> partialUpdates = new ArrayList<ContentDetails>();
		for (ContentDetails cd : part)
		{
			String docId = null;
			String contentId = null;
			String mt = tableSetKey(cd);
			Map<String, String[]> existing = existingByModelType.get(mt);
			if ("YES".equals(cd.getReplacePDFBYHTML()) || null == existing)
			{
				ContentDetails ex = "YES".equals(cd.getReplacePDFBYHTML())
						? mmeDocumentManagementDAO.getDocumentDetailsWD(cd.getFilePath(), cd.getModelType(), cd.getLocale())
						: mmeDocumentManagementDAO.getDocumentDetails(cd.getFilePath(), cd.getModelType(), cd.getLocale());
				if (null != ex)
				{
					docId = ex.getImDocumentId();
					contentId = ex.getImContentType();
				}
			}
			else
			{
				String[] row = existing.get(MMEDocumentBatchDAO.key(cd.getFilePath()));
				if (null != row)
				{
					docId = row[0];
					contentId = row[1];
				}
			}
			if (hasText(docId) && hasText(contentId))
			{
				cd.setImDocumentId(docId);
				cd.setImContentType(contentId);
				if (KaptureArticleBuilder.isPartialWiringDiagramUpdate(cd))
				{
					partialUpdates.add(cd);
				}
				else
				{
					updates.add(cd);
				}
			}
			else
			{
				creates.add(cd);
			}
		}
		logger.info("processDocumentBatch :: new=" + creates.size() + " existing=" + updates.size()
				+ " existing (partial wiring diagram update)=" + partialUpdates.size()
				+ " :: lookup " + (System.currentTimeMillis() - phaseStarted) + " ms");
		phaseStarted = System.currentTimeMillis();

		/*
		 * 2. KAPTURE - new documents in bulk creates, existing ones in bulk updates.
		 */
		List<KaptureContentService.BatchItem> createItems = articlesFor(creates, "CREATE_CONTENT", false);
		if (!createItems.isEmpty())
		{
			kapture.createBatch(createItems);
		}
		for (KaptureContentService.BatchItem it : createItems)
		{
			ContentDetails cd = (ContentDetails) it.owner;
			if (it.result.success)
			{
				logger.info("createContent :: Document created successfully for File {"+ cd.getFilePath() + "} :: Document Id :: > " + it.result.documentId);
				applyKaptureResult(cd, it.result);
			}
			else
			{
				cd.setImContentType(null);
				addKaptureError(it.result.errorCode, "FAILED TO GENERATE DOCUMENT IN KAPTURE. " + it.result.message, cd, "CREATE_CONTENT");
			}
		}

		List<KaptureContentService.BatchItem> updateItems = articlesFor(updates, "MODIFY_CONTENT", true);
		if (!updateItems.isEmpty())
		{
			kapture.updateBatch(updateItems);
		}
		List<ContentDetails> recreate = new ArrayList<ContentDetails>();
		for (KaptureContentService.BatchItem it : updateItems)
		{
			ContentDetails cd = (ContentDetails) it.owner;
			if (!it.result.success && KaptureContentService.ERR_NOT_FOUND.equals(it.result.errorCode))
			{
				/*
				 * THE DOCUMENT IS IN THE DATABASE BUT NOT IN KAPTURE.
				 * CREATE THE CONTENT AS A NEW DOCUMENT, DELETE THE PREVIOUS DOCUMENT ID DETAILS FROM
				 * DATABASE AND MAP THE NEW DOCUMENT ID WITH THE SOURCE LOCATION AS MASTER IDENTIFIER.
				 */
				logger.info("modifyContent :: Document not found in Kapture For Document Id :: >"+ cd.getImDocumentId());
				cd.setPreviousDocumentId(cd.getImDocumentId());
				recreate.add(cd);
			}
			else if (it.result.success)
			{
				logger.info("modifyContent :: Document modified successfully for File {"+ cd.getFilePath() + "} :: Document Id :: > " + it.result.documentId);
				applyKaptureResult(cd, it.result);
			}
			else
			{
				cd.setImContentType(null);
				addKaptureError(it.result.errorCode, "FAILED TO MODIFY DOCUMENT IN KAPTURE. " + it.result.message, cd, "MODIFY_CONTENT");
			}
		}
		// the documents Kapture no longer holds: the COMPLETE document, without an id
		List<KaptureContentService.BatchItem> recreateItems = articlesFor(recreate, "MODIFY_CONTENT", false);
		if (!recreateItems.isEmpty())
		{
			kapture.createBatch(recreateItems);
		}
		for (KaptureContentService.BatchItem it : recreateItems)
		{
			ContentDetails cd = (ContentDetails) it.owner;
			if (it.result.success)
			{
				logger.info("modifyContent :: Document modified successfully for File {"+ cd.getFilePath() + "} :: Document Id :: > " + it.result.documentId);
				applyKaptureResult(cd, it.result);
			}
			else
			{
				cd.setImContentType(null);
				addKaptureError(it.result.errorCode, "FAILED TO GENERATE DOCUMENT IN KAPTURE. " + it.result.message, cd, "MODIFY_CONTENT");
			}
		}
		// read - change - write of a single file's attributes: one document at a time
		for (ContentDetails cd : partialUpdates)
		{
			modifyContent(cd);
		}

		logger.info("processDocumentBatch :: Kapture writes done :: " + (System.currentTimeMillis() - phaseStarted) + " ms");
		phaseStarted = System.currentTimeMillis();

		/*
		 * 3. DATABASE - all details of the written documents in one transaction per kind.
		 */
		List<ContentDetails> savedNew = new ArrayList<ContentDetails>();
		List<ContentDetails> savedUpdate = new ArrayList<ContentDetails>();
		List<ContentDetails> savedReplacing = new ArrayList<ContentDetails>();
		Map<String, int[]> counts = new java.util.LinkedHashMap<String, int[]>();
		for (ContentDetails cd : part)
		{
			if (!hasText(cd.getImDocumentId()) || !hasText(cd.getImContentType()))
			{
				logger.info("performDocumentOperation :: Failed to write Document in Kapture for File :: > "+ cd.getFilePath());
				count(counts, cd, false);
			}
			else if (creates.contains(cd))
			{
				savedNew.add(cd);
			}
			else if (hasText(cd.getPreviousDocumentId()))
			{
				savedReplacing.add(cd);
			}
			else
			{
				savedUpdate.add(cd);
			}
		}
		if (!savedNew.isEmpty())
		{
			try
			{
				MMEDocumentBatchDAO.saveNewDocuments(savedNew);
				for (ContentDetails cd : savedNew)
				{
					logger.info("performDocumentOperation :: Document Details Saved Successfully in Database For Document Id :: "+ cd.getImDocumentId());
					count(counts, cd, true);
				}
			}
			catch(Exception e)
			{
				logger.info("processDocumentBatch :: the batch save failed - saving the " + savedNew.size() + " new documents one by one");
				for (ContentDetails cd : savedNew)
				{
					boolean flag = false;
					try
					{
						flag = mmeDocumentManagementDAO.saveDocumentDetails(cd);
					}
					catch(Exception ex)
					{
						Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDocumentBatch()", ex);
					}
					logger.info("performDocumentOperation :: Document Details " + (flag ? "Saved Successfully" : "could NOT be saved")
							+ " in Database For Document Id :: "+ cd.getImDocumentId());
					count(counts, cd, flag);
				}
			}
		}
		if (!savedUpdate.isEmpty())
		{
			try
			{
				MMEDocumentBatchDAO.updateDocuments(savedUpdate, true);
				for (ContentDetails cd : savedUpdate)
				{
					logger.info("performDocumentOperation :: Document Details Updated Successfully in Database For Document Id :: "+ cd.getImDocumentId());
					count(counts, cd, true);
				}
			}
			catch(Exception e)
			{
				logger.info("processDocumentBatch :: the batch update failed - updating the " + savedUpdate.size() + " documents one by one");
				for (ContentDetails cd : savedUpdate)
				{
					boolean flag = false;
					try
					{
						flag = mmeDocumentManagementDAO.updateDocumentDetails(cd, true);
					}
					catch(Exception ex)
					{
						Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDocumentBatch()", ex);
					}
					logger.info("performDocumentOperation :: Document Details " + (flag ? "Updated Successfully" : "could NOT be updated")
							+ " in Database For Document Id :: "+ cd.getImDocumentId());
					count(counts, cd, flag);
				}
			}
		}
		// a new document id replacing one Kapture no longer had - rare, one by one
		for (ContentDetails cd : savedReplacing)
		{
			boolean flag = false;
			try
			{
				flag = mmeDocumentManagementDAO.saveDocumentDetailsWhileUpdate(cd);
			}
			catch(Exception ex)
			{
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDocumentBatch()", ex);
			}
			removePreviousDocumentGetError(cd);
			cd.setPreviousDocumentId(null);
			logger.info("performDocumentOperation :: Document Details " + (flag ? "Updated Successfully" : "could NOT be updated")
					+ " in Database For Document Id :: "+ cd.getImDocumentId());
			count(counts, cd, flag);
		}

		int batchProcessed = 0;
		int batchFailed = 0;
		for (int[] c : counts.values())
		{
			batchProcessed += c[0];
			batchFailed += c[1];
		}
		logger.info("processDocumentBatch :: database saves done (new=" + savedNew.size() + " updated=" + savedUpdate.size()
				+ " replacing=" + savedReplacing.size() + ") :: " + (System.currentTimeMillis() - phaseStarted) + " ms");
		logger.info("processDocumentBatch :: RESULT :: documents=" + part.size() + " processed=" + batchProcessed
				+ " failed=" + batchFailed);

		/*
		 * 4. SCHEDULE AND ITEM COUNTS - once for the batch
		 */
		for (Map.Entry<String, int[]> e : counts.entrySet())
		{
			String[] ids = e.getKey().split("\\|", -1);
			MMEDocumentBatchDAO.addDocumentCounts(ids[0], ids[1], e.getValue()[0], e.getValue()[1]);
		}
	}

	/** Builds the Kapture article of each document; a document that cannot be built is reported here. */
	private List<KaptureContentService.BatchItem> articlesFor(List<ContentDetails> docs, String operationType, boolean update)
	{
		List<KaptureContentService.BatchItem> items = new ArrayList<KaptureContentService.BatchItem>();
		for (ContentDetails cd : docs)
		{
			try
			{
				cd.setOperationType(operationType);
				if(kaptureAvailable(cd, operationType)==false)
				{
					continue;
				}
				KaptureArticle article = KaptureArticleBuilder.build(cd, kaptureLocale);
				if (update)
				{
					article.documentId = cd.getImDocumentId();
				}
				// a re-created document (create while modifying) does not report its missing categories again
				article.categories = resolveCategories(cd, operationType, update || "CREATE_CONTENT".equals(operationType));
				items.add(new KaptureContentService.BatchItem(article, cd));
			}
			catch(Exception e)
			{
				cd.setImContentType(null);
				logger.info("articlesFor :: {" + operationType + "} :: Error :: >" + e.getMessage());
				capturreOtherErrorDetails(operationType, cd, e);
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
			}
		}
		return items;
	}

	private void count(Map<String, int[]> counts, ContentDetails cd, boolean processed)
	{
		// a document counted as failed (e.g. written to Kapture, but its database save failed) is not previewed
		if (!processed && null != preview)
		{
			preview.documentFailed(cd);
		}
		String key = (null == cd.getScheduleId() ? "" : cd.getScheduleId()) + "|" + (null == cd.getItemId() ? "" : cd.getItemId());
		int[] c = counts.get(key);
		if (null == c)
		{
			c = new int[2];
			counts.put(key, c);
		}
		c[processed ? 0 : 1]++;
	}

	private void performDocumentOperations(ContentDetails contentDetails, String scheduleId, boolean reProcessingDocument)
	{
		try
		{
			if(null!=contentDetails)
			{
				/*
				 * IDENTIFY DOCUMENT WHETEHR EXISTING OR NEW
				 * 	on the basis of Model Type, identify which database table to be look upon
				 * 	if NEWM - THEN NEW MODEL TABLES
				 * 	Else OTHER MODEL TABLES
				 */
				ContentDetails existingDetails = new ContentDetails();
				String fPath = contentDetails.getFilePath();
				if(null!=contentDetails.getReplacePDFBYHTML() && contentDetails.getReplacePDFBYHTML().equals("YES"))
				{
					// GET DOCUMENT EITHER WITH BOTH PDF OR HTML
					existingDetails = mmeDocumentManagementDAO.getDocumentDetailsWD(fPath, contentDetails.getModelType(),contentDetails.getLocale());
				}
				else
				{
					existingDetails = mmeDocumentManagementDAO.getDocumentDetails(fPath, contentDetails.getModelType(), contentDetails.getLocale());
				}
				if(null!=existingDetails && null!=existingDetails.getImDocumentId() && !"".equals(existingDetails.getImDocumentId()) && 
						null!=existingDetails.getImContentType() && !"".equals(existingDetails.getImContentType()))
				{
					/*
					 * PROCEED FOR UPDATE OF DOCUMENT
					 */
					contentDetails.setImDocumentId(existingDetails.getImDocumentId());
					contentDetails.setImContentType(existingDetails.getImContentType());
					
					contentDetails = modifyContent(contentDetails);
					if (null != contentDetails && null != contentDetails.getImDocumentId() 	&& !"".equals(contentDetails.getImDocumentId()) 
							&& null!=contentDetails.getImContentType() && !"".equals(contentDetails.getImContentType())) 
					{
						/*
						 * UPDATE CONTENT DETAILS IN DATABASE
						 * 
						 * CHECK HERE IF PREVIOUS DOCUMENT ID IS NOT NULL FOR THE DOCUMENT
						 * THEN IN THAT CASE, DELETE THE DATA FOR PREVIOUS DOC ID.
						 * INSERT THE DATA FOR NEW DOC ID.
						 */
						boolean flag=false;
						if(null!=contentDetails.getPreviousDocumentId() && !"".equals(contentDetails.getPreviousDocumentId()))
						{
							logger.info("performDocumentOperation :: Document Successfully generated in Info Manager with Document Id :: >"		+ contentDetails.getImDocumentId());
							/*
							 * DELETE OLD DOCUMENT DETAILS IN DATABASE
							 * 
							 * INSERT NEW DOCUMENT ID, ALSO INSERT THE DATA IN NEW TABLE MAPPING.
							 */
							flag = mmeDocumentManagementDAO.saveDocumentDetailsWhileUpdate(contentDetails);
							
							/*
							 * CHECK HERE IF THERES ANY ERROR FOR THE PREVIOUS DOCUMENT ID RELATED 
							 * TO GET CONTENT, PLEASE REMOVE IT FROM ERROR LIST
							 */
							removePreviousDocumentGetError(contentDetails);
							
							// explicitly set PREVIOUS DOCUMENT ID TO NULL
							contentDetails.setPreviousDocumentId(null);
						}
						else
						{
							logger.info("performDocumentOperation :: Document Successfully updated in Info Manager For Document Id :: >"+ contentDetails.getImDocumentId());
							
							/*
							 * UPDATE CONTENT DETAILS IN DATABASE
							 */
							flag = mmeDocumentManagementDAO.updateDocumentDetails(contentDetails, true);
						}
						
						if (flag == true) 
						{
							logger.info("performDocumentOperation :: Document Details Updated Successfully in Database For Document Id :: "+ contentDetails.getImDocumentId());
							/*
							 * now check here, if document Type is PDF, then move
							 * the PDF File to Live and Staging Directory
							 */
							if (contentDetails.getDocumentType().equals(ContentDetails.PDF_DOCUMENT)) 
							{
								if (null != contentDetails.getPdfFilePathAsAttachment() && !"".equals(contentDetails.getPdfFilePathAsAttachment())) 
								{
									File sourceFile = PathUtil.file(contentDetails.getPdfFilePathAsAttachment());
									if (sourceFile.isFile() && sourceFile.exists()) 
									{
										// Call function to move the PDF To Live and
										// Staging Folder
										// the PDF is uploaded to Kapture as the attachment of the document - nothing to copy
									}
								}
							}
							
							/*
							 * INCREMENT DOCUMENT PROCESSING COUNT BY 1 & ITEM DETAILS PROCESSING COUNT BY 1
							 */
							ScheduleDAO.updateProcessingCount(contentDetails.getScheduleId(), contentDetails.getItemId());
							
							
							// INNER LINKS OPERATION
//							addDocsToReUpdateListForInnerLinks(contentDetails);
						} 
						else 
						{
							logger.info("performDocumentOperation :: Failed to Update Document Details in Database For Document Id :: > "+ contentDetails.getImDocumentId());
							/*
							 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
							 */
							ScheduleDAO.updateFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
						}
					}
					else
					{
						logger.info("performDocumentOperation :: Failed to update Document in Info Manager for File :: > "+ contentDetails.getFilePath());
						/*
						 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
						 */
						ScheduleDAO.updateFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
					}
				}
				else
				{
					/*
					 * PROCEED FOR CREATION OF DOCUMENT
					 */
					contentDetails = createContent(contentDetails);
					/*
					 * Proceed for Saving the Details in Database.
					 */
					if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()) 
							&& null!=contentDetails.getImContentType() && !"".equals(contentDetails.getImContentType()))
					{
						logger.info("performDocumentOperation :: Document Successfully generated in Info Manager with Document Id :: >"		+ contentDetails.getImDocumentId());
						// call function to save Document Details in database.
						boolean flag = mmeDocumentManagementDAO.saveDocumentDetails(contentDetails);
						if(flag==true)
						{
							logger.info("performDocumentOperation :: Document Details Saved Successfully in Database For Document Id :: "	+ contentDetails.getImDocumentId());
							
							/*
							 * now check here, if document Type is PDF, then move
							 * the PDF File to Live and Staging Directory
							 */
							if (contentDetails.getDocumentType().equals(ContentDetails.PDF_DOCUMENT)) 
							{
								if (null != contentDetails.getPdfFilePathAsAttachment() && !"".equals(contentDetails.getPdfFilePathAsAttachment())) 
								{
									File sourceFile = PathUtil.file(contentDetails.getPdfFilePathAsAttachment());
									if (sourceFile.isFile() && sourceFile.exists()) 
									{
										// Call function to move the PDF To Live and
										// Staging Folder
										// the PDF is uploaded to Kapture as the attachment of the document - nothing to copy
									}
								}
							}
							
							/*
							 * INCREMENT DOCUMENT PROCESSING COUNT BY 1 & ITEM DETAILS PROCESSING COUNT BY 1
							 */
							ScheduleDAO.updateProcessingCount(contentDetails.getScheduleId(), contentDetails.getItemId());
						}
						else
						{
							logger.info("performDocumentOperation :: Failed to Save Document Details in Database For Document Id :: > "+ contentDetails.getImDocumentId());
							/*
							 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
							 */
							ScheduleDAO.updateFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
						}
					}
					else
					{
						logger.info("performDocumentOperation :: Failed to generate Document in Info Manager for File :: > "+ contentDetails.getFilePath());
						/*
						 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
						 */
						ScheduleDAO.updateFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
					}
				}
				
				/*
				 * ADD CONTENT DETAILS TO PROCESSING FILE DETAILS
				 */
				if(null==processingFileDetailsList || processingFileDetailsList.size()<=0)
				{
					processingFileDetailsList = new ArrayList<ContentDetails>();
				}
				processingFileDetailsList.add(contentDetails);
				
				fPath= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "performDocumentOperations()", e);
		}
	}
	
	/*
	 * =====================================================================================
	 * KAPTURE WRITE PATH
	 *
	 * Everything below replaces what this class used to do through the InfoManager client:
	 * create / modify / modify-for-inner-links, the category lookup and the session handling.
	 *
	 * A DOCUMENT IS NEVER PUBLISHED FROM HERE. Create and update always leave it unpublished;
	 * publishing is a separate job.
	 * =====================================================================================
	 */

	/**
	 * Opens the Kapture session for this job and checks the user groups and view every
	 * document is given. When this fails the job carries on, and every document is reported
	 * with the reason - the same as the documents failing one by one against InfoManager.
	 */
	private void connectToKapture()
	{
		try
		{
			// MME: the MME user groups and view, and the MME stylesheets kept by the HTML clean-up
			kapture = new KaptureContentService(ApplicationProperties.getProperty("kapture.usergroups.mme.refkeys"),
					ApplicationProperties.getProperty("kapture.views.mme.refkeys"), ConversionUtils::isConversionStyleLinkMME);
			kaptureUnavailableReason = null;
		}
		catch(Exception e)
		{
			kapture = null;
			kaptureUnavailableReason = e.getMessage();
			logger.info("connectToKapture :: KAPTURE IS NOT AVAILABLE :: >" + e.getMessage());
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "connectToKapture()", e);
		}
	}

	/** Records a failed Kapture operation for the reports. */
	private void addKaptureError(String errorCode, String errorMessage, ContentDetails contentDetails, String operationType)
	{
		logger.info("addKaptureError :: {" + operationType + "} :: " + errorCode + " :: " + errorMessage + " :: File :: >" + contentDetails.getFilePath());
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorCode(errorCode);
		errorDetails.setErrorMessage(errorMessage);
		errorDetails.setContentDetails(contentDetails);
		// add current time stamp
		errorDetails.setDateTime(new Timestamp(new Date().getTime()));
		errorDetails.setOperationType(operationType);
		if("DELETE_CONTENT".equals(operationType))
		{
			if(null==imErrorDeleteDetailsList || imErrorDeleteDetailsList.size()<=0)
			{
				imErrorDeleteDetailsList = new ArrayList<ErrorDetails>();
			}
			imErrorDeleteDetailsList.add(errorDetails);
		}
		else
		{
			if(null==imErrorDetailsList || imErrorDetailsList.size()<=0)
			{
				imErrorDetailsList = new ArrayList<ErrorDetails>();
			}
			imErrorDetailsList.add(errorDetails);
		}
		errorDetails = null;
	}

	/** false (and the reason recorded) when there is no Kapture session to write through. */
	private boolean kaptureAvailable(ContentDetails contentDetails, String operationType)
	{
		if(null!=kapture)
		{
			return true;
		}
		// Explicitly set CONTENT TYPE AS NULL
		contentDetails.setImContentType(null);
		addKaptureError("KAPEXP001", "KAPTURE IS NOT AVAILABLE. " + kaptureUnavailableReason, contentDetails, operationType);
		return false;
	}

	/**
	 * The categories of the document that exist in Kapture.
	 *
	 * Same rule as before: a category is looked up in the list resolved for the material
	 * folder; one that Kapture does not have is LEFT OUT of the document and, when
	 * reportMissing is true, added to the missing categories list (which fails the document
	 * in the reports and the schedule status).
	 */
	private List<KaptureArticle.Category> resolveCategories(ContentDetails contentDetails, String operationType, boolean reportMissing)
	{
		List<KaptureArticle.Category> categories = new ArrayList<KaptureArticle.Category>();
		if (null != contentDetails.getCategoryList() && contentDetails.getCategoryList().size() > 0)
		{
			CategoryDetails catDetails = null;
			CategoryDetails masterDetails = null;
			for (int i = 0; i < contentDetails.getCategoryList().size(); i++)
			{
				catDetails = (CategoryDetails) contentDetails.getCategoryList().get(i);
				/*
				 * LOOK UP THIS CATEGORY IN ALL CATEGORY LIST FOR MATERIAL FOLDER AND MAP ACCORDINGLY
				 */
				if(null!=catDetails.getCategoryRefKey() && !"".equals(catDetails.getCategoryRefKey()) &&
						null!=categoriesListForMaterialFolder && categoriesListForMaterialFolder.size()>0)
				{
					masterDetails = null;
					for(int b=0;b<categoriesListForMaterialFolder.size();b++)
					{
						masterDetails = (CategoryDetails)categoriesListForMaterialFolder.get(b);
						if(null!=masterDetails.getCategoryRefKey() && masterDetails.getCategoryRefKey().trim().toLowerCase().equals(catDetails.getCategoryRefKey().trim().toLowerCase()))
						{
							// CHECK IF CATEGORY FOUND IN KAPTURE
							if(null!=masterDetails.getKaptureRefKey() && !"".equals(masterDetails.getKaptureRefKey()))
							{
								categories.add(new KaptureArticle.Category(masterDetails.getKaptureRefKey(), masterDetails.getKaptureName(),
										"ESI_CATEGORY".equals(catDetails.getCategoryType())));
							}
							else if(reportMissing==true)
							{
								// add to Missing CategoiesList
								catDetails.setFilePath(contentDetails.getFilePath());
								// add operationType
								catDetails.setOperationType(operationType);
								// add ContentDetails
								catDetails.setContentDetails(contentDetails);
								// add errorCOdes
								catDetails.setErrorCodes(masterDetails.getErrorCodes());
								// add errorMessage
								catDetails.setErrorMessage(masterDetails.getErrorMessage());
								// add catDetails to missingCategoriesList
								if (null == missingCategoriesList || missingCategoriesList.size() <= 0)
								{
									missingCategoriesList = new ArrayList<CategoryDetails>();
								}
								missingCategoriesList.add(catDetails);
							}
							break;
						}
						masterDetails = null;
					}
				}
				catDetails = null;
			}
			catDetails= null;
			masterDetails = null;
		}
		return categories;
	}

	/** Copies what Kapture reported for a written document into the content VO. */
	private void applyKaptureResult(ContentDetails contentDetails, KaptureRecordResult result)
	{
		logger.info(" Document Id :: > "	+ result.documentId);
		logger.info(" Row Id :: > "			+ result.rowId);
		logger.info(" Version :: > "		+ result.articleVersion);
		logger.info(" Doc Status :: > "		+ result.articleState);

		// written, but incomplete (a new document whose attachments could not be added, or a document Kapture already held whose update failed): report it
		if (null != result.warning)
		{
			addKaptureError(null != result.warningCode ? result.warningCode : KaptureContentService.ERR_ATTACHMENT, result.warning, contentDetails, contentDetails.getOperationType());
		}

		/*
		 * Update the Content VO
		 */
		contentDetails.setImDocumentId(result.documentId);
		// the row id of the written version; the document id stands in when Kapture returns none
		contentDetails.setImContentType(null!=result.rowId && !"".equals(result.rowId) ? result.rowId : result.documentId);

		// SET REPORT UPDATED VERSION
		contentDetails.setReportUdatedVersion(result.articleVersion);

		if (kapture.isPublishedState(result.articleState))
		{
			// SET PUBLISH
			contentDetails.setImDocStatus(ApplicationProperties.getProperty("flag.value.publish"));
		}
		else
		{
			// SET UN-PUBLISH
			contentDetails.setImDocStatus(ApplicationProperties.getProperty("flag.value.draft"));
		}

		contentDetails.setImVersion(result.articleVersion);
		// Kapture holds the attachments itself - there is no resource path to copy a PDF into
		contentDetails.setImResourcePath("");

		// the offline preview follows every write: a written document is (re)added, an unpublished one leaves
		if (null != preview)
		{
			if ("DELETE_CONTENT".equals(contentDetails.getOperationType()))
			{
				preview.documentUnpublished(contentDetails);
			}
			else if (null != result.warning)
			{
				// written but incomplete - reported as a failure, so not offered for preview / publish
				preview.documentFailed(contentDetails);
			}
			else
			{
				preview.documentWritten(contentDetails, kaptureLocale);
			}
		}
	}

	/**
	 * Function will perform Create Content Operation in Kapture
	 * @param contentDetails
	 * @return
	 */
	public ContentDetails createContent(ContentDetails contentDetails)
	{
		String operationType="CREATE_CONTENT";
		try
		{
			contentDetails.setOperationType(operationType);
			if(kaptureAvailable(contentDetails, operationType)==false)
			{
				return contentDetails;
			}

			KaptureArticle article = KaptureArticleBuilder.build(contentDetails, kaptureLocale);
			article.categories = resolveCategories(contentDetails, operationType, true);

			// NEW DOCUMENT - NO ID IS SENT, KAPTURE ASSIGNS ONE AND RETURNS IT
			KaptureRecordResult result = kapture.create(article);
			if (result.success)
			{
				logger.info("createContent :: Document created successfully for File {"+ contentDetails.getFilePath() + "} :: Document Id :: > " + result.documentId);
				applyKaptureResult(contentDetails, result);
			}
			else
			{
				// explicity set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);
				logger.info("createContent :: Failed to Generate Document for File :: >"+ contentDetails.getFilePath());
				addKaptureError(result.errorCode, "FAILED TO GENERATE DOCUMENT IN KAPTURE. " + result.message, contentDetails, operationType);
			}
			result = null;
			article = null;
		}
		catch (Exception e)
		{
			// Explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			logger.info("createContent :: Error :: >" + e.getMessage());
			capturreOtherErrorDetails(operationType, contentDetails, e);
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
		}
		return contentDetails;
	}

	/**
	 * Function will perform Modify Content Operation in Kapture
	 * @param contentDetails
	 * @return
	 */
	public ContentDetails modifyContent(ContentDetails contentDetails)
	{
		String operationType="MODIFY_CONTENT";
		try
		{
			contentDetails.setOperationType(operationType);
			if(kaptureAvailable(contentDetails, operationType)==false)
			{
				return contentDetails;
			}

			KaptureArticle article = KaptureArticleBuilder.build(contentDetails, kaptureLocale);
			article.documentId = contentDetails.getImDocumentId();
			article.categories = resolveCategories(contentDetails, operationType, true);

			KaptureRecordResult result = updateInKapture(article, contentDetails);
			if (null!=result && !result.success && KaptureContentService.ERR_NOT_FOUND.equals(result.errorCode))
			{
				logger.info("modifyContent :: Document not found in Kapture For Document Id :: >"+ contentDetails.getImDocumentId());
				/*
				 * THE DOCUMENT IS IN THE DATABASE BUT NOT IN KAPTURE.
				 * CREATE THE CONTENT AS A NEW DOCUMENT, DELETE THE PREVIOUS DOCUMENT ID DETAILS FROM
				 * DATABASE AND MAP THE NEW DOCUMENT ID WITH THE SOURCE LOCATION AS MASTER IDENTIFIER.
				 */
				// SET PREVIOUS DOCUMENT ID
				contentDetails.setPreviousDocumentId(contentDetails.getImDocumentId());

				// the COMPLETE document, without an id
				article = KaptureArticleBuilder.build(contentDetails, kaptureLocale);
				article.categories = resolveCategories(contentDetails, operationType, false);
				result = kapture.create(article);
				if (!result.success)
				{
					// Explicitly set CONTENT TYPE AS NULL
					contentDetails.setImContentType(null);
					logger.info("modifyContent :: Failed to Generate Document for File :: >"+ contentDetails.getFilePath());
					addKaptureError(result.errorCode, "FAILED TO GENERATE DOCUMENT IN KAPTURE. " + result.message, contentDetails, operationType);
					return contentDetails;
				}
			}

			if (null!=result && result.success)
			{
				logger.info("modifyContent :: Document modified successfully for File {"+ contentDetails.getFilePath()	+ "} :: Document Id :: > "	+ result.documentId);
				applyKaptureResult(contentDetails, result);
			}
			else if(null!=result)
			{
				// Explicitly set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);
				logger.info("modifyContent :: Failed to Modify Document for File :: >" 	+ contentDetails.getFilePath());
				addKaptureError(result.errorCode, "FAILED TO MODIFY DOCUMENT IN KAPTURE. " + result.message, contentDetails, operationType);
			}
			result = null;
			article = null;
		}
		catch (Exception e)
		{
			// Explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			logger.info("modifyContent :: Error :: >" + e.getMessage());
			capturreOtherErrorDetails(operationType, contentDetails, e);
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
		}
		return contentDetails;
	}

	/**
	 * Sends the update of one document.
	 *
	 * A WIRING DIAGRAM fed by a PDF and an HTML file changes only that file's attributes: the
	 * rest is read from Kapture and sent back unchanged, because an update rewrites the whole
	 * document. If the document cannot be read it is FAILED, never sent with those attributes
	 * blank.
	 */
	private KaptureRecordResult updateInKapture(KaptureArticle article, ContentDetails contentDetails)
	{
		if(KaptureArticleBuilder.isPartialWiringDiagramUpdate(contentDetails))
		{
			if(null==KaptureLookupDAO.getArticleIdentity(article.documentId, article.locale))
			{
				return KaptureRecordResult.failure(KaptureContentService.ERR_NOT_FOUND,
						"Document " + article.documentId + " (" + article.locale + ") is not present in Kapture");
			}
			JsonObject existing = kapture.readFields(article.documentId, article.channelType, article.locale);
			if(null==existing)
			{
				return KaptureRecordResult.failure("KAPEXP002", "FAILED TO READ DOCUMENT {"+article.documentId+"} FROM KAPTURE.");
			}
			KaptureArticleBuilder.overlayPartialWiringDiagramUpdate(article, contentDetails, existing);
		}
		return kapture.update(article);
	}

	/**
	 * Function will perform Modify Content Operation in Kapture for the Inner Links
	 * @param contentDetails
	 * @return
	 */
	public ContentDetails modifyContentForInnerLinks(ContentDetails contentDetails)
	{
		String operationType="MODIFY_CONTENT_INNERLINKS";
		try
		{
			if(kaptureAvailable(contentDetails, operationType)==false)
			{
				return contentDetails;
			}

			KaptureArticle article = KaptureArticleBuilder.build(contentDetails, kaptureLocale);
			article.documentId = contentDetails.getImDocumentId();
			// the document keeps its categories; missing ones were already reported when it was written
			article.categories = resolveCategories(contentDetails, operationType, false);

			KaptureRecordResult result = updateInKapture(article, contentDetails);
			if (result.success)
			{
				logger.info("modifyContentForInnerLinks :: Document modified successfully for File {"+ contentDetails.getFilePath()	+ "} :: Document Id :: > "	+ result.documentId);
				applyKaptureResult(contentDetails, result);
			}
			else if(KaptureContentService.ERR_NOT_FOUND.equals(result.errorCode))
			{
				logger.info("modifyContentForInnerLinks :: Failed to Fetch Content from Kapture For Document Id :: >"+ contentDetails.getImDocumentId());
				// explicitly set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);

				/*
				 * SET INNER LINKS DETAILS
				 */
				contentDetails.setInnerLinkFound("YES");
				contentDetails.setAllInnerLinksMapped("NO");
				contentDetails.setInnerLinkMappingReason("FAILED TO FETCH {"+contentDetails.getImDocumentId()+"} FROM KAPTURE.");
			}
			else
			{
				// Explicitly set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);
				logger.info("modifyContentForInnerLinks :: Failed to Modify Document for File :: >" 	+ contentDetails.getFilePath());
				addKaptureError(result.errorCode, "FAILED TO MODIFY DOCUMENT IN KAPTURE. " + result.message, contentDetails, operationType);
			}
			result = null;
			article = null;
		}
		catch (Exception e)
		{
			// explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return contentDetails;
	}

	/**
	 * Looks every category of the material folder up in Kapture in ONE query and records, per
	 * reference key, whether it exists there for the locale being processed.
	 */
	private void locateCategoriesInKapture(List<String> uniqueCategoriesList)
	{
		Map<String, String[]> found = null;
		String lookupError = null;
		try
		{
			found = KaptureLookupDAO.getCategories(uniqueCategoriesList, kaptureLocale);
		}
		catch(Exception e)
		{
			// A FAILED LOOKUP IS REPORTED AS SUCH - IT IS NOT "CATEGORY MISSING"
			lookupError = "KAPTURE CATEGORY LOOKUP FAILED. " + e.getMessage() + "\n";
			logger.info("locateCategoriesInKapture :: Error :: >" + e.getMessage());
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "locateCategoriesInKapture()", e);
		}

		CategoryDetails catDetails = null;
		for(int a=0;a<uniqueCategoriesList.size();a++)
		{
			catDetails = new CategoryDetails();
			catDetails.setCategoryRefKey(uniqueCategoriesList.get(a).toString());
			String[] kaptureCategory = (null==found) ? null : found.get(catDetails.getCategoryRefKey().trim().toUpperCase());
			if(null!=kaptureCategory)
			{
				catDetails.setKaptureRefKey(kaptureCategory[0]);
				catDetails.setKaptureName(kaptureCategory[1]);
			}
			else
			{
				catDetails.setErrorMessage(null!=lookupError ? lookupError : "CATEGORY NOT FOUND IN KAPTURE FOR LOCALE {"+kaptureLocale+"}.\n");
				logger.info("locateCategoriesInKapture :: {"+catDetails.getCategoryRefKey()+"} :: " + catDetails.getErrorMessage().trim());
			}
			if(null==categoriesListForMaterialFolder || categoriesListForMaterialFolder.size()<0)
			{
				categoriesListForMaterialFolder = new ArrayList<CategoryDetails>();
			}
			categoriesListForMaterialFolder.add(catDetails);
			catDetails = null;
		}
	}
	/**
	 * Function will CAPTURE OTHER EXCEPTION DETAILS
	 * @param operationType
	 * @param contentDetails
	 * @param e
	 */
	private void capturreOtherErrorDetails(String operationType, ContentDetails contentDetails, Exception e) 
	{
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		e.printStackTrace(print);
		String errorCode = e.getMessage();
		String errorMessage = writer.toString();

		logger.info("captureOtherError :: {" + operationType+ "} :: Error Code :: >" + errorCode);
		logger.info("captureOtherError :: {" + operationType	+ "} :: Error Message :: >" + errorMessage);

		ErrorDetails erDetails = new ErrorDetails();
		erDetails.setErrorCode(String.valueOf(errorCode));
		erDetails.setErrorMessage(errorMessage);
		erDetails.setOperationType(operationType);
		erDetails.setContentDetails(contentDetails);
		// add current time stamp
		erDetails.setDateTime(new Timestamp(new Date().getTime()));

		if(null!=operationType && !"".equals(operationType))
		{
			// add erDetails to imErrorDetailsList
			if (null == imErrorDetailsList 	|| imErrorDetailsList.size() <= 0) 
			{
				imErrorDetailsList = new ArrayList<ErrorDetails>();
			}
			imErrorDetailsList.add(erDetails);
		}
		else
		{
			// add erDetails to imErrorDetailsList
			if (null == imErrorDetailsList 	|| imErrorDetailsList.size() <= 0) 
			{
				imErrorDetailsList = new ArrayList<ErrorDetails>();
			}
			imErrorDetailsList.add(erDetails);
		}
		erDetails = null;

		// set errorCode to null
		errorCode = null;
		// set errorMessage to null
		errorMessage = null;
		// set writer & print to null
		writer = null;
		// set print to null
		print = null;
	}
	
	/**
	 * Function will remove the GET CONTENT ERROR DETAILS FOR THE PREVIOUS DOCUMENT ID
	 * IN THE SCENARIO, WHEN DOCUMENT FOUND IN DATABASE BUT NOT IN IM. SO A NEW DOC 
	 * WIL BE GENERATED AND ALL THE REFERENCE FOR THE PREVIOUS DOC WILL BE REMOVED.
	 * @param contentDetails
	 */
	private void removePreviousDocumentGetError(ContentDetails contentDetails)
	{
		try
		{
			if(null!=imErrorDetailsList && imErrorDetailsList.size()>0)
			{

				String threadId = contentDetails.getThreadId();
				String pathToCheck=contentDetails.getFilePath();
				if(null!=threadId && !"".equals(threadId) && null!=pathToCheck && !"".equals(pathToCheck))
				{
					for(int f=0;f<imErrorDetailsList.size();f++)
					{
						ErrorDetails eDetails = (ErrorDetails)imErrorDetailsList.get(f);
						if(null!=eDetails.getContentDetails() && null!=eDetails.getContentDetails().getThreadId() 
								&& !"".equals(eDetails.getContentDetails().getThreadId()))
						{
							if(threadId.trim().toLowerCase().equals(eDetails.getContentDetails().getThreadId().trim().toLowerCase()))
							{
								if(null!=eDetails.getOperationType() && !"".equals(eDetails.getOperationType()) && 
										eDetails.getOperationType().trim().toLowerCase().equals("get_content"))
								{
									if(null!=eDetails.getContentDetails().getFilePath() && 
											!"".equals(eDetails.getContentDetails().getFilePath()))
									{
										String pathToBeVerified=eDetails.getContentDetails().getFilePath();
										if(null!=pathToBeVerified && !"".equals(pathToBeVerified))
										{
											if(pathToBeVerified.trim().toLowerCase().equals(pathToCheck.trim().toLowerCase()))
											{
												// remove the error
												imErrorDetailsList.remove(f);
												f--;
											}
										}
										pathToBeVerified = null;
									}
								}
							}
						}
					}
				}
				threadId = null;
				pathToCheck=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "removePreviousDocumentGetError()", e);
		}
	}

	/**
	 * Function will COPY WD HTML FOLDER WITH CONN FOLDER TO OKASSETS
	 * @param htmlFolder
	 * @param localeFolderName
	 * @param channelFolderName
	 * @param modelFolderName
	 * @param yearFolderName
	 * @param folderType
	 */
	private void xCopyCompleteDirectory(File htmlFolder, String localeFolderName, String channelFolderName, String modelFolderName,String manualTypeFolderName,String faceLiftFolderName,
				String materialFolderName, String folderType, ContentDetails xcopyContentDetails) 
	{
		try 
		{
			File[] htmlFilesList = htmlFolder.listFiles();
			if (null != htmlFilesList && htmlFilesList.length > 0) 
			{
				for (int a = 0; a < htmlFilesList.length; a++) 
				{
					File sourceFile = (File) htmlFilesList[a];
					if (sourceFile.exists()) 
					{
						if (sourceFile.isFile()) 
						{
							if(!sourceFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
							{
								xcopyUtils.copyFilesToServer(sourceFile,localeFolderName, channelFolderName,modelFolderName, manualTypeFolderName , faceLiftFolderName, 
										materialFolderName, folderType, xcopyContentDetails,wiringDiagramUtils);
							}
						} 
						else if (sourceFile.isDirectory()) 
						{
							// call recursive function
							xCopyCompleteDirectory(sourceFile, localeFolderName, channelFolderName, modelFolderName, manualTypeFolderName, faceLiftFolderName, 
									materialFolderName, folderType, xcopyContentDetails);
						}
					}
					sourceFile = null;
				}
			}
		} catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "xCopyCompleteDirectory()", e);
		}
	}
	
	
	/**
	 * Function will proceed for generating reports.
	 * 
	 * @param wslId
	 * @param scheduleCode
	 */
	private void printReports(String scheduleCode) 
	{
		try 
		{
			/*
			 * SET CURRENT PROCESING STATUS OF SCHEDULE TO REPORTS GENERATION
			 */
			ScheduleDAO.updateCurrentJobStatus(scheduleCode, null, "REPORTS GENERATION");
			/*
			 * now check for StartMMEConversionImpl.processingFileDetailsList
			 * is not null and check for each of the filePath, if does not
			 * exists in - StartMMEConversionImpl.imErrorDetailsList -
			 * DocumentManagementDAO.failedDatabaseSaveDocumentDetails then Status
			 * as - SUCCESS else Status as - FAILED
			 */

			if (null != processingFileDetailsList 	&& processingFileDetailsList.size() > 0) 
			{
				for (int a = 0; a < processingFileDetailsList.size(); a++) 
				{
					ContentDetails con = (ContentDetails) processingFileDetailsList.get(a);

					// set TRANSACTION STATUS AS SUCCESS FOR ALL THE DOCUMENTS
//					con.setProcessingOperationStatus("SUCCESS");
//					con.setReportIMStatus("SUCCESS");
//					con.setReporDBStatus("SUCCESS");
//					con.setReportCategoryMappingStatus("SUCCESS");
//					if(null!=con.getInnerLinksList() && con.getInnerLinksList().size()>0)
//					{
//						con.setReportInnerLinkMappingStatus("SUCCESS");
//					}
					boolean errorFound= false;
					con.setErrorComments("");
					/*
					 * CHECK HERE IF DOCUMENT EXISTS IN INFOMANAGER. IMFAILURE LIST
					 * SET STATUS AS FAILURE AND UPDATE REMARKS AS - FAILED WHILE PERFORMING INFO_MANAGER OPERATION. PLEASE REFER TO FAILURE REPORT.
					 */
					StringBuilder errorCommentsBuilder = new StringBuilder();
					if (null != imErrorDetailsList 	&& imErrorDetailsList.size() > 0) 
					{
						for (int b = 0; b < imErrorDetailsList.size(); b++) 
						{
							ErrorDetails errorVO = (ErrorDetails) imErrorDetailsList.get(b);
							if (null != errorVO.getContentDetails() && null != errorVO.getContentDetails().getFilePath() 
									&& !"".equals(errorVO.getContentDetails().getFilePath())) 
							{
								if(errorVO.getContentDetails().getFilePath().equals(con.getFilePath()))
								{
									// file Matches
									// SET DOCUMENT PROCESSING STATUS AS FAILURE
//									con.setProcessingOperationStatus("FAILURE");
//									con.setReportIMStatus("FAILURE");
									errorFound=true;
									// UPDATE ERROR COMMENTS
									errorCommentsBuilder.append("FAILED WHILE PERFORMING KAPTURE OPERATION. PLEASE REFER TO FAILURE REPORT.");
									errorCommentsBuilder.append("\n");
									break;
								}
							}
						}
					}


					/*
					 * Failure List 
					 * SET STATUS AS FAILURE
					 * UPDATE REMARKS AS - FAILED WHILE PERFORMING DATABASE OPERATION. PLEASE REFER TO FAILURE REPORT.
					 */
					if (null != mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails() 
							&& mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size() > 0) 
					{
						for (int b = 0; b < mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size(); b++) 
						{
							Map<Object, Object> dataMap = (HashMap<Object, Object>) mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().get(b);
							if (null != dataMap && null != dataMap.get("DOCUMENT_LOCATION")	&& !"".equals(dataMap.get("DOCUMENT_LOCATION"))) 
							{
								// CHECK HERE CONTAINS
								if (dataMap.get("DOCUMENT_LOCATION").equals(con.getFilePath())) 
								{
									// file matches =
									// SET DOCUMENT PROCESSING STATUS AS FAILURE
//									con.setProcessingOperationStatus("FAILURE");
//									con.setReporDBStatus("FAIURE");
									errorFound=true;
									// UPDATE ERROR COMMENTS
									errorCommentsBuilder.append("FAILED WHILE PERFORMING DATABASE OPERATION. PLEASE REFER TO FAILURE REPORT.");
									errorCommentsBuilder.append("\n");
									break;
								}
							}
						}
					}

					/*
					 * ALSO CHECK HERE, IF DOCUMENT EXISTS IN MISSING CATEGORIES REPORT -
					 * THEN SET THE DOCUMENT AS FAILURE, UPDATE THE ERROR MESSAGES
					 * FAILED WHILE PERFORMING CATEGORY MAPPING. PLEASE REFER TO MISSING CATEGORIES REPORT.
					 */
					if(null!=missingCategoriesList && missingCategoriesList.size()>0)
					{
						for(int j=0;j<missingCategoriesList.size();j++)
						{
							CategoryDetails catDetails = (CategoryDetails)missingCategoriesList.get(j);
							if(null!=catDetails.getOperationType() && !catDetails.getOperationType().equals("DELETE_CONTENT"))
							{
								if(null!=catDetails.getFilePath() && !"".equals(catDetails.getFilePath()))
								{
									if(catDetails.getFilePath().contains(con.getFilePath()))
									{
										// DOC EXISTS IN MISSING CAT REPORT
										// SET DOCUMENT PROCESSING STATUS AS FAILURE
										//									con.setProcessingOperationStatus("FAILURE");
										//									con.setReportCategoryMappingStatus("FAIURE");
										errorFound=true;
										// UPDATE ERROR COMMENTS
										errorCommentsBuilder.append("FAILED WHILE PERFORMING CATEGORY MAPPING. PLEASE REFER TO MISSING CATEGORIES REPORT.");
										break;
									}
								}
							}
						}
					}

					if(errorFound==true)
					{
						con.setProcessingOperationStatus("FAILURE");
					}
					else
					{
						// check here, IM DOCUMENT ID & CONTENT TYPE MUST NOT BE NULL
						if(null!=con.getImDocumentId() && !"".equals(con.getImDocumentId()) && null!=con.getImContentType() && !"".equals(con.getImContentType()))
						{
							con.setProcessingOperationStatus("SUCCESS");
						}
					}
					
					// add errorComments
					if(null!=errorCommentsBuilder)
					{
						con.setErrorComments(errorCommentsBuilder.toString());
					}
					errorCommentsBuilder = null;
				}
			}
			
			/*
			 * UPDATE IM DOCUMENT ID IN OTHER INNER LINKS LIST
			 */
			if(null!=processingFileDetailsList && processingFileDetailsList.size()>0 && null!=otherInnerLinksList && otherInnerLinksList.size()>0)
			{
				for(int t=0;t<otherInnerLinksList.size();t++)
				{
					ContentDetails conDetails = (ContentDetails)otherInnerLinksList.get(t);
					if(null!=conDetails.getFilePath() && !"".equals(conDetails.getFilePath()))
					{
						for(int a=0;a<processingFileDetailsList.size();a++)
						{
							ContentDetails data = (ContentDetails)processingFileDetailsList.get(a);
							if(data.getFilePath().trim().toLowerCase().equals(conDetails.getFilePath().trim().toLowerCase()))
							{
								conDetails.setImDocumentId(data.getImDocumentId());
								break;
							}
							data = null;
						}
					}
					conDetails = null;
				}
			}
			
			/*
			 * UPDATE IM DOCUMENT ID IN MISSING CATEGORY LIST
			 */
			if(null!=processingFileDetailsList && processingFileDetailsList.size()>0 && null!=missingCategoriesList && missingCategoriesList.size()>0)
			{
				for(int t=0;t<missingCategoriesList.size();t++)
				{
					CategoryDetails catDetails = (CategoryDetails)missingCategoriesList.get(t);
					if(null!=catDetails.getOperationType() && !catDetails.getOperationType().equals("DELETE_CONTENT"))
					{
						for(int a=0;a<processingFileDetailsList.size();a++)
						{
							ContentDetails data = (ContentDetails)processingFileDetailsList.get(a);
							if(data.getFilePath().trim().toLowerCase().equals(catDetails.getFilePath().trim().toLowerCase()))
							{
								if(null!=catDetails.getContentDetails())
								{
									catDetails.getContentDetails().setImDocumentId(data.getImDocumentId());
								}
								break;
							}
						}
					}
				}
			}
			
			/*
			 * NOW CHECK HERE IF XCOPY PROCESSING LIST IS NOT NULL
			 * THEN SET STATUS AS SUCCESS FOR IT. AND ALSO CHECK
			 * IF IT EXISTS IN FAILURE LIST - SET STATUS AS FAILURE
			 * AND SET ERROR COMMENTS - FAILED WHILE PERFORMING XCOPY OPERATION. PLEASE REFER TO FAILURE REPORT.
			 */
			if(null!=xcopyUtils.getFileProcessingList() && xcopyUtils.getFileProcessingList().size()>0)
			{
				for(int a=0;a<xcopyUtils.getFileProcessingList().size();a++)
				{
					Map<Object, Object> dataMap = (HashMap<Object, Object>)xcopyUtils.getFileProcessingList().get(a);
					if(null!=dataMap.get("SOURCE_PATH") && !"".equals(dataMap.get("SOURCE_PATH")))
					{
						// SET STATUS AS SUCCESS
						dataMap.put("PROCESSING_STATUS", "SUCCESS");
						dataMap.put("OPERATION_TYPE", "XCOPY OPERATION");
						/*
						 * now check for the file in failureList
						 */
						if(null!=xcopyUtils.getFileUploadFailureList() && xcopyUtils.getFileUploadFailureList().size()>0)
						{
							for(int b=0;b<xcopyUtils.getFileUploadFailureList().size();b++)
							{
								Map<Object, Object> errorMap = (HashMap<Object, Object>)xcopyUtils.getFileUploadFailureList().get(b);
								if(null!=errorMap.get("SOURCE_PATH") && !"".equals(errorMap.get("SOURCE_PATH")))
								{
									errorMap.put("OPERATION_TYPE", "XCOPY OPERATION");
									if(errorMap.get("SOURCE_PATH").equals(dataMap.get("SOURCE_PATH")))
									{
										// set processingStatus as FAILURE - UPDATE COMMENTS
										dataMap.put("PROCESSING_STATUS", "FAILURE");
										dataMap.put("ERROR_COMMENTS", "FAILED WHILE PERFORMING XCOPY OPERATION. PLEASE REFER TO FAILURE REPORT.");
										break;
									}
								}
							}
						}
					}
				}
			}
			
			boolean innerLinksPassed = true;
			/*
			 * IF IN PROCESSING FILE LIST, 
				APPLICABLE ONLY FOR DOCUMENTS WITH INNER LINK FOUND = YES
				FOR THESE DOCUMENTS , IF ANY HAS ALL LINKS MAPPED TO NO
				THEN INNER LINKS PASSED = FALSE;
			 */
			if(null!=processingFileDetailsList && processingFileDetailsList.size()>0)
			{
				for(int a=0;a<processingFileDetailsList.size();a++)
				{
					ContentDetails con = (ContentDetails)processingFileDetailsList.get(a);
					if(null!=con.getInnerLinkFound() && con.getInnerLinkFound().trim().toLowerCase().equals("yes"))
					{
						if(null!=con.getAllInnerLinksMapped() && con.getAllInnerLinksMapped().trim().toLowerCase().equals("no"))
						{
							innerLinksPassed = false;
							break;
						}
					}
				}
			}
			
			
			
			/*
			 * CHECK FOR DISPLAY ORDER, AS WELL IF FOR ANY OF THE ROW, PROCESSING STATUS IS NULL OR FAILURE THEN DISPLAY ORDER PASSED AS FALSE.
			 */
			boolean displayOrderPassed = true;
			if(null!=finalDisplayOrderList && finalDisplayOrderList.size()>0)
			{
				// CHECK FOR PROCESSING STATUS- NULL "" OR FAILURE
				for(DisplayOrderDetails details : finalDisplayOrderList)
				{
					if(null==details.getProcessingStatus() || "".equals(details.getProcessingStatus()) || "FAILURE".equals(details.getProcessingStatus()))
					{
						displayOrderPassed = false;
						break;
					}
				}
			}
			
			/*
			 * CHECK FOR CD PROCESSING, AS WELL IF FOR ANY OF THE ROW, PROCESSING STATUS IS NULL OR FAILURE THEN CD DATA PASSED AS FALSE.
			 */
			boolean cdProcessingPassed = true;
			if(null!=finalCDProcessingList && finalCDProcessingList.size()>0)
			{
				// CHECK FOR PROCESSING STATUS- NULL "" OR FAILURE
				for(CDProcessingDetails details : finalCDProcessingList)
				{
					if(null==details.getProcessingStatus() || "".equals(details.getProcessingStatus()) || "FAILURE".equals(details.getProcessingStatus()))
					{
						cdProcessingPassed = false;
						break;
					}
				}
			}
			
			if(null!=mmeDocumentManagementDAO.getViewContentDataList() && mmeDocumentManagementDAO.getViewContentDataList().size()>0)
			{
				// ITERATE DATABASE FAILURE LIST AND IDENTIFY IT DOCUMENT IS FAILED, PUT STATUS AS FAILED AND UPDATE MESSAGE AS PLEASE REFER FAILURE REPORT.
				for(MCViewContentDetails details : mmeDocumentManagementDAO.getViewContentDataList())
				{
					details.setProcessingStatus("SUCCESS");
					details.setRemarks("");
					/*
					 * CHECK HERE IF DISPLAY ORDER NAMES ARE NULL IN VIEW CONTENT LIST
					 * SET IT TO FAILURE
					 * ADD REMAKRS - FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES.
					 */
					
					boolean namesFound = true;
					// Start with Level 1
					// Display order CODES are no longer supplied - the file carries the NAMES themselves,
					// so there is no code left to find a name for and nothing to validate here.
					
					if(namesFound==false)
					{
						// set PROCESSING STATUS AS FAILURE
						details.setProcessingStatus("FAILURE");
						details.setRemarks("FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES.");
					}
					
					if(null!=mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails() && mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size()>0)
					{
						for (int b = 0; b < mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size(); b++) 
						{
							Map<Object, Object> dataMap = (HashMap<Object, Object>) mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().get(b);
							if (null != dataMap && null != dataMap.get("IM_DOCUMENT_ID")	&& !"".equals(dataMap.get("IM_DOCUMENT_ID"))) 
							{
								// CHECK HERE CONTAINS
								if (dataMap.get("IM_DOCUMENT_ID").equals(details.getDocumentId())) 
								{
									// SET PROCESSING STATUS AS FAILURE
									details.setProcessingStatus("FAILURE");
									// UPDATE REMARKS COMMENTS
									String remarks="FAILED WHILE PERFORMING VIEW COTNENT OPERATION. PLEASE REFER TO FAILURE REPORT";
									if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
									{
										remarks=details.getRemarks()+"\n"+remarks;
									}
									details.setRemarks(remarks);
									remarks = null;
									break;
								}
							}
						}
					}
				}
			}
			
			/*
			 * IF PROCESSING DELETE FILE LIST IS NOT NULL = 
			 * UPDATE THE STATUS FAILURE / SUCCESS
			 */
			if(null!=processingDeleteFileDetailsList && processingDeleteFileDetailsList.size()>0)
			{
				for (int a = 0; a < processingDeleteFileDetailsList.size(); a++) 
				{
					ContentDetails con = (ContentDetails) processingDeleteFileDetailsList.get(a);

					boolean errorFound= false;
					con.setErrorComments("");
					
					/*
					 * CHECK HERE IF DOCUMENT EXISTS IN INFOMANAGER. IMFAILURE LIST
					 * SET STATUS AS FAILURE AND UPDATE REMARKS AS - FAILED WHILE PERFORMING INFO_MANAGER OPERATION. PLEASE REFER TO FAILURE REPORT.
					 */
					StringBuilder errorCommentsBuilder = new StringBuilder();
					if (null != imErrorDeleteDetailsList 	&& imErrorDeleteDetailsList.size() > 0) 
					{
						for (int b = 0; b < imErrorDeleteDetailsList.size(); b++) 
						{
							ErrorDetails errorVO = (ErrorDetails) imErrorDeleteDetailsList.get(b);
							if (null != errorVO.getContentDetails() && null != errorVO.getContentDetails().getFilePath() 
									&& !"".equals(errorVO.getContentDetails().getFilePath())) 
							{
								if(errorVO.getContentDetails().getFilePath().equals(con.getFilePath()))
								{
									// file Matches
									// SET DOCUMENT PROCESSING STATUS AS FAILURE
//									con.setProcessingOperationStatus("FAILURE");
//									con.setReportIMStatus("FAILURE");
									errorFound=true;
									// UPDATE ERROR COMMENTS
									errorCommentsBuilder.append("FAILED WHILE PERFORMING KAPTURE OPERATION. PLEASE REFER TO FAILURE REPORT.");
									errorCommentsBuilder.append("\n");
									break;
								}
							}
						}
					}


					/*
					 * Failure List 
					 * SET STATUS AS FAILURE
					 * UPDATE REMARKS AS - FAILED WHILE PERFORMING DATABASE OPERATION. PLEASE REFER TO FAILURE REPORT.
					 */
					if (null != mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete()
							&& mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete().size() > 0) 
					{
						for (int b = 0; b < mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete().size(); b++) 
						{
							Map<Object, Object> dataMap = (HashMap<Object, Object>) mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete().get(b);
							if (null != dataMap && null != dataMap.get("DOCUMENT_LOCATION")	&& !"".equals(dataMap.get("DOCUMENT_LOCATION"))) 
							{
								// CHECK HERE CONTAINS
								if (dataMap.get("DOCUMENT_LOCATION").equals(con.getFilePath())) 
								{
									// file matches =
									// SET DOCUMENT PROCESSING STATUS AS FAILURE
//									con.setProcessingOperationStatus("FAILURE");
//									con.setReporDBStatus("FAIURE");
									errorFound=true;
									// UPDATE ERROR COMMENTS
									errorCommentsBuilder.append("FAILED WHILE PERFORMING DATABASE OPERATION. PLEASE REFER TO FAILURE REPORT.");
									errorCommentsBuilder.append("\n");
									break;
								}
							}
						}
					}

					/*
					 * ALSO CHECK HERE, IF DOCUMENT EXISTS IN MISSING CATEGORIES REPORT -
					 * THEN SET THE DOCUMENT AS FAILURE, UPDATE THE ERROR MESSAGES
					 * FAILED WHILE PERFORMING CATEGORY MAPPING. PLEASE REFER TO MISSING CATEGORIES REPORT.
					 */
					if(null!=missingCategoriesList && missingCategoriesList.size()>0)
					{
						for(int j=0;j<missingCategoriesList.size();j++)
						{
							CategoryDetails catDetails = (CategoryDetails)missingCategoriesList.get(j);
							if(null!=catDetails.getOperationType() && catDetails.getOperationType().equals("DELETE_CONTENT"))
							{
								if(null!=catDetails.getFilePath() && !"".equals(catDetails.getFilePath()))
								{
									if(catDetails.getFilePath().contains(con.getFilePath()))
									{
										// DOC EXISTS IN MISSING CAT REPORT
										// SET DOCUMENT PROCESSING STATUS AS FAILURE
										//									con.setProcessingOperationStatus("FAILURE");
										//									con.setReportCategoryMappingStatus("FAIURE");
										errorFound=true;
										// UPDATE ERROR COMMENTS
										errorCommentsBuilder.append("FAILED WHILE PERFORMING CATEGORY MAPPING. PLEASE REFER TO MISSING CATEGORIES REPORT.");
										break;
									}
								}
							}
						}
					}

					if(errorFound==true)
					{
						con.setProcessingOperationStatus("FAILURE");
					}
					else
					{
						// check here, IM DOCUMENT ID & CONTENT TYPE MUST NOT BE NULL
						if(null!=con.getImDocumentId() && !"".equals(con.getImDocumentId()) && null!=con.getImContentType() && !"".equals(con.getImContentType()))
						{
							con.setProcessingOperationStatus("SUCCESS");
						}
					}
					
					// add errorComments
					if(null!=errorCommentsBuilder)
					{
						con.setErrorComments(errorCommentsBuilder.toString());
					}
					errorCommentsBuilder = null;
				}
			}
			
			
			/*
			 * UPDATE IM DOCUMENT ID IN MISSING CATEGORY LIST
			 */
			if(null!=processingDeleteFileDetailsList && processingDeleteFileDetailsList.size()>0 && null!=missingCategoriesList && missingCategoriesList.size()>0)
			{
				for(int t=0;t<missingCategoriesList.size();t++)
				{
					CategoryDetails catDetails = (CategoryDetails)missingCategoriesList.get(t);
					if(null!=catDetails.getOperationType() && catDetails.getOperationType().equals("DELETE_CONTENT"))
					{
						for(int a=0;a<processingDeleteFileDetailsList.size();a++)
						{
							ContentDetails data = (ContentDetails)processingDeleteFileDetailsList.get(a);
							if(data.getFilePath().trim().toLowerCase().equals(catDetails.getFilePath().trim().toLowerCase()))
							{
								if(null!=catDetails.getContentDetails())
								{
									catDetails.getContentDetails().setImDocumentId(data.getImDocumentId());
								}
								break;
							}
						}
					}
				}
			}

			boolean windowJSFailure = false;
			if(null!=wiringDiagramUtils.getWindowJSList() && wiringDiagramUtils.getWindowJSList().size()>0)
			{
				WindowJSDetails jsDetails = null;
				for(int e=0;e<wiringDiagramUtils.getWindowJSList().size();e++)
				{
					jsDetails = (WindowJSDetails)wiringDiagramUtils.getWindowJSList().get(e);
					if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
					{
						windowJSFailure=true;
						break;
					}
					jsDetails = null;
				}
				jsDetails = null;
			}
			
			boolean voltageMapJSFailure=false;
			if(null!=wiringDiagramUtils.getVoltageMapJSList() && wiringDiagramUtils.getVoltageMapJSList().size()>0)
			{
				WindowJSDetails jsDetails = null;
				for(int e=0;e<wiringDiagramUtils.getVoltageMapJSList().size();e++)
				{
					jsDetails = (WindowJSDetails)wiringDiagramUtils.getVoltageMapJSList().get(e);
					if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
					{
						voltageMapJSFailure=true;
						break;
					}
					jsDetails = null;
				}
				jsDetails = null;
			}
			
			boolean voltageLinkMapJSFailure=false;
			if(null!=wiringDiagramUtils.getVoltageMapLinkJSList() && wiringDiagramUtils.getVoltageMapLinkJSList().size()>0)
			{
				WindowJSDetails jsDetails = null;
				for(int e=0;e<wiringDiagramUtils.getVoltageMapLinkJSList().size();e++)
				{
					jsDetails = (WindowJSDetails)wiringDiagramUtils.getVoltageMapLinkJSList().get(e);
					if(null==jsDetails.getInnerLinkDocumentId() || "".equals(jsDetails.getInnerLinkDocumentId()))
					{
						voltageLinkMapJSFailure=true;
						break;
					}
					jsDetails = null;
				}
				jsDetails = null;
			}
			
			boolean vinMLMappingFailure = false;
			if(null!=vinMLMappingList && vinMLMappingList.size()>0)
			{
				VinMLMappingDetails mappingDetails = null;
				for(int a=0;a<vinMLMappingList.size();a++)
				{
					mappingDetails = (VinMLMappingDetails)vinMLMappingList.get(a);
					if((null==mappingDetails.getProcessingStatus() || "".equals(mappingDetails.getProcessingStatus())) ||
							(null!=mappingDetails.getProcessingStatus() && mappingDetails.getProcessingStatus().equals(AutomationConstants.STATUS_FAILURE)))
					{
						vinMLMappingFailure = true;
						break;
					}
					mappingDetails = null;
				}
				mappingDetails = null;
			}
			
			boolean scmVINMappingFailure = false;
			if(null!=finalSCMVINProcessingList && finalSCMVINProcessingList.size()>0)
			{
				VinDetails mappingDetails = null;
				for(int a=0;a<finalSCMVINProcessingList.size();a++)
				{
					mappingDetails = (VinDetails)finalSCMVINProcessingList.get(a);
					if((null==mappingDetails.getProcessingStatus() || "".equals(mappingDetails.getProcessingStatus())) ||
							(null!=mappingDetails.getProcessingStatus() && mappingDetails.getProcessingStatus().trim().toLowerCase().
							equals(AutomationConstants.STATUS_FAILURE.toLowerCase())))
					{
						scmVINMappingFailure = true;
						break;
					}
					mappingDetails = null;
				}
				mappingDetails = null;
			}
			
			if((null==imErrorDetailsList || imErrorDetailsList.size()<=0) &&
					(null==mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails() || mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size()<=0) && 
					(null==missingCategoriesList || missingCategoriesList.size()<=0) && 
					(null==xcopyUtils.getFileUploadFailureList() || xcopyUtils.getFileUploadFailureList().size()<=0) 
					&& (innerLinksPassed==true) && (displayOrderPassed==true) &&  (cdProcessingPassed==true) && (null==imErrorDeleteDetailsList || imErrorDeleteDetailsList.size()<=0) 
					&& (null==mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete() || mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete().size()<=0) 
					&& (windowJSFailure==false) && (vinMLMappingFailure==false) && (voltageMapJSFailure==false) && (voltageLinkMapJSFailure==false) && (scmVINMappingFailure==false)
					&& (masterDataFailed==false))
			{
				// set failureStatus to false
				failureStatus = false;
				logger.info("################################ NO ERRORS FOUND UPDATE JOB STATUS AS SUCCESS ##########################################");
			}
			else
			{
				// set faiureStatus to true
				failureStatus = true;
				logger.info("################################ ERRORS FOUND UPDATE JOB STATUS AS FAILURE ##########################################");
			}
			
			/*
			 * call function to print processing Report
			 */
			printReportsUtils.printProcessingFilesReport(scheduleCode, processingFileDetailsList, xcopyUtils.getFileProcessingList(), processingDeleteFileDetailsList);
			printReportsUtils.printSingleFailureReport(scheduleCode, imErrorDetailsList, mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails(), xcopyUtils.getFileUploadFailureList(), imErrorDeleteDetailsList, mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete());
			printReportsUtils.printMissingCategoriesReport(scheduleCode, missingCategoriesList);
			
			if(null!=allInnerLinksList && allInnerLinksList.size()>0)
			{
				printReportsUtils.printInnerLinksReport(scheduleCode, allInnerLinksList);
			}
			if(null!=otherInnerLinksList && otherInnerLinksList.size()>0)
			{
				printReportsUtils.printOtherInnerLinksReport(scheduleCode, otherInnerLinksList);
			}
			if(null!=finalDisplayOrderList && finalDisplayOrderList.size()>0)
			{
				printReportsUtils.printDisplayOrderDetails(scheduleCode, finalDisplayOrderList);
			}
			if(null!=finalCDProcessingList && finalCDProcessingList.size()>0)
			{
				printReportsUtils.printCDDataDetails(scheduleCode, finalCDProcessingList);
			}
			if(null!=mmeDocumentManagementDAO.getViewContentDataList() && mmeDocumentManagementDAO.getViewContentDataList().size()>0)
			{
				printReportsUtils.printViewContentDetailsReport(scheduleCode, mmeDocumentManagementDAO.getViewContentDataList());
			}
			if(null!=wiringDiagramUtils.getWindowJSList() && wiringDiagramUtils.getWindowJSList().size()>0)
			{
				printReportsUtils.printWindowJSReport(scheduleCode, wiringDiagramUtils.getWindowJSList());
			}
			if(null!=wiringDiagramUtils.getVoltageMapJSList() && wiringDiagramUtils.getVoltageMapJSList().size()>0)
			{
				printReportsUtils.printVoltageMapJSReport(scheduleCode, wiringDiagramUtils.getVoltageMapJSList());
			}
			if(null!=wiringDiagramUtils.getVoltageMapLinkJSList() && wiringDiagramUtils.getVoltageMapLinkJSList().size()>0)
			{
				printReportsUtils.printVoltageLinkMapJSReport(scheduleCode, wiringDiagramUtils.getVoltageMapLinkJSList());
			}
			if(null!=vinMLMappingList && vinMLMappingList.size()>0)
			{
				printReportsUtils.printVinMLMappingReport(scheduleCode, vinMLMappingList);
			}
			if(null!=finalSCMVINProcessingList && finalSCMVINProcessingList.size()>0)
			{
				printReportsUtils.printSCMVINMappingReport(scheduleCode, finalSCMVINProcessingList);
			}
			/*
			 * now here, generate the Zip file for all the reports generated the
			 * path for reports directory will be -
			 * REPORTS_DIRECTORY/wslId/schCode
			 */
			String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY")+ "/"+ scheduleCode + "/";

			// the master data reports are written now, with the others (MasterDataLoad.writeReports)
			com.mazda.gms3.dmt.masterdata.MasterDataLoad.writeReports(scheduleCode);

			/*
			 * call function to generate zip file
			 */
			boolean bool = printReportsUtils.createReportsZip(path, scheduleCode);
			if (bool == true) 
			{
				logger.info("printReports :: Reports Zipped Successfully.");
			} 
			else 
			{
				logger.info("printReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
			path = null;
//			innerLinksList=null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "printReports()", e);
		}
	}
	
	/**
	 * Function will Perform Inner Links Processing for Material FOLDER.
	 * @param scheduleId
	 */
	private void startProcessingInnerLinks(String scheduleId, ScheduleItemDetails itemDetails)
	{
		logger.info("startProcessingInnerLinks :: Started At :: > "+new Date());
		try
		{
			if(null!=mateialFolderDocumentsListForInnerLinks && mateialFolderDocumentsListForInnerLinks.size()>0)
			{
				/*
				 * 
				 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS INNERLINKS PROCESSING 
				 * 
				 */
				ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "INNERLINKS PROCESSING");
				
				logger.info("startProcessingInnerLinks :: Total Documents for Material Folder {"+itemDetails.getMaterialFolderName()+"} Found are :: > " + mateialFolderDocumentsListForInnerLinks.size());
				/*
				 * FROM THE MATERIAL FOLDER LIST, IDENTIFY 
				 * IF THE DOCUMENT NEEDS TO BE UPDATED OR NOT
				 * e.g. INNERLINK FOUND - YES AND ALL LINKS MAPPED - NO AND INNERLINKS > 0 
				 */
				int documentsReUdateCount=0;
				int totalCount=0;
				int successCount=0;
				int failureCount=0;
				ContentDetails contentDetails = null;
				ContentDetails existDetails = null;
				LinkDetails linkDetails = null;
				List<String> uniqueInnerLinkPaths = null;
				boolean addToUniqueList = true;
				String modelType=null;
				String market=null;
				String locale = null;
				for(int i=0;i<mateialFolderDocumentsListForInnerLinks.size();i++)
				{
					contentDetails = (ContentDetails)mateialFolderDocumentsListForInnerLinks.get(i);
					if(null!=contentDetails.getInnerLinkFound() && contentDetails.getInnerLinkFound().equals("YES") 
							&& null!=contentDetails.getInnerLinksList()  && contentDetails.getInnerLinksList().size()>0)
					{
						logger.info("-----------------MME :: doc id :: >" + contentDetails.getImDocumentId());
						logger.info("-----------------MME :: doc id :: All Links Mapped :: >" + contentDetails.getAllInnerLinksMapped());
						logger.info("-----------------MME :: doc id :: InnerLinks List Size :: >" + contentDetails.getInnerLinksList().size());
						totalCount=totalCount+contentDetails.getInnerLinksList().size();
						if(null==modelType || "".equals(modelType))
						{
							modelType=  contentDetails.getModelType();
						}
						if(null==locale || "".equals(locale)) 
						{
							locale = contentDetails.getLocale();
						}
						if(null==market || "".equals(market))
						{
							market=  contentDetails.getMarket();
						}
						/*
						 *  INNER LINKS WITH DOCUMENT EXISTS
						 *  NOW VERIFY IF ALL LINKS ARE MAPPED OR NOT
						 */
						if(null!=contentDetails.getAllInnerLinksMapped() && contentDetails.getAllInnerLinksMapped().equals("NO"))
						{
							// this document is going to get updated
							documentsReUdateCount++;
							// DOCUMENT NEEDS TO BE - REPROCESSED
							linkDetails = null;
							for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
							{
								linkDetails=  (LinkDetails)contentDetails.getInnerLinksList().get(c);
								if(null!=linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath()))
								{
									addToUniqueList = true;
									if(null!=uniqueInnerLinkPaths && uniqueInnerLinkPaths.size()>0)
									{
										for(int d=0;d<uniqueInnerLinkPaths.size();d++)
										{
											if(uniqueInnerLinkPaths.get(d).toString().trim().toLowerCase().equals(linkDetails.getInnerLinkPath().trim().toLowerCase()))
											{
												// innerLink already added
												addToUniqueList = false;
												break;
											}
										}
									}
									
									if(addToUniqueList==true)
									{
										if(null==uniqueInnerLinkPaths || uniqueInnerLinkPaths.size()<=0)
										{
											uniqueInnerLinkPaths = new ArrayList<String>();
										}
										uniqueInnerLinkPaths.add(linkDetails.getInnerLinkPath());
									}
								}
								linkDetails = null;
							}
						}
						else if(null!=contentDetails.getAllInnerLinksMapped() && contentDetails.getAllInnerLinksMapped().equals("YES"))
						{
							// Inner Links mapped in first go
							// add all InnnerLinks as Success Count
							successCount=successCount+contentDetails.getInnerLinksList().size();
							
							// ADD THESE DIRECTLY TO ALL INNER LINKS REPORTS
							// add Content Details to 
							if(null==allInnerLinksList || allInnerLinksList.size()<=0)
							{
								allInnerLinksList = new ArrayList<ContentDetails>();
							}
							allInnerLinksList.add(contentDetails);
						}
					}
				}
				
				logger.info("startProcessingInnerLinks :: Total Documents for Re-Update Found are :: >" + documentsReUdateCount);
				if(null!=uniqueInnerLinkPaths && uniqueInnerLinkPaths.size()>0)
				{
					logger.info("startProcessingInnerLinks :: Total uniqueInnerLinkPaths Found are :: >"+ uniqueInnerLinkPaths.size());
				}
				else
				{
					logger.info("startProcessingInnerLinks :: No uniqueInnerLinkPaths Found are Material Folder :: >"+ itemDetails.getMaterialFolderName());
				}

				/*
				 * SOME LINKS ARE IDENTIFIED FOR UPDATE PROCESS
				 */
				List<SelectItemDetails> innerLinksDocumentIdsList = MMEDocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinkPaths, modelType, locale, market) ;
				if(null!=innerLinksDocumentIdsList && innerLinksDocumentIdsList.size()>0)
				{
					logger.info("startProcessingInnerLinks ::  Documents List Identified for Unique Inner Link Paths are :: > "+innerLinksDocumentIdsList.size());
				}
				else
				{
					logger.info("startProcessingInnerLinks :: No Documents List Identified for Unique Inner Link Paths.");
				}
				String linkUrl = ApplicationProperties.getProperty("LINK_URL");
				String linkToBeReplaced=null;
				// UPDATE DOCUMENT IDs FOR INNER LINKS IN RESPECTIVE DOCUMENTS
				SelectItemDetails si  = null;
				linkDetails = null;
				contentDetails =  null;
				int count=0;
				// pass 0: fix the links of each document and queue it; then the queued documents are written
				// in batches; pass 1: the result of each document, handled as before
				List<ContentDetails> innerLinkUpdates = new ArrayList<ContentDetails>();
				java.util.Set<ContentDetails> innerLinkUpdatesSaved = null;
				for(int pass=0;pass<2;pass++)
				{
				if(pass==1)
				{
					innerLinkUpdatesSaved = updateDocumentsForInnerLinks(innerLinkUpdates);
				}
				for(int a=0;a<mateialFolderDocumentsListForInnerLinks.size();a++)
				{
					contentDetails = (ContentDetails)mateialFolderDocumentsListForInnerLinks.get(a);
					if(null!=contentDetails.getInnerLinkFound() && contentDetails.getInnerLinkFound().equals("YES") 
							&& null!=contentDetails.getInnerLinksList()  && contentDetails.getInnerLinksList().size()>0)
					{
						/*
						 *  INNER LINKS WITH DOCUMENT EXISTS
						 *  NOW VERIFY IF ALL LINKS ARE MAPPED OR NOT
						 */
						if(null!=contentDetails.getAllInnerLinksMapped() && contentDetails.getAllInnerLinksMapped().equals("NO"))
						{
							// DOCUMENT NEEDS TO BE - REPROCESSED
							linkDetails = null;
							if(pass==0)
							{
							count++;
							logger.info("-------------- START RE-UPDATE FOR INNERLINLKS FOR DOCUMENT {"+contentDetails.getImDocumentId()+"} :: > "+ count +" / "+  documentsReUdateCount+".");
							for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
							{
								linkDetails=  (LinkDetails)contentDetails.getInnerLinksList().get(c);
//								if(null!=linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath()) 
//										&&  null!=innerLinksDocumentIdsList && innerLinksDocumentIdsList.size()>0)
								if(null!=linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath()))
								{
									// IDETIFY DOCUMENT ID FOR THIS INNER LINK
									si = null;
									// set default Map Status as N
									linkDetails.setMapStatus("N");
//									linkToBeReplaced = null;
									linkToBeReplaced = linkUrl;
									if(null!=innerLinksDocumentIdsList && innerLinksDocumentIdsList.size()>0)
									{
										for(int r=0;r<innerLinksDocumentIdsList.size();r++)
										{
											si = (SelectItemDetails)innerLinksDocumentIdsList.get(r);
											if(si.getValue().trim().toLowerCase().equals(linkDetails.getInnerLinkPath().trim().toLowerCase()))
											{
												if(null!=si.getLabel() && !"".equals(si.getLabel()))
												{
													// set link for replacing
													//												linkToBeReplaced = linkUrl+si.getLabel();
													linkToBeReplaced = linkToBeReplaced+si.getLabel();
													// set document id
													linkDetails.setInnnerLinkDocumentId(si.getLabel());
													// set LINK MAPPED FLAG TO Y
													linkDetails.setMapStatus("Y");
												}
												break;
											}
											si = null;
										}
									}
									logger.info("MME ----- link to be replaced :: >"+ linkToBeReplaced);
									logger.info("MME --------- source path :: >"+ linkDetails.getInnerLinkPath());
									if(null!=linkToBeReplaced && !"".equals(linkToBeReplaced))
									{
										// REPLACE THIS INNERLINK IN CONTENT DOCUMENT
										if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
										{
											contentDetails.setDocumentContent(contentDetails.getDocumentContent().replace(linkDetails.getInnerLinkPath(), linkToBeReplaced.replace("&", "&amp;")));
										}
									}
									linkToBeReplaced= null;
								}
								else
								{
									// set LINK MAPPED FLAG TO N
									linkDetails.setMapStatus("N");
								}
								linkDetails = null;
							}

							// queued - written in batches before pass 1
							innerLinkUpdates.add(contentDetails);
							continue;
							}

							/*
							 * THE DOCUMENT WAS UPDATED IN KAPTURE (OR NOT) BY updateDocumentsForInnerLinks()
							 */
							if (null != contentDetails && null != contentDetails.getImDocumentId() 	&& !"".equals(contentDetails.getImDocumentId()) 
									&& null!=contentDetails.getImContentType() && !"".equals(contentDetails.getImContentType())) 
							{
								/*
								 * UPDATE CONTENT DETAILS IN DATABASE
								 * 
								 * CHECK HERE IF PREVIOUS DOCUMENT ID IS NOT NULL FOR THE DOCUMENT
								 * THEN IN THAT CASE, DELETE THE DATA FOR PREVIOUS DOC ID.
								 * INSERT THE DATA FOR NEW DOC ID.
								 */
								boolean flag = innerLinkUpdatesSaved.contains(contentDetails);
								if (flag == true) 
								{
									logger.info("startProcessingInnerLinks :: Document Details Updated Successfully in Database For Document Id :: "+ contentDetails.getImDocumentId());
									/*
									 * now check here, if document Type is PDF, then move
									 * the PDF File to Live and Staging Directory
									 */
									if (contentDetails.getDocumentType().equals(ContentDetails.PDF_DOCUMENT)) 
									{
										if (null != contentDetails.getPdfFilePathAsAttachment() && !"".equals(contentDetails.getPdfFilePathAsAttachment())) 
										{
											File sourceFile = PathUtil.file(contentDetails.getPdfFilePathAsAttachment());
											if (sourceFile.isFile() && sourceFile.exists()) 
											{
												// Call function to move the PDF To Live and
												// Staging Folder
												// the PDF is uploaded to Kapture as the attachment of the document - nothing to copy
											}
										}
									}

									/*
									 * SET INNER LINKS DETAILS
									 */
									boolean setFailedStatus=false;
									if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
									{
										linkDetails = null;
										for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
										{
											linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(c);
											if(null!=linkDetails && null!=linkDetails.getMapStatus() && linkDetails.getMapStatus().equals("N"))
											{
												setFailedStatus = true;
												break;
											}
										}

										linkDetails = null;
										for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
										{
											linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(c);
											if(null!=linkDetails && null!=linkDetails.getMapStatus() && linkDetails.getMapStatus().equals("N"))
											{
												// INCREMENT FAILED INNER LINKS COUNT
												failureCount++;
											}
											else if(null!=linkDetails && null!=linkDetails.getMapStatus() && linkDetails.getMapStatus().equals("Y"))
											{
												// INCREMENT PROCESSED INNER LINKS COUNT
												successCount++;
											}
										}
									}
									if(setFailedStatus==true)
									{
										contentDetails.setAllInnerLinksMapped("NO");
										contentDetails.setInnerLinkMappingReason("PLEASE REFER TO INNER LINKS REPORT.");
									}
									else
									{
										contentDetails.setAllInnerLinksMapped("YES");
									}

									// add Content Details to 
									if(null==allInnerLinksList || allInnerLinksList.size()<=0)
									{
										allInnerLinksList = new ArrayList<ContentDetails>();
									}
									allInnerLinksList.add(contentDetails);

									/*
									 * SEARCH FOR THE CONTENT DETAILS IN PROCESSING LIST AND UPDATE THE DETAILS
									 */
									if(null!=processingFileDetailsList && processingFileDetailsList.size()>0)
									{
										existDetails = null;
										for(int e=0;e<processingFileDetailsList.size();e++)
										{
											existDetails = (ContentDetails)processingFileDetailsList.get(e);
											if(null!=existDetails.getFileAbsolutePath() && null!=contentDetails.getFileAbsolutePath())
											{
												if(existDetails.getFileAbsolutePath().equals(contentDetails.getFileAbsolutePath()))
												{
													existDetails.setImContentType(contentDetails.getImContentType());
													existDetails.setImDocStatus(contentDetails.getImDocStatus());
													existDetails.setImResourcePath(contentDetails.getImResourcePath());
													existDetails.setImVersion(contentDetails.getImVersion());

													existDetails.setInnerLinkFound(contentDetails.getInnerLinkFound());
													existDetails.setInnerLinkMappingReason(contentDetails.getInnerLinkMappingReason());
													existDetails.setAllInnerLinksMapped(contentDetails.getAllInnerLinksMapped());
													existDetails.setInnerLinksList(contentDetails.getInnerLinksList());
												}
											}
											existDetails = null;
										}
									}
								} 
								else 
								{
									logger.info("startProcessingInnerLinks :: Failed to Update Document Details in Database For Document Id :: > "+ contentDetails.getImDocumentId());
									/*
									 * SET INNER LINKS DETAILS
									 */
									contentDetails.setInnerLinkFound("YES");
									contentDetails.setAllInnerLinksMapped("NO");
									contentDetails.setInnerLinkMappingReason("FAILED TO UPDATE {"+contentDetails.getImDocumentId()+"} IN DATABASE.");
									// SET INNERLINKS LIST TO NULL 
									//										contentDetails.setInnerLinksList(null);
									// add all innerLinks count for failureCount
									if(null!=contentDetails.getInnerLinksList())
									{
										linkDetails = null;
										for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
										{
											linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(c);
											// set All Flags to N
											linkDetails.setMapStatus("N");
											// INCREMENT FAILED INNER LINKS COUNT
											failureCount++;
											linkDetails = null;
										}
									}
									// add Content Details to 
									if(null==allInnerLinksList || allInnerLinksList.size()<=0)
									{
										allInnerLinksList = new ArrayList<ContentDetails>();
									}
									allInnerLinksList.add(contentDetails);

									/*
									 * iterate File Processing List and Update the Error
									 */
									if(null!=processingFileDetailsList && processingFileDetailsList.size()>0)
									{
										existDetails = null;
										for(int e=0;e<processingFileDetailsList.size();e++)
										{
											existDetails = (ContentDetails)processingFileDetailsList.get(e);
											if(null!=existDetails.getFileAbsolutePath() && null!=contentDetails.getFileAbsolutePath())
											{
												if(existDetails.getFileAbsolutePath().equals(contentDetails.getFileAbsolutePath()))
												{
													existDetails.setImContentType(contentDetails.getImContentType());
													existDetails.setImDocStatus(contentDetails.getImDocStatus());
													existDetails.setImResourcePath(contentDetails.getImResourcePath());
													existDetails.setImVersion(contentDetails.getImVersion());

													existDetails.setInnerLinkFound(contentDetails.getInnerLinkFound());
													existDetails.setInnerLinkMappingReason(contentDetails.getInnerLinkMappingReason());
													existDetails.setAllInnerLinksMapped(contentDetails.getAllInnerLinksMapped());
													existDetails.setInnerLinksList(contentDetails.getInnerLinksList());
												}
											}
											existDetails = null;
										}
									}
								}
							}
							else
							{
								logger.info("startProcessingInnerLinks :: Failed to update Document in Info Manager for File :: > "+ contentDetails.getFilePath());

								/*
								 * SET INNER LINKS DETAILS
								 */
								contentDetails.setInnerLinkFound("YES");
								contentDetails.setAllInnerLinksMapped("NO");
								contentDetails.setInnerLinkMappingReason("FAILED TO UPDATE {"+contentDetails.getImDocumentId()+"} IN INFO MANAGER.");
								// SET INNERLINKS LIST TO NULL 
								//									contentDetails.setInnerLinksList(null);
								// add all innerLinks count for failureCount
								if(null!=contentDetails.getInnerLinksList())
								{
									linkDetails = null;
									for(int c=0;c<contentDetails.getInnerLinksList().size();c++)
									{
										linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(c);
										// set All Flags to N
										linkDetails.setMapStatus("N");
										// INCREMENT FAILED INNER LINKS COUNT
										failureCount++;
										linkDetails = null;
									}
								}
								// add Content Details to 
								if(null==allInnerLinksList || allInnerLinksList.size()<=0)
								{
									allInnerLinksList = new ArrayList<ContentDetails>();
								}
								allInnerLinksList.add(contentDetails);

								/*
								 * iterate File Processing List and Update the Error
								 */
								if(null!=processingFileDetailsList && processingFileDetailsList.size()>0)
								{
									existDetails = null;
									for(int e=0;e<processingFileDetailsList.size();e++)
									{
										existDetails = (ContentDetails)processingFileDetailsList.get(e);
										if(null!=existDetails.getFileAbsolutePath() && null!=contentDetails.getFileAbsolutePath())
										{
											if(existDetails.getFileAbsolutePath().equals(contentDetails.getFileAbsolutePath()))
											{
												existDetails.setImContentType(contentDetails.getImContentType());
												existDetails.setImDocStatus(contentDetails.getImDocStatus());
												existDetails.setImResourcePath(contentDetails.getImResourcePath());
												existDetails.setImVersion(contentDetails.getImVersion());

												existDetails.setInnerLinkFound(contentDetails.getInnerLinkFound());
												existDetails.setInnerLinkMappingReason(contentDetails.getInnerLinkMappingReason());
												existDetails.setAllInnerLinksMapped(contentDetails.getAllInnerLinksMapped());
												existDetails.setInnerLinksList(contentDetails.getInnerLinksList());
											}
										}
										existDetails = null;
									}
								}
							}
							logger.info("-------------- END RE-UPDATE FOR INNERLINLKS FOR DOCUMENT {"+contentDetails.getImDocumentId()+"} :: > "+ count +" / "+  documentsReUdateCount+".");
						}
					}
				}

				}
				uniqueInnerLinkPaths = null;
				innerLinksDocumentIdsList = null;
				/*
				 * UPDATE INNER LINK MAPPING STATUS WITH DOCUMENTS
				 */
				mmeDocumentManagementDAO.updateInnerLinkMappingStatus(mateialFolderDocumentsListForInnerLinks);
				linkUrl  = null;	
			
				logger.info("startProcessingInnerLinks :: For Material Folder "+itemDetails.getMaterialFolderName()+". Total Inner Links Count :: >"+ totalCount);
				logger.info("startProcessingInnerLinks :: For Material Folder "+itemDetails.getMaterialFolderName()+". Total Success Count :: >"+ successCount);
				logger.info("startProcessingInnerLinks :: For Material Folder "+itemDetails.getMaterialFolderName()+". Total Failure Count :: >"+ failureCount);
				
				/*
				 * UPDATE TOTAL, PROCESSED & FAILED INNER LINKS COUNT FOR SCHEDULE ID & ITEM ID
				 */
				ScheduleDAO.updateInnerLinksCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), totalCount, successCount, failureCount);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startProcessingInnerLinks()", e);
		}
		logger.info("startProcessingInnerLinks :: Ended At :: > "+new Date());
	}

	/**
	 * modifyContentForInnerLinks() + updateDocumentDetails(.., false) for all the documents whose
	 * inner links were fixed: bulk updates in Kapture, one database transaction.
	 *
	 * @return the documents whose details were saved in the database
	 */
	private java.util.Set<ContentDetails> updateDocumentsForInnerLinks(List<ContentDetails> docs)
	{
		java.util.Set<ContentDetails> saved = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ContentDetails, Boolean>());
		if (null == docs || docs.isEmpty())
		{
			return saved;
		}
		String operationType="MODIFY_CONTENT_INNERLINKS";
		long innerStarted = System.currentTimeMillis();
		logger.info("updateDocumentsForInnerLinks :: documents to re-update :: >" + docs.size());
		List<KaptureContentService.BatchItem> items = new ArrayList<KaptureContentService.BatchItem>();
		for (ContentDetails cd : docs)
		{
			if (KaptureArticleBuilder.isPartialWiringDiagramUpdate(cd))
			{
				// read - change - write of a single file's attributes: one document at a time
				modifyContentForInnerLinks(cd);
				continue;
			}
			try
			{
				if(kaptureAvailable(cd, operationType)==false)
				{
					continue;
				}
				KaptureArticle article = KaptureArticleBuilder.build(cd, kaptureLocale);
				article.documentId = cd.getImDocumentId();
				// the document keeps its categories; missing ones were already reported when it was written
				article.categories = resolveCategories(cd, operationType, false);
				items.add(new KaptureContentService.BatchItem(article, cd));
			}
			catch (Exception e)
			{
				cd.setImContentType(null);
				Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
				capturreOtherErrorDetails(operationType, cd, e);
			}
		}
		if (!items.isEmpty())
		{
			kapture.updateBatch(items);
		}
		for (KaptureContentService.BatchItem it : items)
		{
			ContentDetails cd = (ContentDetails) it.owner;
			if (it.result.success)
			{
				logger.info("modifyContentForInnerLinks :: Document modified successfully for File {"+ cd.getFilePath()	+ "} :: Document Id :: > "	+ it.result.documentId);
				applyKaptureResult(cd, it.result);
			}
			else if (KaptureContentService.ERR_NOT_FOUND.equals(it.result.errorCode))
			{
				logger.info("modifyContentForInnerLinks :: Failed to Fetch Content from Kapture For Document Id :: >"+ cd.getImDocumentId());
				cd.setImContentType(null);
				cd.setInnerLinkFound("YES");
				cd.setAllInnerLinksMapped("NO");
				cd.setInnerLinkMappingReason("FAILED TO FETCH {"+cd.getImDocumentId()+"} FROM KAPTURE.");
			}
			else
			{
				cd.setImContentType(null);
				logger.info("modifyContentForInnerLinks :: Failed to Modify Document for File :: >" 	+ cd.getFilePath());
				addKaptureError(it.result.errorCode, "FAILED TO MODIFY DOCUMENT IN KAPTURE. " + it.result.message, cd, operationType);
			}
		}

		List<ContentDetails> written = new ArrayList<ContentDetails>();
		for (ContentDetails cd : docs)
		{
			if (hasText(cd.getImDocumentId()) && hasText(cd.getImContentType()))
			{
				written.add(cd);
			}
		}
		if (written.isEmpty())
		{
			return saved;
		}
		logger.info("updateDocumentsForInnerLinks :: Kapture updates done :: written=" + written.size() + " of " + docs.size()
				+ " :: " + (System.currentTimeMillis() - innerStarted) + " ms");
		innerStarted = System.currentTimeMillis();
		try
		{
			MMEDocumentBatchDAO.updateDocuments(written, false);
			saved.addAll(written);
			logger.info("updateDocumentsForInnerLinks :: database updated :: " + written.size() + " documents :: "
					+ (System.currentTimeMillis() - innerStarted) + " ms");
		}
		catch (Exception e)
		{
			logger.info("updateDocumentsForInnerLinks :: the batch update failed - updating the " + written.size() + " documents one by one");
			for (ContentDetails cd : written)
			{
				try
				{
					if (mmeDocumentManagementDAO.updateDocumentDetails(cd, false))
					{
						saved.add(cd);
					}
				}
				catch (Exception ex)
				{
					Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "updateDocumentsForInnerLinks()", ex);
				}
			}
		}
		return saved;
	}

	private void startProcessingDisplayOrder(File materialDirFolder, ScheduleItemDetails itemDetails, String wslId, ArrayList<DisplayOrderDetails> displayOrderList, ArrayList<VinDetails> applicableVINListForMaterialFolder,String channelFolderName)
	{
		try
		{
			logger.info("startProcessingDisplayOrder :: Processing MaterialDirFolder Directory at Path :: > " + PathUtil.winPath(materialDirFolder));

			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DISPLAY ORDER PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "DISPLAY ORDER PROCESSING");

			ArrayList<DisplayOrderDetails> processingDisplayOrderList = ScheduleDAO.getApplicableDisplayOrderForMaterialOrFaceLiftFolder(itemDetails.getModelFolderName(), itemDetails.getManualType(), itemDetails.getFaceLiftFolderName(), itemDetails.getMaterialFolderName());
			DisplayOrderDetails details = null;
			if(null!=processingDisplayOrderList && processingDisplayOrderList.size()>0)
			{
				/*
				 * identify exact Model Name here frm ItemDetails
				 */
				String modelFolderName = itemDetails.getModelFolderName();
				String model=null;
				if(null!=modelFolderName && !"".equals(modelFolderName))
				{
					String afterFirstIndex="";
					if(modelFolderName.indexOf("_")!=-1)
					{
						// SET MODEL TYPE HERE
						afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
					}
					
					// NOW IDENTIFY MODEL 
					if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
					{
						if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
						{
							// OVER HAUL MANUALS.
							model = afterFirstIndex;
						}
						else
						{
							// NORMAL MODEL -> cx-3_ab. Check for Last Index, because model could be Nissan_familia_van_bv
							if(afterFirstIndex.lastIndexOf("_")!=-1)
							{
								model = afterFirstIndex.substring(0, afterFirstIndex.lastIndexOf("_"));
							}
							else
							{
								// set all as Model Name
								model = afterFirstIndex;
							}
						}
					}
					afterFirstIndex = null;
				}
				modelFolderName = null;
				/*
				 * IDENTIFY ALL UNIQUE DOCUMENTS FROM THE PROCESSING DISPLAY ORDER LIST
				 * ON THE BASIS OF IDENTIFIED APPLICABLE DISPLAY ORDER, IDENTIFY APPLICBALE VIN FOR EACH DOCUMENT
				 */
				ArrayList<ContentDetails> documentsList = new ArrayList<ContentDetails>();
				details = null;
				ContentDetails contentDetails = null;
				DisplayOrderDetails existingDisplayOrderDetails = null;
				int failureCount=0;
				int successCount=0;
				int invalidLinesCount=0;
				for(int a=0;a<processingDisplayOrderList.size();a++)
				{
					details = (DisplayOrderDetails)processingDisplayOrderList.get(a);
					// UPDATE SCHEDULE ID & LOCALE IN THE DISPLAY ORDER LIST FOR ALL ROWS
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					
					// set Model Here
					details.setModel(model);
					if(null!=details.getLineType() && details.getLineType().equals(ConversionUtils.LINE_TYPE_VALID))
					{
						// increment processing rows Count
						boolean addDocument = true;
						if(null!=documentsList && documentsList.size()>0)
						{
							for(int b=0;b<documentsList.size();b++)
							{
								contentDetails = (ContentDetails)documentsList.get(b);
								if(null!=contentDetails.getFilePath() && !"".equals(contentDetails.getFilePath()))
								{
									if(contentDetails.getFilePath().trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase()))
									{
										// do not add this document - as it is already added
										addDocument=  false;
										String check = "";
										check = returnKey(check, details);
										// check id displayOrder is added or Not
										boolean dispAlreadyAdded = false;
										if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
										{
											existingDisplayOrderDetails = null;
											for(int x=0;x<contentDetails.getApplicableDisplayOrderList().size();x++)
											{
												existingDisplayOrderDetails = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(x);
												String exist = "";
												exist=  returnKey(exist, existingDisplayOrderDetails);
												if(check.trim().toLowerCase().equals(exist.trim().toLowerCase()))
												{
													// displayOrder Already added = do not add
													dispAlreadyAdded=true;
													break;
												}
												exist = null;
												existingDisplayOrderDetails = null;
											}
										}
										check = null;
										
										if(dispAlreadyAdded==false)
										{
											// add this DisplayOrder to Document
											if(null==contentDetails.getApplicableDisplayOrderList() || contentDetails.getApplicableDisplayOrderList().size()<=0)
											{
												contentDetails.setApplicableDisplayOrderList(new ArrayList<DisplayOrderDetails>());
											}
											contentDetails.getApplicableDisplayOrderList().add(details);
										}
										break;
									}
								}
								contentDetails = null;
							}
						}
						
						if(addDocument==true)
						{
							contentDetails = new ContentDetails();
							contentDetails.setLocale(itemDetails.getLocale());
							contentDetails.setFilePath(details.getFilePath());
							contentDetails.setModel(details.getModel());
							contentDetails.setModelFolderName(details.getModelFolderName());
							contentDetails.setModelType(details.getModelType());
							contentDetails.setManualType(details.getManualType());
							if(null==contentDetails.getApplicableDisplayOrderList())
							{
								contentDetails.setApplicableDisplayOrderList(new ArrayList<DisplayOrderDetails>());
							}
							contentDetails.getApplicableDisplayOrderList().add(details);
							if(null==documentsList || documentsList.size()<=0)
							{
								documentsList = new ArrayList<ContentDetails>();
							}
							documentsList.add(contentDetails);
							contentDetails = null;
						}
					}
					else
					{
						// INVALID LINES
						// increment invalidLinesCount
						invalidLinesCount++;
						details.setProcessingStatus("FAILURE");
						details.setErrorCode("DSP000");
						details.setErrorMessage("DISPLAY ORDER ROW IS NOT A VALID DATA.");
						if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
						{
							finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
						}
						finalDisplayOrderList.add(details);
						// remove this row from processingDisplayOrderList
						processingDisplayOrderList.remove(a);
						a--;
					}
					details = null;
				}
				
				/*
				 * NOW IDENTIFY APPLICABLE VIN FOR EACH DOCUMENT
				 */
				contentDetails = null;
				details = null;
				VinDetails vinDetails = null;
				if(null==documentsList || documentsList.size()<=0)
				{
					documentsList = new ArrayList<ContentDetails>();
				}
				logger.info("startProcessingDisplayOrder :: Unique Documents for Material Folder {"+itemDetails.getMaterialFolderName()+"} are :: > " + documentsList.size());
				if(null!=documentsList && documentsList.size()>0)
				{
					if(null!=applicableVINListForMaterialFolder && applicableVINListForMaterialFolder.size()>0)
					{
						for(int a=0;a<documentsList.size();a++)
						{
							contentDetails = (ContentDetails)documentsList.get(a);
							if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
							{
								vinDetails = null;
								details = null;
								for(int u=0;u<applicableVINListForMaterialFolder.size();u++)
								{
									vinDetails = (VinDetails)applicableVINListForMaterialFolder.get(u);
									if(null!=vinDetails.getDisplayOrderTextFileName() && null!=vinDetails.getMaterialFolder())
									{
										for(int r=0;r<contentDetails.getApplicableDisplayOrderList().size();r++)
										{
											details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(r);
											if(null!=details.getDisplayOrderSourceFileName() && null!=details.getMaterialFolderName())
											{
												// CHECK FOR MATERIAL FOLDER NAME IS ALSO REQUIRED
												if(details.getDisplayOrderSourceFileName().trim().toLowerCase().equals(vinDetails.getDisplayOrderTextFileName().trim().toLowerCase())
														&& details.getMaterialFolderName().trim().toLowerCase().equals(vinDetails.getMaterialFolder().trim().toLowerCase()))
												{
													// add this VIN
													if(null==contentDetails.getApplicableVINList() || contentDetails.getApplicableVINList().size()<=0)
													{
														contentDetails.setApplicableVINList(new ArrayList<VinDetails>());
													}
													contentDetails.getApplicableVINList().add(vinDetails);
													break;
												}
											}
											details = null;
										}
									}
									vinDetails = null;
								}
							}
						}
					}
				}
				
				/*
				 * PREPARE DATA FOR VIEW CONTENT AND INSERT IN DATA BASE TABLE
				 */
				List<DisplayOrderDetails> tList = mmeDocumentManagementDAO.saveDisplayOrderDetails(documentsList, itemDetails.getModelType(), "CHECK_INSIDE_LIST", channelFolderName, itemDetails.getLocale(), model, itemDetails.getManualType());
				if(null!=tList && tList.size()>0)
				{
					details = null;
					boolean namesFound = true;
					for(int a=0;a<tList.size();a++)
					{
						details =(DisplayOrderDetails)tList.get(a);
						if(null!=details.getProcessingStatus() && details.getProcessingStatus().equals("SUCCESS"))
						{
							/*
							 * CHECK HERE IF NAMES AVAILABLE FOR ALL DISPLAY ORDER CODES OR NOT
							 * IF ALL AVAILABLE - THEN SUCCESS - update successCount
							 * ELSE FAILURE - WITH ERROR MESSAGE = DISPLAY ORDER NAMES COULD NOT BE LOCATED.
							 * 	updateFailureCount
							 */
							namesFound = true;
							// Start with Level 1
							// Display order CODES are no longer supplied - the file carries the NAMES themselves,
							// so there is no code left to find a name for and nothing to validate here.
							
							if(namesFound==false)
							{
								// set STATUS AS FAILURE
								details.setProcessingStatus("FAILURE");
								details.setErrorCode("DSP001");
								details.setErrorMessage("FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES IN THE ROW.");
							}
						}
						details = null;
					}
					
					// iDENITFY SUCCESS & FAILURE COUNT FROM THE TLIST AS IT IS SPECIFIC FOR THIS PROCESSING ITEM
					details = null;
					for(int a=0;a<tList.size();a++)
					{
						details = (DisplayOrderDetails)tList.get(a);
						if(null!=details.getProcessingStatus() && details.getProcessingStatus().equals("SUCCESS"))
						{
							successCount++;
						}
						else
						{
							failureCount++;
						}
					}
					
					// add to Final Processing List
					if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
					{
						finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
					}
					finalDisplayOrderList.addAll(tList);
				}
				else
				{
					// ADD ALL PROCESSING DISPLAY LIST COUNT AS FAILURE COUNT FOR ITEM & SCHEDULE + INVALID LINES COUNT
					if(null!=processingDisplayOrderList && processingDisplayOrderList.size()>0)
					{
						// set failureCount as complete = processingDisplayOrderList
						failureCount = processingDisplayOrderList.size();
						
						// iterate and set Processing status as Failure for all rows in ProcessingDisplayOrderList
						details = null;
						for(int e=0;e<processingDisplayOrderList.size();e++)
						{
							details = (DisplayOrderDetails)processingDisplayOrderList.get(e);
							// set STATUS AS FAILURE
							details.setProcessingStatus("FAILURE");
							details.setErrorCode("DSP010");
							details.setErrorMessage("FAILED TO PERFORM DATABASE OPERATIONS FOR THE DISPLAY ORDER ROW. PLEASE CONTACT IT SUPPORT TEAM.");
							if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
							{
								finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
							}
							finalDisplayOrderList.add(details);
							details = null;
						}
					}
				}
				tList = null;
				
				/*
				 * UPDTAE SUCCESS / FAILURE COUNT FOR SCHEDULE & ITEM FOR DISPLAY ORDER
				 * FAILURE COUNT = FAILURE COUNT + INVALID LINES COUNT
				 */
				ScheduleDAO.updateDISPCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), successCount,(failureCount+invalidLinesCount));
				model = null;
			}
			else
			{
				logger.info("startProcessingDisplayOrder :: No Display Order Information Found for Processing Display Order for Material Folder :: > " + itemDetails.getMaterialFolderName());
			}
			processingDisplayOrderList = null;
			channelFolderName = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startProcessingDisplayOrder()", e);
		}
	}

	/*
	 * FUNCTION NOT BE USED 
	 * 25TH JULY 2019
	 */
	private void startProcessingDisplayOrderForFaceLift(File faceLiftDirFolder, ScheduleItemDetails itemDetails, String wslId, 
				ArrayList<DisplayOrderDetails> displayOrderList, ArrayList<VinDetails> allVINList,String channelFolderName)
	{
		try
		{
			logger.info("startProcessingDisplayOrderForFaceLift :: Processing FaceLift Directory at Path :: > " + PathUtil.winPath(faceLiftDirFolder));

			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DISPLAY ORDER PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "DISPLAY ORDER PROCESSING");

			ArrayList<DisplayOrderDetails> processingDisplayOrderList = new ArrayList<DisplayOrderDetails>();
			
			if(null!=displayOrderList && displayOrderList.size()>0)
			{
				/*
				 * identify exact Model Name here frm ItemDetails
				 */
				String modelFolderName = itemDetails.getModelFolderName();
				String model=null;
				if(null!=modelFolderName && !"".equals(modelFolderName))
				{
					String afterFirstIndex="";
					if(modelFolderName.indexOf("_")!=-1)
					{
						// SET MODEL TYPE HERE
						afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
					}
					
					// NOW IDENTIFY MODEL 
					if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
					{
						if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
								afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
						{
							// OVER HAUL MANUALS.
							model = afterFirstIndex;
						}
						else
						{
							// NORMAL MODEL -> cx-3_ab. Check for Last Index, because model could be Nissan_familia_van_bv
							if(afterFirstIndex.lastIndexOf("_")!=-1)
							{
								model = afterFirstIndex.substring(0, afterFirstIndex.lastIndexOf("_"));
							}
							else
							{
								// set all as Model Name
								model = afterFirstIndex;
							}
						}
					}
					afterFirstIndex = null;
				}
				modelFolderName = null;
				
				
				
				// UPDATE SCHEDULE ID & LOCALE IN THE DISPLAY ORDER LIST FOR ALL ROWS
				for(DisplayOrderDetails details : displayOrderList)
				{
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					/*
					 * CHECK MODEL FOLDER NAME, MANUAL TYPE, FACELIFT FOLDER AND MATEIAL FOLDER NAME
					 * MUST MATCH, ONLY THOSE ENTRIES WILL BE PROCESSED FURTHER
					 */
					if(null!=details.getModelFolderName() && null!=details.getManualType() && null!=details.getFaceLiftFolderName() &&  
						 null!=itemDetails.getModelFolderName() && null!=itemDetails.getManualType() && null!=itemDetails.getFaceLiftFolderName())
					{
						if(details.getModelFolderName().toLowerCase().trim().
								equalsIgnoreCase(itemDetails.getModelFolderName().toLowerCase().trim()) 
								&& details.getManualType().trim().toLowerCase().equalsIgnoreCase(itemDetails.getManualType().trim().toLowerCase()) 
								&& details.getFaceLiftFolderName().toLowerCase().trim().
								equalsIgnoreCase(itemDetails.getFaceLiftFolderName().toLowerCase().trim()))
						{
							// set model Name
							details.setModel(model);
							// add to Processing Display Order List
							processingDisplayOrderList.add(details);
						}
					}
				}
				
				if(null!=processingDisplayOrderList && processingDisplayOrderList.size()>0)
				{
					/*
					 * IDENITFY UNIQUE MATERIAL FOLDER LIST FROM ALL VINS LIST
					 */
					List<String> uniqueMaterialFolderPathsList = new ArrayList<String>();
					VinDetails vinDetails = null;
					// iDENITFY UNIQUE MATERIAL FOLDER PATHS
					for (int f=0;f<allVINList.size();f++)
					{
						vinDetails = (VinDetails)allVINList.get(f);
						if(null!=vinDetails.getLineType() && vinDetails.getLineType().equals(ConversionUtils.LINE_TYPE_VALID))
						{
							String path = itemDetails.getLocale()+"\\"+itemDetails.getModelFolderName()+"\\"+itemDetails.getManualType()+"\\"+itemDetails.getFaceLiftFolderName()+"\\"+vinDetails.getMaterialFolder();
							boolean add = true;
							if(null!=uniqueMaterialFolderPathsList && uniqueMaterialFolderPathsList.size()>0)
							{
								for(int y=0;y<uniqueMaterialFolderPathsList.size();y++)
								{
									if(uniqueMaterialFolderPathsList.get(y).trim().toLowerCase().equals(path.trim().toLowerCase()))
									{
										add  = false;
										break;
									}
								}
							}
							if(add==true)
							{
								logger.info("startProcessingDisplayOrderForFaceLift :: Adding Material Folder Path To Unique List :: > "+ path);
								uniqueMaterialFolderPathsList.add(path);
							}
							path  =null;
						}
						vinDetails = null;
					}
					
					if(null==uniqueMaterialFolderPathsList || uniqueMaterialFolderPathsList.size()<=0)
					{
						uniqueMaterialFolderPathsList = new ArrayList<String>();
					}
					logger.info("startProcessingDisplayOrderForFaceLift :: Total Unique Material Folder Paths found are :: >"+ uniqueMaterialFolderPathsList.size());
					// UNIQUE MATERIAL FOLDERS IDENTIFIED 
					if(null!=uniqueMaterialFolderPathsList && uniqueMaterialFolderPathsList.size()>0 && null!=processingDisplayOrderList && processingDisplayOrderList.size()>0)
					{
						/*
						 * START PROCESSING EACH MATERIAL FOLDER
						 * 	IDENTIFY APPLICABLE VIN FOR EACH MATERIAL FOLDER
						 *  Prepare Unique Documents List for each MATERIAL FOLDER
						 *  IDENTIFY APPLICABLE DISPLAY ORDER FOR EACH DOCUMENT
						 *  ADD APPLICBALE VIN FOR EACH DOCUMENT
						 */
						// identifying applicable VIN for each material folder
						ContentDetails contentDetails = null;
						DisplayOrderDetails displayOrderDetails = null;
						DisplayOrderDetails existingDisplayOrderDetails = null;
						ArrayList<ContentDetails> documentsList=new ArrayList<ContentDetails>();
						int invalidLinesCount=0;
						int successCount=0;
						int failureCount=0;
						String path = null;
						for(int o=0;o<uniqueMaterialFolderPathsList.size();o++)
						{
							logger.info("startProcessingDisplayOrderForFaceLift :: ------------ Procesing ------------------------ >"+ uniqueMaterialFolderPathsList.get(o)+ " at ::>" + new Date());
							// PREPARING UNIQUE DOCUMENTS LIST FOR EACH MATERIAL FOLDER
							for(int t=0;t<processingDisplayOrderList.size();t++)
							{
								displayOrderDetails = (DisplayOrderDetails)processingDisplayOrderList.get(t);
								// UPDATE LOCALE, MODEL TYPE, SCHEDULE ID * ITEM ID
								displayOrderDetails.setModelType(itemDetails.getModelType());
								displayOrderDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
								displayOrderDetails.setLocale(itemDetails.getLocale());
								displayOrderDetails.setItemId(String.valueOf(itemDetails.getItemId()));
								
								if(null!=displayOrderDetails.getLineType() && displayOrderDetails.getLineType().equals(ConversionUtils.LINE_TYPE_VALID) && 
										null!=displayOrderDetails.getFilePath() && !"".equals(displayOrderDetails.getFilePath()))
								{
									if(null!=displayOrderDetails.getModelFolderName() && null!=displayOrderDetails.getManualType() && null!=displayOrderDetails.getFaceLiftFolderName() && null!=displayOrderDetails.getMaterialFolderName())
									{
										path = displayOrderDetails.getLocale()+"\\"+displayOrderDetails.getModelFolderName()+"\\"+displayOrderDetails.getManualType()+"\\"+displayOrderDetails.getFaceLiftFolderName()+"\\"+displayOrderDetails.getMaterialFolderName();
										if(path.trim().toLowerCase().equals(uniqueMaterialFolderPathsList.get(o).toString().trim().toLowerCase()))
										{
											// path matches - now check document already added in the list
											boolean add = true;
											if(null!=documentsList && documentsList.size()>0)
											{
												for(int p=0;p<documentsList.size();p++)
												{
													contentDetails = (ContentDetails)documentsList.get(p);
													if(contentDetails.getFilePath().trim().toLowerCase().equals(displayOrderDetails.getFilePath().trim().toLowerCase()))
													{
														// file already added
														add = false;
														String check="";
														check = returnKey(check, displayOrderDetails);
														// check id displayOrder is added or Not
														boolean dispAlreadyAdded = false;
														if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
														{
															for(int x=0;x<contentDetails.getApplicableDisplayOrderList().size();x++)
															{
																existingDisplayOrderDetails = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(x);
																String exist = "";
																exist=  returnKey(exist, existingDisplayOrderDetails);
																if(check.trim().toLowerCase().equals(exist.trim().toLowerCase()))
																{
																	// displayOrder Already added = do not add
																	dispAlreadyAdded=true;
																	break;
																}
																exist = null;
																existingDisplayOrderDetails = null;
															}
														}
														check = null;
														if(dispAlreadyAdded==false)
														{
															// add this DisplayOrder to Document
															if(null==contentDetails.getApplicableDisplayOrderList() || contentDetails.getApplicableDisplayOrderList().size()<=0)
															{
																contentDetails.setApplicableDisplayOrderList(new ArrayList<DisplayOrderDetails>());
															}
															contentDetails.getApplicableDisplayOrderList().add(displayOrderDetails);
														}
														break;
													}
													contentDetails = null;
												}
											}
											
											if(add ==true)
											{
												contentDetails = new ContentDetails();
												contentDetails.setLocale(displayOrderDetails.getLocale());
												contentDetails.setFilePath(displayOrderDetails.getFilePath());
												contentDetails.setModel(displayOrderDetails.getModel());
												contentDetails.setModelFolderName(displayOrderDetails.getModelFolderName());
												contentDetails.setModelType(displayOrderDetails.getModelType());
												contentDetails.setManualType(displayOrderDetails.getManualType());
												
												if(null==contentDetails.getApplicableDisplayOrderList())
												{
													contentDetails.setApplicableDisplayOrderList(new ArrayList<DisplayOrderDetails>());
												}
												contentDetails.getApplicableDisplayOrderList().add(displayOrderDetails);
												if(null==documentsList || documentsList.size()<=0)
												{
													documentsList = new ArrayList<ContentDetails>();
												}
												documentsList.add(contentDetails);
												contentDetails = null;
											}
										}
										path = null;
									}
								}
								else
								{
									// increment invalidLinesCount
									invalidLinesCount++;
									displayOrderDetails.setProcessingStatus("FAILURE");
									displayOrderDetails.setErrorCode("DSP000");
									displayOrderDetails.setErrorMessage("DISPLAY ORDER ROW IS NOT A VALID DATA.");
									if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
									{
										finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
									}
									finalDisplayOrderList.add(displayOrderDetails);
									// remove this row from processingDisplayOrderList
									processingDisplayOrderList.remove(t);
									t--;
								}
								displayOrderDetails = null;
							}
							
							/*
							 *  Unique Documents List identified for Material Folder
							 *  with Applicable DisplayOrder List
							 *  Identify Applicable VIN List for each Document
							 */
							displayOrderDetails = null;
							if(null==documentsList || documentsList.size()<=0)
							{
								documentsList = new ArrayList<ContentDetails>();
							}
							logger.info("startProcessingDisplayOrderForFaceLift :: Unique Documents for Material Folder {"+uniqueMaterialFolderPathsList.get(o)+"} are :: > " + documentsList.size());
							if(null!=documentsList && documentsList.size()>0)
							{
								contentDetails = null;
								for(int g=0;g<documentsList.size();g++)
								{
									contentDetails = (ContentDetails)documentsList.get(g);
									if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 
											&& null!=allVINList && allVINList.size()>0)
									{
										vinDetails = null;
										displayOrderDetails = null;
										for(int u=0;u<allVINList.size();u++)
										{
											vinDetails = (VinDetails)allVINList.get(u);
											if(null!=vinDetails.getDisplayOrderTextFileName() && null!=vinDetails.getMaterialFolder())
											{
												for(int r=0;r<contentDetails.getApplicableDisplayOrderList().size();r++)
												{
													displayOrderDetails = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(r);
													if(null!=displayOrderDetails.getDisplayOrderSourceFileName() && null!=displayOrderDetails.getMaterialFolderName())
													{
														// MATERIAL FOLDER NAME MUST ALSO MATCH
														// ELSE SOMETIMES DUPLICATE ROWS MAY ALSO GET CRATED IN VC TABLE
														if(displayOrderDetails.getDisplayOrderSourceFileName().trim().toLowerCase().equals(vinDetails.getDisplayOrderTextFileName().trim().toLowerCase())
																&& displayOrderDetails.getMaterialFolderName().trim().toLowerCase().equals(vinDetails.getMaterialFolder().trim().toLowerCase()))
														{
															// add this VIN
															if(null==contentDetails.getApplicableVINList() || contentDetails.getApplicableVINList().size()<=0)
															{
																contentDetails.setApplicableVINList(new ArrayList<VinDetails>());
															}
															contentDetails.getApplicableVINList().add(vinDetails);
															break;
														}
													}
													displayOrderDetails = null;
												}
											}
											vinDetails = null;
										}
									}
								}
							}
							
							List<DisplayOrderDetails> tList = mmeDocumentManagementDAO.saveDisplayOrderDetails(documentsList, itemDetails.getModelType(), "CHECK_INSIDE_LIST", channelFolderName, itemDetails.getLocale(), model, itemDetails.getManualType());
							if(null!=tList && tList.size()>0)
							{
								displayOrderDetails = null;
								boolean namesFound = true;
								for(int a=0;a<tList.size();a++)
								{
									displayOrderDetails =(DisplayOrderDetails)tList.get(a);
									if(null!=displayOrderDetails.getProcessingStatus() && displayOrderDetails.getProcessingStatus().equals("SUCCESS"))
									{
										/*
										 * CHECK HERE IF NAMES AVAILABLE FOR ALL DISPLAY ORDER CODES OR NOT
										 * IF ALL AVAILABLE - THEN SUCCESS - update successCount
										 * ELSE FAILURE - WITH ERROR MESSAGE = DISPLAY ORDER NAMES COULD NOT BE LOCATED.
										 * 	updateFailureCount
										 */
										namesFound = true;
										// Start with Level 1
										// Display order CODES are no longer supplied - the file carries the NAMES themselves,
										// so there is no code left to find a name for and nothing to validate here.
										
										if(namesFound==false)
										{
											// set STATUS AS FAILURE
											displayOrderDetails.setProcessingStatus("FAILURE");
											displayOrderDetails.setErrorCode("DSP001");
											displayOrderDetails.setErrorMessage("FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES IN THE ROW.");
										}
									}
									displayOrderDetails = null;
								}
								
								// iDENITFY SUCCESS & FAILURE COUNT FROM THE TLIST AS IT IS SPECIFIC FOR THIS PROCESSING ITEM
								displayOrderDetails = null;
								for(int a=0;a<tList.size();a++)
								{
									displayOrderDetails = (DisplayOrderDetails)tList.get(a);
									if(null!=displayOrderDetails.getProcessingStatus() && displayOrderDetails.getProcessingStatus().equals("SUCCESS"))
									{
										successCount++;
									}
									else
									{
										failureCount++;
									}
								}
								
								// add to Final Processing List
								if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
								{
									finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
								}
								finalDisplayOrderList.addAll(tList);
							}
							else
							{
								// ADD ALL PROCESSING DISPLAY LIST COUNT AS FAILURE COUNT FOR ITEM & SCHEDULE FOR PROCESSING MATERIAL FOLDER + INVALID LINES COUNT
								if(null!=processingDisplayOrderList && processingDisplayOrderList.size()>0)
								{
									// iterate and set Processing status as Failure for all rows in ProcessingDisplayOrderList
									displayOrderDetails = null;
									path = null;
									for(int e=0;e<processingDisplayOrderList.size();e++)
									{
										displayOrderDetails = (DisplayOrderDetails)processingDisplayOrderList.get(e);
										if(null!=displayOrderDetails.getModelFolderName() && null!=displayOrderDetails.getManualType() && null!=displayOrderDetails.getFaceLiftFolderName() && null!=displayOrderDetails.getMaterialFolderName())
										{
											path = displayOrderDetails.getLocale()+"\\"+displayOrderDetails.getModelFolderName()+"\\"+displayOrderDetails.getManualType()+"\\"+displayOrderDetails.getFaceLiftFolderName()+"\\"+displayOrderDetails.getMaterialFolderName();
										}
										if(null!=path && path.trim().toLowerCase().equals(uniqueMaterialFolderPathsList.get(o).toString().trim().toLowerCase()))
										{
											// INCREMENT FAILURE COUNT
											failureCount++;
											// set STATUS AS FAILURE
											displayOrderDetails.setProcessingStatus("FAILURE");
											displayOrderDetails.setErrorCode("DSP010");
											displayOrderDetails.setErrorMessage("FAILED TO PERFORM DATABASE OPERATIONS FOR THE DISPLAY ORDER ROW. PLEASE CONTACT IT SUPPORT TEAM.");
											if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
											{
												finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
											}
											finalDisplayOrderList.add(displayOrderDetails);
										}
										displayOrderDetails = null;
										path = null;
									}
								}
							}
							tList = null;
							documentsList = null;
							
							logger.info("startProcessingDisplayOrderForFaceLift :: ------------ Ending ------------------------ >"+ uniqueMaterialFolderPathsList.get(o)+" at :: >"+ new Date());
						}
						
						contentDetails  =null;
						displayOrderDetails = null;
						existingDisplayOrderDetails =  null;
						documentsList =  null;
						
						/*
						 * UPDTAE SUCCESS / FAILURE COUNT FOR SCHEDULE & ITEM FOR DISPLAY ORDER
						 * FAILURE COUNT = FAILURE COUNT + INVALID LINES COUNT
						 */
						ScheduleDAO.updateDISPCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), successCount,(failureCount+invalidLinesCount));
						
					}
					uniqueMaterialFolderPathsList= null;
				}
				else
				{
					logger.info("startProcessingDisplayOrderForFaceLift :: No Display Order Information Found for Processing Display Order for FaceLift Folder :: > " + faceLiftDirFolder.getName());
				}
				processingDisplayOrderList = null;
			}
			else
			{
				logger.info("startProcessingDisplayOrderForFaceLift :: No Display Order Information Found for Processing in FaceLift Folder at Path :: > " + PathUtil.winPath(faceLiftDirFolder));
			}
			channelFolderName = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startProcessingDisplayOrderForFaceLift()", e);
		}
	}
	
	private void startCDProcessing(File materialDirFolder, ScheduleItemDetails itemDetails, String wslId, ArrayList<CDProcessingDetails> cdProcessingList,String channelFolderName)
	{
		try
		{
			logger.info("startCDProcessing :: Processing MaterialDirFolder Directory at Path :: > " + PathUtil.winPath(materialDirFolder));


			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DISPLAY ORDER PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "CD DISPLAY ORDER PROCESSING");

			ArrayList<CDProcessingDetails> processingCDDataList = new ArrayList<CDProcessingDetails>();
			
			if(null!=cdProcessingList && cdProcessingList.size()>0)
			{
				// UPDATE SCHEDULE ID & LOCALE IN THE CD PROCESSING LIST FOR ALL ROWS
				for(CDProcessingDetails details : cdProcessingList)
				{
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					
					/*
					 * CHECK MODEL FOLDER NAME, MANUAL TYPE, FACELIFT FOLDER AND MATEIAL FOLDER NAME
					 * MUST MATCH, ONLY THOSE ENTRIES WILL BE PROCESSED FURTHER
					 */
					if(null!=details.getModelFolderName() && null!=details.getManualType() && null!=details.getFaceLiftFolderName() && null!=details.getMaterialFolderName() 
						 && null!=itemDetails.getModelFolderName() && null!=itemDetails.getManualType() && null!=itemDetails.getFaceLiftFolderName() && null!=itemDetails.getMaterialFolderName())
					{
						if(details.getModelFolderName().trim().toLowerCase().equals(itemDetails.getModelFolderName().trim().toLowerCase()) 
						&& details.getManualType().trim().toLowerCase().equals(itemDetails.getManualType().trim().toLowerCase()) 
						&& details.getFaceLiftFolderName().trim().toLowerCase().equals(itemDetails.getFaceLiftFolderName().trim().toLowerCase()) 
						&& details.getMaterialFolderName().trim().toLowerCase().equals(itemDetails.getMaterialFolderName().trim().toLowerCase()))
						{
							// add to Processing CD Data List
							processingCDDataList.add(details);
						}
					}
				}
				
				if(null!=processingCDDataList && processingCDDataList.size()>0)
				{
					/*
					 *  REQUIRED PARAMS - CD LIST AND MODEL TYPE & 
					 *  WHETHER PATH IN THE CD PROCESSING FILE NEEDS TO BE REPLACED WITH PDF & HTML WHILE CHECKING.
					 *  
					 *  DO NOT PERFORM THIS CHECK HERE BECAUSE OF HTML5 PROCESSING 
					 *
					String pathToBeReplaced="NO";
					if(null!=itemDetails.getMaterialFolderName() && !"".equals(itemDetails.getMaterialFolderName()))
					{
						// APPLICABLE ONLY FOR WD
						if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
						{
							String tok="";
							String mtName = itemDetails.getMaterialFolderName();
							if(mtName.lastIndexOf("_")!=-1)
							{
								tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
							}
							if(null==tok)
							{
								tok="";
							}
							// DO NOT CHECK FOR NULL
							if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
									!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
									!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
									!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
							{
								// set replaceByPDFTO HTML applicable for this case only
								pathToBeReplaced="YES";
							}
							tok = null;
							mtName = null;
						}
					}
					*/
					
					String pathToBeReplaced="CHECK_INSIDE_LIST";
					/*
					 * CALL DATABASE FUNCTION - ONLY FOR CD PROCESSING FOR THIS PARTICULAR MATERIAL FOLDER
					 */
					ArrayList<CDProcessingDetails> tempList = mmeDocumentManagementDAO.saveCDProcessingDetails(processingCDDataList, itemDetails.getModelType(), pathToBeReplaced, channelFolderName, itemDetails.getLocale());
					if(null!=tempList && tempList.size()>0)
					{
						if(null==finalCDProcessingList || finalCDProcessingList.size()<=0)
						{
							finalCDProcessingList = new ArrayList<CDProcessingDetails>();
						}
						finalCDProcessingList.addAll(tempList);
					}
					tempList= null;
				}
				else
				{
					logger.info("startCDProcessing :: No Information Found for Processing CD DATA for Material Folder :: > " + itemDetails.getMaterialFolderName());
				}
				processingCDDataList = null;
			}
			else
			{
				logger.info("startCDProcessing :: No Information Found for Processing CD Data in Material Folder at Path :: > " + PathUtil.winPath(materialDirFolder));
			}
			channelFolderName = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startCDProcessing()", e);
		}
	}

	private void startCDProcessingForFaceLift(File faceLiftDir, ScheduleItemDetails itemDetails, String wslId, ArrayList<CDProcessingDetails> cdProcessingList,String channelFolderName)
	{
		try
		{
			logger.info("startCDProcessingForFaceLift :: Processing FaceLift Directory at Path :: > " + PathUtil.winPath(faceLiftDir));
			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DISPLAY ORDER PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "CD DISPLAY ORDER PROCESSING");

			ArrayList<CDProcessingDetails> processingCDDataList = new ArrayList<CDProcessingDetails>();
			
			if(null!=cdProcessingList && cdProcessingList.size()>0)
			{
				// UPDATE SCHEDULE ID & LOCALE IN THE CD PROCESSING LIST FOR ALL ROWS
				for(CDProcessingDetails details : cdProcessingList)
				{
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					
					/*
					 * CHECK MODEL FOLDER NAME, MANUAL TYPE, FACELIFT FOLDER AND MATEIAL FOLDER NAME
					 * MUST MATCH, ONLY THOSE ENTRIES WILL BE PROCESSED FURTHER
					 */
					if(null!=details.getModelFolderName() && null!=details.getManualType() && null!=details.getFaceLiftFolderName()  
						 && null!=itemDetails.getModelFolderName() && null!=itemDetails.getManualType() && null!=itemDetails.getFaceLiftFolderName())
					{
						if(details.getModelFolderName().trim().toLowerCase().equals(itemDetails.getModelFolderName().trim().toLowerCase()) 
						&& details.getManualType().trim().toLowerCase().equals(itemDetails.getManualType().trim().toLowerCase()) 
						&& details.getFaceLiftFolderName().trim().toLowerCase().equals(itemDetails.getFaceLiftFolderName().trim().toLowerCase()))
						{
							// add to Processing CD Data List
							processingCDDataList.add(details);
						}
					}
				}
				
				if(null!=processingCDDataList && processingCDDataList.size()>0)
				{
					/*
					 *  REQUIRED PARAMS - CD LIST AND MODEL TYPE & 
					 *  WHETHER PATH IN THE CD PROCESSING FILE NEEDS TO BE REPLACED WITH PDF & HTML WHILE CHECKING.
					 */
					String pathToBeReplaced="CHECK_INSIDE_LIST";
					/*
					 * CALL DATABASE FUNCTION - ONLY FOR CD PROCESSING FOR THIS PARTICULAR MATERIAL FOLDER
					 */
					ArrayList<CDProcessingDetails> tempList = mmeDocumentManagementDAO.saveCDProcessingDetails(processingCDDataList, itemDetails.getModelType(), pathToBeReplaced, channelFolderName, itemDetails.getLocale());
					if(null!=tempList && tempList.size()>0)
					{
						if(null==finalCDProcessingList || finalCDProcessingList.size()<=0)
						{
							finalCDProcessingList = new ArrayList<CDProcessingDetails>();
						}
						finalCDProcessingList.addAll(tempList);
					}
					tempList= null;
				}
				else
				{
					logger.info("startCDProcessingForFaceLift :: No Information Found for Processing CD DATA for FaceLift Folder :: > " + itemDetails.getFaceLiftFolderName());
				}
				processingCDDataList = null;
			}
			else
			{
				logger.info("startCDProcessingForFaceLift :: No Information Found for Processing CD Data in FaceLift Folder at Path :: > " + PathUtil.winPath(faceLiftDir));
			}
			channelFolderName = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startCDProcessingForFaceLift()", e);
		}
	}
	
	private void startSCMVINProcessing(File materialDirFolder, ScheduleItemDetails itemDetails, String wslId, ArrayList<VinDetails> applicableSCMVINList)
	{
		try
		{
			logger.info("startSCMVINProcessing :: Processing MaterialDirFolder Directory at Path :: > " + PathUtil.winPath(materialDirFolder));


			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS DISPLAY ORDER PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "SCM VIN PROCESSING");
			
			if(null!=applicableSCMVINList && applicableSCMVINList.size()>0)
			{
				// UPDATE SCHEDULE ID & LOCALE IN THE SCM VIN PROCESSING LIST FOR ALL ROWS
				for(VinDetails details : applicableSCMVINList)
				{
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					details = null;
				}
				
				if(null!=applicableSCMVINList && applicableSCMVINList.size()>0)
				{
					/*
					 * CALL DATABASE FUNCTION - ONLY FOR SCM PROCESSING FOR THIS PARTICULAR MATERIAL FOLDER
					 */
					ArrayList<VinDetails> tempList = mmeDocumentManagementDAO.saveSCMVinProcessingDetails(applicableSCMVINList, itemDetails.getModelType(), itemDetails.getLocale(), String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
					if(null!=tempList && tempList.size()>0)
					{
						if(null==finalSCMVINProcessingList || finalSCMVINProcessingList.size()<=0)
						{
							finalSCMVINProcessingList = new ArrayList<VinDetails>();
						}
						finalSCMVINProcessingList.addAll(tempList);
					}
					tempList= null;
				}
				else
				{
					logger.info("startSCMVINProcessing :: No Information Found for Processing SCM VIN DATA for Material Folder :: > " + itemDetails.getMaterialFolderName());
				}
			}
			else
			{
				logger.info("startSCMVINProcessing :: No Information Found for Processing SCM VIN Data in Material Folder at Path :: > " + PathUtil.winPath(materialDirFolder));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startSCMVINProcessing()", e);
		}
	}

	private void startSCMVINProcessingForFaceLift(File faceLiftDir, ScheduleItemDetails itemDetails, String wslId, ArrayList<VinDetails> scmVINList)
	{
		try
		{
			logger.info("startSCMVINProcessingForFaceLift :: Processing FaceLift Directory at Path :: > " + PathUtil.winPath(faceLiftDir));
			/*
			 * 
			 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS SCM VIN MAPPING PROCESSING 
			 * 
			 */
			ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "SCM VIN PROCESSING");
			
			if(null!=scmVINList && scmVINList.size()>0)
			{
				// UPDATE SCHEDULE ID & LOCALE IN THE SCM VIN PROCESSING LIST FOR ALL ROWS
				for(VinDetails details : scmVINList)
				{
					details.setModelType(itemDetails.getModelType());
					details.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
					details.setLocale(itemDetails.getLocale());
					details.setItemId(String.valueOf(itemDetails.getItemId()));
					details = null;
				}
				
				if(null!=scmVINList && scmVINList.size()>0)
				{
					
					/*
					 * CALL DATABASE FUNCTION - ONLY FOR SCM PROCESSING FOR THIS PARTICULAR FACELIFT FOLDER
					 */
					ArrayList<VinDetails> tempList = mmeDocumentManagementDAO.saveSCMVinProcessingDetails(scmVINList, itemDetails.getModelType(), itemDetails.getLocale(), String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()));
					if(null!=tempList && tempList.size()>0)
					{
						if(null==finalSCMVINProcessingList || finalSCMVINProcessingList.size()<=0)
						{
							finalSCMVINProcessingList = new ArrayList<VinDetails>();
						}
						finalSCMVINProcessingList.addAll(tempList);
					}
					tempList= null;
				}
				else
				{
					logger.info("startSCMVINProcessingForFaceLift :: No Information Found for Processing SCM VIN MAPPING DATA for FaceLift Folder :: > " + itemDetails.getFaceLiftFolderName());
				}
			}
			else
			{
				logger.info("startSCMVINProcessingForFaceLift :: No Information Found for Processing SCM VIN MAPPING Data in FaceLift Folder at Path :: > " + PathUtil.winPath(faceLiftDir));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(),"startSCMVINProcessingForFaceLift()", e);
		}
	}

	
	private void processDataForDocumentDeletion(DeleteFileDetails deleteFileDetails, ScheduleItemDetails itemDetails, String wslId, String channelFolderName)
	{
		try
		{
			if(null!=deleteFileDetails && null!=itemDetails)
			{
				ContentDetails contentDetails = new ContentDetails();
				contentDetails.setFileName(deleteFileDetails.getFileToBeDeletedName());
				contentDetails.setFilePath(deleteFileDetails.getFileToBeDeletedPath());
				contentDetails.setFileAbsolutePath(deleteFileDetails.getFileToBeDeletedPath());
				
				contentDetails.setLocale(itemDetails.getLocale());
				contentDetails.setMarket(itemDetails.getMarket());
				contentDetails.setModel(itemDetails.getModel());
				contentDetails.setModelType(itemDetails.getModelType());
				contentDetails.setCarlineCode(itemDetails.getCarlineCode());
				contentDetails.setModelFolderName(itemDetails.getModelFolderName());
				contentDetails.setFaceLiftFolderName(itemDetails.getFaceLiftFolderName());
				contentDetails.setManualType(itemDetails.getManualType());
				contentDetails.setMaterialName(itemDetails.getMaterialFolderName());
				// SET THREAD ID
				contentDetails.setThreadId(itemDetails.getThreadId());
				
				contentDetails.setWslId(wslId);
				contentDetails.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
				contentDetails.setItemId(String.valueOf(itemDetails.getItemId()));
				
				if(null!=channelFolderName && !"".equals(channelFolderName))
				{
					if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
					{
						// SET CHANNEL NAME - SERVICE MANUALS
						contentDetails.setChannelName(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME"));
					}
					else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()))
					{
						// SET CHANNEL NAME - SERVICE MANUALS
						contentDetails.setChannelName(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME"));
					}
					else if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
					{
						// SET CHANNEL NAME - OTHER SERVICE MANUALS
						contentDetails.setChannelName(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME"));
					}
				}
				
				// SET ACTUAL FILE PATH
				if(null!=contentDetails.getFilePath() && !"".equals(contentDetails.getFilePath()) && null!=contentDetails.getLocale()
						&& !"".equals(contentDetails.getLocale()))
				{
					if(contentDetails.getFilePath().lastIndexOf(contentDetails.getLocale())!=-1)
					{
						contentDetails.setFilePath(contentDetails.getFilePath().substring(contentDetails.getFilePath().lastIndexOf(contentDetails.getLocale()), contentDetails.getFilePath().length()));
					}
				}
				
				/*
				 * IDENTIFY DOCUMENT TYPE
				 * HERE, CHECK IF MANUAL TYPE IS WD OR E-WD - THEN SET DOCUMENT DOCUMENT AS HTML_DOCUMENT
				 * ELSE GO WITH NORMAL PROCESS
				 */
				boolean isWD = false;
				if(null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType()))
				{
					if(itemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
							itemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder"))	)
					{
						// PROCESSING FOLDER IS WD
						isWD = true;
					}
				}
				
				if(isWD==true)
				{
					/*
					 *  IF CONTENT DETAILS. FILE PATH CONTAINS HTML5 - DO NOT CHECK FOR MATERIAL FOLDER EXTENSION
					 */
					if(!contentDetails.getFilePath().trim().toLowerCase().contains("\\"+ApplicationProperties.getProperty("directory.html5")+"\\"))
					{
						/*
						 * IF MATERIAL FOLDER NAME DOESN'T ENDS WITH HTML / PDF
						 * THEN DOCUMENT TO BE LOOKED BY PDF AND HTML BOTH
						 * EITHER OF IT WILL PROVIDE THE DOCUMENT ID.
						 * 
						 * HERE, THE 
						 */
						String tok="";
						String mtName = contentDetails.getMaterialName();
						if(mtName.lastIndexOf("_")!=-1)
						{
							tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
						}
						if(null==tok)
						{
							tok="";
						}
						// DO NOT CHECK FOR NULL
						if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
						{
							// contains BOTH PDF & HTML, REMOVE EXTENSION
							contentDetails.setReplacePDFBYHTML("YES");
						}
						tok = null;
					}
				}
				
				/*
				 * CALL FUNCTION TO DELETE DOCUMENT
				 */
				performDocumentOperationsForDelete(contentDetails);
			}
			else
			{
				logger.info("processDataForDocumentDeletion :: Parameters are null. ");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "processDataForDocumentDeletion()", e);
		}
	}
	
	private void performDocumentOperationsForDelete(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails)
			{
				/*
				 * IDENTIFY DOCUMENT WHETEHR EXISTING OR NEW
				 * 	on the basis of Model Type, identify which database table to be look upon
				 * 	if NEWM - THEN NEW MODEL TABLES
				 * 	Else OTHER MODEL TABLES
				 */
				String fPath = contentDetails.getFilePath();
				ContentDetails existingDetails  = new ContentDetails();
				if(null!=contentDetails.getReplacePDFBYHTML() && contentDetails.getReplacePDFBYHTML().equals("YES"))
				{
					// GET DOCUMENT EITHER WITH BOTH PDF OR HTML
					existingDetails = mmeDocumentManagementDAO.getDocumentDetailsWD(fPath, contentDetails.getModelType(), contentDetails.getLocale());
				}
				else
				{
					// GET SM DOCUMENT
					existingDetails = mmeDocumentManagementDAO.getDocumentDetails(fPath, contentDetails.getModelType(), contentDetails.getLocale());
				}
				
				if(null!=existingDetails && null!=existingDetails.getImDocumentId() && !"".equals(existingDetails.getImDocumentId()) && 
						null!=existingDetails.getImContentType() && !"".equals(existingDetails.getImContentType()))
				{
					/*
					 * PROCEED FOR DELETE OF DOCUMENT
					 */
					contentDetails.setImDocumentId(existingDetails.getImDocumentId());
					contentDetails.setImContentType(existingDetails.getImContentType());
					
					contentDetails = deleteContent(contentDetails);
					if (null != contentDetails && null != contentDetails.getImDocumentId() 	&& !"".equals(contentDetails.getImDocumentId()) 
							&& null!=contentDetails.getImContentType() && !"".equals(contentDetails.getImContentType())) 
					{
						logger.info("performDocumentOperation :: Document Successfully DELETED in Info Manager For Document Id :: >"+ contentDetails.getImDocumentId());
						/*
						 * DELETE CONTENT DETAILS IN DATABASE
						 */
						boolean flag = mmeDocumentManagementDAO.deleteDocumentDetails(contentDetails);
						
						if (flag == true) 
						{
							logger.info("performDocumentOperation :: Document Details Deleted Successfully in Database For Document Id :: "+ contentDetails.getImDocumentId());
							
							/*
							 * INCREMENT DOCUMENT DELETE PROCESSING COUNT BY 1 & ITEM DETAILS PROCESSING COUNT BY 1
							 */
							ScheduleDAO.updateDeleteProcessingCount(contentDetails.getScheduleId(), contentDetails.getItemId());
							
							
							// INNER LINKS OPERATION
//							addDocsToReUpdateListForInnerLinks(contentDetails);
						} 
						else 
						{
							logger.info("performDocumentOperation :: Failed to Delete Document Details in Database For Document Id :: > "+ contentDetails.getImDocumentId());
							/*
							 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
							 */
							ScheduleDAO.updateDeleteFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
						}
					}
					else
					{
						logger.info("performDocumentOperation :: Failed to Delete Document in Info Manager for File :: > "+ contentDetails.getFilePath());
						/*
						 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
						 */
						ScheduleDAO.updateDeleteFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
					}
				}
				else
				{
					/*
					 * CAPTURE ERROR DETAILS 
					 */
					ErrorDetails errorDetails = new ErrorDetails();
					errorDetails.setErrorCode("IMEDXP001");
					errorDetails.setErrorMessage("FAILED TO IDENTIFY DOCUMENT IN DATABASE.");
					errorDetails.setContentDetails(contentDetails);
					// add current time stamp
					errorDetails.setDateTime(new Timestamp(new Date().getTime()));
					errorDetails.setOperationType("DELETE_CONTENT");
					if(null==imErrorDeleteDetailsList || imErrorDeleteDetailsList.size()<=0)
					{
						imErrorDeleteDetailsList = new ArrayList<ErrorDetails>();
					}
					imErrorDeleteDetailsList.add(errorDetails);
					errorDetails=  null;
					
					/*
					 * INCREMENT DOCUMENT FAILURE COUNT BY 1 & ITEM DETAILS FAILURE COUNT BY 1
					 */
					ScheduleDAO.updateDeleteFailureCount(contentDetails.getScheduleId(), contentDetails.getItemId());
				}
				
				/*
				 * ADD CONTENT DETAILS TO PROCESSING FILE DETAILS
				 */
				if(null==processingDeleteFileDetailsList || processingDeleteFileDetailsList.size()<=0)
				{
					processingDeleteFileDetailsList = new ArrayList<ContentDetails>();
				}
				processingDeleteFileDetailsList.add(contentDetails);
				
				fPath= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "performDocumentOperationsForDelete()", e);
		}
	}
	
	
		/**
	 * DELETE = UNPUBLISH. The document is unpublished in Kapture and nothing else about it is
	 * changed - no attribute is blanked and no category is touched.
	 */
	public ContentDetails deleteContent(ContentDetails contentDetails)
	{
		String operationType="DELETE_CONTENT";
		try
		{
			contentDetails.setOperationType(operationType);
			if(kaptureAvailable(contentDetails, operationType)==false)
			{
				return contentDetails;
			}

			KaptureRecordResult result = kapture.unpublish(contentDetails.getImDocumentId(),
					KaptureArticleBuilder.channelType(contentDetails), kaptureLocale);
			if (result.success)
			{
				logger.info("deleteDocument :: Document unpublished successfully for File {"+ contentDetails.getFilePath()	+ "} :: Document Id :: > "	+ result.documentId);
				applyKaptureResult(contentDetails, result);
			}
			else
			{
				// Explicitly set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);
				logger.info("deleteContent :: Failed to Unpublish Document for File :: >" 	+ contentDetails.getFilePath());
				addKaptureError(result.errorCode, "FAILED TO UNPUBLISH DOCUMENT IN KAPTURE. " + result.message, contentDetails, operationType);
			}
			result = null;
		}
		catch (Exception e)
		{
			// explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return contentDetails;
	}

	private String identifyStatusForMaterialFolder(String materialFolderPath)
	{
		String status="SUCCESS";
		try
		{
			boolean cdProcessingFailure = false;
			boolean displayOrderFailure=false;
			boolean transactionsFailure=false;
			boolean missingCategoryFailure=false;
			boolean viewContentFailure=false;
			boolean innerLinksFailure=false;
			boolean vinMLMappingFailure=false;
			boolean scmVINMappingFailure = false;
			
			// START WITH SCM VIN
			if(null!=finalSCMVINProcessingList && finalSCMVINProcessingList.size()>0)
			{
				VinDetails details = null;
				for(int a=0;a<finalSCMVINProcessingList.size();a++)
				{
					details = (VinDetails)finalSCMVINProcessingList.get(a);
					if(null!=details.getDocSourceFilePath() && !"".equals(details.getDocSourceFilePath()))
					{
						if(details.getDocSourceFilePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
						{
							if(null==details.getProcessingStatus() || (null!=details.getProcessingStatus() && details.getProcessingStatus().trim().toLowerCase().equals("failure")))
							{
								// material folder is failed
								scmVINMappingFailure = true;
								status = "FAILURE";
								break;
							}
						}
					}
					details = null;
				}
				details = null;
			}
			
			if(scmVINMappingFailure==false)
			{
				// check with CD
				if(null!=finalCDProcessingList && finalCDProcessingList.size()>0)
				{
					CDProcessingDetails details = null;
					for(int a=0;a<finalCDProcessingList.size();a++)
					{
						details = (CDProcessingDetails)finalCDProcessingList.get(a);
						if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
						{
							if(details.getFilePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if(null==details.getProcessingStatus() || (null!=details.getProcessingStatus() && details.getProcessingStatus().trim().toLowerCase().equals("failure")))
								{
									// material folder is failed
									cdProcessingFailure = true;
									status = "FAILURE";
									break;
								}
							}
						}
						details = null;
					}
					details = null;
				}
			}
			
			if(cdProcessingFailure==false)
			{
				// NO ERRORS IN CD PROCESSING FOUND - CHECK FOR DISPLAY ORDER
				if(null!=finalDisplayOrderList && finalDisplayOrderList.size()>0)
				{
					DisplayOrderDetails details = null;
					for(int a=0;a<finalDisplayOrderList.size();a++)
					{
						details = (DisplayOrderDetails)finalDisplayOrderList.get(a);
						if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
						{
							if(details.getFilePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if(null==details.getProcessingStatus() || (null!=details.getProcessingStatus() && details.getProcessingStatus().trim().toLowerCase().equals("failure")))
								{
									// material folder is failed
									displayOrderFailure = true;
									status = "FAILURE";
									break;
								}
							}
						}
						details = null;
					}
					details = null;
				}
			}
			
			if(displayOrderFailure==false)
			{
				ArrayList<ErrorDetails> imerrorsList = new ArrayList<ErrorDetails>();
				// NO ERRORS IN DISPLAY ORDER FOUND CHECK FOR FAILURE REPORT
				if(null!=imErrorDetailsList && imErrorDetailsList.size()>0)
				{
					imerrorsList.addAll(imErrorDetailsList);
				}
				if(null!=imErrorDeleteDetailsList && imErrorDeleteDetailsList.size()>0)
				{
					imerrorsList.addAll(imErrorDeleteDetailsList);
				}
				
				boolean proceedFurther= true;
				if(null!=imerrorsList && imerrorsList.size()>0)
				{
					ErrorDetails errors = null;
					ContentDetails details=null;
					for(int r=0;r<imerrorsList.size();r++)
					{
						errors = (ErrorDetails)imerrorsList.get(r);
						details = new ContentDetails();
						if(null!=errors.getContentDetails())
						{
							details = errors.getContentDetails();
						}
						if(null!=details.getFileAbsolutePath() && !"".equals(details.getFileAbsolutePath()))
						{
							if(details.getFileAbsolutePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								// in failure report -  material folder is failed
								transactionsFailure = true;
								proceedFurther=false;
								status = "FAILURE";
								break;
							}
						}
						errors=null;
					}
					details = null;
					errors=null;
				}
				imerrorsList = null;
				
				if(proceedFurther==true)
				{
					// check in database failureLists
					ArrayList<Map<Object, Object>> dbErrorList = new ArrayList<Map<Object, Object>>();
					
					if (null != mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails() && mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails().size() > 0) 
					{
						dbErrorList.addAll(mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetails());
					}
					
					if(null!=mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete() && mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete().size()>0)
					{
						dbErrorList.addAll(mmeDocumentManagementDAO.getFailedDatabaseSaveDocumentDetailsForDelete());
					}
					
					if(null!=dbErrorList && dbErrorList.size()>0)
					{
						Map<Object, Object> dataMap = null;
						ContentDetails con = null;
						for (int i = 0; i < dbErrorList.size(); i++) 
						{
							dataMap = (HashMap<Object, Object>) dbErrorList.get(i);
							
							con = new ContentDetails();
							if(null!=dataMap.get("CONTENT_DETAILS") && !"".equals(dataMap.get("CONTENT_DETAILS")))
							{
								con =(ContentDetails) dataMap.get("CONTENT_DETAILS");
							}
							if(null!=con.getFileAbsolutePath() && !"".equals(con.getFileAbsolutePath()))
							{
								if(con.getFileAbsolutePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
								{
									// in failure report -  material folder is failed
									transactionsFailure = true;
									proceedFurther=false;
									status = "FAILURE";
									break;
								}
							}
							dataMap = null;
						}
						dataMap = null;
						con = null;
					}
					dbErrorList = null;
				}
				
				if(proceedFurther==true)
				{
					// XCOPY FAILURE LIST
					if(null!=xcopyUtils.getFileUploadFailureList() && xcopyUtils.getFileUploadFailureList().size()>0)
					{
						Map<Object, Object> dataMap = null;
						for (int i = 0; i < xcopyUtils.getFileUploadFailureList().size(); i++) 
						{
							dataMap = (HashMap<Object, Object>) xcopyUtils.getFileUploadFailureList().get(i);
							if (null != dataMap.get("SOURCE_PATH")) 
							{
								if(dataMap.get("SOURCE_PATH").toString().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
								{
									transactionsFailure = true;
									status = "FAILURE";
									break;
								}
							}
							dataMap = null;
						}
						dataMap = null;
					}
				}
				
			}
			
			
			if(transactionsFailure ==false)
			{
				if(null!=missingCategoriesList && missingCategoriesList.size()>=0)
				{
					CategoryDetails catDetails=null;
					ContentDetails con=null;
					for (int a = 0; a < missingCategoriesList.size(); a++) 
					{
						catDetails = (CategoryDetails) missingCategoriesList.get(a);
						con = new ContentDetails();
						if(null!=catDetails.getContentDetails() && !"".equals(catDetails.getContentDetails()))
						{
							con = catDetails.getContentDetails();
						}
						if(null!=con.getFileAbsolutePath() && !"".equals(con.getFileAbsolutePath()))
						{
							if(con.getFileAbsolutePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								// in missingCategories report -  material folder is failed
								missingCategoryFailure = true;
								status = "FAILURE";
								break;
							}
						}
						catDetails = null;
					}
					catDetails = null;
					con = null;
				}
			}
			
			if(missingCategoryFailure==false)
			{
				if(null!=mmeDocumentManagementDAO.getViewContentDataList() && mmeDocumentManagementDAO.getViewContentDataList().size()>0)
				{
					MCViewContentDetails details=  null;
					for (int a = 0; a < mmeDocumentManagementDAO.getViewContentDataList().size(); a++) 
					{
						details = (MCViewContentDetails) mmeDocumentManagementDAO.getViewContentDataList().get(a);
						if (null != details.getSourceFilePath() && !"".equals(details.getSourceFilePath())) 
						{
							if(details.getSourceFilePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if(null==details.getProcessingStatus() || (null!=details.getProcessingStatus() && details.getProcessingStatus().trim().toLowerCase().equals("failure")))
								{
									// material folder is failed
									viewContentFailure = true;
									status = "FAILURE";
									break;
								}
							}
						}
						details = null;
					}
					details = null;
				}
			}
			
			
			if(viewContentFailure==false)
			{
				if(null!=allInnerLinksList && allInnerLinksList.size()>0)
				{
					ContentDetails con = null;
					LinkDetails linkDetails = null;
					for(int a=0;a<allInnerLinksList.size();a++)
					{
						con = (ContentDetails)allInnerLinksList.get(a);
						if(null!=con.getFileAbsolutePath() && !"".equals(con.getFileAbsolutePath()))
						{
							if(con.getFileAbsolutePath().trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if(null!=con.getInnerLinksList() && con.getInnerLinksList().size()>0)
								{
									linkDetails =  new LinkDetails();
									for(int y=0;y<con.getInnerLinksList().size();y++)
									{
										linkDetails = (LinkDetails)con.getInnerLinksList().get(y);
										if(null==linkDetails.getMapStatus() || !"y".equals(linkDetails.getMapStatus().trim().toLowerCase()))
										{
											// MTERIAL FOLDER IS FAILED
											innerLinksFailure = true;
											status="FAILURE";
											break;
										}
									}
									linkDetails = null;
								}
							}
							
							if(innerLinksFailure==true)
							{
								// break parent loop no need of checking for other rows, 1 failed innerLink found for material folder
								break;
							}
						}
						con = null;
					}
				}
			}
			
			if(innerLinksFailure==false)
			{
				/*
				 * CHECK FOR VOLTAGE MAP & VOLTAGE LINK MAP JS AS WELL
				 */
				if(null!=wiringDiagramUtils.getWindowJSList() && wiringDiagramUtils.getWindowJSList().size()>0)
				{
					WindowJSDetails jsDetails= null;
					ContentDetails con = null;
					String pathToCheck="";
					for(int a=0;a<wiringDiagramUtils.getWindowJSList().size();a++)
					{
						jsDetails =(WindowJSDetails)wiringDiagramUtils.getWindowJSList().get(a);
						con = new ContentDetails();
						if(null!=jsDetails.getContentDetails())
						{
							con = jsDetails.getContentDetails();
						}
						
						if(null!=con && null!=con.getLocale() && !"".equals(con.getLocale()) && null!=con.getModelFolderName() && 
								!"".equals(con.getModelFolderName()) && null!=con.getManualType() && !"".equals(con.getManualType()) 
										&& null!=con.getFaceLiftFolderName() && !"".equals(con.getFaceLiftFolderName()) 
								&& null!=con.getMaterialName() && !"".equals(con.getMaterialName()))
						{
							pathToCheck = con.getLocale().trim()+"\\"+con.getModelFolderName().trim()+"\\"+
									con.getManualType().trim()+"\\"+con.getFaceLiftFolderName().trim()+"\\"+con.getMaterialName();
						}
						if(null!=pathToCheck && !"".equals(pathToCheck))
						{
							if(pathToCheck.trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if((null==jsDetails.getProcessingStatus() || "".equals(jsDetails.getProcessingStatus()))
										|| (null!=jsDetails.getProcessingStatus() && "failure".equals(jsDetails.getProcessingStatus().trim().toLowerCase())))
								{
									// material folder is failure
									vinMLMappingFailure = true;
									status = "FAILURE";
									break;
								}
							}
						}
						pathToCheck  = null;
						jsDetails = null;
					}
					jsDetails = null;
				}
				
				
				// VOLTAGE MAP JS
				if(null!=wiringDiagramUtils.getVoltageMapJSList() && wiringDiagramUtils.getVoltageMapJSList().size()>0)
				{
					WindowJSDetails jsDetails= null;
					ContentDetails con = null;
					String pathToCheck="";
					for(int a=0;a<wiringDiagramUtils.getVoltageMapJSList().size();a++)
					{
						jsDetails =(WindowJSDetails)wiringDiagramUtils.getVoltageMapJSList().get(a);
						con = new ContentDetails();
						if(null!=jsDetails.getContentDetails())
						{
							con = jsDetails.getContentDetails();
						}

						if(null!=con && null!=con.getLocale() && !"".equals(con.getLocale()) && null!=con.getModelFolderName() && 
								!"".equals(con.getModelFolderName()) && null!=con.getManualType() && !"".equals(con.getManualType()) 
								&& null!=con.getFaceLiftFolderName() && !"".equals(con.getFaceLiftFolderName()) 
								&& null!=con.getMaterialName() && !"".equals(con.getMaterialName()))
						{
							pathToCheck = con.getLocale().trim()+"\\"+con.getModelFolderName().trim()+"\\"+
									con.getManualType().trim()+"\\"+con.getFaceLiftFolderName().trim()+"\\"+con.getMaterialName();
						}
						if(null!=pathToCheck && !"".equals(pathToCheck))
						{
							if(pathToCheck.trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if((null==jsDetails.getProcessingStatus() || "".equals(jsDetails.getProcessingStatus()))
										|| (null!=jsDetails.getProcessingStatus() && "failure".equals(jsDetails.getProcessingStatus().trim().toLowerCase())))
								{
									// material folder is failure
									status = "FAILURE";
									break;
								}
							}
						}
						pathToCheck  = null;
						jsDetails = null;
					}
					jsDetails = null;
				}
				
				// VOLTAGE LINK MAP JS
				if(null!=wiringDiagramUtils.getVoltageMapLinkJSList() && wiringDiagramUtils.getVoltageMapLinkJSList().size()>0)
				{
					WindowJSDetails jsDetails= null;
					ContentDetails con = null;
					String pathToCheck="";
					for(int a=0;a<wiringDiagramUtils.getVoltageMapLinkJSList().size();a++)
					{
						jsDetails =(WindowJSDetails)wiringDiagramUtils.getVoltageMapLinkJSList().get(a);
						con = new ContentDetails();
						if(null!=jsDetails.getContentDetails())
						{
							con = jsDetails.getContentDetails();
						}

						if(null!=con && null!=con.getLocale() && !"".equals(con.getLocale()) && null!=con.getModelFolderName() && 
								!"".equals(con.getModelFolderName()) && null!=con.getManualType() && !"".equals(con.getManualType()) 
								&& null!=con.getFaceLiftFolderName() && !"".equals(con.getFaceLiftFolderName()) 
								&& null!=con.getMaterialName() && !"".equals(con.getMaterialName()))
						{
							pathToCheck = con.getLocale().trim()+"\\"+con.getModelFolderName().trim()+"\\"+
									con.getManualType().trim()+"\\"+con.getFaceLiftFolderName().trim()+"\\"+con.getMaterialName();
						}
						if(null!=pathToCheck && !"".equals(pathToCheck))
						{
							if(pathToCheck.trim().toLowerCase().indexOf(materialFolderPath.trim().toLowerCase())>-1)
							{
								if((null==jsDetails.getProcessingStatus() || "".equals(jsDetails.getProcessingStatus()))
										|| (null!=jsDetails.getProcessingStatus() && "failure".equals(jsDetails.getProcessingStatus().trim().toLowerCase())))
								{
									// material folder is failure
									status = "FAILURE";
									break;
								}
							}
						}
						pathToCheck  = null;
						jsDetails = null;
					}
					jsDetails = null;
				}
				
				
			}
			
			if(vinMLMappingFailure==false)
			{
				if(null!=vinMLMappingList && vinMLMappingList.size()>0)
				{
					VinMLMappingDetails mappingDetails = null;
					ScheduleItemDetails itemDetails = null;
					String pathToCheck="";
					for(int a=0;a<vinMLMappingList.size();a++)
					{
						mappingDetails=  (VinMLMappingDetails) vinMLMappingList.get(a);
						if(null!=mappingDetails && null!=mappingDetails.getItemDetails())
						{
							itemDetails=  new ScheduleItemDetails();
							itemDetails=  mappingDetails.getItemDetails();
						}
						
						if(null!=itemDetails)
						{
							pathToCheck = itemDetails.getLocale()+"\\"+
									 itemDetails.getModelFolderName()+ "\\"+ itemDetails.getManualType() +"\\"+ 
										itemDetails.getFaceLiftFolderName() +"\\"+ itemDetails.getMaterialFolderName();
							if(null!=pathToCheck && pathToCheck.trim().toLowerCase().equals(materialFolderPath.trim().toLowerCase()))
							{
								// checking for specific material folder
								if((null==mappingDetails.getProcessingStatus() || "".equals(mappingDetails.getProcessingStatus())) ||
										(null!=mappingDetails.getProcessingStatus() && mappingDetails.getProcessingStatus().equals(AutomationConstants.STATUS_FAILURE)))
								{
									// MATERIAL FOLDER IS FAILURE
									status = "FAILURE";
									break;
								}
							}
							pathToCheck=  null;
						}
						itemDetails=  null;
						mappingDetails=  null;
					}
					mappingDetails= null;
					itemDetails= null;
					pathToCheck=  null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "identifyStatusForMaterialFolder()", e);
			status = "FAILURE";
		}
		logger.info("identifyStatusForMaterialFolder :: Status for {"+materialFolderPath+"} :: > "+ status);
		return status;
	}

	
	private void startVINManualTypeMapping(ArrayList<VinDetails> applicableVINList, ArrayList<VINEntFileDetails> masterVINList, ScheduleItemDetails itemDetails)
	{
		try
		{
			/*
			 * FOR EACH VIN + LOCALE COMBINATION
			 * CHECK IF ENTRY EXISTS IN DB TABLE - THEN UPDATE MANUAL TYPE MAPPING BY PASSING THE LOCALE IN IT
			 * IF ENTRY DOES NOT EXIST IN DB TABLE - THEN INSERT VIN DATA WITH MANUAL TYPE MAPPING
			 */
			if(null!=itemDetails && null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale()))
			{
				/*
				 * 
				 * HERE UPDATE SCHEDULE & ITEM CURRENT JOB STATUS AS VIN - MANUAL TYPE PROCESSING
				 * 
				 */
				ScheduleDAO.updateCurrentJobStatus(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), "VIN - MANUAL TYPE PROCESSING");
				String manualType="";
				if(null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType()))
				{
					if(itemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
							itemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
					{
						// WD CHANNEL
						manualType = ApplicationProperties.getProperty("wiring.diagram.folder.label");
					}
					else
					{
						// SM OR OSM CHANNEL
						if(null!=itemDetails.getModel() && 
								(itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
										itemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
						{
							// get on ModelName
							manualType = ConversionUtils.identifyManualTypeAsCateogry(itemDetails.getModel());
						}
						else
						{
							if(null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType()))
							{
								// on Manual Type
								manualType = ConversionUtils.identifyManualTypeAsCateogry(itemDetails.getManualType());
							}
						}
					}
				}
				
				logger.info("startVINManualTypeMapping :: Proceed for defining VIN ML Mapping for MANUAL TYPE :: > "+ manualType);
				ArrayList<Object> souceList = new ArrayList<Object>();
				if(null!=applicableVINList && applicableVINList.size()>0)
				{
					
					VinDetails v =null;
					for(int a=0;a<applicableVINList.size();a++)
					{
						v=(VinDetails)applicableVINList.get(a);
						souceList.add(v);
						v = null;
					}
					v=  null;
				}
				
				if(null!=masterVINList && masterVINList.size()>0)
				{
					VINEntFileDetails v = null;
					for(int a=0;a<masterVINList.size();a++)
					{
						v=(VINEntFileDetails)masterVINList.get(a);
						souceList.add(v);
						v = null;
					}
					v=  null;
				}
				
				logger.info("startVINManualTypeMapping :: Proceed for defining VIN ML Mapping for LOCALE :: > "+ itemDetails.getLocale());
				logger.info("startVINManualTypeMapping :: Proceed for defining VIN ML Mapping for VINS :: > "+ souceList.size());
				logger.info("startVINManualTypeMapping :: Proceed for defining VIN ML Mapping for Manual Type :: > "+ manualType);
				// call function to save details in database
				ArrayList<VinMLMappingDetails> tempList = mmeDocumentManagementDAO.saveVinManualTypeMapping(souceList, itemDetails.getLocale(), manualType, itemDetails);
				souceList = null;
				
				if(null!=tempList && tempList.size()>0)
				{
					vinMLMappingList.addAll(tempList);
				}
				tempList=  null;
				manualType=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartMMEConversionImpl.class.getName(), "startVINManualTypeMapping()", e);
		}
	}

	/*
	 * DISPLAY ORDER ROWS OF THE MATERIAL FOLDER, BY DOCUMENT PATH (trimmed, lower case) - the rows the job saves in the
	 * display order staging table (gms3_dmt_el_gr_nm_dispord), held in memory as well. A document's rows are taken
	 * from here instead of one SELECT per document (~200 ms round trip each through the MySQL Router on MC Dev).
	 */
	private java.util.Map<String, List<DisplayOrderDetails>> displayOrderIndex = null;

	private static java.util.Map<String, List<DisplayOrderDetails>> indexDisplayOrder(List<DisplayOrderDetails> rows)
	{
		java.util.Map<String, List<DisplayOrderDetails>> index = new java.util.HashMap<String, List<DisplayOrderDetails>>();
		for(DisplayOrderDetails d : rows)
		{
			if(null!=d && null!=d.getFilePath())
			{
				index.computeIfAbsent(d.getFilePath().trim().toLowerCase(), k -> new ArrayList<DisplayOrderDetails>()).add(d);
			}
		}
		return index;
	}

	/*
	 * THE DISPLAY ORDER ROWS OF ONE DOCUMENT - the same rows (as copies) ScheduleDAO.getApplicableDisplayOrderForDocument
	 * reads from the staging table; that SELECT is still used when no index was built for the material folder.
	 */
	private ArrayList<DisplayOrderDetails> applicableDisplayOrderForDocument(String filePath)
	{
		if(null==displayOrderIndex)
		{
			return ScheduleDAO.getApplicableDisplayOrderForDocument(filePath);
		}
		if(null==filePath || "".equals(filePath))
		{
			return null;
		}
		List<DisplayOrderDetails> rows = displayOrderIndex.get(filePath.trim().toLowerCase());
		if(null==rows || rows.isEmpty())
		{
			return null;
		}
		ArrayList<DisplayOrderDetails> copies = new ArrayList<DisplayOrderDetails>();
		for(DisplayOrderDetails r : rows)
		{
			DisplayOrderDetails d = new DisplayOrderDetails();
			d.setSequenceNo(r.getSequenceNo());
			d.setLineType(r.getLineType());
			d.setFilePath(r.getFilePath().trim());
			d.setModelFolderName(r.getModelFolderName());
			d.setManualType(r.getManualType());
			d.setFaceLiftFolderName(r.getFaceLiftFolderName());
			d.setMaterialFolderName(r.getMaterialFolderName());
			d.setProcessingFolderName(r.getProcessingFolderName());
			d.setFileName(r.getFileName());
			d.setTitle(r.getTitle());
			d.setEngineType(r.getEngineType());
			d.setMissionType(r.getMissionType());
			d.setBodyType(r.getBodyType());
			d.setDriveAxleType(r.getDriveAxleType());
			d.setDisplayOrderLevel1Code(r.getDisplayOrderLevel1Code());
			d.setDisplayOrderLevel2Code(r.getDisplayOrderLevel2Code());
			d.setDisplayOrderLevel3Code(r.getDisplayOrderLevel3Code());
			d.setDisplayOrderLevel4Code(r.getDisplayOrderLevel4Code());
			d.setDisplayOrderLevel5Code(r.getDisplayOrderLevel5Code());
			d.setDisplayOrderLevel6Code(r.getDisplayOrderLevel6Code());
			d.setDisplayOrderLevel1Name(r.getDisplayOrderLevel1Name());
			d.setDisplayOrderLevel2Name(r.getDisplayOrderLevel2Name());
			d.setDisplayOrderLevel3Name(r.getDisplayOrderLevel3Name());
			d.setDisplayOrderLevel4Name(r.getDisplayOrderLevel4Name());
			d.setDisplayOrderLevel5Name(r.getDisplayOrderLevel5Name());
			d.setDisplayOrderLevel6Name(r.getDisplayOrderLevel6Name());
			d.setDisplayOrderSourceFileName(r.getDisplayOrderSourceFileName());
			d.setDisplayOrderSourceFilePath(r.getDisplayOrderSourceFilePath());
			copies.add(d);
		}
		return copies;
	}

	private String returnKey(String check, DisplayOrderDetails displayOrderDetails)
	{
		if(null!=displayOrderDetails.getBodyType())
		{
			check+=displayOrderDetails.getBodyType();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel1Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel1Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel2Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel2Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel3Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel3Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel4Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel4Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel5Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel5Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderLevel6Name())
		{
			check+=displayOrderDetails.getDisplayOrderLevel6Name();
		}
		if(null!=displayOrderDetails.getDisplayOrderSourceFileName())
		{
			check+=displayOrderDetails.getDisplayOrderSourceFileName();
		}
		if(null!=displayOrderDetails.getDriveAxleType())
		{
			check+=displayOrderDetails.getDriveAxleType();
		}
		if(null!=displayOrderDetails.getEngineType())
		{
			check+=displayOrderDetails.getEngineType();
		}
		if(null!=displayOrderDetails.getFileName())
		{
			check+=displayOrderDetails.getFileName();
		}
		if(null!=displayOrderDetails.getFilePath())
		{
			check+=displayOrderDetails.getFilePath();
		}
		if(null!=displayOrderDetails.getManualType())
		{
			check+=displayOrderDetails.getManualType();
		}
		if(null!=displayOrderDetails.getMissionType())
		{
			check+=displayOrderDetails.getMissionType();
		}
		if(null!=displayOrderDetails.getSequenceNo())
		{
			check+=displayOrderDetails.getSequenceNo();
		}
		if(null!=displayOrderDetails.getDisplayOrderSourceFileName())
		{
			check+=displayOrderDetails.getDisplayOrderSourceFileName();
		}
		return check;
	}
	
	private void mfIncrementDataPreparationSuccessCount(String scheduleId, String itemId)
	{
		/*
		 * INCREMENT DATA PREPARATION SUCCESS COUNT BY 1 & ITEM DETAILS DATA PREPARATION SUCCESS COUNT BY 1
		 */
		mfDataPrepSuccessCount++;
		bufferDataPreparationCount(scheduleId, itemId, 1, 0);
	}

	/*
	 * DATA PREPARATION PROGRESS COUNTS, BUFFERED: written with one statement per table every
	 * DATA_PREP_FLUSH_MS (or when the item changes, and always before the reset at the end of Data
	 * Preparation) instead of 4 statements per file. On MC Dev each per-file write cost about
	 * 1.4 s (same buffering as the MC market).
	 */
	private static final long DATA_PREP_FLUSH_MS = 5000L;
	private String prepCountScheduleId = null;
	private String prepCountItemId = null;
	private int prepCountProcessed = 0;
	private int prepCountFailed = 0;
	private long prepCountLastFlush = System.currentTimeMillis();

	private void bufferDataPreparationCount(String scheduleId, String itemId, int processed, int failed)
	{
		if (null != prepCountScheduleId && (!prepCountScheduleId.equals(scheduleId) || !String.valueOf(prepCountItemId).equals(String.valueOf(itemId))))
		{
			flushDataPreparationCount();
		}
		prepCountScheduleId = scheduleId;
		prepCountItemId = itemId;
		prepCountProcessed += processed;
		prepCountFailed += failed;
		if (System.currentTimeMillis() - prepCountLastFlush >= DATA_PREP_FLUSH_MS)
		{
			flushDataPreparationCount();
		}
	}

	private void flushDataPreparationCount()
	{
		if (null != prepCountScheduleId && (prepCountProcessed > 0 || prepCountFailed > 0))
		{
			MMEDocumentBatchDAO.addDocumentCounts(prepCountScheduleId, prepCountItemId, prepCountProcessed, prepCountFailed);
		}
		prepCountProcessed = 0;
		prepCountFailed = 0;
		prepCountLastFlush = System.currentTimeMillis();
	}

	private void mfIncrementDataPreparationFailureCount(String scheduleId, String itemId)
	{
		/*
		 * INCREMENT DATA PREPARATION FAILURE COUNT BY 1 & ITEM DETAILS DATA PREPARATION FAILURE COUNT BY 1
		 */
		mfDataPrepfailureCount++;
		// the failure column - resetDataPreparationCount takes mfDataPrepfailureCount back off DC_FAILED_DOCS_COUNT
		// (legacy raised the processed column here, which the reset never took back)
		bufferDataPreparationCount(scheduleId, itemId, 0, 1);
	}

}