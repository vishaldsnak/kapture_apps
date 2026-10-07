package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mazda.gms3.dmt.conversion.utils.SpecialCharactersUtils;
import com.mazda.gms3.dmt.kapture.KaptureArticle;
import com.mazda.gms3.dmt.vo.ContentDetails;

/**
 * BUILDS THE KAPTURE DOCUMENT FOR ONE CONVERTED FILE (MC market).
 *
 * The attribute names and the rule for filling each one are the same as the content XML the
 * conversion built for InfoManager (InfoQueriaUtils.create...ContentXML) - only the carrier
 * changed, from an XML string to the attribute map Kapture takes.
 */
public class KaptureArticleBuilder {

	public static final String CHANNEL_SERVICE_MANUALS = "SERVICE_MANUALS";
	public static final String CHANNEL_WIRING_DIAGRAMS = "WIRING_DIAGRAMS";
	public static final String CHANNEL_OTHER_SERVICE_MANUALS = "OTHER_SERVICE_MANUALS";

	/** Channel type of a document: its channel name upper-cased, spaces as underscores. */
	public static String channelType(ContentDetails cd) {
		return cd.getChannelName().toUpperCase().replace(" ", "_");
	}

	/**
	 * @param locale Kapture locale, e.g. ja_JP
	 */
	public static KaptureArticle build(ContentDetails cd, String locale) {
		KaptureArticle a = new KaptureArticle();
		a.channelType = channelType(cd);
		a.locale = locale;
		a.title = cleanTitle(cd.getTitle());
		a.sourceIdentifier = nz(cd.getFilePath());
		a.sourceFileName = nz(cd.getFileName());

		if (null != cd.getOemFileLocation() && !"".equals(cd.getOemFileLocation())) {
			a.fields = oemFields(cd, a.title);
		} else if (CHANNEL_WIRING_DIAGRAMS.equals(a.channelType)) {
			a.fields = wiringDiagramFields(cd, a.title);
		} else if (CHANNEL_OTHER_SERVICE_MANUALS.equals(a.channelType)) {
			a.fields = otherServiceManualFields(cd, a.title);
		} else {
			a.fields = serviceManualFields(cd, a.title);
		}
		return a;
	}

	/**
	 * OEM content (SM / OSM, html and html5): the document is NOT loaded into Kapture. Its
	 * source folder is copied to OKAssets as it is and the document only says where its file is.
	 */
	private static Map<String, Object> oemFields(ContentDetails cd, String title) {
		Map<String, Object> f = new LinkedHashMap<String, Object>();
		f.put("TITLE", title);
		f.put("OEM_FILE_LOCATION", cd.getOemFileLocation());
		return f;
	}

	private static Map<String, Object> serviceManualFields(ContentDetails cd, String title) {
		Map<String, Object> f = new LinkedHashMap<String, Object>();
		f.put("TITLE", title);
		f.put("CONTENT", nz(cd.getDocumentContent()));
		f.put("VTOC_FILENAME", nz(cd.getFileNameAttribute()));
		f.put("SIE_ID", nz(cd.getFileName()));
		f.put("DJVU_FILE_LOCATION", nz(cd.getUploadDirectoryPath()));
		f.put("OASIS_FILE_LOCATION", nz(cd.getOasisDirectoryPath()));
		addPdfAttachments(f, cd);
		return f;
	}

	private static Map<String, Object> otherServiceManualFields(ContentDetails cd, String title) {
		Map<String, Object> f = new LinkedHashMap<String, Object>();
		f.put("TITLE", title);
		f.put("CONTENT", nz(cd.getDocumentContent()));
		f.put("DJVU_FILE_LOCATION", nz(cd.getUploadDirectoryPath()));
		f.put("OASIS_FILE_LOCATION", nz(cd.getOasisDirectoryPath()));
		addPdfAttachments(f, cd);
		return f;
	}

	/** SM / OSM: a PDF document carries its file under ATTACHMENTS, titled with the RAW title. */
	private static void addPdfAttachments(Map<String, Object> f, ContentDetails cd) {
		if (ContentDetails.PDF_DOCUMENT.equals(cd.getDocumentType())) {
			Map<String, Object> att = new LinkedHashMap<String, Object>();
			att.put("ATTACHMENT_TITLE", nz(cd.getTitle()));
			att.put("ATTACHMENT", pdfAttachment(cd, nz(cd.getTitle())));
			f.put("ATTACHMENTS", att);
		}
	}

