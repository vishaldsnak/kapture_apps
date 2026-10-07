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

public class VINRangeDataDAO {

	public AutoSyncScheduleItemDetails getVINRangeItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_VIN_RANGE);

			int count=0;
			String sql="";
			
			MarketLocaleMasterDataTypeMapping localeMapping=null;
			String processingLocale="";
			String alternateLocale="";
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
				{
					// LOCALE BELONGS TO MNAO MARKET & FOR VIN_RANGE
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
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT")  > 0)
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							count = count +rs.getInt("COUNT");
						}
					}
					rs.close();rs=null;
					stmt.close();stmt = null;
					sql = null;

					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count +rs.getInt("COUNT");
						}
						rs.close();rs=null;
						stmt.close();stmt = null;
						sql = null;
					}
				
				}
				localeMapping = null;
				processingLocale=  null;
				alternateLocale=  null; 
			}
			localeMapping = null;
			processingLocale=  null;
			alternateLocale=  null;
			sql = null;
			/*
			 *  SET COUNT - 
			 */
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VINRangeDataDAO.class.getName(), "getVINRangeItemsCount()", e);
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
				Utilities.printStackTraceToLogs(VINRangeDataDAO.class.getName(), "getVINRangeItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getVINRangeItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String wmi="";
			String vds="";
			String visStart="";
			String visEnd="";
			String processingLocale="";
			String alternateLocale="";
			
			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
			{

				// LOCALE BELONGS TO MNAO MARKET & FOR VIN_RANGE
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
					checkForAlternateLocale = false;
				}

				// CHECK FOR PROCESSING LOCALE- 
				sql="SELECT MDM_VIN_ID,MDM_VIN_WMI_CODE, MDM_VIN_VDS_CODE, MDM_VIN_VIS_START_RANGE,MDM_VIN_VIS_END_RANGE "
						+ " FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// check for alternate locale = false
					checkForAlternateLocale = false;
					wmi=rs.getString("MDM_VIN_WMI_CODE").trim();
					vds = rs.getString("MDM_VIN_VDS_CODE").trim();
					visStart = rs.getString("MDM_VIN_VIS_START_RANGE").trim();
					visEnd= rs.getString("MDM_VIN_VIS_END_RANGE").trim();

					if(null!=wmi && !"".equals(wmi))
					{
						// add LEVEL 1
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_VIN_ID")));
						
						levelDetails = new AutoSyncCategoryDetails();
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(wmi);
						levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi));
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_VIN_RANGE"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=  null;

						// add LEVEL 2
						if(null!=vds && !"".equals(vds))
						{
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(vds);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi+vds));
							levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(wmi));
							cat.setLevel2Details(new AutoSyncCategoryDetails());
							cat.setLevel2Details(levelDetails);
							levelDetails=  null;

							// add LEVEL 3
							if(null!=visStart && !"".equals(visStart) && null!=visEnd && !"".equals(visEnd))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(wmi+vds+visStart+visEnd);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi+vds+visStart+visEnd));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(wmi+vds));
								cat.setLevel3Details(new AutoSyncCategoryDetails());
								cat.setLevel3Details(levelDetails);
								levelDetails=  null;
							}
						}
						
						categoriesList.add(cat);
						cat = null;
					}

					wmi = null;
					vds = null;
					visStart = null;
					visEnd=null;
				}
				rs.close();rs=null;
				stmt.close();stmt = null;
				sql = null;
				wmi = null;
				vds = null;
				visStart = null;
				visEnd=null;
				cat = null;
				levelDetails = null;

				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT MDM_VIN_ID,MDM_VIN_WMI_CODE, MDM_VIN_VDS_CODE, MDM_VIN_VIS_START_RANGE,MDM_VIN_VIS_END_RANGE "
							+ " FROM gms3_mdm_vin_detail WHERE MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						wmi=rs.getString("MDM_VIN_WMI_CODE").trim();
						vds = rs.getString("MDM_VIN_VDS_CODE").trim();
						visStart = rs.getString("MDM_VIN_VIS_START_RANGE").trim();
						visEnd= rs.getString("MDM_VIN_VIS_END_RANGE").trim();

						if(null!=wmi && !"".equals(wmi))
						{
							// add LEVEL 1
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_VIN_ID")));
							
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(wmi);
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_VIN_RANGE"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=  null;

							// add LEVEL 2
							if(null!=vds && !"".equals(vds))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(vds);
								levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi+vds));
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(wmi));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails=  null;

								// add LEVEL 3
								if(null!=visStart && !"".equals(visStart) && null!=visEnd && !"".equals(visEnd))
								{
									levelDetails = new AutoSyncCategoryDetails();
									// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
									levelDetails.setLocale(processingLocale.replace("-", "_"));
									levelDetails.setCategoryName(wmi+vds+visStart+visEnd);
									levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(wmi+vds+visStart+visEnd));
									levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(wmi+vds));
									cat.setLevel3Details(new AutoSyncCategoryDetails());
									cat.setLevel3Details(levelDetails);
									levelDetails=  null;
								}
							}
							
							categoriesList.add(cat);
							cat = null;
						}

						wmi = null;
						vds = null;
						visStart = null;
						visEnd=null;
					}
					rs.close();rs=null;
					stmt.close();stmt = null;
					sql = null;
					wmi = null;
					vds = null;
					visStart = null;
					visEnd=null;
					cat = null;
					levelDetails = null;
				}
			
			}
			processingLocale=  null;
			alternateLocale=  null;
			sql = null;
			cat = null;
			levelDetails=  null;
			wmi= null;
			vds=null;
			visStart= null;
			visEnd = null;
			
			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VINRangeDataDAO.class.getName(), "getVINRangeItems()", e);
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
				Utilities.printStackTraceToLogs(VINRangeDataDAO.class.getName(), "getVINRangeItems()", e);
			}
		}
		return categoriesList;
	}

}
