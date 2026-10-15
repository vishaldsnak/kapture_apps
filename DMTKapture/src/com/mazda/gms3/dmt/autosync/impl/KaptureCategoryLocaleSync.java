package com.mazda.gms3.dmt.autosync.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncCountBuffer;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncCategoryDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureApiResult;
import com.mazda.gms3.dmt.kapture.KaptureCategoryStore;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;

/**
 * MASTER DATA SYNCING OF ONE ITEM (master data type) FOR ONE MAPPED LOCALE, INTO KAPTURE.
 *
 * Writes go through the Kapture category API or, with kapture.category.write.mode = DB, straight into the
 * categories table (KaptureCategoryStore: multi-row statements per level, same columns the API writes).
 *
 * Replaces the InfoManager calls of the legacy MasterDataSynchingImpl.createCategory / createCategoryOperation.
 * The rules are the legacy ones:
 *   - every level of every row is attempted, also when an earlier level of the row failed;
 *   - a category that exists gets the label of this row for the locale (legacy updateCategory);
 *   - a category that does not exist is created under its parent; a parent that is not there fails
 *     the level (InfoManager threw category.not.found) - the top category is never created;
 *   - a row is SUCCESS only when all of its levels succeeded.
 *
 * Done faster than legacy, with the same result:
 *   - what Kapture already has is read from k_categories once for all reference keys of the locale;
 *   - every distinct category is handled once per locale (legacy repeated shared parents for every row),
 *     and a category whose label is already right costs no API call;
 *   - levels are processed one after the other for all rows: every missing category of a level is created,
 *     then all of them are read back in one query, before the next level starts.
 */
public class KaptureCategoryLocaleSync {

	private static Logger logger = LogManager.getLogger(KaptureCategoryLocaleSync.class);

	private static final String OPERATION_GET = "GET_CATEGORY";
	private static final String OPERATION_CREATE = "CREATE_CATEGORY";

	/** Every row handled, with its processing status (the Transaction Report). */
	public final List<AutoSyncCategoryDetails> rows = new ArrayList<AutoSyncCategoryDetails>();
	/** One entry per failed category (the Failure Report). */
	public final List<AutoSyncCategoryDetails> failures = new ArrayList<AutoSyncCategoryDetails>();

	private final KaptureApiClient api;
	private final AtomicBoolean aborted;
	private final MasterDataSyncCountBuffer counts;
	/** DB mode: reference keys already filled into the missing Kapture default locales by a worker of this job. */
	private final Set<String> filledLocales;
	/** kapture.category.write.mode = DB: categories go straight into the categories table, not through the API. */
	private final boolean dbMode;
	private final int readbackAttempts;
	private final long readbackDelayMs;

	private String kaptureLocale;
	private String logPrefix;
	/**
	 * Matching key (see match()) -> name, for every category that is in Kapture for the locale. Always read and
	 * written through match(): the categories table compares reference keys like utf8mb4_0900_ai_ci, so a key
	 * written with a full-width letter (CVC_ＲN) IS the row stored with the normal letter (CVC_RN).
	 */
	private Map<String, String> known;
	/** Upper-cased reference key -> outcome, for every category handled in this run. */
	private final Map<String, Outcome> outcomes = new HashMap<String, Outcome>();

	private static class Outcome {
		boolean ok;
		String errorCode;
		String errorMessage;
	}

	/** A distinct category of one level: the detail it was first seen on, the label to give it, its parent. */
	private static class Category {
		AutoSyncCategoryDetails first;
		String ref;
		String name = "";
		String parent;
		/** Kapture's answer when the create was refused as "already exists" - reported if the category is still not there. */
		String refusedAnswer;
	}

	public KaptureCategoryLocaleSync(KaptureApiClient api, AtomicBoolean aborted, MasterDataSyncCountBuffer counts, Set<String> filledLocales) {
		this.api = api;
		this.aborted = aborted;
		this.counts = counts;
		this.filledLocales = filledLocales;
		this.dbMode = KaptureCategoryStore.dbMode();
		this.readbackAttempts = KaptureApiClient.requireInt("kapture.category.readback.attempts");
		this.readbackDelayMs = KaptureApiClient.requireInt("kapture.category.readback.delay.ms");
	}

