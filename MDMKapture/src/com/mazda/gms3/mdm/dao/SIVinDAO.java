package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CustomComparator;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class SIVinDAO extends DBConnectionHelper{

	static Logger logger = LogManager.getLogger(SIVinDAO.class);
	
	public static ArrayList<SelectItemDetails> getWMIList(String langauageId) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		ArrayList<SelectItemDetails> wmiList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && !"".equals(langauageId))
			{
				conn = getConnection();
				String sql = "SELECT DISTINCT mdm_crln_wmi_code FROM gms3_mdm_carline_codes  WHERE "
						+ " mdm_ml_id="+langauageId+" AND "
						+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') ORDER BY mdm_crln_wmi_code ASC";
//				logger.info("getWMIList :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(rs.getString("mdm_crln_wmi_code").trim());
					si.setValue(rs.getString("mdm_crln_wmi_code").trim());
					wmiList.add(si);
					si=  null;
				}
				sql = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getWMIList()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return wmiList;
	}
 	
	public static ArrayList<CarlineDetails> getModelsList(String langauageId, String wmiCode) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && !"".equals(langauageId) && null!=wmiCode && !"".equals(wmiCode))
			{
//				conn = getConnection();
//				String sql="SELECT DISTINCT mdm_crln_name_eng_lang, mdm_crln_code FROM gms3_mdm_carline_codes "
//						+ " WHERE mdm_ml_id="+langauageId+" "
//						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
//						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
//						+ " AND TRIM(LOWER(mdm_crln_wmi_code)) ='"+wmiCode.trim().toLowerCase()+"' "
//						+ " ORDER BY mdm_crln_name_eng_lang ASC";
////				logger.info("getModelsList :: Sql :: > " + sql);
//				stmt = conn.createStatement();
//				rs = stmt.executeQuery(sql);
//				while(rs.next())
//				{
//					SelectItemDetails si = new SelectItemDetails();
//					si.setLabel(rs.getString("mdm_crln_name_eng_lang").trim());
//					si.setValue(rs.getString("mdm_crln_name_eng_lang").trim());
//					modelsList.add(si);
//					si=  null;
//				}
//				sql = null;
				
				conn = getConnection();
				/*
				 * GET UNIQUE CARLINE CODES + CARLINE NAMES
				 */
				String sql="SELECT DISTINCT mdm_crln_code, mdm_crln_name_eng_lang FROM "
						+ " gms3_mdm_carline_codes  "
						+ " WHERE mdm_ml_id="+langauageId+" AND TRIM(LOWER(mdm_crln_wmi_code)) ='"+wmiCode.trim().toLowerCase()+"' "
						+ " AND mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ "GROUP BY mdm_crln_code, mdm_crln_name_eng_lang ORDER BY mdm_crln_name_eng_lang ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					CarlineDetails carDetails = new CarlineDetails();
					if(null!=rs.getString("mdm_crln_code"))
					{
						carDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						carDetails.setCarlineNameEng(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()) 
							&& null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
					{
						carDetails.setCarlineIdForCombo(carDetails.getCarlineNameEng().trim().toUpperCase()+"_"+carDetails.getCarlineCode().trim().toUpperCase());
					}
					modelsList.add(carDetails);
					carDetails = null;
				}
				rs.close();rs=null;
				stmt.close();stmt =null;
				sql=null;	
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getModelsList()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return modelsList;
	}

	/**
	 * GET YEARS LIST SPECIFIC FOR THE YEAR CODES E.G. 
	 * 	IF FOR A MODEL THERE A MULTIPLE CODES WITH DATA AS
	 * 	
	 * 	FP 	2003	2006
	 * 	HP	2008	2010
	 * 
	 * So in Years List, following Years will come - 
	 * 		2003, 2004, 2005, 2006, 2008, 2009, 2010
	 * @param countryLocaleId
	 * @param langauageId
	 * @param wmiCode
	 * @param modelId
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<SelectItemDetails> getYearsList(String langauageId, String wmiCode, String carlineEngName, String carlineCode) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		ArrayList<SelectItemDetails> yearsList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && 
					!"".equals(langauageId) && null!=wmiCode && !"".equals(wmiCode) && null!=carlineEngName && !"".equals(carlineEngName) 
					&& null!=carlineCode && !"".equals(carlineCode))
			{
				
				ArrayList<SelectItemDetails> tempList = new ArrayList<SelectItemDetails>();
				conn = getConnection();

				String sql="SELECT mdm_crln_year_start AS MIN_YEAR,  mdm_crln_year_end AS MAX_YEAR FROM gms3_mdm_carline_codes  "
						+ " WHERE mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') AND "
						+ " TRIM(LOWER(mdm_crln_wmi_code))=? "
						+ " AND mdm_ml_id=? "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))=? "
						+ " AND TRIM(LOWER(mdm_crln_code))=? ";
				
//				logger.info("getYearsList :: Sql :: > " + sql);
				
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, wmiCode.trim().toLowerCase());
				stmt.setLong(2, new Long(langauageId).longValue());
				stmt.setString(3, carlineEngName.trim().toLowerCase());
				stmt.setString(4, carlineCode.trim().toLowerCase());
				rs = stmt.executeQuery();

				while(rs.next())
				{
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(String.valueOf(rs.getInt("MIN_YEAR")));
					si.setValue(String.valueOf(rs.getInt("MAX_YEAR")));
					tempList.add(si);
					si = null;
				}
				sql = null;
				
				if(null!=tempList && tempList.size()>0)
				{
					for(int a=0;a<tempList.size();a++)
					{
						SelectItemDetails dataSi = (SelectItemDetails)tempList.get(a);
						int startYear = new Integer(dataSi.getLabel()).intValue();
						int endYear = new Integer(dataSi.getValue()).intValue();
						
						if(startYear>0 && endYear>0)
						{
							if(startYear<=endYear)
							{
								for(int i=startYear;i<=endYear;i++)
								{
									/*
									 * before adding this Check, whether the Year already exists in the List or not
									 * if yes - then do not add that Year
									 * else add.
									 */
									boolean add = true;
									if(null!=yearsList && yearsList.size()>0)
									{
										for(int r=0;r<yearsList.size();r++)
										{
											SelectItemDetails existSi = (SelectItemDetails)yearsList.get(r);
											if(existSi.getValue().equals(String.valueOf(i)))
											{
												add = false;
												break;
											}
										}
									}
									
									if(add==true)
									{
										SelectItemDetails si = new SelectItemDetails();
										si.setLabel(String.valueOf(i));
										si.setValue(String.valueOf(i));
										yearsList.add(si);
										si = null;
									}
								}
							}
						}
						else
						{
							if(startYear>0)
							{
								boolean add = true;
								if(null!=yearsList && yearsList.size()>0)
								{
									for(int r=0;r<yearsList.size();r++)
									{
										SelectItemDetails existSi = (SelectItemDetails)yearsList.get(r);
										if(existSi.getValue().equals(String.valueOf(startYear)))
										{
											add = false;
											break;
										}
									}
								}

								if(add==true)
								{
									SelectItemDetails si = new SelectItemDetails();
									si.setLabel(String.valueOf(startYear));
									si.setValue(String.valueOf(startYear));
									yearsList.add(si);
									si = null;
								}
							}
							if(endYear>0)
							{
								boolean add = true;
								if(null!=yearsList && yearsList.size()>0)
								{
									for(int r=0;r<yearsList.size();r++)
									{
										SelectItemDetails existSi = (SelectItemDetails)yearsList.get(r);
										if(existSi.getValue().equals(String.valueOf(endYear)))
										{
											add = false;
											break;
										}
									}
								}

								if(add==true)
								{
									SelectItemDetails si = new SelectItemDetails();
									si.setLabel(String.valueOf(endYear));
									si.setValue(String.valueOf(endYear));
									yearsList.add(si);
									si = null;
								}
							}
						}
					}
				}
				tempList = null;
				
				if(null!=yearsList && yearsList.size()>0)
				{
					// sort ArrayList in ascending Order.
			        Collections.sort(yearsList, new CustomComparator());
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getYearsList()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return yearsList;
	}

	
	public static ArrayList<SelectItemDetails> getVDSListForDefaultVINs(String langauageId, String wmiCode, String modelId, String yearId) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		ArrayList<SelectItemDetails> vdsList = new ArrayList<SelectItemDetails>();
		Statement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) && null!=modelId && !"".equals(modelId) && null!=yearId && !"".equals(yearId))
			{
				conn = getConnection();

				String sql="SELECT DISTINCT mdm_crln_code FROM gms3_mdm_carline_codes  "
						+ " WHERE mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') AND TRIM(LOWER(mdm_crln_wmi_code))='"+wmiCode.trim().toLowerCase()+"' "
						+ " AND mdm_ml_id="+langauageId+" "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))='"+modelId.trim().toLowerCase()+"' "
						+ " AND ("+yearId+" BETWEEN mdm_crln_year_start AND mdm_crln_year_end ) ORDER BY mdm_crln_code ASC";
				
