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

public class CarlineDataDAO {
	
	public AutoSyncScheduleItemDetails getCarlineItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_CARLINE);

			int carlineCount=0;
			int vinCount = 0;
			String sql="";
			
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale=null;
			String alternateLocale=null;
			
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping = (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
				{
					// LOCALE BELONGS TO MC / MME MARKET AND IS FOR CARLINE
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
						// do not check for alternate Locale
						checkForAlternateLocale=  false;
					}
					
					// CHECK FOR PROCESSING LOCALE- 
					// LEVEL 1 FROM CARLINE TABLE
					sql = "SELECT COUNT(*) AS COUNT FROM gms3_mdm_carline_codes WHERE ";
					sql=sql+" MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") >  0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							// LEVLE 1 COUNT - DO NOT ADD ANYTHING
							carlineCount = carlineCount+ rs.getInt("COUNT");
						}
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt= null;
					}
					sql = null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						// LEVEL 1 FROM CARLINE TABLE
						sql = "SELECT COUNT(*) AS COUNT  FROM gms3_mdm_carline_codes WHERE ";
						sql=sql+" MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
						
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							// LEVLE 1 COUNT - DO NOT ADD ANYTHING
							carlineCount = carlineCount+ rs.getInt("COUNT");
						}
						rs.close();rs=null;
						if(null!=stmt)
						{
							stmt.close();stmt= null;
						}
						sql = null;
					}
				
				}
				sql = null;
				localeMapping=  null;
				sql = null;
				processingLocale = null;
				alternateLocale=  null;
			}
			sql = null;
			localeMapping=  null;	
			processingLocale=  null;
			alternateLocale=  null;
			
			
			/*
			 * OTHER LEVELS
			 */
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping = (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
				{
					// LOCALE BELONGS TO MC / MME MARKET AND IS FOR CARLINE
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
						// do not check for Alternate Locale
						checkForAlternateLocale=  false;
					}
					// CHECK FOR PROCESSING LOCALE- 
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'  ";
					
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") >  0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							vinCount= vinCount+rs.getInt("COUNT");
						}
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt= null;
					}
					sql = null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'  ";
						
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							vinCount= vinCount+rs.getInt("COUNT");
						}
						rs.close();rs=null;
						if(null!=stmt)
						{
							stmt.close();stmt= null;
						}
						sql = null;
					}
				
				}
				localeMapping = null;
				processingLocale = null;
				alternateLocale=  null;
				sql = null;
			}
			
			
			localeMapping = null;
			processingLocale = null;
			alternateLocale=  null;
			sql = null;
			
			
			// SET COUNT 
			int count= carlineCount+ vinCount;
			itemDetails.setTotalCount(count);
			carlineCount=  0;
			vinCount=  0;
			count=0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(CarlineDataDAO.class.getName(), "getCarlineItemsCount()", e);
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
				Utilities.printStackTraceToLogs(CarlineDataDAO.class.getName(), "getCarlineItemsCount()", e);
			}
		}
		return itemDetails;
	}

	public ArrayList<AutoSyncCategoryDetails> getCarlineItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
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
			String carlineCode="";
			String carlineName="";
			String carlineRefKey="";
			String carlineNameForRefKey="";
			
			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
			{
				// LOCALE BELONGS TO MC / MME MARKET AND IS FOR CARLINE
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
					checkForAlternateLocale =  false;
				}
				// CHECK FOR PROCESSING LOCALE- 
				// LEVEL 1 FROM CARLINE TABLE
				sql = "SELECT MDM_CRLN_CODE_ID,MDM_CRLN_CODE, MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG FROM gms3_mdm_carline_codes WHERE ";
				sql=sql+" MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data found for processing locale
					checkForAlternateLocale = false;

					carlineCode= rs.getString("MDM_CRLN_CODE").trim();
					if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
					{
						carlineName= rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
					}
					else
					{
						carlineName = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					}
					carlineNameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					
					if(null!=carlineNameForRefKey && !"".equals(carlineNameForRefKey) && null!=carlineCode  && !"".equals(carlineCode))
					{
						// add LEVEL 1
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
						
						levelDetails = new AutoSyncCategoryDetails();
						// here locale will always be processingLocale
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(carlineName+" "+carlineCode);
						carlineRefKey=carlineNameForRefKey;
						carlineRefKey = carlineRefKey.replace("-", "");
						carlineRefKey=carlineRefKey+"_"+carlineCode;
						levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey));
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=null;
						
						
						categoriesList.add(cat);
						cat = null;
					}
					carlineCode="";
					carlineName="";
					carlineRefKey="";
					carlineNameForRefKey ="";
					cat = null;
				}
				rs.close();rs=null;
				if(null!=stmt)
				{
					stmt.close();stmt= null;
				}
				sql = null;
				
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					// LEVEL 1 FROM CARLINE TABLE
					sql = "SELECT MDM_CRLN_CODE_ID,MDM_CRLN_CODE, MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG FROM gms3_mdm_carline_codes WHERE ";
					sql=sql+" MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						carlineCode= rs.getString("MDM_CRLN_CODE").trim();
						if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
						{
							carlineName= rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
						}
						else
						{
							carlineName = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						}
						carlineNameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						
						if(null!=carlineNameForRefKey && !"".equals(carlineNameForRefKey) && null!=carlineCode  && !"".equals(carlineCode))
						{
							// add LEVEL 1
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_CRLN_CODE_ID")));
							
							levelDetails = new AutoSyncCategoryDetails();
							// here locale will always be processingLocale
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(carlineName+" "+carlineCode);
							carlineRefKey=carlineNameForRefKey;
							carlineRefKey = carlineRefKey.replace("-", "");
							carlineRefKey=carlineRefKey+"_"+carlineCode;
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=null;
							
							
							categoriesList.add(cat);
							cat = null;
						}
						carlineCode="";
						carlineName="";
						carlineRefKey="";
						carlineNameForRefKey ="";
						cat = null;
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt= null;
					}
					sql = null;
				}
			
			}
			
			
			sql = null;
			carlineCode="";
			carlineName="";
			carlineRefKey="";
			carlineNameForRefKey ="";
			cat = null;
			processingLocale=  null;
			alternateLocale=  null;
			levelDetails=null;
			
			
			/*
			 * OTHER LEVELS
			 */
			String wmiRefKey="";
			String wmi="";
			String vds="";
			String visStart="";
			String visEnd="";
			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
			{
				// LOCALE BELONGS TO MC / MME MARKET AND IS FOR CARLINE
				processingLocale = localeMapping.getLocale();
				alternateLocale = localeMapping.getAlternateLocale();
				cat = null;
				carlineCode="";
				carlineName="";
				carlineRefKey="";
				carlineNameForRefKey="";
				wmiRefKey="";
				wmi="";
				vds="";
				visStart="";
				visEnd="";
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
				sql="SELECT MDM_VIN_ID,MDM_CRLN_CODE, MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG ,MDM_VIN_WMI_CODE, MDM_VIN_VDS_CODE, MDM_VIN_VIS_START_RANGE,MDM_VIN_VIS_END_RANGE "
						+ " FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;
					carlineCode= rs.getString("MDM_CRLN_CODE").trim();
					if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
					{
						carlineName= rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
					}
					else
					{
						carlineName = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					}
					carlineNameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
					wmi=rs.getString("MDM_VIN_WMI_CODE").trim();
					vds = rs.getString("MDM_VIN_VDS_CODE").trim();
					visStart = rs.getString("MDM_VIN_VIS_START_RANGE").trim();
					visEnd= rs.getString("MDM_VIN_VIS_END_RANGE").trim();
					
					if(null!=wmi && !"".equals(wmi))
					{
						if(wmi.equals("-"))
						{
							wmiRefKey="___";
						}
						else
						{
							wmiRefKey = wmi;
						}
					}
					if(null!=carlineNameForRefKey && !"".equals(carlineNameForRefKey) && null!=carlineCode  && !"".equals(carlineCode))
					{
						carlineRefKey=carlineNameForRefKey;
						carlineRefKey = carlineRefKey.replace("-", "");
						carlineRefKey=carlineRefKey+"_"+carlineCode;
						
						// add LEVEL 2
						if(null!=wmi && !"".equals(wmi))
						{
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_VIN_ID")));
							levelDetails=  new AutoSyncCategoryDetails();
							
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(wmi);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey+wmiRefKey));
							levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey));
							cat.setLevel2Details(new AutoSyncCategoryDetails());
							cat.setLevel2Details(levelDetails);
							levelDetails = null;
							
							
							// add LEVEL 3
							if(null!=vds && !"".equals(vds))
							{
								levelDetails=  new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(vds);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey+wmiRefKey));
								cat.setLevel3Details(new AutoSyncCategoryDetails());
								cat.setLevel3Details(levelDetails);
								levelDetails=  null;
								
								
								// add LEVEL 4
								if(null!=visStart && !"".equals(visStart))
								{
									levelDetails = new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(visStart);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds));
									cat.setLevel4Details(new AutoSyncCategoryDetails());
									cat.setLevel4Details(levelDetails);
									levelDetails = null;
									
									// add LEVEL 5
									if(null!=visEnd && !"".equals(visEnd))
									{
										levelDetails = new AutoSyncCategoryDetails();
										// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
										levelDetails.setLocale(processingLocale.replace("-", "_"));
										if(wmi.trim().equals("-"))
										{
											levelDetails.setCategoryName(carlineName+" "+carlineCode+" "+vds+"-"+visStart+"-"+visEnd);
										}
										else
										{
											levelDetails.setCategoryName(carlineName+" "+carlineCode+" "+wmi+"-"+vds+"-"+visStart+"-"+visEnd);
										}
										levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart+visEnd));
										levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart));
										cat.setLevel5Details(new AutoSyncCategoryDetails());
										cat.setLevel5Details(levelDetails);
										levelDetails = null;
									}
								}
							}
							
							categoriesList.add(cat);
							cat = null;
						}
					}
					
					carlineRefKey = null;
					wmiRefKey= null;
					carlineCode =null;
					carlineName = null;
					wmi = null;
					vds = null;
					visStart = null;
					visEnd=null;
					carlineNameForRefKey = null;
				}
				rs.close();rs=null;
				if(null!=stmt)
				{
					stmt.close();stmt= null;
				}
				sql = null;
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT MDM_VIN_ID,MDM_CRLN_CODE, MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG ,MDM_VIN_WMI_CODE, MDM_VIN_VDS_CODE, MDM_VIN_VIS_START_RANGE,MDM_VIN_VIS_END_RANGE "
							+ " FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						carlineCode= rs.getString("MDM_CRLN_CODE").trim();
						if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
						{
							carlineName= rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim();
						}
						else
						{
							carlineName = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						}
						carlineNameForRefKey = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
						wmi=rs.getString("MDM_VIN_WMI_CODE").trim();
						vds = rs.getString("MDM_VIN_VDS_CODE").trim();
						visStart = rs.getString("MDM_VIN_VIS_START_RANGE").trim();
						visEnd= rs.getString("MDM_VIN_VIS_END_RANGE").trim();
						
						if(null!=wmi && !"".equals(wmi))
						{
							if(wmi.equals("-"))
							{
								wmiRefKey="___";
							}
							else
							{
								wmiRefKey = wmi;
							}
						}
						if(null!=carlineNameForRefKey && !"".equals(carlineNameForRefKey) && null!=carlineCode  && !"".equals(carlineCode))
						{
							carlineRefKey=carlineNameForRefKey;
							carlineRefKey = carlineRefKey.replace("-", "");
							carlineRefKey=carlineRefKey+"_"+carlineCode;
							
							// add LEVEL 2
							if(null!=wmi && !"".equals(wmi))
							{
								cat = new AutoSyncCategoryDetails();
								cat.setMdmItemId(String.valueOf(rs.getLong("MDM_VIN_ID")));
								levelDetails=  new AutoSyncCategoryDetails();
								
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(wmi);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey+wmiRefKey));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails = null;
								
								
								// add LEVEL 3
								if(null!=vds && !"".equals(vds))
								{
									levelDetails=  new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(vds);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineRefKey+wmiRefKey));
									cat.setLevel3Details(new AutoSyncCategoryDetails());
									cat.setLevel3Details(levelDetails);
									levelDetails=  null;
									
									
									// add LEVEL 4
									if(null!=visStart && !"".equals(visStart))
									{
										levelDetails = new AutoSyncCategoryDetails();
										// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
										levelDetails.setLocale(processingLocale.replace("-", "_"));
										levelDetails.setCategoryName(visStart);
										levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart));
										levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds));
										cat.setLevel4Details(new AutoSyncCategoryDetails());
										cat.setLevel4Details(levelDetails);
										levelDetails = null;
										
										// add LEVEL 5
										if(null!=visEnd && !"".equals(visEnd))
										{
											levelDetails = new AutoSyncCategoryDetails();
											// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
											levelDetails.setLocale(processingLocale.replace("-", "_"));
											if(wmi.trim().equals("-"))
											{
												levelDetails.setCategoryName(carlineName+" "+carlineCode+" "+vds+"-"+visStart+"-"+visEnd);
											}
											else
											{
												levelDetails.setCategoryName(carlineName+" "+carlineCode+" "+wmi+"-"+vds+"-"+visStart+"-"+visEnd);
											}
											levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart+visEnd));
											levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(carlineCode+wmiRefKey+vds+visStart));
											cat.setLevel5Details(new AutoSyncCategoryDetails());
											cat.setLevel5Details(levelDetails);
											levelDetails = null;
										}
									}
								}
								
								categoriesList.add(cat);
								cat = null;
							}
						}
						
						carlineRefKey = null;
						wmiRefKey= null;
						carlineCode =null;
						carlineName = null;
						wmi = null;
						vds = null;
						visStart = null;
						visEnd=null;
						carlineNameForRefKey = null;
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt= null;
					}
					sql  =null;
				}
			
			}
			
			processingLocale = null;
			alternateLocale=  null;
			cat = null;
			carlineRefKey = null;
			wmiRefKey= null;
			carlineCode =null;
			carlineName = null;
			wmi = null;
			vds = null;
			visStart = null;
			visEnd=null;
			carlineNameForRefKey = null;
			sql = null;
			
			
			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(CarlineDataDAO.class.getName(), "getCarlineItems()", e);
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
				Utilities.printStackTraceToLogs(CarlineDataDAO.class.getName(), "getCarlineItems()", e);
			}
		}
		return categoriesList;
	}

}
