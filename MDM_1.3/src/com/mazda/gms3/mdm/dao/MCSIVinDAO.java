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
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class MCSIVinDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(MCSIVinDAO.class);
	
	public static ArrayList<CarlineDetails> getModelsList(String countryLocaleId, String langauageId, String wmiCode, Connection conn, String closeConnection) throws SQLException
	{
		ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		PreparedStatement pstmt=  null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) && null!=wmiCode && !"".equals(wmiCode))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name_eng_lang FROM gms3_mdm_carline_codes WHERE "
						+ " mdm_cl_id="+countryLocaleId+" AND mdm_ml_id="+langauageId+" "
						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') AND TRIM(LOWER(mdm_crln_wmi_code))='"+wmiCode.trim().toLowerCase()+"' "
						+ " GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails si = new CarlineDetails();
					if(null!=rs.getString("mdm_crln_code"))
					{
						si.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						si.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=si.getCarlineCode() && !"".equals(si.getCarlineCode()) && null!=si.getCarlineNameEng() && !"".equals(si.getCarlineNameEng()))
					{
						si.setCarlineIdForCombo(si.getCarlineNameEng().trim().toUpperCase()+"_"+si.getCarlineCode().trim().toUpperCase());
					}
					modelsList.add(si);
					si = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
				
				if(null!=modelsList && modelsList.size()>0)
				{
					// GET REGIONAL NAME 
					for(CarlineDetails si : modelsList)
					{
						sql="SELECT mdm_crln_name_regional_lang FROM gms3_mdm_carline_codes WHERE "
								+ " mdm_cl_id="+countryLocaleId+" AND mdm_ml_id="+langauageId+" "
								+ " AND TRIM(LOWER(mdm_crln_code))=? AND TRIM(LOWER(mdm_crln_name_eng_lang))=? AND TRIM(LOWER(mdm_crln_wmi_code))=? "
								+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
								+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, si.getCarlineCode().trim().toLowerCase());
						pstmt.setString(2, si.getCarlineNameEng().trim().toLowerCase());
						pstmt.setString(3, wmiCode.trim().toLowerCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("mdm_crln_name_regional_lang"))
							{
								si.setCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
							}
						}
						sql = null;
						rs.close();rs=null;
						pstmt.close();pstmt=null;
					}
				}
				
				if(null==closeConnection)
				{
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getModelsList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
		return modelsList;
	}
	
	public static ArrayList<SelectItemDetails> getWMIList(String countryLocaleId, String langauageId) throws SQLException
	{
		ArrayList<SelectItemDetails> wmiList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId))
			{
				conn = getConnection();
				String sql = "SELECT DISTINCT mdm_crln_wmi_code FROM gms3_mdm_carline_codes  WHERE "
						+ " mdm_cl_id="+countryLocaleId+" AND mdm_ml_id="+langauageId+"  "
								+ "  AND "
						+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') ORDER BY mdm_crln_wmi_code ASC";
				logger.info("getWMIList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(rs.getString("mdm_crln_wmi_code").trim());
					si.setValue(rs.getString("mdm_crln_wmi_code").trim());
					wmiList.add(si);
					si=  null;
				}
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getWMIList()", e);
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
		return wmiList;
	}
 
	public static ArrayList<SelectItemDetails> getVDSList(String countryLocaleId, String langauageId, String wmiCode, String selCarlineCode, 
			String carlineEngName, Connection conn , String closeConnection) throws SQLException
	{
		ArrayList<SelectItemDetails> vdsList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) && null!=selCarlineCode && !"".equals(selCarlineCode) 
					&& null!=carlineEngName && !"".equals(carlineEngName))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}

				String sql="SELECT DISTINCT mdm_vin_vds_code FROM gms3_mdm_vin_detail  "
						+ " WHERE mdm_cl_id="+countryLocaleId+" "
						+ " AND mdm_ml_id="+langauageId+" AND mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') AND  TRIM(LOWER(mdm_vin_wmi_code))=? "
							+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))=? "
						+ "  AND TRIM(LOWER(mdm_crln_code)) =? ";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, wmiCode.trim().toLowerCase());
				stmt.setString(2, carlineEngName.trim().toLowerCase());
				stmt.setString(3, selCarlineCode.trim().toLowerCase());
				rs = stmt.executeQuery();
				while(rs.next())
				{
					String vdsCode = rs.getString("mdm_vin_vds_code").trim();
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(vdsCode);
					si.setValue(vdsCode);
					vdsList.add(si);
					vdsCode=  null;
				}
				sql = null;
				
				if(null==vdsList || vdsList.size()<=0)
				{
					logger.info("getVDSList :: No VDS Codes Found on the Basis of Selected Parameters  :: > " +countryLocaleId+ " > " + langauageId+ " > " + wmiCode+" > "+ carlineEngName+" "+selCarlineCode);
				}
				
				if(null==closeConnection)
				{
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getVDSList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return vdsList;
	}
	
	public static ArrayList<SelectItemDetails> getVISStartRangeList(String countryLocaleId, String langauageId, String wmiCode, String selCarlineCode, 
			String carlineEngName, String vdsCode, Connection conn, String closeConnection) throws SQLException
	{
		ArrayList<SelectItemDetails> visStartRangeList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) &&  null!=selCarlineCode && !"".equals(selCarlineCode) 
					&& null!=carlineEngName && !"".equals(carlineEngName)
					&& null!=vdsCode && !"".equals(vdsCode))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}

				String sql="SELECT DISTINCT mdm_vin_vis_start_range FROM gms3_mdm_vin_detail  "
						+ " WHERE mdm_cl_id="+countryLocaleId+" "
						+ " AND mdm_ml_id="+langauageId+" AND mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') AND  TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))= ? AND TRIM(LOWER(mdm_crln_code)) =?"
						+ " AND TRIM(LOWER(mdm_vin_vds_code)) =?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, wmiCode.trim().toLowerCase());
				stmt.setString(2, carlineEngName.trim().toLowerCase());
				stmt.setString(3, selCarlineCode.trim().toLowerCase());
				stmt.setString(4, vdsCode.trim().toLowerCase());
				rs = stmt.executeQuery();
				while(rs.next())
				{
					String visStartRange = rs.getString("mdm_vin_vis_start_range").trim();
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(visStartRange);
					si.setValue(visStartRange);
					visStartRangeList.add(si);
					visStartRange=  null;
				}
				sql = null;
				
				if(null==visStartRangeList || visStartRangeList.size()<=0)
				{
					logger.info("getVISStartRangeList :: No VIS Start Range Found on the Basis of Selected Parameters  :: > " +countryLocaleId+ " > " + langauageId+ " > " + wmiCode +" > "+ carlineEngName+" "+selCarlineCode+" > "+vdsCode);
				}
				if(null==closeConnection)
				{
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getVISStartRangeList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
//		logger.info("getWMIList :: Method Ends.");
		return visStartRangeList;
	}
	
	public static ArrayList<SelectItemDetails> getVISEndRangeList(String countryLocaleId, String langauageId, String wmiCode, String selCarlineCode,
			String carlineNameEng, String vdsCode, String visStartRange, Connection conn, String closeConnection) throws SQLException
	{
		ArrayList<SelectItemDetails> visEndRangeList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) && null!=selCarlineCode && !"".equals(selCarlineCode) 
					&& null!=carlineNameEng && !"".equals(carlineNameEng) 
					&& null!=vdsCode && !"".equals(vdsCode) && null!=visStartRange && !"".equals(visStartRange))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}

				String sql="SELECT DISTINCT mdm_vin_vis_end_range FROM gms3_mdm_vin_detail  "
						+ " WHERE mdm_cl_id="+countryLocaleId+" "
						+ " AND mdm_ml_id="+langauageId+" AND mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') AND  TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))= ? AND TRIM(LOWER(mdm_crln_code)) =? "
						+ " AND TRIM(LOWER(mdm_vin_vds_code)) =? "
						+ " AND TRIM(LOWER(mdm_vin_vis_start_range)) =? ";
				
				
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, wmiCode.trim().toLowerCase());
				stmt.setString(2, carlineNameEng.trim().toLowerCase());
				stmt.setString(3, selCarlineCode.trim().toLowerCase());
				stmt.setString(4, vdsCode.trim().toLowerCase());
				stmt.setString(5, visStartRange.trim().toLowerCase());
				rs = stmt.executeQuery();
				while(rs.next())
				{
					String visEndRange = rs.getString("mdm_vin_vis_end_range").trim();
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(visEndRange);
					si.setValue(visEndRange);
					visEndRangeList.add(si);
					visEndRange=  null;
				}
				sql = null;
				
				if(null==visEndRangeList || visEndRangeList.size()<=0)
				{
					logger.info("getVISEndRangeList :: No VIS End Range Found on the Basis of Selected Parameters  :: > " +countryLocaleId+ " > " + langauageId+ " > " +  wmiCode +" > "+ carlineNameEng+" "+ selCarlineCode+" > "+vdsCode +" > "+ visStartRange );
				}
				
				if(null==closeConnection)
				{
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getVISEndRangeList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
//		logger.info("getWMIList :: Method Ends.");
		return visEndRangeList;
	}
	
	
	
	public static boolean addVINDetails(SIVinDetails documentDetails, Connection conn, String closeConnection)  
	{
		Statement stmt= null;
		ResultSet rs = null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=documentDetails && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()) 
				&& null!=documentDetails.getLocale() && !"".equals(documentDetails.getLocale()) 
				&& null!=documentDetails.getCountryLocaleId() && !"".equals(documentDetails.getCountryLocaleId()) 
				&& null!=documentDetails.getManualLanguageId() && !"".equals(documentDetails.getManualLanguageId()))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
				}
				conn.setAutoCommit(false);
				
				String status="";
				if(documentDetails.isDocumentPublished()==true)
				{
					// PUBLISH
					status = ApplicationProperties.getProperty("flag.value.publish");
				}
				else
				{
					// DRAFT
					status = ApplicationProperties.getProperty("flag.value.draft");
				}
				
				/*
				 * CHECK WHETHER THE DOCUMENT EXISTS IN PARENT TABLE OR NOT
				 */
				long autoDocId=0;
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_mc_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
					+ "AND mdm_cl_id="+documentDetails.getCountryLocaleId()+" AND mdm_ml_id="+documentDetails.getManualLanguageId();
