package com.mazda.gms3.dmt.kapture;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * KAPTURE REST CLIENT - the replacement for the InfoManager IQServiceClient.
 *
 * One instance per job thread: it holds the session token of one login and is not shared
 * between threads. Same endpoints, payloads and rules as the bulk content import job.
 *
 * Every value comes from application.properties; a missing key stops the job with a clear
 * message instead of running on a guessed default.
 */
public class KaptureApiClient {

	private static Logger logger = LogManager.getLogger(KaptureApiClient.class);
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final String baseUrl;
	private final String loginPath;
	private final String bulkCreatePath;
	private final String bulkUpdatePath;
	private final String attachmentUploadPath;
	private final String unpublishPath;
	private final String email;
	private final String password;
	private final String appType;
	private final String publicIp;
	private final String userAgent;
	private final int connectTimeoutMs;
	private final int readTimeoutMs;
	private final int bulkReadTimeoutMs;
	private final int tokenRetryMax;
	private final int retryMax;
	private final int retryBaseDelayMs;
	private final int retryMaxDelayMs;

	private String token = null;
	/** k_user primary key of the API user - Kapture wants it as articleCreatorUserID and uploadedBy. */
	private String userPkId = null;
	/** "First Last" of the API user - createdBy / modifiedBy / owner. Never the e-mail. */
	private String userDisplayName = null;

	public KaptureApiClient() {
		String base = require("kapture.api.base");
		this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		this.loginPath = require("kapture.api.login.path");
		this.bulkCreatePath = require("kapture.api.bulk.create.path");
		this.bulkUpdatePath = require("kapture.api.bulk.update.path");
		this.attachmentUploadPath = require("kapture.api.attachment.upload.path");
		this.unpublishPath = require("kapture.api.unpublish.path");
		this.email = require("kapture.api.email");
		this.password = require("kapture.api.password");
		this.appType = require("kapture.api.appType");
		this.publicIp = require("kapture.api.publicIp");
		this.userAgent = require("kapture.api.userAgent");
		this.connectTimeoutMs = requireInt("kapture.api.connect.timeout.ms");
		this.readTimeoutMs = requireInt("kapture.api.read.timeout.ms");
		this.bulkReadTimeoutMs = requireInt("kapture.api.bulk.read.timeout.ms");
		this.tokenRetryMax = requireInt("kapture.api.token.retry.max");
		this.retryMax = requireInt("kapture.api.retry.max");
		this.retryBaseDelayMs = requireInt("kapture.api.retry.base.delay.ms");
		this.retryMaxDelayMs = requireInt("kapture.api.retry.max.delay.ms");
	}

	public static String require(String key) {
		String v = ApplicationProperties.getProperty(key);
		if (null == v || "".equals(v.trim())) {
			throw new IllegalStateException(key + " is not set in application.properties");
		}
		return v.trim();
	}

	public static int requireInt(String key) {
		try {
			return Integer.parseInt(require(key));
		} catch (NumberFormatException e) {
			throw new IllegalStateException(key + " in application.properties is not a number");
		}
	}

