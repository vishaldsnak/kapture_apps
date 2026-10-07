package com.mazda.gms3.dmt.kapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;

/**
 * OTHER MANUAL LINKS (InfoManager search links) -> KAPTURE SEARCH LINKS.
 *
 *   index?page=result_mc&startover=y&qstr=qstr&fac=...&question_box=X   -> result_mc?question_box=X
 *
 * Same rule table and the same rules as the SEARCH rules of the bulk content import job (Job 10),
 * read from application.properties:
 *
 *   OTHER_MANUAL_LINK_RULES                 = result_mme,result_mc,result   (ordered, first match wins)
 *   OTHER_MANUAL_LINK_RULE.<name>.match     = index?page=result_mc&
 *   OTHER_MANUAL_LINK_RULE.<name>.target    = result_mc?question_box={question_box}
 *
 * A {name} in the target is filled from the query parameter of that name on the source link; the
 * rest (host, page, startover, qstr, fac) is InfoManager routing and is dropped. page=result is a
 * prefix of page=result_mc / result_mme, so every match carries the "&" that follows it and the
 * rules are listed most specific first. A rule named in the list must be fully configured.
 */
public class OtherManualLinkRules {

	private static Logger logger = LogManager.getLogger(OtherManualLinkRules.class);

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)\\}");
	private static volatile OtherManualLinkRules instance;

	/** One InfoManager shape -> Kapture shape. */
	public static final class Rule {
		public final String name, match, target, targetPrefix;

		Rule(String name, String match, String target) {
			this.name = name;
			this.match = match;
			this.target = target;
			int p = target.indexOf('{');
			this.targetPrefix = p < 0 ? target : target.substring(0, p);
		}

		@Override
		public String toString() {
			return name + " [" + match + " -> " + target + "]";
		}
	}

	private final List<Rule> rules;

	private OtherManualLinkRules() {
		List<Rule> list = new ArrayList<Rule>();
		for (String n : KaptureApiClient.require("OTHER_MANUAL_LINK_RULES").split(",")) {
			String name = n.trim();
			if ("".equals(name)) {
				continue;
			}
			String base = "OTHER_MANUAL_LINK_RULE." + name + ".";
			list.add(new Rule(name, KaptureApiClient.require(base + "match").toLowerCase(Locale.US),
					KaptureApiClient.require(base + "target")));
		}
		this.rules = Collections.unmodifiableList(list);
		logger.info("OtherManualLinkRules :: " + rules.size() + " rule(s) loaded :: " + rules);
	}

	/** The rules from application.properties; a missing key throws IllegalStateException. */
	public static OtherManualLinkRules get() {
		if (null == instance) {
			synchronized (OtherManualLinkRules.class) {
				if (null == instance) {
					instance = new OtherManualLinkRules();
				}
			}
		}
		return instance;
	}

	/** First rule whose match appears in the href, or null when the href is not an other manual link. */
	public Rule match(String href) {
		if (null == href) {
			return null;
		}
		String u = unescape(href).toLowerCase(Locale.US);
		for (Rule r : rules) {
			if (u.contains(r.match)) {
				return r;
			}
		}
		return null;
	}

	/** The Kapture link for an href that {@link #match} accepted; a #fragment is carried over. */
	public String apply(Rule rule, String href) {
		String u = unescape(href.trim());
		int hash = u.indexOf('#');
		String fragment = hash >= 0 ? u.substring(hash) : "";
		String query = hash >= 0 ? u.substring(0, hash) : u;
		StringBuffer out = new StringBuffer();
		Matcher m = PLACEHOLDER.matcher(rule.target);
		while (m.find()) {
			String value = param(query, m.group(1));
			if (null == value) {
				logger.warn("OtherManualLinkRules :: rule '" + rule.name + "' wants {" + m.group(1)
						+ "} but the link carries no such parameter - left empty :: " + href);
				value = "";
			}
			m.appendReplacement(out, Matcher.quoteReplacement(value));
		}
		m.appendTail(out);
		return out.toString() + fragment;
	}

	/** The rule whose Kapture link this href is (already rewritten), or null. Used by the preview. */
	public Rule rewritten(String href) {
		if (null == href) {
			return null;
		}
		String u = href.trim();
		for (Rule r : rules) {
			if (u.startsWith(r.targetPrefix)) {
				return r;
			}
		}
		return null;
	}

	private static String param(String url, String name) {
		Matcher m = Pattern.compile("[?&]" + Pattern.quote(name) + "=([^&#]*)", Pattern.CASE_INSENSITIVE).matcher(url);
		return m.find() ? m.group(1) : null;
	}

	/** The same link turns up with & and with &amp; (even &amp;amp;) - normalise before matching. */
	private static String unescape(String href) {
		String s = href;
		for (int i = 0; i < 5 && s.indexOf("&amp;") >= 0; i++) {
			s = s.replace("&amp;", "&");
		}
		return s;
	}
}