	/**
	 * @param categoryList     rows of the autosync DAO for this item and locale
	 * @param processingLocale locale of the mapping row, MDM form (e.g. de-AT); Kapture's is the same with '_'
	 */
	public void run(List<AutoSyncCategoryDetails> categoryList, long itemId, long scheduleId, String market,
			String itemType, String processingLocale) {
		this.kaptureLocale = processingLocale.trim().replace("-", "_");
		this.logPrefix = itemType + " :: " + kaptureLocale + " :: ";
		long startedAt = System.currentTimeMillis();

		// PREPARE THE ROWS EXACTLY AS LEGACY createCategory DID
		Set<String> refKeys = new LinkedHashSet<String>();
		Map<AutoSyncCategoryDetails, Integer> deepest = new LinkedHashMap<AutoSyncCategoryDetails, Integer>();
		for (AutoSyncCategoryDetails row : categoryList) {
			row.setItemId(itemId);
			row.setMarket(market);
			row.setItemType(itemType);
			row.setLocale(processingLocale);
			int depth = 0;
			for (int level = 1; level <= 5; level++) {
				AutoSyncCategoryDetails d = level(row, level);
				if (!hasRef(d)) {
					continue;
				}
				d.setItemId(itemId);
				d.setMarket(market);
				d.setItemType(itemType);
				d.setCategoryRefKey(key(d.getCategoryRefKey()));
				d.setParentRefKey(key(d.getParentRefKey()));
				d.setMdmItemId(row.getMdmItemId());
				d.setCategoryLevel("LEVEL " + level);
				if (null == d.getLocale() || "".equals(d.getLocale().trim())) {
					d.setLocale(kaptureLocale);
				}
				refKeys.add(d.getCategoryRefKey());
				if (!"".equals(d.getParentRefKey())) {
					refKeys.add(d.getParentRefKey());
				}
				depth = level;
			}
			deepest.put(row, Integer.valueOf(depth));
			rows.add(row);
		}
		logger.info("run :: " + logPrefix + "rows " + categoryList.size() + " :: distinct reference keys " + refKeys.size());

		// WHAT KAPTURE ALREADY HAS - ONE LOOKUP FOR THE WHOLE LOCALE
		known = new HashMap<String, String>();
		try {
			for (Map.Entry<String, String[]> e : KaptureLookupDAO.getCategories(new ArrayList<String>(refKeys), kaptureLocale).entrySet()) {
				known.put(match(e.getValue()[0]), null == e.getValue()[1] ? "" : e.getValue()[1].trim());
			}
		} catch (RuntimeException e) {
			// legacy: a failed GET is a failure of the category - here of every row, nothing could be checked
			for (AutoSyncCategoryDetails row : rows) {
				AutoSyncCategoryDetails d = firstLevel(row);
				if (null != d) {
					addFailure(d, OPERATION_GET, "LOOKUP_FAILED", e.getMessage());
				}
				finishRow(row, new boolean[] { false, false, false, false, false, false }, scheduleId);
			}
			return;
		}

		boolean[][] levelOk = new boolean[rows.size()][6];
		for (int r = 0; r < rows.size(); r++) {
			java.util.Arrays.fill(levelOk[r], true);
			if (deepest.get(rows.get(r)).intValue() == 0) {
				// NO LEVEL TO CREATE - legacy counted the row as a success
				finishRow(rows.get(r), levelOk[r], scheduleId);
			}
		}

		for (int level = 1; level <= 5; level++) {
			checkAbort();
			processLevel(level);
			for (int r = 0; r < rows.size(); r++) {
				AutoSyncCategoryDetails d = level(rows.get(r), level);
				if (hasRef(d)) {
					levelOk[r][level] = outcomes.get(d.getCategoryRefKey()).ok;
				}
				if (deepest.get(rows.get(r)).intValue() == level) {
					finishRow(rows.get(r), levelOk[r], scheduleId);
				}
			}
		}
		logger.info("run :: " + logPrefix + "DONE :: rows " + rows.size() + " :: failed categories " + failures.size()
				+ " :: " + (System.currentTimeMillis() - startedAt) + " ms");
	}

