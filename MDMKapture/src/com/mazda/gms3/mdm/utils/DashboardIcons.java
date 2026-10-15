package com.mazda.gms3.mdm.utils;

import java.util.HashMap;
import java.util.Map;

import com.mazda.gms3.mdm.vo.AccessManagementInterface;

/**
 * THE INLINE SVG ICONS DRAWN ON MY PAGE - ONE PER TILE AND ONE PER CATEGORY TAB.
 *
 * BUSINESS SWITCHED FROM mdm_mockups/dashboard-noicons.html TO mdm_mockups/dashboard.html, AND
 * THE ONLY REAL DIFFERENCE BETWEEN THE TWO IS THAT EVERY TILE AND TAB CARRIES AN ICON. THE PATH
 * DATA BELOW IS LIFTED VERBATIM FROM THAT MOCKUP SO THE SCREEN MATCHES WHAT WAS SIGNED OFF.
 *
 * WHY THE MARKUP IS HELD IN JAVA RATHER THAN AN IMAGE OR AN ICON FONT:
 *   - NO NEW BINARY ASSETS TO SHIP OR CACHE BUST, AND NOTHING EXTRA TO REQUEST OVER HTTP.
 *   - stroke="currentColor" MEANS THE CSS DECIDES THE COLOUR, SO HOVER AND ACTIVE STATES ARE
 *     PURE CSS - SEE .mdmTileIcon / .mdmTabIcon IN css/mdm-dashboard.css.
 *   - THE MAPPING IS A PRESENTATION DECISION KEYED ON THE SAME TILE KEYS DashboardCategories
 *     ALREADY USES, SO BOTH TABLES SIT SIDE BY SIDE AND ARE EDITED TOGETHER.
 *
 * AN UNMAPPED KEY GETS DOC RATHER THAN AN EMPTY STRING - A NEW MODULE ROW STILL DRAWS A
 * COMPLETE TILE INSTEAD OF ONE WITH A HOLE WHERE THE ICON SHOULD BE. THAT MIRRORS THE "OTHER"
 * CATEGORY FALLBACK IN DashboardCategories.
 */
public class DashboardIcons {

	// ------------------------------------------------------------------ THE GLYPHS
	public static final String CAR = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M3 13l2-5h14l2 5M5 13h14v5H5zM7 18v2M17 18v2\"/><circle cx=\"8\" cy=\"15.5\" r=\"1\"/><circle cx=\"16\" cy=\"15.5\" r=\"1\"/></svg>";

	public static final String RANGE = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M3 12h18M12 3v18\"/><circle cx=\"12\" cy=\"12\" r=\"9\"/></svg>";

	public static final String TOOL = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M14 7a3 3 0 00-4 4l-6 6 2 2 6-6a3 3 0 004-4l-2 2-2-2 2-2z\"/></svg>";

	public static final String CAL = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><rect x=\"3\" y=\"4\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M3 9h18M8 4v5M16 4v5\"/></svg>";

	public static final String CD = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"12\" cy=\"12\" r=\"9\"/><circle cx=\"12\" cy=\"12\" r=\"2.5\"/><path d=\"M12 12l4-4\"/></svg>";

	public static final String USERS = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"12\" cy=\"8\" r=\"3.5\"/><path d=\"M5 20c0-3.5 3-6 7-6s7 2.5 7 6\"/></svg>";

	public static final String GLOBE = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M3 12h18M12 3c2.5 2.5 2.5 15 0 18M12 3c-2.5 2.5-2.5 15 0 18\"/></svg>";

	public static final String DOC = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M4 5h16v4H4zM4 11h16v4H4zM4 17h10v2H4z\"/></svg>";

	public static final String VIN = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><rect x=\"3\" y=\"8\" width=\"18\" height=\"8\" rx=\"1.5\"/><path d=\"M6 12h.01M9 12h.01M12 12h.01M15 12h6\"/></svg>";

	public static final String CAT = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M3 7l9-4 9 4-9 4-9-4zM3 7v10l9 4 9-4V7\"/></svg>";

	public static final String ORDER = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M4 6h16M4 12h16M4 18h10\"/></svg>";

	public static final String ENGINE = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"12\" cy=\"12\" r=\"4\"/><path d=\"M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2\"/></svg>";

	public static final String GEARBOX = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"7\" cy=\"12\" r=\"3\"/><circle cx=\"17\" cy=\"12\" r=\"3\"/><path d=\"M10 12h4\"/></svg>";

	public static final String TRANS = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M5 8h14M5 12h14M5 16h9\"/><circle cx=\"18\" cy=\"16\" r=\"3\"/></svg>";

	public static final String TRUCK = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M5 17h14M6 17l1-6h10l1 6M8 11V8h8v3\"/><circle cx=\"8\" cy=\"19\" r=\"1.5\"/><circle cx=\"16\" cy=\"19\" r=\"1.5\"/></svg>";

	public static final String CHECK = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M9 11l3 3L22 4M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11\"/></svg>";

	public static final String MAP = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><rect x=\"3\" y=\"4\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M3 9h18M8 13h3M8 16h6\"/></svg>";

	public static final String IMP = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M12 3v12M8 11l4 4 4-4M4 17v2a2 2 0 002 2h12a2 2 0 002-2v-2\"/></svg>";

