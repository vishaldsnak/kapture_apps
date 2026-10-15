package com.mazda.gms3.mdm.dataexttool.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.PreparedStatement;

import com.mazda.gms3.mdm.dao.CategoryProcessingDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportToolDAO;

public class DataExportTransactionDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(DataExportTransactionDAO.class);

	public Connection conn = null;

	public int connectionCount=0;

	public void updateHardJobFailure(long scheduleId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(scheduleId>0)
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

				conn.setAutoCommit(false);
				String sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_processing_status=?, mde_sch_completion_status=?,mde_sch_finish_time =? WHERE mde_sch_id = ?";

				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, ScheduleConstants.STATUS_COMPLETED);
				pstmt.setString(2, ScheduleConstants.STATUS_FAILURE);
				pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
				pstmt.setLong(4, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt = null;
				sql = null;


				/*
				 * UPDATE ALL ITEMS MDE_SCH_PROCESSING_STATUS TO COMPLETED.
				 * AND COMPLETION STATUS AS FAILURE
				 */
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_processing_status = ?, mde_sch_completion_status = ? WHERE mde_sch_id = ?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, ScheduleConstants.STATUS_COMPLETED);
				pstmt.setString(2, ScheduleConstants.STATUS_FAILURE);
				pstmt.setLong(3, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt = null;
				sql = null;

				// commit transaction
				conn.commit();
				// set autoCommit to true
				conn.setAutoCommit(true);
			}
			else
			{
				logger.info("updateHardJobFailure :: Schedule id as parameter is null.");
			}
		}
		catch(Exception e)
		{
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateHardJobFailure()", e1);
			}
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateHardJobFailure()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateHardJobFailure()", e);
			}
		}
	}

	public void updateXMLZIPAndReportsZipSize(String reportZipFileSize, String xmlZipFileSize,String scheduleId)
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

			String sql="";
			sql="UPDATE gms3_mdm_mde_tool_sch SET mdm_sch_reports_size = ?,mde_sch_content_zip_size = ? ";
			sql = sql+" WHERE mde_sch_id="+scheduleId;
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, reportZipFileSize);
			pstmt.setString(2, xmlZipFileSize);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateXMLZIPAndReportsZipSize()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateXMLZIPAndReportsZipSize()", re);
			}
			scheduleId = null;
			reportZipFileSize = null;
			xmlZipFileSize = null;
		}
	}

	
	public void updateScheduleProcessingStatus(String processingStatus, String completionStatus,String scheduleId, boolean updateFinishTime)
	{
		/*
		 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
		 *
		 * The worker stops cooperatively (Thread.stop() is removed in Java 20), so it can still
		 * reach this on its way out and would flip 'Aborted' to Completed or Failure, losing the
		 * abort the user just performed. Refusing the write here covers every branch that calls
		 * this, in the one place they all funnel through.
		 */
		if(DataExportToolDAO.isAborted(scheduleId))
		{
			logger.info("updateScheduleProcessingStatus :: SCHEDULE {"+scheduleId+"} WAS ABORTED - leaving its status as Aborted"
					+" instead of writing {"+processingStatus+"}.");
			return;
		}
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

			String sql="";
			sql="UPDATE gms3_mdm_mde_tool_sch SET mde_sch_processing_status = ?,mde_sch_completion_status = ? ";
			if(updateFinishTime==true)
			{
				sql = sql+" , mde_sch_finish_time = ? ";
			}
			sql = sql+" WHERE mde_sch_id="+scheduleId;
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, processingStatus);
			pstmt.setString(2, completionStatus);
			if(updateFinishTime==true)
			{
				pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
			}
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateScheduleProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateScheduleProcessingStatus()", re);
			}
			scheduleId = null;
		}
	}

	public void updateScheduleCustomError(String customError,String scheduleId)
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

			String sql="";
			sql="UPDATE gms3_mdm_mde_tool_sch SET mde_sch_reports_path = ? WHERE mde_sch_id="+scheduleId;
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, customError);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateScheduleCustomError()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateScheduleCustomError()", re);
			}
			scheduleId = null;
		}
	}

	
	public DataExportToolItemDetails getItemId(DataExportToolItemDetails itemDetails)
	{
		PreparedStatement pstmt = null;
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
			sql="SELECT mde_sch_item_id FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id=? AND mde_sch_locale = ? "
					+ " AND mde_sch_model = ?  "
					+ " AND mde_sch_channel = ? ";
			if(null!=itemDetails.getDocumentType() && !"".equals(itemDetails.getDocumentType()))
			{
				sql  = sql+ " AND mde_sch_doc_type = ? ";
			}
			else
			{
				sql = sql + " AND mde_sch_doc_type IS NULL ";
			}
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, itemDetails.getScheduleId());
			pstmt.setString(2, itemDetails.getLocale());
			pstmt.setString(3, itemDetails.getModel());
