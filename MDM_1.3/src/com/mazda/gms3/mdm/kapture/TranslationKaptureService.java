package com.mazda.gms3.mdm.kapture;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.KaptureArticleIdentity;

/**
 * THE KAPTURE SIDE OF SI TRANSLATION UPDATE - replaces the InQuira IQServiceClient in both the
 * export and the import halves of that screen.
 *
 * WHAT THE SCREEN DOES, so the shape here makes sense. A schedule is ONE LOCALE. Export pulls each
 * document's TRANSLATABLE FIELDS out to a small XML for the vendor; import reads the translated
 * file back and writes those same fields into the SAME locale's document. Nothing else about the
 * document is supposed to change.
 *
 * WHY IT TRADES IN XML. The screen's existing prepareXML() / retrieveTitle() / getDocumentId()
 * work on the InfoManager XML shape, and they encode the field list and the merge rules that
 * business relies on. Kapture serves the same fields as a FLAT JSON OBJECT under the channel key,
 * so this class converts between the two and leaves that logic completely untouched:
 *
 *   read  : latest-article -> {"SERVICE_INFORMATION":{"TITLE":"..","DESCRIPTION":".."}}
 *                          -> &lt;SERVICE_INFORMATION&gt;&lt;TITLE/&gt;&lt;DESCRIPTION/&gt;&lt;/SERVICE_INFORMATION&gt;
 *   write : the merged XML -> the same flat object, put back on the echoed read
 *
 * THE READ IS ECHOED, NOT REBUILT - and that is the critical difference from SI Data Load. The
 * write endpoints REWRITE THE WHOLE ROW, so a field left out of the payload is stored as NULL. SI
 * Data Load rebuilds its attribute node on purpose, because a Word upload is the whole document.
 * A TRANSLATION BATCH IS NOT: it carries 9 fields of a document that has 22. Rebuilding here would
 * blank ATTACHMENTS, CAMPAIGN_NUMBER, TSB_NUMBER, SIGNATURE and the rest. So every field of the
 * read is handed back and only the translated ones are overwritten.
 *
 * PUBLISHED IN, PUBLISHED OUT. The InfoManager call was
 * modifyContent(content, content.getPublished()) - it PRESERVED the state, unlike SI Data Load
 * which hardcoded false. Both write endpoints leave a new UNPUBLISHED version behind, so a
 * document that was live is re-published here; one that was not is left alone.
 *
 * EVERY FAILURE CARRIES AN ERROR CODE AND MESSAGE. They are written to the transaction report, so
 * "it failed" without a reason is not an acceptable outcome - see TranslationResult.
 */
public class TranslationKaptureService {

	static Logger logger = LogManager.getLogger(TranslationKaptureService.class);

	/* Error codes, reported to business through the transaction report's error columns. */
	public static final String ERR_NO_CONTENT_ID = "TRU00001";
	public static final String ERR_NOT_FOUND = "TRU00002";
	public static final String ERR_READ_FAILED = "TRU00003";
	public static final String ERR_NO_ARTICLE_ROW = "TRU00004";
	public static final String ERR_WRITE_FAILED = "TRU00005";
	public static final String ERR_NOT_REPUBLISHED = "TRU00006";
	public static final String ERR_UNEXPECTED = "TRU00007";

	/** Outcome of one Kapture operation - never null, and never a bare failure. */
	public static class TranslationResult {
		private boolean ok = false;
		private boolean found = false;
		private String errorCode = "";
		private String errorMessage = "";
		private String nodeXml = "";
		private String version = "";
		private String contentId = "";
		private boolean published = false;
		private JsonObject article = null;

		public boolean isOk() {
			return ok;
		}

		/** False means the document has no version for this locale - not that the read failed. */
		public boolean isFound() {
			return found;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		/** The channel node as XML, in the shape the screen's prepareXML() expects. */
		public String getNodeXml() {
			return nodeXml;
		}

		public String getVersion() {
			return version;
		}

		public String getContentId() {
			return contentId;
		}

		public boolean isPublished() {
			return published;
		}

		/** The whole latest-article body, so a write can hand every field back. */
		public JsonObject getArticle() {
			return article;
		}
	}

	// ------------------------------------------------------------------ channel