//				logger.info("getVDSListForDefaultVINs :: Sql :: > " + sql);
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				ArrayList<String> list = new ArrayList<String>();
				while(rs.next())
				{
					/*
					 * add Year as well.
					 */
					
					String carCode = rs.getString("mdm_crln_code").trim();
					list.add(carCode);
					carCode=  null;
				}
				sql = null;
				
				if(null!=list && list.size()>0)
				{
					/*
					 * GET YEAR CODE WHICH WILL BE 7 CHARACTER IN VDS FROM VIN_YEAR_XREF
					 */
					String yearCodeForVds = getYearCodeFromVinXref(langauageId, yearId);
					if(null!=yearCodeForVds && !"".equals(yearCodeForVds))
					{
						for(int i=0;i<list.size();i++)
						{
							String carCode = String.valueOf(list.get(i));
							String vdsCodeLabel=carCode.trim()+"****"+yearCodeForVds.trim()+"#";
//							String vdsCode=carCode.trim()+"0000"+yearCodeForVds.trim()+"0";
							SelectItemDetails si = new SelectItemDetails();
							si.setLabel(vdsCodeLabel);
							si.setValue(vdsCodeLabel);
							si.setYearValue(yearId);
							si.setVdsType("DEFAULT");
							vdsList.add(si);
							si = null;
//							vdsCode = null;
							vdsCodeLabel=null;
							carCode= null;
						}
					}
					else
					{
						logger.info("getVDSListForDefaultVINs :: No Year Code Found for {"+yearId+"} in VIN_YEAR_XREF");
					}
					yearCodeForVds = null;
				}
				else
				{
					logger.info("getVDSListForDefaultVINs :: No Carline Codes Found on the Basis of Selected Parameters :: > " + langauageId+ " > " + wmiCode+" > "+ modelId+ " > "+ yearId);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getVDSListForDefaultVINs()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return vdsList;
	}
	
	public static ArrayList<SelectItemDetails> getVDSList(String langauageId, String wmiCode, String carlineEngName, String carlineCode, String fromYear, String toYear) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		ArrayList<SelectItemDetails> vdsList = new ArrayList<SelectItemDetails>();
		PreparedStatement stmt  = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) && null!=carlineCode && !"".equals(carlineCode) 
					&& null!=carlineEngName && !"".equals(carlineEngName) && null!=fromYear && !"".equals(fromYear)
					 && null!=toYear && !"".equals(toYear))
			{
				int startYear = new Integer(fromYear).intValue();
				int endYear = new Integer(toYear).intValue();		
				ArrayList<String> rangeList = new ArrayList<String>();
				// add all Years of Range
				for(int a=startYear;a<=endYear;a++)
				{
					rangeList.add(String.valueOf(a));
				}
				conn = getConnection();

				String sql="SELECT DISTINCT mdm_vin_vds_code FROM gms3_mdm_vin_detail  "
						+ " WHERE mdm_ml_id=? AND mdm_vin_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ " '"+ApplicationProperties.getProperty("flag.value.draft")+"') AND  "
						+ " TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_crln_name_eng_lang))=? AND TRIM(LOWER(mdm_crln_code))=? ";
				
