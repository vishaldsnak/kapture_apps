package com.mazda.gms3.dmt.mc.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.MCViewContentDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.CategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;

public class MCDocumentManagementDAO  extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(MCDocumentManagementDAO.class);
	
	private ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetails = new ArrayList<Map<Object, Object>>();
	
	private ArrayList<MCViewContentDetails> viewContentDataList = new ArrayList<MCViewContentDetails>();

	private ArrayList<Map<Object, Object>> failedDatabaseSaveDocumentDetailsForDelete = new ArrayList<Map<Object, Object>>();
	
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
	
	public ArrayList<MCViewContentDetails> getViewContentDataList() {
		return viewContentDataList;
	}

	public void setViewContentDataList(
			ArrayList<MCViewContentDetails> viewContentDataList) {
		this.viewContentDataList = viewContentDataList;
	}

	/**
	 * Every ACTIVE document the database holds for one material folder.
	 *
	 * @param materialFolderKey the start of DC_SOURCE_NETWORK_LOC for the folder, ending with a
	 *        backslash: locale\modelFolder\manualType\faceLift\material\
	 * @return one entry per document: { DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC }
	 *
	 * LOCATE is used rather than LIKE on purpose: folder names contain underscores, and in a
	 * LIKE pattern an underscore matches ANY character.
	 */
	public List<String[]> getActiveDocumentsForMaterialFolder(String materialFolderKey, String modelType) throws SQLException
	{
		List<String[]> documents = new ArrayList<String[]>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=materialFolderKey && !"".equals(materialFolderKey) && null!=modelType && !"".equals(modelType))
			{
				String tableName="";
				if(modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
				}
				conn = getConnection();
				pstmt = conn.prepareStatement("SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC FROM "+tableName
						+" WHERE DC_DOC_STATUS = ? AND DC_IM_DOC_ID IS NOT NULL AND DC_SOURCE_NETWORK_LOC IS NOT NULL"
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
	public ContentDetails getDocumentDetails(String filePath, String modelType) throws SQLException 
	{
		ContentDetails contentDetails=  new ContentDetails();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=filePath && !"".equals(filePath) && null!=modelType && !"".equals(modelType))
			{
				conn = getConnection();
				
				String tableName="";
				if(modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
				}
				
//				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE "
//						+ " '"+filePath.trim().toLowerCase()+"%' "
//								+ " AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"' " ;
				
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
				logger.info("getDocumentDetails :: File Path/Model Type as Parameter is null, Return null.");
				contentDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getDocumentDetails()", e);
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

	public  ContentDetails getDocumentDetailsWD(String filePath, String modelType) throws SQLException 
	{
		ContentDetails contentDetails=  new ContentDetails();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=filePath && !"".equals(filePath) && null!=modelType && !"".equals(modelType))
			{
				conn = getConnection();
				
				String tableName="";
				if(modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
				}
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
				
//				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE '"+filePath.trim().toLowerCase()+"%' OR TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE '"+anotherFilePath.trim().toLowerCase()+"%' "
//								+ " AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"' " ;
				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = '"+filePath.trim().toLowerCase()+"' "
						+ " OR TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) = '"+anotherFilePath.trim().toLowerCase()+"' " ;
				logger.info("getDocumentDetailsWD :: Sql :: > " + sql);
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
				logger.info("getDocumentDetailsWD :: File Path as Parameter is null, Return null.");
				contentDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getDocumentDetailsWD()", e);
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
	
	public String getDocumentDetailsForInnerLink(String filePath, String modelType) throws SQLException 
	{
		String imDocumentId=null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs  = null;
		try
		{
			if(null!=filePath && !"".equals(filePath) && null!=modelType && !"".equals(modelType))
			{
				conn = getConnection();
				
				String tableName="";
				if(modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
				}
				
				String sql="SELECT * FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE "
						+ " '"+filePath.trim().toLowerCase()+"%' AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"' " ;
				logger.info("getDocumentDetailsForInnerLink :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					imDocumentId =rs.getString("DC_IM_DOC_ID");
				}
				sql = null;
				stmt.close();stmt = null;
				rs.close();rs=null;
			}
			else
			{
				logger.info("getDocumentDetailsForInnerLink :: File Path/Model Type as Parameter is null, Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getDocumentDetailsForInnerLink()", e);
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
		return imDocumentId;
	}

	public boolean updateInnerLinkMappingStatus(List<ContentDetails> documentsList) 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		try
		{
			if(null!=documentsList && documentsList.size()>0)
			{
				/*
				 * MAKE A PARTITION OF 100 AND START SAVING IN A BATCH
				 */
				String tableName="";
				ContentDetails contentDetails = (ContentDetails)documentsList.get(0);
				if(null!=contentDetails)
				{
					if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().
							equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
					{
						// NEW MODELS TABLE
						tableName = "gms3_dmt_mc_newm_imdoc";
					}
					else
					{
						// NORMAL MODELS TABLE
						tableName = "gms3_dmt_mc_imdoc";
					}
				}
				contentDetails = null;
				
				// NO BATCHED UPDATE: MySQL Router (connection sharing) on MC Dev refuses a batch sent as a
				// multi-statement. ONE statement per db.write.batch.size documents, the three values picked
				// per document with CASE (same as MMEDocumentManagementDAO).
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
					for(int b=0;b<subList.size();b++)
					{
						found.append(" WHEN ? THEN ?");
						mapped.append(" WHEN ? THEN ?");
						reason.append(" WHEN ? THEN ?");
						in.append(b>0 ? ",?" : "?");
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
				
				tableName = null;
				contentDetails = null;
				
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", e1);
			}	
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!=conn)
				{
					conn.close();conn = null;
				}
				if(null!=pstmt)
				{
					pstmt.close();pstmt=null;
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "updateInnerLinkMappingStatus()", e1);
			}	
		}
		return true;
	}
	
	/**
	 * Function will Perform Save Operation for a Document
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public boolean saveDocumentDetails(ContentDetails contentDetails) throws SQLException 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				String tableName="";
				String innerLinkTableName="";
				String esiCatTableName="";
				String vinTableName="";
				String categoryTableName="";
				String masterVINTableName="";
				String cvcTableName="";
				if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					innerLinkTableName="gms3_dmt_mc_newm_inrlks";
					esiCatTableName ="gms3_dmt_mc_newm_esicat";
					vinTableName="gms3_dmt_mc_newm_vin";
					categoryTableName="gms3_dmt_mc_newm_cat";
					masterVINTableName="gms3_dmt_mc_newm_vinmaster";
					cvcTableName="gms3_dmt_mc_newm_cvc";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					innerLinkTableName="gms3_dmt_mc_inrlks";
					esiCatTableName="gms3_dmt_mc_esicat";
					vinTableName="gms3_dmt_mc_vin";
					categoryTableName="gms3_dmt_mc_cat";
					masterVINTableName="gms3_dmt_mc_vinmaster";
					cvcTableName="gms3_dmt_mc_cvc";
				}
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
								
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
					
					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
						+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_PUBLISHED_STATUS=?,DC_MARKET_NAME=?,"
						+ "DC_LOCALE_NAME=?,DC_MODEL_NAME=?,DC_MODEL_TYPE=?,DC_CARLINE_CODE=?,DC_MODEL_FOLDER_NAME=?,"
						+ "DC_MANUAL_TYPE_FOLDER_NAME=?,DC_FACELIFT_FOLDER_NAME=?,DC_MATERIAL_FOLDER_NAME=?,DC_SOURCE_FILE_NAME=?,"
						+ "	DC_SOURCE_NETWORK_LOC=?,DC_DOC_STATUS=?,DC_ESI_CATEGORY_MAPPED=?,DC_ESI_CATEGORY_MAPPED_LEVEL=?,"
						+ "	DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_ESI_MAPPED_FILE_NAME=?,DC_VIN_MAPPED=?,DC_VIN_MAPPED_FILE_NAME=?"
						+ ",DC_DISPLAY_ORDER_MAPPED=?,DC_DISPORD_MAPPED_FILE_NAME=?,DC_IM_DOC_TITLE=?, DC_INRLNK_FOUND=?,"
						+ " DC_INRLNK_ALL_MAPPED=?, DC_INRLNK_FAILURE_REASON=?, DC_CREATED_TMSTP=? WHERE DC_DD_ID=?";
					
					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getChannelName());
					pstmt.setString(2, contentDetails.getImContentType());
					pstmt.setString(3, contentDetails.getImVersion());
					pstmt.setString(4, contentDetails.getImResourcePath());
					pstmt.setString(5, contentDetails.getImDocStatus());
					pstmt.setString(6, contentDetails.getMarket());
					pstmt.setString(7, contentDetails.getLocale());
					pstmt.setString(8, contentDetails.getModel());
					pstmt.setString(9, contentDetails.getModelType());
					pstmt.setString(10, contentDetails.getCarlineCode());
					pstmt.setString(11, contentDetails.getModelFolderName());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getFaceLiftFolderName());
					pstmt.setString(14, contentDetails.getMaterialName());
					pstmt.setString(15, contentDetails.getFileName());
					pstmt.setString(16, contentDetails.getFilePath());
					pstmt.setString(17, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setString(18, contentDetails.getEsiCategoryMapped());
					pstmt.setString(19, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(20, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedFileName());
					pstmt.setString(22, contentDetails.getVinMapped());
					pstmt.setString(23, contentDetails.getVinMappedFileName());
					pstmt.setString(24, contentDetails.getDisplayOrderMapped());
					pstmt.setString(25, contentDetails.getDisplayOrderMappedFileName());
					pstmt.setString(26, contentDetails.getTitle());
					pstmt.setString(27, contentDetails.getInnerLinkFound());
					pstmt.setString(28, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(29, contentDetails.getInnerLinkMappingReason());
					pstmt.setTimestamp(30, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(31, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;
					
					/*
					 * INSERT INNER LINK DETAILS
					 */
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						processInnerLinksInformation(contentDetails, conn, innerLinkTableName);
					}
					
					
					/*
					 * INSERT ESI CATEGORY DETAILS 
					 */
					processESICategoryInformation(contentDetails, conn, esiCatTableName);
					
					/*
					 * INSERT DOC CATEGORY DETAILS
					 */
					if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
					{
						processCategoryInformation(contentDetails, conn, categoryTableName);
					}
					
					/*
					 * INSERT INTO VIN DETAILS
					 */
					if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
					{
						processVINInformation(contentDetails, conn, vinTableName);
					}
					
					/*
					 * INSERT INTO MASTER VIN DETAILS
					 */
					if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
					{
						processMasterVINInformation(contentDetails, conn, masterVINTableName);
					}
					
					/*
					 * INSERT INTO DISPLAY ORDER DETAILS
					 * COMMENT IT, AS DISPLAY ORDER PROCESING IS SEPARATE NOW.
					 */
//					if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
//					{
//						processDisplayOrderInformation(contentDetails, conn, displayOrderTableName);
//					}
					
					/*
					 * INSERT INTO CVC CATEGORY DETAILS
					 */
					if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
					{
						processCVCCategoryInformation(contentDetails, conn, cvcTableName);
					}
					
					/*
					 * INSERT INTO VIEW COTNENT DATA
					 */
//					processViewContentData(contentDetails, tableName, conn);
				}
				// commit the transaction
				conn.commit();
				
				tableName=null;
				categoryTableName=null;
				esiCatTableName=null;
				vinTableName=null;
				innerLinkTableName=null;
				masterVINTableName= null;
				cvcTableName=null;
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
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDocumentDetails()", e);
			
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
		return true;
		
	}

	/**
	 * Function will perform Update Operation for a Document
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public boolean updateDocumentDetails(ContentDetails contentDetails,boolean performAllOps) throws SQLException 
	{
		Connection conn = null;
		PreparedStatement pstmt=  null;
		Statement stmt=  null;
		ResultSet rs=  null;
		try
		{
			if(null!=contentDetails.getImDocumentId() && !"".equals(contentDetails.getImDocumentId()))
			{
				String tableName="";
				String innerLinkTableName="";
				String esiCatTableName="";
				String vinTableName="";
				String categoryTableName="";
				String masterVINTableName="";
				String cvcTableName="";
				String displayOrderTableName="";
				String cdProcessingTableName="";
				if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					innerLinkTableName="gms3_dmt_mc_newm_inrlks";
					esiCatTableName ="gms3_dmt_mc_newm_esicat";
					vinTableName="gms3_dmt_mc_newm_vin";
					categoryTableName="gms3_dmt_mc_newm_cat";
					masterVINTableName="gms3_dmt_mc_newm_vinmaster";
					cvcTableName="gms3_dmt_mc_newm_cvc";
					displayOrderTableName="gms3_dmt_mc_newm_dispord";
					cdProcessingTableName="gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					innerLinkTableName="gms3_dmt_mc_inrlks";
					esiCatTableName="gms3_dmt_mc_esicat";
					vinTableName="gms3_dmt_mc_vin";
					categoryTableName="gms3_dmt_mc_cat";
					masterVINTableName="gms3_dmt_mc_vinmaster";
					cvcTableName="gms3_dmt_mc_cvc";
					displayOrderTableName="gms3_dmt_mc_dispord";
					cdProcessingTableName="gms3_dmt_mc_cd_data";
				}
				
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
				
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
						cleanUpOldDataForDocument(conn, contentDetails, tableName, innerLinkTableName, categoryTableName, 
								esiCatTableName, vinTableName, masterVINTableName, displayOrderTableName, cvcTableName, cdProcessingTableName, false, contentDetails.getImDocumentId());
					}
					
					/*
					 * Update other Details in gms3_dmt_conv_imdoc
					 */

					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
							+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_PUBLISHED_STATUS=?,DC_MARKET_NAME=?,"
							+ "DC_LOCALE_NAME=?,DC_MODEL_NAME=?,DC_MODEL_TYPE=?,DC_CARLINE_CODE=?,DC_MODEL_FOLDER_NAME=?,"
							+ "DC_MANUAL_TYPE_FOLDER_NAME=?,DC_FACELIFT_FOLDER_NAME=?,DC_MATERIAL_FOLDER_NAME=?,DC_SOURCE_FILE_NAME=?,"
							+ "	DC_SOURCE_NETWORK_LOC=?,DC_DOC_STATUS=?,DC_ESI_CATEGORY_MAPPED=?,DC_ESI_CATEGORY_MAPPED_LEVEL=?,"
							+ "	DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_ESI_MAPPED_FILE_NAME=?,DC_VIN_MAPPED=?,DC_VIN_MAPPED_FILE_NAME=?"
							+ ",DC_DISPLAY_ORDER_MAPPED=?,DC_DISPORD_MAPPED_FILE_NAME=?,DC_IM_DOC_TITLE=?,DC_INRLNK_FOUND=?,"
							+ " DC_INRLNK_ALL_MAPPED = ?, DC_INRLNK_FAILURE_REASON=?, DC_UPDATED_TMSTP=? WHERE DC_DD_ID=?";

					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getChannelName());
					pstmt.setString(2, contentDetails.getImContentType());
					pstmt.setString(3, contentDetails.getImVersion());
					pstmt.setString(4, contentDetails.getImResourcePath());
					pstmt.setString(5, contentDetails.getImDocStatus());
					pstmt.setString(6, contentDetails.getMarket());
					pstmt.setString(7, contentDetails.getLocale());
					pstmt.setString(8, contentDetails.getModel());
					pstmt.setString(9, contentDetails.getModelType());
					pstmt.setString(10, contentDetails.getCarlineCode());
					pstmt.setString(11, contentDetails.getModelFolderName());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getFaceLiftFolderName());
					pstmt.setString(14, contentDetails.getMaterialName());
					pstmt.setString(15, contentDetails.getFileName());
					pstmt.setString(16, contentDetails.getFilePath());
					pstmt.setString(17, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setString(18, contentDetails.getEsiCategoryMapped());
					pstmt.setString(19, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(20, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedFileName());
					pstmt.setString(22, contentDetails.getVinMapped());
					pstmt.setString(23, contentDetails.getVinMappedFileName());
					pstmt.setString(24, contentDetails.getDisplayOrderMapped());
					pstmt.setString(25, contentDetails.getDisplayOrderMappedFileName());
					pstmt.setString(26, contentDetails.getTitle());
					pstmt.setString(27, contentDetails.getInnerLinkFound());
					pstmt.setString(28, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(29, contentDetails.getInnerLinkMappingReason());
					pstmt.setTimestamp(30, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(31, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;

					/*
					 * DELETE AND THEN
					 * INSERT INNER LINK DETAILS
					 */
					processInnerLinksInformation(contentDetails, conn, innerLinkTableName);

					if(performAllOps==true)
					{
						/*
						 * DELRTE AND THEN
						 * INSERT ESI CATEGORY DETAILS 
						 */
						processESICategoryInformation(contentDetails, conn, esiCatTableName);

						/*
						 * DELETE AND THEN
						 * INSERT DOC CATEGORY DETAILS
						 */
						processCategoryInformation(contentDetails, conn, categoryTableName);


						/*
						 * DELETE AND THEN
						 * INSERT INTO VIN DETAILS
						 */
						processVINInformation(contentDetails, conn, vinTableName);


						/*
						 * DELETE AND THEN
						 * INSERT INTO MASTER VIN DETAILS
						 */
						processMasterVINInformation(contentDetails, conn, masterVINTableName);

						/*
						 * DELETE AND THEN
						 * INSERT INTO DISPLAY ORDER DETAILS
						 * COMMENT IT, AS DISPLAY ORDER PROCESING IS SEPARATE NOW.
						 */
						//	processDisplayOrderInformation(contentDetails, conn, displayOrderTableName);

						/*
						 * DELETE AND THEN
						 * INSERT INTO CVC CATEGORY DETAILS
						 */
						processCVCCategoryInformation(contentDetails, conn, cvcTableName);

						/*
						 * INSERT INTO VIEW COTNENT DATA
						 */
						//	processViewContentData(contentDetails, tableName, conn);
					}
				}
				// commit the transaction
				conn.commit();
				
				tableName=null;
				categoryTableName=null;
				esiCatTableName=null;
				vinTableName=null;
				innerLinkTableName=null;
				masterVINTableName =null;
				cvcTableName = null;
			}
			else
			{
				logger.info("updateDocumentDetails :: IM DOCUMENT IS NULL IN OBJECT. Return false");
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
				errorMap.put("OPERATION_TYPE", "UPDATE");
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
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "updateDocumentDetails()", e);
			
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
			errorMap.put("OPERATION_TYPE", "UPDATE");
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
				String innerLinkTableName="";
				String esiCatTableName="";
				String vinTableName="";
				String displayOrderTableName="";
				String categoryTableName="";
				String masterVINTableName="";
				String cvcTableName="";
				String cdProcessingTableName="";
				if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					innerLinkTableName="gms3_dmt_mc_newm_inrlks";
					esiCatTableName ="gms3_dmt_mc_newm_esicat";
					vinTableName="gms3_dmt_mc_newm_vin";
					displayOrderTableName="gms3_dmt_mc_newm_dispord";
					categoryTableName="gms3_dmt_mc_newm_cat";
					masterVINTableName="gms3_dmt_mc_newm_vinmaster";
					cvcTableName="gms3_dmt_mc_newm_cvc";
					cdProcessingTableName="gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					innerLinkTableName="gms3_dmt_mc_inrlks";
					esiCatTableName="gms3_dmt_mc_esicat";
					vinTableName="gms3_dmt_mc_vin";
					displayOrderTableName="gms3_dmt_mc_dispord";
					categoryTableName="gms3_dmt_mc_cat";
					masterVINTableName="gms3_dmt_mc_vinmaster";
					cvcTableName="gms3_dmt_mc_cvc";
					cdProcessingTableName="gms3_dmt_mc_cd_data";
				}
				
				// INRLKS
				String deleteSql="DELETE FROM "+ innerLinkTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// ESI CAT
				deleteSql="DELETE FROM "+ esiCatTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// VIN
				deleteSql="DELETE FROM "+ vinTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// DISP ORD
				deleteSql="DELETE FROM "+ displayOrderTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// CAT
				deleteSql="DELETE FROM "+ categoryTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// MASTER VIN
				deleteSql="DELETE FROM "+ masterVINTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// CVC
				deleteSql="DELETE FROM "+ cvcTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// CD DATA
				deleteSql="DELETE FROM "+ cdProcessingTableName +" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				
				// VIEW CONTENT
				String locale = contentDetails.getLocale();
				locale = locale.replace("-", "_");
				deleteSql="DELETE FROM gms3_vc_japan_vin_details WHERE VC_VIN_DOCUMENT_ID='"+contentDetails.getImDocumentId().trim()+"' "
						+ " AND VC_VIN_LOCALE='"+locale.trim()+"'";
				stmt = conn.createStatement();
				stmt.executeUpdate(deleteSql);
				stmt.close();
				stmt=  null;
				deleteSql = null;
				locale=  null;

			
				
				/*
				 * Update other Details in gms3_dmt_conv_imdoc
				 */
				String updateDocDetailsSql="UPDATE "+tableName+" SET DC_IM_DOC_CONTENT_ID=?,"
						+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_PUBLISHED_STATUS=?, DC_DOC_STATUS=?, "
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
				innerLinkTableName = null;
				vinTableName = null;
				masterVINTableName = null;
				categoryTableName=  null;
				cvcTableName= null;
				displayOrderTableName=  null;
				esiCatTableName= null;
				cdProcessingTableName = null;
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
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "deleteDocumentDetails()", e);
			
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
	 * Function will Delete the Old Document Reference for the Source Location 
	 * and will Create New Document for the same Source Location as Master Identifier.
	 * @param contentDetails
	 * @return
	 * @throws SQLException
	 */
	public boolean saveDocumentDetailsWhileUpdate(ContentDetails contentDetails) throws SQLException 
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
				String tableName="";
				String innerLinkTableName="";
				String esiCatTableName="";
				String vinTableName="";
				String displayOrderTableName="";
				String categoryTableName="";
				String masterVINTableName="";
				String cvcTableName="";
				String cdProcessingTableName="";
				if(null!=contentDetails.getModelType() && contentDetails.getModelType().trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					innerLinkTableName="gms3_dmt_mc_newm_inrlks";
					esiCatTableName ="gms3_dmt_mc_newm_esicat";
					vinTableName="gms3_dmt_mc_newm_vin";
					displayOrderTableName="gms3_dmt_mc_newm_dispord";
					categoryTableName="gms3_dmt_mc_newm_cat";
					masterVINTableName="gms3_dmt_mc_newm_vinmaster";
					cvcTableName="gms3_dmt_mc_newm_cvc";
					cdProcessingTableName="gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					innerLinkTableName="gms3_dmt_mc_inrlks";
					esiCatTableName="gms3_dmt_mc_esicat";
					vinTableName="gms3_dmt_mc_vin";
					displayOrderTableName="gms3_dmt_mc_dispord";
					categoryTableName="gms3_dmt_mc_cat";
					masterVINTableName="gms3_dmt_mc_vinmaster";
					cvcTableName="gms3_dmt_mc_cvc";
					cdProcessingTableName="gms3_dmt_mc_cd_data";
				}
				conn = getConnection();
				conn.setAutoCommit(false);
				Long autoDocumentId=null;
				/*
				 * DELETE THE PREVIOUS DOCUMENT ID FROM ALL TABLES.
				 */
				cleanUpOldDataForDocument(conn, contentDetails, tableName, innerLinkTableName, categoryTableName, 
						esiCatTableName, vinTableName, masterVINTableName, displayOrderTableName, cvcTableName, cdProcessingTableName, true, contentDetails.getPreviousDocumentId());
				
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
					
					String updateDocDetailsSql="UPDATE "+tableName+" SET DC_IM_CHANNEL_NAME=?,DC_IM_DOC_CONTENT_ID=?,"
						+ "DC_IM_DOC_VERSION=?,DC_IM_DOC_RESOURCE_PATH=?,DC_IM_DOC_PUBLISHED_STATUS=?,DC_MARKET_NAME=?,"
						+ "DC_LOCALE_NAME=?,DC_MODEL_NAME=?,DC_MODEL_TYPE=?,DC_CARLINE_CODE=?,DC_MODEL_FOLDER_NAME=?,"
						+ "DC_MANUAL_TYPE_FOLDER_NAME=?,DC_FACELIFT_FOLDER_NAME=?,DC_MATERIAL_FOLDER_NAME=?,DC_SOURCE_FILE_NAME=?,"
						+ "	DC_SOURCE_NETWORK_LOC=?,DC_DOC_STATUS=?,DC_ESI_CATEGORY_MAPPED=?,DC_ESI_CATEGORY_MAPPED_LEVEL=?,"
						+ "	DC_ESI_CATEGORY_MAPPED_REFKEY=?,DC_ESI_MAPPED_FILE_NAME=?,DC_VIN_MAPPED=?,DC_VIN_MAPPED_FILE_NAME=?"
						+ ",DC_DISPLAY_ORDER_MAPPED=?,DC_DISPORD_MAPPED_FILE_NAME=?,DC_IM_DOC_TITLE=?,DC_INRLNK_FOUND=?,"
						+ " DC_INRLNK_ALL_MAPPED = ?, DC_INRLNK_FAILURE_REASON=?, DC_CREATED_TMSTP=? WHERE DC_DD_ID=?";
					
					pstmt = conn.prepareStatement(updateDocDetailsSql);
					pstmt.setString(1, contentDetails.getChannelName());
					pstmt.setString(2, contentDetails.getImContentType());
					pstmt.setString(3, contentDetails.getImVersion());
					pstmt.setString(4, contentDetails.getImResourcePath());
					pstmt.setString(5, contentDetails.getImDocStatus());
					pstmt.setString(6, contentDetails.getMarket());
					pstmt.setString(7, contentDetails.getLocale());
					pstmt.setString(8, contentDetails.getModel());
					pstmt.setString(9, contentDetails.getModelType());
					pstmt.setString(10, contentDetails.getCarlineCode());
					pstmt.setString(11, contentDetails.getModelFolderName());
					pstmt.setString(12, contentDetails.getManualType());
					pstmt.setString(13, contentDetails.getFaceLiftFolderName());
					pstmt.setString(14, contentDetails.getMaterialName());
					pstmt.setString(15, contentDetails.getFileName());
					pstmt.setString(16, contentDetails.getFilePath());
					pstmt.setString(17, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setString(18, contentDetails.getEsiCategoryMapped());
					pstmt.setString(19, contentDetails.getEsiCategoryMappedLevel());
					pstmt.setString(20, contentDetails.getEsiCategoryMappedRefKey());
					pstmt.setString(21, contentDetails.getEsiCategoryMappedFileName());
					pstmt.setString(22, contentDetails.getVinMapped());
					pstmt.setString(23, contentDetails.getVinMappedFileName());
					pstmt.setString(24, contentDetails.getDisplayOrderMapped());
					pstmt.setString(25, contentDetails.getDisplayOrderMappedFileName());
					pstmt.setString(26, contentDetails.getTitle());
					pstmt.setString(27, contentDetails.getInnerLinkFound());
					pstmt.setString(28, contentDetails.getAllInnerLinksMapped());
					pstmt.setString(29, contentDetails.getInnerLinkMappingReason());
					pstmt.setTimestamp(30, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(31, autoDocumentId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					updateDocDetailsSql = null;
					
					/*
					 * INSERT INNER LINK DETAILS
					 */
					if(null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						processInnerLinksInformation(contentDetails, conn, innerLinkTableName);
					}
					
					
					/*
					 * INSERT ESI CATEGORY DETAILS 
					 */
					processESICategoryInformation(contentDetails, conn, esiCatTableName);
					
					/*
					 * INSERT DOC CATEGORY DETAILS
					 */
					if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
					{
						processCategoryInformation(contentDetails, conn, categoryTableName);
					}
					
					/*
					 * INSERT INTO VIN DETAILS
					 */
					if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
					{
						processVINInformation(contentDetails, conn, vinTableName);
					}
					
					/*
					 * INSERT INTO MASTER VIN DETAILS
					 */
					if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
					{
						processMasterVINInformation(contentDetails, conn, masterVINTableName);
					}
					
					/*
					 * INSERT INTO DISPLAY ORDER DETAILS
					 * COMMENT IT AS DISPLAY ORDER PROCESSING IS SEPARATE NOW.
					 */
//					if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
//					{
//						processDisplayOrderInformation(contentDetails, conn, displayOrderTableName);
//					}
					
					/*
					 * INSERT INTO CVC CATEGORY DETAILS
					 */
					if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
					{
						processCVCCategoryInformation(contentDetails, conn, cvcTableName);
					}
					
					/*
					 * INSERT INTO VIEW COTNENT DATA
					 */
//					processViewContentData(contentDetails, tableName, conn);
				}
				// commit the transaction
				conn.commit();
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
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDocumentDetailsWhileUpdate()", e);
			
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

	public List<DisplayOrderDetails> saveDisplayOrderDetails(ArrayList<ContentDetails> documentsList, String modelType, String pathToBeReplaced, 
			String channelFolderName, String locale,String model, String manualType)  
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		List<DisplayOrderDetails> finalDisplayOrderList = null;
		try
		{
			DisplayOrderDetails details = new  DisplayOrderDetails();
			if(null!=modelType && !"".equals(modelType) && null!=documentsList && documentsList.size()>0 && null!=locale && !"".equals(locale))
			{
				String tableName="";
				String categoryTableName = "";
				if(null!=modelType && modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					categoryTableName = "gms3_dmt_mc_newm_esicat";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					categoryTableName = "gms3_dmt_mc_esicat";
				}

				/*
				 * INSERT INTO DISPLAY ORDER DETAILS
				 */
				// getConnectionObject
				conn = getConnection(); 

				/*
				 * IDENITFY DOCUMENT TYPE LABEL & DOCUMENT TYPE REF KEY FOR ALL PROCESSING DOCS
				 * AS THEY BLONG TO SAME MODEL & MANUAL TYPE
				 */
				String documentTypeRefKey=null;
				String documentTypeName=null;
				String engingBookCode=null;
				String engineBookName=null;
				String missionBookCode=null;
				String missionBookName=null;
				boolean isEngMissionFolder= false;
				List<VINEntFileDetails> masterVINList = null;
				/*
				 * IDENITFY DOCUMENT MANUAL TYPE REF KEY & LABEL FOR ALL PRPCESSING DOCUMENTS
				 */
				String getDocumentTypeSql="";
				if(null!=manualType && !"".equals(manualType))
				{
					if(null!=channelFolderName && channelFolderName.equals(ApplicationProperties.getProperty("wiring.diagram.folder.label")))
					{
						// WD DOCS
						documentTypeRefKey=ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toUpperCase().replace(" ", "_");
						getDocumentTypeSql="SELECT DC_MANUAL_NAME FROM gms3_dmt_conv_manual_type WHERE "
								+ " TRIM(LOWER(DC_MANUAL_CODE)) = '"+manualType.trim().toLowerCase()+"' AND "
								+ " TRIM(LOWER(MDM_ML_LANG_CODE))='"+locale.replace("_", "-").trim().toLowerCase()+"' AND "
								+ " DC_MANUAL_FLAG NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"') ";
					}
					else
					{
						// NORMAL MANUAL TYPES
						// identify CATEGORY REF KEY
						if(null!=model && 
								(model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) ||  
										model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
										model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) ||  
										model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) ||  
										model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) ||  
										model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
										|| model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
						{
							// processing manual type is either enging or mission
							isEngMissionFolder = true;
							// get on ModelName
							documentTypeRefKey = ConversionUtils.identifyManualTypeAsCateogry(model);
							/*
							 * THESE ARE ENGINE OR MISSION DOCS - GET ENGING BOOK & MISSION BOOK CODES / NAMES ON THE MANUAL TYPE FOLDER NAME
							 * THESE CODES ARE APPLICABLE ONLY FOR OVERHUAL MANUALS
							 */
							String sql="";
							if(manualType.trim().toLowerCase().startsWith("eb") || manualType.trim().toLowerCase().startsWith("rq") || 
									manualType.trim().toLowerCase().startsWith("mc") || manualType.trim().toLowerCase().startsWith("dc"))
							{
								// GET ENGINE BOOK NAME
								sql="SELECT B.MDM_EB_NAME_REGIONAL_LANG AS BOOK_NAME  FROM gms3_mdm_engine_book B, gms3_mdm_manual_language C "
										+ " WHERE TRIM(LOWER(B.MDM_EB_CODE))='"+manualType.trim().toLowerCase()+"' AND "
										+ " B.MDM_ML_ID=C.MDM_ML_ID AND TRIM(LOWER(C.MDM_ML_LANG_CODE))='"+locale.replace("_", "-").trim().toLowerCase()+"'";
							}
							else if(manualType.trim().toLowerCase().startsWith("mb"))
							{
								// GET MISSION BOOK NAME
								sql="SELECT B.MDM_TRANSBK_NAME_REGIONAL_LANG AS BOOK_NAME  FROM gms3_mdm_trans_book B, gms3_mdm_manual_language C "
										+ " WHERE TRIM(LOWER(B.MDM_TRANSBK_CODE))='"+manualType.trim().toLowerCase()+"' AND "
										+ " B.MDM_ML_ID=C.MDM_ML_ID AND TRIM(LOWER(C.MDM_ML_LANG_CODE))='"+locale.replace("_", "-").trim().toLowerCase()+"'";
							}
							if(null!=sql && !"".equals(sql))
							{
								logger.info("saveDisplayOrderDetails :: get Engine / Misson Book Qurey for OverHual Manuals :: >"+ sql);
								stmt = conn.createStatement();
								rs = stmt.executeQuery(sql);
								if(rs.next())
								{
									if(manualType.trim().toLowerCase().startsWith("eb") || manualType.trim().toLowerCase().startsWith("rq") || 
											manualType.trim().toLowerCase().startsWith("mc") || manualType.trim().toLowerCase().startsWith("dc"))
									{
										engingBookCode = manualType.trim().toUpperCase();
										engineBookName = rs.getString("BOOK_NAME");
									}
									else if(manualType.trim().toLowerCase().startsWith("mb"))
									{
										missionBookCode = manualType.trim().toUpperCase();
										missionBookName = rs.getString("BOOK_NAME");
									}
								}
								rs.close();rs=null;
								stmt.close();stmt = null;
								sql = null;
							}

							/*
							 * GET MASTER VIN LIST FOR THE COMPLETE OVERHUAL MANUALS DOCS
							 */
							masterVINList = getMasterVinDetailsList(locale, manualType, model, conn, null);
						}
						else
						{
							// on Manual Type
							documentTypeRefKey=ConversionUtils.identifyManualTypeAsCateogry(manualType);
						}

						if(null!=documentTypeRefKey && !"".equals(documentTypeRefKey))
						{
							// get MANUAL TYPE NAME FOR SPECIFIC LOCALE FROM IM DB ON THE BASIS OF REF KEY

							getDocumentTypeSql="SELECT DC_MANUAL_NAME FROM gms3_dmt_conv_manual_type WHERE "
									+ " TRIM(LOWER(DC_MANUAL_REF_KEY)) = '"+documentTypeRefKey.trim().toLowerCase()+"' AND "
									+ " TRIM(LOWER(MDM_ML_LANG_CODE))='"+locale.replace("_", "-").trim().toLowerCase()+"' AND "
									+ " DC_MANUAL_FLAG NOT IN ('"+ApplicationProperties.getProperty("flag.value.draft")+"','"+ApplicationProperties.getProperty("flag.value.delete")+"') ";

						}
					}
				}

				if(null!=getDocumentTypeSql && !"".equals(getDocumentTypeSql))
				{
					logger.info("saveDisplayOrderDetails :: getDocumentTypeSql :: >" + getDocumentTypeSql);
					stmt= conn.createStatement();
					rs = stmt.executeQuery(getDocumentTypeSql);
					if(rs.next())
					{
						documentTypeName = rs.getString("DC_MANUAL_NAME");
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
				}
				getDocumentTypeSql = null;

				/*
				 * ADD DOCUMENT TYPE REF KEY & DOCUMENT TYPE NAME IN DOCUMENTS LIST
				 */
				ContentDetails contentDetails = null;
				for(int a=0;a<documentsList.size();a++)
				{
					contentDetails = (ContentDetails)documentsList.get(a);
					contentDetails.setDocumentTypeRefKey(documentTypeRefKey);
					contentDetails.setDocumentTypeName(documentTypeName);
					contentDetails = null;
				}
				contentDetails  =null;

				/*
				 * FETCH MASTER DATA OF ESI LABELS FOR EN_UK LOCALE
				 * 	CHECK IF PROCESSING LOCALE IS NOT EN_UK LOCALE, THEN FETCH DATA FOR OTHER LOCALE AS WELL
				 */
				List<ESICategoryDetails> esiCatList = null;
				esiCatList = getESICategoryMasterList(locale, conn);
				/*
				 * GET DOCUMENT IDS FOR EACH, DIVIDE INTO SUBSETS AND GET DATA
				 */
				int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.lookup.batch.size"));
				List<List<ContentDetails>> partitions = new ArrayList<List<ContentDetails>>();
				// create Partitions of Unique Items Data and then fetch FK Codes / Data
				for (int i=0; i<documentsList.size(); i += partitionSize) {
					partitions.add(documentsList.subList(i, Math.min(i + partitionSize, documentsList.size())));
				}
				String keyToCheck = null;
				String getDocumentIdSql=null;
				String documentId=null;
				String sourcePath  =null;
				if(null!=partitions && partitions.size()>0)
				{
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
					for(List<ContentDetails> subList: partitions)
					{
						keyToCheck = "";
						if(null!=subList && subList.size()>0)
						{
							for(int a=0;a<subList.size();a++)
							{
								contentDetails = (ContentDetails)subList.get(a);
								keyToCheck+="'"+contentDetails.getFilePath()+"'";
								if(a!=subList.size()-1)
								{
									keyToCheck+=",";
								}
								contentDetails = null;
							}


							// get DocumentId, Source Location, Doc Creation Time & Update Time
							getDocumentIdSql = " SELECT DC_IM_DOC_ID, DC_SOURCE_NETWORK_LOC, DATE_FORMAT(DC_CREATED_TMSTP,'%d-%m-%Y') AS DC_CREATED_TMSTP, "
									+ " DATE_FORMAT(DC_UPDATED_TMSTP,'%d-%m-%Y') AS DC_UPDATED_TMSTP  FROM "+ tableName+" WHERE "
									+ "	TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN ("+keyToCheck.trim().toLowerCase()+") "
									+ " AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
							logger.info("saveDisplayOrderDetails :: getDocumentIdSql  :: " + getDocumentIdSql);
							stmt = conn.createStatement();
							rs = stmt.executeQuery(getDocumentIdSql);
							contentDetails = null;
							while(rs.next())
							{
								documentId =rs.getString("DC_IM_DOC_ID");
								sourcePath = rs.getString("DC_SOURCE_NETWORK_LOC");
								if(null!=documentId && !"".equals(documentId) && null!=sourcePath && !"".equals(sourcePath))
								{
									contentDetails = null;
									for(int b=0;b<documentsList.size();b++)
									{
										contentDetails = (ContentDetails)documentsList.get(b);
										if(contentDetails.getFilePath().trim().toLowerCase().equals(sourcePath.trim().toLowerCase()))
										{
											// SET CREATEDTIME & MODIFIED TIME
											if(null!=rs.getString("DC_CREATED_TMSTP") && !"".equals(rs.getString("DC_CREATED_TMSTP")))
											{
												try
												{
													contentDetails.setDocumentCreatedTime(new Timestamp(sdf.parse(rs.getString("DC_CREATED_TMSTP")).getTime()));
												}
												catch(Exception e) {}
											}
											if(null!=rs.getString("DC_UPDATED_TMSTP") && !"".equals(rs.getString("DC_UPDATED_TMSTP")))
											{
												try
												{
													contentDetails.setDocumentModifiedTime(new Timestamp(sdf.parse(rs.getString("DC_UPDATED_TMSTP")).getTime()));
												}
												catch(Exception e) {}
											}
											// set documentId
											contentDetails.setDocumentId(documentId);
										}
										contentDetails = null;
									}
								}
								documentId = null;
								sourcePath = null;
							}
							rs.close();rs=null;
							stmt.close();stmt = null;
							getDocumentIdSql = null;
							keyToCheck =  null;
						}
					}
					sdf = null;
				}
				partitions = null;
				contentDetails = null;
				keyToCheck = null;

				/*
				 * ITERATE DOCUMENTS LIST AND START
				 * PROCESSING DISPLAY ORFDER DATA + VIEW CONTENT DATA
				 * 
				 * RE-DIVIDE DOCUMENT ID LIST IN SUBSET OF 300 & START PROCESSING
				 */
				partitions = new ArrayList<List<ContentDetails>>();
				// create Partitions of Unique Items Data and then fetch FK Codes / Data
				for (int i=0; i<documentsList.size(); i += partitionSize) {
					partitions.add(documentsList.subList(i, Math.min(i + partitionSize, documentsList.size())));
				}

				VINEntFileDetails vinDetails = null;
				MCViewContentDetails carDetails = null;
				VinDetails appVinDetails = null;
				details = null;
				int listCount=0;
				List<MCViewContentDetails> viewContentList = null;
				List<String> uniqueEngTypeList = null;
				List<String> uniqueMissionTypeList = null;
				boolean addToUniqueEngList = true;
				boolean addToUniqueMissionList = true;
				if(null!=partitions && partitions.size()>0)
				{
					contentDetails = null;
					for(List<ContentDetails> subList : partitions)
					{
						listCount++;
						logger.info("saveDisplayOrderDetails :: Start Processing Subset "+ listCount+ " /"+ partitions.size());
						contentDetails = null;
						viewContentList  =null;
						uniqueEngTypeList = null;
						uniqueMissionTypeList = null;
						if(null!=subList && subList.size()>0)
						{
							keyToCheck  = "";
							/*
							 * GET ESI CATEGORY CODES FOR EACH SUBLIST
							 */
							contentDetails = null;
							for(int a=0;a<subList.size();a++)
							{
								contentDetails = (ContentDetails)subList.get(a);
								if(null!=contentDetails.getDocumentId() && !"".equals(contentDetails.getDocumentId()))
								{
									keyToCheck+="'"+contentDetails.getDocumentId()+"',";
								}
							}

							/*
							 * GET DOCUMENTS ESI CATEGORY CODES & NAMES
							 */
							if(null!=keyToCheck && !"".equals(keyToCheck))
							{
								if(keyToCheck.endsWith(","))
								{
									keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
								}
								getDocumentIdSql="SELECT * FROM "+ categoryTableName +" WHERE DC_IM_DOC_ID IN ("+keyToCheck+")";
								logger.info("saveDisplayOrderDetails :: getESICategoriesForDocument  :: " + getDocumentIdSql);
								stmt = conn.createStatement();
								rs = stmt.executeQuery(getDocumentIdSql);
								while(rs.next())
								{
									if(null!=rs.getString("DC_IM_DOC_ID") && !"".equals(rs.getString("DC_IM_DOC_ID")))
									{
										// iterate documentsList and update categoryCodes
										for(int q=0;q<subList.size();q++)
										{
											contentDetails = (ContentDetails)subList.get(q);
											if(null!=contentDetails.getDocumentId() && rs.getString("DC_IM_DOC_ID").trim().toLowerCase().equals(contentDetails.getDocumentId().trim().toLowerCase())) 
											{
												contentDetails.setCategoryCode(rs.getString("DC_ESICAT_CODE_LEVEL_1"));
												contentDetails.setSubCategoryCode(rs.getString("DC_ESICAT_CODE_LEVEL_2"));
												contentDetails.setSubSubCategoryCode(rs.getString("DC_ESICAT_CODE_LEVEL_3"));

												/*
												 * SET ESI LABELS AS WELL
												 * 	CHECK IF LOCALE IS EN_UK = THEN CHECK ONLY IN EN_UK_ESI_LIST
												 * 	ELSE
												 * 		CHECK IN SPECIFIC LOCALE LIST
												 * 		IF NOT FOUND THEN CHECK IN EN_UK_ESI_LIST
												 */
												contentDetails = retrieveESICategoryLabels(contentDetails, esiCatList);
											}
											contentDetails = null;
										}
									}
								}
								rs.close();rs=null;
								stmt.close();stmt=null;
								getDocumentIdSql = null;
							}
							contentDetails = null;

							for(int a=0;a<subList.size();a++)
							{
								contentDetails = (ContentDetails)subList.get(a);
								if(null!=contentDetails.getDocumentId() && !"".equals(contentDetails.getDocumentId()))
								{
									if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
									{
										// iterate List & check if displayOrderSourceFileName is Not Null - add to processingList
										details = null;
										for(int e=0;e<contentDetails.getApplicableDisplayOrderList().size();e++)
										{
											details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(e);
											// set documentId
											details.setDocumentId(contentDetails.getDocumentId());
											if(null!=details.getDisplayOrderSourceFileName() && !"".equals(details.getDisplayOrderSourceFileName()))
											{
												/*
												 * NOW CHECK IF THE ENGINE / MISSION PROCESSING FOLDER
												 */
												if(isEngMissionFolder==true)
												{
													if(null!=masterVINList && masterVINList.size()>0)
													{
														vinDetails = new  VINEntFileDetails();
														for(int b=0;b<masterVINList.size();b++)
														{
															vinDetails = (VINEntFileDetails)masterVINList.get(b);
															/*
															 * IDENTIFY ENG NAME AND REGIONAL NAME ON THE BASIS OF CARLINE CODE + WMI CODE + VDS CODE
															 * MODEL TYPE WILL BE READ FROM CONTENT DETAILS
															 * 
															 * ENG NAME & REGIONAL NAME ALREADY AVAILABLE, WHEN VIEW CONTENT DATA GETS PROCESSED WHILE
															 * PROCESING DOCUMENT, BUT WHEN PROCSSED FROM DISPLAY ORDER PROCESSING
															 * DMT_VIN_MASTER TABLE DOES NOT HAVE THAT INFORMATION, SO IT NEEDS TO BE PULLED HERE.
															 */
															carDetails = new MCViewContentDetails();
															carDetails.setLocale(locale);
															carDetails.setCarlineCode(vinDetails.getVinCarline());
															carDetails.setWmiCode(vinDetails.getVinWMI());
															carDetails.setVdsCode(vinDetails.getVinVDS());
															carDetails.setVisStartRange(vinDetails.getVinStartRange());
															carDetails.setVisEndRange(vinDetails.getVinEndRange());
															carDetails.setCarlineNameEng(vinDetails.getCarlineEngName());
															carDetails.setCarlineNameReg(vinDetails.getCarlineRegName());

															/*
															 * Call Function to Identify Engine  / Mission Book Code and Name
															 */
															carDetails.setDocumentId(contentDetails.getDocumentId());
															// SET ENGINE / MISSION BOOK DETAILS
															carDetails.setEngineBookCode(engingBookCode);
															carDetails.setEngineBookName(engineBookName);
															carDetails.setMissionBookCode(missionBookCode);
															carDetails.setMissionBookName(missionBookName);
															// PREPARE DATA
															carDetails.setSourceFilePath(contentDetails.getFilePath());
															carDetails.setEsiCatLevel1Code(contentDetails.getCategoryCode());
															carDetails.setEsiCatLevel2Code(contentDetails.getSubCategoryCode());
															carDetails.setEsiCatLevel3Code(contentDetails.getSubSubCategoryCode());

															// SET ESICATEGORY NAMES
															carDetails.setEsiCatLevel1Name(contentDetails.getEsiCategoryLevel1Name());
															carDetails.setEsiCatLevel2Name(contentDetails.getEsiCategoryLevel2Name());
															carDetails.setEsiCatLevel3Name(contentDetails.getEsiCategoryLevel3Name());

															carDetails.setEsiCatFlag(contentDetails.getEsiCatFlag());
															carDetails.setModelType(modelType);

															// SET DOCUMENT TYPE DETAILS
															carDetails.setManualType(documentTypeRefKey);
															carDetails.setManualTypeLabel(documentTypeName);

															// SET DOCUMENT CREATED AND MODIFIED TIMESTAMPS
															carDetails.setImDocCreateDate(contentDetails.getDocumentCreatedTime());
															carDetails.setImDocModifiedDate(contentDetails.getDocumentModifiedTime());

															// SET DISPLAY ORDER DETAILS
															carDetails.setDisplayOrderCodeLevel1(details.getDisplayOrderLevel1Code());
															carDetails.setDisplayOrderCodeLevel2(details.getDisplayOrderLevel2Code());
															carDetails.setDisplayOrderCodeLevel3(details.getDisplayOrderLevel3Code());
															carDetails.setDisplayOrderCodeLevel4(details.getDisplayOrderLevel4Code());
															carDetails.setDisplayOrderCodeLevel5(details.getDisplayOrderLevel5Code());
															carDetails.setDisplayOrderCodeLevel6(details.getDisplayOrderLevel6Code());

															carDetails.setDisplayOrderNameLevel1(details.getDisplayOrderLevel1Name());
															carDetails.setDisplayOrderNameLevel2(details.getDisplayOrderLevel2Name());
															carDetails.setDisplayOrderNameLevel3(details.getDisplayOrderLevel3Name());
															carDetails.setDisplayOrderNameLevel4(details.getDisplayOrderLevel4Name());
															carDetails.setDisplayOrderNameLevel5(details.getDisplayOrderLevel5Name());
															carDetails.setDisplayOrderNameLevel6(details.getDisplayOrderLevel6Name());

															carDetails.setDisplayOrderSequenceNo(details.getSequenceNo());

															// set Title
															carDetails.setTitle(details.getTitle());

															// add details to finalViewContentList
															if(null==viewContentList || viewContentList.size()<=0)
															{
																viewContentList = new ArrayList<MCViewContentDetails>();
															}
															viewContentList.add(carDetails);
															carDetails=  null;
														}
														vinDetails=null;
													}
												}
												else
												{
													if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
													{
														appVinDetails = new VinDetails();
														for(int r=0;r<contentDetails.getApplicableVINList().size();r++)
														{
															appVinDetails = (VinDetails)contentDetails.getApplicableVINList().get(r);
															if(null!=appVinDetails.getDisplayOrderTextFileName() && details.getDisplayOrderSourceFileName().trim().toLowerCase().equals(appVinDetails.getDisplayOrderTextFileName().trim().toLowerCase()))
															{
																/*
																 * IDENTIFY ENG NAME AND REGIONAL NAME ON THE BASIS OF CARLINE CODE + WMI CODE + VDS CODE + VIS START + VIS END
																 * MODEL TYPE WILL BE READ FROM CONTENT DETAILS
																 */
																carDetails = new MCViewContentDetails();
																carDetails.setLocale(locale);
																carDetails.setCarlineCode(appVinDetails.getCarlineCode());
																carDetails.setWmiCode(appVinDetails.getWmiCode());
																carDetails.setVdsCode(appVinDetails.getVdsCode());
																carDetails.setVisStartRange(appVinDetails.getVisStartRange());
																carDetails.setVisEndRange(appVinDetails.getVisEndRange());
																carDetails.setCarlineNameEng(appVinDetails.getCarlineNameEng());
																carDetails.setCarlineNameReg(appVinDetails.getCarlineNameReg());
																
																carDetails.setSourceFilePath(contentDetails.getFilePath());
																carDetails.setLocale(locale);
																carDetails.setDocumentId(contentDetails.getDocumentId());
																carDetails.setEsiCatLevel1Code(contentDetails.getCategoryCode());
																carDetails.setEsiCatLevel2Code(contentDetails.getSubCategoryCode());
																carDetails.setEsiCatLevel3Code(contentDetails.getSubSubCategoryCode());

																// SET ESICATEGORY NAMES
																carDetails.setEsiCatLevel1Name(contentDetails.getEsiCategoryLevel1Name());
																carDetails.setEsiCatLevel2Name(contentDetails.getEsiCategoryLevel2Name());
																carDetails.setEsiCatLevel3Name(contentDetails.getEsiCategoryLevel3Name());

																carDetails.setEsiCatFlag(contentDetails.getEsiCatFlag());

																carDetails.setModelType(modelType);

																// SET DOCUMENT TYPE DETAILS
																carDetails.setManualType(documentTypeRefKey);
																carDetails.setManualTypeLabel(documentTypeName);

																// SET DOCUMENT CREATED AND MODIFIED TIMESTAMPS
																carDetails.setImDocCreateDate(contentDetails.getDocumentCreatedTime());
																carDetails.setImDocModifiedDate(contentDetails.getDocumentModifiedTime());

																// SET DISPLAY ORDER DETAILS
																carDetails.setDisplayOrderCodeLevel1(details.getDisplayOrderLevel1Code());
																carDetails.setDisplayOrderCodeLevel2(details.getDisplayOrderLevel2Code());
																carDetails.setDisplayOrderCodeLevel3(details.getDisplayOrderLevel3Code());
																carDetails.setDisplayOrderCodeLevel4(details.getDisplayOrderLevel4Code());
																carDetails.setDisplayOrderCodeLevel5(details.getDisplayOrderLevel5Code());
																carDetails.setDisplayOrderCodeLevel6(details.getDisplayOrderLevel6Code());

																carDetails.setDisplayOrderNameLevel1(details.getDisplayOrderLevel1Name());
																carDetails.setDisplayOrderNameLevel2(details.getDisplayOrderLevel2Name());
																carDetails.setDisplayOrderNameLevel3(details.getDisplayOrderLevel3Name());
																carDetails.setDisplayOrderNameLevel4(details.getDisplayOrderLevel4Name());
																carDetails.setDisplayOrderNameLevel5(details.getDisplayOrderLevel5Name());
																carDetails.setDisplayOrderNameLevel6(details.getDisplayOrderLevel6Name());

																carDetails.setDisplayOrderSequenceNo(details.getSequenceNo());

																/*
																 * identify Engine Book & Type for the mapped Engine & Mission Type
																 */
																if(null!=details.getEngineType() && !"".equals(details.getEngineType()))
																{
																	carDetails.setEngineType(details.getEngineType());
																	addToUniqueEngList = true;
																	if(null!=uniqueEngTypeList && uniqueEngTypeList.size()>0)
																	{
																		for(int d=0;d<uniqueEngTypeList.size();d++)
																		{
																			if(String.valueOf(uniqueEngTypeList.get(d)).equals(details.getEngineType()))
																			{
																				addToUniqueEngList = false;
																				break;
																			}
																		}
																	}
																	if(addToUniqueEngList == true)
																	{
																		if(null==uniqueEngTypeList || uniqueEngTypeList.size()<=0)
																		{
																			uniqueEngTypeList = new ArrayList<String>();
																		}
																		uniqueEngTypeList.add(details.getEngineType());
																	}
																}
																if(null!=details.getMissionType() && !"".equals(details.getMissionType()))
																{
																	carDetails.setMissionType(details.getMissionType());
																	addToUniqueMissionList = true;
																	if(null!=uniqueMissionTypeList && uniqueMissionTypeList.size()>0)
																	{
																		for(int d=0;d<uniqueMissionTypeList.size();d++)
																		{
																			if(String.valueOf(uniqueMissionTypeList.get(d)).equals(details.getMissionType()))
																			{
																				addToUniqueMissionList = false;
																				break;
																			}
																		}
																	}
																	if(addToUniqueMissionList == true)
																	{
																		if(null==uniqueMissionTypeList || uniqueMissionTypeList.size()<=0)
																		{
																			uniqueMissionTypeList = new ArrayList<String>();
																		}
																		uniqueMissionTypeList.add(details.getMissionType());
																	}
																}

																// set Title
																carDetails.setTitle(details.getTitle());

																// add details to finalViewContentList
																if(null==viewContentList || viewContentList.size()<=0)
																{
																	viewContentList = new ArrayList<MCViewContentDetails>();
																}
																viewContentList.add(carDetails);
																carDetails = null;
															}
															appVinDetails = null;
														}
													}
												}

												// SET PROCESSING STATUS AS SUCCESS 
												details.setProcessingStatus("SUCCESS");
												if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
												{
													finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
												}
												finalDisplayOrderList.add(details);
												details = null;
											}
											else
											{
												// add this with processing status as Failure and do not process
												details.setProcessingStatus("FAILURE");
												details.setErrorCode("DISP003");
												details.setErrorMessage("DISPLAY ORDER TEXT FILE NAME IS NULL.");
												if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
												{
													finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
												}
												finalDisplayOrderList.add(details);
											}
											details= null;
										}
									}
								}
								else
								{
									// FAILED TO IDENTIFY DOCUMENT IDS
									if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
									{
										details = null;
										for(int c=0;c<contentDetails.getApplicableDisplayOrderList().size();c++)
										{
											details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(c);
											details.setProcessingStatus("FAILURE");
											details.setErrorCode("DISP003");
											details.setErrorMessage("FAILED TO IDETNFIY IM DOCUMENT ID.");
											if(null==finalDisplayOrderList || finalDisplayOrderList.size()<=0)
											{
												finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
											}
											finalDisplayOrderList.add(details);
											details = null;
										}
										details = null;
									}
								}
								contentDetails  =null;
							}


							/*
							 * IF VIEW CONTENT LIST IS NOT NULL FOR THIS SUB SET
							 * START SAVING VC DETAILS FOR IT.
							 */
							if(null!=viewContentList  && viewContentList.size()>0)
							{
								/*
								 * CHECK IF UNIQUE ENGINELIST & UNIQUE MISSION LIST ARE NOT NULL
								 * FETCH ENG BOOKS & MISSION BOOKS DATA FOR THEM
								 */
								if(null!=uniqueEngTypeList && uniqueEngTypeList.size()>0)
								{
									List<SelectItemDetails> itemsList = getEngineMissionBookDetails(uniqueEngTypeList, conn, "ENG");
									String[] token=null;
									if(null!=itemsList && itemsList.size()>0)
									{
										SelectItemDetails si  =null;
										// UPDATE THE ENG / MISSION BOOK VALUES IN VIEW CONTENT LIST
										carDetails = null;
										for(int c=0;c<viewContentList.size();c++)
										{
											carDetails = (MCViewContentDetails)viewContentList.get(c);
											if(null!=carDetails.getEngineType() && !"".equals(carDetails.getEngineType()))
											{
												token = carDetails.getEngineType().split(",");
												if(null!=token && token.length>0)
												{
													si = null;
													for(int d=0;d<itemsList.size();d++)
													{
														si = (SelectItemDetails)itemsList.get(d);
														if(si.getCode().trim().toLowerCase().equals(token[0].trim().toLowerCase()))
														{
															// set eng book code & name
															carDetails.setEngineBookCode(si.getLabel());
															carDetails.setEngineBookName(si.getValue());
															break;
														}
														si = null;
													}
												}
											}
											carDetails=  null;
										}
										si = null;
									}
									token = null;
									itemsList = null;
								}

								if(null!=uniqueMissionTypeList && uniqueMissionTypeList.size()>0)
								{
									List<SelectItemDetails> itemsList = getEngineMissionBookDetails(uniqueMissionTypeList, conn, "MISSION");
									String[] token=null;
									if(null!=itemsList && itemsList.size()>0)
									{
										SelectItemDetails si  =null;
										// UPDATE THE ENG / MISSION BOOK VALUES IN VIEW CONTENT LIST
										carDetails = null;
										for(int c=0;c<viewContentList.size();c++)
										{
											carDetails = (MCViewContentDetails)viewContentList.get(c);
											if(null!=carDetails.getMissionType() && !"".equals(carDetails.getMissionType()))
											{
												token = carDetails.getMissionType().split(",");
												if(null!=token && token.length>0)
												{
													si = null;
													for(int d=0;d<itemsList.size();d++)
													{
														si = (SelectItemDetails)itemsList.get(d);
														if(si.getCode().trim().toLowerCase().equals(token[0].trim().toLowerCase()))
														{
															// set mission book code & name
															carDetails.setMissionBookCode(si.getLabel());
															carDetails.setMissionBookName(si.getValue());
															break;
														}
														si = null;
													}
												}
											}
											carDetails=  null;
										}
										si = null;
									}
									token = null;
									itemsList = null;
								}

								/*
								 * PROCEED FOR SAVING VIEW CONTENT DETAILS IN DATABASE
								 */
								try
								{
									conn.setAutoCommit(false);
									processViewContentData(viewContentList, conn, locale, subList);
									conn.commit();
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", e);
									try
									{
										conn.rollback();
									}
									catch(SQLException eq)
									{
										Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", eq);
									}

									/*
									 * SINCE COMPLETE BATCH FAILED, ITERATE SUBSET LIST AND FOR ALL DOCUMENTS SET THE SAME ERROR MESSAGE IN
									 * DISPLAY ORDER LIST WHERE PROCESSING STATUS IS SUCCESS
									 */
									Writer writer = new StringWriter();
									PrintWriter print = new PrintWriter(writer);
									e.printStackTrace(print);
									// SET FAILURE AND REASON FOR FAILURE

									contentDetails = null;
									details = null;
									for(int a=0;a<subList.size();a++)
									{
										contentDetails = (ContentDetails)subList.get(a);
										if(null!=contentDetails.getDocumentId() && !"".equals(contentDetails.getDocumentId()) && null!=finalDisplayOrderList && finalDisplayOrderList.size()>0)
										{
											details = null;
											for(int b=0;b<finalDisplayOrderList.size();b++)
											{
												details = (DisplayOrderDetails)finalDisplayOrderList.get(b);
												if(null!=details.getDocumentId() && details.getDocumentId().equals(contentDetails.getDocumentContent()))
												{
													if(details.getProcessingStatus()==null || (null!=details.getProcessingStatus() && details.getProcessingStatus().equals("SUCCESS")))
													{
														// SET PROCESSING STATUS AS FAILURE
														details.setProcessingStatus("FAILURE");
														details.setErrorCode(e.getMessage());
														details.setErrorMessage(writer.toString());
													}
												}
												details = null;
											}
										}	
										contentDetails = null;
									}
									details = null;
									contentDetails = null;
									// set writer & print to null
									writer = null;
									// set print to null
									print = null;
								}
							}
							uniqueEngTypeList = null;
							uniqueMissionTypeList = null;
							viewContentList = null;
						}
						subList = null;
						logger.info("saveDisplayOrderDetails :: Ends Processing Subset "+ listCount+ " /"+ partitions.size());
					}
				}
				partitions = null;
			}
			else
			{
				logger.info("saveDisplayOrderDetails :: IM DOCUMENT LIST OR MODEL TYPE IS NULL. Return false");
				// ITERATE DOCUMENT LIST AND UPDATE PROCESSING STATUS AS NULL FOR ALL ROWS IN THE LIST
				ContentDetails contentDetails = null;
				details = null;
				if(null!=documentsList && documentsList.size()>0)
				{
					for(int a=0;a<documentsList.size();a++)
					{
						contentDetails = (ContentDetails)documentsList.get(a);
						if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
						{
							details = null;
							for(int c=0;c<contentDetails.getApplicableDisplayOrderList().size();c++)
							{
								details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(c);
								details.setProcessingStatus("FAILURE");
								details.setErrorCode("DISP001");
								details.setErrorMessage("MODEL TYPE AS PARAMETER WAS NULL. FAILED TO IDENTIFY DISPLAY ORDER PROCESSING TABLE NAME.");
								if(null==finalDisplayOrderList || finalDisplayOrderList.size()<0)
								{
									finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
								}
								finalDisplayOrderList.add(details);
								details = null;
							}
						}
						contentDetails = null;
					}
				}
			}
			details= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", e);
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			// SET FAILURE AND REASON FOR FAILURE

			ContentDetails contentDetails = null;
			DisplayOrderDetails details = null;
			if(null!=documentsList && documentsList.size()>0)
			{
				for(int a=0;a<documentsList.size();a++)
				{
					contentDetails = (ContentDetails)documentsList.get(a);
					if(null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0)
					{
						details = null;
						for(int c=0;c<contentDetails.getApplicableDisplayOrderList().size();c++)
						{
							details = (DisplayOrderDetails)contentDetails.getApplicableDisplayOrderList().get(c);
							details.setProcessingStatus("FAILURE");
							details.setErrorCode(e.getMessage());
							details.setErrorMessage(writer.toString());
							if(null==finalDisplayOrderList || finalDisplayOrderList.size()<0)
							{
								finalDisplayOrderList = new ArrayList<DisplayOrderDetails>();
							}
							finalDisplayOrderList.add(details);
							details = null;
						}
					}
					contentDetails = null;
				}
			}
			details = null;
			contentDetails = null;
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
			}
			catch(Exception eq)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveDisplayOrderDetails()", eq);
			}
		}
		return finalDisplayOrderList;
	}

	public ArrayList<CDProcessingDetails> saveCDProcessingDetails_backup(ArrayList<CDProcessingDetails> cdProcessingList, String modelType, String pathToBeReplaced, String channelFolderName,String locale)  
	{
		Connection conn = null;
		try
		{
			CDProcessingDetails details = new CDProcessingDetails();
			if(null!=modelType && !"".equals(modelType) && null!=cdProcessingList && cdProcessingList.size()>0)
			{
				locale = locale.replace("-", "_");
				String tableName="";
				String cdProcessingTableName="";
				if(null!=modelType && modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					cdProcessingTableName="gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					cdProcessingTableName="gms3_dmt_mc_cd_data";
				}

				int failureCount=0;
				conn = getConnection();
				/*
				 * BEFORE PROCESSING FIND ALL THE INVALID LINES AND UPDATE THEIR PROCESSING STATUS
				 */
				details = null;
				ArrayList<CDProcessingDetails> processingList = new ArrayList<CDProcessingDetails>();
				for(int a=0;a<cdProcessingList.size();a++)
				{
					details = (CDProcessingDetails)cdProcessingList.get(a);
					details.setSrNo((a+1));
					if(null!=details.getLineType() && details.getLineType().equals(ConversionUtils.LINE_TYPE_VALID) 
							&& null!=details.getFilePath() && !"".equals(details.getFilePath()))
					{
						// add all the valid Lines to Processing List
						processingList.add(details);
					}
					else
					{
						details.setProcessingStatus("FAILURE");
						if(null==details.getFilePath() || "".equals(details.getFilePath()))
						{
							details.setErrorCode("CDP002");
							details.setErrorMessage("SOURCE FILE PATH FOR THE CD PROCESSING DATA ROW IS NULL.");
						}
						else
						{
							details.setErrorCode("CDP000");
							details.setErrorMessage("CD PROCESSING DATA ROW IS NOT A VALID DATA.");
						}
					}
					details = null;
				}

				/*
				 * CREATE A SUBSET OF 150 DOCUMENTS 
				 */
				int partitionSize=150;
				List<List<CDProcessingDetails>> partitions = new ArrayList<List<CDProcessingDetails>>();
				// create Partitions of Unique Items Data and then fetch FK Codes / Data
				for (int i=0; i<processingList.size(); i += partitionSize) {
					partitions.add(processingList.subList(i, Math.min(i + partitionSize, processingList.size())));
				}

				if(null!=partitions && partitions.size()>0)
				{
					/*
					 * FOR EACH PARTITION PERFORM RETRIEVAL & FETCH OPERATION
					 */
					/*
					 * INSERT INTO CD PROCESSING DETAILS
					 */
					details = new CDProcessingDetails();
					CDProcessingDetails subSetDetails = null;
					int listCount=0;
					for(List<CDProcessingDetails> subList : partitions)
					{
						listCount++;
						logger.info("saveCDProcessingDetails :: Start Processing Subset >> "+ listCount +" / " + partitions.size());
						subList=  processCDDataInformation(subList, tableName, cdProcessingTableName, pathToBeReplaced,channelFolderName, conn);
						/*
						 * ITERATE SUBLIST AND UPDATE THE STATUS / ERROR CODES / ERROR MESSAGES IN ACTUAL CD PROCESSING LIST
						 */
						if(null!=subList && subList.size()>0)
						{
							subSetDetails = null;
							for(int c=0;c<subList.size();c++)
							{
								subSetDetails = (CDProcessingDetails)subList.get(c);
								details = null;
								for(int r=0;r<cdProcessingList.size();r++)
								{
									details = (CDProcessingDetails)cdProcessingList.get(r);
									if(subSetDetails.getSrNo()==details.getSrNo())
									{
										details.setProcessingStatus(subSetDetails.getProcessingStatus());
										details.setErrorCode(subSetDetails.getErrorCode());
										details.setErrorMessage(subSetDetails.getErrorMessage());
										break;
									}
									details = null;
								}
								subSetDetails = null;
							}
						}
						logger.info("saveCDProcessingDetails :: End Processing Subset >> "+ listCount +" / " + partitions.size());
						subList = null;
					}
				}
				partitions=null;
				processingList = null;
				/*
				 * IDENITFY THE SUCCESS & FAILURE COUNT
				 */
				String scheduleId=null;
				String itemId=null;
				details = null;
				int successCount=0;
				boolean namesFound = true;
				for(int a=0;a<cdProcessingList.size();a++)
				{
					details = (CDProcessingDetails)cdProcessingList.get(a);
					scheduleId = details.getScheduleId();
					itemId= details.getItemId();
					if(null!=details.getProcessingStatus() && details.getProcessingStatus().equals("SUCCESS"))
					{
						namesFound = true;
						/*
						 * CHECK HERE IF NAMES AVAILABLE FOR ALL DISPLAY ORDER CODES OR NOT
						 * IF ALL AVAILABLE - THEN SUCCESS - update successCount
						 * ELSE FAILURE - WITH ERROR MESSAGE = DISPLAY ORDER NAMES COULD NOT BE LOCATED.
						 * 	updateFailureCount
						 */
						// Start with Level 1
						// Display order CODES are no longer supplied - the file carries the NAMES themselves,
						// so there is no code left to find a name for and nothing to validate here.

						if(namesFound==true)
						{
							// UPDATE PROCESSING COUNT
							successCount++;
						}
						else
						{
							// UPDATE FAILURE COUNT
							failureCount++;
							// set PROCESSING STATUS AS FAILURE
							details.setProcessingStatus("FAILURE");
							details.setErrorCode("CDP001");
							details.setErrorMessage("FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES IN THE ROW.");
						}
					}
					else
					{
						// increment failure count
						failureCount++;
					}
				}

				// UPDATE SUCCESS COUNT & FAILURE COUNT FOR SCHEDULE & ITEM
				if(null!=scheduleId && !"".equals(scheduleId) && null!=itemId && !"".equals(itemId))
				{
					ScheduleDAO.updateCDCount(scheduleId, itemId, conn, successCount, failureCount);
				}
				scheduleId = null;
				itemId = null;
				tableName= null;
				cdProcessingTableName=null;
				details=null;
			}
			else
			{
				logger.info("saveCDProcessingDetails :: CD PROCESSING LIST OR MODEL TYPE IS NULL. Return false");
				// ITERATE CD PROCESSING LIST AND UPDATE PROCESSING STATUS AS NULL FOR ALL ROWS IN THE LIST
				if(null!=cdProcessingList && cdProcessingList.size()>0)
				{
					details = new CDProcessingDetails();
					String scheduleId="";
					String itemId="";
					for(int a=0;a<cdProcessingList.size();a++)
					{
						details = (CDProcessingDetails)cdProcessingList.get(a);
						scheduleId = details.getScheduleId();
						itemId= details.getItemId();
						details.setProcessingStatus("FAILURE");
						details.setErrorCode("CDP001");
						details.setErrorMessage("MODEL TYPE AS PARAMETER WAS NULL. FAILED TO IDENTIFY CD DATA PROCESSING TABLE NAME.");
					}

					if(null!=scheduleId && !"".equals(scheduleId) && null!=itemId && !"".equals(itemId))
					{
						/*
						 * HERE IN THIS CASE UPDATE CD PROCESSING LIST SIZE AS FAILURE COUNT
						 */
						ScheduleDAO.updateCDCount(scheduleId, itemId, conn, 0, cdProcessingList.size());
					}
					scheduleId = null;
					itemId=  null;
				}
			}
			details=null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveCDProcessingDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();;
			}
			catch(Exception eq)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveCDProcessingDetails()", eq);
			}
		}
		return cdProcessingList;
	}
	

	public ArrayList<CDProcessingDetails> saveCDProcessingDetails(ArrayList<CDProcessingDetails> cdProcessingList, String modelType, String pathToBeReplaced, String channelFolderName,String locale)  
	{
		Connection conn = null;
		try
		{
			CDProcessingDetails details = new CDProcessingDetails();
			if(null!=modelType && !"".equals(modelType) && null!=cdProcessingList && cdProcessingList.size()>0)
			{
				locale = locale.replace("-", "_");
				String tableName="";
				String cdProcessingTableName="";
				if(null!=modelType && modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					cdProcessingTableName="gms3_dmt_mc_newm_cd_data";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					cdProcessingTableName="gms3_dmt_mc_cd_data";
				}

				int failureCount=0;
				logger.info("saveCDProcessingDetails :: CD rows for the material folder :: >" + cdProcessingList.size() + " :: table :: >" + cdProcessingTableName);
				conn = getConnection();
				logger.info("saveCDProcessingDetails :: database connection obtained.");
				/*
				 * BEFORE PROCESSING FIND ALL THE INVALID LINES AND UPDATE THEIR PROCESSING STATUS
				 */
				details = null;
				ArrayList<CDProcessingDetails> processingList = new ArrayList<CDProcessingDetails>();
				for(int a=0;a<cdProcessingList.size();a++)
				{
					details = (CDProcessingDetails)cdProcessingList.get(a);
					details.setSrNo((a+1));
					if(null!=details.getLineType() && details.getLineType().equals(ConversionUtils.LINE_TYPE_VALID) 
							&& null!=details.getFilePath() && !"".equals(details.getFilePath()))
					{
						// add all the valid Lines to Processing List
						processingList.add(details);
					}
					else
					{
						details.setProcessingStatus("FAILURE");
						if(null==details.getFilePath() || "".equals(details.getFilePath()))
						{
							details.setErrorCode("CDP002");
							details.setErrorMessage("SOURCE FILE PATH FOR THE CD PROCESSING DATA ROW IS NULL.");
						}
						else
						{
							details.setErrorCode("CDP000");
							details.setErrorMessage("CD PROCESSING DATA ROW IS NOT A VALID DATA.");
						}
					}
					details = null;
				}

				
				
				/*
				 * CREATE DOCUMENTS WISE CD LIST
				 */
				details = null;
				CDProcessingDetails doccddetails=null;
				List<CDProcessingDetails> documentsWiseCDList = new ArrayList<CDProcessingDetails>();
				for(int r=0;r<processingList.size();r++)
				{
					details = (CDProcessingDetails)processingList.get(r);
					if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
					{
						boolean add = true;
						if(null!=documentsWiseCDList && documentsWiseCDList.size()>0)
						{
							doccddetails = null;
							for(int t=0;t<documentsWiseCDList.size();t++)
							{
								doccddetails = (CDProcessingDetails)documentsWiseCDList.get(t);
								if(null!=doccddetails.getFilePath() && !"".equals(doccddetails.getFilePath()))
								{
									if(doccddetails.getFilePath().trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase()))
									{
										// document already added
										add = false;
										// add current row to child List
										if(null==doccddetails.getChildList() || doccddetails.getChildList().size()<=0)
										{
											doccddetails.setChildList(new ArrayList<CDProcessingDetails>());
										}
										doccddetails.getChildList().add(details);
										break;
									}
								}
								doccddetails = null;
							}
						}
						
						if(add==true)
						{
							// add to document wise CD List
							doccddetails =  new CDProcessingDetails();
							doccddetails.setFilePath(details.getFilePath());
							if(null==doccddetails.getChildList() || doccddetails.getChildList().size()<=0)
							{
								doccddetails.setChildList(new ArrayList<CDProcessingDetails>());
							}
							doccddetails.getChildList().add(details);
							// add to documentsWiseCDList
							documentsWiseCDList.add(doccddetails);
							doccddetails = null;
						}
					}
				}
				
				logger.info("saveCDProcessingDetails :: Total Unique Documents List found are :: >"+ documentsWiseCDList.size());

				/*
				 * CREATE A SUBSET OF 50 DOCUMENTS 
				 */
				int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.lookup.batch.size"));
				List<List<CDProcessingDetails>> partitions = new ArrayList<List<CDProcessingDetails>>();
				// create Partitions of Unique Items Data and then fetch FK Codes / Data
				for (int i=0; i<documentsWiseCDList.size(); i += partitionSize) {
					partitions.add(documentsWiseCDList.subList(i, Math.min(i + partitionSize, documentsWiseCDList.size())));
				}

				if(null!=partitions && partitions.size()>0)
				{
					/*
					 * FOR EACH PARTITION PERFORM RETRIEVAL & FETCH OPERATION
					 */
					/*
					 * INSERT INTO CD PROCESSING DETAILS
					 */
					details = new CDProcessingDetails();
					CDProcessingDetails subSetDetails = null;
					int listCount=0;
					for(List<CDProcessingDetails> subDocList : partitions)
					{
						List<CDProcessingDetails> subList = new ArrayList<CDProcessingDetails>();
						listCount++;
						logger.info("saveCDProcessingDetails :: Start Processing Subset >> "+ listCount +" / " + partitions.size());
						subList=  processCDDataInformation(subDocList, tableName, cdProcessingTableName, pathToBeReplaced,channelFolderName, conn);
						/*
						 * ITERATE SUBLIST AND UPDATE THE STATUS / ERROR CODES / ERROR MESSAGES IN ACTUAL CD PROCESSING LIST
						 */
						if(null!=subList && subList.size()>0)
						{
							subSetDetails = null;
							for(int c=0;c<subList.size();c++)
							{
								subSetDetails = (CDProcessingDetails)subList.get(c);
								details = null;
								for(int r=0;r<cdProcessingList.size();r++)
								{
									details = (CDProcessingDetails)cdProcessingList.get(r);
									if(subSetDetails.getSrNo()==details.getSrNo())
									{
										details.setProcessingStatus(subSetDetails.getProcessingStatus());
										details.setErrorCode(subSetDetails.getErrorCode());
										details.setErrorMessage(subSetDetails.getErrorMessage());
										break;
									}
									details = null;
								}
								subSetDetails = null;
							}
						}
						logger.info("saveCDProcessingDetails :: End Processing Subset >> "+ listCount +" / " + partitions.size());
						subList = null;
						subDocList = null;
					}
				}
				partitions=null;
				processingList = null;
				/*
				 * IDENITFY THE SUCCESS & FAILURE COUNT
				 */
				String scheduleId=null;
				String itemId=null;
				details = null;
				int successCount=0;
				boolean namesFound = true;
				for(int a=0;a<cdProcessingList.size();a++)
				{
					details = (CDProcessingDetails)cdProcessingList.get(a);
					scheduleId = details.getScheduleId();
					itemId= details.getItemId();
					if(null!=details.getProcessingStatus() && details.getProcessingStatus().equals("SUCCESS"))
					{
						namesFound = true;
						/*
						 * CHECK HERE IF NAMES AVAILABLE FOR ALL DISPLAY ORDER CODES OR NOT
						 * IF ALL AVAILABLE - THEN SUCCESS - update successCount
						 * ELSE FAILURE - WITH ERROR MESSAGE = DISPLAY ORDER NAMES COULD NOT BE LOCATED.
						 * 	updateFailureCount
						 */
						// Start with Level 1
						// Display order CODES are no longer supplied - the file carries the NAMES themselves,
						// so there is no code left to find a name for and nothing to validate here.

						if(namesFound==true)
						{
							// UPDATE PROCESSING COUNT
							successCount++;
						}
						else
						{
							// UPDATE FAILURE COUNT
							failureCount++;
							// set PROCESSING STATUS AS FAILURE
							details.setProcessingStatus("FAILURE");
							details.setErrorCode("CDP001");
							details.setErrorMessage("FAILED TO IDENTIFY DISPLAY ORDER NAMES FOR THE DISPLAY ORDER CODES IN THE ROW.");
						}
					}
					else
					{
						// increment failure count
						failureCount++;
					}
				}

				// UPDATE SUCCESS COUNT & FAILURE COUNT FOR SCHEDULE & ITEM
				if(null!=scheduleId && !"".equals(scheduleId) && null!=itemId && !"".equals(itemId))
				{
					ScheduleDAO.updateCDCount(scheduleId, itemId, conn, successCount, failureCount);
				}
				scheduleId = null;
				itemId = null;
				tableName= null;
				cdProcessingTableName=null;
				details=null;
			}
			else
			{
				logger.info("saveCDProcessingDetails :: CD PROCESSING LIST OR MODEL TYPE IS NULL. Return false");
				// ITERATE CD PROCESSING LIST AND UPDATE PROCESSING STATUS AS NULL FOR ALL ROWS IN THE LIST
				if(null!=cdProcessingList && cdProcessingList.size()>0)
				{
					details = new CDProcessingDetails();
					String scheduleId="";
					String itemId="";
					for(int a=0;a<cdProcessingList.size();a++)
					{
						details = (CDProcessingDetails)cdProcessingList.get(a);
						scheduleId = details.getScheduleId();
						itemId= details.getItemId();
						details.setProcessingStatus("FAILURE");
						details.setErrorCode("CDP001");
						details.setErrorMessage("MODEL TYPE AS PARAMETER WAS NULL. FAILED TO IDENTIFY CD DATA PROCESSING TABLE NAME.");
					}

					if(null!=scheduleId && !"".equals(scheduleId) && null!=itemId && !"".equals(itemId))
					{
						/*
						 * HERE IN THIS CASE UPDATE CD PROCESSING LIST SIZE AS FAILURE COUNT
						 */
						ScheduleDAO.updateCDCount(scheduleId, itemId, conn, 0, cdProcessingList.size());
					}
					scheduleId = null;
					itemId=  null;
				}
			}
			details=null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveCDProcessingDetails()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();;
			}
			catch(Exception eq)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "saveCDProcessingDetails()", eq);
			}
		}
		return cdProcessingList;
	}
	
	/**
	 * Function will Process all the InnerLinks for a Document
	 * @param contentDetails
	 * @param conn
	 * @param tableName
	 * @throws SQLException
	 */
	private void processInnerLinksInformation(ContentDetails contentDetails, Connection conn, String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		
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
			LinkDetails linkDetails = new LinkDetails();
			int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
			ArrayList<List<LinkDetails>> partitions = new ArrayList<List<LinkDetails>>();
			for (int i=0; i<linksList.size(); i += partitionSize) {
				partitions.add(linksList.subList(i, Math.min(i + partitionSize, linksList.size())));
			}
			
			if(null!=partitions && partitions.size()>0)
			{
				String insertInnerLinkSql="";
				for(List<LinkDetails> subList : partitions)
				{
					if(null!=subList && subList.size()>0)
					{
						pstmt=  null;
						insertInnerLinkSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
								+ "DC_SOURCE_NETWORK_LOC,DC_INNER_DOC_LINK_PATH,DC_INNER_IM_DOC_ID,"
								+ "DC_INNER_LINK_UPDATED_STATUS,DC_CREATED_TMSTP) "
								+ "VALUES(?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(insertInnerLinkSql);
						for(int i=0;i<subList.size();i++)
						{
							linkDetails = (LinkDetails)subList.get(i);
							pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
							pstmt.setString(2, contentDetails.getImDocumentId());
							pstmt.setString(3, contentDetails.getFilePath());
							pstmt.setString(4, linkDetails.getInnerLinkPath());
							pstmt.setString(5, linkDetails.getInnnerLinkDocumentId());
							pstmt.setString(6, linkDetails.getMapStatus());
							pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
							pstmt.addBatch();
							linkDetails = null;
						}
						pstmt.executeBatch();
						pstmt.close();pstmt = null;
						insertInnerLinkSql  =null;
					}
					subList =null;
				}
			}
			linkDetails = null;
			partitions  =null;
		}
		linksList = null;
	}
	
	
	/**
	 * Function will process the Category Details for a Document
	 * @param contentDetails
	 * @param conn
	 * @param tableName
	 * @throws SQLException
	 */
	private void processCategoryInformation(ContentDetails contentDetails, Connection conn , String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		if(null!=contentDetails.getCategoryList() && contentDetails.getCategoryList().size()>0)
		{
			int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
			ArrayList<List<CategoryDetails>> partitions = new ArrayList<List<CategoryDetails>>();
			for (int i=0; i<contentDetails.getCategoryList().size(); i += partitionSize) {
				partitions.add(contentDetails.getCategoryList().subList(i, Math.min(i + partitionSize, contentDetails.getCategoryList().size())));
			}
			
			if(null!=partitions  && partitions.size()>0)
			{
				CategoryDetails details = new CategoryDetails();
				String insertCategoriesSql="";
				pstmt=null;
				for(List<CategoryDetails> subList : partitions)
				{
					if(null!=subList && subList.size()>0)
					{
						details = null;
						pstmt=null;
						insertCategoriesSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
								+ "DC_SOURCE_NETWORK_LOC,DC_IM_DOC_CATEGORY_NAME,DC_IM_DOC_CATEGORY_REF_KEY,"
								+ "DC_CREATED_TMSTP,DC_IM_DOC_CATEGORY_TYPE) VALUES(?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(insertCategoriesSql);
						for(int i=0;i<subList.size();i++)
						{
							details = (CategoryDetails)subList.get(i);
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
						insertCategoriesSql =null;
						details = null;
						pstmt.close();pstmt=null;
					}
					subList = null;
				}
			}
			partitions = null;
		}
	}

	/**
	 * Function will process ESI Category Details for a Document
	 * @param contentDetails
	 * @param conn
	 * @param tableName
	 * @throws SQLException
	 */
	private void processESICategoryInformation(ContentDetails contentDetails, Connection conn , String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		String insertCategoriesSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
				+ "DC_SOURCE_NETWORK_LOC,DC_ESICAT_CODE_LEVEL_1,DC_ESICAT_CODE_LEVEL_2,DC_ESICAT_CODE_LEVEL_3,"
				+ "DC_ESICAT_MAPPED_LEVEL,DC_ESICAT_MAPPED_REFKEY,DC_ESICAT_TXT_FILE_NAME,DC_ESICAT_TXT_FILE_PATH,"
				+ " DC_ESICAT_CREATED_TMSTP) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
		pstmt = conn.prepareStatement(insertCategoriesSql);
		pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
		pstmt.setString(2, contentDetails.getImDocumentId());
		pstmt.setString(3, contentDetails.getFilePath());
		pstmt.setString(4, contentDetails.getCategoryCode());
		pstmt.setString(5, contentDetails.getSubCategoryCode());
		pstmt.setString(6, contentDetails.getSubSubCategoryCode());
		pstmt.setString(7, contentDetails.getEsiCategoryMappedLevel());
		pstmt.setString(8, contentDetails.getEsiCategoryMappedRefKey());
		pstmt.setString(9, contentDetails.getEsiCategoryMappedFileName());
		pstmt.setString(10, contentDetails.getEsiCategoryMappedFilePath());
		pstmt.setTimestamp(11, new java.sql.Timestamp(new Date().getTime()));
		pstmt.executeUpdate();
		pstmt.close();
		pstmt= null;
		insertCategoriesSql =null;
	}

	/**
	 * Function will Process VIN Category Details for a Document
	 * @param contentDetails
	 * @param conn
	 * @param tableName
	 * @throws SQLException
	 */
	private void processVINInformation(ContentDetails contentDetails, Connection conn , String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		if(null!=contentDetails.getApplicableVINList() && contentDetails.getApplicableVINList().size()>0)
		{
			int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
			ArrayList<List<VinDetails>> partitions = new ArrayList<List<VinDetails>>();
			for (int i=0; i<contentDetails.getApplicableVINList().size(); i += partitionSize) {
				partitions.add(contentDetails.getApplicableVINList().subList(i, Math.min(i + partitionSize, contentDetails.getApplicableVINList().size())));
			}
			if(null!=partitions  && partitions.size()>0)
			{
				VinDetails details = new VinDetails();
				String insertCategoriesSql="";
				for(List<VinDetails> subList : partitions )
				{
					if(null!=subList && subList.size()>0)
					{
						pstmt  =null;
						details = null;
						insertCategoriesSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
								+ "DC_SOURCE_NETWORK_LOC,DC_VIN_CARLINE_CODE,DC_VIN_VDS_CODE,"
								+ "DC_VIN_VIS_START_RANGE,DC_VIN_VIS_END_RANGE, DC_VIN_DISPLAYORDER_ENTRY,DC_VIN_REFKEY,"
								+ "DC_VIN_TXT_FILE_NAME"
								+ ",DC_VIN_TXT_FILE_PATH,DC_VIN_CREATED_TMSTP) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(insertCategoriesSql);
						for(int i=0;i<subList.size();i++)
						{
							details = (VinDetails)subList.get(i);
							pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
							pstmt.setString(2, contentDetails.getImDocumentId());
							pstmt.setString(3, contentDetails.getFilePath());
							pstmt.setString(4, details.getCarlineCode());
							pstmt.setString(5, details.getVdsCode());
							pstmt.setString(6, details.getVisStartRange());
							pstmt.setString(7, details.getVisEndRange());
							pstmt.setString(8, details.getDisplayOrderTextFileName());
							pstmt.setString(9, details.getVinRefKey());
							pstmt.setString(10, details.getVinSourceFileName());
							pstmt.setString(11, details.getVinSorceFilePath());
							pstmt.setTimestamp(12, new java.sql.Timestamp(new Date().getTime()));
							pstmt.addBatch();
							details = null;
						}
						pstmt.executeBatch();
						pstmt.close();pstmt=null;
						insertCategoriesSql=  null;
						details = null;
					}
					subList  = null;
				}
			}
			partitions = null;
		}
	}
	
	/**
	 * Function will Process Master VIN Category details for a Document
	 * @param contentDetails
	 * @param conn
	 * @param tableName
	 * @throws SQLException
	 */
	private void processMasterVINInformation(ContentDetails contentDetails, Connection conn , String tableName) throws SQLException
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		if(null!=contentDetails.getMasterVinList() && contentDetails.getMasterVinList().size()>0)
		{
			int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
			ArrayList<List<VINEntFileDetails>> partitions = new ArrayList<List<VINEntFileDetails>>();
			for (int i=0; i<contentDetails.getMasterVinList().size(); i += partitionSize) {
				partitions.add(contentDetails.getMasterVinList().subList(i, Math.min(i + partitionSize, contentDetails.getMasterVinList().size())));
			}
			
			if(null!=partitions && partitions.size()>0)
			{
				VINEntFileDetails details= new VINEntFileDetails();
				String insertCategoriesSql="";
				for(List<VINEntFileDetails> subList : partitions)
				{
					if(null!=subList && subList.size()>0)
					{
						pstmt  = null;
						details = null;
						insertCategoriesSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
								+ "DC_SOURCE_NETWORK_LOC,DC_VM_CARLINE_CODE,DC_VM_VDS_CODE,"
								+ "DC_VM_VIS_START_RANGE,DC_VM_VIS_END_RANGE,DC_VM_WMI_CODE,DC_VM_REFKEY,"
								+ "DC_VM_CREATED_TMSTP) VALUES(?,?,?,?,?,?,?,?,?,?)";
						pstmt = conn.prepareStatement(insertCategoriesSql);
						for(int i=0;i<subList.size();i++)
						{
							details = (VINEntFileDetails)subList.get(i);
							pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
							pstmt.setString(2, contentDetails.getImDocumentId());
							pstmt.setString(3, contentDetails.getFilePath());
							pstmt.setString(4, details.getVinCarline());
							pstmt.setString(5, details.getVinVDS());
							pstmt.setString(6, details.getVinStartRange());
							pstmt.setString(7, details.getVinEndRange());
							pstmt.setString(8, details.getVinWMI());
							pstmt.setString(9, details.getVinRefKey());
							pstmt.setTimestamp(10, new java.sql.Timestamp(new Date().getTime()));
							pstmt.addBatch();
							details = null;
						}
						pstmt.executeBatch();
						pstmt.close();pstmt=null;
						insertCategoriesSql  =null;
						details = null;
					}
					subList = null;
				}
			}
			partitions = null;
		}
	}

	private List<CDProcessingDetails> processCDDataInformation_backup(List<CDProcessingDetails> subList, String tableName, String cdProcessingTableName,
			String pathToBeReplaced, String channelFolderName, Connection conn)
	{
		Statement stmt=null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//		String checkFormultiplePaths=null;
		Writer writer = null;
		PrintWriter print=  null;
		try
		{
			CDProcessingDetails details = null;
			String keyToCheck=null;
			String filePath=null;
			for(int a=0;a<subList.size();a++)
			{
				details = (CDProcessingDetails)subList.get(a);
				if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
				{
					filePath = details.getFilePath();
					if(null!=filePath && !"".equals(filePath))
					{
						if(null==keyToCheck)
						{
							keyToCheck="";
						}
						keyToCheck+="'"+filePath+"',";
					}
					filePath =null;
				}
				details = null;
			}

			logger.info("processCDDataInformation :: Key To Check :: > "+ keyToCheck);
			/*
			 * Proceed for fetching documentIds for all records
			 */
			if(null!=keyToCheck && !"".equals(keyToCheck))
			{
				/*
				 * check if another KeyToCheck is not null, then append it to the keyToCheck
				 */
				if(keyToCheck.endsWith(","))
				{
					keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
				}
				/*if(null!=anotherKeyToCheck && !"".equals(anotherKeyToCheck))
				{
					if(anotherKeyToCheck.endsWith(","))
					{
						anotherKeyToCheck=anotherKeyToCheck.substring(0, anotherKeyToCheck.length()-1);
					}
					keyToCheck+=","+anotherKeyToCheck;
				}*/

				logger.info("processCDDataInformation :: Final Key To Check with another Key Appended :: > "+ keyToCheck);

				String documentId=null;
				String sourcePath = null;
				details = null;
				String getDocumentSql="SELECT DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC FROM "+ tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN ("+keyToCheck.trim().toLowerCase()+") "
						+ " AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				try
				{
					pstmt = conn.prepareStatement(getDocumentSql);
					//					pstmt.setString(1, keyToCheck.trim().toLowerCase());
					rs = pstmt.executeQuery();
					while(rs.next())
					{
						documentId = rs.getString("DC_IM_DOC_ID");
						sourcePath = rs.getString("DC_SOURCE_NETWORK_LOC");
						if(null!=documentId && !"".equals(documentId) && null!=sourcePath && !"".equals(sourcePath))
						{
							details = null;
							for(int a=0;a<subList.size();a++)
							{
								details = (CDProcessingDetails)subList.get(a);
								filePath=null;
								//								anotherFilePath=null;
								if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
								{
									filePath = details.getFilePath();
									/*checkFormultiplePaths = null;
									// CONDITION WHEN PROCESSING ALL CD PROCESSING FILE KEPT INSIDE FACE LIFT FOLDER.
									if(null!=pathToBeReplaced && pathToBeReplaced.equals("CHECK_INSIDE_LIST"))
									{
										removeExt=true;
										if(null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) && 
												details.getProcessingFolderName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5").trim().toLowerCase()))
										{
											removeExt=false;
										}

										if(removeExt==true && (null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName())))
										{
											// APPLICABLE ONLY FOR WD
											if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
											{
												tok="";
												mtName = details.getMaterialFolderName();
												if(mtName.lastIndexOf("_")!=-1)
												{
													tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
												}
												if(null==tok)
												{
													tok="";
												}
												// DO NOT CHECK FOR NULL
												if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
												{
													// set replaceByPDFTO HTML applicable for this case only (OVER RIDE THE VALUE)
													checkFormultiplePaths = "YES";
												}
												tok = null;
												mtName = null;
											}
										}
									}*/

									// NOW CHECK FOR YES. CONTINUE NORMAL FLOW
									/*if(null!=checkFormultiplePaths && checkFormultiplePaths.equals("YES"))
									{
										if(filePath.contains("\\pdf\\"))
										{
											anotherFilePath = filePath.replace("\\pdf\\", "\\html\\");
										}
										else if(filePath.contains("\\html\\"))
										{
											anotherFilePath = filePath.replace("\\html\\", "\\pdf\\");
										}
									}*/

									/*if(anotherFilePath==null)
									{
										anotherFilePath = "";
									}
									if(sourcePath.trim().toLowerCase().equals(filePath.trim().toLowerCase()) || sourcePath.trim().toLowerCase().equals(anotherFilePath.trim().toLowerCase()))
									{
										details.setDocumentId(documentId);
									}*/
									if(sourcePath.trim().toLowerCase().equals(filePath.trim().toLowerCase()))
									{
										details.setDocumentId(documentId);
									}
								}
								details = null;
								filePath=  null;
								//								anotherFilePath = null;
							}
						}
						documentId=null;
						sourcePath=null;
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
					getDocumentSql=null;
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
				}
			}
			keyToCheck = null;
			//			anotherKeyToCheck = null;

			/*
			 * NOW IDENITFY THE RECORDS TO BE SAVED IN DATABASE WHERE DOCUMENT ID IS NOT NULL
			 */
			details = null;
			String cdProcessingSourceFileName=null;
			List<CDProcessingDetails> dataToBeSaved = new ArrayList<CDProcessingDetails>();
			for(int a=0;a<subList.size();a++)
			{
				details = (CDProcessingDetails)subList.get(a);
				if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()) && null!=details.getCdProcessingSourceFileName() && !"".equals(details.getCdProcessingSourceFileName()))
				{
					cdProcessingSourceFileName = details.getCdProcessingSourceFileName();
					// set Processing status as SUCCESS - DEFAULT
					details.setProcessingStatus("SUCCESS");
					dataToBeSaved.add(details);
				}
				else
				{
					details.setProcessingStatus("FAILURE");
					details.setErrorCode("CDP003");
					String errorMessage="";
					if(null==details.getDocumentId() || "".equals(details.getDocumentId()))
					{
						errorMessage = "FAILED TO IDETNFIY IM DOCUMENT ID.";
						logger.info("processCDDataInformation :: Failed to Identify Document Id for Source File Path :: > "+ details.getFilePath());
					}
					if(null==details.getCdProcessingSourceFileName() || "".equals(details.getCdProcessingSourceFileName()))
					{
						if(null!=errorMessage && !"".equals(errorMessage))
						{
							errorMessage = errorMessage+"\n";
						}
						errorMessage=errorMessage+"CD PROCESSING TEXT FILE NAME IS NULL.";
						logger.info("processCDDataInformation :: CD Processing Text File Name is null for Source File Path :: > "+ details.getFilePath());
					}
					details.setErrorMessage(errorMessage);
					errorMessage  = null;
				}
				details = null;
			}

			if(null!=dataToBeSaved && dataToBeSaved.size()>0)
			{
				details = null;
				conn.setAutoCommit(false);
				try
				{
					// delete the document Id records first
					String deleteIds="";
					details = null;
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						deleteIds+="'"+details.getDocumentId()+"'";
						if(b!=dataToBeSaved.size()-1)
						{
							deleteIds+=",";
						}
						details = null;
					}

					String deleteSql="DELETE FROM "+ cdProcessingTableName+" WHERE DC_IM_DOC_ID IN ("+deleteIds+") "
							+ " AND TRIM(LOWER(DC_CD_TXT_FILE_NAME))='"+cdProcessingSourceFileName.trim().toLowerCase()+"'";
					stmt = conn.createStatement();
					stmt.executeUpdate(deleteSql);
					stmt.close();
					stmt=  null;
					deleteSql = null; 
					deleteIds = null;

					/*
					 * PROCEED FOR INSERTING
					 */
					String insertCDDataSql="INSERT INTO "+cdProcessingTableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
							+ "DC_SOURCE_NETWORK_LOC,DC_CD_DISPORD_LEVEL_1,DC_CD_DISPORD_LEVEL_2,DC_CD_DISPORD_LEVEL_3," +
							" DC_CD_DISPORD_LEVEL_4,DC_CD_DISPORD_LEVEL_5,"
							+ "DC_CD_DISPORD_LEVEL_6,DC_CD_MODEL_FOLDER,DC_CD_MANUALTYPE_FOLDER,DC_CD_FACELIFT_FOLDER,DC_CD_MATERIAL_FOLDER,"
							+ "DC_CD_FILETYPE_FOLDER,DC_CD_SOURCE_FILE_NAME,DC_IM_DOC_MANUAL_TYPE_REFKEY,DC_CD_TXT_FILE_NAME,DC_CD_TXT_FILE_PATH,"
							+ "DC_CD_CREATED_TMSTP,DC_SEQUENCE_NO,DC_ENGINE_TYPE,DC_MISSION_TYPE,DC_DRIVEAXLE_TYPE,DC_BODY_TYPE,"
							+ "DC_TITLE,DC_CD_DISPORD_NAME_LEVEL_1,DC_CD_DISPORD_NAME_LEVEL_2,DC_CD_DISPORD_NAME_LEVEL_3,DC_CD_DISPORD_NAME_LEVEL_4,"
							+ "DC_CD_DISPORD_NAME_LEVEL_5,DC_CD_DISPORD_NAME_LEVEL_6) "
							+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertCDDataSql);
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						pstmt.setLong(1, new Long(details.getScheduleId()).longValue());
						pstmt.setString(2, details.getDocumentId());
						pstmt.setString(3, details.getFilePath());
						pstmt.setString(4, details.getDisplayOrderLevel1Code());
						pstmt.setString(5, details.getDisplayOrderLevel2Code());
						pstmt.setString(6, details.getDisplayOrderLevel3Code());
						pstmt.setString(7, details.getDisplayOrderLevel4Code());
						pstmt.setString(8, details.getDisplayOrderLevel5Code());
						pstmt.setString(9, details.getDisplayOrderLevel6Code());
						pstmt.setString(10, details.getModelFolderName());
						pstmt.setString(11, details.getManualType());
						pstmt.setString(12, details.getFaceLiftFolderName());
						pstmt.setString(13, details.getMaterialFolderName());
						pstmt.setString(14, details.getProcessingFolderName());
						pstmt.setString(15, details.getFileName());
						pstmt.setString(16, details.getManualTypeRefKey());
						pstmt.setString(17, details.getCdProcessingSourceFileName());
						pstmt.setString(18, details.getCdProcessingSourceFilePath());
						pstmt.setTimestamp(19, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(20, details.getSequenceNo());
						pstmt.setString(21, details.getEngineType());
						pstmt.setString(22, details.getMissionType());
						pstmt.setString(23, details.getDriveAxleType());
						pstmt.setString(24, details.getBodyType());
						pstmt.setString(25, details.getTitle());
						pstmt.setString(26, details.getDisplayOrderLevel1Name());
						pstmt.setString(27, details.getDisplayOrderLevel2Name());
						pstmt.setString(28, details.getDisplayOrderLevel3Name());
						pstmt.setString(29, details.getDisplayOrderLevel4Name());
						pstmt.setString(30, details.getDisplayOrderLevel5Name());
						pstmt.setString(31, details.getDisplayOrderLevel6Name());
						pstmt.addBatch();

						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					// commit the transaction
					conn.commit();
					insertCDDataSql = null;
				}
				catch(Exception e)
				{
					// rollBack the transaction
					try
					{
						conn.rollback();
					}
					catch(SQLException e1)
					{
						Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e1);
					}
					Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
					writer = new StringWriter();
					print = new PrintWriter(writer);
					e.printStackTrace(print);

					details = null;
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						// SET FAILURE AND REASON FOR FAILURE
						details.setProcessingStatus("FAILURE");
						details.setErrorCode(e.getMessage());
						details.setErrorMessage(writer.toString());
					}
					// set writer & print to null
					writer = null;
					// set print to null
					print = null;
				}
			}
			cdProcessingSourceFileName = null;

			/*
			 * UPDATE THE PROCESSING STATUS OF DATA TO BE SAVED IN SUBLIST
			 */
			CDProcessingDetails subListDetails = null;
			details = null;
			for(int a=0;a<subList.size();a++)
			{	
				subListDetails = (CDProcessingDetails)subList.get(a);
				details = null;
				for(int b=0;b<dataToBeSaved.size();b++)
				{
					details = (CDProcessingDetails)dataToBeSaved.get(b);
					if(details.getSrNo()==subListDetails.getSrNo())
					{
						subListDetails.setErrorCode(details.getErrorCode());
						subListDetails.setProcessingStatus(details.getProcessingStatus());
						subListDetails.setErrorMessage(details.getErrorMessage());
						break;
					}
					details = null;
				}
				subListDetails = null;
			}
			details = null;
			subListDetails = null;
			dataToBeSaved = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
			writer = new StringWriter();
			print = new PrintWriter(writer);
			e.printStackTrace(print);

			CDProcessingDetails details = null;
			for(int b=0;b<subList.size();b++)
			{
				details = (CDProcessingDetails)subList.get(b);
				// SET FAILURE AND REASON FOR FAILURE
				details.setProcessingStatus("FAILURE");
				details.setErrorCode(e.getMessage());
				details.setErrorMessage(writer.toString());
			}
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			details = null;
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
			}
			//			checkFormultiplePaths = null;
			pathToBeReplaced = null;
		}
		return subList;
	}
	
	
	private List<CDProcessingDetails> processCDDataInformation(List<CDProcessingDetails> subList, String tableName, String cdProcessingTableName,
			String pathToBeReplaced, String channelFolderName, Connection conn)
	{
		List<CDProcessingDetails> childList=new ArrayList<CDProcessingDetails>();
		Statement stmt=null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//		String checkFormultiplePaths=null;
		Writer writer = null;
		PrintWriter print=  null;
		CDProcessingDetails details = null;
		// re-create child list, so all CD Data document wise gets created
		if(null!=subList && subList.size()>0)
		{
			for(int a=0;a<subList.size();a++)
			{
				details = (CDProcessingDetails)subList.get(a);
				if(null!=details.getChildList() && details.getChildList().size()>0)
				{
					childList.addAll(details.getChildList());
				}
				details = null;
			}
		}
		try
		{
			String keyToCheck=null;
			String filePath=null;
			for(int a=0;a<subList.size();a++)
			{
				details = (CDProcessingDetails)subList.get(a);
				if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
				{
					filePath = details.getFilePath();
					if(null!=filePath && !"".equals(filePath))
					{
						if(null==keyToCheck)
						{
							keyToCheck="";
						}
						keyToCheck+="'"+filePath+"',";
					}
					filePath =null;
				}
				details = null;
			}

			logger.info("processCDDataInformation :: Key To Check :: > "+ keyToCheck);
			/*
			 * Proceed for fetching documentIds for all records
			 */
			if(null!=keyToCheck && !"".equals(keyToCheck))
			{
				/*
				 * check if another KeyToCheck is not null, then append it to the keyToCheck
				 */
				if(keyToCheck.endsWith(","))
				{
					keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
				}
				/*if(null!=anotherKeyToCheck && !"".equals(anotherKeyToCheck))
				{
					if(anotherKeyToCheck.endsWith(","))
					{
						anotherKeyToCheck=anotherKeyToCheck.substring(0, anotherKeyToCheck.length()-1);
					}
					keyToCheck+=","+anotherKeyToCheck;
				}*/

				logger.info("processCDDataInformation :: Final Key To Check with another Key Appended :: > "+ keyToCheck);

				String documentId=null;
				String sourcePath = null;
				details = null;
				String getDocumentSql="SELECT DC_IM_DOC_ID,DC_SOURCE_NETWORK_LOC FROM "+ tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) IN ("+keyToCheck.trim().toLowerCase()+") "
						+ " AND DC_DOC_STATUS='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				try
				{
					pstmt = conn.prepareStatement(getDocumentSql);
					//					pstmt.setString(1, keyToCheck.trim().toLowerCase());
					rs = pstmt.executeQuery();
					while(rs.next())
					{
						documentId = rs.getString("DC_IM_DOC_ID");
						sourcePath = rs.getString("DC_SOURCE_NETWORK_LOC");
						if(null!=documentId && !"".equals(documentId) && null!=sourcePath && !"".equals(sourcePath))
						{
							details = null;
							for(int a=0;a<childList.size();a++)
							{
								details = (CDProcessingDetails)childList.get(a);
								filePath=null;
								//								anotherFilePath=null;
								if(null!=details.getFilePath() && !"".equals(details.getFilePath()))
								{
									filePath = details.getFilePath();
									/*checkFormultiplePaths = null;
									// CONDITION WHEN PROCESSING ALL CD PROCESSING FILE KEPT INSIDE FACE LIFT FOLDER.
									if(null!=pathToBeReplaced && pathToBeReplaced.equals("CHECK_INSIDE_LIST"))
									{
										removeExt=true;
										if(null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) && 
												details.getProcessingFolderName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5").trim().toLowerCase()))
										{
											removeExt=false;
										}

										if(removeExt==true && (null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName())))
										{
											// APPLICABLE ONLY FOR WD
											if(channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().toLowerCase()))
											{
												tok="";
												mtName = details.getMaterialFolderName();
												if(mtName.lastIndexOf("_")!=-1)
												{
													tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
												}
												if(null==tok)
												{
													tok="";
												}
												// DO NOT CHECK FOR NULL
												if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
														!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
												{
													// set replaceByPDFTO HTML applicable for this case only (OVER RIDE THE VALUE)
													checkFormultiplePaths = "YES";
												}
												tok = null;
												mtName = null;
											}
										}
									}*/

									// NOW CHECK FOR YES. CONTINUE NORMAL FLOW
									/*if(null!=checkFormultiplePaths && checkFormultiplePaths.equals("YES"))
									{
										if(filePath.contains("\\pdf\\"))
										{
											anotherFilePath = filePath.replace("\\pdf\\", "\\html\\");
										}
										else if(filePath.contains("\\html\\"))
										{
											anotherFilePath = filePath.replace("\\html\\", "\\pdf\\");
										}
									}*/

									/*if(anotherFilePath==null)
									{
										anotherFilePath = "";
									}
									if(sourcePath.trim().toLowerCase().equals(filePath.trim().toLowerCase()) || sourcePath.trim().toLowerCase().equals(anotherFilePath.trim().toLowerCase()))
									{
										details.setDocumentId(documentId);
									}*/
									if(sourcePath.trim().toLowerCase().equals(filePath.trim().toLowerCase()))
									{
										details.setDocumentId(documentId);
									}
								}
								details = null;
								filePath=  null;
								//								anotherFilePath = null;
							}
						}
						documentId=null;
						sourcePath=null;
					}
					rs.close();rs=null;
					pstmt.close();pstmt=null;
					getDocumentSql=null;
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
				}
			}
			keyToCheck = null;
			//			anotherKeyToCheck = null;

			/*
			 * NOW IDENITFY THE RECORDS TO BE SAVED IN DATABASE WHERE DOCUMENT ID IS NOT NULL
			 */
			details = null;
			String cdProcessingSourceFileName=null;
			List<CDProcessingDetails> dataToBeSaved = new ArrayList<CDProcessingDetails>();
			for(int a=0;a<childList.size();a++)
			{
				details = (CDProcessingDetails)childList.get(a);
				if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()) && null!=details.getCdProcessingSourceFileName() && !"".equals(details.getCdProcessingSourceFileName()))
				{
					cdProcessingSourceFileName = details.getCdProcessingSourceFileName();
					// set Processing status as SUCCESS - DEFAULT
					details.setProcessingStatus("SUCCESS");
					dataToBeSaved.add(details);
				}
				else
				{
					details.setProcessingStatus("FAILURE");
					details.setErrorCode("CDP003");
					String errorMessage="";
					if(null==details.getDocumentId() || "".equals(details.getDocumentId()))
					{
						errorMessage = "FAILED TO IDETNFIY IM DOCUMENT ID.";
						logger.info("processCDDataInformation :: Failed to Identify Document Id for Source File Path :: > "+ details.getFilePath());
					}
					if(null==details.getCdProcessingSourceFileName() || "".equals(details.getCdProcessingSourceFileName()))
					{
						if(null!=errorMessage && !"".equals(errorMessage))
						{
							errorMessage = errorMessage+"\n";
						}
						errorMessage=errorMessage+"CD PROCESSING TEXT FILE NAME IS NULL.";
						logger.info("processCDDataInformation :: CD Processing Text File Name is null for Source File Path :: > "+ details.getFilePath());
					}
					details.setErrorMessage(errorMessage);
					errorMessage  = null;
				}
				details = null;
			}

			if(null!=dataToBeSaved && dataToBeSaved.size()>0)
			{
				details = null;
				conn.setAutoCommit(false);
				try
				{
					// delete the document Id records first
					String deleteIds="";
					details = null;
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						deleteIds+="'"+details.getDocumentId()+"'";
						if(b!=dataToBeSaved.size()-1)
						{
							deleteIds+=",";
						}
						details = null;
					}

					String deleteSql="DELETE FROM "+ cdProcessingTableName+" WHERE DC_IM_DOC_ID IN ("+deleteIds+") "
							+ " AND TRIM(LOWER(DC_CD_TXT_FILE_NAME))='"+cdProcessingSourceFileName.trim().toLowerCase()+"'";
					stmt = conn.createStatement();
					stmt.executeUpdate(deleteSql);
					stmt.close();
					stmt=  null;
					deleteSql = null; 
					deleteIds = null;

					/*
					 * PROCEED FOR INSERTING
					 */
					String insertCDDataSql="INSERT INTO "+cdProcessingTableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,"
							+ "DC_SOURCE_NETWORK_LOC,DC_CD_DISPORD_LEVEL_1,DC_CD_DISPORD_LEVEL_2,DC_CD_DISPORD_LEVEL_3," +
							" DC_CD_DISPORD_LEVEL_4,DC_CD_DISPORD_LEVEL_5,"
							+ "DC_CD_DISPORD_LEVEL_6,DC_CD_MODEL_FOLDER,DC_CD_MANUALTYPE_FOLDER,DC_CD_FACELIFT_FOLDER,DC_CD_MATERIAL_FOLDER,"
							+ "DC_CD_FILETYPE_FOLDER,DC_CD_SOURCE_FILE_NAME,DC_IM_DOC_MANUAL_TYPE_REFKEY,DC_CD_TXT_FILE_NAME,DC_CD_TXT_FILE_PATH,"
							+ "DC_CD_CREATED_TMSTP,DC_SEQUENCE_NO,DC_ENGINE_TYPE,DC_MISSION_TYPE,DC_DRIVEAXLE_TYPE,DC_BODY_TYPE,"
							+ "DC_TITLE,DC_CD_DISPORD_NAME_LEVEL_1,DC_CD_DISPORD_NAME_LEVEL_2,DC_CD_DISPORD_NAME_LEVEL_3,DC_CD_DISPORD_NAME_LEVEL_4,"
							+ "DC_CD_DISPORD_NAME_LEVEL_5,DC_CD_DISPORD_NAME_LEVEL_6) "
							+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(insertCDDataSql);
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						pstmt.setLong(1, new Long(details.getScheduleId()).longValue());
						pstmt.setString(2, details.getDocumentId());
						pstmt.setString(3, details.getFilePath());
						pstmt.setString(4, details.getDisplayOrderLevel1Code());
						pstmt.setString(5, details.getDisplayOrderLevel2Code());
						pstmt.setString(6, details.getDisplayOrderLevel3Code());
						pstmt.setString(7, details.getDisplayOrderLevel4Code());
						pstmt.setString(8, details.getDisplayOrderLevel5Code());
						pstmt.setString(9, details.getDisplayOrderLevel6Code());
						pstmt.setString(10, details.getModelFolderName());
						pstmt.setString(11, details.getManualType());
						pstmt.setString(12, details.getFaceLiftFolderName());
						pstmt.setString(13, details.getMaterialFolderName());
						pstmt.setString(14, details.getProcessingFolderName());
						pstmt.setString(15, details.getFileName());
						pstmt.setString(16, details.getManualTypeRefKey());
						pstmt.setString(17, details.getCdProcessingSourceFileName());
						pstmt.setString(18, details.getCdProcessingSourceFilePath());
						pstmt.setTimestamp(19, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setString(20, details.getSequenceNo());
						pstmt.setString(21, details.getEngineType());
						pstmt.setString(22, details.getMissionType());
						pstmt.setString(23, details.getDriveAxleType());
						pstmt.setString(24, details.getBodyType());
						pstmt.setString(25, details.getTitle());
						pstmt.setString(26, details.getDisplayOrderLevel1Name());
						pstmt.setString(27, details.getDisplayOrderLevel2Name());
						pstmt.setString(28, details.getDisplayOrderLevel3Name());
						pstmt.setString(29, details.getDisplayOrderLevel4Name());
						pstmt.setString(30, details.getDisplayOrderLevel5Name());
						pstmt.setString(31, details.getDisplayOrderLevel6Name());
						pstmt.addBatch();

						details = null;
					}
					pstmt.executeBatch();
					pstmt.close();pstmt=null;
					// commit the transaction
					conn.commit();
					insertCDDataSql = null;
				}
				catch(Exception e)
				{
					// rollBack the transaction
					try
					{
						conn.rollback();
					}
					catch(SQLException e1)
					{
						Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e1);
					}
					Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
					writer = new StringWriter();
					print = new PrintWriter(writer);
					e.printStackTrace(print);

					details = null;
					for(int b=0;b<dataToBeSaved.size();b++)
					{
						details = (CDProcessingDetails)dataToBeSaved.get(b);
						// SET FAILURE AND REASON FOR FAILURE
						details.setProcessingStatus("FAILURE");
						details.setErrorCode(e.getMessage());
						details.setErrorMessage(writer.toString());
					}
					// set writer & print to null
					writer = null;
					// set print to null
					print = null;
				}
			}
			cdProcessingSourceFileName = null;

			/*
			 * UPDATE THE PROCESSING STATUS OF DATA TO BE SAVED IN SUBLIST
			 */
			CDProcessingDetails subListDetails = null;
			details = null;
			for(int a=0;a<childList.size();a++)
			{	
				subListDetails = (CDProcessingDetails)childList.get(a);
				details = null;
				for(int b=0;b<dataToBeSaved.size();b++)
				{
					details = (CDProcessingDetails)dataToBeSaved.get(b);
					if(details.getSrNo()==subListDetails.getSrNo())
					{
						subListDetails.setErrorCode(details.getErrorCode());
						subListDetails.setProcessingStatus(details.getProcessingStatus());
						subListDetails.setErrorMessage(details.getErrorMessage());
						break;
					}
					details = null;
				}
				subListDetails = null;
			}
			details = null;
			subListDetails = null;
			dataToBeSaved = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
			writer = new StringWriter();
			print = new PrintWriter(writer);
			e.printStackTrace(print);

			details = null;
			for(int b=0;b<childList.size();b++)
			{
				details = (CDProcessingDetails)childList.get(b);
				// SET FAILURE AND REASON FOR FAILURE
				details.setProcessingStatus("FAILURE");
				details.setErrorCode(e.getMessage());
				details.setErrorMessage(writer.toString());
			}
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
			details = null;
		}
		finally
		{
			try
			{
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "processCDDataInformation()", e);
			}
			//			checkFormultiplePaths = null;
			pathToBeReplaced = null;
		}
		return childList;
	}
	
	/**
	 * Function will get The WMI Code on the basis of CARLINE CODE & LOCALE & VDS CODE IS REQUIRED
	 * @param list
	 * @param locale
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<VinDetails> getWMICodeOnCarlineAndLocale(ArrayList<VinDetails> list, String locale) throws SQLException
	{
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs =null;
		try
		{
			if(null!=list && list.size()>0 && null!=locale && !"".equals(locale))
			{
				conn = getConnection();
				
				/*
				 * REMOVE MODEL TYPE DEPENDENCY
				 * USE CARLINE CODE + VDS CODE TO IDENTIFY WMI
				 */
