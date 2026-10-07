package com.mazda.gms3.dmt.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.dao.UserProfileDAO;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.kapture.KaptureApiResult;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.preview.PreviewPublisher;
import com.mazda.gms3.dmt.utils.ThreadAbortUtil;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * PUBLISH CONTENT JOB (MC, MME and MNAO markets) - publishes the documents a DMT job left unpublished in
 * Kapture, scheduled from that job's Preview page (MGSS_PUB_MC_ / MGSS_PUB_MME_ / MGSS_PUB_MNAO_<date>_<DMT job id>).
 *
 * For every row the DMT job has in gms3_dmt_preview_docs:
 *   DELETED (by a later job, or deleted in the DMT document table)  -> not published, reported with
 *       the job that deleted it, its row removed
 *   otherwise -> its LATEST version (the DMT document table - whichever job wrote it) is published
 *       with the bulk action endpoint, publish.batch.size documents of one channel and locale per
 *       request. A document counts as published only when k_article says so (a 2xx is not success).
 *       Published: DMT document table + view content (Published) updated and the document's rows of
 *       EVERY job removed from gms3_dmt_preview_docs. Failed: its row stays - publish again later.
 * Then the previews (PreviewPublisher), PUBLISH_REPORT.xlsx + Job Summary + zip, status and mail.
 *
 * Abort (History): the job stops at its next checkpoint (every database connection and Kapture
 * call); what was published until then still gets its preview update and report.
 */
public class PublishContentImpl {

	private static Logger logger = LogManager.getLogger(PublishContentImpl.class);

	private final String publishScheduleId;
	private final String sourceScheduleId;
	private final String wslId;
	private String publishName;
	private String sourceName;
	private final List<PublishDocument> docs = new ArrayList<PublishDocument>();
	private final List<PublishDocument> published = new ArrayList<PublishDocument>();
	private final Map<String, String> contentIds = new HashMap<String, String>();
	private int requests = 0;

	public PublishContentImpl(String publishScheduleId, String sourceScheduleId, String wslId) {
		this.publishScheduleId = publishScheduleId.trim();
		this.sourceScheduleId = sourceScheduleId.trim();
		this.wslId = wslId;
	}

