package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ImportActionResult;
import com.mazda.gms3.mdm.vo.ManualTypeDetails;

/*
 * NOTE (Oracle -> MySQL migration):
 * All table and column identifiers in this DAO are written in lowercase to match
 * the MySQL schema produced by the data-migration job (Job 1) and to stay safe on
 * case-sensitive MySQL servers (lower_case_table_names=0 on Linux). No Oracle-only
 * SQL (sequences, SYSDATE, ROWNUM, DUAL, NVL, (+) joins) is used here.
 */
public class ManualTypeDAO extends DBConnectionHelper{



	private static Logger logger = LogManager.getLogger(ManualTypeDAO.class);

	public static ArrayList<ManualTypeDetails> getManualTypeDetailsList(String langCode) throws SQLException
	{
//		logger.info("getManualTypeDetailsList :: Method Starts.");
		ArrayList<ManualTypeDetails> manualTypeList = new ArrayList<ManualTypeDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_dmt_conv_manual_type WHERE mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND dc_manual_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getManualTypeDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					ManualTypeDetails details = new ManualTypeDetails();
					details.setSrNo(manualTypeList.size()+1);
					details.setManualTypeId(rs.getLong("dc_manual_mt_id"));
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
					if(null!=rs.getString("dc_manual_code"))
					{
						details.setManualTypeCode(rs.getString("dc_manual_code").trim());
					}
					if(null!=rs.getString("dc_manual_ref_key"))
					{
						details.setManualTypeRefKey(rs.getString("dc_manual_ref_key").trim());
					}
					if(null!=rs.getString("dc_manual_name"))
					{
						details.setManualTypeName(rs.getString("dc_manual_name").trim());
					}
					details.setFlag(rs.getString("dc_manual_flag"));
					details.setEntryTime(rs.getTimestamp("dc_manual_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("dc_manual_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) &&
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					manualTypeList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getManualTypeDetailsList :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getManualTypeDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "getManualTypeDetailsList()", e);
			logger.info("getManualTypeDetailsList :: ################ Exception ################");
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
//		logger.info("getManualTypeDetailsList :: Method Ends.");
		return manualTypeList;
	}

