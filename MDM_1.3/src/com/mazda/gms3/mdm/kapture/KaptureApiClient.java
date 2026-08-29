package com.mazda.gms3.mdm.kapture;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * MINIMAL KAPTURE REST CLIENT - the replacement for the InQuira IQServiceClient used by
 * InfoManagerServiceImpl.
 *
 * Only what the SI VIN screens need is implemented: authenticate, read one article, write one
 * article back, and (once the endpoint is known) publish it again.
 *
 * WHY HttpURLConnection AND NOT AN HTTP LIBRARY: MDM ships no HTTP client jar, the calls are
 * simple, and the same approach is already proven in ContentImportJob (Job 9) against this very
 * API. The shape of login / Bearer / 401-retry below is deliberately the same as that job's, so
 * the two behave identically against the server.
 *
 * NOT THREAD SAFE - one instance per operation. The SI VIN screens create one per request.
 */
public class KaptureApiClient {

	static Logger logger = LogManager.getLogger(KaptureApiClient.class);

	private final String baseUrl;
	private final String loginPath;
	private final String latestPath;
	private final String translatePath;
	private final String updatePath;
	private final String createPath;
	private final String publishPath;
	private final String categoryCreatePath;
	private final String categoryUpdatePath;

	private final String email;
	private final String password;
	private final String appType;
	private final String publicIp;
	private final String userAgent;

	private final int connectTimeoutMs;
	private final int readTimeoutMs;

	private String token = null;

	/*
	 * THE LOGGED-IN USER, TAKEN FROM THE LOGIN RESPONSE. cms-check returns the profile
	 * alongside the token, so modifiedBy and userEmail on a publish need no extra call.
	 */
	private String userFirstName = null;

	private String userLastName = null;

	/*
	 * THE LOGGED-IN USER'S PRIMARY KEY, sent as articleCreatorUserID on both writes.
	 * cms-check returns it as response.userCmsRoles[0].userPkId - a UUID, the same form
	 * display-Article returns for the field.
	 */
	private String userPkId = null;

	/** Retry budget for category calls - small on purpose, these run inside a user request. */
	private static final int CATEGORY_RETRY_MAX = 2;
	private static final long CATEGORY_RETRY_DELAY_MS = 1000L;

	public KaptureApiClient() {
		this.baseUrl = trimSlash(prop("kapture.api.base", "https://mgssdev.kapturekm.com"));
		this.loginPath = prop("kapture.api.login.path", "/kauthor-api/cms-check");
		this.latestPath = prop("kapture.api.latest.path", "/kauthor-api/latest-article");
		this.translatePath = prop("kapture.api.translate.path",
				"/kauthor-api/save-translate-article");
		this.updatePath = prop("kapture.api.update.path", "/kauthor-api/update-article-tab");
		/*
		 * SAME ENDPOINT ContentImportJob (Job 9) CREATES WITH, and it is the only one that mints a
		 * document. Job 9 has driven every channel through it against this server, SERVICE
		 * INFORMATION included, so the path and the payload shape are already proven.
		 */
		this.createPath = prop("kapture.api.create.path", "/kauthor-api/add-article-tab");
		this.publishPath = prop("kapture.api.publish.path", "");
		/*
		 * SAME ENDPOINT NAMES AS IMCategoryImportJob (Job 4), BUT UNDER /kauthor-api.
		 *
		 * Job 4 called these under /Kapture-Author. That context NO LONGER EXISTS on the server -
		 * it now answers 404 for every path, cms-check included - so the paths below were
		 * re-confirmed against the live API rather than copied across. Both are properties so a
		 * VDI deployment can move them again if the context is renamed a second time.
		 */
		this.categoryCreatePath = prop("kapture.api.category.create.path",
				"/kauthor-api/add-categoryMgt");
		this.categoryUpdatePath = prop("kapture.api.category.update.path",
				"/kauthor-api/updateCategoryByCatRef");
		this.email = prop("kapture.api.email", "");
		this.password = prop("kapture.api.password", "");
		this.appType = prop("kapture.api.appType", "kauthor");
		this.publicIp = prop("kapture.api.publicIp", "");
		this.userAgent = prop("kapture.api.userAgent", "chrome");
		this.connectTimeoutMs = intProp("kapture.api.connect.timeout.ms", 30000);
		this.readTimeoutMs = intProp("kapture.api.read.timeout.ms", 120000);
	}

