package com.mazda.gms3.mdm.utils;

import java.util.List;

/**
 * Shared helpers for the import ACTION / marker column.
 *
 * Every import screen reads the same column with the same accepted values, so the rules live
 * here once instead of being repeated in each servlet - one place to change if the business
 * ever adds a value.
 *
 * Accepted values (case-insensitive, blank allowed):
 *   A / ADD    - the row is created
 *   U / UPDATE - the row is updated
 *   D / DELETE - the row is soft deleted
 *
 * A and U both fall through to the screen's EXISTING create-or-update logic, which already
 * decides insert vs update from that screen's unique combination. Only D changes behaviour.
 */
public class ImportActionUtils {

	private ImportActionUtils() {
		// helper class - not meant to be instantiated
	}

	/**
	 * @return true when the value marks the row for deletion (D / DELETE).
	 */
	public static boolean isDeleteAction(String action) {
		if (null == action || "".equals(action.trim())) {
			return false;
		}
		String a = action.trim().toLowerCase();
		return a.equals("d") || a.equals("delete");
	}

	/**
	 * @return true when the value is one the business defined, or blank. Anything else is a
	 *         hard error raised during Excel validation, BEFORE any database work, so a
	 *         mistyped marker can never be silently treated as create-or-update.
	 */
	public static boolean isValidAction(String action) {
		if (null == action || "".equals(action.trim())) {
			return true;
		}
		String a = action.trim().toLowerCase();
		return a.equals("a") || a.equals("add") || a.equals("u") || a.equals("update")
				|| a.equals("d") || a.equals("delete");
	}

	/**
	 * Null-safe trim, used when matching an imported row against the database.
	 *
	 * The create-or-update path normalises a missing optional value to "" before it reaches
	 * the DAO. ACTION=D rows do not go through that normalisation, so a blank optional cell
	 * would otherwise arrive as null and blow up on getXxx().trim(). Treating null as ""
	 * keeps the delete lookup matching exactly what create-or-update stores.
	 */
	public static String safeTrim(String value) {
		return (null == value) ? "" : value.trim();
	}

	/**
	 * Null-safe trim + lower case, for the TRIM(LOWER(...)) comparisons the screens use.
	 */
	public static String safeLower(String value) {
		return (null == value) ? "" : value.trim().toLowerCase();
	}

	/**
	 * Turn the primary keys resolved for the ACTION=D rows into the comma separated list the
	 * screens' existing delete methods already take, so the import delete reuses that code
	 * instead of repeating each screen's delete rules (sync status, related tables, ...).
	 *
	 * @return the joined ids, or an empty string when there is nothing to delete.
	 */
	public static String buildDeleteIds(List<Long> ids) {
		if (null == ids || ids.size() <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			Long id = (Long) ids.get(i);
			if (null == id || id.longValue() <= 0) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(id.longValue());
		}
		return sb.toString();
	}
}
