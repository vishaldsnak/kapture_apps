package com.mazda.gms3.dmt.mc.utils;

import java.util.ArrayList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;

public class CategoryUtils {

	static Logger logger  =LogManager.getLogger(CategoryUtils.class);
	
	
	public static ContentDetails performCategoriesOperation(ContentDetails contentDetails, String market)
	{
		try
		{
			contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
			
			/*
			 * MANUAL TYPE CATEGORIES
			 * OTHER SERVICE MANUAL TYPE CATEGORIES
			 * ESI CATEGORIES
			 * VIN CATEGORIES
			 * DISPLAY ORDER CATEGORIES
			 * MASTER VIN CATEGORIES
			 * ENGINE / MISSION TYPE CATEGORIES
			 * STEERING TYPES (APPLICABLE ONLY FOR WIRING DIAGRAMS)
			 */
			if(null!=market && market.equals("MNAO"))
			{
				// MNAO: same flow as MC / MME (vin.txt + d01.txt + esicat.txt), MNAO category reference keys
				return performCategoriesOperationForMNAO(contentDetails);
			}
			contentDetails = addManualTypeAsCategories(contentDetails);
			
			/*
			 * ADD ESI CATEGORIES ONLY FOR NEWM MODEL TYPES FOR OTHERS IGNORE.
			 */
			if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new")))
			{
				contentDetails = addESICategories(contentDetails);
			}
			else
			{
				contentDetails.setCategoryCode(null);
				contentDetails.setSubCategoryCode(null);
				contentDetails.setSubSubCategoryCode(null);
				contentDetails.setMappedTaxonomy(null);
				contentDetails.setEsiCategoryMapped(null);
				contentDetails.setEsiCategoryMappedFileName(null);
				contentDetails.setEsiCategoryMappedFilePath(null);
				contentDetails.setEsiCategoryMappedLevel(null);
				contentDetails.setEsiCategoryMappedRefKey(null);
			}
			
			if(null!=market && !"".equals(market) && market.equals("MC"))
			{
				contentDetails = addVINCategories(contentDetails);
			}
			else if(null!=market && !"".equals(market) && market.equals("MME"))
			{
				contentDetails = addVINCategoriesForMME(contentDetails);
			}
			
			contentDetails = addMasterVINCategories(contentDetails);
			
//			contentDetails = addDisplayOrderManualTypeAsCategories(contentDetails);
			contentDetails = addDisplayOrderCategories(contentDetails);
			
			contentDetails = addApplicableEngineMissionTypeCategories(contentDetails);
			
			contentDetails = addCVCCategories(contentDetails);
		
			if(null!=market && !"".equals(market) && market.equals("MME"))
			{
				/*
				 * IF PROCESSING CHANNEL IS WIRING DIAGRAMS -  
				 */
				if(null!=contentDetails && null!=contentDetails.getChannelName() && 
						contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					if(null!=contentDetails.getEsiSteeringTypeInfo() && !"".equals(contentDetails.getEsiSteeringTypeInfo()))
					{
						// add Steering Type Categories
						contentDetails = addSteeringTypeCategories(contentDetails);
					}
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "performCategoriesOperation()", e);
		}
		return contentDetails;
	}
	
	/**
	 * MNAO CATEGORIES. The content now has the MC / MME layout - vin.txt, d01.txt, esicat.txt; no vin.ent /
	 * vin_attribute.ent any more - but the MNAO categories in Kapture keep their own reference keys, so the MNAO rules
	 * (com.mazda.gms3.dmt.conversion.impl.CategoryUtils) still build them:
	 *   manual type, MODEL_YEAR (MODEL_YEAR / "MODEL::YEAR" from the vin.txt model and year), ESI, steering
	 *   (rhdlhdindicator META), CVC, VIN (WMI+VDS+range), master VIN, engine / mission list.
	 * The vin.txt lines of the document are handed to them in the VIN ENT shape they read (also what the
	 * gms3_dmt_<locale>_vin table stores). Engine / mission / drive axle / body type come from the d01.txt rows,
	 * exactly as for MC / MME.
	 */
	private static ContentDetails performCategoriesOperationForMNAO(ContentDetails contentDetails)
	{
		ArrayList<VINEntFileDetails> vinEntList = new ArrayList<VINEntFileDetails>();
		if(null!=contentDetails.getApplicableVINList())
		{
			for(Object o : contentDetails.getApplicableVINList())
			{
				com.mazda.gms3.dmt.mc.vo.VinDetails vin = (com.mazda.gms3.dmt.mc.vo.VinDetails)o;
				VINEntFileDetails v = new VINEntFileDetails();
				v.setFileName(vin.getVinSourceFileName());
				v.setVinCarline(vin.getCarlineCode());
				v.setVinWMI(vin.getWmiCode());
				v.setVinVDS(vin.getVdsCode());
				v.setVinStartRange(vin.getVisStartRange());
				v.setVinEndRange(vin.getVisEndRange());
				v.setModelYear(vin.getModelYear());
				v.setModelNameVINEnt(vin.getModelName());
				vinEntList.add(v);
			}
		}
		contentDetails.setVinEntDetailsList(vinEntList);
		contentDetails = com.mazda.gms3.dmt.conversion.impl.CategoryUtils.performCategoriesOperation(contentDetails);
		contentDetails = addDisplayOrderCategories(contentDetails);
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
					equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME").trim().toLowerCase()) ||  
					contentDetails.getChannelName().trim().toLowerCase().
					equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME").trim().toLowerCase())))
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
			
			/*
			 * APPLICABLE FOR ALL CHANNELS
			 */
//			if(null!=contentDetails.getChannelName() && contentDetails.getChannelName().trim().toLowerCase().
//					equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME").trim().toLowerCase()))
//			{
				if(null!=contentDetails.getModelType() && !"".equals(contentDetails.getModelType()))
				{
					// CALL FUNCTION TO SET OTHER MANUAL TYPE AS CATEGORY FOR THE DOCUMENT 
					// ON THE BASIS OF MODEL TYPE
					String osmTypeCategoryToBeAdded = ConversionUtils.identifyOtherManualTypeAsCateogry(contentDetails.getModelType());
					if(null!=osmTypeCategoryToBeAdded && !"".equals(osmTypeCategoryToBeAdded))
					{
						CategoryDetails catDetails = new CategoryDetails();
						catDetails.setCategoryName(osmTypeCategoryToBeAdded);
						catDetails.setCategoryRefKey(osmTypeCategoryToBeAdded);
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						catDetails.setCategoryType("OTHER_MANUAL_TYPE");
						contentDetails.getCategoryList().add(catDetails);
						catDetails= null;
					}
					osmTypeCategoryToBeAdded = null;
				}
				
				
				/*
				 * ONLY FOR TOYOTA CONTENT  - 02 JULY 2018
				 */
				if(contentDetails.isToyotaContent()==true)
				{
					// MAP TOYATO REF KEY
					CategoryDetails catDetails = new CategoryDetails();
					catDetails.setCategoryName(ApplicationProperties.getProperty("GMS3_OSM_TYPE_TOYOTA_REF_KEY"));
					catDetails.setCategoryRefKey(ApplicationProperties.getProperty("GMS3_OSM_TYPE_TOYOTA_REF_KEY"));
					if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
					{
						contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
					}
					catDetails.setCategoryType("OTHER_MANUAL_TYPE");
					contentDetails.getCategoryList().add(catDetails);
					catDetails= null;
				}
//			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addManualTypeAsCategories()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will ADD ESI CATEGORIES WITH DOCUMENT FOR ALL CHANNELS
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addESICategories(ContentDetails contentDetails)
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
			else
			{
				// ESI codes come ONLY from fields 6-8 of the document's esicat.txt line
				logger.info("addESICategories :: NO ESI CATEGORY CODES for the Document (its line in "
						+ contentDetails.getEsiCategoryMappedFileName() + " has no category level 1/2/3 codes) - no ESI category tagged ::  > "
						+ contentDetails.getFilePath());
			}
			taxonomy = null;
			
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
					logger.info("addESICategories :: Adding SUB SUB Category to the Document ::  > " + contentDetails.getFilePath());
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
					cDetails= null;
					
					// here Mapped Level is 3rd Level
					contentDetails.setEsiCategoryMapped("Y");
					contentDetails.setEsiCategoryMappedLevel("LEVEL3");
					contentDetails.setEsiCategoryMappedRefKey(refKey);
					
					refKey = null;
				}
				else
				{
					logger.info("addESICategories :: SUB SUB Category is {00}. Adding SUB CATEGORY to the Document ::  > " + contentDetails.getFilePath());
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
					cDetails= null;
					// here Mapped Level is 2nd Level
					contentDetails.setEsiCategoryMapped("Y");
					contentDetails.setEsiCategoryMappedLevel("LEVEL2");
					contentDetails.setEsiCategoryMappedRefKey(refKey);
					refKey = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addESICategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * FUNCTION WILL ADD VIN CATEGORIE WITH DOCUMENT FOR ALL CHANNELS
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addVINCategories(ContentDetails contentDetails)
	{
		try
		{
//			MCDocumentManagementDAO mcDAO = new MCDocumentManagementDAO();
			if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
			{
				String vinMapped=null;
				ArrayList<String> vinFilesNames=new ArrayList<String>();
				
				/*
				 * GET WMI CODES FOR ALL THE VIN ON THE BASIS OF CARLINE CODE & LOCALE & VDS CODES
				 * & VIS RANGES ( DATE 9TH AUGUST 2019)
				 * REMOVE THIS PART AS WMI CODE FOR MC WILL ALWAYS BE - DATE 09 AUGUST 2019
				 */
//				String locale = contentDetails.getLocale().replace("_", "-").trim();
//				contentDetails.setApplicableVINList(mcDAO.getWMICodeOnCarlineAndLocale(contentDetails.getApplicableVINList(), locale));
				
				VinDetails details = new VinDetails();
				String wmiCode="";
				String refKey="";
				CategoryDetails cDetails = new CategoryDetails();
				
				for(int a=0;a<contentDetails.getApplicableVINList().size();a++)
				{
					details = (VinDetails)contentDetails.getApplicableVINList().get(a);
					/*
					 * SET WMI CODE EXPLICITLY AS - FOR MC MARKET
					 * date 9th august 2019
					 */
					details.setWmiCode("-");
					/*
					 * GET WMI ON THE BASIS OF CARLINE CODE & LOCALE
					 * THEN PREPARE THE REF KEY FOR THE LAST LEVEL AND ADD TO DOCUMENT
					 */
					if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) && null!=contentDetails.getLocale() 
							&& !"".equals(contentDetails.getLocale()))
					{
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							wmiCode = details.getWmiCode();
							// remove Extra spaces
							wmiCode = wmiCode.trim();
							if(wmiCode.equals("-"))
							{
								// replace it by 3 underscores
								wmiCode="___";
							}
							if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
									&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
									&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
							{
								// PREPARE REF KEY
								refKey=details.getCarlineCode()+wmiCode+details.getVdsCode()+details.getVisStartRange()+details.getVisEndRange();
								refKey = Utilities.replaceCharsForRefKeys(refKey);
								
								// ADD VIN AS CATEGORY WITH DOCUMENT
								cDetails = new CategoryDetails();
								cDetails.setCategoryName(refKey);
								refKey = Utilities.replaceCharsForRefKeys(refKey.trim().toUpperCase());
								// THESE ARE ESI CATEGORIES -
								cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
								if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
								{
									contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
								}
								cDetails.setCategoryType("VIN");
								// add cDetails to categoryList
								contentDetails.getCategoryList().add(cDetails);
								cDetails= null;
								
								// UPDATE THE REF KEY IN APPLICABLE VIN LIST
								details.setVinRefKey(refKey);
								
								refKey= null;
								// set vinMapped TO Y
								vinMapped="Y";
								
								// add VIN FILE NAME TO ARRAY, BEFORE ADDING CHECK IF ALREADY EXISTS OR NOT
								if(null!=details.getVinSourceFileName() && !"".equals(details.getVinSourceFileName()))
								{
									boolean add=true;
									if(null!=vinFilesNames && vinFilesNames.size()>0)
									{
										for(int q=0;q<vinFilesNames.size();q++)
										{
											if(String.valueOf(vinFilesNames.get(q).trim().toLowerCase()).equals(details.getVinSourceFileName().trim().toLowerCase()))
											{
												// already added
												add = false;
												break;
											}
										}
									}
									
									if(add==true)
									{
										vinFilesNames.add(details.getVinSourceFileName());
									}
								}
							}
						}
						else
						{
							logger.info("addVINCategories :: Failed to Identify WMI CODE for CARLINE {"+contentDetails.getCarlineCode()+"} AND LOCALE :: > " + contentDetails.getLocale());
						}
						wmiCode = null;
					}
				}
				cDetails = null;
				refKey=null;
				wmiCode =null;
				details= null;
//				locale=null;
				if(null!=vinMapped && !"".equals(vinMapped))
				{
					contentDetails.setVinMapped(vinMapped);
				}
				if(null!=vinFilesNames && vinFilesNames.size()>0)
				{
					String name="";
					for(int q=0;q<vinFilesNames.size();q++)
					{
						name = name+String.valueOf(vinFilesNames.get(q));
						if(q!=vinFilesNames.size()-1)
						{
							name = name+",";
						}
					}
					if(null!=name && !"".equals(name))
					{
						contentDetails.setVinMappedFileName(name);
					}
					name=null;
				}
				
				vinFilesNames= null;
				vinMapped = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addVINCategories()", e);
		}
		return contentDetails;
	}

