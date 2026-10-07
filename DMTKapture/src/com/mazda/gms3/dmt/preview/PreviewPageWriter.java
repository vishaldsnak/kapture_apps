package com.mazda.gms3.dmt.preview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mazda.gms3.dmt.kapture.OtherManualLinkRules;

/**
 * TURNS THE CONTENT OF A DOCUMENT (as sent to Kapture) INTO ITS OFFLINE PREVIEW PAGE.
 *
 *   /library/...         (images, css, js in OKAssets)  -> <okassets web context>/library/...
 *   content/<id>[#anchor] (link to another document)    -> <id>.html[#anchor], marked data-dmt-link
 *   content/             (link whose document was not found) -> no link, marked data-dmt-unmapped
 *   result_mc?question_box=... (other manual link, OtherManualLinkRules) -> no link, data-dmt-other = that link
 *
 * The page loads _assets/preview_page.js, which hands every document link to the Preview page:
 * the Preview page opens it when the document is part of the job, else says it is not.
 */
public class PreviewPageWriter {

	public static final String ASSETS_FOLDER = "_assets";
	public static final String PAGE_SCRIPT = "preview_page.js";

	/**
	 * The content of _assets/preview_page.js, written into every job folder. Kept in the class (not
	 * as a file on the classpath) so the preview never depends on how the war was packaged.
	 * A link to another document (data-dmt-link) is not followed by the page itself: it is handed to
	 * the Preview page, which opens the document when it is part of the job and otherwise says it is
	 * not. The page also tells the Preview page which document it shows. postMessage works whatever
	 * host serves the OKAssets folder.
	 */
	public static final String PAGE_SCRIPT_SOURCE = ""
			+ "(function () {\n"
			+ "\tfunction send(message) {\n"
			+ "\t\ttry {\n"
			+ "\t\t\tif (window.parent && window.parent !== window) {\n"
			+ "\t\t\t\twindow.parent.postMessage(message, \"*\");\n"
			+ "\t\t\t}\n"
			+ "\t\t} catch (e) {\n"
			+ "\t\t}\n"
			+ "\t}\n"
			+ "\tvar root = document.documentElement;\n"
			+ "\tsend({ dmtPreview: \"shown\", id: root.getAttribute(\"data-dmt-doc\") });\n"
			+ "\tdocument.addEventListener(\"click\", function (ev) {\n"
			+ "\t\tvar a = ev.target;\n"
			+ "\t\twhile (a && a.nodeName !== \"A\") {\n"
			+ "\t\t\ta = a.parentNode;\n"
			+ "\t\t}\n"
			+ "\t\tif (!a) {\n"
			+ "\t\t\treturn;\n"
			+ "\t\t}\n"
			+ "\t\tvar id = a.getAttribute(\"data-dmt-link\");\n"
			+ "\t\tif (id) {\n"
			+ "\t\t\tev.preventDefault();\n"
			+ "\t\t\tsend({ dmtPreview: \"open\", id: id, hash: a.hash || \"\" });\n"
			+ "\t\t} else if (a.hasAttribute(\"data-dmt-other\")) {\n"
			+ "\t\t\tev.preventDefault();\n"
			+ "\t\t\tsend({ dmtPreview: \"other\", link: a.getAttribute(\"data-dmt-other\") });\n"
			+ "\t\t} else if (a.hasAttribute(\"data-dmt-unmapped\")) {\n"
			+ "\t\t\tev.preventDefault();\n"
			+ "\t\t}\n"
			+ "\t}, true);\n"
			// WD HTML5 page: the viewer opens a voltage link with window.open(<prefix><document id>)
			+ "\tvar openPrefix = root.getAttribute(\"data-dmt-open\");\n"
			+ "\tif (openPrefix) {\n"
			+ "\t\tvar realOpen = window.open;\n"
			+ "\t\twindow.open = function (url) {\n"
			+ "\t\t\tvar u = null == url ? \"\" : String(url).replace(/^\\s+|\\s+$/g, \"\");\n"
			+ "\t\t\tvar p = u.indexOf(openPrefix);\n"
			+ "\t\t\tif (p === 0 || (p === 1 && u.charAt(0) === \"/\")) {\n"
			+ "\t\t\t\tvar id = u.substring(p + openPrefix.length).split(/[?#]/)[0];\n"
			+ "\t\t\t\tif (id) {\n"
			+ "\t\t\t\t\tsend({ dmtPreview: \"open\", id: id, hash: \"\" });\n"
			+ "\t\t\t\t}\n"
			+ "\t\t\t\treturn null;\n"
			+ "\t\t\t}\n"
			+ "\t\t\treturn realOpen.apply(window, arguments);\n"
			+ "\t\t};\n"
			+ "\t}\n"
			+ "})();\n";

