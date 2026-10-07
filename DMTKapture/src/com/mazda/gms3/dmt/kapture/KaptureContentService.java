package com.mazda.gms3.dmt.kapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * WRITES DOCUMENTS TO KAPTURE - create, update and unpublish - for one conversion job.
 *
 * This is what the conversion calls where it used to call the InfoManager content record
 * request. One instance per job thread.
 *
 * RULES BUILT IN HERE (learned from the bulk content import; do not relax them):
 *  - A document is ALWAYS sent unpublished (pubFlag false). Publishing is a separate job.
 *  - A 2xx answer is not success. Each document's own status in the answer decides.
 *  - On an update the document's CURRENT state is read from Kapture first and echoed; a
 *    wrong or missing state is rejected by Kapture.
 *  - A request that timed out is never sent again: the server may still be processing it.
 *  - A new document is sent WITHOUT an id; Kapture assigns one and returns it.
 */
public class KaptureContentService {

	private static Logger logger = LogManager.getLogger(KaptureContentService.class);

	public static final String ERR_REQUEST_FAILED = "KAPTURE_REQUEST_FAILED";
	public static final String ERR_NOT_FOUND = "KAPTURE_DOCUMENT_NOT_FOUND";
	public static final String ERR_CHANNEL_UNKNOWN = "KAPTURE_CHANNEL_NOT_FOUND";
	public static final String ERR_ATTACHMENT = "KAPTURE_ATTACHMENT_UPLOAD_FAILED";
	public static final String ERR_NO_RESULT = "KAPTURE_NO_RESULT_FOR_DOCUMENT";
	/** Kapture's answer to a create of a document it already holds (same legacy id / source); carries that document's id */
	public static final String ERR_DUPLICATE_SKIPPED = "DUPLICATE_SKIPPED";
	/** A document Kapture already held (DUPLICATE_SKIPPED) is saved, but its update in this run failed */
	public static final String ERR_DUPLICATE_UPDATE_FAILED = "KAPTURE_EXISTING_DOCUMENT_NOT_UPDATED";

	private final KaptureApiClient client = new KaptureApiClient();
	private final KaptureHtmlSanitizer sanitizer;
	private final String statePublished = KaptureApiClient.require("kapture.article.state.published");
	private final String stateUnpublished = KaptureApiClient.require("kapture.article.state.unpublished");
	// loaded with the service so a missing OTHER_MANUAL_LINK_RULE key stops the job at its start
	private final OtherManualLinkRules otherManualLinkRules = OtherManualLinkRules.get();

	/** Kapture user groups ({refKey, name}) every document of this job is given. */
	private final List<String[]> userGroups = new ArrayList<String[]>();
	/** Kapture views ({refKey, name}) every document of this job is given. */
	private final List<String[]> views = new ArrayList<String[]>();
	/** channel type -> contentId, read once per channel. */
	private final Map<String, String> contentIds = new HashMap<String, String>();

	private final String runStamp = String.valueOf(System.currentTimeMillis());
	private int requestCounter = 0;

	/**
	 * @param userGroupRefKeysCsv Kapture user group reference keys, comma separated
	 * @param viewRefKeysCsv      Kapture view reference keys, comma separated
	 *
	 * A reference key Kapture does not know stops the job here, before any document is sent:
	 * sending it would fail every single document.
	 */
	public KaptureContentService(String userGroupRefKeysCsv, String viewRefKeysCsv) {
		this(userGroupRefKeysCsv, viewRefKeysCsv, ConversionUtils::isConversionStyleLink);
	}

	/**
	 * @param keepLink true for the href of a stylesheet link the conversion of this market adds
	 *                 (ConversionUtils::isConversionStyleLink for MC, ::isConversionStyleLinkMME for MME)
	 */
	public KaptureContentService(String userGroupRefKeysCsv, String viewRefKeysCsv, java.util.function.Predicate<String> keepLink) {
		this.sanitizer = new KaptureHtmlSanitizer(keepLink);
		for (String ref : csv(userGroupRefKeysCsv)) {
			String name = KaptureLookupDAO.getUserGroupName(ref);
			if (null == name) {
				throw new IllegalStateException("user group " + ref + " is not defined in Kapture");
			}
			userGroups.add(new String[] { ref, name });
		}
		for (String ref : csv(viewRefKeysCsv)) {
			String name = KaptureLookupDAO.getViewName(ref);
			if (null == name) {
				throw new IllegalStateException("view " + ref + " is not defined in Kapture");
			}
			views.add(new String[] { ref, name });
		}
		KaptureApiResult lr = client.login();
		if (!lr.isHttpOk()) {
			throw new IllegalStateException("Kapture login failed: " + lr.describe());
		}
	}

	// ---- create ------------------------------------------------------------

	/**
	 * Creates one document - a batch of one, so a document with attachments goes through the
	 * same create-then-attach steps as a batch (see attachAfterCreate).
	 */
	public KaptureRecordResult create(KaptureArticle article) {
		BatchItem it = new BatchItem(article, null);
		List<BatchItem> one = new ArrayList<BatchItem>();
		one.add(it);
		createBatch(one);
		return null == it.result ? KaptureRecordResult.failure(ERR_NO_RESULT, "No result for the document") : it.result;
	}

	// ---- update ------------------------------------------------------------

