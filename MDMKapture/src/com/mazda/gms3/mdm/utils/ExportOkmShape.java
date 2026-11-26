package com.mazda.gms3.mdm.utils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * EXPORTED XML IN THE OKM (InfoManager) SHAPE - the files external consumers received before Kapture.
 *
 * Decided with the user 2026-11-11/12 on the test set VDI_logs/MDM_Logs/OKM_vs_KAP_Complete_XMLs, mapping workbook
 * Environment_Config/OKM_vs_Kapture_Export_Mapping.xlsx. Every name, list, pattern and constant comes from
 * application.properties (okm.export.*); a missing key fails the document, there are no defaults in code.
 *
 * CHANNEL NODE (normalizeChannel - RMI Export, MNAO Data Export and the Bulk Extract):
 *  - renames: FILE_SOURCE_LOCATION -> DJVU_FILE_LOCATION only when the document has no DJVU_FILE_LOCATION (both
 *    present = both passed).
 *  - nodes passed AS IS (okm.export.as.is.nodes, user 2026-11-12): the SM / OSM BIJLAGEN node Kapture added is
 *    written as Kapture wrote it - not renamed, no SIZE / SECURITY, no reorder; only filePath gets the exported value.
 *    The ATTACHMENTS node beside it is the OKM one and is converted like every other attachment. The FILES of an
 *    as-is node are still exported when found (asIsAttachmentFiles, user 2026-11-12); only the XML stays as is.
 *  - attachments: Kapture writes an uploaded file as children (fileName, fileExtension, attachmentTitle, filePath,
 *    fileType), sometimes inside &lt;array&gt;; OKM wrote the file as the element's text. Both forms become text,
 *    built by the caller from the file name. SIZE = bytes of the file in the document's source folder, blank when
 *    the file is not there; a SIZE already in the XML is passed as is. An empty attachment stays empty.
 *  - SECURITY = "" on the configured nodes that have a value (no field-level security in Kapture); one already in the
 *    XML is passed as is.
 *  - NO date in the channel node is converted (user 2026-11-12, confirmed 2026-11-25: TSB_ISSUE_DATE /
 *    FIRST_PUBLICATION_DATE / MOST_RECENT_REVISION_DATE come from Kapture as yyyy-MM-dd HH:mm:ss <zone>).
 *  - children put in the order of the legacy OKM EXPORT files (not the OKM authoring order, which differs per
 *    document): the first element of each listed name in list order, then repeats and unlisted names as they were.
 *  - nodes Kapture has and OKM had not are passed as is; nodes OKM had and Kapture has not are not added.
 *
 * HEADER (toOkmDocument - RMI Export and MNAO Data Export; the Bulk Extract builds its own header):
 *  &lt;CONTENT RECORDID=""&gt; then MASTERIDENTIFIER .. PUBLISHEDTIME rebuilt from the Kapture header, the channel
 *  node, the configured empty nodes after it (WD META), CATEGORIES. Every other Kapture header node is dropped.
 *  DATES IN THE OKM FORMAT (user 2026-11-25, replaces the 2026-11-12 "as is" rule): STARTTIMESTAMP = displayOn,
 *  CREATEDATE = createdDate, LASTMODIFIEDTIMESTAMP = dateModified, PUBLISHEDTIMESTAMP = publishedDate, converted by
 *  OkmExportDates (k_article text read by the document locale = the export locale) to the OKM TIMESTAMP, and the
 *  derived *_MILLIS, STARTDATE/TIME, LASTMODIFIEDDATE/TIME, PUBLISHEDDATE/TIME filled as OKM did. CREATEDATE has no
 *  local DATE / TIME node (as OKM). A value that cannot be read is written as is. A START / CREATE / LASTMODIFIED block is written only when Kapture has its date (legacy left a
 *  missing block out); the PUBLISHED block is always written, blank when Kapture has no publishedDate.
 *  Ids with no Kapture source (RECORDID, TYPE_GUID where not configured) are blank; a value in the Kapture XML
 *  would be passed as is. LOCALEVALUE = the locale code.
 *
 * SAME SOURCE in MDMKapture (com.mazda.gms3.mdm.utils) and NewMazdaDataExportTool (com.mazda.data.export.utils):
 * only the package line differs. Change both.
 */
