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
import com.mazda.gms3.mdm.vo.MissionBookDetails;

public class MissionBookDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(MissionBookDAO.class);
	
	public static ArrayList<MissionBookDetails> getMissionBookDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getMissionBookDetailsList :: Method Starts.");
		ArrayList<MissionBookDetails> bookList = new ArrayList<MissionBookDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_trans_book A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND A.mdm_ml_id="+new Long(langCode).longValue()+" AND"
								+ " A.mdm_transbk_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				
				
//				logger.info("getMissionBookDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					MissionBookDetails bookDetails = new MissionBookDetails();
					bookDetails.setSrNo(bookList.size()+1);
					bookDetails.setBookId(rs.getLong("mdm_transbk_id"));
					bookDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					bookDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						bookDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_transbk_code"))
					{
						bookDetails.setBookCode(rs.getString("mdm_transbk_code").trim());
					}
					if(null!=rs.getString("mdm_transbk_name_eng_lang"))
					{
						bookDetails.setBookNameEng(rs.getString("mdm_transbk_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_transbk_name_regional_lang"))
					{
						bookDetails.setBookNameReg(rs.getString("mdm_transbk_name_regional_lang").trim());
					}
					bookDetails.setFlag(rs.getString("mdm_transbk_flag"));
					bookDetails.setEntryTime(rs.getTimestamp("mdm_transbk_created_tmstp"));
					bookDetails.setUpdatedTime(rs.getTimestamp("mdm_transbk_updated_tmstp"));
					if(null!=bookDetails.getFlag() && !"".equals(bookDetails.getFlag()) && 
							bookDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						bookDetails.setShowCheckBox(false);
					}
					bookList.add(bookDetails);
					bookDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getMissionBookDetailsList :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getMissionBookDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "getMissionBookDetailsList()", e);
			logger.info("getMissionBookDetailsList :: ################ Exception ################");
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
//		logger.info("getMissionBookDetailsList :: Method Ends.");
		return bookList;
	}

	public static ArrayList<MissionBookDetails> getMissionBookDetailsListForCombo(String langCode) throws SQLException 
	{
//		logger.info("getMissionBookDetailsListForCombo :: Method Starts.");
		ArrayList<MissionBookDetails> bookList = new ArrayList<MissionBookDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT mdm_transbk_id,mdm_transbk_code,mdm_transbk_name_eng_lang "
						+ "FROM gms3_mdm_trans_book WHERE "
						+ "mdm_transbk_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"') "
								+ "AND mdm_ml_id="+new Long(langCode).longValue();
				
//				logger.info("getMissionBookDetailsListForCombo :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					MissionBookDetails bookDetails = new MissionBookDetails();
					bookDetails.setSrNo(bookList.size()+1);
					bookDetails.setBookId(rs.getLong("mdm_transbk_id"));
					bookDetails.setBookCode(rs.getString("mdm_transbk_code"));
					String code= rs.getString("mdm_transbk_code");
					String description=rs.getString("mdm_transbk_name_eng_lang");
					String displayString="";
					if(null!=description && !"".equals(description))
					{
						displayString = description.trim();
					}
					if(null!=code && !"".equals(code))
					{
						displayString = displayString+" {"+code.trim()+"}";
					}
					
					bookDetails.setBookNameEng(displayString);
					
					bookList.add(bookDetails);
					bookDetails=  null;

					code =null;
					displayString =null;
					description= null;
				}
				sql = null;
			}
			else
			{
				logger.info("getMissionBookDetailsListForCombo :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getMissionBookDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "getMissionBookDetailsListForCombo()", e);
			logger.info("getMissionBookDetailsListForCombo :: ################ Exception ################");
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
//		logger.info("getMissionBookDetailsListForCombo :: Method Ends.");
		return bookList;
	}

	public static boolean saveMissionBookDetails(MissionBookDetails bookDetails) throws SQLException
	{
		logger.info("saveMissionBookDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=bookDetails.getBookCode())
			{
				bookDetails.setBookCode(bookDetails.getBookCode().trim());
			}
			if(null!=bookDetails.getBookNameEng())
			{
				bookDetails.setBookNameEng(bookDetails.getBookNameEng().trim());
			}
			if(null!=bookDetails.getBookNameReg())
			{
				bookDetails.setBookNameReg(bookDetails.getBookNameReg().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_trans_book(mdm_cl_id,mdm_ml_id,mdm_transbk_code,mdm_transbk_name_eng_lang,mdm_transbk_name_regional_lang,"
					+ "mdm_transbk_flag,mdm_transbk_created_tmstp) VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, bookDetails.getCountryLocaleId());
			pstmt.setLong(2, bookDetails.getManualLanguageId());
			pstmt.setString(3, bookDetails.getBookCode());
			pstmt.setString(4, bookDetails.getBookNameEng());
			pstmt.setString(5, bookDetails.getBookNameReg());
			pstmt.setString(6, bookDetails.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveMissionBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "saveMissionBookDetails()", e);
			logger.info("saveMissionBookDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			bookDetails = null;
		}
		logger.info("saveMissionBookDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<MissionBookDetails> updateMissionBookDetails(ArrayList<MissionBookDetails> list) throws SQLException
	{
		logger.info("updateMissionBookDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				String sql="";
				MissionBookDetails bookDetails = new  MissionBookDetails();
				for(int a=0;a<list.size();a++)
				{
					bookDetails  = (MissionBookDetails)list.get(a);
					if(null!=bookDetails.getBookCode())
					{
						bookDetails.setBookCode(bookDetails.getBookCode().trim());
					}
					if(null!=bookDetails.getBookNameEng())
					{
						bookDetails.setBookNameEng(bookDetails.getBookNameEng().trim());
					}
					if(null!=bookDetails.getBookNameReg())
					{
						bookDetails.setBookNameReg(bookDetails.getBookNameReg().trim());
					}
					sql="UPDATE gms3_mdm_trans_book SET mdm_transbk_code=?,mdm_transbk_name_eng_lang =?,mdm_transbk_name_regional_lang =?,"
							+ "mdm_transbk_flag=?,mdm_transbk_updated_tmstp=? WHERE mdm_transbk_id =?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, bookDetails.getBookCode());
					pstmt.setString(2, bookDetails.getBookNameEng());
					pstmt.setString(3, bookDetails.getBookNameReg());
					pstmt.setString(4, bookDetails.getFlag());
					pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(6, bookDetails.getBookId());
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt=null;
					bookDetails.setSaveStatusWhileImport(true);
				}
			}
			else
			{
				logger.info("updateMissionBookDetails :: List as Parameter is null. Nothing to update. ");
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "updateMissionBookDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateMissionBookDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteMissionBookDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteMissionBookDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_trans_book SET mdm_transbk_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_transbk_updated_tmstp = ?  WHERE mdm_transbk_id = "+ tokens[i].toString();
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
				logger.info("deleteMissionBookDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteMissionBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "deleteMissionBookDetails()", e);
			logger.info("deleteMissionBookDetails :: ################ Exception ################");
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
		logger.info("deleteMissionBookDetails :: Method Ends.");
		return true;
	}

	public static boolean activeMissionBookDetails(String activeIds) throws SQLException
	{
		logger.info("activeMissionBookDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_trans_book SET mdm_transbk_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_transbk_updated_tmstp = ?  WHERE mdm_transbk_id = "+ tokens[i].toString();
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
				logger.info("activeMissionBookDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeMissionBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "activeMissionBookDetails()", e);
			logger.info("activeMissionBookDetails :: ################ Exception ################");
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
		logger.info("activeMissionBookDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<MissionBookDetails> importMissionBookDetails(ArrayList<MissionBookDetails> list) throws SQLException
	{
		logger.info("importMissionBookDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				/*
				 * ITERATE AND PROCESS EACH ROW.
				 */
				String getMissionBookSql = "";
				String sql="";
				MissionBookDetails details = new MissionBookDetails();
				long autoMissionBookId=0;
				for(int a=0;a<list.size();a++)
				{
					details = (MissionBookDetails)list.get(a);
					if(null!=details.getBookCode())
					{
						details.setBookCode(details.getBookCode().trim());
					}
					if(null!=details.getBookNameReg())
					{
						details.setBookNameReg(details.getBookNameReg().trim());
					}
					if(null!=details.getBookNameEng())
					{
						details.setBookNameEng(details.getBookNameEng().trim());
					}
					
					try
					{
						/*
						 * CHECK WHETHER VIN EXISTS OR NOT
						 * IF YES -  THEN UPDATE VIN
						 * ELSE - INSERT VIN
						 */
						getMissionBookSql = "SELECT * FROM gms3_mdm_trans_book WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_transbk_code=?   "
								+ " AND mdm_transbk_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importMissionBookDetails :: getMissionBookSql :: > " + getMissionBookSql);
						pstmt = conn.prepareStatement(getMissionBookSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setString(3, details.getBookCode());
						rs = pstmt.executeQuery();
						autoMissionBookId=0;
						if(rs.next())
						{
							autoMissionBookId= rs.getLong("mdm_transbk_id");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getMissionBookSql = null;
						
						if(autoMissionBookId>0)
						{
							pstmt = null;
							logger.info("importMissionBookDetails :: MissionBook Already Exists. Update Row for Auto MissionBook id : >" + autoMissionBookId);
							 sql="UPDATE gms3_mdm_trans_book SET mdm_transbk_code=?,mdm_transbk_name_eng_lang =?,mdm_transbk_name_regional_lang =?,"
										+ "mdm_transbk_flag=?,mdm_transbk_updated_tmstp=? WHERE mdm_transbk_id =?";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, details.getBookCode());
								pstmt.setString(2, details.getBookNameEng());
								pstmt.setString(3, details.getBookNameReg());
								pstmt.setString(4, details.getFlag());
								pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setLong(6, autoMissionBookId);
								pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							details.setSaveStatusWhileImport(true);
						}
						else
						{
							pstmt = null;
							logger.info("importMissionBookDetails :: MissionBook Does not Exists. Insert New Row.");
							
							sql="INSERT INTO gms3_mdm_trans_book(mdm_cl_id,mdm_ml_id,mdm_transbk_code,mdm_transbk_name_eng_lang,mdm_transbk_name_regional_lang,"
									+ "mdm_transbk_flag,mdm_transbk_created_tmstp) VALUES(?,?,?,?,?,?,?)";
							pstmt = conn.prepareStatement(sql);
							pstmt.setLong(1, details.getCountryLocaleId());
							pstmt.setLong(2, details.getManualLanguageId());
							pstmt.setString(3, details.getBookCode());
							pstmt.setString(4, details.getBookNameEng());
							pstmt.setString(5, details.getBookNameReg());
							pstmt.setString(6, details.getFlag());
							pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							details.setSaveStatusWhileImport(true);
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "importMissionBookDetails()", e);
					}
					
					autoMissionBookId = 0;
					details = null;
				}
				
			}
			else
			{
				logger.info("importMissionBookDetails :: No Mission Book Data Passed as parameter for Importing.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MissionBookDAO.class.getName(), "importMissionBookDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
		}
		logger.info("importMissionBookDetails :: Method Ends.");
		return list;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importMissionBookDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(MissionBookDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getMissionBookSql = null;
						getMissionBookSql = "SELECT * FROM gms3_mdm_trans_book WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_transbk_code=?   "
								+ " AND mdm_transbk_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importMissionBookDetails :: getMissionBookSql :: > " + getMissionBookSql);
						pstmt = conn.prepareStatement(getMissionBookSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setString(3, details.getBookCode());
						rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_transbk_id");
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