//				if(modelType.trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new")))
//				{
//					modelType="New";
//				}
//				else if(modelType.trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.old")))
//				{
//					modelType="Old";
//				}
//				
				String sql="";
				VinDetails details = new  VinDetails();
				for(int a=0;a<list.size();a++)
				{
					details = (VinDetails)list.get(a);
					sql="SELECT MDM_VIN_WMI_CODE FROM gms3_mdm_vin_detail WHERE "
							+ " TRIM(LOWER(MDM_CRLN_CODE))='"+details.getCarlineCode().trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(MDM_VIN_VDS_CODE))='"+details.getVdsCode().trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(MDM_VIN_VIS_START_RANGE))='"+details.getVisStartRange().trim().toLowerCase()+"' "
									+ " AND TRIM(LOWER(MDM_VIN_VIS_END_RANGE))='"+details.getVisEndRange().trim().toLowerCase()+"' "
									+ " AND MDM_VIN_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
					logger.info("getWMICodeOnCarlineAndLocale :: Sql :: > " + sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if(rs.next())
					{
						details.setWmiCode(rs.getString("MDM_VIN_WMI_CODE"));
					}
					sql = null;
					rs.close();rs=null;
					stmt.close();stmt=null;
				}
				sql=null;
				details=null;
			}
			else
			{
				logger.info("getWMICodeOnCarlineAndLocale :: VIN List / Locale  as Paramters are null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getWMICodeOnCarlineAndLocale()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			locale =null;
		}
		return list;
	}
	
	/**
	 * Function will return all the VIN LIST FOR ENGINE / MODEL
	 * @param langCode
	 * @param bookCode
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<VINEntFileDetails> getMasterVinDetailsList(String langCode, String bookCode, String type,Connection conn, String closeConnection) throws SQLException 
	{
		logger.info("getMasterVinDetailsList :: Method Starts.");
		ArrayList<VINEntFileDetails> vinMasterList = new ArrayList<VINEntFileDetails>();
		Statement stmt  = null;
		ResultSet rs = null;
		try
		{
			// REMOVE MODEL TYPE CONDITION.
			if(null!=langCode && !"".equals(langCode) && null!=bookCode && !"".equals(bookCode) 
					&& null!=type &&  !"".equals(type))
			{
				/*
				 * CHECK HERE, IF MODEL TYPE IS NEWM - then use New
				 * IF MODEL TYPE IS OLDM - then use Old
				 * Else use as it is
				 * 
				 * modelType Condition Removed
				 */
