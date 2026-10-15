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
import com.mazda.gms3.mdm.vo.VinCrossReferenceDetails;

public class VINCrossReferenceDAO extends DBConnectionHelper{



	private static Logger logger = LogManager.getLogger(VINCrossReferenceDAO.class);
	
	public static ArrayList<VinCrossReferenceDetails> getVinCrossReferenceDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getVinCrossReferenceDetailsList :: Method Starts.");
		ArrayList<VinCrossReferenceDetails> crossRefList = new ArrayList<VinCrossReferenceDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_vin_xref WHERE mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND mdm_vin_xref_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getVinCrossReferenceDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					VinCrossReferenceDetails details = new VinCrossReferenceDetails();
					details.setSrNo(crossRefList.size()+1);
					details.setVinCrossReferenceId(rs.getLong("mdm_vin_xref_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					if(null!=rs.getString("mdm_cl_locale_code"))
					{
						details.setCountryLocaleCode(rs.getString("mdm_cl_locale_code").trim());
					}
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						details.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_vin_xref_code"))
					{
						details.setVinCrossReferenceCode(rs.getString("mdm_vin_xref_code").trim());
					}
					if(null!=rs.getString("mdm_vin_xref_year"))
					{
						details.setVinCrossReferenceYear(rs.getString("mdm_vin_xref_year").trim());
					}
					details.setFlag(rs.getString("mdm_vin_xref_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_vin_xref_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_vin_xref_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					crossRefList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getVinCrossReferenceDetailsList :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getVinCrossReferenceDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "getVinCrossReferenceDetailsList()", e);
			logger.info("getVinCrossReferenceDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getVinCrossReferenceDetailsList :: Method Ends.");
		return crossRefList;
	}

	public static boolean saveVinCrossReferenceDetails(VinCrossReferenceDetails details) throws SQLException
	{
		logger.info("saveVinCrossReferenceDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getCountryLocaleCode())
			{
				details.setCountryLocaleCode(details.getCountryLocaleCode().trim());
			}
			if(null!=details.getManualLanguageCode())
			{
				details.setManualLanguageCode(details.getManualLanguageCode().trim());
			}
			if(null!=details.getVinCrossReferenceCode())
			{
				details.setVinCrossReferenceCode(details.getVinCrossReferenceCode().trim());
			}
			if(null!=details.getVinCrossReferenceYear())
			{
				details.setVinCrossReferenceYear(details.getVinCrossReferenceYear().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_vin_xref(mdm_cl_id,mdm_ml_id,mdm_vin_xref_code,mdm_vin_xref_year,"
					+ "mdm_vin_xref_flag,mdm_vin_xref_created_tmstp,mdm_cl_locale_code, mdm_ml_lang_code"
					+ ") VALUES(?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getVinCrossReferenceCode());
			pstmt.setString(4, details.getVinCrossReferenceYear());
			pstmt.setString(5, details.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(7, details.getCountryLocaleCode());
			pstmt.setString(8, details.getManualLanguageCode());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveVinCrossReferenceDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "saveVinCrossReferenceDetails()", e);
			logger.info("saveVinCrossReferenceDetails :: ################ Exception ################");
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
		logger.info("saveVinCrossReferenceDetails :: Method Ends.");
		return true;
	}

	public static boolean updateVinCrossReferenceDetails(VinCrossReferenceDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateVinCrossReferenceDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getCountryLocaleCode())
			{
				details.setCountryLocaleCode(details.getCountryLocaleCode().trim());
			}
			if(null!=details.getManualLanguageCode())
			{
				details.setManualLanguageCode(details.getManualLanguageCode().trim());
			}
			if(null!=details.getVinCrossReferenceCode())
			{
				details.setVinCrossReferenceCode(details.getVinCrossReferenceCode().trim());
			}
			if(null!=details.getVinCrossReferenceYear())
			{
				details.setVinCrossReferenceYear(details.getVinCrossReferenceYear().trim());
			}
			// do this when object is passed as null
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_mdm_vin_xref SET mdm_vin_xref_code=?, mdm_vin_xref_year =?,"
					+ " mdm_vin_xref_flag=?,mdm_vin_xref_updated_tmstp=? WHERE mdm_vin_xref_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getVinCrossReferenceCode());
			pstmt.setString(2, details.getVinCrossReferenceYear());
			pstmt.setString(3, details.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, details.getVinCrossReferenceId());
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
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "updateVinCrossReferenceDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateVinCrossReferenceDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteVinCrossReferenceDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteVinCrossReferenceDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_vin_xref SET mdm_vin_xref_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_vin_xref_updated_tmstp = ?  WHERE mdm_vin_xref_id = "+ tokens[i].toString();
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
				logger.info("deleteVinCrossReferenceDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			// rollBack
			conn.rollback();
			logger.info("deleteVinCrossReferenceDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "deleteVinCrossReferenceDetails()", e);
			logger.info("deleteVinCrossReferenceDetails :: ################ Exception ################");
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
		logger.info("deleteVinCrossReferenceDetails :: Method Ends.");
		return true;
	}
	
	public static boolean activeVinCrossReferenceDetails(String activeIds) throws SQLException
	{
		logger.info("activeVinCrossReferenceDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_vin_xref SET mdm_vin_xref_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_vin_xref_updated_tmstp = ?  WHERE mdm_vin_xref_id = "+ tokens[i].toString();
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
				logger.info("activeVinCrossReferenceDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			// rollBack
			if(null!=conn)
			{
				conn.rollback();
			}
			logger.info("activeVinCrossReferenceDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "activeVinCrossReferenceDetails()", e);
			logger.info("activeVinCrossReferenceDetails :: ################ Exception ################");
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
		logger.info("activeVinCrossReferenceDetails :: Method Ends.");
		return true;
	}

	public static boolean importVinCrossReferenceDetails(VinCrossReferenceDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importVinCrossReferenceDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getCountryLocaleCode())
			{
				details.setCountryLocaleCode(details.getCountryLocaleCode().trim());
			}
			if(null!=details.getManualLanguageCode())
			{
				details.setManualLanguageCode(details.getManualLanguageCode().trim());
			}
			if(null!=details.getVinCrossReferenceCode())
			{
				details.setVinCrossReferenceCode(details.getVinCrossReferenceCode().trim());
			}
			if(null!=details.getVinCrossReferenceYear())
			{
				details.setVinCrossReferenceYear(details.getVinCrossReferenceYear().trim());
			}
			// do this when object is passed as null
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			rs = null;
			pstmt= null;
			/*
			 * CHECK WHETHER CROSS REF EXISTS OR NOT
			 * IF YES -  THEN UPDATE CROSS REF
			 * ELSE - INSERT CROSS REF
			 */
			String getCrossRefSql = "SELECT * FROM gms3_mdm_vin_xref WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ "  TRIM(LOWER(mdm_vin_xref_year))=? AND TRIM(LOWER(mdm_vin_xref_code)) = ? ";
			getCrossRefSql = getCrossRefSql	+ " AND mdm_vin_xref_flag "
					+ " NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importVinCrossReferenceDetails :: getCrossRefSql :: > " + getCrossRefSql);
			pstmt = conn.prepareStatement(getCrossRefSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getVinCrossReferenceYear().trim().toLowerCase());
			pstmt.setString(4, details.getVinCrossReferenceCode().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoCrossRefId=0;
			if(rs.next())
			{
				autoCrossRefId= rs.getLong("mdm_vin_xref_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getCrossRefSql = null;
			
			if(autoCrossRefId>0)
			{
				pstmt=null;
				logger.info("importVinCrossReferenceDetails :: Cross Ref Already Exists. Update Row for Auto CROSS REF id : >" + autoCrossRefId);
				
				String sql="UPDATE gms3_mdm_vin_xref SET mdm_vin_xref_code=?, mdm_vin_xref_year =?,"
						+ " mdm_vin_xref_updated_tmstp=? "
						+ " WHERE mdm_vin_xref_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getVinCrossReferenceCode());
				pstmt.setString(2, details.getVinCrossReferenceYear());
				pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(4, autoCrossRefId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
			}
			else
			{
				pstmt = null;
				logger.info("importVinCrossReferenceDetails :: Cross Ref Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_mdm_vin_xref(mdm_cl_id,mdm_ml_id,mdm_vin_xref_code,mdm_vin_xref_year,"
						+ "mdm_vin_xref_flag,mdm_vin_xref_created_tmstp,mdm_cl_locale_code, mdm_ml_lang_code"
						+ ") VALUES(?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getVinCrossReferenceCode());
				pstmt.setString(4, details.getVinCrossReferenceYear());
				pstmt.setString(5, details.getFlag());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(7, details.getCountryLocaleCode());
				pstmt.setString(8, details.getManualLanguageCode());
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
			Utilities.printStackTraceToLogs(VINCrossReferenceDAO.class.getName(), "importVinCrossReferenceDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importVinCrossReferenceDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importVinCrossReferenceDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(VinCrossReferenceDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getCrossRefSql = "SELECT * FROM gms3_mdm_vin_xref WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ "  TRIM(LOWER(mdm_vin_xref_year))=? AND TRIM(LOWER(mdm_vin_xref_code)) = ? ";
			getCrossRefSql = getCrossRefSql	+ " AND mdm_vin_xref_flag "
					+ " NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importVinCrossReferenceDetails :: getCrossRefSql :: > " + getCrossRefSql);
			pstmt = conn.prepareStatement(getCrossRefSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getVinCrossReferenceYear()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getVinCrossReferenceCode()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_vin_xref_id");
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