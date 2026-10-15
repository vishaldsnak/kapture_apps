package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.MissionTypeDetails;

public class MissionTypeDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(MissionTypeDAO.class);
	
	public static ArrayList<MissionTypeDetails> getMissionTypeDetailsList(String langCode, String bookCode) throws SQLException 
	{
//		logger.info("getMissionTypeDetailsList :: Method Starts.");
		ArrayList<MissionTypeDetails> bookList = new ArrayList<MissionTypeDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_transbk_code FROM gms3_mdm_trans_type A, gms3_mdm_trans_book B "
						+ " WHERE "
						+ " A.mdm_transbk_id = B.mdm_transbk_id AND "
						+ " A.mdm_trans_type_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') "
								+ "";
				if(null!=bookCode && !"".equals(bookCode) && !"null".equals(bookCode.trim().toLowerCase()))
				{
					sql  = sql+" AND A.mdm_transbk_id="+new Long(bookCode).longValue();
				}
				if(null!=langCode && !"".equals(langCode) && !"null".equals(langCode.trim().toLowerCase()))
				{
					sql = sql + " AND A.mdm_ml_id="+new Long(langCode).longValue(); 
				}
				
//				logger.info("getMissionTypeDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					MissionTypeDetails typeDetails = new MissionTypeDetails();
					typeDetails.setSrNo(bookList.size()+1);
					typeDetails.setTypeId(rs.getLong("mdm_trans_type_id"));
					typeDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					typeDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					typeDetails.setBookId(rs.getLong("mdm_transbk_id"));
					if(null!=rs.getString("mdm_transbk_code"))
					{
						typeDetails.setBookCode(rs.getString("mdm_transbk_code").trim());
					}
					if(null!=rs.getString("mdm_trans_type_type_code"))
					{
						typeDetails.setTypeCode(rs.getString("mdm_trans_type_type_code").trim());
						typeDetails.setOldTypeCode(rs.getString("mdm_trans_type_type_code").trim());
					}
					if(null!=rs.getString("mdm_trans_type_type_name"))
					{
						typeDetails.setTypeName(rs.getString("mdm_trans_type_type_name").trim());
						typeDetails.setOldTypeName(rs.getString("mdm_trans_type_type_name").trim());
					}
					typeDetails.setFlag(rs.getString("mdm_trans_type_flag"));
					typeDetails.setOldFlag(rs.getString("mdm_trans_type_flag"));
					typeDetails.setEntryTime(rs.getTimestamp("mdm_trans_type_created_tmstp"));
					typeDetails.setUpdatedTime(rs.getTimestamp("mdm_trans_type_updated_tmstp"));
					if(null!=typeDetails.getFlag() && !"".equals(typeDetails.getFlag()) && 
							typeDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						typeDetails.setShowCheckBox(false);
					}
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						typeDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					bookList.add(typeDetails);
					typeDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getMissionTypeDetailsList :: Language id is null as Parameter.");
			}
		
		}
		catch(Exception e)
		{
			logger.info("getMissionTypeDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "getMissionTypeDetailsList()", e);
			logger.info("getMissionTypeDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			bookCode = null;
		}
//		logger.info("getMissionTypeDetailsList :: Method Ends.");
		return bookList;
	}

	public static boolean saveMissionTypeDetails(MissionTypeDetails typeDetails) throws SQLException
	{
		logger.info("saveMissionTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=typeDetails.getTypeCode())
			{
				typeDetails.setTypeCode(typeDetails.getTypeCode().trim());
			}
			if(null!=typeDetails.getTypeName())
			{
				typeDetails.setTypeName(typeDetails.getTypeName().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_trans_type(mdm_cl_id, mdm_ml_id, mdm_transbk_id,mdm_trans_type_type_code,mdm_trans_type_type_name,"
					+ "mdm_trans_type_flag,mdm_trans_type_created_tmstp) VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, typeDetails.getCountryLocaleId());
			pstmt.setLong(2, typeDetails.getManualLanguageId());
			pstmt.setLong(3, typeDetails.getBookId());
			pstmt.setString(4, typeDetails.getTypeCode());
			pstmt.setString(5, typeDetails.getTypeName());
			pstmt.setString(6, typeDetails.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveMissionTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "saveMissionTypeDetails()", e);
			logger.info("saveMissionTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			typeDetails = null;
		}
		logger.info("saveMissionTypeDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<MissionTypeDetails> updateMissionTypeDetails(ArrayList<MissionTypeDetails> list) throws SQLException
	{
		logger.info("updateMissionTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				String sql="";
				MissionTypeDetails typeDetails = new MissionTypeDetails();
				for(int a=0;a<list.size();a++)
				{
					typeDetails = (MissionTypeDetails)list.get(a);
					if(null!=typeDetails.getTypeCode())
					{
						typeDetails.setTypeCode(typeDetails.getTypeCode().trim());
					}
					if(null!=typeDetails.getTypeName())
					{
						typeDetails.setTypeName(typeDetails.getTypeName().trim());
					}
					if(null!=typeDetails.getOldTypeCode())
					{
						typeDetails.setOldTypeCode(typeDetails.getOldTypeCode().trim());
					}
					if(null!=typeDetails.getOldTypeName())
					{
						typeDetails.setOldTypeName(typeDetails.getOldTypeName().trim());
					}
					if(null!=typeDetails.getSyncStatus())
					{
						typeDetails.setSyncStatus(typeDetails.getSyncStatus().trim());
					}
					
					String syncStatus = typeDetails.getSyncStatus();
					/*
					 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
						If PreviousStatus and CurrentStatus is Same and Status is Active - 
							if MISSION TYPE CODE / NAME , then update syncStatus as N

						If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
						If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
					 */
					if(typeDetails.getOldFlag().equals(typeDetails.getFlag()))
					{
						// STATUS IS SAME - check only for active
						if(typeDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
						{
							String keyToCheck=typeDetails.getOldTypeCode()+typeDetails.getOldTypeName();
							String existData=typeDetails.getTypeCode()+typeDetails.getTypeName();
							
							if(keyToCheck.trim().toLowerCase().equals(existData.trim().toLowerCase()))
							{
								// ALL ARE SAME -DO NOTHING WITH SYNC STATUS
							}
							else
							{
								// SOMETHING CHANGED - SET SYNC STATUS TO N
								syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
							}
							keyToCheck=  null;
							existData= null;
						}
					}
					else
					{
						if(typeDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
								typeDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
						{
							// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
							syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
						}
						else if(typeDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
								typeDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
						{
							// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
							syncStatus = null;
						}
					}
					
					sql="UPDATE gms3_mdm_trans_type SET mdm_trans_type_type_code =?, mdm_trans_type_type_name =?,"
							+ "mdm_trans_type_flag=?,mdm_trans_type_updated_tmstp=?,mdm_sync_status = ? WHERE mdm_trans_type_id =?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, typeDetails.getTypeCode());
					pstmt.setString(2, typeDetails.getTypeName());
					pstmt.setString(3, typeDetails.getFlag());
					pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(5, syncStatus);
					pstmt.setLong(6, typeDetails.getTypeId());
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt= null;
					typeDetails.setSaveStatusWhileImport(true);
					syncStatus=  null;
				}
			}
			
		}
		catch(Exception e)
		{
			logger.info("updateMissionTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "updateMissionTypeDetails()", e);
			logger.info("updateMissionTypeDetails :: ################ Exception ################");
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateMissionTypeDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteMissionTypeDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteMissionTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=deleteIds && !"".equals(deleteIds))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens=deleteIds.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						// HERE FOR ALL RECORDS GETTING INACTIVE - SET SYNC STATUS TO NULL
						pstmt = null;
						String sql="UPDATE gms3_mdm_trans_type SET mdm_trans_type_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_trans_type_updated_tmstp = ? ,mdm_sync_status = ? WHERE mdm_trans_type_id = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(2, null);
						pstmt.executeUpdate();
						pstmt.close();pstmt= null;
						sql = null;
					}
				}
				// commit the transaction
				conn.commit();
				tokens = null;
			}
			else
			{
				logger.info("deleteMissionTypeDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteMissionTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "deleteMissionTypeDetails()", e);
			logger.info("deleteMissionTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			deleteIds=null;
		}
		logger.info("deleteMissionTypeDetails :: Method Ends.");
		return true;
	}
	
	public static boolean activeMissionTypeDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeMissionTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=activeIdsList && activeIdsList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				Map<String, String> dataMap= null;
				String syncStatus="";
				for(int i=0;i<activeIdsList.size();i++)
				{
					dataMap= (Map<String, String>)activeIdsList.get(i);
					syncStatus="";
					if(null!=dataMap.get("FLAG") && !"".equals(dataMap.get("FLAG")))
					{
						// IF OLD FLAG WAS DRAFT - SET SYNC STATUS TO N
						if(dataMap.get("FLAG").toString().equals(ApplicationProperties.getProperty("flag.value.draft")))
						{
							syncStatus=AccessManagementInterface.SYNC_STATUS_NO;
						}
						// IF OLD FLAG WAS ACTIVE - DO NOTHING
					}
					pstmt = null;
					String sql="UPDATE gms3_mdm_trans_type SET mdm_trans_type_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_trans_type_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_trans_type_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					syncStatus=  null;
					dataMap = null;
				}
				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("activeMissionTypeDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeMissionTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "activeMissionTypeDetails()", e);
			logger.info("activeMissionTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			activeIdsList=null;
		}
		logger.info("activeMissionTypeDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<MissionTypeDetails> importMissionTypeDetails(ArrayList<MissionTypeDetails> list) throws SQLException
	{
		logger.info("importMissionTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				/*
				 * ITERATE AND PROCESS EACH ROW.
				 */
				String getMissionTypeSql = "";
				String sql="";
				MissionTypeDetails typeDetails = new MissionTypeDetails();
				long autoMissionTypeId=0;
				for(int a=0;a<list.size();a++)
				{
					typeDetails = (MissionTypeDetails)list.get(a);
					if(null!=typeDetails.getTypeCode())
					{
						typeDetails.setTypeCode(typeDetails.getTypeCode().trim());
					}
					if(null!=typeDetails.getTypeName())
					{
						typeDetails.setTypeName(typeDetails.getTypeName().trim());
					}
					
					try
					{
						String oldTypeName="";
						String oldFLag="";
						String syncStatus="";
						/*
						 * CHECK WHETHER VIN EXISTS OR NOT
						 * IF YES -  THEN UPDATE VIN
						 * ELSE - INSERT VIN
						 */
						getMissionTypeSql = "SELECT * FROM gms3_mdm_trans_type WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_transbk_id=? AND  mdm_trans_type_type_code=? "
								+ " AND mdm_trans_type_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importMissionTypeDetails :: getMissionTypeSql :: > " + getMissionTypeSql);
						pstmt = conn.prepareStatement(getMissionTypeSql);
						pstmt.setLong(1, typeDetails.getCountryLocaleId());
						pstmt.setLong(2, typeDetails.getManualLanguageId());
						pstmt.setLong(3, typeDetails.getBookId());
						pstmt.setString(4, typeDetails.getTypeCode());
						rs = pstmt.executeQuery();
						autoMissionTypeId=0;
						if(rs.next())
						{
							autoMissionTypeId= rs.getLong("mdm_trans_type_id");
							if(null!=rs.getString("mdm_trans_type_type_name") && !"".equals(rs.getString("mdm_trans_type_type_name")))
							{
								oldTypeName = rs.getString("mdm_trans_type_type_name").trim();
							}
							oldFLag= rs.getString("mdm_trans_type_flag");
							if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
							{
								syncStatus = rs.getString("mdm_sync_status").trim();
							}
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getMissionTypeSql = null;
						
						if(autoMissionTypeId>0)
						{
							/*
							 * checks for sync status - 
							 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
								If oldStatus is Active - 
									Here, check for TYPE NAME CHANGES (because fetched using ALL TYPE CODES ), 
									 	then update syncStatus as N
										else let it remains as it is
							 */
							if(oldFLag.equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								String keyToCheck=oldTypeName;
								String exist = typeDetails.getTypeName();
								if(keyToCheck.trim().toLowerCase().equals(exist.trim().toLowerCase()))
								{
									// NOTHING CHANGED - LET THE SYNC STATUS REMAINS AS IT IS
								}
								else
								{
									// SOMETHING CHANGED. SET SYNC STATUS AS = N
									syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
								}
								keyToCheck=null;
								exist = null;
							}
							
							pstmt = null;
							logger.info("importMissionTypeDetails :: MissionType Already Exists. Update Row for Auto MissionType id : >" + autoMissionTypeId);
							 sql="UPDATE gms3_mdm_trans_type SET mdm_trans_type_type_code =?, mdm_trans_type_type_name =?,"
										+ "mdm_trans_type_flag=?,mdm_trans_type_updated_tmstp=?,mdm_sync_status = ? WHERE mdm_trans_type_id =?";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, typeDetails.getTypeCode());
								pstmt.setString(2, typeDetails.getTypeName());
								pstmt.setString(3, typeDetails.getFlag());
								pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(5, syncStatus);
								pstmt.setLong(6, autoMissionTypeId);
								pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							typeDetails.setSaveStatusWhileImport(true);
							
							oldFLag = null;
							oldTypeName = null;
							syncStatus=  null;
						}
						else
						{
							pstmt = null;
							logger.info("importMissionTypeDetails :: MissionType Does not Exists. Insert New Row.");
							
							sql="INSERT INTO gms3_mdm_trans_type(mdm_cl_id, mdm_ml_id, mdm_transbk_id,mdm_trans_type_type_code,mdm_trans_type_type_name,"
									+ "mdm_trans_type_flag,mdm_trans_type_created_tmstp) VALUES(?,?,?,?,?,?,?)";
							pstmt = conn.prepareStatement(sql);
							pstmt.setLong(1, typeDetails.getCountryLocaleId());
							pstmt.setLong(2, typeDetails.getManualLanguageId());
							pstmt.setLong(3, typeDetails.getBookId());
							pstmt.setString(4, typeDetails.getTypeCode());
							pstmt.setString(5, typeDetails.getTypeName());
							pstmt.setString(6, typeDetails.getFlag());
							pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							typeDetails.setSaveStatusWhileImport(true);
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "importMissionTypeDetails()", e);
					}
					
					autoMissionTypeId = 0;
					typeDetails = null;
				}
				
			}
			else
			{
				logger.info("importMissionTypeDetails :: No Mission Type Data Passed as parameter for Importing.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "importMissionTypeDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
		}
		logger.info("importMissionTypeDetails :: Method Ends.");
		return list;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importMissionTypeDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(MissionTypeDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getMissionTypeSql = null;
						getMissionTypeSql = "SELECT * FROM gms3_mdm_trans_type WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_transbk_id=? AND  mdm_trans_type_type_code=? "
								+ " AND mdm_trans_type_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importMissionTypeDetails :: getMissionTypeSql :: > " + getMissionTypeSql);
						pstmt = conn.prepareStatement(getMissionTypeSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setLong(3, details.getBookId());
						pstmt.setString(4, details.getTypeCode());
						rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_trans_type_id");
			}
		} finally {
			if (null != rs)
				rs.close();
			if (null != pstmt)
				pstmt.close();
		}
		return existingId;
	}




}