package com.mazda.gms3.dmt.metadataval.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataScheduleConstants;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataValidationUtils;
import com.mazda.gms3.dmt.metadataval.vo.MetaDataFileDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class MetaDataValidationDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(MetaDataValidationDAO.class);
	
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
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getCountryList()", e);
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

	public ArrayList<SelectItemDetails> getLanguageList(String countryLocaleId) throws SQLException 
	{
		//		logger.info("getLanguageList :: Method Starts.");
		ArrayList<SelectItemDetails> languageList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId))
			{
				conn = getConnection();
				String sql = "SELECT MDM_ML_ID,MDM_ML_LANG_CODE,MDM_ML_LANG_DESC FROM gms3_mdm_manual_language WHERE "
						+ "MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'"
						+ "AND MDM_CL_ID="+new Long(countryLocaleId).longValue()+" ORDER BY MDM_ML_LANG_DESC ASC";
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
			else
			{
				logger.info("getLanguageList :: Country Locale Id as parameter is null. Return null.");
				languageList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getLanguageList()", e);
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
			// set param to null
			countryLocaleId = null;
		}
		return languageList;
	}
	
	public Long createSchedule(MetaDataFileDetails details) throws SQLException
	{
		Long scheduleId=null;
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs=null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=details.getFilePath() && !"".equals(details.getFilePath()) && 
					null!=details.getFileName() && !"".equals(details.getFileName()) && 
					null!=details.getLocale() && !"".equals(details.getLocale()))
			{
				
				conn = getConnection();
				conn.setAutoCommit(false);
				// save sch name  - get schId
				String sql="INSERT INTO gms3_dmt_metadata_val_sch (DC_SCHEDULE_NAME,DC_RECORD_STATUS) VALUES ('"+details.getScheduleName()+"','"+AutoSyncConstants.STATUS_ACTIVE+"')";
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
				
				if(scheduleId > 0)
				{
					sql="UPDATE gms3_dmt_metadata_val_sch SET DC_SCHEDULE_NAME = ?, DC_USER_ID=?, DC_STATUS=?,DC_SCH_THREAD_ID=?,"
							+ "DC_SCHEDULE_TMSTP = ?, DC_METADATA_TYPE=?,DC_METADATA_FILE_NAME=?,DC_METADATA_FILE_PATH=?,"
							+ "DC_METADATA_FILE_WEB_PATH=?,DC_LOCALE=? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, details.getScheduleName()+String.valueOf(scheduleId));
					pstmt.setString(2, details.getWslId());
					pstmt.setString(3, details.getStatus());
					pstmt.setString(4, details.getThreadId()+String.valueOf(scheduleId));
					pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
					pstmt.setString(6, details.getMetaDataType());
					pstmt.setString(7, details.getFileName());
					pstmt.setString(8, details.getFilePath());
					pstmt.setString(9, details.getWebFilePath());
					pstmt.setString(10, details.getLocale());
					pstmt.setLong(11, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=null;
					// commit transaction
					conn.commit();
				}
				else
				{
					logger.info("createSchedule :: Failed to Create Schdeule for :: > "+ details.getFileName());
					conn.rollback();
				}
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "createSchedule()", e);
			scheduleId = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=pstmt)
				pstmt.close();
			details= null;
		}
		return scheduleId;
	}

	public ArrayList<MetaDataFileDetails> getScheduleList() throws SQLException
	{
		ArrayList<MetaDataFileDetails> scheduleList = null;
		Connection conn = null;
		ResultSet rs= null;
		PreparedStatement pstmt =null;
		try
		{
			conn = getConnection();
			// add 1 day to current timestamp
			Timestamp currentTime=new Timestamp(new Date().getTime()+1000L*60L*60L*24L*1L);
			// deduct 30 days from current timestamp
			Timestamp beforeTime = new Timestamp(new Date( new Date().getTime() - 1000L*60L*60L*24L*30L).getTime());
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyy");
			currentTime = new Timestamp(sdf.parse(sdf.format(currentTime)).getTime());
			beforeTime = new Timestamp(sdf.parse(sdf.format(beforeTime)).getTime());
			
			/*
			 * FETCH ONLY PAST 30 DAYS RECORDS
			 */
			String sql="SELECT * FROM gms3_dmt_metadata_val_sch WHERE DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_ACTIVE+"' AND DC_SCHEDULE_TMSTP BETWEEN ? AND ? "
					+ " ORDER BY DC_SCHEDULE_ID DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setTimestamp(1, beforeTime);
			pstmt.setTimestamp(2, currentTime);
			rs = pstmt.executeQuery();
			MetaDataFileDetails details = new MetaDataFileDetails();
			while(rs.next())
			{
				if(null==scheduleList || scheduleList.size()<=0)
				{
					scheduleList = new ArrayList<MetaDataFileDetails>();
				}
				details = new MetaDataFileDetails();
				details.setSrNo(scheduleList.size()+1);
				details.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
				details.setScheduleName(rs.getString("DC_SCHEDULE_NAME"));
				details.setWslId(rs.getString("DC_USER_ID"));
				details.setStatus(rs.getString("DC_STATUS"));
				details.setStartTime(rs.getTimestamp("DC_SCHEDULE_TMSTP"));
				details.setFinishTime(rs.getTimestamp("DC_FINISH_TMSTP"));
				details.setTotalCount(rs.getInt("DC_TOTAL_COUNT"));
				details.setSuccessCount(rs.getInt("DC_PROCESSED_COUNT"));
				details.setFailureCount(rs.getInt("DC_FAILED_COUNT"));
				details.setMetaDataType(rs.getString("DC_METADATA_TYPE"));
				details.setFileName(rs.getString("DC_METADATA_FILE_NAME"));
				details.setFilePath(rs.getString("DC_METADATA_FILE_PATH"));
				details.setWebFilePath(com.mazda.gms3.dmt.utils.OkAssetsWeb.url(rs.getString("DC_METADATA_FILE_WEB_PATH")));
				details.setReportsPath(com.mazda.gms3.dmt.utils.OkAssetsWeb.url(rs.getString("DC_REPORTS_PATH")));
				details.setThreadId(rs.getString("DC_SCH_THREAD_ID"));
				details.setLocale(rs.getString("DC_LOCALE"));
				details.setSchdeuleRemarks(rs.getString("DC_REMARKS"));
				scheduleList.add(details);
				details = null;
			}
			details = null;
			sql=null;
			
			currentTime = null;
			beforeTime = null;
			sdf = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getScheduleList()", e);
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
		return scheduleList;
	}

	public boolean updateScheduleStatus(String scheduleId, String status, String remarks)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=status && !"".equals(status))
			{
				conn = getConnection();
				/*
				 * IF STATUS IS PENDING / PROCESSING - DO NOT update finishTimestamp 
				 */
				if(null!=remarks && !"".equals(remarks))
				{
					if(remarks.length()>2000)
					{
						remarks = remarks.substring(0,1999);
					}
				}
				
				String sql="UPDATE gms3_dmt_metadata_val_sch SET DC_STATUS=? ,DC_REMARKS=?  ";
				if(status.equals(MetaDataScheduleConstants.STATUS_SUCCESS) || status.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					sql = sql+", DC_FINISH_TMSTP=? ";
				}
				sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, status);
				pstmt.setString(2, remarks);
				if(status.equals(MetaDataScheduleConstants.STATUS_SUCCESS) || status.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
					pstmt.setLong(4, new Long(scheduleId).longValue());
				}
				else
				{
					pstmt.setLong(3, new Long(scheduleId).longValue());
				}
				pstmt.executeUpdate();
				sql=null;
			}
			else
			{
				logger.info("updateScheduleStatus :: Schedule Id / Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "updateScheduleStatus()", e);
			return false;
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		return true;
	}

	public boolean updateScheduleReportsPath(String scheduleId, String reportsPath)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=reportsPath && !"".equals(reportsPath))
			{
				conn = getConnection();
				String sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_REPORTS_PATH=?  ";
				sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, reportsPath);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				sql=null;
				
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			else
			{
				logger.info("updateScheduleReportsPath :: Schedule Id / Reports Path as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "updateScheduleReportsPath()", e);
			return false;
		}
		return true;
	}
	
	public void updateScheduleTotalAndFailureCount(String scheduleId, int totalCount, int failureCount)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		conn = getConnection();
		/*
		 * IF STATUS IS PENDING / PROCESSING - DO NOT update finishTimestamp 
		 */
		String sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_TOTAL_COUNT=?,DC_FAILED_COUNT=?  ";
		sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, totalCount);
		pstmt.setInt(2, failureCount);
		pstmt.setLong(3, new Long(scheduleId).longValue());
		pstmt.executeUpdate();
		sql=null;
		if(null!=conn)
			conn.close();conn=null;
		if(null!=pstmt)
			pstmt.close();pstmt=null;
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
						sql="UPDATE gms3_dmt_metadata_val_sch SET DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_INACTIVE+"' "
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
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e1);
			}
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e);
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
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e);
			}
			// set passed parameter to null
			scheduleIds=null;
		}
		return true;
	}

	/*
	 * =====================================================================================================
	 * LINE CHECKS - KAPTURE (2026-10-10)
	 *
	 * The legacy checks looked every value up one by one: in MDM, and in InfoManager (OK_IM.TAG). Now:
	 *  - "exists in Kapture" = the reference key is in the Kapture categories table (kapture.category.db.table)
	 *    for ANY locale - the same rule as the locale-free OK_IM.TAG look-up (Kapture creates a category in every
	 *    default locale). Read in ONE pass per file (IN chunks of db.write.batch.size), matched in memory,
	 *    exactly and in upper case, as the conversion matches it.
	 *  - MDM values are read once per file for the locale and matched in memory (same TRIM + LOWER rule).
	 *  - The success / failure counters are written ONCE at the end of the file (addCheckCounts), not per line.
	 *  - Display order and CD lines carry level NAMES now - no level code checks any more.
	 * =====================================================================================================
	 */

	/** Message of a line that misses required nodes; these lines are counted as failed by the caller. */
	private static final String INVALID_LINE_MESSAGE = "Line is INVALID. Required nodes are missing.";

	/**
	 * The reference keys (upper case) of the given keys that exist in Kapture for any locale.
	 * Throws when the look-up itself fails - a failed look-up is not "no such category".
	 */
	private Set<String> findRefKeysInKapture(Collection<String> refKeys) throws SQLException
	{
		Set<String> found = new HashSet<String>();
		List<String> keys = new ArrayList<String>(new LinkedHashSet<String>(refKeys));
		if(keys.isEmpty())
		{
			return found;
		}
		String table = KaptureApiClient.require("kapture.category.db.table").trim();
		int chunk = KaptureApiClient.requireInt("db.write.batch.size");
		long start = System.currentTimeMillis();
		Connection conn = getCMSConnection();
		if(null==conn)
		{
			throw new SQLException("No Kapture CMS connection.");
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			for(int from=0; from<keys.size(); from+=chunk)
			{
				int to = Math.min(from+chunk, keys.size());
				StringBuilder in = new StringBuilder();
				for(int i=from; i<to; i++)
				{
					in.append(i>from ? ",?" : "?");
				}
				pstmt = conn.prepareStatement("SELECT DISTINCT referencekey FROM "+table+" WHERE referencekey IN ("+in+")");
				for(int i=from; i<to; i++)
				{
					pstmt.setString(1+(i-from), keys.get(i));
				}
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					String ref = rs.getString(1);
					if(null!=ref)
					{
						found.add(ref.trim().toUpperCase());
					}
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			conn.close();
		}
		logger.info("findRefKeysInKapture :: "+table+" :: keys "+keys.size()+" :: found "+found.size()+" :: "+(System.currentTimeMillis()-start)+" ms");
		return found;
	}

	/** The Kapture result of one look-up pass: the keys found, or why the look-up failed. */
	private static class KaptureKeys
	{
		Set<String> found = new HashSet<String>();
		String error = null;

		/** null when the key is in Kapture, else the line error for it ("<what> does not exist in Kapture."). */
		String check(String refKey, String what)
		{
			if(null!=error)
			{
				return "Failed to find "+what+" in Kapture, exception while executing Query ->"+error+".";
			}
			if(null==refKey || "".equals(refKey) || !found.contains(refKey.trim().toUpperCase()))
			{
				return what+" does not exist in Kapture.";
			}
			return null;
		}
	}

	private KaptureKeys lookUpInKapture(Collection<String> refKeys, String caller)
	{
		KaptureKeys result = new KaptureKeys();
		try
		{
			result.found = findRefKeysInKapture(refKeys);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), caller, e);
			result.error = e.getMessage();
		}
		return result;
	}

	/** Adds the success / failure counts of the checked lines to the schedule row, in one statement. */
	private void addCheckCounts(String scheduleId, int successCount, int failureCount)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			pstmt = conn.prepareStatement("UPDATE gms3_dmt_metadata_val_sch SET dc_processed_count = IFNULL(dc_processed_count,0) + ?,"
					+ " dc_failed_count = IFNULL(dc_failed_count,0) + ? WHERE dc_schedule_id = ?");
			pstmt.setInt(1, successCount);
			pstmt.setInt(2, failureCount);
			pstmt.setLong(3, Long.parseLong(scheduleId));
			pstmt.executeUpdate();
			logger.info("addCheckCounts :: schedule "+scheduleId+" :: success "+successCount+" :: failure "+failureCount);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "addCheckCounts()", e);
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
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "addCheckCounts()", e);
			}
		}
	}

	/** trim + lower case, the MDM comparison rule of the legacy queries (TRIM(LOWER(col)) = ?); null stays null. */
	private static String mdmKey(String value)
	{
		return null==value ? null : value.trim().toLowerCase();
	}

	/**
	 * The active MDM VINs of the locale, as mdmKey(wmi)|vds|vis start|vis end|carline - the columns of the legacy
	 * per-line query.
	 */
	private Set<String> loadMdmVins(Connection conn, String locale) throws SQLException
	{
		Set<String> vins = new HashSet<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT mdm_vin_wmi_code, mdm_vin_vds_code, mdm_vin_vis_start_range, mdm_vin_vis_end_range, mdm_crln_code"
					+ " FROM gms3_mdm_vin_detail WHERE TRIM(LOWER(mdm_ml_lang_code)) = ? AND mdm_vin_flag = ?");
			pstmt.setString(1, mdmKey(locale));
			pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				vins.add(vinKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
		logger.info("loadMdmVins :: locale "+locale+" :: active VINs "+vins.size());
		return vins;
	}

	private static String vinKey(String wmi, String vds, String visStart, String visEnd, String carline)
	{
		return mdmKey(wmi)+"|"+mdmKey(vds)+"|"+mdmKey(visStart)+"|"+mdmKey(visEnd)+"|"+mdmKey(carline);
	}

	/**
	 * The active codes (mdmKey) of one MDM master data table for the locale - engine / transmission / axle / body type.
	 */
	private Set<String> loadMdmCodes(Connection conn, String table, String flagColumn, String codeColumn, String locale) throws SQLException
	{
		Set<String> codes = new HashSet<String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT a."+codeColumn+" FROM "+table+" a, gms3_mdm_manual_language b WHERE a."+flagColumn+" = ?"
					+ " AND b.mdm_ml_flag = ? AND a.mdm_ml_id = b.mdm_ml_id AND TRIM(LOWER(b.mdm_ml_lang_code)) = ?");
			pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
			pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
			pstmt.setString(3, mdmKey(locale));
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				if(null!=rs.getString(1))
				{
					codes.add(mdmKey(rs.getString(1)));
				}
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
		return codes;
	}

	/**
	 * The ESI category names of the locale's MDM (English names, as legacy), keyed "c", "c|s", "c|s|ss" on mdmKey of
	 * the codes. A key with a blank name is kept with "" - legacy treats a blank name as "does not exist in MDM".
	 */
	private Map<String, String> loadMdmEsiNames(Connection conn, String locale) throws SQLException
	{
		Map<String, String> names = new HashMap<String, String>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT a.mdm_cat_code, a.mdm_scat_code, a.mdm_sscat_code, a.mdm_cat_name_eng_lang,"
					+ " a.mdm_scat_name_eng_lang, a.mdm_sscat_name_eng_lang FROM gms3_mdm_category a, gms3_mdm_manual_language b"
					+ " WHERE a.mdm_ml_id = b.mdm_ml_id AND TRIM(LOWER(b.mdm_ml_lang_code)) = ? AND a.mdm_cat_flag = ? AND b.mdm_ml_flag = ?");
			pstmt.setString(1, mdmKey(locale));
			pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
			pstmt.setString(3, ApplicationProperties.getProperty("flag.value.active"));
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				String c = mdmKey(rs.getString(1));
				String s = mdmKey(rs.getString(2));
				String ss = mdmKey(rs.getString(3));
				putName(names, c, rs.getString(4));
				if(null!=c && null!=s)
				{
					putName(names, c+"|"+s, rs.getString(5));
					if(null!=ss)
					{
						putName(names, c+"|"+s+"|"+ss, rs.getString(6));
					}
				}
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
		logger.info("loadMdmEsiNames :: locale "+locale+" :: keys "+names.size());
		return names;
	}

	private static void putName(Map<String, String> names, String key, String name)
	{
		if(null==key)
		{
			return;
		}
		String n = null==name ? "" : name;
		String old = names.get(key);
		if(null==old || ("".equals(old.trim()) && !"".equals(n.trim())))
		{
			names.put(key, n);
		}
	}

	/** The comma separated codes of a display order type field, trimmed, blanks and the given "none" code left out. */
	private static List<String> typeCodes(String field, String noneCode)
	{
		List<String> codes = new ArrayList<String>();
		if(null!=field && !"".equals(field))
		{
			for(String t : field.split(","))
			{
				if(null!=t && !"".equals(t.trim()) && (null==noneCode || !noneCode.equals(t.trim().toLowerCase())))
				{
					codes.add(t.trim());
				}
			}
		}
		return codes;
	}

	/** Kapture reference key of an engine / transmission / axle / body type code - as the legacy IM look-up built it. */
	private static String typeRefKey(String code)
	{
		return Utilities.replaceCharsForRefKeys(code).trim().toUpperCase();
	}

	private static String join(List<String> values)
	{
		StringBuilder sb = new StringBuilder();
		for(String v : values)
		{
			if(sb.length()>0)
			{
				sb.append(",");
			}
			sb.append(v);
		}
		return sb.length()>0 ? sb.toString() : null;
	}

	/**
	 * MNAO model year category of a vin.txt line - the conversion's rule (conversion/impl/CategoryUtils
	 * processModelYearCategories): model name without '-', reference key characters replaced, upper case, '_',
	 * model year without "MY", upper case. e.g. CX90 + 2026MY -> CX90_2026.
	 */
	private static String modelYearRefKey(String modelName, String modelYear)
	{
		if(null==modelName || "".equals(modelName) || null==modelYear || "".equals(modelYear))
		{
			return null;
		}
		String name = Utilities.replaceCharsForRefKeys(modelName.replace("-", ""));
		String year = modelYear;
		if(year.trim().toLowerCase().contains("my"))
		{
			year = year.replace("my", "").replace("MY", "").replace("mY", "").replace("My", "");
		}
		return name.toUpperCase()+"_"+year.toUpperCase();
	}

	/**
	 * VIN lines of MC / MME (vin.txt, 8 / 9 fields) and MNAO (vin.txt, 11 fields).
	 * Per valid line: the VIN is active in MDM (locale = alternate locale of the processing locale - CARLINE for MC / MME,
	 * VIN for MNAO), the VIN category is in Kapture, the line names its display order file; MNAO also: the model year
	 * category is in Kapture.
	 */
	private ArrayList<VinDetails> checkVINs(ArrayList<VinDetails> vinList, String locale, String scheduleId, boolean mnao)throws SQLException
	{
		if(null==vinList || null==locale || "".equals(locale))
		{
			logger.info("checkVINs :: VIN LIST / Locale as parameters are null. Return as it is.");
			return vinList;
		}
		// MDM VINs of the locale - one read
		Set<String> mdmVins = null;
		String mdmError = null;
		Connection conn = getConnection();
		try
		{
			String mdmLocale = ScheduleDAO.getAlternateLocale(locale.replace("_", "-"),
					mnao ? AutoSyncConstants.ITEM_KEY_VIN_RANGE : AutoSyncConstants.ITEM_KEY_CARLINE, conn);
			mdmVins = loadMdmVins(conn, mdmLocale);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINs()", e);
			mdmError = e.getMessage();
		}
		finally
		{
			conn.close();
		}

		// reference keys of every valid line, then one Kapture look-up
		List<String> refKeys = new ArrayList<String>();
		for(VinDetails details : vinList)
		{
			if(MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				String refKey;
				if(mnao)
				{
					// WMI + VDS + VIS START + VIS END (as the conversion's MNAO VIN category)
					refKey = details.getWmiCode()+details.getVdsCode()+details.getVisStartRange()+details.getVisEndRange();
					details.setModelYearRefKey(modelYearRefKey(details.getModelName(), details.getModelYear()));
					if(null!=details.getModelYearRefKey())
					{
						refKeys.add(details.getModelYearRefKey());
					}
				}
				else
				{
					// CARLINE + WMI ('-' = 3 underscores) + VDS + VIS START + VIS END
					String wmiCode = "-".equals(details.getWmiCode()) ? "___" : details.getWmiCode();
					refKey = details.getCarlineCode()+wmiCode+details.getVdsCode()+details.getVisStartRange()+details.getVisEndRange();
				}
				details.setVinRefKey(Utilities.replaceCharsForRefKeys(refKey).trim().toUpperCase());
				refKeys.add(details.getVinRefKey());
			}
		}
		KaptureKeys kapture = lookUpInKapture(refKeys, "checkVINs()");

		int successCount = 0;
		int failureCount = 0;
		for(VinDetails details : vinList)
		{
			details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
			details.setVinFoundInMDM("N");
			details.setVinFoundInIM("N");
			if(!MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				// already counted with the invalid lines
				details.setErrorMessage(INVALID_LINE_MESSAGE);
				continue;
			}
			StringBuilder errorMessage = new StringBuilder();
			if(null!=mdmError)
			{
				errorMessage.append("Failed to find VIN in MDM, exception while executing Query ->"+mdmError+".");
			}
			else if(mdmVins.contains(vinKey(details.getWmiCode(), details.getVdsCode(), details.getVisStartRange(), details.getVisEndRange(), details.getCarlineCode())))
			{
				details.setVinFoundInMDM("Y");
			}
			else
			{
				errorMessage.append("VIN does not exist in MDM.");
			}

			String error = kapture.check(details.getVinRefKey(), "VIN");
			if(null==error)
			{
				details.setVinFoundInIM("Y");
			}
			else
			{
				errorMessage.append(error);
			}

			if(mnao)
			{
				details.setModelYearFoundInKapture("N");
				error = kapture.check(details.getModelYearRefKey(), "Model Year {"+details.getModelYearRefKey()+"}");
				if(null==error)
				{
					details.setModelYearFoundInKapture("Y");
				}
				else
				{
					errorMessage.append(error);
				}
			}

			if(null==details.getDisplayOrderTextFileName() || "".equals(details.getDisplayOrderTextFileName()))
			{
				errorMessage.append("No entry found for Display Order Text File.");
			}

			if(errorMessage.length()>0)
			{
				details.setErrorMessage(errorMessage.toString());
				failureCount++;
			}
			else
			{
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				successCount++;
			}
		}
		addCheckCounts(scheduleId, successCount, failureCount);
		return vinList;
	}

	public ArrayList<VinDetails> checkMCMMEVINStatus(ArrayList<VinDetails> vinList, String locale, String scheduleId)throws SQLException
	{
		return checkVINs(vinList, locale, scheduleId, false);
	}

	public ArrayList<VinDetails> checkMNAOVINStatus(ArrayList<VinDetails> vinList, String locale, String scheduleId)throws SQLException
	{
		return checkVINs(vinList, locale, scheduleId, true);
	}

	/**
	 * Display order lines. The level fields carry NAMES now (no code check). For a model that is not ENGINE / AT / MT, every
	 * engine / transmission / drive axle / body type code must be active in MDM (alternate locale of its type) AND be a
	 * category in Kapture. E99 / M99 (no engine / transmission) are not checked, as legacy.
	 */
	public ArrayList<DisplayOrderDetails> checkDisplayOrderStatus(ArrayList<DisplayOrderDetails> displayOrderList, String locale, String scheduleId)throws SQLException
	{
		if(null==displayOrderList || null==locale || "".equals(locale))
		{
			logger.info("checkDisplayOrderStatus :: DISPLAY ORDER LIST / Locale as parameters are null. Return as it is.");
			return displayOrderList;
		}
		// MDM codes of the four types - one read each
		Set<String> engines = null, missions = null, axles = null, bodies = null;
		String mdmError = null;
		Connection conn = getConnection();
		try
		{
			engines = loadMdmCodes(conn, "gms3_mdm_engine_type", "mdm_et_flag", "mdm_et_type_code",
					ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_ENGINE_TYPE, conn));
			missions = loadMdmCodes(conn, "gms3_mdm_trans_type", "mdm_trans_type_flag", "mdm_trans_type_type_code",
					ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE, conn));
			axles = loadMdmCodes(conn, "gms3_mdm_axle_type", "mdm_at_flag", "mdm_at_axle_type",
					ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_AXLE_TYPE, conn));
			bodies = loadMdmCodes(conn, "gms3_mdm_body_type", "mdm_bt_flag", "mdm_bt_body_type",
					ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_BODY_TYPE, conn));
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e);
			mdmError = e.getMessage();
		}
		finally
		{
			conn.close();
		}

		List<String> refKeys = new ArrayList<String>();
		for(DisplayOrderDetails details : displayOrderList)
		{
			if(MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()) && !details.isEngineATMT())
			{
				List<String> all = new ArrayList<String>();
				all.addAll(typeCodes(details.getEngineType(), "e99"));
				all.addAll(typeCodes(details.getMissionType(), "m99"));
				all.addAll(typeCodes(details.getDriveAxleType(), null));
				all.addAll(typeCodes(details.getBodyType(), null));
				for(String code : all)
				{
					refKeys.add(typeRefKey(code));
				}
			}
		}
		KaptureKeys kapture = lookUpInKapture(refKeys, "checkDisplayOrderStatus()");

		int successCount = 0;
		int failureCount = 0;
		for(DisplayOrderDetails details : displayOrderList)
		{
			details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
			if(!MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				details.setErrorMessage(INVALID_LINE_MESSAGE);
				continue;
			}
			StringBuilder errorMessage = new StringBuilder();
			if(!details.isEngineATMT())
			{
				// ENGINE TYPES
				List<String> failedMDM = new ArrayList<String>();
				List<String> failedKapture = new ArrayList<String>();
				checkTypeCodes(typeCodes(details.getEngineType(), "e99"), "Engine Type", engines, mdmError, kapture, errorMessage, failedMDM, failedKapture);
				details.setFailedEngineTypeCodesMDM(join(failedMDM));
				details.setFailedEngineTypeCodesIM(join(failedKapture));
				// TRANSMISSION TYPES
				failedMDM = new ArrayList<String>();
				failedKapture = new ArrayList<String>();
				checkTypeCodes(typeCodes(details.getMissionType(), "m99"), "Transmission Type", missions, mdmError, kapture, errorMessage, failedMDM, failedKapture);
				details.setFailedMissionTypeCodesMDM(join(failedMDM));
				details.setFailedMissionTypeCodesIM(join(failedKapture));
				// DRIVE AXLE TYPES
				failedMDM = new ArrayList<String>();
				failedKapture = new ArrayList<String>();
				checkTypeCodes(typeCodes(details.getDriveAxleType(), null), "DriveAxle Type", axles, mdmError, kapture, errorMessage, failedMDM, failedKapture);
				details.setFailedDriveAxleTypeCodesMDM(join(failedMDM));
				details.setFailedDriveAxleTypeCodesIM(join(failedKapture));
				// BODY TYPES
				failedMDM = new ArrayList<String>();
				failedKapture = new ArrayList<String>();
				checkTypeCodes(typeCodes(details.getBodyType(), null), "Body Type", bodies, mdmError, kapture, errorMessage, failedMDM, failedKapture);
				details.setFailedBodyTypeCodesMDM(join(failedMDM));
				details.setFailedBodyTypeCodesIM(join(failedKapture));
			}
			if(errorMessage.length()>0)
			{
				details.setErrorMessage(errorMessage.toString());
				failureCount++;
			}
			else
			{
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				successCount++;
			}
		}
		addCheckCounts(scheduleId, successCount, failureCount);
		return displayOrderList;
	}

	/** One type field of a display order line: each code in MDM and in Kapture; failures go to the message and the lists. */
	private static void checkTypeCodes(List<String> codes, String what, Set<String> mdmCodes, String mdmError, KaptureKeys kapture,
			StringBuilder errorMessage, List<String> failedMDM, List<String> failedKapture)
	{
		for(String code : codes)
		{
			if(null!=mdmError)
			{
				errorMessage.append("Failed to find "+what+" {"+code+"} in MDM, exception while executing Query ->"+mdmError+".");
				failedMDM.add(code);
			}
			else if(!mdmCodes.contains(mdmKey(code)))
			{
				errorMessage.append(what+" {"+code+"} does not exist in MDM.");
				failedMDM.add(code);
			}
			String error = kapture.check(typeRefKey(code), what+" {"+code+"}");
			if(null!=error)
			{
				errorMessage.append(error);
				failedKapture.add(code);
			}
		}
	}

	/**
	 * CD processing lines. The level fields carry NAMES now (no code check); the sequence number is mandatory.
	 */
	public ArrayList<CDProcessingDetails> checkCDProcessingDisplayOrderStatus(ArrayList<CDProcessingDetails> cdProcessingList, String locale, String scheduleId)throws SQLException
	{
		if(null==cdProcessingList || null==locale || "".equals(locale))
		{
			logger.info("checkCDProcessingDisplayOrderStatus :: CD PROCESSING LIST / Locale as parameters are null. Return as it is.");
			return cdProcessingList;
		}
		int successCount = 0;
		int failureCount = 0;
		for(CDProcessingDetails details : cdProcessingList)
		{
			details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
			if(!MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				details.setErrorMessage(INVALID_LINE_MESSAGE);
				continue;
			}
			if(null==details.getSequenceNo() || "".equals(details.getSequenceNo()))
			{
				details.setErrorMessage("No Sequence number found.");
				failureCount++;
			}
			else
			{
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				successCount++;
			}
		}
		addCheckCounts(scheduleId, successCount, failureCount);
		return cdProcessingList;
	}

	/**
	 * ESI category lines (esicat / esi*.txt). Not for the OLD model types: level 1 / 2 / 3 codes in MDM (alternate ESI
	 * locale, English names) and in Kapture (ESI + level 1, level 2, level 2 + level 3; level 3 "00" is not checked).
	 * All types: the title is mandatory; wiring diagram types: every steering type is a category in Kapture.
	 */
	public ArrayList<ESICategoryDetails> checkESICategoryStatus(ArrayList<ESICategoryDetails> esiCategoryList, String locale, String scheduleId, String metaDataType)throws SQLException
	{
		if(null==esiCategoryList || null==locale || "".equals(locale))
		{
			logger.info("checkESICategoryStatus :: ESI CATEGORY LIST / Locale as parameters are null. Return as it is.");
			return esiCategoryList;
		}
		boolean checkCodes = !metaDataType.equals("ESICATEGORY_MANUALS_OLD") && !metaDataType.equals("ESICATEGORY_WD_OLD") && !metaDataType.equals("ESICATEGORY_OLD");
		boolean wiringDiagram = metaDataType.equals("ESICATEGORY_WD_NEW") || metaDataType.equals("ESICATEGORY_WD_OLD");
		String esiPrefix = ApplicationProperties.getProperty("prefix.esicategory.type.refkey");

		Map<String, String> mdmNames = null;
		String mdmError = null;
		if(checkCodes)
		{
			Connection conn = getConnection();
			try
			{
				mdmNames = loadMdmEsiNames(conn, ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_ESI_CATEGORY, conn));
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e);
				mdmError = e.getMessage();
			}
			finally
			{
				conn.close();
			}
		}

		// reference keys of every valid line, then one Kapture look-up
		List<String> refKeys = new ArrayList<String>();
		for(ESICategoryDetails details : esiCategoryList)
		{
			if(!MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				continue;
			}
			if(checkCodes && notBlank(details.getCategoryLevel1Code()))
			{
				details.setCode1RefKey(esiPrefix+details.getCategoryLevel1Code().trim().toUpperCase());
				refKeys.add(details.getCode1RefKey());
				if(notBlank(details.getCategoryLevel2Code()))
				{
					details.setCode2RefKey(details.getCategoryLevel2Code().trim().toUpperCase());
					refKeys.add(details.getCode2RefKey());
					if(notBlank(details.getCategoryLevel3Code()) && !"00".equals(details.getCategoryLevel3Code()))
					{
						details.setCode3RefKey(details.getCategoryLevel2Code().trim().toUpperCase()+details.getCategoryLevel3Code().trim().toUpperCase());
						refKeys.add(details.getCode3RefKey());
					}
				}
			}
			if(wiringDiagram)
			{
				for(String steeringType : typeCodes(details.getSteeringTypeInfoText(), null))
				{
					String ref = steeringTypeRefKey(steeringType);
					if(null!=ref)
					{
						refKeys.add(ref);
					}
				}
			}
		}
		KaptureKeys kapture = lookUpInKapture(refKeys, "checkESICategoryStatus()");

		int successCount = 0;
		int failureCount = 0;
		for(ESICategoryDetails details : esiCategoryList)
		{
			details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
			if(!MetaDataValidationUtils.LINE_TYPE_VALID.equals(details.getLineType()))
			{
				details.setErrorMessage(INVALID_LINE_MESSAGE);
				continue;
			}
			StringBuilder errorMessage = new StringBuilder();
			if(checkCodes)
			{
				if(notBlank(details.getCategoryLevel1Code()))
				{
					String c = mdmKey(details.getCategoryLevel1Code());
					// LEVEL 1
					details.setCode1ExistsInMDM("N");
					details.setCategoryLevel1Name(esiName(mdmNames, c, mdmError, "Level 1", errorMessage));
					if(notBlank(details.getCategoryLevel1Name()))
					{
						details.setCode1ExistsInMDM("Y");
					}
					details.setCode1ExistsInIM("N");
					String error = kapture.check(details.getCode1RefKey(), "ESI Category Level 1");
					if(null==error)
					{
						details.setCode1ExistsInIM("Y");
					}
					else
					{
						errorMessage.append(error);
					}
					// LEVEL 2
					if(notBlank(details.getCategoryLevel2Code()))
					{
						String s = mdmKey(details.getCategoryLevel2Code());
						details.setCode2ExistsInMDM("N");
						details.setCategoryLevel2Name(esiName(mdmNames, c+"|"+s, mdmError, "Level 2", errorMessage));
						if(notBlank(details.getCategoryLevel2Name()))
						{
							details.setCode2ExistsInMDM("Y");
						}
						details.setCode2ExistsInIM("N");
						error = kapture.check(details.getCode2RefKey(), "ESI Category Level 2");
						if(null==error)
						{
							details.setCode2ExistsInIM("Y");
						}
						else
						{
							errorMessage.append(error);
						}
						// LEVEL 3
						if(notBlank(details.getCategoryLevel3Code()) && !"00".equals(details.getCategoryLevel3Code()))
						{
							String ss = mdmKey(details.getCategoryLevel3Code());
							details.setCode3ExistsInMDM("N");
							details.setCategoryLevel3Name(esiName(mdmNames, c+"|"+s+"|"+ss, mdmError, "Level 3", errorMessage));
							if(notBlank(details.getCategoryLevel3Name()))
							{
								details.setCode3ExistsInMDM("Y");
							}
							details.setCode3ExistsInIM("N");
							error = kapture.check(details.getCode3RefKey(), "ESI Category Level 3");
							if(null==error)
							{
								details.setCode3ExistsInIM("Y");
							}
							else
							{
								errorMessage.append(error);
							}
						}
					}
				}
				else
				{
					errorMessage.append("No ESI Category Codes found.");
				}
			}

			// the title is mandatory in an ESI file
			if(null==details.getTitle() || "".equals(details.getTitle()))
			{
				errorMessage.append("No Title found.");
			}

			// wiring diagrams: every steering type must be a category in Kapture
			if(wiringDiagram)
			{
				List<String> failedSteeringTypes = new ArrayList<String>();
				for(String steeringType : typeCodes(details.getSteeringTypeInfoText(), null))
				{
					String error = kapture.check(steeringTypeRefKey(steeringType), "Steering Type {"+steeringType+"}");
					if(null!=error)
					{
						errorMessage.append(error);
						failedSteeringTypes.add(steeringType);
					}
				}
				details.setFailedSteeringTypeInIM(join(failedSteeringTypes));
			}

			if(errorMessage.length()>0)
			{
				details.setErrorMessage(errorMessage.toString());
				failureCount++;
			}
			else
			{
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				successCount++;
			}
		}
		addCheckCounts(scheduleId, successCount, failureCount);
		return esiCategoryList;
	}

	private static boolean notBlank(String value)
	{
		return null!=value && !"".equals(value);
	}

	/** MDM name of an ESI level, "" when missing (the line error is appended); legacy reads a blank name as missing too. */
	private static String esiName(Map<String, String> mdmNames, String key, String mdmError, String level, StringBuilder errorMessage)
	{
		if(null!=mdmError)
		{
			errorMessage.append("Failed to fetch ESI Category "+level+" from MDM, exception while executing Query ->"+mdmError+".");
			return "";
		}
		String name = mdmNames.get(key);
		if(null==name || "".equals(name))
		{
			errorMessage.append("ESI Category "+level+", does not exist in MDM.");
			return "";
		}
		return name;
	}

	/** Kapture reference key of a steering type label (LHD / RHD), null for any other label - which then does not exist. */
	private static String steeringTypeRefKey(String steeringType)
	{
		String t = steeringType.trim().toLowerCase();
		if(t.equals(ApplicationProperties.getProperty("steering.type.label.lhd")))
		{
			return ApplicationProperties.getProperty("steering.type.refkey.lhd");
		}
		if(t.equals(ApplicationProperties.getProperty("steering.type.label.rhd")))
		{
			return ApplicationProperties.getProperty("steering.type.refkey.rhd");
		}
		return null;
	}

}