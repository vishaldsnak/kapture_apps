package com.mazda.gms3.dmt.autosync.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;




import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientException;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.im.ito.CategoryITO;
import com.inquira.im.ito.CategoryKeyITO;
import com.inquira.im.ito.RepositoryKeyITO;
import com.inquira.im.ito.impl.CategoryITOImpl;
import com.inquira.util.ewr.ErrorRecord;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncTransactionDAO;
import com.mazda.gms3.dmt.autosync.utils.PrintReportUtils;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;

public class MasterDataSynchingImpl extends Thread{

	private Logger logger = LogManager.getLogger(MasterDataSynchingImpl.class);
	
	private MasterDataSyncTransactionDAO transactionDAO = null;
	
	private boolean errorsFound = false;
	
	private ArrayList<AutoSyncCategoryDetails> categoriesList = null;
	
	private ArrayList<AutoSyncCategoryDetails> failureList = null;
	
	private IQServiceClient client = null;
	
	private int authenticationTokenCount = 0;
	
	private PrintReportUtils printReportUtils = null;
	
	private MasterDataSyncDAO masterDataDAO = null;
	
	public void startSynching(AutoSyncScheduleDetails schDetails)
	{
		try
		{
			transactionDAO = new MasterDataSyncTransactionDAO();
			errorsFound=  false;
			categoriesList=  new ArrayList<AutoSyncCategoryDetails>();
			failureList =new ArrayList<AutoSyncCategoryDetails>();
			client=  null;
			authenticationTokenCount = 0;
			printReportUtils = new PrintReportUtils();
			masterDataDAO = new  MasterDataSyncDAO();
			
			// UPDATE JOB STATUS TO PROCESSING
			transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_PROCESSING, null);
			
			/*
			 * START PROCESSING EACH ITEM ONE BY ONE
			 */
			if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				AutoSyncScheduleItemDetails itemDetails = null;
				AutoSyncCategoryDetails failCatDetails = null;
				List<MarketLocaleMasterDataTypeMapping> localesList = new ArrayList<MarketLocaleMasterDataTypeMapping>();
				MarketLocaleMasterDataTypeMapping localeMappingDetails = null;
				for(int a=0;a<schDetails.getItemsList().size();a++)
				{
					itemDetails=  (AutoSyncScheduleItemDetails)schDetails.getItemsList().get(a);
					logger.info("startSyncing :: Start Procesisng for Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey());
					// UPDATE ITEM STATUS AS PROCESSING
					transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_PROCESSING);
					/*
					 * CALL Function to Fetch Categories List for the selected Item
					 * For Each Locale & start Processing
					 */
					localesList= masterDataDAO.getMasterDataLocaleMappingList(itemDetails.getLocale(), itemDetails.getItemKey());
					if(null!=localesList && localesList.size()>0)
					{
						/*
						 * fetch category for each locale
						 */
						for(int b=0;b<localesList.size();b++)
						{
							// RE-INITIALIZE CATEGORY LIST
							localeMappingDetails= (MarketLocaleMasterDataTypeMapping)localesList.get(b);
							itemDetails.setCategoryList(new ArrayList<AutoSyncCategoryDetails>());
							
							/*
							 * fetch categoriesList
							 */
							itemDetails.setCategoryList(masterDataDAO.getCategoriesListForProcessing(itemDetails.getItemKey(), schDetails.getMarket(), localeMappingDetails));
							if(null!=itemDetails.getCategoryList() && itemDetails.getCategoryList().size()>0)
							{
								/*
								 * call createCategoryOperation
								 */
								createCategory(itemDetails.getCategoryList(), itemDetails.getItemId(), schDetails.getScheduleId(), schDetails.getMarket(), itemDetails.getItemKey(), localeMappingDetails.getLocale());
							}
							localeMappingDetails =  null;
						}
						
						// check if any entry for the item in Failure List - if Yes then update status as Failure for Item and Job will also Fail
						boolean itemStatusSucces = true;
						if(null!=failureList && failureList.size()>0)
						{
							for(int c=0;c<failureList.size();c++)
							{
								failCatDetails = (AutoSyncCategoryDetails)failureList.get(c);
								if(failCatDetails.getItemId()==itemDetails.getItemId())
								{
									// item is Failure
									itemStatusSucces = false;
									break;
								}
								failCatDetails = null;
							}
							failCatDetails = null;
						}
						
						if(itemStatusSucces==true)
						{
							logger.info("startSyncing :: No Failure For Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey()+". Mark it as Success.");
							transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_SUCCESS);
						}
						else
						{
							logger.info("startSyncing :: Some Failures For Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey()+". Mark it as Failure.");
							transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_FAILURE);
						}
					}
					else
					{
						logger.info("startSyncing :: No Locales Mapping Found for {"+itemDetails.getLocale()+"} Market of Master Data Type :: > " + itemDetails.getItemKey());
						/*
						 * UPDATE ITEM STATUS AS SUCCESS
						 */
						transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_SUCCESS);
					}
					localesList=  null;
					logger.info("startSyncing :: End Procesisng for Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey());
					itemDetails = null;
				}
				itemDetails=  null;
				failCatDetails= null;
			}
			
