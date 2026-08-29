package com.mazda.gms3.mdm.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.DashboardCategoryDetails;
import com.mazda.gms3.mdm.vo.DashboardTileDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;

/**
 * GROUPS THE MY PAGE TILES INTO THE CATEGORY TABS SHOWN ON THE DASHBOARD.
 *
 * WHY THIS LIVES IN CODE AND NOT IN THE DATABASE - gms3_mdm_modules HAS NO CATEGORY COLUMN, AND
 * ADDING ONE WOULD MEAN A DDL CHANGE PLUS A SEED SCRIPT ON EVERY ENVIRONMENT BEFORE MY PAGE COULD
 * RENDER AT ALL. THE CATEGORY OF A SCREEN IS ALSO A PRESENTATION DECISION, NOT MASTER DATA. SO
 * THE MAPPING IS HELD HERE AND THE DISPLAY NAMES COME FROM mdmresource_en.properties LIKE EVERY
 * OTHER LABEL IN THE APPLICATION.
 *
 * IMPORTANT - THIS CLASS ONLY REARRANGES THE TILES THE USER IS ALREADY ENTITLED TO SEE. IT NEVER
 * ADDS A TILE. THE ACCESS RULES STAY EXACTLY WHERE THEY WERE, IN
 * com.mazda.gms3.mdm.filter.AccessManagementFilter, WHICH BUILDS displayTilesList.
 *
 * ANY TILE WHOSE KEY IS NOT LISTED BELOW FALLS INTO THE "OTHER" CATEGORY RATHER THAN
 * DISAPPEARING - SO A NEW MODULE ADDED TO gms3_mdm_modules IS STILL REACHABLE FROM MY PAGE
 * BEFORE ANYONE REMEMBERS TO CATEGORISE IT.
 */
public class DashboardCategories {

	// ---------------------------------------------------------------- CATEGORY KEYS
	public static final String CAT_MASTER_DATA = "MASTER_DATA";
	public static final String CAT_SI_VIN_RANGE = "SI_VIN_RANGE";
	public static final String CAT_SST = "SST";
	public static final String CAT_RUM_VIN_TRANSLATION = "RUM_VIN_TRANSLATION";
	public static final String CAT_TOOLS = "TOOLS";
	public static final String CAT_ADMIN = "ADMIN";
	public static final String CAT_OTHER = "OTHER";

	/*
	 * SYNTHETIC TILE KEYS FOR THE GROUPED MODULE TYPES.
	 *
	 * ENGINE / TRANSMISSION / VEHICLE TYPE / SST / CD CREATION / RUM VIN ARE MANY MODULE ROWS BUT
	 * ONLY ONE TILE EACH - AccessManagementFilter KEEPS THE FIRST MODULE OF THE TYPE AND DROPS THE
	 * REST. THOSE TILES THEREFORE HAVE NO MEANINGFUL REFKEY TO CATEGORISE ON, SO THEY GET A KEY OF
	 * THEIR OWN.
	 */
	public static final String TILE_ENGINE = "ENGINE";
	public static final String TILE_TRANSMISSION = "TRANSMISSION";
	public static final String TILE_VEHICLE_TYPE = "VEHICLE_TYPE";
	public static final String TILE_SST_MAINTENANCE = "SST_MAINTENANCE";
	public static final String TILE_SST_VEHICLE_DATA = "SST_VEHICLE_DATA";
	public static final String TILE_CD_CREATION = "CD_CREATION";
	public static final String TILE_RUM_VIN = "RUM_VIN";

	/*
	 * ACCESS MANAGEMENT IS NOT IN THE TILE LIST - AccessManagementFilter DELIBERATELY EXCLUDES IT,
	 * AND IT USED TO BE REACHED BY A "SETTINGS" LINK SITTING OUTSIDE THE TILES ALTOGETHER. IT IS
	 * NOW A SYNTHETIC TILE IN THE ADMINISTRATION CATEGORY, ADDED ONLY FOR A SUPER ADMIN - EXACTLY
	 * THE CONDITION THE OLD LINK WAS WRAPPED IN.
	 *
	 * THE TILE IS LABELLED ACCESS MANAGEMENT, NOT SETTINGS, BECAUSE THAT IS THE SCREEN IT OPENS -
	 * ROLE BASED ACCESS TO THE MDM SCREENS. ONLY THE navType STAYS "SETTINGS", SINCE THAT IS THE
	 * VALUE THE Dashboard SERVLET ALREADY BRANCHES ON.
	 */

