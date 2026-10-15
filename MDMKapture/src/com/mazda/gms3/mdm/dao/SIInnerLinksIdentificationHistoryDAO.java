package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.SIInnerLinksIdentificationTransactionDetails;

public class SIInnerLinksIdentificationHistoryDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SIInnerLinksIdentificationHistoryDAO.class);
	
	public static ArrayList<SIInnerLinksIdentificationTransactionDetails> getHistoryDataList() throws SQLException
	{
		ArrayList<SIInnerLinksIdentificationTransactionDetails> historyList = new ArrayList<SIInnerLinksIdentificationTransactionDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_si_sm_inrlks_summary ORDER BY sism_schedule_id DESC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SIInnerLinksIdentificationTransactionDetails details =new SIInnerLinksIdentificationTransactionDetails();
				details.setSrNo(historyList.size()+1);
				details.setJobId(rs.getLong("sism_schedule_id"));
				details.setJobName(rs.getString("sism_schedule_name"));
				details.setJobStatus(rs.getString("sism_job_status"));
				details.setProcessingStatus(rs.getString("sism_schedule_status"));
				details.setTotalDocumentsCount(rs.getLong("sism_total_docs_count"));
				details.setProcessedDocumentsCount(rs.getLong("sism_processed_docs_count"));
				details.setFailureDocumentsCount(rs.getLong("sism_failed_docs_count"));
				details.setTotalInnerLinksCount(rs.getLong("sism_total_inrlks_count"));
				details.setActiveInnerLinksCount(rs.getLong("sism_active_inrlks_count"));
				details.setInactiveInnerLinksCount(rs.getLong("sism_inactive_inrlks_count"));
				details.setStartTime(rs.getTimestamp("sism_start_time"));
				details.setFinishTime(rs.getTimestamp("sism_finish_time"));
				historyList.add(details);
				details= null;
			}
			sql = null;
			if(null!=historyList && historyList.size()>0)
			{
				logger.info("getHistoryDataList :: Total History Transaction Data Found are :: > "+ historyList.size());
			}
			else
			{
				logger.info("getHistoryDataList :: No Previous History Transaction Data Found SI INNERLINKS IDENTIFICATION.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistoryDAO.class.getName(), "getHistoryDataList()", e);
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
		return historyList;
	}
	
	public static ArrayList<SIInnerLinksIdentificationTransactionDetails> getReportsList(String scheduleId) throws SQLException
	{
		ArrayList<SIInnerLinksIdentificationTransactionDetails> reportsList = new ArrayList<SIInnerLinksIdentificationTransactionDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_si_sm_inrlks_reports WHERE sism_schedule_id="+scheduleId+" "
						+ " ORDER BY sism_reports_locale ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SIInnerLinksIdentificationTransactionDetails details =new SIInnerLinksIdentificationTransactionDetails();
					details.setSrNo(reportsList.size()+1);
					details.setReportLocale(rs.getString("sism_reports_locale"));
					String reportsPath=rs.getString("sism_reports_path");
					if(null!=reportsPath && !"".equals(reportsPath) && !"null".equals(reportsPath.trim().toLowerCase()))
					{
						details.setReportsPath(reportsPath);
					}
					reportsPath = null;
					reportsList.add(details);
					details= null;
				}
				sql = null;
				if(null!=reportsList && reportsList.size()>0)
				{
					logger.info("getHistoryDataList :: Total Reports Data for Schedule Id {"+scheduleId+"} Found are :: > "+ reportsList.size());
				}
				else
				{
					logger.info("getHistoryDataList :: No Reports Data Found for Schedule Id :: > " + scheduleId);
				}
			}
			else
			{
				logger.info("getReportsList :: Schedule id as parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistoryDAO.class.getName(), "getReportsList()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
			scheduleId = null;
		}
		return reportsList;
	}
	
	
}
