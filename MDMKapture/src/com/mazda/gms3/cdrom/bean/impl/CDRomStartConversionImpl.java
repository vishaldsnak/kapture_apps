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
import com.mazda.gms3.mdm.utils.KaptureLibraryPath;
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

	/*
	 * KEYWORD + VIEW DETAILS, READ FOR A WHOLE MANUAL TYPE BEFORE ITS DOCUMENTS ARE CONVERTED (startConversion).
	 * Two queries per document were ~0.7 s each document on MC Dev. A document is taken out when used, so a
	 * second use (or an inner-link document, never prefetched) asks the DB per document as before.
	 * null map = that batch failed: per document as before.
	 */
	private java.util.Map<String, KeyWordSearchBean> prefetchedKeywords = null;
	private java.util.Set<String> prefetchedKeywordIds = new java.util.HashSet<String>();
	private java.util.Map<String, ViewContentBean> prefetchedViews = null;
	private java.util.Set<String> prefetchedViewIds = new java.util.HashSet<String>();

	/*
	 * DOES AN OK ASSET EXIST ON THE MOUNT - asked once per image path per job. Every document checked every
	 * image it uses with File.exists on /s3kapture (a round trip each), and workshop manual documents share
	 * many images.
	 */
	private final java.util.Map<String, Boolean> okAssetExists = new HashMap<String, Boolean>();

	/** time spent in findOkAsset for the document in flight (timing log in saveFileInFolder) */
	private long okAssetCheckMs = 0L;

	/* the job's DAO and the manual type in flight - copyDocuments shows "Folder Copy" on the History page */
	private CDRomDAO jobDao = null;
	private String currentDocType = null;
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
	/*
	 * 10 s FROM THE END OF THE LAST CHECK (2026-10-30). It was 500 ms from the START of the last check,
	 * but one check costs ~733 ms on MC Dev (pool connection + query, ~360 ms each), so every call went
	 * to the DB: every copied file, image, link and zip write paid 0.7 s (job 849: 733 ms per file, while
	 * the same copy outside Tomcat takes 1 ms). An abort is now noticed within ~11 s.
	 */
	private static final long ABORT_CHECK_INTERVAL_MS = 10000L;

	private long lastAbortCheckTime = 0L;

	private boolean scheduleAborted = false;

	private String abortScheduleId = null;

	/*
	 * Guards the abort-check fields - the copy threads ask too. NOT "synchronized" on this object:
	 * CDRomScheduleServlet runs the whole job inside synchronized (startConvImpl), so the copy threads
	 * would wait for that monitor while the job thread waits for them (MC Dev job 850 hung that way).
	 */
	private final Object abortCheckLock = new Object();

	private boolean isScheduleAborted()
	{
		synchronized (abortCheckLock)
		{
			return isScheduleAbortedLocked();
		}
	}

	private boolean isScheduleAbortedLocked()
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
		scheduleAborted = CDRomDAO.isAborted(abortScheduleId);
		// the next check is due ABORT_CHECK_INTERVAL_MS after this one ENDED
		lastAbortCheckTime = System.currentTimeMillis();
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
		jobDao = cdRomDao;
		currentDocType = docType;
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
			
				prefetchLookups(xmlDocumentList, locale, docType);
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Document conversion - " + docType + " (" + xmlDocumentList.size() + " documents)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.MIXED);
				try {
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
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
				}
				documentIdListFromManual.addAll(docIdList);
				if(docType.equalsIgnoreCase("WD") || docType.equalsIgnoreCase("e-WD")){
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Count WD images (folder listing) - " + docType, com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE);
					try {
						countOkAssetForWiringDiagram(timeStamp, networkPath);
					} finally {
						com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
					}
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
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Inner-link documents (read + convert) - " + docType, com.mazda.gms3.cdrom.bean.CDRomJobTimings.MIXED);
				try {
				processInnerLinkDocs(timeStamp, locale, carLineCode, docType,
						vdsCode, vinStartRange, modelName, scheduleId,
						networkPath, wmiCode, keywordSearchList,
						viewContentList, firstLevelCatList,
						manualTypeName, innerLinkReportBean,
						transactionReportBean, documentFailureReportBean,
						jobStatus,cdRomDao);
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
				}
				String folderPath = getFolderPath(timeStamp, locale,
						carLineCode, docType, vdsCode, vinStartRange,
						modelName, networkPath);
				// UPDATE TOTAL OKASSETS COUNT - OF ORIGINAL DOCS & INNERLINK DOCS
				cdRomDao.updateOKAssetCount(scheduleId, imagePathList.size(), docType);
				
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Image copy - " + docType, com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE);
				try {
					copyImages(folderPath, scheduleId, docType, okAssetReportBean,
							jobStatus,cdRomDao);
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
				}
				// CALL FUNCTION TO  COPY INLINE CONTENT CSS - TOYOTA CONTENT
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Content CSS copy - " + docType, com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE);
				try {
					copyContentCSS(folderPath, scheduleId, docType, okAssetReportBean, jobStatus);
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
				}
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
	 * WHERE THE "/content" SEGMENT LIVES. On the server the root ends "/content". Older content src
	 * is "/library/MAZDA/...", content sent by Job 10 / DMT is "/content/library/MAZDA/..."
	 * (kapture.library.path.context) - both exist side by side. The context prefix is dropped
	 * first (KaptureLibraryPath.withoutKaptureContext), so root + path is always
	 * ".../content/library/MAZDA" - a SINGLE "/content". Do NOT drop "/content" from the root.
	 *
	 * Every OK asset join goes through here (images, css, findOkAsset, copyImagesForDocument).
	 */
	private String resolveAssetPath(String assetRoot, String pathInContent) {
		String root = (null == assetRoot ? "" : assetRoot.trim());
		String path = (null == pathInContent ? "" : KaptureLibraryPath.withoutKaptureContext(pathInContent.trim()));
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
		long imagesStarted = System.currentTimeMillis();
		int imagesTotal = (null == imagePathList ? 0 : imagePathList.size());
		if (imagePathList != null && imagePathList.size() > 0) {
			cdRomDao.updateCurrentProcessingStatus(
					String.valueOf(scheduleId),
					ApplicationProperties
							.getProperty("schedule.current.status.okasset.value"),
					docType);
			logger.info(" Start OKAssetProcessing for manual type: " + docType);
			/*
			 * EACH DISTINCT IMAGE IS COPIED ONCE, cdrom.copy.parallel.threads AT A TIME (2026-10-30).
			 * The list has one entry per image per document ("path,documentId" - the duplicate check in
			 * createImagePathList compares the bare path, so it never matches) and every entry was copied
			 * again, one after another, ~0.7 s each on the /s3kapture mount. The report keeps one row per
			 * entry and the counts move per entry exactly as before; an entry shares the outcome of its
			 * image's one copy. Entries not reached after an abort get no row, as before.
			 */
			String imageRoot = ApplicationProperties.getProperty("cdrom.image.server.path");
			File dest = new File(folderPath + "/images");
			if (!dest.isDirectory() && !dest.mkdirs() && !dest.isDirectory()) {
				logger.info("copyImages :: could not create {" + dest.getAbsolutePath() + "} - every copy below fails.");
			}
			final java.util.LinkedHashMap<String, Integer> pairOfPath = new java.util.LinkedHashMap<String, Integer>();
			final List<File[]> pairs = new ArrayList<File[]>();
			final List<Integer> entriesOfPair = new ArrayList<Integer>();
			int[] entryPair = new int[imagePathList.size()];
			String[] entryImagePath = new String[imagePathList.size()];
			for (int i = 0; i < imagePathList.size(); i++) {
				String arr[] = imagePathList.get(i).split(",");
				String imagePath = resolveAssetPath(imageRoot, arr[0]);
				entryImagePath[i] = imagePath;
				Integer pair = pairOfPath.get(imagePath);
				if (null == pair) {
					pair = Integer.valueOf(pairs.size());
					pairOfPath.put(imagePath, pair);
					pairs.add(new File[] { new File(imagePath), dest });
					entriesOfPair.add(Integer.valueOf(0));
				}
				entryPair[i] = pair.intValue();
				entriesOfPair.set(pair.intValue(), Integer.valueOf(entriesOfPair.get(pair.intValue()).intValue() + 1));
			}
			logger.info("copyImages :: manual type " + docType + " :: " + imagePathList.size() + " image entries, "
					+ pairs.size() + " distinct images to copy");
			final String countScheduleId = scheduleId;
			final String countDocType = docType;
			final CDRomDAO countDao = cdRomDao;
			CopyOutcome[] outcomes = copyFiles(pairs, true, new CopyListener() {
				public void copied(int pair, Exception error) {
					// the History counts move as the copies finish, once per entry as before
					int entries = entriesOfPair.get(pair).intValue();
					try {
						if (null == error) {
							countDao.updateOKAssetProcessingCount(countScheduleId, countDocType, entries);
						} else {
							for (int e = 0; e < entries; e++) {
								countDao.updateOKAssetFailureCount(countScheduleId, countDocType);
							}
						}
					} catch (SQLException e) {
						Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "copyImages()", e);
					}
				}
			});
			for (int i = 0; i < imagePathList.size(); i++) {
				CopyOutcome outcome = outcomes[entryPair[i]];
				if (null == outcome) {
					// not copied - the schedule was aborted
					continue;
				}
				String arr[] = imagePathList.get(i).split(",");
				String okAssetName = arr[0].substring(
						arr[0].lastIndexOf('/') + 1, arr[0].length());
				OKAssetReportBean temp = new OKAssetReportBean();
				temp.setDocumentId(arr[1]);
				temp.setManualType(docType);
				temp.setOkAssetName(okAssetName);
				temp.setOkAssetSourceLocation(entryImagePath[i]);
				if (null == outcome.error) {
					temp.setStatus("Success");
					temp.setOkAssetDestinationLocation(folderPath + "/images");
					temp.setErrorCode("");
					temp.setErrorMessage("");
					temp.setErrorTime("");
				} else {
					temp.setStatus("Failure");
					temp.setErrorCode(outcome.error.getClass().getName());
					temp.setErrorMessage(outcome.error.getMessage());
					temp.setErrorTime(String.valueOf(outcome.finishedAt));
				}
				okAssetReportBean.add(temp);
			}
		}
		if(docType.equalsIgnoreCase("WD") || docType.equalsIgnoreCase("e-WD")){
			cdRomDao.updateOKAssetProcessingCount(scheduleId, docType, okAssetCount);
		}
		logger.info(" End OKAssetProcessing for manual type: " + docType + " :: " + imagesTotal
				+ " images in " + (System.currentTimeMillis() - imagesStarted) + " ms");
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
					long opStarted = System.currentTimeMillis();
					FileUtils.copyFileToDirectory(source, dest);
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File copy - content CSS (one file)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, source.length());
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
			/*
			 * ONE ROUND OF LINKED DOCUMENTS (2026-10-30): the DB lookups of the round are batched - published
			 * versions (any locale, as getDocumentsFromDocList) and keyword + view details - instead of one
			 * round trip each per document (831 documents = 47 min on MC Dev, job 852). The XML of each
			 * document is still read on its own; levels, order, counts and reports are unchanged.
			 * A failed batch falls back to the per-document lookups.
			 */
			java.util.Map<String, com.mazda.gms3.mdm.dao.KapturePublishedContentDAO.PublishedVersion> roundVersions;
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Inner-link documents - published versions (Kapture CMS DB, batched) - " + docType, com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB);
			try {
				roundVersions = com.mazda.gms3.mdm.dao.KapturePublishedContentDAO.findLatestPublishedBatch(newDocList, null);
			} finally {
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
			}
			prefetchLookups(newDocList, locale, docType, "Inner-link documents - keyword + view details lookup, batched - " + docType);
			logger.info("processInnerLinkDocs :: " + docType + " :: round of " + newDocList.size() + " linked documents"
					+ (null == roundVersions ? " - published versions BATCH FAILED, one lookup per document" : ""));
			for (String docId : newDocList) {
				if(isScheduleAborted()) { break; }
				List<LabelBean> newXmlDocumentList;
				if (null == roundVersions) {
					newXmlDocumentList = CDRomManualsDAO
							.getDocumentsFromDocList(docId, locale, carLineCode,
									vdsCode, vinStartRange);
				} else {
					newXmlDocumentList = new ArrayList<LabelBean>();
					long opStarted = System.currentTimeMillis();
					LabelBean clDetails = (null == docId || "".equals(docId.trim())) ? null
							: com.mazda.gms3.cdrom.dao.CDRomKaptureContentDAO.getPublishedChannelNode(docId.trim(), null,
									roundVersions.get(docId.trim().toLowerCase()));
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File read - inner-link document XML (one file)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE,
							System.currentTimeMillis() - opStarted, null == clDetails || null == clDetails.getValue() ? 0L : clDetails.getValue().length());
					if (null != clDetails) {
						newXmlDocumentList.add(clDetails);
					}
				}
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

	/** Keyword + view details of every document of the manual type, two batch reads - see prefetchedKeywords. */
	private void prefetchLookups(List<LabelBean> xmlDocumentList, String locale, String docType) {
		List<String> ids = new ArrayList<String>();
		if (null != xmlDocumentList) {
			for (LabelBean xmlDoc : xmlDocumentList) {
				if (null != xmlDoc.getExtraAttribute() && !"".equals(xmlDoc.getExtraAttribute().trim())) {
					ids.add(xmlDoc.getExtraAttribute().trim());
				}
			}
		}
		prefetchLookups(ids, locale, docType, "Keyword + view details lookup, batched - " + docType);
	}

	/** Keyword + view details of these documents in batched queries (replaces the previous prefetch). */
	private void prefetchLookups(List<String> ids, String locale, String docType, String phase) {
		prefetchedKeywordIds.clear();
		prefetchedViewIds.clear();
		com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin(phase, com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB);
		try {
			prefetchedKeywords = CDRomManualsDAO.getKeyWordSearchDetailsBatch(ids, locale, docType);
			prefetchedViews = CDRomManualsDAO.getViewDetailsFromCDRomBatch(ids, locale);
		} finally {
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
		}
		for (String id : ids) {
			if (null != prefetchedKeywords) {
				prefetchedKeywordIds.add(id.toLowerCase());
			}
			if (null != prefetchedViews) {
				prefetchedViewIds.add(id.toLowerCase());
			}
		}
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
				long lookupStarted = System.currentTimeMillis();
				String lookupKey = (null == documentId ? "" : documentId.trim().toLowerCase());
				if (prefetchedKeywordIds.remove(lookupKey)) {
					keyBean = prefetchedKeywords.remove(lookupKey);
				} else {
					long opStarted = System.currentTimeMillis();
					keyBean = CDRomManualsDAO.getKeyWordSearchDetails(documentId,
							locale,  manualType);
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - keyword details, one document", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis() - opStarted, 0L);
				}
				if (prefetchedViewIds.remove(lookupKey)) {
					viewBean = prefetchedViews.remove(lookupKey);
				} else {
					long opStarted = System.currentTimeMillis();
					viewBean = CDRomManualsDAO.getViewDetailsFromCDRom(documentId,locale);
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("DB - view details, one document", com.mazda.gms3.cdrom.bean.CDRomJobTimings.DB, System.currentTimeMillis() - opStarted, 0L);
				}
				long saveStarted = System.currentTimeMillis();
				okAssetCheckMs = 0L;
				saveFile(filePath, documentId, xml, locale, selectedModel,
						keyBean, viewBean, timeStamp, networkPath,
						filePathForHTML, manualType, innerLinkReportBean, tr);
				logger.info("saveFileInFolder :: {" + documentId + "} :: lookups " + (saveStarted - lookupStarted)
						+ " ms, conversion + write " + (System.currentTimeMillis() - saveStarted) + " ms (image checks "
						+ okAssetCheckMs + " ms, " + tr.getOkAssetCount() + " images)");
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

	private static String publishedVersionFolder(String documentId, String locale) {
		return com.mazda.gms3.mdm.dao.KapturePublishedContentDAO.findContentFolder(documentId, locale.trim().replace("-", "_"));
	}

	private void processPDFFiles(String pdfResourcePath, String filePath,
			String pdfDocId) throws IOException {
		// pdfResourcePath = published version folder of the document (see publishedVersionFolder): the attachments sit beside the content XML
		copyPDFFile(pdfResourcePath, filePath, pdfDocId);
	}

	private void copyPDFFile(String pdfResourcePath, String filePath,
			String pdfDocId) throws IOException {
		File source = new File(pdfResourcePath, pdfDocId);
		File dest = new File(filePath + "/pdf");
		long opStarted = System.currentTimeMillis();
		FileUtils.copyFileToDirectory(source, dest);
		com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File copy - PDF attachment (one file)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, source.length());
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
		long opStarted = System.currentTimeMillis();
		File path = new File(folderPath);
		if (!path.exists()) {
			try {
				FileUtils.forceMkdir(path);
			} catch (IOException e) {
				Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "getFolderPath()", e);
				return "";
			}
		}
		com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("Folder - exists check of the document folder", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, 0L);
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
				// OEM document: its whole material folder (see getParseddocument); DJVU: the file's own folder
				fileLocation = StringUtils.isNotBlank(document.getOemFolder()) ? document.getOemFolder() : fileLocation.substring(0, index);
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
					String pdfResourcePath = publishedVersionFolder(documentId,locale);
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
				String pdfResourcePath = publishedVersionFolder(documentId,locale);
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
				
				
				long writeStarted = System.currentTimeMillis();
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
						String pdfResourcePath = publishedVersionFolder(documentId,locale);
						if (StringUtils.isNotBlank(pdfResourcePath)) {
							processPDFFiles(pdfResourcePath, folderPath,
									lb.getValue());
						}
					}
				}
				fileWriter.write("</body></html>");
				fileWriter.flush();
				fileWriter.close();
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File write - document HTML (create + write + close)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE,
						System.currentTimeMillis() - writeStarted, new File(filePath).length());
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
		long started = System.currentTimeMillis();
		try {
			if (okAssetList != null && okAssetList.size() > 0) {
				String imageRoot = ApplicationProperties.getProperty("cdrom.image.server.path");
				for (int i = 0; i < okAssetList.size(); i++) {
					if(isScheduleAborted()) { break; }
					String imagePath = resolveAssetPath(imageRoot, okAssetList.get(i));
					Boolean exists = okAssetExists.get(imagePath);
					if (null == exists) {
						long opStarted = System.currentTimeMillis();
						exists = Boolean.valueOf(new File(imagePath).exists());
						com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File exists check - OK asset (first time per image)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, 0L);
						okAssetExists.put(imagePath, exists);
					}
					if (!exists.booleanValue()) {
						return "N";
					}
				}
				return "Y";
			}
			return "";
		} finally {
			okAssetCheckMs += System.currentTimeMillis() - started;
		}
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
		/*
		 * KAPTURE LINK FORMS (2026-10-30, as the Bulk Extract job): content loaded by Job 10 / DMT carries
		 * Kapture links, older content may still carry the InfoManager ones - BOTH are handled the legacy way:
		 *   document link     Kapture  content/<id>[#anchor] (cdrom.innerlink.kapture.pattern, group 1 = id)
		 *                     legacy   ...index?page=content&id=<id> (processHrefLink, unchanged)
		 *                     -> <id>.html[#anchor] + the document added to the CD when it is not in it
		 *   other manual link Kapture  result_mc?question_box= / result_mme?question_box= (cdrom.othermanualink.kapture.*)
		 *                     legacy   rimtool.othermanualink.mc / .mme (unchanged)
		 *                     -> its text
		 * content/ with no id (the document was not found when the content was loaded) is left as it is.
		 */
		java.util.regex.Pattern kaptureInnerLink = kaptureInnerLinkPattern();
		String kaptureOtherManualMc = ApplicationProperties.requireProperty("cdrom.othermanualink.kapture.mc.url.pattern").toLowerCase();
		String kaptureOtherManualMme = ApplicationProperties.requireProperty("cdrom.othermanualink.kapture.mme.url.pattern").toLowerCase();
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
						linkHref.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("rimtool.othermanualink.mme.url.pattern").trim().toLowerCase()) ||
						linkHref.trim().toLowerCase().startsWith(kaptureOtherManualMc) ||
						linkHref.trim().toLowerCase().startsWith(kaptureOtherManualMme)))
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
					String anchor = "";
					String newHref;
					String[] kaptureLink = kaptureDocumentLink(linkHref, kaptureInnerLink);
					if (null != kaptureLink) {
						newHref = kaptureLink[0];
						anchor = kaptureLink[1];
					} else {
						newHref = processHrefLink(linkHref);
					}
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
						el.attr("href",newHref+".html"+anchor);
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

	/** cdrom.innerlink.kapture.pattern / cdrom.wd.reflinks.kapture.pattern - compiled once per job */
	private java.util.regex.Pattern kaptureInnerLinkPattern;
	private java.util.regex.Pattern kaptureJsLinkPattern;

	private java.util.regex.Pattern kaptureInnerLinkPattern() {
		if (null == kaptureInnerLinkPattern) {
			kaptureInnerLinkPattern = java.util.regex.Pattern.compile(
					ApplicationProperties.requireProperty("cdrom.innerlink.kapture.pattern"), java.util.regex.Pattern.CASE_INSENSITIVE);
		}
		return kaptureInnerLinkPattern;
	}

	/**
	 * Kapture link to another document: content/SM252326 -> {SM252326, ""}, content/SM252326#id0411 ->
	 * {SM252326, "#id0411"}. null when the href is not one, or carries no document id (content/ = the
	 * document was not found when the content was loaded).
	 */
	private static String[] kaptureDocumentLink(String href, java.util.regex.Pattern innerLinkPattern) {
		if (null == href) {
			return null;
		}
		String value = href.trim();
		java.util.regex.Matcher m = innerLinkPattern.matcher(value);
		if (!m.find()) {
			return null;
		}
		int hash = value.indexOf('#', m.end(1));
		return new String[] { m.group(1), hash < 0 ? "" : value.substring(hash) };
	}

	/**
	 * KAPTURE DOCUMENT LINKS IN A WD JS LINE (window.js / voltage maps, MC + MME, 2026-10-30, as the Bulk
	 * Extract job): DMT writes mazdagms3/content/<id> (cdrom.wd.reflinks.kapture.pattern, with or without a
	 * leading "/") where InfoManager had <context>/index?page=detail_mc|detail_mme&id=<id> (handled by the
	 * caller, unchanged). Each one is handled as the legacy form: the document is saved into sm_doc and the
	 * link points to it. mazdagms3/content/ with no id (not mapped by DMT) is left as it is.
	 */
	private String rewriteKaptureJsLinks(String line, String docFolder, String locale, String selectedModel, String caller) {
		if (null == kaptureJsLinkPattern) {
			String key = ApplicationProperties.requireProperty("cdrom.wd.reflinks.kapture.pattern");
			if (key.startsWith("/")) {
				key = key.substring(1);
			}
			kaptureJsLinkPattern = java.util.regex.Pattern.compile(
					"/?" + java.util.regex.Pattern.quote(key) + "([A-Za-z0-9_\\-]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		}
		java.util.regex.Matcher m = kaptureJsLinkPattern.matcher(line);
		StringBuffer out = null;
		while (m.find()) {
			if (null == out) {
				out = new StringBuffer(line.length() + 64);
			}
			String docId = m.group(1);
			String replacement = m.group(0);
			try {
				String xml = CDRomManualsDAO.getDocumentFromDocumentID(docId);
				ServiceManualDocment document = getParseddocument(xml);
				if (document != null && saveDocument(document, docFolder, docId, locale, selectedModel)) {
					replacement = "../sm_doc/" + docId + ".html";
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), caller, e);
			}
			m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(replacement));
		}
		if (null == out) {
			return line;
		}
		m.appendTail(out);
		return out.toString();
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
					// /content/library/... (Job 10 / DMT) -> /library/...: the disk joins in copyDocuments /
					// copyPDFDocuments and the CD folder layout + links keep the library/... form
					document.setDJVUFileLocation(KaptureLibraryPath.withoutKaptureContext(fileLocation));
				}
				/*
				 * OEM DOCUMENTS (2026-10-30). For MC OSM / SM of an OEM model (Isuzu, Toyota, Nissan, Suzuki ...) DMT does not
				 * load the HTML into Kapture: it copies the material folder to OKAssets and the document carries only
				 * OEM_FILE_LOCATION (no CONTENT). Handled like a DJVU document (saveFile): the folder is copied into the CD and
				 * search / table of contents open the HTML itself - but the whole MATERIAL folder is copied, as DMT copied it,
				 * because the HTML reaches its images / css in the sibling folders.
				 */
				if (StringUtils.isBlank(document.getDJVUFileLocation()) && root.getElementsByTagName("OEM_FILE_LOCATION") != null
						&& root.getElementsByTagName("OEM_FILE_LOCATION").getLength() > 0) {
					String oemLocation = KaptureLibraryPath.withoutKaptureContext(
							root.getElementsByTagName("OEM_FILE_LOCATION").item(0).getTextContent().trim()).replace("\\", "/");
					if (StringUtils.isNotBlank(oemLocation)) {
						document.setDJVUFileLocation(oemLocation);
						document.setOemFolder(oemMaterialFolder(oemLocation));
					}
				}
				if (root.getElementsByTagName("ATTACHMENT") != null
						&& root.getElementsByTagName("ATTACHMENT").getLength() > 0) {
					String attachmentFileName = Utilities.getAttachmentFileName(
							(org.w3c.dom.Element) root.getElementsByTagName("ATTACHMENT").item(0));
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

	/**
	 * /library/MAZDA/<CHANNEL>/<locale>/<model>/<manual>/<facelift>/<material>/.../x.html -> the path up to <material>
	 * (8 folders), or the file's own folder when the path is shorter.
	 */
	private static String oemMaterialFolder(String oemLocation) {
		String[] seg = oemLocation.replaceAll("^/+", "").split("/");
		int keep = Math.min(8, seg.length - 1);
		StringBuilder folder = new StringBuilder();
		for (int i = 0; i < keep; i++) {
			folder.append("/").append(seg[i]);
		}
		return folder.toString();
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
			}else {
				/*
				 * THE WHOLE SHARED FOLDER IS COPIED BEFORE THE FIRST DOCUMENT OF IT IS COUNTED, so the History
				 * page shows "Folder Copy" meanwhile (2026-10-30) - processed counts stay put until it is done.
				 */
				setCurrentStatus("schedule.current.status.folder.copy.value");
				long copyStarted = System.currentTimeMillis();
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Folder copy - shared WD / e-WD folder", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE);
				try {
					copyFolder(source, path);
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
				}
				logger.info("copyDocuments :: folder {" + sourcePath + "} copied in "
						+ (System.currentTimeMillis() - copyStarted) + " ms");
			}
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("WD JS link rewrite (window.js, voltage maps)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.MIXED);
				try {
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
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
					setCurrentStatus("schedule.current.status.document.value");
				}
			}
		}

	/** Current status of the job and its manual type on the History page (a property key); never stops the job. */
	private void setCurrentStatus(String statusKey) {
		if (null == jobDao || null == abortScheduleId) {
			return;
		}
		try {
			jobDao.updateCurrentProcessingStatus(abortScheduleId, ApplicationProperties.getProperty(statusKey), currentDocType);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(), "setCurrentStatus()", e);
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
					if (!s.contains(indexPath)) {
						s = rewriteKaptureJsLinks(s, docFolder, locale, selectedModel, "modifyWindowJs()");
					}
					totalStr += s;
					totalStr = totalStr + "\n";
				}
				
				// replace /dokweb from the library paths
				// DMT window.js carries "/content/library/...", legacy ones "/library/..." - bring both
				// to "/library/..." so the search strings below (built from DJVU_FILE_LOCATION) match
				totalStr = KaptureLibraryPath.quotedAddressesWithoutKaptureContext(totalStr);
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
						if (!s.contains(indexPath)) {
							s = rewriteKaptureJsLinks(s, docFolder, locale, selectedModel, "modifyVoltageMapJs()");
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
						if (!s.contains(indexPath)) {
							s = rewriteKaptureJsLinks(s, docFolder, locale, selectedModel, "modifyVoltageLinkMapJs()");
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
				String pdfResourcePath = publishedVersionFolder(docId,locale);
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
				String sourceImage = resolveAssetPath(ApplicationProperties
						.getProperty("cdrom.image.server.path"), imagePath);
				File source = new File(sourceImage);
				try {
					long opStarted = System.currentTimeMillis();
					FileUtils.copyFileToDirectory(source, dest);
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("File copy - image of a WD JS document (one file)", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, source.length());
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
				setCurrentStatus("schedule.current.status.folder.copy.value");
				com.mazda.gms3.cdrom.bean.CDRomJobTimings.begin("Folder copy - shared PDF folder", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE);
				try {
					copyFolder(source, path);
				} finally {
					com.mazda.gms3.cdrom.bean.CDRomJobTimings.end();
					setCurrentStatus("schedule.current.status.document.value");
				}
			}
		}
		
	}

	/**
	 * COPIES A FOLDER TREE: directories are created first, then the files are copied
	 * cdrom.copy.parallel.threads at a time (2026-10-30). On the /s3kapture NFS mount each file costs
	 * ~0.7 s whatever its size (MC Dev job 849: 5,220 e-WD files = 64 min one after another).
	 */
	public void copyFolder(File source, File destination) {
		List<File[]> pairs = new ArrayList<File[]>();
		collectFiles(source, destination, pairs);
		copyFiles(pairs, false, null);
	}

	private void collectFiles(File source, File destination, List<File[]> pairs) {
		long opStarted = System.currentTimeMillis();
		if (source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}
			String files[] = source.list();
			com.mazda.gms3.cdrom.bean.CDRomJobTimings.op("Folder - list source + create target directory", com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, System.currentTimeMillis() - opStarted, 0L);
			if (null == files) {
				return;
			}
			for (String file : files) {
				if(isScheduleAborted()) { break; }
				collectFiles(new File(source, file), new File(destination, file), pairs);
			}
		} else {
			pairs.add(new File[] { source, destination });
		}
	}

	/** How one copy ended: error null = copied. */
	private static class CopyOutcome {
		Exception error;
		long finishedAt;
	}

	/** Told about every finished copy, on the copying thread. */
	private interface CopyListener {
		void copied(int pair, Exception error);
	}

	/**
	 * COPIES {source, target} PAIRS, cdrom.copy.parallel.threads AT A TIME, on threads of this job only
	 * (named after it, so its log lines stay together). intoDirectory: target is a directory and the file
	 * keeps its name (FileUtils.copyFileToDirectory, as copyImages always did); else target is the file.
	 * Stops taking new files once the schedule is aborted.
	 *
	 * @return one outcome per pair; null = not copied (aborted)
	 */
	private CopyOutcome[] copyFiles(final List<File[]> pairs, final boolean intoDirectory, final CopyListener listener) {
		final CopyOutcome[] outcomes = new CopyOutcome[pairs.size()];
		if (pairs.isEmpty()) {
			return outcomes;
		}
		int threads = Math.min(ApplicationProperties.requirePositiveInt("cdrom.copy.parallel.threads"), pairs.size());
		final String jobThread = Thread.currentThread().getName();
		final java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger(0);
		long started = System.currentTimeMillis();
		Thread[] workers = new Thread[threads];
		for (int t = 0; t < threads; t++) {
			workers[t] = new Thread(new Runnable() {
				public void run() {
					while (true) {
						int i = next.getAndIncrement();
						if (i >= pairs.size() || isScheduleAborted()) {
							return;
						}
						File source = pairs.get(i)[0];
						File target = pairs.get(i)[1];
						CopyOutcome outcome = new CopyOutcome();
						long opStarted = System.currentTimeMillis();
						try {
							if (intoDirectory) {
								FileUtils.copyFileToDirectory(source, target);
							} else {
								logger.info("copyFolder :: Source File :: >" + source.getAbsolutePath()
										+ " :: Destination File :: >" + target.getAbsolutePath());
								java.nio.file.Files.copy(source.toPath(), target.toPath(),
										java.nio.file.StandardCopyOption.REPLACE_EXISTING);
							}
						} catch (Exception e) {
							outcome.error = e;
							Utilities.printStackTraceToLogs(CDRomStartConversionImpl.class.getName(),
									intoDirectory ? "processImages()" : "copyFolder()", e);
						}
						outcome.finishedAt = new Date().getTime();
						long opMs = outcome.finishedAt - opStarted;
						long bytes = (null == outcome.error) ? (intoDirectory ? new File(target, source.getName()).length() : target.length()) : 0L;
						com.mazda.gms3.cdrom.bean.CDRomJobTimings.op(intoDirectory ? "File copy - image (one file)" : "File copy - folder copy (one file)",
								com.mazda.gms3.cdrom.bean.CDRomJobTimings.FILE, opMs, bytes);
						outcomes[i] = outcome;
						if (null != listener) {
							listener.copied(i, outcome.error);
						}
					}
				}
			}, jobThread + "_copy" + (t + 1));
			workers[t].start();
		}
		for (Thread worker : workers) {
			try {
				worker.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.info("copyFiles :: interrupted while waiting for the copy threads.");
				break;
			}
		}
		int copied = 0;
		int failed = 0;
		for (CopyOutcome outcome : outcomes) {
			if (null != outcome) {
				if (null == outcome.error) {
					copied++;
				} else {
					failed++;
				}
			}
		}
		logger.info("copyFiles :: " + pairs.size() + " files on " + threads + " threads :: " + copied + " copied, "
				+ failed + " failed, " + (pairs.size() - copied - failed) + " not copied (abort) in "
				+ (System.currentTimeMillis() - started) + " ms");
		return outcomes;
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
					String attachment = Utilities.getAttachmentFileName(
							(org.w3c.dom.Element) eElement.getElementsByTagName("ATTACHMENT").item(0));
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
