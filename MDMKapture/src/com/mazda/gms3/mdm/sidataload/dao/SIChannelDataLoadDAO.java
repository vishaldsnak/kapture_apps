package com.mazda.gms3.mdm.sidataload.dao;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelErrorsDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelScheduleDetails;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class SIChannelDataLoadDAO extends DBConnectionHelper{
	
	private static Logger logger = LogManager.getLogger(SIChannelDataLoadDAO.class);
	
	public static long createSchedule(SIChannelScheduleDetails schDetails)
	{
		long scheduleId=0;
		Connection conn = null;
		PreparedStatement pstmt=null;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql ="INSERT INTO gms3_mdm_si_dl_schedule(mdm_sch_name,mdm_cl_id,mdm_ml_id,mdm_doc_locale) "
					+ "VALUES('"+schDetails.getScheduleName()+"',"+schDetails.getCountryLocaleId()+","+schDetails.getManualLanguageId()+",'"+schDetails.getLocale()+"')";
			stmt= conn.createStatement();
			String generatedColumns[] = { "mdm_sch_id" };
			stmt.execute(sql, generatedColumns);
			rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				scheduleId = rs.getLong(1);
			}
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			sql = null;
			
			if(scheduleId>0)
			{
				logger.info("createSchedule :: SCHEDULE CREATED FOR SI CHANNEL DATA LOAD. PROCEED FOR UPDATING OTHER DETAILS.");
				sql  ="UPDATE gms3_mdm_si_dl_schedule SET mdm_sch_name = ?,mdm_doc_id = ?,"
						+ "mdm_wsl_id=?,mdm_schedule_tmstp = ?,"
						+ "mdm_sch_status = ? WHERE mdm_sch_id = ?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, schDetails.getScheduleName()+String.valueOf(scheduleId));
				if(null!=schDetails.getDocumentId())
				{
					pstmt.setString(2, schDetails.getDocumentId().trim());
				}
				else
				{
					pstmt.setNull(2, Types.VARCHAR);
				}
				pstmt.setString(3, schDetails.getWslId());
				pstmt.setTimestamp(4, schDetails.getScheduleTime());
				pstmt.setString(5, schDetails.getScheduleStatus());
				pstmt.setLong(6, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
			}
			else
			{
				logger.info("createSchedule :: FAILED TO CREATE SCHEDULE FOR SI CHANNEL DATA LOAD.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "createSchedule()", e);
			scheduleId = 0;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();pstmt=null;
				if(null!=conn)
					conn.close();conn=null;
				if(null!=stmt)
					stmt.close();stmt=null;
				if(null!=rs)
					rs.close();rs=null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "createSchedule()", e);
			}
		}
		return scheduleId;
	}
	
	public static void updateScheduleCompletionDetails(SIChannelScheduleDetails schDetails, List<SIChannelErrorsDetails> errorsList)
	{
		/*
		 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
		 *
		 * The worker stops cooperatively (Thread.stop() is removed in Java 20), so it can still
		 * reach this on its way out and would flip 'Aborted' to Completed or Failure, losing the
		 * abort the user just performed. Refusing the write here covers every branch that calls
		 * this, in the one place they all funnel through.
		 */
		if(isAborted(String.valueOf(schDetails.getScheduleId())))
		{
			logger.info("updateScheduleCompletionDetails :: SCHEDULE {"+String.valueOf(schDetails.getScheduleId())+"} WAS ABORTED - leaving its status as Aborted"
					+" instead of writing {"+"completion"+"}.");
			return;
		}
		PreparedStatement pstmt=null;
		Connection conn = null;
		try
		{
			String errorMessage="";
			if(null!=errorsList && errorsList.size()>0)
			{
				SIChannelErrorsDetails details = null;
				for(int a=0;a<errorsList.size();a++)
				{
					details = (SIChannelErrorsDetails)errorsList.get(a);
					if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
					{
						errorMessage+=details.getErrorMessage()+"\n";
					}
					details = null;
				}
				details = null;
			}
			
			byte[] errorData = errorMessage.getBytes();
			InputStream is = new ByteArrayInputStream(errorData);
			conn = getConnection();
			String sql = "UPDATE gms3_mdm_si_dl_schedule SET mdm_doc_id = ? ,mdm_doc_fetched_version = ?,mdm_doc_modified_version = ?,mdm_doc_status=?,"
					+ "mdm_finish_tmstp = ?,mdm_zip_file_path = ?,mdm_sch_status = ?,mdm_sch_reports_path = ?,"
					+ "mdm_okassets_total_count = ?,mdm_okassets_success_count = ?,mdm_okassets_failure_count= ?,mdm_sch_errors = ? WHERE mdm_sch_id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, schDetails.getDocumentId());
			pstmt.setString(2, schDetails.getFetchedVersion());
			pstmt.setString(3, schDetails.getModifiedVersion());
			pstmt.setString(4, schDetails.getDocumentStatus());
			pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
			pstmt.setString(6, schDetails.getZipFilePath());
			pstmt.setString(7, schDetails.getScheduleStatus());
			pstmt.setString(8, schDetails.getReportsPath());
			pstmt.setLong(9, schDetails.getOkAssetsTotalCount());
			pstmt.setLong(10, schDetails.getOkAssetsSuccessCount());
			pstmt.setLong(11, schDetails.getOkAssetsFailureCount());
			pstmt.setBinaryStream(12, is, errorData.length);
			pstmt.setLong(13, schDetails.getScheduleId());
			pstmt.executeUpdate();
			is.close();is = null;
			errorData = null;
			errorMessage = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateScheduleCompletionDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();pstmt=null;
				if(null!=conn)
					conn.close();conn=null;
					
				errorsList = null;	
				schDetails = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateScheduleCompletionDetails()", e);
			}
		}
	}

	public static void updateScheduleProcessingStatus(String status, long scheduleId)
	{
		PreparedStatement pstmt=null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			String sql = "UPDATE gms3_mdm_si_dl_schedule SET mdm_sch_status = ? WHERE mdm_sch_id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, status);
			pstmt.setLong(2, scheduleId);
			pstmt.executeUpdate();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateScheduleProcessingStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();pstmt=null;
				if(null!=conn)
					conn.close();conn=null;
					
				status = null;
				scheduleId=0;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateScheduleProcessingStatus()", e);
			}
		}
	}

	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button writes mdm_sch_status = aborted through updateAbortStatus() below, and does
	 * so whether or not it could find the running thread. The database therefore already carries
	 * the abort signal and this only reads it back.
	 *
	 * WHY THE DATABASE AND NOT A Thread REFERENCE. The screen used to call Thread.stop() on the
	 * worker. That was REMOVED in Java 20 and throws UnsupportedOperationException, which the
	 * caller's catch(Exception) swallowed - so the schedule was marked Aborted while the worker ran
	 * on to completion. Nothing can kill another thread any more; the worker has to stop itself.
	 *
	 * Returns FALSE on any error: a failed status read must never abort a healthy run.
	 */
	public static boolean isAborted(String scheduleId)
	{
		boolean aborted = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				if(null!=conn)
				{
					String sql="SELECT mdm_sch_status FROM gms3_mdm_si_dl_schedule WHERE mdm_sch_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, scheduleId.trim());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						String status = rs.getString("mdm_sch_status");
						String abortedValue = ScheduleConstants.STATUS_ABORTED;
						if(null!=status && null!=abortedValue
								&& status.trim().equalsIgnoreCase(abortedValue.trim()))
						{
							aborted = true;
						}
						status = null;
						abortedValue = null;
					}
					sql = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "isAborted()", e);
		}
		finally
		{
			try
			{
				if(null!=rs) rs.close();
				if(null!=pstmt) pstmt.close();
				if(null!=conn) conn.close();
			}
			catch(Exception re)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "isAborted()", re);
			}
			rs = null;
			pstmt = null;
			conn = null;
		}
		return aborted;
	}


	public static void updateAbortStatus(String scheduleId)
	{
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="UPDATE gms3_mdm_si_dl_schedule SET mdm_sch_status='"+ScheduleConstants.STATUS_ABORTED+"', "
						+ "  mdm_finish_tmstp = ? "
						+ " WHERE mdm_sch_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateAbortStatus()", e);
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "updateAbortStatus()", re);
			}
			scheduleId = null;
		}
	}

	public static ArrayList<SIChannelScheduleDetails> getScheduleList(String wslId)
	{
		ArrayList<SIChannelScheduleDetails> scheduleList = null;
		Connection conn = null;
		ResultSet rs =null;
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
			
			String sql ="SELECT * FROM gms3_mdm_si_dl_schedule WHERE mdm_wsl_id = ? AND mdm_schedule_tmstp BETWEEN ? AND ? ORDER BY mdm_sch_id DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, wslId);
			pstmt.setTimestamp(2, beforeTime);
			pstmt.setTimestamp(3, currentTime);
			rs= pstmt.executeQuery();
			SIChannelScheduleDetails details = null;
			int count=0;
			while(rs.next())
			{
				details = new SIChannelScheduleDetails();
				count++;
				details.setSrNo(count);
				details.setScheduleId(rs.getLong("mdm_sch_id"));
				details.setScheduleName(rs.getString("mdm_sch_name"));
				details.setDocumentId(rs.getString("mdm_doc_id"));
				details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
				details.setManualLanguageId(rs.getLong("mdm_ml_id"));
				details.setLocale(rs.getString("mdm_doc_locale"));
				details.setFetchedVersion(rs.getString("mdm_doc_fetched_version"));
				details.setModifiedVersion(rs.getString("mdm_doc_modified_version"));
				details.setDocumentStatus(rs.getString("mdm_doc_status"));
				details.setWslId(rs.getString("mdm_wsl_id"));
				details.setScheduleTime(rs.getTimestamp("mdm_schedule_tmstp"));
				details.setFinishTime(rs.getTimestamp("mdm_finish_tmstp"));
				details.setZipFilePath(rs.getString("mdm_zip_file_path"));
				details.setScheduleStatus(rs.getString("mdm_sch_status"));
				if(null!=rs.getBytes("mdm_sch_errors"))
				{
					details.setErrorsComplete(new String(rs.getBytes("mdm_sch_errors")));
				}
				if(null!=details.getErrorsComplete() && !"".equals(details.getErrorsComplete()))
				{
					if(details.getErrorsComplete().length()>100)
					{
						details.setErrorsShort(details.getErrorsComplete().substring(0,99));
					}
					else
					{
						details.setErrorsShort(details.getErrorsComplete());
					}
					
				}
				details.setReportsPath(rs.getString("mdm_sch_reports_path"));
				details.setOkAssetsTotalCount(rs.getLong("mdm_okassets_total_count"));
				details.setOkAssetsSuccessCount(rs.getLong("mdm_okassets_success_count"));
				details.setOkAssetsFailureCount(rs.getLong("mdm_okassets_failure_count"));
				
				if(null==scheduleList || scheduleList.size()<=0)
				{
					scheduleList = new ArrayList<SIChannelScheduleDetails>();
				}
				scheduleList.add(details);
				details = null;
			}
			sql = null;
			currentTime = null;
			beforeTime = null;
			sdf = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "getScheduleList()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();pstmt = null;
				if(null!=conn)
					conn.close();conn = null;
				if(null!=rs)
					rs.close();rs=null;
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(SIChannelDataLoadDAO.class.getName(), "getScheduleList()", re);
			}
		}
		return scheduleList;
				
	}
}
