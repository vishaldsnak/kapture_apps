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
import com.mazda.gms3.mdm.vo.RuleDetails;
import com.mazda.gms3.mdm.vo.RuleLanguageDetails;
import com.mazda.gms3.mdm.vo.RuleModelDetails;

public class RuleDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(RuleDAO.class);
	
	public static ArrayList<RuleDetails> getRuleDetailsList() throws SQLException 
	{
//		logger.info("getRuleDetailsList :: Method Starts.");
		ArrayList<RuleDetails> ruleList = new ArrayList<RuleDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT * FROM gms3_mdm_aml_rule WHERE "
					+ "mdm_aml_rule_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//			logger.info("getRuleDetailsList :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RuleDetails rDetails = new RuleDetails();
				rDetails.setSrNo(ruleList.size()+1);
				rDetails.setRuleId(rs.getLong("mdm_aml_rule_id"));
				rDetails.setRuleName(rs.getString("mdm_aml_rule_name"));
				rDetails.setRuleDesc(rs.getString("mdm_aml_rule_desc"));
				rDetails.setFlag(rs.getString("mdm_aml_rule_flag"));
				rDetails.setEntryTime(rs.getTimestamp("mdm_aml_rule_created_tmstp"));
				rDetails.setUpdatedTime(rs.getTimestamp("mdm_aml_rule_updated_tmstp"));
				if(null!=rDetails.getFlag() && !"".equals(rDetails.getFlag()) && 
						rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
				{
					rDetails.setShowCheckBox(false);
				}
				ruleList.add(rDetails);
				rDetails=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("getRuleDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "getRuleDetailsList()", e);
			logger.info("getRuleDetailsList :: ################ Exception ################");
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
//		logger.info("getRuleDetailsList :: Method Ends.");
		return ruleList;
	}

	public static ArrayList<RuleDetails> getRuleDetailsListForCombo() throws SQLException 
	{
//		logger.info("getRuleDetailsListForCombo :: Method Starts.");
		ArrayList<RuleDetails> ruleList = new ArrayList<RuleDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT mdm_aml_rule_id,mdm_aml_rule_name FROM gms3_mdm_aml_rule "
					+ "WHERE mdm_aml_rule_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
							+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"')  ";
//			logger.info("getRuleDetailsListForCombo :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RuleDetails rDetails = new RuleDetails();
				rDetails.setSrNo(ruleList.size()+1);
				rDetails.setRuleId(rs.getLong("mdm_aml_rule_id"));
				rDetails.setRuleName(rs.getString("mdm_aml_rule_name"));
				ruleList.add(rDetails);
				rDetails=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("getRuleDetailsListForCombo :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "getRuleDetailsListForCombo()", e);
			logger.info("getRuleDetailsListForCombo :: ################ Exception ################");
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
//		logger.info("getRuleDetailsListForCombo :: Method Ends.");
		return ruleList;
	}
	
	public static ArrayList<RuleModelDetails> getRuleModelsDetailsList(String ruleId) throws SQLException 
	{
//		logger.info("getRuleModelsDetailsList :: Method Starts.");
		ArrayList<RuleModelDetails> modelsList = new ArrayList<RuleModelDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=ruleId && !"".equals(ruleId) && !"null".equals(ruleId.trim().toLowerCase()))
			{
			conn = getConnection();
			String sql = "SELECT * FROM gms3_mdm_aml_rule_model WHERE mdm_aml_rule_id="+new Long(ruleId).longValue();
//			logger.info("getRuleModelsDetailsList :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RuleModelDetails rDetails = new RuleModelDetails();
				rDetails.setSrNo(modelsList.size()+1);
				rDetails.setRuleId(rs.getLong("mdm_aml_rule_id"));
				rDetails.setRuleModelMappingId(rs.getLong("mdm_al_rm_id"));
				rDetails.setCarCode(rs.getString("mdm_al_rm_model"));
				rDetails.setWmiCode(rs.getString("mdm_al_rm_wmi_code"));
				rDetails.setVdsCode(rs.getString("mdm_al_rm_vds_code"));
				rDetails.setVisStartRange(rs.getString("mdm_al_rm_vis_start_range"));
				rDetails.setVisEndRange(rs.getString("mdm_al_rm_vis_end_range"));
				modelsList.add(rDetails);
				rDetails=  null;
			}
			sql = null;
			}
			else
			{
				logger.info("getRuleModelsDetailsList :: Rule id as parameter is null. Return null.");
				modelsList  =null;
			}
		}
		catch(Exception e)
		{
			logger.info("getRuleModelsDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "getRuleModelsDetailsList()", e);
			logger.info("getRuleModelsDetailsList :: ################ Exception ################");
			modelsList  =null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			ruleId=  null;
		}
//		logger.info("getRuleModelsDetailsList :: Method Ends.");
		return modelsList;
	}

	public static ArrayList<RuleLanguageDetails> getRuleLanguageDetailsList(String ruleId) throws SQLException 
	{
//		logger.info("getRuleLanguageDetailsList :: Method Starts.");
		ArrayList<RuleLanguageDetails> languageList = new ArrayList<RuleLanguageDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=ruleId && !"".equals(ruleId) && !"null".equals(ruleId.trim().toLowerCase()))
			{
				conn = getConnection();
				String sql = "SELECT A.*, (SELECT B.mdm_ml_lang_desc FROM gms3_mdm_manual_language B WHERE "
						+ "B.mdm_ml_lang_code = A.mdm_aml_rule_from_lang) AS FROM_LANG_NAME, "
						+ "(SELECT C.mdm_ml_lang_desc FROM gms3_mdm_manual_language C WHERE C.mdm_ml_lang_code = "
						+ "A.mdm_aml_rule_to_lang) AS TO_LANG_NAME FROM gms3_mdm_aml_rule_lang_map A "
						+ "WHERE A.mdm_aml_rule_id="+new Long(ruleId).longValue();
//				logger.info("getRuleLanguageDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					RuleLanguageDetails rDetails = new RuleLanguageDetails();
					rDetails.setSrNo(languageList.size()+1);
					rDetails.setRuleId(rs.getLong("mdm_aml_rule_id"));
					rDetails.setRuleLanguageMappingId(rs.getLong("mdm_aml_rule_lang_map_id"));
					rDetails.setFromLangCode(rs.getString("mdm_aml_rule_from_lang"));
					rDetails.setFromLangName(rs.getString("FROM_LANG_NAME"));
					rDetails.setToLangCode(rs.getString("mdm_aml_rule_to_lang"));
					rDetails.setToLangName(rs.getString("TO_LANG_NAME"));
					rDetails.setRefType(rs.getString("mdm_aml_rule_ref_type"));
					languageList.add(rDetails);
					rDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getRuleLanguageDetailsList :: Rule id as parameter is null. Return null.");
				languageList  =null;
			}
		}
		catch(Exception e)
		{
			logger.info("getRuleLanguageDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "getRuleLanguageDetailsList()", e);
			logger.info("getRuleLanguageDetailsList :: ################ Exception ################");
			languageList  =null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			ruleId=  null;
		}
//		logger.info("getRuleLanguageDetailsList :: Method Ends.");
		return languageList;
	}
	
	public static boolean saveRuleDetails(RuleDetails rDetails) throws SQLException
	{
		logger.info("saveRuleDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_aml_rule(mdm_aml_rule_name,mdm_aml_rule_desc,"
					+ "mdm_aml_rule_flag,mdm_aml_rule_created_tmstp) VALUES(?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, rDetails.getRuleName());
			pstmt.setString(2, rDetails.getRuleDesc());
			pstmt.setString(3, rDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveRuleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "saveRuleDetails()", e);
			logger.info("saveRuleDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			rDetails = null;
		}
		logger.info("saveRuleDetails :: Method Ends.");
		return true;
	}

	public static boolean updateModelMappingDetails(String ruleId, ArrayList<RuleModelDetails> modelsList) throws SQLException
	{
		logger.info("updateModelMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Statement stmt = null;
		Connection conn = null;
		try
		{
			if(null!=ruleId && !"".equals(ruleId) && null!=modelsList && modelsList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * delete all the existing model mapping for the rule
				 */
				String deleteSql="DELETE FROM gms3_mdm_aml_rule_model WHERE mdm_aml_rule_id="+new Long(ruleId).longValue();
				logger.info("updateModelMappingDetails :: deleteSql :: > " + deleteSql);
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				deleteSql = null;
				
				/*
				 * now iterate modelList and insert all mapping
				 */
				for(int r=0;r<modelsList.size();r++)
				{
					RuleModelDetails mDetails = (RuleModelDetails)modelsList.get(r);
					String sql = "INSERT INTO gms3_mdm_aml_rule_model(mdm_aml_rule_id,mdm_al_rm_model,"
							+ "mdm_al_rm_wmi_code,mdm_al_rm_vds_code,mdm_al_rm_vis_start_range,mdm_al_rm_vis_end_range,"
							+ "mdm_al_rm_created_tmstp) VALUES(?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setLong(1, new Long(ruleId).longValue());
					pstmt.setString(2, mDetails.getCarCode());
					pstmt.setString(3, mDetails.getWmiCode());
					pstmt.setString(4, mDetails.getVdsCode());
					pstmt.setString(5, mDetails.getVisStartRange());
					pstmt.setString(6, mDetails.getVisEndRange());
					pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					sql = null;
					
					pstmt.close();
					pstmt=null;
					mDetails = null;
				}
				
				conn.commit();
			}
			else
			{
				logger.info("updateModelMappingDetails :: Rule id and Models List as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException r)
			{
				logger.info("updateModelMappingDetails :: ################ Exception ################");
				Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "updateModelMappingDetails()", e);
				logger.info("updateModelMappingDetails :: ################ Exception ################");
				return false;
			}
			logger.info("updateModelMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "updateModelMappingDetails()", e);
			logger.info("updateModelMappingDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			ruleId = null;
			modelsList = null;
		}
		logger.info("updateModelMappingDetails :: Method Ends.");
		return true;
	}

	public static boolean updateLanguageMappingDetails(String ruleId, ArrayList<RuleLanguageDetails> languageList) throws SQLException
	{
		logger.info("updateLanguageMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Statement stmt = null;
		Connection conn = null;
		try
		{
			if(null!=ruleId && !"".equals(ruleId) && null!=languageList && languageList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * delete all the existing model mapping for the rule
				 */
				String deleteSql="DELETE FROM gms3_mdm_aml_rule_lang_map WHERE mdm_aml_rule_id="+new Long(ruleId).longValue();
				logger.info("updateLanguageMappingDetails :: deleteSql :: > " + deleteSql);
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				deleteSql = null;
				
				/*
				 * now iterate modelList and insert all mapping
				 */
				for(int r=0;r<languageList.size();r++)
				{
					RuleLanguageDetails lDetails = (RuleLanguageDetails)languageList.get(r);
					String sql = "INSERT INTO gms3_mdm_aml_rule_lang_map(mdm_aml_rule_id,mdm_aml_rule_from_lang,"
							+ "mdm_aml_rule_to_lang,mdm_aml_rule_ref_type,"
							+ "mdm_aml_rlm_created_tmstp) VALUES(?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setLong(1, new Long(ruleId).longValue());
					pstmt.setString(2, lDetails.getFromLangCode());
					pstmt.setString(3, lDetails.getToLangCode());
					pstmt.setString(4, lDetails.getRefType());
					pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					sql = null;
					
					pstmt.close();
					pstmt=null;
					lDetails=  null;
				}
				
				conn.commit();
			}
			else
			{
				logger.info("updateModelMappingDetails :: Rule id and Models List as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException r)
			{
				logger.info("updateLanguageMappingDetails :: ################ Exception ################");
				Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "updateLanguageMappingDetails()", e);
				logger.info("updateModelMappingDetails :: ################ Exception ################");
				return false;
			}
			logger.info("updateLanguageMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "updateModelMappingDetails()", e);
			logger.info("updateLanguageMappingDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			ruleId = null;
			languageList = null;
		}
		logger.info("updateLanguageMappingDetails :: Method Ends.");
		return true;
	}

	
	public static boolean updateRuleDetails(RuleDetails rDetails) throws SQLException
	{
		logger.info("updateRuleDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			String sql="UPDATE gms3_mdm_aml_rule SET mdm_aml_rule_name =?,mdm_aml_rule_desc =?,"
					+ "mdm_aml_rule_flag=?,mdm_aml_rule_updated_tmstp=? WHERE mdm_aml_rule_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, rDetails.getRuleName());
			pstmt.setString(2, rDetails.getRuleDesc());
			pstmt.setString(3, rDetails.getFlag());
			pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(5, rDetails.getRuleId());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("updateRuleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "updateRuleDetails()", e);
			logger.info("updateRuleDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			rDetails=  null;
		}
		logger.info("updateRuleDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteRuleDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteRuleDetails :: Method Starts.");
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
						String sql="UPDATE gms3_mdm_aml_rule SET mdm_aml_rule_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_aml_rule_updated_tmstp = ?  WHERE mdm_aml_rule_id = "+ tokens[i].toString();
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
				logger.info("deleteRuleDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteRuleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MissionTypeDAO.class.getName(), "deleteRuleDetails()", e);
			logger.info("deleteRuleDetails :: ################ Exception ################");
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
		logger.info("deleteRuleDetails :: Method Ends.");
		return true;
	}


	public static boolean deleteRuleModelMapping(long modelMappingId) throws SQLException
	{
		logger.info("deleteRuleModelMapping :: Method Starts.");
		Statement pstmt = null;
		Connection conn = null;
		try
		{
			if(modelMappingId>0)
			{
				conn = getConnection();
				String sql="delete from gms3_mdm_aml_rule_model where mdm_al_rm_id="+modelMappingId;
				pstmt = conn.createStatement();
				pstmt.executeUpdate(sql);
				sql = null;
			}
		}
		catch(Exception e)
		{
			logger.info("deleteRuleModelMapping :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "deleteRuleModelMapping()", e);
			logger.info("deleteRuleModelMapping :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("deleteRuleModelMapping :: Method Ends.");
		return true;
	}
	
	public static boolean deleteRuleLanguageMapping(long languageMappingId) throws SQLException
	{
		logger.info("deleteRuleLanguageMapping :: Method Starts.");
		Statement pstmt = null;
		Connection conn = null;
		try
		{
			if(languageMappingId>0)
			{
				conn = getConnection();
				String sql="delete from gms3_mdm_aml_rule_lang_map where mdm_aml_rule_lang_map_id="+languageMappingId;
				pstmt = conn.createStatement();
				pstmt.executeUpdate(sql);
				sql = null;
			}
		}
		catch(Exception e)
		{
			logger.info("deleteRuleLanguageMapping :: ################ Exception ################");
			Utilities.printStackTraceToLogs(RuleDAO.class.getName(), "deleteRuleLanguageMapping()", e);
			logger.info("deleteRuleLanguageMapping :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("deleteRuleLanguageMapping :: Method Ends.");
		return true;
	}


}