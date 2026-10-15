package com.mazda.gms3.cdrom.bean.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mazda.gms3.cdrom.bean.DocumentFailureReportBean;
import com.mazda.gms3.cdrom.bean.InnerLinkReportBean;
import com.mazda.gms3.cdrom.bean.KeyWordSearchBean;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.cdrom.bean.OKAssetReportBean;
import com.mazda.gms3.cdrom.bean.ServiceManualDocment;
import com.mazda.gms3.cdrom.bean.TransactionReportBean;
import com.mazda.gms3.cdrom.bean.ViewContentBean;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.cdrom.dao.CDRomManualsDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;

public class CDRomStartConversionImpl {

	static Logger logger = LogManager.getLogger(CDRomStartConversionImpl.class);

	public static void main(String[] args)
	{
		try
		{
			String xml = null;
			String dbUrl= "jdbc:oracle:thin:@OKMPROD.db.mazda.co.jp:1531:OKMPROD.db.mazda.co.jp";
			Driver myDriver = new oracle.jdbc.driver.OracleDriver();
			DriverManager.registerDriver( myDriver );
			// getConnection Object
			Connection connection = DriverManager.getConnection(dbUrl,"v108469", "CHandigarh82##");
			String sql = "SELECT A.RECORDID, B.XML FROM OK_IM.CONTENTTEXT A, OK_IM.CONTENTDATA B WHERE A.DOCUMENTID='SM1613986' " + 
					" AND A.LOCALEID='ja_JP' AND A.PUBLISHED='Y' AND A.RECORDID=B.RECORDID";
			Statement stmt=connection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				xml = rs.getString("XML");
			}
			System.out.println(xml);
			CDRomStartConversionImpl impl = new CDRomStartConversionImpl();
//			byte[] data = FileReadWriteUtil.readFile("C:\\Users\\v068321\\Downloads\\TEST.xml");
//			String xml = new String(data);
			ServiceManualDocment s= new ServiceManualDocment();
			s = impl.getParseddocument(xml);
//			System.out.println(s.getContent());
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	List<String> documentIdListFromFiles = new ArrayList<String>();
	List<String> documentIdListFromManual = new ArrayList<String>();
	List<String> imagePathList = new ArrayList<String>();
	List<String> contentCSSPathsList = new ArrayList<String>();
	List<String> documentsStatusList = new ArrayList<String>();
	List<String> okAssetStatusList = new ArrayList<String>();
	HashMap<String, List<String>> dataMap = new HashMap<String, List<String>>();
	int okAssetCount = 0;
	List<String> djvuPathList = new ArrayList<String>();
	List<String> pdfPathList = new ArrayList<String>();
	MessageProperties msgProps = null;
	String mazdaMCCSSName=  ApplicationProperties.getProperty("GMS3_MC_CUSTOM_CONTENT_CSS");
	

	/*
	 * HAS THE SCREEN ABORTED THIS CD CREATION SCHEDULE?
	 *
	 * This is the DATA EXTRACT schedule (the second of the two CDRom jobs that can be aborted; the
	 * first is the selection-criteria job in CDRomSearchCriteriaJobImpl). It copies images, content
	 * CSS, inner-link XMLs and whole folders, so without a check it runs to the end however early
	 * the user pressed Abort - Thread.stop() was removed in Java 20 and no longer stops anything.
	 *
	 * Throttled and latching, so a loop over thousands of assets cannot turn this into a query per
	 * iteration. CDRom keeps NUMERIC status codes, which CDRomDAO.isAborted() resolves.
	 */
	private static final long ABORT_CHECK_INTERVAL_MS = 500L;

	private long lastAbortCheckTime = 0L;

	private boolean scheduleAborted = false;

	private String abortScheduleId = null;

	private boolean isScheduleAborted()
	{
		if(scheduleAborted==true)
		{
			return true;
		}
		if(null==abortScheduleId || "".equals(abortScheduleId))
		{
			return false;
		}
		long now = System.currentTimeMillis();
		if(now-lastAbortCheckTime < ABORT_CHECK_INTERVAL_MS)
		{
			return false;
		}
		lastAbortCheckTime = now;
		scheduleAborted = CDRomDAO.isAborted(abortScheduleId);
		if(scheduleAborted==true)
		{
			logger.info("isScheduleAborted :: CD SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
					+" SCREEN. Stopping after the item in flight.");
		}
		return scheduleAborted;
	}
	public void startConversion(List<LabelBean> xmlDocumentList,
			String scheduleId, String timeStamp, String locale,
			String carLineCode, String docType, String vdsCode,
			String vinStartRange, String modelName, String networkPath,
			String wmiCode, List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList, 
			String manualTypeName, List<OKAssetReportBean> okAssetReportBean,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String jobStatus,CDRomDAO cdRomDao) {
		// remember the schedule so the abort check works in the helpers below
		abortScheduleId = scheduleId;
//		boolean updateProcessingStatus;
		List<String> docIdList = new ArrayList<String>();
		try {
//			updateProcessingStatus = CDRomDAO
//					.updateCurrentProcessingStatus(
//							String.valueOf(scheduleId),
//							ApplicationProperties
//									.getProperty("schedule.current.status.document.value"),
//							docType);
//			if (updateProcessingStatus == true) {
			
				for (LabelBean xmlDoc : xmlDocumentList) {
					if(isScheduleAborted()) { break; }
//					String documentId = CDRomManualsDAO.getDocumentID(xmlDoc
//							.getKey());
					String documentId =xmlDoc.getExtraAttribute();
					docIdList.add(documentId);
					
					boolean success = saveFileInFolder(documentId,
							xmlDoc.getValue(), timeStamp, locale, carLineCode,
							docType, vdsCode, vinStartRange, modelName,
							networkPath, wmiCode, keywordSearchList,
							viewContentList, firstLevelCatList, 
							manualTypeName, transactionReportBean,
							documentFailureReportBean, jobStatus,
							innerLinkReportBean);
					updateCount(String.valueOf(scheduleId), success, docType,cdRomDao);
				}
				documentIdListFromManual.addAll(docIdList);
				if(docType.equalsIgnoreCase("WD") || docType.equalsIgnoreCase("e-WD")){
					countOkAssetForWiringDiagram(timeStamp, networkPath);
					cdRomDao.updateInnerLinkAndOKAssetCount(scheduleId,
							okAssetCount, documentIdListFromFiles.size(),
							docType);
				}
				
				/*
				 * FOR UPDATING OK ASSETS COUNT
				 * ADD CONTENT CSS PATHS LIST SIZE AS WELL. (TOYOTA CONTENT CHANGE) - 29 JUNE 2018
				 * 
				 * NO NEED OF ADDING CSS PATHS LIST SIZE TO OKASSTES COUNT - IT IS UNNECESSARILY DISTURBING COUNTS.
				 * SET IT TO ORIGINAL - 03 JUNE 2018
				 */
//				CDRomDAO.updateInnerLinkAndOKAssetCount(scheduleId,
//						imagePathList.size()+contentCSSPathsList.size(), documentIdListFromFiles.size(),
//						docType);
//				CDRomDAO.updateInnerLinkAndOKAssetCount(scheduleId,
//						imagePathList.size(), documentIdListFromFiles.size(),
//						docType);
				
				/*
				 * HERE UPDATE ONLY INNER LINKS COUNT 
				 * IMAGES COUNT WILL BE UPDATED AFTER PROCESS INNER LINK DOCS
				 * BECAUSE IMAGES COUNT OF INNER LINK DOCS ALSO NEEDS TO BE UPDATED
				 * DATE - 04 JULY 2018
				 */
				cdRomDao.updateInnerLinksCount(scheduleId,documentIdListFromFiles.size(),docType);
				
				cdRomDao.updateProcessedInnerLinksCount(scheduleId,
						documentIdListFromFiles.size(), docType);
				processInnerLinkDocs(timeStamp, locale, carLineCode, docType,
						vdsCode, vinStartRange, modelName, scheduleId,
						networkPath, wmiCode, keywordSearchList,
						viewContentList, firstLevelCatList, 
						manualTypeName, innerLinkReportBean,
						transactionReportBean, documentFailureReportBean,
						jobStatus,cdRomDao);
				String folderPath = getFolderPath(timeStamp, locale,
						carLineCode, docType, vdsCode, vinStartRange,
						modelName, networkPath);
				// UPDATE TOTAL OKASSETS COUNT - OF ORIGINAL DOCS & INNERLINK DOCS
				cdRomDao.updateOKAssetCount(scheduleId, imagePathList.size(), docType);
				
				copyImages(folderPath, scheduleId, docType, okAssetReportBean,
						jobStatus,cdRomDao);
				// CALL FUNCTION TO  COPY INLINE CONTENT CSS - TOYOTA CONTENT
				copyContentCSS(folderPath, scheduleId, docType, okAssetReportBean, jobStatus);
				documentIdListFromManual.removeAll(documentIdListFromManual);
				documentIdListFromFiles.removeAll(documentIdListFromFiles);
				documentsStatusList.removeAll(documentsStatusList);
				okAssetStatusList.removeAll(okAssetStatusList);
				okAssetCount=0;
				djvuPathList.clear();
//			}

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomStartConversionImpl.class.getName(),
					"startConversion()", e);
			/*try 
			 * NOT NEEDED HERE, BECASUE ITEM STATUS MUST HAVE TO BE COMPLETED NOT FAILURE.
			 * FAILURE WILL BE SCHEDULE STATUS ONLY.
			 * {
				CDRomDAO.updateCurrentProcessingStatus(scheduleId, "Failed",
						docType);
			} catch (SQLException e1) {
				logger.info("startConversion :: ################ Exception ################");
				Utilities.printStackTraceToLogs(
						CDRomStartConversionImpl.class.getName(),
						"startConversion()", e);
				logger.info("startConversion :: ################ Exception ################");
			}*/
		}
		logger.info(" End startConversion for manual type: " + docType);
	}