//				logger.info("addVINDetails :: checkSql :: > " + checkSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(checkSql);
				if(rs.next())
				{
					autoDocId = rs.getLong("mdm_sivin_code_id");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				checkSql =null;
				
				if(autoDocId==0)
				{
					/*
					 * INSERT INTO GMS3_MDM_MC_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_mc_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
					stmt= conn.createStatement();
					String generatedKeys[] = { "mdm_sivin_code_id"};
					stmt.executeUpdate(insertParentSql, generatedKeys);
					rs = stmt.getGeneratedKeys();
					if(rs.next())
					{
						autoDocId = rs.getLong(1);
					}
					insertParentSql= null;
					stmt.close();stmt = null;
					rs.close();rs =null;
					generatedKeys = null;
					
					if(autoDocId>0)
					{
						/*
						 * update the rest of the details
						 */
						String updateSql="UPDATE gms3_mdm_mc_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
								+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
								+ " WHERE  mdm_sivin_code_id=?";
						pstmt = conn.prepareStatement(updateSql);
						pstmt.setString(1, documentDetails.getLocale());
						pstmt.setString(2, documentDetails.getFetchedVersion());
						pstmt.setString(3, documentDetails.getUpdatedVersion());
						pstmt.setString(4, status);
						pstmt.setString(5, documentDetails.getWslId());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setLong(7, autoDocId);
						pstmt.executeUpdate();
						pstmt.close();pstmt = null;
						updateSql= null;
						
						/*
						 * Insert New Records in Add Table
						 */
						if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
						{
							for(int i=0;i<documentDetails.getItemsList().size();i++)
							{
								SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
								String insertItemSql="INSERT INTO gms3_mdm_mc_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getYear());
								pstmt.setString(6, itemDetails.getVdsCode());
								pstmt.setString(7, itemDetails.getVinStartRange());
								pstmt.setString(8, itemDetails.getVinEndRange());
								pstmt.setString(9, itemDetails.getFirstLevelRefKey());
								pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(11, itemDetails.getThirdLevelRefKey());
								pstmt.setString(12, documentDetails.getWslId());
								pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(14, itemDetails.getModelRegionalName());
								pstmt.setString(15, itemDetails.getFourthLevelRefKey());
								pstmt.setString(16, itemDetails.getFifthLevelRefKey());
								pstmt.executeUpdate();
								pstmt.close();pstmt= null;
								insertItemSql=null;		
							
								itemDetails=null;
							}
						}
						// COMMIT THE TRANSACTION
						conn.commit();
					}
				}
				else
				{

					/*
					 * update the rest of the details
					 */
					String updateSql="UPDATE gms3_mdm_mc_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
							+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
							+ " WHERE  mdm_sivin_code_id=?";
					pstmt = conn.prepareStatement(updateSql);
					pstmt.setString(1, documentDetails.getLocale());
					pstmt.setString(2, documentDetails.getFetchedVersion());
					pstmt.setString(3, documentDetails.getUpdatedVersion());
					pstmt.setString(4, status);
					pstmt.setString(5, documentDetails.getWslId());
					pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(7, autoDocId);
					pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					updateSql= null;
					
					/*
					 * Insert New Records in Add Table
					 */
					if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
					{
						for(int i=0;i<documentDetails.getItemsList().size();i++)
						{
							SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
							String insertItemSql="INSERT INTO gms3_mdm_mc_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getYear());
							pstmt.setString(6, itemDetails.getVdsCode());
							pstmt.setString(7, itemDetails.getVinStartRange());
							pstmt.setString(8, itemDetails.getVinEndRange());
							pstmt.setString(9, itemDetails.getFirstLevelRefKey());
							pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(11, itemDetails.getThirdLevelRefKey());
							pstmt.setString(12, documentDetails.getWslId());
							pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(14, itemDetails.getModelRegionalName());
							pstmt.setString(15, itemDetails.getFourthLevelRefKey());
							pstmt.setString(16, itemDetails.getFifthLevelRefKey());
							pstmt.executeUpdate();
							pstmt.close();pstmt= null;
							insertItemSql=null;		
						
							itemDetails=null;
						}
					}
					// COMMIT THE TRANSACTION
					conn.commit();
				}
			}
			else
			{
				logger.info("addVINDetails :: Either Document Details or Mandatory Parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "addVINDetails()", e1);
			}
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "addVINDetails()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
				{
					pstmt.close();
				}
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=closeConnection && closeConnection.equals("Y"))
				{
					// if request from Servlet - close connection
					if(null!=conn)
						conn.close();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "addVINDetails()", e1);
			}
		}
		return true;
	}

	public static boolean deleteVINDetails(SIVinDetails documentDetails) throws SQLException
	{
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=documentDetails && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()) 
				&& null!=documentDetails.getLocale() && !"".equals(documentDetails.getLocale()) 
				&& null!=documentDetails.getCountryLocaleId() && !"".equals(documentDetails.getCountryLocaleId()) 
				&& null!=documentDetails.getManualLanguageId() && !"".equals(documentDetails.getManualLanguageId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				
				String status="";
				if(documentDetails.isDocumentPublished()==true)
				{
					// PUBLISH
					status = ApplicationProperties.getProperty("flag.value.publish");
				}
				else
				{
					// DRAFT
					status = ApplicationProperties.getProperty("flag.value.draft");
				}
				
				/*
				 * CHECK WHETHER THE DOCUMENT EXISTS IN PARENT TABLE OR NOT
				 */
				long autoDocId=0;
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_mc_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
					+ "AND mdm_cl_id="+documentDetails.getCountryLocaleId()+" AND mdm_ml_id="+documentDetails.getManualLanguageId();
//				logger.info("deleteVINDetails :: checkSql :: > " + checkSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(checkSql);
				if(rs.next())
				{
					autoDocId = rs.getLong("mdm_sivin_code_id");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				checkSql =null;
				
				if(autoDocId==0)
				{
					/*
					 * INSERT INTO GMS3_MDM_MC_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_mc_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
					stmt= conn.createStatement();
					String generatedKeys[] = { "mdm_sivin_code_id"};
					stmt.executeUpdate(insertParentSql, generatedKeys);
					rs = stmt.getGeneratedKeys();
					if(rs.next())
					{
						autoDocId = rs.getLong(1);
					}
					insertParentSql= null;
					stmt.close();stmt = null;
					rs.close();rs =null;
					generatedKeys = null;
					
					if(autoDocId>0)
					{
						/*
						 * update the rest of the details
						 */
						String updateSql="UPDATE gms3_mdm_mc_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
								+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
								+ " WHERE  mdm_sivin_code_id=?";
						pstmt = conn.prepareStatement(updateSql);
						pstmt.setString(1, documentDetails.getLocale());
						pstmt.setString(2, documentDetails.getFetchedVersion());
						pstmt.setString(3, documentDetails.getUpdatedVersion());
						pstmt.setString(4, status);
						pstmt.setString(5, documentDetails.getWslId());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setLong(7, autoDocId);
						pstmt.executeUpdate();
						pstmt.close();pstmt = null;
						updateSql= null;
						
						/*
						 * Insert New Records in Delete Table
						 */
						if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
						{
							for(int i=0;i<documentDetails.getItemsList().size();i++)
							{
								SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
								String insertItemSql="INSERT INTO gms3_mdm_mc_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getYear());
								pstmt.setString(6, itemDetails.getVdsCode());
								pstmt.setString(7, itemDetails.getVinStartRange());
								pstmt.setString(8, itemDetails.getVinEndRange());
								pstmt.setString(9, itemDetails.getFirstLevelRefKey());
								pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(11, itemDetails.getThirdLevelRefKey());
								pstmt.setString(12, documentDetails.getWslId());
								pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(14, itemDetails.getModelRegionalName());
								pstmt.setString(15, itemDetails.getFourthLevelRefKey());
								pstmt.setString(16, itemDetails.getFifthLevelRefKey());
								pstmt.execute();
								pstmt.close();pstmt= null;
								insertItemSql=null;		
							
								itemDetails=null;
							}
						}
						// COMMIT THE TRANSACTION
						conn.commit();
					}
				}
				else
				{

					/*
					 * update the rest of the details
					 */
					String updateSql="UPDATE gms3_mdm_mc_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
							+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
							+ " WHERE  mdm_sivin_code_id=?";
					pstmt = conn.prepareStatement(updateSql);
					pstmt.setString(1, documentDetails.getLocale());
					pstmt.setString(2, documentDetails.getFetchedVersion());
					pstmt.setString(3, documentDetails.getUpdatedVersion());
					pstmt.setString(4, status);
					pstmt.setString(5, documentDetails.getWslId());
					pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(7, autoDocId);
					pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					updateSql= null;
					
					/*
					 * Insert New Records in Add Table
					 */
					if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
					{
						for(int i=0;i<documentDetails.getItemsList().size();i++)
						{
							SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
							String insertItemSql="INSERT INTO gms3_mdm_mc_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp, mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getYear());
							pstmt.setString(6, itemDetails.getVdsCode());
							pstmt.setString(7, itemDetails.getVinStartRange());
							pstmt.setString(8, itemDetails.getVinEndRange());
							pstmt.setString(9, itemDetails.getFirstLevelRefKey());
							pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(11, itemDetails.getThirdLevelRefKey());
							pstmt.setString(12, documentDetails.getWslId());
							pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(14, itemDetails.getModelRegionalName());
							pstmt.setString(15, itemDetails.getFourthLevelRefKey());
							pstmt.setString(16, itemDetails.getFifthLevelRefKey());
							pstmt.executeUpdate();
							pstmt.close();pstmt= null;
							insertItemSql=null;		
						
							itemDetails=null;
						}
					}
					// COMMIT THE TRANSACTION
					conn.commit();
				}
			}
			else
			{
				logger.info("deleteVINDetails :: Either Document Details or Mandatory Parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			if(null!=conn)
			{
				conn.rollback();
			}
			logger.info("deleteVINDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "deleteVINDetails()", e);
			logger.info("deleteVINDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!=pstmt)
			{
				pstmt.close();
			}
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
		return true;
	}

	public static CarlineDetails getModel(String countryLocaleId, String langauageId, String carlineCode, String wmiCode, Connection conn, String closeConnection) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		CarlineDetails carlineDetails = new CarlineDetails();
		Statement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) && 
					null!=carlineCode && !"".equals(carlineCode))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				String sql = "SELECT mdm_crln_name_eng_lang , mdm_crln_name_regional_lang, mdm_crln_model_type FROM gms3_mdm_carline_codes WHERE "
						+ " mdm_cl_id="+countryLocaleId+" AND mdm_ml_id="+langauageId+" AND "
						+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ "  AND  TRIM(LOWER(mdm_crln_wmi_code))='"+wmiCode.trim().toLowerCase()+"' AND TRIM(LOWER(mdm_crln_code))='"+carlineCode.trim().toLowerCase()+"' ";
//				logger.info("getModel :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						carlineDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang"))
					{
						carlineDetails.setCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_model_type"))
					{
						carlineDetails.setModelType(rs.getString("mdm_crln_model_type").trim());
					}
				}
				sql = null;
				
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getModel()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
//		logger.info("getWMIList :: Method Ends.");
		return carlineDetails;
	}
	
	public static String getModelType(String countryLocaleId, String langauageId, String carlineCode, String carlineEngName, String wmiCode) throws SQLException
	{
		String modelType="";
		PreparedStatement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId) && null!=langauageId && !"".equals(langauageId) 
					&& null!=carlineCode && !"".equals(carlineCode) && null!=carlineEngName && !"".equals(carlineEngName))
			{
				conn = getConnection();
				String sql="SELECT mdm_crln_model_type FROM gms3_mdm_carline_codes WHERE mdm_cl_id="+countryLocaleId+" AND mdm_ml_id="+langauageId+" AND "
						+ " TRIM(LOWER(mdm_crln_code))=? AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ?  " ;
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					sql = sql+" AND TRIM(LOWER(mdm_crln_wmi_code)) = ? ";
				}
				sql = sql+" AND mdm_crln_flag = '"+ApplicationProperties.getProperty("flag.value.active")+"' ";