	public void run() {
		long started = System.currentTimeMillis();
		boolean aborted = false;
		boolean failed = false;
		logger.info("##################################################################");
		logger.info("publish :: START :: schedule " + publishScheduleId + " :: from DMT job " + sourceScheduleId);
		try {
			ScheduleDAO.updateScheduleStatus(publishScheduleId, KaptureApiClient.require("schedule.status.processing.value"));
			ScheduleDAO.updateCurrentJobStatus(publishScheduleId, null, "PUBLISH PROCESSING");
			publishName = PublishDAO.scheduleName(Long.valueOf(publishScheduleId));
			sourceName = PublishDAO.scheduleName(Long.valueOf(sourceScheduleId));

			docs.addAll(PublishDAO.loadDocuments(sourceScheduleId));
			Map<String, Long> items = PublishDAO.items(publishScheduleId);
			for (PublishDocument d : docs) {
				d.itemId = items.get(d.itemKey());
			}
			logger.info("publish :: " + docs.size() + " document(s) of job " + sourceName + " in " + items.size() + " item(s)");

			List<PublishDocument> deleted = new ArrayList<PublishDocument>();
			Map<String, List<PublishDocument>> groups = new LinkedHashMap<String, List<PublishDocument>>();
			for (PublishDocument d : docs) {
				if (PublishDAO.isDeleted(d)) {
					String by = PublishDAO.scheduleName(d.deletedByScheduleId);
					d.result = PublishDocument.RESULT_DELETED;
					d.message = "Not published - the document was deleted by " + (null != by ? "job " + by : "a later DMT job")
							+ ". See the reports of that job.";
					deleted.add(d);
				} else if (!d.inDmtTables) {
					d.result = PublishDocument.RESULT_FAILURE;
					d.message = "The document is not in the DMT document table " + d.dmtTable + ".";
				} else {
					String key = d.channel + "|" + d.locale;
					if (!groups.containsKey(key)) {
						groups.put(key, new ArrayList<PublishDocument>());
					}
					groups.get(key).add(d);
				}
			}
			countByItem(deleted, 0, 0, 1);
			PublishDAO.removeRows(sourceScheduleId, deleted);
			countByItem(failedWithoutCall(), 0, 1, 0);

			alreadyLive(groups);

			if (!groups.isEmpty()) {
				KaptureApiClient client = new KaptureApiClient();
				KaptureApiResult login = client.login();
				if (!login.isHttpOk()) {
					failAll(groups, "Kapture login failed: " + login.describe());
				} else {
					int batchSize = KaptureApiClient.requireInt("publish.batch.size");
					for (List<PublishDocument> group : groups.values()) {
						for (int from = 0; from < group.size(); from += batchSize) {
							List<PublishDocument> batch = group.subList(from, Math.min(from + batchSize, group.size()));
							try {
								publishBatch(client, batch);
							} catch (RuntimeException e) {
								Utilities.printStackTraceToLogs(PublishContentImpl.class.getName(), "run()", e);
								fail(batch, "The batch could not be published: " + e.getMessage());
							}
						}
					}
				}
			}
		} catch (ThreadAbortUtil.AbortSignal a) {
			aborted = true;
			logger.info("publish :: ABORTED :: published until then=" + published.size());
		} catch (Throwable e) {
			failed = true;
			logger.info("publish :: the job stopped on an error :: " + e);
			if (e instanceof Exception) {
				Utilities.printStackTraceToLogs(PublishContentImpl.class.getName(), "run()", (Exception) e);
			}
		}

		// documents the job did not reach (abort / error) keep their rows - they can be published again
		for (PublishDocument d : docs) {
			if (null == d.result) {
				d.result = PublishDocument.RESULT_FAILURE;
				d.message = aborted ? "Not published - the job was aborted before this document" : "Not published - the job stopped on an error";
			}
		}
		// what was published is in the database already - the previews and the report follow in any case
		try {
			if (!aborted) {
				ScheduleDAO.updateCurrentJobStatus(publishScheduleId, null, "REPORTS GENERATION");
			}
			PreviewPublisher.published(sourceScheduleId, sourceName, publishName, published);
			PublishReport.write(publishScheduleId, wslId, sourceName, docs);
		} catch (Throwable e) {
			logger.info("publish :: previews / reports :: " + e);
		}

		int ok = 0;
		int ko = 0;
		int del = 0;
		Map<Long, Boolean> itemFailed = new HashMap<Long, Boolean>();
		for (PublishDocument d : docs) {
			if (PublishDocument.RESULT_SUCCESS.equals(d.result)) {
				ok++;
			} else if (PublishDocument.RESULT_DELETED.equals(d.result)) {
				del++;
			} else {
				ko++;
				if (null != d.itemId) {
					itemFailed.put(d.itemId, Boolean.TRUE);
				}
			}
		}
		if (!aborted) {
			// History sets the status of an aborted job itself (and sends its mail)
			boolean failure = failed || ko > 0;
			ScheduleDAO.updateScheduleStatus(publishScheduleId,
					KaptureApiClient.require(failure ? "schedule.status.failure.value" : "schedule.status.success.value"));
			for (Long itemId : PublishDAO.byItemIds(docs)) {
				ScheduleDAO.updateScheduleItemStatus(String.valueOf(itemId), itemFailed.containsKey(itemId) ? "FAILURE" : "SUCCESS");
			}
			try {
				NotificationEmailHelper.generateNotificationEmail(publishScheduleId, UserProfileDAO.getUserEmail(wslId));
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(PublishContentImpl.class.getName(), "run()", e);
			}
		}
		logger.info("publish :: DONE :: published=" + ok + " failed=" + ko + " deleted=" + del + " requests=" + requests
				+ (aborted ? " :: ABORTED" : "") + " :: " + (System.currentTimeMillis() - started) + " ms");
		logger.info("##################################################################");
	}