//				if(modelType.trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
//				{
//					modelType ="New";
//				}
//				else if(modelType.trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.old").trim().toLowerCase()))
//				{
//					modelType ="Old";
//				}
				
				try
				{
					if(null==conn || conn.isClosed())
					{
						conn = getConnection();
					}
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getMasterVinDetailsList", e);
				}
				ArrayList<String> typeCodesList =new ArrayList<String>();
				String sql="";
				if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) )
				{
					/*
					 * Fetch all the Engine TypeList Code
					 */
					sql ="SELECT A.MDM_ET_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_engine_type A,gms3_mdm_engine_book B, "
							+ "gms3_mdm_manual_language C WHERE A.MDM_EB_ID=B.MDM_EB_ID AND A.MDM_ML_ID =C.MDM_ML_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
											+ "AND TRIM(LOWER(B.MDM_EB_CODE))='"+bookCode.trim().toLowerCase()+"' "
							+ "AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				else if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
						|| type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
				{
					sql ="SELECT A.MDM_TRANS_TYPE_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_trans_type A,gms3_mdm_trans_book B,"
							+ "gms3_mdm_manual_language C WHERE A.MDM_TRANSBK_ID=B.MDM_TRANSBK_ID AND "
							+ " A.MDM_ML_ID =C.MDM_ML_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
								+ "AND TRIM(LOWER(B.MDM_TRANSBK_CODE))='"+bookCode.trim().toLowerCase()+"'"
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				
				logger.info("getMasterVinDetailsList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				String typeCode=null;
				while(rs.next())
				{
					typeCode= rs.getString("TYPE_CODE");
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
				typeCode=null;
				
				/*
				 * Now Fetch all the VIN DATA MAPPED WITH EACH TYPE CODE
				 */
				if(null!=typeCodesList && typeCodesList.size()>0)
				{
					String processingCode=null;
					String fetchVinSql=null;
					String codeToCheck="";
					String[] tokens= null;
					VINEntFileDetails details=new VINEntFileDetails();
					for(int a=0;a<typeCodesList.size();a++)
					{
						processingCode=String.valueOf(typeCodesList.get(a));
						/*
						 * IF TYPE IS ENGINE - check for MDM_VIN_ENGINE_CODE
						 * ELSE Check for MDM_VIN_TRANSMISSION_CODE
						 */
//						String fetchVinSql = "SELECT A.*  FROM gms3_mdm_vin_detail A, gms3_mdm_carline_codes B "
//								+ "  WHERE  A.MDM_CRLN_CODE = B.MDM_CRLN_CODE AND "
//								+ " A.MDM_VIN_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+ "' "
//								+ " AND TRIM(LOWER(A.MDM_ML_LANG_CODE))='" 	+ langCode.trim().toLowerCase()	+ "' "
//								+ " AND TRIM(LOWER(B.MDM_CRLN_MODEL_TYPE)) = '"+modelType.trim().toLowerCase()+"' ";
						fetchVinSql = "SELECT *  FROM gms3_mdm_vin_detail A "
								+ "  WHERE MDM_VIN_FLAG='"+ ApplicationProperties.getProperty("flag.value.active")+ "' "
								+ " AND TRIM(LOWER(MDM_ML_LANG_CODE))='" 	+ langCode.trim().toLowerCase()	+ "' ";
						logger.info("getMasterVinDetailsList :: fetchVinSql :: > " + fetchVinSql);
						stmt = conn.createStatement();
						rs=  stmt.executeQuery(fetchVinSql);
						while(rs.next())
						{
							codeToCheck="";
							if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
									type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) )
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
								tokens = codeToCheck.split(",");
								if(null!=tokens  && tokens.length>0)
								{
									for(int t=0;t<tokens.length;t++)
									{
										if(processingCode.trim().toLowerCase().equals(tokens[t].trim().toLowerCase()))
										{
											// add VIN Details to MASTER LIST
											details = new VINEntFileDetails();
											
											/*
											 * ADD REGIONAL & ENGLISH NAME AS WELL WITH VIN
											 */
											if(null!=rs.getString("MDM_CRLN_NAME_ENG_LANG"))
											{
												details.setCarlineEngName(rs.getString("MDM_CRLN_NAME_ENG_LANG").trim());
											}
											if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG"))
											{
												details.setCarlineRegName(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG").trim());
											}
											
											details.setVinCarline(rs.getString("MDM_CRLN_CODE"));
											if(null!=details.getVinCarline() && !"".equals(details.getVinCarline())
													&& !"null".equals(details.getVinCarline().trim().toLowerCase()))
											{
												details.setVinCarline(details.getVinCarline().trim());
											}
											details.setVinWMI(rs.getString("MDM_VIN_WMI_CODE"));
											if(null!=details.getVinWMI() && !"".equals(details.getVinWMI()) 
													&& !"null".equals(details.getVinWMI().trim().toLowerCase()))
											{
												details.setVinWMI(details.getVinWMI().trim());
											}
											details.setVinVDS(rs.getString("MDM_VIN_VDS_CODE"));
											if(null!=details.getVinVDS() && !"".equals(details.getVinVDS()) 
													&& !"null".equals(details.getVinVDS().trim().toLowerCase()))
											{
												details.setVinVDS(details.getVinVDS().trim());
											}
											details.setVinStartRange(rs.getString("MDM_VIN_VIS_START_RANGE"));
											if(null!=details.getVinStartRange() && !"".equals(details.getVinStartRange()) 
													&& !"null".equals(details.getVinStartRange().trim().toLowerCase()))
											{
												details.setVinStartRange(details.getVinStartRange().trim());
											}
											details.setVinEndRange(rs.getString("MDM_VIN_VIS_END_RANGE"));
											if(null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()) 
													&& !"null".equals(details.getVinEndRange().trim().toLowerCase()))
											{
												details.setVinEndRange(details.getVinEndRange().trim());
											}
											
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
					processingCode=null;
					fetchVinSql=null;
					codeToCheck=null;
					tokens= null;
					details=null;
				}
			}
			else
			{
				logger.info("getMasterVinDetailsList :: Language Code /  Book Code / Type as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getMasterVinDetailsList", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=closeConnection && closeConnection.equals("Y"))
			{
				if(null!=conn)
				{
					conn.close();
				}
			}
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
	public ArrayList<String> getEngineMissionTypeList(String langCode, String bookCode, String type) throws SQLException 
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
				conn = getConnection();
				String sql="";
				if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
						type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) )
				{
					/*
					 * Fetch all the Engine TypeList Code
					 */
					sql ="SELECT A.MDM_ET_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_engine_type A,gms3_mdm_engine_book B, "
							+ "gms3_mdm_manual_language C WHERE A.MDM_EB_ID=B.MDM_EB_ID AND A.MDM_ML_ID =C.MDM_ML_ID "
							+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(B.MDM_EB_CODE))='"+bookCode.trim().toLowerCase()+"' "
							+ "AND A.MDM_ET_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				else if(type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
						|| type.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
				{
					sql ="SELECT A.MDM_TRANS_TYPE_TYPE_CODE AS TYPE_CODE FROM gms3_mdm_trans_type A,gms3_mdm_trans_book B, "
							+ "gms3_mdm_manual_language C WHERE A.MDM_TRANSBK_ID=B.MDM_TRANSBK_ID AND "
							+ " A.MDM_ML_ID =C.MDM_ML_ID AND TRIM(LOWER(C.MDM_ML_LANG_CODE)) ='"+langCode.trim().toLowerCase()+"' "
							+ " AND TRIM(LOWER(B.MDM_TRANSBK_CODE))='"+bookCode.trim().toLowerCase()+"'"
							+ " AND A.MDM_TRANS_TYPE_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
				}
				
				logger.info("getEngineMissionTypeList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				String typeCode=null;
				while(rs.next())
				{
					typeCode= rs.getString("TYPE_CODE");
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
				typeCode=null;
			}
			else
			{
				logger.info("getEngineMissionTypeList :: Language Code /  Book Code / Type as Parameter is null. Returning null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getEngineMissionTypeList", e);
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


	
	public static ArrayList<CDProcessingDetails> getDisplayOrderNameForCDProcessing(ArrayList<CDProcessingDetails> list, String modelType, String localeCode, String identifyName) throws SQLException
	{
		/*
		 * NOTHING TO LOOK UP ANY MORE. The text file used to carry display order CODES and this
		 * method found the NAME of each one in gms3_mdm_display_order. The file now carries the
		 * NAMES themselves, which the parser puts straight into the name fields of CDProcessingDetails.
		 */
		return list;
	}
	
	public static ArrayList<DisplayOrderDetails> getDisplayOrderNameForDisplayOrderProcessing(ArrayList<DisplayOrderDetails> list, String modelType, String localeCode, String identifyName) throws SQLException
	{
		/*
		 * NOTHING TO LOOK UP ANY MORE. The text file used to carry display order CODES and this
		 * method found the NAME of each one in gms3_mdm_display_order. The file now carries the
		 * NAMES themselves, which the parser puts straight into the name fields of DisplayOrderDetails.
		 */
		return list;
	}
	
	private void processCVCCategoryInformation(ContentDetails contentDetails, Connection conn,String tableName) throws SQLException
	{
		Statement stmt  = null;
		PreparedStatement pstmt=null;

		/*
		 * FIRST DELETE ALL THE INFORMATION FOR THE DOCUMENT
		 * INSERT NEW DETAILS.
		 */
		String deleteSql="DELETE FROM "+ tableName+" WHERE DC_IM_DOC_ID='"+contentDetails.getImDocumentId()+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		if(null!=contentDetails.getCvcCategoryList() && contentDetails.getCvcCategoryList().size()>0)
		{
			CVCCategoryDetails details = new CVCCategoryDetails();
			String insertCVCSql="";
			for(int i=0;i<contentDetails.getCvcCategoryList().size();i++)
			{
				details = (CVCCategoryDetails)contentDetails.getCvcCategoryList().get(i);
				
				/*
				 * CHECK WHETHER CVC INFO EXISTS OR NOT
				 * IF NOT THEN INSERT ESLE SKIP
				 */
				pstmt=  null;
				insertCVCSql="INSERT INTO "+tableName+" (DC_SCHEDULE_ID,DC_IM_DOC_ID,DC_CVC_CAT_CODE,"
						+ " DC_CVC_SYM_CODE, "
						+ "DC_CVC_SUBSYM_CODE,DC_CVC_CON_CODE,DC_CVC_CREATED_TMSTP,DC_SOURCE_NETWORK_LOC,DC_CVC_REFKEY) "
						+ "VALUES(?,?,?,?,?,?,?,?,?)";
				pstmt = conn.prepareStatement(insertCVCSql);
				pstmt.setLong(1, new Long(contentDetails.getScheduleId()).longValue());
				pstmt.setString(2, contentDetails.getImDocumentId());
				pstmt.setString(3, details.getCategoryCode());
				pstmt.setString(4, details.getSymptomCode());
				pstmt.setString(5, details.getSubSymptomCode());
				pstmt.setString(6, details.getConditionCode());
				pstmt.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));
				pstmt.setString(8, contentDetails.getFilePath());
				pstmt.setString(9, details.getCvcRefKey());
				pstmt.executeUpdate();
				pstmt.close();
				pstmt= null;
				insertCVCSql =null;
				details = null;
			}
			insertCVCSql =null;
			details = null;
		}
	}

	private void processViewContentData(List<MCViewContentDetails> viewContentList,	Connection conn,String locale, List<ContentDetails> docsList) throws SQLException
	{
		PreparedStatement pstmt = null;
		if(null!=docsList && docsList.size()>0)
		{
			ContentDetails contentDetails = null;
			String key = "";
			for(int a=0;a<docsList.size();a++)
			{
				contentDetails = (ContentDetails)docsList.get(a);
				key+="'"+contentDetails.getDocumentId()+"'";
				if(a!=docsList.size()-1)
				{
					key+=",";
				}
				contentDetails = null;
			}

			if(null!=key & !"".equals(key))
			{
				String sql="DELETE FROM gms3_vc_japan_vin_details WHERE VC_VIN_LOCALE = '"+ (null == locale ? "" : locale.trim().replace("-", "_").replace("'", "''"))+ "' AND VC_VIN_DOCUMENT_ID IN ("+key+")"; // with the locale the (locale, document id) index is used - without it every delete reads the whole table
				pstmt=  conn.prepareStatement(sql);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql  =null;
			}
			key = null;
		}
		
		if(null!=viewContentList && viewContentList.size()>0)
		{
			MCViewContentDetails details = null;
			logger.info("processViewContentData :: Total VIEW COTNENT DATA RECORDS Found ARE :: > "+ viewContentList.size());
			locale=  locale.replace("-", "_");
			String sql="";
			
			int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
			ArrayList<List<MCViewContentDetails>> partitions = new ArrayList<List<MCViewContentDetails>>();
			for (int i=0; i<viewContentList.size(); i += partitionSize) {
				partitions.add(viewContentList.subList(i, Math.min(i + partitionSize, viewContentList.size())));
			}
			if(null!=partitions && partitions.size()>0)
			{
				for(List<MCViewContentDetails> subList : partitions)
				{
					if(null!=subList && subList.size()>0)
					{
						details = new MCViewContentDetails();
						// SAVE
						sql = "INSERT INTO gms3_vc_japan_vin_details (VC_VIN_LOCALE, VC_VIN_MODEL, VC_VIN_CARLINE_CODE, VC_VIN_MODEL_TYPE, "
								+ " VC_VIN_WMI_CODE, VC_VIN_VDS_CODE, VC_VIN_VIS_START_RANGE, VC_VIN_VIS_END_RANGE, VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE, "
								+ " VC_VIN_DOCUMENT_SUBTYPE, VC_VIN_CATEGORY_CODE_1, VC_VIN_CATEGORY_NAME_1, VC_VIN_CATEGORY_CODE_2, VC_VIN_CATEGORY_NAME_2, "
								+ " VC_VIN_CATEGORY_CODE_3, VC_VIN_CATEGORY_NAME_3, VC_VIN_DISP_CODE_1, VC_VIN_DISP_NAME_1, VC_VIN_DISP_CODE_2, VC_VIN_DISP_NAME_2, "
								+ " VC_VIN_DISP_CODE_3, VC_VIN_DISP_NAME_3, VC_VIN_DISP_CODE_4, VC_VIN_DISP_NAME_4, VC_VIN_DISP_CODE_5, VC_VIN_DISP_NAME_5, "
								+ " VC_VIN_DISP_CODE_6, VC_VIN_DISP_NAME_6, VC_VIN_IMDOC_CREATE_DATE, VC_VIN_IMDOC_MODIFIED_DATE, VC_VIN_DOCUMENT_TITLE, "
								+ " VC_VIN_DOCUMENT_STATUS, VC_VIN_DISPORD_SEQ_NO, VC_VIN_CARLINE_NAME, VC_VIN_DOCUMENT_TYPE_NAME, VC_VIN_DOCUMENT_SUBTYPE_NAME,"
								+ " VC_VIN_ENGINE_BOOK_CODE, VC_VIN_ENGINE_BOOK_NAME, VC_VIN_MISSION_BOOK_CODE,VC_VIN_MISSION_BOOK_NAME, VC_VIN_CREATED_TMSTP,VC_VIN_DESCRIPTION, VC_CONTENT_STATUS ) "
								// VC_CONTENT_STATUS - every row DMT writes is DRAFT; the Publish Content job changes it to PUBLISHED
								+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"
								+ ApplicationProperties.getProperty("view.content.status.draft").replace("'", "''") + "')";
						pstmt = conn.prepareStatement(sql);
						for(int t=0;t<subList.size();t++)
						{
							details= (MCViewContentDetails)subList.get(t);
							if(null!=locale && !"".equals(locale))
							{
								locale = locale.trim();
							}
							if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
							{
								details.setCarlineNameEng(details.getCarlineNameEng().trim());
							}
							if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
							{
								details.setCarlineCode(details.getCarlineCode().trim());
							}
							if(null!=details.getModelType() && !"".equals(details.getModelType()))
							{
								details.setModelType(details.getModelType().trim());
							}
							if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
							{
								details.setWmiCode(details.getWmiCode().trim());
							}
							if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
							{
								details.setVdsCode(details.getVdsCode().trim());
							}
							if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
							{
								details.setVisStartRange(details.getVisStartRange().trim());
							}
							if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
							{
								details.setVisEndRange(details.getVisEndRange().trim());
							}
							if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
							{
								details.setDocumentId(details.getDocumentId().trim());
							}
							if(null!=details.getManualType() && !"".equals(details.getManualType()))
							{
								details.setManualType(details.getManualType().trim());
							}
							if(null!=details.getEsiCatLevel1Code() && !"".equals(details.getEsiCatLevel1Code()))
							{
								details.setEsiCatLevel1Code(details.getEsiCatLevel1Code().trim());
							}
							if(null!=details.getEsiCatLevel1Name() && !"".equals(details.getEsiCatLevel1Name()))
							{
								details.setEsiCatLevel1Name(details.getEsiCatLevel1Name().trim());
							}
							if(null!=details.getEsiCatLevel2Code() && !"".equals(details.getEsiCatLevel2Code()))
							{
								details.setEsiCatLevel2Code(details.getEsiCatLevel2Code().trim());
							}
							if(null!=details.getEsiCatLevel2Name() && !"".equals(details.getEsiCatLevel2Name()))
							{
								details.setEsiCatLevel2Name(details.getEsiCatLevel2Name().trim());
							}
							if(null!=details.getEsiCatLevel3Code() && !"".equals(details.getEsiCatLevel3Code()))
							{
								details.setEsiCatLevel3Code(details.getEsiCatLevel3Code().trim());
							}
							if(null!=details.getEsiCatLevel3Name() && !"".equals(details.getEsiCatLevel3Name()))
							{
								details.setEsiCatLevel3Name(details.getEsiCatLevel3Name().trim());
							}
							
							if(null!=details.getDisplayOrderCodeLevel1() && !"".equals(details.getDisplayOrderCodeLevel1()))
							{
								details.setDisplayOrderCodeLevel1(details.getDisplayOrderCodeLevel1().trim());
							}
							if(null!=details.getDisplayOrderCodeLevel2() && !"".equals(details.getDisplayOrderCodeLevel2()))
							{
								details.setDisplayOrderCodeLevel2(details.getDisplayOrderCodeLevel2().trim());
							}
							if(null!=details.getDisplayOrderCodeLevel3() && !"".equals(details.getDisplayOrderCodeLevel3()))
							{
								details.setDisplayOrderCodeLevel3(details.getDisplayOrderCodeLevel3().trim());
							}
							if(null!=details.getDisplayOrderCodeLevel4() && !"".equals(details.getDisplayOrderCodeLevel4()))
							{
								details.setDisplayOrderCodeLevel4(details.getDisplayOrderCodeLevel4().trim());
							}
							if(null!=details.getDisplayOrderCodeLevel5() && !"".equals(details.getDisplayOrderCodeLevel5()))
							{
								details.setDisplayOrderCodeLevel5(details.getDisplayOrderCodeLevel5().trim());
							}
							if(null!=details.getDisplayOrderCodeLevel6() && !"".equals(details.getDisplayOrderCodeLevel6()))
							{
								details.setDisplayOrderCodeLevel6(details.getDisplayOrderCodeLevel6().trim());
							}
							
							if(null!=details.getDisplayOrderNameLevel1() && !"".equals(details.getDisplayOrderNameLevel1()))
							{
								details.setDisplayOrderNameLevel1(details.getDisplayOrderNameLevel1().trim());
							}
							if(null!=details.getDisplayOrderNameLevel2() && !"".equals(details.getDisplayOrderNameLevel2()))
							{
								details.setDisplayOrderNameLevel2(details.getDisplayOrderNameLevel2().trim());
							}
							if(null!=details.getDisplayOrderNameLevel3() && !"".equals(details.getDisplayOrderNameLevel3()))
							{
								details.setDisplayOrderNameLevel3(details.getDisplayOrderNameLevel3().trim());
							}
							if(null!=details.getDisplayOrderNameLevel4() && !"".equals(details.getDisplayOrderNameLevel4()))
							{
								details.setDisplayOrderNameLevel4(details.getDisplayOrderNameLevel4().trim());
							}
							if(null!=details.getDisplayOrderNameLevel5() && !"".equals(details.getDisplayOrderNameLevel5()))
							{
								details.setDisplayOrderNameLevel5(details.getDisplayOrderNameLevel5().trim());
							}
							if(null!=details.getDisplayOrderNameLevel6() && !"".equals(details.getDisplayOrderNameLevel6()))
							{
								details.setDisplayOrderNameLevel6(details.getDisplayOrderNameLevel6().trim());
							}
							
							if(null!=details.getTitle() && !"".equals(details.getTitle()))
							{
								details.setTitle(details.getTitle().trim());
							}
							if(null!=details.getDisplayOrderSequenceNo() && !"".equals(details.getDisplayOrderSequenceNo()))
							{
								details.setDisplayOrderSequenceNo(details.getDisplayOrderSequenceNo().trim());
							}
							if(null!=details.getManualTypeLabel() && !"".equals(details.getManualTypeLabel()))
							{
								details.setManualTypeLabel(details.getManualTypeLabel());
							}
							if(null!=details.getCarlineNameReg() && !"".equals(details.getCarlineNameReg()))
							{
								details.setCarlineNameReg(details.getCarlineNameReg().trim());
							}
							if(null!=details.getEngineBookCode() && !"".equals(details.getEngineBookCode()))
							{
								details.setEngineBookCode(details.getEngineBookCode().trim());
							}
							if(null!=details.getEngineBookName() && !"".equals(details.getEngineBookName()))
							{
								details.setEngineBookName(details.getEngineBookName().trim());
							}
							if(null!=details.getMissionBookCode() && !"".equals(details.getMissionBookCode()))
							{
								details.setMissionBookCode(details.getMissionBookCode().trim());
							}
							if(null!=details.getMissionBookName() && !"".equals(details.getMissionBookName()))
							{
								details.setMissionBookName(details.getMissionBookName().trim());
							}
							if(null!=details.getDescription() && !"".equals(details.getDescription()))
							{
								details.setDescription(details.getDescription().trim());
							}
							/*
							 * NOW SAVE THE NEW DOCUMENT DETAILS
							 */
							
							pstmt.setString(1, locale);
							pstmt.setString(2, details.getCarlineNameEng());
							pstmt.setString(3, details.getCarlineCode());
							pstmt.setString(4, details.getModelType());
							pstmt.setString(5, details.getWmiCode());
							pstmt.setString(6, details.getVdsCode());
							pstmt.setString(7, details.getVisStartRange());
							pstmt.setString(8, details.getVisEndRange());
							pstmt.setString(9, details.getDocumentId());
							pstmt.setString(10, details.getManualType());
							pstmt.setString(11, "");
							pstmt.setString(12, details.getEsiCatLevel1Code());
							pstmt.setString(13, details.getEsiCatLevel1Name());
							pstmt.setString(14, details.getEsiCatLevel2Code());
							pstmt.setString(15, details.getEsiCatLevel2Name());
							pstmt.setString(16, details.getEsiCatLevel3Code());
							pstmt.setString(17, details.getEsiCatLevel3Name());
							
							pstmt.setString(18, details.getDisplayOrderCodeLevel1());
							pstmt.setString(19, details.getDisplayOrderNameLevel1());
							pstmt.setString(20, details.getDisplayOrderCodeLevel2());
							pstmt.setString(21, details.getDisplayOrderNameLevel2());
							pstmt.setString(22, details.getDisplayOrderCodeLevel3());
							pstmt.setString(23, details.getDisplayOrderNameLevel3());
							pstmt.setString(24, details.getDisplayOrderCodeLevel4());
							pstmt.setString(25, details.getDisplayOrderNameLevel4());
							pstmt.setString(26, details.getDisplayOrderCodeLevel5());
							pstmt.setString(27, details.getDisplayOrderNameLevel5());
							pstmt.setString(28, details.getDisplayOrderCodeLevel6());
							pstmt.setString(29, details.getDisplayOrderNameLevel6());
							
							pstmt.setTimestamp(30, details.getImDocCreateDate());
							pstmt.setTimestamp(31, details.getImDocModifiedDate());
							
							pstmt.setString(32, details.getTitle());
							pstmt.setString(33, "1");
							pstmt.setString(34, details.getDisplayOrderSequenceNo());
							pstmt.setString(35, details.getCarlineNameReg());
							pstmt.setString(36, details.getManualTypeLabel());
							pstmt.setString(37, "");
							pstmt.setString(38, details.getEngineBookCode());
							pstmt.setString(39, details.getEngineBookName());
							pstmt.setString(40, details.getMissionBookCode());
							pstmt.setString(41, details.getMissionBookName());
							
							pstmt.setTimestamp(42, new Timestamp(new Date().getTime()));
							pstmt.setString(43, details.getDescription());
							pstmt.addBatch();
							details = null;
						}
						pstmt.executeBatch();
						pstmt.close();pstmt = null;
						sql = null;
						details = null;
					}
					subList  = null;
				}
			}
			partitions = null;
			details = null;
			sql=null;
			
			/*
			 * BEFORE ADDING THE VIEW CONTENT DATA OF THE DOCUMENT TO FINAL VIEW CONTENT LIST
			 * CHECK IF DOCUMENT & LOCALE ALREADY EXISTS, THEN SKIP IT
			 * 
			 NO NEED OF SKIPPING AS MULTIPLE DISPLAY ORDER CAN COME FOR SAME DOCUMENT AND MULTIPLE ENTRIES NEEDS TO BE MADE INSIDE IT
			 */
			if(null!=viewContentList && viewContentList.size()>0)
			{
				/*
				 * ADD DOC DETAILS TO VIEW CONTENT DATA LIST FOR PRINTING IN REPORTS.
				 */
				if(null==viewContentDataList || viewContentDataList.size()<=0)
				{
					viewContentDataList = new ArrayList<MCViewContentDetails>();
				}
				viewContentDataList.addAll(viewContentList);
			}
			viewContentList = null;
		}
		viewContentList = null;
		locale= null;
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
					+ " TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.replace("_", "-").trim().toLowerCase()+"' "
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
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getESICategoryMasterList()", e);
		}
		finally
		{
			locale = null;
		}
		return esiCatList;
	}
	
	private static List<SelectItemDetails> getEngineMissionBookDetails(List<String> typesList, Connection conn,String type) 
	{
		List<SelectItemDetails> itemsList = null;
		Statement stmt =null;
		ResultSet rs =null;
		try
		{
			String[] tokens=null;
			String key = "";
			for(int a=0;a<typesList.size();a++)
			{
				tokens = typesList.get(a).toString().split(",");
				if(null!=tokens && tokens.length>0)
				{
					key+="'"+tokens[0]+"',"; 
				}
				tokens = null;
			}

			if(null!=key && !"".equals(key))
			{
				if(key.endsWith(","))
				{
					key = key.substring(0, key.length()-1);
				}

				if(null!=key && !"".equalsIgnoreCase(key))
				{
					String locale = ApplicationProperties.getProperty("ja-jp");
					locale = locale.replace("_", "-");
					key = key.trim();
					SelectItemDetails si = null;
					String sql = "";
					if(type.equals("ENG"))
					{
						sql="SELECT A.MDM_ET_TYPE_CODE as TYPE_CODE,B.MDM_EB_CODE AS BOOK_CODE,B.MDM_EB_NAME_REGIONAL_LANG AS BOOK_NAME "
								+ " FROM gms3_mdm_engine_type A, gms3_mdm_engine_book B, gms3_mdm_manual_language C "
								+ " WHERE TRIM(LOWER(A.MDM_ET_TYPE_CODE)) IN ("+key.trim().toLowerCase()+") AND A.MDM_EB_ID=B.MDM_EB_ID AND A.MDM_ML_ID=C.MDM_ML_ID"
								+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"'";
					}
					else if(type.equals("MISSION"))
					{
						sql="SELECT A.MDM_TRANS_TYPE_TYPE_CODE AS TYPE_CODE,B.MDM_TRANSBK_CODE AS BOOK_CODE, B.MDM_TRANSBK_NAME_REGIONAL_LANG AS BOOK_NAME "
								+ " FROM gms3_mdm_trans_type A, gms3_mdm_trans_book B, gms3_mdm_manual_language C "
								+ " WHERE TRIM(LOWER(A.MDM_TRANS_TYPE_TYPE_CODE)) IN ("+key.trim().toLowerCase()+") AND A.MDM_TRANSBK_ID=B.MDM_TRANSBK_ID AND A.MDM_ML_ID=C.MDM_ML_ID"
								+ " AND TRIM(LOWER(C.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"'";
					}
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						si = new SelectItemDetails();
						si.setCode(rs.getString("TYPE_CODE").trim());
						// BOOK CODE
						si.setLabel(rs.getString("BOOK_CODE").trim());
						// BOOK NAME
						si.setValue(rs.getString("BOOK_NAME").trim());
						//						cDetails.setEngineBookCode(rs.getString("MDM_EB_CODE").trim());
						//						cDetails.setEngineBookName(rs.getString("MDM_EB_NAME_REGIONAL_LANG").trim());
						if(null==itemsList || itemsList.size()<=0)
						{
							itemsList = new ArrayList<SelectItemDetails>();
						}
						itemsList.add(si);
						si = null;
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
					sql = null;
					locale= null;
					si = null;
				}
			}
			key = null;
			tokens=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "getEngineMissionBookDetails()", e);
		}
		return itemsList;
	}
	
	public boolean deleteMaterialFolderDocumentsDataFromDispOrdAndVC(String materialFolderPath, String locale,String modelType)
	{
		boolean bool = true;
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=materialFolderPath && !"".equals(materialFolderPath) && null!=locale && !"".equals(locale))
			{
				conn = getConnection();
				if(!materialFolderPath.endsWith("\\"))
				{
					materialFolderPath+="\\";
				}
				List<String> documentIdList = new ArrayList<String>();
				
				String tableName="";
				String displayOrderTableName="";
				String viewContentTableName="gms3_vc_japan_vin_details";
				if(null!=modelType && modelType.trim().toLowerCase().
						equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase()))
				{
					// NEW MODELS TABLE
					tableName = "gms3_dmt_mc_newm_imdoc";
					displayOrderTableName="gms3_dmt_mc_newm_dispord";
				}
				else
				{
					// NORMAL MODELS TABLE
					tableName = "gms3_dmt_mc_imdoc";
					displayOrderTableName="gms3_dmt_mc_dispord";
				}
				
				String sql="SELECT DC_IM_DOC_ID FROM "+tableName+" WHERE TRIM(LOWER(DC_SOURCE_NETWORK_LOC)) LIKE '"+materialFolderPath.trim().toLowerCase()+"%'";
				logger.info("deleteMaterialFolderDocumentsDataFromDispOrdAndVC :: Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					documentIdList.add(rs.getString("DC_IM_DOC_ID"));
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;

				// set AUTO COMMIT TO FALSE NOW, AS DELETE OPERATION IS TO BE CARRIED OUT
				conn.setAutoCommit(false);
				int partitionSize=Integer.parseInt(com.mazda.gms3.dmt.kapture.KaptureApiClient.require("db.write.batch.size"));
				ArrayList<List<String>> partitions = new ArrayList<List<String>>();
				for (int i=0; i<documentIdList.size(); i += partitionSize) {
					partitions.add(documentIdList.subList(i, Math.min(i + partitionSize, documentIdList.size())));
				}

				if(null!=partitions && partitions.size()>0)
				{
					for(List<String> subList : partitions)
					{
						if(null!=subList && subList.size()>0)
						{
							String deleteDisplayorderSql = "DELETE FROM "+displayOrderTableName+" WHERE DC_IM_DOC_ID=?";
							pstmt =conn.prepareStatement(deleteDisplayorderSql);
							for(int e=0;e<subList.size();e++)
							{
								pstmt.setString(1, subList.get(e).toString());
								pstmt.addBatch();
							}
							pstmt.executeBatch();
							pstmt.close();pstmt = null;
							deleteDisplayorderSql = null;

							String deleteVCSql = "DELETE FROM "+ viewContentTableName+" WHERE VC_VIN_DOCUMENT_ID = ?";
							pstmt =conn.prepareStatement(deleteVCSql);
							for(int e=0;e<subList.size();e++)
							{
								pstmt.setString(1, subList.get(e).toString());
								pstmt.addBatch();
							}
							pstmt.executeBatch();
							pstmt.close();pstmt = null;
							deleteVCSql = null;
						}
						subList = null;
					}
				}
				partitions=  null;
				// commit transaction
				conn.commit();
			}
			else
			{
				logger.info("deleteMaterialFolderDocumentsDataFromDispOrdAndVC :: Material Folder Path as Parameter is null. Return false");
				bool = false;
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
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "deleteMaterialFolderDocumentsDataFromDispOrdAndVC()", eq);
			}
			Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "deleteMaterialFolderDocumentsDataFromDispOrdAndVC()", e);
			bool =false;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MCDocumentManagementDAO.class.getName(), "deleteMaterialFolderDocumentsDataFromDispOrdAndVC()", e);
			}
		}
		return bool;
	}
	
	private ContentDetails retrieveESICategoryLabels(ContentDetails contentDetails, List<ESICategoryDetails> esiCatList)
	{
		// SET ESI CAT FLAG TO Y -  DEFAULT VALUE
		contentDetails.setEsiCatFlag("Y");
		ESICategoryDetails details = null;
		// CHECK FOR LEVEL 1
		if(null!=contentDetails.getCategoryCode() && !"".equals(contentDetails.getCategoryCode()))
		{
			if(null!=esiCatList && esiCatList.size()>0)
			{
				details = null;
				for(int a=0;a<esiCatList.size();a++)
				{
					details = (ESICategoryDetails)esiCatList.get(a);
					if(null!=details.getCategoryLevel1Code() && !"".equals(details.getCategoryLevel1Code()) 
							&& details.getCategoryLevel1Code().trim().toLowerCase().equals(contentDetails.getCategoryCode().trim().toLowerCase()))
					{
						contentDetails.setEsiCategoryLevel1Name(details.getCategoryLevel1Name());
						break;
					}
					details = null;
				}
			}

			if(null==contentDetails.getEsiCategoryLevel1Name() || "".equals(contentDetails.getEsiCategoryLevel1Name()))
			{
				// SET ESI CAT FLAG TO N
				contentDetails.setEsiCatFlag("N");
			}

			// CHECK FOR LEVEL 2
			if(null!=contentDetails.getSubCategoryCode() && !"".equals(contentDetails.getSubCategoryCode()))
			{
				if(null!=esiCatList && esiCatList.size()>0)
				{
					details = null;
					for(int at=0;at<esiCatList.size();at++)
					{
						details = (ESICategoryDetails)esiCatList.get(at);
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

				if(null==contentDetails.getEsiCategoryLevel2Name() || "".equals(contentDetails.getEsiCategoryLevel2Name()))
				{
					// SET ESI CAT FLAG TO N
					contentDetails.setEsiCatFlag("N");
				}

				// CHECK FOR LEVEL 3
				if(null!=contentDetails.getSubSubCategoryCode() && !"".equals(contentDetails.getSubSubCategoryCode()))
				{
					if(null!=esiCatList && esiCatList.size()>0)
					{
						details = null;
						for(int aq=0;aq<esiCatList.size();aq++)
						{
							details = (ESICategoryDetails)esiCatList.get(aq);
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


					if(null==contentDetails.getEsiCategoryLevel3Name() || "".equals(contentDetails.getEsiCategoryLevel3Name()))
					{
						// SET ESI CAT FLAG TO N
						contentDetails.setEsiCatFlag("N");
					}
				}
			}
		}
		return contentDetails;
	}

	private void cleanUpOldDataForDocument(Connection conn, ContentDetails contentDetails, String tableName,String innerLinkTableName,String categoryTableName,
			String esiCatTableName,String vinTableName,String masterVINTableName, String displayOrderTableName, String cvcTableName,
			String cdProcessingTableName, boolean saveOperation, String documentId) throws Exception
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
		
		// INNER LINKS
		String deleteSql="DELETE FROM "+ innerLinkTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// MAPPED CATEGORIES
		deleteSql="DELETE FROM "+ categoryTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// ESI CATEGORY
		deleteSql="DELETE FROM "+ esiCatTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// VIN
		deleteSql="DELETE FROM "+ vinTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// MASTER VIN
		deleteSql="DELETE FROM "+ masterVINTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// DISPLAY ORDER
		deleteSql="DELETE FROM "+ displayOrderTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// CVC
		deleteSql="DELETE FROM "+ cvcTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		// CD DATA
		deleteSql="DELETE FROM "+ cdProcessingTableName+" WHERE DC_IM_DOC_ID='"+documentId+"'";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		
		String locale = contentDetails.getLocale();
		locale = locale.replace("-", "_");
		// DELETE FROM VIEW CONTENT TABLE AS WELL.
		deleteSql="DELETE FROM gms3_vc_japan_vin_details WHERE VC_VIN_DOCUMENT_ID ='"+documentId.trim()+"' "
				+ " AND VC_VIN_LOCALE='"+locale.trim()+"' ";
		stmt = conn.createStatement();
		stmt.executeUpdate(deleteSql);
		stmt.close();
		stmt=  null;
		deleteSql = null;
		locale = null;
		logger.info("cleanUpOldDataForDocument :: ------------------------------------------------------------");
	}
}