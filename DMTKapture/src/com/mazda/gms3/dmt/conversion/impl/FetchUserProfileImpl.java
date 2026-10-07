package com.mazda.gms3.dmt.conversion.impl;


import org.apache.log4j.Logger;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.im.ito.RepositoryKeyITO;
import com.inquira.im.ito.UserITO;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;

public class FetchUserProfileImpl {

	private Logger logger = Logger.getLogger(FetchUserProfileImpl.class);
	
	private IQServiceClient client=null;
	
	/**
	 * Function will Fetch the User Profile Details on the Basis of WSL ID.
	 * @param wslId
	 */
	public UserITO getUserProfileDetailsFromIM(String wslId)
	{
		UserITO userITO = null;
		try
		{
			if(null!=wslId && !"".equals(wslId))
			{
				try
				{
					userITO =  client.getUserRequest().getUserByLogin(wslId);
				}
				catch(Exception e)
				{
					// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
					getConnectionWithIM(ApplicationProperties.getProperty("USERNAME"));
					userITO =  client.getUserRequest().getUserByLogin(wslId);
				}
				
				
				/*
				 * SAMPLE CODE SNIPPETS FOR GETTING THE REQUIRED INFORMATION FORM USERITO Object
				 * e.g - DEFAULT LOCALE
				 * 		 CONTENT LOCALES
				 * 		 MAPPED ROLES
				 * 
				 * USE THESE CODE SNIPPETS AS REQUIRED.
				 *
				
				// FOR DEFAUT LOCALE
				String defaultLocale = null;
				LocaleKeyITO defaultLocaleKeyITO = userITO.getDefaultLocale();
				if(null!=defaultLocaleKeyITO && null!=defaultLocaleKeyITO.getRecordID() && !"".equals(defaultLocaleKeyITO))
				{
					defaultLocale = defaultLocaleKeyITO.getRecordID();
				}
//				System.out.println(" DEFAULT LOCALE FOR THE USER :: > " + defaultLocale);
				// HERE, OUTPUT WILL BE - en_US
				defaultLocaleKeyITO = null;
				defaultLocale = null;
				
				// FOR CONTENT LOCALES
				List<LocaleKeyITO> contentLocalesList = userITO.getContentLocales();
				if(null!=contentLocalesList && contentLocalesList.size()>0)
				{
					for(int i=0;i<contentLocalesList.size();i++)
					{
						LocaleKeyITO contentLocaleKeyITO = (LocaleKeyITO)contentLocalesList.get(i);
						// PRINT ALL MAPPED CONTENT LOCALES - 
						System.out.println(" CONTENT LOCALE :: > {"+i+"} :: > " + contentLocaleKeyITO.getRecordID());
						// HERE, OUTPUT WILL BE - en_CA / es_MX / ja_JP
						contentLocaleKeyITO = null;
					}
				}
				contentLocalesList = null;
				
				
				// FOR MAPPED ROLES
				List<SecurityRoleKeyITO> mappedRolesList = 	userITO.getSecurityRoles();
				if(null!=mappedRolesList && mappedRolesList.size()>0)
				{
					for(int i=0;i<mappedRolesList.size();i++)
					{
						SecurityRoleKeyITO mappedRoleKeyITO = (SecurityRoleKeyITO)mappedRolesList.get(i);
						// PRINT ALL MAPPED SECUTIRY ROLES - 
						System.out.println(" MAPPED ROLE :: > {"+i+"} :: > " + mappedRoleKeyITO.getRecordID());
						// HERE, OUTPUT WILL BE - MDM_SUPER_ADMIN / MAZDA_ACE_ADMIN / CEC_AUTHOR / CEC_PUBLISHER ETC.
						mappedRoleKeyITO = null;
					}
				}
				mappedRolesList = null;
				*/
			}
			else
			{
				logger.info("getUserProfileDetailsFromIM :: WSL ID as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchUserProfileImpl.class.getName(), "getUserProfileDetailsFromIM()", e);
		}
		finally
		{
			if(null!=client)
			{
				client.close();
			}
			client=null;
		}
		return userITO;
	}
	
	
	public void getConnectionWithIM(String wslId) {
		boolean establishConnnection = false;
		try
		{
			if(null==client || null==client.getAuthenticationToken() || "".equals(client.getAuthenticationToken()) || client.isValid()==false)
			{
//				logger.info("getConnectionWithIM :: AUTHENTICATION TOKEN IS INVALID / EXPIRED. RE-INITIATE CLIENT AND GET NEW TOKEN.");
				establishConnnection = true;
			}
			
			RepositoryKeyITO rk = client.getRepositoryRequest().getRepositoryKeyByReferenceKey(ApplicationProperties.getProperty("REPOSITORY").toUpperCase());
			if(null!=rk)
			{
//				logger.info("getConnectionWithIM :: REPOSITORY INFO RECEIVED FETCHED REPOSITORY IS ::: > " + rk.getRecordID());
			}
			rk = null;
		}
		catch(Exception e)
		{
//			logger.info("getConnectionWithIM :: EXCEPTION WHILE VALIDATING AUTHENTICATION TOKEN. RE-INITIATE CLIENT AND GET NEW TOKEN.");
			establishConnnection=true;
		}
		
		if(establishConnnection==true)
		{
//			logger.info("getConnectionWithIM :: GENERATING NEW TOKEN FOR SINCE, EITHER CLIENT IS NULL OR AUTHENTICATION TOKEN IS EXPIRED.");
			if(null!=client)
			{
				client.close();
			}
			client = null;
			
			try {
				client= IQServiceClientManager
						.connect(
								wslId,
										""
												+ ApplicationProperties
												.getProperty("PASSWORD") + "",
												""
														+ ApplicationProperties
														.getProperty("DOMAIN") + "",
														""
																+ ApplicationProperties
																.getProperty("REPOSITORY") + "",
																""
																		+ ApplicationProperties
																		.getProperty("IM_CLIENT_LIBRARY_ENDPOINT")
																		+ "",
																		""
																				+ ApplicationProperties
																				.getProperty("SEARCH_CLIENT_LIBRARY_ENDPOINT")
																				+ "", null, true);
			
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(FetchUserProfileImpl.class.getName(), "getConnectionWithIM()", e);
			}
		}
	}

}