	/**
	 * Documents whose LATEST version in Kapture is already the published one (k_article: state
	 * Published, article version = published version) need no publish call - Kapture publishes
	 * about 7 s per document. They only get the DMT updates and are taken out of the groups.
	 * This is what a re-run after a failed DMT update does, and a document published by hand in
	 * Kapture. A failed lookup is logged and every document goes to Kapture as before.
	 */
	private void alreadyLive(Map<String, List<PublishDocument>> groups) {
		Map<String, List<String>> idsByLocale = new HashMap<String, List<String>>();
		for (List<PublishDocument> g : groups.values()) {
			for (PublishDocument d : g) {
				List<String> ids = idsByLocale.get(d.locale);
				if (null == ids) {
					ids = new ArrayList<String>();
					idsByLocale.put(d.locale, ids);
				}
				ids.add(d.documentId);
			}
		}
		if (idsByLocale.isEmpty()) {
			return;
		}
		Map<String, KaptureLookupDAO.ArticleIdentity> now;
		try {
			now = KaptureLookupDAO.getArticleIdentities(idsByLocale);
		} catch (RuntimeException e) {
			logger.info("alreadyLive :: Kapture state could not be read - every document is sent to Kapture :: " + e.getMessage());
			return;
		}
		String statePublished = KaptureApiClient.require("kapture.article.state.published");
		List<PublishDocument> live = new ArrayList<PublishDocument>();
		for (List<PublishDocument> g : groups.values()) {
			for (java.util.Iterator<PublishDocument> it = g.iterator(); it.hasNext();) {
				PublishDocument d = it.next();
				KaptureLookupDAO.ArticleIdentity id = now.get(KaptureLookupDAO.identityKey(d.documentId, d.locale));
				if (null != id && null != id.articleState && statePublished.equalsIgnoreCase(id.articleState.trim())
						&& KaptureLookupDAO.hasPublishedVersion(id.publishedVersion)
						&& id.publishedVersion.trim().equals(PublishDocument.t(id.articleVersion))) {
					d.publishedVersion = id.publishedVersion;
					d.publishedRowId = String.valueOf(id.id);
					d.result = PublishDocument.RESULT_SUCCESS;
					d.message = "Already published in Kapture (version " + id.publishedVersion + ") - DMT tables updated, no publish call";
					live.add(d);
					it.remove();
				}
			}
		}
		for (java.util.Iterator<List<PublishDocument>> it = groups.values().iterator(); it.hasNext();) {
			if (it.next().isEmpty()) {
				it.remove();
			}
		}
		logger.info("alreadyLive :: " + live.size() + " document(s) already published in Kapture - DMT update only");
		int chunk = KaptureApiClient.requireInt("publish.batch.size");
		for (int from = 0; from < live.size(); from += chunk) {
			List<PublishDocument> part = live.subList(from, Math.min(from + chunk, live.size()));
			try {
				PublishDAO.published(sourceScheduleId, part);
				published.addAll(part);
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(PublishContentImpl.class.getName(), "alreadyLive()", e);
				for (PublishDocument d : part) {
					d.result = PublishDocument.RESULT_FAILURE;
					d.message = "Published in Kapture (version " + d.publishedVersion + ") but the DMT tables could not be updated: "
							+ e.getMessage() + " - publish again to update them";
				}
			}
			countByItem(part, 1, 1, 0);
		}
	}

	/** Documents failed before any Kapture call (not in the DMT tables). */
	private List<PublishDocument> failedWithoutCall() {
		List<PublishDocument> l = new ArrayList<PublishDocument>();
		for (PublishDocument d : docs) {
			if (PublishDocument.RESULT_FAILURE.equals(d.result)) {
				l.add(d);
			}
		}
		return l;
	}

