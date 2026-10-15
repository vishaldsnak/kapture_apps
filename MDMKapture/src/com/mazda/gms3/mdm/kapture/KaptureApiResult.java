package com.mazda.gms3.mdm.kapture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Outcome of one Kapture REST call.
 *
 * httpStatus 0 means the call never reached the server (transport failure, or a step that was
 * deliberately not attempted), which is why isOk() checks the 2xx range rather than "no
 * exception was thrown".
 *
 * A 2xx IS NOT ENOUGH ON ITS OWN. Kapture answers some refusals with HTTP 200 and puts the real
 * outcome in the body:
 *
 *   HTTP 200
 *   {"status":"500","message":"Error while publishing article: Unparseable date: ..."}
 *
 * Read as a success that is silent corruption - SI Translation Update reported a published
 * document as re-published while the publish had been refused and the document was left off the
 * air, with the old content still live. So the body's own status is checked too.
 */
public class KaptureApiResult {

	public int httpStatus = 0;
	public String body = null;
	public String message = null;

	public boolean isOk() {
		return httpStatus >= 200 && httpStatus < 300 && !bodyReportsFailure();
	}

	/**
	 * TRUE ONLY WHEN THE BODY EXPLICITLY SAYS IT FAILED.
	 *
	 * DELIBERATELY NARROW. The endpoints do not agree on this field - some omit it, some send
	 * "success", some send a numeric code - and a body that is not JSON at all is a normal answer
	 * from others. Anything this cannot read as a definite non-2xx code is left as it was, so the
	 * only calls whose outcome changes are the ones that stated a failure outright.
	 */
	private boolean bodyReportsFailure() {
		try {
			if (null == body || "".equals(body.trim()) || !body.trim().startsWith("{")) {
				return false;
			}
			JsonElement parsed = new JsonParser().parse(body);
			if (null == parsed || !parsed.isJsonObject()) {
				return false;
			}
			JsonObject object = parsed.getAsJsonObject();
			if (!object.has("status") || object.get("status").isJsonNull()
					|| !object.get("status").isJsonPrimitive()) {
				return false;
			}
			int status = Integer.parseInt(object.get("status").getAsString().trim());
			return status < 200 || status >= 300;
		} catch (Exception e) {
			// not a numeric status, or not parseable - not something to call a failure
			return false;
		}
	}

	/** The body's own message, which is where Kapture puts the reason for a 200-with-status-500. */
	public String bodyMessage() {
		try {
			if (null == body || !body.trim().startsWith("{")) {
				return "";
			}
			JsonObject object = new JsonParser().parse(body).getAsJsonObject();
			return (object.has("message") && object.get("message").isJsonPrimitive())
					? object.get("message").getAsString() : "";
		} catch (Exception e) {
			return "";
		}
	}
}
