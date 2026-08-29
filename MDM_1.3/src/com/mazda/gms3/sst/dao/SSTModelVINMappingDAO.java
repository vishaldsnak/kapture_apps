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
import com.mazda.gms3.sst.vo.SSTModelVINMappingDetails;

public class SSTModelVINMappingDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SSTModelVINMappingDAO.class);
	
	public static ArrayList<SSTModelVINMappingDetails> getSSTModelVINMappingDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getSSTModelVINMappingDetailsList :: Method Starts.");
		ArrayList<SSTModelVINMappingDetails> mappingList = new ArrayList<SSTModelVINMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_model_vin_mapping WHERE "
						+ " mdm_mvin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getSSTModelVINMappingDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTModelVINMappingDetails details = new SSTModelVINMappingDetails();
					details.setSrNo(mappingList.size()+1);
					details.setVinModelMappingId(rs.getLong("mdm_mvin_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_crln_code") && !"".equals(rs.getString("mdm_crln_code")))
					{
						details.setModelCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name") && !"".equals(rs.getString("mdm_crln_name")))
					{
						details.setModelName(rs.getString("mdm_crln_name").trim());
					}
					if(null!=rs.getString("mdm_mvin_name") && !"".equals(rs.getString("mdm_mvin_name")))
					{
						details.setVinNumber(rs.getString("mdm_mvin_name").trim());
					}
					details.setFlag(rs.getString("mdm_mvin_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_mvin_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_mvin_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					mappingList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getSSTModelVINMappingDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTModelVINMappingDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "getSSTModelVINMappingDetailsList()", e);
			logger.info("getSSTModelVINMappingDetailsList :: ################ Exception ################");
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
//		logger.info("getSSTModelVINMappingDetailsList :: Method Ends.");
		return mappingList;
	}

	public static boolean saveSSTModelVINMappingDetails(SSTModelVINMappingDetails details) throws SQLException
	{
		logger.info("saveSSTModelVINMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getVinNumber() && !"".equals(details.getVinNumber()))
			{
				details.setVinNumber(details.getVinNumber().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_model_vin_mapping(mdm_cl_id,mdm_ml_id, mdm_crln_code,mdm_crln_name,  "
					+ " mdm_mvin_name, mdm_mvin_flag,"
					+ " mdm_mvin_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getModelCode());
			pstmt.setString(4, details.getModelName());
			pstmt.setString(5, details.getVinNumber());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveSSTModelVINMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "saveSSTModelVINMappingDetails()", e);
			logger.info("saveSSTModelVINMappingDetails :: ################ Exception ################");
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
		logger.info("saveSSTModelVINMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSSTModelVINMappingDetails(SSTModelVINMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSSTModelVINMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getVinNumber() && !"".equals(details.getVinNumber()))
			{
				details.setVinNumber(details.getVinNumber().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_model_vin_mapping SET  mdm_crln_code=?,mdm_crln_name =?, "
					+ "  mdm_mvin_name = ?,"
					+ " mdm_mvin_flag=?,mdm_mvin_updated_tmstp=?  WHERE mdm_mvin_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getModelCode());
			pstmt.setString(2, details.getModelName());
			pstmt.setString(3, details.getVinNumber());
			pstmt.setString(4, details.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(6, details.getVinModelMappingId());
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
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "updateSSTModelVINMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSSTModelVINMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSSTModelVINMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSSTModelVINMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_model_vin_mapping SET mdm_mvin_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_mvin_updated_tmstp = ?  WHERE mdm_mvin_id = "+ tokens[i].toString();
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
				logger.info("deleteSSTModelVINMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSSTModelVINMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "deleteSSTModelVINMappingDetails()", e);
			logger.info("deleteSSTModelVINMappingDetails :: ################ Exception ################");
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
		logger.info("deleteSSTModelVINMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSSTModelVINMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeSSTModelVINMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_model_vin_mapping SET mdm_mvin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_mvin_updated_tmstp = ?  WHERE mdm_mvin_id = "+ tokens[i].toString();
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
				logger.info("activeSSTModelVINMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSSTModelVINMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "activeSSTModelVINMappingDetails()", e);
			logger.info("activeSSTModelVINMappingDetails :: ################ Exception ################");
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
		logger.info("activeSSTModelVINMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSSTModelVINMappingDetails(SSTModelVINMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSSTModelVINMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getVinNumber() && !"".equals(details.getVinNumber()))
			{
				details.setVinNumber(details.getVinNumber().trim());
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
			String getVINModelMappingSql = "SELECT * FROM gms3_sst_model_vin_mapping WHERE "
					+ " mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(mdm_crln_code) = ? AND TRIM(LOWER(mdm_mvin_name)) = ?  "
					+ " AND mdm_mvin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTModelVINMappingDetails :: getVINModelMappingSql :: > " + getVINModelMappingSql);
			pstmt = conn.prepareStatement(getVINModelMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getModelCode().trim());
			pstmt.setString(4, details.getVinNumber().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoMappingId=0;
			if(rs.next())
			{
				autoMappingId= rs.getLong("mdm_mvin_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getVINModelMappingSql = null;
			
			if(autoMappingId>0)
			{
				pstmt = null;
				logger.info("importSSTModelVINMappingDetails :: VIN Model Mapping Already Exists. Update Row for Auto Mapping id : >" + autoMappingId);
				String sql="UPDATE gms3_sst_model_vin_mapping SET  mdm_crln_code=?, mdm_crln_name=?, mdm_mvin_name = ?,"
						+ " mdm_mvin_updated_tmstp=?  WHERE mdm_mvin_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getModelCode());
				pstmt.setString(2, details.getModelName());
				pstmt.setString(3, details.getVinNumber());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(5, autoMappingId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSSTModelVINMappingDetails :: VIN Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_model_vin_mapping(mdm_cl_id,mdm_ml_id, mdm_crln_code,mdm_crln_name,  "
						+ " mdm_mvin_name, mdm_mvin_flag,"
						+ " mdm_mvin_created_tmstp)"
						+ " VALUES(?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getModelCode());
				pstmt.setString(4, details.getModelName());
				pstmt.setString(5, details.getVinNumber());
				pstmt.setString(6, details.getFlag());
				pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
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
			Utilities.printStackTraceToLogs(SSTModelVINMappingDAO.class.getName(), "importSSTModelVINMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSSTModelVINMappingDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSSTModelVINMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SSTModelVINMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getVINModelMappingSql = "SELECT * FROM gms3_sst_model_vin_mapping WHERE "
					+ " mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(mdm_crln_code) = ? AND TRIM(LOWER(mdm_mvin_name)) = ?  "
					+ " AND mdm_mvin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTModelVINMappingDetails :: getVINModelMappingSql :: > " + getVINModelMappingSql);
			pstmt = conn.prepareStatement(getVINModelMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeTrim(details.getModelCode()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getVinNumber()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_mvin_id");
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