public final class ExportOkmShape {

	private static final Pattern DIGITS = Pattern.compile("(\\d+)");

	private final Function<String, String> props;
	private final List<String> channels;
	private final List<String> attachmentNodes;
	private final List<String[]> securityPaths;
	private final List<String[]> asIsPaths;
	private final Map<String, String> renameIfAbsent;
	private final Map<String, List<String>> childOrder = new HashMap<String, List<String>>();

	/**
	 * @param props required-key lookup: returns the trimmed value or throws when the key is missing / blank
	 */
	public ExportOkmShape(Function<String, String> props) {
		this.props = props;
		this.channels = list("okm.export.channels");
		this.attachmentNodes = list("okm.export.attachment.nodes");
		this.securityPaths = paths("okm.export.security.nodes");
		this.asIsPaths = paths("okm.export.as.is.nodes");
		this.renameIfAbsent = renames("okm.export.rename.if.absent");
		for (String name : list("okm.export.child.order.nodes")) {
			childOrder.put(name, list("okm.export.child.order." + name));
		}
	}

	// ------------------------------------------------------------------ channel node

	/**
	 * @param channel the channel node (SERVICE_INFORMATION, SERVICE_MANUALS, ...); other channels are left as they are
	 * @param attachmentText file name -> the text the attachment element gets (exported path, or the name itself)
	 * @param sourceDir the folder the document's attachments are read from, for SIZE
	 * @param cdata true when the values are already in CDATA (MDM, after ExportXmlCData), false for plain text
	 * @return how many attachment elements now carry a file
	 */
	public int normalizeChannel(Element channel, Function<String, String> attachmentText, File sourceDir, boolean cdata) {
		String channelName = channel.getNodeName();
		if (!channels.contains(channelName)) {
			return 0;
		}
		renameChildren(channel, renameIfAbsent, true);
		asIsFilePaths(channel, attachmentText, cdata);

		int files = 0;
		for (Element element : converted(channel)) {
			if (null == element.getParentNode()) {
				continue; // removed with a Kapture attachment
			}
			files += attachment(element, attachmentText, sourceDir, cdata);
		}
		for (Element element : converted(channel)) {
			String[] chain = chain(channel, element);
			if (matches(securityPaths, chain) && !"".equals(element.getTextContent().trim()) && !element.hasAttribute("SECURITY")) {
				element.setAttribute("SECURITY", "");
			}
		}
		reorder(channel);
		for (Element element : converted(channel)) {
			reorder(element);
		}
		return files;
	}

	/**
	 * Only the rename FILE_SOURCE_LOCATION -> DJVU_FILE_LOCATION, for the step that reads the document BEFORE the full
	 * conversion. BIJLAGEN is not renamed (passed as is, user 2026-11-12).
	 * Running normalizeChannel later on the renamed node changes nothing more here.
	 */
	public static void renameForExport(Function<String, String> props, Element channel) {
		ExportOkmShape shape = new ExportOkmShape(props, true);
		if (shape.channels.contains(channel.getNodeName())) {
			shape.renameChildren(channel, shape.renameIfAbsent, true);
		}
	}

	private ExportOkmShape(Function<String, String> props, boolean renamesOnly) {
		this.props = props;
		this.channels = list("okm.export.channels");
		this.attachmentNodes = list("okm.export.attachment.nodes");
		this.securityPaths = null;
		this.asIsPaths = paths("okm.export.as.is.nodes");
		this.renameIfAbsent = renames("okm.export.rename.if.absent");
	}

