package com.mazda.gms3.mdm.sidataload.kapture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.kapture.KaptureApiResult;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.vo.SIKaptureDocumentDetails;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * THE KAPTURE SIDE OF THE SI DATA LOAD SCREEN - the replacement for the IQServiceClient calls the
 * screen and its schedule threads used to make.
 *
 * This class covers the READ half: given a document id and a locale, decide whether that document
 * exists, and if so whether the version for that locale is the MASTER IDENTIFIER or a TRANSLATION,
 * and whether it is published. Everything the caller needs is decided from the ONE latest-article
 * response - no k_article query is involved.
 */
public class SIChannelKaptureService {

	static Logger logger = LogManager.getLogger(SIChannelKaptureService.class);

	/*
	 * THE LOCALE KEY IS NOT THE SAME IN BOTH SHAPES OF THE RESPONSE - verified against the live API
	 * on SI1123 and SI1125:
	 *
	 *   a MASTER      carries "translatedLocale", equal to "primaryLocale"
	 *                 (SI1123/en_US -> primary en_US, translatedLocale en_US)
	 *   a TRANSLATION carries "translateLocale" - no "d" - and primaryLocale stays the MASTER'S
	 *                 (SI1125/en_EU -> primary en_UK, translateLocale en_EU)
	 *
	 * Both are read and whichever is present wins, so neither shape can be misread as the other.
	 * Getting this wrong picks the wrong WRITE endpoint, which does not fail loudly - it collapses
	 * a translation onto its master's locale.
	 */
	private static final String F_TRANSLATED_LOCALE = "translatedLocale";
	private static final String F_TRANSLATE_LOCALE = "translateLocale";
	private static final String F_PRIMARY_LOCALE = "primaryLocale";
	private static final String F_ARTICLE_STATE = "articleState";
	private static final String F_ARTICLE_VERSION = "articleVersion";
	private static final String F_PUBLISHED_VERSION = "publishedVersion";
	private static final String F_ARTICLE_ID = "articleId";
	private static final String F_TITLE = "title";
	private static final String F_ROW_ID = "id";