	private final String webContext;
	private final String library;
	private final Pattern libraryPath;
	private final String linkUrl;

	/**
	 * @param webContext  okassets.web.context, e.g. /content
	 * @param libraryPath web path of the OKAssets library folder, e.g. /library/
	 * @param linkUrl     LINK_URL - the prefix of a link to another document, e.g. content/
	 */
	public PreviewPageWriter(String webContext, String libraryPath, String linkUrl) {
		this.webContext = webContext;
		this.library = libraryPath;
		// a path value starts right after a quote, a bracket of url( or the ; of an entity (&quot;)
		this.libraryPath = Pattern.compile("([\"'(;])" + Pattern.quote(libraryPath));
		this.linkUrl = linkUrl;
	}

	public String page(String documentId, String title, String content) {
		Document doc = Jsoup.parse(null == content ? "" : content);
		doc.outputSettings().charset("UTF-8");
		doc.outputSettings().prettyPrint(false);

		for (Element a : doc.select("a[href]")) {
			String href = a.attr("href").trim();
			if (null != OtherManualLinkRules.get().rewritten(href)) {
				// other manual link (Kapture search link): a dead link in the preview, the Preview page shows it
				a.removeAttr("href");
				a.removeAttr("target");
				a.attr("data-dmt-other", href);
				continue;
			}
			if (!href.startsWith(linkUrl)) {
				continue;
			}
			String rest = href.substring(linkUrl.length());
			int cut = indexOfAny(rest, "#?");
			String id = (cut < 0 ? rest : rest.substring(0, cut)).trim();
			String anchor = cut >= 0 && rest.charAt(cut) == '#' ? rest.substring(cut) : "";
			if ("".equals(id)) {
				a.removeAttr("href");
				a.attr("data-dmt-unmapped", "Y");
			} else {
				a.attr("href", id + ".html" + anchor);
				a.attr("data-dmt-link", id);
			}
		}

		Element html = doc.select("html").first();
		html.attr("data-dmt-doc", documentId);
		Element head = doc.head();
		head.prependElement("title").text(null == title ? documentId : title);
		head.prependElement("meta").attr("charset", "UTF-8");
		doc.body().appendElement("script").attr("type", "text/javascript").attr("src", ASSETS_FOLDER + "/" + PAGE_SCRIPT);

		String out = "<!DOCTYPE html>\n" + doc.outerHtml();
		return libraryPath.matcher(out).replaceAll("$1" + Matcher.quoteReplacement(webContext + library));
	}

	/**
	 * THE PREVIEW COPY OF A WIRING DIAGRAM HTML5 PAGE - the page itself is left untouched in OKAssets.
	 * The copy keeps the file name (the viewer reads it from the address bar) and gets:
	 *   <base href>          its html5 folder in OKAssets - js (voltage maps too), svg, css load from there
	 *   data-dmt-doc         the document id, as on every preview page
	 *   data-dmt-open        the voltage link prefix: window.open(<prefix><id>) is handed to the Preview page
	 *   the preview script   by its full address (the base would send a relative one to OKAssets)
	 * Only ASCII is added and the bytes are kept as read (ISO-8859-1 in and out), whatever the page
	 * encoding is.
	 */
	public static byte[] wdPage(byte[] source, String documentId, String baseHref, String scriptUrl, String openPrefix) {
		java.nio.charset.Charset bytes = java.nio.charset.StandardCharsets.ISO_8859_1;
		String s = new String(source, bytes);
		String attrs = " data-dmt-doc=\"" + attr(documentId) + "\" data-dmt-open=\"" + attr(openPrefix) + "\"";
		String head = "<base href=\"" + attr(baseHref) + "\"><script type=\"text/javascript\" src=\"" + attr(scriptUrl) + "\"></script>";

		Matcher html = Pattern.compile("<html(?=[\\s>])", Pattern.CASE_INSENSITIVE).matcher(s);
		if (html.find()) {
			s = s.substring(0, html.end()) + attrs + s.substring(html.end());
		} else {
			s = "<html" + attrs + ">" + s + "</html>";
		}
		Matcher h = Pattern.compile("<head(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE).matcher(s);
		if (h.find()) {
			s = s.substring(0, h.end()) + head + s.substring(h.end());
		} else {
			Matcher open = Pattern.compile("<html[^>]*>", Pattern.CASE_INSENSITIVE).matcher(s);
			open.find();
			s = s.substring(0, open.end()) + "<head>" + head + "</head>" + s.substring(open.end());
		}
		return s.getBytes(bytes);
	}

	private static String attr(String v) {
		return null == v ? "" : v.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
	}

	private static int indexOfAny(String s, String chars) {
		for (int i = 0; i < s.length(); i++) {
			if (chars.indexOf(s.charAt(i)) >= 0) {
				return i;
			}
		}
		return -1;
	}
}
