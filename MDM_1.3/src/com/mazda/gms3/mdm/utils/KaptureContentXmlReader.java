package com.mazda.gms3.mdm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;

/**
 * READS DOCUMENT ATTRIBUTES OUT OF THE XML KAPTURE WRITES FOR EVERY DOCUMENT VERSION.
 *
 * WHY THIS EXISTS: the old code fetched attribute values from OK_IM.CONTENTVALUE, selecting them
 * by XPATH (see FetchIMDataDAO.identifySIDocumentTypeDetails / getDescription). Kapture has no
 * equivalent table - the same values live in the generated document XML, under the SAME XPaths.
 * So the XPath strings below are carried over VERBATIM and evaluated against the file instead of
 * matched against a column.
 *
 * MISSING VALUES ARE NOT AN ERROR. Which attributes a document carries depends on its SI
 * sub-type, and a document may legitimately have none of them - e.g. a TECHNICAL_INFORMATION
 * document has no TI_NUMBER and no DESCRIPTION. Every getter returns "" in that case, per the
 * migration decision to pass such fields through as blank.
 *
 * FILE LAYOUT (root comes from kapture.content.xml.root):
 *   <root>/<live|staging>/<CHANNEL>/<bucket>/<articleId>/<locale>/<version>/content_<contentid>.xml
 *
 * - live for a PUBLISHED document (at its published_version), staging for an UNPUBLISHED one
 *   (at its working article_version).
 * - CHANNEL is the leading alphabetic part of the article id - SI1123 -> SI.
 * - bucket is a numbered folder FOUND BY SEARCHING for the document, never calculated. See
 *   bucketFor().
 * - THE FILE NAME IS PER-CHANNEL, NOT PER-DOCUMENT: every SI document's XML is called
 *   content_5481c2d8-....xml because contentid identifies the channel. The FOLDER PATH is what
 *   identifies the document, so this class matches content_*.xml rather than building the name.
 */
public class KaptureContentXmlReader {

	static Logger logger = LogManager.getLogger(KaptureContentXmlReader.class);

	/* XPaths carried over verbatim from FetchIMDataDAO. */
	public static final String XPATH_DESCRIPTION = "//SERVICE_INFORMATION/DESCRIPTION";
	public static final String XPATH_ISSUE_DATE = "//SERVICE_INFORMATION/TSB_ISSUE_DATE";
	public static final String XPATH_FIRST_PUBLICATION_DATE = "//SERVICE_INFORMATION/FIRST_PUBLICATION_DATE";
	public static final String XPATH_CAMPAIGN_NUMBER = "//SERVICE_INFORMATION/CAMPAIGN_NUMBER";
	public static final String XPATH_TSB_NUMBER = "//SERVICE_INFORMATION/TSB_NUMBER";
	public static final String XPATH_MTIPS_NUMBER = "//SERVICE_INFORMATION/MTIPS_NUMBER";
	public static final String XPATH_SA_NUMBER = "//SERVICE_INFORMATION/SA_NUMBER";
	public static final String XPATH_TI_NUMBER = "//SERVICE_INFORMATION/TI_NUMBER";

	private Document document = null;
	private String resolvedPath = null;

	private KaptureContentXmlReader(Document document, String resolvedPath) {
		this.document = document;
		this.resolvedPath = resolvedPath;
	}

	/**
	 * Locates and parses the XML for one document version.
	 *
	 * NEVER THROWS and never returns null - a reader over a missing or unparseable file simply
	 * answers "" to every attribute, so a caller can use it without null checks and the screen
	 * degrades to blank attributes instead of failing.
	 *
	 * @param documentId article id, e.g. "SI1123"
	 * @param locale     document locale, e.g. "en_US"
	 * @param version    published_version when published, else article_version
	 * @param published  true -> read from live, false -> read from staging
	 */
	public static KaptureContentXmlReader open(String documentId, String locale, String version,
			boolean published) {
		return openAt(buildFolderPath(documentId, locale, version, published));
	}