	private void processLevel(int level) {
		// DISTINCT CATEGORIES OF THIS LEVEL, IN ROW ORDER; THE LABEL IS THAT OF THE LAST ROW (legacy updated it row by row)
		Map<String, Category> categories = new LinkedHashMap<String, Category>();
		for (AutoSyncCategoryDetails row : rows) {
			AutoSyncCategoryDetails d = level(row, level);
			if (!hasRef(d) || outcomes.containsKey(d.getCategoryRefKey())) {
				continue;
			}
			Category c = categories.get(d.getCategoryRefKey());
			if (null == c) {
				c = new Category();
				c.first = d;
				c.ref = d.getCategoryRefKey();
				c.parent = d.getParentRefKey();
				categories.put(c.ref, c);
			}
			if (null != d.getCategoryName() && !"".equals(d.getCategoryName().trim())) {
				c.name = d.getCategoryName().trim();
			}
		}
		if (categories.isEmpty()) {
			return;
		}

		recheckMissingParents(categories);

		List<Category> created = new ArrayList<Category>();
		// DB MODE: the writes of the level are collected and sent as multi-row statements after the loop
		List<Category> toRename = new ArrayList<Category>();
		List<Category> toCreate = new ArrayList<Category>();
		int unchanged = 0;
		int renamed = 0;
		for (Category c : categories.values()) {
			checkAbort();
			if (known.containsKey(match(c.ref))) {
				// EXISTS - GIVE IT THE LABEL OF THIS LOCALE (a blank label leaves the existing one, as legacy did)
				if ("".equals(c.name) || known.get(match(c.ref)).equals(c.name)) {
					succeed(c);
					unchanged++;
					continue;
				}
				if (dbMode) {
					toRename.add(c);
					continue;
				}
				KaptureApiResult r = api.updateCategory(c.ref, c.name, kaptureLocale);
				if (r.isOk()) {
					known.put(match(c.ref), c.name);
					succeed(c);
					renamed++;
					logger.info("processLevel :: " + logPrefix + "LEVEL " + level + " :: " + c.ref + " :: LABEL UPDATED");
				} else {
					fail(c, String.valueOf(r.httpStatus), "Update failed :: " + r.describe());
				}
			} else if (!"".equals(c.parent) && !known.containsKey(match(c.parent))) {
				// legacy: getCategoryKeyByReferenceKey threw category.not.found -> the level failed
				fail(c, "PARENT_NOT_FOUND", "Parent category " + c.parent + " is not in Kapture for locale " + kaptureLocale + ".");
			} else if (dbMode) {
				toCreate.add(c);
			} else {
				KaptureApiResult r = api.createCategory(c.ref, "".equals(c.name) ? c.ref : c.name, c.parent, kaptureLocale);
				if (r.isOk() || KaptureApiClient.isAlreadyExists(r)) {
					// a 409 may mean the reference key is there already (e.g. another case) - the read back decides
					c.refusedAnswer = r.isOk() ? null : r.describe();
					created.add(c);
				} else {
					fail(c, String.valueOf(r.httpStatus), "Create failed :: " + r.describe());
				}
			}
		}
		if (dbMode) {
			renamed = writeToTable(level, toRename, toCreate, created);
		}
		readBack(level, created);
		if (dbMode) {
			fillMissingLocales(level, categories.values(), created);
		}
		logger.info("processLevel :: " + logPrefix + "LEVEL " + level + " :: categories " + categories.size() + " :: unchanged "
				+ unchanged + " :: label updated " + renamed + " :: create requests " + created.size());
	}

	/**
	 * DB mode: the label updates and the new categories of one level, each in multi-row statements. A failed
	 * statement fails every category it carried. New categories are then read back like API creates.
	 * @return number of labels updated
	 */
	private int writeToTable(int level, List<Category> toRename, List<Category> toCreate, List<Category> created) {
		int renamed = 0;
		if (!toRename.isEmpty()) {
			try {
				KaptureCategoryStore.updateNames(rows(toRename), kaptureLocale);
				for (Category c : toRename) {
					known.put(match(c.ref), c.name);
					succeed(c);
					logger.info("processLevel :: " + logPrefix + "LEVEL " + level + " :: " + c.ref + " :: LABEL UPDATED (DB)");
				}
				renamed = toRename.size();
			} catch (RuntimeException e) {
				for (Category c : toRename) {
					fail(c, "DB_WRITE_FAILED", "Label update failed :: " + e.getMessage());
				}
			}
		}
		if (!toCreate.isEmpty()) {
			try {
				KaptureCategoryStore.insertNew(rows(toCreate), kaptureLocale, filledLocales);
				// insertNew also copies a new category into every other default locale; a locale worker that created
				// the same category a moment earlier has then left its own label in this locale - put ours back
				KaptureCategoryStore.updateNames(rows(toCreate), kaptureLocale);
				created.addAll(toCreate);
			} catch (RuntimeException e) {
				for (Category c : toCreate) {
					fail(c, "DB_WRITE_FAILED", "Create failed :: " + e.getMessage());
				}
			}
		}
		return renamed;
	}

