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
import com.mazda.gms3.mdm.vo.CVCCategoryDetails;


public class CVCCategoryDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(CVCCategoryDAO.class);
	
	public static ArrayList<CVCCategoryDetails> getCVCCategoryDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getCVCCategoryDetailsList :: Method Starts.");
		ArrayList<CVCCategoryDetails> bookList = new ArrayList<CVCCategoryDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				String sql = "SELECT A.*, B.mdm_ml_lang_code FROM gms3_mdm_cvc_details A, gms3_mdm_manual_language B "
						+ "WHERE A.mdm_ml_id = B.mdm_ml_id AND A.mdm_ml_id="+new Long(langCode).longValue()+" "
										+ " AND A.mdm_cvc_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getCVCCategoryDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CVCCategoryDetails cDetails = new CVCCategoryDetails();
					cDetails.setSrNo(bookList.size()+1);
					cDetails.setCategoryId(rs.getLong("mdm_cvc_id"));
					cDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					cDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						cDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_cat_code"))
					{
						cDetails.setCategoryCode(rs.getString("mdm_cvc_cat_code").trim());
						cDetails.setOldCategoryCode(rs.getString("mdm_cvc_cat_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_cat_name"))
					{
						cDetails.setCategoryNameEng(rs.getString("mdm_cvc_cat_name").trim());
						cDetails.setOldCategoryNameEng(rs.getString("mdm_cvc_cat_name").trim());
					}
					if(null!=rs.getString("mdm_cvc_scat_code"))
					{
						cDetails.setSubCategoryCode(rs.getString("mdm_cvc_scat_code").trim());
						cDetails.setOldSubCategoryCode(rs.getString("mdm_cvc_scat_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_scat_name"))
					{
						cDetails.setSubCategoryName(rs.getString("mdm_cvc_scat_name").trim());
						cDetails.setOldSubCategoryName(rs.getString("mdm_cvc_scat_name").trim());
					}
					if(null!=rs.getString("mdm_cvc_sym_code"))
					{
						cDetails.setSymptomCode(rs.getString("mdm_cvc_sym_code").trim());
						cDetails.setOldSymptomCode(rs.getString("mdm_cvc_sym_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_sym_name"))
					{
						cDetails.setSymptomName(rs.getString("mdm_cvc_sym_name").trim());
						cDetails.setOldSymptomName(rs.getString("mdm_cvc_sym_name").trim());
					}
					if(null!=rs.getString("mdm_cvc_ssym_code"))
					{
						cDetails.setSubSymptomCode(rs.getString("mdm_cvc_ssym_code").trim());
						cDetails.setOldSubSymptomCode(rs.getString("mdm_cvc_ssym_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_ssym_name"))
					{
						cDetails.setSubSymptomName(rs.getString("mdm_cvc_ssym_name").trim());
						cDetails.setOldSubSymptomName(rs.getString("mdm_cvc_ssym_name").trim());
					}
					if(null!=rs.getString("mdm_cvc_con_code"))
					{
						cDetails.setConditionCode(rs.getString("mdm_cvc_con_code").trim());
						cDetails.setOldConditionCode(rs.getString("mdm_cvc_con_code").trim());
					}
					if(null!=rs.getString("mdm_cvc_con_name"))
					{
						cDetails.setConditionName(rs.getString("mdm_cvc_con_name").trim());
						cDetails.setOldConditionName(rs.getString("mdm_cvc_con_name").trim());
					}
					cDetails.setFlag(rs.getString("mdm_cvc_flag"));
					cDetails.setOldFlag(rs.getString("mdm_cvc_flag"));
					if(null!=rs.getString("mdm_cvc_rank"))
					{
						cDetails.setRank(rs.getString("mdm_cvc_rank").trim());
					}
					cDetails.setEntryTime(rs.getTimestamp("mdm_cvc_created_tmstp"));
					cDetails.setUpdatedTime(rs.getTimestamp("mdm_cvc_updated_tmstp"));
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
				logger.info("getCVCCategoryDetailsList :: Language Code and Master Type Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCVCCategoryDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "getCVCCategoryDetailsList()", e);
			logger.info("getCVCCategoryDetailsList :: ################ Exception ################");
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
//		logger.info("getCVCCategoryDetailsList :: Method Ends.");
		return bookList;
	}

	public static boolean saveCVCCategoryDetails(CVCCategoryDetails cDetails) throws SQLException
	{
		logger.info("saveCVCCategoryDetails :: Method Starts.");
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
			if(null!=cDetails.getSubCategoryName() && !"".equals(cDetails.getSubCategoryName()))
			{
				cDetails.setSubCategoryName(cDetails.getSubCategoryName().trim());
			}
			if(null!=cDetails.getSymptomCode() && !"".equals(cDetails.getSymptomCode()))
			{
				cDetails.setSymptomCode(cDetails.getSymptomCode().trim());
			}
			if(null!=cDetails.getSymptomName() && !"".equals(cDetails.getSymptomName()))
			{
				cDetails.setSymptomName(cDetails.getSymptomName().trim());
			}
			if(null!=cDetails.getSubSymptomCode() && !"".equals(cDetails.getSubSymptomCode()))
			{
				cDetails.setSubSymptomCode(cDetails.getSubSymptomCode().trim());
			}
			if(null!=cDetails.getSubSymptomName() && !"".equals(cDetails.getSubSymptomName()))
			{
				cDetails.setSubSymptomName(cDetails.getSubSymptomName().trim());
			}
			if(null!=cDetails.getConditionCode() && !"".equals(cDetails.getConditionCode()))
			{
				cDetails.setConditionCode(cDetails.getConditionCode().trim());
			}
			if(null!=cDetails.getConditionName() && !"".equals(cDetails.getConditionName()))
			{
				cDetails.setConditionName(cDetails.getConditionName().trim());
			}
			if(null!=cDetails.getRank() && !"".equals(cDetails.getRank()))
			{
				cDetails.setRank(cDetails.getRank().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_cvc_details(mdm_cl_id,mdm_ml_id,mdm_cvc_cat_code,mdm_cvc_cat_name,"
					+ "mdm_cvc_scat_code,mdm_cvc_scat_name,"
					+ "mdm_cvc_sym_code,mdm_cvc_sym_name,"
					+ "mdm_cvc_ssym_code,mdm_cvc_ssym_name,"
					+ "mdm_cvc_con_code,mdm_cvc_con_name,mdm_cvc_rank,"
					+ "mdm_cvc_flag,mdm_cvc_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, cDetails.getCountryLocaleId());
			pstmt.setLong(2, cDetails.getManualLanguageId());
			pstmt.setString(3, cDetails.getCategoryCode());
			pstmt.setString(4, cDetails.getCategoryNameEng());
			pstmt.setString(5, cDetails.getSubCategoryCode());
			pstmt.setString(6, cDetails.getSubCategoryName());
			pstmt.setString(7, cDetails.getSymptomCode());
			pstmt.setString(8, cDetails.getSymptomName());
			pstmt.setString(9, cDetails.getSubSymptomCode());
			pstmt.setString(10, cDetails.getSubSymptomName());
			pstmt.setString(11, cDetails.getConditionCode());
			pstmt.setString(12, cDetails.getConditionName());
			pstmt.setString(13, cDetails.getRank());
			pstmt.setString(14, cDetails.getFlag());
			pstmt.setTimestamp(15, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveCVCCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "saveCVCCategoryDetails()", e);
			logger.info("saveCVCCategoryDetails :: ################ Exception ################");
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
		logger.info("saveCVCCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean updateCVCCategoryDetails(CVCCategoryDetails cDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateCVCCategoryDetails :: Method Starts.");
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
			if(null!=cDetails.getSubCategoryName() && !"".equals(cDetails.getSubCategoryName()))
			{
				cDetails.setSubCategoryName(cDetails.getSubCategoryName().trim());
			}
			if(null!=cDetails.getSymptomCode() && !"".equals(cDetails.getSymptomCode()))
			{
				cDetails.setSymptomCode(cDetails.getSymptomCode().trim());
			}
			if(null!=cDetails.getSymptomName() && !"".equals(cDetails.getSymptomName()))
			{
				cDetails.setSymptomName(cDetails.getSymptomName().trim());
			}
			if(null!=cDetails.getSubSymptomCode() && !"".equals(cDetails.getSubSymptomCode()))
			{
				cDetails.setSubSymptomCode(cDetails.getSubSymptomCode().trim());
			}
			if(null!=cDetails.getSubSymptomName() && !"".equals(cDetails.getSubSymptomName()))
			{
				cDetails.setSubSymptomName(cDetails.getSubSymptomName().trim());
			}
			if(null!=cDetails.getConditionCode() && !"".equals(cDetails.getConditionCode()))
			{
				cDetails.setConditionCode(cDetails.getConditionCode().trim());
			}
			if(null!=cDetails.getConditionName() && !"".equals(cDetails.getConditionName()))
			{
				cDetails.setConditionName(cDetails.getConditionName().trim());
			}
			if(null!=cDetails.getRank() && !"".equals(cDetails.getRank()))
			{
				cDetails.setRank(cDetails.getRank().trim());
			}
			
			
			if(null!=cDetails.getOldCategoryCode() && !"".equals(cDetails.getOldCategoryCode()))
			{
				cDetails.setOldCategoryCode(cDetails.getOldCategoryCode().trim());
			}
			if(null!=cDetails.getOldCategoryNameEng() && !"".equals(cDetails.getOldCategoryNameEng()))
			{
				cDetails.setOldCategoryNameEng(cDetails.getOldCategoryNameEng().trim());
			}
			if(null!=cDetails.getOldSubCategoryCode() && !"".equals(cDetails.getOldSubCategoryCode()))
			{
				cDetails.setOldSubCategoryCode(cDetails.getOldSubCategoryCode().trim());
			}
			if(null!=cDetails.getOldSubCategoryName() && !"".equals(cDetails.getOldSubCategoryName()))
			{
				cDetails.setOldSubCategoryName(cDetails.getOldSubCategoryName().trim());
			}
			if(null!=cDetails.getOldSymptomCode() && !"".equals(cDetails.getOldSymptomCode()))
			{
				cDetails.setOldSymptomCode(cDetails.getOldSymptomCode().trim());
			}
			if(null!=cDetails.getOldSymptomName() && !"".equals(cDetails.getOldSymptomName()))
			{
				cDetails.setOldSymptomName(cDetails.getOldSymptomName().trim());
			}
			if(null!=cDetails.getOldSubSymptomCode() && !"".equals(cDetails.getOldSubSymptomCode()))
			{
				cDetails.setOldSubSymptomCode(cDetails.getOldSubSymptomCode().trim());
			}
			if(null!=cDetails.getOldSubSymptomName() && !"".equals(cDetails.getOldSubSymptomName()))
			{
				cDetails.setOldSubSymptomName(cDetails.getOldSubSymptomName().trim());
			}
			if(null!=cDetails.getOldConditionCode() && !"".equals(cDetails.getOldConditionCode()))
			{
				cDetails.setOldConditionCode(cDetails.getOldConditionCode().trim());
			}
			if(null!=cDetails.getOldConditionName() && !"".equals(cDetails.getOldConditionName()))
			{
				cDetails.setOldConditionName(cDetails.getOldConditionName().trim());
			}
			
			if(null!=cDetails.getSyncStatus() && !"".equals(cDetails.getSyncStatus()))
			{
				cDetails.setSyncStatus(cDetails.getSyncStatus().trim());
			}
			
			
			String syncStatus = cDetails.getSyncStatus();
			/*
			 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
				If PreviousStatus and CurrentStatus is Same and Status is Active - 
					if CATEGORY CODE / NAME, SUBCATEGORY CODE / NAME, SYMPTOM CODE  / NAME 
					,SUB SYMPTOM CODE / NAME, CONDITION CODE / NAME then update syncStatus as N

				If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
				If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
			 */
			if(cDetails.getOldFlag().equals(cDetails.getFlag()))
			{
				// STATUS IS SAME - check only for active
				if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					String keyToCheck=cDetails.getOldCategoryCode()+cDetails.getOldCategoryNameEng()+cDetails.getOldSubCategoryCode()+cDetails.getOldSubCategoryName();
					keyToCheck+=cDetails.getOldSymptomCode()+cDetails.getOldSymptomName()+cDetails.getOldSubSymptomCode()+cDetails.getOldSubSymptomName()+cDetails.getOldConditionCode()+cDetails.getOldConditionName();
					
					String existData=cDetails.getCategoryCode()+cDetails.getCategoryNameEng()+cDetails.getSubCategoryCode()+cDetails.getSubCategoryName();
					existData+=cDetails.getSymptomCode()+cDetails.getSymptomName()+cDetails.getSubSymptomCode()+cDetails.getSubSymptomName()+cDetails.getConditionCode()+cDetails.getConditionName();
							
					
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
				conn= getConnection();
			}
			String sql="UPDATE gms3_mdm_cvc_details SET mdm_cvc_cat_code=?, mdm_cvc_cat_name =?,"
					+ "mdm_cvc_scat_code =?,mdm_cvc_scat_name=?,"
					+ "mdm_cvc_sym_code =?,mdm_cvc_sym_name=?,"
					+ "mdm_cvc_ssym_code =?,mdm_cvc_ssym_name=?,"
					+ "mdm_cvc_con_code =?,mdm_cvc_con_name=?,mdm_cvc_rank=?,"
					+ "mdm_cvc_flag=?,mdm_cvc_updated_tmstp=?,mdm_sync_status = ? WHERE mdm_cvc_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, cDetails.getCategoryCode());
			pstmt.setString(2, cDetails.getCategoryNameEng());
			pstmt.setString(3, cDetails.getSubCategoryCode());
			pstmt.setString(4, cDetails.getSubCategoryName());
			pstmt.setString(5, cDetails.getSymptomCode());
			pstmt.setString(6, cDetails.getSymptomName());
			pstmt.setString(7, cDetails.getSubSymptomCode());
			pstmt.setString(8, cDetails.getSubSymptomName());
			pstmt.setString(9, cDetails.getConditionCode());
			pstmt.setString(10, cDetails.getConditionName());
			pstmt.setString(11, cDetails.getRank());
			pstmt.setString(12, cDetails.getFlag());
			pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(14, syncStatus);
			pstmt.setLong(15, cDetails.getCategoryId());
			pstmt.executeUpdate();
			sql = null;
			syncStatus = null;
			
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
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "updateCVCCategoryDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			cDetails=  null;
		}
		logger.info("updateCVCCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteCVCCategoryDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteCVCCategoryDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_cvc_details SET mdm_cvc_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_cvc_updated_tmstp = ? ,mdm_sync_status = ?  WHERE mdm_cvc_id = "+ tokens[i].toString();
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
				logger.info("deleteCVCCategoryDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteCVCCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "deleteCVCCategoryDetails()", e);
			logger.info("deleteCVCCategoryDetails :: ################ Exception ################");
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
		logger.info("deleteCVCCategoryDetails :: Method Ends.");
		return true;
	}

	public static boolean activeCVCCategoryDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeCVCCategoryDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_cvc_details SET mdm_cvc_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_cvc_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_cvc_id = "+ dataMap.get("ID").toString();
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;
					syncStatus = null;
					dataMap = null;
				}
				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("activeCVCCategoryDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			// rollBack
			conn.rollback();
			logger.info("activeCVCCategoryDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "activeCVCCategoryDetails()", e);
			logger.info("activeCVCCategoryDetails :: ################ Exception ################");
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
		logger.info("activeCVCCategoryDetails :: Method Ends.");
		return true;
	}

	
	public static boolean importCVCCategoryDetails(CVCCategoryDetails cDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importCVCCategoryDetails :: Method Starts.");
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
			if(null!=cDetails.getSubCategoryName() && !"".equals(cDetails.getSubCategoryName()))
			{
				cDetails.setSubCategoryName(cDetails.getSubCategoryName().trim());
			}
			if(null!=cDetails.getSymptomCode() && !"".equals(cDetails.getSymptomCode()))
			{
				cDetails.setSymptomCode(cDetails.getSymptomCode().trim());
			}
			if(null!=cDetails.getSymptomName() && !"".equals(cDetails.getSymptomName()))
			{
				cDetails.setSymptomName(cDetails.getSymptomName().trim());
			}
			if(null!=cDetails.getSubSymptomCode() && !"".equals(cDetails.getSubSymptomCode()))
			{
				cDetails.setSubSymptomCode(cDetails.getSubSymptomCode().trim());
			}
			if(null!=cDetails.getSubSymptomName() && !"".equals(cDetails.getSubSymptomName()))
			{
				cDetails.setSubSymptomName(cDetails.getSubSymptomName().trim());
			}
			if(null!=cDetails.getConditionCode() && !"".equals(cDetails.getConditionCode()))
			{
				cDetails.setConditionCode(cDetails.getConditionCode().trim());
			}
			if(null!=cDetails.getConditionName() && !"".equals(cDetails.getConditionName()))
			{
				cDetails.setConditionName(cDetails.getConditionName().trim());
			}
			if(null!=cDetails.getRank() && !"".equals(cDetails.getRank()))
			{
				cDetails.setRank(cDetails.getRank().trim());
			}
			if(null==conn || conn.isClosed()==true)
			{
				conn= getConnection();
			}
			rs = null;
			pstmt= null;
			String oldCategoryName="";
			String oldSubCategoryName="";
			String oldSymptomName="";
			String oldSubSymptomName="";
			String oldConditionName="";
			String oldFlag="";
			String syncStatus="";
			
			/*
			 * CHECK WHETHER CATEGORY EXISTS OR NOT
			 * IF YES -  THEN UPDATE CATEGORY
			 * ELSE - INSERT CATEGORY
			 */
			String getCategorySql = "SELECT * FROM gms3_mdm_cvc_details WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ " TRIM(LOWER(mdm_cvc_cat_code))=? AND TRIM(LOWER(mdm_cvc_scat_code))=? "
					+ " AND TRIM(LOWER(mdm_cvc_sym_code))=?  "
					+ " AND TRIM(LOWER(mdm_cvc_ssym_code))=?  "
					+ " AND TRIM(LOWER(mdm_cvc_con_code))=? "
					+ " AND mdm_cvc_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importCVCCategoryDetails :: getCategorySql :: > " + getCategorySql);
			pstmt = conn.prepareStatement(getCategorySql);
			pstmt.setLong(1, cDetails.getCountryLocaleId());
			pstmt.setLong(2, cDetails.getManualLanguageId());
			pstmt.setString(3, cDetails.getCategoryCode().trim().toLowerCase());
			pstmt.setString(4, cDetails.getSubCategoryCode().trim().toLowerCase());
			pstmt.setString(5, cDetails.getSymptomCode().trim().toLowerCase());
			pstmt.setString(6, cDetails.getSubSymptomCode().trim().toLowerCase());
			pstmt.setString(7, cDetails.getConditionCode().trim().toLowerCase());
			
			rs = pstmt.executeQuery();
			long autoCategoryId=0;
			if(rs.next())
			{
				autoCategoryId= rs.getLong("mdm_cvc_id");
				if(null!=rs.getString("mdm_cvc_cat_name") && !"".equals(rs.getString("mdm_cvc_cat_name")))
				{
					oldCategoryName = rs.getString("mdm_cvc_cat_name").trim();
				}
				if(null!=rs.getString("mdm_cvc_scat_name") && !"".equals(rs.getString("mdm_cvc_scat_name")))
				{
					oldSubCategoryName = rs.getString("mdm_cvc_scat_name").trim();
				}
				if(null!=rs.getString("mdm_cvc_sym_name") && !"".equals(rs.getString("mdm_cvc_sym_name")))
				{
					oldSymptomName = rs.getString("mdm_cvc_sym_name").trim();
				}
				if(null!=rs.getString("mdm_cvc_ssym_name") && !"".equals(rs.getString("mdm_cvc_ssym_name")))
				{
					oldSubSymptomName = rs.getString("mdm_cvc_ssym_name").trim();
				}
				if(null!=rs.getString("mdm_cvc_con_name") && !"".equals(rs.getString("mdm_cvc_con_name")))
				{
					oldConditionName = rs.getString("mdm_cvc_con_name").trim();
				}
				if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
				{
					syncStatus = rs.getString("mdm_sync_status").trim();
				}
				oldFlag= rs.getString("mdm_cvc_flag");
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
					String keyToCheck=oldCategoryName+oldSubCategoryName+oldSymptomName+oldSubSymptomName+oldConditionName;
					
					String exist = cDetails.getCategoryNameEng()+cDetails.getSubCategoryName()+cDetails.getSymptomName()+cDetails.getSubSymptomName()+cDetails.getConditionName();
					
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
				logger.info("importCVCCategoryDetails :: Category Already Exists. Update Row for Auto CATEGORY id : >" + autoCategoryId);
				String sql="UPDATE gms3_mdm_cvc_details SET mdm_cvc_cat_code=?, mdm_cvc_cat_name =?,"
						+ "mdm_cvc_scat_code =?,mdm_cvc_scat_name=?,"
						+ "mdm_cvc_sym_code =?,mdm_cvc_sym_name=?,"
						+ "mdm_cvc_ssym_code =?,mdm_cvc_ssym_name=?,"
						+ "mdm_cvc_con_code =?,mdm_cvc_con_name=?,mdm_cvc_rank=?,"
						+ "mdm_cvc_updated_tmstp=?,mdm_sync_status = ? WHERE mdm_cvc_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, cDetails.getCategoryCode());
				pstmt.setString(2, cDetails.getCategoryNameEng());
				pstmt.setString(3, cDetails.getSubCategoryCode());
				pstmt.setString(4, cDetails.getSubCategoryName());
				pstmt.setString(5, cDetails.getSymptomCode());
				pstmt.setString(6, cDetails.getSymptomName());
				pstmt.setString(7, cDetails.getSubSymptomCode());
				pstmt.setString(8, cDetails.getSubSymptomName());
				pstmt.setString(9, cDetails.getConditionCode());
				pstmt.setString(10, cDetails.getConditionName());
				pstmt.setString(11, cDetails.getRank());
				pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(13, syncStatus);
				pstmt.setLong(14, autoCategoryId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
			}
			else
			{
				pstmt = null;
				logger.info("importCVCCategoryDetails :: Cateogry Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_mdm_cvc_details(mdm_cl_id,mdm_ml_id,mdm_cvc_cat_code,mdm_cvc_cat_name,"
						+ "mdm_cvc_scat_code,mdm_cvc_scat_name,"
						+ "mdm_cvc_sym_code,mdm_cvc_sym_name,"
						+ "mdm_cvc_ssym_code,mdm_cvc_ssym_name,"
						+ "mdm_cvc_con_code,mdm_cvc_con_name,mdm_cvc_rank,"
						+ "mdm_cvc_flag,mdm_cvc_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, cDetails.getCountryLocaleId());
				pstmt.setLong(2, cDetails.getManualLanguageId());
				pstmt.setString(3, cDetails.getCategoryCode());
				pstmt.setString(4, cDetails.getCategoryNameEng());
				pstmt.setString(5, cDetails.getSubCategoryCode());
				pstmt.setString(6, cDetails.getSubCategoryName());
				pstmt.setString(7, cDetails.getSymptomCode());
				pstmt.setString(8, cDetails.getSymptomName());
				pstmt.setString(9, cDetails.getSubSymptomCode());
				pstmt.setString(10, cDetails.getSubSymptomName());
				pstmt.setString(11, cDetails.getConditionCode());
				pstmt.setString(12, cDetails.getConditionName());
				pstmt.setString(13, cDetails.getRank());
				pstmt.setString(14, cDetails.getFlag());
				pstmt.setTimestamp(15, new java.sql.Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
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
			Utilities.printStackTraceToLogs(CVCCategoryDAO.class.getName(), "importCVCCategoryDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			cDetails = null;
		}
		logger.info("importCVCCategoryDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importCVCCategoryDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(CVCCategoryDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getCategorySql = "SELECT * FROM gms3_mdm_cvc_details WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND  "
					+ " TRIM(LOWER(mdm_cvc_cat_code))=? AND TRIM(LOWER(mdm_cvc_scat_code))=? "
					+ " AND TRIM(LOWER(mdm_cvc_sym_code))=?  "
					+ " AND TRIM(LOWER(mdm_cvc_ssym_code))=?  "
					+ " AND TRIM(LOWER(mdm_cvc_con_code))=? "
					+ " AND mdm_cvc_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("importCVCCategoryDetails :: getCategorySql :: > " + getCategorySql);
			pstmt = conn.prepareStatement(getCategorySql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getCategoryCode()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getSubCategoryCode()));
			pstmt.setString(5, ImportActionUtils.safeLower(details.getSymptomCode()));
			pstmt.setString(6, ImportActionUtils.safeLower(details.getSubSymptomCode()));
			pstmt.setString(7, ImportActionUtils.safeLower(details.getConditionCode()));
			
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_cvc_id");
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