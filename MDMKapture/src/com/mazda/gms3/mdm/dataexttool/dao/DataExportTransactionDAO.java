package com.mazda.gms3.mdm.dataexttool.dao;

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
		flushCounts(); // buffered running counts first - see addCount
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


	/*
	 * RUNNING COUNTS, BUFFERED PER JOB (2026-10-30, same as CD Creation - CDRomDAO.addCount).
	 * Every document, every image / attachment / PDF and every inner link used to bump its History
	 * counters with SELECT + UPDATE on the schedule row and again on the item row - 4 round trips plus
	 * the isValid pings, ~1.3 s on MC Dev - so a document with one image spent ~2.6 s on counters alone
	 * (same code as RMI job 302: ~4 s per document). Each job has its own DataExportTransactionDAO (DataExportScheduleImpl.startProcessing),
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
				StringBuilder sql = new StringBuilder("UPDATE ").append(itemRow ? "gms3_mdm_mde_tool_sch_it" : "gms3_mdm_mde_tool_sch").append(" SET ");
				int c = 0;
				for(String column : row.getValue().keySet())
				{
					sql.append(c++ > 0 ? ", " : "").append(column).append("=COALESCE(").append(column).append(",0)+?");
				}
				sql.append(" WHERE mde_sch_id=?").append(itemRow ? " AND mde_sch_item_id=?" : "");
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
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "flushCounts()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "flushCounts()", re);
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
			addCount(scheduleId, itemId, "updateProcessingCount()", "mde_sch_success_docs");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateProcessingCount()", "mde_sch_failure_docs");
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
		// BUFFERED - see addCount
		if(status.equals(ScheduleConstants.STATUS_SUCCESS))
		{
			addCount(scheduleId, itemId, "updateInnerLinkProcessingCount()", "mde_sch_inlk_success");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateInnerLinkProcessingCount()", "mde_sch_inlk_failure");
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
		// BUFFERED - see addCount
		if(status.equals(ScheduleConstants.STATUS_SUCCESS))
		{
			addCount(scheduleId, itemId, "updateOKAssetsCount()", "mde_sch_okassets_success", "mde_sch_okassets_total");
		}
		else if(status.equals(ScheduleConstants.STATUS_FAILURE))
		{
			addCount(scheduleId, itemId, "updateOKAssetsCount()", "mde_sch_okassets_failure", "mde_sch_okassets_total");
		}
	}

	public void createReportSummary(DataExportToolReportSummaryDetails summaryDetails)
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

	/*
	 * VIEW CONTENT DETAILS, PREFETCHED PER ITEM (2026-10-30, same as RMIExportTransactionDAO).
	 * getViewContentDetails ran two SELECTs per document (model year + VIN, ~0.4 s each on MC Dev).
	 * prefetchViewContentDetails reads the whole item VIEW_CONTENT_CHUNK documents at a time - same
	 * tables, same columns, same locale filter, model year rows first then VIN rows as before - and
	 * getViewContentDetails hands each document its rows once. A document that was not prefetched, is
	 * asked for a second time, or a prefetch that failed goes to the DB exactly as before.
	 */
	private static final int VIEW_CONTENT_CHUNK = 500;
	// lower-case document id -> rows (empty = the document has no view content rows)
	private HashMap<String, List<MNAOViewContentDetails>> viewContentPrefetched = null;
	private String viewContentPrefetchedLocale = null;

	public void prefetchViewContentDetails(List<?> documentIds, String locale, String modelName)
	{
		viewContentPrefetched = null;
		viewContentPrefetchedLocale = null;
		if(null==documentIds || documentIds.isEmpty() || null==locale)
		{
			return;
		}
		long started = System.currentTimeMillis();
		locale = locale.replace("-", "_");
		// lower-case id -> id as the item lists it (the SI check below reads it, as before)
		LinkedHashMap<String, String> ids = new LinkedHashMap<String, String>();
		for(Object id : documentIds)
		{
			String documentId = String.valueOf(id);
			if(!ids.containsKey(documentId.toLowerCase()))
			{
				ids.put(documentId.toLowerCase(), documentId);
			}
		}
		HashMap<String, List<MNAOViewContentDetails>> modelYearRows = new HashMap<String, List<MNAOViewContentDetails>>();
		HashMap<String, List<MNAOViewContentDetails>> vinRows = new HashMap<String, List<MNAOViewContentDetails>>();
		List<String> idList = new ArrayList<String>(ids.values());
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
			for(int pass=0;pass<2;pass++)
			{
				boolean modelYear = (pass==0);
				String idColumn = modelYear ? "vc_my_document_id" : "vc_vin_document_id";
				for(int from=0;from<idList.size();from+=VIEW_CONTENT_CHUNK)
				{
					List<String> chunk = idList.subList(from, Math.min(from+VIEW_CONTENT_CHUNK, idList.size()));
					StringBuilder sql = new StringBuilder("SELECT * FROM ")
							.append(modelYear ? "gms3_vc_model_year_details" : "gms3_vc_vin_details")
							.append(" WHERE ").append(idColumn).append(" IN (");
					for(int i=0;i<chunk.size();i++)
					{
						sql.append(i>0 ? ",?" : "?");
					}
					sql.append(") AND ").append(modelYear ? "vc_my_locale" : "vc_vin_locale").append("=?");
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
						String key = String.valueOf(rs.getString(idColumn)).toLowerCase();
						String documentId = ids.get(key);
						if(null==documentId)
						{
							continue;
						}
						HashMap<String, List<MNAOViewContentDetails>> target = modelYear ? modelYearRows : vinRows;
						if(!target.containsKey(key))
						{
							target.put(key, new ArrayList<MNAOViewContentDetails>());
						}
						MNAOViewContentDetails details = modelYear ? readModelYearRow(rs, documentId.startsWith("SI"), modelName)
								: readVinRow(rs, documentId.startsWith("SI"), modelName);
						// viewContentType as MODEL_YEAR / VIN, as getViewContentDetails sets it
						details.setViewContentType(modelYear ? "MODEL_YEAR" : "VIN");
						target.get(key).add(details);
						rows++;
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
				}
			}
			HashMap<String, List<MNAOViewContentDetails>> prefetched = new HashMap<String, List<MNAOViewContentDetails>>();
			for(String key : ids.keySet())
			{
				List<MNAOViewContentDetails> docRows = new ArrayList<MNAOViewContentDetails>();
				if(modelYearRows.containsKey(key))
				{
					docRows.addAll(modelYearRows.get(key));
				}
				if(vinRows.containsKey(key))
				{
					docRows.addAll(vinRows.get(key));
				}
				prefetched.put(key, docRows);
			}
			viewContentPrefetched = prefetched;
			viewContentPrefetchedLocale = locale;
			logger.info("prefetchViewContentDetails :: "+prefetched.size()+" documents, "+queries+" queries, "+rows+" rows in "+(System.currentTimeMillis()-started)+" ms");
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - view content details (batched, 500 documents per query)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis()-started, 0L);
		}
		catch(Exception e)
		{
			// fall back to two SELECTs per document
			Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "prefetchViewContentDetails()", e);
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
				Utilities.printStackTraceToLogs(DataExportTransactionDAO.class.getName(), "prefetchViewContentDetails()", re);
			}
		}
	}

	private static MNAOViewContentDetails readModelYearRow(ResultSet rs, boolean siDocument, String modelName) throws SQLException
	{
		MNAOViewContentDetails details = new MNAOViewContentDetails();
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
		if(siDocument)
		{
			details.setSiNumber(rs.getString("vc_my_si_number"));
		}
		return details;
	}

	private static MNAOViewContentDetails readVinRow(ResultSet rs, boolean siDocument, String modelName) throws SQLException
	{
		MNAOViewContentDetails details = new MNAOViewContentDetails();
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
		if(siDocument)
		{
			details.setSiNumber(rs.getString("vc_vin_si_number"));
		}
		return details;
	}

	public List<MNAOViewContentDetails> getViewContentDetails(String documentId, String locale,String modelName)
	{
		if(null!=viewContentPrefetched && null!=documentId && null!=locale && locale.replace("-", "_").equals(viewContentPrefetchedLocale))
		{
			List<MNAOViewContentDetails> prefetchedRows = viewContentPrefetched.remove(documentId.toLowerCase());
			if(null!=prefetchedRows)
			{
				logger.info("getViewContentDetails :: {"+documentId+"} :: "+prefetchedRows.size()+" rows (prefetched).");
				return prefetchedRows.isEmpty() ? null : prefetchedRows;
			}
		}
		logger.info("getViewContentDetails :: > Start Fetching ViewContentDetails.");
		long viewContentStarted = System.currentTimeMillis();
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
				details = readModelYearRow(rs, documentId.startsWith("SI"), modelName);
				
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
				details = readVinRow(rs, documentId.startsWith("SI"), modelName);
				
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
		com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - view content details (one document, fallback)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis()-viewContentStarted, 0L);
		return viewContentList;
	}

	
}