	/**
	 * The channel node name for a document - SI1123 -&gt; SERVICE_INFORMATION, SM1027 -&gt;
	 * SERVICE_MANUALS.
	 *
	 * TAKEN FROM THE SAME PROPERTIES THE SCREEN ALREADY USES (SI_ROOT_NODE / SM_ROOT_NODE), so the
	 * channel list and the translatable-field list stay configured in one place.
	 */
	public static String rootNodeFor(String documentId) {
		String prefix = prefixOf(documentId);
		if (null == prefix) {
			return null;
		}
		String node = ApplicationProperties.getProperty(prefix + "_ROOT_NODE");
		return (null == node || "".equals(node.trim())) ? null : node.trim();
	}

	/** Leading alphabetic part of an article id, uppercased - SM1027 -&gt; SM. */
	public static String prefixOf(String documentId) {
		if (null == documentId) {
			return null;
		}
		String id = documentId.trim();
		int i = 0;
		while (i < id.length() && Character.isLetter(id.charAt(i))) {
			i++;
		}
		return i > 0 ? id.substring(0, i).toUpperCase() : null;
	}

	// ------------------------------------------------------------------ read

	/**
	 * READS A DOCUMENT'S CHANNEL NODE FOR ONE LOCALE.
	 *
	 * REPLACES getLatestContentRecordByDocumentIDAndLocale(). Like it, this asks for the LATEST
	 * version of the locale - published or not - because that is what a translation is taken from
	 * and written back onto.
	 */
	public static TranslationResult read(String documentId, String locale, KaptureApiClient client) {
		TranslationResult result = new TranslationResult();
		try {
			String rootNode = rootNodeFor(documentId);
			String contentId = FetchKaptureDataDAO.getChannelContentId(prefixOf(documentId));
			if (null == rootNode || null == contentId || "".equals(contentId)) {
				result.errorCode = ERR_NO_CONTENT_ID;
				result.errorMessage = "Could not identify the Kapture channel for " + documentId
						+ ". Check the document id prefix and k_content_type.";
				logger.info("read :: " + result.errorMessage);
				return result;
			}
			result.contentId = contentId;

			KaptureApiResult api = client.latestArticle(documentId, contentId, locale,
					"SI Translation Update - reading {" + documentId + "} / {" + locale + "}");
			if (!api.isOk()) {
				result.errorCode = ERR_READ_FAILED;
				result.errorMessage = "Failed to read " + documentId + " for " + locale
						+ " from Kapture. " + describe(api);
				logger.info("read :: " + result.errorMessage);
				return result;
			}

			JsonObject article = responseBody(api.body);
			if (null == article || article.entrySet().isEmpty()) {
				/*
				 * A 200 WITH NOTHING IN IT MEANS "no such document for this locale". That is a
				 * business outcome, not a fault - the screen reports it as the document not
				 * existing for the locale, exactly as the InfoManager version did.
				 */
				result.errorCode = ERR_NOT_FOUND;
				result.errorMessage = documentId + " does not exist for " + locale
						+ " Locale in Kapture.";
				logger.info("read :: " + result.errorMessage);
				return result;
			}

			result.article = article;
			result.found = true;
			result.published = "Published".equalsIgnoreCase(stringOf(article, "articleState"));
			result.version = result.published
					? defaulted(stringOf(article, "publishedVersion"), stringOf(article, "articleVersion"))
					: stringOf(article, "articleVersion");

			JsonObject node = article.has(rootNode) && article.get(rootNode).isJsonObject()
					? article.getAsJsonObject(rootNode) : null;
			if (null == node) {
				result.errorCode = ERR_NOT_FOUND;
				result.errorMessage = documentId + " for " + locale + " carries no " + rootNode
						+ " content in Kapture.";
				logger.info("read :: " + result.errorMessage);
				return result;
			}

			result.nodeXml = toXml(rootNode, node);
			result.ok = true;
			logger.info("read :: {" + documentId + "} / {" + locale + "} version {" + result.version
					+ "} published=" + result.published + " node fields=" + node.entrySet().size());
			return result;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(TranslationKaptureService.class.getName(), "read()", e);
			result.errorCode = ERR_UNEXPECTED;
			result.errorMessage = "Unexpected error reading " + documentId + " for " + locale + ". "
					+ e.getMessage();
			return result;
		}
	}

