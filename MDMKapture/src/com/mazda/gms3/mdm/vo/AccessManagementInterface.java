package com.mazda.gms3.mdm.vo;

public interface AccessManagementInterface {

	public static final int MODULE_TYPE_DEFAULT = 1;
	public static final int MODULE_TYPE_NORMAL = 2;
	public static final int MODULE_TYPE_ENGINE = 3;
	public static final int MODULE_TYPE_TRANSMISSION = 4;
	public static final int MODULE_TYPE_SSTMAINTENANCE = 5;
	public static final int MODULE_TYPE_SSTVEHICLEDATA = 6;
	public static final int MODULE_TYPE_VEHICLETYPE = 7;
	public static final int MODULE_TYPE_CDCREATION = 8;
	public static final int MODULE_TYPE_RUMVIN=9;

	public static final int ONLY_READ_ACCESS = 1;
	public static final int ONLY_WRITE_ACCESS = 2;
	public static final int READ_AND_WRITE_ACCESS = 3;

	public static final int SUPER_ADMIN_ROLE_PRIORITY = 1;

	public static final String REF_KEY_DASHBOARD = "DASHBOARD";
	public static final String REF_KEY_ACCESS_MANAGEMENT = "ACCESS_MANAGEMENT";
	public static final String REF_KEY_COUNTRY_LOCALE = "COUNTRY_LOCALE";
	public static final String REF_KEY_MANUAL_LANGUAGE = "MANUAL_LANGUAGE";
	public static final String REF_KEY_MANUAL_TYPE = "MANUAL_TYPE";
	public static final String REF_KEY_CARLINE = "CARLINE";
	public static final String REF_KEY_VIN = "VIN";
	public static final String REF_KEY_VIN_CROSS_REFERENCE = "VIN_CROSS_REFERENCE";
	public static final String REF_KEY_ESI_CATEGORY = "ESI_CATEGORY";
	public static final String REF_KEY_CVC_CATEGORY = "CVC_CATEGORY";
	public static final String REF_KEY_DISPLAY_ORDER = "DISPLAY_ORDER";
	public static final String REF_KEY_SIVIN_RANGE = "SIVIN_RANGE";
	public static final String REF_KEY_MNAO_SIVIN_RANGE = "MNAO_SIVIN_RANGE";
	public static final String REF_KEY_MC_SIVIN_RANGE = "MC_SIVIN_RANGE";
	public static final String REF_KEY_ENGINE_BOOK = "ENGINE_BOOK";
	public static final String REF_KEY_ENGINE_TYPE = "ENGINE_TYPE";
	public static final String REF_KEY_TRANSMISSION_BOOK = "TRANSMISSION_BOOK";
	public static final String REF_KEY_TRANSMISSION_TYPE = "TRANSMISSION_TYPE";
	public static final String REF_KEY_DRIVEAXLE_TYPE = "DRIVEAXLE_TYPE";
	public static final String REF_KEY_BODY_TYPE = "BODY_TYPE";
	public static final String REF_KEY_ABBREVIATION_MASTER = "ABBREVIATION_MASTER";
	public static final String REF_KEY_DIVISION_MASTER = "DIVISION_MASTER";
	public static final String REF_KEY_SECTION_MASTER = "SECTION_MASTER";
	public static final String REF_KEY_SSTIMAGE_MASTER = "SSTIMAGE_MASTER";
	public static final String REF_KEY_USEIMAGE_MASTER = "USEIMAGE_MASTER";
	public static final String REF_KEY_SST_MASTER = "SST_MASTER";
	public static final String REF_KEY_SST_SECTION_PARENT_MAPPING = "SST_SECTION_PARENT_MAPPING";
	public static final String REF_KEY_SST_DIVISION_MODEL_MAPPING = "SST_DIVISION_MODEL_MAPPING";
	public static final String REF_KEY_SST_MODEL_VIN_MAPPING = "SST_MODEL_VIN_MAPPING";
	public static final String REF_KEY_SST_OEM_MAPPING = "SST_OEM_MAPPING";
	public static final String REF_KEY_SST_UPLOAD_IMAGES = "SST_UPLOAD_IMAGES";
	public static final String REF_KEY_SST_SECTION_MODEL_MAPPING = "SST_SECTION_MODEL_MAPPING";
	public static final String REF_KEY_SST_MDM_VIN_MAPPING = "SST_MDM_VIN_MAPPING";
	public static final String REF_KEY_SI_INNERLINKS_IDENTIFICATION_HISTORY = "SI_INNERLINKS_IDENTIFICATION_HISTORY";
	public static final String REF_KEY_CD_CREATION = "CD_CREATION";
	public static final String REF_KEY_CD_CREATION_SETTINGS = "CD_CREATION_SETTINGS";
	public static final String REF_KEY_CD_CREATION_HISTORY = "CD_CREATION_HISTORY";
	public static final String REF_KEY_MME_SIVIN_RANGE = "MME_SIVIN_RANGE";
	public static final String REF_KEY_RUM_VIN_SCHEDULE="RUM_VIN_SCHEDULE";
	public static final String REF_KEY_RUM_VIN_DATA="RUM_VIN_DATA";
	public static final String REF_KEY_ESI_LABELS_UPDATE_SCHEDULE="ESI_LABELS_UPDATE_SCHEDULE";
	public static final String REF_KEY_MME_SIVIN_BATCH_TRANSACTIONS="MME_SIVIN_BATCH_TRANSACTIONS";
	public static final String REF_KEY_MC_SIVIN_BATCH_TRANSACTIONS="MC_SIVIN_BATCH_TRANSACTIONS";
	public static final String REF_KEY_MNAO_SIVIN_BATCH_TRANSACTIONS="MNAO_SIVIN_BATCH_TRANSACTIONS";
	public static final String REF_KEY_MASTERDATA_LOCALE_MAPPING="MASTERDATA_LOCALE_MAPPING";
	public static final String REF_KEY_RMI_TOOL="RMI_TOOL";
	public static final String REF_KEY_RMI_TOOL_HISTORY="RMI_TOOL_HISTORY";
	public static final String REF_KEY_SI_CHANNEL_DATA_LOAD = "SI_CHANNEL_DATA_LOAD";
	public static final String REF_KEY_MNAO_DATA_EXPORT_TOOL="MNAO_DATA_EXPORT_TOOL";
	public static final String REF_KEY_MNAO_DATA_EXPORT_TOOL_HISTORY="MNAO_DATA_EXPORT_TOOL_HISTORY";
	
	public static final String REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL="NEW_MNAO_DATA_EXPORT_TOOL";
	public static final String REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL_HISTORY="NEW_MNAO_DATA_EXPORT_TOOL_HISTORY";
	
	
	public static final String REF_KEY_SI_TRANSLATION_UPDATE="SI_TRANSLATION_UPDATE";
	
	public static final String SYNC_STATUS_YES="Y";
	public static final String SYNC_STATUS_NO="N";
	
	public static final String OPERATION_TYPE_IMPORT="Import";
	public static final String OPERATION_TYPE_EXPORT="Export"; 
	
	public static final String REF_KEY_SCH_MAIN_VIN_MAPPING="SCH_MAIN_VIN_MAPPING";
}
