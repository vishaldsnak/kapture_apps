package com.mazda.gms3.mdm.vo;

/**
 * Definition of ONE screen's import template.
 *
 * Every screen that supports import registers one of these in ImportTemplateUtils. The
 * template the user downloads is generated from this definition, so the downloaded file
 * can never drift away from what the screen's readExcelData() expects - the column ORDER
 * here is the same order the servlet reads the cells in.
 */
public class ImportTemplateDetails {

	/** Key passed on the download URL, e.g. MANUAL_TYPE. */
	private String screenKey = null;

	/** Base of the generated file name, e.g. MANUALTYPE -> MANUALTYPE_TEMPLATE_<tmstp>.xlsx */
	private String fileBaseName = null;

	/** Name of the sheet holding the columns. */
	private String sheetName = null;

	/**
	 * Variant of this screen's template, or null when the screen has only one layout.
	 *
	 * A few screens change their COLUMNS according to the country / language selected on
	 * the screen - Carline for instance reads cells 4 and 5 as YEAR START / YEAR END for
	 * the MNAO markets and as MODEL TYPE / ESI CATEGORY FLAG for the others. Such a screen
	 * registers one definition per variant and the variant is resolved at download time
	 * with the SAME rule the screen itself uses.
	 */
	private String variantKey = null;

	/**
	 * Data column headers WITHOUT the action column, in the exact order the screen reads
	 * them. ImportTemplateUtils appends the ACTION column as the last column for every
	 * screen, so that stays consistent everywhere.
	 */
	private String[] columnHeaders = null;

	public ImportTemplateDetails() {
	}

	public ImportTemplateDetails(String screenKey, String fileBaseName, String sheetName,
			String[] columnHeaders) {
		this(screenKey, fileBaseName, sheetName, columnHeaders, null);
	}

	public ImportTemplateDetails(String screenKey, String fileBaseName, String sheetName,
			String[] columnHeaders, String variantKey) {
		this.screenKey = screenKey;
		this.fileBaseName = fileBaseName;
		this.sheetName = sheetName;
		this.columnHeaders = columnHeaders;
		this.variantKey = variantKey;
	}

	public String getScreenKey() {
		return screenKey;
	}

	public void setScreenKey(String screenKey) {
		this.screenKey = screenKey;
	}

	public String getFileBaseName() {
		return fileBaseName;
	}

	public void setFileBaseName(String fileBaseName) {
		this.fileBaseName = fileBaseName;
	}

	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	public String getVariantKey() {
		return variantKey;
	}

	public void setVariantKey(String variantKey) {
		this.variantKey = variantKey;
	}

	public String[] getColumnHeaders() {
		return columnHeaders;
	}

	public void setColumnHeaders(String[] columnHeaders) {
		this.columnHeaders = columnHeaders;
	}
}
