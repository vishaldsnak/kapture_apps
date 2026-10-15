package com.mazda.gms3.dmt.conversion.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.dao.ClearedDisplayData;
import com.mazda.gms3.dmt.dao.UnlistedDisplayDataDAO;
import com.mazda.gms3.dmt.dao.UnlistedDisplayDataDAO.FolderDocument;
import com.mazda.gms3.dmt.dao.UnlistedDisplayDataDAO.ViewContentTable;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.ConversionUtils;
import com.mazda.gms3.dmt.mc.utils.PrintReportsUtil;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;

/**
 * VIEW CONTENT AND CD DATA OF DOCUMENTS NO LONGER LISTED (MC, MME, MNAO).
 *
 * The display order step deletes and re-inserts the view content of the documents its files list, the
 * CD step the CD data of the documents its files list. The other ACTIVE documents of the item's vin.txt
 * material folders lose their view content / CD rows here - also when the files list nothing for a
 * folder. Only those rows go: the DMT document and the Kapture article stay. Each document whose rows
 * were deleted becomes a DELETE row (OPERATION column) of the display order / CD data report.
 *
 * A document this job converted again lost its rows already in the document step (ClearedDisplayData
 * records which ones had rows): when no longer listed it is a DELETE row too - nothing left to delete.
 */
public class UnlistedDisplayData {

	private static Logger logger = LogManager.getLogger(UnlistedDisplayData.class);

	/**
	 * The material folders of the item's vin.txt lines, in the form DC_SOURCE_NETWORK_LOC starts with:
	 * locale\model folder\manual type\facelift\material\ - one entry per material folder.
	 */
	public static List<String> vinMaterialFolders(ScheduleItemDetails itemDetails, Collection<VinDetails> vinList) {
		Map<String, String> folders = new LinkedHashMap<String, String>();
		if (null != vinList) {
			for (VinDetails v : vinList) {
				if (null == v.getLineType() || !v.getLineType().equals(ConversionUtils.LINE_TYPE_VALID) || null == v.getMaterialFolder()
						|| "".equals(v.getMaterialFolder().trim())) {
					continue;
				}
				String folder = itemDetails.getLocale().trim() + "\\" + itemDetails.getModelFolderName().trim() + "\\" + itemDetails.getManualType().trim()
						+ "\\" + itemDetails.getFaceLiftFolderName().trim() + "\\" + v.getMaterialFolder().trim() + "\\";
				folders.put(folder.toLowerCase(), folder);
			}
		}
		return new ArrayList<String>(folders.values());
	}