	/**
	 * JOINS THE OK ASSET ROOT TO THE PATH FOUND IN THE CONTENT, with exactly one separator.
	 *
	 * The two halves come from different places and neither can be relied on for its slashes: the
	 * root is a property somebody edits per environment (cdrom.image.server.path /
	 * cdrom.pdf.server.path), and the path is whatever Kapture wrote into the src attribute.
	 *
	 * WHERE THE "/content" SEGMENT LIVES. The stored content src is "/library/MAZDA/..." - it does
	 * NOT carry a "/content" prefix (verified against the migrated content store; every path ref is
	 * plain "library/MAZDA"). Kapture SERVES those assets to a browser one context deeper, under
	 * "/content/library/MAZDA/...", but that is the web URL, not the stored src. For CDRom, which
	 * reads the files off disk, the "/content" segment is supplied by the ROOT PROPERTY: on the
	 * server the root ends "/content", so root + src = ".../content" + "/library/MAZDA" =
	 * ".../content/library/MAZDA" - a SINGLE "/content", no doubling. Do NOT "fix" this by dropping
	 * "/content" from the root, and do NOT add a "/content" segment in code.
	 *
	 * THE PATH IS OTHERWISE UNTOUCHED - no context is added or stripped here. If the "/content"
	 * segment ever has to be added or removed for a different environment, that belongs in the
	 * property, not in code.
	 *
	 * NOTE two asset lookups still plain-concatenate instead of calling this helper - findOkAsset()
	 * and copyImagesForDocument(). They rely on the root NOT carrying a trailing slash (else the
	 * join yields a harmless "//"). Prefer routing new asset joins through here.
	 */
	private String resolveAssetPath(String assetRoot, String pathInContent) {
		String root = (null == assetRoot ? "" : assetRoot.trim());
		String path = (null == pathInContent ? "" : pathInContent.trim());
		while (root.endsWith("/") || root.endsWith("\\")) {
			if(isScheduleAborted()) { break; }
			root = root.substring(0, root.length() - 1);
		}
		while (path.startsWith("/") || path.startsWith("\\")) {
			if(isScheduleAborted()) { break; }
			path = path.substring(1);
		}
		return root + "/" + path;
	}

	private void copyImages(String folderPath, String scheduleId,
			String docType, List<OKAssetReportBean> okAssetReportBean,
			String jobStatus,CDRomDAO cdRomDao) throws SQLException {
		if (imagePathList != null && imagePathList.size() > 0) {
			cdRomDao.updateCurrentProcessingStatus(
					String.valueOf(scheduleId),
					ApplicationProperties
							.getProperty("schedule.current.status.okasset.value"),
					docType);
			logger.info(" Start OKAssetProcessing for manual type: " + docType);
			for (int i = 0; i < imagePathList.size(); i++) {
				if(isScheduleAborted()) { break; }
				String arr[] = imagePathList.get(i).split(",");
				String imagePath = resolveAssetPath(ApplicationProperties
						.getProperty("cdrom.image.server.path"), arr[0]);
				String okAssetName = arr[0].substring(
						arr[0].lastIndexOf('/') + 1, arr[0].length());
				File source = new File(imagePath);
				File dest = new File(folderPath + "/images");
				OKAssetReportBean temp = new OKAssetReportBean();
				try {
					temp.setDocumentId(arr[1]);
					temp.setManualType(docType);
					temp.setOkAssetName(okAssetName);
					temp.setOkAssetSourceLocation(imagePath);
					FileUtils.copyFileToDirectory(source, dest);
					temp.setStatus("Success");
					temp.setOkAssetDestinationLocation(folderPath + "/images");
					temp.setErrorCode("");
					temp.setErrorMessage("");
					temp.setErrorTime("");
					cdRomDao.updateOKAssetProcessingCount(scheduleId, docType, 1);
				} catch (IOException e) {
					temp.setStatus("Failure");
					temp.setErrorCode(e.getClass().getName());
					temp.setErrorMessage(e.getMessage());
					temp.setErrorTime(String.valueOf(new Date().getTime()));
					cdRomDao.updateOKAssetFailureCount(scheduleId, docType);
					logger.info("processImages :: ################ Exception ################");
					Utilities.printStackTraceToLogs(
							CDRomStartConversionImpl.class.getName(),
							"processImages()", e);
					logger.info("processImages :: ################ Exception ################");
				}
				okAssetReportBean.add(temp);
			}
		}
		if(docType.equalsIgnoreCase("WD") || docType.equalsIgnoreCase("e-WD")){
			cdRomDao.updateOKAssetProcessingCount(scheduleId, docType, okAssetCount);
		}
		logger.info(" End OKAssetProcessing for manual type: " + docType);
		imagePathList.removeAll(imagePathList);
	}

	private void copyContentCSS(String folderPath, String scheduleId,
			String docType, List<OKAssetReportBean> okAssetReportBean,
			String jobStatus) throws SQLException {
		
		if (contentCSSPathsList != null && contentCSSPathsList.size() > 0) {
			
			/*
			 *  do not update any count for this operation
			 *  disturbing okAssets count. - 03 JUNE 2018
			 */
//			CDRomDAO.updateCurrentProcessingStatus(
//					String.valueOf(scheduleId),
//					ApplicationProperties
//							.getProperty("schedule.current.status.okasset.value"),
//					docType);
			logger.info(" Start OKAssetProcessing for manual type: " + docType);
			for (int i = 0; i < contentCSSPathsList.size(); i++) {
				if(isScheduleAborted()) { break; }
				String arr[] = contentCSSPathsList.get(i).split(",");
				String cssPath = resolveAssetPath(ApplicationProperties
						.getProperty("cdrom.image.server.path"), arr[0]);
//				String okAssetName = arr[0].substring(
//						arr[0].lastIndexOf('/') + 1, arr[0].length());
				File source = new File(cssPath);
				File dest = new File(folderPath + "/contentcss");
//				OKAssetReportBean temp = new OKAssetReportBean();
				try {
//					temp.setDocumentId(arr[1]);
//					temp.setManualType(docType);
//					temp.setOkAssetName(okAssetName);
//					temp.setOkAssetSourceLocation(cssPath);
					FileUtils.copyFileToDirectory(source, dest);
//					temp.setStatus("Success");
//					temp.setOkAssetDestinationLocation(folderPath + "/contentcss");
//					temp.setErrorCode("");
//					temp.setErrorMessage("");
//					temp.setErrorTime("");
//					CDRomDAO.updateOKAssetProcessingCount(scheduleId, docType, 1);
				} catch (IOException e) {
//					temp.setStatus("Failure");
//					temp.setErrorCode(e.getClass().getName());
//					temp.setErrorMessage(e.getMessage());
//					temp.setErrorTime(String.valueOf(new Date().getTime()));
//					CDRomDAO.updateOKAssetFailureCount(scheduleId, docType);
					logger.info("processContenCSS :: ################ Exception ################");
					Utilities.printStackTraceToLogs(
							CDRomStartConversionImpl.class.getName(),
							"processContenCSS()", e);
					logger.info("processContenCSS :: ################ Exception ################");
				}
//				okAssetReportBean.add(temp);
			}
		}
//		if(docType.equalsIgnoreCase("WD") || docType.equalsIgnoreCase("e-WD")){
//			CDRomDAO.updateOKAssetProcessingCount(scheduleId, docType, okAssetCount);
//		}
		logger.info(" End OKAssetProcessing for manual type: " + docType);
		contentCSSPathsList.removeAll(contentCSSPathsList);
	}

	
	