	public String getUserPkId() {
		return userPkId;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	// ---- login -------------------------------------------------------------

	public KaptureApiResult login() {
		JsonObject payload = new JsonObject();
		payload.addProperty("email", email);
		payload.addProperty("appType", appType);
		payload.addProperty("publicIp", publicIp);
		payload.addProperty("userAgent", userAgent);
		payload.addProperty("password", password);
		logger.info("login :: POST " + baseUrl + loginPath + " (email=" + email + ")");
		KaptureApiResult r = send("POST", baseUrl + loginPath, payload.toString(), false, readTimeoutMs, null);
		if (r.isHttpOk()) {
			String tok = null;
			try {
				JsonObject root = new JsonParser().parse(r.body).getAsJsonObject();
				JsonObject resp = root.has("response") && root.get("response").isJsonObject()
						? root.getAsJsonObject("response") : root;
				tok = str(resp, "userSessionToken");
				if (null == tok) {
					tok = findAnywhere(root, "userSessionToken");
				}
				// the profile sits in a nested object of the answer - searched in the whole tree,
				// the same way the bulk content import job reads it
				this.userPkId = findAnywhere(root, "userPkId");
				String first = findAnywhere(root, "firstName");
				String last = findAnywhere(root, "lastName");
				this.userDisplayName = ((null == first ? "" : first) + " " + (null == last ? "" : last)).trim();
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "login()", e);
			}
			if (null != tok && !"".equals(tok)
					&& (null == userPkId || "".equals(userPkId) || "".equals(userDisplayName))) {
				// Kapture refuses every article without owner / creator / creator id - stop here
				// with the reason instead of sending documents that are bound to be refused
				logger.error("login :: token received but the user profile is missing (userPkId=" + userPkId
						+ ", name=" + userDisplayName + "). Fields in the answer :: " + fieldNames(root(r.body)));
				r.httpStatus = 0;
				r.message = "user profile (userPkId / firstName / lastName) missing from login response";
				return r;
			}
			if (null != tok && !"".equals(tok)) {
				this.token = tok;
				logger.info("login :: token acquired, user=" + userDisplayName + " id=" + userPkId);
			} else {
				// A 2xx LOGIN WITH NO TOKEN IS A FAILED LOGIN
				logger.error("login :: response 2xx but userSessionToken missing");
				r.httpStatus = 0;
				r.message = "userSessionToken missing from login response";
			}
		} else {
			logger.error("login :: failed " + r.describe());
		}
		return r;
	}

	// ---- bulk create / update ----------------------------------------------

	/**
	 * @param arrayJson a JSON ARRAY of article objects
	 *
	 * publish=false is pinned: with it Kapture honours each record's own pubFlag, and DMT
	 * always sends pubFlag false - publishing is a separate job.
	 */
	public KaptureApiResult bulkCreate(String contentId, int articles, String arrayJson, String correlationId) {
		return bulkPost(bulkCreatePath, contentId, articles, arrayJson, correlationId);
	}

	public KaptureApiResult bulkUpdate(String contentId, int articles, String arrayJson, String correlationId) {
		return bulkPost(bulkUpdatePath, contentId, articles, arrayJson, correlationId);
	}

	private KaptureApiResult bulkPost(String path, String contentId, int articles, String arrayJson,
			String correlationId) {
		String url = baseUrl + path + "?contentId=" + enc(contentId) + "&batchSize=" + articles + "&publish=false";
		return callWithRetries("POST", url, arrayJson, bulkReadTimeoutMs, correlationId);
	}

	// ---- read --------------------------------------------------------------

	/** The latest version of a document for a locale, whatever its state. */
	public KaptureApiResult latestArticle(String articleId, String contentId, String locale) {
		String url = baseUrl + require("kapture.api.latest.path") + "/" + enc(articleId) + "/" + enc(contentId)
				+ "/" + enc(locale);
		return callWithRetries("GET", url, null, readTimeoutMs, null);
	}

	// ---- categories --------------------------------------------------------

	/**
	 * Create ONE category for ONE locale. The parent is given by its reference key and must
	 * already exist for that locale (a missing parent answers 404); blank parent = a root.
	 * A category that is already there answers 409 - see isAlreadyExists().
	 * A 2xx is not proof that the row was written: read k_categories back.
	 */
	public KaptureApiResult createCategory(String referenceKey, String name, String parentReferenceKey, String locale) {
		JsonObject payload = new JsonObject();
		payload.addProperty("categoryRoleName", name);
		payload.addProperty("categoryDescription", name);
		payload.addProperty("categoryRoleRef", referenceKey);
		payload.addProperty("parentRefKey", null == parentReferenceKey ? "" : parentReferenceKey);
		payload.addProperty("locale", locale);
		payload.addProperty("emailId", email);
		String url = baseUrl + require("kapture.api.category.create.path") + "?locale=" + enc(locale);
		return callWithRetries("POST", url, payload.toString(), readTimeoutMs, null);
	}

