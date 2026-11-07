package com.mazda.gms3.mdm.utils;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * EXPORTED XML (RMI Export / MNAO Data Export): TEXT VALUES IN CDATA, AS IN THE LEGACY EXPORT.
 *
 * InfoManager wrote its values in CDATA, so the rich text of an exported file reads as HTML (&lt;img src=...).
 * Kapture writes the same values as XML-escaped text (&amp;lt;img src=...). Both parse to the same value; this
 * changes only how the value is written. The CATEGORIES block is built in CDATA already.
 *
 * Wrapped: every text value inside the channel node, plus the header nodes named in
 * kapture.export.xml.cdata.header.nodes. Other header nodes (dates, flags, ids) stay plain, as legacy's did.
 *
 * LAST STEP BEFORE THE FILE IS WRITTEN. The image, PDF, attachment and other manual link replaces work on the
 * escaped text (Utilities.replaceAttachmentFilePath, replaceRichTextFragment) and must have run already.
 *
 * decodeDoubleEscaped() is the FIRST step: some Kapture rich text is escaped twice (&amp;lt;img src=...), so its images
 * and links would not be identified and the CDATA would still show &lt;img. Translation Update uses it as well.
 */
public final class ExportXmlCData {

	// a value with real markup is never escaped twice: what it holds as &lt;...&gt; is literal text such as <psi>
	private static final Pattern REAL_TAG = Pattern.compile("<[A-Za-z/]");
	// a closing tag, or the start of an image / anchor tag: <CVC> or < Prev are text, not markup
	private static final Pattern ESCAPED_TAG = Pattern.compile("&lt;(?:/[a-z][a-z0-9]*&gt;|img\\s|a\\s)", Pattern.CASE_INSENSITIVE);

	private ExportXmlCData() {
	}

	/**
	 * @param xml the finished document XML
	 * @param channelName the channel node's name, e.g. SERVICE_MANUALS
	 * @return the XML with its values in CDATA, or null when it could not be converted (the document then
	 *         fails with the write failure, not the schedule)
	 */
	public static String withCData(String xml, String channelName) {
		try {
			List<String> headerNodes = Arrays.asList(ApplicationProperties.requireProperty("kapture.export.xml.cdata.header.nodes").trim().split("\\s*,\\s*"));
			InputSource source = new InputSource();
			source.setCharacterStream(new StringReader(xml));
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
			for (Node child = doc.getDocumentElement().getFirstChild(); null != child; child = child.getNextSibling()) {
				if (child.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				if (child.getNodeName().equals(channelName)) {
					wrapValues(doc, (Element) child);
				} else if (headerNodes.contains(child.getNodeName())) {
					wrapValue(doc, (Element) child);
				}
			}
			return Utilities.transformString(doc);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(ExportXmlCData.class.getName(), "withCData()", e);
			return null;
		}
	}

	/**
	 * Takes every value under the node that is escaped twice back to one level.
	 *
	 * @param channelNode the channel node of the source document
	 * @return how many values were changed
	 */
	public static int decodeDoubleEscaped(Node channelNode) {
		int decoded = 0;
		boolean hasChildElement = false;
		for (Node child = channelNode.getFirstChild(); null != child; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				hasChildElement = true;
				decoded += decodeDoubleEscaped(child);
			}
		}
		if (!hasChildElement) {
			String value = channelNode.getTextContent();
			String plain = decodeDoubleEscaped(value);
			if (null != value && !value.equals(plain)) {
				channelNode.setTextContent(plain);
				decoded++;
			}
		}
		return decoded;
	}

	/**
	 * @param value one value of a document
	 * @return the value back at one level when it is escaped twice, otherwise the value as it is
	 */
	public static String decodeDoubleEscaped(String value) {
		if (null == value || REAL_TAG.matcher(value).find() || !ESCAPED_TAG.matcher(value).find()) {
			return value;
		}
		// &amp; last - &amp;lt; is a literal < of the text and must stay one
		return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
	}

	private static void wrapValues(Document doc, Element element) {
		boolean hasChildElement = false;
		for (Node child = element.getFirstChild(); null != child; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				hasChildElement = true;
				wrapValues(doc, (Element) child);
			}
		}
		if (!hasChildElement) {
			wrapValue(doc, element);
		}
	}

	/** A blank value stays as it is. */
	private static void wrapValue(Document doc, Element element) {
		String value = element.getTextContent();
		if (null == value || "".equals(value.trim())) {
			return;
		}
		while (null != element.getFirstChild()) {
			element.removeChild(element.getFirstChild());
		}
		element.appendChild(doc.createCDATASection(value));
	}
}
