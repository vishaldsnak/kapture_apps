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
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.RoleDetails;

public class AccessManagementDAO extends DBConnectionHelper{

	private static Logger  logger  = LogManager.getLogger(AccessManagementDAO.class);
	
	public static ArrayList<ModuleDetails> getExistingAccessDetails(String roleId, String roleRefKey) throws SQLException
	{
		ArrayList<ModuleDetails> rolesBasedModulesList = new ArrayList<ModuleDetails>();
		Connection conn = null;
		ResultSet rs  =null;
		Statement stmt = null;
		try
		{
			if((null!=roleId && !"".equals(roleId)) || (null!=roleRefKey && !"".equals(roleRefKey)))
			{
				conn = getConnection();
				/*
				 * FIRST GET THE ROLE DET
				 */
				String getAccessManagementSql="";
				if(null!=roleId && !"".equals(roleId))
				{
					getAccessManagementSql = "SELECT A.*,B.mdm_module_refkey,B.mdm_module_type,B.mdm_module_path, B.mdm_module_description FROM "
							+ " gms3_mdm_roles_access A,gms3_mdm_modules B WHERE A.mdm_module_id = B.mdm_module_id AND A.mdm_role_id="+ roleId;
				}
				else if(null!=roleRefKey && !"".equals(roleRefKey))
				{
					getAccessManagementSql = "SELECT A.*,C.mdm_module_refkey,C.mdm_module_type,C.mdm_module_path, "
							+ " C.mdm_module_description FROM gms3_mdm_roles_access A,gms3_mdm_roles B,gms3_mdm_modules C "
							+ " WHERE A.mdm_role_id=B.mdm_role_id AND A.mdm_module_id = C.mdm_module_id AND "
							+ " TRIM(LOWER(B.mdm_role_refkey))='"+roleRefKey.trim().toLowerCase()+"'";
				}
//				logger.info("getExistingAccessDetails :: getAccessManagementSql :: > " + getAccessManagementSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getAccessManagementSql);
				while(rs.next())
				{
					ModuleDetails mDetails = new ModuleDetails();
					mDetails.setRoleId(rs.getLong("mdm_role_id"));
					mDetails.setModuleId(rs.getLong("mdm_module_id"));
					mDetails.setAccessType(rs.getInt("mdm_roleacc_access_type"));
					mDetails.setModuleRefkey(rs.getString("mdm_module_refkey"));
					mDetails.setModuleType(rs.getInt("mdm_module_type"));
					mDetails.setModuleDescription(rs.getString("mdm_module_description"));
					/*
					 * set description value in displayOrder Field
					 * change - 16 july 2018
					 */
					if(null!=mDetails.getModuleDescription() && !"".equals(mDetails.getModuleDescription()))
					{
						mDetails.setDisplayOrder(new Integer(mDetails.getModuleDescription().trim()).intValue());
					}
					mDetails.setModulePath(rs.getString("mdm_module_path"));
					rolesBasedModulesList.add(mDetails);
					mDetails= null;
				}
				getAccessManagementSql= null;
			}
			else
			{
				logger.info("getExistingAccessDetails :: Role Ref Key as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getExistingAccessDetails :: Exception ###########################################");
			Utilities.printStackTraceToLogs(AccessManagementDAO.class.getName(), "getExistingAccessDetails()", e);
			logger.info("getExistingAccessDetails :: Exception ###########################################");
			roleId=  null;
			roleRefKey = null;
			rolesBasedModulesList=  null;
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return rolesBasedModulesList;
	}
	
	/**
	 * Function will fetch all the Module Details from Database
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<ModuleDetails> getModuleDetailsList() throws SQLException
	{
		ArrayList<ModuleDetails> modulesList = new ArrayList<ModuleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs=  null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_modules";
//			logger.info("getModuleDetailsList :: Sql :: > " + sql);
			stmt  = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				ModuleDetails mDetails = new ModuleDetails();
				mDetails.setModuleId(rs.getLong("mdm_module_id"));
				mDetails.setModuleRefkey(rs.getString("mdm_module_refkey"));
				mDetails.setModulePath(rs.getString("mdm_module_path"));
				mDetails.setModuleDescription(rs.getString("mdm_module_description"));
				/*
				 * set description value in displayOrder Field
				 * change - 16 july 2018
				 */
				if(null!=mDetails.getModuleDescription() && !"".equals(mDetails.getModuleDescription()))
				{
					mDetails.setDisplayOrder(new Integer(mDetails.getModuleDescription().trim()).intValue());
				}
				mDetails.setModuleType(rs.getInt("mdm_module_type"));
				modulesList.add(mDetails);
				mDetails= null;
			}
		}
		catch(Exception e)
		{
			logger.info("getModuleDetailsList :: Exception ###########################################");
			Utilities.printStackTraceToLogs(AccessManagementDAO.class.getName(), "getModuleDetailsList()", e);
			logger.info("getModuleDetailsList :: Exception ###########################################");
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return modulesList;
	}

	
	public static ArrayList<RoleDetails> getRoleDetailsList() throws SQLException
	{
		ArrayList<RoleDetails> rolesList = new ArrayList<RoleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs=  null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_roles WHERE mdm_role_priority!="+AccessManagementInterface.SUPER_ADMIN_ROLE_PRIORITY;
//			logger.info("getModuleDetailsList :: Sql :: > " + sql);
			stmt  = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RoleDetails rDetails = new RoleDetails();
				rDetails.setRoleId(rs.getLong("mdm_role_id"));
				rDetails.setRoleName(rs.getString("mdm_role_name"));
				rDetails.setRoleRefKey(rs.getString("mdm_role_refkey"));
				rDetails.setPriority(rs.getInt("mdm_role_priority"));
				rolesList.add(rDetails);
				rDetails= null;
			}
		}
		catch(Exception e)
		{
			logger.info("getRoleDetailsList :: Exception ###########################################");
			Utilities.printStackTraceToLogs(AccessManagementDAO.class.getName(), "getRoleDetailsList()", e);
			logger.info("getRoleDetailsList :: Exception ###########################################");
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return rolesList;
	}
	
	public static ArrayList<RoleDetails> getAllRoleDetailsList() throws SQLException
	{
		ArrayList<RoleDetails> rolesList = new ArrayList<RoleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs=  null;
		try
		{
			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_roles";
//			logger.info("getAllRoleDetailsList :: Sql :: > " + sql);
			stmt  = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				RoleDetails rDetails = new RoleDetails();
				rDetails.setRoleId(rs.getLong("mdm_role_id"));
				rDetails.setRoleName(rs.getString("mdm_role_name"));
				rDetails.setRoleRefKey(rs.getString("mdm_role_refkey"));
				rDetails.setPriority(rs.getInt("mdm_role_priority"));
				rolesList.add(rDetails);
				rDetails= null;
			}
		}
		catch(Exception e)
		{
			logger.info("getAllRoleDetailsList :: Exception ###########################################");
			Utilities.printStackTraceToLogs(AccessManagementDAO.class.getName(), "getAllRoleDetailsList()", e);
			logger.info("getAllRoleDetailsList :: Exception ###########################################");
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
		}
		return rolesList;
	}
	
	public static boolean saveAccessManagementData(ArrayList<ModuleDetails> list, String roleId) throws SQLException
	{
		logger.info("saveAccessManagementData :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		Statement stmt =null;
		try
		{
			if(null!=list && list.size()>0 && null!=roleId && !"".equals(roleId))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * FIRST DELETE ALL THE DATA FROM TABLE ON THE BASIS OF ROLE ID 
				 * AND THEN INSERT NEW MAPPING DATA
				 */
				String deleteSql="DELETE FROM gms3_mdm_roles_access WHERE mdm_role_id="+ roleId;
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				deleteSql = null;
				stmt.close();stmt =null;
				
				for(int a=0;a<list.size();a++)
				{
					ModuleDetails moduleDetails = (ModuleDetails)list.get(a);
					pstmt = null;
					String sql = "INSERT INTO gms3_mdm_roles_access(mdm_role_id, mdm_module_id, "
							+ "mdm_roleacc_access_type,mdm_roleacc_created_tmstp) VALUES(?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setLong(1, moduleDetails.getRoleId());
					pstmt.setLong(2, moduleDetails.getModuleId());
					pstmt.setInt(3, moduleDetails.getAccessType());
					pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt= null;
				}
				
				// commit 
				conn.commit();
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("saveAccessManagementData :: ################ Exception ################");
			Utilities.printStackTraceToLogs(AccessManagementDAO.class.getName(), "saveAccessManagementData()", e);
			logger.info("saveAccessManagementData :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			list = null;
			roleId = null;
		}
		logger.info("saveAccessManagementData :: Method Ends.");
		return true;
	}
}