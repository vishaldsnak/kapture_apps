package com.mazda.gms3.dmt.utils;

import java.io.File;

/**
 * WHERE THE SOURCE CONTENT AND THE MASTER DATA FILES ARE, from the path of the Settings page.
 *
 * The source location holds these folders side by side:
 *     <source.content.folder.name>     (GMS3_Content) - the market folders with the content
 *     <source.masterdata.folder.name>  (MDM)          - the master data Excel files
 *     <source.facelift.folder.name>    (Facelift_Content) - the facelift VIN range Excel files
 *
 * The path of the Settings page may be given either as the location holding the folders
 * or as the content folder itself - both are understood here. The MDM and facelift folders do not
 * need the content folder: a folder of that name directly under the Settings path is taken first
 * (a facelift-only source location has no GMS3_Content).
 */
public class SourceContentPaths {

	/** The folder that holds the market folders, in the backslash form the application uses. */
	public static String contentRoot(String settingsPath) {
		if (null == settingsPath || "".equals(settingsPath.trim())) {
			return settingsPath;
		}
		String path = stripEnd(settingsPath.trim().replace("/", "\\"));
		String contentFolderName = ApplicationProperties.getProperty("source.content.folder.name").trim();
		if (PathUtil.file(path + "\\" + contentFolderName).isDirectory()) {
			return path + "\\" + contentFolderName;
		}
		return path;
	}

	/** The folder with the master data Excel files; it may not exist. */
	public static File masterDataFolder(String settingsPath) {
		return besideContent(settingsPath, ApplicationProperties.getProperty("source.masterdata.folder.name").trim());
	}

	/** The folder with the facelift (VIN range replacement) Excel files, beside MDM; it may not exist. */
	public static File faceliftFolder(String settingsPath) {
		return besideContent(settingsPath, ApplicationProperties.getProperty("source.facelift.folder.name").trim());
	}

	/**
	 * A folder beside the content folder: directly under the Settings path when it is there (the location holding the
	 * folders, with or without GMS3_Content), else beside the content folder found by contentRoot.
	 */
	private static File besideContent(String settingsPath, String folderName) {
		if (null != settingsPath && !"".equals(settingsPath.trim())) {
			File direct = PathUtil.file(stripEnd(settingsPath.trim().replace("/", "\\")) + "\\" + folderName);
			if (direct.isDirectory()) {
				return direct;
			}
		}
		String root = contentRoot(settingsPath);
		String parent = root.lastIndexOf("\\") > 0 ? root.substring(0, root.lastIndexOf("\\")) : root;
		return PathUtil.file(parent + "\\" + folderName);
	}

	private static String stripEnd(String path) {
		while (path.length() > 1 && path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
}
