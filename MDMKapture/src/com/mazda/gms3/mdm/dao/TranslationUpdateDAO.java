package com.mazda.gms3.mdm.dao;

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
import java.util.Map;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.SITranslationScheduleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class TranslationUpdateDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(TranslationUpdateDAO.class);

	public static int getPendingAndProcessingJobCount()
	{
		int jobCount=0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet  rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT COUNT(mdm_schedule_id) AS job_count FROM gms3_mdm_trans_update_sch WHERE"
					+ " ( mdm_processing_status='"+ScheduleConstants.STATUS_PENDING+"' OR  mdm_processing_status='"+ScheduleConstants.STATUS_PROCESSING+"' ) ";
			pstmt = conn.prepareStatement(sql);
			// here in table wslId will always be saved in lower case, so make key lower case
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				jobCount = rs.getInt("job_count");
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "getPendingAndProcessingJobCount()", e);
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "getPendingAndProcessingJobCount()", e);
			}
		}
		return jobCount;
	}

	public static long createSchedule(SITranslationScheduleDetails schDetails)
	{
		long scheduleId=0;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			String sql = "INSERT INTO gms3_mdm_trans_update_sch (mdm_user_id,mdm_rec_status) VALUES ('"+schDetails.getUserId().trim().toLowerCase()+"','A')";
			stmt= conn.createStatement();
			String generatedKeys[] = { "mdm_schedule_id"};
			stmt.executeUpdate(sql, generatedKeys);
			rs = stmt.getGeneratedKeys();
			if(rs.next())
			{
				scheduleId = rs.getLong(1);
			}
			sql= null;
			stmt.close();stmt = null;
			rs.close();rs =null;
			generatedKeys = null;

			if(scheduleId > 0)
			{
				// UPDATE REST OF DETAILS
				sql = "UPDATE gms3_mdm_trans_update_sch SET mdm_schedule_name = ?,mdm_processing_status = ?,mdm_total_count = ?,"
						+ "mdm_sch_thread_id = ?,mdm_schedule_tmstp = ?, mdm_operation_type=?,mdm_locale = ? WHERE mdm_schedule_id=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, schDetails.getScheduleName()+String.valueOf(scheduleId));
				pstmt.setString(2, schDetails.getProcessingStatus());
				pstmt.setInt(3, schDetails.getTotalCount());
				pstmt.setString(4, schDetails.getThreadId()+String.valueOf(scheduleId));
				pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
				pstmt.setString(6, schDetails.getOperationType());
				pstmt.setString(7, schDetails.getLocaleCode());
				pstmt.setLong(8, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt =null;
				sql = null;
				// commit transaction
				conn.commit();
			}
		}
		catch(Exception e)
		{
			scheduleId = 0;
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "createSchedule()", e);
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "createSchedule()", eq);
			}
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "createSchedule()", e);
			}
			conn = null;
			rs=null;
			stmt=null;
			pstmt=null;
			schDetails = null;
		}
		return scheduleId;
	}

	public static ArrayList<SITranslationScheduleDetails> getScheduleList()
	{
		ArrayList<SITranslationScheduleDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			// previous 30 days records only
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
			String sql="SELECT * FROM gms3_mdm_trans_update_sch WHERE mdm_rec_status='A' AND mdm_schedule_tmstp BETWEEN ? AND ? "
					+ " ORDER BY mdm_schedule_id DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setTimestamp(1, beforeTime);
			pstmt.setTimestamp(2, currentTime);
			rs = pstmt.executeQuery();
			SITranslationScheduleDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new SITranslationScheduleDetails();
				schDetails.setScheduleId(rs.getLong("mdm_schedule_id"));
				schDetails.setScheduleName(rs.getString("mdm_schedule_name"));
				schDetails.setUserId(rs.getString("mdm_user_id"));
				schDetails.setJobStatus(rs.getString("mdm_job_status"));
				schDetails.setProcessingStatus(rs.getString("mdm_processing_status"));
				schDetails.setScheduleTime(rs.getTimestamp("mdm_schedule_tmstp"));
				schDetails.setFinishTime(rs.getTimestamp("mdm_finish_tmstp"));
				schDetails.setTotalCount(rs.getInt("mdm_total_count"));
				schDetails.setSuccessCount(rs.getInt("mdm_processed_count"));
				schDetails.setFailureCount(rs.getInt("mdm_failed_count"));
				schDetails.setThreadId(rs.getString("mdm_sch_thread_id"));
				schDetails.setLocaleCode(rs.getString("mdm_locale"));
				schDetails.setOperationType(rs.getString("mdm_operation_type"));
				schDetails.setRemarks(rs.getString("mdm_remarks"));
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<SITranslationScheduleDetails>();
				}
				schDetails.setSrNo(list.size()+1);
				list.add(schDetails);
				schDetails  =null;
			}
			sql  =null;
			schDetails= null;
			currentTime = null;
			beforeTime = null;
			sdf = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "getScheduleList()", e);
		}
		finally 
		{
			try
			{
				if (null != pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "getScheduleList()", eq);
			}
		}
		return list;

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
						sql="UPDATE gms3_mdm_trans_update_sch SET mdm_rec_status='I' "
								+ "   WHERE mdm_schedule_id = "+ tokens[i].toString();
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "deleteScheduleDetails()", e1);
			}
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "deleteScheduleDetails()", e);
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "deleteScheduleDetails()", e);
			}
			// set passed parameter to null
			scheduleIds=null;
		}
		return true;
	}
	
	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button writes mdm_processing_status = aborted through updateAbortStatus() below, and does
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
					String sql="SELECT mdm_processing_status FROM gms3_mdm_trans_update_sch WHERE mdm_schedule_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, scheduleId.trim());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						String status = rs.getString("mdm_processing_status");
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
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "isAborted()", e);
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "isAborted()", re);
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
				String sql="UPDATE gms3_mdm_trans_update_sch SET mdm_job_status='"+ScheduleConstants.STATUS_FAILURE+"', "
						+ " mdm_processing_status='"+ScheduleConstants.STATUS_ABORTED+"',mdm_finish_tmstp = ? "
						+ " WHERE mdm_schedule_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "updateAbortStatus()", e);
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
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "updateAbortStatus()", re);
			}
			scheduleId = null;
		}
	}
	
	public static List<Map<String, String>> verifyDocuments(List<Map<String, String>> documentsList)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=documentsList && documentsList.size()>0)
			{
				/*
				 * DOES THIS DOCUMENT EXIST FOR THIS LOCALE?
				 *
				 * WAS: SELECT CONTENTID FROM the InfoManager CONTENTTEXT table, LATESTVERSION='Y', on
				 * the MDM connection. NOW: kapture_cms_db.k_article, latest_version='Y', on the CMS
				 * connection - the two schemas are not on the same server, so the old cross-schema
				 * reference only ever worked because Oracle had both in one instance.
				 *
				 * THE LOCALE IS MATCHED ON THE EFFECTIVE LOCALE, not article_primary_locale. InfoManager
				 * had ONE locale column; Kapture splits it, and a TRANSLATION KEEPS THE MASTER'S primary
				 * locale - so matching primary alone would pass a document as present in a locale it has
				 * no version for, and the schedule would fail on it later with a worse message. Bind the
				 * locale TWICE.
				 */
				conn = getCMSConnection();
				if(null==conn)
				{
					logger.info("verifyDocuments :: no Kapture CMS connection - documents cannot be verified.");
					return documentsList;
				}
				String sql = "SELECT article_id FROM kapture_cms_db.k_article"
						+ " WHERE article_id = ? AND latest_version = 'Y'"
						+ " AND (article_translated_locale = ?"
						+ " OR (article_translated_locale IS NULL AND article_primary_locale = ?))"
						+ " LIMIT 1";
				Map<String, String> docMap = null;
				for(int a=0;a<documentsList.size();a++)
				{
					docMap=  (Map<String, String>)documentsList.get(a);
					if(null!=docMap && null!=docMap.get("DOC_ID") && !"".equals(docMap.get("DOC_ID")) &&
							null!=docMap.get("LOCALE") && !"".equals(docMap.get("LOCALE")))
					{
						String locale = docMap.get("LOCALE").toString().replace("-", "_").trim();
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, docMap.get("DOC_ID").toString().trim());
						pstmt.setString(2, locale);
						pstmt.setString(3, locale);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							docMap.put("STATUS", "Y");
						}
						rs.close();rs=null;
						pstmt.close();pstmt=null;
						locale = null;
					}
				}
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "verifyDocuments()", e);
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(TranslationUpdateDAO.class.getName(), "verifyDocuments()", re);
			}
		}
		return documentsList;
	}

}
