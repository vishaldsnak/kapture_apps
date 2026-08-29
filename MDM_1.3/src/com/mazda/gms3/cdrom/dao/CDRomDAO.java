package com.mazda.gms3.cdrom.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.cdrom.bean.ApplicableVINList;
import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.bean.CDRomSettingDetails;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.VinDetails;

public class CDRomDAO extends DBConnectionHelper {

	static Logger logger = LogManager.getLogger(CDRomDAO.class);

	public static List<LabelBean> getModelsList(String language) throws SQLException {
		List<LabelBean> models = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			//			String sql = "SELECT DISTINCT mdm_crln_code,mdm_crln_name_regional_lang,mdm_crln_model_type"
			//					+ "  FROM gms3_mdm_carline_codes WHERE mdm_ml_lang_code='"
			//					+ language.trim()
			//					+ "' AND mdm_crln_flag NOT IN ('"
			//					+ ApplicationProperties.getProperty("flag.value.delete")
			//					+ "', '"
			//					+ ApplicationProperties.getProperty("flag.value.draft")
			//					+ "') ";
			String sql = "SELECT DISTINCT mdm_crln_code,mdm_crln_name_eng_lang,mdm_crln_name_regional_lang "
					+ "  FROM gms3_mdm_carline_codes WHERE mdm_ml_lang_code='"
					+ language.trim()
					+ "' AND mdm_crln_flag ='"
					+ ApplicationProperties.getProperty("flag.value.active")
					+ "'";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				LabelBean clDetails = new LabelBean();
				/*
				 * SET COMBINATION OF BOTH MODEL CARLINE CODE AND CARLINE NAME
				 */
				//				clDetails.setKey(rs.getString("mdm_crln_code").trim());
				clDetails.setKey(rs.getString("mdm_crln_code").trim()+"_"+rs.getString("mdm_crln_name_eng_lang").trim());
				clDetails.setValue(rs.getString("mdm_crln_name_regional_lang").trim()
						+ "(" + rs.getString("mdm_crln_code").trim() + ")");
				clDetails.setExtraAttribute(rs.getString("mdm_crln_name_regional_lang").trim());
				//				clDetails.setModelTypeForCarline(rs.getString("mdm_crln_model_type").trim());
				models.add(clDetails);
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getModelsList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return models;
	}

	public static List<LabelBean> getWmiList(String language, String model) throws SQLException {
		List<LabelBean> wmi = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			/*
			 * break MODEL KEY AS It is combination of both CARLINE CODE AND CARLINE NAME
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=model && !"".equals(model))
				{
					carlineCode=  model.substring(0, model.indexOf("_"));
					modelName = model.substring(model.indexOf("_")+1, model.length());
				}
			}
			catch(Exception e){}

			conn = getConnection();
			String sql = "SELECT distinct mdm_vin_wmi_code FROM gms3_mdm_vin_detail WHERE  mdm_ml_lang_code='"
					+ language.trim()
					+ "' AND mdm_crln_code='"
					+ carlineCode.trim()
					+ "'"
					+ " AND mdm_crln_name_eng_lang='"+modelName.trim()+"' AND mdm_vin_flag NOT IN ('"
					+ ApplicationProperties.getProperty("flag.value.delete")
					+ "', '"
					+ ApplicationProperties.getProperty("flag.value.draft")
					+ "') ";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				LabelBean clDetails = new LabelBean();
				clDetails.setKey(rs.getString("mdm_vin_wmi_code").trim());
				clDetails.setValue(rs.getString("mdm_vin_wmi_code").trim());
				wmi.add(clDetails);
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getWmiList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return wmi;
	}

	public static List<LabelBean> getVdsList(String language, String model, String wmi) throws SQLException {
		List<LabelBean> vds = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			/*
			 * break MODEL KEY AS It is combination of both CARLINE CODE AND CARLINE NAME
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=model && !"".equals(model))
				{
					carlineCode=  model.substring(0, model.indexOf("_"));
					modelName = model.substring(model.indexOf("_")+1, model.length());
				}
			}
			catch(Exception e){}

			conn = getConnection();
			String sql = "SELECT distinct mdm_vin_vds_code FROM gms3_mdm_vin_detail WHERE mdm_ml_lang_code='"
					+ language.trim()
					+ "' AND mdm_crln_code='"
					+ carlineCode.trim()
					+ "' AND mdm_vin_wmi_code='"
					+ wmi.trim()
					+ "'"
					+ " AND mdm_crln_name_eng_lang='"+modelName+"' AND mdm_vin_flag NOT IN ('"
					+ ApplicationProperties.getProperty("flag.value.delete")
					+ "', '"
					+ ApplicationProperties.getProperty("flag.value.draft")
					+ "') ";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				LabelBean clDetails = new LabelBean();
				clDetails.setKey(rs.getString("mdm_vin_vds_code").trim());

				clDetails.setValue(rs.getString("mdm_vin_vds_code").trim());
				vds.add(clDetails);
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getVdsList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return vds;
	}

	public static List<LabelBean> getVinRangeList(String language, String model, String wmi, String vds)
			throws SQLException {
		List<LabelBean> vinRange = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {

			/*
			 * break MODEL KEY AS It is combination of both CARLINE CODE AND CARLINE NAME
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=model && !"".equals(model))
				{
					carlineCode=  model.substring(0, model.indexOf("_"));
					modelName = model.substring(model.indexOf("_")+1, model.length());
				}
			}
			catch(Exception e){}

			conn = getConnection();
			String sql = "SELECT distinct mdm_vin_vis_start_range FROM gms3_mdm_vin_detail WHERE mdm_ml_lang_code='"
					+ language.trim()
					+ "' AND mdm_crln_code='"
					+ carlineCode.trim()
					+ "' AND mdm_vin_wmi_code='"
					+ wmi.trim()
					+ "' AND mdm_vin_vds_code='"
					+ vds.trim()
					+ "'"
					+ " AND mdm_crln_name_eng_lang='"+modelName.trim()+"' AND mdm_vin_flag NOT IN ('"
					+ ApplicationProperties.getProperty("flag.value.delete")
					+ "', '"
					+ ApplicationProperties.getProperty("flag.value.draft")
					+ "') ";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				LabelBean clDetails = new LabelBean();
				clDetails.setKey(String.valueOf(rs
						.getString("mdm_vin_vis_start_range").trim()));
				clDetails.setValue(rs.getString("mdm_vin_vis_start_range").trim());
				vinRange.add(clDetails);
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getVinRangeList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return vinRange;
	}

	public static List<LabelBean> getServiceContentList_old(String language, String carlineCode, String wmiCode, String vdsCode, String vinRange) throws SQLException {
		List<LabelBean> serviceContent = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if(null!=language && !"".equals(language) && null!=carlineCode && !"".equals(carlineCode) && null!=wmiCode && !"".equals(wmiCode) && 
					null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				conn = getConnection();
				ArrayList<LabelBean> uniqueManualTypeList = new ArrayList<LabelBean>();

				String newModelKey = "\\"+ApplicationProperties.getProperty("cdrom.model.type.folder.key.newm");
				String tableName="gms3_dmt_mc_newm_vin,gms3_dmt_mc_vin";
				String[] tabTokens=tableName.split(",");

				ArrayList<String> uniqueFaceLiftFolderList = new ArrayList<String>();
				/*
				 * IDENTIFY FACELIFT FOLDER PATHS FOR THE VIN PASSED 
				 * CHECK, WHETHER CD ROM DATA EXISTS FOR ALL THE FACELIFT FOLDER OR NOT. 
				 * IF EXISTS, THEN ADD THE MANUAL TYPE FOR THE DEFAULT VIN AS SERVICE MANUAL TYPE 
				 */
				String identifyUniqueFaceLiftFolderSql = null;
				for(int a=0;a<tabTokens.length;a++)
				{
					// retrieve from OLD / NEW MC TABLES
					identifyUniqueFaceLiftFolderSql="SELECT DISTINCT dc_vin_txt_file_path FROM "+tabTokens[a].toString()+" WHERE "
							+ " TRIM(LOWER(dc_vin_carline_code))='"+carlineCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(dc_vin_vds_code))='"+vdsCode.trim().toLowerCase()+"' AND "
							+ " TRIM(LOWER(dc_vin_vis_start_range))='"+vinRange.trim().toLowerCase()+"'";

					logger.info("getServiceContentList :: identifyUniqueFaceLiftFolderSql :: > "+identifyUniqueFaceLiftFolderSql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(identifyUniqueFaceLiftFolderSql);
					while(rs.next())
					{
						String path = rs.getString("dc_vin_txt_file_path");
						if(null!=path && !"".equals(path))
						{
							// TAKE THE VALUE FROM LOCALE, REMOVE STRING BEFORE IT
							if(path.trim().toLowerCase().indexOf(language.trim().toLowerCase())!=-1)
							{
								path= path.substring(path.trim().toLowerCase().indexOf(language.trim().toLowerCase()), path.length());
							}
							if(null!=path && !"".equals(path))
							{
								path = path.trim();
								// REMOVE VIN FILE NAME
								if(path.lastIndexOf("\\")!=-1)
								{
									path = path.substring(0,path.lastIndexOf("\\"));
								}

								if(null!=path && !"".equals(path))
								{
									// BEFORE ADDING CHECK IF ALREADY ADDED IN THE LIST
									boolean addToList = true;
									if(null!=uniqueFaceLiftFolderList && uniqueFaceLiftFolderList.size()>0)
									{
										for(String existingPath : uniqueFaceLiftFolderList)
										{
											if(existingPath.trim().toLowerCase().equals(path.trim().toLowerCase()))
											{
												// ALREADY EXISTS - DO NOT ADD
												addToList = false;
												break;
											}
											existingPath = null;
										}
									}

									if(addToList == true)
									{
										uniqueFaceLiftFolderList.add(path);
									}
								}
							}
						}
						path = null;
					}
					identifyUniqueFaceLiftFolderSql = null;
					rs.close();rs=null;
					stmt.close();stmt=null;
				}


				if(null!=uniqueFaceLiftFolderList && uniqueFaceLiftFolderList.size()>0)
				{
					// ITERATE AND CHECK WHETHER THE CDROM DATA EXISTS FOR THE FACELIFT FOLDER OR NOT, ALSO READ THE MANUAL TYPE FOLDER NAME
					String checkCDRomExistsSql = null;
					String cdRomTableName=null;
					for(String faceLiftFolderPath : uniqueFaceLiftFolderList)
					{
						if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath))
						{
							if(faceLiftFolderPath.trim().toLowerCase().contains(newModelKey.trim().toLowerCase()))
							{
								// NEW TABLES
								cdRomTableName = "gms3_dmt_mc_newm_cd_data";
							}
							else
							{
								// OLD TABLES
								cdRomTableName = "gms3_dmt_mc_cd_data";
							}
						}
						if(null!=cdRomTableName && !"".equals(cdRomTableName))
						{
							checkCDRomExistsSql="SELECT DISTINCT dc_cd_manualtype_folder FROM "+cdRomTableName+" WHERE "
									+ " TRIM(LOWER(dc_cd_txt_file_path)) LIKE '%"+faceLiftFolderPath.trim().toLowerCase()+"%'";
							logger.info("getServiceContentList :: checkCDRomExistsSql :: > " + checkCDRomExistsSql);
							stmt = conn.createStatement();
							rs = stmt.executeQuery(checkCDRomExistsSql);
							while(rs.next())
							{
								String docType = rs.getString("dc_cd_manualtype_folder");
								if(null!=docType && !"".equals(docType))
								{
									docType = docType.trim();
									boolean addToList = true;
									if(null!=uniqueManualTypeList && uniqueManualTypeList.size()>0)
									{
										for(LabelBean exist : uniqueManualTypeList)
										{
											if(exist.getKey().trim().toLowerCase().equals(docType.trim().toLowerCase()))
											{
												// ALREADY ADDED
												addToList = false;
												break;
											}
											exist=  null;
										}
									}

									if(addToList == true)
									{
										LabelBean lb = new LabelBean();
										lb.setKey(docType.trim());
										lb.setExtraAttribute(faceLiftFolderPath.trim());
										uniqueManualTypeList.add(lb);
										lb = null;
									}
									docType = null;
								}
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							checkCDRomExistsSql=  null;
						}
						faceLiftFolderPath=  null;
						cdRomTableName = null;
					}
					cdRomTableName = null;
				}

				if(null!=uniqueManualTypeList && uniqueManualTypeList.size()>0)
				{
					language = language.replace("_", "-");
					for(LabelBean lb : uniqueManualTypeList)
					{
						String getManualTypeSql="SELECT DISTINCT dc_manual_code, dc_manual_name FROM gms3_dmt_conv_manual_type WHERE"
								+ " TRIM(mdm_ml_lang_code)='"+language.trim()+"' "
								+ "AND TRIM(dc_manual_code)='"+lb.getKey().trim()+"' AND dc_manual_flag NOT IN ('"
								+ ApplicationProperties.getProperty("flag.value.delete")
								+ "', '"
								+ ApplicationProperties.getProperty("flag.value.draft")
								+ "')";
						logger.info("getServiceContentList :: getManualTypeSql :: > " + getManualTypeSql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(getManualTypeSql);
						if(rs.next())
						{
							LabelBean bean = new LabelBean();
							bean.setKey(rs.getString("dc_manual_code").trim());
							bean.setValue(rs.getString("dc_manual_name").trim());
							// FACELIFT FOLDER PATH
							bean.setExtraAttribute(lb.getExtraAttribute());
							// add to serviceContentList
							serviceContent.add(bean);
							bean = null;
						}
						getManualTypeSql=  null;
						rs.close();rs=null;
						stmt.close();stmt = null;
					}
				}

				uniqueManualTypeList = null;
				uniqueFaceLiftFolderList = null;
				if(null!=serviceContent && serviceContent.size()>0)
				{
					logger.info("getServiceContentList :: Total Service Manual Types Identified for "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+" are :: > " + serviceContent.size());
				}
				else
				{
					logger.info("getServiceContentList :: No Service Manual Types Identified for "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+".");
				}
				tableName = null;
				tabTokens = null;
				newModelKey = null;
			}
			else
			{
				logger.info("getServiceContentList :: Locale, Carline Code, WMI, VDS & VIN Range for Identifiying Service Manual Types are Null.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "getServiceContentList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			language=  null;
			wmiCode=  null;
			vdsCode = null;
			vinRange=  null;
		}
		return serviceContent;
	}