//				logger.info("getVDSList :: Sql :: > " + sql);
				
				stmt = conn.prepareStatement(sql);
				stmt.setLong(1, new Long(langauageId).longValue());
				stmt.setString(2, wmiCode.trim().toLowerCase());
				stmt.setString(3, carlineEngName.trim().toLowerCase());
				stmt.setString(4, carlineCode.trim().toLowerCase());
				rs = stmt.executeQuery();
				ArrayList<String> list = new ArrayList<String>();
				while(rs.next())
				{
					String vdsCode = rs.getString("mdm_vin_vds_code").trim();
					list.add(vdsCode);
					vdsCode=  null;
				}
				sql = null;
				
				if(null!=list && list.size()>0)
				{
					/*
					 * GET YEAR CODE WHICH WILL BE 7 CHARACTER IN VDS FROM VIN_YEAR_XREF
					 */
					String yearCodeForVds="";
					for(int i=0;i<list.size();i++)
					{
						String vdsCode = String.valueOf(list.get(i));
						if(null!=vdsCode && !"".equals(vdsCode))
						{
							if(vdsCode.length()>=7)
							{
								yearCodeForVds = String.valueOf(vdsCode.charAt(6));
							}
						}
						
						/*
						 * A NEW CONDITION TO BE ADDED ON THE BASIS OF CR - ADD VDS WHERE 7TH CHAR IS START 
						 * IS * OR # FOR THE IDENTIFIED MODEL THEN ADD THEM AS WELL
						 * FOR THOSE VDS - SET MAPPED YEAR AS START YEAR FOR FURTHER PROCESSING
						 */
						
						String vdsYearValue=null;
//						if(null!=yearCodeForVds && !"".equals(yearCodeForVds) && 
//								!"*".equals(yearCodeForVds) && !"#".equals(yearCodeForVds) && null!=rangeList && rangeList.size()>0)
						if(null!=yearCodeForVds && !"".equals(yearCodeForVds) && null!=rangeList && rangeList.size()>0)
						{
							if(yearCodeForVds.equals("*") || yearCodeForVds.equals("#"))
							{
								// ADD VDS - SINCE 7TH CHAR IS * OR # FOR THE SELECTED MODEL - SET YEAR AS START YEAR
								SelectItemDetails si = new SelectItemDetails();
								si.setLabel(vdsCode);
								si.setValue(vdsCode);
								si.setVdsType("MASTER");
								si.setYearValue(String.valueOf(startYear));
								vdsList.add(si);
								si = null;
							}
							else
							{
								/*									
								 * call function to fetch Year from VIN_YEAR_XREF
								 */
								ArrayList<String> mappedYear = getMappedYearFromVinXref(langauageId, yearCodeForVds, conn,"N");
								if(null!=mappedYear && mappedYear.size()>0)
								{
									/*
									 * ITERATE AND CHECK WHICH YEAR LIES IN THE RANGE LIST
									 * ONLY YEAR FALLING IN THE RANGE LIST WILL BE ALLOWED TO ADDED
									 * TO VDS LIST
									 */
									for(int w=0;w<mappedYear.size();w++)
									{
										for(int b=0;b<rangeList.size();b++)
										{
											if(mappedYear.get(w).trim().toLowerCase().equals(rangeList.get(b).trim().toLowerCase()))
											{
												// YEAR EXISTS IN SELECTED RANGE LIST
												vdsYearValue = mappedYear.get(w).trim().toLowerCase();
												break;
											}
										}
									}
								}
							}
							
							
							if(null!=vdsYearValue && !"".equals(vdsYearValue))
							{
								// ADD VDS - SINCE YEAR IDENTIFIED AND FALLS IN SELECTED RANGE LIST
								SelectItemDetails si = new SelectItemDetails();
								si.setLabel(vdsCode);
								si.setValue(vdsCode);
								si.setVdsType("MASTER");
								si.setYearValue(vdsYearValue);
								vdsList.add(si);
								si = null;
							}
							}
							
						vdsCode = null;
						yearCodeForVds=  null;
						vdsYearValue = null;
					}
					yearCodeForVds = null;
				}
				else
				{
					logger.info("getVDSList :: No VDS Codes Found on the Basis of Selected Parameters  :: > " + langauageId+ " > " + wmiCode+" > "+ carlineEngName+" > "+ carlineCode);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getVDSList()", e);
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
//		logger.info("getWMIList :: Method Ends.");
		return vdsList;
	}
	
	
	public static String getYearCodeFromVinXref(String languageId, String yearValue) throws SQLException
	{
		String yearCode="";
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId && !"".equals(languageId) && null!=yearValue && !"".equals(yearValue))
			{
				conn = getConnection();
//				String sql="SELECT vinyearcode FROM ms3_vinyear_xref WHERE vinmappedyear='"+yearValue+"'";
////				logger.info("getYearCodeFromVinXref :: Sql :: > " + sql);
//				stmt = conn.createStatement();
//				rs = stmt.executeQuery(sql);
//				if(rs.next())
//				{
//					yearCode = rs.getString("VINYEARCODE").trim();
//				}
//				sql= null;
				
				String sql="SELECT mdm_vin_xref_code FROM gms3_mdm_vin_xref WHERE TRIM(mdm_vin_xref_year)='"+yearValue.trim()+"' "
						+ " AND mdm_ml_id="+languageId+" AND mdm_vin_xref_flag='"+ApplicationProperties.getProperty("flag.value.active")+"'";
//				logger.info("getYearCodeFromVinXref :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					if(null!=rs.getString("mdm_vin_xref_code"))
					{
						yearCode = rs.getString("mdm_vin_xref_code").trim();
					}
				}
				sql= null;
			}
			else
			{
				logger.info("getYearCodeFromVinXref :: Language id  / Year as parameters are null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getYearCodeFromVinXref()", e);
		}
		finally
		{
			if(null!=conn)
				conn.close();
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			yearValue = null;
		}
		return yearCode;
	}

	public static ArrayList<String> getMappedYearFromVinXref(String languageId, String yearCode, Connection conn, String closeConnection) throws SQLException
	{
		ArrayList<String> mappedYear=new ArrayList<String>();
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=languageId  && !"".equals(languageId) && null!=yearCode && !"".equals(yearCode))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				String sql="SELECT mdm_vin_xref_year FROM gms3_mdm_vin_xref WHERE TRIM(mdm_vin_xref_code) = '"+yearCode.trim()+"' "
						+ " AND mdm_ml_id="+languageId +" AND mdm_vin_xref_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' AND mdm_vin_xref_year !='NOT USED'";