	private void failAll(Map<String, List<PublishDocument>> groups, String message) {
		List<PublishDocument> all = new ArrayList<PublishDocument>();
		for (List<PublishDocument> g : groups.values()) {
			for (PublishDocument d : g) {
				d.result = PublishDocument.RESULT_FAILURE;
				d.message = message;
				all.add(d);
			}
		}
		countByItem(all, 0, 1, 0);
	}

	/**
	 * One request: documents of one channel and one locale. Every document of the batch ends with
	 * a result, whatever happens to the request.
	 */
	private void publishBatch(KaptureApiClient client, List<PublishDocument> batch) {
		ThreadAbortUtil.checkpoint();
		PublishDocument first = batch.get(0);
		String contentId = contentId(first.channel);
		if (null == contentId) {
			fail(batch, "Channel " + first.channel + " is not defined in Kapture");
			return;
		}
		String statePublished = KaptureApiClient.require("kapture.article.state.published");
		String stateUnpublished = KaptureApiClient.require("kapture.article.state.unpublished");
		String publishedFlag = KaptureApiClient.require("flag.value.publish");
		JsonArray array = new JsonArray();
		for (PublishDocument d : batch) {
			JsonObject o = new JsonObject();
			try {
				o.addProperty("id", Long.parseLong(d.rowId.trim()));
			} catch (RuntimeException e) {
				o.addProperty("id", PublishDocument.t(d.rowId));
			}
			o.addProperty("contentId", contentId);
			o.addProperty("articleId", d.documentId);
			o.addProperty("type", d.channel);
			o.addProperty("primaryLocale", d.locale);
			// the state Kapture reported when the DMT job wrote the version (the DMT document table)
			o.addProperty("articleState", publishedFlag.equals(PublishDocument.t(d.publishedFlag)) ? statePublished : stateUnpublished);
			o.addProperty("articleVersion", PublishDocument.t(d.version));
			o.addProperty("publishedVersion", "");
			o.addProperty("title", PublishDocument.t(d.title));
			o.addProperty("articleOwner", client.getUserDisplayName());
			o.addProperty("articleCreator", client.getUserDisplayName());
			o.addProperty("modifiedBy", client.getUserDisplayName());
			o.addProperty("createdBy", client.getUserDisplayName());
			array.add(o);
		}
		long t = System.currentTimeMillis();
		KaptureApiResult r = client.publishDocuments(contentId, array, first.locale, "DMT-PUB-" + publishScheduleId + "-" + (++requests));
		logger.info("publishBatch :: " + first.channel + " " + first.locale + " :: " + batch.size() + " document(s) :: " + r.describe() + " :: "
				+ (System.currentTimeMillis() - t) + " ms");
		Map<String, String[]> answers = answers(r);

		// a document is published when k_article says so - whatever the answer said
		Map<String, List<String>> ids = new HashMap<String, List<String>>();
		List<String> idList = new ArrayList<String>();
		for (PublishDocument d : batch) {
			idList.add(d.documentId);
		}
		ids.put(first.locale, idList);
		Map<String, KaptureLookupDAO.ArticleIdentity> now;
		try {
			now = KaptureLookupDAO.getArticleIdentities(ids);
		} catch (RuntimeException e) {
			fail(batch, "Published state could not be read back from Kapture: " + e.getMessage() + " :: answer " + r.describe());
			return;
		}
		List<PublishDocument> ok = new ArrayList<PublishDocument>();
		for (PublishDocument d : batch) {
			String[] answer = answers.get(d.documentId.toUpperCase());
			KaptureLookupDAO.ArticleIdentity id = now.get(KaptureLookupDAO.identityKey(d.documentId, d.locale));
			boolean live = null != id && null != id.articleState && statePublished.equalsIgnoreCase(id.articleState.trim())
					&& KaptureLookupDAO.hasPublishedVersion(id.publishedVersion);
			if (live) {
				d.publishedVersion = id.publishedVersion;
				d.publishedRowId = String.valueOf(id.id);
				d.result = PublishDocument.RESULT_SUCCESS;
				d.message = null != answer && null != answer[1] ? answer[1] : "Published";
				ok.add(d);
			} else {
				d.result = PublishDocument.RESULT_FAILURE;
				d.message = null != answer ? ("true".equals(answer[0]) ? "Kapture answered success but the document is not published (state "
						+ (null == id ? "-" : id.articleState) + "): " : "") + answer[1]
						: (r.isHttpOk() ? "No result for the document in the answer" : "Publish request failed: " + r.describe());
			}
		}
		try {
			PublishDAO.published(sourceScheduleId, ok);
			published.addAll(ok);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PublishContentImpl.class.getName(), "publishBatch()", e);
			for (PublishDocument d : ok) {
				d.result = PublishDocument.RESULT_FAILURE;
				d.message = "Published in Kapture (version " + d.publishedVersion + ") but the DMT tables could not be updated: " + e.getMessage()
						+ " - publish again to update them";
			}
		}
		countByItem(batch, 1, 1, 0);
	}

	/** articleId (upper case) -> {success, message} from results[] of the answer. */
	private static Map<String, String[]> answers(KaptureApiResult r) {
		Map<String, String[]> m = new HashMap<String, String[]>();
		if (null == r.body || "".equals(r.body.trim())) {
			return m;
		}
		try {
			JsonElement root = new JsonParser().parse(r.body);
			if (!root.isJsonObject() || !root.getAsJsonObject().has("results") || !root.getAsJsonObject().get("results").isJsonArray()) {
				return m;
			}
			for (JsonElement e : root.getAsJsonObject().getAsJsonArray("results")) {
				if (!e.isJsonObject()) {
					continue;
				}
				JsonObject o = e.getAsJsonObject();
				if (!o.has("articleId") || o.get("articleId").isJsonNull()) {
					continue;
				}
				String success = o.has("success") && !o.get("success").isJsonNull() ? o.get("success").getAsString() : "";
				String message = o.has("message") && !o.get("message").isJsonNull() ? o.get("message").getAsString() : "";
				m.put(o.get("articleId").getAsString().trim().toUpperCase(), new String[] { success.toLowerCase(), message });
			}
		} catch (RuntimeException e) {
			logger.info("answers :: the answer could not be read :: " + e);
		}
		return m;
	}

	private void fail(List<PublishDocument> batch, String message) {
		for (PublishDocument d : batch) {
			d.result = PublishDocument.RESULT_FAILURE;
			d.message = message;
		}
		countByItem(batch, 1, 1, 0);
	}

	/** Raises the schedule and item counts by the results of these documents (weights pick which results count). */
	private void countByItem(List<PublishDocument> list, int publishedWeight, int failedWeight, int deletedWeight) {
		Map<Long, int[]> byItem = new LinkedHashMap<Long, int[]>();
		for (PublishDocument d : list) {
			int[] c = byItem.get(d.itemId);
			if (null == c) {
				c = new int[3];
				byItem.put(d.itemId, c);
			}
			if (PublishDocument.RESULT_SUCCESS.equals(d.result)) {
				c[0] += publishedWeight;
			} else if (PublishDocument.RESULT_DELETED.equals(d.result)) {
				c[2] += deletedWeight;
			} else {
				c[1] += failedWeight;
			}
		}
		for (Map.Entry<Long, int[]> e : byItem.entrySet()) {
			PublishDAO.addCounts(publishScheduleId, e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]);
		}
	}

	private String contentId(String channel) {
		if (!contentIds.containsKey(channel)) {
			contentIds.put(channel, KaptureLookupDAO.getChannelContentId(KaptureApiClient.require("kapture.channel.prefix." + channel)));
		}
		return contentIds.get(channel);
	}
}