	private void processInnerLinkDocs(String timeStamp, String locale,
			String carLineCode, String docType, String vdsCode,
			String vinStartRange, String modelName, String scheduleId,
			String networkPath, String wmiCode,
			List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList, 
			String manualTypeName,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String jobStatus,CDRomDAO cdRomDao) throws SQLException {
		while (compareDocList().size() > 0) {
			if(isScheduleAborted()) { break; }
			List<String> newDocList = compareDocList();
			documentIdListFromFiles.removeAll(documentIdListFromFiles);
			for (String docId : newDocList) {
				if(isScheduleAborted()) { break; }
				List<LabelBean> newXmlDocumentList = CDRomManualsDAO
						.getDocumentsFromDocList(docId, locale, carLineCode,
								vdsCode, vinStartRange);
				saveInnerLinkFiles(newXmlDocumentList, timeStamp, locale,
						carLineCode, docType, vdsCode, vinStartRange,
						modelName, scheduleId, networkPath, wmiCode,
						keywordSearchList, viewContentList, firstLevelCatList,
						manualTypeName, innerLinkReportBean,
						transactionReportBean, documentFailureReportBean,
						jobStatus,cdRomDao);
			}
		}
	}

	private void saveInnerLinkFiles(List<LabelBean> newXmlDocumentList,
			String timeStamp, String locale, String carLineCode,
			String docType, String vdsCode, String vinStartRange,
			String modelName, String scheduleId, String networkPath,
			String wmiCode, List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList,
			String manualTypeName,
			List<InnerLinkReportBean> innerLinkReportBean,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String jobStatus,CDRomDAO cdRomDao) throws SQLException {
		int failedDoc = 0, passedDoc = 0;
		for (LabelBean xmlDoc : newXmlDocumentList) {
			if(isScheduleAborted()) { break; }
			/*
			 * THE BEAN CARRIES THE DOCUMENT ID NOW. This used to be a second query -
			 * getDocumentID(recordId) - because OK_IM.CONTENTDATA was keyed by record id and the
			 * caller had nothing else to go on. Kapture is read BY article id, so the reader knows
			 * it already and one query per inner-linked document disappears.
			 */
			String documentId = xmlDoc.getDocumentId();
			if (!documentIdListFromManual.contains(documentId))
				documentIdListFromManual.add(documentId);
			boolean success = saveFileInFolder(documentId, xmlDoc.getValue(),
					timeStamp, locale, carLineCode, docType, vdsCode,
					vinStartRange, modelName, networkPath, wmiCode,
					keywordSearchList, viewContentList, firstLevelCatList,
					manualTypeName, transactionReportBean,
					documentFailureReportBean, jobStatus, innerLinkReportBean);
			if (success)
				passedDoc++;
			else
				// innerLinkReportBean.add(docType+","+documentId+".html");
				failedDoc++;
			break;
		}
		if (failedDoc > 0 || passedDoc > 0) {
			cdRomDao.upadteScheduleDocCount(scheduleId, passedDoc + failedDoc,
					docType);
			cdRomDao.updateProcessingCount(scheduleId, passedDoc, docType);
			cdRomDao.updateFailureCount(scheduleId, failedDoc, docType);
		}
	}

	private List<String> compareDocList() {
		List<String> newDocList = new ArrayList<String>();
		if (documentIdListFromFiles.size() > 0) {
			for (String item : documentIdListFromFiles) {
				if(isScheduleAborted()) { break; }
				if (!documentIdListFromManual.contains(item)) {
					newDocList.add(item);
				}
			}
		}

		return newDocList;
	}

	private void updateCount(String scheduleID, boolean success, String docType,CDRomDAO cdRomDao)
			throws SQLException {
		int count = 1;
		if (success) {
			cdRomDao.updateProcessingCount(scheduleID, count, docType);
		} else {
			cdRomDao.updateFailureCount(scheduleID, count, docType);
		}

	}

	private boolean saveFileInFolder(String documentId, String xml,
			String timeStamp, String locale, String selectedModel,
			String manualType, String selectedVds, String selectedVinRange,
			String modelName, String networkPath, String wmiCode,
			List<KeyWordSearchBean> keywordSearchList,
			List<ViewContentBean> viewContentList,
			Set<String> firstLevelCatList, 
			String manualTypeName,
			List<TransactionReportBean> transactionReportBean,
			List<DocumentFailureReportBean> documentFailureReportBean,
			String jobStatus, List<InnerLinkReportBean> innerLinkReportBean) {
		
		String filePath = getFolderPath(timeStamp, locale, selectedModel,
				manualType, selectedVds, selectedVinRange, modelName,
				networkPath);
		String filePathForHTML = getFilePathForHTML(locale, selectedModel,
				manualType, selectedVds, selectedVinRange, modelName);
		TransactionReportBean tr = new TransactionReportBean();
		KeyWordSearchBean keyBean = null;
		ViewContentBean viewBean = null;
		try {
			if (StringUtils.isNotBlank(filePath)) {

//				keyBean = CDRomManualsDAO.getKeyWordSearchDetails(documentId,s
//						locale, selectedModel, selectedVds, wmiCode,
//						selectedVinRange, manualType);
//				viewBean = CDRomManualsDAO.getViewDetails(documentId, locale,
//						selectedModel, selectedVds, wmiCode, selectedVinRange,
//						manualType);
				keyBean = CDRomManualsDAO.getKeyWordSearchDetails(documentId,
						locale,  manualType);
				viewBean = CDRomManualsDAO.getViewDetailsFromCDRom(documentId,locale);
				saveFile(filePath, documentId, xml, locale, selectedModel,
						keyBean, viewBean, timeStamp, networkPath,
						filePathForHTML, manualType, innerLinkReportBean, tr);
				if(null!=keyBean)
				{
					keyBean.setManualTypeName(manualTypeName);
					if (StringUtils.isBlank(keyBean.getFilePath())) {
						throw new FileNotFoundException();
					}
					keywordSearchList.add(keyBean);
					firstLevelCatList.add(keyBean.getESICat1());
				}
				
				if(null!=viewBean)
				{
					viewBean.setManualTypeName(manualTypeName);
					if (StringUtils.isBlank(viewBean.getFilePath())) {
						throw new FileNotFoundException();
					}
					if(null!=viewBean.getDocumentId() && !"".equals(viewBean.getDocumentId()) && 
							null!=viewBean.getSequencenumber() && !"".equals(viewBean.getSequencenumber()))
					{
						viewContentList.add(viewBean);
					}
				}
				
				if (StringUtils.isNotBlank(tr.getDocumentId())) {
					tr.setStatus("Success");
					tr.setFailureReason("");
					transactionReportBean.add(tr);
				}
				return true;
			}
		} catch (Exception e) {
			tr.setStatus("Failure");
			tr.setFailureReason("Failed while document processing");
			DocumentFailureReportBean dc = new DocumentFailureReportBean();
			dc.setDocumentId(documentId);
			dc.setManualType(manualType);
			
			// GET CORRECT ERROR MESSAGES
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			dc.setErrorCode(e.getMessage());
			dc.setErrorMessage(writer.toString());
			try{ if(null!=writer) {writer.close();}} catch(Exception ex) {}
			writer = null;
			try{ if(null!=print) {print.close();}} catch(Exception ex) {}
			print = null;
			
//			dc.setErrorCode(e.getClass().getName());
//			dc.setErrorMessage(e.getMessage());
			dc.setErrorTime(String.valueOf(new Date().getTime()));
			documentFailureReportBean.add(dc);
			logger.info("saveFileInFolder :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(),"saveFileInFolder()", e);
			logger.info("saveFileInFolder :: ################ Exception ################");
		}

		return false;
	}

	private void processPDFFiles(String pdfResourcePath, String filePath,
			String pdfDocId) throws IOException {
		// sites/MAZDA/content/staging/OTHER_SERVICE_MANUALS/112000/OSM112941/ja_JP/2.0/
		// sites/MAZDA/content/live/OTHER_SERVICE_MANUALS/112000/OSM112941/ja_JP/
		pdfResourcePath = pdfResourcePath.replace("staging", "live");
		int index = StringUtils.ordinalIndexOf(pdfResourcePath, "/", 8);
		pdfResourcePath = pdfResourcePath.substring(0, index + 1);
		copyPDFFile(pdfResourcePath, filePath, pdfDocId);
	}

	private void copyPDFFile(String pdfResourcePath, String filePath,
			String pdfDocId) throws IOException {
		String pdfFileServerPath = ApplicationProperties
				.getProperty("cdrom.pdf.server.path");
		pdfResourcePath = pdfFileServerPath + pdfResourcePath + pdfDocId;
		File source = new File(pdfResourcePath);
		File dest = new File(filePath + "/pdf");
		FileUtils.copyFileToDirectory(source, dest);
	}

	public String getFolderPath(String timeStamp, String locale,
			String selectedModel, String manualType, String selectedVds,
			String selectedVinRange, String modelName, String networkPath) {

		String root = networkPath;

		selectedVds = selectedVds.replaceAll("[^a-zA-Z0-9]", "");
		String marketFolderName="";
		if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
				locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
		{
			// MC MARKET LOCALE
			marketFolderName="MC";
		}
		else
		{
			// MME MARKET LOCALE
			marketFolderName="MME";
		}
		String folderPath = root + "/CD_ROM_" + timeStamp + "/"+marketFolderName+"/" + locale
				+ "/" + modelName + "_" + selectedModel + "/"
				+ selectedVds + "/" + selectedVinRange + "/" + manualType;
		File path = new File(folderPath);
		if (!path.exists()) {
			try {
				FileUtils.forceMkdir(path);
			} catch (IOException e) {
				Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "getFolderPath()", e);
				return "";
			}
		}
		marketFolderName = null;
		return folderPath;
	}

