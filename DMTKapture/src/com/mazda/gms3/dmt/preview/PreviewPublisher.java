package com.mazda.gms3.dmt.preview;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.publish.PublishDocument;
import com.mazda.gms3.dmt.utils.PathUtil;

/**
 * THE PREVIEWS AFTER A PUBLISH CONTENT JOB.
 *
 * A published document stays in the tree of every job that listed it, marked "published" in that
 * job's preview.json ({job, date, version, from, here}):
 *   the job the publish was scheduled from (here = true)  - its page and files are deleted, the
 *                                                           Preview page shows it greyed out
 *   every other job that listed it                        - its page is replaced by a notice
 *                                                           naming the publish job, date and version
 * A job with no unpublished document left loses its whole preview folder, and with it the Preview
 * button on the History page.
 *
 * Nothing here fails the publish job: errors are logged.
 */
public class PreviewPublisher {

	private static Logger logger = LogManager.getLogger(PreviewPublisher.class);
	private static final Charset UTF8 = Charset.forName("UTF-8");

	/** Documents of a preview.json that are not published yet. */
	public static int unpublishedCount(JsonArray documents) {
		int n = 0;
		for (JsonElement e : documents) {
			if (!e.getAsJsonObject().has("published")) {
				n++;
			}
		}
		return n;
	}

	/**
	 * @param sourceScheduleId the job the publish was scheduled from
	 * @param sourceName       its name
	 * @param publishName      the name of the publish job
	 * @param docs             the documents Kapture published (their otherJobs filled)
	 */
	public static void published(String sourceScheduleId, String sourceName, String publishName, List<PublishDocument> docs) {
		if (docs.isEmpty()) {
			return;
		}
		String date = new SimpleDateFormat("dd MMM yyyy HH:mm:ss").format(new Date());
		Map<String, Map<String, PublishDocument>> byJob = new LinkedHashMap<String, Map<String, PublishDocument>>();
		for (PublishDocument d : docs) {
			add(byJob, sourceScheduleId.trim(), d);
			for (String other : d.otherJobs) {
				add(byJob, other, d);
			}
		}
		synchronized (PreviewBuilder.FILE_LOCK) {
			for (Map.Entry<String, Map<String, PublishDocument>> e : byJob.entrySet()) {
				try {
					mark(e.getKey(), e.getKey().equals(sourceScheduleId.trim()), e.getValue(), sourceName, publishName, date);
				} catch (Exception ex) {
					logger.info("preview :: publish :: the preview of job " + e.getKey() + " could not be updated :: " + ex);
				}
			}
		}
	}

	private static void add(Map<String, Map<String, PublishDocument>> byJob, String job, PublishDocument d) {
		Map<String, PublishDocument> m = byJob.get(job);
		if (null == m) {
			m = new HashMap<String, PublishDocument>();
			byJob.put(job, m);
		}
		m.put(d.documentId, d);
	}

	private static void mark(String job, boolean here, Map<String, PublishDocument> docs, String sourceName, String publishName,
			String date) throws Exception {
		File folder = PreviewBuilder.jobFolder(job);
		File f = PathUtil.file(folder, PreviewBuilder.PREVIEW_FILE);
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
		JsonArray documents = root.getAsJsonArray("documents");
		int marked = 0;
		for (JsonElement e : documents) {
			JsonObject o = e.getAsJsonObject();
			PublishDocument d = docs.get(o.get("id").getAsString());
			if (null == d) {
				continue;
			}
			JsonObject p = new JsonObject();
			p.addProperty("job", publishName);
			p.addProperty("date", date);
			p.addProperty("version", null == d.publishedVersion ? "" : d.publishedVersion);
			p.addProperty("from", null == sourceName ? "" : sourceName);
			p.addProperty("here", here);
			o.add("published", p);
			o.add("attachments", new JsonArray());
			deleteTree(PathUtil.file(PathUtil.file(folder, "files"), d.documentId));
			deleteTree(PathUtil.file(PathUtil.file(folder, "wd"), d.documentId));
			File page = PathUtil.file(folder, d.documentId + ".html");
			if (here) {
				page.delete();
				o.addProperty("url", "");
			} else {
				PreviewBuilder.writeText(page, notice(d, publishName, sourceName, date));
				o.addProperty("url", d.documentId + ".html");
			}
			marked++;
		}
		int left = unpublishedCount(documents);
		if (left == 0) {
			// nothing left to preview or publish: the job loses its preview and its Preview button
			deleteTree(folder);
		} else {
			PreviewBuilder.writeText(f, root.toString());
		}
		logger.info("preview :: publish :: job " + job + (here ? " (scheduled from)" : "") + " :: " + marked + " document(s) marked published :: "
				+ left + " unpublished left" + (left == 0 ? " - preview folder deleted" : ""));
	}

	/** The page shown in another job for a document that has been published. */
	private static String notice(PublishDocument d, String publishName, String sourceName, String date) {
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + esc(d.documentId) + "</title>"
				+ "<style>body{font-family:Arial,sans-serif;margin:40px;color:#333}"
				+ ".box{border:1px solid #c8d3e0;background:#f4f7fb;padding:20px 24px;max-width:640px}"
				+ "h1{font-size:18px;margin:0 0 12px}p{margin:6px 0;font-size:14px}</style></head><body><div class=\"box\">"
				+ "<h1>" + esc(d.documentId) + " has been published</h1>"
				+ "<p>Published by job <b>" + esc(publishName) + "</b> on " + esc(date) + ".</p>"
				+ "<p>The publish job was scheduled from job <b>" + esc(sourceName) + "</b>.</p>"
				+ "<p>Published version: <b>" + esc(d.publishedVersion) + "</b></p>"
				+ "</div></body></html>";
	}

	private static String esc(String s) {
		if (null == s) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static void deleteTree(File f) {
		if (null == f || !f.exists()) {
			return;
		}
		File[] children = f.listFiles();
		if (null != children) {
			for (File c : children) {
				deleteTree(c);
			}
		}
		if (!f.delete()) {
			logger.info("preview :: publish :: could not delete " + PathUtil.winPath(f));
		}
	}
}
