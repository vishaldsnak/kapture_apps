package com.mazda.gms3.mdm.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CategoryDetails;

public class CategoryDAO extends DBConnectionHelper{



	private static Logger logger = LogManager.getLogger(CategoryDAO.class);
	
	public static ArrayList<CategoryDetails> getCategoryDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getCategoryDetailsList :: Method Starts.");
		ArrayList<CategoryDetails> bookList = new ArrayList<CategoryDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_category A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND A.mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND A.mdm_cat_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getCategoryDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CategoryDetails cDetails = new CategoryDetails();
					cDetails.setSrNo(bookList.size()+1);
					cDetails.setCategoryId(rs.getLong("mdm_cat_id"));
					cDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					cDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code") && !"".equals(rs.getString("mdm_ml_lang_code")))
					{
						cDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_cat_code") && !"".equals(rs.getString("mdm_cat_code")))
					{
						cDetails.setCategoryCode(rs.getString("mdm_cat_code").trim());
						cDetails.setOldCategoryCode(rs.getString("mdm_cat_code").trim());
					}
					if(null!=rs.getString("mdm_cat_name_eng_lang") && !"".equals(rs.getString("mdm_cat_name_eng_lang")))
					{
						cDetails.setCategoryNameEng(rs.getString("mdm_cat_name_eng_lang").trim());
						cDetails.setOldCategoryName(rs.getString("mdm_cat_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_scat_code") && !"".equals(rs.getString("mdm_scat_code")))
					{
						cDetails.setSubCategoryCode(rs.getString("mdm_scat_code").trim());
						cDetails.setOldSubCategoryCode(rs.getString("mdm_scat_code").trim());
					}
					if(null!=rs.getString("mdm_scat_name_eng_lang") && !"".equals(rs.getString("mdm_scat_name_eng_lang")))
					{
						cDetails.setSubCategoryNameEng(rs.getString("mdm_scat_name_eng_lang").trim());
						cDetails.setOldSubCategoryName(rs.getString("mdm_scat_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_sscat_code") && !"".equals(rs.getString("mdm_sscat_code")))
					{
						cDetails.setSubSubCategoryCode(rs.getString("mdm_sscat_code").trim());
						cDetails.setOldSubSubCategoryCode(rs.getString("mdm_sscat_code").trim());
					}
					if(null!=rs.getString("mdm_sscat_name_eng_lang") && !"".equals(rs.getString("mdm_sscat_name_eng_lang")))
					{
						cDetails.setSubSubCategoryNameEng(rs.getString("mdm_sscat_name_eng_lang").trim());
						cDetails.setOldSubSubCategoryName(rs.getString("mdm_sscat_name_eng_lang").trim());
					}
					cDetails.setFlag(rs.getString("mdm_cat_flag"));
					cDetails.setOldFlag(rs.getString("mdm_cat_flag"));
					cDetails.setEntryTime(rs.getTimestamp("mdm_cat_created_tmstp"));
					cDetails.setUpdatedTime(rs.getTimestamp("mdm_cat_updated_tmstp"));
					if(null!=cDetails.getFlag() && !"".equals(cDetails.getFlag()) && 
							cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						cDetails.setShowCheckBox(false);
					}
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						cDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					bookList.add(cDetails);
					cDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getCategoryDetailsList :: Language Code and Master Type Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCategoryDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "getCategoryDetailsList()", e);
			logger.info("getCategoryDetailsList :: ################ Exception ################");
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
//		logger.info("getCategoryDetailsList :: Method Ends.");
		return bookList;
	}

	public static ArrayList<CategoryDetails> getCategoryDetailsListForCombo(String langCode) throws SQLException 
	{
		logger.info("getCategoryDetailsListForCombo :: Method Starts.");
		ArrayList<CategoryDetails> bookList = new ArrayList<CategoryDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT mdm_cat_id,mdm_cat_code,mdm_cat_name_eng_lang FROM gms3_mdm_category WHERE"
						+ " mdm_cat_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"') "
								+ "AND mdm_ml_id="+new Long(langCode).longValue()+" "
										+ "  ORDER BY mdm_cat_code ASC";
				logger.info("getCategoryDetailsListForCombo :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CategoryDetails cDetails = new CategoryDetails();
					cDetails.setSrNo(bookList.size()+1);
					cDetails.setCategoryId(rs.getLong("mdm_cat_id"));
					String code = rs.getString("mdm_cat_code");
					String description = rs.getString("mdm_cat_name_eng_lang");
					
					String displayString="";
					if(null!=description && !"".equals(description))
					{
						displayString = description.trim();
					}
					if(null!=code && !"".equals(code))
					{
						displayString = displayString+" {"+code.trim()+"}";
					}
					
					cDetails.setCategoryCode(displayString);
					bookList.add(cDetails);
					cDetails=  null;
					code=null;
					description= null;
					displayString =null;
				}
				sql = null;
			}
			else
			{
				logger.info("getCategoryDetailsListForCombo :: Language Code and Master Type id as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCategoryDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "getCategoryDetailsListForCombo()", e);
			logger.info("getCategoryDetailsListForCombo :: ################ Exception ################");
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
		logger.info("getCategoryDetailsListForCombo :: Method Ends.");
		return bookList;
	}

	public static boolean saveCategoryDetails(CategoryDetails cDetails) throws SQLException
	{
		logger.info("saveCategoryDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()))
			{
				cDetails.setCategoryCode(cDetails.getCategoryCode().trim());
			}
			if(null!=cDetails.getCategoryNameEng() && !"".equals(cDetails.getCategoryNameEng()))
			{
				cDetails.setCategoryNameEng(cDetails.getCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode()))
			{
				cDetails.setSubCategoryCode(cDetails.getSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubCategoryNameEng() && !"".equals(cDetails.getSubCategoryNameEng()))
			{
				cDetails.setSubCategoryNameEng(cDetails.getSubCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
			{
				cDetails.setSubSubCategoryCode(cDetails.getSubSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
			{
				cDetails.setSubSubCategoryNameEng(cDetails.getSubSubCategoryNameEng().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_category(mdm_cl_id,mdm_ml_id,mdm_cat_code,mdm_cat_name_eng_lang,"
					+ "mdm_cat_flag,mdm_cat_created_tmstp,mdm_scat_code, mdm_scat_name_eng_lang, "
					+ "mdm_sscat_code,mdm_sscat_name_eng_lang) VALUES(?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, cDetails.getCountryLocaleId());
			pstmt.setLong(2, cDetails.getManualLanguageId());
			pstmt.setString(3, cDetails.getCategoryCode());
			pstmt.setString(4, cDetails.getCategoryNameEng());
			pstmt.setString(5, cDetails.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(7, cDetails.getSubCategoryCode());
			pstmt.setString(8, cDetails.getSubCategoryNameEng());
			pstmt.setString(9, cDetails.getSubSubCategoryCode());
			pstmt.setString(10, cDetails.getSubSubCategoryNameEng());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "saveCategoryDetails()", e);
			logger.info("saveCategoryDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			cDetails = null;
		}
		logger.info("saveCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean updateCategoryDetails(CategoryDetails cDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateCategoryDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()))
			{
				cDetails.setCategoryCode(cDetails.getCategoryCode().trim());
			}
			if(null!=cDetails.getCategoryNameEng() && !"".equals(cDetails.getCategoryNameEng()))
			{
				cDetails.setCategoryNameEng(cDetails.getCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode()))
			{
				cDetails.setSubCategoryCode(cDetails.getSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubCategoryNameEng() && !"".equals(cDetails.getSubCategoryNameEng()))
			{
				cDetails.setSubCategoryNameEng(cDetails.getSubCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
			{
				cDetails.setSubSubCategoryCode(cDetails.getSubSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
			{
				cDetails.setSubSubCategoryNameEng(cDetails.getSubSubCategoryNameEng().trim());
			}
			
			
			if(null!=cDetails.getOldCategoryCode() && !"".equals(cDetails.getOldCategoryCode()))
			{
				cDetails.setOldCategoryCode(cDetails.getOldCategoryCode().trim());
			}
			if(null!=cDetails.getOldCategoryName() && !"".equals(cDetails.getOldCategoryName()))
			{
				cDetails.setOldCategoryName(cDetails.getOldCategoryName().trim());
			}
			if(null!=cDetails.getOldSubCategoryCode() && !"".equals(cDetails.getOldSubCategoryCode()))
			{
				cDetails.setOldSubCategoryCode(cDetails.getOldSubCategoryCode().trim());
			}
			if(null!=cDetails.getOldSubCategoryName() && !"".equals(cDetails.getOldSubCategoryName()))
			{
				cDetails.setOldSubCategoryName(cDetails.getOldSubCategoryName().trim());
			}
			if(null!=cDetails.getOldSubSubCategoryCode() && !"".equals(cDetails.getOldSubSubCategoryCode()))
			{
				cDetails.setOldSubSubCategoryCode(cDetails.getOldSubSubCategoryCode().trim());
			}
			if(null!=cDetails.getOldSubSubCategoryName() && !"".equals(cDetails.getOldSubSubCategoryName()))
			{
				cDetails.setOldSubSubCategoryName(cDetails.getOldSubSubCategoryName().trim());
			}
			if(null!=cDetails.getSyncStatus() && !"".equals(cDetails.getSyncStatus()))
			{
				cDetails.setSyncStatus(cDetails.getSyncStatus().trim());
			}
			
			String syncStatus = cDetails.getSyncStatus();
			/*
			 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
				If PreviousStatus and CurrentStatus is Same and Status is Active - 
					if CATEGORY CODE / NAME, SUBCATEGORY CODE / NAME, SUB SUB CATEGORY CODE  / NAME , then update syncStatus as N

				If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
				If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
			 */
			if(cDetails.getOldFlag().equals(cDetails.getFlag()))
			{
				// STATUS IS SAME - check only for active
				if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					String keyToCheck=cDetails.getOldCategoryCode()+cDetails.getOldCategoryName()+cDetails.getOldSubCategoryCode()+cDetails.getOldSubCategoryName();
					keyToCheck+=cDetails.getOldSubSubCategoryCode();
					if(null!=cDetails.getOldSubSubCategoryName() && !"".equals(cDetails.getOldSubSubCategoryName()))
					{
						keyToCheck+=cDetails.getOldSubSubCategoryName();
					}
					String existData=cDetails.getCategoryCode()+cDetails.getCategoryNameEng()+cDetails.getSubCategoryCode()+cDetails.getSubCategoryNameEng();
					existData+=cDetails.getSubSubCategoryCode();
					if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
					{
						existData+=cDetails.getSubSubCategoryNameEng();
					}
							
					
					if(keyToCheck.trim().toLowerCase().equals(existData.trim().toLowerCase()))
					{
						// ALL ARE SAME -DO NOTHING WITH SYNC STATUS
					}
					else
					{
						// SOMETHING CHANGED - SET SYNC STATUS TO N
						syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
					}
					keyToCheck=  null;
					existData= null;
				}
			}
			else
			{
				if(cDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
						cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				else if(cDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
						cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
				{
					// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
					syncStatus = null;
				}
			}
			
			
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_mdm_category SET mdm_cat_code=?, mdm_cat_name_eng_lang =?,"
					+ " mdm_cat_flag=?,mdm_cat_updated_tmstp=?, mdm_scat_code=? , mdm_scat_name_eng_lang= ?, mdm_sscat_code= ? ,"
					+ " mdm_sscat_name_eng_lang = ?,mdm_sync_status=? WHERE mdm_cat_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, cDetails.getCategoryCode());
			pstmt.setString(2, cDetails.getCategoryNameEng());
			pstmt.setString(3, cDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(5, cDetails.getSubCategoryCode());
			pstmt.setString(6, cDetails.getSubCategoryNameEng());
			pstmt.setString(7, cDetails.getSubSubCategoryCode());
			pstmt.setString(8, cDetails.getSubSubCategoryNameEng());
			pstmt.setString(9, syncStatus);
			pstmt.setLong(10, cDetails.getCategoryId());
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
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "updateCategoryDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			cDetails=  null;
		}
		logger.info("updateCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteCategoryDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteCategoryDetails :: Method Starts.");
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
						// HERE FOR ALL RECORDS GETTING INACTIVE - SET SYNC STATUS TO NULL
						pstmt = null;
						String sql="UPDATE gms3_mdm_category SET mdm_cat_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_cat_updated_tmstp = ?,mdm_sync_status = ?  WHERE mdm_cat_id = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(2, null);
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
				logger.info("deleteCategoryDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			// rollBack
			conn.rollback();
			logger.info("deleteCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "deleteCategoryDetails()", e);
			logger.info("deleteCategoryDetails :: ################ Exception ################");
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
		logger.info("deleteCategoryDetails :: Method Ends.");
		return true;
	}
	
	public static boolean activeCategoryDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeCategoryDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=activeIdsList && activeIdsList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				Map<String, String> dataMap= null;
				String syncStatus="";
				for(int i=0;i<activeIdsList.size();i++)
				{
					dataMap= (Map<String, String>)activeIdsList.get(i);
					syncStatus="";
					if(null!=dataMap.get("FLAG") && !"".equals(dataMap.get("FLAG")))
					{
						// IF OLD FLAG WAS DRAFT - SET SYNC STATUS TO N
						if(dataMap.get("FLAG").toString().equals(ApplicationProperties.getProperty("flag.value.draft")))
						{
							syncStatus=AccessManagementInterface.SYNC_STATUS_NO;
						}
						// IF OLD FLAG WAS ACTIVE - DO NOTHING
					}
					pstmt = null;
					String sql="UPDATE gms3_mdm_category SET mdm_cat_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_cat_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_cat_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					syncStatus =  null;
				}
				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("activeCategoryDetails :: Active Ids as parameter is null. Return false");
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
			logger.info("activeCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "activeCategoryDetails()", e);
			logger.info("activeCategoryDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			activeIdsList=null;
		}
		logger.info("activeCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean importCategoryDetails(CategoryDetails cDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importCategoryDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()))
			{
				cDetails.setCategoryCode(cDetails.getCategoryCode().trim());
			}
			if(null!=cDetails.getCategoryNameEng() && !"".equals(cDetails.getCategoryNameEng()))
			{
				cDetails.setCategoryNameEng(cDetails.getCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode()))
			{
				cDetails.setSubCategoryCode(cDetails.getSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubCategoryNameEng() && !"".equals(cDetails.getSubCategoryNameEng()))
			{
				cDetails.setSubCategoryNameEng(cDetails.getSubCategoryNameEng().trim());
			}
			if(null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
			{
				cDetails.setSubSubCategoryCode(cDetails.getSubSubCategoryCode().trim());
			}
			if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
			{
				cDetails.setSubSubCategoryNameEng(cDetails.getSubSubCategoryNameEng().trim());
			}
			if(null==conn || conn.isClosed()==true)
			{
				conn= getConnection();
			}
			rs = null;
			pstmt= null;
			String oldCategoryName="";
			String oldSubCategoryName="";
			String oldSubSubCategoryName="";
			String oldFlag="";
			String syncStatus="";
			/*
			 * CHECK WHETHER CATEGORY EXISTS OR NOT
			 * IF YES -  THEN UPDATE CATEGORY
			 * ELSE - INSERT CATEGORY
			 */
			String getCategorySql = "SELECT * FROM gms3_mdm_category WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ " TRIM(LOWER(mdm_cat_code))=? AND TRIM(LOWER(mdm_scat_code))=? ";
			getCategorySql = getCategorySql+ " AND TRIM(LOWER(mdm_sscat_code))=? ";
			getCategorySql = getCategorySql	+ " AND mdm_cat_flag "
					+ " NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importCategoryDetails :: getCategorySql :: > " + getCategorySql);
			pstmt = conn.prepareStatement(getCategorySql);
			pstmt.setLong(1, cDetails.getCountryLocaleId());
			pstmt.setLong(2, cDetails.getManualLanguageId());
			pstmt.setString(3, cDetails.getCategoryCode().trim().toLowerCase());
			pstmt.setString(4, cDetails.getSubCategoryCode().trim().toLowerCase());
			pstmt.setString(5, cDetails.getSubSubCategoryCode().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoCategoryId=0;
			if(rs.next())
			{
				autoCategoryId= rs.getLong("mdm_cat_id");
				if(null!=rs.getString("mdm_cat_name_eng_lang") && !"".equals(rs.getString("mdm_cat_name_eng_lang")))
				{	
					oldCategoryName = rs.getString("mdm_cat_name_eng_lang").trim();
				}
				if(null!=rs.getString("mdm_scat_name_eng_lang") && !"".equals(rs.getString("mdm_scat_name_eng_lang")))
				{
					oldSubCategoryName=rs.getString("mdm_scat_name_eng_lang").trim();
				}
				if(null!=rs.getString("mdm_sscat_name_eng_lang") && !"".equals(rs.getString("mdm_sscat_name_eng_lang")))
				{
					oldSubSubCategoryName=rs.getString("mdm_sscat_name_eng_lang").trim();
				}
				oldFlag = rs.getString("mdm_cat_flag");
				if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
				{	
					syncStatus= rs.getString("mdm_sync_status").trim();
				}
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getCategorySql = null;
			
			if(autoCategoryId>0)
			{
				/*
				 * checks for sync status - 
				 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
					If oldStatus is Active - 
						Here, check for CATEGORY NAME, SUBCATEGORY NAME, SUB SUBCATEGORY NAME CHANGES (because fetched using ALL LEVEL CODES ), 
						 	then update syncStatus as N
							else let it remains as it is
				 */
				if(oldFlag.equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					String keyToCheck=oldCategoryName+oldSubCategoryName;
					if(null!=oldSubSubCategoryName && !"".equals(oldSubSubCategoryName))
					{
						keyToCheck+=oldSubSubCategoryName;
					}
					String exist = cDetails.getCategoryNameEng()+cDetails.getSubCategoryNameEng();
					if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
					{
						exist+=cDetails.getSubSubCategoryNameEng();
					}
					if(keyToCheck.trim().toLowerCase().equals(exist.trim().toLowerCase()))
					{
						// NOTHING CHANGED - LET THE SYNC STATUS REMAINS AS IT IS
					}
					else
					{
						// SOMETHING CHANGED. SET SYNC STATUS AS = N
						syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
					}
					keyToCheck=null;
					exist = null;
				}
				
				
				pstmt=null;
				logger.info("importCategoryDetails :: Category Already Exists. Update Row for Auto CATEGORY id : >" + autoCategoryId);
				
				String sql="UPDATE gms3_mdm_category SET mdm_cat_code=?, mdm_cat_name_eng_lang =?,"
						+ " mdm_cat_updated_tmstp=?, mdm_scat_code=? , mdm_scat_name_eng_lang= ?, mdm_sscat_code= ? , mdm_sscat_name_eng_lang = ?,mdm_sync_status = ?  "
						+ " WHERE mdm_cat_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, cDetails.getCategoryCode());
				pstmt.setString(2, cDetails.getCategoryNameEng());
				pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(4, cDetails.getSubCategoryCode());
				pstmt.setString(5, cDetails.getSubCategoryNameEng());
				pstmt.setString(6, cDetails.getSubSubCategoryCode());
				pstmt.setString(7, cDetails.getSubSubCategoryNameEng());
				pstmt.setString(8, syncStatus);
				pstmt.setLong(9, autoCategoryId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
			}
			else
			{
				pstmt = null;
				logger.info("importCategoryDetails :: Cateogry Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_mdm_category(mdm_cl_id,mdm_ml_id,mdm_cat_code,mdm_cat_name_eng_lang,"
						+ "mdm_cat_flag,mdm_cat_created_tmstp,mdm_scat_code, mdm_scat_name_eng_lang, "
						+ "mdm_sscat_code,mdm_sscat_name_eng_lang) VALUES(?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, cDetails.getCountryLocaleId());
				pstmt.setLong(2, cDetails.getManualLanguageId());
				pstmt.setString(3, cDetails.getCategoryCode());
				pstmt.setString(4, cDetails.getCategoryNameEng());
				pstmt.setString(5, cDetails.getFlag());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(7, cDetails.getSubCategoryCode());
				pstmt.setString(8, cDetails.getSubCategoryNameEng());
				pstmt.setString(9, cDetails.getSubSubCategoryCode());
				pstmt.setString(10, cDetails.getSubSubCategoryNameEng());
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
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "importCategoryDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			cDetails = null;
		}
		logger.info("importCategoryDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importCategoryDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(CategoryDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getCategorySql = "SELECT * FROM gms3_mdm_category WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ " TRIM(LOWER(mdm_cat_code))=? AND TRIM(LOWER(mdm_scat_code))=? ";
			getCategorySql = getCategorySql+ " AND TRIM(LOWER(mdm_sscat_code))=? ";
			getCategorySql = getCategorySql	+ " AND mdm_cat_flag "
					+ " NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importCategoryDetails :: getCategorySql :: > " + getCategorySql);
			pstmt = conn.prepareStatement(getCategorySql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getCategoryCode()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getSubCategoryCode()));
			pstmt.setString(5, ImportActionUtils.safeLower(details.getSubSubCategoryCode()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_cat_id");
			}
		} finally {
			if (null != rs)
				rs.close();
			if (null != pstmt)
				pstmt.close();
		}
		return existingId;
	}


	
	public static ArrayList<CategoryDetails> getSortedCategoryList(String langCode) throws SQLException 
	{
//		logger.info("getCategoryDetailsList :: Method Starts.");
		ArrayList<CategoryDetails> catList = new ArrayList<CategoryDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				langCode = langCode.replace("_", "-");
				conn = getConnection();
				String sql = "SELECT DISTINCT A.mdm_cat_code , A.mdm_cat_name_eng_lang FROM gms3_mdm_category A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND TRIM(LOWER(B.mdm_ml_lang_code))='"+langCode.trim().toLowerCase()+"' "
					+ " AND A.mdm_cat_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"') "
					+ " GROUP BY A.mdm_cat_code, A.mdm_cat_name_eng_lang ORDER BY A.mdm_cat_code ASC ";
//				logger.info("getSortedCategoryList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				boolean addToList = true;
				CategoryDetails catDetails= null;
				CategoryDetails existCatDetails= null;
				while(rs.next())
				{
					if(null!=rs.getString("mdm_cat_name_eng_lang") && !"".equals(rs.getString("mdm_cat_name_eng_lang")))
					{
						addToList = true;
						if(null!=catList && catList.size()>0)
						{
							existCatDetails = null;
							for(int a=0;a<catList.size();a++)
							{
								existCatDetails = (CategoryDetails)catList.get(a);
								if(existCatDetails.getCategoryNameEng().trim().toLowerCase().equals(rs.getString("mdm_cat_name_eng_lang").trim().toLowerCase()))
								{
									// break- already added
									addToList = false;
									break;
								}
								existCatDetails = null;
							}
						}
						
						if(addToList == true)
						{
							catDetails = new CategoryDetails();
							catDetails.setCategoryNameEng(rs.getString("mdm_cat_name_eng_lang").trim());
							catDetails.setCategoryCode(rs.getString("mdm_cat_code").trim());
							catList.add(catDetails);
							catDetails = null;
						}
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getSortedCategoryList :: Language Code and Master Type Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSortedCategoryList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CategoryDAO.class.getName(), "getSortedCategoryList()", e);
			logger.info("getSortedCategoryList :: ################ Exception ################");
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
//		logger.info("getSortedCategoryList :: Method Ends.");
		return catList;
	}
}