	public String getFilePathForHTML(String locale, String selectedModel,
			String manualType, String selectedVds, String selectedVinRange,
			String modelName) {
		selectedVds = selectedVds.replaceAll("[^a-zA-Z0-9]", "");
		String marketFolderName="";
		if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
				locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
		{
			// MC MARKET LOCALE
			marketFolderName="MC";
		}
		else
		{
			// MME MARKET LOCALE
			marketFolderName="MME";
		}
		return marketFolderName+"/" + locale + "/" + modelName + "_"
				+ selectedModel + "/" + selectedVds + "/" + selectedVinRange
				+ "/" + manualType;
	}

	//String DJVUFilePath = "";
	//String pdfFileLocation = "";

	public void saveFile(String folderPath, String documentId, String xml,
			String locale, String selectedModel, KeyWordSearchBean keyBean,
			ViewContentBean viewBean, String timeStamp, String networkPath,
			String filePathForHTML, String manualType,
			List<InnerLinkReportBean> innerLinkReportBean,
			TransactionReportBean tr) throws Exception {
		String filePath = folderPath + "/" + documentId + ".html";
		ServiceManualDocment document = getParseddocument(xml);
		if (document != null) {
			if (StringUtils.isNotBlank(document.getDJVUFileLocation())) {
				tr.setDocumentId(documentId);
				tr.setManualType(manualType);
				tr.setDestinationLocation("");
				tr.setInnerLinkCount(0);
				tr.setAllInnerLinkMapped("");
				tr.setOkAssetCount(0);
				tr.setAllOkAssetMapped("");

				String fileLocation = document.getDJVUFileLocation();
				if (fileLocation.contains("WIRING DIAGRAMS")) {
					fileLocation = fileLocation.replace("WIRING DIAGRAMS",
							"WIRING_DIAGRAMS");
				}
				String filename = fileLocation;
				int index = fileLocation.lastIndexOf('/');
				fileLocation = fileLocation.substring(0, index);
					if(!djvuPathList.contains(fileLocation)){
						djvuPathList.add(fileLocation);
						copyDocuments(timeStamp, networkPath, fileLocation, locale, selectedModel);
					}
				if (StringUtils.isNotBlank(document.getAttachment())) {
					index = fileLocation.lastIndexOf('/');
					String pdfLocation = fileLocation.substring(0, index);
					pdfLocation = pdfLocation + "/pdf";
					if (!pdfPathList.contains(pdfLocation)) {
						pdfPathList.add(pdfLocation);
						copyPDFDocuments(timeStamp, networkPath, pdfLocation);
					}
				}
				tr.setDestinationLocation(fileLocation);
				if(null!=keyBean)
				{
					keyBean.setFilePath(filename.substring(1));
					keyBean.setGenerateDocument("No");
				}
				if(null!=viewBean)
				{
					viewBean.setFilePath(filename.substring(1));
					viewBean.setGenerateDocument("No");
				}
			} else if (document.getAttchments() != null && document.getAttchments().size()>0) {
				for (int i = 0; i < document.getAttchments().size(); i++) {
					if(isScheduleAborted()) { break; }
					LabelBean lb = (LabelBean) document.getAttchments()
							.get(i);
					String pdfResourcePath = CDRomDAO.getResourcePath(documentId,locale);
					if (StringUtils.isNotBlank(pdfResourcePath)) {
						tr.setDocumentId(documentId);
						tr.setManualType(manualType);
						tr.setDestinationLocation("");
						tr.setInnerLinkCount(0);
						tr.setAllInnerLinkMapped("");
						tr.setOkAssetCount(0);
						tr.setAllOkAssetMapped("");
						processPDFFiles(pdfResourcePath, folderPath,
								lb.getValue());
						tr.setDestinationLocation(filePathForHTML + "/pdf/"
								+ lb.getValue());
						if(null!=keyBean)
						{
							keyBean.setFilePath(filePathForHTML + "/pdf/"
									+ lb.getValue());
							keyBean.setGenerateDocument("No");
						}
						
						if(null!=viewBean)
						{
							viewBean.setFilePath(filePathForHTML + "/pdf/"
								+ lb.getValue());
							viewBean.setGenerateDocument("No");
						}
					}
				}
			} 
			else if (StringUtils.isNotBlank(document.getAttachment())) {
				String pdfFileName = document.getAttachment();
				String pdfResourcePath = CDRomDAO.getResourcePath(documentId,locale);
				if (StringUtils.isNotBlank(pdfResourcePath)) {
					tr.setDocumentId(documentId);
					tr.setManualType(manualType);
					tr.setDestinationLocation("");
					tr.setInnerLinkCount(0);
					tr.setAllInnerLinkMapped("");
					tr.setOkAssetCount(0);
					tr.setAllOkAssetMapped("");
					processPDFFiles(pdfResourcePath, folderPath, pdfFileName);
					tr.setDestinationLocation(filePathForHTML + "/pdf/"
							+ pdfFileName);
					if(null!=keyBean)
					{
						keyBean.setFilePath(filePathForHTML + "/pdf/" + pdfFileName);
						keyBean.setGenerateDocument("No");
					}
					
					if(null!=viewBean)
					{
						viewBean.setFilePath(filePathForHTML + "/pdf/"
								+ pdfFileName);
						viewBean.setGenerateDocument("No");
					}
				}
			}
			else 
			{
				LabelBean temp = processInnerLinks(document.getContent(),
						documentId, manualType, filePathForHTML,
						innerLinkReportBean);
				String processedContent = temp.getValue();
				List<String> okAssetList = new ArrayList<String>();
				LabelBean temp2 = createImagePathList(processedContent,
						documentId, okAssetList);
				processedContent = temp2.getValue();
				// read inline Content css except - mazda mc custom css
				LabelBean temp3 = createContentCSSPathsList(processedContent, documentId, okAssetList);
				processedContent = temp3.getValue();
				document.setContent(processedContent);
				
				
				Writer fileWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(filePath), "UTF-8"));
				// FileWriter fileWriter = new FileWriter(filePath);
				/*
				fileWriter.write("<style>");
				fileWriter.write(
						".documenttitle{ font-size: 20px; font-weight: bold; color: #ba2b2a; font-family: 'InterstateMazda-Light', Arial; line-height: 1.1; display: inline-block; margin-left: 10px; margin-top: 30px; width: 70%;}");
				fileWriter.write(
						".documentid {font-size: 14px; font-family: 'InterstateMazda-Light', Arial; display: block; color: #333333; font-weight: normal; margin-top: 15px;}");
				fileWriter.write(
						".printicon {width: 40px; text-align: center; height: 20px; border: 1px solid grey; border-radius: 3px; text-decoration: none !important;float: right; margin: 25px;}");
				fileWriter.write("</style>");
				*/
				/*
				 * INSTEAD OF GETTING IT FROM KEY BEAN
				 * USE FROM DOCUMENT OBJECT
				 */
				String keyBeanTitle="";
//				if(null!=keyBean && null!=keyBean.getTitle())
//				{
//					keyBeanTitle = keyBean.getTitle().trim();
//				}
				if(null!=document.getTitle() && !"".equals(document.getTitle()))
				{
					keyBeanTitle = document.getTitle().trim();
				}
				
				// based on Locale get print Value
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					msgProps = new MessageProperties(ApplicationProperties.getProperty("locales.values.japanese"));
				}
				else
				{
					// MME MARKET LOCALE
					msgProps = new MessageProperties(ApplicationProperties.getProperty("locales.values.english"));
				}
				
				
				fileWriter.write("<link href=\"../../../../../../css/main.css\" rel=\"stylesheet\">");
				fileWriter.write("<link href=\"../../../../../../css/font-awesome.min.css\" rel=\"stylesheet\">");
				fileWriter.write("<div class=\"documenttitle\">"+keyBeanTitle+"<span class=\"documentid\">"+documentId+"</span></div>");
				fileWriter.write("<a href=\"javascript:void(0);\" onclick=\"window.open('"+documentId+".html"+"','newwindow');return false\" title=\""+msgProps.getProperty("label.print.cdrom")+"\" class=\"printicon_custom\"></a>");
				fileWriter.write("<html><title>");
				fileWriter.write(document.getTitle());
				fileWriter
						.write("</title><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>");
				fileWriter.write(document.getContent());
				keyBeanTitle = null;
				if (document.getAttchments() != null
						&& document.getAttchments().size() > 0) {
					for (int i = 0; i < document.getAttchments().size(); i++) {
						if(isScheduleAborted()) { break; }
						LabelBean lb = (LabelBean) document.getAttchments()
								.get(i);
						fileWriter.write("<a href=" + '"' + "pdf/"
								+ lb.getValue() + '"' + ">" + lb.getKey()
								+ "</a></br>");
						String pdfResourcePath = CDRomDAO.getResourcePath(documentId,locale);
						if (StringUtils.isNotBlank(pdfResourcePath)) {
							processPDFFiles(pdfResourcePath, folderPath,
									lb.getValue());
						}
					}
				}
				fileWriter.write("</body></html>");
				fileWriter.flush();
				fileWriter.close();
				String innerLinkStatus = "Y";
				if (temp.getKey().equals("0")) {
					innerLinkStatus = "";
				}
				if(null!=keyBean)
				{
					keyBean.setGenerateDocument("Yes");
					keyBean.setFilePath(filePathForHTML);
				}
				
