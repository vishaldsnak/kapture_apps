package com.mazda.gms3.dmt.publish;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ONE DOCUMENT OF A PUBLISH CONTENT JOB: its row in gms3_dmt_preview_docs (the job it was
 * scheduled from), what the DMT document table holds for it now, and what publishing did.
 */
public class PublishDocument {

	public static final String RESULT_SUCCESS = "SUCCESS";
	public static final String RESULT_FAILURE = "FAILURE";
	public static final String RESULT_DELETED = "DELETED";

	// ---- gms3_dmt_preview_docs -----------------------------------------------------------------
	public String documentId;
	/** Kapture locale (ja_JP / en_UK) */
	public String locale;
	/** Kapture channel type (SERVICE_MANUALS / WIRING_DIAGRAMS / OTHER_SERVICE_MANUALS) */
	public String channel;
	public String title;
	public String modelType;
	public String modelFolder;
	public String manualType;
	public String previewStatus;
	/** the job whose delete processing unpublished the document (DELETED rows) */
	public Long deletedByScheduleId;

	// ---- DMT document table (PublishMarket.documentTable) -------------------------------------------
	public boolean inDmtTables = false;
	public String dmtTable;
	public String docStatus;
	public String version;
	/** k_article row id of the latest written version */
	public String rowId;
	public String publishedFlag;
	public String model;
	public String materialFolder;
	public String carlineCode;
	public String faceliftFolder;

	// ---- the publish job -----------------------------------------------------------------------
	public Long itemId;
	public String result;
	public String message;
	public String publishedVersion;
	public String publishedRowId;
	/** the other jobs whose preview listed the document (their page now says it is published) */
	public final Set<String> otherJobs = new LinkedHashSet<String>();

	/** the item (criteria row) of the publish job the document is counted under */
	public String itemKey() {
		return t(modelType) + "|" + t(modelFolder) + "|" + t(manualType) + "|" + t(materialFolder);
	}

	static String t(String s) {
		return null == s ? "" : s.trim();
	}
}
