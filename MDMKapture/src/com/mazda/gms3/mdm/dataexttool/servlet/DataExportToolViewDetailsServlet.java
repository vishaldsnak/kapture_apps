package com.mazda.gms3.mdm.dataexttool.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.dataexttool.dao.DataExportToolDAO;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.utils.GenerateFinalResponse;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * Servlet implementation class DataExportToolViewDetailsServlet
 */
public class DataExportToolViewDetailsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	static MessageProperties msgProps = null;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DataExportToolViewDetailsServlet() {
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
			if(null!=request.getParameter("DATAEXPTOOLHIST_VIEW_ITEM_DETAILS") && !"".equals(request.getParameter("DATAEXPTOOLHIST_VIEW_ITEM_DETAILS")))
			{
				performViewItemDetailsOperation(request, response);
			}
			else if(null!=request.getParameter("DATAEXPTOOLHIST_VIEW_SUMMARY_DETAILS") && !"".equals(request.getParameter("DATAEXPTOOLHIST_VIEW_SUMMARY_DETAILS")))
			{
				performViewSummaryDetailsOperation(request, response);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolViewDetailsServlet.class.getName(), "doPost()", e);
		}
	}
	
	private static void performViewItemDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE");
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				ArrayList<DataExportToolItemDetails> itemsList = DataExportToolDAO.getScheduleItemsList(scheduleCode);
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
					data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\" class=\\\"codeClass\\\">"+msgProps.getProperty("label.locale")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.model")+"</th>";
//					data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.carcode")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.channelname")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.documenttype")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"15%\\\">"+msgProps.getProperty("label.documents")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"15%\\\">"+msgProps.getProperty("label.okassets")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.documents.processing.status")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"12%\\\">"+msgProps.getProperty("label.documents.completion.status")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"15%\\\">"+msgProps.getProperty("label.innerlinks")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"13%\\\">"+msgProps.getProperty("label.innerlinks.processing.status")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"12%\\\">"+msgProps.getProperty("label.innerlinks.completion.status")+"</th>";
					
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					StringBuilder rowsData=new StringBuilder();
					DataExportToolItemDetails itemDetails = null;
					for(int i=0;i<itemsList.size();i++)
					{
						itemDetails = (DataExportToolItemDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+itemDetails.getSrNo()+"</td>");
						rowsData.append("<td>"+itemDetails.getLocale()+"</td>");
						rowsData.append("<td>"+itemDetails.getModel()+"</td>");
//						rowsData.append("<td>"+itemDetails.getCarlineCode()+"</td>");
						rowsData.append("<td>"+itemDetails.getChannelName()+"</td>");
						if(null!=itemDetails.getDocumentType() && !"".equals(itemDetails.getDocumentType()))
						{
							rowsData.append("<td>"+itemDetails.getDocumentType()+"</td>");
						}
						else
						{
							rowsData.append("<td>-</td>");
						}
						rowsData.append("<td>"+itemDetails.getTotalDocsCount()+" / "+itemDetails.getSuccessDocsCount()+ " / "+itemDetails.getFailureDocsCount()+"</td>");
						rowsData.append("<td>"+itemDetails.getTotalOkAssetsCount()+" / "+itemDetails.getSuccessOkAssetsCount()+ " / "+itemDetails.getFailureOkAssetsCount()+"</td>");
						if(null!=itemDetails.getProcessingStatus() && !"".equals(itemDetails.getProcessingStatus()))
						{
							rowsData.append("<td>"+itemDetails.getProcessingStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						if(null!=itemDetails.getCompletionStatus() && !"".equals(itemDetails.getCompletionStatus()))
						{
							rowsData.append("<td>"+itemDetails.getCompletionStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						
						rowsData.append("<td>"+itemDetails.getTotalInnerLinksCount()+" / "+itemDetails.getSuccessInnerLinksCount()+ " / "+itemDetails.getFailureInnerLinksCount()+"</td>");
						if(null!=itemDetails.getInnerLinksProcessingStatus() && !"".equals(itemDetails.getInnerLinksProcessingStatus()))
						{
							rowsData.append("<td>"+itemDetails.getInnerLinksProcessingStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						if(null!=itemDetails.getInnerLinksCompletionStatus() && !"".equals(itemDetails.getInnerLinksCompletionStatus()))
						{
							rowsData.append("<td>"+itemDetails.getInnerLinksCompletionStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
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
			Utilities.printStackTraceToLogs(DataExportToolViewDetailsServlet.class.getName(), "performViewItemDetailsOperation()", e);
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
			if (null != request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("DATAEXPTOOLHIST_SCHEDULE_CODE");
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				ArrayList<DataExportToolReportSummaryDetails> itemsList = DataExportToolDAO.getScheduleSummaryDetails(scheduleCode);
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
			Utilities.printStackTraceToLogs(DataExportToolViewDetailsServlet.class.getName(), "performViewSummaryDetailsOperation()", e);
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
	
}