	private static Map<String, Object> wiringDiagramFields(ContentDetails cd, String title) {
		String type = cd.getDocumentType();
		boolean html5 = ContentDetails.HTML5_DOCUMENT.equals(type);
		Map<String, Object> f = new LinkedHashMap<String, Object>();
		f.put("TITLE", title);
		f.put("HTML_CONTENT", ContentDetails.HTML_DOCUMENT.equals(type) || ContentDetails.ENT_DOCUMENT.equals(type)
				? nz(cd.getDocumentContent()) : "");
		f.put("FLASH_CONTENT", ContentDetails.FLASH_DOCUMENT.equals(type) ? nz(cd.getDocumentContent()) : "");
		f.put("NAVIGATION", html5 ? nz(cd.getNavigation()) : "");
		f.put("HTML5_FILE_NAME", html5 ? nz(cd.getFileName()) : "");
		f.put("HTML5_UPLOAD_DIRECTORY", html5 ? nz(cd.getUploadDirectoryPath()) : "");
		f.put("HTML5_FONT_SELECTION", fontSelection(cd));
		f.put("DJVU_FILE_LOCATION", wdFileLocation(cd));
		f.put("ATTACHMENT", wdAttachment(cd, title));
		return f;
	}

	private static String fontSelection(ContentDetails cd) {
		String type = cd.getDocumentType();
		if (ContentDetails.HTML5_DOCUMENT.equals(type) || ContentDetails.FLASH_DOCUMENT.equals(type)
				|| ContentDetails.HTML_MC_DOCUMENT.equals(type)) {
			return nz(cd.getFontContent());
		}
		return "";
	}

	/** HTML5: the HTML5 file source path when there is one; every other type: the upload path. */
	private static String wdFileLocation(ContentDetails cd) {
		if (ContentDetails.HTML5_DOCUMENT.equals(cd.getDocumentType())) {
			return nz(cd.getHtml5FileSourcePath());
		}
		return nz(cd.getUploadDirectoryPath());
	}

	/** WD carries its PDF directly in ATTACHMENT (no ATTACHMENTS group, no attachment title). */
	private static Object wdAttachment(ContentDetails cd, String title) {
		if (null != cd.getPdfFileNameAsAttachment() && !"".equals(cd.getPdfFileNameAsAttachment())) {
			return pdfAttachment(cd, title);
		}
		return "";
	}

	private static KaptureArticle.Attachment pdfAttachment(ContentDetails cd, String title) {
		String path = cd.getPdfFilePathAsAttachment();
		return new KaptureArticle.Attachment(null == path || "".equals(path) ? null : PathUtil.file(path), title);
	}

	/**
	 * A WIRING DIAGRAM UPDATE THAT CHANGES ONLY PART OF THE DOCUMENT.
	 *
	 * One WD document can be fed by two source files of the same material folder - a PDF and an
	 * HTML. Each of them updates only its own attributes and must leave the other file's
	 * attributes as they are in the document:
	 *   PDF  file -> TITLE and ATTACHMENT
	 *   HTML file -> TITLE, DJVU_FILE_LOCATION and HTML5_FONT_SELECTION
	 *
	 * @param existing the attributes the document holds in Kapture now
	 * @return false when the document type is not one of the two partial ones - the caller
	 *         then sends the complete document built by build()
	 */
	public static boolean overlayPartialWiringDiagramUpdate(KaptureArticle a, ContentDetails cd, JsonObject existing) {
		String type = cd.getDocumentType();
		boolean pdf = ContentDetails.PDF_DOCUMENT.equals(type);
		boolean htmlMc = ContentDetails.HTML_MC_DOCUMENT.equals(type);
		if (!pdf && !htmlMc) {
			return false;
		}
		Map<String, Object> merged = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, JsonElement> e : existing.entrySet()) {
			merged.put(e.getKey(), e.getValue());
		}
		merged.put("TITLE", a.title);
		if (pdf) {
			merged.put("ATTACHMENT", wdAttachment(cd, a.title));
		} else {
			merged.put("DJVU_FILE_LOCATION", wdFileLocation(cd));
			merged.put("HTML5_FONT_SELECTION", fontSelection(cd));
		}
		a.fields = merged;
		return true;
	}

	/** true for a WD document type that updates only part of the document. */
	public static boolean isPartialWiringDiagramUpdate(ContentDetails cd) {
		return CHANNEL_WIRING_DIAGRAMS.equals(channelType(cd))
				&& (ContentDetails.PDF_DOCUMENT.equals(cd.getDocumentType())
						|| ContentDetails.HTML_MC_DOCUMENT.equals(cd.getDocumentType()));
	}

	/** Special characters replaced and every SUP tag removed, as for the InfoManager title. */
	public static String cleanTitle(String rawTitle) {
		if (null == rawTitle || "".equals(rawTitle)) {
			return "";
		}
		String title = SpecialCharactersUtils.replaceSpecialChars(rawTitle);
		return title.replaceAll("(?i)</?sup>", "");
	}

	private static String nz(String s) {
		return null == s ? "" : s;
	}
}