	// ------------------------------------------------------------------ write

	/**
	 * WRITES THE MERGED CHANNEL NODE BACK, and re-publishes when the document was published.
	 *
	 * REPLACES modifyContent(content, content.getPublished()).
	 *
	 * MASTER OR TRANSLATION IS DECIDED FROM k_article, NOT GUESSED. update-article-tab handles the
	 * ORIGINAL ARTICLE ONLY; a translation must go through save-translate-article or its locale is
	 * destroyed. The two are told apart by master_locale, because publishing PROMOTES a translation
	 * to primary = translated = its own locale, leaving those two columns identical to a master's.
	 *
	 * @param mergedNodeXml the channel node AFTER the screen merged the translated fields into it
	 * @param before        the read this update is based on - every field of it is handed back
	 */
	public static TranslationResult write(String documentId, String locale, String mergedNodeXml,
			TranslationResult before, KaptureApiClient client) {
		TranslationResult result = new TranslationResult();
		result.contentId = before.getContentId();
		try {
			String rootNode = rootNodeFor(documentId);
			JsonObject node = carryForward(before, rootNode, toJson(mergedNodeXml, rootNode));
			if (null == node) {
				result.errorCode = ERR_WRITE_FAILED;
				result.errorMessage = "The merged content for " + documentId + " / " + locale
						+ " could not be read back - nothing was written.";
				logger.info("write :: " + result.errorMessage);
				return result;
			}

			KaptureArticleIdentity identity = FetchKaptureDataDAO.getArticleIdentity(documentId, locale);
			if (!identity.isUsable()) {
				result.errorCode = ERR_NO_ARTICLE_ROW;
				result.errorMessage = "No current version of " + documentId + " for " + locale
						+ " in Kapture - it cannot be updated.";
				logger.info("write :: " + result.errorMessage);
				return result;
			}

			boolean translation = identity.isTranslation();
			String payload = TranslationPayloadBuilder.buildUpdate(before.getArticle(), identity,
					documentId, locale, rootNode, node, translation, client);

			KaptureApiResult api = translation
					? client.saveTranslateArticle(before.getContentId(), payload,
							"SI Translation Update - translation {" + documentId + "} / {" + locale + "}")
					: client.updateArticle(before.getContentId(), payload,
							"SI Translation Update - master {" + documentId + "} / {" + locale + "}");

			if (!api.isOk()) {
				result.errorCode = ERR_WRITE_FAILED;
				result.errorMessage = "Failed to update " + documentId + " for " + locale
						+ " in Kapture. " + describe(api);
				logger.info("write :: " + result.errorMessage);
				return result;
			}

			result.ok = true;
			result.found = true;
			KaptureArticleIdentity written = FetchKaptureDataDAO.getArticleIdentity(documentId, locale);
			result.version = written.isUsable() ? written.getArticleVersion() : identity.getArticleVersion();
			logger.info("write :: {" + documentId + "} / {" + locale + "} updated as "
					+ (translation ? "TRANSLATION" : "MASTER") + ", new version {" + result.version + "}");

			/*
			 * PUBLISHED IN, PUBLISHED OUT. Both endpoints leave a NEW UNPUBLISHED version, so
			 * without this an update to a live document silently takes it off the air: the old
			 * content stays published while the translated content sits in a draft nobody sees.
			 */
			if (before.isPublished()) {
				result = republish(documentId, locale, before, written, rootNode, node, result,
						client);
			} else {
				result.published = false;
				logger.info("write :: {" + documentId + "} / {" + locale
						+ "} was not published before the update - left unpublished.");
			}
			return result;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(TranslationKaptureService.class.getName(), "write()", e);
			result.ok = false;
			result.errorCode = ERR_UNEXPECTED;
			result.errorMessage = "Unexpected error updating " + documentId + " for " + locale + ". "
					+ e.getMessage();
			return result;
		}
	}

