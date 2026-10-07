package com.mazda.gms3.dmt.automation.impl;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientException;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.im.ito.CategoryITO;
import com.inquira.im.ito.CategoryKeyITO;
import com.inquira.im.ito.ContentRecordITO;
import com.inquira.im.ito.RepositoryKeyITO;
import com.inquira.im.ito.impl.ContentRecordITOImpl;
import com.inquira.util.ewr.ErrorRecord;
import com.inquira.util.ewr.ErrorWarningResponse;
import com.mazda.gms3.dmt.automation.dao.AutomationDAO;
import com.mazda.gms3.dmt.automation.dao.AutomationIdentificationDAO;
import com.mazda.gms3.dmt.automation.dao.TransactionDAO;
import com.mazda.gms3.dmt.automation.utils.PrintReportsUtils;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.conversion.impl.StartMCConversionImpl;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;

public class StartAutomationProcessingImpl {

	private Logger logger = LogManager.getLogger(StartAutomationProcessingImpl.class);
	
	private IQServiceClient client=null;
	
	private int authTokenGenerationCount=0;
	
	private int authCheckCount = 0;
	
	private boolean jobFailureStatus = false;
	
	private PrintReportsUtils printReportUtils=null;
	
	private ArrayList<Map<String, Object>> errorsList = null;
	
	private ArrayList<Map<String, Object>> transactionList = null;
	
	private TransactionDAO transactionDAO =null;
	
