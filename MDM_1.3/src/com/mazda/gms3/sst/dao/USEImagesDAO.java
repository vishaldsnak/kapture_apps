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
import com.mazda.gms3.sst.vo.USEImageDetails;

public class USEImagesDAO extends DBConnectionHelper {

	private static Logger logger = LogManager.getLogger(USEImagesDAO.class);

	public static ArrayList<USEImageDetails> getUSEImageDetailsList(
			String languageId) throws SQLException {
		// logger.info("getUSEImageDetailsList :: Method Starts.");
		ArrayList<USEImageDetails> useImageList = new ArrayList<USEImageDetails>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if (null != languageId && !"".equals(languageId)) {
				// GET LOCALE CODE
				String localeCode = ManualLanguageDAO
						.getManualLanguageCode(languageId);

				conn = getConnection();
				String sql = "SELECT * FROM gms3_sst_useimg_master WHERE ";
				sql = sql
						+ " mdm_useimg_flag NOT IN ('"
						+ ApplicationProperties
								.getProperty("flag.value.delete") + "')";
				if (null != languageId && !"".equals(languageId)
						&& !"null".equals(languageId.trim().toLowerCase())) {
					sql = sql + " AND mdm_ml_id=" + languageId;
				}
				// logger.info("getUSEImageDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					USEImageDetails details = new USEImageDetails();
					details.setSrNo(useImageList.size() + 1);
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					details.setUseImageId(rs.getLong("mdm_useimg_id"));
					if(null!=rs.getString("mdm_useimg_code") && !"".equals(rs.getString("mdm_useimg_code")))
					{
						details.setUseImageCode(rs.getString("mdm_useimg_code").trim());
					}
					if(null!=rs.getString("mdm_useimg_revision") && !"".equals(rs.getString("mdm_useimg_revision")))
					{
						details.setUseImageRevision(rs
								.getString("mdm_useimg_revision").trim());
					}
					if(null!=rs.getString("mdm_useimg_path") && !"".equals(rs.getString("mdm_useimg_path")))
					{
						details.setUseImagePath(rs.getString("mdm_useimg_path").trim());
					}
					if (null != details.getUseImagePath()
							&& !"".equals(details.getUseImagePath())) {
						String path = ApplicationProperties
								.getProperty("USE_IMAGES_WB_PATH");
						if (!path.endsWith("/")) {
							path = path + "/";
						}
						// ADD SPECIAL SERVICE TOOL FOLDER NAME & LOCALE FOLDER
						// NAME AS WELL IN THE INTIALS OF THE PATH
						path = ApplicationProperties
								.getProperty("SST_FOLDER_WB_PATH")
								+ localeCode.trim().toLowerCase()
								+ path
								+ details.getUseImagePath();
						details.setUseImagePreviewPath(path);
						path = null;
					}
					details.setFlag(rs.getString("mdm_useimg_flag"));
					details.setEntryTime(rs
							.getTimestamp("mdm_useimg_created_tmstp"));
					details.setUpdatedTime(rs
							.getTimestamp("mdm_useimg_updated_tmstp"));
					if (null != details.getFlag()
							&& !"".equals(details.getFlag())
							&& details
									.getFlag()
									.equals(ApplicationProperties
											.getProperty("flag.value.deprecated"))) {
						details.setShowCheckBox(false);
					}
					useImageList.add(details);
					details = null;
				}
				sql = null;
				localeCode = null;
			} else {
				logger.info("getUSEImageDetailsList :: Language id as parameter is null. Return null.");
			}
		} catch (Exception e) {
			logger.info("getUSEImageDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"getUSEImageDetailsList()", e);
			logger.info("getUSEImageDetailsList :: ################ Exception ################");
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			languageId = null;
		}
		// logger.info("getUSEImageDetailsList :: Method Ends.");
		return useImageList;
	}

	public static boolean saveUSEImageDetails(USEImageDetails details)
			throws SQLException {
		logger.info("saveUSEImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getUseImageRevision() && !"".equals(details.getUseImageRevision()))
			{
				details.setUseImageRevision(details.getUseImageRevision().trim());
			}
			if(null!=details.getUseImagePath() && !"".equals(details.getUseImagePath()))
			{
				details.setUseImagePath(details.getUseImagePath().trim());
			}
			conn = getConnection();
			String sql = "INSERT INTO gms3_sst_useimg_master(mdm_cl_id, mdm_ml_id, mdm_useimg_code,mdm_useimg_revision,mdm_useimg_path,mdm_useimg_flag,"
					+ " mdm_useimg_created_tmstp)" + " VALUES(?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getUseImageCode());
			pstmt.setString(4, details.getUseImageRevision());
			pstmt.setString(5, details.getUseImagePath());
			pstmt.setString(6, details.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		} catch (Exception e) {
			logger.info("saveUSEImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"saveUSEImageDetails()", e);
			logger.info("saveUSEImageDetails :: ################ Exception ################");
			return false;
		} finally {
			if (null != pstmt)
				pstmt.close();
			if (null != conn)
				conn.close();
			// set passed param to null
			details = null;
		}
		logger.info("saveUSEImageDetails :: Method Ends.");
		return true;
	}

	public static boolean updateUSEImageDetails(USEImageDetails details , Connection conn, String closeConnection)
			throws SQLException {
		logger.info("updateUSEImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try {
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getUseImageRevision() && !"".equals(details.getUseImageRevision()))
			{
				details.setUseImageRevision(details.getUseImageRevision().trim());
			}
			if(null!=details.getUseImagePath() && !"".equals(details.getUseImagePath()))
			{
				details.setUseImagePath(details.getUseImagePath().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			String sql = "UPDATE gms3_sst_useimg_master SET mdm_useimg_code =?,mdm_useimg_revision =?,mdm_useimg_path = ?, "
					+ " mdm_useimg_flag=?,mdm_useimg_updated_tmstp=?  WHERE mdm_useimg_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getUseImageCode());
			pstmt.setString(2, details.getUseImageRevision());
			pstmt.setString(3, details.getUseImagePath());
			pstmt.setString(4, details.getFlag());
			pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(6, details.getUseImageId());
			pstmt.executeUpdate();
			sql = null;
			if(null==closeConnection)
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"updateUSEImageDetails()", e);
			return false;
		} finally {
			if (null != pstmt)
				pstmt.close();
			// set passed parameter to null
			details = null;
		}
		logger.info("updateUSEImageDetails :: Method Ends.");
		return true;
	}

	public static boolean deleteUSEImageDetails(String deleteIds)
			throws SQLException {
		logger.info("deleteUSEImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			if (null != deleteIds && !"".equals(deleteIds)) {
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens = deleteIds.split(",");
				if (null != tokens && tokens.length > 0) {
					for (int i = 0; i < tokens.length; i++) {
						pstmt = null;
						String sql = "UPDATE gms3_sst_useimg_master SET mdm_useimg_flag='"
								+ ApplicationProperties
										.getProperty("flag.value.delete")
								+ "' "
								+ " ,mdm_useimg_updated_tmstp = ?  WHERE mdm_useimg_id = "
								+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1,
								new java.sql.Timestamp(new Date().getTime()));
						pstmt.executeUpdate();
						pstmt.close();
						pstmt = null;
						sql = null;
					}
				}
				// commit the transaction
				conn.commit();
				tokens = null;
			} else {
				logger.info("deleteUSEImageDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		} catch (Exception e) {
			conn.rollback();
			logger.info("deleteUSEImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"deleteUSEImageDetails()", e);
			logger.info("deleteUSEImageDetails :: ################ Exception ################");
			return false;
		} finally {
			if (null != pstmt)
				pstmt.close();
			if (null != conn)
				conn.close();
			// set passed parameter to null
			deleteIds = null;
		}
		logger.info("deleteUSEImageDetails :: Method Ends.");
		return true;
	}

	public static boolean activeUSEImageDetails(String activeIds)
			throws SQLException {
		logger.info("activeUSEImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			if (null != activeIds && !"".equals(activeIds)) {
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens = activeIds.split(",");
				if (null != tokens && tokens.length > 0) {
					for (int i = 0; i < tokens.length; i++) {
						pstmt = null;
						String sql = "UPDATE gms3_sst_useimg_master SET mdm_useimg_flag='"
								+ ApplicationProperties
										.getProperty("flag.value.active")
								+ "' "
								+ " ,mdm_useimg_updated_tmstp = ?  WHERE mdm_useimg_id = "
								+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1,
								new java.sql.Timestamp(new Date().getTime()));
						pstmt.executeUpdate();
						pstmt.close();
						pstmt = null;
						sql = null;
					}
				}
				// commit the transaction
				conn.commit();
				tokens = null;
			} else {
				logger.info("activeUSEImageDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		} catch (Exception e) {
			conn.rollback();
			logger.info("activeUSEImageDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"activeUSEImageDetails()", e);
			logger.info("activeUSEImageDetails :: ################ Exception ################");
			return false;
		} finally {
			if (null != pstmt)
				pstmt.close();
			if (null != conn)
				conn.close();
			// set passed parameter to null
			activeIds = null;
		}
		logger.info("activeUSEImageDetails :: Method Ends.");
		return true;
	}

	public static boolean importUSEImageDetails(USEImageDetails details, Connection conn, String closeConnection)
			throws SQLException {
		logger.info("importUSEImageDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if(null!=details.getUseImageCode() && !"".equals(details.getUseImageCode()))
			{
				details.setUseImageCode(details.getUseImageCode().trim());
			}
			if(null!=details.getUseImageRevision() && !"".equals(details.getUseImageRevision()))
			{
				details.setUseImageRevision(details.getUseImageRevision().trim());
			}
			if(null!=details.getUseImagePath() && !"".equals(details.getUseImagePath()))
			{
				details.setUseImagePath(details.getUseImagePath().trim());
			}
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			rs = null;
			pstmt = null;
			/*
			 * CHECK WHETHER VIN EXISTS OR NOT IF YES - THEN UPDATE VIN ELSE -
			 * INSERT VIN
			 */
			String getUSEImageSql = "SELECT * FROM gms3_sst_useimg_master WHERE mdm_cl_id = ? AND mdm_ml_id = ? AND  "
					+ " TRIM(LOWER(mdm_useimg_code)) =? AND TRIM(LOWER(mdm_useimg_path)) = ? ";

			if (null != details.getUseImageRevision()
					&& !"".equals(details.getUseImageRevision())) {
				getUSEImageSql = getUSEImageSql
						+ " AND TRIM(LOWER(mdm_useimg_revision)) = ? ";
			} else {
				getUSEImageSql = getUSEImageSql
						+ " AND mdm_useimg_revision IS NULL ";
			}
			getUSEImageSql = getUSEImageSql + " AND mdm_useimg_flag  NOT IN ('"
					+ ApplicationProperties.getProperty("flag.value.delete")
					+ "')";
			// logger.info("importUSEImageDetails :: getUSEImageSql :: > " +
			// getUSEImageSql);
			pstmt = conn.prepareStatement(getUSEImageSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getUseImageCode().trim().toLowerCase());
			pstmt.setString(4, details.getUseImagePath().trim().toLowerCase());
			if (null != details.getUseImageRevision()
					&& !"".equals(details.getUseImageRevision())) {
				pstmt.setString(5, details.getUseImageRevision().trim()
						.toLowerCase());
			}
			rs = pstmt.executeQuery();
			long autoUSEImageId = 0;
			if (rs.next()) {
				autoUSEImageId = rs.getLong("mdm_useimg_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getUSEImageSql = null;

			if (autoUSEImageId > 0) {
				pstmt = null;
				logger.info("importUSEImageDetails :: USEImage Already Exists. Update Row for Auto USEImage id : >"
						+ autoUSEImageId);

				String sql = "UPDATE gms3_sst_useimg_master SET mdm_useimg_code =?,mdm_useimg_revision =?,mdm_useimg_path = ?, "
						+ " mdm_useimg_updated_tmstp=?  WHERE mdm_useimg_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getUseImageCode());
				pstmt.setString(2, details.getUseImageRevision());
				pstmt.setString(3, details.getUseImagePath());
				pstmt.setTimestamp(4,
						new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(5, autoUSEImageId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();
				pstmt = null;
			} else {
				pstmt = null;
				logger.info("importUSEImageDetails :: USEImage Does not Exists. Insert New Row.");
				String sql = "INSERT INTO gms3_sst_useimg_master(mdm_cl_id, mdm_ml_id, mdm_useimg_code,mdm_useimg_revision,mdm_useimg_path,mdm_useimg_flag,"
						+ " mdm_useimg_created_tmstp)"
						+ " VALUES(?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getUseImageCode());
				pstmt.setString(4, details.getUseImageRevision());
				pstmt.setString(5, details.getUseImagePath());
				pstmt.setString(6, details.getFlag());
				pstmt.setTimestamp(7,
						new java.sql.Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();
				pstmt = null;
			}
			if(null==closeConnection)
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(USEImagesDAO.class.getName(),
					"importUSEImageDetails()", e);
			return false;
		} finally {
			if (null != pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importUSEImageDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importUSEImageDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(USEImageDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getUSEImageSql = "SELECT * FROM gms3_sst_useimg_master WHERE mdm_cl_id = ? AND mdm_ml_id = ? AND  "
					+ " TRIM(LOWER(mdm_useimg_code)) =? AND TRIM(LOWER(mdm_useimg_path)) = ? ";

			if (null != details.getUseImageRevision()
					&& !"".equals(details.getUseImageRevision())) {
				getUSEImageSql = getUSEImageSql
						+ " AND TRIM(LOWER(mdm_useimg_revision)) = ? ";
			} else {
				getUSEImageSql = getUSEImageSql
						+ " AND mdm_useimg_revision IS NULL ";
			}
			getUSEImageSql = getUSEImageSql + " AND mdm_useimg_flag  NOT IN ('"
					+ ApplicationProperties.getProperty("flag.value.delete")
					+ "')";
			// logger.info("importUSEImageDetails :: getUSEImageSql :: > " +
			// getUSEImageSql);
			pstmt = conn.prepareStatement(getUSEImageSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getUseImageCode()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getUseImagePath()));
			if (null != details.getUseImageRevision()
					&& !"".equals(details.getUseImageRevision())) {
				pstmt.setString(5, ImportActionUtils.safeLower(details.getUseImageRevision()));
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_useimg_id");
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
