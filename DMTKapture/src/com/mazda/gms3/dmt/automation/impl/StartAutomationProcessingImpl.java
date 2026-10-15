package com.mazda.gms3.dmt.automation.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mazda.gms3.dmt.automation.dao.AutomationDAO;
import com.mazda.gms3.dmt.automation.dao.AutomationIdentificationDAO;
import com.mazda.gms3.dmt.automation.dao.TransactionDAO;
import com.mazda.gms3.dmt.automation.utils.PrintReportsUtils;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureArticle;
import com.mazda.gms3.dmt.kapture.KaptureContentService;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.masterdata.MasterDataLoad;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.ThreadAbortUtil;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;

/**
 * THE UPDATE JOB OF THE IPM FACELIFT VIN UPDATE (job B, thread MGSS_MDSYNC_<id>).
 *
 * 2026-10-11 - InfoManager replaced by Kapture:
 *  1. "Master Data with Facelift" (dc_load_type): the MDM files of the job's locales are loaded and sent to Kapture first
 *     (MasterDataLoad, as the conversion), and the new VIN carline names are read from MDM again.
 *  2. Per SM / OSM / WD item, in batches of kapture.bulk.max.articles.per.request documents:
 *       - the LATEST version of each document is read from Kapture (latest-article) - the whole article;
 *       - ONLY the VIN categories change: every category of an OLD VIN range of the document is removed, the NEW one
 *         added (with its name from the Kapture categories of the locale; missing there = the document FAILS);
 *         everything else - channel attributes, other categories, ESI, user groups, views, title, authorship - is
 *         sent back as read;
 *       - one bulk update for the batch; a document whose latest version was PUBLISHED is then published again (the
 *         version the update wrote), as InfoManager's modifyContent(content, published) did - an unpublished one stays
 *         unpublished;
 *       - the view content rows (and MME VIN manual type mapping) are updated ONLY for the documents Kapture accepted -
 *         a failed document keeps the OLD VIN everywhere; Kapture done but the database failed = a database failure.
 *  3. MME: VIN manual type mapping; reports, zip and the notification e-mail as before.
 * There is no check-out in Kapture: a document changed by someone else meanwhile is refused by Kapture (stale state).
 */
public class StartAutomationProcessingImpl {

	private Logger logger = LogManager.getLogger(StartAutomationProcessingImpl.class);

	public static final String ERR_READ = "KAPEXP001";
	public static final String ERR_MISSING_CATEGORY = "KAPEXP002";
	public static final String ERR_UPDATE = "KAPEXP003";
	public static final String ERR_PUBLISH = "KAPEXP004";
	public static final String ERR_DATABASE = "DBEXP001";

	private boolean jobFailureStatus = false;

	private PrintReportsUtils printReportUtils=null;

	private TransactionDAO transactionDAO =null;

	private KaptureContentService kapture = null;

	private String statePublished = null;

