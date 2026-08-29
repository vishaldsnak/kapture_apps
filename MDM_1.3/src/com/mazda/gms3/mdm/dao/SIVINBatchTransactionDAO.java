package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class SIVINBatchTransactionDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SIVINBatchTransactionDAO.class);
	
	public static int getPendingAndProcessingJobCount(String wslId, String market)
	{
		int jobCount=0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet  rs = null;
		try
		{
			if(null!=wslId && !"".equals(wslId))
			{
				conn = getConnection();
				String sql="SELECT COUNT(mdm_schedule_id) AS COUNT FROM gms3_mdm_sivin_sch WHERE"
						+ " ( mdm_job_status='"+ScheduleConstants.STATUS_PENDING+"' OR  mdm_job_status='"+ScheduleConstants.STATUS_PROCESSING+"' ) "
						+ " AND mdm_user_id=? AND mdm_schedule_name LIKE '%_"+market.trim().toUpperCase()+"_%'";
				pstmt = conn.prepareStatement(sql);
				// here in table wslId will always be saved in lower case, so make key lower case
				pstmt.setString(1, wslId.trim().toLowerCase());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					jobCount = rs.getInt("COUNT");
				}
				sql = null;
			}
			else
			{
				logger.info("getPendingAndProcessingJobCount :: WSL id as parameter is null. Return 0.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "getPendingAndProcessingJobCount()", e);
		}
		finally
		{
			wslId =null;
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
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "getPendingAndProcessingJobCount()", e);
			}
		}
		return jobCount;
	}

	public static long createSchedule(SIVINScheduleDetails schDetails)
	{
		Connection conn = null;
		PreparedStatement pstmt =null;
		Statement stmt = null;
		ResultSet rs = null;
		long scheduleId=0;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			
			String sql = "INSERT INTO gms3_mdm_sivin_sch (mdm_user_id) VALUES ('"+schDetails.getWslId().trim().toLowerCase()+"')";
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
				sql = "UPDATE gms3_mdm_sivin_sch SET mdm_schedule_name = ?,mdm_job_status = ?,mdm_total_count = ?,"
						+ "mdm_sch_thread_id = ?,mdm_schedule_tmstp = ?, mdm_document_id = ?,mdm_locale = ? WHERE mdm_schedule_id=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, schDetails.getScheduleName()+String.valueOf(scheduleId));
				pstmt.setString(2, schDetails.getJobStatus());
				pstmt.setInt(3, schDetails.getTotalCount());
				pstmt.setString(4, schDetails.getScheduleName()+String.valueOf(scheduleId));
				pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
				pstmt.setString(6, schDetails.getDocumentId());
				pstmt.setString(7, schDetails.getLocale());
				pstmt.setLong(8, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt =null;
				sql = null;
				
				// insert schedule Items
				if(null!=schDetails.getCategoryList() && schDetails.getCategoryList().size()>0)
				{
					IMCategoryDetails details = new IMCategoryDetails();
					SIVinDetails items = new SIVinDetails();
					for(int a = 0;a<schDetails.getCategoryList().size();a++)
					{
						details = (IMCategoryDetails)schDetails.getCategoryList().get(a);
						items = new SIVinDetails();
						if(null!=details.getItemDetails())
						{
							items = details.getItemDetails();
						}
						
						sql  = "INSERT INTO gms3_mdm_sivin_sch_items (mdm_schedule_id,mdm_carline_name, mdm_carline_code, mdm_wmi_code, "
								+ "mdm_vds_code, mdm_vis_start_range,mdm_vis_end_range,mdm_cat_name,mdm_cat_ref_key,"
								+ "mdm_parent_ref_key,mdm_cat_level,mdm_cat_locale,mdm_year) "
								+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(sql);
						pstmt.setLong(1, scheduleId);
						if(null!=items.getModelRegionalName() && !"".equals(items.getModelRegionalName()))
						{
							pstmt.setString(2, items.getModelRegionalName());
						}
						else
						{
							pstmt.setString(2, items.getModel());
						}
						pstmt.setString(3, items.getCarlineCode());
						pstmt.setString(4, items.getWmiCode());
						pstmt.setString(5, items.getVdsCode());
						pstmt.setString(6, items.getVinStartRange());
						pstmt.setString(7, items.getVinEndRange());
						pstmt.setString(8, details.getCategoryName());
						pstmt.setString(9, details.getCategoryRefKey());
						pstmt.setString(10, details.getParentRefKey());
						pstmt.setString(11, details.getLevel());
						pstmt.setString(12, details.getLocale());
						pstmt.setString(13, items.getYear());
						pstmt.executeUpdate();
						pstmt.close();pstmt =null;
						sql = null;
						items = null;
						details = null;
					}
					sql = null;
					items = null;
					details = null;
					//commit transaction
					conn.commit();
				}
				else
				{
					scheduleId = 0;
					// roll back - no items found
					conn.rollback();
				}
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "createScheduleId()", e);
			try
			{
				conn.rollback();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "createScheduleId()", eq);
			}
			scheduleId = 0;
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
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "createScheduleId()", eq);
			}
		}
		return scheduleId;
	}

	public static ArrayList<SIVINScheduleDetails> getHistoryTransactionList(String wslId, String market)
	{
		ArrayList<SIVINScheduleDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=wslId && !"".equals(wslId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_mdm_sivin_sch WHERE mdm_user_id = ? AND mdm_schedule_name LIKE '%_"+market.trim().toUpperCase()+"_%' ORDER BY mdm_schedule_id DESC";
				pstmt = conn.prepareStatement(sql);
				// here in table wslId will always be saved in lower case, so make key lower case
				pstmt.setString(1, wslId.trim().toLowerCase());
				rs = pstmt.executeQuery();
				SIVINScheduleDetails schDetails = null;
				while(rs.next())
				{
					schDetails = new SIVINScheduleDetails();
					schDetails.setScheduleId(rs.getLong("mdm_schedule_id"));
					schDetails.setScheduleName(rs.getString("mdm_schedule_name"));
					schDetails.setWslId(rs.getString("mdm_user_id"));
					schDetails.setJobStatus(rs.getString("mdm_job_status"));
					schDetails.setScheduleStatus(rs.getString("mdm_sch_status"));
					schDetails.setScheduleTime(rs.getTimestamp("mdm_schedule_tmstp"));
					schDetails.setFinishTime(rs.getTimestamp("mdm_finish_tmstp"));
					schDetails.setTotalCount(rs.getInt("mdm_total_count"));
					schDetails.setProcessedCount(rs.getInt("mdm_processed_count"));
					schDetails.setFailureCount(rs.getInt("mdm_failed_count"));
					schDetails.setThreadId(rs.getString("mdm_sch_thread_id"));
					schDetails.setDocumentId(rs.getString("mdm_document_id"));
					schDetails.setLocale(rs.getString("mdm_locale"));
					schDetails.setReportsPath(rs.getString("mdm_reports_path"));
					if(null==list || list.size()<=0)
					{
						list = new ArrayList<SIVINScheduleDetails>();
					}
					schDetails.setSrNo(list.size()+1);
					list.add(schDetails);
					schDetails  =null;
				}
				sql  =null;
				schDetails= null;
			}
			else
			{
				logger.info("getHistoryTransactionList :: WSL id as Parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "getHistoryTransactionList()", e);
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
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "getHistoryTransactionList()", eq);
			}
		}
		return list;
				
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
				String sql="UPDATE gms3_mdm_sivin_sch SET mdm_job_status='"+ScheduleConstants.STATUS_ABORTED+"', "
						+ " mdm_sch_status='"+ScheduleConstants.STATUS_FAILURE+"',mdm_finish_tmstp = ? "
						+ " WHERE mdm_schedule_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
				// DO NOTHING WITH ITEMS STATUS
				
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "updateAbortStatus()", e);
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
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "updateAbortStatus()", re);
			}
			scheduleId = null;
		}
	}

	public static Map<String, String> checkDocumentInUse(String documentId, String locale)
	{
		Map<String, String> userMap = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
			{
				String userId="";
				conn = getConnection();
				String sql="SELECT mdm_user_id FROM gms3_mdm_sivin_sch WHERE "
						+ " (mdm_job_status='"+ScheduleConstants.STATUS_PENDING+"' OR mdm_job_status='"+ScheduleConstants.STATUS_PROCESSING+"') "
						+ " AND mdm_document_id='"+documentId+"' AND mdm_locale='"+locale+"'";
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					userId=  rs.getString("mdm_user_id");
				}
				pstmt.close();pstmt=null;
				rs.close();rs=null;
				sql = null;
				
				if(null!=userId && !"".equals(userId))
				{
					logger.info("checkDocumentInUse :: {"+documentId+"} of {"+locale+"} is in Use by :: >"+ userId);
					userMap = new HashMap<String, String>();
					userMap.put("USER_ID", userId);
					
					/*
					 * WHO HOLDS THE DOCUMENT - k_user, NOT OK_IM.USERINFORMATION, and on its own
					 * CMS connection since kapture_cms_db need not share a server with the MDM
					 * schema conn points at. A user with no Kapture row leaves the name blank and
					 * the caller falls back to the id, exactly as a missing row did before.
					 */
					Connection cmsConn = null;
					try
					{
						cmsConn = DBConnectionHelper.getCMSConnection();
						if(null!=cmsConn)
						{
							sql = "SELECT firstname, lastname FROM kapture_cms_db.k_user"
									+ " WHERE userid = ? AND "
									+ Utilities.activeUserStatusPredicate("user_status");
							pstmt= cmsConn.prepareStatement(sql);
							pstmt.setString(1, userId.trim().toLowerCase());
							rs=pstmt.executeQuery();
							if(rs.next())
							{
								if(null!=rs.getString("firstname"))
								{
									userMap.put("FIRST_NAME", rs.getString("firstname"));
								}
								if(null!=rs.getString("lastname"))
								{
									userMap.put("LAST_NAME", rs.getString("lastname"));
								}
							}
							pstmt.close();pstmt=null;
							rs.close();rs=null;
						}
					}
					finally
					{
						if(null!=cmsConn)
						{
							cmsConn.close();
						}
					}
					sql=null;
				}
				userId = null;
			}
			else
			{
				logger.info("checkDocumentInUse :: Document id / Locale as Parameters are null.");
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "checkDocumentInUse()", e);
			userMap = null;
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
				Utilities.printStackTraceToLogs(SIVINBatchTransactionDAO.class.getName(), "checkDocumentInUse()", e);
			}
			documentId = null;
			locale = null;
		}
		return userMap;
	}
}