	/**
	 * File names of the attachments inside the as-is nodes (BIJLAGEN) of the document's channel node, for the copy:
	 * the node is passed as is, its files are exported like every other attachment. Kapture form (fileName / filePath
	 * children, also inside &lt;array&gt;) or a file name as the element's text.
	 */
	public static List<String> asIsAttachmentFiles(Function<String, String> props, Document doc) {
		ExportOkmShape shape = new ExportOkmShape(props, true);
		List<String> names = new ArrayList<String>();
		for (String channelName : shape.channels) {
			org.w3c.dom.NodeList nodes = doc.getElementsByTagName(channelName);
			for (int i = 0; i < nodes.getLength(); i++) {
				Element channel = (Element) nodes.item(i);
				for (Element kid : children(channel)) {
					if (!matches(shape.asIsPaths, chain(channel, kid))) {
						continue;
					}
					for (Element element : descendants(kid)) {
						String name = "";
						if (isFileCarrier(element)) {
							name = kaptureFileName(element);
						} else if (!hasElementChild(element) && !"array".equals(element.getNodeName())
								&& shape.attachmentNodes.contains(element.getNodeName())) {
							name = lastSegment(element.getTextContent().trim());
						}
						if (!"".equals(name) && !names.contains(name)) {
							names.add(name);
						}
					}
				}
			}
		}
		return names;
	}

	private int attachment(Element element, Function<String, String> attachmentText, File sourceDir, boolean cdata) {
		List<Element> carriers = new ArrayList<Element>();
		if (isFileCarrier(element)) {
			carriers.add(element);
		} else {
			for (Element child : children(element)) {
				if ("array".equals(child.getNodeName()) && isFileCarrier(child)) {
					carriers.add(child);
				}
			}
		}
		List<String> names = new ArrayList<String>();
		if (!carriers.isEmpty()) {
			for (Element carrier : carriers) {
				names.add(kaptureFileName(carrier));
			}
			if (carriers.get(0) == element) {
				while (null != element.getFirstChild()) {
					element.removeChild(element.getFirstChild());
				}
			} else {
				for (Element carrier : carriers) {
					element.removeChild(carrier);
				}
				for (Node n = element.getFirstChild(); null != n;) {
					Node next = n.getNextSibling();
					if (n.getNodeType() == Node.TEXT_NODE && "".equals(n.getNodeValue().trim())) {
						element.removeChild(n);
					}
					n = next;
				}
			}
		} else if (attachmentNodes.contains(element.getNodeName()) && !hasElementChild(element)) {
			names.add(lastSegment(element.getTextContent().trim()));
		} else {
			return 0;
		}

		int files = 0;
		Element target = element;
		for (int i = 0; i < names.size(); i++) {
			if (i > 0) {
				Element copy = (Element) element.cloneNode(false);
				element.getParentNode().insertBefore(copy, target.getNextSibling());
				target = copy;
			}
			String name = names.get(i);
			if ("".equals(name)) {
				continue;
			}
			setText(target, attachmentText.apply(name), cdata);
			if (!target.hasAttribute("SIZE")) {
				File file = null == sourceDir ? null : new File(sourceDir, name);
				target.setAttribute("SIZE", null != file && file.isFile() ? String.valueOf(file.length()) : "");
			}
			files++;
		}
		return files;
	}

	private static boolean isFileCarrier(Element element) {
		for (Element child : children(element)) {
			if ("filePath".equals(child.getNodeName()) || "fileName".equals(child.getNodeName())) {
				return true;
			}
		}
		return false;
	}

	/** filePath's last part - the name the attachment copy uses (Utilities.getAttachmentFileName) - else fileName. */
	private static String kaptureFileName(Element carrier) {
		String fileName = "";
		for (Element child : children(carrier)) {
			if ("filePath".equals(child.getNodeName())) {
				String path = lastSegment(child.getTextContent().trim());
				if (!"".equals(path)) {
					return path;
				}
			} else if ("fileName".equals(child.getNodeName())) {
				fileName = child.getTextContent().trim();
			}
		}
		return fileName;
	}

	private static String lastSegment(String path) {
		String p = path.replace('\\', '/');
		return p.substring(p.lastIndexOf('/') + 1);
	}

	private void renameChildren(Element channel, Map<String, String> renames, boolean onlyIfTargetAbsent) {
		String prefix = channel.getNodeName() + "/";
		for (Element child : children(channel)) {
			String to = renames.get(prefix + child.getNodeName());
			if (null == to || (onlyIfTargetAbsent && !childrenNamed(channel, to).isEmpty())) {
				continue;
			}
			channel.getOwnerDocument().renameNode(child, null, to);
		}
	}

