package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.SettingDetails;

public class SettingsDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(SettingsDAO.class);
	
	public static SettingDetails getSettingsData(String userId) throws SQLException
	{
		SettingDetails sDetails = null;
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			if(null!=userId && !"".equals(userId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_dmt_conv_setting WHERE DC_USER_LOGIN_ID='"+userId+"'";
				logger.info("getSettingsData :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					sDetails = new SettingDetails();
					sDetails.setUserId(rs.getString("DC_USER_LOGIN_ID"));
					sDetails.setNetworkPath(rs.getString("DC_GMS3_NETWORK_LOC"));
					sDetails.setCreationTime(rs.getTimestamp("DC_SETTING_CREATED_TMSTP"));
					sDetails.setUpdatedTime(rs.getTimestamp("DC_SETTING_UPDATED_TMSTP"));
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql=null;		
			}
			else
			{
				logger.info("getSettingsData :: User Id as parameter is null. Return null");
				sDetails=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SettingsDAO.class.getName(), "getSettingsData()", e);
			// set sDetails to null
			sDetails = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			userId =null;
		}
		return sDetails;
	}
	
	public static boolean saveNetworkPath(SettingDetails sDetails)
			throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try {
			if (null != sDetails && !"".equals(sDetails) && null!=sDetails.getUserId() && !"".equals(sDetails.getUserId()) 
					&& null != sDetails.getNetworkPath() && !"".equals(sDetails.getNetworkPath()))
			{
				conn = getConnection();
				boolean createFlag = true;
				/*
				 * Now before proceeding check for WSL ID, whether any entry
				 * exists or not if exists - then perform Update Operation else
				 * - Save Operation
				 */
				String getQuery = "SELECT * FROM gms3_dmt_conv_setting WHERE DC_USER_LOGIN_ID='"
						+ sDetails.getUserId() + "'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getQuery);
				if (rs.next()) 
				{
					// set createFlag to false
					createFlag = false;
				}
				getQuery = null;
				stmt.close();
				stmt = null;
				rs.close();
				rs = null;

				if (createFlag == true) 
				{
					logger.info("saveNetworkPath :: No entry for the WSL ID Exists in Database. Proceed for Creating New Row for WSL ID :: > " + sDetails.getUserId());
					String insertQuery = "INSERT INTO gms3_dmt_conv_setting (DC_USER_LOGIN_ID,DC_GMS3_NETWORK_LOC,"
							+ "DC_SETTING_CREATED_TMSTP) VALUES(?,?,?)";
					
					pstmt = conn.prepareStatement(insertQuery);
					pstmt.setString(1, sDetails.getUserId().trim());
					pstmt.setString(2, sDetails.getNetworkPath().trim());
					pstmt.setTimestamp(3,new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					insertQuery = null;
					pstmt.close();
					pstmt = null;

					logger.info("saveNetworkPath :: New Entry for the WSL ID Created Successfully in gms3_dmt_conv_setting.");
				} else if (createFlag == false) {
					logger.info("saveNetworkPath :: Entry for the WSL ID Exists in Database. Proceed for Updating Existing Row for WSL ID :: > " + sDetails.getUserId());
					String updateQuery = "UPDATE gms3_dmt_conv_setting SET DC_GMS3_NETWORK_LOC=?,DC_SETTING_UPDATED_TMSTP=? "
							+ "WHERE DC_USER_LOGIN_ID=?";
					pstmt = conn.prepareStatement(updateQuery);
					pstmt.setString(1, sDetails.getNetworkPath().trim());
					pstmt.setTimestamp(2,new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, sDetails.getUserId().trim());
					pstmt.executeUpdate();
					updateQuery = null;
					pstmt.close();
					pstmt = null;

					logger.info("saveNetworkPath :: Existing Entry for the WSL ID Updated Successfully in gms3_dmt_conv_setting.");
				}
			} else {
				logger.info("saveNetworkPath :: WSL ID and Network Path are passed as null in Parameters. Return false");
				return false;
			}
		} catch (Exception e) {
			if (null != conn) {
				conn.rollback();
			}
			Utilities.printStackTraceToLogs(SettingsDAO.class.getName(), "saveNetworkPath()", e);
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != pstmt)
				pstmt.close();
			sDetails = null;
		}
		return true;
	}

}
