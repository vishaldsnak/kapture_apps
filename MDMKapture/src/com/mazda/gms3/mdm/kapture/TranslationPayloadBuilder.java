package com.mazda.gms3.mdm.kapture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;

/**
 * THE WRITE PAYLOADS FOR SI TRANSLATION UPDATE.
 *
 * THE GOVERNING RULE: THESE ENDPOINTS REWRITE THE WHOLE ROW. A field left out of the payload is
 * stored as NULL, so the payload is the READ HANDED BACK with only what changed replaced. That is
 * not politeness - dropping a field empties the column.
 *
 * WHICH IS WHY THIS BUILDER ECHOES AND SI DATA LOAD'S REBUILDS. A Word upload is the whole
 * document, so rebuilding its attribute node is correct there. A translation batch is 9 fields of
 * a 22-field document; rebuilding here would blank ATTACHMENTS, CAMPAIGN_NUMBER, TSB_NUMBER,
 * SIGNATURE and everything else the vendor never saw.
 *
 * WHAT THIS SCREEN ACTUALLY DECIDES is small: the channel node, the title that goes with it, the
 * row being written (id / version, which must come from k_article, never from the read), and who
 * made the change. Everything else is echoed.
 */
public class TranslationPayloadBuilder {

	static Logger logger = LogManager.getLogger(TranslationPayloadBuilder.class);

	/**
	 * FIELDS NEVER ECHOED BACK.
	 *
	 * The read carries bookkeeping that either belongs to the version being replaced or is the
	 * server's to set. Echoing them either addresses the wrong row or overwrites an audit value
	 * with a stale one. id / articleVersion are set explicitly from k_article; the rest are omitted.
	 */
	private static final String[] NOT_ECHOED = {
			"id", "articleVersion", "publishedVersion", "articleLatestState",
			"isUpdateArticle", "crawlType", "dateModified", "articlePublishDate" };

	/**
	 * THE UPDATE PAYLOAD - a new version of the document carrying the translated content.
	 *
	 * @param article     the latest-article read this update is based on
	 * @param identity    the k_article row being replaced - id and version come from HERE, because
	 *                    the read does not carry the row id and its version can lag
	 * @param rootNode    SERVICE_INFORMATION / SERVICE_MANUALS
	 * @param mergedNode  the channel node after the translated fields were merged in
	 * @param translation true when the row is a TRANSLATION - decided from master_locale, never
	 *                    from the primary/translated pair, which are identical once published
	 */
	public static String buildUpdate(JsonObject article, KaptureArticleIdentity identity,
			String documentId, String locale, String rootNode, JsonObject mergedNode,
			boolean translation, KaptureApiClient client) {
		JsonObject payload = echo(article);

		payload.addProperty("id", identity.getId());
		payload.addProperty("articleId", documentId);
		payload.addProperty("articleVersion", identity.getArticleVersion());

		/*
		 * THE LOCALE PAIR DIFFERS BY ENDPOINT, AND THE KEY IS SPELLED DIFFERENTLY.
		 * save-translate-article wants primaryLocale = the MASTER's locale and translateLocale
		 * (NO "d") = this locale. Echoing primaryLocale for a translation would send its own
		 * promoted locale where Kapture expects the master's.
		 */
		if (translation) {
			payload.addProperty("primaryLocale", defaulted(identity.getMasterLocale(), locale));
			payload.addProperty("translateLocale", locale);
			payload.remove("translatedLocale");
		} else {
			payload.addProperty("primaryLocale",
					defaulted(stringOf(article, "primaryLocale"), locale));
			payload.addProperty("translatedLocale",
					defaulted(stringOf(article, "translatedLocale"), locale));

			/*
			 * A PUBLISHED MASTER MUST DECLARE ITSELF ONE, OR THE UPDATE IS A 404.
			 *
			 * Without these two, update-article-tab answers HTTP 404 "Article not found for
			 * update." on a published master - it looks for a draft to version and there is none.
			 * Unpublished masters were unaffected, which is why this only showed up on live data.
			 *
			 * BOTH COME FROM k_article, NOT FROM THE READ - which is why they are in NOT_ECHOED
			 * rather than being carried through the echo. latest-article returns the version as a
			 * NUMBER, so an echoed 1.0 goes back as "1" and addresses the wrong version.
			 *
			 * pubFlag STAYS FALSE. The update leaves a new UNPUBLISHED version by design; the
			 * document is put back on the air by the publish call that follows, which is also what
			 * preserves the state of one that was NOT published.
			 */
			payload.addProperty("articleState", identity.getArticleState());
			payload.addProperty("pubFlag", Boolean.FALSE);
			if (identity.isPublished()) {
				payload.addProperty("isUpdateArticle", Boolean.TRUE);
				payload.addProperty("publishedVersion", identity.getPublishedVersion());
			}
		}

		// the translated title is the document's title
		String title = stringOf(mergedNode, "TITLE");
		if (!"".equals(title)) {
			payload.addProperty("title", title);
		}

		payload.addProperty("modifiedBy", defaulted(client.getUserDisplayName(),
				stringOf(article, "modifiedBy")));
		payload.addProperty("userEmail", client.getUserEmail());

		// THE CONTENT ITSELF - the whole node, translated fields included
		payload.remove(rootNode);
		payload.add(rootNode, mergedNode);

		normaliseLists(payload);
		normaliseDates(payload);
		return payload.toString();
	}

