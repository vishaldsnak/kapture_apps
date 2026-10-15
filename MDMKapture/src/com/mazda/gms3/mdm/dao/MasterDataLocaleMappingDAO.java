package com.mazda.gms3.mdm.dao;

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
import com.mazda.gms3.mdm.vo.MasterDataLocaleMappingDetails;

public class MasterDataLocaleMappingDAO extends DBConnectionHelper{
	
	private static Logger logger = LogManager.getLogger(MasterDataLocaleMappingDAO.class);
	

	public static ArrayList<MasterDataLocaleMappingDetails> getMasterDataLocaleMappingDetailsList()  
	{
		ArrayList<MasterDataLocaleMappingDetails> list = new ArrayList<MasterDataLocaleMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			MasterDataLocaleMappingDetails details = null;
			conn = getConnection();
			String sql = "SELECT * FROM gms3_mdm_alt_lang_mapping";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				details = new MasterDataLocaleMappingDetails();
				details.setSrNo(list.size()+1);
				details.setMappingId(rs.getLong("mdm_alt_mapping_id"));
				if(null!=rs.getString("mdm_market") && !"".equals(rs.getString("mdm_market")))
				{
					details.setMarket(rs.getString("mdm_market").trim());
				}
				if(null!=rs.getString("mdm_locale") && !"".equals(rs.getString("mdm_locale")))
				{
					details.setLocale(rs.getString("mdm_locale").trim());
				}
				if(null!=rs.getString("mdm_alternate_locale") && !"".equals(rs.getString("mdm_alternate_locale")))
				{
					details.setAlternateLocale(rs.getString("mdm_alternate_locale").trim());
				}
				if(null!=rs.getString("mdm_master_data_type") && !"".equals(rs.getString("mdm_master_data_type")))
				{
					details.setMasterDataType(rs.getString("mdm_master_data_type").trim());
				}
				details.setFlag(rs.getString("mdm_alt_mapping_status"));
				details.setEntryTime(rs.getTimestamp("mdm_alt_created_tmstp"));
				details.setUpdatedTime(rs.getTimestamp("mdm_alt_updated_tmstp"));
				if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
						details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
				{
					details.setShowCheckBox(false);
				}
				list.add(details);
				details=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "getMasterDataLocaleMappingDetailsList()", e);
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "getMasterDataLocaleMappingDetailsList()", e);
			}
		}
		return list;
	}


	public static boolean saveMasterDataLocaleMappingDetails(MasterDataLocaleMappingDetails details) throws SQLException
	{
		logger.info("saveMasterDataLocaleMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getMarket())
			{
				details.setMarket(details.getMarket().trim());
			}
			if(null!=details.getLocale())
			{
				details.setLocale(details.getLocale().trim());
			}
			if(null!=details.getAlternateLocale())
			{
				details.setAlternateLocale(details.getAlternateLocale().trim());
			}
			if(null!=details.getMasterDataType())
			{
				details.setMasterDataType(details.getMasterDataType().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_alt_lang_mapping(mdm_market, mdm_locale, mdm_alternate_locale,mdm_master_data_type,mdm_alt_mapping_status,"
					+ " mdm_alt_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getMarket());
			pstmt.setString(2, details.getLocale());
			pstmt.setString(3, details.getAlternateLocale());
			pstmt.setString(4, details.getMasterDataType());
			pstmt.setString(5, details.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveMasterDataLocaleMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "saveMasterDataLocaleMappingDetails()", e);
			logger.info("saveMasterDataLocaleMappingDetails :: ################ Exception ################");
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
		logger.info("saveMasterDataLocaleMappingDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<MasterDataLocaleMappingDetails> updateMasterDataLocaleMappingDetails(ArrayList<MasterDataLocaleMappingDetails> list) throws SQLException
	{
		logger.info("updateMasterDataLocaleMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				MasterDataLocaleMappingDetails details = new MasterDataLocaleMappingDetails();
				String sql = null;
				conn = getConnection();
				for(int a=0;a<list.size();a++)
				{
					details = (MasterDataLocaleMappingDetails)list.get(a);
					if(null!=details.getMarket())
					{
						details.setMarket(details.getMarket().trim());
					}
					if(null!=details.getLocale())
					{
						details.setLocale(details.getLocale().trim());
					}
					if(null!=details.getAlternateLocale())
					{
						details.setAlternateLocale(details.getAlternateLocale().trim());
					}
					if(null!=details.getMasterDataType())
					{
						details.setMasterDataType(details.getMasterDataType().trim());
					}
					try
					{
						sql="UPDATE gms3_mdm_alt_lang_mapping SET mdm_market =?,mdm_locale =?,"
								+ " mdm_alt_mapping_status=?,mdm_alt_updated_tmstp=?, mdm_master_data_type=?,mdm_alternate_locale = ? "
								+ "  WHERE mdm_alt_mapping_id =?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, details.getMarket());
						pstmt.setString(2, details.getLocale());
						pstmt.setString(3, details.getFlag());
						pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(5, details.getMasterDataType());
						pstmt.setString(6, details.getAlternateLocale());
						pstmt.setLong(7, details.getMappingId());
						pstmt.executeUpdate();
						sql = null;
						pstmt.close();pstmt = null;
						details.setSaveStatusWhileImport(true);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "updateMasterDataLocaleMappingDetails()", e);
					}
				}
			}
			else
			{
				logger.info("updateMasterDataLocaleMappingDetails :: List as Parameter is null. Nothing to update.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "updateMasterDataLocaleMappingDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateMasterDataLocaleMappingDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteMasterDataLocaleMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteMasterDataLocaleMappingDetails :: Method Starts.");
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
						pstmt = null;
						sql="UPDATE gms3_mdm_alt_lang_mapping SET mdm_alt_mapping_status='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_alt_updated_tmstp = ?  WHERE mdm_alt_mapping_id = "+ tokens[i].toString();
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
				logger.info("deleteMasterDataLocaleMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteMasterDataLocaleMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "deleteMasterDataLocaleMappingDetails()", e);
			logger.info("deleteMasterDataLocaleMappingDetails :: ################ Exception ################");
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
		logger.info("deleteMasterDataLocaleMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeMasterDataLocaleMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeMasterDataLocaleMappingDetails :: Method Starts.");
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
					String sql="";
					for(int i=0;i<tokens.length;i++)
					{
						pstmt = null;
						sql="UPDATE gms3_mdm_alt_lang_mapping SET mdm_alt_mapping_status='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_alt_updated_tmstp = ?  WHERE mdm_alt_mapping_id = "+ tokens[i].toString();
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
				logger.info("activeMasterDataLocaleMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeMasterDataLocaleMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "activeMasterDataLocaleMappingDetails()", e);
			logger.info("activeMasterDataLocaleMappingDetails :: ################ Exception ################");
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
		logger.info("activeMasterDataLocaleMappingDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<MasterDataLocaleMappingDetails> importMasterDataLocaleMappingDetails(ArrayList<MasterDataLocaleMappingDetails> list) throws SQLException
	{
		logger.info("importMasterDataLocaleMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				MasterDataLocaleMappingDetails details = new MasterDataLocaleMappingDetails();
				String getDisplayOrderSql="";
				String sql="";
				long autoMappingId=0;
				for(int a=0;a<list.size();a++)
				{
					details = (MasterDataLocaleMappingDetails)list.get(a);
					if(null!=details.getMarket())
					{
						details.setMarket(details.getMarket().trim());
					}
					if(null!=details.getLocale())
					{
						details.setLocale(details.getLocale().trim());
					}
					if(null!=details.getAlternateLocale())
					{
						details.setAlternateLocale(details.getAlternateLocale().trim());
					}
					if(null!=details.getMasterDataType())
					{
						details.setMasterDataType(details.getMasterDataType().trim());
					}
					rs = null;
					pstmt= null;
					try
					{
						/*
						 * CHECK WHETHER mapping EXISTS OR NOT
						 * IF YES -  THEN UPDATE MAPPING - CHANGE STATUS TO ACTIVE
						 * ELSE - INSERT VIN WITH STATUS AS ACTIVE
						 */
						getDisplayOrderSql = "SELECT * FROM gms3_mdm_alt_lang_mapping WHERE  "
								+ " TRIM(LOWER(mdm_market))=? AND TRIM(LOWER(mdm_master_data_type)) = ? AND TRIM(LOWER(mdm_locale)) = ?  ";
						pstmt = conn.prepareStatement(getDisplayOrderSql);
						pstmt.setString(1, details.getMarket().trim().toLowerCase());
						pstmt.setString(2, details.getMasterDataType().trim().toLowerCase());
						pstmt.setString(3, details.getLocale().trim().toLowerCase());
						rs = pstmt.executeQuery();
						autoMappingId=0;
						if(rs.next())
						{
							autoMappingId= rs.getLong("mdm_alt_mapping_id");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getDisplayOrderSql = null;

						if(autoMappingId>0)
						{
							pstmt = null;
							logger.info("importMasterDataLocaleMappingDetails :: Mapping Already Exists. Update Row for Auto Mapping id : >" + autoMappingId);

							/*
							 * ONLY UPDATE ALTERNATE LOCALE + MAPPING STATUS
							 */
							sql="UPDATE gms3_mdm_alt_lang_mapping SET mdm_alternate_locale = ?, mdm_alt_mapping_status = ?, "
									+ " mdm_alt_updated_tmstp=? WHERE mdm_alt_mapping_id =?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, details.getAlternateLocale());
							pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
							pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setLong(4, autoMappingId);
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							details.setSaveStatusWhileImport(true);
						}
						else
						{
							// mapping status - Always Active
							pstmt = null;
							logger.info("importMasterDataLocaleMappingDetails :: Mapping Does not Exists. Insert New Row.");
							sql="INSERT INTO gms3_mdm_alt_lang_mapping(mdm_market, mdm_locale, "
									+ " mdm_alt_mapping_status,mdm_alt_created_tmstp,mdm_master_data_type,mdm_alternate_locale) VALUES(?,?,?,?,?,?)";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, details.getMarket());
							pstmt.setString(2, details.getLocale());
							pstmt.setString(3, ApplicationProperties.getProperty("flag.value.active"));
							pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(5, details.getMasterDataType());
							pstmt.setString(6, details.getAlternateLocale());
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							details.setSaveStatusWhileImport(true);
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "importMasterDataLocaleMappingDetails()", e);
					}
				}
				details = null;
			}
			else
			{
				logger.info("importMasterDataLocaleMappingDetails ::  List as Input Parameter is Null. Nothing to Import.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMappingDAO.class.getName(), "importMasterDataLocaleMappingDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("importMasterDataLocaleMappingDetails :: Method Ends.");
		return list;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importMasterDataLocaleMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(MasterDataLocaleMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getDisplayOrderSql = null;
						getDisplayOrderSql = "SELECT * FROM gms3_mdm_alt_lang_mapping WHERE  "
								+ " TRIM(LOWER(mdm_market))=? AND TRIM(LOWER(mdm_master_data_type)) = ? AND TRIM(LOWER(mdm_locale)) = ?  ";
						pstmt = conn.prepareStatement(getDisplayOrderSql);
						pstmt.setString(1, ImportActionUtils.safeLower(details.getMarket()));
						pstmt.setString(2, ImportActionUtils.safeLower(details.getMasterDataType()));
						pstmt.setString(3, ImportActionUtils.safeLower(details.getLocale()));
						rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_alt_mapping_id");
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
