package com.mazda.gms3.mdm.dao;

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
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.SchVINMappingDetails;

public class SchMaintenanceVINMappingDAO extends DBConnectionHelper{
	
	private static Logger logger = LogManager.getLogger(SchMaintenanceVINMappingDAO.class);
	
	public static ArrayList<SchVINMappingDetails> getSchVinMappingDetails() throws SQLException 
	{
//		logger.info("getSchVinMappingDetails :: Method Starts.");
		ArrayList<SchVINMappingDetails> schVinList = new ArrayList<SchVINMappingDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "select * from gms3_dmt_schm_mapping   ORDER BY (CASE WHEN (scm_modified_tmstp) IS NOT NULL THEN " + 
					"(scm_modified_tmstp)  ELSE (scm_creation_tmstp) END) DESC ";
			
			logger.info("getSchVinMappingDetails :: Sql :: > " + sql);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			SchVINMappingDetails details = null;
			while(rs.next())
			{
				details = new SchVINMappingDetails();
				details.setSrNo(schVinList.size()+1);
				details.setAutoId(rs.getLong("scm_id"));
				details.setDmtScheduleId(rs.getLong("dmt_schedule_id"));
				if(null!=rs.getString("scm_locale"))
				{
					details.setLocale(rs.getString("scm_locale").trim());
				}
				if(null!=rs.getString("scm_model_code"))
				{
					details.setCarlineCode(rs.getString("scm_model_code").trim());
				}
				if(null!=rs.getString("scm_model_name"))
				{
					details.setModelName(rs.getString("scm_model_name").trim());
				}
				if(null!=rs.getString("scm_wmi_code"))
				{
					details.setWmiCode(rs.getString("scm_wmi_code").trim());
				}
				if(null!=rs.getString("scm_vds_code"))
				{
					details.setVdsCode(rs.getString("scm_vds_code").trim());
				}
				if(null!=rs.getString("scm_vis_start_range"))
				{
					details.setVisStartRange(rs.getString("scm_vis_start_range").trim());
				}
				if(null!=rs.getString("scm_vis_end_range"))
				{
					details.setVisEndRange(rs.getString("scm_vis_end_range").trim());
				}
				if(null!=rs.getString("scm_source_file_name"))
				{
					details.setSourceFileName(rs.getString("scm_source_file_name").trim());
				}
				if(null!=rs.getString("scm_source_file_path"))
				{
					details.setSourceFilePath(rs.getString("scm_source_file_path").trim());
				}
				if(null!=rs.getString("scm_okm_doc_id"))
				{
					details.setDocumentId(rs.getString("scm_okm_doc_id").trim());
				}
				if(null!=rs.getString("scm_okm_doc_title"))
				{
					details.setTitle(rs.getString("scm_okm_doc_title").trim());
				}
				
				details.setEntryTime(rs.getTimestamp("scm_creation_tmstp"));
				details.setUpdatedTime(rs.getTimestamp("scm_modified_tmstp"));
				// always show CheckBox
				details.setShowCheckBox(true);
				
				schVinList.add(details);
				details=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			logger.info("getSchVinMappingDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SchMaintenanceVINMappingDAO.class.getName(), "getSchVinMappingDetails()", e);
			logger.info("getSchVinMappingDetails :: ################ Exception ################");
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
//		logger.info("getSchVinMappingDetails :: Method Ends.");
		return schVinList;
	}
	
	public static List<SchVINMappingDetails> importSchVinMappingDetails(List<SchVINMappingDetails> list) throws SQLException
	{
		logger.info("importSchVinMappingDetails :: Method Starts.");
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=list && list.size()>0)
			{
				conn = getConnection();
				/*
				 * ITERATE AND PROCESS EACH ROW.
				 */
				String getSchVinSql = "";
				String sql="";
				SchVINMappingDetails details = new SchVINMappingDetails();
				long autoId=0;
				for(int a=0;a<list.size();a++)
				{
					details = (SchVINMappingDetails)list.get(a);
					if(null!=details.getLocale())
					{
						details.setLocale(details.getLocale().trim());
					}
					if(null!=details.getCarlineCode())
					{
						details.setCarlineCode(details.getCarlineCode().trim());
					}
					if(null!=details.getWmiCode())
					{
						details.setWmiCode(details.getWmiCode().trim());
					}
					if(null!=details.getVdsCode())
					{
						details.setVdsCode(details.getVdsCode().trim());
					}
					if(null!=details.getVisStartRange())
					{
						details.setVisStartRange(details.getVisStartRange().trim());
					}
					if(null!=details.getVisEndRange())
					{
						details.setVisEndRange(details.getVisEndRange().trim());
					}
					if(null!=details.getSourceFilePath())
					{
						details.setSourceFilePath(details.getSourceFilePath().trim());
						// identify sourceFileName
						if(details.getSourceFilePath().lastIndexOf("\\")!=-1)
						{
							details.setSourceFileName(details.getSourceFilePath().substring(details.getSourceFilePath().lastIndexOf("\\")+1, details.getSourceFilePath().length()));
						}
					}
					
					try
					{
						/*
						 * CHECK WHETHER VIN EXISTS OR NOT
						 * IF YES -  THEN UPDATE VIN
						 * ELSE - INSERT VIN
						 */
						getSchVinSql = "SELECT scm_id FROM gms3_dmt_schm_mapping WHERE scm_locale = ? AND  scm_model_code=? AND "
								+ " scm_wmi_code=? AND scm_vds_code = ? AND scm_vis_start_range = ? AND scm_vis_end_range = ? AND LOWER(scm_source_file_path) = ?";
//						logger.info("importSchVinMappingDetails :: getSchVinSql :: > " + getSchVinSql);
						pstmt = conn.prepareStatement(getSchVinSql);
						pstmt.setString(1, details.getLocale());
						pstmt.setString(2, details.getCarlineCode());
						pstmt.setString(3, details.getWmiCode());
						pstmt.setString(4, details.getVdsCode());
						pstmt.setString(5, details.getVisStartRange());
						pstmt.setString(6, details.getVisEndRange());
						pstmt.setString(7, details.getSourceFilePath().trim().toLowerCase());
						rs = pstmt.executeQuery();
						autoId=0;
						if(rs.next())
						{
							autoId= rs.getLong("scm_id");
						}
						rs.close();
						rs = null;
						pstmt.close();
						pstmt = null;
						getSchVinSql = null;
						
						/*
						 * GET MODEL NAME FOR THE PROCESSING VIN
						 */
						details.setModelName(getModelName(details, conn));
						
						if(null!=details.getModelName() && !"".equals(details.getModelName()))
						{
							/*
							 * IDENTIFY DOCUMENT ID ON THE BASIS OF LOCALE AND SOURCE FILE PATH
							 */
							String[] tableName=("gms3_dmt_"+Utilities.tableLocale(details.getLocale())+"_nm_imdoc,gms3_dmt_"
								+Utilities.tableLocale(details.getLocale())+"_imdoc").split(",");
							for(int r=0;r<tableName.length;r++)
							{
								boolean found = false;
								sql = "SELECT dc_im_doc_title,dc_im_doc_id, dc_schedule_id FROM "+ tableName[r]+" WHERE TRIM(LOWER(dc_source_network_loc)) = ? AND dc_doc_status = ?" ;
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, details.getSourceFilePath().trim().toLowerCase());
								pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
								rs = pstmt.executeQuery();
								if(rs.next())
								{
									details.setDocumentId(rs.getString("dc_im_doc_id"));
									details.setTitle(rs.getString("dc_im_doc_title"));
									details.setDmtScheduleId(rs.getLong("dc_schedule_id"));
									found = true;
								}
								rs.close();rs=null;
								pstmt.close();pstmt=null;
								sql = null;
								
								if(found==true)
								{
									break;
								}
							}
							tableName = null;
							
							if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
							{
								if(autoId>0)
								{
									pstmt = null;
									logger.info("importSchVinMappingDetails :: SCH VIN Mapping for "+details.getSourceFilePath()+" Already Exists. Update Row for Auto id : >" + autoId);
									sql = "UPDATE gms3_dmt_schm_mapping SET dmt_schedule_id = ?,scm_locale = ?, scm_model_code = ?,scm_model_name = ?,scm_wmi_code = ?, scm_vds_code = ?,"
											+ "scm_vis_start_range = ?, scm_vis_end_range = ?,scm_source_file_name = ?,scm_source_file_path = ?,scm_okm_doc_id = ?,scm_okm_doc_title = ?,"
											+ "scm_modified_tmstp = ? WHERE scm_id = ?";
									pstmt = conn.prepareStatement(sql);
									pstmt.setLong(1, details.getDmtScheduleId());
									pstmt.setString(2, details.getLocale());
									pstmt.setString(3, details.getCarlineCode());
									pstmt.setString(4, details.getModelName());
									pstmt.setString(5, details.getWmiCode());
									pstmt.setString(6, details.getVdsCode());
									pstmt.setString(7, details.getVisStartRange());
									pstmt.setString(8, details.getVisEndRange());
									pstmt.setString(9, details.getSourceFileName());
									pstmt.setString(10, details.getSourceFilePath());
									pstmt.setString(11, details.getDocumentId());
									pstmt.setString(12, details.getTitle());
									pstmt.setTimestamp(13, new Timestamp(new Date().getTime()));
									pstmt.setLong(14, autoId);
									pstmt.executeUpdate();
									pstmt.close();pstmt=null;
									details.setSaveStatusWhileImport(true);
								}
								else
								{
									pstmt = null;
									logger.info("importSchVinMappingDetails :: SCH VIN Mapping Does not Exists for "+details.getSourceFilePath()+". Insert New Row.");
									
									sql = "INSERT INTO gms3_dmt_schm_mapping (dmt_schedule_id,scm_locale, scm_model_code,scm_model_name,scm_wmi_code, scm_vds_code,"
											+ "scm_vis_start_range, scm_vis_end_range,scm_source_file_name,scm_source_file_path,scm_okm_doc_id,scm_okm_doc_title,"
											+ "scm_creation_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";	
									pstmt = conn.prepareStatement(sql);
									pstmt.setLong(1, details.getDmtScheduleId());
									pstmt.setString(2, details.getLocale());
									pstmt.setString(3, details.getCarlineCode());
									pstmt.setString(4, details.getModelName());
									pstmt.setString(5, details.getWmiCode());
									pstmt.setString(6, details.getVdsCode());
									pstmt.setString(7, details.getVisStartRange());
									pstmt.setString(8, details.getVisEndRange());
									pstmt.setString(9, details.getSourceFileName());
									pstmt.setString(10, details.getSourceFilePath());
									pstmt.setString(11, details.getDocumentId());
									pstmt.setString(12, details.getTitle());
									pstmt.setTimestamp(13, new Timestamp(new Date().getTime()));
									pstmt.executeUpdate();
									pstmt.close();pstmt=null;
									details.setSaveStatusWhileImport(true);
								}
							}
							else
							{
								// NOT ABLE TO FIND DOCUMENT DETAILS, SKIP ITS SAVING
								details.setDocumentNotFoundMessage("DOCUMENT NOT FOUND");
							}
						}
						else
						{
							// NOT ABLE TO FIND MODEL NAME, SKIP ITS SAVING
							details.setModelNotFoundMessage("MODEL NOT FOUND");
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(SchMaintenanceVINMappingDAO.class.getName(), "importSchVinMappingDetails()", e);
					}
					
					autoId = 0;
					details = null;
				}
				
			}
			else
			{
				logger.info("importSchVinMappingDetails :: No SCH Maintenance VIN Mapping Book Data Passed as parameter for Importing.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMappingDAO.class.getName(), "importSchVinMappingDetails()", e);
		}
		finally
		{
			if(null!= pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
		}
		logger.info("importSchVinMappingDetails :: Method Ends.");
		return list;
	}

	private static String getModelName(SchVINMappingDetails details, Connection conn) throws SQLException
	{
		String modelName=null;
		PreparedStatement pstmt= null;
		ResultSet rs = null;
		String locale=ApplicationProperties.getProperty("en_uk");
		locale = locale.replace("_", "-");
		String sql="SELECT mdm_crln_name_eng_lang,mdm_crln_name_regional_lang FROM gms3_mdm_vin_detail "
				+ " WHERE TRIM(LOWER(mdm_crln_code)) ='"+details.getCarlineCode().trim().toLowerCase()+"'"
				+ " AND TRIM(LOWER(mdm_vin_wmi_code)) ='"+details.getWmiCode().trim().toLowerCase()+"' "
				+ " AND TRIM(LOWER(mdm_vin_vds_code)) ='"+details.getVdsCode().trim().toLowerCase()+"' "
				+ " AND TRIM(LOWER(mdm_vin_vis_start_range)) ='"+details.getVisStartRange().trim().toLowerCase()+"' "
				+ " AND TRIM(LOWER(mdm_vin_vis_end_range)) ='"+details.getVisEndRange().trim().toLowerCase()+"' "
				+ " AND TRIM(LOWER(mdm_ml_lang_code)) ='"+locale.trim().toLowerCase()+"' "
				+ " AND mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"'";
		
		logger.info("getModelName :: Sql :: >"+ sql);
		pstmt = conn.prepareStatement(sql);
		rs = pstmt.executeQuery();
		if(rs.next())
		{
			if(null!=rs.getString("mdm_crln_name_eng_lang"))
			{
				modelName = rs.getString("mdm_crln_name_eng_lang").trim();
			}			
		}
		rs.close();rs=null;
		pstmt.close();pstmt=null;
		return modelName;
	}
	
}