	// ---------------------------------------------------------------- TAB ORDER
	private static final String[] CATEGORY_ORDER = new String[] { CAT_MASTER_DATA, CAT_SI_VIN_RANGE,
			CAT_SST, CAT_RUM_VIN_TRANSLATION, CAT_TOOLS, CAT_ADMIN, CAT_OTHER };

	// ---------------------------------------------------------------- TILE KEY -> CATEGORY
	private static final Map<String, String> TILE_CATEGORY_MAP = buildTileCategoryMap();

	private static Map<String, String> buildTileCategoryMap() {
		Map<String, String> map = new HashMap<String, String>();

		// MASTER DATA
		map.put(AccessManagementInterface.REF_KEY_COUNTRY_LOCALE, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_MANUAL_LANGUAGE, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_MANUAL_TYPE, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_CARLINE, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_VIN, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_ESI_CATEGORY, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_CVC_CATEGORY, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_DISPLAY_ORDER, CAT_MASTER_DATA);
		map.put(AccessManagementInterface.REF_KEY_VIN_CROSS_REFERENCE, CAT_MASTER_DATA);
		map.put(TILE_ENGINE, CAT_MASTER_DATA);
		map.put(TILE_TRANSMISSION, CAT_MASTER_DATA);
		map.put(TILE_VEHICLE_TYPE, CAT_MASTER_DATA);

		// SI VIN RANGES
		map.put(AccessManagementInterface.REF_KEY_MNAO_SIVIN_RANGE, CAT_SI_VIN_RANGE);
		map.put(AccessManagementInterface.REF_KEY_MC_SIVIN_RANGE, CAT_SI_VIN_RANGE);
		map.put(AccessManagementInterface.REF_KEY_MME_SIVIN_RANGE, CAT_SI_VIN_RANGE);
		map.put(AccessManagementInterface.REF_KEY_SIVIN_RANGE, CAT_SI_VIN_RANGE);
		map.put(AccessManagementInterface.REF_KEY_SI_INNERLINKS_IDENTIFICATION_HISTORY, CAT_SI_VIN_RANGE);
		map.put(AccessManagementInterface.REF_KEY_SCH_MAIN_VIN_MAPPING, CAT_SI_VIN_RANGE);

		// SST MANAGEMENT
		map.put(TILE_SST_MAINTENANCE, CAT_SST);
		map.put(TILE_SST_VEHICLE_DATA, CAT_SST);

		// RUM VIN AND TRANSLATIONS
		map.put(TILE_RUM_VIN, CAT_RUM_VIN_TRANSLATION);
		map.put(AccessManagementInterface.REF_KEY_ESI_LABELS_UPDATE_SCHEDULE, CAT_RUM_VIN_TRANSLATION);
		map.put(AccessManagementInterface.REF_KEY_SI_TRANSLATION_UPDATE, CAT_RUM_VIN_TRANSLATION);

		// TOOLS, IMPORT AND EXPORT
		map.put(TILE_CD_CREATION, CAT_TOOLS);
		map.put(AccessManagementInterface.REF_KEY_RMI_TOOL, CAT_TOOLS);
		map.put(AccessManagementInterface.REF_KEY_SI_CHANNEL_DATA_LOAD, CAT_TOOLS);
		map.put(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL, CAT_TOOLS);
		map.put(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL, CAT_TOOLS);

		// ADMINISTRATION
		map.put(AccessManagementInterface.REF_KEY_MASTERDATA_LOCALE_MAPPING, CAT_ADMIN);
		map.put(AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT, CAT_ADMIN);

		return map;
	}

