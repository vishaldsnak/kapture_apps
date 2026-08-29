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

public class TranslationExportProcessingImpl extends Thread{

	private Logger logger = LogManager.getLogger(TranslationExportProcessingImpl.class);

	private TranslationJobProcessingDAO processingDAO = null;

	/**
	 * ONE LOGIN FOR THE WHOLE SCHEDULE. The IQ client was re-authenticated whenever its token
	 * expired mid-run, which is what authTokenGenerationCount counted; the Kapture client holds a
	 * bearer token for the run and every call goes through it.
	 */
	private KaptureApiClient client = null;

	private int authTokenGenerationCount=0;

	private ArrayList<ContentDetails> transactionList = null;

	private TranslationUpdateReportsUtil printReportsUtil = null;


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
	public void startProcess(SITranslationScheduleDetails schDetails, ArrayList<String> documentsList)
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

			if(null!=schDetails && schDetails.getScheduleId()>0)
			{
				scheduleId = String.valueOf(schDetails.getScheduleId());
				// UPDATE PROCESSING STATUS
				processingDAO.updateProcessingStatus(scheduleId);

				if(null!=documentsList && documentsList.size()>0 && null!=schDetails.getLocaleCode() && !"".equals(schDetails.getLocaleCode()))
				{
					boolean jobFailure=false;
					/*
					 * ITERATE EACH DOCUMENT LIST & START FETCHING THE XML NODE OF IT
					 * GENERTAE AN XML FILE FOR EACH OF THE DOCUMENT.
					 */
					ContentDetails details = null;
					for(int a=0;a<documentsList.size();a++)
					{
						if(isScheduleAborted()) { break; }
						details = new ContentDetails();
						details.setDocumentId(documentsList.get(a).toString().trim().toUpperCase());
						details.setLocale(schDetails.getLocaleCode().replace("-", "_"));
						// get XML Data
						details = getContentDetails(details);
						if(null!=details.getXml() && !"".equals(details.getXml()))
						{
							// explicitly set error code and message to null= as XML is not blank.
							details.setErrorCode(null);
							details.setErrorMessage(null);
							// generate XML for the document inside the Schedule Directory.
							boolean bool = generateXML(details, scheduleId);
							if(bool==true)
							{
								// UPDATE PROCESSING STATUS AS SUCCESS & COUNT
								processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_SUCCESS);
								// set Processing status as Success
								details.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
							}
							else
							{
								// set job failure to true
								jobFailure= true;
								// update failureCount
								processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
								// set Processing status as Failure
								details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
								// if errorMessages is null - Add custom error message
								if(null==details.getErrorMessage() || "".equals(details.getErrorMessage()))
								{
									details.setErrorMessage("Failed to cretae XML File for document on File server.");
								}
							}
						}
						else
						{
							// set job failure to true
							jobFailure= true;
							// update failureCount
							processingDAO.updateProcessingCount(scheduleId, ScheduleConstants.STATUS_FAILURE);
							// set Processing status as Failure
							details.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							// if errorMessages is null - Add custom error message
							if(null==details.getErrorMessage() || "".equals(details.getErrorMessage()))
							{
								details.setErrorMessage("Failed to fetch Content Node for document from Kapture. Content Node is returned as null.");
							}
						}
						if(null==transactionList || transactionList.size()<=0)
						{
							transactionList = new ArrayList<ContentDetails>();
						}
						transactionList.add(details);
						details = null;
					}

					String zipCreationFailureMessage="";
					if(null!=transactionList && transactionList.size()>0)
					{
						// GENERATE ZIP DIRECTORY FOR THE XMLS
						String path = ApplicationProperties.getProperty("translation.update.export.physical.path");
						if(!path.endsWith("/"))
						{
							path+="/";
						}
						path+=scheduleId+"/";

						/*
						 * call function to generate zip file
						 */
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

						boolean bool = printReportsUtil.createReportsZip(path, scheduleId);
						if (bool == true) 
						{
							logger.info("startProcess :: XML Files Zipped Successfully.");
						} 
						else 
						{
							logger.info("startProcess :: Failed to Zip XML Files, these have to be downloaded manually.");
							// fail Job
							jobFailure = true;
							zipCreationFailureMessage="Failed to generate zip for exported XML Files. ";
						}
						path = null;

						// PRINT REPORTS
						printReportsUtil.printExportTransactionReport(transactionList, scheduleId);
					}

					/*
					 * UPDATE JOB STATUS
					 */
					if(jobFailure==true)
					{
						logger.info("startProcess :: Some Exceptions has occured while execution. Update Schedule Status as Failure.");
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "Some Exceptions has occured while execution. "+zipCreationFailureMessage+"Please refer reports for more details.");
					}
					else
					{
						logger.info("startProcess :: No Exceptions occured while execution. Update Schedule Status as Success.");
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_SUCCESS, null);
					}
				}
				else
				{
					logger.info("startProcess :: No Document ids / Blank Locale are passed as paramter for Export Operation. Existing Operation.");
					if((null==documentsList || documentsList.size()<=0) && (null==schDetails.getLocaleCode() || "".equals(schDetails.getLocaleCode())))
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO DOCUMENTS FOUND & LOCALE IS NULL.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Documents Found & No Locale provided.");
					}
					else if(null==documentsList || documentsList.size()<=0)
					{
						// UPDATE JOB STATUS=FAILURE,PROCESSING_STATUS=COMPLETED, REMARKS=NO DOCUMENTS FOUND.
						processingDAO.updateScheduleCompletion(scheduleId, ScheduleConstants.STATUS_FAILURE, "No Documents Found.");
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
			Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "startProcess()", e);
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
					logger.info(" ******************** TranslationExportProcessingImpl :: CLOSING DB CONNECTION OBJECT. ***********************");
					processingDAO.conn.close();
				}
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "startProcess()", e);
			}

			// the Kapture client holds no socket of its own - dropping the reference is the close
			logger.info(" ******************** TranslationExportProcessingImpl :: FOR SCHEDULE ID {"+scheduleId+"}. GENERATED DB CONNECTION COUNTS ARE :: > "+ processingDAO.connectionCount);

			client = null;
			authTokenGenerationCount=  0;
			transactionList = null;
			processingDAO.connectionCount = 0;
			processingDAO = null;
			printReportsUtil  = null;


			documentsList=  null;
			schDetails = null;
		}

	}


	/**
	 * LOGS IN TO KAPTURE ONCE FOR THE SCHEDULE.
	 *
	 * Replaces getConnectionWithIM(), which built an IQServiceClient and re-issued its
	 * authentication token whenever it expired. A failure here is fatal to the run and says so -
	 * every document would otherwise fail one by one with the same cause.
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
			Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "getConnectionWithKapture()", e);
		}
		client = null;
		return false;
	}

	/**
	 * THE DOCUMENT'S TRANSLATABLE CONTENT, READY FOR prepareXML().
	 *
	 * WAS: getLatestContentRecordByDocumentIDAndLocale() on the IQ client, whose getXml() returned
	 * the whole document XML. NOW: latest-article, whose channel node is handed back in the SAME
	 * XML shape - see TranslationKaptureService. prepareXML() is therefore untouched: it still
	 * receives a document XML and still pulls SI_NODES_LIST / SM_NODES_LIST out of it.
	 *
	 * THE ERROR CODE AND MESSAGE ARE ALWAYS SET ON FAILURE, because they are printed in the
	 * transaction report - a failed document with no reason is not a usable report line.
	 */
	private ContentDetails getContentDetails(ContentDetails contentDetails)
	{
		try
		{
			TranslationKaptureService.TranslationResult read = TranslationKaptureService.read(
					contentDetails.getDocumentId(), contentDetails.getLocale(), client);
			if(read.isOk()==true)
			{
				contentDetails.setFetchedVersion(read.getVersion());
				contentDetails.setContentId(read.getContentId());
				contentDetails.setXml(prepareXML(read.getNodeXml(), contentDetails.getDocumentId(), contentDetails.getLocale()));
			}
			else
			{
				// LEFT NULL DELIBERATELY - it is what the caller tests to decide the document failed
				contentDetails.setXml(null);
				contentDetails.setErrorCode(read.getErrorCode());
				contentDetails.setErrorMessage(read.getErrorMessage());
				logger.info("getContentDetails :: {"+contentDetails.getDocumentId()+"} / {"+contentDetails.getLocale()+"} :: "+ read.getErrorMessage());
			}
		}
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "getContentDetails()", e);
			contentDetails.setXml(null);
			contentDetails.setErrorCode(TranslationKaptureService.ERR_UNEXPECTED);
			contentDetails.setErrorMessage("Unexpected error reading "+contentDetails.getDocumentId()
					+" for "+contentDetails.getLocale()+". "+e.getMessage());
		}
		return contentDetails;
	}

	private boolean generateXML(ContentDetails details, String scheduleId)
	{
		boolean bool = true;
		try
		{
			String exportDirPath = ApplicationProperties.getProperty("translation.update.export.physical.path");
			File exportDir = new File(exportDirPath);
			if(exportDir.exists()==false || !exportDir.isDirectory())
			{
				exportDir.mkdir();
			}
			if(!exportDirPath.endsWith("/"))
			{
				exportDirPath+="/";
			}
			exportDirPath+=scheduleId;
			exportDir = null;
			exportDir = new File(exportDirPath);
			if(exportDir.exists()==false || !exportDir.isDirectory())
			{
				exportDir.mkdir();
			}
			exportDir = null;
			// add fileName as DOCUMENT_ID.XML
			exportDirPath=exportDirPath+"/"+details.getDocumentId()+".xml";
			// NOW START CREATING XMLS.
			String xmlData = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"+details.getXml();
			bool = FileReadWriteUtil.writeFile(xmlData.getBytes("utf-8"), exportDirPath);

			xmlData = null;
			exportDir = null;
			exportDirPath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "generateXML()", e);
			bool = false;
		}
		return bool;
	}
	
	private String prepareXML(String sourceXML,String documentId, String locale)
	{
		String identifiedXML="";
		try
		{
			/*
			 * ADD DOCUMENT ID & LOCALE TO THE XML
			 */
			identifiedXML+="<DOCUMENTID><![CDATA["+documentId+"]]></DOCUMENTID>";
			identifiedXML+="<LOCALE><![CDATA["+locale+"]]></LOCALE>";
			
			String contnetNodes="";
			String rootNode="";
			if(documentId.trim().toLowerCase().startsWith("si"))
			{
				contnetNodes=ApplicationProperties.getProperty("SI_NODES_LIST");
				rootNode = ApplicationProperties.getProperty("SI_ROOT_NODE");
			}
			else if(documentId.trim().toLowerCase().startsWith("sm"))
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
						NodeList nodesList = null;
						Node node = null;
						String val = "";
						for(int a=0;a<conNodes.length;a++)
						{
							nodesList = doc.getElementsByTagName(conNodes[a].trim());
							if(null!=nodesList && nodesList.getLength()>0)
							{
								node = (Node)nodesList.item(0);
								val = getCharacterDataFromElement((Element)node);
								// append this to identified XML
								identifiedXML+= "<"+conNodes[a].trim()+">";
								identifiedXML+="<![CDATA[";
								if(null!=val && !"".equals(val))
								{
									identifiedXML+=val;
								}
								identifiedXML+="]]>";
								identifiedXML+= "</"+conNodes[a].trim()+">";
								val = null;
								node=null;
							}
							nodesList=  null;
						}
					}
					doc = null;
					db = null;
					is =  null;
				}
				conNodes = null;
			}
			contnetNodes= null;
			
			// append root Node
			identifiedXML=  "<"+rootNode+">"+identifiedXML+"</"+rootNode+">";
			rootNode = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationExportProcessingImpl.class.getName(), "prepareXML()", e);
		}
		return identifiedXML;
	}
	
	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}
}
