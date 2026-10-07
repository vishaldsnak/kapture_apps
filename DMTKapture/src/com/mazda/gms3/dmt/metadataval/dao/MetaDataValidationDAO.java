package com.mazda.gms3.dmt.metadataval.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataScheduleConstants;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataValidationUtils;
import com.mazda.gms3.dmt.metadataval.vo.MetaDataFileDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class MetaDataValidationDAO extends DBConnectionHelper{

	private Logger logger = LogManager.getLogger(MetaDataValidationDAO.class);
	
	public ArrayList<SelectItemDetails> getCountryList() throws SQLException 
	{
		ArrayList<SelectItemDetails> countryLocaleList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			String sql = "SELECT MDM_CL_ID,MDM_CL_LOCALE_CODE,MDM_CL_LOCALE_DESC  "
					+ "FROM gms3_mdm_country_locale WHERE "
					+ "MDM_CL_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"' ORDER BY MDM_CL_LOCALE_DESC ASC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				SelectItemDetails details = new SelectItemDetails();
				details.setValue(String.valueOf(rs.getLong("MDM_CL_ID")));
				if(null!=rs.getString("MDM_CL_LOCALE_CODE"))
				{
					details.setCode(rs.getString("MDM_CL_LOCALE_CODE").trim());
				}
				if(null!=rs.getString("MDM_CL_LOCALE_DESC"))
				{
					details.setHeading(rs.getString("MDM_CL_LOCALE_DESC").trim());
					details.setLabel(rs.getString("MDM_CL_LOCALE_DESC").trim());
				}
				
				if(null!=details.getCode() && !"".equals(details.getCode()))
				{
					if(null!=details.getLabel() && !"".equals(details.getLabel()))
					{
						details.setLabel(details.getLabel()+" ("+details.getCode()+")");
					}
					else
					{
						details.setLabel("("+details.getCode()+")");
					}
				}
				countryLocaleList.add(details);
				details=  null;
			}
			sql = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getCountryList()", e);
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
		return countryLocaleList;
	}

	public ArrayList<SelectItemDetails> getLanguageList(String countryLocaleId) throws SQLException 
	{
		//		logger.info("getLanguageList :: Method Starts.");
		ArrayList<SelectItemDetails> languageList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=countryLocaleId && !"".equals(countryLocaleId))
			{
				conn = getConnection();
				String sql = "SELECT MDM_ML_ID,MDM_ML_LANG_CODE,MDM_ML_LANG_DESC FROM gms3_mdm_manual_language WHERE "
						+ "MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'"
						+ "AND MDM_CL_ID="+new Long(countryLocaleId).longValue()+" ORDER BY MDM_ML_LANG_DESC ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SelectItemDetails details = new SelectItemDetails();
					details.setValue(String.valueOf(rs.getLong("MDM_ML_ID")));
					if(null!=rs.getString("MDM_ML_LANG_DESC"))
					{
						details.setHeading(rs.getString("MDM_ML_LANG_DESC").trim());
						details.setLabel(rs.getString("MDM_ML_LANG_DESC").trim());
					}
					if(null!=rs.getString("MDM_ML_LANG_CODE"))
					{
						details.setCode(rs.getString("MDM_ML_LANG_CODE").trim());
					}
					if(null!=details.getCode() && !"".equals(details.getCode()))
					{
						if(null!=details.getLabel() && !"".equals(details.getLabel()))
						{
							details.setLabel(details.getLabel()+" ("+details.getCode()+")");
						}
						else
						{
							details.setLabel("("+details.getCode()+")");
						}
					}
					
					languageList.add(details);
					details=  null;
				}
				sql = null;
			}
			else
			{
				logger.info("getLanguageList :: Country Locale Id as parameter is null. Return null.");
				languageList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getLanguageList()", e);
			languageList = null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
			// set param to null
			countryLocaleId = null;
		}
		return languageList;
	}
	
	public Long createSchedule(MetaDataFileDetails details) throws SQLException
	{
		Long scheduleId=null;
		Connection conn = null;
		Statement stmt=null;
		ResultSet rs=null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=details.getFilePath() && !"".equals(details.getFilePath()) && 
					null!=details.getFileName() && !"".equals(details.getFileName()) && 
					null!=details.getLocale() && !"".equals(details.getLocale()))
			{
				
				conn = getConnection();
				conn.setAutoCommit(false);
				// save sch name  - get schId
				String sql="INSERT INTO gms3_dmt_metadata_val_sch (DC_SCHEDULE_NAME,DC_RECORD_STATUS) VALUES ('"+details.getScheduleName()+"','"+AutoSyncConstants.STATUS_ACTIVE+"')";
				stmt = conn.createStatement();
				String generatedColumns[] = { "DC_SCHEDULE_ID" };
				stmt.execute(sql,generatedColumns);
				rs = stmt.getGeneratedKeys();
				if(rs.next())
				{
					scheduleId = rs.getLong(1);
				}
				rs.close();rs =null;
				stmt.close();stmt = null;
				sql=null;
				
				if(scheduleId > 0)
				{
					sql="UPDATE gms3_dmt_metadata_val_sch SET DC_SCHEDULE_NAME = ?, DC_USER_ID=?, DC_STATUS=?,DC_SCH_THREAD_ID=?,"
							+ "DC_SCHEDULE_TMSTP = ?, DC_METADATA_TYPE=?,DC_METADATA_FILE_NAME=?,DC_METADATA_FILE_PATH=?,"
							+ "DC_METADATA_FILE_WEB_PATH=?,DC_LOCALE=? WHERE DC_SCHEDULE_ID=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, details.getScheduleName()+String.valueOf(scheduleId));
					pstmt.setString(2, details.getWslId());
					pstmt.setString(3, details.getStatus());
					pstmt.setString(4, details.getThreadId()+String.valueOf(scheduleId));
					pstmt.setTimestamp(5, new Timestamp(new Date().getTime()));
					pstmt.setString(6, details.getMetaDataType());
					pstmt.setString(7, details.getFileName());
					pstmt.setString(8, details.getFilePath());
					pstmt.setString(9, details.getWebFilePath());
					pstmt.setString(10, details.getLocale());
					pstmt.setLong(11, scheduleId);
					pstmt.executeUpdate();
					pstmt.close();
					pstmt=null;
					// commit transaction
					conn.commit();
				}
				else
				{
					logger.info("createSchedule :: Failed to Create Schdeule for :: > "+ details.getFileName());
					conn.rollback();
				}
			}
		}
		catch(Exception e)
		{
			conn.rollback();
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "createSchedule()", e);
			scheduleId = null;
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=pstmt)
				pstmt.close();
			details= null;
		}
		return scheduleId;
	}

	public ArrayList<MetaDataFileDetails> getScheduleList() throws SQLException
	{
		ArrayList<MetaDataFileDetails> scheduleList = null;
		Connection conn = null;
		ResultSet rs= null;
		PreparedStatement pstmt =null;
		try
		{
			conn = getConnection();
			// add 1 day to current timestamp
			Timestamp currentTime=new Timestamp(new Date().getTime()+1000L*60L*60L*24L*1L);
			// deduct 30 days from current timestamp
			Timestamp beforeTime = new Timestamp(new Date( new Date().getTime() - 1000L*60L*60L*24L*30L).getTime());
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyy");
			currentTime = new Timestamp(sdf.parse(sdf.format(currentTime)).getTime());
			beforeTime = new Timestamp(sdf.parse(sdf.format(beforeTime)).getTime());
			
			/*
			 * FETCH ONLY PAST 30 DAYS RECORDS
			 */
			String sql="SELECT * FROM gms3_dmt_metadata_val_sch WHERE DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_ACTIVE+"' AND DC_SCHEDULE_TMSTP BETWEEN ? AND ? "
					+ " ORDER BY DC_SCHEDULE_ID DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setTimestamp(1, beforeTime);
			pstmt.setTimestamp(2, currentTime);
			rs = pstmt.executeQuery();
			MetaDataFileDetails details = new MetaDataFileDetails();
			while(rs.next())
			{
				if(null==scheduleList || scheduleList.size()<=0)
				{
					scheduleList = new ArrayList<MetaDataFileDetails>();
				}
				details = new MetaDataFileDetails();
				details.setSrNo(scheduleList.size()+1);
				details.setScheduleId(rs.getLong("DC_SCHEDULE_ID"));
				details.setScheduleName(rs.getString("DC_SCHEDULE_NAME"));
				details.setWslId(rs.getString("DC_USER_ID"));
				details.setStatus(rs.getString("DC_STATUS"));
				details.setStartTime(rs.getTimestamp("DC_SCHEDULE_TMSTP"));
				details.setFinishTime(rs.getTimestamp("DC_FINISH_TMSTP"));
				details.setTotalCount(rs.getInt("DC_TOTAL_COUNT"));
				details.setSuccessCount(rs.getInt("DC_PROCESSED_COUNT"));
				details.setFailureCount(rs.getInt("DC_FAILED_COUNT"));
				details.setMetaDataType(rs.getString("DC_METADATA_TYPE"));
				details.setFileName(rs.getString("DC_METADATA_FILE_NAME"));
				details.setFilePath(rs.getString("DC_METADATA_FILE_PATH"));
				details.setWebFilePath(com.mazda.gms3.dmt.utils.OkAssetsWeb.url(rs.getString("DC_METADATA_FILE_WEB_PATH")));
				details.setReportsPath(com.mazda.gms3.dmt.utils.OkAssetsWeb.url(rs.getString("DC_REPORTS_PATH")));
				details.setThreadId(rs.getString("DC_SCH_THREAD_ID"));
				details.setLocale(rs.getString("DC_LOCALE"));
				details.setSchdeuleRemarks(rs.getString("DC_REMARKS"));
				scheduleList.add(details);
				details = null;
			}
			details = null;
			sql=null;
			
			currentTime = null;
			beforeTime = null;
			sdf = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "getScheduleList()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return scheduleList;
	}

	public boolean updateScheduleStatus(String scheduleId, String status, String remarks)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=status && !"".equals(status))
			{
				conn = getConnection();
				/*
				 * IF STATUS IS PENDING / PROCESSING - DO NOT update finishTimestamp 
				 */
				if(null!=remarks && !"".equals(remarks))
				{
					if(remarks.length()>2000)
					{
						remarks = remarks.substring(0,1999);
					}
				}
				
				String sql="UPDATE gms3_dmt_metadata_val_sch SET DC_STATUS=? ,DC_REMARKS=?  ";
				if(status.equals(MetaDataScheduleConstants.STATUS_SUCCESS) || status.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					sql = sql+", DC_FINISH_TMSTP=? ";
				}
				sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, status);
				pstmt.setString(2, remarks);
				if(status.equals(MetaDataScheduleConstants.STATUS_SUCCESS) || status.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
					pstmt.setLong(4, new Long(scheduleId).longValue());
				}
				else
				{
					pstmt.setLong(3, new Long(scheduleId).longValue());
				}
				pstmt.executeUpdate();
				sql=null;
			}
			else
			{
				logger.info("updateScheduleStatus :: Schedule Id / Status as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "updateScheduleStatus()", e);
			return false;
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=pstmt)
				pstmt.close();
			if(null!=conn)
				conn.close();
		}
		return true;
	}

	public boolean updateScheduleReportsPath(String scheduleId, String reportsPath)
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId) && null!=reportsPath && !"".equals(reportsPath))
			{
				conn = getConnection();
				String sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_REPORTS_PATH=?  ";
				sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, reportsPath);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				sql=null;
				
				if(null!=pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			else
			{
				logger.info("updateScheduleReportsPath :: Schedule Id / Reports Path as parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "updateScheduleReportsPath()", e);
			return false;
		}
		return true;
	}
	
	public void updateScheduleTotalAndFailureCount(String scheduleId, int totalCount, int failureCount)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		conn = getConnection();
		/*
		 * IF STATUS IS PENDING / PROCESSING - DO NOT update finishTimestamp 
		 */
		String sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_TOTAL_COUNT=?,DC_FAILED_COUNT=?  ";
		sql=sql	+ " WHERE DC_SCHEDULE_ID=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, totalCount);
		pstmt.setInt(2, failureCount);
		pstmt.setLong(3, new Long(scheduleId).longValue());
		pstmt.executeUpdate();
		sql=null;
		if(null!=conn)
			conn.close();conn=null;
		if(null!=pstmt)
			pstmt.close();pstmt=null;
	}

	public ArrayList<VinDetails> checkMCMMEVINStatus(ArrayList<VinDetails> vinList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if(null!=vinList && !"".equals(vinList) && null!=locale && !"".equals(locale))
		{
//			locale = locale.replace("-", "_");
//			if(mmeLocales.indexOf(locale)>-1)
//			{
//				// MME MARKET LOCALE - CHECK FOR ALTERNATE LOCALE MAPPED TO PROCESSING LOCALE FOR CARLINE
////				locale = ApplicationProperties.getProperty("en-uk").trim();
//				locale = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"), AutoSyncConstants.STATUS_YES, AutoSyncConstants.ITEM_KEY_CARLINE, locale);
//				locale = locale.replace("_", "-").trim();
//			}
//			else
//			{
//				// user whatever value is passed for locale
//				locale = locale.replace("_", "-").trim();
//			}
			
			conn = getConnection();
			locale = locale.replace("_", "-");
			// get AlternateLocale mapped to procesisngLocale for CARLINE
			locale = ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_CARLINE, conn);
			
			VinDetails details = null;
			String sql="";
			String refKey="";
			String wmiCode="";
			StringBuilder errorMessage = new StringBuilder();
			for(int a=0;a<vinList.size();a++)
			{
				details = (VinDetails)vinList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
				// be default set values to N
				details.setVinFoundInMDM("N");
				details.setVinFoundInIM("N");
				if(null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_VALID))
				{
					errorMessage = new StringBuilder();
					boolean errorsFound=false;
					try
					{
						/*
						 *  if locale is of any of MME Marker, then use en_UK locale for checking in MDM
						 *  proceed
						 *  check carlineCode + wmiCode + vdsCode +visStartRange +visEndRange exists in MDM or not
						 */
						sql="SELECT MDM_VIN_ID FROM gms3_mdm_vin_detail WHERE TRIM(LOWER(MDM_VIN_WMI_CODE))=? AND TRIM(LOWER(MDM_VIN_VDS_CODE))=? "
								+ " AND TRIM(LOWER(MDM_VIN_VIS_START_RANGE))=? "
								+ " AND TRIM(LOWER(MDM_VIN_VIS_END_RANGE))=? AND TRIM(LOWER(MDM_CRLN_CODE))=? AND TRIM(LOWER(MDM_ML_LANG_CODE))=? AND MDM_VIN_FLAG=? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, details.getWmiCode().trim().toLowerCase());
						pstmt.setString(2, details.getVdsCode().trim().toLowerCase());
						pstmt.setString(3, details.getVisStartRange().trim().toLowerCase());
						pstmt.setString(4, details.getVisEndRange().trim().toLowerCase());
						pstmt.setString(5, details.getCarlineCode().trim().toLowerCase());
						pstmt.setString(6, locale.trim().toLowerCase());
						pstmt.setString(7, ApplicationProperties.getProperty("flag.value.active"));
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_VIN_ID") > 0)
							{
								// VIN FOUND IN MDM
								details.setVinFoundInMDM("Y");
							}
						}
						rs.close();rs=null;
						pstmt.close();pstmt=null;
						sql = null;

						if((null==details.getVinFoundInMDM()) || (null!=details.getVinFoundInMDM() && !"Y".equals(details.getVinFoundInMDM())))
						{
							// vin not found in MDM
							errorMessage.append("VIN does not exist in MDM.");
							// set errorsFound to true - status has to Failure for this row
							errorsFound = true;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkMCMMEVINStatus()", e);
						/*
						 * some exception in identifying the VIN FROM MDM
						 */
						errorMessage.append("Failed to find VIN in MDM, exception while executing Query ->"+ e.getMessage()+".");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}

					// proceed for IM 
					try
					{
						// proceed for checking in IM
						// PREPARE REFkEY FOR VIN
						if(details.getWmiCode().equals("-"))
						{
							wmiCode="___";
						}
						else
						{
							wmiCode = details.getWmiCode();
						}
						refKey = details.getCarlineCode()+wmiCode+details.getVdsCode()+details.getVisStartRange()+details.getVisEndRange();
						refKey = Utilities.replaceCharsForRefKeys(refKey).trim().toUpperCase();
						details.setVinRefKey(refKey.trim().toUpperCase());

						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, refKey);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								details.setVinFoundInIM("Y");
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;
						refKey = null;
						wmiCode =null;

						if((null==details.getVinFoundInIM()) || (null!=details.getVinFoundInIM() && !"Y".equals(details.getVinFoundInIM())))
						{
							// VIN Not found in IM.
							errorMessage.append("VIN does not exist in IM.");
							// set errorsFound to true - status has to Failure for this row
							errorsFound = true;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkMCMMEVINStatus()", e);
						/*
						 * some exception in identifying the VIN from IM
						 */
						errorMessage.append("Failed to find VIN in IM, exception while executing Query ->"+ e.getMessage()+".");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}


					// Check if Row does not any display Order Text file entry then failure
					if(null==details.getDisplayOrderTextFileName() || "".equals(details.getDisplayOrderTextFileName()))
					{
						errorMessage.append("No entry found for Display Order Text File.");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}

					if(errorsFound==true)
					{
						// update failureCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
						if(null!=errorMessage && null!=errorMessage.toString())
						{
							details.setErrorMessage(errorMessage.toString());
						}
					}
					else
					{
						// update successCount
						// VIN Located in MDM & IM + Display Order text File Entry Found - Success Transaction
						// update successCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
						// set processing status as SUCCESS
						details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
					}
					errorMessage=null;
				}
				else
				{
					// set error Message line is INVALID, SOME nodes are missing
					details.setErrorMessage("Line is INVALID. Required nodes are missing.");
					// NO NEED OF UPDATING THE FAILURE COUNT FOR THIS ONE - AS ALL FAILURE COUNT ALREADY UPDATED FOR INVALID LINES TOGETHER
				}
			}
			sql=null;
			wmiCode = null;
			refKey = null;
			details = null;
			errorMessage = null;
		}
		else
		{
			logger.info("checkMCMMEVINStatus :: VIN LIST / Locale as parameters are null. Return as it is.");
		}

		if(null!=rs)
			rs.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=conn)
			conn.close();
		return vinList;
	}
	
	public ArrayList<DisplayOrderDetails> checkDisplayOrderStatus(ArrayList<DisplayOrderDetails> displayOrderList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if(null!=displayOrderList && !"".equals(displayOrderList) && null!=locale && !"".equals(locale))
		{
			/*
			 * Iterate for each row and check if the line is VALID
			 * AND IS NOT ENG AT MT - CHEKC FOR ENGINE TYPE, MISSION TYPE, DRIVE ALXE & BODY TYPE REF KEYS IN IM
			 * AS WELL AS IN MDM.
			 */

//			String localeForEngMissDriveBodyTypesCheck=locale;
//			localeForEngMissDriveBodyTypesCheck = localeForEngMissDriveBodyTypesCheck.replace("-", "_");
//			if(mmeLocales.indexOf(localeForEngMissDriveBodyTypesCheck)>-1)
//			{
//				// MME MARKET LOCALE - CHECK FOR ALTERNATE LOCALE MAPPED TO PROCESSING LOCALE FOR CARLINE
////				localeForEngMissDriveBodyTypes = ApplicationProperties.getProperty("en-uk").trim();
//				localeForEng = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"), AutoSyncConstants.STATUS_YES, AutoSyncConstants.ITEM_KEY_ENGINE_TYPE, localeForEng);
//				localeForMission = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"), AutoSyncConstants.STATUS_YES, AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE, localeForMission);
//				localeForAxle = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"), AutoSyncConstants.STATUS_YES, AutoSyncConstants.ITEM_KEY_AXLE_TYPE, localeForAxle);
//				localeForBody = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"), AutoSyncConstants.STATUS_YES, AutoSyncConstants.ITEM_KEY_BODY_TYPE, localeForBody);
//				
//				localeForEng = localeForEng.replace("_", "-");
//				localeForMission = localeForMission.replace("_", "-");
//				localeForAxle = localeForAxle.replace("_", "-");
//				localeForBody = localeForBody.replace("_", "-");
//			}
//			else
//			{
//				// restore locale value and use as it is
////				localeForEngMissDriveBodyTypes = localeForEngMissDriveBodyTypes.replace("_", "-");
//				localeForEng = localeForEng.replace("_", "-");
//				localeForMission = localeForMission.replace("_", "-");
//				localeForAxle = localeForAxle.replace("_", "-");
//				localeForBody = localeForBody.replace("_", "-");
//			}

			conn = getConnection();
			
			// get Alternate Locales for each Master DATA Type
			String localeForEng=ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_ENGINE_TYPE, conn);
			String localeForMission=ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_TRANSMISSION_TYPE, conn);
			String localeForAxle = ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_AXLE_TYPE, conn);
			String localeForBody = ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_BODY_TYPE, conn);
			
			DisplayOrderDetails details = null;
			String sql="";
			StringBuilder errorMessage=new StringBuilder();
			String[] tokens=null;
			String errorMetaDataCodesMDM=null;
			String errorMetaDataCodesIM=null;
			for(int a=0;a<displayOrderList.size();a++)
			{
				details = (DisplayOrderDetails)displayOrderList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
				if(null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_VALID))
				{
					errorMessage=new StringBuilder();
					boolean errorsFound=false;

					// FETCH NAMES FOR EACH DISPLAY ORDER LEVEL CODES
					// NAME FOR LEVEL 1 CODE
					if(null!=details.getDisplayOrderLevel1Code() && !"".equals(details.getDisplayOrderLevel1Code()))
					{
						try
						{
							details.setDisplayOrderLevel1Name(getDisplayOrderName(details.getDisplayOrderLevel1Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel1Name() || "".equals(details.getDisplayOrderLevel1Name()))
							{
								errorMessage.append("Display Order Name for Level 1 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 1 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 2 CODE
					if(null!=details.getDisplayOrderLevel2Code() && !"".equals(details.getDisplayOrderLevel2Code()))
					{
						try
						{
							details.setDisplayOrderLevel2Name(getDisplayOrderName(details.getDisplayOrderLevel2Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel2Name() || "".equals(details.getDisplayOrderLevel2Name()))
							{
								errorMessage.append("Display Order Name for Level 2 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 2 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 3 CODE
					if(null!=details.getDisplayOrderLevel3Code() && !"".equals(details.getDisplayOrderLevel3Code()))
					{
						try
						{
							details.setDisplayOrderLevel3Name(getDisplayOrderName(details.getDisplayOrderLevel3Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel3Name() || "".equals(details.getDisplayOrderLevel3Name()))
							{
								errorMessage.append("Display Order Name for Level 3 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 3 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 4 CODE
					if(null!=details.getDisplayOrderLevel4Code() && !"".equals(details.getDisplayOrderLevel4Code()))
					{
						try
						{
							details.setDisplayOrderLevel4Name(getDisplayOrderName(details.getDisplayOrderLevel4Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel4Name() || "".equals(details.getDisplayOrderLevel4Name()))
							{
								errorMessage.append("Display Order Name for Level 4 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 4 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 5 CODE
					if(null!=details.getDisplayOrderLevel5Code() && !"".equals(details.getDisplayOrderLevel5Code()))
					{
						try
						{
							details.setDisplayOrderLevel5Name(getDisplayOrderName(details.getDisplayOrderLevel5Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel5Name() || "".equals(details.getDisplayOrderLevel5Name()))
							{
								errorMessage.append("Display Order Name for Level 5 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 5 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 6 CODE
					if(null!=details.getDisplayOrderLevel6Code() && !"".equals(details.getDisplayOrderLevel6Code()))
					{
						try
						{
							details.setDisplayOrderLevel6Name(getDisplayOrderName(details.getDisplayOrderLevel6Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel6Name() || "".equals(details.getDisplayOrderLevel6Name()))
							{
								errorMessage.append("Display Order Name for Level 6 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 6 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					/*
					 * NOW CHECK IF NOT ENGINE / AT / MT
					 * THEN ENGINE TYPE / MISSION TYPE / BODY TYPE / DRIVE AXLE TYPE MUST EXIST IN MDM AS WELL AS IM
					 */
					if(details.isEngineATMT()==false)
					{
						// MANUALS / WD
						if(null!=details.getEngineType() && !"".equals(details.getEngineType()))
						{
							errorMetaDataCodesMDM="";
							errorMetaDataCodesIM="";
							tokens = details.getEngineType().split(",");
							boolean found=false;
							if(null!=tokens && tokens.length>0)
							{
								for(int r=0;r<tokens.length;r++)
								{
									if(null!=tokens[r] && !"".equals(tokens[r].trim()) && !"e99".equals(tokens[r].trim().toLowerCase()))
									{
										// MDM CHECK
										try
										{
											sql="SELECT A.MDM_ET_ID FROM gms3_mdm_engine_type A, gms3_mdm_manual_language B WHERE A.MDM_ET_FLAG=? "
													+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
													+ " AND TRIM(LOWER(A.MDM_ET_TYPE_CODE))=?";
											pstmt = conn.prepareStatement(sql);
											pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
											pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
											pstmt.setString(3, localeForEng.trim().toLowerCase());
											pstmt.setString(4, tokens[r].trim().toLowerCase());
											rs = pstmt.executeQuery();
											if(rs.next())
											{
												if(rs.getLong("MDM_ET_ID")>0)
												{
													found = true;
												}
											}
											sql=null;
											pstmt.close();pstmt=null;
											rs.close();rs=null;

											if(found==false)
											{
												errorMessage.append("Engine Type {"+tokens[r]+"} does not exist in MDM.");
												// error found - status has to be failure for this row
												errorsFound=true;
												// add to errorMetaDataCodesMDM
												if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
												{
													errorMetaDataCodesMDM+=",";
												}
												errorMetaDataCodesMDM+=tokens[r];
											}
										}
										catch(Exception e1)
										{
											Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
											errorMessage.append("Failed to find Engine Type {"+tokens[r]+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesMDM
											if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
											{
												errorMetaDataCodesMDM+=",";
											}
											errorMetaDataCodesMDM+=tokens[r];
										}


										// IM CHECK
										found=false;
										try
										{
											sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
											pstmt = conn.prepareStatement(sql);
											pstmt.setString(1, Utilities.replaceCharsForRefKeys(tokens[r]).trim().toUpperCase());
											rs = pstmt.executeQuery();
											if(rs.next())
											{
												if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
												{
													found=true;
												}
											}
											pstmt.close();pstmt=null;
											rs.close();rs=null;
											sql=null;

											if(found==false)
											{
												errorMessage.append("Engine Type {"+tokens[r]+"} does not exist in IM.");
												// error found - status has to be failure for this row
												errorsFound=true;
												// add to errorMetaDataCodesIM
												if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
												{
													errorMetaDataCodesIM+=",";
												}
												errorMetaDataCodesIM+=tokens[r];
											}
										}
										catch(Exception e1)
										{
											Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
											errorMessage.append("Failed to find Engine Type {"+tokens[r]+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesIM
											if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
											{
												errorMetaDataCodesIM+=",";
											}
											errorMetaDataCodesIM+=tokens[r];
										}
									}
								}
							}

							tokens=null;
							if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
							{
								details.setFailedEngineTypeCodesMDM(errorMetaDataCodesMDM);
							}
							errorMetaDataCodesMDM = null;
							if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
							{
								details.setFailedEngineTypeCodesIM(errorMetaDataCodesIM);
							}
							errorMetaDataCodesIM=null;
						}


						// TRANSMISSION TYPES
						if(null!=details.getMissionType() && !"".equals(details.getMissionType()))
						{
							errorMetaDataCodesMDM="";
							errorMetaDataCodesIM="";
							tokens = details.getMissionType().split(",");
							boolean found=false;
							if(null!=tokens && tokens.length>0)
							{
								for(int r=0;r<tokens.length;r++)
								{
									if(null!=tokens[r] && !"".equals(tokens[r].trim()) && !"m99".equals(tokens[r].trim().toLowerCase()))
									{
										// MDM CHECK
										try
										{
											sql="SELECT A.MDM_TRANS_TYPE_ID FROM gms3_mdm_trans_type A, "
													+ " gms3_mdm_manual_language B WHERE A.MDM_TRANS_TYPE_FLAG=? AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID "
													+ " AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? AND TRIM(LOWER(A.MDM_TRANS_TYPE_TYPE_CODE))=? ";
											pstmt = conn.prepareStatement(sql);
											pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
											pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
											pstmt.setString(3, localeForMission.trim().toLowerCase());
											pstmt.setString(4, tokens[r].trim().toLowerCase());
											rs = pstmt.executeQuery();
											if(rs.next())
											{
												if(rs.getLong("MDM_TRANS_TYPE_ID")>0)
												{
													found = true;
												}
											}
											sql=null;
											pstmt.close();pstmt=null;
											rs.close();rs=null;

											if(found==false)
											{
												errorMessage.append("Transmission Type {"+tokens[r]+"} does not exist in MDM.");
												// error found - status has to be failure for this row
												errorsFound=true;
												// add to errorMetaDataCodesMDM
												if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
												{
													errorMetaDataCodesMDM+=",";
												}
												errorMetaDataCodesMDM+=tokens[r];
											}
										}
										catch(Exception e1)
										{
											Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
											errorMessage.append("Failed to find Transmission Type {"+tokens[r]+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesMDM
											if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
											{
												errorMetaDataCodesMDM+=",";
											}
											errorMetaDataCodesMDM+=tokens[r];
										}


										// IM CHECK
										found=false;
										try
										{
											sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
											pstmt = conn.prepareStatement(sql);
											pstmt.setString(1, Utilities.replaceCharsForRefKeys(tokens[r]).trim().toUpperCase());
											rs = pstmt.executeQuery();
											if(rs.next())
											{
												if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
												{
													found=true;
												}
											}
											pstmt.close();pstmt=null;
											rs.close();rs=null;
											sql=null;

											if(found==false)
											{
												errorMessage.append("Transmission Type {"+tokens[r]+"} does not exist in IM.");
												// error found - status has to be failure for this row
												errorsFound=true;
												// add to errorMetaDataCodesIM
												if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
												{
													errorMetaDataCodesIM+=",";
												}
												errorMetaDataCodesIM+=tokens[r];
											}
										}
										catch(Exception e1)
										{
											Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
											errorMessage.append("Failed to find Transmission Type {"+tokens[r]+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesIM
											if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
											{
												errorMetaDataCodesIM+=",";
											}
											errorMetaDataCodesIM+=tokens[r];
										}
									}
								}
							}

							tokens=null;
							if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
							{
								details.setFailedMissionTypeCodesMDM(errorMetaDataCodesMDM);
							}
							errorMetaDataCodesMDM = null;
							if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
							{
								details.setFailedMissionTypeCodesIM(errorMetaDataCodesIM);
							}
							errorMetaDataCodesIM=null;
						}

						// DRIVE AXLE
						if(null!=details.getDriveAxleType() && !"".equals(details.getDriveAxleType()))
						{
							errorMetaDataCodesMDM="";
							errorMetaDataCodesIM="";
							tokens = details.getDriveAxleType().split(",");
							boolean found=false;
							if(null!=tokens && tokens.length>0)
							{
								for(int r=0;r<tokens.length;r++)
								{
									// MDM CHECK
									try
									{
										sql="SELECT A.MDM_AT_ID FROM gms3_mdm_axle_type A, gms3_mdm_manual_language B WHERE A.MDM_AT_FLAG=? "
												+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
												+ " AND TRIM(LOWER(A.MDM_AT_AXLE_TYPE)) = ?";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
										pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
										pstmt.setString(3, localeForAxle.trim().toLowerCase());
										pstmt.setString(4, tokens[r].trim().toLowerCase());
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											if(rs.getLong("MDM_AT_ID")>0)
											{
												found = true;
											}
										}
										sql=null;
										pstmt.close();pstmt=null;
										rs.close();rs=null;

										if(found==false)
										{
											errorMessage.append("DriveAxle Type {"+tokens[r]+"} does not exist in MDM.");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesMDM
											if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
											{
												errorMetaDataCodesMDM+=",";
											}
											errorMetaDataCodesMDM+=tokens[r];
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
										errorMessage.append("Failed to find DriveAxle Type {"+tokens[r]+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
										// add to errorMetaDataCodesMDM
										if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
										{
											errorMetaDataCodesMDM+=",";
										}
										errorMetaDataCodesMDM+=tokens[r];
									}


									// IM CHECK
									found=false;
									try
									{
										sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1, Utilities.replaceCharsForRefKeys(tokens[r]).trim().toUpperCase());
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
											{
												found=true;
											}
										}
										pstmt.close();pstmt=null;
										rs.close();rs=null;
										sql=null;

										if(found==false)
										{
											errorMessage.append("DriveAxle Type {"+tokens[r]+"} does not exist in IM.");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesIM
											if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
											{
												errorMetaDataCodesIM+=",";
											}
											errorMetaDataCodesIM+=tokens[r];
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
										errorMessage.append("Failed to find DriveAxle Type {"+tokens[r]+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
										// add to errorMetaDataCodesIM
										if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
										{
											errorMetaDataCodesIM+=",";
										}
										errorMetaDataCodesIM+=tokens[r];
									}
								}
							}

							tokens=null;
							if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
							{
								details.setFailedDriveAxleTypeCodesMDM(errorMetaDataCodesMDM);
							}
							errorMetaDataCodesMDM = null;
							if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
							{
								details.setFailedDriveAxleTypeCodesIM(errorMetaDataCodesIM);
							}
							errorMetaDataCodesIM=null;
						}

						// BODY TYPE
						if(null!=details.getBodyType() && !"".equals(details.getBodyType()))
						{
							errorMetaDataCodesMDM="";
							errorMetaDataCodesIM="";
							tokens = details.getBodyType().split(",");
							boolean found=false;
							if(null!=tokens && tokens.length>0)
							{
								for(int r=0;r<tokens.length;r++)
								{
									// MDM CHECK
									try
									{
										sql="SELECT A.MDM_BT_ID FROM gms3_mdm_body_type A, gms3_mdm_manual_language B WHERE A.MDM_BT_FLAG=? "
												+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
												+ " AND TRIM(LOWER(A.MDM_BT_BODY_TYPE)) = ?";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
										pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
										pstmt.setString(3, localeForBody.trim().toLowerCase());
										pstmt.setString(4, tokens[r].trim().toLowerCase());
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											if(rs.getLong("MDM_BT_ID")>0)
											{
												found = true;
											}
										}
										sql=null;
										pstmt.close();pstmt=null;
										rs.close();rs=null;

										if(found==false)
										{
											errorMessage.append("Body Type {"+tokens[r]+"} does not exist in MDM.");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesMDM
											if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
											{
												errorMetaDataCodesMDM+=",";
											}
											errorMetaDataCodesMDM+=tokens[r];
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
										errorMessage.append("Failed to find Body Type {"+tokens[r]+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
										// add to errorMetaDataCodesMDM
										if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
										{
											errorMetaDataCodesMDM+=",";
										}
										errorMetaDataCodesMDM+=tokens[r];
									}


									// IM CHECK
									found=false;
									try
									{
										sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1, Utilities.replaceCharsForRefKeys(tokens[r]).trim().toUpperCase());
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
											{
												found=true;
											}
										}
										pstmt.close();pstmt=null;
										rs.close();rs=null;
										sql=null;

										if(found==false)
										{
											errorMessage.append("Body Type {"+tokens[r]+"} does not exist in IM.");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to errorMetaDataCodesIM
											if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
											{
												errorMetaDataCodesIM+=",";
											}
											errorMetaDataCodesIM+=tokens[r];
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkDisplayOrderStatus()", e1);
										errorMessage.append("Failed to find Body Type {"+tokens[r]+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
										// add to errorMetaDataCodesIM
										if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
										{
											errorMetaDataCodesIM+=",";
										}
										errorMetaDataCodesIM+=tokens[r];
									}
								}
							}

							tokens=null;
							if(null!=errorMetaDataCodesMDM && !"".equals(errorMetaDataCodesMDM))
							{
								details.setFailedBodyTypeCodesMDM(errorMetaDataCodesMDM);
							}
							errorMetaDataCodesMDM = null;
							if(null!=errorMetaDataCodesIM && !"".equals(errorMetaDataCodesIM))
							{
								details.setFailedBodyTypeCodesIM(errorMetaDataCodesIM);
							}
							errorMetaDataCodesIM=null;
						}
					}


					/*
					 * if errorsFound = true = updateFailureCount
					 * else = updateSuccessCount
					 */

					if(errorsFound==true)
					{
						// update failureCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
						if(null!=errorMessage && null!=errorMessage.toString())
						{
							details.setErrorMessage(errorMessage.toString());
						}
					}
					else
					{
						// all is success - update successCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
						// set processingStatus as SUCCESS
						details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
					}

					errorMessage = null;
				}
				else
				{
					// set error Message line is INVALID, SOME nodes are missing
					details.setErrorMessage("Line is INVALID. Required nodes are missing.");
					// NO NEED OF UPDATING THE FAILURE COUNT FOR THIS ONE - AS ALL FAILURE COUNT ALREADY UPDATED FOR INVALID LINES TOGETHER
				}
			}
			sql=null;
			details = null;
			errorMessage=null;
			tokens = null;
			errorMetaDataCodesMDM=null;
			errorMetaDataCodesIM=null;
			localeForEng=null;
			localeForMission=null;
			localeForAxle = null;
			localeForBody = null;
		}
		else
		{
			logger.info("checkDisplayOrderStatus :: DISPLAY ORDER LIST / Locale as parameters are null. Return as it is.");
		}

		if(null!=rs)
			rs.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=conn)
			conn.close();

		return displayOrderList;
	}
	
	public ArrayList<CDProcessingDetails> checkCDProcessingDisplayOrderStatus(ArrayList<CDProcessingDetails> cdProcessingList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		if(null!=cdProcessingList && !"".equals(cdProcessingList) && null!=locale && !"".equals(locale))
		{
			/*
			 * Iterate for each row and check if the line is VALID
			 * AS WELL AS IN MDM.
			 */
			conn = getConnection();
			CDProcessingDetails details = null;
			StringBuilder errorMessage=new StringBuilder();
			for(int a=0;a<cdProcessingList.size();a++)
			{
				details = (CDProcessingDetails)cdProcessingList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
				if(null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_VALID))
				{
					errorMessage=new StringBuilder();
					boolean errorsFound=false;

					// FETCH NAMES FOR EACH DISPLAY ORDER LEVEL CODES
					// NAME FOR LEVEL 1 CODE
					if(null!=details.getDisplayOrderLevel1Code() && !"".equals(details.getDisplayOrderLevel1Code()))
					{
						try
						{
							details.setDisplayOrderLevel1Name(getDisplayOrderName(details.getDisplayOrderLevel1Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel1Name() || "".equals(details.getDisplayOrderLevel1Name()))
							{
								errorMessage.append("Display Order Name for Level 1 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 1 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 2 CODE
					if(null!=details.getDisplayOrderLevel2Code() && !"".equals(details.getDisplayOrderLevel2Code()))
					{
						try
						{
							details.setDisplayOrderLevel2Name(getDisplayOrderName(details.getDisplayOrderLevel2Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel2Name() || "".equals(details.getDisplayOrderLevel2Name()))
							{
								errorMessage.append("Display Order Name for Level 2 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 2 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 3 CODE
					if(null!=details.getDisplayOrderLevel3Code() && !"".equals(details.getDisplayOrderLevel3Code()))
					{
						try
						{
							details.setDisplayOrderLevel3Name(getDisplayOrderName(details.getDisplayOrderLevel3Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel3Name() || "".equals(details.getDisplayOrderLevel3Name()))
							{
								errorMessage.append("Display Order Name for Level 3 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 3 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 4 CODE
					if(null!=details.getDisplayOrderLevel4Code() && !"".equals(details.getDisplayOrderLevel4Code()))
					{
						try
						{
							details.setDisplayOrderLevel4Name(getDisplayOrderName(details.getDisplayOrderLevel4Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel4Name() || "".equals(details.getDisplayOrderLevel4Name()))
							{
								errorMessage.append("Display Order Name for Level 4 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 4 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 5 CODE
					if(null!=details.getDisplayOrderLevel5Code() && !"".equals(details.getDisplayOrderLevel5Code()))
					{
						try
						{
							details.setDisplayOrderLevel5Name(getDisplayOrderName(details.getDisplayOrderLevel5Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel5Name() || "".equals(details.getDisplayOrderLevel5Name()))
							{
								errorMessage.append("Display Order Name for Level 5 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 5 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// NAME FOR LEVEL 6 CODE
					if(null!=details.getDisplayOrderLevel6Code() && !"".equals(details.getDisplayOrderLevel6Code()))
					{
						try
						{
							details.setDisplayOrderLevel6Name(getDisplayOrderName(details.getDisplayOrderLevel6Code(), details.getModelType(), locale, conn));
							if(null==details.getDisplayOrderLevel6Name() || "".equals(details.getDisplayOrderLevel6Name()))
							{
								errorMessage.append("Display Order Name for Level 6 Code, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkCDProcessingDisplayOrderStatus()", e1);
							errorMessage.append("Failed to fetch Display Order Name for Level 6 Code, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					// check for Sequence no, in-case of CD Sequence has to be mandatory.
					if(null==details.getSequenceNo() || "".equals(details.getSequenceNo()))
					{
						errorMessage.append("No Sequence number found.");
						// error found - status has to be failure for this row
						errorsFound=true;
					}

					/*
					 * if errorsFound = true = updateFailureCount
					 * else = updateSuccessCount
					 */

					if(errorsFound==true)
					{
						// update failureCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
						if(null!=errorMessage && null!=errorMessage.toString())
						{
							details.setErrorMessage(errorMessage.toString());
						}
					}
					else
					{
						// all is success - update successCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
						// set processingStatus as SUCCESS
						details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
					}

					errorMessage = null;
				}
				else
				{
					// set error Message line is INVALID, SOME nodes are missing
					details.setErrorMessage("Line is INVALID. Required nodes are missing.");
					// NO NEED OF UPDATING THE FAILURE COUNT FOR THIS ONE - AS ALL FAILURE COUNT ALREADY UPDATED FOR INVALID LINES TOGETHER
				}
			}
			details = null;
			errorMessage=null;
		}
		else
		{
			logger.info("checkCDProcessingDisplayOrderStatus :: CD PROCESSING LIST / Locale as parameters are null. Return as it is.");
		}

		if(null!=conn)
			conn.close();
		return cdProcessingList;
	}
	
	public ArrayList<ESICategoryDetails> checkESICategoryStatus(ArrayList<ESICategoryDetails> esiCategoryList, String locale, String scheduleId, String metaDataType)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if(null!=esiCategoryList && !"".equals(esiCategoryList) && null!=locale && !"".equals(locale))
		{
			/*
			 * Iterate for each row and check if the line is VALID
			 * AS WELL AS IN MDM.
			 */
			conn = getConnection();
			ESICategoryDetails details = null;
			StringBuilder errorMessage=new StringBuilder();
			String refKey="";
			String sql="";
			String recordId="";
			String[] tokens=null;
			String failedStrTypes=null;
			String steeringTypeRefKey=null;
			for(int a=0;a<esiCategoryList.size();a++)
			{
				details = (ESICategoryDetails)esiCategoryList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
				if(null!=details.getLineType() && details.getLineType().equals(MetaDataValidationUtils.LINE_TYPE_VALID))
				{
					errorMessage=new StringBuilder();
					boolean errorsFound=false;
					/*
					 * Not Applicable for ESICATEGORY_MANUALS_OLD / ESICATEGORY_WD_OLD / ESICATEGORY_OLD
					 */
					if(!metaDataType.equals("ESICATEGORY_MANUALS_OLD") && !metaDataType.equals("ESICATEGORY_WD_OLD") && !metaDataType.equals("ESICATEGORY_OLD"))
					{
						// FETCH NAMES FOR EACH ESI CATEGORY CODES AND CHECK IF THEY EXIST IN IM OR NOT
						// NAME FOR LEVEL 1 CODE
						if(null!=details.getCategoryLevel1Code() && !"".equals(details.getCategoryLevel1Code()))
						{
							// MDM CHECK
							// set defaultValue for MDM
							details.setCode1ExistsInMDM("N");
							try
							{
								details.setCategoryLevel1Name(getESICategoryName("LEVEL1", details.getCategoryLevel1Code(), "", "", locale, conn));
								if(null==details.getCategoryLevel1Name() || "".equals(details.getCategoryLevel1Name()))
								{
									errorMessage.append("ESI Category Level 1, does not exist in MDM.");
									// error found - status has to be failure for this row
									errorsFound=true;
								}
								else
								{
									// CODE 1 EXISTS IN MDM
									details.setCode1ExistsInMDM("Y");
								}
							}
							catch(Exception e1)
							{
								Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
								errorMessage.append("Failed to fetch ESI Category Level 1 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
								// error found - status has to be failure for this row
								errorsFound=true;
							}

							// IM CHECK
							// set defaultValue for IM
							details.setCode1ExistsInIM("N");
							try
							{
								refKey="ESI"+details.getCategoryLevel1Code().trim().toUpperCase();
								details.setCode1RefKey(refKey);
								sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";								
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1,refKey);
								rs = pstmt.executeQuery();
								if(rs.next())
								{
									recordId = rs.getString("RECORDID");

								}
								pstmt.close();pstmt=null;
								rs.close();rs=null;
								sql=null;
								refKey = null;

								if(null!=recordId && !"".equals(recordId))
								{
									// refKey exists in IM
									details.setCode1ExistsInIM("Y");
								}
								else
								{
									errorMessage.append("ESI Category Level 1, does not exist in IM.");
									// error found - status has to be failure for this row
									errorsFound=true;
								}
							}
							catch(Exception e1)
							{
								Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
								errorMessage.append("Failed to fetch ESI Category Level 1 from IM, exception while executing Query ->"+ e1.getMessage()+".");
								// error found - status has to be failure for this row
								errorsFound=true;
							}

							recordId=null;

							// LEVEL 2 CODE
							if(null!=details.getCategoryLevel2Code() && !"".equals(details.getCategoryLevel2Code()))
							{
								// MDM CHECK
								// set defaultValue for MDM
								details.setCode2ExistsInMDM("N");
								try
								{
									details.setCategoryLevel2Name(getESICategoryName("LEVEL2", details.getCategoryLevel1Code(), details.getCategoryLevel2Code(), "", locale, conn));
									if(null==details.getCategoryLevel2Name() || "".equals(details.getCategoryLevel2Name()))
									{
										errorMessage.append("ESI Category Level 2, does not exist in MDM.");
										// error found - status has to be failure for this row
										errorsFound=true;
									}
									else
									{
										// CODE 2 EXISTS IN MDM
										details.setCode2ExistsInMDM("Y");
									}
								}
								catch(Exception e1)
								{
									Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
									errorMessage.append("Failed to fetch ESI Category Level 2 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
									// error found - status has to be failure for this row
									errorsFound=true;
								}

								// IM CHECK
								// set defaultValue for IM
								details.setCode2ExistsInIM("N");
								try
								{
									refKey=details.getCategoryLevel2Code().trim().toUpperCase();
									details.setCode2RefKey(refKey);
									sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
									pstmt = conn.prepareStatement(sql);
									pstmt.setString(1,refKey);
									rs = pstmt.executeQuery();
									if(rs.next())
									{
										recordId = rs.getString("RECORDID");

									}
									pstmt.close();pstmt=null;
									rs.close();rs=null;
									sql=null;
									refKey = null;

									if(null!=recordId && !"".equals(recordId))
									{
										// refKey exists in IM
										details.setCode2ExistsInIM("Y");
									}
									else
									{
										errorMessage.append("ESI Category Level 2, does not exist in IM.");
										// error found - status has to be failure for this row
										errorsFound=true;
									}
								}
								catch(Exception e1)
								{
									Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
									errorMessage.append("Failed to fetch ESI Category Level 2 from IM, exception while executing Query ->"+ e1.getMessage()+".");
									// error found - status has to be failure for this row
									errorsFound=true;
								}

								recordId=null;

								// LEVEL 3 CODE - NOT NULL & NOT 00
								if(null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code()) && !"00".equals(details.getCategoryLevel3Code()))
								{
									// MDM CHECK
									// set defaultValue for MDM
									details.setCode3ExistsInMDM("N");
									try
									{
										details.setCategoryLevel3Name(getESICategoryName("LEVEL3", details.getCategoryLevel1Code(), details.getCategoryLevel2Code(), details.getCategoryLevel3Code(), locale, conn));
										if(null==details.getCategoryLevel3Name() || "".equals(details.getCategoryLevel3Name()))
										{
											errorMessage.append("ESI Category Level 3, does not exist in MDM.");
											// error found - status has to be failure for this row
											errorsFound=true;
										}
										else
										{
											// CODE 3 EXISTS IN MDM
											details.setCode3ExistsInMDM("Y");
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
										errorMessage.append("Failed to fetch ESI Category Level 3 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
									}

									// IM CHECK
									// set defaultValue for IM
									details.setCode3ExistsInIM("N");
									try
									{
										refKey=details.getCategoryLevel2Code().trim().toUpperCase()+details.getCategoryLevel3Code().trim().toUpperCase();
										details.setCode3RefKey(refKey);
										sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1,refKey);
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											recordId = rs.getString("RECORDID");

										}
										pstmt.close();pstmt=null;
										rs.close();rs=null;
										sql=null;
										refKey = null;

										if(null!=recordId && !"".equals(recordId))
										{
											// refKey exists in IM
											details.setCode3ExistsInIM("Y");
										}
										else
										{
											errorMessage.append("ESI Category Level 3, does not exist in IM.");
											// error found - status has to be failure for this row
											errorsFound=true;
										}
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
										errorMessage.append("Failed to fetch ESI Category Level 3 from IM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
									}

									recordId = null;
								}
							}
						}
						else
						{
							// error found - at least First Level ESI Category code is mandatory
							errorMessage.append("No ESI Category Codes found.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}

					
					// check for Title, in-case of ESI File - Title has to be mandatory.
					if(null==details.getTitle() || "".equals(details.getTitle()))
					{
						errorMessage.append("No Title found.");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
					
					/*
					 *	CHECK IF METADATA TYPE IS ESICATEGORY_WD_NEW / ESICATEGORY_WD_OLD, CHECK FOR STEERING TYPES EXIST IN IM OR NOT  
					 */
					if(metaDataType.equals("ESICATEGORY_WD_NEW") || metaDataType.equals("ESICATEGORY_WD_OLD"))
					{
						if(null!=details.getSteeringTypeInfoText() && !"".equals(details.getSteeringTypeInfoText()))
						{
							tokens = details.getSteeringTypeInfoText().split(",");
							if(null!=tokens && tokens.length>0)
							{
								failedStrTypes="";
								for(int w=0;w<tokens.length;w++)
								{
									// check for each Steering Type exists in IM or NOT
									steeringTypeRefKey="";
									recordId=null;
									try
									{
										if(null!=tokens[w] && tokens[w].trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.lhd")))
										{
											steeringTypeRefKey=ApplicationProperties.getProperty("steering.type.refkey.lhd");
										}
										else if(null!=tokens[w] && tokens[w].trim().toLowerCase().equals(ApplicationProperties.getProperty("steering.type.label.rhd")))
										{
											steeringTypeRefKey=ApplicationProperties.getProperty("steering.type.refkey.rhd");
										}
										
										sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY = ?";
										pstmt = conn.prepareStatement(sql);
										pstmt.setString(1, steeringTypeRefKey);
										rs = pstmt.executeQuery();
										if(rs.next())
										{
											recordId = rs.getString("RECORDID");
										}
										sql=null;
										pstmt.close();pstmt=null;
										rs.close();rs=null;
										steeringTypeRefKey=null;
										
										if(null==recordId || "".equals(recordId))
										{
											errorMessage.append("Steering Type {"+tokens[w]+"} does not exist in IM.");
											// error found - status has to be failure for this row
											errorsFound=true;
											// add to failedSteeringTypes
											if(null!=failedStrTypes && !"".equals(failedStrTypes))
											{
												failedStrTypes+=",";
											}
											failedStrTypes+=tokens[w];
										}
										recordId=null;
									}
									catch(Exception e1)
									{
										Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkESICategoryStatus()", e1);
										errorMessage.append("Failed to fetch Steering Type {"+tokens[w]+"}  from IM, exception while executing Query ->"+ e1.getMessage()+".");
										// error found - status has to be failure for this row
										errorsFound=true;
										// add to failedSteeringTypes
										if(null!=failedStrTypes && !"".equals(failedStrTypes))
										{
											failedStrTypes+=",";
										}
										failedStrTypes+=tokens[w];
									}
								}
							}
							tokens = null;
							
							if(null!=failedStrTypes && !"".equals(failedStrTypes))
							{
								details.setFailedSteeringTypeInIM(failedStrTypes);
							}
							failedStrTypes = null;
						}
					}
					/*
					 * if errorsFound = true = updateFailureCount
					 * else = updateSuccessCount
					 */
					
					if(errorsFound==true)
					{
						// update failureCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
						if(null!=errorMessage && null!=errorMessage.toString())
						{
							details.setErrorMessage(errorMessage.toString());
						}
					}
					else
					{
						// all is success - update successCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
						// set processingStatus as SUCCESS
						details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
					}
					
					errorMessage = null;
				}
				else
				{
					// set error Message line is INVALID, SOME nodes are missing
					details.setErrorMessage("Line is INVALID. Required nodes are missing.");
					// NO NEED OF UPDATING THE FAILURE COUNT FOR THIS ONE - AS ALL FAILURE COUNT ALREADY UPDATED FOR INVALID LINES TOGETHER
				}
			}
			refKey=null;
			sql=null;
			recordId=null;
			details = null;
			errorMessage=null;
			steeringTypeRefKey=null;
			failedStrTypes=null;
			tokens=null;
		}
		else
		{
			logger.info("checkESICategoryStatus :: ESI CATEGORY LIST / Locale as parameters are null. Return as it is.");
		}
		
		if(null!=conn)
			conn.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=rs)
			rs.close();
		return esiCategoryList;
	}
	
	public ArrayList<LeftMenuFileDetails> checkLeftMenuStatus(ArrayList<LeftMenuFileDetails> leftMenuList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if(null!=leftMenuList && leftMenuList.size()>0 && null!=locale && !"".equals(locale))
		{
			/*
			 * Iterate for each row and check if the line is VALID
			 * AS WELL AS IN MDM.
			 */
			conn = getConnection();
			LeftMenuFileDetails details = null;
			StringBuilder errorMessage=new StringBuilder();
			String refKey="";
			String sql="";
			String recordId="";
			for(int a=0;a<leftMenuList.size();a++)
			{
				details = (LeftMenuFileDetails)leftMenuList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);

				errorMessage=new StringBuilder();
				boolean errorsFound=false;

				// FETCH NAMES FOR EACH ESI CATEGORY CODES AND CHECK IF THEY EXIST IN IM OR NOT
				// NAME FOR LEVEL 1 CODE
				if(null!=details.getCategoryCode() && !"".equals(details.getCategoryCode()))
				{
					// MDM CHECK
					// set defaultValue for MDM
					details.setCode1ExistsInMDM("N");
					try
					{
						details.setCategoryLevel1Name(getESICategoryName("LEVEL1", details.getCategoryCode(), "", "", locale, conn));
						if(null==details.getCategoryLevel1Name() || "".equals(details.getCategoryLevel1Name()))
						{
							errorMessage.append("ESI Category Level 1, does not exist in MDM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
						else
						{
							// CODE 1 EXISTS IN MDM
							details.setCode1ExistsInMDM("Y");
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
						errorMessage.append("Failed to fetch ESI Category Level 1 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
					
					// IM CHECK
					// set defaultValue for IM
					details.setCode1ExistsInIM("N");
					try
					{
						refKey="ESI"+details.getCategoryCode().trim().toUpperCase();
						details.setCode1RefKey(refKey);
						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";								
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1,refKey);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							recordId = rs.getString("RECORDID");
							
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;
						refKey = null;
						
						if(null!=recordId && !"".equals(recordId))
						{
							// refKey exists in IM
							details.setCode1ExistsInIM("Y");
						}
						else
						{
							errorMessage.append("ESI Category Level 1, does not exist in IM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
						errorMessage.append("Failed to fetch ESI Category Level 1 from IM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
					
					recordId=null;
					
					// LEVEL 2 CODE
					if(null!=details.getSubCategoryCode() && !"".equals(details.getSubCategoryCode()))
					{
						// MDM CHECK
						// set defaultValue for MDM
						details.setCode2ExistsInMDM("N");
						try
						{
							details.setCategoryLevel2Name(getESICategoryName("LEVEL2", details.getCategoryCode(), details.getSubCategoryCode(), "", locale, conn));
							if(null==details.getCategoryLevel2Name() || "".equals(details.getCategoryLevel2Name()))
							{
								errorMessage.append("ESI Category Level 2, does not exist in MDM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
							else
							{
								// CODE 2 EXISTS IN MDM
								details.setCode2ExistsInMDM("Y");
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
							errorMessage.append("Failed to fetch ESI Category Level 2 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
						
						// IM CHECK
						// set defaultValue for IM
						details.setCode2ExistsInIM("N");
						try
						{
							refKey=details.getSubCategoryCode().trim().toUpperCase();
							details.setCode2RefKey(refKey);
							sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
							pstmt = conn.prepareStatement(sql);
							pstmt.setString(1,refKey);
							rs = pstmt.executeQuery();
							if(rs.next())
							{
								recordId = rs.getString("RECORDID");
								
							}
							pstmt.close();pstmt=null;
							rs.close();rs=null;
							sql=null;
							refKey = null;
							
							if(null!=recordId && !"".equals(recordId))
							{
								// refKey exists in IM
								details.setCode2ExistsInIM("Y");
							}
							else
							{
								errorMessage.append("ESI Category Level 2, does not exist in IM.");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
						}
						catch(Exception e1)
						{
							Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
							errorMessage.append("Failed to fetch ESI Category Level 2 from IM, exception while executing Query ->"+ e1.getMessage()+".");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
						
						recordId=null;
						
						// LEVEL 3 CODE - NOT NULL & NOT 00
						if(null!=details.getSubSubCategoryCode() && !"".equals(details.getSubSubCategoryCode()) && !"00".equals(details.getSubSubCategoryCode()))
						{
							// MDM CHECK
							// set defaultValue for MDM
							details.setCode3ExistsInMDM("N");
							try
							{
								details.setCategoryLevel3Name(getESICategoryName("LEVEL3", details.getCategoryCode(), details.getSubCategoryCode(), details.getSubSubCategoryCode(), locale, conn));
								if(null==details.getCategoryLevel3Name() || "".equals(details.getCategoryLevel3Name()))
								{
									errorMessage.append("ESI Category Level 3, does not exist in MDM.");
									// error found - status has to be failure for this row
									errorsFound=true;
								}
								else
								{
									// CODE 3 EXISTS IN MDM
									details.setCode3ExistsInMDM("Y");
								}
							}
							catch(Exception e1)
							{
								Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
								errorMessage.append("Failed to fetch ESI Category Level 3 from MDM, exception while executing Query ->"+ e1.getMessage()+".");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
							
							// IM CHECK
							// set defaultValue for IM
							details.setCode3ExistsInIM("N");
							try
							{
								refKey=details.getSubCategoryCode().trim().toUpperCase()+details.getSubSubCategoryCode().trim().toUpperCase();
								details.setCode3RefKey(refKey);
								sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1,refKey);
								rs = pstmt.executeQuery();
								if(rs.next())
								{
									recordId = rs.getString("RECORDID");
									
								}
								pstmt.close();pstmt=null;
								rs.close();rs=null;
								sql=null;
								refKey = null;
								
								if(null!=recordId && !"".equals(recordId))
								{
									// refKey exists in IM
									details.setCode3ExistsInIM("Y");
								}
								else
								{
									errorMessage.append("ESI Category Level 3, does not exist in IM.");
									// error found - status has to be failure for this row
									errorsFound=true;
								}
							}
							catch(Exception e1)
							{
								Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkLeftMenuStatus()", e1);
								errorMessage.append("Failed to fetch ESI Category Level 3 from IM, exception while executing Query ->"+ e1.getMessage()+".");
								// error found - status has to be failure for this row
								errorsFound=true;
							}
							
							recordId = null;
						}
					}
				}
				else
				{
					// error found - at least First Level ESI Category code is mandatory
					errorMessage.append("No ESI Category Codes found.");
					// error found - status has to be failure for this row
					errorsFound=true;
				}

				
				// check for Title, in-case of LEFT MENU File - Title has to be mandatory.
				if(null==details.getTitle() || "".equals(details.getTitle()))
				{
					errorMessage.append("No Title found.");
					// error found - status has to be failure for this row
					errorsFound=true;
				}
				
				/*
				 * if errorsFound = true = updateFailureCount
				 * else = updateSuccessCount
				 */
				
				if(errorsFound==true)
				{
					// update failureCount
					updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
					if(null!=errorMessage && null!=errorMessage.toString())
					{
						details.setErrorMessage(errorMessage.toString());
					}
				}
				else
				{
					// all is success - update successCount
					updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
					// set processingStatus as SUCCESS
					details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				}
				errorMessage = null;
			}
			refKey=null;
			sql=null;
			recordId=null;
			details = null;
			errorMessage=null;
		}
		else
		{
			logger.info("checkLeftMenuStatus :: LEFT MENU LIST / Locale as parameters are null. Return as it is.");
		}
		
		if(null!=conn)
			conn.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=rs)
			rs.close();
		return leftMenuList;
	}
	
	public ArrayList<VINEntFileDetails> checkMNAOVINStatus(ArrayList<VINEntFileDetails> vinList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if(null!=vinList && !"".equals(vinList) && null!=locale && !"".equals(locale))
		{
			conn = getConnection();
			locale = locale.replace("-", "_");
			if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en-ca").trim().toLowerCase()))
			{
				// LOCALE - USE en_US
				locale = ApplicationProperties.getProperty("en-us").trim();
				locale = locale.replace("_", "-").trim();
			}
			else
			{
				// user whatever value is passed for locale
				locale = locale.replace("_", "-").trim();
			}
			
			
			VINEntFileDetails details = null;
			String sql="";
			String refKey="";
			StringBuilder errorMessage = new StringBuilder();
			for(int a=0;a<vinList.size();a++)
			{
				details = (VINEntFileDetails)vinList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);
				// be default set values to N
				details.setVinFoundInMDM("N");
				details.setVinFoundInIM("N");
				if(null!=details.getVinWMI() && !"".equals(details.getVinWMI()) && null!=details.getVinVDS() && !"".equals(details.getVinVDS()) &&  
					null!=details.getVinStartRange() && !"".equals(details.getVinStartRange()) && null!=details.getVinEndRange() && !"".equals(details.getVinEndRange())
					&& null!=details.getVinCarline() && !"".equals(details.getVinCarline()))
				{
					errorMessage = new StringBuilder();
					boolean errorsFound=false;
					try
					{
						/*
						 *  if locale is en_CA, then use en_US locale for checking in MDM
						 *  proceed
						 *  check carlineCode + wmiCode + vdsCode +visStartRange +visEndRange exists in MDM or not
						 */
						sql="SELECT MDM_VIN_ID FROM gms3_mdm_vin_detail WHERE TRIM(LOWER(MDM_VIN_WMI_CODE))=? AND TRIM(LOWER(MDM_VIN_VDS_CODE))=? "
								+ " AND TRIM(LOWER(MDM_VIN_VIS_START_RANGE))=? "
								+ " AND TRIM(LOWER(MDM_VIN_VIS_END_RANGE))=? AND TRIM(LOWER(MDM_CRLN_CODE))=? AND TRIM(LOWER(MDM_ML_LANG_CODE))=? AND MDM_VIN_FLAG=? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, details.getVinWMI().trim().toLowerCase());
						pstmt.setString(2, details.getVinVDS().trim().toLowerCase());
						pstmt.setString(3, details.getVinStartRange().trim().toLowerCase());
						pstmt.setString(4, details.getVinEndRange().trim().toLowerCase());
						pstmt.setString(5, details.getVinCarline().trim().toLowerCase());
						pstmt.setString(6, locale.trim().toLowerCase());
						pstmt.setString(7, ApplicationProperties.getProperty("flag.value.active"));
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_VIN_ID") > 0)
							{
								// VIN FOUND IN MDM
								details.setVinFoundInMDM("Y");
							}
						}
						rs.close();rs=null;
						pstmt.close();pstmt=null;
						sql = null;

						if((null==details.getVinFoundInMDM()) || (null!=details.getVinFoundInMDM() && !"Y".equals(details.getVinFoundInMDM())))
						{
							// vin not found in MDM
							errorMessage.append("VIN does not exist in MDM.");
							// set errorsFound to true - status has to Failure for this row
							errorsFound = true;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkMNAOVINStatus()", e);
						/*
						 * some exception in identifying the VIN FROM MDM
						 */
						errorMessage.append("Failed to find VIN in MDM, exception while executing Query ->"+ e.getMessage()+".");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}

					// proceed for IM 
					try
					{
						// proceed for checking in IM
						// PREPARE REFkEY FOR VIN
						refKey = details.getVinWMI().trim()+details.getVinVDS().trim()+details.getVinStartRange().trim()+details.getVinEndRange().trim();
						refKey = Utilities.replaceCharsForRefKeys(refKey).trim().toUpperCase();
						details.setVinRefKey(refKey.trim().toUpperCase());

						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, refKey);
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								details.setVinFoundInIM("Y");
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;
						refKey = null;

						if((null==details.getVinFoundInIM()) || (null!=details.getVinFoundInIM() && !"Y".equals(details.getVinFoundInIM())))
						{
							// VIN Not found in IM.
							errorMessage.append("VIN does not exist in IM.");
							// set errorsFound to true - status has to Failure for this row
							errorsFound = true;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkMNAOVINStatus()", e);
						/*
						 * some exception in identifying the VIN from IM
						 */
						errorMessage.append("Failed to find VIN in IM, exception while executing Query ->"+ e.getMessage()+".");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}


					// Check if Row does not have any Model year entry then failure
					if(null==details.getModelYear() || "".equals(details.getModelYear()))
					{
						errorMessage.append("No entry found for Model Year in the node.");
						// set errorsFound to true - status has to Failure for this row
						errorsFound = true;
					}

					if(errorsFound==true)
					{
						// update failureCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
						if(null!=errorMessage && null!=errorMessage.toString())
						{
							details.setErrorMessage(errorMessage.toString());
						}
					}
					else
					{
						// update successCount
						// VIN Located in MDM & IM + Model Year Entry Found - Success Transaction
						// update successCount
						updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
						// set processing status as SUCCESS
						details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
					}
				}
			else
			{
				// set error Message Node is INVALID, SOME elements are missing
				details.setErrorMessage("Node is INVALID. Required elements are missing.");
				// update failureCount
				updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
			}
				
				errorMessage=null;
			}
			sql=null;
			refKey = null;
			details = null;
			errorMessage = null;
		}
		else
		{
			logger.info("checkMNAOVINStatus :: VIN LIST / Locale as parameters are null. Return as it is.");
		}

		if(null!=rs)
			rs.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=conn)
			conn.close();
		return vinList;
	}
	
	public ArrayList<VinAttributeEntFileDetails> checkVINAttributeStatus(ArrayList<VinAttributeEntFileDetails> vinAttributeList, String locale, String scheduleId)throws SQLException
	{
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if(null!=vinAttributeList && !"".equals(vinAttributeList) && null!=locale && !"".equals(locale))
		{
			/*
			 * CHECK FOR ENGINE TYPE, MISSION TYPE, DRIVE ALXE & BODY TYPE REF KEYS IN IM
			 * AS WELL AS IN MDM.
			 */
			locale = locale.replace("_", "-");
			
			conn = getConnection();
			VinAttributeEntFileDetails details = null;
			String sql="";
			StringBuilder errorMessage=new StringBuilder();
			for(int a=0;a<vinAttributeList.size();a++)
			{
				details = (VinAttributeEntFileDetails)vinAttributeList.get(a);
				// set defaultStatus as Failure
				details.setProcessingStatus(MetaDataScheduleConstants.STATUS_FAILURE);

				errorMessage=new StringBuilder();
				boolean errorsFound=false;

				/*
				 * ENGINE TYPE / MISSION TYPE / BODY TYPE / DRIVE AXLE TYPE MUST EXIST IN MDM AS WELL AS IM
				 */
				if(null!=details.getVinEngineType() && !"".equals(details.getVinEngineType()))
				{
					boolean found=false;

					// MDM CHECK
					try
					{
						sql="SELECT A.MDM_ET_ID FROM gms3_mdm_engine_type A, gms3_mdm_manual_language B WHERE A.MDM_ET_FLAG=? "
								+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
								+ " AND TRIM(LOWER(A.MDM_ET_TYPE_CODE))=?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(3, locale.trim().toLowerCase());
						pstmt.setString(4, details.getVinEngineType().trim().toLowerCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_ET_ID")>0)
							{
								found = true;
							}
						}
						sql=null;
						pstmt.close();pstmt=null;
						rs.close();rs=null;

						if(found==false)
						{
							errorMessage.append("Engine Type {"+details.getVinEngineType()+"} does not exist in MDM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Engine Type {"+details.getVinEngineType()+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}


					// IM CHECK
					found=false;
					try
					{
						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, Utilities.replaceCharsForRefKeys(details.getVinEngineType()).trim().toUpperCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								found=true;
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;

						if(found==false)
						{
							errorMessage.append("Engine Type {"+details.getVinEngineType()+"} does not exist in IM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Engine Type {"+details.getVinEngineType()+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
				
				}


				// TRANSMISSION TYPES
				if(null!=details.getVinTransType() && !"".equals(details.getVinTransType()))
				{
					boolean found=false;

					// MDM CHECK
					try
					{
						sql="SELECT A.MDM_TRANS_TYPE_ID FROM gms3_mdm_trans_type A, "
								+ " gms3_mdm_manual_language B WHERE A.MDM_TRANS_TYPE_FLAG=? AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID "
								+ " AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? AND TRIM(LOWER(A.MDM_TRANS_TYPE_TYPE_CODE))=? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(3, locale.trim().toLowerCase());
						pstmt.setString(4, details.getVinTransType().trim().toLowerCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_TRANS_TYPE_ID")>0)
							{
								found = true;
							}
						}
						sql=null;
						pstmt.close();pstmt=null;
						rs.close();rs=null;

						if(found==false)
						{
							errorMessage.append("Transmission Type {"+details.getVinTransType()+"} does not exist in MDM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Transmission Type {"+details.getVinTransType()+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}


					// IM CHECK
					found=false;
					try
					{
						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, Utilities.replaceCharsForRefKeys(details.getVinTransType()).trim().toUpperCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								found=true;
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;

						if(found==false)
						{
							errorMessage.append("Transmission Type {"+details.getVinTransType()+"} does not exist in IM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Transmission Type {"+details.getVinTransType()+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
				
				}

				// DRIVE AXLE
				if(null!=details.getVinAxleType() && !"".equals(details.getVinAxleType()))
				{
					boolean found=false;

					// MDM CHECK
					try
					{
						sql="SELECT A.MDM_AT_ID FROM gms3_mdm_axle_type A, gms3_mdm_manual_language B WHERE A.MDM_AT_FLAG=? "
								+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
								+ " AND TRIM(LOWER(A.MDM_AT_AXLE_TYPE)) = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(3, locale.trim().toLowerCase());
						pstmt.setString(4, details.getVinAxleType().trim().toLowerCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_AT_ID")>0)
							{
								found = true;
							}
						}
						sql=null;
						pstmt.close();pstmt=null;
						rs.close();rs=null;

						if(found==false)
						{
							errorMessage.append("DriveAxle Type {"+details.getVinAxleType()+"} does not exist in MDM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find DriveAxle Type {"+details.getVinAxleType()+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}


					// IM CHECK
					found=false;
					try
					{
						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, Utilities.replaceCharsForRefKeys(details.getVinAxleType()).trim().toUpperCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								found=true;
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;

						if(found==false)
						{
							errorMessage.append("DriveAxle Type {"+details.getVinAxleType()+"} does not exist in IM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find DriveAxle Type {"+details.getVinAxleType()+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
				}

				// BODY TYPE
				if(null!=details.getVinBodyType() && !"".equals(details.getVinBodyType()))
				{
					boolean found=false;

					// MDM CHECK
					try
					{
						sql="SELECT A.MDM_BT_ID FROM gms3_mdm_body_type A, gms3_mdm_manual_language B WHERE A.MDM_BT_FLAG=? "
								+ " AND B.MDM_ML_FLAG=? AND A.MDM_ML_ID = B.MDM_ML_ID AND TRIM(LOWER(B.MDM_ML_LANG_CODE))=? "
								+ " AND TRIM(LOWER(A.MDM_BT_BODY_TYPE)) = ?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(2, ApplicationProperties.getProperty("flag.value.active"));
						pstmt.setString(3, locale.trim().toLowerCase());
						pstmt.setString(4, details.getVinBodyType().trim().toLowerCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(rs.getLong("MDM_BT_ID")>0)
							{
								found = true;
							}
						}
						sql=null;
						pstmt.close();pstmt=null;
						rs.close();rs=null;

						if(found==false)
						{
							errorMessage.append("Body Type {"+details.getVinBodyType()+"} does not exist in MDM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Body Type {"+details.getVinBodyType()+"} in MDM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}


					// IM CHECK
					found=false;
					try
					{
						sql="SELECT RECORDID FROM OK_IM.TAG WHERE REFERENCEKEY= ? ";
						pstmt = conn.prepareStatement(sql);
						pstmt.setString(1, Utilities.replaceCharsForRefKeys(details.getVinBodyType()).trim().toUpperCase());
						rs = pstmt.executeQuery();
						if(rs.next())
						{
							if(null!=rs.getString("RECORDID") && !"".equals(rs.getString("RECORDID")))
							{
								found=true;
							}
						}
						pstmt.close();pstmt=null;
						rs.close();rs=null;
						sql=null;

						if(found==false)
						{
							errorMessage.append("Body Type {"+details.getVinBodyType()+"} does not exist in IM.");
							// error found - status has to be failure for this row
							errorsFound=true;
						}
					}
					catch(Exception e1)
					{
						Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "checkVINAttributeStatus()", e1);
						errorMessage.append("Failed to find Body Type {"+details.getVinBodyType()+"} in IM, exception while executing Query ->"+ e1.getMessage()+".");
						// error found - status has to be failure for this row
						errorsFound=true;
					}
				}

				/*
				 * if errorsFound = true = updateFailureCount
				 * else = updateSuccessCount
				 */

				if(errorsFound==true)
				{
					// update failureCount
					updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_FAILURE);
					if(null!=errorMessage && null!=errorMessage.toString())
					{
						details.setErrorMessage(errorMessage.toString());
					}
				}
				else
				{
					// all is success - update successCount
					updateScheduleSuccessOrFailureCount(scheduleId, conn, MetaDataScheduleConstants.STATUS_SUCCESS);
					// set processingStatus as SUCCESS
					details.setProcessingStatus(MetaDataScheduleConstants.STATUS_SUCCESS);
				}

				errorMessage = null;
			
			}
			sql=null;
			details = null;
			errorMessage=null;
		}
		else
		{
			logger.info("checkVINAttributeStatus :: VIN ATTRIBUTE LIST / Locale as parameters are null. Return as it is.");
		}

		if(null!=rs)
			rs.close();
		if(null!=pstmt)
			pstmt.close();
		if(null!=conn)
			conn.close();

		return vinAttributeList;
	}
	
	private void updateScheduleSuccessOrFailureCount(String scheduleId, Connection conn, String type)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=scheduleId && !"".equals(scheduleId))
			{
				int count=0;
				String sql="";
				if(type.equals(MetaDataScheduleConstants.STATUS_SUCCESS))
				{
					sql="SELECT DC_PROCESSED_COUNT AS COUNT FROM gms3_dmt_metadata_val_sch WHERE DC_SCHEDULE_ID="+scheduleId;
				}
				else if(type.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					sql="SELECT DC_FAILED_COUNT AS COUNT FROM gms3_dmt_metadata_val_sch WHERE DC_SCHEDULE_ID="+scheduleId;
				}
				pstmt = conn.prepareStatement(sql);
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					count = rs.getInt("COUNT");
				}
				pstmt.close();pstmt=null;
				rs.close();rs=null;
				sql=null;

				// increment count by 1
				count = count+1;

				// update back in table
				if(type.equals(MetaDataScheduleConstants.STATUS_SUCCESS))
				{
					sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_PROCESSED_COUNT=?   WHERE DC_SCHEDULE_ID=?";
				}
				else if(type.equals(MetaDataScheduleConstants.STATUS_FAILURE))
				{
					sql="UPDATE gms3_dmt_metadata_val_sch SET  DC_FAILED_COUNT=? WHERE DC_SCHEDULE_ID=?";
				}
				pstmt = conn.prepareStatement(sql);
				pstmt.setInt(1, count);
				pstmt.setLong(2, new Long(scheduleId).longValue());
				pstmt.executeUpdate();
				sql=null;
				pstmt.close();pstmt=null;
			}
			else
			{
				logger.info("updateScheduleSuccessOrFailureCount :: Schedule Id as parameters is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "updateScheduleSuccessOrFailureCount()", e);
		}
	}
	
	private String getDisplayOrderName(String displayOrderCode, String modelType, String localeCode, Connection conn) throws SQLException
	{
		String displayOrderName=null;
		ResultSet rs = null;
		Statement stmt = null;
		if(null!=displayOrderCode && !"".equals(displayOrderCode) && null!=modelType && !"".equals(modelType) && null!=localeCode && !"".equals(localeCode))
		{
			String sql="SELECT A.MDM_DISPORD_NAME FROM gms3_mdm_display_order A, gms3_mdm_manual_language B WHERE A.MDM_ML_ID=B.MDM_ML_ID "
					+ " AND TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+localeCode.trim().toLowerCase()+"' "
					+ " AND TRIM(LOWER(A.MDM_DISPORD_MODEL_TYPE))='"+modelType.trim().toLowerCase()+"' "
					+ " AND TRIM(LOWER(A.MDM_DISPORD_CODE))='"+displayOrderCode.trim().toLowerCase()+"' "
					+ " AND A.MDM_DISPORD_FLAG='"+ApplicationProperties.getProperty("flag.value.active")+"'";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
			{
				displayOrderName = rs.getString("MDM_DISPORD_NAME");
			}
			sql = null;
		}
		else
		{
			logger.info("getDisplayOrderName :: Required Parameters Display Order Code  / Model Type / Locale are null. Return null.");
		}

		if(null!=rs)
			rs.close();
		if(null!=stmt)
			stmt.close();
		return displayOrderName;
	}
	
	private String getESICategoryName(String level, String code1, String code2, String code3, String locale, Connection conn)throws SQLException 
	{
		String categoryName="";
		Statement stmt =null;
		ResultSet rs = null;
		
		/*
		 * USE EN_UK LOCALE ONLY FOR MME MARKET
		 */
//		boolean useENUKLocale=false;
//		String enukLocale=null;
//		locale = locale.replace("-", "_");
//		if(mmeLocales.trim().toLowerCase().indexOf(locale.trim().toLowerCase())>-1)
//		{
//			// MME LOCALE
//			useENUKLocale=true;
//			enukLocale=ApplicationProperties.getProperty("en-uk");
//			enukLocale = enukLocale.replace("_", "-");
//		}
		
		// GET ALTERNATE LOCALE FOR PROCESSING LOCALE FOR ESI CATEGORY MASTER DATA TYPE
		locale = ScheduleDAO.getAlternateLocale(locale, AutoSyncConstants.ITEM_KEY_ESI_CATEGORY, conn);
		// re- replace _ by - for MDM Transactions.
		locale = locale.replace("_", "-");
		String getCatNameSql="";
		if(level.equals("LEVEL1"))
		{
			if(null!=code1 && !"".equals(code1))
			{
				getCatNameSql = "SELECT DISTINCT A.MDM_CAT_NAME_ENG_LANG AS CAT_NAME FROM gms3_mdm_category A, gms3_mdm_manual_language B WHERE A.MDM_ML_ID =B.MDM_ML_ID AND ";
//				if(useENUKLocale==true)
//				{
//					getCatNameSql=getCatNameSql+ " (TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"' OR TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+enukLocale.trim().toLowerCase()+"') ";
//				}
//				else
				{
					getCatNameSql=getCatNameSql+ " TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"'  ";
				}
				getCatNameSql=getCatNameSql+ " AND TRIM(LOWER(A.MDM_CAT_CODE)) = '"+code1.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql+ " AND A.MDM_CAT_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
				getCatNameSql=getCatNameSql+ " AND B.MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
			}
		}
		else if(level.equals("LEVEL2"))
		{
			if(null!=code1 && !"".equals(code1) && null!=code2 && !"".equals(code2))
			{
				getCatNameSql = "SELECT DISTINCT A.MDM_SCAT_NAME_ENG_LANG AS CAT_NAME FROM gms3_mdm_category A, gms3_mdm_manual_language B WHERE A.MDM_ML_ID =B.MDM_ML_ID AND ";
//				if(useENUKLocale==true)
//				{
//					getCatNameSql=getCatNameSql+ " (TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"' OR TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+enukLocale.trim().toLowerCase()+"') ";
//				}
//				else
				{
					getCatNameSql=getCatNameSql+ " TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"'  ";
				}
				getCatNameSql=getCatNameSql		+ " AND TRIM(LOWER(A.MDM_CAT_CODE)) = '"+code1.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql		+ " AND TRIM(LOWER(A.MDM_SCAT_CODE)) = '"+code2.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql		+ " AND A.MDM_CAT_FLAG  ='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
				getCatNameSql=getCatNameSql		+ " AND B.MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
			}
		}
		else if(level.equals("LEVEL3"))
		{
			if(null!=code1 && !"".equals(code1) && null!=code2 && !"".equals(code2) && null!=code3 && !"".equals(code3))
			{
				getCatNameSql = "SELECT DISTINCT A.MDM_SSCAT_NAME_ENG_LANG AS CAT_NAME FROM gms3_mdm_category A, gms3_mdm_manual_language B WHERE A.MDM_ML_ID =B.MDM_ML_ID AND ";
//				if(useENUKLocale==true)
//				{
//					getCatNameSql=getCatNameSql+ " (TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"' OR TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+enukLocale.trim().toLowerCase()+"') ";
//				}
//				else
				{
					getCatNameSql=getCatNameSql+ " TRIM(LOWER(B.MDM_ML_LANG_CODE))='"+locale.trim().toLowerCase()+"'  ";
				}
				getCatNameSql=getCatNameSql		+ " AND TRIM(LOWER(A.MDM_CAT_CODE)) = '"+code1.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql		+ " AND TRIM(LOWER(A.MDM_SCAT_CODE)) = '"+code2.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql		+ " AND TRIM(LOWER(A.MDM_SSCAT_CODE)) = '"+code3.trim().toLowerCase()+"' ";
				getCatNameSql=getCatNameSql		+ " AND A.MDM_CAT_FLAG  ='"+ApplicationProperties.getProperty("flag.value.active")+"' ";
				getCatNameSql=getCatNameSql		+ " AND B.MDM_ML_FLAG ='"+ApplicationProperties.getProperty("flag.value.active")+"'";
			}
		}
		if(null!=getCatNameSql && !"".equals(getCatNameSql))
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(getCatNameSql);
			if(rs.next())
			{
				categoryName = rs.getString("CAT_NAME");
			}
			rs.close();rs=null;
			stmt.close();stmt=null;
		}
		getCatNameSql= null;
		locale=  null;
//		enukLocale=null;
	
		return categoryName;
	}
	
	public boolean deleteScheduleDetails(String scheduleIds) 
	{
		PreparedStatement pstmt = null;
		Connection conn = null;
		try
		{
			if(null!=scheduleIds && !"".equals(scheduleIds))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				String[] tokens=scheduleIds.split(",");
				String sql="";
				if(null!=tokens && tokens.length>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						pstmt = null;
						sql="UPDATE gms3_dmt_metadata_val_sch SET DC_RECORD_STATUS='"+AutoSyncConstants.STATUS_INACTIVE+"' "
								+ "   WHERE DC_SCHEDULE_ID = "+ tokens[i].toString();
						pstmt = conn.prepareStatement(sql);
						pstmt.executeUpdate();
						pstmt.close();pstmt= null;
						sql = null;
					}
				}
				sql = null;
				// commit the transaction
				conn.commit();
				tokens = null;
			}
			else
			{
				logger.info("deleteScheduleDetails :: Schedule Ids as parameter is null. Return false");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				conn.rollback();
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e1);
			}
			Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e);
			return false;
		}
		finally
		{
			try
			{
				if(null!= pstmt)
					pstmt.close();
				if(null!=conn)
					conn.close();
			}
			catch(SQLException e)
			{
				Utilities.printStackTraceToLogs(MetaDataValidationDAO.class.getName(), "deleteScheduleDetails()", e);
			}
			// set passed parameter to null
			scheduleIds=null;
		}
		return true;
	}

}