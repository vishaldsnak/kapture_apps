package com.mazda.gms3.dmt.kapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clean-up applied to the HTML attribute values of a document before they are sent, so that
 * Kapture's input check does not reject the document. Same rules as the bulk content import
 * job, which loaded the existing documents:
 *   - removes whole script blocks, and meta / iframe / object / embed / link tags - except the
 *     stylesheet links the conversion itself adds, as legacy did (ConversionUtils.isConversionStyleLink:
 *     content.css / contents.css of an HTML document, the MAZDA css of an ENT document)
 *   - removes inline event-handler attributes (every on* when configured so)
 *   - removes any attribute whose value is an executable URL (javascript: / vbscript:)
 * Text, hrefs and img srcs are kept.
 *
 *   kapture.sanitize.strip.tags                 = meta,script,iframe,object,embed,link
 *   kapture.sanitize.strip.attributes           = onclick,onload,onerror
 *   kapture.sanitize.strip.all.event.attributes = TRUE
 *   kapture.sanitize.strip.url.schemes          = javascript,vbscript
 */
public class KaptureHtmlSanitizer {

	private final List<Pattern> tagRemovers = new ArrayList<Pattern>();
	private final List<Pattern> attrRemovers = new ArrayList<Pattern>();
	/** true for the href of a <link> that is sent although link tags are stripped */
	private final Predicate<String> keepLink;

	/**
	 * @param keepLink true for the href of a stylesheet link that is kept when link tags are stripped
	 *                 (ConversionUtils::isConversionStyleLink), or null to strip every link
	 */
	public KaptureHtmlSanitizer(Predicate<String> keepLink) {
		this.keepLink = keepLink;
		String tags = KaptureApiClient.require("kapture.sanitize.strip.tags");
		String attrs = KaptureApiClient.require("kapture.sanitize.strip.attributes");
		boolean allEvents = "TRUE".equalsIgnoreCase(
				KaptureApiClient.require("kapture.sanitize.strip.all.event.attributes"));
		String schemes = KaptureApiClient.require("kapture.sanitize.strip.url.schemes");

		for (String t : split(tags)) {
			String q = Pattern.quote(t);
			// paired form first: <tag ...>...</tag> - removed WITH its content
			tagRemovers.add(Pattern.compile("<" + q + "\\b[^>]*>.*?</" + q + "\\s*>",
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
			// then any remaining opening / void / self-closing tag
			tagRemovers.add(Pattern.compile("<" + q + "\\b[^>]*/?>", Pattern.CASE_INSENSITIVE));
		}
		for (String a : split(attrs)) {
			String q = Pattern.quote(a);
			attrRemovers.add(Pattern.compile("\\s*" + q + "\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
					Pattern.CASE_INSENSITIVE));
		}
		if (allEvents) {
			attrRemovers.add(Pattern.compile("\\s*on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
					Pattern.CASE_INSENSITIVE));
		}
		for (String s : split(schemes)) {
			String q = Pattern.quote(s);
			// <attr>="javascript:..." -> drop the attribute, keep the element and its text
			attrRemovers.add(Pattern.compile(
					"\\s*[A-Za-z_:][-A-Za-z0-9_:.]*\\s*=\\s*"
							+ "(\"\\s*" + q + ":[^\"]*\"|'\\s*" + q + ":[^']*'|" + q + ":[^\\s>]*)",
					Pattern.CASE_INSENSITIVE));
		}
	}

	public String sanitize(String html) {
		if (null == html || "".equals(html)) {
			return html;
		}
		String s = html;
		for (Pattern p : tagRemovers) {
			Matcher m = p.matcher(s);
			StringBuffer out = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(out, isKeptLink(m.group()) ? Matcher.quoteReplacement(m.group()) : "");
			}
			m.appendTail(out);
			s = out.toString();
		}
		for (Pattern p : attrRemovers) {
			s = p.matcher(s).replaceAll("");
		}
		return s;
	}

	private boolean isKeptLink(String tag) {
		return null != keepLink && tag.regionMatches(true, 0, "<link", 0, 5) && keepLink.test(href(tag));
	}

	private static final Pattern HREF = Pattern.compile("\\bhref\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);

	private static String href(String tag) {
		Matcher m = HREF.matcher(tag);
		if (!m.find()) {
			return null;
		}
		String v = null != m.group(2) ? m.group(2) : null != m.group(3) ? m.group(3) : m.group(4);
		return v.trim();
	}

	private static List<String> split(String csv) {
		List<String> out = new ArrayList<String>();
		for (String p : csv.split(",")) {
			String t = p.trim().toLowerCase(Locale.US);
			if (!"".equals(t)) {
				out.add(t);
			}
		}
		return out;
	}
}
