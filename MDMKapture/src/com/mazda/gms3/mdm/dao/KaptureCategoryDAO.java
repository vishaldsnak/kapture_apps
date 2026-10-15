package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;

/**
 * READS THE KAPTURE CATEGORY MASTER (kapture_cms_db.k_categories).
 *
 * REPLACES THE InQuira CATEGORY LOOKUPS used by the SI VIN screens:
 *   IQ getCategoryRequest().getCategoryByReferenceKey     -> getCategory()
 *   IQ getCategoryRequest().getCategoryKeyByReferenceKey  -> categoryExists()
 *
 * READ ONLY, AND NOT BY ACCIDENT. kapture_cms_db is granted SELECT to this application and
 * nothing more, so categories cannot be created or updated here. Those go through the REST API -
 * see KaptureCategoryServiceImpl, which uses this class for its lookups and the API for its
 * writes. Do not add an INSERT or UPDATE to this class; it would fail at run time on every
 * environment.
 */
public class KaptureCategoryDAO extends DBConnectionHelper {

	static Logger logger = LogManager.getLogger(KaptureCategoryDAO.class);

	private static final String T_CATEGORIES = "kapture_cms_db.k_categories";

	/** Guard against a malformed parent chain looping forever. */
	private static final int MAX_HIERARCHY_DEPTH = 20;

	/**
	 * IQ EQUIVALENT: getCategoryKeyByReferenceKey.
	 *
	 * A reference key exists once PER LOCALE, so "does it exist" is only meaningful with the
	 * locale. A key that exists for another locale but not this one is exactly the case that
	 * requires a new row - see KaptureCategoryServiceImpl.ensureCategoriesForLocale().
	 */
	public static boolean categoryExists(String referenceKey, String locale, Connection conn) {
		return null != getCategory(referenceKey, locale, conn);
	}

	/**
	 * IQ EQUIVALENT: getCategoryByReferenceKey.
	 *
	 * @return the row for (referenceKey, locale), or null when there is none
	 */
	public static IMCategoryDetails getCategory(String referenceKey, String locale, Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		IMCategoryDetails details = null;
		try {
			if (null == referenceKey || "".equals(referenceKey.trim()) || null == locale
					|| "".equals(locale.trim())) {
				return null;
			}
			String sql = "SELECT referencekey, categoryname, categorydescription, parentrefkey,"
					+ " locale FROM " + T_CATEGORIES
					+ " WHERE referencekey = ? AND locale = ? LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, referenceKey.trim());
			pstmt.setString(2, locale.trim());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				details = new IMCategoryDetails();
				details.setCategoryRefKey(safeTrim(rs.getString("referencekey")));
				details.setCategoryName(safeTrim(rs.getString("categoryname")));
				details.setParentRefKey(safeTrim(rs.getString("parentrefkey")));
				details.setLocale(safeTrim(rs.getString("locale")));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryDAO.class.getName(), "getCategory()", e);
			details = null;
		} finally {
			closeQuietly(rs, pstmt);
		}
		return details;
	}

	/**
	 * Reads a category's full parent chain, root-first, from ANY locale that has it.
	 *
	 * Used as the template when the same chain has to be created for another locale: the tree
	 * shape is identical across locales, only the row per locale is missing.
	 *
	 * The depth is NOT assumed - MNAO hangs VIN ranges off the VIN root (4 levels) while MC and
	 * MME hang them off CARLINE (6 levels), so this follows parentrefkey until it runs out.
	 *
	 * @return root-first chain; empty when the key is not present in the source locale
	 */
	public static ArrayList<IMCategoryDetails> getHierarchy(String referenceKey, String locale,
			Connection conn) {
		ArrayList<IMCategoryDetails> chain = new ArrayList<IMCategoryDetails>();
		try {
			String currentKey = safeTrim(referenceKey);
			int depth = 0;
			while (!"".equals(currentKey) && depth < MAX_HIERARCHY_DEPTH) {
				IMCategoryDetails level = getCategory(currentKey, locale, conn);
				if (null == level) {
					logger.info("getHierarchy :: {" + currentKey + "} not present for locale {"
							+ locale + "} - chain stops here.");
					break;
				}
				chain.add(0, level);
				currentKey = safeTrim(level.getParentRefKey());
				depth++;
			}
			if (depth >= MAX_HIERARCHY_DEPTH) {
				logger.info("getHierarchy :: depth guard hit for {" + referenceKey
						+ "} - parent chain may be circular.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureCategoryDAO.class.getName(), "getHierarchy()", e);
		}
		return chain;
	}

	private static String safeTrim(String value) {
		return null == value ? "" : value.trim();
	}

	private static void closeQuietly(ResultSet rs, PreparedStatement pstmt) {
		try {
			if (null != rs)
				rs.close();
		} catch (Exception ignore) {
			// nothing useful to do here
		}
		try {
			if (null != pstmt)
				pstmt.close();
		} catch (Exception ignore) {
			// nothing useful to do here
		}
	}
}
