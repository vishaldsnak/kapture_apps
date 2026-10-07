package com.mazda.gms3.dmt.kapture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Outcome of ONE HTTP request to Kapture.
 *
 * A 2xx IS NOT SUCCESS on its own. This object only says whether the REQUEST went through;
 * for the bulk endpoints every document in the request has its own status inside the body,
 * which is read by KaptureContentService.
 */
public class KaptureApiResult {

	/** 0 = no HTTP answer at all (network error or timeout). */
	public int httpStatus = 0;
	public String body = "";
	public String message = "";
	/** Value of a Retry-After header in seconds, 0 when absent. */
	public int retryAfterSeconds = 0;
	/**
	 * The read timed out. The server may still be processing the request, so it must NEVER be
	 * sent again blindly - a resend can create the same document twice.
	 */
	public boolean timedOut = false;
	/** The JSON that was sent, kept so that a failure can be logged together with its request. */
	public String requestBody = null;

	public boolean isHttpOk() {
		return httpStatus >= 200 && httpStatus < 300;
	}

	/**
	 * For the single-document endpoints, which can answer HTTP 200 with a body that says
	 * {"status":"500", ...}: true only when the HTTP status is 2xx AND the body carries no
	 * numeric status outside 2xx.
	 */
	public boolean isOk() {
		if (!isHttpOk()) {
			return false;
		}
		try {
			JsonElement root = new JsonParser().parse(body);
			if (null != root && root.isJsonObject()) {
				JsonObject obj = root.getAsJsonObject();
				if (obj.has("status") && obj.get("status").isJsonPrimitive()) {
					String s = obj.get("status").getAsString().trim();
					if (s.matches("[0-9]{3}")) {
						int code = Integer.parseInt(s);
						return code >= 200 && code < 300;
					}
				}
			}
		} catch (Exception e) {
			// not JSON - the HTTP status is all there is
		}
		return true;
	}

	public String describe() {
		return "http=" + httpStatus + (timedOut ? " (timed out)" : "") + " message=" + message;
	}
}