	/**
	 * Change the name of ONE category for ONE locale. The parent cannot be changed by this call.
	 */
	public KaptureApiResult updateCategory(String referenceKey, String name, String locale) {
		JsonObject payload = new JsonObject();
		payload.addProperty("categoryRoleName", name);
		payload.addProperty("categoryDescription", name);
		payload.addProperty("locale", locale);
		payload.addProperty("emailId", email);
		payload.addProperty("categoryRoleRef", referenceKey);
		String url = baseUrl + require("kapture.api.category.update.path") + "?categoryRef=" + enc(referenceKey)
				+ "&locale=" + enc(locale);
		return callWithRetries("POST", url, payload.toString(), readTimeoutMs, null);
	}

	/** The category was there already - for a create this is as good as a success. */
	public static boolean isAlreadyExists(KaptureApiResult r) {
		return r.httpStatus == 409 || (null != r.body && r.body.toLowerCase().contains("already exists"));
	}

	// ---- unpublish ---------------------------------------------------------

	/**
	 * Unpublish one document. Nothing else about the document is changed.
	 * The answer is HTTP 200 even when it failed - check KaptureApiResult.isOk().
	 */
	public KaptureApiResult unpublish(String contentId, String articleId, String locale) {
		JsonObject payload = new JsonObject();
		payload.addProperty("contentId", contentId);
		payload.addProperty("articleId", articleId);
		payload.addProperty("primaryLocale", locale);
		payload.addProperty("unpublishMode", require("kapture.api.unpublish.mode"));
		payload.addProperty("modifiedBy", userDisplayName);
		return callWithRetries("POST", baseUrl + unpublishPath, payload.toString(), readTimeoutMs, null);
	}

	// ---- publish -----------------------------------------------------------

	/**
	 * Publish the latest (draft) version of documents of ONE channel and ONE locale - the bulk
	 * action endpoint, nothing of the content is sent again.
	 *
	 * @param documents JSON array of {id, contentId, articleId, type, primaryLocale, articleState,
	 *                  articleVersion, publishedVersion, title, articleOwner, articleCreator,
	 *                  modifiedBy, createdBy}
	 *
	 * The answer is per document (results[].success) and a success there is still read back from
	 * k_article by the caller.
	 */
	public KaptureApiResult publishDocuments(String contentId, com.google.gson.JsonArray documents, String locale,
			String correlationId) {
		JsonObject payload = new JsonObject();
		payload.addProperty("action", require("kapture.api.publish.action"));
		payload.addProperty("contentId", contentId);
		payload.add("documents", documents);
		JsonObject options = new JsonObject();
		com.google.gson.JsonArray locales = new com.google.gson.JsonArray();
		locales.add(new com.google.gson.JsonPrimitive(locale));
		options.add("locales", locales);
		options.addProperty("bypassWorkflowAndPublish", true);
		payload.add("options", options);
		return callWithRetries("POST", baseUrl + require("kapture.api.publish.path"), payload.toString(), bulkReadTimeoutMs,
				correlationId);
	}

	// ---- attachment upload -------------------------------------------------

	/** One file of a temp-attachment upload request. */
	public static class UploadFile {
		public final File file;
		public final String clientAttachmentId;
		/** may be null/blank for a document that does not exist in Kapture yet */
		public final String documentId;
		public final String locale;

		public UploadFile(File file, String clientAttachmentId, String documentId, String locale) {
			this.file = file;
			this.clientAttachmentId = clientAttachmentId;
			this.documentId = documentId;
			this.locale = locale;
		}
	}

	/** Upload ONE file - uploadTempAttachments() with a single file. */
	public KaptureApiResult uploadTempAttachment(File file, String clientAttachmentId, String documentId,
			String locale) {
		java.util.List<UploadFile> one = new java.util.ArrayList<UploadFile>();
		one.add(new UploadFile(file, clientAttachmentId, documentId, locale));
		return uploadTempAttachments(one, true);
	}

