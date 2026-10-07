package com.mazda.gms3.dmt.kapture;

/**
 * WHAT KAPTURE DID WITH ONE DOCUMENT. Replaces the values the conversion used to read back
 * from the InfoManager ContentRecordITO (document id, version, published flag).
 */
public class KaptureRecordResult {

	/** true only when Kapture reported SUCCESS for this document itself. */
	public boolean success = false;
	/** Kapture document id (e.g. SM179790) - on a create, the id Kapture assigned. */
	public String documentId = null;
	/** k_article row id of the written version. */
	public String rowId = null;
	public String articleVersion = null;
	public String publishedVersion = null;
	/** "Published" / "Unpublished" after the write. */
	public String articleState = null;
	/** Kapture's error code for a failed document, e.g. VALIDATION_ERROR. */
	public String errorCode = null;
	/** Message for the reports: Kapture's own, plus the field level validation errors. */
	public String message = null;
	/**
	 * Set on a SUCCESSFUL write that is still incomplete - a new document Kapture created whose
	 * attachments could not be added afterwards. The document exists and is saved; the warning
	 * goes to the failure report.
	 */
	public String warning = null;
	/** Error code the warning is reported under; null = KaptureContentService.ERR_ATTACHMENT (attachments not added). */
	public String warningCode = null;

	public static KaptureRecordResult failure(String errorCode, String message) {
		KaptureRecordResult r = new KaptureRecordResult();
		r.success = false;
		r.errorCode = errorCode;
		r.message = message;
		return r;
	}
}
