package com.mazda.gms3.dmt.masterdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncTransactionDAO;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureApiResult;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;

/**
 * SENDS THE MASTER DATA THAT IS WAITING IN MDM (sync status N) TO KAPTURE AS CATEGORIES.
 *
 * Which rows wait, and the reference key, name and parent of each category level, come from
 * the same queries the Master Data Sync screen uses (autosync DAOs) - only the target changed
 * from InfoManager to the Kapture category API:
 *
 *   CARLINE            MC / MME: carline (level 1) and its VIN ranges (levels 2 to 5)
 *   MODEL_YEAR         MNAO: model (level 1) and <MODEL>_<YEAR> for each year of the carline (level 2)
 *   VIN                MNAO: WMI / WMI+VDS / WMI+VDS+VIS start+VIS end (levels 1 to 3)
 *   ESI                category / sub category / sub sub category
 *   ENGINE_TYPE        group type (level 1) and engine type (level 2)
 *   TRANSMISSION_TYPE  transmission type
 *   AXLE_TYPE          drive axle type (all markets)
 *   BODY_TYPE          body type (all markets)
 *
 * Engine books and transmission books are not categories.
 *
 * A category is created parent first. The top category of a tree (CARLINE, ESI, ENGINE_TYPE,
 * TRANSMISSION_TYPE, MODEL_YEAR, VIN, AXLE_TYPE, BODY_TYPE) is created when Kapture does not have it
 * yet for the locale; any other missing parent is a failure of that row. A row of MDM gets sync
 * status Y only when every one of its levels is in Kapture (for MODEL_YEAR: every year of the carline).
 * The autosync DAOs return a type only for the markets it belongs to (CARLINE MC / MME, MODEL_YEAR
 * and VIN MNAO).
 */
public class KaptureCategorySync {

	private static Logger logger = LogManager.getLogger(KaptureCategorySync.class);

	private static final String[] TYPES = { AutoSyncConstants.ITEM_KEY_CARLINE, AutoSyncConstants.ITEM_KEY_MODEL_YEAR,
			AutoSyncConstants.ITEM_KEY_VIN_RANGE, AutoSyncConstants.ITEM_KEY_ESI_CATEGORY, AutoSyncConstants.ITEM_KEY_ENGINE_TYPE,
			AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE, AutoSyncConstants.ITEM_KEY_AXLE_TYPE, AutoSyncConstants.ITEM_KEY_BODY_TYPE };

	/** One line per category handled. */
	public final List<MasterDataRecord> records = new ArrayList<MasterDataRecord>();

	private KaptureApiClient api = null;
	private String kaptureLocale;
	private String locale;
	private String type;
	/** Upper-cased reference key -> name, for every category known to be in Kapture for the locale. */
	private Map<String, String> known;
	/** Upper-cased reference key -> outcome, for every category already handled in this run. */
	private Map<String, Boolean> done;

	/**
	 * @param market        MC / MME / MNAO
	 * @param locale        MDM locale, e.g. ja-JP
	 * @param kaptureLocale Kapture locale, e.g. ja_JP
	 */
	public void sync(String market, String locale, String kaptureLocale) {
		this.locale = locale;
		this.kaptureLocale = kaptureLocale;
		MasterDataSyncDAO dao = new MasterDataSyncDAO();
		for (int t = 0; t < TYPES.length; t++) {
			type = TYPES[t];
			try {
				MarketLocaleMasterDataTypeMapping mapping = new MarketLocaleMasterDataTypeMapping();
				mapping.setMarket(market);
				mapping.setLocale(locale);
				mapping.setAlternateLocale(locale);
				mapping.setMasterDataType(type);
				List<AutoSyncCategoryDetails> items = dao.getCategoriesListForProcessing(type, market, mapping);
				logger.info("sync :: " + type + " :: " + locale + " :: rows waiting for Kapture :: > " + (null == items ? 0 : items.size()));
				if (null == items || items.isEmpty()) {
					continue;
				}
				if (null == api) {
					api = new KaptureApiClient();
				}
				syncType(items);
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(KaptureCategorySync.class.getName(), "sync()", e);
				MasterDataRecord rec = new MasterDataRecord();
				rec.type = type;
				rec.locale = locale;
				rec.result = MasterDataRecord.RESULT_FAILED;
				rec.message = "Categories of this type could not be processed: " + e.getMessage();
				records.add(rec);
			}
		}
	}

