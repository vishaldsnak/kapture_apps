package com.mazda.gms3.dmt.metadataval.impl;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.metadataval.dao.MetaDataValidationDAO;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataScheduleConstants;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataValidationUtils;
import com.mazda.gms3.dmt.metadataval.utils.PrintReportsUtil;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class MetaDataValidationScheduleImpl {

	private Logger logger = LogManager.getLogger(MetaDataValidationScheduleImpl.class);
	
	private String serverPath = null;
	
	private MetaDataValidationDAO metaDataDAO=null;
	
	private PrintReportsUtil printReportsUtil=null;
	
	private String mnaoLocales=null;
	private String mcLocales=null;
	private String mmeLocales=null;
	
	public void startValidation(String scheduleId, String fileName, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		String scheduleRemarks=null;
		try
		{
			// initialize all variables
			metaDataDAO = new MetaDataValidationDAO();
			printReportsUtil = new PrintReportsUtil();
			getMarketSpecificLocales();
			
			// serverPath
			serverPath = ApplicationProperties.getProperty("metadata.validation.upload.physicalpath");
			// check for params
			if(null!=scheduleId && !"".equals(scheduleId) && null!=fileName && !"".equals(fileName) 
					&& null!=metaDataType && !"".equals(metaDataType) && null!=locale && !"".equals(locale) 
					&& null!=serverPath && !"".equals(serverPath))
			{
				/*
				 * update schedule status to processing
				 */
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_PROCESSING,"");
				
				serverPath = serverPath.replace("\\", "/");
				if(!serverPath.endsWith("/"))
				{
					serverPath+= "/";
				}
				
				String processingFilePath = serverPath+fileName;
				File processingFile = null;
				try
				{
					processingFile = PathUtil.file(processingFilePath);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "startValidation()", e);
				}
				
				if(null!=processingFile && processingFile.isFile() && processingFile.exists())
				{
					/*
					 * PROCEED FOR VALIDATING FILE
					 */
					String localeToCheck = locale;
					localeToCheck = localeToCheck.replace("-", "_");
					if(mnaoLocales.indexOf(localeToCheck)>-1)
					{
						// MNAO OPERATIONS
						mnaoOperations(scheduleId, processingFile, metaDataType, locale);
					}
					else if(mcLocales.indexOf(localeToCheck)>-1)
					{
						// MC OPERATIONS
						mcOperations(scheduleId, processingFile, metaDataType, locale);
					}
					else if(mmeLocales.indexOf(localeToCheck)>-1)
					{
						// MME OPERATIONS
						mmeOperations(scheduleId, processingFile, metaDataType, locale);
					}
					else
					{
						// UPDATE FAILURE STATUS - LOCALE IS NOT VALID.
						logger.info("startValidation :: Locale {"+locale+"} mapped with MetaData File could not be located in any of the Markets. Exit Conversion");
						/*
						 * Update Status to Failure
						 */
						updateFailureStatus=true;
						scheduleRemarks="Locale {"+locale+"} mapped with MetaData File could not be located in any of the Markets.";
					}
					localeToCheck = null;
				}
				else
				{
					logger.info("startValidation :: Processing File does not exist at path :: > "+ processingFilePath+". Exit Conversion.");
					/*
					 * Update Status to Failure
					 */
					updateFailureStatus=true;	
					scheduleRemarks="Processing File does not exist at path :: > "+ processingFilePath+".";
				}
				processingFile=null;
				processingFilePath = null;
			}
			else
			{
				logger.info("####################################################################");
				logger.info("startValidation :: Required Parameters are null. Exit Conversion.   ");
				logger.info("####################################################################");
				/*
				 * Update Status to Failure
				 */
				updateFailureStatus=true;
				scheduleRemarks="Required Parameters are null. Failed to initiate validation.";
			}
			
			// if Failure status Flag is true
			if(updateFailureStatus == true)
			{
				/*
				 * CALL DB Function to Update Status to Failure
				 */
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, scheduleRemarks);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "startValidation()", e);
			scheduleRemarks = "Exception while initiating Validation Schedule ->"+ e.getMessage();
			/*
			 * Update Status to Failure
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, scheduleRemarks);
			} 
			catch (SQLException e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "startValidation()", e);
			}
		}
		finally
		{
			serverPath = null;
			updateFailureStatus = false;
			scheduleRemarks=null;
			metaDataDAO=null;
			printReportsUtil=null;
			mnaoLocales=  null;
			mcLocales=  null;
			mmeLocales=  null;
		}
		
		// call function to stop thread
		stopActiveThead(scheduleId);
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
			String threadId=ApplicationProperties.getProperty("metadata.sch.key")+scheduleCode;
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
								Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "stopActiveThead()", e);
		}
	}
	
	private void mnaoOperations(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		try
		{
			if(metaDataType.equals("VIN"))
			{
				mnaoVINOperation(scheduleId, processingFile, metaDataType, locale);
			}
			else if(metaDataType.equals("VINATTRIBUTE"))
			{
				vinAttributeOperation(scheduleId, processingFile, metaDataType, locale);
			}
			else if(metaDataType.equals("LEFTMENU"))
			{
				leftMenuOperation(scheduleId, processingFile, locale);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoOperations()", e);
		}
	}

	private void mcOperations(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		try
		{
			if(metaDataType.equals("VIN"))
			{
				mmemcVINOperation(scheduleId, processingFile, metaDataType, locale);
			}
			else if(metaDataType.startsWith("ESICATEGORY"))
			{
				esiCategoryOperation(scheduleId, processingFile, metaDataType, locale);
			}
			else if(metaDataType.equals("DISPLAYORDER"))
			{
				displayOrderOperation(scheduleId, processingFile, metaDataType, locale);
			}
			else if(metaDataType.equals("CDPROCESSING"))
			{
				cdProcessingOperation(scheduleId, processingFile, metaDataType, locale);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mcOperations()", e);
		}
	}
	
	private void mmeOperations(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		if(metaDataType.equals("VIN"))
		{
			mmemcVINOperation(scheduleId, processingFile, metaDataType, locale);
		}
		else if(metaDataType.startsWith("ESICATEGORY"))
		{
			esiCategoryOperation(scheduleId, processingFile, metaDataType, locale);
		}
		else if(metaDataType.equals("DISPLAYORDER"))
		{
			displayOrderOperation(scheduleId, processingFile, metaDataType, locale);
		}
		else if(metaDataType.equals("CDPROCESSING"))
		{
			cdProcessingOperation(scheduleId, processingFile, metaDataType, locale);
		}
	}
	
	private void generateZIPForReports(String scheduleId)
	{
		try
		{
			/*
			 * now here, generate the Zip file for all the reports generated the
			 * path for reports directory will be -
			 * REPORTS_DIRECTORY/schCode
			 */
			String path = ApplicationProperties.getProperty("metadata.validation.reports.physicalpath");
			if(!path.endsWith("/"))
			{
				path = path+"/";
			}
			path = path+scheduleId+"/";

			/*
			 * call function to generate zip file
			 */
			boolean bool = printReportsUtil.createReportsZip(path, scheduleId);
			if (bool == true) 
			{
				logger.info("generateZIPForReports :: Reports Zipped Successfully.");
			} 
			else 
			{
				logger.info("generateZIPForReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
			path = null;
			
			// update reports path
			String reportsPath = ApplicationProperties.getProperty("metadata.validation.reports.relativepath");
			if(!reportsPath.endsWith("/"))
			{
				reportsPath=reportsPath+"/";
			}
			reportsPath=reportsPath+scheduleId+"/"+scheduleId+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
//			reportsPath=reportsPath+scheduleId+"/"+scheduleId+"_REPORT"+ApplicationProperties.getProperty("extension.txt");
			metaDataDAO.updateScheduleReportsPath(scheduleId, reportsPath);
			reportsPath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "generateZIPForReports()", e);
		}
	}

	private void mmemcVINOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<VinDetails> vinList=new ArrayList<VinDetails>();
		ArrayList<VinDetails> failureList=new ArrayList<VinDetails>();
		boolean proceedFurther=true;
		VinDetails details = null;
		try
		{
			locale = locale.replace("-", "_");
			if(ApplicationProperties.getProperty("mc.locales").indexOf(locale)>-1)
			{
				// read VIN File
				vinList = MetaDataValidationUtils.readVINTextFile(processingFile);
				if(null!=vinList && vinList.size()>0)
				{
					// iterate and set WMI CODE AS - for all
					for(int a=0;a<vinList.size();a++)
					{
						details = (VinDetails)vinList.get(a);
						details.setWmiCode("-");
					}
					details = null;
				}
			}
			else
			{
				// read VIN File
				vinList = MetaDataValidationUtils.readVINTextFileForMME(processingFile);
			}
		}
		catch(Exception e)
		{
			vinList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mmemcVINOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for VIN from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}
		
		// revert locale values
		locale = locale.replace("_", "-");
		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=vinList && vinList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				int totalCount = vinList.size();
				int failureCount=0;
				details = null;
				for(int a=0;a<vinList.size();a++)
				{
					details = (VinDetails)vinList.get(a);
					if(null==details.getLineType() || (null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_INVALID)))
					{
						// increment failureCount by 1
						failureCount++;
					}
					details = null;
				}
				details = null;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mmemcVINOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					// PROCEED FOR CHECKING VIN EXISTS IN MDM + IM.
					try
					{
						vinList = metaDataDAO.checkMCMMEVINStatus(vinList, locale, scheduleId);
						if(null!=vinList && vinList.size()>0)
						{
							failureList = new ArrayList<VinDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<vinList.size();a++)
							{
								details = (VinDetails)vinList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<vinList.size();a++)
							{
								details = (VinDetails)vinList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;
							
							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}

							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printMCMMEVinReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
						else
						{
							// update scheduleStatus to failure
							updateFailureStatus=true;
							schdeuleRemarks="Failed to identify VINs in MDM & IM. List is returned as null.";
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mmemcVINOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify VINs in MDM & IM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("mmemcVINOperation :: No Data read for VIN from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for VIN from MetaData Processing File.";	
			}
		}
		vinList=null;
		failureList = null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mmemcVINOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mmemcVINOperation()", e1);
			}
		}
		
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void displayOrderOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<DisplayOrderDetails> displayOrderList=new ArrayList<DisplayOrderDetails>();
		ArrayList<DisplayOrderDetails> failureList=new ArrayList<DisplayOrderDetails>();
		boolean proceedFurther=true;
		try
		{
			// read DISPLAY ORDER File
			displayOrderList = MetaDataValidationUtils.readDisplayOrderTextFile(processingFile, locale);
		}
		catch(Exception e)
		{
			displayOrderList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "displayOrderOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for Display Order from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}

		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=displayOrderList && displayOrderList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				DisplayOrderDetails details = null;
				int totalCount = displayOrderList.size();
				int failureCount=0;
				for(int a=0;a<displayOrderList.size();a++)
				{
					details = (DisplayOrderDetails)displayOrderList.get(a);
					/*
					 * CHECK IF ANY DISPLAY ORDER CODE IS NOT NULL 
					 * AND IF ITS NAME IS NULL - SET IT FAILURE
					 */
					if(null==details.getLineType() || (null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_INVALID)))
					{
						// increment failureCount by 1
						failureCount++;
					}
					details = null;
				}
				details = null;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "displayOrderOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					/*
					 *  PROCEED FOR CHECKING CATEGORIES FROM DISPLAY ORDER IF NOT ENGINE AT/MT
					 *  CHECK NAMES FOR EACH DISPLAY ORDER CODE AVAILABLE
					 */
					try
					{
						displayOrderList = metaDataDAO.checkDisplayOrderStatus(displayOrderList, locale, scheduleId);
						if(null!=displayOrderList && displayOrderList.size()>0)
						{
							failureList = new ArrayList<DisplayOrderDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<displayOrderList.size();a++)
							{
								details = (DisplayOrderDetails)displayOrderList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<displayOrderList.size();a++)
							{
								details = (DisplayOrderDetails)displayOrderList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;

							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}


							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printDisplayOrderReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
						else
						{
							// update scheduleStatus to failure
							updateFailureStatus=true;
							schdeuleRemarks="Failed to identify Display Orders in MDM & IM. List is returned as null";
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "displayOrderOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify Display Orders in MDM & IM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("displayOrderOperation :: No Data read for Display Order from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for Display Order from MetaData Processing File.";	
			}
		}
		displayOrderList=null;
		failureList=null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "displayOrderOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "displayOrderOperation()", e1);
			}
		}
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void cdProcessingOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<CDProcessingDetails> cdProcessingList=new ArrayList<CDProcessingDetails>();
		ArrayList<CDProcessingDetails> failureList = new ArrayList<CDProcessingDetails>();
		boolean proceedFurther=true;
		try
		{
			// read CD PROCESSING File
			cdProcessingList = MetaDataValidationUtils.readCDProcessingTextFile(processingFile, locale);
		}
		catch(Exception e)
		{
			cdProcessingList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "cdProcessingOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for CD Processing from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}

		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=cdProcessingList && cdProcessingList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				CDProcessingDetails details = null;
				int totalCount = cdProcessingList.size();
				int failureCount=0;
				for(int a=0;a<cdProcessingList.size();a++)
				{
					details = (CDProcessingDetails)cdProcessingList.get(a);
					/*
					 * CHECK IF ANY CD PROCESSING CODE IS NOT NULL 
					 * AND IF ITS NAME IS NULL - SET IT FAILURE
					 */
					if(null==details.getLineType() || (null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_INVALID)))
					{
						// increment failureCount by 1
						failureCount++;
					}
					details = null;
				}
				details = null;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "cdProcessingOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					/*
					 *  PROCEED FOR CHECKING NAMES FOR EACH DISPLAY ORDER CODE AVAILABLE
					 */
					try
					{
						cdProcessingList = metaDataDAO.checkCDProcessingDisplayOrderStatus(cdProcessingList, locale, scheduleId);
						if(null!=cdProcessingList && cdProcessingList.size()>0)
						{
							failureList = new ArrayList<CDProcessingDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<cdProcessingList.size();a++)
							{
								details = (CDProcessingDetails)cdProcessingList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<cdProcessingList.size();a++)
							{
								details = (CDProcessingDetails)cdProcessingList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;

							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}


							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printCDProcessingReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
						else
						{
							// update failureStatus
							updateFailureStatus=true;
							schdeuleRemarks="Failed to identify Display Orders for CD Processing Data in MDM. List is returned as null.";
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "cdProcessingOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify Display Orders for CD Processing Data in MDM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("cdProcessingOperation :: No Data read for CD Processing from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for CD Processing from MetaData Processing File.";	
			}
		}
		cdProcessingList=null;
		failureList= null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "cdProcessingOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "cdProcessingOperation()", e1);
			}
		}
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void esiCategoryOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<ESICategoryDetails> esiCategoryList=new ArrayList<ESICategoryDetails>();
		ArrayList<ESICategoryDetails> failureList = new ArrayList<ESICategoryDetails>();
		boolean proceedFurther=true;
		try
		{
			// read ESI CATEGORY TEXT File
			if(metaDataType.equals("ESICATEGORY_WD_NEW") || metaDataType.equals("ESICATEGORY_WD_OLD"))
			{
				esiCategoryList = MetaDataValidationUtils.readESICategoryTextFileForMME_WD(processingFile);
			}
			else 
			{
				esiCategoryList = MetaDataValidationUtils.readESICategoryTextFile(processingFile);
			}
		}
		catch(Exception e)
		{
			esiCategoryList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "esiCategoryOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for ESI Category from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}

		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=esiCategoryList && esiCategoryList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				ESICategoryDetails details = null;
				int totalCount = esiCategoryList.size();
				int failureCount=0;
				for(int a=0;a<esiCategoryList.size();a++)
				{
					details = (ESICategoryDetails)esiCategoryList.get(a);
					/*
					 * CHECK IF ANY CD PROCESSING CODE IS NOT NULL 
					 * AND IF ITS NAME IS NULL - SET IT FAILURE
					 */
					if(null==details.getLineType() || (null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_INVALID)))
					{
						// increment failureCount by 1
						failureCount++;
					}
					details = null;
				}
				details = null;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "esiCategoryOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					/*
					 *  PROCEED FOR CHECKING NAMES FOR EACH ESI CATEGORY CODE AVAILABLE
					 */
					try
					{
						esiCategoryList = metaDataDAO.checkESICategoryStatus(esiCategoryList, locale, scheduleId, metaDataType);
						if(null!=esiCategoryList && esiCategoryList.size()>0)
						{
							failureList = new ArrayList<ESICategoryDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<esiCategoryList.size();a++)
							{
								details = (ESICategoryDetails)esiCategoryList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<esiCategoryList.size();a++)
							{
								details = (ESICategoryDetails)esiCategoryList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;

							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}

							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printESICategoryReport(scheduleId, failureList, locale, metaDataType);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "esiCategoryOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify ESI Category Data in MDM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("esiCategoryOperation :: No Data read for ESI Category from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for ESI Category from MetaData Processing File.";	
			}
		}
		esiCategoryList=null;
		failureList=null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "esiCategoryOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "esiCategoryOperation()", e1);
			}
		}
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void leftMenuOperation(String scheduleId, File processingFile, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<LeftMenuFileDetails> leftMenuList=new ArrayList<LeftMenuFileDetails>();
		ArrayList<LeftMenuFileDetails> failureList = new ArrayList<LeftMenuFileDetails>();
		boolean proceedFurther=true;
		try
		{
			// read LEFT MENU TEXT File
			leftMenuList = MetaDataValidationUtils.readLeftMenuTextFile(processingFile);
		}
		catch(Exception e)
		{
			leftMenuList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "leftMenuOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for LeftMenu Data from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}

		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=leftMenuList && leftMenuList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				LeftMenuFileDetails details = null;
				int totalCount = leftMenuList.size();
				int failureCount=0;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "leftMenuOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					/*
					 *  PROCEED FOR CHECKING NAMES FOR EACH ESI CATEGORY CODE AVAILABLE
					 */
					try
					{
						leftMenuList = metaDataDAO.checkLeftMenuStatus(leftMenuList, locale, scheduleId);
						if(null!=leftMenuList && leftMenuList.size()>0)
						{
							failureList = new ArrayList<LeftMenuFileDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<leftMenuList.size();a++)
							{
								details = (LeftMenuFileDetails)leftMenuList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<leftMenuList.size();a++)
							{
								details = (LeftMenuFileDetails)leftMenuList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;

							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}

							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printLeftMenuReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "leftMenuOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify Left Menu Data in MDM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("leftMenuOperation :: No Data read for Left Menu from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for Left Menu from MetaData Processing File.";	
			}
		}
		leftMenuList=null;
		failureList=null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "leftMenuOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "leftMenuOperation()", e1);
			}
		}
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}
	
	private void mnaoVINOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<VINEntFileDetails> vinList=new ArrayList<VINEntFileDetails>();
		ArrayList<VINEntFileDetails> failureList=new ArrayList<VINEntFileDetails>();
		boolean proceedFurther=true;
		VINEntFileDetails details = null;
		try
		{
			// read VIN File
			vinList = MetaDataValidationUtils.readVINENTFile(processingFile);
		}
		catch(Exception e)
		{
			vinList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoVINOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for VIN from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}
		
		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=vinList && vinList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				int totalCount = vinList.size();
				int failureCount=0;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoVINOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					// PROCEED FOR CHECKING VIN EXISTS IN MDM + IM.
					try
					{
						vinList = metaDataDAO.checkMNAOVINStatus(vinList, locale, scheduleId);
						if(null!=vinList && vinList.size()>0)
						{
							failureList = new ArrayList<VINEntFileDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<vinList.size();a++)
							{
								details = (VINEntFileDetails)vinList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<vinList.size();a++)
							{
								details = (VINEntFileDetails)vinList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;
							
							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}

							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printMNAOVinReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
						else
						{
							// update scheduleStatus to failure
							updateFailureStatus=true;
							schdeuleRemarks="Failed to identify VINs in MDM & IM. List is returned as null.";
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoVINOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify VINs in MDM & IM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("mnaoVINOperation :: No Data read for VIN from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for VIN from MetaData Processing File.";	
			}
		}
		vinList=null;
		failureList = null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoVINOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "mnaoVINOperation()", e1);
			}
		}
		
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void vinAttributeOperation(String scheduleId, File processingFile, String metaDataType, String locale)
	{
		boolean updateFailureStatus=false;
		boolean updateSuccessStatus=false;
		String schdeuleRemarks=null;

		ArrayList<VinAttributeEntFileDetails> vinAttributeList=new ArrayList<VinAttributeEntFileDetails>();
		ArrayList<VinAttributeEntFileDetails> failureList=new ArrayList<VinAttributeEntFileDetails>();
		boolean proceedFurther=true;
		try
		{
			// read VIN ATTRIBUTE END File
			vinAttributeList = MetaDataValidationUtils.readVINAttributeENTFile(processingFile);
		}
		catch(Exception e)
		{
			vinAttributeList=null;
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "vinAttributeOperation()", e);
			proceedFurther=false;
			// update failureStatus
			updateFailureStatus=true;
			schdeuleRemarks="Failed to read data for VIN Attribute from MetaData Processing File. Exception while reading File ->"+ e.getMessage()+".";
		}

		// proceedFurther is true
		if(proceedFurther==true)
		{
			if(null!=vinAttributeList && vinAttributeList.size()>0)
			{
				// UPDATE TOTAL & FAILURE COUNT(FOR INVALID LINES ALL TOGETHER)
				VinAttributeEntFileDetails details = null;
				int totalCount = vinAttributeList.size();
				int failureCount=0;

				// update failureCount & totalCount
				proceedFurther=true;
				try
				{
					metaDataDAO.updateScheduleTotalAndFailureCount(scheduleId, totalCount, failureCount);
				}
				catch (Exception e1) 
				{
					Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "vinAttributeOperation()", e1);
					proceedFurther=false;
					// update failureStatus
					updateFailureStatus=true;
					schdeuleRemarks="Failed to update Total Lines count for the MetaData Processing File. Exception while updating ->"+e1.getMessage();
				}

				if(proceedFurther==true)
				{
					/*
					 *  PROCEED FOR CHECKING CATEGORIES FROM VIN ATTRIBUTE - ENIGNE TYPE, MISSION TYPE, BODY TYPE, AXLE TYPE
					 *  IN MDM & IM
					 */
					try
					{
						vinAttributeList = metaDataDAO.checkVINAttributeStatus(vinAttributeList, locale, scheduleId);
						if(null!=vinAttributeList && vinAttributeList.size()>0)
						{
							failureList = new ArrayList<VinAttributeEntFileDetails>();
							/*
							 * iterate and check for all if PROCESSING STATUS AS SUCCESS
							 * IF any FAILURE STATUS FOUND - Fail Schedule
							 */
							boolean schSuccess=true;
							for(int a=0;a<vinAttributeList.size();a++)
							{
								details = (VinAttributeEntFileDetails)vinAttributeList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									schSuccess = false;
									break;
								}
							}
							
							for(int a=0;a<vinAttributeList.size();a++)
							{
								details = (VinAttributeEntFileDetails)vinAttributeList.get(a);
								if((null==details.getProcessingStatus()) || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals(MetaDataScheduleConstants.STATUS_FAILURE)))
								{
									failureList.add(details);
								}
								details=null;
							}
							details=null;

							if(schSuccess==true)
							{
								// update scheduleStatus to success
								updateSuccessStatus = true;
							}
							else
							{
								// update scheduleStatus to failure
								updateFailureStatus=true;
							}


							/*
							 * call function to PRINT REPORTS
							 * do this only when scheduleStatus is failure
							 * and only for failed records in a text file with delimiter as ||
							 */
							if(updateFailureStatus==true)
							{
								printReportsUtil.printVinAttributeReport(scheduleId, failureList, locale);
								/*
								 * call function to generate Zip
								 * no need of generating zip, simply update the text file path
								 */
								generateZIPForReports(scheduleId);
							}
						}
						else
						{
							// update scheduleStatus to failure
							updateFailureStatus=true;
							schdeuleRemarks="Failed to identify VIN Attributes in MDM & IM. List is returned as null";
						}
					}
					catch (Exception e1) 
					{
						Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "vinAttributeOperation()", e1);
						// update failureStatus
						updateFailureStatus=true;
						schdeuleRemarks="Failed to identify VIN Attributes in MDM & IM. Exception while performing operation ->"+ e1.getMessage()+".";
					}
				}
			}
			else
			{
				logger.info("vinAttributeOperation :: No Data read for VIN Attributes from Processing File {"+processingFile.getName()+"}.");
				// update failureStatus
				updateFailureStatus=true;
				schdeuleRemarks="No Data read for VIN Attributes from MetaData Processing File.";	
			}
		}
		vinAttributeList=null;
		failureList=null;

		if(updateFailureStatus==true)
		{
			/*
			 * update schdeuleStatus to Failure - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_FAILURE, schdeuleRemarks);
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "vinAttributeOperation()", e1);
			}
		}
		
		if(updateSuccessStatus==true)
		{
			/*
			 * update schdeuleStatus to Success - Exit Conversion
			 */
			try 
			{
				metaDataDAO.updateScheduleStatus(scheduleId, MetaDataScheduleConstants.STATUS_SUCCESS, "");
			} 
			catch (Exception e1) 
			{
				Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "vinAttributeOperation()", e1);
			}
		}
		updateFailureStatus = false;
		updateSuccessStatus = false;
		schdeuleRemarks=  null;
	}

	private void getMarketSpecificLocales()
	{
		try
		{
			if(null==mnaoLocales || "".equals(mnaoLocales))
			{
				mnaoLocales = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mnao"));
			}
			if(null==mcLocales || "".equals(mcLocales))
			{
				mcLocales = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mc"));
			}
			if(null==mmeLocales || "".equals(mmeLocales))
			{
				mmeLocales=  Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationScheduleImpl.class.getName(), "getMarketSpecificLocales()", e);
		}
		
	}
	
}