	/**
	 * READS ONE SI DOCUMENT IN ONE LOCALE.
	 *
	 * Replaces getLatestContentRecordByDocumentIDAndLocale(). Never returns null; the caller reads
	 * isReadFailed() and isFound() to tell "could not ask" from "asked, and it is not there".
	 *
	 * @param documentId e.g. SI1123
	 * @param locale     the locale being worked on - master or translation, this resolves either
	 * @param context    a short label for the log, saying which operation is asking
	 */
	public static SIKaptureDocumentDetails getDocument(String documentId, String locale,
			String context) {
		SIKaptureDocumentDetails details = new SIKaptureDocumentDetails();
		details.setDocumentId(documentId);
		try {
			if (null == documentId || "".equals(documentId.trim()) || null == locale
					|| "".equals(locale.trim())) {
				details.setReadFailed(true);
				details.setErrorMessage("Document Id and Locale are both required to read a document from Kapture.");
				return details;
			}

			/*
			 * THE CHANNEL, NOT THE DOCUMENT. Read from k_content_type and cached; on the New
			 * Document path there is no document to take it from, so it always comes from there.
			 */
			String contentId = FetchKaptureDataDAO.getSIChannelContentId();
			if (null == contentId || "".equals(contentId)) {
				details.setReadFailed(true);
				details.setErrorMessage("The Service Information channel could not be identified in Kapture.");
				logger.info("getDocument :: NO CONTENT ID for the SERVICE_INFORMATION channel - {"
						+ documentId + "} / {" + locale + "} cannot be read.");
				return details;
			}

			String apiLocale = locale.replace("-", "_").trim();
			KaptureApiClient client = new KaptureApiClient();
			KaptureApiResult read = client.latestArticle(documentId.trim(), contentId, apiLocale,
					context);

			/*
			 * NOT FOUND IS A 200 WITH AN EMPTY BODY, not a 404 - true both for an unknown document
			 * id and for a locale the document has no version in. Testing the status alone would
			 * report a missing document as a successful read of nothing.
			 */
			if (read.isOk() && (null == read.body || "".equals(read.body.trim()))) {
				logger.info("getDocument :: {" + documentId + "} has no version in locale {"
						+ apiLocale + "}.");
				details.setFound(false);
				return details;
			}
			if (!read.isOk() || null == read.body || "".equals(read.body.trim())) {
				details.setReadFailed(true);
				details.setErrorMessage(null == read.message ? "The document could not be read from Kapture."
						: read.message);
				logger.info("getDocument :: READ FAILED for {" + documentId + "} / {" + apiLocale
						+ "} :: " + read.message);
				return details;
			}

			JsonElement parsed = new JsonParser().parse(read.body);
			if (null == parsed || !parsed.isJsonObject()) {
				details.setReadFailed(true);
				details.setErrorMessage("Kapture returned a response that could not be read as a document.");
				logger.info("getDocument :: UNREADABLE RESPONSE for {" + documentId + "} / {"
						+ apiLocale + "}.");
				return details;
			}
			JsonObject article = parsed.getAsJsonObject();

			/*
			 * A BODY THAT PARSES BUT NAMES NO DOCUMENT IS NOT A DOCUMENT. Guards against an error
			 * wrapper being mistaken for a successful read and, on the New Document path, against
			 * treating a real document as absent.
			 */
			String returnedId = stringOf(article, F_ARTICLE_ID);
			if ("".equals(returnedId)) {
				logger.info("getDocument :: response for {" + documentId + "} / {" + apiLocale
						+ "} carries no articleId - treating as not found.");
				details.setFound(false);
				return details;
			}

			details.setFound(true);
			details.setArticle(article);
			details.setDocumentId(returnedId);
			details.setPrimaryLocale(stringOf(article, F_PRIMARY_LOCALE));
			details.setTitle(stringOf(article, F_TITLE));
			details.setArticleVersion(stringOf(article, F_ARTICLE_VERSION));
			details.setPublishedVersion(stringOf(article, F_PUBLISHED_VERSION));
			details.setArticleRowId(stringOf(article, F_ROW_ID));

			/*
			 * articleState IS ALLOWED TO BE ABSENT - see the VO. Left null rather than defaulted to
			 * a string, so "Kapture told us nothing" stays distinguishable in the log from
			 * "Kapture said Unpublished".
			 */
			String state = stringOf(article, F_ARTICLE_STATE);
			details.setArticleState("".equals(state) ? null : state);

			/*
			 * WHICH LOCALE THIS VERSION IS. translateLocale (a translation) wins over
			 * translatedLocale (a master); if neither is present the row can only be the master, so
			 * the primary locale stands in.
			 */
			String effective = stringOf(article, F_TRANSLATE_LOCALE);
			if ("".equals(effective)) {
				effective = stringOf(article, F_TRANSLATED_LOCALE);
			}
			if ("".equals(effective)) {
				effective = details.getPrimaryLocale();
			}
			details.setEffectiveLocale(effective);

			/*
			 * MASTER OR TRANSLATION, decided on the VALUES rather than on which key carried them,
			 * so a change in the response shape cannot flip it. Equal locales - or a primary we
			 * could not read - mean the master identifier.
			 */
			String primary = null == details.getPrimaryLocale() ? "" : details.getPrimaryLocale().trim();
			String actual = null == effective ? "" : effective.trim();
			details.setTranslation(!"".equals(primary) && !"".equals(actual)
					&& !primary.equalsIgnoreCase(actual));

			logger.info("getDocument :: " + details.describe());
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(SIChannelKaptureService.class.getName(), "getDocument()", e);
			details.setFound(false);
			details.setReadFailed(true);
			details.setErrorMessage(String.valueOf(e.getMessage()));
		}
		return details;
	}