	/**
	 * Removes the view content of the documents of the folders that the display order files do not list.
	 *
	 * @param documentTable the market's DMT document table of the item
	 * @param statusColumn  its ACTIVE flag column (DC_DOC_STATUS; MNAO DC_IM_DOC_FLAG)
	 * @param viewContent   the market's view content table(s)
	 * @param listed        the documents built from this run's display order rows
	 * @return the DELETE rows for the display order report
	 */
	public static List<DisplayOrderDetails> removeViewContent(ScheduleItemDetails itemDetails, String documentTable, String statusColumn, List<ViewContentTable> viewContent,
			List<String> folders, Collection<ContentDetails> listed) {
		List<DisplayOrderDetails> report = new ArrayList<DisplayOrderDetails>();
		if (null == folders || folders.isEmpty()) {
			logger.info("removeViewContent :: item " + itemDetails.getItemId() + " :: no material folder in vin.txt :: nothing checked");
			return report;
		}
		Set<String> listedPaths = new HashSet<String>();
		Set<String> listedIds = new HashSet<String>();
		if (null != listed) {
			for (ContentDetails c : listed) {
				addKey(listedPaths, c.getFilePath(), true);
				addKey(listedIds, c.getDocumentId(), false);
			}
		}
		for (String folder : folders) {
			long started = System.currentTimeMillis();
			try {
				List<FolderDocument> docs = UnlistedDisplayDataDAO.documentsUnder(documentTable, statusColumn, folder);
				Map<String, FolderDocument> unlisted = new LinkedHashMap<String, FolderDocument>();
				for (FolderDocument d : docs) {
					if (!listed(d, listedPaths, listedIds)) {
						unlisted.put(d.id, d);
					}
				}
				Set<String> withRows = UnlistedDisplayDataDAO.withViewContent(viewContent, itemDetails.getLocale(), unlisted.keySet());
				// converted again by this job: the document step already cleared their view content (ClearedDisplayData)
				// and the display order step did not re-create it - removed as well, nothing left to delete
				Map<String, ClearedDisplayData.Cleared> cleared = ClearedDisplayData.get(String.valueOf(itemDetails.getScheduleId()), ClearedDisplayData.VIEW_CONTENT);
				List<String> clearedEarlier = new ArrayList<String>();
				for (String id : unlisted.keySet()) {
					if (!withRows.contains(id) && cleared.containsKey(id)) {
						clearedEarlier.add(id);
					}
				}
				if (withRows.isEmpty() && clearedEarlier.isEmpty()) {
					logger.info("removeViewContent :: " + folder + " :: " + docs.size() + " document(s), " + unlisted.size()
							+ " not in the display order, none with view content :: " + (System.currentTimeMillis() - started) + " ms");
					continue;
				}
				String status = "SUCCESS";
				String code = null;
				String message = "VIEW CONTENT DELETED - DOCUMENT IS NOT IN THE DISPLAY ORDER FILE.";
				try {
					int rows = UnlistedDisplayDataDAO.deleteViewContent(viewContent, itemDetails.getLocale(), withRows);
					logger.info("removeViewContent :: " + folder + " :: " + docs.size() + " document(s), " + unlisted.size() + " not in the display order, "
							+ withRows.size() + " had view content :: " + rows + " row(s) deleted :: " + withRows + " :: " + clearedEarlier.size()
							+ " cleared by the document step and not re-created :: " + clearedEarlier + " :: " + (System.currentTimeMillis() - started) + " ms");
				} catch (Exception e) {
					Utilities.printStackTraceToLogs(UnlistedDisplayData.class.getName(), "removeViewContent()", e);
					status = "FAILURE";
					code = "DSP020";
					message = "FAILED TO DELETE THE VIEW CONTENT OF THE DOCUMENT THAT IS NOT IN THE DISPLAY ORDER FILE :: " + e.getMessage();
				}
				for (String id : withRows) {
					FolderDocument d = unlisted.get(id);
					DisplayOrderDetails row = displayOrderRow(itemDetails, status, code, message);
					row.setFilePath(null == d ? null : d.path);
					row.setDocumentId(id);
					row.setTitle(null == d ? null : d.title);
					report.add(row);
				}
				for (String id : clearedEarlier) {
					FolderDocument d = unlisted.get(id);
					DisplayOrderDetails row = displayOrderRow(itemDetails, "SUCCESS", null, "VIEW CONTENT DELETED - DOCUMENT IS NOT IN THE DISPLAY ORDER FILE.");
					row.setFilePath(null == d ? null : d.path);
					row.setDocumentId(id);
					row.setTitle(null == d ? null : d.title);
					report.add(row);
				}
			} catch (Exception e) {
				// the documents of the folder could not be read - nothing was deleted
				Utilities.printStackTraceToLogs(UnlistedDisplayData.class.getName(), "removeViewContent()", e);
				DisplayOrderDetails row = displayOrderRow(itemDetails, "FAILURE", "DSP021",
						"FAILED TO CHECK THE VIEW CONTENT OF THE DOCUMENTS NOT IN THE DISPLAY ORDER FILE :: " + e.getMessage());
				row.setFilePath(folder);
				report.add(row);
			}
		}
		return report;
	}

