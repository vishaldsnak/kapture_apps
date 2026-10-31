package com.mazda.gms3.mdm.rmitool.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;

import com.mazda.gms3.mdm.dao.CategoryProcessingDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolItemDetails;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.MMEViewContentDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.rmitool.dao.RMIExportToolDAO;

public class RMIExportTransactionDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(RMIExportTransactionDAO.class);

	public Connection conn = null;

	public int connectionCount=0;

	public void updateHardJobFailure(long scheduleId)
	{
		flushCounts(); // buffered running counts first - see addCount
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
				String sql = "UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_processing_status=?, rmi_sch_completion_status=?,rmi_sch_finish_time =? WHERE rmi_sch_id = ?";

				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, ScheduleConstants.STATUS_COMPLETED);
				pstmt.setString(2, ScheduleConstants.STATUS_FAILURE);
				pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
				pstmt.setLong(4, scheduleId);
				pstmt.executeUpdate();
				pstmt.close();pstmt = null;
				sql = null;


				/*
				 * UPDATE ALL ITEMS RMI_SCH_PROCESSING_STATUS TO COMPLETED.
				 * AND COMPLETION STATUS AS FAILURE
				 */
				sql = "UPDATE gms3_mdm_rmi_tool_sch_item SET rmi_sch_processing_status = ?, rmi_sch_completion_status = ? WHERE rmi_sch_id = ?";
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateHardJobFailure()", e1);
			}
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateHardJobFailure()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateHardJobFailure()", e);
			}
		}
	}

	public void updateScheduleProcessingStatus(String processingStatus, String completionStatus,String scheduleId, boolean updateFinishTime)
	{
		flushCounts(); // buffered running counts first - see addCount
		/*
		 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
		 *
		 * The worker stops cooperatively (Thread.stop() is removed in Java 20), so it can still
		 * reach this on its way out and would flip 'Aborted' to Completed or Failure, losing the
		 * abort the user just performed. Refusing the write here covers every branch that calls
		 * this, in the one place they all funnel through.
		 */
		if(RMIExportToolDAO.isAborted(scheduleId))
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
			sql="UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_processing_status = ?,rmi_sch_completion_status = ? ";
			if(updateFinishTime==true)
			{
				sql = sql+" , rmi_sch_finish_time = ? ";
			}
			sql = sql+" WHERE rmi_sch_id="+scheduleId;
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateScheduleProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateScheduleProcessingStatus()", re);
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
			sql="UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_reports_path = ? WHERE rmi_sch_id="+scheduleId;
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, customError);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateScheduleCustomError()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateScheduleCustomError()", re);
			}
			scheduleId = null;
		}
	}

	
	public RMIExportToolItemDetails getItemId(RMIExportToolItemDetails itemDetails)
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
			sql="SELECT rmi_sch_item_id FROM gms3_mdm_rmi_tool_sch_item WHERE rmi_sch_id=? AND rmi_sch_locale = ? "
					+ " AND rmi_sch_model = ? AND rmi_sch_carline_code = ? "
					+ " AND rmi_sch_channel = ? ";
			if(null!=itemDetails.getDocumentType() && !"".equals(itemDetails.getDocumentType()))
			{
				sql  = sql+ " AND rmi_sch_doc_type = ? ";
			}
			else
			{
				sql = sql + " AND rmi_sch_doc_type IS NULL ";
			}
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, itemDetails.getScheduleId());
			pstmt.setString(2, itemDetails.getLocale());
			pstmt.setString(3, itemDetails.getModel());
			pstmt.setString(4, itemDetails.getCarlineCode());
			pstmt.setString(5, itemDetails.getChannelName());
			if(null!=itemDetails.getDocumentType() && !"".equals(itemDetails.getDocumentType()))
			{
				pstmt.setString(6, itemDetails.getDocumentType());
			}
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				itemDetails.setItemId(rs.getLong("rmi_sch_item_id"));
			}	
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getItemId()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getItemId()", re);
			}
		}
		return itemDetails;
	}

	public void updateItemProcessingStatus(String processingStatus, String completionStatus, long itemId)
	{
		flushCounts(); // buffered running counts first - see addCount
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
			sql="UPDATE gms3_mdm_rmi_tool_sch_item SET rmi_sch_processing_status = ? , rmi_sch_completion_status = ? WHERE rmi_sch_item_id=?";
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateItemProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateItemProcessingStatus()", re);
			}
			itemId = 0;
			processingStatus = null;
			completionStatus =null;
		}
	}

	public void updateTotalInnerLinksCounts(long itemId,long scheduleId, long count)
	{
		flushCounts(); // buffered running counts first - see addCount
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
			sql = "SELECT rmi_sch_inlk_total FROM gms3_mdm_rmi_tool_sch WHERE rmi_sch_id ="+ scheduleId;
			pstmt= conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				existingCount = rs.getLong("rmi_sch_inlk_total");
			}
			pstmt.close();pstmt=null;
			rs.close();rs=null;
			sql = null;
			
			// add count to existingCount
			existingCount = existingCount+count;
			// update in SCH TABLE
			sql="UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_inlk_total = ? WHERE rmi_sch_id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, existingCount);
			pstmt.setLong(2, scheduleId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
			
			// update Count in ITEM TABLE
			sql="UPDATE gms3_mdm_rmi_tool_sch_item SET rmi_sch_inlk_total = ?  WHERE rmi_sch_item_id=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, count);
			pstmt.setLong(2, itemId);
			pstmt.executeUpdate();
			pstmt.close();pstmt = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateTotalInnerLinksCounts()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateTotalInnerLinksCounts()", re);
			}
			itemId = 0;
			scheduleId = 0;
			count =0;
		}
	}


	/*
	 * RUNNING COUNTS, BUFFERED PER JOB (2026-10-30, same as CD Creation - CDRomDAO.addCount).
	 * Every document, every image / attachment / PDF and every inner link used to bump its History
	 * counters with SELECT + UPDATE on the schedule row and again on the item row - 4 round trips plus
	 * the isValid pings, ~1.3 s on MC Dev - so a document with one image spent ~2.6 s on counters alone
	 * (RMI job 302: ~4 s per document). Each job has its own RMIExportTransactionDAO (RMIExportScheduleImpl.startProcessing),
	 * so the deltas live on this instance and are written as "col = COALESCE(col,0) + ?" at most every
	 * COUNT_FLUSH_INTERVAL_MS (10 s), and always before a status change, the inner-link total, a report summary
	 * and at the end of the job (flushCounts). History lags the job by a few seconds at most.
	 */
	// 10 s, not the CD's 3 s: a flush is 2-3 round trips (~0.5-1 s on MC Dev) and documents here take ~0.2 s
	private static final long COUNT_FLUSH_INTERVAL_MS = 10000L;
	private final LinkedHashMap<String, Long> pendingCounts = new LinkedHashMap<String, Long>();
	private long lastCountFlush = System.currentTimeMillis();

	/** Adds 1 to each column, all before any flush - so success|failure and total move together. */
	private synchronized void addCount(String scheduleId, String itemId, String caller, String... columns)
	{
		if(null==scheduleId || "".equals(scheduleId.trim()))
		{
			logger.info(caller+" :: Schedule id as Parameter is null.");
			return;
		}
		for(String column : columns)
		{
			String key = scheduleId.trim()+"\u0001"+(null==itemId ? "" : itemId.trim())+"\u0001"+column;
			Long current = pendingCounts.get(key);
			pendingCounts.put(key, Long.valueOf((null==current ? 0L : current.longValue())+1L));
		}
		if(System.currentTimeMillis()-lastCountFlush >= COUNT_FLUSH_INTERVAL_MS)
		{
			flushCounts();
		}
	}

	/** Writes every buffered count: one UPDATE per schedule row, one per item row. */
	public synchronized void flushCounts()
	{
		lastCountFlush = System.currentTimeMillis();
		if(pendingCounts.isEmpty())
		{
			return;
		}
		// row key "scheduleId\u0001itemId" (itemId blank = the schedule row) -> column -> delta
		LinkedHashMap<String, LinkedHashMap<String, Long>> rows = new LinkedHashMap<String, LinkedHashMap<String, Long>>();
		for(Map.Entry<String, Long> e : pendingCounts.entrySet())
		{
			String[] k = e.getKey().split("\u0001", -1);
			addTo(rows, k[0]+"\u0001", k[2], e.getValue());
			if(!"".equals(k[1]))
			{
				addTo(rows, k[0]+"\u0001"+k[1], k[2], e.getValue());
			}
		}
		pendingCounts.clear();
		long flushStarted = System.currentTimeMillis();
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
			for(Map.Entry<String, LinkedHashMap<String, Long>> row : rows.entrySet())
			{
				String[] k = row.getKey().split("\u0001", -1);
				boolean itemRow = !"".equals(k[1]);
				StringBuilder sql = new StringBuilder("UPDATE ").append(itemRow ? "gms3_mdm_rmi_tool_sch_item" : "gms3_mdm_rmi_tool_sch").append(" SET ");
				int c = 0;
				for(String column : row.getValue().keySet())
				{
					sql.append(c++ > 0 ? ", " : "").append(column).append("=COALESCE(").append(column).append(",0)+?");
				}
				sql.append(" WHERE rmi_sch_id=?").append(itemRow ? " AND rmi_sch_item_id=?" : "");
				pstmt = conn.prepareStatement(sql.toString());
				int i = 1;
				for(Long delta : row.getValue().values())
				{
					pstmt.setLong(i++, delta.longValue());
				}
				pstmt.setLong(i++, Long.parseLong(k[0]));
				if(itemRow)
				{
					pstmt.setLong(i, Long.parseLong(k[1]));
				}
				pstmt.executeUpdate();
				pstmt.close();pstmt = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "flushCounts()", e);
		}
		finally
		{
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - History count update (buffered, every 10 s)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis()-flushStarted, 0L);
			try
			{
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "flushCounts()", re);
			}
		}
	}

	private static void addTo(LinkedHashMap<String, LinkedHashMap<String, Long>> rows, String rowKey, String column, Long delta)
	{
		LinkedHashMap<String, Long> columns = rows.get(rowKey);
		if(null==columns)
		{
			columns = new LinkedHashMap<String, Long>();
			rows.put(rowKey, columns);
		}
		Long current = columns.get(column);
		columns.put(column, Long.valueOf((null==current ? 0L : current.longValue())+delta.longValue()));
	}

	public void updateProcessingCount(String scheduleId, String itemId, String status)
	{
		// BUFFERED - see addCount
		if(status.equals(ScheduleConstants.STATUS_SUCCESS))
		{
			addCount(scheduleId, itemId, "updateProcessingCount()", "rmi_sch_success_docs");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateProcessingCount()", "rmi_sch_failure_docs");
		}
	}

	public void updateItemInnerLinkProcessingStatus(String processingStatus, String completionStatus, long itemId)
	{
		flushCounts(); // buffered running counts first - see addCount
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
			sql="UPDATE gms3_mdm_rmi_tool_sch_item SET rmi_sch_inlk_processing_status = ? , rmi_sch_inlk_completion_status = ? WHERE rmi_sch_item_id=?";
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateItemInnerLinkProcessingStatus()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "updateItemInnerLinkProcessingStatus()", re);
			}
			itemId = 0;
			processingStatus = null;
			completionStatus =null;
		}
	}

	public void updateInnerLinkProcessingCount(String scheduleId, String itemId, String status)
	{
		// BUFFERED - see addCount
		if(status.equals(ScheduleConstants.STATUS_SUCCESS))
		{
			addCount(scheduleId, itemId, "updateInnerLinkProcessingCount()", "rmi_sch_inlk_success");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateInnerLinkProcessingCount()", "rmi_sch_inlk_failure");
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

			String tableNames=  "gms3_dmt_"+Utilities.tableLocale(locale)+"_nm_imdoc,gms3_dmt_"+Utilities.tableLocale(locale)+"_imdoc";
			String[] tok = tableNames.split(",");
			String sql="";
			for(int a=0;a<tok.length;a++)
			{
				sql="SELECT dc_model_folder_name FROM "+tok[a]+" WHERE dc_im_doc_id=? AND dc_doc_status=?";
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getModelFolderName()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getModelFolderName()", re);
			}
		}
		return modelFolderName;
	}
	
	public void updateOKAssetsCount(String scheduleId, String itemId, String status)
	{
		// BUFFERED - see addCount
		if(status.equals(ScheduleConstants.STATUS_SUCCESS))
		{
			addCount(scheduleId, itemId, "updateOKAssetsCount()", "rmi_sch_okassets_success", "rmi_sch_okassets_total");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateOKAssetsCount()", "rmi_sch_okassets_failure", "rmi_sch_okassets_total");
		}
	}

	public void createReportSummary(RMIExportToolReportSummaryDetails summaryDetails)
	{
		flushCounts(); // buffered running counts first - see addCount
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

			String sql="INSERT INTO gms3_mdm_rmi_sch_summary (rmi_sch_id,rmi_sch_report_name,rmi_sch_report_status,rmi_sch_total_count,"
					+ "rmi_sch_success_count,rmi_sch_failure_count) VALUES ("+summaryDetails.getScheduleId()+",?,?,?,?,?)";
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "createReportSummary()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "createReportSummary()", re);
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
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getEmailId()", e);
		}
		finally
		{
			wslId =null;
		}
		return emailId;
	}

	/*
	 * VIEW CONTENT DETAILS, PREFETCHED PER ITEM (2026-10-30). getViewContentDetails ran one SELECT per
	 * document (~0.4 s on MC Dev). prefetchViewContentDetails reads the whole item VIEW_CONTENT_CHUNK
	 * documents at a time - same tables, same columns, same locale filter - and getViewContentDetails
	 * hands each document its rows once. A document that was not prefetched, is asked for a second
	 * time, or a prefetch that failed goes to the DB exactly as before.
	 */
	private static final int VIEW_CONTENT_CHUNK = 500;
	// lower-case document id -> rows (empty = the document has no view content rows)
	private HashMap<String, List<MMEViewContentDetails>> viewContentPrefetched = null;
	private String viewContentPrefetchedLocale = null;

	private static boolean isManualDocument(String documentId)
	{
		return documentId.startsWith("SM") || documentId.startsWith("OSM") || documentId.startsWith("WD");
	}

	private static String viewContentTable(String documentId, String locale)
	{
		return isManualDocument(documentId) ? "gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(locale) : "gms3_vc_mme_vin_si_detail";
	}

	public void prefetchViewContentDetails(List<?> documentIds, String locale, String modelName, String carlineCode)
	{
		viewContentPrefetched = null;
		viewContentPrefetchedLocale = null;
		if(null==documentIds || documentIds.isEmpty() || null==locale)
		{
			return;
		}
		long started = System.currentTimeMillis();
		locale = locale.replace("-", "_");
		HashMap<String, List<MMEViewContentDetails>> prefetched = new HashMap<String, List<MMEViewContentDetails>>();
		// table -> distinct document ids
		LinkedHashMap<String, List<String>> idsPerTable = new LinkedHashMap<String, List<String>>();
		for(Object id : documentIds)
		{
			String documentId = String.valueOf(id);
			if(prefetched.containsKey(documentId.toLowerCase()))
			{
				continue;
			}
			prefetched.put(documentId.toLowerCase(), new ArrayList<MMEViewContentDetails>());
			String table = viewContentTable(documentId, locale);
			if(!idsPerTable.containsKey(table))
			{
				idsPerTable.put(table, new ArrayList<String>());
			}
			idsPerTable.get(table).add(documentId);
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int queries = 0;
		int rows = 0;
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
			for(Map.Entry<String, List<String>> table : idsPerTable.entrySet())
			{
				boolean manualTable = !"gms3_vc_mme_vin_si_detail".equals(table.getKey());
				List<String> ids = table.getValue();
				for(int from=0;from<ids.size();from+=VIEW_CONTENT_CHUNK)
				{
					List<String> chunk = ids.subList(from, Math.min(from+VIEW_CONTENT_CHUNK, ids.size()));
					StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table.getKey()).append(" WHERE vc_vin_document_id IN (");
					for(int i=0;i<chunk.size();i++)
					{
						sql.append(i>0 ? ",?" : "?");
					}
					sql.append(") AND vc_vin_locale=?");
					pstmt = conn.prepareStatement(sql.toString());
					int p = 1;
					for(String id : chunk)
					{
						pstmt.setString(p++, id);
					}
					pstmt.setString(p, locale);
					rs = pstmt.executeQuery();
					queries++;
					while(rs.next())
					{
						List<MMEViewContentDetails> docRows = prefetched.get(String.valueOf(rs.getString("vc_vin_document_id")).toLowerCase());
						if(null!=docRows)
						{
							docRows.add(readViewContentRow(rs, manualTable, modelName, carlineCode));
							rows++;
						}
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
				}
			}
			viewContentPrefetched = prefetched;
			viewContentPrefetchedLocale = locale;
			logger.info("prefetchViewContentDetails :: "+prefetched.size()+" documents, "+queries+" queries, "+rows+" rows in "+(System.currentTimeMillis()-started)+" ms");
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - view content details (batched, 500 documents per query)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis()-started, 0L);
		}
		catch(Exception e)
		{
			// fall back to one SELECT per document
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "prefetchViewContentDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "prefetchViewContentDetails()", re);
			}
		}
	}

	private static MMEViewContentDetails readViewContentRow(ResultSet rs, boolean manualTable, String modelName, String carlineCode) throws SQLException
	{
		MMEViewContentDetails details = new MMEViewContentDetails();
		// COMMON VARIABLES
		details.setDocumentId(rs.getString("vc_vin_document_id"));
		details.setLocale(rs.getString("vc_vin_locale"));
		details.setModelName(modelName);
		details.setCarlineCode(carlineCode);
		details.setCarlineName(rs.getString("vc_vin_carline_name"));
		details.setWmiCode(rs.getString("vc_vin_wmi_code"));
		details.setVdsCode(rs.getString("vc_vin_vds_code"));
		details.setVisStartRange(rs.getString("vc_vin_vis_start_range"));
		details.setVisEndRange(rs.getString("vc_vin_vis_end_range"));
		details.setEsiCategoryName1(rs.getString("vc_vin_category_name_1"));
		details.setEsiCategoryName2(rs.getString("vc_vin_category_name_2"));
		details.setEsiCategoryName3(rs.getString("vc_vin_category_name_3"));
		details.setTitle(rs.getString("vc_vin_document_title"));
		details.setImDocModifiedDate(rs.getTimestamp("vc_vin_imdoc_modified_date"));
		details.setDocumentType(rs.getString("vc_vin_document_type"));
		details.setDocumentTypeName(rs.getString("vc_vin_document_type_name"));
		details.setDocumentSubType(rs.getString("vc_vin_document_subtype"));
		details.setDocumentSubTypeName(rs.getString("vc_vin_document_subtype_name"));
		// DOCUMENT SPECIFIC VARIABLES
		if(manualTable)
		{
			details.setDisplayOrderName1(rs.getString("vc_vin_disp_name_1"));
			details.setDisplayOrderName2(rs.getString("vc_vin_disp_name_2"));
			details.setDisplayOrderName3(rs.getString("vc_vin_disp_name_3"));
			details.setDisplayOrderName4(rs.getString("vc_vin_disp_name_4"));
			details.setDisplayOrderName5(rs.getString("vc_vin_disp_name_5"));
			details.setDisplayOrderName6(rs.getString("vc_vin_disp_name_6"));
			details.setSequenceNo(rs.getString("vc_vin_dispord_seq_no"));
		}
		else
		{
			details.setSiNumber(rs.getString("vc_vin_si_number"));
		}
		return details;
	}

	public List<MMEViewContentDetails> getViewContentDetails(String documentId, String locale,String modelName, String carlineCode)
	{
		if(null!=viewContentPrefetched && null!=documentId && null!=locale && locale.replace("-", "_").equals(viewContentPrefetchedLocale))
		{
			List<MMEViewContentDetails> prefetchedRows = viewContentPrefetched.remove(documentId.toLowerCase());
			if(null!=prefetchedRows)
			{
				logger.info("getViewContentDetails :: {"+documentId+"} :: "+prefetchedRows.size()+" rows (prefetched).");
				return prefetchedRows.isEmpty() ? null : prefetchedRows;
			}
		}
		logger.info("getViewContentDetails :: > Start Fetching ViewContentDetails.");
		long viewContentStarted = System.currentTimeMillis();
		List<MMEViewContentDetails> viewContentList = null;
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
			if(documentId.startsWith("SM") || documentId.startsWith("OSM") || documentId.startsWith("WD"))
			{
				tableName = "gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(locale);
			}
			else
			{
				tableName = "gms3_vc_mme_vin_si_detail";
			}
			
			String sql="";
			sql="SELECT * FROM "+tableName+" WHERE vc_vin_document_id=? AND vc_vin_locale=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, documentId);
			pstmt.setString(2, locale);
			rs = pstmt.executeQuery();
			MMEViewContentDetails details = null;
			while(rs.next())
			{
				details = readViewContentRow(rs, isManualDocument(documentId), modelName, carlineCode);

				if(null==viewContentList || viewContentList.size()<=0)
				{
					viewContentList = new ArrayList<MMEViewContentDetails>();
				}
				viewContentList.add(details);
				details = null;
			}	
			sql = null;
			details = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getViewContentDetails()", e);
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
				Utilities.printStackTraceToLogs(RMIExportTransactionDAO.class.getName(), "getViewContentDetails()", re);
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
		com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - view content details (one document, fallback)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis()-viewContentStarted, 0L);
		return viewContentList;
	}

	
}