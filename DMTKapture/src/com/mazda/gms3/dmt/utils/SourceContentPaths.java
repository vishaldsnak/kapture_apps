package com.mazda.gms3.dmt.utils;

import java.io.File;

/**
 * WHERE THE SOURCE CONTENT AND THE MASTER DATA FILES ARE, from the path of the Settings page.
 *
 * The source location holds two folders side by side:
 *     <source.content.folder.name>     (GMS3_Content) - the market folders with the content
 *     <source.masterdata.folder.name>  (MDM)          - the master data Excel files
 *
 * The path of the Settings page may be given either as the location holding the two folders
 * or as the content folder itself - both are understood here.
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
		String root = contentRoot(settingsPath);
		String parent = root.lastIndexOf("\\") > 0 ? root.substring(0, root.lastIndexOf("\\")) : root;
		return PathUtil.file(parent + "\\" + ApplicationProperties.getProperty("source.masterdata.folder.name").trim());
	}

	private static String stripEnd(String path) {
		while (path.length() > 1 && path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
}
