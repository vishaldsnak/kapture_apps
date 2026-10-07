package com.mazda.gms3.dmt.automation.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;

public class TransactionDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(TransactionDAO.class);
	
	public Map<String, Object> updateDocumentDetails(Map<String, Object> documentMap, String locale,String documentId, ArrayList<ExcelRowDetails> vinList)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			/*
			 * FETCH DOCUMENT VIN DETAILS FROM DMT_VIN_DETAILS BASED ON LOCALE
			 * BOTH VIN_MASTER AND VIN_DETAILS
			 * REMOVE THE EXISTING VIN DATA AND INSERT NEW VIN DATA.
			 * PREPARE THE TABLE NAMES BASED ON THE LOCALE
			 * AND UPDATE VIN DATA IN VIEW CONTENT TABLE
			 */
//			String vinMasterTable="";
//			String vinTable="";
//			String categoryTable="";
			String viewContentTable="";
			locale = locale.replace("-", "_");
			
			/*
			 * LET THE CONDITIONS REMAIN FOR MNAO & MC LOCALES FOR IDENTIFYING TABLES
			 */
			if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
			{
				// MNAO LOCALE
//				vinTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_VIN";
//				vinMasterTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_VINMASTER";
//				categoryTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_CAT";
				viewContentTable="gms3_vc_vin_details";
			}
			else if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
			{
				// MC LOCALE
//				vinTable="gms3_dmt_mc_newm_vin,gms3_dmt_mc_vin";
//				vinMasterTable="gms3_dmt_mc_newm_vinmaster,gms3_dmt_mc_vinmaster";
//				categoryTable="gms3_dmt_mc_newm_cat,gms3_dmt_mc_cat";
				viewContentTable = "gms3_vc_japan_vin_details";
			}
//			else if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
			else
			{
				// MME LOCALE
//				vinTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_NM_VIN,GMS3_DMT_"+locale.trim().toUpperCase()+"_VIN";
//				vinMasterTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_NM_VINMAST,GMS3_DMT_"+locale.trim().toUpperCase()+"_VINMAST";
//				categoryTable="GMS3_DMT_"+locale.trim().toUpperCase()+"_NM_CAT,GMS3_DMT_"+locale.trim().toUpperCase()+"_CAT";
				viewContentTable="GMS3_VC_MME_VIN_DTL_"+locale.trim().toUpperCase();
			}
			
			conn = getConnection();
			conn.setAutoCommit(false);
			/*
			 * FETCH DATA FROM ALL THE IDENTIFIED TABLES ABOVE AND START REPLACING OLD VIN VALUES BY NEW VIN VALUES
			 */
			String sql="";
