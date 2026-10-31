package com.mazda.gms3.mdm.utils;

/**
 * LIBRARY PATHS READ OUT OF KAPTURE CONTENT.
 *
 * Job 10 and DMT send every OKAssets library path to Kapture with the kapture.library.path.context
 * prefix (/content/library/MAZDA/...), older content still carries /library/MAZDA/... - both forms
 * live side by side. The physical roots MDM joins these paths to (cdrom.server.path.name,
 * cdrom.image.server.path, dataexttool / rmitool okassets source dir) already end in the context
 * folder, so the prefix is dropped before the join; a path without it is returned unchanged.
 * The same rule as DMT OkAssetsWeb.withoutKaptureContext. Missing key = error.
 */
public final class KaptureLibraryPath {

	private KaptureLibraryPath() {
	}

	/**
	 * /content/library/MAZDA/... -> /library/MAZDA/... (a blank context returns the path as it is)
	 */
	public static String withoutKaptureContext(String path) {
		if (null == path) {
			return null;
		}
		String ctx = ApplicationProperties.getProperty("kapture.library.path.context");
		if (null == ctx) {
			throw new IllegalStateException("kapture.library.path.context is not set in application.properties");
		}
		ctx = ctx.trim().replace("\\", "/");
		while (ctx.endsWith("/")) {
			ctx = ctx.substring(0, ctx.length() - 1);
		}
		if ("".equals(ctx)) {
			return path;
		}
		if (!ctx.startsWith("/")) {
			ctx = "/" + ctx;
		}
		String p = path.trim();
		String slashed = p.replace("\\", "/");
		if (!slashed.startsWith("/")) {
			slashed = "/" + slashed;
		}
		if (slashed.toLowerCase().startsWith(ctx.toLowerCase() + "/")) {
			return slashed.substring(ctx.length());
		}
		return path;
	}

	/**
	 * EXPORTED XML (RMI Export / MNAO Data Export): every library path back in the legacy form - a
	 * /content/library/... that starts a node value (>), a CDATA value ([) or an attribute value (" or ')
	 * becomes /library/... . Legacy exports carry /library/MAZDA/... (WD HTML5_UPLOAD_DIRECTORY and
	 * DJVU_FILE_LOCATION, the SM css link ...) and resolve it under the export's OkAssets folder.
	 */
	public static String xmlLibraryPathsWithoutKaptureContext(String xml) {
		if (null == xml || "".equals(xml)) {
			return xml;
		}
		String ctx = ApplicationProperties.getProperty("kapture.library.path.context");
		if (null == ctx) {
			throw new IllegalStateException("kapture.library.path.context is not set in application.properties");
		}
		ctx = ctx.trim().replace("\\", "/");
		while (ctx.endsWith("/")) {
			ctx = ctx.substring(0, ctx.length() - 1);
		}
		if ("".equals(ctx)) {
			return xml;
		}
		if (!ctx.startsWith("/")) {
			ctx = "/" + ctx;
		}
		return xml.replaceAll("(?i)(?<=[>\\[\"'])" + java.util.regex.Pattern.quote(ctx) + "(?=/library/)", "");
	}

	/**
	 * window.js text: every quoted address "/content/library/... (or '...) becomes "/library/...,
	 * so new DMT window.js files and the legacy ones are rewritten by the same CD search strings.
	 */
	public static String quotedAddressesWithoutKaptureContext(String js) {
		if (null == js || "".equals(js)) {
			return js;
		}
		String ctx = ApplicationProperties.getProperty("kapture.library.path.context");
		if (null == ctx) {
			throw new IllegalStateException("kapture.library.path.context is not set in application.properties");
		}
		ctx = ctx.trim().replace("\\", "/");
		while (ctx.endsWith("/")) {
			ctx = ctx.substring(0, ctx.length() - 1);
		}
		if ("".equals(ctx)) {
			return js;
		}
		if (!ctx.startsWith("/")) {
			ctx = "/" + ctx;
		}
		return js.replaceAll("([\"'])" + java.util.regex.Pattern.quote(ctx) + "(?=/)", "$1");
	}
}
