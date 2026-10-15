package com.mazda.gms3.cdrom.dao;

import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.mdm.dao.KapturePublishedContentDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.KaptureContentXmlReader;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * THE CD ROM DOCUMENT CONTENT SOURCE - REPLACES OK_IM.CONTENTTEXT / OK_IM.CONTENTDATA.
 *
 * WHY THIS EXISTS. CD Creation used to read one column: OK_IM.CONTENTDATA.XML, the whole document
 * as InfoManager stored it, found by joining CONTENTTEXT on documentid + localeid + published='Y'.
 * Neither table exists under MySQL. Kapture keeps the same information in the XML it writes per
 * document version, and the channel node inside that file is what the column used to hold.
 *
 * FINDING THE FILE IS NOT THIS CLASS'S JOB - KapturePublishedContentDAO does that, because RMI
 * Export needs the identical answer and the two must not drift. What is left here is the bit that
 * is genuinely CD-specific: pulling the CHANNEL NODE out and handing it back as a String.
 *
 * SO THE VALUE RETURNED IS THE CHANNEL NODE, NOT THE WHOLE FILE - e.g. for a service manual the
 * &lt;SERVICE_MANUALS&gt;&lt;TITLE/&gt;&lt;CONTENT/&gt;&lt;/SERVICE_MANUALS&gt; subtree, serialized back to a String.
 * That is exactly the shape CDRomStartConversionImpl.getParseddocument() already parses (it XPaths
 * /ROOT/TITLE and /ROOT/CONTENT off whatever the root element is), which is why the 1,700-line
 * conversion engine downstream did not have to change at all.
 *
 * (RMI Export wants the OPPOSITE - the whole document, filtered - which is why the split between
 * the two classes falls exactly here.)
 *
 * NO API CALLS. The CD walks thousands of documents plus an unbounded recursion through inner
 * links; the Kapture write APIs are rate limited and reading content through them would take days.
 * The XML on disk is the same content the API would return, so this reads the file directly.
 */
public class CDRomKaptureContentDAO {

	static Logger logger = LogManager.getLogger(CDRomKaptureContentDAO.class);

	/**
	 * Channel node name per document id prefix, used only when the XML does not name its own type.
	 * Overridable as cdrom.channel.node.&lt;PREFIX&gt; so VDI can add a channel without a rebuild.
	 */
	private static final String[][] DEFAULT_CHANNEL_NODES = {
			{ "SM", "SERVICE_MANUALS" },
			{ "WD", "WIRING_DIAGRAMS" },
			{ "OSM", "OTHER_SERVICE_MANUALS" } };

	/**
	 * THE REPLACEMENT FOR THE CONTENTTEXT/CONTENTDATA JOIN, FOR ONE DOCUMENT.
	 *
	 * @param documentId article id, e.g. "SM1027"
	 * @param locale     the locale being processed; when null or blank the highest published
	 *                   version in ANY locale is taken - which is what the inner-link and JS
	 *                   rewriting paths did before, since their SQL never filtered on locale either
	 * @return a LabelBean carrying recordId (k_article.id), the channel node XML and the article
	 *         id, or null when the document has no published version or no readable XML
	 */
	public static LabelBean getPublishedChannelNode(String documentId, String locale) {
		if (null == documentId || "".equals(documentId.trim())) {
			logger.info("getPublishedChannelNode :: document id as parameter is null.");
			return null;
		}
		String docId = documentId.trim();
		try {
			KapturePublishedContentDAO.PublishedVersion version =
					KapturePublishedContentDAO.findLatestPublished(docId, locale);
			if (null == version) {
				logger.info("getPublishedChannelNode :: no PUBLISHED version in k_article for {"
						+ docId + "} / {" + locale + "} - document skipped.");
				return null;
			}

			String folder = KapturePublishedContentDAO.resolveContentFolder(docId,
					version.getLocale(), version.getVersion());
			if (null == folder) {
				logger.info("getPublishedChannelNode :: no content XML on disk for {" + docId
						+ "} / {" + version.getLocale() + "} / version {" + version.getVersion()
						+ "} - document skipped.");
				return null;
			}

			KaptureContentXmlReader reader = KaptureContentXmlReader.openAt(folder);
			if (!reader.isAvailable()) {
				logger.info("getPublishedChannelNode :: content XML under {" + folder
						+ "} could not be parsed for {" + docId + "} - document skipped.");
				return null;
			}

			String nodeName = resolveChannelNodeName(reader, docId);
			String nodeXml = null;
			if (null != nodeName) {
				nodeXml = reader.getNodeXml(nodeName);
			}
			if (null == nodeXml || "".equals(nodeXml.trim())) {
				logger.info("getPublishedChannelNode :: channel node {" + nodeName
						+ "} is absent or empty in {" + reader.getResolvedPath() + "} for {" + docId
						+ "} - document skipped.");
				return null;
			}

			logger.info("getPublishedChannelNode :: {" + docId + "} / {" + version.getLocale()
					+ "} version {" + version.getVersion() + "} node {" + nodeName + "} read from {"
					+ reader.getResolvedPath() + "}");

			LabelBean details = new LabelBean();
			details.setKey(version.getRecordId());
			details.setValue(nodeXml);
			details.setDocumentId(docId);
			return details;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomKaptureContentDAO.class.getName(),
					"getPublishedChannelNode()", e);
			return null;
		}
	}

	/**
	 * WHICH NODE HOLDS THE DOCUMENT - SM -&gt; SERVICE_MANUALS, WD -&gt; WIRING_DIAGRAMS,
	 * OSM -&gt; OTHER_SERVICE_MANUALS.
	 *
	 * THE XML NAMES ITS OWN CHANNEL in &lt;type&gt;, so that is asked first and a hardcoded map is only
	 * the fallback. A document that gains a channel, or one whose node is renamed, then needs no
	 * code change at all - and the fallback still covers a file written without a type element.
	 */
	private static String resolveChannelNodeName(KaptureContentXmlReader reader, String documentId) {
		String declared = reader.getTextOf("type");
		if (null != declared && !"".equals(declared.trim()) && reader.hasNode(declared.trim())) {
			return declared.trim();
		}

		String prefix = KaptureContentXmlReader.getChannel(documentId);
		if (null == prefix) {
			logger.info("resolveChannelNodeName :: cannot derive a channel from {" + documentId + "}");
			return null;
		}
		String configured = ApplicationProperties.getProperty("cdrom.channel.node."
				+ prefix.toUpperCase());
		if (null != configured && !"".equals(configured.trim())) {
			return configured.trim();
		}
		for (int i = 0; i < DEFAULT_CHANNEL_NODES.length; i++) {
			if (DEFAULT_CHANNEL_NODES[i][0].equalsIgnoreCase(prefix)) {
				return DEFAULT_CHANNEL_NODES[i][1];
			}
		}
		logger.info("resolveChannelNodeName :: no channel node known for prefix {" + prefix
				+ "} - set cdrom.channel.node." + prefix.toUpperCase() + " to name it.");
		return null;
	}
}
