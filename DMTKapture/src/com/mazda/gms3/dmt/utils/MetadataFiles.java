package com.mazda.gms3.dmt.utils;

/**
 * The metadata / attribute files of the source content - the ONE place that recognises them by
 * name, for the Schedule counts and the conversion alike. A name is checked by its START (or as
 * a whole) together with its extension, case-insensitive, so a document whose name only CONTAINS
 * one of the words (RM100000001GVIN.html, id0102q200vin.ent) is a document like any other.
 *
 *   METADATA (read as today, never counted or converted as a document):
 *     display order   d*.txt, not delete.txt   (file.name.displayorder + extension.txt)
 *     CD processing   cd*.txt                  (file.name.cdprocessing + extension.txt)
 *     ESI category    esicat.txt               (file.name.esicat + extension.txt)
 *     VIN             vin*.txt                 (file.vin.ent + extension.txt)
 *     SCM VIN         scmvin*.txt              (file.scm.vin.ent + extension.txt)
 *   IGNORED everywhere (not a document, not counted, not copied as an OKAssets file):
 *                     delete.txt               (file.name.delete + extension.txt)
 *                     leftmenu.txt             (file.leftment.txt + extension.txt)
 *                     vin*.ent, vin_attribute*.ent (file.vin.ent + extension.ent)
 *   window.js         exact name               (file.name.window.js) - not a document; the
 *                                              conversion still copies it to OKAssets (its paths
 *                                              are rewritten there)
 */
public class MetadataFiles {

	private MetadataFiles() {
	}

	private static String key(String k) {
		String v = ApplicationProperties.getProperty(k);
		if (null == v || "".equals(v.trim())) {
			throw new IllegalStateException("application.properties has no value for " + k);
		}
		return v.trim().toLowerCase();
	}

	private static String name(String fileName) {
		return null == fileName ? "" : fileName.trim().toLowerCase();
	}

	private static boolean startsAndEnds(String fileName, String startKey, String extensionKey) {
		String n = name(fileName);
		return n.startsWith(key(startKey)) && n.endsWith(key(extensionKey));
	}

	/** vin*.txt */
	public static boolean isVinFile(String fileName) {
		return startsAndEnds(fileName, "file.vin.ent", "extension.txt");
	}

	/** scmvin*.txt (MME WM) */
	public static boolean isScmVinFile(String fileName) {
		return startsAndEnds(fileName, "file.scm.vin.ent", "extension.txt");
	}

	/** d*.txt except delete.txt - the display order text file */
	public static boolean isDisplayOrderFile(String fileName) {
		return startsAndEnds(fileName, "file.name.displayorder", "extension.txt") && !isDeleteFile(fileName);
	}

	/** cd*.txt - the CD processing text file */
	public static boolean isCdProcessingFile(String fileName) {
		return startsAndEnds(fileName, "file.name.cdprocessing", "extension.txt");
	}

	/** esicat.txt */
	public static boolean isEsiCatFile(String fileName) {
		return name(fileName).equals(key("file.name.esicat") + key("extension.txt"));
	}

	/** delete.txt */
	public static boolean isDeleteFile(String fileName) {
		return name(fileName).equals(key("file.name.delete") + key("extension.txt"));
	}

	/** leftmenu.txt */
	public static boolean isLeftMenuFile(String fileName) {
		return name(fileName).equals(key("file.leftment.txt") + key("extension.txt"));
	}

	/** vin*.ent - vin_attribute*.ent included */
	public static boolean isVinEntFile(String fileName) {
		return startsAndEnds(fileName, "file.vin.ent", "extension.ent");
	}

	/** window.js exactly */
	public static boolean isWindowJs(String fileName) {
		return name(fileName).equals(key("file.name.window.js"));
	}

	/** delete.txt, leftmenu.txt, vin*.ent - ignored everywhere (no document, no OKAssets file) */
	public static boolean isIgnoredFile(String fileName) {
		return isDeleteFile(fileName) || isLeftMenuFile(fileName) || isVinEntFile(fileName);
	}

	/** Every metadata / attribute file: never counted or converted as a document. */
	public static boolean isMetadataFile(String fileName) {
		return isDisplayOrderFile(fileName) || isCdProcessingFile(fileName) || isEsiCatFile(fileName) || isVinFile(fileName)
				|| isScmVinFile(fileName) || isIgnoredFile(fileName) || isWindowJs(fileName);
	}
}
