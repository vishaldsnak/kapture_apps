package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class CategoryProcessingDAO extends DBConnectionHelper{
	
	private Logger logger = LogManager.getLogger(CategoryProcessingDAO.class);
	
	public Connection conn = null;
	
	public int connectionCount=0;
	
	/**
	 * THE OTHER LOCALES OF A DOCUMENT THAT THE MME BATCH SHOULD ALSO UPDATE.
	 *
	 * MIGRATED TO KAPTURE 2026-08-05. This held a copy of the InfoManager query
	 * "SELECT DISTINCT LOCALEID FROM OK_IM.CONTENTTEXT ...", a cross-schema Oracle read with no
	 * equivalent on this connection - so it threw, the catch swallowed it, and the method returned
	 * null on EVERY call. The MME batch's translation branch therefore never actually ran.
	 *
	 * IT NOW DELEGATES rather than holding a second copy of the query. MMESIVinDAO owns the one
	 * implementation, and the SCREEN already uses it - the two must not be able to disagree about
	 * which locales a document has. The exclusion list (the selected en_EU plus the MNAO and MC
	 * locales) and the effective-locale rule live there; see MMESIVinDAO.getDocumentTranslations.
	 */
	public List<String> getDocumentTranslations(String documentId)
	{
		return MMESIVinDAO.getDocumentTranslations(documentId);
	}

	public SIVINScheduleDetails getScheduleDetails(String scheduleId, String itemsRequired)
	{
		SIVINScheduleDetails schDetails =null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId))
			{
				String sql="SELECT * FROM gms3_mdm_sivin_sch WHERE mdm_schedule_id="+scheduleId;
				try
				{
					conn.isValid(0);
				}
				catch(Exception  e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
					// increment connectionCount by 1
					connectionCount++;
				}
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
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
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;
				
				// FETCH ITEM DETAILS
				if(null!=itemsRequired && itemsRequired.equals("Y") && null!=schDetails && schDetails.getScheduleId()>0)
				{
					sql = "SELECT * FROM gms3_mdm_sivin_sch_items WHERE mdm_schedule_id="+scheduleId;
					stmt = conn.createStatement();
					rs  =stmt.executeQuery(sql);
					IMCategoryDetails details = null;
					while(rs.next())
					{
						details = new IMCategoryDetails();
						details.setItemId(rs.getLong("mdm_item_id"));
						details.setScheduleId(new Long(scheduleId).longValue());
						
						details.setItemDetails(new SIVinDetails());
						details.getItemDetails().setModel(rs.getString("mdm_carline_name"));
						details.getItemDetails().setCarlineCode(rs.getString("mdm_carline_code"));
						details.getItemDetails().setYear(rs.getString("mdm_year"));
						details.getItemDetails().setWmiCode(rs.getString("mdm_wmi_code"));
						details.getItemDetails().setVdsCode(rs.getString("mdm_vds_code"));
						details.getItemDetails().setVinStartRange(rs.getString("mdm_vis_start_range"));
						details.getItemDetails().setVinEndRange(rs.getString("mdm_vis_end_range"));
						// set wslId as well
						details.getItemDetails().setWslId(schDetails.getWslId());
						
						details.setCategoryName(rs.getString("mdm_cat_name"));
						details.setCategoryRefKey(rs.getString("mdm_cat_ref_key"));
						details.setParentRefKey(rs.getString("mdm_parent_ref_key"));
						details.setLevel(rs.getString("mdm_cat_level"));
						details.setLocale(rs.getString("mdm_cat_locale"));
						details.setProcessingStatus(rs.getString("mdm_processing_status"));
						
						if(null==schDetails.getCategoryList() || schDetails.getCategoryList().size()<=0)
						{
							schDetails.setCategoryList(new ArrayList<IMCategoryDetails>());
						}
						schDetails.getCategoryList().add(details);
						details = null;
					}
					details = null;
					rs.close();rs=null;
					stmt.close();stmt = null;
					sql  =null;
				}
			}
			else
			{
				logger.info("getScheduleDetails :: Schedule id as parameter is null or 0. Return Null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getScheduleDetails()", e);
			schDetails = null;
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getScheduleDetails()", re);
			}
			scheduleId = null;
		}
		return schDetails;
	}

	public void updateJobStatus(String scheduleId, String status)
	{
		Statement stmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception  e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
					// increment connectionCount by 1
					connectionCount++;
				}
				String sql="UPDATE gms3_mdm_sivin_sch SET mdm_job_status='"+status+"' WHERE mdm_schedule_id="+scheduleId;
				stmt  =conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateJobStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateJobStatus()", re);
			}
			scheduleId = null;
			status = null;
		}
	}

	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button on the Batch Transactions screen writes
	 * <code>mdm_job_status = 'Aborted'</code> through
	 * <code>SIVINBatchTransactionDAO.updateAbortStatus()</code>, and does so whether or not it
	 * could find the running thread. So the database already carries the abort signal and this
	 * only has to read it back.
	 *
	 * WHY THE DATABASE AND NOT A Thread REFERENCE. The screen used to call
	 * <code>Thread.stop()</code> on the worker. That method was REMOVED in Java 20 and now throws
	 * <code>UnsupportedOperationException</code>, which the caller's <code>catch(Exception)</code>
	 * swallows - so on JDK 25 the job was marked Aborted while the worker ran happily to the end,
	 * still creating categories and still writing item status rows. Nothing in the JVM can kill
	 * another thread any more; the worker has to stop itself. Reading the flag it was already
	 * given costs one indexed single-row SELECT per item, against loops that already do an UPDATE
	 * and several Kapture API calls per item.
	 *
	 * Returns FALSE on any error. A failed status read must not abort a healthy run - the job
	 * carrying on is the same behaviour as before this check existed.
	 *
	 * @param scheduleId the schedule being processed
	 * @return true only when the row exists and its job status is exactly the aborted constant
	 */
	public boolean isAborted(String scheduleId)
	{
		boolean aborted = false;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception  e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
					// increment connectionCount by 1
					connectionCount++;
				}
				String sql="SELECT mdm_job_status FROM gms3_mdm_sivin_sch WHERE mdm_schedule_id="+scheduleId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					String status = rs.getString("mdm_job_status");
					if(null!=status && ScheduleConstants.STATUS_ABORTED.equals(status.trim()))
					{
						aborted = true;
					}
					status = null;
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "isAborted()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "isAborted()", re);
			}
			rs = null;
			stmt = null;
		}
		return aborted;
	}

	public void updateScheduleCompletion(String scheduleId, String scheduleStatus)
	{
		PreparedStatement pstmt = null;
		try
		{
			/*
			 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
			 *
			 * The workers call this from five places each: the success path and four failure
			 * branches. The abort check in startProcess() returns before the normal one, but a
			 * branch that has not been walked would still land here and flip 'Aborted' to
			 * Completed or Failure - which is exactly what was seen: the schedule was aborted,
			 * the work stopped, and the status then read Completed as though nothing had
			 * happened. Refusing the write here closes every one of those paths at once, in the
			 * one place they all funnel through.
			 */
			if(isAborted(scheduleId))
			{
				logger.info("updateScheduleCompletion :: SCHEDULE {"+scheduleId+"} WAS ABORTED -"
						+ " leaving its status as Aborted instead of writing {"+scheduleStatus+"}.");
				return;
			}
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception  e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
					// increment connectionCount by 1
					connectionCount++;
				}
				String sql="UPDATE gms3_mdm_sivin_sch SET mdm_job_status='"+ScheduleConstants.STATUS_COMPLETED+"', "
						+ " mdm_sch_status='"+scheduleStatus+"',mdm_finish_tmstp = ? "
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
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateScheduleCompletion()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateScheduleCompletion()", re);
			}
			scheduleId = null;
			scheduleStatus = null;
		}
	}

	public void updateProcessingStatus(String scheduleId, String processingStatus, String itemId, String level)
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			try
			{
				conn.isValid(0);
			}
			catch(Exception  e)
			{
				// connection is either null or not active - re-initialize connection object
				conn = getConnection();
				// increment connectionCount by 1
				connectionCount++;
			}
			
			String sql="";
			if(level.equals(ScheduleConstants.LEVEL_3) || level.equals(ScheduleConstants.LEVEL_4))
			{
				// UPDATE PROCESSING STATUS OF ITEM ONLY
				sql="UPDATE gms3_mdm_sivin_sch_items SET mdm_processing_status = '"+processingStatus+"' WHERE mdm_item_id="+itemId;
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql = null;
			}
			else if(level.equals(ScheduleConstants.LEVEL_5))
			{
				// UPDATE PROCESSING STATUS OF ITEM 
				sql="UPDATE gms3_mdm_sivin_sch_items SET mdm_processing_status = '"+processingStatus+"' WHERE mdm_item_id="+itemId;
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql = null;
				
				// UPDATE SUCCESS / FAILURE COUNT FOR SCHEDULE
				int count=0;
				if(processingStatus.equals(ScheduleConstants.STATUS_SUCCESS))
				{
					sql = "SELECT mdm_processed_count AS COUNT FROM gms3_mdm_sivin_sch WHERE mdm_schedule_id= "+scheduleId;
				}
				else if(processingStatus.equals(ScheduleConstants.STATUS_FAILURE))
				{
					sql = "SELECT mdm_failed_count AS COUNT FROM gms3_mdm_sivin_sch WHERE mdm_schedule_id= "+scheduleId;
				}
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					count = rs.getInt("COUNT");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;
				
				// add 1 to count
				count = count+1;
				// update count back in DATABASE TABLE
				if(processingStatus.equals(ScheduleConstants.STATUS_SUCCESS))
				{
					sql = "UPDATE gms3_mdm_sivin_sch SET mdm_processed_count="+count+" WHERE mdm_schedule_id= "+scheduleId;
				}
				else if(processingStatus.equals(ScheduleConstants.STATUS_FAILURE))
				{
					sql = "UPDATE gms3_mdm_sivin_sch SET mdm_failed_count="+count+" WHERE mdm_schedule_id= "+scheduleId;
				}
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();stmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateProcessingStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateProcessingStatus()", re);
			}
			scheduleId = null;
		}
	}
	
	public void updateMappingCountDetails(String scheduleId, String mappingCountInfo)
	{
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception  e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
					// increment connectionCount by 1
					connectionCount++;
				}
				String sql="UPDATE gms3_mdm_sivin_sch SET mdm_reports_path=?  "
						+ " WHERE mdm_schedule_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setString(1, mappingCountInfo);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateMappingCountDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "updateMappingCountDetails()", re);
			}
			scheduleId = null;
			mappingCountInfo = null;
		}
	}
	
	/**
	 * THE NOTIFICATION ADDRESS FOR A WSL USER - k_user, NOT OK_IM.USERINFORMATION.
	 *
	 * OK_IM.USERINFORMATION.LOGIN / EMAIL / ACTIVE='Y' map onto k_user.userid / email /
	 * user_status='Active'. The lookup runs on ITS OWN CMS CONNECTION because the two schemas
	 * are not on the same server - the old cross-schema reference off the MDM connection only
	 * worked because Oracle had both in one instance.
	 *
	 * THE WSL ID MUST EXIST IN KAPTURE for this to return anything. A user known only to MDM
	 * has no k_user row and simply gets no notification, which is how a missing address behaved
	 * before as well.
	 *
	 * user_status IS NOT WRITTEN CONSISTENTLY BY KAPTURE - the same environment holds "Active"
	 * for one user and "ACTIVE" for the next - so the comparison goes through the shared
	 * predicate rather than matching a literal. An exact match here silently sent no e-mail to
	 * perfectly active users, with nothing in the logs to say why.
	 */
	public String getUserEmailDetails(String userId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection cmsConn = null;
		String emailId=null;
		try
		{
			if(null!=userId && !"".equals(userId))
			{
				cmsConn = DBConnectionHelper.getCMSConnection();
				if(null==cmsConn)
				{
					logger.info("getUserEmailDetails :: no Kapture CMS connection - cannot resolve"
							+ " an address for {" + userId + "}.");
					return null;
				}
				String sql="SELECT email FROM kapture_cms_db.k_user WHERE userid = ?"
						+ " AND " + Utilities.activeUserStatusPredicate("user_status");
				pstmt = cmsConn.prepareStatement(sql);
				pstmt.setString(1, userId.trim());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					if(null!=rs.getString("email"))
					{
						emailId = rs.getString("email");
					}
				}
				else
				{
					logger.info("getUserEmailDetails :: {" + userId + "} is not an active Kapture"
							+ " user - no address to notify.");
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getUserEmailDetails()", e);
			emailId=  null;
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=cmsConn)
					cmsConn.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getUserEmailDetails()", re);
			}
			userId = null;
		}
		return emailId;
	}

	public List<String> getALLIMLocales(String localesToSkip)
	{
		List<String> localesList = null;
		Statement stmt = null;
		ResultSet rs = null;
		Connection cmsConn = null;
		try
		{
			localesList = new ArrayList<String>();
			/*
			 * OWN CMS CONNECTION - the locale master moved to kapture_cms_db, which is not
			 * necessarily on the same server as the MDM schema this DAO's conn points at.
			 */
			cmsConn = DBConnectionHelper.getCMSConnection();
			if(null==cmsConn)
			{
				logger.info("getALLIMLocales :: no Kapture CMS connection - returning an"
						+ " empty locale list.");
				return localesList;
			}
			
			String[] tok = null;
			String skipValues=null;
			if(null!=localesToSkip && !"".equals(localesToSkip))
			{
				tok = localesToSkip.split(",");
				if(null!=tok && tok.length>0)
				{
					skipValues="";
					for(int a=0;a<tok.length;a++)
					{
						skipValues+= "'"+tok[a]+"'";
						if(a!=tok.length-1)
						{
							skipValues+=",";
						}
					}
				}
				tok = null;
			}
			
			/*
			 * k_default_locales IS THE LOCALE MASTER and every row in it is live, so there
			 * is no equivalent of OK_IM.LOCALE.ACTIVE='Y' to carry over.
			 */
			String sql="SELECT locale FROM kapture_cms_db.k_default_locales";
			if(null!=skipValues && !"".equals(skipValues))
			{
				sql = sql+" WHERE locale NOT IN ("+skipValues+")";
			}
			logger.info("getALLIMLocales :: Sql :: sql :: > "+ sql);
			stmt = cmsConn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				if(null!=rs.getString("locale") && !"".equals(rs.getString("locale")))
				{
					localesList.add(rs.getString("locale"));
				}
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			sql = null;
			skipValues = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getALLIMLocales()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=cmsConn)
					cmsConn.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(CategoryProcessingDAO.class.getName(), "getALLIMLocales()", re);
			}
			localesToSkip = null;
		}
		return localesList;
	}
}