	private void syncType(List<AutoSyncCategoryDetails> items) {
		// EVERYTHING KAPTURE ALREADY HAS, IN ONE LOOKUP
		Set<String> refKeys = new LinkedHashSet<String>();
		for (AutoSyncCategoryDetails item : items) {
			for (int level = 1; level <= 5; level++) {
				AutoSyncCategoryDetails d = level(item, level);
				if (null != d && null != d.getCategoryRefKey()) {
					refKeys.add(key(d.getCategoryRefKey()));
					refKeys.add(key(d.getParentRefKey()));
				}
			}
		}
		known = new HashMap<String, String>();
		for (Map.Entry<String, String[]> e : KaptureLookupDAO.getCategories(new ArrayList<String>(refKeys), kaptureLocale).entrySet()) {
			known.put(e.getKey(), null == e.getValue()[1] ? "" : e.getValue()[1].trim());
		}
		done = new HashMap<String, Boolean>();

		ArrayList<String> level1Ids = new ArrayList<String>();
		ArrayList<String> otherIds = new ArrayList<String>();
		// an MDM row with several items (MODEL_YEAR: one item per year of the carline) is Y only when all are in Kapture
		Set<String> failedIds = new java.util.HashSet<String>();
		Set<String> seenIds = new java.util.HashSet<String>();
		for (AutoSyncCategoryDetails item : items) {
			boolean ok = true;
			boolean any = false;
			for (int level = 1; level <= 5 && ok; level++) {
				AutoSyncCategoryDetails d = level(item, level);
				if (null == d || null == d.getCategoryRefKey() || "".equals(d.getCategoryRefKey().trim())) {
					continue;
				}
				any = true;
				ok = ensure(level, key(d.getCategoryRefKey()), d.getCategoryName(), key(d.getParentRefKey()));
			}
			if (!ok && null != item.getMdmItemId()) {
				failedIds.add(item.getMdmItemId());
			}
			if (ok && any && null != item.getMdmItemId() && seenIds.add(item.getMdmItemId())) {
				// A CARLINE ITEM IS EITHER A CARLINE ROW (LEVEL 1 ONLY) OR A VIN ROW (LEVELS 2 TO 5)
				if (null != item.getLevel1Details() && null == item.getLevel2Details()) {
					level1Ids.add(item.getMdmItemId());
				} else {
					otherIds.add(item.getMdmItemId());
				}
			}
		}

		level1Ids.removeAll(failedIds);
		otherIds.removeAll(failedIds);

		// SYNC STATUS Y FOR THE ROWS THAT ARE COMPLETELY IN KAPTURE
		Map<String, List<String>> synced = new HashMap<String, List<String>>();
		if (AutoSyncConstants.ITEM_KEY_CARLINE.equals(type)) {
			synced.put(AutoSyncConstants.ITEM_KEY_CARLINE, level1Ids);
			synced.put("VIN_FOR_MC_MME", otherIds);
		} else {
			otherIds.addAll(level1Ids);
			synced.put(type, otherIds);
		}
		logger.info("syncType :: " + type + " :: " + locale + " :: MDM rows " + items.size() + " :: completely in Kapture (sync status Y) "
				+ (level1Ids.size() + otherIds.size() - (AutoSyncConstants.ITEM_KEY_CARLINE.equals(type) ? 0 : level1Ids.size())));
		new MasterDataSyncTransactionDAO().updateMDMItemSyncStatus(synced);
	}