	/**
	 * As open(), but for a folder the CALLER has already resolved.
	 *
	 * Exists for CD Creation, which cannot simply be told live-or-staging: it needs the published
	 * version out of live but has to fall back to staging when live holds only an empty version
	 * folder, and to a directory scan when the bucket does not compute. That search belongs with
	 * the screen that needs it, not in here - so it hands over the folder it settled on.
	 */
	public static KaptureContentXmlReader openAt(String path) {
		Document doc = null;
		String usedFile = null;
		InputStream is = null;
		try {
			if (null != path) {
				File folder = new File(path);
				File xmlFile = findContentXml(folder);
				if (null != xmlFile) {
					usedFile = xmlFile.getAbsolutePath();
					/*
					 * Parsed from a stream and NOT from a File/URI so the encoding declared in the
					 * prolog (the files are ISO-8859-15) is honoured by the parser rather than
					 * guessed from the platform default.
					 */
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setNamespaceAware(false);
					// the content is generated internally, but there is no reason to resolve
					// anything external while parsing it
					trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
					trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
					trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
					DocumentBuilder builder = factory.newDocumentBuilder();
					is = new FileInputStream(xmlFile);
					doc = builder.parse(is);
					doc.getDocumentElement().normalize();
				} else {
					logger.info("KaptureContentXmlReader :: no content_*.xml under {" + path + "}");
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(), "open()", e);
			doc = null;
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (Exception ignore) {
					// nothing useful to do here
				}
			}
		}
		return new KaptureContentXmlReader(doc, usedFile);
	}

	/** True when an XML was actually found and parsed. */
	public boolean isAvailable() {
		return null != document;
	}

	/** Absolute path of the file that was read, or null when none was found. */
	public String getResolvedPath() {
		return resolvedPath;
	}

	/**
	 * Evaluates one of the XPaths above.
	 *
	 * @return the element's text, or "" when the document is unavailable or the element is absent
	 */
	public String getValue(String xPathExpression) {
		String value = "";
		try {
			if (null != document && null != xPathExpression && !"".equals(xPathExpression.trim())) {
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node node = (Node) xPath.evaluate(xPathExpression, document, XPathConstants.NODE);
				if (null != node && null != node.getTextContent()) {
					value = node.getTextContent().trim();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(), "getValue()", e);
			value = "";
		}
		return value;
	}

	/** Convenience for the attributes the SI VIN screens use. Blank when absent. */
	public String getDescription() {
		return getValue(XPATH_DESCRIPTION);
	}

	public String getIssueDate() {
		return getValue(XPATH_ISSUE_DATE);
	}

	public String getFirstPublicationDate() {
		return getValue(XPATH_FIRST_PUBLICATION_DATE);
	}

	/**
	 * The document's "SI number", whose element depends on the SI sub-type. The mapping is
	 * carried over verbatim from FetchIMDataDAO.identifySIDocumentTypeDetails().
	 *
	 * @param documentTypeRefKey CAMPAIGN / TECHNICAL_SERVICE_BULLETIN / M_TIPS / SERVICE_ALERT /
	 *                           TECHNICAL_INFORMATION
	 */
	public String getSINumber(String documentTypeRefKey) {
		String xPath = getSINumberXPath(documentTypeRefKey);
		if (null == xPath || "".equals(xPath)) {
			return "";
		}
		return getValue(xPath);
	}

	/** Exposed separately so callers can log which XPath was chosen, as the old code did. */
	public static String getSINumberXPath(String documentTypeRefKey) {
		if (null == documentTypeRefKey || "".equals(documentTypeRefKey.trim())) {
			return "";
		}
		String key = documentTypeRefKey.trim().toLowerCase();
		if (key.equals("CAMPAIGN".toLowerCase())) {
			return XPATH_CAMPAIGN_NUMBER;
		} else if (key.equals("TECHNICAL_SERVICE_BULLETIN".toLowerCase())) {
			return XPATH_TSB_NUMBER;
		} else if (key.equals("M_TIPS".toLowerCase())) {
			return XPATH_MTIPS_NUMBER;
		} else if (key.equals("SERVICE_ALERT".toLowerCase())) {
			return XPATH_SA_NUMBER;
		} else if (key.equals("TECHNICAL_INFORMATION".toLowerCase())) {
			return XPATH_TI_NUMBER;
		}
		return "";
	}

	/**
	 * SERIALIZES A WHOLE ELEMENT BACK TO XML, the element itself included.
	 *
	 * This is what makes the file a drop-in replacement for OK_IM.CONTENTDATA.XML: the channel
	 * node inside Kapture's document XML - SERVICE_MANUALS, WIRING_DIAGRAMS - has the same shape
	 * InfoManager stored, so handing its subtree back as a String gives the old parsers exactly
	 * what they used to read out of the column.
	 *
	 * ESCAPING SURVIVES THE ROUND TRIP. CONTENT holds HTML as escaped text (&amp;lt;p&amp;gt;); the
	 * transformer re-escapes it identically, so a later getTextContent() returns the HTML intact.
	 *
	 * @return the element's XML, or "" when the document is unavailable or has no such element
	 */
	public String getNodeXml(String tagName) {
		try {
			Node node = firstNode(tagName);
			if (null == node) {
				return "";
			}
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			// the fragment is embedded in a String, so a second <?xml ...?> prolog would be wrong
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(), "getNodeXml()", e);
			return "";
		}
	}

