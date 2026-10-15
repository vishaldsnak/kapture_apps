package com.mazda.gms3.mdm.kapture;

import java.sql.Connection;
import java.util.ArrayList;

import com.mazda.gms3.mdm.dao.KaptureCategoryDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;

/**
 * CREATES AND UPDATES KAPTURE CATEGORIES THROUGH THE REST API.
 *
 * REPLACES THE InQuira CATEGORY WRITES used by the SI VIN screens:
 *   IQ getCategoryRequest().addCategory    -> KaptureApiClient.createCategory
 *   IQ getCategoryRequest().updateCategory -> KaptureApiClient.updateCategory
 *
 * WHY NOT SQL: kapture_cms_db is READ ONLY to this application - the account carries
 * GRANT SELECT and nothing more - so k_categories cannot be written to directly. The API is the
 * only supported write path, and it is the one IMCategoryImportJob (Job 4) used to load all 51
 * locales, so its behaviour here is already proven at volume.
 *
 * READS STILL GO STRAIGHT TO SQL through KaptureCategoryDAO. Reading is granted, and a query
 * answers "does this key exist for this locale" and "what is its parent chain" in one round trip
 * where the API would need several. So this class READS over JDBC and WRITES over REST.
 *
 * THERE IS NO TRANSACTION. Each category is a separate HTTP call that commits on the server, so a
 * failure part-way through leaves the levels already written in place. Rows are therefore written
 * PARENT BEFORE CHILD, which means an interrupted run leaves a SHORTER but still VALID chain
 * rather than an orphaned child - and re-running simply continues from where it stopped, because
 * a category that is already there reports "already exists" and is treated as success.
 */
public class KaptureCategoryServiceImpl {

	static Logger logger = LogManager.getLogger(KaptureCategoryServiceImpl.class);

	/** How many times a freshly created category is looked for before giving up. */
	private static final int CATEGORY_READBACK_ATTEMPTS = 4;

	private static final long CATEGORY_READBACK_DELAY_MS = 1500L;