	/**
	 * Makes sure one category is in Kapture with the given name.
	 */
	private boolean ensure(int level, String ref, String name, String parent) {
		if (done.containsKey(ref)) {
			return done.get(ref).booleanValue();
		}
		name = null == name || "".equals(name.trim()) ? ref : name.trim();
		MasterDataRecord rec = new MasterDataRecord();
		rec.type = type;
		rec.locale = locale;
		rec.categoryLevel = String.valueOf(level);
		rec.categoryRefKey = ref;
		rec.categoryName = name;
		rec.parentRefKey = parent;
		try {
			if (!"".equals(parent) && !known.containsKey(parent)) {
				if (level == 1) {
					// THE TOP OF THE TREE - CREATED WHEN KAPTURE DOES NOT HAVE IT YET
					if (!ensureRoot(parent)) {
						throw new IllegalStateException("Top category " + parent + " is not in Kapture and could not be created.");
					}
				} else {
					throw new IllegalStateException("Parent category " + parent + " is not in Kapture for locale " + kaptureLocale + ".");
				}
			}
			if (known.containsKey(ref)) {
				if (known.get(ref).equals(name)) {
					rec.result = MasterDataRecord.RESULT_NO_CHANGE;
				} else {
					KaptureApiResult r = api.updateCategory(ref, name, kaptureLocale);
					if (!r.isOk()) {
						throw new IllegalStateException("Update failed :: " + r.describe());
					}
					known.put(ref, name);
					rec.result = MasterDataRecord.RESULT_UPDATED;
				}
			} else {
				create(ref, name, parent);
				rec.result = MasterDataRecord.RESULT_ADDED;
			}
		} catch (Exception e) {
			rec.result = MasterDataRecord.RESULT_FAILED;
			rec.message = null == e.getMessage() ? e.toString() : e.getMessage();
			logger.info("ensure :: " + type + " :: LEVEL " + level + " :: " + ref + " :: FAILED :: " + rec.message);
		}
		if (!rec.failed()) {
			logger.info("ensure :: " + type + " :: LEVEL " + level + " :: " + ref + " (parent " + parent + ") :: " + rec.result);
		}
		records.add(rec);
		done.put(ref, Boolean.valueOf(!rec.failed()));
		return !rec.failed();
	}

	private boolean ensureRoot(String root) {
		if (done.containsKey(root)) {
			return done.get(root).booleanValue();
		}
		MasterDataRecord rec = new MasterDataRecord();
		rec.type = type;
		rec.locale = locale;
		rec.categoryLevel = "0";
		rec.categoryRefKey = root;
		rec.categoryName = root;
		try {
			create(root, root, "");
			rec.result = MasterDataRecord.RESULT_ADDED;
		} catch (Exception e) {
			rec.result = MasterDataRecord.RESULT_FAILED;
			rec.message = null == e.getMessage() ? e.toString() : e.getMessage();
		}
		logger.info("ensureRoot :: " + type + " :: TOP CATEGORY " + root + " :: " + rec.result + " " + rec.message);
		records.add(rec);
		done.put(root, Boolean.valueOf(!rec.failed()));
		return !rec.failed();
	}

	/**
	 * Creates the category and waits until Kapture really has it: the answer of the create call
	 * alone is not proof, the row appears in k_categories a few seconds later.
	 */
	private void create(String ref, String name, String parent) {
		KaptureApiResult r = api.createCategory(ref, name, parent, kaptureLocale);
		if (!r.isOk() && !KaptureApiClient.isAlreadyExists(r)) {
			throw new IllegalStateException("Create failed :: " + r.describe());
		}
		int attempts = KaptureApiClient.requireInt("kapture.category.readback.attempts");
		long delay = KaptureApiClient.requireInt("kapture.category.readback.delay.ms");
		for (int a = 0; a < attempts; a++) {
			if (KaptureLookupDAO.getCategories(Collections.singletonList(ref), kaptureLocale).containsKey(ref)) {
				known.put(ref, name);
				return;
			}
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		throw new IllegalStateException("Kapture answered the create (" + r.describe() + ") but the category is not there.");
	}

	private static AutoSyncCategoryDetails level(AutoSyncCategoryDetails item, int level) {
		switch (level) {
		case 1:
			return item.getLevel1Details();
		case 2:
			return item.getLevel2Details();
		case 3:
			return item.getLevel3Details();
		case 4:
			return item.getLevel4Details();
		default:
			return item.getLevel5Details();
		}
	}

	private static String key(String refKey) {
		return null == refKey ? "" : refKey.trim().toUpperCase();
	}
}
