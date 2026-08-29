package com.mazda.gms3.mdm.im.impl;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mazda.gms3.mdm.dao.TranslationJobProcessingDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.kapture.TranslationKaptureService;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.TranslationUpdateReportsUtil;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ContentDetails;
import com.mazda.gms3.mdm.vo.SITranslationScheduleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.dao.TranslationUpdateDAO;

public class TranslationImportProcessingImpl extends Thread{

	private Logger logger = LogManager.getLogger(TranslationImportProcessingImpl.class);

	private TranslationJobProcessingDAO processingDAO = null;

	/** One Kapture login for the whole schedule - see getConnectionWithKapture(). */
	private KaptureApiClient client = null;

	private int authTokenGenerationCount=0;

	private ArrayList<ContentDetails> transactionList = null;

	private TranslationUpdateReportsUtil printReportsUtil = null;
	
	private String wslId=null;
	

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
		scheduleAborted = TranslationUpdateDAO.isAborted(abortScheduleId);
		if(scheduleAborted==true)
		{
			logger.info("isScheduleAborted :: SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
					+" SCREEN. Stopping after the item in flight.");
		}
		return scheduleAborted;
	}
	public void startProcess(SITranslationScheduleDetails schDetails, String folderPath)
	{
		// remember the schedule so the abort check works in the helpers
		abortScheduleId = String.valueOf(schDetails.getScheduleId());
		String scheduleId="";
		// initialize all variables
		processingDAO = new TranslationJobProcessingDAO();
		try
		{
			client =null;
			authTokenGenerationCount = 0;
			if(getConnectionWithKapture()==false)
			{
				logger.info("startProcess :: COULD NOT LOG IN TO KAPTURE - every document would fail with the same cause, so the schedule is stopped here.");
			}
			transactionList=  new ArrayList<ContentDetails>();
			printReportsUtil = new TranslationUpdateReportsUtil();
			wslId= "";
			if(null!=schDetails && schDetails.getScheduleId()>0)
			{
				scheduleId = String.valueOf(schDetails.getScheduleId());
				wslId= schDetails.getUserId();
				// UPDATE PROCESSING STATUS
				processingDAO.updateProcessingStatus(scheduleId);

				if(null!=folderPath && !"".equals(folderPath) && null!=schDetails.getLocaleCode() && !"".equals(schDetails.getLocaleCode()))
				{
					boolean jobFailure=false;

					/*
					 * CHECK IF FOLDER PATH EXISTS OR NOT
					 */
					File zipDir = new File(folderPath);
					if(zipDir.exists() && zipDir.isDirectory())
					{
						File[] xmlFiles = zipDir.listFiles();
						if(null!=xmlFiles && xmlFiles.length>0)
						{
							File xmlFile = null;
							byte[] data = null;
							ContentDetails details = null;
							for(int a=0;a<xmlFiles.length;a++)
							{
								if(isScheduleAborted()) { break; }
								xmlFile = (File)xmlFiles[a];
								if(!xmlFile.getName().trim().toLowerCase().equals("thumbs.db"))
								{
									details = new ContentDetails();
									details.setLocale(schDetails.getLocaleCode().trim().replace("-", "_"));
									try
									{
//										details.setDocumentId(xmlFile.getName().substring(0,xmlFile.getName().lastIndexOf(".")));
										
										// start Preparing Data
										data = FileReadWriteUtil.readFile(xmlFile.getAbsolutePath());
										if(null!=data && data.length>0)
										{
											details.setXml(new String(data));
											details.setDocumentId(getDocumentId(details.getXml()));
											// proceed for fetching content and modifying it.
											details=  modifyContent(details);
											if(null!=details.getContentId() && !"".equals(details.getContentId()))
											{
												/*
												 * check if Processing document is SM
												 * Update the title in LOCALE BASED VIEW CONTENT TABLE FOR THE DOCUMENT IDENTIFIED USING THE 
												 * LOCALE
												 */

												// check if Locale belongs to MME LOCALE
												if((details.getDocumentId().trim().toLowerCase().startsWith("sm")) && (!ApplicationProperties.getProperty("mnao.countries.locales.codes").trim().toLowerCase().contains(details.getLocale().replace("-", "_").trim().toLowerCase()) 
														&& !ApplicationProperties.getProperty("mc.countries.locales.codes").trim().toLowerCase().contains(details.getLocale().replace("-", "_").trim().toLowerCase())))
												{
													// update title in view content table
													String title="";
													if(null!=details.getXml() && !"".equals(details.getXml()))
													{
														try
														{
															title = retrieveTitle(details.getXml());
															if(null!=title && !"".equals(title))
															{
																details = processingDAO.updateTitleInViewContent(details, title);
																if(details.isTitleUpdateStatus()==true)
																{
																	// title successfully updated in VIEW CONTENT TABLE
																	// set processingStatus as Success
																	details.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
																	// update successCount
																	processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_SUCCESS);
																}
																else
																{
																	// set processingStatus as Failure
																	details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
																	// update failureCount
																	processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
																	// set job status as Failure
																	jobFailure = true;
																}
															}
															else
															{
																// set processingStatus as Failure
																details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
																// update failureCount
																processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
																// set job status as Failure
																jobFailure = true;
																if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
																{
																	details.setErrorMessage(details.getErrorMessage()+". Failed to identify Title from Source XML for updating in View Content. Either it is blank or Title node not available in Source XML.");
																}
																else
																{
																	details.setErrorMessage("Failed to identify Title from Source XML for updating in View Content. Either it is blank or Title node not available in Source XML.");
																}
															}
														}
														catch(Exception e)
														{
															Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "startProcess()", e);
															// set processingStatus as Failure
															details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
															// update failureCount
															processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
															// set job status as Failure
															jobFailure = true;
															if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
															{
																details.setErrorMessage(details.getErrorMessage()+"."+ e.getMessage());
															}
															else
															{
																details.setErrorMessage(e.getMessage());
															}
														}
													}
													title=  null;
												}
												else
												{
													// set processingStatus as Success
													details.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
													// update successCount
													processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_SUCCESS);
												}
											}
											else
											{
												// set processingStatus as Failure
												details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
												// update failureCount
												processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
												// set job status as Failure
												jobFailure = true;
											}
										}
										else
										{
											if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
											{
												details.setErrorMessage(details.getErrorMessage()+" "+"Failed to read XML File.");
											}
											else
											{
												details.setErrorMessage("Failed to read XML File.");
											}
											// set processingStatus as Failure
											details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
											// update failureCount
											processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
											// set job status as Failure
											jobFailure = true;
										}
										data = null;
										// proceed for fetching document and update the new content data from the uploaded XML
									}
									catch(Exception e)
									{
										Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "startProcess()", e);
										// set job status as Failure = as failed to read / identify 1 file
										jobFailure=true;
										if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
										{
											details.setErrorMessage(details.getErrorMessage()+" "+"Exception Occured while processing File :: >"+ e.getLocalizedMessage());
										}
										else
										{
											details.setErrorMessage("Exception Occured while processing File :: >"+ e.getLocalizedMessage());
										}
										// set processingStatus as Failure
										details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
										// update failureCount
										processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
									}
									// add to transactionList
									if(null==transactionList || transactionList.size()<=0)
									{
										transactionList = new ArrayList<ContentDetails>();
									}
									transactionList.add(details);
									details = null;
								}
								xmlFile=  null;
							}
							details=  null;
							xmlFile=  null;
							data = null;

							if(null!=transactionList && transactionList.size()>0)
							{
								// PRINT REPORTS
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

								printReportsUtil.printImportTransactionReport(transactionList, scheduleId);
							}

							if(jobFailure==true)
							{
								logger.info("startProcess :: Some Exceptions has occured while execution. Update Schedule Status as Failure.");
								processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "Some Exceptions has occured while execution. Please refer reports for more details.");
							}
							else
							{
								logger.info("startProcess :: No Exceptions occured while execution. Update Schedule Status as Success.");
								processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_SUCCESS, null);
							}
						}
						else
						{
							// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO ZIP / XML FILE DATA FOUND.
							processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Extracted ZIP / XML File Data Found.");
						}
						xmlFiles= null;
					}
					else
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO ZIP / XML FILE DATA FOUND.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Extracted ZIP / XML File Data Found.");
					}
					zipDir= null;
				}
				else
				{
					logger.info("startProcess :: No ZIP / Blank Locale are passed as paramter for Export Operation. Existing Operation.");
					if((null==folderPath || !"".equals(folderPath)) && (null==schDetails.getLocaleCode() || "".equals(schDetails.getLocaleCode())))
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO ZIP / XML FILE DATA FOUND & LOCALE IS NULL.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Extracted ZIP / XML File Data Found & No Locale provided.");
					}
					else if(null==folderPath || "".equals(folderPath))
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO ZIP / XML FILE DATA FOUND.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Extracted ZIP / XML File Data Found.");
					}
					else if(null==schDetails.getLocaleCode() || "".equals(schDetails.getLocaleCode()))
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=LOCALE IS NULL.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Locale provided.");
					}

				}
			}
			else
			{
				logger.info("startProcess :: Schedule Details / Schedule id as Parameter is null. Exiting Operation.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "startProcess()", e);
			// update scheduleCompletion - with scheduleStatus as Failure
			processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE,"Exceptions Occured while execution :: >"+ e.getMessage());
		}
		finally
		{

			// close all active connection & client objects
			try
			{
				if(null!=processingDAO.conn)
				{
					logger.info(" ******************** TranslationImportProcessingImpl :: CLOSING DB CONNECTION OBJECT. ***********************");
					processingDAO.conn.close();
				}
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "startProcess()", e);
			}

			// the Kapture client holds no socket of its own - dropping the reference is the close
			logger.info(" ******************** TranslationImportProcessingImpl :: FOR SCHEDULE ID {"+scheduleId+"}. GENERATED DB CONNECTION COUNTS ARE :: > "+ processingDAO.connectionCount);

			client = null;
			authTokenGenerationCount=  0;
			transactionList = null;
			processingDAO.connectionCount = 0;
			processingDAO = null;
			printReportsUtil  = null;


			folderPath=  null;
			schDetails = null;
			wslId=  null;
		}

	}


	/**
	 * LOGS IN TO KAPTURE ONCE FOR THE SCHEDULE. Replaces getConnectionWithIM() and its token
	 * re-issue loop. A failure is fatal to the run and is logged as such, because every document
	 * would otherwise fail one by one with the same cause.
	 */
	private boolean getConnectionWithKapture()
	{
		try
		{
			client = new KaptureApiClient();
			if(client.login()==true)
			{
				logger.info("getConnectionWithKapture :: LOGGED IN TO KAPTURE.");
				return true;
			}
			logger.info("getConnectionWithKapture :: KAPTURE LOGIN FAILED - the schedule cannot run.");
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "getConnectionWithKapture()", e);
		}
		client = null;
		return false;
	}

	/**
	 * WRITES THE TRANSLATED CONTENT BACK INTO THE DOCUMENT.
	 *
	 * WAS: modifyContent(content, content.getPublished()) on the IQ client.
	 * NOW: update-article-tab for a master identifier, save-translate-article for a translation,
	 * followed by publish-article when the document WAS published - see TranslationKaptureService.
	 *
	 * PUBLISHED IN, PUBLISHED OUT, and that is deliberate. The InfoManager call passed
	 * content.getPublished() rather than a hardcoded false, so this screen has always preserved the
	 * state. (SI Data Load hardcoded FALSE and therefore never publishes - the two screens differ
	 * on purpose; do not make them agree.)
	 *
	 * THE MERGE IS UNCHANGED. prepareXML() still takes the document's CURRENT content and replaces
	 * only the fields the vendor translated, which is why the payload echoes the read - a
	 * translation batch carries 9 fields of a 22-field document and the endpoints null anything
	 * omitted.
	 */
	private ContentDetails modifyContent(ContentDetails details)
	{
		try
		{
			// here since the document is PARENT DOCUMENT so no need of sending PARENT LOCALE
			details = getContentDetails(details);

			if (null != details && null != details.getXml() && !"".equals(details.getXml()))
			{
				if(FetchKaptureDataDAO.isDocumentCheckedOut(details.getDocumentId())==false)
				{
					/*
					 * PERFORM XML OPERATION - merge the translated fields into the current content
					 */
					String mergedXml = prepareXML(details, details.getXml());

					TranslationKaptureService.TranslationResult before =
							(TranslationKaptureService.TranslationResult) details.getReadResult();
					TranslationKaptureService.TranslationResult written = TranslationKaptureService.write(
							details.getDocumentId(), details.getLocale(), mergedXml, before, client);

					if (written.isOk()==true)
					{
						logger.info("modifyContent :: Document Updated successfully for Document Id is :: > " + details.getDocumentId());
						logger.info(" Content Id :: > "	+ written.getContentId());
						logger.info(" Version :: > "	+ written.getVersion());
						logger.info(" Doc Status :: > "	+ written.isPublished());

						details.setContentId(written.getContentId());
						details.setModifiedVersion(written.getVersion());
						if(written.isPublished()==true)
						{
							details.setPublishStatus("Publish");
						}
						else
						{
							details.setPublishStatus("Draft");
						}
						details.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
					}
					else
					{
						logger.info("modifyContent :: Failed to Modify Document for DOCUMENT ID :: >"+ details.getDocumentId());
						// EXPLICITLY LEFT NULL - it is what the caller tests to decide the write failed
						details.setContentId(null);
						details.setErrorCode(written.getErrorCode());
						details.setErrorMessage(written.getErrorMessage());
						details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
					}
					mergedXml = null;
				}
				else
				{
					logger.info("modifyContent :: Document is Checked Out. Failed to Modify Document for DOCUMENT ID :: >"+ details.getDocumentId()+".");
					details.setErrorCode("IMMOD001");
					details.setErrorMessage("Document is checked out in Kapture. Cannot be modified.");
					details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				}
			}
			else
			{
				logger.info("modifyContent :: Failed to Fetch Content from Kapture For Document id :: >"+ details.getDocumentId()+" for Locale :: >"+ details.getLocale());
				details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
		}
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "modifyContent()", e);
			details.setErrorCode(TranslationKaptureService.ERR_UNEXPECTED);
			details.setErrorMessage("Unexpected error updating "+details.getDocumentId()+" for "
					+details.getLocale()+". "+e.getMessage());
			details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
		}
		return details;
	}

	/**
	 * THE DOCUMENT'S CURRENT CONTENT, for the merge to be applied to.
	 *
	 * WAS: getLatestContentRecordByDocumentIDAndLocale(), whose ContentRecordITO carried the XML,
	 * the version and the checked-out flag together. Those now come from three places - the XML and
	 * version from latest-article, the checked-out flag from k_article_checkout - so the READ IS
	 * KEPT ON THE DETAILS OBJECT: the write has to hand every field of it back, and re-reading
	 * would risk basing the write on a different version from the one merged.
	 */
	private ContentDetails getContentDetails(ContentDetails details)
	{
		try
		{
			TranslationKaptureService.TranslationResult read = TranslationKaptureService.read(
					details.getDocumentId(), details.getLocale(), client);
			if(read.isOk()==true)
			{
				details.setFetchedVersion(read.getVersion());
				details.setContentId(read.getContentId());
				details.setXml(read.getNodeXml());
				details.setReadResult(read);
			}
			else
			{
				// LEFT NULL DELIBERATELY - the caller tests it to decide the document failed
				details.setXml(null);
				details.setErrorCode(read.getErrorCode());
				details.setErrorMessage(read.getErrorMessage());
				logger.info("getContentDetails :: {"+details.getDocumentId()+"} / {"+details.getLocale()+"} :: "+ read.getErrorMessage());
			}
		}
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "getContentDetails()", e);
			details.setXml(null);
			details.setErrorCode(TranslationKaptureService.ERR_UNEXPECTED);
			details.setErrorMessage("Unexpected error reading "+details.getDocumentId()+" for "
					+details.getLocale()+". "+e.getMessage());
		}
		return details;
	}

	private String prepareXML(ContentDetails details, String sourceXML)
	{
		try
		{
			if(null!=details.getXml() && !"".equals(details.getXml()))
			{
				String contnetNodes="";
				String rootNode="";
				if(details.getDocumentId().trim().toLowerCase().startsWith("si"))
				{
					contnetNodes=ApplicationProperties.getProperty("SI_NODES_LIST");
					rootNode = ApplicationProperties.getProperty("SI_ROOT_NODE");
				}
				else if(details.getDocumentId().trim().toLowerCase().startsWith("sm"))
				{
					contnetNodes = ApplicationProperties.getProperty("SM_NODES_LIST");
					rootNode=  ApplicationProperties.getProperty("SM_ROOT_NODE");
				}

				if(null!=contnetNodes && !"".equals(contnetNodes))
				{
					String[] conNodes = contnetNodes.split(",");
					if(null!=conNodes && conNodes.length>0)
					{
						// convert SOURCE XML TO DOCUMENT
						DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						InputSource is = new InputSource();
						is.setCharacterStream(new StringReader(sourceXML));
						Document doc = db.parse(is);
						if(null!=doc)
						{
							NodeList nodesList = doc.getElementsByTagName(rootNode);
							if(null!=nodesList && nodesList.getLength()>0)
							{
								Node node = nodesList.item(0);
								NodeList childNodesList = node.getChildNodes();
								Node childNode=null;
								NodeList otherNodesList = null;
								for (int b=0;b<conNodes.length;b++)
								{
									if(null!=childNodesList && childNodesList.getLength()>0)
									{
										for(int a=0;a<childNodesList.getLength();a++)
										{
											childNode =(Node)childNodesList.item(a);
											if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
											{
												if(childNode.getNodeName().equalsIgnoreCase(conNodes[b]))
												{
													node.removeChild(childNode);
													break;
												}
											}
											childNode= null;											
										}
									}
									// for safe side explicitly search node and remove them
									otherNodesList = doc.getElementsByTagName(conNodes[b]);
									if(null!=otherNodesList && otherNodesList.getLength()> 0)
									{
										node.removeChild(otherNodesList.item(0));
									}
									otherNodesList = null;
								}
								childNodesList=null;
								node = null;
							}
							nodesList = null;

							// convert doc to String
							String convertedString =Utilities.transformString(doc);
							// remove the last index of root node closing node from converted string
							String beforeData = convertedString.substring(0, convertedString.lastIndexOf("</"+rootNode+">"));
							String afterData = convertedString.substring(convertedString.lastIndexOf("</"+rootNode+">"), convertedString.length());

							String dataToAdd = details.getXml();
							dataToAdd = dataToAdd.substring(dataToAdd.indexOf("<"+rootNode+">")+1, dataToAdd.indexOf("</"+rootNode+">"));

							// prepare Source XML
							sourceXML= beforeData+dataToAdd+afterData;

							convertedString=  null;
							beforeData=  null;
							afterData=  null;
							dataToAdd = null;
						}
						doc = null;
						is=null;
						db = null;
					}
				}
				contnetNodes= null;
				rootNode= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "prepareXML", e);
		}
		return sourceXML;
	}

	private String retrieveTitle(String xml)
	{
		String title="";
		try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			Document doc = db.parse(is);
			if(null!=doc)
			{
				NodeList nodeList = doc.getElementsByTagName("TITLE");
				if(null!=nodeList && nodeList.getLength()>0)
				{
					Node node = nodeList.item(0);
					title = getCharacterDataFromElement((Element)node);
					node = null;
				}
				nodeList = null;
			}
			doc = null;
			is = null;
			db = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "retrieveTitle", e);
		}
		return title;
	}
	
	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}
	
	private String getDocumentId(String xml)
	{
		String documentId="";
		try
		{
			if(null!=xml && !"".equals(xml))
			{
				// convert SOURCE XML TO DOCUMENT
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xml));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList list = doc.getElementsByTagName("DOCUMENTID");
					if(null!=list && list.getLength()>0)
					{
						Node node = list.item(0);
						documentId= getCharacterDataFromElement((Element)node);
						node = null;
					}
					list = null;
				}
				doc=  null;
				is = null;
				db = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationImportProcessingImpl.class.getName(), "getDocumentId()", e);
		}
		return documentId;
	}
}
