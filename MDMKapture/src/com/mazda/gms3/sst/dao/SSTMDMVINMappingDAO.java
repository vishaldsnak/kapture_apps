package com.mazda.gms3.sst.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
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
import com.mazda.gms3.sst.vo.SSTMDMVinDetails;

public class SSTMDMVINMappingDAO  extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(SSTMDMVINMappingDAO.class);
	
	public static ArrayList<SSTMDMVinDetails> getSSTMDMVinDetails(String manualLanguageId) throws SQLException
	{
		ArrayList<SSTMDMVinDetails> vinDetailsList = new ArrayList<SSTMDMVinDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=manualLanguageId && !"".equals(manualLanguageId))
			{
				conn = getConnection();
				String sql="SELECT * FROM gms3_sst_vin_mapping WHERE mdm_sstvin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				
				if(null!=manualLanguageId && !"".equals(manualLanguageId) && !"null".equals(manualLanguageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(manualLanguageId).longValue();
				}
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SSTMDMVinDetails details = new SSTMDMVinDetails();
					details.setSrNo(vinDetailsList.size()+1);
					details.setSstMdmVinMappingId(rs.getLong("mdm_sstvin_id"));
					details.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					details.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_crln_code") && !"".equals(rs.getString("mdm_crln_code")))
					{
						details.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng") && !"".equals(rs.getString("mdm_crln_name_eng")))
					{
						details.setCarlineNameEng(rs.getString("mdm_crln_name_eng").trim());
					}
					if(null!=rs.getString("mdm_crln_name_reg") && !"".equals(rs.getString("mdm_crln_name_reg")))
					{
						details.setCarlineNameReg(rs.getString("mdm_crln_name_reg").trim());
					}
					if(null!=rs.getString("mdm_wmi_code") && !"".equals(rs.getString("mdm_wmi_code")))
					{
						details.setWmiCode(rs.getString("mdm_wmi_code").trim());
					}
					if(null!=rs.getString("mdm_vds_code") && !"".equals(rs.getString("mdm_vds_code")))
					{
						details.setVdsCode(rs.getString("mdm_vds_code").trim());
					}
					if(null!=rs.getString("mdm_vis_start_range") && !"".equals(rs.getString("mdm_vis_start_range")))
					{
						details.setVisStart(rs.getString("mdm_vis_start_range").trim());
					}
					if(null!=rs.getString("mdm_vis_end_range") && !"".equals(rs.getString("mdm_vis_end_range")))
					{
						details.setVisEnd(rs.getString("mdm_vis_end_range").trim());
					}
					if(null!=rs.getString("mdm_sst_model_code") && !"".equals(rs.getString("mdm_sst_model_code")))
					{
						details.setSstCarlineCode(rs.getString("mdm_sst_model_code").trim());
					}
					
					/*
					 * PREPARE CARLINE COMBO ID
					 */
					if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
							&& null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
					{
						details.setCarlineComboId(details.getCarlineNameEng().trim().toUpperCase()+"_"+details.getCarlineCode().trim().toUpperCase());
					}
					
					details.setFlag(rs.getString("mdm_sstvin_flag"));
					details.setEntryTime(rs.getTimestamp("mdm_sstvin_created_tmstp"));
					details.setUpdatedTime(rs.getTimestamp("mdm_sstvin_updated_tmstp"));
					if(null!=details.getFlag() && !"".equals(details.getFlag()) && 
							details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						details.setShowCheckBox(false);
					}
					vinDetailsList.add(details);
					details= null;
				}
				sql= null;
			}
			else
			{
				logger.info("getSSTMDMVinDetails() :: Manual Language id as parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "getSSTMDMVinDetails()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			manualLanguageId = null;
		}
		return vinDetailsList;
	}
	
	public static boolean saveSSTMDMVinDetails(SSTMDMVinDetails details) throws SQLException
	{
		logger.info("saveSSTMDMVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
			{
				details.setCarlineCode(details.getCarlineCode().trim());
			}
			if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
			{
				details.setCarlineNameEng(details.getCarlineNameEng().trim());
			}
			if(null!=details.getCarlineNameReg() && !"".equals(details.getCarlineNameReg()))
			{
				details.setCarlineNameReg(details.getCarlineNameReg().trim());
			}
			if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
			{
				details.setWmiCode(details.getWmiCode().trim());
			}
			if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
			{
				details.setVdsCode(details.getVdsCode().trim());
			}
			if(null!=details.getVisStart() && !"".equals(details.getVisStart()))
			{
				details.setVisStart(details.getVisStart().trim());
			}
			if(null!=details.getVisEnd() && !"".equals(details.getVisEnd()))
			{
				details.setVisEnd(details.getVisEnd().trim());
			}
			if(null!=details.getSstCarlineCode() && !"".equals(details.getSstCarlineCode()))
			{
				details.setSstCarlineCode(details.getSstCarlineCode().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_sst_vin_mapping(mdm_cl_id,mdm_ml_id,mdm_crln_code, mdm_crln_name_eng, mdm_crln_name_reg,"
					+ " mdm_wmi_code,mdm_vds_code, mdm_vis_start_range, mdm_vis_end_range, mdm_sst_model_code, mdm_sstvin_flag, "
					+ " mdm_sstvin_created_tmstp)"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getCarlineCode());
			pstmt.setString(4, details.getCarlineNameEng());
			pstmt.setString(5, details.getCarlineNameReg());
			pstmt.setString(6, details.getWmiCode());
			pstmt.setString(7, details.getVdsCode());
			pstmt.setString(8, details.getVisStart());
			pstmt.setString(9, details.getVisEnd());
			pstmt.setString(10, details.getSstCarlineCode());
			pstmt.setString(11, details.getFlag());
			pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "saveSSTMDMVinDetails()", e);
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
		logger.info("saveSSTMDMVinDetails :: Method Ends.");
		return true;
	}

	public static boolean updateSSTMDMVinDetails(SSTMDMVinDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateSSTMDMVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
			{
				details.setCarlineCode(details.getCarlineCode().trim());
			}
			if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
			{
				details.setCarlineNameEng(details.getCarlineNameEng().trim());
			}
			if(null!=details.getCarlineNameReg() && !"".equals(details.getCarlineNameReg()))
			{
				details.setCarlineNameReg(details.getCarlineNameReg().trim());
			}
			if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
			{
				details.setWmiCode(details.getWmiCode().trim());
			}
			if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
			{
				details.setVdsCode(details.getVdsCode().trim());
			}
			if(null!=details.getVisStart() && !"".equals(details.getVisStart()))
			{
				details.setVisStart(details.getVisStart().trim());
			}
			if(null!=details.getVisEnd() && !"".equals(details.getVisEnd()))
			{
				details.setVisEnd(details.getVisEnd().trim());
			}
			if(null!=details.getSstCarlineCode() && !"".equals(details.getSstCarlineCode()))
			{
				details.setSstCarlineCode(details.getSstCarlineCode().trim());
			}
			
			if(null==conn || conn.isClosed())
			{
				conn = getConnection();
			}
			
			String sql="UPDATE gms3_sst_vin_mapping SET mdm_crln_code =?,mdm_crln_name_eng =?,"
					+ " mdm_crln_name_reg=?,mdm_wmi_code=?, mdm_vds_code=?, mdm_vis_start_range=?,"
					+ " mdm_vis_end_range=?, mdm_sst_model_code=?,mdm_sstvin_flag=?,"
					+ " mdm_sstvin_updated_tmstp=?  WHERE mdm_sstvin_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, details.getCarlineCode());
			pstmt.setString(2, details.getCarlineNameEng());
			pstmt.setString(3, details.getCarlineNameReg());
			pstmt.setString(4, details.getWmiCode());
			pstmt.setString(5, details.getVdsCode());
			pstmt.setString(6, details.getVisStart());
			pstmt.setString(7, details.getVisEnd());
			pstmt.setString(8, details.getSstCarlineCode());
			pstmt.setString(9, details.getFlag());
			pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setLong(11, details.getSstMdmVinMappingId());
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
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "updateSSTMDMVinDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			details=  null;
		}
		logger.info("updateSSTMDMVinDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteSSTMDMVinDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteSSTMDMVinDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_vin_mapping SET mdm_sstvin_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " ,mdm_sstvin_updated_tmstp = ?  WHERE mdm_sstvin_id = "+ tokens[i].toString();
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
				logger.info("deleteSSTMDMVinDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteSSTMDMVinDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "deleteSSTMDMVinDetails()", e);
			logger.info("deleteSSTMDMVinDetails :: ################ Exception ################");
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
		logger.info("deleteSSTMDMVinDetails :: Method Ends.");
		return true;
	}

	public static boolean activeSSTMDMVinDetails(String activeIds) throws SQLException
	{
		logger.info("activeSSTMDMVinDetails :: Method Starts.");
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
						String sql="UPDATE gms3_sst_vin_mapping SET mdm_sstvin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
								+ " ,mdm_sstvin_updated_tmstp = ?  WHERE mdm_sstvin_id = "+ tokens[i].toString();
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
				logger.info("activeSSTMDMVinDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeSSTMDMVinDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "activeSSTMDMVinDetails()", e);
			logger.info("activeSSTMDMVinDetails :: ################ Exception ################");
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
		logger.info("activeSSTMDMVinDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importSSTMDMVinDetails(SSTMDMVinDetails details, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importSSTMDMVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
			{
				details.setCarlineCode(details.getCarlineCode().trim());
			}
			if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
			{
				details.setCarlineNameEng(details.getCarlineNameEng().trim());
			}
			if(null!=details.getCarlineNameReg() && !"".equals(details.getCarlineNameReg()))
			{
				details.setCarlineNameReg(details.getCarlineNameReg().trim());
			}
			if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
			{
				details.setWmiCode(details.getWmiCode().trim());
			}
			if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
			{
				details.setVdsCode(details.getVdsCode().trim());
			}
			if(null!=details.getVisStart() && !"".equals(details.getVisStart()))
			{
				details.setVisStart(details.getVisStart().trim());
			}
			if(null!=details.getVisEnd() && !"".equals(details.getVisEnd()))
			{
				details.setVisEnd(details.getVisEnd().trim());
			}
			if(null!=details.getSstCarlineCode() && !"".equals(details.getSstCarlineCode()))
			{
				details.setSstCarlineCode(details.getSstCarlineCode().trim());
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
			String getMappingSql = "SELECT * FROM gms3_sst_vin_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_crln_code=? AND mdm_wmi_code=? AND mdm_vds_code=? AND mdm_vis_start_range=? AND mdm_vis_end_range=? AND"
					+ " mdm_sst_model_code =? AND TRIM(LOWER(mdm_crln_name_eng))= ? "
					+ " AND mdm_sstvin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTMDMVinDetails :: getMappingSql :: > " + getMappingSql);
			pstmt = conn.prepareStatement(getMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getCarlineCode());
			pstmt.setString(4, details.getWmiCode());
			pstmt.setString(5, details.getVdsCode());
			pstmt.setString(6, details.getVisStart());
			pstmt.setString(7, details.getVisEnd());
			pstmt.setString(8, details.getSstCarlineCode());
			pstmt.setString(9, details.getCarlineNameEng().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoSSIVINMappingId=0;
			if(rs.next())
			{
				autoSSIVINMappingId= rs.getLong("mdm_sstvin_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getMappingSql = null;
			
			if(autoSSIVINMappingId>0)
			{
				pstmt = null;
				logger.info("importSSTMDMVinDetails :: Mapping Already Exists. Update Row for Auto Division id : >" + autoSSIVINMappingId);
				
				String sql="UPDATE gms3_sst_vin_mapping SET mdm_crln_code =?,mdm_crln_name_eng =?,"
						+ " mdm_crln_name_reg=?,mdm_wmi_code=?, mdm_vds_code=?, mdm_vis_start_range=?,"
						+ " mdm_vis_end_range=?, mdm_sst_model_code=?,"
						+ " mdm_sstvin_updated_tmstp=?  WHERE mdm_sstvin_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, details.getCarlineCode());
				pstmt.setString(2, details.getCarlineNameEng());
				pstmt.setString(3, details.getCarlineNameReg());
				pstmt.setString(4, details.getWmiCode());
				pstmt.setString(5, details.getVdsCode());
				pstmt.setString(6, details.getVisStart());
				pstmt.setString(7, details.getVisEnd());
				pstmt.setString(8, details.getSstCarlineCode());
				pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setLong(10, autoSSIVINMappingId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importSSTMDMVinDetails :: Mapping Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_sst_vin_mapping(mdm_cl_id,mdm_ml_id,mdm_crln_code, mdm_crln_name_eng, mdm_crln_name_reg,"
						+ " mdm_wmi_code,mdm_vds_code, mdm_vis_start_range, mdm_vis_end_range, mdm_sst_model_code, mdm_sstvin_flag, "
						+ " mdm_sstvin_created_tmstp)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, details.getCountryLocaleId());
				pstmt.setLong(2, details.getManualLanguageId());
				pstmt.setString(3, details.getCarlineCode());
				pstmt.setString(4, details.getCarlineNameEng());
				pstmt.setString(5, details.getCarlineNameReg());
				pstmt.setString(6, details.getWmiCode());
				pstmt.setString(7, details.getVdsCode());
				pstmt.setString(8, details.getVisStart());
				pstmt.setString(9, details.getVisEnd());
				pstmt.setString(10, details.getSstCarlineCode());
				pstmt.setString(11, details.getFlag());
				pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
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
			Utilities.printStackTraceToLogs(SSTMDMVINMappingDAO.class.getName(), "importSSTMDMVinDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			details = null;
		}
		logger.info("importSSTMDMVinDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importSSTMDMVinDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(SSTMDMVinDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getMappingSql = "SELECT * FROM gms3_sst_vin_mapping WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND "
					+ " mdm_crln_code=? AND mdm_wmi_code=? AND mdm_vds_code=? AND mdm_vis_start_range=? AND mdm_vis_end_range=? AND"
					+ " mdm_sst_model_code =? AND TRIM(LOWER(mdm_crln_name_eng))= ? "
					+ " AND mdm_sstvin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importSSTMDMVinDetails :: getMappingSql :: > " + getMappingSql);
			pstmt = conn.prepareStatement(getMappingSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, details.getCarlineCode());
			pstmt.setString(4, details.getWmiCode());
			pstmt.setString(5, details.getVdsCode());
			pstmt.setString(6, details.getVisStart());
			pstmt.setString(7, details.getVisEnd());
			pstmt.setString(8, details.getSstCarlineCode());
			pstmt.setString(9, ImportActionUtils.safeLower(details.getCarlineNameEng()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_sstvin_id");
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