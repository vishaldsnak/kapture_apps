package com.mazda.gms3.dmt.masterdata;

/** One line of the master data reports: a row of an Excel file, or a category sent to Kapture. */
public class MasterDataRecord {

	public static final String RESULT_ADDED = "ADDED";
	public static final String RESULT_UPDATED = "UPDATED";
	public static final String RESULT_NO_CHANGE = "NO CHANGE";
	public static final String RESULT_DELETED = "DELETED";
	public static final String RESULT_FAILED = "FAILED";

	/** Master data type: CARLINE, CAT, ENGINE_BOOK, ENGINE_TYPE, TRANSMISSION_BOOK, TRANSMISSION_TYPE, ALL_VIN. */
	public String type = "";
	public String fileName = "";
	public String locale = "";
	/** Row number in the Excel sheet as the user sees it (the header is row 1). */
	public int rowNumber = 0;
	/** The values of the row that identify it. */
	public String key = "";
	public String indicator = "";
	public String result = "";
	public String message = "";

	// category lines only
	public String categoryLevel = "";
	public String categoryRefKey = "";
	public String categoryName = "";
	public String parentRefKey = "";

	public boolean failed() {
		return RESULT_FAILED.equals(result);
	}
}
