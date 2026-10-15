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
import com.mazda.gms3.mdm.vo.ESILabelUpdateScheduleDetails;

public class ESILabelsUpdateScheduleDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(ESILabelsUpdateScheduleDAO.class);
	
	public static ArrayList<ESILabelUpdateScheduleDetails> getTransactionsList() throws SQLException
	{
		ArrayList<ESILabelUpdateScheduleDetails> transactionList = new ArrayList<ESILabelUpdateScheduleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_mme_esicat_sch_dtl ORDER BY mdm_schedule_id DESC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				ESILabelUpdateScheduleDetails details =new ESILabelUpdateScheduleDetails();
				details.setSrNo(transactionList.size()+1);
				details.setJobId(rs.getLong("mdm_schedule_id"));
				details.setJobName(rs.getString("mdm_schedule_name"));
				details.setJobStatus(rs.getString("mdm_job_status"));
				details.setProcessingStatus(rs.getString("mdm_sch_status"));
				
				details.setTotalCount(rs.getLong("mdm_total_docs_count"));
				details.setSuccessCount(rs.getLong("mdm_success_count"));
				details.setFailureCount(rs.getLong("mdm_failure_count"));
				
				details.setStartTime(rs.getTimestamp("mdm_sch_start_tmstp"));
				details.setFinishTime(rs.getTimestamp("mdm_sch_finish_tmstp"));
				
				details.setRemakrs(rs.getString("mdm_sch_remarks"));
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
			Utilities.printStackTraceToLogs(ESILabelsUpdateScheduleDAO.class.getName(), "getTransactionsList()", e);
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
	
}