			if(null!=failureList && failureList.size()>0)
			{
				// some exceptions occurred, set Job as Failure
				errorsFound=true;
			}
			
			/*
			 * UPDATE MDM ITEMS SYNC STATUS
			 */
			updateSyncStatusForItems();
			/*
			 * PRINT REPORTS
			 */
			generateReports(String.valueOf(schDetails.getScheduleId()));
			
			if(errorsFound==false)
			{
				logger.info("startSynching :: No Errors Found, update Schedule Status as SUCCESS. Job completed Successfully.");
				// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO SUCCESS
				transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_SUCCESS);
			}
			else
			{
				logger.info("startSynching :: Errors Found, update Schedule Status as FAILURE. Job completed with some Failures, please refer to Reports.");
				// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO FAILURE
				transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_FAILURE);
			}
		}
		catch(Exception  e)
		{
			logger.info("startSynching :: Some Exception Occured While Executing the Schedule. Failing Job.");
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "startSynching()", e);
			// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO FAILURE
			transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_FAILURE);
		}
		finally
		{
			transactionDAO = null;
			errorsFound=  false;
			categoriesList=  null;
			failureList = null;
			errorsFound = false;
			
			if(null!=client && client.isValid())
			{
				logger.info(" ******************** MasterDataSynchingImpl :: CLOSING IQ SERVICE CLIENT OBJECT. ***********************");
				client.close();
			}
			
			logger.info(" ******************** MasterDataSynchingImpl :: FOR SCHEDULE ID {"+schDetails.getScheduleId()+"}. GENERATED AUTH TOKEN COUNTS ARE :: > "+ authenticationTokenCount);
			client = null;
			authenticationTokenCount = 0;
			printReportUtils = null;
		}
		
		// set Notification Email
		NotificationEmailHelper.generateMasterDataSyncNotificationEmail(String.valueOf(schDetails.getScheduleId()));
		// stop active thread
		stopActiveThead(schDetails.getThreadId());
	}
	
	private void createCategory(ArrayList<AutoSyncCategoryDetails> categoryList, Long itemId, Long scheduleId, String market, String itemType, String processingLocale)
	{
		try
		{
			if(null!=categoryList && categoryList.size()>0)
			{
				AutoSyncCategoryDetails categoryData = null;
				String errorRemarks="";
				for(int i=0;i<categoryList.size();i++)
				{
					categoryData = (AutoSyncCategoryDetails)categoryList.get(i);
					categoryData.setItemId(itemId);
					categoryData.setMarket(market);
					categoryData.setItemType(itemType);
					categoryData.setLocale(processingLocale);
					
					boolean level1 = true;
					boolean level2 = true;
					boolean level3 = true;
					boolean level4 = true;
					boolean level5 = true;
					
					// LEVEL 1
					if(null!=categoryData.getLevel1Details() && null!=categoryData.getLevel1Details().getCategoryRefKey() && !"".equals(categoryData.getLevel1Details().getCategoryRefKey()))
					{
						categoryData.getLevel1Details().setItemId(itemId);
						categoryData.getLevel1Details().setMarket(market);
						categoryData.getLevel1Details().setItemType(itemType);
						categoryData.getLevel1Details().setCategoryRefKey(categoryData.getLevel1Details().getCategoryRefKey().trim().toUpperCase());
						categoryData.getLevel1Details().setParentRefKey(categoryData.getLevel1Details().getParentRefKey().trim().toUpperCase());
						categoryData.getLevel1Details().setMdmItemId(categoryData.getMdmItemId());
						categoryData.getLevel1Details().setCategoryLevel("LEVEL 1");
						// PROCEED FOR CREATING LEVEL 1
						level1 = createCategoryOperation(categoryData.getLevel1Details());
					}
					
					// LEVEL 2
					if(null!=categoryData.getLevel2Details() && null!=categoryData.getLevel2Details().getCategoryRefKey() && !"".equals(categoryData.getLevel2Details().getCategoryRefKey()))
					{
						categoryData.getLevel2Details().setItemId(itemId);
						categoryData.getLevel2Details().setMarket(market);
						categoryData.getLevel2Details().setItemType(itemType);
						categoryData.getLevel2Details().setCategoryRefKey(categoryData.getLevel2Details().getCategoryRefKey().trim().toUpperCase());
						categoryData.getLevel2Details().setParentRefKey(categoryData.getLevel2Details().getParentRefKey().trim().toUpperCase());
						categoryData.getLevel2Details().setMdmItemId(categoryData.getMdmItemId());
						categoryData.getLevel2Details().setCategoryLevel("LEVEL 2");
						// PROCEED FOR CREATING LEVEL 2
						level2 = createCategoryOperation(categoryData.getLevel2Details());
					}
					
					// LEVEL 3
					if(null!=categoryData.getLevel3Details() && null!=categoryData.getLevel3Details().getCategoryRefKey() && !"".equals(categoryData.getLevel3Details().getCategoryRefKey()))
					{
						categoryData.getLevel3Details().setItemId(itemId);
						categoryData.getLevel3Details().setMarket(market);
						categoryData.getLevel3Details().setItemType(itemType);
						categoryData.getLevel3Details().setCategoryRefKey(categoryData.getLevel3Details().getCategoryRefKey().trim().toUpperCase());
						categoryData.getLevel3Details().setParentRefKey(categoryData.getLevel3Details().getParentRefKey().trim().toUpperCase());
						categoryData.getLevel3Details().setMdmItemId(categoryData.getMdmItemId());
						categoryData.getLevel3Details().setCategoryLevel("LEVEL 3");
						// PROCEED FOR CREATING LEVEL 3
						level3 = createCategoryOperation(categoryData.getLevel3Details());
					}
					
					// LEVEL 4
					if(null!=categoryData.getLevel4Details() && null!=categoryData.getLevel4Details().getCategoryRefKey() && !"".equals(categoryData.getLevel4Details().getCategoryRefKey()))
					{
						categoryData.getLevel4Details().setItemId(itemId);
						categoryData.getLevel4Details().setMarket(market);
						categoryData.getLevel4Details().setItemType(itemType);
						categoryData.getLevel4Details().setCategoryRefKey(categoryData.getLevel4Details().getCategoryRefKey().trim().toUpperCase());
						categoryData.getLevel4Details().setParentRefKey(categoryData.getLevel4Details().getParentRefKey().trim().toUpperCase());
						categoryData.getLevel4Details().setMdmItemId(categoryData.getMdmItemId());
						categoryData.getLevel4Details().setCategoryLevel("LEVEL 4");
						// PROCEED FOR CREATING LEVEL 4
						level4 = createCategoryOperation(categoryData.getLevel4Details());
					}
					
					// LEVEL 5
					if(null!=categoryData.getLevel5Details() && null!=categoryData.getLevel5Details().getCategoryRefKey() && !"".equals(categoryData.getLevel5Details().getCategoryRefKey()))
					{
						categoryData.getLevel5Details().setItemId(itemId);
						categoryData.getLevel5Details().setMarket(market);
						categoryData.getLevel5Details().setItemType(itemType);
						categoryData.getLevel5Details().setCategoryRefKey(categoryData.getLevel5Details().getCategoryRefKey().trim().toUpperCase());
						categoryData.getLevel5Details().setParentRefKey(categoryData.getLevel5Details().getParentRefKey().trim().toUpperCase());
						categoryData.getLevel5Details().setMdmItemId(categoryData.getMdmItemId());
						categoryData.getLevel5Details().setCategoryLevel("LEVEL 5");
						// PROCEED FOR CREATING LEVEL 5
						level5 = createCategoryOperation(categoryData.getLevel5Details());
					}
					
					if(level1==true && level2==true && level3==true && level4==true && level5==true)
					{
						// ALL LEVELS SUCCESS
						// set ProcessingStatus as Success
						categoryData.setProcessingStatus(AutoSyncConstants.STATUS_SUCCESS);
						// set operationType as CREATE_CATEGORY
						categoryData.setOperationType("CREATE_CATEGORY");
						// UPDATE SUCCESS COUNT
						transactionDAO.updateProcessingCount(String.valueOf(scheduleId), String.valueOf(itemId), AutoSyncConstants.STATUS_SUCCESS);
					}
					else
					{
						// set ProcessingStatus as Success
						categoryData.setProcessingStatus(AutoSyncConstants.STATUS_FAILURE);
						// set operationType as CREATE_CATEGORY
						categoryData.setOperationType("CREATE_CATEGORY");
						// UPDATE FAILURE COUNT
						transactionDAO.updateProcessingCount(String.valueOf(scheduleId), String.valueOf(itemId), AutoSyncConstants.STATUS_FAILURE);
						
						// IDENTIFY WHAT ALL LEVELS FAILED AND TO CUSTOM ERROR MESSAGE
						errorRemarks=  "Failed to create ";
						if(level1==false)
						{
							errorRemarks+=" LEVEL 1,";
						}
						if(level2==false)
						{
							errorRemarks+=" LEVEL 2,";
						}
						if(level3==false)
						{
							errorRemarks+=" LEVEL 3,";
						}
						if(level4==false)
						{
							errorRemarks+=" LEVEL 4,";
						}
						if(level5==false)
						{
							errorRemarks+=" LEVEL 5";
						}
						
						if(errorRemarks.endsWith(","))
						{
							errorRemarks = errorRemarks.substring(0, errorRemarks.length()-1);
						}
						errorRemarks+=" Categories. Please refer to Failure Report for more details.";
						categoryData.setErrorMessage(errorRemarks);
						errorRemarks=  null;
					}
					
					// ADD TO PROCESSING LIST
					if(null==categoriesList || categoriesList.size()<=0)
					{
						categoriesList = new ArrayList<AutoSyncCategoryDetails>();
					}
					categoriesList.add(categoryData);
					categoryData = null;
					errorRemarks=  null;
				}
				categoryData = null;
				errorRemarks=  null;
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "createCategory()", e);
		} 
	}
	
	private boolean createCategoryOperation(AutoSyncCategoryDetails categoryData)
	{
		boolean bool = false;
		try
		{
			/*
			 * BEFORE CREATING CHECK WHETHER IT ALREADY EXISTS OR NOT.
			 * IF EXISTS THEN UPDATE LABEL FOR ADD ELSE ADD
			 * start performing create Category Operation
			 */
			CategoryITO existCatITO=getCategory(categoryData);
			if(null!=existCatITO && null!=existCatITO.getReferenceKey() && !"".equals(existCatITO.getReferenceKey()))
			{
				logger.info("createCategoryOperation :: Category {"+existCatITO.getReferenceKey()+"} Already Exists. Do not proceed for Creation.");
				/*
				 * UPDATE THE LABEL FOR THE PROCESSING LOCALE.
				 */
				// set Label
				if(null!=categoryData.getCategoryName() && !"".equals(categoryData.getCategoryName()))
				{
					existCatITO.setName(categoryData.getCategoryName());
				}
				existCatITO.setDescription("");
				
				/*
				 * call function to perform update Category Operation
				 */
				CategoryKeyITO updatedCategoryVO = null;
				try
				{
					updatedCategoryVO = client.getCategoryRequest().updateCategory(existCatITO, categoryData.getLocale());
				}
				catch(Exception  e)
				{
					getConnectionWithIM();
					updatedCategoryVO = client.getCategoryRequest().updateCategory(existCatITO, categoryData.getLocale());
				}
			
				if(null!=updatedCategoryVO && null!=updatedCategoryVO.getReferenceKey() && !"".equals(updatedCategoryVO.getReferenceKey()))
				{
					logger.info("createCategoryOperation :: Category Successfully Updated in IM for Locale :: >" + categoryData.getLocale());
					// set bool  to true
					bool = true;
				}
				else
				{
					logger.info("createCategoryOperation :: Failed to Update Category with Ref Key {"+categoryData.getCategoryRefKey()+"} in IM.");
				}
				updatedCategoryVO = null;
			}
			else
			{
				/*
				 * DO NOT PROCEED IF THIS CATEGORY EXISTS IN FAILURE LIST
				 */
				boolean proceedFurther = true;
				if(null!=failureList && failureList.size()>0)
				{
					AutoSyncCategoryDetails errorDetails = null;
					for(int r=0;r<failureList.size();r++)
					{
						errorDetails=  (AutoSyncCategoryDetails)failureList.get(r);
						if(errorDetails.getLocale().equals(categoryData.getLocale()) && errorDetails.getCategoryRefKey().equals(categoryData.getCategoryRefKey()))
						{
							// category exists in failureList do not proceed
							proceedFurther = false;
							break;
						}
						errorDetails = null;
					}
					errorDetails = null;
				}
				
				if(proceedFurther==false)
				{
					logger.info("createCategoryOperation :: Skip processing for {"+categoryData.getCategoryRefKey()+"}. Some Exception Ouccured while performing Get Operation.");
				}
				else
				{
					logger.info("createCategoryOperation :: Category {"+categoryData.getCategoryRefKey()+"} Does not Exists Proceed for Creation.");
					
					
					CategoryITO categoryITO = new CategoryITOImpl();
					categoryITO.setReferenceKey(categoryData.getCategoryRefKey());
					if(null!=categoryData.getCategoryName() && !"".equals(categoryData.getCategoryName()))
					{
						categoryITO.setName(categoryData.getCategoryName());
					}
					categoryITO.setDescription("");
					/*
					 * check for PARENT IS PARENT REF KEY IS NOT NULL
					 */
					if(null!=categoryData.getParentRefKey() && !"".equals(categoryData.getParentRefKey()))
					{
						CategoryKeyITO parentKey = null;
						try
						{
							parentKey = client.getCategoryRequest().getCategoryKeyByReferenceKey(categoryData.getParentRefKey());
						}
						catch(Exception e)
						{
							// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
							getConnectionWithIM();
							parentKey = client.getCategoryRequest().getCategoryKeyByReferenceKey(categoryData.getParentRefKey());
						}
						if(null!=parentKey && null!=parentKey.getReferenceKey() && !"".equals(parentKey.getReferenceKey()))
						{
							categoryITO.setParent(parentKey);
						}
						parentKey = null;
					}

					/*
					 * call function to perform Create Category Operation
					 */
					CategoryKeyITO createdCategoryVO  = null;
					try
					{
						createdCategoryVO =  client.getCategoryRequest().addCategory(categoryITO, categoryData.getLocale());
					}
					catch(Exception e)
					{
						// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
						getConnectionWithIM();
						createdCategoryVO =  client.getCategoryRequest().addCategory(categoryITO, categoryData.getLocale());
					}
					if(null!=createdCategoryVO && null!=createdCategoryVO.getReferenceKey() && !"".equals(createdCategoryVO.getReferenceKey()))
					{
						logger.info("createCategoryOperation :: Category {"+createdCategoryVO.getReferenceKey()+"} Successfully Created in IM for Locale :: >"+ categoryData.getLocale());
						// set bool  to true
						bool = true;
					}
					else
					{
						logger.info("createCategoryOperation :: Failed to Create Category with Ref Key {"+categoryData.getCategoryRefKey()+"} in IM for Locale  :: >"+ categoryData.getLocale());
					}
					createdCategoryVO = null;
					categoryITO = null;
				}
				
			}
			existCatITO = null;
		}
		catch (IQServiceClientException e) 
		{
			if (null!=client) 
			{
				if (null!=client.getEWR() && client.getEWR().hasErrorsOrWarnings()) 
				{
					// EWR reported a problem, check to see if it is an error
					if (client.getEWR().hasErrors()) 
					{
						// output the errors
						List<ErrorRecord> errors = client.getEWR().getErrors();
						for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
							ErrorRecord rec = iter.next();
							logger.info("createCategoryOperation :: checkEWR :: Error Code :: >" + rec.getCode());
							logger.info("createCategoryOperation :: checkEWR :: Error Message :: >" + rec.getMessage());

							categoryData.setErrorCode(String.valueOf(rec.getCode()));
							categoryData.setErrorMessage(rec.getMessage());
							categoryData.setOperationType("CREATE_CATEGORY");
							// add errorCode and errorMessage - add categoryTo Failure List
							if(null==failureList || failureList.size()<=0)
							{
								failureList = new ArrayList<AutoSyncCategoryDetails>();
							}
							failureList.add(categoryData);
						}
					}
				}
			
			}
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "createCategoryOperation()", e);
		} 
		catch (Exception e) 
		{
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			
			logger.info("createCategoryOperation :: otherErrors :: Error Code :: >" + e.getMessage());
			
			categoryData.setErrorCode(e.getMessage());
			categoryData.setErrorMessage(writer.toString());
			categoryData.setOperationType("CREATE_CATEGORY");
			// add errorCode and errorMessage - add categoryTo Failure List
			if(null==failureList || failureList.size()<=0)
			{
				failureList = new ArrayList<AutoSyncCategoryDetails>();
			}
			failureList.add(categoryData);
			
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "createCategoryOperation()", e);
		} 
		return bool;
	}
	
	private CategoryITO getCategory(AutoSyncCategoryDetails categoryDetails) 
	{
		String operationType="GET_CATEGORY";
		CategoryITO catIto = new CategoryITOImpl();
		try 
		{
			catIto = client.getCategoryRequest().getCategoryByReferenceKey(categoryDetails.getCategoryRefKey());
		} 
		catch (Exception e) 
		{
			try
			{
				// TOKEN MIGHT HAVE EXPIRED OR INVALIDATED. RE-EXECUTE THE STEP
				getConnectionWithIM();
				catIto = client.getCategoryRequest().getCategoryByReferenceKey(categoryDetails.getCategoryRefKey());
			}
			catch(IQServiceClientException e1)
			{
				boolean printStack=true;
				if(null!=client)
				{
					if (null!=client.getEWR() && client.getEWR().hasErrorsOrWarnings()) 
					{
						// EWR reported a problem, check to see if it is an error
						if (client.getEWR().hasErrors()) 
						{
							// output the errors
							List<ErrorRecord> errors = client.getEWR().getErrors();
							for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
								ErrorRecord rec = iter.next();
								boolean addError = true;
								if(rec.getCode()==110)
								{
									if(null!=rec.getMessage() && !"".equals(rec.getMessage()))
									{
										if(rec.getMessage().trim().toLowerCase().equals("category.not.found"))
										{
											printStack=false;
											addError = false;
										}
									}
								}
								
								if(addError == true)
								{
									categoryDetails.setErrorCode(String.valueOf(rec.getCode()));
									categoryDetails.setErrorMessage(rec.getMessage());
									categoryDetails.setOperationType(operationType);
									// add errorCode and errorMessage - add categoryTo Failure List
									if(null==failureList || failureList.size()<=0)
									{
										failureList = new ArrayList<AutoSyncCategoryDetails>();
									}
									failureList.add(categoryDetails);
								}
								
								logger.info("getCategory :: checkEWR :: Error Code :: >" + rec.getCode());
								logger.info("getCategory :: checkEWR :: Error Message :: >" + rec.getMessage());
								rec = null;
							}
						}
					}
				}
				
				if(printStack==true)
				{
					Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "getCategory()", e1);
				}
			}
			catch (Exception e1) 
			{
				Writer writer = new StringWriter();
				PrintWriter print = new PrintWriter(writer);
				e1.printStackTrace(print);
				
				logger.info("getCategory :: otherErrors :: Error Code :: >" + e1.getMessage());
				
				categoryDetails.setErrorCode(e1.getMessage());
				categoryDetails.setErrorMessage(writer.toString());
				categoryDetails.setOperationType(operationType);
				// add errorCode and errorMessage - add categoryTo Failure List
				if(null==failureList || failureList.size()<=0)
				{
					failureList = new ArrayList<AutoSyncCategoryDetails>();
				}
				failureList.add(categoryDetails);
				
				// set writer & print to null
				writer = null;
				// set print to null
				print = null;
				Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "getCategory()", e1);
			}
		} 
		return catIto;
	}

	private void getConnectionWithIM() {
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
//				logger.info("getConnectionWithIM :: REPOSITORY INFO RECEIVED FETCHED REPOSITORY IS ::: > " + rk.getRecordID());
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
			authenticationTokenCount++;
			logger.info("getConnectionWithIM :: GENERATING NEW TOKEN FOR SINCE, EITHER CLIENT IS NULL OR AUTHENTICATION TOKEN IS EXPIRED.");
			if(null!=client)
			{
				client.close();
			}
			client = null;
			
			try {
				client= IQServiceClientManager
						.connect(
								ApplicationProperties
								.getProperty("USERNAME").trim().toLowerCase(),
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
				Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "getConnectionWithIM()", e);
			}
		}
		
		logger.info("getConnectionWithIM() :: CLIENT :: >" + client);
		if(null!=client)
		{
			logger.info("getConnectionWithIM() :: AUTHENTICATION TOKEN :: >" + client.getAuthenticationToken());
			logger.info("getConnectionWithIM() :: IS VALID :: >" + client.isValid());
		}
	}
	
	@SuppressWarnings("deprecation")
	private void stopActiveThead(String threadId)
	{
		try
		{
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
								Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "stopActiveThead()", e);
		}
	}

	private void updateSyncStatusForItems()
	{
		try
		{
			if(null!=categoriesList && categoriesList.size()>0)
			{
				AutoSyncCategoryDetails catDetails = null;
				List<String> modelYearCodes=new ArrayList<String>();
				List<String> vinRangeCodes=new ArrayList<String>();
				List<String> carlineCodes=new ArrayList<String>();
				List<String> vinCodes=new ArrayList<String>();
				List<String> axleCodes=new ArrayList<String>();
				List<String> bodyCodes=new ArrayList<String>();
				List<String> esiCodes=new ArrayList<String>();
				List<String> cvcCodes=new ArrayList<String>();
				List<String> manualTypeCodes=new ArrayList<String>();
				List<String> engineTypeCodes=new ArrayList<String>();
				List<String> transmissionTypeCodes=new ArrayList<String>();
				AutoSyncCategoryDetails failureDetails=  null;
				
				for(int a=0;a<categoriesList.size();a++)
				{
					catDetails= (AutoSyncCategoryDetails)categoriesList.get(a);
					/*
					 * CHECK IF PROCESSING STATUS OF ROW IS SUCCESS
					 * ADD TO LIST FOR UPDATING SYNC STATUS TO Y
					 */
					if(null!=catDetails.getItemType() && !"".equals(catDetails.getItemType()) && 
							null!=catDetails.getProcessingStatus() && catDetails.getProcessingStatus().equals(AutoSyncConstants.STATUS_SUCCESS))
					{
						if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								axleCodes = addMDMItems(catDetails, axleCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								bodyCodes = addMDMItems(catDetails, bodyCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								modelYearCodes = addMDMItems(catDetails, modelYearCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								vinRangeCodes = addMDMItems(catDetails, vinRangeCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
						{
							// LEVEL 1 CHECK
							if(null!=catDetails.getLevel1Details() && null!=catDetails.getLevel1Details().getParentRefKey() && 
									catDetails.getLevel1Details().getParentRefKey().equals(ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE")))
							{
								// CARLINE CODES - add TO LIST

								/*
								 * BEFORE ADDING TO LIST
								 * CHECK IF THE MDM ITEM ID EXISTS IN FAILURE LIST FOR ANY LEVEL / LOCALE
								 * DO NOT ADD
								 */
								boolean add=true;
								if(null!=failureList && failureList.size()>0)
								{
									for(int e=0;e<failureList.size();e++)
									{
										failureDetails=  (AutoSyncCategoryDetails)failureList.get(e);
										if(null!=failureDetails.getItemType())
										{
											if(failureDetails.getItemType().equals(catDetails.getItemType()))
											{
												// ITEM TYPE MATCHED - CHECK FOR LEVEL 1 ONLY
												if(null!=failureDetails.getParentRefKey() && failureDetails.getParentRefKey().equals(ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE")))
												{
													if(null!=catDetails.getMdmItemId() && null!=failureDetails.getMdmItemId())
													{
														if(catDetails.getMdmItemId().equals(failureDetails.getMdmItemId()))
														{
															// EXISTS IN FAILURE LIST
															add =false;
															break;
														}
													}
												}
											}
										}
										failureDetails = null;
									}
									failureDetails = null;
								}
								
								if(add==true)
								{
									boolean addToList = true;
									// check if already added or not
									if(null!=carlineCodes && carlineCodes.size()>0)
									{
										for(int r=0;r<carlineCodes.size();r++)
										{
											if(String.valueOf(carlineCodes.get(r)).equals(catDetails.getMdmItemId()))
											{
												// MDM ID ALREADY ADDED - DO NOT ADD
												addToList = false;
												break;
											}
										}
									}
									
									if(addToList== true)
									{
										// add TO LIST
										if(null==carlineCodes || carlineCodes.size()<=0)
										{
											carlineCodes = new ArrayList<String>();
										}
										carlineCodes.add(catDetails.getMdmItemId());
									}
								}
							}
							else
							{
								// OTHER LEVELS - VIN CODES - add TO LIST
								/*
								 * BEFORE ADDING TO LIST
								 * CHECK IF THE MDM ITEM ID EXISTS IN FAILURE LIST FOR ANY LEVEL / LOCALE
								 * DO NOT ADD
								 */
								boolean add=true;
								if(null!=failureList && failureList.size()>0)
								{
									for(int e=0;e<failureList.size();e++)
									{
										failureDetails=  (AutoSyncCategoryDetails)failureList.get(e);
										if(null!=failureDetails.getItemType())
										{
											if(failureDetails.getItemType().equals(catDetails.getItemType()))
											{
												// ITEM TYPE MATCHED - NO CHECK FOR LEVEL ONLY
												if(null!=failureDetails.getParentRefKey() && !failureDetails.getParentRefKey().equals(ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE")))
												{
													if(null!=catDetails.getMdmItemId() && null!=failureDetails.getMdmItemId())
													{
														if(catDetails.getMdmItemId().equals(failureDetails.getMdmItemId()))
														{
															// EXISTS IN FAILURE LIST
															add =false;
															break;
														}
													}
												}
											}
										}
										failureDetails = null;
									}
									failureDetails = null;
								}
								
								if(add==true)
								{
									boolean addToList = true;
									// check if already added or not
									if(null!=vinCodes && vinCodes.size()>0)
									{
										for(int r=0;r<vinCodes.size();r++)
										{
											if(String.valueOf(vinCodes.get(r)).equals(catDetails.getMdmItemId()))
											{
												// MDM ID ALREADY ADDED - DO NOT ADD
												addToList = false;
												break;
											}
										}
									}
									
									if(addToList== true)
									{
										// add TO LIST
										if(null==vinCodes || vinCodes.size()<=0)
										{
											vinCodes = new ArrayList<String>();
										}
										vinCodes.add(catDetails.getMdmItemId());
									}
								}
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								engineTypeCodes = addMDMItems(catDetails, engineTypeCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								transmissionTypeCodes = addMDMItems(catDetails, transmissionTypeCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								manualTypeCodes = addMDMItems(catDetails, manualTypeCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								esiCodes = addMDMItems(catDetails, esiCodes);
							}
						}
						else if(catDetails.getItemType().equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
						{
							if(null!=catDetails.getMdmItemId() && !"".equals(catDetails.getMdmItemId()))
							{
								cvcCodes = addMDMItems(catDetails, cvcCodes);
							}
						}
					}
					catDetails = null;
				}
				catDetails = null;
				
				
				logger.info("updateSyncStatusForItems :: ------------------- Axle Codes :: > "+ axleCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- Body Codes :: > "+ bodyCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- Model Year Codes :: > "+ modelYearCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- VIN Range Codes :: > "+ vinRangeCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- Carline Codes :: > "+ carlineCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- VIN MC / MME Codes :: > "+ vinCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- ESI Codes :: > "+ esiCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- CVC Codes :: > "+ cvcCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- Engine Type Codes :: > "+ engineTypeCodes.size());
				logger.info("updateSyncStatusForItems :: ------------------- Transmission Type Codes :: > "+ transmissionTypeCodes.size());
				/*
				 * CREATE A MAP OBJECT AND SEND TO DAO FOR UPDATING ALL MDM ITEMS SYNC STATUS
				 */
				Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
				dataMap.put(AutoSyncConstants.ITEM_KEY_AXLE_TYPE, axleCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_BODY_TYPE, bodyCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_MODEL_YEAR, modelYearCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_VIN_RANGE, vinRangeCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_CARLINE, carlineCodes);
				dataMap.put("VIN_FOR_MC_MME", vinCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY, esiCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY, cvcCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE, engineTypeCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE, transmissionTypeCodes);
				dataMap.put(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE, manualTypeCodes);
				
				/*
				 * call database function to update the status for each Item
				 */
				transactionDAO.updateMDMItemSyncStatus(dataMap);
				
				
				dataMap=  null;
				axleCodes= null;
				bodyCodes=null;
				modelYearCodes=  null;
				vinRangeCodes= null;
				carlineCodes=  null;
				vinCodes=  null;
				esiCodes=  null;
				cvcCodes=null;
				engineTypeCodes=  null;
				transmissionTypeCodes = null;
				manualTypeCodes= null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "updateSyncStatusForItems()", e);
		}
	}
	
	
	private void generateReports(String scheduleId)
	{
		try
		{
			if(null!=categoriesList && categoriesList.size()>0)
			{
				int partitionSize=10000;
				ArrayList<List<AutoSyncCategoryDetails>> partitions = new ArrayList<List<AutoSyncCategoryDetails>>();
				for (int i=0; i<categoriesList.size(); i += partitionSize) {
			        partitions.add(categoriesList.subList(i, Math.min(i + partitionSize, categoriesList.size())));
			    }
				
				int listCount=0;
				if(null!=partitions && partitions.size()>0)
				{
					for(List<AutoSyncCategoryDetails> subList : partitions)
					{
						listCount++;
						if(null!=subList && subList.size()>0)
						{
							printReportUtils.printTransactionReport(subList, scheduleId, listCount);
						}
						subList = null;
					}
				}
				partitions = null;
			}
			
			if(null!=failureList && failureList.size()>0)
			{
				int partitionSize=10000;
				ArrayList<List<AutoSyncCategoryDetails>> partitions = new ArrayList<List<AutoSyncCategoryDetails>>();
				for (int i=0; i<failureList.size(); i += partitionSize) {
			        partitions.add(failureList.subList(i, Math.min(i + partitionSize, failureList.size())));
			    }
				
				int listCount=0;
				if(null!=partitions && partitions.size()>0)
				{
					for(List<AutoSyncCategoryDetails> subList : partitions)
					{
						listCount++;
						if(null!=subList && subList.size()>0)
						{
							printReportUtils.printFailureReport(subList, scheduleId, listCount);
						}
						subList = null;
					}
				}
				partitions = null;
			}
			
			
			/*
			 * now here, generate the Zip file for all the reports generated the
			 * path for reports directory will be -
			 * REPORTS_DIRECTORY/schCode
			 */
			String path = ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");
			if(!path.endsWith("/"))
			{
				path = path+"/";
			}
			path=path+scheduleId+"/";

			/*
			 * call function to generate zip file
			 */
			boolean bool = printReportUtils.createReportsZip(path, scheduleId);
			if (bool == true) 
			{
				logger.info("generateReports :: Reports Zipped Successfully.");
			} 
			else 
			{
				logger.info("generateReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
			path = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "generateReports()", e);
		}
	}
	
	private List<String> addMDMItems(AutoSyncCategoryDetails catDetails, List<String> processingList)
	{
		try
		{
			/*
			 * BEFORE ADDING TO LIST
			 * CHECK IF THE MDM ITEM ID EXISTS IN FAILURE LIST FOR ANY LEVEL / LOCALE
			 * DO NOT ADD
			 */
			boolean add=true;
			if(null!=failureList && failureList.size()>0)
			{
				AutoSyncCategoryDetails failureDetails=  null;
				for(int e=0;e<failureList.size();e++)
				{
					failureDetails=  (AutoSyncCategoryDetails)failureList.get(e);
					if(null!=failureDetails.getItemType())
					{
						if(failureDetails.getItemType().equals(catDetails.getItemType()))
						{
							// ITEM TYPE MATCHED
							if(null!=catDetails.getMdmItemId() && null!=failureDetails.getMdmItemId())
							{
								if(catDetails.getMdmItemId().equals(failureDetails.getMdmItemId()))
								{
									// EXISTS IN FAILURE LIST
									add =false;
									break;
								}
							}
						}
					}
					failureDetails = null;
				}
				failureDetails = null;
			}
			
			if(add==true)
			{
				boolean addToList = true;
				// check if already added or not
				if(null!=processingList && processingList.size()>0)
				{
					for(int r=0;r<processingList.size();r++)
					{
						if(String.valueOf(processingList.get(r)).equals(catDetails.getMdmItemId()))
						{
							// MDM ID ALREADY ADDED - DO NOT ADD
							addToList = false;
							break;
						}
					}
				}
				
				if(addToList== true)
				{
					// add TO LIST
					if(null==processingList || processingList.size()<=0)
					{
						processingList = new ArrayList<String>();
					}
					processingList.add(catDetails.getMdmItemId());
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "addMDMItems()", e);
		}
		return processingList;
	}
}
