package com.mazda.gms3.mdm.sidataload.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.apache.poi.xwpf.converter.core.FileImageExtractor;
import org.apache.poi.xwpf.converter.core.FileURIResolver;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.google.gson.JsonObject;

import com.mazda.gms3.mdm.dao.CategoryProcessingDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.kapture.KaptureApiResult;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.dao.SIChannelDataLoadDAO;
import com.mazda.gms3.mdm.sidataload.kapture.SIChannelKaptureService;
import com.mazda.gms3.mdm.sidataload.kapture.SIChannelPayloadBuilder;
import com.mazda.gms3.mdm.sidataload.utils.PrintReportsUtils;
import com.mazda.gms3.mdm.sidataload.utils.ReadContentUtils;
import com.mazda.gms3.mdm.sidataload.utils.UnzipUtils;
import com.mazda.gms3.mdm.sidataload.utils.XcopyUtil;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelErrorsDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelImageDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelScheduleDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelSchemaDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIKaptureDocumentDetails;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.SendMailUsingAuthentication;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.utils.AbortCheck;


public class SIChannelDataLoadProcessingImpl {

	private Logger logger = LogManager.getLogger(SIChannelDataLoadProcessingImpl.class);

	private String zipFilePath = null;

	private XcopyUtil xcopyUtils = null;

	private SIChannelSchemaDetails contentDetails = null;

	private SIChannelErrorsDetails errorDetails = null;

