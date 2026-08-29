package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.RumVinDetails;
import com.mazda.gms3.mdm.vo.RumVinScheduleDetails;

public class RumVinScheduleDAO extends DBConnectionHelper{
	
	private static Logger logger = LogManager.getLogger(RumVinScheduleDAO.class);
	
	public static ArrayList<RumVinScheduleDetails> getTransactionsList() throws SQLException
	{
		ArrayList<RumVinScheduleDetails> transactionList = new ArrayList<RumVinScheduleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_rum_vin_sch_dtl ORDER BY mdm_schedule_id DESC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RumVinScheduleDetails details =new RumVinScheduleDetails();
				details.setSrNo(transactionList.size()+1);
				details.setJobId(rs.getLong("mdm_schedule_id"));
				details.setJobName(rs.getString("mdm_schedule_name"));
				details.setJobStatus(rs.getString("mdm_job_status"));
				details.setProcessingStatus(rs.getString("mdm_sch_status"));
				
				details.setTotalLinesCount(rs.getLong("mdm_total_lines_count"));
				details.setValidLinesCount(rs.getLong("mdm_total_valid_lines_count"));
				details.setSuccessCount(rs.getLong("mdm_success_count"));
				details.setFailureCount(rs.getLong("mdm_failure_count"));
				
				details.setStartTime(rs.getTimestamp("mdm_sch_start_tmstp"));
				details.setFinishTime(rs.getTimestamp("mdm_sch_finish_tmstp"));
				
				details.setRemakrs(rs.getString("mdm_sch_remarks"));
				details.setFileName(rs.getString("mdm_file_name"));
				details.setReportsPath(rs.getString("mdm_reports_path"));
				
				transactionList.add(details);
				details= null;
			}
			sql = null;
			if(null!=transactionList && transactionList.size()>0)
			{
				logger.info("getTransactionsList :: Total Schedule Transaction Data Found are :: > "+ transactionList.size());
			}
			else
			{
				logger.info("getTransactionsList :: No Previous Schedule Transaction Data Found for RUM VIN Mapping.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinScheduleDAO.class.getName(), "getTransactionsList()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return transactionList;
	}
	
	public static ArrayList<RumVinDetails> getRumVinDataList() throws SQLException
	{
		ArrayList<RumVinDetails> dataList = new ArrayList<RumVinDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_rum_vin_mapping order by mdm_rum_vin_created_tmstp, mdm_rum_vin_modified_tmstp DESC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RumVinDetails details =new RumVinDetails();
				details.setSrNo(dataList.size()+1);
				details.setRussiaVin(rs.getString("mdm_rum_vin"));
				details.setMscModelBase(rs.getString("mdm_msc_model_base"));
				details.setMscModelCode(rs.getString("mdm_msc_model_code"));
				details.setMscSpecCode(rs.getString("mdm_msc_model_spec_code"));
				
				details.setExtColorCode(rs.getString("mdm_ext_color_code"));
				details.setIntColorCode(rs.getString("mdm_int_color_code"));
				details.setLineCode(rs.getString("mdm_line_code"));
				details.setEngineType(rs.getString("mdm_engine_type"));
				details.setEngineStampedNo(rs.getString("mdm_engine_stamped_no"));
				details.setProdDateAtRussia(rs.getString("mdm_prod_date_russia"));
				details.setRetailDate(rs.getString("mdm_retail_date"));
				details.setWarrantyDistrict(rs.getString("mdm_warranty_district"));
				details.setDealerCode(rs.getString("mdm_dealer_code"));
				details.setShippingDistrict(rs.getString("mdm_shipping_district"));
				details.setShippingDateAtRussia(rs.getString("mdm_shipping_date_russia"));
				
				details.setIgnitionKey(rs.getString("mdm_ignition_key"));
				details.setMazdaVin(rs.getString("mdm_mazda_vin"));
				details.setProdDateAtMazda(rs.getString("mdm_prod_date_mazda"));
				details.setShippingDateAtMazda(rs.getString("mdm_shipping_date_mazda"));
				
				details.setCreatedTime(rs.getTimestamp("mdm_rum_vin_created_tmstp"));
				details.setModifiedTime(rs.getTimestamp("mdm_rum_vin_modified_tmstp"));
				
				dataList.add(details);
				details= null;
			}
			sql = null;
			if(null!=dataList && dataList.size()>0)
			{
				logger.info("getRumVinDataList :: Total RUM VIN Mapping Data Found are :: > "+ dataList.size());
			}
			else
			{
				logger.info("getRumVinDataList :: No Data Found for RUM VIN Mapping.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinScheduleDAO.class.getName(), "getRumVinDataList()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return dataList;
	}

}
