package com.mazda.gms3.dmt.preview;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureHtmlSanitizer;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.utils.KaptureArticleBuilder;
import com.mazda.gms3.dmt.mc.vo.MCViewContentDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.OkAssetsWeb;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;

/**
 * THE OFFLINE PREVIEW OF ONE MC / MME JOB - written WHILE the job runs, from what the job already holds
 * in memory, so that the Preview page needs neither Kapture nor the database.
 *
 * Folder <preview.physical.path><schedule id>/ :
 *   <document id>.html       the document content exactly as sent to Kapture (same sanitizer), made viewable offline
 *                            (written at every successful write - the inner links pass rewrites it)
 *   files/<document id>/...  copies of the document's PDF files
 *   wd/<document id>/<page>  wiring diagram HTML5 page copy: same file name, loads the rest of the
 *                            viewer (voltage maps too) from OKAssets, which is never changed
 *   _assets/preview_page.js  hands the document links of a page to the Preview page
 *   preview.json             written at the END of the job: every document with its display order
 *                            positions, manual type and VINs (the tree and the carline / VIN filters;
 *                            MNAO: "mnao": true and model / year on each VIN - the model / year / VIN filters)
 *
 * Carline names are the ones the job read with the VINs (MDM); a VIN MDM has no name for stays blank.
 *
 * Nothing here may stop or fail the job: every error is logged and the job goes on.
 */
public class PreviewBuilder {

	private static Logger logger = LogManager.getLogger(PreviewBuilder.class);

	public static final String PREVIEW_FILE = "preview.json";
	private static final String FILES_FOLDER = "files";
	private static final String WD_FOLDER = "wd";
	private static final Charset UTF8 = Charset.forName("UTF-8");
	/** Jobs rewrite each other's preview.json when a later job unpublishes or publishes a document. */
	static final Object FILE_LOCK = new Object();

	private final String scheduleId;
	private final File folder;
	private final String webContext;
	private final PreviewPageWriter pageWriter;
	private final String wdOpenPrefix;
	/** MNAO job: the VINs carry model and year (vin.txt / master VIN), the preview filters on them, not on the carline */
	private final boolean mnao;
	private final KaptureHtmlSanitizer sanitizer;
	private String locale = null;

	private final Map<String, PreviewDocument> documents = new LinkedHashMap<String, PreviewDocument>();
	private final Set<String> unpublished = new LinkedHashSet<String>();
	/** Every VIN of the job: key -> carline code, carline name, WMI, VDS, VIS start, VIS end, model, year (model / year: MNAO) */
	private final Map<String, String[]> vins = new LinkedHashMap<String, String[]>();
	private int pagesWritten = 0;
	private int filesCopied = 0;
	private int failures = 0;

	/** The preview of an MC job. */
	public PreviewBuilder(String scheduleId) {
		this(scheduleId, false);
	}

	/**
	 * @param mme true for an MME job, false for an MC job (see PreviewBuilder(String, String))
	 */
	public PreviewBuilder(String scheduleId, boolean mme) {
		this(scheduleId, mme ? KaptureApiClient.require("market.mme") : KaptureApiClient.require("market.mc"));
	}

	/**
	 * @param market the market of the job (market.mc / market.mme / market.mnao): the stylesheets kept by the
	 *               HTML clean-up (the same rule as the documents sent to Kapture) and the voltage map link property
	 */
	public PreviewBuilder(String scheduleId, String market) {
		boolean mme = market.trim().equalsIgnoreCase(KaptureApiClient.require("market.mme").trim());
		this.mnao = market.trim().equalsIgnoreCase(KaptureApiClient.require("market.mnao").trim());
		this.scheduleId = scheduleId.trim();
		this.sanitizer = new KaptureHtmlSanitizer(mnao ? ConversionUtils::isConversionStyleLinkMNAO
				: mme ? ConversionUtils::isConversionStyleLinkMME : ConversionUtils::isConversionStyleLink);
		this.folder = jobFolder(this.scheduleId);
		this.webContext = OkAssetsWeb.context();
		String library = "/" + KaptureApiClient.require("SERVER_LIBRARY_DIRECTORY").replace("\\", "/").replaceAll("^/+", "");
		if (!library.endsWith("/")) {
			library = library + "/";
		}
		this.pageWriter = new PreviewPageWriter(webContext, library, KaptureApiClient.require("LINK_URL"));
		// the voltage map links as the job writes them (WiringDiagramUtils): <app content><link url><id>
		this.wdOpenPrefix = KaptureApiClient.require("GMS3_INFOCENTER_APPLICATION_CONTENT").trim()
				+ KaptureApiClient.require(mnao ? "LINK_URL" : mme ? "LINK_URL_MME_WINDOW_JS" : "LINK_URL_MC_WINDOW_JS").trim();
	}