	/**
	 * A parent that was not there when the locale was looked up may have been written since by another locale worker
	 * of this job (new categories go into every default locale) - looked up once more before the level fails.
	 */
	private void recheckMissingParents(Map<String, Category> categories) {
		Set<String> parents = new LinkedHashSet<String>();
		for (Category c : categories.values()) {
			if (!known.containsKey(match(c.ref)) && !"".equals(c.parent) && !known.containsKey(match(c.parent))) {
				parents.add(c.parent);
			}
		}
		if (parents.isEmpty()) {
			return;
		}
		try {
			for (String[] row : KaptureLookupDAO.getCategories(new ArrayList<String>(parents), kaptureLocale).values()) {
				known.put(match(row[0]), null == row[1] ? "" : row[1].trim());
			}
		} catch (RuntimeException e) {
			// the level fails on the missing parents as before
			logger.info("recheckMissingParents :: " + logPrefix + "lookup failed :: " + e.getMessage());
		}
	}

	/**
	 * DB mode: every category of the level that is in Kapture for this locale (unchanged or label updated) is also
	 * written into the Kapture default locales that do not have it yet, with this locale's label - so a category is
	 * in every locale whichever screen processes it. New categories were filled by insertNew already. A failed fill
	 * fails the category (the row stays N and is done again by the next run).
	 */
	private void fillMissingLocales(int level, java.util.Collection<Category> categories, List<Category> created) {
		Set<String> createdRefs = new java.util.HashSet<String>();
		for (Category c : created) {
			createdRefs.add(c.ref);
		}
		List<Category> existing = new ArrayList<Category>();
		List<KaptureCategoryStore.Row> fill = new ArrayList<KaptureCategoryStore.Row>();
		for (Category c : categories) {
			Outcome o = outcomes.get(c.ref);
			if (null == o || !o.ok || createdRefs.contains(c.ref)) {
				continue;
			}
			String name = "".equals(c.name) ? known.get(match(c.ref)) : c.name;
			existing.add(c);
			fill.add(new KaptureCategoryStore.Row(c.ref, null == name || "".equals(name) ? c.ref : name, c.parent));
		}
		if (fill.isEmpty()) {
			return;
		}
		try {
			int written = KaptureCategoryStore.fillMissingLocales(fill, filledLocales);
			if (written > 0) {
				logger.info("processLevel :: " + logPrefix + "LEVEL " + level + " :: rows added for missing locales " + written);
			}
		} catch (RuntimeException e) {
			for (Category c : existing) {
				fail(c, "DB_WRITE_FAILED", "Adding the category to the missing locales failed :: " + e.getMessage());
			}
		}
	}

	private static List<KaptureCategoryStore.Row> rows(List<Category> categories) {
		List<KaptureCategoryStore.Row> rows = new ArrayList<KaptureCategoryStore.Row>();
		for (Category c : categories) {
			rows.add(new KaptureCategoryStore.Row(c.ref, "".equals(c.name) ? c.ref : c.name, c.parent));
		}
		return rows;
	}

