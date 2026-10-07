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
import com.mazda.gms3.mdm.utils.ImportActionUtils;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.BodyTypeDetails;

public class BodyTypeDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(BodyTypeDAO.class);
	
	
	public static ArrayList<BodyTypeDetails> getAllBodyTypesList(String langCode) throws SQLException 
	{
//		logger.info("getAllBodyTypesList :: Method Starts.");
		ArrayList<BodyTypeDetails> bodyTypeList = new ArrayList<BodyTypeDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{

				conn = getConnection();
				
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_body_type A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND A.mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND A.mdm_bt_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				
//				logger.info("getAllBodyTypesList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					BodyTypeDetails bDetails = new BodyTypeDetails();
					bDetails.setSrNo(bodyTypeList.size()+1);
					bDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					bDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					bDetails.setBodyTypeId(rs.getLong("mdm_bt_id"));
					if(null!=rs.getString("mdm_bt_body_type") && !"".equals(rs.getString("mdm_bt_body_type")))
					{
						bDetails.setBodyCode(rs.getString("mdm_bt_body_type").trim());
						bDetails.setOldBodyCode(rs.getString("mdm_bt_body_type").trim());
					}
					if(null!=rs.getString("mdm_bt_body_type_desc_eng") && !"".equals(rs.getString("mdm_bt_body_type_desc_eng")))
					{
						bDetails.setBodyCodeDescription(rs.getString("mdm_bt_body_type_desc_eng").trim());
						bDetails.setOldBodyCodeDescription(rs.getString("mdm_bt_body_type_desc_eng").trim());
					}
					if(null!=rs.getString("mdm_bt_body_type_desc_reg") && !"".equals(rs.getString("mdm_bt_body_type_desc_reg")))
					{
						bDetails.setBodyCodeDescriptionRegional(rs.getString("mdm_bt_body_type_desc_reg").trim());
						bDetails.setOldBodyCodeDescriptionRegional(rs.getString("mdm_bt_body_type_desc_reg").trim());
					}
					bDetails.setFlag(rs.getString("mdm_bt_flag"));
					bDetails.setOldFlag(rs.getString("mdm_bt_flag"));
					bDetails.setEntryTime(rs.getTimestamp("mdm_bt_created_tmstp"));
					bDetails.setUpdatedTime(rs.getTimestamp("mdm_bt_updated_tmstp"));
					
					if(null!=bDetails.getFlag() && !"".equals(bDetails.getFlag()) && 
							bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						bDetails.setShowCheckBox(false);
					}
					
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						bDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					bodyTypeList.add(bDetails);
					bDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getAllBodyTypesList :: Language Code as parameter is null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getAllBodyTypesList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "getAllBodyTypesList()", e);
			logger.info("getAllBodyTypesList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
//		logger.info("getAllBodyTypesList :: Method Ends.");
		return bodyTypeList;
	}
	
	public static boolean saveBodyTypeDetails(BodyTypeDetails bDetails) throws SQLException
	{
		logger.info("saveBodyTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=bDetails.getBodyCode() && !"".equals(bDetails.getBodyCode()))
			{
				bDetails.setBodyCode(bDetails.getBodyCode().trim());
			}
			if(null!=bDetails.getBodyCodeDescription() && !"".equals(bDetails.getBodyCodeDescription()))
			{
				bDetails.setBodyCodeDescription(bDetails.getBodyCodeDescription().trim());
			}
			if(null!=bDetails.getBodyCodeDescriptionRegional() && !"".equals(bDetails.getBodyCodeDescriptionRegional()))
			{
				bDetails.setBodyCodeDescriptionRegional(bDetails.getBodyCodeDescriptionRegional().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_body_type(mdm_bt_body_type,mdm_bt_body_type_desc_eng,"
					+ "mdm_bt_flag,mdm_bt_created_tmstp,mdm_cl_id,mdm_ml_id,mdm_bt_body_type_desc_reg) VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, bDetails.getBodyCode());
			pstmt.setString(2, bDetails.getBodyCodeDescription());
			pstmt.setString(3, bDetails.getFlag().trim());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, bDetails.getCountryLocaleId());
			pstmt.setLong(6, bDetails.getManualLanguageId());
			pstmt.setString(7, bDetails.getBodyCodeDescriptionRegional());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveBodyTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "saveBodyTypeDetails()", e);
			logger.info("saveBodyTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			bDetails = null;
		}
		logger.info("saveBodyTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean updateBodyTypeDetails(BodyTypeDetails bDetails) throws SQLException
	{
		logger.info("updateBodyTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=bDetails.getBodyCode() && !"".equals(bDetails.getBodyCode()))
			{
				bDetails.setBodyCode(bDetails.getBodyCode().trim());
			}
			if(null!=bDetails.getBodyCodeDescription() && !"".equals(bDetails.getBodyCodeDescription()))
			{
				bDetails.setBodyCodeDescription(bDetails.getBodyCodeDescription().trim());
			}
			if(null!=bDetails.getBodyCodeDescriptionRegional() && !"".equals(bDetails.getBodyCodeDescriptionRegional()))
			{
				bDetails.setBodyCodeDescriptionRegional(bDetails.getBodyCodeDescriptionRegional().trim());
			}
			if(null!=bDetails.getOldBodyCode() && !"".equals(bDetails.getOldBodyCode()))
			{
				bDetails.setOldBodyCode(bDetails.getOldBodyCode().trim());
			}
			if(null!=bDetails.getOldBodyCodeDescription() && !"".equals(bDetails.getOldBodyCodeDescription()))
			{
				bDetails.setOldBodyCodeDescription(bDetails.getOldBodyCodeDescription().trim());
			}
			if(null!=bDetails.getOldBodyCodeDescriptionRegional() && !"".equals(bDetails.getOldBodyCodeDescriptionRegional()))
			{
				bDetails.setOldBodyCodeDescriptionRegional(bDetails.getOldBodyCodeDescriptionRegional().trim());
			}
			
			if(null!=bDetails.getSyncStatus() && !"".equals(bDetails.getSyncStatus()))
			{
				bDetails.setSyncStatus(bDetails.getSyncStatus().trim());
			}
			
			String syncStatus = bDetails.getSyncStatus();
			/*
			 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
				If PreviousStatus and CurrentStatus is Same and Status is Active - 
					if Body CODE / NAME / REGIONAL NAME , then update syncStatus as N

				If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
				If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
			 */
			if(bDetails.getOldFlag().equals(bDetails.getFlag()))
			{
				// STATUS IS SAME - check only for active
				if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					String keyToCheck=bDetails.getOldBodyCode()+bDetails.getOldBodyCodeDescription();
					if(null!=bDetails.getOldBodyCodeDescriptionRegional() && !"".equals(bDetails.getOldBodyCodeDescriptionRegional()))
					{
						keyToCheck+=bDetails.getOldBodyCodeDescriptionRegional();
					}
					String existData=bDetails.getBodyCode()+bDetails.getBodyCodeDescription();
					if(null!=bDetails.getBodyCodeDescriptionRegional() && !"".equals(bDetails.getBodyCodeDescriptionRegional()))
					{
						existData+=bDetails.getBodyCodeDescriptionRegional();
					}
							
					
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
				if(bDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
						bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				else if(bDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
						bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
				{
					// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
					syncStatus = null;
				}
			}

			conn = getConnection();
			String sql="UPDATE gms3_mdm_body_type SET mdm_bt_body_type=?,mdm_bt_body_type_desc_eng =?,"
					+ "mdm_bt_flag=?,mdm_bt_updated_tmstp=?,mdm_bt_body_type_desc_reg=?,mdm_sync_status=? WHERE mdm_bt_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, bDetails.getBodyCode());
			pstmt.setString(2, bDetails.getBodyCodeDescription());
			pstmt.setString(3, bDetails.getFlag().trim());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(5,bDetails.getBodyCodeDescriptionRegional());
			pstmt.setString(6, syncStatus);
			pstmt.setLong(7, bDetails.getBodyTypeId());
			pstmt.executeUpdate();
			sql = null;
			syncStatus = null;
		}
		catch(Exception e)
		{
			logger.info("updateBodyTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "updateBodyTypeDetails()", e);
			logger.info("updateBodyTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			bDetails=  null;
		}
		logger.info("updateBodyTypeDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteBodyDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteBodyDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_body_type SET mdm_bt_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " , mdm_bt_updated_tmstp = ?,mdm_sync_status=?  WHERE mdm_bt_id = "+ tokens[i].toString();
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
				logger.info("deleteBodyDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteBodyDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "deleteBodyDetails()", e);
			logger.info("deleteBodyDetails :: ################ Exception ################");
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
		logger.info("deleteBodyDetails :: Method Ends.");
		return true;
	}

	public static boolean activeBodyDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeBodyDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_body_type SET mdm_bt_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " , mdm_bt_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_bt_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					dataMap = null;
					syncStatus=  null;
				}
				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("activeBodyDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeBodyDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "activeBodyDetails()", e);
			logger.info("activeBodyDetails :: ################ Exception ################");
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
		logger.info("activeBodyDetails :: Method Ends.");
		return true;
	}


	/**
	 * EXCEL IMPORT LOOKUP - the id of the ACTIVE row an import line refers to, or 0 when there is
	 * none. Unique criteria: Country Locale + Manual Language + Code, case- and space-insensitive -
	 * the same rule the screen's own Entry / Update enforces. Deleted rows are never matched, so an
	 * add / update of a code that was deleted earlier inserts a new row.
	 */
	public static long findExistingIdForImport(BodyTypeDetails details, Connection conn) throws Exception
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT mdm_bt_id FROM gms3_mdm_body_type WHERE mdm_cl_id = ? AND mdm_ml_id = ?"
					+ " AND TRIM(LOWER(mdm_bt_body_type)) = ? AND mdm_bt_flag NOT IN (?)");
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getBodyCode()));
			pstmt.setString(4, ApplicationProperties.getProperty("flag.value.delete"));
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				return rs.getLong(1);
			}
			return 0;
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
	}

	/**
	 * EXCEL IMPORT - CREATE OR UPDATE ONE ROW (Action A / U / blank).
	 *
	 * The row is looked up with the same criteria as findExistingIdForImport.
	 *  - FOUND: the code and both descriptions are updated. The STATUS IS KEPT - an import never
	 *    activates or drafts a row. The sync status follows the screen's rule for an unchanged
	 *    status: an ACTIVE row whose code / descriptions changed is set to N so it is synchronised
	 *    again; otherwise it is left as it is.
	 *  - NOT FOUND: inserted exactly as the screen's Entry inserts it (status from details = Draft).
	 *
	 * Uses the caller's connection; opens (and closes) its own only when none is passed.
	 */
	public static boolean importBodyTypeDetails(BodyTypeDetails details, Connection conn)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean ownConnection = false;
		try
		{
			details.setBodyCode(ImportActionUtils.safeTrim(details.getBodyCode()));
			details.setBodyCodeDescription(ImportActionUtils.safeTrim(details.getBodyCodeDescription()));
			details.setBodyCodeDescriptionRegional(ImportActionUtils.safeTrim(details.getBodyCodeDescriptionRegional()));
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
				ownConnection = true;
			}

			long existingId = 0;
			String oldValues = "";
			String oldFlag = "";
			String syncStatus = null;
			pstmt = conn.prepareStatement("SELECT mdm_bt_id, mdm_bt_body_type, mdm_bt_body_type_desc_eng, mdm_bt_body_type_desc_reg,"
					+ " mdm_bt_flag, mdm_sync_status FROM gms3_mdm_body_type WHERE mdm_cl_id = ? AND mdm_ml_id = ?"
					+ " AND TRIM(LOWER(mdm_bt_body_type)) = ? AND mdm_bt_flag NOT IN (?)");
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getBodyCode().toLowerCase());
			pstmt.setString(4, ApplicationProperties.getProperty("flag.value.delete"));
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				existingId = rs.getLong(1);
				oldValues = ImportActionUtils.safeTrim(rs.getString(2)) + "|" + ImportActionUtils.safeTrim(rs.getString(3))
						+ "|" + ImportActionUtils.safeTrim(rs.getString(4));
				oldFlag = ImportActionUtils.safeTrim(rs.getString(5));
				syncStatus = rs.getString(6);
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;

			if(existingId>0)
			{
				String newValues = details.getBodyCode() + "|" + details.getBodyCodeDescription() + "|" + details.getBodyCodeDescriptionRegional();
				if(oldFlag.equals(ApplicationProperties.getProperty("flag.value.active"))
						&& !oldValues.toLowerCase().equals(newValues.toLowerCase()))
				{
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				pstmt = conn.prepareStatement("UPDATE gms3_mdm_body_type SET mdm_bt_body_type = ?, mdm_bt_body_type_desc_eng = ?,"
						+ " mdm_bt_body_type_desc_reg = ?, mdm_bt_updated_tmstp = ?, mdm_sync_status = ? WHERE mdm_bt_id = ?");
				pstmt.setString(1, details.getBodyCode());
				pstmt.setString(2, details.getBodyCodeDescription());
				pstmt.setString(3, details.getBodyCodeDescriptionRegional());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(5, syncStatus);
				pstmt.setLong(6, existingId);
				pstmt.executeUpdate();
			}
			else
			{
				pstmt = conn.prepareStatement("INSERT INTO gms3_mdm_body_type(mdm_bt_body_type,mdm_bt_body_type_desc_eng,"
						+ "mdm_bt_flag,mdm_bt_created_tmstp,mdm_cl_id,mdm_ml_id,mdm_bt_body_type_desc_reg) VALUES(?,?,?,?,?,?,?)");
				pstmt.setString(1, details.getBodyCode());
				pstmt.setString(2, details.getBodyCodeDescription());
				pstmt.setString(3, details.getFlag().trim());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(5, details.getCountryLocaleId());
				pstmt.setLong(6, details.getManualLanguageId());
				pstmt.setString(7, details.getBodyCodeDescriptionRegional());
				pstmt.executeUpdate();
			}
			return true;
		}
		catch(Exception e)
		{
			logger.info("importBodyTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "importBodyTypeDetails()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
				if(ownConnection && null!=conn)
					conn.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(BodyTypeDAO.class.getName(), "importBodyTypeDetails()", e);
			}
		}
	}
}
