package com.mazda.gms3.mdm.nmdtool.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.nmdtool.vo.NMDJobDetails;
import com.mazda.gms3.mdm.nmdtool.vo.NMDScheduleDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;

public class NMDScheduleDAO extends DBConnectionHelper{
	
	private static Logger logger= LogManager.getLogger(NMDScheduleDAO.class);
	
	public static List<NMDScheduleDetails> getModelDetails(String locale, String loggedInUser)
	{
		List<NMDScheduleDetails> modelsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		Statement stmt=null;
		try
		{
			if(null!=locale && !"".equals(locale))
			{
				conn =getConnection();
				// set AutoCommit to false
				conn.setAutoCommit(false);
				// fetch Active Model List
				String sql="select DISTINCT mdm_crln_name_eng_lang from gms3_mdm_carline_codes where "
						+ " mdm_ml_lang_code='"+locale+"' AND (mdm_crln_year_start >= 2012 "
						+ " OR mdm_crln_year_end >= 2012) AND mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
				logger.info("getModelDetails() :: Get Active Master Carline Details Sql :: >"+ sql);
				pstmt = conn.prepareStatement(sql);
				rs=  pstmt.executeQuery();
				NMDScheduleDetails cd = null;
				List<NMDScheduleDetails> temp = new ArrayList<NMDScheduleDetails>();
				String carlineName=null;
				while(rs.next())
				{
					carlineName =rs.getString("mdm_crln_name_eng_lang");
					if(null!=carlineName && !"".equals(carlineName))
					{
						cd = new NMDScheduleDetails();
						cd.setModel(carlineName.trim());
						if(null==temp || temp.size()<=0)
						{
							temp = new ArrayList<NMDScheduleDetails>();
						}
						temp.add(cd);
						cd = null;
					}
				}
				sql  =null;
				cd = null;
				pstmt.close();pstmt=null;
				rs.close();rs=null;
				
				/*
				 * FIRST DELETE ROWS FROM GMS3_MDM_NMD_TOOL_SCH WHICH ARE NOT IN MODELS LIST IDENTIFIED ABOVE 
				 */
				if(null!=temp && temp.size()>0)
				{
					String keyToDel="";
					cd = null;
					for(int a=0;a<temp.size();a++)
					{
						cd = (NMDScheduleDetails)temp.get(a);
						if(null==keyToDel)
						{
							keyToDel="";
						}
						keyToDel+="'"+cd.getModel()+"'";
						if(a!=temp.size()-1)
						{
							keyToDel+=",";
						}
						cd = null;
					}
					cd = null;
					
					if(null!=keyToDel && !"".equals(keyToDel))
					{
						// delete all models not in keyToDel
						sql = "delete from gms3_mdm_nmd_tool_sch where nmd_model NOT IN ("+keyToDel+")";
						logger.info("getModelDetails :: Delete Additional Models Sql :: >" + sql);
						pstmt = conn.prepareStatement(sql);
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql=null;
					}
					else
					{
						// no active models found - delete all entries from GMS3_MDM_NMD_TOOL_SCH
						sql = "delete from gms3_mdm_nmd_tool_sch ";
						logger.info("getModelDetails :: Delete Additional Models Sql :: >" + sql);
						pstmt = conn.prepareStatement(sql);
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql=null;
					}
					keyToDel = null;
				}
				
				if(null!=temp && temp.size()>0)
				{
					/*
					 * FOR EACH MODEL ITERATE AND FETCH THE OTHER DETAILS FROM NMD SCHEDULE TABLE
					 */
					cd = null;
					Integer day=null;
					Integer hour=null;
					for(int a=0;a<temp.size();a++)
					{
						cd = (NMDScheduleDetails)temp.get(a);
						
						logger.info("getModelDetails :: Fetching Schedule Details for Model :: >"+ cd.getModel());
						sql  ="select nmd_sch_id,nmd_locale,nmd_sch_day_of_month,nmd_sch_time_of_month,nmd_sch_wsl_id,nmd_sch_last_exc_time,rec_creation_tmstp,"
								+ "rec_modified_tmspt from gms3_mdm_nmd_tool_sch where nmd_model = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, cd.getModel().trim());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							cd.setScheduleId(rs.getLong("nmd_sch_id"));
							cd.setLocale(rs.getString("nmd_locale"));
							if(rs.getInt("nmd_sch_day_of_month") > 0)
							{
								// set Day & Hour for Setting up for Models Not Found in GMS3_MDM_NMD_TOOL_SCH Table
								day = rs.getInt("nmd_sch_day_of_month");
								hour = rs.getInt("nmd_sch_time_of_month");
								cd.setDayOfMonth(String.valueOf(rs.getInt("nmd_sch_day_of_month")));
								// set hours only when Day is greater than 0
								int hours = rs.getInt("nmd_sch_time_of_month");
								if(hours<10)
								{
									cd.setTimeofMonth("0"+hours+":00");
								}
								else
								{
									cd.setTimeofMonth(hours+":00");
								}
								hours = 0;
							}
							cd.setWslId(rs.getString("nmd_sch_wsl_id"));
							cd.setLastExecutionTime(rs.getTimestamp("nmd_sch_last_exc_time"));
							cd.setCreatedTime(rs.getTimestamp("rec_creation_tmstp"));
							cd.setModifiedTime(rs.getTimestamp("rec_modified_tmspt"));
						}
						rs.close();rs=null;
						pstmt.close();pstmt=null;
						sql = null;
					
						
						// check if Locale is null - then set passed Locale
						if(null==cd.getLocale() || "".equals(cd.getLocale()))
						{
							cd.setLocale(locale);
						}
						
						/*
						 * CHECK HERE IF FOR MODEL SCHEDULE ID IS NULL
						 * 	THEN CREATE ENTRY SET HOURS AND DAY AS SAME AS OF OTHER EXISTING MODELS
						 */
						if(cd.getScheduleId()<=0)
						{
							logger.info("getModelDetails :: Schedule Entry Does Not Exist. Proceed for Creating.");

							sql  ="INSERT INTO gms3_mdm_nmd_tool_sch (nmd_locale) VALUES('"+cd.getLocale()+"')";
							stmt = conn.createStatement();
							String generatedColumns[] = { "nmd_sch_id" };
							stmt.execute(sql,generatedColumns);
							rs = stmt.getGeneratedKeys();
							if(rs.next())
							{
								cd.setScheduleId(rs.getLong(1));
							}
							rs.close();rs =null;
							stmt.close();stmt = null;
							sql=null;
							generatedColumns = null;
							
							if(cd.getScheduleId()>0)
							{
								// Update entry for Model
								sql = "UPDATE gms3_mdm_nmd_tool_sch SET nmd_model=?,nmd_sch_day_of_month=?,"
										+ "nmd_sch_time_of_month=?,nmd_sch_wsl_id=?,rec_creation_tmstp=? WHERE nmd_sch_id=?";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, cd.getModel());
								if(null!=day && day.intValue()>0)
								{
									pstmt.setInt(2, day.intValue());
								}
								else
								{
									pstmt.setNull(2, Types.INTEGER);
								}
								if(null!=hour && hour.intValue()>0)
								{
									pstmt.setInt(3, hour.intValue());
								}
								else
								{
									pstmt.setNull(3, Types.INTEGER);
								}
								pstmt.setString(4, loggedInUser);
								pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
								pstmt.setLong(6, cd.getScheduleId());
								pstmt.executeUpdate();
								pstmt.close();pstmt=null;
								sql =null;
								
								// set other values
								if(null!=day)
								{
									cd.setDayOfMonth(String.valueOf(day.intValue()));
								}
								if(null!=hour)
								{
									if(hour.intValue()<10)
									{
										cd.setTimeofMonth("0"+hour.intValue()+":00");
									}
									else
									{
										cd.setTimeofMonth(hour.intValue()+":00");
									}
								}
								cd.setWslId(loggedInUser);
							}
						}
						
						if(null==modelsList || modelsList.size()<=0)
						{
							modelsList = new ArrayList<NMDScheduleDetails>();
						}
						modelsList.add(cd);
						cd = null;
					}
					cd = null;
					day = null;
					hour = null;
				}
				else
				{
					logger.info("getModelDetails() :: No Models Found for Locale :: >"+ locale+" Year Greater Than 2012.");
				}
				temp= null;
				// commit transaction
				conn.commit();
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
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getModelDetails()", e1);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getModelDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getModelDetails()", e);
			}
			locale = null;
			loggedInUser = null;
		}
		return modelsList;
	}
	
	public static boolean createModelsSchedule(List<NMDScheduleDetails> list)
	{
		Connection conn = null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn  =getConnection();
				conn.setAutoCommit(false);
				String sql = null;
				/*
				 * ITERATE LIST AND CHECK IF SCH ID FOUND - THEN UPDATE ELSE CREATE
				 */
				NMDScheduleDetails cd = null;
				for(int a=0;a<list.size();a++)
				{
					cd = (NMDScheduleDetails) list.get(a);
					if(cd.getScheduleId()>0)
					{
						// PROCEED FOR UPDATE
						sql ="update gms3_mdm_nmd_tool_sch SET nmd_sch_day_of_month = ? ,nmd_sch_time_of_month = ?,nmd_sch_wsl_id = ? ,rec_modified_tmspt = ? WHERE nmd_sch_id = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, new Integer(cd.getDayOfMonth()).intValue());
						pstmt.setInt(2, new Integer(cd.getTimeofMonth()).intValue());
						pstmt.setString(3, cd.getWslId());
						pstmt.setTimestamp(4, new Timestamp(new Date().getTime()));
						pstmt.setLong(5, cd.getScheduleId());
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql  =null;
					}
					else
					{
						// PROCEED FOR CREATE
						sql  ="insert into gms3_mdm_nmd_tool_sch (nmd_locale,nmd_model,nmd_sch_day_of_month,nmd_sch_time_of_month,nmd_sch_wsl_id,rec_creation_tmstp) "
								+ "values(?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, cd.getLocale());
						pstmt.setString(2, cd.getModel());
						pstmt.setInt(3, new Integer(cd.getDayOfMonth()).intValue());
						pstmt.setInt(4, new Integer(cd.getTimeofMonth()).intValue());
						pstmt.setString(5, cd.getWslId());
						pstmt.setTimestamp(6, new Timestamp(new Date().getTime()));
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql  =null;
					}
					cd = null;
				}
				
				// COMMIT TRANSACTIONS
				conn.commit();
				cd  =null;
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
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createModelsSchedule()", eq);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createModelsSchedule()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createModelsSchedule()", e);
			}
			list = null;
		}
		return true;
	}

	public static boolean resetLastExecTime(List<String> list,String wslId)
	{
		Connection conn = null;
		PreparedStatement pstmt=null;
		boolean bool=false;
		try
		{
			if(null!=list && list.size()>0)
			{
				Timestamp ts = new Timestamp(new Date().getTime());
				conn  =getConnection();
				conn.setAutoCommit(false);
				String sql = "update gms3_mdm_nmd_tool_sch set nmd_sch_last_exc_time=null, rec_modified_tmspt=?, nmd_sch_wsl_id=? where nmd_sch_id =? ";
				pstmt =conn.prepareStatement(sql);
				for(int a=0;a<list.size();a++)
				{
					pstmt.setTimestamp(1, ts);
					pstmt.setString(2, wslId);
					pstmt.setLong(3, new Long(list.get(a).toString()).longValue());
					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.close();pstmt=null;
				sql = null;
				ts = null;
				// COMMIT TRANSACTIONS
				conn.commit();
				bool = true;
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
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "resetLastExecTime()", eq);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "resetLastExecTime()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "resetLastExecTime()", e);
			}
			list = null;
			wslId=null;
		}
		return bool;
	}

	public static boolean updateLastExecTime(List<String> list, Timestamp updateTime, String wslId)
	{
		Connection conn = null;
		PreparedStatement pstmt=null;
		boolean bool=false;
		try
		{
			if(null!=list && list.size()>0)
			{
				Timestamp ts = new Timestamp(new Date().getTime());
				conn  =getConnection();
				conn.setAutoCommit(false);
				String sql = "update gms3_mdm_nmd_tool_sch set nmd_sch_last_exc_time=?, rec_modified_tmspt=?,nmd_sch_wsl_id=? where nmd_sch_id =? ";
				pstmt =conn.prepareStatement(sql);
				for(int a=0;a<list.size();a++)
				{
					pstmt.setTimestamp(1, updateTime);
					pstmt.setTimestamp(2, ts);
					pstmt.setString(3, wslId);
					pstmt.setLong(4, new Long(list.get(a).toString()).longValue());
					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.close();pstmt=null;
				sql = null;
				ts = null;
				// COMMIT TRANSACTIONS
				conn.commit();
				bool = true;
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
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "updateLastExecTime()", eq);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "updateLastExecTime()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "updateLastExecTime()", e);
			}
			list = null;
			updateTime = null;
			wslId  =null;
		}
		return bool;
	}

	
	public static boolean createOnDemandSchedule(List<String> list)
	{
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs =null;
		boolean bool=false;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn  =getConnection();
				conn.setAutoCommit(false);
				String sql = null;
				/*
				 * ITERATE LIST AND CHECK IF SCH ID FOUND - THEN UPDATE ELSE CREATE
				 */
				String schId=null;
				Long existId=null;
				for(int a=0;a<list.size();a++)
				{
					schId = list.get(a).toString();
					// check if SCH ID ALREADY EXISTS - DO NOT RECREATE
					sql  ="SELECT nmd_rod_id FROM gms3_mdm_nmd_job_rod WHERE nmd_sch_id="+schId;
					logger.info("createOnDemandSchedule :: Check Schedule Sql :: >"+ sql);
					pstmt=conn.prepareStatement(sql);
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						if(rs.getLong("nmd_rod_id") > 0)
						{
							existId = rs.getLong("nmd_rod_id");
						}
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
					sql=null;
					
					if(null!=existId && existId.longValue()>0)
					{
						logger.info("createOnDemandSchedule :: On Demand Schedule Already Exists for Schedule id :: >"+ schId+". Skip Re-creating.");
					}
					else
					{
						logger.info("createOnDemandSchedule :: On Demand Schedule Does Not Exists for Schedule id :: >"+ schId+". Proceed for Re-creating.");
						// insert
						sql  ="INSERT INTO gms3_mdm_nmd_job_rod (nmd_sch_id,rec_creation_tmstp) VALUES(?,?)";
						pstmt=conn.prepareStatement(sql);
						pstmt.setLong(1, new Long(schId).longValue());
						pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql = null;
					}
					schId = null;
				}
				
				// COMMIT TRANSACTIONS
				conn.commit();
				bool = true;
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
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createOnDemandSchedule()", eq);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createOnDemandSchedule()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "createOnDemandSchedule()", e);
			}
			list = null;
		}
		return bool;
	}

	
	public static List<NMDJobDetails> getJobsList(String fromDate, String toDate)
	{
		List<NMDJobDetails> jobsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql="select a.*,b.nmd_atmpt_id from gms3_mdm_nmd_job_sch a, gms3_mdm_nmd_job_atmpt b where "
					+ " a.nmd_job_id=b.nmd_job_id and b.nmd_atmpt_id = (select c.nmd_atmpt_id from gms3_mdm_nmd_job_atmpt c "
					+ " where c.nmd_job_id=a.nmd_job_id order by nmd_atmpt_count desc LIMIT 1 ) ";
				
			if(null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate))
			{
				sql = sql+" AND DATE_FORMAT(a.nmd_job_start_time,'%Y-%m-%d') BETWEEN '"+fromDate+"' AND '"+toDate+"' ";
			}
			sql+= " ORDER BY a.nmd_job_id DESC";
			logger.info("getJobsList :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			NMDJobDetails jd = null;
			int count=0;
			while(rs.next())
			{
				jd = new NMDJobDetails();
				// increment count by 1
				count++;
				jd.setSrNo(count);
				jd.setJobId(rs.getLong("nmd_job_id"));
				jd.setJobName(rs.getString("nmd_job_name"));
				jd.setLocale(rs.getString("nmd_locale"));
				jd.setModel(rs.getString("nmd_model"));
				jd.setStartTime(rs.getTimestamp("nmd_job_start_time"));
				jd.setEndTime(rs.getTimestamp("nmd_job_finish_time"));
				jd.setRunningStatus(rs.getString("nmd_job_execution_status"));
				jd.setJobStatus(rs.getString("nmd_job_status"));
				jd.setThreadId(rs.getString("nmd_job_thread_id"));
				jd.setReportsPath(rs.getString("nmd_job_reports_path"));
				jd.setTotalDocs(rs.getLong("nmd_job_total_docs"));
				jd.setSuccessDoc(rs.getLong("nmd_job_success_docs"));
				jd.setFailureDocs(rs.getLong("nmd_job_failure_docs"));
				jd.setTotalInnerLinks(rs.getLong("nmd_job_inlk_total"));
				jd.setSuccessInnerLinks(rs.getLong("nmd_job_inlk_success"));
				jd.setFailureInnnerLinks(rs.getLong("nmd_job_inlk_failure"));
				jd.setTotalOkAssets(rs.getLong("nmd_job_okassets_total"));
				jd.setSuccessOkAssets(rs.getLong("nmd_job_okassets_success"));
				jd.setFailureOkAssets(rs.getLong("nmd_job_okassets_failure"));
				jd.setAttemptsCount(rs.getInt("nmd_job_attempt_count"));
				jd.setCreatedTime(rs.getTimestamp("rec_creation_tmstp"));
				jd.setModifiedTime(rs.getTimestamp("rec_modified_tmspt"));
				
				jd.setTotalDelDocs(rs.getLong("nmd_job_total_del_docs"));
				jd.setSuccessDelDoc(rs.getLong("nmd_job_success_del_docs"));
				jd.setFailureDelDocs(rs.getLong("nmd_job_failure_del_docs"));
				jd.setFailureReason(rs.getString("nmd_job_failure_reason"));
				
				jd.setTotalTransferDocs(rs.getLong("nmd_job_transfer_total"));
				jd.setSuccessTransferDoc(rs.getLong("nmd_job_transfer_success"));
				jd.setFailureTransferDocs(rs.getLong("nmd_job_transfer_failure"));
				jd.setSkippedTranferDocs(rs.getLong("nmd_job_transfer_skipped"));
				
				jd.setAttemptId(rs.getLong("nmd_atmpt_id"));
				
				if(null==jobsList || jobsList.size()<=0)
				{
					jobsList = new ArrayList<NMDJobDetails>();
				}
				jobsList.add(jd);
				jd = null;
			}
			sql  = null;
			jd = null;
			
			if(null!=jobsList && jobsList.size()>0)
			{
				logger.info("getAttemptsList() :: Total Jobs records found are :: >"+ jobsList.size());
			}
			else
			{
				logger.info("getAttemptsList() :: No Jobs record found.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getJobsList()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getJobsList()", e);
			}
			fromDate  =null;
			toDate  =null;
		}
				
		return jobsList;
	}

	public static List<NMDJobDetails> getAttemptsList(String jobId)
	{
		List<NMDJobDetails> attemptsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		try
		{
			if(null!=jobId && !"".equals(jobId))
			{

				conn = getConnection();
				String sql="select * from gms3_mdm_nmd_job_atmpt where nmd_job_id="+jobId+" ORDER BY nmd_atmpt_id DESC";
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				NMDJobDetails jd = null;
				int count=0;
				while(rs.next())
				{
					jd = new NMDJobDetails();
					// increment count by 1
					count++;
					jd.setSrNo(count);
					jd.setAttemptId(rs.getLong("nmd_atmpt_id"));
					
					jd.setJobId(rs.getLong("nmd_job_id"));
					jd.setLocale(rs.getString("nmd_locale"));
					jd.setModel(rs.getString("nmd_model"));
					jd.setStartTime(rs.getTimestamp("nmd_atmpt_start_time"));
					jd.setEndTime(rs.getTimestamp("nmd_atmpt_finish_time"));
					jd.setRunningStatus(rs.getString("nmd_atmpt_execution_status"));
					jd.setJobStatus(rs.getString("nmd_atmpt_status"));
					jd.setThreadId(rs.getString("nmd_atmpt_thread_id"));
					jd.setReportsPath(rs.getString("nmd_atmpt_reports_path"));
					jd.setTotalDocs(rs.getLong("nmd_atmpt_total_docs"));
					jd.setSuccessDoc(rs.getLong("nmd_atmpt_success_docs"));
					jd.setFailureDocs(rs.getLong("nmd_atmpt_failure_docs"));
					jd.setTotalInnerLinks(rs.getLong("nmd_atmpt_inlk_total"));
					jd.setSuccessInnerLinks(rs.getLong("nmd_atmpt_inlk_success"));
					jd.setFailureInnnerLinks(rs.getLong("nmd_atmpt_inlk_failure"));
					jd.setTotalOkAssets(rs.getLong("nmd_atmpt_okassets_total"));
					jd.setSuccessOkAssets(rs.getLong("nmd_atmpt_okassets_success"));
					jd.setFailureOkAssets(rs.getLong("nmd_atmpt_okassets_failure"));
					jd.setAttemptsCount(rs.getInt("nmd_atmpt_count"));
					jd.setCreatedTime(rs.getTimestamp("rec_creation_tmstp"));
					jd.setModifiedTime(rs.getTimestamp("rec_modified_tmspt"));
					
					jd.setTotalDelDocs(rs.getLong("nmd_atmpt_total_del_docs"));
					jd.setSuccessDelDoc(rs.getLong("nmd_atmpt_success_del_docs"));
					jd.setFailureDelDocs(rs.getLong("nmd_atmpt_failure_del_docs"));
					jd.setFailureReason(rs.getString("nmd_atmpt_failure_reason"));
					
					jd.setTotalTransferDocs(rs.getLong("nmd_atmpt_transfer_total"));
					jd.setSuccessTransferDoc(rs.getLong("nmd_atmpt_transfer_success"));
					jd.setFailureTransferDocs(rs.getLong("nmd_atmpt_transfer_failure"));
					jd.setSkippedTranferDocs(rs.getLong("nmd_atmpt_transfer_skipped"));
					
					
					if(null==attemptsList || attemptsList.size()<=0)
					{
						attemptsList = new ArrayList<NMDJobDetails>();
					}
					attemptsList.add(jd);
					jd = null;
				}
				sql  = null;
				jd = null;
				if(null!=attemptsList && attemptsList.size()>0)
				{
					logger.info("getAttemptsList() :: Aattempts found for Job Id : >"+ jobId+" >> are :: >"+ attemptsList.size());
				}
				else
				{
					logger.info("getAttemptsList() :: No attempts record found for Job Id :: >"+ jobId);
				}
			}
			else
			{
				logger.info("getAttemptsList() :: Job id as Parameter is Null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getAttemptsList()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getAttemptsList()", e);
			}
			jobId = null;
		}
		return attemptsList;
	}

	public static ArrayList<DataExportToolReportSummaryDetails> getJobSummaryDetails(String scheduleId,String attempId)
	{
		ArrayList<DataExportToolReportSummaryDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_nmd_job_summary WHERE nmd_job_id = "+ scheduleId;
			if(null!=attempId && !"".equals(attempId) && !"0".equals(attempId))
			{
				sql+=" and nmd_attempt_id = "+attempId;
			}
			else
			{
				sql+=" ORDER BY nmd_attempt_id DESC LIMIT 1";
			}
			logger.info("getJobSummaryDetails() :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			DataExportToolReportSummaryDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new DataExportToolReportSummaryDetails();
				schDetails.setScheduleId(String.valueOf(rs.getLong("nmd_job_id")));
				schDetails.setAttempId(String.valueOf(rs.getLong("nmd_attempt_id")));
				schDetails.setReportName(rs.getString("nmd_job_report_name"));
				schDetails.setReportStatus(rs.getString("nmd_job_report_status"));
				
				schDetails.setTotalCount(rs.getLong("nmd_job_total_count"));
				schDetails.setSuccessCount(rs.getLong("nmd_job_success_count"));
				schDetails.setFailureCount(rs.getLong("nmd_job_failure_count"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<DataExportToolReportSummaryDetails>();
				}
				schDetails.setSrNo(list.size()+1);
				list.add(schDetails);
				schDetails  =null;
			}
			sql  =null;
			schDetails= null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getJobSummaryDetails()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getJobSummaryDetails()", eq);
			}
			scheduleId = null;
			attempId = null;
		}
		return list;
	}
	
	public static String getReportsPath(String scheduleId,String attempId)
	{
		String reportsPath=null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="";
			if(null!=attempId && !"".equals(attempId) && !"0".equals(attempId))
			{
				sql="SELECT nmd_atmpt_reports_path as report_path FROM gms3_mdm_nmd_job_atmpt "
						+ " WHERE nmd_job_id = "+ scheduleId +" AND nmd_atmpt_id="+attempId;
			}
			else
			{
				sql="SELECT nmd_job_reports_path as report_path FROM gms3_mdm_nmd_job_sch WHERE nmd_job_id = "+ scheduleId;
			}
			
			logger.info("getReportsPath() :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				if(null!=rs.getString("report_path") && !"".equals(rs.getString("report_path")))
				{
					reportsPath =rs.getString("report_path");
				}
			}
			sql  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getReportsPath()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getReportsPath()", eq);
			}
			scheduleId = null;
			attempId = null;
		}
		return reportsPath;
				
	}
	
	public static List<NMDScheduleDetails> getRunningScheduleList(String scheduleIds)
	{
		List<NMDScheduleDetails> modelsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		try
		{
			conn =getConnection();
			// get In Progress Jobs
			String sql=null;
			if(null!=scheduleIds && !"".equals(scheduleIds))
			{
				sql="select distinct a.nmd_model from gms3_mdm_nmd_job_sch a , gms3_mdm_nmd_tool_sch b where a.nmd_model=b.nmd_model and " + 
						"nmd_job_execution_status='In Progress' and nmd_job_name like '%_EXT_%' and b.nmd_sch_id in ("+scheduleIds+")";
			}
			else
			{
				sql="select nmd_job_id from gms3_mdm_nmd_job_sch where nmd_job_execution_status='In Progress' and nmd_job_name like '%_EXT_%'";
			}
			logger.info("getRunningScheduleList() :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs=  pstmt.executeQuery();
			NMDScheduleDetails cd = null;
			while(rs.next())
			{
				if(null!=scheduleIds && !"".equals(scheduleIds))
				{
					if(null!=rs.getString("nmd_model") && !"".equals(rs.getString("nmd_model")))
					{
						cd = new NMDScheduleDetails();
						cd.setModel(rs.getString("nmd_model").trim());
						if(null==modelsList || modelsList.size()<=0)
						{
							modelsList = new ArrayList<NMDScheduleDetails>();
						}
						modelsList.add(cd);
						cd = null;
					}
				}
				else
				{
					if(rs.getLong("nmd_job_id")>0)
					{
						cd = new NMDScheduleDetails();
						cd.setScheduleId(rs.getLong("nmd_job_id"));
						if(null==modelsList || modelsList.size()<=0)
						{
							modelsList = new ArrayList<NMDScheduleDetails>();
						}
						modelsList.add(cd);
						cd = null;
					}
				}
			}
			sql  =null;
			cd = null;
			pstmt.close();pstmt=null;
			rs.close();rs=null;
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
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getRunningScheduleList()", e1);
			}
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getRunningScheduleList()", e);
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getRunningScheduleList()", e);
			}
			scheduleIds = null;
		}
		return modelsList;
	}
	
	public static NMDScheduleDetails getSystemErrorDetails(String scheduleId,String attempId)
	{
		NMDScheduleDetails details = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="";
			if(null!=attempId && !"".equals(attempId) && !"0".equals(attempId))
			{
				sql="SELECT nmd_system_error_comment, nmd_system_error_details  FROM gms3_mdm_nmd_job_atmpt "
						+ " WHERE nmd_job_id = "+ scheduleId +" AND nmd_atmpt_id="+attempId;
			}
			else
			{
				sql="SELECT nmd_system_error_comment, nmd_system_error_details FROM gms3_mdm_nmd_job_sch WHERE nmd_job_id = "+ scheduleId;
			}
			
			logger.info("getSystemErrorDetails() :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				details=  new NMDScheduleDetails();
				details.setSystemErrorComments(rs.getString("nmd_system_error_comment"));
				if(null!=rs.getBytes("nmd_system_error_details"))
				{
					details.setSystemErrorMessage(new String(rs.getBytes("nmd_system_error_details")));
				}
			}
			sql  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getSystemErrorDetails()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getSystemErrorDetails()", eq);
			}
			scheduleId = null;
			attempId = null;
		}
		return details;
				
	}
	
	public static NMDJobDetails getTransferSummaryDetails(String scheduleId,String attempId)
	{
		NMDJobDetails details = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="";
			if(null!=attempId && !"".equals(attempId) && !"0".equals(attempId))
			{
				sql="SELECT nmd_delta_transfer_total, nmd_delta_transfer_success,nmd_delta_transfer_failure,nmd_delta_transfer_skipped,nmd_full_transfer_total,"
						+ "nmd_full_transfer_success,nmd_full_transfer_failure,nmd_full_transfer_skipped,nmd_delta_trans_del_total,nmd_delta_trans_del_success,"
						+ "nmd_delta_trans_del_failure,nmd_full_trans_del_total,nmd_full_trans_del_success,nmd_full_trans_del_failure "
						+ "  FROM gms3_mdm_nmd_job_atmpt "
						+ " WHERE nmd_job_id = "+ scheduleId +" AND nmd_atmpt_id="+attempId;
			}
			else
			{
				sql="SELECT nmd_delta_transfer_total, nmd_delta_transfer_success,nmd_delta_transfer_failure,nmd_delta_transfer_skipped,nmd_full_transfer_total," + 
					 "nmd_full_transfer_success,nmd_full_transfer_failure,nmd_full_transfer_skipped,nmd_delta_trans_del_total,nmd_delta_trans_del_success,"
					 + "nmd_delta_trans_del_failure,nmd_full_trans_del_total,nmd_full_trans_del_success,nmd_full_trans_del_failure "
					 + " FROM gms3_mdm_nmd_job_sch WHERE nmd_job_id = "+ scheduleId;
			}
			
			logger.info("getTransferSummaryDetails() :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				details=  new NMDJobDetails();
				
				details.setDeltaTotalCount(rs.getInt("nmd_delta_transfer_total"));
				details.setDeltaSuccessCount(rs.getInt("nmd_delta_transfer_success"));
				details.setDeltaFailureCount(rs.getInt("nmd_delta_transfer_failure"));
				details.setDeltaSkippedCount(rs.getInt("nmd_delta_transfer_skipped"));
				
				details.setFullTotalCount(rs.getInt("nmd_full_transfer_total"));
				details.setFullSuccessCount(rs.getInt("nmd_full_transfer_success"));
				details.setFullFailureCount(rs.getInt("nmd_full_transfer_failure"));
				details.setFullSkippedCount(rs.getInt("nmd_full_transfer_skipped"));
				
				details.setDeltaDeleteTotalCount(rs.getInt("nmd_delta_trans_del_total"));
				details.setDeltaDeleteSuccessCount(rs.getInt("nmd_delta_trans_del_success"));
				details.setDeltaDeleteFailureCount(rs.getInt("nmd_delta_trans_del_failure"));
				
				details.setFullDeleteTotalCount(rs.getInt("nmd_full_trans_del_total"));
				details.setFullDeleteSuccessCount(rs.getInt("nmd_full_trans_del_success"));
				details.setFullDeleteFailureCount(rs.getInt("nmd_full_trans_del_failure"));
			}
			sql  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getTransferSummaryDetails()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(NMDScheduleDAO.class.getName(), "getTransferSummaryDetails()", eq);
			}
			scheduleId = null;
			attempId = null;
		}
		return details;
	}
}