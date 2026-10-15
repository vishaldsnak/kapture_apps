package com.mazda.gms3.mdm.im.impl;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;





import java.sql.Connection;

import com.mazda.gms3.mdm.dao.CategoryProcessingDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.kapture.KaptureCategoryServiceImpl;
import com.mazda.gms3.mdm.kapture.KaptureContentServiceImpl;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;
import com.mazda.gms3.mdm.dao.MCSIVinDAO;
import com.mazda.gms3.mdm.dao.MMESIVinDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.SIVINBatchProcessingReportsUtil;
import com.mazda.gms3.mdm.utils.SendMailUsingAuthentication;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;


public class MCSIVINBatchProcessingImpl extends Thread{

	private Logger logger = LogManager.getLogger(MCSIVINBatchProcessingImpl.class);
	
	private CategoryProcessingDAO categoryProcessingDAO = null;
	
	/*
	 * STILL INFOMANAGER, AND ONLY FOR THE TRANSLATION UPDATE SUB-FLOW -
	 * modifyTranslations() / getContentDetailsForTranslations() / updateCategory(). That block
	 * is gated on the translationUpdateFlag argument and migrates with the translation work.
	 */


	/*
	 * ONE CLIENT AND ONE CONNECTION FOR THE WHOLE RUN. A schedule can carry thousands of
	 * categories; building either per category would mean a fresh API login and a fresh
	 * connection each time - slow, and straight into the API's rate limit.
	 */
	private KaptureApiClient kaptureApi = null;

	private Connection cmsConn = null;
	
	
	
	private List<IMCategoryDetails> categoryProcessingList = null;
	
	private List<IMCategoryDetails> failureCategoryList = null;
	
	private SIVINScheduleDetails documentDetails = null;
	
	private SIVINBatchProcessingReportsUtil printReportsUtil = null;
	
	

	/*
	 * HAS THE SCREEN ABORTED THIS SCHEDULE?
	 *
	 * The Abort button used to call Thread.stop() on this worker. That method was REMOVED in
	 * Java 20 and now throws UnsupportedOperationException, which the screen's catch(Exception)
	 * swallows - so the schedule was marked Aborted while this thread carried on to the end,
	 * still creating categories and still writing item status rows. Nothing in the JVM can kill
	 * another thread any more, so the worker has to stop itself.
	 *
	 * The abort signal is already in the database: the screen writes mdm_job_status='Aborted'
	 * whether or not it found the thread. This reads it back, and the per-item loops break on it.
	 * Breaking is all it does - the cleanup, reports and notification that follow are untouched.
	 *
	 * THROTTLED ON PURPOSE. Some of these loops run through thousands of rows quickly and a
	 * SELECT per iteration would be pure load. The answer is cached for ABORT_CHECK_INTERVAL_MS,
	 * so the database sees at most one read every two seconds however fast the loop spins, and
	 * once aborted the flag latches and it never asks again. Worst case the check itself is half a second
	 * behind the click - the rest of any delay is the operation already in flight.
	 */
	private static final long ABORT_CHECK_INTERVAL_MS = 500L;

	private long lastAbortCheckTime = 0L;

	/* The schedule being processed, so the check works in the helper methods
	 * below - they do the per-category work but never receive the id. */
	private String abortScheduleId = null;

	private boolean scheduleAborted = false;