	public static boolean saveManualTypeDetails(ManualTypeDetails details) throws SQLException
	{
		logger.info("saveManualTypeDetails :: Method Starts.");
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
			if(null!=details.getManualTypeCode())
			{
				details.setManualTypeCode(details.getManualTypeCode().trim());
			}
			if(null!=details.getManualTypeRefKey())
			{
				details.setManualTypeRefKey(details.getManualTypeRefKey().trim());
			}
			if(null!=details.getManualTypeName())
			{
				details.setManualTypeName(details.getManualTypeName().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_dmt_conv_manual_type(mdm_cl_id,mdm_ml_id,dc_manual_code,dc_manual_ref_key,"
					+ "dc_manual_flag,dc_manual_created_tmstp,mdm_cl_locale_code, mdm_ml_lang_code,dc_manual_name"
					+ ") VALUES(?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getManualTypeCode());
			pstmt.setString(4, details.getManualTypeRefKey());
			pstmt.setString(5, details.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(7, details.getCountryLocaleCode());
			pstmt.setString(8, details.getManualLanguageCode());
			pstmt.setString(9, details.getManualTypeName());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveManualTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "saveManualTypeDetails()", e);
			logger.info("saveManualTypeDetails :: ################ Exception ################");
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
		logger.info("saveManualTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean updateManualTypeDetails(ManualTypeDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateManualTypeDetails :: Method Starts.");
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
			if(null!=details.getManualTypeCode())
			{
				details.setManualTypeCode(details.getManualTypeCode().trim());
			}
			if(null!=details.getManualTypeRefKey())
			{
				details.setManualTypeRefKey(details.getManualTypeRefKey().trim());
			}
			if(null!=details.getManualTypeName())
			{
				details.setManualTypeName(details.getManualTypeName().trim());
			}
			if(null==conn || conn.isClosed()==true)
			{
				conn= getConnection();
			}
			String sql="UPDATE gms3_dmt_conv_manual_type SET dc_manual_code=?, dc_manual_ref_key =?,"
					+ " dc_manual_flag=?,dc_manual_updated_tmstp=?,dc_manual_name=? WHERE dc_manual_mt_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getManualTypeCode());
			pstmt.setString(2, details.getManualTypeRefKey());
			pstmt.setString(3, details.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(5, details.getManualTypeName());
			pstmt.setLong(6, details.getManualTypeId());
			pstmt.executeUpdate();
			sql = null;
			if(null==closeConnection)
			{
				// close connection
				if(null!=conn)
				{
					conn.close();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "updateManualTypeDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateManualTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteManualTypeDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteManualTypeDetails :: Method Starts.");
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
						String sql="UPDATE gms3_dmt_conv_manual_type SET dc_manual_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,dc_manual_updated_tmstp = ?  WHERE dc_manual_mt_id = "+ tokens[i].toString();
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
				logger.info("deleteManualTypeDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			// rollBack
			conn.rollback();
			logger.info("deleteManualTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "deleteManualTypeDetails()", e);
			logger.info("deleteManualTypeDetails :: ################ Exception ################");
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
		logger.info("deleteManualTypeDetails :: Method Ends.");
		return true;
	}

	public static boolean activeManualTypeDetails(String activeIds) throws SQLException
	{
		logger.info("activeManualTypeDetails :: Method Starts.");
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
						String sql="UPDATE gms3_dmt_conv_manual_type SET dc_manual_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,dc_manual_updated_tmstp = ?  WHERE dc_manual_mt_id = "+ tokens[i].toString();
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
				logger.info("activeManualTypeDetails :: Active Ids as parameter is null. Return false");
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
			logger.info("activeManualTypeDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "activeManualTypeDetails()", e);
			logger.info("activeManualTypeDetails :: ################ Exception ################");
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
		logger.info("activeManualTypeDetails :: Method Ends.");
		return true;
	}

	/**
	 * Import handler for the whole uploaded Excel, honoring the Action / Marker column.
	 *
	 * saveList   - rows marked A/ADD/U/UPDATE (or blank): create-or-update on the existing
	 *              unique combination (locale + language + lower(code)).
	 * deleteList - rows marked D/DELETE: soft-delete (set flag to flag.value.delete) the row
	 *              matched by the SAME unique combination. Excel carries no primary key, so the
	 *              row to retire is located exactly the way create-or-update locates an existing row.
	 *
	 * Performance / correctness (Oracle -> MySQL migration hardening):
	 *  - ONE connection, ONE transaction for the whole file (autocommit off, single commit) instead
	 *    of the previous per-row autocommit.
	 *  - The lookup / insert / update / soft-delete PreparedStatements are prepared ONCE and reused
	 *    for every row (no re-parse per row).
	 *  - Each row runs inside its own SAVEPOINT so a single bad row is isolated and rolled back
	 *    without discarding the rows that already succeeded; its row number is reported as a failure.
	 *
	 * The connection is owned by the caller (it is NOT closed here).
	 */
	public static ImportActionResult importManualTypeDetails(List<ManualTypeDetails> saveList,
			List<ManualTypeDetails> deleteList, Connection conn) throws SQLException
	{
		logger.info("importManualTypeDetails (batch) :: Method Starts.");
		ImportActionResult result = new ImportActionResult();
		PreparedStatement psSelect = null;
		PreparedStatement psInsert = null;
		PreparedStatement psUpdate = null;
		PreparedStatement psSoftDelete = null;
		boolean priorAutoCommit = true;
		boolean autoCommitChanged = false;
		boolean createdConn = false;
		String deleteFlag = ApplicationProperties.getProperty("flag.value.delete");
		try
		{
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
				createdConn = true;
			}
			priorAutoCommit = conn.getAutoCommit();
			if(priorAutoCommit)
			{
				conn.setAutoCommit(false);
				autoCommitChanged = true;
			}

			// Existence check on the unique combination, excluding already soft-deleted rows.
			String selectSql = "SELECT dc_manual_mt_id FROM gms3_dmt_conv_manual_type "
					+ "WHERE mdm_cl_id=? AND mdm_ml_id=? AND TRIM(LOWER(dc_manual_code))=? "
					+ "AND dc_manual_flag NOT IN (?)";
			String insertSql = "INSERT INTO gms3_dmt_conv_manual_type(mdm_cl_id,mdm_ml_id,dc_manual_code,"
					+ "dc_manual_ref_key,dc_manual_flag,dc_manual_created_tmstp,mdm_cl_locale_code,"
					+ "mdm_ml_lang_code,dc_manual_name) VALUES(?,?,?,?,?,?,?,?,?)";
			String updateSql = "UPDATE gms3_dmt_conv_manual_type SET dc_manual_code=?, dc_manual_ref_key=?, "
					+ "dc_manual_updated_tmstp=?, dc_manual_name=? WHERE dc_manual_mt_id=?";
			String softDeleteSql = "UPDATE gms3_dmt_conv_manual_type SET dc_manual_flag=?, "
					+ "dc_manual_updated_tmstp=? WHERE dc_manual_mt_id=?";

			psSelect = conn.prepareStatement(selectSql);
			psInsert = conn.prepareStatement(insertSql);
			psUpdate = conn.prepareStatement(updateSql);
			psSoftDelete = conn.prepareStatement(softDeleteSql);

			// ---------- CREATE-OR-UPDATE (A / U / blank) ----------
			if(null!=saveList && saveList.size()>0)
			{
				for(int i=0;i<saveList.size();i++)
				{
					ManualTypeDetails details = saveList.get(i);
					trimForImport(details);
					Savepoint sp = conn.setSavepoint();
					try
					{
						long existingId = findExistingId(psSelect, details, deleteFlag);
						if(existingId>0)
						{
							psUpdate.setString(1, details.getManualTypeCode());
							psUpdate.setString(2, details.getManualTypeRefKey());
							psUpdate.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
							psUpdate.setString(4, details.getManualTypeName());
							psUpdate.setLong(5, existingId);
							psUpdate.executeUpdate();
						}
						else
						{
							psInsert.setLong(1, details.getCountryLocaleId());
							psInsert.setLong(2, details.getManualLanguageId());
							psInsert.setString(3, details.getManualTypeCode());
							psInsert.setString(4, details.getManualTypeRefKey());
							psInsert.setString(5, details.getFlag());
							psInsert.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
							psInsert.setString(7, details.getCountryLocaleCode());
							psInsert.setString(8, details.getManualLanguageCode());
							psInsert.setString(9, details.getManualTypeName());
							psInsert.executeUpdate();
						}
						conn.releaseSavepoint(sp);
						result.getSaveSuccessRows().add(details.getSrNo());
					}
					catch(Exception rowEx)
					{
						conn.rollback(sp);
						result.getSaveFailureRows().add(details.getSrNo());
						logger.info("importManualTypeDetails (batch) :: create-or-update failed for Row No {"+details.getSrNo()+"}.");
						Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "importManualTypeDetails() save row", rowEx);
					}
					details = null;
				}
			}

			// ---------- SOFT-DELETE (D) ----------
			if(null!=deleteList && deleteList.size()>0)
			{
				for(int i=0;i<deleteList.size();i++)
				{
					ManualTypeDetails details = deleteList.get(i);
					trimForImport(details);
					Savepoint sp = conn.setSavepoint();
					try
					{
						long existingId = findExistingId(psSelect, details, deleteFlag);
						if(existingId>0)
						{
							psSoftDelete.setString(1, deleteFlag);
							psSoftDelete.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
							psSoftDelete.setLong(3, existingId);
							psSoftDelete.executeUpdate();
							conn.releaseSavepoint(sp);
							result.getDeleteSuccessRows().add(details.getSrNo());
						}
						else
						{
							// No active row for this unique combination -> nothing to delete.
							conn.releaseSavepoint(sp);
							result.getDeleteFailureRows().add(details.getSrNo());
							logger.info("importManualTypeDetails (batch) :: no active row to delete for Row No {"+details.getSrNo()+"}.");
						}
					}
					catch(Exception rowEx)
					{
						conn.rollback(sp);
						result.getDeleteFailureRows().add(details.getSrNo());
						logger.info("importManualTypeDetails (batch) :: soft-delete failed for Row No {"+details.getSrNo()+"}.");
						Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "importManualTypeDetails() delete row", rowEx);
					}
					details = null;
				}
			}

			conn.commit();
		}
		catch(Exception e)
		{
			result.setFatal(true);
			try
			{
				if(null!=conn && conn.isClosed()==false)
				{
					conn.rollback();
				}
			}
			catch(Exception ignore) { }
			logger.info("importManualTypeDetails (batch) :: ################ Exception ################");
			Utilities.printStackTraceToLogs(ManualTypeDAO.class.getName(), "importManualTypeDetails()", e);
			logger.info("importManualTypeDetails (batch) :: ################ Exception ################");
		}
		finally
		{
			if(null!=psSelect)
				psSelect.close();
			if(null!=psInsert)
				psInsert.close();
			if(null!=psUpdate)
				psUpdate.close();
			if(null!=psSoftDelete)
				psSoftDelete.close();
			// restore autocommit if we changed it (connection is normally closed by the caller)
			try
			{
				if(autoCommitChanged && null!=conn && conn.isClosed()==false)
				{
					conn.setAutoCommit(priorAutoCommit);
				}
			}
			catch(Exception ignore) { }
			// if THIS method opened the connection (caller passed null/closed), close it here too
			try
			{
				if(createdConn && null!=conn && conn.isClosed()==false)
				{
					conn.close();
				}
			}
			catch(Exception ignore) { }
		}
		logger.info("importManualTypeDetails (batch) :: Method Ends. saveSuccess="+result.getSaveSuccessRows().size()
				+", saveFail="+result.getSaveFailureRows().size()+", deleteSuccess="+result.getDeleteSuccessRows().size()
				+", deleteFail="+result.getDeleteFailureRows().size());
		return result;
	}

	/*
	 * Resolve the primary key for a row from its unique combination
	 * (country locale + manual language + lower(manual code)), ignoring soft-deleted rows.
	 * Returns 0 when no active row matches.
	 */
	private static long findExistingId(PreparedStatement psSelect, ManualTypeDetails details, String deleteFlag) throws SQLException
	{
		long id = 0;
		ResultSet rs = null;
		try
		{
			psSelect.setLong(1, details.getCountryLocaleId());
			psSelect.setLong(2, details.getManualLanguageId());
			psSelect.setString(3, details.getManualTypeCode()==null ? "" : details.getManualTypeCode().trim().toLowerCase());
			psSelect.setString(4, deleteFlag);
			rs = psSelect.executeQuery();
			if(rs.next())
			{
				id = rs.getLong("dc_manual_mt_id");
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
		}
		return id;
	}

	private static void trimForImport(ManualTypeDetails details)
	{
		if(null!=details.getCountryLocaleCode())
		{
			details.setCountryLocaleCode(details.getCountryLocaleCode().trim());
		}
		if(null!=details.getManualLanguageCode())
		{
			details.setManualLanguageCode(details.getManualLanguageCode().trim());
		}
		if(null!=details.getManualTypeCode())
		{
			details.setManualTypeCode(details.getManualTypeCode().trim());
		}
		if(null!=details.getManualTypeRefKey())
		{
			details.setManualTypeRefKey(details.getManualTypeRefKey().trim());
		}
		if(null!=details.getManualTypeName())
		{
			details.setManualTypeName(details.getManualTypeName().trim());
		}
	}

}
