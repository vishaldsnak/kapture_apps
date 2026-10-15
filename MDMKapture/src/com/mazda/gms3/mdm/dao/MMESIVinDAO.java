package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.MMESIVINVDSComparator;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class MMESIVinDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(MMESIVinDAO.class);
	
	public static ArrayList<SelectItemDetails> getWMIList() throws SQLException
	{
		ArrayList<SelectItemDetails> wmiList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			// HERE LOCALE WILL ALWYAS BE EN-UK
			String loc = ApplicationProperties.getProperty("en_uk");
			loc = loc.replace("_", "-");
			conn = getConnection();
			String sql = "SELECT DISTINCT mdm_crln_wmi_code FROM gms3_mdm_carline_codes  WHERE "
					+ " mdm_ml_lang_code='"+loc+"' AND "
					+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
					+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') ORDER BY mdm_crln_wmi_code ASC";
//			logger.info("getWMIList :: Sql :: > " + sql);
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
			loc = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getWMIList()", e);
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
 	
	public static ArrayList<CarlineDetails> getModelsList(String wmiCode) throws SQLException
	{
		ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=wmiCode && !"".equals(wmiCode))
			{
				// HERE LOCALE WILL ALWYAS BE EN-UK
				String loc = ApplicationProperties.getProperty("en_uk");
				loc = loc.replace("_", "-");
				conn = getConnection();
				String sql="SELECT DISTINCT mdm_crln_name_eng_lang , mdm_crln_code "
						+ " FROM gms3_mdm_carline_codes WHERE mdm_ml_lang_code='"+loc+"' "
						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ " AND TRIM(LOWER(mdm_crln_wmi_code)) ='"+wmiCode.trim().toLowerCase()+"' "
						+ " GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC ";
//				logger.info("getModelsList :: Sql :: > " + sql);
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
					si=  null;
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
								+ "  mdm_ml_lang_code='"+loc+"' "
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
				loc=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getModelsList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		return modelsList;
	}
	
	public static ArrayList<SelectItemDetails> getVDSList(String wmiCode, String carlineCode,String carlineNameEng, String[] engineTypeId, String[] missionTypeId) throws SQLException
	{
		ArrayList<SelectItemDetails> vdsList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=wmiCode && !"".equals(wmiCode) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=carlineNameEng && !"".equals(carlineNameEng) && 
					null!=engineTypeId && engineTypeId.length>0
					&& null!=missionTypeId && missionTypeId.length>0)
			{
				// HERE LOCALE WILL ALWYAS BE EN-UK
				String loc = ApplicationProperties.getProperty("en_uk");
				loc = loc.replace("_", "-");
				conn = getConnection();
				String sql=null;
				
				String vdsCode=null;
				String startRange=null;
				String endRange = null;
				for(int e=0;e<engineTypeId.length;e++)
				{
					if(null!=engineTypeId[e] && !"".equals(engineTypeId[e]) && 
							!engineTypeId[e].trim().toLowerCase().equals(ApplicationProperties.getProperty("check.all.label").trim().toLowerCase()))
					{	
						for(int m=0;m<missionTypeId.length;m++)
						{
							if(null!=missionTypeId[m] && !"".equals(missionTypeId[m]) && 
									!missionTypeId[m].trim().toLowerCase().equals(ApplicationProperties.getProperty("check.all.label").trim().toLowerCase()))
							{
								sql="SELECT DISTINCT mdm_vin_vds_code,mdm_vin_vis_start_range, mdm_vin_vis_end_range FROM gms3_mdm_vin_detail  "
										+ " WHERE mdm_ml_lang_code='"+loc+"' "
										+ " AND mdm_vin_flag ='"+ApplicationProperties.getProperty("flag.value.active")+"' "
										+ " AND  TRIM(LOWER(mdm_vin_wmi_code))='"+wmiCode.trim().toLowerCase()+"' "
										+ " AND TRIM(LOWER(mdm_crln_code))='"+carlineCode.trim().toLowerCase()+"' "
										+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))= '"+carlineNameEng.trim().toLowerCase()+"' " 
										+ " AND TRIM(LOWER(mdm_vin_engine_code)) LIKE '%"+engineTypeId[e].trim().toLowerCase()+"%' "
										+ " AND TRIM(LOWER(mdm_vin_transmission_code)) LIKE '%"+missionTypeId[m].trim().toLowerCase()+"%' "
										+ " GROUP BY mdm_vin_vds_code , mdm_vin_vis_start_range, mdm_vin_vis_end_range";
								stmt = conn.createStatement();
								rs = stmt.executeQuery(sql);
								/*
								 * 
								 */
								SelectItemDetails si = new SelectItemDetails();
								boolean addToList = true;
								SelectItemDetails exist = new SelectItemDetails();
								while(rs.next())
								{
									addToList=true;
									// PREPARE DATA
									si = new SelectItemDetails();
									vdsCode = rs.getString("mdm_vin_vds_code");
									startRange = rs.getString("mdm_vin_vis_start_range");
									endRange = rs.getString("mdm_vin_vis_end_range");
									si.setLabel(vdsCode.trim() +" ("+startRange.trim()+" - "+endRange.trim()+")");
									si.setValue(vdsCode.trim() +" ("+startRange.trim()+" - "+endRange.trim()+")");
									
									if(null!=vdsList && vdsList.size()>0)
									{
										exist = null;
										for(int r=0;r<vdsList.size();r++)
										{
											exist = (SelectItemDetails)vdsList.get(r);
											if(exist.getValue().trim().toLowerCase().equals(si.getValue().trim().toLowerCase()))
											{
												/// already added - avoid duplicacy
												addToList=false;
												break;
											}
											exist = null;
										}
										exist = null;
									}
									
									if(addToList==true)
									{
										vdsList.add(si);
									}
									si = null;
									startRange = null;
									endRange = null;
									vdsCode=  null;
								}
								si  =null;
								/*
								while(rs.next())
								{
									vdsCode = rs.getString("mdm_vin_vds_code");
									if(null!=vdsCode && !"".equals(vdsCode))
									{
										boolean addToList = true;
										if(null!=vdsList && vdsList.size()>0)
										{
											for(SelectItemDetails exist : vdsList)
											{
												if(null!=exist.getValue() && !"".equals(exist.getValue()))
												{
													if(exist.getValue().trim().toLowerCase().equals(vdsCode.trim().toLowerCase()))
													{
														// already added - do not add this VDS Again
														addToList = false;
														break;
													}
												}
												exist = null;
											}
										}

										if(addToList==true)
										{
											SelectItemDetails si = new SelectItemDetails();
											startRange = rs.getString("mdm_vin_vis_start_range");
											endRange = rs.getString("mdm_vin_vis_end_range");
											si.setLabel(vdsCode.trim() +" ("+startRange.trim()+" - "+endRange.trim()+")");
											si.setValue(vdsCode.trim());
											vdsList.add(si);
											si = null;
											startRange = null;
											endRange = null;
										}
									}
									vdsCode = null;
								}
								*/
								rs.close();rs=null;
								stmt.close();stmt=null;
								sql=null;
							}
						}
					}
				}
				sql = null;
				loc = null;
			}
			
			if(null!=vdsList && vdsList.size()>0)
			{
				// Compare and Sort
				MMESIVINVDSComparator vdsComparator = new MMESIVINVDSComparator();
				Collections.sort(vdsList,vdsComparator);
				vdsComparator = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getVDSList()", e);
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
		return vdsList;
	}
	
	public static ArrayList<SelectItemDetails> getEngineTypeList(String wmiCode, String carlineCode, String carlineNameEng) throws SQLException
	{
		ArrayList<SelectItemDetails> engineTypeList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=wmiCode && !"".equals(wmiCode) && null!=carlineCode && !"".equals(carlineCode) && null!=carlineNameEng && !"".equals(carlineNameEng))
			{
				// HERE LOCALE WILL ALWYAS BE EN-UK
				String loc = ApplicationProperties.getProperty("en_uk");
				loc = loc.replace("_", "-");
				conn = getConnection();

				String sql="SELECT DISTINCT mdm_vin_engine_code FROM gms3_mdm_vin_detail  "
						+ " WHERE mdm_ml_lang_code='"+loc+"' "
						+ " AND mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') AND  TRIM(LOWER(mdm_vin_wmi_code))='"+wmiCode.trim().toLowerCase()+"' "
						+ "  AND TRIM(LOWER(mdm_crln_code))='"+carlineCode.trim().toLowerCase()+"' "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))= '"+carlineNameEng.trim().toLowerCase()+"' ";
				