	/**
	 * Update the latest version of article.documentId. The result is errorCode ERR_NOT_FOUND
	 * when Kapture holds no such document - the caller decides what that means.
	 */
	public KaptureRecordResult update(KaptureArticle article) {
		try {
			String contentId = contentIdFor(article.channelType);
			if (null == contentId) {
				return KaptureRecordResult.failure(ERR_CHANNEL_UNKNOWN,
						"Channel " + article.channelType + " is not defined in Kapture");
			}
			KaptureLookupDAO.ArticleIdentity identity =
					KaptureLookupDAO.getArticleIdentity(article.documentId, article.locale);
			if (null == identity) {
				return KaptureRecordResult.failure(ERR_NOT_FOUND,
						"Document " + article.documentId + " (" + article.locale + ") is not present in Kapture");
			}
			JsonObject payload = buildPayload(article, identity);
			if (null == payload) {
				return KaptureRecordResult.failure(ERR_ATTACHMENT, lastAttachmentError);
			}
			JsonArray array = new JsonArray();
			array.add(payload);
			KaptureApiResult r = client.bulkUpdate(contentId, 1, array.toString(), correlationId("U"));
			return readSingleResult(r, article);
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "update()", e);
			return KaptureRecordResult.failure(ERR_REQUEST_FAILED, e.getMessage());
		}
	}

	// ---- bulk: many documents per request --------------------------------------

	/**
	 * One document of a bulk write. The caller fills article (and owner, its own handle for the
	 * document); result is filled here - for EVERY item, whatever happens to its request.
	 */
	public static class BatchItem {
		public final KaptureArticle article;
		public final Object owner;
		public KaptureRecordResult result;
		String json;
		int bytes;
		KaptureLookupDAO.ArticleIdentity identity;
		/** a new document with attachments: created without them, attached once it has an id */
		boolean attachLater;

		public BatchItem(KaptureArticle article, Object owner) {
			this.article = article;
			this.owner = owner;
		}
	}

	/** Creates every item's document, as many per request as the limits allow. */
	public void createBatch(List<BatchItem> items) {
		writeBatch(items, false);
	}

	/**
	 * Updates every item's document (article.documentId). An item whose document Kapture does
	 * not hold gets errorCode ERR_NOT_FOUND - the caller decides what that means.
	 */
	public void updateBatch(List<BatchItem> items) {
		writeBatch(items, true);
	}

	/**
	 * THE SAME RULES AS THE BULK CONTENT IMPORT JOB:
	 *  - creates and updates are never mixed in one request (separate endpoints);
	 *  - a request closes when the next document would pass the document count or the byte
	 *    limit (kapture.bulk.max.articles.per.request / kapture.bulk.max.request.bytes);
	 *  - the same document id never appears twice in one request (results could not be told
	 *    apart);
	 *  - a document that alone is larger than the byte limit is failed, never truncated;
	 *  - each result is matched to its document by sourceIdentifier (unique per document),
	 *    sequence number as the fallback; a document with no result is FAILED, never assumed
	 *    written;
	 *  - a request that timed out is not sent again.
	 */
	private void writeBatch(List<BatchItem> items, boolean update) {
		if (null == items || items.isEmpty()) {
			return;
		}
		int maxArticles = KaptureApiClient.requireInt("kapture.bulk.max.articles.per.request");
		long maxBytes = Long.parseLong(KaptureApiClient.require("kapture.bulk.max.request.bytes"));
		long started = System.currentTimeMillis();
		int requestsBefore = requestCounter;

		// one request per channel: the contentId is part of the url
		Map<String, List<BatchItem>> byChannel = new java.util.LinkedHashMap<String, List<BatchItem>>();
		for (BatchItem it : items) {
			List<BatchItem> l = byChannel.get(it.article.channelType);
			if (null == l) {
				l = new ArrayList<BatchItem>();
				byChannel.put(it.article.channelType, l);
			}
			l.add(it);
		}
		for (Map.Entry<String, List<BatchItem>> ch : byChannel.entrySet()) {
			String contentId;
			try {
				contentId = contentIdFor(ch.getKey());
			} catch (RuntimeException e) {
				Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "writeBatch()", e);
				failAll(ch.getValue(), ERR_REQUEST_FAILED, e.getMessage());
				continue;
			}
			if (null == contentId) {
				failAll(ch.getValue(), ERR_CHANNEL_UNKNOWN, "Channel " + ch.getKey() + " is not defined in Kapture");
				continue;
			}
			List<BatchItem> ready = prepare(ch.getValue(), update);

			// pack
			List<BatchItem> request = new ArrayList<BatchItem>();
			java.util.Set<String> idsInRequest = new java.util.HashSet<String>();
			long bytes = 2;
			for (BatchItem it : ready) {
				if (it.bytes + 2 > maxBytes) {
					it.result = KaptureRecordResult.failure(ERR_REQUEST_FAILED, "The document is larger ("
							+ it.bytes + " bytes) than one Kapture request may be (kapture.bulk.max.request.bytes="
							+ maxBytes + "). It was not sent.");
					continue;
				}
				String id = update ? it.article.documentId : null;
				boolean full = !request.isEmpty() && (request.size() + 1 > maxArticles
						|| bytes + it.bytes + 1 > maxBytes || (null != id && idsInRequest.contains(id)));
				if (full) {
					send(contentId, request, update, bytes);
					request = new ArrayList<BatchItem>();
					idsInRequest.clear();
					bytes = 2;
				}
				request.add(it);
				bytes += it.bytes + 1;
				if (null != id) {
					idsInRequest.add(id);
				}
			}
			if (!request.isEmpty()) {
				send(contentId, request, update, bytes);
			}
		}
		summarise(items, update, requestCounter - requestsBefore, System.currentTimeMillis() - started);
		if (!update) {
			retryWithoutAttachments(items);
			attachAfterCreate(items);
			adoptDuplicates(items);
		}
	}

	/**
	 * NEW DOCUMENTS KAPTURE ALREADY HOLDS. A create Kapture answers DUPLICATE_SKIPPED is a document an
	 * earlier job created in Kapture but never saved in the database (the job stopped between the two,
	 * e.g. aborted). Kapture returns that document's id: the content is sent as an UPDATE of it, and the
	 * create takes that update's result, so the caller saves the document as for any new one and the
	 * next run updates it - instead of being refused as a duplicate on every run.
	 * Should that update fail, the document is STILL saved (it exists in Kapture - the same rule as
	 * attachAfterCreate): the create returns success with the Kapture id and a warning, which goes to
	 * the failure report and keeps the document out of preview / publish; the next run updates it.
	 */
	private void adoptDuplicates(List<BatchItem> items) {
		List<BatchItem> created = new ArrayList<BatchItem>();
		List<BatchItem> followUps = new ArrayList<BatchItem>();
		for (BatchItem it : items) {
			if (null != it.result && !it.result.success && ERR_DUPLICATE_SKIPPED.equalsIgnoreCase(nz(it.result.errorCode).trim())
					&& null != it.result.documentId && !"".equals(it.result.documentId.trim())) {
				it.article.documentId = it.result.documentId.trim();
				created.add(it);
				followUps.add(new BatchItem(it.article, it.owner));
			}
		}
		if (created.isEmpty()) {
			return;
		}
		long t0 = System.currentTimeMillis();
		logger.info("duplicates :: " + created.size() + " new document(s) already in Kapture (DUPLICATE_SKIPPED - created by an"
				+ " earlier job, not saved in the database) - updating them under their Kapture ids");
		try {
			writeBatch(followUps, true);
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "adoptDuplicates()", e);
			failAll(followUps, ERR_REQUEST_FAILED, e.getMessage());
		}
		int ok = 0;
		for (int i = 0; i < created.size(); i++) {
			BatchItem c = created.get(i);
			KaptureRecordResult f = followUps.get(i).result;
			if (null != f && f.success) {
				ok++;
				c.result = f;
			} else {
				// saved all the same - the document exists in Kapture; the failed update is reported
				KaptureRecordResult kept = new KaptureRecordResult();
				kept.success = true;
				kept.documentId = c.result.documentId.trim();
				kept.warningCode = ERR_DUPLICATE_UPDATE_FAILED;
				kept.warning = "Kapture already held the document as " + kept.documentId + " (DUPLICATE_SKIPPED - created by an"
						+ " earlier job, not saved in the database). It is saved now, but updating it failed: "
						+ (null == f ? "no result" : f.errorCode + " " + f.message) + ". Run the folder again to update it.";
				c.result = kept;
			}
			logger.info("duplicates :: " + (null != f && f.success ? "TAKEN OVER" : "SAVED, UPDATE FAILED") + " :: " + c.result.documentId
					+ " :: source=" + c.article.sourceIdentifier);
		}
		logger.info("duplicates :: taken over=" + ok + " saved with update failed=" + (created.size() - ok) + " :: "
				+ (System.currentTimeMillis() - t0) + " ms");
	}

	/**
	 * FALLBACK for new documents refused BECAUSE OF AN ATTACHMENT. A new document's files are
	 * uploaded with a unique id in place of the document id it does not have yet (see
	 * uploadDocumentId). Should Kapture refuse that - the upload, or the create when it ties the
	 * file to the article - the document is NOT created, so it is safe to create it again
	 * without its files and add them once it has an id (attachAfterCreate). A missing file is
	 * not retried: it would fail the same way.
	 */
	private void retryWithoutAttachments(List<BatchItem> items) {
		List<BatchItem> retry = new ArrayList<BatchItem>();
		for (BatchItem it : items) {
			if (it.attachLater || null == it.result || it.result.success) {
				continue;
			}
			String msg = nz(it.result.message).toLowerCase();
			boolean attachmentProblem = (ERR_ATTACHMENT.equals(it.result.errorCode) || msg.contains("attach"))
					&& !msg.contains("file not found");
			if (!attachmentProblem) {
				continue;
			}
			List<KaptureArticle.Attachment> atts = new ArrayList<KaptureArticle.Attachment>();
			collectAttachments(it.article.fields, atts);
			if (atts.isEmpty()) {
				continue;
			}
			logger.info("attachments :: RETRY :: source=" + it.article.sourceIdentifier + " was refused because of its"
					+ " attachment (" + it.result.errorCode + " " + it.result.message + ") - creating it without the"
					+ " attachment, which is then added under the new document id");
			it.attachLater = true;
			it.result = null;
			it.json = null;
			it.bytes = 0;
			retry.add(it);
		}
		if (!retry.isEmpty()) {
			// writeBatch -> attachAfterCreate adds the files of the re-created documents
			writeBatch(retry, false);
		}
	}

	/**
	 * THE ATTACHMENTS OF NEW DOCUMENTS. The temp-attachment upload REQUIRES the document id of
	 * every file (attachments[i].legacyDocumentId - "is required", 400) and Kapture checks it
	 * against the article when it finalises the file, but a new document has no id until Kapture
	 * creates it. So a new document with attachments is created WITHOUT them, and once the create
	 * has returned its id, its files are uploaded under that id and added in ONE bulk update for
	 * all of them - the same packing and upload batching as any update.
	 *
	 * The create result is kept either way: the document exists in Kapture, so it must be saved
	 * in the database (a re-run then updates it, and uploads the files again, instead of creating
	 * a second document). A document whose files could not be added carries result.warning,
	 * which goes to the failure report.
	 */
	private void attachAfterCreate(List<BatchItem> items) {
		List<BatchItem> created = new ArrayList<BatchItem>();
		List<BatchItem> followUps = new ArrayList<BatchItem>();
		for (BatchItem it : items) {
			if (it.attachLater && null != it.result && it.result.success) {
				// done here once - the outer writeBatch of a fallback pass must not add it again
				it.attachLater = false;
				it.article.documentId = it.result.documentId;
				created.add(it);
				followUps.add(new BatchItem(it.article, it.owner));
			}
		}
		if (created.isEmpty()) {
			return;
		}
		long t0 = System.currentTimeMillis();
		logger.info("attachments :: " + created.size() + " new document(s) created without their attachments (the upload needs"
				+ " the document id) - adding them now in a bulk update");
		try {
			writeBatch(followUps, true);
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "attachAfterCreate()", e);
			failAll(followUps, ERR_REQUEST_FAILED, e.getMessage());
		}
		int ok = 0;
		for (int i = 0; i < created.size(); i++) {
			KaptureRecordResult c = created.get(i).result;
			KaptureRecordResult f = followUps.get(i).result;
			if (null != f && f.success) {
				ok++;
				c.rowId = f.rowId;
				c.articleVersion = f.articleVersion;
				c.publishedVersion = f.publishedVersion;
				c.articleState = f.articleState;
			} else {
				c.warning = "Document created in Kapture as " + c.documentId + " but its attachment(s) could not be added: "
						+ (null == f ? "no result" : f.errorCode + " " + f.message)
						+ ". The document is saved; run the folder again to add them.";
				logger.info("attachments :: NOT ADDED :: " + c.documentId + " :: source=" + created.get(i).article.sourceIdentifier
						+ " :: " + (null == f ? "no result" : f.errorCode + " " + f.message));
			}
		}
		logger.info("attachments :: added to new documents=" + ok + " not added=" + (created.size() - ok) + " :: "
				+ (System.currentTimeMillis() - t0) + " ms");
	}

	/**
	 * ONE line per bulk write with the totals and the time, and one line per document that failed
	 * BEFORE it was sent (no request carried it, so no result line was logged for it).
	 */
	private static void summarise(List<BatchItem> items, boolean update, int requests, long ms) {
		int ok = 0;
		int failed = 0;
		Map<String, Integer> byError = new java.util.TreeMap<String, Integer>();
		for (BatchItem it : items) {
			if (null != it.result && it.result.success) {
				ok++;
				continue;
			}
			failed++;
			String code = null == it.result ? "NO_RESULT" : String.valueOf(it.result.errorCode);
			byError.put(code, (byError.containsKey(code) ? byError.get(code) : 0) + 1);
			if (null == it.json && null != it.result) {
				logger.info("bulk " + (update ? "UPDATE" : "CREATE") + " :: NOT SENT :: source=" + it.article.sourceIdentifier
						+ (null == it.article.documentId ? "" : " id=" + it.article.documentId) + " :: " + it.result.errorCode
						+ " " + it.result.message);
			}
		}
		logger.info("bulk " + (update ? "UPDATE" : "CREATE") + " SUMMARY :: documents=" + items.size() + " requests=" + requests
				+ " success=" + ok + " failed=" + failed + (byError.isEmpty() ? "" : " errors=" + byError) + " :: " + ms + " ms"
				+ (items.isEmpty() ? "" : " (" + (ms / items.size()) + " ms per document)"));
	}

	// ---- attachments of a batch: uploaded several per request ----------------------

	/** Attachments already uploaded for the batch being prepared -> the payload object. */
	private final Map<KaptureArticle.Attachment, JsonObject> preUploaded =
			new java.util.IdentityHashMap<KaptureArticle.Attachment, JsonObject>();
	/** Attachments of the batch that could not be uploaded -> why. */
	private final Map<KaptureArticle.Attachment, String> preUploadErrors =
			new java.util.IdentityHashMap<KaptureArticle.Attachment, String>();

	/** true while the payload of a new document whose files are added after the create is built */
	private boolean skipAttachments = false;

	private static class PendingUpload {
		final KaptureArticle.Attachment attachment;
		final KaptureApiClient.UploadFile file;

		PendingUpload(KaptureArticle.Attachment attachment, KaptureApiClient.UploadFile file) {
			this.attachment = attachment;
			this.file = file;
		}
	}

	/**
	 * Uploads the attachments of every item of the batch BEFORE the payloads are built, packed
	 * into requests of at most kapture.attachment.upload.max.files files and
	 * kapture.attachment.upload.max.bytes bytes (the bulk content import's limits: the endpoint
	 * bounced requests of 15 files or more). A refused request is halved until a single file is
	 * left, so one bad file never fails the files it travelled with. A file larger than the byte
	 * limit goes on its own. The results are matched by OUR clientAttachmentId, never by position.
	 */
	private void preUploadAttachments(List<BatchItem> items) {
		List<PendingUpload> pending = new ArrayList<PendingUpload>();
		for (BatchItem it : items) {
			List<KaptureArticle.Attachment> atts = new ArrayList<KaptureArticle.Attachment>();
			collectAttachments(it.article.fields, atts);
			for (KaptureArticle.Attachment att : atts) {
				if (null == att.file || !att.file.isFile()) {
					preUploadErrors.put(att, "Attachment file not found: " + (null == att.file ? "null" : att.file.getPath()));
					logger.info("attachments :: FILE NOT FOUND :: " + (null == att.file ? "null" : PathUtil.winPath(att.file))
							+ " :: source=" + it.article.sourceIdentifier);
					continue;
				}
				String clientId = "CAI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
				pending.add(new PendingUpload(att, new KaptureApiClient.UploadFile(att.file, clientId,
						uploadDocumentId(it.article, clientId), it.article.locale)));
			}
		}
		if (pending.isEmpty()) {
			return;
		}
		int maxFiles = KaptureApiClient.requireInt("kapture.attachment.upload.max.files");
		long maxBytes = Long.parseLong(KaptureApiClient.require("kapture.attachment.upload.max.bytes"));
		List<List<PendingUpload>> groups = new ArrayList<List<PendingUpload>>();
		List<PendingUpload> cur = new ArrayList<PendingUpload>();
		long bytes = 0;
		for (PendingUpload p : pending) {
			long len = p.file.file.length();
			if (!cur.isEmpty() && (cur.size() >= maxFiles || bytes + len > maxBytes)) {
				groups.add(cur);
				cur = new ArrayList<PendingUpload>();
				bytes = 0;
			}
			cur.add(p);
			bytes += len;
		}
		groups.add(cur);
		logger.info("attachments :: " + pending.size() + " file(s) of " + items.size() + " document(s) -> " + groups.size()
				+ " upload request(s) (at most " + maxFiles + " files / " + maxBytes + " bytes each)");
		long t0 = System.currentTimeMillis();
		for (List<PendingUpload> g : groups) {
			uploadGroup(g);
		}
		logger.info("attachments :: uploaded=" + preUploaded.size() + " failed=" + preUploadErrors.size() + " :: "
				+ (System.currentTimeMillis() - t0) + " ms");
	}

	/**
	 * attachments[i].legacyDocumentId of an upload. The endpoint requires one for every file
	 * ("is required", 400), but a new document has no id until Kapture creates it - so a new
	 * document's file carries its own unique id (the clientAttachmentId) in its place.
	 */
	private static String uploadDocumentId(KaptureArticle a, String clientId) {
		return null == a.documentId || "".equals(a.documentId.trim()) ? clientId : a.documentId;
	}

	private static void collectAttachments(Map<String, Object> fields, List<KaptureArticle.Attachment> out) {
		for (Object v : fields.values()) {
			if (v instanceof KaptureArticle.Attachment) {
				out.add((KaptureArticle.Attachment) v);
			} else if (v instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> child = (Map<String, Object>) v;
				collectAttachments(child, out);
			}
		}
	}

	private void uploadGroup(List<PendingUpload> group) {
		List<KaptureApiClient.UploadFile> files = new ArrayList<KaptureApiClient.UploadFile>();
		for (PendingUpload p : group) {
			files.add(p.file);
		}
		// no backoff retries while the group can still be split
		KaptureApiResult r = client.uploadTempAttachments(files, group.size() == 1);
		if ((r.httpStatus == 413 || r.httpStatus == 0) && group.size() > 1) {
			int mid = group.size() / 2;
			logger.info("attachments :: upload of " + group.size() + " files refused (" + r.describe() + ") - splitting into "
					+ mid + " + " + (group.size() - mid));
			uploadGroup(new ArrayList<PendingUpload>(group.subList(0, mid)));
			uploadGroup(new ArrayList<PendingUpload>(group.subList(mid, group.size())));
			return;
		}
		if (!r.isHttpOk()) {
			for (PendingUpload p : group) {
				preUploadErrors.put(p.attachment, "Attachment upload failed for " + p.file.file.getName() + ": " + r.describe());
				logger.info("attachments :: UPLOAD FAILED :: " + PathUtil.winPath(p.file.file) + " :: " + r.describe());
			}
			return;
		}
		Map<String, JsonObject> byClientId = new HashMap<String, JsonObject>();
		try {
			JsonElement root = new JsonParser().parse(r.body);
			if (root.isJsonArray()) {
				JsonArray arr = root.getAsJsonArray();
				for (int i = 0; i < arr.size(); i++) {
					JsonObject e = arr.get(i).getAsJsonObject();
					String cid = KaptureApiClient.str(e, "clientAttachmentId");
					if (null != cid) {
						byClientId.put(cid, e);
					}
				}
			}
		} catch (Exception ex) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "uploadGroup()", ex);
		}
		for (PendingUpload p : group) {
			JsonObject e = byClientId.get(p.file.clientAttachmentId);
			JsonObject att = null == e ? null : attachmentObject(e, p.attachment, p.file.clientAttachmentId);
			if (null == att) {
				preUploadErrors.put(p.attachment, "Attachment upload for " + p.file.file.getName()
						+ " returned no temporary attachment id");
				logger.info("attachments :: NO TEMP ID RETURNED :: " + PathUtil.winPath(p.file.file) + " clientAttachmentId="
						+ p.file.clientAttachmentId);
			} else {
				preUploaded.put(p.attachment, att);
			}
		}
	}

	/** Builds each item's payload; an item that cannot be built gets its failure here. */
	private List<BatchItem> prepare(List<BatchItem> items, boolean update) {
		preUploaded.clear();
		preUploadErrors.clear();
		try {
			return prepareItems(items, update);
		} finally {
			preUploaded.clear();
			preUploadErrors.clear();
		}
	}

	private List<BatchItem> prepareItems(List<BatchItem> items, boolean update) {
		List<BatchItem> ready = new ArrayList<BatchItem>();
		Map<String, KaptureLookupDAO.ArticleIdentity> identities = null;
		if (update) {
			try {
				long t0 = System.currentTimeMillis();
				identities = KaptureLookupDAO.getArticleIdentities(items);
				logger.info("prepare :: documents to update=" + items.size() + " found in Kapture=" + identities.size()
						+ " :: " + (System.currentTimeMillis() - t0) + " ms");
			} catch (RuntimeException e) {
				failAll(items, ERR_REQUEST_FAILED, e.getMessage());
				return ready;
			}
		}
		// the attachments of every document that will be sent, several per upload request
		List<BatchItem> toUpload = new ArrayList<BatchItem>();
		int attachLater = 0;
		for (BatchItem it : items) {
			if (it.attachLater) {
				// fallback pass: created without its files, they are added after the create
				attachLater++;
			} else if (!update || identities.containsKey(KaptureLookupDAO.identityKey(it.article.documentId, it.article.locale))) {
				toUpload.add(it);
			}
		}
		if (attachLater > 0) {
			logger.info("prepare :: " + attachLater + " of " + items.size() + " new document(s) created WITHOUT their"
					+ " attachments - added after the create");
		}
		try {
			preUploadAttachments(toUpload);
		} catch (RuntimeException e) {
			// whatever was not uploaded here is uploaded one by one while the payload is built
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "preUploadAttachments()", e);
		}
		long buildStarted = System.currentTimeMillis();
		for (BatchItem it : items) {
			try {
				if (update) {
					it.identity = identities.get(KaptureLookupDAO.identityKey(it.article.documentId, it.article.locale));
					if (null == it.identity) {
						it.result = KaptureRecordResult.failure(ERR_NOT_FOUND, "Document " + it.article.documentId
								+ " (" + it.article.locale + ") is not present in Kapture");
						continue;
					}
				}
				JsonObject payload;
				skipAttachments = it.attachLater;
				try {
					payload = buildPayload(it.article, update ? it.identity : null);
				} finally {
					skipAttachments = false;
				}
				if (null == payload) {
					it.result = KaptureRecordResult.failure(ERR_ATTACHMENT, lastAttachmentError);
					continue;
				}
				it.json = payload.toString();
				it.bytes = it.json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
				ready.add(it);
			} catch (RuntimeException e) {
				Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "prepare()", e);
				it.result = KaptureRecordResult.failure(ERR_REQUEST_FAILED, e.getMessage());
			}
		}
		logger.info("prepare :: payloads built=" + ready.size() + " of " + items.size() + " :: "
				+ (System.currentTimeMillis() - buildStarted) + " ms");
		return ready;
	}

	private void send(String contentId, List<BatchItem> request, boolean update, long bytes) {
		StringBuilder array = new StringBuilder((int) Math.min(Integer.MAX_VALUE, bytes + 16));
		array.append('[');
		for (int i = 0; i < request.size(); i++) {
			array.append(i > 0 ? "," : "").append(request.get(i).json);
		}
		array.append(']');
		String corr = correlationId(update ? "U" : "C");
		logger.info("bulk " + (update ? "UPDATE" : "CREATE") + " :: " + corr + " :: documents=" + request.size()
				+ " bytes=" + bytes);
		KaptureApiResult r;
		try {
			r = update ? client.bulkUpdate(contentId, request.size(), array.toString(), corr)
					: client.bulkCreate(contentId, request.size(), array.toString(), corr);
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "send()", e);
			failAll(request, ERR_REQUEST_FAILED, e.getMessage());
			return;
		}
		if (r.timedOut) {
			failAll(request, ERR_REQUEST_FAILED, "Kapture did not answer in time (request " + corr + "). The"
					+ " documents may or may not have been written; they were not sent again. Check them in Kapture.");
			return;
		}
		if (!r.isHttpOk()) {
			failAll(request, ERR_REQUEST_FAILED, "Kapture request failed: " + r.describe());
			return;
		}
		JsonArray arr;
		try {
			arr = resultsArray(new JsonParser().parse(r.body).getAsJsonObject());
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "send()", e);
			failAll(request, ERR_REQUEST_FAILED, "Kapture answer could not be read: " + e.getMessage());
			return;
		}
		Map<String, BatchItem> bySource = new HashMap<String, BatchItem>();
		for (BatchItem it : request) {
			bySource.put(nz(it.article.sourceIdentifier), it);
		}
		int seqBase = Integer.MAX_VALUE;
		if (null != arr) {
			for (int i = 0; i < arr.size(); i++) {
				String s = KaptureApiClient.str(arr.get(i).getAsJsonObject(), "sequenceNumber");
				try {
					seqBase = Math.min(seqBase, Integer.parseInt(s));
				} catch (Exception e) {
					// no sequence number on this record
				}
			}
			for (int i = 0; i < arr.size(); i++) {
				JsonObject rec = arr.get(i).getAsJsonObject();
				BatchItem it = bySource.get(nz(KaptureApiClient.str(rec, "sourceIdentifier")));
				if (null == it) {
					try {
						int idx = Integer.parseInt(KaptureApiClient.str(rec, "sequenceNumber")) - seqBase;
						if (idx >= 0 && idx < request.size()) {
							it = request.get(idx);
						}
					} catch (Exception e) {
						// cannot be placed
					}
				}
				if (null == it || null != it.result) {
					logger.info("bulk :: " + corr + " :: a result could not be matched to a sent document :: "
							+ KaptureApiClient.cut(rec.toString()));
					continue;
				}
				it.result = parseRecord(rec);
				logResult(it.article, it.result, it.json);
			}
		}
		for (BatchItem it : request) {
			if (null == it.result) {
				it.result = KaptureRecordResult.failure(ERR_NO_RESULT, "Kapture returned no result for the document"
						+ " in request " + corr + ".");
				logResult(it.article, it.result, it.json);
			}
		}
	}

	private static void failAll(List<BatchItem> items, String code, String message) {
		for (BatchItem it : items) {
			if (null == it.result) {
				it.result = KaptureRecordResult.failure(code, message);
			}
		}
	}

	// ---- unpublish ---------------------------------------------------------

	/**
	 * Unpublish a document. Its content, categories and attachments are left exactly as they
	 * are. The answer of the unpublish call decides - no read-back from k_article (its
	 * published_version is not kept current on drafts saved before an unpublish):
	 *   status 2xx in the answer ("Article Unpublished Successfully" or "Article is already
	 *   Unpublished") with the article in it = done; the row id, version and state come from it.
	 */
	public KaptureRecordResult unpublish(String documentId, String channelType, String locale) {
		try {
			String contentId = contentIdFor(channelType);
			if (null == contentId) {
				return KaptureRecordResult.failure(ERR_CHANNEL_UNKNOWN,
						"Channel " + channelType + " is not defined in Kapture");
			}
			KaptureApiResult r = client.unpublish(contentId, documentId, locale);
			if (!r.isOk()) {
				return KaptureRecordResult.failure(ERR_REQUEST_FAILED, "Unpublish failed: " + r.describe());
			}
			// response.retrieveArticleTabInfoVo[0] = the article Kapture acted on
			JsonObject article = null;
			String answer = "";
			try {
				JsonObject root = new JsonParser().parse(r.body).getAsJsonObject();
				answer = root.has("message") && root.get("message").isJsonPrimitive() ? root.get("message").getAsString() : "";
				JsonObject response = root.has("response") && root.get("response").isJsonObject() ? root.getAsJsonObject("response") : null;
				JsonArray vo = null == response || !response.has("retrieveArticleTabInfoVo") || !response.get("retrieveArticleTabInfoVo").isJsonArray()
						? null : response.getAsJsonArray("retrieveArticleTabInfoVo");
				if (null != vo && vo.size() > 0 && vo.get(0).isJsonObject()) {
					article = vo.get(0).getAsJsonObject();
				}
			} catch (RuntimeException e) {
				// not the expected answer - handled below
			}
			if (null == article) {
				return KaptureRecordResult.failure(ERR_NOT_FOUND, "Kapture did not return document " + documentId + " (" + locale
						+ ") in its unpublish answer: " + r.describe());
			}
			KaptureRecordResult out = new KaptureRecordResult();
			out.documentId = documentId;
			out.success = true;
			out.rowId = KaptureApiClient.str(article, "id");
			out.articleVersion = KaptureApiClient.str(article, "articleVersion");
			String state = KaptureApiClient.str(article, "articleState");
			out.articleState = null == state || "".equals(state.trim()) ? stateUnpublished : state;
			logger.info("unpublish :: " + documentId + " :: " + answer + " :: row " + out.rowId + " version " + out.articleVersion
					+ " state " + out.articleState);
			return out;
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "unpublish()", e);
			return KaptureRecordResult.failure(ERR_REQUEST_FAILED, e.getMessage());
		}
	}

	// ---- read --------------------------------------------------------------

	/**
	 * The attribute values the latest version of a document holds in Kapture - the object under
	 * the channel's name (TITLE, DJVU_FILE_LOCATION, ATTACHMENT ...).
	 *
	 * Needed where an update changes only SOME attributes: an update rewrites the whole
	 * document, so the attributes that are not changing have to be sent back as they are.
	 *
	 * @return null when the document could not be read - the caller must then fail the
	 *         document, never send it with the other attributes blank
	 */
	public JsonObject readFields(String documentId, String channelType, String locale) {
		try {
			String contentId = contentIdFor(channelType);
			if (null == contentId) {
				return null;
			}
			KaptureApiResult r = client.latestArticle(documentId, contentId, locale);
			if (!r.isOk() || null == r.body || "".equals(r.body.trim())) {
				logger.info("readFields :: could not read " + documentId + " (" + locale + ") :: " + r.describe());
				return null;
			}
			JsonObject node = findObject(new JsonParser().parse(r.body), channelType, 0);
			if (null == node) {
				logger.info("readFields :: the answer for " + documentId + " carries no " + channelType + " object.");
			}
			return node;
		} catch (RuntimeException e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "readFields()", e);
			return null;
		}
	}

	/** First object member called name, searched breadth-limited from the root of the answer. */
	private static JsonObject findObject(JsonElement e, String name, int depth) {
		if (null == e || depth > 4) {
			return null;
		}
		if (e.isJsonObject()) {
			JsonObject o = e.getAsJsonObject();
			if (o.has(name) && o.get(name).isJsonObject()) {
				return o.getAsJsonObject(name);
			}
			for (Map.Entry<String, JsonElement> m : o.entrySet()) {
				JsonObject found = findObject(m.getValue(), name, depth + 1);
				if (null != found) {
					return found;
				}
			}
		} else if (e.isJsonArray()) {
			JsonArray a = e.getAsJsonArray();
			for (int i = 0; i < a.size(); i++) {
				JsonObject found = findObject(a.get(i), name, depth + 1);
				if (null != found) {
					return found;
				}
			}
		}
		return null;
	}

	/** true when the state Kapture reported means the document is published. */
	public boolean isPublishedState(String articleState) {
		return null != articleState && statePublished.equalsIgnoreCase(articleState.trim());
	}

	// ---- payload -----------------------------------------------------------

	private String lastAttachmentError = null;

	/**
	 * @param identity null for a create, the document's current state for an update
	 * @return null when an attachment could not be uploaded (reason in lastAttachmentError)
	 */
	private JsonObject buildPayload(KaptureArticle a, KaptureLookupDAO.ArticleIdentity identity) {
		lastAttachmentError = null;
		JsonObject o = new JsonObject();
		boolean update = null != identity;
		if (update) {
			o.addProperty("legacyDocumentId", a.documentId);
		}
		o.addProperty("sourceIdentifier", nz(a.sourceIdentifier));
		o.addProperty("sourceFileName", nz(a.sourceFileName));
		o.addProperty("primaryLocale", a.locale);
		o.addProperty("translatedLocale", a.locale);
		o.addProperty("title", nz(a.title));
		// on a create Kapture accepts only "Unpublished"; on an update it must be the CURRENT state
		o.addProperty("articleState", update ? identity.articleState : stateUnpublished);
		o.addProperty("articleOwner", client.getUserDisplayName());
		o.addProperty("articleCreator", client.getUserDisplayName());
		o.addProperty("articleCreatorUserID", client.getUserPkId());
		o.addProperty("createdBy", client.getUserDisplayName());
		o.addProperty("modifiedBy", client.getUserDisplayName());
		o.addProperty("type", a.channelType);
		o.addProperty("ISARTICLE", "KAPTURE");
		o.addProperty("crawlType", "KAPTURE");
		// NEVER PUBLISHED BY THE CONVERSION
		o.addProperty("pubFlag", false);

		JsonArray cats = new JsonArray();
		JsonArray esiCats = new JsonArray();
		StringBuilder catCsv = new StringBuilder();
		StringBuilder esiCsv = new StringBuilder();
		for (KaptureArticle.Category c : a.categories) {
			JsonObject jc = new JsonObject();
			jc.addProperty("categoryRefKey", c.refKey);
			jc.addProperty("categoryName", null == c.name || "".equals(c.name) ? c.refKey : c.name);
			jc.addProperty("selected", true);
			if (c.esi) {
				esiCats.add(jc);
				esiCsv.append(esiCsv.length() > 0 ? "," : "").append(c.refKey);
			} else {
				cats.add(jc);
				catCsv.append(catCsv.length() > 0 ? "," : "").append(c.refKey);
			}
		}
		o.add("articlelistcategory", cats);
		o.add("articleesicategory", esiCats);
		o.addProperty("categoriesTreeRefKeys", catCsv.toString());
		o.addProperty("categoriesESITreeRefKeys", esiCsv.toString());

		JsonArray ugs = new JsonArray();
		for (String[] ug : userGroups) {
			JsonObject j = new JsonObject();
			j.addProperty("userGroupRefKey", ug[0]);
			j.addProperty("userGroupName", ug[1]);
			ugs.add(j);
		}
		o.add("articlelistusergroup", ugs);
		JsonArray vws = new JsonArray();
		for (String[] v : views) {
			JsonObject j = new JsonObject();
			j.addProperty("viewRefKey", v[0]);
			j.addProperty("viewName", v[1]);
			vws.add(j);
		}
		o.add("articlelistview", vws);
		o.add("facetsSchemaFields", new JsonArray());
		o.add("fieldSecurities", new JsonArray());
		o.add("articleTagList", new JsonArray());

		if (update) {
			// id and articleVersion are deliberately NOT sent: Kapture resolves the latest
			// version from articleId + locale. Sending either switches it to id targeting.
			o.addProperty("articleId", a.documentId);
			o.addProperty("publishedVersion", nz(identity.publishedVersion));
			o.addProperty("isUpdateArticle", true);
		}

		JsonObject node = buildNode(a.fields, a);
		if (null == node) {
			return null;
		}
		if (!node.has("ATTACHMENTS")) {
			node.add("ATTACHMENTS", new JsonArray());
		}
		o.add(a.channelType, node);
		return o;
	}

	@SuppressWarnings("unchecked")
	private JsonObject buildNode(Map<String, Object> fields, KaptureArticle a) {
		JsonObject node = new JsonObject();
		for (Map.Entry<String, Object> e : fields.entrySet()) {
			Object v = e.getValue();
			if (v instanceof KaptureArticle.Attachment) {
				if (skipAttachments) {
					// a new document: the file is added after the create (attachAfterCreate). The
					// attribute is sent as an empty list, the same shape as a document with no file.
					node.add(e.getKey(), new JsonArray());
					continue;
				}
				KaptureArticle.Attachment at = (KaptureArticle.Attachment) v;
				JsonObject att;
				if (preUploaded.containsKey(at)) {
					// uploaded with the other files of the batch
					att = preUploaded.get(at);
				} else if (preUploadErrors.containsKey(at)) {
					lastAttachmentError = preUploadErrors.get(at);
					return null;
				} else {
					att = uploadAttachment(at, a);
				}
				if (null == att) {
					return null;
				}
				JsonArray one = new JsonArray();
				one.add(att);
				node.add(e.getKey(), one);
			} else if (v instanceof JsonElement) {
				// a value read back from Kapture and sent on unchanged
				node.add(e.getKey(), (JsonElement) v);
			} else if (v instanceof Map) {
				JsonObject child = buildNode((Map<String, Object>) v, a);
				if (null == child) {
					return null;
				}
				node.add(e.getKey(), child);
			} else {
				node.addProperty(e.getKey(), sanitizer.sanitize(null == v ? "" : v.toString()));
			}
		}
		return node;
	}

	/**
	 * Upload one file and return the object that takes the file attribute's place in the
	 * payload. The upload result is checked here: an article save reports SUCCESS even when
	 * its attachment never arrived.
	 */
	private JsonObject uploadAttachment(KaptureArticle.Attachment att, KaptureArticle a) {
		if (null == att.file || !att.file.isFile()) {
			lastAttachmentError = "Attachment file not found: " + (null == att.file ? "null" : att.file.getPath());
			return null;
		}
		String clientId = "CAI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		KaptureApiResult r = client.uploadTempAttachment(att.file, clientId, uploadDocumentId(a, clientId), a.locale);
		if (!r.isHttpOk()) {
			lastAttachmentError = "Attachment upload failed for " + att.file.getName() + ": " + r.describe();
			return null;
		}
		try {
			JsonElement root = new JsonParser().parse(r.body);
			JsonArray arr = root.isJsonArray() ? root.getAsJsonArray() : null;
			if (null != arr) {
				for (int i = 0; i < arr.size(); i++) {
					JsonObject e = arr.get(i).getAsJsonObject();
					// correlate by OUR id, never by position
					if (!clientId.equals(KaptureApiClient.str(e, "clientAttachmentId"))) {
						continue;
					}
					JsonObject out = attachmentObject(e, att, clientId);
					if (null == out) {
						break;
					}
					return out;
				}
			}
		} catch (Exception ex) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "uploadAttachment()", ex);
		}
		lastAttachmentError = "Attachment upload for " + att.file.getName()
				+ " returned no temporary attachment id";
		return null;
	}

	/** The object that takes a file attribute's place in the payload; null without a temp id. */
	private static JsonObject attachmentObject(JsonObject e, KaptureArticle.Attachment att, String clientId) {
		String tempId = KaptureApiClient.str(e, "tempAttachmentId");
		if (null == tempId || "".equals(tempId)) {
			return null;
		}
		JsonObject out = new JsonObject();
		out.addProperty("tempAttachmentId", tempId);
		out.addProperty("clientAttachmentId", clientId);
		out.addProperty("attachmentAction", "NEW");
		out.addProperty("fileName", nz(KaptureApiClient.str(e, "fileName")));
		out.addProperty("attachmentTitle", null == att.title || "".equals(att.title) ? att.file.getName() : att.title);
		out.addProperty("fileExtension", nz(KaptureApiClient.str(e, "fileExtension")));
		out.addProperty("fileType", nz(KaptureApiClient.str(e, "fileType")));
		if (e.has("size") && e.get("size").isJsonPrimitive()) {
			out.add("size", e.get("size"));
		}
		return out;
	}

	// ---- response ----------------------------------------------------------

	private KaptureRecordResult readSingleResult(KaptureApiResult r, KaptureArticle a) {
		if (r.timedOut) {
			return KaptureRecordResult.failure(ERR_REQUEST_FAILED, "Kapture did not answer in time. The document"
					+ " may or may not have been written; it was not sent again. Check it in Kapture.");
		}
		if (!r.isHttpOk()) {
			return KaptureRecordResult.failure(ERR_REQUEST_FAILED, "Kapture request failed: " + r.describe());
		}
		try {
			JsonObject root = new JsonParser().parse(r.body).getAsJsonObject();
			JsonArray arr = resultsArray(root);
			if (null == arr || arr.size() == 0) {
				// no record for a document that was sent = that document failed
				return KaptureRecordResult.failure(ERR_NO_RESULT, "Kapture returned no result for the document. "
						+ nz(KaptureApiClient.str(root, "message")));
			}
			KaptureRecordResult out = parseRecord(arr.get(0).getAsJsonObject());
			logResult(a, out, r.requestBody);
			return out;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(KaptureContentService.class.getName(), "readSingleResult()", e);
			return KaptureRecordResult.failure(ERR_REQUEST_FAILED,
					"Kapture answer could not be read: " + e.getMessage());
		}
	}

	/** results[] of a bulk answer - a plain array on the POST, a page (content[]) on GET /jobs. */
	private static JsonArray resultsArray(JsonObject root) {
		JsonElement results = root.get("results");
		if (null != results && results.isJsonArray()) {
			return results.getAsJsonArray();
		}
		if (null != results && results.isJsonObject() && results.getAsJsonObject().has("content")) {
			return results.getAsJsonObject().getAsJsonArray("content");
		}
		return null;
	}

	/** One document's record of a bulk answer. A 2xx answer is not success - this record decides. */
	private static KaptureRecordResult parseRecord(JsonObject rec) {
		KaptureRecordResult out = new KaptureRecordResult();
		String status = KaptureApiClient.str(rec, "status");
		out.success = "SUCCESS".equalsIgnoreCase(status);
		out.documentId = KaptureApiClient.str(rec, "kaptureDocumentId");
		out.rowId = KaptureApiClient.str(rec, "id");
		out.articleVersion = KaptureApiClient.str(rec, "articleVersion");
		out.publishedVersion = KaptureApiClient.str(rec, "publishedVersion");
		out.articleState = KaptureApiClient.str(rec, "articleState");
		out.errorCode = out.success ? null : nz(KaptureApiClient.str(rec, "errorCode"));
		StringBuilder msg = new StringBuilder(nz(KaptureApiClient.str(rec, "message")));
		if (rec.has("validationErrors") && rec.get("validationErrors").isJsonArray()) {
			JsonArray ve = rec.getAsJsonArray("validationErrors");
			for (int i = 0; i < ve.size(); i++) {
				JsonObject v = ve.get(i).getAsJsonObject();
				msg.append(" [").append(nz(KaptureApiClient.str(v, "field"))).append(": ")
						.append(nz(KaptureApiClient.str(v, "message"))).append("]");
			}
		}
		out.message = msg.toString();
		if (!out.success && "".equals(out.errorCode)) {
			out.errorCode = null == status ? ERR_REQUEST_FAILED : status;
		}
		if (out.success && (null == out.documentId || "".equals(out.documentId.trim()))) {
			// SUCCESS without an id cannot be used: the id is the key for every later update
			out.success = false;
			out.errorCode = ERR_NO_RESULT;
			out.message = "Kapture reported success but returned no document id. " + out.message;
		}
		return out;
	}

	/** One line per document; a refused document also logs what was sent for it. */
	private static void logResult(KaptureArticle a, KaptureRecordResult out, String sent) {
		logger.info("result :: " + (null == a.documentId ? "(new)" : a.documentId) + " -> "
				+ (out.success ? "SUCCESS" : "FAILED") + " id=" + out.documentId + " version=" + out.articleVersion
				+ " state=" + out.articleState + " :: source=" + a.sourceIdentifier
				+ (out.success ? "" : " :: error=" + out.errorCode + " " + out.message));
		if (!out.success && null != sent) {
			// THE DOCUMENT WAS REFUSED - log what was sent for it
			logger.info("result :: REFUSED DOCUMENT :: source=" + a.sourceIdentifier + " :: SENT="
					+ KaptureApiClient.cut(sent));
		}
	}

	// ---- helpers -----------------------------------------------------------

	private String contentIdFor(String channelType) {
		if (!contentIds.containsKey(channelType)) {
			String prefix = KaptureApiClient.require("kapture.channel.prefix." + channelType);
			contentIds.put(channelType, KaptureLookupDAO.getChannelContentId(prefix));
		}
		return contentIds.get(channelType);
	}

	private String correlationId(String op) {
		requestCounter++;
		return "dmt-" + Thread.currentThread().getName() + "-" + runStamp + "-" + op + requestCounter;
	}

	private static List<String> csv(String s) {
		List<String> out = new ArrayList<String>();
		if (null != s) {
			for (String p : s.split(",")) {
				if (!"".equals(p.trim())) {
					out.add(p.trim());
				}
			}
		}
		return out;
	}

	private static String nz(String s) {
		return null == s ? "" : s;
	}
}
