package com.mazda.gms3.dmt.conversion.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.MNAOModelYearViewContentDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class MNAODocumentManagementDAO extends DBConnectionHelper{
	
	Logger logger = LogManager.getLogger(MNAODocumentManagementDAO.class);
	
	private static Logger logger1 =LogManager.getLogger(MNAODocumentManagementDAO.class);
	
	private ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetails = new ArrayList<Map<Object, Object>>();
	
	private ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetailsForDelete = new ArrayList<Map<Object, Object>>();
	
	private ArrayList<MNAOModelYearViewContentDetails> viewContentModelYearList = new ArrayList<MNAOModelYearViewContentDetails>();
	
	private ArrayList<MNAOModelYearViewContentDetails> viewContentVINList = new ArrayList<MNAOModelYearViewContentDetails>();
	
	/** The gms3_vc_model_year_details rows this job wrote - VIEW_CONTENT_MODEL_YEAR_REPORT. */
	public ArrayList<MNAOModelYearViewContentDetails> getViewContentModelYearList()
	{
		return viewContentModelYearList;
	}

	/** The gms3_vc_vin_details rows this job wrote - VIEW_CONTENT_VIN_REPORT. */
	public ArrayList<MNAOModelYearViewContentDetails> getViewContentVINList()
	{
		return viewContentVINList;
	}

	/**
	 * The view content rows this job wrote (model year + VIN), in the shape the reports and the preview read
	 * (MCViewContentDetails, as for MC / MME). The model year rows carry "MODEL YEAR" as the model name.
	 */
	public ArrayList<com.mazda.gms3.dmt.mc.vo.MCViewContentDetails> getViewContentDataList()
	{
		ArrayList<com.mazda.gms3.dmt.mc.vo.MCViewContentDetails> list = new ArrayList<com.mazda.gms3.dmt.mc.vo.MCViewContentDetails>();
		List<MNAOModelYearViewContentDetails> all = new ArrayList<MNAOModelYearViewContentDetails>(viewContentModelYearList);
		all.addAll(viewContentVINList);
		for(MNAOModelYearViewContentDetails v : all)
		{
			com.mazda.gms3.dmt.mc.vo.MCViewContentDetails m = new com.mazda.gms3.dmt.mc.vo.MCViewContentDetails();
			m.setSourceFilePath(v.getSourceFilePath());
			m.setLocale(v.getLocale());
			m.setDocumentId(v.getDocumentId());
			m.setCarlineNameEng(null==v.getModel() ? null : (v.getModel() + (null==v.getYear() ? "" : " " + v.getYear())));
			m.setWmiCode(v.getWmiCode());
			m.setVdsCode(v.getVdsCode());
			m.setVisStartRange(v.getVisStartRange());
			m.setVisEndRange(v.getVisEndRange());
			m.setEsiCatLevel1Code(v.getEsiCatLevel1Code());
			m.setEsiCatLevel1Name(v.getEsiCatLevel1Name());
			m.setEsiCatLevel2Code(v.getEsiCatLevel2Code());
			m.setEsiCatLevel2Name(v.getEsiCatLevel2Name());
			m.setEsiCatLevel3Code(v.getEsiCatLevel3Code());
			m.setEsiCatLevel3Name(v.getEsiCatLevel3Name());
			m.setManualType(v.getManualType());
			m.setManualTypeLabel(v.getManualTypeLabel());
			m.setImDocCreateDate(v.getImDocCreateDate());
			m.setImDocModifiedDate(v.getImDocModifiedDate());
			m.setTitle(v.getTitle());
			m.setDescription(v.getDescription());
			m.setProcessingStatus(v.getProcessingStatus());
			m.setRemarks(v.getErrorCodes());
			com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d = v.getDisplayOrder();
			if(null!=d)
			{
				m.setDisplayOrderNameLevel1(d.getDisplayOrderLevel1Name());
				m.setDisplayOrderNameLevel2(d.getDisplayOrderLevel2Name());
				m.setDisplayOrderNameLevel3(d.getDisplayOrderLevel3Name());
				m.setDisplayOrderNameLevel4(d.getDisplayOrderLevel4Name());
				m.setDisplayOrderNameLevel5(d.getDisplayOrderLevel5Name());
				m.setDisplayOrderNameLevel6(d.getDisplayOrderLevel6Name());
				m.setDisplayOrderSequenceNo(d.getSequenceNo());
			}
			list.add(m);
		}
		return list;
	}

	public ArrayList<Map<Object, Object>> getFailedDatabaseSaveDocumentDetailsForDelete() {
		return failedDatabaseSaveDocumentDetailsForDelete;
	}

	public void setFailedDatabaseSaveDocumentDetailsForDelete(
			ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetailsForDelete) {
		this.failedDatabaseSaveDocumentDetailsForDelete = failedDatabaseSaveDocumentDetailsForDelete;
	}

	public ArrayList<Map<Object, Object>> getFailedDatabaseSaveDocumentDetails() {
		return failedDatabaseSaveDocumentDetails;
	}
	
	public void setFailedDatabaseSaveDocumentDetails(
			ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetails) {
		this.failedDatabaseSaveDocumentDetails = failedDatabaseSaveDocumentDetails;
	}

	public  ContentDetails getDocumentDetails(String filePath, String locale) throws SQLException 
	{
		ContentDetails contentDetails=  new ContentDetails();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=filePath && !"".equals(filePath) && null!=locale && !"".equals(locale))
			{
				String tableName="";
				if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
				}
				conn = getConnection();
				
				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = "
						+ " '"+filePath.trim().toLowerCase()+"' " ;
				logger.info("getDocumentDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					contentDetails = new ContentDetails();
					contentDetails.setImDocumentId(rs.getString("DC_IM_DOC_ID"));
					contentDetails.setImContentType(rs.getString("DC_IM_DOC_CONTENT_ID"));
					contentDetails.setFilePath(rs.getString("DC_SOURCE_NETWORK_LOC"));
				}
				sql = null;
				stmt.close();stmt = null;
				rs.close();rs=null;
			}
			else
			{
				logger.info("getDocumentDetails :: File Path as Parameter is null, Return null.");
				contentDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getDocumentDetails()", e);
			contentDetails = null;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return contentDetails;
	}
	
	public  ContentDetails getDocumentDetailsWD(String filePath, String locale) throws SQLException 
	{
		ContentDetails contentDetails=  new ContentDetails();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=filePath && !"".equals(filePath) && null!=locale && !"".equals(locale))
			{
				/*
				 * CHECK FOR BOTH PDF & HTML
				 */
				String anotherFilePath="";
				if(filePath.contains("\\pdf\\"))
				{
					anotherFilePath = filePath.replace("\\pdf\\", "\\html\\");
				}
				else if(filePath.contains("\\html\\"))
				{
					anotherFilePath = filePath.replace("\\html\\", "\\pdf\\");
				}
				
				String tableName="";
				if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
				}
				else if(ApplicationProperties.getProperty(locale.trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
				}
				conn = getConnection();
				
//				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE '"+filePath.trim().toLowerCase()+"%' OR TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE '"+anotherFilePath.trim().toLowerCase()+"%' "
//								+ " AND DC_IM_DOC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' " ;
				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = '"+filePath.trim().toLowerCase()+"' OR "
						+ " TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = '"+anotherFilePath.trim().toLowerCase()+"' " ;
				logger.info("getDocumentDetails :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					contentDetails = new ContentDetails();
					contentDetails.setImDocumentId(rs.getString("DC_IM_DOC_ID"));
					contentDetails.setImContentType(rs.getString("DC_IM_DOC_CONTENT_ID"));
					contentDetails.setFilePath(rs.getString("DC_SOURCE_NETWORK_LOC"));
				}
				sql = null;
				stmt.close();stmt = null;
				rs.close();rs=null;
				anotherFilePath = null;
			}
			else
			{
				logger.info("getDocumentDetails :: File Path as Parameter is null, Return null.");
				contentDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getDocumentDetails()", e);
			contentDetails = null;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return contentDetails;
	}
	
	/**
	 * Every ACTIVE document (DC_IM_DOC_FLAG) the database holds for one material folder - the MNAO form of
	 * MCDocumentManagementDAO.getActiveDocumentsForMaterialFolder(), on the locale's own table.
	 *
	 * @param materialFolderKey the start of DC_SOURCE_NETWORK_LOC for the folder, ending with a
	 *        backslash: locale\modelFolder\manualType\faceLift\material\
	 * @return one entry per document: { DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC }
	 *
	 * LOCATE is used rather than LIKE on purpose: folder names contain underscores, and in a
	 * LIKE pattern an underscore matches ANY character.
	 */
	public static List<String[]> getActiveDocumentsForMaterialFolder(String materialFolderKey, String locale) throws SQLException
	{
		List<String[]> documents = new ArrayList<String[]>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=materialFolderKey && !"".equals(materialFolderKey) && null!=locale && !"".equals(locale))
			{
				String tableName="gms3_dmt_"+locale.replace("-", "_").trim().toLowerCase()+"_imdoc";
				conn = getConnection();
				pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC FROM "+tableName
						+" WHERE DC_IM_DOC_FLAG = ? AND DC_IM_DOC_ID IS NOT NULL AND DC_SOURCE_NETWORK_LOC IS NOT NULL"
						+" AND LOCATE(?, TRIM(LOWER(DC_SOURCE_NETWORK_LOC))) = 1");
				pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
				pstmt.setString(2, materialFolderKey.trim().toLowerCase());
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					documents.add(new String[]{rs.getString("DC_IM_DOC_ID"), rs.getString("DC_SOURCE_NETWORK_LOC")});
				}
			}
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		return documents;
	}

	public static List<SelectItemDetails> getDocumentDetailsForInnerLink(List<String> innerLinkPathsList, String locale) throws SQLException 
	{
		List<SelectItemDetails> docsList = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=innerLinkPathsList && innerLinkPathsList.size()>0 && null!=locale && !"".equals(locale))
			{
				conn = getConnection();
				locale = locale.replace("-", "_");
				String tableName="";
				if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
				}
				
				/*
				 * DO A PARTITION OF 300 AND FETCH THEM ALL IN ONE GET
				 */
				int partitionSize=300;
				List<List<String>> partitions = new ArrayList<List<String>>();
				// create Partitions of Unique Items Data and then fetch FK Codes / Data
				for (int i=0; i<innerLinkPathsList.size(); i += partitionSize) {
					partitions.add(innerLinkPathsList.subList(i, Math.min(i + partitionSize, innerLinkPathsList.size())));
				}
				
				String sql="";
				String keyToCheck=null;
				SelectItemDetails si = null;
				if(null!=partitions && partitions.size()>0)
				{
					for(List<String> subList : partitions)
					{
						keyToCheck = null;
						if(null!=subList && subList.size()>0)
						{
							for(int e=0;e<subList.size();e++)
							{
								if(null==keyToCheck)
								{
									keyToCheck = "";
								}
								keyToCheck+="'"+subList.get(e).toString()+"'";
								if(e!=subList.size()-1)
								{
									keyToCheck+=",";
								}
							}
							
							if(null!=keyToCheck  && !"".equals(keyToCheck))
							{
								sql="SELECT DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN "
										+ " ("+keyToCheck.trim().toLowerCase()+") AND DC_IM_DOC_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' " ;
								logger1.info("getDocumentDetailsForInnerLink :: Sql :: > " + sql);
								stmt = conn.createStatement();
								rs = stmt.executeQuery(sql);
								si  = null;
								while(rs.next())
								{
									si = new SelectItemDetails();
									// set document Id
									si.setLabel(rs.getString("DC_IM_DOC_ID"));
									// set source path
									si.setValue(rs.getString("DC_SOURCE_NETWORK_LOC"));
									if(null==docsList || docsList.size()<=0)
									{
										docsList = new ArrayList<SelectItemDetails>();
									}
									docsList.add(si);
									si = null;
								}
								si = null;
								sql = null;
								stmt.close();stmt = null;
								rs.close();rs=null;
							}
							keyToCheck = null;
						}
						keyToCheck = null;
						subList=  null;
					}
				}
				partitions = null;
			}
			else
			{
				logger1.info("getDocumentDetailsForInnerLink :: InnerLinks Path List / Locale as Parameter is null, Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getDocumentDetailsForInnerLink()", e);
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return docsList;
	}
	
	
	public boolean updateInnerLinkMappingStatus(List<ContentDetails> documentsList,String locale) 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		try
		{
			if(null!=documentsList && documentsList.size()>0 && null!=locale && !"".equals(locale))
			{
				
				String tableName="";
				locale = locale.replace("-", "_");
				
				if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
				}
				else if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
				}
				
				// NO BATCHED UPDATE: MySQL Router (connection sharing) on MC Dev refuses a batch sent as a
				// multi-statement. ONE statement per db.write.batch.size documents, the three values picked
				// per document with CASE.
				int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
				conn = getConnection();
				conn.setAutoCommit(false);
				for (int from=0; from<documentsList.size(); from += partitionSize)
				{
					List<ContentDetails> subList = documentsList.subList(from, Math.min(from + partitionSize, documentsList.size()));
					StringBuilder found = new StringBuilder("CASE DC_IM_DOC_ID");
					StringBuilder mapped = new StringBuilder("CASE DC_IM_DOC_ID");
					StringBuilder reason = new StringBuilder("CASE DC_IM_DOC_ID");
					StringBuilder in = new StringBuilder();
					for(int c=0;c<subList.size();c++)
					{
						found.append(" WHEN ? THEN ?");
						mapped.append(" WHEN ? THEN ?");
						reason.append(" WHEN ? THEN ?");
						in.append(c>0 ? ",?" : "?");
					}
					found.append(" ELSE DC_INRLNK_FOUND END");
					mapped.append(" ELSE DC_INRLNK_ALL_MAPPED END");
					reason.append(" ELSE DC_INRLNK_FAILURE_REASON END");
					pstmt = conn.prepareStatement("UPDATE "+tableName+" SET DC_INRLNK_FOUND = "+found
							+ ", DC_INRLNK_ALL_MAPPED = "+mapped+", DC_INRLNK_FAILURE_REASON = "+reason
							+ " WHERE DC_IM_DOC_ID IN ("+in+")");
					int p=1;
					for(ContentDetails cd : subList)
					{
						pstmt.setString(p++, cd.getImDocumentId());
						pstmt.setString(p++, cd.getInnerLinkFound());
					}
					for(ContentDetails cd : subList)
					{
						pstmt.setString(p++, cd.getImDocumentId());
						pstmt.setString(p++, cd.getAllInnerLinksMapped());
					}
					for(ContentDetails cd : subList)
					{
						pstmt.setString(p++, cd.getImDocumentId());
						pstmt.setString(p++, cd.getInnerLinkMappingReason());
					}
					for(ContentDetails cd : subList)
					{
						pstmt.setString(p++, cd.getImDocumentId());
					}
					pstmt.executeUpdate();
					pstmt.close();pstmt=null;
				}
				conn.commit();
				tableName=null;
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", eq);
			}
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=conn)
				{
					conn.close();
				}
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", e);
			}
		}
		return true;
	}
	
	/**
	 * Function will Save Document Details in Database
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public boolean saveDocumentDetails(ContentDetails contentDetails) throws SQLException 
	{
		logger.info("saveDocumentDetails :: #######################################################################  Starts.");
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			logger.info("saveDocumentDetails :: Document Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
			if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
				
				String tableName="";
				String catTableName="";
				String vinTableName="";
				String vinAttrTableName="";
				String cvcTableName="";
				String vinMasterTableName="";
				String innerLinkTableName="";
				String navTableName="";
				
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
					catTableName = "gms3_dmt_en_us_cat";
					vinTableName=  "gms3_dmt_en_us_vin";
					vinAttrTableName = "gms3_dmt_en_us_vinattr";
					cvcTableName = "gms3_dmt_en_us_cvc";
					vinMasterTableName= "gms3_dmt_en_us_vinmaster";
					innerLinkTableName = "gms3_dmt_en_us_inrlks";
					navTableName= "gms3_dmt_en_us_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
					catTableName = "gms3_dmt_en_ca_cat";
					vinTableName=  "gms3_dmt_en_ca_vin";
					vinAttrTableName = "gms3_dmt_en_ca_vinattr";
					cvcTableName = "gms3_dmt_en_ca_cvc";
					vinMasterTableName= "gms3_dmt_en_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_en_ca_inrlks";
					navTableName= "gms3_dmt_en_ca_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
					catTableName = "gms3_dmt_es_mx_cat";
					vinTableName=  "gms3_dmt_es_mx_vin";
					vinAttrTableName = "gms3_dmt_es_mx_vinattr";
					cvcTableName = "gms3_dmt_es_mx_cvc";
					vinMasterTableName= "gms3_dmt_es_mx_vinmaster";
					innerLinkTableName = "gms3_dmt_es_mx_inrlks";
					navTableName= "gms3_dmt_es_mx_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
					catTableName = "gms3_dmt_fr_ca_cat";
					vinTableName=  "gms3_dmt_fr_ca_vin";
					vinAttrTableName = "gms3_dmt_fr_ca_vinattr";
					cvcTableName = "gms3_dmt_fr_ca_cvc";
					vinMasterTableName= "gms3_dmt_fr_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_fr_ca_inrlks";
					navTableName= "gms3_dmt_fr_ca_nav";
				}
				
				/*
				 * insert in to gms3_dmt_conv_imdoc
				 */
				String getAutoIdSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID) "
						+ "VALUES("+new Long(contentDetails.getScheduleId()).longValue()+",'"+contentDetails.getImDocumentId()+"')";
				stmt = conn.createStatement();
				String generatedKeys[] = { "DC_DD_ID"};
				stmt.executeUpdate(getAutoIdSql, generatedKeys);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					autoDocumentId = rs.getLong(1);
				}
				getAutoIdSql= null;
				stmt.close();stmt = null;
				rs.close();rs =null;
				
				if(null!=autoDocumentId && autoDocumentId>0)
				{
					/*
					 * Update other Details in gms3_dmt_conv_imdoc
					 */
					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_LOCALE_NAME=?,DC_SOURCE_FILE_NAME=?,"
							+ "DC_SOURCE_NETWORK_LOC=?,DC_FILE_NAME_ATTRIBUTE=?,DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
							+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_STATUS=?,DC_MARKET_NAME=?,DC_MODEL_NAME=?,"
							+ "DC_MANUAL_TYPE=?,DC_IM_DOC_CAT_TYPE=?,DC_IM_DOC_SUBCAT_TYPE=?,DC_IM_DOC_FLAG=?,DC_CREATED_TMSTP=?,"
							+ "DC_MODEL_FOLDER_NAME = ?, DC_MATERIAL_FOLDER_NAME = ?, DC_IM_DOC_SUBSUBCAT_TYPE=?, DC_ESI_CATEGORY_MAPPED=?,"
							+ "DC_ESI_CATEGORY_MAPPED_LEVEL=?,DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_VIN_MAPPED=?,"
							+ " DC_IM_DOC_TITLE=?, DC_INRLNK_FOUND=?,"
							+ " DC_INRLNK_ALL_MAPPED=?, DC_INRLNK_FAILURE_REASON=?  "
							+ " WHERE DC_DD_ID=?";
					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getLocale());
					pstmt.setString(2, contentDetails.getFileName());
					pstmt.setString(3, contentDetails.getFilePath());
					pstmt.setString(4, contentDetails.getFileNameAttribute());
					pstmt.setString(5, contentDetails.getChannelName());
					pstmt.setString(6, contentDetails.getImContentType());
					pstmt.setString(7, contentDetails.getImVersion());
					pstmt.setString(8, contentDetails.getImResourcePath());
					pstmt.setString(9, contentDetails.getImDocStatus());
					pstmt.setString(10, contentDetails.getMarket());
					pstmt.setString(11, contentDetails.getModel());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getCategoryCode());
					pstmt.setString(14, contentDetails.getSubCategoryCode());
					pstmt.setString(15, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setTimestamp(16, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(17, contentDetails.getModelFolderName());
					pstmt.setString(18, contentDetails.getMaterialName());
					pstmt.setString(19, contentDetails.getSubSubCategoryCode());
					pstmt.setString(20, contentDetails.getEsiCategoryMapped());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(22, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(23, contentDetails.getVinMapped());
					pstmt.setString(24, contentDetails.getTitle());
					pstmt.setString(25, contentDetails.getInnerLinkFound());
					pstmt.setString(26, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(27, contentDetails.getInnerLinkMappingReason());
					pstmt.setLong(28, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;
					
					logger.info("saveDocumentDetails :: Document Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					String previousFileName="";
					if(null!=contentDetails.getPreviousLink() && !"".equals(contentDetails.getPreviousLink()))
					{
						if(contentDetails.getPreviousLink().lastIndexOf("\\")!=-1)
						{
							previousFileName = contentDetails.getPreviousLink().substring(contentDetails.getPreviousLink().lastIndexOf("\\")+1, contentDetails.getPreviousLink().length());
						}
					}
					String nextFileName="";
					if(null!=contentDetails.getNextLink() && !"".equals(contentDetails.getNextLink()))
					{
						if(contentDetails.getNextLink().lastIndexOf("\\")!=-1)
						{
							nextFileName = contentDetails.getNextLink().substring(contentDetails.getNextLink().lastIndexOf("\\")+1, contentDetails.getNextLink().length());
						}
					}
					/*
					 * INSERT NAVIGATON LINK DETAILS
					 */
					String insertNavigationSql="INSERT INTO "+navTableName+" (DC_IM_DOC_ID, "
							+ "DC_IM_DOC_NEXT_LINK_FILE_NAME, DC_IM_DOC_NEXT_LINK_FILE_PATH, DC_IM_DOC_NEXT_IMDOC_ID,"
							+ "DC_IM_DOC_PREV_LINK_FILE_NAME, DC_IM_DOC_PREV_LINK_FILE_PATH, DC_IM_DOC_PREV_IMDOC_ID,"
							+ "DC_NAV_CREATED_TMSTP) "
							+ "VALUES(?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertNavigationSql);
					pstmt.setString(1, contentDetails.getImDocumentId());
					pstmt.setString(2, nextFileName);
					pstmt.setString(3, contentDetails.getNextLink());
					pstmt.setString(4, contentDetails.getNextLinkDocId());
					pstmt.setString(5, previousFileName);
					pstmt.setString(6, contentDetails.getPreviousLink());
					pstmt.setString(7, contentDetails.getPreviousLinkDocId());
					pstmt.setTimestamp(8, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					insertNavigationSql = null;
					nextFileName = null;
					previousFileName= null;
					
					/*
					 * INSERT INNER LINK DETAILS
					 */
					logger.info("saveDocumentDetails :: Inner Links Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						performInnerLinksOperation(contentDetails, conn, innerLinkTableName);
					}
					logger.info("saveDocumentDetails :: Inner Links Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					/*
					 * INSERT DOC CATEGORY DETAILS
					 */
					logger.info("saveDocumentDetails :: Category Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
					if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
					{
						processCategoryInformation(contentDetails, conn, catTableName);
					}
					logger.info("saveDocumentDetails :: Category Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					/*
					 * INSERT INTO VIN DETAILS
					 */
					logger.info("saveDocumentDetails :: VIN ENT Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
					if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
					{
						processVINENTInformation(contentDetails, conn, vinTableName);
					}
					logger.info("saveDocumentDetails :: VIN ENT Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					/*
					 * INSERT INTO VIN ATTRIBUTE DETAILS
					 */
					logger.info("saveDocumentDetails :: VIN ATTRIBUTE Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
					if(null!=contentDetails.getVinAttributeEntDetailsList() && contentDetails.getVinAttributeEntDetailsList().size()>0)
					{
						processVINAttributeENTInformation(contentDetails, conn, vinAttrTableName);
					}
					logger.info("saveDocumentDetails :: VIN ATTRIBUTE Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					/*
					 * INSERT INTO CVC CATEGORIES DATA
					 */
					logger.info("saveDocumentDetails :: CVC Cateegory Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
					if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
					{
						processCVCCategoryInformation(contentDetails, conn, cvcTableName);
					}
					logger.info("saveDocumentDetails :: CVC Category Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					
					/*
					 * INSERT INTO VIN MASTER DATA - APPLICABLE FOR ENGINE / MISSSION
					 */
					if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
					{
						logger.info("saveDocumentDetails :: MASTER VIN Details Table Data Insert Started at :: >" + new Timestamp(new Date().getTime()));
						processVINMasterInformation(contentDetails, conn, vinMasterTableName);
						logger.info("saveDocumentDetails :: MASTER VIN Details Table Data Insert Completed at :: >" + new Timestamp(new Date().getTime()));
					}
					
					/*
					 * PERFORM VIEW CONTENT OPERATION
					 */
//					processViewContentData(contentDetails, tableName, conn);
				}
				// commit the transaction
				conn.commit();
				
				tableName=null;
				catTableName=null;
				vinTableName=null;
				vinAttrTableName=null;
				cvcTableName=null;
				vinMasterTableName=null;
				innerLinkTableName=null;
				navTableName=null;
			}
			else
			{
				logger.info("saveDocumentDetails :: IM DOCUMENT IS NULL IN OBJECT. Return false");
				/*
				 * Add the Document Details to
				 */
				if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
				{
					// initialize the list
					setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
				}
				/*
				 * now create a Map Object and add the following IM Document ID
				 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
				 */
				Map<Object, Object> errorMap = new HashMap<Object, Object>();
				errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
				errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
				errorMap.put("CHANNEL", contentDetails.getChannelName());
				errorMap.put("ERROR_CODE", "DBEXP001");
				errorMap.put("ERROR_MESSAGE", "IM_DOCUMENT_ID AS PARAMETER IS NULL.");
				errorMap.put("RECORD_ID", contentDetails.getImContentType());
				errorMap.put("OPERATION_TYPE", "CREATE");
				errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
				errorMap.put("CONTENT_DETAILS", contentDetails);
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
				errorMap.put("DATE_TIME", sdf.format(date));
				// set date to null
				date = null;
				// set sdf to null
				sdf = null;

				// add errorMap to FailedDatabaseSaveDocumentDetails
				getFailedDatabaseSaveDocumentDetails().add(errorMap);
				// set errorMap to null
				errorMap = null;
				
				
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDocumentDetails()", e);
			
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			String errorCode = e.getMessage();
			String errorMessage = writer.toString();

			/*
			 * Add the Document Details to
			 */
			if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
			{
				// initialize the list
				setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
			}
			/*
			 * now create a Map Object and add the following IM Document ID
			 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
			 */
			Map<Object, Object> errorMap = new HashMap<Object, Object>();
			errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
			errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
			errorMap.put("CHANNEL", contentDetails.getChannelName());
			errorMap.put("ERROR_CODE", errorCode);
			errorMap.put("ERROR_MESSAGE", errorMessage);
			errorMap.put("RECORD_ID", contentDetails.getImContentType());
			errorMap.put("OPERATION_TYPE", "CREATE");
			errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
			errorMap.put("CONTENT_DETAILS", contentDetails);
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
			errorMap.put("DATE_TIME", sdf.format(date));
			// set date to null
			date = null;
			// set sdf to null
			sdf = null;

			// add errorMap to FailedDatabaseSaveDocumentDetails
			getFailedDatabaseSaveDocumentDetails().add(errorMap);
			// set errorMap to null
			errorMap = null;
			// set errorCode to null
			errorCode = null;
			// set errorMessage to null
			errorMessage = null;
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			
			return false;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		logger.info("saveDocumentDetails :: ####################################################################### Ends.");
		return true;
		
	}

	/**
	 * Function will Delete the Old Document Reference for the Source Location 
	 * and will Create New Document for the same Source Location as Master Identifier.
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public  boolean saveDocumentDetailsWhileUpdate(ContentDetails contentDetails) throws SQLException 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			if(null!=contentDetails.getPreviousDocumentId() && !"".equals(contentDetails.getPreviousDocumentId()) && 
					null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
				
				String tableName="";
				String catTableName="";
				String vinTableName="";
				String vinAttrTableName="";
				String cvcTableName="";
				String vinMasterTableName="";
				String innerLinkTableName="";
				String navTableName="";
				
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
					catTableName = "gms3_dmt_en_us_cat";
					vinTableName=  "gms3_dmt_en_us_vin";
					vinAttrTableName = "gms3_dmt_en_us_vinattr";
					cvcTableName = "gms3_dmt_en_us_cvc";
					vinMasterTableName= "gms3_dmt_en_us_vinmaster";
					innerLinkTableName = "gms3_dmt_en_us_inrlks";
					navTableName= "gms3_dmt_en_us_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
					catTableName = "gms3_dmt_en_ca_cat";
					vinTableName=  "gms3_dmt_en_ca_vin";
					vinAttrTableName = "gms3_dmt_en_ca_vinattr";
					cvcTableName = "gms3_dmt_en_ca_cvc";
					vinMasterTableName= "gms3_dmt_en_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_en_ca_inrlks";
					navTableName= "gms3_dmt_en_ca_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
					catTableName = "gms3_dmt_es_mx_cat";
					vinTableName=  "gms3_dmt_es_mx_vin";
					vinAttrTableName = "gms3_dmt_es_mx_vinattr";
					cvcTableName = "gms3_dmt_es_mx_cvc";
					vinMasterTableName= "gms3_dmt_es_mx_vinmaster";
					innerLinkTableName = "gms3_dmt_es_mx_inrlks";
					navTableName= "gms3_dmt_es_mx_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
					catTableName = "gms3_dmt_fr_ca_cat";
					vinTableName=  "gms3_dmt_fr_ca_vin";
					vinAttrTableName = "gms3_dmt_fr_ca_vinattr";
					cvcTableName = "gms3_dmt_fr_ca_cvc";
					vinMasterTableName= "gms3_dmt_fr_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_fr_ca_inrlks";
					navTableName= "gms3_dmt_fr_ca_nav";
				}
				
				/*
				 * DELETE THE PREVIOUS DOCUMENT ID FROM ALL TABLES.
				 */
				cleanUpOldDataForDocument(conn, contentDetails, tableName, catTableName, innerLinkTableName, vinMasterTableName, navTableName,
						cvcTableName, vinTableName, vinAttrTableName, true, contentDetails.getPreviousDocumentId());
				
				/*
				 * insert in to gms3_dmt_conv_imdoc
				 */
				String getAutoIdSql="INSERT INTO "+tableName+"(DC_SCHEDULE_ID,DC_IM_DOC_ID) "
						+ "VALUES("+new Long(contentDetails.getScheduleId()).longValue()+",'"+contentDetails.getImDocumentId()+"')";
				stmt = conn.createStatement();
				String generatedKeys[] = { "DC_DD_ID"};
				stmt.executeUpdate(getAutoIdSql, generatedKeys);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					autoDocumentId = rs.getLong(1);
				}
				getAutoIdSql= null;
				stmt.close();stmt = null;
				rs.close();rs =null;
				
				if(null!=autoDocumentId && autoDocumentId>0)
				{
					/*
					 * Update other Details in gms3_dmt_conv_imdoc
					 */
					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_LOCALE_NAME=?,DC_SOURCE_FILE_NAME=?,"
							+ "DC_SOURCE_NETWORK_LOC=?,DC_FILE_NAME_ATTRIBUTE=?,DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
							+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_STATUS=?,DC_MARKET_NAME=?,DC_MODEL_NAME=?,"
							+ "DC_MANUAL_TYPE=?,DC_IM_DOC_CAT_TYPE=?,DC_IM_DOC_SUBCAT_TYPE=?,DC_IM_DOC_FLAG=?,DC_CREATED_TMSTP=?,"
							+ "DC_MODEL_FOLDER_NAME = ?, DC_MATERIAL_FOLDER_NAME = ?, DC_IM_DOC_SUBSUBCAT_TYPE=?, DC_ESI_CATEGORY_MAPPED=?,"
							+ "DC_ESI_CATEGORY_MAPPED_LEVEL=?,DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_VIN_MAPPED=?,"
							+ " DC_IM_DOC_TITLE=?, DC_INRLNK_FOUND=?,"
							+ " DC_INRLNK_ALL_MAPPED=?, DC_INRLNK_FAILURE_REASON=?  "
							+ " WHERE DC_DD_ID=?";
					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getLocale());
					pstmt.setString(2, contentDetails.getFileName());
					pstmt.setString(3, contentDetails.getFilePath());
					pstmt.setString(4, contentDetails.getFileNameAttribute());
					pstmt.setString(5, contentDetails.getChannelName());
					pstmt.setString(6, contentDetails.getImContentType());
					pstmt.setString(7, contentDetails.getImVersion());
					pstmt.setString(8, contentDetails.getImResourcePath());
					pstmt.setString(9, contentDetails.getImDocStatus());
					pstmt.setString(10, contentDetails.getMarket());
					pstmt.setString(11, contentDetails.getModel());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getCategoryCode());
					pstmt.setString(14, contentDetails.getSubCategoryCode());
					pstmt.setString(15, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setTimestamp(16, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(17, contentDetails.getModelFolderName());
					pstmt.setString(18, contentDetails.getMaterialName());
					pstmt.setString(19, contentDetails.getSubSubCategoryCode());
					pstmt.setString(20, contentDetails.getEsiCategoryMapped());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(22, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(23, contentDetails.getVinMapped());
					pstmt.setString(24, contentDetails.getTitle());
					pstmt.setString(25, contentDetails.getInnerLinkFound());
					pstmt.setString(26, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(27, contentDetails.getInnerLinkMappingReason());
					pstmt.setLong(28, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;
					
					String previousFileName="";
					if(null!=contentDetails.getPreviousLink() && !"".equals(contentDetails.getPreviousLink()))
					{
						if(contentDetails.getPreviousLink().lastIndexOf("\\")!=-1)
						{
							previousFileName = contentDetails.getPreviousLink().substring(contentDetails.getPreviousLink().lastIndexOf("\\")+1, contentDetails.getPreviousLink().length());
						}
					}
					String nextFileName="";
					if(null!=contentDetails.getNextLink() && !"".equals(contentDetails.getNextLink()))
					{
						if(contentDetails.getNextLink().lastIndexOf("\\")!=-1)
						{
							nextFileName = contentDetails.getNextLink().substring(contentDetails.getNextLink().lastIndexOf("\\")+1, contentDetails.getNextLink().length());
						}
					}
					/*
					 * INSERT NAVIGATON LINK DETAILS
					 */
					String insertNavigationSql="INSERT INTO "+navTableName+"(DC_IM_DOC_ID, "
							+ "DC_IM_DOC_NEXT_LINK_FILE_NAME, DC_IM_DOC_NEXT_LINK_FILE_PATH, DC_IM_DOC_NEXT_IMDOC_ID,"
							+ "DC_IM_DOC_PREV_LINK_FILE_NAME, DC_IM_DOC_PREV_LINK_FILE_PATH, DC_IM_DOC_PREV_IMDOC_ID,"
							+ "DC_NAV_CREATED_TMSTP) "
							+ "VALUES(?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertNavigationSql);
					pstmt.setString(1, contentDetails.getImDocumentId());
					pstmt.setString(2, nextFileName);
					pstmt.setString(3, contentDetails.getNextLink());
					pstmt.setString(4, contentDetails.getNextLinkDocId());
					pstmt.setString(5, previousFileName);
					pstmt.setString(6, contentDetails.getPreviousLink());
					pstmt.setString(7, contentDetails.getPreviousLinkDocId());
					pstmt.setTimestamp(8, new java.sql.Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();
					pstmt= null;
					insertNavigationSql = null;
					nextFileName = null;
					previousFileName= null;
					
					/*
					 * INSERT INNER LINK DETAILS
					 */
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						performInnerLinksOperation(contentDetails, conn, innerLinkTableName);
					}
					
					/*
					 * INSERT DOC CATEGORY DETAILS
					 */
					if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
					{
						processCategoryInformation(contentDetails, conn, catTableName);
					}
					
					/*
					 * INSERT INTO VIN DETAILS
					 */
					if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
					{
						processVINENTInformation(contentDetails, conn, vinTableName);
					}
					
					/*
					 * INSERT INTO VIN ATTRIBUTE DETAILS
					 */
					if(null!=contentDetails.getVinAttributeEntDetailsList() && contentDetails.getVinAttributeEntDetailsList().size()>0)
					{
						processVINAttributeENTInformation(contentDetails, conn, vinAttrTableName);
					}
					
					/*
					 * INSERT INTO CVC CATEGORIES DATA
					 */
					if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
					{
						processCVCCategoryInformation(contentDetails, conn, cvcTableName);
					}
					
					/*
					 * INSERT INTO VIN MASTER DATA - APPLICABLE FOR ENGINE / MISSSION
					 */
					if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
					{
						processVINMasterInformation(contentDetails, conn, vinMasterTableName);
					}
					
					/*
					 * PERFORM VIEW CONTENT OPERATION
					 */
//					processViewContentData(contentDetails, tableName, conn);
				}
				// commit the transaction
				conn.commit();
				
				tableName=null;
				catTableName=null;
				vinTableName=null;
				vinAttrTableName=null;
				cvcTableName=null;
				vinMasterTableName=null;
				innerLinkTableName=null;
				navTableName=null;
			}
			else
			{
				logger.info("saveDocumentDetailsWhileUpdate :: IM DOCUMENT IS NULL IN OBJECT. Return false");
				/*
				 * Add the Document Details to
				 */
				if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
				{
					// initialize the list
					setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
				}
				/*
				 * now create a Map Object and add the following IM Document ID
				 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
				 */
				Map<Object, Object> errorMap = new HashMap<Object, Object>();
				errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
				errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
				errorMap.put("CHANNEL", contentDetails.getChannelName());
				errorMap.put("ERROR_CODE", "DBEXP001");
				errorMap.put("ERROR_MESSAGE", "IM_DOCUMENT_ID AS PARAMETER IS NULL.");
				errorMap.put("RECORD_ID", contentDetails.getImContentType());
				errorMap.put("OPERATION_TYPE", "MODIFY");
				errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
				errorMap.put("CONTENT_DETAILS", contentDetails);
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
				errorMap.put("DATE_TIME", sdf.format(date));
				// set date to null
				date = null;
				// set sdf to null
				sdf = null;

				// add errorMap to FailedDatabaseSaveDocumentDetails
				getFailedDatabaseSaveDocumentDetails().add(errorMap);
				// set errorMap to null
				errorMap = null;
				
				
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDocumentDetailsWhileUpdate()", e);
			
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			String errorCode = e.getMessage();
			String errorMessage = writer.toString();

			/*
			 * Add the Document Details to
			 */
			if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
			{
				// initialize the list
				setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
			}
			/*
			 * now create a Map Object and add the following IM Document ID
			 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
			 */
			Map<Object, Object> errorMap = new HashMap<Object, Object>();
			errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
			errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
			errorMap.put("CHANNEL", contentDetails.getChannelName());
			errorMap.put("ERROR_CODE", errorCode);
			errorMap.put("ERROR_MESSAGE", errorMessage);
			errorMap.put("RECORD_ID", contentDetails.getImContentType());
			errorMap.put("OPERATION_TYPE", "MODIFY");
			errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
			errorMap.put("CONTENT_DETAILS", contentDetails);
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
			errorMap.put("DATE_TIME", sdf.format(date));
			// set date to null
			date = null;
			// set sdf to null
			sdf = null;

			// add errorMap to FailedDatabaseSaveDocumentDetails
			getFailedDatabaseSaveDocumentDetails().add(errorMap);
			// set errorMap to null
			errorMap = null;
			// set errorCode to null
			errorCode = null;
			// set errorMessage to null
			errorMessage = null;
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			
			return false;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
		
	}

	
	
	/**
	 * Function will update the Document Details for each File.
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public  boolean updateDocumentDetails(ContentDetails contentDetails, boolean performAllOps) throws SQLException 
	{
		logger.info("updateDocumentDetails :: #######################################################################  Starts.");
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			logger.info("updateDocumentDetails :: Document Details Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
			if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
				
				String tableName="";
				String catTableName="";
				String vinTableName="";
				String vinAttrTableName="";
				String cvcTableName="";
				String vinMasterTableName="";
				String innerLinkTableName="";
				String navTableName="";
				
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
					catTableName = "gms3_dmt_en_us_cat";
					vinTableName=  "gms3_dmt_en_us_vin";
					vinAttrTableName = "gms3_dmt_en_us_vinattr";
					cvcTableName = "gms3_dmt_en_us_cvc";
					vinMasterTableName= "gms3_dmt_en_us_vinmaster";
					innerLinkTableName = "gms3_dmt_en_us_inrlks";
					navTableName= "gms3_dmt_en_us_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
					catTableName = "gms3_dmt_en_ca_cat";
					vinTableName=  "gms3_dmt_en_ca_vin";
					vinAttrTableName = "gms3_dmt_en_ca_vinattr";
					cvcTableName = "gms3_dmt_en_ca_cvc";
					vinMasterTableName= "gms3_dmt_en_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_en_ca_inrlks";
					navTableName= "gms3_dmt_en_ca_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
					catTableName = "gms3_dmt_es_mx_cat";
					vinTableName=  "gms3_dmt_es_mx_vin";
					vinAttrTableName = "gms3_dmt_es_mx_vinattr";
					cvcTableName = "gms3_dmt_es_mx_cvc";
					vinMasterTableName= "gms3_dmt_es_mx_vinmaster";
					innerLinkTableName = "gms3_dmt_es_mx_inrlks";
					navTableName= "gms3_dmt_es_mx_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
					catTableName = "gms3_dmt_fr_ca_cat";
					vinTableName=  "gms3_dmt_fr_ca_vin";
					vinAttrTableName = "gms3_dmt_fr_ca_vinattr";
					cvcTableName = "gms3_dmt_fr_ca_cvc";
					vinMasterTableName= "gms3_dmt_fr_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_fr_ca_inrlks";
					navTableName= "gms3_dmt_fr_ca_nav";
				}
				
				/*
				 * insert in to gms3_dmt_conv_imdoc
				 */
				String getAutoIdSql="SELECT DC_DD_ID FROM "+tableName+" WHERE "
						+ " DC_IM_DOC_ID = '"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getAutoIdSql);
				if(rs.next())
				{
					autoDocumentId = rs.getLong("DC_DD_ID");
				}
				getAutoIdSql= null;
				stmt.close();stmt = null;
				rs.close();rs =null;
				
				if(null!=autoDocumentId && autoDocumentId>0)
				{
					/*
					 * DELETE THE DOCUMENT ID DETAILS FROM ALL TABLES.
					 * ONLY WHEN PERFORMING ALL OPS (E.G. PERFORMING NORMAL UPDATE - NOT INNERLINLS UPDATE
					 */
					if(performAllOps==true)
					{
						cleanUpOldDataForDocument(conn, contentDetails, tableName, catTableName, innerLinkTableName, vinMasterTableName, navTableName, cvcTableName,
								vinTableName, vinAttrTableName, false, contentDetails.getImDocumentId());
					}
					
					/*
					 * Update other Details in gms3_dmt_conv_imdoc
					 */
					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_LOCALE_NAME=?,DC_SOURCE_FILE_NAME=?,"
							+ "DC_SOURCE_NETWORK_LOC=?,DC_FILE_NAME_ATTRIBUTE=?,DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
							+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_STATUS=?,DC_MARKET_NAME=?,DC_MODEL_NAME=?,"
							+ "DC_MANUAL_TYPE=?,DC_IM_DOC_CAT_TYPE=?,DC_IM_DOC_SUBCAT_TYPE=?,DC_IM_DOC_FLAG=?,DC_UPDATED_TMSTP=?,"
							+ "DC_MODEL_FOLDER_NAME = ?, DC_MATERIAL_FOLDER_NAME = ?, DC_IM_DOC_SUBSUBCAT_TYPE=?, DC_ESI_CATEGORY_MAPPED=?,"
							+ "DC_ESI_CATEGORY_MAPPED_LEVEL=?,DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_VIN_MAPPED=?,"
							+ " DC_IM_DOC_TITLE=?, DC_INRLNK_FOUND=?,"
							+ " DC_INRLNK_ALL_MAPPED=?, DC_INRLNK_FAILURE_REASON=?  "
							+ " WHERE DC_DD_ID=?";
					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getLocale());
					pstmt.setString(2, contentDetails.getFileName());
					pstmt.setString(3, contentDetails.getFilePath());
					pstmt.setString(4, contentDetails.getFileNameAttribute());
					pstmt.setString(5, contentDetails.getChannelName());
					pstmt.setString(6, contentDetails.getImContentType());
					pstmt.setString(7, contentDetails.getImVersion());
					pstmt.setString(8, contentDetails.getImResourcePath());
					pstmt.setString(9, contentDetails.getImDocStatus());
					pstmt.setString(10, contentDetails.getMarket());
					pstmt.setString(11, contentDetails.getModel());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getCategoryCode());
					pstmt.setString(14, contentDetails.getSubCategoryCode());
					pstmt.setString(15, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setTimestamp(16, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setString(17, contentDetails.getModelFolderName());
					pstmt.setString(18, contentDetails.getMaterialName());
					pstmt.setString(19, contentDetails.getSubSubCategoryCode());
					pstmt.setString(20, contentDetails.getEsiCategoryMapped());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(22, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(23, contentDetails.getVinMapped());
					pstmt.setString(24, contentDetails.getTitle());
					pstmt.setString(25, contentDetails.getInnerLinkFound());
					pstmt.setString(26, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(27, contentDetails.getInnerLinkMappingReason());
					pstmt.setLong(28, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;
					
					logger.info("updateDocumentDetails :: Document Details Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));
					
					logger.info("updateDocumentDetails :: Inner Links Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
					/*
					 * INSERT INNER LINK DETAILS
					 */
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						performInnerLinksOperation(contentDetails, conn, innerLinkTableName);
					}
					
					logger.info("updateDocumentDetails :: Inner Links Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));
					
					
					if(performAllOps==true)
					{
						/*
						 * CHECK WHETHER NAVIGATION DATA EXISTS OR NOT
						 * IF NOT THEN INSERT ELSE UPDATE
						 */
						stmt = null;
						String deleteNavSql="DELETE FROM "+navTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
						stmt = conn.createStatement();
						stmt.executeUpdate(deleteNavSql);
						deleteNavSql = null;

						/*
						 * CREATE THE LINK REFERENCE
						 */
						String previousFileName="";
						if(null!=contentDetails.getPreviousLink() && !"".equals(contentDetails.getPreviousLink()))
						{
							if(contentDetails.getPreviousLink().lastIndexOf("\\")!=-1)
							{
								previousFileName = contentDetails.getPreviousLink().substring(contentDetails.getPreviousLink().lastIndexOf("\\")+1, contentDetails.getPreviousLink().length());
							}
						}
						String nextFileName="";
						if(null!=contentDetails.getNextLink() && !"".equals(contentDetails.getNextLink()))
						{
							if(contentDetails.getNextLink().lastIndexOf("\\")!=-1)
							{
								nextFileName = contentDetails.getNextLink().substring(contentDetails.getNextLink().lastIndexOf("\\")+1, contentDetails.getNextLink().length());
							}
						}
						/*
						 * INSERT NAVIGATON LINK DETAILS
						 */
						String insertNavigationSql="INSERT INTO "+navTableName+"(DC_IM_DOC_ID, "
								+ "DC_IM_DOC_NEXT_LINK_FILE_NAME, DC_IM_DOC_NEXT_LINK_FILE_PATH, DC_IM_DOC_NEXT_IMDOC_ID,"
								+ "DC_IM_DOC_PREV_LINK_FILE_NAME, DC_IM_DOC_PREV_LINK_FILE_PATH, DC_IM_DOC_PREV_IMDOC_ID,"
								+ "DC_NAV_CREATED_TMSTP) "
								+ "VALUES(?,?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(insertNavigationSql);
						pstmt.setString(1, contentDetails.getImDocumentId());
						pstmt.setString(2, nextFileName);
						pstmt.setString(3, contentDetails.getNextLink());
						pstmt.setString(4, contentDetails.getNextLinkDocId());
						pstmt.setString(5, previousFileName);
						pstmt.setString(6, contentDetails.getPreviousLink());
						pstmt.setString(7, contentDetails.getPreviousLinkDocId());
						pstmt.setTimestamp(8, new java.sql.Timestamp(new Date().getTime()));
						pstmt.executeUpdate();
						pstmt.close();
						pstmt= null;
						insertNavigationSql = null;
						nextFileName = null;
						previousFileName= null;

						/*
						 * INSERT DOC CATEGORY DETAILS
						 * 
						 * HERE CHECK IF THE CATEGORY MAPPING FOR THE DOCUMENT ID
						 * ALREADY EXISTS, DO NOTHING
						 * ELSE INSERT IT
						 */
						logger.info("updateDocumentDetails :: Category Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
						if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
						{
							processCategoryInformation(contentDetails, conn, catTableName);
						}
						logger.info("updateDocumentDetails :: Category Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));

						/*
						 * INSERT INTO VIN DETAILS
						 */
						logger.info("updateDocumentDetails :: VIN ENT Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
						if(null!=contentDetails.getVinEntDetailsList() && contentDetails.getVinEntDetailsList().size()>0)
						{
							processVINENTInformation(contentDetails, conn, vinTableName);
						}
						logger.info("updateDocumentDetails :: VIN ENT Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));

						/*
						 * INSERT INTO VIN ATTRIBUTE DETAILS
						 */
						logger.info("updateDocumentDetails :: VIN ATTRIBUTE Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
						if(null!=contentDetails.getVinAttributeEntDetailsList() && contentDetails.getVinAttributeEntDetailsList().size()>0)
						{
							processVINAttributeENTInformation(contentDetails, conn, vinAttrTableName);
						}
						logger.info("updateDocumentDetails :: VIN ATTRIBUTE Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));

						/*
						 * INSERT INTO CVC CATEGORIES DATA
						 */
						if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
						{
							logger.info("updateDocumentDetails :: CVC Category Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
							processCVCCategoryInformation(contentDetails, conn, cvcTableName);
							logger.info("updateDocumentDetails :: CVC Category Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));
						}

						/*
						 * INSERT INTO VIN MASTER DATA - APPLICABLE FOR ENGINE / MISSSION
						 */

						if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
						{
							logger.info("updateDocumentDetails :: VIN MASTER Table Data Update Started at :: >" + new Timestamp(new Date().getTime()));
							processVINMasterInformation(contentDetails, conn, vinMasterTableName);
							logger.info("updateDocumentDetails :: VIN MASTER Table Data Update Completed at :: >" + new Timestamp(new Date().getTime()));
						}

						/*
						 * PERFORM VIEW CONTENT OPERATION
						 */
//						processViewContentData(contentDetails, tableName, conn);
					}
					// commit the transaction
					conn.commit();
				}
				else
				{
					logger.info("updateDocumentDetails :: Auto Id Fetched for IM DOCUMENT ID {"+contentDetails.getImDocumentId()+"} IS NULL. Return false.");
					/*
					 * Add the Document Details to
					 */
					if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
					{
						// initialize the list
						setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
					}
					/*
					 * now create a Map Object and add the following IM Document ID
					 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
					 */
					Map<Object, Object> errorMap = new HashMap<Object, Object>();
					errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
					errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
					errorMap.put("CHANNEL", contentDetails.getChannelName());
					errorMap.put("ERROR_CODE", "DBEXP002");
					errorMap.put("ERROR_MESSAGE", "UNABLE TO LOCATE RECORD FOR IM_DOCUMENT_ID IN DATABASE.");
					errorMap.put("RECORD_ID", contentDetails.getImContentType());
					errorMap.put("OPERATION_TYPE", "MODIFY");
					errorMap.put("DOCUMENT_LOCATION", contentDetails.getFileAbsolutePath());
					errorMap.put("CONTENT_DETAILS", contentDetails);
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
					errorMap.put("DATE_TIME", sdf.format(date));
					// set date to null
					date = null;
					// set sdf to null
					sdf = null;

					// add errorMap to FailedDatabaseSaveDocumentDetails
					getFailedDatabaseSaveDocumentDetails().add(errorMap);
					// set errorMap to null
					errorMap = null;

					return false;
				}
				
				tableName=null;
				catTableName=null;
				vinTableName=null;
				vinAttrTableName=null;
				cvcTableName=null;
				vinMasterTableName=null;
				innerLinkTableName=null;
				navTableName=null;
			}
			else
			{
				logger.info("updateDocumentDetails :: IM DOCUMENT ID IS NULL. Return false.");
				/*
				 * Add the Document Details to
				 */
				if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
				{
					// initialize the list
					setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
				}
				/*
				 * now create a Map Object and add the following IM Document ID
				 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
				 */
				Map<Object, Object> errorMap = new HashMap<Object, Object>();
				errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
				errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
				errorMap.put("CHANNEL", contentDetails.getChannelName());
				errorMap.put("ERROR_CODE", "DBEXP001");
				errorMap.put("ERROR_MESSAGE", "IM_DOCUMENT_ID AS PARAMETER IS NULL.");
				errorMap.put("RECORD_ID", contentDetails.getImContentType());
				errorMap.put("OPERATION_TYPE", "MODIFY");
				errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
				errorMap.put("CONTENT_DETAILS", contentDetails);
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
				errorMap.put("DATE_TIME", sdf.format(date));
				// set date to null
				date = null;
				// set sdf to null
				sdf = null;

				// add errorMap to FailedDatabaseSaveDocumentDetails
				getFailedDatabaseSaveDocumentDetails().add(errorMap);
				// set errorMap to null
				errorMap = null;
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "updateDocumentDetails()", e);
			
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			String errorCode = e.getMessage();
			String errorMessage = writer.toString();

			/*
			 * Add the Document Details to
			 */
			if (null == getFailedDatabaseSaveDocumentDetails() 	|| getFailedDatabaseSaveDocumentDetails().size() <= 0) 
			{
				// initialize the list
				setFailedDatabaseSaveDocumentDetails(new ArrayList<Map<Object, Object>>());
			}
			/*
			 * now create a Map Object and add the following IM Document ID
			 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
			 */
			Map<Object, Object> errorMap = new HashMap<Object, Object>();
			errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
			errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
			errorMap.put("CHANNEL", contentDetails.getChannelName());
			errorMap.put("ERROR_CODE", errorCode);
			errorMap.put("ERROR_MESSAGE", errorMessage);
			errorMap.put("RECORD_ID", contentDetails.getImContentType());
			errorMap.put("OPERATION_TYPE", "MODIFY");
			errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
			errorMap.put("CONTENT_DETAILS", contentDetails);
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
			errorMap.put("DATE_TIME", sdf.format(date));
			// set date to null
			date = null;
			// set sdf to null
			sdf = null;

			// add errorMap to FailedDatabaseSaveDocumentDetails
			getFailedDatabaseSaveDocumentDetails().add(errorMap);
			// set errorMap to null
			errorMap = null;
			// set errorCode to null
			errorCode = null;
			// set errorMessage to null
			errorMessage = null;
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			return false;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		logger.info("updateDocumentDetails :: #######################################################################  Ends.");
		return true;
	}

	/**
	 * Function will Perform Delete Document Details Operation for each file.
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public boolean deleteDocumentDetails(ContentDetails contentDetails) throws SQLException 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				
				String tableName="";
				String catTableName="";
				String vinTableName="";
				String vinAttrTableName="";
				String cvcTableName="";
				String vinMasterTableName="";
				String innerLinkTableName="";
				String navTableName="";
				
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_us_imdoc";
					catTableName = "gms3_dmt_en_us_cat";
					vinTableName=  "gms3_dmt_en_us_vin";
					vinAttrTableName = "gms3_dmt_en_us_vinattr";
					cvcTableName = "gms3_dmt_en_us_cvc";
					vinMasterTableName= "gms3_dmt_en_us_vinmaster";
					innerLinkTableName = "gms3_dmt_en_us_inrlks";
					navTableName= "gms3_dmt_en_us_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_en_ca_imdoc";
					catTableName = "gms3_dmt_en_ca_cat";
					vinTableName=  "gms3_dmt_en_ca_vin";
					vinAttrTableName = "gms3_dmt_en_ca_vinattr";
					cvcTableName = "gms3_dmt_en_ca_cvc";
					vinMasterTableName= "gms3_dmt_en_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_en_ca_inrlks";
					navTableName= "gms3_dmt_en_ca_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("es-mx").trim().toLowerCase()))
				{
					tableName="gms3_dmt_es_mx_imdoc";
					catTableName = "gms3_dmt_es_mx_cat";
					vinTableName=  "gms3_dmt_es_mx_vin";
					vinAttrTableName = "gms3_dmt_es_mx_vinattr";
					cvcTableName = "gms3_dmt_es_mx_cvc";
					vinMasterTableName= "gms3_dmt_es_mx_vinmaster";
					innerLinkTableName = "gms3_dmt_es_mx_inrlks";
					navTableName= "gms3_dmt_es_mx_nav";
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).trim().toLowerCase().equals(ApplicationProperties.getProperty("fr-ca").trim().toLowerCase()))
				{
					tableName="gms3_dmt_fr_ca_imdoc";
					catTableName = "gms3_dmt_fr_ca_cat";
					vinTableName=  "gms3_dmt_fr_ca_vin";
					vinAttrTableName = "gms3_dmt_fr_ca_vinattr";
					cvcTableName = "gms3_dmt_fr_ca_cvc";
					vinMasterTableName= "gms3_dmt_fr_ca_vinmaster";
					innerLinkTableName = "gms3_dmt_fr_ca_inrlks";
					navTableName= "gms3_dmt_fr_ca_nav";
				}
				
				String deleteSql="DELETE FROM "+ navTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ vinMasterTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ innerLinkTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ cvcTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ vinAttrTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ vinTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM "+ catTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				String locale = contentDetails.getLocale();
				locale= locale.replace("-", "_");
				deleteSql="DELETE FROM gms3_vc_model_year_details WHERE VC_MY_DOCUMENT_ID='"+contentDetails.getImDocumentId().trim()+"' "
						+ " AND VC_MY_LOCALE='"+locale.trim()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				deleteSql="DELETE FROM gms3_vc_vin_details WHERE VC_VIN_DOCUMENT_ID='"+contentDetails.getImDocumentId().trim()+"' "
						+ " AND VC_VIN_LOCALE='"+locale.trim()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				locale= null;
				

				/*
				 * Update other Details in gms3_dmt_conv_imdoc
				 */
				String updateDocDetailsSql="UPDATE "+tableName+" SET DC_IM_DOC_CONTENT_ID=?,"
						+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_STATUS=?, DC_IM_DOC_FLAG=?, "
						+ " DC_UPDATED_TMSTP=? "
						+ " WHERE DC_IM_DOC_ID=?";
				pstmt = conn.prepareStatement(updateDocDetailsSql);
				pstmt.setString(1, contentDetails.getImContentType());
				pstmt.setString(2, contentDetails.getImVersion());
				pstmt.setString(3, contentDetails.getImResourcePath());
				pstmt.setString(4, contentDetails.getImDocStatus());
				pstmt.setString(5, ApplicationProperties.getProperty("flag.value.delete"));
				pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(7, contentDetails.getImDocumentId());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt = null;
				updateDocDetailsSql = null;
				
				// commit the transaction
				conn.commit();
				
				tableName=null;
				catTableName=null;
				vinTableName=null;
				vinAttrTableName=null;
				cvcTableName=null;
				vinMasterTableName=null;
				innerLinkTableName=null;
				navTableName=null;
			}
			else
			{
				logger.info("deleteDocumentDetails :: IM DOCUMENT IS NULL IN OBJECT. Return false");
				/*
				 * Add the Document Details to
				 */
				if (null == getFailedDatabaseSaveDocumentDetailsForDelete() 	|| getFailedDatabaseSaveDocumentDetailsForDelete().size() <= 0) 
				{
					// initialize the list
					setFailedDatabaseSaveDocumentDetailsForDelete(new ArrayList<Map<Object, Object>>());
				}
				/*
				 * now create a Map Object and add the following IM Document ID
				 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
				 */
				Map<Object, Object> errorMap = new HashMap<Object, Object>();
				errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
				errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
				errorMap.put("CHANNEL", contentDetails.getChannelName());
				errorMap.put("ERROR_CODE", "DBEXP001");
				errorMap.put("ERROR_MESSAGE", "IM_DOCUMENT_ID AS PARAMETER IS NULL.");
				errorMap.put("RECORD_ID", contentDetails.getImContentType());
				errorMap.put("OPERATION_TYPE", "DELETE");
				errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
				errorMap.put("CONTENT_DETAILS", contentDetails);
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
				errorMap.put("DATE_TIME", sdf.format(date));
				// set date to null
				date = null;
				// set sdf to null
				sdf = null;

				// add errorMap to FailedDatabaseSaveDocumentDetails
				getFailedDatabaseSaveDocumentDetailsForDelete().add(errorMap);
				// set errorMap to null
				errorMap = null;
				
				
				return false;
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "deleteDocumentDetails()", e);
			
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			String errorCode = e.getMessage();
			String errorMessage = writer.toString();

			/*
			 * Add the Document Details to
			 */
			if (null == getFailedDatabaseSaveDocumentDetailsForDelete() 	|| getFailedDatabaseSaveDocumentDetailsForDelete().size() <= 0) 
			{
				// initialize the list
				setFailedDatabaseSaveDocumentDetailsForDelete(new ArrayList<Map<Object, Object>>());
			}
			/*
			 * now create a Map Object and add the following IM Document ID
			 * CONTENT TYPE ERROR CODE ERROR MESSAGE DATE TIME
			 */
			Map<Object, Object> errorMap = new HashMap<Object, Object>();
			errorMap.put("IM_DOCUMENT_ID", contentDetails.getImDocumentId());
			errorMap.put("LOCALE", ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()));
			errorMap.put("CHANNEL", contentDetails.getChannelName());
			errorMap.put("ERROR_CODE", errorCode);
			errorMap.put("ERROR_MESSAGE", errorMessage);
			errorMap.put("RECORD_ID", contentDetails.getImContentType());
			errorMap.put("OPERATION_TYPE", "CREATE");
			errorMap.put("DOCUMENT_LOCATION", contentDetails.getFilePath());
			errorMap.put("CONTENT_DETAILS", contentDetails);
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
			errorMap.put("DATE_TIME", sdf.format(date));
			// set date to null
			date = null;
			// set sdf to null
			sdf = null;

			// add errorMap to getFailedDatabaseSaveDocumentDetailsForDelete
			getFailedDatabaseSaveDocumentDetailsForDelete().add(errorMap);
			// set errorMap to null
			errorMap = null;
			// set errorCode to null
			errorCode = null;
			// set errorMessage to null
			errorMessage = null;
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			
			return false;
		}
		finally
		{
			if(null!=conn)
			{
				conn.close();
			}
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return true;
		
	}
	
	/**
	 * Function will return all the VIN LIST FOR ENGINE / MODEL
	 * @param langCode
	 * @param bookCode
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public  ArrayList<VINEntFileDetails> getMasterVinDetailsList(String langCode, String bookCode, String type) throws SQLException 
	{
		logger.info("getMasterVinDetailsList :: Method Starts.");
		ArrayList<VINEntFileDetails> vinMasterList = new ArrayList<VINEntFileDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode) && null!=bookCode && !"".equals(bookCode) 
					&& null!=type &&  !"".equals(type))
			{
				/*
				 * IDENTIFY COUNTRY CODE FROM LANGUAGE,
				 * LANGUAGE CODE WILL BE AS IT IS
				 */
				String countryLocaleCode="";
				if(langCode.indexOf("-")!=-1)
				{
					countryLocaleCode = langCode.substring(langCode.indexOf("-")+1, langCode.length());
				}
				
				conn = getConnection();
				ArrayList<String> typeCodesList =new ArrayList<String>();
				String sql="";
				if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()))
				{
					/*
					 * Fetch all the Engine TypeList Code
					 */
					sql ="SELECT A.MDM_ET_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_engine_type A,gms3_mdm_engine_book B, "
							+ "gms3_mdm_manual_language C,"
							+ "gms3_mdm_country_locale D WHERE A.MDM_EB_ID=B.MDM_EB_ID AND A.MDM_ML_ID =C.MDM_ML_ID AND "
							+ "A.MDM_CL_ID=D.MDM_CL_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
									+ "AND TRIM(LOWER(D.MDM_CL_LOCALE_CODE)) ='"+countryLocaleCode.trim().toLowerCase()+"' "
											+ "AND TRIM(LOWER(B.MDM_EB_CODE))='"+bookCode.trim().toLowerCase()+"' "
							+ "AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				else if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
						|| type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
				{
					sql ="SELECT A.MDM_TRANS_TYPE_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_trans_type A,gms3_mdm_trans_book B, gms3_mdm_manual_language C, "
							+ "gms3_mdm_country_locale D WHERE A.MDM_TRANSBK_ID=B.MDM_TRANSBK_ID AND "
							+ "A.MDM_ML_ID =C.MDM_ML_ID AND A.MDM_CL_ID=D.MDM_CL_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(D.MDM_CL_LOCALE_CODE)) ='"+countryLocaleCode.trim().toLowerCase()+"' "
											+ " AND TRIM(LOWER(B.MDM_TRANSBK_CODE))='"+bookCode.trim().toLowerCase()+"'"
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				
				logger.info("getMasterVinDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					String typeCode= rs.getString("TYPE_CODE");
					if(null!=typeCode && !"".equals(typeCode))
					{
						typeCode = typeCode.trim();
						typeCodesList.add(typeCode);
					}
					typeCode = null;
				}
				rs.close();rs =null;
				stmt.close();stmt =null;
				sql = null;
				
				/*
				 * Now Fetch all the VIN DATA MAPPED WITH EACH TYPE CODE
				 */
				if(null!=typeCodesList && typeCodesList.size()>0)
				{
					for(int a=0;a<typeCodesList.size();a++)
					{
						String processingCode=String.valueOf(typeCodesList.get(a));
						/*
						 * IF TYPE IS ENGINE - check for MDM_VIN_ENGINE_CODE
						 * ELSE Check for MDM_VIN_TRANSMISSION_CODE
						 */
						String fetchVinSql = "SELECT A.* FROM gms3_mdm_vin_detail A, "
								+ " gms3_mdm_manual_language C , gms3_mdm_country_locale D "
								+ "WHERE  A.MDM_ML_ID=C.MDM_ML_ID AND A.MDM_CL_ID=D.MDM_CL_ID AND  "
								+ " A.MDM_VIN_FLAG='"
								+ ApplicationProperties
										.getProperty("flag.value.active")
								+ "'  AND TRIM(LOWER(C.MDM_ML_LANG_CODE))='"
								+ langCode.trim().toLowerCase()
								+ "' AND TRIM(LOWER(D.MDM_CL_LOCALE_CODE)) ='"
								+ countryLocaleCode.trim().toLowerCase() + "'";
						logger.info("getMasterVinDetailsList :: fetchVinSql :: > " + fetchVinSql);
						stmt = conn.createStatement();
						rs=  stmt.executeQuery(fetchVinSql);
						while(rs.next())
						{
							String codeToCheck="";
							if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()))
							{
								codeToCheck = rs.getString("MDM_VIN_ENGINE_CODE");
							}
							else if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
									|| type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
							{
								codeToCheck = rs.getString("MDM_VIN_TRANSMISSION_CODE");
							}
							
							if(null!=codeToCheck && !"".equals(codeToCheck))
							{
								String[] tokens = codeToCheck.split(",");
								if(null!=tokens  && tokens.length>0)
								{
									for(int t=0;t<tokens.length;t++)
									{
										if(processingCode.trim().toLowerCase().equals(tokens[t].trim().toLowerCase()))
										{
											// add VIN Details to MASTER LIST
											VINEntFileDetails details = new VINEntFileDetails();
											String code = rs.getString("MDM_CRLN_NAME_ENG_LANG");
											if(null!=code && !"".equals(code))
											{
												details.setModelCode(code);
											}
											code = null;
											details.setVinCarline(rs.getString("MDM_CRLN_CODE"));
											if(null!=details.getVinCarline() && !"".equals(details.getVinCarline()))
											{
												details.setVinCarline(details.getVinCarline().trim());
											}
											details.setVinWMI(rs.getString("MDM_VIN_WMI_CODE"));
											if(null!=details.getVinWMI() && !"".equals(details.getVinWMI()))
											{
												details.setVinWMI(details.getVinWMI().trim());
											}
											details.setVinVDS(rs.getString("MDM_VIN_VDS_CODE"));
											if(null!=details.getVinVDS() && !"".equals(details.getVinVDS()))
											{
												details.setVinVDS(details.getVinVDS().trim());
											}
											details.setVinStartRange(rs.getString("MDM_VIN_VIS_START_RANGE"));
											if(null!=details.getVinStartRange() && !"".equals(details.getVinStartRange()))
											{
												details.setVinStartRange(details.getVinStartRange().trim());
											}
											details.setVinEndRange(rs.getString("MDM_VIN_VIS_END_RANGE"));
											if(null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()))
											{
												details.setVinEndRange(details.getVinEndRange().trim());
											}
											// set LangCode as well for Year Ranges
											details.setLangCodeForYearRange(langCode);
											/* 
											 * ADD PROCESSING CODE AS ENGINE TYPE / MISSION TYPE CODE. This will be passed as CATEGORY WITH DOCUMENT
											 */
											details.setVinMasterEngineMissionType(processingCode.trim());
											vinMasterList.add(details);
											details = null;
											break;
										}
									}
								}
								tokens = null;
							}
							codeToCheck = null;
						}
						rs.close();rs=null;
						stmt.close();stmt=null;
						fetchVinSql=null;
					}
				}
				countryLocaleCode = null;
				
				if(null!=vinMasterList && vinMasterList.size()>0)
				{
					/*
					 * DATE CHANGE 17 SEPT 2026
					 * IF PROCESSING LOCALE IS EN_US 
					 * 	THEN FETCH MODEL FOR EN_CA LOCALE AS WELL ON THE BASIS OF WMI CODE, AND CARLINE CODE
					 * AND CHECK IF IT IS ALREADY ADDED - THEN DO NOTHING
					 * ELSE ADD A NEW ROW WITH CARLINE NAME AND CODE - DO NOTHING WITH WMI, VDS, VIS RANGES - AS THIS MODEL YEAR NEEDS TO BE MAPPED
					 * NOT THE VIN RANGES
					 */
					String checkLocForENCA = langCode;
					checkLocForENCA = checkLocForENCA.replace("-", "_");
					String enCaLocale=ApplicationProperties.getProperty("en-ca");
					if(null!=enCaLocale && !"".equals(enCaLocale))
					{
						enCaLocale = enCaLocale.replace("_", "-");
						boolean enCAOpApplicable=false;
						if(checkLocForENCA.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-us").trim().toLowerCase()))
						{
							// enCA Locale Operation Applicable
							enCAOpApplicable = true;
						}
						
						if(enCAOpApplicable==true)
						{
							sql=null;
							List<VINEntFileDetails> tempCAModelsList = new ArrayList<VINEntFileDetails>();
							for(int a=0;a<vinMasterList.size();a++)
							{
								VINEntFileDetails details = (VINEntFileDetails)vinMasterList.get(a);
								if(null!=details.getVinWMI() && !"".equals(details.getVinWMI()) && null!=details.getVinCarline() && !"".equals(details.getVinCarline()))
								{
									sql="SELECT MDM_CRLN_NAME_ENG_LANG FROM gms3_mdm_carline_codes WHERE MDM_CRLN_WMI_CODE='"+details.getVinWMI()+"' "
											+ " AND MDM_CRLN_CODE='"+details.getVinCarline()+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
											+ "AND MDM_ML_LANG_CODE='"+enCaLocale+"'";
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									VINEntFileDetails tDetails = null;
									while(rs.next())
									{
										tDetails = new VINEntFileDetails();
										if(null!=rs.getString("MDM_CRLN_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_ENG_LANG")))
										{
											tDetails.setModelCode(rs.getString("MDM_CRLN_NAME_ENG_LANG"));
										}
										// set rest of the VIN Details as is
										tDetails.setVinWMI(details.getVinWMI());
										tDetails.setVinCarline(details.getVinCarline());
										tDetails.setVinVDS(details.getVinVDS());
										tDetails.setVinStartRange(details.getVinStartRange());
										tDetails.setVinEndRange(details.getVinEndRange());
										tDetails.setLangCodeForYearRange(enCaLocale);
										tempCAModelsList.add(tDetails);
										tDetails = null;
									}
									rs.close();rs=null;
									stmt.close();stmt=null;
									sql = null;
									tDetails = null;
								}
								details = null;
							}
							
							/*
							 * NOW ITERATE AND VERIFY IF THE MODEL EXISTS - THEN DO NOT ADD ELSE ADD TO VIN MASTER LIST 
							 */
							
							if(null!=tempCAModelsList && tempCAModelsList.size()>0)
							{
								VINEntFileDetails tDetails = null;
								VINEntFileDetails checkDetails = null;
								for(int re=0;re<tempCAModelsList.size();re++)
								{
									tDetails = (VINEntFileDetails) tempCAModelsList.get(re);
									if(null!=tDetails.getModelCode() && !"".equals(tDetails.getModelCode()) && 
											null!=tDetails.getVinWMI() && !"".equals(tDetails.getVinWMI()) && 
											null!=tDetails.getVinVDS() && !"".equals(tDetails.getVinVDS()) && 
											null!=tDetails.getVinCarline() && !"".equals(tDetails.getVinCarline()) && 
											null!=tDetails.getVinStartRange() && !"".equals(tDetails.getVinStartRange()) && 
											null!=tDetails.getVinEndRange() && !"".equals(tDetails.getVinEndRange()))
									{
										boolean addToVINMaster=true;
										if(null!=vinMasterList && vinMasterList.size()>0)
										{
											checkDetails = null;
											for(int wrt=0;wrt<vinMasterList.size();wrt++)
											{
												checkDetails = (VINEntFileDetails)vinMasterList.get(wrt);
												if(null!=checkDetails.getModelCode() && !"".equals(checkDetails.getModelCode()) && 
														null!=checkDetails.getVinWMI() && !"".equals(checkDetails.getVinWMI()) && 
														null!=checkDetails.getVinVDS() && !"".equals(checkDetails.getVinVDS()) && 
														null!=checkDetails.getVinCarline() && !"".equals(checkDetails.getVinCarline()) && 
														null!=checkDetails.getVinStartRange() && !"".equals(checkDetails.getVinStartRange()) && 
														null!=checkDetails.getVinEndRange() && !"".equals(checkDetails.getVinEndRange()))
												{
													if(tDetails.getModelCode().equals(checkDetails.getModelCode()) && 
															tDetails.getVinWMI().equals(checkDetails.getVinWMI()) &&	
															tDetails.getVinVDS().equals(checkDetails.getVinVDS()) && 
															tDetails.getVinCarline().equals(checkDetails.getVinCarline()) &&
															tDetails.getVinStartRange().equals(checkDetails.getVinStartRange()) &&
															tDetails.getVinEndRange().equals(checkDetails.getVinEndRange()))
													{
														addToVINMaster = false;
														break;
													}
												}
												checkDetails = null;
											}
											checkDetails = null;
										}
										
										if(addToVINMaster==true)
										{
											VINEntFileDetails newTDetails = new VINEntFileDetails();
											newTDetails.setModelCode(tDetails.getModelCode());
											// set rest of the VIN Details as is
											newTDetails.setVinWMI(tDetails.getVinWMI());
											newTDetails.setVinCarline(tDetails.getVinCarline());
											newTDetails.setVinVDS(tDetails.getVinVDS());
											newTDetails.setVinStartRange(tDetails.getVinStartRange());
											newTDetails.setVinEndRange(tDetails.getVinEndRange());
											newTDetails.setLangCodeForYearRange(tDetails.getLangCodeForYearRange());
											if(null==vinMasterList || vinMasterList.size()<=0)
											{
												vinMasterList = new ArrayList<VINEntFileDetails>();
											}
											vinMasterList.add(newTDetails);
											newTDetails = null;
										}
									}
									tDetails = null;
								}
								tDetails = null;
								checkDetails = null;
							}
							tempCAModelsList = null;
						}
					}
					checkLocForENCA = null;
					enCaLocale = null;
					
					
					
					/*
					 * iterate VIN Master List and identify Year from each VDS on the basis of 7th position character
					 */
					sql = null;
					for(int a=0;a<vinMasterList.size();a++)
					{
						VINEntFileDetails details = (VINEntFileDetails)vinMasterList.get(a);
						if(null!=details.getVinVDS() && !"".equals(details.getVinVDS()))
						{
							if(details.getVinVDS().length()>=7)
							{
								String yearChar = String.valueOf(details.getVinVDS().charAt(6));
								if(null!=yearChar && !"".equals(yearChar))
								{
									/*
									 * FETCH YEAR VALUE FOR THE CODE, MULTIPLE VALUES CAN RETURN SO - 
									 * IDENTIFY WHICH YEAR IS APPLICABLE FETCH YEAR RANGE ON THE BASIS OF CARLINE CODE & WMI CODE & LOCALE CODE 
									 * & CARLIEN NAME ENG
									 * CHECK WHICH YEAR LIES IN THE RANGE AND ACCORDINGLY IDENITY IT.
									 */
									
									// instead of landCode use details.getLangCodeForYearRange()
									sql="SELECT MDM_VIN_XREF_YEAR FROM gms3_mdm_vin_xref WHERE MDM_VIN_XREF_CODE='"+yearChar+"' AND MDM_ML_LANG_CODE='"+details.getLangCodeForYearRange()+"'"
									+ " AND MDM_VIN_XREF_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND"
									+ " MDM_VIN_XREF_YEAR BETWEEN (SELECT MDM_CRLN_YEAR_START FROM gms3_mdm_carline_codes WHERE "
									+ " MDM_CRLN_CODE='"+details.getVinCarline()+"' AND MDM_CRLN_WMI_CODE='"+details.getVinWMI()+"' "
									+ " AND MDM_CRLN_NAME_ENG_LANG='"+details.getModelCode()+"' "
									+ " AND MDM_ML_LANG_CODE='"+details.getLangCodeForYearRange()+"' AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"') "
									+ " AND (SELECT MDM_CRLN_YEAR_END FROM gms3_mdm_carline_codes WHERE MDM_CRLN_CODE='"+details.getVinCarline()+"' "
									+ " AND MDM_CRLN_WMI_CODE='"+details.getVinWMI()+"' AND MDM_ML_LANG_CODE='"+details.getLangCodeForYearRange()+"' "
									+ " AND MDM_CRLN_NAME_ENG_LANG='"+details.getModelCode()+"' AND  "
									+ " MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"')";
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									if(rs.next())
									{
										if(null!=rs.getString("MDM_VIN_XREF_YEAR") && !"".equals(rs.getString("MDM_VIN_XREF_YEAR")))
										{
											details.setModelYear(rs.getString("MDM_VIN_XREF_YEAR").trim());
										}
									}
									stmt.close();stmt = null;
									rs.close();rs=null;
									sql = null;
								}
								yearChar = null;
							}
						}
					}
				}
			}
			else
			{
				logger.info("getMasterVinDetailsList :: Language Code /  Book Code / Type as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getCategoryName :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getMasterVinDetailsList", e);
			logger.info("getCategoryName :: ################ Exception ################");
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
			bookCode=null;
			type = null;
		}
		logger.info("getMasterVinDetailsList :: Method Ends.");
		return vinMasterList;
	}

	/**
	 * Function will fetch all the mapped Types with Engine / Mission Book Codes
	 * @param langCode
	 * @param bookCode
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public  ArrayList<String> getEngineMissionTypeList(String langCode, String bookCode, String type) throws SQLException 
	{
		logger.info("getEngineMissionTypeList :: Method Starts.");
		ArrayList<String> typeCodesList = new ArrayList<String>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langCode && !"".equals(langCode) && null!=bookCode && !"".equals(bookCode) 
					&& null!=type &&  !"".equals(type))
			{
				/*
				 * IDENTIFY COUNTRY CODE FROM LANGUAGE,
				 * LANGUAGE CODE WILL BE AS IT IS
				 */
				String countryLocaleCode="";
				if(langCode.indexOf("-")!=-1)
				{
					countryLocaleCode = langCode.substring(langCode.indexOf("-")+1, langCode.length());
				}
				
				conn = getConnection();
				String sql="";
				if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()))
				{
					/*
					 * Fetch all the Engine TypeList Code
					 */
					sql ="SELECT A.MDM_ET_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_engine_type A,gms3_mdm_engine_book B, "
							+ "gms3_mdm_manual_language C,"
							+ "gms3_mdm_country_locale D WHERE A.MDM_EB_ID=B.MDM_EB_ID AND A.MDM_ML_ID =C.MDM_ML_ID AND "
							+ "A.MDM_CL_ID=D.MDM_CL_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
									+ "AND TRIM(LOWER(D.MDM_CL_LOCALE_CODE)) ='"+countryLocaleCode.trim().toLowerCase()+"' "
											+ "AND TRIM(LOWER(B.MDM_EB_CODE))='"+bookCode.trim().toLowerCase()+"' "
							+ "AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				else if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
						|| type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
				{
					sql ="SELECT A.MDM_TRANS_TYPE_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_trans_type A,gms3_mdm_trans_book B, gms3_mdm_manual_language C, "
							+ "gms3_mdm_country_locale D WHERE A.MDM_TRANSBK_ID=B.MDM_TRANSBK_ID AND "
							+ "A.MDM_ML_ID =C.MDM_ML_ID AND A.MDM_CL_ID=D.MDM_CL_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(D.MDM_CL_LOCALE_CODE)) ='"+countryLocaleCode.trim().toLowerCase()+"' "
											+ " AND TRIM(LOWER(B.MDM_TRANSBK_CODE))='"+bookCode.trim().toLowerCase()+"'"
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				
				logger.info("getEngineMissionTypeList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					String typeCode= rs.getString("TYPE_CODE");
					if(null!=typeCode && !"".equals(typeCode))
					{
						typeCode = typeCode.trim();
						typeCodesList.add(typeCode);
					}
					typeCode = null;
				}
				rs.close();rs =null;
				stmt.close();stmt =null;
				sql = null;
			}
			else
			{
				logger.info("getEngineMissionTypeList :: Language Code /  Book Code / Type as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			logger.info("getEngineMissionTypeList :: ################ Exception ################");
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getEngineMissionTypeList", e);
			logger.info("getEngineMissionTypeList :: ################ Exception ################");
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
			bookCode=null;
			type = null;
		}
		logger.info("getEngineMissionTypeList :: Method Ends.");
		return typeCodesList;
	}
	
	/**
	 * FUNCTION WILL PROCESS VIN MASTER INFORMATION
	 * @param contentDetails
	 * @param conn
	 * @throws SQLException 
	 */
	private  void processVINMasterInformation(ContentDetails contentDetails, Connection conn, String tableName) throws SQLException
	{
		Statement stmt = null;
		PreparedStatement pstmt = null;
		
		String deleteSql="DELETE FROM "+ tableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		VINEntFileDetails details = null;
		String insertVinEntSql =null;
		int partitionSize=100;
		ArrayList<List<VINEntFileDetails>> partitions = new ArrayList<List<VINEntFileDetails>>();
		for (int i=0; i<contentDetails.getMasterVinList().size(); i += partitionSize) {
			partitions.add(contentDetails.getMasterVinList().subList(i, Math.min(i + partitionSize, contentDetails.getMasterVinList().size())));
		}
		
		if(null!=partitions && partitions.size()>0)
		{
			for(List<VINEntFileDetails> subList : partitions)
			{
				insertVinEntSql =null;
				if(null!=subList && subList.size()>0)
				{
					details = null;
					pstmt= null;
					insertVinEntSql="INSERT INTO "+tableName+" (DC_IM_DOC_ID,DC_VM_MANUAL_TYPE,"
							+ " DC_VM_MANUAL_TYPE_CODE, "
							+ "DC_VM_CARLINE_CODE,DC_VM_WMI_CODE,DC_VM_VDS_CODE,DC_VM_VIS_START_RANGE,DC_VM_VIS_END_RANGE,"
							+ "DC_VM_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_VM_REFKEY) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertVinEntSql);
					for(int a=0;a<subList.size();a++)
					{
						details = (VINEntFileDetails)subList.get(a);
						pstmt.setString(1, contentDetails.getImDocumentId());
						pstmt.setString(2, contentDetails.getModel());
						pstmt.setString(3, contentDetails.getManualType());
						pstmt.setString(4, details.getVinCarline());
						pstmt.setString(5, details.getVinWMI());
						pstmt.setString(6, details.getVinVDS());
						pstmt.setString(7, details.getVinStartRange());
						pstmt.setString(8, details.getVinEndRange());
						pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(10, contentDetails.getFilePath());
						pstmt.setString(11, details.getVinRefKey());
						pstmt.addBatch();
						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					insertVinEntSql = null;
					
				}
				subList= null;
			}
		}
		partitions=  null;
		insertVinEntSql =null;
		details = null;
	}

	private void performInnerLinksOperation(ContentDetails contentDetails, Connection conn, String tableName)  throws SQLException
	{
		Statement stmt = null;
		PreparedStatement pstmt = null;
		LinkDetails linkDetails=null;
		int partitionSize=100;
		
		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteInnerLinksSql="DELETE FROM "+tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"' ";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteInnerLinksSql);
		stmt.close();stmt=null;
		deleteInnerLinksSql=null;
		
		ArrayList<LinkDetails> linksList = new ArrayList<LinkDetails>();
		if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
		{
			linksList.addAll(contentDetails.getInnerLinksList());
		}
		if(null!=contentDetails.getAdditionalInnerLinks() && contentDetails.getAdditionalInnerLinks().size()>0)
		{
			linksList.addAll(contentDetails.getAdditionalInnerLinks());
		}
//		if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
		if(null!=linksList && linksList.size()>0)
		{
			ArrayList<List<LinkDetails>> partitions = new ArrayList<List<LinkDetails>>();
			for (int i=0; i<linksList.size(); i += partitionSize) {
				partitions.add(linksList.subList(i, Math.min(i + partitionSize, linksList.size())));
			}
			if(null!=partitions && partitions.size()>0)
			{
				String sql = "";
				linkDetails = null;
				for(List<LinkDetails> subList : partitions)
				{
					if(null!=subList && subList.size()>0)
					{
						sql= "INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
								+ "DC_SOURCE_NETWORK_LOC,DC_INNER_DOC_LINK_PATH,DC_INNER_IM_DOC_ID,"
								+ "DC_INNER_LINK_UPDATED_STATUS,DC_CREATED_TMSTP, DC_INNER_DOC_VTOC_NAME) VALUES(?,?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(sql);
						linkDetails = null;
						for(int b=0;b<subList.size();b++)
						{
							linkDetails=  (LinkDetails)subList.get(b);
							pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
							pstmt.setString(2, contentDetails.getImDocumentId());
							pstmt.setString(3, contentDetails.getFilePath());
							pstmt.setString(4, linkDetails.getInnerLinkPath());
							pstmt.setString(5, linkDetails.getInnnerLinkDocumentId());
							pstmt.setString(6, linkDetails.getMapStatus());
							pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
							pstmt.setString(8, linkDetails.getVtocFileName());
							pstmt.addBatch();
							linkDetails=  null;
						}
						pstmt.executeBatch();
						pstmt.close();pstmt=null;
						sql = null;
					}
					subList= null;
				}
			}
			partitions = null;
			linkDetails = null;
		}
		linksList = null;
	}
	
	
	private  void processCategoryInformation(ContentDetails contentDetails, Connection conn, String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;
		String deleteSql="DELETE FROM "+ tableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;

		CategoryDetails details = null;
		String insertCategoriesSql=null;
		int partitionSize=100;
		ArrayList<List<CategoryDetails>> partitions = new ArrayList<List<CategoryDetails>>();
		for (int i=0; i<contentDetails.getCategoryList().size(); i += partitionSize) {
			partitions.add(contentDetails.getCategoryList().subList(i, Math.min(i + partitionSize, contentDetails.getCategoryList().size())));
		}
		
		if(null!=partitions && partitions.size()>0)
		{
			for(List<CategoryDetails> subList : partitions)
			{
				if(null!=subList && subList.size()>0)
				{
					pstmt = null;
					details = null;
					insertCategoriesSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
							+ "DC_SOURCE_NETWORK_LOC,DC_IM_DOC_CATEGORY_NAME,DC_IM_DOC_CATEGORY_REF_KEY,"
							+ "DC_CREATED_TMSTP, DC_IM_DOC_CATEGORY_TYPE) VALUES(?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertCategoriesSql);
					for(int a=0;a<subList.size();a++)
					{
						details  =(CategoryDetails)subList.get(a);
						pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
						pstmt.setString(2, contentDetails.getImDocumentId());
						pstmt.setString(3, contentDetails.getFilePath());
						pstmt.setString(4, details.getCategoryName());
						pstmt.setString(5, details.getCategoryRefKey());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(7, details.getCategoryType());
						pstmt.addBatch();
						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					insertCategoriesSql=  null;
					details = null;
				}
			}
		}
		partitions=null;
		details=  null;
		insertCategoriesSql=  null;
	}

	private  void processVINENTInformation(ContentDetails contentDetails, Connection conn, String tableName) throws SQLException
	{
		Statement stmt = null;
		PreparedStatement pstmt = null;
		
		String deleteSql="DELETE FROM "+ tableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		VINEntFileDetails details= null;
		String insertVinEntSql = null;
		int partitionSize=100;
		ArrayList<List<VINEntFileDetails>> partitions = new ArrayList<List<VINEntFileDetails>>();
		for (int i=0; i<contentDetails.getVinEntDetailsList().size(); i += partitionSize) {
			partitions.add(contentDetails.getVinEntDetailsList().subList(i, Math.min(i + partitionSize, contentDetails.getVinEntDetailsList().size())));
		}
		
		if(null!=partitions && partitions.size()>0)
		{
			for(List<VINEntFileDetails> subList : partitions)
			{
				if(null!=subList && subList.size()>0)
				{
					details = null;
					pstmt=  null;
					insertVinEntSql="INSERT INTO "+tableName+" (DC_IM_DOC_ID,"
							+ "DC_VIN_FILE_NAME,DC_VIN_MODEL_YEAR,DC_VIN_WMI,DC_VIN_CARLINE,DC_VIN_VDS,DC_VIN_START,DC_VIN_END,"
							+ "DC_VIN_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_VIN_REFKEY) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertVinEntSql);
					for(int a=0;a<subList.size();a++)
					{
						details = (VINEntFileDetails)contentDetails.getVinEntDetailsList().get(a);
						pstmt.setString(1, contentDetails.getImDocumentId());
						pstmt.setString(2, details.getFileName());
						pstmt.setString(3, details.getModelYear());
						pstmt.setString(4, details.getVinWMI());
						pstmt.setString(5, details.getVinCarline());
						pstmt.setString(6, details.getVinVDS());
						pstmt.setString(7, details.getVinStartRange());
						pstmt.setString(8, details.getVinEndRange());
						pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(10, contentDetails.getFilePath());
						pstmt.setString(11, details.getVinRefKey());
						pstmt.addBatch();
						details=  null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					insertVinEntSql = null;
					details = null;
					
				}
			}
		}
		partitions = null;
		insertVinEntSql =null;
		details = null;
	}

	private  void processVINAttributeENTInformation(ContentDetails contentDetails, Connection conn, String tableName) throws SQLException
	{
		Statement stmt = null;
		PreparedStatement pstmt = null;
		
		String deleteSql="DELETE FROM "+ tableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		VinAttributeEntFileDetails details = null;
		String insertVinEntSql = null;
		int partitionSize=100;
		ArrayList<List<VinAttributeEntFileDetails>> partitions = new ArrayList<List<VinAttributeEntFileDetails>>();
		for (int i=0; i<contentDetails.getVinAttributeEntDetailsList().size(); i += partitionSize) {
			partitions.add(contentDetails.getVinAttributeEntDetailsList().subList(i, Math.min(i + partitionSize, contentDetails.getVinAttributeEntDetailsList().size())));
		}
		
		if(null!=partitions && partitions.size()>0)
		{
			for(List<VinAttributeEntFileDetails> subList : partitions)
			{
				if(null!=subList && subList.size()>0)
				{
					details=  null;
					pstmt = null;
					insertVinEntSql="INSERT INTO "+tableName+" (DC_IM_DOC_ID,"
							+ "DC_VIN_ATTR_FILE_NAME,DC_VIN_ATTR_TRANS_TYPE,DC_VIN_ATTR_ENGINE_TYPE,DC_VIN_ATTR_BODY_TYPE,"
							+ "DC_VIN_ATTR_BODY_TYPE_VAL,DC_VIN_ATTR_AXLE_TYPE,DC_VIN_ATTR_AXLE_TYPE_VAL,"
							+ "DC_VIN_ATTR_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC) VALUES(?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertVinEntSql);
					for(int a=0;a<subList.size();a++)
					{
						details = (VinAttributeEntFileDetails)subList.get(a);
						pstmt.setString(1, contentDetails.getImDocumentId());
						pstmt.setString(2, details.getFileName());
						pstmt.setString(3, details.getVinTransType());
						pstmt.setString(4, details.getVinEngineType());
						pstmt.setString(5, details.getVinBodyType());
						pstmt.setString(6, details.getVinBodyTypeValue());
						pstmt.setString(7, details.getVinAxleType());
						pstmt.setString(8, details.getVinAxleTypeValue());
						pstmt.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(10, contentDetails.getFilePath());
						pstmt.addBatch();
						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					insertVinEntSql = null;
					details = null;
				}
				subList=  null;
			}
		}
		partitions=  null;
		insertVinEntSql =null;
		details = null;
	}

	private  void processCVCCategoryInformation(ContentDetails contentDetails, Connection conn,String tableName) throws SQLException
	{
		Statement stmt  = null;
		PreparedStatement pstmt=null;

		String deleteSql="DELETE FROM "+ tableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		CVCCategoryDetails details = null;
		String insertCVCSql = null;
		int partitionSize=100;
		ArrayList<List<CVCCategoryDetails>> partitions = new ArrayList<List<CVCCategoryDetails>>();
		for (int i=0; i<contentDetails.getCvcCategoryList().size(); i += partitionSize) {
			partitions.add(contentDetails.getCvcCategoryList().subList(i, Math.min(i + partitionSize, contentDetails.getCvcCategoryList().size())));
		}
		
		if(null!=partitions && partitions.size()>0)
		{
			for(List<CVCCategoryDetails> subList : partitions)
			{
				if(null!=subList && subList.size()>0)
				{
					details = null;
					pstmt=  null;
					insertCVCSql="INSERT INTO "+tableName+" (DC_IM_DOC_ID,DC_CVC_CAT_CODE,"
							+ " DC_CVC_SYM_CODE, "
							+ "DC_CVC_SUBSYM_CODE,DC_CVC_CON_CODE,DC_CVC_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC) VALUES(?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertCVCSql);
					for(int a=0;a<subList.size();a++)
					{
						details = (CVCCategoryDetails)subList.get(a);
						pstmt.setString(1, contentDetails.getImDocumentId());
						pstmt.setString(2, details.getCategoryCode());
						pstmt.setString(3, details.getSymptomCode());
						pstmt.setString(4, details.getSubSymptomCode());
						pstmt.setString(5, details.getConditionCode());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(7, contentDetails.getFilePath());
						pstmt.addBatch();
						details= null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					insertCVCSql= null;
					details = null;
				}
				subList = null;
			}
		}
		partitions = null;
		insertCVCSql =null;
		details = null;
	}
	
	
	
	/**
	 * Function will get the ESICategory Names on the basis of Locale, Levels and Codes
	 * @param level
	 * @param code1
	 * @param code2
	 * @param code3
	 * @param locale
	 * @param conn
	 * @return
	 */
	/**
	 * DISPLAY ORDER PHASE OF ONE MATERIAL FOLDER - the MNAO form of MMEDocumentManagementDAO.saveDisplayOrderDetails().
	 *
	 * The documents come from the display order (d01.txt ...) rows of the folder, each with its display order rows
	 * (getApplicableDisplayOrderList) and the vin.txt lines whose display order file list names one of those rows'
	 * files (getApplicableVINList). Written per document, after its old rows of the locale are removed:
	 *   gms3_vc_model_year_details - one row per MODEL_YEAR category of the document (gms3_dmt_<locale>_cat) x display order row
	 *   gms3_vc_vin_details        - one row (with its model and year) per vin.txt line of that display order file, and per master VIN (Engine / AT / MT ...,
	 *                                not the en_CA year range rows - 17 SEPT 2026), x display order row
	 * Display order CODES stay empty, the NAMES come from the row (MC rule); vc_content_status is Draft until the
	 * Publish Content job. The display order rows are returned with their status (SUCCESS / FAILURE) for the report.
	 */
	public List<com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails> saveDisplayOrderDetails(List<ContentDetails> documentsList, String channelFolderName,
			String locale, String model, String manualType, List<VINEntFileDetails> masterVINList)
	{
		List<com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails> processed = new ArrayList<com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails>();
		if(null==documentsList || documentsList.isEmpty() || null==locale || "".equals(locale.trim()))
		{
			return processed;
		}
		String vcLocale = locale.replace("-", "_").trim();
		String tableName = "gms3_dmt_"+vcLocale.toLowerCase()+"_imdoc";
		String catTableName = "gms3_dmt_"+vcLocale.toLowerCase()+"_cat";
		String draft = ApplicationProperties.getProperty("view.content.status.draft");
		int chunk = Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();

			// DOCUMENT TYPE (manual type) REF KEY + LABEL - the same for every document of the folder
			String documentTypeRefKey = null;
			String documentTypeLabel = null;
			String getDocumentTypeSql = null;
			String langCode = vcLocale.replace("_", "-").toLowerCase();
			if(null!=manualType && (manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) ||
					manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder"))))
			{
				documentTypeRefKey = ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toUpperCase().replace(" ", "_");
				getDocumentTypeSql = "SELECT DC_MANUAL_NAME FROM gms3_dmt_conv_manual_type WHERE TRIM(LOWER(DC_MANUAL_CODE)) = ? AND TRIM(LOWER(MDM_ML_LANG_CODE)) = ?"
						+ " AND DC_MANUAL_FLAG NOT IN (?,?)";
			}
			else
			{
				documentTypeRefKey = ConversionUtils.identifyManualTypeAsCateogry(isOverhaulModel(model) ? model : manualType);
				if(null!=documentTypeRefKey && !"".equals(documentTypeRefKey))
				{
					getDocumentTypeSql = "SELECT DC_MANUAL_NAME FROM gms3_dmt_conv_manual_type WHERE TRIM(LOWER(DC_MANUAL_REF_KEY)) = ? AND TRIM(LOWER(MDM_ML_LANG_CODE)) = ?"
							+ " AND DC_MANUAL_FLAG NOT IN (?,?)";
				}
			}
			if(null!=getDocumentTypeSql)
			{
				pstmt = conn.prepareStatement(getDocumentTypeSql);
				pstmt.setString(1, (getDocumentTypeSql.contains("DC_MANUAL_CODE") ? manualType : documentTypeRefKey).trim().toLowerCase());
				pstmt.setString(2, langCode);
				pstmt.setString(3, ApplicationProperties.getProperty("flag.value.draft"));
				pstmt.setString(4, ApplicationProperties.getProperty("flag.value.delete"));
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					documentTypeLabel = rs.getString("DC_MANUAL_NAME");
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
			}

			// THE DOCUMENT ROWS (active) BY PATH: id, title, ESI codes, created / updated
			Map<String, ContentDetails> byPath = new HashMap<String, ContentDetails>();
			for(ContentDetails cd : documentsList)
			{
				if(null!=cd.getFilePath())
				{
					byPath.put(cd.getFilePath().trim().toLowerCase(), cd);
				}
			}
			List<String> paths = new ArrayList<String>(byPath.keySet());
			for(int from=0; from<paths.size(); from+=chunk)
			{
				List<String> part = paths.subList(from, Math.min(from+chunk, paths.size()));
				pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC, DC_IM_DOC_TITLE, DC_IM_DOC_CAT_TYPE, DC_IM_DOC_SUBCAT_TYPE,"
						+ " DC_IM_DOC_SUBSUBCAT_TYPE, DC_CREATED_TMSTP, DC_UPDATED_TMSTP FROM "+tableName
						+ " WHERE DC_IM_DOC_FLAG = ? AND TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN ("+marks(part.size())+")");
				int i=1;
				pstmt.setString(i++, ApplicationProperties.getProperty("flag.value.active"));
				for(String path : part)
				{
					pstmt.setString(i++, path);
				}
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					ContentDetails cd = byPath.get(null==rs.getString("DC_SOURCE_NETWORK_LOC") ? "" : rs.getString("DC_SOURCE_NETWORK_LOC").trim().toLowerCase());
					if(null!=cd && null==cd.getImDocumentId())
					{
						cd.setImDocumentId(rs.getString("DC_IM_DOC_ID"));
						cd.setDocumentId(rs.getString("DC_IM_DOC_ID"));
						cd.setTitle(rs.getString("DC_IM_DOC_TITLE"));
						cd.setCategoryCode(rs.getString("DC_IM_DOC_CAT_TYPE"));
						cd.setSubCategoryCode(rs.getString("DC_IM_DOC_SUBCAT_TYPE"));
						cd.setSubSubCategoryCode(rs.getString("DC_IM_DOC_SUBSUBCAT_TYPE"));
						cd.setDocumentCreatedTime(rs.getTimestamp("DC_CREATED_TMSTP"));
						cd.setDocumentModifiedTime(rs.getTimestamp("DC_UPDATED_TMSTP"));
					}
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
			}

			// MODEL_YEAR CATEGORIES PER DOCUMENT ("MODEL::YEAR" names, as CategoryUtils wrote them)
			Map<String, List<String>> modelYears = new HashMap<String, List<String>>();
			List<String> docIds = new ArrayList<String>();
			for(ContentDetails cd : documentsList)
			{
				if(null!=cd.getImDocumentId() && !docIds.contains(cd.getImDocumentId()))
				{
					docIds.add(cd.getImDocumentId());
				}
			}
			for(int from=0; from<docIds.size(); from+=chunk)
			{
				List<String> part = docIds.subList(from, Math.min(from+chunk, docIds.size()));
				pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_IM_DOC_CATEGORY_NAME FROM "+catTableName
						+ " WHERE DC_IM_DOC_CATEGORY_TYPE = 'MODEL_YEAR' AND DC_IM_DOC_ID IN ("+marks(part.size())+")");
				int i=1;
				for(String id : part)
				{
					pstmt.setString(i++, id);
				}
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					String key = rs.getString("DC_IM_DOC_ID").trim().toLowerCase();
					List<String> names = modelYears.get(key);
					if(null==names)
					{
						names = new ArrayList<String>();
						modelYears.put(key, names);
					}
					if(null!=rs.getString("DC_IM_DOC_CATEGORY_NAME") && !names.contains(rs.getString("DC_IM_DOC_CATEGORY_NAME")))
					{
						names.add(rs.getString("DC_IM_DOC_CATEGORY_NAME"));
					}
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
			}

			List<ESICategoryDetails> esiCategoryList = getESICategoryMasterList(vcLocale, conn);

			// THE VIEW CONTENT ROWS, DOCUMENT BY DOCUMENT
			List<ContentDetails> written = new ArrayList<ContentDetails>();
			List<MNAOModelYearViewContentDetails> myRows = new ArrayList<MNAOModelYearViewContentDetails>();
			List<MNAOModelYearViewContentDetails> vinRows = new ArrayList<MNAOModelYearViewContentDetails>();
			for(ContentDetails cd : documentsList)
			{
				List<com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails> rows = cd.getApplicableDisplayOrderList();
				if(null==rows || rows.isEmpty())
				{
					continue;
				}
				if(null==cd.getImDocumentId())
				{
					for(com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d : rows)
					{
						d.setProcessingStatus("FAILURE");
						d.setErrorCode("DSP002");
						d.setErrorMessage("NO ACTIVE DOCUMENT FOUND IN THE DATABASE FOR THE DISPLAY ORDER ROW.");
						processed.add(d);
					}
					continue;
				}
				retrieveESICategoryLabels(cd, esiCategoryList);
				cd.setDocumentTypeRefKey(documentTypeRefKey);
				cd.setDocumentTypeName(documentTypeLabel);
				written.add(cd);
				List<String> docModelYears = modelYears.get(cd.getImDocumentId().trim().toLowerCase());
				for(com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d : rows)
				{
					d.setDocumentId(cd.getImDocumentId());
					if(null!=docModelYears)
					{
						for(String name : docModelYears)
						{
							MNAOModelYearViewContentDetails v = viewContentRow(cd, vcLocale, documentTypeRefKey, documentTypeLabel, d);
							int sep = name.indexOf("::");
							v.setModel((sep!=-1 ? name.substring(0, sep) : name).trim().replace("-", ""));
							v.setYear(sep!=-1 ? name.substring(sep+2).trim() : null);
							myRows.add(v);
						}
					}
					if(null!=cd.getApplicableVINList())
					{
						for(Object o : cd.getApplicableVINList())
						{
							com.mazda.gms3.dmt.mc.vo.VinDetails vin = (com.mazda.gms3.dmt.mc.vo.VinDetails)o;
							// only the vin.txt lines that name THIS row's display order file
							if(null!=vin.getDisplayOrderTextFileName() && null!=d.getDisplayOrderSourceFileName()
									&& d.getDisplayOrderSourceFileName().trim().toLowerCase().equals(vin.getDisplayOrderTextFileName().trim().toLowerCase()))
							{
								MNAOModelYearViewContentDetails v = viewContentRow(cd, vcLocale, documentTypeRefKey, documentTypeLabel, d);
								v.setModel(null==vin.getModelName() ? null : vin.getModelName().trim().replace("-", ""));
								v.setYear(null==vin.getModelYear() ? null : vin.getModelYear().replaceAll("(?i)my", "").trim());
								v.setWmiCode(vin.getWmiCode());
								v.setVdsCode(vin.getVdsCode());
								v.setVisStartRange(vin.getVisStartRange());
								v.setVisEndRange(vin.getVisEndRange());
								vinRows.add(v);
							}
						}
					}
					if(null!=masterVINList)
					{
						for(VINEntFileDetails vin : masterVINList)
						{
							// 17 SEPT 2026: the en_CA year range rows only serve the model year categories
							if(null!=vin.getLangCodeForYearRange() && !"".equals(vin.getLangCodeForYearRange())
									&& !vin.getLangCodeForYearRange().trim().toLowerCase().replace("-", "_").equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
							{
								MNAOModelYearViewContentDetails v = viewContentRow(cd, vcLocale, documentTypeRefKey, documentTypeLabel, d);
								// model and year of the master VIN row (getMasterVinDetailsList), as for the vin.txt rows
								v.setModel(null==vin.getModelCode() ? null : vin.getModelCode().trim().replace("-", ""));
								v.setYear(null==vin.getModelYear() ? null : vin.getModelYear().replaceAll("(?i)my", "").trim());
								v.setWmiCode(vin.getVinWMI());
								v.setVdsCode(vin.getVinVDS());
								v.setVisStartRange(vin.getVinStartRange());
								v.setVisEndRange(vin.getVinEndRange());
								vinRows.add(v);
							}
						}
					}
				}
			}

			// WRITE: old rows of the documents (this locale) go, the new ones are inserted - one transaction
			conn.setAutoCommit(false);
			try
			{
				List<String> writtenIds = new ArrayList<String>();
				for(ContentDetails cd : written)
				{
					if(!writtenIds.contains(cd.getImDocumentId().trim()))
					{
						writtenIds.add(cd.getImDocumentId().trim());
					}
				}
				String[][] targets = { {"gms3_vc_model_year_details", "VC_MY_LOCALE", "VC_MY_DOCUMENT_ID"}, {"gms3_vc_vin_details", "VC_VIN_LOCALE", "VC_VIN_DOCUMENT_ID"} };
				for(String[] t : targets)
				{
					for(int from=0; from<writtenIds.size(); from+=chunk)
					{
						List<String> part = writtenIds.subList(from, Math.min(from+chunk, writtenIds.size()));
						pstmt = conn.prepareStatement("DELETE FROM "+t[0]+" WHERE "+t[1]+" = ? AND "+t[2]+" IN ("+marks(part.size())+")");
						int i=1;
						pstmt.setString(i++, vcLocale);
						for(String id : part)
						{
							pstmt.setString(i++, id);
						}
						pstmt.executeUpdate();
						pstmt.close();pstmt=null;
					}
				}
				insertViewContentRows(conn, "gms3_vc_model_year_details", "VC_MY_", myRows, false, draft, chunk);
				insertViewContentRows(conn, "gms3_vc_vin_details", "VC_VIN_", vinRows, true, draft, chunk);
				conn.commit();
				for(ContentDetails cd : written)
				{
					for(com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d : cd.getApplicableDisplayOrderList())
					{
						d.setProcessingStatus("SUCCESS");
						processed.add(d);
					}
				}
				for(MNAOModelYearViewContentDetails v : myRows)
				{
					v.setProcessingStatus("SUCCESS");
				}
				for(MNAOModelYearViewContentDetails v : vinRows)
				{
					v.setProcessingStatus("SUCCESS");
				}
			}
			catch(Exception e)
			{
				try
				{
					conn.rollback();
				}
				catch(SQLException eq)
				{
					Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", eq);
				}
				Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", e);
				for(ContentDetails cd : written)
				{
					for(com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d : cd.getApplicableDisplayOrderList())
					{
						d.setProcessingStatus("FAILURE");
						d.setErrorCode("DSP010");
						d.setErrorMessage("FAILED TO SAVE THE VIEW CONTENT ROWS OF THE DISPLAY ORDER ROW: "+e.getMessage());
						processed.add(d);
					}
				}
				for(MNAOModelYearViewContentDetails v : myRows)
				{
					v.setProcessingStatus("FAILURE");
					v.setErrorCodes(e.getMessage());
				}
				for(MNAOModelYearViewContentDetails v : vinRows)
				{
					v.setProcessingStatus("FAILURE");
					v.setErrorCodes(e.getMessage());
				}
			}
			finally
			{
				conn.setAutoCommit(true);
			}
			// for the reports and the preview
			viewContentModelYearList.addAll(myRows);
			viewContentVINList.addAll(vinRows);
			logger.info("saveDisplayOrderDetails :: documents="+written.size()+" model year rows="+myRows.size()+" VIN rows="+vinRows.size());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", e);
			}
		}
		return processed;
	}

	/** Engine / AT / MT / DM ... pseudo models (overhaul manuals). */
	private static boolean isOverhaulModel(String model)
	{
		if(null==model)
		{
			return false;
		}
		String m = model.trim().toLowerCase();
		String[] keys = {"check.engine.label", "check.engine.mc.label", "check.rq.label", "check.mcm.label", "check.dm.label",
				"check.auto.mission.label", "check.manual.mission.label"};
		for(String k : keys)
		{
			if(m.equals(ApplicationProperties.getProperty(k).trim().toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	/** A view content row of a document for one display order row - the columns both MNAO view content tables share. */
	private static MNAOModelYearViewContentDetails viewContentRow(ContentDetails cd, String vcLocale, String documentTypeRefKey,
			String documentTypeLabel, com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d)
	{
		MNAOModelYearViewContentDetails v = new MNAOModelYearViewContentDetails();
		v.setSourceFilePath(null==cd.getFilePath() ? null : cd.getFilePath().trim());
		v.setLocale(vcLocale);
		v.setDocumentId(cd.getImDocumentId().trim());
		v.setEsiCatLevel1Code(cd.getCategoryCode());
		v.setEsiCatLevel2Code(cd.getSubCategoryCode());
		v.setEsiCatLevel3Code(cd.getSubSubCategoryCode());
		v.setEsiCatLevel1Name(cd.getEsiCategoryLevel1Name());
		v.setEsiCatLevel2Name(cd.getEsiCategoryLevel2Name());
		v.setEsiCatLevel3Name(cd.getEsiCategoryLevel3Name());
		v.setManualType(documentTypeRefKey);
		v.setManualTypeLabel(documentTypeLabel);
		v.setImDocCreateDate(cd.getDocumentCreatedTime());
		v.setImDocModifiedDate(cd.getDocumentModifiedTime());
		v.setTitle(null==cd.getTitle() ? null : cd.getTitle().trim());
		if(null!=v.getTitle())
		{
			v.setDescription(v.getTitle().length()>100 ? v.getTitle().substring(0, 100) : v.getTitle());
		}
		v.setDisplayOrder(d);
		return v;
	}

	/** Batched INSERT of view content rows (INSERT batches are allowed on MC Dev; UPDATE / DELETE batches are not). */
	private static void insertViewContentRows(Connection conn, String table, String p, List<MNAOModelYearViewContentDetails> rows,
			boolean vin, String contentStatus, int chunk) throws SQLException
	{
		if(rows.isEmpty())
		{
			return;
		}
		String sql = "INSERT INTO "+table+" ("+p+"LOCALE,"+p+"MODEL,"+p+"YEAR,"+p+"DOCUMENT_ID,"+p+"SOURCE_NTWRK_LOC,"+p+"DOCUMENT_TYPE,"
				+ p+"ESI_CAT_CODE_1,"+p+"ESI_CAT_NAME_1,"+p+"ESI_CAT_CODE_2,"+p+"ESI_CAT_NAME_2,"+p+"ESI_CAT_CODE_3,"+p+"ESI_CAT_NAME_3,"
				+ p+"DOCUMENT_TITLE,"+p+"DOCUMENT_STATUS,"+p+"CREATED_TMSTP,"+p+"DOCUMENT_SUBTYPE,"+p+"DOCUMENT_TYPE_NAME,"
				+ p+"DOC_CREATE_DATE,"+p+"DOC_LAST_MODIFIED_DATE,"+p+"DESCRIPTION,VC_CONTENT_STATUS,"
				+ p+"DISP_CODE_1,"+p+"DISP_NAME_1,"+p+"DISP_CODE_2,"+p+"DISP_NAME_2,"+p+"DISP_CODE_3,"+p+"DISP_NAME_3,"
				+ p+"DISP_CODE_4,"+p+"DISP_NAME_4,"+p+"DISP_CODE_5,"+p+"DISP_NAME_5,"+p+"DISP_CODE_6,"+p+"DISP_NAME_6,"+p+"DISPORD_SEQ_NO"
				+ (vin ? ","+p+"WMI_CODE,"+p+"VDS_CODE,"+p+"VIS_START_RANGE,"+p+"VIS_END_RANGE" : "")
				+ ") VALUES ("+marks(vin ? 38 : 34)+")";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try
		{
			int pending = 0;
			Timestamp now = new Timestamp(new Date().getTime());
			for(MNAOModelYearViewContentDetails v : rows)
			{
				com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails d = v.getDisplayOrder();
				int i=1;
				pstmt.setString(i++, v.getLocale());
				pstmt.setString(i++, null==v.getModel() ? null : v.getModel().toUpperCase());
				pstmt.setString(i++, v.getYear());
				pstmt.setString(i++, v.getDocumentId());
				pstmt.setString(i++, v.getSourceFilePath());
				pstmt.setString(i++, v.getManualType());
				pstmt.setString(i++, v.getEsiCatLevel1Code());
				pstmt.setString(i++, v.getEsiCatLevel1Name());
				pstmt.setString(i++, v.getEsiCatLevel2Code());
				pstmt.setString(i++, v.getEsiCatLevel2Name());
				pstmt.setString(i++, v.getEsiCatLevel3Code());
				pstmt.setString(i++, v.getEsiCatLevel3Name());
				pstmt.setString(i++, v.getTitle());
				pstmt.setString(i++, "1");
				pstmt.setTimestamp(i++, now);
				pstmt.setString(i++, "");
				pstmt.setString(i++, v.getManualTypeLabel());
				pstmt.setTimestamp(i++, v.getImDocCreateDate());
				pstmt.setTimestamp(i++, v.getImDocModifiedDate());
				pstmt.setString(i++, v.getDescription());
				pstmt.setString(i++, contentStatus);
				// display order: codes are no longer supplied, the names come from the d01.txt row
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel1Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel1Name());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel2Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel2Name());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel3Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel3Name());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel4Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel4Name());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel5Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel5Name());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel6Code());
				pstmt.setString(i++, null==d ? null : d.getDisplayOrderLevel6Name());
				pstmt.setString(i++, null==d ? null : d.getSequenceNo());
				if(vin)
				{
					pstmt.setString(i++, v.getWmiCode());
					pstmt.setString(i++, v.getVdsCode());
					pstmt.setString(i++, v.getVisStartRange());
					pstmt.setString(i++, v.getVisEndRange());
				}
				pstmt.addBatch();
				if(++pending>=chunk)
				{
					pstmt.executeBatch();
					pending = 0;
				}
			}
			if(pending>0)
			{
				pstmt.executeBatch();
			}
		}
		finally
		{
			pstmt.close();
		}
	}

	private static String marks(int n)
	{
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<n;i++)
		{
			sb.append(i>0 ? ",?" : "?");
		}
		return sb.toString();
	}

	private List<ESICategoryDetails> getESICategoryMasterList(String locale, Connection conn) 
	{
		List<ESICategoryDetails> esiCatList = null;
		Statement stmt =null;
		ResultSet rs = null;
		try
		{
			ESICategoryDetails details = null;
			locale = locale.replace("_", "-");
			String getSql="SELECT A.MDM_CAT_NAME_ENG_LANG, A.MDM_SCAT_NAME_ENG_LANG, A.MDM_SSCAT_NAME_ENG_LANG,A.MDM_CAT_CODE,A.MDM_SCAT_CODE,A.MDM_SSCAT_CODE "
					+ " FROM gms3_mdm_category A, gms3_mdm_manual_language B WHERE A.MDM_ML_ID =B.MDM_ML_ID AND " 
					+ " TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"' "
					+ " AND A.MDM_CAT_FLAG NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"')";
			logger.info("getESICategoryMasterList :: getSql :: >" + getSql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(getSql);
			while(rs.next())
			{
				details  =new ESICategoryDetails();
				if(null!=rs.getString("MDM_CAT_CODE") && !"".equals(rs.getString("MDM_CAT_CODE")))
				{
					details.setCategoryLevel1Code(rs.getString("MDM_CAT_CODE"));
				}
				if(null!=rs.getString("MDM_SCAT_CODE") && !"".equals(rs.getString("MDM_SCAT_CODE")))
				{
					details.setCategoryLevel2Code(rs.getString("MDM_SCAT_CODE"));
				}
				if(null!=rs.getString("MDM_SSCAT_CODE") && !"".equals(rs.getString("MDM_SSCAT_CODE")))
				{
					details.setCategoryLevel3Code(rs.getString("MDM_SSCAT_CODE"));
				}
				if(null!=rs.getString("MDM_CAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CAT_NAME_ENG_LANG")))
				{
					details.setCategoryLevel1Name(rs.getString("MDM_CAT_NAME_ENG_LANG"));
				}
				if(null!=rs.getString("MDM_SCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SCAT_NAME_ENG_LANG")))
				{
					details.setCategoryLevel2Name(rs.getString("MDM_SCAT_NAME_ENG_LANG"));
				}
				if(null!=rs.getString("MDM_SSCAT_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_SSCAT_NAME_ENG_LANG")))
				{
					details.setCategoryLevel3Name(rs.getString("MDM_SSCAT_NAME_ENG_LANG"));
				}
				if(null==esiCatList || esiCatList.size()<=0)
				{
					esiCatList = new ArrayList<ESICategoryDetails>();
				}
				esiCatList.add(details);
				details= null;
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			getSql= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getESICategoryMasterList()", e);
		}
		finally
		{
			locale = null;
		}
		return esiCatList;
	}
	
	/**
	 * getModelYearRangesList() results of the running job, by its four parameters: the method is called
	 * for every master VIN row of every document, and the MDM carline rows do not change during a job.
	 * Emptied by clearModelYearRangesCache() when a conversion starts.
	 */
	private static final Map<String, ArrayList<String>> MODEL_YEAR_RANGES_CACHE = new java.util.concurrent.ConcurrentHashMap<String, ArrayList<String>>();

	public static void clearModelYearRangesCache()
	{
		MODEL_YEAR_RANGES_CACHE.clear();
	}

	public  ArrayList<String> getModelYearRangesList(String langCode, String carlineCode, String wmiCode,  String modelName)
	{
		String cacheKey = null;
		if(null!=langCode && null!=carlineCode && null!=wmiCode && null!=modelName)
		{
			cacheKey = (langCode.replace("_", "-")+"|"+carlineCode+"|"+wmiCode+"|"+modelName).trim().toLowerCase();
			ArrayList<String> cached = MODEL_YEAR_RANGES_CACHE.get(cacheKey);
			if(null!=cached)
			{
				return new ArrayList<String>(cached);
			}
		}
		ArrayList<String> rangesList = null;
		PreparedStatement stmt  = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if(null!=langCode && !"".equals(langCode) && null!=carlineCode && !"".equals(carlineCode) && null!=wmiCode && !"".equals(wmiCode) 
					&& null!=modelName && !"".equals(modelName))
			{
				conn = getConnection();
				/*
				 * IDENTIFY COUNTRY CODE FROM LANGUAGE,
				 * LANGUAGE CODE WILL BE AS IT IS
				 */
				rangesList = new ArrayList<String>();
				langCode= langCode.replace("_", "-");
				
				String sql="SELECT MDM_CRLN_YEAR_START,MDM_CRLN_YEAR_END FROM gms3_mdm_carline_codes WHERE TRIM(LOWER(MDM_ML_LANG_CODE))=? AND "
						+ " TRIM(LOWER(MDM_CRLN_CODE))=? AND TRIM(LOWER(MDM_CRLN_WMI_CODE))=? "
						+ " AND TRIM(LOWER(MDM_CRLN_NAME_ENG_LANG))=? AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				logger.info("getModelYearRangesList :: Sql :: > " + sql);
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, langCode.trim().toLowerCase());
				stmt.setString(2, carlineCode.trim().toLowerCase());
				stmt.setString(3, wmiCode.trim().toLowerCase());
				stmt.setString(4, modelName.trim().toLowerCase());
				rs = stmt.executeQuery();
				while(rs.next())
				{
					int startRange = rs.getInt("MDM_CRLN_YEAR_START");
					int endRange = rs.getInt("MDM_CRLN_YEAR_END");
					for(int a=startRange;a<=endRange;a++)
					{
						boolean add = true;
						if(null!=rangesList && rangesList.size()>0)
						{
							for(int b=0;b<rangesList.size();b++)
							{
								if(String.valueOf(a).equals(rangesList.get(b)))
								{
									// already added
									add = false;
									break;
								}
							}
						}
						
						if(add == true)
						{
							rangesList.add(String.valueOf(a));
						}
					}
					
				}
				sql = null;
			}
			else
			{
				logger.info("getModelYearRangesList :: Language Code, Carline Code,  WMI Code & Model Name as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getModelYearRangesList()", e);
		}
		finally
		{

			try {
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=conn)
					conn.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(MNAODocumentManagementDAO.class.getName(), "getModelYearRangesList()", e);
			}
			stmt = null;
			rs = null;
			conn = null;
			langCode = null;
			carlineCode=null;
		}
		if(null!=cacheKey && null!=rangesList)
		{
			MODEL_YEAR_RANGES_CACHE.put(cacheKey, new ArrayList<String>(rangesList));
		}
		return rangesList;
	}
	
	private ContentDetails retrieveESICategoryLabels(ContentDetails contentDetails, List<ESICategoryDetails> esiCategoryList)
	{
		// SET ESI CAT FLAG TO Y -  DEFAULT VALUE
		ESICategoryDetails details = null;
		// CHECK FOR LEVEL 1
		if(null!=contentDetails.getCategoryCode() && !"".equals(contentDetails.getCategoryCode()))
		{
			if(null!=esiCategoryList && esiCategoryList.size()>0)
			{
				details = null;
				for(int a=0;a<esiCategoryList.size();a++)
				{
					details = (ESICategoryDetails)esiCategoryList.get(a);
					if(null!=details.getCategoryLevel1Code() && !"".equals(details.getCategoryLevel1Code()) 
							&& details.getCategoryLevel1Code().trim().toLowerCase().equals(contentDetails.getCategoryCode().trim().toLowerCase()))
					{
						contentDetails.setEsiCategoryLevel1Name(details.getCategoryLevel1Name());
						break;
					}
					details = null;
				}
			}

			// CHECK FOR LEVEL 2
			if(null!=contentDetails.getSubCategoryCode() && !"".equals(contentDetails.getSubCategoryCode()))
			{
				if(null!=esiCategoryList && esiCategoryList.size()>0)
				{
					details = null;
					for(int at=0;at<esiCategoryList.size();at++)
					{
						details = (ESICategoryDetails)esiCategoryList.get(at);
						if(null!=details.getCategoryLevel1Code() && !"".equals(details.getCategoryLevel1Code()) && 
								null!=details.getCategoryLevel2Code() && !"".equals(details.getCategoryLevel2Code()) 
								&& details.getCategoryLevel1Code().trim().toLowerCase().equals(contentDetails.getCategoryCode().trim().toLowerCase()) 
								&& details.getCategoryLevel2Code().trim().toLowerCase().equals(contentDetails.getSubCategoryCode().trim().toLowerCase()))
						{
							contentDetails.setEsiCategoryLevel2Name(details.getCategoryLevel2Name());
							break;
						}
						details = null;
					}
				}

				// CHECK FOR LEVEL 3
				if(null!=contentDetails.getSubSubCategoryCode() && !"".equals(contentDetails.getSubSubCategoryCode()))
				{
					if(null!=esiCategoryList && esiCategoryList.size()>0)
					{
						details = null;
						for(int aq=0;aq<esiCategoryList.size();aq++)
						{
							details = (ESICategoryDetails)esiCategoryList.get(aq);
							if(null!=details.getCategoryLevel1Code() && !"".equals(details.getCategoryLevel1Code()) && 
									null!=details.getCategoryLevel2Code() && !"".equals(details.getCategoryLevel2Code()) 
									&& null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code())
									&& details.getCategoryLevel1Code().trim().toLowerCase().equals(contentDetails.getCategoryCode().trim().toLowerCase()) 
									&& details.getCategoryLevel2Code().trim().toLowerCase().equals(contentDetails.getSubCategoryCode().trim().toLowerCase()) 
									&& details.getCategoryLevel3Code().trim().toLowerCase().equals(contentDetails.getSubSubCategoryCode().trim().toLowerCase()))
							{
								contentDetails.setEsiCategoryLevel3Name(details.getCategoryLevel3Name());
								break;
							}
							details = null;
						}
					}
				}
			}
		}
		return contentDetails;
	}
	
	private void cleanUpOldDataForDocument(Connection conn, ContentDetails contentDetails,String tableName,String catTableName,String innerLinkTableName,
			String vinMasterTableName, String navTableName,String cvcTableName,String vinTableName, String vinAttrTableName, boolean saveOperation, String documentId) throws Exception 
	{
		Statement stmt=null;
		logger.info("cleanUpOldDataForDocument :: Proceed for Cleaning Up Document Data for Document Id :: >"+ documentId);
		/*
		 * DELETE THE PREVIOUS DOCUMENT ID FROM ALL TABLES.
		 */
		if(saveOperation==true)
		{
			logger.info("cleanUpOldDataForDocument :: Proceed for Cleaning Up Document Data from IM DOC Table (Save) Operation :: >"+ documentId);
			String deleteDocSql="DELETE FROM "+tableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
			stmt = conn.createStatement();
			stmt.executeUpdate(deleteDocSql);
			stmt.close();
			stmt = null;
			deleteDocSql= null;
		}
		
		String deleteSql="DELETE FROM "+ catTableName +" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM "+ innerLinkTableName +" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM "+ vinMasterTableName +" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM "+ navTableName +" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM "+ cvcTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM "+vinTableName+" WHERE DC_IM_DOC_ID='"+documentId+"' ";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();stmt=null;
		deleteSql=null;
		
		deleteSql="DELETE FROM "+ vinAttrTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		String locale = contentDetails.getLocale();
		locale= locale.replace("-", "_");
		deleteSql="DELETE FROM gms3_vc_model_year_details WHERE VC_MY_DOCUMENT_ID='"+documentId.trim()+"' "
				+ " AND VC_MY_LOCALE='"+locale.trim()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		deleteSql="DELETE FROM gms3_vc_vin_details WHERE VC_VIN_DOCUMENT_ID='"+documentId.trim()+"' "
				+ " AND VC_VIN_LOCALE='"+locale.trim()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		locale= null;
		
		logger.info("cleanUpOldDataForDocument :: ------------------------------------------------------------");
	}
	
}