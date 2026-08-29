package com.mazda.gms3.mdm.dataexttool.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolScheduleDetails;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolReportSummaryDetails;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class DataExportToolDAO extends DBConnectionHelper{

	private static Logger logger = LogManager.getLogger(DataExportToolDAO.class);

	public static ArrayList<DataExportToolItemDetails> getDocumentsMatrixList(List<String> localesList, List<CarlineDetails> modelsList, String fromDate, String toDate)
	{
		ArrayList<DataExportToolItemDetails> itemsList = null;
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
//				String carlineCode="";
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
//						carlineCode = clDetails.getCarlineCode();
						
						model = model.trim();
//						carlineCode = carlineCode.trim();
						
						/*
						 * FETCH DATA FOR ALL CHANNELS
						 * SI , VI, TR, SM, WD
						 */
//						String[] tok = "SI,VI,TR".split(",");
						String[] tok = "SI".split(",");
						String sql = "";
						ArrayList<DataExportToolItemDetails> tempList = new ArrayList<DataExportToolItemDetails>();
						DataExportToolItemDetails details = null;
						if(null!=tok && tok.length>0)
						{
							for(int a=0;a<tok.length;a++)
							{
								/*
								 * FETCH IT ON MODEL NAME  AND LOCALE ONLY 
								 * ALSO FOR SI CHANNEL - DO NOT FETCH TECHNICAL_INFORMATION
								 */
								sql = "SELECT DISTINCT vc_my_document_id, vc_my_document_type FROM gms3_vc_model_year_details WHERE vc_my_document_id LIKE '"+tok[a]+"%' "
										+ " AND  vc_my_locale='"+locale+"' AND vc_my_model='"+model+"' "
										+ " AND vc_my_document_type!='TECHNICAL_INFORMATION'";
								if(null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate))
								{
									sql+=" AND DATE_FORMAT(vc_my_doc_last_modified_date,'%Y-%m-%d') BETWEEN '"+fromDate+"' AND '"+toDate+"' ";
								}
								logger.info("getDocumentsMatrixList :: sql :: >"+ sql);
								stmt = conn.createStatement();
								rs = stmt.executeQuery(sql);
								while(rs.next())
								{
									details = new DataExportToolItemDetails();
									details.setLocale(locale);
									details.setModel(model);
//									details.setCarlineCode(carlineCode);
									details.setDocumentType(rs.getString("vc_my_document_type"));
									details.setDocumentId(rs.getString("vc_my_document_id"));
									if(tok[a].equals("SI"))
									{
										details.setChannelName("SERVICE_INFORMATION");
									}
									
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
							DataExportToolItemDetails existingDetails = null;
							String checkKey = null;
							String existingKey = null;
							boolean add = true;
							for(int a=0;a<tempList.size();a++)
							{
								details = (DataExportToolItemDetails)tempList.get(a);
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
//								if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
//								{
//									checkKey+=details.getCarlineCode();
//								}
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
									existingDetails = new DataExportToolItemDetails();
									for(int b=0;b<itemsList.size();b++)
									{
										existingDetails = (DataExportToolItemDetails) itemsList.get(b);
										existingKey = "";
										if(null!=existingDetails.getLocale() && !"".equals(existingDetails.getLocale()))
										{
											existingKey+=existingDetails.getLocale();
										}
										if(null!=existingDetails.getModel() && !"".equals(existingDetails.getModel()))
										{
											existingKey+=existingDetails.getModel();
										}
//										if(null!=existingDetails.getCarlineCode() && !"".equals(existingDetails.getCarlineCode()))
//										{
//											existingKey+=existingDetails.getCarlineCode();
//										}
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
										itemsList = new ArrayList<DataExportToolItemDetails>();
									}
									itemsList.add(details);
								}

								checkKey = null;
								details = null;
							}
						}
						else
						{
							logger.info("getDocumentsMatrixList :: No Rows fetched for Channels :: SI. For Locale :: > "+ locale);
						}

						tempList = null;
						tok = null;
						/*
						 * Now fetch data for OSM, SM & WD
						 */
						String mnaoLocales="en_US,en_CA,fr_CA,es_MX";
						/*
						 * PROCEED ONLY WHEN ABOVE LOCALE IS SELECTED
						 */
						if(mnaoLocales.indexOf(locale)!=-1)
						{
							tok ="SM,OSM,WD".split(",");
//							String tableName="GMS3_VC_MME_VIN_DTL_"+locale.trim().toUpperCase();
							String tableName="gms3_vc_model_year_details";
							if(null!=tok && tok.length>0)
							{
								tempList = new ArrayList<DataExportToolItemDetails>();
								details = null;
								DataExportToolItemDetails existingDetails = null;
								String checkKey = null;
								String existingKey = null;
								for(int a=0;a<tok.length;a++)
								{
									/*
									 * FETCH IT ON MODEL CODE AND LOCALE ONLY 
									 */
									sql = "SELECT DISTINCT vc_my_document_id, vc_my_document_type FROM "+ tableName+" WHERE vc_my_document_id LIKE '"+tok[a]+"%'  "
											+ " AND  vc_my_locale='"+locale+"' AND vc_my_model='"+model+"'  ";
									if(null!=fromDate && !"".equals(fromDate) && null!=toDate && !"".equals(toDate))
									{
										sql+=" AND DATE_FORMAT(vc_my_doc_last_modified_date,'%Y-%m-%d') BETWEEN '"+fromDate+"' AND '"+toDate+"' ";
									}
									logger.info("getDocumentsMatrixList :: Sql :: > "+ sql);
									stmt = conn.createStatement();
									rs = stmt.executeQuery(sql);
									while(rs.next())
									{
										details = new DataExportToolItemDetails();
										details.setLocale(locale);
//										details.setCarlineCode(carlineCode);
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
										details.setDocumentType(rs.getString("vc_my_document_type"));
										
										if(null!=details.getChannelName() && "WIRING_DIAGRAMS".equals(details.getChannelName()))
										{
											// set document Type as null FOR WIRING DIAGRAMS
											details.setDocumentType(null);
										}
										details.setDocumentId(rs.getString("vc_my_document_id"));
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
									ArrayList<DataExportToolItemDetails> manualsItemsList = new ArrayList<DataExportToolItemDetails>();
									boolean add  = true;
									for(int a=0;a<tempList.size();a++)
									{
										details = (DataExportToolItemDetails)tempList.get(a);
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
//										if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
//										{
//											checkKey+=details.getCarlineCode();
//										}
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
											existingDetails = new DataExportToolItemDetails();
											for(int b=0;b<manualsItemsList.size();b++)
											{
												existingDetails = (DataExportToolItemDetails) manualsItemsList.get(b);
												existingKey = "";
												if(null!=existingDetails.getLocale() && !"".equals(existingDetails.getLocale()))
												{
													existingKey+=existingDetails.getLocale();
												}
												if(null!=existingDetails.getModel() && !"".equals(existingDetails.getModel()))
												{
													existingKey+=existingDetails.getModel();
												}
//												if(null!=existingDetails.getCarlineCode() && !"".equals(existingDetails.getCarlineCode()))
//												{
//													existingKey+=existingDetails.getCarlineCode();
//												}
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
										itemsList = new ArrayList<DataExportToolItemDetails>();
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
						mnaoLocales = null;
						model = null;
//						carlineCode = null;
						clDetails=  null;
					}
				}
				locale = null;
				model = null;
//				carlineCode = null;
				
				
				
				if(null!=itemsList && itemsList.size()>0)
				{
					DataExportToolItemDetails details = null;
					int count=0;
					for(int a=0;a<itemsList.size();a++)
					{
						details = (DataExportToolItemDetails) itemsList.get(a);
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
							details = (DataExportToolItemDetails) itemsList.get(a);
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getDocumentsMatrixList()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getDocumentsMatrixList()", e);
			}
		}
		return itemsList;
	}
	
	public static Long createSchedule(DataExportToolScheduleDetails schDetails)
	{
		Long scheduleId=null;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			if(null!=schDetails && null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				/*
				 * INSERT IN TO GMS3_MDM_MDE_TOOL_SCH
				 * First insert the SCH_NAME, USER_ID AND SCHEDULE_TIME to get the SCHEDULE_ID
				 */
				String sql="INSERT INTO gms3_mdm_mde_tool_sch(mde_sch_name,mde_sch_wsl_id,mde_sch_processing_status) VALUES"
						+ "('"+schDetails.getScheduleName()+"','"+schDetails.getWslId()+"','"+schDetails.getProcessingStatus()+"')";
				logger.info("createSchedule :: Generate Schedule Id :: Sql :: > " + sql);
				stmt = conn.createStatement();
				String generatedColumns[] = { "mde_sch_id" };
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
					String schSql="UPDATE gms3_mdm_mde_tool_sch SET mde_sch_name=?,mde_sch_start_time=?,"
							+ "mde_sch_total_docs=?,mde_sch_thread_id=?,mdm_sch_pub_from_date=?,mdm_sch_pub_from_date_str=?,"
							+ "mdm_sch_pub_to_date=?,mdm_sch_pub_to_date_str=?  WHERE mde_sch_id= ?" ;
					pstmt = conn.prepareStatement(schSql);
					pstmt.setString(1, schDetails.getScheduleName());
					pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
					pstmt.setLong(3, schDetails.getTotalDocsCount());
					pstmt.setString(4, schDetails.getThreadId());
					if(null!=schDetails.getFromDate())
					{
						pstmt.setDate(5, new java.sql.Date(schDetails.getFromDate().getTime()));
					}
					else
					{
						pstmt.setNull(5, Types.DATE);
					}
					pstmt.setString(6, schDetails.getFromDateStr());
					if(null!=schDetails.getToDate())
					{
						pstmt.setDate(7, new java.sql.Date(schDetails.getToDate().getTime()));
					}
					else
					{
						pstmt.setNull(7, Types.DATE);
					}
					pstmt.setString(8, schDetails.getToDateStr());
					pstmt.setLong(9, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=  null;
					schSql= null;
					
					if(null!=schDetails.getItemsList() && schDetails.getItemsList().size()>0)
					{
						DataExportToolItemDetails itemDetails = null;
						for(int i=0;i<schDetails.getItemsList().size();i++)
						{
							 itemDetails = (DataExportToolItemDetails)schDetails.getItemsList().get(i);
							String itemSql="INSERT INTO gms3_mdm_mde_tool_sch_it(mde_sch_id,mde_sch_locale,mde_sch_model,mde_sch_carline_code,mde_sch_channel,"
									+ "mde_sch_doc_type,mde_sch_total_docs,mde_sch_processing_status) "
									+ "VALUES (?,?,?,?,?,?,?,?)";
							pstmt = null;
							pstmt = conn.prepareStatement(itemSql);
							pstmt.setLong(1, scheduleId);
							pstmt.setString(2, itemDetails.getLocale());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setNull(4, Types.VARCHAR);
//							pstmt.setString(4, itemDetails.getCarlineCode());
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "createSchedule()", e1);
			}
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "createSchedule()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "createSchedule()", e);
			}
			// set schDetails to null
			schDetails = null;
		}
		return scheduleId;
	}
	
	public static ArrayList<DataExportToolScheduleDetails> getHistoryTransactionList()
	{
		ArrayList<DataExportToolScheduleDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_mde_tool_sch ORDER BY mde_sch_id DESC";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			DataExportToolScheduleDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new DataExportToolScheduleDetails();
				schDetails.setScheduleId(rs.getLong("mde_sch_id"));
				schDetails.setScheduleName(rs.getString("mde_sch_name"));
				schDetails.setWslId(rs.getString("mde_sch_wsl_id"));
				schDetails.setTotalDocsCount(rs.getLong("mde_sch_total_docs"));
				schDetails.setSuccessDocsCount(rs.getLong("mde_sch_success_docs"));
				schDetails.setFailureDocsCount(rs.getLong("mde_sch_failure_docs"));
				schDetails.setProcessingStatus(rs.getString("mde_sch_processing_status"));
				schDetails.setCompletionStatus(rs.getString("mde_sch_completion_status"));
				schDetails.setStartTime(rs.getTimestamp("mde_sch_start_time"));
				schDetails.setFinishTime(rs.getTimestamp("mde_sch_finish_time"));
				schDetails.setThreadId(rs.getString("mde_sch_thread_id"));
				schDetails.setReportsPath(rs.getString("mde_sch_reports_path"));
				schDetails.setZipFilePath(rs.getString("mde_sch_content_zip_path"));
				
				schDetails.setTotalOkAssetsCount(rs.getLong("mde_sch_okassets_total"));
				schDetails.setSuccessOkAssetsCount(rs.getLong("mde_sch_okassets_success"));
				schDetails.setFailureOkAssetsCount(rs.getLong("mde_sch_okassets_failure"));
				
				schDetails.setReportZipSize(rs.getString("mdm_sch_reports_size"));
				schDetails.setXmlZipSize(rs.getString("mde_sch_content_zip_size"));
//				schDetails.setAwsCompletionStatus(rs.getString("mde_aws_completion_status"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<DataExportToolScheduleDetails>();
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getHistoryTransactionList()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getHistoryTransactionList()", eq);
			}
		}
		return list;
				
	}

	public static ArrayList<DataExportToolItemDetails> getScheduleItemsList(String scheduleId)
	{
		ArrayList<DataExportToolItemDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_mde_tool_sch_it WHERE mde_sch_id = "+ scheduleId;
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			DataExportToolItemDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new DataExportToolItemDetails();
				schDetails.setScheduleId(rs.getLong("mde_sch_id"));
				schDetails.setItemId(rs.getLong("mde_sch_item_id"));
				schDetails.setLocale(rs.getString("mde_sch_locale"));
				schDetails.setModel(rs.getString("mde_sch_model"));
//				schDetails.setCarlineCode(rs.getString("mde_sch_carline_code"));
				schDetails.setChannelName(rs.getString("mde_sch_channel"));
				schDetails.setDocumentType(rs.getString("mde_sch_doc_type"));
				schDetails.setProcessingStatus(rs.getString("mde_sch_processing_status"));
				schDetails.setCompletionStatus(rs.getString("mde_sch_completion_status"));
				schDetails.setTotalDocsCount(rs.getLong("mde_sch_total_docs"));
				schDetails.setSuccessDocsCount(rs.getLong("mde_sch_success_docs"));
				schDetails.setFailureDocsCount(rs.getLong("mde_sch_failure_docs"));
				
				schDetails.setTotalInnerLinksCount(rs.getLong("mde_sch_inlk_total"));
				schDetails.setSuccessInnerLinksCount(rs.getLong("mde_sch_inlk_success"));
				schDetails.setFailureInnerLinksCount(rs.getLong("mde_sch_inlk_failure"));
				schDetails.setInnerLinksProcessingStatus(rs.getString("mde_sch_inlk_processing_status"));
				schDetails.setInnerLinksCompletionStatus(rs.getString("mde_sch_inlk_completion_status"));
				
				schDetails.setTotalOkAssetsCount(rs.getLong("mde_sch_okassets_total"));
				schDetails.setSuccessOkAssetsCount(rs.getLong("mde_sch_okassets_success"));
				schDetails.setFailureOkAssetsCount(rs.getLong("mde_sch_okassets_failure"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<DataExportToolItemDetails>();
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getScheduleItemsList()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getScheduleItemsList()", eq);
			}
			scheduleId = null;
		}
		return list;
				
	}

	public static ArrayList<DataExportToolReportSummaryDetails> getScheduleSummaryDetails(String scheduleId)
	{
		ArrayList<DataExportToolReportSummaryDetails> list = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{

			conn = getConnection();
			String sql="SELECT * FROM gms3_mdm_mde_sch_summary WHERE mde_sch_id = "+ scheduleId;
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			DataExportToolReportSummaryDetails schDetails = null;
			while(rs.next())
			{
				schDetails = new DataExportToolReportSummaryDetails();
				schDetails.setScheduleId(String.valueOf(rs.getLong("mde_sch_id")));
				schDetails.setReportName(rs.getString("mde_sch_report_name"));
				schDetails.setReportStatus(rs.getString("mde_sch_report_status"));
				
				schDetails.setTotalCount(rs.getLong("mde_sch_total_count"));
				schDetails.setSuccessCount(rs.getLong("mde_sch_success_count"));
				schDetails.setFailureCount(rs.getLong("mde_sch_failure_count"));
				
				if(null==list || list.size()<=0)
				{
					list = new ArrayList<DataExportToolReportSummaryDetails>();
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getScheduleSummaryDetails()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "getScheduleSummaryDetails()", eq);
			}
			scheduleId = null;
		}
		return list;
	}

	
	/**
	 * HAS THIS SCHEDULE BEEN ABORTED FROM THE SCREEN?
	 *
	 * The Abort button writes mde_sch_processing_status = aborted through updateAbortStatus() below, and does
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
					String sql="SELECT mde_sch_processing_status FROM gms3_mdm_mde_tool_sch WHERE mde_sch_id = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, scheduleId.trim());
					rs = pstmt.executeQuery();
					if(rs.next())
					{
						String status = rs.getString("mde_sch_processing_status");
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "isAborted()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "isAborted()", re);
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
				String sql="UPDATE gms3_mdm_mde_tool_sch SET mde_sch_processing_status='"+ScheduleConstants.STATUS_ABORTED+"', "
						+ " mde_sch_completion_status='"+ScheduleConstants.STATUS_FAILURE+"',mde_sch_finish_time = ? "
						+ " WHERE mde_sch_id="+scheduleId;
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
			Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "updateAbortStatus()", e);
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
				Utilities.printStackTraceToLogs(DataExportToolDAO.class.getName(), "updateAbortStatus()", re);
			}
			scheduleId = null;
		}
	}
}
