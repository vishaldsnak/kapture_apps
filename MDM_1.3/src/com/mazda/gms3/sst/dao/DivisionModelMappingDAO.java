package com.mazda.gms3.sst.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.sst.vo.DivisionModelMappingDetails;

public class DivisionModelMappingDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(DivisionModelMappingDAO.class);
	
	public static ArrayList<DivisionModelMappingDetails> getDivisionModelMappingDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getDivisionModelMappingDetailsList :: Method Starts.");
		ArrayList<DivisionModelMappingDetails> mappingList = new ArrayList<DivisionModelMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_div_name, B.mdm_div_code FROM gms3_sst_div_model_mapping A, gms3_sst_division_master B  WHERE "
						+ " A.mdm_divmm_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') AND A.mdm_div_id=B.mdm_div_id ";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND A.mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getDivisionModelMappingDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					DivisionModelMappingDetails details = new DivisionModelMappingDetails();
					details.setSrNo(mappingList.size()+1);
					details.setDivisionModelMappingId(rs.getLong("mdm_divmm_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					Integer sortId = rs.getInt("mdm_divmm_sort_id");
					if(null!=sortId)
					{
						details.setSortId(String.valueOf(rs.getInt("mdm_divmm_sort_id")));
					}
					details.setDivisionId(rs.getLong("mdm_div_id"));
					if(null!=rs.getString("mdm_div_name") && !"".equals(rs.getString("mdm_div_name")))
					{
						details.setDivisionName(rs.getString("mdm_div_name"));
					}
					if(null!=rs.getString("mdm_div_code") && !"".equals(rs.getString("mdm_div_code")))
					{
						details.setDivisionCode(rs.getString("mdm_div_code"));
					}
					if(null!=rs.getString("mdm_crln_code") && !"".equals(rs.getString("mdm_crln_code")))
					{
						details.setModelCode(rs.getString("mdm_crln_code"));
					}
					if(null!=rs.getString("mdm_crln_name") && !"".equals(rs.getString("mdm_crln_name")))
					{
						details.setModelName(rs.getString("mdm_crln_name"));
					}
					if(null!=rs.getString("mdm_esi_crln_code") && !"".equals(rs.getString("mdm_esi_crln_code")))
					{
						details.setEsiModelCode(rs.getString("mdm_esi_crln_code"));
					}
					details.setFlag(rs.getString("mdm_divmm_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_divmm_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_divmm_updated_tmstp"));
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
				logger.info("getDivisionModelMappingDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getDivisionModelMappingDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "getDivisionModelMappingDetailsList()", e);
			logger.info("getDivisionModelMappingDetailsList :: ################ Exception ################");
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
//		logger.info("getDivisionModelMappingDetailsList :: Method Ends.");
		return mappingList;
	}

	public static boolean saveDivisionModelMappingDetails(DivisionModelMappingDetails details) throws SQLException
	{
		logger.info("saveDivisionModelMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getEsiModelCode() && !"".equals(details.getEsiModelCode()))
			{
				details.setEsiModelCode(details.getEsiModelCode().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_div_model_mapping(mdm_cl_id,mdm_ml_id,mdm_div_id,"
					+ " mdm_crln_code, mdm_crln_name, mdm_esi_crln_code, "
					+ " mdm_divmm_sort_id,  mdm_divmm_flag,"
					+ " mdm_divmm_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getDivisionId());
			pstmt.setString(4, details.getModelCode());
			pstmt.setString(5, details.getModelName());
			pstmt.setString(6, details.getEsiModelCode());
			if(null!=sortId)
			{
				pstmt.setInt(7, sortId);
			}
			else
			{
				pstmt.setNull(7, Types.INTEGER);
			}
			pstmt.setString(8, details.getFlag());
			pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
			sortId = null;
		}
		catch(Exception e)
		{
			logger.info("saveDivisionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "saveDivisionModelMappingDetails()", e);
			logger.info("saveDivisionModelMappingDetails :: ################ Exception ################");
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
		logger.info("saveDivisionModelMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateDivisionModelMappingDetails(DivisionModelMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateDivisionModelMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getEsiModelCode() && !"".equals(details.getEsiModelCode()))
			{
				details.setEsiModelCode(details.getEsiModelCode().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_div_model_mapping SET mdm_div_id=?, mdm_crln_code=?,mdm_crln_name=?, mdm_esi_crln_code = ?, "
					+ " mdm_divmm_sort_id =?, "
					+ " mdm_divmm_flag=?,mdm_divmm_updated_tmstp=?  WHERE mdm_divmm_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getDivisionId());
			pstmt.setString(2, details.getModelCode());
			pstmt.setString(3, details.getModelName());
			pstmt.setString(4, details.getEsiModelCode());
			if(null!=sortId)
			{
				pstmt.setInt(5, sortId);
			}
			else
			{
				pstmt.setNull(5, Types.INTEGER);
			}
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(8, details.getDivisionModelMappingId());
			pstmt.executeUpdate();
			sql = null;
			sortId=  null;
			
			/*
			 * also UPDATE - MODEL NAMES IN GMS3_SST_MODEL_VIN_MAPPING
			 * AND GMS3_SST_SEC_MODEL_MAPPING ON THE BASIS OF CARLINE CODE AND LANGUAGE ID
			 * 
			 * TO DO
			 * 
			 */
			pstmt.close();
			pstmt = null;
			String updateVINSql="UPDATE gms3_sst_model_vin_mapping SET mdm_crln_name=? WHERE TRIM(LOWER(mdm_crln_code)) =? AND mdm_ml_id = ?";
			pstmt = conn.prepareStatement(updateVINSql);
			pstmt.setString(1, details.getModelName());
			pstmt.setString(2, details.getModelCode().trim().toLowerCase());
			pstmt.setLong(3, details.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();
			pstmt = null;
			updateVINSql = null;
			
			String updateSecModelSql="UPDATE gms3_sst_sec_model_mapping SET mdm_crln_name=? WHERE TRIM(LOWER(mdm_crln_code)) =? AND mdm_ml_id = ?";
			pstmt = conn.prepareStatement(updateSecModelSql);
			pstmt.setString(1, details.getModelName());
			pstmt.setString(2, details.getModelCode().trim().toLowerCase());
			pstmt.setLong(3, details.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();
			pstmt = null;
			updateSecModelSql = null;
			
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
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "updateDivisionModelMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateDivisionModelMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteDivisionModelMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteDivisionModelMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_div_model_mapping SET mdm_divmm_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_divmm_updated_tmstp = ?  WHERE mdm_divmm_id = "+ tokens[i].toString();
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
				logger.info("deleteDivisionModelMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteDivisionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "deleteDivisionModelMappingDetails()", e);
			logger.info("deleteDivisionModelMappingDetails :: ################ Exception ################");
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
		logger.info("deleteDivisionModelMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeDivisionModelMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeDivisionModelMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_div_model_mapping SET mdm_divmm_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_divmm_updated_tmstp = ?  WHERE mdm_divmm_id = "+ tokens[i].toString();
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
				logger.info("activeDivisionModelMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeDivisionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "activeDivisionModelMappingDetails()", e);
			logger.info("activeDivisionModelMappingDetails :: ################ Exception ################");
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
		logger.info("activeDivisionModelMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importDivisionModelMappingDetails(DivisionModelMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importDivisionModelMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
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
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				details.setModelCode(details.getModelCode().trim());
			}
			if(null!=details.getModelName() && !"".equals(details.getModelName()))
			{
				details.setModelName(details.getModelName().trim());
			}
			if(null!=details.getEsiModelCode() && !"".equals(details.getEsiModelCode()))
			{
				details.setEsiModelCode(details.getEsiModelCode().trim());
			}
			
			/*
			 * CHECK HERE, IF ANY EXISTING MODEL CODE 
			 * INSIDE A DIVISION, LANGUAGE & COUNTRY UPDATE IT
			 */
			
			String getDivisionMappingSql = "SELECT * FROM gms3_sst_div_model_mapping WHERE mdm_cl_id = ? AND "
					+ " mdm_ml_id=? AND "
					+ " mdm_div_id=? AND TRIM(LOWER(mdm_esi_crln_code)) =? AND TRIM(LOWER(mdm_crln_code)) = ? ";
			getDivisionMappingSql  =getDivisionMappingSql	+" AND mdm_divmm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importDivisionModelMappingDetails :: getDivisionMappingSql :: > " + getDivisionMappingSql);
			pstmt = conn.prepareStatement(getDivisionMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getDivisionId());
			pstmt.setString(4, details.getEsiModelCode().trim().toLowerCase());
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				pstmt.setString(5, details.getModelCode().trim().toLowerCase());
			}
			else
			{
				details.setModelCode(null);
				pstmt.setString(5, details.getModelCode());
			}
			rs = pstmt.executeQuery();
			long autoDivisionId=0;
			if(rs.next())
			{
				autoDivisionId= rs.getLong("mdm_divmm_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getDivisionMappingSql = null;
			
			if(autoDivisionId>0)
			{
				pstmt = null;
				logger.info("importDivisionModelMappingDetails :: Division Model Mapping Already Exists. Update Row for Auto Mapping id : >" + autoDivisionId);
				
				String sql="UPDATE gms3_sst_div_model_mapping SET mdm_div_id=?, mdm_crln_code=?,mdm_crln_name=?, mdm_esi_crln_code = ?, "
						+ " mdm_divmm_sort_id =?, "
						+ " mdm_divmm_updated_tmstp=?  WHERE mdm_divmm_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getDivisionId());
				pstmt.setString(2, details.getModelCode());
				pstmt.setString(3, details.getModelName());
				pstmt.setString(4, details.getEsiModelCode());
				if(null!=sortId)
				{
					pstmt.setInt(5, sortId);
				}
				else
				{
					pstmt.setNull(5, Types.INTEGER);
				}
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(7, autoDivisionId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
				
				/*
				 * also UPDATE - MODEL NAMES IN GMS3_SST_MODEL_VIN_MAPPING
				 * AND GMS3_SST_SEC_MODEL_MAPPING ON THE BASIS OF CARLINE CODE AND LANGUAGE ID
				 * 
				 * TO DO
				 * 
				 */
				String updateVINSql="UPDATE gms3_sst_model_vin_mapping SET mdm_crln_name=? WHERE TRIM(LOWER(mdm_crln_code)) =? AND mdm_ml_id = ?";
				pstmt = conn.prepareStatement(updateVINSql);
				pstmt.setString(1, details.getModelName());
				pstmt.setString(2, details.getModelCode().trim().toLowerCase());
				pstmt.setLong(3, details.getManualLanguageId());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateVINSql = null;
				
				String updateSecModelSql="UPDATE gms3_sst_sec_model_mapping SET mdm_crln_name=? WHERE TRIM(LOWER(mdm_crln_code)) =? AND mdm_ml_id = ?";
				pstmt = conn.prepareStatement(updateSecModelSql);
				pstmt.setString(1, details.getModelName());
				pstmt.setString(2, details.getModelCode().trim().toLowerCase());
				pstmt.setLong(3, details.getManualLanguageId());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateSecModelSql = null;
			}
			else
			{
				pstmt = null;
				logger.info("importDivisionModelMappingDetails :: Division Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_div_model_mapping(mdm_cl_id,mdm_ml_id,mdm_div_id,"
						+ " mdm_crln_code, mdm_crln_name, mdm_esi_crln_code, "
						+ " mdm_divmm_sort_id,  mdm_divmm_flag,"
						+ " mdm_divmm_created_tmstp)"
						+ " VALUES(?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setLong(3, details.getDivisionId());
				pstmt.setString(4, details.getModelCode());
				pstmt.setString(5, details.getModelName());
				pstmt.setString(6, details.getEsiModelCode());
				if(null!=sortId)
				{
					pstmt.setInt(7, sortId);
				}
				else
				{
					pstmt.setNull(7, Types.INTEGER);
				}
				pstmt.setString(8, details.getFlag());
				pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			
			sortId= null;
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
			Utilities.printStackTraceToLogs(DivisionModelMappingDAO.class.getName(), "importDivisionModelMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importDivisionModelMappingDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importDivisionModelMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(DivisionModelMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getDivisionMappingSql = "SELECT * FROM gms3_sst_div_model_mapping WHERE mdm_cl_id = ? AND "
					+ " mdm_ml_id=? AND "
					+ " mdm_div_id=? AND TRIM(LOWER(mdm_esi_crln_code)) =? AND TRIM(LOWER(mdm_crln_code)) = ? ";
			getDivisionMappingSql  =getDivisionMappingSql	+" AND mdm_divmm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importDivisionModelMappingDetails :: getDivisionMappingSql :: > " + getDivisionMappingSql);
			pstmt = conn.prepareStatement(getDivisionMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getDivisionId());
			pstmt.setString(4, ImportActionUtils.safeLower(details.getEsiModelCode()));
			if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
			{
				pstmt.setString(5, ImportActionUtils.safeLower(details.getModelCode()));
			}
			else
			{
				details.setModelCode(null);
				pstmt.setString(5, details.getModelCode());
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_divmm_id");
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