				if(null!=viewBean)
				{
					viewBean.setGenerateDocument("Yes");
					viewBean.setFilePath(filePathForHTML);
				}

				tr.setDocumentId(documentId);
				tr.setManualType(manualType);
				tr.setDestinationLocation(filePathForHTML);
				tr.setInnerLinkCount(Integer.parseInt(temp.getKey()));
				tr.setAllInnerLinkMapped(innerLinkStatus);
				// images count + css count 
//				tr.setOkAssetCount(Integer.parseInt(temp2.getKey())+ Integer.parseInt(temp3.getKey()));
				tr.setOkAssetCount(Integer.parseInt(temp2.getKey()));
				tr.setAllOkAssetMapped(findOkAsset(okAssetList));
				
				/*String printIcon = ApplicationProperties.getProperty("cdrom.japan.html.folder.path");
				printIcon = printIcon + "/images/print.png";
				File printIconCheck = new File(folderPath+"/print.png");
				File source = new File(printIcon);
				File dest = new File(folderPath);
				if(source.exists()){
					if(!printIconCheck.exists()){
						FileUtils.copyFileToDirectory(source, dest);
					}
				}*/
			}
		}
	}

	private void countOkAssetForWiringDiagram(String timeStamp, String networkPath) {
		if(djvuPathList.size()>0){
			for(String path: djvuPathList){
				if(isScheduleAborted()) { break; }
				String folderPath = networkPath + "/CD_ROM_" + timeStamp + path +"/image";
				File imagePath = new File(folderPath);
				if (imagePath.exists()) {
					okAssetCount  = okAssetCount + imagePath.list().length;
				}
				 folderPath = networkPath + "/CD_ROM_" + timeStamp + path +"/images";
				File imagsePath = new File(folderPath);
				if (imagsePath.exists()) {
					okAssetCount  = okAssetCount + imagsePath.list().length;
				}
			}
		}
				
	}

	private String findOkAsset(List<String> okAssetList) {
		if (okAssetList != null && okAssetList.size() > 0) {
			for (int i = 0; i < okAssetList.size(); i++) {
				if(isScheduleAborted()) { break; }
				String imagePath = ApplicationProperties
						.getProperty("cdrom.image.server.path")
						+ okAssetList.get(i);
				File image = new File(imagePath);
				if (!image.exists()) {
					return "N";
				}
			}
			return "Y";
		}
		return "";
	}

	private LabelBean createImagePathList(String content, String documentId,
			List<String> okAssetList) {
		Document doc = Jsoup.parse(content);
		Elements link = doc.getElementsByTag("img");
		int count = 0;
		if (link != null && link.size() > 0) {
			for (int i = 0; i < link.size(); i++) {
				if(isScheduleAborted()) { break; }
				Element el = link.get(i);
				String imagePath = el.attr("src");
				if (!imagePathList.contains(imagePath)) {
					imagePathList.add(imagePath + "," + documentId);
					okAssetList.add(imagePath);
					count++;
				}
				int index = imagePath.lastIndexOf("/");
				String newImagePath = "images" + imagePath.substring(index);
				content = content.replace(imagePath, newImagePath);
			}
		}
		LabelBean temp = new LabelBean();
		temp.setKey(String.valueOf(count));
		temp.setValue(content);
		return temp;
	}
	
	/*
	 * FUNCTION ADDED FOR TOYOTA CONTENT - ADD INLINE CONTENT CSS
	 * EXCEPT - mazda-css-jp.css (SINCE THIS CSS IS ADDED WITH ENT CONTENT, IT NEEDS TO BE AVOIDED)
	 */
	private LabelBean createContentCSSPathsList(String content, String documentId,
			List<String> okAssetList) {
		Document doc = Jsoup.parse(content);
		Elements link = doc.getElementsByTag("link");
		int count = 0;
		if (link != null && link.size() > 0) {
			for (int i = 0; i < link.size(); i++) {
				if(isScheduleAborted()) { break; }
				Element el = link.get(i);
				String cssPath = el.attr("href");
				if(null!=cssPath && !"".equals(cssPath))
				{
					if(!cssPath.trim().toLowerCase().endsWith(mazdaMCCSSName.trim().toLowerCase()))
					{
						if(!contentCSSPathsList.contains(cssPath))
						{
							contentCSSPathsList.add(cssPath +","+documentId);
							// do not add this to OkassetsList - so that okasset count does not gets disturbed.
//							okAssetList.add(cssPath);
							count++;
						}
						int index = cssPath.lastIndexOf("/");
						String newCSSPath = "contentcss" + cssPath.substring(index);
						content = content.replace(cssPath, newCSSPath);
						newCSSPath = null;
					}
				}
			}
		}
		LabelBean temp = new LabelBean();
		temp.setKey(String.valueOf(count));
		temp.setValue(content);
		return temp;

	}
	
	

	private LabelBean processInnerLinks(String content, String documentId,
			String manualType, String filePathForHTML,
			List<InnerLinkReportBean> innerLinkReportBean) {
		
		List<InnerLinkReportBean> otherManualLists = null;
		Document doc = Jsoup.parse(content);
		Elements link = doc.getElementsByTag("a");
		int count = 0;
		if (link != null && link.size() > 0) {
			for (int i = 0; i < link.size(); i++) {
				if(isScheduleAborted()) { break; }
				Element el = link.get(i);
				String linkHref = el.attr("href");
				/*
				 * NEW INNERLINK TYPE CATEGORY INTRODUCED FOR BOTH MC & MME MARKET AS BELOW - 
				 *  FOR MC - index?page=result_mc&startover=y&qstr=qstr&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&question_box={Tag ID}
				 *  FOR MME - index?page=result_mme&startover=y&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&qstr=qstr&question_box={Tag ID}
				 *  
				 *  FOR SUCH CASES - CONVERT THIS LINK TO DEAD LINK AND PASS AS IT IS
				 *  REPLACE HREF ATTRIBUTE WITH JAVASCRIPT:VOID(0);
				 *  
				 *  Date change - 21 May 2024
				 *  DO NOT CONVERT THESE TO DEAD LINKS, REPLACE THESE LINKS WITH HEADING TEXT
				 * 
				 */
				if(null!=linkHref && !"".equals(linkHref) && (linkHref.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("rimtool.othermanualink.mc.url.pattern").trim().toLowerCase()) || 
						linkHref.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("rimtool.othermanualink.mme.url.pattern").trim().toLowerCase()))) 
				{
					//el.removeAttr("href");
					//el.attr("href","javascript:void(0);");
					InnerLinkReportBean oth = new InnerLinkReportBean();
					oth.setSourceTag(el.outerHtml());
					oth.setContentToBeReplaced(el.html());
					if(null==otherManualLists || otherManualLists.size()<=0)
					{
						otherManualLists = new ArrayList<InnerLinkReportBean>();
					}
					otherManualLists.add(oth);
					oth = null;
				}
				else
				{
					String newHref = processHrefLink(linkHref);
					if (StringUtils.isNotBlank(newHref)) {
						if (!documentIdListFromFiles.contains(newHref)) {
							InnerLinkReportBean temp = new InnerLinkReportBean();
							temp.setSourceDocumentId(documentId);
							temp.setManualType(manualType);
							temp.setMappedDocumentHTMLFileName(newHref + ".html");
							temp.setMappedDocumentHTMLFilePath(filePathForHTML);
							temp.setMappingStatus("Y");
							temp.setDocumentId(newHref);
							documentIdListFromFiles.add(newHref);
							innerLinkReportBean.add(temp);
							count++;
						}
						/*
						 * replace element href value
						 */
						el.removeAttr("href");
						el.attr("href",newHref+".html");
//						content = content.replace(linkHref, newHref + ".html");
					}
				}
			}
		}
		
		// replace content with doc
		if(null!=doc)
		{
			content = doc.toString();
			if(null!=content && !"".equals(content) && null!=otherManualLists && otherManualLists.size()>0)
			{
				/*
				 * ITERATE OTHER MANUAL LINKS LITS AND CONVERT THEM TO TEXT LABELS
				 */
				InnerLinkReportBean oth = null;
				for(int q=0;q<otherManualLists.size();q++)
				{
					if(isScheduleAborted()) { break; }
					oth = (InnerLinkReportBean)otherManualLists.get(q);
					if(null!=oth.getSourceTag() && !"".equals(oth.getSourceTag()) && null!=oth.getContentToBeReplaced() && !"".equals(oth.getContentToBeReplaced()))
					{
//						logger.info("---- cd src to replace :: >"+ oth.getSourceTag());
						content = content.replace(oth.getSourceTag(), oth.getContentToBeReplaced());
					}
					oth = null;
				}
			}
		}
		otherManualLists = null;
		LabelBean temp = new LabelBean();
		temp.setKey(String.valueOf(count));
		temp.setValue(content);
		return temp;
	}

	private String processHrefLink(String nodeValue) {
		if (StringUtils.isNotBlank(nodeValue)) {
			// may be possibility that & must be coming as &amp;
			// replace it with & symbol before identifying innerLink Doc Id
			// best way is change &id to id=
			// as innerLink value will be = index?page=content&amp;id=SM2581168
//			nodeValue = nodeValue.replace("&amp;", "&");
//			int index = nodeValue.indexOf("&id", 0);
//			int index = nodeValue.indexOf("id=", 0);
//			if (index > 0) {
////				nodeValue = nodeValue.substring(index + 4);
//				nodeValue = nodeValue.substring(index + 3);
//				return nodeValue;
//			}
			if(nodeValue.indexOf("id=")!=-1)
			{
				nodeValue = nodeValue.substring(nodeValue.indexOf("id=")+3, nodeValue.length());
				return nodeValue;
			}
		}
		return "";

	}

	private ServiceManualDocment getParseddocument(String xml) throws Exception {
		ServiceManualDocment document = new ServiceManualDocment();
		if(StringUtils.isNotBlank(xml)){
			org.w3c.dom.Element root = getRootNode(xml);
			String rootNode = root.getNodeName();
			if (StringUtils.isNotBlank(rootNode)) {
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xPath = xpf.newXPath();
				NodeList nodesTitles = (NodeList) xPath.evaluate(
						"/" + rootNode.trim() + "/TITLE", new InputSource(
								new StringReader(xml)), XPathConstants.NODESET);
				NodeList nodesContent = (NodeList) xPath.evaluate(
						"/" + rootNode.trim() + "/CONTENT", new InputSource(
								new StringReader(xml)), XPathConstants.NODESET);
				for (int x = 0; x < nodesTitles.getLength(); x++) {
					if(isScheduleAborted()) { break; }
					Node titleNode = nodesTitles.item(x);
					document.setTitle(titleNode.getTextContent());
				}
				for (int x = 0; x < nodesContent.getLength(); x++) {
					if(isScheduleAborted()) { break; }
					Node contentNode = nodesContent.item(x);
					document.setContent(contentNode.getTextContent());
				}
				parseAttachment("/" + rootNode.trim(), document, xml, xPath);
				
				if (root.getElementsByTagName("DJVU_FILE_LOCATION") != null
						&& root.getElementsByTagName("DJVU_FILE_LOCATION")
								.getLength() > 0) {
					String fileLocation = root
							.getElementsByTagName("DJVU_FILE_LOCATION").item(0)
							.getTextContent();
					document.setDJVUFileLocation(fileLocation);
				}
				if (root.getElementsByTagName("ATTACHMENT") != null
						&& root.getElementsByTagName("ATTACHMENT").getLength() > 0) {
					String attachmentFileName = root
							.getElementsByTagName("ATTACHMENT").item(0)
							.getTextContent();
					document.setAttachment(attachmentFileName);
				}
			} else {
				document.setContent(StringUtils.EMPTY);
				document.setTitle(StringUtils.EMPTY);
			}
		}else{
			return null;
		}

		return document;
	}

	private void copyDocuments(String timeStamp, String networkPath, String fileLocation, String locale, String selectedModel)
			throws IOException {
		String folderPath = networkPath + "/CD_ROM_" + timeStamp + fileLocation;
		File path = new File(folderPath);
		String sourcePath = ApplicationProperties
				.getProperty("cdrom.server.path.name") + fileLocation;
		File source = new File(sourcePath);
		if (!path.exists()) {
			if (!path.mkdirs()) {
				logger.info("getFolderPath :: Failed to create directory");
			}else
				copyFolder(source, path);
				modifyWindowJs(folderPath, fileLocation, locale, selectedModel);
				/*
				 * CAL FUNCTION TO MODIFY voltageMAP & voltagelinkMAP JS Files
				 */
				modifyVoltageMapJs(folderPath, fileLocation, locale, selectedModel);
				
				modifyVoltageLinkMapJs(folderPath, fileLocation, locale, selectedModel);
				/*
				 * CALL FUNCTION TO MODIFY default_act_main.js
				 */
				modifyDefaultActMainJS(folderPath);
			}
		}

	private boolean modifyDefaultActMainJS(String folderPath)
	{
		try
		{
			File jsFile = new File(folderPath + "/default_act_main.js");
			if (!jsFile.exists()) {
				logger.info("default_act_main.js not exist.");
				return false;
			}
			String s;
			String totalStr = "";
			BufferedWriter fw = null;
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(jsFile), "UTF-8"));

				while ((s = br.readLine()) != null) {
					if(isScheduleAborted()) { break; }
					// replace all WIDTH="100%" TO WIDTH="800"
					//replace all HEIGHT="100%" TO HEIGHT="800"
//					s=s.replace("WIDTH=\"100%\"", "WIDTH=\"800\"");
					s=s.replace("HEIGHT=\"100%\"", "HEIGHT=\"650px\"");
					
//					s=s.replace("width=\"100%\"", "width=\"800\"");
					s=s.replace("height=\"100%\"", "height=\"650px\"");
					
					totalStr += s;
					totalStr = totalStr + "\n";
				}
				fw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(jsFile), "UTF-8"));
				fw.write(totalStr);
				fw.flush();
				fw.close();
				br.close();
			} finally {
				if (fw != null) {
					fw.close();
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyDefaultActMainJS()", e);
		}
		return true;
	}

	private boolean modifyWindowJs(String folderPath, String fileLocation, String locale, String selectedModel) {
		try {
			File jsFile = new File(folderPath + "/window.js");
			if (!jsFile.exists()) {
				logger.info("Window.js not exist.");
				return false;
			}
			
			int index = fileLocation.lastIndexOf('/');
			String pdfPathSearch = fileLocation.substring(0, index);
			pdfPathSearch = pdfPathSearch + "/pdf";
			String htmlPathSearch = fileLocation + "/";
			// PATH Has to be - /qokinfoctr/mazdagms3/index?page=detail_mc&id= or /qokinfoctr/mazdagms3/index?page=detail_mme&id=
			String infoCenterContext = ApplicationProperties.getProperty("application.infocenter.context");
			String infoCenterWebContext=ApplicationProperties.getProperty("application.infocenter.web.context");
//			String indexPath = infoCenterContext+"/index?page=detail_mc&id=";
			String indexPath = "";
			if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				indexPath = infoCenterContext+"/index?page=detail_mc&id=";
			}
			else
			{
				// MME MARKET LOCALE
				indexPath = infoCenterContext+"/index?page=detail_mme&id=";
			}
			
			
			String htmlReplace = "";
			String pdfReplace = "../pdf";
			String docFolder = folderPath.substring(0, folderPath.lastIndexOf('/'))+"/sm_doc";

			// FileReader fr = new FileReader(jsFile);
			String s;
			String totalStr = "";
			BufferedWriter fw = null;
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(jsFile), "UTF-8"));

				while ((s = br.readLine()) != null) {
					if(isScheduleAborted()) { break; }
					if(s.contains(indexPath)){
						try{
							int i = s.indexOf("&id");
							String docId = s.substring(i+4, s.indexOf("\"", i));
							String replaceStr = indexPath+docId;
							String xml = CDRomManualsDAO.getDocumentFromDocumentID(docId);
							ServiceManualDocment document = getParseddocument(xml);
							if(document !=null){
								boolean flag = saveDocument(document, docFolder, docId, locale, selectedModel);
								if(flag){
									s = s.replace(replaceStr, "../sm_doc/"+docId+".html");
								}
							}
						}catch(Exception e){
							Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyWindowJs()", e);
						}
					}
					totalStr += s;
					totalStr = totalStr + "\n";
				}
				
				// replace /dokweb from the library paths
				totalStr = totalStr.replaceAll(infoCenterWebContext, "");
				totalStr = totalStr.replaceAll(htmlPathSearch, htmlReplace);
				totalStr = totalStr.replaceAll(pdfPathSearch, pdfReplace);
				fw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(jsFile), "UTF-8"));
				fw.write(totalStr);
				fw.flush();
				fw.close();
				br.close();
			} finally {
				if (fw != null) {
					fw.close();
				}
			}

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyWindowJs()", e);
		}
		return true;
	}

	private boolean modifyVoltageMapJs(String folderPath, String fileLocation, String locale, String selectedModel) {
		try {
			File jsFile = null;
			boolean fileExists= false;
			if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				jsFile = new File(folderPath + "/js/voltageMAP.js");
				if (jsFile.exists()) {
					fileExists =  true;
				}
			}
			else
			{
				// MME MARKET LOCALE
				jsFile = new File(folderPath + "/js/voltageMAP.js");
				if(jsFile.exists())
				{
					fileExists =  true;
				}
				
				// check for voltageMAP_ECE
				if(fileExists==false)
				{
					jsFile = null;
					jsFile = new File(folderPath+"/js/voltageMAP_ECE.js");
					if(jsFile.exists())
					{
						fileExists = true;
					}
				}
				
				// check for voltageMAP_UK
				if(fileExists==false)
				{
					jsFile = new File(folderPath+"/js/voltageMAP_UK.js");
					if(jsFile.exists())
					{
						fileExists = true;
					}
				}
			}
			
			
//			File jsFile = new File(folderPath + "/js/VoltageMAP.js");
//			if (!jsFile.exists()) {
//				logger.info("VoltageLinkMAP.js not exist.");
//				return false;
//			}
			
			if(fileExists==true &&  null!=jsFile && jsFile.exists())
			{
				// PATH Has to be - /qokinfoctr/mazdagms3/index?page=detail_mc&id= /qokinfoctr/mazdagms3/index?page=detail_mme&id=
				String infoCenterContext = ApplicationProperties.getProperty("application.infocenter.context");
				String infoCenterWebContext=ApplicationProperties.getProperty("application.infocenter.web.context");
				String indexPath = "";
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					indexPath = infoCenterContext+"/index?page=detail_mc&id=";
				}
				else
				{
					// MME MARKET LOCALE
					indexPath = infoCenterContext+"/index?page=detail_mme&id=";
				}
				String docFolder = folderPath.substring(0, folderPath.lastIndexOf('/'))+"/sm_doc";

				// FileReader fr = new FileReader(jsFile);
				String s;
				String totalStr = "";
				BufferedWriter fw = null;
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(
							new FileInputStream(jsFile), "UTF-8"));

					while ((s = br.readLine()) != null) {
						if(isScheduleAborted()) { break; }
						if(s.contains(indexPath)){
							try{
								int i = s.indexOf("&id");
								String docId = s.substring(i+4, s.indexOf("'", i));
								String replaceStr = indexPath+docId;
								String xml = CDRomManualsDAO.getDocumentFromDocumentID(docId);
								ServiceManualDocment document = getParseddocument(xml);
								if(document !=null){
									boolean flag = saveDocument(document, docFolder, docId, locale, selectedModel);
									if(flag){
										s = s.replace(replaceStr, "../sm_doc/"+docId+".html");
									}
								}
							}catch(Exception e){
								Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyVoltageMapJs()", e);
							}
						}
						totalStr += s;
						totalStr = totalStr + "\n";
					}

					// replace /dokweb from the library paths
					totalStr = totalStr.replaceAll(infoCenterWebContext, "");
					fw = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(jsFile), "UTF-8"));
					fw.write(totalStr);
					fw.flush();
					fw.close();
					br.close();
				} finally {
					if (fw != null) {
						fw.close();
					}
				}
			}
			else
			{
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					logger.info("VoltageLinkMAP.js not exist.");
				}
				else
				{
					logger.info("VoltageLinkMAP.js / voltageMAP_ECE.js /  voltageMAP_UK.js not exist.");
				}
				return false;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyVoltageMapJs()", e);
			return false;
		}
		return true;
	}

	private boolean modifyVoltageLinkMapJs(String folderPath, String fileLocation, String locale, String selectedModel) {
		try {
			File jsFile = null;
			boolean fileExists= false;
			if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
					locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
			{
				// MC MARKET LOCALE
				jsFile = new File(folderPath + "/js/voltagelinkMAP.js");
				if (jsFile.exists()) {
					fileExists =  true;
				}
			}
			else
			{
				// MME MARKET LOCALE
				jsFile = new File(folderPath + "/js/voltagelinkMAP.js");
				if(jsFile.exists())
				{
					fileExists =  true;
				}
				
				// check for voltagelinkMAP_ECE
				if(fileExists==false)
				{
					jsFile = null;
					jsFile = new File(folderPath+"/js/voltagelinkMAP_ECE.js");
					if(jsFile.exists())
					{
						fileExists = true;
					}
				}
				
				// check for voltagelinkMAP_UK
				if(fileExists==false)
				{
					jsFile = new File(folderPath+"/js/voltagelinkMAP_UK.js");
					if(jsFile.exists())
					{
						fileExists = true;
					}
				}
			}
			
			
//			File jsFile = new File(folderPath + "/js/voltagelinkMAP.js");
//			if (!jsFile.exists()) {
//				logger.info("VoltageLinkMAP.js not exist.");
//				return false;
//			}
			
			if(fileExists == true && null!=jsFile && jsFile.exists())
			{
				// PATH Has to be - /qokinfoctr/mazdagms3/index?page=detail_mc&id=
				String infoCenterContext = ApplicationProperties.getProperty("application.infocenter.context");
				String infoCenterWebContext=ApplicationProperties.getProperty("application.infocenter.web.context");
				String indexPath = "";
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					// MC MARKET LOCALE
					indexPath = infoCenterContext+"/index?page=detail_mc&id=";
				}
				else
				{
					// MME MARKET LOCALE
					indexPath = infoCenterContext+"/index?page=detail_mme&id=";
				}
				String docFolder = folderPath.substring(0, folderPath.lastIndexOf('/'))+"/sm_doc";

				// FileReader fr = new FileReader(jsFile);
				String s;
				String totalStr = "";
				BufferedWriter fw = null;
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(
							new FileInputStream(jsFile), "UTF-8"));

					while ((s = br.readLine()) != null) {
						if(isScheduleAborted()) { break; }
						if(s.contains(indexPath)){
							try{
								int i = s.indexOf("&id");
								String docId = s.substring(i+4, s.indexOf("'", i));
								String replaceStr = indexPath+docId;
								String xml = CDRomManualsDAO.getDocumentFromDocumentID(docId);
								ServiceManualDocment document = getParseddocument(xml);
								if(document !=null){
									boolean flag = saveDocument(document, docFolder, docId, locale, selectedModel);
									if(flag){
										s = s.replace(replaceStr, "../sm_doc/"+docId+".html");
									}
								}
							}catch(Exception e){
								Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyVoltageLinkMapJs()", e);
							}
						}
						totalStr += s;
						totalStr = totalStr + "\n";
					}

					// replace /dokweb from the library paths
					totalStr = totalStr.replaceAll(infoCenterWebContext, "");
					fw = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(jsFile), "UTF-8"));
					fw.write(totalStr);
					fw.flush();
					fw.close();
					br.close();
				} finally {
					if (fw != null) {
						fw.close();
					}
				}
			}
			else
			{
				if(locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("ja_jp").trim().toLowerCase()) || 
						locale.replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_jp").trim().toLowerCase()))
				{
					logger.info("VoltageLinkMAP.js not exist.");
				}
				else
				{
					logger.info("VoltageLinkMAP.js / VoltageLinkMAP_ECE.js / VoltageLinkMAP_UK.js not exist.");
				}
				return false;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "modifyVoltageLinkMapJs()", e);
			return false;
		}
		return true;
	}

	
	private boolean saveDocument(ServiceManualDocment document, String docFolder, String docId, String locale, String selectedModel) {
		
		File path = new File(docFolder);
		if (!path.exists()) {
			try {
				FileUtils.forceMkdir(path);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		String filePath = docFolder + "/" + docId + ".html";

		Writer fileWriter;
		try {
			fileWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath), "UTF-8"));
		
		// FileWriter fileWriter = new FileWriter(filePath);
		String content = copyImagesForDocument(document.getContent(),docFolder);
		document.setContent(content);

		fileWriter.write("<html><title>");
		fileWriter.write(document.getTitle());
		fileWriter
				.write("</title><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>");
		fileWriter.write(document.getContent());
		if (document.getAttchments() != null
				&& document.getAttchments().size() > 0) {
			for (int i = 0; i < document.getAttchments().size(); i++) {
				if(isScheduleAborted()) { break; }
				LabelBean lb = (LabelBean) document.getAttchments()
						.get(i);
				fileWriter.write("<a href=" + '"' + "pdf/"
						+ lb.getValue() + '"' + ">" + lb.getKey()
						+ "</a></br>");
				String pdfResourcePath = CDRomDAO.getResourcePath(docId,locale);
				if (StringUtils.isNotBlank(pdfResourcePath)) {
					processPDFFiles(pdfResourcePath, docFolder,
							lb.getValue());
				}
			}
		}
		fileWriter.write("</body></html>");
		fileWriter.flush();
		fileWriter.close();
		}catch(Exception e){
			logger.info("saveDocument :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomManualsDAO.class.getName(),
					"saveDocument()", e);
			logger.info("saveDocument :: ################ Exception ################");
			return false;

		}
		return true;
	}

	private String copyImagesForDocument(String content, String docFolder) {
		File dest = new File(docFolder + "/images");
		Document doc = Jsoup.parse(content);
		Elements link = doc.getElementsByTag("img");
		if (link != null && link.size() > 0) {
			for (int i = 0; i < link.size(); i++) {
				if(isScheduleAborted()) { break; }
				Element el = link.get(i);
				String imagePath = el.attr("src");
				int index = imagePath.lastIndexOf("/");
				String newImagePath = "images" + imagePath.substring(index);
				content = content.replace(imagePath, newImagePath);
				String sourceImage = ApplicationProperties
						.getProperty("cdrom.image.server.path") + imagePath;
				File source = new File(sourceImage);
				try {
					FileUtils.copyFileToDirectory(source, dest);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}	
		return content;
	}

	private void copyPDFDocuments(String timeStamp, String networkPath, String pdfLocation)
			throws IOException {
		String folderPath = networkPath + "/CD_ROM_" + timeStamp
				+ pdfLocation;
		File path = new File(folderPath);
		String sourcePath = ApplicationProperties
				.getProperty("cdrom.server.path.name") + pdfLocation;
		File source = new File(sourcePath);
		if (!path.exists()) {
			if (!path.mkdirs()) {
				logger.info("getFolderPath :: Failed to create directory");
			}else{
				copyFolder(source, path);
			}
		}
		
	}

	public void copyFolder(File source, File destination) {
		if (source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}

			String files[] = source.list();

			for (String file : files) {
				if(isScheduleAborted()) { break; }
				File srcFile = new File(source, file);
				File destFile = new File(destination, file);

				copyFolder(srcFile, destFile);
			}
		} else {
			InputStream in = null;
			OutputStream out = null;

			try {
				logger.info("copyFolder :: Else Con :: Source File :: >"+ source.getAbsolutePath());
				logger.info("copyFolder :: Else Con :: Destination File :: >"+ destination.getAbsolutePath());
				in = new FileInputStream(source);
				out = new FileOutputStream(destination);

				byte[] buffer = new byte[1024];

				int length;
				while ((length = in.read(buffer)) > 0) {
					if(isScheduleAborted()) { break; }
					out.write(buffer, 0, length);
				}
			} catch (Exception e) {
				try {
					if(null!=in)
					{
						in.close();
					}
				} catch (IOException e1) {
					Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "copyFolder()", e1);
				}

				try {
					if(null!=out)
					{
						out.close();
					}
				} catch (IOException e1) {
					Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "copyFolder()", e1);
				}
			}
		}
	}

	private void parseAttachment(String root, ServiceManualDocment document,
			String xml, XPath xPath) throws Exception {
		NodeList attachments = (NodeList) xPath.evaluate(root + "/ATTACHMENTS",
				new InputSource(new StringReader(xml)), XPathConstants.NODESET);
		List<LabelBean> attachmentList = new ArrayList<LabelBean>();
		if (attachments != null && attachments.getLength() > 0) {
			for (int x = 0; x < attachments.getLength(); x++) {
				if(isScheduleAborted()) { break; }
				Node nNode = attachments.item(x);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					org.w3c.dom.Element eElement = (org.w3c.dom.Element) nNode;
					String title = eElement
							.getElementsByTagName("ATTACHMENT_TITLE").item(0)
							.getTextContent();
					String attachment = eElement
							.getElementsByTagName("ATTACHMENT").item(0)
							.getTextContent();
					/*
					 * add this only when attachment is not null
					 */
					if(null!=attachment && !"".equals(attachment))
					{
						LabelBean lb = new LabelBean();
						lb.setKey(title);
						lb.setValue(attachment);
						attachmentList.add(lb);
						lb = null;
					}
					attachment = null;
					title=  null;
				}
			}
		}
		document.setAttchments(attachmentList);
	}

	private org.w3c.dom.Element getRootNode(String xml) throws Exception{

		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xml));

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		DocumentBuilder db;
		org.w3c.dom.Element root = null;
		
		db = dbf.newDocumentBuilder();
		

		org.w3c.dom.Document doc = db.parse(is);

		root = doc.getDocumentElement();
		
//		try {
//			db = dbf.newDocumentBuilder();
//
//			org.w3c.dom.Document doc = db.parse(is);
//
//			root = doc.getDocumentElement();
//		} catch (ParserConfigurationException e) {
//			Utilities.printStackTraceToLogs(
//					CDRomStartConversionImpl.class.getName(), "getRootNode()",
//					e);
//		} catch (SAXException e) {
//			Utilities.printStackTraceToLogs(
//					CDRomStartConversionImpl.class.getName(), "getRootNode()",
//					e);
//		} catch (IOException e) {
//			Utilities.printStackTraceToLogs(
//					CDRomStartConversionImpl.class.getName(), "getRootNode()",
//					e);
//		} catch (Exception e) {
//			Utilities.printStackTraceToLogs(
//					CDRomStartConversionImpl.class.getName(), "getRootNode()",
//					e);
//		}

		return root;
	}
}
