package com.mazda.gms3.sst.dao;

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
import com.mazda.gms3.sst.vo.SectionDetails;

public class SectionDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SectionDAO.class);
	
	public static ArrayList<SectionDetails> getSectionDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getSectionDetailsList :: Method Starts.");
		ArrayList<SectionDetails> sectionList = new ArrayList<SectionDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_div_name, B.mdm_div_code FROM gms3_sst_section_master A, gms3_sst_division_master B WHERE "
						+ " A.mdm_sec_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') AND A.mdm_div_id=B.mdm_div_id ";
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND A.mdm_ml_id="+languageId;
				}
//				logger.info("getSectionDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SectionDetails details = new SectionDetails();
					details.setSrNo(sectionList.size()+1);
					details.setSectionId(rs.getLong("mdm_sec_id"));
					details.setDivisionId(rs.getLong("mdm_div_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					Integer sortId = rs.getInt("mdm_sec_sort_id");
					if(null!=sortId)
					{
						details.setSortId(String.valueOf(sortId));
					}
					if(null!=rs.getString("mdm_div_name") && !"".equals(rs.getString("mdm_div_name")))
					{
						details.setDivisionName(rs.getString("mdm_div_name").trim());
					}
					if(null!=rs.getString("mdm_div_code") && !"".equals(rs.getString("mdm_div_code")))
					{
						details.setDivisionCode(rs.getString("mdm_div_code").trim());
					}
					if(null!=rs.getString("mdm_sec_name") && !"".equals(rs.getString("mdm_sec_name")))
					{
						details.setSectionName(rs.getString("mdm_sec_name").trim());
					}
					Integer secIndex = rs.getInt("mdm_sec_index");
					if(null!=secIndex)
					{
						details.setSectionIndex(String.valueOf(secIndex));
					}
//					details.setHtmlPath(rs.getString("mdm_sec_html_path"));
					details.setFlag(rs.getString("mdm_sec_flag"));
					Integer secCode = rs.getInt("mdm_sec_code");
					if(null!=secCode)
					{
						details.setSectionCode(secCode);
					}
					details.setEntryTime(rs.getTimestamp("mdm_sec_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sec_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					sectionList.add(details);
					details=  null;
					sortId =null;
					secIndex =null;
					secCode = null;
				}
				sql = null;
			}
			else
			{
				logger.info("getSectionDetailsList :: Division id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSectionDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "getSectionDetailsList()", e);
			logger.info("getSectionDetailsList :: ################ Exception ################");
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
//		logger.info("getSectionDetailsList :: Method Ends.");
		return sectionList;
	}

	public static ArrayList<SectionDetails> getSectionDetailsListForCombo(String languageId, String divisionId) throws SQLException 
	{
//		logger.info("getSectionDetailsList :: Method Starts.");
		ArrayList<SectionDetails> sectionList = new ArrayList<SectionDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT * FROM gms3_sst_section_master WHERE "
					+ " mdm_sec_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"')  ";
			
			if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
			{
				sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
			}
			if(null!=divisionId && !"".equals(divisionId) && !"null".equals(divisionId.trim().toLowerCase()))
			{
				sql = sql+" AND mdm_div_id="+new Long(divisionId).longValue();
			}
//			logger.info("getSectionDetailsList :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SectionDetails details = new SectionDetails();
				details.setSrNo(sectionList.size()+1);
				details.setSectionId(rs.getLong("mdm_sec_id"));
				if(null!=rs.getString("mdm_sec_name") && !"".equals(rs.getString("mdm_sec_name")))
				{
					details.setSectionName(rs.getString("mdm_sec_name").trim());
				}
				Integer secCode = rs.getInt("mdm_sec_code");
				if(null!=secCode)
				{
					details.setSectionCode(secCode);
				}
				secCode =null;
				sectionList.add(details);
				details=  null;
			}
			sql = null;
		
		}
		catch(Exception e)
		{
			logger.info("getSectionDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "getSectionDetailsList()", e);
			logger.info("getSectionDetailsList :: ################ Exception ################");
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
//		logger.info("getSectionDetailsList :: Method Ends.");
		return sectionList;
	}
	
	public static boolean saveSectionDetails(SectionDetails details) throws SQLException
	{
		logger.info("saveSectionDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			/*
			 * 0 IS ALLOWED AS SECTION INDEX & SORT ID
			 */
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			Integer sectionIndex=null;
			if(null!=details.getSectionIndex() && !"".equals(details.getSectionIndex()))
			{
				sectionIndex=  new Integer(details.getSectionIndex()).intValue();
			}
			
			if(null!=details.getSectionName() && !"".equals(details.getSectionName()))
			{
				details.setSectionName(details.getSectionName().trim());
			}
			
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_section_master(mdm_cl_id,mdm_ml_id,mdm_div_id,mdm_sec_index,"
					+ " mdm_sec_html_path, mdm_sec_sort_id, mdm_sec_name, mdm_sec_flag,"
					+ " mdm_sec_created_tmstp, mdm_sec_code)"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setLong(3, details.getDivisionId());
			pstmt.setInt(4, sectionIndex);
//			pstmt.setString(5, details.getHtmlPath());
			pstmt.setString(5, "");
			pstmt.setInt(6, sortId);
			pstmt.setString(7, details.getSectionName());
			pstmt.setString(8, details.getFlag());
			pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setInt(10, details.getSectionCode());
			pstmt.executeUpdate();
			sql = null;
			sortId = null;
			sectionIndex= null;
		}
		catch(Exception e)
		{
			logger.info("saveSectionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "saveSectionDetails()", e);
			logger.info("saveSectionDetails :: ################ Exception ################");
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
		logger.info("saveSectionDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSectionDetails(SectionDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSectionDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			/*
			 * 0 ALLOWED AS SORT ID & SECTION INDEX
			 */
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			Integer sectionIndex=null;
			if(null!=details.getSectionIndex() && !"".equals(details.getSectionIndex()))
			{
				sectionIndex=  new Integer(details.getSectionIndex()).intValue();
			}
			if(null!=details.getSectionName() && !"".equals(details.getSectionName()))
			{
				details.setSectionName(details.getSectionName().trim());
			}
			
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_section_master SET mdm_div_id=?, mdm_sec_index=?, mdm_sec_html_path=?, "
					+ " mdm_sec_sort_id =?,mdm_sec_name =?,"
					+ " mdm_sec_flag=?,mdm_sec_updated_tmstp=?, mdm_sec_code=?  WHERE mdm_sec_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getDivisionId());
			pstmt.setInt(2, sectionIndex);
//			pstmt.setString(3, details.getHtmlPath());
			pstmt.setString(3, "");
			pstmt.setInt(4, sortId);
			pstmt.setString(5, details.getSectionName());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setInt(8, details.getSectionCode());
			pstmt.setLong(9, details.getSectionId());
			pstmt.executeUpdate();
			sql = null;
			sortId=  null;
			sectionIndex= null;
			
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
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "updateSectionDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSectionDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSectionDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSectionDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_section_master SET mdm_sec_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sec_updated_tmstp = ?  WHERE mdm_sec_id = "+ tokens[i].toString();
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
				logger.info("deleteSectionDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSectionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "deleteSectionDetails()", e);
			logger.info("deleteSectionDetails :: ################ Exception ################");
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
		logger.info("deleteSectionDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSectionDetails(String activeIds) throws SQLException
	{
		logger.info("activeSectionDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_section_master SET mdm_sec_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_sec_updated_tmstp = ?  WHERE mdm_sec_id = "+ tokens[i].toString();
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
				logger.info("activeSectionDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSectionDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "activeSectionDetails()", e);
			logger.info("activeSectionDetails :: ################ Exception ################");
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
		logger.info("activeSectionDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSectionDetails(SectionDetails details,Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSectionDetails :: Method Starts.");
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
			 * SORT ID & SECTION INDEX CAN BE 0
			 */
			Integer sortId=null;
			if(null!=details.getSortId() && !"".equals(details.getSortId()))
			{
				sortId = new Integer(details.getSortId()).intValue();
			}
			Integer sectionIndex=null;
			if(null!=details.getSectionIndex() && !"".equals(details.getSectionIndex()))
			{
				sectionIndex=  new Integer(details.getSectionIndex()).intValue();
			}
			if(null!=details.getSectionName() && !"".equals(details.getSectionName()))
			{
				details.setSectionName(details.getSectionName().trim());
			}
			/*
			 * HERE, WE HAVE UNIQUE ENTITY AS SECTION CODE FOR A LANGUAGE
			 * SO SEARCH ON IT, IF ENTRY FOUND - THEN UPDATE THE ROW
			 * NO MATTER WHAT DIVISION OR SORT ID MAPPED TO IT
			 */
			
			String getSectionSql = "SELECT * FROM gms3_sst_section_master WHERE mdm_cl_id = ? AND  "
					+ " mdm_ml_id=? AND mdm_sec_code =? ";
			getSectionSql  =getSectionSql+"  AND mdm_sec_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSectionDetails :: getDivisionSql :: > " + getDivisionSql);
			pstmt = conn.prepareStatement(getSectionSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setInt(3, details.getSectionCode());
			rs = pstmt.executeQuery();
			long autoSectionId=0;
			if(rs.next())
			{
				autoSectionId= rs.getLong("mdm_sec_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getSectionSql = null;
//			htmlPath= null;
			if(autoSectionId>0)
			{
				pstmt = null;
				logger.info("importSectionDetails :: Section Already Exists. Update Row for Auto Division id : >" + autoSectionId);
				
				String sql="UPDATE gms3_sst_section_master SET mdm_div_id=?, mdm_sec_index=?, mdm_sec_html_path=?, "
						+ " mdm_sec_sort_id =?,mdm_sec_name =?,"
						+ " mdm_sec_updated_tmstp=?,mdm_sec_code=?  WHERE mdm_sec_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getDivisionId());
				pstmt.setInt(2, sectionIndex);
//				pstmt.setString(3, details.getHtmlPath());
				pstmt.setString(3, "");
				pstmt.setInt(4, sortId);
				pstmt.setString(5, details.getSectionName());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setInt(7, details.getSectionCode());
				pstmt.setLong(8, autoSectionId);
				pstmt.executeUpdate();
				
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSectionDetails :: Section Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_section_master(mdm_cl_id,mdm_ml_id,mdm_div_id,mdm_sec_index,"
						+ " mdm_sec_html_path, mdm_sec_sort_id, mdm_sec_name, mdm_sec_flag,"
						+ " mdm_sec_created_tmstp,mdm_sec_code)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setLong(3, details.getDivisionId());
				pstmt.setInt(4, sectionIndex);
//				pstmt.setString(5, details.getHtmlPath());
				pstmt.setString(5, "");
				pstmt.setInt(6, sortId);
				pstmt.setString(7, details.getSectionName());
				pstmt.setString(8, details.getFlag());
				pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setInt(10, details.getSectionCode());
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
			Utilities.printStackTraceToLogs(SectionDAO.class.getName(), "importSectionDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSectionDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSectionDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SectionDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getSectionSql = "SELECT * FROM gms3_sst_section_master WHERE mdm_cl_id = ? AND  "
					+ " mdm_ml_id=? AND mdm_sec_code =? ";
			getSectionSql  =getSectionSql+"  AND mdm_sec_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSectionDetails :: getDivisionSql :: > " + getDivisionSql);
			pstmt = conn.prepareStatement(getSectionSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setInt(3, details.getSectionCode());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sec_id");
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