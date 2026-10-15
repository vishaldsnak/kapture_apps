package com.mazda.gms3.mdm.dataexttool.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportTransactionDAO;
import com.mazda.gms3.mdm.dataexttool.utils.DataExportToolPrintUtils;
import com.mazda.gms3.mdm.dataexttool.utils.DataExportToolUtils;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolAttachmentDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolInnerLinkDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolScheduleDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.SendMailUsingAuthentication;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportToolDAO;

public class DataExportScheduleImpl {

	private Logger logger = LogManager.getLogger(DataExportScheduleImpl.class);

	private DataExportTransactionDAO transactionDAO = null;

	private DataExportToolUtils dataExportUtils = null;

	private ArrayList<DataExportToolItemDetails> transactionList = null;

	private boolean anyErrorReceivedDuringCompleteSchedule = false;

	private DataExportToolPrintUtils printUtils = null;

	private ArrayList<DataExportToolAttachmentDetails> fileTransactionList = null;

	private ArrayList<DataExportToolItemDetails> innerLinksList = null;

	private ArrayList<DataExportToolInnerLinkDetails> innerLinksTransactionList = null;

	private ArrayList<DataExportToolAttachmentDetails> wdOkAssetsListForItems=null;
	
	private List<MNAOViewContentDetails> siViewContentList = null;
	
	private List<MNAOViewContentDetails> smViewContentList = null;


	/*
	 * HAS THE SCREEN ABORTED THIS SCHEDULE?
	 *
	 * The Abort button used to call Thread.stop() on this worker. That was REMOVED in Java 20 and
	 * throws UnsupportedOperationException, which the screen's catch(Exception) swallowed - so the
	 * schedule was marked Aborted while this thread carried on to the end. Nothing can kill another
	 * thread any more, so the worker stops itself by reading the status the screen already wrote.
	 *
	 * Throttled: the answer is cached for ABORT_CHECK_INTERVAL_MS and latches once aborted, so a
	 * fast loop cannot turn this into a query per iteration. Worst case the check itself is half a second
	 * behind the click - the rest of any delay is the operation already in flight.
	 */
	private static final long ABORT_CHECK_INTERVAL_MS = 500L;

	private long lastAbortCheckTime = 0L;

	private boolean scheduleAborted = false;

	private String abortScheduleId = null;

	private boolean isScheduleAborted()
	{
		if(scheduleAborted==true)
		{
			return true;
		}
		if(null==abortScheduleId || "".equals(abortScheduleId))
		{
			return false;
		}
		long now = System.currentTimeMillis();
		if(now-lastAbortCheckTime < ABORT_CHECK_INTERVAL_MS)
		{
			return false;
		}
		lastAbortCheckTime = now;
		scheduleAborted = DataExportToolDAO.isAborted(abortScheduleId);
		if(scheduleAborted==true)
		{
			logger.info("isScheduleAborted :: SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
					+" SCREEN. Stopping after the item in flight.");
		}
		return scheduleAborted;
	}
	public void startProcessing(DataExportToolScheduleDetails schDetails)
	{
		// remember the schedule so the abort check works in the helpers
		abortScheduleId = String.valueOf(schDetails.getScheduleId());
		String threadId = null;
		String zipFilePathForEmailNotif=null;
		try
		{
			if(null!=schDetails && schDetails.getScheduleId()>0 && null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				transactionDAO = new DataExportTransactionDAO();
				dataExportUtils = new DataExportToolUtils();
				printUtils = new DataExportToolPrintUtils();
				transactionList = null;
				// set threadId
				threadId = schDetails.getThreadId();

				/*
				 *  UPDATE SCHEDULE PROCESSING STATUS TO PROCESSING
				 */
				transactionDAO.updateScheduleProcessingStatus(ScheduleConstants.STATUS_PROCESSING,null, String.valueOf(schDetails.getScheduleId()), false);
				/*
				 * start iterating Items List and then extracting each of the document against it
				 */
				DataExportToolItemDetails itemDetails = null;
				for(int a=0;a<schDetails.getItemsList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					itemDetails = (DataExportToolItemDetails) schDetails.getItemsList().get(a);
					itemDetails.setScheduleId(schDetails.getScheduleId());
					/*
					 * GET ITEM ID FOR THE PROCESSING ITEM
					 */
					itemDetails = transactionDAO.getItemId(itemDetails);

					/*
					 * REPLACE / IN MODEL NAME BY _
					 */
					itemDetails.setModel(itemDetails.getModel().replace("/", "_"));

					/*
					 * call Function for itemsProcessing
					 */
					startItemsProcessing(itemDetails);
					itemDetails = null;
				}
				itemDetails = null;

				/*
				 * GENERATE ZIP FILE OF THE WORKING DIRECTORY FOR SCHEULE ID TO BE DOWNLOADED
				 */
				String xmlZipSize =null;
				String reportZipSize = null;
				String path = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				path+=String.valueOf(schDetails.getScheduleId())+"/";
//				path+=String.valueOf(schDetails.getScheduleName())+"/";

				/*
				 * START INNERLINK PROCESSING
				 */
				if(null!=innerLinksList && innerLinksList.size()>0)
				{
					/*
					 * ITERTAE FOR EACH ITEM
					 * 		LOOK FOR THE DOCUMENTID.XML IN THE RMI TOOL WORKING DIRECTORY
					 * 		PREPARE ITS RELATIVE PATH AND REPLACE IT WITH SORUCE PATH IN THE <A> TAGS
					 * 		REWRITE THE XML IN THE RMI TOOL WORKING DIRECTORY
					 */
					startInnerLinkProcessing(path);
				}

				/*
				 * GENERATE CHANNEL WISE VIEW CONTNET LIST REPORT
				 */
				if(null!=smViewContentList && smViewContentList.size()>0)
				{
					/*
					 * ABORTED FROM THE SCREEN - STOP HERE, exactly as the pre-migration code did.
					 *
					 * THIS MUST SIT BEFORE THE FIRST REPORT IS WRITTEN. The View Content reports below run
					 * ahead of the transaction reports, so a guard placed further down still let them be
					 * produced for an aborted schedule - the mistake seen live on SI Channel 4329. On Java 8
					 * stop() killed the thread here and no report existed at all.
					 */
					if(isScheduleAborted()) { return; }
					printUtils.printSMChannelViewContentModelYearReport(smViewContentList, String.valueOf(schDetails.getScheduleId()));
					printUtils.printSMChannelViewContentVINReport(smViewContentList, String.valueOf(schDetails.getScheduleId()));
				}
				if(null!=siViewContentList && siViewContentList.size()>0)
				{
					printUtils.printSIChannelViewContentModelYearReport(siViewContentList, String.valueOf(schDetails.getScheduleId()));
					printUtils.printSIChannelViewContentVINReport(siViewContentList, String.valueOf(schDetails.getScheduleId()));
				}
				
				String customScheduleErrorMessage = null;

				boolean createWDZipFlag = printUtils.createWorkingDirZip(path, String.valueOf(schDetails.getScheduleId()), schDetails.getScheduleName());
				if (createWDZipFlag == true) 
				{
					logger.info("startProcessing() :: XML Files Zipped Successfully.");
					/*
					 * LOOK FOR THE FILE SIZE OF ZIP FILE
					 */
					String zipFilePath = path	+ String.valueOf(schDetails.getScheduleName())+"_XMLS"	+ ApplicationProperties.getProperty("ZIP_SUFFIX");
					File zipFile = new File(zipFilePath);
					if(zipFile.isFile() && zipFile.exists())
					{
						long size = zipFile.length();
						xmlZipSize = getStringSizeLengthFile(size);
						size = 0;
						
						/*
						 * Create Zip File Path for Email Notification
						 */
						zipFilePathForEmailNotif =ApplicationProperties.getProperty("application.hostname");
						zipFilePathForEmailNotif+= ApplicationProperties.getProperty("dataexttool.working.dir.web.path");
						// add scheduleName
						zipFilePathForEmailNotif+=String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleName())+"_XMLS";
						// add Zip Extension
						zipFilePathForEmailNotif+=ApplicationProperties.getProperty("ZIP_SUFFIX");
					}
					zipFile = null;
					zipFilePath = null;
				} 
				else 
				{
					logger.info("startProcessing() :: Failed to Zip XML Files, these have to be downloaded manually.");
					// fail Job
					anyErrorReceivedDuringCompleteSchedule = true;
					customScheduleErrorMessage="Failed to generate zip for exported XML Files. ";
				}
				path = null;

				/*
				 * PRINT REPORTS
				 * AND GENERATE ZIP FILE OF REPORTS
				 */
				if(null!=transactionList && transactionList.size()>0)
				{
					/*
					 * ABORTED FROM THE SCREEN - STOP HERE, exactly as the pre-migration code did.
					 *
					 * In the InfoManager original the screen called Thread.stop() and Java 8 killed this
					 * thread outright, so nothing below this point ever ran: no reports, no completion
					 * status, no notification e-mail. Thread.stop() was removed in Java 20, so the worker
					 * has to reproduce that itself. No abort e-mail is sent because the original sends
					 * none - NotificationEmailHelper.generateNotificationEmail() is commented out in every
					 * abort screen there.
					 *
					 * Returning is safe: the connections are closed in the finally of this method.
					 */
					if(isScheduleAborted()) { return; }

					printUtils.printTransactionReport(transactionList, String.valueOf(schDetails.getScheduleId()), transactionDAO);
				}
				if(null!=fileTransactionList && fileTransactionList.size()>0)
				{
					printUtils.printFilesTransactionReport(fileTransactionList, String.valueOf(schDetails.getScheduleId()),transactionDAO);
				}
				if(null!=innerLinksTransactionList && innerLinksTransactionList.size()>0)
				{
					printUtils.printInnerLinkTransactionReport(innerLinksTransactionList, String.valueOf(schDetails.getScheduleId()),transactionDAO);
				}
				

				// GENERATE REPORTS ZIP
				path = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
				if(!path.endsWith("/"))
				{
					path = path+"/";
				}
				path+=String.valueOf(schDetails.getScheduleId())+"/";
				/*
				 * call function to generate zip file
				 */
				boolean bool = printUtils.createReportsZip(path, String.valueOf(schDetails.getScheduleName()));
				if (bool == true) 
				{
					logger.info("startProcessing() :: Reports Zipped Successfully.");
					/*
					 * LOOK FOR REPORTS ZIP FILE AND GET IS SIZE
					 */
					String reportZipFilePath = path	+ String.valueOf(schDetails.getScheduleName())	+ "_"	+ ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
					File reportZipFile = new File(reportZipFilePath);
					if(reportZipFile.isFile() && reportZipFile.exists())
					{
						long size = reportZipFile.length();
						reportZipSize = getStringSizeLengthFile(size);
						size = 0;
					}
					reportZipFile = null;
					reportZipFilePath = null;
				} 
				else 
				{
					logger.info("startProcessing() :: Failed to Zip Reports, these have to be downloaded manually.");
					// fail Job
					anyErrorReceivedDuringCompleteSchedule = true;
					customScheduleErrorMessage="Failed to generate zip file for Reports. Contact Admin for Access to Reports.";
				}
				path = null;

				/*
				 * CALL FUNCTION TO UPDATE REPORTS ZIP FILE SIZE AND ZIP FILE SIZE
				 */
				transactionDAO.updateXMLZIPAndReportsZipSize(reportZipSize, xmlZipSize, String.valueOf(schDetails.getScheduleId()));
				reportZipSize = null;
				xmlZipSize = null;
				
				/*
				 * UPDATE SCHEDULE STATUS NOW - PROCESSING STATUS AS COMPLETED AND 
				 * COMPLETION STATUS BASED ON anyErrorReceivedDuringCompleteSchedule FLAG VALUE
				 */
				String schStatus=null;
				if(anyErrorReceivedDuringCompleteSchedule==true)
				{
					logger.info("startProcessing() :: Some Exception has occured while processing Export. Update Schedule Status as Failure.");
					// errors received for any item processing
					transactionDAO.updateScheduleProcessingStatus(ScheduleConstants.STATUS_COMPLETED,ScheduleConstants.STATUS_FAILURE, String.valueOf(schDetails.getScheduleId()), true);
					// update error remarks as well if any
					if(null!=customScheduleErrorMessage && !"".equals(customScheduleErrorMessage))
					{
						transactionDAO.updateScheduleCustomError(customScheduleErrorMessage, String.valueOf(schDetails.getScheduleId()));
					}
					schStatus = ScheduleConstants.STATUS_FAILURE;
				}
				else
				{
					logger.info("startProcessing() :: No Exceptions occured while execution. Update Schedule Status as Success.");
					// no errors received
					transactionDAO.updateScheduleProcessingStatus(ScheduleConstants.STATUS_COMPLETED,ScheduleConstants.STATUS_SUCCESS, String.valueOf(schDetails.getScheduleId()), true);
					schStatus = ScheduleConstants.STATUS_SUCCESS;
				}
				customScheduleErrorMessage = null;

				/*
				 * SEND NOTIFICATION EMAIL
				 */
				sendEmailNotification(String.valueOf(schDetails.getScheduleId()), schStatus, schDetails.getScheduleName(), schDetails.getWslId(), zipFilePathForEmailNotif);
				schStatus = null;
			}
			else
			{
				logger.info("startProcessing() :: Schedule Details are Parameter is null or Schedule id is null or Items List is null. Exiting Processing.");
				/*
				 * UPDATE SCHEDULE PROCESSING STATUS TO COMPLETE AND JOB COMPLETION STATUS TO FAILURE
				 */
				transactionDAO.updateHardJobFailure(schDetails.getScheduleId());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startProcessing()", e);
			/*
			 * UPDATE SCHEDULE PROCESSING STATUS TO COMPLETE AND JOB COMPLETION STATUS TO FAILURE
			 */
			transactionDAO.updateHardJobFailure(schDetails.getScheduleId());
		}
		finally
		{
			// close all active connection & client objects
			try
			{
				if(null!=transactionDAO.conn)
				{
					logger.info(" ******************** DataExportScheduleImpl :: CLOSING DB CONNECTION OBJECT. ***********************");
					transactionDAO.conn.close();
				}
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startProcessing()", e);
			}

			logger.info(" ******************** DataExportScheduleImpl :: FOR SCHEDULE ID {"+schDetails.getScheduleId()+"}. GENERATED DB CONNECTION COUNTS ARE :: > "+ transactionDAO.connectionCount);

			transactionDAO.connectionCount = 0;
			transactionDAO = null;
			dataExportUtils =null;
			transactionList = null;
			fileTransactionList = null;
			schDetails =  null;
			anyErrorReceivedDuringCompleteSchedule = false;
			innerLinksList = null;
			innerLinksTransactionList = null;
			wdOkAssetsListForItems = null;
			siViewContentList = null;
			smViewContentList = null;
			zipFilePathForEmailNotif  =null;
		}

	}