	/** The preview folder of a job. */
	public static File jobFolder(String scheduleId) {
		return PathUtil.file(KaptureApiClient.require("preview.physical.path").replace("\\", "/"), scheduleId.trim());
	}

	/** Web address of a job's preview folder (with the OKAssets context, ending with /). */
	public static String jobWebFolder(String scheduleId) {
		String p = KaptureApiClient.require("preview.web.path").replace("\\", "/");
		return OkAssetsWeb.url((p.endsWith("/") ? p : p + "/") + scheduleId.trim() + "/");
	}

	/** True when the job has a preview with at least one document (a file check, no database). */
	public static boolean hasPreview(String scheduleId) {
		try {
			File f = PathUtil.file(jobFolder(scheduleId), PREVIEW_FILE);
			return f.isFile() && f.length() > 0;
		} catch (Exception e) {
			return false;
		}
	}

	// ---- while the job runs ------------------------------------------------------------------

	/** A document was written to Kapture (created / updated / inner links updated). */
	public void documentWritten(ContentDetails cd, String kaptureLocale) {
		try {
			String id = cd.getImDocumentId();
			if (null == id || "".equals(id.trim())) {
				return;
			}
			id = id.trim();
			if (null == locale && null != kaptureLocale) {
				locale = kaptureLocale;
			}
			unpublished.remove(id);
			PreviewDocument d = documents.get(id);
			if (null == d) {
				d = new PreviewDocument();
				d.id = id;
				documents.put(id, d);
			}
			d.version = cd.getImVersion();
			d.rowId = cd.getImContentType();
			d.title = KaptureArticleBuilder.cleanTitle(cd.getTitle());
			d.channel = KaptureArticleBuilder.channelType(cd);
			d.type = cd.getDocumentType();
			d.modelType = cd.getModelType();
			d.model = cd.getModel();
			d.modelFolder = cd.getModelFolderName();
			d.manualTypeFolder = cd.getManualType();
			d.sourcePath = cd.getFilePath();
			if (hasText(cd.getDocumentTypeName())) {
				d.manualType = cd.getDocumentTypeName();
			}
			addVins(d, cd);

			String type = cd.getDocumentType();
			if (hasText(cd.getOemFileLocation())) {
				d.fileUrl = OkAssetsWeb.url(cd.getOemFileLocation());
			} else if (ContentDetails.HTML5_DOCUMENT.equals(type) && hasText(cd.getHtml5FileSourcePath())) {
				d.fileUrl = OkAssetsWeb.url(cd.getHtml5FileSourcePath());
				writeWdPage(d, cd);
			} else if ((ContentDetails.HTML_MC_DOCUMENT.equals(type) || ContentDetails.DJVU_DOCUMENT.equals(type))
					&& hasText(cd.getUploadDirectoryPath())) {
				d.fileUrl = OkAssetsWeb.url(cd.getUploadDirectoryPath());
			} else if (!ContentDetails.PDF_DOCUMENT.equals(type) && hasText(cd.getDocumentContent())) {
				// exactly the content sent to Kapture: the same sanitizer (keeps the content.css / ENT MAZDA css links)
				writeText(PathUtil.file(folder, id + ".html"), pageWriter.page(id, d.title, sanitizer.sanitize(cd.getDocumentContent())));
				d.pageUrl = id + ".html";
				pagesWritten++;
			}
			if (hasText(cd.getPdfFilePathAsAttachment())) {
				copyPdf(d, PathUtil.file(cd.getPdfFilePathAsAttachment()));
			}
		} catch (Throwable e) {
			failures++;
			logger.info("preview :: could not add document " + cd.getImDocumentId() + " to the preview :: " + e);
		}
	}