	/**
	 * Removes the CD data of the documents of the folders that the CD files do not list.
	 *
	 * @param cdTable the market's CD data table of the item
	 * @param listed  this run's CD rows of the item
	 * @return the DELETE rows for the CD data report
	 */
	public static List<CDProcessingDetails> removeCDData(ScheduleItemDetails itemDetails, String cdTable, List<String> folders,
			Collection<CDProcessingDetails> listed) {
		List<CDProcessingDetails> report = new ArrayList<CDProcessingDetails>();
		if (null == folders || folders.isEmpty()) {
			logger.info("removeCDData :: item " + itemDetails.getItemId() + " :: no material folder in vin.txt :: nothing checked");
			return report;
		}
		Set<String> listedPaths = new HashSet<String>();
		Set<String> listedIds = new HashSet<String>();
		if (null != listed) {
			for (CDProcessingDetails c : listed) {
				addKey(listedPaths, c.getFilePath(), true);
				addKey(listedIds, c.getDocumentId(), false);
			}
		}
		for (String folder : folders) {
			long started = System.currentTimeMillis();
			try {
				List<FolderDocument> docs = UnlistedDisplayDataDAO.cdDocumentsUnder(cdTable, folder);
				List<FolderDocument> unlisted = new ArrayList<FolderDocument>();
				List<String> ids = new ArrayList<String>();
				Set<String> withRows = new HashSet<String>();
				for (FolderDocument d : docs) {
					withRows.add(d.id);
					if (!listed(d, listedPaths, listedIds)) {
						unlisted.add(d);
						ids.add(d.id);
					}
				}
				// converted again by this job: the document step already cleared their CD data (ClearedDisplayData)
				// and the CD step did not re-create it - removed as well, nothing left to delete
				List<FolderDocument> clearedEarlier = new ArrayList<FolderDocument>();
				String prefix = folder.trim().toLowerCase();
				for (Map.Entry<String, ClearedDisplayData.Cleared> e : ClearedDisplayData.get(String.valueOf(itemDetails.getScheduleId()), ClearedDisplayData.CD_DATA).entrySet()) {
					FolderDocument d = new FolderDocument();
					d.id = e.getKey();
					d.path = e.getValue().path;
					d.title = e.getValue().title;
					if (!withRows.contains(d.id) && null != d.path && d.path.trim().toLowerCase().startsWith(prefix) && !listed(d, listedPaths, listedIds)) {
						clearedEarlier.add(d);
					}
				}
				if (unlisted.isEmpty() && clearedEarlier.isEmpty()) {
					logger.info("removeCDData :: " + folder + " :: " + docs.size() + " document(s) with CD data, all in the CD files :: "
							+ (System.currentTimeMillis() - started) + " ms");
					continue;
				}
				String status = "SUCCESS";
				String code = null;
				String message = "CD DATA DELETED - DOCUMENT IS NOT IN THE CD FILE.";
				try {
					int rows = UnlistedDisplayDataDAO.deleteCDData(cdTable, folder, ids);
					List<String> clearedIds = new ArrayList<String>();
					for (FolderDocument d : clearedEarlier) {
						clearedIds.add(d.id);
					}
					logger.info("removeCDData :: " + folder + " :: " + docs.size() + " document(s) with CD data, " + unlisted.size()
							+ " not in the CD files :: " + rows + " row(s) deleted :: " + ids + " :: " + clearedEarlier.size()
							+ " cleared by the document step and not re-created :: " + clearedIds + " :: " + (System.currentTimeMillis() - started) + " ms");
				} catch (Exception e) {
					Utilities.printStackTraceToLogs(UnlistedDisplayData.class.getName(), "removeCDData()", e);
					status = "FAILURE";
					code = "CDP020";
					message = "FAILED TO DELETE THE CD DATA OF THE DOCUMENT THAT IS NOT IN THE CD FILE :: " + e.getMessage();
				}
				for (FolderDocument d : unlisted) {
					CDProcessingDetails row = cdRow(itemDetails, status, code, message);
					row.setFilePath(d.path);
					row.setDocumentId(d.id);
					row.setTitle(d.title);
					row.setCdProcessingSourceFileName(d.cdFileName);
					report.add(row);
				}
				for (FolderDocument d : clearedEarlier) {
					CDProcessingDetails row = cdRow(itemDetails, "SUCCESS", null, "CD DATA DELETED - DOCUMENT IS NOT IN THE CD FILE.");
					row.setFilePath(d.path);
					row.setDocumentId(d.id);
					row.setTitle(d.title);
					report.add(row);
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(UnlistedDisplayData.class.getName(), "removeCDData()", e);
				CDProcessingDetails row = cdRow(itemDetails, "FAILURE", "CDP021",
						"FAILED TO CHECK THE CD DATA OF THE DOCUMENTS NOT IN THE CD FILE :: " + e.getMessage());
				row.setFilePath(folder);
				report.add(row);
			}
		}
		return report;
	}

	/** listed = its document id or its source path is among this run's rows */
	private static boolean listed(FolderDocument d, Set<String> listedPaths, Set<String> listedIds) {
		return listedIds.contains(d.id) || (null != d.path && listedPaths.contains(d.path.trim().toLowerCase()));
	}

	private static void addKey(Set<String> keys, String value, boolean lower) {
		if (null != value && !"".equals(value.trim())) {
			keys.add(lower ? value.trim().toLowerCase() : value.trim());
		}
	}

	private static DisplayOrderDetails displayOrderRow(ScheduleItemDetails itemDetails, String status, String code, String message) {
		DisplayOrderDetails row = new DisplayOrderDetails();
		row.setOperation(PrintReportsUtil.OPERATION_DELETE);
		row.setProcessingStatus(status);
		row.setErrorCode(code);
		row.setErrorMessage(message);
		row.setLocale(itemDetails.getLocale());
		row.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
		row.setItemId(String.valueOf(itemDetails.getItemId()));
		row.setModelType(itemDetails.getModelType());
		return row;
	}

	private static CDProcessingDetails cdRow(ScheduleItemDetails itemDetails, String status, String code, String message) {
		CDProcessingDetails row = new CDProcessingDetails();
		row.setOperation(PrintReportsUtil.OPERATION_DELETE);
		row.setProcessingStatus(status);
		row.setErrorCode(code);
		row.setErrorMessage(message);
		row.setLocale(itemDetails.getLocale());
		row.setScheduleId(String.valueOf(itemDetails.getScheduleId()));
		row.setItemId(String.valueOf(itemDetails.getItemId()));
		row.setModelType(itemDetails.getModelType());
		return row;
	}
}