	/**
	 * THE publish-article PAYLOAD, for the version the update just created.
	 *
	 * @param published  the row the UPDATE produced, re-read from k_article - NOT the one read
	 *                   before the update, whose id and version belong to the previous version
	 * @param mergedNode the channel node the UPDATE WROTE. Publishing rewrites the row too, so the
	 *                   pre-update read cannot be the content here - it would revert the translation
	 */
	public static String buildPublish(JsonObject article, KaptureArticleIdentity published,
			String documentId, String locale, String contentId, String rootNode,
			JsonObject mergedNode, KaptureApiClient client) {
		JsonObject payload = echo(article);

		payload.addProperty("id", published.getId());
		payload.addProperty("contentId", contentId);
		payload.addProperty("articleId", documentId);
		payload.addProperty("articleVersion", published.getArticleVersion());
		payload.addProperty("articleState", published.getArticleState());
		payload.addProperty("articleLatestState", "Published");
		payload.addProperty("pubFlag", Boolean.TRUE);

		if (published.isTranslation()) {
			payload.addProperty("primaryLocale", defaulted(published.getMasterLocale(), locale));
			payload.addProperty("translateLocale", locale);
			payload.remove("translatedLocale");
		} else {
			payload.addProperty("primaryLocale",
					defaulted(stringOf(article, "primaryLocale"), locale));
			payload.addProperty("translatedLocale",
					defaulted(stringOf(article, "translatedLocale"), locale));
		}

		/*
		 * THE CONTENT PUBLISHED IS THE CONTENT THE UPDATE JUST WROTE.
		 *
		 * publish-article rewrites the whole row like every other Kapture write, so echoing the
		 * read taken BEFORE the update would publish the OLD channel node - putting the source
		 * text back on exactly the documents that were live. The merged node goes out instead.
		 *
		 * THE TITLE IS WRITTEN IN BOTH PLACES - inside the channel node (TITLE) and on the article
		 * itself (title) - so the translated title is what Kapture shows wherever it reads it from.
		 */
		if (null != rootNode && null != mergedNode) {
			payload.remove(rootNode);
			payload.add(rootNode, mergedNode);
			String title = stringOf(mergedNode, "TITLE");
			if (!"".equals(title)) {
				payload.addProperty("title", title);
			}
		}

		payload.addProperty("modifiedBy", defaulted(client.getUserDisplayName(),
				stringOf(article, "modifiedBy")));
		payload.addProperty("userEmail", client.getUserEmail());

		normaliseLists(payload);
		normaliseDates(payload);
		return payload.toString();
	}

	/**
	 * FIELDS THAT MUST BE SENT AS A LIST, WHATEVER THE READ GAVE BACK.
	 *
	 * latest-article returns these as a single OBJECT when the document has one entry and as an
	 * ARRAY when it has several. The write endpoints only accept a list, so echoing the read
	 * verbatim gets a one-entry document refused:
	 *   400 "Article User Group must be a valid list." / "Article List View must be a valid list."
	 * Found on live data - four of six test documents were rejected this way.
	 */
	private static final String[] ALWAYS_A_LIST = { "articlelistusergroup", "articlelistview",
			"articlelistcategory", "articleesicategory" };

	/** Wraps a lone object in an array; leaves an existing array, and an absent field, alone. */
	private static void normaliseLists(JsonObject payload) {
		for (int i = 0; i < ALWAYS_A_LIST.length; i++) {
			String field = ALWAYS_A_LIST[i];
			if (!payload.has(field) || payload.get(field).isJsonNull()) {
				continue;
			}
			JsonElement value = payload.get(field);
			if (value.isJsonArray()) {
				continue;
			}
			JsonArray array = new JsonArray();
			array.add(value);
			payload.add(field, array);
			logger.info("normaliseLists :: {" + field + "} came back as a single object - sent as a list.");
		}
	}

