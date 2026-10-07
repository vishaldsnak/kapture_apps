package com.mazda.gms3.dmt.autosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

	public void updateProcessingCount(String scheduleId, String itemId, String status)
	{
		Connection conn = null;
		PreparedStatement stmt =null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "";
			if(status.equals(AutoSyncConstants.STATUS_SUCCESS))
			{
				sql = "SELECT AS_PROCESSED_COUNT AS COUNT FROM gms3_auto_sync_sch WHERE AS_SCHEDULE_ID="+scheduleId;
			}
			else
			{
				sql = "SELECT AS_FAILED_COUNT AS COUNT FROM gms3_auto_sync_sch WHERE AS_SCHEDULE_ID="+scheduleId;
			}
			stmt = conn.prepareStatement(sql);
			rs= stmt.executeQuery();
			int count=0;
			if(rs.next())
			{
				count= rs.getInt("COUNT");
			}
			stmt.close();stmt=null;
			rs.close();rs=null;
			sql = null;
			
			// increment count by 1
			count = count+1;
			
			// update count
			if(status.equals(AutoSyncConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_auto_sync_sch  SET AS_PROCESSED_COUNT = ? WHERE AS_SCHEDULE_ID="+scheduleId;
			}
			else
			{
				sql = "UPDATE gms3_auto_sync_sch  SET AS_FAILED_COUNT = ? WHERE AS_SCHEDULE_ID="+scheduleId;
			}
			stmt  = conn.prepareStatement(sql);
			stmt.setInt(1, count);
			stmt.executeUpdate();
			stmt.close();stmt=null;
			sql = null;
			
			// NOW UPDATE ITEMS COUNT
			if(status.equals(AutoSyncConstants.STATUS_SUCCESS))
			{
				sql = "SELECT AS_PROCESSED_COUNT AS COUNT FROM gms3_auto_sync_sch_items WHERE AS_ITEM_ID="+itemId;
			}
			else
			{
				sql = "SELECT AS_FAILED_COUNT AS COUNT FROM gms3_auto_sync_sch_items WHERE AS_ITEM_ID="+itemId;
			}
			stmt = conn.prepareStatement(sql);
			rs= stmt.executeQuery();
			int itemCount=0;
			if(rs.next())
			{
				itemCount= rs.getInt("COUNT");
			}
			stmt.close();stmt=null;
			rs.close();rs=null;
			sql = null;
			
			// increment itemCount by 1
			itemCount = itemCount+1;
			
			// update count
			if(status.equals(AutoSyncConstants.STATUS_SUCCESS))
			{
				sql = "UPDATE gms3_auto_sync_sch_items  SET AS_PROCESSED_COUNT = ? WHERE AS_ITEM_ID="+itemId;
			}
			else
			{
				sql = "UPDATE gms3_auto_sync_sch_items  SET AS_FAILED_COUNT = ? WHERE AS_ITEM_ID="+itemId;
			}
			stmt  = conn.prepareStatement(sql);
			stmt.setInt(1, itemCount);
			stmt.executeUpdate();
			stmt.close();stmt=null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateProcessingCount()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(MasterDataSyncTransactionDAO.class.getName(), "updateProcessingCount()", e);
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
