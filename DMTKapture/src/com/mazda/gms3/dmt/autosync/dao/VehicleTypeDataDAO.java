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

public class VehicleTypeDataDAO {

	public AutoSyncScheduleItemDetails getAxleTypeItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_AXLE_TYPE);
			String sql="";
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale = null;
			String alternateLocale = null;
			int count = 0;
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR AXLE_TYPE
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
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_axle_type A, "
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_AT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					
					stmt = conn.createStatement();
					rs  =stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") >  0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							count = count+ rs.getInt("COUNT");
						}
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();
						stmt=null;
					}
					sql = null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_axle_type A, "
								+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
								+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_AT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
						
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count+rs.getInt("COUNT");
						}
						rs.close();rs=null;
						if(null!=stmt)
						{
							stmt.close();
							stmt=null;
						}
						sql = null;
					}
				
				}
				localeMapping = null;
				processingLocale=  null;
				alternateLocale=  null;
				sql = null;
			}
			localeMapping = null;
			processingLocale=  null;
			alternateLocale=  null;
			sql = null;
			
			/*
			 * COUNT NO MULTIPLLICATION AS LEVEL 1
			 */
			itemDetails.setTotalCount(count);
			count=0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getAxleTypeItemsCount()", e);
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
				Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getAxleTypeItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getAxleTypeItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String processingLocale = null;
			String alternateLocale = null;

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR AXLE_TYPE
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
				sql="SELECT A.MDM_AT_ID,A.MDM_AT_AXLE_TYPE,A.MDM_AT_AXLE_TYPE_DESC_REG,A.MDM_AT_AXLE_TYPE_DESC_ENG FROM gms3_mdm_axle_type A, "
						+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
						+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_AT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;
					
					cat = new AutoSyncCategoryDetails();
					cat.setMdmItemId(String.valueOf(rs.getLong("MDM_AT_ID")));
					/*
					 * always 1 level only
					 */
					levelDetails = new AutoSyncCategoryDetails();
					// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
					levelDetails.setLocale(processingLocale.replace("-", "_"));
					if(null!=rs.getString("MDM_AT_AXLE_TYPE_DESC_REG") && !"".equals(rs.getString("MDM_AT_AXLE_TYPE_DESC_REG")))
					{
						levelDetails.setCategoryName(rs.getString("MDM_AT_AXLE_TYPE_DESC_REG").trim());
					}
					else
					{
						levelDetails.setCategoryName(rs.getString("MDM_AT_AXLE_TYPE_DESC_ENG").trim());
					}
					levelDetails.setCategoryRefKey(rs.getString("MDM_AT_AXLE_TYPE").trim());
					levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_AXLE_TYPE").trim());
					cat.setLevel1Details(new AutoSyncCategoryDetails());
					cat.setLevel1Details(levelDetails);
					levelDetails = null;
					
					categoriesList.add(cat);
					cat = null;
				}
				rs.close();rs = null;
				if(null!=stmt)
				{
					stmt.close();stmt=null;
				}
				sql = null;
				
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT A.MDM_AT_ID,A.MDM_AT_AXLE_TYPE,A.MDM_AT_AXLE_TYPE_DESC_REG,A.MDM_AT_AXLE_TYPE_DESC_ENG FROM gms3_mdm_axle_type A, "
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_AT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_AT_ID")));
						/*
						 * always 1 level only
						 */
						levelDetails = new AutoSyncCategoryDetails();
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						if(null!=rs.getString("MDM_AT_AXLE_TYPE_DESC_REG") && !"".equals(rs.getString("MDM_AT_AXLE_TYPE_DESC_REG")))
						{
							levelDetails.setCategoryName(rs.getString("MDM_AT_AXLE_TYPE_DESC_REG").trim());
						}
						else
						{
							levelDetails.setCategoryName(rs.getString("MDM_AT_AXLE_TYPE_DESC_ENG").trim());
						}
						levelDetails.setCategoryRefKey(rs.getString("MDM_AT_AXLE_TYPE").trim());
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_AXLE_TYPE").trim());
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails = null;
						
						categoriesList.add(cat);
						cat = null;
					}
					rs.close();rs = null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql = null;
				}
			
			}
			processingLocale=  null;
			alternateLocale=  null;
			sql = null;
			cat = null;
			levelDetails = null;

			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getAxleTypeItems()", e);
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
				Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getAxleTypeItems()", e);
			}
		}
		return categoriesList;
	}
	
	public AutoSyncScheduleItemDetails getBodyTypeItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_BODY_TYPE);
			itemDetails.setCategoryList(new ArrayList<AutoSyncCategoryDetails>());
			
			String sql="";
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale = null;
			String alternateLocale = null;
			int count = 0;
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR BODY_TYPE
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
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_body_type A, "
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID  AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_BT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						if(rs.getInt("COUNT") >  0 )
						{
							// data Found for processingLocale
							checkForAlternateLocale = false;
							count = count+rs.getInt("COUNT");
						}
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();
						stmt=null;
					}
					sql = null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_body_type A, "
								+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID  AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
								+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_BT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'  ";
						
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count+rs.getInt("COUNT");
						}
						rs.close();rs=null;
						if(null!=stmt)
						{
							stmt.close();
							stmt=null;
						}
						sql = null;
					}
				
				}
				localeMapping = null;
				processingLocale= null;
				alternateLocale=  null;
			}
			localeMapping = null;
			processingLocale= null;
			alternateLocale=  null;
			sql = null;
			
			/*
			 * SET COUNT NO MULTIPLICATION AS LEVEL 1 ONLY
			 */
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getBodyTypeItemsCount()", e);
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
				Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getBodyTypeItemsCount()", e);
			}
		}
		return itemDetails;
	}

	public ArrayList<AutoSyncCategoryDetails> getBodyTypeItems(String market,String catType, Connection conn,MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String processingLocale = null;
			String alternateLocale = null;

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR BODY_TYPE
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
				sql="SELECT A.MDM_BT_ID,A.MDM_BT_BODY_TYPE ,A.MDM_BT_BODY_TYPE_DESC_REG,A.MDM_BT_BODY_TYPE_DESC_ENG FROM gms3_mdm_body_type A, "
						+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID  AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
						+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_BT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;

					/*
					 * always 1 level only
					 */
					cat = new AutoSyncCategoryDetails();
					cat.setMdmItemId(String.valueOf(rs.getLong("MDM_BT_ID")));
					
					levelDetails = new AutoSyncCategoryDetails();
					// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
					levelDetails.setLocale(processingLocale.replace("-", "_"));
					if(null!=rs.getString("MDM_BT_BODY_TYPE_DESC_REG") && !"".equals(rs.getString("MDM_BT_BODY_TYPE_DESC_REG")))
					{
						levelDetails.setCategoryName(rs.getString("MDM_BT_BODY_TYPE_DESC_REG").trim());
					}
					else
					{
						levelDetails.setCategoryName(rs.getString("MDM_BT_BODY_TYPE_DESC_ENG").trim());
					}
					levelDetails.setCategoryRefKey(rs.getString("MDM_BT_BODY_TYPE").trim());
					levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_BODY_TYPE"));
					cat.setLevel1Details(new AutoSyncCategoryDetails());
					cat.setLevel1Details(levelDetails);
					levelDetails=null;
					
					categoriesList.add(cat);
					cat = null;
				}
				rs.close();rs=null;
				if(null!=stmt)
				{
					stmt.close();
					stmt=null;
				}
				sql = null;
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT A.MDM_BT_ID,A.MDM_BT_BODY_TYPE ,A.MDM_BT_BODY_TYPE_DESC_REG,A.MDM_BT_BODY_TYPE_DESC_ENG FROM gms3_mdm_body_type A, "
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID  AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_BT_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"' ";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{	
						/*
						 * always 1 level only
						 */
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_BT_ID")));
						
						levelDetails = new AutoSyncCategoryDetails();
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						if(null!=rs.getString("MDM_BT_BODY_TYPE_DESC_REG") && !"".equals(rs.getString("MDM_BT_BODY_TYPE_DESC_REG")))
						{
							levelDetails.setCategoryName(rs.getString("MDM_BT_BODY_TYPE_DESC_REG").trim());
						}
						else
						{
							levelDetails.setCategoryName(rs.getString("MDM_BT_BODY_TYPE_DESC_ENG").trim());
						}
						levelDetails.setCategoryRefKey(rs.getString("MDM_BT_BODY_TYPE").trim());
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_BODY_TYPE"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=null;
						
						categoriesList.add(cat);
						cat = null;
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();
						stmt=null;
					}
					sql = null;
				}
			
			}
			processingLocale= null;
			alternateLocale=  null;
			cat = null;
			levelDetails = null;
			sql = null;

			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getBodyTypeItems()", e);
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
				Utilities.printStackTraceToLogs(VehicleTypeDataDAO.class.getName(), "getBodyTypeItems()", e);
			}
		}
		return categoriesList;
	}

}