	/**
	 * DATE FIELDS THE WRITE SIDE PARSES, AND THE FORMAT IT PARSES THEM IN.
	 *
	 * latest-article RETURNS DATES IN A FORMAT THE WRITE ENDPOINTS DO NOT ACCEPT - it answers
	 * MM/dd/yyyy HH:mm:ss and they want yyyy-MM-dd HH:mm:ss. Echoing the read verbatim therefore
	 * cannot be right for these two fields, which is the one exception to this class's rule.
	 *
	 * FOUND ON A PUBLISHED DOCUMENT. update-article-tab tolerates the read's format, publish-article
	 * does not - it answered HTTP 200 carrying {"status":"500", "message":"... Unparseable date:
	 * \"09/03/2026 00:00:00\""} and left the document unpublished. So the symptom was that
	 * PUBLISHED documents silently stopped being published, and only those.
	 */
	private static final String[] DATE_FIELDS = { "expireDate", "reviewDate" };

	private static final String WRITE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/** Read leniently - the format the read uses first, then the one the write side wants. */
	private static final String[] READ_DATE_FORMATS = {
			"MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };

	/**
	 * Puts every date the payload carries into the format the write side parses.
	 *
	 * A BLANK DATE GOES AS JSON null, NOT AS "". An empty string is a value: it would overwrite a
	 * date somebody set between the read and this write, and it is not parseable either.
	 *
	 * A value in no recognised format is SENT AS IT WAS. Guessing at it could move a date by a
	 * month - dd/MM and MM/dd are indistinguishable for the first twelve days - and a refusal that
	 * names the field is a better outcome than a document quietly expiring on the wrong day.
	 */
	private static void normaliseDates(JsonObject payload) {
		for (int i = 0; i < DATE_FIELDS.length; i++) {
			String field = DATE_FIELDS[i];
			if (!payload.has(field)) {
				continue;
			}
			String value = stringOf(payload, field);
			if ("".equals(value.trim())) {
				payload.add(field, com.google.gson.JsonNull.INSTANCE);
				continue;
			}
			String written = toWriteFormat(value.trim());
			if (null == written) {
				logger.info("normaliseDates :: {" + field + "} = {" + value
						+ "} is in no recognised format - sent unchanged.");
				continue;
			}
			payload.addProperty(field, written);
		}
	}

	/** The value in yyyy-MM-dd HH:mm:ss, or null if it is in none of the formats we know. */
	private static String toWriteFormat(String value) {
		for (int i = 0; i < READ_DATE_FORMATS.length; i++) {
			try {
				SimpleDateFormat reader = new SimpleDateFormat(READ_DATE_FORMATS[i]);
				// STRICT - lenient parsing turns 13/01/2026 into January 2027 rather than failing
				reader.setLenient(false);
				Date parsed = reader.parse(value);
				if (null == parsed) {
					continue;
				}
				return new SimpleDateFormat(WRITE_DATE_FORMAT).format(parsed);
			} catch (Exception ignore) {
				// try the next format
			}
		}
		return null;
	}

	/** The read, copied, minus the bookkeeping that must not travel back. */
	private static JsonObject echo(JsonObject article) {
		JsonObject payload = new JsonObject();
		if (null == article) {
			return payload;
		}
		Iterator<Map.Entry<String, JsonElement>> it = article.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JsonElement> entry = it.next();
			if (!isEchoed(entry.getKey())) {
				continue;
			}
			payload.add(entry.getKey(), entry.getValue());
		}
		return payload;
	}

	private static boolean isEchoed(String field) {
		for (int i = 0; i < NOT_ECHOED.length; i++) {
			if (NOT_ECHOED[i].equals(field)) {
				return false;
			}
		}
		return true;
	}

	private static String stringOf(JsonObject object, String field) {
		try {
			if (null == object || !object.has(field) || object.get(field).isJsonNull()) {
				return "";
			}
			JsonElement value = object.get(field);
			return value.isJsonPrimitive() ? value.getAsString() : value.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String defaulted(String preferred, String fallback) {
		return (null == preferred || "".equals(preferred.trim()))
				? (null == fallback ? "" : fallback) : preferred;
	}
}
