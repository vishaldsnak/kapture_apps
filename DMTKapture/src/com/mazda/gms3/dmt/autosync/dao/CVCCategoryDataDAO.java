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

public class CVCCategoryDataDAO {

	public AutoSyncScheduleItemDetails getCVCCategoryItemsCount(String market,String catType, Connection conn,List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_CVC_CATEGORY);
			
			String sql="";
			
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale=null;
			String alternateLocale=null;
			int count = 0;			
			
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR CVC_CATEGORY
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
						checkForAlternateLocale=false;
					}

					// CHECK FOR PROCESSING LOCALE- 
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_cvc_details A , gms3_mdm_manual_language B "
							+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND  A.MDM_CVC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") >  0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							count = count + rs.getInt("COUNT");
						}
					}
					rs.close();rs=  null;
					if(null!=stmt)
					{
						stmt.close();stmt = null;
					}
					sql = null;
					
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_cvc_details A , gms3_mdm_manual_language B "
								+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
								+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND  A.MDM_CVC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
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
							stmt.close();stmt = null;
						}
						sql = null;
					}
				
				}
				sql = null;
				localeMapping=  null;
				processingLocale=  null;
				alternateLocale=  null;
			}
			
			sql = null;
			/*
			 * set count
			 */
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(CVCCategoryDataDAO.class.getName(), "getCVCCategoryItemsCount()", e);
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
				localesList = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(CVCCategoryDataDAO.class.getName(), "getCVCCategoryItemsCount()", e);
			}
		}
		return itemDetails;
	}

	public ArrayList<AutoSyncCategoryDetails> getCVCCategoryItems(String market,String catType, Connection conn,MarketLocaleMasterDataTypeMapping localeMapping)
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
			String catCode="";
			String catName="";
			String sCatCode="";
			String sCatName="";
			String symCode="";
			String symName="";
			String sSymCode="";
			String sSymName="";
			String conCode="";
			String conName="";

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR CVC_CATEGORY
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
				sql="SELECT A.MDM_CVC_ID,A.MDM_CVC_CAT_CODE,A.MDM_CVC_CAT_NAME,A.MDM_CVC_SCAT_CODE,A.MDM_CVC_SCAT_NAME,"
						+ " A.MDM_CVC_SYM_CODE,A.MDM_CVC_SYM_NAME,A.MDM_CVC_SSYM_CODE, A.MDM_CVC_SSYM_NAME,A.MDM_CVC_CON_CODE,A.MDM_CVC_CON_NAME"
						+ " FROM gms3_mdm_cvc_details A , gms3_mdm_manual_language B "
						+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
						+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND  A.MDM_CVC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;

					catCode =rs.getString("MDM_CVC_CAT_CODE").trim();
					catName= rs.getString("MDM_CVC_CAT_NAME").trim();
					sCatCode = rs.getString("MDM_CVC_SCAT_CODE").trim();
					sCatName = rs.getString("MDM_CVC_SCAT_NAME").trim();
					symCode = rs.getString("MDM_CVC_SYM_CODE").trim();
					symName = rs.getString("MDM_CVC_SYM_NAME").trim();
					sSymCode = rs.getString("MDM_CVC_SSYM_CODE").trim();
					sSymName = rs.getString("MDM_CVC_SSYM_NAME").trim();
					conCode= rs.getString("MDM_CVC_CON_CODE").trim();
					conName = rs.getString("MDM_CVC_CON_NAME").trim();

					if(null!=catCode && !"".equals(catCode) && null!=catName  && !"".equals(catName))
					{
						// add LEVEL 1
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CVC_ID")));
						levelDetails=  new AutoSyncCategoryDetails();
						
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(catName);
						levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode));
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=  null;

						// add LEVEL 2
						if(null!=sCatCode && !"".equals(sCatCode) && null!=sCatName && !"".equals(sCatName))
						{
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(sCatName);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode));
							levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode));
							cat.setLevel2Details(new AutoSyncCategoryDetails());
							cat.setLevel2Details(levelDetails);
							levelDetails = null;
							

							// add LEVEL 3
							if(null!=symCode && !"".equals(symCode) && null!=symName && !"".equals(symName))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(symName);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode));
								cat.setLevel3Details(new AutoSyncCategoryDetails());
								cat.setLevel3Details(levelDetails);
								levelDetails = null;

								// add LEVEL 4
								if(null!=sSymCode && !"".equals(sSymCode) && null!=sSymName && !"".equals(sSymName))
								{
									levelDetails = new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(sSymName);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode));
									cat.setLevel4Details(new AutoSyncCategoryDetails());
									cat.setLevel4Details(levelDetails);
									levelDetails=  null;

									// add LEVEL 5
									if(null!=conCode && !"".equals(conCode) && null!=conName && !"".equals(conName))
									{
										levelDetails = new AutoSyncCategoryDetails();
										// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
										levelDetails.setLocale(processingLocale.replace("-", "_"));
										levelDetails.setCategoryName(conName);
										levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode+conCode));
										levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode));
										cat.setLevel5Details(new AutoSyncCategoryDetails());
										cat.setLevel5Details(levelDetails);
										levelDetails = null;
									}
								}
							}
						}
						categoriesList.add(cat);
						cat = null;
					}

					catCode=  null;
					catName = null;
					sCatCode=null;
					sCatName = null;
					symCode=  null;
					symName =null;
					sSymCode=null;
					sSymName =null;
					conCode= null;
					conName=  null;
					cat= null;
					levelDetails=null;
				}
				rs.close();rs=  null;
				if(null!=stmt)
				{
					stmt.close();stmt = null;
				}
				catCode=  null;
				catName = null;
				sCatCode=null;
				sCatName = null;
				symCode=  null;
				symName =null;
				sSymCode=null;
				sSymName =null;
				conCode= null;
				conName=  null;
				cat= null;
				levelDetails=null;
				sql = null;
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT A.MDM_CVC_ID,A.MDM_CVC_CAT_CODE,A.MDM_CVC_CAT_NAME,A.MDM_CVC_SCAT_CODE,A.MDM_CVC_SCAT_NAME,"
							+ " A.MDM_CVC_SYM_CODE,A.MDM_CVC_SYM_NAME,A.MDM_CVC_SSYM_CODE, A.MDM_CVC_SSYM_NAME,A.MDM_CVC_CON_CODE,A.MDM_CVC_CON_NAME"
							+ " FROM gms3_mdm_cvc_details A , gms3_mdm_manual_language B "
							+ " WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND  A.MDM_CVC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						catCode =rs.getString("MDM_CVC_CAT_CODE").trim();
						catName= rs.getString("MDM_CVC_CAT_NAME").trim();
						sCatCode = rs.getString("MDM_CVC_SCAT_CODE").trim();
						sCatName = rs.getString("MDM_CVC_SCAT_NAME").trim();
						symCode = rs.getString("MDM_CVC_SYM_CODE").trim();
						symName = rs.getString("MDM_CVC_SYM_NAME").trim();
						sSymCode = rs.getString("MDM_CVC_SSYM_CODE").trim();
						sSymName = rs.getString("MDM_CVC_SSYM_NAME").trim();
						conCode= rs.getString("MDM_CVC_CON_CODE").trim();
						conName = rs.getString("MDM_CVC_CON_NAME").trim();

						if(null!=catCode && !"".equals(catCode) && null!=catName  && !"".equals(catName))
						{
							// add LEVEL 1
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CVC_ID")));
							levelDetails=  new AutoSyncCategoryDetails();
							
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(catName);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=  null;

							// add LEVEL 2
							if(null!=sCatCode && !"".equals(sCatCode) && null!=sCatName && !"".equals(sCatName))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(sCatName);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails = null;
								

								// add LEVEL 3
								if(null!=symCode && !"".equals(symCode) && null!=symName && !"".equals(symName))
								{
									levelDetails = new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(symName);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode));
									cat.setLevel3Details(new AutoSyncCategoryDetails());
									cat.setLevel3Details(levelDetails);
									levelDetails = null;

									// add LEVEL 4
									if(null!=sSymCode && !"".equals(sSymCode) && null!=sSymName && !"".equals(sSymName))
									{
										levelDetails = new AutoSyncCategoryDetails();
										// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
										levelDetails.setLocale(processingLocale.replace("-", "_"));
										levelDetails.setCategoryName(sSymName);
										levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode));
										levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode));
										cat.setLevel4Details(new AutoSyncCategoryDetails());
										cat.setLevel4Details(levelDetails);
										levelDetails=  null;

										// add LEVEL 5
										if(null!=conCode && !"".equals(conCode) && null!=conName && !"".equals(conName))
										{
											levelDetails = new AutoSyncCategoryDetails();
											// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
											levelDetails.setLocale(processingLocale.replace("-", "_"));
											levelDetails.setCategoryName(conName);
											levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode+conCode));
											levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(ApplicationProperties.getProperty("PARENT_REF_KEYS_CVC_CATEGORY")+"_"+catCode+sCatCode+symCode+sSymCode));
											cat.setLevel5Details(new AutoSyncCategoryDetails());
											cat.setLevel5Details(levelDetails);
											levelDetails = null;
										}
									}
								}
							}
							categoriesList.add(cat);
							cat = null;
						}

						catCode=  null;
						catName = null;
						sCatCode=null;
						sCatName = null;
						symCode=  null;
						symName =null;
						sSymCode=null;
						sSymName =null;
						conCode= null;
						conName=  null;
						cat= null;
						levelDetails=null;
					}
					rs.close();rs=  null;
					if(null!=stmt)
					{
						stmt.close();stmt = null;
					}
					catCode=  null;
					catName = null;
					sCatCode=null;
					sCatName = null;
					symCode=  null;
					symName =null;
					sSymCode=null;
					sSymName =null;
					conCode= null;
					conName=  null;
					cat= null;
					levelDetails=null;
					sql = null;
						
				}
			
			}
			sql = null;
			processingLocale=  null;
			alternateLocale=  null;
			catCode=  null;
			catName = null;
			sCatCode=null;
			sCatName = null;
			symCode=  null;
			symName =null;
			sSymCode=null;
			sSymName =null;
			conCode= null;
			conName=  null;
			cat= null;
			levelDetails=null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(CVCCategoryDataDAO.class.getName(), "getCVCCategoryItems()", e);
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
				Utilities.printStackTraceToLogs(CVCCategoryDataDAO.class.getName(), "getCVCCategoryItems()", e);
			}
		}
		return categoriesList;
	}

}
