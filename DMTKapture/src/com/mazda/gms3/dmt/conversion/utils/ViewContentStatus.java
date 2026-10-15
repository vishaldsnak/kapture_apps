package com.mazda.gms3.dmt.conversion.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.vo.ContentDetails;

/**
 * VC_CONTENT_STATUS OF THE VIEW CONTENT ROWS THE DISPLAY ORDER STEP WRITES (all markets).
 *
 * The status follows the DMT document: Published when its published flag is flag.value.publish, else
 * Draft. A document this job converted is always saved with the draft flag (Kapture writes are
 * unpublished), so its rows are Draft and its Publish Content job sets them Published. A document
 * the display order lists but this job did NOT convert keeps the state it has - an already published
 * one does not fall back to Draft (it is in no preview of this job, so nothing would publish it again).
 */
public class ViewContentStatus {

	/** The status for a document with this published flag (the DMT document table's value). */
	public static String forPublishedFlag(String publishedFlag) {
		boolean published = null != publishedFlag && publishedFlag.trim().equals(KaptureApiClient.require("flag.value.publish").trim());
		return KaptureApiClient.require(published ? "view.content.status.published" : "view.content.status.draft");
	}

	/**
	 * Document id -> status, from the documents of the display order step (their getImDocStatus carries
	 * the published flag read with the document id).
	 */
	public static Map<String, String> byDocument(Collection<ContentDetails> docs) {
		Map<String, String> m = new HashMap<String, String>();
		if (null != docs) {
			for (ContentDetails cd : docs) {
				if (null != cd.getDocumentId() && !"".equals(cd.getDocumentId().trim())) {
					m.put(cd.getDocumentId().trim(), forPublishedFlag(cd.getImDocStatus()));
				}
			}
		}
		return m;
	}

	/** The status of one row: the document's, Draft when the document is not known. */
	public static String of(Map<String, String> byDocument, String documentId) {
		String s = null == documentId ? null : byDocument.get(documentId.trim());
		return null == s ? KaptureApiClient.require("view.content.status.draft") : s;
	}
}