//				logger.info("getEngineTypeList :: Sql :: > " + sql);
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				ArrayList<String> list = new ArrayList<String>();
				while(rs.next())
				{
					if(null!=rs.getString("mdm_vin_engine_code"))
					{
						String code = rs.getString("mdm_vin_engine_code").trim();
						if(null!=code && !"".equals(code))
						{
							String[] etCodes = code.split(",");
							if(null!=etCodes && etCodes.length>0)
							{
								for(int a=0;a<etCodes.length;a++)
								{
									boolean addToList = true;
									if(null!=list && list.size()>0)
									{
										for(String exist : list)
										{
											if(exist.trim().toLowerCase().equals(etCodes[a].trim().toLowerCase()))
											{
												addToList = false;
												break;
											}
											exist = null;
										}
									}
									
									if(addToList==true)
									{
										list.add(etCodes[a].trim());
									}
								}
							}
							etCodes = null;
						}
						code = null;
					
					}
				}
				sql = null;
				
				if(null!=list && list.size()>0)
				{
					/*
					 * GET ENGINE TYPE NAME
					 */
					for(int i=0;i<list.size();i++)
					{
						String code = String.valueOf(list.get(i));
						sql = "SELECT A.mdm_et_type_name FROM gms3_mdm_engine_type A, gms3_mdm_manual_language B "
							+ " WHERE TRIM(LOWER(A.mdm_et_type_code))='"+code.trim().toLowerCase()+"' AND "
							+ " A.mdm_ml_id=B.mdm_ml_id AND "
							+ " B.mdm_ml_lang_code='"+loc+"'"
							+ " AND A.mdm_et_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"')";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							if(null!=rs.getString("mdm_et_type_name"))
							{
								SelectItemDetails si = new SelectItemDetails();
								si.setLabel(rs.getString("mdm_et_type_name").trim() + " ("+code.trim()+")");
								si.setValue(code.trim());
								engineTypeList.add(si);
								si = null;
							}
						}
						rs.close();rs = null;
						stmt.close();stmt = null;
						sql = null;
					}
				}
				list = null;
				loc=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getEngineTypeList()", e);
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
		return engineTypeList;
	}

	public static ArrayList<SelectItemDetails> getMissionTypeList(String wmiCode, String carlineCode,String carlineNameEng, String[] engineTypeId) throws SQLException
	{
		ArrayList<SelectItemDetails> missionTypeList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=wmiCode && !"".equals(wmiCode) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=carlineNameEng && !"".equals(carlineNameEng) && 
					null!=engineTypeId && engineTypeId.length>0)
			{
				conn = getConnection();
				// HERE LOCALE WILL ALWYAS BE EN-UK
				String loc = ApplicationProperties.getProperty("en_uk");
				loc = loc.replace("_", "-");
				String sql="";
				
				ArrayList<String> list = new ArrayList<String>();
				for(int t=0;t<engineTypeId.length;t++)
				{
					if(null!=engineTypeId[t] && !"".equals(engineTypeId[t]) && 
							!engineTypeId[t].trim().toLowerCase().equals(ApplicationProperties.getProperty("check.all.label").trim().toLowerCase()))
					{
					sql="SELECT DISTINCT mdm_vin_transmission_code FROM gms3_mdm_vin_detail  "
							+ " WHERE mdm_ml_lang_code='"+loc+"' "
							+ " AND mdm_vin_flag = '"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " AND  TRIM(LOWER(mdm_vin_wmi_code))='"+wmiCode.trim().toLowerCase()+"' "
							+ "  AND TRIM(LOWER(mdm_crln_code))='"+carlineCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))= '"+carlineNameEng.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(mdm_vin_engine_code)) LIKE '%"+engineTypeId[t].trim().toLowerCase()+"%'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						if(null!=rs.getString("mdm_vin_transmission_code"))
						{
							String code = rs.getString("mdm_vin_transmission_code").trim();
							if(null!=code && !"".equals(code))
							{
								String[] etCodes = code.split(",");
								if(null!=etCodes && etCodes.length>0)
								{
									for(int a=0;a<etCodes.length;a++)
									{
										boolean addToList = true;
										if(null!=list && list.size()>0)
										{
											for(String exist : list)
											{
												if(exist.trim().toLowerCase().equals(etCodes[a].trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
												exist = null;
											}
										}
										
										if(addToList==true)
										{
											list.add(etCodes[a].trim());
										}
									}
								}
								etCodes = null;
							}
							code = null;
						}
					}
					sql = null;
					rs.close();rs=null;
					stmt.close();stmt=null;
				}
				}
				sql  = null;
				
				
