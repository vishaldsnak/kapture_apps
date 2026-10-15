
package com.mazda.gms3.mdm.dao;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;




import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.mdm.vo.VinDetails;

public class CarlineDAO extends DBConnectionHelper{

	/*
	 * LOGIC FOR MANAGING SYNC STATUS
	 * 
	 * If a new record is getting inserted -
		saveNew Functionality - 
		status is Draft
		syncStatus - Blank
		
		Active Functionality -  
		
		check previous status, 
		if Draft, then update syncStatus as N
		if Active, then let it remains as it is
		
		Inactive Functionality - then blank syncStatus
		
		Update Functionality - 
		If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
		
		If PreviousStatus and CurrentStatus is Same and Status is Active - 
		if CARLINE NAME (ENG / REG), CARLINE CODE, YEAR START, YEAR END CHANGES , then update syncStatus as N
		
		If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
		
		IF PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
		
		IMPORT - 
		
		INCASE OF CREATE - 
			STATUS IS ALWAYS DRAFT - SO synStatus as Blank
		INCASE OF Update
				Set Old details and compare it with new details
					if details same - 
						do nothing with syncStatus
					if details different -
						if status is Draft - do nothing with Blank status
						if status is Active - update syncStatus as N


	 */
	
	
	private static Logger logger = LogManager.getLogger(CarlineDAO.class);
	
	public static ArrayList<CarlineDetails> getCarlineDetailsList(String langCode) throws SQLException 
	{
//		logger.info("getCarlineDetailsList :: Method Starts.");
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();

				String sql="SELECT * FROM gms3_mdm_carline_codes WHERE mdm_ml_id="+langCode+" "
						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				String sql="SELECT * FROM gms3_mdm_carline_codes WHERE mdm_ml_lang_code='"+langCode+"' "
//						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
//				logger.info("getCarlineDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
					carDetails.setSrNo(carlineList.size()+1);
					carDetails.setCarlineCodeId(rs.getLong("mdm_crln_code_id"));
					if(null!=rs.getString("mdm_crln_name_eng_lang") && !"".equals(rs.getString("mdm_crln_name_eng_lang")))
					{
						carDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
						carDetails.setOldCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang") && !"".equals(rs.getString("mdm_crln_name_regional_lang")))
					{
						carDetails.setCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
						carDetails.setOldCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
					}
					carDetails.setCountryLocaleId(rs.getLong("mdm_cl_id"));
					carDetails.setManualLanguageId(rs.getLong("mdm_ml_id"));
					if(null!=rs.getString("mdm_ml_lang_code") && !"".equals(rs.getString("mdm_ml_lang_code")))
					{
						carDetails.setManualLanguageCode(rs.getString("mdm_ml_lang_code").trim());
					}
					if(null!=rs.getString("mdm_cl_locale_code") && !"".equals(rs.getString("mdm_cl_locale_code")))
					{
						carDetails.setCountryLocaleCode(rs.getString("mdm_cl_locale_code").trim());
					}
					carDetails.setCarlineCode(rs.getString("mdm_crln_code"));
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
					{
						carDetails.setCarlineCode(carDetails.getCarlineCode().trim());
						carDetails.setOldCarlineCode(carDetails.getCarlineCode().trim());
					}
					carDetails.setModelType(rs.getString("mdm_crln_model_type"));
					if(null!=carDetails.getModelType() && !"".equals(carDetails.getModelType()))
					{
						carDetails.setModelType(carDetails.getModelType().trim());
					}
					carDetails.setEsiCategoryFlag(rs.getString("mdm_crln_esi_cat_flag"));
					if(rs.getInt("mdm_crln_year_start")>0)
					{
						carDetails.setYearStart(String.valueOf(rs.getInt("mdm_crln_year_start")));
						carDetails.setOldYearStart(String.valueOf(rs.getInt("mdm_crln_year_start")));
					}
					if(rs.getInt("mdm_crln_year_end")>0)
					{
						carDetails.setYearEnd(String.valueOf(rs.getInt("mdm_crln_year_end")));
						carDetails.setOldYearEnd(String.valueOf(rs.getInt("mdm_crln_year_end")));
					}
					carDetails.setFlag(rs.getString("mdm_crln_flag"));
					carDetails.setOldFlag(rs.getString("mdm_crln_flag"));
					carDetails.setEntryTime(rs.getTimestamp("mdm_crln_created_tmstp"));
					carDetails.setUpdatedTime(rs.getTimestamp("mdm_crln_updated_tmstp"));
					carDetails.setWmiCode(rs.getString("mdm_crln_wmi_code"));
					if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
					{
						carDetails.setWmiCode(carDetails.getWmiCode().trim());
					}
					if(null!=carDetails.getFlag() && !"".equals(carDetails.getFlag()) && 
							carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
					{
						carDetails.setShowCheckBox(false);
					}
					
					// SET IM NOTIFICATION VARIABLES
					carDetails.setCarlineNotificationSent(rs.getString("mdm_crln_notif_sent"));
					carDetails.setCarlineNotificationYearEndOnly(rs.getString("mdm_crln_year_end_notif_only"));
					carDetails.setCarlineNotifcationPreYearValue(rs.getString("mdm_crln_prev_year_end"));
					carDetails.setCarlineNotifcationSentTimestamp(rs.getTimestamp("mdm_crln_notif_sent_tmstp"));
					
					if(rs.getInt("mdm_crln_year_end")>0)
					{
						carDetails.setPreviousYearEnd(String.valueOf(rs.getInt("mdm_crln_year_end")));
					}
					
					
					if(null!=rs.getString("mdm_ic_display_carline_code") && !"".equals(rs.getString("mdm_ic_display_carline_code")))
					{
						carDetails.setIcDisplayCarlineCode(rs.getString("mdm_ic_display_carline_code").trim());
					}
					
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						carDetails.setSyncStatus(rs.getString("mdm_sync_status").trim());
					}
					carlineList.add(carDetails);
					
					carDetails=  null;
				}
				stmt.close();stmt=null;
				rs.close();rs=null;
				sql = null;
			}
			else
			{
				logger.info("getCarlineDetailsList :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCarlineDetailsList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineDetailsList()", e);
			logger.info("getCarlineDetailsList :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getCarlineDetailsList :: Method Ends.");
		return carlineList;
	}