//				logger.info("getYearCodeFromVinXref :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next())
				{
					mappedYear.add(rs.getString("mdm_vin_xref_year").trim());
				}
				sql= null;
				
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
			else
			{
				logger.info("getMappedYearFromVinXref :: Year Code as parameters are null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getMappedYearFromVinXref()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			yearCode = null;
		}
		return mappedYear;
	}
	
	public static String getModel(String langauageId, String wmiCode, String year, String carlineCode,  Connection conn, String closeConnection) throws SQLException
	{
//		logger.info("getWMIList :: Method Starts.");
		String model="";
		Statement stmt  = null;
		ResultSet rs = null;
		try
		{
			if(null!=langauageId && !"".equals(langauageId) && 
					null!=wmiCode && !"".equals(wmiCode) && null!=year && !"".equals(year) && 
					null!=carlineCode && !"".equals(carlineCode))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				String sql = "SELECT mdm_crln_name_eng_lang FROM gms3_mdm_carline_codes WHERE "
						+ " mdm_ml_id="+langauageId+" AND "
						+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
						+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
						+ " AND TRIM(LOWER(mdm_crln_wmi_code)) ='"+wmiCode.trim().toLowerCase()+"' "
						+ " AND ("+year+" BETWEEN mdm_crln_year_start AND mdm_crln_year_end ) AND TRIM(LOWER(mdm_crln_code)) ='"+carlineCode.trim().toLowerCase()+"'";
//				logger.info("getModel :: Sql :: > " + sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					model = rs.getString("mdm_crln_name_eng_lang").trim();
				}
				sql = null;
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "getModel()", e);
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
//		logger.info("getWMIList :: Method Ends.");
		return model;
	}
	
	
	public static boolean addVINDetails(SIVinDetails documentDetails, Connection conn, String closeConnection) 
	{
		Statement stmt= null;
		ResultSet rs = null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=documentDetails && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()) 
				&& null!=documentDetails.getLocale() && !"".equals(documentDetails.getLocale()) 
				&& null!=documentDetails.getCountryLocaleId() && !"".equals(documentDetails.getCountryLocaleId()) 
				&& null!=documentDetails.getManualLanguageId() && !"".equals(documentDetails.getManualLanguageId()))
			{
				try
				{
					conn.isValid(0);
				}
				catch(Exception e)
				{
					// connection is either null or not active - re-initialize connection object
					conn = getConnection();
				}
				conn.setAutoCommit(false);
				
				String status="";
				if(documentDetails.isDocumentPublished()==true)
				{
					// PUBLISH
					status = ApplicationProperties.getProperty("flag.value.publish");
				}
				else
				{
					// DRAFT
					status = ApplicationProperties.getProperty("flag.value.draft");
				}
				
				/*
				 * CHECK WHETHER THE DOCUMENT EXISTS IN PARENT TABLE OR NOT
				 */
				long autoDocId=0;
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
					+ "AND mdm_cl_id="+documentDetails.getCountryLocaleId()+" AND mdm_ml_id="+documentDetails.getManualLanguageId();
//				logger.info("addVINDetails :: checkSql :: > " + checkSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(checkSql);
				if(rs.next())
				{
					autoDocId = rs.getLong("mdm_sivin_code_id");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				checkSql =null;
				
				if(autoDocId==0)
				{
					/*
					 * INSERT INTO GMS3_MDM_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
					stmt= conn.createStatement();
					String generatedKeys[] = { "mdm_sivin_code_id"};
					stmt.executeUpdate(insertParentSql, generatedKeys);
					rs = stmt.getGeneratedKeys();
					if(rs.next())
					{
						autoDocId = rs.getLong(1);
					}
					insertParentSql= null;
					stmt.close();stmt = null;
					rs.close();rs =null;
					generatedKeys = null;
					
					if(autoDocId>0)
					{
						/*
						 * update the rest of the details
						 */
						String updateSql="UPDATE gms3_mdm_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
								+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
								+ " WHERE  mdm_sivin_code_id=?";
						pstmt = conn.prepareStatement(updateSql);
						pstmt.setString(1, documentDetails.getLocale());
						pstmt.setString(2, documentDetails.getFetchedVersion());
						pstmt.setString(3, documentDetails.getUpdatedVersion());
						pstmt.setString(4, status);
						pstmt.setString(5, documentDetails.getWslId());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setLong(7, autoDocId);
						pstmt.executeUpdate();
						pstmt.close();pstmt = null;
						updateSql= null;
						
						/*
						 * Insert New Records in Add Table
						 */
						if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
						{
							for(int i=0;i<documentDetails.getItemsList().size();i++)
							{
								SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
								String insertItemSql="INSERT INTO gms3_mdm_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getYear());
								pstmt.setString(6, itemDetails.getVdsCode());
								pstmt.setString(7, itemDetails.getVinStartRange());
								pstmt.setString(8, itemDetails.getVinEndRange());
								pstmt.setString(9, itemDetails.getFirstLevelRefKey());
								pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(11, itemDetails.getThirdLevelRefKey());
								pstmt.setString(12, documentDetails.getWslId());
								pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
								pstmt.executeUpdate();
								pstmt.close();pstmt= null;
								insertItemSql=null;		
							
								itemDetails=null;
							}
						}
						// COMMIT THE TRANSACTION
						conn.commit();
					}
				}
				else
				{

					/*
					 * update the rest of the details
					 */
					String updateSql="UPDATE gms3_mdm_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
							+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
							+ " WHERE  mdm_sivin_code_id=?";
					pstmt = conn.prepareStatement(updateSql);
					pstmt.setString(1, documentDetails.getLocale());
					pstmt.setString(2, documentDetails.getFetchedVersion());
					pstmt.setString(3, documentDetails.getUpdatedVersion());
					pstmt.setString(4, status);
					pstmt.setString(5, documentDetails.getWslId());
					pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(7, autoDocId);
					pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					updateSql= null;
					
					/*
					 * Insert New Records in Add Table
					 */
					if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
					{
						for(int i=0;i<documentDetails.getItemsList().size();i++)
						{
							SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
							String insertItemSql="INSERT INTO gms3_mdm_sivin_add (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getYear());
							pstmt.setString(6, itemDetails.getVdsCode());
							pstmt.setString(7, itemDetails.getVinStartRange());
							pstmt.setString(8, itemDetails.getVinEndRange());
							pstmt.setString(9, itemDetails.getFirstLevelRefKey());
							pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(11, itemDetails.getThirdLevelRefKey());
							pstmt.setString(12, documentDetails.getWslId());
							pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
							pstmt.executeUpdate();
							pstmt.close();pstmt= null;
							insertItemSql=null;		
						
							itemDetails=null;
						}
					}
					// COMMIT THE TRANSACTION
					conn.commit();
				}
			}
			else
			{
				logger.info("addVINDetails :: Either Document Details or Mandatory Parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "addVINDetails()", e1);
			}
			logger.info("addVINDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "addVINDetails()", e);
			logger.info("addVINDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			try
			{
				if(null!=pstmt)
				{
					pstmt.close();
				}
				if(null!=stmt)
					stmt.close();
				if(null!=rs)
					rs.close();
				if(null!=closeConnection && closeConnection.equals("Y"))
				{
					// if request from Servlet - close connection
					if(null!=conn)
						conn.close();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "addVINDetails()", e1);
			}
		}
		return true;
	}

	public static boolean deleteVINDetails(SIVinDetails documentDetails) throws SQLException
	{
		Connection conn = null;
		Statement stmt= null;
		ResultSet rs = null;
		PreparedStatement pstmt=null;
		try
		{
			if(null!=documentDetails && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()) 
				&& null!=documentDetails.getLocale() && !"".equals(documentDetails.getLocale()) 
				&& null!=documentDetails.getCountryLocaleId() && !"".equals(documentDetails.getCountryLocaleId()) 
				&& null!=documentDetails.getManualLanguageId() && !"".equals(documentDetails.getManualLanguageId()))
			{
				conn = getConnection();
				conn.setAutoCommit(false);
				
				String status="";
				if(documentDetails.isDocumentPublished()==true)
				{
					// PUBLISH
					status = ApplicationProperties.getProperty("flag.value.publish");
				}
				else
				{
					// DRAFT
					status = ApplicationProperties.getProperty("flag.value.draft");
				}
				
				/*
				 * CHECK WHETHER THE DOCUMENT EXISTS IN PARENT TABLE OR NOT
				 */
				long autoDocId=0;
				String checkSql="SELECT mdm_sivin_code_id FROM gms3_mdm_sivin_details WHERE TRIM(mdm_doc_id)='"+documentDetails.getDocumentId().trim()+"' "
					+ "AND mdm_cl_id="+documentDetails.getCountryLocaleId()+" AND mdm_ml_id="+documentDetails.getManualLanguageId();
//				logger.info("deleteVINDetails :: checkSql :: > " + checkSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(checkSql);
				if(rs.next())
				{
					autoDocId = rs.getLong("mdm_sivin_code_id");
				}
				rs.close();rs=null;
				stmt.close();stmt=null;
				checkSql =null;
				
				if(autoDocId==0)
				{
					/*
					 * INSERT INTO GMS3_MDM_SIVIN_DETAILS TABLE
					 */
					String insertParentSql="INSERT INTO gms3_mdm_sivin_details (mdm_doc_id, mdm_cl_id, mdm_ml_id) VALUES('"+documentDetails.getDocumentId()+"',"+new Long(documentDetails.getCountryLocaleId()).longValue()+" , "+new Long(documentDetails.getManualLanguageId()).longValue()+") ";
					stmt= conn.createStatement();
					String generatedKeys[] = { "mdm_sivin_code_id"};
					stmt.executeUpdate(insertParentSql, generatedKeys);
					rs = stmt.getGeneratedKeys();
					if(rs.next())
					{
						autoDocId = rs.getLong(1);
					}
					insertParentSql= null;
					stmt.close();stmt = null;
					rs.close();rs =null;
					generatedKeys = null;
					
					if(autoDocId>0)
					{
						/*
						 * update the rest of the details
						 */
						String updateSql="UPDATE gms3_mdm_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
								+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
								+ " WHERE  mdm_sivin_code_id=?";
						pstmt = conn.prepareStatement(updateSql);
						pstmt.setString(1, documentDetails.getLocale());
						pstmt.setString(2, documentDetails.getFetchedVersion());
						pstmt.setString(3, documentDetails.getUpdatedVersion());
						pstmt.setString(4, status);
						pstmt.setString(5, documentDetails.getWslId());
						pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
						pstmt.setLong(7, autoDocId);
						pstmt.executeUpdate();
						pstmt.close();pstmt = null;
						updateSql= null;
						
						/*
						 * Insert New Records in Delete Table
						 */
						if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
						{
							for(int i=0;i<documentDetails.getItemsList().size();i++)
							{
								SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
								String insertItemSql="INSERT INTO gms3_mdm_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
										+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
										+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
								pstmt= conn.prepareStatement(insertItemSql);
								pstmt.setLong(1, autoDocId);
								pstmt.setString(2, itemDetails.getWmiCode());
								pstmt.setString(3, itemDetails.getModel());
								pstmt.setString(4, itemDetails.getCarlineCode());
								pstmt.setString(5, itemDetails.getYear());
								pstmt.setString(6, itemDetails.getVdsCode());
								pstmt.setString(7, itemDetails.getVinStartRange());
								pstmt.setString(8, itemDetails.getVinEndRange());
								pstmt.setString(9, itemDetails.getFirstLevelRefKey());
								pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
								pstmt.setString(11, itemDetails.getThirdLevelRefKey());
								pstmt.setString(12, documentDetails.getWslId());
								pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
								pstmt.execute();
								pstmt.close();pstmt= null;
								insertItemSql=null;		
							
								itemDetails=null;
							}
						}
						// COMMIT THE TRANSACTION
						conn.commit();
					}
				}
				else
				{

					/*
					 * update the rest of the details
					 */
					String updateSql="UPDATE gms3_mdm_sivin_details SET mdm_doc_locale=?, mdm_doc_fetched_version=?, "
							+ " mdm_doc_modified_version=?, mdm_doc_status=?, mdm_sivin_wsl_id=?, mdm_sivin_updated_tmstp=? "
							+ " WHERE  mdm_sivin_code_id=?";
					pstmt = conn.prepareStatement(updateSql);
					pstmt.setString(1, documentDetails.getLocale());
					pstmt.setString(2, documentDetails.getFetchedVersion());
					pstmt.setString(3, documentDetails.getUpdatedVersion());
					pstmt.setString(4, status);
					pstmt.setString(5, documentDetails.getWslId());
					pstmt.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
					pstmt.setLong(7, autoDocId);
					pstmt.executeUpdate();
					pstmt.close();pstmt = null;
					updateSql= null;
					
					/*
					 * Insert New Records in Add Table
					 */
					if(null!=documentDetails.getItemsList() && documentDetails.getItemsList().size()>0)
					{
						for(int i=0;i<documentDetails.getItemsList().size();i++)
						{
							SIVinDetails itemDetails = (SIVinDetails)documentDetails.getItemsList().get(i);
							String insertItemSql="INSERT INTO gms3_mdm_sivin_delete (mdm_sivin_code_id,mdm_sivin_wmi,mdm_sivin_model"
									+ ",mdm_sivin_carcode,mdm_sivin_year,mdm_sivin_vds,mdm_sivin_vis_start,mdm_sivin_vis_end,mdm_sivin_flevel_refkey,"
									+ "mdm_sivin_slevel_refkey,mdm_sivin_tlevel_refkey,mdm_sivin_wsl_id,mdm_sivin_created_tmstp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
							pstmt= conn.prepareStatement(insertItemSql);
							pstmt.setLong(1, autoDocId);
							pstmt.setString(2, itemDetails.getWmiCode());
							pstmt.setString(3, itemDetails.getModel());
							pstmt.setString(4, itemDetails.getCarlineCode());
							pstmt.setString(5, itemDetails.getYear());
							pstmt.setString(6, itemDetails.getVdsCode());
							pstmt.setString(7, itemDetails.getVinStartRange());
							pstmt.setString(8, itemDetails.getVinEndRange());
							pstmt.setString(9, itemDetails.getFirstLevelRefKey());
							pstmt.setString(10, itemDetails.getSeconddLevelRefKey());
							pstmt.setString(11, itemDetails.getThirdLevelRefKey());
							pstmt.setString(12, documentDetails.getWslId());
							pstmt.setTimestamp(13, new java.sql.Timestamp(new Date().getTime()));
							pstmt.executeUpdate();
							pstmt.close();pstmt= null;
							insertItemSql=null;		
						
							itemDetails=null;
						}
					}
					// COMMIT THE TRANSACTION
					conn.commit();
				}
			}
			else
			{
				logger.info("deleteVINDetails :: Either Document Details or Mandatory Parameters are null. Return false.");
				return false;
			}
		}
		catch(Exception e)
		{
			if(null!=conn)
			{
				conn.rollback();
			}
			logger.info("deleteVINDetails :: ################ Exception ################");
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "deleteVINDetails()", e);
			logger.info("deleteVINDetails :: ################ Exception ################");
			return false;
		}
		finally
		{
			if(null!=pstmt)
			{
				pstmt.close();
			}
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
			if(null!=conn)
				conn.close();
		}
		return true;
	}

	public static boolean performViewContentOperation(MNAOViewContentDetails docDetails, Connection conn, String closeConnection) 
	{
		Statement stmt = null;
		PreparedStatement pstmt = null;
		try 
		{
			/*
			 * CHECK COMBINATION OF DOCUMENTID & LOCALE I ALREADY EXISTS THEN
			 * DELETE THEM FIRST THEN SAVE
			 */
			try
			{
				conn.isValid(0);
			}
			catch(Exception e)
			{
				// connection is either null or not active - re-initialize connection object
				conn = getConnection();
			}
			conn.setAutoCommit(false);
			String deleteSql = "DELETE FROM gms3_vc_model_year_details WHERE trim(vc_my_locale)='"+ docDetails.getLocale().trim()+ "' "+ " AND trim(vc_my_document_id)='"
					+ docDetails.getDocumentId().trim() + "'";
			
			logger.info("performViewContentOperation :: Delete Model Details SQL ::  > " +  deleteSql);
			stmt = conn.createStatement();
			stmt.executeUpdate(deleteSql);
			stmt.close();
			stmt = null;
			deleteSql = null;
			

			deleteSql = "DELETE FROM gms3_vc_vin_details WHERE trim(vc_vin_locale)='"+ docDetails.getLocale().trim()+ "' "+ " AND trim(vc_vin_document_id)='"
					+ docDetails.getDocumentId().trim() + "'";
			
			logger.info("performViewContentOperation :: Delete VIN Details SQL ::  > " +  deleteSql);
			stmt = conn.createStatement();
			stmt.executeUpdate(deleteSql);
			stmt.close();
			stmt = null;
			deleteSql = null;
		
			
			// SAVE ONLY WHEN DOCUMENT STATUS IS NOT NULL AND Y & MODEL YEAR LIST IS NOT NULL
			if (null != docDetails.getDocumentStatus() 	&& !"".equals(docDetails.getDocumentStatus()) 
					&& docDetails.getDocumentStatus().trim().toLowerCase().equals("y") && null!=docDetails.getModelYearList() && docDetails.getModelYearList().size()>0)
			{
				String model = null;
				String sql = null;
				for(MNAOViewContentDetails modelDetails : docDetails.getModelYearList())
				{
					/*
					 * NOW SAVE THE NEW DOCUMENT DETAILS
					 * ALWAYS SET MODEL NAME AS UPPERCASE
					 */
					if(null!=modelDetails.getModel() && !"".equals(modelDetails.getModel()))
					{
						model = modelDetails.getModel().trim().toUpperCase();
					}
					if(null!=modelDetails.getLocale() && !"".equals(modelDetails.getLocale()))
					{
						modelDetails.setLocale(modelDetails.getLocale().trim());
					}
					if(null!=modelDetails.getYear() && !"".equals(modelDetails.getYear()))
					{
						modelDetails.setYear(modelDetails.getYear());
					}
					if(null!=modelDetails.getDocumentId() && !"".equals(modelDetails.getDocumentId()))
					{
						modelDetails.setDocumentId(modelDetails.getDocumentId().trim());
					}
					if(null!=modelDetails.getSourceLocation() && !"".equals(modelDetails.getSourceLocation()))
					{
						modelDetails.setSourceLocation(modelDetails.getSourceLocation().trim());
					}
					
					if(null!=modelDetails.getCatCodeLevel1() && !"".equals(modelDetails.getCatCodeLevel1()))
					{
						modelDetails.setCatCodeLevel1(modelDetails.getCatCodeLevel1().trim());
					}
					if(null!=modelDetails.getCatNameLevel1() && !"".equals(modelDetails.getCatNameLevel1()))
					{
						modelDetails.setCatNameLevel1(modelDetails.getCatNameLevel1().trim());
					}
					if(null!=modelDetails.getCatCodeLevel2() && !"".equals(modelDetails.getCatCodeLevel2()))
					{
						modelDetails.setCatCodeLevel2(modelDetails.getCatCodeLevel2().trim());
					}
					if(null!=modelDetails.getCatNameLevel2() && !"".equals(modelDetails.getCatNameLevel2()))
					{
						modelDetails.setCatNameLevel2(modelDetails.getCatNameLevel2().trim());
					}
					if(null!=modelDetails.getCatCodeLevel3() && !"".equals(modelDetails.getCatCodeLevel3()))
					{
						modelDetails.setCatCodeLevel3(modelDetails.getCatCodeLevel3().trim());
					}
					if(null!=modelDetails.getCatNameLevel3() && !"".equals(modelDetails.getCatNameLevel3()))
					{
						modelDetails.setCatNameLevel3(modelDetails.getCatNameLevel3().trim());
					}
					if(null!=modelDetails.getTitle() && !"".equals(modelDetails.getTitle()))
					{
						modelDetails.setTitle(modelDetails.getTitle().trim());
					}
					if(null!=modelDetails.getDocumentType() && !"".equals(modelDetails.getDocumentType()))
					{
						modelDetails.setDocumentType(modelDetails.getDocumentType().trim());
					}
					if(null!=modelDetails.getDocumentTypeName() && !"".equals(modelDetails.getDocumentTypeName()))
					{
						modelDetails.setDocumentTypeName(modelDetails.getDocumentTypeName().trim());
					}
					if(null!=modelDetails.getDocumentSubType() && !"".equals(modelDetails.getDocumentSubType()))
					{
						modelDetails.setDocumentSubType(modelDetails.getDocumentSubType().trim());
					}
					if(null!=modelDetails.getDocumentSubTypeName() && !"".equals(modelDetails.getDocumentSubTypeName()))
					{
						modelDetails.setDocumentSubTypeName(modelDetails.getDocumentSubTypeName().trim());
					}
					if(null!=modelDetails.getIssueDate() && !"".equals(modelDetails.getIssueDate()))
					{
						modelDetails.setIssueDate(modelDetails.getIssueDate().trim());
					}
					if(null!=modelDetails.getFirstPublicationDate() && !"".equals(modelDetails.getFirstPublicationDate()))
					{
						modelDetails.setFirstPublicationDate(modelDetails.getFirstPublicationDate().trim());
					}
					if(null!=modelDetails.getSiNumber() && !"".equals(modelDetails.getSiNumber()))
					{
						modelDetails.setSiNumber(modelDetails.getSiNumber().trim());
					}
					if(null!=modelDetails.getDescription() && !"".equals(modelDetails.getDescription()))
					{
						modelDetails.setDescription(modelDetails.getDescription().trim());
					}
					
					// SAVE
					sql = "INSERT INTO gms3_vc_model_year_details (vc_my_locale,vc_my_model,vc_my_year,"
							+ "vc_my_document_id,vc_my_source_ntwrk_loc, vc_my_document_type,vc_my_esi_cat_code_1,"
							+ "vc_my_esi_cat_name_1,vc_my_esi_cat_code_2,vc_my_esi_cat_name_2,vc_my_esi_cat_code_3,"
							+ "vc_my_esi_cat_name_3,vc_my_document_title,vc_my_document_status,vc_my_created_tmstp,"
							+ " vc_my_document_subtype,vc_my_document_type_name,vc_my_document_subtype_name,"
							+ "vc_my_doc_create_date,vc_my_doc_last_modified_date,vc_my_issue_date,"
							+ "vc_my_first_publication_date,vc_my_si_number,vc_my_description) "
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, modelDetails.getLocale());
					pstmt.setString(2, model);
					pstmt.setString(3, modelDetails.getYear());
					pstmt.setString(4, modelDetails.getDocumentId());
					pstmt.setString(5, modelDetails.getSourceLocation());
					pstmt.setString(6, modelDetails.getDocumentType());
					pstmt.setString(7, modelDetails.getCatCodeLevel1());
					pstmt.setString(8, modelDetails.getCatNameLevel1());
					pstmt.setString(9, modelDetails.getCatCodeLevel2());
					pstmt.setString(10, modelDetails.getCatNameLevel2());
					pstmt.setString(11, modelDetails.getCatCodeLevel3());
					pstmt.setString(12, modelDetails.getCatNameLevel3());
					pstmt.setString(13, modelDetails.getTitle());
					// pstmt.setString(14, modelDetails.getDocumentStatus());
					pstmt.setString(14, "1");
					pstmt.setTimestamp(15, new Timestamp(new Date().getTime()));
					pstmt.setString(16, modelDetails.getDocumentSubType());
					pstmt.setString(17, modelDetails.getDocumentTypeName());
					pstmt.setString(18, modelDetails.getDocumentSubTypeName());
					pstmt.setTimestamp(19, modelDetails.getDocumentCreateDate());
					pstmt.setTimestamp(20, modelDetails.getDocumentLastModifiedDate());
					pstmt.setString(21, modelDetails.getIssueDate());
					pstmt.setString(22, modelDetails.getFirstPublicationDate());
					pstmt.setString(23, modelDetails.getSiNumber());
					pstmt.setString(24, modelDetails.getDescription());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					sql = null;
					model = null;
				}
				model = null;
				sql = null;
			}
			
			
			// SAVE ONLY WHEN DOCUMENT STATUS IS NOT NULL AND Y & VIN LIST IS NOT NULL
			if (null != docDetails.getDocumentStatus() 	&& !"".equals(docDetails.getDocumentStatus()) 
					&& docDetails.getDocumentStatus().trim().toLowerCase().equals("y") && 
					null!=docDetails.getVinList() && docDetails.getVinList().size()>0)
			{
				String sql = null;
				String model = null;
				for(MNAOViewContentDetails vinDetails : docDetails.getVinList())
				{
					/*
					 * NOW SAVE THE NEW DOCUMENT DETAILS
					 * ALWAYS SET MODEL NAME AS UPPERCASE
					 */
					if(null!=vinDetails.getModel() && !"".equals(vinDetails.getModel()))
					{
						model = vinDetails.getModel().trim().toUpperCase();
					}
					if(null!=vinDetails.getLocale() && !"".equals(vinDetails.getLocale()))
					{
						vinDetails.setLocale(vinDetails.getLocale().trim());
					}
					if(null!=vinDetails.getYear() && !"".equals(vinDetails.getYear()))
					{
						vinDetails.setYear(vinDetails.getYear());
					}
					if(null!=vinDetails.getDocumentId() && !"".equals(vinDetails.getDocumentId()))
					{
						vinDetails.setDocumentId(vinDetails.getDocumentId().trim());
					}
					if(null!=vinDetails.getSourceLocation() && !"".equals(vinDetails.getSourceLocation()))
					{
						vinDetails.setSourceLocation(vinDetails.getSourceLocation().trim());
					}
					
					if(null!=vinDetails.getCatCodeLevel1() && !"".equals(vinDetails.getCatCodeLevel1()))
					{
						vinDetails.setCatCodeLevel1(vinDetails.getCatCodeLevel1().trim());
					}
					if(null!=vinDetails.getCatNameLevel1() && !"".equals(vinDetails.getCatNameLevel1()))
					{
						vinDetails.setCatNameLevel1(vinDetails.getCatNameLevel1().trim());
					}
					if(null!=vinDetails.getCatCodeLevel2() && !"".equals(vinDetails.getCatCodeLevel2()))
					{
						vinDetails.setCatCodeLevel2(vinDetails.getCatCodeLevel2().trim());
					}
					if(null!=vinDetails.getCatNameLevel2() && !"".equals(vinDetails.getCatNameLevel2()))
					{
						vinDetails.setCatNameLevel2(vinDetails.getCatNameLevel2().trim());
					}
					if(null!=vinDetails.getCatCodeLevel3() && !"".equals(vinDetails.getCatCodeLevel3()))
					{
						vinDetails.setCatCodeLevel3(vinDetails.getCatCodeLevel3().trim());
					}
					if(null!=vinDetails.getCatNameLevel3() && !"".equals(vinDetails.getCatNameLevel3()))
					{
						vinDetails.setCatNameLevel3(vinDetails.getCatNameLevel3().trim());
					}
					if(null!=vinDetails.getTitle() && !"".equals(vinDetails.getTitle()))
					{
						vinDetails.setTitle(vinDetails.getTitle().trim());
					}
					if(null!=vinDetails.getDocumentType() && !"".equals(vinDetails.getDocumentType()))
					{
						vinDetails.setDocumentType(vinDetails.getDocumentType().trim());
					}
					if(null!=vinDetails.getDocumentTypeName() && !"".equals(vinDetails.getDocumentTypeName()))
					{
						vinDetails.setDocumentTypeName(vinDetails.getDocumentTypeName().trim());
					}
					if(null!=vinDetails.getDocumentSubType() && !"".equals(vinDetails.getDocumentSubType()))
					{
						vinDetails.setDocumentSubType(vinDetails.getDocumentSubType().trim());
					}
					if(null!=vinDetails.getDocumentSubTypeName() && !"".equals(vinDetails.getDocumentSubTypeName()))
					{
						vinDetails.setDocumentSubTypeName(vinDetails.getDocumentSubTypeName().trim());
					}
					if(null!=vinDetails.getIssueDate() && !"".equals(vinDetails.getIssueDate()))
					{
						vinDetails.setIssueDate(vinDetails.getIssueDate().trim());
					}
					if(null!=vinDetails.getFirstPublicationDate() && !"".equals(vinDetails.getFirstPublicationDate()))
					{
						vinDetails.setFirstPublicationDate(vinDetails.getFirstPublicationDate().trim());
					}
					if(null!=vinDetails.getSiNumber() && !"".equals(vinDetails.getSiNumber()))
					{
						vinDetails.setSiNumber(vinDetails.getSiNumber().trim());
					}
					if(null!=vinDetails.getDescription() && !"".equals(vinDetails.getDescription()))
					{
						vinDetails.setDescription(vinDetails.getDescription().trim());
					}
					if(null!=vinDetails.getWmiCode() && !"".equals(vinDetails.getWmiCode()))
					{
						vinDetails.setWmiCode(vinDetails.getWmiCode().trim());
					}
					if(null!=vinDetails.getVdsCode() && !"".equals(vinDetails.getVdsCode()))
					{
						vinDetails.setVdsCode(vinDetails.getVdsCode().trim());
					}
					if(null!=vinDetails.getVisStartRange() && !"".equals(vinDetails.getVisStartRange()))
					{
						vinDetails.setVisStartRange(vinDetails.getVisStartRange().trim());
					}
					if(null!=vinDetails.getVisEndRange() && !"".equals(vinDetails.getVisEndRange()))
					{
						vinDetails.setVisEndRange(vinDetails.getVisEndRange().trim());
					}
					
					// SAVE
					sql = "INSERT INTO gms3_vc_vin_details (vc_vin_locale,vc_vin_model,vc_vin_year,"
							+ "vc_vin_document_id,vc_vin_source_ntwrk_loc, vc_vin_document_type,vc_vin_esi_cat_code_1,"
							+ "vc_vin_esi_cat_name_1,vc_vin_esi_cat_code_2,vc_vin_esi_cat_name_2,vc_vin_esi_cat_code_3,"
							+ "vc_vin_esi_cat_name_3,vc_vin_document_title,vc_vin_document_status,vc_vin_created_tmstp,"
							+ " vc_vin_document_subtype,vc_vin_document_type_name,vc_vin_document_subtype_name,"
							+ "vc_vin_doc_create_date,vc_vin_doc_last_modified_date,vc_vin_issue_date,"
							+ "vc_vin_first_publication_date,vc_vin_si_number,vc_vin_wmi_code, "
							+ "vc_vin_vds_code,vc_vin_vis_start_range,vc_vin_vis_end_range,vc_vin_description) "
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, vinDetails.getLocale());
					pstmt.setString(2, model);
					pstmt.setString(3, vinDetails.getYear());
					pstmt.setString(4, vinDetails.getDocumentId());
					pstmt.setString(5, vinDetails.getSourceLocation());
					pstmt.setString(6, vinDetails.getDocumentType());
					pstmt.setString(7, vinDetails.getCatCodeLevel1());
					pstmt.setString(8, vinDetails.getCatNameLevel1());
					pstmt.setString(9, vinDetails.getCatCodeLevel2());
					pstmt.setString(10, vinDetails.getCatNameLevel2());
					pstmt.setString(11, vinDetails.getCatCodeLevel3());
					pstmt.setString(12, vinDetails.getCatNameLevel3());
					pstmt.setString(13, vinDetails.getTitle());
					// pstmt.setString(14, vinDetails.getDocumentStatus());
					pstmt.setString(14, "1");
					pstmt.setTimestamp(15, new Timestamp(new Date().getTime()));
					pstmt.setString(16, vinDetails.getDocumentSubType());
					pstmt.setString(17, vinDetails.getDocumentTypeName());
					pstmt.setString(18, vinDetails.getDocumentSubTypeName());
					pstmt.setTimestamp(19, vinDetails.getDocumentCreateDate());
					pstmt.setTimestamp(20, vinDetails.getDocumentLastModifiedDate());
					pstmt.setString(21, vinDetails.getIssueDate());
					pstmt.setString(22, vinDetails.getFirstPublicationDate());
					pstmt.setString(23, vinDetails.getSiNumber());
					pstmt.setString(24, vinDetails.getWmiCode());
					pstmt.setString(25, vinDetails.getVdsCode());
					pstmt.setString(26, vinDetails.getVisStartRange());
					pstmt.setString(27, vinDetails.getVisEndRange());
					pstmt.setString(28, vinDetails.getDescription());
					pstmt.executeUpdate();
					pstmt.close();
					pstmt = null;
					sql = null;
					model = null;
				}
				model = null;
				sql = null;
			}
			
			// commit transaction
			conn.commit();
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "performViewContentOperation()", e);
			try
			{
				if(null!=conn)
				{
					conn.rollback();
				}
			}
			catch(SQLException eq)
			{
				Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "performViewContentOperation()", eq);
			}
			return false;
		} 
		finally 
		{
			try
			{
				if(null!=pstmt)
				{
					pstmt.close();
				}
				if(null!=stmt)
					stmt.close();
				if(null!=closeConnection && closeConnection.equals("Y"))
				{
					// if request from Servlet - close connection
					if(null!=conn)
						conn.close();
				}
			}
			catch(SQLException e1)
			{
				Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "performViewContentOperation()", e1);
			}
		}
		return true;
	}

	
	public static SIVinDetails checkVININMDM(String languageId, SIVinDetails fieldDetails, Connection conn, String closeConnection) throws SQLException
	{
		PreparedStatement pstmt=null;
		ResultSet rs= null;
		try
		{
			if(null!=languageId && !"".equals(languageId) && null!=fieldDetails && null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) 
					&& null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()))
			{
				if(null==conn || conn.isClosed())
				{
					conn = getConnection();
				}
				
				boolean vdsExists=  false;
				
				String sql="SELECT mdm_crln_code, mdm_crln_name_eng_lang, mdm_crln_name_regional_lang FROM gms3_mdm_vin_detail WHERE mdm_ml_id = "+languageId+" "
						+ " AND mdm_vin_flag='"+ApplicationProperties.getProperty("flag.value.active")+"' AND TRIM(LOWER(mdm_vin_wmi_code))=? "
						+ " AND TRIM(LOWER(mdm_vin_vds_code))=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, fieldDetails.getWmiCode().trim().toLowerCase());
				pstmt.setString(2, fieldDetails.getVdsCode().trim().toLowerCase());
				rs = pstmt.executeQuery();
				if(rs.next())
				{
					// VDS CODE EXISTS - IDENTIFY VDS YEAR
					vdsExists = true;
					if(null!=rs.getString("mdm_crln_code"))
					{
						fieldDetails.setCarlineCode(rs.getString("mdm_crln_code").trim());
					}
					if(null!=rs.getString("mdm_crln_name_eng_lang"))
					{
						fieldDetails.setModel(rs.getString("mdm_crln_name_eng_lang").trim());
					}
					if(null!=rs.getString("mdm_crln_name_regional_lang"))
					{
						fieldDetails.setModelRegionalName(rs.getString("mdm_crln_name_regional_lang").trim());
					}
				}
				rs.close();rs = null;
				pstmt.close();pstmt= null;
				sql = null;
				
				
				if(vdsExists==true && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) && null!=fieldDetails.getModel() 
						&& !"".equals(fieldDetails.getModel()))
				{
					// IDENTIFY YEAR VALUE FOR VDS AND CHECK IF IT FALLS UNDER THE YEAR RANGE OF THE IDENTIFIED CARLINE DETAILS.
					String yearCodeFromVds=null;
					if(fieldDetails.getVdsCode().length() >= 7)
					{
						yearCodeFromVds = String.valueOf(fieldDetails.getVdsCode().charAt(6));
					}
					if(null!=yearCodeFromVds && !"".equals(yearCodeFromVds))
					{
						// GET YEAR VALUES ON THE BASIS OF CODE AND LOCALE
						ArrayList<String> mappedYear = getMappedYearFromVinXref(languageId, yearCodeFromVds, conn,"N");
						if(null!=mappedYear && mappedYear.size()>0)
						{
							String carFromQuery=null;
							// IDENTIFY FROM THE MAPPED YEAR LIST WHICH YEAR EXISTS FOR THE CARLINE.
							for(int a=0;a<mappedYear.size();a++)
							{
								sql = "SELECT mdm_crln_name_eng_lang FROM gms3_mdm_carline_codes WHERE "
										+ " mdm_ml_id="+languageId+" AND "
										+ " mdm_crln_flag NOT IN ('"+ApplicationProperties.getProperty("flag.value.delete")+"', "
										+ "'"+ApplicationProperties.getProperty("flag.value.draft")+"') "
										+ " AND TRIM(LOWER(mdm_crln_wmi_code)) =? "
										+ " AND ("+mappedYear.get(a).toString()+" BETWEEN mdm_crln_year_start AND mdm_crln_year_end ) AND "
										+ " TRIM(LOWER(mdm_crln_code)) =? AND "
										+ " TRIM(LOWER(mdm_crln_name_eng_lang))=?";
								
								pstmt = conn.prepareStatement(sql);
								pstmt.setString(1, fieldDetails.getWmiCode().trim().toLowerCase());
								pstmt.setString(2, fieldDetails.getCarlineCode().trim().toLowerCase());
								pstmt.setString(3, fieldDetails.getModel().trim().toLowerCase());
								rs = pstmt.executeQuery();
								if(rs.next())
								{
									if(null!=rs.getString("mdm_crln_name_eng_lang") && !"".equals(rs.getString("mdm_crln_name_eng_lang")))
									{
										// YEAR IS FOUND - SET IN FIELD DETAILS
										carFromQuery = rs.getString("mdm_crln_name_eng_lang");
									}
								}
								rs.close();rs=null;
								pstmt.close();pstmt=null;
								sql = null;
								
								if(null!=carFromQuery && !"".equals(carFromQuery))
								{
									fieldDetails.setYear(String.valueOf(mappedYear.get(a)));
									break;
								}
								carFromQuery = null;
							}
						}
					}
				}
				
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(SIVinDAO.class.getName(), "checkVININMDM()", e);
		} 
		finally 
		{
			if (null != pstmt)
				pstmt.close();
			if(null!=rs)
				rs.close();
		}
		return fieldDetails;
	}
	
}