	public static final String EXP = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M12 21V9M8 13l4-4 4 4M4 7V5a2 2 0 012-2h12a2 2 0 012 2v2\"/></svg>";

	public static final String RMI = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><path d=\"M4 4h16v12H4zM8 20h8M12 16v4\"/></svg>";

	public static final String GEAR = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.7\"><circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"M19 12a7 7 0 00-.1-1l2-1.5-2-3.5-2.4 1a7 7 0 00-1.7-1L14.5 3h-5l-.3 2.5a7 7 0 00-1.7 1l-2.4-1-2 3.5L3.1 11a7 7 0 000 2l-2 1.5 2 3.5 2.4-1a7 7 0 001.7 1l.3 2.5h5l.3-2.5a7 7 0 001.7-1l2.4 1 2-3.5-2-1.5a7 7 0 00.1-1z\"/></svg>";

	/** USED WHEN A TILE KEY IS NOT IN THE TABLE BELOW. */
	private static final String DEFAULT_ICON = DOC;

	private static final Map<String, String> TILE_ICONS = buildTileIcons();

	private static final Map<String, String> CATEGORY_ICONS = buildCategoryIcons();

	private static Map<String, String> buildTileIcons() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(AccessManagementInterface.REF_KEY_COUNTRY_LOCALE, GLOBE);
		map.put(AccessManagementInterface.REF_KEY_MANUAL_LANGUAGE, TRANS);
		map.put(AccessManagementInterface.REF_KEY_MANUAL_TYPE, DOC);
		map.put(AccessManagementInterface.REF_KEY_CARLINE, CAR);
		map.put(AccessManagementInterface.REF_KEY_VIN, VIN);
		map.put(AccessManagementInterface.REF_KEY_ESI_CATEGORY, CAT);
		map.put(AccessManagementInterface.REF_KEY_CVC_CATEGORY, CAT);
		map.put(AccessManagementInterface.REF_KEY_DISPLAY_ORDER, ORDER);
		map.put(AccessManagementInterface.REF_KEY_VIN_CROSS_REFERENCE, CAL);
		map.put(DashboardCategories.TILE_ENGINE, ENGINE);
		map.put(DashboardCategories.TILE_TRANSMISSION, GEARBOX);
		map.put(DashboardCategories.TILE_VEHICLE_TYPE, TRUCK);
		map.put(AccessManagementInterface.REF_KEY_MNAO_SIVIN_RANGE, RANGE);
		map.put(AccessManagementInterface.REF_KEY_MC_SIVIN_RANGE, RANGE);
		map.put(AccessManagementInterface.REF_KEY_MME_SIVIN_RANGE, RANGE);
		map.put(AccessManagementInterface.REF_KEY_SIVIN_RANGE, RANGE);
		map.put(AccessManagementInterface.REF_KEY_SI_INNERLINKS_IDENTIFICATION_HISTORY, CHECK);
		map.put(AccessManagementInterface.REF_KEY_SCH_MAIN_VIN_MAPPING, MAP);
		map.put(DashboardCategories.TILE_SST_MAINTENANCE, TOOL);
		map.put(DashboardCategories.TILE_SST_VEHICLE_DATA, TRUCK);
		map.put(DashboardCategories.TILE_RUM_VIN, CAL);
		map.put(AccessManagementInterface.REF_KEY_ESI_LABELS_UPDATE_SCHEDULE, CAL);
		map.put(AccessManagementInterface.REF_KEY_SI_TRANSLATION_UPDATE, TRANS);
		map.put(DashboardCategories.TILE_CD_CREATION, CD);
		map.put(AccessManagementInterface.REF_KEY_RMI_TOOL, RMI);
		map.put(AccessManagementInterface.REF_KEY_SI_CHANNEL_DATA_LOAD, IMP);
		map.put(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL, EXP);
		map.put(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL, EXP);
		map.put(AccessManagementInterface.REF_KEY_MASTERDATA_LOCALE_MAPPING, GLOBE);
		map.put(AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT, USERS);
		return map;
	}

	private static Map<String, String> buildCategoryIcons() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(DashboardCategories.CAT_MASTER_DATA, CAR);
		map.put(DashboardCategories.CAT_SI_VIN_RANGE, RANGE);
		map.put(DashboardCategories.CAT_SST, TOOL);
		map.put(DashboardCategories.CAT_RUM_VIN_TRANSLATION, CAL);
		map.put(DashboardCategories.CAT_TOOLS, CD);
		map.put(DashboardCategories.CAT_ADMIN, USERS);
		map.put(DashboardCategories.CAT_OTHER, GEAR);
		return map;
	}

	/** THE ICON FOR ONE TILE. NEVER NULL AND NEVER EMPTY - SEE DEFAULT_ICON. */
	public static String forTile(String tileKey) {
		if (null == tileKey || "".equals(tileKey.trim())) {
			return DEFAULT_ICON;
		}
		String icon = TILE_ICONS.get(tileKey.trim());
		return (null == icon) ? DEFAULT_ICON : icon;
	}

	/** THE ICON FOR ONE CATEGORY TAB. NEVER NULL AND NEVER EMPTY. */
	public static String forCategory(String categoryKey) {
		if (null == categoryKey || "".equals(categoryKey.trim())) {
			return GEAR;
		}
		String icon = CATEGORY_ICONS.get(categoryKey.trim());
		return (null == icon) ? GEAR : icon;
	}
}