	/**
	 * The job reports the document as FAILED although Kapture took it (its database save failed, or a
	 * new document's attachments could not be added): it leaves this job's preview and is not saved
	 * for publishing. Previews of other jobs are not touched.
	 */
	public void documentFailed(ContentDetails cd) {
		String id = cd.getImDocumentId();
		if (null != id && null != documents.remove(id.trim())) {
			logger.info("preview :: document " + id.trim() + " left out of the preview - the job reports it as failed");
		}
	}

	/** A document was unpublished (delete processing): it leaves this and every earlier preview. */
	public void documentUnpublished(ContentDetails cd) {
		String id = cd.getImDocumentId();
		if (null != id && !"".equals(id.trim())) {
			documents.remove(id.trim());
			unpublished.add(id.trim());
		}
	}

	private void addVins(PreviewDocument d, ContentDetails cd) {
		if (null != cd.getApplicableVINList()) {
			for (VinDetails v : cd.getApplicableVINList()) {
				addVin(d, v.getCarlineCode(), v.getCarlineNameReg(), v.getWmiCode(), v.getVdsCode(), v.getVisStartRange(), v.getVisEndRange(),
						v.getModelName(), v.getModelYear());
			}
		}
		// ENGINE / MISSION / AT / MT books: no vin.txt - the master VINs of the book
		if (null != cd.getMasterVinList()) {
			for (VINEntFileDetails v : cd.getMasterVinList()) {
				addVin(d, v.getVinCarline(), v.getCarlineRegName(), v.getVinWMI(), v.getVinVDS(), v.getVinStartRange(), v.getVinEndRange(),
						v.getModelCode(), v.getModelYear());
			}
		}
	}

	/**
	 * @param model MNAO: the model of the VIN (the view content model - "CX-5" becomes "CX5"), else not used
	 * @param year  MNAO: the model year of the VIN ("MY2026" becomes "2026"), else not used
	 */
	private void addVin(PreviewDocument d, String carline, String name, String wmi, String vds, String start, String end,
			String model, String year) {
		if (mnao) {
			if (!hasText(model)) {
				return;
			}
		} else if (!hasText(carline)) {
			return;
		}
		String[] v = new String[] { t(carline), t(name), t(wmi), t(vds), t(start), t(end),
				mnao ? t(model).replace("-", "").toUpperCase() : "", mnao ? t(year).replaceAll("(?i)my", "").trim() : "" };
		String key = v[0] + "|" + v[2] + "|" + v[3] + "|" + v[4] + "|" + v[5] + (mnao ? "|" + v[6] + "|" + v[7] : "");
		String[] known = vins.get(key);
		if (null == known) {
			vins.put(key, v);
		} else if ("".equals(known[1]) && !"".equals(v[1])) {
			known[1] = v[1];
		}
		d.vinKeys.add(key);
	}

