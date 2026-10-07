package com.mazda.gms3.dmt.preview;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ONE DOCUMENT OF A JOB'S PREVIEW, as collected while the job writes it to Kapture.
 */
public class PreviewDocument {

	/** A file of the document the preview offers as a link (PDF). url is relative to the job folder. */
	public static class Attachment {
		public final String name;
		public final String url;

		public Attachment(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}

	public String id;
	public String version;
	/** Kapture row id of the version the job wrote (k_article). */
	public String rowId;
	public String title;
	public String channel;
	public String type;
	public String modelType;
	public String model;
	public String modelFolder;
	public String manualTypeFolder;
	/** Name of the manual type - the root node of the document in the tree. */
	public String manualType;
	public String sourcePath;

	/** The page written from the document content, relative to the job folder (e.g. SM231260.html). */
	public String pageUrl;
	/** The document's own HTML file in OKAssets (WD / OEM), web path with the OKAssets context. */
	public String fileUrl;
	public final List<Attachment> attachments = new ArrayList<Attachment>();

	/** Display order positions: sequence number, then level 1..6 names (blank levels dropped). */
	public final Set<List<String>> placements = new LinkedHashSet<List<String>>();
	/** Keys of the job's VIN list (PreviewBuilder) the document is mapped to. */
	public final Set<String> vinKeys = new LinkedHashSet<String>();

	/** What the right pane opens: the HTML (page or file) first, else the first attachment. */
	public String url() {
		if (null != pageUrl) {
			return pageUrl;
		}
		if (null != fileUrl) {
			return fileUrl;
		}
		return attachments.isEmpty() ? null : attachments.get(0).url;
	}
}
