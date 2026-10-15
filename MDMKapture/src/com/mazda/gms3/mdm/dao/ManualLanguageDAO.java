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
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;

public class ManualLanguageDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(ManualLanguageDAO.class);

	public static ArrayList<ManualLanguageDetails> getManualLanguageDetailsList(String countryLocaleId) throws SQLException 
	{
		//		logger.info("getManualLanguageDetailsList :: Method Starts.");
		ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_manual_language WHERE "
						+ "mdm_cl_id="+ new Long(countryLocaleId).longValue();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					ManualLanguageDetails mlDetails = new ManualLanguageDetails();
					mlDetails.setSrNo(languageList.size()+1);
					mlDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					mlDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						mlDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_ml_lang_desc"))
					{
						mlDetails.setManualLanguageName(rs.getString("mdm_ml_lang_desc").trim());
					}
					mlDetails.setFlag(rs.getString("mdm_ml_flag"));
					mlDetails.setEntryTime(rs.getTimestamp("mdm_ml_created_tmstp"));
					mlDetails.setUpdatedTime(rs.getTimestamp("mdm_ml_updated_tmstp"));
					if(null!=mlDetails.getFlag() && !"".equals(mlDetails.getFlag()) && 
							mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						mlDetails.setShowCheckBox(false);
					}
					languageList.add(mlDetails);
					mlDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getManualLanguageDetailsList :: Country Locale id as Paramteter is null. Return null.");
				languageList = null;
			}
		}
		catch(Exception e)
		{
			logger.info("getManualLanguageDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "getManualLanguageDetailsList()", e);
			logger.info("getManualLanguageDetailsList :: ################ Exception ################");
			languageList = null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			// set used param to null
			countryLocaleId = null;
		}
		//		logger.info("getManualLanguageDetailsList :: Method Ends.");
		return languageList;
	}

	public static String getManualLanguageName(String langCode) throws SQLException 
	{
		logger.info("getManualLanguageName :: Method Starts.");
		String langName=null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT mdm_ml_lang_desc FROM gms3_mdm_manual_language "
						+ " WHERE TRIM(LOWER(mdm_ml_lang_code)) ='"+langCode+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(null!=rs.getString("mdm_ml_lang_desc"))
					{
						langName =  rs.getString("mdm_ml_lang_desc").trim();
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getManualLanguageName :: Language Code as parameter is null. Return null");
			}
		}
		catch(Exception e)
		{
			langName = null;
			logger.info("getManualLanguageName :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "getManualLanguageName()", e);
			logger.info("getManualLanguageName :: ################ Exception ################");
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
		logger.info("getManualLanguageName :: Method Ends.");
		return langName;
	}

	public static String getManualLanguageCode(String languageId) throws SQLException 
	{
		String langCode=null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT mdm_ml_lang_code FROM gms3_mdm_manual_language  WHERE "
						+ " mdm_ml_id = "+new Long(languageId).longValue()+" ";
				//				logger.info("getManualLanguageCode :: Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						langCode =  rs.getString("mdm_ml_lang_code").trim();
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getManualLanguageCode :: Language id as parameter is null. Return null");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "getManualLanguageCode()", e);
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
		return langCode;
	}

	public static String getManualLanguageId(String languageCode) throws SQLException 
	{
		String languageId=null;
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageCode && !"".equals(languageCode))
			{
				conn = getConnection();
				String sql = "SELECT mdm_ml_id FROM gms3_mdm_manual_language  WHERE "
						+ " TRIM(LOWER(mdm_ml_lang_code)) = '"+languageCode.trim().toLowerCase()+"' AND "
						+ " mdm_ml_flag ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(rs.getLong("mdm_ml_id")>0)
					{
						languageId =  String.valueOf(rs.getLong("mdm_ml_id"));
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getManualLanguageCode :: Language id as parameter is null. Return null");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "getManualLanguageCode()", e);
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
		return languageId;
	}

	
	public static boolean saveManualLanguageDetails(ManualLanguageDetails mlDetails) throws SQLException
	{
		logger.info("saveManualLanguageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=mlDetails.getManualLanguageCode())
			{
				mlDetails.setManualLanguageCode(mlDetails.getManualLanguageCode().trim());
			}
			if(null!=mlDetails.getManualLanguageName())
			{
				mlDetails.setManualLanguageName(mlDetails.getManualLanguageName().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_manual_language(mdm_cl_id,mdm_ml_lang_code,mdm_ml_lang_desc,"
					+ "mdm_ml_flag,mdm_ml_created_tmstp) VALUES(?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, mlDetails.getCountryLocaleId());
			pstmt.setString(2, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setString(3, mlDetails.getManualLanguageName());
			pstmt.setString(4, mlDetails.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveManualLanguageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "saveManualLanguageDetails()", e);
			logger.info("saveManualLanguageDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			mlDetails = null;
		}
		logger.info("saveManualLanguageDetails :: Method Ends.");
		return true;
	}

	public static boolean updateManualLanguageDetails(ManualLanguageDetails mlDetails) throws SQLException
	{
		logger.info("updateManualLanguageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=mlDetails.getManualLanguageCode())
			{
				mlDetails.setManualLanguageCode(mlDetails.getManualLanguageCode().trim());
			}
			if(null!=mlDetails.getManualLanguageName())
			{
				mlDetails.setManualLanguageName(mlDetails.getManualLanguageName().trim());
			}
			conn = getConnection();
			String sql="UPDATE gms3_mdm_manual_language SET mdm_ml_lang_code=?,mdm_ml_lang_desc =?,"
					+ "mdm_ml_flag=?,mdm_ml_updated_tmstp=? WHERE mdm_ml_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setString(2, mlDetails.getManualLanguageName());
			pstmt.setString(3, mlDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, mlDetails.getManualLanguageId());
			pstmt.executeUpdate();
			sql = null;
			pstmt.close();pstmt=null;
			// CARLINE
			sql="UPDATE gms3_mdm_carline_codes SET mdm_ml_lang_code=? WHERE mdm_ml_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setLong(2, mlDetails.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
			// VIN
			sql="UPDATE gms3_mdm_vin_detail SET mdm_ml_lang_code=? WHERE mdm_ml_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setLong(2, mlDetails.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
			// MANUAL TYPES
			sql="UPDATE gms3_dmt_conv_manual_type SET mdm_ml_lang_code=? WHERE mdm_ml_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setLong(2, mlDetails.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;

			// VIN CROSS REF
			sql="UPDATE gms3_mdm_vin_xref SET mdm_ml_lang_code=? WHERE mdm_ml_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, Utilities.convertStringForCode(mlDetails.getManualLanguageCode().trim()));
			pstmt.setLong(2, mlDetails.getManualLanguageId());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;
		}
		catch(Exception e)
		{
			logger.info("updateManualLanguageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "updateManualLanguageDetails()", e);
			logger.info("updateManualLanguageDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			mlDetails=  null;
		}
		logger.info("updateManualLanguageDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteManualLanguageDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteManualLanguageDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_manual_language SET mdm_ml_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_ml_updated_tmstp = ?  WHERE mdm_ml_id = "+ tokens[i].toString();
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
				logger.info("deleteManualLanguageDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteManualLanguageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "deleteManualLanguageDetails()", e);
			logger.info("deleteManualLanguageDetails :: ################ Exception ################");
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
		logger.info("deleteManualLanguageDetails :: Method Ends.");
		return true;
	}

	public static boolean permanentDeleteManualLanguageDetails(String deleteIds) throws SQLException
	{
		logger.info("permanentDeleteManualLanguageDetails :: Method Starts.");
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
						String sql="DELETE FROM gms3_mdm_manual_language  WHERE mdm_ml_id = "+ tokens[i].toString();
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
				logger.info("permanentDeleteManualLanguageDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("permanentDeleteManualLanguageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "permanentDeleteManualLanguageDetails()", e);
			logger.info("permanentDeleteManualLanguageDetails :: ################ Exception ################");
			/*
			 * A referential-integrity failure (MySQL 1451 - the Manual Language is still
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
		logger.info("permanentDeleteManualLanguageDetails :: Method Ends.");
		return true;
	}


	public static ArrayList<ManualLanguageDetails> getManualLanguageDetailsListForCombo(String countryLocaleId) throws SQLException 
	{
		//		logger.info("getManualLanguageDetailsListForCombo :: Method Starts.");
		ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId))
			{
				conn = getConnection();
				String sql = "SELECT mdm_ml_id,mdm_ml_lang_code,mdm_ml_lang_desc FROM gms3_mdm_manual_language WHERE "
						+ "mdm_ml_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ "AND mdm_cl_id="+new Long(countryLocaleId).longValue()+" ORDER BY mdm_ml_lang_desc ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					ManualLanguageDetails mlDetails = new ManualLanguageDetails();
					mlDetails.setSrNo(languageList.size()+1);
					mlDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_desc"))
					{
						mlDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_desc").trim());
					}
					// set Language Code as well, as it is now required for Phase 1.2 since most of the development is done. Use Dec variable
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						mlDetails.setManualLanguageName(rs.getString("mdm_ml_lang_code").trim());
					}
					languageList.add(mlDetails);
					mlDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getManualLanguageDetailsListForCombo :: Country Locale id as parameter is null. Return null.");
				languageList = null;
			}
		}
		catch(Exception e)
		{
			logger.info("getManualLanguageDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualLanguageDAO.class.getName(), "getManualLanguageDetailsListForCombo()", e);
			logger.info("getManualLanguageDetailsListForCombo :: ################ Exception ################");
			languageList = null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			// set param to null
			countryLocaleId = null;
		}
		//		logger.info("getManualLanguageDetailsListForCombo :: Method Ends.");
		return languageList;
	}

}