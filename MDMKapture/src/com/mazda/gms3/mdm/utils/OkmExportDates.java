package com.mazda.gms3.mdm.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * HEADER DATES OF THE EXPORTED XML IN THE OKM FORMAT (user 2026-11-25; replaces the 2026-11-12 "dates as is" rule for
 * the header). RMI Export, MNAO Data Export and the Bulk Extract.
 *
 * Read (Kapture values, all UTC):
 *  - ISO with a zone or Z (displayOn, publishedDate: 2026-10-25T13:45:28.381294Z)
 *  - ISO without a zone (some dateModified: 2026-09-13T07:02:52.411131648) - UTC
 *  - yyyy-MM-dd HH:mm:ss &lt;zone&gt; (already the OKM form) and yyyy-MM-dd HH:mm:ss (UTC)
 *  - k_article text (createdDate, dateModified) by the DOCUMENT locale, as Kapture writes it (confirmed 24-Nov-2026,
 *    same rule as KaptureDocumentDates / kauthor-api KaptureArticleDates): en_US MM/dd/yyyy, ja_JP yyyy-MM-dd, every
 *    other locale dd/MM/yyyy, each with or without HH:mm:ss. dd/MM and MM/dd are never guessed from the value.
 * Written (okm.export.date.* keys, no defaults in code):
 *  - TIMESTAMP       okm.export.date.timestamp.pattern in okm.export.date.timestamp.zone (2018-12-17 18:17:02 Etc/GMT)
 *  - _MILLIS         epoch milliseconds of the same instant
 *  - local DATE      okm.export.date.local.zone, month-first pattern for okm.export.date.local.date.month.first.locales,
 *                    else the day-first pattern (OKM: en_US 10/25/2024, en_UK 18/12/2018, Japan time)
 *  - local TIME      okm.export.date.local.time.pattern in the local zone (OKM: 08:42 AM)
 * A value that cannot be read is written as is in the TIMESTAMP node; the derived nodes are then blank.
 *
 * SAME SOURCE in MDMKapture (com.mazda.gms3.mdm.utils) and NewMazdaDataExportTool (com.mazda.data.export.utils):
 * only the package line differs. Change both.
 */
public final class OkmExportDates {

	/** Kapture's own locale literal for its month-first k_article format (KaptureDocumentDates). */
	private static final String KAPTURE_MONTH_FIRST_LOCALE = "en_US";

	private static final DateTimeFormatter[] KAPTURE_MONTH_FIRST = { strict("MM/dd/uuuu HH:mm:ss"), strict("MM/dd/uuuu") };

	private static final DateTimeFormatter[] KAPTURE_DAY_FIRST = { strict("dd/MM/uuuu HH:mm:ss"), strict("dd/MM/uuuu") };

	private static final DateTimeFormatter ZONED = strict("uuuu-MM-dd HH:mm:ss VV");

	/** ja_JP k_article text (and the old k_article published_date), UTC; year-first cannot be misread. */
	private static final DateTimeFormatter PLAIN = strict("uuuu-MM-dd HH:mm:ss");

	private static final DateTimeFormatter PLAIN_DATE = strict("uuuu-MM-dd");

	/** Keys the Bulk Extract needs (TIMESTAMP only). */
	public static final String[] TIMESTAMP_KEYS = { "okm.export.date.timestamp.pattern", "okm.export.date.timestamp.zone" };

	/** Keys RMI Export / MNAO Data Export need in addition (the derived local DATE / TIME nodes). */
	public static final String[] LOCAL_KEYS = { "okm.export.date.local.zone", "okm.export.date.local.time.pattern",
			"okm.export.date.local.date.month.first.locales", "okm.export.date.local.date.month.first.pattern",
			"okm.export.date.local.date.day.first.pattern" };

	private final DateTimeFormatter timestamp;
	private final ZoneId timestampZone;
	private final ZoneId localZone;
	private final DateTimeFormatter localTime;
	private final DateTimeFormatter monthFirstDate;
	private final DateTimeFormatter dayFirstDate;
	private final List<String> monthFirstLocales = new ArrayList<String>();