	/**
	 * WIRING DIAGRAM HTML5 document: a copy of its page in wd/<document id>/<same file name>, loading
	 * everything else from its own html5 folder in OKAssets (see PreviewPageWriter.wdPage). The folder
	 * per document keeps pages of the same name from different models apart. When the copy cannot be
	 * written the document still opens from OKAssets (fileUrl), only its voltage links do not.
	 */
	private void writeWdPage(PreviewDocument d, ContentDetails cd) throws Exception {
		File source = hasText(cd.getFileAbsolutePath()) ? PathUtil.file(cd.getFileAbsolutePath()) : null;
		if (null == source || !source.isFile()) {
			logger.info("preview :: WD page of document " + d.id + " not found :: " + cd.getFileAbsolutePath());
			return;
		}
		String web = d.fileUrl;
		String baseHref = web.substring(0, web.lastIndexOf('/') + 1);
		String name = web.substring(web.lastIndexOf('/') + 1);
		byte[] page = PreviewPageWriter.wdPage(java.nio.file.Files.readAllBytes(source.toPath()), d.id, baseHref,
				jobWebFolder(scheduleId) + PreviewPageWriter.ASSETS_FOLDER + "/" + PreviewPageWriter.PAGE_SCRIPT, wdOpenPrefix);
		File dir = PathUtil.file(PathUtil.file(folder, WD_FOLDER), d.id);
		dir.mkdirs();
		java.nio.file.Files.write(PathUtil.file(dir, name).toPath(), page);
		d.pageUrl = WD_FOLDER + "/" + d.id + "/" + name;
		pagesWritten++;
	}

