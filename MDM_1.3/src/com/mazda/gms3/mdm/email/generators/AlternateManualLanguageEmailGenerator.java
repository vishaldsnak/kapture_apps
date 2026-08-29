package com.mazda.gms3.mdm.email.generators;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.SendMailUsingAuthentication;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.RuleDetails;

public class AlternateManualLanguageEmailGenerator {

	static Logger logger = LogManager.getLogger(AlternateManualLanguageEmailGenerator.class);
	
	
	private static String toMailid=ApplicationProperties.getProperty("NOTIF_EMAIL_TO_ID");
	private static String fromMailid=ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID");
	
	public static void emailDataForNewRecord(RuleDetails ruleDetails, String wslId)
	{
		try
		{
			if(null==wslId || "".equals(wslId))
			{
				wslId = "Business User";
			}
			String templatePath = "/com/mazda/gms3/mdm/email/templates/AML_CREATE_HTML_TEMPLATE.html";
			InputStream is = AlternateManualLanguageEmailGenerator.class.getResourceAsStream(templatePath);
			if(null!=is)
			{
				String templateString = Utilities.readInputStramToString(is);
				if(null!=templateString && !"".equals(templateString))
				{
					/*
					 *  REPLACE THE FOLLOWING PLACE HOLDERS
					 *  KEY_LABEL
					 *  USER_WSL
					 *  SYSTEM_TIME
					 *  DATA_PLACEHOLDER
					 */
					MessageProperties msg = new MessageProperties(ApplicationProperties.getProperty("locales.values.english"));
					templateString =templateString.replace("KEY_LABEL", msg.getProperty("label.altmanuallanguage"));
					templateString = templateString.replace("USER_WSL", wslId);
					templateString = templateString.replace("SYSTEM_TIME", Utilities.fromatDateForEmail(new Date()));
					// PREPARE  NEW DATA
					StringBuilder newData = new StringBuilder();
					newData.append("<tr>");
					newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;\">1</td>");
					newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleName()+"</td>");
					newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleDesc()+"</td>");
					newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+msg.getProperty("flag.label.active")+"</td>");
					newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+Utilities.fromatDateForEmail(new Date())+"</td>");
					newData.append("</tr>");
					templateString = templateString.replace("DATA_PLACEHOLDER", newData.toString());
					newData = null;
					
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
					
					String subject = msg.addMessage("email.notification.subject.newrecord",msg.getProperty("label.altmanuallanguage"));
					boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, fromMailid);
					if(sendEmail==true)
					{
						logger.info("emailDataForNewRecord() :: Notification Sent Successfully."); 
					}
					else
					{
						logger.info("emailDataForNewRecord() :: Failed to Send Email Notification.");
					}
					subject = null;
					templateString = null;
					msg = null;
					toList = null;
				}
			}
			templatePath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguageEmailGenerator.class.getName(), "emailDataForNewRecord()", e);
		}
	}
	
	public static void emailDataForDeleteRecord(ArrayList<RuleDetails> ruleList, String wslId)
	{
		try
		{
			if(null==wslId || "".equals(wslId))
			{
				wslId = "Business User";
			}
			String templatePath = "/com/mazda/gms3/mdm/email/templates/AML_DELETE_HTML_TEMPLATE.html";
			InputStream is = AlternateManualLanguageEmailGenerator.class.getResourceAsStream(templatePath);
			if(null!=is)
			{
				String templateString = Utilities.readInputStramToString(is);
				if(null!=templateString && !"".equals(templateString))
				{
					/*
					 *  REPLACE THE FOLLOWING PLACE HOLDERS
					 *  KEY_LABEL
					 *  USER_WSL
					 *  SYSTEM_TIME
					 *  DATA_PLACEHOLDER
					 */
					MessageProperties msg = new MessageProperties(ApplicationProperties.getProperty("locales.values.english"));
					templateString =templateString.replace("KEY_LABEL", msg.getProperty("label.altmanuallanguage"));
					templateString = templateString.replace("USER_WSL", wslId);
					templateString = templateString.replace("SYSTEM_TIME", Utilities.fromatDateForEmail(new Date()));
					// PREPARE  NEW DATA
					StringBuilder newData = new StringBuilder();
					for(int i=0;i<ruleList.size();i++)
					{
						RuleDetails ruleDetails = (RuleDetails)ruleList.get(i);
						int srNo = i+1;
						newData.append("<tr>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+srNo+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleName()+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleDesc()+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+msg.getProperty("flag.label.delete")+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+Utilities.fromatDateForEmail(new Date())+"</td>");
						newData.append("</tr>");
					}
					templateString = templateString.replace("DATA_PLACEHOLDER", newData.toString());
					newData = null;
					
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
					String subject = msg.addMessage("email.notification.subject.deleterecord",msg.getProperty("label.altmanuallanguage"));
					boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, fromMailid);
					if(sendEmail==true)
					{
						logger.info("emailDataForDeleteRecord() :: Notification Sent Successfully."); 
					}
					else
					{
						logger.info("emailDataForDeleteRecord() :: Failed to Send Email Notification.");
					}
					subject = null;
					templateString = null;
					msg = null;
					toList = null;
				}
			}
			templatePath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguageEmailGenerator.class.getName(), "emailDataForDeleteRecord()", e);
		}
	}

	public static void emailDataForUpdateRecord(ArrayList<RuleDetails> ruleList, String wslId)
	{
		try
		{
			if(null==wslId || "".equals(wslId))
			{
				wslId = "Business User";
			}
			String templatePath = "/com/mazda/gms3/mdm/email/templates/AML_UPDATE_HTML_TEMPLATE.html";
			InputStream is = AlternateManualLanguageEmailGenerator.class.getResourceAsStream(templatePath);
			if(null!=is)
			{
				String templateString = Utilities.readInputStramToString(is);
				if(null!=templateString && !"".equals(templateString))
				{
					/*
					 *  REPLACE THE FOLLOWING PLACE HOLDERS
					 *  KEY_LABEL
					 *  USER_WSL
					 *  SYSTEM_TIME
					 *  DATA_PLACEHOLDER
					 */
					MessageProperties msg = new MessageProperties(ApplicationProperties.getProperty("locales.values.english"));
					templateString =templateString.replace("KEY_LABEL", msg.getProperty("label.altmanuallanguage"));
					templateString = templateString.replace("USER_WSL", wslId);
					templateString = templateString.replace("SYSTEM_TIME", Utilities.fromatDateForEmail(new Date()));
					// PREPARE  NEW DATA
					StringBuilder newData = new StringBuilder();
					for(int i=0;i<ruleList.size();i++)
					{
						RuleDetails ruleDetails = (RuleDetails)ruleList.get(i);
						int srNo = i+1;
						newData.append("<tr>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;\">"+srNo+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleName()+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getRuleDesc()+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+ruleDetails.getFlagLabel()+"</td>");
						newData.append("<td style=\"font-size:12px;color:#5C5B65;border-top:1px solid #DADADA;border-left: 1px solid #DADADA;\">"+Utilities.fromatDateForEmail(new Date())+"</td>");
						newData.append("</tr>");
					}
					templateString = templateString.replace("DATA_PLACEHOLDER", newData.toString());
					newData = null;
					
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
					String subject = msg.addMessage("email.notification.subject.updaterecord",msg.getProperty("label.altmanuallanguage"));
					boolean sendEmail = SendMailUsingAuthentication.newPostHTMLMail(toList, subject, templateString, fromMailid);
					if(sendEmail==true)
					{
						logger.info("emailDataForUpdateRecord() :: Notification Sent Successfully."); 
					}
					else
					{
						logger.info("emailDataForUpdateRecord() :: Failed to Send Email Notification.");
					}
					subject = null;
					templateString = null;
					msg = null;
					toList = null;
				}
			}
			templatePath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguageEmailGenerator.class.getName(), "emailDataForUpdateRecord()", e);
		}
	}

}