//				logger.info("getMissionTypeList :: Sql :: > " + sql);
				
				
				
				if(null!=list && list.size()>0)
				{
					/*
					 * GET TRANS TYPE NAME
					 */
					for(int i=0;i<list.size();i++)
					{
						String code = String.valueOf(list.get(i));
						sql = "SELECT A.mdm_trans_type_type_name FROM gms3_mdm_trans_type A, gms3_mdm_manual_language B "
							+ " WHERE TRIM(LOWER(A.mdm_trans_type_type_code))='"+code.trim().toLowerCase()+"' AND A.mdm_ml_id=B.mdm_ml_id AND "
							+ " B.mdm_ml_lang_code='"+loc+"'  AND A.mdm_trans_type_flag='"+ApplicationProperties.getProperty("flag.value.active")+"'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							if(null!=rs.getString("mdm_trans_type_type_name"))
							{
								SelectItemDetails si = new SelectItemDetails();
								si.setLabel(rs.getString("mdm_trans_type_type_name").trim()+" ("+code.trim()+")");
								si.setValue(code.trim());
								missionTypeList.add(si);
								si = null;
							}
						}
						rs.close();rs = null;
						stmt.close();stmt = null;
						sql = null;
					}
				}
				list = null;
				loc = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getMissionTypeList()", e);
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
		return missionTypeList;
	}

	public static CarlineDetails getModel(String carlineCode, String wmiCode, Connection conn, String closeConnection) throws SQLException
	{
		CarlineDetails carlineDetails = new CarlineDetails();
		Statement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=carlineCode && !"".equals(carlineCode) && null!=wmiCode && !"".equals(wmiCode))
			{
				// HERE LOCALE WILL ALWAYS BE EN_UK
				String loc = ApplicationProperties.getProperty("en_uk");
				loc = loc.replace("_", "-");
				
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				
				String sql = "SELECT mdm_crln_name_eng_lang , mdm_crln_name_regional_lang, mdm_crln_model_type FROM gms3_mdm_carline_codes WHERE "
						+ " mdm_ml_lang_code='"+loc+"' AND TRIM(LOWER(mdm_crln_wmi_code))='"+wmiCode.trim().toLowerCase()+"' AND "
						+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ " AND TRIM(LOWER(mdm_crln_code))='"+carlineCode.trim().toLowerCase()+"'";
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
				loc = null;
				
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
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getModel()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return carlineDetails;
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
				
				String enUKLocaleForNewTable = ApplicationProperties.getProperty("en_uk");
				enUKLocaleForNewTable = enUKLocaleForNewTable.replace("_", "-");
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
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_mme_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
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
					 * INSERT INTO OK_DC.GMS3_MDM_MME_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_mme_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
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
						String updateSql="UPDATE gms3_mdm_mme_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
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
								String insertItemSql="INSERT INTO gms3_mdm_mme_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getVdsCode());
								pstmt.setString(6, itemDetails.getVinStartRange());
								pstmt.setString(7, itemDetails.getVinEndRange());
								pstmt.setString(8, itemDetails.getFirstLevelRefKey());
								pstmt.setString(9, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(10, itemDetails.getThirdLevelRefKey());
								pstmt.setString(11, documentDetails.getWslId());
								pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(13, itemDetails.getModelRegionalName());
								pstmt.setString(14, itemDetails.getFourthLevelRefKey());
								pstmt.setString(15, itemDetails.getFifthLevelRefKey());
								pstmt.executeUpdate();
								pstmt.close();pstmt= null;
								insertItemSql=null;		
							
								
								/*
								 * check if CUSTOM VDS IS TRUE = INSERT DATA IN NEW TABLE GMS3_MDM_MME_SIVIN_CUSTOM
								 * 23RD JUNE 2019
								 */
								if(itemDetails.isCustomVDS()==true)
								{
									try
									{
										if(null!=rs)
										{
											rs.close();
										}
										pstmt = null;
										rs = null;
										long newTableAutoId=0;
										insertItemSql = "SELECT mdm_custom_vds_id FROM gms3_mdm_mme_sivin_custom WHERE TRIM(LOWER(mdm_locale))=? AND "
												+ "TRIM(LOWER(mdm_crln_code))=?  AND TRIM(LOWER(mdm_crln_name_eng))=? AND TRIM(LOWER(mdm_vds_code)) = ? AND "
												+ "TRIM(LOWER(mdm_vis_end_range)) = ? AND TRIM(LOWER(mdm_vis_start_range)) = ? AND TRIM(LOWER(mdm_wmi_code)) = ?";
										pstmt = conn.prepareStatement(insertItemSql);
										pstmt.setString(1, enUKLocaleForNewTable.trim().toLowerCase());
										pstmt.setString(2, itemDetails.getCarlineCode().trim().toLowerCase());
										pstmt.setString(3, itemDetails.getModel().trim().toLowerCase());
										pstmt.setString(4, itemDetails.getVdsCode().trim().toLowerCase());
										pstmt.setString(5, itemDetails.getVinEndRange().trim().toLowerCase());
										pstmt.setString(6, itemDetails.getVinStartRange().trim().toLowerCase());
										pstmt.setString(7, itemDetails.getWmiCode().trim().toLowerCase());
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											newTableAutoId = rs.getLong("mdm_custom_vds_id");
										}
										rs.close();rs= null;
										pstmt.close();pstmt = null;
										insertItemSql = null;
										if(newTableAutoId > 0)
										{
											// UPDTAE EXISTING ROW TIMESTAMP
											insertItemSql = "UPDATE gms3_mdm_mme_sivin_custom SET mdm_custom_updated_tmstp = ? WHERE mdm_custom_vds_id = ?";
											pstmt = conn.prepareStatement(insertItemSql);
											pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
											pstmt.setLong(2, newTableAutoId);
											pstmt.executeUpdate();
											pstmt.close();pstmt = null;
											insertItemSql = null;
										}
										else
										{
											// INSERT NEW ROW
											insertItemSql = "INSERT INTO gms3_mdm_mme_sivin_custom (mdm_locale,mdm_crln_code,mdm_crln_name_eng,mdm_wmi_code,"
													+ "mdm_vds_code,mdm_vis_start_range,mdm_vis_end_range,mdm_custom_created_tmstp) VALUES(?,?,?,?,?,?,?,?)";
											pstmt = conn.prepareStatement(insertItemSql);
											pstmt.setString(1, enUKLocaleForNewTable.trim());
											pstmt.setString(2, itemDetails.getCarlineCode().trim());
											pstmt.setString(3, itemDetails.getModel().trim());
											pstmt.setString(4, itemDetails.getWmiCode().trim());
											pstmt.setString(5, itemDetails.getVdsCode().trim());
											pstmt.setString(6, itemDetails.getVinStartRange().trim());
											pstmt.setString(7, itemDetails.getVinEndRange().trim());
											pstmt.setTimestamp(8, new Timestamp(new Date().getTime()));
											pstmt.executeUpdate();
											pstmt.close();pstmt = null;
											insertItemSql = null;
										}
									}
									catch(Exception eq)
									{
										Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "addVINDetails()", eq);
									}
								}
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
					String updateSql="UPDATE gms3_mdm_mme_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
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
							String insertItemSql="INSERT INTO gms3_mdm_mme_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getVdsCode());
							pstmt.setString(6, itemDetails.getVinStartRange());
							pstmt.setString(7, itemDetails.getVinEndRange());
							pstmt.setString(8, itemDetails.getFirstLevelRefKey());
							pstmt.setString(9, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(10, itemDetails.getThirdLevelRefKey());
							pstmt.setString(11, documentDetails.getWslId());
							pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(13, itemDetails.getModelRegionalName());
							pstmt.setString(14, itemDetails.getFourthLevelRefKey());
							pstmt.setString(15, itemDetails.getFifthLevelRefKey());
							pstmt.executeUpdate();
							pstmt.close();pstmt= null;
							insertItemSql=null;		
						
							/*
							 * check if CUSTOM VDS IS TRUE = INSERT DATA IN NEW TABLE GMS3_MDM_MME_SIVIN_CUSTOM
							 * 23RD JUNE 2019
							 */
							if(itemDetails.isCustomVDS()==true)
							{
								try
								{
									if(null!=rs)
									{
										rs.close();
									}
									pstmt = null;
									rs = null;
									long newTableAutoId=0;
									insertItemSql = "SELECT mdm_custom_vds_id FROM gms3_mdm_mme_sivin_custom WHERE TRIM(LOWER(mdm_locale))=? AND "
											+ "TRIM(LOWER(mdm_crln_code))=?  AND TRIM(LOWER(mdm_crln_name_eng))=? AND TRIM(LOWER(mdm_vds_code)) = ? AND "
											+ "TRIM(LOWER(mdm_vis_end_range)) = ? AND TRIM(LOWER(mdm_vis_start_range)) = ? AND TRIM(LOWER(mdm_wmi_code)) = ?";
									pstmt = conn.prepareStatement(insertItemSql);
									pstmt.setString(1, enUKLocaleForNewTable.trim().toLowerCase());
									pstmt.setString(2, itemDetails.getCarlineCode().trim().toLowerCase());
									pstmt.setString(3, itemDetails.getModel().trim().toLowerCase());
									pstmt.setString(4, itemDetails.getVdsCode().trim().toLowerCase());
									pstmt.setString(5, itemDetails.getVinEndRange().trim().toLowerCase());
									pstmt.setString(6, itemDetails.getVinStartRange().trim().toLowerCase());
									pstmt.setString(7, itemDetails.getWmiCode().trim().toLowerCase());
									rs = pstmt.executeQuery();
									if(rs.next())
									{
										newTableAutoId = rs.getLong("mdm_custom_vds_id");
									}
									rs.close();rs= null;
									pstmt.close();pstmt = null;
									insertItemSql = null;
									if(newTableAutoId > 0)
									{
										// UPDTAE EXISTING ROW TIMESTAMP
										insertItemSql = "UPDATE gms3_mdm_mme_sivin_custom SET mdm_custom_updated_tmstp = ? WHERE mdm_custom_vds_id = ?";
										pstmt = conn.prepareStatement(insertItemSql);
										pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
										pstmt.setLong(2, newTableAutoId);
										pstmt.executeUpdate();
										pstmt.close();pstmt = null;
										insertItemSql = null;
									}
									else
									{
										// INSERT NEW ROW
										insertItemSql = "INSERT INTO gms3_mdm_mme_sivin_custom (mdm_locale,mdm_crln_code,mdm_crln_name_eng,mdm_wmi_code,"
												+ "mdm_vds_code,mdm_vis_start_range,mdm_vis_end_range,mdm_custom_created_tmstp) VALUES(?,?,?,?,?,?,?,?)";
										pstmt = conn.prepareStatement(insertItemSql);
										pstmt.setString(1, enUKLocaleForNewTable.trim());
										pstmt.setString(2, itemDetails.getCarlineCode().trim());
										pstmt.setString(3, itemDetails.getModel().trim());
										pstmt.setString(4, itemDetails.getWmiCode().trim());
										pstmt.setString(5, itemDetails.getVdsCode().trim());
										pstmt.setString(6, itemDetails.getVinStartRange().trim());
										pstmt.setString(7, itemDetails.getVinEndRange().trim());
										pstmt.setTimestamp(8, new Timestamp(new Date().getTime()));
										pstmt.executeUpdate();
										pstmt.close();pstmt = null;
										insertItemSql = null;
									}
								}
								catch(Exception eq)
								{
									Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "addVINDetails()", eq);
								}
							}
							
							itemDetails=null;
						}
					}
					// COMMIT THE TRANSACTION
					conn.commit();
				}
				enUKLocaleForNewTable = null;
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
				Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "addVINDetails()", e1);
			}
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "addVINDetails()", e);
			
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
				Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "addVINDetails()", e1);
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
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_mme_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
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
					 * INSERT INTO OK_DC.GMS3_MDM_MME_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_mme_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
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
						String updateSql="UPDATE gms3_mdm_mme_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
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
								String insertItemSql="INSERT INTO gms3_mdm_mme_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp,mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getVdsCode());
								pstmt.setString(6, itemDetails.getVinStartRange());
								pstmt.setString(7, itemDetails.getVinEndRange());
								pstmt.setString(8, itemDetails.getFirstLevelRefKey());
								pstmt.setString(9, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(10, itemDetails.getThirdLevelRefKey());
								pstmt.setString(11, documentDetails.getWslId());
								pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
								pstmt.setString(13, itemDetails.getModelRegionalName());
								pstmt.setString(14, itemDetails.getFourthLevelRefKey());
								pstmt.setString(15, itemDetails.getFifthLevelRefKey());
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
					String updateSql="UPDATE gms3_mdm_mme_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
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
							String insertItemSql="INSERT INTO gms3_mdm_mme_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp, mdm_sivin_model_regional,"
										+ "mdm_sivin_forthlevel_refkey, mdm_sivin_fifthlevel_refkey) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getVdsCode());
							pstmt.setString(6, itemDetails.getVinStartRange());
							pstmt.setString(7, itemDetails.getVinEndRange());
							pstmt.setString(8, itemDetails.getFirstLevelRefKey());
							pstmt.setString(9, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(10, itemDetails.getThirdLevelRefKey());
							pstmt.setString(11, documentDetails.getWslId());
							pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(13, itemDetails.getModelRegionalName());
							pstmt.setString(14, itemDetails.getFourthLevelRefKey());
							pstmt.setString(15, itemDetails.getFifthLevelRefKey());
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
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "deleteVINDetails()", e);
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
	
	public static SIVinDetails checkVININMDM(String languageId, SIVinDetails fieldDetails, Connection conn, String closeConnection) throws SQLException
	{
		PreparedStatement pstmt=null;
		ResultSet rs= null;
		try
		{
			if(null!=languageId && !"".equals(languageId) && null!=fieldDetails && null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) 
					&& null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()))
			{
				// HERE LOCALE WILL ALWAYS BE ENUK LOCALE
				String enukLocale = ApplicationProperties.getProperty("en_uk");
				enukLocale= enukLocale.replace("_", "-");
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				
				boolean vinFound = false;
				String sql="SELECT mdm_crln_code, mdm_crln_name_eng_lang, mdm_crln_name_regional_lang FROM gms3_mdm_vin_detail WHERE mdm_ml_lang_code = '"+enukLocale+"' "
						+ " AND mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' AND TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_vin_vds_code))=? AND TRIM(LOWER(mdm_crln_code))=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, fieldDetails.getWmiCode().trim().toLowerCase());
				pstmt.setString(2, fieldDetails.getVdsCode().trim().toLowerCase());
				pstmt.setString(3, fieldDetails.getCarlineCode().trim().toLowerCase());
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
				
				/*
				 * ADDED ON 2ND JUNE 2019, CUSTOM VINS ALSO ALLOWED WHICH DOES NOT EXIST IN MDM
				 * IN THAT CASE GET CARLINE DETAILS FROM MDM USING CARLINE CODE + WMI CODE
				 */
				if(vinFound==false)
				{
					/*
					 * VDS CODE IS CUSTOM VDS SET FLAG TO TRUE
					 * 23RD JUNE 2019
					 */
					fieldDetails.setCustomVDS(true);
					
					sql="SELECT mdm_crln_code, mdm_crln_name_eng_lang, mdm_crln_name_regional_lang FROM gms3_mdm_carline_codes WHERE mdm_ml_lang_code = '"+enukLocale+"' "
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
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "checkVININMDM()", e);
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
	
	public static List<String> getDocumentTranslations(String documentId)
	{
		List<String> translationLoclaes= null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Connection  conn = null;
		try
		{
			if(null!=documentId && !"".equals(documentId))
			{
				/*
				 * change 4th feb 2021 - do not allow MNAO & MC Locales as well
				 *
				 * MIGRATED TO KAPTURE 2026-08-04. This was still
				 * "SELECT DISTINCT LOCALEID FROM OK_IM.CONTENTTEXT" - a cross-schema Oracle read
				 * with no equivalent on this connection, so it threw, the catch below swallowed it
				 * and the method returned null EVERY TIME. The caller
				 * (MMESIVin deleteContent loop over translation locales) has therefore never
				 * actually run. Note it also moved to the CMS connection: k_article lives in
				 * kapture_cms_db, which is a SEPARATE SERVER from the MDM schema.
				 *
				 * A ROW'S LOCALE IS ITS EFFECTIVE LOCALE - article_translated_locale when set,
				 * article_primary_locale otherwise. A translation carries the MASTER's primary
				 * locale, so reading article_primary_locale alone would name the master once per
				 * translation and never name the translation itself.
				 *
				 * THE MASTER NEEDS NO SPECIAL CASE - THE EXCLUSION LIST ALREADY DECIDES IT.
				 * An en_UK master survives the list like any other MME locale and IS updated;
				 * an en_US or ja_JP master is filtered out as an MNAO/MC locale. That is the
				 * business rule ("update the master too, unless the master is MNAO's or MC's")
				 * falling straight out of the filter, so do NOT add a master-vs-translation test.
				 */
				String sql = "SELECT DISTINCT COALESCE(article_translated_locale,"
						+ " article_primary_locale) AS effective_locale"
						+ " FROM kapture_cms_db.k_article"
						+ " WHERE article_id = ? AND latest_version = 'Y'"
						+ " AND COALESCE(article_translated_locale, article_primary_locale)"
						+ " NOT IN (?, ?, ?, ?, ?, ?, ?)";
				conn = getCMSConnection();
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, documentId.trim());
				stmt.setString(2, ApplicationProperties.getProperty("en_eu"));
				stmt.setString(3, ApplicationProperties.getProperty("en_us"));
				stmt.setString(4, ApplicationProperties.getProperty("en_ca"));
				stmt.setString(5, ApplicationProperties.getProperty("es_mx"));
				stmt.setString(6, ApplicationProperties.getProperty("fr_ca"));
				stmt.setString(7, ApplicationProperties.getProperty("ja_jp"));
				stmt.setString(8, ApplicationProperties.getProperty("en_jp"));
				rs = stmt.executeQuery();
				while(rs.next())
				{
					String effectiveLocale = rs.getString("effective_locale");
					if(null!=effectiveLocale && !"".equals(effectiveLocale.trim()))
					{
						if(null==translationLoclaes || translationLoclaes.size()<=0)
						{
							translationLoclaes = new ArrayList<String>();
						}
						translationLoclaes.add(effectiveLocale.trim());
					}
				}
				logger.info("getDocumentTranslations :: {" + documentId + "} -> "
						+ (null == translationLoclaes ? 0 : translationLoclaes.size())
						+ " other locale(s) to update :: " + translationLoclaes);
				sql = null;
			}
			else
			{
				logger.info("getDocumentTranslations :: Document id as Parameter is null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getDocumentTranslations()", e);
			translationLoclaes = null;
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(MMESIVinDAO.class.getName(), "getDocumentTranslations()", re);
			}
			documentId = null;
		}
		return translationLoclaes;
	}
	
}
