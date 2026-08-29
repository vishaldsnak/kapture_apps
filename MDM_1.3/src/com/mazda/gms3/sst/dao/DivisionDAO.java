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
import com.mazda.gms3.sst.vo.DivisionDetails;

public class DivisionDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(DivisionDAO.class);
	
	public static ArrayList<DivisionDetails> getDivisionDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getDivisionDetailsList :: Method Starts.");
		ArrayList<DivisionDetails> divisionList = new ArrayList<DivisionDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_division_master WHERE mdm_div_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getDivisionDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					DivisionDetails details = new DivisionDetails();
					details.setSrNo(divisionList.size()+1);
					details.setDivisionId(rs.getLong("mdm_div_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					details.setSortId(String.valueOf(rs.getInt("mdm_div_sort_id")));
					if(null!=rs.getString("mdm_div_name") && !"".equals(rs.getString("mdm_div_name")))
					{
						details.setDivisionName(rs.getString("mdm_div_name").trim());
					}
					if(null!=rs.getString("mdm_div_code") && !"".equals(rs.getString("mdm_div_code")))
					{
						details.setDivisionCode(rs.getString("mdm_div_code").trim());
					}
					details.setFlag(rs.getString("mdm_div_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_div_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_div_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					divisionList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getDivisionDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getDivisionDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "getDivisionDetailsList()", e);
			logger.info("getDivisionDetailsList :: ################ Exception ################");
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
//		logger.info("getDivisionDetailsList :: Method Ends.");
		return divisionList;
	}

	public static ArrayList<DivisionDetails> getDivisionDetailsListForCombo(String languageId) throws SQLException 
	{
//		logger.info("getDivisionDetailsList :: Method Starts.");
		ArrayList<DivisionDetails> divisionList = new ArrayList<DivisionDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_division_master WHERE "
						+ " mdm_div_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"')  ";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getDivisionDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					DivisionDetails details = new DivisionDetails();
					details.setSrNo(divisionList.size()+1);
					details.setDivisionId(rs.getLong("mdm_div_id"));
					if(null!=rs.getString("mdm_div_name") && !"".equals(rs.getString("mdm_div_name")))
					{
						details.setDivisionName(rs.getString("mdm_div_name").trim());
					}
					if(null!=rs.getString("mdm_div_code") && !"".equals(rs.getString("mdm_div_code")))
					{
						details.setDivisionCode(rs.getString("mdm_div_code").trim());
					}
					divisionList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getDivisionDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getDivisionDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "getDivisionDetailsList()", e);
			logger.info("getDivisionDetailsList :: ################ Exception ################");
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
//		logger.info("getDivisionDetailsList :: Method Ends.");
		return divisionList;
	}

	public static boolean saveDivisionDetails(DivisionDetails details) throws SQLException
	{
		logger.info("saveDivisionDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()) && !"0".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			if(null!=details.getDivisionCode() && !"".equals(details.getDivisionCode()))
			{
				details.setDivisionCode(details.getDivisionCode().trim());
			}
			if(null!=details.getDivisionName() && !"".equals(details.getDivisionName()))
			{
				details.setDivisionName(details.getDivisionName().trim());
			}
			
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_division_master(mdm_cl_id,mdm_ml_id,mdm_div_sort_id, mdm_div_name, mdm_div_flag,"
					+ " mdm_div_created_tmstp, mdm_div_code)"
					+ " VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setInt(3, sortId);
			pstmt.setString(4, details.getDivisionName());
			pstmt.setString(5, details.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(7, details.getDivisionCode());
			pstmt.executeUpdate();
			sql = null;
			sortId = null;
		}
		catch(Exception e)
		{
			logger.info("saveDivisionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "saveDivisionDetails()", e);
			logger.info("saveDivisionDetails :: ################ Exception ################");
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
		logger.info("saveDivisionDetails :: Method Ends.");
		return true;
	}

	public static boolean updateDivisionDetails(DivisionDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateDivisionDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()) && !"0".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			if(null!=details.getDivisionCode() && !"".equals(details.getDivisionCode()))
			{
				details.setDivisionCode(details.getDivisionCode().trim());
			}
			if(null!=details.getDivisionName() && !"".equals(details.getDivisionName()))
			{
				details.setDivisionName(details.getDivisionName().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_division_master SET mdm_div_sort_id =?,mdm_div_name =?,"
					+ " mdm_div_flag=?,mdm_div_updated_tmstp=?,mdm_div_code=?  WHERE mdm_div_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, sortId);
			pstmt.setString(2, details.getDivisionName());
			pstmt.setString(3, details.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(5, details.getDivisionCode());
			pstmt.setLong(6, details.getDivisionId());
			pstmt.executeUpdate();
			sql = null;
			sortId=  null;
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
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "updateDivisionDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateDivisionDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteDivisionDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteDivisionDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_division_master SET mdm_div_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_div_updated_tmstp = ?  WHERE mdm_div_id = "+ tokens[i].toString();
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
				logger.info("deleteDivisionDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteDivisionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "deleteDivisionDetails()", e);
			logger.info("deleteDivisionDetails :: ################ Exception ################");
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
		logger.info("deleteDivisionDetails :: Method Ends.");
		return true;
	}

	public static boolean activeDivisionDetails(String activeIds) throws SQLException
	{
		logger.info("activeDivisionDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_division_master SET mdm_div_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_div_updated_tmstp = ?  WHERE mdm_div_id = "+ tokens[i].toString();
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
				logger.info("activeDivisionDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeDivisionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "activeDivisionDetails()", e);
			logger.info("activeDivisionDetails :: ################ Exception ################");
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
		logger.info("activeDivisionDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importDivisionDetails(DivisionDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importDivisionDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			rs = null;
			pstmt= null;
			if(null!=details.getDivisionCode() && !"".equals(details.getDivisionCode()))
			{
				details.setDivisionCode(details.getDivisionCode().trim());
			}
			if(null!=details.getDivisionName() && !"".equals(details.getDivisionName()))
			{
				details.setDivisionName(details.getDivisionName().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			/*
			 * CHECK WHETHER VIN EXISTS OR NOT
			 * IF YES -  THEN UPDATE VIN
			 * ELSE - INSERT VIN
			 * SORT ID CAN BE 0
			 */
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			String getDivisionSql = "SELECT * FROM gms3_sst_division_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_div_code=? "
					+ " AND mdm_div_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importDivisionDetails :: getDivisionSql :: > " + getDivisionSql);
			pstmt = conn.prepareStatement(getDivisionSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getDivisionCode().trim());
			rs = pstmt.executeQuery();
			long autoDivisionId=0;
			if(rs.next())
			{
				autoDivisionId= rs.getLong("mdm_div_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getDivisionSql = null;
			
			if(autoDivisionId>0)
			{
				pstmt = null;
				logger.info("importDivisionDetails :: Division Already Exists. Update Row for Auto Division id : >" + autoDivisionId);
				
				String sql="UPDATE gms3_sst_division_master SET mdm_div_sort_id =?,mdm_div_name =?,"
						+ " mdm_div_updated_tmstp=?,mdm_div_code=? WHERE mdm_div_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setInt(1, sortId);
				pstmt.setString(2, details.getDivisionName());
				pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(4, details.getDivisionCode());
				pstmt.setLong(5, autoDivisionId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importDivisionDetails :: Division Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_division_master(mdm_cl_id,mdm_ml_id,mdm_div_sort_id, mdm_div_name, "
						+ " mdm_div_flag,mdm_div_created_tmstp,mdm_div_code) VALUES(?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setInt(3, sortId);
				pstmt.setString(4, details.getDivisionName());
				pstmt.setString(5, details.getFlag());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(7, details.getDivisionCode());
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
			Utilities.printStackTraceToLogs(DivisionDAO.class.getName(), "importDivisionDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importDivisionDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importDivisionDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(DivisionDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getDivisionSql = "SELECT * FROM gms3_sst_division_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_div_code=? "
					+ " AND mdm_div_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importDivisionDetails :: getDivisionSql :: > " + getDivisionSql);
			pstmt = conn.prepareStatement(getDivisionSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeTrim(details.getDivisionCode()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_div_id");
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
