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

public class EngineMissionTypesDataDAO {

	public AutoSyncScheduleItemDetails getTransmissionTypeItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_TRANSMISSION_TYPE);
			
			int count=0;
			String sql="";
			MarketLocaleMasterDataTypeMapping localeMapping = null;
			String processingLocale=null;
			String alternateLocale= null;
			
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR TRANSMISSION_TYPE
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
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_trans_type A,"
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
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
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql = null;
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_trans_type A,"
								+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
								+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
						
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count + rs.getInt("COUNT");
						}
						rs.close();rs=null;
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
			
			/*
			 * set count - level 1 only so no multiplication
			 */
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getTransmissionTypeItemsCount()", e);
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
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getTransmissionTypeItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getTransmissionTypeItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String processingLocale=null;
			String alternateLocale= null;

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR TRANSMISSION_TYPE
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
				sql="SELECT A.MDM_TRANS_TYPE_ID,A.MDM_TRANS_TYPE_TYPE_CODE,A.MDM_TRANS_TYPE_TYPE_NAME FROM gms3_mdm_trans_type A,"
						+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' "
						+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
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
					cat.setMdmItemId(String.valueOf(rs.getLong("MDM_TRANS_TYPE_ID")));

					levelDetails=  new  AutoSyncCategoryDetails();
					// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
					levelDetails.setLocale(processingLocale.replace("-", "_"));
					levelDetails.setCategoryName(rs.getString("MDM_TRANS_TYPE_TYPE_NAME").trim());
					levelDetails.setCategoryRefKey(rs.getString("MDM_TRANS_TYPE_TYPE_CODE").trim());
					levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_TRANSMISSION_TYPE"));
					cat.setLevel1Details(new AutoSyncCategoryDetails());
					cat.setLevel1Details(levelDetails);
					levelDetails=  null;
					categoriesList.add(cat);
					cat = null;
				}
				rs.close();rs=null;
				if(null!=stmt)
				{
					stmt.close();stmt=null;
				}
				sql = null;
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					sql="SELECT A.MDM_TRANS_TYPE_ID,A.MDM_TRANS_TYPE_TYPE_CODE,A.MDM_TRANS_TYPE_TYPE_NAME FROM gms3_mdm_trans_type A,"
							+ " gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' "
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						/*
						 * always 1 level only
						 */
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_TRANS_TYPE_ID")));

						levelDetails=  new  AutoSyncCategoryDetails();
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(rs.getString("MDM_TRANS_TYPE_TYPE_NAME").trim());
						levelDetails.setCategoryRefKey(rs.getString("MDM_TRANS_TYPE_TYPE_CODE").trim());
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_TRANSMISSION_TYPE"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=  null;
						categoriesList.add(cat);
						cat = null;
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql = null;
				}
			
			}

			processingLocale=  null;
			alternateLocale = null;
			cat = null;
			levelDetails = null;
			sql = null;
			cat = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getTransmissionTypeItems()", e);
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
				Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getTransmissionTypeItems()", e);
			}
		}
		return categoriesList;
	}

	public AutoSyncScheduleItemDetails getEngineTypeItemsCount(String market,String catType, Connection conn, List<MarketLocaleMasterDataTypeMapping> localesList)
	{
		AutoSyncScheduleItemDetails itemDetails = new AutoSyncScheduleItemDetails();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			// for ITEM DETAILS = SET MARKET IN LOCALE VARIABLE
			itemDetails.setLocale(market);
			itemDetails.setItemKey(catType);
			itemDetails.setItemName(AutoSyncConstants.ITEM_LABEL_ENGINE_TYPE);
			
			String sql="";
			MarketLocaleMasterDataTypeMapping localeMapping =null;
			String processingLocale=null;
			String alternateLocale = null;
			int count=0;
			for(int a=0;a<localesList.size();a++)
			{
				localeMapping=  (MarketLocaleMasterDataTypeMapping)localesList.get(a);
				if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
				{
					// LOCALE BELONGS TO SPECIFIC MARKET & FOR TRANSMISSION_TYPE
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
					sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_engine_type A,"
							+ " gms3_mdm_manual_language B WHERE A.MDM_ET_GROUP_TYPE IS NOT NULL AND  A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND "
							+ " B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
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
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql = null;
					
					if(checkForAlternateLocale==true)
					{
						// CHECK FOR ALTERNATE LOCALE
						sql="SELECT COUNT(*) AS COUNT FROM gms3_mdm_engine_type A,"
								+ " gms3_mdm_manual_language B WHERE A.MDM_ET_GROUP_TYPE IS NOT NULL AND  A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND "
								+ " B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							count = count + rs.getInt("COUNT");
						}
						rs.close();rs=null;
						if(null!=stmt)
						{
							stmt.close();stmt=null;
						}
						sql  =null;
					}
				
				}
				localeMapping =null;
				processingLocale=null;
				alternateLocale = null;
				sql = null;
			}
			localeMapping =null;
			processingLocale=null;
			alternateLocale = null;
			sql = null;
			
			/*
			 * CALCULATE COUNT 
			 */
			itemDetails.setTotalCount(count);
			count = 0;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getEngineTypeItemsCount()", e);
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
				Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getEngineTypeItemsCount()", e);
			}
		}
		return itemDetails;
	}
	
	public ArrayList<AutoSyncCategoryDetails> getEngineTypeItems(String market,String catType, Connection conn, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			String sql="";
			String processingLocale=null;
			String alternateLocale = null;
			AutoSyncCategoryDetails cat = null;
			AutoSyncCategoryDetails levelDetails = null;
			String groupName="";
			String etCode="";
			String etName="";

			if(null!=localeMapping.getMasterDataType() && localeMapping.getMasterDataType().equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
			{
				// LOCALE BELONGS TO SPECIFIC MARKET & FOR TRANSMISSION_TYPE
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
				sql="SELECT A.MDM_ET_ID,A.MDM_ET_GROUP_TYPE,A.MDM_ET_TYPE_CODE,A.MDM_ET_TYPE_NAME FROM gms3_mdm_engine_type A,"
						+ " gms3_mdm_manual_language B WHERE A.MDM_ET_GROUP_TYPE IS NOT NULL AND A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+processingLocale.replace("_", "-")+"' AND "
						+ " B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					// data Found for processingLocale
					checkForAlternateLocale = false;

					if(null!=rs.getString("MDM_ET_GROUP_TYPE") && !"".equals(rs.getString("MDM_ET_GROUP_TYPE")))
					{
						groupName=rs.getString("MDM_ET_GROUP_TYPE").trim();
					}
					
					etCode = rs.getString("MDM_ET_TYPE_CODE").trim();
					etName= rs.getString("MDM_ET_TYPE_NAME").trim();

					if(null!=groupName && !"".equals(groupName))
					{
						// add LEVEL 1
						cat = new AutoSyncCategoryDetails();
						cat.setMdmItemId(String.valueOf(rs.getLong("MDM_ET_ID")));
						levelDetails= new AutoSyncCategoryDetails();
						
						// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
						levelDetails.setLocale(processingLocale.replace("-", "_"));
						levelDetails.setCategoryName(groupName);
						groupName = groupName.replace("(", "");
						groupName = groupName.replace(")", "");
						levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(groupName));
						levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_ENGINE_TYPE"));
						cat.setLevel1Details(new AutoSyncCategoryDetails());
						cat.setLevel1Details(levelDetails);
						levelDetails=null;

						// add LEVEL 2
						if(null!=etCode && !"".equals(etCode) && null!=etName && !"".equals(etName))
						{
							levelDetails = new AutoSyncCategoryDetails();
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(etName);
							levelDetails.setCategoryRefKey(etCode);
							levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(groupName));
							cat.setLevel2Details(new AutoSyncCategoryDetails());
							cat.setLevel2Details(levelDetails);
							levelDetails=  null;
						}
						categoriesList.add(cat);
						cat = null;
					}
					groupName = null;
					etCode=  null;
					etName =  null;
				}
				rs.close();rs=null;
				if(null!=stmt)
				{
					stmt.close();stmt=null;
				}
				sql = null;
				
				if(checkForAlternateLocale==true)
				{
					// CHECK FOR ALTERNATE LOCALE
					
					sql="SELECT A.MDM_ET_ID,A.MDM_ET_GROUP_TYPE,A.MDM_ET_TYPE_CODE,A.MDM_ET_TYPE_NAME FROM gms3_mdm_engine_type A,"
							+ " gms3_mdm_manual_language B WHERE A.MDM_ET_GROUP_TYPE IS NOT NULL AND A.MDM_ML_ID = B.MDM_ML_ID AND B.MDM_ML_LANG_CODE='"+alternateLocale.replace("_", "-")+"' AND "
							+ " B.MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND A.MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_NO+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						if(null!=rs.getString("MDM_ET_GROUP_TYPE") && !"".equals(rs.getString("MDM_ET_GROUP_TYPE")))
						{
							groupName=rs.getString("MDM_ET_GROUP_TYPE").trim();
						}
						etCode = rs.getString("MDM_ET_TYPE_CODE").trim();
						etName= rs.getString("MDM_ET_TYPE_NAME").trim();

						if(null!=groupName && !"".equals(groupName))
						{
							// add LEVEL 1
							cat = new AutoSyncCategoryDetails();
							cat.setMdmItemId(String.valueOf(rs.getLong("MDM_ET_ID")));
							levelDetails= new AutoSyncCategoryDetails();
							
							// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
							levelDetails.setLocale(processingLocale.replace("-", "_"));
							levelDetails.setCategoryName(groupName);
							groupName = groupName.replace("(", "");
							groupName = groupName.replace(")", "");
							levelDetails.setCategoryRefKey(Utilities.replaceCharsForRefKeys(groupName));
							levelDetails.setParentRefKey(ApplicationProperties.getProperty("PARENT_REF_KEYS_ENGINE_TYPE"));
							cat.setLevel1Details(new AutoSyncCategoryDetails());
							cat.setLevel1Details(levelDetails);
							levelDetails=null;

							// add LEVEL 2
							if(null!=etCode && !"".equals(etCode) && null!=etName && !"".equals(etName))
							{
								levelDetails = new AutoSyncCategoryDetails();
								// HERE LOCALE WILL ALWAYS BE PROCESSING LOCALE
								levelDetails.setLocale(processingLocale.replace("-", "_"));
								levelDetails.setCategoryName(etName);
								levelDetails.setCategoryRefKey(etCode);
								levelDetails.setParentRefKey(Utilities.replaceCharsForRefKeys(groupName));
								cat.setLevel2Details(new AutoSyncCategoryDetails());
								cat.setLevel2Details(levelDetails);
								levelDetails=  null;
							}
							categoriesList.add(cat);
							cat = null;
						}
						groupName = null;
						etCode=  null;
						etName =  null;
					}
					rs.close();rs=null;
					if(null!=stmt)
					{
						stmt.close();stmt=null;
					}
					sql = null;
				}
			
			}
			processingLocale=null;
			alternateLocale = null;
			cat = null;
			levelDetails = null;
			groupName=null;
			etCode=null;
			etName=null;
			sql = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getEngineTypeItems()", e);
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
				Utilities.printStackTraceToLogs(EngineMissionTypesDataDAO.class.getName(), "getEngineTypeItems()", e);
			}
		}
		return categoriesList;
	}

}