//			String[] tokens=null;
			
			ExcelRowDetails vinDetails = null;
			// UPDATE IN DMT TABLES
			for(int r=0;r<vinList.size();r++)
			{
				vinDetails = (ExcelRowDetails) vinList.get(r);
				if(null!=vinDetails.getOldRefKey() && !"".equals(vinDetails.getOldRefKey()))
				{
					// VIN TABLE
//					tokens = vinTable.split(",");
//					if(null!=tokens && tokens.length>0)
//					{
//						for(int a=0;a<tokens.length;a++)
//						{
//							logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN DMT TABLE -> " + tokens[a]);
//							if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
//							{
//								// MNAO UPDATE
////								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE=?,DC_VIN_WMI=?,DC_VIN_VDS=?,DC_VIN_START=?,"
////										+ "DC_VIN_END=?, DC_VIN_REFKEY=?, DC_VIN_MODEL_YEAR=? WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_VIN_REFKEY)=?";
//								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE=?,DC_VIN_WMI=?,DC_VIN_VDS=?,DC_VIN_START=?,"
//										+ "DC_VIN_END=?, DC_VIN_REFKEY=? WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_VIN_REFKEY)=?";
//								pstmt = conn.prepareStatement(sql);
//								pstmt.setString(1, vinDetails.getNewCarlineCode());
//								pstmt.setString(2, vinDetails.getNewVdsCode());
//								pstmt.setString(3, vinDetails.getNewVisStartRange());
//								pstmt.setString(4, vinDetails.getNewVisEndRange());
//								pstmt.setString(5, vinDetails.getNewRefKey());
////								pstmt.setString(6, vinDetails.getNewYear());
//								pstmt.setString(6, documentId);
//								pstmt.setString(7, vinDetails.getOldRefKey());
//								pstmt.executeUpdate();
//								pstmt.close();pstmt=null;
//								sql=null;
//							}
//							else if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
//							{
//								// MC UPDATE
//								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE_CODE=?,DC_VIN_VDS_CODE=?,DC_VIN_VIS_START_RANGE=?,"
//										+ "DC_VIN_VIS_END_RANGE=?, DC_VIN_REFKEY=? WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_VIN_REFKEY)=?";
//								pstmt = conn.prepareStatement(sql);
//								pstmt.setString(1, vinDetails.getNewCarlineCode());
//								pstmt.setString(2, vinDetails.getNewVdsCode());
//								pstmt.setString(3, vinDetails.getNewVisStartRange());
//								pstmt.setString(4, vinDetails.getNewVisEndRange());
//								pstmt.setString(5, vinDetails.getNewRefKey());
//								pstmt.setString(6, documentId);
//								pstmt.setString(7, vinDetails.getOldRefKey());
//								pstmt.executeUpdate();
//								pstmt.close();pstmt=null;
//								sql=null;
//								
//							}
////							else if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
//							else
//							{
//								// MME UPDATE
//								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE_CODE=?,DC_VIN_WMI_CODE=?, DC_VIN_VDS_CODE=?,DC_VIN_VIS_START_RANGE=?,"
//										+ "DC_VIN_VIS_END_RANGE=?, DC_VIN_REFKEY=? WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_VIN_REFKEY)=?";
//								pstmt = conn.prepareStatement(sql);
//								pstmt.setString(1, vinDetails.getNewCarlineCode());
//								pstmt.setString(2, vinDetails.getNewWmiCode());
//								pstmt.setString(3, vinDetails.getNewVdsCode());
//								pstmt.setString(4, vinDetails.getNewVisStartRange());
//								pstmt.setString(5, vinDetails.getNewVisEndRange());
//								pstmt.setString(6, vinDetails.getNewRefKey());
//								pstmt.setString(7, documentId);
//								pstmt.setString(8, vinDetails.getOldRefKey());
//								pstmt.executeUpdate();
//								pstmt.close();pstmt=null;
//								sql=null;
//							}
//						}
//					}
//					tokens=null;
					

					// VIN MASTER TABLE
//					tokens = vinMasterTable.split(",");
//					if(null!=tokens && tokens.length>0)
//					{
//						for(int a=0;a<tokens.length;a++)
//						{
//							logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN VIN MASTER TABLE -> " + tokens[a]);
//							if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
//							{
//								// MNAO UPDATE
////								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE=?,DC_VIN_WMI=?,DC_VIN_VDS=?,DC_VIN_START=?,"
////										+ "DC_VIN_END=?, DC_VIN_REFKEY=?, DC_VIN_MODEL_YEAR=? WHERE TRIM(DC_IM_DOC_ID)=? AND "
////										+ " TRIM(LOWER(DC_VIN_CARLINE))=? AND TRIM(LOWER(DC_VIN_WMI))=? AND TRIM(LOWER(DC_VIN_VDS))=? "
////										+ "AND TRIM(LOWER(DC_VIN_START))=? AND TRIM(LOWER(DC_VIN_END))=?";
//								sql="UPDATE "+tokens[a]+" SET DC_VIN_CARLINE=?,DC_VIN_WMI=?,DC_VIN_VDS=?,DC_VIN_START=?,"
//										+ "DC_VIN_END=?, DC_VIN_REFKEY=? WHERE TRIM(DC_IM_DOC_ID)=? AND "
//										+ " TRIM(LOWER(DC_VIN_CARLINE))=? AND TRIM(LOWER(DC_VIN_WMI))=? AND TRIM(LOWER(DC_VIN_VDS))=? "
//										+ "AND TRIM(LOWER(DC_VIN_START))=? AND TRIM(LOWER(DC_VIN_END))=?";
//								pstmt = conn.prepareStatement(sql);
//								pstmt.setString(1, vinDetails.getNewCarlineCode());
//								pstmt.setString(2, vinDetails.getNewVdsCode());
//								pstmt.setString(3, vinDetails.getNewVisStartRange());
//								pstmt.setString(4, vinDetails.getNewVisEndRange());
//								pstmt.setString(5, vinDetails.getNewRefKey());
////								pstmt.setString(6, vinDetails.getNewYear());
//								pstmt.setString(6, documentId);
//								pstmt.setString(7, vinDetails.getOldCarlineCode().trim().toLowerCase());
//								pstmt.setString(8, vinDetails.getOldWmiCode().trim().toLowerCase());
//								pstmt.setString(9, vinDetails.getOldVdsCode().trim().toLowerCase());
//								pstmt.setString(10, vinDetails.getOldVisStartRange().trim().toLowerCase());
//								pstmt.setString(11, vinDetails.getOldVisEndRange().trim().toLowerCase());
//								pstmt.executeUpdate();
//								pstmt.close();pstmt=null;
//								sql=null;
//							}
////							else if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1 || 
////									ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
//							else
//							{
//								// MC OR MME UPDATE
//								sql="UPDATE "+tokens[a]+" SET DC_VM_CARLINE_CODE=?,DC_VM_WMI_CODE=?, DC_VM_VDS_CODE=?,DC_VM_VIS_START_RANGE=?,"
//										+ "DC_VM_VIS_END_RANGE=?, DC_VM_REFKEY=? WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_VM_REFKEY)=?";
//								pstmt = conn.prepareStatement(sql);
//								pstmt.setString(1, vinDetails.getNewCarlineCode());
//								pstmt.setString(2, vinDetails.getNewWmiCode());
//								pstmt.setString(3, vinDetails.getNewVdsCode());
//								pstmt.setString(4, vinDetails.getNewVisStartRange());
//								pstmt.setString(5, vinDetails.getNewVisEndRange());
//								pstmt.setString(6, vinDetails.getNewRefKey());
//								pstmt.setString(7, documentId.trim());
//								pstmt.setString(8, vinDetails.getOldRefKey().trim());
//								pstmt.executeUpdate();
//								pstmt.close();pstmt=null;
//								sql=null;
//							}
//						}
//					}
//					tokens=null;
					

					// CATEGORY TABLE
//					tokens = categoryTable.split(",");
//					if(null!=tokens && tokens.length>0)
//					{
//						for(int a=0;a<tokens.length;a++)
//						{
//							logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN CATEGORY TABLE -> " + tokens[a]);
//							// MNAO / MC / MME UPDATE
//							sql="UPDATE "+tokens[a]+" SET DC_IM_DOC_CATEGORY_NAME=?,DC_IM_DOC_CATEGORY_REF_KEY=? "
//									+ " WHERE TRIM(DC_IM_DOC_ID)=? AND TRIM(DC_IM_DOC_CATEGORY_REF_KEY)=?";
//							pstmt = conn.prepareStatement(sql);
//							pstmt.setString(1, vinDetails.getNewRefKey());
//							pstmt.setString(2, vinDetails.getNewRefKey());
//							pstmt.setString(3, documentId);
//							pstmt.setString(4, vinDetails.getOldRefKey());
//							pstmt.executeUpdate();
//							pstmt.close();pstmt=null;
//							sql=null;
//						}
//					}
					
					// VIEW CONTENT TABLE
					if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
					{
						logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN MNAO VC TABLE -> " + viewContentTable);
						// MNAO UPDATE
						updateMNAOViewContentData(vinDetails, conn, viewContentTable, locale, documentId);
					}
					else if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
					{
						logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN MC VC TABLE -> " + viewContentTable);
						// MC UPDATE
						updateMCMMEViewContentData(vinDetails, conn, viewContentTable, locale, documentId, "MC");
					}
//					else if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase()) > -1)
					else
					{
						logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN MME VC TABLE -> " + viewContentTable);
						// MME UPDATE
						updateMCMMEViewContentData(vinDetails, conn, viewContentTable, locale, documentId, "MME");
						
						/*
						 * FOR MME LOCALE - UPDATE VIN MANUAL TYPE MAPPING TABLE AS WELL - 12TH NOVEMBER NEW CHANGE
						 * REPLACE OLD VIN WITH NEW VIN
						 */
						logger.info("updateDocumentDetails :: ----------- CHECKING AND PERFORMING VIN UPDATE IN MDM VIN ML MAPPING TABLE -> gms3_mdm_vin_ml_mapping" );
						List<String> list = new ArrayList<String>();
						sql="SELECT VIN_ML_ID FROM gms3_mdm_vin_ml_mapping WHERE VIN_ML_LOCALE=? AND VIN_ML_CARLINE_CODE=? AND VIN_ML_WMI_CODE=? "
									+ " AND VIN_ML_VDS_CODE=? AND VIN_ML_VIS_START=? AND VIN_ML_VIS_END=? AND VIN_ML_STATUS='A' ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, locale);
						pstmt.setString(2, vinDetails.getOldCarlineCode());
						pstmt.setString(3, vinDetails.getOldWmiCode());
						pstmt.setString(4, vinDetails.getOldVdsCode());
						pstmt.setString(5, vinDetails.getOldVisStartRange());
						pstmt.setString(6, vinDetails.getOldVisEndRange());
						rs = pstmt.executeQuery();
						while(rs.next())
						{
							list.add(String.valueOf(rs.getLong("VIN_ML_ID")));
						}
						rs.close();rs=null;
						pstmt.close();pstmt=null;
						sql=null;
						
						if(null!=list && list.size()>0)
						{
							logger.info("updateDocumentDetails :: -------- Entry for OLD VIN Found,Replacing Entry for New VIN for {"+list.size()+"} Rows.");
							// iterate all rows and replace old VINs with New VINs
							for(int f=0;f<list.size();f++)
							{
								sql="UPDATE gms3_mdm_vin_ml_mapping SET VIN_ML_CARLINE_CODE=? , VIN_ML_WMI_CODE=? "
									+ " , VIN_ML_VDS_CODE=? , VIN_ML_VIS_START=? , VIN_ML_VIS_END=? WHERE VIN_ML_ID = "+ String.valueOf(list.get(f));
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, vinDetails.getNewCarlineCode());
								pstmt.setString(2, vinDetails.getNewWmiCode());
								pstmt.setString(3, vinDetails.getNewVdsCode());
								pstmt.setString(4, vinDetails.getNewVisStartRange());
								pstmt.setString(5, vinDetails.getNewVisEndRange());
								pstmt.executeUpdate();
								pstmt.close();pstmt=null;
								sql=null;
							}
						}
						else
						{
							logger.info("updateDocumentDetails :: -------- No Entry for OLD VIN Found, No updates being carried out in MDM VIN ML Mapping Table.");
						}
						list = null;
					}
				}
				vinDetails = null;
			}
			// commit transaction
			conn.commit();
			// set document processing status to success - as all transactions completed. No error
			documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_SUCCESS);
			
			vinDetails = null;
