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
import com.mazda.gms3.mdm.vo.DisplayOrderDetails;

public class DisplayOrderDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(DisplayOrderDAO.class);
	
	public static ArrayList<DisplayOrderDetails> getDisplayOrderDetailsList(String languageId, String filterModelType) throws SQLException 
	{
//		logger.info("getDisplayOrderDetailsList :: Method Starts.");
		ArrayList<DisplayOrderDetails> displayOrderList = new ArrayList<DisplayOrderDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_display_order WHERE mdm_dispord_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
				
				if(null!=filterModelType && !"".equals(filterModelType))
				{
					sql = sql+ " AND TRIM(LOWER(mdm_dispord_model_type))='"+filterModelType.trim().toLowerCase()+"'";
				}
//				logger.info("getDisplayOrderDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					DisplayOrderDetails displayOrderDetails = new DisplayOrderDetails();
					displayOrderDetails.setSrNo(displayOrderList.size()+1);
					displayOrderDetails.setDisplayOrderId(rs.getLong("mdm_dispord_id"));
					displayOrderDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					displayOrderDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_dispord_code") && !"".equals(rs.getString("mdm_dispord_code")))
					{
						displayOrderDetails.setDisplayOrderCode(rs.getString("mdm_dispord_code").trim());
					}
					if(null!=rs.getString("mdm_dispord_name"))
					{
						displayOrderDetails.setDisplayOrderName(rs.getString("mdm_dispord_name").trim());
					}
					if(null!=rs.getString("mdm_dispord_model_type"))
					{
						displayOrderDetails.setModelType(rs.getString("mdm_dispord_model_type").trim());
					}
					displayOrderDetails.setFlag(rs.getString("mdm_dispord_flag"));
					displayOrderDetails.setEntryTime(rs.getTimestamp("mdm_dispord_created_tmstp"));
					displayOrderDetails.setUpdatedTime(rs.getTimestamp("mdm_dispord_updated_tmstp"));
					if(null!=displayOrderDetails.getFlag() && !"".equals(displayOrderDetails.getFlag()) && 
							displayOrderDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						displayOrderDetails.setShowCheckBox(false);
					}
					displayOrderList.add(displayOrderDetails);
					displayOrderDetails=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getDisplayOrderDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getDisplayOrderDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "getDisplayOrderDetailsList()", e);
			logger.info("getDisplayOrderDetailsList :: ################ Exception ################");
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
//		logger.info("getDisplayOrderDetailsList :: Method Ends.");
		return displayOrderList;
	}

	public static boolean saveDisplayOrderDetails(DisplayOrderDetails displayOrderDetails) throws SQLException
	{
		logger.info("saveDisplayOrderDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=displayOrderDetails.getDisplayOrderCode())
			{
				displayOrderDetails.setDisplayOrderCode(displayOrderDetails.getDisplayOrderCode().trim());
			}
			if(null!=displayOrderDetails.getDisplayOrderName())
			{
				displayOrderDetails.setDisplayOrderName(displayOrderDetails.getDisplayOrderName().trim());
			}
			if(null!=displayOrderDetails.getModelType())
			{
				displayOrderDetails.setModelType(displayOrderDetails.getModelType().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_display_order(mdm_cl_id,mdm_ml_id,mdm_dispord_code, mdm_dispord_name, mdm_dispord_flag,"
					+ " mdm_dispord_created_tmstp,mdm_dispord_model_type)"
					+ " VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, displayOrderDetails.getCountryLocaleId());
			pstmt.setLong(2, displayOrderDetails.getManualLanguageId());
			pstmt.setString(3, displayOrderDetails.getDisplayOrderCode());
			pstmt.setString(4, displayOrderDetails.getDisplayOrderName());
			pstmt.setString(5, displayOrderDetails.getFlag());
			pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(7, displayOrderDetails.getModelType());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveDisplayOrderDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "saveDisplayOrderDetails()", e);
			logger.info("saveDisplayOrderDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			displayOrderDetails = null;
		}
		logger.info("saveDisplayOrderDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<DisplayOrderDetails> updateDisplayOrderDetails(ArrayList<DisplayOrderDetails> list) throws SQLException
	{
		logger.info("updateDisplayOrderDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				DisplayOrderDetails displayOrderDetails = new DisplayOrderDetails();
				String sql = null;
				conn = getConnection();
				for(int a=0;a<list.size();a++)
				{
					displayOrderDetails = (DisplayOrderDetails)list.get(a);
					if(null!=displayOrderDetails.getDisplayOrderCode())
					{
						displayOrderDetails.setDisplayOrderCode(displayOrderDetails.getDisplayOrderCode().trim());
					}
					if(null!=displayOrderDetails.getDisplayOrderName())
					{
						displayOrderDetails.setDisplayOrderName(displayOrderDetails.getDisplayOrderName().trim());
					}
					if(null!=displayOrderDetails.getModelType())
					{
						displayOrderDetails.setModelType(displayOrderDetails.getModelType().trim());
					}
					try
					{
						sql="UPDATE gms3_mdm_display_order SET mdm_dispord_code =?,mdm_dispord_name =?,"
								+ " mdm_dispord_flag=?,mdm_dispord_updated_tmstp=?, mdm_dispord_model_type=?"
								+ "  WHERE mdm_dispord_id =?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, displayOrderDetails.getDisplayOrderCode());
						pstmt.setString(2, displayOrderDetails.getDisplayOrderName());
						pstmt.setString(3, displayOrderDetails.getFlag());
						pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(5, displayOrderDetails.getModelType());
						pstmt.setLong(6, displayOrderDetails.getDisplayOrderId());
						pstmt.executeUpdate();
						sql = null;
						pstmt.close();pstmt = null;
						displayOrderDetails.setSaveStatusWhileImport(true);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "updateDisplayOrderDetails()", e);
					}
				}
			}
			else
			{
				logger.info("updateDisplayOrderDetails :: List as Parameter is null. Nothing to update.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "updateDisplayOrderDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("updateDisplayOrderDetails :: Method Ends.");
		return list;
	}
	
	public static boolean deleteDisplayOrderDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteDisplayOrderDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_display_order SET mdm_dispord_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_dispord_updated_tmstp = ?  WHERE mdm_dispord_id = "+ tokens[i].toString();
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
				logger.info("deleteDisplayOrderDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteDisplayOrderDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "deleteDisplayOrderDetails()", e);
			logger.info("deleteDisplayOrderDetails :: ################ Exception ################");
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
		logger.info("deleteDisplayOrderDetails :: Method Ends.");
		return true;
	}

	public static boolean activeDisplayOrderDetails(String activeIds) throws SQLException
	{
		logger.info("activeDisplayOrderDetails :: Method Starts.");
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
						sql="UPDATE gms3_mdm_display_order SET mdm_dispord_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_dispord_updated_tmstp = ?  WHERE mdm_dispord_id = "+ tokens[i].toString();
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
				logger.info("activeDisplayOrderDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeDisplayOrderDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "activeDisplayOrderDetails()", e);
			logger.info("activeDisplayOrderDetails :: ################ Exception ################");
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
		logger.info("activeDisplayOrderDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<DisplayOrderDetails> importDisplayOrderDetails(ArrayList<DisplayOrderDetails> list) throws SQLException
	{
		logger.info("importDisplayOrderDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				DisplayOrderDetails displayOrderDetails = new DisplayOrderDetails();
				String getDisplayOrderSql="";
				String sql="";
				long autoDisplayOrderId=0;
				for(int a=0;a<list.size();a++)
				{
					displayOrderDetails = (DisplayOrderDetails)list.get(a);
					if(null!=displayOrderDetails.getDisplayOrderCode())
					{
						displayOrderDetails.setDisplayOrderCode(displayOrderDetails.getDisplayOrderCode().trim());
					}
					if(null!=displayOrderDetails.getDisplayOrderName())
					{
						displayOrderDetails.setDisplayOrderName(displayOrderDetails.getDisplayOrderName().trim());
					}
					if(null!=displayOrderDetails.getModelType())
					{
						displayOrderDetails.setModelType(displayOrderDetails.getModelType().trim());
					}
					rs = null;
					pstmt= null;
					try
					{
						/*
						 * CHECK WHETHER VIN EXISTS OR NOT
						 * IF YES -  THEN UPDATE VIN
						 * ELSE - INSERT VIN
						 */
						getDisplayOrderSql = "SELECT * FROM gms3_mdm_display_order WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
								+ " TRIM(LOWER(mdm_dispord_code))=? AND TRIM(LOWER(mdm_dispord_model_type)) = ?  "
								+ " AND mdm_dispord_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
						//					logger.info("importDisplayOrderDetails :: getDisplayOrderSql :: > " + getDisplayOrderSql);
						pstmt = conn.prepareStatement(getDisplayOrderSql);
						pstmt.setLong(1, displayOrderDetails.getCountryLocaleId());
						pstmt.setLong(2, displayOrderDetails.getManualLanguageId());
						pstmt.setString(3, displayOrderDetails.getDisplayOrderCode().trim().toLowerCase());
						pstmt.setString(4, displayOrderDetails.getModelType().trim().toLowerCase());
						rs = pstmt.executeQuery();
						autoDisplayOrderId=0;
						if(rs.next())
						{
							autoDisplayOrderId= rs.getLong("mdm_dispord_id");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getDisplayOrderSql = null;

						if(autoDisplayOrderId>0)
						{
							pstmt = null;
							logger.info("importDisplayOrderDetails :: DisplayOrder Already Exists. Update Row for Auto DisplayOrder id : >" + autoDisplayOrderId);

							sql="UPDATE gms3_mdm_display_order SET mdm_dispord_code =?,mdm_dispord_name =?, mdm_dispord_model_type = ? , "
									+ " mdm_dispord_updated_tmstp=? WHERE mdm_dispord_id =?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, displayOrderDetails.getDisplayOrderCode());
							pstmt.setString(2, displayOrderDetails.getDisplayOrderName());
							pstmt.setString(3, displayOrderDetails.getModelType());
							pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setLong(5, autoDisplayOrderId);
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							displayOrderDetails.setSaveStatusWhileImport(true);
						}
						else
						{
							pstmt = null;
							logger.info("importDisplayOrderDetails :: DisplayOrder Does not Exists. Insert New Row.");
							sql="INSERT INTO gms3_mdm_display_order(mdm_cl_id,mdm_ml_id,mdm_dispord_code, mdm_dispord_name, "
									+ " mdm_dispord_flag,mdm_dispord_created_tmstp,mdm_dispord_model_type) VALUES(?,?,?,?,?,?,?)";
							pstmt = conn.prepareStatement(sql);
							pstmt.setLong(1, displayOrderDetails.getCountryLocaleId());
							pstmt.setLong(2, displayOrderDetails.getManualLanguageId());
							pstmt.setString(3, displayOrderDetails.getDisplayOrderCode());
							pstmt.setString(4, displayOrderDetails.getDisplayOrderName());
							pstmt.setString(5, displayOrderDetails.getFlag());
							pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(7, displayOrderDetails.getModelType());
							pstmt.executeUpdate();
							sql = null;
							pstmt.close();pstmt = null;
							displayOrderDetails.setSaveStatusWhileImport(true);
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "importDisplayOrderDetails()", e);
					}
				}
				displayOrderDetails = null;
			}
			else
			{
				logger.info("importDisplayOrderDetails ::  List as Input Parameter is Null. Nothing to Import.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrderDAO.class.getName(), "importDisplayOrderDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		logger.info("importDisplayOrderDetails :: Method Ends.");
		return list;
	}

}
