package com.mazda.gms3.dmt.automation.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.kapture.KaptureApiClient;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * THE DATABASE PART OF THE VIN SWAP - for the documents Kapture accepted, the view content rows that carry an OLD VIN
 * range get the NEW one (MNAO: WMI / VDS / VIS; MC / MME also carline code, model and carline name), and for MME the
 * VIN manual type mapping rows of the old range move to the new one. The DMT document tables (_VIN / _VINMASTER / _CAT)
 * are not touched - as in legacy, where those updates were commented out.
 *
 * 2026-10-11 (Kapture): SET-BASED. One UPDATE per VIN range for all the documents of a batch (was one connection per
 * document and a SELECT plus an UPDATE per row). Should the batch fail, it is rolled back and every document is done again
 * on its own, so one bad document fails alone - the legacy unit of failure.
 *
 * 2026-10-12: a document this job PUBLISHED in Kapture also gets VC_CONTENT_STATUS = view.content.status.published in
 * its view content rows (the Publish Content job's update, same tables - MNAO VIN + model year). Only that column; the
 * DMT document tables and the preview table stay as they are. A document left as a draft keeps its status.
 */
public class TransactionDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(TransactionDAO.class);

	/**
	 * @param locale  locale of the documents (either form)
	 * @param docVins document id -> the VIN ranges of the document
	 * @param published document ids this job published in Kapture - their view content status becomes Published
	 * @return document id -> why its update failed; a document not in the map is done
	 */
	public Map<String, String> updateDocumentDetails(String locale, Map<String, List<ExcelRowDetails>> docVins, Set<String> published)
	{
		if(null==published)
		{
			published = Collections.emptySet();
		}
		Map<String, String> failed = new LinkedHashMap<String, String>();
		if(null==docVins || docVins.isEmpty())
		{
			return failed;
		}
		try
		{
			update(locale, docVins, published);
			return failed;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "updateDocumentDetails()", e);
			if(1==docVins.size())
			{
				failed.put(docVins.keySet().iterator().next(), message(e));
				return failed;
			}
			logger.info("updateDocumentDetails :: the batch of "+docVins.size()+" documents failed ("+e.getMessage()+") - each document is updated on its own.");
		}
		for(Map.Entry<String, List<ExcelRowDetails>> doc : docVins.entrySet())
		{
			Map<String, List<ExcelRowDetails>> one = new LinkedHashMap<String, List<ExcelRowDetails>>();
			one.put(doc.getKey(), doc.getValue());
			try
			{
				update(locale, one, published);
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "updateDocumentDetails()", e);
				logger.info("updateDocumentDetails :: Failed to Update New VIN Details for "+doc.getKey()+" in DATABASE.");
				failed.put(doc.getKey(), message(e));
			}
		}
		return failed;
	}

	/** All or nothing for the given documents. */
	private void update(String locale, Map<String, List<ExcelRowDetails>> docVins, Set<String> published) throws Exception
	{
		String market = AutomationDAO.marketOf(locale);
		String table = AutomationDAO.viewContentTable(locale);
		String vcLocale = locale.trim().replace("-", "_");

		// THE DOCUMENTS OF EACH VIN RANGE
		Map<String, ExcelRowDetails> vins = new LinkedHashMap<String, ExcelRowDetails>();
		Map<String, List<String>> docsByVin = new LinkedHashMap<String, List<String>>();
		for(Map.Entry<String, List<ExcelRowDetails>> doc : docVins.entrySet())
		{
			for(ExcelRowDetails vin : doc.getValue())
			{
				if(null==vin.getOldRefKey() || "".equals(vin.getOldRefKey()))
				{
					continue;
				}
				String key = vin.getOldRefKey().trim().toUpperCase()+"|"+nz(vin.getNewRefKey()).toUpperCase();
				if(!vins.containsKey(key))
				{
					vins.put(key, vin);
					docsByVin.put(key, new ArrayList<String>());
				}
				docsByVin.get(key).add(doc.getKey().trim());
			}
		}

		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			int rows = 0;
			for(Map.Entry<String, ExcelRowDetails> e : vins.entrySet())
			{
				ExcelRowDetails v = e.getValue();
				List<String> docs = docsByVin.get(e.getKey());
				StringBuilder in = new StringBuilder();
				for(int i=0;i<docs.size();i++)
				{
					in.append(i>0 ? ",?" : "?");
				}
				int p = 1;
				if("MNAO".equals(market))
				{
					// MNAO: WMI / VDS / VIS only - model and year are never touched (D7, legacy)
					pstmt = conn.prepareStatement("UPDATE "+table+" SET vc_vin_wmi_code=?, vc_vin_vds_code=?, vc_vin_vis_start_range=?, vc_vin_vis_end_range=?"
							+" WHERE vc_vin_document_id IN ("+in+") AND vc_vin_locale=? AND vc_vin_wmi_code=? AND vc_vin_vds_code=?"
							+" AND vc_vin_vis_start_range=? AND vc_vin_vis_end_range=?");
					pstmt.setString(p++, nz(v.getNewWmiCode()));
					pstmt.setString(p++, nz(v.getNewVdsCode()));
					pstmt.setString(p++, nz(v.getNewVisStartRange()));
					pstmt.setString(p++, nz(v.getNewVisEndRange()));
					for(String d : docs)
					{
						pstmt.setString(p++, d);
					}
					pstmt.setString(p++, vcLocale);
				}
				else
				{
					boolean mc = "MC".equals(market);
					pstmt = conn.prepareStatement("UPDATE "+table+" SET vc_vin_wmi_code=?, vc_vin_vds_code=?, vc_vin_vis_start_range=?, vc_vin_vis_end_range=?,"
							+" vc_vin_carline_code=?, vc_vin_model=?, vc_vin_carline_name=?"
							+" WHERE vc_vin_document_id IN ("+in+") AND vc_vin_carline_code=? AND vc_vin_wmi_code=? AND vc_vin_vds_code=?"
							+" AND vc_vin_vis_start_range=? AND vc_vin_vis_end_range=? AND vc_vin_model=?");
					pstmt.setString(p++, nz(v.getNewWmiCode()));
					pstmt.setString(p++, nz(v.getNewVdsCode()));
					pstmt.setString(p++, nz(v.getNewVisStartRange()));
					pstmt.setString(p++, nz(v.getNewVisEndRange()));
					pstmt.setString(p++, nz(v.getNewCarlineCode()));
					// MC: model = English name, carline name = regional; MME: model = English, carline name = regional
					pstmt.setString(p++, mc ? v.getNewModelNameEngForMC() : v.getNewModelName());
					pstmt.setString(p++, mc ? v.getNewModelName() : v.getNewModelNameRegForMNAOMME());
					for(String d : docs)
					{
						pstmt.setString(p++, d);
					}
					pstmt.setString(p++, nz(v.getOldCarlineCode()));
				}
				pstmt.setString(p++, nz(v.getOldWmiCode()));
				pstmt.setString(p++, nz(v.getOldVdsCode()));
				pstmt.setString(p++, nz(v.getOldVisStartRange()));
				pstmt.setString(p++, nz(v.getOldVisEndRange()));
				if(!"MNAO".equals(market))
				{
					pstmt.setString(p, nz("MC".equals(market) ? v.getOldModelNameEngForMC() : v.getOldModelName()));
				}
				rows += pstmt.executeUpdate();
				pstmt.close();pstmt=null;

				if("MME".equals(market))
				{
					// MME: THE VIN MANUAL TYPE MAPPING of the old range moves to the new range (12 NOVEMBER change)
					pstmt = conn.prepareStatement("UPDATE gms3_mdm_vin_ml_mapping SET vin_ml_carline_code=?, vin_ml_wmi_code=?, vin_ml_vds_code=?,"
							+" vin_ml_vis_start=?, vin_ml_vis_end=? WHERE vin_ml_locale=? AND vin_ml_carline_code=? AND vin_ml_wmi_code=?"
							+" AND vin_ml_vds_code=? AND vin_ml_vis_start=? AND vin_ml_vis_end=? AND vin_ml_status='A'");
					pstmt.setString(1, v.getNewCarlineCode());
					pstmt.setString(2, v.getNewWmiCode());
					pstmt.setString(3, v.getNewVdsCode());
					pstmt.setString(4, v.getNewVisStartRange());
					pstmt.setString(5, v.getNewVisEndRange());
					pstmt.setString(6, vcLocale);
					pstmt.setString(7, v.getOldCarlineCode());
					pstmt.setString(8, v.getOldWmiCode());
					pstmt.setString(9, v.getOldVdsCode());
					pstmt.setString(10, v.getOldVisStartRange());
					pstmt.setString(11, v.getOldVisEndRange());
					int moved = pstmt.executeUpdate();
					pstmt.close();pstmt=null;
					if(moved>0)
					{
						logger.info("updateDocumentDetails :: VIN manual type mapping :: "+v.getOldRefKey()+" -> "+v.getNewRefKey()+" :: rows :: > "+moved);
					}
				}
			}

			// VIEW CONTENT STATUS OF THE DOCUMENTS PUBLISHED BY THIS JOB (as the Publish Content job)
			List<String> live = new ArrayList<String>();
			for(String d : docVins.keySet())
			{
				if(published.contains(d.trim()))
				{
					live.add(d.trim());
				}
			}
			int statusRows = 0;
			if(!live.isEmpty())
			{
				String[][] statusTables = "MNAO".equals(market)
						? new String[][] { { table, "vc_vin_" }, { "gms3_vc_model_year_details", "vc_my_" } }
						: new String[][] { { table, "vc_vin_" } };
				StringBuilder in = new StringBuilder();
				for(int i=0;i<live.size();i++)
				{
					in.append(i>0 ? ",?" : "?");
				}
				for(String[] t : statusTables)
				{
					pstmt = conn.prepareStatement("UPDATE "+t[0]+" SET vc_content_status=? WHERE "+t[1]+"locale=? AND "+t[1]+"document_id IN ("+in+")");
					int p = 1;
					pstmt.setString(p++, KaptureApiClient.require("view.content.status.published"));
					pstmt.setString(p++, vcLocale);
					for(String d : live)
					{
						pstmt.setString(p++, d);
					}
					statusRows += pstmt.executeUpdate();
					pstmt.close();pstmt=null;
				}
			}
			conn.commit();
			logger.info("updateDocumentDetails :: "+table+" :: documents :: > "+docVins.size()+" :: VIN ranges :: > "+vins.size()+" :: rows updated :: > "+rows
					+(live.isEmpty() ? "" : " :: published documents :: > "+live.size()+" :: status rows set Published :: > "+statusRows));
		}
		catch(Exception e)
		{
			if(null!=conn)
			{
				try
				{
					conn.rollback();
				}
				catch(SQLException e1)
				{
					Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "update()", e1);
				}
			}
			throw e;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
				{
					conn.setAutoCommit(true);
					conn.close();
				}
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(TransactionDAO.class.getName(), "update()", e);
			}
		}
	}

	private static String message(Exception e)
	{
		return null==e.getMessage() ? e.toString() : e.getMessage();
	}

	private static String nz(String s)
	{
		return null==s ? "" : s.trim();
	}
}
