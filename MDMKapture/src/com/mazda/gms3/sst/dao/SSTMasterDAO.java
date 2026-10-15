package com.mazda.gms3.sst.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.sst.vo.SSTDetails;

public class SSTMasterDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SSTMasterDAO.class);
	
	public static ArrayList<SSTDetails> getSSTDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getSSTDetailsList :: Method Starts.");
		ArrayList<SSTDetails> sstMasterList = new ArrayList<SSTDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				// GET LOCALE CODE
				String localeCode = ManualLanguageDAO.getManualLanguageCode(languageId);
				
				conn = getConnection();
				String sql = "SELECT * "
						+ "  FROM gms3_sst_master "
						+ "  WHERE mdm_sst_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getSSTDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTDetails details = new SSTDetails();
					details.setSrNo(sstMasterList.size()+1);
					details.setSstId(rs.getLong("mdm_sst_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_sstimg_code") && !"".equals(rs.getString("mdm_sstimg_code")))
					{
						details.setSstImageCode(rs.getString("mdm_sstimg_code"));
					}
					if(null!=rs.getString("mdm_useimg_code") && !"".equals(rs.getString("mdm_useimg_code")))
					{
						details.setUseImageCode(rs.getString("mdm_useimg_code"));
					}
					if(null!=rs.getString("mdm_sst_name") && !"".equals(rs.getString("mdm_sst_name")))
					{
						details.setSstName(rs.getString("mdm_sst_name"));
					}
					if(null!=rs.getString("mdm_sst_revision") && !"".equals(rs.getString("mdm_sst_revision")))
					{
						details.setSstRevision(rs.getString("mdm_sst_revision"));
					}
					if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
					{
						details.setSstNumber(rs.getString("mdm_sst_number"));
					}
					details.setFlag(rs.getString("mdm_sst_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sst_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sst_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					sstMasterList.add(details);
					details=  null;
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt =null;
				
				// FETCH SST IMAGE PATH AND USE IMAGE PATH
				if(null!=sstMasterList && sstMasterList.size()>0)
				{
					for(SSTDetails details : sstMasterList)
					{
						// SST Image
						if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
						{
							String getSSTImagePathSql="SELECT mdm_sstimg_path FROM gms3_sst_sstimg_master WHERE "
								+ "TRIM(LOWER(mdm_sstimg_code)) = '"+details.getSstImageCode().trim().toLowerCase()+"' AND "
								+ " mdm_sstimg_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"',"
										+ "'"+ApplicationProperties.getProperty("flag.value.delete")+"') AND mdm_ml_id="+details.getManualLanguageId();
							stmt = conn.createStatement();
							rs = stmt.executeQuery(getSSTImagePathSql);
							if(rs.next())
							{
								if(null!=rs.getString("mdm_sstimg_path") && !"".equals(rs.getString("mdm_sstimg_path")))
								{
									String path = ApplicationProperties.getProperty("SST_IMAGES_WB_PATH");
									if(!path.endsWith("/"))
									{
										path= path+"/";
									}
									// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER NAME AS WELL IN THE INTIALS OF THE PATH
									path = ApplicationProperties.getProperty("SST_FOLDER_WB_PATH")+localeCode.trim().toLowerCase()+path+rs.getString("mdm_sstimg_path").trim();
									details.setSstImagePreviewPath(path);
									path= null;
								}
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							getSSTImagePathSql = null;
						}
						
						// Use Image
						if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
						{
							String getUseImagePathSql="SELECT mdm_useimg_path FROM gms3_sst_useimg_master WHERE "
								+ "TRIM(LOWER(mdm_useimg_code)) = '"+details.getUseImageCode().trim().toLowerCase()+"' AND "
								+ " mdm_useimg_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"',"
										+ "'"+ApplicationProperties.getProperty("flag.value.delete")+"') AND mdm_ml_id="+details.getManualLanguageId();
							stmt = conn.createStatement();
							rs = stmt.executeQuery(getUseImagePathSql);
							if(rs.next())
							{
								if(null!=rs.getString("mdm_useimg_path") && !"".equals(rs.getString("mdm_useimg_path")))
								{
									String path = ApplicationProperties.getProperty("USE_IMAGES_WB_PATH");
									if(!path.endsWith("/"))
									{
										path= path+"/";
									}
									// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER NAME AS WELL IN THE INTIALS OF THE PATH
									path = ApplicationProperties.getProperty("SST_FOLDER_WB_PATH")+localeCode.trim().toLowerCase()+path+rs.getString("mdm_useimg_path").trim();
									details.setUseImagePreviewPath(path);
									path= null;
								}
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							getUseImagePathSql = null;
						}
					}
				}
				localeCode=  null;
			}
			else
			{
				logger.info("getSSTDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "getSSTDetailsList()", e);
			logger.info("getSSTDetailsList :: ################ Exception ################");
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
//		logger.info("getSSTDetailsList :: Method Ends.");
		return sstMasterList;
	}

	public static SSTDetails getSSTDetails(String sstId, Connection conn, String localeCode) throws SQLException 
	{
//		logger.info("getSSTDetails :: Method Starts.");
		SSTDetails details = null;
		Statement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=sstId && !"".equals(sstId) && !"null".equals(sstId.trim().toLowerCase()))
			{
				//				conn = getConnection();
				String sql = "SELECT * "
						+ "  FROM gms3_sst_master "
						+ "  WHERE mdm_sst_id="+sstId;
				//				logger.info("getSSTDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					details = new SSTDetails();
					details.setSstId(rs.getLong("mdm_sst_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_sstimg_code") && !"".equals(rs.getString("mdm_sstimg_code")))
					{
						details.setSstImageCode(rs.getString("mdm_sstimg_code"));
					}
					if(null!=rs.getString("mdm_useimg_code") && !"".equals(rs.getString("mdm_useimg_code")))
					{
						details.setUseImageCode(rs.getString("mdm_useimg_code"));
					}
					if(null!=rs.getString("mdm_sst_name") && !"".equals(rs.getString("mdm_sst_name")))
					{
						details.setSstName(rs.getString("mdm_sst_name"));
					}
					if(null!=rs.getString("mdm_sst_revision") && !"".equals(rs.getString("mdm_sst_revision")))
					{
						details.setSstRevision(rs.getString("mdm_sst_revision"));
					}
					if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
					{
						details.setSstNumber(rs.getString("mdm_sst_number"));
					}
					
					details.setFlag(rs.getString("mdm_sst_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sst_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sst_updated_tmstp"));
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt = null;
				
				// HANDLE NULL POINTER 
				if(null==localeCode)
				{
					localeCode ="";
				}
				// SST Image
				if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
				{
					String getSSTImagePathSql="SELECT mdm_sstimg_path FROM gms3_sst_sstimg_master WHERE "
							+ "TRIM(LOWER(mdm_sstimg_code)) = '"+details.getSstImageCode().trim().toLowerCase()+"' AND "
							+ " mdm_sstimg_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"',"
							+ "'"+ApplicationProperties.getProperty("flag.value.delete")+"') AND mdm_ml_id="+details.getManualLanguageId();
					stmt = conn.createStatement();
					rs = stmt.executeQuery(getSSTImagePathSql);
					if(rs.next())
					{
						if(null!=rs.getString("mdm_sstimg_path") && !"".equals(rs.getString("mdm_sstimg_path")))
						{
							String path = ApplicationProperties.getProperty("SST_IMAGES_WB_PATH");
							if(!path.endsWith("/"))
							{
								path= path+"/";
							}
							// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER NAME AS WELL IN THE INTIALS OF THE PATH
							path = ApplicationProperties.getProperty("SST_FOLDER_WB_PATH")+localeCode.trim().toLowerCase()+path+rs.getString("mdm_sstimg_path").trim();
							details.setSstImagePreviewPath(path);
							path= null;
						}
					}
					rs.close();rs=null;
					stmt.close();stmt = null;
					getSSTImagePathSql = null;
				}

				// Use Image
				if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
				{
					String getUseImagePathSql="SELECT mdm_useimg_path FROM gms3_sst_useimg_master WHERE "
							+ "TRIM(LOWER(mdm_useimg_code)) = '"+details.getUseImageCode().trim().toLowerCase()+"' AND "
							+ " mdm_useimg_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"',"
							+ "'"+ApplicationProperties.getProperty("flag.value.delete")+"') AND mdm_ml_id="+details.getManualLanguageId();
					stmt = conn.createStatement();
					rs = stmt.executeQuery(getUseImagePathSql);
					if(rs.next())
					{
						if(null!=rs.getString("mdm_useimg_path") && !"".equals(rs.getString("mdm_useimg_path")))
						{
							String path = ApplicationProperties.getProperty("USE_IMAGES_WB_PATH");
							if(!path.endsWith("/"))
							{
								path= path+"/";
							}
							// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER NAME AS WELL IN THE INTIALS OF THE PATH
							path = ApplicationProperties.getProperty("SST_FOLDER_WB_PATH")+localeCode.trim().toLowerCase()+path+rs.getString("mdm_useimg_path").trim();
							details.setUseImagePreviewPath(path);
							path= null;
						}
					}
					rs.close();rs=null;
					stmt.close();stmt = null;
					getUseImagePathSql = null;
				}

			}
			else
			{
				logger.info("getSSTDetailsList :: SST id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "getSSTDetailsList()", e);
			logger.info("getSSTDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			sstId = null;
		}
//		logger.info("getSSTDetailsList :: Method Ends.");
		return details;
	}
	
	public static ArrayList<SSTDetails> getSSTDetailsListForCombo(String languageId) throws SQLException 
	{
//		logger.info("getSSTDetailsList :: Method Starts.");
		ArrayList<SSTDetails> sstList = new ArrayList<SSTDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_master WHERE "
						+ " mdm_sst_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', '"+ApplicationProperties.getProperty("flag.value.draft")+"')  ";
				
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
//				logger.info("getSSTDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTDetails details = new SSTDetails();
					details.setSrNo(sstList.size()+1);
					details.setSstId(rs.getLong("mdm_sst_id"));
					if(null!=rs.getString("mdm_sst_name") && !"".equals(rs.getString("mdm_sst_name")))
					{
						details.setSstName(rs.getString("mdm_sst_name").trim());
					}
					if(null!=rs.getString("mdm_sst_number") && !"".equals(rs.getString("mdm_sst_number")))
					{
						details.setSstNumber(rs.getString("mdm_sst_number").trim());
					}
					sstList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getSSTDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "getSSTDetailsList()", e);
			logger.info("getSSTDetailsList :: ################ Exception ################");
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
//		logger.info("getSSTDetailsList :: Method Ends.");
		return sstList;
	}
	
	
	
	public static boolean saveSSTDetails(SSTDetails details) throws SQLException
	{
		logger.info("saveSSTDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getSstNumber() && !"".equals(details.getSstNumber()))
			{
				details.setSstNumber(details.getSstNumber().trim());
			}
			if(null!=details.getSstName() && !"".equals(details.getSstName()))
			{
				details.setSstName(details.getSstName().trim());
			}
			if(null!=details.getSstRevision() && !"".equals(details.getSstRevision()))
			{
				details.setSstRevision(details.getSstRevision().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_master(mdm_cl_id,mdm_ml_id,mdm_sstimg_code,mdm_useimg_code,"
					+ " mdm_sst_number, mdm_sst_name, mdm_sst_revision, mdm_sst_flag,"
					+ " mdm_sst_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getSstImageCode());
			pstmt.setString(4, details.getUseImageCode());
			pstmt.setString(5, details.getSstNumber());
			pstmt.setString(6, details.getSstName());
			pstmt.setString(7, details.getSstRevision());
			pstmt.setString(8, details.getFlag());
			pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveSSTDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "saveSSTDetails()", e);
			logger.info("saveSSTDetails :: ################ Exception ################");
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
		logger.info("saveSSTDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSSTDetails(SSTDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSSTDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getSstNumber() && !"".equals(details.getSstNumber()))
			{
				details.setSstNumber(details.getSstNumber().trim());
			}
			if(null!=details.getSstName() && !"".equals(details.getSstName()))
			{
				details.setSstName(details.getSstName().trim());
			}
			if(null!=details.getSstRevision() && !"".equals(details.getSstRevision()))
			{
				details.setSstRevision(details.getSstRevision().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_master SET mdm_sstimg_code=?, mdm_useimg_code=?, mdm_sst_number =?,"
					+ " mdm_sst_name =?,mdm_sst_revision=?, "
					+ " mdm_sst_flag=?,mdm_sst_updated_tmstp=?  WHERE mdm_sst_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getSstImageCode());
			pstmt.setString(2, details.getUseImageCode());
			pstmt.setString(3, details.getSstNumber());
			pstmt.setString(4, details.getSstName());
			pstmt.setString(5, details.getSstRevision());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(8, details.getSstId());
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
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "updateSSTDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSSTDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSSTDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSSTDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_master SET mdm_sst_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sst_updated_tmstp = ?  WHERE mdm_sst_id = "+ tokens[i].toString();
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
				logger.info("deleteSSTDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSSTDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "deleteSSTDetails()", e);
			logger.info("deleteSSTDetails :: ################ Exception ################");
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
		logger.info("deleteSSTDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSSTDetails(String activeIds) throws SQLException
	{
		logger.info("activeSSTDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_master SET mdm_sst_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_sst_updated_tmstp = ?  WHERE mdm_sst_id = "+ tokens[i].toString();
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
				logger.info("activeSSTDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSSTDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "activeSSTDetails()", e);
			logger.info("activeSSTDetails :: ################ Exception ################");
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
		logger.info("activeSSTDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSSTDetails(SSTDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSSTDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getSstNumber() && !"".equals(details.getSstNumber()))
			{
				details.setSstNumber(details.getSstNumber().trim());
			}
			if(null!=details.getSstName() && !"".equals(details.getSstName()))
			{
				details.setSstName(details.getSstName().trim());
			}
			if(null!=details.getSstRevision() && !"".equals(details.getSstRevision()))
			{
				details.setSstRevision(details.getSstRevision().trim());
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
			String getSSTSql = "SELECT * FROM gms3_sst_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_sst_number))=?  "
					+ " AND mdm_sst_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTDetails :: getSSTSql :: > " + getSSTSql);
			pstmt = conn.prepareStatement(getSSTSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getSstNumber().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoSSTId=0;
			if(rs.next())
			{
				autoSSTId= rs.getLong("mdm_sst_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getSSTSql = null;
			
			if(autoSSTId>0)
			{
				pstmt = null;
				logger.info("importSSTDetails :: SST Already Exists. Update Row for Auto SST id : >" + autoSSTId);
				
				String sql="UPDATE gms3_sst_master SET mdm_sstimg_code =?,mdm_useimg_code=?, mdm_sst_number = ?, "
						+ " mdm_sst_name =?, mdm_sst_revision = ? , "
						+ " mdm_sst_updated_tmstp=? WHERE mdm_sst_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getSstImageCode());
				pstmt.setString(2, details.getUseImageCode());
				pstmt.setString(3, details.getSstNumber());
				pstmt.setString(4, details.getSstName());
				pstmt.setString(5, details.getSstRevision());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(7, autoSSTId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSSTDetails :: SST Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_master(mdm_cl_id,mdm_ml_id,mdm_sstimg_code,mdm_useimg_code,mdm_sst_number, "
						+ " mdm_sst_name,mdm_sst_revision, "
						+ " mdm_sst_flag,mdm_sst_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getSstImageCode());
				pstmt.setString(4, details.getUseImageCode());
				pstmt.setString(5, details.getSstNumber());;
				pstmt.setString(6, details.getSstName());
				pstmt.setString(7, details.getSstRevision());
				pstmt.setString(8, details.getFlag());
				pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
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
			Utilities.printStackTraceToLogs(SSTMasterDAO.class.getName(), "importSSTDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSSTDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSSTDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SSTDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getSSTSql = "SELECT * FROM gms3_sst_master WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_sst_number))=?  "
					+ " AND mdm_sst_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTDetails :: getSSTSql :: > " + getSSTSql);
			pstmt = conn.prepareStatement(getSSTSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getSstNumber()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sst_id");
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