	private void copyPdf(PreviewDocument d, File pdf) throws Exception {
		if (!pdf.isFile()) {
			logger.info("preview :: PDF of document " + d.id + " not found :: " + PathUtil.winPath(pdf));
			return;
		}
		File dir = PathUtil.file(PathUtil.file(folder, FILES_FOLDER), d.id);
		dir.mkdirs();
		java.nio.file.Files.copy(pdf.toPath(), PathUtil.file(dir, pdf.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		filesCopied++;
		String url = FILES_FOLDER + "/" + d.id + "/" + pdf.getName();
		for (PreviewDocument.Attachment a : d.attachments) {
			if (a.url.equals(url)) {
				return;
			}
		}
		d.attachments.add(new PreviewDocument.Attachment(pdf.getName(), url));
	}

	// ---- end of the job ----------------------------------------------------------------------

	/**
	 * Writes preview.json and the job's rows, and takes the documents this job unpublished out of
	 * the previews of earlier jobs.
	 *
	 * @param viewContent the view content rows the job built (display order positions + VINs)
	 */
	public void finish(List<MCViewContentDetails> viewContent) {
		long started = System.currentTimeMillis();
		try {
			if (!documents.isEmpty()) {
				addDisplayOrders(viewContent);
				fillManualTypeNames();
				writeText(PathUtil.file(folder, PREVIEW_FILE), json().toString());
				copyPageScript();
				PreviewDAO.saveDocuments(scheduleId, locale, documents.values());
			}
			if (!unpublished.isEmpty()) {
				removeFromEarlierJobs();
			}
			logger.info("preview :: DONE :: documents=" + documents.size() + " pages=" + pagesWritten + " files=" + filesCopied
					+ " vins=" + vins.size() + " unpublished=" + unpublished.size() + " not added=" + failures
					+ " :: folder " + PathUtil.winPath(folder) + " :: " + (System.currentTimeMillis() - started) + " ms");
		} catch (Throwable e) {
			logger.info("preview :: the preview of the job could not be completed :: " + e);
			if (e instanceof Exception) {
				Utilities.printStackTraceToLogs(PreviewBuilder.class.getName(), "finish()", (Exception) e);
			}
		}
	}

	/** Display order positions, manual type names and the VINs of each position. */
	private void addDisplayOrders(List<MCViewContentDetails> rows) {
		if (null == rows) {
			return;
		}
		for (MCViewContentDetails r : rows) {
			PreviewDocument d = null == r.getDocumentId() ? null : documents.get(r.getDocumentId().trim());
			if (null == d) {
				continue;
			}
			if (!hasText(d.manualType) && hasText(r.getManualTypeLabel())) {
				d.manualType = r.getManualTypeLabel().trim();
			}
			List<String> levels = new ArrayList<String>(Arrays.asList(r.getDisplayOrderNameLevel1(), r.getDisplayOrderNameLevel2(),
					r.getDisplayOrderNameLevel3(), r.getDisplayOrderNameLevel4(), r.getDisplayOrderNameLevel5(), r.getDisplayOrderNameLevel6()));
			List<String> placement = new ArrayList<String>();
			placement.add(t(r.getDisplayOrderSequenceNo()));
			for (String l : levels) {
				if (hasText(l)) {
					placement.add(l.trim());
				}
			}
			if (placement.size() > 1) {
				d.placements.add(placement);
			}
			addVin(d, r.getCarlineCode(), r.getCarlineNameReg(), r.getWmiCode(), r.getVdsCode(), r.getVisStartRange(), r.getVisEndRange(), null, null);
		}
	}

	/**
	 * A document with no display order row has no manual type name yet: taken from a document of
	 * the same model and manual type folder, else from the manual type table (one query, only then).
	 */
	private void fillManualTypeNames() throws Exception {
		Map<String, String> byFolder = new HashMap<String, String>();
		for (PreviewDocument d : documents.values()) {
			if (hasText(d.manualType)) {
				byFolder.put(folderKey(d), d.manualType);
			}
		}
		Map<String, String> table = null;
		for (PreviewDocument d : documents.values()) {
			if (hasText(d.manualType)) {
				continue;
			}
			d.manualType = byFolder.get(folderKey(d));
			if (hasText(d.manualType)) {
				continue;
			}
			if (null == table) {
				table = PreviewDAO.manualTypeNames(langCode());
			}
			d.manualType = table.get(manualTypeLookupKey(d));
			if (!hasText(d.manualType)) {
				d.manualType = t(d.manualTypeFolder);
			}
			byFolder.put(folderKey(d), d.manualType);
		}
	}

	/** The same rule as the display order processing (MCDocumentManagementDAO.saveDisplayOrderDetails). */
	private static String manualTypeLookupKey(PreviewDocument d) {
		String manualType = t(d.manualTypeFolder);
		if (KaptureArticleBuilder.CHANNEL_WIRING_DIAGRAMS.equals(d.channel)) {
			return "CODE:" + manualType.toLowerCase();
		}
		String refKey = isEngineOrMissionModel(d.model) ? ConversionUtils.identifyManualTypeAsCateogry(d.model)
				: ConversionUtils.identifyManualTypeAsCateogry(manualType);
		return "REF:" + t(refKey).toLowerCase();
	}

	private static boolean isEngineOrMissionModel(String model) {
		if (!hasText(model)) {
			return false;
		}
		for (String key : new String[] { "check.engine.label", "check.engine.mc.label", "check.rq.label", "check.mcm.label",
				"check.dm.label", "check.auto.mission.label", "check.manual.mission.label" }) {
			if (model.trim().equalsIgnoreCase(KaptureApiClient.require(key))) {
				return true;
			}
		}
		return false;
	}

	private JsonObject json() {
		JsonObject root = new JsonObject();
		root.addProperty("scheduleId", scheduleId);
		root.addProperty("locale", null == locale ? "" : locale);
		root.addProperty("generated", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		if (mnao) {
			root.addProperty("mnao", true);
		}

		Map<String, Integer> index = new HashMap<String, Integer>();
		JsonArray vinArray = new JsonArray();
		for (Map.Entry<String, String[]> e : vins.entrySet()) {
			String[] v = e.getValue();
			JsonObject o = new JsonObject();
			o.addProperty("carlineCode", v[0]);
			o.addProperty("carlineName", v[1]);
			o.addProperty("wmi", v[2]);
			o.addProperty("vds", v[3]);
			o.addProperty("visStart", v[4]);
			o.addProperty("visEnd", v[5]);
			if (mnao) {
				o.addProperty("model", v[6]);
				o.addProperty("year", v[7]);
			}
			index.put(e.getKey(), vinArray.size());
			vinArray.add(o);
		}

		JsonArray docs = new JsonArray();
		for (PreviewDocument d : documents.values()) {
			JsonObject o = new JsonObject();
			o.addProperty("id", d.id);
			o.addProperty("title", t(d.title));
			o.addProperty("version", t(d.version));
			o.addProperty("channel", t(d.channel));
			o.addProperty("type", t(d.type));
			o.addProperty("manualType", t(d.manualType));
			o.addProperty("url", t(d.url()));
			JsonArray atts = new JsonArray();
			for (PreviewDocument.Attachment a : d.attachments) {
				JsonObject ao = new JsonObject();
				ao.addProperty("name", a.name);
				ao.addProperty("url", a.url);
				atts.add(ao);
			}
			o.add("attachments", atts);
			JsonArray placements = new JsonArray();
			for (List<String> p : d.placements) {
				JsonArray pa = new JsonArray();
				for (String s : p) {
					pa.add(new JsonPrimitive(s));
				}
				placements.add(pa);
			}
			o.add("placements", placements);
			JsonArray vi = new JsonArray();
			for (String k : d.vinKeys) {
				vi.add(new JsonPrimitive(index.get(k)));
			}
			o.add("vins", vi);
			docs.add(o);
		}
		root.add("documents", docs);
		root.add("vins", vinArray);
		return root;
	}

	private void copyPageScript() throws Exception {
		File dir = PathUtil.file(folder, PreviewPageWriter.ASSETS_FOLDER);
		dir.mkdirs();
		java.nio.file.Files.write(PathUtil.file(dir, PreviewPageWriter.PAGE_SCRIPT).toPath(),
				PreviewPageWriter.PAGE_SCRIPT_SOURCE.getBytes(UTF8));
	}

	/** The previews of earlier jobs lose the documents this job unpublished. */
	private void removeFromEarlierJobs() throws Exception {
		Map<String, Set<String>> byJob = PreviewDAO.withdraw(scheduleId, unpublished);
		synchronized (FILE_LOCK) {
			for (Map.Entry<String, Set<String>> e : byJob.entrySet()) {
				removeDocuments(e.getKey(), e.getValue());
			}
		}
	}

	private static void removeDocuments(String otherJob, Collection<String> ids) throws Exception {
		File f = PathUtil.file(jobFolder(otherJob), PREVIEW_FILE);
		if (!f.isFile()) {
			return;
		}
		JsonObject root;
		Reader r = new InputStreamReader(PathUtil.fileInputStream(f), UTF8);
		try {
			root = new JsonParser().parse(r).getAsJsonObject();
		} finally {
			r.close();
		}
		JsonArray kept = new JsonArray();
		int removed = 0;
		for (JsonElement e : root.getAsJsonArray("documents")) {
			if (ids.contains(e.getAsJsonObject().get("id").getAsString())) {
				removed++;
			} else {
				kept.add(e);
			}
		}
		if (PreviewPublisher.unpublishedCount(kept) == 0) {
			// nothing left to preview or publish: the job loses its Preview button
			if (!f.delete()) {
				logger.info("preview :: could not delete " + PathUtil.winPath(f));
			}
		} else {
			root.add("documents", kept);
			writeText(f, root.toString());
		}
		logger.info("preview :: job " + otherJob + " :: " + removed + " document(s) removed - unpublished by a later job :: "
				+ kept.size() + " left");
	}

	// ---- helpers -----------------------------------------------------------------------------

	/** Written to a temporary file first, so a reader never sees a half written file. */
	static void writeText(File target, String text) throws Exception {
		target.getParentFile().mkdirs();
		File tmp = PathUtil.file(target.getParentFile(), target.getName() + ".tmp");
		OutputStream os = PathUtil.fileOutputStream(tmp);
		Writer w = new OutputStreamWriter(os, UTF8);
		try {
			w.write(text);
		} finally {
			w.close();
		}
		java.nio.file.Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
	}

	private String langCode() {
		return null == locale ? "" : locale.replace("_", "-");
	}

	private static String folderKey(PreviewDocument d) {
		return t(d.modelFolder).toLowerCase() + "|" + t(d.manualTypeFolder).toLowerCase();
	}

	private static boolean hasText(String s) {
		return null != s && !"".equals(s.trim());
	}

	private static String t(String s) {
		return null == s ? "" : s.trim();
	}
}
