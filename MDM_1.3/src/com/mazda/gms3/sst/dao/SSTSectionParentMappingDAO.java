package com.mazda.gms3.sst.dao;

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
import com.mazda.gms3.sst.vo.SSTSectionParentMappingDetails;

public class SSTSectionParentMappingDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SSTSectionParentMappingDAO.class);
	
	public static ArrayList<SSTSectionParentMappingDetails> getSSTSectionParentMappingDetailsList(String languageId, String divisionId) throws SQLException 
	{
//		logger.info("getSSTSectionParentMappingDetailsList :: Method Starts.");
		ArrayList<SSTSectionParentMappingDetails> sstSectionParentMappingList = new ArrayList<SSTSectionParentMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId) && null!=divisionId && !"".equals(divisionId))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_sst_number, C.mdm_sec_name, C.mdm_sec_code "
						+ "  FROM gms3_sst_parent_mapping A, gms3_sst_master B, gms3_sst_section_master C"
						+ "  WHERE A.mdm_sstpm_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') "
						+ " AND A.mdm_sst_id = B.mdm_sst_id AND A.mdm_sec_id = C.mdm_sec_id AND C.mdm_div_id="+divisionId;
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND A.mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getSSTSectionParentMappingDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTSectionParentMappingDetails details = new SSTSectionParentMappingDetails();
					details.setSrNo(sstSectionParentMappingList.size()+1);
					details.setSstSectionParentMappingId(rs.getLong("mdm_sstpm_id"));
					details.setSstId(rs.getLong("mdm_sst_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					details.setSectionId(rs.getLong("mdm_sec_id"));
					details.setSectionCode(String.valueOf(rs.getInt("mdm_sec_code")));
					if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
					{
						details.setSstNumber(rs.getString("mdm_sst_number").trim());
					}
					if(null!=rs.getString("mdm_sec_name") && !"".equals(rs.getString("mdm_sec_name")))
					{
						details.setSectionName(rs.getString("mdm_sec_name").trim());
					}
					if(null!=rs.getString("mdm_sstpm_remarks") && !"".equals(rs.getString("mdm_sstpm_remarks")))
					{
						details.setRemarks(rs.getString("mdm_sstpm_remarks").trim());
					}
					details.setParentSSTId(rs.getLong("mdm_sst_parent_id"));
					details.setFlag(rs.getString("mdm_sstpm_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sstpm_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sstpm_updated_tmstp"));
					
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					sstSectionParentMappingList.add(details);
					details=  null;
				}
				sql = null;
				rs.close();rs= null;
				stmt.close();stmt = null;
				
				if(null!=sstSectionParentMappingList && sstSectionParentMappingList.size()>0)
				{
					for(int a=0;a<sstSectionParentMappingList.size();a++)
					{
						SSTSectionParentMappingDetails details = (SSTSectionParentMappingDetails)sstSectionParentMappingList.get(a);
						if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
						{
							stmt = null;
							rs = null;
							String getParentSQL ="SELECT mdm_sst_number FROM gms3_sst_master WHERE mdm_sst_id = "+ details.getParentSSTId();
							stmt = conn.createStatement();
							rs = stmt.executeQuery(getParentSQL);
							if(rs.next())
							{
								if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
								{
									details.setParentSSTNumber(rs.getString("mdm_sst_number").trim());
								}
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							getParentSQL= null;
						}
					}
				}
			}
			else
			{
				logger.info("getSSTSectionParentMappingDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTSectionParentMappingDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "getSSTSectionParentMappingDetailsList()", e);
			logger.info("getSSTSectionParentMappingDetailsList :: ################ Exception ################");
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
//		logger.info("getSSTSectionParentMappingDetailsList :: Method Ends.");
		return sstSectionParentMappingList;
	}
	
	public static boolean saveSSTSectionParentMappingDetails(SSTSectionParentMappingDetails details) throws SQLException
	{
		logger.info("saveSSTSectionParentMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
			{
				details.setRemarks(details.getRemarks().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_parent_mapping(mdm_cl_id,mdm_ml_id,mdm_sec_id,mdm_sst_id, "
					+ " mdm_sstpm_remarks, mdm_sstpm_flag,"
					+ " mdm_sstpm_created_tmstp";
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				sql = sql+" ,mdm_sst_parent_id";
			}
			sql = sql+") VALUES(?,?,?,?,?,?,?";
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				sql = sql+" ,?";
			}
			sql = sql+")";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSectionId());
			pstmt.setLong(4, details.getSstId());
			pstmt.setString(5, details.getRemarks());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			if(null!=details.getParentSSTId())
			{
				pstmt.setLong(8, details.getParentSSTId());
			}
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveSSTSectionParentMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "saveSSTSectionParentMappingDetails()", e);
			logger.info("saveSSTSectionParentMappingDetails :: ################ Exception ################");
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
		logger.info("saveSSTSectionParentMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSSTSectionParentMappingDetails(SSTSectionParentMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSSTSectionParentMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Statement stmt =null;
		try
		{
			if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
			{
				details.setRemarks(details.getRemarks().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_parent_mapping SET mdm_sec_id=?, mdm_sst_id=?,"
					+ " mdm_sstpm_remarks =?, "
					+ " mdm_sstpm_flag=?,mdm_sstpm_updated_tmstp=? , mdm_sst_parent_id =?  WHERE mdm_sstpm_id =? ";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getSectionId());
			pstmt.setLong(2, details.getSstId());
			pstmt.setString(3, details.getRemarks());
			pstmt.setString(4, details.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				pstmt.setLong(6, details.getParentSSTId());
			}
			else
			{
				pstmt.setNull(6, Types.LONGNVARCHAR);
			}
			pstmt.setLong(7, details.getSstSectionParentMappingId());
			pstmt.executeUpdate();
			sql = null;
			pstmt.close();pstmt=null;
			
			if(null==details.getParentSSTId() || details.getParentSSTId()<=0)
			{
				// EXPLICITY SET PARENT SST ID TO NULL
				sql="UPDATE gms3_sst_parent_mapping SET mdm_sst_parent_id = NULL WHERE mdm_sstpm_id="+ details.getSstSectionParentMappingId();
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				sql = null;
				stmt.close();stmt=null;
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
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "updateSSTSectionParentMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSSTSectionParentMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSSTSectionParentMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSSTSectionParentMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_parent_mapping SET mdm_sstpm_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sstpm_updated_tmstp = ?  WHERE mdm_sstpm_id = "+ tokens[i].toString();
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
				logger.info("deleteSSTSectionParentMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSSTSectionParentMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "deleteSSTSectionParentMappingDetails()", e);
			logger.info("deleteSSTSectionParentMappingDetails :: ################ Exception ################");
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
		logger.info("deleteSSTSectionParentMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSSTSectionParentMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeSSTSectionParentMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_parent_mapping SET mdm_sstpm_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " , mdm_sstpm_updated_tmstp = ?  WHERE mdm_sstpm_id = "+ tokens[i].toString();
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
				logger.info("activeSSTSectionParentMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSSTSectionParentMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "activeSSTSectionParentMappingDetails()", e);
			logger.info("activeSSTSectionParentMappingDetails :: ################ Exception ################");
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
		logger.info("activeSSTSectionParentMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSSTSectionParentMappingDetails(SSTSectionParentMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSSTSectionParentMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
			{
				details.setRemarks(details.getRemarks().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			rs = null;
			pstmt= null;
			/*
			 * IF SEC_ID & SST_ID ALREADY EXISTS - THE UPDATE THE ROW
			 * NO MATTER PARENT ID FOUND OR NOT.
			 */
			String getSSTSql = "SELECT * FROM gms3_sst_parent_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_sec_id =? AND mdm_sst_id=? ";
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				getSSTSql =getSSTSql+"AND mdm_sst_parent_id = ?  ";
			}
			else
			{
				getSSTSql = getSSTSql+"AND mdm_sst_parent_id IS NULL  ";
			}
			getSSTSql  = getSSTSql + " AND mdm_sstpm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importSSTSectionParentMappingDetails :: getSSTSql :: > " + getSSTSql);
			pstmt = conn.prepareStatement(getSSTSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSectionId());
			pstmt.setLong(4, details.getSstId());
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				pstmt.setLong(5, details.getParentSSTId());
			}
			rs = pstmt.executeQuery();
			long autoMappingId=0;
			if(rs.next())
			{
				autoMappingId= rs.getLong("mdm_sstpm_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getSSTSql = null;
			
			if(autoMappingId>0)
			{
				pstmt = null;
				logger.info("importSSTSectionParentMappingDetails :: SSTSectionParentMapping Already Exists. Update Row for Auto Mapping id : >" + autoMappingId);
				
				String sql="UPDATE gms3_sst_parent_mapping SET mdm_sec_id=?, mdm_sst_id=?,"
						+ " mdm_sstpm_remarks =?, "
						+ " mdm_sstpm_updated_tmstp=? ";
				sql = sql+" , mdm_sst_parent_id =?  WHERE mdm_sstpm_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getSectionId());
				pstmt.setLong(2, details.getSstId());
				pstmt.setString(3, details.getRemarks());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
				{
					pstmt.setLong(5, details.getParentSSTId());
				}
				else
				{
					pstmt.setNull(5, Types.LONGNVARCHAR);
				}
				pstmt.setLong(6, autoMappingId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
			}
			else
			{
				pstmt = null;
				logger.info("importSSTSectionParentMappingDetails :: SSTSectionParentMapping Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_parent_mapping(mdm_cl_id,mdm_ml_id,mdm_sec_id,mdm_sst_id, "
						+ " mdm_sstpm_remarks, mdm_sstpm_flag,"
						+ " mdm_sstpm_created_tmstp";
				if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
				{
					sql = sql+" ,mdm_sst_parent_id";
				}
				sql = sql+") VALUES(?,?,?,?,?,?,?";
				if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
				{
					sql = sql+" ,?";
				}
				sql = sql+")";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setLong(3, details.getSectionId());
				pstmt.setLong(4, details.getSstId());
				pstmt.setString(5, details.getRemarks());
				pstmt.setString(6, details.getFlag());
				pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
				if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
				{
					pstmt.setLong(8, details.getParentSSTId());
				}
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
			Utilities.printStackTraceToLogs(SSTSectionParentMappingDAO.class.getName(), "importSSTSectionParentMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSSTSectionParentMappingDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSSTSectionParentMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SSTSectionParentMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getSSTSql = "SELECT * FROM gms3_sst_parent_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_sec_id =? AND mdm_sst_id=? ";
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				getSSTSql =getSSTSql+"AND mdm_sst_parent_id = ?  ";
			}
			else
			{
				getSSTSql = getSSTSql+"AND mdm_sst_parent_id IS NULL  ";
			}
			getSSTSql  = getSSTSql + " AND mdm_sstpm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importSSTSectionParentMappingDetails :: getSSTSql :: > " + getSSTSql);
			pstmt = conn.prepareStatement(getSSTSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getSectionId());
			pstmt.setLong(4, details.getSstId());
			if(null!=details.getParentSSTId() && details.getParentSSTId()>0)
			{
				pstmt.setLong(5, details.getParentSSTId());
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sstpm_id");
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

