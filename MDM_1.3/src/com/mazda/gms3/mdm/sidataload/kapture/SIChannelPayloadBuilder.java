package com.mazda.gms3.mdm.sidataload.kapture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.mazda.gms3.mdm.kapture.KaptureApiClient;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelSchemaDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;

/**
 * BUILDS THE JSON KAPTURE IS SENT - what InfoquiraUtils.createSIContentXML() used to do in XML
 * (that class is gone; this is its whole replacement).
 *
 * SAME FIELD NAMES, DIFFERENT CONTAINER. The channel's attributes were an XML fragment
 * (&lt;SERVICE_INFORMATION&gt;&lt;TITLE&gt;...) and are now a JSON object under the same key
 * ("SERVICE_INFORMATION":{"TITLE":...}), with the tag names carried across unchanged. Verified
 * against the live API: the SERVICE_INFORMATION node on SI1123 uses exactly these names.
 *
 * ONE NAME DIFFERS, AND ONLY IN THE PAYLOAD. The Word file's token is [||TSB_ISSUE_DATE||] and
 * stays that way; Kapture's schema field is "Issue Date", so the value is emitted as ISSUE_DATE.
 * There is no TSB_ISSUE_DATE field in the SI channel - sending it would silently drop the date.
 *
 * THREE PAYLOADS, BECAUSE THERE ARE THREE ENDPOINTS:
 *   buildCreate            -> add-article-tab           a document that does not exist yet
 *   buildUpdate            -> update-article-tab        the MASTER IDENTIFIER of one that does
 *   buildTranslationUpdate -> save-translate-article    a TRANSLATION, created or updated alike
 *
 * THE RULE THAT GOVERNS THE TWO UPDATE PAYLOADS: Kapture's write endpoints REWRITE THE WHOLE ROW,
 * so any field left out is stored as NULL. Everything therefore comes back from the read
 * untouched, and only what this screen actually means to change is replaced.
 */
public class SIChannelPayloadBuilder {

	static Logger logger = LogManager.getLogger(SIChannelPayloadBuilder.class);

	/** Kapture's own name for the channel - the value of "type" and the key of the attribute node. */
	private static final String CHANNEL_TYPE = "SERVICE_INFORMATION";

	private static final String ARTICLE_STATE_UNPUBLISHED = "Unpublished";
	private static final String ISARTICLE = "KAPTURE";
	private static final String CRAWL_TYPE = "KAPTURE";

	/*
	 * ECHOED WHOLESALE ONTO A TRANSLATION PAYLOAD - the identical list
	 * KaptureContentServiceImpl.buildTranslationPayload() uses, so the two writers cannot disagree
	 * about what save-translate-article expects.
	 */
	private static final String[] TRANSLATE_ECHO_FIELDS = { "articleOwner", "ISARTICLE",
			"articleCreator", "contentId", "contentid", "emailId", "type", "title", "DOCUMENTID",
			"crawlType", "userLocale", "articleId", "articlelistview", "createdDate",
			"contentTranslateList", "articleCreatorUserID", "status" };

	// ------------------------------------------------------------------ the channel attributes

