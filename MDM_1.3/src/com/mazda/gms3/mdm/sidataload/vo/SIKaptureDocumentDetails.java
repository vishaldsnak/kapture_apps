package com.mazda.gms3.mdm.sidataload.vo;

import com.google.gson.JsonObject;

/**
 * WHAT KAPTURE KNOWS ABOUT ONE SI DOCUMENT IN ONE LOCALE - the replacement for the
 * ContentRecordITO that getLatestContentRecordByDocumentIDAndLocale() used to hand back.
 *
 * Filled from a single latest-article call. Three outcomes have to be told apart and the two
 * booleans below do that, because callers must react differently to each:
 *
 *   readFailed = true                 the API could not be reached or answered an error. NOTHING is
 *                                     known about the document - it may well exist. Never treat
 *                                     this as "new document".
 *   readFailed = false, found = false the call succeeded and the document has NO VERSION in this
 *                                     locale. Kapture reports this as HTTP 200 WITH AN EMPTY BODY,
 *                                     not as a 404, so the body has to be tested and not just the
 *                                     status.
 *   found = true                      the document exists; every field below is populated.
 */
public class SIKaptureDocumentDetails {

	private boolean found = false;

	private boolean readFailed = false;

	private String errorMessage = null;

	private String documentId = null;

	/** The locale the MASTER of this document was authored in. */
	private String primaryLocale = null;

	/**
	 * THE LOCALE THIS VERSION ACTUALLY IS, master or translation.
	 *
	 * Resolved from whichever key the response carried - see SIChannelKaptureService, and note the
	 * two shapes use DIFFERENT KEY NAMES for it.
	 */
	private String effectiveLocale = null;

	/**
	 * TRUE WHEN THIS IS A TRANSLATION rather than the master identifier.
	 *
	 * The test is effectiveLocale != primaryLocale, which is what decides the WRITE ENDPOINT:
	 * a master goes to update-article-tab, a translation to save-translate-article. Sending a
	 * translation through update-article-tab does not fail - it silently collapses the translation
	 * onto the master's locale.
	 */
	private boolean translation = false;

	/**
	 * 'Published' / 'Unpublished' as Kapture reported it, OR NULL.
	 *
	 * NULL IS A REAL VALUE HERE, not a parsing miss - some translation versions come back with no
	 * articleState at all (verified on SI1125 it_IT and de_DE). isPublished() treats that as
	 * unpublished, which is the safe reading: it means "do not re-publish this".
	 */
	private String articleState = null;

	private String articleVersion = null;

	private String publishedVersion = null;

	private String title = null;

	/**
	 * k_article.id, WHEN THE RESPONSE HAPPENS TO CARRY IT - translations sometimes do, masters
	 * never. NOT TO BE RELIED ON: SI1125/en_EU returns it, SI1125/de_DE does not. Anything that
	 * needs the row id must read k_article.
	 */
	private String articleRowId = null;

	/**
	 * THE WHOLE RESPONSE, KEPT DELIBERATELY.
	 *
	 * Both Kapture write endpoints REWRITE THE ENTIRE ROW - any field the request leaves out is
	 * stored as NULL. So an update has to echo back everything the read returned and change only
	 * what it means to change, which is impossible unless the original response survives the
	 * lookup. See the payload builder.
	 */
	private JsonObject article = null;

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

	public boolean isReadFailed() {
		return readFailed;
	}

	public void setReadFailed(boolean readFailed) {
		this.readFailed = readFailed;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getPrimaryLocale() {
		return primaryLocale;
	}

	public void setPrimaryLocale(String primaryLocale) {
		this.primaryLocale = primaryLocale;
	}

	public String getEffectiveLocale() {
		return effectiveLocale;
	}

	public void setEffectiveLocale(String effectiveLocale) {
		this.effectiveLocale = effectiveLocale;
	}

	public boolean isTranslation() {
		return translation;
	}

	public void setTranslation(boolean translation) {
		this.translation = translation;
	}

	public String getArticleState() {
		return articleState;
	}

	public void setArticleState(String articleState) {
		this.articleState = articleState;
	}

	/** TRUE ONLY for an explicit 'Published'. A null or unrecognised state is NOT published. */
	public boolean isPublished() {
		return null != articleState && "Published".equalsIgnoreCase(articleState.trim());
	}

	public String getArticleVersion() {
		return articleVersion;
	}

	public void setArticleVersion(String articleVersion) {
		this.articleVersion = articleVersion;
	}

	public String getPublishedVersion() {
		return publishedVersion;
	}

	public void setPublishedVersion(String publishedVersion) {
		this.publishedVersion = publishedVersion;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArticleRowId() {
		return articleRowId;
	}

	public void setArticleRowId(String articleRowId) {
		this.articleRowId = articleRowId;
	}

	public JsonObject getArticle() {
		return article;
	}

	public void setArticle(JsonObject article) {
		this.article = article;
	}

	/** One line for the log - which document, which locale, master or translation, what state. */
	public String describe() {
		StringBuffer sb = new StringBuffer();
		sb.append("{").append(documentId).append("} / {").append(effectiveLocale).append("} ");
		/*
		 * ONLY CLAIMED WHEN IT IS KNOWN. save-translate-article's response carries no primaryLocale,
		 * so without this an unknown would print as "MASTER IDENTIFIER" on a line describing a
		 * translation.
		 */
		if (null == primaryLocale || "".equals(primaryLocale.trim())) {
			sb.append("(master/translation not stated)");
		} else {
			sb.append(translation ? "TRANSLATION of {" + primaryLocale + "}" : "MASTER IDENTIFIER");
		}
		sb.append(" version {").append(articleVersion).append("} state {");
		sb.append(null == articleState ? "none" : articleState).append("}");
		return sb.toString();
	}
}
