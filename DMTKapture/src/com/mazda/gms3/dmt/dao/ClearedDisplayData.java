package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.vo.ContentDetails;

/**
 * VIEW CONTENT / CD DATA THE DOCUMENT STEP CLEARED - per job, in memory.
 *
 * A document converted again loses all its old details first (cleanUpOldDataForDocument / the batch
 * DAOs' updateDocuments), its view content and CD rows included; the display order / CD step then
 * re-creates them only for the documents its files list. So that UnlistedDisplayData can report a
 * document that HAD rows and is no longer listed (a DELETE row of the report) - the document step
 * records here, just before it deletes, which documents actually had rows (one SELECT per chunk).
 */
public class ClearedDisplayData {

	public static final String VIEW_CONTENT = "VC";
	public static final String CD_DATA = "CD";

	/** A document whose rows were cleared: its source path and title (for the report). */
	public static class Cleared {
		public final String path;
		public final String title;

		Cleared(String path, String title) {
			this.path = path;
			this.title = title;
		}
	}

	/** schedule id + "|" + kind -> document id -> cleared */
	private static final Map<String, Map<String, Cleared>> CLEARED = new ConcurrentHashMap<String, Map<String, Cleared>>();

	/**
	 * Records which of the documents have rows in the table - call it on the same connection right
	 * before the DELETE.
	 *
	 * @param localeColumn the locale column (value: locale with "_"), null when the table has none to filter on
	 */
	public static void recordExisting(Connection conn, String kind, String table, String localeColumn, String documentColumn,
			List<ContentDetails> docs) throws Exception {
		if (null == docs || docs.isEmpty()) {
			return;
		}
		// per job and locale (a batch is one job; the locale is the same for all in practice)
		Map<String, List<ContentDetails>> groups = new LinkedHashMap<String, List<ContentDetails>>();
		for (ContentDetails cd : docs) {
			if (null == cd.getImDocumentId() || "".equals(cd.getImDocumentId().trim()) || null == cd.getScheduleId()) {
				continue;
			}
			String key = cd.getScheduleId().trim() + "\u0000" + (null == cd.getLocale() ? "" : cd.getLocale().trim().replace("-", "_"));
			List<ContentDetails> l = groups.get(key);
			if (null == l) {
				l = new ArrayList<ContentDetails>();
				groups.put(key, l);
			}
			l.add(cd);
		}
		int chunk = Integer.parseInt(KaptureApiClient.require("db.lookup.batch.size"));
		for (Map.Entry<String, List<ContentDetails>> g : groups.entrySet()) {
			String scheduleId = g.getKey().substring(0, g.getKey().indexOf('\u0000'));
			String locale = g.getKey().substring(g.getKey().indexOf('\u0000') + 1);
			Map<String, ContentDetails> byId = new LinkedHashMap<String, ContentDetails>();
			for (ContentDetails cd : g.getValue()) {
				byId.put(cd.getImDocumentId().trim(), cd);
			}
			List<String> ids = new ArrayList<String>(byId.keySet());
			Map<String, Cleared> target = target(scheduleId, kind);
			for (int i = 0; i < ids.size(); i += chunk) {
				List<String> part = ids.subList(i, Math.min(i + chunk, ids.size()));
				StringBuilder marks = new StringBuilder();
				for (int m = 0; m < part.size(); m++) {
					marks.append(m == 0 ? "?" : ",?");
				}
				PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT " + documentColumn + " FROM " + table + " WHERE "
						+ (null == localeColumn ? "" : localeColumn + " = ? AND ") + documentColumn + " IN (" + marks + ")");
				try {
					int p = 1;
					if (null != localeColumn) {
						ps.setString(p++, locale);
					}
					for (String id : part) {
						ps.setString(p++, id);
					}
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						String id = null == rs.getString(1) ? null : rs.getString(1).trim();
						ContentDetails cd = null == id ? null : byId.get(id);
						if (null != cd) {
							target.put(id, new Cleared(cd.getFilePath(), cd.getTitle()));
						}
					}
					rs.close();
				} finally {
					ps.close();
				}
			}
		}
	}

	/** The single-document form (cleanUpOldDataForDocument). */
	public static void recordExisting(Connection conn, String kind, String table, String localeColumn, String documentColumn,
			ContentDetails cd, String documentId) throws Exception {
		if (null == cd || null == documentId) {
			return;
		}
		String saved = cd.getImDocumentId();
		try {
			cd.setImDocumentId(documentId);
			recordExisting(conn, kind, table, localeColumn, documentColumn, Collections.singletonList(cd));
		} finally {
			cd.setImDocumentId(saved);
		}
	}

	/** The documents of the job whose rows of the kind the document step cleared (document id -> cleared). */
	public static Map<String, Cleared> get(String scheduleId, String kind) {
		Map<String, Cleared> m = CLEARED.get(scheduleId.trim() + "|" + kind);
		return null == m ? new HashMap<String, Cleared>() : new HashMap<String, Cleared>(m);
	}

	/** Forgets the job (its start and its end). */
	public static void discard(String scheduleId) {
		if (null == scheduleId) {
			return;
		}
		CLEARED.remove(scheduleId.trim() + "|" + VIEW_CONTENT);
		CLEARED.remove(scheduleId.trim() + "|" + CD_DATA);
	}

	private static Map<String, Cleared> target(String scheduleId, String kind) {
		String key = scheduleId + "|" + kind;
		Map<String, Cleared> m = CLEARED.get(key);
		if (null == m) {
			CLEARED.putIfAbsent(key, new ConcurrentHashMap<String, Cleared>());
			m = CLEARED.get(key);
		}
		return m;
	}
}
