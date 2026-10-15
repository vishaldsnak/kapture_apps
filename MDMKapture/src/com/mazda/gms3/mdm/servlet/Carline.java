package com.mazda.gms3.mdm.servlet;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.bean.CarlineBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CarlineDAO;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class Carline
 */
public class Carline extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	 Logger logger = LogManager.getLogger(Carline.class);
	
	 MessageProperties msgProps= null;
	 String moduleRefKey=AccessManagementInterface.REF_KEY_CARLINE;
       
	 String wslId = null;
	 String reportName=null;
	
	 ArrayList<CarlineDetails> uniqueWMIList = new ArrayList<CarlineDetails>();
	 ArrayList<CarlineDetails> uniqueCarlineNameList = new ArrayList<CarlineDetails>();
	 ArrayList<CarlineDetails> uniqueCarlineCodeList = new ArrayList<CarlineDetails>();
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Carline() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		/*
		 * Initialize bean
		 */
		CarlineBean sessionBean = getSessionBean(request);
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
				// EXPLICITY MAKE WSLID TO LOWERCASE
				if(null!=wslId && !"".equals(wslId))
				{
					wslId = wslId.trim().toLowerCase();
				}
			}
			
			// perform Access Check
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			
			sessionBean.setCountryLocaleList(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setLanguageList(null);
			sessionBean.setCarList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
//			sessionBean.setYearStartMandatory(false);
			
			sessionBean.setUpdatedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setAllDataCarList(null);
			sessionBean.setReportViewPath(null);
			sessionBean.setCarListToImport(null);
			
			/*
			 * DO NOT SET MODEL TYPE & CARLINE LIST TO NULL EXPLICITYLY
			 * AS THEY WILL BE LOADED ONCE AND NO CHANGE IS EVER GOING TO 
			 * HAPPEN TO THEM.
			 */
			
			sessionBean.setShowButtons(false);
			sessionBean.setShowView(false);
			sessionBean.setShowAdd(false);
			sessionBean.setMnaoContent(false);
			
			sessionBean.setSearchWmiId(null);
			sessionBean.setSearchWmiList(null);
			sessionBean.setSearchCarlineNameId(null);
			sessionBean.setSearchCarlineNameList(null);
			sessionBean.setSearchCarlineCodeId(null);
			sessionBean.setSearchCarlineCodeList(null);
			sessionBean.setSearchModelTypeId(null);
			sessionBean.setSearchESICategoryFlagId(null);
			
			sessionBean.setAddNewWmiId(null);
			sessionBean.setAddNewWmiList(null);
			sessionBean.setAddNewCarlineNameId(null);
			sessionBean.setAddNewCarlineNameList(null);
			sessionBean.setAddNewESICategoryFlagId(null);
			sessionBean.setAddNewModelTypeId(null);
			
			sessionBean.setShowAddNewWMIField(false);
			sessionBean.setShowAddNewCarlineNameField(false);
			sessionBean.setNewCarlineNameEngFieldValue(null);
			sessionBean.setNewCarlineNameRegFieldValue(null);
			sessionBean.setNewWmiFieldValue(null);
			
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
		
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to load ESI Category Flag Values
			 */
			getESICategoryFlagList(sessionBean);
			/*
			 * call function to load Model Type List
			 */
			getModelTypeList(sessionBean);
			
			
			/*
			 * 
			 * TO DO
			 * 
			 */
			// EXPLICITY SET sessionBean.setShowButtons(true);
			sessionBean.setShowButtons(true);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName()	, "doGet()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				// perform MME Countries check
//				checkForMMECountries(sessionBean);
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/carline.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		/*
		 * Initialize bean
		 */
		CarlineBean sessionBean = getSessionBean(request);
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
				// EXPLICITY MAKE WSLID TO LOWERCASE
				if(null!=wslId && !"".equals(wslId))
				{
					wslId = wslId.trim().toLowerCase();
				}
			}
			
			// do access check
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setInfoMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setReportViewPath(null);
			sessionBean.setMnaoContent(false);
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			/*
			 * PERFORM IDENTIFICATION FOR WHETHER MNAO CONTENT = TRUE / FALSE
			 */
			performCheckforMNAOContent(sessionBean);
			
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_LOCALE_SELECTION"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
					sessionBean.setManualLanguageId(null);
					
					// FOR ADD BLOCK
					sessionBean.setAddNewWmiId(null);
					sessionBean.setAddNewCarlineNameId(null);
					sessionBean.setNewWmiFieldValue(null);
					sessionBean.setNewCarlineNameEngFieldValue(null);
					sessionBean.setNewCarlineNameRegFieldValue(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setAddNewModelTypeId(null);
					sessionBean.setAddNewESICategoryFlagId(null);
					
					// FOR SEARCH BLOCK
					sessionBean.setSearchCarlineCodeId(null);
					sessionBean.setSearchCarlineNameId(null);
					sessionBean.setSearchESICategoryFlagId(null);
					sessionBean.setSearchWmiId(null);
					sessionBean.setSearchModelTypeId(null);
					
					
					/*
					 * call function to load all the Manual language data
					 */
					getLanguageList(sessionBean, request);
					/*
					 * call function to load carLineDetails
					 */
					getCarList(sessionBean);
					
					/*
					 * YEAR RANGE IS ALWAYS MANDATORY.
					 * DATE - 23 JUNE 2018
					 */
//					sessionBean.setYearStartMandatory(false);
//					if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
//					{
//						/*
//						 * CHECK IF FROM DEPENDENT COUNTRIES - US / CA / FR, THEN SKIP YEA START AS NON MANDATORY.
//						 */
//						if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
//						{
//							for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
//							{
//								CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
//								if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
//								{
//									/*
//									 * GET THE CODE AND CHECK IF IN DEPENDENT COUNTRIES
//									 */
//									String code = clDetails.getCountryLocaleDesc();
//									String depedentCountries = ApplicationProperties.getProperty("carline.dependent.countries");
//									if(null!=depedentCountries && !"".equals(depedentCountries))
//									{
//										String[] tokens = depedentCountries.split(",");
//										if(null!=tokens && tokens.length>0)
//										{
//											for(int t=0;t<tokens.length;t++)
//											{
//												if(code.trim().toLowerCase().equals(tokens[t].trim().toLowerCase()))
//												{
//													// set YEAR MANDATORY TO TRUE
//													sessionBean.setYearStartMandatory(true);
//													break;
//												}
//											}
//										}
//										tokens = null;
//										code = null;
//										depedentCountries = null;
//									}
//									break;
//								}
//							}
//						}
//					}
				}
				
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
					
					// FOR ADD BLOCK
					sessionBean.setAddNewWmiId(null);
					sessionBean.setAddNewCarlineNameId(null);
					sessionBean.setNewWmiFieldValue(null);
					sessionBean.setNewCarlineNameEngFieldValue(null);
					sessionBean.setNewCarlineNameRegFieldValue(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setAddNewModelTypeId(null);
					sessionBean.setAddNewESICategoryFlagId(null);
					
					// FOR SEARCH BLOCK
					sessionBean.setSearchCarlineCodeId(null);
					sessionBean.setSearchCarlineNameId(null);
					sessionBean.setSearchESICategoryFlagId(null);
					sessionBean.setSearchWmiId(null);
					sessionBean.setSearchModelTypeId(null);
					
					/*
					 * call function to load carLineDetails
					 */
					getCarList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_CAR"))
				{
					/*
					 * Save Operation called
					 */
					saveCarlineDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_CAR"))
				{
					/*
					 * Edit Operation called
					 */
					editCarlineDetails(request, sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("DELETE_CAR"))
				{
					/*
					 * Delete Operation called
					 */
					deleteCarlineDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_CAR"))
				{
					/*
					 * Active Operation called
					 */
					activeCarDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_CAR"))
				{
					/*
					 * Update Operation called
					 */
					updateCarlineDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_CAR"))
				{
					// reset some fields
					sessionBean.setCarList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					sessionBean.setUpdatedRows(null);
					
					sessionBean.setEsiCategoryFlagList(null);
					/*
					 * call getCarList
					 */
					getCarList(sessionBean);	
					
					/*
					 * call function to load ESI Category Flag Values
					 */
					getESICategoryFlagList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_CAR"))
				{
					/*
					 * Export VIN Operation
					 */
					exportCarDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_CAR"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
					/*
					 * Search Operation Called
					 */
					searchCarline(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("CLEAR_SEARCH"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setSearchWmiId(null);
					sessionBean.setSearchCarlineNameId(null);
					sessionBean.setSearchCarlineCodeId(null);
					sessionBean.setSearchESICategoryFlagId(null);
					sessionBean.setSearchModelTypeId(null);
					sessionBean.setUpdatedRows(null);
					/*
					 * call getCalineList
					 */
					getCarList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ADD_NEW_BUTTON"))
				{
					/*
					 * FIRST VALIDATE - COUNTRY AND LANGUAGE MUST BE SELECTED.
					 */
					if(validateFileUpload(sessionBean))
					{
						sessionBean.setShowAdd(true);
						sessionBean.setShowView(false);
						sessionBean.setShowUpdate(false);
						sessionBean.setSelectedRows(null);
						sessionBean.setSearchWmiId(null);
						sessionBean.setSearchCarlineNameId(null);
						sessionBean.setSearchCarlineCodeId(null);
						sessionBean.setSearchESICategoryFlagId(null);
						sessionBean.setSearchModelTypeId(null);
						sessionBean.setUpdatedRows(null);
						/*
						 * call getCalineList
						 */
						getCarList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("VIEW_BUTTON"))
				{
					/*
					 * FIRST VALIDATE - COUNTRY AND LANGUAGE MUST BE SELECTED.
					 */
					if(validateFileUpload(sessionBean))
					{
						sessionBean.setShowAdd(false);
						sessionBean.setShowView(true);
						sessionBean.setShowUpdate(false);
						sessionBean.setSelectedRows(null);
						sessionBean.setSearchWmiId(null);
						sessionBean.setSearchCarlineNameId(null);
						sessionBean.setSearchCarlineCodeId(null);
						sessionBean.setSearchESICategoryFlagId(null);
						sessionBean.setSearchModelTypeId(null);
						sessionBean.setUpdatedRows(null);
						/*
						 * call getCalineList
						 */
						getCarList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_WMI_SELECTION"))
				{
					/*
					 * call SearchWMI OPeration
					 */
					searchWMIOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_CARLINENAME_SELECTION"))
				{
					/*
					 * call Search CARLINE NAME Operation
					 */
					searchCarlineNameOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ADD_WMI_SELECTION"))
				{
					/*
					 * call ADDWMI OPeration
					 */
					addWMIOperation(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "doPost()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				// perform MME Countries check
//				checkForMMECountries(sessionBean);
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/carline.jsp");
				rs.forward(request, response);
			}
		}
	}
	
	private CarlineBean getSessionBean(HttpServletRequest request) 
	{
		CarlineBean sessionBean = null;
		if (null != request.getSession().getAttribute("carlineBean") && !"".equals(request.getSession().getAttribute("carlineBean"))) 
		{
			sessionBean = (CarlineBean) request.getSession().getAttribute("carlineBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CarlineBean();
			request.getSession().setAttribute("carlineBean", sessionBean);
		}
		return sessionBean;
	}
	
	
	private  void performCheckforMNAOContent(CarlineBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				// set TO FALSE WHILE CHECKING SO BECOMES JAPAN LANGUAGE
				sessionBean.setMnaoContent(false);
				String key = sessionBean.getManualLanguageId();
				if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
				{
					for(int a=0;a<sessionBean.getLanguageList().size();a++)
					{
						ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
						if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
						{
							// USE NAME FOR IDENTIFING CODE
							key  =mlDetails.getManualLanguageName();
							break;
						}
					}
				}
				if(null!=key && !"".equals(key))
				{
					key = key.replace("-", "_");

					StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
					while(str.hasMoreTokens())
					{
						String tok = str.nextToken();
						if(tok.trim().toLowerCase().equals(key.trim().toLowerCase()))
						{
							// MATCH FOUND - NORTH AMERICA LANGUAGE
							sessionBean.setMnaoContent(true);
							break;
						}
						tok = null;
					}
				}
				key = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "performCheckforMNAOContent()", e);
		}
	}
	
	private  void getCountryLocaleList(CarlineBean sessionBean, HttpServletRequest request)
	{
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			if(null!=list && list.size()>0)
			{
				/*
				 * ITERATE LIST AND REMOVE ALL NON-WRITABLE MME COUNTRIES
				 */
				String nonWritableMMECountries=ApplicationProperties.getProperty("mme.not.writable.countries");
				String[] tokens = nonWritableMMECountries.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						String ccCode = tokens[a];
						if(null!=list && list.size()>0)
						{
							for(int b=0;b<list.size();b++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)list.get(b);
								if(clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(ccCode.trim().toLowerCase()))
								{
									list.remove(b);
									b--;
									break;
								}
							}
						}
						ccCode = null;
					}
				}
				tokens = null;
				nonWritableMMECountries = null;
			}
			// PROCEED NOW
			if(null!=list && list.size()>0)
			{
				if(userSessionBean.isSuperAdminUser()==true)
				{
					sessionBean.setCountryLocaleList(list);
				}
				else if(userSessionBean.isSuperAdminUser()==false)
				{
					ArrayList<CountryLocaleDetails> finalCountryList = new ArrayList<CountryLocaleDetails>();
					// NOW CHECK FOR USER LOCALES
					if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
					{
						/*
						 * CHECK IF USER LOCALE CONTIANS ANY MME LOCALES AND DOES NOT CONTAIN EN-UK LOCALE
						 * THEN ALLOW EN-UK EXPLICITLY.
						 */
						String mmeNonWritableLoclaes=ApplicationProperties.getProperty("mme.not.writable.locales");
						String[] localeTokens = mmeNonWritableLoclaes.split(",");
						String enukLocale = ApplicationProperties.getProperty("en_uk");
						enukLocale = enukLocale.replace("_", "-");
						boolean containsMMELocale=false;
						boolean containsENUKLocale=false;
						for(String contentLocale : userSessionBean.getUserLocalesList())
						{
							for(int b=0;b<localeTokens.length;b++)
							{
								String loc = localeTokens[b];
								loc = loc.replace("_", "-");
								if(contentLocale.trim().toLowerCase().equals(loc.trim().toLowerCase()))
								{
									// USER CONTAINS MME LOCALE
									containsMMELocale=true;
									break;
								}
								loc = null;
							}
						}
						
						if(containsMMELocale==true)
						{
							// CHECK WHETHER USER CONTAIN ENUK LOCALE
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(contentLocale.trim().toLowerCase().equals(enukLocale.trim().toLowerCase()))
								{
									// USER CONTAINS ENUK LOCALE
									containsENUKLocale=true;
									break;
								}
							}
						}
						
						// PROCEED FOR IDENTIFYING COUNTRIES FOR COMBO
						for(String contentLocale : userSessionBean.getUserLocalesList())
						{
							if(contentLocale.lastIndexOf("-")!=-1)
							{
								String cCode = contentLocale.substring(contentLocale.lastIndexOf("-")+1, contentLocale.length());
								if(null!=cCode && !"".equals(cCode))
								{
									for(CountryLocaleDetails cldDetails : list)
									{
										boolean addToList = false;
										/*
										 * IF MME LOCALE IS TRUE AND EN UK LOCALE IS FALSE
										 * EXPLICITY ADD ENUK LOCALE AS WELL
										 */
										if(containsMMELocale==true && containsENUKLocale==false)
										{
											String ukCountry="";
											if(enukLocale.lastIndexOf("-")!=-1)
											{
												ukCountry = enukLocale.substring(enukLocale.lastIndexOf("-")+1, enukLocale.length());
											}
											if(ukCountry.trim().toLowerCase().equals(cldDetails.getCountryLocaleDesc().trim().toLowerCase()))
											{
												// addToList
												addToList= true;
											}
											ukCountry = null;
										}
										

										// NORMAL FLOW - REMAINS AS IT IS
										if(cCode.trim().toLowerCase().equals(cldDetails.getCountryLocaleDesc().trim().toLowerCase()))
										{
											// addToList
											addToList= true;
										}
										
										if(addToList==true)
										{
											boolean proceed = true;
											if(null!=finalCountryList && finalCountryList.size()>0)
											{
												for(CountryLocaleDetails existDetails : finalCountryList)
												{
													if(existDetails.getCountryLocaleId()==cldDetails.getCountryLocaleId())
													{
														// alreadyAdded - proceed - false
														proceed = false;
														break;
													}
												}
											}

											if(proceed==true)
											{
												finalCountryList.add(cldDetails);
											}
										}
										cldDetails = null;
									}
								}
								cCode = null;
							}
							contentLocale = null;
						}
						mmeNonWritableLoclaes = null;
						enukLocale = null;
						localeTokens = null;
					}

					if(null!=finalCountryList && finalCountryList.size()>0)
					{
						CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
						Collections.sort(finalCountryList,countryLocaleComparator);
						sessionBean.setCountryLocaleList(finalCountryList);
						countryLocaleComparator = null;
					}
					finalCountryList=  null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private  void getLanguageList(CarlineBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				list = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId());
				if(null!=list && list.size()>0)
				{
					/*
					 * ITERATE LIST AND REMOVE ALL NON-WRITABLE MME LANGUAGES
					 */
					String nonWritableMMELocales=ApplicationProperties.getProperty("mme.not.writable.locales");
					String[] tokens = nonWritableMMELocales.split(",");
					if(null!=tokens && tokens.length>0)
					{
						for(int a=0;a<tokens.length;a++)
						{
							String lcCode = tokens[a];
							if(null!=list && list.size()>0)
							{
								for(int b=0;b<list.size();b++)
								{
									ManualLanguageDetails mlDetails = (ManualLanguageDetails)list.get(b);
									if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(lcCode.trim().toLowerCase()))
									{
										list.remove(b);
										b--;
										break;
									}
								}
							}
							lcCode = null;
						}
					}
					tokens = null;
					nonWritableMMELocales = null;
				}
				
				if(null!=list && list.size()>0)
				{
					UserAccessBean userSessionBean = getUserSessionBean(request);
					if(userSessionBean.isSuperAdminUser()==true)
					{
						sessionBean.setLanguageList(list);
					}
					else if(userSessionBean.isSuperAdminUser()==false)
					{
						/*
						 * ITERATE LIST AND CHECK FOR THE LOCALE CODE WHETHER EXISTS IN USER'S DEFAULT LOCALE AND CONTENT LOCALE OR NOT
						 * IF EXISTS, THEN ONLY PROCEED. ELSE SKIP
						 */
						ArrayList<ManualLanguageDetails> finalLocaleList = new ArrayList<ManualLanguageDetails>();
						// CHECK WITH USER LOCALES
						if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
						{
							/*
							 * CHECK HERE IF USER CONTAINS ANY MME LOCALE BUT DOES NOT CONTAIN
							 * ENUK LOCALE - ADD IT EXPLICITYLY 
							 */
							/*
							 * CHECK IF USER LOCALE CONTIANS ANY MME LOCALES AND DOES NOT CONTAIN EN-UK LOCALE
							 * THEN ALLOW EN-UK EXPLICITLY.
							 */
							String mmeNonWritableLoclaes=ApplicationProperties.getProperty("mme.not.writable.locales");
							String[] localeTokens = mmeNonWritableLoclaes.split(",");
							String enukLocale = ApplicationProperties.getProperty("en_uk");
							enukLocale = enukLocale.replace("_", "-");
							boolean containsMMELocale=false;
							boolean containsENUKLocale=false;
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								for(int b=0;b<localeTokens.length;b++)
								{
									String loc = localeTokens[b];
									loc = loc.replace("_", "-");
									if(contentLocale.trim().toLowerCase().equals(loc.trim().toLowerCase()))
									{
										// USER CONTAINS MME LOCALE
										containsMMELocale=true;
										break;
									}
									loc = null;
								}
							}
							
							if(containsMMELocale==true)
							{
								// CHECK WHETHER USER CONTAIN ENUK LOCALE
								for(String contentLocale : userSessionBean.getUserLocalesList())
								{
									if(contentLocale.trim().toLowerCase().equals(enukLocale.trim().toLowerCase()))
									{
										// USER CONTAINS ENUK LOCALE
										containsENUKLocale=true;
										break;
									}
								}
							}
							
							
							// PROCEED FOR IDENTIFYING LANGUAGES FOR COMBO
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
										
										/*
										 * IF MME LOCALE IS TRUE AND EN UK LOCALE IS FALSE
										 * EXPLICITY ADD ENUK LOCALE AS WELL
										 */
										if(containsMMELocale==true && containsENUKLocale==false)
										{
											if(enukLocale.trim().toLowerCase().equals(mldDetails.getManualLanguageName().trim().toLowerCase()))
											{
												// addToList
												addToList= true;
											}
										}
										
										// NORMAL FLOW - REMAINS AS IT IS
										if(contentLocale.trim().toLowerCase().equals(mldDetails.getManualLanguageName().trim().toLowerCase()))
										{
											// addToList
											addToList= true;
										}
									
										if(addToList==true)
										{
											boolean proceed = true;
											if(null!=finalLocaleList && finalLocaleList.size()>0)
											{
												for(ManualLanguageDetails existDetails : finalLocaleList)
												{
													if(existDetails.getManualLanguageId()==mldDetails.getManualLanguageId())
													{
														// alreadyAdded - proceed - false
														proceed = false;
														break;
													}
												}
											}

											if(proceed==true)
											{
												finalLocaleList.add(mldDetails);
											}
										}
										mldDetails = null;
									}
								}
								contentLocale = null;
							}
							
							mmeNonWritableLoclaes = null;
							enukLocale = null;
							localeTokens = null;
						}
						
						if(null!=finalLocaleList && finalLocaleList.size()>0)
						{
							ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
							Collections.sort(finalLocaleList,manualLanguageComparator);
							sessionBean.setLanguageList(finalLocaleList);
							manualLanguageComparator = null;
						}
						finalLocaleList=  null;
					}
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "getLanguageList()", e);
		}
	}
	
	private  void getFlagList(CarlineBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
	
	private  void getESICategoryFlagList(CarlineBean sessionBean)
	{
		if(null==sessionBean.getEsiCategoryFlagList() || sessionBean.getEsiCategoryFlagList().size()<=0)
		{
			sessionBean.setEsiCategoryFlagList(new ArrayList<SelectItemDetails>());
			/*
			 * ADD DETAILS FROM PROPERTIES FILE
			 */
			SelectItemDetails si = new SelectItemDetails();
			si.setLabel(msgProps.getProperty("label.esicategory.flag.yes"));
			si.setValue(ApplicationProperties.getProperty("value.esicategory.flag.yes"));
			sessionBean.getEsiCategoryFlagList().add(si);
			si = null;
			
			si = new SelectItemDetails();
			si.setLabel(msgProps.getProperty("label.esicategory.flag.no"));
			si.setValue(ApplicationProperties.getProperty("value.esicategory.flag.no"));
			sessionBean.getEsiCategoryFlagList().add(si);
			si  =null;
		}
	}
	
	private  void getModelTypeList(CarlineBean sessionBean)
	{
		try
		{
			if(null==sessionBean.getModelTypeList() || sessionBean.getModelTypeList().size()<=0)
			{
				sessionBean.setModelTypeList(new ArrayList<SelectItemDetails>());
				ArrayList<SelectItemDetails> list = CarlineDAO.getModelTypeDetails();
				if(null!=list && list.size()>0)
				{
					sessionBean.setModelTypeList(list);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "getModelTypeList()", e);
		}
	}
	
	private void searchWMIOperation(CarlineBean sessionBean)
	{
		try
		{
			// set searhCarlineNameId & codeId with List to null
			sessionBean.setSearchCarlineCodeId(null);
			sessionBean.setSearchCarlineNameId(null);
			sessionBean.setSearchCarlineNameList(new ArrayList<CarlineDetails>());
			sessionBean.setSearchCarlineCodeList(new ArrayList<CarlineDetails>());
			/*
			 * FILTER CARLINE NAME & CARLINE CODE LIST ON THE BASIS OF SELECTED WMI
			 */
			if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()))
			{
				if(null!=uniqueCarlineNameList && uniqueCarlineNameList.size()>0)
				{
					for(int a=0;a<uniqueCarlineNameList.size();a++)
					{
						CarlineDetails cDetails = (CarlineDetails) uniqueCarlineNameList.get(a);
						if(null!=cDetails.getWmiCode())
						{
							if(cDetails.getWmiCode().trim().toLowerCase().
									equals(sessionBean.getSearchWmiId().trim().toLowerCase()))
							{
								/*
								 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
								 */
								boolean add=true;
								if(null!=sessionBean.getSearchCarlineNameList() && sessionBean.getSearchCarlineNameList().size()>0)
								{
									for(int e=0;e<sessionBean.getSearchCarlineNameList().size();e++)
									{
										CarlineDetails c = (CarlineDetails)sessionBean.getSearchCarlineNameList().get(e);
										if(null!=c.getCarlineNameEng() && null!=cDetails.getCarlineNameEng())
										{
											if(c.getCarlineNameEng().trim().toLowerCase().equals(cDetails.getCarlineNameEng().trim().toLowerCase()))
											{
												// already Exists
												add = false;
												break;
											}
											c = null;
										}
									}
								}
								if(add==true)
								{
									if(null!=cDetails.getCarlineNameEng() && !"".equals(cDetails.getCarlineNameEng()))
									{
										if(!"".equals(cDetails.getCarlineNameEng().trim()))
										{
											sessionBean.getSearchCarlineNameList().add(cDetails);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "searchWMIOperation()", e);
		}
	}
	
	private void searchCarlineNameOperation(CarlineBean sessionBean)
	{
		try
		{
			// set searhCarlineCodeId & List to null
			sessionBean.setSearchCarlineCodeId(null);
			sessionBean.setSearchCarlineCodeList(new ArrayList<CarlineDetails>());
			/*
			 * FILTER CARLINE CODE LIST ON THE BASIS OF  WMI CODE & CARLINE NAME, it HAS TO BE SELECTED
			 */
			if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
					null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
			{
				if(null!=uniqueCarlineCodeList && uniqueCarlineCodeList.size()>0)
				{
					for(int a=0;a<uniqueCarlineCodeList.size();a++)
					{
						CarlineDetails cDetails = (CarlineDetails) uniqueCarlineCodeList.get(a);
						if(null!=cDetails.getCarlineNameEng() && null!=cDetails.getWmiCode())
						{
							if(cDetails.getWmiCode().trim().toLowerCase().
									equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
									cDetails.getCarlineNameEng().trim().toLowerCase().
									equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase())	)
							{
								boolean add=true;
								if(null!=sessionBean.getSearchCarlineCodeList() && sessionBean.getSearchCarlineCodeList().size()>0)
								{
									for(int e=0;e<sessionBean.getSearchCarlineCodeList().size();e++)
									{
										CarlineDetails c = (CarlineDetails)sessionBean.getSearchCarlineCodeList().get(e);
										if(null!=c.getCarlineCode() && null!=cDetails.getCarlineCode())
										{
											if(c.getCarlineCode().trim().toLowerCase().equals(cDetails.getCarlineCode().trim().toLowerCase()))
											{
												// already Exists
												add = false;
												break;
											}
											c = null;
										}
									}
								}
								if(add==true)
								{
									if(null!=cDetails.getCarlineCode() && !"".equals(cDetails.getCarlineCode()))
									{
										if(!"".equals(cDetails.getCarlineCode().trim()))
										{
											sessionBean.getSearchCarlineCodeList().add(cDetails);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "searchCarlineNameOperation()", e);
		}
	}
	
	private void addWMIOperation(CarlineBean sessionBean)
	{
		try
		{
			// set searhCarlineNameId & codeId with List to null
			sessionBean.setAddNewCarlineNameId(null);
			sessionBean.setNewCarlineNameEngFieldValue(null);
			sessionBean.setNewCarlineNameRegFieldValue(null);
			
			sessionBean.setAddNewCarlineNameList(new ArrayList<CarlineDetails>());
			ArrayList<CarlineDetails> searchedList = new ArrayList<CarlineDetails>();
			/*
			 * FILTER CARLINE NAME ON THE BASIS OF SELECTED WMI
			 */
			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()) 
					 && !"ADDNEW".equals(sessionBean.getAddNewWmiId()))
			{
				if(null!=uniqueCarlineNameList && uniqueCarlineNameList.size()>0)
				{
					for(int a=0;a<uniqueCarlineNameList.size();a++)
					{
						CarlineDetails cDetails = (CarlineDetails) uniqueCarlineNameList.get(a);
						if(null!=cDetails.getWmiCode())
						{
							if(cDetails.getWmiCode().trim().toLowerCase().
									equals(sessionBean.getAddNewWmiId().trim().toLowerCase()))
							{
								boolean add=true;
								if(null!=searchedList && searchedList.size()>0)
								{
									for(int e=0;e<searchedList.size();e++)
									{
										CarlineDetails c = (CarlineDetails)searchedList.get(e);
										if(null!=c.getCarlineNameEng() && null!=cDetails.getCarlineNameEng())
										{
											if(c.getCarlineNameEng().trim().toLowerCase().equals(cDetails.getCarlineNameEng().trim().toLowerCase()))
											{
												// already Exists
												add = false;
												break;
											}
											c = null;
										}
									}
								}
								if(add==true)
								{
									if(null!=cDetails.getCarlineNameEng() && !"".equals(cDetails.getCarlineNameEng()))
									{
										if(!"".equals(cDetails.getCarlineNameEng().trim()))
										{
											searchedList.add(cDetails);
										}
									}
								}
							}
						}
					}
				}
			}
			
			/*
			 * ADD ADDNEW OPTION
			 * and then ADD Searched List
			 */
			CarlineDetails c = new CarlineDetails();
			c.setCarlineNameEng("ADDNEW");
			c.setCarlineNameReg("ADDNEW");
			sessionBean.getAddNewCarlineNameList().add(c);
			c =null;
			if(null!=searchedList && searchedList.size()>0)
			{
				sessionBean.getAddNewCarlineNameList().addAll(searchedList);
			}
			searchedList  = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "addWMIOperation()", e);
		}
	}
	
	private  void getCarList(CarlineBean sessionBean)
	{
		try
		{
			sessionBean.setAllDataCarList(new ArrayList<CarlineDetails>());
			sessionBean.setCarList(new ArrayList<CarlineDetails>());
			sessionBean.setSearchWmiList(new ArrayList<CarlineDetails>());
			sessionBean.setSearchCarlineNameList(new ArrayList<CarlineDetails>());
			sessionBean.setSearchCarlineCodeList(new ArrayList<CarlineDetails>());
			sessionBean.setAddNewWmiList(new ArrayList<CarlineDetails>());
			sessionBean.setAddNewCarlineNameList(new ArrayList<CarlineDetails>());
			
			uniqueWMIList = new ArrayList<CarlineDetails>();
			uniqueCarlineNameList = new ArrayList<CarlineDetails>();
			uniqueCarlineCodeList = new ArrayList<CarlineDetails>();
			
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				String languageId = identifyLanguageId(sessionBean);
				/*
				 * IDENTIFY & FETCH ON LOCALE CODE
				 */
//				String localeCode=identifyLocaleCodeforMMECountries(sessionBean);
				list = CarlineDAO.getCarlineDetailsList(languageId);
//				list = CarlineDAO.getCarlineDetailsList(localeCode);
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						CarlineDetails carDetails = (CarlineDetails)list.get(i);
						if(null!=carDetails.getFlag() && !"".equals(carDetails.getFlag()))
						{
							// set Label
							if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								carDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								carDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								carDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								carDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(carDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								carDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						
						if(null!=carDetails.getEsiCategoryFlag() && !"".equals(carDetails.getEsiCategoryFlag()))
						{
							// set Label
							if(carDetails.getEsiCategoryFlag().equals(ApplicationProperties.getProperty("value.esicategory.flag.yes")))
							{
								carDetails.setEsiCategoryFlagLabel(msgProps.getProperty("label.esicategory.flag.yes"));
							}
							else if(carDetails.getEsiCategoryFlag().equals(ApplicationProperties.getProperty("value.esicategory.flag.no")))
							{
								carDetails.setEsiCategoryFlagLabel(msgProps.getProperty("label.esicategory.flag.no"));
							} 
						}
						carDetails = null;
					}
					
					sessionBean.setAllDataCarList(list);
					sessionBean.setCarList(list);
					
					
					/*
					 * PREPARE - 
					 * 	 WMI SEARCH AND ADD NEW LIST
					 * 	 CARLINE NAME SEARCH NAME AND ADD NEW LIST
					 * 	 CARLINE CODE SEARCH LIST
					 */
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							boolean addWMI = true;
							
							if(null!=uniqueWMIList && uniqueWMIList.size()>0)
							{
								for(int r=0;r<uniqueWMIList.size();r++)
								{
									CarlineDetails wmiDetails = (CarlineDetails)uniqueWMIList.get(r);
									if(null!=wmiDetails.getWmiCode() && !"".equals(wmiDetails.getWmiCode()) && 
											null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
									{
										if(wmiDetails.getWmiCode().trim().toLowerCase().equals(carDetails.getWmiCode().trim().toLowerCase()))
										{
											addWMI = false;
											// exists
											break;
										}
									}
								}
							}
							
							if(addWMI == true)
							{
								// ONLY WHEN WMI CODE IS NOT NULL
								if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
								{
									if(!"".equals(carDetails.getWmiCode().trim()))
									{
										uniqueWMIList.add(carDetails);
									}
								}
							}
						}
					}
				}
				list = null;
				languageId = null;
				
				CarlineDetails c = new CarlineDetails();
				c.setWmiCode("ADDNEW");
				sessionBean.getAddNewWmiList().add(c);
				
				if(null!=uniqueWMIList)
				{
					sessionBean.setSearchWmiList(uniqueWMIList);
					sessionBean.getAddNewWmiList().addAll(uniqueWMIList);
				}
				
				// EXPLICITY SET ALL DATA LIST TO  UNIQUE CARLINE NAME & CODE LIST
				if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
				{
					uniqueCarlineNameList = sessionBean.getAllDataCarList();
					uniqueCarlineCodeList = sessionBean.getAllDataCarList();
				}
				c= null;
				// DO NOT SET THE UNIQUE LIST TO NULL, AS THEY WILL BE USED FOR FILTERING COMBOS
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "getCarList()", e);
		}
	}

	/**
	 * Function will perform Search
	 * @param sessionBean
	 */
	private  void searchCarline(CarlineBean sessionBean)
	{
		try
		{
			sessionBean.setCarList(new ArrayList<CarlineDetails>());
			
			if(sessionBean.isMnaoContent()==true)
			{
				// NORTH AMERICA OPERATIONS
				if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() && null!=carDetails.getCarlineCode())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						 null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode()  && null!=carDetails.getCarlineCode())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineNameEng() && null!=carDetails.getCarlineCode())
							{
								if(carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineCode())
							{
								if(carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
			}
			else
			{
				// JAPAN OPERATIONS
				/*
				 * SEARCH FILTERS COMBINATIONS
				 * MODEL TYPE + ESI CAT + WMI + CARLINE NAME + CARLINE CODE
					MODEL TYPE + ESI CAT + WMI + CARLINE NAME
					MODEL TYPE + ESI CAT + WMI + CARLINE CODE
					MODEL TYPE + ESI CAT + CARLINE NAME + CARLINE CODE
					MODEL TYPE + WMI CODE + CARLINE NAME + CARLINE CODE
					ESI CAT FLAG + WMI CODE + CARLINE NAME + CARLINE CODE
					ESI CAT FLAG + WMI CODE + CARLINE NAME
					ESI CAT FLAG + WMI CODE + CARLINE CODE
					WMI CODE + CARLINE NAME + CARLINE CODE
					MODEL TYPE + WMI CODE + CARLINE NAME
					MODEL TYPE + WMI CODE + CARLINE CODE
					MODEL TYPE + ESI CAT FLAG + WMI CODE
					MODEL TYPE + ESI CAT FLAG
					MODEL TYPE + WMI CODE
					MODEL TYPE + CARLINE NAME
					MODEL TYPE + CARLINE CODE
					ESI CAT FLAG + WMI CODE
					ESI CAT FLAG + CARLINE NAME
					ESI CAT FLAG + CARLINE CODE
					WMI CODE + CARLINE NAME
					WMI CODE + CARLINE CODE
					CARLINE CODE + CARLIE NAME
					WMI CODE
					MODEL TYPE
					ESI CAT FLAG
					CARLINE NAME
					CARLINE CODE
				 */
				if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + ESI CAT + WMI + CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					// MODEL TYPE + ESI CAT + WMI + CARLINE NAME
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}

				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + ESI CAT + WMI + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode()  
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + ESI CAT + CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + WMI + CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() &&  
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// ESI CAT + WMI + CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					// ESI CAT + WMI + CARLINE NAME 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() )
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// ESI CAT + WMI + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode()  
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// WMI + CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + WMI + CARLINE NAME 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() &&  
									null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE + WMI + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType()  && 
									null!=carDetails.getWmiCode()  
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()))
				{
					// MODEL TYPE + ESI CAT + WMI 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()))
				{
					// MODEL TYPE + ESI CAT 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && null!=carDetails.getEsiCategoryFlag())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()))
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) )
				{
					// MODEL TYPE + WMI 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() && 
									null!=carDetails.getWmiCode() )
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) )
				{
					// MODEL TYPE + CARLINE NAME
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType()  && null!=carDetails.getCarlineNameEng() )
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId())  
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// MODEL TYPE  + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) )
				{
					// ESI CAT + WMI 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getWmiCode() )
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) )
				{
					//  ESI CAT  + CARLINE NAME 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if( null!=carDetails.getEsiCategoryFlag() && 
									null!=carDetails.getCarlineNameEng() )
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// ESI CAT  + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag()  
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) && 	
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					// WMI + CARLINE NAME 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode() && null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()) && 
						null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// WMI + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode()  
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) && 
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()) 
						&& null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// CARLINE NAME + CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineNameEng() 
									&& null!=carDetails.getCarlineCode())
							{
								if(carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) &&
										carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchModelTypeId() && !"".equals(sessionBean.getSearchModelTypeId()))
				{
					// MODEL TYPE 
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getModelType() )
							{
								if(carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getSearchModelTypeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchESICategoryFlagId() && !"".equals(sessionBean.getSearchESICategoryFlagId()))
				{
					// ESI CAT
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getEsiCategoryFlag() )
							{
								if(carDetails.getEsiCategoryFlag().trim().toLowerCase().equals(sessionBean.getSearchESICategoryFlagId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchWmiId() && !"".equals(sessionBean.getSearchWmiId()))
				{
					// WMI
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getWmiCode() )
							{
								if(carDetails.getWmiCode().trim().toLowerCase().equals(sessionBean.getSearchWmiId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineNameId() && !"".equals(sessionBean.getSearchCarlineNameId()))
				{
					// CARLINE NAME
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineNameEng())
							{
								if(carDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getSearchCarlineNameId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}
				else if(null!=sessionBean.getSearchCarlineCodeId() && !"".equals(sessionBean.getSearchCarlineCodeId()))
				{
					// CARLINE CODE
					if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
					{
						for(int a=0;a<sessionBean.getAllDataCarList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getAllDataCarList().get(a);
							if(null!=carDetails.getCarlineCode())
							{
								if(carDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getSearchCarlineCodeId().trim().toLowerCase()) )
								{
									sessionBean.getCarList().add(carDetails);
								}
							}
						}
					}
				}

			}
			
			// UPDATE SR NO.
			if(null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
			{
				for(int e=0;e<sessionBean.getCarList().size();e++)
				{
					CarlineDetails c = (CarlineDetails)sessionBean.getCarList().get(e);
					c.setSrNo(e+1);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "searchCarline()", e);
		}
	}
	
	private  void readParamsFromRequest(CarlineBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new CarlineDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setSearchCarlineCodeId(null);
			sessionBean.setSearchCarlineNameId(null);
			sessionBean.setSearchWmiId(null);
			sessionBean.setSearchESICategoryFlagId(null);
			sessionBean.setSearchModelTypeId(null);
			sessionBean.setAddNewCarlineNameId(null);
			sessionBean.setAddNewWmiId(null);
			sessionBean.setAddNewESICategoryFlagId(null);
			sessionBean.setAddNewModelTypeId(null);
			
			sessionBean.setShowAddNewWMIField(false);
			sessionBean.setShowAddNewCarlineNameField(false);
			sessionBean.setNewCarlineNameEngFieldValue(null);
			sessionBean.setNewCarlineNameRegFieldValue(null);
			sessionBean.setNewWmiFieldValue(null);
			
			sessionBean.setCarListToImport(null);
			
			
			// Create FileItemFactory instance
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
			// By using fileItemFactory instance get the ServletFileUpload
			// object
			// as
			ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);
			// Now get the list of all files by parsing the request
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			// iterate fileItems and check for the Image File
			Iterator<FileItem> iterator = fileItems.iterator();
//			logger.info("readParamsFromRequest() :: iterating File Items");
			while (iterator.hasNext()) 
			{
				FileItem fileItem = iterator.next();
				if (fileItem.isFormField())
				{
//					logger.info("readParamsFromRequest() :: When Fields are Not Form Fields. Check each File Name and set the values accordingly in each attribute.");
					String fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						if (fieldName.equals("CAR_UpdatedRows")) 
						{
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setUpdatedRows(value);
							}
							// set value to null
							value = null;
						}
						
						
						if(fieldName.equals("CAR_SelectedRows"))
						{
							// set the value in sessionBean.setSelectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedRows(value);
							}
							// set value to null
							value = null;
						}
						
						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("CAR_DataTabel_displayPageNo")) 
						{
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_DataTabel_displayPageLen")) 
						{
							// set the value in sessionBean.displayPageLenght
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_SelectedRows")) 
						{
							// set the value in sessionBean.selectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedRows(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_CountryLocale_Code")) 
						{
							// set the value in sessionBean.setCountryLocaleId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setCountryLocaleId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_Lang_Code")) 
						{
							// set the value in sessionBean.setManualLanguageId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setManualLanguageId(value);
							}
							// set value to null
							value = null;
						}
						
						
						if (fieldName.equals("CAR_SearchWmiCode")) 
						{
							// set the value in sessionBean.setSearchWmiId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchWmiId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_SearchCarlineName")) 
						{
							// set the value in sessionBean.setSearchCarlineNameId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchCarlineNameId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_SearchCarlineCode")) 
						{
							// set the value in sessionBean.setSearchCarlineCodeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchCarlineCodeId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_SearchModelType")) 
						{
							// set the value in sessionBean.setSearchCarlineCodeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchModelTypeId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_SearchESICategoryFlag")) 
						{
							// set the value in sessionBean.setSearchCarlineCodeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchESICategoryFlagId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_AddNewWmiCode")) 
						{
							// set the value in sessionBean.setAddNewWmiId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddNewWmiId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_AddNewCarlineNameId")) 
						{
							// set the value in sessionBean.setAddNewCarlineNameId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddNewCarlineNameId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_AddNewModelType")) 
						{
							// set the value in sessionBean.setAddNewModelTypeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddNewModelTypeId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_AddNewESICategoryFlag")) 
						{
							// set the value in sessionBean.setAddNewESICategoryFlagId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddNewESICategoryFlagId(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("CAR_NewWmiCode"))
						{
							// set the value in sessionBean.setNewWmiFieldValue
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewWmiFieldValue(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("CAR_NewCarlineNameEng"))
						{
							// set the value in sessionBean.setNewCarlineNameEngFieldValue
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCarlineNameEngFieldValue(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("CAR_NewCarlineNameReg"))
						{
							// set the value in sessionBean.setNewCarlineNameRegFieldValue
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCarlineNameRegFieldValue(value);
							}
							// set value to null
							value = null;
						}
							
						
						
						if (fieldName.equals("CAR_CarCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setCarlineCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setCarlineCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_ICDisplayCarCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setIcDisplayCarlineCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setIcDisplayCarlineCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_YearStart")) 
						{
							// set the value in sessionBean.getFieldDetails().setYearStart
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setYearStart(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_YearEnd")) 
						{
							// set the value in sessionBean.getFieldDetails().setYearEnd
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setYearEnd(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("CAR_ActionClicked")) 
						{
							// set the value in sessionBean.actionClicked
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setActionClicked(value);
							}
							// set value to null
							value = null;
						}
					}
				}
				else if (!fileItem.isFormField()) 
				{
					if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked())
							&& sessionBean.getActionClicked().equals("FILE_UPLOAD"))
					{
						if(validateFileUpload(sessionBean))
						{
							
							/*
							 * DO EXPLICITY CHECK FOR MANO CONTENT
							 */
							/*
							 * PERFORM IDENTIFICATION FOR WHETHER MNAO CONTENT = TRUE / FALSE
							 */
							performCheckforMNAOContent(sessionBean);
							
							String fileName = fileItem.getName();
							byte[] data = fileItem.get();
							if(null!=fileName && !"".equals(fileName) && null!=data)
							{
								/*
								 * check for CSV
								 */
								String extension="";
								if(fileName.lastIndexOf(".")!=-1)
								{
									extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
									if(null!=extension && !"".equals(extension))
									{
										if(extension.trim().toLowerCase().equals("xlsx") || extension.trim().toLowerCase().equals("xls"))
										{
											/*
											 * call function to operate on uploaded excel
											 */
											executeExcelOperation(sessionBean, data, extension);
										}
										else
										{
											logger.info("readParamsFromRequest() :: Extension is not EXCEL. Throw Message - Uploaded file not supported."); 
											sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
										}
									}
									else
									{
										logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid Excel File.");
										sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
									}
								}
								else
								{
									logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid Excel File.");
									sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
								}
								extension = null;
							}	
							else
							{
								logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid Excel File.");
								sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
							}
							fileName=  null;
							data = null;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	
	private  boolean validateFileUpload(CarlineBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.countrylocale")));
		}
		
		if(null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.language")));
		}
		
		String messages=errorMessage.toString();
		if(null!=messages && !"".equals(messages))
		{
			sessionBean.setErrorMessage(messages);
			messages=  null;
			errorMessage= null;
			return false;
		}
		messages= null;
		errorMessage = null;
		return true;
	}
	
	
	
	private  boolean validate(CarlineDetails fieldDetails, CarlineBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(sessionBean.isMnaoContent()==true)
		{
			boolean addMandatory = false;
			// NORTH AMERICA OPERATIONS
//			if(sessionBean.isYearStartMandatory()==true)
//			{
//				if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
//						null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
//						null==sessionBean.getAddNewWmiId() || "".equals(sessionBean.getAddNewWmiId()) ||  
//						null==sessionBean.getAddNewCarlineNameId() || "".equals(sessionBean.getAddNewCarlineNameId()) || 
//						null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
//						 null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()) || 
//						 null==fieldDetails.getYearEnd() || "".equals(fieldDetails.getYearEnd()))
//				{
//					addMandatory = true;
//				}
//			}
//			else
//			{
//				if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
//						null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
//						null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
//								null==sessionBean.getAddNewWmiId() || "".equals(sessionBean.getAddNewWmiId()) || 
//						null==sessionBean.getAddNewCarlineNameId() || "".equals(sessionBean.getAddNewCarlineNameId()))
//				{
//					addMandatory=  true;
//				}
//			}
			
			/*
			 * YEAR RANGE IS ALWAYS MANDATORY
			 */
			if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
					null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
					null==sessionBean.getAddNewWmiId() || "".equals(sessionBean.getAddNewWmiId()) ||  
					null==sessionBean.getAddNewCarlineNameId() || "".equals(sessionBean.getAddNewCarlineNameId()) || 
					null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
					 null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()) || 
					 null==fieldDetails.getYearEnd() || "".equals(fieldDetails.getYearEnd()))
			{
				addMandatory = true;
			}
			
			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null==sessionBean.getNewWmiFieldValue() || "".equals(sessionBean.getNewWmiFieldValue()))
					{
						addMandatory=true;
					}
				}
			}
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null==sessionBean.getNewCarlineNameEngFieldValue() || "".equals(sessionBean.getNewCarlineNameEngFieldValue()) || 
							null==sessionBean.getNewCarlineNameRegFieldValue() || "".equals(sessionBean.getNewCarlineNameRegFieldValue()))
					{
						addMandatory = true;
					}
					
				}
			}
			
			if(addMandatory==true)
			{
				errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
			}
			
			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
			{
				if(fieldDetails.getCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carcode"),"20"));
				}
			}
			
			if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
			{
				if(fieldDetails.getIcDisplayCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.icdisplaycode"),"20"));
				}
			}

			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewWmiFieldValue() && !"".equals(sessionBean.getNewWmiFieldValue()))
					{
						if(sessionBean.getNewWmiFieldValue().length()>5)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.wmicode"),"5"));
						}
					}
				}
			}
			
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewCarlineNameRegFieldValue() && !"".equals(sessionBean.getNewCarlineNameRegFieldValue()))
					{
						if(sessionBean.getNewCarlineNameRegFieldValue().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carname.reg"),"200"));
						}
					}
					
					if(null!=sessionBean.getNewCarlineNameEngFieldValue() && !"".equals(sessionBean.getNewCarlineNameEngFieldValue()))
					{
						if(sessionBean.getNewCarlineNameEngFieldValue().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carname.eng"),"200"));
						}
					}
				}
			}
			
			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()))
			{
				int sYear = new Integer(fieldDetails.getYearStart()).intValue();
				if(sYear==0)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.field.cannot.zero", msgProps.getProperty("label.yearstart")));
				}
				
				if(fieldDetails.getYearStart().trim().length()!=4)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.yearstart"),"4"));
				}
			}
			
			if(null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
			{
				int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
				if(eYear==0)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.field.cannot.zero", msgProps.getProperty("label.yearend")));
				}
				
				
				if(fieldDetails.getYearEnd().trim().length()!=4)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.yearend"),"4"));
				}
			}
			
			
			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()) && null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
			{
				int sYear = new Integer(fieldDetails.getYearStart()).intValue();
				int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
				if(sYear > eYear)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.data.greater", msgProps.getProperty("label.yearend"),msgProps.getProperty("label.yearstart")));
					
				}
			}


			/*
			 * ENABLE VALIDATION - COMBINATION OF CARLINE CODE + CARLINE NAME + WMI CODE + YEAR START
			 * HAS TO UNIQUE, ELSE DUPLICATE ROW
			 */
			int fYearStart=0;
			String wmiCode=null;
			String carlineNameEng=null;
			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()))
			{
				fYearStart = new Integer(fieldDetails.getYearStart()).intValue();
			}
			
			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewWmiFieldValue() && !"".equals(sessionBean.getNewWmiFieldValue()))
					{
						wmiCode = sessionBean.getNewWmiFieldValue();
					}
				}
				else
				{
					wmiCode = sessionBean.getAddNewWmiId();
				}
			}
			
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewCarlineNameEngFieldValue() && !"".equals(sessionBean.getNewCarlineNameEngFieldValue()))
					{
						carlineNameEng = sessionBean.getNewCarlineNameEngFieldValue();
					}
				}
				else
				{
					carlineNameEng = sessionBean.getAddNewCarlineNameId();
				}
			}
			
			// WHEN ALL MANDATORY FIELDS ARE PROVIDED
			if(addMandatory==false && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
					&& null!=wmiCode && !"".equals(wmiCode) && null!=carlineNameEng && !"".equals(carlineNameEng) && 
			null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
			{
				for(int a=0;a<sessionBean.getCarList().size();a++)
				{
					CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);
					/*
					 * same code with in same year range
					 */
					int existSyear=0;
					if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
					{
						existSyear = new Integer(carDetails.getYearStart()).intValue();
					}
					
					if(carDetails.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase())
							&& fYearStart == existSyear && wmiCode.trim().toLowerCase().equals(carDetails.getWmiCode().trim().toLowerCase()) 
							&& carlineNameEng.trim().toLowerCase().equals(carDetails.getCarlineNameEng().trim().toLowerCase())) 
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.car")));
						break;
					}
					carDetails=  null;
				}
			}
			fYearStart=0;
			wmiCode=  null;
			carlineNameEng = null;
		}
		else
		{
			// JAPAN OPERATIONS
			boolean addMandatory = false;
			if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
					null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
					null==sessionBean.getAddNewModelTypeId() || "".equals(sessionBean.getAddNewModelTypeId()) || 
					null==sessionBean.getAddNewESICategoryFlagId() || "".equals(sessionBean.getAddNewESICategoryFlagId()) || 
							null==sessionBean.getAddNewWmiId() || "".equals(sessionBean.getAddNewWmiId()) || 
					null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
					null==sessionBean.getAddNewCarlineNameId() || "".equals(sessionBean.getAddNewCarlineNameId()))
			{
				addMandatory = true;
			}
		
			
			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null==sessionBean.getNewWmiFieldValue() || "".equals(sessionBean.getNewWmiFieldValue()))
					{
						addMandatory= true;
					}
				}
			}
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null==sessionBean.getNewCarlineNameEngFieldValue() || "".equals(sessionBean.getNewCarlineNameEngFieldValue()) || 
							null==sessionBean.getNewCarlineNameRegFieldValue() || "".equals(sessionBean.getNewCarlineNameRegFieldValue()))
					{
						addMandatory = true;
					}
					
				}
			}
			
			if(addMandatory==true)
			{
				errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
			}
			
			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
			{
				if(fieldDetails.getCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carcode"),"20"));
				}
			}
			

			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewWmiFieldValue() && !"".equals(sessionBean.getNewWmiFieldValue()))
					{
						if(sessionBean.getNewWmiFieldValue().length()>5)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.wmicode"),"5"));
						}
					}
				}
			}
			
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewCarlineNameRegFieldValue() && !"".equals(sessionBean.getNewCarlineNameRegFieldValue()))
					{
						if(sessionBean.getNewCarlineNameRegFieldValue().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carname.reg"),"200"));
						}
					}
					
					if(null!=sessionBean.getNewCarlineNameEngFieldValue() && !"".equals(sessionBean.getNewCarlineNameEngFieldValue()))
					{
						if(sessionBean.getNewCarlineNameEngFieldValue().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.carname.eng"),"200"));
						}
					}
				}
			}
			
			if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
			{
				if(fieldDetails.getIcDisplayCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.icdisplaycode"),"20"));
				}
			}
			
			/*
			 * ENABLE VALIDATION - COMBINATION OF MODEL TYPE / CARLINE CODE / WMI CODE 
			 * HAS TO UNIQUE, ELSE DUPLICATE ROW
			 */
			String wmiCode=null;
			String carlineNameEng = null;
			if(null!=sessionBean.getAddNewWmiId() && !"".equals(sessionBean.getAddNewWmiId()))
			{
				if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewWmiFieldValue() && !"".equals(sessionBean.getNewWmiFieldValue()))
					{
						wmiCode = sessionBean.getNewWmiFieldValue();
					}
				}
				else
				{
					wmiCode = sessionBean.getAddNewWmiId();
				}
			}
			
			if(null!=sessionBean.getAddNewCarlineNameId() && !"".equals(sessionBean.getAddNewCarlineNameId()))
			{
				if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
				{
					if(null!=sessionBean.getNewCarlineNameEngFieldValue() && !"".equals(sessionBean.getNewCarlineNameEngFieldValue()))
					{
						carlineNameEng = sessionBean.getNewCarlineNameEngFieldValue();
					}
				}
				else
				{
					carlineNameEng = sessionBean.getAddNewCarlineNameId();
				}
			}
			// WHEN ALL MANDATORY FIELDS ARE PROVIDED
			if(addMandatory==false && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
					&& null!=wmiCode && !"".equals(wmiCode)  && null!=carlineNameEng && !"".equals(carlineNameEng) &&  
					null!=sessionBean.getAddNewModelTypeId() && !"".equals(sessionBean.getAddNewModelTypeId()) && 
			null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
			{
				for(int a=0;a<sessionBean.getCarList().size();a++)
				{
					CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);
					
					if(carDetails.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase())
							&& carDetails.getModelType().trim().toLowerCase().equals(sessionBean.getAddNewModelTypeId().trim().toLowerCase()) &&  
							 wmiCode.trim().toLowerCase().equals(carDetails.getWmiCode().trim().toLowerCase()) &&  
							 carlineNameEng.trim().toLowerCase().equals(carDetails.getCarlineNameEng().trim().toLowerCase())) 
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.car")));
						break;
					}
					carDetails=  null;
				}
			}
			wmiCode=  null; 
			carlineNameEng = null;
		}
		
		String messages=errorMessage.toString();
		if(null!=messages && !"".equals(messages))
		{
			sessionBean.setErrorMessage(messages);
			messages=  null;
			errorMessage= null;
			return false;
		}
		messages= null;
		errorMessage = null;
		return true;
	}
	
	private  boolean validateUpdate(CarlineDetails fieldDetails, CarlineBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(sessionBean.isMnaoContent()==true)
		{
			// NORTH AMERICA OPERATIONS
//			if(sessionBean.isYearStartMandatory()==true)
//			{
//				if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
//						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
//						null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
//						null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) 
//						|| null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()))
//				{
//					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//					{
//						errorMessage.append("<MSG_TOKEN>");
//					}
//					errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
//				}
//
//			}
//			else
//			{
//				if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
//						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
//						null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
//						null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()))
//				{
//					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//					{
//						errorMessage.append("<MSG_TOKEN>");
//					}
//					errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
//				}
//
//			}
			
			/*
			 * YEAR RANGE IS ALWAYS MANDATORY
			 * DATE 23 JUNE 2018
			 */
			if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
					null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
					null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
					null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) 
					|| null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()) || 
					null==fieldDetails.getYearEnd() || "".equals(fieldDetails.getYearEnd()))
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
			}

			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
			{
				if(fieldDetails.getCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carcode")+",20,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}

			if(null!=fieldDetails.getCarlineNameReg() && !"".equals(fieldDetails.getCarlineNameReg()))
			{
				if(fieldDetails.getCarlineNameReg().length()>200)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carname.reg")+",200,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}

			if(null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()))
			{
				if(fieldDetails.getCarlineNameEng().length()>200)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carname.eng")+",200,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;

				}
			}

			if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
			{
				if(fieldDetails.getWmiCode().length()>5)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.wmicode")+",5,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}

			if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
			{
				if(fieldDetails.getIcDisplayCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.icdisplaycode")+",20,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}
			
			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()))
			{
				int sYear = new Integer(fieldDetails.getYearStart()).intValue();
				if(sYear==0)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.yearstart")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.field.cannot.zero.for.row"));
					data = null;
					id = null;
				}

				if(fieldDetails.getYearStart().trim().length()!=4)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.yearstart")+",4,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
					data = null;
					id = null;
				}
			}

			if(null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
			{
				int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
				if(eYear==0)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.yearend")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.field.cannot.zero.for.row"));
					data = null;
					id = null;
				}

				if(fieldDetails.getYearEnd().trim().length()!=4)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.yearend")+",4,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
					data = null;
					id = null;
				}
			}

			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()) 
					&& null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
			{
				int sYear = new Integer(fieldDetails.getYearStart()).intValue();
				int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
				if(sYear > eYear)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.yearend")+","+msgProps.getProperty("label.yearstart")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.data.greater.for.row"));
					data = null;
					id = null;
				}
			}


			/*
			 * VALIDATION ON UNIQUE NESS 
				CARLINE CODE ID NOT SAME, CARLINE CODE, YEAR START, WMI CODE CANNOT BE SAME
			 */
			int fYearStart=0;
			if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()))
			{
				fYearStart = new Integer(fieldDetails.getYearStart()).intValue();
			}

			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
					&& null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
					null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
					null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
			{
				for(int a=0;a<sessionBean.getCarList().size();a++)
				{
					CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);

					int existYStart=0;
					if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
					{
						existYStart = new Integer(carDetails.getYearStart()).intValue();
					}

					if(carDetails.getCarlineCodeId() != fieldDetails.getCarlineCodeId() && 
							carDetails.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase())
							&& fYearStart == existYStart  
							&& carDetails.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase()) && 
							carDetails.getCarlineNameEng().trim().toLowerCase().equals(fieldDetails.getCarlineNameEng().trim().toLowerCase())) 
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.car")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
					carDetails=  null;
				}
			}
			fYearStart=0;
		}
		else
		{
			// JAPAN OPERATIONS

			if(null==fieldDetails.getModelType() || "".equals(fieldDetails.getModelType()) || 
					null==fieldDetails.getEsiCategoryFlag() || "".equals(fieldDetails.getEsiCategoryFlag()) || 
					null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
					null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
					null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
					null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()))
			{
				errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
			}
			
			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
			{
				if(fieldDetails.getCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carcode")+",20,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}
			
			if(null!=fieldDetails.getCarlineNameReg() && !"".equals(fieldDetails.getCarlineNameReg()))
			{
				if(fieldDetails.getCarlineNameReg().length()>200)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carname.reg")+",200,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}
			
			if(null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()))
			{
				if(fieldDetails.getCarlineNameEng().length()>200)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.carname.eng")+",200,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;

				}
			}

			if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
			{
				if(fieldDetails.getWmiCode().length()>5)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.wmicode")+",5,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}
			
			if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
			{
				if(fieldDetails.getIcDisplayCarlineCode().length()>20)
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.icdisplaycode")+",20,"+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
					data = null;
					id = null;
				}
			}
			
			/*
			 * VALIDATION ON UNIQUE NESS 
				CARLINE CODE ID NOT SAME, CARLINE CODE, MODEL TYPE, WMI CODE CANNOT BE SAME
			 */
			if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
					&& null!=fieldDetails.getModelType() && !"".equals(fieldDetails.getModelType()) && 
					null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
					null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
					null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
			{
				for(int a=0;a<sessionBean.getCarList().size();a++)
				{
					CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);

					if(carDetails.getCarlineCodeId() != fieldDetails.getCarlineCodeId() && 
							carDetails.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase()) 
							&& carDetails.getModelType().trim().toLowerCase().equals(fieldDetails.getModelType().trim().toLowerCase())
							&& carDetails.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase()) 
							&& carDetails.getCarlineNameEng().trim().toLowerCase().equals(fieldDetails.getCarlineNameEng().trim().toLowerCase())) 
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.car")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
					carDetails=  null;
				}
			}
		}
		String messages=errorMessage.toString();
		if(null!=messages && !"".equals(messages))
		{
			sessionBean.setErrorMessage(messages);
			messages=  null;
			errorMessage= null;
			return false;
		}
		messages= null;
		errorMessage = null;
		return true;
	}
	
	private  void saveCarlineDetails(CarlineBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */
				// update Selected Country Locale Code & Manual Language Code
				if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
				{
					for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
					{
						CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
						if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
						{
							// USE DESC FOR IDENTIFING CODE
							sessionBean.getFieldDetails().setCountryLocaleCode(clDetails.getCountryLocaleDesc());
							break;
						}
					}
				}
				if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
				{
					for(int a=0;a<sessionBean.getLanguageList().size();a++)
					{
						ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
						if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
						{
							// USE NAME FOR IDENTIFING CODE
							sessionBean.getFieldDetails().setManualLanguageCode(mlDetails.getManualLanguageName());
							break;
						}
					}
				}
				
				if(null!=sessionBean.getAddNewWmiId())
				{
					if(sessionBean.getAddNewWmiId().equals("ADDNEW"))
					{
						sessionBean.getFieldDetails().setWmiCode(sessionBean.getNewWmiFieldValue());
					}
					else
					{
						sessionBean.getFieldDetails().setWmiCode(sessionBean.getAddNewWmiId());
					}
				}
				
				// IDENTIFY MODEL TYPE & ESI CAT FLAG FOR MC & MME MARKET
				if(sessionBean.isMnaoContent()==false)
				{
					if(null!=sessionBean.getAddNewModelTypeId() && !"".equals(sessionBean.getAddNewModelTypeId()))
					{
						sessionBean.getFieldDetails().setModelType(sessionBean.getAddNewModelTypeId());
					}
					if(null!=sessionBean.getAddNewESICategoryFlagId() && !"".equals(sessionBean.getAddNewESICategoryFlagId()))
					{
						sessionBean.getFieldDetails().setEsiCategoryFlag(sessionBean.getAddNewESICategoryFlagId());
					}
				}
				
				
				if(null!=sessionBean.getAddNewCarlineNameId())
				{
					if(sessionBean.getAddNewCarlineNameId().equals("ADDNEW"))
					{
						sessionBean.getFieldDetails().setCarlineNameEng(sessionBean.getNewCarlineNameEngFieldValue());
						sessionBean.getFieldDetails().setCarlineNameReg(sessionBean.getNewCarlineNameRegFieldValue());
					}
					else
					{
						sessionBean.getFieldDetails().setCarlineNameEng(sessionBean.getAddNewCarlineNameId());
						/*
						 * IDENTIFY REGIONAL NAME
						 */
						if(null!=sessionBean.getAllDataCarList() && sessionBean.getAllDataCarList().size()>0)
						{
							for(int r=0;r<sessionBean.getAllDataCarList().size();r++)
							{
								CarlineDetails cDetails = (CarlineDetails) sessionBean.getAllDataCarList().get(r);
								if(sessionBean.isMnaoContent()==true)
								{
									// NORTH AMERICA OPERATIONS - INCLUDE WMI AS WELL
									if(sessionBean.getCountryLocaleId().equals(String.valueOf(cDetails.getCountryLocaleId())) 
											&& sessionBean.getManualLanguageId().equals(String.valueOf(cDetails.getManualLanguageId()))
											&& sessionBean.getFieldDetails().getWmiCode().trim().toLowerCase().equals(cDetails.getWmiCode().trim().toLowerCase()) 
											&& sessionBean.getAddNewCarlineNameId().trim().toLowerCase().equals(cDetails.getCarlineNameEng().trim().toLowerCase()))
									{
										sessionBean.getFieldDetails().setCarlineNameReg(cDetails.getCarlineNameReg());
										break;
									}
								}
								else
								{
									// JAPAN OPERATIONS - INCLUDE WMI & MODEL TYPE
									if(sessionBean.getCountryLocaleId().equals(String.valueOf(cDetails.getCountryLocaleId())) 
											&& sessionBean.getManualLanguageId().equals(String.valueOf(cDetails.getManualLanguageId())) 
											&& sessionBean.getFieldDetails().getWmiCode().trim().toLowerCase().equals(cDetails.getWmiCode().trim().toLowerCase()) 
											&& sessionBean.getFieldDetails().getModelType().trim().toLowerCase().equals(cDetails.getModelType().trim().toLowerCase()) 
											&& sessionBean.getAddNewCarlineNameId().trim().toLowerCase().equals(cDetails.getCarlineNameEng().trim().toLowerCase()))
									{
										sessionBean.getFieldDetails().setCarlineNameReg(cDetails.getCarlineNameReg());
										break;
									}
								}
							}
						}
					}
				}
				
				
				
				sessionBean.getFieldDetails().setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
				sessionBean.getFieldDetails().setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = CarlineDAO.saveCarlineDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveCarlineDetails :: Carline Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.car")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setCarList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setAddNewWmiId(null);
					sessionBean.setAddNewCarlineNameId(null);
					sessionBean.setNewWmiFieldValue(null);
					sessionBean.setNewCarlineNameEngFieldValue(null);
					sessionBean.setNewCarlineNameRegFieldValue(null);
					
					sessionBean.setAddNewModelTypeId(null);
					sessionBean.setAddNewESICategoryFlagId(null);
					/*
					 * call getLanguageList
					 */
					getCarList(sessionBean);
				}
				else
				{
					logger.info("saveCarlineDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.car")));
				}
			}
			else
			{
				logger.info("saveCarlineDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "saveCarlineDetails()", e);
		}
	}
	
	private  void editCarlineDetails(HttpServletRequest request, CarlineBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getCarList() && !"".equals(sessionBean.getCarList().size()>0))
			{
				for(int a=0;a<sessionBean.getCarList().size();a++)
				{
					CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);
					carDetails.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCarList() && !"".equals(sessionBean.getCarList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCarList().size();a++)
							{
								CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);
								// MALE EDITABLE ON CAR LINE CODE ID
								if(rowId.equals(String.valueOf(carDetails.getCarlineCodeId())))
								{
									logger.info("editCarlineDetails :: Making Row No {"+carDetails.getSrNo()+"} Editable.");
									carDetails.setEditableFlag(true);
									break;
								}
							}
							rowId=  null;
						}
					}
					rows = null;
				}
				// show Update Button
				sessionBean.setShowUpdate(true);
			}
			else
			{
				logger.info("editCarlineDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "editCarlineDetails()", e);
		}
	}

	private  void deleteCarlineDetails(HttpServletRequest request, CarlineBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String deleteIds=sessionBean.getSelectedRows();
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = CarlineDAO.deleteCarlineDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.car")));
						/*
						 * call getCarList
						 */
						getCarList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.car")));
					}
				}
			}
			else
			{
				logger.info("deleteCarlineDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "deleteCarlineDetails()", e);
		}
	}

	
	private  void activeCarDetails(HttpServletRequest request, CarlineBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String activeIds=sessionBean.getSelectedRows();
				if(null!=activeIds && !"".equals(activeIds))
				{
					if(activeIds.endsWith(","))
					{
						activeIds = activeIds.substring(0,activeIds.length()-1);
					}
					/*
					 * for each active id - fetch Old status from the carlineList
					 */
					List<Map<String, String>> activeIdsList = new ArrayList<Map<String, String>>();
					String[] tok = activeIds.split(",");
					CarlineDetails carDetails = null;
					Map<String,String> dataMap = null;
					if(null!=tok && tok.length>0)
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
							{
								carDetails  =null;
								for(int b=0;b<sessionBean.getCarList().size();b++)
								{
									carDetails=  (CarlineDetails)sessionBean.getCarList().get(b);
									if(String.valueOf(carDetails.getCarlineCodeId()).equals(tok[a]))
									{
										dataMap= new HashMap<String, String>();
										dataMap.put("ID", tok[a]);
										dataMap.put("FLAG", carDetails.getOldFlag());
										activeIdsList.add(dataMap);
										dataMap=  null;
									}
									carDetails = null;
								}
							}
						}
					}
					tok = null;
					dataMap=  null;
					
					boolean bool = CarlineDAO.activeCarDetails(activeIdsList);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.car")));
						/*
						 * call getMissionBookList
						 */
						getCarList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.car")));
					}
					activeIds=  null;
					activeIdsList = null;
				}
			}
			else
			{
				logger.info("activeCarDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "activeCarDetails()", e);
		}
	}
	
	
	private  void updateCarlineDetails(HttpServletRequest request, CarlineBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<CarlineDetails> updateDataList = new ArrayList<CarlineDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				// break all fieldStrings
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
					{
						for(int i=0;i<sessionBean.getCarList().size();i++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(i);
							if(carDetails.isEditableFlag()==true)
							{
								// set fields empty 
								carDetails.setCarlineCode("");
								carDetails.setCarlineNameEng("");
								carDetails.setCarlineNameReg("");
								carDetails.setYearStart("");
								carDetails.setYearEnd("");
								carDetails.setFlag("");
								carDetails.setWmiCode("");
								carDetails.setModelType("");
								carDetails.setEsiCategoryFlag("");
								carDetails.setIcDisplayCarlineCode("");
								/*
								 * fetch the values from request
								 * and set in CarList
								 */
								String carCodeId="CAR_CarList_Code_"+String.valueOf(carDetails.getCarlineCodeId());
								String carNameRegId="CAR_CarList_Name_Reg_"+String.valueOf(carDetails.getCarlineCodeId());
								String carNameEnId="CAR_CarList_Name_En_"+String.valueOf(carDetails.getCarlineCodeId());
								String yearStart ="CAR_CarList_YearStart_"+String.valueOf(carDetails.getCarlineCodeId());
								String yearEnd="CAR_CarList_YearEnd_"+String.valueOf(carDetails.getCarlineCodeId());
								String flag="CAR_LangList_Flag_"+String.valueOf(carDetails.getCarlineCodeId());
								String wmiCode="CAR_CarList_Wmi_Code_"+String.valueOf(carDetails.getCarlineCodeId());
								String modelType="CAR_CarList_ModelType_"+String.valueOf(carDetails.getCarlineCodeId());
								String esiCatFlag="CAR_CarList_ESICatFlag_"+String.valueOf(carDetails.getCarlineCodeId());
								String icDisplayCode="CAR_CarList_ICDisplayCode_"+String.valueOf(carDetails.getCarlineCodeId());
								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(carCodeId))
										{
											// BREAK TOKEN STRING
											carDetails.setCarlineCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(carNameRegId))
										{
											// BREAK TOKEN STRING
											carDetails.setCarlineNameReg(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(carNameEnId))
										{
											carDetails.setCarlineNameEng(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(yearStart))
										{
											carDetails.setYearStart(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(yearEnd))
										{
											carDetails.setYearEnd(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(flag))
										{
											carDetails.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(wmiCode))
										{
											carDetails.setWmiCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(modelType))
										{
											carDetails.setModelType(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(esiCatFlag))
										{
											carDetails.setEsiCategoryFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(icDisplayCode))
										{
											carDetails.setIcDisplayCarlineCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key=null;
										token=null;
									}
								}

								// set all request params ids to null
								carCodeId = null;
								carNameEnId = null;
								carNameRegId=null;
								yearStart=null;
								yearEnd=null;
								flag= null;
								wmiCode = null;
								modelType = null;
								esiCatFlag= null;
								icDisplayCode=null;
							}
						}

						for(int i=0;i<sessionBean.getCarList().size();i++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(i);
							if(carDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate carDetails Object
								 */
								if(validateUpdate(carDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(carDetails);
								}
								else
								{
									// set updateList to null;
									updateDataList=  null;
									break;
								}
							}
						}

						if(null!=updateDataList && updateDataList.size()>0)
						{
							logger.info("updateCarlineDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							
							String countryLocaleCode="";
							String languageCode="";
							// update Selected Country Locale Code & Manual Language Code
							if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
							{
								for(int b=0;b<sessionBean.getCountryLocaleList().size();b++)
								{
									CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(b);
									if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
									{
										// USE DESC FOR IDENTIFING CODE
										countryLocaleCode=clDetails.getCountryLocaleDesc();
										break;
									}
								}
							}
							if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
							{
								for(int b=0;b<sessionBean.getLanguageList().size();b++)
								{
									ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(b);
									if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
									{
										// USE NAME FOR IDENTIFING CODE
										languageCode= mlDetails.getManualLanguageName();
										break;
									}
								}
							}
							
							
							for(int a=0;a<updateDataList.size();a++)
							{
								CarlineDetails carDetails = (CarlineDetails)updateDataList.get(a);
								carDetails.setCountryLocaleCode(countryLocaleCode);
								carDetails.setManualLanguageCode(languageCode);
							}
							// update carlineData in Database
							updateDataList = CarlineDAO.updateCarlineDetails(updateDataList);
							for(int a=0;a<updateDataList.size();a++)
							{
								CarlineDetails carDetails = (CarlineDetails)updateDataList.get(a);
								carDetails.setCountryLocaleCode(countryLocaleCode);
								carDetails.setManualLanguageCode(languageCode);
								if(carDetails.isSaveStatusWhileImport() ==true)
								{
									logger.info("updateCarlineDetails :: Car Details updated successfully for Row No :: > " + carDetails.getSrNo());
									successMessage = successMessage+String.valueOf(carDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateCarlineDetails :: Failed to Update Car Details for Row No :: >  "+ carDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(carDetails.getSrNo())+",";	
								}
								carDetails=  null;
							}
							countryLocaleCode=null;
							languageCode = null;
							
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.car"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.car"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateCarlineDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setAllDataCarList(null);
								sessionBean.setCarList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getCarList
								 */
								getCarList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateCarlineDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getCarList() && sessionBean.getCarList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getCarList().size();a++)
										{
											CarlineDetails cDetails = (CarlineDetails)sessionBean.getCarList().get(a);
											cDetails.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(cDetails.getSrNo())))
												{
													cDetails.setEditableFlag(true);
													break;
												}
											}
										}
									}
									tokens= null;
								}
							}
							successMessage= null;
							errorMessage= null;
						}
						updateDataList= null;
					}
				}
				updatedRows = null;
				updatedRowsTokens = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "updateCarlineDetails()", e);
		}
	}
	
	private  void executeExcelOperation(CarlineBean sessionBean, byte[] data, String extension)
	{
		try
		{
			/*
			 * proceed for uploading and parsing.
			 */
			readExcelData(data, sessionBean, extension);
			
			/*
			 * call function to validate ALL THE EXCEL ROWS
			 */
			if(validateExcelRowData(sessionBean))
			{
				StringBuilder errorMessage = new StringBuilder();
				int errorCount=0;
				String duplicateRowNo="";
				int duplicateRowsCount=0;
				int failureCount=0;
				ArrayList<CarlineDetails> listToSave = new ArrayList<CarlineDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<CarlineDetails> listToDelete = new ArrayList<CarlineDetails>();
				for(int i=0;i<sessionBean.getCarListToImport().size();i++)
				{
					CarlineDetails fieldDetails = (CarlineDetails)sessionBean.getCarListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));

					/*
					 * ACTION = D -> this row is a delete. Keep it out of listToSave so
					 * the existing create-or-update logic stays exactly as it was.
					 */
					if(ImportActionUtils.isDeleteAction(fieldDetails.getImportAction()))
					{
						listToDelete.add(fieldDetails);
						continue;
					}
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							CarlineDetails existingDetails = (CarlineDetails)listToSave.get(j);
							
							/*
							 * IDENTIFY DUPLICATE ROW ON THE BASIS OF
							 * IF MNAO 
							 * CAR LINE CODE
							 * CARLINE NAME ENG
							 * CARLINE NAME REG
							 * WMI CODE
							 * YEAR START
							 * YEAR END
							 * IC DISPLAY CARLINE CODE
							 * 
							 * ELSE 
							 * CARLINE CODE
							 * CARLINE NAME ENG
							 * CARLINE NAME REG
							 * WMI CODE
							 * MODEL TYPE
							 * ESI CAT FLAG
							 * IC DISPLAY CARLINE CODE
							 */
							if(sessionBean.isMnaoContent())
							{
								if(null==fieldDetails.getWmiCode())
								{
									fieldDetails.setWmiCode("");
								}
								if(null== fieldDetails.getYearStart())
								{
									fieldDetails.setYearStart("");
								}
								if(null== fieldDetails.getYearEnd())
								{
									fieldDetails.setYearEnd("");
								}
								if(null==fieldDetails.getIcDisplayCarlineCode())
								{
									fieldDetails.setIcDisplayCarlineCode("");
								}
								if(null==existingDetails.getWmiCode())
								{
									existingDetails.setWmiCode("");
								}
								if(null==existingDetails.getYearStart())
								{
									existingDetails.setYearStart("");
								}
								if(null==existingDetails.getYearEnd())
								{
									existingDetails.setYearEnd("");
								}
								if(null==existingDetails.getIcDisplayCarlineCode())
								{
									existingDetails.setIcDisplayCarlineCode("");
								}
								if(fieldDetails.getCarlineCode().equals(existingDetails.getCarlineCode()) 
										&& fieldDetails.getCarlineNameEng().equals(existingDetails.getCarlineNameEng())
										&& fieldDetails.getCarlineNameReg().equals(existingDetails.getCarlineNameReg())
										&& fieldDetails.getWmiCode().equals(existingDetails.getWmiCode())
										&& fieldDetails.getYearStart().equals(existingDetails.getYearStart())
										&& fieldDetails.getYearEnd().equals(existingDetails.getYearEnd()) && 
										fieldDetails.getIcDisplayCarlineCode().equals(existingDetails.getIcDisplayCarlineCode()))
								{
									if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
									{
										duplicateRowNo = duplicateRowNo+",";
									}
									duplicateRowNo = duplicateRowNo+String.valueOf(fieldDetails.getSrNo());
									// already Added = SKIP IT
									addToList = false;
									break;
								}
							}
							else
							{
								if(null==fieldDetails.getWmiCode())
								{
									fieldDetails.setWmiCode("");
								}
								if(null==existingDetails.getWmiCode())
								{
									existingDetails.setWmiCode("");
								}
								if(null==fieldDetails.getIcDisplayCarlineCode())
								{
									fieldDetails.setIcDisplayCarlineCode("");
								}
								if(null==existingDetails.getIcDisplayCarlineCode())
								{
									existingDetails.setIcDisplayCarlineCode("");
								}
								
								if(fieldDetails.getCarlineCode().equals(existingDetails.getCarlineCode()) 
										&& fieldDetails.getCarlineNameEng().equals(existingDetails.getCarlineNameEng())
										&& fieldDetails.getCarlineNameReg().equals(existingDetails.getCarlineNameReg())
										&& fieldDetails.getWmiCode().equals(existingDetails.getWmiCode())
										&& fieldDetails.getModelType().equals(existingDetails.getModelType())
										&& fieldDetails.getEsiCategoryFlag().equals(existingDetails.getEsiCategoryFlag()) 
										&& fieldDetails.getIcDisplayCarlineCode().equals(existingDetails.getIcDisplayCarlineCode()))
								{
									if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
									{
										duplicateRowNo = duplicateRowNo+",";
									}
									duplicateRowNo = duplicateRowNo+String.valueOf(fieldDetails.getSrNo());
									// already Added = SKIP IT
									addToList = false;
									break;
								}
							}
						}
					}
					if(addToList==true)
					{
						listToSave.add(fieldDetails);
					}
				}
				
				if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
				{
					if(duplicateRowNo.endsWith(","))
					{
						duplicateRowNo = duplicateRowNo.substring(0, duplicateRowNo.length()-1);
					}
					String[] rows = duplicateRowNo.split(",");
					if(null!=rows && rows.length>0)
					{
						duplicateRowsCount = rows.length;
						for(int a=0;a<rows.length;a++)
						{
							// increment errorCount by 1
							errorCount++;
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.car"), String.valueOf(rows[a])));
						}
					}
					rows = null;
				}
				duplicateRowNo = null;
				
				
				if(null!=listToSave && listToSave.size()>0)
				{
					String countryLocaleCode="";
					String languageCode="";
					// update Selected Country Locale Code & Manual Language Code
					if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
					{
						for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
						{
							CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
							if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
							{
								// USE DESC FOR IDENTIFING CODE
								countryLocaleCode = clDetails.getCountryLocaleDesc();
								break;
							}
						}
					}
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
							{
								// USE NAME FOR IDENTIFING CODE
								languageCode = mlDetails.getManualLanguageName();
								break;
							}
						}
					}
					
					Connection conn = null;
					String connClosed="N";
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
					}
					/*
					 * ITERATE AND START SAVING EACH ROW
					 */
					String successLineNo="";
					String errorLineNo="";
					int successCount=0;
					
					for(int i=0;i<listToSave.size();i++)
					{
						CarlineDetails fieldDetails = (CarlineDetails)listToSave.get(i);
						fieldDetails.setCountryLocaleCode(countryLocaleCode);
						fieldDetails.setManualLanguageCode(languageCode);
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						
						boolean saveVinData = CarlineDAO.importCarlineDetails(fieldDetails, conn, connClosed);
						if(saveVinData == true)
						{
							logger.info("executeExcelOperation() :: CAR Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("executeExcelOperation() :: Failed to Import CAR Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails= null;
					}
					
					countryLocaleCode = null;
					languageCode = null;
					connClosed = null;
					try
					{
						if(null!=conn)
						{
							// close connection object
							conn.close();
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
					}
					conn = null;
					
					
					if(null!=successLineNo && !"".equals(successLineNo))
					{
						if(successLineNo.endsWith(","))
						{
							successLineNo = successLineNo.substring(0, successLineNo.length()-1);
						}
						String[] successRows = successLineNo.split(",");
						if(null!=successRows && successRows.length>0)
						{
							successCount = successRows.length;
						}
						if(successCount>0)
						{
							sessionBean.setSuccessMessage(msgProps.addMessage("import.success.count.message", String.valueOf(successCount)));
						}
						successRows= null;
//						successLineNo = "( "+successLineNo+" )";
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.car"), successLineNo ));
						
						/*
						 * call function to load updated vin list
						 */
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						getCarList(sessionBean);
					}
					
					if(null!=errorLineNo && !"".equals(errorLineNo))
					{
						if(errorLineNo.endsWith(","))
						{
							errorLineNo= errorLineNo.substring(0, errorLineNo.length()-1);
						}
						String[] errorRows = errorLineNo.split(",");
						if(null!=errorRows && errorRows.length>0)
						{
							failureCount = errorRows.length;
							for(int a=0;a<errorRows.length;a++)
							{
								// increment errorCount by 1
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.car"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.car"), errorLineNo));
					}
					
					successLineNo = null;
					errorLineNo = null;		
				}
				// only complain about an empty file when there is nothing to delete either
				else if(null==listToDelete || listToDelete.size()<=0)
				{
					/*
					 * NO DATA FOUND TO IMPORT
					 */
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					// increment erroCount by 1
					errorCount++;
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.car")));
				}
				/*
				 * ACTION = D ROWS - SOFT DELETE.
				 *
				 * The Excel carries no primary key, so each row is located with the SAME
				 * unique combination the create-or-update path uses, and the ids are handed
				 * to this screen's EXISTING delete method - so the import delete behaves
				 * identically to the on-screen delete (sync status, related tables, ...).
				 */
				if(null!=listToDelete && listToDelete.size()>0)
				{
					String deleteSuccessLineNo="";
					String deleteErrorLineNo="";
					java.util.List<Long> deleteIdList = new ArrayList<Long>();
					Connection deleteConn = null;
					try
					{
						deleteConn = DBConnectionHelper.getConnection();
						for(int i=0;i<listToDelete.size();i++)
						{
							CarlineDetails deleteDetails = (CarlineDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = CarlineDAO.findExistingIdForImport(deleteDetails, deleteConn, sessionBean.isMnaoContent());
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
							}
							if(existingId>0)
							{
								deleteIdList.add(new Long(existingId));
								if(null!=deleteSuccessLineNo && !"".equals(deleteSuccessLineNo))
								{
									deleteSuccessLineNo = deleteSuccessLineNo+",";
								}
								deleteSuccessLineNo = deleteSuccessLineNo+String.valueOf(deleteDetails.getSrNo());
							}
							else
							{
								/* NO ACTIVE ROW MATCHES - REPORT IT, never a silent no-op */
								if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
								{
									deleteErrorLineNo = deleteErrorLineNo+",";
								}
								deleteErrorLineNo = deleteErrorLineNo+String.valueOf(deleteDetails.getSrNo());
							}
							deleteDetails = null;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
					}
					finally
					{
						try
						{
							if(null!=deleteConn)
							{
								deleteConn.close();
							}
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
						}
						deleteConn = null;
					}
					int deleteSuccessCount=0;
					String deleteIds = ImportActionUtils.buildDeleteIds(deleteIdList);
					if(null!=deleteIds && !"".equals(deleteIds))
					{
						boolean deleted = false;
						try
						{
							deleted = CarlineDAO.deleteCarlineDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getCarList(sessionBean);
						}
						else
						{
							/* the delete itself failed - every row it covered is a failure */
							if(null!=deleteSuccessLineNo && !"".equals(deleteSuccessLineNo))
							{
								if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
								{
									deleteErrorLineNo = deleteErrorLineNo+",";
								}
								deleteErrorLineNo = deleteErrorLineNo+deleteSuccessLineNo;
							}
							deleteSuccessLineNo = "";
						}
					}
					if(deleteSuccessCount>0)
					{
						/* successMessage is a single escaped c:out on the JSP - join with a SPACE */
						String existingMsg = sessionBean.getSuccessMessage();
						String deleteMsg = msgProps.addMessage("import.delete.success.count.message", String.valueOf(deleteSuccessCount));
						if(null!=existingMsg && !"".equals(existingMsg))
						{
							deleteMsg = existingMsg+" "+deleteMsg;
						}
						sessionBean.setSuccessMessage(deleteMsg);
					}
					if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
					{
						String[] deleteErrorRows = deleteErrorLineNo.split(",");
						if(null!=deleteErrorRows && deleteErrorRows.length>0)
						{
							failureCount = failureCount+deleteErrorRows.length;
							for(int a=0;a<deleteErrorRows.length;a++)
							{
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.car"), String.valueOf(deleteErrorRows[a])));
							}
						}
						deleteErrorRows = null;
					}
					deleteSuccessLineNo = null;
					deleteErrorLineNo = null;
					deleteIdList = null;
				}
				listToDelete = null;

				listToSave = null;
				
				if(errorCount>0 )
				{
					decideErrorDisplay(sessionBean, errorMessage, errorCount);
				}
				
				
				/*
				 * also check if duplicateCount or failureCount is more than 0
				 * then set infoMessage
				 */
				String mess="";

				if(duplicateRowsCount>0)
				{
					mess = msgProps.addMessage("error.excel.duplicate.rows.count", String.valueOf(duplicateRowsCount));
				}
				
				if(failureCount>0)
				{
					if(null!=mess && !"".equals(mess))
					{
						mess = mess+" ";
					}
					mess = mess+msgProps.addMessage("error.excel.failure.rows.count", String.valueOf(failureCount));
				}
				
				if(null!=mess && !"".equals(mess))
				{
					sessionBean.setInfoMessage(mess);
				}
				mess = null;
			
				errorMessage = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "executeExcelOperation()", e);
		}
	}

	private  void readExcelData(byte[] data, CarlineBean sessionBean, String extension)
	{
		sessionBean.setCarListToImport(new ArrayList<CarlineDetails>());
		try
		{
			if(null!=data)
			{
				InputStream is = new ByteArrayInputStream(data);
				
				XSSFWorkbook workbook  = null;
				XSSFSheet sheet = null;
				HSSFWorkbook xlsWorkBook = null;
				HSSFSheet xlsSheet = null;
				Iterator<Row> rowIterator = null;
				
				if(null!=extension && extension.equals("xlsx"))
				{
					//Create Workbook instance holding reference to .xlsx file
					workbook = new XSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					sheet = workbook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = sheet.iterator();
				}
				else if(null!=extension && extension.equals("xls"))
				{
					// Create workbook instance holding reference to .xls file
					xlsWorkBook = new HSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					xlsSheet=  xlsWorkBook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = xlsSheet.iterator();
				}
				
				long rowCount=0;
				/*
				 * IF MNAO FLAG TRUE - 
				 * CAR LINE CODE
				 * CAR LINE NAME ENG
				 * CAR LINE NAME REG
				 * WMI CODE
				 * YEAR START
				 * YEAR END
				 * IC DISPLAY CARLINE CODE
				 * 
				 * ELSE
				 * 
				 * CARLINE CODE
				 * CARLINE NAME ENG
				 * CARLINE NAME REG
				 * WMI CODE
				 * MODEL TYPE
				 * ESI CATEGORY FLAG
				 * IC DISPLAY CARLINE CODE
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						CarlineDetails carDetails = new CarlineDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setCarlineNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setCarlineNameReg(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						if(sessionBean.isMnaoContent()==true)
						{
							// NORTH AMERICA OPERATIONS
							dataCell = SSTUtils.readCellValue(row.getCell(4));
							if(null!=dataCell && !"".equals(dataCell))
							{
								carDetails.setYearStart(String.valueOf(dataCell).trim());
							}
							dataCell = null;

							dataCell = SSTUtils.readCellValue(row.getCell(5));
							if(null!=dataCell && !"".equals(dataCell))
							{
								carDetails.setYearEnd(String.valueOf(dataCell).trim());
							}
							dataCell = null;
						}
						else
						{
							// JAPAN OPERATIONS
							dataCell = SSTUtils.readCellValue(row.getCell(4));
							if(null!=dataCell && !"".equals(dataCell))
							{
								carDetails.setModelType(String.valueOf(dataCell).trim());
							}
							dataCell = null;

							dataCell = SSTUtils.readCellValue(row.getCell(5));
							if(null!=dataCell && !"".equals(dataCell))
							{
								carDetails.setEsiCategoryFlag(String.valueOf(dataCell).trim());
							}
							dataCell = null;
						}
						
						dataCell = SSTUtils.readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setIcDisplayCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(7));
						if(null!=dataCell && !"".equals(dataCell))
						{
							carDetails.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * add details to carList for import
						 */
						if(null==sessionBean.getCarListToImport() || sessionBean.getCarListToImport().size()<=0)
						{
							sessionBean.setCarListToImport(new ArrayList<CarlineDetails>());
						}

						sessionBean.getCarListToImport().add(carDetails);
						carDetails= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				is.close();
				is = null;
				xlsWorkBook=  null;
				xlsSheet = null;
				rowIterator=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "readExcelData()", e);
		}
	}

	private  boolean validateExcelRowData(CarlineBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getCarListToImport() && sessionBean.getCarListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getCarListToImport().size();i++)
			{
				CarlineDetails fieldDetails = (CarlineDetails) sessionBean.getCarListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;

				/*
				 * NEW ACTION / MARKER COLUMN - an unrecognised value is a HARD ERROR here,
				 * before any database work, so a mistyped marker can never be silently
				 * treated as create-or-update. Blank stays allowed.
				 */
				if(!ImportActionUtils.isValidAction(fieldDetails.getImportAction()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.excel.unknown.action", String.valueOf(rowNo)));
					errorCount++;
				}
				
				if(sessionBean.isMnaoContent()==true)
				{
					// NORTH AMERICA OPERATIONS
//					if(sessionBean.isYearStartMandatory()==true)
//					{
//						if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
//								null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
//								null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
//								null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()))
//						{
//							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//							{
//								errorMessage.append("<MSG_TOKEN>");
//							}
//							/*
//							 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
//							 */
//							String data = msgProps.getProperty("label.car")+","+String.valueOf(rowNo);
//							String[] id = data.split(",");
//							errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
//							data = null;
//							id = null;
//							// increment errorCount by 1
//							errorCount++;
//						}
//					}
//					else
//					{
//						if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
//								null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
//								null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()))
//						{
//							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//							{
//								errorMessage.append("<MSG_TOKEN>");
//							}
//							/*
//							 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
//							 */
//							String data = msgProps.getProperty("label.car")+","+String.valueOf(rowNo);
//							String[] id = data.split(",");
//							errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
//							data = null;
//							id = null;
//							// increment errorCount by 1
//							errorCount++;
//						}
//					}
					
					
					/*
					 *  YEAR RANGE IS ALWAYS MANDATORY
					 *  DATE 23 JUNE 2018
					 */
					if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
							null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
							null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
							null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()) ||
							null==fieldDetails.getYearStart() || "".equals(fieldDetails.getYearStart()) || 
							null==fieldDetails.getYearEnd() || "".equals(fieldDetails.getYearEnd()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						/*
						 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
						 */
						String data = msgProps.getProperty("label.car")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				
					
					
					if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
					{
						if(fieldDetails.getCarlineCode().length()>20)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carcode")+",20,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getCarlineNameReg() && !"".equals(fieldDetails.getCarlineNameReg()))
					{
						if(fieldDetails.getCarlineNameReg().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carname.reg")+",200,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()))
					{
						if(fieldDetails.getCarlineNameEng().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carname.eng")+",200,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}

					if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
					{
						if(fieldDetails.getWmiCode().length()>5)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.wmicode")+",5,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
					{
						if(fieldDetails.getIcDisplayCarlineCode().length()>20)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.icdisplaycode")+",20,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()))
					{
						int sYear = new Integer(fieldDetails.getYearStart()).intValue();
						if(sYear==0)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.yearstart")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.field.cannot.zero.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
						
						if(fieldDetails.getYearStart().trim().length()!=4)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.yearstart")+",4,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
					{
						int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
						if(eYear==0)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.yearend")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.field.cannot.zero.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
						
						if(fieldDetails.getYearEnd().trim().length()!=4)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.yearend")+",4,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getYearStart() && !"".equals(fieldDetails.getYearStart()) 
							&& null!=fieldDetails.getYearEnd() && !"".equals(fieldDetails.getYearEnd()))
					{
						int sYear = new Integer(fieldDetails.getYearStart()).intValue();
						int eYear = new Integer(fieldDetails.getYearEnd()).intValue();
						if(sYear > eYear)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.yearend")+","+msgProps.getProperty("label.yearstart")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.data.greater.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
				}
				else
				{
					// JAPAN OPERATIONS
					if(null==fieldDetails.getModelType() || "".equals(fieldDetails.getModelType()) ||
							null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
							null==fieldDetails.getEsiCategoryFlag() || "".equals(fieldDetails.getEsiCategoryFlag()) || 
							null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
							null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
							null==fieldDetails.getCarlineNameReg() || "".equals(fieldDetails.getCarlineNameReg()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						/*
						 * INCOMPLETE DATA FOR CAR AT ROW NO . rowNo
						 */
						String data = msgProps.getProperty("label.car")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					
					if(null!=fieldDetails.getModelType() && !"".equals(fieldDetails.getModelType()))
					{
						boolean matchFound = false;
						if(null!=sessionBean.getModelTypeList() && sessionBean.getModelTypeList().size()>0)
						{
							for(int a=0;a<sessionBean.getModelTypeList().size();a++)
							{
								SelectItemDetails si = (SelectItemDetails)sessionBean.getModelTypeList().get(a);
								if(si.getLabel().trim().toLowerCase().equals(fieldDetails.getModelType().trim().toLowerCase()))
								{
									// match Found
									fieldDetails.setModelType(si.getValue());
									matchFound  = true;
									break;
								}
							}
						}
						
						if(matchFound==false)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							/*
							 * INCOMPLETE DATA FOR CAR AT ROW NO . rowNo
							 */
							String data = msgProps.getProperty("label.modeltype")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getEsiCategoryFlag() && !"".equals(fieldDetails.getEsiCategoryFlag()))
					{
						boolean matchFound = false;
						if(null!=sessionBean.getEsiCategoryFlagList() && sessionBean.getEsiCategoryFlagList().size()>0)
						{
							for(int a=0;a<sessionBean.getEsiCategoryFlagList().size();a++)
							{
								SelectItemDetails si = (SelectItemDetails)sessionBean.getEsiCategoryFlagList().get(a);
								if(si.getValue().trim().toLowerCase().equals(fieldDetails.getEsiCategoryFlag().trim().toLowerCase()))
								{
									// match Found
									fieldDetails.setEsiCategoryFlag(si.getValue());
									matchFound  = true;
									break;
								}
							}
						}
						
						if(matchFound==false)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							/*
							 * INCOMPLETE DATA FOR CAR AT ROW NO . rowNo
							 */
							String data = msgProps.getProperty("label.esicategoryflag")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
					{
						if(fieldDetails.getCarlineCode().length()>20)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carcode")+",20,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getCarlineNameReg() && !"".equals(fieldDetails.getCarlineNameReg()))
					{
						if(fieldDetails.getCarlineNameReg().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carname.reg")+",200,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()))
					{
						if(fieldDetails.getCarlineNameEng().length()>200)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.carname.eng")+",200,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}

					if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
					{
						if(fieldDetails.getWmiCode().length()>5)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.wmicode")+",5,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
					
					if(null!=fieldDetails.getIcDisplayCarlineCode() && !"".equals(fieldDetails.getIcDisplayCarlineCode()))
					{
						if(fieldDetails.getIcDisplayCarlineCode().length()>20)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.icdisplaycode")+",20,"+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
				}
			}
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.car")));
			// increment errorCount by 1
			errorCount++;
		}
		
		
		/*
		 * HERE Check, if the Error Count is more than 10, then do not show error Messages on the screen.
		 * Instead, show a message, Multiple errors found while performing the transaction. Please Click <a>here</a> to view the details.
		 */
		if(errorCount>0)
		{
			decideErrorDisplay(sessionBean, errorMessage, errorCount);
			return false;
		}
		errorMessage = null;
		return true;
	}

	private  void decideErrorDisplay(CarlineBean sessionBean, StringBuilder errorMessage, int errorCount)
	{
		/*
		 * HERE Check, if the Error Count is more than 10, then do not show error Messages on the screen.
		 * Instead, show a message, Multiple errors found while performing the transaction. Please Click <a>here</a> to view the details.
		 */
		if(errorCount>10)
		{
			long currentTime = new Timestamp(new Date().getTime()).getTime();
			// WRITE ALL THE ERROR MESAGES TO A TEXT FILE  , NAME IT ON THE BASIS OF VIN_TIMESTAMP.TXT
			String eFPath = ApplicationProperties.getProperty("EXPORT_ERROR_PHYSICAL_PATH");
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_CAR_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
			File errorFile = new File(eFPath+eFName);
			try {
				String data = errorMessage.toString();
				data = data.replace("<MSG_TOKEN>", "\n");
				
				FileOutputStream fos = new FileOutputStream(errorFile);
				fos.write(data.getBytes());
				fos.flush();
				fos.close();
				fos = null;
				data = null;
			} catch (FileNotFoundException e) {
				Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
			}
			errorFile=null;
			String webPath = ApplicationProperties.getProperty("EXPORT_ERROR_WB_PATH")+eFName;
			
			// label.error.screen.help.text.start
			// label.here
			// label.view.the.details
			String message=msgProps.addMessage("error.import.invalid.custom.message", String.valueOf(errorCount));
			message = message + " "+msgProps.getProperty("label.error.screen.help.text.start");
			message = message+ " <a href=\""+webPath+"\" target=\"_blank\">" +msgProps.getProperty("label.here")+"</a>";
			message = message +" " +msgProps.getProperty("label.view.the.details");
			
			// set message in errorMessage
			sessionBean.setErrorMessage(message);
			message = null;
			eFPath = null;
			eFName=  null;
			webPath = null;
		}
		else
		{
			String messages=errorMessage.toString();
			if(null!=messages && !"".equals(messages))
			{
				sessionBean.setErrorMessage(messages);
				messages=  null;
				errorMessage= null;
			}
		}
	}

	private  void exportCarDetails(HttpServletRequest request, CarlineBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<CarlineDetails> exportDataList = new ArrayList<CarlineDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCarList() && !"".equals(sessionBean.getCarList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCarList().size();a++)
							{
								CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarList().get(a);
								if(rowId.equals(String.valueOf(carDetails.getCarlineCodeId())))
								{
									// add to export List
									exportDataList.add(carDetails);
									break;
								}
							}
							rowId=  null;
						}
					}
					rows = null;
				}
				
				if(null!=exportDataList && exportDataList.size()>0)
				{
					/*
					 * CALL FUNCTION TO GENERATE EXCEL FOR THE SELECTED ROWS
					 */
					writeCARExcel(exportDataList, sessionBean);
					/*
					 * PREARE VIN REORT PATH AND MAKE IT DOWNLOAD
					 */
					if(null!=reportName && !"".equals(reportName))
					{
						// set in SESSION BEAN
						String path = ApplicationProperties.getProperty("EXPORT_DATA_WB_PATH");
						if(!path.endsWith("/"))
						{
							path = path+"/";
						}
						path = path+reportName;
						sessionBean.setReportViewPath(path);
						path = null;
					}
					reportName = null;
				}
				else
				{
					logger.info("exportCarDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportCarDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "exportCarDetails()", e);
		}
	}

	private void writeCARExcel(ArrayList<CarlineDetails> carList, CarlineBean sessionBean)
	{
		try
		{
			String path = ApplicationProperties.getProperty("EXPORT_DATA_PHYSICAL_PATH");
			if(null!=path && !"".equals(path))
			{
				if(!path.endsWith("/") && !path.endsWith("\\"))
				{
					path = path+"/";
				}
				// add VIN DATA NAME
				String name = "";
				/*
				 * add SELECTED COUNTRY LOCALE CODE, ADD MANUAL LANGUAGE CODE, ADD MODEL CODE
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - US_EN-US_ND_VIN_DDMMYYYY_HHMMSS.XSLX
				 */
				
				String countryLocaleCode = "";
				if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
				{
					countryLocaleCode = CountryLocaleDAO.getCountryLocaleCode(sessionBean.getCountryLocaleId());
					if(null!=countryLocaleCode && !"".equals(countryLocaleCode))
					{
						name= countryLocaleCode.trim().toUpperCase();
					}
				}
				countryLocaleCode = null;
				String manualLanguageCode="";
				if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
				{
					manualLanguageCode = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId());
					if(null!=manualLanguageCode && !"".equals(manualLanguageCode))
					{
						if(null!=name && !"".equals(name))
						{
							name = name.trim()+"_";
						}
						name = name.trim()+manualLanguageCode.trim().toUpperCase();
					}
				}
				manualLanguageCode = null;
				
				if(null!=name && !"".equals(name))
				{
					name = name.trim()+"_";
				}
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_CAR_NAME");
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				String displayValue = sdf.format(new Date());
				
				// ADD ZIP NAME.
				String zipName = name.trim()+"_"+displayValue+ApplicationProperties.getProperty("EXPORT_DATA_EXTENSION_ZIP");
				// ADD ZIP PATH
				String zipPath = path+zipName;
				name = name.trim()+"_"+displayValue+ApplicationProperties.getProperty("EXPORT_DATA_EXTENSION");
				path= path+name;
				
				// SET REPORT NAME TO ZIP FILE NAME INSTEAD OF EXCEL FILE
				reportName = zipName;
				name = null;
				zipName = null;
				
				File excelFile = new File(path);
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);
				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("EXPORTED DATA");
				Row headerRow = mySheet.createRow(0);
				
				Cell carlineCodeCell = headerRow.createCell(0);
				carlineCodeCell.setCellValue("CARLINE CODE");
				Cell carNameEngCell = headerRow.createCell(1);
				carNameEngCell.setCellValue("CAR NAME ENG LANG");
				Cell carNameRegCell = headerRow.createCell(2);
				carNameRegCell.setCellValue("CAR NAME REGIONAL LANG");
				Cell wmiCell = headerRow.createCell(3);
				wmiCell.setCellValue("WMI CODE");
				
				Cell mysteryCell = headerRow.createCell(4);
				Cell mysteryCell1 = headerRow.createCell(5);
				if(sessionBean.isMnaoContent()==true)
				{
					mysteryCell.setCellValue("YEAR START");
					mysteryCell1.setCellValue("YEAR END");
				}
				else
				{
					mysteryCell.setCellValue("MODEL TYPE");
					mysteryCell1.setCellValue("ESI CATEGORY FLAG");
				}
				Cell displayCarlineCodeCell = headerRow.createCell(6);
				displayCarlineCodeCell.setCellValue("IC DISPLAY CARLINE CODE");
				
				int rowCount=0;
				if(null!=carList && carList.size()>0)
				{
					for(int i=0;i<carList.size();i++)
					{
						CarlineDetails carDetails = (CarlineDetails)carList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						
						if(null!=carDetails.getCarlineCode() && !"".equals(carDetails.getCarlineCode()))
						{
							cell0.setCellValue(carDetails.getCarlineCode());
						}
						if(null!=carDetails.getCarlineNameEng() && !"".equals(carDetails.getCarlineNameEng()))
						{
							cell1.setCellValue(carDetails.getCarlineNameEng());
						}
						if(null!=carDetails.getCarlineNameReg() && !"".equals(carDetails.getCarlineNameReg()))
						{
							cell2.setCellValue(carDetails.getCarlineNameReg());
						}
						if(null!=carDetails.getWmiCode() && !"".equals(carDetails.getWmiCode()))
						{
							cell3.setCellValue(carDetails.getWmiCode());
						}
						if(sessionBean.isMnaoContent()==true)
						{
							if(null!=carDetails.getYearStart() && !"".equals(carDetails.getYearStart()))
							{
								cell4.setCellValue(carDetails.getYearStart());
							}
							if(null!=carDetails.getYearEnd() && !"".equals(carDetails.getYearEnd()))
							{
								cell5.setCellValue(carDetails.getYearEnd());
							}
						}
						else
						{
							if(null!=carDetails.getModelType() && !"".equals(carDetails.getModelType()))
							{
								cell4.setCellValue(carDetails.getModelType());
							}
							if(null!=carDetails.getEsiCategoryFlagLabel() && !"".equals(carDetails.getEsiCategoryFlagLabel()))
							{
								cell5.setCellValue(carDetails.getEsiCategoryFlag());
							}
						}
						
						if(null!=carDetails.getIcDisplayCarlineCode() && !"".equals(carDetails.getIcDisplayCarlineCode()))
						{
							cell6.setCellValue(carDetails.getIcDisplayCarlineCode());
						}
						
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						row = null;
						carDetails = null;
					}
					
					headerRow =  null;
					carlineCodeCell = null;
					carNameEngCell = null;
					carNameRegCell = null;
					wmiCell = null;
					mysteryCell =null;
					mysteryCell1 = null;
					displayCarlineCodeCell = null;
					/*
					 * Before Writing check for size if equals to or more than 10 MB
					 * then generate a file with a extension to it.
					 */

					FileOutputStream os = new FileOutputStream(excelFile);
					myWorkBook.write(os);
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set sdf to null
					sdf = null;
					/*
					 * CONVERT THIS FILE TO ZIP FILE.
					 */
					Utilities.createReportsZip(zipPath, excelFile);
					
					// set excelFile to null
					excelFile = null;
					// set zipPath to null
					zipPath=  null;
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "writeCARExcel()", e);
		}
	}

	private UserAccessBean getUserSessionBean(HttpServletRequest request) 
	{
		UserAccessBean sessionBean = null;
		if (null != request.getSession().getAttribute("userAccessBean") && !"".equals(request.getSession().getAttribute("userAccessBean"))) 
		{
			sessionBean = (UserAccessBean) request.getSession().getAttribute("userAccessBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new UserAccessBean();
			request.getSession().setAttribute("userAccessBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void performAccessCheck(CarlineBean sessionBean, HttpServletRequest request)
	{
		/*
		 * Check User Has access to this Functionality or Not.
		 * Identify if User is Super Admin - then enable Read / Write Access on this page
		 * else - check if user has access to this functionality
		 * 		check for the accessType
		 * 			if READ ACCESS - SHOW READ CONTROLS
		 * 			if WRITE ACCESS - SHOW WRITE CONTROLS
		 * 			if READ & WRITE ACCESS - SHOW READ & WRITE CONTROLS
		 */
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setShowReadControls(false);
			sessionBean.setShowWriteControls(false);
			if(userSessionBean.isSuperAdminUser()==true)
			{
				sessionBean.setShowReadControls(true);
				sessionBean.setShowWriteControls(true);
			}
			else if(userSessionBean.isSuperAdminUser()==false)
			{
				if(null!=userSessionBean.getUserAllModulesList() && userSessionBean.getUserAllModulesList().size()>0)
				{
					for(int a=0;a<userSessionBean.getUserAllModulesList().size();a++)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getUserAllModulesList().get(a);
						if(modDetails.getModuleRefkey().trim().toLowerCase().equals(moduleRefKey.trim().toLowerCase()))
						{
							// USER HAS ACCESS TO THIS SCREEN. CHECK FOR ACCESS TYPE
							if(modDetails.getAccessType()==AccessManagementInterface.ONLY_READ_ACCESS)
							{
								sessionBean.setShowReadControls(true);
							}
							else if(modDetails.getAccessType()==AccessManagementInterface.ONLY_WRITE_ACCESS)
							{
								sessionBean.setShowWriteControls(true);
							}
							else if(modDetails.getAccessType()==AccessManagementInterface.READ_AND_WRITE_ACCESS)
							{
								sessionBean.setShowReadControls(true);
								sessionBean.setShowWriteControls(true);
							}
							else
							{
								sessionBean.setShowReadControls(false);
								sessionBean.setShowWriteControls(false);
							}
							break;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "performAccessCheck()", e);
		}
	}
	
	private String identifyLanguageId(CarlineBean sessionBean)
	{
		String languageId=null;
		try
		{
			// set Combo selectedValue
			languageId = sessionBean.getManualLanguageId();
			/*
			 * IDENTIFY HERE LANGUAGE ID
			 * 	IF EN-CA IS SELECTED - SHOW DATA FOR EN-US (DATE 27 JUNE 2018)
			 * ELSE SELECTED LOCALE
			 */
//			if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
//			{
//				String encaLocale=ApplicationProperties.getProperty("en_ca");
//				String enusLocale =ApplicationProperties.getProperty("en_us");
//				encaLocale = encaLocale.replace("_", "-");
//				enusLocale = enusLocale.replace("_", "-");
//				boolean useENUS = false;
//				for(int r=0;r<sessionBean.getLanguageList().size();r++)
//				{
//					ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
//					if(sessionBean.getManualLanguageId().trim().equals(String.valueOf(mlDetails.getManualLanguageId())))
//					{
//						if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(encaLocale.trim().toLowerCase()))
//						{
//							// selectedLocale is en-CA - use en-US
//							useENUS = true;
//						}
//						break;
//					}
//					mlDetails= null;
//				}
//				
//				if(useENUS==true)
//				{
//					// identify Language Id for enUS
//					try
//					{
//						languageId=ManualLanguageDAO.getManualLanguageId(enusLocale);
//					}
//					catch(Exception e)
//					{
//						Utilities.printStackTraceToLogs(Carline.class.getName(), "identifyLanguageId()", e);
//					}
//				}
//				encaLocale = null;
//				enusLocale = null;
//			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Carline.class.getName(), "identifyLanguageId()", e);
		}
		return languageId;
	}

}
