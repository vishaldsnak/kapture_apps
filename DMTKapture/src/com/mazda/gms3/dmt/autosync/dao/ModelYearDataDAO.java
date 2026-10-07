package com.mazda.gms3.dmt.autosync.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;

public class ModelYearDataDAO {
	
	public AutoSyncScheduleItemDetails getModelYearItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_MODEL_YEAR);
			
			String sql="";
			
			int count=0;
			int startYear=0;
			int endYear=0;
			String nameForRefKey="";
					
			
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale="";
			String alternateLocale="";
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
				{
					// LOCALE BELONGS TO MNAO MARKET & FOR MODEL_YEAR
					processingLocale = localeMapping.getLocale();
					alternateLocale = localeMapping.getAlternateLocale();
					
					/*
					 * IF BOTH PROCESSING LOCALE & ALTERNATE LOCALE ARE SAME, THEN USE PROCESSING LOCALE FOR FETCHING DATA
					 * ELSE 
					 * 	FIRST CHECK FOR PROCESSING LOCALE - 
					 * 		IF NO DATA FOUND
					 * 			THEN FETCH FOR ALTERNATE LOCALE AND UPDATE FOR PROCESING LOCALE
					 * 		IF DATA FOUND
					 * 			THEN UPDATE ONLY FOR PROCESSING LOCALE (NO FETCH FOR ALTERNATE LOCALE HERE)
					 */
					boolean checkForAlternateLocale=true;
					
					if(processingLocale.equals(alternateLocale))
					{
						checkForAlternateLocale=  false;
					}

					// CHECK FOR PROCESSING LOCALE- 
					sql="SELECT MDM_CRLN_CODE_ID,MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG,MDM_CRLN_YEAR_START,MDM_CRLN_YEAR_END "
							+ " FROM gms3_mdm_carline_codes WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						// CHECK FOR ALTERNATE LOCALE = FALSE;
						checkForAlternateLocale= false;
						nameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						startYear = rs.getInt("MDM_CRLN_YEAR_START");
						endYear= rs.getInt("MDM_CRLN_YEAR_END");

						if(null!=nameForRefKey && !"".equals(nameForRefKey))
						{
							if(startYear>0 && endYear>0 && startYear<=endYear)
							{
								/* 
								 * ADD LEVEL 1 & LEVEL 2 FOR ALL RANGES
								 */
								for(int b=startYear;b<=endYear;b++)
								{
									// increment count by 1
									count=count+1;
								}
							}
							else
							{
								// LEVEL 1 ONLY
								// increment count by 1
								count=count+1;
							}
						}
						startYear=  0;
						endYear=  0;
						nameForRefKey = null;
					}
					rs.close();rs = null;
					stmt.close();stmt=null;
					sql = null;	
					startYear=  0;
					endYear=  0;
					nameForRefKey = null;
					
					// if check for ALternate Locale is True
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT MDM_CRLN_CODE_ID,MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG,MDM_CRLN_YEAR_START,MDM_CRLN_YEAR_END "
								+ " FROM gms3_mdm_carline_codes WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						while(rs.next())
						{
							nameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
							startYear = rs.getInt("MDM_CRLN_YEAR_START");
							endYear= rs.getInt("MDM_CRLN_YEAR_END");

							if(null!=nameForRefKey && !"".equals(nameForRefKey))
							{
								if(startYear>0 && endYear>0 && startYear<=endYear)
								{
									/* 
									 * ADD LEVEL 1 & LEVEL 2 FOR ALL RANGES
									 */
									for(int b=startYear;b<=endYear;b++)
									{
										// increment count by 1
										count=count+1;
									}
								}
								else
								{
									// LEVEL 1 ONLY
									// increment count by 1
									count=count+1;
								}
							}
							startYear=  0;
							endYear=  0;
							nameForRefKey = null;
						}
						rs.close();rs=null;
						// clost stmt object
						stmt.close();stmt=null;
						sql = null;
						startYear=  0;
						endYear=  0;
						nameForRefKey = null;
					}
				
					
					sql = null;
					processingLocale = null;
					alternateLocale= null;
				}
				localeMapping = null;
			}
			sql = null;
			processingLocale=  null;
			alternateLocale=  null;
			localeMapping = null;
			
			// set count
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ModelYearDataDAO.class.getName(), "getModelYearItemsCount()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				market = null;
				catType = null;
				localesList=  null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ModelYearDataDAO.class.getName(), "getModelYearItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getModelYearItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String name="";
			String nameForRefKey="";
			int startYear=0;
			int endYear=0;
			
			String processingLocale="";
			String alternateLocale="";
			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
			{
				// LOCALE BELONGS TO MNAO MARKET & FOR MODEL_YEAR
				processingLocale = localeMapping.getLocale();
				alternateLocale = localeMapping.getAlternateLocale();
				
				/*
				 * IF BOTH PROCESSING LOCALE & ALTERNATE LOCALE ARE SAME, THEN USE PROCESSING LOCALE FOR FETCHING DATA
				 * ELSE 
				 * 	FIRST CHECK FOR PROCESSING LOCALE - 
				 * 		IF NO DATA FOUND
				 * 			THEN FETCH FOR ALTERNATE LOCALE AND UPDATE FOR PROCESING LOCALE
				 * 		IF DATA FOUND
				 * 			THEN UPDATE ONLY FOR PROCESSING LOCALE (NO FETCH FOR ALTERNATE LOCALE HERE)
				 */
				boolean checkForAlternateLocale=true;
				
				if(processingLocale.equals(alternateLocale))
				{
					checkForAlternateLocale=  false;
				}

				// CHECK FOR PROCESSING LOCALE- 
				sql="SELECT MDM_CRLN_CODE_ID,MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG,MDM_CRLN_YEAR_START,MDM_CRLN_YEAR_END "
						+ " FROM gms3_mdm_carline_codes WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// CHECK FOR ALTERNATE LOCALE = FALSE;
					checkForAlternateLocale= false;
					if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
					{
						name=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
					}
					else
					{
						name=rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					}
					nameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					startYear = rs.getInt("MDM_CRLN_YEAR_START");
					endYear= rs.getInt("MDM_CRLN_YEAR_END");

					if(null!=nameForRefKey && !"".equals(nameForRefKey))
					{
						if(startYear>0 && endYear>0 && startYear<=endYear)
						{
							/* 
							 * ADD LEVEL 1 & LEVEL 2 FOR ALL RANGES
							 */
							for(int b=startYear;b<=endYear;b++)
							{
								// add LEVEL 1
								cat = new AutoSyncCategoryDetails();
								cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
								
								levelDetails = new AutoSyncCategoryDetails();
								// here locale will always be processingLocale
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(name);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
								levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_MODEL_YEAR"));
								cat.setLevel1Details(new AutoSyncCategoryDetails());
								cat.setLevel1Details(levelDetails);
								levelDetails=  null;
								
								
								// add LEVEL 2
								levelDetails = new AutoSyncCategoryDetails();
								// here locale will always be processingLocale
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(name+" "+String.valueOf(b));
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")+"_"+String.valueOf(b)));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails=  null;
								
								
								categoriesList.add(cat);
								cat = null;
							}
						}
						else
						{
							// add LEVEL 1
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
							
							levelDetails = new AutoSyncCategoryDetails();
							// here locale will always be processingLocale
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(name);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_MODEL_YEAR"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=  null;
							
							categoriesList.add(cat);
							cat = null;
						}
						
					}
					name = null;
					startYear=  0;
					endYear=  0;
					nameForRefKey = null;
				}
				rs.close();rs=null;
				// close stmt object
				stmt.close();stmt=null;
				sql = null;
				name = null;
				startYear = 0;
				endYear=  0;
				cat = null;
				levelDetails=  null;
				nameForRefKey = null;
				
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT MDM_CRLN_CODE_ID,MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG,MDM_CRLN_YEAR_START,MDM_CRLN_YEAR_END "
							+ " FROM gms3_mdm_carline_codes WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
						{
							name=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
						}
						else
						{
							name=rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						}
						nameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						startYear = rs.getInt("MDM_CRLN_YEAR_START");
						endYear= rs.getInt("MDM_CRLN_YEAR_END");

						if(null!=nameForRefKey && !"".equals(nameForRefKey))
						{
							if(startYear>0 && endYear>0 && startYear<=endYear)
							{
								/* 
								 * ADD LEVEL 1 & LEVEL 2 FOR ALL RANGES
								 */
								for(int b=startYear;b<=endYear;b++)
								{
									// add LEVEL 1
									cat = new AutoSyncCategoryDetails();
									cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
									
									levelDetails = new AutoSyncCategoryDetails();
									// here locale will always be processingLocale
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(name);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
									levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_MODEL_YEAR"));
									cat.setLevel1Details(new AutoSyncCategoryDetails());
									cat.setLevel1Details(levelDetails);
									levelDetails=  null;
									
									
									// add LEVEL 2
									levelDetails = new AutoSyncCategoryDetails();
									// here locale will always be processingLocale
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(name+" "+String.valueOf(b));
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")+"_"+String.valueOf(b)));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
									cat.setLevel2Details(new AutoSyncCategoryDetails());
									cat.setLevel2Details(levelDetails);
									levelDetails=  null;
									
									
									categoriesList.add(cat);
									cat = null;
								}
							}
							else
							{
								// add LEVEL 1
								cat = new AutoSyncCategoryDetails();
								cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
								
								levelDetails = new AutoSyncCategoryDetails();
								// here locale will always be processingLocale
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(name);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(nameForRefKey.replace("-", "")));
								levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_MODEL_YEAR"));
								cat.setLevel1Details(new AutoSyncCategoryDetails());
								cat.setLevel1Details(levelDetails);
								levelDetails=  null;
								
								categoriesList.add(cat);
								cat = null;
							}
							
						}
						name = null;
						startYear=  0;
						endYear=  0;
						nameForRefKey = null;
					}
					rs.close();rs=null;
					// close stmt object
					stmt.close();stmt=null;
					sql = null;
					name = null;
					startYear = 0;
					endYear=  0;
					cat = null;
					levelDetails=  null;
					nameForRefKey = null;
				}
			
				processingLocale = null;
				alternateLocale= null;
			}
			sql = null;
			name = null;
			startYear = 0;
			endYear=  0;
			cat = null;
			levelDetails=  null;
			nameForRefKey = null;
			processingLocale=  null;
			alternateLocale=  null;
			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ModelYearDataDAO.class.getName(), "getModelYearItems()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				market = null;
				catType = null;
				localeMapping=  null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ModelYearDataDAO.class.getName(), "getModelYearItems()", e);
			}
		}
		return categoriesList;
	}
}
