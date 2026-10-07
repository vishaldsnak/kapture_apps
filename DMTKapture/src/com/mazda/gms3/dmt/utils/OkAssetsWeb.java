package com.mazda.gms3.dmt.utils;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;

/**
 * WEB ADDRESS OF A FILE IN THE OKASSETS FOLDER, as the BROWSER must use it.
 *
 * OKAssets is served under okassets.web.context (e.g. /content), so the web path DMT keeps in its
 * properties (/library/MAZDA/...) is given to the browser as /content/library/MAZDA/...
 * Only for addresses given to the browser - paths sent to Kapture inside documents stay /library/...
 */
public final class OkAssetsWeb {

	private OkAssetsWeb() {
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
