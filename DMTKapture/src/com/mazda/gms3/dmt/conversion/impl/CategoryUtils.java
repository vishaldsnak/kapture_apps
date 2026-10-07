package com.mazda.gms3.dmt.conversion.impl;

import java.util.ArrayList;

import com.mazda.gms3.dmt.conversion.dao.MNAODocumentManagementDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class CategoryUtils {

	static Logger logger  =LogManager.getLogger(CategoryUtils.class);
	
	/**
	 * PERFORM CATEGORIES OPERATION
	 * 
	 * CHECK FOR MODEL - YEAR
	 * 	IF AVAILABLE - THEN ONLY MODEL - YEAR WILL GO
	 * 			CONTENTDETAILS.YEAR VALUE - 
	 * 			VIN ENT MODEL YEAR VALUES - 
	 * 			REF KEY FOR MODEL YEAR WILL BE - MODEL_YEAR
	 * ESI CATEGORIES - 
	 * 		ONLY PUSH SUB CATEGORIES - 
	 * 		REF KEY WILL BE - SUB CAT CODE
	 * 
	 * CVC CATEGORIES - 
	 * 		LOOK FOR THE LAST LEVEL - 
	 *		WHAT SO EVER ABAILABLE
	 *		REF KEY = CATCODE+SUBCATCODE+SYMPCODE+SUBSYMPCODE
	 *
	 * VIN ENT AS CATEGORIES
	 * 		PREPARE THE LAST LEVEL CATEGORY
	 * 		REF KEY - WMI+VDS+VISSTART+VISEND
	 * 
	 * VIN ATTRIBUTE AS CATEGORIES
	 * 		ENGINE TYPE TO BE FIGURED OUT
	 * 		TRANSMISISON TYPE TO BE FIGURED OUT
	 * 		BODY TYPE - REFKEY - B0+BODYTYPECODE
	 * 		AXLE TYPE - REFKEY  - D0+AXLETYPECODE	
	 * 
	 * ONCE THE REF KEYS ARE PREPARED, CHECK EACH CATEGORY
	 * 	IF ANY OF THE CATEGORY FAILS, 
	 * 		SKIP THE DOCUMENT AND ADD TO CATEGORY FAILURE LIST
	 * 
	 * STEERING TYPES
	 * 	IF RHD - RIGHT HAND DRIVE
	 * 	IF LHD - LEFT HAND DRIVE
	 * 
	 * 
	 * ADD ALL ENGINE TYPE / MISSION TYPE ON THE BASIS OF BOOK WHEN PROCESSING ENGINE / MT / AT DIRECTORY 
	 * @param contentDetails
	 * @return
	 */
	
	public static ContentDetails performCategoriesOperation(ContentDetails contentDetails)
	{
		try
		{
			contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
			/*
			 * CATEGORIES WILL BE - 
			 * MANUAL TYPE
			 * MODEL
			 * YEAR
			 * ESI CATEGORIES - CATEGORY TYPE AND SUBCATEGORY TYPE
			 * CVC CATEGORIES - 
			 * VIN INFORMATION
			 * FOR ENGINE / MISSION - FETCH ALL THE ENGINE TYPE CODES AND FETCH ALL THE MAPPED VIN INFO AGAINST IT.
			 * STEERING TYPE CATEGORIES
			 */
			
			/*
			 * CALL FUNCTION TO ADD MANUAL TYPE CATEGORY
			 */
			contentDetails = addManualTypeAsCategories(contentDetails);
			
			/*
			 * CALL FUNCTION TO ADD MODEL YEAR CATEGORY
			 */
			contentDetails = processModelYearCategories(contentDetails);
			
			/*
			 * CALL FUNCTION TO ADD ESI CATEGORIES
			 */
			contentDetails = processESICategories(contentDetails);
			
			/*
			 * CALL FUNCTION TO ADD STEERING TYPE CATEGORIES
			 */
			contentDetails = processSteeringTypeCategories(contentDetails);
			
			/*
			 * CALL FUNCTION TO ADD CVC CATEGORY
			 */
			if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
			{
				contentDetails = processCVCCategories(contentDetails);
			}
			
			/*
			 * CHECK FOR MAPPED VIN DATA AS CATEGORY
			 */
			if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
			{
				contentDetails=processVINCategories(contentDetails.getVinEntDetailsList(), contentDetails, "VIN");
			}
			
			/*
			 * 
			 * CHECK FOR MAPPED VIN ATTRIBUTE DATA AS CATEGORY
			 */
			if(null!=contentDetails.getVinAttributeEntDetailsList() && contentDetails.getVinAttributeEntDetailsList().size()>0)
			{
				contentDetails = processVINAttributeCategories(contentDetails.getVinAttributeEntDetailsList(), contentDetails);
			}
			
			/*
			 * CHECK FOR VIN MASTER DATA
			 */
			if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
			{
				contentDetails= processVINCategories(contentDetails.getMasterVinList(), contentDetails, "MASTER_VIN");
			}
			
			
			/*
			 * ADD ALL APPLICABLE ENGINE TYPES / MISSION TYPES 
			 */
			if(null!=contentDetails.getApplicableEngineMissionTypeList() && contentDetails.getApplicableEngineMissionTypeList().size()>0)
			{
				/*
				 * CALL FUNCTION TO ADD EACH OF THE TYPE CODES AS CATEGORY WITH DOCUMENT
				 */
				contentDetails = processEngineMissionTypeCategories(contentDetails.getApplicableEngineMissionTypeList(), contentDetails);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "performCategoriesOperation()", e);
		}
		return contentDetails;
	}
	
	
	
	
	/**
	 * Function will add MODEL AND YEAR AS CATEGORIES DATA
	 * FUNCTION CHANGE = 06 JUNE 2018
	 * NOW NO MODELS WILL BE MAPPED IF FOR ANY ROW YEAR IS NOT AVAILABLE - ADD IT TO MISSING CATEGORY REPORT AND MAKE IT AS FAILURE
	 * WITH REASON AS NO YEAR INFO AVAILABLE 
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processModelYearCategories(ContentDetails contentDetails)
	{
		try
		{
			/*
			 * ASLO CHECK VIN ENT FILE IF FOR ANY MODEL YEAR VALUE IS FOUND
			 * ADD TO CATEGORY LIST
			 */
			if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
			{
				for(int i=0;i<contentDetails.getVinEntDetailsList().size();i++)
				{
					VINEntFileDetails details = (VINEntFileDetails)contentDetails.getVinEntDetailsList().get(i);
					/*
					 * call function to get ModelName on the basis of CARLINE CODE and Locale
					 */
//					String modelName = "";
//					if(null!=details.getVinCarline() && !"".equals(details.getVinCarline()))
//					{
//						modelName=DocumentManagementDAO.getModelName(contentDetails.getLocale(), details.getVinCarline());
//						if(null!=modelName && !"".equals(modelName))
//						{
//							// for Model name - replace - by blank
//							modelName= modelName.replace("-", "");
//							modelName = Utilities.replaceCharsForRefKeys(modelName);
//						}
//					}
					
					/*
					 * HERE A NEW CHANGE - 17 SEPT 2026
					 * 	MODEL HAS TO BE READ FROM VIN ENT INSTEAD OF MODEL FOLDER NAME
					 */
					
//					if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()) && 
//							null!=details.getModelYear() && !"".equals(details.getModelYear()))
					if(null!=details.getModelNameVINEnt() && !"".equals(details.getModelNameVINEnt()) && 
							null!=details.getModelYear() && !"".equals(details.getModelYear()))
					{
						// MODEL NAME WILL BE READ FROM THE FOLDER NAME, NO OTHER MODELS WILL EVER BE USED WHEN WORKING ON 1 MODEL
						/*
						 * HERE A NEW CHANGE - 17 SEPT 2026
						 * 	MODEL HAS TO BE READ FROM VIN ENT INSTEAD OF MODEL FOLDER NAME
						 */
						String modelName="";
						if(null!=details.getModelNameVINEnt() && !"".equals(details.getModelNameVINEnt()))
						{
							modelName = details.getModelNameVINEnt();
						}
						if(null!=modelName && !"".equals(modelName))
						{
							// for Model name - replace - by blank
							modelName= modelName.replace("-", "");
							modelName = Utilities.replaceCharsForRefKeys(modelName);
						}
						
						String modelYearVal= details.getModelYear();
						if(modelYearVal.trim().toLowerCase().contains("my"))
						{
							modelYearVal = modelYearVal.replace("my", "");
							modelYearVal = modelYearVal.replace("MY", "");
							modelYearVal = modelYearVal.replace("mY", "");
							modelYearVal = modelYearVal.replace("My", "");
						}
						//ADD THIS YEAR AS REF KEY TO CATEGORIES LIST
						String refKey = modelName.toUpperCase()+"_"+modelYearVal.toUpperCase();
						boolean addToCatList = true;
						if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
						{
							for(int r=0;r<contentDetails.getCategoryList().size();r++)
							{
								CategoryDetails catDetails = (CategoryDetails)contentDetails.getCategoryList().get(r);
								if(catDetails.getCategoryRefKey().equals(refKey.toUpperCase()))
								{
									// MODEL YEAR ALREADY ADDED - SKIP IT.
									addToCatList = false;
									break;
								}
							}
						}
						
						if(addToCatList==true)
						{
							// add Year Ref Key which will be - MODEL_YEAR
							CategoryDetails cDetails = new CategoryDetails();
							/*
							 * HERE A NEW CHANGE - 17 SEPT 2026
							 * 	MODEL HAS TO BE READ FROM VIN ENT INSTEAD OF MODEL FOLDER NAME
							 */
//							cDetails.setCategoryName(contentDetails.getModel()+"::"+ modelYearVal);
							cDetails.setCategoryName(details.getModelNameVINEnt()+"::"+ modelYearVal);
							cDetails.setCategoryRefKey(refKey);
							logger.info("processModelYearCategories :: Adding MODEL YEAR :: > " + cDetails.getCategoryRefKey());
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("MODEL_YEAR");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							cDetails= null;
						}
						refKey=  null;
						modelName  = null;
					}
					else
					{
						/*
						 * add each row Document to the MISSING CATEGORY LIST STATING THAT NO YEAR FOUND FOR THE MODEL
						 * THIS WILL BE APPLICABLE FOR ALL MANUAL TYPES EXCEPT ENGINE, AT & MT.
						 */
						
						/*
						 * HERE A NEW CHANGE - 17 SEPT 2026
						 * 	MODEL HAS TO BE READ FROM VIN ENT INSTEAD OF MODEL FOLDER NAME
						 */
						CategoryDetails catDetails = new CategoryDetails();
//						catDetails.setModel(contentDetails.getModel());
						catDetails.setModel(details.getModelNameVINEnt());
						catDetails.setCarlineCode(details.getVinCarline());
						catDetails.setWmiCode(details.getVinWMI());
						catDetails.setVdsCode(details.getVinVDS());
						// set type as MODEL_YEAR
						catDetails.setCategoryType("MODEL_YEAR");
						// before adding check if already added to Error List or not
						boolean add = true;
						if(null!=contentDetails.getModelsWithMissingYearsList() && contentDetails.getModelsWithMissingYearsList().size()>0)
						{
							for(int t=0;t<contentDetails.getModelsWithMissingYearsList().size();t++)
							{
								CategoryDetails exist = (CategoryDetails)contentDetails.getModelsWithMissingYearsList().get(t);
								if(null!=exist.getModel() && null!=exist.getCarlineCode() && null!=exist.getWmiCode() && null!=exist.getVdsCode() && 
										null!=exist.getCategoryType() && null!=catDetails.getCategoryType() && 
										null!=catDetails.getModel() && null!=catDetails.getCarlineCode() && null!=catDetails.getWmiCode() && null!=catDetails.getVdsCode())
								{
									if(exist.getModel().trim().toLowerCase().equals(catDetails.getModel().trim().toLowerCase()) && 
											exist.getCarlineCode().trim().toLowerCase().equals(catDetails.getCarlineCode().trim().toLowerCase()) && 
											exist.getWmiCode().trim().toLowerCase().equals(catDetails.getWmiCode().trim().toLowerCase()) && 
											exist.getVdsCode().trim().toLowerCase().equals(catDetails.getVdsCode().trim().toLowerCase()) && 
											exist.getCategoryType().trim().toLowerCase().equals(catDetails.getCategoryType().trim().toLowerCase()))
									{
										// already added do not add
										add = false;
										break;
									}
								}
							}
						}
						
						if(add==true)
						{
							// add reason
							catDetails.setRemarks("NO YEAR AVAILABLE IN THE NODE IN VIN.ENT FILE.");
							if(null==contentDetails.getModelsWithMissingYearsList() || contentDetails.getModelsWithMissingYearsList().size()<=0)
							{
								contentDetails.setModelsWithMissingYearsList(new ArrayList<CategoryDetails>());
							}
							contentDetails.getModelsWithMissingYearsList().add(catDetails);
						}
						catDetails =  null;
					}
					
					details  =null;
				}
			}
			
			/*
			 * ASLO CHECK VIN MASTER LIST IF FOR ANY MODEL YEAR VALUE IS FOUND
			 * ADD TO CATEGORY LIST
			 * THIS HAPPENS WHEN MISSION OR ENGINE - 
			 * 
			 * IDENITFY FOR WHAT ALL MODELS YEAR IS NOT AVAILABLE
			 * 	ADD THOSE MODELS TO MISSING CATEGORY LIST SO THAT THE JOB CAN BE FAILURE.
			 * 
			 * NEW CHANGE (14 SEPTEMBER 2018)- FOR MASTER VIN LIST IF MODEL YEAR IS NOT FOUND - IN THAT CASE
			 * FETCH ALL MODEL YEAR RANGES FROM CARLINE TABLE ON THE BASIS OF
			 * CARLINE CODE + MODEL NAME + WMI CODE AND ALL MODEL YEAR RANGES AS CATEGORY TO DOCUMENT
			 * 
			 * NO NEED OF ADDING SUCH CASES TO MISSING CATEGORY LIST, FOR MAKING JOB FAILURE 
			 * 			 
			 */
			if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
			{
				MNAODocumentManagementDAO mnaoDocumentDAO = new MNAODocumentManagementDAO();
				for(int i=0;i<contentDetails.getMasterVinList().size();i++)
				{
					VINEntFileDetails details = (VINEntFileDetails)contentDetails.getMasterVinList().get(i);
					/*
					 * PROCEED ONLY IF YEAR INFO IS AVAILABLE FOR EACH ROW
					 */
					if(null!=details.getModelCode() && !"".equals(details.getModelCode()) && null!=details.getModelYear() && !"".equals(details.getModelYear()))
					{
						/*
						 * Here, no need to fetch MODEL NAME, as it will be already available when fetching Master Data
						 */
						String modelName = details.getModelCode();
						// for Model name - replace - by blank
						modelName= modelName.replace("-", "");
						modelName = Utilities.replaceCharsForRefKeys(modelName);

						//ADD THIS YEAR AS REF KEY TO CATEGORIES LIST
						String refKey = modelName.toUpperCase()+"_"+details.getModelYear();
						boolean addToCatList = true;
						if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
						{
							for(int r=0;r<contentDetails.getCategoryList().size();r++)
							{
								CategoryDetails catDetails = (CategoryDetails)contentDetails.getCategoryList().get(r);
								if(catDetails.getCategoryRefKey().equals(refKey.toUpperCase()))
								{
									// MODEL YEAR ALREADY ADDED - SKIP IT.
									addToCatList = false;
									break;
								}
							}
						}

						if(addToCatList==true)
						{
							// add Year Ref Key which will be - MODEL
							CategoryDetails cDetails = new CategoryDetails();
							cDetails.setCategoryName(details.getModelCode()+"::"+details.getModelYear());
							logger.info("processModelYearCategories :: Adding MODEL YEAR {ENGINE / MISSION} :: > " + refKey);
							cDetails.setCategoryRefKey(refKey);
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("MODEL_YEAR");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							cDetails= null;
						}
						refKey=  null;
						modelName = null;
					}
//					else
//					{
//						/*
//						 * ADD TO MIDDING MODEL YEAR LIST IN CONTENT DETAILS
//						 * PREVIOUS ELSE CONDITION - COMMENTING BECAUSE OF CHANGE 
//						 * ON 14 SEPTEMBER 2018
//						 */
//						CategoryDetails catDetails = new CategoryDetails();
//						catDetails.setModel(details.getModelCode());
//						catDetails.setCarlineCode(details.getVinCarline());
//						catDetails.setWmiCode(details.getVinWMI());
//						catDetails.setVdsCode(details.getVinVDS());
//						// set type as MODEL_YEAR
//						catDetails.setCategoryType("MODEL_YEAR");
//						String reason="";
//						if(null==details.getModelCode() || "".equals(details.getModelCode()))
//						{
//							reason+="NO MODEL FOUND FOR THE MASTER VDS.";
//						}
//						if(null==details.getModelYear() || "".equals(details.getModelYear()))
//						{
//							reason+="NO YEAR FOUND FROM THE MASTER VDS.";
//						}
//						catDetails.setErrorMessage(reason);
//						if(null==contentDetails.getModelsWithMissingYearsList() || contentDetails.getModelsWithMissingYearsList().size()<=0)
//						{
//							contentDetails.setModelsWithMissingYearsList(new ArrayList<CategoryDetails>());
//						}
//						contentDetails.getModelsWithMissingYearsList().add(catDetails);
//						catDetails = null;
//						reason = null;
//					}
					else
					{
						/*
						 * NEW CHANGE CONDITION - 14 SEPT 2018
						 * FETCH YEAR RANGES ON THE BASIS OF LOCALE + CARLINE CODE + NAME + WMI CODE FROM CARLINE TABLE
						 * AND ALL AS CATEGORIES TO DOCUMENT
						 */
						
						String modelName = details.getModelCode();
						// for Model name - replace - by blank
						modelName= modelName.replace("-", "");
						modelName = Utilities.replaceCharsForRefKeys(modelName);

						//ADD THIS YEAR AS REF KEY TO CATEGORIES LIST
						String refKey = "";
						/*
						 * DATE CHANGE - 17 SEPT 2026
						 * HERE INSTEAD OF IDENTIFYING YEAR RANGES FOR MODEL ON THE BASIS OF CONTENT LOCALE
						 * USE LOCALE MAPPED WITH CARLINE IDENTIFED WHEN MASTER VIN LIST WAS FETCHED
						 * E.G INSTEAD OF contentDetails.getLocale() USE
						 */
						ArrayList<String> rangeList = mnaoDocumentDAO.getModelYearRangesList(details.getLangCodeForYearRange(), details.getVinCarline(), details.getVinWMI(), details.getModelCode());
						if(null!=rangeList && rangeList.size()>0)
						{
							for(int u=0;u<rangeList.size();u++)
							{
								refKey = modelName.toUpperCase()+"_"+rangeList.get(u);
								// before adding check if the category is already added or not. If yes then skip it else add
								boolean add =true;
								if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
								{
									for(int d=0;d<contentDetails.getCategoryList().size();d++)
									{
										CategoryDetails exist = (CategoryDetails)contentDetails.getCategoryList().get(d);
										if(null!=exist.getCategoryRefKey())
										{
											if(exist.getCategoryRefKey().trim().toLowerCase().equals(refKey.trim().toLowerCase()))
											{
												// already added
												add = false;
												break;
											}
										}
									}
								}
								
								if(add ==true)
								{
									// add Year Ref Key which will be - MODEL
									CategoryDetails cDetails = new CategoryDetails();
									cDetails.setCategoryName(details.getModelCode()+"::"+rangeList.get(u));
									logger.info("processModelYearCategories :: Adding MODEL YEAR {ENGINE / MISSION} :: > " + refKey);
									cDetails.setCategoryRefKey(refKey);
									cDetails.setCategoryType("MODEL_YEAR");
									if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
									{
										contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
									}

									// add cDetails to categoryList
									contentDetails.getCategoryList().add(cDetails);
									cDetails= null;
								}
								refKey = null;
							}
						}
						else
						{
							/*
							 *  ADD MODEL AND ADD TO MISSING CATEGORIES LIST STATING NO YEAR RANGES COUND BE FOUND FOR COMBINATION.
							 *  ADD TO MIDDING MODEL YEAR LIST IN CONTENT DETAILS
							 */
							CategoryDetails catDetails = new CategoryDetails();
							catDetails.setModel(details.getModelCode());
							catDetails.setCarlineCode(details.getVinCarline());
							catDetails.setWmiCode(details.getVinWMI());
							catDetails.setVdsCode(details.getVinVDS());
							// set type as MODEL_YEAR
							catDetails.setCategoryType("MODEL_YEAR");
							// before adding check if already added to Error List or not
							boolean add = true;
							if(null!=contentDetails.getModelsWithMissingYearsList() && contentDetails.getModelsWithMissingYearsList().size()>0)
							{
								for(int t=0;t<contentDetails.getModelsWithMissingYearsList().size();t++)
								{
									CategoryDetails exist = (CategoryDetails)contentDetails.getModelsWithMissingYearsList().get(t);
									if(null!=exist.getModel() && null!=exist.getCarlineCode() && null!=exist.getWmiCode() && null!=exist.getVdsCode() && 
											null!=exist.getCategoryType() && null!=catDetails.getCategoryType() && 
											null!=catDetails.getModel() && null!=catDetails.getCarlineCode() && null!=catDetails.getWmiCode() && 
											null!=catDetails.getVdsCode())
									{
										if(exist.getModel().trim().toLowerCase().equals(catDetails.getModel().trim().toLowerCase()) && 
												exist.getCarlineCode().trim().toLowerCase().equals(catDetails.getCarlineCode().trim().toLowerCase()) && 
												exist.getWmiCode().trim().toLowerCase().equals(catDetails.getWmiCode().trim().toLowerCase()) && 
												exist.getVdsCode().trim().toLowerCase().equals(catDetails.getVdsCode().trim().toLowerCase()) && 
												exist.getCategoryType().trim().toLowerCase().equals(catDetails.getCategoryType().trim().toLowerCase()))
										{
											// already added do not add
											add = false;
											break;
										}
									}
								}
							}
							
							if(add==true)
							{
								catDetails.setRemarks("NO YEAR RANGES FOUND FROM THE MASTER VDS FOR COMBINATION OF MODEL NAME + CARLINE CODE + WMI CODE.");
								if(null==contentDetails.getModelsWithMissingYearsList() || contentDetails.getModelsWithMissingYearsList().size()<=0)
								{
									contentDetails.setModelsWithMissingYearsList(new ArrayList<CategoryDetails>());
								}
								contentDetails.getModelsWithMissingYearsList().add(catDetails);
							}
							catDetails = null;
						}
						rangeList = null;
						modelName = null;
						refKey = null;
					}
					details  =null;
				}
				mnaoDocumentDAO = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processModelYearCategories()", e);
		}
		return contentDetails;
	}
	
	public static ContentDetails processModelYearCategories_BackUp(ContentDetails contentDetails)
	{
		try
		{
			boolean yearRefKeyAdded = false;
			/*
			 * ASLO CHECK VIN ENT FILE IF FOR ANY MODEL YEAR VALUE IS FOUND
			 * ADD TO CATEGORY LIST
			 */
			if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
			{
				for(int i=0;i<contentDetails.getVinEntDetailsList().size();i++)
				{
					VINEntFileDetails details = (VINEntFileDetails)contentDetails.getVinEntDetailsList().get(i);
					/*
					 * call function to get ModelName on the basis of CARLINE CODE and Locale
					 */
//					String modelName = "";
//					if(null!=details.getVinCarline() && !"".equals(details.getVinCarline()))
//					{
//						modelName=DocumentManagementDAO.getModelName(contentDetails.getLocale(), details.getVinCarline());
//						if(null!=modelName && !"".equals(modelName))
//						{
//							// for Model name - replace - by blank
//							modelName= modelName.replace("-", "");
//							modelName = Utilities.replaceCharsForRefKeys(modelName);
//						}
//					}
					
					// MODEL NAME WILL BE READ FROM THE FOLDER NAME, NO OTHER MODELS WILL EVER BE USED WHEN WORKING ON 1 MODEL
					String modelName="";
					if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
					{
						modelName = contentDetails.getModel();
					}
					if(null!=modelName && !"".equals(modelName))
					{
						// for Model name - replace - by blank
						modelName= modelName.replace("-", "");
						modelName = Utilities.replaceCharsForRefKeys(modelName);
					}
					if(null!=details.getModelYear() && !"".equals(details.getModelYear()) && null!=modelName && !"".equals(modelName))
					{
						String modelYearVal= details.getModelYear();
						if(modelYearVal.trim().toLowerCase().contains("my"))
						{
							modelYearVal = modelYearVal.replace("my", "");
							modelYearVal = modelYearVal.replace("MY", "");
							modelYearVal = modelYearVal.replace("mY", "");
							modelYearVal = modelYearVal.replace("My", "");
						}
						//ADD THIS YEAR AS REF KEY TO CATEGORIES LIST
						String refKey = modelName.toUpperCase()+"_"+modelYearVal.toUpperCase();
						boolean addToCatList = true;
						if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
						{
							for(int r=0;r<contentDetails.getCategoryList().size();r++)
							{
								CategoryDetails catDetails = (CategoryDetails)contentDetails.getCategoryList().get(r);
								if(catDetails.getCategoryRefKey().equals(refKey.toUpperCase()))
								{
									// MODEL YEAR ALREADY ADDED - SKIP IT.
									addToCatList = false;
									break;
								}
							}
						}
						
						if(addToCatList==true)
						{
							// add Year Ref Key which will be - MODEL_YEAR
							CategoryDetails cDetails = new CategoryDetails();
							cDetails.setCategoryName(contentDetails.getModel()+"::"+ modelYearVal);
							logger.info("processModelYearCategories :: Adding MODEL YEAR :: > " + cDetails.getCategoryRefKey());
							cDetails.setCategoryRefKey(refKey);
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("MODEL_YEAR");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							yearRefKeyAdded = true;
							cDetails= null;
						}
						refKey=  null;
					}
					modelName  = null;
					details  =null;
				}
			}
			
			
			/*
			 * ASLO CHECK VIN MASTER LIST IF FOR ANY MODEL YEAR VALUE IS FOUND
			 * ADD TO CATEGORY LIST
			 * THIS HAPPENS WHEN MISSION OR ENGINE - SO IDENTIFY THE MODEL AND ADD TO CATEGORY LIST
			 * HERE - NO MODEL YEAR WILL BE AVAILABEL
			 */
			if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
			{
				for(int i=0;i<contentDetails.getMasterVinList().size();i++)
				{
					VINEntFileDetails details = (VINEntFileDetails)contentDetails.getMasterVinList().get(i);
					/*
					 * Here, no need to fetch MODEL NAME, as it will be already available when fetching Master Data
					 */
					String modelName = details.getModelCode();
					if(null!=modelName && !"".equals(modelName))
					{
						// for Model name - replace - by blank
						modelName= modelName.replace("-", "");
						modelName = Utilities.replaceCharsForRefKeys(modelName);
					
						//ADD THIS YEAR AS REF KEY TO CATEGORIES LIST
						String refKey = modelName.toUpperCase();
						boolean addToCatList = true;
						if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
						{
							for(int r=0;r<contentDetails.getCategoryList().size();r++)
							{
								CategoryDetails catDetails = (CategoryDetails)contentDetails.getCategoryList().get(r);
								if(catDetails.getCategoryRefKey().equals(refKey.toUpperCase()))
								{
									// MODEL ALREADY ADDED - SKIP IT.
									addToCatList = false;
									break;
								}
							}
						}
						
						if(addToCatList==true)
						{
							// add Year Ref Key which will be - MODEL
							CategoryDetails cDetails = new CategoryDetails();
							cDetails.setCategoryName(details.getModelCode());
							logger.info("processModelYearCategories :: Adding MODEL {ENGINE / MISSION} :: > " + refKey);
							cDetails.setCategoryRefKey(refKey);
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("MODEL");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							cDetails= null;
						}
						refKey=  null;
					}
					details  =null;
					modelName = null;
				}
			}
			
			
			if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
			{
				if(!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) && 
						!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) &&  
						!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) && 
						!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) && 
						!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) 
						&& !contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) && 
						!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
				{
					String modelName = contentDetails.getModel();
					if(null!=modelName && !"".equals(modelName))
					{
						// for Model name - replace - by blank
						modelName= modelName.replace("-", "");
						modelName = Utilities.replaceCharsForRefKeys(modelName);
					}
					
					// CHECK FOR YEAR FROM CONTENT VARIABLES VALUE (THIS WILL BE READ FROM META TAG OF THE HTML FILE
					if(null!=contentDetails.getYear() && !"".equals(contentDetails.getYear()))
					{
						// add Year Ref Key which will be - MODEL_YEAR
						CategoryDetails cDetails = new CategoryDetails();
						cDetails.setCategoryName(contentDetails.getModel()+"::"+ contentDetails.getYear());
						cDetails.setCategoryRefKey(modelName.toUpperCase()+"_"+contentDetails.getYear());
						logger.info("processModelyearCategories :: Adding MODEL YEAR :: > " + cDetails.getCategoryRefKey());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						cDetails.setCategoryType("MODEL_YEAR");
						// add cDetails to categoryList
						contentDetails.getCategoryList().add(cDetails);
						yearRefKeyAdded  = true;
						cDetails= null;
					}
					modelName = null;
				}
			}
			
			
			/*
			 * CHECK HERE, IF YEAR REF KET IS NOT ADDED - THEN CHECK FOR MODEL
			 * IF NOT ENGINE / MISSION - CREATE ITS REF KEY
			 * AND ADD TO CATEGORY LIST
			 */
			if(yearRefKeyAdded==false)
			{
				if(null!=contentDetails.getModel() && !"".equals(contentDetails.getModel()))
				{
					logger.info("processModelyearCategories :: NO YEAR REF KEYS FOUND - ADDING - MODEL AS CATEGORY.");
					if(!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) && 
							!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) &&  
							!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) && 
							!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) && 
							!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) 
							&& !contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) && 
							!contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
					{
						String modelName = contentDetails.getModel();
						if(null!=modelName && !"".equals(modelName))
						{
							// for Model name - replace - by blank
							modelName= modelName.replace("-", "");
							modelName = Utilities.replaceCharsForRefKeys(modelName);
						}
						CategoryDetails cDetails = new CategoryDetails();
						cDetails.setCategoryName(contentDetails.getModel());
						cDetails.setCategoryRefKey(modelName.toUpperCase());
						logger.info("processModelyearCategories :: Adding MODEL {NO YEAR INFO AVAILABLE }:: > " + cDetails.getCategoryRefKey());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						cDetails.setCategoryType("MODEL");
						// add cDetails to categoryList
						contentDetails.getCategoryList().add(cDetails);
						cDetails  = null;
						modelName = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processModelYearCategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * FUNCTION WILL ADD ESI CATEGORIES
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processESICategories(ContentDetails contentDetails)
	{
		try
		{
			/*
			 * PREPARE TAXONOMY FOR THE DOCUMENT, THIS NEEDS TO BE ADDED TO TRNASACTION REPORT
			 */
			String taxonomy="";
			if(null!=contentDetails.getCategoryCode() && !"".equals(contentDetails.getCategoryCode()))
			{
				taxonomy=contentDetails.getCategoryCode().trim()+"||";
				if(null!=contentDetails.getSubCategoryCode() && !"".equals(contentDetails.getSubCategoryCode()))
				{
					taxonomy=taxonomy+contentDetails.getSubCategoryCode().trim()+"||";
					if(null!=contentDetails.getSubSubCategoryCode() && !"".equals(contentDetails.getSubSubCategoryCode()))
					{
						taxonomy=taxonomy+contentDetails.getSubSubCategoryCode().trim();
					}
				}
			}
			
			if(null!=taxonomy && !"".equals(taxonomy))
			{
				contentDetails.setMappedTaxonomy(taxonomy);
			}
			taxonomy = null;
			
			/*
			 * CHECK HERE IF CHANNEL NAME IS WIRING DIAGRAMS
			 * AND CATEGORY CODE IS NOT NULL = THEN ADD CATEGORY TO WD
			 * ELSE WORK ON SUB CATEGORY
			 * 
			 * NOT APPLICABLE NOW, SINCE PDFS IN WD PROCESSING HAS CHANGED AND NOW THEY ALSO HAVE 
			 * 3 LEVEL CATEGORY CODES
			 */
//			if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName())
//					&& contentDetails.getChannelName().trim().toLowerCase().
//					equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME").trim().toLowerCase()))
//			{
//				/*
//				 * ONLY WHEN DOCUMENT TYPE IS PDF
//				 */
//				if(null!=contentDetails.getDocumentType() && !"".equals(contentDetails.getDocumentType()) && 
//						contentDetails.getDocumentType().equals(ContentDetails.PDF_DOCUMENT))
//				{
//					if(null!=contentDetails.getCategoryCode() && !"".equals(contentDetails.getCategoryCode()))
//					{
//						// SET MAPPED CATEGORY AS LEVEL 1 && CATEGORY NAME AS CODE
//						contentDetails.setMappedCategoryLevel("LEVEL 1");
//						contentDetails.setMappedCategoryName(contentDetails.getCategoryCode().trim());
//						
//						String name=contentDetails.getCategoryCode();
//						/*
//						 * ADD CATEGORY TO THE DOCUMENT - GET THE PREFIC AND ADD CATEGORY CODE TO IT 
//						 */
//						// ADD TO CATEGORY DETAILS
//						CategoryDetails cDetails = new CategoryDetails();
//						cDetails.setCategoryName(name.trim().toUpperCase());
//						String refKey =ApplicationProperties.getProperty("prefix.esicategory.type.refkey")+name;
//						cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
//						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
//						{
//							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
//						}
//						// add cDetails to categoryList
//						contentDetails.getCategoryList().add(cDetails);
//						cDetails= null;
//						
//					}
//				}
//			}
			
			/*
			 * APPLICABE FOR ALL CHANNELS - WIRING DIAGRAM AND SERVICE MANUALS.
			 * IF SUB SUB CATEGORY CODE IS NOT NULL
			 * 	CHECK IF SUB SUB CATEGORY CODE IS 00
			 * 		THEN ADD SUB CATEGORY TO THE DOCUMENT
			 * 	ELSE 
			 * 		PREPARE SUB SUB CATEGORY REF KEY AND ADD TO 
			 * 		DOCUMENT.
			 * 
			 * 
			 */
			if(null!=contentDetails.getSubCategoryCode() && !"".equals(contentDetails.getSubCategoryCode()) && 
					null!=contentDetails.getSubSubCategoryCode() && !"".equals(contentDetails.getSubSubCategoryCode()))
			{
				/*
				 * NO NEED TO DO ANYTHING WITH THE CATEGORY CODE 
				 */
				if(!contentDetails.getSubSubCategoryCode().trim().equals("00"))
				{
					logger.info("processESICategories :: Adding SUB SUB Category to the Document ::  > " + contentDetails.getFilePath());
					/*
					 * NO NEED TO DO ANYTHING WITH THE CATEGORY CODE -  POPULATE SUB SUB CATEGORY ONLY
					 * REF KEY WILL BE SUBCATEGORY CODE + SUB SUB CATEGORY CODE
					 */
					String refKey=  contentDetails.getSubCategoryCode().trim()+contentDetails.getSubSubCategoryCode().trim();
					CategoryDetails cDetails = new CategoryDetails();
					cDetails.setCategoryName(refKey);
					refKey = Utilities.replaceCharsForRefKeys(refKey.trim().toUpperCase());
					// THESE ARE ESI CATEGORIES -
					cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
					if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
					{
						contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
					}
					cDetails.setCategoryType("ESI_CATEGORY");
					// add cDetails to categoryList
					contentDetails.getCategoryList().add(cDetails);
					
					// here Mapped Level is 3rd Level
					contentDetails.setEsiCategoryMapped("Y");
					contentDetails.setEsiCategoryMappedLevel("LEVEL3");
					contentDetails.setEsiCategoryMappedRefKey(refKey);
					
					cDetails= null;
					refKey = null;
				}
				else
				{
					logger.info("processESICategories :: SUB SUB Category is {00}. Adding SUB CATEGORY to the Document ::  > " + contentDetails.getFilePath());
					/*
					 * NO NEED TO DO ANYTHING WITH THE CATEGORY CODE -  POPULATE SUB CATEGORY ONLY
					 * REF KEY WILL BE SUBCATEGORY CODE
					 */
					String refKey= contentDetails.getSubCategoryCode();
					CategoryDetails cDetails = new CategoryDetails();
					cDetails.setCategoryName(refKey);
					refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
					// THESE ARE ESI CATEGORIES -
					cDetails.setCategoryRefKey(refKey.trim());
					if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
					{
						contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
					}
					cDetails.setCategoryType("ESI_CATEGORY");
					// add cDetails to categoryList
					contentDetails.getCategoryList().add(cDetails);
					
					// here Mapped Level is 2nd Level
					contentDetails.setEsiCategoryMapped("Y");
					contentDetails.setEsiCategoryMappedLevel("LEVEL2");
					contentDetails.setEsiCategoryMappedRefKey(refKey);
					
					cDetails= null;
					refKey = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processESICategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will add all the ENGINE TYPES / MISSION TYPES AS CATEGORIES WITH THE DOCUMENT
	 * @param typeCodeList
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processEngineMissionTypeCategories(ArrayList<String> typeCodeList, ContentDetails contentDetails)
	{
		try
		{
			if(null!=typeCodeList && typeCodeList.size()>0)
			{
				for(int i=0;i<typeCodeList.size();i++)
				{
					String typeCode = String.valueOf(typeCodeList.get(i));
					if(null!=typeCode && !"".equals(typeCode))
					{
						// ADD TO CATEGORY DETAILS
						CategoryDetails cDetails = new CategoryDetails();
						cDetails.setCategoryName(typeCode.trim().toUpperCase());
						cDetails.setCategoryRefKey(typeCode.trim().toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						cDetails.setCategoryType("ENGINE_MISSION_TYPE");
						// add cDetails to categoryList
						contentDetails.getCategoryList().add(cDetails);
						cDetails= null;
					}
					typeCode = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processEngineMissionTypeCategories()", e);
		}
		return contentDetails;
	}
	
	
	/**
	 * Function will Process STEERING TYPE CATEGORIES WITHT THE DOCUMENT
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processSteeringTypeCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getRhdlhdIndicator() && !"".equals(contentDetails.getRhdlhdIndicator()))
			{
				/*
				 * CHECK HERE IF getRhdlhdIndicator IS = LHD / RHD
				 * FETCH THE REF KEYS FROM PROPERTIES FILE AND ADD TO CATEGORIES LIST
				 */
				String refKey= "";
				if(contentDetails.getRhdlhdIndicator().trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.lhd")))
				{
					refKey = ApplicationProperties.getProperty("steering.type.refkey.lhd");
				}
				else if(contentDetails.getRhdlhdIndicator().trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.rhd")))
				{
					refKey = ApplicationProperties.getProperty("steering.type.refkey.rhd");
				}
				
				CategoryDetails cDetails = new CategoryDetails();
				cDetails.setCategoryName(contentDetails.getRhdlhdIndicator());
				refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
				// THESE ARE ESI CATEGORIES -
				cDetails.setCategoryRefKey(refKey.trim());
				if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
				{
					contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
				}
				cDetails.setCategoryType("STEERING_TYPE");
				// add cDetails to categoryList
				contentDetails.getCategoryList().add(cDetails);
				cDetails= null;
				refKey = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processESICategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will process CVC Categories
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processCVCCategories(ContentDetails contentDetails)
	{
		try
		{
			for(int a=0;a<contentDetails.getCvcCategoryList().size();a++)
			{
				CVCCategoryDetails ccDetails =  (CVCCategoryDetails)contentDetails.getCvcCategoryList().get(a);
				/*
				 * THE LAST REF KEY WILL GO, WHICH WILL BE COMBINATION OF 
				 * CAT CODE + SYMP CODE + SUBSYMPCODE + COND CODE
				 */
				String refKey="";
				String name="";
				if(null!=ccDetails.getCategoryCode() && !"".equals(ccDetails.getCategoryCode()))
				{
					refKey = "CVC_";
					// add CAT CODE TO REF KEY
					name = ccDetails.getCategoryCode().trim();
					refKey= refKey+ccDetails.getCategoryCode().trim();
//					if(null!=ccDetails.getSubCategoryCode() && !"".equals(ccDetails.getSubCategoryCode()))
//					{
//						// add SUB CAT CODE TO REF KEY
//						name = ccDetails.getSubCategoryCode().trim();
//						refKey = refKey+ccDetails.getSubCategoryCode().trim();
//						if(null!=ccDetails.getSymptomCode() && !"".equals(ccDetails.getSymptomCode()))
//						{
//							// add SYMP CODE TO REF KEY
//							name = ccDetails.getSymptomCode().trim();
//							refKey = refKey+ccDetails.getSymptomCode().trim();
//							if(null!=ccDetails.getSubSymptomCode() && !"".equals(ccDetails.getSubSymptomCode()))
//							{
//								// add SUB SYMP CODE TO REF KEY
//								name = ccDetails.getSubSymptomCode().trim();
//								refKey=refKey+ccDetails.getSubSymptomCode().trim();
//								if(null!=ccDetails.getConditionCode() && !"".equals(ccDetails.getConditionCode()))
//								{
//									// add COND CODE TO REF KEY
//									name = ccDetails.getConditionCode().trim();
//									refKey = refKey+ccDetails.getConditionCode().trim();
//								}
//							}
//						}
//					}
					
					if(null!=ccDetails.getSymptomCode() && !"".equals(ccDetails.getSymptomCode()))
					{
						// add SYMP CODE TO REF KEY
						name = name+ccDetails.getSymptomCode().trim();
						refKey = refKey+ccDetails.getSymptomCode().trim();
						if(null!=ccDetails.getSubSymptomCode() && !"".equals(ccDetails.getSubSymptomCode()))
						{
							// add SUB SYMP CODE TO REF KEY
							name = name+ccDetails.getSubSymptomCode().trim();
							refKey=refKey+ccDetails.getSubSymptomCode().trim();
							if(null!=ccDetails.getConditionCode() && !"".equals(ccDetails.getConditionCode()))
							{
								// add COND CODE TO REF KEY
								name = name+ccDetails.getConditionCode().trim();
								refKey = refKey+ccDetails.getConditionCode().trim();
							}
						}
					}
				}
				
				if(null!=refKey && !"".equals(refKey))
				{
					// ADD CVC Categories
					CategoryDetails cDetails = new CategoryDetails();
					cDetails.setCategoryName(name.trim());
					refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
					cDetails.setCategoryRefKey(refKey.toUpperCase());
					if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
					{
						contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
					}
					cDetails.setCategoryType("CVC_CATEGORY");
					contentDetails.getCategoryList().add(cDetails);
					cDetails= null;
				}
				refKey = null;
				name = null;
				ccDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processCVCCategories", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will add VIN ENT FILE AND MASTER VIN AS CATEGORIES FOR DOCUMENT
	 * @param vinList
	 * @param contentDetails
	 */
	private static ContentDetails processVINCategories(ArrayList<VINEntFileDetails> vinList, ContentDetails contentDetails, String vinType)
	{
		try
		{
			String vinMapped=null;
			/*
			 * PREPARE A UNIQUE ENGINE TYPE / MISSION TYPE LIST 
			 * FROM THE vinList - THIS WILL BE AVAILABLE ONLY WHEN PROCESSING VIN MASTER LIST
			 */
			ArrayList<String> typeList = new ArrayList<String>();
			
			for(int i=0;i<vinList.size();i++)
			{
				VINEntFileDetails vinEntFileDetails = (VINEntFileDetails)vinList.get(i);
				/*
				 * iterate and MAP EACH OF THE ITEM AS CATEGORY
				 * WMI - REF_KEY - WMI
				 * VDS - REF_KEY - WMI_VDS
				 * VISSTART - REF_KEY - WMI_VDS_VISSTART
				 * VISEND - REF_KEY - WMI_VDS_VISEND
				 * 
				 * HERE, THE LAST LEVEL WILL GO WHICH WILL BE VISEND, REF KEY WILL BE - WMI+VDS+VISSTART+VISEND
				 */
				if(null!=vinEntFileDetails.getVinWMI() && !"".equals(vinEntFileDetails.getVinWMI() ))
				{
					String refKey="";
					String name="";
					refKey = vinEntFileDetails.getVinWMI().trim();
					name = vinEntFileDetails.getVinWMI().trim();
					// PROCESS VDS
					if(null!=vinEntFileDetails.getVinVDS() & !"".equals(vinEntFileDetails.getVinVDS()))
					{
						refKey = refKey+vinEntFileDetails.getVinVDS().trim();
						name=name+vinEntFileDetails.getVinVDS().trim();
						// PROCESS VIS
						if(null!=vinEntFileDetails.getVinStartRange() && !"".equals(vinEntFileDetails.getVinStartRange()))
						{
							refKey = refKey+vinEntFileDetails.getVinStartRange().trim();
							name=name+vinEntFileDetails.getVinStartRange().trim();
							// PROCESS VIS END RANGE
							if(null!=vinEntFileDetails.getVinEndRange() && !"".equals(vinEntFileDetails.getVinEndRange()))
							{
								refKey = refKey+vinEntFileDetails.getVinEndRange().trim();
								name=name+vinEntFileDetails.getVinEndRange().trim();
							}
						}
					}
					
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(name);
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType(vinType);
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
						
						vinMapped ="Y";
					}
					vinEntFileDetails.setVinRefKey(refKey);
					refKey= null;
					name=  null;
				}
				
				
				/*
				 * add unique type data
				 */
				if(null!=vinEntFileDetails.getVinMasterEngineMissionType() && !"".equals(vinEntFileDetails.getVinMasterEngineMissionType()))
				{
					boolean addToType=true;
					if(null!=typeList && typeList.size()>0)
					{
						for(int r=0;r<typeList.size();r++)
						{
							if(vinEntFileDetails.getVinMasterEngineMissionType().trim().toLowerCase().equals(String.valueOf(typeList.get(r).trim().toLowerCase())))
							{
								// already added
								addToType = false;
								break;
							}
						}
					}
					
					if(addToType==true)
					{
						typeList.add(vinEntFileDetails.getVinMasterEngineMissionType());
					}
				}
			}
			
			
			if(null!=typeList && typeList.size()>0)
			{
				for(int t=0;t<typeList.size();t++)
				{
					String typeCode = String.valueOf(typeList.get(t));
					/*
					 * ADD ENGINE TYPE / TRANSMISSION AS IT IS TO THE DOCUMENT
					 */
					String refKey = typeCode.trim();
					refKey = Utilities.replaceCharsForRefKeys(refKey);
					// add to CATEGORY DETAILS
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(typeCode.toUpperCase());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("ENGINE_MISSION_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
					} 
					refKey =null;
					typeCode = null;
				}
			}
			typeList = null;
		
			if(null!=vinMapped && !"".equals(vinMapped))
			{
				contentDetails.setVinMapped(vinMapped);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processVINCategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will add VIN Attributes as Category with the Document
	 * @param vinAtrList
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails processVINAttributeCategories(ArrayList<VinAttributeEntFileDetails> vinAtrList, ContentDetails contentDetails)
	{
		try
		{
			for(int i=0;i<vinAtrList.size();i++)
			{
				VinAttributeEntFileDetails vinAttributeFileDetails = (VinAttributeEntFileDetails)vinAtrList.get(i);
				/*
				 * iterate and MAP EACH OF THE ITEM AS CATEGORY
				 * ENGINE TYPE
				 * TRANSMISSION TYPE
				 * BODY TYPE
				 * DRIVE AXLE TYPE
				 */
				
				if(null!=vinAttributeFileDetails.getVinEngineType() && !"".equals(vinAttributeFileDetails.getVinEngineType()))
				{
					/*
					 * ADD ENGINE TYPE AS IT IS TO THE DOCUMENT
					 */
					String refKey = vinAttributeFileDetails.getVinEngineType().trim();
					refKey = Utilities.replaceCharsForRefKeys(refKey);
					// add to CATEGORY DETAILS
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(vinAttributeFileDetails.getVinEngineType());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("ENGINE_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
					} 
					refKey =null;
				}
				
				if(null!=vinAttributeFileDetails.getVinTransType() && !"".equals(vinAttributeFileDetails.getVinTransType()))
				{
					/*
					 * ADD TRANSMISSION TYPE AS IT IS TO DOCUMENT
					 */
					String refKey = vinAttributeFileDetails.getVinTransType().trim();
					refKey = Utilities.replaceCharsForRefKeys(refKey);
					// add to CATEGORY DETAILS
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(vinAttributeFileDetails.getVinTransType());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("MISSION_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
					} 
					refKey =null;
				}
				
				if(null!=vinAttributeFileDetails.getVinBodyType() && !"".equals(vinAttributeFileDetails.getVinBodyType()))
				{
					/*
					 * FOR BODY TYPES, 
					 * if BODY TYPE CODE IS 2, THEN REFKEY WILL BE - B02, 
					 * ADD PREFIX B0 TO THE BODY TYPE CODE
					 * 
					 * NO NEED TO ADD ANY PREFIX, THE CODES HAVE CHANGED, WHAT EVER VALUE IS RECEIVED IN CONTENT WILL BE PASSED
					 * AS IT IS - 07 JULY 2016
					 * 
					 * 
					 */
//					String prefix = ApplicationProperties.getProperty("prefix.body.type.refkey");
					String prefix = "";
					String refKey = prefix+vinAttributeFileDetails.getVinBodyType();
					refKey = Utilities.replaceCharsForRefKeys(refKey);
					// add to CATEGORY DETAILS
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(vinAttributeFileDetails.getVinBodyType());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("BODY_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
					} 
					refKey =null;
					prefix= null;
				}
				
				
				if(null!=vinAttributeFileDetails.getVinAxleType() && !"".equals(vinAttributeFileDetails.getVinAxleType()))
				{

					/*
					 * FOR AXLE TYPES, 
					 * if AXLE TYPE CODE IS 2, THEN REFKEY WILL BE - D02, 
					 * ADD PREFIX B0 TO THE AXLE TYPE CODE
					 * 
					 * NO NEED TO ADD ANY PREFIX, THE CODES HAVE CHANGED, WHAT EVER VALUE IS RECEIVED IN CONTENT WILL BE PASSED
					 * AS IT IS - 07 JULY 2016
					 */
//					String prefix = ApplicationProperties.getProperty("prefix.axle.type.refkey");
					String prefix = "";
					String refKey = prefix+vinAttributeFileDetails.getVinAxleType();
					refKey = Utilities.replaceCharsForRefKeys(refKey);
					// add to CATEGORY DETAILS
					if(null!=refKey && !"".equals(refKey))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(vinAttributeFileDetails.getVinAxleType());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						catDetails.setCategoryRefKey(refKey.toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("DRIVEAXLE_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails = null;
					} 
					refKey =null;
					prefix= null;
				
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "processVINAttributeCategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will ADD MANUAL TYPE CATEGORY WITH DOCUMENT FOR SM CHANNEL
	 * OTHER MANUL TYPE WITH DOCUMENT FOR OSM CHANNEL
	 * @param contentDetails
	 * @return
	 */
	public static ContentDetails addManualTypeAsCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getChannelName() && (contentDetails.getChannelName().trim().toLowerCase().
					equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME").trim().toLowerCase())))
			{
				// CALL FUNCTION TO SET MANUAL TYPE AS CATEGORIES WITH THE DOCUMENT.
				String smTypeCategoryToBeAdded="";
				if(null!=contentDetails.getModel() && 
						(contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
								contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
								contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
								contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
								contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
						contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
						|| contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
				{
					// get on ModelName
					smTypeCategoryToBeAdded = ConversionUtils.identifyManualTypeAsCateogry(contentDetails.getModel());
				}
				else
				{
					if(null!=contentDetails.getManualType() && !"".equals(contentDetails.getManualType()))
					{
						// on Manual Type
						smTypeCategoryToBeAdded = ConversionUtils.identifyManualTypeAsCateogry(contentDetails.getManualType());
					}
				}
				
				if(null!=smTypeCategoryToBeAdded && !"".equals(smTypeCategoryToBeAdded))
				{
					CategoryDetails catDetails = new CategoryDetails();
					catDetails.setCategoryName(smTypeCategoryToBeAdded);
					catDetails.setCategoryRefKey(smTypeCategoryToBeAdded);
					catDetails.setCategoryType("MANUAL_TYPE");
					if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
					{
						contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
					}
					contentDetails.getCategoryList().add(catDetails);
					catDetails= null;
				}
				smTypeCategoryToBeAdded = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addManualTypeAsCategories()", e);
		}
		return contentDetails;
	}

}