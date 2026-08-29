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
import com.mazda.gms3.sst.vo.SSTDetails;
import com.mazda.gms3.sst.vo.SectionModelMappingDetails;

public class SectionModelMappingDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SectionModelMappingDAO.class);
	
	public static ArrayList<SectionModelMappingDetails> getSectionModelMappingDetailsList(String divisionId) throws SQLException 
	{
//		logger.info("getSectionModelMappingDetailsList :: Method Starts.");
		ArrayList<SectionModelMappingDetails> mappingList = new ArrayList<SectionModelMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=divisionId && !"".equals(divisionId))
			{
				conn = getConnection();
				String sql ="SELECT A.*, B.mdm_sec_name,B.mdm_sec_code, C.mdm_ml_lang_code FROM gms3_sst_sec_model_mapping A, "
						+ " gms3_sst_section_master B, gms3_mdm_manual_language C WHERE A.mdm_sec_id = B.mdm_sec_id "
						+ " AND A.mdm_ml_id = C.mdm_ml_id  "
						+ " AND A.mdm_ssmm_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') "
								+ " AND B.mdm_div_id = "+ divisionId; 
//				logger.info("getSectionModelMappingDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SectionModelMappingDetails details = new SectionModelMappingDetails();
					details.setSrNo(mappingList.size()+1);
					details.setSectionModelMappingId(rs.getLong("mdm_ssmm_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						details.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_crln_code") && !"".equals(rs.getString("mdm_crln_code")))
					{
						details.setModelCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name") && !"".equals(rs.getString("mdm_crln_name")))
					{
						details.setModelName(rs.getString("mdm_crln_name").trim());
					}
					details.setSectionId(rs.getLong("mdm_sec_id"));
					if(null!=rs.getString("mdm_sec_name") && !"".equals(rs.getString("mdm_sec_name")))
					{
						details.setSectionName(rs.getString("mdm_sec_name").trim());
					}
					details.setSectionCode(String.valueOf(rs.getInt("mdm_sec_code")));
					details.setSstId(rs.getLong("mdm_sst_id"));
					if(null!=rs.getString("mdm_ssmm_remarks") && !"".equals(rs.getString("mdm_ssmm_remarks")))
					{
						details.setRemarks(rs.getString("mdm_ssmm_remarks").trim());
					}
					details.setFlag(rs.getString("mdm_ssmm_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_ssmm_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_ssmm_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					mappingList.add(details);
					details=  null;
				}
				sql = null;
				rs.close();rs = null;
				stmt.close();stmt = null;
				if(null!=mappingList && mappingList.size()>0)
				{
					for(int i=0;i<mappingList.size();i++)
					{
						SectionModelMappingDetails details = (SectionModelMappingDetails)mappingList.get(i);
						/*
						 * call function to get SST Details
						 */
						SSTDetails sstNumberDetails = SSTMasterDAO.getSSTDetails(String.valueOf(details.getSstId()), conn, details.getManualLanguageCode());
						if(null!=sstNumberDetails && null!=sstNumberDetails.getSstId() && sstNumberDetails.getSstId()>0)
						{
							details.setSstNumberDetails(new SSTDetails());
							details.setSstNumberDetails(sstNumberDetails);
						}
						sstNumberDetails= null;
					}
				}
			}
			else
			{
				logger.info("getSectionModelMappingDetailsList :: Division id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSectionModelMappingDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "getSectionModelMappingDetailsList()", e);
			logger.info("getSectionModelMappingDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			divisionId = null;
		}
//		logger.info("getSectionModelMappingDetailsList :: Method Ends.");
		return mappingList;
	}

	public static boolean saveSectionModelMappingDetails(SectionModelMappingDetails details) throws SQLException
	{
		logger.info("saveSectionModelMappingDetails :: Method Starts.");
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
			if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
			{
				details.setRemarks(details.getRemarks().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_sec_model_mapping(mdm_cl_id,mdm_ml_id, "
					+ " mdm_crln_code, mdm_crln_name, mdm_sec_id, "
					+ " mdm_ssmm_remarks,  mdm_ssmm_flag,"
					+ " mdm_ssmm_created_tmstp, mdm_sst_id";
			sql=sql+ " ) VALUES(?,?,?,?,?,?,?,?,?";
			sql = sql+")";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getModelCode());
			pstmt.setString(4, details.getModelName());
			pstmt.setLong(5, details.getSectionId());
			pstmt.setString(6, details.getRemarks());
			pstmt.setString(7, details.getFlag());
			pstmt.setTimestamp(8, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(9, details.getSstId());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveSectionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "saveSectionModelMappingDetails()", e);
			logger.info("saveSectionModelMappingDetails :: ################ Exception ################");
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
		logger.info("saveSectionModelMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSectionModelMappingDetails(SectionModelMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSectionModelMappingDetails :: Method Starts.");
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
			if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
			{
				details.setRemarks(details.getRemarks().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_sec_model_mapping SET mdm_crln_code=?, mdm_crln_name=?,"
					+ " mdm_sec_id = ?,"
					+ " mdm_ssmm_remarks =?, "
					+ " mdm_ssmm_flag=?,mdm_ssmm_updated_tmstp=?,mdm_sst_id=?  "
					+ "  WHERE mdm_ssmm_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getModelCode());
			pstmt.setString(2, details.getModelName());
			pstmt.setLong(3, details.getSectionId());
			pstmt.setString(4, details.getRemarks());
			pstmt.setString(5, details.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(7, details.getSstId());
			pstmt.setLong(8, details.getSectionModelMappingId());
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
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "updateSectionModelMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSectionModelMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSectionModelMappingDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSectionModelMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_sec_model_mapping SET mdm_ssmm_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_ssmm_updated_tmstp = ?  WHERE mdm_ssmm_id = "+ tokens[i].toString();
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
				logger.info("deleteSectionModelMappingDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSectionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "deleteSectionModelMappingDetails()", e);
			logger.info("deleteSectionModelMappingDetails :: ################ Exception ################");
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
		logger.info("deleteSectionModelMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSectionModelMappingDetails(String activeIds) throws SQLException
	{
		logger.info("activeSectionModelMappingDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_sec_model_mapping SET mdm_ssmm_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_ssmm_updated_tmstp = ?  WHERE mdm_ssmm_id = "+ tokens[i].toString();
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
				logger.info("activeSectionModelMappingDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSectionModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "activeSectionModelMappingDetails()", e);
			logger.info("activeSectionModelMappingDetails :: ################ Exception ################");
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
		logger.info("activeSectionModelMappingDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSectionModelMappingDetails(SectionModelMappingDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSectionModelMappingDetails :: Method Starts.");
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
			 * CHECK WHETHER VIN EXISTS OR NOT
			 * IF YES -  THEN UPDATE VIN
			 * ELSE - INSERT VIN
			 */
			
			/*
			 * if modelCode, sstId, secCode for a language exists, update it
			 */
			String getSectionMappingSql = "SELECT * FROM gms3_sst_sec_model_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ "  TRIM(LOWER(mdm_crln_code)) = ? AND mdm_sec_id = ?  AND mdm_sst_id=? ";
			getSectionMappingSql=getSectionMappingSql+ " AND mdm_ssmm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSectionModelMappingDetails :: getSectionMappingSql :: > " + getSectionMappingSql);
			pstmt = conn.prepareStatement(getSectionMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getModelCode().trim().toLowerCase());
			pstmt.setLong(4, details.getSectionId());
			pstmt.setLong(5, details.getSstId());
			rs = pstmt.executeQuery();
			long autoMappingId=0;
			if(rs.next())
			{
				autoMappingId= rs.getLong("mdm_ssmm_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getSectionMappingSql = null;
			
			if(autoMappingId>0)
			{
				pstmt = null;
				logger.info("importSectionModelMappingDetails :: Section Model Mapping Already Exists. Update Row for Auto Mapping id : >" + autoMappingId);
				
				String sql="UPDATE gms3_sst_sec_model_mapping SET mdm_crln_code=?, mdm_crln_name=?,"
						+ " mdm_sec_id = ?,"
						+ " mdm_ssmm_remarks =?, "
						+ " mdm_ssmm_updated_tmstp=?,mdm_sst_id=?  "
						+ "  WHERE mdm_ssmm_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getModelCode());
				pstmt.setString(2, details.getModelName());
				pstmt.setLong(3, details.getSectionId());
				pstmt.setString(4, details.getRemarks());
				pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(6, details.getSstId());
				pstmt.setLong(7, autoMappingId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSectionModelMappingDetails :: Section Model Mapping Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_sec_model_mapping(mdm_cl_id,mdm_ml_id, "
						+ " mdm_crln_code, mdm_crln_name, mdm_sec_id, "
						+ " mdm_ssmm_remarks,  mdm_ssmm_flag,"
						+ " mdm_ssmm_created_tmstp, mdm_sst_id";
				sql=sql+ " ) VALUES(?,?,?,?,?,?,?,?,?";
				sql = sql+")";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getModelCode());
				pstmt.setString(4, details.getModelName());
				pstmt.setLong(5, details.getSectionId());
				pstmt.setString(6, details.getRemarks());
				pstmt.setString(7, details.getFlag());
				pstmt.setTimestamp(8, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(9, details.getSstId());
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
			Utilities.printStackTraceToLogs(SectionModelMappingDAO.class.getName(), "importSectionModelMappingDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSectionModelMappingDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSectionModelMappingDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SectionModelMappingDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getSectionMappingSql = "SELECT * FROM gms3_sst_sec_model_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ "  TRIM(LOWER(mdm_crln_code)) = ? AND mdm_sec_id = ?  AND mdm_sst_id=? ";
			getSectionMappingSql=getSectionMappingSql+ " AND mdm_ssmm_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSectionModelMappingDetails :: getSectionMappingSql :: > " + getSectionMappingSql);
			pstmt = conn.prepareStatement(getSectionMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getModelCode()));
			pstmt.setLong(4, details.getSectionId());
			pstmt.setLong(5, details.getSstId());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_ssmm_id");
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
