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
import com.mazda.gms3.mdm.vo.EngineTypeDetails;

public class EngineTypeDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(EngineTypeDAO.class);
	
	public static ArrayList<EngineTypeDetails> getEngineTypeDetailsList(String langCode, String bookCode) throws SQLException 
	{
//		logger.info("getEngineTypeDetailsList :: Method Starts.");
		ArrayList<EngineTypeDetails> bookList = new ArrayList<EngineTypeDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_eb_code FROM gms3_mdm_engine_type A, gms3_mdm_engine_book B "
						+ " WHERE A.mdm_eb_id = B.mdm_eb_id AND "
						+ " A.mdm_et_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				if(null!=bookCode && !"".equals(bookCode) && !"null".equals(bookCode.trim().toLowerCase()))
				{
					sql  = sql+" AND A.mdm_eb_id="+new Long(bookCode).longValue();
				}
				if(null!=langCode && !"".equals(langCode) && !"null".equals(langCode.trim().toLowerCase()))
				{
					sql = sql+" AND A.mdm_ml_id="+new Long(langCode).longValue();
				}
				
//				logger.info("getEngineTypeDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					EngineTypeDetails typeDetails = new EngineTypeDetails();
					typeDetails.setSrNo(bookList.size()+1);
					typeDetails.setTypeId(rs.getLong("mdm_et_id"));
					typeDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					typeDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					typeDetails.setBookId(rs.getLong("mdm_eb_id"));
					if(null!=rs.getString("mdm_eb_code"))
					{
						typeDetails.setBookCode(rs.getString("mdm_eb_code").trim());
					}
					if(null!=rs.getString("mdm_et_type_code"))
					{
						typeDetails.setTypeCode(rs.getString("mdm_et_type_code").trim());
						typeDetails.setOldTypeCode(rs.getString("mdm_et_type_code").trim());
					}
					if(null!=rs.getString("mdm_et_type_name"))
					{
						typeDetails.setTypeName(rs.getString("mdm_et_type_name").trim());
						typeDetails.setOldTypeName(rs.getString("mdm_et_type_name").trim());
					}
					typeDetails.setFlag(rs.getString("mdm_et_flag"));
					typeDetails.setOldFlag(rs.getString("mdm_et_flag"));
					typeDetails.setEntryTime(rs.getTimestamp("mdm_et_created_tmstp"));
					typeDetails.setUpdatedTime(rs.getTimestamp("mdm_et_updated_tmstp"));
					if(null!=rs.getString("mdm_et_group_type"))
					{
						typeDetails.setGroupCode(rs.getString("mdm_et_group_type").trim());
						typeDetails.setOldGroupCode(rs.getString("mdm_et_group_type").trim());
					}
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
				logger.info("getEngineTypeDetailsList :: Language id is null as Parameter.");
			}
		}
		catch(Exception e)
		{
			logger.info("getEngineTypeDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "getEngineTypeDetailsList()", e);
			logger.info("getEngineTypeDetailsList :: ################ Exception ################");
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
//		logger.info("getEngineTypeDetailsList :: Method Ends.");
		return bookList;
	}


	public static boolean saveEngineTypeDetails(EngineTypeDetails typeDetails) throws SQLException
	{
		logger.info("saveEngineTypeDetails :: Method Starts.");
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
			if(null!=typeDetails.getGroupCode())
			{
				typeDetails.setGroupCode(typeDetails.getGroupCode().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_engine_type(mdm_cl_id, mdm_ml_id,mdm_eb_id,mdm_et_type_code,mdm_et_type_name,"
					+ "mdm_et_flag,mdm_et_created_tmstp, mdm_et_group_type) VALUES(?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, typeDetails.getCountryLocaleId());
			pstmt.setLong(2, typeDetails.getManualLanguageId());
			pstmt.setLong(3, typeDetails.getBookId());
			pstmt.setString(4, typeDetails.getTypeCode());
			pstmt.setString(5, typeDetails.getTypeName());
			pstmt.setString(6, typeDetails.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(8, typeDetails.getGroupCode());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveEngineTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "saveEngineTypeDetails()", e);
			logger.info("saveEngineTypeDetails :: ################ Exception ################");
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
		logger.info("saveEngineTypeDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<EngineTypeDetails> updateEngineTypeDetails(ArrayList<EngineTypeDetails> list) throws SQLException
	{
		logger.info("updateEngineTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				String sql="";
				EngineTypeDetails typeDetails = new EngineTypeDetails();
				for(int a= 0;a<list.size();a++)
				{
					typeDetails  = (EngineTypeDetails)list.get(a);
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
						if(null!=typeDetails.getGroupCode())
						{
							typeDetails.setGroupCode(typeDetails.getGroupCode().trim());
						}
						if(null!=typeDetails.getOldTypeCode())
						{
							typeDetails.setOldTypeCode(typeDetails.getOldTypeCode().trim());
						}
						if(null!=typeDetails.getOldTypeName())
						{
							typeDetails.setOldTypeName(typeDetails.getOldTypeName().trim());
						}
						if(null!=typeDetails.getOldGroupCode())
						{
							typeDetails.setOldGroupCode(typeDetails.getOldGroupCode().trim());
						}
						if(null!=typeDetails.getSyncStatus())
						{
							typeDetails.setSyncStatus(typeDetails.getSyncStatus().trim());
						}
						
						String syncStatus = typeDetails.getSyncStatus();
						/*
						 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
							If PreviousStatus and CurrentStatus is Same and Status is Active - 
								if ENGINE TYPE CODE / NAME/ GROUP NAME , then update syncStatus as N

							If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
							If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
						 */
						if(typeDetails.getOldFlag().equals(typeDetails.getFlag()))
						{
							// STATUS IS SAME - check only for active
							if(typeDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								String keyToCheck=typeDetails.getOldTypeCode()+typeDetails.getOldTypeName()+typeDetails.getOldGroupCode();
								String existData=typeDetails.getTypeCode()+typeDetails.getTypeName()+typeDetails.getGroupCode();
								
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
						
						
						
						sql="UPDATE gms3_mdm_engine_type SET mdm_et_type_code = ?, mdm_et_type_name =?,"
								+ "mdm_et_flag=?,mdm_et_updated_tmstp=?, mdm_et_group_type=?,mdm_sync_status = ? WHERE mdm_et_id =?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, typeDetails.getTypeCode());
						pstmt.setString(2, typeDetails.getTypeName());
						pstmt.setString(3, typeDetails.getFlag());
						pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(5, typeDetails.getGroupCode());
						pstmt.setString(6, syncStatus);
						pstmt.setLong(7, typeDetails.getTypeId());
						pstmt.executeUpdate();
						pstmt.close();pstmt = null;
						sql = null;
						typeDetails.setSaveStatusWhileImport(true);
						syncStatus = null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "updateEngineTypeDetails()", e);
					}
					typeDetails = null;
				}
			}
			else
			{
				logger.info("updateEngineTypeDetails :: List as Parameter is null. Nothing to Update.");
			}
			
		}
		catch(Exception e)
		{
			logger.info("updateEngineTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "updateEngineTypeDetails()", e);
			logger.info("updateEngineTypeDetails :: ################ Exception ################");
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateEngineTypeDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteEngineTypeDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteEngineTypeDetails :: Method Starts.");
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
					String sql="";
					for(int i=0;i<tokens.length;i++)
					{
						// HERE FOR ALL RECORDS GETTING INACTIVE - SET SYNC STATUS TO NULL
						pstmt = null;
						sql="UPDATE gms3_mdm_engine_type SET mdm_et_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_et_updated_tmstp = ?,mdm_sync_status = ?  WHERE mdm_et_id = "+ tokens[i].toString();
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
				logger.info("deleteEngineTypeDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteEngineTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "deleteEngineTypeDetails()", e);
			logger.info("deleteEngineTypeDetails :: ################ Exception ################");
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
		logger.info("deleteEngineTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean activeEngineTypeDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeEngineTypeDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_engine_type SET mdm_et_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_et_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+="WHERE mdm_et_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					syncStatus = null;
					dataMap=  null;
				}
				// commit the transaction
				conn.commit();
				
			}
			else
			{
				logger.info("activeEngineTypeDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeEngineTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "activeEngineTypeDetails()", e);
			logger.info("activeEngineTypeDetails :: ################ Exception ################");
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
		logger.info("activeEngineTypeDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<EngineTypeDetails> importEngineTypeDetails(ArrayList<EngineTypeDetails> list) throws SQLException
	{
		logger.info("importEngineTypeDetails :: Method Starts.");
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
				String getEngineTypeSql = "";
				String sql="";
				EngineTypeDetails typeDetails = new EngineTypeDetails();
				long autoEngineTypeId=0;
				for(int a=0;a<list.size();a++)
				{
					typeDetails = (EngineTypeDetails)list.get(a);
					if(null!=typeDetails.getTypeCode())
					{
						typeDetails.setTypeCode(typeDetails.getTypeCode().trim());
					}
					if(null!=typeDetails.getTypeName())
					{
						typeDetails.setTypeName(typeDetails.getTypeName().trim());
					}
					if(null!=typeDetails.getGroupCode())
					{
						typeDetails.setGroupCode(typeDetails.getGroupCode().trim());
					}
					
					try
					{
						String oldTypeName="";
						String oldGroupCode="";
						String oldFLag="";
						String syncStatus="";
						/*
						 * CHECK WHETHER VIN EXISTS OR NOT
						 * IF YES -  THEN UPDATE VIN
						 * ELSE - INSERT VIN
						 */
						getEngineTypeSql = "SELECT * FROM gms3_mdm_engine_type WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_eb_id=? AND  mdm_et_type_code=? "
								+ " AND mdm_et_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importEngineTypeDetails :: getEngineTypeSql :: > " + getEngineTypeSql);
						pstmt = conn.prepareStatement(getEngineTypeSql);
						pstmt.setLong(1, typeDetails.getCountryLocaleId());
						pstmt.setLong(2, typeDetails.getManualLanguageId());
						pstmt.setLong(3, typeDetails.getBookId());
						pstmt.setString(4, typeDetails.getTypeCode());
						rs = pstmt.executeQuery();
						autoEngineTypeId=0;
						if(rs.next())
						{
							autoEngineTypeId= rs.getLong("mdm_et_id");
							if(null!=rs.getString("mdm_et_type_name") && !"".equals(rs.getString("mdm_et_type_name")))
							{
								oldTypeName = rs.getString("mdm_et_type_name").trim();
							}
							if(null!=rs.getString("mdm_et_group_type") && !"".equals(rs.getString("mdm_et_group_type")))
							{
								oldGroupCode = rs.getString("mdm_et_group_type").trim();
							}
							oldFLag= rs.getString("mdm_et_flag");
							if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
							{
								syncStatus = rs.getString("mdm_sync_status").trim();
							}
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getEngineTypeSql = null;
						
						if(autoEngineTypeId>0)
						{
							/*
							 * checks for sync status - 
							 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
								If oldStatus is Active - 
									Here, check for TYPE NAME, GROUP NAME CHANGES (because fetched using ALL TYPE CODES ), 
									 	then update syncStatus as N
										else let it remains as it is
							 */
							if(oldFLag.equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								String keyToCheck=oldTypeName+oldGroupCode;
								String exist = typeDetails.getTypeName()+typeDetails.getGroupCode();
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
							logger.info("importEngineTypeDetails :: EngineType Already Exists. Update Row for Auto EngineType id : >" + autoEngineTypeId);
							sql="UPDATE gms3_mdm_engine_type SET mdm_et_type_code = ?, mdm_et_type_name =?,"
									+ "mdm_et_updated_tmstp=?, mdm_et_group_type=?,mdm_sync_status=? WHERE mdm_et_id =?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, typeDetails.getTypeCode());
							pstmt.setString(2, typeDetails.getTypeName());
							pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(4, typeDetails.getGroupCode());
							pstmt.setString(5, syncStatus);
							pstmt.setLong(6, autoEngineTypeId);
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							typeDetails.setSaveStatusWhileImport(true);
							
							oldTypeName=  null;
							oldGroupCode=  null;
							oldFLag=null;
							syncStatus= null;
						}
						else
						{
							pstmt = null;
							logger.info("importEngineTypeDetails :: EngineType Does not Exists. Insert New Row.");
							
							sql="INSERT INTO gms3_mdm_engine_type(mdm_cl_id, mdm_ml_id,mdm_eb_id,mdm_et_type_code,mdm_et_type_name,"
									+ "mdm_et_flag,mdm_et_created_tmstp, mdm_et_group_type) VALUES(?,?,?,?,?,?,?,?)";
							pstmt = conn.prepareStatement(sql);
							pstmt.setLong(1, typeDetails.getCountryLocaleId());
							pstmt.setLong(2, typeDetails.getManualLanguageId());
							pstmt.setLong(3, typeDetails.getBookId());
							pstmt.setString(4, typeDetails.getTypeCode());
							pstmt.setString(5, typeDetails.getTypeName());
							pstmt.setString(6, typeDetails.getFlag());
							pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(8, typeDetails.getGroupCode());
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							typeDetails.setSaveStatusWhileImport(true);
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "importEngineTypeDetails()", e);
					}
					
					autoEngineTypeId = 0;
					typeDetails = null;
				}
				
			}
			else
			{
				logger.info("importEngineTypeDetails :: No Engine Type Data Passed as parameter for Importing.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(EngineTypeDAO.class.getName(), "importEngineTypeDetails()", e);
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
		logger.info("importEngineTypeDetails :: Method Ends.");
		return list;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importEngineTypeDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(EngineTypeDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getEngineTypeSql = null;
						getEngineTypeSql = "SELECT * FROM gms3_mdm_engine_type WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_eb_id=? AND  mdm_et_type_code=? "
								+ " AND mdm_et_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importEngineTypeDetails :: getEngineTypeSql :: > " + getEngineTypeSql);
						pstmt = conn.prepareStatement(getEngineTypeSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setLong(3, details.getBookId());
						pstmt.setString(4, details.getTypeCode());
						rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_et_id");
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