	private void startItemsProcessing(DataExportToolItemDetails itemDetails)
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
//			logger.info("startItemsProcessing :: ------------------> "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getCarlineCode()+" >>" + itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());
			logger.info("startItemsProcessing :: ------------------> "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());
			if(null!=itemDetails.getDocumentIdsList() && itemDetails.getDocumentIdsList().size()>0)
			{
				/*
				 * UPDATE ITEM PROCESSING STATUS TO PROCESSING AND COMPLETION STATUS TO NULL
				 */
				transactionDAO.updateItemProcessingStatus(ScheduleConstants.STATUS_PROCESSING, null,  itemDetails.getItemId());

				String documentId=null;
				int count=0;
				String liveFolderPath = ApplicationProperties.getProperty("dataexttool.live.folder.path");
				String channelFolderPath=null;
				File documentXMLFile = null;
				String xmlFilteredData = null;
				String rmiExportToolWorkingDirectoryPath = ApplicationProperties.getProperty("dataexttool.working.dir.folder.path");
				String tempWriteFolderPath=null;
				String actualWriteFolderPath = null;
				File revrifyXMLFile = null;
				boolean writeFlag = false;
				boolean reverifyFlag = false;
				boolean anyErrorFound=false;
				ArrayList<DataExportToolAttachmentDetails> docAttachmentsList = null;
				DataExportToolAttachmentDetails attachmentDetails = null;
				boolean errorsInAttachmentsMoving = false;
				String attachmentReplacementPath = null;
				DataExportToolItemDetails newItemDetails = null;
				boolean errorsInImagesMoving = false;
				boolean errorsInPDFsMoving = false;
				ArrayList<DataExportToolInnerLinkDetails> docInnerLinksList = null;
				List<DataExportToolInnerLinkDetails> otherManualLinksList = null;
				DataExportToolItemDetails innerLinkItemDetails = null;
				DataExportToolItemDetails innerLinkItemDocumentDetails = null;
				long totalInnerLinksCount=0;
				String wiringDiagramDocumentIdForOkAssetsMovement=null;
				DataExportToolInnerLinkDetails inrLkDetails  =null;
				List<MNAOViewContentDetails> viewContentListForDoucment=  null;
				for(int a=0;a<itemDetails.getDocumentIdsList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					documentId = String.valueOf(itemDetails.getDocumentIdsList().get(a));
					count++;
					logger.info("############## Start Processing >> "+documentId+" {"+count+" / "+ itemDetails.getDocumentIdsList().size()+"}.");

					if(null!=itemDetails.getChannelName() && itemDetails.getChannelName().trim().toLowerCase().equals("WIRING_DIAGRAMS".trim().toLowerCase()))
					{
						// set ANY Document Id FOR IDENTIFYING THE MODEL FOLDER NAME
						wiringDiagramDocumentIdForOkAssetsMovement = documentId;
					}

					/*
					 * SET DATA IN NEW ITEM DETAILS 
					 * REFERENCING HAPPENING
					 */
					newItemDetails = new DataExportToolItemDetails();
					newItemDetails.setScheduleId(itemDetails.getScheduleId());
					newItemDetails.setItemId(itemDetails.getItemId());
					newItemDetails.setLocale(itemDetails.getLocale());
//					newItemDetails.setCarlineCode(itemDetails.getCarlineCode());
					newItemDetails.setModel(itemDetails.getModel());
					newItemDetails.setChannelName(itemDetails.getChannelName());
					newItemDetails.setDocumentType(itemDetails.getDocumentType());

					/*
					 * IDENTIFY DOCUMENT XML FROM LIVE FOLDER BASED ON 
					 */
					channelFolderPath = liveFolderPath;
					if(!channelFolderPath.endsWith("/"))
					{
						channelFolderPath=channelFolderPath+"/";
					}
					channelFolderPath = channelFolderPath+ newItemDetails.getChannelName();
					// set Data in ItemDetails
					newItemDetails.setChannelFolderPath(channelFolderPath);
					newItemDetails.setDocumentId(documentId);
					/*
					 * FETCH VIEW CONTENT DETAILS FOR THE DOCUMENT
					 */
//					viewContentListForDoucment = transactionDAO.getViewContentDetails(newItemDetails.getDocumentId(), newItemDetails.getLocale(), 
//							newItemDetails.getModel(), newItemDetails.getCarlineCode());
					viewContentListForDoucment = transactionDAO.getViewContentDetails(newItemDetails.getDocumentId(), newItemDetails.getLocale(), 
							newItemDetails.getModel());
					if(null!=viewContentListForDoucment && viewContentListForDoucment.size()>0)
					{
						// add view contentData to channelWise Specific List
//						addViewContentDataToChannelSpecificList(viewContentListForDoucment, newItemDetails.getLocale(), newItemDetails.getModel(), 
//								newItemDetails.getCarlineCode(), newItemDetails.getDocumentId());
						addViewContentDataToChannelSpecificList(viewContentListForDoucment, newItemDetails.getLocale(), newItemDetails.getModel(), 
								null, newItemDetails.getDocumentId());
						
						/*
						 * IDENTIFY DOCUMENT XML FILE 
						 */
						documentXMLFile = dataExportUtils.getDocumentXMLFile(channelFolderPath, documentId, newItemDetails.getLocale());
						if(null!=documentXMLFile && documentXMLFile.exists() && documentXMLFile.isFile())
						{
							// set XML File in ItemDetails
							newItemDetails.setXmlFile(documentXMLFile);
							/*
							 * CONVERT THIS XML FILE TO:
							 *  1.) XML DOCUMENT
							 *  2.) REMOVE UNWANTED NODES
							 *  3.) IDENTIFY ESI CATEGORY NODES AND ADD THEIR TYPE, E.G. - ESI CATEGORY / VIN / CVC CATEGORY ETC
							 *  4.) TRANSFORM THE REMAINING XML TO STRING 
							 */
							xmlFilteredData = dataExportUtils.getXMLFilteredData(documentXMLFile, newItemDetails.getChannelName());
							if(null!=xmlFilteredData && !"".equals(xmlFilteredData))
							{
								/*
								 * generate Folder Structure Path and Write XML File over there
								 * WorkingDirectoryPath + tempWriteFolderPath
//								 * tempWriteFolderPath = scheduleId/locale/Model CarlineCode/Channel/DocumentType/DocumentId
								 * tempWriteFolderPath = scheduleId/locale/Model /Channel/DocumentType/DocumentId
								 */
//								tempWriteFolderPath = String.valueOf(newItemDetails.getScheduleId())+"/"+newItemDetails.getLocale()+"/"+
//										newItemDetails.getModel()+" "+newItemDetails.getCarlineCode()+"/"+newItemDetails.getChannelName()+"/";
								tempWriteFolderPath = String.valueOf(newItemDetails.getScheduleId())+"/"+newItemDetails.getLocale()+"/"+
										newItemDetails.getModel()+"/"+newItemDetails.getChannelName()+"/";
								if(null!=newItemDetails.getDocumentType() && !"".equals(newItemDetails.getDocumentType()))
								{
									tempWriteFolderPath = tempWriteFolderPath+newItemDetails.getDocumentType()+"/";
								}
								tempWriteFolderPath  = tempWriteFolderPath+newItemDetails.getDocumentId();

								actualWriteFolderPath = dataExportUtils.createFolderStructure(tempWriteFolderPath, rmiExportToolWorkingDirectoryPath);
								if(null!=actualWriteFolderPath && !"".equals(actualWriteFolderPath))
								{
									// SET DOCUMENT DIR PATH IN ITEM DETAILS - SET IT BEFORE ADDING DOCUMENT.XML TO IT
									newItemDetails.setDocumentDirPath(actualWriteFolderPath);

									/*
									 * PERFORM ATTACHMENTS OPERATION FOR THE FILTERED DOCUMENT XML DATA
									 */
									docAttachmentsList = attachmentsOperation(xmlFilteredData, documentXMLFile.getParentFile().getAbsolutePath(), newItemDetails); 
									if(null!=docAttachmentsList && docAttachmentsList.size()>0)
									{
										// add to fileTransactionList
										if(null==fileTransactionList || fileTransactionList.size()<=0)
										{
											fileTransactionList = new ArrayList<DataExportToolAttachmentDetails>();
										}
										fileTransactionList.addAll(docAttachmentsList);

										// DO NOT FAIL DOCUMENT INCASE OF AN ATTACHMENT FAILURE
										/*
										 * ITERATE & VERIFY IF ALL ATTACHMENTS ARE COPIED SUCCESSFULLY
										 */
										// re-initialize Flag for attachments check
										errorsInAttachmentsMoving = false;
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if((null==attachmentDetails.getProcessingStatus()) || (null!=attachmentDetails.getProcessingStatus() && !attachmentDetails.getProcessingStatus().equalsIgnoreCase(ScheduleConstants.STATUS_SUCCESS)))
											{
												// update failureCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
												// some attachments failed while moving
												errorsInAttachmentsMoving=true;
												//											break;
											}
											else if(null!=attachmentDetails.getProcessingStatus() && attachmentDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
											{
												// update successCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);
											}
											attachmentDetails = null;
										}

										/*
										 * REPLACE THE ATTACHMENTS PATH IN XML FILTERED DATA
										 */
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if(null!=attachmentDetails.getName() && !"".equals(attachmentDetails.getName()))
											{
												/*
												 * replace <![CDATA[ATTACHMENT NAME]]> WITH
												 * <![CDATA[LOCALE/MODEL/CHANNEL/<DOCUMENT TYPE IF APPLICABLE>/DOCUMENT_ID/ATTACHMENT NAME]]>
												 */
//												attachmentReplacementPath = newItemDetails.getLocale()+"/"+newItemDetails.getModel()+" "+newItemDetails.getCarlineCode()+"/"+newItemDetails.getChannelName()+"/";
												attachmentReplacementPath = newItemDetails.getLocale()+"/"+newItemDetails.getModel()+"/"+newItemDetails.getChannelName()+"/";
												if(null!=newItemDetails.getDocumentType() && !"".equals(newItemDetails.getDocumentType()))
												{
													attachmentReplacementPath+=newItemDetails.getDocumentType()+"/";
												}
												attachmentReplacementPath+=newItemDetails.getDocumentId()+"/"+attachmentDetails.getName();

												xmlFilteredData = xmlFilteredData.replace("<![CDATA["+attachmentDetails.getName()+"]]>", "<![CDATA["+attachmentReplacementPath+"]]>");
												attachmentReplacementPath= null;
											}
											attachmentDetails = null;
										}
									}
									docAttachmentsList = null;

									if(errorsInAttachmentsMoving==true)
									{
										/*
										 * errors found while processing Attachments
										 * DO NOT FAIL DOCUMENT IN THIS SCENARIO - ONLY ITEM WILL BE FAILED
										 * DETAILS CAN BE TRACKED FROM SUMMARY.
										 */
										//									newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										//									newItemDetails.setErrorMessage("FAILED TO MOVE SOME ATTACHEMENTS FOR THE DOCUMENT. PLEASE REFER TO FILE TRANSACTIONS REPORT.");
										/*
										 * set anyErrorFound flag to ture, as some error generated for the processing of this item
										 * the item will be in Failure State
										 */
										anyErrorFound = true;
									}

									/*
									 * PERFORM IMAGES OPERATION FOR THE FILTERED DOCUMENT XML DATA
									 */
									docAttachmentsList = dataExportUtils.readInlineImages(xmlFilteredData, newItemDetails);
									if(null!=docAttachmentsList && docAttachmentsList.size()>0)
									{
										// add to fileTransactionList
										if(null==fileTransactionList || fileTransactionList.size()<=0)
										{
											fileTransactionList = new ArrayList<DataExportToolAttachmentDetails>();
										}
										fileTransactionList.addAll(docAttachmentsList);

										/*
										 * ITERATE & VERIFY IF ALL IMAGES ARE COPIED SUCCESSFULLY
										 */
										// re-initialize Flag for images check
										errorsInImagesMoving = false;
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if((null==attachmentDetails.getProcessingStatus()) || (null!=attachmentDetails.getProcessingStatus() && !attachmentDetails.getProcessingStatus().equalsIgnoreCase(ScheduleConstants.STATUS_SUCCESS)))
											{
												// update failureCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
												// some images failed while moving
												errorsInImagesMoving=true;
												//											break;
											}
											else if(null!=attachmentDetails.getProcessingStatus() && attachmentDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
											{
												// update successCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);
											}
											attachmentDetails = null;
										}

										/*
										 * REPLACE THE IMAGES PATH IN XML FILTERED DATA
										 */
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if(null!=attachmentDetails.getSrcHrefPath() && !"".equals(attachmentDetails.getSrcHrefPath()) 
													&& null!=attachmentDetails.getPathToBeReplaced() && !"".equals(attachmentDetails.getPathToBeReplaced()))
											{
												/*
												 * replace SRC ORIGINAL PATH WITH RMI TOOL PATHS TO BE REPLACED
												 */
												xmlFilteredData = xmlFilteredData.replace(attachmentDetails.getSrcHrefPath(), attachmentDetails.getPathToBeReplaced());
											}
											attachmentDetails = null;
										}
									}
									docAttachmentsList = null;

									if(errorsInImagesMoving==true)
									{
										/*
										 * errors found while processing Images
										 * DO NOT FAIL DOCUMENT IN THIS SCENARIO - ONLY ITEM WILL BE FAILED
										 * DETAILS CAN BE TRACKED FROM SUMMARY.
										 */
										//									newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										//									if(null!=newItemDetails.getErrorMessage() && !"".equals(newItemDetails.getErrorMessage()))
										//									{
										//										newItemDetails.setErrorMessage(newItemDetails.getErrorMessage()+"\n"+"FAILED TO MOVE SOME INLINE IMAGES FOR THE DOCUMENT. PLEASE REFER TO FILE TRANSACTIONS REPORT.");
										//									}
										//									else
										//									{
										//										newItemDetails.setErrorMessage("FAILED TO MOVE SOME INLINE IMAGES FOR THE DOCUMENT. PLEASE REFER TO FILE TRANSACTIONS REPORT.");
										//									}
										/*
										 * set anyErrorFound flag to ture, as some error generated for the processing of this item
										 * the item will be in Failure State
										 */
										anyErrorFound = true;
									}


									/*
									 * PERFORM PDFS OPERATION FOR THE FILTERED DOCUMENT XML DATA
									 */
									docAttachmentsList = dataExportUtils.readInlinePDFs(xmlFilteredData, newItemDetails);
									if(null!=docAttachmentsList && docAttachmentsList.size()>0)
									{
										// add to fileTransactionList
										if(null==fileTransactionList || fileTransactionList.size()<=0)
										{
											fileTransactionList = new ArrayList<DataExportToolAttachmentDetails>();
										}
										fileTransactionList.addAll(docAttachmentsList);

										/*
										 * ITERATE & VERIFY IF ALL PDFS ARE COPIED SUCCESSFULLY
										 */
										// re-initialize Flag for pdfs check
										errorsInPDFsMoving = false;
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if((null==attachmentDetails.getProcessingStatus()) || (null!=attachmentDetails.getProcessingStatus() && !attachmentDetails.getProcessingStatus().equalsIgnoreCase(ScheduleConstants.STATUS_SUCCESS)))
											{
												// update failureCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
												// some pdfs failed while moving
												errorsInPDFsMoving=true;
												//											break;
											}
											else if(null!=attachmentDetails.getProcessingStatus() && attachmentDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
											{
												// update successCount for Files Movement
												transactionDAO.updateOKAssetsCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);
											}
											attachmentDetails = null;
										}

										/*
										 * REPLACE THE PDFS PATH IN XML FILTERED DATA
										 */
										attachmentDetails = null;
										for(int t=0;t<docAttachmentsList.size();t++)
										{
											if(isScheduleAborted()) { break; }
											attachmentDetails = (DataExportToolAttachmentDetails)docAttachmentsList.get(t);
											if(null!=attachmentDetails.getSrcHrefPath() && !"".equals(attachmentDetails.getSrcHrefPath()) 
													&& null!=attachmentDetails.getPathToBeReplaced() && !"".equals(attachmentDetails.getPathToBeReplaced()))
											{
												/*
												 * replace HREF ORIGINAL PATH WITH RMI TOOL PATHS TO BE REPLACED
												 */
												xmlFilteredData = xmlFilteredData.replace(attachmentDetails.getSrcHrefPath(), attachmentDetails.getPathToBeReplaced());
											}
											attachmentDetails = null;
										}
									}
									docAttachmentsList=  null;

									if(errorsInPDFsMoving==true)
									{
										/*
										 * errors found while processing PDFs
										 * DO NOT FAIL DOCUMENT IN THIS SCENARIO - ONLY ITEM WILL BE FAILED
										 * DETAILS CAN BE TRACKED FROM SUMMARY.
										 */
										//									newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										//									if(null!=newItemDetails.getErrorMessage() && !"".equals(newItemDetails.getErrorMessage()))
										//									{
										//										newItemDetails.setErrorMessage(newItemDetails.getErrorMessage()+"\n"+"FAILED TO MOVE SOME INLINE PDFS FOR THE DOCUMENT. PLEASE REFER TO FILE TRANSACTIONS REPORT.");
										//									}
										//									else
										//									{
										//										newItemDetails.setErrorMessage("FAILED TO MOVE SOME INLINE PDFS FOR THE DOCUMENT. PLEASE REFER TO FILE TRANSACTIONS REPORT.");
										//									}
										/*
										 * set anyErrorFound flag to ture, as some error generated for the processing of this item
										 * the item will be in Failure State
										 */
										anyErrorFound = true;
									}

									/*
									 * WRITE XML CONTENT TO THIS DIRECTORY WITH XML NAME AS DOCUMENTID.XML
									 */
									if(!actualWriteFolderPath.endsWith("/"))
									{
										actualWriteFolderPath=actualWriteFolderPath+"/";
									}
									actualWriteFolderPath=actualWriteFolderPath+documentId+".xml";

									// set the updatedDocumentDir Path in newItemDetails as attachmentsOperation are done now
									newItemDetails.setDocumentDirPath(actualWriteFolderPath);
									/*
									 * PERFORM INNERLINKS OPERATION FOR THE FILTERED DOCUMENT XML DATA
									 * IDENTIFY INNER LINKS AND ADD THEM TO NEW ITEM DETAILS
									 * AS PROCESSING OF INNERLINKS WILL BE CARRIED OUT AFTER THE COMPLETE EXTRACT
									 */
									docInnerLinksList = dataExportUtils.readInnerLinks(xmlFilteredData, newItemDetails, actualWriteFolderPath);
									if(null!=docInnerLinksList && docInnerLinksList.size()>0)
									{
										/*
										 * iterate docsInnerListSize and add only those count to totalInnerLinksCount where considerStatusForProcessing is YES
										 */
										inrLkDetails = null;
										for(int we=0;we<docInnerLinksList.size();we++)
										{
											if(isScheduleAborted()) { break; }
											inrLkDetails = (DataExportToolInnerLinkDetails)docInnerLinksList.get(we);
											if(null!=inrLkDetails.getConsiderInnerLinkForProcessing() && inrLkDetails.getConsiderInnerLinkForProcessing().equals(ScheduleConstants.STATUS_YES) && 
													null!=inrLkDetails.getInnerLinkDocumentId() && !"".equals(inrLkDetails.getInnerLinkDocumentId()))
											{
												// increment totalInnerLinksCount by 1
												totalInnerLinksCount=totalInnerLinksCount+1;
											}
											inrLkDetails = null;
										}
										inrLkDetails = null;

										/*
										 * ADD DOC INNER LINKS LIST TO TOTAL INNER LINKS LIST
										 * STRUCTURE FOR TOTAL INNER LINKS LIST WILL BE
										 * 	ITEMS (UNIQUE ITEM ID ONLY)
										 * 		UNIQUE DOCUMENT ID WITH XML PATH IN RMI TOOL WORKING DIR
										 * 			INNERLINKS LIST
										 */
										boolean itemAdded = false;
										innerLinkItemDetails = null;
										innerLinkItemDocumentDetails = null;
										if(null!=innerLinksList && innerLinksList.size()>0)
										{
											innerLinkItemDetails = null;
											for(int ot=0;ot<innerLinksList.size();ot++)
											{
												if(isScheduleAborted()) { break; }
												innerLinkItemDetails = (DataExportToolItemDetails) innerLinksList.get(ot);
												if(innerLinkItemDetails.getItemId()==newItemDetails.getItemId())
												{
													// ITEM ALREADY ADDED - DO NOT ADD
													itemAdded=true;
													boolean documentAdded=false;
													if(null!=innerLinkItemDetails.getInnerLinkDocumetsList() && innerLinkItemDetails.getInnerLinkDocumetsList().size()>0)
													{
														innerLinkItemDocumentDetails = null;
														for(int er=0;er<innerLinkItemDetails.getInnerLinkDocumetsList().size();er++)
														{
															if(isScheduleAborted()) { break; }
															innerLinkItemDocumentDetails = (DataExportToolItemDetails)innerLinkItemDetails.getInnerLinkDocumetsList().get(er);
															if(innerLinkItemDocumentDetails.getDocumentId().equals(newItemDetails.getDocumentId()))
															{
																// DOCUMENT ALREADY ADDED - DO NOT ADD
																documentAdded = true;
																// ADD ALL INNER LINKS TO THIS DOCUMENT
																if(null==innerLinkItemDocumentDetails.getInnerLinksList() || innerLinkItemDocumentDetails.getInnerLinksList().size()<=0)
																{
																	innerLinkItemDocumentDetails.setInnerLinksList(new ArrayList<DataExportToolInnerLinkDetails>());
																}
																innerLinkItemDocumentDetails.getInnerLinksList().addAll(docInnerLinksList);
																break;
															}
														}
													}

													if(documentAdded==false)
													{
														// add this document to itemDetails
														innerLinkItemDocumentDetails = new DataExportToolItemDetails();
														innerLinkItemDocumentDetails.setDocumentId(newItemDetails.getDocumentId());
														innerLinkItemDocumentDetails.setDocumentDirPath(actualWriteFolderPath);
														// ADD ALL INNER LINKS TO THIS DOCUMENT
														if(null==innerLinkItemDocumentDetails.getInnerLinksList() || innerLinkItemDocumentDetails.getInnerLinksList().size()<=0)
														{
															innerLinkItemDocumentDetails.setInnerLinksList(new ArrayList<DataExportToolInnerLinkDetails>());
														}
														innerLinkItemDocumentDetails.getInnerLinksList().addAll(docInnerLinksList);

														// add to DocumentList of innerLinkItemDetails
														if(null==innerLinkItemDetails.getInnerLinkDocumetsList() || innerLinkItemDetails.getInnerLinkDocumetsList().size()<=0)
														{
															innerLinkItemDetails.setInnerLinkDocumetsList(new ArrayList<DataExportToolItemDetails>());
														}
														innerLinkItemDetails.getInnerLinkDocumetsList().add(innerLinkItemDocumentDetails);
														innerLinkItemDocumentDetails = null;

													}
													break;
												}
												innerLinkItemDetails = null;
											}
										}

										if(itemAdded==false)
										{
											// add this item to innerLinksList
											innerLinkItemDetails = new DataExportToolItemDetails();
											innerLinkItemDetails.setItemId(newItemDetails.getItemId());
											innerLinkItemDetails.setScheduleId(newItemDetails.getScheduleId());
											innerLinkItemDetails.setLocale(newItemDetails.getLocale());
											innerLinkItemDetails.setModel(newItemDetails.getModel());
//											innerLinkItemDetails.setCarlineCode(newItemDetails.getCarlineCode());
											innerLinkItemDetails.setChannelName(newItemDetails.getChannelName());
											innerLinkItemDetails.setDocumentType(newItemDetails.getDocumentType());

											// add document to innerLinkItemDetails
											innerLinkItemDocumentDetails = new DataExportToolItemDetails();
											innerLinkItemDocumentDetails.setDocumentId(newItemDetails.getDocumentId());
											innerLinkItemDocumentDetails.setDocumentDirPath(actualWriteFolderPath);

											if(null==innerLinkItemDocumentDetails.getInnerLinksList() || innerLinkItemDocumentDetails.getInnerLinksList().size()<=0)
											{
												innerLinkItemDocumentDetails.setInnerLinksList(new ArrayList<DataExportToolInnerLinkDetails>());
											}
											// add INNERLINKS
											innerLinkItemDocumentDetails.getInnerLinksList().addAll(docInnerLinksList);

											// add Document
											if(null==innerLinkItemDetails.getInnerLinkDocumetsList() || innerLinkItemDetails.getInnerLinkDocumetsList().size()<=0)
											{
												innerLinkItemDetails.setInnerLinkDocumetsList(new ArrayList<DataExportToolItemDetails>());
											}
											innerLinkItemDetails.getInnerLinkDocumetsList().add(innerLinkItemDocumentDetails);


											// add innerLinkItemDetails to 
											if(null==innerLinksList || innerLinksList.size()<=0)
											{
												innerLinksList = new ArrayList<DataExportToolItemDetails>();
											}
											innerLinksList.add(innerLinkItemDetails);
											innerLinkItemDetails = null;
											innerLinkItemDocumentDetails = null;
										}
									}
									docInnerLinksList  =null;
									innerLinkItemDetails = null;
									innerLinkItemDocumentDetails = null;
									
									/*
									 * PROCEED FOR CHECKING OTHER MANUAL LINKS IN XML FOR EACH RICH TEXT CONTENT
									 * REPLACE OTHER MANUAL LINKS AS DEAD LINKS IN XML CONTENT
									 */
									otherManualLinksList = dataExportUtils.readOtherManualInnerLinks(xmlFilteredData, newItemDetails);
									if(null!=otherManualLinksList && otherManualLinksList.size()>0)
									{
										inrLkDetails = null;
										for(int rut=0;rut<otherManualLinksList.size();rut++)
										{
											if(isScheduleAborted()) { break; }
											inrLkDetails = (DataExportToolInnerLinkDetails)otherManualLinksList.get(rut);
											if(null!=inrLkDetails.getSrcHrefPath() && !"".equals(inrLkDetails.getSrcHrefPath()) 
												 && null!=inrLkDetails.getPathToBeReplaced() && !"".equals(inrLkDetails.getPathToBeReplaced()))
											{
//												logger.info("------------- src path :: >"+ inrLkDetails.getSrcHrefPath());
												// replace & symbol with its html code before replacing in XML File
//												inrLkDetails.setSrcHrefPath(inrLkDetails.getSrcHrefPath().replace("&", "&amp;"));
//												logger.info("------------- src path after :: >"+ inrLkDetails.getSrcHrefPath());
												xmlFilteredData = xmlFilteredData.replace(inrLkDetails.getSrcHrefPath(), inrLkDetails.getPathToBeReplaced());
											}
											inrLkDetails = null;
										}
									}
									otherManualLinksList = null;
									
									// WRITE FILE
									writeFlag = FileReadWriteUtil.writeFile(xmlFilteredData.getBytes(), actualWriteFolderPath);
									if(writeFlag==true)
									{
										/*
										 * REVIRFY IF THE XML FILE EXISTS OR NOT
										 */
										try
										{
											revrifyXMLFile = new File(actualWriteFolderPath);
											if(revrifyXMLFile.exists() && revrifyXMLFile.isFile())
											{
												// set reVerifyFlag to true
												reverifyFlag = true;
											}
											revrifyXMLFile = null;
										}
										catch(Exception e)
										{
											Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startItemsProcessing()", e);
											// set reVerifyFlag to false
											reverifyFlag = false;
										}

										if(reverifyFlag==true)
										{
											// FILE WRITTEN SUCCESSFULLY

											/*
											 * CHECK IF ANY ERRORS WHILE ATTACHMENTS / IMAGES / PDFs MOVING
											 * DO NOT FAIL DOCUMENT IN THIS CASE - ALSO THE COUNT OF DOCUMENT PROCESSING WILL BE SUCCESS
											 * ONLY ITEM WILL GET FAILED AS SOME INLINE IMAGES / PDF / ATTACHMENTS MOVEMENT FAILED.
											 */
											//										if(errorsInAttachmentsMoving==true || errorsInImagesMoving==true || errorsInPDFsMoving==true)
											//										{
											//											/*
											//											 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
											//											 */
											//											transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
											//
											//											/*
											//											 * set ItemProcessingStatus as Failure
											//											 */
											//											newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
											//											/*
											//											 * set anyErrorFound flag to ture, as some error generated for the processing of this item
											//											 * the item will be in Failure State
											//											 */
											//											anyErrorFound = true;
											//										}
											//										else
											//										{
											//											/*
											//											 * UPDATE SUCCESS COUNT FOR THE PROCESSING ITEM
											//											 */
											//											transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);
											//
											//											/*
											//											 * set ItemProcessingStatus as Failure
											//											 */
											//											newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
											//											newItemDetails.setErrorMessage(null);
											//										}

											/*
											 * UPDATE SUCCESS COUNT FOR THE PROCESSING ITEM
											 */
											transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);

											/*
											 * set ItemProcessingStatus as Failure
											 */
											newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
											newItemDetails.setErrorMessage(null);
										}
										else
										{
											/*
											 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
											 */
											transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);

											/*
											 * set ItemProcessingStatus as Failure
											 */
											newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
											if(null!=newItemDetails.getErrorMessage() && !"".equals(newItemDetails.getErrorMessage()))
											{
												newItemDetails.setErrorMessage(newItemDetails.getErrorMessage()+"\n"+"FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER. PATH IS :: >"+ newItemDetails.getDocumentDirPath()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
											}
											else
											{
												newItemDetails.setErrorMessage("FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER. PATH IS :: >"+ newItemDetails.getDocumentDirPath()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
											}
											/*
											 * set anyErrorFound flag to ture, as some error generated for the processing of this item
											 * the item will be in Failure State
											 */
											anyErrorFound = true;

										}
										reverifyFlag=false;
									}
									else
									{
										/*
										 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
										 */
										transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);

										/*
										 * set ItemProcessingStatus as Failure
										 */
										newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										if(null!=newItemDetails.getErrorMessage() && !"".equals(newItemDetails.getErrorMessage()))
										{
											newItemDetails.setErrorMessage(newItemDetails.getErrorMessage()+"\n"+"FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER. PATH IS :: >"+ newItemDetails.getDocumentDirPath()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
										}
										else
										{
											newItemDetails.setErrorMessage("FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER. PATH IS :: >"+ newItemDetails.getDocumentDirPath()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
										}
										/*
										 * set anyErrorFound flag to ture, as some error generated for the processing of this item
										 * the item will be in Failure State
										 */
										anyErrorFound = true;
									}
									writeFlag = false;
								}
								else
								{
									/*
									 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
									 */
									transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
									/*
									 * set ItemProcessingStatus as Failure
									 */
									newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
									newItemDetails.setErrorMessage("FAILED TO CREATE REQUIRED DIRECTORY STRUCTURE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER. PATH IS :: >"+ (rmiExportToolWorkingDirectoryPath+tempWriteFolderPath)+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
									/*
									 * set anyErrorFound flag to ture, as some error generated for the processing of this item
									 * the item will be in Failure State
									 */
									anyErrorFound = true;
								}
								actualWriteFolderPath = null;
								tempWriteFolderPath = null;
							}
							else
							{
								/*
								 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
								 */
								transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
								/*
								 * set ItemProcessingStatus as Failure
								 */
								newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
								newItemDetails.setErrorMessage("FAILED TO REMOVE UNWANTED XML NODES DURING DATA EXTRACTION. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
								/*
								 * set anyErrorFound flag to ture, as some error generated for the processing of this item
								 * the item will be in Failure State
								 */
								anyErrorFound = true;
							}
						}
						else
						{
							/*
							 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
							 */
							transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
							/*
							 * set ItemProcessingStatus as Failure
							 */
							newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							newItemDetails.setErrorMessage("FAILED TO FIND XML DOCUMENT FILE INSIDE MGSS SERVER.");
							/*
							 * set anyErrorFound flag to ture, as some error generated for the processing of this item
							 * the item will be in Failure State
							 */
							anyErrorFound = true;
						}
						channelFolderPath = null;
						documentXMLFile = null;
						xmlFilteredData = null;
					}
					else
					{
						/*
						 * UPDATE FAILURE COUNT FOR THE PROCESSING ITEM
						 */
						transactionDAO.updateProcessingCount(String.valueOf(newItemDetails.getScheduleId()), String.valueOf(newItemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
						/*
						 * set ItemProcessingStatus as Failure
						 */
						newItemDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
						newItemDetails.setErrorMessage("FAILED TO FETCH DISPLAY ORDER / VIEW CONTENT DETAILS FOR DOCUMENT.");
						/*
						 * set anyErrorFound flag to ture, as some error generated for the processing of this item
						 * the item will be in Failure State
						 */
						anyErrorFound = true;
					}
					viewContentListForDoucment = null;
					/*
					 * add ItemDetails with each Document Update to transactionItemsList
					 */
					if(null==transactionList || transactionList.size()<=0)
					{
						transactionList = new ArrayList<DataExportToolItemDetails>();
					}
					transactionList.add(newItemDetails);
					newItemDetails = null;
					logger.info("############## End Processing >> "+documentId+" {"+count+" / "+ itemDetails.getDocumentIdsList().size()+"}.");
					documentId = null;
				}
				liveFolderPath = null;
				rmiExportToolWorkingDirectoryPath = null;


				/*
				 * IF CHANNEL NAME = WIRING DIAGRAMS
				 * MOVE OKASSETS FOLDER FOR THE WIRING DIAGRAM IN THE EXPORT DIR
				 */
				// set wdOkAssetsListForItems to null - so that it always contains Items for the Processing WD ITEM Only
				wdOkAssetsListForItems = null;
				// start WD OKAssets Processing
				if(null!=itemDetails.getChannelName() && itemDetails.getChannelName().trim().toLowerCase().equals("WIRING_DIAGRAMS".trim().toLowerCase()))
				{
					/*
					 * NO NEED OF SETTING DATA TYPE HERE
					 * AS NO DOCUMENT ID WILL BE ADDED FOR THIS OKASSETS FOLDER IN THE REPORTS
					 */

					if(null!=wiringDiagramDocumentIdForOkAssetsMovement && !"".equals(wiringDiagramDocumentIdForOkAssetsMovement))
					{
						/*
						 * identify Model FolderName for the WD Document from DMT LOCALE BASED IMDOC TRANSACTION TABLE
						 * this will be used for preparing sourcePath for OkAssets Movement
						 */
						String localeDir = itemDetails.getLocale();
						localeDir= localeDir.replace("-", "_");
						String modelFolderName = transactionDAO.getModelFolderName(localeDir, wiringDiagramDocumentIdForOkAssetsMovement);
						if(null!=modelFolderName && !"".equals(modelFolderName))
						{
							logger.info("startItemsProcessing :: Model Folder Name for Document Id {"+wiringDiagramDocumentIdForOkAssetsMovement+"} IS :: >"+ modelFolderName);
							/*
							 *  PREPARE SOURCE PATH FOR THE MODEL FOLDER IN OKASSETS
							 *  //m4okfs10/Okassets/library/MAZDA/<CHANNEL_NAME>/<LOCALE_DIR IN LOWECASE>/<MODEL_FOLDER_NAME IN LOWERCASE>
							 */
							String sourceOkAssetsDirPath=ApplicationProperties.getProperty("dataexttool.okassets.source.dir.path");
							if(!sourceOkAssetsDirPath.endsWith("/"))
							{
								sourceOkAssetsDirPath+="/";
							}
							// add library Folder
							sourceOkAssetsDirPath+=ApplicationProperties.getProperty("dataexttool.library.dir.name")+"/";
							// add MAZDA Folder
							sourceOkAssetsDirPath+=ApplicationProperties.getProperty("dataexttool.mazda.dir.name")+"/";
							// add channelFolder
							sourceOkAssetsDirPath+=itemDetails.getChannelName().trim().toUpperCase()+"/";
							// add localeFolder
							sourceOkAssetsDirPath+=localeDir.trim().toLowerCase()+"/";
							// add modelFolder
							sourceOkAssetsDirPath+=modelFolderName.trim().toLowerCase();
							File sourceDir = new File(sourceOkAssetsDirPath);
							if(sourceDir.exists() && sourceDir.isDirectory())
							{
//								logger.info("startItemsProcessing :: Start XCopying OKAssets Directory for :: >"+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+itemDetails.getCarlineCode()+" >> "+ itemDetails.getChannelName());
								logger.info("startItemsProcessing :: Start XCopying OKAssets Directory for :: >"+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getChannelName());
								/*
								 * PREPARE DESTINATION PATH
								 */
								// destinationPath - RMI TOOL WORKING DIR/ScheduleId/OKAssets/library/MAZDA/CHANNEL_NAME/locale/modelFolderName/
								String destPath=null;
								// add scheduleId
								String tempDestinationDirPath=String.valueOf(itemDetails.getScheduleId());
								// add OkassestDestDirSubPath
								String okAssetsDirPath = ApplicationProperties.getProperty("dataexttool.okassets.dir.sub.path");
								if(!okAssetsDirPath.startsWith("/"))
								{
									okAssetsDirPath="/"+okAssetsDirPath;
								}
								tempDestinationDirPath = tempDestinationDirPath+okAssetsDirPath;
								if(!tempDestinationDirPath.endsWith("/"))
								{
									tempDestinationDirPath +="/";
								}
								// add Channel
								tempDestinationDirPath+=itemDetails.getChannelName()+"/";
								// add locale
								tempDestinationDirPath+=localeDir+"/";
								// add modelFolderName
								tempDestinationDirPath+=modelFolderName+"/";

								destPath = dataExportUtils.createFolderStructure(tempDestinationDirPath, ApplicationProperties.getProperty("dataexttool.working.dir.folder.path"));
								if(null!=destPath && !"".equals(destPath))
								{
									logger.info("startItemsProcessing :: Start Copying OKAssets Directory at Destination Path :: >"+ destPath);
									/*
									 * START COPYING DIRECTORY
									 */
									File destDir = new File(destPath);
									copyDirectory(sourceDir, destDir, itemDetails);
									destDir = null;
								}
								else
								{
									logger.info("startItemsProcessing :: Failed to Create OKAssets DIR for "+itemDetails.getChannelName()+" at PATH :: >" + ApplicationProperties.getProperty("dataexttool.working.dir.folder.path")+tempDestinationDirPath);
								}
								destPath = null;
								tempDestinationDirPath = null;
								okAssetsDirPath = null;
							}
							else
							{
//								logger.info("startItemsProcessing :: No OKAssets Directory Exists for :: >"+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+itemDetails.getCarlineCode()+" >> "+ itemDetails.getChannelName());
								logger.info("startItemsProcessing :: No OKAssets Directory Exists for :: >"+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getChannelName());
							}
							sourceDir = null;
							sourceOkAssetsDirPath = null;
						}
						else
						{
							logger.info("startItemsProcessing :: Failed to Identify Model Folder Name for Document Id :: >"+ wiringDiagramDocumentIdForOkAssetsMovement);
						}
						localeDir = null;
						modelFolderName = null;

						/*
						 * CHECK IF wdOkAssetsListForItems IS NOT NULL - E.G. FILES FOUND IN OKASSETS FOLDER FOR CURRENT PROCESSING WD ITEM
						 * CHECK IF ALL ARE SUCCESS OR ANY FAILURE
						 */
						if(null!=wdOkAssetsListForItems && wdOkAssetsListForItems.size()>0)
						{
							DataExportToolAttachmentDetails attDetails = null;
							for(int ew=0;ew<wdOkAssetsListForItems.size();ew++)
							{
								if(isScheduleAborted()) { break; }
								attDetails=  (DataExportToolAttachmentDetails)wdOkAssetsListForItems.get(ew);
								// errorFound for this item
								if((null==attDetails.getProcessingStatus()) || (null!=attDetails.getProcessingStatus() && !attDetails.getProcessingStatus().equalsIgnoreCase(ScheduleConstants.STATUS_SUCCESS)))
								{
									// update failureCount for Files Movement
									transactionDAO.updateOKAssetsCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), ScheduleConstants.STATUS_FAILURE);
									/*
									 * set anyErrorFound flag to ture, as some error generated for the processing of this item
									 * the item will be in Failure State
									 */
									anyErrorFound = true;
								}
								else if(null!=attDetails.getProcessingStatus() && attDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
								{
									// update successCount for Files Movement
									transactionDAO.updateOKAssetsCount(String.valueOf(itemDetails.getScheduleId()), String.valueOf(itemDetails.getItemId()), ScheduleConstants.STATUS_SUCCESS);
								}
							}

							/*
							 * add wdOkAssetsListForItems to fileTransactionList (for Reports)
							 */
							if(null==fileTransactionList || fileTransactionList.size()<=0)
							{
								fileTransactionList = new ArrayList<DataExportToolAttachmentDetails>();
							}
							fileTransactionList.addAll(wdOkAssetsListForItems);
						}
					}
					else
					{
						logger.info("startItemsProcessing :: No WIRING DIAGRAM DOCUMENT IDENTIFIED For OKAssets Movement for the Processing WIRING DIAGRAM ITEM.");
					}
				}
				wdOkAssetsListForItems = null;

				/*
				 * UPDATE ITEM PROCESSING STATUS AS COMPLETED AND BASED ON ANY ERROR FOUND FLAG & ANY INNER LINK FOUND FLAG, UPDATE COMPLETION STATUS AS SUCCESS / FAILURE
				 */
				if(anyErrorFound==true)
				{
					// errors Found
					transactionDAO.updateItemProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_FAILURE,  itemDetails.getItemId());
					/*
					 * set anyErrorReceivedDuringCompleteSchedule for schedule as true
					 * as error found for an item processing
					 */
					anyErrorReceivedDuringCompleteSchedule = true;
				}
				else
				{
					// no errors Found for this item
					transactionDAO.updateItemProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_SUCCESS,  itemDetails.getItemId());
				}