	/**
	 * THE NODE TO WRITE: THE READ, WITH THE MERGE APPLIED OVER IT.
	 *
	 * ONLY THE CONFIGURED NODES CHANGE - EVERYTHING ELSE PASSES THROUGH AS IS. That is the screen's
	 * rule, and it cannot be left to the merge to honour: on live data the merge returned a node
	 * missing six fields the document had (LEGACY_ID, FIRST_PUBLICATION_DATE, ISSUE_DATE,
	 * INTERNAL_DOCUMENT_REFERENCE_NUMBER, MC_AUTHORIZATION_NUMBER, SHOW_AS_NEWS_FOR_WEEKS). Since
	 * the write endpoints rewrite the whole row, sending that would have stored all six as NULL.
	 *
	 * So the base is the READ, and the merge only overwrites. A field the merge does not mention
	 * keeps its current value by construction rather than by trust - which is the same reason the
	 * payload echoes the read rather than rebuilding it.
	 */
	private static JsonObject carryForward(TranslationResult before, String rootNode,
			JsonObject merged) {
		JsonObject base = null;
		if (null != before && null != before.getArticle() && null != rootNode
				&& before.getArticle().has(rootNode)
				&& before.getArticle().get(rootNode).isJsonObject()) {
			// a copy, so the read stays untouched for the publish payload that follows
			base = toJson(toXml(rootNode, before.getArticle().getAsJsonObject(rootNode)), rootNode);
		}
		if (null == base) {
			return merged;
		}
		if (null == merged) {
			return base;
		}
		Iterator<Map.Entry<String, JsonElement>> it = merged.entrySet().iterator();
		int overwritten = 0;
		while (it.hasNext()) {
			Map.Entry<String, JsonElement> entry = it.next();
			base.add(entry.getKey(), entry.getValue());
			overwritten++;
		}
		logger.info("carryForward :: " + base.entrySet().size() + " field(s) written, "
				+ overwritten + " from the merge, the rest carried from the read.");
		return base;
	}

	/**
	 * Publishes the version the update just created.
	 *
	 * THE IDENTITY IS RE-READ, NOT REUSED - the row to publish is the one the update CREATED, which
	 * has a different k_article id and version from the one read beforehand. Publishing the old
	 * identity would promote the wrong version.
	 *
	 * A FAILURE HERE IS A FAILURE OF THE WHOLE STEP. The content is in, but the document is no
	 * longer live; reporting Success would tell business their published document was updated when
	 * it has in fact been taken off the air.
	 *
	 * THE MERGED NODE IS PASSED THROUGH because publishing rewrites the row as well - see
	 * buildPublish. Publishing the pre-update read would revert the translation.
	 */
	private static TranslationResult republish(String documentId, String locale,
			TranslationResult before, KaptureArticleIdentity written, String rootNode,
			JsonObject mergedNode, TranslationResult result, KaptureApiClient client) {
		if (!written.isUsable()) {
			result.ok = false;
			result.errorCode = ERR_NOT_REPUBLISHED;
			result.errorMessage = documentId + " for " + locale
					+ " was updated but the new version could not be identified, so it could not be"
					+ " re-published and is no longer live.";
			logger.info("republish :: " + result.errorMessage);
			return result;
		}

		String payload = TranslationPayloadBuilder.buildPublish(before.getArticle(), written,
				documentId, locale, result.getContentId(), rootNode, mergedNode, client);
		KaptureApiResult api = client.publishArticle(documentId, payload,
				"SI Translation Update - re-publishing {" + documentId + "} / {" + locale + "}");
		if (api.isOk()) {
			result.published = true;
			logger.info("republish :: {" + documentId + "} / {" + locale + "} re-published at {"
					+ written.getArticleVersion() + "}");
			return result;
		}

		result.ok = false;
		result.published = false;
		result.errorCode = ERR_NOT_REPUBLISHED;
		result.errorMessage = documentId + " for " + locale
				+ " was updated but could not be re-published, so it is no longer live. "
				+ describe(api);
		logger.info("republish :: " + result.errorMessage);
		return result;
	}

	// ------------------------------------------------------------------ JSON <-> XML