	/**
	 * @param settingsPath the source content path of the Settings page (the master data folder is found from it)
	 */
	public void startAutomation(ScheduleDetails scheduleDetails, ArrayList<ExcelRowDetails> inputDataList, String settingsPath)
	{
		boolean sendNotificationEmail=false;
		String scheduleId = null==scheduleDetails ? null : String.valueOf(scheduleDetails.getScheduleId());
		try
		{
			if(null==scheduleDetails || scheduleDetails.getScheduleId()<=0)
			{
				logger.info("startAutomation :: Schedule Id is null as parameter. Exit.");
				return;
			}
			printReportUtils = new PrintReportsUtils();
			transactionDAO = new TransactionDAO();
			List<ItemDetails> items = null==scheduleDetails.getAutomationItemsList() ? new ArrayList<ItemDetails>()
					: new ArrayList<ItemDetails>(scheduleDetails.getAutomationItemsList());

			if(items.isEmpty() && (null==inputDataList || inputDataList.isEmpty()))
			{
				logger.info("startAutomation :: No Items Found, Updating Status as Success.");
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.success.value"));
				sendNotificationEmail = true;
			}
			else
			{
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.processing.value"));
				boolean masterDataWithFacelift = ScheduleDAO.isMasterDataWithContent(scheduleId);
				logger.info("startAutomation :: Schedule {"+scheduleId+"} :: items :: > "+items.size()+" :: VIN ranges :: > "
						+(null==inputDataList ? 0 : inputDataList.size())+" :: masterDataWithFacelift=" + masterDataWithFacelift
						+" kaptureApi=" + ApplicationProperties.getProperty("kapture.api.base"));

				// 1. "MASTER DATA WITH FACELIFT" - the MDM files of the job's locales, per market, before any document
				if(masterDataWithFacelift)
				{
					loadMasterData(scheduleId, settingsPath, items, inputDataList);
				}

				// 2. THE DOCUMENTS
				for(ItemDetails details : items)
				{
					ThreadAbortUtil.checkpoint();
					processItem(scheduleDetails, details);
				}

				// 3. MME - VIN MANUAL TYPE MAPPING (MME locales only - no MME row, no step and no report)
				boolean hasMmeRows = false;
				if(null!=inputDataList)
				{
					for(ExcelRowDetails vinDetails : inputDataList)
					{
						if(AutomationDAO.isLocaleOf("mme.locales", vinDetails.getLocale()))
						{
							hasMmeRows = true;
							break;
						}
					}
				}
				if(hasMmeRows)
				{
					inputDataList = AutomationDAO.processVinManualTypeMapping(inputDataList);
					if(null!=inputDataList && inputDataList.size()>0)
					{
						for(ExcelRowDetails vinDetails : inputDataList)
						{
							if(AutomationDAO.isLocaleOf("mme.locales", vinDetails.getLocale())
									&& (null==vinDetails.getProcessingStatus() || "".equals(vinDetails.getProcessingStatus())
									|| AutomationConstants.STATUS_FAILURE.equals(vinDetails.getProcessingStatus())))
							{
								jobFailureStatus = true;
								break;
							}
						}
						printReportUtils.printVINManualMappingReport(scheduleId, inputDataList);
					}
				}

				// 4. THE OLD VIN RANGES INACTIVE IN MDM - only when everything above succeeded (nothing changes in Kapture)
				if(!jobFailureStatus && null!=inputDataList && !inputDataList.isEmpty())
				{
					ThreadAbortUtil.checkpoint();
					List<String[]> oldVins = AutomationDAO.deactivateOldVins(inputDataList);
					for(String[] line : oldVins)
					{
						if(AutomationDAO.OLD_VIN_FAILED.equals(line[8]))
						{
							jobFailureStatus = true;
						}
					}
					printReportUtils.printOldVinDeactivationReport(scheduleId, oldVins);
				}
				else if(jobFailureStatus)
				{
					logger.info("startAutomation :: Errors Found - the old VIN ranges stay active in MDM.");
				}

				logger.info("###############################################################################");
				logger.info("startAutomation :: "+(jobFailureStatus ? "Errors Found, Updating Status as Failure" : "No Errors Found, Updating Status as Success")+".");
				logger.info("###############################################################################");
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty(jobFailureStatus ? "schedule.status.failure.value" : "schedule.status.success.value"));
				sendNotificationEmail = true;
			}
			// the master data reports go into the zip with the others
			MasterDataLoad.writeReports(scheduleId);
			generateZIP(scheduleId);
		}
		catch(Exception e)
		{
			// an abort (ThreadAbortUtil.AbortSignal, an Error) passes through: History has set the status already
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "startAutomation()", e);
			try {
				ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
				ScheduleDAO.updateScheduleAllItemStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.failure.value"));
				sendNotificationEmail = true;
			} catch (Exception e1) {
				Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "startAutomation()", e1);
			}
		}
		finally
		{
			if(null!=scheduleId)
			{
				MasterDataLoad.discardReports(scheduleId);
			}
			printReportUtils = null;
			transactionDAO = null;
			kapture = null;
		}

		if(null!=scheduleDetails && null!=scheduleDetails.getUserId() && !"".equals(scheduleDetails.getUserId()) && sendNotificationEmail)
		{
			// set userId as null for FaceLift
			NotificationEmailHelper.generateNotificationEmail(scheduleId, null);
		}
		if(null!=scheduleId && scheduleDetails.getScheduleId()>0)
		{
			stopActiveThead(scheduleId);
		}
	}

	/**
	 * MasterDataLoad per market (MC / MME / MNAO) for the locales of the job, then the NEW VIN carline names are read from
	 * MDM again for every VIN row of the job (new ranges loaded now had none, or only the file's English name).
	 */
	@SuppressWarnings("unchecked")
	private void loadMasterData(String scheduleId, String settingsPath, List<ItemDetails> items, List<ExcelRowDetails> inputDataList)
	{
		Map<String, Set<String>> localesByMarket = new LinkedHashMap<String, Set<String>>();
		List<ExcelRowDetails> allVins = new ArrayList<ExcelRowDetails>();
		if(null!=inputDataList)
		{
			allVins.addAll(inputDataList);
		}
		for(ItemDetails details : items)
		{
			if(null!=details.getLocale() && !"".equals(details.getLocale().trim()))
			{
				String market = AutomationDAO.marketOf(details.getLocale());
				if(!localesByMarket.containsKey(market))
				{
					localesByMarket.put(market, new LinkedHashSet<String>());
				}
				// MDM form, e.g. ja-JP
				localesByMarket.get(market).add(details.getLocale().trim().replace("_", "-"));
			}
			if(null!=details.getApplicableVINList())
			{
				allVins.addAll(details.getApplicableVINList());
			}
			if(null!=details.getImpactedDocumentsList())
			{
				for(Map<String, Object> documentMap : details.getImpactedDocumentsList())
				{
					if(null!=documentMap.get(AutomationConstants.VIN_LIST))
					{
						allVins.addAll((List<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST));
					}
				}
			}
		}
		for(Map.Entry<String, Set<String>> e : localesByMarket.entrySet())
		{
			ThreadAbortUtil.checkpoint();
			// a failed row in either master data report fails the job, like the other reports
			if(MasterDataLoad.run(scheduleId, settingsPath, e.getKey(), e.getValue()))
			{
				jobFailureStatus = true;
			}
		}
		AutomationDAO.refreshNewModelNames(allVins);
		logger.info("loadMasterData :: markets :: > "+localesByMarket+" :: new VIN names read again for "+allVins.size()+" VIN rows.");
	}

	/** One item: report-only channels as before; SM / OSM / WD documents in batches. */
	@SuppressWarnings("unchecked")
	private void processItem(ScheduleDetails scheduleDetails, ItemDetails details) throws Exception
	{
		String scheduleId = String.valueOf(scheduleDetails.getScheduleId());
		if(details.getItemId()<=0 || null==details.getLocale() || "".equals(details.getLocale()) || null==details.getChannelRefKey()
				|| "".equals(details.getChannelRefKey()) || null==details.getCarlineInfo() || "".equals(details.getCarlineInfo()))
		{
			logger.info("processItem :: Item does not have all mandatory Parameters. Exiting its Processing.");
			if(details.getItemId()>0)
			{
				ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_FAILURE);
				jobFailureStatus = true;
			}
			return;
		}
		List<Map<String, Object>> documents = null==details.getImpactedDocumentsList() ? new ArrayList<Map<String, Object>>()
				: details.getImpactedDocumentsList();

		// THE ITEM LEAVES THE IDENTIFICATION JOB
		if(null!=details.getDocIdnSchId() && details.getDocIdnSchId().longValue()>0 && null!=details.getDocIdnItemId() && details.getDocIdnItemId().longValue()>0)
		{
			AutomationIdentificationDAO.deleteItemDetails(details.getDocIdnSchId(), details.getDocIdnItemId(), documents.size(),
					documents.size()+" documents ("+details.getLocale()+" "+details.getChannelRefKey()+" "+details.getCarlineInfo()+") processed by "
					+scheduleDetails.getScheduleName());
		}
		ScheduleDAO.updateCurrentJobStatus(scheduleId, String.valueOf(details.getItemId()), "DOCUMENT PROCESSING");
		if(documents.isEmpty())
		{
			logger.info("processItem :: No Impacted Documents Found for Item "+details.getLocale()+" >> "+details.getChannelRefKey()+" >> "+details.getCarlineInfo()+".");
			ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
			return;
		}

		String channel = details.getChannelRefKey();
		if(channel.equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES) || channel.equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE)
				|| channel.equals(AutomationConstants.CHANNEL_REFKEY_TRAINING) || channel.equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS))
		{
			// REPORT ONLY (not updatable since 2019-07-24)
			ScheduleDAO.updateMultipleProcessingCount(scheduleId, String.valueOf(details.getItemId()), documents.size());
			printReportUtils.printOtherChannelReports(details, scheduleId);
			ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
			return;
		}
		String channelType = kaptureChannelType(channel);
		if(null==channelType)
		{
			logger.info("processItem :: channel "+channel+" is not updatable. Item fails.");
			ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_FAILURE);
			jobFailureStatus = true;
			return;
		}

		ArrayList<Map<String, Object>> transactionList = new ArrayList<Map<String, Object>>();
		ArrayList<Map<String, Object>> errorsList = new ArrayList<Map<String, Object>>();
		int batchSize = KaptureApiClient.requireInt("kapture.bulk.max.articles.per.request");
		for(int from=0; from<documents.size(); from+=batchSize)
		{
			ThreadAbortUtil.checkpoint();
			List<Map<String, Object>> batch = documents.subList(from, Math.min(from+batchSize, documents.size()));
			logger.info("################## PROCESSING "+channel+" DOCUMENTS OF "+details.getLocale()+" -> "+(from+1)+" .. "+(from+batch.size())+" / "+documents.size());
			for(Map<String, Object> documentMap : batch)
			{
				documentMap.put(AutomationConstants.ITEM_DETAILS, details);
				documentMap.put(AutomationConstants.SCHEDULE_DETAILS, scheduleDetails);
			}
			processBatch(details, channelType, batch, errorsList);
			long ok = 0;
			long failed = 0;
			for(Map<String, Object> documentMap : batch)
			{
				if(AutomationConstants.STATUS_SUCCESS.equals(documentMap.get(AutomationConstants.PROCESSING_STATUS)))
				{
					ok++;
				}
				else
				{
					failed++;
					jobFailureStatus = true;
				}
				transactionList.add(documentMap);
			}
			ScheduleDAO.addDocumentCounts(scheduleDetails.getScheduleId(), details.getItemId(), ok, failed);
		}

		// REPORTS OF THE ITEM
		printReportUtils.printSMWDChannelReports(details, scheduleId, transactionList);
		if(!errorsList.isEmpty())
		{
			printReportUtils.printErrorReports(details, scheduleId, errorsList);
			logger.info("processItem :: Errors Found for Item "+details.getLocale()+" >> "+channel+" >> "+details.getCarlineInfo()+". Item Status Failure.");
			ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_FAILURE);
			jobFailureStatus = true;
		}
		else
		{
			logger.info("processItem :: No Errors Found for Item "+details.getLocale()+" >> "+channel+" >> "+details.getCarlineInfo()+". Item Status Success.");
			ScheduleDAO.updateCurrentJobStatusForItem(String.valueOf(details.getItemId()), AutomationConstants.STATUS_SUCCESS);
		}
	}

	/**
	 * One batch of documents of one item: read, swap the VIN categories, bulk update, publish what was published, then the
	 * view content of the documents Kapture accepted. Every document of the batch ends with PROCESSING_STATUS set.
	 */
	@SuppressWarnings("unchecked")
	private void processBatch(ItemDetails details, String channelType, List<Map<String, Object>> batch, List<Map<String, Object>> errorsList)
	{
		String locale = details.getLocale().trim().replace("-", "_");
		KaptureContentService service = kapture();

		// THE NEW VIN CATEGORIES OF THE BATCH, WITH THEIR NAMES - one lookup
		List<String> newRefKeys = new ArrayList<String>();
		for(Map<String, Object> documentMap : batch)
		{
			for(ExcelRowDetails vin : vinsOf(documentMap))
			{
				if(null!=vin.getNewRefKey() && !"".equals(vin.getNewRefKey()))
				{
					newRefKeys.add(vin.getNewRefKey().trim());
				}
			}
		}
		Map<String, String[]> categories;
		try
		{
			categories = KaptureLookupDAO.getCategories(new ArrayList<String>(new LinkedHashSet<String>(newRefKeys)), locale);
		}
		catch(RuntimeException e)
		{
			for(Map<String, Object> documentMap : batch)
			{
				fail(documentMap, errorsList, ERR_MISSING_CATEGORY, null, "New VIN categories could not be read from Kapture: "+e.getMessage());
			}
			return;
		}

		// READ + SWAP
		List<KaptureContentService.BatchItem> toWrite = new ArrayList<KaptureContentService.BatchItem>();
		Map<Map<String, Object>, Boolean> wasPublished = new IdentityHashMap<Map<String, Object>, Boolean>();
		Set<String> sourceIds = new HashSet<String>();
		for(Map<String, Object> documentMap : batch)
		{
			String documentId = null==documentMap.get(AutomationConstants.DOCUMENTID) ? null : documentMap.get(AutomationConstants.DOCUMENTID).toString().trim();
			List<ExcelRowDetails> vins = vinsOf(documentMap);
			if(null==documentId || "".equals(documentId))
			{
				fail(documentMap, null, null, null, "Document Id is null.");
				continue;
			}
			if(vins.isEmpty())
			{
				fail(documentMap, null, null, null, "Applicable VINs to be replaced are null.");
				continue;
			}
			String missing = null;
			for(ExcelRowDetails vin : vins)
			{
				if(null==vin.getNewRefKey() || !categories.containsKey(vin.getNewRefKey().trim().toUpperCase()))
				{
					missing = null==vin.getNewRefKey() ? "" : vin.getNewRefKey();
					break;
				}
			}
			if(null!=missing)
			{
				fail(documentMap, errorsList, ERR_MISSING_CATEGORY, missing, "NEW VIN CATEGORY {"+missing+"} DOES NOT EXIST IN KAPTURE FOR LOCALE "+locale+".");
				continue;
			}
			ThreadAbortUtil.checkpoint();
			JsonObject read = service.readArticle(documentId, channelType, locale);
			if(null==read)
			{
				fail(documentMap, errorsList, ERR_READ, null, "FAILED TO FETCH DOCUMENT FROM KAPTURE.");
				continue;
			}
			KaptureArticle article = swapVins(read, documentId, channelType, locale, vins, categories, sourceIds);
			toWrite.add(new KaptureContentService.BatchItem(article, documentMap));
		}
		if(toWrite.isEmpty())
		{
			return;
		}
		// PUBLISHED OR NOT - from k_article (the read can be thin), one lookup for the batch
		try
		{
			Map<String, KaptureLookupDAO.ArticleIdentity> identities = KaptureLookupDAO.getArticleIdentities(toWrite);
			for(KaptureContentService.BatchItem it : toWrite)
			{
				KaptureLookupDAO.ArticleIdentity id = identities.get(KaptureLookupDAO.identityKey(it.article.documentId, it.article.locale));
				wasPublished.put((Map<String, Object>)it.owner, Boolean.valueOf(null!=id && service.isPublishedState(id.articleState)));
			}
		}
		catch(RuntimeException e)
		{
			for(KaptureContentService.BatchItem it : toWrite)
			{
				fail((Map<String, Object>)it.owner, errorsList, ERR_READ, null, "The state of the document could not be read from Kapture: "+e.getMessage());
			}
			return;
		}

		// ONE BULK UPDATE
		try
		{
			service.updateBatch(toWrite);
		}
		catch(RuntimeException e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "processBatch()", e);
		}
		List<KaptureContentService.PublishItem> toPublish = new ArrayList<KaptureContentService.PublishItem>();
		Map<KaptureContentService.PublishItem, Map<String, Object>> publishOwner = new IdentityHashMap<KaptureContentService.PublishItem, Map<String, Object>>();
		List<Map<String, Object>> accepted = new ArrayList<Map<String, Object>>();
		for(KaptureContentService.BatchItem it : toWrite)
		{
			Map<String, Object> documentMap = (Map<String, Object>)it.owner;
			if(null==it.result || !it.result.success)
			{
				fail(documentMap, errorsList, ERR_UPDATE, null, "FAILED TO MODIFY DOCUMENT IN KAPTURE. "
						+(null==it.result ? "No result." : it.result.errorCode+" "+it.result.message));
				continue;
			}
			documentMap.put(AutomationConstants.CONTENT_ID, it.result.rowId);
			documentMap.put(AutomationConstants.DOCUMENT_VERSION, it.result.articleVersion);
			documentMap.put(AutomationConstants.DOCUMENT_STATUS, "Draft");
			if(Boolean.TRUE.equals(wasPublished.get(documentMap)))
			{
				KaptureContentService.PublishItem p = new KaptureContentService.PublishItem(it.article.documentId, it.result, it.article.title, it.article);
				toPublish.add(p);
				publishOwner.put(p, documentMap);
			}
			else
			{
				accepted.add(documentMap);
			}
		}

		// THE LATEST WAS PUBLISHED - IT GOES BACK AS PUBLISHED
		Set<String> published = new HashSet<String>();
		if(!toPublish.isEmpty())
		{
			ThreadAbortUtil.checkpoint();
			service.publish(channelType, locale, toPublish, "DMT-FL-"+details.getItemId());
			for(KaptureContentService.PublishItem p : toPublish)
			{
				Map<String, Object> documentMap = publishOwner.get(p);
				if(null==p.error)
				{
					documentMap.put(AutomationConstants.DOCUMENT_STATUS, "Publish");
					// the live version (x.0) read back from k_article, not the draft the update wrote
					if(null!=p.publishedVersion && !"".equals(p.publishedVersion.trim()))
					{
						documentMap.put(AutomationConstants.DOCUMENT_VERSION, p.publishedVersion.trim());
					}
					published.add(p.documentId.trim());
					accepted.add(documentMap);
				}
				else
				{
					// the new VIN is only in a draft version - the live version and the view content keep the old VIN
					fail(documentMap, errorsList, ERR_PUBLISH, null, "THE DOCUMENT WAS PUBLISHED BEFORE THE UPDATE; THE NEW VERSION COULD NOT BE PUBLISHED. "
							+p.error+" - publish it in Kapture; the view content keeps the old VIN.");
				}
			}
		}

		// VIEW CONTENT - ONLY FOR THE DOCUMENTS KAPTURE ACCEPTED
		if(accepted.isEmpty())
		{
			return;
		}
		Map<String, List<ExcelRowDetails>> docVins = new LinkedHashMap<String, List<ExcelRowDetails>>();
		Map<String, Map<String, Object>> byId = new HashMap<String, Map<String, Object>>();
		for(Map<String, Object> documentMap : accepted)
		{
			String documentId = documentMap.get(AutomationConstants.DOCUMENTID).toString().trim();
			docVins.put(documentId, vinsOf(documentMap));
			byId.put(documentId, documentMap);
		}
		Map<String, String> dbFailed = transactionDAO.updateDocumentDetails(details.getLocale(), docVins, published);
		for(Map.Entry<String, Map<String, Object>> e : byId.entrySet())
		{
			Map<String, Object> documentMap = e.getValue();
			if(dbFailed.containsKey(e.getKey()))
			{
				logger.info("processBatch :: Failed to Modify "+e.getKey()+" in Database.");
				// done in Kapture, not in the database - a database failure, as in legacy
				fail(documentMap, errorsList, ERR_DATABASE, null, dbFailed.get(e.getKey()));
				documentMap.put(AutomationConstants.ERROR_MESSAGE, "Failed to perform Modify Operaion in Database. Please refer failure reports.");
			}
			else
			{
				documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_SUCCESS);
			}
		}
	}

	/**
	 * The article to send: what Kapture holds, with the categories of the OLD VIN ranges of the document removed and the
	 * NEW ones added. Category entries are reduced to the mapped (selected) leaves - the write contract.
	 */
	private KaptureArticle swapVins(JsonObject read, String documentId, String channelType, String locale, List<ExcelRowDetails> vins,
			Map<String, String[]> categories, Set<String> sourceIds)
	{
		KaptureArticle a = new KaptureArticle();
		a.documentId = documentId;
		a.channelType = channelType;
		a.locale = locale;
		a.title = stringOf(read, "title");
		Set<String> oldKeys = new HashSet<String>();
		for(ExcelRowDetails vin : vins)
		{
			if(null!=vin.getOldRefKey())
			{
				oldKeys.add(vin.getOldRefKey().trim().toUpperCase());
			}
		}
		Set<String> present = new HashSet<String>();
		for(JsonElement e : asArray(read, "articlelistcategory"))
		{
			if(!e.isJsonObject() || !isSelected(e.getAsJsonObject()))
			{
				continue;
			}
			String ref = stringOf(e.getAsJsonObject(), "categoryRefKey");
			if(null==ref || "".equals(ref.trim()) || oldKeys.contains(ref.trim().toUpperCase()) || !present.add(ref.trim().toUpperCase()))
			{
				// an OLD VIN range - removed
				continue;
			}
			a.categories.add(new KaptureArticle.Category(ref, stringOf(e.getAsJsonObject(), "categoryName"), false));
		}
		for(ExcelRowDetails vin : vins)
		{
			String[] cat = categories.get(vin.getNewRefKey().trim().toUpperCase());
			if(present.add(cat[0].trim().toUpperCase()))
			{
				a.categories.add(new KaptureArticle.Category(cat[0], cat[1], false));
			}
		}
		for(JsonElement e : asArray(read, "articleesicategory"))
		{
			if(e.isJsonObject() && isSelected(e.getAsJsonObject()) && null!=stringOf(e.getAsJsonObject(), "categoryRefKey"))
			{
				a.categories.add(new KaptureArticle.Category(stringOf(e.getAsJsonObject(), "categoryRefKey"), stringOf(e.getAsJsonObject(), "categoryName"), true));
			}
		}
		// THE REST AS READ
		a.userGroupsJson = asArray(read, "articlelistusergroup");
		a.viewsJson = asArray(read, "articlelistview");
		for(String key : new String[] { "articleOwner", "articleCreator", "articleCreatorUserID", "createdBy" })
		{
			a.carriedOver.put(key, stringOf(read, key));
		}
		JsonObject node = read.getAsJsonObject(channelType);
		for(Map.Entry<String, JsonElement> e : node.entrySet())
		{
			a.fields.put(e.getKey(), e.getValue());
		}
		// traceability as Kapture holds it; unique per request (the bulk results are matched on it)
		String source = stringOf(read, "sourceIdentifier");
		if(null==source || "".equals(source.trim()) || !sourceIds.add(source.trim()))
		{
			source = "dmt/facelift/"+channelType.toLowerCase()+"/"+documentId+"/"+locale;
			sourceIds.add(source);
		}
		a.sourceIdentifier = source;
		a.sourceFileName = stringOf(read, "sourceFileName");
		return a;
	}

	private KaptureContentService kapture()
	{
		if(null==kapture)
		{
			// no user groups / views of the job: every document keeps its own
			kapture = new KaptureContentService("", "");
			statePublished = KaptureApiClient.require("kapture.article.state.published");
			logger.info("kapture :: session opened :: published state = "+statePublished);
		}
		return kapture;
	}

	/** SM / OSM / WD item channel -> Kapture channel type; null for any other. */
	private static String kaptureChannelType(String channelRefKey)
	{
		if(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE.equals(channelRefKey))
		{
			return "SERVICE_MANUALS";
		}
		if(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE.equals(channelRefKey))
		{
			return "OTHER_SERVICE_MANUALS";
		}
		if(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS.equals(channelRefKey))
		{
			return "WIRING_DIAGRAMS";
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static List<ExcelRowDetails> vinsOf(Map<String, Object> documentMap)
	{
		Object v = documentMap.get(AutomationConstants.VIN_LIST);
		return null==v ? new ArrayList<ExcelRowDetails>() : (List<ExcelRowDetails>)v;
	}

	/** The document FAILS; with errorsList it also goes to the failure report (once). */
	private void fail(Map<String, Object> documentMap, List<Map<String, Object>> errorsList, String code, String missingRefKey, String message)
	{
		documentMap.put(AutomationConstants.PROCESSING_STATUS, AutomationConstants.STATUS_FAILURE);
		documentMap.put(AutomationConstants.ERROR_MESSAGE, message);
		logger.info("document FAILED :: "+documentMap.get(AutomationConstants.DOCUMENTID)+" :: "+code+" :: "+message);
		if(null==errorsList)
		{
			return;
		}
		Map<String, Object> errorMap = new HashMap<String, Object>();
		errorMap.put(AutomationConstants.ERROR_CODE, code);
		errorMap.put(AutomationConstants.ERROR_MESSAGE, message);
		errorMap.put(AutomationConstants.SCHEDULE_DETAILS, documentMap.get(AutomationConstants.SCHEDULE_DETAILS));
		errorMap.put(AutomationConstants.ITEM_DETAILS, documentMap.get(AutomationConstants.ITEM_DETAILS));
		errorMap.put(AutomationConstants.DOCUMENTID, documentMap.get(AutomationConstants.DOCUMENTID));
		if(null!=missingRefKey)
		{
			errorMap.put(AutomationConstants.MISSING_REFKEY, missingRefKey);
		}
		errorsList.add(errorMap);
	}

	/** A field that should be an array but comes as a single object when there is one entry. */
	private static JsonArray asArray(JsonObject o, String field)
	{
		JsonArray out = new JsonArray();
		if(null==o || !o.has(field) || o.get(field).isJsonNull())
		{
			return out;
		}
		JsonElement e = o.get(field);
		if(e.isJsonArray())
		{
			return e.getAsJsonArray();
		}
		if(e.isJsonObject())
		{
			out.add(e);
		}
		return out;
	}

	private static boolean isSelected(JsonObject entry)
	{
		try
		{
			return entry.has("selected") && !entry.get("selected").isJsonNull() && entry.get("selected").getAsBoolean();
		}
		catch(Exception e)
		{
			return false;
		}
	}

	private static String stringOf(JsonObject o, String key)
	{
		if(null==o || !o.has(key) || o.get(key).isJsonNull() || !o.get(key).isJsonPrimitive())
		{
			return null;
		}
		return o.get(key).getAsString();
	}

	/**
	 * Function will stop the Active Thread in Web Container once the Job Finishes
	 */
	private void stopActiveThead(String scheduleCode)
	{
		try
		{
			String threadId=ApplicationProperties.getProperty("automation.sch.key")+scheduleCode;
			for(Thread at : Thread.getAllStackTraces().keySet())
			{
				if(null!=at.getName() && at.getName().equals(threadId) && at.isAlive())
				{
					logger.info(" ################################### STOPPING THREAD ID :: > " + threadId);
					ThreadAbortUtil.abort(at);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "stopActiveThead()", e);
		}
	}

	private void generateZIP(String scheduleCode)
	{
		try
		{
			// REPORTS_DIRECTORY/<schedule>/
			String path = ApplicationProperties.getProperty("REPORTS_DIRECTORY");
			if(!path.endsWith("/"))
			{
				path = path+"/";
			}
			path = path+scheduleCode+"/";
			if(printReportUtils.createReportsZip(path, scheduleCode))
			{
				logger.info("printReports :: Reports Zipped Successfully.");
			}
			else
			{
				logger.info("printReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(StartAutomationProcessingImpl.class.getName(), "generateZIP()", e);
		}
	}
}