//				logger.info("getModelsList :: Sql :: > " + sql);
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, carlineCode.trim().toLowerCase());
				stmt.setString(2, carlineEngName.trim().toLowerCase());
				if(null!=wmiCode && !"".equals(wmiCode))
				{
					stmt.setString(3, wmiCode.trim().toLowerCase());
				}
				rs = stmt.executeQuery();
				if(rs.next())
				{
					if(null!=rs.getString("mdm_crln_model_type"))
					{
						modelType = rs.getString("mdm_crln_model_type").trim();
					}
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "getModelType()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return modelType;
	}
	
	public static SIVinDetails checkVININMDM(String languageId, SIVinDetails fieldDetails, Connection conn, String closeConnection) throws SQLException
	{
		PreparedStatement pstmt=null;
		ResultSet rs= null;
		try
		{
			if(null!=languageId && !"".equals(languageId) && null!=fieldDetails && null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) 
					&& null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) && null!=fieldDetails.getCarlineCode() 
					&& !"".equals(fieldDetails.getCarlineCode()))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				
				boolean vinFound = false;
				String sql="SELECT mdm_crln_code, mdm_crln_name_eng_lang, mdm_crln_name_regional_lang FROM gms3_mdm_vin_detail WHERE mdm_ml_id = "+languageId+" "
						+ " AND mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' AND TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_vin_vds_code))=? AND TRIM(LOWER(mdm_crln_code))=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, fieldDetails.getWmiCode().trim().toLowerCase());
				pstmt.setString(2, fieldDetails.getVdsCode().trim().toLowerCase());
				pstmt.setString(3, fieldDetails.getCarlineCode().trim().toLowerCase());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					vinFound=  true;
					if(null!=rs.getString("mdm_crln_code"))
					{
						fieldDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						fieldDetails.setModel(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang"))
					{
						fieldDetails.setModelRegionalName(rs.getString("mdm_crln_name_regional_lang").trim());
					}
					
				}
				rs.close();rs = null;
				pstmt.close();pstmt= null;
				sql = null;
				
				/*
				 * ADDED ON 23RD JUNE 2019, CUSTOM VINS ALSO ALLOWED WHICH DOES NOT EXIST IN MDM
				 * IN THAT CASE GET CARLINE DETAILS FROM MDM USING CARLINE CODE + WMI CODE
				 */
				if(vinFound==false)
				{
					sql="SELECT mdm_crln_code, mdm_crln_name_eng_lang, mdm_crln_name_regional_lang FROM gms3_mdm_carline_codes WHERE mdm_ml_id = "+languageId+" "
							+ " AND mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' AND TRIM(LOWER(mdm_crln_wmi_code))=? "
							+ " AND TRIM(LOWER(mdm_crln_code))=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, fieldDetails.getWmiCode().trim().toLowerCase());
					pstmt.setString(2, fieldDetails.getCarlineCode().trim().toLowerCase());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						vinFound = true;
						if(null!=rs.getString("mdm_crln_code"))
						{
							fieldDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
						}
						if(null!=rs.getString("mdm_crln_name_eng_lang"))
						{
							fieldDetails.setModel(rs.getString("mdm_crln_name_eng_lang").trim());
						}
						if(null!=rs.getString("mdm_crln_name_regional_lang"))
						{
							fieldDetails.setModelRegionalName(rs.getString("mdm_crln_name_regional_lang").trim());
						}
						
					}
					rs.close();rs = null;
					pstmt.close();pstmt= null;
					sql = null;
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
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(MCSIVinDAO.class.getName(), "checkVININMDM()", e);
		} 
		finally 
		{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return fieldDetails;
	}
}