	private void reorder(Element element) {
		List<String> order = childOrder.get(element.getNodeName());
		if (null == order) {
			return;
		}
		List<Element> kids = children(element);
		List<Element> sorted = new ArrayList<Element>();
		// the FIRST element of each listed name in list order; repeats (a 2nd ATTACHMENTS group) and unlisted names
		// follow in their own order - legacy OKM exports put repeated groups at the end the same way
		for (String name : order) {
			for (Element kid : kids) {
				if (kid.getNodeName().equals(name)) {
					sorted.add(kid);
					break;
				}
			}
		}
		for (Element kid : kids) {
			if (!sorted.contains(kid)) {
				sorted.add(kid);
			}
		}
		if (sorted.equals(kids)) {
			return;
		}
		for (Node n = element.getFirstChild(); null != n;) {
			Node next = n.getNextSibling();
			if (n.getNodeType() == Node.TEXT_NODE && "".equals(n.getNodeValue().trim())) {
				element.removeChild(n);
			}
			n = next;
		}
		for (Element kid : sorted) {
			element.appendChild(kid); // appendChild moves the node
		}
	}

	// ------------------------------------------------------------------ whole document (MDM)

	/**
	 * The call the exports make, LAST before the file is written (after ExportXmlCData.withCData).
	 *
	 * @return the XML in the OKM shape, or null - a missing key or an unreadable XML fails the document, not the schedule
	 */
	public static String forExport(Function<String, String> props, String xml, String channelName, String exportLocale,
			String attachmentPrefix, File sourceDir) {
		if (null == xml) {
			return null;
		}
		try {
			return new ExportOkmShape(props).toOkmDocument(xml, channelName, exportLocale, attachmentPrefix, sourceDir);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(ExportOkmShape.class.getName(), "forExport()", e);
			return null;
		}
	}

