package com.mazda.gms3.cdrom.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.cdrom.bean.KeyWordSearchBean;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.cdrom.bean.ViewContentBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;

public class CDRomManualsDAO extends DBConnectionHelper {

	static Logger logger = LogManager.getLogger(CDRomManualsDAO.class);

	/**
	 * THE DOCUMENTS SELECTED FOR ONE MANUAL TYPE, WITH THEIR CONTENT - the main CD ROM read.
	 *
	 * WAS: one query per document joining OK_IM.CONTENTTEXT to OK_IM.CONTENTDATA on
	 * documentid + localeid + published='Y', returning RECORDID and the XML column.
	 * NOW: kapture_cms_db.k_article gives the newest PUBLISHED version and its id, and the channel
	 * node of that version's generated XML gives the content. See CDRomKaptureContentDAO.
	 *
	 * THE published='Y' FILTER SURVIVES AS article_state='Published', and it is what makes a
	 * document "an actual document" for the CD - an unpublished document is not skipped by
	 * accident, it is skipped on purpose, exactly as before.
	 *
	 * A DOCUMENT WITH NO PUBLISHED VERSION IS OMITTED FROM THE LIST rather than added blank. That
	 * is the old behaviour too (the old rs.next() simply found nothing), and the caller counts the
	 * shortfall as documents that did not make the CD.
	 */
	public static List<LabelBean> getDocumentRecordIdAndXMLForManual(ArrayList<String> documentIdList, String locale) throws SQLException
	{
		List<LabelBean> docList = new ArrayList<LabelBean>();
		try {
			if (null != documentIdList && documentIdList.size() > 0 && null!=locale && !"".equals(locale))
			{
				String localeToCheck = locale.trim().replace("-", "_");
				/*
				 * ITERATE LIST AND FOR EACH DOCUMENT ID USING LOCALE SEARCH FOR THE NEWEST
				 * PUBLISHED VERSION - ONLY THESE DOCUMENTS WILL BE COUNTED AS ACTUAL DOCUMENTS
				 */
				for (String docId : documentIdList) {
					if(null==docId || "".equals(docId.trim()))
					{
						continue;
					}
					LabelBean clDetails = CDRomKaptureContentDAO
							.getPublishedChannelNode(docId.trim(), localeToCheck);
					if(null!=clDetails)
					{
						// the caller reads the document id off extraAttribute - kept as it was
						clDetails.setExtraAttribute(docId.trim());
						docList.add(clDetails);
					}
					else
					{
						logger.info("getDocumentRecordIdAndXMLForManual :: no published content for {"
								+ docId.trim() + "} / {" + localeToCheck + "} - not added to the CD.");
					}
				}
			}
		} catch (Exception e) {
			logger.info("getDocumentRecordIdAndXMLForManual :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"getDocumentRecordIdAndXMLForManual()", e);
			logger.info("getDocumentRecordIdAndXMLForManual :: ################ Exception ################");
		}
		return docList;
	}

	/**
	 * CONTENT FOR A DOCUMENT DISCOVERED THROUGH AN INNER LINK.
	 *
	 * NO LOCALE FILTER, DELIBERATELY - the old SQL had none either (it selected on documentid and
	 * Published='Y' alone), so the newest published version in ANY locale is taken. Changing that
	 * here would alter which documents the recursion pulls in, which is a behaviour change and not
	 * a migration, so it is left exactly as it was. Flagged for review alongside the inner-link
	 * pattern work.
	 *
	 * carLineCode / vdsCode / vinStartRange were already unused by the old query. They are kept in
	 * the signature so the call site does not have to change.
	 *
	 * THE RETURNED BEAN NOW CARRIES THE DOCUMENT ID, which is what lets the caller stop asking
	 * getDocumentID(recordId) for it.
	 */
	public static List<LabelBean> getDocumentsFromDocList(String docId,
			String locale, String carLineCode, String vdsCode,
			String vinStartRange) throws SQLException {
		List<LabelBean> docList = new ArrayList<LabelBean>();
		try {
			LabelBean clDetails = CDRomKaptureContentDAO.getPublishedChannelNode(docId, null);
			if (null != clDetails) {
				docList.add(clDetails);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"getDocumentsFromDocList()", e);
		}
		return docList;

	}

	/**
	 * NO LONGER USED BY THE CD FLOW - kept only so nothing outside this package breaks.
	 *
	 * It existed to turn a record id back into a document id, because OK_IM.CONTENTDATA held the
	 * XML under RECORDID and the caller had no other way to know which document it had. Kapture
	 * reads content by article id in the first place, so CDRomKaptureContentDAO sets the document
	 * id on the bean and saveInnerLinkFiles now takes it from there.
	 *
	 * There is no k_article equivalent worth writing: id -> article_id would be a one-line lookup,
	 * but every caller already has the article id.
	 *
	 * @deprecated read LabelBean.getDocumentId() instead
	 */
	@Deprecated
	public static String getDocumentID(String key) throws SQLException {
		logger.info("getDocumentID :: called for record id {" + key + "} - this lookup is obsolete,"
				+ " the document id is carried on LabelBean. Returning blank.");
		return "";
	}

	public static KeyWordSearchBean getKeyWordSearchDetails(String documentId,
			String vinLocale, String docType)
			throws SQLException {
		KeyWordSearchBean keyBean = null;
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		vinLocale = vinLocale.replace('-', '_');
		String manualType = docType;
		try {
			conn = getConnection();
			String tableName=null;
			if(vinLocale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					vinLocale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				tableName = "gms3_vc_japan_vin_details";
			}
			else
			{
				// MME MARKET LOCALE
				tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(vinLocale);
			}
			
			logger.info("getKeyWordSearchDetails :: LOCALE :: >"+ vinLocale + " tableName ::  >"+ tableName );
			String sql = "Select DISTINCT vc_vin_document_title,"
					+ "  vc_vin_category_name_1, vc_vin_category_name_2, vc_vin_category_name_3 "
					+ " from "+tableName+" "
					+ "  where TRIM(vc_vin_document_id) = '"
					+ documentId.trim()
					+ "' GROUP BY vc_vin_document_title,vc_vin_category_name_1, "
							+ " vc_vin_category_name_2, vc_vin_category_name_3";
			logger.info("getKeyWordSearchDetails :: sql :: > "+ sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				keyBean = new KeyWordSearchBean();
				keyBean.setDocumentId(documentId);
				keyBean.setTitle(rs.getString("vc_vin_document_title"));
				keyBean.setESICat1(rs.getString("vc_vin_category_name_1"));
				keyBean.setESICat2(rs.getString("vc_vin_category_name_2"));
				keyBean.setESICat3(rs.getString("vc_vin_category_name_3"));
				keyBean.setManualType(manualType);
			}
			sql = null;
			tableName = null;
		} catch (Exception e) {
			logger.info("getKeyWordSearchDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"getKeyWordSearchDetails()", e);
			logger.info("getKeyWordSearchDetails :: ################ Exception ################");
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return keyBean;
	}

	public static ViewContentBean getViewDetails(String documentId,
			String vinLocale, String carLineCode, String vdsCode,
			String wmiCode, String visStartRange, String docType)
			throws SQLException {
		ViewContentBean viewBean = null;
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		vinLocale = vinLocale.replace('-', '_');
		try {
			conn = getConnection();
			String tableName=null;
			if(vinLocale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					vinLocale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				tableName = "gms3_vc_japan_vin_details";
			}
			else
			{
				// MME MARKET LOCALE
				tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(vinLocale);
			}
			
			logger.info("getViewDetails :: LOCALE :: >"+ vinLocale + " tableName ::  >"+ tableName );
			String sql = "Select DISTINCT * from "+tableName+" "
					+ "  where TRIM(vc_vin_document_id) = '"
					+ documentId.trim()
					+ "' AND TRIM(vc_vin_locale) = '"
					+ vinLocale.trim()
					+ "' AND TRIM(vc_vin_carline_code) = '"
					+ carLineCode.trim()
					+ "'"
					+ " AND TRIM(vc_vin_wmi_code) ='"
					+ wmiCode.trim()
					+ "' AND TRIM(vc_vin_vds_code) = '"
					+ vdsCode.trim()
					+ "'"
					+ " AND TRIM(vc_vin_vis_start_range) = '"
					+ visStartRange.trim() + "'";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				viewBean = new ViewContentBean();
				viewBean.setDocumentId(rs.getString("vc_vin_document_id"));
				viewBean.setTitle(rs.getString("vc_vin_document_title"));
				viewBean.setSequencenumber(rs
						.getString("vc_vin_dispord_seq_no"));
				viewBean.setDisplayLevel1(rs.getString("vc_vin_disp_name_1"));
				viewBean.setDisplayLevel2(rs.getString("vc_vin_disp_name_2"));
				viewBean.setDisplayLevel3(rs.getString("vc_vin_disp_name_3"));
				viewBean.setDisplayLevel4(rs.getString("vc_vin_disp_name_4"));
				viewBean.setDisplayLevel5(rs.getString("vc_vin_disp_name_5"));
				viewBean.setDisplayLevel6(rs.getString("vc_vin_disp_name_6"));
				
			}
			sql = null;
		} catch (Exception e) {
			logger.info("getViewDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"getViewDetails()", e);
			logger.info("getViewDetails :: ################ Exception ################");
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return viewBean;
	}

	public static ViewContentBean getViewDetailsFromCDRom(String documentId,String locale)
			throws SQLException {
		ViewContentBean viewBean = null;
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
			{
				/*
				 * IDENTIFY TABLE NAME
				 * CHECK HERE IN BOTH TABLES, WHERE EVER ENTRY FOR DOCUMENT ID IS FOUND BREAK THE LOOP.
				 */
				String tableName=null;
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					tableName = "gms3_dmt_mc_newm_cd_data,gms3_dmt_mc_cd_data";
				}
				else
				{
					// MME MARKET LOCALE
					tableName = "gms3_dmt_"+Utilities.tableLocale(locale)+"_nm_cd_data";
					tableName+=",gms3_dmt_"+Utilities.tableLocale(locale)+"_cd_data";
				}
				
				logger.info("getViewDetailsFromCDRom :: LOCALE :: >"+ locale + " tableName ::  >"+ tableName );
				
				
				String[] tabTables = tableName.split(",");
				String sql=null;
				conn = getConnection();
				
				for(int a=0;a<tabTables.length;a++)
				{
					sql="SELECT dc_title, dc_cd_dispord_name_level_1,dc_cd_dispord_name_level_2, dc_cd_dispord_name_level_3, dc_cd_dispord_name_level_4, "
							+ " dc_cd_dispord_name_level_5,dc_cd_dispord_name_level_6,dc_cd_dispord_name_level_7,"
							+ " dc_sequence_no FROM "+ tabTables[a].toString() +" WHERE dc_im_doc_id='"+documentId.trim()+"'";
					logger.info("getViewDetailsFromCDRom :: Sql :: > "+ sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					if (rs.next()) {
						viewBean = new ViewContentBean();
						viewBean.setDocumentId(documentId);
						viewBean.setTitle(rs.getString("dc_title"));
						viewBean.setSequencenumber(rs
								.getString("dc_sequence_no"));
						viewBean.setDisplayLevel1(rs.getString("dc_cd_dispord_name_level_1"));
						viewBean.setDisplayLevel2(rs.getString("dc_cd_dispord_name_level_2"));
						viewBean.setDisplayLevel3(rs.getString("dc_cd_dispord_name_level_3"));
						viewBean.setDisplayLevel4(rs.getString("dc_cd_dispord_name_level_4"));
						viewBean.setDisplayLevel5(rs.getString("dc_cd_dispord_name_level_5"));
						viewBean.setDisplayLevel6(rs.getString("dc_cd_dispord_name_level_6"));
						viewBean.setDisplayLevel7(rs.getString("dc_cd_dispord_name_level_7"));
						// break the loop
						break;
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
					sql = null;
				}
				sql = null;
				tabTables = null;
				tableName=  null;
			}
			else
			{
				logger.info("getViewDetailsFromCDRom :: DOCUMENT id AS PARAMETER IS NULL.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"getViewDetailsFromCDRom()", e);
		} finally {
			if (null != stmt)
				stmt.close();
			if (null != rs)
				rs.close();
			if (null != conn)
				conn.close();
		}
		return viewBean;
	}
	
	/**
	 * CONTENT OF A SINGLE DOCUMENT, USED BY THE WIRING DIAGRAM JS REWRITERS.
	 *
	 * NO LOCALE FILTER, as before - see getDocumentsFromDocList.
	 *
	 * @return the channel node XML, "" when the document has no published content, or null when
	 *         no document id was given - the three answers the old method gave
	 */
	public static String getDocumentFromDocumentID(String docId)
			throws SQLException {
		if (!StringUtils.isNotBlank(docId)) {
			return null;
		}
		String xml = "";
		try {
			LabelBean details = CDRomKaptureContentDAO.getPublishedChannelNode(docId, null);
			if (null != details && null != details.getValue()) {
				xml = details.getValue();
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomManualsDAO.class.getName(), "getDocumentFromDocumentID()", e);
		}
		return xml;
	}

	public static String getManualTypeNameOnCode(String code, String locale) throws SQLException
	{
		String manualTypeName=null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=code && !"".equals(code) && null!=locale && !"".equals(locale))
			{
				locale = locale.replace("_", "-");
				conn = getConnection();
				String sql="SELECT dc_manual_name FROM gms3_dmt_conv_manual_type WHERE TRIM(LOWER(dc_manual_code))='"+code.trim().toLowerCase()+"' AND " +
						" TRIM(LOWER(mdm_ml_lang_code)) ='"+locale.trim().toLowerCase()+"' AND dc_manual_flag='1'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					manualTypeName = rs.getString("dc_manual_name");
					if(null!=manualTypeName && !"".equals(manualTypeName))
					{
						manualTypeName = manualTypeName.trim();
					}
				}
			}
			else
			{
				logger.info("getManualTypeNameOnCode :: Manual Type Code & Locale as Parameters are null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(), "getManualTypeNameOnCode()", e);
		}
		finally
		{
			code = null;
			locale = null;
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return manualTypeName;
	}
	
	public static String getManualTypeOnDocumentIdAndLocale(String documentId, String locale) throws SQLException
	{
		String manualTypeName=null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
			{
				String tableName=null;
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
				
				
				locale = locale.replace("_", "-");
				conn = getConnection();
				String sql="SELECT B.dc_manual_name FROM "+tableName+" A, gms3_dmt_conv_manual_type B " +
						" WHERE  TRIM(A.vc_vin_document_id) ='"+documentId+"' AND TRIM(A.vc_vin_document_type) = TRIM(B.dc_manual_ref_key) " +
						" AND TRIM(LOWER(B.mdm_ml_lang_code))='"+locale.trim().toLowerCase()+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					manualTypeName = rs.getString("dc_manual_name");
					if(null!=manualTypeName && !"".equals(manualTypeName))
					{
						manualTypeName = manualTypeName.trim();
					}
				}
				tableName = null;
				sql  =null;
			}
			else
			{
				logger.info("getManualTypeOnDocumentIdAndLocale :: Document id & Locale as Parameters are null. Return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(), "getManualTypeOnDocumentIdAndLocale()", e);
		}
		finally
		{
			documentId = null;
			locale = null;
			if(null!=conn)
				conn.close();
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return manualTypeName;
	}
}