	private boolean isScheduleAborted()
	{
		if(scheduleAborted==true)
		{
			return true;
		}
		long now = System.currentTimeMillis();
		if(now-lastAbortCheckTime < ABORT_CHECK_INTERVAL_MS)
		{
			return false;
		}
		lastAbortCheckTime = now;
		if(null!=categoryProcessingDAO)
		{
			scheduleAborted = categoryProcessingDAO.isAborted(abortScheduleId);
			if(scheduleAborted==true)
			{
				logger.info("isScheduleAborted :: SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
						+" SCREEN. Stopping after the item in flight.");
			}
		}
		return scheduleAborted;
	}
	public void startProcess(String scheduleId, SIVinDetails addVinDetails, String market,String translationUpdateFlag)
	{
		// remember the schedule so the abort check works in the helpers below
		abortScheduleId = scheduleId;
		logger.info("startProcess :: Translations Updated Allowed :: > "+ translationUpdateFlag);
		// initialize all variables
		categoryProcessingDAO = new CategoryProcessingDAO();
		try
		{
			/*
			 * OPENED ONCE FOR THE WHOLE SCHEDULE. Every category lookup and every content read
			 * in this run borrows this connection, and the API client keeps one token throughout.
			 */
			kaptureApi = new KaptureApiClient();
			cmsConn = DBConnectionHelper.getCMSConnection();
			if(null==cmsConn)
			{
				logger.info(" ******************** SIVINBatchProcessingImpl :: NO KAPTURE CMS CONNECTION. Categories and content cannot be processed. ***********************");
			}
			categoryProcessingList = new ArrayList<IMCategoryDetails>();
			failureCategoryList = new ArrayList<IMCategoryDetails>();
			documentDetails = new SIVINScheduleDetails();
			printReportsUtil = new SIVINBatchProcessingReportsUtil();
			
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId))
			{
				// FETCH SCHEDULE DETAILS
				SIVINScheduleDetails schDetails = categoryProcessingDAO.getScheduleDetails(scheduleId,"Y");
				if(null!=schDetails && schDetails.getScheduleId()>0 && null!=schDetails.getDocumentId() && !"".equals(schDetails.getDocumentId()) && 
						null!=schDetails.getLocale() && !"".equals(schDetails.getLocale()) && null!=schDetails.getCategoryList() && schDetails.getCategoryList().size()>0)
				{
					/*
					 * START CATEGORY PROCESSING FOR ALL ITEMS
					 */
					// update jobStatus to Processing
					categoryProcessingDAO.updateJobStatus(scheduleId, ScheduleConstants.STATUS_PROCESSING);
					
					/*
					 *  ITERATE ITEMS LIST AND START PROCESSING EACH CATEGORY
					 *  PREPARE DATA IN THE FOLLOWING STRUCTURE - 
					 *  THIRD LEVEL (APPLICABLE ONLY FOR MME)
					 *  	ALL ITS CHILD FORTH LEVELS
					 *  FORTH LEVEL 
					 *  	ALL ITS CHILD FIFTH LEVELS
					 *  FORTH LEVLE 2
					 *  	ALL ITS CHILD FIFTH LEVELS
					 *   
					 *   ALSO IDENIFY FIRST ALL LEVEL 4s, THEN ALL LEVEL5 (THIS IS REQUIRED FOR MAPPING OF CATEGORY WITH DOCUMENT)
					 *   PERFORM LEVEL 3s, AS WELL IF MARKET IS MME
					 */
					IMCategoryDetails details = null;
					List<IMCategoryDetails> thirdLevelList = null;
					List<IMCategoryDetails> forthLevelList = null;
					List<IMCategoryDetails> fifthLevelList = null;
					for(int a=0;a<schDetails.getCategoryList().size();a++)
					{
						if(isScheduleAborted()) { break; }
						details = (IMCategoryDetails) schDetails.getCategoryList().get(a);
						if(null!=details.getLevel() && !"".equals(details.getLevel()))
						{
							if(details.getLevel().trim().equals(ScheduleConstants.LEVEL_3))
							{
								if(null==thirdLevelList || thirdLevelList.size()<=0)
								{
									thirdLevelList = new ArrayList<IMCategoryDetails>();
								}
								thirdLevelList.add(details);
							}
							if(details.getLevel().trim().equals(ScheduleConstants.LEVEL_4))
							{
								if(null==forthLevelList || forthLevelList.size()<=0)
								{
									forthLevelList = new ArrayList<IMCategoryDetails>();
								}
								forthLevelList.add(details);
							}
							else if(details.getLevel().trim().equals(ScheduleConstants.LEVEL_5))
							{
								if(null==fifthLevelList || fifthLevelList.size()<=0)
								{
									fifthLevelList = new ArrayList<IMCategoryDetails>();
								}
								fifthLevelList.add(details);
							}
						}
						details = null;
					}
					details = null;
					
					if(null!=thirdLevelList && thirdLevelList.size()>0)
					{
						// IDENTIFY CHILDS FOR EACH LEVEL
						details = null;
						IMCategoryDetails parentDetails = null;
						for(int a=0;a<schDetails.getCategoryList().size();a++)
						{
							if(isScheduleAborted()) { break; }
							details = (IMCategoryDetails) schDetails.getCategoryList().get(a);
							if(null!=details.getLevel() && !"".equals(details.getLevel()))
							{
								// CHECK ONLY FOR LEVEL 4
								if(details.getLevel().equals(ScheduleConstants.LEVEL_4))
								{
									// ITERATE 3RD LEVEL LIST AND FOR THE MATCHING PARENT REF KEY ADD CHILD
									for(int b=0;b<thirdLevelList.size();b++)
									{
										if(isScheduleAborted()) { break; }
										parentDetails= (IMCategoryDetails)thirdLevelList.get(b);
										if(null!=parentDetails.getCategoryRefKey() && null!=details.getParentRefKey())
										{
											if(parentDetails.getCategoryRefKey().trim().equals(details.getParentRefKey().trim()))
											{
												if(null==parentDetails.getChildList() || parentDetails.getChildList().size()<=0)
												{
													parentDetails.setChildList(new ArrayList<IMCategoryDetails>());
												}
												parentDetails.getChildList().add(details);
											}
										}
										parentDetails = null;
									}
								}
							}
							details = null;
						}
						parentDetails=  null;
						details = null;
						
						// IDENTIFY CHILDS AT FIFTH LEVEL FOR EACH OF THE FORTH LEVEL CHILD 
						IMCategoryDetails thirdLevelDetails=  null;
						for(int a=0;a<schDetails.getCategoryList().size();a++)
						{
							if(isScheduleAborted()) { break; }
							details = (IMCategoryDetails) schDetails.getCategoryList().get(a);
							if(null!=details.getLevel() && !"".equals(details.getLevel()))
							{
								// CHECK ONLY FOR LEVEL 5
								if(details.getLevel().equals(ScheduleConstants.LEVEL_5))
								{
									// ITERTAE THIRD LEVLE AND SEE IF ANY 4TH LEVEL IS AVAILABLE - IF YES
									// ITERATE 4TH LEVEL LIST AND FOR THE MATCHING PARENT REF KEY ADD CHILD
									for(int r=0;r<thirdLevelList.size();r++)
									{
										if(isScheduleAborted()) { break; }
										thirdLevelDetails = (IMCategoryDetails)thirdLevelList.get(r);
										// 4th LEVEL EXISTS
										if(null!=thirdLevelDetails.getChildList() && thirdLevelDetails.getChildList().size()>0)
										{
											parentDetails = null;
											for(int e=0;e<thirdLevelDetails.getChildList().size();e++)
											{
												if(isScheduleAborted()) { break; }
												parentDetails = (IMCategoryDetails)thirdLevelDetails.getChildList().get(e);
												if(null!=parentDetails.getCategoryRefKey() && null!=details.getParentRefKey())
												{
													if(parentDetails.getCategoryRefKey().trim().equals(details.getParentRefKey().trim()))
													{
														if(null==parentDetails.getChildList() || parentDetails.getChildList().size()<=0)
														{
															parentDetails.setChildList(new ArrayList<IMCategoryDetails>());
														}
														parentDetails.getChildList().add(details);
													}
												}
												parentDetails = null;
											}
										}
										thirdLevelDetails = null;
									}
								}
							}
							details = null;
						}
					}
					
//					if(null!=forthLevelList && forthLevelList.size()>0)
//					{
//						// IDENTIFY CHILDS FOR EACH LEVEL
//						details = null;
//						IMCategoryDetails parentDetails = null;
//						for(int a=0;a<schDetails.getCategoryList().size();a++)
//						{
//							details = (IMCategoryDetails) schDetails.getCategoryList().get(a);
//							if(null!=details.getLevel() && !"".equals(details.getLevel()))
//							{
//								// CHECK ONLY FOR LEVEL 5
//								if(details.getLevel().equals(ScheduleConstants.LEVEL_5))
//								{
//									// ITERATE 4TH LEVEL LIST AND FOR THE MATCHING PARENT REF KEY ADD CHILD
//									for(int b=0;b<forthLevelList.size();b++)
//									{
//										parentDetails= (IMCategoryDetails)forthLevelList.get(b);
//										if(null!=parentDetails.getCategoryRefKey() && null!=details.getParentRefKey())
//										{
//											if(parentDetails.getCategoryRefKey().trim().equals(details.getParentRefKey().trim()))
//											{
//												if(null==parentDetails.getChildList() || parentDetails.getChildList().size()<=0)
//												{
//													parentDetails.setChildList(new ArrayList<IMCategoryDetails>());
//												}
//												parentDetails.getChildList().add(details);
//											}
//										}
//										parentDetails = null;
//									}
//								}
//							}
//							details = null;
//						}
//						parentDetails=  null;
//						details = null;
//					}
					
					
					
					
					/*
					 * FIRST PROCESS ALL 4TH LEVELS THEN ALL ITS 5TH LEVELS,
					 */
					if(null!=thirdLevelList && thirdLevelList.size()>0 && null!=forthLevelList && forthLevelList.size()>0 && 
							null!=fifthLevelList && fifthLevelList.size()>0)
					{
						// FIRST THIRD LEVEL - > THAN ALL ITS 4TH LEVELS - > THEN ALL ITS 5TH LEVELS
						if(null!=thirdLevelList && thirdLevelList.size()>0)
						{
							createCategory(thirdLevelList);
						}
						
//						// FIRST FORTH LEVEL - > THEN ALL ITS 5TH LEVELS
//						if(null!=forthLevelList && forthLevelList.size()>0)
//						{
//							createCategory(forthLevelList);
//						}
						
						if(null!=fifthLevelList && fifthLevelList.size()>0)
						{
							/*
							 * PROCEED FOR MODIFYING DOCUMENT - ONLY WHEN FIFTH LEVEL IS NOT NULL
							 */
							
							modifyContent(schDetails.getDocumentId(), schDetails.getLocale(), schDetails.getWslId(), fifthLevelList);
							if(null!=documentDetails && null!=documentDetails.getContentId() && !"".equals(documentDetails.getContentId()))
							{
								// save ADD VIN Details
								if(null!=addVinDetails)
								{
									// set documentId
									addVinDetails.setDocumentId(documentDetails.getDocumentId());
									addVinDetails.setFetchedVersion(documentDetails.getFetchedVersion());
									addVinDetails.setUpdatedVersion(documentDetails.getUpdatedVersion());
									addVinDetails.setDocumentPublished(documentDetails.isDocumentPublishedStatus());
									boolean bool = false;
									if(market.equals("MME"))
									{
										bool = MMESIVinDAO.addVINDetails(addVinDetails, categoryProcessingDAO.conn,"N");
									}
									else if(market.equals("MC"))
									{
										bool = MCSIVinDAO.addVINDetails(addVinDetails, categoryProcessingDAO.conn, "N");
									}
									
									/*
									 * CHECK HERE, IF CATEGORY PROCESSING DAO CONN IS NOT NULL
									 * SET AUTO COMMIT TO TRUE FOR FURTHER OPERATIONS
									 */
									try
									{
										if(null!=categoryProcessingDAO.conn && categoryProcessingDAO.conn.isValid(0))
										{
											// set autoCommit to true
											categoryProcessingDAO.conn.setAutoCommit(true);
										}
									}
									catch(Exception  e){
										Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "startProcess()", e);
									}
									
									if(bool==true)
									{
										documentDetails.setDbprocessingStatus(ScheduleConstants.STATUS_SUCCESS);
									}
									else
									{
										documentDetails.setDbprocessingStatus(ScheduleConstants.STATUS_FAILURE);
										documentDetails.setErrorCode("DBMOD001");
										documentDetails.setErrorMessage("Failed to save added VIN(s) information in database.");
									}
								}
							}
							
						}
						
						
						
						/*
						 * ABORTED FROM THE SCREEN - stop here. No reports, no schedule completion
						 * status: the schedule stays Aborted, which is
						 * what the screen already recorded. Returning is safe - the DB and Kapture
						 * connections are closed in the finally below, which still runs.
						 */
						if(isScheduleAborted())
						{
							/*
							 * NO NOTIFICATION ON ABORT - the pre-migration code sent none.
							 */
							return;
						}

						/*
						 * call function to print reports & 
						 * 
						 * if failure List is null & documentDetails.contentId is not null and IM Processing + DB Processing + Category Mapping status =(all are Success)
						 * set Job Status as Completed and Schedule Status as SUCCESS
						 * ELSE FAILURE
						 * 
						 * Update Reports Paths
						 * 
						 */
						boolean errorsFound=false;
						if(null!=failureCategoryList && failureCategoryList.size()>0)
						{
							errorsFound = true;
						}
						else 
						{
							// check if any translation document failed while updating
							
							
							if(null!=documentDetails)
							{
								if((null==documentDetails.getImProcessingStatus()) || (null!=documentDetails.getImProcessingStatus() && documentDetails.getImProcessingStatus().equals(ScheduleConstants.STATUS_FAILURE)))
								{
									errorsFound = true;
								}
								else if((null==documentDetails.getDbprocessingStatus()) || (null!=documentDetails.getDbprocessingStatus() && documentDetails.getDbprocessingStatus().equals(ScheduleConstants.STATUS_FAILURE)))
								{
									errorsFound = true;
								}
								else
								{
									if(null!=documentDetails.getCategoryList() && documentDetails.getCategoryList().size()>0)
									{
										IMCategoryDetails cat = null;
										for(int a=0;a<documentDetails.getCategoryList().size();a++)
										{
											if(isScheduleAborted()) { break; }
											cat = (IMCategoryDetails)documentDetails.getCategoryList().get(a);
											if((null==cat.getProcessingStatus()) || (null!=cat.getProcessingStatus() && cat.getProcessingStatus().equals(ScheduleConstants.STATUS_FAILURE)))
											{
												errorsFound = true;
												break;
											}
											cat = null;
										}
									}
									else
									{
										// no categoryList for adding to document.
										errorsFound = true;
									}
								}
							}
						}
						
						// identify and update mappingCount
						if(null!=documentDetails && null!=documentDetails.getCategoryList() && documentDetails.getCategoryList().size()>0)
						{
							int totalCount  = documentDetails.getCategoryList().size();
							int mapCount=0;
							IMCategoryDetails cat = null;
							for(int a=0;a<documentDetails.getCategoryList().size();a++)
							{
								if(isScheduleAborted()) { break; }
								cat = (IMCategoryDetails)documentDetails.getCategoryList().get(a);
								if(null!=cat.getProcessingStatus() && cat.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
								{
									mapCount++;
								}
								cat = null;
							}
							
							// update count
							categoryProcessingDAO.updateMappingCountDetails(scheduleId, String.valueOf(mapCount)+" / "+ String.valueOf(totalCount));
							mapCount = 0;
							totalCount = 0;
						}
						/*
						 * call function to print reports 
						 */
						try
						{
							printReportsUtil.printCategoryFailureReport(failureCategoryList, scheduleId, market);
							printReportsUtil.printCategoryTransactionReport(categoryProcessingList, scheduleId, market);
							printReportsUtil.printDocumentTransactionReport(documentDetails, scheduleId);
						}
						catch(Exception  e)
						{
							Utilities.printStackTraceToLogs(SIVINBatchProcessingReportsUtil.class.getName(), "startProcess()", e);
						}
						/*
						 * now here, generate the Zip file for all the reports generated the
						 * path for reports directory will be -
						 * REPORTS_DIRECTORY/schCode
						 */
						String path = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY")+ "/"+ scheduleId + "/";

						/*
						 * call function to generate zip file
						 */
						boolean bool = printReportsUtil.createReportsZip(path, scheduleId);
						if (bool == true) 
						{
							logger.info("startProcess :: Reports Zipped Successfully.");
						} 
						else 
						{
							logger.info("startProcess :: Failed to Zip Reports, these have to be downloaded manually.");
						}
						path = null;
						
						if(errorsFound == true)
						{
							logger.info("startProcess ::  Errors Found Updating Schedule as Failure.");
							// update scheduleCompletion - with scheduleStatus as Failure
							categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE);
						}
						else
						{
							logger.info("startProcess ::  No Errors Found Updating Schedule as Success.");
							// update scheduleCompletion - with scheduleStatus as Success
							categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_SUCCESS);
						}
					}
					else
					{
						logger.info("startProcess :: No Forth / Fifth level categories identified for Schedule Id :: > "+ scheduleId+". Exit Conversion.");
						// update scheduleCompletion - with scheduleStatus as Failure
						categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE);
					}
					forthLevelList = null;
					fifthLevelList = null;
				}
				else
				{
					logger.info("startProcess :: Either Schedule Details Could not be fetched or Items List / Document id / Locale are null for Schedule id :: > "+ scheduleId+". Exit Conversion.");
					// update scheduleCompletion - with scheduleStatus as Failure
					categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE);
				}
				schDetails = null;
			}
			else
			{
				logger.info("startProcess :: Schedule id as parameter is null. Exist Conversion.");
				// update scheduleCompletion - with scheduleStatus as Failure
				categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE);
			}
			
			
			// send email notification
			generateTextNotificationEmail(scheduleId, market.trim().toUpperCase());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "startProcess()", e);
			// update scheduleCompletion - with scheduleStatus as Failure
			categoryProcessingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE);
		}
		finally
		{
			// close all active connection & client objects
			try
			{
				if(null!=categoryProcessingDAO.conn)
				{
					logger.info(" ******************** SIVINBatchProcessingImpl :: CLOSING DB CONNECTION OBJECT. ***********************");
					categoryProcessingDAO.conn.close();
				}
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "startProcess()", e);
			}
			
			
			try
			{
				if(null!=cmsConn)
				{
					logger.info(" ******************** SIVINBatchProcessingImpl :: CLOSING KAPTURE CMS CONNECTION OBJECT. ***********************");
					cmsConn.close();
				}
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "startProcess()", e);
			}
			cmsConn = null;
			kaptureApi = null;

			
			logger.info(" ******************** SIVINBatchProcessingImpl :: FOR SCHEDULE ID {"+scheduleId+"}. GENERATED DB CONNECTION COUNTS ARE :: > "+ categoryProcessingDAO.connectionCount);
			categoryProcessingDAO.connectionCount = 0;
			categoryProcessingDAO = null;
			addVinDetails = null;
			categoryProcessingList = null;
			failureCategoryList = null;
			documentDetails = null;
			printReportsUtil = null;
		}
		
	}
	

	private void createCategory(List<IMCategoryDetails> categoryList)
	{
		// ABORTED FROM THE SCREEN - do no further Kapture work.
		if(isScheduleAborted()) { return; }
		try
		{
			if(null!=categoryList && categoryList.size()>0)
			{
				IMCategoryDetails categoryData = null;
				IMCategoryDetails childCategoryData = null;
				IMCategoryDetails thirdLevelCategoryData = null;
				IMCategoryDetails newThirdLevelCategoryData = null;
				IMCategoryDetails newCategoryData = null;
				IMCategoryDetails newChildCategoryData = null;
				
				for(int i=0;i<categoryList.size();i++)
				{
					if(isScheduleAborted()) { break; }
					thirdLevelCategoryData = (IMCategoryDetails)categoryList.get(i);
					/*
					 * PROCEED FOR CATEGORY CREATION
					 */
					boolean bool = createCategoryOperation(thirdLevelCategoryData);
					if(bool==true)
					{
						// if LEVEL 3 & Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS SUCCESS
						// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS PROCESSED COUNT
						categoryProcessingDAO.updateProcessingStatus(String.valueOf(thirdLevelCategoryData.getScheduleId()), ScheduleConstants.STATUS_SUCCESS, String.valueOf(thirdLevelCategoryData.getItemId()), thirdLevelCategoryData.getLevel());
						// set ProcessingStatus as Success
						thirdLevelCategoryData.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
						// set operationType as CREATE_CATEGORY
						thirdLevelCategoryData.setOperationType("CREATE_CATEGORY");
					}
					else
					{
						// if level 3 & Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS FAILURE
						// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS FAILURE COUNT
						categoryProcessingDAO.updateProcessingStatus(String.valueOf(thirdLevelCategoryData.getScheduleId()), ScheduleConstants.STATUS_FAILURE, String.valueOf(thirdLevelCategoryData.getItemId()), thirdLevelCategoryData.getLevel());
						// set ProcessingStatus as Success
						thirdLevelCategoryData.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
						// set operationType as CREATE_CATEGORY
						thirdLevelCategoryData.setOperationType("CREATE_CATEGORY");
					}
					
					// ADD TO PROCESSING LIST
					if(null==categoryProcessingList || categoryProcessingList.size()<=0)
					{
						categoryProcessingList = new ArrayList<IMCategoryDetails>();
					}
					newThirdLevelCategoryData  = new IMCategoryDetails();
					newThirdLevelCategoryData.setCategoryName(thirdLevelCategoryData.getCategoryName());
					newThirdLevelCategoryData.setCategoryRefKey(thirdLevelCategoryData.getCategoryRefKey());
					newThirdLevelCategoryData.setChildList(thirdLevelCategoryData.getChildList());
					newThirdLevelCategoryData.setErrorCode(thirdLevelCategoryData.getErrorCode());
					newThirdLevelCategoryData.setErrorMessage(thirdLevelCategoryData.getErrorMessage());
					newThirdLevelCategoryData.setItemDetails(thirdLevelCategoryData.getItemDetails());
					newThirdLevelCategoryData.setItemId(thirdLevelCategoryData.getItemId());
					newThirdLevelCategoryData.setLevel(thirdLevelCategoryData.getLevel());
					newThirdLevelCategoryData.setLocale(thirdLevelCategoryData.getLocale());
					newThirdLevelCategoryData.setObjectId(thirdLevelCategoryData.getObjectId());
					newThirdLevelCategoryData.setOperationType(thirdLevelCategoryData.getOperationType());
					/*
					 * THE STATUS THE ROW IS BEING REPORTED FOR. This copy is hand-written field by field
					 * and left it out, so the category report printed a BLANK status column for every
					 * created and updated category - the value is set on the source object just above.
					 */
					newThirdLevelCategoryData.setProcessingStatus(thirdLevelCategoryData.getProcessingStatus());
					newThirdLevelCategoryData.setParentRefKey(thirdLevelCategoryData.getParentRefKey());
					newThirdLevelCategoryData.setScheduleId(thirdLevelCategoryData.getScheduleId());
					newThirdLevelCategoryData.setSrNo(thirdLevelCategoryData.getSrNo());
					categoryProcessingList.add(newThirdLevelCategoryData);
					newThirdLevelCategoryData=  null;
					
					/*
					 * NOW CHECK IF CHILD LIST OF CURRENT CATEGORY IS NOT NULL
					 * PROCESS ALL THE CHILD CATEGORIES AS WELL - LEVEL 4 PROCESSING
					 */
					if(null!=thirdLevelCategoryData.getChildList() && thirdLevelCategoryData.getChildList().size()>0)
					{
						categoryData = null;
						for(int t=0;t<thirdLevelCategoryData.getChildList().size();t++)
						{
							if(isScheduleAborted()) { break; }
							categoryData = (IMCategoryDetails)thirdLevelCategoryData.getChildList().get(t);
							/*
							 * PROCEED FOR CATEGORY CREATION
							 */
							boolean boolChild = createCategoryOperation(categoryData);
							if(boolChild==true)
							{
								// if Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS SUCCESS
								// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS PROCESSED COUNT
								categoryProcessingDAO.updateProcessingStatus(String.valueOf(categoryData.getScheduleId()), ScheduleConstants.STATUS_SUCCESS, String.valueOf(categoryData.getItemId()), categoryData.getLevel());
								// set ProcessingStatus as Success
								categoryData.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
								// set operationType as CREATE_CATEGORY
								categoryData.setOperationType("CREATE_CATEGORY");
							}
							else
							{
								// if Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS FAILURE
								// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS FAILURE COUNT
								categoryProcessingDAO.updateProcessingStatus(String.valueOf(categoryData.getScheduleId()), ScheduleConstants.STATUS_FAILURE, String.valueOf(categoryData.getItemId()), categoryData.getLevel());
								// set ProcessingStatus as Success
								categoryData.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
								// set operationType as CREATE_CATEGORY
								categoryData.setOperationType("CREATE_CATEGORY");
							}
							
							// ADD TO PROCESSING LIST
							if(null==categoryProcessingList || categoryProcessingList.size()<=0)
							{
								categoryProcessingList = new ArrayList<IMCategoryDetails>();
							}
							newCategoryData  = new IMCategoryDetails();
							newCategoryData.setCategoryName(categoryData.getCategoryName());
							newCategoryData.setCategoryRefKey(categoryData.getCategoryRefKey());
							newCategoryData.setChildList(categoryData.getChildList());
							newCategoryData.setErrorCode(categoryData.getErrorCode());
							newCategoryData.setErrorMessage(categoryData.getErrorMessage());
							newCategoryData.setItemDetails(categoryData.getItemDetails());
							newCategoryData.setItemId(categoryData.getItemId());
							newCategoryData.setLevel(categoryData.getLevel());
							newCategoryData.setLocale(categoryData.getLocale());
							newCategoryData.setObjectId(categoryData.getObjectId());
							newCategoryData.setOperationType(categoryData.getOperationType());
							/*
							 * THE STATUS THE ROW IS BEING REPORTED FOR. This copy is hand-written field by field
							 * and left it out, so the category report printed a BLANK status column for every
							 * created and updated category - the value is set on the source object just above.
							 */
							newCategoryData.setProcessingStatus(categoryData.getProcessingStatus());
							newCategoryData.setParentRefKey(categoryData.getParentRefKey());
							newCategoryData.setScheduleId(categoryData.getScheduleId());
							newCategoryData.setSrNo(categoryData.getSrNo());
							categoryProcessingList.add(newCategoryData);
							newCategoryData = null;
							
							if(null!=categoryData.getChildList() && categoryData.getChildList().size()>0)
							{
								childCategoryData = null;
								for(int w=0;w<categoryData.getChildList().size();w++)
								{
									if(isScheduleAborted()) { break; }
									childCategoryData = (IMCategoryDetails)categoryData.getChildList().get(w);
									/*
									 * PROCEED FOR CATEGORY CREATION
									 */
									boolean boolSubChild = createCategoryOperation(childCategoryData);
									if(boolSubChild==true)
									{
										// if Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS SUCCESS
										// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS PROCESSED COUNT
										categoryProcessingDAO.updateProcessingStatus(String.valueOf(childCategoryData.getScheduleId()), ScheduleConstants.STATUS_SUCCESS, String.valueOf(childCategoryData.getItemId()), childCategoryData.getLevel());
										// set ProcessingStatus as Success
										childCategoryData.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
										// set operationType as CREATE_CATEGORY
										childCategoryData.setOperationType("CREATE_CATEGORY");
									}
									else
									{
										// if Level 4 - ONLY UPDATE PROCESSING STATUS FOR THE ITEM AS FAILURE
										// if Level 5 - UPDATE PROCESSING STATUS AS WELL AS FAILURE COUNT
										categoryProcessingDAO.updateProcessingStatus(String.valueOf(childCategoryData.getScheduleId()), ScheduleConstants.STATUS_FAILURE, String.valueOf(childCategoryData.getItemId()), childCategoryData.getLevel());
										// set ProcessingStatus as Success
										childCategoryData.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										// set operationType as CREATE_CATEGORY
										childCategoryData.setOperationType("CREATE_CATEGORY");
									}
									
									// ADD TO PROCESSING LIST
									if(null==categoryProcessingList || categoryProcessingList.size()<=0)
									{
										categoryProcessingList = new ArrayList<IMCategoryDetails>();
									}
									newChildCategoryData  = new IMCategoryDetails();
									newChildCategoryData.setCategoryName(childCategoryData.getCategoryName());
									newChildCategoryData.setCategoryRefKey(childCategoryData.getCategoryRefKey());
									newChildCategoryData.setChildList(childCategoryData.getChildList());
									newChildCategoryData.setErrorCode(childCategoryData.getErrorCode());
									newChildCategoryData.setErrorMessage(childCategoryData.getErrorMessage());
									newChildCategoryData.setItemDetails(childCategoryData.getItemDetails());
									newChildCategoryData.setItemId(childCategoryData.getItemId());
									newChildCategoryData.setLevel(childCategoryData.getLevel());
									newChildCategoryData.setLocale(childCategoryData.getLocale());
									newChildCategoryData.setObjectId(childCategoryData.getObjectId());
									newChildCategoryData.setOperationType(childCategoryData.getOperationType());
									/*
									 * THE STATUS THE ROW IS BEING REPORTED FOR. This copy is hand-written field by field
									 * and left it out, so the category report printed a BLANK status column for every
									 * created and updated category - the value is set on the source object just above.
									 */
									newChildCategoryData.setProcessingStatus(childCategoryData.getProcessingStatus());
									newChildCategoryData.setParentRefKey(childCategoryData.getParentRefKey());
									newChildCategoryData.setScheduleId(childCategoryData.getScheduleId());
									newChildCategoryData.setSrNo(childCategoryData.getSrNo());
									categoryProcessingList.add(newChildCategoryData);
									newChildCategoryData = null;
									childCategoryData = null;
								}
							}
						}
						categoryData = null;
					}
					thirdLevelCategoryData = null;
				}
				thirdLevelCategoryData = null;
				categoryData = null;
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "createCategory()", e);
		} 
	}
	
	/**
	 * CREATE OR UPDATE ONE CATEGORY IN KAPTURE.
	 *
	 * WAS: getCategory() against InfoManager, then addCategory or updateCategory on the IQ client.
	 * NOW: the same add-or-update decision, made from kapture_cms_db.k_categories and carried out
	 * through the category REST API - see KaptureCategoryServiceImpl. kapture_cms_db grants SELECT
	 * only, so the write cannot be a direct INSERT.
	 *
	 * The old getCategory() helper is gone with it: "does this category exist for this locale" is
	 * one SELECT now, not a service round trip that needed its own token-expiry retry.
	 *
	 * FAILURES STILL LAND IN failureCategoryList, unchanged, so a category that could not be
	 * written is skipped for the rest of the run and reported exactly as before.
	 */
	private boolean createCategoryOperation(IMCategoryDetails categoryData)
	{
		boolean bool = false;
		try
		{
			/*
			 * DO NOT PROCEED IF THIS CATEGORY ALREADY FAILED EARLIER IN THIS RUN - carried over
			 * verbatim from the InfoManager version.
			 */
			boolean proceedFurther = true;
			if(null!=failureCategoryList && failureCategoryList.size()>0)
			{
				IMCategoryDetails errorDetails = null;
				for(int r=0;r<failureCategoryList.size();r++)
				{
					errorDetails=  (IMCategoryDetails)failureCategoryList.get(r);
					if(errorDetails.getCategoryRefKey().equals(categoryData.getCategoryRefKey()) &&
							errorDetails.getLocale().equals(categoryData.getLocale()))
					{
						// category exists in failureList do not proceed
						proceedFurther = false;
						break;
					}
					errorDetails = null;
				}
				errorDetails = null;
			}

			if(proceedFurther==false)
			{
				logger.info("createCategoryOperation :: Skip processing for {"+categoryData.getCategoryRefKey()+"} of Locale {"+categoryData.getLocale()+"}. It already failed earlier in this run.");
				return false;
			}

			if(null==cmsConn)
			{
				logger.info("createCategoryOperation :: No Kapture CMS connection. Cannot process {"+categoryData.getCategoryRefKey()+"}.");
				addToFailureList(categoryData, "CMS_CONN", "No connection to kapture_cms_db.");
				return false;
			}

			bool = KaptureCategoryServiceImpl.createOrUpdateCategory(kaptureApi, categoryData,
					categoryData.getLocale(), cmsConn);
			if(bool==true)
			{
				logger.info("createCategoryOperation :: Category {"+categoryData.getCategoryRefKey()+"} written in Kapture for Locale :: >" + categoryData.getLocale());
			}
			else
			{
				logger.info("createCategoryOperation :: Failed to write Category with Ref Key {"+categoryData.getCategoryRefKey()+"} in Kapture for Locale :: >"+ categoryData.getLocale());
				/*
				 * WHAT KAPTURE ACTUALLY SAID, when it said anything. writeCategory() records the
				 * response on the category itself; the generic line is only the fallback for a
				 * failure that never reached the server.
				 */
				String reason = categoryData.getErrorMessage();
				if(null==reason || "".equals(reason.trim()))
				{
					reason = "Failed to create or update the category in Kapture.";
				}
				addToFailureList(categoryData, "KAPTURE_CAT", reason);
			}
		}
		catch (Exception e)
		{
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);

			logger.info("createCategoryOperation :: otherErrors :: Error Code :: >" + e.getMessage());
			addToFailureList(categoryData, e.getMessage(), writer.toString());

			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "createCategoryOperation()", e);
		}
		return bool;
	}

	/** Records a category as failed, so the rest of the run skips it and the report shows it. */
	private void addToFailureList(IMCategoryDetails categoryData, String errorCode, String errorMessage)
	{
		if(null==categoryData)
		{
			return;
		}
		categoryData.setErrorCode(errorCode);
		categoryData.setErrorMessage(errorMessage);
		categoryData.setOperationType("CREATE_CATEGORY");
		if(null==failureCategoryList || failureCategoryList.size()<=0)
		{
			failureCategoryList = new ArrayList<IMCategoryDetails>();
		}
		failureCategoryList.add(categoryData);
	}


	
	
	/**
	 * MAPS THE PROCESSED VIN RANGES ONTO THE DOCUMENT.
	 *
	 * WAS: fetch the ContentRecordITO from InfoManager, push each category onto it, and call
	 * modifyContent on the IQ client.
	 * NOW: exactly the same operation the SI VIN screen performs, through
	 * KaptureContentServiceImpl - display-Article, change the mapped categories, update-article-tab,
	 * and publish-article when the version updated was published. Sharing that one implementation
	 * is deliberate: the screen and the batch must not be able to disagree about how a document
	 * gets tagged.
	 *
	 * THE REPORTING CONTRACT IS UNCHANGED. documentDetails still carries fetchedVersion, contentId,
	 * updatedVersion, published status, and an imProcessingStatus of SUCCESS or FAILURE, because
	 * the notification email and the batch report are built from those fields.
	 */
	private void modifyContent(String documentId, String locale, String wslId, List<IMCategoryDetails> categoriesToBeAddedList)
	{
		// ABORTED FROM THE SCREEN - do no further Kapture work.
		if(isScheduleAborted()) { return; }
		try
		{
			// SET IN DOCUMENT DETAILS OBJECT
			documentDetails = new SIVINScheduleDetails();
			documentDetails.setDocumentId(documentId);
			documentDetails.setLocale(locale);
			documentDetails.setWslId(wslId);

			// explicitly set userId to lowerCase
			wslId=wslId.trim().toLowerCase();

			if(null==cmsConn)
			{
				logger.info("modifyContent :: No Kapture CMS connection. Cannot modify Document Id :: >"+ documentId);
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				return;
			}

			/*
			 * THE DOCUMENT AS KAPTURE HOLDS IT TODAY - the equivalent of the old getContentDetails()
			 * call. getDocumentsData also resolves the checked-out flag, which is why it is used
			 * here rather than reading k_article directly.
			 */
			SIVinDetails content = FetchKaptureDataDAO.getDocumentsData(documentId, locale,
					ApplicationProperties.getProperty("MC.VIN.HIERARCHY.REFKEY"), "MC", cmsConn, "N");
			if (null == content || null == content.getDocumentId() || "".equals(content.getDocumentId()))
			{
				logger.info("modifyContent :: Failed to Fetch Content from Kapture For Document id :: >"+ documentId);
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				return;
			}

			// SET FETCHED VERSION
			documentDetails.setFetchedVersion(content.getFetchedVersion());
			documentDetails.setContentId(FetchKaptureDataDAO.getContentId(documentId, locale, cmsConn));

			if(content.isCheckedOut()==true)
			{
				logger.info("modifyContent :: Document is Checked Out. Failed to Modify Document for DOCUMENT ID :: >"+ documentId+".");
				documentDetails.setErrorCode("IMMOD001");
				documentDetails.setErrorMessage("Document is checked out in Kapture. Cannot be modified.");
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				return;
			}

			/*
			 * COLLECT THE REFERENCE KEYS TO MAP, and record each category on documentDetails so the
			 * report lists exactly what was sent - same as the InfoManager version did.
			 */
			ArrayList<String> categoriesToBeAddedToDocument = new ArrayList<String>();
			if (null != categoriesToBeAddedList && categoriesToBeAddedList.size() > 0)
			{
				IMCategoryDetails catDetails = null;
				for (int i = 0; i < categoriesToBeAddedList.size(); i++)
				{
					if(isScheduleAborted()) { break; }
					catDetails = (IMCategoryDetails) categoriesToBeAddedList.get(i);
					if (null != catDetails.getCategoryRefKey() && !"".equals(catDetails.getCategoryRefKey()))
					{
						categoriesToBeAddedToDocument.add(catDetails.getCategoryRefKey().trim());

						if(null==documentDetails.getCategoryList() || documentDetails.getCategoryList().size()<=0)
						{
							documentDetails.setCategoryList(new ArrayList<IMCategoryDetails>());
						}
						documentDetails.getCategoryList().add(catDetails);
					}
					catDetails = null;
				}
				catDetails = null;
			}

			if(categoriesToBeAddedToDocument.size()<=0)
			{
				logger.info("modifyContent :: No categories to map onto Document Id :: >"+ documentId);
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				return;
			}

			SIVinDetails updated = KaptureContentServiceImpl.modifyContent(documentId, locale, wslId,
					categoriesToBeAddedToDocument, new SIVinDetails());
			if (null == updated || null == updated.getDocumentId() || "".equals(updated.getDocumentId()))
			{
				logger.info("modifyContent :: Failed to Modify Document for DOCUMENT ID :: >"+ documentId);
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				/*
				 * CARRY THE REASON INTO THE REPORT. Without this the modification report shows
				 * Failure with ERROR_CODE and ERROR_MESSAGE empty, and the only way to find out
				 * why is to go back to the server log.
				 */
				if (null != updated)
				{
					documentDetails.setErrorCode(updated.getErrorCode());
					documentDetails.setErrorMessage(updated.getErrorMessage());
				}
				return;
			}

			/*
			 * THE VERSION KAPTURE ENDED UP ON. An update always creates a new version, so this is
			 * re-read rather than assumed - and it is what the report shows as "updated version".
			 */
			KaptureArticleIdentity after = FetchKaptureDataDAO.getArticleIdentity(documentId, locale, cmsConn);
			documentDetails.setUpdatedVersion(after.getArticleVersion());
			documentDetails.setDocumentPublishedStatus(after.isPublished());

			logger.info("modifyContent :: Document Updated successfully for Document Id is :: > " + documentId);
			logger.info(" Content Id :: > "	+ documentDetails.getContentId());
			logger.info(" Version :: > "	+ after.getArticleVersion());
			logger.info(" Doc Status :: > "	+ after.getArticleState());

			/*
			 * A DOCUMENT THAT WAS PUBLISHED AND COULD NOT BE PUBLISHED AGAIN IS A FAILURE, not a
			 * success with a note. The categories were mapped, but the document is left in a state
			 * nobody asked for, and the run report is the only place that would ever say so.
			 */
			if(null!=updated.getErrorCode()
					&& KaptureContentServiceImpl.ERROR_NOT_REPUBLISHED.equals(updated.getErrorCode()))
			{
				logger.info("modifyContent :: {"+documentId+"} was published and could NOT be re-published.");
				documentDetails.setErrorCode(KaptureContentServiceImpl.ERROR_NOT_REPUBLISHED);
				documentDetails.setErrorMessage("Categories were mapped but the document could not be published again. It is left as an unpublished version.");
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
			else
			{
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
			}
		}
		catch (Exception e)
		{
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);

			logger.info("modifyContent :: otherErrors :: Error Code :: >" + e.getMessage());

			if(null!=documentDetails)
			{
				documentDetails.setErrorCode(e.getMessage());
				documentDetails.setErrorMessage(writer.toString());
				documentDetails.setImProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}

			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "modifyContent()", e);
		}
	}


	

	
	
	private void generateHTMLNotificationEmail(String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{
				/*
				 * CALL FUNCTION TO FETCH SCHEDULE AND ITEM DETAIS FOR THE SCHEDULE CODE
				 */
				SIVINScheduleDetails schDetails = categoryProcessingDAO.getScheduleDetails(scheduleCode,"N");
				if(null!=schDetails && schDetails.getScheduleId()>0)
				{
					// set default Portal Host Name
					String hostName=ApplicationProperties.getProperty("application.hostname");
					if(null!=schDetails.getWslId() && !"".equals(schDetails.getWslId()))
					{
						if(schDetails.getWslId().trim().toLowerCase().endsWith("@mazda.co.jp") || 
								schDetails.getWslId().trim().toLowerCase().endsWith("@mazdaeur.com"))
						{
							// MC  OR MME USER = USE GTS HOST
							hostName = ApplicationProperties.getProperty("application.mc.hostname");
						}
					}
					
					/*
					 * NOW LOAD THE EMAIL TEMPLATE
					 * REPLACE THE FOLLOWING PLACE HOLDERS
					 * 
					 * SCHEDULE_NAME
					 * DOCUMENT_ID
					 * LOCALE_ID
					 * END_TIME
					 * JOB_STATUS
					 * 
					 * DATA_PLACE_HOLDER
					 * 
					 * URL_REPORT_DOWNLOAD
					 * HOSTNAME
					 * _CONTEXTNAME
					 * SERVLET_URL
					 * 
					 * COPYRIGHT_YEAR
					 * 
					 * DATE FORMAT - dd MMM yyyy HH:mm:ss
					 */
					String templatePath="/com/mazda/gms3/mdm/email/templates/SIVIN_BATCH_NOTIFICATION_TEMPLATE.html";
					InputStream is = MCSIVINBatchProcessingImpl.class.getResourceAsStream(templatePath);
					if(null!=is)
					{
						Date systemDate = new Date();
						SimpleDateFormat displaySdf = new SimpleDateFormat("dd-MM-yyyy");
						String convDate = displaySdf.format(systemDate);
						if(null!=convDate)
						{
							if(convDate.lastIndexOf("-")!=-1)
							{
								convDate = convDate.substring(convDate.lastIndexOf("-")+1, convDate.length());
							}
						}
						String templateString = Utilities.readInputStramToString(is);
						if(null!=templateString && !"".equals(templateString))
						{
							// HOST NAME + APP CONTEXT
							templateString = templateString.replace("HOSTNAME", hostName);
							// SCHEDULE_JOB_NAME
							if(null!=schDetails.getScheduleName() && !"".equals(schDetails.getScheduleName()))
							{
								templateString = templateString.replace("SCHEDULE_NAME", schDetails.getScheduleName());
							}
							else
							{
								templateString = templateString.replace("SCHEDULE_NAME", "");
							}
							
							// CONTEXTNAME
							templateString  =templateString.replace("_CONTEXTNAME", ApplicationProperties.getProperty("application.environment.context"));
							
							// END_TIME 
							SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
							if(null!=schDetails.getFinishTime())
							{
								templateString = templateString.replace("END_TIME", sdf.format(schDetails.getFinishTime()));
							}
							else
							{
								templateString = templateString.replace("END_TIME", "-");
							}
							
							// JOB_STATUS
							if(null!=schDetails.getJobStatus() && !"".equals(schDetails.getJobStatus()))
							{
								if(schDetails.getJobStatus().equals(ScheduleConstants.STATUS_COMPLETED))
								{
									if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
									{
										templateString = templateString.replace("JOB_STATUS", schDetails.getScheduleStatus());
									}
									else
									{
										templateString = templateString.replace("JOB_STATUS", "-");
									}
								}
								else if(schDetails.getJobStatus().equals(ScheduleConstants.STATUS_ABORTED))
								{
									templateString = templateString.replace("JOB_STATUS", ScheduleConstants.STATUS_FAILURE);
								}
							}
							else
							{
								templateString = templateString.replace("JOB_STATUS", "-");
							}
							
							// DATA_PLACE_HOLDER
							StringBuilder dataHolder = new StringBuilder();
							dataHolder.append("<tr style=\"height:30px;\">");
							dataHolder.append("<td style=\"color:#5C5B65;border-left: none !important;font-size:12px;text-align:center;border-bottom: 1px solid #DADADA;\">1</td>");
							
							// categories creation count
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getProcessedCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getFailureCount()+"</td>");
							
							// mapping count
							int mapCount=0;
							try
							{
								if(null!=schDetails.getReportsPath() && !"".equals(schDetails.getReportsPath()))
								{
									String before = "";
									if(schDetails.getReportsPath().indexOf("/")!=-1)
									{
										before=  schDetails.getReportsPath().substring(0, schDetails.getReportsPath().indexOf("/"));
										if(null!=before && !"".equals(before))
										{
											mapCount=  new Integer(before.trim()).intValue();
										}
									}
									before=null;
								}
							}
							catch(Exception  e)
							{
							}
							
							
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+mapCount+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getTotalCount() -  mapCount)+"</td>");
							
							dataHolder.append("</tr>");
							
							templateString = templateString.replace("DATA_PLACE_HOLDER", dataHolder.toString());
							dataHolder = null;
							
							
							// URL_REPORT_DOWNLOAD - HOSTNAME/dokweb/library/MAZDA/GMS3_CUSTOM/GMS3_REPORTS/SCHEDULECODE/SCHEDULECODE_REPORTS.ZIP
							String url = hostName;
							url = url+ApplicationProperties.getProperty("application.infocenter.web.context");
							url = url+ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY_WEB");
							url = url +scheduleCode+"/"+scheduleCode+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
							templateString = templateString.replace("URL_REPORT_DOWNLOAD", url);
							url = null;
							
							// REPALCE COPYRIGHT YEAR
							templateString = templateString.replace("COPYRIGHT_YEAR", String.valueOf(convDate));
							
							// REPLACE DOCUMENT_ID
							templateString = templateString.replace("DOCUMENT_ID", schDetails.getDocumentId());
							// REPLACE LOCALE_ID
							templateString = templateString.replace("LOCALE_ID", schDetails.getLocale());
							// REPLACE SERVLET_URL
							templateString = templateString.replace("SERVLET_URL", "mmesivin");
							
							/*
							 * CALL FUNCTION TO SEND EMAIL
							 */
							
							/*
							 * call function to get user Details
							 */
							String toMailId = categoryProcessingDAO.getUserEmailDetails(schDetails.getWslId());
							if(null!=toMailId && !"".equals(toMailId))
							{
								ArrayList<String> toList = new ArrayList<String>();
								toList.add(toMailId);
								
								String subject = "SI VIN Mapping Summary - "+ schDetails.getScheduleName();
								boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID"));
								if(sendEmail==true)
								{
									logger.info("generateNotificationEmail() :: Notification Sent Successfully."); 
								}
								else
								{
									logger.info("generateNotificationEmail() :: Failed to Send Email Notification.");
								}
								subject = null;
								toList = null;
							}
							else
							{
								logger.info("generateNotificationEmail() :: Failed to fetch Email details for User :: > "+ schDetails.getWslId()+". Failed to Send Email Notification."); 
							}
							toMailId=null;
							templateString = null;
						}
					}
					hostName = null;
				}
				schDetails = null;
			}
			else
			{
				logger.info("generateNotificationEmail :: Schedule Code as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "generateNotificationEmail()", e);
		}
	}

	private void generateTextNotificationEmail(String scheduleCode, String market)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{
				/*
				 * CALL FUNCTION TO FETCH SCHEDULE AND ITEM DETAIS FOR THE SCHEDULE CODE
				 */
				SIVINScheduleDetails schDetails = categoryProcessingDAO.getScheduleDetails(scheduleCode,"N");
				if(null!=schDetails && schDetails.getScheduleId()>0)
				{
					
					StringBuilder message = new StringBuilder();
					String status="";
					String messageStr="<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
							+ "	style=\"margin-left: auto; margin-right: auto;\" color: \"#333\">"
							+ "<tr>"
							+ "		<td style=\"font-size: 12px; color: #333;\">Dear User,<br/></td>"
							+ "	</tr>"
							+ "	<tr>		<td>&nbsp;</td>"
							+ "	</tr>"
							+ "	<tr>		<td style=\"font-size: 12px; color: #333;\">MGSS "+market+" - SI VIN Mapping Job "+schDetails.getScheduleId()+" has been finished ";
					
					message.append(messageStr);
					// END_TIME 
					SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
					if(null!=schDetails.getFinishTime())
					{
						message.append(" at "+sdf.format(schDetails.getFinishTime())+", ");
					}
					message.append(" with status as ");
					// JOB_STATUS
					if(null!=schDetails.getJobStatus() && !"".equals(schDetails.getJobStatus()))
					{
						if(schDetails.getJobStatus().equals(ScheduleConstants.STATUS_COMPLETED))
						{
							if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
							{
								message.append("<b>"+schDetails.getScheduleStatus()+"</b>");
								status= schDetails.getScheduleStatus();
							}
							else
							{
								message.append("-");
								status="-";
							}
						}
						else if(schDetails.getJobStatus().equals(ScheduleConstants.STATUS_ABORTED))
						{
							message.append("<b>"+ScheduleConstants.STATUS_FAILURE+"</b>");
							status = ScheduleConstants.STATUS_FAILURE;
						}
					}
					else
					{
						message.append("-");
						status="-";
					}

					message.append(". Please find attached report for reference.</td>");
					message.append("	</tr>");	
					message.append("<tr>");
					message.append("<td>&nbsp;</td>");
					message.append("</tr>");

					message.append("<tr>");
					message.append("<td style=\"font-size: 12px; color: #333;\">For more details,");
					message.append("please refer MDM - "+market+" SI VIN Range - SI VIN Schedule Page.</td>");
					message.append("</tr>");
					message.append("<tr>");
					message.append("	<td>&nbsp;</td>");
					message.append("</tr>");
					message.append("<tr>");
					message.append("	<td style=\"font-size: 12px; color: #333;\">Regards,<br />MGSS");
					message.append("	Application.<br />Note - This is an auto generated email. Please do");
					message.append("	not respond to this email.");
					message.append("</td>");
					message.append("</tr>");
					message.append("</table>");
					
					messageStr = null;
					

					/*
					 * CALL FUNCTION TO SEND EMAIL
					 */
					
					/*
					 * call function to get user Details
					 * 
					 * check here, if wsl id is skoguru
					 * check for pre-defined email ids for SI VIN BATCH 
					 * if available - use them for sending emails
					 * else send it to skoguru
					 */
					String toMailId = categoryProcessingDAO.getUserEmailDetails(schDetails.getWslId());
					if(null!=toMailId && !"".equals(toMailId))
					{
						String finalMails="";
						if(ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS").trim().
								toLowerCase().indexOf(schDetails.getWslId().trim().toLowerCase()) != -1)
						{
							try
							{
								finalMails=  ApplicationProperties.getProperty("category.email.ids.for.internalusers");
							}
							
							catch(Exception e)
							{}
						}
						
						
						if(null!=finalMails && !"".equals(finalMails))
						{
							toMailId=  finalMails;
						}
						finalMails = null;
						
						ArrayList<String> toList = new ArrayList<String>();
						String[] tok = toMailId.split(",");
						if(null!=tok && tok.length>0)
						{
							for(int r=0;r<tok.length;r++)
							{
								toList.add(tok[r]);
							}
						}
						tok = null;
						String subject = market+" SI VIN Mapping Summary - "+ schDetails.getScheduleId() +" finished -"+ status;
						String attachmentPath = ApplicationProperties.getProperty("SIVIN_BATCH_REPORTS_DIRECTORY")+scheduleCode+"/"+scheduleCode+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
						boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMailWithAttachment(toList, subject, message.toString(), ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID"), attachmentPath);
						if(sendEmail==true)
						{
							logger.info("generateNotificationEmail() :: Notification Sent Successfully."); 
						}
						else
						{
							logger.info("generateNotificationEmail() :: Failed to Send Email Notification.");
						}
						subject = null;
						toList = null;
					}
					else
					{
						logger.info("generateNotificationEmail() :: Failed to fetch Email details for User :: > "+ schDetails.getWslId()+". Failed to Send Email Notification."); 
					}
					toMailId=null;
				}
				schDetails = null;
			}
			else
			{
				logger.info("generateNotificationEmail :: Schedule Code as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVINBatchProcessingImpl.class.getName(), "generateNotificationEmail()", e);
		}
	}

}