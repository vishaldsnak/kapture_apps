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

public class ESICategoryDataDAO {

	public AutoSyncScheduleItemDetails getESICategoryItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_ESI_CATEGORY);
			
			int count=0;
			String sql="";
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale=null;
			String alternateLocale=null;
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR ESI_CATEGORY
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
						// no need to check for alternate Locale
						checkForAlternateLocale = false;
					}
					// CHECK FOR PROCESSING LOCALE- 
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_category A , gms3_mdm_manual_language B "
							+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND  A.MDM_CAT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") > 0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							count = count+ rs.getInt("COUNT");
						}
					}
					rs.close();rs=  null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql  =null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_category A , gms3_mdm_manual_language B "
								+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
								+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND  A.MDM_CAT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count + rs.getInt("COUNT");
						}
						rs.close();rs=  null;
						if(null!=stmt)
						{
							stmt.close();stmt=null;
						}
						sql = null;
					}
				}
				localeMapping=  null;
				processingLocale=  null;
				alternateLocale = null;
				sql = null;
			}
			localeMapping=  null;
			processingLocale=  null;
			alternateLocale = null;
			sql = null;
			
			// SET COUNT 
			itemDetails.setTotalCount(count);
			count = 0;
			sql = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ESICategoryDataDAO.class.getName(), "getESICategoryItemsCount()", e);
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
				Utilities.printStackTraceToLogs(ESICategoryDataDAO.class.getName(), "getESICategoryItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getESICategoryItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			String processingLocale=null;
			String alternateLocale=null;
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR ESI_CATEGORY
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
					// do not check for alternate locale
					checkForAlternateLocale=  false;
				}
				// CHECK FOR PROCESSING LOCALE- 
				sql="SELECT A.MDM_CAT_ID,A.MDM_CAT_CODE,A.MDM_CAT_NAME_ENG_LANG,A.MDM_SCAT_CODE,A.MDM_SCAT_NAME_ENG_LANG,"
						+ " A.MDM_SSCAT_CODE,A.MDM_SSCAT_NAME_ENG_LANG FROM gms3_mdm_category A , gms3_mdm_manual_language B "
						+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
						+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND  A.MDM_CAT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;

					if(null!=rs.getString("MDM_CAT_CODE") && !"".equals(rs.getString("MDM_CAT_CODE")) && 
							null!=rs.getString("MDM_CAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CAT_NAME_ENG_LANG")))
					{
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CAT_ID")));
						
						// add LEVEL 1
						levelDetails = new AutoSyncCategoryDetails();
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(rs.getString("MDM_CAT_NAME_ENG_LANG").trim());
						levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY")+rs.getString("MDM_CAT_CODE").trim()));
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=  null;

						// add LEVEL 2
						if(null!=rs.getString("MDM_SCAT_CODE") && !"".equals(rs.getString("MDM_SCAT_CODE")) && 
								null!=rs.getString("MDM_SCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SCAT_NAME_ENG_LANG")))
						{
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(rs.getString("MDM_SCAT_NAME_ENG_LANG").trim());
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()));
							levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY")+rs.getString("MDM_CAT_CODE").trim()));
							cat.setLevel2Details(new AutoSyncCategoryDetails());
							cat.setLevel2Details(levelDetails);
							levelDetails = null;

							// add LEVEL 3
							if(null!=rs.getString("MDM_SSCAT_CODE") && !"".equals(rs.getString("MDM_SSCAT_CODE")) && !"00".equals(rs.getString("MDM_SSCAT_CODE").trim()) && 
									null!=rs.getString("MDM_SSCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SSCAT_NAME_ENG_LANG")))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(rs.getString("MDM_SSCAT_NAME_ENG_LANG").trim());
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()+rs.getString("MDM_SSCAT_CODE").trim()));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()));
								cat.setLevel3Details(levelDetails);
								levelDetails = null;
							}
						}
						
						// add to categoriesList
						categoriesList.add(cat);
						cat = null;
					}
				}
				rs.close();rs=  null;
				if(null!=stmt)
				{
					stmt.close();stmt=null;
				}
				cat = null;
				sql = null;

				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT A.MDM_CAT_ID,A.MDM_CAT_CODE,A.MDM_CAT_NAME_ENG_LANG,A.MDM_SCAT_CODE,A.MDM_SCAT_NAME_ENG_LANG,"
							+ " A.MDM_SSCAT_CODE,A.MDM_SSCAT_NAME_ENG_LANG FROM gms3_mdm_category A , gms3_mdm_manual_language B "
							+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND  A.MDM_CAT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						if(null!=rs.getString("MDM_CAT_CODE") && !"".equals(rs.getString("MDM_CAT_CODE")) && 
								null!=rs.getString("MDM_CAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CAT_NAME_ENG_LANG")))
						{
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CAT_ID")));
							
							// add LEVEL 1
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(rs.getString("MDM_CAT_NAME_ENG_LANG").trim());
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY")+rs.getString("MDM_CAT_CODE").trim()));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=  null;

							// add LEVEL 2
							if(null!=rs.getString("MDM_SCAT_CODE") && !"".equals(rs.getString("MDM_SCAT_CODE")) && 
									null!=rs.getString("MDM_SCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SCAT_NAME_ENG_LANG")))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(rs.getString("MDM_SCAT_NAME_ENG_LANG").trim());
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_ESI_CATEGORY")+rs.getString("MDM_CAT_CODE").trim()));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails = null;

								// add LEVEL 3
								if(null!=rs.getString("MDM_SSCAT_CODE") && !"".equals(rs.getString("MDM_SSCAT_CODE")) && !"00".equals(rs.getString("MDM_SSCAT_CODE").trim()) && 
										null!=rs.getString("MDM_SSCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SSCAT_NAME_ENG_LANG")))
								{
									levelDetails = new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(rs.getString("MDM_SSCAT_NAME_ENG_LANG").trim());
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()+rs.getString("MDM_SSCAT_CODE").trim()));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(rs.getString("MDM_SCAT_CODE").trim()));
									cat.setLevel3Details(levelDetails);
									levelDetails = null;
								}
							}
							
							// add to categoriesList
							categoriesList.add(cat);
							cat = null;
						}
					}
					rs.close();rs=  null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					cat = null;
					sql = null;
				}

				sql = null;
			}
			processingLocale=  null;
			alternateLocale = null;
			sql = null;
			cat = null;
			levelDetails=  null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ESICategoryDataDAO.class.getName(), "getESICategoryItems()", e);
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
				localeMapping = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ESICategoryDataDAO.class.getName(), "getESICategoryItems()", e);
			}
		}
		return categoriesList;
	}
}