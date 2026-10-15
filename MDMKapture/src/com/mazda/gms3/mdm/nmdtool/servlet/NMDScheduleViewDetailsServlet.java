package com.mazda.gms3.mdm.nmdtool.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.nmdtool.dao.NMDScheduleDAO;
import com.mazda.gms3.mdm.nmdtool.vo.NMDJobDetails;
import com.mazda.gms3.mdm.nmdtool.vo.NMDScheduleDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.GenerateFinalResponse;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

/**
 * Servlet implementation class NMDScheduleViewDetailsServlet
 */
public class NMDScheduleViewDetailsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static Logger logger = LogManager.getLogger(NMDScheduleViewDetailsServlet.class);
	
	static MessageProperties msgProps = null;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NMDScheduleViewDetailsServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try
		{
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			if(null!=request.getParameter("NMDSCHHISTORY_VIEW_ATTEMPT_DETAILS") && !"".equals(request.getParameter("NMDSCHHISTORY_VIEW_ATTEMPT_DETAILS")))
			{
				performViewAttemptDetailsOperation(request, response);
			}
			else if(null!=request.getParameter("NMDSCHHISTORY_VIEW_SUMMARY_DETAILS") && !"".equals(request.getParameter("NMDSCHHISTORY_VIEW_SUMMARY_DETAILS")))
			{
				performViewSummaryDetailsOperation(request, response);
			}
			else if(null!=request.getParameter("NMDSCHHISTORY_VIEW_TRANSFER_SUMMARY_DETAILS") && !"".equals(request.getParameter("NMDSCHHISTORY_VIEW_TRANSFER_SUMMARY_DETAILS")))
			{
				performViewTransferSummaryDetailsOperation(request, response);
			}
			else if(null!=request.getParameter("NMDSCHHISTORY_VIEW_SYSTEM_ERROR") && !"".equals(request.getParameter("NMDSCHHISTORY_VIEW_SYSTEM_ERROR")))
			{
				performViewSystemErrorDetailsOperation(request, response);
			}
			else if(null!=request.getParameter("NMDSCHHISTORY_VIEW_REPORTS") && !"".equals(request.getParameter("NMDSCHHISTORY_VIEW_REPORTS")))
			{
				performViewReportsOperation(request, response);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "doPost()", e);
		}
	}
	
	private static void performViewAttemptDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE") && !"".equals(request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE");
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				List<NMDJobDetails> itemsList = NMDScheduleDAO.getAttemptsList(scheduleCode);
				if(null!=itemsList && itemsList.size()>0)
				{
					String data="";
					
					data=data+"<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data=data+"<thead>";
					data=data+"<tr>";
					data=data+"<th scope=\\\"col\\\" width=\\\"3%\\\">#.</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">"+msgProps.getProperty("label.locale")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"8%\\\">"+msgProps.getProperty("label.model")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.scheduletime")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.finishtime")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.documents")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.success")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.innerlinks")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.success")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.okassets")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.success")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.actual.delete")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.success")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.transfer")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.success")+" / "+msgProps.getProperty("label.skipped")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.job")+" "+msgProps.getProperty("label.running.progress")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.data")+" "+msgProps.getProperty("label.status")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.failurereason")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">"+msgProps.getProperty("label.attempt")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">"+msgProps.getProperty("label.download")+" "+msgProps.getProperty("label.reports")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">"+msgProps.getProperty("label.job")+" "+msgProps.getProperty("label.summary")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">"+msgProps.getProperty("label.remarks")+"</th>";
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					StringBuilder rowsData=new StringBuilder();
					NMDJobDetails itemDetails = null;
					SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
					for(int i=0;i<itemsList.size();i++)
					{
						itemDetails = (NMDJobDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+itemDetails.getSrNo()+"</td>");
						rowsData.append("<td>"+itemDetails.getLocale()+"</td>");
						rowsData.append("<td>"+itemDetails.getModel()+"</td>");
						if(null!=itemDetails.getStartTime())
						{
							rowsData.append("<td>"+sdf.format(itemDetails.getStartTime())+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						if(null!=itemDetails.getEndTime())
						{
							rowsData.append("<td>"+sdf.format(itemDetails.getEndTime())+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						
						rowsData.append("<td>"+itemDetails.getTotalDocs()+" / "+itemDetails.getSuccessDoc()+ " / "+itemDetails.getFailureDocs()+"</td>");
						rowsData.append("<td>"+itemDetails.getTotalInnerLinks()+" / "+itemDetails.getSuccessInnerLinks()+ " / "+itemDetails.getFailureInnnerLinks()+"</td>");
						rowsData.append("<td>"+itemDetails.getTotalOkAssets()+" / "+itemDetails.getSuccessOkAssets()+ " / "+itemDetails.getFailureOkAssets()+"</td>");
						rowsData.append("<td>"+itemDetails.getTotalDelDocs()+" / "+itemDetails.getSuccessDelDoc()+ " / "+itemDetails.getFailureDelDocs()+"</td>");
						rowsData.append("<td><a href=\\\"javascript:void(0);\\\" onclick=\\\"nmdschhistory_viewTransferSummary('"+itemDetails.getJobId()+"','"+itemDetails.getAttemptId()+"');\\\">"+itemDetails.getTotalTransferDocs()+" / "+itemDetails.getSuccessTransferDoc()+ " / "+itemDetails.getSkippedTranferDocs()+" / "+itemDetails.getFailureTransferDocs()+"</a></td>");
						if(null!=itemDetails.getRunningStatus() && !"".equals(itemDetails.getRunningStatus()))
						{
							rowsData.append("<td>"+itemDetails.getRunningStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						if(null!=itemDetails.getJobStatus() && !"".equals(itemDetails.getJobStatus()))
						{
							rowsData.append("<td>"+itemDetails.getJobStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						if(null!=itemDetails.getFailureReason() && !"".equals(itemDetails.getFailureReason()))
						{
							rowsData.append("<td>"+itemDetails.getFailureReason()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						
						rowsData.append("<td>"+itemDetails.getAttemptsCount()+"</td>");
						if(null!=itemDetails.getReportsPath() && !"".equals(itemDetails.getReportsPath()))
						{
							rowsData.append("<td><a href=\\\"javascript:void(0);\\\" onclick=\\\"nmdschhistory_viewReport('"+itemDetails.getJobId()+"','"+itemDetails.getAttemptId()+"');\\\"  style=\\\"text-decoration:none;\\\"><i class=\\\"downloadReports\\\"></i></a></td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						
						if(null!=itemDetails.getRunningStatus() && (itemDetails.getRunningStatus().equals(ScheduleConstants.STATUS_COMPLETED) || itemDetails.getRunningStatus().equals(ScheduleConstants.STATUS_ABORTED)))
						{
							// show Summary
							rowsData.append("<td><a href=\\\"javascript:void(0)\\\"  style=\\\"text-decoration:none;\\\" onclick=\\\"nmdschhistory_viewSummary('"+itemDetails.getJobId()+"','"+itemDetails.getAttemptId()+"');\\\"><i class=\\\"summaryIcon\\\"></i></a></td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						
						if(null!=itemDetails.getFailureReason() && itemDetails.getFailureReason().equals(ScheduleConstants.JOB_FAILURE_SYSTEM))
						{
							// show Remarks
							rowsData.append("<td><a href=\\\"javascript:void(0)\\\"  style=\\\"text-decoration:none;\\\" onclick=\\\"nmdschhistory_viewSystemError('"+itemDetails.getJobId()+"','"+itemDetails.getAttemptId()+"');\\\"><i class=\\\"commentsIcon\\\"></i></a></td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						rowsData.append("</tr>");
						itemDetails  = null;
					}
					if(null!=rowsData)
					{
						data =data+rowsData.toString();
					}
					data=data+"</tbody>";
					data=data+"</table>";	
					data=data+"</td>";
					data=data+"</tr>";
					data=data+"</table>";

					token="DATA";
					responseString = data;
					data = null;
					rowsData= null;
				}
				else
				{
					token="DATA";
					responseString = "<p>";
					responseString = responseString
							+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewAttemptDetailsOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}


	private static void performViewSummaryDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE") && !"".equals(request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE");
				String attemptCode = null;
				if (null != request.getParameter("NMDSCHHISTORY_ATTEMPT_ID") && !"".equals(request.getParameter("NMDSCHHISTORY_ATTEMPT_ID")))
				{
					attemptCode =  (String) request.getParameter("NMDSCHHISTORY_ATTEMPT_ID");
				}
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				ArrayList<DataExportToolReportSummaryDetails> itemsList = NMDScheduleDAO.getJobSummaryDetails(scheduleCode,attemptCode);
				if(null!=itemsList && itemsList.size()>0)
				{
					String data="";
					
					data=data+"<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data=data+"<thead>";
					data=data+"<tr>";
					data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\">#.</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"25%\\\" class=\\\"codeClass\\\">"+msgProps.getProperty("label.reports")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"15%\\\">"+msgProps.getProperty("label.status")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.total.uppercase")+" "+msgProps.getProperty("label.transactions")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.success")+" "+msgProps.getProperty("label.transactions")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"19%\\\">"+msgProps.getProperty("label.failure")+" "+msgProps.getProperty("label.transactions")+"</th>";
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					
					StringBuilder rowsData=new StringBuilder();
					DataExportToolReportSummaryDetails itemDetails = null;
					for(int i=0;i<itemsList.size();i++)
					{
						itemDetails = (DataExportToolReportSummaryDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+itemDetails.getSrNo()+"</td>");
						rowsData.append("<td>"+itemDetails.getReportName()+"</td>");
						rowsData.append("<td>"+itemDetails.getReportStatus()+"</td>");
						if(itemDetails.getTotalCount()>0)
						{
							rowsData.append("<td>"+itemDetails.getTotalCount()+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						if(itemDetails.getSuccessCount()>0)
						{
							rowsData.append("<td>"+itemDetails.getSuccessCount()+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						if(itemDetails.getFailureCount()>0)
						{
							rowsData.append("<td>"+itemDetails.getFailureCount()+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						rowsData.append("</tr>");
						itemDetails  = null;
					}
					if(null!=rowsData)
					{
						data =data+rowsData.toString();
					}
					data=data+"</tbody>";
					data=data+"</table>";	
					data=data+"</td>";
					data=data+"</tr>";
					data=data+"</table>";

					token="DATA";
					responseString = data;
					data = null;
					rowsData= null;
				}
				else
				{
					token="DATA";
					responseString = "<p>";
					responseString = responseString
							+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewSummaryDetailsOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}

	private static void performViewTransferSummaryDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE") && !"".equals(request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE");
				String attemptCode = null;
				if (null != request.getParameter("NMDSCHHISTORY_ATTEMPT_ID") && !"".equals(request.getParameter("NMDSCHHISTORY_ATTEMPT_ID")))
				{
					attemptCode =  (String) request.getParameter("NMDSCHHISTORY_ATTEMPT_ID");
				}
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				NMDJobDetails details = NMDScheduleDAO.getTransferSummaryDetails(scheduleCode, attemptCode);
				if(null!=details)
				{
					String data="";
					
					data=data+"<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data=data+"<thead>";
					data=data+"<tr>";
					data=data+"<th scope=\\\"col\\\" width=\\\"4%\\\">#.</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.ocifolder")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.total")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.success")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.failure")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.skipped")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.actual.delete")+" "+msgProps.getProperty("label.success")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.actual.delete")+" "+msgProps.getProperty("label.failure")+"</th>";
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					
					StringBuilder rowsData=new StringBuilder();
					
					// add Delta Data Row
					rowsData.append("<tr>");
					rowsData.append("<td>1</td>");
					rowsData.append("<td>"+ApplicationProperties.getProperty("mgssexttool.delta.dir")+"</td>");
					rowsData.append("<td>"+(details.getDeltaTotalCount()+details.getDeltaDeleteTotalCount())+"</td>");
					rowsData.append("<td>"+details.getDeltaSuccessCount()+"</td>");
					rowsData.append("<td>"+details.getDeltaFailureCount()+"</td>");
					rowsData.append("<td>"+details.getDeltaSkippedCount()+"</td>");
					
					rowsData.append("<td>"+details.getDeltaDeleteSuccessCount()+"</td>");
					rowsData.append("<td>"+details.getDeltaDeleteFailureCount()+"</td>");
					rowsData.append("</tr>");
					
					// add Full Data Row
					rowsData.append("<tr>");
					rowsData.append("<td>2</td>");
					rowsData.append("<td>"+ApplicationProperties.getProperty("mgssexttool.full.dir")+"</td>");
					rowsData.append("<td>"+(details.getFullTotalCount()+details.getFullDeleteTotalCount())+"</td>");
					rowsData.append("<td>"+details.getFullSuccessCount()+"</td>");
					rowsData.append("<td>"+details.getFullFailureCount()+"</td>");
					rowsData.append("<td>"+details.getFullSkippedCount()+"</td>");
					
					rowsData.append("<td>"+details.getFullDeleteSuccessCount()+"</td>");
					rowsData.append("<td>"+details.getFullDeleteFailureCount()+"</td>");
					rowsData.append("</tr>");
					
					if(null!=rowsData)
					{
						data =data+rowsData.toString();
					}
					data=data+"</tbody>";
					data=data+"</table>";	
					data=data+"</td>";
					data=data+"</tr>";
					data=data+"</table>";

					token="DATA";
					responseString = data;
					data = null;
					rowsData= null;
				}
				else
				{
					token="DATA";
					responseString = "<p>";
					responseString = responseString
							+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
				details = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewTransferSummaryDetailsOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}

	
	private static void performViewSystemErrorDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE") && !"".equals(request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE");
				String attemptCode = null;
				if (null != request.getParameter("NMDSCHHISTORY_ATTEMPT_ID") && !"".equals(request.getParameter("NMDSCHHISTORY_ATTEMPT_ID")))
				{
					attemptCode =  (String) request.getParameter("NMDSCHHISTORY_ATTEMPT_ID");
				}
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				NMDScheduleDetails errorDetails = NMDScheduleDAO.getSystemErrorDetails(scheduleCode, attemptCode);
				if(null!=errorDetails)
				{
					String data="";
					
					data=data+"<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\"historyTable\" cellspacing=\"0\" width=\"100%\" style=\"border-left:1px solid #DADADA;\">";
					data=data+"<tbody>";
					data=data+"<tr>";
					data=data+"<td width=\"20%\"><b>"+msgProps.getProperty("label.remarks")+":</b></td>";
					data=data+"<td width=\"80%\" style=\"border-left:1px solid #DADADA;\">";
					if(null!=errorDetails.getSystemErrorComments() && !"".equals(errorDetails.getSystemErrorComments()))
					{
						data+=errorDetails.getSystemErrorComments();
					}
					else
					{
						data+=msgProps.getProperty("label.info.default.system.issue.remark");
					}
					data+="</td>";
					data=data+"</tr>";
					data=data+"<tr>";
					data=data+"<td><b>"+msgProps.getProperty("label.error")+" "+msgProps.getProperty("label.details")+":</b></td>";
					data=data+"<td style=\"border-left:1px solid #DADADA;\">";
					if(null!=errorDetails.getSystemErrorMessage() && !"".equals(errorDetails.getSystemErrorMessage()))
					{
						// replace double quote in data error to single quote for avoiding parsing error
						data+=errorDetails.getSystemErrorMessage();
					}
					else
					{
						data+="-";
					}
					data+="</td>";
					data=data+"</tr>";
					data=data+"</tbody>";
					data=data+"</table>";	
					data=data+"</td>";
					data=data+"</tr>";
					data=data+"</table>";

					token="DATA";
					responseString = data;
					data = null;
				}
				else
				{
					token="DATA";
					responseString = "<p>";
					responseString = responseString
							+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
				errorDetails = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewSystemErrorDetailsOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
			responseString = responseString + "</p>";
		}
//		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		try {
			// set in Response
			response.setContentType("text/html");
			response.getWriter().write(responseString);
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewSystemErrorDetailsOperation()", e);
		}
		
		token = null;
		responseString = null;
	}

	
	private static void performViewReportsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE") && !"".equals(request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("NMDSCHHISTORY_SCHEDULE_CODE");
				String attemptCode = null;
				if (null != request.getParameter("NMDSCHHISTORY_ATTEMPT_ID") && !"".equals(request.getParameter("NMDSCHHISTORY_ATTEMPT_ID")))
				{
					attemptCode =  (String) request.getParameter("NMDSCHHISTORY_ATTEMPT_ID");
				}
				/*
				 * Check for the Reports Path on the basis of Job Id & Attempt Id
				 */
				String reportsPath = NMDScheduleDAO.getReportsPath(scheduleCode,attemptCode);
				if(null!=reportsPath && !"".equals(reportsPath))
				{
					String zipFileRelativePath=null;
					Timestamp ts = new Timestamp(new Date().getTime());
					String zipFileName="REPORTS_"+ts.getTime()+"_"+scheduleCode+"_"+attemptCode+".zip";
					String zipFilePhysicalPath=ApplicationProperties.getProperty("mgssexttool.reports.zip.physical.path");
					zipFilePhysicalPath = zipFilePhysicalPath.replace("\\", "/");
					if(!zipFilePhysicalPath.endsWith("/"))
					{
						zipFilePhysicalPath+="/";
					}
					// add zip fileName
					zipFilePhysicalPath+=zipFileName;
					File sourceDir = new File(reportsPath);
					if(sourceDir.exists() && sourceDir.isDirectory())
					{
						File[] listFiles= sourceDir.listFiles();
						if(null!=listFiles && listFiles.length>0)
						{
							/*
							 * call function to Zip Files
							 */
							zipFiles(listFiles, zipFilePhysicalPath);
							/*
							 * CHECK IF zipFilePhysicalPath EXISTS OR NOT
							 */
							File checkFile = new File(zipFilePhysicalPath);
							if(checkFile.exists() && checkFile.isFile())
							{
								logger.info("performViewReportsOperation :: Reports Zip File >"+ zipFileName+". Written Successfully.");
								// set zipFileRelativePath
								zipFileRelativePath = ApplicationProperties.getProperty("mgssexttool.reports.zip.relative.path");
								if(!zipFileRelativePath.endsWith("/"))
								{
									zipFileRelativePath+="/";
								}
								// add fileName
								zipFileRelativePath+=zipFileName;
							}
							else
							{
								logger.info("performViewReportsOperation :: Failed to Write Reports Zip File >"+ zipFileName+".");
							}
						}
						listFiles=  null;
					}
					sourceDir = null;
					
					if(null!=zipFileRelativePath && !"".equals(zipFileRelativePath))
					{
						token="PATH";
						responseString = zipFileRelativePath;
					}
					else
					{
						token="DATA";
						responseString = "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
						responseString = responseString + "</p>";
					}
					zipFileRelativePath = null;
					zipFileName = null;
					zipFilePhysicalPath = null;
					ts = null;
				}
				else
				{
					token="DATA";
					responseString = "<p>";
					responseString = responseString
							+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "performViewReportsOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.norecordsfound")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}

	private static void zipFiles(File[] srcFiles, String zipFilePath)
	{
		// A buffer to read file data in chunks
		byte[] buffer = new byte[1024];
		try
		{
			// 1. Create a FileOutputStream for the destination zip file
			FileOutputStream fos = new FileOutputStream(zipFilePath);
			// 2. Create a ZipOutputStream chained to the FileOutputStream
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (int a=0;a<srcFiles.length;a++)
			{
				File srcFile = (File)srcFiles[a];
				// Ensure the file exists before trying to add it
				if(srcFile.exists() && srcFile.isFile())
				{
					// 3. Create a new ZipEntry for each file, using the file name as the entry name
					ZipEntry zipEntry = new ZipEntry(srcFile.getName());
					zos.putNextEntry(zipEntry);

					// 4. Read the content of the source file and write it to the ZipOutputStream
					FileInputStream fis = new FileInputStream(srcFile);
					int len;
					while ((len = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
					// 5. Close the current zip entry
					zos.closeEntry();
					zipEntry = null;
					fis.close();
					fis =null;
				}
				srcFile =null;
			}
			zos.close();
			zos = null;
			fos.close();fos=null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleViewDetailsServlet.class.getName(), "zipFiles()", e);
		}
	}
}
