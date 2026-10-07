package com.mazda.gms3.dmt.automation.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class AutomationDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(AutomationDAO.class);

	public static ArrayList<ExcelRowDetails> validateExcelData(ArrayList<ExcelRowDetails> inputDataList)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn= null;
		try
		{
			if(null!=inputDataList && inputDataList.size()>0)
			{
				String sql="";
				String mdmLocaleCode="";
				String imLocaleCode="";
				String localeToCheck="";
				String modelName="";
				conn = getConnection();
				for(int a=0;a<inputDataList.size();a++)
				{
					ExcelRowDetails data = (ExcelRowDetails)inputDataList.get(a);

					mdmLocaleCode = data.getLocale();
					imLocaleCode = data.getLocale();

					// for MDM replace _ by -
					mdmLocaleCode = mdmLocaleCode.replace("_", "-");
					// for IM replace - by _
					imLocaleCode = imLocaleCode.replace("-", "_");

					// Locale Check - MDM
					try
					{
						// check Locale in MDM
						sql="SELECT MDM_ML_ID FROM gms3_mdm_manual_language WHERE TRIM(LOWER(MDM_ML_LANG_CODE))='"+mdmLocaleCode.trim().toLowerCase()+"' "
								+ " AND MDM_ML_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							data.setLocaleExistsInMDM(true);
						}
						rs.close();rs=null;
						stmt.close();stmt=null;
						sql=null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}

					// Locale Check - IM
					try
					{
						sql="SELECT RECORDID FROM OK_IM.LOCALE WHERE TRIM(LOWER(LOCALECODE))='"+imLocaleCode.trim().toLowerCase()+"' and ACTIVE='Y'";
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							data.setLocaleExistsInIM(true);
						}
						rs.close();rs=null;
						stmt.close();stmt=null;
						sql=null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}

					// OLD VIN IN MDM
					try
					{
						localeToCheck = data.getLocale();
						localeToCheck=  localeToCheck.replace("-", "_");
						modelName = checkVINExistsInMDM(mdmLocaleCode, data.getOldCarlineCode(), data.getOldWmiCode(), data.getOldVdsCode(), data.getOldVisStartRange(), data.getOldVisEndRange(), conn);
						if(null!=modelName && !"".equals(modelName))
						{
							if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(localeToCheck.trim().toLowerCase())>-1)
							{
								/*
								 * FOR MC Model Name will be Regional Value (for display) for identifying impacted documents
								 * and Eng Value will be different variable
								 */
								data.setOldModelName(modelName.substring(0,modelName.indexOf("<TOK_SEP>")));
								data.setOldModelNameEngForMC(modelName.substring(modelName.indexOf("<TOK_SEP>")+9, modelName.length()));
							}
							else
							{
								/*
								 * FOR MNAO & MME MODEL NAME WILL BE ENGLISH VALUE
								 * AND REGIONAL VALUE WILL BE DIFFERENT VARIABLE
								 */
								// english value
								data.setOldModelName(modelName.substring(modelName.indexOf("<TOK_SEP>")+9, modelName.length()));
								// regional value
								data.setOldModelNameRegForMNAOMME(modelName.substring(0,modelName.indexOf("<TOK_SEP>")));
							}
							
						}
						modelName =  null;
						localeToCheck = null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}

					if(null!=data.getOldModelName() && !"".equals(data.getOldModelName()))
					{
						data.setOldVinExistsinMDM(true);
					}

					// OLD VIN IN IM
					try
					{
						data.setOldVinExistsinIM(checkVINExistsInIM(data.getOldRefKey(), conn));
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}


					// NEW VIN IM MDM
					try
					{
//						data.setNewModelName(checkVINExistsInMDM(mdmLocaleCode, data.getNewCarlineCode(), data.getNewWmiCode(), data.getNewVdsCode(), data.getNewVisStartRange(), data.getNewVisEndRange(), conn));
						localeToCheck = data.getLocale();
						localeToCheck=  localeToCheck.replace("-", "_");
						modelName = checkVINExistsInMDM(mdmLocaleCode, data.getNewCarlineCode(), data.getNewWmiCode(), data.getNewVdsCode(), data.getNewVisStartRange(), data.getNewVisEndRange(), conn);
						if(null!=modelName && !"".equals(modelName))
						{
							if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(localeToCheck.trim().toLowerCase())>-1)
							{
								/*
								 * FOR MC Model Name will be Regional Value 
								 * and Eng Value will be different variable
								 */
								data.setNewModelName(modelName.substring(0,modelName.indexOf("<TOK_SEP>")));
								data.setNewModelNameEngForMC(modelName.substring(modelName.indexOf("<TOK_SEP>")+9, modelName.length()));
							}
							else
							{
								/*
								 * FOR MNAO & MME MODEL NAME WILL BE ENGLISH VALUE
								 * AND REGIONAL VALUE WILL BE DIFFERENT VARIABLE
								 */
								// english value
								data.setNewModelName(modelName.substring(modelName.indexOf("<TOK_SEP>")+9, modelName.length()));
								// regional value
								data.setNewModelNameRegForMNAOMME(modelName.substring(0,modelName.indexOf("<TOK_SEP>")));
							}
						}
						modelName =  null;
						localeToCheck=  null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}

					if(null!=data.getNewModelName() && !"".equals(data.getNewModelName()))
					{
						data.setNewVinExistsinMDM(true);
					}

					// NEW VIN IN IM
					try
					{
						data.setNewVinExistsinIM(checkVINExistsInIM(data.getNewRefKey(), conn));
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
					}

					mdmLocaleCode = null;
					imLocaleCode=null;
				}
				sql=null;
				mdmLocaleCode = null;
				imLocaleCode=null;
			}
			else
			{
				logger.info("validateExcelData ::  InputDataList as parameter is null. return null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
				if(null!=conn)
					conn.close();
				rs=null;
				stmt=null;
				conn = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "validateExcelData()", e);
			}
		}
		return inputDataList;
	}

	public static ArrayList<String> getSubTypesList(String referenceKey) 
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn= null;
		ArrayList<String> childsList = null;
		try
		{
			String objectId="";
			conn = getConnection();
			String sql="SELECT OBJECTID FROM OK_IM.TAG WHERE REFERENCEKEY='"+referenceKey.trim()+"'";
			//			logger.info("getSubTypesList ::  getParentObjectId :: Sql :: > "+ sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				objectId = rs.getString("OBJECTID");
			}
			stmt.close();stmt=null;
			rs.close();rs=null;
			sql=null;
			if(null!=objectId && !"".equals(objectId))
			{
				// fetch all immediate childs
				objectId = objectId.trim()+".";
				sql = "SELECT REFERENCEKEY, OBJECTID FROM OK_IM.TAG WHERE OBJECTID LIKE '"+objectId+"%'";
				//				logger.info("getSubTypesList ::  getChildRefKeys :: Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				String refKey="";
				String childObjectId="";
				while(rs.next())
				{
					refKey = rs.getString("REFERENCEKEY");
					childObjectId = rs.getString("OBJECTID");
					if(null!=childObjectId && !"".equals(childObjectId))
					{
						if(null!=childObjectId.split("\\.") && childObjectId.split("\\.").length==2)
						{
							if(null==childsList || childsList.size()<=0)
							{
								childsList = new ArrayList<String>();
							}
							// sub child add it
							childsList.add(refKey);
						}
					}
					refKey = null;
					childObjectId = null;
				}
				refKey = null;
				childObjectId = null;
				sql=null;
				stmt.close();stmt=null;
				rs.close();rs=null;

				if(null!=childsList && childsList.size()>0)
				{
					logger.info("getSubTypesList :: Total Childs found for {"+referenceKey+"} are :: >"+ childsList.size());
				}
				else
				{
					logger.info("getSubTypesList :: No Childs found for {"+referenceKey+"}.");
				}
			}
			else
			{
				logger.info("getSubTypesList ::  Failed to fetch Object Id for Parent Reference Key :: > "+ referenceKey);
			}
			objectId = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getSubTypesList()", e);
		}
		finally
		{
			try
			{
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
				if(null!=conn)
					conn.close();
				rs=null;
				stmt=null;
				conn = null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getSubTypesList()", e);
			}
		}
		return childsList;
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

	private static ArrayList<Map<String, Object>> getImpactedDocumentsData_OLD(ArrayList<ExcelRowDetails> applicableVINList, String documentPrefix, String locale, Connection conn)
	{
		ArrayList<Map<String, Object>> impactedDocumentsList = null;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			impactedDocumentsList = new ArrayList<Map<String,Object>>();
			ExcelRowDetails data = new ExcelRowDetails();
			String sql="";
			Map<String, Object> documentMap = null;
			ArrayList<ExcelRowDetails> documentsVINList = null;
			ExcelRowDetails existData = new ExcelRowDetails();
			for(int a=0;a<applicableVINList.size();a++)
			{
				data = (ExcelRowDetails)applicableVINList.get(a);
				if(null!=data.getOldRefKey() && !"".equals(data.getOldRefKey()))
				{
					sql="SELECT DISTINCT A.DOCUMENTID FROM OK_IM.CONTENTTEXT A, OK_IM.CONTENTTEXTCATEGORY B, "
							+ " OK_IM.TAG C WHERE A.RECORDID = B.CONTENTTEXTID AND B.TAGID=C.RECORDID "
							+ " AND C.REFERENCEKEY='"+data.getOldRefKey()+"' AND A.LOCALEID='"+locale+"' AND A.LATESTVERSION='Y' "
							+ " AND A.DOCUMENTID LIKE '"+documentPrefix+"%'";
					logger.info("getImpactedDocumentsData :: Sql :: >"+ sql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next())
					{
						if(null!=rs.getString("DOCUMENTID") && !"".equals(rs.getString("DOCUMENTID")))
						{
							boolean add = true;
							if(null!=impactedDocumentsList && impactedDocumentsList.size()>0)
							{
								documentMap = new HashMap<String, Object>();
								for(int y=0;y<impactedDocumentsList.size();y++)
								{
									documentMap = (Map<String, Object>)impactedDocumentsList.get(y);
									if(null!=documentMap && null!=documentMap.get(AutomationConstants.DOCUMENTID))
									{
										if(String.valueOf(documentMap.get(AutomationConstants.DOCUMENTID)).trim().equals(rs.getString("DOCUMENTID").trim()))
										{
											// already exists
											add = false;

											/*
											 * CHECK HERE, IF THE VIN IS ADDED WITH  THE DOCUMENT OR NOT
											 * IF YES - SKIP IT
											 * IF NO  - ADD TO VINS LIST OF DOCUMENT
											 */
											boolean addVin=true;
											if(null!=documentMap.get(AutomationConstants.VIN_LIST))
											{
												documentsVINList = (ArrayList<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST);
												if(null!=documentsVINList && documentsVINList.size()>0)
												{
													existData = new ExcelRowDetails();
													for(int hg=0;hg<documentsVINList.size();hg++)
													{
														existData = (ExcelRowDetails)documentsVINList.get(hg);
														if(null!=existData.getOldRefKey() && !"".equals(existData.getOldRefKey()))
														{
															if(existData.getOldRefKey().trim().toLowerCase().equals(data.getOldRefKey().trim().toLowerCase()))
															{
																// VIN IS ALREADY ADDED TO DOCUMENT - DO NOT ADD
																addVin = false;
																break;
															}
														}
													}
												}
											}

											if(addVin==true)
											{
												if(null==documentsVINList || documentsVINList.size()<=0)
												{
													documentsVINList = new ArrayList<ExcelRowDetails>();
												}
												documentsVINList.add(data);
												// set in documentMap
												documentMap.put(AutomationConstants.VIN_LIST, documentsVINList);
											}
											documentsVINList = null;

											break;
										}
									}
									documentMap = null;
								}
								documentMap = null;
							}

							if(add == true)
							{
								documentMap = new HashMap<String, Object>();
								documentMap.put(AutomationConstants.DOCUMENTID, rs.getString("DOCUMENTID"));

								/*
								 * HERE ADD THE VIN WITH THE DOCUMENT ID
								 * SO THAT WHEN REPORT IS PRINTED OR DOCUMENT NEEDS TO BE IDENTIFIED
								 * IT CONTAINS APPLICABLE VINS FOR IT
								 */
								documentsVINList = new ArrayList<ExcelRowDetails>();
								documentsVINList.add(data);
								documentMap.put(AutomationConstants.VIN_LIST, documentsVINList);
								impactedDocumentsList.add(documentMap);
								documentMap = null;
								documentsVINList = null;
							}
						}
					}
					rs.close();rs=null;
					stmt.close();stmt=null;
					sql=null;
				}
				data = null;
			}
			sql = null;
			documentMap = null;
			rs = null;
			stmt =null;

			if(null!=impactedDocumentsList && impactedDocumentsList.size()>0)
			{
				logger.info("getImpactedDocumentsData :: Total Impacted Documents Found are :: >"+ impactedDocumentsList.size());
			}
			else
			{
				logger.info("getImpactedDocumentsData :: No Impacted Documents Found.");
			}

			/*
			 * FOR ALL THE IDENTIFIED IMPACTED DOCUMENTS LIST FOR CHANNEL TYPE IF SM / SI 
			 * IDENTIFY DOCUMENT TYPES AS WELL
			 */
			if(null!=impactedDocumentsList && impactedDocumentsList.size()>0 && (documentPrefix.equals("SM") || documentPrefix.equals("SI")))
			{
				logger.info("getImpactedDocumentsData :: Proceed for fetching Document Types for "+documentPrefix+" Documents.");
				String refKey = "";
				String objectId="";
				if(documentPrefix.equals("SM"))
				{
					refKey = AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE;
				}
				else if(documentPrefix.equals("SI"))
				{
					refKey = AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE;
				}
				else if(documentPrefix.equals("OSM"))
				{
					refKey = AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE;
				}

				// getObjectId for chanelRefKey and for each document identify the firstLevel Type
				sql="SELECT OBJECTID FROM OK_IM.TAG WHERE REFERENCEKEY='"+refKey+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					objectId = rs.getString("OBJECTID");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				sql = null;

				if(null!=objectId && !"".equals(objectId))
				{
					documentMap = new HashMap<String, Object>();
					String tempObjectId=null;
					String tempRefKey = null;
					for(int a=0;a<impactedDocumentsList.size();a++)
					{
						documentMap = (Map<String, Object>)impactedDocumentsList.get(a);
						if(null!=documentMap && null!=documentMap.get(AutomationConstants.DOCUMENTID) && !"".equals(documentMap.get(AutomationConstants.DOCUMENTID)))
						{
							sql="SELECT A.REFERENCEKEY, A.OBJECTID FROM OK_IM.TAG A, OK_IM.CONTENTTEXTCATEGORY B, OK_IM.CONTENTTEXT C WHERE "
									+ " A.RECORDID = B.TAGID AND B.CONTENTTEXTID = C.RECORDID AND C.LOCALEID='"+locale+"' AND C.LATESTVERSION='Y' AND "
									+ " C.DOCUMENTID='"+documentMap.get(AutomationConstants.DOCUMENTID)+"' "
									+ " AND A.OBJECTID LIKE '"+objectId+".%'";
							//							logger.info("---------------- getDocumentType :: Sql :: > "+ sql);
							stmt = conn.createStatement();
							rs = stmt.executeQuery(sql);
							if(rs.next())
							{
								tempObjectId = rs.getString("OBJECTID");
								tempRefKey = rs.getString("REFERENCEKEY");
							}
							stmt.close();stmt=null;
							rs.close();rs=null;
							sql = null;

							// split objectId in array and check if count is 2, then map identified refKey, if 3 - fetch its parent refKey
							if(null!=tempObjectId && !"".equals(tempObjectId) && null!=tempRefKey && !"".equals(tempRefKey))
							{
								if(null!=tempObjectId.split("\\.") && tempObjectId.split("\\.").length==2)
								{
									documentMap.put(AutomationConstants.REFERENCEKEY, tempRefKey);
								}
								else if(null!=tempObjectId.split("\\.") && tempObjectId.split("\\.").length==3)
								{
									// fetch parent refKey
									sql="SELECT REFERENCEKEY FROM OK_IM.TAG WHERE OBJECTID='"+tempObjectId.split("\\.")[0]+"."+tempObjectId.split("\\.")[1]+"'";
									//									logger.info("---------------- getParentDocumentType :: Sql :: > "+ sql);
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									if(rs.next())
									{
										if(null!=rs.getString("REFERENCEKEY") && !"".equals(rs.getString("REFERENCEKEY")))
										{
											documentMap.put(AutomationConstants.REFERENCEKEY, rs.getString("REFERENCEKEY"));
										}
									}
									rs.close();rs=null;
									stmt.close();stmt=null;
									sql = null;
								}
							}
							tempObjectId = null;
							tempRefKey = null;
						}
					}

					documentMap = null;
				}
				else
				{
					logger.info("getImpactedDocumentsData :: Failed to Identify Object Id for REFKEY :: >"+ refKey);
				}
				objectId = null;
				refKey = null;

			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsData()", e);
		}
		return impactedDocumentsList;
	}
	
	private static ArrayList<Map<String, Object>> getImpactedDocumentsData(ArrayList<ExcelRowDetails> applicableVINList, String documentPrefix, String locale, Connection conn)
	{
		ArrayList<Map<String, Object>> impactedDocumentsList = null;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			impactedDocumentsList = new ArrayList<Map<String,Object>>();
			ExcelRowDetails data = new ExcelRowDetails();
			String sql="";
			Map<String, Object> documentMap = null;
			ArrayList<ExcelRowDetails> documentsVINList = null;
			ExcelRowDetails existData = new ExcelRowDetails();
			for(int a=0;a<applicableVINList.size();a++)
			{
				data = (ExcelRowDetails)applicableVINList.get(a);
				if(null!=data.getOldRefKey() && !"".equals(data.getOldRefKey()))
				{
					try
					{
						if(ApplicationProperties.getProperty("mnao.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase())>-1)
						{
							// MNAO LOCALE
							sql="SELECT DISTINCT VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE FROM gms3_vc_vin_details WHERE VC_VIN_LOCALE='"+locale+"' AND "
									+ " VC_VIN_WMI_CODE='"+data.getOldWmiCode()+"' AND VC_VIN_VDS_CODE='"+data.getOldVdsCode()+"' "
									+ " AND VC_VIN_VIS_START_RANGE='"+data.getOldVisStartRange()+"' AND VC_VIN_VIS_END_RANGE='"+data.getOldVisEndRange()+"' AND VC_VIN_DOCUMENT_ID LIKE '"+documentPrefix+"%'";
						}
						else if(ApplicationProperties.getProperty("mc.locales").trim().toLowerCase().indexOf(locale.trim().toLowerCase())>-1)
						{
							// MC LOCALE
//							sql="SELECT DISTINCT VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE FROM gms3_vc_japan_vin_details WHERE VC_VIN_LOCALE='"+locale+"' AND "
//									+ " VC_VIN_WMI_CODE='"+data.getOldWmiCode()+"' AND VC_VIN_VDS_CODE='"+data.getOldVdsCode()+"' "
//									+ " AND VC_VIN_VIS_START_RANGE='"+data.getOldVisStartRange()+"' AND VC_VIN_VIS_END_RANGE='"+data.getOldVisEndRange()+"' AND "
//									+ " VC_VIN_CARLINE_CODE='"+data.getOldCarlineCode()+"' AND VC_VIN_MODEL='"+data.getOldModelNameEngForMC()+"' AND VC_VIN_DOCUMENT_ID LIKE '"+documentPrefix+"%'";
							// REMOVE MODEL NAME FROM WHERE CLAUSE - 11 NOVEMBER 2022
							sql="SELECT DISTINCT VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE FROM gms3_vc_japan_vin_details WHERE VC_VIN_LOCALE='"+locale+"' AND "
									+ " VC_VIN_WMI_CODE='"+data.getOldWmiCode()+"' AND VC_VIN_VDS_CODE='"+data.getOldVdsCode()+"' "
									+ " AND VC_VIN_VIS_START_RANGE='"+data.getOldVisStartRange()+"' AND VC_VIN_VIS_END_RANGE='"+data.getOldVisEndRange()+"' AND "
									+ " VC_VIN_CARLINE_CODE='"+data.getOldCarlineCode()+"' AND VC_VIN_DOCUMENT_ID LIKE '"+documentPrefix+"%'";
							
						}
						else
						{
							// MME LOCALE
//							sql="SELECT DISTINCT VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE FROM GMS3_VC_MME_VIN_DTL_"+locale.trim().toUpperCase()+" WHERE VC_VIN_LOCALE='"+locale+"' AND "
//									+ " VC_VIN_WMI_CODE='"+data.getOldWmiCode()+"' AND VC_VIN_VDS_CODE='"+data.getOldVdsCode()+"' "
//									+ " AND VC_VIN_VIS_START_RANGE='"+data.getOldVisStartRange()+"' AND VC_VIN_VIS_END_RANGE='"+data.getOldVisEndRange()+"' AND "
//									+ " VC_VIN_CARLINE_CODE='"+data.getOldCarlineCode()+"' AND VC_VIN_MODEL='"+data.getOldModelName()+"' AND VC_VIN_DOCUMENT_ID LIKE '"+documentPrefix+"%'";
							// REMOVE MODEL NAME FROM WHERE CLAUSE - 11 NOVEMBER 2022
							sql="SELECT DISTINCT VC_VIN_DOCUMENT_ID, VC_VIN_DOCUMENT_TYPE FROM GMS3_VC_MME_VIN_DTL_"+locale.trim().toUpperCase()+" WHERE VC_VIN_LOCALE='"+locale+"' AND "
									+ " VC_VIN_WMI_CODE='"+data.getOldWmiCode()+"' AND VC_VIN_VDS_CODE='"+data.getOldVdsCode()+"' "
									+ " AND VC_VIN_VIS_START_RANGE='"+data.getOldVisStartRange()+"' AND VC_VIN_VIS_END_RANGE='"+data.getOldVisEndRange()+"' AND "
									+ " VC_VIN_CARLINE_CODE='"+data.getOldCarlineCode()+"' AND VC_VIN_DOCUMENT_ID LIKE '"+documentPrefix+"%'";
						}
						logger.info("getImpactedDocumentsData :: Sql :: >"+ sql);
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						while(rs.next())
						{
							if(null!=rs.getString("VC_VIN_DOCUMENT_ID") && !"".equals(rs.getString("VC_VIN_DOCUMENT_ID")))
							{
//								if(rs.getString("VC_VIN_DOCUMENT_ID").startsWith(documentPrefix))
								{
									boolean add = true;
									if(null!=impactedDocumentsList && impactedDocumentsList.size()>0)
									{
										documentMap = new HashMap<String, Object>();
										for(int y=0;y<impactedDocumentsList.size();y++)
										{
											documentMap = (Map<String, Object>)impactedDocumentsList.get(y);
											if(null!=documentMap && null!=documentMap.get(AutomationConstants.DOCUMENTID))
											{
												if(String.valueOf(documentMap.get(AutomationConstants.DOCUMENTID)).trim().equals(rs.getString("VC_VIN_DOCUMENT_ID").trim()))
												{
													// already exists
													add = false;

													/*
													 * CHECK HERE, IF THE VIN IS ADDED WITH  THE DOCUMENT OR NOT
													 * IF YES - SKIP IT
													 * IF NO  - ADD TO VINS LIST OF DOCUMENT
													 */
													boolean addVin=true;
													if(null!=documentMap.get(AutomationConstants.VIN_LIST))
													{
														documentsVINList = (ArrayList<ExcelRowDetails>)documentMap.get(AutomationConstants.VIN_LIST);
														if(null!=documentsVINList && documentsVINList.size()>0)
														{
															existData = new ExcelRowDetails();
															for(int hg=0;hg<documentsVINList.size();hg++)
															{
																existData = (ExcelRowDetails)documentsVINList.get(hg);
																if(null!=existData.getOldRefKey() && !"".equals(existData.getOldRefKey()))
																{
																	if(existData.getOldRefKey().trim().toLowerCase().equals(data.getOldRefKey().trim().toLowerCase()))
																	{
																		// VIN IS ALREADY ADDED TO DOCUMENT - DO NOT ADD
																		addVin = false;
																		break;
																	}
																}
															}
														}
													}

													if(addVin==true)
													{
														if(null==documentsVINList || documentsVINList.size()<=0)
														{
															documentsVINList = new ArrayList<ExcelRowDetails>();
														}
														documentsVINList.add(data);
														// set in documentMap
														documentMap.put(AutomationConstants.VIN_LIST, documentsVINList);
													}
													documentsVINList = null;

													break;
												}
											}
											documentMap = null;
										}
										documentMap = null;
									}
									
									if(add == true)
									{
										documentMap = new HashMap<String, Object>();
										documentMap.put(AutomationConstants.DOCUMENTID, rs.getString("VC_VIN_DOCUMENT_ID"));
										// add document type as well - do this only for SM / OSM / SI
										if(documentPrefix.equals("SM") || documentPrefix.equals("OSM") || documentPrefix.equals("SI"))
										{
											documentMap.put(AutomationConstants.REFERENCEKEY, rs.getString("VC_VIN_DOCUMENT_TYPE"));
										}

										/*
										 * HERE ADD THE VIN WITH THE DOCUMENT ID
										 * SO THAT WHEN REPORT IS PRINTED OR DOCUMENT NEEDS TO BE IDENTIFIED
										 * IT CONTAINS APPLICABLE VINS FOR IT
										 */
										documentsVINList = new ArrayList<ExcelRowDetails>();
										documentsVINList.add(data);
										documentMap.put(AutomationConstants.VIN_LIST, documentsVINList);
										impactedDocumentsList.add(documentMap);
										documentMap = null;
										documentsVINList = null;
									}
								}
							}
						}
						rs.close();rs=null;
						stmt.close();stmt = null;
						sql = null;
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsData()", e);
					}
				}
				data = null;
			}
			sql = null;
			documentMap = null;
			rs = null;
			stmt =null;

			if(null!=impactedDocumentsList && impactedDocumentsList.size()>0)
			{
				logger.info("getImpactedDocumentsData :: Total Impacted Documents Found are :: >"+ impactedDocumentsList.size());
			}
			else
			{
				logger.info("getImpactedDocumentsData :: No Impacted Documents Found.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "getImpactedDocumentsData()", e);
		}
		return impactedDocumentsList;
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

	private static boolean checkVINExistsInIM(String refKey, Connection conn)
	{
		boolean bool = false;
		Statement stmt=null;
		ResultSet rs = null;
		try
		{
			String sql="";
			sql= "SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY='"+refKey+"'";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
				{
					// VIN REFKEY EXISTS IN IM.
					bool = true;
				}
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "checkVINExistsInIM()", e);
			bool = false;
		}
		finally
		{
			try {
				if(null!=rs)
					rs.close();
				if(null!=stmt)
					stmt.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "checkVINExistsInIM()", e);
			}
			stmt=null;
			rs=null;
		}
		return bool;
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
					if(ApplicationProperties.getProperty("mme.locales").trim().toLowerCase().indexOf(vinDetails.getLocale().replace("-", "_").trim().toLowerCase())>-1)
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
		Statement stmt = null;
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
			String tableName = "GMS3_VC_MME_VIN_DTL_"+locale.trim().toUpperCase();
			String sql="SELECT DISTINCT VC_VIN_DOCUMENT_TYPE FROM "+ tableName+" WHERE VC_VIN_CARLINE_CODE='"+matrixData.getCarlineCode()+"' "
					+ " AND VC_VIN_WMI_CODE='"+matrixData.getWmiCode()+"' AND  VC_VIN_VDS_CODE='"+matrixData.getVdsCode()+"' "
					+ " AND VC_VIN_VIS_START_RANGE='"+matrixData.getVisStartRange()+"' "
					+ " AND VC_VIN_VIS_END_RANGE='"+matrixData.getVisEndRange()+"' AND VC_VIN_MODEL='"+modelName.trim()+"'";
			logger.info("retrieveManualTypeData :: SQL :: > "+ sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
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
			Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "processVinManualTypeMapping()", e);
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

			// SET STATUS AS SUCCESS
			vinDetails.setProcessingStatus(AutomationConstants.STATUS_SUCCESS);
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
		Statement stmt = null;
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
					sql="UPDATE gms3_mdm_vin_ml_mapping SET "+COLUMN_NAME+" = '"+mappingDetails.getLocaleCode()+"' WHERE VIN_ML_ID="+ autoCode;
					stmt = conn.createStatement();
					stmt.executeUpdate(sql);
					stmt.close();stmt=null;
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

			// SET STATUS AS SUCCESS
			vinDetails.setProcessingStatus(AutomationConstants.STATUS_SUCCESS);

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
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			} catch (SQLException e) {
				Utilities.printStackTraceToLogs(AutomationDAO.class.getName(), "saveVINManualTypeMapping()", e);
			}
			stmt=null;
			rs = null;
			type=null;
			mappingDetails = null;
		}
		return vinDetails;
	}
}