				/*
				 * if any INNERLINKS FOUND - UPDATE TOTAL INNERLINKS COUNT FOR ITEM & SCHEDULE
				 */
				if(totalInnerLinksCount>0)
				{
					transactionDAO.updateTotalInnerLinksCounts(itemDetails.getItemId(), itemDetails.getScheduleId(), totalInnerLinksCount);
				}
				totalInnerLinksCount = 0;
			}
			else
			{
//				logger.info("startItemsProcessing :: Documents List is null for "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getCarlineCode()+" >>" + itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());
				logger.info("startItemsProcessing :: Documents List is null for "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());
				/*
				 * UPDATE ITEM PROCESSING STATUS TO COMPLETED AND COMPLETION STATUS TO SUCCESS
				 */
				transactionDAO.updateItemProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_SUCCESS,  itemDetails.getItemId());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startItemsProcessing()", e);
			/*
			 * UPDATE ITEM PROCESSING STATUS TO COMPLETED AND COMPLETION STATUS TO FAILURE
			 */
			transactionDAO.updateItemProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_FAILURE,  itemDetails.getItemId());
		}
	}


	private ArrayList<DataExportToolAttachmentDetails> attachmentsOperation(String xmlFileData, String sourceDirPath, DataExportToolItemDetails itemDetails)
	{
		ArrayList<DataExportToolAttachmentDetails> attachmentsList = null;
		try
		{
			if(itemDetails.getChannelName().equals("SERVICE_INFORMATION"))
			{
				attachmentsList = dataExportUtils.readServiceInformationAttachmentsList(xmlFileData, sourceDirPath, itemDetails);
			}
			else if(itemDetails.getChannelName().equals("SERVICE_MANUALS") || itemDetails.getChannelName().equals("OTHER_SERVICE_MANUALS") || itemDetails.getChannelName().equals("TRAINING"))
			{
				attachmentsList = dataExportUtils.readServiceManualsAttachmentsList(xmlFileData, sourceDirPath, itemDetails);
			}
			else if(itemDetails.getChannelName().equals("VIDEOS"))
			{
				attachmentsList = dataExportUtils.readVideosAttachmentsList(xmlFileData, sourceDirPath, itemDetails);
			}
			else if(itemDetails.getChannelName().equals("WIRING_DIAGRAMS"))
			{
				attachmentsList = dataExportUtils.readWiringDiagramsAttachmentsList(xmlFileData, sourceDirPath, itemDetails);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "attachmentsOperation()", e);
		}
		return attachmentsList;
	}


	private void startInnerLinkProcessing(String rmiToolWorkingDirPath)
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			DataExportToolItemDetails itemDetails = null;
			DataExportToolItemDetails documentDetails = null;
			DataExportToolInnerLinkDetails innerLinkDetails = null;
			String documentXMLContent=null;
			String documentToSearch=null;
			byte[] xmlData = null;
			boolean anyErrorFoundForItem=false;
			boolean writeFlag=false;
			boolean reverifyFlag = false;
			File revrifyXMLFile = null;
			for(int a=0;a<innerLinksList.size();a++)
			{
				if(isScheduleAborted()) { break; }
				itemDetails = (DataExportToolItemDetails)innerLinksList.get(a);
//				logger.info("startInnerLinkProcessing :: ------------------> "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> "+ itemDetails.getCarlineCode()+" >>" + itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());
				logger.info("startInnerLinkProcessing :: ------------------> "+ itemDetails.getLocale()+" >> "+ itemDetails.getModel()+ " >> " + itemDetails.getChannelName()+" >>" + itemDetails.getDocumentType());

				anyErrorFoundForItem = false;
				/*
				 * UPDATE ITEM PROCESSING STATUS TO PROCESSING AND COMPLETION STATUS TO NULL
				 */
				transactionDAO.updateItemInnerLinkProcessingStatus(ScheduleConstants.STATUS_PROCESSING, null,  itemDetails.getItemId());

				if(null!=itemDetails.getInnerLinkDocumetsList() && itemDetails.getInnerLinkDocumetsList().size()>0)
				{
					documentDetails = null;
					int count=0;
					for(int b=0;b<itemDetails.getInnerLinkDocumetsList().size();b++)
					{
						if(isScheduleAborted()) { break; }
						documentDetails = (DataExportToolItemDetails)itemDetails.getInnerLinkDocumetsList().get(b);
						count++;
						logger.info("############## Start InnerLink Processing for >> "+documentDetails.getDocumentId()+" {"+count+" / "+ itemDetails.getInnerLinkDocumetsList().size()+"}.");

						if(null!=documentDetails.getInnerLinksList() && documentDetails.getInnerLinksList().size()>0)
						{
							/*
							 * READ DOCUMENT XML FILE
							 */
							xmlData=  FileReadWriteUtil.readFile(documentDetails.getDocumentDirPath());
							if(null!=xmlData && xmlData.length>0)
							{
								documentXMLContent = new String(xmlData, "UTF-8");
							}

							if(null!=documentXMLContent && !"".equals(documentXMLContent))
							{
								/*
								 * start Iterating InnerLinks list
								 * for each link - find the InnerLinkDocumentId.xml 
								 * 		if found -
								 * 			identify the relative path - /locale/model carlineCode/channel/documentType/documentId/documentId.xml
								 * 			replace it with srcPath in the document XML Content
								 * 			update Processing Status as Success for InnerLink
								 * 			update Success Count
								 * 		if not found - 
								 * 			update Processing Status as Failure for InnerLink
								 * 			update Failure Count
								 */
								innerLinkDetails = null;
								for(int r=0;r<documentDetails.getInnerLinksList().size();r++)
								{
									if(isScheduleAborted()) { break; }
									innerLinkDetails = (DataExportToolInnerLinkDetails)documentDetails.getInnerLinksList().get(r);
									if(null!=innerLinkDetails.getConsiderInnerLinkForProcessing() && innerLinkDetails.getConsiderInnerLinkForProcessing().equals(ScheduleConstants.STATUS_YES) 
											&& null!=innerLinkDetails.getInnerLinkDocumentId() && !"".equals(innerLinkDetails.getInnerLinkDocumentId()))
									{
										//										documentToSearch = innerLinkDetails.getInnerLinkDocumentId()+".xml";
										documentToSearch = innerLinkDetails.getInnerLinkDocumentId();
										// search INNERLINK DOCUMENT & FIND ITS RELATIVE PATH
										/*
										 * INSTEAD OF SEARCHING DOCUMENT IN RMI EXPORT DIRECTORY
										 * 	SEARCH IN TRANSACTION LIST (AS SEARCH IN DIRECTORY IS TAKING LOT OF TIME)
										 * 		LOOK FOR THE INNERLINK DOCUMENT ID AND LOCALE IN TRANSACTION LIST
										 * 			IF FOUND - CHECK PROCESSING STATUS - 
										 * 				IF SUCCESS - INNERLINK WILL BE SUCCESS
										 * 				IF FAILURE - 
										 * 					CHECK ERROR MESSAGE CONTAINS -  EITHER OF THE BELOW
										 * 						FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER
																FAILED TO CREATE REQUIRED DIRECTORY STRUCTURE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER
																FAILED TO REMOVE UNWANTED NODES FROM XML DOCUMENT FILE AND TRANSFORM TO STRING
																FAILED TO READ / IDENTIFY XML DOCUMENT FILE INSIDE CHANNEL FOLDER PATH
															FAIL INNERLINK - AS DOCUMENT.XML NOT WRITTEN IN EXPORT DIR
															ELSE
																SUCCESS INNERLINK - DOCUMENT.XML WRITTEN (BUT ISSUES WITH SOME INLINE IMAGES / PDFS / ATTACHMENTS)
										 */

										//										innerLinkDetails.setPathToBeReplaced(dataExportUtils.searchDocumentXMLinDirectory(rmiToolWorkingDirPath, itemDetails.getLocale(), documentToSearch));
										innerLinkDetails = dataExportUtils.searchDocumentXMLinTransactionList(itemDetails.getLocale(), documentToSearch, transactionList,innerLinkDetails);

										if(null!=innerLinkDetails.getPathToBeReplaced() && !"".equals(innerLinkDetails.getPathToBeReplaced()))
										{
											// set INNERLINK PROCESSING STATUS AS SUCCESS
											innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
											// replace innerLink Path in documentXMLContent
											documentXMLContent = documentXMLContent.replace(innerLinkDetails.getSrcHrefPath(), innerLinkDetails.getPathToBeReplaced());
										}
										else
										{
											// MAKE SUCH LINKS AS DEAD LINKS
											innerLinkDetails.setPathToBeReplaced("#");
											// replace innerLink Path in documentXMLContent
											documentXMLContent = documentXMLContent.replace(innerLinkDetails.getSrcHrefPath(), innerLinkDetails.getPathToBeReplaced());
											// FAILED TO FIND INNER LINK DOCUMENT - UPDATE PROCESSING STATUS FOR LINK AS FAILURE & UPDATE FAILURE INNERLINK COUNT
											innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
											innerLinkDetails.setErrorMessage("FAILED TO FIND INNERLINK DOCUMENT id XML IN THE EXPORTED DIRECTORY. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
										}
										documentToSearch = null;
									}
									innerLinkDetails = null;
								}

								/*
								 * WRITE DOCUMENT XML CONTENT TO THE ACTUAL PATH
								 */
								writeFlag = FileReadWriteUtil.writeFile(documentXMLContent.getBytes(), documentDetails.getDocumentDirPath());
								if(writeFlag==true)
								{
									/*
									 * REVIERY IF XML IS WRITTEN PROPERLY IN THE LOCATION
									 */
									try
									{
										revrifyXMLFile = new File(documentDetails.getDocumentDirPath());
										if(revrifyXMLFile.exists() && revrifyXMLFile.isFile())
										{
											// set reVerifyFlag to true
											reverifyFlag = true;
										}
										revrifyXMLFile = null;
									}
									catch(Exception e)
									{
										Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startInnerLinkProcessing()", e);
										// set reVerifyFlag to false
										reverifyFlag = false;
									}
								}

								// UPDATE PROCESSING STATUS & COUNTS
								if(writeFlag==false || reverifyFlag==false)
								{
									// RE WRITE OPERATION FAILED
									innerLinkDetails = null;
									for(int r=0;r<documentDetails.getInnerLinksList().size();r++)
									{
										if(isScheduleAborted()) { break; }
										innerLinkDetails = (DataExportToolInnerLinkDetails)documentDetails.getInnerLinksList().get(r);
										// set INNER LINK PROCESSING STATUS AS FAILURE - ONLY FOR THOSE WHERE CONSIDER FOR PROCESSING = YES
										if(null!=innerLinkDetails.getConsiderInnerLinkForProcessing() && innerLinkDetails.getConsiderInnerLinkForProcessing().equals(ScheduleConstants.STATUS_YES) && 
												null!=innerLinkDetails.getInnerLinkDocumentId() && !"".equals(innerLinkDetails.getInnerLinkDocumentId()))
										{
											innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
											// SET ERROR MESSAGE - FAILED TO REWRITE SOURCE DOCUMENT XML IN EXPORTED DIRECTORY.
											if(null!=innerLinkDetails.getErrorMessage() && !"".equals(innerLinkDetails.getErrorMessage()))
											{
												innerLinkDetails.setErrorMessage(innerLinkDetails.getErrorMessage()+"\n"+" FAILED TO REWRITE SOURCE DOCUMENT XML IN EXPORTED DIRECTORY. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
											}
											else
											{
												innerLinkDetails.setErrorMessage("FAILED TO REWRITE SOURCE DOCUMENT XML IN EXPORTED DIRECTORY. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
											}

											/*
											 * set anyErrorFoundForItem to true since an errorFound
											 * so that CompletionStatus for the Item can be updated as Failure 
											 */
											anyErrorFoundForItem = true;

											// update FailureInnerlinkCount for Item & Schedule
											transactionDAO.updateInnerLinkProcessingCount(String.valueOf(itemDetails.getScheduleId()),String.valueOf(itemDetails.getItemId()) , ScheduleConstants.STATUS_FAILURE);
										}
										// add to innerLinksTransactionList
										if(null==innerLinksTransactionList || innerLinksTransactionList.size()<=0)
										{
											innerLinksTransactionList = new ArrayList<DataExportToolInnerLinkDetails>();
										}
										innerLinksTransactionList.add(innerLinkDetails);
										innerLinkDetails = null;
									}
									innerLinkDetails = null;
								}
								else if(writeFlag==true && reverifyFlag==true)
								{
									// COMPLETE SUCCESS CONDITION - XML FILE GOT REWRITTEN SUCCESSFULLY.
									innerLinkDetails = null;
									for(int r=0;r<documentDetails.getInnerLinksList().size();r++)
									{
										if(isScheduleAborted()) { break; }
										innerLinkDetails = (DataExportToolInnerLinkDetails)documentDetails.getInnerLinksList().get(r);
										// ONLY FOR LINKS WITH CONSIDER PROCESSING STATUS AS YES
										if(null!=innerLinkDetails.getConsiderInnerLinkForProcessing() && innerLinkDetails.getConsiderInnerLinkForProcessing().equals(ScheduleConstants.STATUS_YES) && 
												null!=innerLinkDetails.getInnerLinkDocumentId() && !"".equals(innerLinkDetails.getInnerLinkDocumentId()))
										{
											if(null!=innerLinkDetails.getProcessingStatus() && ScheduleConstants.STATUS_SUCCESS.equals(innerLinkDetails.getProcessingStatus()))
											{
												// update SuccessInnerlinkCount for Item & Schedule
												transactionDAO.updateInnerLinkProcessingCount(String.valueOf(itemDetails.getScheduleId()),String.valueOf(itemDetails.getItemId()) , ScheduleConstants.STATUS_SUCCESS);
											}
											else
											{
												/*
												 * set anyErrorFoundForItem to true since an errorFound
												 * so that CompletionStatus for the Item can be updated as Failure 
												 */
												anyErrorFoundForItem = true;

												// update FailureInnerlinkCount for Item & Schedule
												transactionDAO.updateInnerLinkProcessingCount(String.valueOf(itemDetails.getScheduleId()),String.valueOf(itemDetails.getItemId()) , ScheduleConstants.STATUS_FAILURE);
											}
										}
										// add to innerLinksTransactionList
										if(null==innerLinksTransactionList || innerLinksTransactionList.size()<=0)
										{
											innerLinksTransactionList = new ArrayList<DataExportToolInnerLinkDetails>();
										}
										innerLinksTransactionList.add(innerLinkDetails);
										innerLinkDetails = null;
									}
								}
								writeFlag =false;
								reverifyFlag = false;
							}
							else
							{
								logger.info("startInnerLinkProcessing :: Failed to read XML File for Document {"+documentDetails.getDocumentId()+"} at Path :: >"+ documentDetails.getDocumentDirPath());
								/*
								 * SET ALL INNERLINKS STATUS TO FAILURE IF ANY
								 * UPDATE INNERLINK FAILURE COUNT FOR ITEM & SCHEDULE
								 */
								innerLinkDetails = null;
								for(int r=0;r<documentDetails.getInnerLinksList().size();r++)
								{
									if(isScheduleAborted()) { break; }
									innerLinkDetails = (DataExportToolInnerLinkDetails)documentDetails.getInnerLinksList().get(r);
									/*
									 * ONLY FOR ITEMS WHERE CONSIDER LINK PROCESSING IS YES
									 */
									if(null!=innerLinkDetails.getConsiderInnerLinkForProcessing() && innerLinkDetails.getConsiderInnerLinkForProcessing().equals(ScheduleConstants.STATUS_YES) && 
											null!=innerLinkDetails.getInnerLinkDocumentId() && !"".equals(innerLinkDetails.getInnerLinkDocumentId()))
									{
										innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										innerLinkDetails.setErrorMessage("FAILED TO READ SOURCE DOCUMENT XML FROM EXPORTED DIRECTORY. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
										/*
										 * UPDATE INNERLINK FAILURE COUNT FOR ITEM & SCHEDULE
										 */
										transactionDAO.updateInnerLinkProcessingCount(String.valueOf(itemDetails.getScheduleId()),String.valueOf(itemDetails.getItemId()) , ScheduleConstants.STATUS_FAILURE);
										/*
										 * set anyErrorFoundForItem to true since an errorFound
										 * so that CompletionStatus for the Item can be updated as Failure 
										 */
										anyErrorFoundForItem = true;
									}

									// add this item to innerLinksTransactionList
									if(null==innerLinksTransactionList || innerLinksTransactionList.size()<=0)
									{
										innerLinksTransactionList = new ArrayList<DataExportToolInnerLinkDetails>();
									}
									innerLinksTransactionList.add(innerLinkDetails);
									innerLinkDetails = null;
								}
							}
							documentXMLContent = null;
							xmlData = null;
						}
						logger.info("############## End InnerLink Processing for >> "+documentDetails.getDocumentId()+" {"+count+" / "+ itemDetails.getInnerLinkDocumetsList().size()+"}.");
						documentDetails = null;
					}
				}

				/*
				 * check if AnyErrorFoundForItem = true
				 * 	update ProcessingStatus as Completed & Completion Status as Failure
				 * 	else Completion Status as Success
				 */
				if(anyErrorFoundForItem==true)
				{
					// errors Found
					transactionDAO.updateItemInnerLinkProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_FAILURE,  itemDetails.getItemId());
					/*
					 * set anyErrorReceivedDuringCompleteSchedule for schedule as true
					 * as error found for an item processing
					 */
					anyErrorReceivedDuringCompleteSchedule = true;
				}
				else
				{
					// no errors Found for this item
					transactionDAO.updateItemInnerLinkProcessingStatus(ScheduleConstants.STATUS_COMPLETED, ScheduleConstants.STATUS_SUCCESS,  itemDetails.getItemId());
				}
				itemDetails = null;
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "startInnerLinkProcessing()", e);
		}
	}

	private void copyDirectoryImpl(File sourceDir, File destDir,DataExportToolItemDetails itemDetails) 
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			File[] items = sourceDir.listFiles();
			if (items != null && items.length > 0) 
			{
				for (File anItem : items) 
				{
					if(isScheduleAborted()) { break; }
					if (anItem.isDirectory()) 
					{
						// create the directory in the destination
						File newDir = new File(destDir, anItem.getName());
						logger.info("copyDirectoryImpl :: CREATED DIR :: >"+ newDir.getAbsolutePath());
						// make Dir
						newDir.mkdir();

						// copy the directory (recursive call)
						copyDirectory(anItem, newDir, itemDetails);
						newDir = null;
					} 
					else
					{
						// copy the file
						File destFile = new File(destDir, anItem.getName());
						/*
						 * PREPARE OKASSETS FILE DATA
						 */
						DataExportToolAttachmentDetails okAssetsFileDetails = new DataExportToolAttachmentDetails();
						okAssetsFileDetails = copySingleFile(anItem, destFile,itemDetails, okAssetsFileDetails);
						if(null!=okAssetsFileDetails)
						{
							/*
							 * add to wdOkAssetsListForItems
							 */
							if(null==wdOkAssetsListForItems || wdOkAssetsListForItems.size()<0)
							{
								wdOkAssetsListForItems = new ArrayList<DataExportToolAttachmentDetails>();
							}
							wdOkAssetsListForItems.add(okAssetsFileDetails);
						}
						okAssetsFileDetails = null;
						destFile = null;
					}
					anItem = null;
				}
			}
			items = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "copyDirectoryImpl()", e);
		}
	}

	private void copyDirectory(File sourceDir, File destDir, DataExportToolItemDetails itemDetails) 
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			// creates the destination directory if it does not exist
			if (!destDir.exists()) 
			{
				destDir.mkdirs();
			}

			// throws exception if the source does not exist
			if (!sourceDir.exists()) 
			{
				logger.info("copyDirectory :: SourceDIR does not exist.");
			}

			// throws exception if the arguments are not directories
			if (sourceDir.isFile() || destDir.isFile()) 
			{
				logger.info("copyDirectory :: Either sourceDir or destDir is not a directory.");
			}
			/*
			 * callFunction to CopyFile
			 */
			copyDirectoryImpl(sourceDir, destDir, itemDetails);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "copyDirectory()", e);
		}
	}

	@SuppressWarnings("resource")
	private DataExportToolAttachmentDetails copySingleFile(File sourceFile, File destFile, DataExportToolItemDetails itemDetails, DataExportToolAttachmentDetails okAssetsFileDetails)
	{
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;
		try
		{
			if (!destFile.exists()) 
			{
				destFile.createNewFile();
			}
			sourceChannel = new FileInputStream(sourceFile).getChannel();
			destChannel = new FileOutputStream(destFile).getChannel();
			sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
			logger.info("copySingleFile :: COPY FILE :: From :: >"+ sourceFile.getAbsolutePath()+" To :: >"+ destFile.getAbsolutePath() );
			/*
			 * PREPARE FILES DATA 
			 */
			// set ITEM Details
			okAssetsFileDetails.setItemDetails(new DataExportToolItemDetails());
			okAssetsFileDetails.setItemDetails(itemDetails);
			/*
			 *	HERE EXPLICITY SET DOCUMENT ID FOR OKASSETS ITEMDETAILS TO NULL
			 * SO THAT SMAE DOCUMENT DOES NOT GETS PRINT FOR ALL FILES 
			 */
			okAssetsFileDetails.getItemDetails().setDocumentId(null);

			// set File Name
			okAssetsFileDetails.setName(sourceFile.getName());
			// set sourcePath
			okAssetsFileDetails.setSourcePath(sourceFile.getAbsolutePath());
			// set attachmentType
			okAssetsFileDetails.setAttachmentType("OKASSETS_WD_FILES");
			// set destinationPath
			okAssetsFileDetails.setDestinationPath(destFile.getAbsolutePath());

			File tDestFile =new File(destFile.getAbsolutePath());
			if(tDestFile.exists() && tDestFile.isFile() && tDestFile.length() > 0)
			{
				// set ProcessingStatus for the file as Success
				okAssetsFileDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
				okAssetsFileDetails.setErrorMessage(null);
			}
			else
			{
				// set ProcessingStatus for the file as Failure
				okAssetsFileDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				// set ErrorMessage as - FAILED TO MOVE FILE TO 
				okAssetsFileDetails.setErrorMessage("FAILED TO MOVE FILE TO DESTINATION LOCATION. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "copySingleFile()", e);
			// set Processing Status to Failure
			okAssetsFileDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			okAssetsFileDetails.setErrorMessage("UNABLE TO PERFORM XCOPY OPERATION. ERROR :: >"+ e.getMessage()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
		}
		finally 
		{
			try
			{
				if (null!=sourceChannel) 
				{
					sourceChannel.close();
				}
				if (null!=destChannel) 
				{
					destChannel.close();
				}
				sourceChannel = null;
				destChannel = null;
			}
			catch (Exception e) 
			{
				Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "copySingleFile()", e);
			}
		}
		return okAssetsFileDetails;
	}

	private void sendEmailNotification(String scheduleId,String scheduleStatus,String scheduleName,String wslId, String zipFilePathForEmailNotif)
	{
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=scheduleStatus && !"".equals(scheduleStatus) && null!=wslId && !"".equals(wslId))
			{
				String[] toEmailIds = null;
				/*
				 * Identify TO EMAIL IDs
				 * if INTERNAL USERS - GMS3_INTERNAL_TEAM_USERS=gbansal1,vdabkara,dsinghga,skoguru,achoragu
				 * 		THEN SEND EMAIL ON IDS ADDED IN PROPERTIES FILE - dataexttool.to.email.id
				 * ELSE FETCH EMAIL ID ON WSL ID AND SEND EMAIL TO ID
				 */
				if(ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS").trim().toLowerCase().indexOf(wslId.trim().toLowerCase())!=-1)
				{
					// USER IS AN INTERNAL USER
					toEmailIds = ApplicationProperties.getProperty("dataexttool.to.email.id").split(",");
				}
				else
				{
					// FETCH EMAIL ID ON WSL ID
					String emailid = transactionDAO.getEmailId(wslId,null);
					if(null!=emailid && !"".equals(emailid))
					{
						toEmailIds = emailid.split(",");
					}
					else
					{
						logger.info("sendEmailNotification :: Email Id retrieved for "+wslId+" is NULL. Email Notification cannot be sent.");
					}
					emailid = null;
				}

				if(null!=toEmailIds && toEmailIds.length>0)
				{
					String fromEmailId = ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID");
					ArrayList<String> toEmailsList = new ArrayList<String>();
					if(null!=toEmailIds && toEmailIds.length>0)
					{
						for(int a=0;a<toEmailIds.length;a++)
						{
							if(isScheduleAborted()) { break; }
							logger.info("sendEmailNotification :: Send Emails to  :: >"+ toEmailIds[a]);
							toEmailsList.add(toEmailIds[a]);
						}
					}
					String subject=  "MNAO Data Export Summary - "+ scheduleName;
					String body="Hi,<br/>The MNAO Data Export for Job "+scheduleName+" has been finished";
					if(scheduleStatus.equals(ScheduleConstants.STATUS_SUCCESS))
					{
						body+=" Successfully.";
					}
					else 
					{
						body+=" with Errors.";
					}

					String reportsZipPath = ApplicationProperties.getProperty("dataexttool.reports.folder.path");
					if(!reportsZipPath.endsWith("/"))
					{
						reportsZipPath+="/";
					}
					reportsZipPath+=scheduleId+"/"+scheduleName+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");

					body+="<br/>";
					File reportsZip = new File(reportsZipPath);
					if(reportsZip.exists() && reportsZip.isFile())
					{
						body+="Please find the attached Zip file with the reports for the MNAO Data Export Job.";
					}

					if(null!=zipFilePathForEmailNotif && !"".equals(zipFilePathForEmailNotif))
					{
						body+="<br/><br/> Please click <a href=\""+zipFilePathForEmailNotif+"\" target=\"_blank\">here<a/> to download extracted data zip file.";
					}
					
					body+="<br/><br/>Regards,<br/>GMS3 Application.<br/>Note - This is an auto generated email. Please do not respond to this email.";

					if(reportsZip.exists() && reportsZip.isFile())
					{
						// send Mail with Attachments
						SendMailUsingAuthentication.newPostHTMLMailWithAttachment(toEmailsList, subject, body, fromEmailId, reportsZipPath);
					}
					else
					{
						// send normal HTML Mail
						SendMailUsingAuthentication.newPostHTMLMail(toEmailsList, subject, body, fromEmailId);
					}
					reportsZip = null;
					reportsZipPath = null;
					fromEmailId = null;
					toEmailsList = null;
					body = null;
					subject = null;
				}
				toEmailIds=null;
			}
			else
			{
				logger.info("sendEmailNotification :: Email Notification could not be sent, because require parameters are null. "+ scheduleId+"/"+scheduleName+"/"+scheduleStatus+"/"+wslId);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "sendEmailNotification()", e);
		}
	}
	
	private void addViewContentDataToChannelSpecificList(List<MNAOViewContentDetails> vcList, String locale, String model, String carlineCode,String documentId)
	{
		try
		{
			if(documentId.startsWith("SM") || documentId.startsWith("OSM") || documentId.startsWith("WD"))
			{
				boolean alreadyExist = false;
				if(null!=smViewContentList && smViewContentList.size()>0) 
				{
					MNAOViewContentDetails existHierarchyDetails = null;
					for(int a=0;a<smViewContentList.size();a++)
					{
						existHierarchyDetails = (MNAOViewContentDetails)smViewContentList.get(a);
//						if(existHierarchyDetails.getLocale().equals(locale) && existHierarchyDetails.getModelName().equals(model) 
//								&& existHierarchyDetails.getCarlineCode().equals(carlineCode))
						if(existHierarchyDetails.getLocale().equals(locale) && existHierarchyDetails.getProcessingModelForMNAODataExport().equals(model))
						{
							alreadyExist = true;
							// hierarchy already exists - just add ViewContentDetails
							if(null==existHierarchyDetails.getViewContentList() || existHierarchyDetails.getViewContentList().size()<=0)
							{
								existHierarchyDetails.setViewContentList(new ArrayList<MNAOViewContentDetails>());
							}
							existHierarchyDetails.getViewContentList().addAll(vcList);
							break;
						}
						existHierarchyDetails = null;
					}
				}
				if(alreadyExist==false)
				{
					/*
					 * PREPARE FIRST LEVEL HIERARCHY WHICH WILL BE
					 * 	LOCALE
					 * 		MODEL_NAME CARLINECODE
					 * 			VIEW_CONTENT_LIST
					 */
					MNAOViewContentDetails hierarchyDetails = new MNAOViewContentDetails();
					hierarchyDetails.setLocale(locale);
					hierarchyDetails.setProcessingModelForMNAODataExport(model);
//					hierarchyDetails.setCarlineCode(carlineCode);
					if(null==hierarchyDetails.getViewContentList() || hierarchyDetails.getViewContentList().size()<=0)
					{
						hierarchyDetails.setViewContentList(new ArrayList<MNAOViewContentDetails>());
					}
					hierarchyDetails.getViewContentList().addAll(vcList);
					if(null==smViewContentList || smViewContentList.size()<=0)
					{
						smViewContentList = new ArrayList<MNAOViewContentDetails>();
					}
					smViewContentList.add(hierarchyDetails);
					hierarchyDetails  =null;
				}
			}
			else
			{
				boolean alreadyExist = false;
				if(null!=siViewContentList && siViewContentList.size()>0) 
				{
					MNAOViewContentDetails existHierarchyDetails = null;
					for(int a=0;a<siViewContentList.size();a++)
					{
						existHierarchyDetails = (MNAOViewContentDetails)siViewContentList.get(a);
//						if(existHierarchyDetails.getLocale().equals(locale) && existHierarchyDetails.getModelName().equals(model) 
//								&& existHierarchyDetails.getCarlineCode().equals(carlineCode))
						if(existHierarchyDetails.getLocale().equals(locale) && existHierarchyDetails.getProcessingModelForMNAODataExport().equals(model))
						{
							alreadyExist = true;
							// hierarchy already exists - just add ViewContentDetails
							if(null==existHierarchyDetails.getViewContentList() || existHierarchyDetails.getViewContentList().size()<=0)
							{
								existHierarchyDetails.setViewContentList(new ArrayList<MNAOViewContentDetails>());
							}
							existHierarchyDetails.getViewContentList().addAll(vcList);
							break;
						}
						existHierarchyDetails = null;
					}
				}
				if(alreadyExist==false)
				{
					/*
					 * PREPARE FIRST LEVEL HIERARCHY WHICH WILL BE
					 * 	LOCALE
					 * 		MODEL_NAME CARLINECODE
					 * 			VIEW_CONTENT_LIST
					 */
					MNAOViewContentDetails hierarchyDetails = new MNAOViewContentDetails();
					hierarchyDetails.setLocale(locale);
					hierarchyDetails.setProcessingModelForMNAODataExport(model);
//					hierarchyDetails.setCarlineCode(carlineCode);
					if(null==hierarchyDetails.getViewContentList() || hierarchyDetails.getViewContentList().size()<=0)
					{
						hierarchyDetails.setViewContentList(new ArrayList<MNAOViewContentDetails>());
					}
					hierarchyDetails.getViewContentList().addAll(vcList);
					if(null==siViewContentList || siViewContentList.size()<=0)
					{
						siViewContentList = new ArrayList<MNAOViewContentDetails>();
					}
					siViewContentList.add(hierarchyDetails);
					hierarchyDetails  =null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "addViewContentDataToChannelSpecificList()", e);
		}
	}
	
	private static String getStringSizeLengthFile(long size) 
	{
		String sizeStr=null;
		try
		{
			DecimalFormat df = new DecimalFormat("0.00");
			float sizeKb = 1024.0f;
			float sizeMb = sizeKb * sizeKb;
			float sizeGb = sizeMb * sizeKb;
			float sizeTerra = sizeGb * sizeKb;

			if(size < sizeMb)
			{
				sizeStr = df.format(size / sizeKb)+ " KB";
			}
			else if(size < sizeGb)
			{
				sizeStr= df.format(size / sizeMb) + " MB";
			}
			else if(size < sizeTerra)
			{
				sizeStr = df.format(size / sizeGb) + " GB";
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportScheduleImpl.class.getName(), "addViewContentDataToChannelSpecificList()", e);
		}
	    return sizeStr;
	}
	
}