//			tokens = null;
//			vinMasterTable=null;
//			vinTable=null;
			viewContentTable=null;
//			categoryTable = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "updateDocumentDetails()", e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "updateDocumentDetails()", e1);
			}
			logger.info("updateDocumentDetails :: Failed to Update New VIN Details for "+documentId+" in DATABASE.");
			
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			documentMap.put(AutomationConstants.ERROR_CODE, e.getMessage());
			documentMap.put(AutomationConstants.ERROR_MESSAGE, writer.toString());
			// set processingStatus to failure - as DATABASE TRANSCATION FAILED.
			documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
			// set writer & print to null
			writer = null;
			// set print to null
			print = null;
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
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "updateDocumentDetails()", e);
			}
		}
		return documentMap;
	}
	
	private void updateMNAOViewContentData(ExcelRowDetails vinDetails, Connection conn, String tableName, String locale, String documentId) throws SQLException
	{
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		/*
		 * CHECK IF OLD VIN EXISTS IN VIEW CONTENT TABLE OR NOT
		 * IF YES - UPDATE THE VIN BY NEW VIN
		 * IF NO - INSERT NEW ROW FOR NEW VIN 
		 */
		ArrayList<String> rowsIdList = new ArrayList<String>();
		String sql="SELECT VC_VIN_ID FROM "+tableName+" WHERE VC_VIN_DOCUMENT_ID=? AND VC_VIN_LOCALE=? AND "
				+ " VC_VIN_WMI_CODE=? AND VC_VIN_VDS_CODE= ? AND VC_VIN_VIS_START_RANGE = ? "
				+ " AND VC_VIN_VIS_END_RANGE=? ";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, documentId.trim());
		pstmt.setString(2, locale.trim());
		pstmt.setString(3, vinDetails.getOldWmiCode().trim());
		pstmt.setString(4, vinDetails.getOldVdsCode().trim());
		pstmt.setString(5, vinDetails.getOldVisStartRange().trim());
		pstmt.setString(6, vinDetails.getOldVisEndRange().trim());
		rs = pstmt.executeQuery();
		while(rs.next())
		{
			if(rs.getLong("VC_VIN_ID")> 0)
			{
				rowsIdList.add(String.valueOf(rs.getLong("VC_VIN_ID")));
			}
		}
		rs.close();rs=null;
		pstmt.close();pstmt=null;
		sql=null;
		
		if(null!=rowsIdList && rowsIdList.size()>0)
		{
			// rows found needs to be updated.
			for(int a=0;a<rowsIdList.size();a++)
			{
				sql="UPDATE "+tableName+" SET VC_VIN_WMI_CODE=?,VC_VIN_VDS_CODE=?,VC_VIN_VIS_START_RANGE=?,VC_VIN_VIS_END_RANGE=? WHERE VC_VIN_ID="+rowsIdList.get(a);
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, vinDetails.getNewWmiCode().trim());
				pstmt.setString(2, vinDetails.getNewVdsCode().trim());
				pstmt.setString(3, vinDetails.getNewVisStartRange().trim());
				pstmt.setString(4, vinDetails.getNewVisEndRange().trim());
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql=null;
			}
		}
		rowsIdList = null;
	}

	private void updateMCMMEViewContentData(ExcelRowDetails vinDetails, Connection conn, String tableName,String locale, String documentId, String type) throws SQLException
	{
		PreparedStatement pstmt=null;
		ResultSet rs = null;
		/*
		 * CHECK IF OLD VIN EXISTS IN VIEW CONTENT TABLE OR NOT
		 * IF YES - UPDATE THE VIN BY NEW VIN
		 * IF NO - INSERT NEW ROW FOR NEW VIN 
		 */
		ArrayList<String> rowsIdList = new ArrayList<String>();
		String sql="SELECT VC_VIN_ID FROM "+tableName+" WHERE VC_VIN_DOCUMENT_ID=? AND VC_VIN_CARLINE_CODE=? AND "
				+ " VC_VIN_WMI_CODE=? AND VC_VIN_VDS_CODE= ? AND VC_VIN_VIS_START_RANGE = ? "
				+ " AND VC_VIN_VIS_END_RANGE=? AND VC_VIN_MODEL = ?";
//		logger.info("updateMCMMEViewContentData :: > sql >"+ sql);
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, documentId.trim());
		pstmt.setString(2, vinDetails.getOldCarlineCode().trim());
		pstmt.setString(3, vinDetails.getOldWmiCode().trim());
		pstmt.setString(4, vinDetails.getOldVdsCode().trim());
		pstmt.setString(5, vinDetails.getOldVisStartRange().trim());
		pstmt.setString(6, vinDetails.getOldVisEndRange().trim());
		if(type.equals("MC"))
		{
			// incase of MC Use eNGLIGH NAME FROM different Variable
			pstmt.setString(7, vinDetails.getOldModelNameEngForMC().trim());
		}
		else
		{
			// incase of MME Use English value default variable
			pstmt.setString(7, vinDetails.getOldModelName().trim());
		}
		rs = pstmt.executeQuery();
		while(rs.next())
		{
			if(rs.getLong("VC_VIN_ID")> 0)
			{
				rowsIdList.add(String.valueOf(rs.getLong("VC_VIN_ID")));
			}
		}
		rs.close();rs=null;
		pstmt.close();pstmt=null;
		sql=null;
		if(null!=rowsIdList && rowsIdList.size()>0)
		{
			logger.info("updateMCMMEViewContentData :: -------- Entry for OLD VIN Found, for { "+documentId.trim()+"} Replacing Entry for New VIN for {"+rowsIdList.size()+"} Rows.");
			
			/*
			 * NOT REQUIRED 25TH JULY 2019 AS WE ALREADY HAVE THE ENGLIGH & REGIONAL LABELS
			 * MODEL TYPE INFROMATION IS NOT AT ALL IMPORTANT SAVE DB TRANSACTIONS
			 */
//			String mdmLocaleCode = "";
//			if(type.equals("MC"))
//			{
//				// use same Locale
//				mdmLocaleCode = locale;
//			}
//			else
//			{
//				// en-UK Locale
//				mdmLocaleCode =ApplicationProperties.getProperty("en-uk");
//			}
//			mdmLocaleCode = mdmLocaleCode.replace("_", "-");
//			String carlineEngName="";
//			String carlineRegName="";
//			String modelType="";
//			/*
//			 * FETCH THE MODEL DETAILS FOR NEW VIN ON THE BASIS OF CARLINE CODE + WMI CODE + VDS CODE + VIS START + VIS END
//			 */
//			sql="SELECT MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG FROM gms3_mdm_vin_detail WHERE MDM_VIN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' AND "
//					+ " TRIM(LOWER(MDM_CRLN_CODE))=? AND TRIM(LOWER(MDM_VIN_WMI_CODE))=? AND TRIM(LOWER(MDM_VIN_VDS_CODE))=? AND TRIM(LOWER(MDM_ML_LANG_CODE))=?";
//			pstmt = conn.prepareStatement(sql);
//			pstmt.setString(1, vinDetails.getNewCarlineCode().trim().toLowerCase());
//			pstmt.setString(2, vinDetails.getNewWmiCode().trim().toLowerCase());
//			pstmt.setString(3, vinDetails.getNewVdsCode().trim().toLowerCase());
//			pstmt.setString(4, mdmLocaleCode.trim().toLowerCase());
//			rs = pstmt.executeQuery();
//			if(rs.next())
//			{
//				if(null!=rs.getString("MDM_CRLN_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_ENG_LANG")))
//				{
//					carlineEngName = rs.getString("MDM_CRLN_NAME_ENG_LANG").trim();
//				}
//				if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
//				{
//					carlineRegName = rs.getString("MDM_CRLN_NAME_REGIONAL_LANG");
//				}
//			}
//			rs.close();rs=null;
//			pstmt.close();pstmt=null;
//			sql=null;
//			
//			if(null!=carlineEngName && !"".equals(carlineEngName))
//			{
//				/*
//				 * if CARLINE ENG NAME IS NOT NULL - FETCH MODEL TYPE ON THE BASIS OF 
//				 * CARLINE ENG NAME + CARLINE CODE + WMI CODE FROM CARLINE TABLE
//				 */
//				sql="SELECT MDM_CRLN_MODEL_TYPE FROM gms3_mdm_carline_codes WHERE TRIM(LOWER(MDM_CRLN_NAME_ENG_LANG))=? AND TRIM(LOWER(MDM_CRLN_CODE))=? "
//						+ " AND TRIM(LOWER(MDM_CRLN_WMI_CODE)) = ? AND MDM_CRLN_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' "
//								+ " AND TRIM(LOWER(MDM_ML_LANG_CODE))=?";
//				pstmt = conn.prepareStatement(sql);
//				pstmt.setString(1, carlineEngName.trim().toLowerCase());
//				pstmt.setString(2, vinDetails.getNewCarlineCode().trim().toLowerCase());
//				pstmt.setString(3, vinDetails.getNewWmiCode().trim().toLowerCase());
//				pstmt.setString(4, mdmLocaleCode.trim().toLowerCase());
//				rs = pstmt.executeQuery();
//				if(rs.next())
//				{
//					if(null!=rs.getString("MDM_CRLN_MODEL_TYPE") && !"".equals(rs.getString("MDM_CRLN_MODEL_TYPE")))
//					{
//						modelType = rs.getString("MDM_CRLN_MODEL_TYPE");
//					}
//				}
//				rs.close();rs=null;
//				pstmt.close();pstmt=null;
//				sql=null;
//			}
//			
//			if(null!=carlineEngName && !"".equals(carlineEngName))
//			{
//				carlineEngName = carlineEngName.trim();
//			}
//			if(null!=carlineRegName && !"".equals(carlineRegName))
//			{
//				carlineRegName = carlineRegName.trim();
//			}
//			if(null!=modelType && !"".equals(modelType))
//			{
//				modelType = modelType.trim();
//			}
			
			// rows found needs to be updated.
			pstmt = null;
			for(int a=0;a<rowsIdList.size();a++)
			{
//				logger.info("--------------- update query start time :: >"+ new Date());
				sql="UPDATE "+tableName+" SET VC_VIN_WMI_CODE=?,VC_VIN_VDS_CODE=?,VC_VIN_VIS_START_RANGE=?,VC_VIN_VIS_END_RANGE=?,"
						+ " VC_VIN_CARLINE_CODE=?, VC_VIN_MODEL=?,  "
						+ " VC_VIN_CARLINE_NAME=? WHERE VC_VIN_ID="+rowsIdList.get(a);
				logger.info("updateMCMMEViewContentData :: Update sql :: > "+ sql);
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, vinDetails.getNewWmiCode().trim());
				pstmt.setString(2, vinDetails.getNewVdsCode().trim());
				pstmt.setString(3, vinDetails.getNewVisStartRange().trim());
				pstmt.setString(4, vinDetails.getNewVisEndRange().trim());
				pstmt.setString(5, vinDetails.getNewCarlineCode().trim());
				if(type.equals("MC"))
				{
					pstmt.setString(6, vinDetails.getNewModelNameEngForMC());
					pstmt.setString(7, vinDetails.getNewModelName());
				}
				else
				{
					pstmt.setString(6, vinDetails.getNewModelName());
					pstmt.setString(7, vinDetails.getNewModelNameRegForMNAOMME());
				}
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql=null;
			}
			
			sql = null;
//			mdmLocaleCode = null;
//			carlineEngName= null;
//			carlineRegName= null;
//			modelType= null;
		}
		rowsIdList = null;
	}

}
