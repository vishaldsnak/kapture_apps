package com.mazda.gms3.dmt.servlet;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataScheduleConstants;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.GenerateFinalResponse;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;

/**
 * Servlet implementation class ScheduleReports
 */
public class ScheduleReports extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static MessageProperties msgProps = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ScheduleReports() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try
		{
			msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
			if(null!=request.getParameter("DMT_HISTORY_VIEW_REPORTS") && !"".equals(request.getParameter("DMT_HISTORY_VIEW_REPORTS")))
			{
				performViewReportOperation(request, response);
			}

			if(null!=request.getParameter("DMT_HISTORY_VIEW_ITEM_DETAILS") && !"".equals(request.getParameter("DMT_HISTORY_VIEW_ITEM_DETAILS")))
			{
				performViewItemDetailsOperation(request, response);
			}
			
			if(null!=request.getParameter("DMT_HISTORY_VIEW_REPORTS_SUMMARY") && !"".equals(request.getParameter("DMT_HISTORY_VIEW_REPORTS_SUMMARY")))
			{
				performViewReportSummaryOperation(request, response);
			}
			
			
			if(null!=request.getParameter("MDVAL_VIEW_REPORTS") && !"".equals(request.getParameter("MDVAL_VIEW_REPORTS")))
			{
				performViewReportOperationForMetaDataSchedule(request, response);
			}
			
			if(null!=request.getParameter("MD_SYNC_VIEW_REPORTS") && !"".equals(request.getParameter("MD_SYNC_VIEW_REPORTS")))
			{
				performViewReportOperationForMasterDataSyncSchedule(request, response);
			}
			
			if(null!=request.getParameter("MD_SYNC_VIEW_ITEM_DETAILS") && !"".equals(request.getParameter("MD_SYNC_VIEW_ITEM_DETAILS")))
			{
				performMasterDataSyncViewItemDetailsOperation(request, response);
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "doPost()", e);
		}
	}

	/**
	 * CALL FUNCTION TO GENERATE REPORTS
	 * @param request
	 * @param response
	 */
	private static void performViewReportOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("DMT_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("DMT_SCHEDULE_CODE")) 
					&& null != request.getParameter("DMT_SCHEDULE_STATUS") 	&& !"".equals(request.getParameter("DMT_SCHEDULE_STATUS"))) 
			{
				String scheduleCode = (String) request.getParameter("DMT_SCHEDULE_CODE");
				String scheduleStatus = (String)request.getParameter("DMT_SCHEDULE_STATUS");
				// Initialize with Default Value
				String scheduleType=AutomationConstants.SCHEDULE_TYPE_DATALOAD;
				if(null!=request.getParameter("DMT_SCHEDULE_TYPE") && !"".equals(request.getParameter("DMT_SCHEDULE_TYPE")))
				{
					// if available from request, set it
					scheduleType = (String)request.getParameter("DMT_SCHEDULE_TYPE");
				}
				
				/*
				 * now prepare the path for the reports directory
				 */
				String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY")+ scheduleCode;
				File reportDir = PathUtil.file(path);
				if (reportDir.exists() && reportDir.isDirectory()) 
				{
					token = "DATA";
					File[] listFiles = reportDir.listFiles();
					if (null != listFiles && listFiles.length > 0) 
					{
						String data = "<table width=\\\"100%\\\" class=\\\"display historyTable\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\">";
						for (int a = 0; a < listFiles.length; a++) 
						{
							File file = listFiles[a];
							/*
							 * prepare the Web URL for the file Name
							 */

							String webUrl = com.mazda.gms3.dmt.utils.OkAssetsWeb.url(ApplicationProperties.getProperty("REPORTS_DIRECTORY_WEB")+ scheduleCode+ "/" + file.getName());
							data = data + "<tr>";
							data = data + "<td><strong>" + file.getName()
									+ "</strong></td>";
							data = data + "<td>";
							data = data
									+ "<a href=\\\"javascript:void(0);\\\" style=\\\"text-decoration:none;\\\" onclick=\\\"dmt_sch_openReports('"+webUrl+"');\\\">";
							data = data+ "<i class=\\\"downloadReports\\\"></i>";
							data = data + "</a>";
							data = data + "</td>";
							data = data + "</tr>";
							file = null;
							webUrl = null;
						}
						data = data + "</table>";
						responseString = data;
						listFiles = null;
						data = null;
					} 
					else 
					{
						token = "DATA";
						if (scheduleStatus.equals(ApplicationProperties.getProperty("schedule.status.pending.value"))
								|| scheduleStatus.equals(ApplicationProperties.getProperty("schedule.status.processing.value"))) 
						{
							if(scheduleType.trim().equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
							{
								responseString = "<p>";
								responseString = responseString
										+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
										+ scheduleStatus
										+ "</u>. "+msgProps.getProperty("label.pending.dialog.info.end")+"</strong>";
								responseString = responseString + "</p>";
							}
							else
							{
								// AUTOMATION
								responseString = "<p>";
								responseString = responseString
										+ "<strong>"+msgProps.getProperty("label.ipm.facelift.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
										+ scheduleStatus
										+ "</u>. "+msgProps.getProperty("label.ipm.facelift.pending.dialog.info.end")+"</strong>";
								responseString = responseString + "</p>";
							}
						}
						else
						{
							responseString = "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
							responseString = responseString + "</p>";
						}
					}
				} 
				else 
				{
					token = "DATA";
					if (scheduleStatus.equals(ApplicationProperties.getProperty("schedule.status.pending.value"))
							|| scheduleStatus.equals(ApplicationProperties.getProperty("schedule.status.processing.value"))) 
					{
						if(scheduleType.trim().equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
						{
							responseString = "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
									+ scheduleStatus
									+ "</u>. "+msgProps.getProperty("label.pending.dialog.info.end")+"</strong>";
							responseString = responseString + "</p>";
						}
						else
						{
							// AUTOMATION
							responseString = "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.ipm.facelift.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
									+ scheduleStatus
									+ "</u>. "+msgProps.getProperty("label.ipm.facelift.pending.dialog.info.end")+"</strong>";
							responseString = responseString + "</p>";
						}
					} 
					else  
					{
						responseString = "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
						responseString = responseString + "</p>";
					} 
				}
				path = null;
				reportDir = null;
				scheduleCode = null;
				scheduleStatus = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performViewReportOperation()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
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
			if (null != request.getParameter("DMT_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("DMT_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("DMT_SCHEDULE_CODE");
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				ArrayList<ScheduleItemDetails> itemsList = ScheduleDAO.getScheduleItemDetails(scheduleCode);
				if(null!=itemsList && itemsList.size()>0)
				{
					// setDefault Value - DataLoad
					String scheduleType=AutomationConstants.SCHEDULE_TYPE_DATALOAD;
					for(int i=0;i<itemsList.size();i++)
					{
						ScheduleItemDetails itemDetails = (ScheduleItemDetails)itemsList.get(i);
						if(null!=itemDetails.getScheduleType() && !"".equals(itemDetails.getScheduleType()))
						{
							scheduleType = itemDetails.getScheduleType();
						}
					}	
					
					
					String data="";
					
					data=data+"<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data=data+"<thead>";
					data=data+"<tr>";
					data=data+"<th scope=\\\"col\\\" width=\\\"3%\\\">#.</th>";
					if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
					{
						data=data+"<th scope=\\\"col\\\" width=\\\"5%\\\" class=\\\"codeClass\\\">"+msgProps.getProperty("label.locale")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.model")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.manualtype")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"7%\\\">"+msgProps.getProperty("label.materialname")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"11%\\\">"+msgProps.getProperty("label.documents")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.okassetscount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.innerlinkscount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.deletecount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.displayordercount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.cdprocessingcount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.scmvinmappingcount")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">"+msgProps.getProperty("label.contentstatus")+"</th>";
					}
					else if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_AUTOMATION))
					{
						data=data+"<th scope=\\\"col\\\" width=\\\"12%\\\" class=\\\"codeClass\\\">"+msgProps.getProperty("label.locale")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.channel")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"17%\\\">"+msgProps.getProperty("label.documenttype")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"17%\\\">"+msgProps.getProperty("label.carlinedetails")+"</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.documents")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
						data=data+"<th scope=\\\"col\\\" width=\\\"15%\\\">"+msgProps.getProperty("label.contentstatus")+"</th>";
					}
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					StringBuilder rowsData=new StringBuilder();
					for(int i=0;i<itemsList.size();i++)
					{
						ScheduleItemDetails itemDetails = (ScheduleItemDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+itemDetails.getSrNo()+"</td>");
						if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
						{
//							rowsData.append("<td>"+itemDetails.getMarket()+"</td>");
							rowsData.append("<td>"+itemDetails.getLocale()+"</td>");
							rowsData.append("<td>"+itemDetails.getModelFolderName()+"</td>");
							rowsData.append("<td>"+itemDetails.getManualType()+"</td>");
							if(null!=itemDetails.getMaterialFolderName() && !"".equals(itemDetails.getMaterialFolderName()))
							{
								rowsData.append("<td>"+itemDetails.getMaterialFolderName()+"</td>");
							}
							else
							{
								rowsData.append("<td>-</td>");
							}
							rowsData.append("<td>"+itemDetails.getTotalDocsForProcessing()+" / "+itemDetails.getProcessedDocsCount()+ " / "+itemDetails.getFailedProcessedDocsCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getOkAssetsCount()+" / "+itemDetails.getProcessedOkAssetsCount()+ " / "+itemDetails.getFailedOkAssetsCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getTotalInnerLinksCount()+" / "+itemDetails.getProcessedInnerLinksCount()+ " / "+itemDetails.getFailedInnerLinksCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getTotalDocsForDeletion()+" / "+itemDetails.getDeletedDocsCount()+ " / "+itemDetails.getFailedDeletedDocsCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getTotalDisplayOrderCount()+" / "+itemDetails.getProcessedDisplayOrderCount()+ " / "+itemDetails.getFailedDisplayOrderCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getTotalCDProcessingCount()+" / "+itemDetails.getProcessedCDProcessingCount()+ " / "+itemDetails.getFailedCDProcessingCount()+"</td>");
							rowsData.append("<td>"+itemDetails.getTotalSCMVinCount()+" / "+itemDetails.getProcessedSCMVinCount()+ " / "+itemDetails.getFailedSCMVinCount()+"</td>");
							if(null!=itemDetails.getCurrentProcessingStatus() && !"".equals(itemDetails.getCurrentProcessingStatus()))
							{
								rowsData.append("<td>"+itemDetails.getCurrentProcessingStatus()+"</td>");
							}
							else
							{
								rowsData.append("<td></td>");
							}
						}
						else if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_AUTOMATION))
						{
							rowsData.append("<td>"+itemDetails.getLocale()+"</td>");
							if(null!=itemDetails.getModelFolderName())
							{
								if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_ACCESSORIES+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_TRAINING))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_TRAINING+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_VIDEOS+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_WIRING_DIAGRAMS+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_SERVICE_MANUAL_TYPE+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_OTHER_MANUAL_TYPE+"</td>");
								}
								else if(itemDetails.getModelFolderName().trim().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE))
								{
									rowsData.append("<td>"+AutomationConstants.CHANNEL_LABEL_SERVICE_INFORMATION_TYPE+"</td>");
								}
							}
							else
							{
								rowsData.append("<td>-</td>");
							}
							
							
							if(null!=itemDetails.getManualType())
							{
								rowsData.append("<td>"+itemDetails.getManualType().replace("_", " ")+"</td>");
							}
							else
							{
								rowsData.append("<td>-</td>");
							}
							
							if(null!=itemDetails.getModel())
							{
								rowsData.append("<td>"+itemDetails.getModel()+"</td>");
							}
							else
							{
								rowsData.append("<td>-</td>");
							}
							
							rowsData.append("<td>"+itemDetails.getTotalDocsForProcessing()+" / "+itemDetails.getProcessedDocsCount()+ " / "+itemDetails.getFailedProcessedDocsCount()+"</td>");
							if(null!=itemDetails.getCurrentProcessingStatus() && !"".equals(itemDetails.getCurrentProcessingStatus()))
							{
								rowsData.append("<td>"+itemDetails.getCurrentProcessingStatus()+"</td>");
							}
							else
							{
								rowsData.append("<td></td>");
							}
							
						}

						
						rowsData.append("</tr>");
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
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performViewItemDetailsOperation()", e);
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
	
	private static void performViewReportSummaryOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("DMT_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("DMT_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("DMT_SCHEDULE_CODE");
				/*
				 * now call function to fetch Report Summary Details for the Schedule Id
				 */
				ArrayList<ReportsSummaryDetails> itemsList = ScheduleDAO.getReportSummaryDetailsForSchedule(scheduleCode);
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
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("label.total")+" "+msgProps.getProperty("label.transactions")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"18%\\\">"+msgProps.getProperty("schedule.status.success.label")+" "+msgProps.getProperty("label.transactions")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"19%\\\">"+msgProps.getProperty("schedule.status.failure.label")+" "+msgProps.getProperty("label.transactions")+"</th>";
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					StringBuilder rowsData=new StringBuilder();
					for(int i=0;i<itemsList.size();i++)
					{
						ReportsSummaryDetails itemDetails = (ReportsSummaryDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+(i+1)+"</td>");
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
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performViewReportSummaryOperation()", e);
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

	private static void performViewReportOperationForMetaDataSchedule(HttpServletRequest request,HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("MDVAL_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("MDVAL_SCHEDULE_CODE")) 
					&& null != request.getParameter("MDVAL_SCHEDULE_STATUS") 	&& !"".equals(request.getParameter("MDVAL_SCHEDULE_STATUS"))) 
			{
				String scheduleCode = (String) request.getParameter("MDVAL_SCHEDULE_CODE");
				String scheduleStatus = (String)request.getParameter("MDVAL_SCHEDULE_STATUS");
				String remarks="";
				if(null!=request.getParameter("MDVAL_SCHEDULE_REMARKS") && !"".equals(request.getParameter("MDVAL_SCHEDULE_REMARKS")))
				{
					remarks = String.valueOf(request.getParameter("MDVAL_SCHEDULE_REMARKS"));
				}

				/*
				 * now prepare the path for the reports directory
				 */
				String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				path+=scheduleCode;
				File reportDir = PathUtil.file(path);
				if (reportDir.exists() && reportDir.isDirectory()) 
				{
					token = "DATA";
					File[] listFiles = reportDir.listFiles();
					if (null != listFiles && listFiles.length > 0) 
					{
						String data = "<table width=\\\"100%\\\" class=\\\"display historyTable\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\">";
						if(null!=remarks && !"".equals(remarks))
						{
							data = data + "<tr>";
							data = data + "<td colspan=\\\"2\\\"><strong>Remarks: " 
									+ "</strong>"+remarks+"</td>";
							data = data + "</tr>";
						}
						for (int a = 0; a < listFiles.length; a++) 
						{
							File file = listFiles[a];
							/*
							 * prepare the Web URL for the file Name
							 */

							String webUrl = ApplicationProperties.getProperty("metadata.validation.reports.relativepath");
							if(!webUrl.endsWith("/"))
							{
								webUrl+="/";
							}
							webUrl+=scheduleCode+ "/" + file.getName();
							webUrl = com.mazda.gms3.dmt.utils.OkAssetsWeb.url(webUrl);
							data = data + "<tr>";
							data = data + "<td><strong>" + file.getName()
									+ "</strong></td>";
							data = data + "<td>";
							data = data
									+ "<a href=\\\"javascript:void(0);\\\" style=\\\"text-decoration:none;\\\" onclick=\\\"mdval_showReports('"+webUrl+"');\\\">";
							data = data+ "<i class=\\\"downloadReports\\\"></i>";
							data = data + "</a>";
							data = data + "</td>";
							data = data + "</tr>";
							file = null;
							webUrl = null;
						}
						data = data + "</table>";
						responseString = data;
						listFiles = null;
						data = null;
					}
					else 
					{
						token = "DATA";
						if (scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PENDING)
								|| scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PROCESSING)) 
						{
							responseString = "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
									+ scheduleStatus
									+ "</u>. "+msgProps.getProperty("label.metadataval.pending.dialog.info.end")+"</strong>";
							responseString = responseString + "</p>";
						}
						else
						{
							responseString="";
							if(null!=remarks && !"".equals(remarks))
							{
								responseString = "<p>";
								responseString = responseString
										+ "<strong>Remarks: </strong>"+remarks;
								responseString = responseString + "</p><br/>";
							}
							responseString+= "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
							responseString = responseString + "</p>";
						}
					}
				} 
				else 
				{
					token = "DATA";
					if (scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PENDING)
							|| scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PROCESSING)) 
					{
						responseString = "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
								+ scheduleStatus
								+ "</u>. "+msgProps.getProperty("label.metadataval.pending.dialog.info.end")+"</strong>";
						responseString = responseString + "</p>";
					} 
					else  
					{
						responseString="";
						if(null!=remarks && !"".equals(remarks))
						{
							responseString = "<p>";
							responseString = responseString
									+ "<strong>Remarks: </strong>"+remarks;
							responseString = responseString + "</p><br/>";
						}
						responseString+= "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
						responseString = responseString + "</p>";
					} 
				}
				path = null;
				reportDir = null;
				scheduleCode = null;
				scheduleStatus = null;
				remarks=null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performViewReportOperationForMetaDataSchedule()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}

	private static void performViewReportOperationForMasterDataSyncSchedule(HttpServletRequest request,HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("MD_SYNC_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("MD_SYNC_SCHEDULE_CODE")) 
					&& null != request.getParameter("MD_SYNC_SCHEDULE_STATUS") 	&& !"".equals(request.getParameter("MD_SYNC_SCHEDULE_STATUS"))) 
			{
				String scheduleCode = (String) request.getParameter("MD_SYNC_SCHEDULE_CODE");
				String scheduleStatus = (String)request.getParameter("MD_SYNC_SCHEDULE_STATUS");

				/*
				 * now prepare the path for the reports directory
				 */
				String path = ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				path+=scheduleCode;
				File reportDir = PathUtil.file(path);
				if (reportDir.exists() && reportDir.isDirectory()) 
				{
					token = "DATA";
					File[] listFiles = reportDir.listFiles();
					if (null != listFiles && listFiles.length > 0) 
					{
//						String data = "<table width=\\\"100%\\\" class=\\\"display historyTable\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\">";
//						for (int a = 0; a < listFiles.length; a++) 
//						{
//							File file = listFiles[a];
//							/*
//							 * prepare the Web URL for the file Name
//							 */
//
//							String webUrl = ApplicationProperties.getProperty("masterdata.synching.reports.relativepath");
//							if(!webUrl.endsWith("/"))
//							{
//								webUrl+="/";
//							}
//							webUrl+=scheduleCode+ "/" + file.getName();
//							data = data + "<tr>";
//							data = data + "<td><strong>" + file.getName()
//									+ "</strong></td>";
//							data = data + "<td>";
//							data = data
//									+ "<a href=\\\"javascript:void(0);\\\" style=\\\"text-decoration:none;\\\" onclick=\\\"mdsync_showReports('"+webUrl+"');\\\">";
//							data = data+ "<i class=\\\"downloadReports\\\"></i>";
//							data = data + "</a>";
//							data = data + "</td>";
//							data = data + "</tr>";
//							file = null;
//							webUrl = null;
//						}
//						data = data + "</table>";
						String reportsPath="";
						File file = null;
						String webUrl = ApplicationProperties.getProperty("masterdata.synching.reports.relativepath");
						if(!webUrl.endsWith("/"))
						{
							webUrl+="/";
						}
						for (int a = 0; a < listFiles.length; a++) 
						{
							file = listFiles[a];
							if(file.exists() && file.isFile() && file.getName().trim().toLowerCase().endsWith(".zip"))
							{
								reportsPath=com.mazda.gms3.dmt.utils.OkAssetsWeb.url(webUrl+scheduleCode+ "/" + file.getName());
							}
							file = null;
						}
						file = null;
						webUrl=  null;
						if(null!=reportsPath && !"".equals(reportsPath))
						{
							token="DATA_WITH_URL";
							responseString=reportsPath;
						}
						else
						{
							token = "DATA";
							responseString="";
							responseString+= "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
							responseString = responseString + "</p>";
						}
//						responseString = data;
						listFiles = null;
//						data = null;
					}
					else 
					{
						token = "DATA";
						if (scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PENDING)
								|| scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PROCESSING)) 
						{
							responseString = "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
									+ scheduleStatus
									+ "</u>. "+msgProps.getProperty("label.masterdatasync.pending.dialog.info.end")+"</strong>";
							responseString = responseString + "</p>";
						}
						else
						{
							responseString="";
							responseString+= "<p>";
							responseString = responseString
									+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
							responseString = responseString + "</p>";
						}
					}
				} 
				else 
				{
					token = "DATA";
					if (scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PENDING)
							|| scheduleStatus.equals(MetaDataScheduleConstants.STATUS_PROCESSING)) 
					{
						responseString = "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.pending.dialog.info.start")+" <u style=\\\"color:#D0021B\\\">"
								+ scheduleStatus
								+ "</u>. "+msgProps.getProperty("label.masterdatasync.pending.dialog.info.end")+"</strong>";
						responseString = responseString + "</p>";
					} 
					else  
					{
						responseString="";
						responseString+= "<p>";
						responseString = responseString
								+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
						responseString = responseString + "</p>";
					} 
				}
				path = null;
				reportDir = null;
				scheduleCode = null;
				scheduleStatus = null;
			} 
			else 
			{
				token="DATA";
				responseString = "<p>";
				responseString = responseString
						+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
				responseString = responseString + "</p>";
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performViewReportOperationForMetaDataSchedule()", e);
			token="DATA";
			responseString = "<p>";
			responseString = responseString
					+ "<strong>"+msgProps.getProperty("label.no.reports.found")+"</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,responseString);
		token = null;
		responseString = null;
	}
	
	private static void performMasterDataSyncViewItemDetailsOperation(HttpServletRequest request,	HttpServletResponse response) 
	{
		String token = "";
		String responseString = "";
		try 
		{
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("MD_SYNC_SCHEDULE_CODE") 	&& !"".equals(request.getParameter("MD_SYNC_SCHEDULE_CODE"))) 
			{
				String scheduleCode = (String) request.getParameter("MD_SYNC_SCHEDULE_CODE");
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 * Identify the Schedule Type from fetchedItemsList
				 * if automation - then show #. locale>channel>document type>model>documentCounts>currentStatus
				 * else if dataLoad
				 * existing flow
				 */
				ArrayList<AutoSyncScheduleItemDetails> itemsList = MasterDataSyncDAO.getScheduleItemDetails(scheduleCode);
				if(null!=itemsList && itemsList.size()>0)
				{
					String data="";
					
					data=data+"<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data=data+"<tr>";
					data=data+"<td>";
					data=data+"<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data=data+"<thead>";
					data=data+"<tr>";
					data=data+"<th scope=\\\"col\\\" width=\\\"10%\\\">#.</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"25%\\\" class=\\\"codeClass\\\">"+msgProps.getProperty("label.masterdatatype")+"</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"45%\\\">"+msgProps.getProperty("label.categories")+" - ("+msgProps.getProperty("label.total")+" / "+msgProps.getProperty("label.processed")+" / "+msgProps.getProperty("label.failed")+")</th>";
					data=data+"<th scope=\\\"col\\\" width=\\\"20%\\\">"+msgProps.getProperty("label.status")+"</th>";
					
					
					data=data+"</tr>";
					data=data+"</thead>";
					data=data+"<tbody>";
					StringBuilder rowsData=new StringBuilder();
					for(int i=0;i<itemsList.size();i++)
					{
						AutoSyncScheduleItemDetails itemDetails = (AutoSyncScheduleItemDetails)itemsList.get(i);
						rowsData.append("<tr>");
						rowsData.append("<td>"+itemDetails.getSrNo()+"</td>");
						rowsData.append("<td>"+itemDetails.getItemName()+"</td>");
						rowsData.append("<td>"+itemDetails.getTotalCount()+" / "+itemDetails.getSuccessCount()+ " / "+itemDetails.getFailureCount()+"</td>");
						if(null!=itemDetails.getProcessingStatus() && !"".equals(itemDetails.getProcessingStatus()))
						{
							rowsData.append("<td>"+itemDetails.getProcessingStatus()+"</td>");
						}
						else
						{
							rowsData.append("<td></td>");
						}
						rowsData.append("</tr>");
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
			Utilities.printStackTraceToLogs(ScheduleReports.class.getName(), "performMasterDataSyncViewItemDetailsOperation()", e);
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