	private PrintReportsUtils printUtils = null;


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
		scheduleAborted = SIChannelDataLoadDAO.isAborted(abortScheduleId);
		if(scheduleAborted==true)
		{
			logger.info("isScheduleAborted :: SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
					+" SCREEN. Stopping after the item in flight.");
		}
		return scheduleAborted;
	}
	public void startProcessing_WithWordFile(SIChannelScheduleDetails schDetails, byte[] data,String extension)
	{
		// remember the schedule so the abort check works in the helpers
		abortScheduleId = String.valueOf(schDetails.getScheduleId());
		contentDetails = new SIChannelSchemaDetails();
		try
		{
			if(null!=schDetails && schDetails.getScheduleId()>0 && null!=data && data.length>0)
			{
				xcopyUtils = new XcopyUtil();
				/*
				 * The image copy walks every image of the document inside XcopyUtil, so it needs its
				 * own way to notice an abort - otherwise a document with many images keeps copying
				 * long after the click (schedule 4330 only looked clean because it had three).
				 */
				xcopyUtils.setAbortCheck(new AbortCheck()
				{
					public boolean isAborted()
					{
						return isScheduleAborted();
					}
				});
				printUtils = new PrintReportsUtils();
				contentDetails.setLocale(schDetails.getLocale());
				contentDetails.setDocumentType(schDetails.getDocumentType());
				contentDetails.setDocumentId(schDetails.getDocumentId());
				contentDetails.setWslId(schDetails.getWslId());
				
				/*
				 * IDENTIFY MARKET 
				 */
				if(schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_us").trim().toLowerCase()) || 
						schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_ca").trim().toLowerCase()) || 
						schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("es_mx").trim().toLowerCase()) || 
						schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("fr_ca").trim().toLowerCase()))
				{
					// MNAO MARKET
					contentDetails.setMarket(ApplicationProperties.getProperty("market.mnao").trim().toUpperCase());
				}
				else if(schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						schDetails.getLocale().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET
					contentDetails.setMarket(ApplicationProperties.getProperty("market.mc").trim().toUpperCase());
				}
				else
				{
					// MME MARKET
					contentDetails.setMarket(ApplicationProperties.getProperty("market.mme").trim().toUpperCase());
				}

				/*
				 * WRTIE FILE DATA TO A TEMP DIRECTORY
				 * CONVERT WORD FILE DATA TO HTML FILE IN TEMP DIRECTORY
				 */
				/*
				 * ABORT CHECK EITHER SIDE OF THE LONG SINGLE-SHOT STEP BELOW.
				 *
				 * The Word-to-HTML conversion (and the zip write) is ONE call with no loop, so nothing
				 * inside it can test the abort flag - a schedule aborted while it runs would otherwise
				 * carry on through the whole read/import afterwards. Seen live on schedule 4327: the
				 * abort was recorded but the worker never reached a guarded point.
				 *
				 * It cannot interrupt the conversion itself - that is unavoidable - but it stops the
				 * moment control returns.
				 */
				if(isScheduleAborted()) { return; }
				boolean convertToHTML = convertWordToHtml(data, String.valueOf(schDetails.getScheduleId()), extension);
				if(isScheduleAborted()) { return; }
				if(convertToHTML==true)
				{
					// UPDATE PROCESSING STATUS TO PROCESSING
					SIChannelDataLoadDAO.updateScheduleProcessingStatus(ScheduleConstants.STATUS_PROCESSING, schDetails.getScheduleId());
					logger.info("startProcessing_WithWordFile :: Uploaded Word File Converted to HTML Successfully.");
					/*
					 * PROCEED FOR UNZIPPING ZIP FILE
					 */
					String destDirectory = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
					if(!destDirectory.endsWith("/"))
					{
						destDirectory+="/";
					}
					destDirectory+=String.valueOf(schDetails.getScheduleId())+"/Content/";
					/*
					 *  START LOOKING FOR THE HTML FILES
					 *  READ HTML FILE AND START IDENTIFYING SCHEMA FIELDS CONTENT
					 */
					startReadingContent_FromWord(destDirectory, schDetails);
					destDirectory  =null;
				}
				else
				{
					logger.info("startProcessing_WithWordFile :: Failed to Convert Uploaded Word File to HTML.Update Schedule Status as Failure.");

					/*
					 * TRACK ERROR
					 */
					errorDetails = new SIChannelErrorsDetails();
					errorDetails.setErrorMessage("Failed to Convert Uploaded Word File to HTML.");
					if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
					{
						contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
					}
					contentDetails.getErrorsList().add(errorDetails);
					errorDetails = null;
					// set Processing Details as NULL
					contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				}
			}
			else
			{
				logger.info("startProcessing_WithWordFile :: Schedule id / File Data is Null. Update Schedule Status as Failure.");
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Schedule Details or Uploaded File Data is Null.");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
		}
		catch(Exception e)
		{
			logger.info("startProcessing_WithWordFile :: Some Exception. Update Schedule Status as Failure for Reason :: > "+ e.getMessage());
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startProcessing_WithWordFile()", e);

			/*
			 * TRACK ERROR
			 */
			errorDetails = new SIChannelErrorsDetails();
			errorDetails.setErrorMessage(e.getMessage()+" Exception Occured.");
			if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
			{
				contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
			}
			contentDetails.getErrorsList().add(errorDetails);
			errorDetails = null;
			// set Processing Details as NULL
			contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
		}
		finally
		{
			zipFilePath = null;
		}

		try
		{
			/*
			 * PRINT REPORTS
			 * 	BEFORE THAT CHECK IF XCOPY LIST HAS ANY FAILURE, THEN PROCESSING STATUS OF CONTENT DETAILS WILL BE FAILURE;
			 */
			if(null!=xcopyUtils.getFileProcessingList() && xcopyUtils.getFileProcessingList().size()>0)
			{
				// set total OkAssets Count
				schDetails.setOkAssetsTotalCount(xcopyUtils.getFileProcessingList().size());
				SIChannelImageDetails imageDetails = null;
				for(int a=0;a<xcopyUtils.getFileProcessingList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					imageDetails = (SIChannelImageDetails)xcopyUtils.getFileProcessingList().get(a);
					if(null==imageDetails.getProcessingStatus() || (null!=imageDetails.getProcessingStatus() && imageDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_FAILURE))) 
					{
						// SET CONTENT DETAILS PROCESSING STATUS TO FAILURE
						contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

						/*
						 * TRACK THIS ERROR
						 */
						errorDetails = new SIChannelErrorsDetails();
						errorDetails.setErrorMessage("Please refer to File Transaction Report for more details.");
						if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
						{
							contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
						}
						contentDetails.getErrorsList().add(errorDetails);
						errorDetails = null;
						break;
					}
				}

				// identify successCount & failureCount
				int successCount=0;
				int failureCount=0;
				imageDetails = null;
				for(int a=0;a<xcopyUtils.getFileProcessingList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					imageDetails = (SIChannelImageDetails)xcopyUtils.getFileProcessingList().get(a);
					if(null!=imageDetails.getProcessingStatus() && imageDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
					{
						successCount++;
					}
					else
					{
						failureCount++;
					}
					imageDetails = null;
				}
				schDetails.setOkAssetsSuccessCount(successCount);
				schDetails.setOkAssetsFailureCount(failureCount);

				successCount = 0;
				failureCount = 0;
			}
			// set OTHER DETAILS
			schDetails.setDocumentId(contentDetails.getDocumentId());
			schDetails.setFetchedVersion(contentDetails.getFetchedVersion());
			schDetails.setModifiedVersion(contentDetails.getModifiedVersion());
			schDetails.setDocumentStatus(contentDetails.getPublishStatus());
			// set ZIP PATH
			schDetails.setZipFilePath(ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.web.path")+String.valueOf(schDetails.getScheduleId())+"/Content/"+String.valueOf(schDetails.getScheduleId())+"_Content."+ extension);
			// set REPORTS PATH
			schDetails.setReportsPath(ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.web.path")+ApplicationProperties.getProperty("sichannel.data.load.reports.dir")+String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleId())+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX"));

			/*
			 * Generate Reports
			 */
			/*
			 * ABORTED FROM THE SCREEN - STOP HERE, exactly as the pre-migration code did.
			 *
			 * THIS MUST SIT BEFORE THE FIRST REPORT IS WRITTEN, not before the zip. Anchoring it on
			 * createReportsZip() let 4329 write 4329_TRANSACTION_REPORT.xlsx and
			 * 4329_OKASSETS_REPORT.xlsx after the abort was already detected. On Java 8 stop() killed
			 * the thread here, so no report existed at all.
			 */
			if(isScheduleAborted()) { return; }
			printUtils.printTransactionReport(contentDetails, String.valueOf(schDetails.getScheduleId()));
			if(null!=xcopyUtils.getFileProcessingList() && xcopyUtils.getFileProcessingList().size()>0)
			{
				printUtils.printFileProcessingReport(xcopyUtils.getFileProcessingList(), String.valueOf(schDetails.getScheduleId()));
			}

			/*
			 * NOW PROCEED FOR CREATING ZIP FILE
			 */
			String reportsDirPath = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add reports folder
			reportsDirPath+=ApplicationProperties.getProperty("sichannel.data.load.reports.dir");
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add schedule id Dir
			reportsDirPath+=String.valueOf(schDetails.getScheduleId())+"/";

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

			boolean reportsZipGenerated=printUtils.createReportsZip(reportsDirPath, String.valueOf(schDetails.getScheduleId()));
			if(reportsZipGenerated==true)
			{
				logger.info("startProcessing_WithWordFile :: Reports Zipped Successfully.");
			}
			else
			{
				logger.info("startProcessing_WithWordFile :: Failed to Zip Reports. Please contact IT Admin Support.Update Schedule Status as Failure");
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Failed to Zip Generated Reports");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
			reportsDirPath = null;

			/*
			 *  update other Schedule Details & completion status
			 *  set scheduleStatus as well before moving to DB
			 */
			schDetails.setScheduleStatus(contentDetails.getProcessingStatus());
			SIChannelDataLoadDAO.updateScheduleCompletionDetails(schDetails, contentDetails.getErrorsList());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startProcessing_WithWordFile()", e);
		}
		contentDetails = null;
		zipFilePath = null;
		errorDetails = null;
		printUtils = null;
		xcopyUtils = null;
		// SEND EMAIL NOTIFICATION 
		sendEmailNotification(String.valueOf(schDetails.getScheduleId()), schDetails.getScheduleStatus(), schDetails.getScheduleName(), schDetails.getWslId());

	}

	public void startProcessing_WithZipFile(SIChannelScheduleDetails schDetails, byte[] data)
	{
		// remember the schedule so the abort check works in the helpers
		abortScheduleId = String.valueOf(schDetails.getScheduleId());
		contentDetails = new SIChannelSchemaDetails();
		try
		{
			if(null!=schDetails && schDetails.getScheduleId()>0 && null!=data && data.length>0)
			{
				xcopyUtils = new XcopyUtil();
				/*
				 * The image copy walks every image of the document inside XcopyUtil, so it needs its
				 * own way to notice an abort - otherwise a document with many images keeps copying
				 * long after the click (schedule 4330 only looked clean because it had three).
				 */
				xcopyUtils.setAbortCheck(new AbortCheck()
				{
					public boolean isAborted()
					{
						return isScheduleAborted();
					}
				});
				printUtils = new PrintReportsUtils();
				contentDetails.setLocale(schDetails.getLocale());
				contentDetails.setDocumentType(schDetails.getDocumentType());
				contentDetails.setDocumentId(schDetails.getDocumentId());
				contentDetails.setWslId(schDetails.getWslId());

				/*
				 * WRTIE FILE DATA TO A TEMP DIRECTORY
				 * CONVERT WORD FILE DATA TO HTML FILE IN TEMP DIRECTORY
				 */
				/*
				 * ABORT CHECK EITHER SIDE OF THE LONG SINGLE-SHOT STEP BELOW.
				 *
				 * The Word-to-HTML conversion (and the zip write) is ONE call with no loop, so nothing
				 * inside it can test the abort flag - a schedule aborted while it runs would otherwise
				 * carry on through the whole read/import afterwards. Seen live on schedule 4327: the
				 * abort was recorded but the worker never reached a guarded point.
				 *
				 * It cannot interrupt the conversion itself - that is unavoidable - but it stops the
				 * moment control returns.
				 */
				if(isScheduleAborted()) { return; }
				boolean writeZipFile = writeFileToWorkingDir(data, String.valueOf(schDetails.getScheduleId()));
				if(isScheduleAborted()) { return; }
				if(writeZipFile==true)
				{
					// UPDATE PROCESSING STATUS TO PROCESSING
					SIChannelDataLoadDAO.updateScheduleProcessingStatus(ScheduleConstants.STATUS_PROCESSING, schDetails.getScheduleId());
					logger.info("startProcessing_WithZipFile :: Uploaded File Data Written successfully to Web Server.");
					/*
					 * PROCEED FOR UNZIPPING ZIP FILE
					 */
					String destDirectory = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
					if(!destDirectory.endsWith("/"))
					{
						destDirectory+="/";
					}
					destDirectory+=String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleId())+"_SIContent";
					boolean proceedFurther = true;
					try
					{
						logger.info("startProcessing_WithZipFile :: Unizipping Zip File at Path :: >"+ destDirectory);

						UnzipUtils.unzip(zipFilePath, destDirectory);

						logger.info("startProcessing_WithZipFile :: Uploaded File at Source Location {"+zipFilePath+"} Unzipped Successfully.");
					}
					catch(Exception e)
					{
						proceedFurther=false;
						logger.info("startProcessing_WithZipFile :: Failed to Unzip uploaded Zip File at Path :: >" + destDirectory);
						logger.info("startProcessing_WithZipFile :: Unzip Exception. Update Schedule Status as Failure for Reason :: > "+ e.getMessage());
						Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startProcessing_WithZipFile()", e);
						/*
						 * TRACK ERROR
						 */
						errorDetails = new SIChannelErrorsDetails();
						errorDetails.setErrorMessage("Failed to Unzip the uploaded Zip File.");
						if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
						{
							contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
						}
						contentDetails.getErrorsList().add(errorDetails);
						errorDetails = null;
						// set Processing Details as NULL
						contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
					}

					if(proceedFurther==true)
					{
						/*
						 *  START LOOKING FOR THE HTML FILES
						 *  READ HTML FILE AND START IDENTIFYING SCHEMA FIELDS CONTENT
						 */
						startReadingContent_FromZip(destDirectory, schDetails);
					}

					destDirectory  =null;
				}
				else
				{
					logger.info("startProcessing_WithZipFile :: Failed to Upload File Data Written to Web Server.Update Schedule Status as Failure.");

					/*
					 * TRACK ERROR
					 */
					errorDetails = new SIChannelErrorsDetails();
					errorDetails.setErrorMessage("Failed to upload Zip file to Web Server.");
					if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
					{
						contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
					}
					contentDetails.getErrorsList().add(errorDetails);
					errorDetails = null;
					// set Processing Details as NULL
					contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				}
			}
			else
			{
				logger.info("startProcessing_WithZipFile :: Schedule id / File Data is Null. Update Schedule Status as Failure.");
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Schedule Details or Uploaded File Data is Null.");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
		}
		catch(Exception e)
		{
			logger.info("startProcessing_WithZipFile :: Some Exception. Update Schedule Status as Failure for Reason :: > "+ e.getMessage());
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startProcessing_WithZipFile()", e);

			/*
			 * TRACK ERROR
			 */
			errorDetails = new SIChannelErrorsDetails();
			errorDetails.setErrorMessage(e.getMessage()+" Exception Occured.");
			if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
			{
				contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
			}
			contentDetails.getErrorsList().add(errorDetails);
			errorDetails = null;
			// set Processing Details as NULL
			contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
		}
		finally
		{
			zipFilePath = null;
		}

		try
		{
			/*
			 * PRINT REPORTS
			 * 	BEFORE THAT CHECK IF XCOPY LIST HAS ANY FAILURE, THEN PROCESSING STATUS OF CONTENT DETAILS WILL BE FAILURE;
			 */
			if(null!=xcopyUtils.getFileProcessingList() && xcopyUtils.getFileProcessingList().size()>0)
			{
				// set total OkAssets Count
				schDetails.setOkAssetsTotalCount(xcopyUtils.getFileProcessingList().size());
				SIChannelImageDetails imageDetails = null;
				for(int a=0;a<xcopyUtils.getFileProcessingList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					imageDetails = (SIChannelImageDetails)xcopyUtils.getFileProcessingList().get(a);
					if(null==imageDetails.getProcessingStatus() || (null!=imageDetails.getProcessingStatus() && imageDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_FAILURE))) 
					{
						// SET CONTENT DETAILS PROCESSING STATUS TO FAILURE
						contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

						/*
						 * TRACK THIS ERROR
						 */
						errorDetails = new SIChannelErrorsDetails();
						errorDetails.setErrorMessage("Please refer to File Transaction Report for more details.");
						if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
						{
							contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
						}
						contentDetails.getErrorsList().add(errorDetails);
						errorDetails = null;
						break;
					}
				}

				// identify successCount & failureCount
				int successCount=0;
				int failureCount=0;
				imageDetails = null;
				for(int a=0;a<xcopyUtils.getFileProcessingList().size();a++)
				{
					if(isScheduleAborted()) { break; }
					imageDetails = (SIChannelImageDetails)xcopyUtils.getFileProcessingList().get(a);
					if(null!=imageDetails.getProcessingStatus() && imageDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
					{
						successCount++;
					}
					else
					{
						failureCount++;
					}
					imageDetails = null;
				}
				schDetails.setOkAssetsSuccessCount(successCount);
				schDetails.setOkAssetsFailureCount(failureCount);

				successCount = 0;
				failureCount = 0;
			}
			// set OTHER DETAILS
			schDetails.setDocumentId(contentDetails.getDocumentId());
			schDetails.setFetchedVersion(contentDetails.getFetchedVersion());
			schDetails.setModifiedVersion(contentDetails.getModifiedVersion());
			schDetails.setDocumentStatus(contentDetails.getPublishStatus());
			// set ZIP PATH
			schDetails.setZipFilePath(ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.web.path")+String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleId())+"_SIContent.zip");
			// set REPORTS PATH
			schDetails.setReportsPath(ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.web.path")+ApplicationProperties.getProperty("sichannel.data.load.reports.dir")+String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleId())+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX"));

			/*
			 * Generate Reports
			 */
			/*
			 * ABORTED FROM THE SCREEN - STOP HERE, exactly as the pre-migration code did.
			 *
			 * THIS MUST SIT BEFORE THE FIRST REPORT IS WRITTEN, not before the zip. Anchoring it on
			 * createReportsZip() let 4329 write 4329_TRANSACTION_REPORT.xlsx and
			 * 4329_OKASSETS_REPORT.xlsx after the abort was already detected. On Java 8 stop() killed
			 * the thread here, so no report existed at all.
			 */
			if(isScheduleAborted()) { return; }
			printUtils.printTransactionReport(contentDetails, String.valueOf(schDetails.getScheduleId()));
			if(null!=xcopyUtils.getFileProcessingList() && xcopyUtils.getFileProcessingList().size()>0)
			{
				printUtils.printFileProcessingReport(xcopyUtils.getFileProcessingList(), String.valueOf(schDetails.getScheduleId()));
			}

			/*
			 * NOW PROCEED FOR CREATING ZIP FILE
			 */
			String reportsDirPath = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add reports folder
			reportsDirPath+=ApplicationProperties.getProperty("sichannel.data.load.reports.dir");
			if(!reportsDirPath.endsWith("/"))
			{
				reportsDirPath=reportsDirPath+"/";
			}
			// Add schedule id Dir
			reportsDirPath+=String.valueOf(schDetails.getScheduleId())+"/";

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

			boolean reportsZipGenerated=printUtils.createReportsZip(reportsDirPath, String.valueOf(schDetails.getScheduleId()));
			if(reportsZipGenerated==true)
			{
				logger.info("startProcessing_WithZipFile :: Reports Zipped Successfully.");
			}
			else
			{
				logger.info("startProcessing_WithZipFile :: Failed to Zip Reports. Please contact IT Admin Support.Update Schedule Status as Failure");
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Failed to Zip Generated Reports");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
			}
			reportsDirPath = null;

			/*
			 *  update other Schedule Details & completion status
			 *  set scheduleStatus as well before moving to DB
			 */
			schDetails.setScheduleStatus(contentDetails.getProcessingStatus());
			SIChannelDataLoadDAO.updateScheduleCompletionDetails(schDetails, contentDetails.getErrorsList());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startProcessing_WithZipFile()", e);
		}
		contentDetails = null;
		zipFilePath = null;
		errorDetails = null;
		printUtils = null;
		xcopyUtils = null;
		// SEND EMAIL NOTIFICATION 
		sendEmailNotification(String.valueOf(schDetails.getScheduleId()), schDetails.getScheduleStatus(), schDetails.getScheduleName(), schDetails.getWslId());
				
	}


	private boolean convertWordToHtml(byte[] data,String scheduleId,String extension) 
	{
		try 
		{
			// also here write word file to temp directory
			// path will be temp physical directory/scheduleId/content/scheduleId_content.docx
			String path = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			// append scheduleId folder to the path
			if(!path.endsWith("/"))
			{
				path+="/";
			}
			path+=scheduleId+"/";
			// create schedule directory
			File dir = new File(path);
			if(!dir.exists() || !dir.isDirectory())
			{
				// make new directory
				dir.mkdir();
			}
			dir = null;

			// append content folder to path
			path+="Content"+"/";

			// create content directory
			dir = new File(path);
			if(!dir.exists() || !dir.isDirectory())
			{
				// make new directory
				dir.mkdir();
			}
			dir = null;
			// Write word file to this location
			FileReadWriteUtil.writeFile(data, path+scheduleId+"_Content."+extension);

			/*
			 * NOW START TRANSFORMING WORD FILE INTO HTML FILE
			 */
			// 1) Load DOCX into XWPFDocument
			InputStream doc = new ByteArrayInputStream(data);
			XWPFDocument document = new XWPFDocument(doc);
			// 2) Prepare XHTML options (here we set the IURIResolver to load images from a "word/media" folder)
			XHTMLOptions options = XHTMLOptions.create(); //.URIResolver(new FileURIResolver(new File("word/media")));;
			// Extract image
			File imageFolder = new File( path+scheduleId+"_Content_Files" );
			options.setExtractor( new FileImageExtractor( imageFolder ) );
			// URI resolver
			options.URIResolver( new FileURIResolver( imageFolder ) );
			OutputStream out = new FileOutputStream(new File(path+scheduleId+"_Content"+".html"));
			XHTMLConverter.getInstance().convert(document, out, options);

			out.close();out = null;
			imageFolder = null;
			dir = null;
			path = null;
			options = null;
			document.close();document = null;
			doc.close();doc = null;	
		} 
		catch (FileNotFoundException ex) 
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "convertWordToHtml()", ex);
			return false;
		}
		catch (IOException ex) 
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "convertWordToHtml()", ex);
			return false;
		}
		catch(Exception ex)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "convertWordToHtml()", ex);
			return false;
		}
		return true;
	}


	private boolean writeFileToWorkingDir(byte[] data, String scheduleId)
	{
		// UPDATE PROCESSING STATUS TO FILE UPLOAD
		SIChannelDataLoadDAO.updateScheduleProcessingStatus("Uploading File", new Long(scheduleId).longValue());
		boolean bool = false;
		try
		{
			zipFilePath = ApplicationProperties.getProperty("sichannel.data.load.zip.file.directory.physical.path");
			// append scheduleId folder to the path
			if(!zipFilePath.endsWith("/"))
			{
				zipFilePath+="/";
			}
			zipFilePath+=scheduleId;
			// create schedule directory
			File dir = new File(zipFilePath);
			if(!dir.exists() || !dir.isDirectory())
			{
				// make new directory
				dir.mkdir();
			}
			// append temp zip file name to the file
			zipFilePath+="/"+scheduleId+"_SIContent.zip";
			// Now write Byte Array Data to the File
			bool = FileReadWriteUtil.writeFile(data, zipFilePath);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "writeFileToWorkingDir()", e);
			bool= false;
			// set zipFilePath to null;
			zipFilePath = null;
		}
		return bool;
	}

	private void startReadingContent_FromWord(String directoryPath, SIChannelScheduleDetails schDetails)
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			logger.info("startReadingContent_FromWord :: directoryPath :: > "+ directoryPath);
			File dir = new File(directoryPath);
			if(dir.exists() && dir.isDirectory())
			{
				File[] listFiles= dir.listFiles();
				if(null!=listFiles && listFiles.length>0)
				{
					File htmlFile = null;
					for(int a=0;a<listFiles.length;a++)
					{
						if(isScheduleAborted()) { break; }
						if(listFiles[a].exists() && listFiles[a].isFile() && 
								(listFiles[a].getName().toLowerCase().endsWith(ApplicationProperties.getProperty("sichannel.data.load.content.file.html.ext"))) || 
								(listFiles[a].getName().toLowerCase().endsWith(ApplicationProperties.getProperty("sichannel.data.load.content.file.htm.ext"))))
						{
							// File is either .html or .htm
							htmlFile= listFiles[a];
							break;
						}
					}

					if(null!=htmlFile && htmlFile.exists() && htmlFile.isFile())
					{
						logger.info("startReadingContent_FromWord :: Source Content HTML File ("+htmlFile.getName()+") found inside Uploaded directory. Proceed for reading Schema Field Values.");
						/*
						 * READ SCHEMA FIELD VALUES AND IDENTUFY IMAGES INSIDE EACH OF THEM
						 */
						contentDetails = ReadContentUtils.getSIChannelSchemaDetails(contentDetails, htmlFile, schDetails.getLocale(),String.valueOf(schDetails.getScheduleId()));
						if(null!=contentDetails)
						{
							/*
							 * PROCEED FOR CREATING / MODOFYING DOCUMENT IN KAPTURE
							 */
							if(null!=contentDetails.getDocumentId() && !"".equals(contentDetails.getDocumentId()))
							{
								logger.info("startReadingContent_FromWord :: Proceed for Modifying Existing Document ("+schDetails.getDocumentId()+") Of Locale :: >"+ schDetails.getLocale()+" For Content HTML File :: >"+ htmlFile.getName());
								/*
								 * PROCEED FOR MODIFICATION OF EXISTING DOCUMENT
								 */
								modifyContent();
								if(null!=contentDetails && null!=contentDetails.getContentId() && !"".equals(contentDetails.getContentId()))
								{
									logger.info("startReadingContent_FromWord :: Document Modified Successfully in Kapture for Existing Document  :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Proceed for OKAssets Operation, if any.");
									/*
									 * PROCEED FOR PERFORMING OKASSETS OPERATION
									 */
									if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
									{
										xcopyUtils.copyFilesToServer(contentDetails);
									}
								}
								else
								{
									/*
									 * TRACK ERROR
									 */
									errorDetails = new SIChannelErrorsDetails();
									errorDetails.setErrorMessage("Failed to modify Document in Kapture. Please refer to the Transaction Report.");
									if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
									{
										contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
									}
									contentDetails.getErrorsList().add(errorDetails);
									errorDetails = null;
									// set Processing Details as NULL
									contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
									logger.info("startReadingContent_FromWord :: Failed to Modidy Document in Kapture for Existing Document  :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Update Schedule Status as Failure.");
								}
							}
							else
							{
								logger.info("startReadingContent_FromWord :: Proceed for Creating New Document Of Locale :: >"+ schDetails.getLocale()+" For Content HTML File :: >"+ htmlFile.getName());
								/*
								 * PROCEED FOR CREATION OF NEW DOCUMENT
								 */
								createContent(htmlFile);
								if(null!=contentDetails && null!=contentDetails.getContentId() && !"".equals(contentDetails.getContentId()))
								{
									logger.info("startReadingContent_FromWord :: Document Created Successfully in Kapture for "+htmlFile.getName()+", generated Document Id :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Proceed for OKAssets Operation, if any.");

									/*
									 * PROCEED FOR PEROFRMING OKASSETS OPERATION
									 */
									if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
									{
										xcopyUtils.copyFilesToServer(contentDetails);
									}
								}
								else
								{
									/*
									 * TRACK ERROR
									 */
									errorDetails = new SIChannelErrorsDetails();
									errorDetails.setErrorMessage("Failed to create Document in Kapture. Please refer to the Transaction Report.");
									if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
									{
										contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
									}
									contentDetails.getErrorsList().add(errorDetails);
									errorDetails = null;
									// set Processing Details as NULL
									contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
									logger.info("startReadingContent_FromWord :: Failed to Create Document in Kapture for HTML File  :: >"+htmlFile.getName()+" of Locale :: >"+schDetails.getLocale()+". Update Schedule Status as Failure.");
								}
							}
						}
						else
						{
							/*
							 * TRACK ERROR
							 */
							errorDetails = new SIChannelErrorsDetails();
							errorDetails.setErrorMessage("Failed to parse and read source content from the HTML File for the schema fields.");
							if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
							{
								contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
							}
							contentDetails.getErrorsList().add(errorDetails);
							errorDetails = null;
							// set Processing Details as NULL
							contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							logger.info("startReadingContent_FromWord :: Failed to Parse Source Content HTML File at Path :: >"+htmlFile.getAbsolutePath()+". Update Schedule Status as Failure.");
						}
					}
					else
					{
						/*
						 * TRACK ERROR
						 */
						errorDetails = new SIChannelErrorsDetails();
						errorDetails.setErrorMessage("No Source Content HTML File Found, Uploaded Word File got failed to get Transformed to HTML.");
						if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
						{
							contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
						}
						contentDetails.getErrorsList().add(errorDetails);
						errorDetails = null;
						// set Processing Details as NULL
						contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

						logger.info("startReadingContent_FromWord :: No Source Content HTML found inside Destination Directory at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
					}
					htmlFile=  null;
				}
				else
				{
					/*
					 * TRACK ERROR
					 */
					errorDetails = new SIChannelErrorsDetails();
					errorDetails.setErrorMessage("No Content Files found for the Uploaded File. Uploaded Word File got failed to get Transformed to HTML.");
					if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
					{
						contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
					}
					contentDetails.getErrorsList().add(errorDetails);
					errorDetails = null;
					// set Processing Details as NULL
					contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
					logger.info("startReadingContent_FromWord :: No Files found inside Destination Directory at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
				}
				listFiles=  null;
			}
			else
			{
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Failed to read Content Files directory for the Uploaded Word File.");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

				logger.info("startReadingContent_FromWord :: Content Destination Directory does not exist at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
			}
			dir = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startReadingContent_FromWord()", e);

			/*
			 * TRACK ERROR
			 */
			errorDetails = new SIChannelErrorsDetails();
			errorDetails.setErrorMessage(e.getMessage()+" Exception Occured.");
			if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
			{
				contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
			}
			contentDetails.getErrorsList().add(errorDetails);
			errorDetails = null;
			// set Processing Details as NULL
			contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

			logger.info("startReadingContent_FromWord :: Some Exception. Update Schedule Status as Failure for Reason :: > "+ e.getMessage());
		}
		finally
		{
			directoryPath = null;
		}
	}

	private void startReadingContent_FromZip(String directoryPath, SIChannelScheduleDetails schDetails)
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			File dir = new File(directoryPath);
			if(dir.exists() && dir.isDirectory())
			{
				File[] listFiles= dir.listFiles();
				if(null!=listFiles && listFiles.length>0)
				{
					File htmlFile = null;
					for(int a=0;a<listFiles.length;a++)
					{
						if(isScheduleAborted()) { break; }
						if(listFiles[a].exists() && listFiles[a].isFile() && 
								(listFiles[a].getName().toLowerCase().endsWith(ApplicationProperties.getProperty("sichannel.data.load.content.file.html.ext"))) || 
								(listFiles[a].getName().toLowerCase().endsWith(ApplicationProperties.getProperty("sichannel.data.load.content.file.htm.ext"))))
						{
							// File is either .html or .htm
							htmlFile= listFiles[a];
							break;
						}
					}

					if(null!=htmlFile && htmlFile.exists() && htmlFile.isFile())
					{
						logger.info("startReadingContent_FromZip :: Source Content HTML File ("+htmlFile.getName()+") found inside Unzipped directory. Proceed for reading Schema Field Values.");
						/*
						 * READ SCHEMA FIELD VALUES AND IDENTUFY IMAGES INSIDE EACH OF THEM
						 */
						contentDetails = ReadContentUtils.getSIChannelSchemaDetails(contentDetails, htmlFile, schDetails.getLocale(),String.valueOf(schDetails.getScheduleId()));
						if(null!=contentDetails)
						{
							/*
							 * PROCEED FOR CREATING / MODOFYING DOCUMENT IN KAPTURE
							 */
							if(null!=contentDetails.getDocumentId() && !"".equals(contentDetails.getDocumentId()))
							{
								logger.info("startReadingContent_FromZip :: Proceed for Modifying Existing Document ("+schDetails.getDocumentId()+") Of Locale :: >"+ schDetails.getLocale()+" For Content HTML File :: >"+ htmlFile.getName());
								/*
								 * PROCEED FOR MODIFICATION OF EXISTING DOCUMENT
								 */
								modifyContent();
								if(null!=contentDetails && null!=contentDetails.getContentId() && !"".equals(contentDetails.getContentId()))
								{
									logger.info("startReadingContent_FromZip :: Document Modified Successfully in Kapture for Existing Document  :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Proceed for OKAssets Operation, if any.");
									/*
									 * PROCEED FOR PERFORMING OKASSETS OPERATION
									 */
									if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
									{
										xcopyUtils.copyFilesToServer(contentDetails);
									}
								}
								else
								{
									/*
									 * TRACK ERROR
									 */
									errorDetails = new SIChannelErrorsDetails();
									errorDetails.setErrorMessage("Failed to modify Document in Kapture. Please refer to the Transaction Report.");
									if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
									{
										contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
									}
									contentDetails.getErrorsList().add(errorDetails);
									errorDetails = null;
									// set Processing Details as NULL
									contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
									logger.info("startReadingContent_FromZip :: Failed to Modidy Document in Kapture for Existing Document  :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Update Schedule Status as Failure.");
								}
							}
							else
							{
								logger.info("startReadingContent_FromZip :: Proceed for Creating New Document Of Locale :: >"+ schDetails.getLocale()+" For Content HTML File :: >"+ htmlFile.getName());
								/*
								 * PROCEED FOR CREATION OF NEW DOCUMENT
								 */
								createContent(htmlFile);
								if(null!=contentDetails && null!=contentDetails.getContentId() && !"".equals(contentDetails.getContentId()))
								{
									logger.info("startReadingContent_FromZip :: Document Created Successfully in Kapture for "+htmlFile.getName()+", generated Document Id :: >"+schDetails.getDocumentId()+" of Locale :: >"+schDetails.getLocale()+". Proceed for OKAssets Operation, if any.");

									/*
									 * PROCEED FOR PEROFRMING OKASSETS OPERATION
									 */
									if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
									{
										xcopyUtils.copyFilesToServer(contentDetails);
									}
								}
								else
								{
									/*
									 * TRACK ERROR
									 */
									errorDetails = new SIChannelErrorsDetails();
									errorDetails.setErrorMessage("Failed to create Document in Kapture. Please refer to the Transaction Report.");
									if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
									{
										contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
									}
									contentDetails.getErrorsList().add(errorDetails);
									errorDetails = null;
									// set Processing Details as NULL
									contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
									logger.info("startReadingContent_FromZip :: Failed to Create Document in Kapture for HTML File  :: >"+htmlFile.getName()+" of Locale :: >"+schDetails.getLocale()+". Update Schedule Status as Failure.");
								}
							}
						}
						else
						{
							/*
							 * TRACK ERROR
							 */
							errorDetails = new SIChannelErrorsDetails();
							errorDetails.setErrorMessage("Failed to parse and read source content from the HTML File for the schema fields.");
							if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
							{
								contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
							}
							contentDetails.getErrorsList().add(errorDetails);
							errorDetails = null;
							// set Processing Details as NULL
							contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							logger.info("startReadingContent_FromZip :: Failed to Parse Source Content HTML File at Path :: >"+htmlFile.getAbsolutePath()+". Update Schedule Status as Failure.");
						}
					}
					else
					{
						/*
						 * TRACK ERROR
						 */
						errorDetails = new SIChannelErrorsDetails();
						errorDetails.setErrorMessage("Uploaded Zip file does not contain any Source Content HTML File.");
						if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
						{
							contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
						}
						contentDetails.getErrorsList().add(errorDetails);
						errorDetails = null;
						// set Processing Details as NULL
						contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

						logger.info("startReadingContent_FromZip :: No Source Content HTML found inside Destination Directory at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
					}
					htmlFile=  null;
				}
				else
				{
					/*
					 * TRACK ERROR
					 */
					errorDetails = new SIChannelErrorsDetails();
					errorDetails.setErrorMessage("No Content Files found inside the Unzipped directory.");
					if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
					{
						contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
					}
					contentDetails.getErrorsList().add(errorDetails);
					errorDetails = null;
					// set Processing Details as NULL
					contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
					logger.info("startReadingContent_FromZip :: No Files found inside Destination Directory at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
				}
				listFiles=  null;
			}
			else
			{
				/*
				 * TRACK ERROR
				 */
				errorDetails = new SIChannelErrorsDetails();
				errorDetails.setErrorMessage("Failed to read Unzip File directory.");
				if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
				{
					contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
				}
				contentDetails.getErrorsList().add(errorDetails);
				errorDetails = null;
				// set Processing Details as NULL
				contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

				logger.info("startReadingContent_FromZip :: Unzip Destination Directory does not exist at Path :: >"+directoryPath+". Update Schedule Status as Failure.");
			}
			dir = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "startReadingContent_FromZip()", e);

			/*
			 * TRACK ERROR
			 */
			errorDetails = new SIChannelErrorsDetails();
			errorDetails.setErrorMessage(e.getMessage()+" Exception Occured.");
			if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
			{
				contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
			}
			contentDetails.getErrorsList().add(errorDetails);
			errorDetails = null;
			// set Processing Details as NULL
			contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);

			logger.info("startReadingContent_FromZip :: Some Exception. Update Schedule Status as Failure for Reason :: > "+ e.getMessage());
		}
		finally
		{
			directoryPath = null;
		}
	}

	/**
	 * UPDATES AN EXISTING DOCUMENT WITH THE CONTENT READ OUT OF THE WORD FILE.
	 *
	 * THIS SCREEN NEVER PUBLISHES - NOT ON UPDATE, NOT ON CREATE. A document that was live is left
	 * live at its previous version with the new content sitting in an unpublished draft, and
	 * somebody publishes it by hand after checking it.
	 *
	 * DELIBERATE, AND IT WAS DELIBERATE BEFORE THE MIGRATION TOO. The InfoManager call here was
	 * modifyContent(content, FALSE) - hardcoded - while the SI VIN screens in this same codebase
	 * passed modifyContent(content, content.getPublished()) to preserve the state. The two differ
	 * on purpose. SI_Import_Guidelines.pdf says the same thing from the user's side: "please
	 * verify the document in IM... User may require minor adjustment in imported content" - a
	 * review step that only exists because the import does not go live on its own.
	 *
	 * A re-publish was written and REMOVED on 2026-08-06 once that was established. Do not add it
	 * back without the same conversation.
	 *
	 * THE ENDPOINT DEPENDS ON WHAT THE DOCUMENT IS. A master identifier goes to update-article-tab;
	 * a translation goes to save-translate-article. Sending a translation through the master
	 * endpoint does not fail - it silently collapses the translation onto the master's locale - so
	 * the decision is taken from the read rather than assumed.
	 */
	private void modifyContent()
	{
		// ABORTED FROM THE SCREEN - do no further work.
		if(isScheduleAborted()) { return; }
		try
		{
			if(null==contentDetails)
			{
				return;
			}

			String locale = contentDetails.getLocale().replace("-", "_").trim();
			String documentId = contentDetails.getDocumentId();
			String context = "SI Data Load - {"+documentId+"} / {"+locale+"}";

			String contentId = FetchKaptureDataDAO.getSIChannelContentId();
			if(null==contentId || "".equals(contentId))
			{
				trackError("IMD00005", "The Service Information channel could not be identified in Kapture.");
				return;
			}

			/*
			 * READ FIRST - both to confirm the document is there and to get the version back that
			 * has to be echoed into the write. Kapture rewrites the whole row on an update, so the
			 * payload is the read with only the content replaced.
			 */
			SIKaptureDocumentDetails current = SIChannelKaptureService.getDocument(documentId, locale, context);
			if(current.isReadFailed()==true)
			{
				logger.info("modifyContent :: Could not read {"+documentId+"} / {"+locale+"} from Kapture :: >"+ current.getErrorMessage());
				trackError("IMD00001", "Failed to Fetch Content from Kapture. "+ current.getErrorMessage());
				return;
			}
			if(current.isFound()==false)
			{
				logger.info("modifyContent :: {"+documentId+"} has no version in {"+locale+"}.");
				trackError("IMD00001", "Failed to Fetch Content from Kapture");
				return;
			}

			// SET FETCHED VERSION
			contentDetails.setFetchedVersion(current.getArticleVersion());

			/*
			 * CHECKED OUT - REFUSED, exactly as it was under InfoManager. The screen tests this
			 * before scheduling too, but a document can be checked out in between.
			 */
			if(FetchKaptureDataDAO.isDocumentCheckedOut(documentId)==true)
			{
				logger.info("modifyContent :: {"+documentId+"} is Checked Out in Kapture. Cannot be modified.");
				trackError("IMD00002", "Document is checked out in Kapture. Cannot be modified.");
				return;
			}

			/*
			 * id AND articleVersion COME FROM k_article, NEVER FROM THE READ - latest-article
			 * carries no id and returns the version as a NUMBER, so 1.0 would go back as "1" and
			 * address a different version.
			 */
			KaptureArticleIdentity identity = FetchKaptureDataDAO.getArticleIdentity(documentId, locale);
			if(identity.isUsable()==false)
			{
				logger.info("modifyContent :: no k_article row for {"+documentId+"} / {"+locale+"} - cannot update.");
				trackError("IMD00006", "The document version to update could not be identified.");
				return;
			}

			KaptureApiClient client = new KaptureApiClient();
			if(client.login()==false)
			{
				trackError("IMD00007", "Could not authenticate with Kapture.");
				return;
			}

			JsonObject article = current.getArticle();
			String payload = null;
			KaptureApiResult result = null;
			if(current.isTranslation()==true)
			{
				logger.info("modifyContent :: UPDATE "+ current.describe()+" via save-translate-article.");
				payload = SIChannelPayloadBuilder.buildTranslationUpdate(article, contentDetails,
						identity, documentId, contentId, locale, identity.getMasterLocale());
				result = client.saveTranslateArticle(contentId, payload, context);
			}
			else
			{
				logger.info("modifyContent :: UPDATE "+ current.describe()+" via update-article-tab.");
				payload = SIChannelPayloadBuilder.buildUpdate(article, contentDetails, identity,
						documentId, locale, client);
				result = client.updateArticle(contentId, payload, context);
			}

			applySaveResult(result, contentId, context, "modifyContent", "IMD00003",
					"Failed to Modify Document In Kapture.");
		}
		catch (Exception e)
		{
			logger.info("modifyContent :: otherErrors :: Error Code :: >" + e.getMessage());
			trackError(e.getMessage(), stackTraceOf(e));
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "modifyContent()", e);
		}
	}

	/**
	 * CREATES A NEW DOCUMENT FROM THE CONTENT READ OUT OF THE WORD FILE.
	 *
	 * ALWAYS THE MASTER IDENTIFIER FOR THE SELECTED LOCALE - a translation is never created here;
	 * it is made by writing to save-translate-article against a master that already exists.
	 *
	 * THE DOCUMENT ID IS KAPTURE'S TO ALLOCATE. It comes from k_content_type (documentid_prefix +
	 * documentid), so it is learned from the response and cannot be predicted or requested.
	 */
	private void createContent( File htmlFile)
	{
		try
		{
			if(null==contentDetails)
			{
				return;
			}

			String locale = contentDetails.getLocale().replace("-", "_").trim();
			String context = "SI Data Load - new document for {"+locale+"} from {"+htmlFile.getName()+"}";

			String contentId = FetchKaptureDataDAO.getSIChannelContentId();
			if(null==contentId || "".equals(contentId))
			{
				trackError("IMD00005", "The Service Information channel could not be identified in Kapture.");
				return;
			}

			KaptureApiClient client = new KaptureApiClient();
			/*
			 * LOGGED IN BEFORE THE PAYLOAD IS BUILT, NOT DURING THE CALL. The payload names the
			 * author and the owner, and those come from the account the token was issued for - so
			 * the profile has to exist before the JSON is written, not by the time it is sent.
			 */
			if(client.login()==false)
			{
				trackError("IMD00007", "Could not authenticate with Kapture.");
				return;
			}

			logger.info("createContent :: CREATE a new SERVICE_INFORMATION document for {"+locale+"} from {"+htmlFile.getName()+"}.");
			String payload = SIChannelPayloadBuilder.buildCreate(contentDetails, locale, client);
			KaptureApiResult result = client.createArticle(contentId, payload, context);

			applySaveResult(result, contentId, context, "createContent", "IMD00004",
					"FAILED TO GENERATE DOCUMENT IN KAPTURE.");
		}
		catch (Exception e)
		{
			logger.info("createContent :: otherErrors :: Error Code :: >" + e.getMessage());
			trackError(e.getMessage(), stackTraceOf(e));
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "createContent()", e);
		}
	}

	/**
	 * RECORDS THE OUTCOME OF A WRITE ON THE CONTENT VO - shared by create and update, which differ
	 * only in which endpoint produced the response.
	 *
	 * CONTENT ID IS THE SUCCESS FLAG. startReadingContent_FromWord() decides whether to go on to
	 * the image copy by testing contentDetails.getContentId(), so it is set HERE AND ONLY HERE, on
	 * a write that actually produced a document.
	 */
	private void applySaveResult(KaptureApiResult result, String contentId, String context,
			String caller, String errorCode, String errorMessage)
	{
		SIKaptureDocumentDetails saved = SIChannelKaptureService.readSaveResponse(result, context);
		if(saved.isFound()==true)
		{
			logger.info(caller+" :: SUCCESS "+ saved.describe());

			contentDetails.setDocumentId(saved.getDocumentId());
			contentDetails.setContentId(contentId);
			contentDetails.setModifiedVersion(saved.getArticleVersion());
			if(saved.isPublished()==true)
			{
				contentDetails.setPublishStatus(ScheduleConstants.STATUS_YES.substring(0,1));
			}
			else
			{
				contentDetails.setPublishStatus(ScheduleConstants.STATUS_NO.substring(0,1));
			}
			contentDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
		}
		else
		{
			logger.info(caller+" :: "+errorMessage+" :: >"+ saved.getErrorMessage());
			// EXPLICITLY LEFT NULL - it is what the caller tests to decide the write failed
			contentDetails.setContentId(null);
			trackError(errorCode, errorMessage
					+ (null==saved.getErrorMessage() ? "" : " "+saved.getErrorMessage()));
		}
	}

	/** Marks the run failed and adds one line to the transaction report. */
	private void trackError(String code, String message)
	{
		contentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
		errorDetails = new SIChannelErrorsDetails();
		errorDetails.setErrorCode(code);
		errorDetails.setErrorMessage(message);
		if(null==contentDetails.getErrorsList() || contentDetails.getErrorsList().size()<=0)
		{
			contentDetails.setErrorsList(new ArrayList<SIChannelErrorsDetails>());
		}
		contentDetails.getErrorsList().add(errorDetails);
		errorDetails = null;
	}

	private String stackTraceOf(Exception e)
	{
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		e.printStackTrace(print);
		return writer.toString();
	}

	private void sendEmailNotification(String scheduleId,String scheduleStatus,String scheduleName,String wslId)
	{
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=scheduleStatus && !"".equals(scheduleStatus) && null!=wslId && !"".equals(wslId))
			{
				String[] toEmailIds = null;
				/*
				 * Identify TO EMAIL IDs
				 * if INTERNAL USERS - GMS3_INTERNAL_TEAM_USERS=gbansal1,vdabkara,dsinghga,skoguru,achoragu
				 * 		THEN SEND EMAIL ON IDS ADDED IN PROPERTIES FILE - sichannel.data.load.email.id
				 * ELSE FETCH EMAIL ID ON WSL ID AND SEND EMAIL TO ID
				 */
				if(ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS").trim().toLowerCase().indexOf(wslId.trim().toLowerCase())!=-1)
				{
					// USER IS AN INTERNAL USER
					toEmailIds = ApplicationProperties.getProperty("sichannel.data.load.email.id").split(",");
				}
				else
				{
					/*
					 * THE ADDRESS COMES FROM KAPTURE'S k_user, NOT FROM OK_IM.USERINFORMATION.
					 *
					 * RMIExportTransactionDAO.getEmailId() still runs
					 * "SELECT EMAIL FROM OK_IM.USERINFORMATION" - a schema that does not exist under
					 * MySQL - so it threw "Unknown database 'ok_im'" on every call and NO EXTERNAL
					 * USER HAS EVER BEEN NOTIFIED by this screen since the migration. It failed
					 * quietly because the exception is swallowed and a null address is a legitimate
					 * "nobody to tell", so the run still reported Success.
					 *
					 * CategoryProcessingDAO.getUserEmailDetails() is the same lookup already
					 * migrated for the SI VIN screens - k_user.userid / email / user_status='Active'
					 * - and it opens its own CMS connection, which matters because the two schemas
					 * are not on the same server.
					 */
					CategoryProcessingDAO userDAO = new CategoryProcessingDAO();
					String emailid = userDAO.getUserEmailDetails(wslId);
					if(null!=emailid && !"".equals(emailid))
					{
						toEmailIds = emailid.split(",");
					}
					else
					{
						logger.info("sendEmailNotification :: Email Id retrieved for "+wslId+" is NULL. Email Notification cannot be sent.");
					}
					emailid = null;
					userDAO = null;
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
					String subject=  "SI Data Import Summary - "+ scheduleName;
					String body="Hi,<br/>The Service Information Data Import Job "+scheduleName+" has been finished";
					if(scheduleStatus.equals(ScheduleConstants.STATUS_SUCCESS))
					{
						body+=" Successfully.";
					}
					else 
					{
						body+=" with Errors.";
					}

					body+="<br/>";
					body+="Please Login to MDM for viewing more details.";

					body+="<br/><br/>Regards,<br/>GMS3 Application.<br/>Note - This is an auto generated email. Please do not respond to this email.";

					// send normal HTML Mail
					SendMailUsingAuthentication.newPostHTMLMail(toEmailsList, subject, body, fromEmailId);

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
			Utilities.printStackTraceToLogs(SIChannelDataLoadProcessingImpl.class.getName(), "sendEmailNotification()", e);
		}
	}

}
