package com.mazda.gms3.dmt.automation.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.mazda.gms3.dmt.automation.dao.AutomationIdentificationDAO;
import com.mazda.gms3.dmt.automation.utils.AutomationUtils;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.SendMailUsingAuthentication;
import com.mazda.gms3.dmt.utils.Utilities;

public class StartDocumentsIdentificationImpl {
	
	private Logger logger = LogManager.getLogger(StartDocumentsIdentificationImpl.class);

	public void startDocumentsIdentification(List<ExcelRowDetails> inputDataList, String scheduleId, String scheduleName)
	{
		String processingStatus=ApplicationProperties.getProperty("schedule.status.processing.value");
		String completedStatus = ApplicationProperties.getProperty("schedule.status.completed.value");
		String successStatus = ApplicationProperties.getProperty("schedule.status.success.value");
		String failureStatus=ApplicationProperties.getProperty("schedule.status.failure.value");
		String remarks=null;
		boolean sendCompletionEmail=false;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equalsIgnoreCase(scheduleId)  && null!=inputDataList && inputDataList.size()>0)
			{
				/*
				 * update Schedule status as Processing
				 * Do Nothing with Job status
				 * No operation on Child and SubChild for Schedule Id
				 */
				AutomationIdentificationDAO.updateScheduleStatus(scheduleId, processingStatus, null, false, null);
				
				/*
				 * PROCEED FOR FINDING IMPACTED DOCUMENTS MODEL WISE
				 */
				List<ItemDetails> automationList = AutomationUtils.identifyImpactedDocumentsListModelWiseForSchedule(inputDataList);
				
				if(null!=automationList && automationList.size()>0)
				{
					/*
					 * identify Total Impacted Documents Count from AutomationList
					 * Update totalDocsCounts / scheduleStatus = Completed / Job Status = Success
					 * 
					 * Populate ItemDetails in Child Table and Applicable VINs for each in SubChild Table
					 * 
					 * Iterate AutomationList and remove the items with Documents Count to 0
					 */
					ItemDetails details = null;
					for(int a=0;a<automationList.size();a++)
					{
						details = (ItemDetails)automationList.get(a);
						if(null==details.getTotalDocumentsCount() || details.getTotalDocumentsCount().longValue()==0)
						{
							automationList.remove(a);
							a--;
						}
						details = null;
					}
					details = null;
					
					if(null!=automationList && automationList.size()>0)
					{
						int totalCount=0;
						details = null;
						for(int a=0;a<automationList.size();a++)
						{
							details = (ItemDetails)automationList.get(a);
							if(null!=details.getTotalDocumentsCount() && details.getTotalDocumentsCount().longValue()>0)
							{
								totalCount = totalCount + details.getTotalDocumentsCount().intValue();
							}
							details = null;
						}
						details = null;
						logger.info("startDocumentsIdentification :: Total Impacted Documents Found are :: >"+ totalCount+". Proceed for Saving Item Details.");
						/*
						 * PROCEED FOR CALLING FUNCTION TO POPULATE
						 * Schedule Table = totalDocsCounts / scheduleStatus = Completed / Job Status = Success
						 * Item Details = Populate Impacted Model Wise Data
						 * ApplicableVIN Details = Populate All Application VIN Data for each Model Wise
						 */
						boolean bool = AutomationIdentificationDAO.updateScheduleItemDetails(scheduleId, totalCount, automationList, completedStatus, successStatus);
						if(bool==true)
						{
							logger.info("startDocumentsIdentification :: Documents Identiciation Job Completed.");
							// send Completion Email
							sendCompletionEmail = true;
						}
						else
						{
							logger.info("startDocumentsIdentification :: Failed to Complete Documents Identiciation Job. Proceed for failing, Cannot proceed further.");
							/*
							 * Update totalDocsCounts = 0 / scheduleStatus = Completed / Job Status = Failure
							 * Delete all entries from Child & SubChild for Schedule Id
							 */
							// set remarks
							remarks = "Failed to Completed Documents Identiciation Job. Proceed for failing, Cannot proceed further.";
							AutomationIdentificationDAO.updateScheduleStatus(scheduleId, completedStatus,failureStatus, false, remarks);
							// send Completion Email
							sendCompletionEmail = true;
						}
					}
					else
					{
						logger.info("startDocumentsIdentification :: No Impacted Documents Found for the Uploaded VINs. Finish Job with Status as SUCCESS");
						// set remarks
						remarks = "No Impacted Documents Found for the Uploaded VINs. Finish Job with Status as SUCCESS.";
						AutomationIdentificationDAO.updateScheduleStatus(scheduleId, completedStatus,successStatus, false, remarks);
						// send Completion Email
						sendCompletionEmail = true;
					}
					automationList  =null;
				}
				else
				{
					/*
					 * Update totalDocsCounts = 0 / scheduleStatus = Completed / Job Status = Failure
					 * Delete all entries from Child & SubChild for Schedule Id
					 */
					logger.info("startDocumentsIdentification ::  Failed to Preapre Unique Locales + Models + Channel Types List. Cannot proceed further.Return null");
					// set remarks
					remarks = "Failed to Preapre Unique Locales + Models + Channel Types List. Cannot proceed further.Return null.";
					AutomationIdentificationDAO.updateScheduleStatus(scheduleId, completedStatus,failureStatus, false, remarks);
					// send Completion Email
					sendCompletionEmail = true;
				}
				automationList = null;
			}
			else
			{
				logger.info("startDocumentsIdentification :: Schedule Id / Input Data List as VINs for Documents Identification is NULL. ");
				// set remarks
				remarks = "Schedule Id / Input Data List as VINs for Documents Identification is NULL.";
				/*
				 * update schedule status Completed
				 * Job Status as Failure
				 * Delete all entries from Child & SubChild for Schedule Id
				 */
				AutomationIdentificationDAO.updateScheduleStatus(scheduleId, completedStatus,failureStatus, false, remarks);
				// send Completion Email
				sendCompletionEmail = true;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartDocumentsIdentificationImpl.class.getName(), "startDocumentsIdentification()", e);
			// set remarks
			remarks  = e.getMessage();
			/*
			 * update schedule status Completed
			 * Update Job Status to Failure
			 * and Delete all its Child and SubChild Items
			 */
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equalsIgnoreCase(scheduleId))
			{
				AutomationIdentificationDAO.updateScheduleStatus(scheduleId, completedStatus,failureStatus, false, remarks);
				// send Completion Email
				sendCompletionEmail = true;
			}
		}
		finally
		{
			processingStatus = null;
			completedStatus = null;
			successStatus = null;
			failureStatus = null;
			inputDataList = null;
		}
		
		if(sendCompletionEmail == true)
		{
			/*
			 * PROCEED FOR SENDING EMAILS
			 */
			ArrayList<String> toList = new ArrayList<String>();
			String userId = ApplicationProperties.getProperty("NOTIF_EMAIL_TO_ID");
			if(null!=userId && !"".equals(userId))
			{
				String[] tok = userId.split(",");
				if(null!=tok && tok.length>0)
				{
					for(int c=0;c<tok.length;c++)
					{
						toList.add(tok[c].trim().toString());
					}
				}
				tok = null;
			}
			String fromEmailId=ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID");
			String subject="FACELIFT - DOCUMENTS IDETIFICATION JOB - "+ scheduleName+" - COMPLETED";
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
			StringBuilder str = new StringBuilder();
			str.append("<p>Dear User,</p>");
			str.append("<p>FaceLift Documents Identification Job <b>"+scheduleName+"</b> has been Completed at "+sdf.format(new Date())+".</p>");
			str.append("<p>For more details please login to DMT Application.</p>");
			str.append("<p>Regards,<br />MGSS Application.<br />Note - This is an auto generated email. Please do not respond to this email.</p>");
			
			SendMailUsingAuthentication.newPostHTMLMail(toList, subject, str.toString(), fromEmailId);
			
			toList = null;
			fromEmailId = null;
			subject = null;
			str = null;
			sdf = null;
			userId = null;
		}
		
		if(null!=scheduleId && !"".equals(scheduleId) && !"0".equalsIgnoreCase(scheduleId))
		{
			stopActiveThead(scheduleId);
		}
		scheduleId = null;
		scheduleName  =null;
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
			String threadId=ApplicationProperties.getProperty("automation.identification.sch.key")+scheduleCode;
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
								Utilities.printStackTraceToLogs(StartDocumentsIdentificationImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartDocumentsIdentificationImpl.class.getName(), "stopActiveThead()", e);
		}
	}
	
	

}
