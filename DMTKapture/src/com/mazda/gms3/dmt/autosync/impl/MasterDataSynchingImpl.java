package com.mazda.gms3.dmt.autosync.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncCountBuffer;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncTransactionDAO;
import com.mazda.gms3.dmt.autosync.utils.PrintReportUtils;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureCategoryStore;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.ThreadAbortUtil;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;

/**
 * MASTER DATA SYNCING JOB - sends the MDM rows waiting for syncing (MDM_SYNC_STATUS N) to Kapture as categories.
 *
 * The process is the legacy one: item by item, every locale mapped to the item's master data type
 * (gms3_mdm_alt_lang_mapping), the rows of the autosync DAOs, level 1 to 5; then sync status Y, reports and
 * the notification e-mail. Only the target changed - the InfoManager client calls are replaced by the Kapture
 * category API, or - kapture.category.write.mode = DB - a direct write into the Kapture categories table
 * (see KaptureCategoryLocaleSync, KaptureCategoryStore). The locales of an item are independent of each other and are
 * processed in parallel (masterdata.sync.locale.threads), each worker with its own Kapture login in API mode.
 */
public class MasterDataSynchingImpl extends Thread{

	private Logger logger = LogManager.getLogger(MasterDataSynchingImpl.class);

	private MasterDataSyncTransactionDAO transactionDAO = null;

	private boolean errorsFound = false;

	private ArrayList<AutoSyncCategoryDetails> categoriesList = null;

	private ArrayList<AutoSyncCategoryDetails> failureList = null;

	/** Item types for which a whole locale could not be processed - none of their rows gets sync status Y. */
	private Set<String> incompleteItemTypes = null;

	private PrintReportUtils printReportUtils = null;

	private MasterDataSyncDAO masterDataDAO = null;

	/** Result of one locale of one item. */
	private static class LocaleResult {
		List<AutoSyncCategoryDetails> rows = new ArrayList<AutoSyncCategoryDetails>();
		List<AutoSyncCategoryDetails> failures = new ArrayList<AutoSyncCategoryDetails>();
		boolean incomplete = false;
	}

	public void startSynching(AutoSyncScheduleDetails schDetails)
	{
		ExecutorService workers = null;
		final AtomicBoolean aborted = new AtomicBoolean(false);
		final AtomicInteger kaptureLogins = new AtomicInteger(0);
		// DB mode: reference keys already filled into the missing Kapture default locales in this job (one worker does it)
		final java.util.Set<String> filledLocales = java.util.concurrent.ConcurrentHashMap.newKeySet();
		MasterDataSyncCountBuffer counts = null;
		try
		{
			transactionDAO = new MasterDataSyncTransactionDAO();
			errorsFound=  false;
			categoriesList=  new ArrayList<AutoSyncCategoryDetails>();
			failureList =new ArrayList<AutoSyncCategoryDetails>();
			incompleteItemTypes = new HashSet<String>();
			printReportUtils = new PrintReportUtils();
			masterDataDAO = new  MasterDataSyncDAO();
			counts = new MasterDataSyncCountBuffer(schDetails.getScheduleId());

			// UPDATE JOB STATUS TO PROCESSING
			transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_PROCESSING, null);

			int threads = KaptureApiClient.requireInt("masterdata.sync.locale.threads");
			if(threads<1)
			{
				throw new IllegalStateException("masterdata.sync.locale.threads must be 1 or more, is " + threads);
			}
			// API: through the Kapture category API / DB: straight into the categories table (no Kapture login)
			final boolean dbMode = KaptureCategoryStore.dbMode();
			logger.info("startSynching :: category write mode :: " + (dbMode ? KaptureCategoryStore.MODE_DB : KaptureCategoryStore.MODE_API)
					+ " :: locale worker threads " + threads);
			final String workerPrefix = schDetails.getThreadId() + "_L";
			final AtomicInteger workerNo = new AtomicInteger(0);
			workers = Executors.newFixedThreadPool(threads, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, workerPrefix + workerNo.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});
			// ONE KAPTURE LOGIN PER WORKER THREAD, REUSED FOR EVERY LOCALE IT PROCESSES
			final ThreadLocal<KaptureApiClient> clients = new ThreadLocal<KaptureApiClient>() {
				@Override
				protected KaptureApiClient initialValue() {
					kaptureLogins.incrementAndGet();
					return new KaptureApiClient();
				}
			};

