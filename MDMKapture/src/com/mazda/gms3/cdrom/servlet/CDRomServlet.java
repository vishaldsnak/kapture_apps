package com.mazda.gms3.cdrom.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.bean.CDRomSearchBean;
import com.mazda.gms3.cdrom.bean.CDRomUtil;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.cdrom.bean.impl.CDRomSearchCriteriaJobImpl;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;

/**
 * Servlet implementation class CDRomServlet
 */
public class CDRomServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomServlet.class);
	String wslId = "";
	MessageProperties msgProps = null;
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;
	String scheduleIdForJobSearch=null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException 
	{
		scheduleIdForJobSearch=null;
		boolean useReqDis = true;
		if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) 
		{
			// Get WSL ID From request Header and set in Variable
			wslId = request.getHeader("iv-user");
			if (null == wslId || "".equals(wslId)) 
			{
				/*
				 * re direct to Error
				 */
				response.sendRedirect(request.getContextPath() + "/error");
				useReqDis = false;
				return;
			}
		}
		
		try 
		{
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			CDRomSearchBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false && sessionBean.isShowWriteControls() == false) 
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			// RESET ERROR MESSAGE
			sessionBean.setErrorMessage("");
			
			// RESET SESSIONBEAN
			if(null==request.getParameter("cromibk") || "".equals(request.getParameter("cromibk")))
			{
				// DO NOT RESET WHEN CLICKED BACK FROM DETAIL PAGE
				sessionBean.setSelectedCountry(null);
				sessionBean.setSelectedLanguage(null);
				sessionBean.setSelectedModel(null);
				sessionBean.setSelectedWmi(null);
				sessionBean.setSelectedVds(null);
				sessionBean.setSelectedVinRange(null);
				
				sessionBean.setCountryLocaleList(null);
				sessionBean.setLanguageList(null);
				sessionBean.setModels(null);
				sessionBean.setWmi(null);
				sessionBean.setVds(null);
				sessionBean.setVinRange(null);
				// WHEN CLICKED FORM BACK RESTORE THE PAGE LENGTH AND NUMBER VALUES
				sessionBean.setDisplayPageLength(null);
				sessionBean.setDisplayPageNo(null);
				// set today's date as ToDate
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				sessionBean.setToDate(sdf.format(new Date()));
				// get 30 days before today's date
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DATE, -30);
				Date dateBefore30Days = cal.getTime();
				sessionBean.setFromDate(sdf.format(dateBefore30Days));
				cal  =null;
				dateBefore30Days = null;
			}
			else
			{
				/*
				 * populate All List
				 */
				getLanguageList(sessionBean, request);
				CDRomUtil.manageModelsList(sessionBean);
				CDRomUtil.manageWmiList(sessionBean);
				CDRomUtil.manageVdsList(sessionBean);
				CDRomUtil.manageVinRangeList(sessionBean);
			}
			
			
			sessionBean.setActionClicked(null);
			sessionBean.setServiceContents(null);
			sessionBean.setEngineWorkshopManuals(null);
			sessionBean.setTransmissionWorkshopManual(null);
			sessionBean.setSelectedServiceContents(null);
			sessionBean.setSelectedEngineWorkshopManuals(null);
			sessionBean.setSelectedTransmissionWorkshopManual(null);
			sessionBean.setApplicableVINList(null);
			sessionBean.setItemsList(null);
			
			
			/* 
			 * Call function to getCountryList
			 */
			getCountryLocaleList(sessionBean, request);
			/*
			 * call function to getSearchCriteriaJob List
			 */
			getSearchCriteriaJobList(sessionBean);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(),
					"doGet()", e);
		}
		
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN
			 * IS NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size() == 1)) 
			{
				response.sendRedirect(request.getContextPath() + "/mypage");
			} 
			else 
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/cdrom/cdromIndex.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		String jspOutCome="/jsps/cdrom/cdromIndex.jsp";
		try
		{
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			
			CDRomSearchBean sessionBean = getSessionBean(request);
			// set ErrorMessage to empty
			sessionBean.setErrorMessage("");
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false && sessionBean.isShowWriteControls() == false) 
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			/*
			 * CALL Function to read Parameters from Request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_SELECTION"))
				{
					sessionBean.setSelectedLanguage(null);
					sessionBean.setSelectedModel(null);
					sessionBean.setSelectedWmi(null);
					sessionBean.setSelectedVds(null);
					sessionBean.setSelectedVinRange(null);
					/*
					 * populate All List
					 */
					getLanguageList(sessionBean, request);
					CDRomUtil.manageModelsList(sessionBean);
					CDRomUtil.manageWmiList(sessionBean);
					CDRomUtil.manageVdsList(sessionBean);
					CDRomUtil.manageVinRangeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setSelectedModel(null);
					sessionBean.setSelectedWmi(null);
					sessionBean.setSelectedVds(null);
					sessionBean.setSelectedVinRange(null);
					/*
					 * populate All List
					 */
					CDRomUtil.manageModelsList(sessionBean);
					CDRomUtil.manageWmiList(sessionBean);
					CDRomUtil.manageVdsList(sessionBean);
					CDRomUtil.manageVinRangeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("MODEL_SELECTION"))
				{
					sessionBean.setSelectedWmi(null);
					sessionBean.setSelectedVds(null);
					sessionBean.setSelectedVinRange(null);
					/*
					 * populate All List
					 */
					CDRomUtil.manageWmiList(sessionBean);
					CDRomUtil.manageVdsList(sessionBean);
					CDRomUtil.manageVinRangeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("WMI_SELECTION"))
				{
					sessionBean.setSelectedVds(null);
					sessionBean.setSelectedVinRange(null);
					/*
					 * populate All List
					 */
					CDRomUtil.manageVdsList(sessionBean);
					CDRomUtil.manageVinRangeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VDS_SELECTION"))
				{
					sessionBean.setSelectedVinRange(null);
					/*
					 * populate All List
					 */
					CDRomUtil.manageVinRangeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("GO_CLICKED"))
				{
					// VALIDATE ALL FIELDS AND NAVIGATE TO NEXT PAGE
					if(validate(sessionBean))
					{
						/*
						 * Schedule Search Criteria Job for identifying selected Engine & Mission Book Codes
						 */
						scheduleJobOperation(sessionBean, request);
						/*
						 * call function to getSearchCriteriaJob List
						 */
						getSearchCriteriaJobList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("REDIRECT_TO_DETAIL"))
				{
					if(null!=scheduleIdForJobSearch && !"".equals(scheduleIdForJobSearch))
					{
						// redirect to Detail Servlet
						jspOutCome = "REDIRECT_TO_DETAIL";
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					}
				}
				else if(sessionBean.getActionClicked().equals("REFRESH"))
				{
					/*
					 * call function to getSearchCriteriaJob List
					 */
					getSearchCriteriaJobList(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "doPost()", e);
		}
		if (useReqDis == true) 
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size() <= 0 
					|| (null != userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size() == 1)) 
			{
				response.sendRedirect(request.getContextPath() + "/mypage");
			} 
			else 
			{
				if(null!=jspOutCome && jspOutCome.equals("REDIRECT_TO_DETAIL"))
				{
					response.sendRedirect(request.getContextPath()+"/cdromdetail?cdsch="+scheduleIdForJobSearch);
				}
				else
				{
					RequestDispatcher rs = request.getRequestDispatcher(jspOutCome);
					rs.forward(request, response);
				}
			}
		}
		jspOutCome = null;
	}

	private CDRomSearchBean getSessionBean(HttpServletRequest request) {
		CDRomSearchBean sessionBean = null;
		if (null != request.getSession().getAttribute("CDRomSearchBean")
				&& !"".equals(request.getSession().getAttribute(
						"CDRomSearchBean"))) {
			sessionBean = (CDRomSearchBean) request.getSession().getAttribute(
					"CDRomSearchBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CDRomSearchBean();
			request.getSession().setAttribute("CDRomSearchBean", sessionBean);
		}
		return sessionBean;
	}

	private UserAccessBean getUserSessionBean(HttpServletRequest request) {
		UserAccessBean sessionBean = null;
		if (null != request.getSession().getAttribute("userAccessBean")
				&& !"".equals(request.getSession().getAttribute(
						"userAccessBean"))) {
			sessionBean = (UserAccessBean) request.getSession().getAttribute(
					"userAccessBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new UserAccessBean();
			request.getSession().setAttribute("userAccessBean", sessionBean);
		}
		return sessionBean;
	}

	private void performAccessCheck(CDRomSearchBean sessionBean,
			HttpServletRequest request) {
		/*
		 * Check User Has access to this Functionality or Not. Identify if User
		 * is Super Admin - then enable Read / Write Access on this page else -
		 * check if user has access to this functionality check for the
		 * accessType if READ ACCESS - SHOW READ CONTROLS if WRITE ACCESS - SHOW
		 * WRITE CONTROLS if READ & WRITE ACCESS - SHOW READ & WRITE CONTROLS
		 */
		try {
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setShowReadControls(false);
			sessionBean.setShowWriteControls(false);
			if (userSessionBean.isSuperAdminUser() == true) {
				sessionBean.setShowReadControls(true);
				sessionBean.setShowWriteControls(true);
			} else if (userSessionBean.isSuperAdminUser() == false) {
				if (null != userSessionBean.getUserAllModulesList()
						&& userSessionBean.getUserAllModulesList().size() > 0) {
					for (int a = 0; a < userSessionBean.getUserAllModulesList()
							.size(); a++) {
						ModuleDetails modDetails = (ModuleDetails) userSessionBean
								.getUserAllModulesList().get(a);
						if (modDetails.getModuleRefkey().trim().toLowerCase()
								.equals(moduleRefKey.trim().toLowerCase())) {
							// USER HAS ACCESS TO THIS SCREEN. CHECK FOR ACCESS
							// TYPE
							if (modDetails.getAccessType() == AccessManagementInterface.ONLY_READ_ACCESS) {
								sessionBean.setShowReadControls(true);
							} else if (modDetails.getAccessType() == AccessManagementInterface.ONLY_WRITE_ACCESS) {
								sessionBean.setShowWriteControls(true);
							} else if (modDetails.getAccessType() == AccessManagementInterface.READ_AND_WRITE_ACCESS) {
								sessionBean.setShowReadControls(true);
								sessionBean.setShowWriteControls(true);
							} else {
								sessionBean.setShowReadControls(false);
								sessionBean.setShowWriteControls(false);
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(),
					"performAccessCheck()", e);
		}
	}
	
	private void getCountryLocaleList(CDRomSearchBean sessionBean, HttpServletRequest request)
	{
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			/*
			 * SHOW ONLY JAPAN & UK
			 */
			String jajpLocale = ApplicationProperties.getProperty("ja_jp");
			String enukLocale = ApplicationProperties.getProperty("en_uk");
			
			if(null!=list && list.size()>0)
			{
				// IDENITFY COUNTRY CODES FOR JAPAN & UK
				String jajpCountryCode = jajpLocale.substring(jajpLocale.lastIndexOf("_")+1,jajpLocale.length());
				String enukCountryCode = enukLocale.substring(enukLocale.lastIndexOf("_")+1,enukLocale.length());
				
				ArrayList<CountryLocaleDetails> tempList = new ArrayList<CountryLocaleDetails>();
				// ITERATE LIST AND REMOVE ALL OTHER COUNTRIES
				for(int a=0;a<list.size();a++)
				{
					CountryLocaleDetails clDetails = (CountryLocaleDetails)list.get(a);
					if(clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(jajpCountryCode.trim().toLowerCase()) || 
						clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(enukCountryCode.trim().toLowerCase()))
					{
						// add to tempList
						tempList.add(clDetails);
					}
					clDetails = null;
				}
				// set tempList in list
				list = tempList;
				tempList = null;
				jajpCountryCode = null;
				enukCountryCode = null;
			}
			
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
			jajpLocale = null;
			enukLocale = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private void getLanguageList(CDRomSearchBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getSelectedCountry() && !"".equals(sessionBean.getSelectedCountry()))
			{
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				list = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getSelectedCountry());
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
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "getLanguageList()", e);
		}
	}

	private void readParamsFromRequest(CDRomSearchBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setSelectedCountry(null);
			sessionBean.setSelectedLanguage(null);
			sessionBean.setSelectedModel(null);
			sessionBean.setSelectedWmi(null);
			sessionBean.setSelectedVds(null);
			sessionBean.setSelectedVinRange(null);
			sessionBean.setActionClicked(null);
			scheduleIdForJobSearch=null;
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			
			// do not reset from & to fields
			if(null!=request.getParameter("fromDateField") && !"".equals(request.getParameter("fromDateField")))
			{
				sessionBean.setFromDate((String)request.getParameter("fromDateField"));
			}
			if(null!=request.getParameter("toDateField") && !"".equals(request.getParameter("toDateField")))
			{
				sessionBean.setToDate((String)request.getParameter("toDateField"));
			}
			/*
			 * set displayPageNo and displayPageLenght
			 */
			if (null != request.getParameter("MDM_HIS_displayPageNo") && !"".equals(request.getParameter("MDM_HIS_displayPageNo"))) 
			{
				sessionBean.setDisplayPageNo((String) request.getParameter("MDM_HIS_displayPageNo"));
			}
			if (null != request.getParameter("MDM_HIS_displayPageLen") && !"".equals(request.getParameter("MDM_HIS_displayPageLen"))) 
			{
				sessionBean.setDisplayPageLength((String) request.getParameter("MDM_HIS_displayPageLen"));
			}
			
			if(null!=request.getParameter("CD_Rom_Country_List") && !"".equals(request.getParameter("CD_Rom_Country_List")))
			{
				sessionBean.setSelectedCountry(request.getParameter("CD_Rom_Country_List"));
			}
			if(null!=request.getParameter("CD_Rom_Language_List") && !"".equals(request.getParameter("CD_Rom_Language_List")))
			{
				sessionBean.setSelectedLanguage(request.getParameter("CD_Rom_Language_List"));
			}
			if(null!=request.getParameter("CD_Rom_Models_List") && !"".equals(request.getParameter("CD_Rom_Models_List")))
			{
				sessionBean.setSelectedModel(request.getParameter("CD_Rom_Models_List"));
			}
			if(null!=request.getParameter("CD_Rom_Wmi_List") && !"".equals(request.getParameter("CD_Rom_Wmi_List")))
			{
				sessionBean.setSelectedWmi(request.getParameter("CD_Rom_Wmi_List"));
			}
			if(null!=request.getParameter("CD_Rom_vds_List") && !"".equals(request.getParameter("CD_Rom_vds_List")))
			{
				sessionBean.setSelectedVds(request.getParameter("CD_Rom_vds_List"));
			}
			if(null!=request.getParameter("CD_Rom_vinRange_List") && !"".equals(request.getParameter("CD_Rom_vinRange_List")))
			{
				sessionBean.setSelectedVinRange(request.getParameter("CD_Rom_vinRange_List"));
			}
			if(null!=request.getParameter("CD_Rom_ActionClicked") && !"".equals(request.getParameter("CD_Rom_ActionClicked")))
			{
				sessionBean.setActionClicked(request.getParameter("CD_Rom_ActionClicked"));
			}
			if(null!=request.getParameter("CD_Rom_SearchSchId") && !"".equals(request.getParameter("CD_Rom_SearchSchId")))
			{
				scheduleIdForJobSearch=request.getParameter("CD_Rom_SearchSchId");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "readParamsFromRequest()", e);
		}
	}
	
	private boolean validate(CDRomSearchBean sessionBean)
	{
		if (StringUtils.isBlank(sessionBean.getSelectedCountry())
				|| StringUtils.isBlank(sessionBean.getSelectedLanguage())
				|| StringUtils.isBlank(sessionBean.getSelectedModel())
				|| StringUtils.isBlank(sessionBean.getSelectedWmi())
				|| StringUtils.isBlank(sessionBean.getSelectedVds())
				|| StringUtils.isBlank(sessionBean.getSelectedVinRange())) {
			sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			return false;
		}
		return true;
	}
	
	
	private boolean scheduleJobOperation(CDRomSearchBean sessionBean , HttpServletRequest request)
	{
		boolean bool = false;
		try
		{
			
			String carlineNameRegional="";
			if(null!=sessionBean.getModels())
			{
				for(LabelBean clDetails : sessionBean.getModels())
				{
					if(clDetails.getKey().trim().toLowerCase().equals(sessionBean.getSelectedModel().trim().toLowerCase()))
					{
						carlineNameRegional = clDetails.getExtraAttribute().trim();
						/*
						 * Issue RE: MGSS-MC: Bongo(SK) of CD creation data has problem Date 25 Oct 2020
						 * REPLACE / BY SPACE IN THE REGIONAL NAME AS IT IS CREATING EXTRA FOLDER IN THE EXTRACTED STRUCTURE
						 */
						if(null!=carlineNameRegional && !"".equals(carlineNameRegional))
						{
							carlineNameRegional = carlineNameRegional.replace("/", " ");
						}
						break;
					}
					clDetails= null;
				}
			}
			
			String timeStamp = String.valueOf(System.currentTimeMillis());
			CDRomScheduleDetails schDetails = new CDRomScheduleDetails();
			schDetails.setScheduleName("MDM_CDROM_SC_" + timeStamp + "_");
			schDetails.setThreadId("MDM_CDROM_SC_");
			schDetails.setUserId(wslId);
			schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
			
			schDetails.setCountryLocalId(new Long(sessionBean.getSelectedCountry()).longValue());
			schDetails.setLocaleCode(sessionBean.getSelectedLanguage());
			if(null!=sessionBean.getLanguageList())
			{
				for(ManualLanguageDetails mlDetails : sessionBean.getLanguageList())
				{
					if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(sessionBean.getSelectedLanguage().trim().toLowerCase()))
					{
						schDetails.setLanguageLocalId(mlDetails.getManualLanguageId());
						break;
					}
					mlDetails = null;
				}
			}
			
			String carlineCode="";
			String modelNameEnglish="";
			if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
			{
				try
				{
					carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
					modelNameEnglish = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
				}
				catch(Exception e){}
			}
			// SET SELECTED MODEL AS CARLINE CODE TO BE USED FOR DETAILS PAGE FOR FURTHER PROCESSING.
			schDetails.setCarlineCode(sessionBean.getSelectedModel());
			// SET REGIONAL NAME + CARLINE CODE FOR DISPLAY
			schDetails.setCarlineNameRegional(carlineNameRegional.trim()+" ("+ carlineCode+")");
			
			schDetails.setVinWmiCode(sessionBean.getSelectedWmi());
			schDetails.setVinVdsCode(sessionBean.getSelectedVds());
			schDetails.setVinStartRange(sessionBean.getSelectedVinRange());
			
			
			Long scheduleId = CDRomDAO.createSchedule(schDetails);
			if(null!=scheduleId && scheduleId>0)
			{
				logger.info("scheduleJobOperation :: Job {"+schDetails.getScheduleName()+"} Scheduled Successfully.");
				bool = true;
				
				/*
				 * INITIATE JOB 
				 */
				final Long scheduleIdForJob = scheduleId;
				final String locale = sessionBean.getSelectedLanguage();
				final String vinStartRange = sessionBean.getSelectedVinRange().trim();
				final String vdsCode = sessionBean.getSelectedVds().trim();
				final String wmiCode = sessionBean.getSelectedWmi().trim();
				final String modelNameEng = modelNameEnglish.trim();
				final String carLineCode = carlineCode.trim();
				
				
				final CDRomSearchCriteriaJobImpl searchJobImpl = new CDRomSearchCriteriaJobImpl();
				String threadId = "MDM_CDROM_SC_" + String.valueOf(scheduleId);
				
				Runnable runn = new Runnable() {
					public void run() {
						synchronized (searchJobImpl) {
							logger.info("Thread starts here for schedule id : "	+ String.valueOf(scheduleIdForJob));

							searchJobImpl.startProcessing(locale, modelNameEng, carLineCode, wmiCode, vdsCode, vinStartRange, scheduleIdForJob);
						}
					}
				};
				Thread th = new Thread(runn, threadId);
				th.start();
				threadId = null;
			}
			else
			{
				logger.info("scheduleJobOperation :: Failed to Create Schedule Id. Throw Generic Error.");
				sessionBean.setErrorMessage(msgProps.getProperty("error.job.schedule.failure"));
			}
			schDetails =null;
			timeStamp = null;
			scheduleId = null;
			carlineNameRegional=null;
			carlineCode=null;
			modelNameEnglish = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "scheduleJobOperation()", e);
		}
		return bool;
	}

	private void getSearchCriteriaJobList(CDRomSearchBean sessionBean)
	{
		try
		{
			sessionBean.setRunReloadScript(false);
			sessionBean.setSearchCritriaJobList(new ArrayList<CDRomScheduleDetails>());
			if(null!=sessionBean.getFromDate() && !"".equals(sessionBean.getFromDate()) && null!=sessionBean.getToDate()  && !"".equals(sessionBean.getToDate()))
			{
				ArrayList<CDRomScheduleDetails> tempList = CDRomDAO.getSearchCriteriaScheduleDetails("",sessionBean.getFromDate(),sessionBean.getToDate());
				if(null!=tempList && tempList.size()>0)
				{
					CDRomScheduleDetails schDetails = null;
					for(int a=0;a<tempList.size();a++)
					{
						schDetails = (CDRomScheduleDetails)tempList.get(a);
						schDetails.setShowViewLink(false);
						if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
						{
							if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.pending.label"));
								// set reloadPage to true
								sessionBean.setRunReloadScript(true);
							}
							else if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.processing.label"));
								// set reloadPage to true
								sessionBean.setRunReloadScript(true);
							}
							else if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.success.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.success.label"));
								// set show View Link to true
								schDetails.setShowViewLink(true);
							}
							else if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.failure.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.failure.label"));
							}
							else if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.aborted.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.aborted.label"));
							}
							else if (schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.no.record.value"))) 
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.no.record.label"));
							}
						}
						sessionBean.getSearchCritriaJobList().add(schDetails);
						schDetails = null;
					}
				}
				tempList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomServlet.class.getName(), "getSearchCriteriaJobList()", e);
		}
	}
}
