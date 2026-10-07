package com.mazda.gms3.dmt.autosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class MasterDataSyncDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(MasterDataSyncDAO.class);  
	
	public ArrayList<SelectItemDetails> getCountryList() throws SQLException 
	{
		ArrayList<SelectItemDetails> countryLocaleList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT MDM_CL_ID,MDM_CL_LOCALE_CODE,MDM_CL_LOCALE_DESC  "
					+ "FROM gms3_mdm_country_locale WHERE "
					+ "MDM_CL_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"' ORDER BY MDM_CL_LOCALE_DESC ASC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SelectItemDetails details = new SelectItemDetails();
				details.setValue(String.valueOf(rs.getLong("MDM_CL_ID")));
				if(null!=rs.getString("MDM_CL_LOCALE_CODE"))
				{
					details.setCode(rs.getString("MDM_CL_LOCALE_CODE").trim());
				}
				if(null!=rs.getString("MDM_CL_LOCALE_DESC"))
				{
					details.setHeading(rs.getString("MDM_CL_LOCALE_DESC").trim());
					details.setLabel(rs.getString("MDM_CL_LOCALE_DESC").trim());
				}
				
				if(null!=details.getCode() && !"".equals(details.getCode()))
				{
					if(null!=details.getLabel() && !"".equals(details.getLabel()))
					{
						details.setLabel(details.getLabel()+" ("+details.getCode()+")");
					}
					else
					{
						details.setLabel("("+details.getCode()+")");
					}
				}
				countryLocaleList.add(details);
				details=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getCountryList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
		return countryLocaleList;
	}

	public ArrayList<SelectItemDetails> getLanguageList(String[] countryIds) throws SQLException 
	{
		//		logger.info("getLanguageList :: Method Starts.");
		ArrayList<SelectItemDetails> languageList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT MDM_ML_ID,MDM_ML_LANG_CODE,MDM_ML_LANG_DESC FROM gms3_mdm_manual_language WHERE "
					+ "MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
			if(null!=countryIds && countryIds.length>0)
			{
				String countryValues="";
				for(int a=0;a<countryIds.length;a++)
				{
					countryValues+= countryIds[a];
					if(a!=countryIds.length-1)
					{
						countryValues+=",";
					}
				}
				sql = sql+" AND MDM_CL_ID IN ("+countryValues+")";
				countryValues = null;
			}
			
			sql=sql+ " ORDER BY MDM_ML_LANG_DESC ASC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SelectItemDetails details = new SelectItemDetails();
				details.setValue(String.valueOf(rs.getLong("MDM_ML_ID")));
				if(null!=rs.getString("MDM_ML_LANG_DESC"))
				{
					details.setHeading(rs.getString("MDM_ML_LANG_DESC").trim());
					details.setLabel(rs.getString("MDM_ML_LANG_DESC").trim());
				}
				if(null!=rs.getString("MDM_ML_LANG_CODE"))
				{
					details.setCode(rs.getString("MDM_ML_LANG_CODE").trim());
				}
				if(null!=details.getCode() && !"".equals(details.getCode()))
				{
					if(null!=details.getLabel() && !"".equals(details.getLabel()))
					{
						details.setLabel(details.getLabel()+" ("+details.getCode()+")");
					}
					else
					{
						details.setLabel("("+details.getCode()+")");
					}
				}
				
				languageList.add(details);
				details=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getLanguageList()", e);
			languageList = null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			countryIds  =null;
		}
		return languageList;
	}
	
	public ArrayList<AutoSyncScheduleItemDetails> getItemsCountForProcessing(String[] selCats, String market)
	{
		ArrayList<AutoSyncScheduleItemDetails> itemsList = null;
		Connection conn  =null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=selCats && selCats.length>0 && null!=market && !"".equals(market))
			{
				conn = getConnection();
				/*
				 * FETCH LOCALES BASED MATRIX ON THE BASIS OF MARKET
				 */
				List<MarketLocaleMasterDataTypeMapping> localesList = new ArrayList<MarketLocaleMasterDataTypeMapping>();
				String sql = "SELECT * FROM gms3_mdm_alt_lang_mapping WHERE MDM_MARKET='"+market+"' "
						+ " AND MDM_ALT_MAPPING_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				MarketLocaleMasterDataTypeMapping mapping = null;
				while(rs.next())
				{
					mapping = new MarketLocaleMasterDataTypeMapping();
					mapping.setMarket(rs.getString("MDM_MARKET"));
					mapping.setLocale(rs.getString("MDM_LOCALE"));
					mapping.setAlternateLocale(rs.getString("MDM_ALTERNATE_LOCALE"));
					mapping.setMasterDataType(rs.getString("MDM_MASTER_DATA_TYPE"));
					localesList.add(mapping);
					mapping=  null;
				}
				mapping=  null;
				rs.close();rs = null;
				stmt.close();stmt=null;
				sql = null;
				
				if(null!=localesList && localesList.size()>0)
				{
					ModelYearDataDAO modelYearDAO = new ModelYearDataDAO();
					VINRangeDataDAO vinRangeDAO = new VINRangeDataDAO();
					CarlineDataDAO carlineDataDAO = new CarlineDataDAO();
					VehicleTypeDataDAO vehicleTypeDataDAO= new  VehicleTypeDataDAO();
					EngineMissionTypesDataDAO engineMissionTypeDataDAO = new EngineMissionTypesDataDAO();
					ManualTypeDataDAO manualTypeDataDAO = new ManualTypeDataDAO();
					ESICategoryDataDAO esiCateogryDAO  =new ESICategoryDataDAO();
					CVCCategoryDataDAO cvcCategoryDAO = new CVCCategoryDataDAO();
					
					itemsList = new ArrayList<AutoSyncScheduleItemDetails>();
					for(int a=0;a<selCats.length;a++)
					{
						if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
						{
							/*
							 * IF MNAO MARKET- THE ONLY PROCEED
							 */
							if(market.equals(ApplicationProperties.getProperty("market.mnao")))
							{
								itemsList.add(modelYearDAO.getModelYearItemsCount(market, selCats[a], conn, localesList));
							}
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
						{
							/*
							 * IF MNAO MARKET- THE ONLY PROCEED
							 */
							if(market.equals(ApplicationProperties.getProperty("market.mnao")))
							{	
								itemsList.add(vinRangeDAO.getVINRangeItemsCount(market, selCats[a], conn, localesList));
							}
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_CARLINE))
						{
							/*
							 * IF MC / MME MARKET - THEN ONLY PROCEED
							 */
							if(market.equals(ApplicationProperties.getProperty("market.mc")) || market.equals(ApplicationProperties.getProperty("market.mme")))
							{
								itemsList.add(carlineDataDAO.getCarlineItemsCount(market, selCats[a], conn, localesList));
							}
						}
						/*
						 * NOW COMMON CATEGORIES FOR ALL MARKETS
						 */
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
						{
							itemsList.add(vehicleTypeDataDAO.getAxleTypeItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
						{
							itemsList.add(vehicleTypeDataDAO.getBodyTypeItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
						{
							itemsList.add(engineMissionTypeDataDAO.getEngineTypeItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
						{
							itemsList.add(engineMissionTypeDataDAO.getTransmissionTypeItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE))
						{
							itemsList.add(manualTypeDataDAO.getManualTypeItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
						{
							itemsList.add(esiCateogryDAO.getESICategoryItemsCount(market, selCats[a], conn, localesList));
						}
						else if(selCats[a].equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
						{
							itemsList.add(cvcCategoryDAO.getCVCCategoryItemsCount(market, selCats[a], conn,localesList));
						}
					}
					modelYearDAO=  null;
					vinRangeDAO = null;
					carlineDataDAO=  null;
					vehicleTypeDataDAO = null;
					engineMissionTypeDataDAO=  null;
					manualTypeDataDAO=  null;
					esiCateogryDAO=  null;
					cvcCategoryDAO = null;
							
				}
				else
				{
					logger.info("getItemsCountForProcessing :: No Locales Definition Found for "+market+" Market. No Items could be fetched.");
				}
				localesList=  null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getItemsCountForProcessing()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
				market=  null;
				selCats = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getItemsCountForProcessing()", e);
			}
		}
		return itemsList;
	}
	
	
	
	
	public ArrayList<AutoSyncCategoryDetails> getCategoriesListForProcessing(String selCats, String market, MarketLocaleMasterDataTypeMapping localeMapping)
	{
		Connection conn  =null;
		Statement stmt = null;
		ResultSet rs = null;
		ArrayList<AutoSyncCategoryDetails> categoriesList = new ArrayList<AutoSyncCategoryDetails>();
		try
		{
			if(null!=selCats && !"".equals(selCats) && null!=market && !"".equals(market))
			{
				conn = getConnection();

				if(selCats.equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
				{
					ModelYearDataDAO modelYearDAO = new ModelYearDataDAO();
					/*
					 * IF MNAO MARKET- THE ONLY PROCEED
					 */
					if(market.equals(ApplicationProperties.getProperty("market.mnao")))
					{
						categoriesList = modelYearDAO.getModelYearItems(market, selCats, conn, localeMapping);
					}
					modelYearDAO  = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
				{
					/*
					 * IF MNAO MARKET- THE ONLY PROCEED
					 */
					if(market.equals(ApplicationProperties.getProperty("market.mnao")))
					{	
						VINRangeDataDAO vinRangeDAO = new VINRangeDataDAO();
						categoriesList = vinRangeDAO.getVINRangeItems(market, selCats, conn, localeMapping);
						vinRangeDAO = null;
					}
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_CARLINE))
				{
					/*
					 * IF MC / MME MARKET - THEN ONLY PROCEED
					 */
					if(market.equals(ApplicationProperties.getProperty("market.mc")) || market.equals(ApplicationProperties.getProperty("market.mme")))
					{
						CarlineDataDAO carlineDAO  = new CarlineDataDAO();
						categoriesList = carlineDAO.getCarlineItems(market, selCats, conn, localeMapping);
						carlineDAO  =null;
					}
				}
				/*
				 * NOW COMMON CATEGORIES FOR ALL MARKETS
				 */
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
				{
					VehicleTypeDataDAO vehicleTypeDAO = new VehicleTypeDataDAO();
					categoriesList = vehicleTypeDAO.getAxleTypeItems(market, selCats, conn, localeMapping);
					vehicleTypeDAO = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
				{
					VehicleTypeDataDAO  vehicleTypeDAO  = new VehicleTypeDataDAO();
					categoriesList = vehicleTypeDAO.getBodyTypeItems(market, selCats, conn, localeMapping);
					vehicleTypeDAO = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
				{
					EngineMissionTypesDataDAO engMissionTypeDAO  =new EngineMissionTypesDataDAO();
					categoriesList = engMissionTypeDAO.getEngineTypeItems(market, selCats, conn, localeMapping);
					engMissionTypeDAO  =null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
				{
					EngineMissionTypesDataDAO engMissionTypeDAO  =new EngineMissionTypesDataDAO();
					categoriesList = engMissionTypeDAO.getTransmissionTypeItems(market, selCats, conn, localeMapping);
					engMissionTypeDAO = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE))
				{
					ManualTypeDataDAO manualTypeDAO  =new ManualTypeDataDAO();
					categoriesList = manualTypeDAO.getManualTypeItems(market, selCats, conn, localeMapping);
					manualTypeDAO = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
				{
					ESICategoryDataDAO esiCategoryDAO =new ESICategoryDataDAO();
					categoriesList = esiCategoryDAO.getESICategoryItems(market, selCats, conn, localeMapping);
					esiCategoryDAO = null;
				}
				else if(selCats.equals(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
				{
					CVCCategoryDataDAO cvcCategoryDAO = new CVCCategoryDataDAO();
					categoriesList = cvcCategoryDAO.getCVCCategoryItems(market, selCats, conn, localeMapping);
					cvcCategoryDAO = null;
				}
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getCategoriesListForProcessing()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
				market=  null;
				selCats = null;
				localeMapping = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getCategoriesListForProcessing()", e);
			}
		}
		return categoriesList;
	}
	
	public AutoSyncScheduleDetails createSchedule(AutoSyncScheduleDetails details) 
	{
		Long scheduleId=null;
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs=null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=details && null!=details.getItemsList() && details.getItemsList().size()>0)
			{
				
				conn = getConnection();
				conn.setAutoCommit(false);
				// save sch name  - get schId
				String sql="INSERT INTO gms3_auto_sync_sch (AS_SCHEDULE_NAME,AS_RECORD_STATUS) VALUES ('"+details.getScheduleName()+"','"+AutoSyncConstants.STATUS_ACTIVE+"')";
				stmt = conn.createStatement();
				String generatedColumns[] = { "AS_SCHEDULE_ID" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				
				if(scheduleId > 0)
				{
					// set in schedule Id
					details.setScheduleId(scheduleId);
					// update scheduleName & threadId
					details.setScheduleName(details.getScheduleName()+String.valueOf(scheduleId));
					details.setThreadId(details.getThreadId()+String.valueOf(scheduleId));
					// update in table
					sql="UPDATE gms3_auto_sync_sch SET AS_SCHEDULE_NAME = ?, AS_USER_ID=?, AS_JOB_STATUS=?,AS_SCH_THREAD_ID=?,"
							+ "AS_SCHEDULE_TMSTP = ?, AS_LOCALE=?,AS_TOTAL_COUNT=? WHERE AS_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, details.getScheduleName());
					pstmt.setString(2, details.getUserId());
					pstmt.setString(3, details.getJobStatus());
					pstmt.setString(4, details.getThreadId());
					pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
					pstmt.setString(6, details.getMarket());
					pstmt.setInt(7, details.getTotalCount());
					pstmt.setLong(8, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=null;
					sql =null;
					/*
					 * NOW INSERT ITEMS LIST
					 */
					AutoSyncScheduleItemDetails itemDetails = null;
					long itemId=0;
					for(int a=0;a<details.getItemsList().size();a++)
					{
						itemDetails = (AutoSyncScheduleItemDetails)details.getItemsList().get(a);
						sql="INSERT INTO gms3_auto_sync_sch_items (AS_SCHEDULE_ID) VALUES ("+details.getScheduleId()+")";
						stmt = conn.createStatement();
						String generatedItemColumns[] = { "AS_ITEM_ID" };
						stmt.execute(sql,generatedItemColumns);
						rs = stmt.getGeneratedKeys();
						if(rs.next())
						{
							itemId = rs.getLong(1);
						}
						rs.close();rs =null;
						stmt.close();stmt = null;
						sql=null;
						generatedItemColumns = null;
						
						if(itemId>0)
						{
							// set in item Id
							itemDetails.setItemId(itemId);
							// set scheduleId
							itemDetails.setScheduleId(scheduleId);
							// update in table
							sql="UPDATE gms3_auto_sync_sch_items SET AS_LOCALE = ?, AS_ITEM_NAME=?, AS_TOTAL_COUNT=?"
									+ " WHERE AS_ITEM_ID=?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1,itemDetails.getItemKey());
							pstmt.setString(2,itemDetails.getItemName());
							pstmt.setInt(3, itemDetails.getTotalCount());
							pstmt.setLong(4, itemId);
							pstmt.executeUpdate();
							pstmt.close();
							pstmt=null;
							sql =null;
						}
						itemId = 0;
					}
					itemDetails = null;
					// commit transaction
					conn.commit();
				}
				else
				{
					logger.info("createSchedule :: Failed to Create Schdeule for Market :: > "+ details.getMarket());
					conn.rollback();
				}
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "createSchedule()", e1);
			}
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "createSchedule()", e);
			details.setScheduleId(0);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "createSchedule()", e);
			}
			
		}
		return details;
	}

	public ArrayList<AutoSyncScheduleDetails> getScheduleDetailsList()
	{
		ArrayList<AutoSyncScheduleDetails> list =new ArrayList<AutoSyncScheduleDetails>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			// add 1 day to current timestamp
			Timestamp currentTime=new Timestamp(new Date().getTime()+1000L*60L*60L*24L*1L);
			// deduct 30 days from current timestamp
			Timestamp beforeTime = new Timestamp(new Date( new Date().getTime() - 1000L*60L*60L*24L*30L).getTime());
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyy");
			currentTime = new Timestamp(sdf.parse(sdf.format(currentTime)).getTime());
			beforeTime = new Timestamp(sdf.parse(sdf.format(beforeTime)).getTime());
			
			conn = getConnection();
			String sql="SELECT * FROM gms3_auto_sync_sch WHERE AS_RECORD_STATUS='"+AutoSyncConstants.STATUS_ACTIVE+"' AND AS_SCHEDULE_TMSTP BETWEEN ? AND ? "
					+ " ORDER BY AS_SCHEDULE_ID DESC";
			stmt=conn.prepareStatement(sql);
			stmt.setTimestamp(1, beforeTime);
			stmt.setTimestamp(2, currentTime);
			rs = stmt.executeQuery();
			AutoSyncScheduleDetails details = null;
			while(rs.next())
			{
				details = new AutoSyncScheduleDetails();
				details.setSrNo(list.size()+1);
				details.setScheduleId(rs.getLong("AS_SCHEDULE_ID"));
				details.setScheduleName(rs.getString("AS_SCHEDULE_NAME"));
				details.setUserId(rs.getString("AS_USER_ID"));
				details.setTotalCount(rs.getInt("AS_TOTAL_COUNT"));
				details.setSuccessCount(rs.getInt("AS_PROCESSED_COUNT"));
				details.setFailureCount(rs.getInt("AS_FAILED_COUNT"));
				details.setThreadId(rs.getString("AS_SCH_THREAD_ID"));
				details.setMarket(rs.getString("AS_LOCALE"));
				details.setJobStatus(rs.getString("AS_JOB_STATUS"));
				details.setScheduleStatus(rs.getString("AS_SCH_STATUS"));
				details.setScheduleTime(rs.getTimestamp("AS_SCHEDULE_TMSTP"));
				details.setFinishTime(rs.getTimestamp("AS_FINISH_TMSTP"));
				list.add(details);
				details = null;
			}
			details=  null;
			sql = null;
			
			currentTime = null;
			beforeTime = null;
			sdf = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleDetailsList()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleDetailsList()", e);
			}
		}
		return list;
	}
	
	public static ArrayList<AutoSyncScheduleItemDetails> getScheduleItemDetails(String scheduleId)
	{
		ArrayList<AutoSyncScheduleItemDetails> itemsList = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_auto_sync_sch_items WHERE AS_SCHEDULE_ID="+scheduleId;
				stmt = conn.createStatement();
				rs=stmt.executeQuery(sql);
				itemsList= new ArrayList<AutoSyncScheduleItemDetails>();
				AutoSyncScheduleItemDetails details = null;
				while(rs.next())
				{
					details = new AutoSyncScheduleItemDetails();
					details.setSrNo(itemsList.size()+1);
					details.setItemKey(rs.getString("AS_LOCALE"));
					details.setItemName(rs.getString("AS_ITEM_NAME"));
					details.setTotalCount(rs.getInt("AS_TOTAL_COUNT"));
					details.setSuccessCount(rs.getInt("AS_PROCESSED_COUNT"));
					details.setFailureCount(rs.getInt("AS_FAILED_COUNT"));
					details.setProcessingStatus(rs.getString("AS_PROCESSING_STATUS"));
					itemsList.add(details);
					details = null;
				}
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleItemDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleItemDetails()", re);
			}
		}
		return itemsList;
	}
	
	public static AutoSyncScheduleDetails getScheduleSpecificDetails(String scheduleId)
	{
		AutoSyncScheduleDetails details = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_auto_sync_sch WHERE AS_RECORD_STATUS='"+AutoSyncConstants.STATUS_ACTIVE+"' AND AS_SCHEDULE_ID="+scheduleId;
				stmt=conn.prepareStatement(sql);
				rs = stmt.executeQuery();
				if(rs.next())
				{
					details = new AutoSyncScheduleDetails();
					details.setScheduleId(rs.getLong("AS_SCHEDULE_ID"));
					details.setScheduleName(rs.getString("AS_SCHEDULE_NAME"));
					details.setUserId(rs.getString("AS_USER_ID"));
					details.setTotalCount(rs.getInt("AS_TOTAL_COUNT"));
					details.setSuccessCount(rs.getInt("AS_PROCESSED_COUNT"));
					details.setFailureCount(rs.getInt("AS_FAILED_COUNT"));
					details.setThreadId(rs.getString("AS_SCH_THREAD_ID"));
					details.setMarket(rs.getString("AS_LOCALE"));
					details.setJobStatus(rs.getString("AS_JOB_STATUS"));
					details.setScheduleStatus(rs.getString("AS_SCH_STATUS"));
					details.setScheduleTime(rs.getTimestamp("AS_SCHEDULE_TMSTP"));
					details.setFinishTime(rs.getTimestamp("AS_FINISH_TMSTP"));
				}
				rs.close();rs = null;
				stmt.close();stmt=null;
				sql = null;

				/*
				 * GET ITEMS DETAILS
				 */
				details.setItemsList(new ArrayList<AutoSyncScheduleItemDetails>());
				sql="SELECT * FROM gms3_auto_sync_sch_items WHERE AS_SCHEDULE_ID="+scheduleId;
				stmt = conn.prepareStatement(sql);
				rs=stmt.executeQuery();
				AutoSyncScheduleItemDetails itemDetails = null;
				while(rs.next())
				{
					itemDetails = new AutoSyncScheduleItemDetails();
					itemDetails.setSrNo(details.getItemsList().size()+1);
					itemDetails.setItemKey(rs.getString("AS_LOCALE"));
					itemDetails.setItemName(rs.getString("AS_ITEM_NAME"));
					itemDetails.setTotalCount(rs.getInt("AS_TOTAL_COUNT"));
					itemDetails.setSuccessCount(rs.getInt("AS_PROCESSED_COUNT"));
					itemDetails.setFailureCount(rs.getInt("AS_FAILED_COUNT"));
					itemDetails.setProcessingStatus(rs.getString("AS_PROCESSING_STATUS"));
					details.getItemsList().add(itemDetails);
					itemDetails = null;
				}
				sql = null;
				rs.close();rs = null;
				stmt.close();stmt = null;
				itemDetails = null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleSpecificDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				scheduleId = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getScheduleSpecificDetails()", e);
			}
		}
		return details;
	}
	
	
	
	public boolean deleteScheduleDetails(String scheduleIds) 
	{
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleIds && !"".equals(scheduleIds))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens=scheduleIds.split(",");
				String sql="";
				if(null!=tokens && tokens.length>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						pstmt = null;
						sql="UPDATE gms3_auto_sync_sch SET AS_RECORD_STATUS='"+AutoSyncConstants.STATUS_INACTIVE+"' "
								+ "   WHERE AS_SCHEDULE_ID = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.executeUpdate();
						pstmt.close();pstmt= null;
						sql = null;
					}
				}
				sql = null;
				// commit the transaction
				conn.commit();
				tokens = null;
			}
			else
			{
				logger.info("deleteScheduleDetails :: Schedule Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "deleteScheduleDetails()", e1);
			}
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "deleteScheduleDetails()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!= pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "deleteScheduleDetails()", e);
			}
			// set passed parameter to null
			scheduleIds=null;
		}
		return true;
	}
	
	public List<MarketLocaleMasterDataTypeMapping> getMasterDataLocaleMappingList(String market, String masterDataType)
	{
		List<MarketLocaleMasterDataTypeMapping> localesList = new ArrayList<MarketLocaleMasterDataTypeMapping>();
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if(null!=market && !"".equals(market) && null!=masterDataType && !"".equals(masterDataType))
			{
				conn = getConnection();
				/*
				 * FETCH LOCALES BASED MATRIX ON THE BASIS OF MARKET
				 */
				String sql = "SELECT * FROM gms3_mdm_alt_lang_mapping WHERE MDM_MARKET='"+market+"' AND MDM_MASTER_DATA_TYPE='"+masterDataType+"' AND MDM_ALT_MAPPING_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				MarketLocaleMasterDataTypeMapping mapping = null;
				while(rs.next())
				{
					mapping = new MarketLocaleMasterDataTypeMapping();
					mapping.setMarket(rs.getString("MDM_MARKET"));
					mapping.setLocale(rs.getString("MDM_LOCALE"));
					mapping.setAlternateLocale(rs.getString("MDM_ALTERNATE_LOCALE"));
					mapping.setMasterDataType(rs.getString("MDM_MASTER_DATA_TYPE"));
					localesList.add(mapping);
					mapping=  null;
				}
				mapping=  null;
				rs.close();rs = null;
				stmt.close();stmt=null;
				sql = null;
			
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getMasterDataLocaleMapping()", e);
		}
		finally
		{
			try
			{
				if(null!= stmt)
					stmt.close();
				if(null!=conn)
					conn.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncDAO.class.getName(), "getMasterDataLocaleMapping()", e);
			}
			// set passed parameter to null
			market=null;
			masterDataType = null;
		}
		return localesList;
	}
}