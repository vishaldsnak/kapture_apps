package com.mazda.gms3.dmt.email.generator;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.SendMailUsingAuthentication;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;

public class NotificationEmailHelper {

	static Logger logger = LogManager.getLogger(NotificationEmailHelper.class);
	
	private static String toMailid=ApplicationProperties.getProperty("NOTIF_EMAIL_TO_ID");
	private static String fromMailid=ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID");
	private static String context = ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
	
	public static void generateNotificationEmail(String scheduleCode, String loggeInUserEmailId)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{
				MessageProperties msgProps = new MessageProperties(ApplicationProperties.getProperty("locales.values.english"));
				/*
				 * CALL FUNCTION TO FETCH SCHEDULE AND ITEM DETAIS FOR THE SCHEDULE CODE
				 */
				ScheduleDetails schDetails = ScheduleDAO.getScheduleSpecificDetails(scheduleCode);
				if(null!=schDetails && schDetails.getScheduleId()>0)
				{
					// set Default as DATALOAD
					String scheduleType=AutomationConstants.SCHEDULE_TYPE_DATALOAD;
					if(null!=schDetails.getScheduleType() && !"".equals(schDetails.getScheduleType()))
					{
						scheduleType = schDetails.getScheduleType();
					}
					// set default Portal Host Name
					String hostName=ApplicationProperties.getProperty("GMS3_EMAIL_NOTIF_HOSTNAME");
					if(null!=schDetails.getUserId() && !"".equals(schDetails.getUserId()))
					{
						if(schDetails.getUserId().trim().toLowerCase().endsWith("@mazda.co.jp") || 
								schDetails.getUserId().trim().toLowerCase().endsWith("@mazdaeur.com"))
						{
							// MC  OR MME USER = USE GTS HOST
							hostName = ApplicationProperties.getProperty("GMS3_EMAIL_GTS_NOTIF_HOSTNAME");
						}
					}
					/*
					 * CALL FUNCTION TO FETCH ITEM DETAILS
					 */
					ArrayList<ScheduleItemDetails> itemsList = ScheduleDAO.getScheduleItemDetails(scheduleCode);
					
					/*
					 * NOW LOAD THE EMAIL TEMPLATE
					 * REPLACE THE FOLLOWING PLACE HOLDERS
					 * 
					 * HOSTNAME
					 * _CONTEXTNAME
					 * SCHEDULE_JOB_NAME
					 * END_USER_NAME
					 * END_TIME
					 * JOB_STATUS
					 * DATA_PLACE_HOLDER
					 * ITEM_DETAILS_PLACE_HOLDER
					 * URL_REPORT_DOWNLOAD
					 * COPYRIGHT_YEAR
					 * 
					 * DATE FORMAT - dd MMM yyyy HH:mm:ss
					 */
					String templatePath="";
					if(scheduleType.trim().equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
					{
						templatePath = "/com/mazda/gms3/dmt/email/template/DMT_NOTIFICATION_TEMPLATE.html";
					}
					else
					{
						templatePath = "/com/mazda/gms3/dmt/email/template/IPM_FACELIFT_NOTIFICATION_TEMPLATE.html";
					}
					InputStream is = NotificationEmailHelper.class.getResourceAsStream(templatePath);
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
								templateString = templateString.replace("SCHEDULE_JOB_NAME", schDetails.getScheduleName());
							}
							else
							{
								templateString = templateString.replace("SCHEDULE_JOB_NAME", "A");
							}
							
							// CONTEXTNAME
							templateString  =templateString.replace("_CONTEXTNAME", context);
							
							// END_USER_NAME
							if(null!=schDetails.getUserId() && !"".equals(schDetails.getUserId()))
							{
								templateString = templateString.replace("END_USER_NAME", schDetails.getUserId());
							}
							else
							{
								templateString = templateString.replace("END_USER_NAME", "Business User");
							}
							
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
							if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
							{
								if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value")))
								{
									schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.pending.label"));
								}
								else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
								{
									schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.processing.label"));
								}
								else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.success.value")))
								{
									schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.success.label"));
								}
								else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.failure.value")))
								{
									schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.failure.label"));
								}
								else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.aborted.value")))
								{
									schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.aborted.label"));
								}
								
								if(null!=schDetails.getScheduleStatusLabel() && !"".equals(schDetails.getScheduleStatusLabel()))
								{
									templateString = templateString.replace("JOB_STATUS", schDetails.getScheduleStatusLabel());
								}
								else
								{
									templateString = templateString.replace("JOB_STATUS", "-");
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
							
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalDocsForProcessing()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getProcessedDocsCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getTotalDocsForProcessing() -  schDetails.getProcessedDocsCount())+"</td>");
							
							if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
							{
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalDocsForDeletion()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getDeletedDocsCount()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getTotalDocsForDeletion() -  schDetails.getDeletedDocsCount())+"</td>");

								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getOkAssetsCount()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getProcessedOkAssetsCount()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getOkAssetsCount() -  schDetails.getProcessedOkAssetsCount())+"</td>");

								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getOkAssetsDeleteCount()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getProcessedOkAssetsDeleteCount()+"</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getOkAssetsDeleteCount() -  schDetails.getProcessedOkAssetsDeleteCount())+"</td>");

								//	dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalMetaDataDocsCount()+"</td>");
								//	dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getProcessedMetaDataDocsCount()+"</td>");
								//	dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+(schDetails.getTotalMetaDataDocsCount() -  schDetails.getProcessedMetaDataDocsCount())+"</td>");

								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">0</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">0</td>");
								dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">0</td>");
							}
							dataHolder.append("</tr>");
							
							templateString = templateString.replace("DATA_PLACE_HOLDER", dataHolder.toString());
							dataHolder = null;
							
							// ITEM_DETAILS_PLACE_HOLDER
							if(null!=itemsList && itemsList.size()>0)
							{
								StringBuilder itemHolder = new StringBuilder();
								for(int a=0;a<itemsList.size();a++)
								{
									ScheduleItemDetails itemDetails = (ScheduleItemDetails)itemsList.get(a);
									itemHolder.append("<tr style=\"height:30px;\">");
									itemHolder.append("<td style=\"color:#5C5B65;border-left: none !important;font-size:12px;text-align:center;border-bottom: 1px solid #DADADA;\">"+(a+1)+"</td>");
									if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
									{
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getMarket()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getLocale()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getModelFolderName()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getManualType()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getMaterialFolderName()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getTotalDocsForProcessing()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getTotalDocsForDeletion()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getOkAssetsCount()+"</td>");
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getOkAssetsDeleteCount()+"</td>");
									}
									else
									{
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getLocale()+"</td>");
										if(null!=itemDetails.getModelFolderName() && !"".equals(itemDetails.getModelFolderName()))
										{
											if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_ACCESSORIES+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_TRAINING.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_TRAINING+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_VIDEOS+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_WIRING_DIAGRAMS+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_SERVICE_INFORMATION_TYPE+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_SERVICE_MANUAL_TYPE+"</td>");
											}
											else if(itemDetails.getModelFolderName().trim().toLowerCase().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE.trim().toLowerCase()))
											{
												itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+AutomationConstants.CHANNEL_LABEL_OTHER_MANUAL_TYPE+"</td>");
											}
										}
										else
										{
											itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">-</td>");
										}
										if(null!=itemDetails.getManualType() && !"".equals(itemDetails.getManualType()))
										{
											itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getManualType().replace("_", " ")+"</td>");
										}
										else
										{
											itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">-</td>");
										}
										if(null!=itemDetails.getModel() && !"".equals(itemDetails.getModel()))
										{
											itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getModel()+"</td>");
										}
										else
										{
											itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">-</td>");
										}
										
										itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getTotalDocsForProcessing()+"</td>");
									}
									itemHolder.append("</tr>");
									itemDetails=  null;
								}
								templateString = templateString.replace("ITEM_DETAILS_PLACE_HOLDER", itemHolder.toString());
								itemHolder = null;
							}
							else
							{
								String data = "";
								if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
								{
									data = "<tr><td colspan=\"10\"> - </td></tr>";
								}
								else
								{
									data = "<tr><td colspan=\"6\"> - </td></tr>";
								}
								templateString = templateString.replace("ITEM_DETAILS_PLACE_HOLDER", data);
								data = null;
							}
							
							// URL_REPORT_DOWNLOAD - HOSTNAME/dmdcapps/library/MAZDA/GMS3_CUSTOM/GMS3_REPORTS/SCHEDULECODE/SCHEDULECODE_REPORTS.ZIP
							String url = hostName;
							url = url+ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
							url = url+com.mazda.gms3.dmt.utils.OkAssetsWeb.url(ApplicationProperties.getProperty("REPORTS_DIRECTORY_WEB")
									+scheduleCode+"/"+scheduleCode+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX"));
							templateString = templateString.replace("URL_REPORT_DOWNLOAD", url);
							url = null;
							
							// REPALCE COPYRIGHT YEAR
							templateString = templateString.replace("COPYRIGHT_YEAR", String.valueOf(convDate));
							
							
							/*
							 * CALL FUNCTION TO SEND EMAIL
							 */
							
							/*
							 * CALL FUNCTION TO SEND EMAIL
							 */
							ArrayList<String> toList = new ArrayList<String>();
							if(null!=loggeInUserEmailId && !"".equals(loggeInUserEmailId))
							{
								toList.add(loggeInUserEmailId);
							}
							// add other email Id from config
							if(null!=toMailid && !"".equals(toMailid))
							{
								String emails[] = toMailid.split(",");
								if(null!=emails && emails.length>0)
								{
									for(int a=0;a<emails.length;a++)
									{
										toList.add(emails[a]);
									}
								}
								emails = null;
							}
							
							String subject = "";
							if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
							{
								subject = "Data Conversion Summary - "+ schDetails.getScheduleName();
							}
							else
							{
								subject = "IPM Facelift VIN Update Summary - "+ schDetails.getScheduleName();
							}
							boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, fromMailid);
							if(sendEmail==true)
							{
								logger.info("generateNotificationEmail() :: Notification Sent Successfully."); 
							}
							else
							{
								logger.info("generateNotificationEmail() :: Failed to Send Email Notification.");
							}
							subject = null;
							templateString = null;
							msgProps = null;
							toList = null;
							
						}
					}
					itemsList=  null;
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
			Utilities.printStackTraceToLogs(NotificationEmailHelper.class.getName(), "generateNotificationEmail()", e);
		}
	}

	public static void generateMasterDataSyncNotificationEmail(String scheduleCode)
	{
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{
				/*
				 * CALL FUNCTION TO FETCH SCHEDULE AND ITEM DETAIS FOR THE SCHEDULE CODE
				 */
				AutoSyncScheduleDetails schDetails = MasterDataSyncDAO.getScheduleSpecificDetails(scheduleCode);
				if(null!=schDetails && schDetails.getScheduleId()>0)
				{
					// set default Portal Host Name
					String hostName=ApplicationProperties.getProperty("GMS3_EMAIL_NOTIF_HOSTNAME");
					if(null!=schDetails.getUserId() && !"".equals(schDetails.getUserId()))
					{
						if(schDetails.getUserId().trim().toLowerCase().endsWith("@mazda.co.jp") || 
								schDetails.getUserId().trim().toLowerCase().endsWith("@mazdaeur.com"))
						{
							// MC  OR MME USER = USE GTS HOST
							hostName = ApplicationProperties.getProperty("GMS3_EMAIL_GTS_NOTIF_HOSTNAME");
						}
					}
					/*
					 * CALL FUNCTION TO FETCH ITEM DETAILS
					 */
					ArrayList<AutoSyncScheduleItemDetails> itemsList = schDetails.getItemsList();
					
					/*
					 * NOW LOAD THE EMAIL TEMPLATE
					 * REPLACE THE FOLLOWING PLACE HOLDERS
					 * 
					 * HOSTNAME
					 * _CONTEXTNAME
					 * SCHEDULE_JOB_NAME
					 * END_USER_NAME
					 * END_TIME
					 * JOB_STATUS
					 * DATA_PLACE_HOLDER
					 * ITEM_DETAILS_PLACE_HOLDER
					 * URL_REPORT_DOWNLOAD
					 * COPYRIGHT_YEAR
					 * 
					 * DATE FORMAT - dd MMM yyyy HH:mm:ss
					 */
					String templatePath="/com/mazda/gms3/dmt/email/template/MASTER_DATA_SYNCHING_NOTIFICATION_TEMPLATE.html";
					InputStream is = NotificationEmailHelper.class.getResourceAsStream(templatePath);
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
								templateString = templateString.replace("SCHEDULE_JOB_NAME", schDetails.getScheduleName());
							}
							else
							{
								templateString = templateString.replace("SCHEDULE_JOB_NAME", "A");
							}
							
							// CONTEXTNAME
							templateString  =templateString.replace("_CONTEXTNAME", context);
							
							// END_USER_NAME
							if(null!=schDetails.getUserId() && !"".equals(schDetails.getUserId()))
							{
								templateString = templateString.replace("END_USER_NAME", schDetails.getUserId());
							}
							else
							{
								templateString = templateString.replace("END_USER_NAME", "Business User");
							}
							
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
							if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
							{
								templateString = templateString.replace("JOB_STATUS", schDetails.getScheduleStatus());
							}
							else
							{
								templateString = templateString.replace("JOB_STATUS", "-");
							}
							
							if(null!=schDetails.getMarket() && !"".equals(schDetails.getMarket()))
							{
								templateString = templateString.replace("MARKET_NAME", schDetails.getMarket());
							}
							else
							{
								templateString = templateString.replace("MARKET_NAME", "-");
							}
							
							
							// DATA_PLACE_HOLDER
							StringBuilder dataHolder = new StringBuilder();
							dataHolder.append("<tr style=\"height:30px;\">");
							dataHolder.append("<td style=\"color:#5C5B65;border-left: none !important;font-size:12px;text-align:center;border-bottom: 1px solid #DADADA;\">1</td>");
							
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getTotalCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getSuccessCount()+"</td>");
							dataHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+schDetails.getFailureCount()+"</td>");
							
							dataHolder.append("</tr>");
							
							templateString = templateString.replace("DATA_PLACE_HOLDER", dataHolder.toString());
							dataHolder = null;
							
							// ITEM_DETAILS_PLACE_HOLDER
							if(null!=itemsList && itemsList.size()>0)
							{
								StringBuilder itemHolder = new StringBuilder();
								AutoSyncScheduleItemDetails itemDetails = null;
								for(int a=0;a<itemsList.size();a++)
								{
									itemDetails = (AutoSyncScheduleItemDetails)itemsList.get(a);
									if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_AXLE_TYPE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_BODY_TYPE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_MANUAL_TYPE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_MODEL_YEAR);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_VIN_RANGE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_CARLINE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_ESI_CATEGORY);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_CVC_CATEGORY);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_ENGINE_TYPE);
									}
									else if(itemDetails.getItemKey().equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
									{
										itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_TRANSMISSION_TYPE);
									}
									
									
									itemHolder.append("<tr style=\"height:30px;\">");
									itemHolder.append("<td style=\"color:#5C5B65;border-left: none !important;font-size:12px;text-align:center;border-bottom: 1px solid #DADADA;\">"+(a+1)+"</td>");
									itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getItemName()+"</td>");
									itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getTotalCount()+"</td>");
									itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getSuccessCount()+"</td>");
									itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getFailureCount()+"</td>");
									itemHolder.append("<td style=\"color:#5C5B65;font-size:12px;color:#5C5B65;border-left: 1px solid #DADADA;text-align:center;border-bottom: 1px solid #DADADA;\">"+itemDetails.getProcessingStatus()+"</td>");
									itemHolder.append("</tr>");
									itemDetails=  null;
								}
								templateString = templateString.replace("ITEM_DETAILS_PLACE_HOLDER", itemHolder.toString());
								itemHolder = null;
							}
							else
							{
								String data = "";
								data = "<tr><td colspan=\"4\"> - </td></tr>";
								templateString = templateString.replace("ITEM_DETAILS_PLACE_HOLDER", data);
								data = null;
							}
							
							// URL_REPORT_DOWNLOAD - HOSTNAME/dmdcapps/library/MAZDA/GMS3_CUSTOM/MASTERDATA_SYNC/REPORTS/SCHEDULECODE/SCHEDULECODE_REPORTS.ZIP
							String url = hostName;
							url = url+ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
							url = url+com.mazda.gms3.dmt.utils.OkAssetsWeb.url(ApplicationProperties.getProperty("masterdata.synching.reports.relativepath")
									+scheduleCode+"/"+scheduleCode+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX"));
							templateString = templateString.replace("URL_REPORT_DOWNLOAD", url);
							url = null;
							
							// REPALCE COPYRIGHT YEAR
							templateString = templateString.replace("COPYRIGHT_YEAR", String.valueOf(convDate));
							
							/*
							 * CALL FUNCTION TO SEND EMAIL
							 */
							ArrayList<String> toList = new ArrayList<String>();
							if(null!=toMailid && !"".equals(toMailid))
							{
								String emails[] = toMailid.split(",");
								if(null!=emails && emails.length>0)
								{
									for(int a=0;a<emails.length;a++)
									{
										toList.add(emails[a]);
									}
								}
								emails = null;
							}
							
							String subject = "Master Data Syncing Summary - "+ schDetails.getScheduleName()+" for "+ schDetails.getMarket()+" Market";
							
							boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, fromMailid);
							if(sendEmail==true)
							{
								logger.info("generateMasterDataSyncNotificationEmail() :: Notification Sent Successfully."); 
							}
							else
							{
								logger.info("generateMasterDataSyncNotificationEmail() :: Failed to Send Email Notification.");
							}
							subject = null;
							templateString = null;
							toList = null;
							
						}
					}
					itemsList=  null;
					hostName = null;
				}
				schDetails = null;
			}
			else
			{
				logger.info("generateMasterDataSyncNotificationEmail :: Schedule Code as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NotificationEmailHelper.class.getName(), "generateMasterDataSyncNotificationEmail()", e);
		}
	}

}
