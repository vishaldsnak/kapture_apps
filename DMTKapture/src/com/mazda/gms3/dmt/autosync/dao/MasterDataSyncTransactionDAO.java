package com.mazda.gms3.dmt.autosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;

public class MasterDataSyncTransactionDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(MasterDataSyncTransactionDAO.class);

	public void updateJobStatus(String schdeuleId,String jobStatus, String scheduleStatus)
	{
		Connection conn = null;
		PreparedStatement stmt =null;
		try
		{
			conn = getConnection();
			String sql = "UPDATE gms3_auto_sync_sch SET AS_JOB_STATUS='"+jobStatus+"' ";
			if(jobStatus.equals(AutoSyncConstants.STATUS_COMPLETED) || jobStatus.equals(AutoSyncConstants.STATUS_ABORTED))
			{
				// add finish time as well as scheduleStatus
				sql = sql+", AS_FINISH_TMSTP = ?, AS_SCH_STATUS = ? ";
			}
			sql=sql+" WHERE AS_SCHEDULE_ID=? ";
			stmt = conn.prepareStatement(sql);
			if(jobStatus.equals(AutoSyncConstants.STATUS_COMPLETED) || jobStatus.equals(AutoSyncConstants.STATUS_ABORTED))
			{
				stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				stmt.setString(2, scheduleStatus);
				stmt.setLong(3, new Long(schdeuleId).longValue());
			}
			else
			{
				stmt.setLong(1, new Long(schdeuleId).longValue());
			}
			stmt.executeUpdate();
			sql = null;
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateJobStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateJobStatus()", e);
			}
		}
	}

	public void updateItemStatus(String itemId,String status)
	{
		Connection conn = null;
		PreparedStatement stmt =null;
		try
		{
			conn = getConnection();
			String sql = "UPDATE gms3_auto_sync_sch_items SET AS_PROCESSING_STATUS='"+status+"' ";
			sql=sql+" WHERE AS_ITEM_ID=? ";
			stmt = conn.prepareStatement(sql);
			stmt.setLong(1, new Long(itemId).longValue());
			stmt.executeUpdate();
			sql = null;
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateItemStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateItemStatus()", e);
			}
		}
	}

	/*
	 * ADDS processed / failed to the counts of the schedule and of the item - one UPDATE each, no read first.
	 * Called by MasterDataSyncCountBuffer every few seconds (was 4 statements per category row).
	 */
	public void addProcessingCounts(long scheduleId, long itemId, int processed, int failed)
	{
		if(processed<=0 && failed<=0)
		{
			return;
		}
		Connection conn = null;
		PreparedStatement stmt =null;
		try
		{
			conn = getConnection();
			stmt = conn.prepareStatement("UPDATE gms3_auto_sync_sch SET AS_PROCESSED_COUNT = COALESCE(AS_PROCESSED_COUNT,0) + ?,"
					+ " AS_FAILED_COUNT = COALESCE(AS_FAILED_COUNT,0) + ? WHERE AS_SCHEDULE_ID = ?");
			stmt.setInt(1, processed);
			stmt.setInt(2, failed);
			stmt.setLong(3, scheduleId);
			stmt.executeUpdate();
			stmt.close();stmt=null;
			stmt = conn.prepareStatement("UPDATE gms3_auto_sync_sch_items SET AS_PROCESSED_COUNT = COALESCE(AS_PROCESSED_COUNT,0) + ?,"
					+ " AS_FAILED_COUNT = COALESCE(AS_FAILED_COUNT,0) + ? WHERE AS_ITEM_ID = ?");
			stmt.setInt(1, processed);
			stmt.setInt(2, failed);
			stmt.setLong(3, itemId);
			stmt.executeUpdate();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "addProcessingCounts()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "addProcessingCounts()", e);
			}
		}
	}

	public void updateMDMItemSyncStatus(Map<String, List<String>> dataMap)
	{
		Connection conn = null;
		try
		{
			if(null!=dataMap)
			{
				conn = getConnection();
				String sql="";
				List<String> itemsList = null;
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_AXLE_TYPE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_AXLE_TYPE);
					sql="UPDATE gms3_mdm_axle_type SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_AT_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_BODY_TYPE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_BODY_TYPE);
					sql="UPDATE gms3_mdm_body_type SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_BT_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_MODEL_YEAR))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_MODEL_YEAR);
					sql="UPDATE gms3_mdm_carline_codes SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_CRLN_CODE_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_CARLINE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_CARLINE);
					sql="UPDATE gms3_mdm_carline_codes SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_CRLN_CODE_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_VIN_RANGE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_VIN_RANGE);
					sql="UPDATE gms3_mdm_vin_detail SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_VIN_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get("VIN_FOR_MC_MME"))
				{
					itemsList = (ArrayList<String>)dataMap.get("VIN_FOR_MC_MME");
					sql="UPDATE gms3_mdm_vin_detail SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_VIN_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_ESI_CATEGORY);
					sql="UPDATE gms3_mdm_category SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_CAT_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_CVC_CATEGORY);
					sql="UPDATE gms3_mdm_cvc_details SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_CVC_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_ENGINE_TYPE);
					sql="UPDATE gms3_mdm_engine_type SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_ET_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE);
					sql="UPDATE gms3_mdm_trans_type SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE MDM_TRANS_TYPE_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				if(null!=dataMap.get(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE))
				{
					itemsList = (ArrayList<String>)dataMap.get(AutoSyncConstants.ITEM_KEY_MANUAL_TYPE);
					sql="UPDATE gms3_dmt_conv_manual_type SET MDM_SYNC_STATUS='"+AutoSyncConstants.STATUS_YES+"' WHERE DC_MANUAL_MT_ID=";
					processMDMItemSyncStatus(sql, itemsList, conn);
				}
				sql = null;
				itemsList=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateItemStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				dataMap = null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateItemStatus()", e);
			}
		}
	}

	/*
	 * SYNC STATUS Y FOR THE GIVEN MDM ROWS - one UPDATE ... WHERE <id> IN (...) per db.write.batch.size ids (was one
	 * UPDATE per row, ~250 ms each through the MySQL Router on MC Dev: 264 ESI rows = 66 s). sql ends with "WHERE <id>=".
	 */
	private void processMDMItemSyncStatus(String sql, List<String> itemsList, Connection conn)
	{
		PreparedStatement stmt =null;
		try
		{
			if(null!=itemsList && itemsList.size()>0)
			{
				long startedAt = System.currentTimeMillis();
				String base = sql.substring(0, sql.lastIndexOf('=')).trim();
				int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
				int updated = 0;
				for(int from=0; from<itemsList.size(); from+=chunk)
				{
					List<String> part = itemsList.subList(from, Math.min(from+chunk, itemsList.size()));
					stmt = conn.prepareStatement(base+" IN ("+String.join(",", java.util.Collections.nCopies(part.size(), "?"))+")");
					int p=1;
					for(String id : part)
					{
						stmt.setLong(p++, Long.parseLong(String.valueOf(id).trim()));
					}
					updated += stmt.executeUpdate();
					stmt.close();stmt=null;
				}
				logger.info("processMDMItemSyncStatus :: " + base + " IN (...) :: ids " + itemsList.size() + " :: rows updated " + updated
						+ " :: " + (System.currentTimeMillis() - startedAt) + " ms");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "processMDMItemSyncStatus()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "processMDMItemSyncStatus()", e);
			}
		}
	}

}
