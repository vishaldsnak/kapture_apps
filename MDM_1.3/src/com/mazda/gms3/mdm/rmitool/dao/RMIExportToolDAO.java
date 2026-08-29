package com.mazda.gms3.mdm.rmitool.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolItemDetails;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolScheduleDetails;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class RMIExportToolDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(RMIExportToolDAO.class);

	public static ArrayList<RMIExportToolItemDetails> getDocumentsMatrixList(List<String> localesList, List<CarlineDetails> modelsList)
	{
		ArrayList<RMIExportToolItemDetails> itemsList = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=localesList && localesList.size()>0 && null!=modelsList && modelsList.size()>0)
			{
				/*
				 * ITERATE LOCALE LOOP FOR ALL MODELS
				 */
				String locale="";
				String model = "";
				String carlineCode="";
				conn = getConnection();
				CarlineDetails clDetails = null;
				for(int t=0;t<localesList.size();t++)
				{
					locale = null;
					locale = localesList.get(t);
					locale = locale.replace("-", "_");
					for(int r=0;r<modelsList.size();r++)
					{
						clDetails = (CarlineDetails)modelsList.get(r);
						model  =clDetails.getCarlineNameEng();
						carlineCode = clDetails.getCarlineCode();
						
						model = model.trim();
						carlineCode = carlineCode.trim();
						
						/*
						 * FETCH DATA FOR ALL CHANNELS
						 * SI , VI, TR, SM, WD
						 */
						String[] tok = "SI,VI,TR".split(",");
						String sql = "";
						ArrayList<RMIExportToolItemDetails> tempList = new ArrayList<RMIExportToolItemDetails>();
						RMIExportToolItemDetails details = null;
						if(null!=tok && tok.length>0)
						{
							for(int a=0;a<tok.length;a++)
							{
								/*
								 * FETCH IT ON CARLINE CODE AND LOCALE ONLY 
								 * NO NEED OF USING MODEL NAME - AS CARLINE CODE IS AWLAYS UNIQUE
								 * AS PER DISCUSSION WITH GAURAV ON 05 MARCH 2020
								 */
//								sql = "SELECT DISTINCT vc_vin_document_id, vc_vin_document_type, vc_vin_channel_name FROM gms3_vc_mme_vin_si_detail WHERE vc_vin_document_id LIKE '"+tok[a]+"%' "
//										+ " AND  vc_vin_model='"+model+"' AND vc_vin_locale='"+locale+"' AND vc_vin_carline_code='"+carlineCode+"' "
//										+ " AND vc_vin_inc_ug_mapping='Y'";
								sql = "SELECT DISTINCT vc_vin_document_id, vc_vin_document_type, vc_vin_channel_name FROM gms3_vc_mme_vin_si_detail WHERE vc_vin_document_id LIKE '"+tok[a]+"%' "
										+ " AND  vc_vin_locale='"+locale+"' AND vc_vin_carline_code='"+carlineCode+"' "
										+ " AND vc_vin_inc_ug_mapping='Y'";
								logger.info("getDocumentsMatrixList :: sql :: >"+ sql);
								stmt = conn.createStatement();
								rs = stmt.executeQuery(sql);
								while(rs.next())
								{
									details = new RMIExportToolItemDetails();
									details.setLocale(locale);
									details.setModel(model);
									details.setCarlineCode(carlineCode);
									details.setDocumentType(rs.getString("vc_vin_document_type"));
									details.setDocumentId(rs.getString("vc_vin_document_id"));
									details.setChannelName(rs.getString("vc_vin_channel_name"));
									
									if(null!=details.getChannelName() && ("VIDEOS".equals(details.getChannelName()) || "TRAINING".equals(details.getChannelName())))
									{
										// set document Type as null FOR VIDEOS & TRAINING
										details.setDocumentType(null);
									}
									tempList.add(details);
									details = null;
								}
								stmt.close();stmt = null;
								rs.close();rs = null;
								sql = null;
								details = null;
							}
						}

						/*
						 * PREPARE UNIQUE & DOCUMENTS COUNT DATA FOR SI, VI & TR CHANNEL
						 */
						if(null!=tempList && tempList.size()>0)
						{
							logger.info("getDocumentsMatrixList :: Total rows fetched are :: > "+ tempList.size());
							details = null;
							RMIExportToolItemDetails existingDetails = null;
							String checkKey = null;
							String existingKey = null;
							boolean add = true;
							for(int a=0;a<tempList.size();a++)
							{
								details = (RMIExportToolItemDetails)tempList.get(a);
								add = true;
								checkKey = "";
								if(null!=details.getLocale() && !"".equals(details.getLocale()))
								{
									checkKey+=details.getLocale();
								}
								if(null!=details.getModel() && !"".equals(details.getModel()))
								{
									checkKey+=details.getModel();
								}
								if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
								{
									checkKey+=details.getCarlineCode();
								}
								if(null!=details.getDocumentType() && !"".equals(details.getDocumentType()))
								{
									checkKey+=details.getDocumentType();
								}
								if(null!=details.getChannelName() && !"".equals(details.getChannelName()))
								{
									checkKey+=details.getChannelName();
								}


								if(null!=itemsList && itemsList.size()>0)
								{
									existingDetails = new RMIExportToolItemDetails();
									for(int b=0;b<itemsList.size();b++)
									{
										existingDetails = (RMIExportToolItemDetails) itemsList.get(b);
										existingKey = "";
										if(null!=existingDetails.getLocale() && !"".equals(existingDetails.getLocale()))
										{
											existingKey+=existingDetails.getLocale();
										}
										if(null!=existingDetails.getModel() && !"".equals(existingDetails.getModel()))
										{
											existingKey+=existingDetails.getModel();
										}
										if(null!=existingDetails.getCarlineCode() && !"".equals(existingDetails.getCarlineCode()))
										{
											existingKey+=existingDetails.getCarlineCode();
										}
										if(null!=existingDetails.getDocumentType() && !"".equals(existingDetails.getDocumentType()))
										{
											existingKey+=existingDetails.getDocumentType();
										}
										if(null!=existingDetails.getChannelName() && !"".equals(existingDetails.getChannelName()))
										{
											existingKey+=existingDetails.getChannelName();
										}

										if(checkKey.trim().toLowerCase().equals(existingKey.trim().toLowerCase()))
										{
											// item already added =- just increment the document count
											existingDetails.setDocumentsCounts(existingDetails.getDocumentsCounts()+1);
											// just add document to existingDetails Document Ids List
											if(null==existingDetails.getDocumentIdsList() || existingDetails.getDocumentIdsList().size()<=0)
											{
												existingDetails.setDocumentIdsList(new ArrayList<String>());
											}
											existingDetails.getDocumentIdsList().add(details.getDocumentId());
											add = false;
											break;
										}
										existingKey = null;
									}
								}

								if(add ==true)
								{
									// increment document count by 1 - as the item is getting added for the firstTime.
									details.setDocumentsCounts(details.getDocumentsCounts()+1);
									// just add document to details Document Ids List
									if(null==details.getDocumentIdsList() || details.getDocumentIdsList().size()<=0)
									{
										details.setDocumentIdsList(new ArrayList<String>());
									}
									details.getDocumentIdsList().add(details.getDocumentId());
									
									if(null==itemsList || itemsList.size()<=0)
									{
										itemsList = new ArrayList<RMIExportToolItemDetails>();
									}
									itemsList.add(details);
								}

								checkKey = null;
								details = null;
							}
						}
						else
						{
							logger.info("getDocumentsMatrixList :: No Rows fetched for Channels :: SI / VI / TR. For Locale :: > "+ locale);
						}

						tempList = null;
						tok = null;
						/*
						 * Now fetch data for OSM, SM & WD
						 */
						String mmeManualLocales="en_UK,cs_CZ,pl_PL,sv_SE,de_DE,fi_FI,pt_PT,tr_TR,fr_FR,el_GR,ru_RU,nl_NL,it_IT,es_ES";
						/*
						 * PROCEED ONLY WHEN ABOVE LOCALE IS SELECTED
						 */
						if(mmeManualLocales.indexOf(locale)!=-1)
						{
							tok ="SM,OSM,WD".split(",");
							String tableName="gms3_vc_mme_vin_dtl_"+Utilities.tableLocale(locale);
							if(null!=tok && tok.length>0)
							{
								tempList = new ArrayList<RMIExportToolItemDetails>();
								details = null;
								RMIExportToolItemDetails existingDetails = null;
								String checkKey = null;
								String existingKey = null;
								for(int a=0;a<tok.length;a++)
								{
									/*
									 * FETCH IT ON CARLINE CODE AND LOCALE ONLY 
									 * NO NEED OF USING MODEL NAME - AS CARLINE CODE IS AWLAYS UNIQUE
									 * AS PER DISCUSSION WITH GAURAV ON 05 MARCH 2020
									 */
//									sql = "SELECT DISTINCT vc_vin_document_id, vc_vin_document_type FROM "+ tableName+" WHERE vc_vin_document_id LIKE '"+tok[a]+"%'  "
//											+ " AND  vc_vin_model='"+model+"' AND vc_vin_locale='"+locale+"' AND vc_vin_carline_code='"+carlineCode+"'";
									sql = "SELECT DISTINCT vc_vin_document_id, vc_vin_document_type FROM "+ tableName+" WHERE vc_vin_document_id LIKE '"+tok[a]+"%'  "
											+ " AND  vc_vin_locale='"+locale+"' AND vc_vin_carline_code='"+carlineCode+"'  ";
									logger.info("getDocumentsMatrixList :: Sql :: > "+ sql);
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									while(rs.next())
									{
										details = new RMIExportToolItemDetails();
										details.setLocale(locale);
										details.setCarlineCode(carlineCode);
										details.setModel(model);
										if(tok[a].equals("SM"))
										{
											details.setChannelName("SERVICE_MANUALS");
										}
										else if(tok[a].equals("WD"))
										{
											details.setChannelName("WIRING_DIAGRAMS");
										}
										else if(tok[a].equals("OSM"))
										{
											details.setChannelName("OTHER_SERVICE_MANUALS");
										}
										details.setDocumentType(rs.getString("vc_vin_document_type"));
										
										if(null!=details.getChannelName() && "WIRING_DIAGRAMS".equals(details.getChannelName()))
										{
											// set document Type as null FOR WIRING DIAGRAMS
											details.setDocumentType(null);
										}
										details.setDocumentId(rs.getString("vc_vin_document_id"));
										tempList.add(details);
										details = null;
									}
									rs.close();rs=null;
									stmt.close();stmt= null;
									sql = null;
									details = null;
								}

								if(null!=tempList && tempList.size()>0)
								{
									logger.info("getDocumentsMatrixList :: Total rows fetched are :: > "+ tempList.size());
									details= null;
									ArrayList<RMIExportToolItemDetails> manualsItemsList = new ArrayList<RMIExportToolItemDetails>();
									boolean add  = true;
									for(int a=0;a<tempList.size();a++)
									{
										details = (RMIExportToolItemDetails)tempList.get(a);
										add = true;
										checkKey = "";
										if(null!=details.getLocale() && !"".equals(details.getLocale()))
										{
											checkKey+=details.getLocale();
										}
										if(null!=details.getModel() && !"".equals(details.getModel()))
										{
											checkKey+=details.getModel();
										}
										if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
										{
											checkKey+=details.getCarlineCode();
										}
										if(null!=details.getDocumentType() && !"".equals(details.getDocumentType()))
										{
											checkKey+=details.getDocumentType();
										}
										if(null!=details.getChannelName() && !"".equals(details.getChannelName()))
										{
											checkKey+=details.getChannelName();
										}


										if(null!=manualsItemsList && manualsItemsList.size()>0)
										{
											existingDetails = new RMIExportToolItemDetails();
											for(int b=0;b<manualsItemsList.size();b++)
											{
												existingDetails = (RMIExportToolItemDetails) manualsItemsList.get(b);
												existingKey = "";
												if(null!=existingDetails.getLocale() && !"".equals(existingDetails.getLocale()))
												{
													existingKey+=existingDetails.getLocale();
												}
												if(null!=existingDetails.getModel() && !"".equals(existingDetails.getModel()))
												{
													existingKey+=existingDetails.getModel();
												}
												if(null!=existingDetails.getCarlineCode() && !"".equals(existingDetails.getCarlineCode()))
												{
													existingKey+=existingDetails.getCarlineCode();
												}
												if(null!=existingDetails.getDocumentType() && !"".equals(existingDetails.getDocumentType()))
												{
													existingKey+=existingDetails.getDocumentType();
												}
												if(null!=existingDetails.getChannelName() && !"".equals(existingDetails.getChannelName()))
												{
													existingKey+=existingDetails.getChannelName();
												}

												if(checkKey.trim().toLowerCase().equals(existingKey.trim().toLowerCase()))
												{
													// item already added =- just increment the document count
													existingDetails.setDocumentsCounts(existingDetails.getDocumentsCounts()+1);
													// just add document to existingDetails Document Ids List
													if(null==existingDetails.getDocumentIdsList() || existingDetails.getDocumentIdsList().size()<=0)
													{
														existingDetails.setDocumentIdsList(new ArrayList<String>());
													}
													existingDetails.getDocumentIdsList().add(details.getDocumentId());
													add = false;
													break;
												}
												existingKey = null;
											}
										}

										if(add ==true)
										{
											// increment document count by 1 - as the item is getting added for the firstTime.
											details.setDocumentsCounts(details.getDocumentsCounts()+1);
											// just add document to details Document Ids List
											if(null==details.getDocumentIdsList() || details.getDocumentIdsList().size()<=0)
											{
												details.setDocumentIdsList(new ArrayList<String>());
											}
											details.getDocumentIdsList().add(details.getDocumentId());
											manualsItemsList.add(details);
										}

										checkKey = null;
										details = null;
									}

									
									if(null==itemsList || itemsList.size()<=0)
									{
										itemsList = new ArrayList<RMIExportToolItemDetails>();
									}
									if(null!=manualsItemsList && manualsItemsList.size()>0)
									{
										// add manualsData
										itemsList.addAll(manualsItemsList);
									}
									manualsItemsList  =null;
								}
								else
								{
									logger.info("getDocumentsMatrixList :: No rows fetched for channel : SM / OSM / WD. For Locale :: > "+ locale);
								}
								tempList=  null;
								details = null;
								existingDetails = null;
								checkKey = null;
								existingKey  =null;
								tok = null;
							}
							tok  =null;
							tableName = null;
						}
						mmeManualLocales = null;
						model = null;
						carlineCode = null;
						clDetails=  null;
					}
				}
				locale = null;
				model = null;
				carlineCode = null;
				
				
				
				
				if(null!=itemsList && itemsList.size()>0)
				{
					RMIExportToolItemDetails details = null;
					int count=0;
					for(int a=0;a<itemsList.size();a++)
					{
						details = (RMIExportToolItemDetails) itemsList.get(a);
						// remove items where count =0;
						if(details.getDocumentsCounts()==0)
						{
							itemsList.remove(a);
							a--;
						}
					}
					
					if(null!=itemsList && itemsList.size()>0)
					{
						logger.info("getDocumentsMatrixList :: Total final items found are :: > "+ itemsList.size());
						details = null;
						for(int a=0;a<itemsList.size();a++)
						{
							details = (RMIExportToolItemDetails) itemsList.get(a);
							count++;
							details.setSrNo(count);
						}
					}
				}
			}
			else
			{
				logger.info("getDocumentsMatrixList :: Required Parameters are null. Return null." );
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getDocumentsMatrixList()", e);
			itemsList = null;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				conn = null;
				stmt = null;
				rs = null;
				localesList = null;
				modelsList =  null;
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getDocumentsMatrixList()", e);
			}
		}
		return itemsList;
	}
	
	public static Long createSchedule(RMIExportToolScheduleDetails schDetails)
	{
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			if(null!=schDetails && !"".equals(schDetails) && null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO GMS3_MDM_RMI_TOOL_SCH
				 * First insert the SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql="INSERT INTO gms3_mdm_rmi_tool_sch(rmi_sch_name,rmi_sch_wsl_id,rmi_sch_processing_status) VALUES"
						+ "('"+schDetails.getScheduleName()+"','"+schDetails.getWslId()+"','"+schDetails.getProcessingStatus()+"')";
				logger.info("createSchedule :: Generate Schedule Id :: Sql :: > " + sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "rmi_sch_id" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				if(null!=scheduleId && scheduleId>0)
				{
					logger.info("createSchedule :: Procced for Storing details for Schedule Id :: > " + scheduleId);
					/*
					 * add scheduleId to name and update It
					 * add scheduleId to thread and update It
					 */
					schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
					schDetails.setThreadId(schDetails.getScheduleName());
					String schSql="UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_name=?,rmi_sch_start_time=?,"
							+ "rmi_sch_total_docs=?,rmi_sch_thread_id=?  WHERE rmi_sch_id= ?" ;
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
					pstmt.setLong(3, schDetails.getTotalDocsCount());
					pstmt.setString(4, schDetails.getThreadId());
					pstmt.setLong(5, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=  null;
					schSql= null;
					
					if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
					{
						RMIExportToolItemDetails itemDetails = null;
						for(int i=0;i<schDetails.getItemsList().size();i++)
						{
							 itemDetails = (RMIExportToolItemDetails)schDetails.getItemsList().get(i);
							String itemSql="INSERT INTO gms3_mdm_rmi_tool_sch_item(rmi_sch_id,rmi_sch_locale,rmi_sch_model,rmi_sch_carline_code,rmi_sch_channel,"
									+ "rmi_sch_doc_type,rmi_sch_total_docs,rmi_sch_processing_status) "
									+ "VALUES (?,?,?,?,?,?,?,?)";
							pstmt = null;
							pstmt = conn.prepareStatement(itemSql);
							pstmt.setLong(1, scheduleId);
							pstmt.setString(2, itemDetails.getLocale());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getChannelName());
							pstmt.setString(6, itemDetails.getDocumentType());
							pstmt.setLong(7, itemDetails.getDocumentsCounts());
							pstmt.setString(8, schDetails.getProcessingStatus());
							pstmt.executeUpdate();
							pstmt.close();
							pstmt=  null;
							itemDetails = null;
							itemSql= null;
						}
						
						/*
						 * commit the complete transaction
						 */
						conn.commit();
						logger.info("createSchedule :: Schedule Conversion with Name as {"+schDetails.getScheduleName()+"} Created Successfully.");
					}
				}
				else
				{
					logger.info("createSchedule :: Failed to Generate Schedule Id. Return null.");
				}
			}
			else
			{
				logger.info("createSchedule :: Schedule Details or Items in it are null as parameters, return null.");
			}
		}
		catch(Exception e)
		{
			logger.info("createSchedule :: Exception while Creating Schedule.Roll back data insertion. Return null.");
			try
			{
				conn.rollback();
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "createSchedule()", e1);
			}
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "createSchedule()", e);
			scheduleId = null;
		}
		finally
		{
			try
			{
				if(null!=conn)
					conn.close();
				if(null!=pstmt)
					pstmt.close();
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "createSchedule()", e);
			}
			// set schDetails to null
			schDetails = null;
		}
		return scheduleId;
	}
	
	public static ArrayList<RMIExportToolScheduleDetails> getHistoryTransactionList()
	{
		ArrayList<RMIExportToolScheduleDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_rmi_tool_sch ORDER BY rmi_sch_id DESC";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			RMIExportToolScheduleDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new RMIExportToolScheduleDetails();
				schDetails.setScheduleId(rs.getLong("rmi_sch_id"));
				schDetails.setScheduleName(rs.getString("rmi_sch_name"));
				schDetails.setWslId(rs.getString("rmi_sch_wsl_id"));
				schDetails.setTotalDocsCount(rs.getLong("rmi_sch_total_docs"));
				schDetails.setSuccessDocsCount(rs.getLong("rmi_sch_success_docs"));
				schDetails.setFailureDocsCount(rs.getLong("rmi_sch_failure_docs"));
				schDetails.setProcessingStatus(rs.getString("rmi_sch_processing_status"));
				schDetails.setCompletionStatus(rs.getString("rmi_sch_completion_status"));
				schDetails.setStartTime(rs.getTimestamp("rmi_sch_start_time"));
				schDetails.setFinishTime(rs.getTimestamp("rmi_sch_finish_time"));
				schDetails.setThreadId(rs.getString("rmi_sch_thread_id"));
				schDetails.setReportsPath(rs.getString("rmi_sch_reports_path"));
				
				schDetails.setTotalOkAssetsCount(rs.getLong("rmi_sch_okassets_total"));
				schDetails.setSuccessOkAssetsCount(rs.getLong("rmi_sch_okassets_success"));
				schDetails.setFailureOkAssetsCount(rs.getLong("rmi_sch_okassets_failure"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<RMIExportToolScheduleDetails>();
				}
				schDetails.setSrNo(list.size()+1);
				list.add(schDetails);
				schDetails  =null;
			}
			sql  =null;
			schDetails= null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getHistoryTransactionList()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getHistoryTransactionList()", eq);
			}
		}
		return list;
				
	}

	public static ArrayList<RMIExportToolItemDetails> getScheduleItemsList(String scheduleId)
	{
		ArrayList<RMIExportToolItemDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_rmi_tool_sch_item WHERE rmi_sch_id = "+ scheduleId;
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			RMIExportToolItemDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new RMIExportToolItemDetails();
				schDetails.setScheduleId(rs.getLong("rmi_sch_id"));
				schDetails.setItemId(rs.getLong("rmi_sch_item_id"));
				schDetails.setLocale(rs.getString("rmi_sch_locale"));
				schDetails.setModel(rs.getString("rmi_sch_model"));
				schDetails.setCarlineCode(rs.getString("rmi_sch_carline_code"));
				schDetails.setChannelName(rs.getString("rmi_sch_channel"));
				schDetails.setDocumentType(rs.getString("rmi_sch_doc_type"));
				schDetails.setProcessingStatus(rs.getString("rmi_sch_processing_status"));
				schDetails.setCompletionStatus(rs.getString("rmi_sch_completion_status"));
				schDetails.setTotalDocsCount(rs.getLong("rmi_sch_total_docs"));
				schDetails.setSuccessDocsCount(rs.getLong("rmi_sch_success_docs"));
				schDetails.setFailureDocsCount(rs.getLong("rmi_sch_failure_docs"));
				
				schDetails.setTotalInnerLinksCount(rs.getLong("rmi_sch_inlk_total"));
				schDetails.setSuccessInnerLinksCount(rs.getLong("rmi_sch_inlk_success"));
				schDetails.setFailureInnerLinksCount(rs.getLong("rmi_sch_inlk_failure"));
				schDetails.setInnerLinksProcessingStatus(rs.getString("rmi_sch_inlk_processing_status"));
				schDetails.setInnerLinksCompletionStatus(rs.getString("rmi_sch_inlk_completion_status"));
				
				schDetails.setTotalOkAssetsCount(rs.getLong("rmi_sch_okassets_total"));
				schDetails.setSuccessOkAssetsCount(rs.getLong("rmi_sch_okassets_success"));
				schDetails.setFailureOkAssetsCount(rs.getLong("rmi_sch_okassets_failure"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<RMIExportToolItemDetails>();
				}
				schDetails.setSrNo(list.size()+1);
				list.add(schDetails);
				schDetails  =null;
			}
			sql  =null;
			schDetails= null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getScheduleItemsList()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getScheduleItemsList()", eq);
			}
			scheduleId = null;
		}
		return list;
				
	}

	public static ArrayList<RMIExportToolReportSummaryDetails> getScheduleSummaryDetails(String scheduleId)
	{
		ArrayList<RMIExportToolReportSummaryDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_rmi_sch_summary WHERE rmi_sch_id = "+ scheduleId;
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			RMIExportToolReportSummaryDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new RMIExportToolReportSummaryDetails();
				schDetails.setScheduleId(String.valueOf(rs.getLong("rmi_sch_id")));
				schDetails.setReportName(rs.getString("rmi_sch_report_name"));
				schDetails.setReportStatus(rs.getString("rmi_sch_report_status"));
				
				schDetails.setTotalCount(rs.getLong("rmi_sch_total_count"));
				schDetails.setSuccessCount(rs.getLong("rmi_sch_success_count"));
				schDetails.setFailureCount(rs.getLong("rmi_sch_failure_count"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<RMIExportToolReportSummaryDetails>();
				}
				schDetails.setSrNo(list.size()+1);
				list.add(schDetails);
				schDetails  =null;
			}
			sql  =null;
			schDetails= null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getScheduleSummaryDetails()", e);
		}
		finally 
		{
			try
			{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "getScheduleSummaryDetails()", eq);
			}
			scheduleId = null;
		}
		return list;
				
	}

	
	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button writes rmi_sch_processing_status = aborted through updateAbortStatus() below, and does
	 * so whether or not it could find the running thread. The database therefore already carries
	 * the abort signal and this only reads it back.
	 *
	 * WHY THE DATABASE AND NOT A Thread REFERENCE. The screen used to call Thread.stop() on the
	 * worker. That was REMOVED in Java 20 and throws UnsupportedOperationException, which the
	 * caller's catch(Exception) swallowed - so the schedule was marked Aborted while the worker ran
	 * on to completion. Nothing can kill another thread any more; the worker has to stop itself.
	 *
	 * Returns FALSE on any error: a failed status read must never abort a healthy run.
	 */
	public static boolean isAborted(String scheduleId)
	{
		boolean aborted = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				if(null!=conn)
				{
					String sql="SELECT rmi_sch_processing_status FROM gms3_mdm_rmi_tool_sch WHERE rmi_sch_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, scheduleId.trim());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						String status = rs.getString("rmi_sch_processing_status");
						String abortedValue = ScheduleConstants.STATUS_ABORTED;
						if(null!=status && null!=abortedValue
								&& status.trim().equalsIgnoreCase(abortedValue.trim()))
						{
							aborted = true;
						}
						status = null;
						abortedValue = null;
					}
					sql = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "isAborted()", e);
		}
		finally
		{
			try
			{
				if(null!=rs) rs.close();
				if(null!=pstmt) pstmt.close();
				if(null!=conn) conn.close();
			}
			catch(Exception re)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "isAborted()", re);
			}
			rs = null;
			pstmt = null;
			conn = null;
		}
		return aborted;
	}


	public static void updateAbortStatus(String scheduleId)
	{
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				conn = getConnection();
				String sql="UPDATE gms3_mdm_rmi_tool_sch SET rmi_sch_processing_status='"+ScheduleConstants.STATUS_ABORTED+"', "
						+ " rmi_sch_completion_status='"+ScheduleConstants.STATUS_FAILURE+"',rmi_sch_finish_time = ? "
						+ " WHERE rmi_sch_id="+scheduleId;
				pstmt  =conn.prepareStatement(sql);
				pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));
				pstmt.executeUpdate();
				pstmt.close();pstmt=null;
				sql = null;
				// DO NOTHING WITH ITEMS STATUS
				
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "updateAbortStatus()", e);
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
			catch(SQLException re)
			{
				Utilities.printStackTraceToLogs(RMIExportToolDAO.class.getName(), "updateAbortStatus()", re);
			}
			scheduleId = null;
		}
	}
}
