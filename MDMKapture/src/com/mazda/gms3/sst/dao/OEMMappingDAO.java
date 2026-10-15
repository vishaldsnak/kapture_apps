package com.mazda.gms3.sst.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.sst.vo.OEMMappingDetails;

public class OEMMappingDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(OEMMappingDAO.class);
	
	public static ArrayList<OEMMappingDetails> getOEMMappingDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getOEMMappingDetailsList :: Method Starts.");
		ArrayList<OEMMappingDetails> vinList = new ArrayList<OEMMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_sst_number FROM gms3_sst_oem_mapping A, gms3_sst_master B WHERE "
						+ " A.mdm_sstoem_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') AND A.mdm_sst_id = B.mdm_sst_id ";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND A.mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getOEMMappingDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					OEMMappingDetails details = new OEMMappingDetails();
					details.setSrNo(vinList.size()+1);
					details.setOemMappingId(rs.getLong("mdm_sstoem_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					details.setSstId(rs.getLong("mdm_sst_id"));
					if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
					{
						details.setSstNumber(rs.getString("mdm_sst_number").trim());
					}
					if(null!=rs.getString("mdm_sstoem_ford_number") && !"".equals(rs.getString("mdm_sstoem_ford_number")))
					{
						details.setFordNumber(rs.getString("mdm_sstoem_ford_number").trim());
					}
					if(null!=rs.getString("mdm_sstoem_nissan_number") && !"".equals(rs.getString("mdm_sstoem_nissan_number")))
					{
						details.setNissanNumber(rs.getString("mdm_sstoem_nissan_number").trim());
					}
					if(null!=rs.getString("mdm_sstoem_isuzu_number") && !"".equals(rs.getString("mdm_sstoem_isuzu_number")))
					{
						details.setIsuzuNumber(rs.getString("mdm_sstoem_isuzu_number").trim());
					}
					if(null!=rs.getString("mdm_sstoem_suzuki_number") && !"".equals(rs.getString("mdm_sstoem_suzuki_number")))
					{
						details.setSuzukiNumber(rs.getString("mdm_sstoem_suzuki_number").trim());
					}
					details.setFlag(rs.getString("mdm_sstoem_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sstoem_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sstoem_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					vinList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getOEMMappingDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getOEMMappingDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "getOEMMappingDetailsList()", e);
			logger.info("getOEMMappingDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			languageId = null;
		}
//		logger.info("getOEMMappingDetailsList :: Method Ends.");
		return vinList;
	}

	public static boolean saveOEMMappingDetails(OEMMappingDetails details) throws SQLException
	{
		logger.info("saveOEMMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getFordNumber() && !"".equals(details.getFordNumber()))
			{
				details.setFordNumber(details.getFordNumber().trim());
			}
			if(null!=details.getNissanNumber() && !"".equals(details.getNissanNumber()))
			{
				details.setNissanNumber(details.getNissanNumber().trim());
			}
			if(null!=details.getIsuzuNumber() && !"".equals(details.getIsuzuNumber()))
			{
				details.setIsuzuNumber(details.getIsuzuNumber().trim());
			}
			if(null!=details.getSuzukiNumber() && !"".equals(details.getSuzukiNumber()))
			{
				details.setSuzukiNumber(details.getSuzukiNumber().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_oem_mapping(mdm_cl_id,mdm_ml_id,mdm_sst_id, mdm_sstoem_ford_number,mdm_sstoem_nissan_number,"
					+ " mdm_sstoem_isuzu_number, mdm_sstoem_suzuki_number,  mdm_sstoem_flag,"
					+ " mdm_sstoem_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSstId());
			pstmt.setString(4, details.getFordNumber());
			pstmt.setString(5, details.getNissanNumber());
			pstmt.setString(6, details.getIsuzuNumber());
			pstmt.setString(7, details.getSuzukiNumber());
			pstmt.setString(8, details.getFlag());
			pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveOEMMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "saveOEMMappingDetails()", e);
			logger.info("saveOEMMappingDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			details = null;
		}
		logger.info("saveOEMMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateOEMMappingDetails(OEMMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateOEMMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getFordNumber() && !"".equals(details.getFordNumber()))
			{
				details.setFordNumber(details.getFordNumber().trim());
			}
			if(null!=details.getNissanNumber() && !"".equals(details.getNissanNumber()))
			{
				details.setNissanNumber(details.getNissanNumber().trim());
			}
			if(null!=details.getIsuzuNumber() && !"".equals(details.getIsuzuNumber()))
			{
				details.setIsuzuNumber(details.getIsuzuNumber().trim());
			}
			if(null!=details.getSuzukiNumber() && !"".equals(details.getSuzukiNumber()))
			{
				details.setSuzukiNumber(details.getSuzukiNumber().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_oem_mapping SET mdm_sst_id =?,mdm_sstoem_ford_number =?,mdm_sstoem_nissan_number=?,"
					+ " mdm_sstoem_isuzu_number = ?, mdm_sstoem_suzuki_number = ? , "
					+ " mdm_sstoem_flag=?,mdm_sstoem_updated_tmstp=?  WHERE mdm_sstoem_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getSstId());
			pstmt.setString(2, details.getFordNumber());
			pstmt.setString(3, details.getNissanNumber());
			pstmt.setString(4, details.getIsuzuNumber());
			pstmt.setString(5, details.getSuzukiNumber());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(8, details.getOemMappingId());
			pstmt.executeUpdate();
			sql = null;
			if(null==closeConnection)
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "updateOEMMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateOEMMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteOEMMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteOEMMappingDetails :: Method Starts.");
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
						pstmt = null;
						String sql="UPDATE gms3_sst_oem_mapping SET mdm_sstoem_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sstoem_updated_tmstp = ?  WHERE mdm_sstoem_id = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
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
				logger.info("deleteOEMMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteOEMMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "deleteOEMMappingDetails()", e);
			logger.info("deleteOEMMappingDetails :: ################ Exception ################");
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
		logger.info("deleteOEMMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeOEMMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeOEMMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=activeIds && !"".equals(activeIds))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens=activeIds.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						pstmt = null;
						String sql="UPDATE gms3_sst_oem_mapping SET mdm_sstoem_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_sstoem_updated_tmstp = ?  WHERE mdm_sstoem_id = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
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
				logger.info("activeOEMMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeOEMMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "activeOEMMappingDetails()", e);
			logger.info("activeOEMMappingDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			activeIds=null;
		}
		logger.info("activeOEMMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importOEMMappingDetails(OEMMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importOEMMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getFordNumber() && !"".equals(details.getFordNumber()))
			{
				details.setFordNumber(details.getFordNumber().trim());
			}
			if(null!=details.getNissanNumber() && !"".equals(details.getNissanNumber()))
			{
				details.setNissanNumber(details.getNissanNumber().trim());
			}
			if(null!=details.getIsuzuNumber() && !"".equals(details.getIsuzuNumber()))
			{
				details.setIsuzuNumber(details.getIsuzuNumber().trim());
			}
			if(null!=details.getSuzukiNumber() && !"".equals(details.getSuzukiNumber()))
			{
				details.setSuzukiNumber(details.getSuzukiNumber().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			rs = null;
			pstmt= null;
			/*
			 * CHECK WHETHER VIN EXISTS OR NOT
			 * IF YES -  THEN UPDATE VIN
			 * ELSE - INSERT VIN
			 */
			String getOEMMappingSql = "SELECT * FROM gms3_sst_oem_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND mdm_sst_id = ? AND "
					+ " TRIM(LOWER(mdm_sstoem_ford_number))=?  AND TRIM(LOWER(mdm_sstoem_nissan_number)) = ?  "
					+ " AND TRIM(LOWER(mdm_sstoem_isuzu_number)) = ? AND TRIM(LOWER(mdm_sstoem_suzuki_number)) = ? "
					+ " AND mdm_sstoem_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importOEMMappingDetails :: getOEMMappingSql :: > " + getOEMMappingSql);
			pstmt = conn.prepareStatement(getOEMMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSstId());
			pstmt.setString(4, details.getFordNumber().trim().toLowerCase());
			pstmt.setString(5, details.getNissanNumber().trim().toLowerCase());
			pstmt.setString(6, details.getIsuzuNumber().trim().toLowerCase());
			pstmt.setString(7, details.getSuzukiNumber().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoOEMMappingId=0;
			if(rs.next())
			{
				autoOEMMappingId= rs.getLong("mdm_sstoem_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getOEMMappingSql = null;
			
			if(autoOEMMappingId>0)
			{
				pstmt = null;
				logger.info("importOEMMappingDetails :: OEMMapping Already Exists. Update Row for Auto OEMMapping id : >" + autoOEMMappingId);
				
				String sql="UPDATE gms3_sst_oem_mapping SET mdm_sst_id =?,mdm_sstoem_ford_number =?,mdm_sstoem_nissan_number=?,"
						+ " mdm_sstoem_isuzu_number = ?, mdm_sstoem_suzuki_number = ? , "
						+ " mdm_sstoem_updated_tmstp=?  WHERE mdm_sstoem_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getSstId());
				pstmt.setString(2, details.getFordNumber());
				pstmt.setString(3, details.getNissanNumber());
				pstmt.setString(4, details.getIsuzuNumber());
				pstmt.setString(5, details.getSuzukiNumber());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(7, autoOEMMappingId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importOEMMappingDetails :: OEMMapping Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_oem_mapping(mdm_cl_id,mdm_ml_id,mdm_sst_id, mdm_sstoem_ford_number,mdm_sstoem_nissan_number,"
						+ " mdm_sstoem_isuzu_number, mdm_sstoem_suzuki_number,  mdm_sstoem_flag,"
						+ " mdm_sstoem_created_tmstp)"
						+ " VALUES(?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setLong(3, details.getSstId());
				pstmt.setString(4, details.getFordNumber());
				pstmt.setString(5, details.getNissanNumber());
				pstmt.setString(6, details.getIsuzuNumber());
				pstmt.setString(7, details.getSuzukiNumber());
				pstmt.setString(8, details.getFlag());
				pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			if(null==closeConnection)
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(OEMMappingDAO.class.getName(), "importOEMMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importOEMMappingDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importOEMMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(OEMMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getOEMMappingSql = "SELECT * FROM gms3_sst_oem_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND mdm_sst_id = ? AND "
					+ " TRIM(LOWER(mdm_sstoem_ford_number))=?  AND TRIM(LOWER(mdm_sstoem_nissan_number)) = ?  "
					+ " AND TRIM(LOWER(mdm_sstoem_isuzu_number)) = ? AND TRIM(LOWER(mdm_sstoem_suzuki_number)) = ? "
					+ " AND mdm_sstoem_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importOEMMappingDetails :: getOEMMappingSql :: > " + getOEMMappingSql);
			pstmt = conn.prepareStatement(getOEMMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSstId());
			pstmt.setString(4, ImportActionUtils.safeLower(details.getFordNumber()));
			pstmt.setString(5, ImportActionUtils.safeLower(details.getNissanNumber()));
			pstmt.setString(6, ImportActionUtils.safeLower(details.getIsuzuNumber()));
			pstmt.setString(7, ImportActionUtils.safeLower(details.getSuzukiNumber()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sstoem_id");
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