			/*
			 * START PROCESSING EACH ITEM ONE BY ONE
			 */
			if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				for(int a=0;a<schDetails.getItemsList().size();a++)
				{
					final AutoSyncScheduleItemDetails itemDetails=  (AutoSyncScheduleItemDetails)schDetails.getItemsList().get(a);
					logger.info("startSyncing :: Start Procesisng for Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey());
					// UPDATE ITEM STATUS AS PROCESSING
					transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_PROCESSING);
					/*
					 * CALL Function to Fetch Categories List for the selected Item
					 * For Each Locale & start Processing
					 */
					List<MarketLocaleMasterDataTypeMapping> localesList= masterDataDAO.getMasterDataLocaleMappingList(itemDetails.getLocale(), itemDetails.getItemKey());
					if(null!=localesList && localesList.size()>0)
					{
						logger.info("startSyncing :: Item " + itemDetails.getItemKey() + " :: locales mapped :: > " + localesList.size() + " :: worker threads " + threads);
						List<Future<LocaleResult>> futures = new ArrayList<Future<LocaleResult>>();
						for(int b=0;b<localesList.size();b++)
						{
							final MarketLocaleMasterDataTypeMapping localeMappingDetails= (MarketLocaleMasterDataTypeMapping)localesList.get(b);
							final long scheduleId = schDetails.getScheduleId();
							final String market = schDetails.getMarket();
							final MasterDataSyncCountBuffer itemCounts = counts;
							futures.add(workers.submit(() -> syncLocale(itemDetails, localeMappingDetails, scheduleId, market,
									dbMode ? null : clients.get(), aborted, itemCounts, filledLocales)));
						}
						// RESULTS IN THE ORDER OF THE LOCALES (same report order as legacy); ABORT IS CHECKED WHILE WAITING
						for(Future<LocaleResult> future : futures)
						{
							LocaleResult result = waitFor(future);
							categoriesList.addAll(result.rows);
							failureList.addAll(result.failures);
							if(result.incomplete)
							{
								incompleteItemTypes.add(itemDetails.getItemKey());
							}
						}
						counts.flush();

						// check if any entry for the item in Failure List - if Yes then update status as Failure for Item and Job will also Fail
						boolean itemStatusSucces = true;
						for(AutoSyncCategoryDetails failCatDetails : failureList)
						{
							if(failCatDetails.getItemId()==itemDetails.getItemId())
							{
								itemStatusSucces = false;
								break;
							}
						}

						if(itemStatusSucces==true)
						{
							logger.info("startSyncing :: No Failure For Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey()+". Mark it as Success.");
							transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_SUCCESS);
						}
						else
						{
							logger.info("startSyncing :: Some Failures For Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey()+". Mark it as Failure.");
							transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_FAILURE);
						}
					}
					else
					{
						logger.info("startSyncing :: No Locales Mapping Found for {"+itemDetails.getLocale()+"} Market of Master Data Type :: > " + itemDetails.getItemKey());
						/*
						 * UPDATE ITEM STATUS AS SUCCESS
						 */
						transactionDAO.updateItemStatus(String.valueOf(itemDetails.getItemId()), AutoSyncConstants.STATUS_SUCCESS);
					}
					localesList=  null;
					logger.info("startSyncing :: End Procesisng for Item :: > "+ itemDetails.getLocale()+" - >"+ itemDetails.getItemKey());
				}
			}

			if(null!=failureList && failureList.size()>0)
			{
				// some exceptions occurred, set Job as Failure
				errorsFound=true;
			}

			/*
			 * UPDATE MDM ITEMS SYNC STATUS
			 */
			updateSyncStatusForItems();
			/*
			 * PRINT REPORTS
			 */
			generateReports(String.valueOf(schDetails.getScheduleId()));

			if(errorsFound==false)
			{
				logger.info("startSynching :: No Errors Found, update Schedule Status as SUCCESS. Job completed Successfully.");
				// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO SUCCESS
				transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_SUCCESS);
			}
			else
			{
				logger.info("startSynching :: Errors Found, update Schedule Status as FAILURE. Job completed with some Failures, please refer to Reports.");
				// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO FAILURE
				transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_FAILURE);
			}
		}
		catch(Exception  e)
		{
			logger.info("startSynching :: Some Exception Occured While Executing the Schedule. Failing Job.");
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "startSynching()", e);
			// UPDATE JOB STATUS TO COMPLETED & SCHEDULE STATUS TO FAILURE
			transactionDAO.updateJobStatus(String.valueOf(schDetails.getScheduleId()), AutoSyncConstants.STATUS_COMPLETED, AutoSyncConstants.STATUS_FAILURE);
		}
		finally
		{
			// on abort (AbortSignal passes the catch above) the workers stop at their next category
			aborted.set(true);
			if(null!=workers)
			{
				workers.shutdownNow();
			}
			if(null!=counts)
			{
				counts.flush();
			}
			transactionDAO = null;
			errorsFound=  false;
			categoriesList=  null;
			failureList = null;
			incompleteItemTypes = null;

			logger.info(" ******************** MasterDataSynchingImpl :: FOR SCHEDULE ID {"+schDetails.getScheduleId()+"}. KAPTURE LOGINS :: > "+ kaptureLogins.get());
			printReportUtils = null;
		}

		// set Notification Email
		NotificationEmailHelper.generateMasterDataSyncNotificationEmail(String.valueOf(schDetails.getScheduleId()));
		// stop active thread
		stopActiveThead(schDetails.getThreadId());
	}

	/**
	 * One locale of one item, on a worker thread: the rows waiting in MDM for the locale (processing locale,
	 * else alternate locale - autosync DAO), then Kapture.
	 */
	private LocaleResult syncLocale(AutoSyncScheduleItemDetails itemDetails, MarketLocaleMasterDataTypeMapping localeMappingDetails,
			long scheduleId, String market, KaptureApiClient api, AtomicBoolean aborted, MasterDataSyncCountBuffer counts,
			java.util.Set<String> filledLocales)
	{
		LocaleResult result = new LocaleResult();
		String processingLocale = localeMappingDetails.getLocale();
		try
		{
			if(aborted.get())
			{
				result.incomplete = true;
				return result;
			}
			ArrayList<AutoSyncCategoryDetails> rows = new MasterDataSyncDAO().getCategoriesListForProcessing(itemDetails.getItemKey(), market, localeMappingDetails);
			if(null!=rows && rows.size()>0)
			{
				KaptureCategoryLocaleSync sync = new KaptureCategoryLocaleSync(api, aborted, counts, filledLocales);
				try
				{
					sync.run(rows, itemDetails.getItemId(), scheduleId, market, itemDetails.getItemKey(), processingLocale);
				}
				finally
				{
					result.rows.addAll(sync.rows);
					result.failures.addAll(sync.failures);
				}
			}
			else
			{
				logger.info("syncLocale :: " + itemDetails.getItemKey() + " :: " + processingLocale + " :: no rows waiting for syncing.");
			}
		}
		catch(Throwable e)
		{
			result.incomplete = true;
			if(aborted.get())
			{
				// THE JOB WAS ABORTED (or has ended) - nothing is reported any more
				logger.info("syncLocale :: " + itemDetails.getItemKey() + " :: " + processingLocale + " :: stopped, the job was aborted.");
				return result;
			}
			// THE LOCALE COULD NOT BE PROCESSED COMPLETELY - REPORTED AS A FAILURE OF THE ITEM
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "syncLocale()", e instanceof Exception ? (Exception)e : new Exception(e));
			AutoSyncCategoryDetails failure = new AutoSyncCategoryDetails();
			failure.setItemId(itemDetails.getItemId());
			failure.setMarket(market);
			failure.setItemType(itemDetails.getItemKey());
			failure.setLocale(processingLocale);
			failure.setOperationType("CREATE_CATEGORY");
			failure.setErrorCode("LOCALE_NOT_PROCESSED");
			failure.setErrorMessage("The locale could not be processed completely: " + (null == e.getMessage() ? e.toString() : e.getMessage()));
			result.failures.add(failure);
		}
		return result;
	}

	private LocaleResult waitFor(Future<LocaleResult> future) throws Exception
	{
		while(true)
		{
			try
			{
				return future.get(1, TimeUnit.SECONDS);
			}
			catch(TimeoutException e)
			{
				// throws AbortSignal when the job was aborted from the screen
				ThreadAbortUtil.checkpoint();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void stopActiveThead(String threadId)
	{
		try
		{
			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
			Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
			if (null != threadArray && threadArray.length > 0)
			{
				for (int i = 0; i < threadArray.length; i++)
				{
					Thread at = threadArray[i];
					if (null != at.getName() && !"".equals(at.getName())) {
						if (at.getName().equals(threadId))
						{
							try
							{
								if (at.isAlive())
								{
									logger.info(" ################################### STOPPING THREAD ID :: > " + threadId);
									com.mazda.gms3.dmt.utils.ThreadAbortUtil.abort(at);
								}
							}
							catch (Exception e)
							{
								Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "stopActiveThead()", e);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "stopActiveThead()", e);
		}
	}

	/**
	 * SYNC STATUS Y FOR THE MDM ROWS THAT ARE COMPLETELY IN KAPTURE.
	 *
	 * Legacy rule: a row is Y when it was processed with SUCCESS and its MDM id is not in the failure list for
	 * any level / locale. Here a row is Y only when it succeeded in EVERY locale it was processed for (every
	 * failure is now recorded, legacy missed failures that did not throw). CARLINE items hold two MDM tables:
	 * the carline row (level 1 under PARENT_REF_KEYS_CARLINE) and the VIN rows (levels 2 to 5).
	 */
	private void updateSyncStatusForItems()
	{
		try
		{
			if(null!=categoriesList && categoriesList.size()>0)
			{
				String carlineRoot = ApplicationProperties.getProperty("PARENT_REF_KEYS_CARLINE");
				// TYPE KEY OF THE STATUS UPDATE + MDM ID OF EVERY ROW THAT FAILED SOMEWHERE
				Set<String> failed = new HashSet<String>();
				for(AutoSyncCategoryDetails catDetails : categoriesList)
				{
					if(!AutoSyncConstants.STATUS_SUCCESS.equals(catDetails.getProcessingStatus()))
					{
						failed.add(statusKey(catDetails, carlineRoot) + "|" + catDetails.getMdmItemId());
					}
				}
				for(AutoSyncCategoryDetails failureDetails : failureList)
				{
					if(null!=failureDetails.getItemType() && null!=failureDetails.getMdmItemId())
					{
						failed.add(failureStatusKey(failureDetails, carlineRoot) + "|" + failureDetails.getMdmItemId());
					}
				}

				Map<String, Set<String>> synced = new HashMap<String, Set<String>>();
				for(AutoSyncCategoryDetails catDetails : categoriesList)
				{
					if(null==catDetails.getItemType() || "".equals(catDetails.getItemType())
							|| !AutoSyncConstants.STATUS_SUCCESS.equals(catDetails.getProcessingStatus())
							|| null==catDetails.getMdmItemId() || "".equals(catDetails.getMdmItemId())
							|| incompleteItemTypes.contains(catDetails.getItemType()))
					{
						continue;
					}
					String statusKey = statusKey(catDetails, carlineRoot);
					if(null==statusKey || failed.contains(statusKey + "|" + catDetails.getMdmItemId()))
					{
						continue;
					}
					Set<String> ids = synced.get(statusKey);
					if(null==ids)
					{
						ids = new LinkedHashSet<String>();
						synced.put(statusKey, ids);
					}
					ids.add(catDetails.getMdmItemId());
				}

				Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
				String[] keys = { AutoSyncConstants.ITEM_KEY_AXLE_TYPE, AutoSyncConstants.ITEM_KEY_BODY_TYPE, AutoSyncConstants.ITEM_KEY_MODEL_YEAR,
						AutoSyncConstants.ITEM_KEY_VIN_RANGE, AutoSyncConstants.ITEM_KEY_CARLINE, "VIN_FOR_MC_MME", AutoSyncConstants.ITEM_KEY_ESI_CATEGORY,
						AutoSyncConstants.ITEM_KEY_CVC_CATEGORY, AutoSyncConstants.ITEM_KEY_ENGINE_TYPE, AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE,
						AutoSyncConstants.ITEM_KEY_MANUAL_TYPE };
				for(String key : keys)
				{
					List<String> ids = new ArrayList<String>();
					if(null!=synced.get(key))
					{
						ids.addAll(synced.get(key));
					}
					logger.info("updateSyncStatusForItems :: ------------------- " + key + " Codes :: > "+ ids.size());
					dataMap.put(key, ids);
				}
				if(!incompleteItemTypes.isEmpty())
				{
					logger.info("updateSyncStatusForItems :: no sync status Y for " + incompleteItemTypes + " - a locale could not be processed completely.");
				}

				/*
				 * call database function to update the status for each Item
				 */
				transactionDAO.updateMDMItemSyncStatus(dataMap);
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "updateSyncStatusForItems()", e);
		}
	}

	/** Key of the MDM table a processed row belongs to (the keys of updateMDMItemSyncStatus). */
	private static String statusKey(AutoSyncCategoryDetails row, String carlineRoot)
	{
		if(AutoSyncConstants.ITEM_KEY_CARLINE.equals(row.getItemType()))
		{
			// LEVEL 1 UNDER CARLINE = CARLINE ROW, OTHERWISE A VIN ROW (legacy check)
			if(null!=row.getLevel1Details() && null!=row.getLevel1Details().getParentRefKey()
					&& row.getLevel1Details().getParentRefKey().equals(carlineRoot))
			{
				return AutoSyncConstants.ITEM_KEY_CARLINE;
			}
			return "VIN_FOR_MC_MME";
		}
		return row.getItemType();
	}

	/** Same key for a failure entry, which is the level that failed (legacy: parent ref key of the level). */
	private static String failureStatusKey(AutoSyncCategoryDetails failure, String carlineRoot)
	{
		if(AutoSyncConstants.ITEM_KEY_CARLINE.equals(failure.getItemType()))
		{
			return null!=failure.getParentRefKey() && failure.getParentRefKey().equals(carlineRoot)
					? AutoSyncConstants.ITEM_KEY_CARLINE : "VIN_FOR_MC_MME";
		}
		return failure.getItemType();
	}

	private void generateReports(String scheduleId)
	{
		try
		{
			if(null!=categoriesList && categoriesList.size()>0)
			{
				int partitionSize=10000;
				ArrayList<List<AutoSyncCategoryDetails>> partitions = new ArrayList<List<AutoSyncCategoryDetails>>();
				for (int i=0; i<categoriesList.size(); i += partitionSize) {
			        partitions.add(categoriesList.subList(i, Math.min(i + partitionSize, categoriesList.size())));
			    }

				int listCount=0;
				if(null!=partitions && partitions.size()>0)
				{
					for(List<AutoSyncCategoryDetails> subList : partitions)
					{
						listCount++;
						if(null!=subList && subList.size()>0)
						{
							printReportUtils.printTransactionReport(subList, scheduleId, listCount);
						}
						subList = null;
					}
				}
				partitions = null;
			}

			if(null!=failureList && failureList.size()>0)
			{
				int partitionSize=10000;
				ArrayList<List<AutoSyncCategoryDetails>> partitions = new ArrayList<List<AutoSyncCategoryDetails>>();
				for (int i=0; i<failureList.size(); i += partitionSize) {
			        partitions.add(failureList.subList(i, Math.min(i + partitionSize, failureList.size())));
			    }

				int listCount=0;
				if(null!=partitions && partitions.size()>0)
				{
					for(List<AutoSyncCategoryDetails> subList : partitions)
					{
						listCount++;
						if(null!=subList && subList.size()>0)
						{
							printReportUtils.printFailureReport(subList, scheduleId, listCount);
						}
						subList = null;
					}
				}
				partitions = null;
			}


			/*
			 * now here, generate the Zip file for all the reports generated the
			 * path for reports directory will be -
			 * REPORTS_DIRECTORY/schCode
			 */
			String path = ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");
			if(!path.endsWith("/"))
			{
				path = path+"/";
			}
			path=path+scheduleId+"/";

			/*
			 * call function to generate zip file
			 */
			boolean bool = printReportUtils.createReportsZip(path, scheduleId);
			if (bool == true)
			{
				logger.info("generateReports :: Reports Zipped Successfully.");
			}
			else
			{
				logger.info("generateReports :: Failed to Zip Reports, these have to be downloaded manually.");
			}
			path = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSynchingImpl.class.getName(), "generateReports()", e);
		}
	}
}
