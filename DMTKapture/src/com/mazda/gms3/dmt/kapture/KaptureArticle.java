package com.mazda.gms3.dmt.kapture;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ONE DOCUMENT TO BE WRITTEN TO KAPTURE - what the conversion hands to KaptureContentService.
 * Replaces the InfoManager ContentRecordITO the conversion used to fill.
 */
public class KaptureArticle {

	/** A category to tag the document with. */
	public static class Category {
		public final String refKey;
		public final String name;
		/** true for an ESI category - Kapture takes those in a separate list. */
		public final boolean esi;

		public Category(String refKey, String name, boolean esi) {
			this.refKey = refKey;
			this.name = name;
			this.esi = esi;
		}
	}

	/** A file to be sent as an attachment of the document. */
	public static class Attachment {
		public final File file;
		public final String title;

		public Attachment(File file, String title) {
			this.file = file;
			this.title = title;
		}
	}

	/** Kapture document id. NULL/blank for a new document - Kapture assigns one and returns it. */
	public String documentId = null;
	/** Channel type and name of the content node: SERVICE_MANUALS, WIRING_DIAGRAMS, OTHER_SERVICE_MANUALS. */
	public String channelType = null;
	/** Locale in the underscore form, e.g. ja_JP. */
	public String locale = null;
	public String title = null;
	/** Where the document came from - kept by Kapture for traceability only. */
	public String sourceIdentifier = null;
	public String sourceFileName = null;

	public List<Category> categories = new ArrayList<Category>();

	/**
	 * The attributes of the channel, in schema order: element name to String value, or to an
	 * Attachment for a file attribute, or to a nested Map for a group element (ATTACHMENTS).
	 */
	public Map<String, Object> fields = new LinkedHashMap<String, Object>();
}
