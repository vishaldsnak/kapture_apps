package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.EngineBookDetails;
import com.mazda.gms3.dmt.vo.ManualTypeDetails;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;
import com.mazda.gms3.dmt.vo.MissionBookDetails;
import com.mazda.gms3.dmt.vo.ReportsSummaryDetails;
import com.mazda.gms3.dmt.vo.ScheduleDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;

public class ScheduleDAO  extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(ScheduleDAO.class);
	
	public static ManualTypeDetails getManualTypeDetailsOnManualCode(String manualTypeCode, String locale) throws SQLException 
	{
		ManualTypeDetails mtDetails = null;
		Connection conn = null;
		Statement stmt=  null;
		ResultSet rs = null;
		try
		{
			if(null!=manualTypeCode && !"".equals(manualTypeCode) && null!=locale && !"".equals(locale))
			{
				locale = locale.replace("_", "-");
				conn = getConnection();
				String sql="";
				/*
				 * FOR ENGINE, CHECK FOR BOTH ENG & ENGINE
				 */
				if(manualTypeCode.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
						manualTypeCode.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) )
				{
					sql = "SELECT * FROM gms3_dmt_conv_manual_type "
							+ " WHERE (TRIM(LOWER(DC_MANUAL_CODE)) ='"+ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()+"' "
									+ " OR TRIM(LOWER(DC_MANUAL_CODE))='"+ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()+"' )"
							+ " AND TRIM(LOWER(MDM_ML_LANG_CODE)) = '"+locale.trim().toLowerCase()+"' AND "
							+ " DC_MANUAL_FLAG NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				}
				else
				{
					sql = "SELECT * FROM gms3_dmt_conv_manual_type "
							+ " WHERE TRIM(LOWER(DC_MANUAL_CODE)) ='"+manualTypeCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(MDM_ML_LANG_CODE)) = '"+locale.trim().toLowerCase()+"' AND "
							+ " DC_MANUAL_FLAG NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				}
				
				logger.info("getManualTypeDetailsOnManualCode :: Sql :: >  " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					mtDetails=  new ManualTypeDetails();
					mtDetails.setManualTypeId(rs.getLong("DC_MANUAL_MT_ID"));
					mtDetails.setManualTypeCode(rs.getString("DC_MANUAL_CODE"));
					mtDetails.setManualTypeName(rs.getString("DC_MANUAL_NAME"));
					mtDetails.setManualTypeRefKey(rs.getString("DC_MANUAL_REF_KEY"));
					mtDetails.setCreationTime(rs.getTimestamp("DC_MANUAL_CREATED_TMSTP"));
					mtDetails.setUpdatedTime(rs.getTimestamp("DC_MANUAL_UPDATED_TMSTP"));
				}
				sql = null;
			}
			else
			{
				logger.info("getManualTypeDetailsOnManualCode :: Manual Type Code as parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getManualTypeDetailsOnManualCode()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			// set manualTypeCode to null
			manualTypeCode = null;
		}
		return mtDetails;
	}
	
	public static EngineBookDetails getEngineBookDetails(String langCode,String engineBookCode) throws SQLException 
	{
		logger.info("getEngineBookDetails :: Method Starts.");
		EngineBookDetails bookDetails = null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode) && null!=engineBookCode && !"".equals(engineBookCode))
			{
				/*
				 * IDENTIFY COUNTRY CODE FROM LANGUAGE,
				 * LANGUAGE CODE WILL BE AS IT IS
				 */
				String countryLocale="";
				if(langCode.indexOf("-")!=-1)
				{
					countryLocale = langCode.substring(langCode.indexOf("-")+1, langCode.length());
				}
				if(null!=countryLocale && !"".equals(countryLocale))
				{

					conn = getConnection();
					String sql = "SELECT A.*, B.MDM_ML_LANG_CODE FROM gms3_mdm_engine_book A, gms3_mdm_manual_language B, gms3_mdm_country_locale C  "
							+ "WHERE A.MDM_ML_ID = B.MDM_ML_ID AND A.MDM_CL_ID=C.MDM_CL_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+langCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(C.MDM_CL_LOCALE_CODE))='"+countryLocale.trim().toLowerCase()+"' "
							+ "AND TRIM(LOWER(A.MDM_EB_CODE))='"+engineBookCode.trim().toLowerCase()+"'";
					logger.info("getEngineBookDetails :: Sql :: > " + sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						bookDetails = new EngineBookDetails();
						bookDetails.setBookId(rs.getLong("MDM_EB_ID"));
						bookDetails.setManualLanguageId(rs.getLong("MDM_ML_ID"));
						bookDetails.setManualLanguageCode(rs.getString("MDM_ML_LANG_CODE"));
						bookDetails.setBookCode(rs.getString("MDM_EB_CODE"));
						bookDetails.setBookNameEng(rs.getString("MDM_EB_NAME_ENG_LANG"));
						bookDetails.setBookNameReg(rs.getString("MDM_EB_NAME_REGIONAL_LANG"));
						bookDetails.setFlag(rs.getString("MDM_EB_FLAG"));
						bookDetails.setEntryTime(rs.getTimestamp("MDM_EB_CREATED_TMSTP"));
						bookDetails.setUpdatedTime(rs.getTimestamp("MDM_EB_UPDATED_TMSTP"));
					}
					sql = null;
				}
				else
				{
					logger.info("getEngineBookDetails :: Language Code and Country Locale Code extracted as Null from Parameter.");
				}
				countryLocale =null;
			}
			else
			{
				logger.info("getEngineBookDetails :: Language Code and Engine Book Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getEngineBookDetails()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
			engineBookCode = null;
		}
		logger.info("getEngineBookDetails :: Method Ends.");
		return bookDetails;
	}

	public static MissionBookDetails getMissionBookDetails(String langCode, String missionBookCode) throws SQLException 
	{
		logger.info("getMissionBookDetails :: Method Starts.");
		MissionBookDetails bookDetails = null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode) && null!=missionBookCode && !"".equals(missionBookCode))
			{
				/*
				 * IDENTIFY COUNTRY CODE FROM LANGUAGE,
				 * LANGUAGE CODE WILL BE AS IT IS
				 */
				String countryLocale="";
				if(langCode.indexOf("-")!=-1)
				{
					countryLocale = langCode.substring(langCode.indexOf("-")+1, langCode.length());
				}
				
				if(null!=countryLocale && !"".equals(countryLocale))
				{
					conn = getConnection();
					String sql = "SELECT A.*, B.MDM_ML_LANG_CODE FROM gms3_mdm_trans_book A, gms3_mdm_manual_language B, gms3_mdm_country_locale C  "
							+ "WHERE A.MDM_ML_ID = B.MDM_ML_ID AND A.MDM_CL_ID=C.MDM_CL_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+langCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(C.MDM_CL_LOCALE_CODE))='"+countryLocale.trim().toLowerCase()+"' "
							+ "AND TRIM(LOWER(A.MDM_TRANSBK_CODE))='"+missionBookCode.trim().toLowerCase()+"'";
					logger.info("getMissionBookDetails :: Sql :: > " + sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						bookDetails = new MissionBookDetails();
						bookDetails.setBookId(rs.getLong("MDM_TRANSBK_ID"));
						bookDetails.setManualLanguageId(rs.getLong("MDM_ML_ID"));
						bookDetails.setManualLanguageCode(rs.getString("MDM_ML_LANG_CODE"));
						bookDetails.setBookCode(rs.getString("MDM_TRANSBK_CODE"));
						bookDetails.setBookNameEng(rs.getString("MDM_TRANSBK_NAME_ENG_LANG"));
						bookDetails.setBookNameReg(rs.getString("MDM_TRANSBK_NAME_REGIONAL_LANG"));
						bookDetails.setFlag(rs.getString("MDM_TRANSBK_FLAG"));
						bookDetails.setEntryTime(rs.getTimestamp("MDM_TRANSBK_CREATED_TMSTP"));
						bookDetails.setUpdatedTime(rs.getTimestamp("MDM_TRANSBK_UPDATED_TMSTP"));
					}
					sql = null;
				}
				else
				{
					logger.info("getMissionBookDetails :: Language Code and Country Locale Code extracted as Null from Parameter.");
				}
				countryLocale = null;
			}
			else
			{
				logger.info("getMissionBookDetails :: Language Code and Mission Book Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getMissionBookDetails()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
			missionBookCode = null;
		}
		logger.info("getMissionBookDetails :: Method Ends.");
		return bookDetails;
	}

	public static Long createSchedule(ScheduleDetails schDetails) throws SQLException 
	{
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			if(null!=schDetails && !"".equals(schDetails) && null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO gms3_dmt_conv_schedule
				 * First insert the SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql="INSERT INTO gms3_dmt_conv_schedule(DC_SCHEDULE_NAME,DC_USER_ID,DC_RECORD_STATUS) VALUES"
						+ "('"+schDetails.getScheduleName()+"','"+schDetails.getUserId()+"','"+AutoSyncConstants.STATUS_ACTIVE+"')";
				logger.info("createSchedule :: Generate Schedule Id :: Sql :: > " + sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "DC_SCHEDULE_ID" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				if(null!=scheduleId && scheduleId>0)
				{
					logger.info("createSchedule :: Procced for Storing details for Schedule Id :: > " + scheduleId);
					/*
					 * add scheduleId to name and update It
					 * add scheduleId to thread and update It
					 */
					schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
					schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(scheduleId));
					String schSql="UPDATE gms3_dmt_conv_schedule SET DC_SCHEDULE_NAME=?,DC_IM_PROCESSING_STATUS=?,"
							+ "DC_STATUS=?,DC_TOTAL_DOCS_COUNT=?,DC_TOTAL_DOCS_COUNT_DEL=?,DC_OKASSETS_COUNT=?,"
							+ "DC_OKASSETS_COUNT_DEL=?,DC_SCH_THREAD_ID=?,DC_SCHEDULE_TMSTP=?, "
							+ " DC_TOTAL_METADOCS_COUNT=?,DC_TOTAL_DISPORD_COUNT=?,DC_TOTAL_CD_COUNT=?,DC_TOTAL_SCM_COUNT =? WHERE DC_SCHEDULE_ID= ?" ;
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setString(2, schDetails.getImDocsProcessingStatus());
					pstmt.setString(3, schDetails.getScheduleStatus());
					pstmt.setLong(4, schDetails.getTotalDocsForProcessing());
					pstmt.setLong(5, schDetails.getTotalDocsForDeletion());
					pstmt.setLong(6, schDetails.getOkAssetsCount());
					pstmt.setLong(7, schDetails.getOkAssetsDeleteCount());
					pstmt.setString(8, schDetails.getThreadId());
					pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(10, schDetails.getTotalMetaDataDocsCount());
					pstmt.setLong(11, schDetails.getTotalDisplayOrderCount());
					pstmt.setLong(12, schDetails.getTotalCDProcessingCount());
					pstmt.setLong(13, schDetails.getTotalSCMVinCount());
					pstmt.setLong(14, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=  null;
					schSql= null;
					
					if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
					{
						for(int i=0;i<schDetails.getItemsList().size();i++)
						{
							ScheduleItemDetails itemDetails = (ScheduleItemDetails)schDetails.getItemsList().get(i);
							String itemSql="INSERT INTO gms3_dmt_conv_sch_criteria(DC_SCHEDULE_ID,DC_MARKET,DC_LOCALE,"
									+ "DC_MODEL,DC_MANUAL_TYPE,DC_TOTAL_DOCS_COUNT,DC_TOTAL_DOCS_COUNT_DEL,DC_OKASSETS_COUNT,"
									+ "DC_OKASSETS_COUNT_DEL,DC_CRITERIA_CREATED_TMSTP, DC_MATERIAL_NAME, "
									+ "DC_METADOCS_COUNT, DC_MODEL_FOLDER_NAME, DC_MODEL_TYPE,DC_CARLINE_CODE,"
									+ "DC_FACELIFT_FOLDER_NAME,DC_TOTAL_DISPORD_COUNT,DC_TOTAL_CD_COUNT,DC_TOTAL_SCM_COUNT ) "
									+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt = null;
							pstmt = conn.prepareStatement(itemSql);
							pstmt.setLong(1, scheduleId);
							pstmt.setString(2, itemDetails.getMarket());
							pstmt.setString(3, itemDetails.getLocale());
							pstmt.setString(4, itemDetails.getModel());
							pstmt.setString(5, itemDetails.getManualType());
							pstmt.setLong(6, itemDetails.getTotalDocsForProcessing());
							pstmt.setLong(7, itemDetails.getTotalDocsForDeletion());
							pstmt.setLong(8, itemDetails.getOkAssetsCount());
							pstmt.setLong(9, itemDetails.getOkAssetsDeleteCount());
							pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(11, itemDetails.getMaterialFolderName());
							pstmt.setLong(12, itemDetails.getMetaDocsCount());
							pstmt.setString(13, itemDetails.getModelFolderName());
							pstmt.setString(14, itemDetails.getModelType());
							pstmt.setString(15, itemDetails.getCarlineCode());
							pstmt.setString(16, itemDetails.getFaceLiftFolderName());
							pstmt.setLong(17, itemDetails.getTotalDisplayOrderCount());
							pstmt.setLong(18, itemDetails.getTotalCDProcessingCount());
							pstmt.setLong(19, itemDetails.getTotalSCMVinCount());
							pstmt.executeUpdate();
							pstmt.close();
							pstmt=  null;
							itemDetails = null;
							itemSql= null;
						}
						
						/*
						 * commit the complete transaction
						 */
						conn.commit();
						logger.info("createSchedule :: Schedule Conversion with Name as {"+schDetails.getScheduleName()+"} Created Successfully.");
					}
				}
				else
				{
					logger.info("createSchedule :: Failed to Generate Schedule Id. Return null.");
				}
			}
			else
			{
				logger.info("createSchedule :: Schedule Details or Items in it are null as parameters, return null.");
			}
		}
		catch(Exception e)
		{
			logger.info("createSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			conn.rollback();
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "createSchedule()", e);
			scheduleId = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			// set schDetails to null
			schDetails = null;
		}
		return scheduleId;
	}
	
	public static boolean updateScheduleStatus(String scheduleId, String status) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId) && null!=status && !"".equals(status))
			{
				String completeStatus="";
				// SET FINIFH TIME AS WELL HERE IF STATUS IS OT PENDING / PROCESSING
				String updateStatusSql="";
				if(status.equals(ApplicationProperties.getProperty("schedule.status.pending.value")) || 
						status.equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
				{
					updateStatusSql = "UPDATE gms3_dmt_conv_schedule SET DC_STATUS=? WHERE DC_SCHEDULE_ID= ?" ;
				}
				else
				{
					completeStatus = "COMPLETED";
					// set finish Time
					updateStatusSql = "UPDATE gms3_dmt_conv_schedule SET DC_STATUS=?, DC_FINISH_TMSTP = ?, DC_CURRENT_PROCESSING_STATUS = ? WHERE DC_SCHEDULE_ID= ?" ;
				}
				
				
				conn = getConnection();
				conn.setAutoCommit(false);
				
				pstmt = conn.prepareStatement(updateStatusSql);
				if(status.equals(ApplicationProperties.getProperty("schedule.status.pending.value")) || 
						status.equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
				{
					pstmt.setString(1, status);
					pstmt.setLong(2, new Long(scheduleId).longValue());
				}
				else
				{
					pstmt.setString(1, status);
					pstmt.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, completeStatus);
					pstmt.setLong(4, new Long(scheduleId).longValue());
					
				}
				int i=pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateStatusSql=  null;
				if(i==1)
				{
					logger.info("updateScheduleStatus :: Status for Schedule Id {"+scheduleId+"} Updated to :: >" + status);
					
					/*
					 * CHECK HERE IF SCHEDULE STATUS IS SUCCESS,
					 * UPDATE ALL ITS ITEMS STATUS TO SUCCESS
					 */
					if(status.equals(ApplicationProperties.getProperty("schedule.status.success.value")))
					{
						String sql="UPDATE gms3_dmt_conv_sch_criteria SET DC_CURRENT_PROCESSING_STATUS='SUCCESS' WHERE DC_SCHEDULE_ID="+scheduleId;
						Statement stmt = conn.createStatement();
						stmt.executeUpdate(sql);
						stmt.close();stmt=null;
						sql=null;
					}
					
					// commit transaction
					conn.commit();
				}
				else
				{
					logger.info("updateScheduleStatus :: Failed to Update {"+status+"} Status for Schedule Id {"+scheduleId+"}.");
					return false;
				}
			}
			else
			{
				logger.info("updateScheduleStatus :: Schedule Id / Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
				}
			if(null!=pstmt)
				try {
					pstmt.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
				}
			// set params to null
			scheduleId = null;
			status = null;
		}
		return true;
	}


	public static boolean updateScheduleAllItemStatus(String scheduleId, String status) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId) && null!=status && !"".equals(status))
			{
				if(status.equals(ApplicationProperties.getProperty("schedule.status.success.value")))
				{
					status="SUCCESS";
				}
				else if(status.equals(ApplicationProperties.getProperty("schedule.status.failure.value")))
				{
					status = "FAILURE";
				}
				
				conn = getConnection();
				String sql="UPDATE gms3_dmt_conv_sch_criteria SET DC_CURRENT_PROCESSING_STATUS='"+status.toUpperCase().trim()+"' WHERE DC_SCHEDULE_ID="+scheduleId;
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql=null;
			}
			else
			{
				logger.info("updateScheduleAllItemStatus :: Schedule Id / Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleAllItemStatus()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleAllItemStatus()", e);
			}
			// set params to null
			scheduleId = null;
			status = null;
		}
		return true;
	}

	public static boolean updateScheduleItemStatus(String itemId, String status) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId) && null!=status && !"".equals(status))
			{
				conn = getConnection();

				String sql="UPDATE gms3_dmt_conv_sch_criteria SET DC_CURRENT_PROCESSING_STATUS='"+status.toUpperCase().trim()+"' WHERE DC_CRITERIA_ID="+itemId;
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql=null;
			}
			else
			{
				logger.info("updateScheduleItemStatus :: Item Id / Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleItemStatus()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleItemStatus()", e);
			}
			// set params to null
			itemId = null;
			status = null;
		}
		return true;
	}

	
	public static ArrayList<ScheduleDetails> getScheduleDetails(String fromDate, String toDate) throws SQLException 
	{
		ArrayList<ScheduleDetails> schedueList = new ArrayList<ScheduleDetails>();
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT A.*, (SELECT DISTINCT B.DC_MARKET FROM "
					+ " gms3_dmt_conv_sch_criteria B WHERE B.DC_SCHEDULE_ID = A.DC_SCHEDULE_ID) AS MARKET "
					+ " FROM gms3_dmt_conv_schedule A WHERE A.DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_ACTIVE+"' ";
			boolean byDate = null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate);
			if(byDate)
			{
				/*
				 * compared as timestamps through the driver - the same conversion the schedule time is
				 * written and read with. DATE_FORMAT on the stored value compared the database's clock
				 * with the application's date: a job scheduled late in the day fell on the next date
				 * and was missing from History until the next day.
				 */
				sql = sql+" AND A.DC_SCHEDULE_TMSTP >= ? AND A.DC_SCHEDULE_TMSTP < ? ";
			}
			sql+=" ORDER BY A.DC_SCHEDULE_ID DESC";
			PreparedStatement ps = conn.prepareStatement(sql);
			stmt = ps;
			if(byDate)
			{
				SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
				day.setLenient(false);
				java.util.Calendar to = java.util.Calendar.getInstance();
				to.setTime(day.parse(toDate.trim()));
				to.add(java.util.Calendar.DATE, 1);
				ps.setTimestamp(1, new Timestamp(day.parse(fromDate.trim()).getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTimeInMillis()));
			}
			rs = ps.executeQuery();
			while(rs.next())
			{
				ScheduleDetails schDetails = new ScheduleDetails();
				schDetails.setSrNo(schedueList.size()+1);
				schDetails.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
				schDetails.setScheduleName(rs.getString("DC_SCHEDULE_NAME"));
				schDetails.setUserId(rs.getString("DC_USER_ID"));
				schDetails.setImDocsProcessingStatus(rs.getString("DC_IM_PROCESSING_STATUS"));
				schDetails.setTotalDocsForProcessing(rs.getLong("DC_TOTAL_DOCS_COUNT"));
				schDetails.setTotalDocsForDeletion(rs.getLong("DC_TOTAL_DOCS_COUNT_DEL"));
				schDetails.setOkAssetsCount(rs.getLong("DC_OKASSETS_COUNT"));
				schDetails.setOkAssetsDeleteCount(rs.getLong("DC_OKASSETS_COUNT_DEL"));
				schDetails.setProcessedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT"));
				schDetails.setDeletedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL"));
				schDetails.setScheduleStatus(rs.getString("DC_STATUS"));
				schDetails.setScheduleTime(rs.getTimestamp("DC_SCHEDULE_TMSTP"));
				schDetails.setFinishTime(rs.getTimestamp("DC_FINISH_TMSTP"));
				schDetails.setThreadId(rs.getString("DC_SCH_THREAD_ID"));
				schDetails.setTotalMetaDataDocsCount(rs.getLong("DC_TOTAL_METADOCS_COUNT"));
				schDetails.setProcessedMetaDataDocsCount(rs.getLong("DC_PROCESSED_METADOCS_COUNT"));
				schDetails.setProcessedOkAssetsCount(rs.getLong("DC_PROCESSED_OKAST_COUNT"));
				schDetails.setProcessedOkAssetsDeleteCount(rs.getLong("DC_PROCESSED_OKAST_COUNT_DEL"));
				schDetails.setTotalInnerLinksCount(rs.getLong("DC_TOTAL_INRLKS_COUNT"));
				schDetails.setProcessedInnerLinksCount(rs.getLong("DC_PROCESSED_INRLKS_COUNT"));
				schDetails.setFailedProcessedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT"));
				schDetails.setFailedDeletedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT_DEL"));
				schDetails.setFailedOkAssetsCount(rs.getLong("DC_FAILED_OKAST_COUNT"));
				schDetails.setFailedOkAssetsDeleteCount(rs.getLong("DC_FAILED_OKAST_COUNT_DEL"));
				schDetails.setFailedInnerLinksCount(rs.getLong("DC_FAILED_INRLKS_COUNT"));
				schDetails.setCurrentProcessingStatus(rs.getString("DC_CURRENT_PROCESSING_STATUS"));
				schDetails.setMarketName(rs.getString("MARKET"));
				schDetails.setTotalDisplayOrderCount(rs.getLong("DC_TOTAL_DISPORD_COUNT"));
				schDetails.setProcessedDisplayOrderCount(rs.getLong("DC_PROCESSED_DISPORD_COUNT"));
				schDetails.setFailedDisplayOrderCount(rs.getLong("DC_FAILED_DISPORD_COUNT"));
				
				schDetails.setTotalCDProcessingCount(rs.getLong("DC_TOTAL_CD_COUNT"));
				schDetails.setProcessedCDProcessingCount(rs.getLong("DC_PROCESSED_CD_COUNT"));
				schDetails.setFailedCDProcessingCount(rs.getLong("DC_FAILED_CD_COUNT"));
				
				schDetails.setTotalSCMVinCount(rs.getLong("DC_TOTAL_SCM_COUNT"));
				schDetails.setProcessedSCMVinCount(rs.getLong("DC_PROCESSED_SCM_COUNT"));
				schDetails.setFailedSCMVinCount(rs.getLong("DC_FAILED_SCM_COUNT"));
				
				/*
				 *  automation.sch.key=MGSS_IPM_, then type is Automation
				 * else Data Load
				 */
				// Set Default
				schDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_DATALOAD);
				if(null!=schDetails.getThreadId() && !"".equals(schDetails.getThreadId()))
				{
					if(schDetails.getThreadId().startsWith(ApplicationProperties.getProperty("automation.sch.key")))
					{
						// set Type as Automation
						schDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_AUTOMATION);
					}
				}
				schedueList.add(schDetails);
				schDetails =null;
			}
			rs.close();rs =null;
			stmt.close();stmt = null;
			sql = null;
			
			if(null!=schedueList && schedueList.size()>0)
			{
				// IDENTIFY THE MARKET FOR EACH SCHEDULE 
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getScheduleDetails()", e);
			// set scheduleList to null
			schedueList =null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return schedueList;
	}

	public static ScheduleDetails getScheduleSpecificDetails(String scheduleCode) throws SQLException 
	{
		ScheduleDetails schDetails = null;
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{

				conn = getConnection();
				String sql="SELECT A.*, (SELECT DISTINCT B.DC_MARKET FROM "
					+ " gms3_dmt_conv_sch_criteria B WHERE B.DC_SCHEDULE_ID = A.DC_SCHEDULE_ID) AS MARKET"
					+ " FROM gms3_dmt_conv_schedule A WHERE A.DC_SCHEDULE_ID ="+new Long(scheduleCode).longValue();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					schDetails = new ScheduleDetails();
					schDetails.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
					schDetails.setScheduleName(rs.getString("DC_SCHEDULE_NAME"));
					schDetails.setUserId(rs.getString("DC_USER_ID"));
					schDetails.setImDocsProcessingStatus(rs.getString("DC_IM_PROCESSING_STATUS"));
					schDetails.setTotalDocsForProcessing(rs.getLong("DC_TOTAL_DOCS_COUNT"));
					schDetails.setTotalDocsForDeletion(rs.getLong("DC_TOTAL_DOCS_COUNT_DEL"));
					schDetails.setOkAssetsCount(rs.getLong("DC_OKASSETS_COUNT"));
					schDetails.setOkAssetsDeleteCount(rs.getLong("DC_OKASSETS_COUNT_DEL"));
					schDetails.setProcessedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT"));
					schDetails.setDeletedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL"));
					schDetails.setScheduleStatus(rs.getString("DC_STATUS"));
					schDetails.setScheduleTime(rs.getTimestamp("DC_SCHEDULE_TMSTP"));
					schDetails.setFinishTime(rs.getTimestamp("DC_FINISH_TMSTP"));
					schDetails.setThreadId(rs.getString("DC_SCH_THREAD_ID"));
					schDetails.setTotalMetaDataDocsCount(rs.getLong("DC_TOTAL_METADOCS_COUNT"));
					schDetails.setProcessedMetaDataDocsCount(rs.getLong("DC_PROCESSED_METADOCS_COUNT"));
					schDetails.setProcessedOkAssetsCount(rs.getLong("DC_PROCESSED_OKAST_COUNT"));
					schDetails.setProcessedOkAssetsDeleteCount(rs.getLong("DC_PROCESSED_OKAST_COUNT_DEL"));
					schDetails.setTotalInnerLinksCount(rs.getLong("DC_TOTAL_INRLKS_COUNT"));
					schDetails.setProcessedInnerLinksCount(rs.getLong("DC_PROCESSED_INRLKS_COUNT"));
					schDetails.setFailedProcessedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT"));
					schDetails.setFailedDeletedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT_DEL"));
					schDetails.setFailedOkAssetsCount(rs.getLong("DC_FAILED_OKAST_COUNT"));
					schDetails.setFailedOkAssetsDeleteCount(rs.getLong("DC_FAILED_OKAST_COUNT_DEL"));
					schDetails.setFailedInnerLinksCount(rs.getLong("DC_FAILED_INRLKS_COUNT"));
					schDetails.setCurrentProcessingStatus(rs.getString("DC_CURRENT_PROCESSING_STATUS"));
					schDetails.setMarketName(rs.getString("MARKET"));
					schDetails.setTotalDisplayOrderCount(rs.getLong("DC_TOTAL_DISPORD_COUNT"));
					schDetails.setProcessedDisplayOrderCount(rs.getLong("DC_PROCESSED_DISPORD_COUNT"));
					schDetails.setFailedDisplayOrderCount(rs.getLong("DC_FAILED_DISPORD_COUNT"));
					
					schDetails.setTotalCDProcessingCount(rs.getLong("DC_TOTAL_CD_COUNT"));
					schDetails.setProcessedCDProcessingCount(rs.getLong("DC_PROCESSED_CD_COUNT"));
					schDetails.setFailedCDProcessingCount(rs.getLong("DC_FAILED_CD_COUNT"));
					
					schDetails.setTotalSCMVinCount(rs.getLong("DC_TOTAL_SCM_COUNT"));
					schDetails.setProcessedSCMVinCount(rs.getLong("DC_PROCESSED_SCM_COUNT"));
					schDetails.setFailedSCMVinCount(rs.getLong("DC_FAILED_SCM_COUNT"));
					
					/*
					 *  automation.sch.key=MGSS_IPM_, then type is Automation
					 * else Data Load
					 */
					// Set Default
					schDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_DATALOAD);
					if(null!=schDetails.getThreadId() && !"".equals(schDetails.getThreadId()))
					{
						if(schDetails.getThreadId().startsWith(ApplicationProperties.getProperty("automation.sch.key")))
						{
							// set Type as Automation
							schDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_AUTOMATION);
						}
					}
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql = null;
			}
			else
			{
				logger.info("getScheduleSpecificDetails :: Schedule Code as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getScheduleSpecificDetails()", e);
			schDetails = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return schDetails;
	}
	
	public static ArrayList<ScheduleItemDetails> getScheduleItemDetails(String scheduleId) throws SQLException
	{
		ArrayList<ScheduleItemDetails> itemsList = new ArrayList<ScheduleItemDetails>();
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="SELECT A.*, B.DC_SCH_THREAD_ID FROM gms3_dmt_conv_sch_criteria A, gms3_dmt_conv_schedule B  WHERE  A.DC_SCHEDULE_ID = B.DC_SCHEDULE_ID AND "
						+ " A.DC_SCHEDULE_ID ="+new Long(scheduleId).longValue();
				logger.info("getScheduleItemDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					ScheduleItemDetails schItemDetails =new ScheduleItemDetails();
					schItemDetails.setSrNo(itemsList.size()+1);
					schItemDetails.setItemId(rs.getLong("DC_CRITERIA_ID"));
					schItemDetails.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
					schItemDetails.setMarket(rs.getString("DC_MARKET"));
					schItemDetails.setLocale(rs.getString("DC_LOCALE"));
					schItemDetails.setModel(rs.getString("DC_MODEL"));
					schItemDetails.setModelFolderName(rs.getString("DC_MODEL_FOLDER_NAME"));
					schItemDetails.setManualType(rs.getString("DC_MANUAL_TYPE"));
					schItemDetails.setMaterialFolderName(rs.getString("DC_MATERIAL_NAME"));
					schItemDetails.setTotalDocsForProcessing(rs.getLong("DC_TOTAL_DOCS_COUNT"));
					schItemDetails.setTotalDocsForDeletion(rs.getLong("DC_TOTAL_DOCS_COUNT_DEL"));
					schItemDetails.setOkAssetsCount(rs.getLong("DC_OKASSETS_COUNT"));
					schItemDetails.setOkAssetsDeleteCount(rs.getLong("DC_OKASSETS_COUNT_DEL"));
					schItemDetails.setMetaDocsCount(rs.getLong("DC_METADOCS_COUNT"));
					schItemDetails.setThreadId(rs.getString("DC_SCH_THREAD_ID"));
					
					schItemDetails.setModelType(rs.getString("DC_MODEL_TYPE"));
					schItemDetails.setCarlineCode(rs.getString("DC_CARLINE_CODE"));
					schItemDetails.setFaceLiftFolderName(rs.getString("DC_FACELIFT_FOLDER_NAME"));
					schItemDetails.setProcessedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT"));
					schItemDetails.setFailedProcessedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT"));
					schItemDetails.setDeletedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL"));
					schItemDetails.setFailedDeletedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT_DEL"));
					schItemDetails.setProcessedOkAssetsCount(rs.getLong("DC_PROCESSED_OKAST_COUNT"));
					schItemDetails.setFailedOkAssetsCount(rs.getLong("DC_FAILED_OKAST_COUNT"));
					schItemDetails.setProcessedOkAssetsDeleteCount(rs.getLong("DC_PROCESSED_OKAST_COUNT_DEL"));
					schItemDetails.setFailedOkAssetsDeleteCount(rs.getLong("DC_FAILED_OKAST_COUNT_DEL"));
					
					schItemDetails.setTotalInnerLinksCount(rs.getLong("DC_TOTAL_INRLKS_COUNT"));
					schItemDetails.setProcessedInnerLinksCount(rs.getLong("DC_PROCESSED_INRLKS_COUNT"));
					schItemDetails.setFailedInnerLinksCount(rs.getLong("DC_FAILED_INRLKS_COUNT"));
					
					schItemDetails.setCurrentProcessingStatus(rs.getString("DC_CURRENT_PROCESSING_STATUS"));
					
					schItemDetails.setTotalDisplayOrderCount(rs.getLong("DC_TOTAL_DISPORD_COUNT"));
					schItemDetails.setProcessedDisplayOrderCount(rs.getLong("DC_PROCESSED_DISPORD_COUNT"));
					schItemDetails.setFailedDisplayOrderCount(rs.getLong("DC_FAILED_DISPORD_COUNT"));
					
					schItemDetails.setTotalCDProcessingCount(rs.getLong("DC_TOTAL_CD_COUNT"));
					schItemDetails.setProcessedCDProcessingCount(rs.getLong("DC_PROCESSED_CD_COUNT"));
					schItemDetails.setFailedCDProcessingCount(rs.getLong("DC_FAILED_CD_COUNT"));
					
					schItemDetails.setTotalSCMVinCount(rs.getLong("DC_TOTAL_SCM_COUNT"));
					schItemDetails.setProcessedSCMVinCount(rs.getLong("DC_PROCESSED_SCM_COUNT"));
					schItemDetails.setFailedSCMVinCount(rs.getLong("DC_FAILED_SCM_COUNT"));
					
					/*
					 * if thread id is not null and thread id starts with 
					 * automation.sch.key=MGSS_IPM_, then type is Automation
					 * else Data Load
					 */
					// Set Default
					schItemDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_DATALOAD);
					if(null!=schItemDetails.getThreadId() && !"".equals(schItemDetails.getThreadId()))
					{
						if(schItemDetails.getThreadId().startsWith(ApplicationProperties.getProperty("automation.sch.key")))
						{
							// set Type as Automation
							schItemDetails.setScheduleType(AutomationConstants.SCHEDULE_TYPE_AUTOMATION);
						}
					}
					itemsList.add(schItemDetails);
					schItemDetails= null;
				}
				rs.close();rs =null;
				stmt.close();stmt=null;
				sql=null;
			}
			else
			{
				logger.info("getScheduleItemDetails :: Schedule Id as parameters are null. Return null");
				itemsList  = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getScheduleItemDetails()", e);
			// set itemLsit to null;
			itemsList = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			// set scheduleId to null
			scheduleId = null;
		}
		return itemsList;
	}
	
	/** The master data Excel files are loaded and sent to Kapture before the content is processed. */
	public static final String LOAD_TYPE_MASTER_DATA_WITH_CONTENT = "MASTER_DATA_WITH_CONTENT";
	/** Only the content is processed. */
	public static final String LOAD_TYPE_ONLY_CONTENT = "ONLY_CONTENT";

	/** Stores what a schedule loads (gms3_dmt_conv_schedule.dc_load_type). */
	public static void saveScheduleLoadType(Long scheduleId, String loadType)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			pstmt = conn.prepareStatement("UPDATE gms3_dmt_conv_schedule SET dc_load_type = ? WHERE dc_schedule_id = ?");
			pstmt.setString(1, LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(loadType) ? LOAD_TYPE_MASTER_DATA_WITH_CONTENT : LOAD_TYPE_ONLY_CONTENT);
			pstmt.setLong(2, scheduleId.longValue());
			pstmt.executeUpdate();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveScheduleLoadType()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveScheduleLoadType()", e);
			}
		}
	}

	/** true when the schedule was created with "Master Data with Content". */
	public static boolean isMasterDataWithContent(String scheduleId)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			pstmt = conn.prepareStatement("SELECT dc_load_type FROM gms3_dmt_conv_schedule WHERE dc_schedule_id = ?");
			pstmt.setLong(1, Long.parseLong(scheduleId));
			rs = pstmt.executeQuery();
			return rs.next() && LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(rs.getString(1));
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "isMasterDataWithContent()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "isMasterDataWithContent()", e);
			}
		}
	}

	/**
	 * Function will fetch the IM PROCESSING STATUS OF THE DOCUMENTS FOR THE SCHEDULE ID
	 * @param scheduleId
	 * @return
	 * @throws SQLException
	 */
	public static String getScheduleIMProcessingStatus(String scheduleId) throws SQLException 
	{
		String imProcessingStatus=null;
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="SELECT DC_IM_PROCESSING_STATUS FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				logger.info("getScheduleIMProcessingStatus :: SQL :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					imProcessingStatus = rs.getString("DC_IM_PROCESSING_STATUS");
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql = null;
			}
			else
			{
				logger.info("getScheduleIMProcessingStatus :: Schedule Id as parameter is null. Return null.");
				imProcessingStatus=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getScheduleIMProcessingStatus()", e);
			// set imProcessingStatus to null
			imProcessingStatus =null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return imProcessingStatus;
	}

	/**
	 * Function will get the Pending / Processing Jobs Count.
	 * @return
	 * @throws SQLException
	 */
	public static int getProcessingScheduleJobCount() throws SQLException 
	{
		int count=0;
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT COUNT(DC_SCHEDULE_ID) AS COUNT FROM gms3_dmt_conv_schedule WHERE "
					+ " DC_STATUS IN ('"+ApplicationProperties.getProperty("schedule.status.pending.value")+"',"
							+ " '"+ApplicationProperties.getProperty("schedule.status.processing.value")+"')";
			logger.info("getProcessingScheduleJobCount :: SQL :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				count = rs.getInt("COUNT");
			}
			rs.close();rs =null;
			stmt.close();stmt = null;
			sql = null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getProcessingScheduleJobCount()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return count;
	}

	public static boolean updateDeleteProcessingCount(String scheduleId, String itemId) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_PROCESSED_DOCS_COUNT_DEL FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DOCS_COUNT_DEL =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemCount=0;
					String getExistingItemsCount="SELECT DC_PROCESSED_DOCS_COUNT_DEL FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemCount = rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemCount = itemCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DOCS_COUNT_DEL =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				
			}
			else
			{
				logger.info("updateDeleteProcessingCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDeleteProcessingCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}
	
	
	/**
	 * Function will update the Processing Count of the Schedule
	 * @param scheduleId
	 * @return
	 * @throws SQLException
	 */
	public static boolean updateProcessingCount(String scheduleId, String itemId)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_PROCESSED_DOCS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_PROCESSED_DOCS_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DOCS_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemCount=0;
					String getExistingItemsCount="SELECT DC_PROCESSED_DOCS_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemCount = rs.getLong("DC_PROCESSED_DOCS_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemCount = itemCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DOCS_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				
			}
			else
			{
				logger.info("updateProcessingCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingCount()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingCount()", e);
			}
		}
		return true;
	}
	
	/**
	 * Function will update the Failure Count for the Schedule
	 * @param scheduleId
	 * @param itemId
	 * @return
	 * @throws SQLException
	 */
	public static boolean updateFailureCount(String scheduleId, String itemId) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_FAILED_DOCS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_FAILED_DOCS_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_FAILED_DOCS_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
			
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemFailureCount=0;
					String getExistingItemsCount="SELECT DC_FAILED_DOCS_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemFailureCount = rs.getLong("DC_FAILED_DOCS_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemFailureCount = itemFailureCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_FAILED_DOCS_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemFailureCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
			}
			else
			{
				logger.info("updateFailureCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateFailureCount()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateFailureCount()", e);
			}
		}
		return true;
	}
	
	/**
	 * Function will reset the Data Preparation Success & Failure Count for Processing Material Folder and Schedule
	 * @param scheduleId
	 * @param itemId
	 * @param successCountForReset
	 * @param failureCountForReset
	 * @return
	 */
	public static boolean resetDataPreparationCount(String scheduleId, String itemId,long successCountForReset, long failureCountForReset) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				long successCount=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_PROCESSED_DOCS_COUNT,DC_FAILED_DOCS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_FAILED_DOCS_COUNT");
					successCount = rs.getLong("DC_PROCESSED_DOCS_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				count = count-failureCountForReset;
				successCount = successCount-successCountForReset;
				
				if(count<0)
				{
					count=0;
				}
				if(successCount<0)
				{
					successCount= 0;
				}
				
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DOCS_COUNT = ?, DC_FAILED_DOCS_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, successCount);
				pstmt.setLong(2, count);
				pstmt.setLong(3, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				count=0;
				successCount = 0;
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					/*
					 * FOR ITEM DIRECTLY SET SUCCESS & FAILURE COUNT TO 0
					 */
					count=0;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DOCS_COUNT=?,DC_FAILED_DOCS_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, count);
					pstmt.setLong(2, count);
					pstmt.setLong(3, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
			}
			else
			{
				logger.info("updateFailureCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateFailureCount()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateFailureCount()", e);
			}
		}
		return true;
	}
	
	
	public static boolean updateDisplayOrderProcessingCount(String scheduleId, String itemId, Connection conn) throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				String getExistingCount="SELECT DC_PROCESSED_DISPORD_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_PROCESSED_DISPORD_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DISPORD_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemCount=0;
					String getExistingItemsCount="SELECT DC_PROCESSED_DISPORD_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemCount = rs.getLong("DC_PROCESSED_DISPORD_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemCount = itemCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DISPORD_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				if(null!=conn && conn.getAutoCommit()==false)
				{
					// commit transaction
					conn.commit();
				}
			}
			else
			{
				logger.info("updateDisplayOrderProcessingCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDisplayOrderProcessingCount()", e);
			return false;
		}
		finally
		{
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}
	
	public static boolean updateDisplayOrderFailureCount(String scheduleId, String itemId, Connection conn) throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				String getExistingCount="SELECT DC_FAILED_DISPORD_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_FAILED_DISPORD_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_FAILED_DISPORD_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
			
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemFailureCount=0;
					String getExistingItemsCount="SELECT DC_FAILED_DISPORD_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemFailureCount = rs.getLong("DC_FAILED_DISPORD_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemFailureCount = itemFailureCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_FAILED_DISPORD_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemFailureCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				if(null!=conn && conn.getAutoCommit()==false)
				{
					// commit transaction
					conn.commit();
				}
			}
			else
			{
				logger.info("updateDisplayOrderFailureCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDisplayOrderFailureCount()", e);
			return false;
		}
		finally
		{
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}

	public static boolean updateDISPCount(String scheduleId, String itemId, int successCount, int failureCount)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				int existingSuccessCount=0;
				int existingFailureCount=0;
				
				String getExistingCount="SELECT DC_PROCESSED_DISPORD_COUNT,DC_FAILED_DISPORD_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					existingSuccessCount = rs.getInt("DC_PROCESSED_DISPORD_COUNT");
					existingFailureCount = rs.getInt("DC_FAILED_DISPORD_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				existingSuccessCount = existingSuccessCount+successCount;
				existingFailureCount = existingFailureCount+failureCount;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DISPORD_COUNT =?,DC_FAILED_DISPORD_COUNT = ? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setInt(1, existingSuccessCount);
				pstmt.setInt(2, existingFailureCount);
				pstmt.setLong(3, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DISPORD_COUNT =?,DC_FAILED_DISPORD_COUNT = ? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setInt(1, successCount);
					pstmt.setInt(2, failureCount);
					pstmt.setLong(3, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				if(null!=conn && conn.getAutoCommit()==false)
				{
					// commit transaction
					conn.commit();
				}
			}
			else
			{
				logger.info("updateDISPCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDISPCount()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDISPCount()", e);
			}
		}
		return true;
	}
	
	
	public static boolean updateCDCount(String scheduleId, String itemId,Connection conn, int successCount, int failureCount) throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				int existingSuccessCount=0;
				int existingFailureCount=0;
				
				String getExistingCount="SELECT DC_PROCESSED_CD_COUNT,DC_FAILED_CD_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					existingSuccessCount = rs.getInt("DC_PROCESSED_CD_COUNT");
					existingFailureCount = rs.getInt("DC_FAILED_CD_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				existingSuccessCount = existingSuccessCount+successCount;
				existingFailureCount = existingFailureCount+failureCount;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_CD_COUNT =?,DC_FAILED_CD_COUNT = ? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setInt(1, existingSuccessCount);
				pstmt.setInt(2, existingFailureCount);
				pstmt.setLong(3, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_CD_COUNT =?,DC_FAILED_CD_COUNT = ? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setInt(1, successCount);
					pstmt.setInt(2, failureCount);
					pstmt.setLong(3, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				if(null!=conn && conn.getAutoCommit()==false)
				{
					// commit transaction
					conn.commit();
				}
			}
			else
			{
				logger.info("updateCDCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateCDCount()", e);
			return false;
		}
		finally
		{
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}

	public static boolean updateSCMVINCount(String scheduleId, String itemId,int successCount, int failureCount) throws SQLException
	{
		Connection  conn  = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				int existingSuccessCount=0;
				int existingFailureCount=0;
				
				String getExistingCount="SELECT DC_PROCESSED_SCM_COUNT,DC_FAILED_SCM_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					existingSuccessCount = rs.getInt("DC_PROCESSED_SCM_COUNT");
					existingFailureCount = rs.getInt("DC_FAILED_SCM_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				existingSuccessCount = existingSuccessCount+successCount;
				existingFailureCount = existingFailureCount+failureCount;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_SCM_COUNT =?,DC_FAILED_SCM_COUNT = ? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setInt(1, existingSuccessCount);
				pstmt.setInt(2, existingFailureCount);
				pstmt.setLong(3, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_SCM_COUNT =?,DC_FAILED_SCM_COUNT = ? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setInt(1, successCount);
					pstmt.setInt(2, failureCount);
					pstmt.setLong(3, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				if(null!=conn && conn.getAutoCommit()==false)
				{
					// commit transaction
					conn.commit();
				}
			}
			else
			{
				logger.info("updateSCMVINCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateSCMVINCount()", e);
			return false;
		}
		finally
		{
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
		return true;
	}

	
	public static boolean updateDeleteFailureCount(String scheduleId, String itemId) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_FAILED_DOCS_COUNT_DEL FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_FAILED_DOCS_COUNT_DEL");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// increment by 1
				count = count+1;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_FAILED_DOCS_COUNT_DEL =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
			
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemFailureCount=0;
					String getExistingItemsCount="SELECT DC_FAILED_DOCS_COUNT_DEL FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemFailureCount = rs.getLong("DC_FAILED_DOCS_COUNT_DEL");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// increment by 1
					itemFailureCount = itemFailureCount+1;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_FAILED_DOCS_COUNT_DEL =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemFailureCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
			}
			else
			{
				logger.info("updateDeleteFailureCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateDeleteFailureCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}

	public static boolean updateProcessingOKAssetsCount(String scheduleId, String itemId) throws SQLException
	{
		// written in bulk while the schedule's OKAssets step runs (OkAssetsCountBuffer)
		if(OkAssetsCountBuffer.add(scheduleId, itemId, true))
		{
			return true;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				scheduleId = scheduleId.trim();
				if(null!=scheduleId && !"".equals(scheduleId))
				{
					long count=0;
					conn = getConnection();
					String getExistingCount="SELECT DC_PROCESSED_OKAST_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
					pstmt = conn.prepareStatement(getExistingCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						count = rs.getLong("DC_PROCESSED_OKAST_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingCount= null;
					
					// increment by 1
					count = count+1;
					String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_OKAST_COUNT =? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setLong(1, count);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateCountSql= null;
					
					if(null!=itemId && !"".equals(itemId))
					{
						long itemCount=0;
						String getExistingItemsCount="SELECT DC_PROCESSED_OKAST_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
						pstmt = conn.prepareStatement(getExistingItemsCount);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							itemCount = rs.getLong("DC_PROCESSED_OKAST_COUNT");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt=null;
						getExistingItemsCount= null;
						
						// increment by 1
						itemCount = itemCount+1;
						String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_OKAST_COUNT =? WHERE DC_CRITERIA_ID=?";
						pstmt = conn.prepareStatement(updateItemsCountSql);
						pstmt.setLong(1, itemCount);
						pstmt.setLong(2, new Long(itemId).longValue());
						pstmt.executeUpdate();
						pstmt.close();
						pstmt= null;
						updateItemsCountSql= null;
					
					}
				}
			}
			else
			{
				logger.info("updateProcessingOKAssetsCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingOKAssetsCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}

	public static boolean updateFailureOKAssetsCount(String scheduleId, String itemId) throws SQLException
	{
		// written in bulk while the schedule's OKAssets step runs (OkAssetsCountBuffer)
		if(OkAssetsCountBuffer.add(scheduleId, itemId, false))
		{
			return true;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				scheduleId = scheduleId.trim();
				if(null!=scheduleId && !"".equals(scheduleId))
				{
					long count=0;
					conn = getConnection();
					String getExistingCount="SELECT DC_FAILED_OKAST_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
					pstmt = conn.prepareStatement(getExistingCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						count = rs.getLong("DC_FAILED_OKAST_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingCount= null;
					
					// increment by 1
					count = count+1;
					String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_FAILED_OKAST_COUNT =? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setLong(1, count);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateCountSql= null;
					
					if(null!=itemId && !"".equals(itemId))
					{
						long itemCount=0;
						String getExistingItemsCount="SELECT DC_FAILED_OKAST_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
						pstmt = conn.prepareStatement(getExistingItemsCount);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							itemCount = rs.getLong("DC_FAILED_OKAST_COUNT");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt=null;
						getExistingItemsCount= null;
						
						// increment by 1
						itemCount = itemCount+1;
						String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_FAILED_OKAST_COUNT =? WHERE DC_CRITERIA_ID=?";
						pstmt = conn.prepareStatement(updateItemsCountSql);
						pstmt.setLong(1, itemCount);
						pstmt.setLong(2, new Long(itemId).longValue());
						pstmt.executeUpdate();
						pstmt.close();
						pstmt= null;
						updateItemsCountSql= null;
					
					}
				}
			}
			else
			{
				logger.info("updateFailedOKAssetsCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateFailedOKAssetsCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}
	
	public static boolean updateProcessingOKAssetsDeleteCount(String scheduleId) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				scheduleId = scheduleId.trim();
				if(null!=scheduleId && !"".equals(scheduleId))
				{
					long count=0;
					conn = getConnection();
					String getExistingCount="SELECT DC_PROCESSED_OKAST_COUNT_DEL FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
					pstmt = conn.prepareStatement(getExistingCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						count = rs.getLong("DC_PROCESSED_OKAST_COUNT_DEL");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingCount= null;
					
					// increment by 1
					count = count+1;
					String updateCountSql="UPDATE DC_PROCESSED_OKAST_COUNT_DEL SET DC_PROCESSED_OKAST_COUNT =? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setLong(1, count);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateCountSql= null;
				
				}
					
			}
			else
			{
				logger.info("updateProcessingOKAssetsDeleteCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingOKAssetsDeleteCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}

	public static int getPendingProcessingJobsCount() throws SQLException
	{
		int count=0;
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			conn= getConnection();
			String sql="SELECT COUNT(DC_SCHEDULE_ID) AS COUNT FROM gms3_dmt_conv_schedule WHERE DC_STATUS IN ('"+ApplicationProperties.getProperty("schedule.status.pending.value")+"','"+ApplicationProperties.getProperty("schedule.status.processing.value")+"')";
			logger.info("getPendingProcessingJobsCount :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs=  stmt.executeQuery(sql);
			if(rs.next())
			{
				count = rs.getInt("COUNT");
			}
			sql=null;
			rs.close();rs=  null;
			stmt.close();stmt = null;
			
			if(count==0)
			{
				/*
				 * check if any Master Data Synching Job is Pending or Processing
				 */
				sql = "SELECT COUNT(AS_SCHEDULE_ID) AS COUNT FROM gms3_auto_sync_sch WHERE AS_JOB_STATUS IN ('"+AutoSyncConstants.STATUS_PENDING+"','"+AutoSyncConstants.STATUS_PROCESSING+"')";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					count = rs.getInt("COUNT");
				}
				sql = null;
				rs.close();rs=  null;
				stmt.close();stmt = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getPendingProcessingJobsCount()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return count;
	}

	public static ArrayList<ScheduleItemDetails> getPendingProcessingScheduleItemDetails() throws SQLException
	{
		ArrayList<ScheduleItemDetails> itemsList = new ArrayList<ScheduleItemDetails>();
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT A.*, B.DC_SCHEDULE_NAME FROM gms3_dmt_conv_sch_criteria A, gms3_dmt_conv_schedule B  WHERE  "
					+ " A.DC_SCHEDULE_ID = B.DC_SCHEDULE_ID AND B.DC_STATUS IN ('"+ApplicationProperties.getProperty("schedule.status.pending.value")+"','"+ApplicationProperties.getProperty("schedule.status.processing.value")+"')";
			logger.info("getPendingProcessingScheduleItemDetails :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				ScheduleItemDetails schItemDetails =new ScheduleItemDetails();
				schItemDetails.setSrNo(itemsList.size()+1);
				schItemDetails.setItemId(rs.getLong("DC_CRITERIA_ID"));
				schItemDetails.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
				schItemDetails.setScheduleName(rs.getString("DC_SCHEDULE_NAME"));
				schItemDetails.setMarket(rs.getString("DC_MARKET"));
				schItemDetails.setLocale(rs.getString("DC_LOCALE"));
				schItemDetails.setModel(rs.getString("DC_MODEL"));
				schItemDetails.setModelFolderName(rs.getString("DC_MODEL_FOLDER_NAME"));
				schItemDetails.setManualType(rs.getString("DC_MANUAL_TYPE"));
				schItemDetails.setMaterialFolderName(rs.getString("DC_MATERIAL_NAME"));
				schItemDetails.setTotalDocsForProcessing(rs.getLong("DC_TOTAL_DOCS_COUNT"));
				schItemDetails.setTotalDocsForDeletion(rs.getLong("DC_TOTAL_DOCS_COUNT_DEL"));
				schItemDetails.setOkAssetsCount(rs.getLong("DC_OKASSETS_COUNT"));
				schItemDetails.setOkAssetsDeleteCount(rs.getLong("DC_OKASSETS_COUNT_DEL"));
				schItemDetails.setMetaDocsCount(rs.getLong("DC_METADOCS_COUNT"));
				
				schItemDetails.setModelType(rs.getString("DC_MODEL_TYPE"));
				schItemDetails.setCarlineCode(rs.getString("DC_CARLINE_CODE"));
				schItemDetails.setFaceLiftFolderName(rs.getString("DC_FACELIFT_FOLDER_NAME"));
				schItemDetails.setProcessedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT"));
				schItemDetails.setFailedProcessedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT"));
				schItemDetails.setDeletedDocsCount(rs.getLong("DC_PROCESSED_DOCS_COUNT_DEL"));
				schItemDetails.setFailedDeletedDocsCount(rs.getLong("DC_FAILED_DOCS_COUNT_DEL"));
				schItemDetails.setProcessedOkAssetsCount(rs.getLong("DC_PROCESSED_OKAST_COUNT"));
				schItemDetails.setFailedOkAssetsCount(rs.getLong("DC_FAILED_OKAST_COUNT"));
				schItemDetails.setProcessedOkAssetsDeleteCount(rs.getLong("DC_PROCESSED_OKAST_COUNT_DEL"));
				schItemDetails.setFailedOkAssetsDeleteCount(rs.getLong("DC_FAILED_OKAST_COUNT_DEL"));
				
				schItemDetails.setTotalInnerLinksCount(rs.getLong("DC_TOTAL_INRLKS_COUNT"));
				schItemDetails.setProcessedInnerLinksCount(rs.getLong("DC_PROCESSED_INRLKS_COUNT"));
				schItemDetails.setFailedInnerLinksCount(rs.getLong("DC_FAILED_INRLKS_COUNT"));
				
				schItemDetails.setCurrentProcessingStatus(rs.getString("DC_CURRENT_PROCESSING_STATUS"));
				
				schItemDetails.setTotalDisplayOrderCount(rs.getLong("DC_TOTAL_DISPORD_COUNT"));
				schItemDetails.setProcessedDisplayOrderCount(rs.getLong("DC_PROCESSED_DISPORD_COUNT"));
				schItemDetails.setFailedDisplayOrderCount(rs.getLong("DC_FAILED_DISPORD_COUNT"));
				
				schItemDetails.setTotalCDProcessingCount(rs.getLong("DC_TOTAL_CD_COUNT"));
				schItemDetails.setProcessedCDProcessingCount(rs.getLong("DC_PROCESSED_CD_COUNT"));
				schItemDetails.setFailedCDProcessingCount(rs.getLong("DC_FAILED_CD_COUNT"));
				
				itemsList.add(schItemDetails);
				schItemDetails= null;
			}
			rs.close();rs =null;
			stmt.close();stmt=null;
			sql=null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getPendingProcessingScheduleItemDetails()", e);
			// set itemLsit to null;
			itemsList = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return itemsList;
	}

	
	public static boolean updateTotalInnerLinksCount(String scheduleId, long totalInnerLinksCount) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				scheduleId = scheduleId.trim();
				if(null!=scheduleId && !"".equals(scheduleId))
				{
					conn = getConnection();
					String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_TOTAL_INRLKS_COUNT =? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setLong(1, totalInnerLinksCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateCountSql= null;
				}
			}
			else
			{
				logger.info("updateTotalInnerLinksCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateTotalInnerLinksCount()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
		}
		return true;
	}


	public static boolean updateProcessingCountForInnerLinks(String scheduleId) throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				
				/*
				 * GET INNER LINKS COUNT FOR SCHEDULE WITH STATUS AS Y
				 */
				
				String getProcessedCount ="SELECT COUNT(*) AS PROCESSED_COUNT FROM gms3_dmt_conv_imdoc_inrlks WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue() + " AND TRIM(LOWER(DC_INNER_LINK_UPDATED_STATUS)) ='y'";
//				String getExistingCount="SELECT DC_PROCESSED_INRLKS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getProcessedCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("PROCESSED_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getProcessedCount= null;
				
				// add processingInnerLinksCount
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_INRLKS_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
			}
			else
			{
				logger.info("updateProcessingCountForInnerLinks :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingCountForInnerLinks()", e);
			return false;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
	}
	
	public static boolean updateCurrentJobStatus(String scheduleId, String itemId,String currentProcessingStatus)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=currentProcessingStatus && !"".equals(currentProcessingStatus))
			{
				conn = getConnection();
				String sql="UPDATE gms3_dmt_conv_schedule SET DC_CURRENT_PROCESSING_STATUS =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, currentProcessingStatus);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				sql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					String itemSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_CURRENT_PROCESSING_STATUS =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(itemSql);
					pstmt.setString(1, currentProcessingStatus);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					itemSql= null;
				}
			}
			else
			{
				logger.info("updateProcessingCount :: Schedule Id / Current Processing Status as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateCurrentJobStatus()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateCurrentJobStatus()", e);
			}
		}
		return true;
	}

	public static boolean updateCurrentJobStatusForItem(String itemId,String currentProcessingStatus) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=itemId && !"".equals(itemId) && null!=currentProcessingStatus && !"".equals(currentProcessingStatus))
			{
				conn = getConnection();
				String itemSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_CURRENT_PROCESSING_STATUS =? WHERE DC_CRITERIA_ID=?";
				pstmt = conn.prepareStatement(itemSql);
				pstmt.setString(1, currentProcessingStatus.trim().toUpperCase());
				pstmt.setLong(2, new Long(itemId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				itemSql= null;
			
			}
			else
			{
				logger.info("updateCurrentJobStatusForItem :: Item Id / Current Processing Status as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateCurrentJobStatusForItem()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateCurrentJobStatusForItem()", e);
			}
		}
		return true;
	}

	
	public static boolean updateInnerLinksCount(String scheduleId, String itemId, long totalCount, long processedCount, long failedCount) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				scheduleId = scheduleId.trim();
				if(null!=scheduleId && !"".equals(scheduleId))
				{
					long existTotalCount=0;
					long existProcessedCount=0;
					long existFailedCount=0;
					conn = getConnection();
					String getExistingCount="SELECT DC_TOTAL_INRLKS_COUNT,DC_PROCESSED_INRLKS_COUNT,"
							+ "DC_FAILED_INRLKS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
					pstmt = conn.prepareStatement(getExistingCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						existTotalCount = rs.getLong("DC_TOTAL_INRLKS_COUNT");
						existProcessedCount = rs.getLong("DC_PROCESSED_INRLKS_COUNT");
						existFailedCount = rs.getLong("DC_FAILED_INRLKS_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingCount= null;
					
					// add Count values
					existTotalCount = existTotalCount+totalCount;
					existProcessedCount = existProcessedCount+ processedCount;
					existFailedCount = existFailedCount + failedCount;
					String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_TOTAL_INRLKS_COUNT =?,"
							+ " DC_PROCESSED_INRLKS_COUNT = ?,DC_FAILED_INRLKS_COUNT = ?"
							+ " WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setLong(1, existTotalCount);
					pstmt.setLong(2, existProcessedCount);
					pstmt.setLong(3, existFailedCount);
					pstmt.setLong(4, new Long(scheduleId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateCountSql= null;
					
					if(null!=itemId && !"".equals(itemId))
					{
						/*
						 * DON'T NEED TO DO THIS FOR ITEM
						 * AS FOR ALL MATERIAL FOLDERS THE COUNT WILL ALWAYS BE DIFFERENT AND SEPARATE
						 */
//						long itemTotalCount=0;
//						long itemProcessedCount=0;
//						long itemFailedCount=0;
//						String getExistingItemsCount="SELECT DC_TOTAL_INRLKS_COUNT,DC_PROCESSED_INRLKS_COUNT,"
//							+ "DC_FAILED_INRLKS_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
//						pstmt = conn.prepareStatement(getExistingItemsCount);
//						rs = pstmt.executeQuery();
//						if(rs.next())
//						{
//							itemTotalCount = rs.getLong("DC_TOTAL_INRLKS_COUNT");
//							itemProcessedCount = rs.getLong("DC_PROCESSED_INRLKS_COUNT");
//							itemFailedCount = rs.getLong("DC_FAILED_INRLKS_COUNT");
//						}
//						rs.close();
//						rs = null;
//						pstmt.close();
//						pstmt=null;
//						getExistingItemsCount= null;
//						
						// add counts
//						itemTotalCount = itemTotalCount+totalCount;
//						itemProcessedCount = itemProcessedCount+processedCount;
//						itemFailedCount= itemFailedCount+failedCount;
						String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria "
								+ " SET DC_TOTAL_INRLKS_COUNT =?,"
							+ " DC_PROCESSED_INRLKS_COUNT = ?,DC_FAILED_INRLKS_COUNT = ?"
							+ " WHERE DC_CRITERIA_ID=?";
						pstmt = conn.prepareStatement(updateItemsCountSql);
						pstmt.setLong(1, totalCount);
						pstmt.setLong(2, processedCount);
						pstmt.setLong(3, failedCount);
						pstmt.setLong(4, new Long(itemId).longValue());
						pstmt.executeUpdate();
						pstmt.close();
						pstmt= null;
						updateItemsCountSql= null;
					
					}
				}
			}
			else
			{
				logger.info("updateProcessingOKAssetsCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingOKAssetsCount()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateProcessingOKAssetsCount()", e);
			}
		}
		return true;
	}

	/**
	 * Function will save report summary for Schedule Code
	 * @param rsd
	 */
	public static void saveReportSummaryDetailsForSchedule(ReportsSummaryDetails rsd) throws SQLException
	{
		Statement stmt = null;
		Connection conn = null;
		try
		{
			if(null!=rsd.getSchduleCode() && !"".equals(rsd.getSchduleCode()))
			{
				if(null==rsd.getSuccessCount())
				{
					rsd.setSuccessCount(new Long(0).longValue());
				}
				if(null==rsd.getFailureCount())
				{
					rsd.setFailureCount(new Long(0).longValue());
				}
				if(null==rsd.getTotalCount())
				{
					rsd.setTotalCount(new Long(0).longValue());
				}
				conn = getConnection();
				String sql="INSERT INTO gms3_dmt_schedule_summary (DC_SCHEDULE_ID,DC_REPORT_NAME,DC_REPORT_STATUS,DC_TOTAL_COUNT,"
						+ "DC_SUCCESS_COUNT,DC_FAILURE_COUNT) VALUES("+new Long(rsd.getSchduleCode()).longValue()+", '"+rsd.getReportName()+"',"
						+ "'"+rsd.getReportStatus()+"',"+rsd.getTotalCount()+","+rsd.getSuccessCount()+","+rsd.getFailureCount()+")";
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				sql = null;
				logger.info("saveReportSummaryDetailsForSchedule :: {"+rsd.getReportName()+"} SUMMARY Saved for Schedule Id :: > "+ rsd.getSchduleCode());
			}
			else
			{
				logger.info("saveReportSummaryDetailsForSchedule :: Schedule Id as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveReportSummaryDetailsForSchedule()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			// set rsd to null;
			rsd = null;
		}
	}
	
	/**
	 * Function will get Report Summary Details on the basis of Schedule Id
	 * @param scheduleCode
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<ReportsSummaryDetails> getReportSummaryDetailsForSchedule(String scheduleCode) throws SQLException
	{
		ArrayList<ReportsSummaryDetails> list = new ArrayList<ReportsSummaryDetails>();
		Statement stmt =null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleCode && !"".equals(scheduleCode))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_dmt_schedule_summary WHERE DC_SCHEDULE_ID="+scheduleCode;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					ReportsSummaryDetails rsd = new ReportsSummaryDetails();
					rsd.setReportName(rs.getString("DC_REPORT_NAME"));
					rsd.setReportStatus(rs.getString("DC_REPORT_STATUS"));
					rsd.setTotalCount(rs.getLong("DC_TOTAL_COUNT"));
					rsd.setSuccessCount(rs.getLong("DC_SUCCESS_COUNT"));
					rsd.setFailureCount(rs.getLong("DC_FAILURE_COUNT"));
					list.add(rsd);
					rsd = null;
				}
				sql = null;
			}
			else
			{
				logger.info("getReportSummaryDetailsForSchedule :: Schedule Id as parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getReportSummaryDetailsForSchedule()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			// set scheduleCode to null
			scheduleCode= null;
		}
		return list;
	}

	public static ScheduleDetails createAutomationSchedule(ScheduleDetails schDetails) throws SQLException 
	{
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			if(null!=schDetails)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO gms3_dmt_conv_schedule
				 * First insert the SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql="INSERT INTO gms3_dmt_conv_schedule(DC_SCHEDULE_NAME,DC_USER_ID,DC_RECORD_STATUS) VALUES"
						+ "('"+schDetails.getScheduleName()+"','"+schDetails.getUserId()+"','"+AutoSyncConstants.STATUS_ACTIVE+"')";
				logger.info("createAutomationSchedule :: Generate Schedule Id :: Sql :: > " + sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "DC_SCHEDULE_ID" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				generatedColumns = null;
				if(null!=scheduleId && scheduleId>0)
				{
					logger.info("createAutomationSchedule :: Procced for Storing details for Schedule Id :: > " + scheduleId);
					/*
					 * add scheduleId to name and update It
					 * add scheduleId to thread and update It
					 */
					schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
					schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(scheduleId));
					String schSql="UPDATE gms3_dmt_conv_schedule SET DC_SCHEDULE_NAME=?,"
							+  "DC_STATUS=?,DC_TOTAL_DOCS_COUNT=?,"
							+ " DC_SCH_THREAD_ID=?,DC_SCHEDULE_TMSTP=? WHERE DC_SCHEDULE_ID= ?" ;
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setString(2, schDetails.getScheduleStatus());
					pstmt.setLong(3, schDetails.getTotalDocsForProcessing());
					pstmt.setString(4, schDetails.getThreadId());
					pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(6, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=  null;
					schSql= null;
					
					boolean allItemsCreated = true;
					if(null!=schDetails.getAutomationItemsList() && schDetails.getAutomationItemsList().size()>0)
					{
						Long itemId=null;
						for(int a=0;a<schDetails.getAutomationItemsList().size();a++)
						{
							// HERE USE COLUMN DC_MODEL_FOLDER_NAME FOR STORING CHANNEL REF KEY
							ItemDetails itemDetails = (ItemDetails)schDetails.getAutomationItemsList().get(a);
							sql="INSERT INTO gms3_dmt_conv_sch_criteria(DC_SCHEDULE_ID,DC_LOCALE) VALUES ("+scheduleId+",'"+itemDetails.getLocale()+"')";
							stmt = conn.createStatement();
							String generatedItemColumns[] = { "DC_CRITERIA_ID" };
							stmt.execute(sql,generatedItemColumns);
							rs = stmt.getGeneratedKeys();
							if(rs.next())
							{
								itemId = rs.getLong(1);
							}
							rs.close();rs=null;
							stmt.close();stmt=null;
							sql=null;
							generatedItemColumns = null;
							
							if(null!=itemId && itemId>0)
							{
								sql="UPDATE gms3_dmt_conv_sch_criteria SET DC_MODEL=?, DC_MANUAL_TYPE = ?,DC_TOTAL_DOCS_COUNT = ?,"
										+ " DC_CRITERIA_CREATED_TMSTP = ?,DC_MODEL_FOLDER_NAME = ? WHERE DC_CRITERIA_ID = ?";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, itemDetails.getCarlineInfo());
								pstmt.setString(2, itemDetails.getDocumentTypeRefKey());
								pstmt.setLong(3, itemDetails.getTotalDocumentsCount());
								pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(5, itemDetails.getChannelRefKey());
								pstmt.setLong(6, itemId);
								pstmt.executeUpdate();
								pstmt.close();
								pstmt=  null;
								sql=null;
								// set Item id
								itemDetails.setItemId(itemId);
							}
							else
							{
								// all itemsCreated = false and break the loop.
								allItemsCreated = false;
								break;
							}
							itemId = null;
						}
					}
					
					if(allItemsCreated==true)
					{
						/*
						 * commit the complete transaction
						 */
						conn.commit();
						logger.info("createAutomationSchedule :: IPM Facelift VIN Update Conversion with Name as {"+schDetails.getScheduleName()+"} Created Successfully.");
						// set scheduleId in schDetails
						schDetails.setScheduleId(scheduleId);
					}
					else
					{
						/*
						 * SOME iTEMS FAILED WHILE CREATION
						 * Roll Back the transaction
						 */
						conn.rollback();
						logger.info("createAutomationSchedule :: Failed to create IPM Facelift VIN Update Conversion. Could not process all Items.");
					}
				}
				else
				{
					/*
					 * roll back transaction
					 */
					if(null!=conn && !conn.isClosed())
					{
						conn.rollback();
					}
					logger.info("createAutomationSchedule :: Failed to Generate Schedule Id. Return null.");
				}
			}
			else
			{
				logger.info("createAutomationSchedule :: Schedule Details or Items in it are null as parameters, return null.");
			}
		}
		catch(Exception e)
		{
			logger.info("createAutomationSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			conn.rollback();
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "createAutomationSchedule()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			scheduleId = null;
		}
		return schDetails;
	}


	public static boolean updateMultipleProcessingCount(String scheduleId, String itemId, long processedCount)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				long count=0;
				conn = getConnection();
				String getExistingCount="SELECT DC_PROCESSED_DOCS_COUNT FROM gms3_dmt_conv_schedule WHERE DC_SCHEDULE_ID="+new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getLong("DC_PROCESSED_DOCS_COUNT");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt=null;
				getExistingCount= null;
				
				// add processedCount
				count = count+processedCount;
				String updateCountSql="UPDATE gms3_dmt_conv_schedule SET DC_PROCESSED_DOCS_COUNT =? WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				updateCountSql= null;
				
				if(null!=itemId && !"".equals(itemId) && !"0".equals(itemId))
				{
					long itemCount=0;
					String getExistingItemsCount="SELECT DC_PROCESSED_DOCS_COUNT FROM gms3_dmt_conv_sch_criteria WHERE DC_CRITERIA_ID="+new Long(itemId).longValue();
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						itemCount = rs.getLong("DC_PROCESSED_DOCS_COUNT");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt=null;
					getExistingItemsCount= null;
					
					// add processedCount
					itemCount = itemCount+processedCount;
					String updateItemsCountSql="UPDATE gms3_dmt_conv_sch_criteria SET DC_PROCESSED_DOCS_COUNT =? WHERE DC_CRITERIA_ID=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(itemId).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					updateItemsCountSql= null;
				}
				
			}
			else
			{
				logger.info("updateMultipleProcessingCount :: Schedule Id as Parameter is null.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateMultipleProcessingCount()", e);
			return false;
		}
		finally
		{
			try
			{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateMultipleProcessingCount()", e);
			}
		}
		return true;
	}

	public static List<MarketLocaleMasterDataTypeMapping> getMarketBasedLocalesList(String market)
	{
		List<MarketLocaleMasterDataTypeMapping> localesList = new ArrayList<MarketLocaleMasterDataTypeMapping>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=market && !"".equals(market))
			{

				/*
				 * FETCH LOCALES BASED MATRIX ON THE BASIS OF MARKET
				 */
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_alt_lang_mapping WHERE MDM_MARKET='"+market+"' AND MDM_ALT_MAPPING_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
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
			else
			{
				logger.info("getMarketBasedLocalesList :: Market as Parameter is null. Return null.");
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getMarketBasedLocalesList()", e);
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
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getMarketBasedLocalesList()", e);	
			}
		}
		return localesList;
	}

	public static String getAlternateLocale(String locale, String masterDataType, Connection conn)
	{
		String alternateLocale="";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=locale && !"".equals(locale) && null!=masterDataType && !"".equals(masterDataType))
			{
				locale= locale.replace("_", "-");
				/*
				 * FETCH ALTERNATE LOCALES BASED ON PROCESSING LOCLAE & MASTER DATA TYPE
				 */
				String sql = "SELECT MDM_ALTERNATE_LOCALE FROM gms3_mdm_alt_lang_mapping WHERE MDM_LOCALE=? AND MDM_MASTER_DATA_TYPE=? "
						+ " AND MDM_ALT_MAPPING_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				pstmt=conn.prepareStatement(sql);
				pstmt.setString(1, locale);
				pstmt.setString(2, masterDataType);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					alternateLocale=rs.getString("MDM_ALTERNATE_LOCALE");
				}
				rs.close();rs = null;
				pstmt.close();pstmt=null;
				sql = null;
			}
			else
			{
				logger.info("getAlternateLocale :: Processing Locale / Master Data Type as Parameter are null. Return null.");
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getAlternateLocale()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getAlternateLocale()", e);	
			}
		}
		return alternateLocale;
	}

	public static boolean deleteScheduleDetails(String scheduleIds) 
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
						sql="UPDATE gms3_dmt_conv_schedule SET DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_INACTIVE+"' "
								+ "   WHERE DC_SCHEDULE_ID = "+ tokens[i].toString();
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
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteScheduleDetails()", e1);
			}
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteScheduleDetails()", e);
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
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteScheduleDetails()", e);
			}
			// set passed parameter to null
			scheduleIds=null;
		}
		return true;
	}
	
	public static void saveDisplayOrderDetailsTemp(ArrayList<DisplayOrderDetails> displayOrderList, String scheduleId)
	{
		PreparedStatement pstmt=null;
		Connection conn = null;
		try
		{
			if(null!=displayOrderList && displayOrderList.size()>0 && null!=scheduleId && !"".equals(scheduleId))
			{
				/*
				 * DIVIDE INTO PARITION OF 100 AND SAVE THEM IN DATABASE
				 */
				int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
				ArrayList<List<DisplayOrderDetails>> partitions = new ArrayList<List<DisplayOrderDetails>>();
				for (int i=0; i<displayOrderList.size(); i += partitionSize) {
					partitions.add(displayOrderList.subList(i, Math.min(i + partitionSize, displayOrderList.size())));
				}
				
				if(null!=partitions && partitions.size()>0)
				{
					DisplayOrderDetails details = null;
					// COLUMN DC_IM_DOC_ID WILL BE USED TO STORING LINE TYPE - VALID / INVALID
					String inserSql ="INSERT INTO gms3_dmt_el_gr_nm_dispord (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
							+ "DC_SOURCE_NETWORK_LOC,DC_DISPORD_LEVEL_1,DC_DISPORD_LEVEL_2,DC_DISPORD_LEVEL_3,DC_DISPORD_LEVEL_4,DC_DISPORD_LEVEL_5,"
							+ "DC_DISPORD_LEVEL_6,DC_DISPORD_MODEL_FOLDER,DC_DISPORD_MANUALTYPE_FOLDER,DC_DISPORD_FACELIFT_FOLDER,DC_DISPORD_MATERIAL_FOLDER,"
							+ "DC_DISPORD_FILETYPE_FOLDER,DC_DISPORD_SOURCE_FILE_NAME,DC_IM_DOC_MANUAL_TYPE_REFKEY,DC_DISPORD_TXT_FILE_NAME,DC_DISPORD_TXT_FILE_PATH,"
							+ "DC_DISPORD_CREATED_TMSTP,DC_SEQUENCE_NO,DC_ENGINE_TYPE,DC_MISSION_TYPE,DC_DRIVEAXLE_TYPE,DC_BODY_TYPE,"
							+ "DC_TITLE,DC_DISPORD_NAME_LEVEL_1,DC_DISPORD_NAME_LEVEL_2,DC_DISPORD_NAME_LEVEL_3,DC_DISPORD_NAME_LEVEL_4,"
							+ "DC_DISPORD_NAME_LEVEL_5,DC_DISPORD_NAME_LEVEL_6) "
							+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					conn = getConnection();
					// ONE TRANSACTION FOR ALL ROWS - a commit per row costs a cluster round trip each
					long startedAt = System.currentTimeMillis();
					logger.info("saveDisplayOrderDetailsTemp :: Rows to save :: >" + displayOrderList.size() + " in " + partitions.size() + " batches of " + partitionSize);
					conn.setAutoCommit(false);
					for(List<DisplayOrderDetails> subList : partitions)
					{
						if(null!=subList && subList.size()>0)
						{
							try
							{
								pstmt=null;
								pstmt=conn.prepareStatement(inserSql);
								details=null;
								for(int a=0;a<subList.size();a++)
								{
									details=(DisplayOrderDetails)subList.get(a);
									pstmt.setLong(1, new Long(scheduleId).longValue());
									pstmt.setString(2, details.getLineType());
									if(null!=details.getFilePath())
									{
										pstmt.setString(3, details.getFilePath().trim());
									}
									else
									{
										pstmt.setNull(3, Types.VARCHAR);
									}
									
									pstmt.setString(4, details.getDisplayOrderLevel1Code());
									pstmt.setString(5, details.getDisplayOrderLevel2Code());
									pstmt.setString(6, details.getDisplayOrderLevel3Code());
									pstmt.setString(7, details.getDisplayOrderLevel4Code());
									pstmt.setString(8, details.getDisplayOrderLevel5Code());
									pstmt.setString(9, details.getDisplayOrderLevel6Code());
									pstmt.setString(10, details.getModelFolderName());
									pstmt.setString(11, details.getManualType());
									pstmt.setString(12, details.getFaceLiftFolderName());
									pstmt.setString(13, details.getMaterialFolderName());
									pstmt.setString(14, details.getProcessingFolderName());
									pstmt.setString(15, details.getFileName());
									pstmt.setString(16, details.getManualTypeRefKey());
									pstmt.setString(17, details.getDisplayOrderSourceFileName());
									pstmt.setString(18, details.getDisplayOrderSourceFilePath());
									pstmt.setTimestamp(19, new java.sql.Timestamp(new Date().getTime()));
									pstmt.setString(20, details.getSequenceNo());
									pstmt.setString(21, details.getEngineType());
									pstmt.setString(22, details.getMissionType());
									pstmt.setString(23, details.getDriveAxleType());
									pstmt.setString(24, details.getBodyType());
									pstmt.setString(25, details.getTitle());
									pstmt.setString(26, details.getDisplayOrderLevel1Name());
									pstmt.setString(27, details.getDisplayOrderLevel2Name());
									pstmt.setString(28, details.getDisplayOrderLevel3Name());
									pstmt.setString(29, details.getDisplayOrderLevel4Name());
									pstmt.setString(30, details.getDisplayOrderLevel5Name());
									pstmt.setString(31, details.getDisplayOrderLevel6Name());
									pstmt.addBatch();
									details=null;
								}
								pstmt.executeBatch();
								pstmt.close();pstmt=null;
							}
							catch(Exception  e)
							{
								Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveDisplayOrderDetailsTemp()", e);
							}
						}
						subList=null;
					}
					conn.commit();
					conn.setAutoCommit(true);
					logger.info("saveDisplayOrderDetailsTemp :: Rows saved :: >" + displayOrderList.size() + " in " + (System.currentTimeMillis() - startedAt) + " ms");
				}
				partitions=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveDisplayOrderDetailsTemp()", e);
			try
			{
				if(null!=conn && !conn.getAutoCommit())
				{
					conn.rollback();
				}
			}
			catch(Exception re)
			{
				// nothing more to do
			}
		}
		finally
		{
			try
			{
				if(null!= pstmt)
					pstmt.close();
				if(null!=conn)
				{
					// the pool hands the connection out again - never in a half-open transaction
					try { if(!conn.getAutoCommit()) { conn.rollback(); conn.setAutoCommit(true); } } catch(Exception ae) { }
					conn.close();
				}
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "saveDisplayOrderDetailsTemp()", e);
			}
			displayOrderList =null;
			scheduleId= null;
		}
	}
	
	public static ArrayList<DisplayOrderDetails> getApplicableDisplayOrderForDocument(String filePath)
	{
		ArrayList<DisplayOrderDetails> applicableDisplayOrderDetails = null;
		PreparedStatement pstmt=null;
		ResultSet rs=  null;
		Connection conn= null;
		try
		{
			if(null!=filePath && !"".equals(filePath))
			{
				DisplayOrderDetails details = null;
				conn = getConnection();
				String sql="SELECT * FROM gms3_dmt_el_gr_nm_dispord WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = ?";
				pstmt=conn.prepareStatement(sql);
				pstmt.setString(1, filePath.trim().toLowerCase());
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					details = new DisplayOrderDetails();
					details.setSequenceNo(rs.getString("DC_SEQUENCE_NO"));
					details.setLineType(rs.getString("DC_IM_DOC_ID"));
					details.setFilePath(rs.getString("DC_SOURCE_NETWORK_LOC"));
					details.setModelFolderName(rs.getString("DC_DISPORD_MODEL_FOLDER"));
					details.setManualType(rs.getString("DC_DISPORD_MANUALTYPE_FOLDER"));
					details.setFaceLiftFolderName(rs.getString("DC_DISPORD_FACELIFT_FOLDER"));
					details.setMaterialFolderName(rs.getString("DC_DISPORD_MATERIAL_FOLDER"));
					details.setProcessingFolderName(rs.getString("DC_DISPORD_FILETYPE_FOLDER"));
					details.setFileName(rs.getString("DC_DISPORD_SOURCE_FILE_NAME"));
					details.setTitle(rs.getString("DC_TITLE"));
					details.setEngineType(rs.getString("DC_ENGINE_TYPE"));
					details.setMissionType(rs.getString("DC_MISSION_TYPE"));
					details.setBodyType(rs.getString("DC_BODY_TYPE"));
					details.setDriveAxleType(rs.getString("DC_DRIVEAXLE_TYPE"));
					details.setDisplayOrderLevel1Code(rs.getString("DC_DISPORD_LEVEL_1"));
					details.setDisplayOrderLevel2Code(rs.getString("DC_DISPORD_LEVEL_2"));
					details.setDisplayOrderLevel3Code(rs.getString("DC_DISPORD_LEVEL_3"));
					details.setDisplayOrderLevel4Code(rs.getString("DC_DISPORD_LEVEL_4"));
					details.setDisplayOrderLevel5Code(rs.getString("DC_DISPORD_LEVEL_5"));
					details.setDisplayOrderLevel6Code(rs.getString("DC_DISPORD_LEVEL_6"));
					
					details.setDisplayOrderLevel1Name(rs.getString("DC_DISPORD_NAME_LEVEL_1"));
					details.setDisplayOrderLevel2Name(rs.getString("DC_DISPORD_NAME_LEVEL_2"));
					details.setDisplayOrderLevel3Name(rs.getString("DC_DISPORD_NAME_LEVEL_3"));
					details.setDisplayOrderLevel4Name(rs.getString("DC_DISPORD_NAME_LEVEL_4"));
					details.setDisplayOrderLevel5Name(rs.getString("DC_DISPORD_NAME_LEVEL_5"));
					details.setDisplayOrderLevel6Name(rs.getString("DC_DISPORD_NAME_LEVEL_6"));
					
					details.setDisplayOrderSourceFileName(rs.getString("DC_DISPORD_TXT_FILE_NAME"));
		    		details.setDisplayOrderSourceFilePath(rs.getString("DC_DISPORD_TXT_FILE_PATH"));
					
					if(null==applicableDisplayOrderDetails || applicableDisplayOrderDetails.size()<=0)
					{
						applicableDisplayOrderDetails = new ArrayList<DisplayOrderDetails>();
					}
					applicableDisplayOrderDetails.add(details);
					details = null;
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;
				details = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getApplicableDisplayOrderForDocument()", e);
		}
		finally
		{
			try
			{
				if(null!= pstmt)
					pstmt.close();pstmt=null;
				if(null!=conn)
					conn.close();conn=null;
				if(null!=rs)
					rs.close();rs=null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getApplicableDisplayOrderForDocument()", e);
			}
			filePath =null;
		}
		return applicableDisplayOrderDetails;
	}
	
	public static ArrayList<DisplayOrderDetails> getApplicableDisplayOrderForMaterialOrFaceLiftFolder(String modelFolderName,String manualType,String faceliftFolderName,String materialFolderName)
	{
		ArrayList<DisplayOrderDetails> applicableDisplayOrderDetails = null;
		PreparedStatement pstmt=null;
		ResultSet rs=  null;
		Connection conn= null;
		try
		{
			if(null!=modelFolderName && !"".equals(modelFolderName) && null!=manualType && !"".equals(manualType) && null!=faceliftFolderName && !"".equals(faceliftFolderName))
			{
				DisplayOrderDetails details = null;
				conn = getConnection();
				String sql="SELECT * FROM gms3_dmt_el_gr_nm_dispord WHERE TRIM(LOWER(DC_DISPORD_MODEL_FOLDER)) = ? AND TRIM(LOWER(DC_DISPORD_MANUALTYPE_FOLDER)) = ? AND "
						+ "TRIM(LOWER(DC_DISPORD_FACELIFT_FOLDER)) = ? ";
				if(null!=materialFolderName && !"".equals(materialFolderName))
				{
					sql+=" AND TRIM(LOWER(DC_DISPORD_MATERIAL_FOLDER)) = ?";
				}
				pstmt=conn.prepareStatement(sql);
				pstmt.setString(1, modelFolderName.trim().toLowerCase());
				pstmt.setString(2, manualType.trim().toLowerCase());
				pstmt.setString(3, faceliftFolderName.trim().toLowerCase());
				if(null!=materialFolderName && !"".equals(materialFolderName))
				{
					pstmt.setString(4, materialFolderName.trim().toLowerCase());
				}
				logger.info("getApplicableDisplayOrderForMaterialOrFaceLiftFolder :: Sql :: >" + sql);
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					details = new DisplayOrderDetails();
					details.setSequenceNo(rs.getString("DC_SEQUENCE_NO"));
					details.setLineType(rs.getString("DC_IM_DOC_ID"));
					details.setFilePath(rs.getString("DC_SOURCE_NETWORK_LOC"));
					details.setModelFolderName(rs.getString("DC_DISPORD_MODEL_FOLDER"));
					details.setManualType(rs.getString("DC_DISPORD_MANUALTYPE_FOLDER"));
					details.setFaceLiftFolderName(rs.getString("DC_DISPORD_FACELIFT_FOLDER"));
					details.setMaterialFolderName(rs.getString("DC_DISPORD_MATERIAL_FOLDER"));
					details.setProcessingFolderName(rs.getString("DC_DISPORD_FILETYPE_FOLDER"));
					details.setFileName(rs.getString("DC_DISPORD_SOURCE_FILE_NAME"));
					details.setTitle(rs.getString("DC_TITLE"));
					details.setEngineType(rs.getString("DC_ENGINE_TYPE"));
					details.setMissionType(rs.getString("DC_MISSION_TYPE"));
					details.setBodyType(rs.getString("DC_BODY_TYPE"));
					details.setDriveAxleType(rs.getString("DC_DRIVEAXLE_TYPE"));
					details.setDisplayOrderLevel1Code(rs.getString("DC_DISPORD_LEVEL_1"));
					details.setDisplayOrderLevel2Code(rs.getString("DC_DISPORD_LEVEL_2"));
					details.setDisplayOrderLevel3Code(rs.getString("DC_DISPORD_LEVEL_3"));
					details.setDisplayOrderLevel4Code(rs.getString("DC_DISPORD_LEVEL_4"));
					details.setDisplayOrderLevel5Code(rs.getString("DC_DISPORD_LEVEL_5"));
					details.setDisplayOrderLevel6Code(rs.getString("DC_DISPORD_LEVEL_6"));
					
					details.setDisplayOrderLevel1Name(rs.getString("DC_DISPORD_NAME_LEVEL_1"));
					details.setDisplayOrderLevel2Name(rs.getString("DC_DISPORD_NAME_LEVEL_2"));
					details.setDisplayOrderLevel3Name(rs.getString("DC_DISPORD_NAME_LEVEL_3"));
					details.setDisplayOrderLevel4Name(rs.getString("DC_DISPORD_NAME_LEVEL_4"));
					details.setDisplayOrderLevel5Name(rs.getString("DC_DISPORD_NAME_LEVEL_5"));
					details.setDisplayOrderLevel6Name(rs.getString("DC_DISPORD_NAME_LEVEL_6"));
					
					details.setDisplayOrderSourceFileName(rs.getString("DC_DISPORD_TXT_FILE_NAME"));
		    		details.setDisplayOrderSourceFilePath(rs.getString("DC_DISPORD_TXT_FILE_PATH"));
					
					if(null==applicableDisplayOrderDetails || applicableDisplayOrderDetails.size()<=0)
					{
						applicableDisplayOrderDetails = new ArrayList<DisplayOrderDetails>();
					}
					applicableDisplayOrderDetails.add(details);
					details = null;
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;
				details = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getApplicableDisplayOrderForMaterialOrFaceLiftFolder()", e);
		}
		finally
		{
			try
			{
				if(null!= pstmt)
					pstmt.close();pstmt=null;
				if(null!=conn)
					conn.close();conn=null;
				if(null!=rs)
					rs.close();rs=null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "getApplicableDisplayOrderForMaterialOrFaceLiftFolder()", e);
			}
			modelFolderName =null;
			manualType = null;
			materialFolderName = null;
			faceliftFolderName=  null;
		}
		return applicableDisplayOrderDetails;
	}

	public static void deleteDisplayOrderTemp()
	{
		logger.info("deleteDisplayOrderTemp :: ------------ START DELETE PROCESS ------------");
		PreparedStatement pstmt=null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			String sql = "DELETE FROM gms3_dmt_el_gr_nm_dispord";
			pstmt = conn.prepareStatement(sql);
			pstmt.executeUpdate();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteDisplayOrderTemp()", e);
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
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteDisplayOrderTemp()", e);
			}
		}
		logger.info("deleteDisplayOrderTemp :: ------------ END DELETE PROCESS ------------");
	}
	
}