	public static List<LabelBean> getServiceContentList(String language, String modelName, String carlineCode, String wmiCode, String vdsCode, String vinRange) throws SQLException {
		List<LabelBean> serviceContent = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if(null!=language && !"".equals(language) && null!=modelName && !"".equals(modelName) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=wmiCode && !"".equals(wmiCode) && 
					null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				conn = getConnection();
				ArrayList<LabelBean> uniqueManualTypeList = new ArrayList<LabelBean>();
				String tableName=null;
				/*
				 * CHECK IF MC LOCALE - THEN FETCH FROM MC VIEW CONTENT TABLE
				 * ELSE IF MME LOCALE - 
				 * 	THEN FETCH FROM LOCALE SPECIFIC VIEW CONTENT TABLE
				 */
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				String sql="SELECT DISTINCT vc_vin_document_type, vc_vin_document_type_name from  "+tableName+" WHERE "
						+ " vc_vin_document_type NOT IN ('"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")+"','"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"',"
								+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"','"+ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY")+"',"
								+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY")+"','"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY")+"') "
						+ " AND vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
						+ carlineCode.trim()
						+ "' AND vc_vin_wmi_code='"
						+ wmiCode.trim()
						+ "' AND vc_vin_vds_code='"
						+ vdsCode.trim()
						+ "' AND vc_vin_vis_start_range='"
						+ vinRange.trim()
						+ "'";
				logger.info("getServiceContentList ::  Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				LabelBean details = null;
				while(rs.next())
				{
					if(null!=rs.getString("vc_vin_document_type") && !"".equals(rs.getString("vc_vin_document_type")) 
							&& null!=rs.getString("vc_vin_document_type_name") && !"".equals(rs.getString("vc_vin_document_type_name")))
					{
						details = new LabelBean();
						details.setKey(rs.getString("vc_vin_document_type").trim());
						details.setValue(rs.getString("vc_vin_document_type_name").trim());
						uniqueManualTypeList.add(details);
						details = null;
					}
				}
				tableName=  null;
				if(null!=uniqueManualTypeList && uniqueManualTypeList.size()>0)
				{
					language = language.replace("_", "-");
					String getManualTypeSql = null;
					for(LabelBean lb : uniqueManualTypeList)
					{
						if(lb.getKey().equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_WD_REF_KEY")))
						{
							// USE MANUAL NAME AS WELL TO IDENTIFY MANUAL TYPE CODE AND NAME FOR WD / E-WD
							getManualTypeSql="SELECT DISTINCT dc_manual_code, dc_manual_name FROM gms3_dmt_conv_manual_type WHERE"
									+ " TRIM(mdm_ml_lang_code)='"+language.trim()+"' "
									+ "AND TRIM(dc_manual_ref_key)='"+lb.getKey().trim()+"' AND TRIM(dc_manual_name)='"+lb.getValue().trim()+"' AND dc_manual_flag ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
						}
						else
						{
							getManualTypeSql="SELECT DISTINCT dc_manual_code, dc_manual_name FROM gms3_dmt_conv_manual_type WHERE"
									+ " TRIM(mdm_ml_lang_code)='"+language.trim()+"' "
									+ "AND TRIM(dc_manual_ref_key)='"+lb.getKey().trim()+"' AND dc_manual_flag ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
						}

						logger.info("getServiceContentList :: getManualTypeSql :: > " + getManualTypeSql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(getManualTypeSql);
						LabelBean bean = null;
						if(rs.next())
						{
							bean = new LabelBean();
							bean.setKey(rs.getString("dc_manual_code").trim());
							bean.setValue(rs.getString("dc_manual_name").trim());
							// MANUAL CODE REF KEY
							bean.setExtraAttribute(lb.getKey());
							// add to serviceContentList
							serviceContent.add(bean);
							bean = null;
						}
						getManualTypeSql=  null;
						rs.close();rs=null;
						stmt.close();stmt = null;
						bean = null;
					}
				}

				uniqueManualTypeList = null;
				if(null!=serviceContent && serviceContent.size()>0)
				{
					logger.info("getServiceContentList :: Total Service Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+" are :: > " + serviceContent.size());
				}
				else
				{
					logger.info("getServiceContentList :: No Service Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+".");
				}
			}
			else
			{
				logger.info("getServiceContentList :: Locale, Model Name, Carline Code, WMI, VDS & VIN Range for Identifiying Service Manual Types are Null.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "getServiceContentList()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			language=  null;
			wmiCode=  null;
			vdsCode = null;
			vinRange=  null;
		}
		return serviceContent;
	}


	public static List<LabelBean> getEngineWorkshopManualList_old(String language, ArrayList<ApplicableVINList> applicableVINList) throws SQLException 
	{
		List<LabelBean> engineManualList = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try 
		{
			if(null!=language && !"".equals(language) && null!=applicableVINList && applicableVINList.size()>0)
			{
				language = language.replace("-", "_");
				conn = getConnection();

				for(ApplicableVINList details : applicableVINList)
				{
					String sql = "Select DISTINCT  vc_vin_engine_book_name, vc_vin_engine_book_code from gms3_vc_japan_vin_details where "
							+ " vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")+"' "
							+ "AND vc_vin_locale='"
							+ language.trim()
							+ "' AND vc_vin_carline_code='"
							+ details.getCarlineCode().trim()
							+ "' AND vc_vin_wmi_code='"
							+ details.getWmiCode().trim()
							+ "' AND vc_vin_vds_code='"
							+ details.getVdsCode().trim()
							+ "' AND vc_vin_vis_start_range='"
							+ details.getVisStartRange().trim()
							+ "'";
					logger.info("getEngineWorkshopManualList :: Sql :: >"+ sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while (rs.next()) 
					{
						LabelBean clDetails = new LabelBean();
						clDetails.setKey(rs.getString("vc_vin_engine_book_code").trim());
						clDetails.setValue(rs.getString("vc_vin_engine_book_name").trim());

						boolean addToList = true;
						if(null!=engineManualList && engineManualList.size()>0)
						{
							for(LabelBean exist : engineManualList)
							{
								if(exist.getKey().equals(clDetails.getKey()))
								{
									// ALREADY ADDED
									addToList = false;
									break;
								}
								exist=  null;
							}
						}
						if(addToList == true)
						{
							engineManualList.add(clDetails);
						}
						clDetails = null;
					}
					sql = null;
					rs.close();rs=null;
					stmt.close();stmt = null;
					details=  null;
				}

				if(null!=engineManualList && engineManualList.size()>0)
				{
					logger.info("getEngineWorkshopManualList :: Total Engine Workshop Manual Types Identified are :: > " + engineManualList.size());
				}
				else
				{
					logger.info("getEngineWorkshopManualList :: No Engine Workshop Manual Types Identified for the Applicable VINs.");
				}
				language= null;
			}
			else
			{
				logger.info("getEngineWorkshopManualList :: Locale & Applicable VIN List for Identifiying Engine Workshop Manual Types are Null.");
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getEngineWorkshopManualList()", e);
		}
		finally 
		{
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return engineManualList;
	}
	
	/**
	 * The function is going to change 26-MARCH-2022
	 * NEW LOGIC INTRODUCED BY MC TEAM
	 * 1.) FETCH FACELIFT FOLDER ON THE BASIS OF VIN
	 * 2.) IDENTIFY APPLICABLE FOR FACELIFT FOLDER
	 * @param language
	 * @param modelName
	 * @param carlineCode
	 * @param wmiCode
	 * @param vdsCode
	 * @param vinRange
	 * @return
	 * @throws SQLException
	 */
	public static List<LabelBean> getEngineWorkshopManualList(String language, String modelName,String carlineCode, String wmiCode, String vdsCode, String vinRange) throws SQLException 
	{
		List<LabelBean> engineManualList = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try 
		{
			if(null!=language && !"".equals(language) && null!=modelName && !"".equals(modelName) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=wmiCode && !"".equals(wmiCode) && 
					null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				language = language.replace("-", "_");
				conn = getConnection();

				/*
				 * IN-191220-2968 DATE - 08 MARCH 2020
				 * FETCH ENGINE BOOK DATA ON THE BASIS OF MODEL ONLY - NO VIN
				 */
				
//				String sql = "Select DISTINCT  vc_vin_engine_book_name, vc_vin_engine_book_code from gms3_vc_japan_vin_details where "
//						+ " vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")+"' "
//						+ " AND vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//						+ carlineCode.trim()
//						+ "' AND vc_vin_wmi_code='"
//						+ wmiCode.trim()
//						+ "' AND vc_vin_vds_code='"
//						+ vdsCode.trim()
//						+ "' AND vc_vin_vis_start_range='"
//						+ vinRange.trim()
//						+ "'";
				
				/*
				 * CHECK IF MC LOCALE - THEN FETCH FROM MC VIEW CONTENT TABLE
				 * ELSE IF MME LOCALE - 
				 * 	THEN FETCH FROM LOCALE SPECIFIC VIEW CONTENT TABLE
				 */
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				
				String sql = "Select DISTINCT  vc_vin_engine_book_name, vc_vin_engine_book_code from "+tableName+" where "
						+ " vc_vin_document_type IN ('"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")+"','"+ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY")+"',"
								+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY")+"','"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY")+"' ) "
						+ " AND vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
						+ carlineCode.trim()
						+ "'";
				logger.info("getEngineWorkshopManualList :: Sql :: >"+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				LabelBean clDetails = null;
				boolean addToList = true;
				while (rs.next()) 
				{
					clDetails = new LabelBean();
					clDetails.setKey(rs.getString("vc_vin_engine_book_code").trim());
					clDetails.setValue(rs.getString("vc_vin_engine_book_name").trim());

					addToList = true;
					if(null!=engineManualList && engineManualList.size()>0)
					{
						for(LabelBean exist : engineManualList)
						{
							if(exist.getKey().equals(clDetails.getKey()))
							{
								// ALREADY ADDED
								addToList = false;
								break;
							}
							exist=  null;
						}
					}
					if(addToList == true)
					{
						engineManualList.add(clDetails);
					}
					clDetails = null;
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt = null;
				clDetails = null;
				tableName=  null;
				if(null!=engineManualList && engineManualList.size()>0)
				{
					logger.info("getEngineWorkshopManualList :: Total Engine Workshop Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+" are :: > " + engineManualList.size());
				}
				else
				{
					logger.info("getEngineWorkshopManualList :: No Engine Workshop Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+".");
				}
				language= null;
			}
			else
			{
				logger.info("getEngineWorkshopManualList :: Locale, Model Name, Carline Code, WMI, VDS & VIN Range for Identifiying Engine Workshop Manual Types are Null.");
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getEngineWorkshopManualList()", e);
		}
		finally 
		{
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return engineManualList;
	}

	public static List<LabelBean> getTransmissionWorkshopManual_old(String language, ArrayList<ApplicableVINList> applicableVINList) throws SQLException {
		List<LabelBean> transmissionList = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if(null!=language && !"".equals(language) && null!=applicableVINList && applicableVINList.size()>0)
			{
				conn = getConnection();
				language = language.replace("-", "_");

				for(ApplicableVINList details : applicableVINList)
				{
					String sql = "Select distinct  vc_vin_mission_book_name, vc_vin_mission_book_code from gms3_vc_japan_vin_details "
							+ " where (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') "
							+ "AND vc_vin_locale='"
							+ language.trim()
							+ "' AND vc_vin_carline_code='"
							+ details.getCarlineCode().trim()
							+ "' AND vc_vin_wmi_code='"
							+ details.getWmiCode().trim()
							+ "' AND vc_vin_vds_code='"
							+ details.getVdsCode().trim()
							+ "' AND vc_vin_vis_start_range='"
							+ details.getVisStartRange().trim()
							+ "'";
					logger.info("getTransmissionWorkshopManual :: Sql :: > " + sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while (rs.next()) {
						LabelBean clDetails = new LabelBean();
						clDetails.setKey(rs.getString("vc_vin_mission_book_code").trim());
						clDetails.setValue(rs.getString("vc_vin_mission_book_name").trim());

						boolean addToList = true;
						if(null!=transmissionList && transmissionList.size()>0)
						{
							for(LabelBean exist : transmissionList)
							{
								if(exist.getKey().equals(clDetails.getKey()))
								{
									// already added
									addToList = false;
									break;
								}
								exist = null;
							}
						}
						if(addToList==true)
						{
							transmissionList.add(clDetails);
						}
						clDetails = null;
					}
					sql = null;
					rs.close();rs=null;
					stmt.close();stmt = null;
				}

				if(null!=transmissionList && transmissionList.size()>0)
				{
					logger.info("getTransmissionWorkshopManual :: Total Transmission Workshop Manual Types Identified are :: > " + transmissionList.size());
				}
				else
				{
					logger.info("getTransmissionWorkshopManual :: No Transmission Workshop Manual Types Identified for the Applicable VINs.");
				}
				language= null;
			}
			else
			{
				logger.info("getTransmissionWorkshopManual :: Locale & Applicable VIN List for Identifiying Transmission Workshop Manual Types are Null.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getTransmissionWorkshopManual()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return transmissionList;
	}

	public static List<LabelBean> getTransmissionWorkshopManual(String language, String modelName,String carlineCode, String wmiCode, String vdsCode, String vinRange) throws SQLException {
		List<LabelBean> transmissionList = new ArrayList<LabelBean>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if(null!=language && !"".equals(language) && null!=modelName && !"".equals(modelName) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=wmiCode && !"".equals(wmiCode) && 
					null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				conn = getConnection();
				language = language.replace("-", "_");

				/*
				 * IN-191220-2968 DATE - 08 MARCH 2020
				 * FETCH TRANSMISSION BOOK DATA ON THE BASIS OF MODEL ONLY - NO VIN
				 */
				
//				String sql = "Select distinct  vc_vin_mission_book_name, vc_vin_mission_book_code from gms3_vc_japan_vin_details "
//						+ " where (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
//						+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') "
//						+ " AND vc_vin_model='"+modelName.trim()+"' AND vc_vin_carline_code='"
//						+ carlineCode.trim()
//						+ "' AND vc_vin_wmi_code='"
//						+ wmiCode.trim()
//						+ "' AND vc_vin_vds_code='"
//						+ vdsCode.trim()
//						+ "' AND vc_vin_vis_start_range='"
//						+ vinRange.trim()
//						+ "'";
				/*
				 * CHECK IF MC LOCALE - THEN FETCH FROM MC VIEW CONTENT TABLE
				 * ELSE IF MME LOCALE - 
				 * 	THEN FETCH FROM LOCALE SPECIFIC VIEW CONTENT TABLE
				 */
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				String sql = "Select distinct  vc_vin_mission_book_name, vc_vin_mission_book_code from "+tableName+" "
						+ " where (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
						+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') "
						+ " AND vc_vin_model='"+modelName.trim()+"' AND vc_vin_carline_code='"
						+ carlineCode.trim()
						+ "'";
				logger.info("getTransmissionWorkshopManual :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				LabelBean clDetails = null;
				boolean addToList = true;
				while (rs.next()) {
					clDetails = new LabelBean();
					clDetails.setKey(rs.getString("vc_vin_mission_book_code").trim());
					clDetails.setValue(rs.getString("vc_vin_mission_book_name").trim());

					addToList = true;
					if(null!=transmissionList && transmissionList.size()>0)
					{
						for(LabelBean exist : transmissionList)
						{
							if(exist.getKey().equals(clDetails.getKey()))
							{
								// already added
								addToList = false;
								break;
							}
							exist = null;
						}
					}
					if(addToList==true)
					{
						transmissionList.add(clDetails);
					}
					clDetails = null;
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt = null;
				clDetails =  null;

				if(null!=transmissionList && transmissionList.size()>0)
				{
					logger.info("getTransmissionWorkshopManual :: Total Transmission Workshop Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+" are :: > " + transmissionList.size());
				}
				else
				{
					logger.info("getTransmissionWorkshopManual :: No Transmission Workshop Manual Types Identified for "+ modelName+" >> "+carlineCode+" >> "+wmiCode+" >> "+vdsCode+" >> "+vinRange+".");
				}
				language= null;
			}
			else
			{
				logger.info("getTransmissionWorkshopManual :: Locale, Model Name, Carline Code, WMI, VDS & VIN Range for Identifiying Transmission Workshop Manual Types are Null.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getTransmissionWorkshopManual()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return transmissionList;
	}

	public static CDRomSettingDetails getSettingsData(String userId)
			throws SQLException {
		CDRomSettingDetails sDetails = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			if (null != userId && !"".equals(userId)) {
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_cdrom_setting WHERE mdm_cdrom_user_login_id='"
						+ userId.trim() + "'";
				logger.info("getSettingsData :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if (rs.next()) {
					sDetails = new CDRomSettingDetails();
					sDetails.setUserId(rs.getString("mdm_cdrom_user_login_id"));
					sDetails.setNetworkPath(rs
							.getString("mdm_cdrom_gms3_network_loc"));
					sDetails.setCreationTime(rs
							.getTimestamp("mdm_cdrom_set_created_tmstp"));
					sDetails.setUpdatedTime(rs
							.getTimestamp("mdm_cdrom_set_updated_tmstp"));
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
				sql = null;
			} else {
				logger.info("getSettingsData :: User id as parameter is null. Return null");
				sDetails = null;
			}
		} catch (Exception e) {
			logger.info("getSettingsData :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getSettingsData()", e);
			logger.info("getSettingsData :: ################ Exception ################");
		} finally {
			if (null != conn)
				conn.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			userId = null;
		}
		return sDetails;
	}

	public static boolean saveNetworkPath(CDRomSettingDetails sDetails)
			throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try {
			if (null != sDetails && !"".equals(sDetails)
					&& null != sDetails.getUserId()
					&& !"".equals(sDetails.getUserId())
					&& null != sDetails.getNetworkPath()
					&& !"".equals(sDetails.getNetworkPath())) {
				conn = getConnection();
				boolean createFlag = true;
				/*
				 * Now before proceeding check for WSL ID, whether any entry
				 * exists or not if exists - then perform Update Operation else
				 * - Save Operation
				 */
				String getQuery = "SELECT * FROM gms3_mdm_cdrom_setting WHERE mdm_cdrom_user_login_id='"
						+ sDetails.getUserId() + "'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getQuery);
				if (rs.next()) {
					// set createFlag to false
					createFlag = false;
				}
				getQuery = null;
				stmt.close();
				stmt = null;
				rs.close();
				rs = null;

				if (createFlag == true) {
					logger.info("saveNetworkPath :: No entry for the WSL id Exists in Database. Proceed for Creating New Row for WSL id :: > "
							+ sDetails.getUserId());
					String insertQuery = "INSERT INTO gms3_mdm_cdrom_setting (mdm_cdrom_user_login_id,mdm_cdrom_gms3_network_loc,"
							+ "mdm_cdrom_set_created_tmstp) VALUES(?,?,?)";

					pstmt = conn.prepareStatement(insertQuery);
					pstmt.setString(1, sDetails.getUserId().trim());
					pstmt.setString(2, sDetails.getNetworkPath().trim());
					pstmt.setTimestamp(3,
							new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					insertQuery = null;
					pstmt.close();
					pstmt = null;

					logger.info("saveNetworkPath :: New Entry for the WSL id Created Successfully in gms3_dmt_conv_setting.");
				} else if (createFlag == false) {
					logger.info("saveNetworkPath :: Entry for the WSL id Exists in Database. Proceed for Updating Existing Row for WSL id :: > "
							+ sDetails.getUserId());
					String updateQuery = "UPDATE gms3_mdm_cdrom_setting SET mdm_cdrom_gms3_network_loc=?,mdm_cdrom_set_updated_tmstp=? "
							+ "WHERE mdm_cdrom_user_login_id=?";
					pstmt = conn.prepareStatement(updateQuery);
					pstmt.setString(1, sDetails.getNetworkPath().trim());
					pstmt.setTimestamp(2,
							new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, sDetails.getUserId().trim());
					pstmt.executeUpdate();
					updateQuery = null;
					pstmt.close();
					pstmt = null;

					logger.info("saveNetworkPath :: Existing Entry for the WSL id Updated Successfully in gms3_dmt_conv_setting.");
				}
			} else {
				logger.info("saveNetworkPath :: WSL id and Network Path are passed as null in Parameters. Return false");
				return false;
			}
		} catch (Exception e) {
			if (null != conn) {
				conn.rollback();
			}
			logger.info("saveNetworkPath :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"saveNetworkPath()", e);
			logger.info("saveNetworkPath :: ################ Exception ################");
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

	public static ArrayList<CDRomScheduleDetails> getScheduleDetails(
			String scheduleId,String fromDate, String toDate) throws SQLException {
		ArrayList<CDRomScheduleDetails> schedueList = new ArrayList<CDRomScheduleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		try {
			conn = getConnection();
			if (StringUtils.isBlank(scheduleId)) {
				//				sql = "SELECT A.*, (SELECT DISTINCT B.cd_manual_type FROM "
				//						+ " gms3_mdm_cdrom_sch_items B WHERE B.cd_schedule_id = A.cd_schedule_id) AS MANUALTYPE "
				//						+ " FROM gms3_mdm_cdrom_schedule A ORDER BY A.cd_schedule_id DESC";
				sql="SELECT * FROM gms3_mdm_cdrom_schedule where cd_schedule_name not like 'MDM_CDROM_SC_%'  ";
				if(null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate))
				{
					sql = sql+" AND DATE_FORMAT(cd_schedule_tmstp,'%Y-%m-%d') BETWEEN '"+fromDate+"' AND '"+toDate+"' ";
				}
				sql = sql+" ORDER BY cd_schedule_id DESC ";
			} else {
				//				sql = "SELECT A.*, (SELECT DISTINCT B.cd_manual_type FROM "
				//						+ " gms3_mdm_cdrom_sch_items B WHERE B.cd_schedule_id = A.cd_schedule_id) AS MANUALTYPE "
				//						+ " FROM gms3_mdm_cdrom_schedule A WHERE cd_schedule_id="
				//						+ Long.parseLong(scheduleId)
				//						+ " ORDER BY A.cd_schedule_id DESC";

				sql = "SELECT * FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="+scheduleId;
			}
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				CDRomScheduleDetails schDetails = new CDRomScheduleDetails();
				schDetails.setSrNo(schedueList.size() + 1);
				schDetails.setScheduleId(rs.getLong("cd_schedule_id"));
				schDetails.setScheduleName(rs.getString("cd_schedule_name"));
				schDetails.setUserId(rs.getString("cd_user_id"));
				schDetails.setTotalDocsForProcessing(rs
						.getLong("cd_total_docs_count"));
				schDetails.setOkAssetsCount(rs.getLong("cd_total_okast_count"));
				schDetails.setProcessedDocsCount(rs
						.getLong("cd_processed_docs_count"));
				schDetails.setScheduleStatus(rs.getString("cd_sch_status"));
				schDetails
				.setScheduleTime(rs.getTimestamp("cd_schedule_tmstp"));
				schDetails.setFinishTime(rs.getTimestamp("cd_finish_tmstp"));
				schDetails.setThreadId(rs.getString("cd_sch_thread_id"));
				schDetails.setProcessedOkAssetsCount(rs
						.getLong("cd_processed_okast_count"));
				schDetails.setTotalInnerLinksCount(rs
						.getLong("cd_total_inrlks_count"));
				schDetails.setProcessedInnerLinksCount(rs
						.getLong("cd_processed_inrlks_count"));
				schDetails.setFailedProcessedDocsCount(rs
						.getLong("cd_failed_docs_count"));
				schDetails.setFailedOkAssetsCount(rs
						.getLong("cd_failed_okast_count"));
				schDetails.setFailedInnerLinksCount(rs
						.getLong("cd_failed_inrlks_count"));
				schDetails.setCurrentProcessingStatus(rs
						.getString("cd_current_processing_status"));
				schDetails.setCarlineCode(rs.getString("mdm_crln_code"));
				schDetails.setCarlineNameRegional(rs
						.getString("mdm_crln_name_regional_lang"));
				schDetails.setCountryLocalId(rs.getLong("mdm_cl_id"));
				schDetails.setLocaleCode(rs.getString("mdm_ml_lang_code"));
				schDetails.setModelType(rs.getString("mdm_crln_model_type"));
				schDetails.setVinId(rs.getLong("mdm_vin_id"));
				schDetails.setVinStartRange(rs
						.getString("mdm_vin_vis_start_range"));
				schDetails.setVinVdsCode(rs.getString("mdm_vin_vds_code"));
				schDetails.setVinWmiCode(rs.getString("mdm_vin_wmi_code"));
				schDetails.setZipFileName(rs.getString("cd_zip_file_name"));
				schedueList.add(schDetails);
				schDetails = null;
			}
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			sql = null;

			if (null != schedueList && schedueList.size() > 0) {
				// IDENTIFY THE MARKET FOR EACH SCHEDULE
			}
		} catch (Exception e) {
			logger.info("getScheduleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getScheduleDetails()", e);
			logger.info("getScheduleDetails :: ################ Exception ################");
			// set scheduleList to null
			schedueList = null;
		} finally {
			if (null != conn)
				conn.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
		}
		return schedueList;
	}

	public static ArrayList<CDRomScheduleItemDetails> getScheduleItemDetails(
			String scheduleId) throws SQLException {
		ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				conn = getConnection();
				String sql = "SELECT B.mdm_ml_lang_code,B.mdm_crln_code,B.mdm_crln_name_regional_lang,B.mdm_crln_model_type,B.mdm_vin_wmi_code"
						+ ", B.mdm_vin_vds_code,B.mdm_vin_vis_start_range,B.cd_sch_thread_id, A.* FROM gms3_mdm_cdrom_sch_items A, gms3_mdm_cdrom_schedule B  WHERE  A.cd_schedule_id = B.cd_schedule_id AND "
						+ " A.cd_schedule_id ="
						+ new Long(scheduleId).longValue();
				logger.info("getScheduleItemDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					CDRomScheduleItemDetails schItemDetails = new CDRomScheduleItemDetails();
					schItemDetails.setSrNo(itemsList.size() + 1);
					schItemDetails.setItemId(rs.getLong("cd_item_id"));
					schItemDetails.setScheduleId(rs.getLong("cd_schedule_id"));
					schItemDetails.setManualTypeCode(rs
							.getString("cd_manual_type_code"));
					schItemDetails.setManualTypeName(rs
							.getString("cd_manual_type_name"));
					schItemDetails
					.setManualType(rs.getString("cd_manual_type"));
					schItemDetails.setCurrentProcessingStatus(rs
							.getString("cd_current_processing_status"));
					schItemDetails.setChannelName(rs
							.getString("cd_channel_name"));
					schItemDetails.setChannelRefKey(rs
							.getString("cd_channel_ref_key"));
					schItemDetails.setTotalDocsForProcessing(rs
							.getLong("cd_total_docs_count"));
					schItemDetails.setOkAssetsCount(rs
							.getLong("cd_total_okast_count"));
					schItemDetails
					.setThreadId(rs.getString("cd_sch_thread_id"));
					schItemDetails.setProcessedDocsCount(rs
							.getLong("cd_processed_docs_count"));
					schItemDetails.setFailedProcessedDocsCount(rs
							.getLong("cd_failed_docs_count"));
					schItemDetails.setProcessedOkAssetsCount(rs
							.getLong("cd_processed_okast_count"));
					schItemDetails.setFailedOkAssetsCount(rs
							.getLong("cd_failed_okast_count"));
					schItemDetails.setTotalInnerLinksCount(rs
							.getLong("cd_total_inrlks_count"));
					schItemDetails.setProcessedInnerLinksCount(rs
							.getLong("cd_processed_inrlks_count"));
					schItemDetails.setFailedInnerLinksCount(rs
							.getLong("cd_failed_inrlks_count"));

					schItemDetails.setLocale(rs.getString("mdm_ml_lang_code"));
					schItemDetails
					.setCarlineCode(rs.getString("mdm_crln_code"));
					schItemDetails.setModel(rs.getString("mdm_crln_name_regional_lang"));
					schItemDetails.setModelType(rs.getString("mdm_crln_model_type"));
					schItemDetails.setWmiCode(rs.getString("mdm_vin_wmi_code"));
					schItemDetails.setVdsCode(rs.getString("mdm_vin_vds_code"));
					schItemDetails.setVinRange(rs
							.getString("mdm_vin_vis_start_range"));
					itemsList.add(schItemDetails);
					schItemDetails = null;
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
				sql = null;
			} else {
				logger.info("getScheduleItemDetails :: Schedule id as parameters are null. Return null");
				itemsList = null;
			}
		} catch (Exception e) {
			logger.info("getScheduleItemDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getScheduleItemDetails()", e);
			logger.info("getScheduleItemDetails :: ################ Exception ################");
			// set itemLsit to null;
			itemsList = null;
		} finally {
			if (null != conn)
				conn.close();
			if (null != stmt)
				stmt.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			// set scheduleId to null
			scheduleId = null;
		}
		return itemsList;
	}

	public boolean updateScheduleStatus(String scheduleId, String status)
			throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)
					&& !"0".equals(scheduleId) && null != status
					&& !"".equals(status)) {
				String completeStatus = "";
				// SET FINIFH TIME AS WELL HERE IF STATUS IS OT PENDING /
				// PROCESSING
				String updateStatusSql = "";
				if (status.equals(ApplicationProperties
						.getProperty("schedule.status.pending.value"))
						|| status
						.equals(ApplicationProperties
								.getProperty("schedule.status.processing.value"))) {
					updateStatusSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_sch_status=? WHERE cd_schedule_id= ?";
				} else {
					completeStatus = "COMPLETED";
					// set finish Time
					updateStatusSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_sch_status=?, cd_finish_tmstp = ?, cd_current_processing_status = ? WHERE cd_schedule_id= ?";
				}

				conn = getConnection();

				pstmt = conn.prepareStatement(updateStatusSql);
				if (status.equals(ApplicationProperties
						.getProperty("schedule.status.pending.value"))
						|| status
						.equals(ApplicationProperties
								.getProperty("schedule.status.processing.value"))) {
					pstmt.setString(1, status);
					pstmt.setLong(2, new Long(scheduleId).longValue());
				} else {
					pstmt.setString(1, status);
					pstmt.setTimestamp(2,
							new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, completeStatus);
					pstmt.setLong(4, new Long(scheduleId).longValue());

				}
				int i = pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateStatusSql = null;
				if (i == 1) {
					logger.info("updateScheduleStatus :: Status for Schedule Id {"
							+ scheduleId + "} Updated to :: >" + status);
				} else {
					logger.info("updateScheduleStatus :: Failed to Update {"
							+ status + "} Status for Schedule Id {"
							+ scheduleId + "}.");
					return false;
				}
			} else {
				logger.info("updateScheduleStatus :: Schedule id / Status as parameters are null. Return false.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateScheduleStatus :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateScheduleStatus()", e);
			logger.info("updateScheduleStatus :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			// set params to null
			scheduleId = null;
			status = null;
		}
		return true;
	}


	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button writes cd_sch_status = aborted through updateAbortStatus() below, and does
	 * so whether or not it could find the running thread. The database therefore already carries
	 * the abort signal and this only reads it back.
	 *
	 * WHY THE DATABASE AND NOT A Thread REFERENCE. The screen used to call Thread.stop() on the
	 * worker. That was REMOVED in Java 20 and throws UnsupportedOperationException, which the
	 * caller's catch(Exception) swallowed - so the schedule was marked Aborted while the worker ran
	 * on to completion. Nothing can kill another thread any more; the worker has to stop itself.
	 *
	 * CDRom stores NUMERIC status codes (aborted = 5), not the ScheduleConstants strings.
	 *
	 * Returns FALSE on any error: a failed status read must never abort a healthy run.
	 */
	public static boolean isAborted(String scheduleId)
	{
		boolean aborted = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				if(null!=conn)
				{
					String sql="SELECT cd_sch_status FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, scheduleId.trim());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						String status = rs.getString("cd_sch_status");
						String abortedValue = ApplicationProperties.getProperty("schedule.status.aborted.value");
						if(null!=status && null!=abortedValue
								&& status.trim().equalsIgnoreCase(abortedValue.trim()))
						{
							aborted = true;
						}
						status = null;
						abortedValue = null;
					}
					sql = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "isAborted()", e);
		}
		finally
		{
			try
			{
				if(null!=rs) rs.close();
				if(null!=pstmt) pstmt.close();
				if(null!=conn) conn.close();
			}
			catch(Exception re)
			{
				Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "isAborted()", re);
			}
			rs = null;
			pstmt = null;
			conn = null;
		}
		return aborted;
	}


	public static boolean updateAbortStatus(String scheduleId, String status)
			throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)
					&& !"0".equals(scheduleId) && null != status
					&& !"".equals(status)) {
				String completeStatus = "COMPLETED";
				// SET FINIFH TIME AS WELL HERE IF STATUS IS OT PENDING /
				// PROCESSING
				String updateStatusSql = "";
				// set finish Time
				updateStatusSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_sch_status=?, cd_finish_tmstp = ?, cd_current_processing_status = ? WHERE cd_schedule_id= ?";

				conn = getConnection();
				conn.setAutoCommit(false);

				pstmt = conn.prepareStatement(updateStatusSql);

				pstmt.setString(1, status);
				pstmt.setTimestamp(2,
						new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(3, completeStatus);
				pstmt.setLong(4, new Long(scheduleId).longValue());

				int i = pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateStatusSql = null;

				/*
				 * also update all the items status to completed.
				 */
				updateStatusSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_current_processing_status = ? WHERE cd_schedule_id= ?";
				pstmt = conn.prepareStatement(updateStatusSql);

				pstmt.setString(1, completeStatus);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				int J = pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateStatusSql = null;


				if (i > 0 && J > 0) {
					logger.info("updateAbortStatus :: Status for Schedule Id {"
							+ scheduleId + "} Updated to :: >" + status);
					conn.commit();
				} else {
					logger.info("updateAbortStatus :: Failed to Update {"
							+ status + "} Status for Schedule Id {"
							+ scheduleId + "}.");
					return false;
				}
			} else {
				logger.info("updateAbortStatus :: Schedule id / Status as parameters are null. Return false.");
				return false;
			}
		} catch (Exception e) {
			conn.rollback();
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateAbortStatus()", e);
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			// set params to null
			scheduleId = null;
			status = null;
		}
		return true;
	}


	/**
	 * Function will get the Pending / Processing Jobs Count.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static int getProcessingScheduleJobCount() throws SQLException {
		int count = 0;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			String sql = "SELECT COUNT(MDM_CDROM_SCHEDULE_ID) AS COUNT FROM gms3_mdm_cdrom_schedule WHERE "
					+ " cd_sch_status IN ('"
					+ ApplicationProperties
					.getProperty("schedule.status.pending.value")
					+ "',"
					+ " '"
					+ ApplicationProperties
					.getProperty("schedule.status.processing.value")
					+ "')";
			logger.info("getProcessingScheduleJobCount :: SQL :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				count = rs.getInt("COUNT");
			}
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			sql = null;

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getProcessingScheduleJobCount()", e);
		} finally {
			if (null != conn)
				conn.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
		}
		return count;
	}

	public static Long createSchedule(CDRomScheduleDetails schDetails)
			throws SQLException {
		Long scheduleId = null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try {
			if (null != schDetails) {
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO GMS3_DMT_CONV_SCHEDULE First insert the
				 * SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql = "INSERT INTO gms3_mdm_cdrom_schedule(cd_schedule_name,cd_user_id) VALUES"
						+ "('"
						+ schDetails.getScheduleName()
						+ "','"
						+ schDetails.getUserId() + "')";
				logger.info("createSchedule :: Generate Schedule Id :: Sql :: > "
						+ sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "cd_schedule_id" };
				stmt.execute(sql, generatedColumns);
				rs = stmt.getGeneratedKeys();
				if (rs.next()) {
					scheduleId = rs.getLong(1);
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
				sql = null;
				if (null != scheduleId && scheduleId > 0) {
					logger.info("createSchedule :: Procced for Storing details for Schedule Id :: > "
							+ scheduleId);
					/*
					 * add scheduleId to name and update It add scheduleId to
					 * thread and update It
					 */
					schDetails.setScheduleName(schDetails.getScheduleName()+ String.valueOf(scheduleId));
					schDetails.setThreadId(schDetails.getThreadId()	+ String.valueOf(scheduleId));
					String schSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_schedule_name=?,mdm_cl_id=?,"
							+ "mdm_ml_id=?,mdm_ml_lang_code=?,mdm_crln_code=?,mdm_crln_name_regional_lang=?,"
							+ "mdm_crln_model_type=?,mdm_vin_id=?,mdm_vin_wmi_code=?,"
							+ "mdm_vin_vds_code=?,mdm_vin_vis_start_range=?,cd_sch_status=?,cd_total_docs_count=?,"
							+ "cd_total_okast_count=?,cd_sch_thread_id=?,cd_schedule_tmstp=?"
							+ "WHERE cd_schedule_id= ?";
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setLong(2, schDetails.getCountryLocalId());
					pstmt.setLong(3, schDetails.getLanguageLocalId());
					pstmt.setString(4, schDetails.getLocaleCode());
					pstmt.setString(5, schDetails.getCarlineCode());
					pstmt.setString(6, schDetails.getCarlineNameRegional());
					pstmt.setString(7, schDetails.getModelType());
					pstmt.setLong(8, schDetails.getVinId());
					pstmt.setString(9, schDetails.getVinWmiCode());
					pstmt.setString(10, schDetails.getVinVdsCode());
					pstmt.setString(11, schDetails.getVinStartRange());
					pstmt.setString(12, schDetails.getScheduleStatus());
					pstmt.setLong(13, schDetails.getTotalDocsForProcessing());
					pstmt.setLong(14, schDetails.getOkAssetsCount());
					pstmt.setString(15, schDetails.getThreadId());
					pstmt.setTimestamp(16,new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(17, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					schSql = null;

					/*
					 * PROCEED FOR SAVING ITEM DETAILS
					 */
					if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
					{
						for(CDRomScheduleItemDetails itemDetails : schDetails.getItemsList())
						{
							String itemSql = "INSERT INTO gms3_mdm_cdrom_sch_items(cd_schedule_id,cd_manual_type_code,cd_manual_type_name,"
									+ "cd_manual_type,cd_channel_name,cd_channel_ref_key,cd_total_docs_count,cd_total_okast_count,"
									+ "cd_total_inrlks_count,cd_created_tmstp) "
									+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
							pstmt = null;
							pstmt = conn.prepareStatement(itemSql);
							pstmt.setLong(1, scheduleId);
							pstmt.setString(2, itemDetails.getManualTypeCode());
							pstmt.setString(3, itemDetails.getManualTypeName());
							pstmt.setString(4, itemDetails.getManualType());
							pstmt.setString(5, itemDetails.getChannelName());
							pstmt.setString(6, itemDetails.getChannelRefKey());
							pstmt.setLong(7, itemDetails.getTotalDocsForProcessing());
							pstmt.setLong(8, itemDetails.getOkAssetsCount());
							pstmt.setLong(9, itemDetails.getTotalInnerLinksCount());
							pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
							pstmt.executeUpdate();
							pstmt.close();
							pstmt = null;
							itemDetails = null;
							itemSql = null;
						}
					}
					// commit the transaction
					conn.commit();

				} else {
					logger.info("createSchedule :: Failed to Generate Schedule Id. Return null.");
				}
			}
			else {
				logger.info("createSchedule :: Schedule Details or Items in it are null as parameters, return null.");
			}
		} catch (Exception e) {
			logger.info("createSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			conn.rollback();
			logger.info("createSchedule :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"createSchedule()", e);
			logger.info("createSchedule :: ################ Exception ################");
			scheduleId = null;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			// set schDetails to null
			schDetails = null;
		}
		return scheduleId;
	}

	public boolean updateFailureCount(String scheduleId, int failedDoc,
			String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_failed_docs_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_failed_docs_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				count = count + failedDoc;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_failed_docs_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemFailureCount = 0;
					String getExistingItemsCount = "SELECT cd_failed_docs_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemFailureCount = rs.getLong("cd_failed_docs_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					itemFailureCount = itemFailureCount + failedDoc;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_failed_docs_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemFailureCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}
			} else {
				logger.info("updateFailureCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateFailureCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateFailureCount()", e);
			logger.info("updateFailureCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateOKAssetFailureCount(String scheduleId,
			String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_failed_okast_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_failed_okast_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				// increment by 1
				count = count + 1;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_failed_okast_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemFailureCount = 0;
					String getExistingItemsCount = "SELECT cd_failed_okast_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemFailureCount = rs.getLong("cd_failed_okast_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					// increment by 1
					itemFailureCount = itemFailureCount + 1;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_failed_okast_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemFailureCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}
			} else {
				logger.info("updateOKAssetFailureCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateOKAssetFailureCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateOKAssetFailureCount()", e);
			logger.info("updateOKAssetFailureCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateOKAssetProcessingCount(String scheduleId,
			String docType, int okAssetCount) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_processed_okast_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_processed_okast_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				// increment by 1
				count = count + okAssetCount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_processed_okast_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemCount = 0;
					String getExistingItemsCount = "SELECT cd_processed_okast_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemCount = rs.getLong("cd_processed_okast_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					// increment by 1
					itemCount = itemCount + okAssetCount;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_processed_okast_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}

			} else {
				logger.info("updateOKAssetProcessingCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateOKAssetProcessingCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateOKAssetProcessingCount()", e);
			logger.info("updateOKAssetProcessingCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateProcessingCount(String scheduleId,
			int passedDoc, String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_processed_docs_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_processed_docs_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				count = count + passedDoc;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_processed_docs_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemCount = 0;
					String getExistingItemsCount = "SELECT cd_processed_docs_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemCount = rs.getLong("cd_processed_docs_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					itemCount = itemCount + passedDoc;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_processed_docs_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}

			} else {
				logger.info("updateProcessingCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateProcessingCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateProcessingCount()", e);
			logger.info("updateProcessingCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean upadteScheduleDocCount(String scheduleId,
			int docCount, String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_total_docs_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_total_docs_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				// increment by 1
				count = count + docCount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_total_docs_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;
			}
			if (null != docType && !"".equals(docType) && !"0".equals(docType)) {
				long itemCount = 0;
				String getExistingItemsCount = "SELECT cd_total_docs_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue()
						+ "AND cd_manual_type_code='" + docType + "'";
				pstmt = conn.prepareStatement(getExistingItemsCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					itemCount = rs.getLong("cd_total_docs_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingItemsCount = null;

				// increment by 1
				itemCount = itemCount + docCount;
				String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_total_docs_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
				pstmt = conn.prepareStatement(updateItemsCountSql);
				pstmt.setLong(1, itemCount);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.setString(3, docType);
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateItemsCountSql = null;
			} else {
				logger.info("upadteScheduleDocCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("upadteScheduleDocCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"upadteScheduleDocCount()", e);
			logger.info("upadteScheduleDocCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateInnerLinksCount(String scheduleId,
			int innerLinkCount, String docType)
					throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {

				long innerCount = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_total_inrlks_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					innerCount = rs.getLong("cd_total_inrlks_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				innerCount = innerCount + innerLinkCount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_total_inrlks_count=? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, innerCount);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET  cd_total_inrlks_count=? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, innerLinkCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}
			} else {
				logger.info("updateInnerLinksCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateInnerLinksCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateInnerLinksCount()", e);
			logger.info("updateInnerLinksCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateOKAssetCount(String scheduleId,
			int okAssetCount, String docType)
					throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {

				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_total_okast_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_total_okast_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				count = count + okAssetCount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_total_okast_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;
				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemCount = 0;
					String getExistingItemsCount = "SELECT cd_total_okast_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemCount = rs.getLong("cd_total_okast_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					itemCount = itemCount + okAssetCount;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_total_okast_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}
			} else {
				logger.info("upadteOKAssetCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("upadteOKAssetCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"upadteScheduleDocCount()", e);
			logger.info("upadteOKAssetCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateInnerLinkAndOKAssetCount(String scheduleId,
			int okAssetCount, int innerLinkCount, String docType)
					throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {

				long count = 0;
				long innerCount = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_total_okast_count,cd_total_inrlks_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_total_okast_count");
					innerCount = rs.getLong("cd_total_inrlks_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				count = count + okAssetCount;
				innerCount = innerCount + innerLinkCount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_total_okast_count =?, cd_total_inrlks_count=? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, innerCount);
				pstmt.setLong(3, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;
				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemCount = 0;
					String getExistingItemsCount = "SELECT cd_total_okast_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemCount = rs.getLong("cd_total_okast_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					itemCount = itemCount + okAssetCount;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_total_okast_count =?, cd_total_inrlks_count=? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, innerLinkCount);
					pstmt.setLong(3, new Long(scheduleId).longValue());
					pstmt.setString(4, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}
			} else {
				logger.info("upadteOKAssetCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("upadteOKAssetCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"upadteScheduleDocCount()", e);
			logger.info("upadteOKAssetCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateCurrentProcessingStatus(String scheduleId,
			String status, String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				String updateCountSql;
				conn = getConnection();
				if (StringUtils.isBlank(docType)) {
					updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_current_processing_status =? WHERE cd_schedule_id=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setString(1, status);
					pstmt.setLong(2,
							new Long(String.valueOf(scheduleId)).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateCountSql = null;
				}
				if (StringUtils.isNotBlank(docType)
						&& !status
						.equalsIgnoreCase(ApplicationProperties
								.getProperty("schedule.current.status.complete.value"))) {
					updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_current_processing_status =? WHERE cd_schedule_id=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setString(1, status);
					pstmt.setLong(2,
							new Long(String.valueOf(scheduleId)).longValue());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateCountSql = null;
				}
				if (StringUtils.isNotBlank(docType)) {

					updateCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_current_processing_status =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateCountSql);
					pstmt.setString(1, status);
					pstmt.setLong(2,
							new Long(String.valueOf(scheduleId)).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateCountSql = null;
				}
			} else {
				logger.info("updateCriteriaStatus :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateCriteriaStatus :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateCriteriaStatus()", e);
			logger.info("updateCriteriaStatus :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;
	}

	public boolean updateProcessedInnerLinksCount(String scheduleId,
			int innerLinkcount, String docType) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				long count = 0;
				conn = getConnection();
				String getExistingCount = "SELECT cd_processed_inrlks_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="
						+ new Long(scheduleId).longValue();
				pstmt = conn.prepareStatement(getExistingCount);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					count = rs.getLong("cd_processed_inrlks_count");
				}
				rs.close();
				rs = null;
				pstmt.close();
				pstmt = null;
				getExistingCount = null;

				count = count + innerLinkcount;
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_processed_inrlks_count =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setLong(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;

				if (null != docType && !"".equals(docType)
						&& !"0".equals(docType)) {
					long itemCount = 0;
					String getExistingItemsCount = "SELECT cd_processed_inrlks_count FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id="
							+ new Long(scheduleId).longValue()
							+ "AND cd_manual_type_code='" + docType + "'";
					pstmt = conn.prepareStatement(getExistingItemsCount);
					rs = pstmt.executeQuery();
					if (rs.next()) {
						itemCount = rs.getLong("cd_processed_inrlks_count");
					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					getExistingItemsCount = null;

					itemCount = itemCount + innerLinkcount;
					String updateItemsCountSql = "UPDATE gms3_mdm_cdrom_sch_items SET cd_processed_inrlks_count =? WHERE cd_schedule_id=? AND cd_manual_type_code=?";
					pstmt = conn.prepareStatement(updateItemsCountSql);
					pstmt.setLong(1, itemCount);
					pstmt.setLong(2, new Long(scheduleId).longValue());
					pstmt.setString(3, docType);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateItemsCountSql = null;
				}

			} else {
				logger.info("updateProcessedInnerLinksCount :: Schedule id as Parameter is null.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateProcessedInnerLinksCount :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateProcessedInnerLinksCount()", e);
			logger.info("updateProcessedInnerLinksCount :: ################ Exception ################");
			return false;
		} finally {
			if (null != conn)
				conn.close();
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
		}
		return true;

	}

	public static String getResourcePath(String documentId,String language) throws SQLException {
		String resourcePath = "";
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		String sql = "";
		try {
			if(null!=documentId && !"".equals(documentId))
			{
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET 
					tableName="gms3_dmt_mc_newm_imdoc,gms3_dmt_mc_imdoc";
				}
				else
				{
					// MME MARKET
					tableName = "gms3_dmt_"+Utilities.tableLocale(language)+"_nm_imdoc";
					tableName+=",gms3_dmt_"+Utilities.tableLocale(language)+"_imdoc";
				}
				
				String[] tabTables = tableName.split(",");
				/*
				 * CHECK FOR DOCUMENT ID IN BOTH TABLES
				 * WHERE EVER FOUND BREAK THE LOOP
				 */
				conn = getConnection();

				for(int a=0;a<tabTables.length;a++)
				{
					sql="SELECT dc_im_doc_resource_path FROM "+ tabTables[a].toString()+" WHERE dc_im_doc_id='"+documentId+"'";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if (rs.next()) 
					{
						resourcePath = (rs.getString("dc_im_doc_resource_path"));
						if(null!=resourcePath && !"".equals(resourcePath))
						{
							resourcePath = resourcePath.trim();
						}
						// break the loop resource path found.
						break;
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
					sql = null;
				}
			}
			else
			{
				logger.info("getResourcePath :: DOCUMENT id AS PARAMETERS ARE NULL.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getResourcePath()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return resourcePath;
	}

	public void updateZipFileName(String zipFileName, String scheduleId)
			throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				conn = getConnection();
				String updateCountSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_zip_file_name =? WHERE cd_schedule_id=?";
				pstmt = conn.prepareStatement(updateCountSql);
				pstmt.setString(1, zipFileName);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateCountSql = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateZipFileName()", e);
		} finally {
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}

	}

	public static String getZipFileName(String scheduleCode)
			throws SQLException {
		String zipfile = "";
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			String sql = "SELECT DISTINCT cd_zip_file_name FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id ="
					+ Long.parseLong(scheduleCode);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				zipfile = (rs.getString("cd_zip_file_name"));
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getZipFileName()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return zipfile;
	}

	public int getFailureCount(Long scheduleID) throws SQLException {
		int count = 0;
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			String sql = "SELECT cd_failed_docs_count,cd_failed_okast_count,cd_failed_inrlks_count FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id ="
					+ scheduleID;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int docCount = rs.getInt("cd_failed_docs_count");
				int okAssetCount = rs.getInt("cd_failed_okast_count");
				int innerLinkCount = rs.getInt("cd_failed_inrlks_count");
				count = docCount + okAssetCount + innerLinkCount;
			}
			sql = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"getFailureCount()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return count;
	}

	/**
	 * Function will Fetch All the Applicable VINs on the basis of VIN Selected on Screen.
	 * @param locale
	 * @param carlineCode
	 * @param vdsCode
	 * @param visStartRange
	 * @param modelType
	 * @throws SQLException
	 */
	public static ArrayList<ApplicableVINList> getApplicableVINList_old(String locale, String carlineCode, String vdsCode, String visStartRange) throws SQLException
	{
		ArrayList<ApplicableVINList> applicableVinList = new ArrayList<ApplicableVINList>();
		Connection conn = null;
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			if(null!=locale && !"".equals(locale) && null!=carlineCode && !"".equals(carlineCode) && 
					null!=vdsCode && !"".equals(vdsCode) && null!=visStartRange && !"".equals(visStartRange))
			{
				String newModelKey = "\\"+ApplicationProperties.getProperty("cdrom.model.type.folder.key.newm");

				locale = locale.replace("_", "-");
				conn = getConnection();
				String tableName="gms3_dmt_mc_newm_vin,gms3_dmt_mc_vin";
				String[] tabTokens=tableName.split(",");

				ArrayList<String> uniqueFaceLiftFolderList = new ArrayList<String>();
				String identifyUniqueFaceLiftFolderSql=null;
				if(null!=tabTokens && tabTokens.length>0)
				{
					for(int a=0;a<tabTokens.length;a++)
					{
						// retrieve from OLD / NEW MC TABLES
						identifyUniqueFaceLiftFolderSql="SELECT DISTINCT dc_vin_txt_file_path FROM "+tabTokens[a].toString()+" WHERE "
								+ " TRIM(LOWER(dc_vin_carline_code))='"+carlineCode.trim().toLowerCase()+"' "
								+ " AND TRIM(LOWER(dc_vin_vds_code))='"+vdsCode.trim().toLowerCase()+"' "
								+ " AND TRIM(LOWER(dc_vin_vis_start_range))='"+visStartRange.trim().toLowerCase()+"'";
						logger.info("getApplicableVINList :: identifyUniqueFaceLiftFolderSql :: > "+identifyUniqueFaceLiftFolderSql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(identifyUniqueFaceLiftFolderSql);
						while(rs.next())
						{
							String path = rs.getString("dc_vin_txt_file_path");
							if(null!=path && !"".equals(path))
							{
								// TAKE THE VALUE FROM LOCALE, REMOVE STRING BEFORE IT
								if(path.trim().toLowerCase().indexOf(locale.trim().toLowerCase())!=-1)
								{
									path= path.substring(path.trim().toLowerCase().indexOf(locale.trim().toLowerCase()), path.length());
								}
								if(null!=path && !"".equals(path))
								{
									path = path.trim();
									// REMOVE VIN FILE NAME
									if(path.lastIndexOf("\\")!=-1)
									{
										path = path.substring(0,path.lastIndexOf("\\"));
									}

									if(null!=path && !"".equals(path))
									{
										// BEFORE ADDING CHECK IF ALREADY ADDED IN THE LIST
										boolean addToList = true;
										if(null!=uniqueFaceLiftFolderList && uniqueFaceLiftFolderList.size()>0)
										{
											for(String existingPath : uniqueFaceLiftFolderList)
											{
												if(existingPath.trim().toLowerCase().equals(path.trim().toLowerCase()))
												{
													// ALREADY EXISTS - DO NOT ADD
													addToList = false;
													break;
												}
												existingPath = null;
											}
										}

										if(addToList == true)
										{
											uniqueFaceLiftFolderList.add(path);
										}
									}
								}
							}
							path = null;
						}
						identifyUniqueFaceLiftFolderSql = null;
						rs.close();rs=null;
						stmt.close();stmt=null;
					}
				}
				identifyUniqueFaceLiftFolderSql = null;

				if(null!=uniqueFaceLiftFolderList && uniqueFaceLiftFolderList.size()>0)
				{
					String getVINSql = null;
					String vinTableName=null;
					for(String faceLiftFolderPath : uniqueFaceLiftFolderList)
					{
						if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath))
						{
							if(faceLiftFolderPath.trim().toLowerCase().contains(newModelKey.trim().toLowerCase()))
							{
								vinTableName = "gms3_dmt_mc_newm_vin";
							}
							else
							{
								vinTableName = "gms3_dmt_mc_vin";
							}
						}
						/*
						 * CHECK FACELIFT FOLDER PATH IN BOTH TABLES
						 * IT WILL EXIST IN EITHER OF IT.
						 */
						if(null!=vinTableName && !"".equals(vinTableName))
						{
							// CHECK IN OLD / NEW TABLE
							getVINSql="SELECT DISTINCT dc_vin_carline_code, dc_vin_vds_code, dc_vin_vis_start_range "
									+ " FROM "+vinTableName+" WHERE TRIM(LOWER(dc_vin_txt_file_path)) LIKE '%"+faceLiftFolderPath.trim().toLowerCase()+"%' "
									+ " GROUP BY dc_vin_carline_code, dc_vin_vds_code, dc_vin_vis_start_range";
							logger.info("getApplicableVINList :: getVINSql :: > " + getVINSql);
							stmt = conn.createStatement();
							rs = stmt.executeQuery(getVINSql);
							while(rs.next())
							{
								ApplicableVINList appVINDetails =new ApplicableVINList();
								appVINDetails.setCarlineCode(rs.getString("dc_vin_carline_code"));
								appVINDetails.setVdsCode(rs.getString("dc_vin_vds_code"));
								appVINDetails.setVisStartRange(rs.getString("dc_vin_vis_start_range"));
								appVINDetails.setWmiCode("-");
								// ALSO ADD FACELIFT PATH TO THE APPLICABLE VIN LIST
								appVINDetails.setFaceLiftFolderPath(faceLiftFolderPath);
								/*
								 * CHECK THE APP VIN NEEDS TO BE ADDED IN THE LIST OR NOT
								 */
								boolean addToList = true;
								if(null!=applicableVinList && applicableVinList.size()>0)
								{
									for(ApplicableVINList existVINDetails : applicableVinList)
									{
										if(null!=existVINDetails.getCarlineCode() && null!=existVINDetails.getVdsCode() && null!=existVINDetails.getVisStartRange() 
												&& null!=appVINDetails.getCarlineCode() && null!=appVINDetails.getVdsCode() && null!=appVINDetails.getVisStartRange())
										{
											if(existVINDetails.getCarlineCode().trim().toLowerCase().equals(appVINDetails.getCarlineCode().trim().toLowerCase()) && 
													existVINDetails.getVdsCode().trim().toLowerCase().equals(appVINDetails.getVdsCode().trim().toLowerCase()) && 
													existVINDetails.getVisStartRange().trim().toLowerCase().equals(appVINDetails.getVisStartRange().trim().toLowerCase()))
											{
												// VIN ALREADY ADDED
												addToList = false;
												break;
											}
										}
										existVINDetails = null;
									}
								}

								if(addToList == true)
								{
									applicableVinList.add(appVINDetails);
								}
								appVINDetails=  null;
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							getVINSql = null;
						}
						vinTableName = null;
					}
					getVINSql = null;

					if(null!=applicableVinList && applicableVinList.size()>0)
					{
						logger.info("getApplicableVINList :: Total Applicable VINs Found are :: > " + applicableVinList.size());
					} 
					else
					{
						logger.info("getApplicableVINList :: No Applicable VINs Found for :: >" + carlineCode+" > "+ vdsCode+" >" + visStartRange);
					}
				}
				else
				{
					logger.info("getApplicableVINList :: No Face Lift Folder Found for :: >" + carlineCode+" > "+ vdsCode+" >" + visStartRange);
				}
				uniqueFaceLiftFolderList = null;
				tableName = null;
				tabTokens =  null;
				newModelKey = null;
			}
			else
			{
				logger.info("getApplicableVINList :: LOCALE, CARLINE CODE, VDS CODE & VIS START RANGE AS PARAMETERS ARE NULL.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "getApplicableVINList()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			vdsCode = null;
			carlineCode=  null;
			locale= null;
			visStartRange=  null;
		}
		return applicableVinList;
	}

	public ArrayList<ApplicableVINList> getApplicableVINList(ArrayList<String> documentsList,String modelName, String carlineCode, String wmiCode, String language) throws SQLException
	{
		ArrayList<ApplicableVINList> applicableVinList = new ArrayList<ApplicableVINList>();
		Connection conn = null;
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			if(null!=documentsList && documentsList.size()>0)
			{
				String sql="";
				String documentId="";
				conn = getConnection();
				ApplicableVINList details = null;
				ApplicableVINList exist = null;
				boolean addToList = true;
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				for(int a=0;a<documentsList.size();a++)
				{
					documentId = String.valueOf(documentsList.get(a));
					if(null!=documentId && !"".equals(documentId))
					{
						sql = "SELECT DISTINCT vc_vin_vds_code, vc_vin_vis_start_range FROM "+tableName+" "
								+ "WHERE vc_vin_document_id='"+documentId.trim()+"' AND vc_vin_carline_code='"+carlineCode.trim()+"' "
										+ "AND vc_vin_model='"+modelName.trim()+"' AND vc_vin_wmi_code='"+wmiCode.trim()+"'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						while(rs.next())
						{
							addToList = true;
							details = new ApplicableVINList();
							details.setCarlineCode(carlineCode);
//							details.setWmiCode("-");
							details.setWmiCode(wmiCode);
							details.setVdsCode(rs.getString("vc_vin_vds_code"));
							details.setVisStartRange(rs.getString("vc_vin_vis_start_range"));

							if(null!=applicableVinList && applicableVinList.size()>0)
							{
								exist = null;
								for(int r=0;r<applicableVinList.size();r++)
								{
									exist = (ApplicableVINList)applicableVinList.get(r);
									if(null!=exist.getCarlineCode() && !"".equals(exist.getCarlineCode()) && null!=exist.getWmiCode() && !"".equals(exist.getWmiCode()) 
											&& null!=exist.getVdsCode() && !"".equals(exist.getVdsCode()) && null!=exist.getVisStartRange() && !"".equals(exist.getVisStartRange()) 
											&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) && null!=details.getWmiCode() && !"".equals(details.getWmiCode()) && 
											null!=details.getVdsCode() && !"".equals(details.getVdsCode()) && null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
									{
										if(exist.getCarlineCode().trim().toLowerCase().equals(details.getCarlineCode().trim().toLowerCase()) && 
												exist.getWmiCode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase()) && 
												exist.getVdsCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) && 
												exist.getVisStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
									exist = null;
								}
								exist = null;
							}


							if(addToList==true)
							{
								if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) &&  null!=details.getWmiCode() && !"".equals(details.getWmiCode()) && 
										null!=details.getVdsCode() && !"".equals(details.getVdsCode()) && null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
								{
									applicableVinList.add(details);
								}
							}
							details = null;
						}
						stmt.close();stmt = null;
						rs.close();rs= null;
					}
				}
				tableName = null;
			}
			else
			{
				logger.info("getApplicableVINList :: DOCUMENT IDS LIST / Model Name / Carline Code AS PARAMETERS ARE NULL.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "getApplicableVINList()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return applicableVinList;
	}



	/**
	 * Function will check whether CD Rom File exists for the FaceLift Folder or Not.
	 * @param faceLiftFolderPath
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> fetchCDRomDataOnFaceLift_old(String faceLiftFolderPath, String secondFaceLiftFolderPath) throws SQLException
	{
		ArrayList<String> documentIdsList = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath))
			{
				logger.info("fetchCDRomDataOnFaceLift :: Fetching for ################# :: > " + faceLiftFolderPath);
				conn = getConnection();
				/*
				 * IDENTIFY TABLE NAME
				 */
				String newModelKey = "\\"+ApplicationProperties.getProperty("cdrom.model.type.folder.key.newm");
				String tableName="";
				if(faceLiftFolderPath.trim().toLowerCase().contains(newModelKey.trim().toLowerCase()))
				{
					tableName = "gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					tableName = "gms3_dmt_mc_cd_data";
				}

				String sql="";

				if(null!=secondFaceLiftFolderPath && !"".equals(secondFaceLiftFolderPath))
				{
					sql="SELECT DISTINCT dc_im_doc_id FROM "+tableName +" WHERE "
							+ " TRIM(LOWER(dc_cd_txt_file_path)) LIKE'%"+faceLiftFolderPath.trim().toLowerCase()+"%' "
							+ " OR TRIM(LOWER(dc_cd_txt_file_path)) LIKE'%"+secondFaceLiftFolderPath.trim().toLowerCase()+"%'";
				}
				else
				{
					sql="SELECT DISTINCT dc_im_doc_id FROM "+tableName +" WHERE "
							+ " TRIM(LOWER(dc_cd_txt_file_path)) LIKE'%"+faceLiftFolderPath.trim().toLowerCase()+"%'";
				}


				logger.info("fetchCDRomDataOnFaceLift :: Sql ################ :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					documentIdsList.add(rs.getString("dc_im_doc_id"));
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;
				tableName=  null;
				newModelKey=  null;
			}
			else
			{
				logger.info("fetchCDRomDataOnFaceLift :: Face Lift Folder Path is null as parameter.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "fetchCDRomDataOnFaceLift()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			faceLiftFolderPath = null;
		}
		return documentIdsList;
	}

	public static ArrayList<String> fetchCDRomDataOnFaceLift(String manualTypeRefKey,String manualTypeName,String modelName,String carlineCode,String wmiCode, String vdsCode, String vinRange,String language) throws SQLException
	{
		ArrayList<String> documentIdsList = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=manualTypeRefKey && !"".equals(manualTypeRefKey) && null!=manualTypeName && !"".equals(manualTypeName) && null!=modelName && !"".equals(modelName) && null!=language && !"".equals(language) && 
					null!=carlineCode && !"".equals(carlineCode) && null!=wmiCode && !"".equals(wmiCode) && null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				logger.info("fetchCDRomDataOnFaceLift :: Fetching for ################# :: Locale :: >> "+language+" >> Manual Type >> " + manualTypeRefKey+" >> Model >>"+modelName+" >> Carline Code  >>"+ carlineCode+ ">> WMI >> "+ wmiCode+" >> VDS Code >>"+ vdsCode+" >> Vin Range >> "+ vinRange);
				conn = getConnection();

				/*
				 * new change added 12th november 2019
				 * Now fetch first document of each manual type and then identify its Facelift folder path
				 * once identified, fetch all document id for that facelift folder 
				 */

				String sql="";
				String manualTypeDocumentId=null;
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				if(!manualTypeRefKey.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_WD_REF_KEY")))
				{
					//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+manualTypeRefKey.trim()+"' AND "
					//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
					//							+ carlineCode.trim()
					//							+ "' AND vc_vin_wmi_code='"
					//							+ wmiCode.trim()
					//							+ "' AND vc_vin_vds_code='"
					//							+ vdsCode.trim()
					//							+ "' AND vc_vin_vis_start_range='"
					//							+ vinRange.trim()
					//							+ "'";

					sql = "select vc_vin_document_id from "+tableName+" WHERE vc_vin_document_type ='"+manualTypeRefKey.trim()+"' AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_wmi_code='"
							+ wmiCode.trim()
							+ "' AND vc_vin_vds_code='"
							+ vdsCode.trim()
							+ "' AND vc_vin_vis_start_range='"
							+ vinRange.trim()
							+ "' order by  vc_vin_document_id desc LIMIT 1";
				}
				else
				{
					// WD / E-WD
					//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+manualTypeRefKey.trim()+"' AND vc_vin_document_type_name='"+manualTypeName.trim()+"' AND "
					//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
					//							+ carlineCode.trim()
					//							+ "' AND vc_vin_wmi_code='"
					//							+ wmiCode.trim()
					//							+ "' AND vc_vin_vds_code='"
					//							+ vdsCode.trim()
					//							+ "' AND vc_vin_vis_start_range='"
					//							+ vinRange.trim()
					//							+ "'";
					sql = "select vc_vin_document_id from "+tableName+" WHERE vc_vin_document_type ='"+manualTypeRefKey.trim()+"' AND vc_vin_document_type_name='"+manualTypeName.trim()+"' AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_wmi_code='"
							+ wmiCode.trim()
							+ "' AND vc_vin_vds_code='"
							+ vdsCode.trim()
							+ "' AND vc_vin_vis_start_range='"
							+ vinRange.trim()
							+ "' order by  vc_vin_document_id desc LIMIT 1";
				}
				logger.info("fetchCDRomDataOnFaceLift :: Sql ################ :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				ArrayList<String> tempDocumentsList = new ArrayList<String>();
				if(rs.next())
				{
					if(null!=rs.getString("vc_vin_document_id") && !"".equals(rs.getString("vc_vin_document_id")))
					{
						manualTypeDocumentId =rs.getString("vc_vin_document_id"); 
					}
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;

				/*
				 * NOW ITERATE AND CHECK IF CD ROM DATA EXISTS FOR THE IDENTIFIED DOCUMENTS OR NOT
				 * IF YES, THEN ONLY ADD TO DOCUMENTS LIST
				 */
				if(null!=manualTypeDocumentId && !"".equals(manualTypeDocumentId))
				{
					/*
					 * NOW FETCH FACELIFT FOLDER PATH FOR THIS DOCUMENT
					 */
					String tableNames=null;
					if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
							language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
					{
						// MC MARKET 
						tableNames="gms3_dmt_mc_newm_imdoc,gms3_dmt_mc_imdoc";
					}
					else
					{
						// MME MARKET
						tableNames = "gms3_dmt_"+Utilities.tableLocale(language)+"_nm_imdoc";
						tableNames+=",gms3_dmt_"+Utilities.tableLocale(language)+"_imdoc";
					}
					String[] tok = tableNames.split(",");
					boolean docFound = false;
					String faceLiftFolderPath=null;
					String documentTableName=null;
					if(null!=manualTypeDocumentId && !"".equals(manualTypeDocumentId))
					{
						for(int b=0;b<tok.length;b++)
						{
							docFound= false;
							sql= "select dc_source_network_loc from "+tok[b]+" WHERE dc_im_doc_id='"+manualTypeDocumentId+"'";
							stmt = conn.createStatement();
							rs = stmt.executeQuery(sql);
							if(rs.next())
							{
								if(null!=rs.getString("dc_source_network_loc") && !"".equals(rs.getString("dc_source_network_loc")))
								{
									faceLiftFolderPath = rs.getString("dc_source_network_loc");
									// document found
									docFound=  true;
									// set table name
									documentTableName = tok[b];
								}
							}
							stmt.close();stmt = null;
							rs.close();rs = null;
							sql = null;

							if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath))
							{
								String[] pathTok = faceLiftFolderPath.split("\\\\");
								if(null!=pathTok && pathTok.length>0)
								{
									faceLiftFolderPath = pathTok[0]+"\\"+pathTok[1]+"\\"+pathTok[2]+"\\"+pathTok[3]+"\\";
								}
								pathTok = null;
							}
							if(docFound==true)
							{
								break;
							}
						}
					}

					logger.info("fetchCDRomDataOnFaceLift ::  FaceLiftFolderPath :: > "+ faceLiftFolderPath);
					logger.info("fetchCDRomDataOnFaceLift ::  DocumentTableName :: > "+ documentTableName);
					/*
					 * NOW FETCH ALL DOCUMENT ID FOR THE FACELIFT FOLDER
					 */
					if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath) && null!=documentTableName && !"".equals(documentTableName))
					{
						tempDocumentsList = new ArrayList<String>();
						sql = "SELECT dc_im_doc_id FROM "+documentTableName+" WHERE TRIM(LOWER(dc_source_network_loc)) LIKE '"+faceLiftFolderPath.trim().trim().toLowerCase()+"%' AND "
								+ "dc_doc_status='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
						logger.info("fetchCDRomDataOnFaceLift :: Get All Documents Sql :: >"+ sql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						while(rs.next())
						{
							if(null!=rs.getString("dc_im_doc_id") && !"".equals(rs.getString("dc_im_doc_id")))
							{
								// add to tempDocList
								tempDocumentsList.add(rs.getString("dc_im_doc_id"));
							}
						}
						stmt.close();stmt = null;
						rs.close();rs = null;
						sql  =null;
						
						
						/*
						 * NOW CHECK FOR EACH DOCUMENT, IF AVAILABLE IN CD DATA OR NOT
						 */
						if(null!=tempDocumentsList && tempDocumentsList.size()>0)
						{
							tableNames=null;
							if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
									language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
							{
								// MC MARKET 
								tableNames="gms3_dmt_mc_newm_cd_data,gms3_dmt_mc_cd_data";
							}
							else
							{
								// MME MARKET
								tableNames = "gms3_dmt_"+Utilities.tableLocale(language)+"_nm_cd_data";
								tableNames+=",gms3_dmt_"+Utilities.tableLocale(language)+"_cd_data";
							}
							
							
							tok = tableNames.split(",");
							docFound = false;;
							for(int a=0;a<tempDocumentsList.size();a++)
							{
								for(int b=0;b<tok.length;b++)
								{
									docFound= false;
									sql = "SELECT dc_cd_id FROM "+tok[b]+" WHERE dc_im_doc_id='"+tempDocumentsList.get(a).toString().trim()+"' AND dc_sequence_no IS NOT NULL";
									//							logger.info("fetchCDRomDataOnFaceLift :: Veifying CD Data for Document SQL :: > "+ sql);
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									if(rs.next())
									{
										if(rs.getLong("dc_cd_id")>0)
										{
											// add document to documentsList
											docFound=  true;
										}
									}
									stmt.close();stmt = null;
									rs.close();rs= null;
									sql  =null;
									if(docFound==true)
									{
										documentIdsList.add(tempDocumentsList.get(a).toString().trim());
										break;
									}
								}
							}
						}
					}
					tableNames=  null;
					tok = null;
					faceLiftFolderPath =  null;
					docFound = false;
					documentTableName = null;
				}
				manualTypeDocumentId = null;
				tempDocumentsList = null;
			}
			else
			{
				logger.info("fetchCDRomDataOnFaceLift :: Locale / Manual Type / Model / Carline / WMI / VDS / VIN Range are null as parameter.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "fetchCDRomDataOnFaceLift()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return documentIdsList;
	}

	/**
	 * Fetch Documents for Engine / Mission from CD ROM On the basis of Locale and Book Code (e.g. Manual Type) 
	 * @param faceLiftFolderPath
	 * @param secondFaceLiftFolderPath
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> fetchCDRomDataOnBookCodes_old(String bookCode, String locale) throws SQLException
	{
		ArrayList<String> documentIdsList = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=bookCode  && !"".equals(bookCode) && null!=locale && !"".equals(locale))
			{
				logger.info("fetchCDRomDataOnBookCodes :: Fetching for ################# :: > " + bookCode);
				conn = getConnection();
				/*
				 * CHECK IN BOTH OLD & NEW TABLES
				 */
				String tableName="gms3_dmt_mc_newm_cd_data,gms3_dmt_mc_cd_data";
				String[] tabTokens=tableName.split(",");

				String sql="";

				for(int a=0;a<tabTokens.length;a++)
				{
					sql="SELECT DISTINCT dc_im_doc_id FROM "+tabTokens[a].toString()+" WHERE TRIM(LOWER(dc_cd_manualtype_folder))='"+bookCode.trim().toLowerCase()+"' ";
					logger.info("fetchCDRomDataOnBookCodes :: Sql ################ :: > " + sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						boolean add = true;
						if(null!=documentIdsList && documentIdsList.size()>0)
						{
							for(String existDoc : documentIdsList)
							{
								if(null!=existDoc && null!=rs.getString("dc_im_doc_id") && !"".equals(rs.getString("dc_im_doc_id")) && !"".equals(existDoc))
								{
									if(existDoc.trim().equals(rs.getString("dc_im_doc_id").trim()))
									{
										// already added
										add = false;
										break;
									}
								}
								existDoc = null;
							}
						}

						if(add == true)
						{
							documentIdsList.add(rs.getString("dc_im_doc_id"));
						}
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
					sql = null;
				}
				tableName=  null;
				tabTokens = null;
				sql = null;
			}
			else
			{
				logger.info("fetchCDRomDataOnBookCodes :: Book Code & Locale are null as parameter.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "fetchCDRomDataOnBookCodes()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			bookCode = null;
			locale = null;
		}
		return documentIdsList;
	}

	public static ArrayList<String> fetchCDRomDataOnBookCodes_08March2020BackUp(String bookCode, String manualTypeRefKey,String modelName,String carlineCode, String wmiCode,String vdsCode, String vinRange) throws SQLException
	{
		ArrayList<String> documentIdsList = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=bookCode  && !"".equals(bookCode) && null!=manualTypeRefKey && !"".equals(manualTypeRefKey) && null!=modelName && !"".equals(modelName) && 
					null!=carlineCode && !"".equals(carlineCode) && null!=wmiCode && !"".equals(wmiCode) && null!=vdsCode && !"".equals(vdsCode) && null!=vinRange && !"".equals(vinRange))
			{
				logger.info("fetchCDRomDataOnBookCodes :: Fetching for ################# :: Book Code >> "+bookCode+" >> Manual Type >> " + manualTypeRefKey+" >> Model >>"+modelName+" >> Carline Code  >>"+ carlineCode+ ">> WMI >> "+ wmiCode+" >> VDS Code >>"+ vdsCode+" >> Vin Range >> "+ vinRange);

				conn = getConnection();
				/*
				 * new change added 12th november 2019
				 * Now fetch first document of each manual type and then identify its Facelift folder path
				 * once identified, fetch all document id for that facelift folder 
				 */
				String sql="";
				String manualTypeDocumentId=null;
				if(manualTypeRefKey.equals("ENGINE"))
				{
//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY").trim()+"' AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_engine_book_code='"+bookCode.trim()+"' ";
					
					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY").trim()+"' AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_wmi_code='"
							+ wmiCode.trim()
							+ "' AND vc_vin_vds_code='"
							+ vdsCode.trim()
							+ "' AND vc_vin_vis_start_range='"
							+ vinRange.trim()
							+ "' AND vc_vin_engine_book_code='"+bookCode.trim()+"' order by  vc_vin_document_id desc LIMIT 1";
				}
				else
				{
					// MT & AT BOTH
//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
//							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_mission_book_code='"+bookCode.trim()+"' ";
					sql = "select vc_vin_document_id from gms3_vc_japan_vin_details WHERE (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_wmi_code='"
							+ wmiCode.trim()
							+ "' AND vc_vin_vds_code='"
							+ vdsCode.trim()
							+ "' AND vc_vin_vis_start_range='"
							+ vinRange.trim()
							+ "' AND vc_vin_mission_book_code='"+bookCode.trim()+"' order by  vc_vin_document_id desc LIMIT 1";
				}
				logger.info("fetchCDRomDataOnBookCodes :: Sql ################ :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				ArrayList<String> tempDocumentsList = new ArrayList<String>();
				while(rs.next())
				{
					if(null!=rs.getString("vc_vin_document_id") && !"".equals(rs.getString("vc_vin_document_id")))
					{
						manualTypeDocumentId =rs.getString("vc_vin_document_id"); 
					}
				}
				stmt.close();stmt=null;
				rs.close();rs= null;
				sql = null;

				
				if(null!=manualTypeDocumentId && !"".equals(manualTypeDocumentId))
				{
					/*
					 * NOW FETCH FACELIFT FOLDER PATH FOR THIS DOCUMENT
					 */
					String tableNames="gms3_dmt_mc_newm_imdoc,gms3_dmt_mc_imdoc";
					String[] tok = tableNames.split(",");
					boolean docFound = false;
					String faceLiftFolderPath=null;
					String documentTableName=null;
					if(null!=manualTypeDocumentId && !"".equals(manualTypeDocumentId))
					{
						for(int b=0;b<tok.length;b++)
						{
							docFound= false;
							sql= "select dc_source_network_loc from "+tok[b]+" WHERE dc_im_doc_id='"+manualTypeDocumentId+"'";
							stmt = conn.createStatement();
							rs = stmt.executeQuery(sql);
							if(rs.next())
							{
								if(null!=rs.getString("dc_source_network_loc") && !"".equals(rs.getString("dc_source_network_loc")))
								{
									faceLiftFolderPath = rs.getString("dc_source_network_loc");
									// document found
									docFound=  true;
									// set table name
									documentTableName = tok[b];
								}
							}
							stmt.close();stmt = null;
							rs.close();rs = null;
							sql = null;

							if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath))
							{
								String[] pathTok = faceLiftFolderPath.split("\\\\");
								if(null!=pathTok && pathTok.length>0)
								{
									faceLiftFolderPath = pathTok[0]+"\\"+pathTok[1]+"\\"+pathTok[2]+"\\"+pathTok[3]+"\\";
								}
								pathTok = null;
							}
							if(docFound==true)
							{
								break;
							}
						}
					}

					logger.info("fetchCDRomDataOnBookCodes ::  FaceLiftFolderPath :: > "+ faceLiftFolderPath);
					logger.info("fetchCDRomDataOnBookCodes ::  DocumentTableName :: > "+ documentTableName);
					/*
					 * NOW FETCH ALL DOCUMENT ID FOR THE FACELIFT FOLDER
					 */
					if(null!=faceLiftFolderPath && !"".equals(faceLiftFolderPath) && null!=documentTableName && !"".equals(documentTableName))
					{
						tempDocumentsList = new ArrayList<String>();
						sql = "SELECT dc_im_doc_id FROM "+documentTableName+" WHERE dc_source_network_loc LIKE '"+faceLiftFolderPath+"%' AND dc_doc_status='"+ApplicationProperties.getProperty("flag.value.active")+"'";
						logger.info("fetchCDRomDataOnBookCodes :: Get All Documents Sql :: >"+ sql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						while(rs.next())
						{
							if(null!=rs.getString("dc_im_doc_id") && !"".equals(rs.getString("dc_im_doc_id")))
							{
								// add to tempDocList
								tempDocumentsList.add(rs.getString("dc_im_doc_id"));
							}
						}
						stmt.close();stmt = null;
						rs.close();rs = null;
						sql  =null;
						
						/*
						 * NOW ITERATE AND CHECK IF CD ROM DATA EXISTS FOR THE IDENTIFIED DOCUMENTS OR NOT
						 * IF YES, THEN ONLY ADD TO DOCUMENTS LIST
						 */
						if(null!=tempDocumentsList && tempDocumentsList.size()>0)
						{
							tableNames="gms3_dmt_mc_newm_cd_data,gms3_dmt_mc_cd_data";
							tok = tableNames.split(",");
							docFound = false;;
							for(int a=0;a<tempDocumentsList.size();a++)
							{
								for(int b=0;b<tok.length;b++)
								{
									docFound= false;
									sql = "SELECT dc_cd_id FROM "+tok[b]+" WHERE dc_im_doc_id='"+tempDocumentsList.get(a).toString().trim()+"' AND dc_sequence_no IS NOT NULL";
									//							logger.info("fetchCDRomDataOnBookCodes :: Veifying CD Data for Document SQL :: > "+ sql);
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									if(rs.next())
									{
										if(rs.getLong("dc_cd_id")>0)
										{
											// add document to documentsList
											docFound=  true;
										}
									}
									stmt.close();stmt = null;
									rs.close();rs= null;
									sql  =null;
									if(docFound==true)
									{
										documentIdsList.add(tempDocumentsList.get(a).toString().trim());
										break;
									}
								}
							}
							tableNames=  null;
							tok = null;
						}
					}
					faceLiftFolderPath = null;
					documentTableName = null;
				}
				manualTypeDocumentId = null;
				tempDocumentsList = null;
			}
			else
			{
				logger.info("fetchCDRomDataOnBookCodes :: Book Code / Manual Type / Model / Carline Code / WMI / VDS / VIN Range are null as parameter.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "fetchCDRomDataOnBookCodes()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			bookCode = null;
		}
		return documentIdsList;
	}

	/**
	 * 
	 * IN-191220-2968
	 * FETCH ALL DOCUMENTS FROM VIEW CONTENT TABLE BASED ON BOOK CODE, MODEL, CARLINE CODE
	 * @param bookCode
	 * @param manualTypeRefKey
	 * @param modelName
	 * @param carlineCode
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> fetchCDRomDataOnBookCodes(String bookCode, String manualTypeRefKey,String modelName,String carlineCode,String language) throws SQLException
	{
		ArrayList<String> documentIdsList = new ArrayList<String>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=bookCode  && !"".equals(bookCode) && null!=manualTypeRefKey && !"".equals(manualTypeRefKey) && null!=modelName && !"".equals(modelName) && 
					null!=carlineCode && !"".equals(carlineCode) && null!=language && !"".equals(language))
			{
				logger.info("fetchCDRomDataOnBookCodes :: Fetching for ################# :: Locale :: >> "+language+" >> Book Code >> "+bookCode+" >> Manual Type >> " + manualTypeRefKey+" >> Model >>"+modelName+" >> Carline Code  >>"+ carlineCode);

				conn = getConnection();
				String tableName=null;
				if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_vc_japan_vin_details";
				}
				else
				{
					// MME MARKET LOCALE
					tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(language);
				}
				/*
				 * IN-191220-2968
				 * FETCH ALL DOCUMENTS FROM VIEW CONTENT TABLE BASED ON BOOK CODE, MODEL, CARLINE CODE
				 */
				String sql="";
				if(manualTypeRefKey.equals("ENGINE"))
				{
//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY").trim()+"' AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_engine_book_code='"+bookCode.trim()+"' ";
					
//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE vc_vin_document_type ='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY").trim()+"' AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_engine_book_code='"+bookCode.trim()+"' order by  vc_vin_document_id desc FETCH FIRST  1 ROW only";
					sql = "select DISTINCT vc_vin_document_id from "+tableName+" WHERE "
							+ "vc_vin_document_type IN ('"+ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY").trim()+"',"
									+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY")+"',"
									+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY")+"',"
									+ "'"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY")+"') AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_engine_book_code='"+bookCode.trim()+"' ";
				}
				else
				{
					// MT & AT BOTH
//					sql = "select DISTINCT vc_vin_document_id from gms3_vc_japan_vin_details WHERE (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
//							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_mission_book_code='"+bookCode.trim()+"' ";
//					sql = "select vc_vin_document_id from gms3_vc_japan_vin_details WHERE (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
//							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') AND "
//							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
//							+ carlineCode.trim()
//							+ "' AND vc_vin_wmi_code='"
//							+ wmiCode.trim()
//							+ "' AND vc_vin_vds_code='"
//							+ vdsCode.trim()
//							+ "' AND vc_vin_vis_start_range='"
//							+ vinRange.trim()
//							+ "' AND vc_vin_mission_book_code='"+bookCode.trim()+"' order by  vc_vin_document_id desc FETCH FIRST  1 ROW only";
					sql = "select DISTINCT vc_vin_document_id from "+tableName+" WHERE (vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")+"' "
							+ " OR vc_vin_document_type='"+ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")+"') AND "
							+ " vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
							+ carlineCode.trim()
							+ "' AND vc_vin_mission_book_code='"+bookCode.trim()+"' ";
				}
				logger.info("fetchCDRomDataOnBookCodes :: Sql ################ :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				ArrayList<String> tempDocumentsList = new ArrayList<String>();
				boolean add=true;
				while(rs.next())
				{
					if(null!=rs.getString("vc_vin_document_id") && !"".equals(rs.getString("vc_vin_document_id")))
					{
						add = true;
						if(null!=tempDocumentsList && tempDocumentsList.size()>0)
						{
							for(String existDoc : documentIdsList)
							{
								if(null!=existDoc && !"".equals(existDoc))
								{
									if(existDoc.trim().equals(rs.getString("vc_vin_document_id").trim()))
									{
										// already added
										add = false;
										break;
									}
								}
								existDoc = null;
							}
						}

						if(add == true)
						{
							tempDocumentsList.add(rs.getString("vc_vin_document_id"));
						}
					}
				}
				stmt.close();stmt=null;
				rs.close();rs= null;
				sql = null;

				/*
				 * NOW ITERATE AND CHECK IF CD ROM DATA EXISTS FOR THE IDENTIFIED DOCUMENTS OR NOT
				 * IF YES, THEN ONLY ADD TO DOCUMENTS LIST
				 */
				if(null!=tempDocumentsList && tempDocumentsList.size()>0)
				{
					String tableNames="";
					tableNames=null;
					if(language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
							language.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
					{
						// MC MARKET 
						tableNames="gms3_dmt_mc_newm_cd_data,gms3_dmt_mc_cd_data";
					}
					else
					{
						// MME MARKET
						tableNames = "gms3_dmt_"+Utilities.tableLocale(language)+"_nm_cd_data";
						tableNames+=",gms3_dmt_"+Utilities.tableLocale(language)+"_cd_data";
					}
					String[] tok = tableNames.split(",");
					boolean docFound = false;;
					for(int a=0;a<tempDocumentsList.size();a++)
					{
						for(int b=0;b<tok.length;b++)
						{
							docFound= false;
							sql = "SELECT dc_cd_id FROM "+tok[b]+" WHERE dc_im_doc_id='"+tempDocumentsList.get(a).toString().trim()+"' AND dc_sequence_no IS NOT NULL";
							//							logger.info("fetchCDRomDataOnBookCodes :: Veifying CD Data for Document SQL :: > "+ sql);
							stmt = conn.createStatement();
							rs = stmt.executeQuery(sql);
							if(rs.next())
							{
								if(rs.getLong("dc_cd_id")>0)
								{
									// add document to documentsList
									docFound=  true;
								}
							}
							stmt.close();stmt = null;
							rs.close();rs= null;
							sql  =null;
							if(docFound==true)
							{
								documentIdsList.add(tempDocumentsList.get(a).toString().trim());
								break;
							}
						}
					}
					tableNames=  null;
					tok = null;
				}
				tempDocumentsList = null;
			}
			else
			{
				logger.info("fetchCDRomDataOnBookCodes :: Locale / Book Code / Manual Type / Model / Carline Code are null as parameter.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "fetchCDRomDataOnBookCodes()", e);
		}
		finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
			bookCode = null;
		}
		return documentIdsList;
	}
	
	/**
	 * NEW FUNCTION ADDED TO IDENTIFY SELECTED BOOK CODES FOR THE SELECTED VIN
	 * @param locale
	 * @param modelName
	 * @param carlineCode
	 * @param wmiCode
	 * @param vdsCode
	 * @param visStartRange
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getSelectedBooks(String locale, String modelName,String carlineCode,String wmiCode,String vdsCode,String visStartRange,String type) throws SQLException
	{
		List<String> booksList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			/*
			 * LOGIC 1 - 
			 * FETCH UNIQUE MATERIAL FOLDER LIST ON THE BASIS OF SELECTED VIN 
			 * 	FROM VIN TABLES (DMT)
			 * FOR THE IDENTIFIED MATERIAL FOLDERS, FIND ALL VINS
			 * FETCH ALL UNIQUE ENGINE TYPE / MISSION TYPE FOR EACH VIN
			 * FIND ENGINE BOOK / MISSION BOOK FOR THE IDENTIFED ENGINE / MISSION TYPES
			 * SET IN SELECTED VINS	
			 */
			String[] tables=null;
			if(locale.replace("-", "_").toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").toLowerCase()))
			{
				// MC MARKET
				tables = "gms3_dmt_mc_newm_vin,gms3_dmt_mc_vin".split(",");
			}
			else
			{
				// MME MARKET
				String tabs ="gms3_dmt_"+locale.replace("-", "_").toLowerCase()+"_nm_vin,gms3_dmt_"+locale.replace("-", "_").toLowerCase()+"_VIN"; 
				tables = tabs.split(",");
				tabs = null;
			}
			String sql  =null;


			List<String> tempMatFolderList = new ArrayList<String>();
			if(null!=tables && tables.length>0)
			{
				String sourceLoc = null;
				for(int a=0;a<tables.length;a++)
				{
					if(locale.replace("-", "_").toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").toLowerCase()))
					{
						// MC MARKET
						sql = "SELECT DISTINCT dc_source_network_loc FROM "+ tables[a]+" WHERE dc_vin_carline_code=? AND  "
								+ "dc_vin_vds_code=? AND dc_vin_vis_start_range = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, carlineCode);
						pstmt.setString(2, vdsCode);
						pstmt.setString(3, visStartRange);
					}
					else
					{
						// MME MARKET
						sql = "SELECT DISTINCT dc_source_network_loc FROM "+ tables[a]+" WHERE dc_vin_carline_code=? AND dc_vin_wmi_code = ? AND "
								+ "dc_vin_vds_code=? AND dc_vin_vis_start_range = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, carlineCode);
						pstmt.setString(2, wmiCode);
						pstmt.setString(3, vdsCode);
						pstmt.setString(4, visStartRange);

					}
					rs = pstmt.executeQuery();
					while(rs.next())
					{
						if(null!=rs.getString("dc_source_network_loc") && !"".equals(rs.getString("dc_source_network_loc")))
						{
							sourceLoc=rs.getString("dc_source_network_loc");
						}

						if(null!=sourceLoc && !"".equals(sourceLoc))
						{
							// remove file name (e.g. anc.ent etc)
							if(sourceLoc.lastIndexOf("\\")!=-1)
							{
								sourceLoc = sourceLoc.substring(0, sourceLoc.lastIndexOf("\\"));
							}

							if(null!=sourceLoc && !"".equals(sourceLoc))
							{
								// remove processing folderName (html / html5/ ent / etc.)
								if(sourceLoc.lastIndexOf("\\")!=-1)
								{
									sourceLoc = sourceLoc.substring(0, sourceLoc.lastIndexOf("\\"));
									if(null!=sourceLoc && !"".equals(sourceLoc))
									{
										boolean add = true;

										if(null!=tempMatFolderList && tempMatFolderList.size()>0)
										{
											for(int b=0;b<tempMatFolderList.size();b++)
											{
												if(tempMatFolderList.get(b).toString().trim().toLowerCase().equals(sourceLoc.trim().toLowerCase()))
												{
													// already added
													add = false;
													break;
												}
											}
										}

										if(add==true)
										{
											/*
											 * add to unique folders List
											 */
											if(null==tempMatFolderList || tempMatFolderList.size()<=0)
											{
												tempMatFolderList= new ArrayList<String>();
											}
											tempMatFolderList.add(sourceLoc);
										}
									}
								}
							}
						}
						sourceLoc = null;
					}
					rs.close();rs=null;
					pstmt.close();pstmt = null;
					sql  =null;
				}
			}

			logger.info("getSelectedBooks :: Logic 1 :: Material Folders List Found for "+ carlineCode+" > "+ wmiCode+" > "+ vdsCode+" > "+ visStartRange +" > are :: >"+ tempMatFolderList.size());

			/*
			 * STEP 2 - IDENTIFY ALL VINS AGAINST IDENTIFIED UNIQUE MATERIAL FOLDER
			 */
			if(null!=tempMatFolderList && tempMatFolderList.size()>0 && null!=tables && tables.length>0)
			{
				String japanLocale=ApplicationProperties.getProperty("ja_jp").toLowerCase();
				List<VinDetails> vinList = new ArrayList<VinDetails>();
				for(int a=0;a<tempMatFolderList.size();a++)
				{
					VinDetails details= null;
					VinDetails exist = null;
					for(int b=0;b<tables.length;b++)
					{
						if(locale.replace("-", "_").toLowerCase().equals(japanLocale))
						{
							// MC MARKET
							sql = "SELECT DISTINCT dc_vin_carline_code, dc_vin_vds_code, "
									+ "dc_vin_vis_start_range FROM "+tables[b]+ " WHERE dc_source_network_loc LIKE '"+tempMatFolderList.get(a)+"%'";
						}
						else
						{
							// MME MARKET
							sql = "SELECT DISTINCT dc_vin_carline_code, dc_vin_wmi_code,dc_vin_vds_code, "
									+ "dc_vin_vis_start_range FROM "+tables[b]+ " WHERE dc_source_network_loc LIKE '"+tempMatFolderList.get(a)+"%'";
						}
						logger.info("getSelectedBooks :: Logic 1 :: GET VINS for Each Material Folder :: >"+ sql);
						pstmt =conn.prepareStatement(sql);
						rs = pstmt.executeQuery();
						while(rs.next())
						{
							details = new VinDetails();
							details.setCarlineCode(rs.getString("dc_vin_carline_code"));
							if(locale.replace("-", "_").toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").toLowerCase()))
							{
								// MC MARKER
								details.setWmiCode("-");
							}
							else
							{
								// MME MARKET
								details.setWmiCode(rs.getString("dc_vin_wmi_code"));
							}
							details.setVdsCode(rs.getString("dc_vin_vds_code"));
							details.setVisStartRange(rs.getString("dc_vin_vis_start_range"));

							boolean add = true;
							if(null!=vinList && vinList.size()>0)
							{
								exist = null;
								for(int r=0;r<vinList.size();r++)
								{
									exist = (VinDetails)vinList.get(r);
									if(exist.getCarlineCode().trim().toLowerCase().equals(details.getCarlineCode().trim().toLowerCase()) && 
											exist.getWmiCode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase()) && 
											exist.getVdsCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) && 
											exist.getVisStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()))
									{

										add = false;
										break;
									}
									exist = null;
								}

							}

							if(add==true)
							{
								vinList.add(details);
							}
							details = null;
						}
						rs.close();rs=null;
						pstmt.close();pstmt = null;
						sql = null;		
					}
				}

				if(null!=vinList && vinList.size()>0)
				{
					logger.info("getSelectedBooks :: Logic 1 :: Total VINs found for the identified Material Folders are :: >"+ vinList.size());
					/*
					 * FETCH UNIQUE ENGINE / MISSION TYPE FOR ALL THE IDENTIFIED VINS
					 */
					VinDetails details = null;
					sql=null;
					String columnName=null;
					List<String> itemsList=new ArrayList<String>();
					String[] types=null;
					for(int a=0;a<vinList.size();a++)
					{
						details = (VinDetails)vinList.get(a);
						if(type.equals("ENGINE"))
						{
							columnName = "mdm_vin_engine_code";
						}
						else
						{
							columnName = "mdm_vin_transmission_code";
						}
						sql = "SELECT DISTINCT "+columnName+" AS ITEM_TYPE FROM gms3_mdm_vin_detail WHERE mdm_crln_code = ? AND mdm_vin_wmi_code=? AND mdm_vin_vds_code= ? AND mdm_vin_vis_start_range = ? AND "
								+ " TRIM(LOWER(mdm_ml_lang_code)) = ? AND mdm_vin_flag = '1'";
						pstmt=  conn.prepareStatement(sql);
						pstmt.setString(1, details.getCarlineCode());
						pstmt.setString(2, details.getWmiCode());
						pstmt.setString(3, details.getVdsCode());
						pstmt.setString(4, details.getVisStartRange());
						pstmt.setString(5, locale.replace("_", "-").toLowerCase());
						rs = pstmt.executeQuery();
						while(rs.next())
						{
							if(null!=rs.getString("ITEM_TYPE") && !"".equals(rs.getString("ITEM_TYPE")))
							{
								types = rs.getString("ITEM_TYPE").split(",");
								if(null!=types && types.length>0)
								{
									for(int y=0;y<types.length;y++)
									{
										boolean add = true;
										if(null!=itemsList && itemsList.size()>0)
										{
											for(int t=0;t<itemsList.size();t++)
											{
												if(itemsList.get(t).toString().equals(types[y].toString()))
												{
													// already added
													add = false;
													break;
												}
											}
										}

										if(add==true)
										{
											itemsList.add(types[y].toString());
										}
									}
								}
								types = null;
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql = null;
						details = null;
						columnName = null;
					}


					if(null!=itemsList && itemsList.size()>0)
					{
						logger.info("getSelectedBooks :: Logic 1 :: Total "+type+" TYPE found for the identified VINs for the Identified Material Folders are :: >"+ itemsList.size());
						/*
						 * ITERATE ITEMS LIST AND FETCH ALL THE APPLICABLE BOOKS
						 */
						for(int a=0;a<itemsList.size();a++)
						{
							if(type.equals("ENGINE"))
							{
								sql = "SELECT B.mdm_eb_code AS BOOK_CODE FROM gms3_mdm_engine_type A, gms3_mdm_engine_book B, gms3_mdm_manual_language C "
										+ " WHERE A.mdm_eb_id = B.mdm_eb_id AND A.mdm_et_flag = '1' AND B.mdm_eb_flag='1' AND A.mdm_ml_id = C.mdm_ml_id AND "
										+ " C.mdm_ml_flag='1' AND TRIM(LOWER(C.mdm_ml_lang_code)) = '"+locale.replace("_", "-").toLowerCase()+"' "
												+ "AND A.mdm_et_type_code = '"+itemsList.get(a)+"' ";
							}
							else
							{
								sql = "SELECT B.mdm_transbk_code AS BOOK_CODE FROM gms3_mdm_trans_type A, gms3_mdm_trans_book B, gms3_mdm_manual_language C "
										+ " WHERE A.mdm_transbk_id = B.mdm_transbk_id AND A.mdm_trans_type_flag = '1' AND B.mdm_transbk_flag='1' AND A.mdm_ml_id = C.mdm_ml_id AND "
										+ " C.mdm_ml_flag='1' AND TRIM(LOWER(C.mdm_ml_lang_code)) = '"+locale.replace("_", "-").toLowerCase()+"' AND A.mdm_trans_type_type_code = '"+itemsList.get(a)+"' ";
							}
							logger.info("getSelectedBooks :: Logic 1 :: Get Books Sql : >" + sql);
							pstmt = conn.prepareStatement(sql);
							rs=  pstmt.executeQuery();
							while(rs.next())
							{
								if(null!=rs.getString("BOOK_CODE") && !"".equals(rs.getString("BOOK_CODE")))
								{
									boolean add = true;
									if(null!=booksList && booksList.size()>0)
									{
										for(int r=0;r<booksList.size();r++)
										{
											if(booksList.get(r).trim().toString().equals(rs.getString("BOOK_CODE").trim()))
											{
												// already added
												add = false;
												break;
											}
										}
									}

									if(add==true)
									{
										if(null==booksList || booksList.size()<=0)
										{
											booksList = new ArrayList<String>();
										}
										booksList.add(rs.getString("BOOK_CODE").trim());
									}
								}
							}
							pstmt.close();pstmt=null;
							rs.close();rs=null;
							sql = null;
						}
					}
					else
					{
						logger.info("getSelectedBooks :: Logic 1 :: No "+type+" TYPE found for the identified VINs for the Identified Material Folders.");
					}
				}
				else
				{
					logger.info("getSelectedBooks :: Logic 1 :: No VINs found for the identified Material Folders.");
				}
				japanLocale = null;
			}
			else
			{
				logger.info("getSelectedBooks :: Logic 1 :: NO Material Folders List Found for "+ carlineCode+" > "+ wmiCode+" > "+ vdsCode+" > "+ visStartRange +".");
			}
			tempMatFolderList = null;

			if(null!=booksList && booksList.size()>0)
			{
				logger.info("getSelectedBooks :: Logic 1 :: Total Books Found are :: >"+ booksList.size());
			}
			else
			{
				logger.info("getSelectedBooks :: Logic 1 :: No Books Found.");
			}


			/*
			 * PROCEED FOR LOGIC 2
			 * FETCH ALL THE BOOK CODES FROM VIEW CONTENT TABLE FOR THE SELECTED VIN
			 * AND ADD THEM TO BOOKS LIST
			 */
			sql = null;
			String tableName=null;
			String colName=null;
			if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				tableName = "gms3_vc_japan_vin_details";
			}
			else
			{
				// MME MARKET LOCALE
				tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(locale);
			}

			if(type.equals("ENGINE"))
			{
				colName = "vc_vin_engine_book_code";
			}
			else
			{
				colName= "vc_vin_mission_book_code";
			}


			sql = "Select DISTINCT "+colName+" AS BOOK_CODE  from "+tableName+" where "
					+ "  vc_vin_model ='"+modelName.trim()+"' AND vc_vin_carline_code='"
					+ carlineCode.trim()
					+ "' AND vc_vin_wmi_code='"
					+ wmiCode.trim()
					+ "' AND vc_vin_vds_code='"
					+ vdsCode.trim()
					+ "' AND vc_vin_vis_start_range='"
					+ visStartRange.trim()
					+ "'";
			logger.info("getSelectedBooks :: LOGIC 2 :: Sql :: >"+ sql);
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				if(null!=rs.getString("BOOK_CODE") && !"".equals(rs.getString("BOOK_CODE")))
				{
					boolean add = true;
					if(null!=booksList && booksList.size()>0)
					{
						for(int r=0;r<booksList.size();r++)
						{
							if(booksList.get(r).trim().toString().equals(rs.getString("BOOK_CODE").trim()))
							{
								// already added
								add = false;
								break;
							}
						}
					}

					if(add==true)
					{
						if(null==booksList || booksList.size()<=0)
						{
							booksList = new ArrayList<String>();
						}
						booksList.add(rs.getString("BOOK_CODE").trim());
					}
				}
			}
			pstmt.close();pstmt=null;
			rs.close();rs=null;
			sql  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(), "getSelectedBooks()", e);
		}
		finally {
			if (null != pstmt)
				pstmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return booksList;
	}

	public static boolean updateSearchCriteriaJobStatus(String scheduleId, String status) {
		/*
		 * AN ABORTED SCHEDULE IS FINISHED - DO NOT OVERWRITE ITS STATUS.
		 *
		 * The worker stops cooperatively (Thread.stop() is removed in Java 20), so it can still
		 * reach this on its way out and would flip 'Aborted' to Completed or Failure, losing the
		 * abort the user just performed. Refusing the write here covers every branch that calls
		 * this, in the one place they all funnel through.
		 */
		if(isAborted(scheduleId))
		{
			logger.info("updateSearchCriteriaJobStatus :: SCHEDULE {"+scheduleId+"} WAS ABORTED - leaving its status as Aborted"
					+" instead of writing {"+status+"}.");
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)
					&& !"0".equals(scheduleId) && null != status
					&& !"".equals(status)) {
				String completeStatus = "";
				// SET FINIFH TIME AS WELL HERE IF STATUS IS OT PENDING /
				// PROCESSING
				String updateStatusSql = "";
				if (status.equals(ApplicationProperties
						.getProperty("schedule.status.pending.value"))
						|| status
						.equals(ApplicationProperties
								.getProperty("schedule.status.processing.value"))) {
					updateStatusSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_sch_status=? WHERE cd_schedule_id= ?";
				} else {
					completeStatus = "COMPLETED";
					// set finish Time
					updateStatusSql = "UPDATE gms3_mdm_cdrom_schedule SET cd_sch_status=?, cd_finish_tmstp = ?, cd_current_processing_status = ? WHERE cd_schedule_id= ?";
				}

				conn = getConnection();

				pstmt = conn.prepareStatement(updateStatusSql);
				if (status.equals(ApplicationProperties
						.getProperty("schedule.status.pending.value"))
						|| status
						.equals(ApplicationProperties
								.getProperty("schedule.status.processing.value"))) {
					pstmt.setString(1, status);
					pstmt.setLong(2, new Long(scheduleId).longValue());
				} else {
					pstmt.setString(1, status);
					pstmt.setTimestamp(2,
							new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(3, completeStatus);
					pstmt.setLong(4, new Long(scheduleId).longValue());

				}
				int i = pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateStatusSql = null;
				if (i == 1) {
					logger.info("updateSearchCriteriaJobStatus :: Status for Schedule Id {"
							+ scheduleId + "} Updated to :: >" + status);
				} else {
					logger.info("updateScheduleStatus :: Failed to Update {"
							+ status + "} Status for Schedule Id {"
							+ scheduleId + "}.");
					return false;
				}
			} else {
				logger.info("updateSearchCriteriaJobStatus :: Schedule id / Status as parameters are null. Return false.");
				return false;
			}
		} catch (Exception e) {
			logger.info("updateSearchCriteriaJobStatus :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
					"updateSearchCriteriaJobStatus()", e);
			logger.info("updateSearchCriteriaJobStatus :: ################ Exception ################");
			return false;
		} finally {
			try
			{
				if (null != conn)
					conn.close();conn = null;
				if (null != pstmt)
					pstmt.close();pstmt=null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
						"updateSearchCriteriaJobStatus()", e);
			}
			// set params to null
			scheduleId = null;
			status = null;
		}
		return true;
	}

	public static boolean createSearchCriteriaItemsData(ArrayList<CDRomScheduleItemDetails> itemsList, long scheduleId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try {
			/*
			 * RETURN SUCCESS, EVEN IF NOT ITEM IS FOUND.
			 */
			if(null!=itemsList && itemsList.size()>0)
			{
				conn = getConnection();
				for(CDRomScheduleItemDetails itemDetails : itemsList)
				{
					String itemSql = "INSERT INTO gms3_mdm_cdrom_sch_items(cd_schedule_id,cd_manual_type_code,"
							+ "cd_manual_type,cd_created_tmstp) "
							+ "VALUES (?,?,?,?)";
					pstmt = null;
					pstmt = conn.prepareStatement(itemSql);
					pstmt.setLong(1, scheduleId);
					pstmt.setString(2, itemDetails.getManualTypeCode());
					pstmt.setString(3, itemDetails.getManualType());
					pstmt.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					itemDetails = null;
					itemSql = null;
				}
			}
		} catch (Exception e) {
			logger.info("createSearchCriteriaItemsData :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"createSearchCriteriaItemsData()", e);
			logger.info("createSearchCriteriaItemsData :: ################ Exception ################");
			return false;
		} finally {
			try
			{
				if (null != conn)
					conn.close();conn=null;
				if (null != pstmt)
					pstmt.close();pstmt=null;
				if (null != stmt)
					stmt.close();stmt=null;
				if (null != rs)
					rs.close();rs=null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"createSearchCriteriaItemsData()", e);		
				return false;
			}
		}
		return true;
	}

	
	public static ArrayList<CDRomScheduleDetails> getSearchCriteriaScheduleDetails(String scheduleId,String fromDate, String toDate) {
		ArrayList<CDRomScheduleDetails> schedueList = new ArrayList<CDRomScheduleDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		try {
			conn = getConnection();
			if (StringUtils.isBlank(scheduleId)) {
				sql="SELECT * FROM gms3_mdm_cdrom_schedule where cd_schedule_name like 'MDM_CDROM_SC_%'  ";
				if(null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate))
				{
					sql = sql+" AND DATE_FORMAT(cd_schedule_tmstp,'%Y-%m-%d') BETWEEN '"+fromDate+"' AND '"+toDate+"' ";
				}
				sql = sql+" ORDER BY cd_schedule_id DESC ";
			} else {
				sql = "SELECT * FROM gms3_mdm_cdrom_schedule WHERE cd_schedule_id="+scheduleId;
			}
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				CDRomScheduleDetails schDetails = new CDRomScheduleDetails();
				schDetails.setSrNo(schedueList.size() + 1);
				schDetails.setScheduleId(rs.getLong("cd_schedule_id"));
				schDetails.setScheduleName(rs.getString("cd_schedule_name"));
				schDetails.setUserId(rs.getString("cd_user_id"));
				schDetails.setScheduleStatus(rs.getString("cd_sch_status"));
				schDetails.setScheduleTime(rs.getTimestamp("cd_schedule_tmstp"));
				schDetails.setFinishTime(rs.getTimestamp("cd_finish_tmstp"));
				schDetails.setThreadId(rs.getString("cd_sch_thread_id"));
				schDetails.setCurrentProcessingStatus(rs.getString("cd_current_processing_status"));
				schDetails.setCarlineCode(rs.getString("mdm_crln_code"));
				schDetails.setCarlineNameRegional(rs.getString("mdm_crln_name_regional_lang"));
				schDetails.setCountryLocalId(rs.getLong("mdm_cl_id"));
				schDetails.setLocaleCode(rs.getString("mdm_ml_lang_code"));
				schDetails.setVinStartRange(rs.getString("mdm_vin_vis_start_range"));
				schDetails.setVinVdsCode(rs.getString("mdm_vin_vds_code"));
				schDetails.setVinWmiCode(rs.getString("mdm_vin_wmi_code"));
				schedueList.add(schDetails);
				schDetails = null;
			}
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			sql = null;
		} catch (Exception e) {
			logger.info("getSearchCriteriaScheduleDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getSearchCriteriaScheduleDetails()", e);
			logger.info("getSearchCriteriaScheduleDetails :: ################ Exception ################");
		} finally {
			try
			{
				if (null != conn)
					conn.close();conn=null;
				if (null != stmt)
					stmt.close();stmt=null;
				if (null != rs)
					rs.close();rs=null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getSearchCriteriaScheduleDetails()", e);
			}
		}
		return schedueList;
	}

	public static ArrayList<CDRomScheduleItemDetails> getSearchCriteriaScheduleItemDetails(String scheduleId) {
		ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			if (null != scheduleId && !"".equals(scheduleId)) {
				conn = getConnection();
				String sql = "SELECT * FROM gms3_mdm_cdrom_sch_items WHERE cd_schedule_id ="+ new Long(scheduleId).longValue();
				logger.info("getSearchCriteriaScheduleItemDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					CDRomScheduleItemDetails schItemDetails = new CDRomScheduleItemDetails();
					schItemDetails.setSrNo(itemsList.size() + 1);
					schItemDetails.setItemId(rs.getLong("cd_item_id"));
					schItemDetails.setScheduleId(rs.getLong("cd_schedule_id"));
					schItemDetails.setManualTypeCode(rs.getString("cd_manual_type_code"));
					schItemDetails.setManualType(rs.getString("cd_manual_type"));
					itemsList.add(schItemDetails);
					schItemDetails = null;
				}
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
				sql = null;
			} else {
				logger.info("getSearchCriteriaScheduleItemDetails :: Schedule id as parameters are null. Return null");
				itemsList = null;
			}
		} catch (Exception e) {
			logger.info("getSearchCriteriaScheduleItemDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getSearchCriteriaScheduleItemDetails()", e);
			logger.info("getSearchCriteriaScheduleItemDetails :: ################ Exception ################");
		} finally {
			try
			{
				if (null != conn)
					conn.close();conn=null;
				if (null != stmt)
					stmt.close();stmt=null;
				if (null != rs)
					rs.close();rs=null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),"getSearchCriteriaScheduleItemDetails()", e);
			}
		}
		return itemsList;
	}


}