	/**
	 * WHAT KAPTURE SAYS THE DOCUMENT IS AFTER A WRITE.
	 *
	 * All three write endpoints - add-article-tab, update-article-tab and save-translate-article -
	 * answer with the same articleSaveResponse block, so one reader serves the lot.
	 *
	 * THE CREATE PATH LEARNS THE DOCUMENT ID HERE AND NOWHERE ELSE. Kapture allocates it from
	 * k_content_type, so on a New Document this response is the first and only place the id exists.
	 *
	 * The VO is reused rather than duplicated: found = "the write produced a document", and
	 * readFailed = "the call itself did not succeed", which is exactly what a caller has to
	 * distinguish here too.
	 */
	public static SIKaptureDocumentDetails readSaveResponse(KaptureApiResult result, String context) {
		SIKaptureDocumentDetails details = new SIKaptureDocumentDetails();
		try {
			if (null == result || !result.isOk()) {
				details.setReadFailed(true);
				details.setErrorMessage(null == result || null == result.message
						? "The write to Kapture did not succeed." : result.message);
				logger.info("readSaveResponse :: WRITE FAILED :: " + context + " :: "
						+ details.getErrorMessage());
				return details;
			}
			if (null == result.body || "".equals(result.body.trim())) {
				details.setReadFailed(true);
				details.setErrorMessage("Kapture accepted the call but returned no document details.");
				logger.info("readSaveResponse :: EMPTY BODY on a successful write :: " + context);
				return details;
			}

			JsonElement parsed = new JsonParser().parse(result.body);
			if (null == parsed || !parsed.isJsonObject()) {
				details.setReadFailed(true);
				details.setErrorMessage("Kapture returned a response that could not be read.");
				return details;
			}
			JsonObject body = parsed.getAsJsonObject();

			/*
			 * THE BLOCK IS BURIED, AND NOT ALWAYS AT THE SAME DEPTH.
			 *
			 * add-article-tab returns it as response.articleSaveResponse - the outer object carries
			 * only status / message / sectionKey - so a reader that looked at the top level alone
			 * found nothing and reported a FAILURE ON A DOCUMENT THAT HAD JUST BEEN CREATED. That is
			 * the worst shape a bug can take here: the caller retries, and Kapture mints another
			 * document every time.
			 *
			 * So the block is hunted for rather than assumed: top level first, then response, then a
			 * shallow walk of the remaining objects.
			 */
			JsonObject saved = findSaveBlock(body);
			if (null != saved) {
				/*
				 * SHAPE ONE - add-article-tab and update-article-tab, which describe the version
				 * they wrote in a nested articleSaveResponse block.
				 */
				String articleId = stringOf(saved, F_ARTICLE_ID);
				if ("".equals(articleId)) {
					details.setReadFailed(true);
					details.setErrorMessage("Kapture accepted the call but named no document.");
					logger.info("readSaveResponse :: NO articleId IN articleSaveResponse :: " + context);
					return details;
				}
				details.setFound(true);
				details.setArticle(saved);
				details.setDocumentId(articleId);
				details.setArticleRowId(stringOf(saved, F_ROW_ID));
				details.setArticleVersion(stringOf(saved, F_ARTICLE_VERSION));
				details.setPublishedVersion(stringOf(saved, F_PUBLISHED_VERSION));
				details.setPrimaryLocale(stringOf(saved, F_PRIMARY_LOCALE));
				String state = stringOf(saved, F_ARTICLE_STATE);
				details.setArticleState("".equals(state) ? null : state);
			} else {
				/*
				 * SHAPE TWO - save-translate-article, WHICH SENDS NO articleSaveResponse AT ALL.
				 *
				 * It reports through the envelope instead, under different names again:
				 *   sectionKey / response.createdArticleId  the document id
				 *   latestVersion                           the version just written
				 *   documentPrimaryID                       the new k_article id
				 *   locale                                  the locale it landed on
				 *
				 * There is no articleState here, which matches what the READ says about a
				 * translation - it frequently has none either.
				 *
				 * NOT AN EDGE CASE TO TOLERATE, A SHAPE TO SUPPORT: reading it as a failure means
				 * reporting Failure on a translation that was written correctly, and any caller
				 * that retries on failure would write it again.
				 */
				String articleId = stringOf(body, "sectionKey");
				if ("".equals(articleId)) {
					articleId = stringOf(responseBlock(body), "createdArticleId");
				}
				if ("".equals(articleId)) {
					details.setReadFailed(true);
					details.setErrorMessage("Kapture accepted the call but named no document.");
					logger.info("readSaveResponse :: NEITHER articleSaveResponse NOR sectionKey :: "
							+ context + " :: " + trimmed(result.body));
					return details;
				}
				details.setFound(true);
				details.setArticle(body);
				details.setDocumentId(articleId);
				details.setArticleRowId(stringOf(body, "documentPrimaryID"));
				details.setArticleVersion(stringOf(body, "latestVersion"));
				details.setArticleState(null);
				/*
				 * primaryLocale IS NOT IN THIS SHAPE, so it is left empty rather than guessed -
				 * describe() reports the locale without claiming master or translation.
				 */
			}

			/*
			 * THE LOCALE THE WRITE LANDED ON. Both shapes call it "locale" - a third name for it,
			 * after translatedLocale and translateLocale on the read - so all three are tried, on
			 * the block when there was one and on the envelope otherwise.
			 */
			JsonObject source = null == saved ? body : saved;
			String effective = stringOf(source, "locale");
			if ("".equals(effective)) {
				effective = stringOf(body, "locale");
			}
			if ("".equals(effective)) {
				effective = stringOf(source, F_TRANSLATE_LOCALE);
			}
			if ("".equals(effective)) {
				effective = stringOf(source, F_TRANSLATED_LOCALE);
			}
			if ("".equals(effective)) {
				effective = details.getPrimaryLocale();
			}
			details.setEffectiveLocale(effective);

			String primary = null == details.getPrimaryLocale() ? "" : details.getPrimaryLocale().trim();
			String actual = null == effective ? "" : effective.trim();
			details.setTranslation(!"".equals(primary) && !"".equals(actual)
					&& !primary.equalsIgnoreCase(actual));

			logger.info("readSaveResponse :: " + context + " :: " + details.describe());
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(SIChannelKaptureService.class.getName(),
					"readSaveResponse()", e);
			details.setFound(false);
			details.setReadFailed(true);
			details.setErrorMessage(String.valueOf(e.getMessage()));
		}
		return details;
	}