	/**
	 * THE SERVICE_INFORMATION NODE, BUILT FROM THE WORD FILE.
	 *
	 * FIELD FOR FIELD WHAT createSIContentXML() WROTE, including which fields are written
	 * unconditionally and which only when they have a value - that distinction is not cosmetic,
	 * because a field that IS written blanks whatever the document had there.
	 *
	 * BULLETIN_NOTES LOSES ITS SECURITY ATTRIBUTE. The XML carried
	 * &lt;BULLETIN_NOTES SECURITY="TECHNICIAN"&gt; and JSON has nowhere to put an attribute on a
	 * string; Kapture models field-level security separately, in fieldSecurities. The TEXT is
	 * carried across unchanged - only the marking is not.
	 */
	public static JsonObject buildAttributeNode(SIChannelSchemaDetails contentDetails) {
		JsonObject node = new JsonObject();
		if (null == contentDetails) {
			return node;
		}

		node.addProperty("TITLE", nz(contentDetails.getTitle()));

		/*
		 * WRITTEN ONLY WHEN PRESENT - exactly as the XML builder did. These four were wrapped in an
		 * "is it empty" test there, so an upload that does not mention them leaves whatever the
		 * document already has.
		 */
		if (!"".equals(nz(contentDetails.getTsbNumber()))) {
			node.addProperty("TSB_NUMBER", nz(contentDetails.getTsbNumber()));
		}
		if (!"".equals(nz(contentDetails.getCampaignNumber()))) {
			node.addProperty("CAMPAIGN_NUMBER", nz(contentDetails.getCampaignNumber()));
		}
		if (!"".equals(nz(contentDetails.getTiNumber()))) {
			node.addProperty("TI_NUMBER", nz(contentDetails.getTiNumber()));
		}
		if (!"".equals(nz(contentDetails.getMtipsNumber()))) {
			node.addProperty("MTIPS_NUMBER", nz(contentDetails.getMtipsNumber()));
		}

		// WRITTEN ALWAYS - blank when the document did not carry them, again as the XML builder did
		node.addProperty("LEGACY_ID", nz(contentDetails.getLegacyId()));
		/*
		 * THE ONE RENAME. Read from [||TSB_ISSUE_DATE||] in the Word file, sent as Kapture's
		 * ISSUE_DATE - see the class comment.
		 */
		node.addProperty("ISSUE_DATE", nz(contentDetails.getTsbIssueDate()));
		node.addProperty("FIRST_PUBLICATION_DATE", nz(contentDetails.getFirstPublicationDate()));
		node.addProperty("DESCRIPTION", nz(contentDetails.getDescription()));
		node.addProperty("REPAIR_PROCEDURE", nz(contentDetails.getRepairProcedure()));
		node.addProperty("PARTS_INFORMATION", nz(contentDetails.getPartsInformation()));
		node.addProperty("WARRANTY_INFORMATION", nz(contentDetails.getWarrantyInformation()));
		node.addProperty("CALIBRATION", nz(contentDetails.getCallibration()));
		node.addProperty("SI_HISTORY", nz(contentDetails.getSiHistory()));
		node.addProperty("SHOW_AS_NEWS_FOR_WEEKS", nz(contentDetails.getShowAsNewsForWeek()));
		node.addProperty("MC_AUTHORIZATION_NUMBER", nz(contentDetails.getMcAuthorizationNo()));
		node.addProperty("BULLETIN_NOTES", nz(contentDetails.getBulletinNotes()));
		node.addProperty("INTERNAL_DOCUMENT_REFERENCE_NUMBER",
				nz(contentDetails.getInternalDocRefNumber()));

		return node;
	}

	// ------------------------------------------------------------------ create

	/**
	 * THE add-article-tab PAYLOAD - a brand new document in the selected locale.
	 *
	 * CREATED AS THE MASTER IDENTIFIER: primaryLocale and translatedLocale are both the selected
	 * locale, which is what makes the document its own master rather than a translation of
	 * something. A translation is never created here - it goes through save-translate-article
	 * against a master that already exists.
	 *
	 * THE VIEW COMES FROM THE MARKET, exactly as createContent() derived it - MNAO / MC / MME by
	 * locale - and Kapture names those views MNAO_KEY / MC_KEY / MME_KEY.
	 *
	 * NO CATEGORIES. The old creation path mapped none either; categories are applied afterwards
	 * by the SI VIN screens.
	 */
	public static String buildCreate(SIChannelSchemaDetails contentDetails, String locale,
			KaptureApiClient client) {
		JsonObject payload = new JsonObject();

		String user = defaulted(client.getUserDisplayName(), nz(contentDetails.getWslId()));

		payload.addProperty("primaryLocale", locale);
		payload.addProperty("translatedLocale", locale);
		payload.addProperty("title", nz(contentDetails.getTitle()));
		payload.addProperty("articleState", ARTICLE_STATE_UNPUBLISHED);
		payload.addProperty("articleOwner", user);
		payload.addProperty("articleCreator", user);
		payload.addProperty("createdBy", user);
		payload.addProperty("modifiedBy", user);
		payload.addProperty("articleCreatorUserID", client.getUserPkId());
		payload.addProperty("userEmail", client.getUserEmail());
		payload.addProperty("type", CHANNEL_TYPE);
		payload.addProperty("ISARTICLE", ISARTICLE);
		payload.addProperty("crawlType", CRAWL_TYPE);
		payload.addProperty("categoriesTreeRefKeys", "");
		payload.addProperty("categoriesESITreeRefKeys", "");

		payload.add("articlelistusergroup", new JsonArray());
		payload.add("articlelistview", viewArray(viewRefKeyFor(contentDetails.getMarket())));
		payload.add("facetsSchemaFields", new JsonArray());

		payload.add(CHANNEL_TYPE, buildAttributeNode(contentDetails));
		return payload.toString();
	}

