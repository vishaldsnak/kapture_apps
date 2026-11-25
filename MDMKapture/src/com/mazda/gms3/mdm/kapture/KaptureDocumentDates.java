package com.mazda.gms3.mdm.kapture;

/**
 * THE FORMAT OF A k_article TEXT DATE (created_date, date_added, date_modified, article_expire_date,
 * article_review_date) - and of the same dates as the read APIs return them.
 *
 * It follows the DOCUMENT locale = article_translated_locale, else article_primary_locale
 * (confirmed by Kapture 24-Nov-2026; a translation uses its OWN locale format, not its master's):
 *   en_US        -> MM/dd/yyyy HH:mm:ss
 *   ja_JP        -> yyyy-MM-dd HH:mm:ss
 *   every other  -> dd/MM/yyyy HH:mm:ss
 *
 * dd/MM and MM/dd cannot be told apart from the value for the first twelve days of a month, so
 * the slash form is ALWAYS read by the document locale, never guessed from the value.
 */
public final class KaptureDocumentDates {

	/** Kapture's own locale literal for the month-first format. */
	private static final String MONTH_FIRST_LOCALE = "en_US";

	private static final String[] MONTH_FIRST = { "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy" };

	private static final String[] DAY_FIRST = { "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy" };

	private KaptureDocumentDates() {
	}

	/**
	 * The slash patterns (with time first, then date only) a document of this locale is written in.
	 *
	 * @param documentLocale COALESCE(article_translated_locale, article_primary_locale) of the row
	 */
	public static String[] slashFormats(String documentLocale) {
		boolean monthFirst = null != documentLocale && MONTH_FIRST_LOCALE.equals(documentLocale.trim());
		return (monthFirst ? MONTH_FIRST : DAY_FIRST).clone();
	}
}