	@SuppressWarnings("unchecked")
	public void startAutomation(ScheduleDetails scheduleDetails, ArrayList<ExcelRowDetails> inputDataList)
	{
		boolean sendNotificationEmail=false;
		try
		{
			if(null!=scheduleDetails && scheduleDetails.getScheduleId()>0)
			{
				// initialize all variables
				printReportUtils = new PrintReportsUtils();
				errorsList = new ArrayList<Map<String,Object>>();
				transactionList = new ArrayList<Map<String,Object>>();
				transactionDAO = new TransactionDAO();
				if((null!=scheduleDetails.getAutomationItemsList() && scheduleDetails.getAutomationItemsList().size()>0) ||  
					(null!=inputDataList && inputDataList.size()>0	))
				{
					/*
					 * Update the Status as Processing for the Schedule
					 */
					ScheduleDAO.updateScheduleStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.processing.value"));
					
					if(null!=scheduleDetails.getAutomationItemsList() && scheduleDetails.getAutomationItemsList().size()>0)
					{
						/*
						 * ITERATE AND START PROCESSING EACH ITEM
						 */
						ItemDetails details = new ItemDetails();
						Map<String, Object> documentMap = null;
						Map<String, Object> errorMap = null;
						ArrayList<ExcelRowDetails> vinList = null;
						for(int a=0;a<scheduleDetails.getAutomationItemsList().size();a++)
						{
							details = (ItemDetails)scheduleDetails.getAutomationItemsList().get(a);
							if(details.getItemId()>0 && null!=details.getLocale() && !"".equals(details.getLocale()) && 
									null!=details.getChannelRefKey() && !"".equals(details.getChannelRefKey()) && 
									null!=details.getCarlineInfo() && !"".equals(details.getCarlineInfo()))
							{
								/*
								 * DELETE ENTRY FOR THIS ITEM ON THE BASIS OF DOC IDENTIFICATION SCH ID AND DOC IDENTIFICATION ITEM ID FROM
								 * DOC IDENTIFICATION SCHEDULE TABLES - ALSO UPDATE THE DOCUMENTS COUNT
								 */
								if(null!=details.getDocIdnSchId() && details.getDocIdnSchId().longValue()>0 && null!=details.getDocIdnItemId() && details.getDocIdnItemId().longValue()>0)
								{
									int totalDocsCountToReduce = 0;
									if(null!=details.getImpactedDocumentsList() && details.getImpactedDocumentsList().size()>0)
									{
										totalDocsCountToReduce = details.getImpactedDocumentsList().size();
									}
									AutomationIdentificationDAO.deleteItemDetails(details.getDocIdnSchId(), details.getDocIdnItemId(), totalDocsCountToReduce);
								}
								/*
								 * Update Current Job Status as DOCUMENT PROCESSING FOR BOTH ITEM & SCHDEULE
								 */
								ScheduleDAO.updateCurrentJobStatus(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()), "DOCUMENT PROCESSING");
								
								if(null!=details.getImpactedDocumentsList() && details.getImpactedDocumentsList().size()>0)
								{
									if(details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES) || 
											details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE) || 
											details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_TRAINING) || 
											details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS))
									{
										long processedCount = details.getImpactedDocumentsList().size();
										// update all documents Count as successDocuments Count
										ScheduleDAO.updateMultipleProcessingCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()),
												processedCount);
										/*
										 * PRINT REPORTS OF THE ITEM
										 * CALL OTHER CHANNEL PRINTING REPORTS
										 */
										printReportUtils.printOtherChannelReports(details, String.valueOf(scheduleDetails.getScheduleId()));

										// update itemStatus as SUCCESS
										ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
									}
									else if(details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE) || 
											details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE) || 
											details.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS))
									{
										// re-initialize transactionList & errorsList
										transactionList = new ArrayList<Map<String,Object>>();
										errorsList = new ArrayList<Map<String,Object>>();

										/*
										 * ITERATE IMPACTED DOCUMENTS LIST
										 * REMOVE OLD VINS AND ADD NEW VINS
										 * UPDATE DOCUMENT BACK IN INFOMANAGER.
										 */
										documentMap = new HashMap<String, Object>();
										for(int r=0;r<details.getImpactedDocumentsList().size();r++)
										{
											documentMap = (Map<String, Object>)details.getImpactedDocumentsList().get(r);
											logger.info("################## PROCESSING "+details.getChannelRefKey() +" DOCUMENT OF "+details.getLocale()+" -> "+(r+1) +" / "+ details.getImpactedDocumentsList().size());
											if(null!=documentMap.get(AutomationConstants.DOCUMENTID) && !"".equals(documentMap.get(AutomationConstants.DOCUMENTID)))
											{
												// add itemDetails & scheduleDetails to documentMap
												documentMap.put(AutomationConstants.ITEM_DETAILS, details);
												documentMap.put(AutomationConstants.SCHEDULE_DETAILS, scheduleDetails);


												if(null!=documentMap.get(AutomationConstants.VIN_LIST))
												{
													vinList = (ArrayList<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST);
												}

												if(null!=vinList && vinList.size()>0)
												{
													/*
													 * fetch Document Id + Locale data from InfoManager
													 */
													documentMap = modifyDocument(documentMap, vinList, details.getLocale(), scheduleDetails.getUserId());
													if(null!=documentMap && null!=documentMap.get(AutomationConstants.CONTENT_ID) && !"".equals(documentMap.get(AutomationConstants.CONTENT_ID)))
													{
														// PERFORM DB OPERATIONS FOR SM & WD DOCUMENTS
														documentMap=transactionDAO.updateDocumentDetails(documentMap, details.getLocale(), documentMap.get(AutomationConstants.DOCUMENTID).toString(), vinList);
														if(null!=documentMap && null!=documentMap.get(AutomationConstants.PROCESSING_STATUS) && documentMap.get(AutomationConstants.PROCESSING_STATUS).equals(AutomationConstants.STATUS_SUCCESS))
														{
															logger.info("startProcessing :: "+documentMap.get(AutomationConstants.DOCUMENTID)+" details updated successfully in Database.");
															// update success count for item & schedule Both = Document processing is successful
															ScheduleDAO.updateProcessingCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()));
															// set processing status as SUCCESS
															documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_SUCCESS);
														}
														else
														{
															logger.info("startProcessing :: Failed to Modify "+documentMap.get(AutomationConstants.DOCUMENTID)+" in Database.");
															/*
															 * add this document to errorList, as Database errors not added yet
															 */
															// add to error List
															errorMap = new HashMap<String, Object>();
															errorMap.put(AutomationConstants.ERROR_CODE, documentMap.get(AutomationConstants.ERROR_CODE));
															errorMap.put(AutomationConstants.ERROR_MESSAGE, documentMap.get(AutomationConstants.ERROR_MESSAGE));
															errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
															errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
															errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));

															// add erDetails to imErrorDetailsList
															if (null == errorsList 	|| errorsList.size() <= 0) 
															{
																errorsList = new ArrayList<Map<String, Object>>();
															}
															errorsList.add(errorMap);

															errorMap = null;

															// some error while processing document - Set jobFailureStatus = true, as error Found
															jobFailureStatus = true;
															// update failureCount for item & schedule Both - AS Database Processing Failed
															ScheduleDAO.updateFailureCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()));
															// set processing status as FAILURE
															documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
															// set errorMessage
															documentMap.put(AutomationConstants.ERROR_MESSAGE, "Failed to perform Modify Operaion in Database. Please refer failure reports.");
														}
													}
													else
													{
														logger.info("startProcessing :: Failed to Modify "+documentMap.get(AutomationConstants.DOCUMENTID)+" in InfoManager.");
														// check here if document does not exist in Error List - add to errorList, else skip
														boolean docFound = false;
														if(null!=errorsList && errorsList.size()>0)
														{
															errorMap = new HashMap<String, Object>();
															for(int fr=0;fr<errorsList.size();fr++)
															{
																errorMap = (Map<String, Object>)errorsList.get(fr);
																if(null!=errorMap && null!=errorMap.get(AutomationConstants.DOCUMENTID) && !"".equals(errorMap.get(AutomationConstants.DOCUMENTID)))
																{
																	if(errorMap.get(AutomationConstants.DOCUMENTID).equals(documentMap.get(AutomationConstants.DOCUMENTID)))
																	{
																		// NO NEED OF ADDING THIS DOCUMENT TO ERROR LIST - ALREADY ADDED
																		docFound = true;
																		break;
																	}
																}
																errorMap = null;
															}
															errorMap = null;
														}

														if(docFound==false)
														{
															// add to error List
															errorMap = new HashMap<String, Object>();
															errorMap.put(AutomationConstants.ERROR_CODE, "IMEXP003");
															errorMap.put(AutomationConstants.ERROR_MESSAGE, "FAILED TO MODIFY DOCUMENT IN INFO MANAGER.");
															errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
															errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
															errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));

															// add erDetails to imErrorDetailsList
															if (null == errorsList 	|| errorsList.size() <= 0) 
															{
																errorsList = new ArrayList<Map<String, Object>>();
															}
															errorsList.add(errorMap);

															errorMap = null;
														}

														// some error while processing document - Set jobFailureStatus = true, as error Found
														jobFailureStatus = true;
														// update failureCount for item & schedule Both - AS Document Processing Failed
														ScheduleDAO.updateFailureCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()));
														// set processing status as FAILURE
														documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
														// set errorMessage
														documentMap.put(AutomationConstants.ERROR_MESSAGE, "Failed to perform Modify Operaion in InfoManager. Please refer failure reports.");
													}
												}
												else
												{
													// set jobFailureStatus = true - Fail the Job
													jobFailureStatus = true;
													// update failureCount for item & schedule Both - AS VINs to be replaced are null
													ScheduleDAO.updateFailureCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()));
													// set processing status as FAILURE
													documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
													// set errorMessage
													documentMap.put(AutomationConstants.ERROR_MESSAGE, "Applicable VINs to be replaced are null.");
												}
												vinList = null;
											}
											else
											{
												// set jobFailureStatus = true - Fail the Job
												jobFailureStatus = true;
												// update failureCount for item & schedule Both - AS Document Id is null
												ScheduleDAO.updateFailureCount(String.valueOf(scheduleDetails.getScheduleId()), String.valueOf(details.getItemId()));
												// set processing status as FAILURE
												documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
												// set errorMessage
												documentMap.put(AutomationConstants.ERROR_MESSAGE, "Document Id is null.");
											}


											// add documentMap to transactionList
											if(null==transactionList || transactionList.size()<=0)
											{
												transactionList = new ArrayList<Map<String,Object>>();
											}
											transactionList.add(documentMap);

											documentMap= null;
										}
										documentMap = null;

										/*
										 * PRINT TRANSACTION & ERROR REPORTS FOR PROCESSING ITEM
										 */
										if(null!=transactionList && transactionList.size()>0)
										{
											printReportUtils.printSMWDChannelReports(details, String.valueOf(scheduleDetails.getScheduleId()), transactionList);
										}

										if(null!=errorsList && errorsList.size()>0)
										{
											printReportUtils.printErrorReports(details, String.valueOf(scheduleDetails.getScheduleId()), errorsList);
										}

										if(null!=errorsList && errorsList.size()>0)
										{
											logger.info("startAutomation :: Errors Found for Item"+details.getLocale()+" >> "+details.getChannelRefKey()+" >> "+ details.getCarlineInfo()+". Update Status of the Item as Failure." );
											/*
											 * Update Item Status as Failure.
											 */
											ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_FAILURE);
											// set jobFailureStatus = true - Fail the Job
											jobFailureStatus = true;
										}
										else
										{
											logger.info("startAutomation :: No Erros Found for Item"+details.getLocale()+" >> "+details.getChannelRefKey()+" >> "+ details.getCarlineInfo()+". Update Status of the Item as Success." );
											/*
											 * No Errors Found. Update Item Status as Success. NO Errors Found
											 */
											ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
										}

										transactionList = null;
										errorsList = null;
										errorMap = null;
									}
								}
								else
								{
									logger.info("startAutomation :: No Impacted Documents Found for Item"+details.getLocale()+" >> "+details.getChannelRefKey()+" >> "+ details.getCarlineInfo()+". Update Status of the Item as Success." );
									/*
									 * No Impacted Documents Found. Update Item Status as Success. NO Impacted Documents Found
									 */
									ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
								}
							}
							else
							{
								logger.info("startAutomation :: Item at Index ("+a+") does not have all mandatory Parameters. Exiting its Processing.");
								if(details.getItemId()>0)
								{
									/*
									 * Update Item Status as Failure.
									 */
									ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_FAILURE);
									// set jobFailureStatus = true - Fail the Job
									jobFailureStatus = true;
								}
							}
						}
						details = null;
					}
					
					/*
					 * TO DO VIN MANUAL TYPE PROCESSING
					 */
					
					if(null!=inputDataList && inputDataList.size()>0)
					{
						// identify manual types for the old vin for locale
						// identify manual types for the new vin for locale
						// match them if all manual types matches, delete the entry for old vin form vin manual mapping table & create entry for new vin.
						// if not matches - update the old vin row and new vin row , if they do not exist create for both 
						
						inputDataList= AutomationDAO.processVinManualTypeMapping(inputDataList);
						if(null!=inputDataList  && inputDataList.size()>0)
						{
							ExcelRowDetails vinDetails = null;
							for(int a=0;a<inputDataList.size();a++)
							{
								vinDetails=  (ExcelRowDetails )inputDataList.get(a);
								if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(vinDetails.getLocale().replace("-", "_").trim().toLowerCase())>-1)
								{
									/*
									 * check if for any row status is blank or failure - set jobStatus as failure
									 */
									if(null==vinDetails.getProcessingStatus() || "".equals(vinDetails.getProcessingStatus()))
									{
										// set jobFailureStatus = true - Fail the Job
										jobFailureStatus = true;
										break;
									}
									else
									{
										if(null!=vinDetails.getProcessingStatus() && vinDetails.getProcessingStatus().equals(AutomationConstants.STATUS_FAILURE))
										{
											// set jobFailureStatus = true - Fail the Job
											jobFailureStatus = true;
											break;
										}
									}
									vinDetails= null;	
								}
							}
							/*
							 * PRINT VIN MANUAL TYPE MAPPING REPORT
							 */
							printReportUtils.printVINManualMappingReport(String.valueOf(scheduleDetails.getScheduleId()), inputDataList);
						}
					}
					
					if(jobFailureStatus==true)
					{
						logger.info("###############################################################################");
						logger.info("startAutomation :: Errors Found, Updating Status as Failure . Exit Conversion.");
						logger.info("###############################################################################");
						/*
						 * SOME ERRORS HAVE BEEN FOUND WHILE EXECUTING THE JOB.
						 * UPDATE SCHEDULE STATUS AS FAILURE
						 */
						ScheduleDAO.updateScheduleStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.failure.value"));
						// send Notification Email
						sendNotificationEmail = true;
					}
					else
					{
						logger.info("###############################################################################");
						logger.info("startAutomation :: No Errors Found, Updating Status as Success . Exit Conversion.");
						logger.info("###############################################################################");
						/*
						 * NO ERRORS FOUND - UPDATE SCHEDULE STATUS AS SUCCESS
						 */
						ScheduleDAO.updateScheduleStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.success.value"));
						// send Notification Email
						sendNotificationEmail = true;
					}
				}
				else
				{

					logger.info("###############################################################################");
					logger.info("startAutomation :: No Items Found, Updating Status as Success . Exit Conversion.");
					logger.info("###############################################################################");
					/*
					 * Update the Status as Success for the Schedule
					 */
					ScheduleDAO.updateScheduleStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.success.value"));
					// send Notification Email
					sendNotificationEmail = true;
				}
			}
			else
			{
				logger.info("####################################################################");
				logger.info("startAutomation :: Schedule Id is null as parameter. Exit Conversion.");
				logger.info("####################################################################");
			}
			
			
			if(null!=scheduleDetails && scheduleDetails.getScheduleId()>0)
			{
				/*
				 * call function to generate reports
				 */
				generateZIP(String.valueOf(scheduleDetails.getScheduleId()));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "startAutomation()", e);
			/*
			 * Update Status to Failure
			 */
			try {
				ScheduleDAO.updateScheduleStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.failure.value"));
				ScheduleDAO.updateScheduleAllItemStatus(String.valueOf(scheduleDetails.getScheduleId()), ApplicationProperties.getProperty("schedule.status.failure.value"));
				// send Notification Email
				sendNotificationEmail = true;
			} catch (Exception e1) {
				Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "startAutomation()", e1);
			}
		}
		finally
		{
			logger.info(" ******************** StartAutomationProcessingImpl :: FOR SCHEDULE ID {"+scheduleDetails.getScheduleId()+"}. GENERATED AUTH TOKEN COUNTS ARE :: > "+ authTokenGenerationCount);
			logger.info(" ******************** StartAutomationProcessingImpl :: FOR SCHEDULE ID {"+scheduleDetails.getScheduleId()+"}. TOTAL OPERATION COUNTS ARE :: > "+ authCheckCount);
			if(null!=client)
			{
				logger.info(" ******************** StartAutomationProcessingImpl :: CLOSING IQ SERVICE CLIENT OBJECT. ***********************");
				client.close();
			}
			
			// set all used variables to null
			client = null;
			authTokenGenerationCount=0;
			authCheckCount=0;
			
			printReportUtils = null;
			errorsList = null;
			transactionList = null;
			transactionDAO = null;
		}
		
		/*
		 * CHECK FOR REQUIRED PARAMS AND SENT NOTIFICATION EMAILS
		 */
		if(null!=scheduleDetails && null!=scheduleDetails.getUserId() && !"".equals(scheduleDetails.getUserId()) && sendNotificationEmail==true)
		{
			// set userId as null for FaceLift
			NotificationEmailHelper.generateNotificationEmail(String.valueOf(scheduleDetails.getScheduleId()), null);
		}
		
		if(null!=scheduleDetails && scheduleDetails.getScheduleId()>0)
		{
			// call stopCurrentThread 
			stopActiveThead(String.valueOf(scheduleDetails.getScheduleId()));
		}
	}
	
	/**
	 * Function will Perform the GET Content Operation in IM
	 * @param contentDetails
	 * @return
	 */
	private ContentRecordITO getContentDetails(Map<String, Object> documentMap , String documentId, String locale, String wslId) 
	{	
		ContentRecordITO crIto = new ContentRecordITOImpl();
		try 
		{
			try
			{
				crIto = client.getContentRecordRequest().getLatestContentRecordByDocumentIDAndLocale(documentId,locale);
			}
			catch(Exception e)
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(documentMap, wslId);
				crIto = client.getContentRecordRequest().getLatestContentRecordByDocumentIDAndLocale(documentId,locale);
			}
		} 
		catch (IQServiceClientException e) 
		{
			if (null!=client) 
			{
				captureErrorDetails(client.getEWR(),  documentMap);
			}
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "getContentDetails()", e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "getContentDetails()", e);
			captureOtherErrorDetails(e, documentMap);
		}
		return crIto;
	}
	
	private Map<String, Object> modifyDocument(Map<String, Object> documentMap, ArrayList<ExcelRowDetails> vinList, String locale, String wslId)
	{
		try
		{
			String documentId = (String)documentMap.get(AutomationConstants.DOCUMENTID);
			locale = locale.replace("-", "_");
			// fetch contentDetails
			// create Object of NewContentRecordRequest
			ContentRecordITOImpl content = new ContentRecordITOImpl();
			content = (ContentRecordITOImpl) getContentDetails(documentMap, documentId, locale, wslId);
			if (null != content && null != content.getDocumentID() 	&& !"".equals(content.getDocumentID())) 
			{
				// check if document is checkedOut
				if(content.getCheckedOut()==false)
				{
					/*
					 * Proceed for modifyingDocument
					 */
					if(null!=vinList && vinList.size()>0)
					{
						ExcelRowDetails vinDetails=null;
						CategoryKeyITO catKeyIto = null;
						// iterate and remove all OlD VINS FROM DOCUMENT
						for(int a=0;a<vinList.size();a++)
						{
							vinDetails = (ExcelRowDetails) vinList.get(a);
							if(null!=vinDetails.getOldRefKey() && !"".equals(vinDetails.getOldRefKey()))
							{
								if(null!=content.getCategories() && content.getCategories().size()>0)
								{
									catKeyIto = null;
									for(int t=0;t<content.getCategories().size();t++)
									{
										catKeyIto = (CategoryKeyITO)content.getCategories().get(t);
										if(null!=catKeyIto && null!=catKeyIto.getReferenceKey() && !"".equals(catKeyIto.getReferenceKey()))
										{
											// check if matches with oldRfKey remove it
											if(catKeyIto.getReferenceKey().trim().toLowerCase().equals(vinDetails.getOldRefKey().trim().toLowerCase()))
											{
												content.getCategories().remove(t);
												t--;
												break;
											}
										}
										catKeyIto = null;
									}
								}
							}
							vinDetails = null;
						}
						
						
						
						// iterate and add all NEW VINS TO DOCUMENT
						vinDetails = null;
						catKeyIto = null;
						CategoryITO categoryITO = null;
						for(int a=0;a<vinList.size();a++)
						{
							vinDetails = (ExcelRowDetails) vinList.get(a);
							if(null!=vinDetails.getNewRefKey() && !"".equals(vinDetails.getNewRefKey()))
							{
								categoryITO = addCategoryToDocument(vinDetails.getNewRefKey(), documentMap, wslId);
								if (null != categoryITO	&& null != categoryITO.getReferenceKey() && !"".equals(categoryITO.getReferenceKey())) 
								{
									if(null==content.getCategories() || content.getCategories().size()<=0)
									{
										content.setCategories(new ArrayList<CategoryKeyITO>());
									}
									// add category to categories List
									content.getCategories().add(categoryITO);
								}
								categoryITO = null;
							}
							vinDetails = null;
						}
						categoryITO = null;
					}
					
					// modifyContent
					ContentRecordITO crIto=null;
					try
					{
						crIto  = client.getContentRecordRequest().modifyContent(content, content.getPublished());
					}
					catch(Exception e)
					{
						// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
						getConnectionWithIM(documentMap, wslId);
						crIto  = client.getContentRecordRequest().modifyContent(content, content.getPublished());
					}

					if (null != crIto && null != crIto.getDocumentID() 	&& !"".equals(crIto.getDocumentID()))
					{
						logger.info("modifyDocument :: Document {"+documentId+"} modified successfully. Updated Version :: > "	+ crIto.getMajorVersion()+"."+crIto.getMinorVersion());
						logger.info(" Document Id :: > " 	+ crIto.getDocumentID());
						logger.info(" Content Id :: > " 		+ crIto.getContentID());
						logger.info(" Version :: > " 		+ crIto.getMajorVersion());
						logger.info(" Resource Path :: > "	+ crIto.getResourcePath());
						logger.info(" Doc Status :: > "		+ crIto.getPublished());

						/*
						 * Update the DOCUMENT MAP
						 */
						documentMap.put(AutomationConstants.CONTENT_ID, crIto.getContentID());
						documentMap.put(AutomationConstants.DOCUMENT_VERSION, String.valueOf(crIto.getMajorVersion())+"."+String.valueOf(crIto.getMinorVersion()));
						if (crIto.getPublished() == true) 
						{
							// SET PUBLISH
							documentMap.put(AutomationConstants.DOCUMENT_STATUS, "Publish");
						} else 
						{
							// SET UN-PUBLISH or DRAFT
							documentMap.put(AutomationConstants.DOCUMENT_STATUS, "Draft");
						}
					}
					else
					{
						logger.info("modifyContent :: Failed to Modify Document :: >"+documentId);

						/*
						 * CAPTURE ERROR DETAILS 
						 */
						Map<String, Object> errorMap = new HashMap<String, Object>();
						errorMap.put(AutomationConstants.ERROR_CODE, "IMEXP003");
						errorMap.put(AutomationConstants.ERROR_MESSAGE, "FAILED TO MODIFY DOCUMENT IN INFO MANAGER.");
						errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
						errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
						errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));


						// add erDetails to imErrorDetailsList
						if (null == errorsList 	|| errorsList.size() <= 0) 
						{
							errorsList = new ArrayList<Map<String, Object>>();
						}
						errorsList.add(errorMap);
					
						errorMap = null;
					}
					crIto = null;
				}
				else
				{
					logger.info("modifyContent :: Document Id :: >"+ documentId+" is checked out in IM. Cannot proceed for modifying document.");
					
					Map<String, Object> errorMap = new HashMap<String, Object>();
					errorMap.put(AutomationConstants.ERROR_CODE, "IMEXP002");
					errorMap.put(AutomationConstants.ERROR_MESSAGE, "DOCUMENT IS CHECKED OUT IN INFO MANAGER.");
					errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
					errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
					errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));


					// add erDetails to imErrorDetailsList
					if (null == errorsList 	|| errorsList.size() <= 0) 
					{
						errorsList = new ArrayList<Map<String, Object>>();
					}
					errorsList.add(errorMap);
					errorMap = null;
				}
			}
			else 
			{
				logger.info("modifyContent :: Failed to Fetch Content from IM For Document Id :: >"+ documentId);
				
				Map<String, Object> errorMap = new HashMap<String, Object>();
				errorMap.put(AutomationConstants.ERROR_CODE, "IMEXP001");
				errorMap.put(AutomationConstants.ERROR_MESSAGE, "FAILED TO FETCH DOCUMENT FROM INFO MANAGER.");
				errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
				errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
				errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));


				// add erDetails to imErrorDetailsList
				if (null == errorsList 	|| errorsList.size() <= 0) 
				{
					errorsList = new ArrayList<Map<String, Object>>();
				}
				errorsList.add(errorMap);
				errorMap = null;
			}
			content = null;
			documentId = null;
		}
		catch (IQServiceClientException e) 
		{
			if(null!=client)
			{
				captureErrorDetails(client.getEWR(),documentMap);
			}
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(),"modifyDocument()" , e);
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "modifyDocument()" , e);
			captureOtherErrorDetails(e,documentMap);
		}
		return documentMap;
	}
	
	/**
	 * Function will check whether TOKEN IS VALID OR NOT 
	 * IF NOT VALID = THEMN WILL GENERATE THE NEW TOKEN AND SET IT IN SCOPE
	 * @param contentDetails
	 */
	private void getConnectionWithIM(Map<String, Object> documentMap, String wslId) {
		authCheckCount++;
		boolean establishConnnection = false;
		try
		{
			if(null==client || null==client.getAuthenticationToken() || "".equals(client.getAuthenticationToken()) || client.isValid()==false)
			{
				logger.info("getConnectionWithIM :: AUTHENTICATION TOKEN IS INVALID / EXPIRED. RE-INITIATE CLIENT AND GET NEW TOKEN.");
				establishConnnection = true;
			}
			
			RepositoryKeyITO rk = client.getRepositoryRequest().getRepositoryKeyByReferenceKey(ApplicationProperties.getProperty("REPOSITORY").toUpperCase());
			if(null!=rk)
			{
				logger.info("getConnectionWithIM :: REPOSITORY INFO RECEIVED FETCHED REPOSITORY IS ::: > " + rk.getRecordID());
			}
			rk = null;
		}
		catch(Exception e)
		{
			logger.info("getConnectionWithIM :: EXCEPTION WHILE VALIDATING AUTHENTICATION TOKEN. RE-INITIATE CLIENT AND GET NEW TOKEN.");
			establishConnnection=true;
		}
		
		if(establishConnnection==true)
		{
			authTokenGenerationCount++;
			logger.info("getConnectionWithIM :: GENERATING NEW TOKEN FOR SINCE, EITHER CLIENT IS NULL OR AUTHENTICATION TOKEN IS EXPIRED.");
			if(null!=client)
			{
				client.close();
			}
			client = null;
			
			/* 
			 * CHANGE DATE - 21 SEPTEMBER 2017
			 * FINAL USER FOR GENERATING TOKEN WILL ALWAYS BY DEFAULT USER -OKADMIN
			 */
			String finalUserToSet="";
			if(null!=wslId && !"".equals(wslId))
			{
				finalUserToSet= wslId.trim().toLowerCase();
			}
			else
			{
				// SINCE WSL IS NULL - SET DEFAULT USER  - OKADMIN
				finalUserToSet = ApplicationProperties.getProperty("USERNAME");
			}
			String internalUsers = ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS");
			if(null!=internalUsers && !"".equals(internalUsers))
			{
				if(internalUsers.contains(finalUserToSet.trim().toLowerCase()))
				{
					// set as OKADMIN
					finalUserToSet = ApplicationProperties.getProperty("USERNAME");
				}
			}
			internalUsers = null;
			
			try {
				client= IQServiceClientManager
						.connect(
								finalUserToSet,
										""
												+ ApplicationProperties
												.getProperty("PASSWORD") + "",
												""
														+ ApplicationProperties
														.getProperty("DOMAIN") + "",
														""
																+ ApplicationProperties
																.getProperty("REPOSITORY") + "",
																""
																		+ ApplicationProperties
																		.getProperty("IM_CLIENT_LIBRARY_ENDPOINT")
																		+ "",
																		""
																				+ ApplicationProperties
																				.getProperty("SEARCH_CLIENT_LIBRARY_ENDPOINT")
																				+ "", null, true);
			
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "getConnectionWithIM()", e);
				captureOtherErrorDetails(e, documentMap);
			}
			finalUserToSet = null;
		}
		
		logger.info("getConnectionWithIM() :: CLIENT :: >" + client);
		if(null!=client)
		{
			logger.info("getConnectionWithIM() :: AUTHENTICATION TOKEN :: >" + client.getAuthenticationToken());
			logger.info("getConnectionWithIM() :: IS VALID :: >" + client.isValid());
		}
	}

	/**
	 * Function will CAPTURE OTHER EXCEPTION DETAILS
	 * @param 
	 * @param contentDetails
	 * @param e
	 */
	private void captureOtherErrorDetails(Exception e,  Map<String, Object> documentMap) 
	{
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		e.printStackTrace(print);
		String errorCode = e.getMessage();
		String errorMessage = writer.toString();

		logger.info("captureOtherError :: Error Code :: >" + errorCode);
		logger.info("captureOtherError :: Error Message :: >" + errorMessage);

		Map<String, Object> errorMap = new HashMap<String, Object>();
		errorMap.put(AutomationConstants.ERROR_CODE, String.valueOf(errorCode));
		errorMap.put(AutomationConstants.ERROR_MESSAGE, errorMessage);
		errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
		errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
		errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));
		if(null!=documentMap.get(AutomationConstants.MISSING_REFKEY) && !"".equals(documentMap.get(AutomationConstants.MISSING_REFKEY)))
		{
			errorMap.put(AutomationConstants.MISSING_REFKEY, documentMap.get(AutomationConstants.MISSING_REFKEY));
		}

		// add erDetails to imErrorDetailsList
		if (null == errorsList 	|| errorsList.size() <= 0) 
		{
			errorsList = new ArrayList<Map<String, Object>>();
		}
		errorsList.add(errorMap);
	
		errorMap = null;
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
	 * Function will CAPTURE IM EXCEPTION DETAILS.
	 * @param ewr
	 * @param 
	 * @param contentDetails
	 * @return
	 */
	private boolean captureErrorDetails(ErrorWarningResponse ewr, Map<String, Object> documentMap) 
	{
		boolean result = true;
		if (null!=ewr && ewr.hasErrorsOrWarnings()) 
		{
			// EWR reported a problem, check to see if it is an error
			if (ewr.hasErrors()) 
			{
				// error was reported
				result = true;
				// output the errors
				List<ErrorRecord> errors = ewr.getErrors();
				for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
					ErrorRecord rec = iter.next();
					logger.info("checkEWR :: Error Code :: >" + rec.getCode());
					logger.info("checkEWR :: Error Message :: >" + rec.getMessage());
					
					Map<String, Object> errorMap = new HashMap<String, Object>();
					errorMap.put(AutomationConstants.ERROR_CODE, String.valueOf(rec.getCode()));
					errorMap.put(AutomationConstants.ERROR_MESSAGE, rec.getMessage());
					errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
					errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
					errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));
					if(null!=documentMap.get(AutomationConstants.MISSING_REFKEY) && !"".equals(documentMap.get(AutomationConstants.MISSING_REFKEY)))
					{
						errorMap.put(AutomationConstants.MISSING_REFKEY, documentMap.get(AutomationConstants.MISSING_REFKEY));
					}
					// add erDetails to imErrorDetailsList
					if (null == errorsList 	|| errorsList.size() <= 0) 
					{
						errorsList = new ArrayList<Map<String, Object>>();
					}
					errorsList.add(errorMap);
				

					errorMap = null;
					rec = null;
				}
				errors = null;
			} 
			else 
			{
				// warning must have been reported, assume it is safe to go on
				result = false;
			}
		} 
		else 
		{
			result = false;
		}
		return result;
	}

	
	/**
	 * FUNCTION WILL ADD CATEGORY INFORMATION WITH THE DOCUMENT
	 * @param refKey
	 * @param 
	 * @param contentDetails
	 * @param catDetails
	 * @return
	 */
	private CategoryITO addCategoryToDocument(String refKey, Map<String, Object> documentMap, String wslId)
	{
		CategoryITO catITO = null;
		if(null!=refKey && !"".equals(refKey))
		{
			try 
			{
				catITO = client.getCategoryRequest().getCategoryByReferenceKey(refKey);
			} 
			catch (Exception e) 
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(documentMap, wslId);
				try
				{
					catITO = client.getCategoryRequest().getCategoryByReferenceKey(refKey);
				}
				catch (IQServiceClientException e1) 
				{
					logger.info("addCategoryToDocument ::  Failed to IDENTIFY REF KEY FROM IM FOR CATEGORY KEY :: > " + refKey);
					if(null!=client)
					{
						documentMap.put(AutomationConstants.MISSING_REFKEY, refKey);
						captureErrorDetails(client.getEWR(),documentMap);
					}
					logger.info("addCategoryToDocument :: Error :: >" + e1.getMessage());
					Utilities.printStackTraceToLogs(StartMCConversionImpl.class.getName(),"addCategoryToDocument" , e1);
				}
				catch(Exception e1)
				{
					// STILL GETTING EXCEPTION - CATCH IT.
					logger.info("addCategoryToDocument :: Failed to IDENTIFY REF KEY FROM IM FOR CATEGORY KEY :: > " + refKey);
					documentMap.put(AutomationConstants.MISSING_REFKEY, refKey);
					Utilities.printStackTraceToLogs(StartMCConversionImpl.class.getName(),"addCategoryToDocument" , e1);
					// also AddDocument to failureList
					captureOtherErrorDetails(e1,documentMap);
				}
			}
		}
		return catITO;
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
			String threadId=ApplicationProperties.getProperty("automation.sch.key")+scheduleCode;
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
								Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "stopActiveThead()", e);
		}
	}
	
	private void generateZIP(String scheduleCode)
	{
		try
		{
			/*
			 * now here, generate the Zip file for all the reports generated the
			 * path for reports directory will be -
			 * REPORTS_DIRECTORY/schCode
			 */
			String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");
			if(!path.endsWith("/"))
			{
				path = path+"/";
			}
			path = path+scheduleCode+"/";
			/*
			 * call function to generate zip file
			 */
			boolean bool = printReportUtils.createReportsZip(path, scheduleCode);
			if (bool == true) 
			{
				logger.info("printReports :: Reports Zipped Successfully.");
			} 
			else 
			{
				logger.info("printReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
			path = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "generateZIP()", e);
		}
	}
	
	
}
