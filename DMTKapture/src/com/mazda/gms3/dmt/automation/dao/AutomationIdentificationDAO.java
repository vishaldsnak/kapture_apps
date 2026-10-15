package com.mazda.gms3.dmt.automation.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;

/**
 * THE DOCUMENTS IDENTIFICATION JOB (job A of the IPM Facelift VIN Update) - gms3_dmt_fm_sch (job), gms3_dmt_fm_sch_itm
 * (one row per locale / model / channel / document type) and gms3_dmt_fm_itm_ap_vin (APP_VIN rows: the VIN ranges of an
 * item; IMP_DOC rows: one per impacted document and VIN range).
 *
 * 2026-10-11 (Kapture): every statement is prepared; an item row is written with ONE insert (was an insert plus a
 * batched UPDATE, which the MC Dev MySQL Router refuses as a multi-statement); a failure rolls the whole result back;
 * the items are read back with one query for all VIN rows of the job, and a document appears ONCE per item (the legacy
 * read compared a lower-cased id with the stored one and made a document map per VIN row of an upper-case id).
 */
public class AutomationIdentificationDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(AutomationIdentificationDAO.class);

	final static String REC_TYPE_APPLICABLE_VIN="APP_VIN";
	final static String REC_TYPE_IMPACTED_DOCUMENTS="IMP_DOC";

	/** Rows per INSERT batch of gms3_dmt_fm_itm_ap_vin (INSERT batches are accepted by the Router). */
	private static final int VIN_INSERT_BATCH = 100;

	/*
	 * CALL FUNCTION TO UPDATE SCHEDULE STATUS - and, when asked, remove the identified items of the job
	 */
	public static boolean updateScheduleStatus(String scheduleId, String processingStatus,String jobStatus, boolean deleteChildEntries, String remarks)
	{
		if(null==scheduleId || "".equals(scheduleId) || "0".equals(scheduleId) || null==processingStatus || "".equals(processingStatus))
		{
			logger.info("updateScheduleStatus :: Schedule Id / Processing Status as parameters are null. Return false.");
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			long id = Long.parseLong(scheduleId.trim());
			boolean withJobStatus = null!=jobStatus && !"".equals(jobStatus);
			conn = getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement("UPDATE gms3_dmt_fm_sch SET fm_processing_status=?, fm_remarks=?"
					+(withJobStatus ? ", fm_job_status=?, fm_finish_time=?" : "")+" WHERE fm_sch_id=?");
			int p = 1;
			pstmt.setString(p++, processingStatus);
			pstmt.setBytes(p++, (null==remarks ? "" : remarks).getBytes());
			if(withJobStatus)
			{
				pstmt.setString(p++, jobStatus);
				pstmt.setTimestamp(p++, new Timestamp(new Date().getTime()));
			}
			pstmt.setLong(p, id);
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;

			if(deleteChildEntries)
			{
				pstmt = conn.prepareStatement("DELETE FROM gms3_dmt_fm_sch_itm WHERE fm_sch_id=?");
				pstmt.setLong(1, id);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;

				pstmt = conn.prepareStatement("DELETE FROM gms3_dmt_fm_itm_ap_vin WHERE fm_sch_id=?");
				pstmt.setLong(1, id);
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
			}
			conn.commit();
			logger.info("updateScheduleStatus :: Schedule {"+scheduleId+"} :: "+processingStatus+(withJobStatus ? " / "+jobStatus : ""));
			return true;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "updateScheduleStatus()", e);
			rollback(conn, "updateScheduleStatus()");
			return false;
		}
		finally
		{
			close(null, pstmt, null, conn, "updateScheduleStatus()");
		}
	}

	/**
	 * Saves the result of the identification job: the job's status and document count, one row per item and its VIN rows
	 * (APP_VIN) and impacted document rows (IMP_DOC). All or nothing.
	 */
	@SuppressWarnings("unchecked")
	public static boolean updateScheduleItemDetails(String scheduleId, int totalDocsCount,List<ItemDetails> itemsList, String scheduleStatus,String jobStatus)
	{
		if(null==scheduleId || "".equals(scheduleId) || "0".equals(scheduleId) || null==itemsList || itemsList.isEmpty())
		{
			logger.info("updateScheduleItemDetails :: Schedule Id / ItemsList as parameters are null. Return false.");
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			long schId = Long.parseLong(scheduleId.trim());
			conn = getConnection();
			conn.setAutoCommit(false);
			// THE JOB - STATUS & DOCS COUNT
			pstmt = conn.prepareStatement("UPDATE gms3_dmt_fm_sch SET fm_processing_status=?, fm_job_status=?, fm_finish_time=?, fm_total_docs_count=? WHERE fm_sch_id=?");
			pstmt.setString(1, scheduleStatus);
			pstmt.setString(2, jobStatus);
			pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
			if(totalDocsCount>0)
			{
				pstmt.setLong(4, totalDocsCount);
			}
			else
			{
				pstmt.setNull(4, Types.BIGINT);
			}
			pstmt.setLong(5, schId);
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;

			// THE ITEMS - one insert each (its id is needed for its VIN rows)
			pstmt = conn.prepareStatement("INSERT INTO gms3_dmt_fm_sch_itm (fm_sch_id, fm_locale, fm_carline_info, fm_ch_label, fm_ch_refkey,"
					+" fm_disp_carline_info, fm_dc_type_label, fm_dc_type_refkey, fm_docs_count, rec_creation_tmstp, rec_modified_tmstp, fm_app_vin_count)"
					+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
			List<ExcelRowDetails> vinRows = new ArrayList<ExcelRowDetails>();
			Timestamp now = new Timestamp(new Date().getTime());
			for(ItemDetails details : itemsList)
			{
				pstmt.setLong(1, schId);
				pstmt.setString(2, details.getLocale());
				pstmt.setString(3, details.getCarlineInfo());
				pstmt.setString(4, details.getChannelLabel());
				pstmt.setString(5, details.getChannelRefKey());
				pstmt.setString(6, details.getDisplayCarlineInfo());
				pstmt.setString(7, details.getDocumentTypeLabel());
				pstmt.setString(8, details.getDocumentTypeRefKey());
				if(null!=details.getTotalDocumentsCount() && details.getTotalDocumentsCount()>0)
				{
					pstmt.setLong(9, details.getTotalDocumentsCount().longValue());
				}
				else
				{
					pstmt.setNull(9, Types.BIGINT);
				}
				pstmt.setTimestamp(10, now);
				pstmt.setTimestamp(11, now);
				if(null!=details.getApplicableVINList() && details.getApplicableVINList().size()>0)
				{
					pstmt.setLong(12, details.getApplicableVINList().size());
				}
				else
				{
					pstmt.setNull(12, Types.BIGINT);
				}
				pstmt.executeUpdate();
				rs = pstmt.getGeneratedKeys();
				if(!rs.next())
				{
					throw new SQLException("No id returned for the item "+details.getLocale()+" / "+details.getCarlineInfo()+" / "+details.getChannelRefKey());
				}
				details.setItemId(rs.getLong(1));
				rs.close();rs=null;

				if(null!=details.getApplicableVINList())
				{
					for(ExcelRowDetails exDetails : details.getApplicableVINList())
					{
						vinRows.add(prepareAppVINData(exDetails, schId, details.getItemId(), REC_TYPE_APPLICABLE_VIN));
					}
				}
				if(null!=details.getImpactedDocumentsList())
				{
					for(Map<String, Object> docMap : details.getImpactedDocumentsList())
					{
						if(null==docMap.get(AutomationConstants.DOCUMENTID) || null==docMap.get(AutomationConstants.VIN_LIST))
						{
							continue;
						}
						for(ExcelRowDetails docVinDetails : (List<ExcelRowDetails>)docMap.get(AutomationConstants.VIN_LIST))
						{
							ExcelRowDetails row = prepareAppVINData(docVinDetails, schId, details.getItemId(), REC_TYPE_IMPACTED_DOCUMENTS);
							row.setDocumentId(docMap.get(AutomationConstants.DOCUMENTID).toString());
							row.setDocTypeRefKey(null==docMap.get(AutomationConstants.REFERENCEKEY) ? null : docMap.get(AutomationConstants.REFERENCEKEY).toString());
							vinRows.add(row);
						}
					}
				}
			}
			pstmt.close();pstmt=null;

			// THE VIN AND DOCUMENT ROWS - INSERT batches
			pstmt = conn.prepareStatement("INSERT INTO gms3_dmt_fm_itm_ap_vin (fm_itm_id, fm_sch_id, fm_n_carline_code, fm_n_wmi, fm_n_model_name,"
					+" fm_n_model_name_reg_mc, fm_n_model_name_reg_mnaomme, fm_n_vdscode, fm_n_visstart, fm_n_visend, fm_n_refkey, fm_o_carline_code,"
					+" fm_o_wmi, fm_o_model_name, fm_o_model_name_reg_mc, fm_o_model_name_reg_mnaomme, fm_o_vdscode, fm_o_visstart, fm_o_visend,"
					+" fm_o_refkey, fm_locale_exist_mdm, fm_locale_exist_im, fm_n_vin_exist_mdm, fm_n_vin_exist_im, fm_o_vin_exist_mdm, fm_o_vin_exist_im,"
					+" rec_creation_tmstp, rec_modified_tmstp, fm_locale, fm_record_type, fm_document_id, fm_doc_type_refkey)"
					+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			int inBatch = 0;
			for(ExcelRowDetails ch : vinRows)
			{
				pstmt.setLong(1, ch.getItemId());
				pstmt.setLong(2, ch.getScheduleId());
				pstmt.setString(3, ch.getNewCarlineCode());
				pstmt.setString(4, ch.getNewWmiCode());
				pstmt.setString(5, ch.getNewModelName());
				pstmt.setString(6, ch.getNewModelNameEngForMC());
				pstmt.setString(7, ch.getNewModelNameRegForMNAOMME());
				pstmt.setString(8, ch.getNewVdsCode());
				pstmt.setString(9, ch.getNewVisStartRange());
				pstmt.setString(10, ch.getNewVisEndRange());
				pstmt.setString(11, ch.getNewRefKey());
				pstmt.setString(12, ch.getOldCarlineCode());
				pstmt.setString(13, ch.getOldWmiCode());
				pstmt.setString(14, ch.getOldModelName());
				pstmt.setString(15, ch.getOldModelNameEngForMC());
				pstmt.setString(16, ch.getOldModelNameRegForMNAOMME());
				pstmt.setString(17, ch.getOldVdsCode());
				pstmt.setString(18, ch.getOldVisStartRange());
				pstmt.setString(19, ch.getOldVisEndRange());
				pstmt.setString(20, ch.getOldRefKey());
				pstmt.setString(21, yn(ch.isLocaleExistsInMDM()));
				// "IM" columns = Kapture since 2026-10-11
				pstmt.setString(22, yn(ch.isLocaleExistsInIM()));
				pstmt.setString(23, yn(ch.isNewVinExistsinMDM()));
				pstmt.setString(24, yn(ch.isNewVinExistsinIM()));
				pstmt.setString(25, yn(ch.isOldVinExistsinMDM()));
				pstmt.setString(26, yn(ch.isOldVinExistsinIM()));
				pstmt.setTimestamp(27, now);
				pstmt.setTimestamp(28, now);
				pstmt.setString(29, ch.getLocale());
				pstmt.setString(30, ch.getRecordType());
				setOrNull(pstmt, 31, ch.getDocumentId());
				setOrNull(pstmt, 32, ch.getDocTypeRefKey());
				pstmt.addBatch();
				if(++inBatch>=VIN_INSERT_BATCH)
				{
					pstmt.executeBatch();
					inBatch = 0;
				}
			}
			if(inBatch>0)
			{
				pstmt.executeBatch();
			}
			pstmt.close();pstmt=null;

			conn.commit();
			logger.info("updateScheduleItemDetails :: Schedule {"+scheduleId+"} :: items :: > "+itemsList.size()+" :: VIN / document rows :: > "+vinRows.size());
			return true;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "updateScheduleItemDetails()", e);
			rollback(conn, "updateScheduleItemDetails()");
			return false;
		}
		finally
		{
			close(rs, pstmt, null, conn, "updateScheduleItemDetails()");
		}
	}

	public static Long createSchedule(ScheduleDetails schDetails) throws SQLException
	{
		if(null==schDetails)
		{
			logger.info("createSchedule :: Schedule Details are null as parameters, return null.");
			return null;
		}
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			// the id is part of the name and of the thread name: insert, then name the row
			pstmt = conn.prepareStatement("INSERT INTO gms3_dmt_fm_sch (fm_sch_name, fm_sch_by, fm_processing_status) VALUES (?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, schDetails.getScheduleName());
			pstmt.setString(2, schDetails.getUserId());
			pstmt.setString(3, schDetails.getScheduleStatus());
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if(rs.next())
			{
				scheduleId = rs.getLong(1);
			}
			rs.close();rs=null;
			pstmt.close();pstmt=null;
			if(null==scheduleId || scheduleId<=0)
			{
				logger.info("createSchedule :: Failed to Generate Schedule Id. Return null.");
				rollback(conn, "createSchedule()");
				return null;
			}
			schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
			schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(scheduleId));
			pstmt = conn.prepareStatement("UPDATE gms3_dmt_fm_sch SET fm_sch_name=?, fm_sch_time=?, fm_thread_id=? WHERE fm_sch_id=?");
			pstmt.setString(1, schDetails.getScheduleName());
			pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
			pstmt.setString(3, schDetails.getThreadId());
			pstmt.setLong(4, scheduleId);
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			conn.commit();
			logger.info("createSchedule :: Schedule Id :: > " + scheduleId);
		}
		catch(Exception e)
		{
			logger.info("createSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "createSchedule()", e);
			rollback(conn, "createSchedule()");
			scheduleId = null;
		}
		finally
		{
			close(rs, pstmt, null, conn, "createSchedule()");
		}
		return scheduleId;
	}

	public static List<ScheduleDetails> getScheduleList(String fromDate, String toDate)
	{
		List<ScheduleDetails> scheduleList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			// + the items not yet taken into an update job (the View link shows only while there is one)
			String sql="SELECT s.*, (SELECT COUNT(*) FROM gms3_dmt_fm_sch_itm i WHERE i.fm_sch_id = s.fm_sch_id) AS fm_remaining_items"
					+ " FROM gms3_dmt_fm_sch s ";
			boolean byDate = null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate);
			if(byDate)
			{
				// compared as timestamps through the driver, not DATE_FORMAT on the stored value (see ScheduleDAO.getScheduleDetails)
				sql = sql+" WHERE FM_SCH_TIME >= ? AND FM_SCH_TIME < ? ";
			}
			sql+=" ORDER BY FM_SCH_ID DESC";
			conn = getConnection();
			pstmt  =conn.prepareStatement(sql);
			if(byDate)
			{
				java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("yyyy-MM-dd");
				day.setLenient(false);
				java.util.Calendar to = java.util.Calendar.getInstance();
				to.setTime(day.parse(toDate.trim()));
				to.add(java.util.Calendar.DATE, 1);
				pstmt.setTimestamp(1, new Timestamp(day.parse(fromDate.trim()).getTime()));
				pstmt.setTimestamp(2, new Timestamp(to.getTimeInMillis()));
			}
			rs = pstmt.executeQuery();
			int count=0;
			while(rs.next())
			{
				ScheduleDetails details=  new ScheduleDetails();
				count++;
				details.setSrNo(count);
				details.setScheduleId(rs.getLong("FM_SCH_ID"));
				details.setScheduleName(rs.getString("FM_SCH_NAME"));
				details.setThreadId(rs.getString("FM_THREAD_ID"));
				details.setScheduleTime(rs.getTimestamp("FM_SCH_TIME"));
				details.setFinishTime(rs.getTimestamp("FM_FINISH_TIME"));
				details.setUserId(rs.getString("FM_SCH_BY"));
				details.setTotalDocsForProcessing(rs.getLong("FM_TOTAL_DOCS_COUNT"));
				details.setRemainingItemsCount(rs.getLong("fm_remaining_items"));
				if(null!=rs.getBytes("FM_REMARKS"))
				{
					details.setRemarks(new String(rs.getBytes("FM_REMARKS")));
				}
				details.setScheduleStatus(rs.getString("FM_PROCESSING_STATUS"));
				details.setJobStatus(rs.getString("FM_JOB_STATUS"));
				if(null==scheduleList)
				{
					scheduleList = new ArrayList<ScheduleDetails>();
				}
				scheduleList.add(details);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleList()", e);
		}
		finally
		{
			close(rs, pstmt, null, conn, "getScheduleList()");
		}
		return scheduleList;
	}

	/**
	 * The items of an identification job with their VIN ranges (APP_VIN rows) and their impacted documents (IMP_DOC rows,
	 * ONE document map per document with every VIN range of it). Two queries for the whole job.
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<ItemDetails> getScheduleItemsList(String scheduleId)
	{
		ArrayList<ItemDetails> itemsList = null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			long schId = Long.parseLong(scheduleId.trim());
			conn = getConnection();
			pstmt = conn.prepareStatement("SELECT * FROM gms3_dmt_fm_sch_itm WHERE fm_sch_id=? ORDER BY fm_itm_id");
			pstmt.setLong(1, schId);
			rs = pstmt.executeQuery();
			Map<Long, ItemDetails> byId = new LinkedHashMap<Long, ItemDetails>();
			int count=0;
			while(rs.next())
			{
				ItemDetails details=  new ItemDetails();
				count++;
				details.setSrNo(count);
				details.setDocIdnSchId(rs.getLong("FM_SCH_ID"));
				details.setDocIdnItemId(rs.getLong("FM_ITM_ID"));
				details.setLocale(rs.getString("FM_LOCALE"));
				details.setCarlineInfo(rs.getString("FM_CARLINE_INFO"));
				details.setChannelLabel(rs.getString("FM_CH_LABEL"));
				details.setChannelRefKey(rs.getString("FM_CH_REFKEY"));
				details.setDisplayCarlineInfo(rs.getString("FM_DISP_CARLINE_INFO"));
				details.setDocumentTypeLabel(rs.getString("FM_DC_TYPE_LABEL"));
				details.setDocumentTypeRefKey(rs.getString("FM_DC_TYPE_REFKEY"));
				details.setTotalDocumentsCount(rs.getLong("FM_DOCS_COUNT"));
				details.setApplicableVinsCount(rs.getLong("FM_APP_VIN_COUNT"));
				if(null!=rs.getBytes("FM_REMARKS"))
				{
					details.setRemarks(new String(rs.getBytes("FM_REMARKS")));
				}
				byId.put(details.getDocIdnItemId(), details);
			}
			rs.close();rs=null;
			pstmt.close();pstmt=null;
			if(byId.isEmpty())
			{
				return null;
			}

			// THE VIN AND DOCUMENT ROWS OF THE WHOLE JOB
			Map<Long, Map<String, Map<String, Object>>> docsByItem = new HashMap<Long, Map<String, Map<String, Object>>>();
			pstmt = conn.prepareStatement("SELECT * FROM gms3_dmt_fm_itm_ap_vin WHERE fm_sch_id=? ORDER BY fm_vin_id");
			pstmt.setLong(1, schId);
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				ItemDetails details = byId.get(rs.getLong("FM_ITM_ID"));
				if(null==details)
				{
					continue;
				}
				String recordType = rs.getString("FM_RECORD_TYPE");
				if(REC_TYPE_APPLICABLE_VIN.equals(recordType))
				{
					if(null==details.getApplicableVINList())
					{
						details.setApplicableVINList(new ArrayList<ExcelRowDetails>());
					}
					details.getApplicableVINList().add(extractInfoFromResultSet(rs));
				}
				else if(REC_TYPE_IMPACTED_DOCUMENTS.equals(recordType))
				{
					String documentId = rs.getString("FM_DOCUMENT_ID");
					if(null==documentId || "".equals(documentId.trim()))
					{
						continue;
					}
					Map<String, Map<String, Object>> docs = docsByItem.get(details.getDocIdnItemId());
					if(null==docs)
					{
						docs = new LinkedHashMap<String, Map<String, Object>>();
						docsByItem.put(details.getDocIdnItemId(), docs);
					}
					Map<String, Object> docMap = docs.get(documentId.trim().toUpperCase());
					if(null==docMap)
					{
						docMap = new HashMap<String, Object>();
						docMap.put(AutomationConstants.DOCUMENTID, documentId);
						docMap.put(AutomationConstants.REFERENCEKEY, rs.getString("FM_DOC_TYPE_REFKEY"));
						docMap.put(AutomationConstants.VIN_LIST, new ArrayList<ExcelRowDetails>());
						docs.put(documentId.trim().toUpperCase(), docMap);
						if(null==details.getImpactedDocumentsList())
						{
							details.setImpactedDocumentsList(new ArrayList<Map<String,Object>>());
						}
						details.getImpactedDocumentsList().add(docMap);
					}
					((List<ExcelRowDetails>)docMap.get(AutomationConstants.VIN_LIST)).add(extractInfoFromResultSet(rs));
				}
			}
			itemsList = new ArrayList<ItemDetails>(byId.values());
			for(ItemDetails details : itemsList)
			{
				logger.info("getScheduleItemsList :: item {"+details.getDocIdnItemId()+"} :: VINs :: > "
						+(null==details.getApplicableVINList() ? 0 : details.getApplicableVINList().size())+" :: documents :: > "
						+(null==details.getImpactedDocumentsList() ? 0 : details.getImpactedDocumentsList().size()));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getScheduleItemsList()", e);
		}
		finally
		{
			close(rs, pstmt, null, conn, "getScheduleItemsList()");
		}
		return itemsList;
	}

	private static ExcelRowDetails extractInfoFromResultSet(ResultSet rs) throws SQLException
	{
		ExcelRowDetails ex = new ExcelRowDetails();
		ex.setScheduleId(rs.getLong("FM_SCH_ID"));
		ex.setItemId(rs.getLong("FM_ITM_ID"));
		ex.setLocale(rs.getString("FM_LOCALE"));

		ex.setNewCarlineCode(rs.getString("FM_N_CARLINE_CODE"));
		ex.setNewWmiCode(rs.getString("FM_N_WMI"));
		ex.setNewModelName(rs.getString("FM_N_MODEL_NAME"));
		ex.setNewModelNameEngForMC(rs.getString("FM_N_MODEL_NAME_REG_MC"));
		ex.setNewModelNameRegForMNAOMME(rs.getString("FM_N_MODEL_NAME_REG_MNAOMME"));
		ex.setNewVdsCode(rs.getString("FM_N_VDSCODE"));
		ex.setNewVisStartRange(rs.getString("FM_N_VISSTART"));
		ex.setNewVisEndRange(rs.getString("FM_N_VISEND"));
		ex.setNewRefKey(rs.getString("FM_N_REFKEY"));

		ex.setOldCarlineCode(rs.getString("FM_O_CARLINE_CODE"));
		ex.setOldWmiCode(rs.getString("FM_O_WMI"));
		ex.setOldModelName(rs.getString("FM_O_MODEL_NAME"));
		ex.setOldModelNameEngForMC(rs.getString("FM_O_MODEL_NAME_REG_MC"));
		ex.setOldModelNameRegForMNAOMME(rs.getString("FM_O_MODEL_NAME_REG_MNAOMME"));
		ex.setOldVdsCode(rs.getString("FM_O_VDSCODE"));
		ex.setOldVisStartRange(rs.getString("FM_O_VISSTART"));
		ex.setOldVisEndRange(rs.getString("FM_O_VISEND"));
		ex.setOldRefKey(rs.getString("FM_O_REFKEY"));

		ex.setProcessingStatus(rs.getString("FM_PROCESSING_STATUS"));
		if(null!=rs.getBytes("FM_REMARKS"))
		{
			ex.setRemarks(new String(rs.getBytes("FM_REMARKS")));
		}
		ex.setLocaleExistsInMDM(isY(rs.getString("FM_LOCALE_EXIST_MDM")));
		ex.setLocaleExistsInIM(isY(rs.getString("FM_LOCALE_EXIST_IM")));
		ex.setNewVinExistsinMDM(isY(rs.getString("FM_N_VIN_EXIST_MDM")));
		ex.setNewVinExistsinIM(isY(rs.getString("FM_N_VIN_EXIST_IM")));
		ex.setOldVinExistsinMDM(isY(rs.getString("FM_O_VIN_EXIST_MDM")));
		ex.setOldVinExistsinIM(isY(rs.getString("FM_O_VIN_EXIST_IM")));
		return ex;
	}

	/**
	 * The distinct VIN ranges (APP_VIN rows) of the selected items - the VIN manual type mapping of the update job and
	 * the "Only Facelift" check of new VIN categories missing in Kapture.
	 */
	public static ArrayList<ExcelRowDetails> getUniqueVINsListForSelectedItems(List<Long> itemIds)
	{
		ArrayList<ExcelRowDetails> uniqueVinsList = null;
		if(null==itemIds || itemIds.isEmpty())
		{
			return uniqueVinsList;
		}
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs  =null;
		try
		{
			StringBuilder in = new StringBuilder();
			for(int i=0;i<itemIds.size();i++)
			{
				in.append(i>0 ? ",?" : "?");
			}
			conn = getConnection();
			pstmt = conn.prepareStatement("SELECT DISTINCT fm_locale, fm_n_carline_code, fm_n_wmi, fm_n_model_name, fm_n_model_name_reg_mc,"
					+" fm_n_model_name_reg_mnaomme, fm_n_vdscode, fm_n_visstart, fm_n_visend, fm_n_refkey, fm_n_vin_exist_im, fm_o_carline_code,"
					+" fm_o_wmi, fm_o_model_name, fm_o_model_name_reg_mc, fm_o_model_name_reg_mnaomme, fm_o_vdscode, fm_o_visstart, fm_o_visend,"
					+" fm_o_refkey FROM gms3_dmt_fm_itm_ap_vin WHERE fm_itm_id IN ("+in+") AND fm_record_type=?");
			int p = 1;
			for(Long id : itemIds)
			{
				pstmt.setLong(p++, id.longValue());
			}
			pstmt.setString(p, REC_TYPE_APPLICABLE_VIN);
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				ExcelRowDetails ex = new ExcelRowDetails();
				ex.setLocale(rs.getString("FM_LOCALE"));
				ex.setNewCarlineCode(rs.getString("FM_N_CARLINE_CODE"));
				ex.setNewWmiCode(rs.getString("FM_N_WMI"));
				ex.setNewModelName(rs.getString("FM_N_MODEL_NAME"));
				ex.setNewModelNameEngForMC(rs.getString("FM_N_MODEL_NAME_REG_MC"));
				ex.setNewModelNameRegForMNAOMME(rs.getString("FM_N_MODEL_NAME_REG_MNAOMME"));
				ex.setNewVdsCode(rs.getString("FM_N_VDSCODE"));
				ex.setNewVisStartRange(rs.getString("FM_N_VISSTART"));
				ex.setNewVisEndRange(rs.getString("FM_N_VISEND"));
				ex.setNewRefKey(rs.getString("FM_N_REFKEY"));
				ex.setNewVinExistsinIM(isY(rs.getString("FM_N_VIN_EXIST_IM")));
				ex.setOldCarlineCode(rs.getString("FM_O_CARLINE_CODE"));
				ex.setOldWmiCode(rs.getString("FM_O_WMI"));
				ex.setOldModelName(rs.getString("FM_O_MODEL_NAME"));
				ex.setOldModelNameEngForMC(rs.getString("FM_O_MODEL_NAME_REG_MC"));
				ex.setOldModelNameRegForMNAOMME(rs.getString("FM_O_MODEL_NAME_REG_MNAOMME"));
				ex.setOldVdsCode(rs.getString("FM_O_VDSCODE"));
				ex.setOldVisStartRange(rs.getString("FM_O_VISSTART"));
				ex.setOldVisEndRange(rs.getString("FM_O_VISEND"));
				ex.setOldRefKey(rs.getString("FM_O_REFKEY"));
				if(null==uniqueVinsList)
				{
					uniqueVinsList = new ArrayList<ExcelRowDetails>();
				}
				uniqueVinsList.add(ex);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "getUniqueVINsListForSelectedItems()", e);
		}
		finally
		{
			close(rs, pstmt, null, conn, "getUniqueVINsListForSelectedItems()");
		}
		return uniqueVinsList;
	}

	private static ExcelRowDetails prepareAppVINData(ExcelRowDetails details, long scheduleId, Long itemId, String recordType)
	{
		ExcelRowDetails nDetails = new ExcelRowDetails();
		nDetails.setScheduleId(Long.valueOf(scheduleId));
		nDetails.setItemId(itemId);
		nDetails.setRecordType(recordType);
		nDetails.setDocTypeRefKey(details.getDocTypeRefKey());
		nDetails.setDocumentId(details.getDocumentId());
		nDetails.setLocale(details.getLocale());
		nDetails.setLocaleExistsInIM(details.isLocaleExistsInIM());
		nDetails.setLocaleExistsInMDM(details.isLocaleExistsInMDM());
		nDetails.setNewCarlineCode(details.getNewCarlineCode());
		nDetails.setNewModelName(details.getNewModelName());
		nDetails.setNewModelNameEngForMC(details.getNewModelNameEngForMC());
		nDetails.setNewModelNameRegForMNAOMME(details.getNewModelNameRegForMNAOMME());
		nDetails.setNewRefKey(details.getNewRefKey());
		nDetails.setNewVdsCode(details.getNewVdsCode());
		nDetails.setNewVinExistsinIM(details.isNewVinExistsinIM());
		nDetails.setNewVinExistsinMDM(details.isNewVinExistsinMDM());
		nDetails.setNewVisEndRange(details.getNewVisEndRange());
		nDetails.setNewVisStartRange(details.getNewVisStartRange());
		nDetails.setNewWmiCode(details.getNewWmiCode());
		nDetails.setOldCarlineCode(details.getOldCarlineCode());
		nDetails.setOldModelName(details.getOldModelName());
		nDetails.setOldModelNameEngForMC(details.getOldModelNameEngForMC());
		nDetails.setOldModelNameRegForMNAOMME(details.getOldModelNameRegForMNAOMME());
		nDetails.setOldRefKey(details.getOldRefKey());
		nDetails.setOldVdsCode(details.getOldVdsCode());
		nDetails.setOldVinExistsinIM(details.isOldVinExistsinIM());
		nDetails.setOldVinExistsinMDM(details.isOldVinExistsinMDM());
		nDetails.setOldVisEndRange(details.getOldVisEndRange());
		nDetails.setOldVisStartRange(details.getOldVisStartRange());
		nDetails.setOldWmiCode(details.getOldWmiCode());
		nDetails.setProcessingStatus(details.getProcessingStatus());
		nDetails.setRemarks(details.getRemarks());
		return nDetails;
	}

	/**
	 * An item taken into an update job leaves the identification job: its rows are deleted (legacy). 2026-10-12: the job
	 * KEEPS its document count (legacy reduced it, so a fully processed job showed 0) and gets a remark instead, e.g.
	 * "820 documents (ja-JP WIRING_DIAGRAMS CX-5 (KM)) processed by MGSS_MDSYNC_20261012_5708" - appended, in SQL.
	 *
	 * @param processedRemark the remark for this item
	 */
	public static boolean deleteItemDetails(Long scheduleId,Long itemId, int totalDocsCountToReduce, String processedRemark)
	{
		if(null==scheduleId || scheduleId.longValue()<=0 || null==itemId || itemId.longValue()<=0)
		{
			logger.info("deleteItemDetails :: Schedule Id / Item Id as parameters are null. Return false.");
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement("DELETE FROM gms3_dmt_fm_sch_itm WHERE fm_sch_id=? AND fm_itm_id=?");
			pstmt.setLong(1, scheduleId.longValue());
			pstmt.setLong(2, itemId.longValue());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;

			pstmt = conn.prepareStatement("DELETE FROM gms3_dmt_fm_itm_ap_vin WHERE fm_sch_id=? AND fm_itm_id=?");
			pstmt.setLong(1, scheduleId.longValue());
			pstmt.setLong(2, itemId.longValue());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;

			if(null!=processedRemark && !"".equals(processedRemark.trim()))
			{
				pstmt = conn.prepareStatement("UPDATE gms3_dmt_fm_sch SET fm_remarks = CASE WHEN fm_remarks IS NULL OR LENGTH(fm_remarks)=0"
						+ " THEN ? ELSE CONCAT(fm_remarks, ?) END WHERE fm_sch_id=?");
				pstmt.setBytes(1, processedRemark.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
				pstmt.setBytes(2, (" / "+processedRemark.trim()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
				pstmt.setLong(3, scheduleId.longValue());
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
			}

			conn.commit();
			logger.info("deleteItemDetails :: identification job {"+scheduleId+"} item {"+itemId+"} removed, documents :: > "+totalDocsCountToReduce);
			return true;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), "deleteItemDetails()", e);
			rollback(conn, "deleteItemDetails()");
			return false;
		}
		finally
		{
			close(null, pstmt, null, conn, "deleteItemDetails()");
		}
	}

	// ---- helpers -----------------------------------------------------------

	private static String yn(boolean b)
	{
		return b ? "Y" : "N";
	}

	private static boolean isY(String s)
	{
		return null!=s && "y".equals(s.trim().toLowerCase());
	}

	private static void setOrNull(PreparedStatement pstmt, int index, String value) throws SQLException
	{
		if(null!=value && !"".equals(value))
		{
			pstmt.setString(index, value);
		}
		else
		{
			pstmt.setNull(index, Types.VARCHAR);
		}
	}

	private static void rollback(Connection conn, String method)
	{
		try
		{
			if(null!=conn)
			{
				conn.rollback();
			}
		}
		catch(SQLException e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), method, e);
		}
	}

	private static void close(ResultSet rs, PreparedStatement pstmt, Statement stmt, Connection conn, String method)
	{
		try
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
			{
				// back to the pool in its default mode
				if(!conn.getAutoCommit())
				{
					conn.setAutoCommit(true);
				}
				conn.close();
			}
		}
		catch(SQLException e)
		{
			Utilities.printStackTraceToLogs(AutomationIdentificationDAO.class.getName(), method, e);
		}
	}
}