	/**
	 * REPLACEMENT FOR InfoManagerServiceImpl.createCategory(catList, wslId).
	 *
	 * The screen hands over a list already ordered PARENT BEFORE CHILD - the SI VIN screens build
	 * it in three passes, WMI then VDS then VIS range - which is exactly the order the rows have to
	 * be written in, so the list is sent as-is rather than re-sorted.
	 *
	 * ADD-OR-UPDATE, as the old code was: it looked the category up first and called updateCategory
	 * when it was already there, addCategory otherwise. The same decision is made here, from a
	 * single SELECT per level.
	 *
	 * @param categories parent-before-child, each carrying refKey / name / parentRefKey
	 * @param locale     locale to write them for
	 * @param wslId      the requesting user, for the log trail only
	 * @return true when every category ended up present
	 */
	public static boolean createCategories(ArrayList<IMCategoryDetails> categories, String locale,
			String wslId) {
		Connection cmsConn = null;
		boolean ok = true;
		try {
			if (null == categories || categories.size() <= 0) {
				return true;
			}
			cmsConn = DBConnectionHelper.getCMSConnection();
			if (null == cmsConn) {
				logger.info("createCategories :: no CMS connection to check existing categories - "
						+ categories.size() + " categor(y/ies) NOT written for {" + locale + "}");
				return false;
			}
			KaptureApiClient api = new KaptureApiClient();

			for (IMCategoryDetails category : categories) {
				if (null == category || "".equals(safeTrim(category.getCategoryRefKey()))) {
					continue;
				}
				String refKey = safeTrim(category.getCategoryRefKey());
				String rowLocale = safeTrim(category.getLocale());
				if ("".equals(rowLocale)) {
					rowLocale = safeTrim(locale);
				}
				if (!writeCategory(api, category, refKey, effectiveName(category),
						safeTrim(category.getParentRefKey()), rowLocale, cmsConn)) {
					ok = false;
					/*
					 * STOP AT THE FIRST FAILURE. The list is parent-before-child, so carrying on
					 * would try to create a child under a parent that was just rejected.
					 */
					break;
				}
			}
			logger.info("createCategories :: " + categories.size() + " categor(y/ies) requested for {"
					+ locale + "} by {" + wslId + "} - success=" + ok);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryServiceImpl.class.getName(),
					"createCategories()", e);
			ok = false;
		} finally {
			closeQuietly(cmsConn);
		}
		return ok;
	}

	/**
	 * REPLACEMENT FOR InfoManagerServiceImpl.getCategoryDetails(list, wslId) AS USED BY
	 * modelExistsInIM.
	 *
	 * The old call asked InfoManager "which of these reference keys exist?" and the screen then
	 * split the list into exists / does-not-exist, erroring on the latter. Two things differ under
	 * Kapture and BOTH are preserved here:
	 *
	 *   - a reference key exists PER LOCALE, so a key present for another locale but not this one
	 *     is not "missing" - it is created for this locale, together with every level above it
	 *     (the common case, since the tree is replicated per locale);
	 *   - a key that exists NOWHERE is still genuinely missing and is returned to the caller, so
	 *     the screen reports it exactly as it did before.
	 *
	 * @param referenceKeys the keys to ensure - not modified
	 * @param locale        locale the screen is working in
	 * @param sourceLocale  locale to copy a missing chain from (MNAO: en_US)
	 * @return the keys that could NOT be ensured, i.e. genuinely absent. Never null.
	 */
	public static ArrayList<String> ensureCategoriesForLocale(ArrayList<String> referenceKeys,
			String locale, String sourceLocale) {
		ArrayList<String> missing = new ArrayList<String>();
		Connection cmsConn = null;
		try {
			if (null == referenceKeys || referenceKeys.size() <= 0) {
				return missing;
			}
			cmsConn = DBConnectionHelper.getCMSConnection();
			if (null == cmsConn) {
				logger.info("ensureCategoriesForLocale :: no CMS connection - treating all "
						+ referenceKeys.size() + " key(s) as missing.");
				missing.addAll(referenceKeys);
				return missing;
			}
			KaptureApiClient api = new KaptureApiClient();

			for (String referenceKey : referenceKeys) {
				if (null == referenceKey || "".equals(referenceKey.trim())) {
					continue;
				}
				if (!ensureCategoryForLocale(api, referenceKey.trim(), locale, sourceLocale, cmsConn)) {
					missing.add(referenceKey);
				}
			}
			logger.info("ensureCategoriesForLocale :: " + referenceKeys.size() + " key(s) checked for {"
					+ locale + "}, " + missing.size() + " genuinely absent.");
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryServiceImpl.class.getName(),
					"ensureCategoriesForLocale()", e);
			/*
			 * NOTHING WAS RELIABLY CREATED, SO NOTHING MAY BE REPORTED AS PRESENT. Treating the
			 * whole batch as missing keeps the screen on its existing error path rather than
			 * letting it proceed to map categories that may not be there.
			 */
			missing = new ArrayList<String>();
			if (null != referenceKeys) {
				missing.addAll(referenceKeys);
			}
		} finally {
			closeQuietly(cmsConn);
		}
		return missing;
	}

	/**
	 * THE WHOLE "MAKE SURE THIS CATEGORY IS USABLE FOR THIS LOCALE" STEP, FOR ONE KEY.
	 *
	 * Answers whether the category is available for the wanted locale and, when it is not, creates
	 * it - together with every level above it - by copying the chain from a locale that does have
	 * it.
	 *
	 * ONLY MISSING LEVELS ARE CREATED; a level that already exists for the target locale is left
	 * exactly as it is. That is deliberate: the ancestor already carries a name written for ITS
	 * OWN locale, and pushing the source locale's name over it would silently mistranslate a node
	 * shared by every VIN range beneath it. The shape is what has to be replicated, not the text.
	 *
	 * @param referenceKey the LAST level, i.e. the one that will be mapped onto the document
	 */
	/**
	 * THE PARENT HAS TO BE THERE BEFORE THE CHILD - and until now nobody checked.
	 *
	 * The SI VIN screens queue only the VDS and VIS levels for creation and take the MODEL
	 * and WMI levels above them on trust. The model IS verified separately
	 * (ensureCategoriesForLocale, called with the model reference key), THE WMI LEVEL WAS
	 * NOT, so a locale whose tree stopped at the model produced nothing but a 404 from the
	 * child's create: SI1092 / en_EU holds CX7_ER but not CX7_ERJM3, and the run died on
	 * "Parent category not found for reference key: CX7_ERJM3".
	 *
	 * GUARDING THE PARENT RATHER THAN A PARTICULAR LEVEL covers level 2 down to the last
	 * one without this code knowing how any market shapes its keys - including the rule
	 * that a WMI of "-" becomes "___", which is applied where the key is built.
	 *
	 * THE MODEL LEVEL IS NOT BUILT HERE. It is verified up front by
	 * ensureCategoriesForLocale and reported to the user when absent, which is the agreed
	 * split: the model raises a validation error, everything from the WMI level down is
	 * created or updated as needed.
	 *
	 * A ROOT HAS NO PARENT and passes straight through, as does a parent already present.
	 */
	/**
	 * DID THE ROW ACTUALLY LAND? Asked more than once, because a create is not instant.
	 *
	 * Kapture replicates a new category to EVERY locale it knows - 42 of them - and the rows
	 * appear over a few seconds: the MME chain created on 2026-08-02 spread from 12:58:37 to
	 * 12:58:41. Reading back the instant the 200 arrives can therefore miss a row that is
	 * perfectly well on its way, and a single negative answer used to be reported as "could
	 * not be created" - failing a run that had in fact worked.
	 *
	 * A FEW SHORT WAITS, NOT AN OPEN-ENDED ONE. This runs inside a user request, so it gives
	 * the server a moment and then gives up rather than holding the screen.
	 */
	private static boolean waitForCategory(String referenceKey, String locale,
			Connection cmsConn) {
		for (int attempt = 1; attempt <= CATEGORY_READBACK_ATTEMPTS; attempt++) {
			if (KaptureCategoryDAO.categoryExists(referenceKey, locale, cmsConn)) {
				if (attempt > 1) {
					logger.info("waitForCategory :: {" + referenceKey + "} appeared for {" + locale
							+ "} on attempt " + attempt + ".");
				}
				return true;
			}
			if (attempt < CATEGORY_READBACK_ATTEMPTS) {
				try {
					Thread.sleep(CATEGORY_READBACK_DELAY_MS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		return false;
	}

	private static boolean ensureParent(KaptureApiClient api, IMCategoryDetails category,
			String refKey, String parentRefKey, String locale, Connection cmsConn) {
		try {
			String parent = safeTrim(parentRefKey);
			if ("".equals(parent) || null == cmsConn) {
				return true;
			}
			if (KaptureCategoryDAO.categoryExists(parent, locale, cmsConn)) {
				return true;
			}
			/*
			 * BUILT FROM THE MDM VALUES, NEVER COPIED FROM ANOTHER LOCALE.
			 *
			 * A k_categories NAME IS LOCALE-SPECIFIC - the SAME reference key reads as the
			 * Japanese regional name in ja_JP and as "CX-7 ER" in en_EU - so lifting the row
			 * from whichever locale happens to hold the key would put the wrong language on
			 * the new category. The names come from the MDM carline and VIN masters
			 * (mdm_crln_name_regional_lang for the model level, the WMI / VDS / VIS codes
			 * below it), which the screen has already resolved onto the item.
			 */
			logger.info("ensureParent :: {" + parent + "} missing for {" + locale
					+ "} - building it before writing {" + refKey + "}.");
			return buildParentFromItem(api, category, parent, refKey, locale, cmsConn);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryServiceImpl.class.getName(),
					"ensureParent()", e);
			return false;
		}
	}

	/**
	 * CREATES A PARENT THAT EXISTS IN NO LOCALE, from the item its child was built from.
	 *
	 * Only the WMI level is ever in this position: the screens queue the VDS and VIS levels,
	 * the model level is verified separately, and everything above that is master data. The
	 * SIVinDetails on the category carries all three values needed - second level key, second
	 * level NAME (the WMI code as displayed, "-" or e.g. "JM3"), and the first level key to
	 * hang it off - so nothing is invented and the "-" to "___" rule is inherited from
	 * wherever the key was built.
	 *
	 * IT REFUSES TO GUESS. If the item does not describe this exact parent, or the level above
	 * it is missing too, it fails and the caller reports it - better than a category tree
	 * quietly growing a branch nobody asked for.
	 *
	 * THE NAME IS MDM'S, NOT ANOTHER LOCALE'S. secondLevelName is the WMI code the screen
	 * read from the carline master, so the row created here carries the same value the
	 * screen would have used had the level been queued explicitly.
	 */
	private static boolean buildParentFromItem(KaptureApiClient api, IMCategoryDetails category,
			String parent, String refKey, String locale, Connection cmsConn) {
		SIVinDetails item = (null == category) ? null : category.getItemDetails();
		if (null == item) {
			logger.info("buildParentFromItem :: {" + parent + "} needed by {" + refKey
					+ "} exists in no locale and there are no item details to build it from.");
			return false;
		}
		String itemParentKey = Utilities.replaceRefKeys(
				safeTrim(item.getSeconddLevelRefKey()).toUpperCase());
		if (!parent.equalsIgnoreCase(itemParentKey)) {
			logger.info("buildParentFromItem :: {" + parent + "} is not the WMI level of {"
					+ refKey + "} (that is {" + itemParentKey + "}) - not building it.");
			return false;
		}
		String grandParent = Utilities.replaceRefKeys(
				safeTrim(item.getFirstLevelRefKey()).toUpperCase());
		if ("".equals(grandParent) || !KaptureCategoryDAO.categoryExists(grandParent, locale,
				cmsConn)) {
			logger.info("buildParentFromItem :: the model level {" + grandParent + "} is not in {"
					+ locale + "} either - {" + parent + "} NOT created.");
			return false;
		}
		String name = safeTrim(item.getSecondLevelName());
		if ("".equals(name)) {
			name = safeTrim(item.getWmiCode());
		}
		if ("".equals(name)) {
			name = parent;
		}
		logger.info("buildParentFromItem :: creating the WMI level {" + parent + "} named {"
				+ name + "} under {" + grandParent + "} for {" + locale + "}.");
		KaptureApiResult result = api.createCategory(locale, parent, name, name, grandParent);
		/*
		 * THE RESPONSE IS LOGGED WHATEVER IT SAYS. A create that answers 2xx - or "already
		 * exists" - and writes nothing is indistinguishable from success unless the body is
		 * recorded, and that is exactly what CX7_ER___ did: no error anywhere, no row in any
		 * of the 42 locales.
		 */
		logger.info("buildParentFromItem :: create {" + parent + "} answered http="
				+ result.httpStatus + " :: " + (null == result.body ? "" : result.body));
		if (!result.isOk() && !KaptureApiClient.isAlreadyExists(result)) {
			logger.info("buildParentFromItem :: create FAILED for {" + parent + "} http="
					+ result.httpStatus + " :: " + result.message);
			return false;
		}
		/* RE-READ RATHER THAN TRUST THE 2xx - the child is written next and needs it there. */
		boolean written = waitForCategory(parent, locale, cmsConn);
		if (!written) {
			logger.info("buildParentFromItem :: {" + parent + "} was ACCEPTED by the API but is"
					+ " still not in k_categories for {" + locale + "} - the server reported no"
					+ " error and created no row.");
		}
		return written;
	}

	private static boolean ensureCategoryForLocale(KaptureApiClient api, String referenceKey,
			String locale, String sourceLocale, Connection cmsConn) {
		try {
			if (KaptureCategoryDAO.categoryExists(referenceKey, locale, cmsConn)) {
				return true;
			}

			/*
			 * THE KEY EXISTS SOMEWHERE ELSE BUT NOT HERE - the common case. Copy the shape from the
			 * source locale and write the missing levels for this one.
			 */
			ArrayList<IMCategoryDetails> chain = KaptureCategoryDAO.getHierarchy(referenceKey,
					sourceLocale, cmsConn);
			if (null == chain || chain.size() <= 0) {
				logger.info("ensureCategoryForLocale :: {" + referenceKey + "} is not present for {"
						+ locale + "} and has no chain to copy from {" + sourceLocale + "}.");
				return false;
			}

			int created = 0;
			// ROOT FIRST - a child is never created before the parent it hangs off
			for (IMCategoryDetails level : chain) {
				if (null == level || "".equals(safeTrim(level.getCategoryRefKey()))) {
					continue;
				}
				String levelKey = safeTrim(level.getCategoryRefKey());
				if (KaptureCategoryDAO.categoryExists(levelKey, locale, cmsConn)) {
					continue;
				}
				KaptureApiResult result = api.createCategory(locale, levelKey, effectiveName(level),
						effectiveName(level), safeTrim(level.getParentRefKey()));
				if (!result.isOk() && !KaptureApiClient.isAlreadyExists(result)) {
					logger.info("ensureCategoryForLocale :: create FAILED for {" + levelKey + "} in {"
							+ locale + "} http=" + result.httpStatus + " :: " + result.message);
					return false;
				}
				created++;
			}
			logger.info("ensureCategoryForLocale :: created " + created + " of " + chain.size()
					+ " level(s) for {" + referenceKey + "} in {" + locale + "} from {" + sourceLocale
					+ "}.");
			/*
			 * RE-READ RATHER THAN TRUST THE 2xx. The API writes the row, so if the SELECT still does
			 * not see it, something is wrong and the screen must not go on to map it.
			 */
			return waitForCategory(referenceKey, locale, cmsConn);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryServiceImpl.class.getName(),
					"ensureCategoryForLocale()", e);
			return false;
		}
	}

	/**
	 * ADD-OR-UPDATE ONE CATEGORY - the entry point for the BATCH processors.
	 *
	 * The batch owns the client and the connection and passes them in, rather than this method
	 * creating its own: a schedule can carry thousands of categories, and building a client per
	 * category would mean a fresh LOGIN per category - slow, and straight into the API's rate
	 * limit. One client, reused, holds a single token for the whole run.
	 *
	 * @param api      a logged-in (or lazily logging-in) client, reused across the run
	 * @param category refKey / name / parentRefKey of the category to write
	 * @param locale   locale to write it for
	 * @return true when the category is present afterwards
	 */
	public static boolean createOrUpdateCategory(KaptureApiClient api, IMCategoryDetails category,
			String locale, Connection cmsConn) {
		if (null == api || null == category || null == cmsConn) {
			return false;
		}
		String refKey = safeTrim(category.getCategoryRefKey());
		if ("".equals(refKey)) {
			return false;
		}
		String rowLocale = safeTrim(locale);
		if ("".equals(rowLocale)) {
			rowLocale = safeTrim(category.getLocale());
		}
		return writeCategory(api, category, refKey, effectiveName(category),
				safeTrim(category.getParentRefKey()), rowLocale, cmsConn);
	}

	/**
	 * ADD-OR-UPDATE ONE CATEGORY.
	 *
	 * The endpoint is chosen from what is already in the table: add-categoryMgt when the row is
	 * absent for the locale, updateCategoryByCatRef when it is there. A create that races with
	 * another user and comes back "already exists" is SUCCESS, not a failure - the row is present,
	 * which is all the caller wanted.
	 */
	/**
	 * THE CATEGORY IS PASSED IN SO A FAILURE CAN BE RECORDED ON IT.
	 *
	 * The server's answer used to be logged here and then dropped, so the category report
	 * could only say "Failed to create or update the category in Kapture" - true, and of no
	 * use to whoever has to fix it. The batches now print what Kapture actually replied.
	 *
	 * THE MESSAGE IS CLEARED ON SUCCESS as well, so a category that fails and is then
	 * written on a later attempt cannot carry a stale reason into the report.
	 */
	private static boolean writeCategory(KaptureApiClient api, IMCategoryDetails category,
			String refKey, String name, String parentRefKey, String locale, Connection cmsConn) {
		if (!ensureParent(api, category, refKey, parentRefKey, locale, cmsConn)) {
			String reason = "parent category {" + parentRefKey + "} does not exist for {"
					+ locale + "} and could not be created";
			logger.info("writeCategory :: FAILED for {" + refKey + "} - " + reason + ".");
			if (null != category) {
				category.setErrorMessage(reason);
			}
			return false;
		}
		KaptureApiResult result = null;
		if (KaptureCategoryDAO.categoryExists(refKey, locale, cmsConn)) {
			result = api.updateCategory(locale, refKey, name, name);
		} else {
			result = api.createCategory(locale, refKey, name, name, parentRefKey);
		}
		if (result.isOk() || KaptureApiClient.isAlreadyExists(result)) {
			if (null != category) {
				category.setErrorMessage(null);
			}
			return true;
		}
		String reason = "http " + result.httpStatus + " :: "
				+ (null == result.message ? "" : result.message);
		logger.info("writeCategory :: FAILED for {" + refKey + "} in {" + locale + "} "
				+ reason);
		if (null != category) {
			category.setErrorMessage(reason);
		}
		return false;
	}

	/**
	 * A BLANK NAME FALLS BACK TO THE REFERENCE KEY - same rule as Jobs 4 and 8, so a category is
	 * never sent with an empty name and rejected for one. MNAO's VIN ranges legitimately carry a
	 * name equal to their reference key anyway.
	 */
	private static String effectiveName(IMCategoryDetails category) {
		if (null == category) {
			return "";
		}
		String name = safeTrim(category.getCategoryName());
		return "".equals(name) ? safeTrim(category.getCategoryRefKey()) : name;
	}

	private static String safeTrim(String value) {
		return null == value ? "" : value.trim();
	}

	private static void closeQuietly(Connection conn) {
		if (null != conn) {
			try {
				conn.close();
			} catch (Exception ignore) {
				// nothing useful to do here
			}
		}
	}
}
