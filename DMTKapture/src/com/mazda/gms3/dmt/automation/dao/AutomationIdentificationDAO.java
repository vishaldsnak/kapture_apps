package com.mazda.gms3.dmt.automation.dao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;

public class AutomationIdentificationDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(AutomationDAO.class);
	
	final static String REC_TYPE_APPLICABLE_VIN="APP_VIN";
	final static String REC_TYPE_IMPACTED_DOCUMENTS="IMP_DOC";
			
	
	/*
	 * CALL FUNCTION TO UPDATE SCHEDULE STATUS AS FAILURE
	 */
	public static boolean updateScheduleStatus(String scheduleId, String processingStatus,String jobStatus, boolean deleteChildEntries, String remarks) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId) && null!=processingStatus && !"".equals(processingStatus))
			{
				byte[] data = null;
				if(null==remarks)
				{
					remarks ="";
				}
				data = remarks.getBytes();
				
				String sql="UPDATE gms3_dmt_fm_sch SET FM_PROCESSING_STATUS =?,FM_REMARKS = ?  ";
				if(null!=jobStatus && !"".equals(jobStatus))
				{
					sql+=" ,  FM_JOB_STATUS = ?, FM_FINISH_TIME =? ";
				}
				sql+=" WHERE FM_SCH_ID = "+scheduleId;
				conn = getConnection();
				conn.setAutoCommit(false);
				logger.info("updateScheduleStatus :: Sql :: >"+ sql);
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, processingStatus);
				InputStream is = new ByteArrayInputStream(data);
				pstmt.setBinaryStream(2, is, data.length);
				if(null!=jobStatus && !"".equals(jobStatus))
				{
					pstmt.setString(3, jobStatus);
					pstmt.setTimestamp(4, new Timestamp(new Date().getTime()));
				}
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
				is.close();is=null;
				data = null;
				remarks  =null;
				
				if(deleteChildEntries == true)
				{
					sql="DELETE FROM gms3_dmt_fm_sch_itm WHERE FM_SCH_ID = "+scheduleId;
					pstmt = conn.prepareStatement(sql);
					pstmt.executeUpdate();
					pstmt.close();pstmt=null;
					sql = null;
					
					sql="DELETE FROM gms3_dmt_fm_itm_ap_vin WHERE FM_SCH_ID = "+scheduleId;
					pstmt = conn.prepareStatement(sql);
					pstmt.executeUpdate();
					pstmt.close();pstmt=null;
					sql = null;
				}
				// commit transaction
				conn.commit();
				
				logger.info("updateScheduleStatus :: Schedule / Job Status Completed Successfully.");
			}
			else
			{
				logger.info("updateScheduleStatus :: Schedule Id / Processing Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e1);
			}
			return false;
		}
		finally
		{
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
				}
			if(null!=pstmt)
				try {
					pstmt.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleStatus()", e);
				}
			// set params to null
			scheduleId = null;
			processingStatus = null;
			jobStatus = null;
		}
		return true;
	}

	public static boolean updateScheduleItemDetails(String scheduleId, int totalDocsCount,List<ItemDetails> itemsList, String scheduleStatus,String jobStatus) 
	{
		boolean bool = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId) && null!=itemsList && itemsList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				// FIRST UPDATE SCHEDULE DETAILS - STATUS & DOCS COUNT
				String sql="UPDATE gms3_dmt_fm_sch SET FM_PROCESSING_STATUS=?, FM_JOB_STATUS =?, FM_FINISH_TIME =?,FM_TOTAL_DOCS_COUNT = ? "
						+ " WHERE FM_SCH_ID ="+ scheduleId;
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, scheduleStatus);
				pstmt.setString(2, jobStatus);
				pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
				if(totalDocsCount> 0)
				{
					pstmt.setLong(4, totalDocsCount);
				}
				else
				{
					pstmt.setNull(4, Types.BIGINT);
				}
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
				
				// PROCEED FOR INSERTING CHILD ITEM DETAILS
				if(null!=itemsList && itemsList.size()>0)
				{
					ItemDetails details = null;
					for(int a=0;a<itemsList.size();a++)
					{
						details = (ItemDetails)itemsList.get(a);
						// Proceed for Inserting each ItemDetails
						
						Long itemId=null;
						sql="INSERT INTO gms3_dmt_fm_sch_itm (FM_SCH_ID) VALUES ("+scheduleId+")";
						stmt = conn.createStatement();
						String generatedColumns[] = { "FM_ITM_ID" };
						stmt.execute(sql,generatedColumns);
						rs = stmt.getGeneratedKeys();
						if(rs.next())
						{
							itemId = rs.getLong(1);
							details.setItemId(itemId);
						}
						rs.close();rs =null;
						stmt.close();stmt = null;
						sql=null;
						itemId  =null;
						details = null;
					}
					details = null;
					
					
					List<ExcelRowDetails> applicableVINList = null;
					ExcelRowDetails exDetails = null;
					List<ExcelRowDetails> tempImpactedDocsList = null;
					Map<String, Object> docMap = null;
					ExcelRowDetails docVinDetails = null;
					/*
					 *  NOW PROCEED FOR BATCH UPDATES
					 * PROCEED FOR UPDATING DETAILS IN CHILD TABLE - gms3_dmt_fm_sch_itm
					 */
					sql = "UPDATE gms3_dmt_fm_sch_itm SET FM_LOCALE=?,FM_CARLINE_INFO=?,FM_CH_LABEL=?,FM_CH_REFKEY=?,FM_DISP_CARLINE_INFO=?,FM_DC_TYPE_LABEL=?,"
							+ "FM_DC_TYPE_REFKEY = ?,FM_DOCS_COUNT = ?,REC_CREATION_TMSTP=?,REC_MODIFIED_TMSTP=?,FM_APP_VIN_COUNT=? WHERE FM_ITM_ID=?";
					pstmt = conn.prepareStatement(sql);
					
					for(int a=0;a<itemsList.size();a++)
					{
						details = (ItemDetails)itemsList.get(a);
						pstmt.setString(1, details.getLocale());
						pstmt.setString(2, details.getCarlineInfo());
						pstmt.setString(3, details.getChannelLabel());
						pstmt.setString(4, details.getChannelRefKey());
						pstmt.setString(5, details.getDisplayCarlineInfo());
						pstmt.setString(6, details.getDocumentTypeLabel());
						pstmt.setString(7, details.getDocumentTypeRefKey());
						if(null!=details.getTotalDocumentsCount() && details.getTotalDocumentsCount()>0)
						{
							pstmt.setLong(8, details.getTotalDocumentsCount().longValue());
						}
						else
						{
							pstmt.setNull(8, Types.BIGINT);
						}
						pstmt.setTimestamp(9, new Timestamp(new Date().getTime()));
						pstmt.setTimestamp(10, new Timestamp(new Date().getTime()));
						if(null!=details.getApplicableVINList() && details.getApplicableVINList().size()>0)
						{
							pstmt.setLong(11, details.getApplicableVINList().size());
						}
						else
						{
							pstmt.setNull(11, Types.BIGINT);
						}
						pstmt.setLong(12, details.getItemId());
						pstmt.addBatch();
						
						if(null!=details.getApplicableVINList() && details.getApplicableVINList().size()>0)
						{
							/*
							 * iterate applicableVINList and set scheuleId & itemId
							 */
							exDetails = null;
							for(int b=0;b<details.getApplicableVINList().size();b++)
							{
								exDetails  =(ExcelRowDetails)details.getApplicableVINList().get(b);
//								exDetails.setScheduleId(new Long(scheduleId).longValue());
//								exDetails.setItemId(new Long(details.getItemId()).longValue());
//								// set recordType as APP VIN
//								exDetails.setRecordType(REC_TYPE_APPLICABLE_VIN);
								
								// add to applicableVINsList
								if(null==applicableVINList || applicableVINList.size()<=0)
								{
									applicableVINList = new ArrayList<ExcelRowDetails>();
								}
//								applicableVINList.add(exDetails);
								applicableVINList.add(prepareAppVINData(exDetails, scheduleId, details.getItemId(), REC_TYPE_APPLICABLE_VIN));
								// set exDetails = null;
								exDetails  =null;
							}
						}
						/*
						 * Now check for Impacted DocumentsList 
						 * if available - then addAllTo Local List
						 */
						if(null!=details.getImpactedDocumentsList() && details.getImpactedDocumentsList().size()>0)
						{
							docMap = null;
							tempImpactedDocsList = null;
							for(int ct=0;ct<details.getImpactedDocumentsList().size();ct++)
							{
								docMap  = (Map<String, Object>)details.getImpactedDocumentsList().get(ct);
								if(null!=docMap.get(AutomationConstants.VIN_LIST))
								{
									tempImpactedDocsList = (ArrayList<ExcelRowDetails>)docMap.get(AutomationConstants.VIN_LIST);
									if(null!=tempImpactedDocsList && tempImpactedDocsList.size()>0)
									{
										docVinDetails = null;
										for(int d=0;d<tempImpactedDocsList.size();d++)
										{
											docVinDetails = (ExcelRowDetails)tempImpactedDocsList.get(d);
											// add scheduleId & ItemId
//											docVinDetails.setScheduleId(new Long(scheduleId).longValue());
//											docVinDetails.setItemId(new Long(details.getItemId()).longValue());
											
											// add Doucment Id / Ref Key and Record Type as DOC_IMP
											if(null!=docMap.get(AutomationConstants.DOCUMENTID))
											{
												docVinDetails.setDocumentId(docMap.get(AutomationConstants.DOCUMENTID).toString());
											}
											if(null!=docMap.get(AutomationConstants.REFERENCEKEY))
											{
												docVinDetails.setDocTypeRefKey(docMap.get(AutomationConstants.REFERENCEKEY).toString());
											}
											// SET RECORD TYPE AS IMPACTED DOCUMENT
//											docVinDetails.setRecordType(REC_TYPE_IMPACTED_DOCUMENTS);
											
											if(null!=docVinDetails.getDocumentId() && !"".equals(docVinDetails.getDocumentId()))
											{
												// add this to applicableVINsList
												if(null==applicableVINList || applicableVINList.size()<=0)
												{
													applicableVINList = new ArrayList<ExcelRowDetails>();
												}
//												applicableVINList.add(docVinDetails);
												applicableVINList.add(prepareAppVINData(docVinDetails, scheduleId, details.getItemId(), REC_TYPE_IMPACTED_DOCUMENTS));
											}
											docVinDetails = null;
										}
										docVinDetails = null;
									}
									tempImpactedDocsList = null;
								}
								docMap= null;
							}
						}
						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					details = null;
					
					// PROCEED FOR ENTERING APPLICABLE VIN LIST FOR EACH SCHEDULE AND ITS ITEMS
					if(null!=applicableVINList && applicableVINList.size()>0)
					{
						/*
						 * DIVIDE INTO PARITION OF 100 AND SAVE THEM IN DATABASE
						 */
						int partitionSize=100;
						List<List<ExcelRowDetails>> partitions = new ArrayList<List<ExcelRowDetails>>();
						for (int i=0; i<applicableVINList.size(); i += partitionSize) {
							partitions.add(applicableVINList.subList(i, Math.min(i + partitionSize, applicableVINList.size())));
						}
						
						if(null!=partitions && partitions.size()>0)
						{
							
							ExcelRowDetails chDetails = null;
							for(List<ExcelRowDetails> subList: partitions)
							{
								if(null!=subList && subList.size()>0)
								{
									chDetails = null;
									sql="INSERT INTO gms3_dmt_fm_itm_ap_vin (FM_ITM_ID,FM_SCH_ID,FM_N_CARLINE_CODE,FM_N_WMI,FM_N_MODEL_NAME,FM_N_MODEL_NAME_REG_MC,FM_N_MODEL_NAME_REG_MNAOMME,"
											+ "FM_N_VDSCODE,FM_N_VISSTART,FM_N_VISEND,FM_N_REFKEY,FM_O_CARLINE_CODE,FM_O_WMI,FM_O_MODEL_NAME,FM_O_MODEL_NAME_REG_MC,FM_O_MODEL_NAME_REG_MNAOMME,FM_O_VDSCODE,"
											+ "FM_O_VISSTART,FM_O_VISEND,FM_O_REFKEY,FM_LOCALE_EXIST_MDM,FM_LOCALE_EXIST_IM,FM_N_VIN_EXIST_MDM,FM_N_VIN_EXIST_IM,FM_O_VIN_EXIST_MDM,FM_O_VIN_EXIST_IM,"
											+ "REC_CREATION_TMSTP,REC_MODIFIED_TMSTP,FM_LOCALE,FM_RECORD_TYPE,FM_DOCUMENT_ID,FM_DOC_TYPE_REFKEY) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
									pstmt = conn.prepareStatement(sql);
									for(int cw=0;cw<subList.size();cw++)
									{
										chDetails = (ExcelRowDetails) subList.get(cw);
										
										if(null!=chDetails.getItemId())
										{
											pstmt.setLong(1, chDetails.getItemId());
										}
										else
										{
											pstmt.setNull(1, Types.BIGINT);
										}
										if(null!=chDetails.getScheduleId())
										{
											pstmt.setLong(2, chDetails.getScheduleId());
										}
										else
										{
											pstmt.setNull(2, Types.BIGINT);
										}
										pstmt.setString(3, chDetails.getNewCarlineCode());
										pstmt.setString(4, chDetails.getNewWmiCode());
										pstmt.setString(5, chDetails.getNewModelName());
										pstmt.setString(6, chDetails.getNewModelNameEngForMC());
										pstmt.setString(7, chDetails.getNewModelNameRegForMNAOMME());
										pstmt.setString(8, chDetails.getNewVdsCode());
										pstmt.setString(9, chDetails.getNewVisStartRange());
										pstmt.setString(10, chDetails.getNewVisEndRange());
										pstmt.setString(11, chDetails.getNewRefKey());
										pstmt.setString(12, chDetails.getOldCarlineCode());
										pstmt.setString(13, chDetails.getOldWmiCode());
										pstmt.setString(14, chDetails.getOldModelName());
										pstmt.setString(15, chDetails.getOldModelNameEngForMC());
										pstmt.setString(16, chDetails.getOldModelNameRegForMNAOMME());
										pstmt.setString(17, chDetails.getOldVdsCode());
										pstmt.setString(18, chDetails.getOldVisStartRange());
										pstmt.setString(19, chDetails.getOldVisEndRange());
										pstmt.setString(20, chDetails.getOldRefKey());
										if(chDetails.isLocaleExistsInMDM()==true)
										{
											pstmt.setString(21, "Y");
										}
										else
										{
											pstmt.setString(21, "N");
										}
										if(chDetails.isLocaleExistsInIM()==true)
										{
											pstmt.setString(22, "Y");
										}
										else
										{
											pstmt.setString(22, "N");
										}
										if(chDetails.isNewVinExistsinMDM()==true)
										{
											pstmt.setString(23, "Y");
										}
										else
										{
											pstmt.setString(23, "N");
										}
										if(chDetails.isNewVinExistsinIM()==true)
										{
											pstmt.setString(24, "Y");
										}
										else
										{
											pstmt.setString(24, "N");
										}
										if(chDetails.isOldVinExistsinMDM()==true)
										{
											pstmt.setString(25, "Y");
										}
										else
										{
											pstmt.setString(25, "N");
										}
										if(chDetails.isOldVinExistsinIM()==true)
										{
											pstmt.setString(26, "Y");
										}
										else
										{
											pstmt.setString(26, "N");
										}
										pstmt.setTimestamp(27, new Timestamp(new Date().getTime()));
										pstmt.setTimestamp(28, new Timestamp(new Date().getTime()));
										pstmt.setString(29, chDetails.getLocale());
										pstmt.setString(30, chDetails.getRecordType());
										if(null!=chDetails.getDocumentId() && !"".equals(chDetails.getDocumentId()))
										{
											pstmt.setString(31, chDetails.getDocumentId());
										}
										else
										{
											pstmt.setNull(31, Types.VARCHAR);
										}
										if(null!=chDetails.getDocTypeRefKey() && !"".equals(chDetails.getDocTypeRefKey()))
										{
											pstmt.setString(32, chDetails.getDocTypeRefKey());
										}
										else
										{
											pstmt.setNull(32, Types.VARCHAR);
										}
										pstmt.addBatch();
										
										sql = null;
										chDetails = null;
									}
									pstmt.executeBatch();
									pstmt.close();pstmt=null;
									sql  =null;
									chDetails=  null;
								}
								subList = null;
							}
						}
						partitions  =null;
					}
					applicableVINList = null;
				}
				
				// commit transaction
				conn.commit();
				// set bool - true
				bool = true;
			}
			else
			{
				logger.info("updateScheduleItemDetails :: Schedule Id / ItemsList as parameters are null. Return false.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleItemDetails()", e);
		}
		finally
		{
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleItemDetails()", e);
				}
			if(null!=pstmt)
				try {
					pstmt.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "updateScheduleItemDetails()", e);
				}
			// set params to null
			scheduleId = null;
			itemsList = null;
		}
		return bool;
	}

	public static Long createSchedule(ScheduleDetails schDetails) throws SQLException 
	{
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			if(null!=schDetails)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO gms3_dmt_conv_schedule
				 * First insert the SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql="INSERT INTO gms3_dmt_fm_sch(FM_SCH_NAME,FM_SCH_BY,FM_PROCESSING_STATUS) VALUES"
						+ "('"+schDetails.getScheduleName()+"','"+schDetails.getUserId()+"','"+schDetails.getScheduleStatus()+"')";
				logger.info("createSchedule :: Generate Schedule Id :: Sql :: > " + sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "FM_SCH_ID" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				if(null!=scheduleId && scheduleId>0)
				{
					logger.info("createSchedule :: Procced for Storing details for Schedule Id :: > " + scheduleId);
					/*
					 * add scheduleId to name and update It
					 * add scheduleId to thread and update It
					 */
					schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
					schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(scheduleId));
					
					String schSql="UPDATE gms3_dmt_fm_sch SET FM_SCH_NAME=?,FM_SCH_TIME=?,FM_THREAD_ID=? WHERE FM_SCH_ID= ?" ;
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, schDetails.getThreadId());
					pstmt.setLong(4, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=  null;
					schSql= null;
					
					
					// commit transaction
					conn.commit();
				}
				else
				{
					logger.info("createSchedule :: Failed to Generate Schedule Id. Return null.");
				}
			}
			else
			{
				logger.info("createSchedule :: Schedule Details are null as parameters, return null.");
			}
		}
		catch(Exception e)
		{
			logger.info("createSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			conn.rollback();
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "createSchedule()", e);
			scheduleId = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			// set schDetails to null
			schDetails = null;
		}
		return scheduleId;
	}

	public static List<ScheduleDetails> getScheduleList(String fromDate, String toDate)
	{
		List<ScheduleDetails> scheduleList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			String sql="SELECT * FROM gms3_dmt_fm_sch  ";
			boolean byDate = null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate);
			if(byDate)
			{
				// compared as timestamps through the driver, not DATE_FORMAT on the stored value (see ScheduleDAO.getScheduleDetails)
				sql = sql+" WHERE FM_SCH_TIME >= ? AND FM_SCH_TIME < ? ";
			}
			sql+=" ORDER BY FM_SCH_ID DESC";
			logger.info("getScheduleList :: Sql :: >"+ sql);
			conn = getConnection();
			pstmt  =conn.prepareStatement(sql);
			if(byDate)
			{
				java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("yyyy-MM-dd");
				day.setLenient(false);
				java.util.Calendar to = java.util.Calendar.getInstance();
				to.setTime(day.parse(toDate.trim()));
				to.add(java.util.Calendar.DATE, 1);
				pstmt.setTimestamp(1, new Timestamp(day.parse(fromDate.trim()).getTime()));
				pstmt.setTimestamp(2, new Timestamp(to.getTimeInMillis()));
			}
			rs = pstmt.executeQuery();
			ScheduleDetails details = null;
			int count=0;
			while(rs.next())
			{
				details=  new ScheduleDetails();
				// INCREMENT COUNT
				count++;
				details.setSrNo(count);
				details.setScheduleId(rs.getLong("FM_SCH_ID"));
				details.setScheduleName(rs.getString("FM_SCH_NAME"));
				details.setThreadId(rs.getString("FM_THREAD_ID"));
				details.setScheduleTime(rs.getTimestamp("FM_SCH_TIME"));
				details.setFinishTime(rs.getTimestamp("FM_FINISH_TIME"));
				details.setUserId(rs.getString("FM_SCH_BY"));
				details.setTotalDocsForProcessing(rs.getLong("FM_TOTAL_DOCS_COUNT"));
				if(null!=rs.getBytes("FM_REMARKS"))
				{
					details.setRemarks(new String(rs.getBytes("FM_REMARKS")));
				}
				details.setScheduleStatus(rs.getString("FM_PROCESSING_STATUS"));
				details.setJobStatus(rs.getString("FM_JOB_STATUS"));
				if(null==scheduleList || scheduleList.size()<=0)
				{
					scheduleList = new ArrayList<ScheduleDetails>();
				}
				scheduleList.add(details);
				details = null;
			}
			details = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleList()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleList()", e);
			}
			fromDate  = null;
			toDate = null;
		}
		return scheduleList;
	}

	public static ArrayList<ItemDetails> getScheduleItemsList(String scheduleId)
	{
		ArrayList<ItemDetails> itemsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			String sql="SELECT * FROM gms3_dmt_fm_sch_itm WHERE FM_SCH_ID=  "+ scheduleId;
			logger.info("getScheduleItemsList :: Sql :: >"+ sql);
			conn = getConnection();
			pstmt  =conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			ItemDetails details = null;
			int count=0;
			while(rs.next())
			{
				details=  new ItemDetails();
				// INCREMENT COUNT
				count++;
				details.setSrNo(count);
				details.setDocIdnSchId(rs.getLong("FM_SCH_ID"));
				details.setDocIdnItemId(rs.getLong("FM_ITM_ID"));
				details.setLocale(rs.getString("FM_LOCALE"));
				details.setCarlineInfo(rs.getString("FM_CARLINE_INFO"));
				details.setChannelLabel(rs.getString("FM_CH_LABEL"));
				details.setChannelRefKey(rs.getString("FM_CH_REFKEY"));
				details.setDisplayCarlineInfo(rs.getString("FM_DISP_CARLINE_INFO"));
				details.setDocumentTypeLabel(rs.getString("FM_DC_TYPE_LABEL"));
				details.setDocumentTypeRefKey(rs.getString("FM_DC_TYPE_REFKEY"));
				details.setTotalDocumentsCount(rs.getLong("FM_DOCS_COUNT"));
				details.setApplicableVinsCount(rs.getLong("FM_APP_VIN_COUNT"));
				if(null!=rs.getBytes("FM_REMARKS"))
				{
					details.setRemarks(new String(rs.getBytes("FM_REMARKS")));
				}
				if(null==itemsList || itemsList.size()<=0)
				{
					itemsList = new ArrayList<ItemDetails>();
				}
				itemsList.add(details);
				details = null;
			}
			details = null;
			sql = null;
			pstmt.close();pstmt=null;
			rs.close();rs=null;
			sql  =null;
			
			if(null!=itemsList && itemsList.size()>0)
			{
				details = null;
				Map<String, Object> docMap = null;
				Map<String, Object> nDocMap = null;
				List<ExcelRowDetails> docsVinList = null;
				for(int a=0;a<itemsList.size();a++)
				{
					details = (ItemDetails)itemsList.get(a);
					// READ APPLICABLE VINS FOR EACH ITEM WHERE RECORD_TYPE="APP_VIN"
					sql="SELECT * FROM gms3_dmt_fm_itm_ap_vin WHERE FM_ITM_ID="+ details.getDocIdnItemId().longValue()+ " AND FM_SCH_ID="+scheduleId +" AND FM_RECORD_TYPE='"+REC_TYPE_APPLICABLE_VIN+"'";
					logger.info("getScheduleItemsAppVINsList :: Sql :: >"+ sql);
					pstmt = conn.prepareStatement(sql);
					rs = pstmt.executeQuery();
					while(rs.next())
					{
						if(null==details.getApplicableVINList() || details.getApplicableVINList().size()<=0)
						{
							details.setApplicableVINList(new ArrayList<ExcelRowDetails>());
						}
						details.getApplicableVINList().add(extractInfoFromResultSet(rs));
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
					sql = null;
					if(null!=details.getApplicableVINList())
					{
						logger.info("----- itemId ::>"+ details.getDocIdnItemId().longValue()+">> APP VIN SIZE :: >"+ details.getApplicableVINList().size());
					}
					// READ IMPACTED DOCUMENTS WITH THEIR SPECIFIC VIN FOR EACH ITEM WHERE RECORD_TYPE="IMP_DOC"
					sql="SELECT * FROM gms3_dmt_fm_itm_ap_vin WHERE FM_ITM_ID="+ details.getDocIdnItemId().longValue()+ " AND FM_SCH_ID="+scheduleId +" AND FM_RECORD_TYPE='"+REC_TYPE_IMPACTED_DOCUMENTS+"'";
					logger.info("getScheduleItemsImpactedDocumentsList :: Sql :: >"+ sql);
					pstmt = conn.prepareStatement(sql);
					rs = pstmt.executeQuery();
					while(rs.next())
					{
						if(null!=rs.getString("FM_DOCUMENT_ID") && !"".equals(rs.getString("FM_DOCUMENT_ID")))
						{
							boolean add = true;
							if(null!=details.getImpactedDocumentsList() && details.getImpactedDocumentsList().size()>0)
							{
								docMap = null;
								for(int e=0;e<details.getImpactedDocumentsList().size();e++)
								{
									docMap=  (Map<String, Object>)details.getImpactedDocumentsList().get(e);
									if(null!=docMap.get(AutomationConstants.DOCUMENTID))
									{
										if(docMap.get(AutomationConstants.DOCUMENTID).toString().trim().toLowerCase().equals(rs.getString("FM_DOCUMENT_ID")))
										{
											add = false;
											docsVinList  =null;
											// DOCUMENT ID ALREADY ADDED - PROCEED FOR ADDING VIN DETAILS TO THE DOCUMENT ID
											if(null!=docMap.get(AutomationConstants.VIN_LIST))
											{
												docsVinList = (ArrayList<ExcelRowDetails>)docMap.get(AutomationConstants.VIN_LIST);
											}
											
											if(null==docsVinList || docsVinList.size()<=0)
											{
												docsVinList = new ArrayList<ExcelRowDetails>();
											}
											docsVinList.add(extractInfoFromResultSet(rs));
											// update docsVINList back in Map
											docMap.put(AutomationConstants.VIN_LIST, docsVinList);
											docsVinList = null;
											break;
										}
									}
									docMap = null;
								}
								docMap = null;
							}
							
							if(add==true)
							{
								nDocMap = new HashMap<String, Object>();
								nDocMap.put(AutomationConstants.DOCUMENTID, rs.getString("FM_DOCUMENT_ID"));
								nDocMap.put(AutomationConstants.REFERENCEKEY, rs.getString("FM_DOC_TYPE_REFKEY"));
								// now add Applicable VIN List for Document
								docsVinList = null;
								if(null==docsVinList || docsVinList.size()<=0)
								{
									docsVinList = new ArrayList<ExcelRowDetails>();
								}
								docsVinList.add(extractInfoFromResultSet(rs));
								// add docsVINList to Map
								nDocMap.put(AutomationConstants.VIN_LIST, docsVinList);
								docsVinList = null;
								
								// add to itemDetails. ImpactedDocuments List
								if(null==details.getImpactedDocumentsList() || details.getImpactedDocumentsList().size()<=0)
								{
									details.setImpactedDocumentsList(new ArrayList<Map<String,Object>>());
								}
								details.getImpactedDocumentsList().add(nDocMap);
								nDocMap = null;
							}
						}
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
					sql = null;
					docMap = null;
					nDocMap = null;
					docsVinList = null;
					if(null!=details.getImpactedDocumentsList())
					{
						logger.info("----- itemId ::>"+ details.getDocIdnItemId()+">> IMP DOC SIZE :: >"+ details.getImpactedDocumentsList().size());
					}
					details=null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleItemsList()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleItemsList()", e);
			}
			scheduleId = null;
		}
		return itemsList;
	}

	private static ExcelRowDetails extractInfoFromResultSet(ResultSet rs) throws SQLException 
	{
		ExcelRowDetails ex = new ExcelRowDetails();
		ex.setScheduleId(rs.getLong("FM_SCH_ID"));
		ex.setItemId(rs.getLong("FM_ITM_ID"));
		ex.setLocale(rs.getString("FM_LOCALE"));
		
		ex.setNewCarlineCode(rs.getString("FM_N_CARLINE_CODE"));
		ex.setNewWmiCode(rs.getString("FM_N_WMI"));
		ex.setNewModelName(rs.getString("FM_N_MODEL_NAME"));
		ex.setNewModelNameEngForMC(rs.getString("FM_N_MODEL_NAME_REG_MC"));
		ex.setNewModelNameRegForMNAOMME(rs.getString("FM_N_MODEL_NAME_REG_MNAOMME"));
		ex.setNewVdsCode(rs.getString("FM_N_VDSCODE"));
		ex.setNewVisStartRange(rs.getString("FM_N_VISSTART"));
		ex.setNewVisEndRange(rs.getString("FM_N_VISEND"));
		ex.setNewRefKey(rs.getString("FM_N_REFKEY"));
		
		
		ex.setOldCarlineCode(rs.getString("FM_O_CARLINE_CODE"));
		ex.setOldWmiCode(rs.getString("FM_O_WMI"));
		ex.setOldModelName(rs.getString("FM_O_MODEL_NAME"));
		ex.setOldModelNameEngForMC(rs.getString("FM_O_MODEL_NAME_REG_MC"));
		ex.setOldModelNameRegForMNAOMME(rs.getString("FM_O_MODEL_NAME_REG_MNAOMME"));
		ex.setOldVdsCode(rs.getString("FM_O_VDSCODE"));
		ex.setOldVisStartRange(rs.getString("FM_O_VISSTART"));
		ex.setOldVisEndRange(rs.getString("FM_O_VISEND"));
		ex.setOldRefKey(rs.getString("FM_O_REFKEY"));
		
		ex.setProcessingStatus(rs.getString("FM_PROCESSING_STATUS"));
		if(null!=rs.getBytes("FM_REMARKS"))
		{
			ex.setRemarks(new String(rs.getBytes("FM_REMARKS")));
		}
		
		if(null!=rs.getString("FM_LOCALE_EXIST_MDM") && rs.getString("FM_LOCALE_EXIST_MDM").trim().toLowerCase().equals("y"))
		{
			ex.setLocaleExistsInMDM(true);
		}
		else
		{
			ex.setLocaleExistsInMDM(false);
		}
		
		if(null!=rs.getString("FM_LOCALE_EXIST_IM") && rs.getString("FM_LOCALE_EXIST_IM").trim().toLowerCase().equals("y"))
		{
			ex.setLocaleExistsInIM(true);
		}
		else
		{
			ex.setLocaleExistsInIM(false);
		}
		
		if(null!=rs.getString("FM_N_VIN_EXIST_MDM") && rs.getString("FM_N_VIN_EXIST_MDM").trim().toLowerCase().equals("y"))
		{
			ex.setNewVinExistsinMDM(true);
		}
		else
		{
			ex.setNewVinExistsinMDM(false);
		}
		
		if(null!=rs.getString("FM_N_VIN_EXIST_IM") && rs.getString("FM_N_VIN_EXIST_IM").trim().toLowerCase().equals("y"))
		{
			ex.setNewVinExistsinIM(true);
		}
		else
		{
			ex.setNewVinExistsinIM(false);
		}
		
		if(null!=rs.getString("FM_O_VIN_EXIST_MDM") && rs.getString("FM_O_VIN_EXIST_MDM").trim().toLowerCase().equals("y"))
		{
			ex.setOldVinExistsinMDM(true);
		}
		else
		{
			ex.setOldVinExistsinMDM(false);
		}
		
		if(null!=rs.getString("FM_O_VIN_EXIST_IM") && rs.getString("FM_O_VIN_EXIST_IM").trim().toLowerCase().equals("y"))
		{
			ex.setOldVinExistsinIM(true);
		}
		else
		{
			ex.setOldVinExistsinIM(false);
		}
		return ex;
	}
	
	
	public static ArrayList<ExcelRowDetails> getUniqueVINsListForSelectedItems(String itemIds)
	{
		ArrayList<ExcelRowDetails> uniqueVinsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			String sql="SELECT DISTINCT FM_LOCALE,FM_N_CARLINE_CODE,FM_N_WMI,FM_N_MODEL_NAME,FM_N_VDSCODE,FM_N_VISSTART,FM_N_VISEND,FM_O_CARLINE_CODE,FM_O_WMI,FM_O_MODEL_NAME,"
					+ " FM_O_VDSCODE,FM_O_VISSTART,FM_O_VISEND FROM gms3_dmt_fm_itm_ap_vin WHERE FM_ITM_ID IN ( "+ itemIds+" ) AND FM_RECORD_TYPE='"+REC_TYPE_APPLICABLE_VIN+"'";
			logger.info("getUniqueVINsListForSelectedItems :: Sql :: >"+ sql);
			conn = getConnection();
			pstmt  =conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			ExcelRowDetails ex = null;
			while(rs.next())
			{
				ex = new ExcelRowDetails();
				
				ex.setLocale(rs.getString("FM_LOCALE"));
				
				ex.setNewCarlineCode(rs.getString("FM_N_CARLINE_CODE"));
				ex.setNewWmiCode(rs.getString("FM_N_WMI"));
				ex.setNewModelName(rs.getString("FM_N_MODEL_NAME"));
				ex.setNewVdsCode(rs.getString("FM_N_VDSCODE"));
				ex.setNewVisStartRange(rs.getString("FM_N_VISSTART"));
				ex.setNewVisEndRange(rs.getString("FM_N_VISEND"));
				
				ex.setOldCarlineCode(rs.getString("FM_O_CARLINE_CODE"));
				ex.setOldWmiCode(rs.getString("FM_O_WMI"));
				ex.setOldModelName(rs.getString("FM_O_MODEL_NAME"));
				ex.setOldVdsCode(rs.getString("FM_O_VDSCODE"));
				ex.setOldVisStartRange(rs.getString("FM_O_VISSTART"));
				ex.setOldVisEndRange(rs.getString("FM_O_VISEND"));
				
				if(null==uniqueVinsList || uniqueVinsList.size()<=0)
				{
					uniqueVinsList = new ArrayList<ExcelRowDetails>();
				}
				uniqueVinsList.add(ex);
			}
			ex = null;
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getUniqueVINsListForSelectedItems()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getUniqueVINsListForSelectedItems()", e);
			}
			itemIds = null;
		}
		return uniqueVinsList;
	}
	
	
	private static ExcelRowDetails prepareAppVINData(ExcelRowDetails details, String scheduleId, Long itemId, String recordType)
	{
		ExcelRowDetails nDetails = new ExcelRowDetails();
		nDetails.setScheduleId(new Long(scheduleId).longValue());
		nDetails.setItemId(itemId);
		nDetails.setRecordType(recordType);
		nDetails.setDocTypeRefKey(details.getDocTypeRefKey());
		nDetails.setDocumentId(details.getDocumentId());
		nDetails.setLocale(details.getLocale());
		nDetails.setLocaleExistsInIM(details.isLocaleExistsInIM());
		nDetails.setLocaleExistsInMDM(details.isLocaleExistsInMDM());
		nDetails.setNewCarlineCode(details.getNewCarlineCode());
		nDetails.setNewModelName(details.getNewModelName());
		nDetails.setNewModelNameEngForMC(details.getNewModelNameEngForMC());
		nDetails.setNewModelNameRegForMNAOMME(details.getNewModelNameRegForMNAOMME());
		nDetails.setNewRefKey(details.getNewRefKey());
		nDetails.setNewVdsCode(details.getNewVdsCode());
		nDetails.setNewVinExistsinIM(details.isNewVinExistsinIM());
		nDetails.setNewVinExistsinMDM(details.isNewVinExistsinMDM());
		nDetails.setNewVisEndRange(details.getNewVisEndRange());
		nDetails.setNewVisStartRange(details.getNewVisStartRange());
		nDetails.setNewWmiCode(details.getNewWmiCode());
		nDetails.setOldCarlineCode(details.getOldCarlineCode());
		nDetails.setOldModelName(details.getOldModelName());
		nDetails.setOldModelNameEngForMC(details.getOldModelNameEngForMC());
		nDetails.setOldModelNameRegForMNAOMME(details.getOldModelNameRegForMNAOMME());
		nDetails.setOldRefKey(details.getOldRefKey());
		nDetails.setOldVdsCode(details.getOldVdsCode());
		nDetails.setOldVinExistsinIM(details.isOldVinExistsinIM());
		nDetails.setOldVinExistsinMDM(details.isOldVinExistsinMDM());
		nDetails.setOldVisEndRange(details.getOldVisEndRange());
		nDetails.setOldVisStartRange(details.getOldVisStartRange());
		nDetails.setOldWmiCode(details.getOldWmiCode());
		nDetails.setProcessingStatus(details.getProcessingStatus());
		nDetails.setRemarks(details.getRemarks());
		
		return nDetails;
	}

	public static boolean deleteItemDetails(Long scheduleId,Long itemId, int totalDocsCountToReduce) 
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && scheduleId.longValue()>0 && null!=itemId && itemId.longValue()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				
				// DELETE FROM gms3_dmt_fm_sch_itm
				String sql = "DELETE FROM gms3_dmt_fm_sch_itm WHERE FM_SCH_ID="+scheduleId.longValue() +" AND FM_ITM_ID="+itemId.longValue();
				pstmt = conn.prepareStatement(sql);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql  =null;
				
				// DELETE FROM gms3_dmt_fm_itm_ap_vin
				sql = "DELETE FROM gms3_dmt_fm_itm_ap_vin WHERE FM_SCH_ID="+scheduleId.longValue() +" AND FM_ITM_ID="+itemId.longValue();
				pstmt = conn.prepareStatement(sql);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql  =null;
				
				// UPDATE SCHEDULE DOCS COUNT
				int totalDocsCount=0;
				sql = "SELECT FM_TOTAL_DOCS_COUNT FROM gms3_dmt_fm_sch WHERE FM_SCH_ID="+scheduleId.longValue();
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					totalDocsCount = rs.getInt("FM_TOTAL_DOCS_COUNT");
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;
				
				int remainingCount = totalDocsCount - totalDocsCountToReduce;
				sql=" UPDATE gms3_dmt_fm_sch SET FM_TOTAL_DOCS_COUNT="+remainingCount+" WHERE FM_SCH_ID="+scheduleId.longValue();
				pstmt = conn.prepareStatement(sql);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql  =null;
				
				// commit transaction
				conn.commit();
				
				logger.info("deleteItemDetails :: Child Enteries Deleted Successfully.");
			}
			else
			{
				logger.info("deleteItemDetails :: Schedule Id / Item Id as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteItemDetails()", e);
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteItemDetails()", e1);
			}
			return false;
		}
		finally
		{
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteItemDetails()", e);
				}
			if(null!=pstmt)
				try {
					pstmt.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(ScheduleDAO.class.getName(), "deleteItemDetails()", e);
				}
			// set params to null
			scheduleId = null;
			itemId = null;
		}
		return true;
	}

	
}