	// ------------------------------------------------------------------ update, master identifier

	/**
	 * THE update-article-tab PAYLOAD - a new version of an existing MASTER IDENTIFIER.
	 *
	 * EVERY FIELD EXCEPT THE CONTENT IS THE READ, HANDED BACK. The endpoint rewrites the whole row,
	 * so this is not politeness - dropping a field empties the column. The only things this screen
	 * decides are the attribute node, the title that goes with it, and who is making the change.
	 *
	 * id / articleVersion / publishedVersion / articleState COME FROM k_article, never from the
	 * read: latest-article carries no id and returns the version as a NUMBER, so 1.0 would go back
	 * as "1" and address the wrong version.
	 */
	public static String buildUpdate(JsonObject article, SIChannelSchemaDetails contentDetails,
			KaptureArticleIdentity identity, String documentId, String locale,
			KaptureApiClient client) {
		JsonObject payload = new JsonObject();

		payload.addProperty("id", identity.getId());
		payload.addProperty("primaryLocale", stringOf(article, "primaryLocale"));
		payload.addProperty("articleState", identity.getArticleState());
		payload.addProperty("articleOwner", stringOf(article, "articleOwner"));
		payload.addProperty("articleCreator", stringOf(article, "articleCreator"));
		payload.addProperty("createdBy", stringOf(article, "createdBy"));
		payload.addProperty("createdDate", stringOf(article, "createdDate"));
		payload.addProperty("dateAdded", stringOf(article, "createdDate"));
		payload.addProperty("articleVersion", identity.getArticleVersion());
		payload.addProperty("articleId", documentId);
		payload.addProperty("pubFlag", Boolean.FALSE);

		/* WHO IS WRITING, not who wrote last - the row records this change honestly. */
		payload.addProperty("modifiedBy", defaulted(client.getUserDisplayName(),
				stringOf(article, "modifiedBy")));
		payload.addProperty("userEmail", client.getUserEmail());

		/*
		 * A7 - ORIGINAL AUTHORSHIP IS PRESERVED, NOT REPLACED WITH THE EDITOR.
		 *
		 * Kapture confirmed (2026-08-07) that the backend stores this EXACTLY AS SENT and does not
		 * overwrite it afterwards - so whoever sends the editing user here is the one destroying
		 * the original author, and for KAuthor's own updates that is KAuthor. The instruction was
		 * "send the existing values and use modifiedBy for the editor".
		 *
		 * THE READ WINS; the token user is a fallback ONLY for a document that carries no value.
		 * This was the other way round until A7 was answered, which silently replaced the author
		 * on every update and every publish.
		 */
		payload.addProperty("articleCreatorUserID", defaulted(
				stringOf(article, "articleCreatorUserID"), client.getUserPkId()));

		/*
		 * BLANK GOES AS JSON null, NOT AS "". An empty string is a value and would overwrite a date
		 * somebody set between the read and this write.
		 */
		addDate(payload, "expireDate", stringOf(article, "expireDate"));
		addDate(payload, "reviewDate", stringOf(article, "reviewDate"));

		if (identity.isPublished()) {
			payload.addProperty("isUpdateArticle", Boolean.TRUE);
			payload.addProperty("publishedVersion", identity.getPublishedVersion());
		}

		payload.addProperty("type", CHANNEL_TYPE);
		payload.addProperty("ISARTICLE", defaulted(stringOf(article, "ISARTICLE"), ISARTICLE));
		payload.addProperty("crawlType", defaulted(stringOf(article, "crawlType"), CRAWL_TYPE));
		/*
		 * THE FALLBACK IS LOAD BEARING. The read OMITS translatedLocale on an unpublished
		 * translation, so echoing it alone would send null and blank the column.
		 */
		payload.addProperty("translatedLocale",
				defaulted(stringOf(article, "translatedLocale"), locale));

		/*
		 * CARRIED OVER COMPLETE, NAMES INCLUDED - dropping userGroupName or viewName makes KAuthor
		 * render "undefined". The read returns a SINGLE OBJECT when there is one group rather than
		 * a one-element array, so the shape is normalised.
		 */
		payload.add("articlelistusergroup", asArray(article, "articlelistusergroup"));
		payload.add("articlelistview", asArray(article, "articlelistview"));
		payload.add("facetsSchemaFields", new JsonArray());

		/*
		 * CATEGORIES ARE THIS DOCUMENT'S, UNCHANGED. This screen loads CONTENT and has no opinion
		 * about mapping; the arrays have to be sent or the update drops the document's categories.
		 */
		payload.add("articlelistcategory", asArray(article, "articlelistcategory"));
		payload.add("articleesicategory", asArray(article, "articleesicategory"));
		payload.addProperty("categoriesTreeRefKeys", stringOf(article, "categoriesTreeRefKeys"));
		payload.addProperty("categoriesTreeLabels", stringOf(article, "categoriesTreeLabels"));
		payload.addProperty("categoriesESITreeRefKeys", stringOf(article, "categoriesESITreeRefKeys"));
		payload.addProperty("categoriesESITreeLabels", stringOf(article, "categoriesESITreeLabels"));

		// WHAT THIS SCREEN IS ACTUALLY HERE TO CHANGE
		payload.addProperty("title", nz(contentDetails.getTitle()));
		payload.add(CHANNEL_TYPE, buildAttributeNode(contentDetails));
		return payload.toString();
	}