	/**
	 * Upload SEVERAL files - of one or several documents - in ONE request to the temp-attachment
	 * endpoint. Each file carries its own document identity (attachments[i].legacyDocumentId,
	 * .locale) and our own clientAttachmentId, by which the answer (a JSON array) is matched back.
	 *
	 * @param retryTransient false while the caller can still SPLIT the request: a body refused for
	 *        its size fails the same way however often it is re-sent (bulk content import rule).
	 */
	public KaptureApiResult uploadTempAttachments(java.util.List<UploadFile> files, boolean retryTransient) {
		String url = baseUrl + attachmentUploadPath;
		KaptureApiResult tokenErr = ensureToken();
		if (null != tokenErr) {
			return tokenErr;
		}
		KaptureApiResult last = sendMultipart(url, files);
		if (last.httpStatus == 401) {
			KaptureApiResult lr = login();
			if (!lr.isHttpOk()) {
				return lr;
			}
			last = sendMultipart(url, files);
		}
		int retries = 0;
		while (retryTransient && isRetryable(last) && retries < retryMax) {
			retries++;
			long delay = backoffMs(retries, last.retryAfterSeconds);
			logger.info("uploadTempAttachments :: transient " + last.describe() + " - retry " + retries + "/"
					+ retryMax + " after " + delay + "ms files=" + files.size());
			if (!sleepMs(delay)) {
				break;
			}
			last = sendMultipart(url, files);
		}
		return last;
	}

