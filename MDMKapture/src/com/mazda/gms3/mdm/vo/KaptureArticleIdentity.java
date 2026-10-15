package com.mazda.gms3.mdm.vo;

/**
 * THE k_article ROW THAT IDENTIFIES THE VERSION BEING UPDATED.
 *
 * These five values have to come from the TABLE rather than from display-Article:
 *
 *   id               - display-Article never returns it, and update-article-tab requires it. It is
 *                      NOT articleId: articleId is the document number (SI1123), id is the row's
 *                      primary key and is DIFFERENT FOR EVERY VERSION of that document.
 *   articleVersion   - display-Article returns it as a JSON NUMBER, so 1.0 arrives as 1 and would
 *                      be sent back as "1", addressing a version that does not exist. The table
 *                      holds it as text and is authoritative.
 *   publishedVersion - only sent for a published document; absent from the display response of an
 *                      unpublished one.
 *   articleState     - decides which of the two payload shapes is built.
 */
public class KaptureArticleIdentity {

	private String id = "";
	private String articleVersion = "";
	private String publishedVersion = "";
	private String articleState = "";

	/*
	 * THE LOCALE PAIR THAT SAYS WHETHER THIS ROW IS A MASTER OR A TRANSLATION, AND IT DECIDES
	 * WHICH WRITE ENDPOINT IS CALLED - SEE isTranslation().
	 *
	 * masterLocale     - k_article.master_locale, falling back to article_primary_locale. Kapture
	 *                    leaves master_locale NULL on a translation DRAFT and fills it on publish,
	 *                    and on a draft the primary locale IS the master's, so the fallback holds.
	 * effectiveLocale  - COALESCE(article_translated_locale, article_primary_locale): the locale
	 *                    this row actually BELONGS to, which is what the screens select on.
	 */
	private String masterLocale = "";
	private String effectiveLocale = "";

	/**
	 * TRUE WHEN THIS ROW IS A TRANSLATION OF ANOTHER LOCALE'S DOCUMENT.
	 *
	 * THIS IS THE BRANCH POINT FOR THE WHOLE WRITE PATH. update-article-tab HANDLES THE ORIGINAL
	 * ARTICLE ONLY - CONFIRMED BY THE KAPTURE API TEAM - AND WILL NOT POPULATE
	 * article_translated_locale, SO SENDING A TRANSLATION THROUGH IT COLLAPSES THAT ROW ONTO THE
	 * MASTER'S LOCALE AND THE TRANSLATION STOPS EXISTING. TRANSLATIONS GO TO
	 * save-translate-article INSTEAD.
	 *
	 * WHY NOT SIMPLY article_primary_locale != article_translated_locale: THAT MISSES A PUBLISHED
	 * TRANSLATION. PUBLISHING PROMOTES THE ROW TO primary = translated = ITS OWN LOCALE, so a
	 * published en_EU translation looks exactly like an en_EU master and only master_locale
	 * (en_UK) still tells them apart. Verified across all 1026 k_article rows.
	 */
	public boolean isTranslation() {
		if (null == masterLocale || "".equals(masterLocale.trim())
				|| null == effectiveLocale || "".equals(effectiveLocale.trim())) {
			// NOT ENOUGH INFORMATION - TREAT IT AS A MASTER, WHICH IS THE BEHAVIOUR WE ALREADY HAD
			return false;
		}
		return !masterLocale.trim().equalsIgnoreCase(effectiveLocale.trim());
	}

	public String getMasterLocale() {
		return masterLocale;
	}

	public void setMasterLocale(String masterLocale) {
		this.masterLocale = masterLocale;
	}

	public String getEffectiveLocale() {
		return effectiveLocale;
	}

	public void setEffectiveLocale(String effectiveLocale) {
		this.effectiveLocale = effectiveLocale;
	}

	public boolean isPublished() {
		return null != articleState && "Published".equalsIgnoreCase(articleState.trim());
	}

	public boolean isUsable() {
		return null != id && !"".equals(id.trim());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getArticleState() {
		return articleState;
	}

	public void setArticleState(String articleState) {
		this.articleState = articleState;
	}

}