//			pstmt.setString(4, itemDetails.getCarlineCode());
			pstmt.setString(4, itemDetails.getChannelName());
			if(null!=itemDetails.getDocumentType() && !"".equals(itemDetails.getDocumentType()))
			{
				pstmt.setString(5, itemDetails.getDocumentType());
			}
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				itemDetails.setItemId(rs.getLong("mde_sch_item_id"));
			}	
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getItemId()", e);
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getItemId()", re);
			}
		}
		return itemDetails;
	}

	public void updateItemProcessingStatus(String processingStatus, String completionStatus, long itemId)
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

			String sql="";
			sql="UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_processing_status = ? , mde_sch_completion_status = ? WHERE mde_sch_item_id=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, processingStatus);
			pstmt.setString(2, completionStatus);
			pstmt.setLong(3, itemId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateItemProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateItemProcessingStatus()", re);
			}
			itemId = 0;
			processingStatus = null;
			completionStatus =null;
		}
	}

	public void updateTotalInnerLinksCounts(long itemId,long scheduleId, long count)
	{
		PreparedStatement pstmt = null;
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
			long existingCount =0;
			sql = "SELECT mde_sch_inlk_total FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id ="+ scheduleId;
			pstmt= conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				existingCount = rs.getLong("mde_sch_inlk_total");
			}
			pstmt.close();pstmt=null;
			rs.close();rs=null;
			sql = null;
			
			// add count to existingCount
			existingCount = existingCount+count;
			// update in SCH TABLE
			sql="UPDATE gms3_mdm_mde_tool_sch SET mde_sch_inlk_total = ? WHERE mde_sch_id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, existingCount);
			pstmt.setLong(2, scheduleId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
			
			// update Count in ITEM TABLE
			sql="UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_inlk_total = ?  WHERE mde_sch_item_id=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, count);
			pstmt.setLong(2, itemId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateTotalInnerLinksCounts()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateTotalInnerLinksCounts()", re);
			}
			itemId = 0;
			scheduleId = 0;
			count =0;
		}
	}


	public void updateProcessingCount(String scheduleId, String itemId, String status)
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
				sql = "SELECT mde_sch_success_docs AS COUNT FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_failure_docs AS COUNT FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
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
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_success_docs="+count+" WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_failure_docs="+count+" WHERE mde_sch_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

			// UPDATE SUCCESS / FAILURE COUNT FOR ITEM
			count=0;
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "SELECT mde_sch_success_docs AS COUNT FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_failure_docs AS COUNT FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
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
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_success_docs="+count+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_failure_docs="+count+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateProcessingCount()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateProcessingCount()", re);
			}
			scheduleId = null;
		}
	}

	public void updateItemInnerLinkProcessingStatus(String processingStatus, String completionStatus, long itemId)
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

			String sql="";
			sql="UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_inlk_processing_status = ? , mde_sch_inlk_completion_status = ? WHERE mde_sch_item_id=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, processingStatus);
			pstmt.setString(2, completionStatus);
			pstmt.setLong(3, itemId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateItemInnerLinkProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateItemInnerLinkProcessingStatus()", re);
			}
			itemId = 0;
			processingStatus = null;
			completionStatus =null;
		}
	}

	public void updateInnerLinkProcessingCount(String scheduleId, String itemId, String status)
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
				sql = "SELECT mde_sch_inlk_success AS COUNT FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_inlk_failure AS COUNT FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
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
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_inlk_success="+count+" WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_inlk_failure="+count+" WHERE mde_sch_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

			// UPDATE SUCCESS / FAILURE COUNT FOR ITEM
			count=0;
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "SELECT mde_sch_inlk_success AS COUNT FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_inlk_failure AS COUNT FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
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
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_inlk_success="+count+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_inlk_failure="+count+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateInnerLinkProcessingCount()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateInnerLinkProcessingCount()", re);
			}
			scheduleId = null;
		}
	}

	public String getModelFolderName(String locale,String documentId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String modelFolderName=null;
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

//			String tableNames=  "GMS3_DMT_"+locale.trim().toUpperCase()+"_NM_IMDOC,GMS3_DMT_"+locale.trim().toUpperCase()+"_IMDOC";
			String tableNames=  "gms3_dmt_"+Utilities.tableLocale(locale)+"_imdoc";
			String[] tok = tableNames.split(",");
			String sql="";
			for(int a=0;a<tok.length;a++)
			{
				sql="SELECT dc_model_folder_name FROM "+tok[a]+" WHERE dc_im_doc_id=? AND dc_im_doc_flag=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, documentId);
				pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					if(null!=rs.getString("dc_model_folder_name") && !"".equals(rs.getString("dc_model_folder_name")))
					{
						modelFolderName = rs.getString("dc_model_folder_name");
					}
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;
				if(null!=modelFolderName && !"".equals(modelFolderName))
				{
					// break the loop
					break;
				}
			}
			sql  =null;
			tableNames = null;
			tok  = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getModelFolderName()", e);
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getModelFolderName()", re);
			}
		}
		return modelFolderName;
	}
	
	public void updateOKAssetsCount(String scheduleId, String itemId, String status)
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
			int totalCount=0;
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "SELECT mde_sch_okassets_success AS COUNT,mde_sch_okassets_total AS TOTAL FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_okassets_failure AS COUNT,mde_sch_okassets_total AS TOTAL FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				count = rs.getInt("COUNT");
				totalCount= rs.getInt("TOTAL");
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			sql = null;

			// add 1 to count
			count = count+1;
			// add 1 to totalCount
			totalCount= totalCount+1;
			// update count back in DATABASE TABLE
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_okassets_success="+count+",mde_sch_okassets_total="+totalCount+" WHERE mde_sch_id= "+scheduleId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch SET mde_sch_okassets_failure="+count+",mde_sch_okassets_total="+totalCount+" WHERE mde_sch_id= "+scheduleId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

			// UPDATE SUCCESS / FAILURE COUNT FOR ITEM
			count=0;
			totalCount= 0;
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "SELECT mde_sch_okassets_success AS COUNT,mde_sch_okassets_total AS TOTAL FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "SELECT mde_sch_okassets_failure AS COUNT,mde_sch_okassets_total AS TOTAL FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				count = rs.getInt("COUNT");
				totalCount = rs.getInt("TOTAL");
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			sql = null;

			// add 1 to count
			count = count+1;
			// add 1 to totalCount
			totalCount= totalCount+1;
			// update count back in DATABASE TABLE
			if(status.equals(ScheduleConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_okassets_success="+count+",mde_sch_okassets_total="+totalCount+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			else if(status.equals(ScheduleConstants.STATUS_FAILURE))
			{
				sql = "UPDATE gms3_mdm_mde_tool_sch_it SET mde_sch_okassets_failure="+count+",mde_sch_okassets_total="+totalCount+" WHERE mde_sch_id= "+scheduleId+" AND mde_sch_item_id="+itemId;
			}
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();stmt=null;
			sql = null;

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateOKAssetsCount()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "updateOKAssetsCount()", re);
			}
			scheduleId = null;
		}
	}

	public void createReportSummary(DataExportToolReportSummaryDetails summaryDetails)
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

			String sql="INSERT INTO gms3_mdm_mde_sch_summary (mde_sch_id,mde_sch_report_name,mde_sch_report_status,mde_sch_total_count,"
					+ "mde_sch_success_count,mde_sch_failure_count) VALUES ("+summaryDetails.getScheduleId()+",?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, summaryDetails.getReportName());
			pstmt.setString(2, summaryDetails.getReportStatus());
			pstmt.setLong(3, summaryDetails.getTotalCount());
			pstmt.setLong(4, summaryDetails.getSuccessCount());
			pstmt.setLong(5, summaryDetails.getFailureCount());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "createReportSummary()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "createReportSummary()", re);
			}
			summaryDetails = null;
		}
	}

	/**
	 * THE ADDRESS TO NOTIFY WHEN A SCHEDULE FINISHES.
	 *
	 * WAS: SELECT EMAIL FROM OK_IM.USERINFORMATION WHERE ACTIVE='Y' AND LOGIN = ?
	 * That schema does not exist under MySQL, so the query threw, the catch swallowed it, and a
	 * null address is a legitimate "nobody to tell" - so the schedule reported Success and quietly
	 * notified no one. Nothing in the UI or the schedule row said an email had been skipped.
	 *
	 *   OK_IM.USERINFORMATION.LOGIN / EMAIL / ACTIVE='Y'
	 *     ->  kapture_cms_db.k_user.userid / email / user_status='Active'
	 *
	 * THE LOOKUP OPENS ITS OWN CMS CONNECTION, which is the point of delegating rather than
	 * rewriting the SQL here: kapture_cms_db and this DAO's connection are not the same server, so
	 * the old cross-schema reference only ever worked because Oracle had both in one instance.
	 *
	 * @param closeConnection retained for the call site; it referred to THIS DAO's connection,
	 *                        which this method no longer touches
	 */
	public String getEmailId(String wslId, String closeConnection)
	{
		String emailId = null;
		try
		{
			emailId = new CategoryProcessingDAO().getUserEmailDetails(wslId);
			if(null==emailId || "".equals(emailId.trim()))
			{
				logger.info("getEmailId :: no active Kapture user found for {"+wslId+"} - no notification will be sent to the requester.");
				emailId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getEmailId()", e);
		}
		finally
		{
			wslId =null;
		}
		return emailId;
	}

	public List<MNAOViewContentDetails> getViewContentDetails(String documentId, String locale,String modelName)
	{
		logger.info("getViewContentDetails :: > Start Fetching ViewContentDetails.");
		List<MNAOViewContentDetails> viewContentList = null;
		PreparedStatement pstmt = null;
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
			locale = locale.replace("-", "_");
			String tableName=null;
//			if(documentId.startsWith("SM") || documentId.startsWith("OSM") || documentId.startsWith("WD"))
//			{
//				tableName = "GMS3_VC_MME_VIN_DTL_"+locale.toUpperCase();
//			}
//			else
//			{
//				tableName = "gms3_vc_mme_vin_si_detail";
//			}
			
			tableName  ="gms3_vc_model_year_details";
			
			String sql="";
			sql="SELECT * FROM "+tableName+" WHERE vc_my_document_id=? AND vc_my_locale=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId);
			pstmt.setString(2, locale);
			rs = pstmt.executeQuery();
			MNAOViewContentDetails details = null;
			while(rs.next())
			{
				details = new MNAOViewContentDetails();
				// COMMON VARIABLES
				details.setDocumentId(rs.getString("vc_my_document_id"));
				details.setLocale(rs.getString("vc_my_locale"));
				details.setProcessingModelForMNAODataExport(modelName);
				details.setModel(rs.getString("vc_my_model"));
				details.setYear(rs.getString("vc_my_year"));
				details.setCatNameLevel1(rs.getString("vc_my_esi_cat_name_1"));
				details.setCatNameLevel2(rs.getString("vc_my_esi_cat_name_2"));
				details.setCatNameLevel3(rs.getString("vc_my_esi_cat_name_3"));
				details.setTitle(rs.getString("vc_my_document_title"));
				details.setImDocLastModifiedDate(rs.getTimestamp("vc_my_doc_last_modified_date"));
				details.setDocumentType(rs.getString("vc_my_document_type"));
				details.setDocumentTypeName(rs.getString("vc_my_document_type_name"));
				details.setDocumentSubType(rs.getString("vc_my_document_subtype"));
				details.setDocumentSubTypeName(rs.getString("vc_my_document_subtype_name"));
				// DOCUMENT SPECIFIC VARIABLES
				if(documentId.startsWith("SI"))
				{
					details.setSiNumber(rs.getString("vc_my_si_number"));
				}
				
				if(null==viewContentList || viewContentList.size()<=0)
				{
					viewContentList = new ArrayList<MNAOViewContentDetails>();
				}
				// add viewContentType as MODEL_YEAR
				details.setViewContentType("MODEL_YEAR");
				viewContentList.add(details);
				details = null;
			}	
			rs.close();rs=null;
			pstmt.close();pstmt=null;
			sql = null;
			details = null;
			tableName = null;
			
			/*
			 * NOW FETCH VIN DATA FOR DOCUMENT ID FROM OK_DC.GMS3_VC_VIN_DETAILS
			 */
			tableName = "gms3_vc_vin_details";
			details = null;
			sql="SELECT * FROM "+tableName+" WHERE vc_vin_document_id=? AND vc_vin_locale=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId);
			pstmt.setString(2, locale);
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				details = new MNAOViewContentDetails();
				// COMMON VARIABLES
				details.setDocumentId(rs.getString("vc_vin_document_id"));
				details.setProcessingModelForMNAODataExport(modelName);
				details.setLocale(rs.getString("vc_vin_locale"));
				details.setWmiCode(rs.getString("vc_vin_wmi_code"));
				details.setVdsCode(rs.getString("vc_vin_vds_code"));
				details.setVisStartRange(rs.getString("vc_vin_vis_start_range"));
				details.setVisEndRange(rs.getString("vc_vin_vis_end_range"));
				details.setCatNameLevel1(rs.getString("vc_vin_esi_cat_name_1"));
				details.setCatNameLevel2(rs.getString("vc_vin_esi_cat_name_2"));
				details.setCatNameLevel3(rs.getString("vc_vin_esi_cat_name_3"));
				details.setTitle(rs.getString("vc_vin_document_title"));
				details.setImDocLastModifiedDate(rs.getTimestamp("vc_vin_doc_last_modified_date"));
				details.setDocumentType(rs.getString("vc_vin_document_type"));
				details.setDocumentTypeName(rs.getString("vc_vin_document_type_name"));
				details.setDocumentSubType(rs.getString("vc_vin_document_subtype"));
				details.setDocumentSubTypeName(rs.getString("vc_vin_document_subtype_name"));
				// DOCUMENT SPECIFIC VARIABLES
				if(documentId.startsWith("SI"))
				{
					details.setSiNumber(rs.getString("vc_vin_si_number"));
				}
				
				if(null==viewContentList || viewContentList.size()<=0)
				{
					viewContentList = new ArrayList<MNAOViewContentDetails>();
				}
				// add viewContentType as VIN
				details.setViewContentType("VIN");
				viewContentList.add(details);
				details = null;
			}	
			rs.close();rs=null;
			pstmt.close();pstmt=null;
			sql = null;
			details = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getViewContentDetails()", e);
			viewContentList = null;
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "getViewContentDetails()", re);
			}
		}
		if(null!=viewContentList)
		{
			logger.info("getViewContentDetails :: > End Fetching ViewContentDetails.Total Rows Fetched are :: >"+ viewContentList.size());
		}
		else
		{
			logger.info("getViewContentDetails :: > End Fetching ViewContentDetails.No Rows Fetched. ");
		}
		return viewContentList;
	}

	
}