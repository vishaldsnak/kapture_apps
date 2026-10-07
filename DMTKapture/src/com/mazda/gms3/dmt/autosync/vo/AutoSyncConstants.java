package com.mazda.gms3.dmt.autosync.vo;

public interface AutoSyncConstants {
	
	public static final String STATUS_YES="Y";
	public static final String STATUS_NO="N";
	public static final String STATUS_ACTIVE="A";
	public static final String STATUS_INACTIVE="I";
	
	public static final String STATUS_PENDING="Pending";
	public static final String STATUS_PROCESSING="Processing";
	public static final String STATUS_COMPLETED="Completed";
	public static final String STATUS_SUCCESS="Success";
	public static final String STATUS_FAILURE="Failure";
	public static final String STATUS_ABORTED="Aborted";
	
	// REMOVE SERVICE MANUAL TYPE FOR ALL MARKETS - FEEDBACK 20TH NOVEMBER
//	public static final String ITEMS_KEYS="MODEL_YEAR,VIN,CARLINE,ESI,CVC,ENGINE_TYPE,TRANSMISSION_TYPE,AXLE_TYPE,BODY_TYPE,SERVICE_MANUAL_TYPE";
//	public static final String ITEMS_LABELS="Model and Year,VIN Range,Carline,ESI Category,CVC Code,Engine Type,Transmission Type,Driveline/Axle Type,Body Type,Service Manual Type";
	
	public static final String ITEMS_KEYS="ALL,MODEL_YEAR,VIN,CARLINE,ESI,CVC,ENGINE_TYPE,TRANSMISSION_TYPE,AXLE_TYPE,BODY_TYPE";
	public static final String ITEMS_LABELS="All,Model and Year,VIN Range,Carline + VIN,ESI Category,CVC Code,Engine Type,Transmission Type,Driveline/Axle Type,Body Type";
	
	
	public static final String ITEM_KEY_MODEL_YEAR="MODEL_YEAR";
	public static final String ITEM_KEY_VIN_RANGE="VIN";
	public static final String ITEM_KEY_CARLINE="CARLINE";
	public static final String ITEM_KEY_ESI_CATEGORY="ESI";
	public static final String ITEM_KEY_CVC_CATEGORY="CVC";
	public static final String ITEM_KEY_ENGINE_TYPE="ENGINE_TYPE";
	public static final String ITEM_KEY_TRANSMISSION_TYPE="TRANSMISSION_TYPE";
	public static final String ITEM_KEY_AXLE_TYPE="AXLE_TYPE";
	public static final String ITEM_KEY_BODY_TYPE="BODY_TYPE";
	public static final String ITEM_KEY_MANUAL_TYPE="SERVICE_MANUAL_TYPE";
	public static final String ITEM_KEY_ALL="ALL";
	
	
	public static final String ITEM_LABEL_MODEL_YEAR="Model and Year";
	public static final String ITEM_LABEL_VIN_RANGE="VIN Range";
	public static final String ITEM_LABEL_CARLINE="Carline + VIN";
	public static final String ITEM_LABEL_ESI_CATEGORY="ESI Category";
	public static final String ITEM_LABEL_CVC_CATEGORY="CVC Code";
	public static final String ITEM_LABEL_ENGINE_TYPE="Engine Type";
	public static final String ITEM_LABEL_TRANSMISSION_TYPE="Transmission Type";
	public static final String ITEM_LABEL_AXLE_TYPE="Driveline/Axle Type";
	public static final String ITEM_LABEL_BODY_TYPE="Body Type";
	public static final String ITEM_LABEL_MANUAL_TYPE="Service Manual Type";

}
