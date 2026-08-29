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
import com.mazda.gms3.sst.vo.SSTImageDetails;

public class SSTImagesDAO extends DBConnectionHelper{



	private static Logger logger = LogManager.getLogger(SSTImagesDAO.class);
	
	public static ArrayList<SSTImageDetails> getSSTImageDetailsList(String languageId) throws SQLException 
	{
//		logger.info("getSSTImageDetailsList :: Method Starts.");
		ArrayList<SSTImageDetails> sstImageList = new ArrayList<SSTImageDetails>();
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
				String sql = "SELECT * FROM gms3_sst_sstimg_master WHERE "
						+ "	mdm_sstimg_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id=" + new Long(languageId).longValue();
				}
				//			logger.info("getSSTImageDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTImageDetails details = new SSTImageDetails();
					details.setSrNo(sstImageList.size()+1);
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					details.setSstImageId(rs.getLong("mdm_sstimg_id"));
					if(null!=rs.getString("mdm_sstimg_code") && !"".equals(rs.getString("mdm_sstimg_code")))
					{
						details.setSstImageCode(rs.getString("mdm_sstimg_code").trim());
					}
					if(null!=rs.getString("mdm_sstimg_revision") && !"".equals(rs.getString("mdm_sstimg_revision")))
					{
						details.setSstImageRevision(rs.getString("mdm_sstimg_revision").trim());
					}
					if(null!=rs.getString("mdm_sstimg_path") && !"".equals(rs.getString("mdm_sstimg_path")))
					{
						details.setSstImagePath(rs.getString("mdm_sstimg_path").trim());
					}
					if(null!=details.getSstImagePath() && !"".equals(details.getSstImagePath()))
					{
						String path = ApplicationProperties.getProperty("SST_IMAGES_WB_PATH");
						if(!path.endsWith("/"))
						{
							path= path+"/";
						}
						// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER NAME AS WELL IN THE INTIALS OF THE PATH
						path = ApplicationProperties.getProperty("SST_FOLDER_WB_PATH")+localeCode.trim().toLowerCase()+path+details.getSstImagePath();
						details.setSstImagePreviewPath(path);
						path= null;
					}
					details.setFlag(rs.getString("mdm_sstimg_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sstimg_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sstimg_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					sstImageList.add(details);
					details=  null;
				}
				sql = null;
				localeCode = null;
			}
			else
			{
				logger.info("getSSTImageDetailsList :: Language id as parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getSSTImageDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "getSSTImageDetailsList()", e);
			logger.info("getSSTImageDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			languageId= null;
		}
//		logger.info("getSSTImageDetailsList :: Method Ends.");
		return sstImageList;
	}

	public static boolean saveSSTImageDetails(SSTImageDetails details) throws SQLException
	{
		logger.info("saveSSTImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				details.setSstImageRevision(details.getSstImageRevision().trim());
			}
			if(null!=details.getSstImagePath() && !"".equals(details.getSstImagePath()))
			{
				details.setSstImagePath(details.getSstImagePath().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_sstimg_master(mdm_sstimg_code,mdm_sstimg_revision,mdm_sstimg_path,mdm_sstimg_flag,"
					+ " mdm_sstimg_created_tmstp, mdm_cl_id, mdm_ml_id)"
					+ " VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getSstImageCode());
			pstmt.setString(2, details.getSstImageRevision());
			pstmt.setString(3, details.getSstImagePath());
			pstmt.setString(4, details.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(6, details.getCountryLocaleId());
			pstmt.setLong(7, details.getManualLanguageId());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveSSTImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "saveSSTImageDetails()", e);
			logger.info("saveSSTImageDetails :: ################ Exception ################");
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
		logger.info("saveSSTImageDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSSTImageDetails(SSTImageDetails details , Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSSTImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				details.setSstImageRevision(details.getSstImageRevision().trim());
			}
			if(null!=details.getSstImagePath() && !"".equals(details.getSstImagePath()))
			{
				details.setSstImagePath(details.getSstImagePath().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_sst_sstimg_master SET mdm_sstimg_code =?,mdm_sstimg_revision =?,mdm_sstimg_path = ?, "
					+ " mdm_sstimg_flag=?,mdm_sstimg_updated_tmstp=?  WHERE mdm_sstimg_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getSstImageCode());
			pstmt.setString(2, details.getSstImageRevision());
			pstmt.setString(3, details.getSstImagePath());
			pstmt.setString(4, details.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(6, details.getSstImageId());
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
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "updateSSTImageDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSSTImageDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSSTImageDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSSTImageDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_sstimg_master SET mdm_sstimg_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sstimg_updated_tmstp = ?  WHERE mdm_sstimg_id = "+ tokens[i].toString();
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
				logger.info("deleteSSTImageDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSSTImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "deleteSSTImageDetails()", e);
			logger.info("deleteSSTImageDetails :: ################ Exception ################");
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
		logger.info("deleteSSTImageDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSSTImageDetails(String activeIds) throws SQLException
	{
		logger.info("activeSSTImageDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_sstimg_master SET mdm_sstimg_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_sstimg_updated_tmstp = ?  WHERE mdm_sstimg_id = "+ tokens[i].toString();
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
				logger.info("activeSSTImageDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSSTImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "activeSSTImageDetails()", e);
			logger.info("activeSSTImageDetails :: ################ Exception ################");
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
		logger.info("activeSSTImageDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSSTImageDetails(SSTImageDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSSTImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getSstImageCode() && !"".equals(details.getSstImageCode()))
			{
				details.setSstImageCode(details.getSstImageCode().trim());
			}
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				details.setSstImageRevision(details.getSstImageRevision().trim());
			}
			if(null!=details.getSstImagePath() && !"".equals(details.getSstImagePath()))
			{
				details.setSstImagePath(details.getSstImagePath().trim());
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
			String getSSTImageSql = "SELECT * FROM gms3_sst_sstimg_master WHERE mdm_cl_id=? AND mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_sstimg_code)) = ?  AND TRIM(LOWER(mdm_sstimg_path)) = ? ";
			
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				getSSTImageSql = getSSTImageSql+" AND TRIM(LOWER(mdm_sstimg_revision)) = ? ";
			}
			else
			{
				getSSTImageSql = getSSTImageSql+" AND mdm_sstimg_revision IS NULL ";
			}
			getSSTImageSql = getSSTImageSql	+ " AND mdm_sstimg_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTImageDetails :: getSSTImageSql :: > " + getSSTImageSql);
			pstmt = conn.prepareStatement(getSSTImageSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getSstImageCode().trim().toLowerCase());
			pstmt.setString(4, details.getSstImagePath().trim().toLowerCase());
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				pstmt.setString(5, details.getSstImageRevision().trim().toLowerCase());
			}
			rs = pstmt.executeQuery();
			long autoSSTImageId=0;
			if(rs.next())
			{
				autoSSTImageId= rs.getLong("mdm_sstimg_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getSSTImageSql = null;
			
			if(autoSSTImageId>0)
			{
				pstmt = null;
				logger.info("importSSTImageDetails :: SSTImage Already Exists. Update Row for Auto SSTImage id : >" + autoSSTImageId);
				
				String sql="UPDATE gms3_sst_sstimg_master SET mdm_sstimg_code =?,mdm_sstimg_revision =?,mdm_sstimg_path = ?, "
						+ " mdm_sstimg_updated_tmstp=?  WHERE mdm_sstimg_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getSstImageCode());
				pstmt.setString(2, details.getSstImageRevision());
				pstmt.setString(3, details.getSstImagePath());
				pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(5, autoSSTImageId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSSTImageDetails :: SSTImage Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_sstimg_master(mdm_sstimg_code,mdm_sstimg_revision,mdm_sstimg_path,mdm_sstimg_flag,"
						+ " mdm_sstimg_created_tmstp,mdm_cl_id, mdm_ml_id)"
						+ " VALUES(?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getSstImageCode());
				pstmt.setString(2, details.getSstImageRevision());
				pstmt.setString(3, details.getSstImagePath());
				pstmt.setString(4, details.getFlag());
				pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(6, details.getCountryLocaleId());
				pstmt.setLong(7, details.getManualLanguageId());
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
			Utilities.printStackTraceToLogs(SSTImagesDAO.class.getName(), "importSSTImageDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSSTImageDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSSTImageDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SSTImageDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getSSTImageSql = "SELECT * FROM gms3_sst_sstimg_master WHERE mdm_cl_id=? AND mdm_ml_id=? AND "
					+ " TRIM(LOWER(mdm_sstimg_code)) = ?  AND TRIM(LOWER(mdm_sstimg_path)) = ? ";
			
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				getSSTImageSql = getSSTImageSql+" AND TRIM(LOWER(mdm_sstimg_revision)) = ? ";
			}
			else
			{
				getSSTImageSql = getSSTImageSql+" AND mdm_sstimg_revision IS NULL ";
			}
			getSSTImageSql = getSSTImageSql	+ " AND mdm_sstimg_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTImageDetails :: getSSTImageSql :: > " + getSSTImageSql);
			pstmt = conn.prepareStatement(getSSTImageSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getSstImageCode()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getSstImagePath()));
			if(null!=details.getSstImageRevision() && !"".equals(details.getSstImageRevision()))
			{
				pstmt.setString(5, ImportActionUtils.safeLower(details.getSstImageRevision()));
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sstimg_id");
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
