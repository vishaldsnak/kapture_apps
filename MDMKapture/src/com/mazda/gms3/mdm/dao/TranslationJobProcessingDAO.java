package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ContentDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class TranslationJobProcessingDAO extends DBConnectionHelper{

	/*
	 * This class had no logger of its own and was borrowing DBConnectionHelper's, which is not
	 * visible outside that package.
	 */
	private static Logger logger = LogManager.getLogger(TranslationJobProcessingDAO.class);
	
	public Connection conn = null;
	
	public int connectionCount=0;
	
	public void updateScheduleCompletion(String scheduleId, String scheduleStatus, String remarks)
	{
		/*
		 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
		 *
		 * The worker stops cooperatively (Thread.stop() is removed in Java 20), so it can still
		 * reach this on its way out and would flip 'Aborted' to Completed or Failure, losing the
		 * abort the user just performed. Refusing the write here covers every branch that calls
		 * this, in the one place they all funnel through.
		 */
		if(TranslationUpdateDAO.isAborted(scheduleId))
		{
			logger.info("updateScheduleCompletion :: SCHEDULE {"+scheduleId+"} WAS ABORTED - leaving its status as Aborted"
					+" instead of writing {"+scheduleStatus+"}.");
			return;
		}
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
				String sql="UPDATE gms3_mdm_trans_update_sch SET mdm_processing_status='"+ScheduleConstants.STATUS_COMPLETED+"', "
						+ " mdm_job_status='"+scheduleStatus+"', mdm_finish_tmstp = ? , mdm_remarks = ? "
						+ " WHERE mdm_schedule_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				pstmt.setString(2, remarks);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateScheduleCompletion()", e);
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
				Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateScheduleCompletion()", re);
			}
			scheduleId = null;
			scheduleStatus = null;
		}
	}

	public void updateProcessingStatus(String scheduleId)
	{
		Statement stmt = null;
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
			sql="UPDATE gms3_mdm_trans_update_sch SET mdm_processing_status = '"+ScheduleConstants.STATUS_PROCESSING+"' WHERE mdm_schedule_id="+scheduleId;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateProcessingStatus()", re);
			}
			scheduleId = null;
		}
	}

	public void updateProcessingCount(String scheduleId, String status)
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
			// UPDATE SUCCESS / FAILURE COUNT FOR SCHEDULE
			int count=0;
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "SELECT mdm_processed_count AS job_count FROM gms3_mdm_trans_update_sch WHERE mdm_schedule_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mdm_failed_count AS job_count FROM gms3_mdm_trans_update_sch WHERE mdm_schedule_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				count = rs.getInt("job_count");
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			sql = null;
			
			// add 1 to count
			count = count+1;
			// update count back in DATABASE TABLE
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_trans_update_sch SET mdm_processed_count="+count+" WHERE mdm_schedule_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_trans_update_sch SET mdm_failed_count="+count+" WHERE mdm_schedule_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateProcessingCount()", e);
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
				Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateProcessingCount()", re);
			}
			scheduleId = null;
		}
	}

	public ContentDetails updateTitleInViewContent(ContentDetails details, String title)
	{
		PreparedStatement pstmt = null;
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
			
			/*
			 * LOWERCASE, because MySQL on Linux is case sensitive about table names and the
			 * migrated schema is lowercase. The uppercase form resolved on a Windows workstation
			 * and would have failed on VDI - working everywhere it is developed and nowhere it is
			 * deployed.
			 *
			 * ONLY MME LOCALES REACH HERE. The caller gates on the document being SM and the
			 * locale being in neither mnao.countries.locales.codes nor mc.countries.locales.codes,
			 * so the per-locale MME table is the right target.
			 */
			String tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(details.getLocale());
			String sql = "UPDATE "+ tableName+" SET vc_vin_document_title=? WHERE vc_vin_document_id=? ";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, title);
			pstmt.setString(2, details.getDocumentId());
			pstmt.executeUpdate();
			tableName=  null;
			sql  =null;
			// set flag to true
			details.setTitleUpdateStatus(true);
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateTitleInViewContent()", e);
			details.setTitleUpdateStatus(false);
			if(null!=details.getErrorMessage() && !"".equals(details.getErrorMessage()))
			{
				details.setErrorMessage(details.getErrorMessage()+" ."+ e.getMessage());
			}
			else
			{
				details.setErrorMessage(e.getMessage());
			}
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
				Utilities.printStackTraceToLogs(TranslationJobProcessingDAO.class.getName(), "updateTitleInViewContent()", re);
			}
		}
		
		return details;
	}
	
	
}