	/**
	 * The channel node as XML, so the screen's existing parsing can consume it unchanged.
	 *
	 * VALUES GO IN AS CDATA because they are rich text - the fields being translated hold HTML.
	 * ("]]&gt;" inside a value is split across two CDATA sections, the only way to escape it.)
	 */
	public static String toXml(String rootNode, JsonObject node) {
		StringBuffer sb = new StringBuffer();
		sb.append("<").append(rootNode).append(">");
		Iterator<Map.Entry<String, JsonElement>> it = node.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JsonElement> entry = it.next();
			String name = entry.getKey();
			if (!isElementName(name)) {
				// not something that can be an element - it cannot have come from a channel node
				logger.info("toXml :: skipping field {" + name + "} - not a usable element name.");
				continue;
			}
			sb.append("<").append(name).append("><![CDATA[");
			sb.append(textOf(entry.getValue()).replace("]]>", "]]]]><![CDATA[>"));
			sb.append("]]></").append(name).append(">");
		}
		sb.append("</").append(rootNode).append(">");
		return sb.toString();
	}

	/**
	 * The channel node back as a flat JSON object, after the screen has merged the translations in.
	 *
	 * EVERY CHILD IS CARRIED OVER, not just the translated ones - the merge hands back the whole
	 * node and the write must too, or the endpoint nulls what is missing.
	 */
	public static JsonObject toJson(String nodeXml, String rootNode) {
		try {
			if (null == nodeXml || "".equals(nodeXml.trim())) {
				return null;
			}
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(nodeXml));
			Document doc = db.parse(is);
			Element root = doc.getDocumentElement();
			if (null == root) {
				return null;
			}
			if (null != rootNode && !rootNode.equals(root.getNodeName())) {
				NodeList found = doc.getElementsByTagName(rootNode);
				if (null != found && found.getLength() > 0 && found.item(0) instanceof Element) {
					root = (Element) found.item(0);
				}
			}

			JsonObject node = new JsonObject();
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				node.addProperty(child.getNodeName(), textOf((Element) child));
			}
			return node;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(TranslationKaptureService.class.getName(), "toJson()", e);
			return null;
		}
	}

	/** Text of an element, CDATA included - the same rule the screen's own reader uses. */
	private static String textOf(Element element) {
		StringBuffer sb = new StringBuffer();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.CDATA_SECTION_NODE
					|| child.getNodeType() == Node.TEXT_NODE) {
				if (null != child.getNodeValue()) {
					sb.append(child.getNodeValue());
				}
			}
		}
		return sb.toString();
	}

	private static boolean isElementName(String name) {
		return null != name && name.matches("[A-Za-z_][A-Za-z0-9_.-]*");
	}

	// ------------------------------------------------------------------ helpers

	/** latest-article wraps its payload in "response" on some deployments and not on others. */
	private static JsonObject responseBody(String body) {
		try {
			if (null == body || "".equals(body.trim())) {
				return null;
			}
			JsonElement parsed = new JsonParser().parse(body);
			if (null == parsed || !parsed.isJsonObject()) {
				return null;
			}
			JsonObject object = parsed.getAsJsonObject();
			if (object.has("response") && object.get("response").isJsonObject()) {
				return object.getAsJsonObject("response");
			}
			return object;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(TranslationKaptureService.class.getName(),
					"responseBody()", e);
			return null;
		}
	}

	static String stringOf(JsonObject object, String field) {
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

	private static String textOf(JsonElement value) {
		if (null == value || value.isJsonNull()) {
			return "";
		}
		return value.isJsonPrimitive() ? value.getAsString() : value.toString();
	}

	static String defaulted(String preferred, String fallback) {
		return (null == preferred || "".equals(preferred.trim()) || "NaN".equalsIgnoreCase(preferred))
				? (null == fallback ? "" : fallback) : preferred;
	}

	/** Everything worth telling business about a failed call, in one line. */
	private static String describe(KaptureApiResult api) {
		if (null == api) {
			return "No response from Kapture.";
		}
		List<String> parts = new ArrayList<String>();
		if (api.httpStatus > 0) {
			parts.add("HTTP " + api.httpStatus);
		}
		if (null != api.message && !"".equals(api.message.trim())) {
			parts.add(api.message.trim());
		}
		if (null != api.body && !"".equals(api.body.trim())) {
			String body = api.body.trim();
			parts.add(body.length() > 500 ? body.substring(0, 500) + "..." : body);
		}
		return parts.isEmpty() ? "No detail returned." : join(parts, " :: ");
	}

	private static String join(List<String> parts, String separator) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				sb.append(separator);
			}
			sb.append(parts.get(i));
		}
		return sb.toString();
	}
}
