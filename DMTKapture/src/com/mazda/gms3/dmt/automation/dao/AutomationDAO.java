package com.mazda.gms3.dmt.automation.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.kapture.KaptureLookupDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.masterdata.MdmExcelLoader;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class AutomationDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(AutomationDAO.class);

	/*
	 * =====================================================================================================
	 * CHECKS OF THE FACELIFT ROWS - KAPTURE (2026-10-11, InfoManager / OK_IM replaced)
	 *  - locale in MDM (gms3_mdm_manual_language) and in Kapture (kapture.default.locales.table)
	 *  - old / new VIN range in MDM (gms3_mdm_vin_detail, alternate locale as before) - its carline names are kept
	 *  - old / new VIN category in Kapture: the reference key in the Kapture categories FOR THE LOCALE of the row,
	 *    one query per locale for all rows (the document is updated in that locale, so the category must be there)
	 *  - a NEW VIN range missing in MDM or Kapture is still accepted when the ALL_VIN file of the locale in the
	 *    master data folder carries it (newVinInMasterDataFile): "Master Data with Facelift" loads it at the start
	 *    of the update job; "Only Facelift" cannot be scheduled while it is missing in Kapture.
	 * Every lookup is made once per distinct value. A lookup that FAILS throws - it is never taken as "not found".
	 * =====================================================================================================
	 */
	public static ArrayList<ExcelRowDetails> validateExcelData(ArrayList<ExcelRowDetails> inputDataList, File masterDataFolder) throws Exception
	{
		if(null==inputDataList || inputDataList.isEmpty())
		{
			return inputDataList;
		}
		Connection conn = null;
		try
		{
			conn = getConnection();
			Set<String> kaptureLocales = KaptureLookupDAO.getDefaultLocales();
			Map<String, Boolean> mdmLocales = new HashMap<String, Boolean>();
			Map<String, String> mdmVins = new HashMap<String, String>();
			Map<String, List<String>> refKeysByLocale = new LinkedHashMap<String, List<String>>();
			for(ExcelRowDetails data : inputDataList)
			{
				String mdmLocale = data.getLocale().trim().replace("_", "-");
				String kLocale = data.getLocale().trim().replace("-", "_");
				Boolean inMdm = mdmLocales.get(mdmLocale.toLowerCase());
				if(null==inMdm)
				{
					inMdm = Boolean.valueOf(localeExistsInMDM(mdmLocale, conn));
					mdmLocales.put(mdmLocale.toLowerCase(), inMdm);
				}
				data.setLocaleExistsInMDM(inMdm.booleanValue());
				data.setLocaleExistsInIM(kaptureLocales.contains(kLocale.toUpperCase()));

				boolean mc = isLocaleOf("mc.locales", kLocale);
				applyModelNames(data, true, mc, mdmVin(mdmVins, mdmLocale, data.getOldCarlineCode(), data.getOldWmiCode(), data.getOldVdsCode(),
						data.getOldVisStartRange(), data.getOldVisEndRange(), conn));
				applyModelNames(data, false, mc, mdmVin(mdmVins, mdmLocale, data.getNewCarlineCode(), data.getNewWmiCode(), data.getNewVdsCode(),
						data.getNewVisStartRange(), data.getNewVisEndRange(), conn));
				data.setOldVinExistsinMDM(null!=data.getOldModelName() && !"".equals(data.getOldModelName()));
				data.setNewVinExistsinMDM(null!=data.getNewModelName() && !"".equals(data.getNewModelName()));

				List<String> keys = refKeysByLocale.get(kLocale.toUpperCase());
				if(null==keys)
				{
					keys = new ArrayList<String>();
					refKeysByLocale.put(kLocale.toUpperCase(), keys);
				}
				keys.add(data.getOldRefKey());
				keys.add(data.getNewRefKey());
			}

			// THE VIN CATEGORIES IN KAPTURE - one lookup per locale
			Map<String, Set<String>> foundByLocale = new HashMap<String, Set<String>>();
			for(Map.Entry<String, List<String>> e : refKeysByLocale.entrySet())
			{
				foundByLocale.put(e.getKey(), KaptureLookupDAO.getCategories(new ArrayList<String>(new LinkedHashSet<String>(e.getValue())), e.getKey()).keySet());
			}
			Map<String, Map<String, String>> masterDataFileVins = new HashMap<String, Map<String, String>>();
			for(ExcelRowDetails data : inputDataList)
			{
				String kLocale = data.getLocale().trim().replace("-", "_");
				Set<String> found = foundByLocale.get(kLocale.toUpperCase());
				data.setOldVinExistsinIM(found.contains(data.getOldRefKey().trim().toUpperCase()));
				data.setNewVinExistsinIM(found.contains(data.getNewRefKey().trim().toUpperCase()));
				if(data.isNewVinExistsinMDM() && data.isNewVinExistsinIM())
				{
					continue;
				}
				// NOT (FULLY) THERE YET - IS IT IN THE MASTER DATA FILE OF THE LOCALE?
				String mdmLocale = data.getLocale().trim().replace("_", "-");
				Map<String, String> fileVins = masterDataFileVins.get(mdmLocale.toLowerCase());
				if(null==fileVins)
				{
					fileVins = new HashMap<String, String>();
					for(String[] v : MdmExcelLoader.readAllVinRanges(masterDataFolder, mdmLocale))
					{
						fileVins.put(vinKey(v[1], v[2], v[3], v[4], v[5]), v[0]);
					}
					masterDataFileVins.put(mdmLocale.toLowerCase(), fileVins);
				}
				String model = fileVins.get(vinKey(data.getNewCarlineCode(), data.getNewWmiCode(), data.getNewVdsCode(), data.getNewVisStartRange(), data.getNewVisEndRange()));
				if(null!=model)
				{
					data.setNewVinInMasterDataFile(true);
					// the English name from the file until the update job reads both names from MDM after the load
					if(null==data.getNewModelName() || "".equals(data.getNewModelName()))
					{
						if(isLocaleOf("mc.locales", kLocale))
						{
							data.setNewModelNameEngForMC(model);
						}
						else
						{
							data.setNewModelName(model);
						}
					}
				}
			}
			logger.info("validateExcelData :: rows checked :: > "+inputDataList.size()+" :: MDM VIN lookups :: > "+mdmVins.size()
					+" :: Kapture locales :: > "+refKeysByLocale.keySet());
		}
		finally
		{
			if(null!=conn)
			{
				try
				{
					conn.close();
				}
				catch(SQLException e)
				{
					Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
				}
			}
		}
		return inputDataList;
	}

	/**
	 * The new VIN reference keys (upper case) of the rows that are in Kapture now for the row's locale - for the rows
	 * that relied on the master data file when they were validated. One lookup per locale; a failed lookup throws.
	 */
	public static Set<String> newVinsInKapture(List<ExcelRowDetails> rows)
	{
		Map<String, List<String>> byLocale = new LinkedHashMap<String, List<String>>();
		for(ExcelRowDetails data : rows)
		{
			String kLocale = data.getLocale().trim().replace("-", "_").toUpperCase();
			List<String> keys = byLocale.get(kLocale);
			if(null==keys)
			{
				keys = new ArrayList<String>();
				byLocale.put(kLocale, keys);
			}
			keys.add(data.getNewRefKey());
		}
		Set<String> found = new HashSet<String>();
		for(Map.Entry<String, List<String>> e : byLocale.entrySet())
		{
			for(String k : KaptureLookupDAO.getCategories(new ArrayList<String>(new LinkedHashSet<String>(e.getValue())), e.getKey()).keySet())
			{
				found.add(e.getKey()+"|"+k);
			}
		}
		return found;
	}

	/** VIN range key of a row and of the ALL_VIN file - trimmed, upper case. */
	private static String vinKey(String carline, String wmi, String vds, String visStart, String visEnd)
	{
		return (nz(carline)+"|"+nz(wmi)+"|"+nz(vds)+"|"+nz(visStart)+"|"+nz(visEnd)).toUpperCase();
	}

	private static String nz(String s)
	{
		return null==s ? "" : s.trim();
	}

	private static boolean localeExistsInMDM(String mdmLocale, Connection conn) throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.prepareStatement("SELECT mdm_ml_id FROM gms3_mdm_manual_language WHERE TRIM(LOWER(mdm_ml_lang_code))=? AND mdm_ml_flag=?");
			pstmt.setString(1, mdmLocale.trim().toLowerCase());
			pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
			rs = pstmt.executeQuery();
			return rs.next();
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
		}
	}

	/** checkVINExistsInMDM, once per distinct VIN range. */
	private static String mdmVin(Map<String, String> cache, String mdmLocale, String carline, String wmi, String vds, String visStart, String visEnd, Connection conn)
	{
		String key = mdmLocale.toUpperCase()+"|"+vinKey(carline, wmi, vds, visStart, visEnd);
		if(!cache.containsKey(key))
		{
			cache.put(key, checkVINExistsInMDM(mdmLocale, carline, wmi, vds, visStart, visEnd, conn));
		}
		return cache.get(key);
	}

	/**
	 * MDM carline names ("regional<TOK_SEP>english") onto the row, as before: MC keeps the REGIONAL name as the model
	 * name and the English one apart; MNAO / MME keep the ENGLISH name as the model name and the regional one apart.
	 */
	private static void applyModelNames(ExcelRowDetails data, boolean old, boolean mc, String names)
	{
		if(null==names || "".equals(names) || names.indexOf("<TOK_SEP>")<0)
		{
			return;
		}
		String regional = names.substring(0, names.indexOf("<TOK_SEP>"));
		String english = names.substring(names.indexOf("<TOK_SEP>")+9);
		if(old)
		{
			if(mc)
			{
				data.setOldModelName(regional);
				data.setOldModelNameEngForMC(english);
			}
			else
			{
				data.setOldModelName(english);
				data.setOldModelNameRegForMNAOMME(regional);
			}
		}
		else
		{
			if(mc)
			{
				data.setNewModelName(regional);
				data.setNewModelNameEngForMC(english);
			}
			else
			{
				data.setNewModelName(english);
				data.setNewModelNameRegForMNAOMME(regional);
			}
		}
	}

	/**
	 * Reads the NEW VIN carline names from MDM again - after "Master Data with Facelift" has loaded them. A row whose
	 * VIN range is still not in MDM keeps what it has.
	 */
	public static void refreshNewModelNames(List<ExcelRowDetails> rows)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			Map<String, String> cache = new HashMap<String, String>();
			for(ExcelRowDetails data : rows)
			{
				boolean mc = isLocaleOf("mc.locales", data.getLocale());
				String names = mdmVin(cache, data.getLocale().trim().replace("_", "-"), data.getNewCarlineCode(), data.getNewWmiCode(),
						data.getNewVdsCode(), data.getNewVisStartRange(), data.getNewVisEndRange(), conn);
				if(null!=names && !"".equals(names))
				{
					applyModelNames(data, false, mc, names);
					data.setNewVinExistsinMDM(true);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "refreshNewModelNames()", e);
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "refreshNewModelNames()", e);
			}
		}
	}

	/** true when the locale (either form) is one of the comma separated locales of the property (mnao.locales ...). */
	public static boolean isLocaleOf(String property, String locale)
	{
		if(null==locale)
		{
			return false;
		}
		String wanted = locale.trim().replace("-", "_");
		for(String l : ApplicationProperties.getProperty(property).split(","))
		{
			if(l.trim().equalsIgnoreCase(wanted))
			{
				return true;
			}
		}
		return false;
	}

	/** MNAO / MC / MME of a locale - MME for any locale that is neither MNAO nor MC, as the legacy table choice. */
	public static String marketOf(String locale)
	{
		if(isLocaleOf("mnao.locales", locale))
		{
			return "MNAO";
		}
		if(isLocaleOf("mc.locales", locale))
		{
			return "MC";
		}
		return "MME";
	}

	/** The view content VIN table of a locale - MME has one table per locale, in lower case. */
	public static String viewContentTable(String locale)
	{
		String market = marketOf(locale);
		if("MNAO".equals(market))
		{
			return "gms3_vc_vin_details";
		}
		if("MC".equals(market))
		{
			return "gms3_vc_japan_vin_details";
		}
		return "gms3_vc_mme_vin_dtl_"+locale.trim().replace("-", "_").toLowerCase();
	}

	public static ArrayList<ItemDetails> getImpactedDocumentsListModelWise(ArrayList<ItemDetails> itemsList)
	{
		Connection conn = null;
		ArrayList<ItemDetails> finalItemsList = new ArrayList<ItemDetails>();
		try
		{
			if(null!=itemsList && itemsList.size()>0)
			{
				conn = getConnection();
				String locale="";
				String documentPrefix="";
				ItemDetails itemDetails = new ItemDetails();
				Map<String, Object> data = null;
				ArrayList<Map<String, Object>> impactedDocumentsList = new ArrayList<Map<String,Object>>();
				for(int r=0;r<itemsList.size();r++)
				{
					itemDetails = (ItemDetails)itemsList.get(r);
					// applicableVIN List
					if(null!=itemDetails.getChannelRefKey() && !"".equals(itemDetails.getChannelRefKey()) && 
							null!=itemDetails.getLocale() && !"".equals(itemDetails.getLocale()) && 
							null!=itemDetails.getApplicableVINList() && itemDetails.getApplicableVINList().size()>0)
					{
						impactedDocumentsList = new ArrayList<Map<String,Object>>();

						locale = itemDetails.getLocale();
						locale = locale.replace("-", "_");

						if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES))
						{
							documentPrefix="AC";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_TRAINING))
						{
							documentPrefix="TR";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS))
						{
							documentPrefix="VI";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS))
						{
							documentPrefix="WD";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE))
						{
							documentPrefix="SI";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE))
						{
							documentPrefix="SM";
						}
						else if(null!=itemDetails.getChannelRefKey() && itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
						{
							documentPrefix="OSM";
						}

						// get impactedDocumentsList
						impactedDocumentsList = getImpactedDocumentsData(itemDetails.getApplicableVINList(), documentPrefix, locale, conn);
						locale=null;
						documentPrefix = null;

						if(null!=impactedDocumentsList && impactedDocumentsList.size()>0)
						{
							itemDetails.setImpactedDocumentsList(impactedDocumentsList);
						}
						impactedDocumentsList = null;
					}
				}
				itemDetails = null;


				/*
				 *  NOW ITERATE ITEMS LIST AND IDENTIFY DISTINCT DOCUMENT TYPES ALONG WITH IMPACTED DOCS COUNT FOR SI SM & OSM 
				 */
				if(null!=itemsList && itemsList.size()>0)
				{
					itemDetails = new ItemDetails();
					ItemDetails existDetails = new ItemDetails();
					ItemDetails newDetails = new ItemDetails();
					for(int a=0;a<itemsList.size();a++)
					{
						itemDetails = (ItemDetails)itemsList.get(a);
						if(null!=itemDetails.getChannelRefKey())
						{
							if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE) || 
									itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE) ||  
									itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
							{
								if(null!=itemDetails.getImpactedDocumentsList() && itemDetails.getImpactedDocumentsList().size()>0)
								{
									data = new HashMap<String, Object>();
									for(int r=0;r<itemDetails.getImpactedDocumentsList().size();r++)
									{
										data = (Map<String, Object>)itemDetails.getImpactedDocumentsList().get(r);
										if(null!=data.get(AutomationConstants.REFERENCEKEY) && !"".equals(data.get(AutomationConstants.REFERENCEKEY)))
										{
											boolean add = true;
											if(null!=finalItemsList && finalItemsList.size()>0)
											{
												existDetails = new ItemDetails();
												for(int y=0;y<finalItemsList.size();y++)
												{
													existDetails = (ItemDetails)finalItemsList.get(y);
													if(existDetails.getLocale().trim().toLowerCase().equals(itemDetails.getLocale().trim().toLowerCase()) && 
															existDetails.getChannelRefKey().trim().toLowerCase().equals(itemDetails.getChannelRefKey().trim().toLowerCase()) &&
															existDetails.getCarlineInfo().trim().toLowerCase().equals(itemDetails.getCarlineInfo().trim().toLowerCase()) &&
															existDetails.getDocumentTypeRefKey().trim().toLowerCase().equals(String.valueOf(data.get(AutomationConstants.REFERENCEKEY)).trim().toLowerCase()))
													{
														// do not add - New Item, Document Type already added instead add Document to impactedDocumentsList
														if(null==existDetails.getImpactedDocumentsList())
														{
															existDetails.setImpactedDocumentsList(new ArrayList<Map<String,Object>>());
														}
														// add current DocumentId to impactedDocumentsList of documentType
														existDetails.getImpactedDocumentsList().add(data);
														add =false;
														break;
													}
												}
												existDetails = null;
											}

											if(add==true)
											{
												newDetails = new ItemDetails();
												newDetails.setLocale(itemDetails.getLocale());
												newDetails.setChannelLabel(itemDetails.getChannelLabel());
												newDetails.setChannelRefKey(itemDetails.getChannelRefKey());
												newDetails.setDocumentTypeRefKey(String.valueOf(data.get(AutomationConstants.REFERENCEKEY)));
												if(null!=newDetails.getDocumentTypeRefKey())
												{
													newDetails.setDocumentTypeLabel(newDetails.getDocumentTypeRefKey().replace("_", " "));
												}
												newDetails.setCarlineInfo(itemDetails.getCarlineInfo());
												newDetails.setApplicableVINList(itemDetails.getApplicableVINList());
												if(null==newDetails.getImpactedDocumentsList())
												{
													newDetails.setImpactedDocumentsList(new ArrayList<Map<String,Object>>());
												}
												// add current DocumentId to impactedDocumentsList of documentType
												newDetails.getImpactedDocumentsList().add(data);
												finalItemsList.add(newDetails);
												newDetails = null;
											}
										}
									}
								}
							}
							else
							{
								// OTHER CHANNELS - add them as it is
								finalItemsList.add(itemDetails);
							}
						}
						itemDetails = null;
					}
					itemDetails = null;
					existDetails = null;
					newDetails = null;
				}

				locale=null;
				documentPrefix=null;
				itemDetails = null;
				data = null;
				impactedDocumentsList = null;


				// DO A FINAL ITERATION AND SET TOTAL DOCUMENTS COUNT FOR EACH ROW
				if(null!=finalItemsList && finalItemsList.size()>0)
				{
					itemDetails = new ItemDetails();
					for(int a=0;a<finalItemsList.size();a++)
					{
						itemDetails = (ItemDetails)finalItemsList.get(a);
						itemDetails.setSrNo(a+1);
						if(null!=itemDetails.getImpactedDocumentsList())
						{
							itemDetails.setTotalDocumentsCount(new Long(itemDetails.getImpactedDocumentsList().size()).longValue());
						}
					}
					itemDetails = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsListModelWise()", e);
		}
		finally
		{
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsListModelWise()", e);
				}
		}
		return finalItemsList;
	}

	/**
	 * The documents of the channel (document id prefix) mapped to any of the OLD VIN ranges in the view content table of
	 * the locale - MNAO on WMI / VDS / VIS, MC / MME also on the carline code (model name left out since 2022-11-11).
	 * One document map per document, with the VINs it carries (VIN_LIST) and, for SM / OSM / SI, its document type.
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<Map<String, Object>> getImpactedDocumentsData(ArrayList<ExcelRowDetails> applicableVINList, String documentPrefix, String locale, Connection conn)
	{
		Map<String, Map<String, Object>> byDocument = new LinkedHashMap<String, Map<String, Object>>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			boolean mnao = isLocaleOf("mnao.locales", locale);
			String sql = "SELECT DISTINCT vc_vin_document_id, vc_vin_document_type FROM "+viewContentTable(locale)
					+" WHERE vc_vin_locale=? AND vc_vin_wmi_code=? AND vc_vin_vds_code=? AND vc_vin_vis_start_range=? AND vc_vin_vis_end_range=?"
					+(mnao ? "" : " AND vc_vin_carline_code=?")+" AND vc_vin_document_id LIKE ?";
			pstmt = conn.prepareStatement(sql);
			for(ExcelRowDetails data : applicableVINList)
			{
				if(null==data.getOldRefKey() || "".equals(data.getOldRefKey()))
				{
					continue;
				}
				int p = 1;
				pstmt.setString(p++, locale);
				pstmt.setString(p++, data.getOldWmiCode());
				pstmt.setString(p++, data.getOldVdsCode());
				pstmt.setString(p++, data.getOldVisStartRange());
				pstmt.setString(p++, data.getOldVisEndRange());
				if(!mnao)
				{
					pstmt.setString(p++, data.getOldCarlineCode());
				}
				pstmt.setString(p, documentPrefix+"%");
				rs = pstmt.executeQuery();
				while(rs.next())
				{
					String documentId = rs.getString(1);
					if(null==documentId || "".equals(documentId.trim()))
					{
						continue;
					}
					Map<String, Object> documentMap = byDocument.get(documentId.trim());
					if(null==documentMap)
					{
						documentMap = new HashMap<String, Object>();
						documentMap.put(AutomationConstants.DOCUMENTID, documentId);
						// the document type - only for SM / OSM / SI
						if(documentPrefix.equals("SM") || documentPrefix.equals("OSM") || documentPrefix.equals("SI"))
						{
							documentMap.put(AutomationConstants.REFERENCEKEY, rs.getString(2));
						}
						documentMap.put(AutomationConstants.VIN_LIST, new ArrayList<ExcelRowDetails>());
						byDocument.put(documentId.trim(), documentMap);
					}
					// THE VINS OF THE DOCUMENT - for the reports and the update
					ArrayList<ExcelRowDetails> vins = (ArrayList<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST);
					boolean addVin = true;
					for(ExcelRowDetails existData : vins)
					{
						if(existData.getOldRefKey().trim().equalsIgnoreCase(data.getOldRefKey().trim()))
						{
							addVin = false;
							break;
						}
					}
					if(addVin)
					{
						vins.add(data);
					}
				}
				rs.close();
				rs = null;
			}
			logger.info("getImpactedDocumentsData :: "+locale+" "+documentPrefix+" :: impacted documents :: > "+byDocument.size());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsData()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsData()", e);
			}
		}
		return new ArrayList<Map<String, Object>>(byDocument.values());
	}

	private static String checkVINExistsInMDM(String localeCode,String carlineCode,String wmiCode,String vdsCode, String visStartRange, String visEndRange, Connection conn)
	{
		String carlineName=null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			//			String localeToCheck=localeCode;
			//			localeToCheck = localeToCheck.replace("-", "_");
			//			if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(localeToCheck.trim().toLowerCase())>-1)
			//			{
			//				// check here, if localeCode is en_CA - then use en_US
			//				if(localeCode.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
			//				{
			//					// use en_US locale to check
			//					localeCode = ApplicationProperties.getProperty("en-us");
			//					// replace _ by - for MDM Transactions
			//					localeCode  = localeCode.replace("_", "-");
			//				}
			//			}
			//			else if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(localeToCheck.trim().toLowerCase())>-1)
			//			{
			//				// always use en_UK locale to check
			//				localeCode = ApplicationProperties.getProperty("en-uk");
			//				// replace _ by - for MDM Transactions
			//				localeCode  = localeCode.replace("_", "-");
			//			}

			// GET ALTERNATE LOCALE
			String localeToCheck=localeCode;
			localeToCheck = localeToCheck.replace("-", "_");
			// LET THIS BE THERE FOR IDENTIFYING MASTER DATA TYPE
			if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(localeToCheck.trim().toLowerCase())>-1)
			{
				localeCode = ScheduleDAO.getAlternateLocale(localeCode, AutoSyncConstants.ITEM_KEY_VIN_RANGE, conn);
			}
			else
			{
				// MC / MME - CARLINE
				localeCode = ScheduleDAO.getAlternateLocale(localeCode, AutoSyncConstants.ITEM_KEY_CARLINE, conn);
			}
			// check in VIN Table
			String  sql="SELECT MDM_CRLN_NAME_ENG_LANG,MDM_CRLN_NAME_REGIONAL_LANG FROM gms3_mdm_vin_detail WHERE MDM_VIN_FLAG=? "
					+ " AND TRIM(LOWER(MDM_ML_LANG_CODE))=? AND TRIM(LOWER(MDM_CRLN_CODE))=? "
					+ " AND TRIM(LOWER(MDM_VIN_WMI_CODE))=? AND TRIM(LOWER(MDM_VIN_VDS_CODE))=? "
					+ " AND TRIM(LOWER(MDM_VIN_VIS_START_RANGE))=? AND TRIM(LOWER(MDM_VIN_VIS_END_RANGE))=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
			pstmt.setString(2, localeCode.trim().toLowerCase());
			pstmt.setString(3, carlineCode.trim().toLowerCase());
			pstmt.setString(4, wmiCode.trim().toLowerCase());
			pstmt.setString(5, vdsCode.trim().toLowerCase());
			pstmt.setString(6, visStartRange.trim().toLowerCase());
			pstmt.setString(7, visEndRange.trim().toLowerCase());
			rs = pstmt.executeQuery();
			if(rs.next())
			{
				if(null!=rs.getString("MDM_CRLN_NAME_REGIONAL_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_REGIONAL_LANG")))
				{
					carlineName  =rs.getString("MDM_CRLN_NAME_REGIONAL_LANG");
				}
				/*
				 * ADD ENGLISH LABEL AS WELL USED FOR IDENITFYING IMPACTED DOCUMENTS FOR MC LOCLAE
				 * AND ALSO REQUIRED FOR POPULATING IN VIEW CONTENT TABLE
				 * 24 JULY 2019
				 */
				if(null==carlineName || "".equals(carlineName))
				{
					carlineName="";
				}
				carlineName+="<TOK_SEP>";
				
				if(null!=rs.getString("MDM_CRLN_NAME_ENG_LANG") && !"".equals(rs.getString("MDM_CRLN_NAME_ENG_LANG")))
				{
					carlineName+=rs.getString("MDM_CRLN_NAME_ENG_LANG");
				}
			}
			sql=null;
			localeToCheck = null; 
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "checkVINExistsInMDM()", e);
		}
		finally
		{
			try {
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "checkVINExistsInMDM()", e);
			}
			pstmt=null;
			rs=null;

			// set all params to null;
			localeCode = null;
			carlineCode=null;
			wmiCode  = null;
			vdsCode=null;
			visStartRange=null;
			visEndRange = null;
		}
		return carlineName;
	}	

	public static ArrayList<ExcelRowDetails> processVinManualTypeMapping(ArrayList<ExcelRowDetails> inputDataList)
	{
		Connection conn = null;
		try
		{
			if(null!=inputDataList && inputDataList.size()>0)
			{
				// get connection

				// identify manual types for the old vin for locale
				// identify manual types for the new vin for locale
				// match them if all manual types matches, delete the entry for old vin form vin manual mapping table & create entry for new vin.
				// if not matches - update the old vin row and new vin row , if they do not exist create for both

				conn = getConnection();
				ExcelRowDetails vinDetails =null;
				VinMLMappingDetails oldDetails = null;
				VinMLMappingDetails newDetails = null;
				for(int a=0;a<inputDataList.size();a++)
				{
					vinDetails=  (ExcelRowDetails) inputDataList.get(a);
					/*
					 * APPLICABLE ONLY FOR MME LOCLAES
					 * 25TH JULY 2019
					 */
					if(isLocaleOf("mme.locales", vinDetails.getLocale()))
					{
						// as it is MME - Use Default Variable for Model Name
						// FETCH OLD VIN DATA
						oldDetails =  retrieveManualTypeData(vinDetails.getLocale().replace("-", "_"), vinDetails.getOldCarlineCode(), 
								vinDetails.getOldWmiCode(), vinDetails.getOldVdsCode(), vinDetails.getOldVisStartRange(), vinDetails.getOldVisEndRange(), vinDetails.getOldModelName(), conn);

						// FETCH NEW VIN DATA
						newDetails = retrieveManualTypeData(vinDetails.getLocale().replace("-", "_"), vinDetails.getNewCarlineCode(), 
								vinDetails.getNewWmiCode(), vinDetails.getNewVdsCode(), vinDetails.getNewVisStartRange(), vinDetails.getNewVisEndRange(), vinDetails.getNewModelName(), conn);

						/*
						 * NOW CHECK IF MANUAL TYPE LIST FOR OLD VIN & NEW VIN IS NULL - DELETE ROW FORM VIN MANUAL TYPE MAPPING TABLE FOR BOTH
						 * 	UPDATE PROCESSING STATUS OF THE ROW AS SUCCESS.
						 * 	SET REMARKS - ROWS FOR BOTH VINS DLEETED AS NO MANUAL TYPE MAPPINGS FOUND FOR BOTH OLD & NEW VIN.
						 */
						vinDetails.setProcessingStatus(null);
						if(null==oldDetails || null==newDetails)
						{
							// the manual types could not be read - nothing is changed for this VIN, the row FAILS
							vinDetails.setProcessingStatus(AutomationConstants.STATUS_FAILURE);
							vinDetails.setRemarks("Failed to read the manual types of the "+(null==oldDetails ? "Old" : "New")+" VIN from the view content table.");
						}
						if(null!=oldDetails && null!=newDetails)
						{
							if(null!=oldDetails.getManualTypesList() && oldDetails.getManualTypesList().size()>0)
							{
								// insert / update Row for OLD VIN in TABLE.
								vinDetails = saveVINManualTypeMapping(vinDetails, oldDetails, "Old", conn);
							}
							else
							{
								// delete Row for OLD VIN from TABLE.
								vinDetails=  deleteVINManualTypeMapping(vinDetails, oldDetails, "Old", conn);
							}
							if(null!=newDetails.getManualTypesList() && newDetails.getManualTypesList().size()>0)
							{
								// insert / update Row for NEW VIN in TABLE.
								vinDetails = saveVINManualTypeMapping(vinDetails, newDetails, "New", conn);
							}
							else
							{
								// delete Row for NEW VIN from TABLE.
								vinDetails=  deleteVINManualTypeMapping(vinDetails, newDetails, "New", conn);
							}
						}

						// set oldDetails and newDetails in vinDetails
						vinDetails.setOldDetails(oldDetails);
						vinDetails.setNewDetails(newDetails);
						oldDetails=null;
						newDetails=null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "processVinManualTypeMapping()", e);
		}
		finally
		{
			try {
				if(null!=conn)
					conn.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "processVinManualTypeMapping()", e);
			}
			conn = null;
		}
		return inputDataList;
	}

	private static VinMLMappingDetails retrieveManualTypeData(String locale,String carlineCode,String wmiCode,String vdsCode,String visStart,String visEnd,String modelName, Connection conn)
	{
		VinMLMappingDetails matrixData=new VinMLMappingDetails();
		PreparedStatement stmt = null;
		ResultSet  rs = null;
		try
		{
			matrixData.setLocaleCode(locale.trim().replace("-", "_"));
			matrixData.setCarlineCode(carlineCode.trim());
			matrixData.setWmiCode(wmiCode.trim());
			matrixData.setVdsCode(vdsCode.trim());
			matrixData.setVisStartRange(visStart.trim());
			matrixData.setVisEndRange(visEnd.trim());

			ArrayList<String> manualTypeList = new ArrayList<String>();
			String tableName = viewContentTable(locale);
			String sql="SELECT DISTINCT vc_vin_document_type FROM "+ tableName+" WHERE vc_vin_carline_code=? AND vc_vin_wmi_code=? AND vc_vin_vds_code=?"
					+ " AND vc_vin_vis_start_range=? AND vc_vin_vis_end_range=? AND vc_vin_model=?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, matrixData.getCarlineCode());
			stmt.setString(2, matrixData.getWmiCode());
			stmt.setString(3, matrixData.getVdsCode());
			stmt.setString(4, matrixData.getVisStartRange());
			stmt.setString(5, matrixData.getVisEndRange());
			stmt.setString(6, modelName.trim());
			rs = stmt.executeQuery();
			while(rs.next())
			{
				if(null!=rs.getString("VC_VIN_DOCUMENT_TYPE"))
				{
					if(null==manualTypeList || manualTypeList.size()<=0)
					{
						manualTypeList = new ArrayList<String>();
					}
					manualTypeList.add(rs.getString("VC_VIN_DOCUMENT_TYPE").trim());
				}
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
			sql = null;
			tableName = null;

			matrixData.setManualTypesList(new ArrayList<String>());
			if(null!=manualTypeList && manualTypeList.size()>0)
			{
				// set in Matrix DATA
				matrixData.setManualTypesList(manualTypeList);
				String manualType="";
				for(int a=0;a<manualTypeList.size();a++)
				{
					manualType=  manualTypeList.get(a).toString();
					// UPDATE THE SPECIFIC MANUAL TYPE LOCALES
					if(manualType.equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().replace(" ", "_")))
					{
						// WD
						matrixData.setWdMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_WD_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_WM_REF_KEY")))
					{
						// WM
						matrixData.setWmMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_WM_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_BSM_REF_KEY")))
					{
						// BSM
						matrixData.setBsmMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_BSM_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MC_REF_KEY")))
					{
						// MC
						matrixData.setMcMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_MC_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")))
					{
						// AT
						matrixData.setAtMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_AT_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")))
					{
						// MT
						matrixData.setMtMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_MT_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")))
					{
						// ENG
						matrixData.setEngineMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_ENG_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_SH_REF_KEY")))
					{
						// TG
						matrixData.setTgMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_TG_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_TQG_REF_KEY")))
					{
						// TQG
						matrixData.setTqgMappingLocale(locale);
						// COLUMN_NAME="VIN_ML_TQG_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY")))
					{
						// DM
						matrixData.setDmMappingLocale(locale);
//						COLUMN_NAME="VIN_ML_DM_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY")))
					{
						// MCM
						matrixData.setMcmMappingLocale(locale);
//						COLUMN_NAME="VIN_ML_MCM_MAPPING";
					}
					else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY")))
					{
						// RQ
						matrixData.setRqMappingLocale(locale);
//						COLUMN_NAME="VIN_ML_RQ_MAPPING";
					}
					manualType = null;
				}

			}
			manualTypeList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "retrieveManualTypeData()", e);
			// NOT READ IS NOT "NO MANUAL TYPE" - the caller would delete the mapping
			matrixData = null;
		}
		finally
		{
			try {
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "processVinManualTypeMapping()", e);
			}
			stmt=null;
			rs=null;
			locale=null;
			carlineCode=  null;
			wmiCode= null;
			vdsCode = null;
			visStart=  null;
			visEnd = null;
		}
		return matrixData;

	}

	private static ExcelRowDetails deleteVINManualTypeMapping(ExcelRowDetails vinDetails, VinMLMappingDetails mapDetails, String type, Connection conn)
	{
		PreparedStatement pstmt = null;
		try
		{
			String sql = "UPDATE gms3_mdm_vin_ml_mapping SET VIN_ML_STATUS='I',VIN_ML_CREATION_TMSTP=? WHERE TRIM(LOWER(VIN_ML_LOCALE))=? AND VIN_ML_CARLINE_CODE=? AND VIN_ML_WMI_CODE=? "
					+ " AND VIN_ML_VDS_CODE=? AND VIN_ML_VIS_START=? AND VIN_ML_VIS_END=? ";
			pstmt=conn.prepareStatement(sql);
			pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
			pstmt.setString(2, mapDetails.getLocaleCode().trim().toLowerCase());
			pstmt.setString(3, mapDetails.getCarlineCode());
			pstmt.setString(4, mapDetails.getWmiCode());
			pstmt.setString(5, mapDetails.getVdsCode());
			pstmt.setString(6, mapDetails.getVisStartRange());
			pstmt.setString(7, mapDetails.getVisEndRange());
			pstmt.executeUpdate();
			pstmt.close();pstmt=null;
			sql=null;

			// SET STATUS AS SUCCESS - unless the other VIN of the row failed already
			if(!AutomationConstants.STATUS_FAILURE.equals(vinDetails.getProcessingStatus()))
			{
				vinDetails.setProcessingStatus(AutomationConstants.STATUS_SUCCESS);
			}
		}
		catch(Exception  e)
		{
			// SET STATUS AS FAILURE
			vinDetails.setProcessingStatus(AutomationConstants.STATUS_FAILURE);
			// SET REMARKS
			vinDetails.setRemarks("Failed to Delete "+type+" VIN Reference from VIN MANUAL TYPE MAPPING DB TABLE.");
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "deleteVINManualTypeMapping()", e);
		}
		finally
		{
			try {
				if(null!=pstmt)
					pstmt.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "deleteVINManualTypeMapping()", e);
			}
			pstmt=null;
			type=null;
			mapDetails = null;
		}
		return vinDetails;
	}

	private static ExcelRowDetails saveVINManualTypeMapping(ExcelRowDetails vinDetails, VinMLMappingDetails mappingDetails, String type, Connection conn)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			String sql="";
			String manualType="";
			String COLUMN_NAME="";
			long autoCode=0;
			for(int a=0;a<mappingDetails.getManualTypesList().size();a++)
			{
				manualType=  mappingDetails.getManualTypesList().get(a).toString();
				// UPDATE THE SPECIFIC MANUAL TYPE LOCALES
				if(manualType.equals(ApplicationProperties.getProperty("wiring.diagram.folder.label").trim().replace(" ", "_")))
				{
					// WD
					COLUMN_NAME="VIN_ML_WD_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_WM_REF_KEY")))
				{
					// WM
					COLUMN_NAME="VIN_ML_WM_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_BSM_REF_KEY")))
				{
					// BSM
					COLUMN_NAME="VIN_ML_BSM_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MC_REF_KEY")))
				{
					// MC
					COLUMN_NAME="VIN_ML_MC_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY")))
				{
					// AT
					COLUMN_NAME="VIN_ML_AT_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY")))
				{
					// MT
					COLUMN_NAME="VIN_ML_MT_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY")))
				{
					// ENG
					COLUMN_NAME="VIN_ML_ENG_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_SH_REF_KEY")))
				{
					// TG
					COLUMN_NAME="VIN_ML_TG_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_TQG_REF_KEY")))
				{
					// TQG
					COLUMN_NAME="VIN_ML_TQG_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY")))
				{
					// DM
					COLUMN_NAME="VIN_ML_DM_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY")))
				{
					// MCM
					COLUMN_NAME="VIN_ML_MCM_MAPPING";
				}
				else if(manualType.equals(ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY")))
				{
					// RQ
					COLUMN_NAME="VIN_ML_RQ_MAPPING";
				}



				if(null==COLUMN_NAME || "".equals(COLUMN_NAME))
				{
					// a manual type without a mapping column - nothing to write for it (was an invalid UPDATE before)
					logger.info("saveVINManualTypeMapping :: manual type {"+manualType+"} has no VIN manual type mapping column - skipped.");
					manualType = null;
					continue;
				}
				// CHECK WHETHER TO UPDATE OR INSERT
				sql="SELECT VIN_ML_ID FROM gms3_mdm_vin_ml_mapping WHERE TRIM(LOWER(VIN_ML_LOCALE))=? AND VIN_ML_CARLINE_CODE=? AND VIN_ML_WMI_CODE=? "
						+ " AND VIN_ML_VDS_CODE=? AND VIN_ML_VIS_START=? AND VIN_ML_VIS_END=? AND VIN_ML_STATUS='A'";
				pstmt  =conn.prepareStatement(sql);
				pstmt.setString(1, mappingDetails.getLocaleCode().trim().toLowerCase());
				pstmt.setString(2, mappingDetails.getCarlineCode());
				pstmt.setString(3, mappingDetails.getWmiCode());
				pstmt.setString(4, mappingDetails.getVdsCode());
				pstmt.setString(5, mappingDetails.getVisStartRange());
				pstmt.setString(6, mappingDetails.getVisEndRange());
				rs= pstmt.executeQuery();
				if(rs.next())
				{
					autoCode = rs.getLong("VIN_ML_ID");
				}
				rs.close();rs=null;
				pstmt.close();pstmt=null;
				sql = null;

				if(autoCode > 0)
				{
					/*
					 * update status as well incase if set to Active as LOCALE for a COLUMN IS GETTING UPDATED.
					 * 4TH AUGUST 2019
					 */
					// vin already exists for the locale - simply update the new Manual Type Mapping
					sql="UPDATE gms3_mdm_vin_ml_mapping SET "+COLUMN_NAME+" = ? WHERE VIN_ML_ID = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, mappingDetails.getLocaleCode());
					pstmt.setLong(2, autoCode);
					pstmt.executeUpdate();
					pstmt.close();pstmt=null;
					sql = null;
				}
				else
				{
					// vin does not exist for the locale, create new entry for the new Manual Type Mapping
					sql="INSERT INTO gms3_mdm_vin_ml_mapping (VIN_ML_LOCALE, VIN_ML_CARLINE_CODE, VIN_ML_WMI_CODE, VIN_ML_VDS_CODE,"
							+ " VIN_ML_VIS_START, VIN_ML_VIS_END, "+COLUMN_NAME+",VIN_ML_STATUS, VIN_ML_CREATION_TMSTP) VALUES (?,?,?,?,?,?,?,?,?)";
					pstmt =conn.prepareStatement(sql);
					pstmt.setString(1, mappingDetails.getLocaleCode());
					pstmt.setString(2, mappingDetails.getCarlineCode());
					pstmt.setString(3, mappingDetails.getWmiCode());
					pstmt.setString(4, mappingDetails.getVdsCode());
					pstmt.setString(5, mappingDetails.getVisStartRange());
					pstmt.setString(6, mappingDetails.getVisEndRange());
					pstmt.setString(7, mappingDetails.getLocaleCode());
					pstmt.setString(8, "A");
					pstmt.setTimestamp(9, new Timestamp(new Date().getTime()));
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					sql = null;
				}
				manualType = null;
				sql= null;
				COLUMN_NAME=  null;
				autoCode= 0 ;
			}

			// SET STATUS AS SUCCESS - unless the other VIN of the row failed already
			if(!AutomationConstants.STATUS_FAILURE.equals(vinDetails.getProcessingStatus()))
			{
				vinDetails.setProcessingStatus(AutomationConstants.STATUS_SUCCESS);
			}

		}
		catch(Exception e)
		{
			// SET STATUS AS FAILURE
			vinDetails.setProcessingStatus(AutomationConstants.STATUS_FAILURE);
			// SET REMARKS
			vinDetails.setRemarks("Failed to Insert / Update "+type+" VIN Reference in VIN MANUAL TYPE MAPPING DB TABLE.");
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "saveVINManualTypeMapping()", e);
		}
		finally
		{
			try {
				if(null!=rs)
					rs.close();
				if(null!=pstmt)
					pstmt.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "saveVINManualTypeMapping()", e);
			}
			pstmt=null;
			rs = null;
			type=null;
			mappingDetails = null;
		}
		return vinDetails;
	}

	/* =====================================================================================================
	 * OLD VIN RANGES INACTIVE IN MDM (2026-10-12) - after a SUCCESSFUL update job
	 * =====================================================================================================
	 */
	public static final String OLD_VIN_DEACTIVATED = "DEACTIVATED";
	public static final String OLD_VIN_NO_CHANGE = "NO CHANGE";
	public static final String OLD_VIN_KEPT = "KEPT";
	public static final String OLD_VIN_FAILED = "FAILED";

	/**
	 * The OLD VIN ranges of a job that succeeded are made inactive in MDM, as a D row of a master data file does:
	 * mdm_vin_flag = flag.value.delete, mdm_sync_status cleared. The row is the one validation found - locale (through
	 * the alternate locale mapping, as at validation) + carline + WMI + VDS + VIS start / end, ACTIVE rows only. One
	 * UPDATE per distinct range. Nothing changes in Kapture (the old VIN category stays). A range that is also the NEW
	 * range of a row of the same job stays active.
	 *
	 * @return one line per distinct old range: { locale, MDM locale, carline, WMI, VDS, VIS start, VIS end, ref key,
	 *         action (DEACTIVATED / NO CHANGE / KEPT / FAILED), rows updated, remarks }
	 */
	public static List<String[]> deactivateOldVins(List<ExcelRowDetails> rows)
	{
		List<String[]> out = new ArrayList<String[]>();
		if(null==rows || rows.isEmpty())
		{
			return out;
		}
		// THE NEW RANGES OF THE JOB - never deactivated
		Set<String> newRanges = new HashSet<String>();
		for(ExcelRowDetails r : rows)
		{
			newRanges.add(nz(r.getLocale()).replace("_", "-").toUpperCase()+"|"+vinKey(r.getNewCarlineCode(), r.getNewWmiCode(), r.getNewVdsCode(),
					r.getNewVisStartRange(), r.getNewVisEndRange()));
		}
		Set<String> done = new HashSet<String>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			conn = getConnection();
			Timestamp now = new Timestamp(System.currentTimeMillis());
			for(ExcelRowDetails r : rows)
			{
				String locale = nz(r.getLocale()).replace("_", "-");
				String key = locale.toUpperCase()+"|"+vinKey(r.getOldCarlineCode(), r.getOldWmiCode(), r.getOldVdsCode(), r.getOldVisStartRange(), r.getOldVisEndRange());
				if(!done.add(key))
				{
					continue;
				}
				String[] line = new String[] { locale, "", nz(r.getOldCarlineCode()), nz(r.getOldWmiCode()), nz(r.getOldVdsCode()),
						nz(r.getOldVisStartRange()), nz(r.getOldVisEndRange()), nz(r.getOldRefKey()), "", "0", "" };
				out.add(line);
				if(newRanges.contains(key))
				{
					line[8] = OLD_VIN_KEPT;
					line[10] = "The range is also a NEW VIN range of this job.";
					continue;
				}
				try
				{
					String mdmLocale = ScheduleDAO.getAlternateLocale(locale, isLocaleOf("mnao.locales", locale)
							? AutoSyncConstants.ITEM_KEY_VIN_RANGE : AutoSyncConstants.ITEM_KEY_CARLINE, conn);
					if(null==mdmLocale || "".equals(mdmLocale.trim()))
					{
						throw new IllegalStateException("No MDM locale for "+locale+" (gms3_mdm_alt_lang_mapping).");
					}
					line[1] = mdmLocale.trim();
					pstmt = conn.prepareStatement("UPDATE gms3_mdm_vin_detail SET mdm_vin_flag=?, mdm_sync_status=NULL, mdm_vin_updated_tmstp=?"
							+ " WHERE mdm_vin_flag=? AND TRIM(LOWER(mdm_ml_lang_code))=? AND TRIM(LOWER(mdm_crln_code))=?"
							+ " AND TRIM(LOWER(mdm_vin_wmi_code))=? AND TRIM(LOWER(mdm_vin_vds_code))=?"
							+ " AND TRIM(LOWER(mdm_vin_vis_start_range))=? AND TRIM(LOWER(mdm_vin_vis_end_range))=?");
					int p = 1;
					pstmt.setString(p++, ApplicationProperties.getProperty("flag.value.delete"));
					pstmt.setTimestamp(p++, now);
					pstmt.setString(p++, ApplicationProperties.getProperty("flag.value.active"));
					pstmt.setString(p++, mdmLocale.trim().toLowerCase());
					pstmt.setString(p++, line[2].toLowerCase());
					pstmt.setString(p++, line[3].toLowerCase());
					pstmt.setString(p++, line[4].toLowerCase());
					pstmt.setString(p++, line[5].toLowerCase());
					pstmt.setString(p++, line[6].toLowerCase());
					int n = pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					line[9] = String.valueOf(n);
					line[8] = n>0 ? OLD_VIN_DEACTIVATED : OLD_VIN_NO_CHANGE;
					line[10] = n>0 ? "" : "No active row in MDM.";
					logger.info("deactivateOldVins :: "+locale+" (MDM "+mdmLocale.trim()+") "+line[7]+" :: "+line[8]+" :: rows :: > "+n);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "deactivateOldVins()", e);
					line[8] = OLD_VIN_FAILED;
					line[10] = null==e.getMessage() ? e.toString() : e.getMessage();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "deactivateOldVins()", e);
			out.add(new String[] { "-", "-", "-", "-", "-", "-", "-", "-", OLD_VIN_FAILED, "0",
					"Old VIN ranges not made inactive: "+(null==e.getMessage() ? e.toString() : e.getMessage()) });
		}
		finally
		{
			try
			{
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "deactivateOldVins()", e);
			}
		}
		return out;
	}
}