	/** Text of the first element with this name - "" when absent. */
	public String getTextOf(String tagName) {
		Node node = firstNode(tagName);
		if (null == node || null == node.getTextContent()) {
			return "";
		}
		return node.getTextContent().trim();
	}

	/** True when the document carries at least one element with this name. */
	public boolean hasNode(String tagName) {
		return null != firstNode(tagName);
	}

	/**
	 * First element with this name, searched from the root.
	 *
	 * Matched by TAG NAME rather than by XPath because the caller may be passing a name that came
	 * out of the file itself (the type element), which must not be interpolated into an expression.
	 */
	private Node firstNode(String tagName) {
		if (null == document || null == tagName || "".equals(tagName.trim())) {
			return null;
		}
		try {
			Element root = document.getDocumentElement();
			if (null == root) {
				return null;
			}
			NodeList nodes = root.getElementsByTagName(tagName.trim());
			if (null != nodes && nodes.getLength() > 0) {
				return nodes.item(0);
			}
			if (root.getNodeName().equals(tagName.trim())) {
				return root;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(), "firstNode()", e);
		}
		return null;
	}

	/**
	 * Builds the folder that holds the document version's XML. Public so it can be logged or
	 * asserted in isolation.
	 *
	 * @return absolute folder path, or null when the inputs cannot form one
	 */
	public static String buildFolderPath(String documentId, String locale, String version,
			boolean published) {
		try {
			if (null == documentId || "".equals(documentId.trim()) || null == locale
					|| "".equals(locale.trim()) || null == version || "".equals(version.trim())) {
				logger.info("buildFolderPath :: documentId / locale / version missing - documentId {"
						+ documentId + "} locale {" + locale + "} version {" + version + "}");
				return null;
			}
			String root = ApplicationProperties.getProperty("kapture.content.xml.root");
			if (null == root || "".equals(root.trim())) {
				logger.info("buildFolderPath :: kapture.content.xml.root is not set.");
				return null;
			}

			String channel = getChannel(documentId);
			String bucket = bucketFor(documentId, published);
			if (null == channel || null == bucket) {
				logger.info("buildFolderPath :: could not derive channel / bucket from {"
						+ documentId + "}");
				return null;
			}

			StringBuffer sb = new StringBuffer();
			sb.append(root.trim());
			if (!root.trim().endsWith("/") && !root.trim().endsWith(File.separator)) {
				sb.append(File.separator);
			}
			sb.append(published ? "live" : "staging").append(File.separator);
			sb.append(channel).append(File.separator);
			sb.append(bucket).append(File.separator);
			sb.append(documentId.trim()).append(File.separator);
			sb.append(locale.trim().replace("-", "_")).append(File.separator);
			sb.append(version.trim());
			return sb.toString();
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(),
					"buildFolderPath()", e);
			return null;
		}
	}

	/** Leading alphabetic part of the article id - SI1123 -> SI, OSM1004 -> OSM. */
	public static String getChannel(String documentId) {
		if (null == documentId) {
			return null;
		}
		String id = documentId.trim();
		int i = 0;
		while (i < id.length() && Character.isLetter(id.charAt(i))) {
			i++;
		}
		return i > 0 ? id.substring(0, i) : null;
	}

	/** Bucket folder names per channel, keyed "live|SM". Refreshed on a miss - see bucketFor(). */
	private static final Map<String, List<String>> BUCKET_CACHE = new HashMap<String, List<String>>();

	/** Resolved buckets per document, keyed "live|SM1027" - a document never moves between them. */
	private static final Map<String, String> DOCUMENT_BUCKET_CACHE = new HashMap<String, String>();

	/**
	 * THE BUCKET FOLDER FOR A DOCUMENT, FOUND BY LOOKING FOR THE DOCUMENT ITSELF.
	 *
	 * WHY IT IS A SEARCH AND NOT A CALCULATION. The obvious implementation floors the article
	 * number to a fixed step, and that is what this used to do, driven by a configured step size.
	 * That encoded a guess - that every channel buckets on the same step from the same origin - and
	 * nothing guarantees it: boundaries may start at 0, step by 100 in one channel and 1000 in
	 * another, or change over time. A wrong step produces a path that simply does not exist, and
	 * every caller reports that as "no content for this document" rather than as a fault, so the
	 * export comes up short with nothing to show for it.
	 *
	 * The step-size property and the getBucket() helper that read it were DELETED rather than left
	 * inert, because an obviously-named helper returning a plausible number is how the assumption
	 * would come back.
	 *
	 * SEARCHING CANNOT BE WRONG IN THAT WAY. The channel's bucket folders are examined for a
	 * folder named after the document; wherever it is found IS the bucket. No assumption about
	 * spacing, origin or per-channel consistency remains in the code.
	 *
	 * THE ORDER IS A SPEED HEURISTIC ONLY. Candidates are tried nearest-first - the bucket whose
	 * number is closest at or below the document's - so conventionally numbered trees match on the
	 * first probe. Correctness does not depend on it: every bucket is tried before giving up.
	 *
	 * TWO CACHES, because a CD build walks thousands of documents plus a recursion through inner
	 * links, and this sits on a network share. The channel listing is read once; each document's
	 * answer is remembered. A listing that goes stale costs one re-read on the miss, not a wrong
	 * answer.
	 *
	 * @param published true to search live, false to search staging - a channel can exist in one
	 *                  tree and not the other, so they are listed separately
	 * @return the bucket folder name, or null when no bucket holds the document
	 */
	public static String bucketFor(String documentId, boolean published) {
		if (null == documentId || "".equals(documentId.trim())) {
			return null;
		}
		String tree = published ? "live" : "staging";
		String docId = documentId.trim();
		String cacheKey = tree + "|" + docId;
		try {
			synchronized (DOCUMENT_BUCKET_CACHE) {
				if (DOCUMENT_BUCKET_CACHE.containsKey(cacheKey)) {
					return DOCUMENT_BUCKET_CACHE.get(cacheKey);
				}
			}

			String bucket = searchForDocument(docId, published, false);
			if (null == bucket) {
				// the listing may predate the folder - read it again before giving up
				bucket = searchForDocument(docId, published, true);
			}
			if (null == bucket) {
				logger.info("bucketFor :: no bucket folder under " + tree + " holds {" + docId + "}");
			}
			synchronized (DOCUMENT_BUCKET_CACHE) {
				DOCUMENT_BUCKET_CACHE.put(cacheKey, bucket);
			}
			return bucket;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(), "bucketFor()", e);
			return null;
		}
	}

	/** Tries every bucket folder for one named after the document, nearest-numbered first. */
	private static String searchForDocument(String documentId, boolean published,
			boolean forceReload) {
		String channel = getChannel(documentId);
		if (null == channel) {
			return null;
		}
		File channelFolder = channelFolder(channel, published);
		if (null == channelFolder) {
			return null;
		}
		List<String> buckets = bucketFolders(channel, published, forceReload);
		for (int i = 0; i < buckets.size(); i++) {
			String bucket = buckets.get(i);
			if (new File(new File(channelFolder, bucket), documentId).isDirectory()) {
				return bucket;
			}
		}
		return null;
	}

	/**
	 * The channel's bucket folders, ordered nearest-first relative to the document being sought.
	 *
	 * The order is recomputed per call because it depends on the document; the LISTING is what is
	 * cached, and that is the part that costs a round trip to the share.
	 */
	private static List<String> bucketFolders(String channel, boolean published,
			boolean forceReload) {
		String key = (published ? "live" : "staging") + "|" + channel;
		List<String> cached = null;
		synchronized (BUCKET_CACHE) {
			if (!forceReload) {
				cached = BUCKET_CACHE.get(key);
			}
		}
		if (null != cached) {
			return cached;
		}

		List<String> names = new ArrayList<String>();
		try {
			File channelFolder = channelFolder(channel, published);
			File[] entries = (null == channelFolder ? null : channelFolder.listFiles());
			if (null != entries) {
				for (int i = 0; i < entries.length; i++) {
					if (entries[i].isDirectory()) {
						names.add(entries[i].getName().trim());
					}
				}
				/*
				 * DESCENDING NUMERICALLY, so the highest bucket at or below a document's number is
				 * met early - which is the conventional layout's answer. Non-numeric folder names
				 * sort last rather than being discarded: they are not expected, but excluding them
				 * would reintroduce an assumption about what a bucket may be called.
				 */
				Collections.sort(names, new Comparator<String>() {
					public int compare(String left, String right) {
						Long l = numberOf(left);
						Long r = numberOf(right);
						if (null == l && null == r) {
							return left.compareTo(right);
						}
						if (null == l) {
							return 1;
						}
						if (null == r) {
							return -1;
						}
						return r.compareTo(l);
					}
				});
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentXmlReader.class.getName(),
					"bucketFolders()", e);
		}
		synchronized (BUCKET_CACHE) {
			BUCKET_CACHE.put(key, names);
		}
		return names;
	}

	private static File channelFolder(String channel, boolean published) {
		String root = ApplicationProperties.getProperty("kapture.content.xml.root");
		if (null == root || "".equals(root.trim())) {
			return null;
		}
		return new File(new File(root.trim(), published ? "live" : "staging"), channel);
	}

	/** Leading digits of a name as a number - "1000" -> 1000, "SM1027" -> 1027, "x" -> null. */
	private static Long numberOf(String value) {
		if (null == value) {
			return null;
		}
		try {
			String digits = value.trim().replaceAll("[^0-9]", "");
			return "".equals(digits) ? null : Long.valueOf(Long.parseLong(digits));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Picks the content XML inside a version folder. Matched by pattern rather than by name
	 * because the name carries the CHANNEL's contentid, which this code has no reason to know.
	 */
	private static File findContentXml(File folder) {
		if (null == folder || !folder.isDirectory()) {
			return null;
		}
		File[] files = folder.listFiles();
		if (null == files) {
			return null;
		}
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			if (files[i].isFile() && null != name && name.toLowerCase().startsWith("content_")
					&& name.toLowerCase().endsWith(".xml")) {
				return files[i];
			}
		}
		return null;
	}

	private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
		try {
			factory.setFeature(feature, value);
		} catch (Exception ignore) {
			// parser does not know the feature - not worth failing the read over
		}
	}
}