	public String getToken() {
		return token;
	}

	/** The account the token was issued for - publish sends it as userEmail. */
	public String getUserEmail() {
		return email;
	}

	/**
	 * "firstName lastName" of the logged-in account, for modifiedBy. Empty until
	 * login() has run; callers that need it should already have made a call.
	 */
	public String getUserPkId() {
		return null == userPkId ? "" : userPkId.trim();
	}

	public String getUserDisplayName() {
		String first = (null == userFirstName) ? "" : userFirstName.trim();
		String last = (null == userLastName) ? "" : userLastName.trim();
		return (first + " " + last).trim();
	}

	/**
	 * POST <base>/kauthor-api/cms-check.
	 *
	 * The token comes back nested under "response.userSessionToken". It is pulled out by a flat
	 * text search rather than by walking the tree, so a future change to the wrapper does not
	 * break login - the same approach Job 9 uses.
	 *
	 * @return true when a token was obtained
	 */
	public boolean login() {
		try {
			StringBuffer body = new StringBuffer();
			body.append("{");
			appendJsonString(body, "email", email).append(",");
			appendJsonString(body, "appType", appType).append(",");
			appendJsonString(body, "publicIp", publicIp).append(",");
			appendJsonString(body, "userAgent", userAgent).append(",");
			appendJsonString(body, "password", password);
			body.append("}");

			KaptureApiResult result = send("POST", baseUrl + loginPath, body.toString(), false);
			body = null;
			if (!result.isOk()) {
				logger.info("login :: FAILED http=" + result.httpStatus + " :: " + result.message);
				return false;
			}
			String extracted = extractJsonString(result.body, "userSessionToken");
			if (null == extracted || "".equals(extracted)) {
				logger.info("login :: response was 2xx but userSessionToken is missing.");
				return false;
			}
			this.token = extracted;
			/*
			 * NAME OF THE ACCOUNT THE TOKEN BELONGS TO. Same flat-text extraction as the
			 * token above - the first firstName / lastName in the response are the ones on
			 * userCmsRoles[0], i.e. the account that just logged in.
			 */
			this.userFirstName = extractJsonString(result.body, "firstName");
			this.userLastName = extractJsonString(result.body, "lastName");
			this.userPkId = extractJsonString(result.body, "userPkId");
			logger.info("login :: token acquired for {" + email + "} (len=" + token.length() + ")");
			return true;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "login()", e);
			return false;
		}
	}

	/**
	 * GET <base>/kauthor-api/latest-article/{articleId}/{contentId}/{locale}
	 *
	 * THE READ HALF OF THE MODIFY FLOW, FOR A MASTER AND A TRANSLATION ALIKE.
	 *
	 * REPLACED display-Article ON 2026-08-05. That endpoint had to be told the version AND the
	 * state, which meant computing both before we could read - and getting either wrong addressed
	 * a different row silently. This one resolves the latest version for the locale ITSELF, so
	 * there is nothing to get wrong.
	 *
	 * THE TWO RETURN THE SAME THING. Verified byte for byte (MD5) over seven cases - master
	 * unpublished, master published, translation unpublished and translation published - same
	 * fields, same category-entry keys, same values. So nothing downstream had to change.
	 *
	 * NOT FOUND IS HTTP 200 WITH AN EMPTY BODY, not a 404 - true for both an unknown article id
	 * and a locale the document does not exist in. Callers must test the body, not just the status.
	 *
	 * STILL NOT ENOUGH ON ITS OWN: the response has no k_article id, and returns articleVersion as
	 * a NUMBER (1.0 arrives as 1), so both still come from the table - see KaptureArticleIdentity.
	 *
	 * @param articleId e.g. "SI1123"
	 * @param contentId the CHANNEL content id (all SI documents share one)
	 * @param locale    the locale being worked on - master or translated, it resolves either
	 */
	public KaptureApiResult latestArticle(String articleId, String contentId, String locale,
			String context) {
		StringBuffer url = new StringBuffer();
		url.append(baseUrl).append(latestPath);
		url.append("/").append(encode(articleId));
		url.append("/").append(encode(contentId));
		url.append("/").append(encode(locale));
		logger.info("latestArticle :: GET " + articleId + " / " + locale + " :: " + nz(context));
		return sendWithAuth("GET", url.toString(), null);
	}

	/**
	 * POST <base>/kauthor-api/save-translate-article?contentId=...
	 *
	 * THE WRITE PATH FOR A TRANSLATION. update-article-tab HANDLES THE ORIGINAL ARTICLE ONLY -
	 * confirmed by the Kapture API team on 2026-08-05 - and will not populate
	 * article_translated_locale, so a translation pushed through it loses its locale and collapses
	 * onto the master's. Translated content has to come here.
	 *
	 * SAME NOTE AS updateArticle: for a PUBLISHED translation this leaves a new unpublished
	 * version behind, which publishArticle then promotes.
	 */
	public KaptureApiResult saveTranslateArticle(String contentId, String payloadJson,
			String context) {
		String url = baseUrl + translatePath + "?contentId=" + encode(contentId);
		logger.info("saveTranslateArticle :: POST contentId=" + contentId + " payload="
				+ (null == payloadJson ? 0 : payloadJson.length()) + " bytes :: " + nz(context));
		return sendWithAuth("POST", url, payloadJson);
	}

	/**
	 * PUT <base>/kauthor-api/update-article-tab?contentId=...
	 *
	 * NOTE FOR A PUBLISHED DOCUMENT: Kapture creates a NEW UNPUBLISHED version from this call.
	 * Re-publishing is a separate step - see publishArticle().
	 */
	public KaptureApiResult updateArticle(String contentId, String payloadJson, String context) {
		String url = baseUrl + updatePath + "?contentId=" + encode(contentId);
		logger.info("updateArticle :: PUT contentId=" + contentId + " payload="
				+ (null == payloadJson ? 0 : payloadJson.length()) + " bytes :: " + nz(context));
		return sendWithAuth("PUT", url, payloadJson);
	}

	/**
	 * POST <base>/kauthor-api/add-article-tab?contentId=...
	 *
	 * CREATES A NEW DOCUMENT AND RETURNS THE ID KAPTURE ASSIGNED IT. The replacement for
	 * createContent() on the InQuira client, which is what the SI Data Load screen calls on its
	 * New Document path.
	 *
	 * THE DOCUMENT ID IS NOT OURS TO CHOOSE. Kapture allocates it from k_content_type
	 * (documentid_prefix + documentid), so the caller learns it from the RESPONSE - there is
	 * nothing to send and nothing to predict.
	 *
	 * ALWAYS CREATED UNPUBLISHED, like every other path here: publishing is a separate call.
	 */
	public KaptureApiResult createArticle(String contentId, String payloadJson, String context) {
		String url = baseUrl + createPath + "?contentId=" + encode(contentId);
		logger.info("createArticle :: POST contentId=" + contentId + " payload="
				+ (null == payloadJson ? 0 : payloadJson.length()) + " bytes :: " + nz(context));
		return sendWithAuth("POST", url, payloadJson);
	}

	/**
	 * POST <base>/kauthor-api/publish-article, with a payload prepared by the caller.
	 *
	 * THE PAYLOAD IS BUILT OUTSIDE THIS CLASS, exactly as updateArticle() works, because it has
	 * to carry the document as display-Article returned it.
	 *
	 * WHY THAT MATTERS: publish-article REWRITES THE WHOLE ROW. Anything the request leaves out
	 * is stored as NULL, so the original 7-field request silently wiped article_title,
	 * article_owner, created_by and the rest on every version it created.
	 */
	public KaptureApiResult publishArticle(String articleId, String payload, String context) {
		if (null == publishPath || "".equals(publishPath.trim())) {
			KaptureApiResult result = new KaptureApiResult();
			result.httpStatus = 0;
			result.message = "publish endpoint not configured (kapture.api.publish.path)";
			logger.info("publishArticle :: SKIPPED for " + articleId + " - " + result.message);
			return result;
		}
		logger.info("publishArticle :: POST " + articleId + " :: " + nz(context));
		return sendWithAuth("POST", baseUrl + publishPath, payload);
	}

	// ------------------------------------------------------------------ categories

	/**
	 * POST <base>/kauthor-api/add-categoryMgt?locale=...
	 *
	 * REPLACES THE DIRECT INSERT INTO kapture_cms_db.k_categories. That schema is READ ONLY to
	 * this application, so every category write goes through the API - the same route
	 * IMCategoryImportJob (Job 4) took, with the identical payload field names, so the two paths
	 * cannot disagree about how a category row is created.
	 *
	 * A category that is already there comes back as 409 with a message of the form
	 * "Category reference key <ref> already exists." - VERIFIED against the live API. That means
	 * "the row is present", which is what the caller wanted. See isAlreadyExists().
	 *
	 * @param parentRefKey empty for a root category
	 */
	public KaptureApiResult createCategory(String locale, String categoryRefKey, String categoryName,
			String categoryDescription, String parentRefKey) {
		StringBuffer body = new StringBuffer();
		body.append("{");
		appendJsonString(body, "categoryRoleName", nz(categoryName)).append(",");
		appendJsonString(body, "categoryDescription", nz(categoryDescription)).append(",");
		appendJsonString(body, "categoryRoleRef", nz(categoryRefKey)).append(",");
		appendJsonString(body, "parentRefKey", nz(parentRefKey)).append(",");
		appendJsonString(body, "locale", nz(locale)).append(",");
		appendJsonString(body, "emailId", email);
		body.append("}");

		String url = baseUrl + categoryCreatePath + "?locale=" + encode(locale);
		logger.info("createCategory :: POST {" + categoryRefKey + "} parent {" + nz(parentRefKey)
				+ "} locale {" + locale + "}");
		KaptureApiResult result = sendCategoryCall(url, body.toString());
		body = null;
		return result;
	}

	/**
	 * POST <base>/kauthor-api/updateCategoryByCatRef?categoryRef=...&locale=...
	 *
	 * POST, NOT PUT - verified against the live API, which answers 200 "Updated Categories
	 * Successfully" to a POST and 500 to a PUT on the same URL.
	 *
	 * NOTE THE PARENT IS NOT SENT. The update endpoint carries no parentRefKey - Job 4 does not
	 * send one either - so a category cannot be RE-PARENTED through this call. That is not a
	 * limitation for the SI VIN screens: a VIN range always hangs off the same WMI/VDS chain it
	 * was created under, so only the name and description are ever refreshed.
	 *
	 * SIDE EFFECT WORTH KNOWING: the server rewrites the row rather than patching it, so
	 * k_categories.date_added is reset to the update time. Nothing in MDM reads that column, but
	 * it does mean "when was this category first created" is lost for any category this screen
	 * touches.
	 */
	public KaptureApiResult updateCategory(String locale, String categoryRefKey, String categoryName,
			String categoryDescription) {
		StringBuffer body = new StringBuffer();
		body.append("{");
		appendJsonString(body, "categoryRoleName", nz(categoryName)).append(",");
		appendJsonString(body, "categoryDescription", nz(categoryDescription)).append(",");
		appendJsonString(body, "locale", nz(locale)).append(",");
		appendJsonString(body, "emailId", email).append(",");
		appendJsonString(body, "categoryRoleRef", nz(categoryRefKey));
		body.append("}");

		String url = baseUrl + categoryUpdatePath + "?categoryRef=" + encode(categoryRefKey)
				+ "&locale=" + encode(locale);
		logger.info("updateCategory :: POST {" + categoryRefKey + "} locale {" + locale + "}");
		KaptureApiResult result = sendCategoryCall(url, body.toString());
		body = null;
		return result;
	}

	/**
	 * TRUE WHEN THE SERVER IS SAYING "THAT CATEGORY IS ALREADY THERE".
	 *
	 * Reported two different ways depending on the endpoint, so both are accepted - exactly the
	 * test Job 4 applies. Callers treat this as SUCCESS, because a category that already exists is
	 * the state they were trying to reach.
	 */
	public static boolean isAlreadyExists(KaptureApiResult result) {
		if (null == result) {
			return false;
		}
		if (409 == result.httpStatus) {
			return true;
		}
		String text = null == result.body ? "" : result.body.toLowerCase();
		return text.indexOf("already exists") >= 0;
	}

	/**
	 * One category call, with a short bounded retry for TRANSIENT conditions only - 429 (the API
	 * is rate limited at 100 calls/minute), 5xx, and transport failures.
	 *
	 * Deliberately far smaller than Job 4's retry budget: this runs inside a user request on a
	 * screen, so it must fail visibly in seconds rather than stall the page. A deterministic 4xx
	 * (400 bad data, 409 already-exists) is returned as-is and never retried.
	 */
	private KaptureApiResult sendCategoryCall(String url, String body) {
		KaptureApiResult result = sendWithAuth("POST", url, body);
		int attempt = 0;
		while (attempt < CATEGORY_RETRY_MAX && isTransient(result)) {
			attempt++;
			long delay = CATEGORY_RETRY_DELAY_MS * attempt;
			logger.info("sendCategoryCall :: transient http=" + result.httpStatus + " - waiting "
					+ delay + "ms then retry " + attempt + "/" + CATEGORY_RETRY_MAX);
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				break;
			}
			result = sendWithAuth("POST", url, body);
		}
		return result;
	}

	private static boolean isTransient(KaptureApiResult result) {
		if (null == result) {
			return false;
		}
		if (429 == result.httpStatus) {
			return true;
		}
		if (result.httpStatus >= 500 && result.httpStatus <= 599) {
			return true;
		}
		// 0 means the call never reached the server
		return 0 == result.httpStatus;
	}

	private static String nz(String value) {
		return null == value ? "" : value;
	}

	// ------------------------------------------------------------------ transport

	/** Adds the Bearer token, logging in first if needed and once more on a 401. */
	private KaptureApiResult sendWithAuth(String method, String url, String body) {
		if (null == token || "".equals(token)) {
			if (!login()) {
				KaptureApiResult failed = new KaptureApiResult();
				failed.httpStatus = 0;
				failed.message = "not authenticated";
				return failed;
			}
		}
		KaptureApiResult result = send(method, url, body, true);
		if (401 == result.httpStatus) {
			// TOKEN EXPIRED - one re-login and one retry, as Job 9 does
			logger.info("sendWithAuth :: 401 received, re-authenticating once.");
			token = null;
			if (login()) {
				result = send(method, url, body, true);
			}
		}
		return result;
	}

	private KaptureApiResult send(String method, String url, String body, boolean withAuth) {
		KaptureApiResult result = new KaptureApiResult();
		HttpURLConnection con = null;
		OutputStream os = null;
		InputStream is = null;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod(method);
			con.setConnectTimeout(connectTimeoutMs);
			con.setReadTimeout(readTimeoutMs);
			con.setRequestProperty("Accept", "application/json");
			if (withAuth && null != token) {
				con.setRequestProperty("Authorization", "Bearer " + token);
			}
			if (null != body) {
				byte[] payload = body.getBytes("UTF-8");
				con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				con.setRequestProperty("Content-Length", String.valueOf(payload.length));
				con.setDoOutput(true);
				os = con.getOutputStream();
				os.write(payload);
				os.flush();
				payload = null;
			}

			result.httpStatus = con.getResponseCode();
			is = (result.httpStatus >= 200 && result.httpStatus < 300)
					? con.getInputStream() : con.getErrorStream();
			result.body = readAll(is);
			if (!result.isOk()) {
				result.message = "http " + result.httpStatus + " :: "
						+ (null == result.body ? "" : trimForLog(result.body));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "send()", e);
			result.httpStatus = 0;
			result.message = String.valueOf(e.getMessage());
		} finally {
			closeQuietly(os);
			closeQuietly(is);
			if (null != con) {
				con.disconnect();
			}
		}
		return result;
	}

	private static String readAll(InputStream is) {
		if (null == is) {
			return "";
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int read = 0;
			while ((read = is.read(buf)) != -1) {
				bos.write(buf, 0, read);
			}
			return new String(bos.toByteArray(), "UTF-8");
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureApiClient.class.getName(), "readAll()", e);
			return "";
		}
	}

	/**
	 * Pulls "key":"value" out of a JSON document by text search.
	 *
	 * Used only for the session token, where the value is a JWT and cannot contain a quote or a
	 * backslash, so a flat search is safe and survives the wrapper object moving around.
	 */
	static String extractJsonString(String json, String key) {
		if (null == json || null == key) {
			return null;
		}
		String needle = "\"" + key + "\"";
		int k = json.indexOf(needle);
		if (k < 0) {
			return null;
		}
		int colon = json.indexOf(':', k + needle.length());
		if (colon < 0) {
			return null;
		}
		int open = json.indexOf('"', colon + 1);
		if (open < 0) {
			return null;
		}
		int close = json.indexOf('"', open + 1);
		if (close < 0) {
			return null;
		}
		return json.substring(open + 1, close);
	}

	private static StringBuffer appendJsonString(StringBuffer sb, String key, String value) {
		sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\"");
		return sb;
	}

	private static String escape(String value) {
		if (null == value) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '"' || c == '\\') {
				sb.append('\\').append(c);
			} else if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\r') {
				sb.append("\\r");
			} else if (c == '\t') {
				sb.append("\\t");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static String encode(String value) {
		try {
			return URLEncoder.encode(null == value ? "" : value, "UTF-8");
		} catch (Exception e) {
			return null == value ? "" : value;
		}
	}

	/**
	 * THE ERROR BODY IS LOGGED WHOLE - IT USED TO BE CUT AT 500 CHARACTERS.
	 *
	 * Kapture puts the generic line first and the USEFUL part last: a 400 opens with
	 * "Validation failed. Missing or invalid mandatory fields." followed by pages of null
	 * wrapper fields, and the validationErrors array naming the offending field sits past
	 * the 500th character. Cutting it left the log saying only that something was invalid,
	 * and finding out WHICH field cost a manual replay of the payload through Postman.
	 *
	 * ONLY FAILURES REACH HERE - see send(), which builds result.message under
	 * if(!result.isOk()). A successful response body is never logged, so this cannot turn
	 * into every article being written to the log file.
	 */
	private static String trimForLog(String value) {
		return null == value ? "" : value;
	}

	private static void closeQuietly(java.io.Closeable c) {
		if (null != c) {
			try {
				c.close();
			} catch (Exception ignore) {
				// nothing useful to do here
			}
		}
	}

	private static String prop(String key, String fallback) {
		String value = ApplicationProperties.getProperty(key);
		return (null == value || "".equals(value.trim())) ? fallback : value.trim();
	}

	private static int intProp(String key, int fallback) {
		try {
			return Integer.parseInt(prop(key, String.valueOf(fallback)));
		} catch (Exception e) {
			return fallback;
		}
	}

	private static String trimSlash(String value) {
		if (null == value) {
			return "";
		}
		String v = value.trim();
		while (v.endsWith("/")) {
			v = v.substring(0, v.length() - 1);
		}
		return v;
	}
}
