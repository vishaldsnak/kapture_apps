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
import com.mazda.gms3.mdm.vo.EngineBookDetails;

public class EngineBookDAO extends DBConnectionHelper{



	private static Logger logger = LogManager.getLogger(EngineBookDAO.class);
	
	public static ArrayList<EngineBookDetails> getEngineBookDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getEngineBookDetailsList :: Method Starts.");
		ArrayList<EngineBookDetails> bookList = new ArrayList<EngineBookDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_engine_book A, "
						+ " gms3_mdm_manual_language B WHERE A.mdm_ml_id = B.mdm_ml_id AND "
						+ "A.mdm_ml_id="+new Long(langCode).longValue()+" AND A.mdm_eb_flag "
								+ "NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				
//				logger.info("getEngineBookDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					EngineBookDetails bookDetails = new EngineBookDetails();
					bookDetails.setSrNo(bookList.size()+1);
					bookDetails.setBookId(rs.getLong("mdm_eb_id"));
					bookDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					bookDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						bookDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_eb_code"))
					{
						bookDetails.setBookCode(rs.getString("mdm_eb_code").trim());
					}
					if(null!=rs.getString("mdm_eb_name_eng_lang"))
					{
						bookDetails.setBookNameEng(rs.getString("mdm_eb_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_eb_name_regional_lang"))
					{
						bookDetails.setBookNameReg(rs.getString("mdm_eb_name_regional_lang").trim());
					}
					bookDetails.setFlag(rs.getString("mdm_eb_flag"));
					bookDetails.setEntryTime(rs.getTimestamp("mdm_eb_created_tmstp"));
					bookDetails.setUpdatedTime(rs.getTimestamp("mdm_eb_updated_tmstp"));
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
				logger.info("getEngineBookDetailsList :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getEngineBookDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "getEngineBookDetailsList()", e);
			logger.info("getEngineBookDetailsList :: ################ Exception ################");
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
//		logger.info("getEngineBookDetailsList :: Method Ends.");
		return bookList;
	}

	public static ArrayList<EngineBookDetails> getEngineBookDetailsListForCombo(String langCode) throws SQLException 
	{
//		logger.info("getEngineBookDetailsListForCombo :: Method Starts.");
		ArrayList<EngineBookDetails> bookList = new ArrayList<EngineBookDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT mdm_eb_id,mdm_eb_code,mdm_eb_name_eng_lang FROM gms3_mdm_engine_book "
						+ "WHERE mdm_eb_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"') "
								+ "AND mdm_ml_id="+new Long(langCode).longValue()+" ";
//				logger.info("getEngineBookDetailsListForCombo :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					EngineBookDetails bookDetails = new EngineBookDetails();
					bookDetails.setSrNo(bookList.size()+1);
					bookDetails.setBookId(rs.getLong("mdm_eb_id"));
					bookDetails.setBookCode(rs.getString("mdm_eb_code").trim());
					
					String code = rs.getString("mdm_eb_code");
					String description= rs.getString("mdm_eb_name_eng_lang");
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
					code=null;
					description= null;
					displayString= null;
				}
				sql = null;
			}
			else
			{
				logger.info("getEngineBookDetailsListForCombo :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getEngineBookDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "getEngineBookDetailsListForCombo()", e);
			logger.info("getEngineBookDetailsListForCombo :: ################ Exception ################");
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
//		logger.info("getEngineBookDetailsListForCombo :: Method Ends.");
		return bookList;
	}

	public static boolean saveEngineBookDetails(EngineBookDetails bookDetails) throws SQLException
	{
		logger.info("saveEngineBookDetails :: Method Starts.");
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
			String sql="INSERT INTO gms3_mdm_engine_book(mdm_cl_id,mdm_ml_id,mdm_eb_code,mdm_eb_name_eng_lang,mdm_eb_name_regional_lang,"
					+ "mdm_eb_flag,mdm_eb_created_tmstp) VALUES(?,?,?,?,?,?,?)";
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
			logger.info("saveEngineBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "saveEngineBookDetails()", e);
			logger.info("saveEngineBookDetails :: ################ Exception ################");
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
		logger.info("saveEngineBookDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<EngineBookDetails> updateEngineBookDetails(ArrayList<EngineBookDetails> list) throws SQLException
	{
		logger.info("updateEngineBookDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				String sql="";
				EngineBookDetails bookDetails = new EngineBookDetails();
				for(int a=0;a<list.size();a++)
				{
					bookDetails  = (EngineBookDetails)list.get(a);
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
					try
					{
						sql="UPDATE gms3_mdm_engine_book SET mdm_eb_code = ?,mdm_eb_name_eng_lang =?,mdm_eb_name_regional_lang =?,"
								+ "mdm_eb_flag=?,mdm_eb_updated_tmstp=? WHERE mdm_eb_id =?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, bookDetails.getBookCode());
						pstmt.setString(2, bookDetails.getBookNameEng());
						pstmt.setString(3, bookDetails.getBookNameReg());
						pstmt.setString(4, bookDetails.getFlag());
						pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setLong(6, bookDetails.getBookId());
						pstmt.executeUpdate();
						sql = null;
						bookDetails.setSaveStatusWhileImport(true);
						pstmt.close();pstmt = null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "updateEngineBookDetails()", e);
					}
					bookDetails = null;
				}
			}
			else
			{
				logger.info("updateEngineBookDetails :: List as Parameter is Null. Nothing to Update.");
			}
		}
		catch(Exception e)
		{
			logger.info("updateEngineBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "updateEngineBookDetails()", e);
			logger.info("updateEngineBookDetails :: ################ Exception ################");
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateEngineBookDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteEngineBookDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteEngineBookDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_engine_book SET mdm_eb_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_eb_updated_tmstp = ?  WHERE mdm_eb_id = "+ tokens[i].toString();
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
				logger.info("deleteEngineBookDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteEngineBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "deleteEngineBookDetails()", e);
			logger.info("deleteEngineBookDetails :: ################ Exception ################");
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
		logger.info("deleteEngineBookDetails :: Method Ends.");
		return true;
	}
	
	public static boolean activeEngineBookDetails(String activeIds) throws SQLException
	{
		logger.info("activeEngineBookDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_engine_book SET mdm_eb_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_eb_updated_tmstp = ?  WHERE mdm_eb_id = "+ tokens[i].toString();
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
				logger.info("activeEngineBookDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeEngineBookDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "activeEngineBookDetails()", e);
			logger.info("activeEngineBookDetails :: ################ Exception ################");
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
		logger.info("activeEngineBookDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<EngineBookDetails> importEngineBookDetails(ArrayList<EngineBookDetails> list) throws SQLException
	{
		logger.info("importEngineBookDetails :: Method Starts.");
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
				String getEngineBookSql = "";
				String sql="";
				EngineBookDetails details = new EngineBookDetails();
				long autoEngineBookId=0;
				for(int a=0;a<list.size();a++)
				{
					details = (EngineBookDetails)list.get(a);
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
						getEngineBookSql = "SELECT * FROM gms3_mdm_engine_book WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_eb_code=?   "
								+ " AND mdm_eb_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importEngineBookDetails :: getEngineBookSql :: > " + getEngineBookSql);
						pstmt = conn.prepareStatement(getEngineBookSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setString(3, details.getBookCode());
						rs = pstmt.executeQuery();
						autoEngineBookId=0;
						if(rs.next())
						{
							autoEngineBookId= rs.getLong("mdm_eb_id");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getEngineBookSql = null;
						
						if(autoEngineBookId>0)
						{
							pstmt = null;
							logger.info("importEngineBookDetails :: EngineBook Already Exists. Update Row for Auto EngineBook id : >" + autoEngineBookId);
							sql="UPDATE gms3_mdm_engine_book SET mdm_eb_code = ?,mdm_eb_name_eng_lang =?,mdm_eb_name_regional_lang =?,"
									+ " mdm_eb_updated_tmstp=? WHERE mdm_eb_id =?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, details.getBookCode());
							pstmt.setString(2, details.getBookNameEng());
							pstmt.setString(3, details.getBookNameReg());
							pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setLong(5, autoEngineBookId);
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							details.setSaveStatusWhileImport(true);
						}
						else
						{
							pstmt = null;
							logger.info("importEngineBookDetails :: EngineBook Does not Exists. Insert New Row.");
							
							sql="INSERT INTO gms3_mdm_engine_book(mdm_cl_id,mdm_ml_id,mdm_eb_code,mdm_eb_name_eng_lang,mdm_eb_name_regional_lang,"
									+ "mdm_eb_flag,mdm_eb_created_tmstp) VALUES(?,?,?,?,?,?,?)";
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
						Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "importEngineBookDetails()", e);
					}
					
					autoEngineBookId = 0;
					details = null;
				}
				
			}
			else
			{
				logger.info("importEngineBookDetails :: No Engine Book Data Passed as parameter for Importing.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(EngineBookDAO.class.getName(), "importEngineBookDetails()", e);
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
		logger.info("importEngineBookDetails :: Method Ends.");
		return list;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importEngineBookDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(EngineBookDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getEngineBookSql = null;
						getEngineBookSql = "SELECT * FROM gms3_mdm_engine_book WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " mdm_eb_code=?   "
								+ " AND mdm_eb_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//						logger.info("importEngineBookDetails :: getEngineBookSql :: > " + getEngineBookSql);
						pstmt = conn.prepareStatement(getEngineBookSql);
						pstmt.setLong(1, details.getCountryLocaleId());
						pstmt.setLong(2, details.getManualLanguageId());
						pstmt.setString(3, details.getBookCode());
						rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_eb_id");
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