	/**
	 * TURNS THE FLAT displayTilesList PREPARED BY AccessManagementFilter INTO THE ORDERED LIST OF
	 * CATEGORY TABS RENDERED BY dashboard.jsp.
	 *
	 * THE ORDER OF THE TILES INSIDE A CATEGORY IS THE ORDER THEY ARRIVE IN, WHICH IS ALREADY THE
	 * DISPLAY ORDER SORT DONE BY THE FILTER - SO THE EXISTING ORDERING IS PRESERVED.
	 *
	 * @param displayTilesList THE TILES THE LOGGED IN USER MAY SEE. NULL OR EMPTY IS SAFE.
	 * @param msgProps         RESOLVES THE TITLES AND DESCRIPTIONS FOR THE CURRENT LOCALE.
	 * @param superAdminUser   WHEN TRUE, THE SETTINGS TILE IS ADDED TO ADMINISTRATION. THIS IS THE
	 *                         SAME CONDITION THAT USED TO GUARD THE SETTINGS LINK.
	 * @return NEVER NULL. EMPTY WHEN THE USER HAS NO TILES.
	 */
	public static ArrayList<DashboardCategoryDetails> groupTiles(ArrayList<ModuleDetails> displayTilesList,
			MessageProperties msgProps, boolean superAdminUser) {
		ArrayList<DashboardCategoryDetails> categoriesList = new ArrayList<DashboardCategoryDetails>();
		try {
			// KEEPS INSERTION ORDER SO A CATEGORY IS BUILT ONCE AND FILLED AS TILES ARRIVE
			LinkedHashMap<String, DashboardCategoryDetails> byKey = new LinkedHashMap<String, DashboardCategoryDetails>();

			if (null != displayTilesList && displayTilesList.size() > 0) {
				for (ModuleDetails modDetails : displayTilesList) {
					addTile(byKey, buildTile(modDetails, msgProps), msgProps);
				}
			}

			/*
			 * ACCESS MANAGEMENT GOES LAST SO IT SITS AT THE END OF THE ADMINISTRATION TAB. IT IS
			 * ADDED EVEN WHEN THE USER HAS NO OTHER ADMINISTRATION TILE - THE TAB IS CREATED FOR IT.
			 */
			if (superAdminUser) {
				String accessKey = AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT;
				DashboardTileDetails accessTile = new DashboardTileDetails();
				accessTile.setNavType("SETTINGS");
				accessTile.setTileKey(accessKey);
				accessTile.setTitle(resolve(msgProps, "label.dash." + accessKey, accessKey));
				accessTile.setDescription(resolve(msgProps, "label.dashtile." + accessKey + ".desc", ""));
				accessTile.setIcon(DashboardIcons.forTile(accessKey));
				addTile(byKey, accessTile, msgProps);
			}

			// EMIT IN THE FIXED TAB ORDER, SKIPPING CATEGORIES THIS USER HAS NO TILES FOR
			for (int i = 0; i < CATEGORY_ORDER.length; i++) {
				DashboardCategoryDetails category = byKey.remove(CATEGORY_ORDER[i]);
				if (null != category && category.getTileCount() > 0) {
					categoriesList.add(category);
				}
			}
			// ANYTHING NOT NAMED IN CATEGORY_ORDER STILL GETS A TAB, AFTER THE KNOWN ONES
			Iterator<DashboardCategoryDetails> leftOver = byKey.values().iterator();
			while (leftOver.hasNext()) {
				DashboardCategoryDetails category = leftOver.next();
				if (category.getTileCount() > 0) {
					categoriesList.add(category);
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(DashboardCategories.class.getName(), "groupTiles()", e);
		}
		return categoriesList;
	}

	/**
	 * FILES ONE TILE UNDER ITS CATEGORY, CREATING THE CATEGORY THE FIRST TIME IT IS NEEDED.
	 * A NULL TILE IS IGNORED, AND A TILE WHOSE KEY IS NOT IN THE MAP FALLS INTO "OTHER".
	 */
	private static void addTile(LinkedHashMap<String, DashboardCategoryDetails> byKey,
			DashboardTileDetails tile, MessageProperties msgProps) {
		if (null == tile) {
			return;
		}

		String categoryKey = TILE_CATEGORY_MAP.get(tile.getTileKey());
		if (null == categoryKey || "".equals(categoryKey)) {
			// NOT CATEGORISED YET - SHOW IT RATHER THAN LOSE IT
			categoryKey = CAT_OTHER;
		}

		DashboardCategoryDetails category = byKey.get(categoryKey);
		if (null == category) {
			category = new DashboardCategoryDetails();
			category.setCategoryKey(categoryKey);
			category.setTitle(resolve(msgProps, "label.dashcat." + categoryKey, categoryKey));
			category.setDescription(resolve(msgProps, "label.dashcat." + categoryKey + ".desc", ""));
			category.setIcon(DashboardIcons.forCategory(categoryKey));
			byKey.put(categoryKey, category);
		}
		category.getTilesList().add(tile);
	}

	/**
	 * MAPS ONE MODULE ROW ONTO THE TILE THE DASHBOARD DRAWS FOR IT.
	 *
	 * THE navType AND THE TITLE KEY OF EVERY BRANCH BELOW ARE TAKEN VERBATIM FROM THE c:if CHAIN
	 * THAT USED TO LIVE IN dashboard.jsp, SO CLICK BEHAVIOUR AND WORDING ARE UNCHANGED.
	 */
	private static DashboardTileDetails buildTile(ModuleDetails modDetails, MessageProperties msgProps) {
		if (null == modDetails || null == modDetails.getModuleType()) {
			return null;
		}

		DashboardTileDetails tile = new DashboardTileDetails();
		int moduleType = modDetails.getModuleType().intValue();

		if (moduleType == AccessManagementInterface.MODULE_TYPE_DEFAULT) {
			tile.setNavType("DEFAULT");
			tile.setRefKey(safe(modDetails.getModuleRefkey()));
			tile.setTileKey(safe(modDetails.getModuleRefkey()));
			tile.setTitle(safe(modDetails.getModuleDisplayName()));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_NORMAL) {
			tile.setNavType("NORMAL");
			tile.setRefKey(safe(modDetails.getModuleRefkey()));
			tile.setTileKey(safe(modDetails.getModuleRefkey()));
			tile.setTitle(safe(modDetails.getModuleDisplayName()));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_ENGINE) {
			tile.setNavType("ENGINE");
			tile.setTileKey(TILE_ENGINE);
			tile.setTitle(resolve(msgProps, "label.engine", TILE_ENGINE));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_TRANSMISSION) {
			tile.setNavType("MISSION");
			tile.setTileKey(TILE_TRANSMISSION);
			tile.setTitle(resolve(msgProps, "label.mission", TILE_TRANSMISSION));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_VEHICLETYPE) {
			tile.setNavType("VEHICLE_TYPE");
			tile.setTileKey(TILE_VEHICLE_TYPE);
			tile.setTitle(resolve(msgProps, "label.vehicletype", TILE_VEHICLE_TYPE));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_SSTMAINTENANCE) {
			tile.setNavType("SST_MAINTENANCE");
			tile.setTileKey(TILE_SST_MAINTENANCE);
			tile.setTitle(resolve(msgProps, "label.dash.sstmaintenance", TILE_SST_MAINTENANCE));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_SSTVEHICLEDATA) {
			tile.setNavType("SST_VEHICLE_MAINTENANCE");
			tile.setTileKey(TILE_SST_VEHICLE_DATA);
			tile.setTitle(resolve(msgProps, "label.dash.sstvehicletypemanagement", TILE_SST_VEHICLE_DATA));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_CDCREATION) {
			tile.setNavType("CD_CREATION");
			tile.setTileKey(TILE_CD_CREATION);
			tile.setTitle(resolve(msgProps, "label.dash.CD_CREATION", TILE_CD_CREATION));
		} else if (moduleType == AccessManagementInterface.MODULE_TYPE_RUMVIN) {
			tile.setNavType("RUMVIN");
			tile.setTileKey(TILE_RUM_VIN);
			tile.setTitle(resolve(msgProps, "label.rumvin", TILE_RUM_VIN));
		} else {
			// UNKNOWN MODULE TYPE - NOT CLICKABLE IN ANY EXISTING BRANCH OF THE SERVLET, SO DROP IT
			// RATHER THAN DRAW A DEAD TILE
			return null;
		}

		// THE SUBTITLE UNDER THE TILE NAME. OPTIONAL - AN ABSENT KEY JUST LEAVES IT BLANK
		tile.setDescription(resolve(msgProps, "label.dashtile." + tile.getTileKey() + ".desc", ""));
		// THE TILE ICON. AN UNMAPPED KEY GETS THE NEUTRAL DEFAULT RATHER THAN A HOLE IN THE GRID
		tile.setIcon(DashboardIcons.forTile(tile.getTileKey()));
		return tile;
	}

	private static String resolve(MessageProperties msgProps, String key, String fallback) {
		String value = null;
		if (null != msgProps) {
			value = msgProps.getProperty(key);
		}
		if (null == value || "".equals(value.trim())) {
			return fallback;
		}
		return value.trim();
	}

	private static String safe(String value) {
		return null == value ? "" : value.trim();
	}
}
