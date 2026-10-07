package com.mazda.gms3.dmt.utils;

/*
Some SMTP servers require a username and password authentication before you
can use their Server for Sending mail. This is most common with couple
of ISP's who provide SMTP Address to Send Mail.

This Program gives any example on how to do SMTP Authentication
(User and Password verification)
*/

import javax.mail.*;
import javax.mail.internet.*;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.sun.mail.smtp.SMTPAddressFailedException;

import java.util.*;

/**
  To use this program, change values for the following three constants,

    SMTP_HOST_NAME -- Has your SMTP Host Name
    SMTP_AUTH_USER -- Has your SMTP Authentication UserName
    SMTP_AUTH_PWD  -- Has your SMTP Authentication Password

  Next change values for fields

  emailMsgTxt  -- Message Text for the Email
  emailSubjectTxt  -- Subject for email
  emailFromAddress -- Email Address whose name will appears as "from" address

  Next change value for "emailList".
  This String array has List of all Email Addresses to Email Email needs to be sent to.


  Next to run the program, execute it as follows,

  SendMailUsingAuthentication authProg = new SendMailUsingAuthentication();

*/

public class SendMailUsingAuthentication
{
	private static Logger logger = LogManager.getLogger(SendMailUsingAuthentication.class);

	private static String smtpHost=ApplicationProperties.getProperty("SMTP_HOST_NAME");
	
	
	/**
	 * The Function is used to send Text Mail to End User
	 * @param toList
	 * @param subject
	 * @param message
	 * @throws MessagingException
	 */
	public static boolean newPostTextMail(ArrayList<String> toList, String subject,String message, String fromEmailId) 
	{
		boolean debug = false;
		try
		{
			/**
			 * Set the host smtp address
			 */
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);

			Session session = Session.getInstance(props);
			session.setDebug(debug);
			/**
			 * create a message
			 */
			MimeMessage msg = new MimeMessage(session);
			
			/**
			 * set the from and to address
			 */
			InternetAddress addressFrom = new InternetAddress(fromEmailId);
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[toList.size()];
			for (int i = 0; i < toList.size(); i++)
			{
				addressTo[i] = new InternetAddress(toList.get(i).toString());
			}

			msg.addRecipients(Message.RecipientType.TO, addressTo);

			/**
			 * Setting the Subject and Content Type
			 */
			msg.setSubject(subject);
			msg.setContent(message, "text/plain");
			/**
			 * Another Way Could be
			 * 	SMTPMessage smtp  = new SMTPMessage(session);
				smtp.setFrom(addressFrom);
				smtp.setSubject(subject);
				smtp.setContent(message, "text/plain");
				smtp.addRecipients(Message.RecipientType.TO,  addressTo);
				smtp.setReturnOption(SMTPMessage.RETURN_HDRS);
				smtp.setNotifyOptions(SMTPMessage.NOTIFY_DELAY|SMTPMessage.NOTIFY_FAILURE|SMTPMessage.NOTIFY_SUCCESS);
				Transport.send(smtp);
			 * 
			 */			  
			Transport.send(msg);
		
		}
		catch(SMTPAddressFailedException e)
		{
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostTextMail()", e);
			return false;
		}
		catch(SendFailedException e)
		{
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostTextMail()", e);
			return false;
		}
		catch (MessagingException mex) 
		{
			mex.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostTextMail()", mex);
			return false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostTextMail()", e);
			return false;
		}
		return true;
	}
	
	/**
	 * The function is used to send html mails to end users
	 * @param toList
	 * @param subject
	 * @param message
	 * @throws MessagingException
	 */
	public static boolean newPostHTMLMail(ArrayList<String> toList, String subject,String message, String fromEmailId) 
	{
		logger.info("newPostHTMLMail() :: Method Starts.");
		boolean debug = false;
		try
		{
			/**
			 * Get SMTP Server Details
			 */

			logger.info("newPostHTMLMail() :: Email Gateway Details are Not null, proceed for Sending Email.");
			/**
			 * Set the host smtp address
			 */
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);
			logger.info("newPostHTMLMail() :: Authentication is not required.");
			Session session = Session.getInstance(props);
			/*
			 * check here if session is not null, then only proceed 
			 * else return false
			 */
			if(null!=session)
			{
				session.setDebug(debug);
				/**
				 * create a message
				 */
				MimeMessage msg = new MimeMessage(session);
				// set Internet Address
				InternetAddress addressFrom = new InternetAddress(fromEmailId);
				msg.setFrom(addressFrom);

				/**
				 * set the to address
				 */

				InternetAddress[] addressTo = new InternetAddress[toList.size()];
				for (int i = 0; i < toList.size(); i++)
				{
					addressTo[i] = new InternetAddress(toList.get(i).toString());
				}

				msg.addRecipients(Message.RecipientType.TO, addressTo);

				/**
				 * Setting the Subject and Content Type
				 */
				msg.setSubject(subject);
				msg.setContent(message, "text/html");
				/**
				 * Another Way Could be
				 * 	SMTPMessage smtp  = new SMTPMessage(session);
					smtp.setFrom(addressFrom);
					smtp.setSubject(subject);
					smtp.setContent(message, "text/plain");
					smtp.addRecipients(Message.RecipientType.TO,  addressTo);
					smtp.setReturnOption(SMTPMessage.RETURN_HDRS);
					smtp.setNotifyOptions(SMTPMessage.NOTIFY_DELAY|SMTPMessage.NOTIFY_FAILURE|SMTPMessage.NOTIFY_SUCCESS);
					Transport.send(smtp);
				 * 
				 */
				Transport.send(msg);
				logger.info("newPostHTMLMail() :: Mail Sent Successfully.");
			}
			else
			{
				return false;
			}
		}
		catch(SMTPAddressFailedException e)
		{
			logger.info("newPostHTMLMail() :: SMTPAddressFailed Exception  :: >" + e.getMessage());
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostHTMLMail()", e);
			return false;
		}
		catch(SendFailedException e)
		{
			logger.info("newPostHTMLMail() :: SendFailed Exception  :: >" + e.getMessage());
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostHTMLMail()", e);
			return false;
		}
		catch (MessagingException mex) 
		{
			logger.info("newPostHTMLMail() :: Messaging Exception  :: >" + mex.getMessage());
			mex.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostHTMLMail()", mex);
			return false;
		}
		catch(Exception e)
		{
			logger.info("newPostHTMLMail() :: Exception  :: >" + e.getMessage());
			e.printStackTrace();
			Utilities.printStackTraceToLogs(SendMailUsingAuthentication.class.getName(), "newPostHTMLMail()", e);
			return false;
		}
		logger.info("newPostHTMLMail() :: Method Ends.");
		return true;
	}
}