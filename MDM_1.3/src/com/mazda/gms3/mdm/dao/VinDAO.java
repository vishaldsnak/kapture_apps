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
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.mdm.vo.VinDetails;

public class VinDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(VinDAO.class);
	
	public static ArrayList<VinDetails> getVinDetailsList(String carlineNameEng, String carlineCode, String manualLanguageId) throws SQLException 
	{
//		logger.info("getVinDetailsList :: Method Starts.");
		ArrayList<VinDetails> vinList = new ArrayList<VinDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if((null!=manualLanguageId && !"".equals(manualLanguageId)))
			{
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_vin_detail   "
						+ "WHERE mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
				if(null!=manualLanguageId && !"".equals(manualLanguageId) && !"null".equals(manualLanguageId.trim().toLowerCase()))
				{
					sql = sql +" AND mdm_ml_id="+ manualLanguageId.trim();
				}
				if(null!=carlineNameEng && !"".equals(carlineNameEng) && !"null".equals(carlineNameEng.trim().toLowerCase()))
				{
					sql = sql+" AND TRIM(LOWER(mdm_crln_name_eng_lang))='"+carlineNameEng.trim().toLowerCase()+"' ";
				}
				if(null!=carlineCode && !"".equals(carlineCode) && !"null".equals(carlineCode.trim().toLowerCase()))
				{
					sql = sql + " AND TRIM(LOWER(mdm_crln_code)) = '" + carlineCode.trim().toLowerCase()+"'";
				}
//				logger.info("getVinDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					VinDetails vinDetails = new VinDetails();
					vinDetails.setSrNo(vinList.size()+1);
					vinDetails.setVinId(rs.getLong("mdm_vin_id"));
					vinDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					if(null!=rs.getString("mdm_cl_locale_code"))
					{
						vinDetails.setCountryLocaleCode(rs.getString("mdm_cl_locale_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						vinDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang"))
					{
						vinDetails.setCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
					}
					if(null!=rs.getString("mdm_ic_display_carline_code"))
					{
						vinDetails.setIcDisplayCarlineCode(rs.getString("mdm_ic_display_carline_code").trim());
					}
					
					vinDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code"))
					{
						vinDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_crln_code"))
					{
						vinDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_vin_group_code"))
					{
						vinDetails.setGroupCode(rs.getString("mdm_vin_group_code").trim());
					}
					if(null!=rs.getString("mdm_vin_wmi_code"))
					{
						vinDetails.setWmiCode(rs.getString("mdm_vin_wmi_code").trim());
						vinDetails.setOldWmiCode(rs.getString("mdm_vin_wmi_code").trim());
					}
					if(null!=rs.getString("mdm_vin_vds_code"))
					{
						vinDetails.setVdsCode(rs.getString("mdm_vin_vds_code").trim());
						vinDetails.setOldVdsCode(rs.getString("mdm_vin_vds_code").trim());
					}
					if(null!=rs.getString("mdm_vin_vis_start_range"))
					{
						vinDetails.setVisStartRange(rs.getString("mdm_vin_vis_start_range").trim());
						vinDetails.setOldVisStartRange(rs.getString("mdm_vin_vis_start_range").trim());
					}
					if(null!=rs.getString("mdm_vin_vis_end_range"))
					{
						vinDetails.setVisEndRange(rs.getString("mdm_vin_vis_end_range").trim());
						vinDetails.setOldVisEndRange(rs.getString("mdm_vin_vis_end_range").trim());
					}
					if(null!=rs.getString("mdm_vin_engine_code"))
					{
						vinDetails.setEngineCode(rs.getString("mdm_vin_engine_code").trim());
					}
					if(null!=rs.getString("mdm_vin_transmission_code"))
					{
						vinDetails.setMissionCode(rs.getString("mdm_vin_transmission_code").trim());
					}
					vinDetails.setFlag(rs.getString("mdm_vin_flag"));
					vinDetails.setOldFlag(rs.getString("mdm_vin_flag"));
					vinDetails.setEntryTime(rs.getTimestamp("mdm_vin_created_tmstp"));
					vinDetails.setUpdatedTime(rs.getTimestamp("mdm_vin_updated_tmstp"));
					if(null!=vinDetails.getFlag() && !"".equals(vinDetails.getFlag()) && 
							vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						vinDetails.setShowCheckBox(false);
					}
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						vinDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					vinList.add(vinDetails);
					vinDetails=  null;
				}
				sql = null;
				stmt.close();stmt=null;
				rs.close();rs=null;
				
				if(null!=vinList && vinList.size()>0)
				{
					for(int a=0;a<vinList.size();a++)
					{
						VinDetails vinDetails  = (VinDetails)vinList.get(a);
						// set wmiList to new - here language setting is not required- as no en_ca data will come
						vinDetails.setWmiCodeList(new ArrayList<SelectItemDetails>());
						if(null!=vinDetails.getCarlineNameEng() && !"".equals(vinDetails.getCarlineNameEng()) && null!=vinDetails.getCarlineCode() 
								&& !"".equals(vinDetails.getCarlineCode()) && null!=vinDetails.getManualLanguageId() && vinDetails.getManualLanguageId()>0)
						{
							try
							{
								ArrayList<SelectItemDetails> list = CarlineDAO.getWMIList(vinDetails.getCarlineCode(), vinDetails.getCarlineNameEng(), String.valueOf(vinDetails.getManualLanguageId()), conn,"N");
								if(null!=list)
								{
									vinDetails.setWmiCodeList(list);
								}
								list = null;
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(VinDAO.class.getName(), "getVinDetailsList()", e);
							}
						}
					}
				}
			}
			else
			{
				logger.info("getVinDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "getVinDetailsList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			carlineCode = null;
//			carlineNameEng = null;
			manualLanguageId = null;
		}
//		logger.info("getVinDetailsList :: Method Ends.");
		return vinList;
	}

	public static boolean saveVinDetails(VinDetails vinDetails) throws SQLException
	{
		logger.info("saveVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=vinDetails.getWmiCode())
			{
				vinDetails.setWmiCode(vinDetails.getWmiCode().trim());
			}
			if(null!=vinDetails.getVdsCode())
			{
				vinDetails.setVdsCode(vinDetails.getVdsCode().trim());
			}
			if(null!=vinDetails.getVisStartRange())
			{
				vinDetails.setVisStartRange(vinDetails.getVisStartRange().trim());
			}
			if(null!=vinDetails.getVisEndRange())
			{
				vinDetails.setVisEndRange(vinDetails.getVisEndRange().trim());
			}
			if(null!=vinDetails.getEngineCode())
			{
				vinDetails.setEngineCode(vinDetails.getEngineCode().trim());
			}
			if(null!=vinDetails.getMissionCode())
			{
				vinDetails.setMissionCode(vinDetails.getMissionCode().trim());
			}
			if(null!=vinDetails.getCountryLocaleCode())
			{
				vinDetails.setCountryLocaleCode(vinDetails.getCountryLocaleCode().trim());
			}
			if(null!=vinDetails.getManualLanguageCode())
			{
				vinDetails.setManualLanguageCode(vinDetails.getManualLanguageCode().trim());
			}
			if(null!=vinDetails.getCarlineCode())
			{
				vinDetails.setCarlineCode(vinDetails.getCarlineCode().trim());
			}
			if(null!=vinDetails.getCarlineNameEng())
			{
				vinDetails.setCarlineNameEng(vinDetails.getCarlineNameEng().trim());
			}
			if(null!=vinDetails.getCarlineNameReg())
			{
				vinDetails.setCarlineNameReg(vinDetails.getCarlineNameReg().trim());
			}
			if(null!=vinDetails.getGroupCode())
			{
				vinDetails.setGroupCode(vinDetails.getGroupCode().trim());
			}
			if(null!=vinDetails.getIcDisplayCarlineCode())
			{
				vinDetails.setIcDisplayCarlineCode(vinDetails.getIcDisplayCarlineCode().trim());
			}
			conn = getConnection();
			String sql="INSERT INTO gms3_mdm_vin_detail(mdm_cl_id,mdm_ml_id,mdm_vin_wmi_code, mdm_vin_vds_code, mdm_vin_vis_start_range,mdm_vin_vis_end_range"
					+ ",mdm_vin_engine_code, mdm_vin_transmission_code,mdm_vin_flag,mdm_vin_created_tmstp,mdm_vin_group_code"
					+ ", mdm_cl_locale_code, mdm_ml_lang_code,mdm_crln_name_eng_lang, "
					+ " mdm_crln_name_regional_lang,mdm_crln_code,mdm_ic_display_carline_code )"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, vinDetails.getCountryLocaleId());
			pstmt.setLong(2, vinDetails.getManualLanguageId());
			pstmt.setString(3, vinDetails.getWmiCode());
			pstmt.setString(4, vinDetails.getVdsCode());
			pstmt.setString(5, vinDetails.getVisStartRange());
			pstmt.setString(6, vinDetails.getVisEndRange().toUpperCase());
			pstmt.setString(7, vinDetails.getEngineCode());
			pstmt.setString(8, vinDetails.getMissionCode());
			pstmt.setString(9, vinDetails.getFlag());
			pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(11, vinDetails.getGroupCode());
			pstmt.setString(12, vinDetails.getCountryLocaleCode());
			pstmt.setString(13, vinDetails.getManualLanguageCode());
			pstmt.setString(14, vinDetails.getCarlineNameEng());
			pstmt.setString(15, vinDetails.getCarlineNameReg());
			pstmt.setString(16, vinDetails.getCarlineCode());
			pstmt.setString(17, vinDetails.getIcDisplayCarlineCode());
			pstmt.executeUpdate();
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("saveVinDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "saveVinDetails()", e);
			logger.info("saveVinDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			vinDetails = null;
		}
		logger.info("saveVinDetails :: Method Ends.");
		return true;
	}

	public static boolean updateVinDetails(VinDetails vinDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("updateVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		try
		{
			if(null!=vinDetails.getWmiCode())
			{
				vinDetails.setWmiCode(vinDetails.getWmiCode().trim());
			}
			if(null!=vinDetails.getVdsCode())
			{
				vinDetails.setVdsCode(vinDetails.getVdsCode().trim());
			}
			if(null!=vinDetails.getVisStartRange())
			{
				vinDetails.setVisStartRange(vinDetails.getVisStartRange().trim());
			}
			if(null!=vinDetails.getVisEndRange())
			{
				vinDetails.setVisEndRange(vinDetails.getVisEndRange().trim());
			}
			if(null!=vinDetails.getEngineCode())
			{
				vinDetails.setEngineCode(vinDetails.getEngineCode().trim());
			}
			if(null!=vinDetails.getMissionCode())
			{
				vinDetails.setMissionCode(vinDetails.getMissionCode().trim());
			}
			if(null!=vinDetails.getCountryLocaleCode())
			{
				vinDetails.setCountryLocaleCode(vinDetails.getCountryLocaleCode().trim());
			}
			if(null!=vinDetails.getManualLanguageCode())
			{
				vinDetails.setManualLanguageCode(vinDetails.getManualLanguageCode().trim());
			}
			if(null!=vinDetails.getCarlineCode())
			{
				vinDetails.setCarlineCode(vinDetails.getCarlineCode().trim());
			}
			if(null!=vinDetails.getCarlineNameEng())
			{
				vinDetails.setCarlineNameEng(vinDetails.getCarlineNameEng().trim());
			}
			if(null!=vinDetails.getCarlineNameReg())
			{
				vinDetails.setCarlineNameReg(vinDetails.getCarlineNameReg().trim());
			}
			if(null!=vinDetails.getIcDisplayCarlineCode())
			{
				vinDetails.setIcDisplayCarlineCode(vinDetails.getIcDisplayCarlineCode().trim());
			}
			if(null!=vinDetails.getGroupCode())
			{
				vinDetails.setGroupCode(vinDetails.getGroupCode().trim());
			}
			
			if(null!=vinDetails.getOldWmiCode())
			{
				vinDetails.setOldWmiCode(vinDetails.getOldWmiCode().trim());
			}
			if(null!=vinDetails.getOldVdsCode())
			{
				vinDetails.setOldVdsCode(vinDetails.getOldVdsCode().trim());
			}
			if(null!=vinDetails.getOldVisStartRange())
			{
				vinDetails.setOldVisStartRange(vinDetails.getOldVisStartRange().trim());
			}
			if(null!=vinDetails.getOldVisEndRange())
			{
				vinDetails.setOldVisEndRange(vinDetails.getOldVisEndRange().trim());
			}
			if(null!=vinDetails.getSyncStatus() && !"".equals(vinDetails.getSyncStatus()))
			{
				vinDetails.setSyncStatus(vinDetails.getSyncStatus().trim());
			}
			
			String syncStatus = vinDetails.getSyncStatus();
			/*
			 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
				If PreviousStatus and CurrentStatus is Same and Status is Active - 
					if WMI / VDS / VIS START / VIS END CHANGES , then update syncStatus as N

				If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
				If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
			 */
			if(vinDetails.getOldFlag().equals(vinDetails.getFlag()))
			{
				// STATUS IS SAME - check only for active
				if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					if(vinDetails.getOldWmiCode().trim().toLowerCase().equals(vinDetails.getWmiCode().trim().toLowerCase()) 
							&& vinDetails.getOldVdsCode().trim().toLowerCase().equals(vinDetails.getVdsCode().trim().toLowerCase()) 
							&& vinDetails.getOldVisStartRange().trim().toLowerCase().equals(vinDetails.getVisStartRange().trim().toLowerCase()) 
							&& vinDetails.getOldVisEndRange().trim().toLowerCase().equals(vinDetails.getVisEndRange().trim().toLowerCase()))
					{
						// ALL ARE SAME -DO NOTHING WITH SYNC STATUS
					}
					else
					{
						// SOMETHING CHANGED - SET SYNC STATUS TO N
						syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
					}
				}
			}
			else
			{
				if(vinDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
						vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
				{
					// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
					syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
				}
				else if(vinDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
						vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
				{
					// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
					syncStatus = null;
				}
			}
			
			
			
			// do this when object is passed as null
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			String sql="UPDATE gms3_mdm_vin_detail SET mdm_vin_wmi_code =?,mdm_vin_vds_code =?,"
					+ "mdm_vin_vis_start_range=?,mdm_vin_engine_code=?,mdm_vin_transmission_code=?,"
					+ "mdm_vin_flag=?,mdm_vin_updated_tmstp=?, mdm_vin_group_code=?,mdm_vin_vis_end_range=?,mdm_sync_status = ? "
					+ "  WHERE mdm_vin_id =?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, vinDetails.getWmiCode());
			pstmt.setString(2, vinDetails.getVdsCode());
			pstmt.setString(3, vinDetails.getVisStartRange());
			pstmt.setString(4, vinDetails.getEngineCode());
			pstmt.setString(5, vinDetails.getMissionCode());
			pstmt.setString(6, vinDetails.getFlag());
			pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
			pstmt.setString(8, vinDetails.getGroupCode());
			pstmt.setString(9, vinDetails.getVisEndRange().toUpperCase());
			pstmt.setString(10, syncStatus);
			pstmt.setLong(11, vinDetails.getVinId());
			pstmt.executeUpdate();
			sql = null;
			if(null==closeConnection)
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
			syncStatus = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "updateVinDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed parameter to null
			vinDetails=  null;
		}
		logger.info("updateVinDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteVinDetails(List<VinDetails> deleteIdsList) throws SQLException
	{
		logger.info("deleteVinDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=deleteIdsList && deleteIdsList.size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				VinDetails vinDetails = null;
				for(int i=0;i<deleteIdsList.size();i++)
				{
					vinDetails = (VinDetails)deleteIdsList.get(i);
					// HERE FOR ALL RECORDS GETTING INACTIVE - SET SYNC STATUS TO NULL
					pstmt = null;
					String sql="UPDATE gms3_mdm_vin_detail SET mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
							+ " ,mdm_vin_updated_tmstp = ? , mdm_sync_status=?   WHERE mdm_vin_id = "+ String.valueOf(vinDetails.getVinId());
					pstmt = conn.prepareStatement(sql);
					pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(2, null);
					pstmt.executeUpdate();
					pstmt.close();pstmt= null;
					sql = null;

					/*
					 * CHANGE 4 AUGUST 2019
					 * NO UPDATE STATUS HARD DELETE FROM GMS3_MDM_VIN_ML_MAPPING
					 */
					
					/*
					 * UPDATE THE STATUS OF THE VIN  IN TABLE - GMS3_MDM_VIN_ML_MAPPING, UPDATE STATUS AS INACTIVE
					 * CHECK ON THE BASIS OF CARLINE CODE + WMI CODE + VDS CODE + VIS START + VIS END
					 */
//					sql = "UPDATE gms3_mdm_vin_ml_mapping SET vin_ml_status='I' WHERE TRIM(LOWER(vin_ml_carline_code)) = ? AND TRIM(LOWER(vin_ml_wmi_code))=? "
//							+ " AND TRIM(LOWER(vin_ml_vds_code))=? AND TRIM(LOWER(vin_ml_vis_start))=? AND TRIM(LOWER(vin_ml_vis_end))=? ";
					sql="DELETE FROM gms3_mdm_vin_ml_mapping WHERE TRIM(LOWER(vin_ml_carline_code)) = ? AND TRIM(LOWER(vin_ml_wmi_code))=? "
							+ " AND TRIM(LOWER(vin_ml_vds_code))=? AND TRIM(LOWER(vin_ml_vis_start))=? AND TRIM(LOWER(vin_ml_vis_end))=? ";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, vinDetails.getCarlineCode().trim().toLowerCase());
					pstmt.setString(2, vinDetails.getWmiCode().trim().toLowerCase());
					pstmt.setString(3, vinDetails.getVdsCode().trim().toLowerCase());
					pstmt.setString(4, vinDetails.getVisStartRange().trim().toLowerCase());
					pstmt.setString(5, vinDetails.getVisEndRange().trim().toLowerCase());
					pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					sql = null;
				}

				// commit the transaction
				conn.commit();
			}
			else
			{
				logger.info("deleteVinDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteVinDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "deleteVinDetails()", e);
			logger.info("deleteVinDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed parameter to null
			deleteIdsList=null;
		}
		logger.info("deleteVinDetails :: Method Ends.");
		return true;
	}

	public static boolean activeVinDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeVinDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_vin_detail SET mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_vin_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_vin_id = "+ dataMap.get("ID").toString();
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
				logger.info("activeVinDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeVinDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "activeVinDetails()", e);
			logger.info("activeVinDetails :: ################ Exception ################");
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
		logger.info("activeVinDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importVINDetails(VinDetails vinDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importVINDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=vinDetails.getWmiCode())
			{
				vinDetails.setWmiCode(vinDetails.getWmiCode().trim());
			}
			if(null!=vinDetails.getVdsCode())
			{
				vinDetails.setVdsCode(vinDetails.getVdsCode().trim());
			}
			if(null!=vinDetails.getVisStartRange())
			{
				vinDetails.setVisStartRange(vinDetails.getVisStartRange().trim());
			}
			if(null!=vinDetails.getVisEndRange())
			{
				vinDetails.setVisEndRange(vinDetails.getVisEndRange().trim());
			}
			if(null!=vinDetails.getEngineCode())
			{
				vinDetails.setEngineCode(vinDetails.getEngineCode().trim());
			}
			if(null!=vinDetails.getMissionCode())
			{
				vinDetails.setMissionCode(vinDetails.getMissionCode().trim());
			}
			if(null!=vinDetails.getCountryLocaleCode())
			{
				vinDetails.setCountryLocaleCode(vinDetails.getCountryLocaleCode().trim());
			}
			if(null!=vinDetails.getManualLanguageCode())
			{
				vinDetails.setManualLanguageCode(vinDetails.getManualLanguageCode().trim());
			}
			if(null!=vinDetails.getCarlineCode())
			{
				vinDetails.setCarlineCode(vinDetails.getCarlineCode().trim());
			}
			if(null!=vinDetails.getCarlineNameEng())
			{
				vinDetails.setCarlineNameEng(vinDetails.getCarlineNameEng().trim());
			}
			if(null!=vinDetails.getCarlineNameReg())
			{
				vinDetails.setCarlineNameReg(vinDetails.getCarlineNameReg().trim());
			}
			if(null!=vinDetails.getIcDisplayCarlineCode())
			{
				vinDetails.setIcDisplayCarlineCode(vinDetails.getIcDisplayCarlineCode().trim());
			}
			if(null!=vinDetails.getGroupCode())
			{
				vinDetails.setGroupCode(vinDetails.getGroupCode().trim());
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
			 * CHECK ON CARLINE ENG NAME + CARLINE CODE 
			 */
			String getVinSql = "SELECT * FROM gms3_mdm_vin_detail WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND TRIM(LOWER(mdm_crln_name_eng_lang)) =? AND "
					+ " TRIM(LOWER(mdm_vin_wmi_code))=? AND TRIM(LOWER(mdm_vin_vds_code))=? AND TRIM(LOWER(mdm_vin_vis_start_range))=? "
					+ " AND "
					+ " TRIM(LOWER(mdm_vin_vis_end_range))=? AND TRIM(LOWER(mdm_vin_group_code))=? AND TRIM(LOWER(mdm_crln_code))=? "
					+ " AND mdm_vin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importVINDetails :: getVinSql :: > " + getVinSql);
			pstmt = conn.prepareStatement(getVinSql);
			pstmt.setLong(1, vinDetails.getCountryLocaleId());
			pstmt.setLong(2, vinDetails.getManualLanguageId());
			pstmt.setString(3, vinDetails.getCarlineNameEng().trim().toLowerCase());
			pstmt.setString(4, vinDetails.getWmiCode().trim().toLowerCase());
			pstmt.setString(5, vinDetails.getVdsCode().trim().toLowerCase());
			pstmt.setString(6, vinDetails.getVisStartRange().trim().toLowerCase());
			pstmt.setString(7, vinDetails.getVisEndRange().trim().toLowerCase());
			pstmt.setString(8, vinDetails.getGroupCode().trim().toLowerCase());
			pstmt.setString(9, vinDetails.getCarlineCode().trim().toLowerCase());
			rs = pstmt.executeQuery();
			long autoVinId=0;
			if(rs.next())
			{
				autoVinId= rs.getLong("mdm_vin_id");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			getVinSql = null;
			
			if(autoVinId>0)
			{
				pstmt = null;
				logger.info("importVINDetails :: VIN Already Exists. Update Row for Auto VIN id : >" + autoVinId);
				
				
				/*
				 * checks for sync status - 
				 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
					If oldStatus is Active - 
						Here, No check for data (because fetched using wmi / vdsCode / visStart / visEnd), 
						 	so skip this step then update syncStatus as N
							else let it remains as it is
							
					For VIN No sync update status, let it remains as it is		
				 */
				
				
				/*
				 * DO NOT UPDATE FLAG HERE - 23 JUNE 2018
				 */
				
				String sql="UPDATE gms3_mdm_vin_detail SET mdm_vin_wmi_code =?,mdm_vin_vds_code =?,"
						+ "mdm_vin_vis_start_range=?,mdm_vin_engine_code=?,mdm_vin_transmission_code=?,"
						+ " mdm_vin_updated_tmstp=?, mdm_vin_group_code=?,mdm_vin_vis_end_range=?, "
						+ " mdm_cl_locale_code =? , mdm_ml_lang_code =?,mdm_crln_name_eng_lang =?, "
						+ " mdm_crln_name_regional_lang=?,mdm_crln_code=?,mdm_ic_display_carline_code=? WHERE mdm_vin_id =?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, vinDetails.getWmiCode());
				pstmt.setString(2, vinDetails.getVdsCode());
				pstmt.setString(3, vinDetails.getVisStartRange());
				pstmt.setString(4, vinDetails.getEngineCode());
				pstmt.setString(5, vinDetails.getMissionCode());
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(7, vinDetails.getGroupCode());
				pstmt.setString(8, vinDetails.getVisEndRange().toUpperCase());
				pstmt.setString(9, vinDetails.getCountryLocaleCode());
				pstmt.setString(10, vinDetails.getManualLanguageCode());
				pstmt.setString(11, vinDetails.getCarlineNameEng());
				pstmt.setString(12, vinDetails.getCarlineNameReg());
				pstmt.setString(13, vinDetails.getCarlineCode());
				pstmt.setString(14, vinDetails.getIcDisplayCarlineCode());
				pstmt.setLong(15, autoVinId);
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt = null;
			}
			else
			{
				pstmt = null;
				logger.info("importVINDetails :: VIN Does not Exists. Insert New Row.");
				String sql="INSERT INTO gms3_mdm_vin_detail(mdm_cl_id,mdm_ml_id,mdm_vin_wmi_code, mdm_vin_vds_code, mdm_vin_vis_start_range,mdm_vin_vis_end_range"
						+ ",mdm_vin_engine_code, mdm_vin_transmission_code,mdm_vin_flag,mdm_vin_created_tmstp,mdm_vin_group_code"
						+ ", mdm_cl_locale_code, mdm_ml_lang_code,mdm_crln_name_eng_lang, "
						+ " mdm_crln_name_regional_lang,mdm_crln_code,mdm_ic_display_carline_code )"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setLong(1, vinDetails.getCountryLocaleId());
				pstmt.setLong(2, vinDetails.getManualLanguageId());
				pstmt.setString(3, vinDetails.getWmiCode());
				pstmt.setString(4, vinDetails.getVdsCode());
				pstmt.setString(5, vinDetails.getVisStartRange());
				pstmt.setString(6, vinDetails.getVisEndRange().toUpperCase());
				pstmt.setString(7, vinDetails.getEngineCode());
				pstmt.setString(8, vinDetails.getMissionCode());
				pstmt.setString(9, vinDetails.getFlag());
				pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(11, vinDetails.getGroupCode());
				pstmt.setString(12, vinDetails.getCountryLocaleCode());
				pstmt.setString(13, vinDetails.getManualLanguageCode());
				pstmt.setString(14, vinDetails.getCarlineNameEng());
				pstmt.setString(15, vinDetails.getCarlineNameReg());
				pstmt.setString(16, vinDetails.getCarlineCode());
				pstmt.setString(17, vinDetails.getIcDisplayCarlineCode());
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
			Utilities.printStackTraceToLogs(VinDAO.class.getName(), "importVINDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			vinDetails = null;
		}
		logger.info("importVINDetails :: Method Ends.");
		return true;
	}
	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination
	 * the create-or-update path above uses (the SELECT and its parameter binding are
	 * lifted from importVINDetails). The import Excel carries no primary key, so this
	 * is how an ACTION=D row is matched. Already deleted rows are excluded, so a
	 * second delete of the same row correctly reports 'nothing to delete'.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(VinDetails details, Connection conn)
			throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String getVinSql = "SELECT * FROM gms3_mdm_vin_detail WHERE mdm_cl_id = ? AND  mdm_ml_id=? AND TRIM(LOWER(mdm_crln_name_eng_lang)) =? AND "
					+ " TRIM(LOWER(mdm_vin_wmi_code))=? AND TRIM(LOWER(mdm_vin_vds_code))=? AND TRIM(LOWER(mdm_vin_vis_start_range))=? "
					+ " AND "
					+ " TRIM(LOWER(mdm_vin_vis_end_range))=? AND TRIM(LOWER(mdm_vin_group_code))=? AND TRIM(LOWER(mdm_crln_code))=? "
					+ " AND mdm_vin_flag  NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
//			logger.info("importVINDetails :: getVinSql :: > " + getVinSql);
			pstmt = conn.prepareStatement(getVinSql);
			pstmt.setLong(1, details.getCountryLocaleId());
			pstmt.setLong(2, details.getManualLanguageId());
			pstmt.setString(3, ImportActionUtils.safeLower(details.getCarlineNameEng()));
			pstmt.setString(4, ImportActionUtils.safeLower(details.getWmiCode()));
			pstmt.setString(5, ImportActionUtils.safeLower(details.getVdsCode()));
			pstmt.setString(6, ImportActionUtils.safeLower(details.getVisStartRange()));
			pstmt.setString(7, ImportActionUtils.safeLower(details.getVisEndRange()));
			pstmt.setString(8, ImportActionUtils.safeLower(details.getGroupCode()));
			pstmt.setString(9, ImportActionUtils.safeLower(details.getCarlineCode()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_vin_id");
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