	// ------------------------------------------------------------------ update, translation

	/**
	 * THE save-translate-article PAYLOAD - a TRANSLATION, whether it exists yet or not.
	 *
	 * ONE ENDPOINT FOR BOTH. Kapture creates the translation if the locale has no version and
	 * versions it if it does, so there is no separate "create translation" call to make.
	 *
	 * NOTE translateLocale - NO "d". That is the key this endpoint uses, and it is not the same key
	 * the master payload uses (translatedLocale). primaryLocale must stay the MASTER'S locale:
	 * publishing promotes a translation to primary = its own locale, so the read would say en_EU
	 * where Kapture expects en_UK.
	 */
	public static String buildTranslationUpdate(JsonObject article,
			SIChannelSchemaDetails contentDetails, KaptureArticleIdentity identity,
			String documentId, String contentId, String locale, String masterLocale) {
		JsonObject payload = new JsonObject();

		for (int i = 0; i < TRANSLATE_ECHO_FIELDS.length; i++) {
			String field = TRANSLATE_ECHO_FIELDS[i];
			if (null != article && article.has(field) && !article.get(field).isJsonNull()) {
				payload.add(field, article.get(field));
			}
		}

		/*
		 * USER GROUPS ARE ECHOED WHEN PRESENT AND OMITTED WHEN ABSENT - NEVER SENT EMPTY.
		 * save-translate-article discards the groups it is handed, so a document whose groups were
		 * lost that way reads back without them; sending [] would look like an instruction to clear
		 * what is still in k_article_usergroup.
		 */
		if (null != article && article.has("articlelistusergroup")
				&& !article.get("articlelistusergroup").isJsonNull()) {
			payload.add("articlelistusergroup", article.get("articlelistusergroup"));
		}

		if (null != identity && identity.isUsable()) {
			payload.addProperty("id", identity.getId());
			payload.addProperty("articleVersion", identity.getArticleVersion());
		}
		payload.addProperty("primaryLocale", masterLocale);
		payload.addProperty("translateLocale", locale);
		payload.addProperty("isAutoPublish", Boolean.FALSE);
		payload.addProperty("pubFlag", Boolean.FALSE);

		// FILLED ONLY WHERE THE READ DID NOT CARRY THEM - these are known independently
		if (!payload.has("contentId")) {
			payload.addProperty("contentId", contentId);
		}
		if (!payload.has("articleId")) {
			payload.addProperty("articleId", documentId);
		}
		if (!payload.has("DOCUMENTID")) {
			payload.addProperty("DOCUMENTID", documentId);
		}
		if (!payload.has("userLocale")) {
			payload.addProperty("userLocale", locale);
		}
		if (!payload.has("type")) {
			payload.addProperty("type", CHANNEL_TYPE);
		}

		payload.add("articlelistcategory", asArray(article, "articlelistcategory"));
		payload.add("articleesicategory", asArray(article, "articleesicategory"));

		// WHAT THIS SCREEN IS ACTUALLY HERE TO CHANGE
		payload.addProperty("title", nz(contentDetails.getTitle()));
		payload.add(CHANNEL_TYPE, buildAttributeNode(contentDetails));
		return payload.toString();
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * THE KAPTURE VIEW FOR A MARKET - MNAO_KEY / MC_KEY / MME_KEY.
	 *
	 * createContent() picked the view from the locale through the view.mnao / view.mc / view.mme
	 * properties; the same properties are used here and Kapture's reference key is that value with
	 * _KEY appended, which is how the views are named on every existing SI document.
	 */
	static String viewRefKeyFor(String market) {
		String value = null == market ? "" : market.trim().toUpperCase();
		String view = null;
		if (value.equals(prop("market.mnao", "MNAO").toUpperCase())) {
			view = prop("view.mnao", "MNAO");
		} else if (value.equals(prop("market.mc", "MC").toUpperCase())) {
			view = prop("view.mc", "MC");
		} else {
			view = prop("view.mme", "MME");
		}
		return view.trim().toUpperCase() + "_KEY";
	}

	private static JsonArray viewArray(String viewRefKey) {
		JsonArray views = new JsonArray();
		JsonObject view = new JsonObject();
		view.addProperty("viewRefKey", viewRefKey);
		views.add(view);
		return views;
	}

	/** A JSON array field, normalising the single-object shape the API returns for one entry. */
	private static JsonArray asArray(JsonObject article, String field) {
		JsonArray array = new JsonArray();
		if (null == article || !article.has(field)) {
			return array;
		}
		JsonElement element = article.get(field);
		if (null == element || element.isJsonNull()) {
			return array;
		}
		if (element.isJsonArray()) {
			return element.getAsJsonArray();
		}
		if (element.isJsonObject()) {
			array.add(element);
		}
		return array;
	}

	private static void addDate(JsonObject payload, String field, String value) {
		if (null == value || "".equals(value.trim())) {
			payload.add(field, com.google.gson.JsonNull.INSTANCE);
		} else {
			payload.addProperty(field, toWriteFormat(value.trim()));
		}
	}

	/** The slash forms the read APIs emit - see toWriteFormat(). */
	private static final String[] SLASH_DATE_FORMATS = { "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy" };

	private static final String WRITE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * THE ONE PLACE A VALUE FROM THE READ IS NOT ECHOED BACK VERBATIM.
	 *
	 * THE READ APIs EMIT A FORMAT THE WRITE APIs REJECT. They return expireDate and reviewDate as
	 * MM/dd/yyyy HH:mm:ss, and sending that straight back is refused:
	 *
	 *   update-article-tab  : 400 Validation failed, "Expire Date must be valid for locale en_US."
	 *   publish-article     : HTTP 200 carrying {"status":"500", "message":"... Unparseable
	 *                         date: \"09/03/2026 00:00:00\""} - which leaves the document
	 *                         UNPUBLISHED while reporting success at the HTTP level
	 *
	 * THE SAME CONVERSION IS IN KaptureContentServiceImpl AND TranslationPayloadBuilder, where it
	 * was verified by hand against the live API on SI1123 / en_US. This screen was writing the
	 * slash form back untouched, so a document it updated could not afterwards be published by
	 * anything - including KAuthor.
	 *
	 * SCOPE IS DELIBERATELY THESE TWO FIELDS ONLY. createdDate and dateAdded go out in the SAME
	 * request in the SAME slash format and are accepted; the server takes those either way.
	 *
	 * ANYTHING NOT IN THE SLASH FORM IS PASSED THROUGH UNTOUCHED, including an already-hyphenated
	 * value and anything unrecognisable - dd/MM and MM/dd cannot be told apart for the first twelve
	 * days of a month, so guessing at an unknown shape could move a date by months in silence.
	 */
	private static String toWriteFormat(String value) {
		for (int i = 0; i < SLASH_DATE_FORMATS.length; i++) {
			java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(SLASH_DATE_FORMATS[i]);
			// STRICT - lenient parsing rolls 13/01/2026 forward into 2027 instead of failing
			in.setLenient(false);
			try {
				java.util.Date parsed = in.parse(value);
				return new java.text.SimpleDateFormat(WRITE_DATE_FORMAT).format(parsed);
			} catch (java.text.ParseException ignore) {
				// not this pattern - try the next, then send it as received
			}
		}
		return value;
	}

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

	private static String nz(String value) {
		return null == value ? "" : value;
	}

	private static String defaulted(String preferred, String fallback) {
		return (null == preferred || "".equals(preferred.trim())) ? nz(fallback) : preferred.trim();
	}

	private static String prop(String key, String fallback) {
		String value = ApplicationProperties.getProperty(key);
		return (null == value || "".equals(value.trim())) ? fallback : value.trim();
	}
}