	/**
	 * RMI Export / MNAO Data Export: the finished document XML (after ExportXmlCData) in the OKM shape.
	 *
	 * @param xml the document XML as the export built it
	 * @param channelName the channel node's name
	 * @param exportLocale the locale the export runs for (en_US, en_UK): LOCALECODE / LOCALEVALUE. Not the XML's own
	 *        locale fields: an en_US document can carry only primaryLocale en_CA
	 * @param attachmentPrefix exported folder of the document, e.g. en_US/CX90/SERVICE_INFORMATION/TSB/SI1/
	 * @param sourceDir the document's Kapture folder, for SIZE
	 * @return the XML, or null when it could not be converted (the document then fails with the write failure)
	 */
	public String toOkmDocument(String xml, String channelName, String exportLocale, final String attachmentPrefix, File sourceDir) {
		InputSource source = new InputSource();
		source.setCharacterStream(new StringReader(xml));
		try {
			Document in = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
			Element oldRoot = in.getDocumentElement();
			Map<String, Element> header = new LinkedHashMap<String, Element>();
			for (Element child : children(oldRoot)) {
				if (!header.containsKey(child.getNodeName())) {
					header.put(child.getNodeName(), child);
				}
			}
			Element channel = header.get(channelName);
			if (null != channel) {
				normalizeChannel(channel, new Function<String, String>() {
					public String apply(String name) {
						return attachmentPrefix + name;
					}
				}, sourceDir, true);
			}

			Document out = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = out.createElement("CONTENT");
			out.appendChild(root);
			String recordId = oldRoot.hasAttribute("RECORDID") ? oldRoot.getAttribute("RECORDID") : text(header.get("RECORDID"));
			root.setAttribute("RECORDID", null == recordId ? "" : recordId);

			List<String> cdataNodes = list("okm.export.header.cdata.nodes");
			Map<String, String> typeGuids = pairs("okm.export.header.type.guids");
			Map<String, String> metaPaths = pairs("okm.export.header.metaresourcepath");
			Map<String, String> afterChannel = pairs("okm.export.header.after.channel");

			String documentId = text(header.get("DOCUMENTID"));
			String locale = exportLocale;
			add(out, root, "MASTERIDENTIFIER", text(header.get("title")), cdataNodes);
			add(out, root, "TYPE", text(header.get("type")), cdataNodes);
			add(out, root, "TYPE_GUID", typeGuids.get(channelName), cdataNodes);
			add(out, root, "REPOSITORY", props.apply("okm.export.header.repository"), cdataNodes);
			add(out, root, "REPOSITORY_GUID", props.apply("okm.export.header.repository.guid"), cdataNodes);
			add(out, root, "DOCUMENTID", documentId, cdataNodes);
			add(out, root, "LOCALECODE", locale, cdataNodes);
			add(out, root, "LOCALEVALUE", locale, cdataNodes);
			add(out, root, "VERSION", firstNonBlank(text(header.get("publishedVersion")), text(header.get("articleVersion"))), cdataNodes);

			// dates in the OKM format, k_article text read by the document locale (user 2026-11-25) - see class comment
			OkmExportDates dates = new OkmExportDates(props, true);
			String start = text(header.get("displayOn"));
			String created = text(header.get("createdDate"));
			String modified = text(header.get("dateModified"));
			if (null != start) {
				addDateBlock(out, root, cdataNodes, dates.block(start, locale), "STARTTIMESTAMP", "STARTTIMESTAMP_MILLIS", "STARTDATE", "STARTTIME");
			}
			if (null != created) {
				addDateBlock(out, root, cdataNodes, dates.block(created, locale), "CREATEDATE", "CREATEDATE_MILLIS");
			}
			if (null != modified) {
				addDateBlock(out, root, cdataNodes, dates.block(modified, locale), "LASTMODIFIEDTIMESTAMP", "LASTMODIFIEDTIMESTAMP_MILLIS", "LASTMODIFIEDDATE", "LASTMODIFIEDTIME");
			}
			String metaPrefix = metaPaths.get(channelName);
			if (null != metaPrefix && null != documentId) {
				Matcher number = DIGITS.matcher(documentId);
				if (number.find()) {
					long folder = Long.parseLong(number.group(1)) / 1000L * 1000L;
					add(out, root, "METARESOURCEPATH", metaPrefix + folder + "/" + documentId + "/", cdataNodes);
				}
			}
			addDateBlock(out, root, cdataNodes, dates.block(text(header.get("publishedDate")), locale), "PUBLISHEDTIMESTAMP", "PUBLISHEDTIMESTAMP_MILLIS", "PUBLISHEDDATE", "PUBLISHEDTIME");
			if (null != channel) {
				root.appendChild(out.importNode(channel, true));
			}
			String after = afterChannel.get(channelName);
			if (null != after) {
				for (String name : after.split("\\s*\\+\\s*")) {
					root.appendChild(out.createElement(name));
				}
			}
			if (null != header.get("CATEGORIES")) {
				root.appendChild(out.importNode(header.get("CATEGORIES"), true));
			}

			StringWriter writer = new StringWriter();
			Transformer serializer = TransformerFactory.newInstance().newTransformer();
			serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			serializer.transform(new DOMSource(out), new StreamResult(writer));
			return props.apply("okm.export.xml.declaration") + writer.toString();
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(ExportOkmShape.class.getName(), "toOkmDocument()", e);
			return null;
		}
	}

	/** names[i] gets values[i] (TIMESTAMP, _MILLIS, local DATE, local TIME - OkmExportDates.block); blank values stay empty. */
	private static void addDateBlock(Document out, Element root, List<String> cdataNodes, String[] values, String... names) {
		for (int i = 0; i < names.length; i++) {
			add(out, root, names[i], i < values.length ? values[i] : null, cdataNodes);
		}
	}

	private static void add(Document out, Element root, String name, String value, List<String> cdataNodes) {
		Element element = out.createElement(name);
		if (null != value && !"".equals(value.trim())) {
			element.appendChild(cdataNodes.contains(name) ? out.createCDATASection(value) : out.createTextNode(value));
		}
		root.appendChild(element);
	}

	// ------------------------------------------------------------------ helpers

	private static void setText(Element element, String value, boolean cdata) {
		while (null != element.getFirstChild()) {
			element.removeChild(element.getFirstChild());
		}
		Document doc = element.getOwnerDocument();
		element.appendChild(cdata ? doc.createCDATASection(value) : doc.createTextNode(value));
	}

	private static String text(Element element) {
		return null == element ? null : element.getTextContent().trim();
	}