	/**
	 * The answer of a create is not proof - the row appears in k_categories a moment later. All categories
	 * created for a level are read back together.
	 */
	private void readBack(int level, List<Category> created) {
		List<Category> pending = new ArrayList<Category>(created);
		// a DB write is visible at once - one look; an API create may need a few
		int attempts = dbMode ? 1 : readbackAttempts;
		for (int a = 0; a < attempts && !pending.isEmpty(); a++) {
			if (a > 0) {
				try {
					Thread.sleep(readbackDelayMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					checkAbort();
				}
			}
			List<String> refs = new ArrayList<String>();
			for (Category c : pending) {
				refs.add(c.ref);
			}
			Map<String, String[]> found;
			try {
				found = KaptureLookupDAO.getCategories(refs, kaptureLocale);
			} catch (RuntimeException e) {
				for (Category c : pending) {
					fail(c, "LOOKUP_FAILED", "Created, but the read back failed :: " + e.getMessage());
				}
				return;
			}
			Set<String> foundKeys = new java.util.HashSet<String>();
			for (String[] row : found.values()) {
				foundKeys.add(match(row[0]));
			}
			List<Category> still = new ArrayList<Category>();
			for (Category c : pending) {
				if (foundKeys.contains(match(c.ref))) {
					known.put(match(c.ref), "".equals(c.name) ? c.ref : c.name);
					succeed(c);
				} else if (null != c.refusedAnswer) {
					// a refused create wrote nothing - one look is enough, waiting would not change the answer
					fail(c, "REFUSED", "Create refused :: " + c.refusedAnswer);
				} else {
					still.add(c);
				}
			}
			pending = still;
		}
		for (Category c : pending) {
			fail(c, "NOT_CREATED", dbMode ? "Written to the categories table but not found when read back."
					: "Kapture answered the create but the category is not there.");
		}
	}

	private void succeed(Category c) {
		Outcome o = new Outcome();
		o.ok = true;
		outcomes.put(c.ref, o);
	}

	private void fail(Category c, String code, String message) {
		Outcome o = new Outcome();
		o.ok = false;
		o.errorCode = code;
		o.errorMessage = message;
		outcomes.put(c.ref, o);
		logger.info("fail :: " + logPrefix + c.first.getCategoryLevel() + " :: " + c.ref + " :: " + message);
		// legacy: one failure entry per category and locale (a later row skipped a category already in the failure list)
		addFailure(c.first, OPERATION_CREATE, code, message);
	}

	private void addFailure(AutoSyncCategoryDetails d, String operation, String code, String message) {
		d.setOperationType(operation);
		d.setErrorCode(code);
		d.setErrorMessage(message);
		failures.add(d);
	}

	/** Same status, operation type and remarks as legacy createCategory. */
	private void finishRow(AutoSyncCategoryDetails row, boolean[] levelOk, long scheduleId) {
		row.setOperationType(OPERATION_CREATE);
		StringBuilder failed = new StringBuilder();
		for (int level = 1; level <= 5; level++) {
			if (!levelOk[level] && hasRef(level(row, level))) {
				failed.append(failed.length() > 0 ? "," : "").append(" LEVEL ").append(level);
			}
		}
		boolean lookupFailed = !levelOk[0];
		if (failed.length() == 0 && !lookupFailed) {
			row.setProcessingStatus(AutoSyncConstants.STATUS_SUCCESS);
			counts.add(row.getItemId(), true);
		} else {
			row.setProcessingStatus(AutoSyncConstants.STATUS_FAILURE);
			row.setErrorMessage("Failed to create " + (failed.length() > 0 ? failed.toString() : " LEVEL 1")
					+ " Categories. Please refer to Failure Report for more details.");
			counts.add(row.getItemId(), false);
		}
	}

	private void checkAbort() {
		if (aborted.get()) {
			throw new IllegalStateException("Master data syncing aborted");
		}
	}

	private static AutoSyncCategoryDetails firstLevel(AutoSyncCategoryDetails row) {
		for (int level = 1; level <= 5; level++) {
			if (hasRef(level(row, level))) {
				return level(row, level);
			}
		}
		return null;
	}

	private static boolean hasRef(AutoSyncCategoryDetails d) {
		return null != d && null != d.getCategoryRefKey() && !"".equals(d.getCategoryRefKey().trim());
	}

	static AutoSyncCategoryDetails level(AutoSyncCategoryDetails item, int level) {
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

	/**
	 * How the categories table compares reference keys (utf8mb4_0900_ai_ci): letter width and case do not count.
	 * NFKC turns full-width letters and digits into the normal ones (Ｒ -> R); then upper case.
	 * Only used to MATCH keys - every key is written exactly as MDM gives it.
	 */
	static String match(String refKey) {
		return null == refKey ? "" : java.text.Normalizer.normalize(refKey.trim(), java.text.Normalizer.Form.NFKC).toUpperCase(java.util.Locale.ROOT);
	}

	private static String key(String refKey) {
		return null == refKey ? "" : refKey.trim().toUpperCase();
	}
}
