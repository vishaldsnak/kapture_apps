package com.mazda.gms3.dmt.utils;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;

/**
 * WEB ADDRESS OF A FILE IN THE OKASSETS FOLDER, as the BROWSER must use it.
 *
 * OKAssets is served under okassets.web.context (e.g. /content), so the web path DMT keeps in its
 * properties (/library/MAZDA/...) is given to the browser as /content/library/MAZDA/...
 * Paths sent to Kapture inside documents are prefixed separately, with kapture.library.path.context
 * (see forKapture) - the same rule Job 10 ContentBulkImportJob applies to the one-time import.
 */
public final class OkAssetsWeb {

	private OkAssetsWeb() {
	}

	/**
	 * A document attribute value as it is sent to Kapture: every OKAssets library path in it gets the
	 * kapture.library.path.context prefix - /library/MAZDA/... -> /content/library/MAZDA/...
	 *   - a value that IS a path (HTML5_UPLOAD_DIRECTORY, DJVU_FILE_LOCATION, OEM_FILE_LOCATION ...)
	 *   - every src / href attribute in an HTML value (img, a, link, embed ...)
	 * The library folder comes from SERVER_LIBRARY_DIRECTORY. A path that already carries the prefix is
	 * left alone (it no longer starts with /library/), so a value is never prefixed twice. A blank
	 * kapture.library.path.context sends the paths unchanged. Missing key = error.
	 */
	public static String forKapture(String value) {
		if (null == value || "".equals(value)) {
			return value;
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
			return value;
		}
		if (!ctx.startsWith("/")) {
			ctx = "/" + ctx;
		}
		String library = KaptureApiClient.require("SERVER_LIBRARY_DIRECTORY").trim().replace("\\", "/");
		while (library.startsWith("/")) {
			library = library.substring(1);
		}
		if (!library.endsWith("/")) {
			library = library + "/";
		}
		library = "/" + library;
		String trimmed = value.trim();
		if (trimmed.startsWith(library)) {
			return ctx + trimmed;
		}
		if (value.indexOf(library) < 0) {
			return value;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("(?i)(\\b(?:src|href)\\s*=\\s*[\"']\\s*)(" + java.util.regex.Pattern.quote(library) + ")")
				.matcher(value);
		StringBuffer sb = new StringBuffer(value.length() + 1024);
		while (m.find()) {
			m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(1) + ctx + m.group(2)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * The reverse of forKapture for one path: /content/library/MAZDA/... -> /library/MAZDA/...
	 * Used where a path that may already carry the kapture.library.path.context prefix is compared with
	 * the /library/ path DMT builds (the stylesheet links the Kapture sanitizer keeps). Any other value,
	 * or a blank context, is returned as it is.
	 */
	public static String withoutKaptureContext(String path) {
		if (null == path) {
			return null;
		}
		String p = path.trim();
		String ctx = ApplicationProperties.getProperty("kapture.library.path.context");
		if (null == ctx) {
			throw new IllegalStateException("kapture.library.path.context is not set in application.properties");
		}
		ctx = ctx.trim().replace("\\", "/");
		while (ctx.endsWith("/")) {
			ctx = ctx.substring(0, ctx.length() - 1);
		}
		if ("".equals(ctx)) {
			return p;
		}
		if (!ctx.startsWith("/")) {
			ctx = "/" + ctx;
		}
		String library = KaptureApiClient.require("SERVER_LIBRARY_DIRECTORY").trim().replace("\\", "/");
		while (library.startsWith("/")) {
			library = library.substring(1);
		}
		if (!library.endsWith("/")) {
			library = library + "/";
		}
		return p.toLowerCase().startsWith((ctx + "/" + library).toLowerCase()) ? p.substring(ctx.length()) : p;
	}

	/** okassets.web.context without a trailing slash (missing key = error). */
	public static String context() {
		String ctx = KaptureApiClient.require("okassets.web.context").replace("\\", "/");
		while (ctx.endsWith("/")) {
			ctx = ctx.substring(0, ctx.length() - 1);
		}
		return ctx.startsWith("/") ? ctx : "/" + ctx;
	}

	/** /library/a/b.zip -> /content/library/a/b.zip (a path already carrying the context is kept). */
	public static String url(String webPath) {
		if (null == webPath || "".equals(webPath.trim())) {
			return webPath;
		}
		String p = webPath.trim().replace("\\", "/");
		if (!p.startsWith("/")) {
			p = "/" + p;
		}
		String ctx = context();
		return p.startsWith(ctx + "/") ? p : ctx + p;
	}
}