	private static String firstNonBlank(String a, String b) {
		return null != a && !"".equals(a) ? a : b;
	}

	private static boolean hasElementChild(Element element) {
		return !children(element).isEmpty();
	}

	private static List<Element> children(Element element) {
		List<Element> kids = new ArrayList<Element>();
		for (Node n = element.getFirstChild(); null != n; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				kids.add((Element) n);
			}
		}
		return kids;
	}

	private static List<Element> childrenNamed(Element element, String name) {
		List<Element> kids = new ArrayList<Element>();
		for (Element kid : children(element)) {
			if (kid.getNodeName().equals(name)) {
				kids.add(kid);
			}
		}
		return kids;
	}

	private static List<Element> descendants(Element element) {
		List<Element> all = new ArrayList<Element>();
		for (Element kid : children(element)) {
			all.add(kid);
			all.addAll(descendants(kid));
		}
		return all;
	}

	/**
	 * Inside the as-is nodes (BIJLAGEN) ONLY filePath changes: it gets the same value as every converted attachment
	 * (the exported path in MDM, the file name in the Bulk Extract), because the file is exported there (user
	 * 2026-11-12). In MDM the path step already wrote that value, so this changes nothing there.
	 */
	private void asIsFilePaths(Element channel, Function<String, String> attachmentText, boolean cdata) {
		for (Element kid : children(channel)) {
			if (!matches(asIsPaths, chain(channel, kid))) {
				continue;
			}
			for (Element element : descendants(kid)) {
				if ("filePath".equals(element.getNodeName()) && !hasElementChild(element)) {
					String name = lastSegment(element.getTextContent().trim());
					if (!"".equals(name)) {
						setText(element, attachmentText.apply(name), cdata);
					}
				}
			}
		}
	}

	/** The channel's descendants WITHOUT the as-is nodes and everything inside them. */
	private List<Element> converted(Element channel) {
		List<Element> all = new ArrayList<Element>();
		collect(channel, channel, all);
		return all;
	}

	private void collect(Element channel, Element element, List<Element> all) {
		for (Element kid : children(element)) {
			if (matches(asIsPaths, chain(channel, kid))) {
				continue;
			}
			all.add(kid);
			collect(channel, kid, all);
		}
	}

	/** Names from the channel node down to the element, the channel first. */
	private static String[] chain(Element channel, Element element) {
		List<String> names = new ArrayList<String>();
		for (Node n = element; null != n && n != channel.getParentNode(); n = n.getParentNode()) {
			names.add(0, n.getNodeName());
		}
		return names.toArray(new String[0]);
	}

	/** CHANNEL/NAME matches NAME anywhere under that channel; CHANNEL/PARENT/NAME needs that parent as well. */
	private static boolean matches(List<String[]> paths, String[] chain) {
		for (String[] path : paths) {
			if (path.length < 2 || !path[0].equals(chain[0]) || chain.length < path.length) {
				continue;
			}
			boolean same = true;
			for (int i = 1; i < path.length && same; i++) {
				same = path[path.length - i].equals(chain[chain.length - i]);
			}
			if (same) {
				return true;
			}
		}
		return false;
	}

	private List<String> list(String key) {
		return Arrays.asList(props.apply(key).trim().split("\\s*,\\s*"));
	}

	private List<String[]> paths(String key) {
		List<String[]> paths = new ArrayList<String[]>();
		for (String path : list(key)) {
			paths.add(path.split("/"));
		}
		return paths;
	}

	/** CHANNEL/FROM&gt;TO,... */
	private Map<String, String> renames(String key) {
		Map<String, String> map = new HashMap<String, String>();
		for (String entry : list(key)) {
			String[] fromTo = entry.split(">");
			map.put(fromTo[0].trim(), fromTo[1].trim());
		}
		return map;
	}

	/** CHANNEL:VALUE,... - a channel not listed has no value (blank). */
	private Map<String, String> pairs(String key) {
		Map<String, String> map = new HashMap<String, String>();
		for (String entry : list(key)) {
			int colon = entry.indexOf(':');
			map.put(entry.substring(0, colon).trim(), entry.substring(colon + 1).trim());
		}
		return map;
	}
}