	/**
	 * FUNCTION WILL ADD VIN CATEGORIES WITH DOCUMENTS FOR ALL CHANNELS FOR MME MARKET
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addVINCategoriesForMME(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
			{
				String vinMapped=null;
				ArrayList<String> vinFilesNames=new ArrayList<String>();
				
				VinDetails details = new VinDetails();
				String wmiCode="";
				String refKey="";
				CategoryDetails cDetails = new CategoryDetails();
				
				for(int a=0;a<contentDetails.getApplicableVINList().size();a++)
				{
					details = (VinDetails)contentDetails.getApplicableVINList().get(a);
					/*
					 * GET WMI ON THE BASIS OF CARLINE CODE & LOCALE - 
					 * NO NEED OF FETCHING WMI CODE AS IT IS ALREADY AVAILABLE WITH VIN IN VIN.TXT
					 * THEN PREPARE THE REF KEY FOR THE LAST LEVEL AND ADD TO DOCUMENT
					 */
					if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) && 
							null!=details.getWmiCode() && !"".equals(details.getWmiCode()) && 
							null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
							&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
							&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
					{
						wmiCode = details.getWmiCode();
						// remove Extra spaces
						wmiCode = wmiCode.trim();
						if(wmiCode.equals("-"))
						{
							// replace it by 3 underscores
							wmiCode="___";
						}
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
								&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
								&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
						{
							// PREPARE REF KEY
							refKey=details.getCarlineCode()+wmiCode+details.getVdsCode()+details.getVisStartRange()+details.getVisEndRange();
							refKey = Utilities.replaceCharsForRefKeys(refKey);

							// ADD VIN AS CATEGORY WITH DOCUMENT
							cDetails = new CategoryDetails();
							cDetails.setCategoryName(refKey);
							refKey = Utilities.replaceCharsForRefKeys(refKey.trim().toUpperCase());
							// THESE ARE ESI CATEGORIES -
							cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("VIN");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							cDetails= null;

							// UPDATE THE REF KEY IN APPLICABLE VIN LIST
							details.setVinRefKey(refKey);

							refKey= null;
							// set vinMapped TO Y
							vinMapped="Y";

							// add VIN FILE NAME TO ARRAY, BEFORE ADDING CHECK IF ALREADY EXISTS OR NOT
							if(null!=details.getVinSourceFileName() && !"".equals(details.getVinSourceFileName()))
							{
								boolean add=true;
								if(null!=vinFilesNames && vinFilesNames.size()>0)
								{
									for(int q=0;q<vinFilesNames.size();q++)
									{
										if(String.valueOf(vinFilesNames.get(q).trim().toLowerCase()).equals(details.getVinSourceFileName().trim().toLowerCase()))
										{
											// already added
											add = false;
											break;
										}
									}
								}

								if(add==true)
								{
									vinFilesNames.add(details.getVinSourceFileName());
								}
							}
						}
					}
					else
					{
						logger.info("addVINCategoriesForMME :: INVALID VIN ENTRY FOUND. ALL THE REQUIRED PARAMS ARE NULL TO PREPARE LAST LEVEL REFKEY");
					}
					wmiCode = null;
				}
				cDetails = null;
				refKey=null;
				wmiCode =null;
				details= null;
				if(null!=vinMapped && !"".equals(vinMapped))
				{
					contentDetails.setVinMapped(vinMapped);
				}
				if(null!=vinFilesNames && vinFilesNames.size()>0)
				{
					String name="";
					for(int q=0;q<vinFilesNames.size();q++)
					{
						name = name+String.valueOf(vinFilesNames.get(q));
						if(q!=vinFilesNames.size()-1)
						{
							name = name+",";
						}
					}
					if(null!=name && !"".equals(name))
					{
						contentDetails.setVinMappedFileName(name);
					}
					name=null;
				}
				vinFilesNames= null;
				vinMapped = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addVINCategoriesForMME()", e);
		}
		return contentDetails;
	}
	
	
	/**
	 * FUNCTION WILL ADD 
	 * ENGINE TYPE
	 * MISSION TYPE
	 * BODY TYPE
	 * DRIVE AXLE TYPE
	 * 
	 * NO ADDITIONAL MANUAL TYPES IF MULTIPLE FOUND IN DISPLAY ORDER
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addDisplayOrderCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
			{
				for(int a=0;a<contentDetails.getApplicableDisplayOrderList().size();a++)
				{
					DisplayOrderDetails details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(a);
					if(null!=details.getEngineType() && !"".equals(details.getEngineType()))
					{
						logger.info("addDisplayOrderCategories :: Addding Engine Type Categories :: > " + details.getEngineType());
						String[] tokens = details.getEngineType().trim().split(",");
						if(null!=tokens && tokens.length>0)
						{
							for(int b=0;b<tokens.length;b++)
							{
								String key = tokens[b];
								if(null!=key && !"".equals(key.trim()) && !"e99".equals(key.trim().toLowerCase()))
								{
									// ADD TOK AS CATEGORY TO THE DOCUMENT 
									CategoryDetails cDetails = new CategoryDetails();
									cDetails.setCategoryName(key);
									key = Utilities.replaceCharsForRefKeys(key.trim().toUpperCase());
									// THESE ARE ENGINE TYPE CATEGORIES -
									cDetails.setCategoryRefKey(key.trim().toUpperCase());
									if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
									{
										contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
									}
									cDetails.setCategoryType("ENGINE_TYPE_CATEGORY");
									// add cDetails to categoryList
									contentDetails.getCategoryList().add(cDetails);
									cDetails= null;
								}
								key = null;
							}
						}
						tokens = null;
					}
					
					if(null!=details.getMissionType() && !"".equals(details.getMissionType()))
					{
						logger.info("addDisplayOrderCategories :: Addding Mission Type Categories :: > " + details.getMissionType());
						String[] tokens = details.getMissionType().trim().split(",");
						if(null!=tokens && tokens.length>0)
						{
							for(int b=0;b<tokens.length;b++)
							{
								String key = tokens[b];
								if(null!=key && !"".equals(key.trim()) && !"m99".equals(key.trim().toLowerCase()))
								{
									// ADD TOK AS CATEGORY TO THE DOCUMENT 
									CategoryDetails cDetails = new CategoryDetails();
									cDetails.setCategoryName(key);
									key = Utilities.replaceCharsForRefKeys(key.trim().toUpperCase());
									// THESE ARE MISSION TYPE CATEGORIES -
									cDetails.setCategoryRefKey(key.trim().toUpperCase());
									if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
									{
										contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
									}
									cDetails.setCategoryType("MISSION_TYPE_CATEGORY");
									// add cDetails to categoryList
									contentDetails.getCategoryList().add(cDetails);
									cDetails= null;
								}
								key = null;
							}
						}
						tokens = null;
					}
					
					if(null!=details.getDriveAxleType() && !"".equals(details.getDriveAxleType()))
					{
						logger.info("addDisplayOrderCategories :: Addding DriveAxle Type Categories :: > " + details.getDriveAxleType());
						String[] tokens = details.getDriveAxleType().trim().split(",");
						if(null!=tokens && tokens.length>0)
						{
							for(int b=0;b<tokens.length;b++)
							{
								String key = tokens[b];
								if(null!=key && !"".equals(key.trim()))
								{
									// ADD TOK AS CATEGORY TO THE DOCUMENT 
									CategoryDetails cDetails = new CategoryDetails();
									cDetails.setCategoryName(key);
									key = Utilities.replaceCharsForRefKeys(key.trim().toUpperCase());
									// THESE ARE DRIVE AXLE TYPE CATEGORIES -
									cDetails.setCategoryRefKey(key.trim().toUpperCase());
									if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
									{
										contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
									}
									cDetails.setCategoryType("DRIVEAXLE_TYPE_CATEGORY");
									// add cDetails to categoryList
									contentDetails.getCategoryList().add(cDetails);
									cDetails= null;
								}
								key = null;
							}
						}
						tokens = null;
					}
					
					if(null!=details.getBodyType() && !"".equals(details.getBodyType()))
					{
						logger.info("addDisplayOrderCategories :: Addding Body Type Categories :: > " + details.getBodyType());
						String[] tokens = details.getBodyType().trim().split(",");
						if(null!=tokens && tokens.length>0)
						{
							for(int b=0;b<tokens.length;b++)
							{
								String key = tokens[b];
								if(null!=key && !"".equals(key.trim()))
								{
									// ADD TOK AS CATEGORY TO THE DOCUMENT 
									CategoryDetails cDetails = new CategoryDetails();
									cDetails.setCategoryName(key);
									key = Utilities.replaceCharsForRefKeys(key.trim().toUpperCase());
									// THESE ARE BODT TYPE CATEGORIES -
									cDetails.setCategoryRefKey(key.trim().toUpperCase());
									if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
									{
										contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
									}
									cDetails.setCategoryType("BODY_TYPE_CATEGORY");
									// add cDetails to categoryList
									contentDetails.getCategoryList().add(cDetails);
									cDetails= null;
								}
								key = null;
							}
						}
						tokens = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addDisplayOrderManualTypeAsCategories()", e);
		}
		return contentDetails;
	}

	
	public static ContentDetails addDisplayOrderManualTypeAsCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getChannelName() && contentDetails.getChannelName().trim().toLowerCase().
					equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME").trim().toLowerCase()))
			{
				if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
				{
					for(int a=0;a<contentDetails.getApplicableDisplayOrderList().size();a++)
					{
						DisplayOrderDetails details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(a);
						if(null!=details.getManualType() && !"".equals(details.getManualType()))
						{
							if(!details.getManualType().trim().toLowerCase().equals(contentDetails.getManualType().trim().toLowerCase()))
							{
								if(null!=details.getModelFolderName() && !"".equals(details.getModelFolderName()))
								{
									String manualTypeKey="";
									String afterFirstIndex="";
									if(details.getModelFolderName().indexOf("_")!=-1)
									{
										afterFirstIndex = details.getModelFolderName().substring(details.getModelFolderName().indexOf("_")+1, details.getModelFolderName().length());
									}
									if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
									{
										if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
											afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
											afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
											afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
											afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
											afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
											|| afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
										{
											manualTypeKey = afterFirstIndex;
										}
										else
										{
											manualTypeKey = details.getManualType();
										}
									}
									
									
									// CALL FUNCTION TO SET MANUAL TYPE AS CATEGORIES WITH THE DOCUMENT.
									String smTypeCategoryToBeAdded="";
									if(null!=manualTypeKey && !"".equals(manualTypeKey))
									{
										smTypeCategoryToBeAdded = ConversionUtils.identifyManualTypeAsCateogry(manualTypeKey);
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
									manualTypeKey=null;
									afterFirstIndex = null;
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addDisplayOrderManualTypeAsCategories()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will add MASTER VIN as Categories with a Document
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addMasterVINCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
			{
				String vinMapped=null;
				for(int a=0;a<contentDetails.getMasterVinList().size();a++)
				{
					VINEntFileDetails details = (VINEntFileDetails)contentDetails.getMasterVinList().get(a);
					/*
					 * PREPARE THE REF KEY FOR THE LAST LEVEL AND ADD TO DOCUMENT
					 */
					if(null!=details.getVinCarline() && !"".equals(details.getVinCarline()) 
						&& null!=details.getVinWMI() && !"".equals(details.getVinWMI())
						&& null!=details.getVinVDS() && !"".equals(details.getVinVDS())
						&& null!=details.getVinStartRange() && !"".equals(details.getVinStartRange())
						&& null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()))
					{
						String wmiCode= details.getVinWMI().trim();
						if(null!=wmiCode && !"".equals(wmiCode))
						{
							// remove Extra spaces
							wmiCode = wmiCode.trim();
							if(wmiCode.equals("-"))
							{
								// replace it by 3 underscores
								wmiCode="___";
							}

							// PREPARE REF KEY
							String refKey=details.getVinCarline()+wmiCode+details.getVinVDS()+details.getVinStartRange()+details.getVinEndRange();
							refKey = Utilities.replaceCharsForRefKeys(refKey);

							// ADD VIN AS CATEGORY WITH DOCUMENT
							CategoryDetails cDetails = new CategoryDetails();
							cDetails.setCategoryName(refKey);
							refKey = Utilities.replaceCharsForRefKeys(refKey.trim().toUpperCase());
							// THESE ARE ESI CATEGORIES -
							cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							cDetails.setCategoryType("MASTER_VIN");
							// add cDetails to categoryList
							contentDetails.getCategoryList().add(cDetails);
							cDetails= null;
							
							// UPDATE THE REF KEY IN MASTER VIN LIST
							details.setVinRefKey(refKey);
							
							refKey= null;
							// set vinMapped TO Y
							vinMapped="Y";
						}
						wmiCode = null;
					}
				}
				
				if(null!=vinMapped && !"".equals(vinMapped))
				{
					contentDetails.setVinMapped(vinMapped);
					contentDetails.setVinMappedFileName("MASTER_VIN_FROM_MDM");
				}
				vinMapped = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addMASTERVINCategories()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will add All the Applicable Engine / Mission Type Categories with a Document
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addApplicableEngineMissionTypeCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getApplicableEngineMissionTypeList() && 
					contentDetails.getApplicableEngineMissionTypeList().size()>0)
			{
				for(int a=0;a<contentDetails.getApplicableEngineMissionTypeList().size();a++)
				{
					String key = (String)contentDetails.getApplicableEngineMissionTypeList().get(a);
					/*
					 * PREPARE THE REF KEY AND ADD TO DOCUMENT
					 */
					if(null!=key && !"".equals(key))
					{
						// PREPARE REF KEY
						String refKey=key.trim();
						refKey = Utilities.replaceCharsForRefKeys(refKey);

						// ADD VIN AS CATEGORY WITH DOCUMENT
						CategoryDetails cDetails = new CategoryDetails();
						cDetails.setCategoryName(refKey);
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim().toUpperCase());
						// THESE ARE ESI CATEGORIES -
						cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						if(null!=contentDetails.getModel() && 
								(contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
										contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
										contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
										contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
										contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
										|| contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
						{
							cDetails.setCategoryType("ENGINE_TYPE_CATEGORY");
						}
						else if(null!=contentDetails.getModel() && 
								(contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) || 
								contentDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
						{
							cDetails.setCategoryType("MISSION_TYPE_CATEGORY");
						}
						
						// add cDetails to categoryList
						contentDetails.getCategoryList().add(cDetails);
						cDetails= null;
						refKey= null;
					}
					key = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addApplicableEngineMissionTypeCategories()", e);
		}
		return contentDetails;
	}
	
	
	private static ContentDetails addCVCCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
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
//						if(null!=ccDetails.getSubCategoryCode() && !"".equals(ccDetails.getSubCategoryCode()))
//						{
//							// add SUB CAT CODE TO REF KEY
//							name = ccDetails.getSubCategoryCode().trim();
//							refKey = refKey+ccDetails.getSubCategoryCode().trim();
//							if(null!=ccDetails.getSymptomCode() && !"".equals(ccDetails.getSymptomCode()))
//							{
//								// add SYMP CODE TO REF KEY
//								name = ccDetails.getSymptomCode().trim();
//								refKey = refKey+ccDetails.getSymptomCode().trim();
//								if(null!=ccDetails.getSubSymptomCode() && !"".equals(ccDetails.getSubSymptomCode()))
//								{
//									// add SUB SYMP CODE TO REF KEY
//									name = ccDetails.getSubSymptomCode().trim();
//									refKey=refKey+ccDetails.getSubSymptomCode().trim();
//									if(null!=ccDetails.getConditionCode() && !"".equals(ccDetails.getConditionCode()))
//									{
//										// add COND CODE TO REF KEY
//										name = ccDetails.getConditionCode().trim();
//										refKey = refKey+ccDetails.getConditionCode().trim();
//									}
//								}
//							}
//						}
						
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
						// ADD REF KEY TO ccDetails
						ccDetails.setCvcRefKey(refKey.trim().toUpperCase());
						
						// ADD CVC Categories
						CategoryDetails cDetails = new CategoryDetails();
						cDetails.setCategoryName(name.trim());
						refKey = Utilities.replaceCharsForRefKeys(refKey.trim());
						cDetails.setCategoryRefKey(refKey.trim().toUpperCase());
						// SET CATEGORY TYPE AS CVC_CATEGORY
						cDetails.setCategoryType("CVC_CATEGORY");
						if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
						{
							contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
						}
						contentDetails.getCategoryList().add(cDetails);
						cDetails= null;
					}
					refKey = null;
					name = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addCVCCategories", e);
		}
		return contentDetails;
	}
	
	/**
	 * FUNCTION WILL ADD STEERING TYPE AS CATEGORIES WITH THE DOCUMENTS
	 * APPLICABLE ONLY FOR MME MARKET SPECIFIC TO WIRING DIAGRAMS CHANNEL
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails addSteeringTypeCategories(ContentDetails contentDetails)
	{
		try
		{
			if(null!=contentDetails.getEsiSteeringTypeInfo() && !"".equals(contentDetails.getEsiSteeringTypeInfo()))
			{
				String[] tokens = contentDetails.getEsiSteeringTypeInfo().split(",");
				if(null!=tokens && tokens.length>0)
				{
					String steeringTypeRefKey=null;
					String steeringType=null;
					CategoryDetails catDetails = new CategoryDetails();
					for(int a=0;a<tokens.length;a++)
					{
						steeringType = tokens[a].trim();
						if(null!=steeringType && steeringType.trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.lhd")))
						{
							steeringTypeRefKey=ApplicationProperties.getProperty("steering.type.refkey.lhd");
						}
						else if(null!=steeringType && steeringType.trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.rhd")))
						{
							steeringTypeRefKey=ApplicationProperties.getProperty("steering.type.refkey.rhd");
						}
						
						if(null!=steeringTypeRefKey && !"".equals(steeringTypeRefKey))
						{
							catDetails = new CategoryDetails();
							catDetails.setCategoryName(steeringType);
							catDetails.setCategoryRefKey(steeringTypeRefKey);
							catDetails.setCategoryType("STEERING_TYPE");
							if(null==contentDetails.getCategoryList() || contentDetails.getCategoryList().size()<=0)
							{
								contentDetails.setCategoryList(new ArrayList<CategoryDetails>());
							}
							contentDetails.getCategoryList().add(catDetails);
							catDetails= null;
						}
						steeringType = null;
						steeringTypeRefKey = null;
					}
					steeringType=null;
					steeringTypeRefKey=null;
					catDetails = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryUtils.class.getName(), "addSteeringTypeCategories()", e);
		}
		return contentDetails;
	}
}