	/**
	 * FINDS THE articleSaveResponse BLOCK WHEREVER THE WRAPPER HAPPENS TO PUT IT.
	 *
	 * Verified shapes: add-article-tab nests it under "response"; the block itself always carries
	 * articleId, articleVersion and articleState. One level of nesting is walked, which covers every
	 * response seen and stops well short of trawling the whole document.
	 */
	/** The wrapper's "response" object, or null - both shapes carry one. */
	private static JsonObject responseBlock(JsonObject body) {
		if (null != body && body.has("response") && body.get("response").isJsonObject()) {
			return body.getAsJsonObject("response");
		}
		return null;
	}

	/** The body, cut down for a log line - only ever used when a write could not be understood. */
	private static String trimmed(String body) {
		if (null == body) {
			return "";
		}
		return body.length() <= 600 ? body : body.substring(0, 600) + "...";
	}

	private static JsonObject findSaveBlock(JsonObject body) {
		if (null == body) {
			return null;
		}
		if (body.has("articleSaveResponse") && body.get("articleSaveResponse").isJsonObject()) {
			return body.getAsJsonObject("articleSaveResponse");
		}
		for (java.util.Map.Entry<String, JsonElement> entry : body.entrySet()) {
			JsonElement value = entry.getValue();
			if (null == value || !value.isJsonObject()) {
				continue;
			}
			JsonObject child = value.getAsJsonObject();
			if (child.has("articleSaveResponse") && child.get("articleSaveResponse").isJsonObject()) {
				return child.getAsJsonObject("articleSaveResponse");
			}
		}
		return null;
	}

	/** A JSON string field, or "" - absent, null and JSON null all collapse to the same thing. */
	private static String stringOf(JsonObject object, String field) {
		try {
			if (null == object || !object.has(field)) {
				return "";
			}
			JsonElement element = object.get(field);
			if (null == element || element.isJsonNull()) {
				return "";
			}
			return element.getAsString().trim();
		} catch (Exception e) {
			return "";
		}
	}
}