	/**
	 * @param props required-key lookup: returns the trimmed value or throws when the key is missing / blank
	 * @param withLocal true to also read the local DATE / TIME keys (RMI, MNAO); false for TIMESTAMP only (Bulk)
	 */
	public OkmExportDates(Function<String, String> props, boolean withLocal) {
		this.timestamp = DateTimeFormatter.ofPattern(props.apply(TIMESTAMP_KEYS[0]), Locale.US);
		this.timestampZone = ZoneId.of(props.apply(TIMESTAMP_KEYS[1]));
		if (withLocal) {
			this.localZone = ZoneId.of(props.apply(LOCAL_KEYS[0]));
			this.localTime = DateTimeFormatter.ofPattern(props.apply(LOCAL_KEYS[1]), Locale.US);
			for (String locale : props.apply(LOCAL_KEYS[2]).split(",")) {
				if (!"".equals(locale.trim())) {
					monthFirstLocales.add(locale.trim());
				}
			}
			this.monthFirstDate = DateTimeFormatter.ofPattern(props.apply(LOCAL_KEYS[3]), Locale.US);
			this.dayFirstDate = DateTimeFormatter.ofPattern(props.apply(LOCAL_KEYS[4]), Locale.US);
		} else {
			this.localZone = null;
			this.localTime = null;
			this.monthFirstDate = null;
			this.dayFirstDate = null;
		}
	}

	/** The Kapture value as an instant; null when blank or not in a known form. */
	public static Instant read(String value, String documentLocale) {
		if (null == value || "".equals(value.trim())) {
			return null;
		}
		String v = value.trim();
		try {
			return OffsetDateTime.parse(v, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
		} catch (Exception notThisForm) {
			// next form
		}
		try {
			return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
		} catch (Exception notThisForm) {
			// next form
		}
		try {
			return ZonedDateTime.parse(v, ZONED).toInstant();
		} catch (Exception notThisForm) {
			// next form
		}
		try {
			return LocalDateTime.parse(v, PLAIN).toInstant(ZoneOffset.UTC);
		} catch (Exception notThisForm) {
			// next form
		}
		try {
			return LocalDate.parse(v, PLAIN_DATE).atStartOfDay().toInstant(ZoneOffset.UTC);
		} catch (Exception notThisForm) {
			// next form
		}
		boolean monthFirst = null != documentLocale && KAPTURE_MONTH_FIRST_LOCALE.equals(documentLocale.trim());
		DateTimeFormatter[] slash = monthFirst ? KAPTURE_MONTH_FIRST : KAPTURE_DAY_FIRST;
		try {
			return LocalDateTime.parse(v, slash[0]).toInstant(ZoneOffset.UTC);
		} catch (Exception notThisForm) {
			// next form
		}
		try {
			return LocalDate.parse(v, slash[1]).atStartOfDay().toInstant(ZoneOffset.UTC);
		} catch (Exception notThisForm) {
			return null;
		}
	}

	/** TIMESTAMP node: the OKM form, or the value as is when it cannot be read; null when blank. */
	public String timestamp(String value, String documentLocale) {
		Instant instant = read(value, documentLocale);
		if (null == instant) {
			return null == value || "".equals(value.trim()) ? null : value.trim();
		}
		return timestamp.format(instant.atZone(timestampZone));
	}

	/**
	 * TIMESTAMP, _MILLIS, local DATE, local TIME of one Kapture value, for a block of up to four nodes. Unreadable:
	 * { value as is, null, null, null }; blank: all null.
	 */
	public String[] block(String value, String documentLocale) {
		Instant instant = read(value, documentLocale);
		if (null == instant) {
			return new String[] { null == value || "".equals(value.trim()) ? null : value.trim(), null, null, null };
		}
		ZonedDateTime local = instant.atZone(localZone);
		DateTimeFormatter date = null != documentLocale && monthFirstLocales.contains(documentLocale.trim()) ? monthFirstDate : dayFirstDate;
		return new String[] { timestamp.format(instant.atZone(timestampZone)), String.valueOf(instant.toEpochMilli()),
				date.format(local), localTime.format(local) };
	}

	private static DateTimeFormatter strict(String pattern) {
		return DateTimeFormatter.ofPattern(pattern, Locale.US).withResolverStyle(ResolverStyle.STRICT);
	}
}
