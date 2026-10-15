package com.mazda.gms3.sst.dao;

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
import com.mazda.gms3.sst.vo.AbbreviationDetails;

public class AbbreviationDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(AbbreviationDAO.class);
	
	public static ArrayList<AbbreviationDetails> getAbbreviationDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getAbbreviationDetailsList :: Method Starts.");
		ArrayList<AbbreviationDetails> vinList = new ArrayList<AbbreviationDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_abbrv_master WHERE mdm_abbrv_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getAbbreviationDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					AbbreviationDetails abbrvDetails = new AbbreviationDetails();
					abbrvDetails.setSrNo(vinList.size()+1);
					abbrvDetails.setAbbreviationMasterId(rs.getLong("mdm_abbrv_id"));
					abbrvDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					abbrvDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_abbrv_code") && !"".equals(rs.getString("mdm_abbrv_code")))
					{
						abbrvDetails.setAbbreviationCode(rs.getString("mdm_abbrv_code").trim());
					}
					if(null!=rs.getString("mdm_abbrv_name") && !"".equals(rs.getString("mdm_abbrv_name")))
					{
						abbrvDetails.setAbbreviationName(rs.getString("mdm_abbrv_name").trim());
					}
					abbrvDetails.setFlag(rs.getString("mdm_abbrv_flag"));
					abbrvDetails.setEntryTime(rs.getTimestamp("mdm_abbrv_created_tmstp"));
					abbrvDetails.setUpdatedTime(rs.getTimestamp("mdm_abbrv_updated_tmstp"));
					if(null!=abbrvDetails.getFlag() && !"".equals(abbrvDetails.getFlag()) && 
							abbrvDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						abbrvDetails.setShowCheckBox(false);
					}
					vinList.add(abbrvDetails);
					abbrvDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getAbbreviationDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getAbbreviationDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "getAbbreviationDetailsList()", e);
			logger.info("getAbbreviationDetailsList :: ################ Exception ################");
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
//		logger.info("getAbbreviationDetailsList :: Method Ends.");
		return vinList;
	}

	public static boolean saveAbbreviationDetails(AbbreviationDetails abbrvDetails) throws SQLException
	{
		logger.info("saveAbbreviationDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=abbrvDetails.getAbbreviationCode() && !"".equals(abbrvDetails.getAbbreviationCode()))
			{
				abbrvDetails.setAbbreviationCode(abbrvDetails.getAbbreviationCode().trim());
			}
			if(null!=abbrvDetails.getAbbreviationName() && !"".equals(abbrvDetails.getAbbreviationName()))
			{
				abbrvDetails.setAbbreviationName(abbrvDetails.getAbbreviationName().trim());
			}
			
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_abbrv_master(mdm_cl_id,mdm_ml_id,mdm_abbrv_code, mdm_abbrv_name, mdm_abbrv_flag,"
					+ " mdm_abbrv_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, abbrvDetails.getCountryLocaleId());
			pstmt.setLong(2, abbrvDetails.getManualLanguageId());
			pstmt.setString(3, abbrvDetails.getAbbreviationCode());
			pstmt.setString(4, abbrvDetails.getAbbreviationName());
			pstmt.setString(5, abbrvDetails.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveAbbreviationDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "saveAbbreviationDetails()", e);
			logger.info("saveAbbreviationDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			abbrvDetails = null;
		}
		logger.info("saveAbbreviationDetails :: Method Ends.");
		return true;
	}

	public static boolean updateAbbreviationDetails(AbbreviationDetails abbrvDetails,Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateAbbreviationDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=abbrvDetails.getAbbreviationCode() && !"".equals(abbrvDetails.getAbbreviationCode()))
			{
				abbrvDetails.setAbbreviationCode(abbrvDetails.getAbbreviationCode().trim());
			}
			if(null!=abbrvDetails.getAbbreviationName() && !"".equals(abbrvDetails.getAbbreviationName()))
			{
				abbrvDetails.setAbbreviationName(abbrvDetails.getAbbreviationName().trim());
			}
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_abbrv_master SET mdm_abbrv_code =?,mdm_abbrv_name =?,"
					+ " mdm_abbrv_flag=?,mdm_abbrv_updated_tmstp=?  WHERE mdm_abbrv_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, abbrvDetails.getAbbreviationCode());
			pstmt.setString(2, abbrvDetails.getAbbreviationName());
			pstmt.setString(3, abbrvDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, abbrvDetails.getAbbreviationMasterId());
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
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "updateAbbreviationDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			abbrvDetails=  null;
		}
		logger.info("updateAbbreviationDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteAbbreviationDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteAbbreviationDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_abbrv_master SET mdm_abbrv_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_abbrv_updated_tmstp = ?  WHERE mdm_abbrv_id = "+ tokens[i].toString();
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
				logger.info("deleteAbbreviationDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteAbbreviationDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "deleteAbbreviationDetails()", e);
			logger.info("deleteAbbreviationDetails :: ################ Exception ################");
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
		logger.info("deleteAbbreviationDetails :: Method Ends.");
		return true;
	}

	public static boolean activeAbbreviationDetails(String activeIds) throws SQLException
	{
		logger.info("activeAbbreviationDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_abbrv_master SET mdm_abbrv_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_abbrv_updated_tmstp = ?  WHERE mdm_abbrv_id = "+ tokens[i].toString();
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
				logger.info("activeAbbreviationDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeAbbreviationDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "activeAbbreviationDetails()", e);
			logger.info("activeAbbreviationDetails :: ################ Exception ################");
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
		logger.info("activeAbbreviationDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importAbbreviationDetails(AbbreviationDetails abbrvDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importAbbreviationDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=abbrvDetails.getAbbreviationCode() && !"".equals(abbrvDetails.getAbbreviationCode()))
			{
				abbrvDetails.setAbbreviationCode(abbrvDetails.getAbbreviationCode().trim());
			}
			if(null!=abbrvDetails.getAbbreviationName() && !"".equals(abbrvDetails.getAbbreviationName()))
			{
				abbrvDetails.setAbbreviationName(abbrvDetails.getAbbreviationName().trim());
			}
			if(null==conn || conn.isClosed()==true)
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
			String getAbbreviationSql = "SELECT * FROM gms3_sst_abbrv_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_abbrv_code))=?  "
					+ " AND mdm_abbrv_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importAbbreviationDetails :: getAbbreviationSql :: > " + getAbbreviationSql);
			pstmt = conn.prepareStatement(getAbbreviationSql);
			pstmt.setLong(1, abbrvDetails.getCountryLocaleId());
			pstmt.setLong(2, abbrvDetails.getManualLanguageId());
			pstmt.setString(3, abbrvDetails.getAbbreviationCode().toLowerCase());
			rs = pstmt.executeQuery();
			long autoAbbreviationId=0;
			if(rs.next())
			{
				autoAbbreviationId= rs.getLong("mdm_abbrv_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getAbbreviationSql = null;
			
			if(autoAbbreviationId>0)
			{
				pstmt = null;
				logger.info("importAbbreviationDetails :: Abbreviation Already Exists. Update Row for Auto Abbreviation id : >" + autoAbbreviationId);
				
				String sql="UPDATE gms3_sst_abbrv_master SET mdm_abbrv_code =?,mdm_abbrv_name =?,"
						+ " mdm_abbrv_updated_tmstp=? WHERE mdm_abbrv_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, abbrvDetails.getAbbreviationCode());
				pstmt.setString(2, abbrvDetails.getAbbreviationName());
				pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(4, autoAbbreviationId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importAbbreviationDetails :: Abbreviation Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_abbrv_master(mdm_cl_id,mdm_ml_id,mdm_abbrv_code, mdm_abbrv_name, "
						+ " mdm_abbrv_flag,mdm_abbrv_created_tmstp) VALUES(?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, abbrvDetails.getCountryLocaleId());
				pstmt.setLong(2, abbrvDetails.getManualLanguageId());
				pstmt.setString(3, abbrvDetails.getAbbreviationCode());
				pstmt.setString(4, abbrvDetails.getAbbreviationName());
				pstmt.setString(5, abbrvDetails.getFlag());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
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
			Utilities.printStackTraceToLogs(AbbreviationDAO.class.getName(), "importAbbreviationDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			abbrvDetails = null;
		}
		logger.info("importAbbreviationDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importAbbreviationDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(AbbreviationDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getAbbreviationSql = "SELECT * FROM gms3_sst_abbrv_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_abbrv_code))=?  "
					+ " AND mdm_abbrv_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importAbbreviationDetails :: getAbbreviationSql :: > " + getAbbreviationSql);
			pstmt = conn.prepareStatement(getAbbreviationSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getAbbreviationCode().toLowerCase());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_abbrv_id");
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
