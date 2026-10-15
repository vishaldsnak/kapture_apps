package com.mazda.gms3.mdm.dataexttool.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CarlineDAO;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.bean.DataExportToolSessionBean;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportToolDAO;
import com.mazda.gms3.mdm.dataexttool.impl.DataExportScheduleImpl;
import com.mazda.gms3.mdm.dataexttool.servlet.DataExportToolServlet;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolItemDetails;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolScheduleDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class DataExportToolServlet
 */
public class DataExportToolServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger logger = LogManager.getLogger(DataExportToolServlet.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL;


	/**
	 * Default constructor. 
	 */
	public DataExportToolServlet() {
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
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

			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Initialize bean
			 */
			DataExportToolSessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setCountryLocaleList(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setLanguageList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setActionClicked(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setModelId(null);
			sessionBean.setModelsList(null);
			sessionBean.setItemsList(null);
			sessionBean.setSelectedItemsList(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setScheduleName(null);
			sessionBean.setFromDate(null);
			sessionBean.setToDate(null);
			sessionBean.setShowDatesBlock(false);
			sessionBean.setDataExtractionTypeList(null);
			sessionBean.setSelectedDataExtractionType(null);
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
			
			/*
			 * call function to load Data Extraction Type List
			 */
			getDataExtractionTypeList(sessionBean);
			if(null!=sessionBean.getDataExtractionTypeList() && sessionBean.getDataExtractionTypeList().size()>0)
			{
				/*
				 * set Default Value selected for Data Extraction Type as Full Data
				 */
				sessionBean.setSelectedDataExtractionType(ApplicationProperties.getProperty("dataexttool.operation.type.fullextract.value"));
			}
		}

		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/dataexttool/welcome.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		String jspPath = "/jsps/dataexttool/welcome.jsp";
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

			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Initialize bean
			 */
			DataExportToolSessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setInfoMessage("");
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setFromDate(null);
			sessionBean.setToDate(null);
			sessionBean.setShowDatesBlock(false);

			/*
			 * call function to read parametrs from request
			 */
			readParametersFromRequest(sessionBean, request);

			/*
			 * On the Basis of Selected Data Type 
			 * Show Dates Block
			 */
			if(null!=sessionBean.getSelectedDataExtractionType() && !"".equals(sessionBean.getSelectedDataExtractionType()))
			{
				if(sessionBean.getSelectedDataExtractionType().equals(ApplicationProperties.getProperty("dataexttool.operation.type.fullextract.value")))
				{
					sessionBean.setShowDatesBlock(false);
					sessionBean.setFromDate(null);
					sessionBean.setToDate(null);
				}
				else if(sessionBean.getSelectedDataExtractionType().equals(ApplicationProperties.getProperty("dataexttool.operation.type.specificextract.value")))
				{
					sessionBean.setShowDatesBlock(true);
				}
			}
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_SELECTION"))
				{
					sessionBean.setManualLanguageId(null);
					sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
					sessionBean.setModelId(null);
					sessionBean.setModelsList(null);
					/*
					 * call function to get language list
					 */
					getLanguageList(sessionBean, request);
					/*
					 * call function to get models list
					 */
					getModelsList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setModelId(null);
					sessionBean.setModelsList(null);
					/*
					 * call function to get model list
					 */
					getModelsList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SUBMIT"))
				{
					if(validate(sessionBean))
					{
						// get matrix data
						getMatrixData(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					sessionBean.setCountryLocaleList(null);
					sessionBean.setCountryLocaleId(null);
					sessionBean.setLanguageList(null);
					sessionBean.setManualLanguageId(null);
					sessionBean.setActionClicked(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setInfoMessage(null);
					sessionBean.setModelId(null);
					sessionBean.setModelsList(null);
					sessionBean.setItemsList(null);
					sessionBean.setSelectedItemsList(null);
					sessionBean.setSelectedRows(null);

					/*
					 * call function to load all the Country Locale Data
					 */
					getCountryLocaleList(sessionBean, request);
				}
				else if(sessionBean.getActionClicked().equals("SCHEDULE"))
				{
					schedule(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher(jspPath);
				rs.forward(request, response);
			}
		}
	}

	private DataExportToolSessionBean getSessionBean(HttpServletRequest request) 
	{
		DataExportToolSessionBean sessionBean = null;
		if (null != request.getSession().getAttribute("dataExportToolBean") && !"".equals(request.getSession().getAttribute("dataExportToolBean"))) 
		{
			sessionBean = (DataExportToolSessionBean) request.getSession().getAttribute("dataExportToolBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new DataExportToolSessionBean();
			request.getSession().setAttribute("dataExportToolBean", sessionBean);
		}
		return sessionBean;
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

	private void performAccessCheck(DataExportToolSessionBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "performAccessCheck()", e);
		}
	}

	private void readParametersFromRequest(DataExportToolSessionBean sessionBean, HttpServletRequest request) 
	{
		try
		{
			sessionBean.setActionClicked(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setModelId(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setSelectedDataExtractionType(null);
			sessionBean.setFromDate(null);
			sessionBean.setToDate(null);

			if (null!=request.getParameter("DATAEXPTOOL_DataTabel_displayPageNo") && !"".equals(request.getParameter("DATAEXPTOOL_DataTabel_displayPageNo"))) 
			{
				// set the value in sessionBean.setDisplayPageNo
				sessionBean.setDisplayPageNo((String)request.getParameter("DATAEXPTOOL_DataTabel_displayPageNo"));
			}

			if(null!=request.getParameter("DATAEXPTOOL_DataTabel_displayPageLen") && !"".equals(request.getParameter("DATAEXPTOOL_DataTabel_displayPageLen")))
			{
				// set the value in sessionBean.setDisplayPageLength
				sessionBean.setDisplayPageLength((String)request.getParameter("DATAEXPTOOL_DataTabel_displayPageLen"));
			}

			if(null!=request.getParameter("DATAEXPTOOL_DataExtractionTypeId") && !"".equals(request.getParameter("DATAEXPTOOL_DataExtractionTypeId")))
			{
				// set the value in sessionBean.setSelectedDataExtractionType
				sessionBean.setSelectedDataExtractionType((String)request.getParameter("DATAEXPTOOL_DataExtractionTypeId"));
			}
			
			if(null!=request.getParameter("DATAEXPTOOL_FromDate") && !"".equals(request.getParameter("DATAEXPTOOL_FromDate")))
			{
				// set the value in sessionBean.setFromDate
				sessionBean.setFromDate((String)request.getParameter("DATAEXPTOOL_FromDate"));
			}
			
			if(null!=request.getParameter("DATAEXPTOOL_ToDate") && !"".equals(request.getParameter("DATAEXPTOOL_ToDate")))
			{
				// set the value in sessionBean.setToDate
				sessionBean.setToDate((String)request.getParameter("DATAEXPTOOL_ToDate"));
			}
			
			if(null!=request.getParameterValues("DATAEXPTOOL_CountryId") && request.getParameterValues("DATAEXPTOOL_CountryId").length>0)
			{
				String[] sel = request.getParameterValues("DATAEXPTOOL_CountryId");
				if(null!=sel && sel.length>0)
				{
					sessionBean.setCountryLocaleId(sel);
				}
				sel = null;
			}
			if(null!=request.getParameterValues("DATAEXPTOOL_LanguageId") && request.getParameterValues("DATAEXPTOOL_LanguageId").length>0)
			{
				String[] sel = request.getParameterValues("DATAEXPTOOL_LanguageId");
				if(null!=sel && sel.length>0)
				{
					sessionBean.setManualLanguageId(sel);
				}
				sel = null;
			}
			if(null!=request.getParameterValues("DATAEXPTOOL_ModelId") && request.getParameterValues("DATAEXPTOOL_ModelId").length>0)
			{
				String[] sel = request.getParameterValues("DATAEXPTOOL_ModelId");
				if(null!=sel && sel.length>0)
				{
					sessionBean.setModelId(sel);
				}
				sel = null;
			}
			if(null!=request.getParameter("DATAEXPTOOL_ActionClicked") && !"".equals(request.getParameter("DATAEXPTOOL_ActionClicked")))
			{
				sessionBean.setActionClicked((String)request.getParameter("DATAEXPTOOL_ActionClicked"));
			}
			if(null!=request.getParameter("DATAEXPTOOL_SelectedRows") && !"".equals(request.getParameter("DATAEXPTOOL_SelectedRows")))
			{
				sessionBean.setSelectedRows((String)request.getParameter("DATAEXPTOOL_SelectedRows"));
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "readParametersFromRequest()", e);
		}
	}


	private void getDataExtractionTypeList(DataExportToolSessionBean sessionBean)
	{
		sessionBean.setDataExtractionTypeList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("dataexttool.operation.type.fullextract.label"));
		si.setValue(ApplicationProperties.getProperty("dataexttool.operation.type.fullextract.value"));
		sessionBean.getDataExtractionTypeList().add(si);
		si = null;

		si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("dataexttool.operation.type.specificextract.label"));
		si.setValue(ApplicationProperties.getProperty("dataexttool.operation.type.specificextract.value"));
		sessionBean.getDataExtractionTypeList().add(si);
		si = null;
	}

	

	private void getCountryLocaleList(DataExportToolSessionBean sessionBean, HttpServletRequest request)
	{
		try
		{

			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			if(null!=list && list.size()>0)
			{
				if(userSessionBean.isSuperAdminUser()==true)
				{
					/*
					 * ALSO CHECK BEFORE SETTING FINAL COUNTRY LIST
					 * ONLY MME COUNTRIES ALLOWED HERE
					 */
					ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
					String countriesToBeAccepted=ApplicationProperties.getProperty("mnao.countries.codes").trim();
					if(!countriesToBeAccepted.endsWith(","))
					{
						countriesToBeAccepted+=",";
					}

					String[] tokens=countriesToBeAccepted.split(",");
					for(CountryLocaleDetails clD : list)
					{
						boolean addToList = false;
						for(int r=0;r<tokens.length;r++)
						{
							if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(tokens[r].trim().toLowerCase()))
							{
								// country to be added - Since MNAO Country
								addToList=true;
								break;
							}
						}
						if(addToList==true)
						{
							actList.add(clD);
						}
					}


					if(null!=actList && actList.size()>0)
					{
						CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
						Collections.sort(actList,countryLocaleComparator);
						sessionBean.setCountryLocaleList(actList);
						countryLocaleComparator = null;
					}
					actList=  null;
					tokens = null;
					countriesToBeAccepted = null;
				}
				else if(userSessionBean.isSuperAdminUser()==false)
				{
					ArrayList<CountryLocaleDetails> finalCountryList = new ArrayList<CountryLocaleDetails>();
					// NOW CHECK FOR USER LOCALES
					if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
					{
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
					}

					if(null!=finalCountryList && finalCountryList.size()>0)
					{
						/*
						 * ALSO CHECK BEFORE SETTING FINAL COUNTRY LIST
						 * ONLY MME COUNTRIES ALLOWED HERE
						 */
						ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
						String countriesToBeAccepted=ApplicationProperties.getProperty("mnao.countries.codes").trim();
						if(!countriesToBeAccepted.endsWith(","))
						{
							countriesToBeAccepted+=",";
						}

						String[] tokens=countriesToBeAccepted.split(",");
						for(CountryLocaleDetails clD : finalCountryList)
						{
							boolean addToList = false;
							for(int r=0;r<tokens.length;r++)
							{
								if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(tokens[r].trim().toLowerCase()))
								{
									// country to be added - MNAO Country
									addToList=true;
									break;
								}
							}
							if(addToList==true)
							{
								actList.add(clD);
							}
						}

						if(null!=actList && actList.size()>0)
						{
							CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
							Collections.sort(actList,countryLocaleComparator);
							sessionBean.setCountryLocaleList(actList);
							countryLocaleComparator = null;
						}
						actList=  null;
						tokens=null;
						countriesToBeAccepted = null;
					}
					finalCountryList=  null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private void getLanguageList(DataExportToolSessionBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
			if(null!=sessionBean.getCountryLocaleId() && sessionBean.getCountryLocaleId().length>0)
			{
				ArrayList<ManualLanguageDetails> tList = new ArrayList<ManualLanguageDetails>();
				for(int a=0;a<sessionBean.getCountryLocaleId().length;a++)
				{
					// remove value for All Option (100000)
					if(null!=sessionBean.getCountryLocaleId()[a] && !"100000".equals(sessionBean.getCountryLocaleId()[a]))
					{
						tList=  ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId()[a]);
						if(null!=tList && tList.size()>0)
						{
							list.addAll(tList);
						}
						tList=  null;
					}
				}
			}
			if(null!=list && list.size()>0)
			{
				UserAccessBean userSessionBean = getUserSessionBean(request);
				if(userSessionBean.isSuperAdminUser()==true)
				{
					/*
					 * ALSO CHECK BEFORE SETTING FINAL LANGUAGE LIST
					 * ONLY MNAO LANGUAGES ALLOWED HERE
					 */
					ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
					String languagesToBeAdded=ApplicationProperties.getProperty("mnao.countries.locales.codes").trim();
					String[] tokens = languagesToBeAdded.split(",");
					for(ManualLanguageDetails mlD : list)
					{
						boolean addToList = false;
						for(int r=0;r<tokens.length;r++)
						{
							String token = tokens[r];
							token = token.replace("_", "-");
							if(mlD.getManualLanguageName().trim().toLowerCase().equals(token.trim().toLowerCase()))
							{
								// add this Locale
								addToList = true;
								break;
							}
							token=null;
						}
						if(addToList==true)
						{
							actList.add(mlD);
						}
					}

					if(null!=actList && actList.size()>0)
					{
						ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
						Collections.sort(actList,manualLanguageComparator);
						/*
						 * BEFORE ADDING SORTED LIST - ADD ALL OPTION
						 */
						ManualLanguageDetails mlDetails = new ManualLanguageDetails();
						mlDetails.setManualLanguageId(new Long(1000000).longValue());
						mlDetails.setManualLanguageCode(msgProps.getProperty("label.all"));
						sessionBean.getLanguageList().add(mlDetails);
						sessionBean.getLanguageList().addAll(actList);
						manualLanguageComparator = null;
						mlDetails=null;
					}
					actList=  null;
					languagesToBeAdded = null;
					tokens = null;

				}
				else if(userSessionBean.isSuperAdminUser()==false)
				{
					/*
					 * ITERATE LIST AND CHECK FOR THE LOCALE CODE WHETHER EXISTS IN USER'S DEFAULT LOCALE AND CONTENT LOCALE OR NOT
					 * IF EXISTS, THEN ONLY PROCEED. ELSE SKIP
					 */
					ArrayList<ManualLanguageDetails> finalLocaleList = new ArrayList<ManualLanguageDetails>();
					String languagesToBeAdded=ApplicationProperties.getProperty("mnao.countries.locales.codes").trim();
					languagesToBeAdded = languagesToBeAdded.replace("_", "-");
					// CHECK WITH USER LOCALES
					if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
					{
						for(String contentLocale : userSessionBean.getUserLocalesList())
						{
							if(null!=contentLocale && !"".equals(contentLocale))
							{
								for(ManualLanguageDetails mldDetails : list)
								{
									boolean addToList = false;
									if(contentLocale.trim().toLowerCase().equals(mldDetails.getManualLanguageName().trim().toLowerCase()) && 
											languagesToBeAdded.indexOf(contentLocale)!=-1)
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
					}
					languagesToBeAdded = null;
					if(null!=finalLocaleList && finalLocaleList.size()>0)
					{
						/*
						 * ALSO CHECK BEFORE SETTING FINAL LANGUAGE LIST
						 * ONLY MNAO LANGUAGES ALLOWED HERE
						 */
						ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
						actList= finalLocaleList;
						if(null!=actList && actList.size()>0)
						{
							ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
							Collections.sort(actList,manualLanguageComparator);
							
							/*
							 * BEFORE ADDING SORTED LIST - ADD ALL OPTION
							 */
							ManualLanguageDetails mlDetails = new ManualLanguageDetails();
							mlDetails.setManualLanguageId(new Long(1000000).longValue());
							mlDetails.setManualLanguageCode(msgProps.getProperty("label.all"));
							sessionBean.getLanguageList().add(mlDetails);
							sessionBean.getLanguageList().addAll(actList);
							manualLanguageComparator = null;
							mlDetails=null;
						}
						actList=  null;
						languagesToBeAdded = null;
					}
					finalLocaleList=  null;
				}
			}
			list = null;

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "getLanguageList()", e);
		}
	}

	private void getModelsList(DataExportToolSessionBean sessionBean)
	{
		sessionBean.setModelsList(new ArrayList<CarlineDetails>());
		try
		{
			if(null!=sessionBean.getManualLanguageId() && sessionBean.getManualLanguageId().length>0 && 
					null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
			{
				String key="";
				ManualLanguageDetails mld = null;
				for(int a=0;a<sessionBean.getManualLanguageId().length;a++)
				{
					mld = null;
					for(int b=0;b<sessionBean.getLanguageList().size();b++)
					{
						mld = (ManualLanguageDetails)sessionBean.getLanguageList().get(b);
						if(sessionBean.getManualLanguageId()[a].trim().toLowerCase().equals(String.valueOf(mld.getManualLanguageId()).trim().toLowerCase()))
						{
							// add Lang Code for search key
							key+="'"+mld.getManualLanguageName()+"',";
							break;
						}
						mld = null;
					}
				}
				
				if(null!=key && !"".equals(key))
				{
					if(key.endsWith(","))
					{
						key = key.substring(0, key.length()-1);
					}
					
					if(null!=key && !"".equals(key))
					{
						/*
						 * proceed for searching models for mnao locales
						 */
						key=  key.replace("_", "-");
						ArrayList<CarlineDetails> list = CarlineDAO.getCarlineDetailsListForComboFORMNAOExportTool(key);
						if(null!=list && list.size()>0)
						{
							sessionBean.setModelsList(list);
						}
						list = null;
					}
				}
				key  =null;
				mld = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "getModelsList()", e);
		}
	}

	private boolean validate(DataExportToolSessionBean sessionBean)
	{
		boolean data = true;
		try
		{
			if(sessionBean.isShowDatesBlock()==true)
			{
				// from Date and To Dates must not be null
				if(null==sessionBean.getFromDate() || "".equals(sessionBean.getFromDate()) || null==sessionBean.getToDate() || "".equals(sessionBean.getToDate()) || 
						null==sessionBean.getCountryLocaleId() || sessionBean.getCountryLocaleId().length<=0 || null==sessionBean.getManualLanguageId() || 
						sessionBean.getManualLanguageId().length<=0 || null==sessionBean.getModelId() || sessionBean.getModelId().length<=0)
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
					data =  false;
				}
				
				if(null!=sessionBean.getFromDate() && !"".equals(sessionBean.getFromDate()) && null!=sessionBean.getToDate() && !"".equals(sessionBean.getToDate()))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					Date fromDate = sdf.parse(sessionBean.getFromDate());
					Date toDate = sdf.parse(sessionBean.getToDate());
					
					if(fromDate.getTime()>= toDate.getTime())
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.data.greater", msgProps.getProperty("label.pubdoc.todate"), msgProps.getProperty("label.pubdoc.fromdate")));
						data  = false;
					}
					fromDate = null;
					toDate  = null;
					sdf = null;
				}
			}
			else
			{
				if(null==sessionBean.getCountryLocaleId() || sessionBean.getCountryLocaleId().length<=0 || null==sessionBean.getManualLanguageId() || 
						sessionBean.getManualLanguageId().length<=0 || null==sessionBean.getModelId() || sessionBean.getModelId().length<=0)
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
					data = false;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "validate()", e);
			data = false;
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
		return data;
	}

	private void getMatrixData(DataExportToolSessionBean sessionBean)
	{
		sessionBean.setItemsList(new ArrayList<DataExportToolItemDetails>());
		sessionBean.setSelectedItemsList(null);
		try
		{
			List<String> locale = null;
			List<CarlineDetails> models = null;
			if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
			{
				ManualLanguageDetails mlDetails = null;
				for(int a=0;a<sessionBean.getLanguageList().size();a++)
				{
					mlDetails= (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
					if(null!=sessionBean.getManualLanguageId() && sessionBean.getManualLanguageId().length>0)
					{
						for(int b=0;b<sessionBean.getManualLanguageId().length;b++)
						{
							// remove value for All Option (1000000)
							if(null!=sessionBean.getManualLanguageId()[b] && !"1000000".equals(sessionBean.getManualLanguageId()[b]))
							{
								if(sessionBean.getManualLanguageId()[b].equals(String.valueOf(mlDetails.getManualLanguageId())))
								{
									if(null==locale || locale.size()<=0)
									{
										locale = new ArrayList<String>();
									}
									// name variable contains code value
									locale.add(mlDetails.getManualLanguageName());
								}
							}
						}
					}
					mlDetails = null;
				}
				mlDetails=  null;
			}

			if(null!=sessionBean.getModelsList() && sessionBean.getModelsList().size()>0)
			{
				CarlineDetails clDetails = null;
				for(int a=0;a<sessionBean.getModelsList().size();a++)
				{
					clDetails = (CarlineDetails)sessionBean.getModelsList().get(a);
					if(null!=sessionBean.getModelId() && sessionBean.getModelId().length>0)
					{
						for(int b=0;b<sessionBean.getModelId().length;b++)
						{
							if(sessionBean.getModelId()[b].equals(clDetails.getCarlineIdForCombo()))
							{
								if(null==models || models.size()<=0)
								{
									models = new ArrayList<CarlineDetails>();
								}
								models.add(clDetails);
							}
						}
					}
					clDetails = null;
				}
				clDetails=  null;
			}

			if(null!=locale && locale.size()>0 && null!=models && models.size()>0)
			{
				if(sessionBean.isShowDatesBlock()==true)
				{
					logger.info("getMatrixData :: > Fetching for "+locale.size()+" Locales >> "+ models.size() +" Models in Between Dates :: >"+ sessionBean.getFromDate()+" - "+sessionBean.getToDate());
				}
				else 
				{
					logger.info("getMatrixData :: > Fetching for "+locale.size()+" Locales >> "+ models.size() +" Models");
				}
				ArrayList<DataExportToolItemDetails> list = DataExportToolDAO.getDocumentsMatrixList(locale, models, sessionBean.getFromDate(), sessionBean.getToDate());
				if(null!=list && list.size()>0)
				{
					sessionBean.setItemsList(list);
				}
				list = null;
			}
			else
			{
				logger.info("getMatrixData :: Locale / Model / Carline Code are null.");
			}
			locale=  null;
			models = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "getMatrixData()", e);
		}
	}
	
	private boolean validateSchedule(DataExportToolSessionBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
			{
				if(null==sessionBean.getSelectedRows() || "".equals(sessionBean.getSelectedRows()))
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.checkbox"));
					return false;
				}
				
				if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
				{
					sessionBean.setSelectedItemsList(new ArrayList<DataExportToolItemDetails>());
					if(sessionBean.getSelectedRows().endsWith(","))
					{
						sessionBean.setSelectedRows(sessionBean.getSelectedRows().substring(0, sessionBean.getSelectedRows().length()-1));
					}
					String[] tok = sessionBean.getSelectedRows().split(",");
					
					DataExportToolItemDetails details = null;
					if(null!=tok && tok.length>0)
					{
						for(int b=0;b<tok.length;b++)
						{
							for(int a=0;a<sessionBean.getItemsList().size();a++)
							{
								details= (DataExportToolItemDetails)sessionBean.getItemsList().get(a);
								if(String.valueOf(details.getSrNo()).equals(tok[b]))
								{
									sessionBean.getSelectedItemsList().add(details);
									break;
								}
								details = null;
							}
						}
					}
					details = null;
					tok = null;
				}
				
				if(null==sessionBean.getSelectedItemsList() || sessionBean.getSelectedItemsList().size()<=0)
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.checkbox"));
					return false;
				}
			}
			else
			{
				// no error message required - as the itemslist is null, so Schedule button will be disabled on screen
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "validateSchedule()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
			return false;
		}
		return true;
	}
	
	private void schedule(DataExportToolSessionBean sessionBean)
	{
		try
		{
			if(validateSchedule(sessionBean))
			{
				DataExportToolScheduleDetails schDetails = new DataExportToolScheduleDetails();
				schDetails.setScheduleName("MGSS_MNAO_EXPORT_");
				
				String schModelName=null;
				DataExportToolItemDetails itemDetails = null;
				/*
				 * IDENTIFY PROCESSING MODEL NAME AND APPEND IT TO SCHEDULE NAME
				 */
				for(int a=0;a<sessionBean.getSelectedItemsList().size();a++)
				{
					itemDetails = (DataExportToolItemDetails) sessionBean.getSelectedItemsList().get(a);
					if(null!=itemDetails.getModel() && !"".equals(itemDetails.getModel()))
					{
						schModelName = itemDetails.getModel();
						break;
					}
					itemDetails = null;
				}
				
				// replace / & space by _ underscore for schModelName
				if(null!=schModelName && !"".equals(schModelName))
				{
					schModelName = schModelName.replace("/", "_");
					schModelName = schModelName.replace(" ", "_");
					// append schModelName to scheduleName
					schDetails.setScheduleName(schDetails.getScheduleName()+schModelName+"_");
				}
				
				
				long totalDocsCount=0;
				
				/*
				 * IDENTIFY UNIQUE LOCALES FROM THE SELECTEDITEMS LIST
				 * AND FETCH DATA FOR ALL CARLINE FOR ALL CHANNELS
				 * ALL CARLINE DATA WILL ALWAYS GET EXTRACTED FOR THE SELECTED LOCALES FOR ALL CHANNELS
				 */
				List<String> uniqueLocalesList = null;
				List<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
				CarlineDetails clDetails = new CarlineDetails();
				clDetails.setCarlineCode("All");
				clDetails.setCarlineNameEng("All Carline");
				modelsList.add(clDetails);
				clDetails =null;
				boolean localeAdded = false;
				for(int a=0;a<sessionBean.getSelectedItemsList().size();a++)
				{
					itemDetails = (DataExportToolItemDetails) sessionBean.getSelectedItemsList().get(a);
					localeAdded = false;
					if(null!=uniqueLocalesList && uniqueLocalesList.size()>0)
					{
						for(int b=0;b<uniqueLocalesList.size();b++)
						{
							if(uniqueLocalesList.get(b).toString().equals(itemDetails.getLocale()))
							{
								// LOCALE ALREADY ADDED
								localeAdded=true;
							}
						}
					}
					if(localeAdded==false)
					{
						if(null==uniqueLocalesList || uniqueLocalesList.size()<=0)
						{
							uniqueLocalesList = new ArrayList<String>();
						}
						uniqueLocalesList.add(itemDetails.getLocale());
					}
					itemDetails = null;
				}
				itemDetails = null;
				
				if(null!=uniqueLocalesList && uniqueLocalesList.size()>0 && null!=modelsList && modelsList.size()>0)
				{
					logger.info("schedule :: > Fetching for "+uniqueLocalesList.size()+" Locales >> for All Carline Model.");
					
					/*
					 * FETCH DOCUMENTS MATRIX DATA ON THE BASIS OF IDENTIFIED UNIQUE LOCALE LIST AND MODEL LIST (ALL)
					 */
					ArrayList<DataExportToolItemDetails> tempAllCarlineItemDetails = DataExportToolDAO.getDocumentsMatrixList(uniqueLocalesList, modelsList, sessionBean.getFromDate(), sessionBean.getToDate());
					if(null!=tempAllCarlineItemDetails && tempAllCarlineItemDetails.size()>0)
					{
						// add these to selectedItemsList
						sessionBean.getSelectedItemsList().addAll(tempAllCarlineItemDetails);
					}
					tempAllCarlineItemDetails  =null;
				}
				else
				{
					logger.info("schedule :: Locales are null. All Carline Model documents cannot be fetched.");
				}
				uniqueLocalesList = null;
				modelsList = null;
				
				for(int a=0;a<sessionBean.getSelectedItemsList().size();a++)
				{
					itemDetails = (DataExportToolItemDetails) sessionBean.getSelectedItemsList().get(a);
					totalDocsCount = totalDocsCount+itemDetails.getDocumentsCounts();
					itemDetails = null;
				}
				schDetails.setTotalDocsCount(totalDocsCount);
				schDetails.setProcessingStatus(ScheduleConstants.STATUS_PENDING);
				schDetails.setWslId(wslId);
				schDetails.setItemsList(sessionBean.getSelectedItemsList());
				schDetails.setFromDateStr(sessionBean.getFromDate());
				schDetails.setToDateStr(sessionBean.getToDate());
				
				long scheduleId = DataExportToolDAO.createSchedule(schDetails);
				if(scheduleId > 0)
				{
					schDetails.setScheduleName(null);
					schDetails.setThreadId(null);
					// set them explicitly now
					if(null!=schModelName && !"".equals(schModelName))
					{
						schDetails.setScheduleName("MGSS_MNAO_EXPORT_"+schModelName+"_"+ String.valueOf(scheduleId));
					}
					else
					{
						schDetails.setScheduleName("MGSS_MNAO_EXPORT_" + String.valueOf(scheduleId));
					}
					schDetails.setThreadId(schDetails.getScheduleName());
					// set successMessage
					sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));
					// set scheduleName in sessionBean
					sessionBean.setScheduleName(schDetails.getScheduleName());
					
					/*
					 * start parallel Thread Processing
					 */
					final DataExportToolScheduleDetails details = new DataExportToolScheduleDetails();
					details.setScheduleId(scheduleId);
					details.setScheduleName(schDetails.getScheduleName());
					details.setProcessingStatus(ScheduleConstants.STATUS_PENDING);
					details.setWslId(wslId);
					details.setItemsList(sessionBean.getSelectedItemsList());
					details.setTotalDocsCount(schDetails.getTotalDocsCount());
					details.setThreadId(schDetails.getThreadId());
					
					final DataExportScheduleImpl startConvImpl = new DataExportScheduleImpl();
					Runnable runn = new Runnable() 
					{
						@Override
						public void run() {
							
							synchronized (startConvImpl) {
								try {
									startConvImpl.startProcessing(details);
								} catch (Exception e) {
									Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "run()", e);
								}
							}
						}
					};
					
					Thread th = new Thread(runn, schDetails.getThreadId());
					th.start();
					sessionBean.setSelectedItemsList(null);
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
				}
				schDetails = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolServlet.class.getName(), "schedule()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}
}