	private KaptureApiResult sendMultipart(String url, java.util.List<UploadFile> files) {
		KaptureApiResult out = new KaptureApiResult();
		long started = System.currentTimeMillis();
		HttpURLConnection con = null;
		String boundary = "----DMTKapture" + Long.toHexString(System.nanoTime());
		byte[] dashB = ("--" + boundary).getBytes(UTF8);
		byte[] crlf = new byte[] { 13, 10 };
		long totalFileBytes = 0;
		StringBuilder names = new StringBuilder();
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("POST");
			con.setConnectTimeout(connectTimeoutMs);
			con.setReadTimeout(bulkReadTimeoutMs);
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			con.setRequestProperty("Accept", "application/json");
			if (null != token) {
				con.setRequestProperty("Authorization", "Bearer " + token);
			}

			// the body as literal chunks and file placeholders: files are streamed from disk, never held
			java.util.List<Object> body = new java.util.ArrayList<Object>();
			for (int i = 0; i < files.size(); i++) {
				UploadFile f = files.get(i);
				ByteArrayOutputStream head = new ByteArrayOutputStream();
				head.write(dashB);
				head.write(crlf);
				head.write(("Content-Disposition: form-data; name=" + (char) 34 + "attachments[" + i + "].file" + (char) 34
						+ "; filename=" + (char) 34 + f.file.getName() + (char) 34).getBytes(UTF8));
				head.write(crlf);
				head.write("Content-Type: application/octet-stream".getBytes(UTF8));
				head.write(crlf);
				head.write(crlf);
				body.add(head.toByteArray());
				body.add(f.file);
				// articleVersion is deliberately NOT sent - Kapture fails the document if it is.
				ByteArrayOutputStream fields = new ByteArrayOutputStream();
				fields.write(crlf);
				String p = "attachments[" + i + "].";
				writeField(fields, dashB, crlf, p + "clientAttachmentId", f.clientAttachmentId);
				if (null != f.documentId && !"".equals(f.documentId.trim())) {
					writeField(fields, dashB, crlf, p + "legacyDocumentId", f.documentId.trim());
				}
				writeField(fields, dashB, crlf, p + "locale", f.locale);
				body.add(fields.toByteArray());
				totalFileBytes += f.file.length();
				names.append(names.length() > 0 ? ", " : "").append(f.file.getName()).append(" [")
						.append(f.clientAttachmentId).append(null == f.documentId ? "" : " " + f.documentId).append("]");
			}
			// uploadedBy MUST be the session user's id, or the article save fails with
			// "Temp attachment ownership mismatch".
			ByteArrayOutputStream tail = new ByteArrayOutputStream();
			writeField(tail, dashB, crlf, "uploadedBy", userPkId);
			tail.write(dashB);
			tail.write("--".getBytes(UTF8));
			tail.write(crlf);
			body.add(tail.toByteArray());

			long total = 0;
			for (Object o : body) {
				total += (o instanceof byte[]) ? ((byte[]) o).length : ((File) o).length();
			}
			con.setFixedLengthStreamingMode(total);

			OutputStream os = null;
			try {
				os = con.getOutputStream();
				byte[] buf = new byte[65536];
				for (Object o : body) {
					if (o instanceof byte[]) {
						os.write((byte[]) o);
						continue;
					}
					File file = (File) o;
					long fileLength = file.length();
					InputStream is = PathUtil.fileInputStream(file);
					long written = 0;
					try {
						int n;
						while ((n = is.read(buf)) > 0) {
							os.write(buf, 0, n);
							written += n;
						}
					} finally {
						try { is.close(); } catch (IOException ignore) { }
					}
					if (written != fileLength) {
						throw new IOException("attachment " + file.getName() + " changed size while uploading");
					}
				}
				os.flush();
			} finally {
				if (null != os) {
					try { os.close(); } catch (IOException ignore) { }
				}
			}

			out.httpStatus = con.getResponseCode();
			out.retryAfterSeconds = parseRetryAfter(con.getHeaderField("Retry-After"));
			out.body = readBody(out.httpStatus < 400 ? con.getInputStream() : con.getErrorStream());
			out.message = messageOf(out.body);
		} catch (SocketTimeoutException ste) {
			out.httpStatus = 0;
			out.timedOut = true;
			out.message = "SocketTimeoutException: " + ste.getMessage();
		} catch (IOException e) {
			out.httpStatus = 0;
			out.message = e.getClass().getSimpleName() + ": " + e.getMessage();
			Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "sendMultipart(" + url + ")", e);
		} finally {
			if (null != con) {
				con.disconnect();
			}
		}
		logger.info("KAPTURE CALL :: POST " + url + " :: http=" + out.httpStatus + (out.timedOut ? " (TIMED OUT)" : "")
				+ " :: " + (System.currentTimeMillis() - started) + " ms :: FILES=" + files.size() + " (" + totalFileBytes
				+ " bytes) " + cut(names.toString()) + " :: ANSWER=" + (0 == out.httpStatus ? out.message : cut(out.body)));
		return out;
	}

	private static void writeField(ByteArrayOutputStream b, byte[] dashB, byte[] crlf, String name, String value)
			throws IOException {
		b.write(dashB);
		b.write(crlf);
		b.write(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes(UTF8));
		b.write(crlf);
		b.write(crlf);
		b.write((null == value ? "" : value).getBytes(UTF8));
		b.write(crlf);
	}

	// ---- retry / token -----------------------------------------------------

	/**
	 * Request-level retry only: a 429, a 5xx or no answer means the WHOLE REQUEST bounced.
	 * A failure reported for a document INSIDE a 200 answer is never retried here.
	 */
	private KaptureApiResult callWithRetries(String method, String url, String body, int timeoutMs,
			String correlationId) {
		KaptureApiResult last = callOnceWithTokenRetry(method, url, body, timeoutMs, correlationId);
		int retries = 0;
		while (isRetryable(last) && retries < retryMax) {
			retries++;
			long delay = backoffMs(retries, last.retryAfterSeconds);
			logger.info("callWithRetries :: transient " + last.describe() + " - retry " + retries + "/" + retryMax
					+ " after " + delay + "ms url=" + url);
			if (!sleepMs(delay)) {
				break;
			}
			last = callOnceWithTokenRetry(method, url, body, timeoutMs, correlationId);
		}
		return last;
	}

	private boolean isRetryable(KaptureApiResult r) {
		if (null == r || r.timedOut) {
			// a timed-out request may still be running on the server - never resend it
			return false;
		}
		return r.httpStatus == 429 || (r.httpStatus >= 500 && r.httpStatus <= 599) || r.httpStatus == 0;
	}

	private long backoffMs(int retryNum, int retryAfterSeconds) {
		long delay = retryAfterSeconds > 0 ? retryAfterSeconds * 1000L : retryBaseDelayMs * (1L << (retryNum - 1));
		return delay > retryMaxDelayMs ? retryMaxDelayMs : delay;
	}

	private static boolean sleepMs(long ms) {
		try {
			Thread.sleep(ms);
			return true;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private KaptureApiResult callOnceWithTokenRetry(String method, String url, String body, int timeoutMs,
			String correlationId) {
		KaptureApiResult tokenErr = ensureToken();
		if (null != tokenErr) {
			return tokenErr;
		}
		KaptureApiResult last = send(method, url, body, true, timeoutMs, correlationId);
		int attempts = 0;
		while (last.httpStatus == 401 && attempts < tokenRetryMax) {
			attempts++;
			logger.info("callOnceWithTokenRetry :: 401 - logging in again (" + attempts + "/" + tokenRetryMax + ")");
			KaptureApiResult lr = login();
			if (!lr.isHttpOk()) {
				return lr;
			}
			last = send(method, url, body, true, timeoutMs, correlationId);
		}
		return last;
	}

	private KaptureApiResult ensureToken() {
		if (null != token) {
			return null;
		}
		KaptureApiResult lr = login();
		return lr.isHttpOk() ? null : lr;
	}

	// ---- HTTP --------------------------------------------------------------

	private KaptureApiResult send(String method, String url, String jsonBody, boolean withAuth, int timeoutMs,
			String correlationId) {
		// a job that was asked to abort ends here instead of making another call
		com.mazda.gms3.dmt.utils.ThreadAbortUtil.checkpoint();
		KaptureApiResult out = new KaptureApiResult();
		out.requestBody = jsonBody;
		long started = System.currentTimeMillis();
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod(method);
			con.setConnectTimeout(connectTimeoutMs);
			con.setReadTimeout(timeoutMs);
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Accept", "application/json");
			if (withAuth && null != token) {
				con.setRequestProperty("Authorization", "Bearer " + token);
			}
			if (null != correlationId && !"".equals(correlationId)) {
				con.setRequestProperty("X-Correlation-ID", correlationId);
			}
			if (null != jsonBody) {
				con.setDoOutput(true);
				byte[] payload = jsonBody.getBytes(UTF8);
				OutputStream os = null;
				try {
					os = con.getOutputStream();
					os.write(payload);
					os.flush();
				} finally {
					if (null != os) {
						try { os.close(); } catch (IOException ignore) { }
					}
				}
			}
			out.httpStatus = con.getResponseCode();
			out.retryAfterSeconds = parseRetryAfter(con.getHeaderField("Retry-After"));
			out.body = readBody(out.httpStatus < 400 ? con.getInputStream() : con.getErrorStream());
			out.message = messageOf(out.body);
		} catch (SocketTimeoutException ste) {
			out.httpStatus = 0;
			out.timedOut = true;
			out.message = "SocketTimeoutException: " + ste.getMessage();
			logger.error("send :: READ TIMEOUT after " + timeoutMs + "ms - the server may still be processing"
					+ " this request; it will NOT be sent again. url=" + url);
		} catch (IOException ioe) {
			out.httpStatus = 0;
			out.message = ioe.getClass().getSimpleName() + ": " + ioe.getMessage();
			Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "send(" + url + ")", ioe);
		} finally {
			if (null != con) {
				con.disconnect();
			}
		}
		trace(method, url, jsonBody, out, started, correlationId);
		return out;
	}

	/**
	 * ONE LOG LINE FOR EVERY CALL MADE TO KAPTURE: what was called, the HTTP status, how long it
	 * took and the answer. When the call failed the request that was sent is logged too, so the
	 * log alone shows what Kapture was given and what it said.
	 *
	 * The login call is never logged with its request or answer - they carry the password and
	 * the session token. kapture.api.log.payload=Y logs the request of every call, not only of
	 * the failed ones; every text is cut at kapture.api.log.max.chars.
	 */
	private void trace(String method, String url, String request, KaptureApiResult r, long started, String correlationId) {
		try {
			boolean login = url.endsWith(loginPath);
			StringBuilder line = new StringBuilder("KAPTURE CALL :: " + method + " " + url + " :: http=" + r.httpStatus
					+ (r.timedOut ? " (TIMED OUT)" : "") + " :: " + (System.currentTimeMillis() - started) + " ms"
					+ " :: sent " + (null == request ? 0 : request.length()) + " chars, received "
					+ (null == r.body ? 0 : r.body.length()) + " chars"
					+ (null == correlationId || "".equals(correlationId) ? "" : " :: ref=" + correlationId));
			if (!login) {
				boolean failed = !r.isOk();
				line.append(" :: ANSWER=").append(0 == r.httpStatus ? r.message : cut(r.body));
				if (null != request && (failed || "Y".equalsIgnoreCase(require("kapture.api.log.payload")))) {
					line.append(" :: REQUEST=").append(cut(request));
				}
			}
			logger.info(line.toString());
		} catch (Exception e) {
			// logging must never break the call
		}
	}

	/** A text shortened for the log. */
	public static String cut(String text) {
		if (null == text) {
			return "";
		}
		int max = requireInt("kapture.api.log.max.chars");
		String oneLine = text.replace((char) 13, ' ').replace((char) 10, ' ');
		return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + " ...(" + oneLine.length() + " chars in all)";
	}

	private static String readBody(InputStream is) throws IOException {
		if (null == is) {
			return "";
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF8));
		try {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append('\n');
			}
			return sb.toString();
		} finally {
			try { br.close(); } catch (IOException ignore) { }
		}
	}

	/** Top-level "message" of a JSON object body, "" when there is none. */
	private static String messageOf(String body) {
		try {
			JsonElement root = new JsonParser().parse(body);
			if (null != root && root.isJsonObject()) {
				String m = str(root.getAsJsonObject(), "message");
				return null == m ? "" : m;
			}
		} catch (Exception e) {
			// not JSON
		}
		return "";
	}

	/** First non-blank primitive value of the key anywhere in the tree (depth first). */
	static String findAnywhere(JsonElement el, String key) {
		if (null == el || el.isJsonNull()) {
			return null;
		}
		if (el.isJsonObject()) {
			JsonObject obj = el.getAsJsonObject();
			String v = str(obj, key);
			if (null != v && !"".equals(v.trim())) {
				return v.trim();
			}
			for (java.util.Map.Entry<String, JsonElement> e : obj.entrySet()) {
				String found = findAnywhere(e.getValue(), key);
				if (null != found) {
					return found;
				}
			}
		} else if (el.isJsonArray()) {
			for (JsonElement item : el.getAsJsonArray()) {
				String found = findAnywhere(item, key);
				if (null != found) {
					return found;
				}
			}
		}
		return null;
	}

	/** Field names of the answer (paths only, never values - the answer carries the token). */
	private static String fieldNames(JsonElement el) {
		StringBuilder sb = new StringBuilder();
		collectFieldNames(el, "", sb);
		return sb.toString();
	}

	private static void collectFieldNames(JsonElement el, String path, StringBuilder sb) {
		if (null == el || !el.isJsonObject()) {
			return;
		}
		for (java.util.Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
			String p = "".equals(path) ? e.getKey() : path + "." + e.getKey();
			sb.append(sb.length() > 0 ? ", " : "").append(p);
			collectFieldNames(e.getValue(), p, sb);
		}
	}

	private static JsonElement root(String body) {
		try {
			return new JsonParser().parse(body);
		} catch (Exception e) {
			return null;
		}
	}

	static String str(JsonObject obj, String key) {
		if (null == obj || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
			return null;
		}
		return obj.get(key).getAsString();
	}

	private static String enc(String s) {
		try {
			return URLEncoder.encode(null == s ? "" : s, "UTF-8");
		} catch (Exception e) {
			return null == s ? "" : s;
		}
	}

	private static int parseRetryAfter(String v) {
		try {
			int secs = Integer.parseInt(null == v ? "" : v.trim());
			return secs > 0 ? secs : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
