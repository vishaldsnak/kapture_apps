package com.mazda.gms3.mdm.dao;

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
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;

public class CountryLocaleDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(CountryLocaleDAO.class);
	

	public static ArrayList<CountryLocaleDetails> getCountryLocaleDetailsList() throws SQLException 
	{
//		logger.info("getCountryLocaleDetailsList :: Method Starts.");
		ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
//			String sql = "SELECT * FROM gms3_mdm_country_locale WHERE "
//					+ "mdm_cl_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
			
			String sql = "SELECT * FROM gms3_mdm_country_locale ";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				CountryLocaleDetails clDetails = new CountryLocaleDetails();
				clDetails.setSrNo(countryLocaleList.size()+1);
				clDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
				if(null!=rs.getString("mdm_cl_locale_code") && !"".equals(rs.getString("mdm_cl_locale_code")))
				{	
					clDetails.setCountryLocaleCode(rs.getString("mdm_cl_locale_code").trim());
				}
				if(null!=rs.getString("mdm_cl_locale_desc") && !"".equals(rs.getString("mdm_cl_locale_desc")))
				{
					clDetails.setCountryLocaleDesc(rs.getString("mdm_cl_locale_desc").trim());
				}
				clDetails.setFlag(rs.getString("mdm_cl_flag"));
				clDetails.setEntryTime(rs.getTimestamp("mdm_cl_created_tmstp"));
				clDetails.setUpdatedTime(rs.getTimestamp("mdm_cl_updated_tmstp"));
				if(null!=clDetails.getFlag() && !"".equals(clDetails.getFlag()) && 
						clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
				{
					clDetails.setShowCheckBox(false);
				}
				countryLocaleList.add(clDetails);
				clDetails=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("getCountryLocaleDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "getCountryLocaleDetailsList()", e);
			logger.info("getCountryLocaleDetailsList :: ################ Exception ################");
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
//		logger.info("getCountryLocaleDetailsList :: Method Ends.");
		return countryLocaleList;
	}

	public static boolean saveCountryLocaleDetails(CountryLocaleDetails clDetails) throws SQLException
	{
		logger.info("saveCountryLocaleDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=clDetails.getCountryLocaleCode() && !"".equals(clDetails.getCountryLocaleCode()))
			{
				clDetails.setCountryLocaleCode(clDetails.getCountryLocaleCode().trim());
			}
			if(null!=clDetails.getCountryLocaleDesc() && !"".equals(clDetails.getCountryLocaleDesc()))
			{
				clDetails.setCountryLocaleDesc(clDetails.getCountryLocaleDesc().trim());
			}
			
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_country_locale(mdm_cl_locale_code,mdm_cl_locale_desc,"
					+ "mdm_cl_flag,mdm_cl_created_tmstp) VALUES(?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setString(2, clDetails.getCountryLocaleDesc());
			pstmt.setString(3, clDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveCountryLocaleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "saveCountryLocaleDetails()", e);
			logger.info("saveCountryLocaleDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			clDetails = null;
		}
		logger.info("saveCountryLocaleDetails :: Method Ends.");
		return true;
	}

	public static boolean updateCountryLocaleDetails(CountryLocaleDetails clDetails) throws SQLException
	{
		logger.info("updateCountryLocaleDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=clDetails.getCountryLocaleCode() && !"".equals(clDetails.getCountryLocaleCode()))
			{
				clDetails.setCountryLocaleCode(clDetails.getCountryLocaleCode().trim());
			}
			if(null!=clDetails.getCountryLocaleDesc() && !"".equals(clDetails.getCountryLocaleDesc()))
			{
				clDetails.setCountryLocaleDesc(clDetails.getCountryLocaleDesc().trim());
			}
			conn = getConnection();
			String sql="UPDATE gms3_mdm_country_locale SET mdm_cl_locale_code=?,mdm_cl_locale_desc =?,"
					+ "mdm_cl_flag=?,mdm_cl_updated_tmstp=? WHERE mdm_cl_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setString(2, clDetails.getCountryLocaleDesc());
			pstmt.setString(3, clDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, clDetails.getCountryLocaleId());
			pstmt.executeUpdate();
			sql = null;
			pstmt.close();pstmt=null;
			
			// CARLINE
			sql="UPDATE gms3_mdm_carline_codes SET mdm_cl_locale_code=? WHERE mdm_cl_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setLong(2, clDetails.getCountryLocaleId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
			// VIN
			sql="UPDATE gms3_mdm_vin_detail SET mdm_cl_locale_code=? WHERE mdm_cl_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setLong(2, clDetails.getCountryLocaleId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
			// MANUAL TYPES
			sql="UPDATE gms3_dmt_conv_manual_type SET mdm_cl_locale_code=? WHERE mdm_cl_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setLong(2, clDetails.getCountryLocaleId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
			
			// VIN CROSS REF
			sql="UPDATE gms3_mdm_vin_xref SET mdm_cl_locale_code=? WHERE mdm_cl_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clDetails.getCountryLocaleCode().toUpperCase());
			pstmt.setLong(2, clDetails.getCountryLocaleId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
		}
		catch(Exception e)
		{
			logger.info("updateCountryLocaleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "updateCountryLocaleDetails()", e);
			logger.info("updateCountryLocaleDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			clDetails=  null;
		}
		logger.info("updateCountryLocaleDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteCountryLocaleDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteCountryLocaleDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_country_locale SET mdm_cl_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_cl_updated_tmstp = ?  WHERE mdm_cl_id = "+ tokens[i].toString().trim();
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
				logger.info("deleteCountryLocaleDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteCountryLocaleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "deleteCountryLocaleDetails()", e);
			logger.info("deleteCountryLocaleDetails :: ################ Exception ################");
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
		logger.info("deleteCountryLocaleDetails :: Method Ends.");
		return true;
	}


	public static boolean permanentDeleteCountryLocaleDetails(String deleteIds) throws SQLException
	{
		logger.info("permanentDeleteCountryLocaleDetails :: Method Starts.");
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
						String sql="DELETE FROM gms3_mdm_country_locale WHERE mdm_cl_id = "+ tokens[i].toString().trim();
						pstmt = conn.prepareStatement(sql);
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
				logger.info("permanentDeleteCountryLocaleDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("permanentDeleteCountryLocaleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "permanentDeleteCountryLocaleDetails()", e);
			logger.info("permanentDeleteCountryLocaleDetails :: ################ Exception ################");
			/*
			 * A referential-integrity failure (MySQL 1451 - the Country/Locale is still
			 * referenced by child rows) is NOT transient: the record is in use. Re-throw it so
			 * the servlet can show a specific "in use" message. Any other failure keeps the
			 * original return-false path, which maps to the generic operation-failed message.
			 * (Under NO_ACTION FKs on the GR cluster the DB no longer cascade-deletes children,
			 * so this is now a reachable outcome instead of a silent cascade.)
			 */
			if(e instanceof java.sql.SQLIntegrityConstraintViolationException)
			{
				throw (java.sql.SQLIntegrityConstraintViolationException) e;
			}
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
		logger.info("permanentDeleteCountryLocaleDetails :: Method Ends.");
		return true;
	}


	public static ArrayList<CountryLocaleDetails> getCountryLocaleDetailsListForCombo() throws SQLException 
	{
//		logger.info("getCountryLocaleDetailsListForCombo :: Method Starts.");
		ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT mdm_cl_id,mdm_cl_locale_code,mdm_cl_locale_desc  "
					+ "FROM gms3_mdm_country_locale WHERE "
					+ "mdm_cl_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"') ORDER BY mdm_cl_locale_desc ASC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				CountryLocaleDetails clDetails = new CountryLocaleDetails();
				clDetails.setSrNo(countryLocaleList.size()+1);
				clDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
				/*
				 * do it reverse because desc added later for displaying
				 */
				if(null!=rs.getString("mdm_cl_locale_code"))
				{
					clDetails.setCountryLocaleDesc(rs.getString("mdm_cl_locale_code").trim());
				}
				if(null!=rs.getString("mdm_cl_locale_desc"))
				{
					clDetails.setCountryLocaleCode(rs.getString("mdm_cl_locale_desc").trim());
				}
				countryLocaleList.add(clDetails);
				clDetails=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("getCountryLocaleDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "getCountryLocaleDetailsListForCombo()", e);
			logger.info("getCountryLocaleDetailsListForCombo :: ################ Exception ################");
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
//		logger.info("getCountryLocaleDetailsListForCombo :: Method Ends.");
		return countryLocaleList;
	}

	public static String getCountryLocaleCode(String countryLocaleId) throws SQLException 
	{
		String countryLocaleCode=null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId))
			{
				conn = getConnection();
				String sql = "SELECT mdm_cl_locale_code   "
						+ "FROM gms3_mdm_country_locale WHERE mdm_cl_id="+new Long(countryLocaleId).longValue()+" AND "
						+ "mdm_cl_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
//				logger.info("getCountryLocaleCode :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(null!=rs.getString("mdm_cl_locale_code") && !"".equals(rs.getString("mdm_cl_locale_code")))
					{
						countryLocaleCode = rs.getString("mdm_cl_locale_code").trim();
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getCountryLocaleCode :: Country Locale id as Parameter is Null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCountryLocaleCode :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "getCountryLocaleCode()", e);
			logger.info("getCountryLocaleCode :: ################ Exception ################");
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
		return countryLocaleCode;
	}


}
