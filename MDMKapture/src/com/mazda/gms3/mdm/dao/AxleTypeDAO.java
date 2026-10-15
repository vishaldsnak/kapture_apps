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
import com.mazda.gms3.mdm.vo.AxleTypeDetails;

public class AxleTypeDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(AxleTypeDAO.class);
	
	public static ArrayList<AxleTypeDetails> getAllAxleTypesList(String langCode) throws SQLException 
	{
//		logger.info("getAllAxleTypesList :: Method Starts.");
		ArrayList<AxleTypeDetails> axleTypeList = new ArrayList<AxleTypeDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_axle_type A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND A.mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND A.mdm_at_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getAllAxleTypesList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					AxleTypeDetails aDetails = new AxleTypeDetails();
					aDetails.setSrNo(axleTypeList.size()+1);
					aDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					aDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					aDetails.setAxleTypeId(rs.getLong("mdm_at_id"));
					if(null!=rs.getString("mdm_at_axle_type") && !"".equals(rs.getString("mdm_at_axle_type")))
					{
						aDetails.setAxleCode(rs.getString("mdm_at_axle_type").trim());
						aDetails.setOldAxleCode(rs.getString("mdm_at_axle_type").trim());
					}
					if(null!=rs.getString("mdm_at_axle_type_desc_eng") && !"".equals(rs.getString("mdm_at_axle_type_desc_eng")))
					{
						aDetails.setAxleCodeDescription(rs.getString("mdm_at_axle_type_desc_eng").trim());
						aDetails.setOldAxleCodeDescription(rs.getString("mdm_at_axle_type_desc_eng").trim());
					}
					if(null!=rs.getString("mdm_at_axle_type_desc_reg") && !"".equals(rs.getString("mdm_at_axle_type_desc_reg")))
					{
						aDetails.setAxleCodeDescriptionRegional(rs.getString("mdm_at_axle_type_desc_reg").trim());
						aDetails.setOldAxleCodeDescriptionRegional(rs.getString("mdm_at_axle_type_desc_reg").trim());
					}
					aDetails.setFlag(rs.getString("mdm_at_flag"));
					aDetails.setOldFlag(rs.getString("mdm_at_flag"));
					aDetails.setEntryTime(rs.getTimestamp("mdm_at_created_tmstp"));
					aDetails.setUpdatedTime(rs.getTimestamp("mdm_at_updated_tmstp"));
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						aDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					if(null!=aDetails.getFlag() && !"".equals(aDetails.getFlag()) && 
							aDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						aDetails.setShowCheckBox(false);
					}
					axleTypeList.add(aDetails);
					aDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getAllAxleTypesList :: Language Code Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getAllAxleTypesList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "getAllAxleTypesList()", e);
			logger.info("getAllAxleTypesList :: ################ Exception ################");
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
//		logger.info("getAllAxleTypesList :: Method Ends.");
		return axleTypeList;
	}
	
	public static boolean saveAxleTypeDetails(AxleTypeDetails aDetails) throws SQLException
	{
		logger.info("saveAxleTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=aDetails.getAxleCode() && !"".equals(aDetails.getAxleCode()))
			{
				aDetails.setAxleCode(aDetails.getAxleCode().trim());
			}
			if(null!=aDetails.getAxleCodeDescription() && !"".equals(aDetails.getAxleCodeDescription()))
			{
				aDetails.setAxleCodeDescription(aDetails.getAxleCodeDescription().trim());
			}
			if(null!=aDetails.getAxleCodeDescriptionRegional() && !"".equals(aDetails.getAxleCodeDescriptionRegional()))
			{
				aDetails.setAxleCodeDescriptionRegional(aDetails.getAxleCodeDescriptionRegional().trim());
			}
			
			
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_axle_type(mdm_at_axle_type,mdm_at_axle_type_desc_eng,"
					+ "mdm_at_flag,mdm_at_created_tmstp,mdm_cl_id,mdm_ml_id,mdm_at_axle_type_desc_reg) VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, aDetails.getAxleCode());
			pstmt.setString(2, aDetails.getAxleCodeDescription());
			pstmt.setString(3, aDetails.getFlag().trim());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, aDetails.getCountryLocaleId());
			pstmt.setLong(6, aDetails.getManualLanguageId());
			pstmt.setString(7, aDetails.getAxleCodeDescriptionRegional());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveAxleTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "saveAxleTypeDetails()", e);
			logger.info("saveAxleTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			aDetails = null;
		}
		logger.info("saveAxleTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean updateAxleTypeDetails(AxleTypeDetails aDetails) throws SQLException
	{
		logger.info("updateAxleTypeDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=aDetails.getAxleCode() && !"".equals(aDetails.getAxleCode()))
			{
				aDetails.setAxleCode(aDetails.getAxleCode().trim());
			}
			if(null!=aDetails.getAxleCodeDescription() && !"".equals(aDetails.getAxleCodeDescription()))
			{
				aDetails.setAxleCodeDescription(aDetails.getAxleCodeDescription().trim());
			}
			if(null!=aDetails.getAxleCodeDescriptionRegional() && !"".equals(aDetails.getAxleCodeDescriptionRegional()))
			{
				aDetails.setAxleCodeDescriptionRegional(aDetails.getAxleCodeDescriptionRegional().trim());
			}
			
			if(null!=aDetails.getOldAxleCode() && !"".equals(aDetails.getOldAxleCode()))
			{
				aDetails.setOldAxleCode(aDetails.getOldAxleCode().trim());
			}
			if(null!=aDetails.getOldAxleCodeDescription() && !"".equals(aDetails.getOldAxleCodeDescription()))
			{
				aDetails.setOldAxleCodeDescription(aDetails.getOldAxleCodeDescription().trim());
			}
			if(null!=aDetails.getOldAxleCodeDescriptionRegional() && !"".equals(aDetails.getOldAxleCodeDescriptionRegional()))
			{
				aDetails.setOldAxleCodeDescriptionRegional(aDetails.getOldAxleCodeDescriptionRegional().trim());
			}
			
			if(null!=aDetails.getSyncStatus() && !"".equals(aDetails.getSyncStatus()))
			{
				aDetails.setSyncStatus(aDetails.getSyncStatus().trim());
			}
			
			String syncStatus = aDetails.getSyncStatus();
			/*
			 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
				If PreviousStatus and CurrentStatus is Same and Status is Active - 
					if AXLE CODE / NAME / REGIONAL NAME , then update syncStatus as N

				If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
				If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
			 */
			if(aDetails.getOldFlag().equals(aDetails.getFlag()))
			{
				// STATUS IS SAME - check only for active
				if(aDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					String keyToCheck=aDetails.getOldAxleCode()+aDetails.getOldAxleCodeDescription();
					if(null!=aDetails.getOldAxleCodeDescriptionRegional() && !"".equals(aDetails.getOldAxleCodeDescriptionRegional()))
					{
						keyToCheck+=aDetails.getOldAxleCodeDescriptionRegional();
					}
					String existData=aDetails.getAxleCode()+aDetails.getAxleCodeDescription();
					if(null!=aDetails.getAxleCodeDescriptionRegional() && !"".equals(aDetails.getAxleCodeDescriptionRegional()))
					{
						existData+=aDetails.getAxleCodeDescriptionRegional();
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
				if(aDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
						aDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				else if(aDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
						aDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
				{
					// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
					syncStatus = null;
				}
			}
			
			
			
			
			
			conn = getConnection();
			String sql="UPDATE gms3_mdm_axle_type SET mdm_at_axle_type=?,mdm_at_axle_type_desc_eng =?,"
					+ "mdm_at_flag=?,mdm_at_updated_tmstp=?, mdm_at_axle_type_desc_reg =?,mdm_sync_status = ? WHERE mdm_at_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, aDetails.getAxleCode());
			pstmt.setString(2, aDetails.getAxleCodeDescription());
			pstmt.setString(3, aDetails.getFlag().trim());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(5, aDetails.getAxleCodeDescriptionRegional());
			pstmt.setString(6, syncStatus);
			pstmt.setLong(7, aDetails.getAxleTypeId());
			pstmt.executeUpdate();
			sql = null;
			syncStatus=null;
		}
		catch(Exception e)
		{
			logger.info("updateAxleTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "updateAxleTypeDetails()", e);
			logger.info("updateAxleTypeDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			aDetails=  null;
		}
		logger.info("updateAxleTypeDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteAxleDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteAxleDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_axle_type SET mdm_at_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " , mdm_at_updated_tmstp = ?,mdm_sync_status=?  WHERE mdm_at_id = "+ tokens[i].toString();
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
				logger.info("deleteAxleDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteAxleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "deleteAxleDetails()", e);
			logger.info("deleteAxleDetails :: ################ Exception ################");
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
		logger.info("deleteAxleDetails :: Method Ends.");
		return true;
	}
	
	public static boolean activeAxleDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeAxleDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_axle_type SET mdm_at_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " , mdm_at_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_at_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					
					dataMap=  null;
					syncStatus=  null;
				}
				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("activeAxleDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeAxleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "activeAxleDetails()", e);
			logger.info("activeAxleDetails :: ################ Exception ################");
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
		logger.info("activeAxleDetails :: Method Ends.");
		return true;
	}


	/**
	 * EXCEL IMPORT LOOKUP - the id of the ACTIVE row an import line refers to, or 0 when there is
	 * none. Unique criteria: Country Locale + Manual Language + Code, case- and space-insensitive -
	 * the same rule the screen's own Entry / Update enforces. Deleted rows are never matched, so an
	 * add / update of a code that was deleted earlier inserts a new row.
	 */
	public static long findExistingIdForImport(AxleTypeDetails details, Connection conn) throws Exception
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT mdm_at_id FROM gms3_mdm_axle_type WHERE mdm_cl_id = ? AND mdm_ml_id = ?"
					+ " AND TRIM(LOWER(mdm_at_axle_type)) = ? AND mdm_at_flag NOT IN (?)");
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getAxleCode()));
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
	public static boolean importAxleTypeDetails(AxleTypeDetails details, Connection conn)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean ownConnection = false;
		try
		{
			details.setAxleCode(ImportActionUtils.safeTrim(details.getAxleCode()));
			details.setAxleCodeDescription(ImportActionUtils.safeTrim(details.getAxleCodeDescription()));
			details.setAxleCodeDescriptionRegional(ImportActionUtils.safeTrim(details.getAxleCodeDescriptionRegional()));
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
				ownConnection = true;
			}

			long existingId = 0;
			String oldValues = "";
			String oldFlag = "";
			String syncStatus = null;
			pstmt = conn.prepareStatement("SELECT mdm_at_id, mdm_at_axle_type, mdm_at_axle_type_desc_eng, mdm_at_axle_type_desc_reg,"
					+ " mdm_at_flag, mdm_sync_status FROM gms3_mdm_axle_type WHERE mdm_cl_id = ? AND mdm_ml_id = ?"
					+ " AND TRIM(LOWER(mdm_at_axle_type)) = ? AND mdm_at_flag NOT IN (?)");
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getAxleCode().toLowerCase());
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
				String newValues = details.getAxleCode() + "|" + details.getAxleCodeDescription() + "|" + details.getAxleCodeDescriptionRegional();
				if(oldFlag.equals(ApplicationProperties.getProperty("flag.value.active"))
						&& !oldValues.toLowerCase().equals(newValues.toLowerCase()))
				{
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				pstmt = conn.prepareStatement("UPDATE gms3_mdm_axle_type SET mdm_at_axle_type = ?, mdm_at_axle_type_desc_eng = ?,"
						+ " mdm_at_axle_type_desc_reg = ?, mdm_at_updated_tmstp = ?, mdm_sync_status = ? WHERE mdm_at_id = ?");
				pstmt.setString(1, details.getAxleCode());
				pstmt.setString(2, details.getAxleCodeDescription());
				pstmt.setString(3, details.getAxleCodeDescriptionRegional());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(5, syncStatus);
				pstmt.setLong(6, existingId);
				pstmt.executeUpdate();
			}
			else
			{
				pstmt = conn.prepareStatement("INSERT INTO gms3_mdm_axle_type(mdm_at_axle_type,mdm_at_axle_type_desc_eng,"
						+ "mdm_at_flag,mdm_at_created_tmstp,mdm_cl_id,mdm_ml_id,mdm_at_axle_type_desc_reg) VALUES(?,?,?,?,?,?,?)");
				pstmt.setString(1, details.getAxleCode());
				pstmt.setString(2, details.getAxleCodeDescription());
				pstmt.setString(3, details.getFlag().trim());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(5, details.getCountryLocaleId());
				pstmt.setLong(6, details.getManualLanguageId());
				pstmt.setString(7, details.getAxleCodeDescriptionRegional());
				pstmt.executeUpdate();
			}
			return true;
		}
		catch(Exception e)
		{
			logger.info("importAxleTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "importAxleTypeDetails()", e);
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
				Utilities.printStackTraceToLogs(AxleTypeDAO.class.getName(), "importAxleTypeDetails()", e);
			}
		}
	}
}