	public static ArrayList<SelectItemDetails> getModelTypeDetails() throws SQLException
	{
		ArrayList<SelectItemDetails> modelTypeDetails = new ArrayList<SelectItemDetails>();
		Connection  conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT * FROM gms3_mdm_carline_modeltype";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SelectItemDetails si  =new SelectItemDetails();
				si.setLabel(rs.getString("mdm_crlnmt_name"));
				if(null!=si.getLabel())
				{
					si.setLabel(si.getLabel().trim());
				}
				si.setValue(rs.getString("mdm_crlnmt_name"));
				if(null!=si.getValue())
				{
					si.setValue(si.getValue().trim());
				}
				modelTypeDetails.add(si);
				si = null;
			}
			sql  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getModelTypeDetails()", e);
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
		return modelTypeDetails;
	}
	
	public static ArrayList<CarlineDetails> getCarlineDetailsListForComboFORVIN(String langCode) throws SQLException 
	{
//		logger.info("getCarlineDetailsListForCombo :: Method Starts.");
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				/*
				 * GET UNIQUE CARLINE CODES + CARLINE NAMES
				 */
				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name_eng_lang FROM "
					+ " gms3_mdm_carline_codes  "
					+ " WHERE mdm_ml_id="+new Long(langCode).longValue()+" "
							+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
									+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
											+ "GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
					if(null!=rs.getString("mdm_crln_code"))
					{
						carDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						carDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()) 
							&& null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
						carDetails.setCarlineIdForCombo(carDetails.getCarlineNameEng().trim().toUpperCase()+"_"+carDetails.getCarlineCode().trim().toUpperCase());
					}
					carlineList.add(carDetails);
					carDetails = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
			}
			else
			{
				logger.info("getCarlineDetailsListForComboFORVIN :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineDetailsListForComboFORVIN()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getCarlineDetailsListForCombo :: Method Ends.");
		return carlineList;
	}
	
	public static ArrayList<CarlineDetails> getCarlineDetailsListForComboFORRMIExportTool(String langCode) throws SQLException 
	{
//		logger.info("getCarlineDetailsListForCombo :: Method Starts.");
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				/*
				 * GET UNIQUE CARLINE CODES + CARLINE NAMES
				 */
				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name_eng_lang FROM "
					+ " gms3_mdm_carline_codes  "
					+ " WHERE mdm_ml_lang_code='"+langCode+"' "
							+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
									+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
											+ "GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
					if(null!=rs.getString("mdm_crln_code"))
					{
						carDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						carDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()) 
							&& null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
						carDetails.setCarlineIdForCombo(carDetails.getCarlineNameEng().trim().toUpperCase()+"_"+carDetails.getCarlineCode().trim().toUpperCase());
					}
					carlineList.add(carDetails);
					carDetails = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
			}
			else
			{
				logger.info("getCarlineDetailsListForComboFORRMIExportTool :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineDetailsListForComboFORRMIExportTool()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getCarlineDetailsListForCombo :: Method Ends.");
		return carlineList;
	}
	
	public static ArrayList<CarlineDetails> getCarlineDetailsListForComboFORMNAOExportTool(String langCode) throws SQLException 
	{
//		logger.info("getCarlineDetailsListForCombo :: Method Starts.");
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				/*
				 * GET UNIQUE CARLINE CODES + CARLINE NAMES
				 * 
				 * GET UNIQUE CARLINE NAMES ONLY
				 */
//				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name_eng_lang FROM "
//					+ " gms3_mdm_carline_codes  "
//					+ " WHERE mdm_ml_lang_code IN ("+langCode+") "
//							+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
//									+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
//											+ "GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				String sql="SELECT DISTINCT  mdm_crln_name_eng_lang FROM "
						+ " gms3_mdm_carline_codes  "
						+ " WHERE mdm_ml_lang_code IN ("+langCode+") "
								+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
										+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
												+ "GROUP BY  mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
//					if(null!=rs.getString("mdm_crln_code"))
//					{
//						carDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
//					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						carDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
//					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()) 
//							&& null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
//						carDetails.setCarlineIdForCombo(carDetails.getCarlineNameEng().trim().toUpperCase()+"_"+carDetails.getCarlineCode().trim().toUpperCase());
						carDetails.setCarlineIdForCombo(carDetails.getCarlineNameEng().trim().toUpperCase());
					}
					carlineList.add(carDetails);
					carDetails = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
			}
			else
			{
				logger.info("getCarlineDetailsListForComboFORMNAOExportTool :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineDetailsListForComboFORMNAOExportTool()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getCarlineDetailsListForComboFORMNAOExportTool :: Method Ends.");
		return carlineList;
	}
	
	
	public static ArrayList<SelectItemDetails> getWMIList(String carlineCode, String carlineEngName, String langCode , 
			Connection conn, String closeConnection) throws SQLException
	{
		ArrayList<SelectItemDetails> wmiList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				// do this when object is passed as null
				if(null==conn || conn.isClosed()==true)
				{
					conn = getConnection();
				}
				String sql="SELECT DISTINCT mdm_crln_wmi_code FROM gms3_mdm_carline_codes WHERE mdm_ml_id=? ";
				if(null!=carlineEngName && !"".equals(carlineEngName) && null!=carlineCode && !"".equals(carlineCode))
				{
					sql = sql+" AND TRIM(LOWER(mdm_crln_name_eng_lang))=? AND TRIM(LOWER(mdm_crln_code))=? ";
				}
				else if(null!=carlineEngName && !"".equals(carlineEngName))
				{
					sql = sql+" AND TRIM(LOWER(mdm_crln_name_eng_lang))=? ";
				}
				else if(null!=carlineCode && !"".equals(carlineCode))
				{
					sql = sql+" AND TRIM(LOWER(mdm_crln_code))=? ";
				}
				
				sql = sql+" AND mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
				
				stmt = conn.prepareStatement(sql);
				stmt.setLong(1, new Long(langCode).longValue());
				if(null!=carlineEngName && !"".equals(carlineEngName) && null!=carlineCode && !"".equals(carlineCode))
				{
					stmt.setString(2, carlineEngName.trim().toLowerCase());
					stmt.setString(3, carlineCode.trim().toLowerCase());
				}
				else if(null!=carlineEngName && !"".equals(carlineEngName))
				{
					stmt.setString(2, carlineEngName.trim().toLowerCase());
				}
				else if(null!=carlineCode && !"".equals(carlineCode))
				{
					stmt.setString(2, carlineCode.trim().toLowerCase());
				}
				
				rs = stmt.executeQuery();
				while(rs.next())
				{
					SelectItemDetails si= new SelectItemDetails();
					si.setLabel(rs.getString("mdm_crln_wmi_code"));
					si.setValue(rs.getString("mdm_crln_wmi_code"));
					wmiList.add(si);
				}
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
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getWMIList()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			langCode = null;
		}
		return wmiList;
				
	}
	
	public static VinDetails getCarlineRegionalName(String carlineCode, String carlineEngName, String wmiCode,String langCode, Connection conn, String closeConnection, VinDetails vindetails) throws SQLException 
	{
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=carlineCode && !"".equals(carlineCode) && null!=carlineEngName && !"".equals(carlineEngName) 
					&& null!=wmiCode && !"".equals(wmiCode) && null!=langCode && !"".equals(langCode))				
			{
				// do this when object is passed as null
				if(null==conn || conn.isClosed()==true)
				{
					conn = getConnection();
				}
				String sql = "SELECT mdm_crln_name_regional_lang,mdm_ic_display_carline_code  FROM "
						+ " gms3_mdm_carline_codes WHERE mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))=? "
						+ " AND TRIM(LOWER(mdm_crln_code))=? AND "
						+ " TRIM(LOWER(mdm_crln_wmi_code))=? AND mdm_ml_id=?";
				
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, carlineEngName.trim().toLowerCase());
				stmt.setString(2, carlineCode.trim().toLowerCase());
				stmt.setString(3, wmiCode.trim().toLowerCase());
				stmt.setLong(4, new Long(langCode).longValue());
				rs = stmt.executeQuery();
				if(rs.next())
				{
					// SET NAME IN THE CARLINE
					if(null!=rs.getString("mdm_crln_name_regional_lang") && !"".equals(rs.getString("mdm_crln_name_regional_lang")))
					{
//						regionalName = rs.getString("mdm_crln_name_regional_lang").trim();
						vindetails.setCarlineNameReg(rs.getString("mdm_crln_name_regional_lang").trim());
					}
					if(null!=rs.getString("mdm_ic_display_carline_code") && !"".equals(rs.getString("mdm_ic_display_carline_code")))
					{
						vindetails.setIcDisplayCarlineCode(rs.getString("mdm_ic_display_carline_code"));
					}
				}
				sql = null;
			}
			else
			{
				logger.info("getCarlineRegionalName :: Carline Code / Carline Eng Name / WMI Code / Lang Code as Parameter are null. Returning null.");
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
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineRegionalName()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return vindetails;
	}
	
	public static boolean saveCarlineDetails(CarlineDetails carDetails) throws SQLException
	{
		logger.info("saveCarlineDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			/*
			 * IDENTIFY PERFORMING SAVE FOR WHICH LOCATION
			 * JAPAN OR MNAO COUNTRIES
			 */
			boolean mnaoCountries = false;
			if(null!=carDetails.getManualLanguageCode() && 
					 !"".equals(carDetails.getManualLanguageCode()))
			{
				String key = carDetails.getManualLanguageCode();
				key = key.replace("-", "_");
				StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
				while(str.hasMoreTokens())
				{
					String tok = str.nextToken();
					if(tok.trim().toLowerCase().equals(key.trim().toLowerCase()))
					{
						// MNAO COUNTRY
						mnaoCountries = true;
						break;
					}
				}
				key = null;
			}
			int yearEnd=0;
			if(null!=carDetails.getYearEnd() && !"".equals(carDetails.getYearEnd()))
			{
				yearEnd= new Integer(carDetails.getYearEnd().trim()).intValue();
			}
			int yearStart=0;
			if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
			{
				yearStart = new Integer(carDetails.getYearStart().trim()).intValue();
			}
			
			if(mnaoCountries==true)
			{
				if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
				{
					carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
				}
				if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
				{
					carDetails.setWmiCode(carDetails.getWmiCode().trim());
				}
				if(null!=carDetails.getCountryLocaleCode() && !"".equals(carDetails.getCountryLocaleCode()))
				{
					carDetails.setCountryLocaleCode(carDetails.getCountryLocaleCode().trim());
				}
				if(null!=carDetails.getManualLanguageCode() && !"".equals(carDetails.getManualLanguageCode()))
				{
					carDetails.setManualLanguageCode(carDetails.getManualLanguageCode().trim());
				}
				if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
				{
					carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim().toUpperCase());
				}
				if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
				{
					carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim().toUpperCase());
				}
				if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
				{
					carDetails.setIcDisplayCarlineCode(carDetails.getIcDisplayCarlineCode().trim().toUpperCase());
				}
				String sql="INSERT INTO gms3_mdm_carline_codes( mdm_crln_code,"
						+ " mdm_crln_year_start,mdm_crln_year_end,mdm_crln_flag,"
						+ " mdm_crln_created_tmstp,mdm_crln_wmi_code, mdm_cl_locale_code, mdm_ml_lang_code,"
						+ " mdm_crln_name_eng_lang, mdm_crln_name_regional_lang,mdm_cl_id, mdm_ml_id, mdm_crln_notif_sent,mdm_ic_display_carline_code) "
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, carDetails.getCarlineCode());
				pstmt.setInt(2, yearStart);
				pstmt.setInt(3, yearEnd);
				pstmt.setString(4, carDetails.getFlag());
				pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(6, carDetails.getWmiCode());
				pstmt.setString(7, carDetails.getCountryLocaleCode());
				pstmt.setString(8, carDetails.getManualLanguageCode());
				pstmt.setString(9, carDetails.getCarlineNameEng());
				pstmt.setString(10, carDetails.getCarlineNameReg());
				pstmt.setLong(11,carDetails.getCountryLocaleId());
				pstmt.setLong(12,carDetails.getManualLanguageId());
				// ALWAYS SET HERE CARLINE NOTIFICATION FLAG AS N
				pstmt.setString(13, ApplicationProperties.getProperty("value.esicategory.flag.no").trim());
				pstmt.setString(14, carDetails.getIcDisplayCarlineCode());
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
				// COMMIT THE TRANSACTION
				conn.commit();
			}
			else if(mnaoCountries==false)
			{
				if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
				{
					carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
				}
				if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
				{
					carDetails.setWmiCode(carDetails.getWmiCode().trim());
				}
				if(null!=carDetails.getCountryLocaleCode() && !"".equals(carDetails.getCountryLocaleCode()))
				{
					carDetails.setCountryLocaleCode(carDetails.getCountryLocaleCode().trim());
				}
				if(null!=carDetails.getManualLanguageCode() && !"".equals(carDetails.getManualLanguageCode()))
				{
					carDetails.setManualLanguageCode(carDetails.getManualLanguageCode().trim());
				}
				if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
				{
					carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim());
				}
				if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
				{
					carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim());
				}
				if(null!=carDetails.getModelType() && !"".equals(carDetails.getModelType()))
				{
					carDetails.setModelType(carDetails.getModelType().trim());
				}
				if(null!=carDetails.getEsiCategoryFlag() && !"".equals(carDetails.getEsiCategoryFlag()))
				{
					carDetails.setEsiCategoryFlag(carDetails.getEsiCategoryFlag().trim());
				}
				if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
				{
					carDetails.setIcDisplayCarlineCode(carDetails.getIcDisplayCarlineCode().trim().toUpperCase());
				}
				
				String sql="INSERT INTO gms3_mdm_carline_codes(mdm_crln_code "
						+ " ,mdm_crln_flag,"
						+ " mdm_crln_created_tmstp,mdm_crln_wmi_code, mdm_cl_locale_code, mdm_ml_lang_code,"
						+ " mdm_crln_name_eng_lang, mdm_crln_name_regional_lang , "
						+ " mdm_crln_model_type, mdm_crln_esi_cat_flag,mdm_cl_id, mdm_ml_id,mdm_crln_notif_sent,mdm_ic_display_carline_code) "
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, carDetails.getCarlineCode());
				pstmt.setString(2, carDetails.getFlag());
				pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(4, carDetails.getWmiCode());
				pstmt.setString(5, carDetails.getCountryLocaleCode());
				pstmt.setString(6, carDetails.getManualLanguageCode());
				pstmt.setString(7, carDetails.getCarlineNameEng());
				pstmt.setString(8, carDetails.getCarlineNameReg());
				pstmt.setString(9, carDetails.getModelType());
				pstmt.setString(10, carDetails.getEsiCategoryFlag());
				pstmt.setLong(11,carDetails.getCountryLocaleId());
				pstmt.setLong(12,carDetails.getManualLanguageId());
				// ALWAYS SET HERE CARLINE NOTIFICATION FLAG AS N
				pstmt.setString(13, ApplicationProperties.getProperty("value.esicategory.flag.no").trim());
				pstmt.setString(14, carDetails.getIcDisplayCarlineCode());
				pstmt.executeUpdate();
				sql = null;
				pstmt.close();pstmt=null;
				// COMMIT THE TRANSACTION
				conn.commit();
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("saveCarlineDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "saveCarlineDetails()", e);
			logger.info("saveCarlineDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			// set passed param to null
			carDetails = null;
		}
		logger.info("saveCarlineDetails :: Method Ends.");
		return true;
	}
	
	public static ArrayList<CarlineDetails> updateCarlineDetails(ArrayList<CarlineDetails> dataList) throws SQLException
	{
		logger.info("updateCarlineDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn= null;
		try
		{
			if(null!=dataList && dataList.size()>0)
			{
				conn = getConnection();
				String sql = null;
				CarlineDetails carDetails = new CarlineDetails();
				for(int a=0;a<dataList.size();a++)
				{
					carDetails = (CarlineDetails)dataList.get(a);
					try
					{
						/*
						 * IDENTIFY PERFORMING SAVE FOR WHICH LOCATION
						 * JAPAN OR MNAO COUNTRIES
						 */
						boolean mnaoCountries = false;
						if(null!=carDetails.getManualLanguageCode() && 
								!"".equals(carDetails.getManualLanguageCode()))
						{
							String key = carDetails.getManualLanguageCode();
							key = key.replace("-", "_");
							StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
							while(str.hasMoreTokens())
							{
								String tok = str.nextToken();
								if(tok.trim().toLowerCase().equals(key.trim().toLowerCase()))
								{
									// MNAO COUNTRY
									mnaoCountries = true;
									break;
								}
							}
							key = null;
						}

						int yearEnd=0;
						if(null!=carDetails.getYearEnd() && !"".equals(carDetails.getYearEnd()))
						{
							yearEnd= new Integer(carDetails.getYearEnd().trim()).intValue();
						}
						int yearStart=0;
						if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
						{
							yearStart = new Integer(carDetails.getYearStart().trim()).intValue();
						}

						if(mnaoCountries==true)
						{
							if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
							{
								carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
							}
							if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
							{
								carDetails.setWmiCode(carDetails.getWmiCode().trim());
							}
							if(null!=carDetails.getCountryLocaleCode() && !"".equals(carDetails.getCountryLocaleCode()))
							{
								carDetails.setCountryLocaleCode(carDetails.getCountryLocaleCode().trim());
							}
							if(null!=carDetails.getManualLanguageCode() && !"".equals(carDetails.getManualLanguageCode()))
							{
								carDetails.setManualLanguageCode(carDetails.getManualLanguageCode().trim());
							}
							if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
							{
								carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim().toUpperCase());
							}
							if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
							{
								carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim().toUpperCase());
							}
							if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
							{
								carDetails.setIcDisplayCarlineCode(carDetails.getIcDisplayCarlineCode().trim().toUpperCase());
							}
							
							if(null!=carDetails.getOldCarlineCode() && !"".equals(carDetails.getOldCarlineCode()))
							{
								carDetails.setOldCarlineCode(carDetails.getOldCarlineCode().trim());
							}
							if(null!=carDetails.getOldCarlineNameEng() && !"".equals(carDetails.getOldCarlineNameEng()))
							{
								carDetails.setOldCarlineNameEng(carDetails.getOldCarlineNameEng().trim());
							}
							if(null!=carDetails.getOldCarlineNameReg() && !"".equals(carDetails.getOldCarlineNameReg()))
							{
								carDetails.setOldCarlineNameReg(carDetails.getOldCarlineNameReg().trim());
							}
							if(null!=carDetails.getSyncStatus() && !"".equals(carDetails.getSyncStatus()))
							{
								carDetails.setSyncStatus(carDetails.getSyncStatus().trim());
							}
							/*
							 * 
							 * CHECK IF CARLINE NOTIF FLAG IS NOT NULL & N - 
							 * DO NOT PERFORM BELOW STEPS AS IT IS NEW CARLINE AND NOTIFICATION HAS NOT BEEN SENT YET
							 * NO CHECK FOR YEAR START
							 * 
							 * IF YEAR END NOTIF IS NULL OR Y (FIRST TIME OR NOTIFICATION ALREADY SENT)
							 * 		IF CURRENT YEAR END IS GREATER THAN PREV YEAR END VALUE
							 * 			UPDATE YEAR END NOTIF FLAG - N
							 * 			SET NOTIF YEAR END VALUE - PREV YEAR END VALUE
							 * 		IF CURRENT YEAR END IS LESS THAN OR EQUALS TO PREV YEAR END VALUE
							 * 			DO NOTHING WITH NOTIF FLAGS / NOTIF YEAR END VALUE
							 * ELSE IF YEAR END NOTIF IS NOT NULL AND N (NOTIFICATION NOT YET SENT)
							 * 		IF CURRENT YEAR END IS GREATER THAN OR EQUALS TO PREV YEAR END VALUE
							 * 			DO NOTHING WITH NOTIF FLAGS / NOTIF YEAR END VALUE
							 * 		IF CURRENT YEAR END IS LESS THAN PREV YEAR END VALUE
							 * 			CHECK IF CURRENT YEAR END IS LESS THAN OR EQUALS TO NOTIF YEAR END VALUE
							 * 				SET NOTIF FLAG TO NULL
							 * 				SET NOTIF YEAR END VALUE TO NULL + TIMESTAMP AS WELL
							 * 			ELSE IF CURRENT YEAR END IS GREATER THEN NOTIF YEAR END VALUE
							 * 				DO NOTHIG WITH NOTIF FLAGS / NOTIF YEAR END VALUE
							 */

							/*
							 *  set these Variables from existing Values - to cover the scenario
							 *  a new row gets  created and same gets updated - let's say name updated
							 *  it should store the existing flags value which got  added while creation
							 *  else if these are initialized by null = they will override the flags and 
							 *  no notification will be sent for them in that cases.
							 */
							String carlineNotificationFlag=carDetails.getCarlineNotificationSent();
							String yearEndNotificationOnly=carDetails.getCarlineNotificationYearEndOnly();
							String notifYearEndValue=carDetails.getCarlineNotifcationPreYearValue();
							Timestamp notifSentTime = carDetails.getCarlineNotifcationSentTimestamp();

							boolean proceedFurther = true;
							if(null!=carDetails.getCarlineNotificationSent() && carDetails.getCarlineNotificationSent().equals(ApplicationProperties.getProperty("value.esicategory.flag.no").trim()))
							{
								// FLAG IS N DO NOT PROCEED
								proceedFurther=false;
							}

							if(proceedFurther==true)
							{
								int previousYearEnd=0;
								if(null!=carDetails.getPreviousYearEnd() && !"".equals(carDetails.getPreviousYearEnd()))
								{
									previousYearEnd= new Integer(carDetails.getPreviousYearEnd().trim()).intValue();
								}

								if(null!=yearEndNotificationOnly && yearEndNotificationOnly.equals(ApplicationProperties.getProperty("value.esicategory.flag.no").trim()))
								{
									// NOTIFICATION HAS NOT BEEN SENT YET
									if(yearEnd>0 && previousYearEnd>0)
									{
										if(yearEnd >= previousYearEnd)
										{
											// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP.
										}
										else if (yearEnd < previousYearEnd)
										{
											// CURRENT VALUE IS LESS THAN PREV VALUE
											if(null!=notifYearEndValue && !"".equals(notifYearEndValue))
											{
												int notifYearValue = new Integer(notifYearEndValue).intValue();
												if(notifYearValue>0)
												{
													if(yearEnd <= notifYearValue)
													{
														// CURRENT YEAR VALUE IS LESS THAN NOTIF YEAR VALUE - SET ALL FLAGS TO NULL
														notifYearEndValue = null;
														yearEndNotificationOnly = null;
														notifSentTime = null;
													}
													else if(yearEnd > notifYearValue)
													{
														// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP. 
													}
												}
											}
										}
									}
								}
								else
								{
									// EITHER FIRST TIME OR NOTIFICATION HAS ALREADY BEEN SENT
									if(yearEnd > previousYearEnd)
									{
										yearEndNotificationOnly=ApplicationProperties.getProperty("value.esicategory.flag.no").trim();
										notifYearEndValue = carDetails.getPreviousYearEnd();
									}
									else
									{
										// CURRENT YEAR END IS LESS THAN PREV YEAR END
										// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP.
									}
								}
							}

							// UPDATE THE VALUES BACK IN THE VARIABLES
							carDetails.setCarlineNotificationSent(carlineNotificationFlag);
							carDetails.setCarlineNotificationYearEndOnly(yearEndNotificationOnly);
							carDetails.setCarlineNotifcationPreYearValue(notifYearEndValue);
							carDetails.setCarlineNotifcationSentTimestamp(notifSentTime);

							carlineNotificationFlag = null;
							yearEndNotificationOnly = null;
							notifYearEndValue = null;
							notifSentTime = null;

							String syncStatus = carDetails.getSyncStatus();
							/*
							 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
								If PreviousStatus and CurrentStatus is Same and Status is Active - 
									if CARLINE NAME (ENG / REG), CARLINE CODE, YEAR START, YEAR END CHANGES , then update syncStatus as N

								If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
								If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
							 */
							if(carDetails.getOldFlag().equals(carDetails.getFlag()))
							{
								// STATUS IS SAME - check only for active
								if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
								{
									if(carDetails.getOldCarlineCode().trim().toLowerCase().equals(carDetails.getCarlineCode().trim().toLowerCase()) 
											&& carDetails.getOldCarlineNameEng().trim().toLowerCase().equals(carDetails.getCarlineNameEng().trim().toLowerCase()) 
											&& carDetails.getOldCarlineNameReg().trim().toLowerCase().equals(carDetails.getCarlineNameReg().trim().toLowerCase()) 
											&& carDetails.getOldYearStart().trim().equals(carDetails.getYearStart().trim()) 
											&& carDetails.getOldYearEnd().trim().equals(carDetails.getYearEnd().trim()))
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
								if(carDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
										carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
								{
									// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
									syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
								}
								else if(carDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
										carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
								{
									// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
									syncStatus = null;
								}
							}
							
							sql="UPDATE gms3_mdm_carline_codes SET mdm_crln_code=?, "
									+ "mdm_crln_year_start=?,mdm_crln_year_end=?,mdm_crln_flag=?,"
									+ " mdm_crln_updated_tmstp=?, mdm_crln_wmi_code=?,mdm_cl_locale_code=?,"
									+ " mdm_ml_lang_code = ?,mdm_crln_name_eng_lang = ?,mdm_crln_name_regional_lang = ?, mdm_crln_notif_sent = ? ,"
									+ " mdm_crln_year_end_notif_only = ? , mdm_crln_prev_year_end = ? ,"
									+ " mdm_crln_notif_sent_tmstp=?, mdm_ic_display_carline_code=?, mdm_sync_status=?  "
									+ "  WHERE mdm_crln_code_id =?";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1, carDetails.getCarlineCode());
							pstmt.setInt(2, yearStart);
							pstmt.setInt(3, yearEnd);
							pstmt.setString(4, carDetails.getFlag());
							pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(6, carDetails.getWmiCode());
							pstmt.setString(7, carDetails.getCountryLocaleCode());
							pstmt.setString(8, carDetails.getManualLanguageCode());
							pstmt.setString(9, carDetails.getCarlineNameEng());
							pstmt.setString(10, carDetails.getCarlineNameReg());
							pstmt.setString(11, carDetails.getCarlineNotificationSent());
							pstmt.setString(12, carDetails.getCarlineNotificationYearEndOnly());
							pstmt.setString(13, carDetails.getCarlineNotifcationPreYearValue());
							pstmt.setTimestamp(14, carDetails.getCarlineNotifcationSentTimestamp());
							pstmt.setString(15, carDetails.getIcDisplayCarlineCode());
							pstmt.setString(16, syncStatus);
							pstmt.setLong(17, carDetails.getCarlineCodeId());
							pstmt.executeUpdate();
							sql = null;
							syncStatus = null;
							carDetails.setSaveStatusWhileImport(true);
							pstmt.close();pstmt=null;
							/*
							 * UPDATE CARLINE CODE, ENG NAME, REG NAME IN VIN DETAILS TABLE
							 */
							try
							{
								sql = "UPDATE gms3_mdm_vin_detail SET mdm_crln_code=?, mdm_crln_name_eng_lang=?, mdm_crln_name_regional_lang =?,mdm_ic_display_carline_code = ? "
										+ " WHERE mdm_crln_code=? AND mdm_crln_name_eng_lang=? AND mdm_crln_name_regional_lang =? AND mdm_cl_id=? AND mdm_ml_id=? AND mdm_vin_wmi_code = ? ";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, carDetails.getCarlineCode());
								pstmt.setString(2, carDetails.getCarlineNameEng());
								pstmt.setString(3, carDetails.getCarlineNameReg());
								pstmt.setString(4, carDetails.getIcDisplayCarlineCode());
								pstmt.setString(5, carDetails.getOldCarlineCode());
								pstmt.setString(6, carDetails.getOldCarlineNameEng());
								pstmt.setString(7, carDetails.getOldCarlineNameReg());
								pstmt.setLong(8, carDetails.getCountryLocaleId());
								pstmt.setLong(9, carDetails.getManualLanguageId());
								pstmt.setString(10, carDetails.getWmiCode());
								pstmt.executeUpdate();
								pstmt.close();pstmt=null;
								sql = null;
							}
							catch(Exception eq)
							{
								Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "updateCarlineDetails()", eq);
							}
						}
						else if(mnaoCountries==false)
						{
							if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
							{
								carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
							}
							if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
							{
								carDetails.setWmiCode(carDetails.getWmiCode().trim());
							}
							if(null!=carDetails.getCountryLocaleCode() && !"".equals(carDetails.getCountryLocaleCode()))
							{
								carDetails.setCountryLocaleCode(carDetails.getCountryLocaleCode().trim());
							}
							if(null!=carDetails.getManualLanguageCode() && !"".equals(carDetails.getManualLanguageCode()))
							{
								carDetails.setManualLanguageCode(carDetails.getManualLanguageCode().trim());
							}
							if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
							{
								carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim());
							}
							if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
							{
								carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim());
							}
							if(null!=carDetails.getModelType() && !"".equals(carDetails.getModelType()))
							{
								carDetails.setModelType(carDetails.getModelType().trim());
							}
							if(null!=carDetails.getEsiCategoryFlag() && !"".equals(carDetails.getEsiCategoryFlag()))
							{
								carDetails.setEsiCategoryFlag(carDetails.getEsiCategoryFlag().trim());
							}
							if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
							{
								carDetails.setIcDisplayCarlineCode(carDetails.getIcDisplayCarlineCode().trim().toUpperCase());
							}

							if(null!=carDetails.getOldCarlineCode() && !"".equals(carDetails.getOldCarlineCode()))
							{
								carDetails.setOldCarlineCode(carDetails.getOldCarlineCode().trim());
							}
							if(null!=carDetails.getOldCarlineNameEng() && !"".equals(carDetails.getOldCarlineNameEng()))
							{
								carDetails.setOldCarlineNameEng(carDetails.getOldCarlineNameEng().trim());
							}
							if(null!=carDetails.getOldCarlineNameReg() && !"".equals(carDetails.getOldCarlineNameReg()))
							{
								carDetails.setOldCarlineNameReg(carDetails.getOldCarlineNameReg().trim());
							}
							if(null!=carDetails.getSyncStatus() && !"".equals(carDetails.getSyncStatus()))
							{
								carDetails.setSyncStatus(carDetails.getSyncStatus().trim());
							}

							
							String syncStatus = carDetails.getSyncStatus();
							/*
							 * 	If PreviousStatus and CurrentStatus is Same and Status is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
								If PreviousStatus and CurrentStatus is Same and Status is Active - 
									if CARLINE NAME (ENG / REG), CARLINE CODE  then update syncStatus as N

								If PreviousStatus is DRAFT and CurrentStatus is Active - then update syncStatus as N
								If PreviousStatus is Active and currentStatus is Draft - then blank syncStatus
							 */
							if(carDetails.getOldFlag().equals(carDetails.getFlag()))
							{
								// STATUS IS SAME - check only for active
								if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
								{
									if(carDetails.getOldCarlineCode().trim().toLowerCase().equals(carDetails.getCarlineCode().trim().toLowerCase()) 
											&& carDetails.getOldCarlineNameEng().trim().toLowerCase().equals(carDetails.getCarlineNameEng().trim().toLowerCase()) 
											&& carDetails.getOldCarlineNameReg().trim().toLowerCase().equals(carDetails.getCarlineNameReg().trim().toLowerCase()))
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
								if(carDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.draft")) && 
										carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
								{
									// STATUS CHANGES FROM DRAFT TO ACTIVE - SET SYNC STATUS TO N
									syncStatus = AccessManagementInterface.SYNC_STATUS_NO;
								}
								else if(carDetails.getOldFlag().equals(ApplicationProperties.getProperty("flag.value.active")) && 
										carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
								{
									// STATUS CHANGES FROM ACTIVE TO DRAFT - SET SYNC STATUS TO NULL
									syncStatus = null;
								}
							}
							
							/*
							 * NO NEED TO DO ANYTHING WITH THE NOTIFICATION FLAG HERE
							 */
							sql="UPDATE gms3_mdm_carline_codes SET mdm_crln_code=? "
									+ " ,mdm_crln_flag=?,"
									+ " mdm_crln_updated_tmstp=?, mdm_crln_wmi_code=?,mdm_cl_locale_code=?,"
									+ " mdm_ml_lang_code = ?,mdm_crln_name_eng_lang = ?,mdm_crln_name_regional_lang = ?, "
									+ " mdm_crln_model_type=?, mdm_crln_esi_cat_flag=?, mdm_ic_display_carline_code = ?, mdm_sync_status=?    "
									+ "  WHERE mdm_crln_code_id =?";
							pstmt = conn.prepareStatement(sql);

							pstmt.setString(1, carDetails.getCarlineCode());
							pstmt.setString(2, carDetails.getFlag());
							pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(4, carDetails.getWmiCode());
							pstmt.setString(5, carDetails.getCountryLocaleCode());
							pstmt.setString(6, carDetails.getManualLanguageCode());
							pstmt.setString(7, carDetails.getCarlineNameEng());
							pstmt.setString(8, carDetails.getCarlineNameReg());
							pstmt.setString(9, carDetails.getModelType());
							pstmt.setString(10, carDetails.getEsiCategoryFlag());
							pstmt.setString(11, carDetails.getIcDisplayCarlineCode());
							pstmt.setString(12, syncStatus);
							pstmt.setLong(13, carDetails.getCarlineCodeId());
							pstmt.executeUpdate();
							sql = null;
							syncStatus = null;
							carDetails.setSaveStatusWhileImport(true);
							pstmt.close();pstmt=null;
							/*
							 * UPDATE CARLINE CODE, ENG NAME, REG NAME IN VIN DETAILS TABLE
							 */
							try
							{
								sql = "UPDATE gms3_mdm_vin_detail SET mdm_crln_code=?, mdm_crln_name_eng_lang=?, mdm_crln_name_regional_lang =?,mdm_ic_display_carline_code=?  "
										+ " WHERE mdm_crln_code=? AND mdm_crln_name_eng_lang=? AND mdm_crln_name_regional_lang =? AND mdm_cl_id=? AND mdm_ml_id=? AND mdm_vin_wmi_code = ? ";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, carDetails.getCarlineCode());
								pstmt.setString(2, carDetails.getCarlineNameEng());
								pstmt.setString(3, carDetails.getCarlineNameReg());
								pstmt.setString(4, carDetails.getIcDisplayCarlineCode());
								pstmt.setString(5, carDetails.getOldCarlineCode());
								pstmt.setString(6, carDetails.getOldCarlineNameEng());
								pstmt.setString(7, carDetails.getOldCarlineNameReg());
								pstmt.setLong(8, carDetails.getCountryLocaleId());
								pstmt.setLong(9, carDetails.getManualLanguageId());
								pstmt.setString(10, carDetails.getWmiCode());
								pstmt.executeUpdate();
								pstmt.close();pstmt=null;
								sql = null;
							}
							catch(Exception eq)
							{
								Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "updateCarlineDetails()", eq);
							}
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "updateCarlineDetails()", e);
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "updateCarlineDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
		}
		logger.info("updateCarlineDetails :: Method Ends.");
		return dataList;
	}
	
	public static boolean activeCarDetails(List<Map<String,String>> activeIdsList) throws SQLException
	{
		logger.info("activeCarDetails :: Method Starts.");
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
					String sql="UPDATE gms3_mdm_carline_codes SET mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' "
							+ " ,mdm_crln_updated_tmstp = ?  ";
					if(null!=syncStatus && syncStatus.equals(AccessManagementInterface.SYNC_STATUS_NO))
					{
						sql+=" , mdm_sync_status ='"+syncStatus+"' ";
					}
					sql+=" WHERE mdm_crln_code_id = "+ dataMap.get("ID").toString();
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
				logger.info("activeCarDetails :: Active Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("activeCarDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "activeCarDetails()", e);
			logger.info("activeCarDetails :: ################ Exception ################");
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
		logger.info("activeCarDetails :: Method Ends.");
		return true;
	}
	
	public static boolean deleteCarlineDetails(String deleteIds) throws SQLException
	{
		logger.info("deleteCarlineDetails :: Method Starts.");
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
						// HERE FOR ALL RECORDS GETTING INACTIVE - SET SYNC STATUS TO NULL
						pstmt = null;
						String sql="UPDATE gms3_mdm_carline_codes SET mdm_crln_flag='"+ApplicationProperties.getProperty("flag.value.delete")+"' "
								+ " , mdm_crln_updated_tmstp = ? , mdm_sync_status=? WHERE mdm_crln_code_id = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(2, null);
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
				logger.info("deleteCarlineDetails :: Delete Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			logger.info("deleteCarlineDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "deleteCarlineDetails()", e);
			logger.info("deleteCarlineDetails :: ################ Exception ################");
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
		logger.info("deleteCarlineDetails :: Method Ends.");
		return true;
	}
	
	public static boolean importCarlineDetails(CarlineDetails carDetails, Connection conn, String closeConnection) throws SQLException
	{
		logger.info("importCarlineDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			// do this when object is passed as null
			if(null==conn || conn.isClosed()==true)
			{
				conn = getConnection();
			}
			
			if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
			{
				carDetails.setCarlineCode(carDetails.getCarlineCode().trim());
			}
			if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
			{
				carDetails.setWmiCode(carDetails.getWmiCode().trim());
			}
			if(null!=carDetails.getCountryLocaleCode() && !"".equals(carDetails.getCountryLocaleCode()))
			{
				carDetails.setCountryLocaleCode(carDetails.getCountryLocaleCode().trim());
			}
			if(null!=carDetails.getManualLanguageCode() && !"".equals(carDetails.getManualLanguageCode()))
			{
				carDetails.setManualLanguageCode(carDetails.getManualLanguageCode().trim());
			}
			if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
			{
				carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim());
			}
			if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
			{
				carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim());
			}
			if(null!=carDetails.getModelType() && !"".equals(carDetails.getModelType()))
			{
				carDetails.setModelType(carDetails.getModelType().trim());
			}
			if(null!=carDetails.getEsiCategoryFlag() && !"".equals(carDetails.getEsiCategoryFlag()))
			{
				carDetails.setEsiCategoryFlag(carDetails.getEsiCategoryFlag().trim());
			}
			if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
			{
				carDetails.setIcDisplayCarlineCode(carDetails.getIcDisplayCarlineCode().trim().toUpperCase());
			}
			
			/*
			 * IDENTIFY PERFORMING SAVE FOR WHICH LOCATION
			 * JAPAN OR MNAO COUNTRIES
			 */
			boolean mnaoCountries = false;
			if(null!=carDetails.getManualLanguageCode() && 
					 !"".equals(carDetails.getManualLanguageCode()))
			{
				String key = carDetails.getManualLanguageCode();
				key = key.replace("-", "_");
				StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
				while(str.hasMoreTokens())
				{
					String tok = str.nextToken();
					if(tok.trim().toLowerCase().equals(key.trim().toLowerCase()))
					{
						// MNAO COUNTRY
						mnaoCountries = true;
						break;
					}
				}
				key = null;
			}
			
			int yearEnd=0;
			if(null!=carDetails.getYearEnd() && !"".equals(carDetails.getYearEnd()))
			{
				yearEnd= new Integer(carDetails.getYearEnd().trim()).intValue();
			}
			int yearStart=0;
			if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
			{
				yearStart = new Integer(carDetails.getYearStart().trim()).intValue();
			}
			
			/*
			 * CHECK WHETHER THE CARLINE DESC ALREADY EXISTS OR NOT, IF YES
			 * THEN FETCH ITS CARLINE ID , UPDATE THE TIMESTAMP OF IT AND INSERT IN TO 
			 * CARLINE_CODES
			 */
			long autoCarlineCodeId=0;

			if(mnaoCountries==true)
			{
				String carlineNotificationSent=null;
				String yearEndNotificationOnly=null;
				String previousYearEndNoticationValue=null;
				String previousEndYear = null;
				Timestamp notifSentTime=null;
				
				String oldcarlineNameReg="";
				String oldYearStart="";
				String oldYearEnd="";
				String oldFlag="";
				String syncStatus="";
				
				/*
				 * IDENTIFY CARLINE CODE WHETHER EXISTS OR NOT. 
				 * E.G. CARLINE CODE, CARLINE NAME + WMI & YEAR START
				 * A CARLINE CODE + WMI CODE CAN HAVE MULTIPLE ROWS WITH DIFFERENT YEAR START VALUES
				 * NO YEAR START HERE - AS THE EXISTING ROW NEEDS TO BE UPDATED FOR THE CARLINE WITH NEW YEAR VALUES 
				 */
				String checkCarlineCodeSql="SELECT mdm_crln_name_regional_lang,mdm_crln_year_start,mdm_crln_flag,mdm_sync_status, "
						+ " mdm_crln_code_id,mdm_crln_notif_sent,mdm_crln_year_end_notif_only,"
						+ " mdm_crln_prev_year_end, mdm_crln_year_end, mdm_crln_notif_sent_tmstp  "
						+ "  FROM gms3_mdm_carline_codes "
						+ " WHERE TRIM(LOWER(mdm_crln_wmi_code)) = ? AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ? "
								+ " AND TRIM(LOWER(mdm_crln_code))=?  AND  mdm_crln_year_start = ? "
										+ " AND mdm_cl_id=? AND mdm_ml_id=? " 
								+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				
				pstmt = conn.prepareStatement(checkCarlineCodeSql);
				pstmt.setString(1, carDetails.getWmiCode().trim().toLowerCase());
				pstmt.setString(2, carDetails.getCarlineNameEng().trim().toLowerCase());
				pstmt.setString(3, carDetails.getCarlineCode().trim().toLowerCase());
				pstmt.setInt(4, yearStart);
				pstmt.setLong(5, carDetails.getCountryLocaleId());
				pstmt.setLong(6, carDetails.getManualLanguageId());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					autoCarlineCodeId = rs.getLong("mdm_crln_code_id");
					carlineNotificationSent = rs.getString("mdm_crln_notif_sent");
					yearEndNotificationOnly = rs.getString("mdm_crln_year_end_notif_only");
					previousYearEndNoticationValue = rs.getString("mdm_crln_prev_year_end");
					notifSentTime = rs.getTimestamp("mdm_crln_notif_sent_tmstp");
					if(rs.getInt("mdm_crln_year_end") > 0)
					{
						previousEndYear = String.valueOf(rs.getInt("mdm_crln_year_end"));
						oldYearEnd = String.valueOf(rs.getInt("mdm_crln_year_end"));
					}
					if(rs.getInt("mdm_crln_year_start")>0)
					{
						oldYearStart = String.valueOf(rs.getInt("mdm_crln_year_start"));
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang") && !"".equals(rs.getString("mdm_crln_name_regional_lang")))
					{
						oldcarlineNameReg = rs.getString("mdm_crln_name_regional_lang").trim();
					}
					oldFlag = rs.getString("mdm_crln_flag");
					syncStatus=  rs.getString("mdm_sync_status");
				}
				checkCarlineCodeSql = null;
				rs.close();rs=null;
				pstmt.close();pstmt= null;
				
				if(autoCarlineCodeId>0)
				{
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
					{
						carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
					}
					if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
						carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim().toUpperCase());
					}
					if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
					{
						carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim().toUpperCase());
					}
					
					
					/*
					 * CHECK IF CARLINE NOTIF FLAG IS NOT NULL & N - 
					 * DO NOT PERFORM BELOW STEPS AS IT IS NEW CARLINE AND NOTIFICATION HAS NOT BEEN SENT YET
					 * NO CHECK FOR YEAR START
					 * 
					 * IF YEAR END NOTIF IS NULL OR Y (FIRST TIME OR NOTIFICATION ALREADY SENT)
					 * 		IF CURRENT YEAR END IS GREATER THAN PREV YEAR END VALUE
					 * 			UPDATE YEAR END NOTIF FLAG - N
					 * 			SET NOTIF YEAR END VALUE - PREV YEAR END VALUE
					 * 		IF CURRENT YEAR END IS LESS THAN OR EQUALS TO PREV YEAR END VALUE
					 * 			DO NOTHING WITH NOTIF FLAGS / NOTIF YEAR END VALUE
					 * ELSE IF YEAR END NOTIF IS NOT NULL AND N (NOTIFICATION NOT YET SENT)
					 * 		IF CURRENT YEAR END IS GREATER THAN OR EQUALS TO PREV YEAR END VALUE
					 * 			DO NOTHING WITH NOTIF FLAGS / NOTIF YEAR END VALUE
					 * 		IF CURRENT YEAR END IS LESS THAN PREV YEAR END VALUE
					 * 			CHECK IF CURRENT YEAR END IS LESS THAN OR EQUALS TO NOTIF YEAR END VALUE
					 * 				SET NOTIF FLAG TO NULL
					 * 				SET NOTIF YEAR END VALUE TO NULL + TIMESTAMP AS WELL
					 * 			ELSE IF CURRENT YEAR END IS GREATER THEN NOTIF YEAR END VALUE
					 * 				DO NOTHIG WITH NOTIF FLAGS / NOTIF YEAR END VALUE
					 */
					
					boolean proceedFurther=true;
					if(null!=carlineNotificationSent && carlineNotificationSent.equals(ApplicationProperties.getProperty("value.esicategory.flag.no").trim()))
					{
						// DO NOT PROCEED AS NEW NOTIFICATION NOT SENT YET
						proceedFurther = false;
					}
					
					if(proceedFurther==true)
					{
						int previousYearEnd=0;
						if(null!=previousEndYear && !"".equals(previousEndYear))
						{
							previousYearEnd= new Integer(previousEndYear.trim()).intValue();
						}
						
						if(null!=yearEndNotificationOnly && yearEndNotificationOnly.equals(ApplicationProperties.getProperty("value.esicategory.flag.no").trim()))
						{
							// NOTIFICATION HAS NOT BEEN SENT YET
							if(yearEnd>0 && previousYearEnd>0)
							{
								if(yearEnd >= previousYearEnd)
								{
									// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP.
								}
								else if (yearEnd < previousYearEnd)
								{
									// CURRENT VALUE IS LESS THAN PREV VALUE
									if(null!=previousYearEndNoticationValue && !"".equals(previousYearEndNoticationValue))
									{
										int notifYearValue = new Integer(previousYearEndNoticationValue).intValue();
										if(notifYearValue>0)
										{
											if(yearEnd <= notifYearValue)
											{
												// CURRENT YEAR VALUE IS LESS THAN NOTIF YEAR VALUE - SET ALL FLAGS TO NULL
												previousYearEndNoticationValue = null;
												yearEndNotificationOnly = null;
												notifSentTime = null;
											}
											else if(yearEnd > notifYearValue)
											{
												// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP. 
											}
										}
									}
								}
							}
						}
						else
						{
							// EITHER FIRST TIME OR NOTIFICATION HAS ALREADY BEEN SENT
							if(yearEnd > previousYearEnd)
							{
								yearEndNotificationOnly=ApplicationProperties.getProperty("value.esicategory.flag.no").trim();
								previousYearEndNoticationValue = previousEndYear;
							}
							else
							{
								// CURRENT YEAR END IS LESS THAN PREV YEAR END
								// DO NOTHING WITH YEAR END NOTIF FLAG, NOTIF YEAR END VALUE & NOTIF SENT TIMESTAMP.
							}
						}
					}
					
					/*
					 * checks for sync status - 
					 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
						If oldStatus is Active - 
							Here, check for CARLINE NAME (REG),YEAR START, YEAR END CHANGES (because fetched using carlineEngName & carlineCode), 
							 	then update syncStatus as N
								else let it remains as it is
					 */
					if(oldFlag.equals(ApplicationProperties.getProperty("flag.value.active")))
					{
						if(oldcarlineNameReg.trim().toLowerCase().equals(carDetails.getCarlineNameReg().trim().toLowerCase()) && 
								oldYearStart.trim().toLowerCase().equals(carDetails.getYearStart().trim().toLowerCase()) && 
								oldYearEnd.trim().toLowerCase().equals(carDetails.getYearEnd().trim().toLowerCase()))
						{
							// NOTHING CHANGED - LET THE SYNC STATUS REMAINS AS IT IS
						}
						else
						{
							// SOMETHING CHANGED. SET SYNC STATUS AS = N
							syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
						}
					}
					
					/*
					 * INCASE OF UPDATE DO NOT UPDATE THE CRLN_FLAG
					 */
					// UPDATE THE CARLINE CODE ROW
					String sql="UPDATE  gms3_mdm_carline_codes SET mdm_crln_code = ? ,"
							+ " mdm_crln_year_start = ? ,mdm_crln_year_end = ? , "
							+ " mdm_crln_updated_tmstp = ? , mdm_crln_wmi_code = ?, mdm_cl_locale_code = ?, "
							+ " mdm_ml_lang_code = ?,"
							+ " mdm_crln_name_eng_lang = ?, mdm_crln_name_regional_lang = ?, mdm_crln_notif_sent = ? ,"
							+ " mdm_crln_year_end_notif_only = ? , mdm_crln_prev_year_end = ?, mdm_crln_notif_sent_tmstp = ?, "
							+ " mdm_ic_display_carline_code = ?,mdm_sync_status = ? WHERE mdm_crln_code_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, carDetails.getCarlineCode());
					pstmt.setInt(2, yearStart);
					pstmt.setInt(3, yearEnd);
					pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(5, carDetails.getWmiCode());
					pstmt.setString(6, carDetails.getCountryLocaleCode());
					pstmt.setString(7, carDetails.getManualLanguageCode());
					pstmt.setString(8, carDetails.getCarlineNameEng());
					pstmt.setString(9, carDetails.getCarlineNameReg());
					pstmt.setString(10, carlineNotificationSent);
					pstmt.setString(11, yearEndNotificationOnly);
					pstmt.setString(12, previousYearEndNoticationValue);
					pstmt.setTimestamp(13, notifSentTime);
					pstmt.setString(14, carDetails.getIcDisplayCarlineCode());
					pstmt.setString(15, syncStatus);
					pstmt.setLong(16, autoCarlineCodeId);
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt=null;
					
					carlineNotificationSent = null;
					yearEndNotificationOnly = null;
					previousYearEndNoticationValue = null;
					previousEndYear=null;
					notifSentTime=null;
					
					
					/*
					 * UPDATE CARLINE CODE, ENG NAME, REG NAME IN VIN DETAILS TABLE
					 */
					try
					{
						sql = "UPDATE gms3_mdm_vin_detail SET mdm_crln_code=?, mdm_crln_name_eng_lang=?, mdm_crln_name_regional_lang =?,mdm_ic_display_carline_code=?  "
								+ " WHERE mdm_crln_code=? AND mdm_crln_name_eng_lang=? AND mdm_crln_name_regional_lang =? AND mdm_cl_id=? AND mdm_ml_id=? AND mdm_vin_wmi_code = ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, carDetails.getCarlineCode());
						pstmt.setString(2, carDetails.getCarlineNameEng());
						pstmt.setString(3, carDetails.getCarlineNameReg());
						pstmt.setString(4, carDetails.getIcDisplayCarlineCode());
						pstmt.setString(5, carDetails.getCarlineCode());
						pstmt.setString(6, carDetails.getCarlineNameEng());
						pstmt.setString(7, oldcarlineNameReg);
						pstmt.setLong(8, carDetails.getCountryLocaleId());
						pstmt.setLong(9, carDetails.getManualLanguageId());
						pstmt.setString(10, carDetails.getWmiCode());
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql = null;
					}
					catch(Exception eq)
					{
						Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "importCarlineDetails()", eq);
					}
					
					
					oldcarlineNameReg = null;
					oldYearEnd=  null;
					oldYearStart = null;
					oldFlag = null;
					syncStatus=  null;
				}
				else 
				{
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
					{
						carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
					}
					if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
						carDetails.setCarlineNameEng(carDetails.getCarlineNameEng().trim().toUpperCase());
					}
					if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
					{
						carDetails.setCarlineNameReg(carDetails.getCarlineNameReg().trim().toUpperCase());
					}
					
					// HERE ALWAYS SET CARLINE NOTIFICATION SENT TO N
					
					// INSERT THE NEW ROW
					String sql="INSERT INTO gms3_mdm_carline_codes(mdm_crln_code,"
							+ " mdm_crln_year_start,mdm_crln_year_end,mdm_crln_flag,"
							+ " mdm_crln_created_tmstp,mdm_crln_wmi_code, mdm_cl_locale_code, mdm_ml_lang_code,"
							+ " mdm_crln_name_eng_lang, mdm_crln_name_regional_lang,mdm_cl_id, mdm_ml_id,mdm_crln_notif_sent,mdm_ic_display_carline_code) "
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, carDetails.getCarlineCode());
					pstmt.setInt(2, yearStart);
					pstmt.setInt(3, yearEnd);
					pstmt.setString(4, carDetails.getFlag());
					pstmt.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(6, carDetails.getWmiCode());
					pstmt.setString(7, carDetails.getCountryLocaleCode());
					pstmt.setString(8, carDetails.getManualLanguageCode());
					pstmt.setString(9, carDetails.getCarlineNameEng());
					pstmt.setString(10, carDetails.getCarlineNameReg());
					pstmt.setLong(11,carDetails.getCountryLocaleId());
					pstmt.setLong(12,carDetails.getManualLanguageId());
					pstmt.setString(13, ApplicationProperties.getProperty("value.esicategory.flag.no").trim());
					pstmt.setString(14, carDetails.getIcDisplayCarlineCode());
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt=null;
				}
			}
			else if(mnaoCountries==false)
			{
				/*
				 * IDENTIFY CARLINE CODE WHETHER EXISTS OR NOT. 
				 * E.G. CARLINE CODE, CARLINE NAME, WMI &  MODEL TYPE
				 * NO CHECK FOR REGIONAL & ESI CATEGORY FLAG
				 */
				String oldcarlineNameReg = "";
				String oldFlag = null;
				String syncStatus=  null;
				
				
				String checkCarlineCodeSql="SELECT mdm_crln_code_id,mdm_crln_name_regional_lang,mdm_crln_flag,mdm_sync_status FROM gms3_mdm_carline_codes "
						+ " WHERE TRIM(LOWER(mdm_crln_wmi_code)) = ? AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ? " 
						+ " AND TRIM(LOWER(mdm_crln_code))=?  "
						+ " AND TRIM(LOWER(mdm_crln_model_type)) = ?  AND mdm_cl_id =? AND mdm_ml_id=? "
								+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"')";
				pstmt = conn.prepareStatement(checkCarlineCodeSql);
				pstmt.setString(1, carDetails.getWmiCode().trim().toLowerCase());
				pstmt.setString(2, carDetails.getCarlineNameEng().trim().toLowerCase());
				pstmt.setString(3, carDetails.getCarlineCode().trim().toLowerCase());
				pstmt.setString(4, carDetails.getModelType().trim().toLowerCase());
				pstmt.setLong(5, carDetails.getCountryLocaleId());
				pstmt.setLong(6, carDetails.getManualLanguageId());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					autoCarlineCodeId = rs.getLong("mdm_crln_code_id");
					if(null!=rs.getString("mdm_crln_name_regional_lang") && !"".equals(rs.getString("mdm_crln_name_regional_lang")))
					{
						oldcarlineNameReg = rs.getString("mdm_crln_name_regional_lang").trim();
					}
					oldFlag = rs.getString("mdm_crln_flag");
					if(null!=rs.getString("mdm_sync_status") && !"".equals(rs.getString("mdm_sync_status")))
					{
						syncStatus= rs.getString("mdm_sync_status");
					}
				}
				checkCarlineCodeSql = null;
				rs.close();rs=null;
				pstmt.close();pstmt= null;
				
				if(autoCarlineCodeId>0)
				{
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
					{
						carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
					}
					
					/*
					 * checks for sync status - 
					 * 	If oldStatus is Draft - DO NOTHING WITH SYNC STATUS, let is remains as it is
						If oldStatus is Active - 
							Here, check for CARLINE NAME (REG) CHANGES (because fetched using carlineEngName & carlineCode), 
							 	then update syncStatus as N
								else let it remains as it is
					 */
					if(oldFlag.equals(ApplicationProperties.getProperty("flag.value.active")))
					{
						if(oldcarlineNameReg.trim().toLowerCase().equals(carDetails.getCarlineNameReg().trim().toLowerCase()))
						{
							// NOTHING CHANGED - LET THE SYNC STATUS REMAINS AS IT IS
						}
						else
						{
							// SOMETHING CHANGED. SET SYNC STATUS AS = N
							syncStatus=  AccessManagementInterface.SYNC_STATUS_NO;
						}
					}
					
					
					/*
					 * NO NEED TO DO ANYTHING WITH NOTIFICATION FLAG HERE
					 * ALSO DO NOT UPDATE THE CARLINE FLAG
					 */
					
					// UPDATE THE CARLINE CODE ROW
					String sql="UPDATE  gms3_mdm_carline_codes SET mdm_crln_code = ? ,"
							+ " mdm_crln_updated_tmstp = ? , mdm_crln_wmi_code = ?, mdm_cl_locale_code = ?, "
							+ " mdm_ml_lang_code = ?,"
							+ " mdm_crln_name_eng_lang = ?, mdm_crln_name_regional_lang = ?, mdm_crln_model_type = ? , "
							+ " mdm_crln_esi_cat_flag = ?, mdm_ic_display_carline_code = ?, mdm_sync_status = ?   "
							+ " WHERE mdm_crln_code_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, carDetails.getCarlineCode());
					pstmt.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, carDetails.getWmiCode());
					pstmt.setString(4, carDetails.getCountryLocaleCode());
					pstmt.setString(5, carDetails.getManualLanguageCode());
					pstmt.setString(6, carDetails.getCarlineNameEng());
					pstmt.setString(7, carDetails.getCarlineNameReg());
					pstmt.setString(8, carDetails.getModelType());
					pstmt.setString(9, carDetails.getEsiCategoryFlag());
					pstmt.setString(10, carDetails.getIcDisplayCarlineCode());
					pstmt.setString(11, syncStatus);
					pstmt.setLong(12, autoCarlineCodeId);
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt=null;
					
					
					/*
					 * UPDATE CARLINE CODE, ENG NAME, REG NAME IN VIN DETAILS TABLE
					 */
					try
					{
						sql = "UPDATE gms3_mdm_vin_detail SET mdm_crln_code=?, mdm_crln_name_eng_lang=?, mdm_crln_name_regional_lang =?,mdm_ic_display_carline_code=?  "
								+ " WHERE mdm_crln_code=? AND mdm_crln_name_eng_lang=? AND mdm_crln_name_regional_lang =? AND mdm_cl_id=? AND mdm_ml_id=? AND mdm_vin_wmi_code = ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, carDetails.getCarlineCode());
						pstmt.setString(2, carDetails.getCarlineNameEng());
						pstmt.setString(3, carDetails.getCarlineNameReg());
						pstmt.setString(4, carDetails.getIcDisplayCarlineCode());
						pstmt.setString(5, carDetails.getCarlineCode());
						pstmt.setString(6, carDetails.getCarlineNameEng());
						pstmt.setString(7, oldcarlineNameReg);
						pstmt.setLong(8, carDetails.getCountryLocaleId());
						pstmt.setLong(9, carDetails.getManualLanguageId());
						pstmt.setString(10, carDetails.getWmiCode());
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
						sql = null;
					}
					catch(Exception eq)
					{
						Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "importCarlineDetails()", eq);
					}
					oldcarlineNameReg=  null;
					oldFlag= null;
					syncStatus= null;
				}
				else 
				{
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
					{
						carDetails.setCarlineCode(carDetails.getCarlineCode().trim().toUpperCase());
					}
					
					// SET NOTIFICATION FLAG TO N
					
					// INSERT THE NEW ROW
					String sql="INSERT INTO gms3_mdm_carline_codes(mdm_crln_code,"
							+ " mdm_crln_flag,"
							+ " mdm_crln_created_tmstp,mdm_crln_wmi_code, mdm_cl_locale_code, mdm_ml_lang_code,"
							+ " mdm_crln_name_eng_lang, mdm_crln_name_regional_lang, mdm_crln_model_type,mdm_crln_esi_cat_flag ,"
							+ " mdm_cl_id, mdm_ml_id,mdm_crln_notif_sent,mdm_ic_display_carline_code) "
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, carDetails.getCarlineCode());
					pstmt.setString(2, carDetails.getFlag());
					pstmt.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(4, carDetails.getWmiCode());
					pstmt.setString(5, carDetails.getCountryLocaleCode());
					pstmt.setString(6, carDetails.getManualLanguageCode());
					pstmt.setString(7, carDetails.getCarlineNameEng());
					pstmt.setString(8, carDetails.getCarlineNameReg());
					pstmt.setString(9, carDetails.getModelType());
					pstmt.setString(10, carDetails.getEsiCategoryFlag());
					pstmt.setLong(11, carDetails.getCountryLocaleId());
					pstmt.setLong(12, carDetails.getManualLanguageId());
					pstmt.setString(13, ApplicationProperties.getProperty("value.esicategory.flag.no").trim());
					pstmt.setString(14, carDetails.getIcDisplayCarlineCode());
					pstmt.executeUpdate();
					sql = null;
					pstmt.close();pstmt=null;
				}
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
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "importCarlineDetails()", e);
			return false;
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			// set passed param to null
			carDetails = null;
		}
		logger.info("importCarlineDetails :: Method Ends.");
		return true;
	}

	public static ArrayList<CarlineDetails> getModelDetailsListForSSTCombo(String languageId) throws SQLException 
	{
//		logger.info("getModelDetailsList :: Method Starts.");
		ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId))
			{
				conn = getConnection();
				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name FROM "
						+ " gms3_sst_div_model_mapping WHERE "
						+ " mdm_divmm_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"')   ";
				if(null!=languageId && !"".equals(languageId) && !"null".equals(languageId.trim().toLowerCase()))
				{
					sql = sql+" AND mdm_ml_id="+new Long(languageId).longValue();
				}
				sql = sql+"  GROUP BY mdm_crln_code, mdm_crln_name ORDER BY mdm_crln_code,mdm_crln_name ASC";
//				logger.info("getModelDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails details = new CarlineDetails();
					details.setCarlineCode(rs.getString("mdm_crln_code"));
					details.setCarlineNameEng(rs.getString("mdm_crln_name"));
//					String comboLabel = "";
//					if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
//					{
//						comboLabel = details.getCarlineNameEng();
//					}
//					if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
//					{
//						comboLabel = comboLabel+ "("+details.getCarlineCode()+")";
//					}
//					details.setCarlineIdForCombo(comboLabel);
					modelsList.add(details);
					details=  null;
//					comboLabel= null;
				}
				sql = null;
				
			}
			else
			{
				logger.info("getModelDetailsList :: Language id as Parameters is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getModelDetailsList()", e);
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
		return modelsList;
	}
	
	
	public static ArrayList<CarlineDetails> getCarlineDetailsListForComboFORDivisionModelMapping(String langCode) throws SQLException 
	{
//		logger.info("getCarlineDetailsListForCombo :: Method Starts.");
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode))
			{
				conn = getConnection();
				/*
				 * GET UNIQUE CARLINE CODES 
				 * AND FETCH ALL APPLICABLE NAMES FOR THOSE CARLINE CODES. THIS WAY MULTIPLE ENG NAMES CAN BE SHOWN
				 * AND NO DEPENDENCY FOR THE CARLINE ENG NAME HAS TO BE INTRODUCED IN DIVISION MODEL MAPPING
				 */
				String sql="SELECT DISTINCT mdm_crln_code FROM "
						+ " gms3_mdm_carline_codes  "
						+ " WHERE mdm_ml_id="+new Long(langCode).longValue()+" "
						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ " ORDER BY mdm_crln_code ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
					if(null!=rs.getString("mdm_crln_code"))
					{
						carDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					carlineList.add(carDetails);
					carDetails = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
				
				/*
				 * GET ALL APPLICABLE NAMES FOR THE CARLINE CODE - 
				 */
				if(null!=carlineList && carlineList.size()>0)
				{
					for(CarlineDetails details : carlineList)
					{
						if(null!=details.getCarlineCode())
						{
							String carlineName="";
							sql = "SELECT DISTINCT mdm_crln_name_eng_lang FROM gms3_mdm_carline_codes WHERE "
									+ "mdm_ml_id="+new Long(langCode).longValue()+"  AND  "
									+ "mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
									+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
									+ "AND TRIM(LOWER(mdm_crln_code))='"+details.getCarlineCode().trim().toLowerCase()+"'";
							stmt = conn.createStatement();
							rs = stmt.executeQuery(sql);
							while(rs.next())
							{
								if(null!=rs.getString("mdm_crln_name_eng_lang"))
								{
									carlineName = carlineName+rs.getString("mdm_crln_name_eng_lang")+" / ";
								}
							}
							stmt.close();stmt=null;
							rs.close();rs=null;
							sql = null;
							if(null!=carlineName && !"".equals(carlineName))
							{
								carlineName = carlineName.trim();
								if(carlineName.trim().endsWith("/"))
								{
									carlineName = carlineName.substring(0, carlineName.length()-1);
								}
							}
							//set inList
							details.setCarlineNameEng(carlineName);
							carlineName = null;
						}
					}
				}
			}
			else
			{
				logger.info("getCarlineDetailsListForComboFORDivisionModelMapping :: Language Code as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CarlineDAO.class.getName(), "getCarlineDetailsListForComboFORDivisionModelMapping()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			langCode = null;
		}
//		logger.info("getCarlineDetailsListForCombo :: Method Ends.");
		return carlineList;
	}


	/**
	 * Locate the row an imported line refers to, using EXACTLY the unique combination the
	 * create-or-update path uses. The import Excel carries no primary key, so this is how
	 * an ACTION=D row is matched. Rows already deleted are excluded, so deleting the same
	 * row twice correctly reports 'nothing to delete'.
	 *
	 * CARLINE HAS TWO UNIQUE COMBINATIONS, exactly as importCarlineDetails() does:
	 *   MNAO markets  -> ... AND mdm_crln_year_start = ?
	 *   other markets -> ... AND TRIM(LOWER(mdm_crln_model_type)) = ?
	 * The caller passes the same isMnaoContent() flag the import path uses; keying on the
	 * wrong column would match nothing, or the wrong row.
	 *
	 * @return the primary key, or 0 when there is no active matching row.
	 */
	public static long findExistingIdForImport(CarlineDetails details, Connection conn,
			boolean mnaoContent) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long existingId = 0;
		try {
			if (null == conn || conn.isClosed() == true) {
				conn = getConnection();
			}
			String checkCarlineCodeSql = null;
			if (mnaoContent == true) {
				int yearStart = 0;
				if (null != details.getYearStart() && !"".equals(ImportActionUtils.safeTrim(details.getYearStart()))) {
					yearStart = new Integer(ImportActionUtils.safeTrim(details.getYearStart())).intValue();
				}
				checkCarlineCodeSql = "SELECT mdm_crln_code_id FROM gms3_mdm_carline_codes "
						+ " WHERE TRIM(LOWER(mdm_crln_wmi_code)) = ? AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ? "
						+ " AND TRIM(LOWER(mdm_crln_code))=?  AND  mdm_crln_year_start = ? "
						+ " AND mdm_cl_id=? AND mdm_ml_id=? "
						+ " AND mdm_crln_flag NOT IN ('" + ApplicationProperties.getProperty("flag.value.delete") + "')";
				pstmt = conn.prepareStatement(checkCarlineCodeSql);
				pstmt.setString(1, ImportActionUtils.safeLower(details.getWmiCode()));
				pstmt.setString(2, ImportActionUtils.safeLower(details.getCarlineNameEng()));
				pstmt.setString(3, ImportActionUtils.safeLower(details.getCarlineCode()));
				pstmt.setInt(4, yearStart);
				pstmt.setLong(5, details.getCountryLocaleId());
				pstmt.setLong(6, details.getManualLanguageId());
			} else {
				checkCarlineCodeSql = "SELECT mdm_crln_code_id FROM gms3_mdm_carline_codes "
						+ " WHERE TRIM(LOWER(mdm_crln_wmi_code)) = ? AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ? "
						+ " AND TRIM(LOWER(mdm_crln_code))=?  "
						+ " AND TRIM(LOWER(mdm_crln_model_type)) = ?  AND mdm_cl_id =? AND mdm_ml_id=? "
						+ " AND mdm_crln_flag NOT IN ('" + ApplicationProperties.getProperty("flag.value.delete") + "')";
				pstmt = conn.prepareStatement(checkCarlineCodeSql);
				pstmt.setString(1, ImportActionUtils.safeLower(details.getWmiCode()));
				pstmt.setString(2, ImportActionUtils.safeLower(details.getCarlineNameEng()));
				pstmt.setString(3, ImportActionUtils.safeLower(details.getCarlineCode()));
				pstmt.setString(4, null == details.getModelType() ? ""
						: ImportActionUtils.safeLower(details.getModelType()));
				pstmt.setLong(5, details.getCountryLocaleId());
				pstmt.setLong(6, details.getManualLanguageId());
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				existingId = rs.getLong("mdm_crln_code_id");
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