package com.mazda.gms3.dmt.conversion.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientException;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.im.ito.CategoryITO;
import com.inquira.im.ito.CategoryKeyITO;
import com.inquira.im.ito.ContentChannelDataITO;
import com.inquira.im.ito.ContentRecordITO;
import com.inquira.im.ito.RepositoryKeyITO;
import com.inquira.im.ito.UserDataITO;
import com.inquira.im.ito.UserGroupITO;
import com.inquira.im.ito.UserGroupKeyITO;
import com.inquira.im.ito.ViewKeyITO;
import com.inquira.im.ito.impl.ContentRecordITOImpl;
import com.inquira.util.ewr.ErrorRecord;
import com.inquira.util.ewr.ErrorWarningResponse;
import com.mazda.gms3.dmt.conversion.utils.SpecialCharactersUtils;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.UserGroupDetails;

public class CreateContentImpl {

	private Logger logger = LogManager.getLogger(CreateContentImpl.class);
	
	private IQServiceClient client=null;
	
	private int authTokenGenerationCount=0;
	
	private int authCheckCount = 0;

	private ArrayList<CategoryDetails> missingCategoriesList = new ArrayList<CategoryDetails>();

	public void startConversion(String scheduleId,String wslId)
	{
		logger.info("StartMCConversionImpl :: Method Starts.");
		try
		{
			// SET WSL ID TO LOWERCASE
			wslId = wslId.toLowerCase();
			/*
			 * CALL FUNCTION TO CREATE CONTENT  HERE
			 */
			ContentDetails contentDetails = new ContentDetails();
			/*
			 * SET DATA IN THE REQUIRED VARIABLES OF CONTENTDETAILS CLASS OBJECT AND PASS IT TO CREATECONTENT METHOD
			 */
			createContent(contentDetails);
			contentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), "startConversion()", e);
		}
		finally
		{
			logger.info(" ******************** StartMCConversionImpl :: FOR SCHEDULE ID {"+scheduleId+"}. GENERATED AUTH TOKEN COUNTS ARE :: > "+ authTokenGenerationCount);
			logger.info(" ******************** StartMCConversionImpl :: FOR SCHEDULE ID {"+scheduleId+"}. TOTAL OPERATION COUNTS ARE :: > "+ authCheckCount);
			if(null!=client)
			{
				logger.info(" ******************** StartMCConversionImpl :: CLOSING IQ SERVICE CLIENT OBJECT. ***********************");
				client.close();
			}
			client = null;
			authTokenGenerationCount=0;
			authCheckCount=0;
		}
	}
	
	/**
	 * Function will perform Create Content Operation in IM
	 * @param contentDetails
	 * @return
	 */
	public ContentDetails createContent(ContentDetails contentDetails) 
	{
		String operationType="CREATE_CONTENT";
		try 
		{
			contentDetails.setOperationType(operationType);
			// create Object of NewContentRecordRequest
			ContentRecordITOImpl content = new ContentRecordITOImpl();
			// GET Channel RefKey and GUID
			content = addChannel(content, contentDetails, operationType);

			// GET VIEW RefKey and GUID
			content = addViews(content, contentDetails, operationType);

			// GET LOCALE REF KEY
			content = addLocale(content, contentDetails, operationType);

			// GET REPOSITORY RefKey and GUID
			content= addRepository(content, contentDetails, operationType);

			//Add Users & Owners
			content = addUsersAndOwners(content, contentDetails, operationType);

			// add UserGroups
			content = addUserGroups(content, contentDetails, operationType);
			
			if (null != contentDetails.getCategoryList() && contentDetails.getCategoryList().size() > 0) 
			{
				List<CategoryKeyITO> categories = new ArrayList<CategoryKeyITO>();
				for (int i = 0; i < contentDetails.getCategoryList().size(); i++) 
				{		
					CategoryDetails catDetails = (CategoryDetails) contentDetails.getCategoryList().get(i);
					if (null != catDetails.getCategoryRefKey() && !"".equals(catDetails.getCategoryRefKey() )) 
					{
						CategoryITO catITO  = addCategoryToDocument(catDetails.getCategoryRefKey(), operationType, contentDetails, catDetails);
						if (null != catITO	&& null != catITO.getReferenceKey() && !"".equals(catITO.getReferenceKey())) 
						{
							// add to categories
							categories.add(catITO);
						}
						catITO = null;
					}
					catDetails = null;
				}

				if (null != categories && categories.size() > 0) 
				{
					content.setCategories(categories);
				}
				categories = null;
			}

			/*
			 * SET 10 MINS BEFORE THE CURRENT DATE
			 */
			Date systemDate = new Date();
			long time = systemDate.getTime();
			time = time - 600000;
			Date newDate = new Date(time);

			// SET DISPLAY START DATE FOR THE DOCUMENT
			content.setDisplayStartDate(newDate);
			newDate = null;
			systemDate=  null;

			if(contentDetails.getChannelName().equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME")))
			{
				// CREATE CONTENT XML
				StringBuilder str = new StringBuilder();
				content.setXml(CreateContentImpl.createServiceManualsContentXML(contentDetails,str).toString());
				str=null;
			}
			else if(contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
			{
				// CREATE CONTENT XML
				StringBuilder str = new StringBuilder();
				content.setXml(CreateContentImpl.createWiringDiagramsContentXML(contentDetails, str).toString());
				str = null;
			}
			else if(contentDetails.getChannelName().equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME")))
			{
				// CREATE CONTENT XML
				StringBuilder str = new StringBuilder();
				content.setXml(CreateContentImpl.createOtherServiceManualsContentXML(contentDetails,str).toString());
				str=null;
			}

			// DO NOT ADD META XML
			content.setMetaDataXml("<META></META>");

			boolean pubFlag = true;
			if (contentDetails.getImProcessingStatus().equals(ApplicationProperties.getProperty("flag.value.publish"))) 
			{
				// publish document
				pubFlag = true;
			} 
			else
			{
				// unpublish document
				pubFlag = false;
			}

			// CAL FUNCTION TO CREATE CONTENT
			ContentRecordITO crIto  = null;
			try
			{
				crIto = client.getContentRecordRequest().createContent(content, pubFlag);
			}
			catch(Exception e)
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(contentDetails);
				crIto = client.getContentRecordRequest().createContent(content, pubFlag);
			}

			if (null != crIto && null != crIto.getDocumentID() && !"".equals(crIto.getDocumentID())) 
			{
				logger.info("createContent :: Document created successfully for File {"+ contentDetails.getFilePath() + "} :: Document Id :: > " + crIto.getDocumentID());
				logger.info(" Document Id :: > "	+ crIto.getDocumentID());
				logger.info(" Content Id :: > "	+ crIto.getContentID());
				logger.info(" Version :: > "		+ crIto.getMajorVersion());
				logger.info(" Resource Path :: > "	+ crIto.getResourcePath());
				logger.info(" Doc Status :: > "		+ crIto.getPublished());

				/*
				 * Update the Content VO
				 */
				contentDetails.setImDocumentId(crIto.getDocumentID());
				contentDetails.setImContentType(crIto.getContentID());

				// SET REPORT UPDATED VERSION 
				contentDetails.setReportUdatedVersion(String.valueOf(crIto.getMajorVersion()));

				if (crIto.getPublished() == true) 
				{
					// SET PUBLISH
					contentDetails.setImDocStatus(ApplicationProperties.getProperty("flag.value.publish"));
				} else {
					// SET UN-PUBLISH
					contentDetails.setImDocStatus(ApplicationProperties.getProperty("flag.value.draft"));
				}

				contentDetails.setImVersion(String.valueOf(crIto.getMajorVersion()));
				contentDetails.setImResourcePath(crIto.getResourcePath());
			}
			else 
			{
				// explicity set CONTENT TYPE AS NULL
				contentDetails.setImContentType(null);
				logger.info("createContent :: Failed to Generate Document for File :: >"+ contentDetails.getFilePath());
			}
			crIto = null;
		} 
		catch (IQServiceClientException e) 
		{
			// Explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType,contentDetails);
			}
			logger.info("createContent :: Error :: >" + e.getMessage());
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		} catch (Exception e) 
		{
			// Explicitly set CONTENT TYPE AS NULL
			contentDetails.setImContentType(null);
			logger.info("createContent :: Error :: >" + e.getMessage());
			capturreOtherErrorDetails(operationType, contentDetails, e);
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		} 
		return contentDetails;
	}
	
	
	/**
	 * Function will check whether TOKEN IS VALID OR NOT 
	 * IF NOT VALID = THEMN WILL GENERATE THE NEW TOKEN AND SET IT IN SCOPE
	 * @param contentDetails
	 */
	private void getConnectionWithIM(ContentDetails contentDetails) {
		authCheckCount++;
		boolean establishConnnection = false;
		try
		{
			if(null==client || null==client.getAuthenticationToken() || "".equals(client.getAuthenticationToken()) || client.isValid()==false)
			{
				logger.info("getConnectionWithIM :: AUTHENTICATION TOKEN IS INVALID / EXPIRED. RE-INITIATE CLIENT AND GET NEW TOKEN.");
				establishConnnection = true;
			}
			
			RepositoryKeyITO rk = client.getRepositoryRequest().getRepositoryKeyByReferenceKey(ApplicationProperties.getProperty("REPOSITORY").toUpperCase());
			if(null!=rk)
			{
				logger.info("getConnectionWithIM :: REPOSITORY INFO RECEIVED FETCHED REPOSITORY IS ::: > " + rk.getRecordID());
			}
			rk = null;
		}
		catch(Exception e)
		{
			logger.info("getConnectionWithIM :: EXCEPTION WHILE VALIDATING AUTHENTICATION TOKEN. RE-INITIATE CLIENT AND GET NEW TOKEN.");
			establishConnnection=true;
		}
		
		if(establishConnnection==true)
		{
			authTokenGenerationCount++;
			logger.info("getConnectionWithIM :: GENERATING NEW TOKEN FOR SINCE, EITHER CLIENT IS NULL OR AUTHENTICATION TOKEN IS EXPIRED.");
			if(null!=client)
			{
				client.close();
			}
			client = null;
			
			/* 
			 * CHANGE DATE - 21 SEPTEMBER 2017
			 * FINAL USER FOR GENERATING TOKEN WILL ALWAYS BY DEFAULT USER -OKADMIN
			 */
			String finalUserToSet="";
			if(null!=contentDetails && null!=contentDetails.getWslId() && !"".equals(contentDetails.getWslId()))
			{
				finalUserToSet= contentDetails.getWslId().trim().toLowerCase();
			}
			else
			{
				// SINCE WSL IS NULL - SET DEFAULT USER  - OKADMIN
				finalUserToSet = ApplicationProperties.getProperty("USERNAME");
			}
			String internalUsers = ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS");
			if(null!=internalUsers && !"".equals(internalUsers))
			{
				if(internalUsers.contains(finalUserToSet.trim().toLowerCase()))
				{
					// set as OKADMIN
					finalUserToSet = ApplicationProperties.getProperty("USERNAME");
				}
			}
			internalUsers = null;
			
			try {
				client= IQServiceClientManager
						.connect(
								finalUserToSet,
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
				Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), "getConnectionWithIM()", e);
				if(null==contentDetails )
				{
					contentDetails = new ContentDetails();
				}
				capturreOtherErrorDetails("GET_CONNECTION_WITH_IM", contentDetails, e);
			}
			finalUserToSet = null;
		}
		
		logger.info("getConnectionWithIM() :: CLIENT :: >" + client);
		if(null!=client)
		{
			logger.info("getConnectionWithIM() :: AUTHENTICATION TOKEN :: >" + client.getAuthenticationToken());
			logger.info("getConnectionWithIM() :: IS VALID :: >" + client.isValid());
		}
	}
	
	/**
	 * FUNCTION WILL ADD CHANNEL INFROMATION WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addChannel(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
			{
				// Get Channel RefKey and GUID
				String channelRefKey = contentDetails.getChannelName().replace(" ","_");
				ContentChannelDataITO channelDataITO = null;
				try
				{
					channelDataITO = client.getContentChannelRequest().getContentChannelByReferenceKey(channelRefKey.toUpperCase());
				}
				catch(Exception e)
				{
					// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
					getConnectionWithIM(contentDetails);
					channelDataITO = client.getContentChannelRequest().getContentChannelByReferenceKey(channelRefKey.toUpperCase());
				}
				if(null!=channelDataITO && null!=channelDataITO.getReferenceKey() && !"".equals(channelDataITO.getReferenceKey()))
				{
					content.setChannel(channelDataITO);
				}
				channelRefKey = null;
				channelDataITO = null;
			}
		}
		catch(IQServiceClientException e)
		{
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType, contentDetails);
			}
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}
	
	/**
	 * FUNCTION WILL ADD VIEW INFROMATION WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addViews(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			// GET VIEW RefKey and GUID
			List<ViewKeyITO> views = new ArrayList<ViewKeyITO>();
			ViewKeyITO viewITO = null;
			try
			{
				viewITO= client.getViewRequest().getViewKeyByReferenceKey(ApplicationProperties.getProperty("VIEW_MC").toUpperCase());
			}
			catch(Exception e)
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(contentDetails);
				viewITO= client.getViewRequest().getViewKeyByReferenceKey(ApplicationProperties.getProperty("VIEW_MC").toUpperCase());
			}
			
			if(null!=viewITO && null!=viewITO.getReferenceKey() && !"".equals(viewITO.getReferenceKey()))
			{
				views.add(viewITO);
			}
			if(null!=views && views.size()>0)
			{
				content.setViews(views);
			}
			views = null;
			viewITO = null;
		}
		catch(IQServiceClientException e)
		{
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType, contentDetails);
			}
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}
	
	/**
	 * FUNCTION WILL ADD REPOSITORY INFORMATION WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addRepository(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			// GET REPOSITORY RefKey and GUID
			try
			{
				content.setRepository(client.getRepositoryRequest().getRepositoryKeyByReferenceKey(ApplicationProperties.getProperty("REPOSITORY").toUpperCase()));
			}
			catch(Exception e)
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(contentDetails);
				content.setRepository(client.getRepositoryRequest().getRepositoryKeyByReferenceKey(ApplicationProperties.getProperty("REPOSITORY").toUpperCase()));
			}
		}
		catch(IQServiceClientException e)
		{
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType, contentDetails);
			}
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}
	
	/**
	 * FUNCTION WILL ADD LOCALE INFROMATION WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addLocale(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			// GET LOCALE REF KEY
			String locale = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
			try
			{
				content.setLocale(client.getLocaleRequest().getLocaleKeyByLocaleCode(locale));
			}
			catch(Exception e)
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(contentDetails);
				content.setLocale(client.getLocaleRequest().getLocaleKeyByLocaleCode(locale));
			}
			locale=null;
		}
		catch(IQServiceClientException e)
		{
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType, contentDetails);
			}
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}
	
	/**
	 * FUNCTION WILL ADD USER GROUPS WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addUserGroups(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			if(null!=contentDetails.getUserGroupsList() && contentDetails.getUserGroupsList().size()>0)
			{
				List<UserGroupKeyITO> userGroupsList = new ArrayList<UserGroupKeyITO>();
				for(int i=0;i<contentDetails.getUserGroupsList().size();i++)
				{
					UserGroupDetails ugDetails =(UserGroupDetails)contentDetails.getUserGroupsList().get(i);
					UserGroupITO ugITO = null;
					try 
					{
						ugITO = client.getUserGroupRequest().getUserGroupByReferenceKey(ugDetails.getUserGroupRefKey());
					} 
					catch (Exception e) 
					{
						// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
						getConnectionWithIM(contentDetails);
						try
						{
							ugITO = client.getUserGroupRequest().getUserGroupByReferenceKey(ugDetails.getUserGroupRefKey());
						}
						catch(IQServiceClientException e1)
						{
							if(null!=client)
							{
								capturreErrorDetails(client.getEWR(), operationType, contentDetails);
							}
							Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e1);
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e1);
							capturreOtherErrorDetails(operationType, contentDetails, e1);
						}
					}
					
					if (null != ugITO	&& null != ugITO.getReferenceKey() && !"".equals(ugITO.getReferenceKey())) 
					{
						// add to userGroupsList
						userGroupsList.add(ugITO);
					}
					ugITO = null;
				}
				
				if(null!=userGroupsList && userGroupsList.size()>0)
				{
					// set in Content
					content.setUserGroups(userGroupsList);
				}
				userGroupsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}
	
	/**
	 * FUNCTION WILL ADD USERS & OWNER INFORMATION WITH THE DOCUMENT
	 * @param content
	 * @param contentDetails
	 * @param operationType
	 * @return
	 */
	private ContentRecordITOImpl addUsersAndOwners(ContentRecordITOImpl content, ContentDetails contentDetails, String operationType)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getWslId() && !"".equals(contentDetails.getWslId()))
			{
				/*
				 * CHECK HERE IF WSL ID IS AMONG ANY OF THE INTERNAL USERS
				 */
				String finalUserToSet=contentDetails.getWslId().trim().toLowerCase();
				String internalUsers = ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS");
				if(null!=internalUsers && !"".equals(internalUsers))
				{
					if(internalUsers.contains(contentDetails.getWslId().trim().toLowerCase()))
					{
						// set as OKADMIN
						finalUserToSet = ApplicationProperties.getProperty("USERNAME");
					}
				}
				internalUsers = null;
				
				// Set Users and Owners
				UserDataITO userDataITO = null;
				try
				{
					userDataITO = client.getUserRequest().getUserDataByLogin(finalUserToSet);
				}
				catch(Exception e)
				{
					// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
					getConnectionWithIM(contentDetails);
					userDataITO = client.getUserRequest().getUserDataByLogin(finalUserToSet);
				}
				
				if(null!=userDataITO && null!=userDataITO.getRecordID())
				{
					// set content Owner
					content.setContentOwner(userDataITO);
					// set Owner Id
					content.setOwnerID(userDataITO.getRecordID());
					// update Author Id
					content.setAuthorID(userDataITO.getRecordID());
					// update Author Name
					String name="";
					if(null!=userDataITO.getFirstName() && !"".equals(userDataITO.getFirstName()))
					{
						name = userDataITO.getFirstName();
					}
					name = name+" ";
					if(null!=userDataITO.getLastName() && !"".equals(userDataITO.getLastName()))
					{
						name = name+userDataITO.getLastName();
					}
					name = name.trim();
					content.setAuthorName(name);
					// update user Id
					content.setUserID(userDataITO.getRecordID());
				}
				
				finalUserToSet=  null;
				userDataITO = null;
			}
		}
		catch(IQServiceClientException e)
		{
			if(null!=client)
			{
				capturreErrorDetails(client.getEWR(), operationType, contentDetails);
			}
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e);
			capturreOtherErrorDetails(operationType, contentDetails, e);
		}
		return content;
	}

	/**
	 * FUNCTION WILL ADD CATEGORY INFORMATION WITH THE DOCUMENT
	 * @param refKey
	 * @param operationType
	 * @param contentDetails
	 * @param catDetails
	 * @return
	 */
	private CategoryITO addCategoryToDocument(String refKey, String operationType, ContentDetails contentDetails, CategoryDetails catDetails)
	{
		CategoryITO catITO = null;
		if(null!=refKey && !"".equals(refKey))
		{
			boolean addToError = false;
			try 
			{
				catITO = client.getCategoryRequest().getCategoryByReferenceKey(refKey);
			} 
			catch (Exception e) 
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM(contentDetails);
				try
				{
					catITO = client.getCategoryRequest().getCategoryByReferenceKey(refKey);
				}
				catch (IQServiceClientException e1) 
				{
					logger.info("addCategoryToDocument :: {"+operationType+"} ::  Failed to IDENTIFY REF KEY FROM IM FOR CATEGORY KEY :: > " + catDetails.getCategoryRefKey());
					addToError = true;
					if(null!=client)
					{
						capturreErrorDetails(client.getEWR(), operationType,contentDetails);
					}
					logger.info("addCategoryToDocument :: {"+operationType+"} :: Error :: >" + e1.getMessage());
					Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e1);
				}
				catch(Exception e1)
				{
					addToError = true;
					// STILL GETTING EXCEPTION - CATCH IT.
					logger.info("addCategoryToDocument :: {"+operationType+"} ::  Failed to IDENTIFY REF KEY FROM IM FOR CATEGORY KEY :: > " + catDetails.getCategoryRefKey());
					Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), operationType, e1);
					// also AddDocument to failureList
					capturreOtherErrorDetails(operationType, contentDetails, e1);
				}
			}
			
			if(addToError == true)
			{
				/*
				 * add to missing Categories List
				 */
				catDetails.setFilePath(contentDetails.getFilePath());
				// add operationType
				catDetails.setOperationType(operationType);
				// add ContentDetails
				catDetails.setContentDetails(contentDetails);
				// add catDetails to missingCategoriesList
				if (null == missingCategoriesList || missingCategoriesList.size() <= 0) 
				{
					missingCategoriesList = new ArrayList<CategoryDetails>();
				}
				missingCategoriesList.add(catDetails);
			}
		}
		return catITO;
	}

	
	/**
	 * Function will CAPTURE IM EXCEPTION DETAILS.
	 * @param ewr
	 * @param operationType
	 * @param contentDetails
	 * @return
	 */
	private boolean capturreErrorDetails(ErrorWarningResponse ewr,String operationType, ContentDetails contentDetails) 
	{
		boolean result = true;
		if (null!=ewr && ewr.hasErrorsOrWarnings()) 
		{
			// EWR reported a problem, check to see if it is an error
			if (ewr.hasErrors()) 
			{
				// error was reported
				result = true;
				// output the errors
				List<ErrorRecord> errors = ewr.getErrors();
				for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
					ErrorRecord rec = iter.next();
					logger.info("checkEWR :: {" + operationType+ "} :: Error Code :: >" + rec.getCode());
					logger.info("checkEWR :: {" + operationType	+ "} :: Error Message :: >" + rec.getMessage());
					rec = null;
				}
				errors = null;
			} 
			else 
			{
				// warning must have been reported, assume it is safe to go on
				result = false;
			}
		} 
		else 
		{
			result = false;
		}
		return result;
	}

	/**
	 * Function will CAPTURE OTHER EXCEPTION DETAILS
	 * @param operationType
	 * @param contentDetails
	 * @param e
	 */
	private void capturreOtherErrorDetails(String operationType, ContentDetails contentDetails, Exception e) 
	{
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		e.printStackTrace(print);
		String errorCode = e.getMessage();
		String errorMessage = writer.toString();

		logger.info("captureOtherError :: {" + operationType+ "} :: Error Code :: >" + errorCode);
		logger.info("captureOtherError :: {" + operationType	+ "} :: Error Message :: >" + errorMessage);

		// set errorCode to null
		errorCode = null;
		// set errorMessage to null
		errorMessage = null;
		// set writer & print to null
		writer = null;
		// set print to null
		print = null;
	}
	
	
	/**
	 * <SERVICE_MANUALS>
	 * 	<TITLE>
	 * 		<SERVICE_MANUAL_TYPE></SERVICE_MANUAL_TYPE>
	 * 		<CONTENT></CONTENT>
	 * 		<ATTACHMENTS>
	 * 			<ATTACHMENT_TITLE>
	 * 			<ATTACHMENT>
	 * 		</ATTACHMENTS>
	 * 		<VTOC_FILENAME>
	 * 		<SIE_ID>
	 * </SERVICE_MANUALS>
	 * @param contentVO
	 * @return
	 */
	public static StringBuilder createServiceManualsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"
						+ contentVO.getChannelName().toUpperCase()
								.replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					/*
					 * also replace <SUP> TAG BOTH OPENING & CLOSING
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					
					
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				}
				str.append("]]></TITLE>");
				
				str.append("<CONTENT><![CDATA[");
				if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
				{
					str.append(contentVO.getDocumentContent());
				}
				str.append("]]></CONTENT>");

				str.append("<VTOC_FILENAME><![CDATA[");
				if (null != contentVO.getFileNameAttribute() 	&& !"".equals(contentVO.getFileNameAttribute())) 
				{
					str.append(contentVO.getFileNameAttribute());
				}
				str.append("]]></VTOC_FILENAME>");
				
				str.append("<SIE_ID><![CDATA[");
				if (null != contentVO.getFileName() 	&& !"".equals(contentVO.getFileName())) 
				{
					str.append(contentVO.getFileName());
				}
				str.append("]]></SIE_ID>");
				
				str.append("<DJVU_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
				{
					str.append(contentVO.getUploadDirectoryPath());
				}
				str.append("]]></DJVU_FILE_LOCATION>");
				
				
				str.append("<OASIS_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getOasisDirectoryPath() && !"".equals(contentVO.getOasisDirectoryPath()))
				{
					str.append(contentVO.getOasisDirectoryPath());
				}
				str.append("]]></OASIS_FILE_LOCATION>");
				
				
				
				if (contentVO.getDocumentType().equals(ContentDetails.PDF_DOCUMENT))
				{
					// add Attachment
					str.append("<ATTACHMENTS>");
					str.append("<ATTACHMENT_TITLE><![CDATA[");
					if(null!=contentVO.getTitle() && !"".equals(contentVO.getTitle()))
					{
						str.append(contentVO.getTitle());
					}
					str.append("]]></ATTACHMENT_TITLE>");
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
					str.append("</ATTACHMENTS>");
				}
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), "createServiceManualsContentXML()", e);
		}
		return str;
	}

	/**
	 *  <WIRING_DIAGRAMS>
	 * 	<TITLE>
	 * 	<NAVIGATION>
	 * 	<HTML_CONTENT>
	 * 	<FLASH_CONTENT>
	 * 	<HTML5_FILE_NAME>
	 * 	<HTML5_UPLOAD_DIRECTORY>
	 * 	<HTML5_FONT_SELECTION>
	 * 	<ATTACHMENT>
	 * 
	 * </WIRING_DIAGRAMS>
	 * @param contentVO
	 * @return
	 */
	public static StringBuilder createWiringDiagramsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 */
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());

					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					
					/*
					 * REPLACE SUP BOTH OPENING & CLOSING TAGS
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				
				}
				str.append("]]></TITLE>");
				
				/*
				 * DO NOT SEND ANYTHING IN HTML_CONTENT
				 * FOR HTML5 DOCUMENTS.
				 * NOT FOR PDFS
				 */
				str.append("<HTML_CONTENT><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML_DOCUMENT) 
						|| contentVO.getDocumentType().equals(ContentDetails.ENT_DOCUMENT))
				{
					if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
					{
						str.append(contentVO.getDocumentContent());
					}
				}
				str.append("]]></HTML_CONTENT>");

				str.append("<FLASH_CONTENT><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT))
				{
					if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
					{
						str.append(contentVO.getDocumentContent());
					}
				}
				str.append("]]></FLASH_CONTENT>");
				
				
				str.append("<NAVIGATION><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if(null!=contentVO.getNavigation() && !"".equals(contentVO.getNavigation()))
					{
						str.append(contentVO.getNavigation());
					}
				}
				str.append("]]></NAVIGATION>");
				
				str.append("<HTML5_FILE_NAME><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if (null != contentVO.getFileName() && !"".equals(contentVO.getFileName())) 
					{
						str.append(contentVO.getFileName());
					}
				}
				str.append("]]></HTML5_FILE_NAME>");
				
				
				str.append("<HTML5_UPLOAD_DIRECTORY><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
					{
						str.append(contentVO.getUploadDirectoryPath());
					}
				}
				str.append("]]></HTML5_UPLOAD_DIRECTORY>");
				
				str.append("<HTML5_FONT_SELECTION><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT) || 
						contentVO.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT) || 
						contentVO.getDocumentType().equals(ContentDetails.HTML_MC_DOCUMENT))
				{
					if (null != contentVO.getFontContent() 	&& !"".equals(contentVO.getFontContent())) 
					{
						str.append(contentVO.getFontContent());
					}
				}
				str.append("]]></HTML5_FONT_SELECTION>");
				
				if(null!=contentVO.getHtml5FileSourcePath() && !"".equals(contentVO.getHtml5FileSourcePath()))
				{
					// WHEN ONLY HTML5 DOCUMENT - THE VALUE WILL COME
					str.append("<DJVU_FILE_LOCATION><![CDATA[");
					if(null!=contentVO.getHtml5FileSourcePath() && !"".equals(contentVO.getHtml5FileSourcePath()))
					{
						str.append(contentVO.getHtml5FileSourcePath());
					}
					str.append("]]></DJVU_FILE_LOCATION>");
				}
				// WHEN NOT HTML5 DOCUMENT
				if(!contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					str.append("<DJVU_FILE_LOCATION><![CDATA[");
					if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
					{
						str.append(contentVO.getUploadDirectoryPath());
					}
					str.append("]]></DJVU_FILE_LOCATION>");
				}
				
				// NO NEED OF CHECKING PDF_DOCUMENT
				if(null!=contentVO.getPdfFileNameAsAttachment() && !"".equals(contentVO.getPdfFileNameAsAttachment()))
				{
					// add Attachment -- add Sizd as Attribute
					/*					long length=0;
									if(null!=contentVO.getFileData())
									{
										length= contentVO.getFileData();
					 *///					}
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
				}
				else
				{
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					str.append("]]>");
					str.append("</ATTACHMENT>");
				}
				
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), "createWiringDiagramsContentXML()", e);
		}
		return str;
	}

	/**
	 * Function will generate SCHEMA XML FOR OTHER SERVICE MANUALS CHANNEL
	 * @param contentVO
	 * @param str
	 * @return
	 */
	public static StringBuilder createOtherServiceManualsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"
						+ contentVO.getChannelName().toUpperCase()
								.replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					/*
					 * also replace <SUP> TAG BOTH OPENING & CLOSING
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					
					
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				}
				str.append("]]></TITLE>");
				
				str.append("<CONTENT><![CDATA[");
				if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
				{
					str.append(contentVO.getDocumentContent());
				}
				str.append("]]></CONTENT>");

				str.append("<DJVU_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
				{
					str.append(contentVO.getUploadDirectoryPath());
				}
				str.append("]]></DJVU_FILE_LOCATION>");
				
				str.append("<OASIS_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getOasisDirectoryPath() && !"".equals(contentVO.getOasisDirectoryPath()))
				{
					str.append(contentVO.getOasisDirectoryPath());
				}
				str.append("]]></OASIS_FILE_LOCATION>");
				
				if (contentVO.getDocumentType().equals(ContentDetails.PDF_DOCUMENT))
				{
					// add Attachment
					str.append("<ATTACHMENTS>");
					str.append("<ATTACHMENT_TITLE><![CDATA[");
					if(null!=contentVO.getTitle() && !"".equals(contentVO.getTitle()))
					{
						str.append(contentVO.getTitle());
					}
					str.append("]]></ATTACHMENT_TITLE>");
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
					str.append("</ATTACHMENTS>");
				}
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CreateContentImpl.class.getName(), "createOtherServiceManualsContentXML()", e);
		}
		return str;
	}


}