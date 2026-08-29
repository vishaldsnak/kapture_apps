package com.mazda.gms3.mdm.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.mazda.gms3.mdm.bean.BodyTypeBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.BodyTypeDAO;
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
import com.mazda.gms3.mdm.vo.BodyTypeDetails;
import com.mazda.gms3.mdm.vo.CategoryDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class BodyType
 */
public class BodyType extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	Logger logger = LogManager.getLogger(BodyType.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_BODY_TYPE;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BodyType() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
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
			BodyTypeBean sessionBean = getSessionBean(request);
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
			sessionBean.setBodyTypeList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setReportViewPath(null);
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/bodyType.jsp");
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
			BodyTypeBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			/*
			 * set displayPageNo and displayPageLenght
			 */
			if(null!=request.getParameter("BDT_DataTabel_displayPageNo") && !"".equals(request.getParameter("BDT_DataTabel_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("BDT_DataTabel_displayPageNo"));
			}
			if(null!=request.getParameter("BDT_DataTabel_displayPageLen") && !"".equals(request.getParameter("BDT_DataTabel_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("BDT_DataTabel_displayPageLen"));
			}
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=request.getParameter("BDT_CountrySelection") && !"".equals(request.getParameter("BDT_CountrySelection")))
			{
				sessionBean.setShowUpdate(false);
				sessionBean.setSelectedRows(null);
				sessionBean.setManualLanguageId(null);
				/*
				 * call function to load all the Manual language data
				 */
				getLanguageList(sessionBean, request);
				/*
				 * call function to load axleTypeList
				 */
				getBodyTypeList(sessionBean);
			}
			
			if(null!=request.getParameter("BDT_LanguageSelection") && !"".equals(request.getParameter("BDT_LanguageSelection")))
			{
				sessionBean.setShowUpdate(false);
				sessionBean.setSelectedRows(null);
				/*
				 * call function to load bodyTypeList
				 */
				getBodyTypeList(sessionBean);
			}
			
			if(null!=request.getParameter("BDT_Entry") && !"".equals(request.getParameter("BDT_Entry")))
			{
				/*
				 * Save Operation called
				 */
				saveBodyTypeDetails(sessionBean);
			}
			
			if(null!=request.getParameter("BDT_EditAction") && !"".equals(request.getParameter("BDT_EditAction")))
			{
				/*
				 * Edit Operation called
				 */
				editBodyTypeDetails(request, sessionBean);	
			}
			
			if(null!=request.getParameter("BDT_DeleteAction") && !"".equals(request.getParameter("BDT_DeleteAction")))
			{
				/*
				 * Edit Operation called
				 */
				deleteBodyTypeDetails(request, sessionBean);	
			}
			
			if(null!=request.getParameter("BDT_Update") && !"".equals(request.getParameter("BDT_Update")))
			{
				/*
				 * Update Operation called
				 */
				updateBodyTypeDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("BDT_ActiveAction") && !"".equals(request.getParameter("BDT_ActiveAction")))
			{
				/*
				 * call Active Operation
				 */
				activeBodyTypeDetails(request, sessionBean);
			}
			if(null!=request.getParameter("BDT_ExportAction") && !"".equals(request.getParameter("BDT_ExportAction")))
			{
				/*
				 * call Export Operation
				 */
				exportBodyTypeDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("BDT_ResetAction") && !"".equals(request.getParameter("BDT_ResetAction")))
			{
				// reset fields
				sessionBean.setBodyTypeList(null);
				sessionBean.setFlagList(null);
				sessionBean.setErrorMessage(null);
				sessionBean.setSuccessMessage(null);
				sessionBean.setSelectedRows(null);
				sessionBean.setShowUpdate(false);
				sessionBean.setDisplayPageLength(null);
				sessionBean.setDisplayPageNo(null);
				sessionBean.setReportViewPath(null);
				/*
				 * call getBodyTypeList
				 */
				getBodyTypeList(sessionBean);	
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(),"doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/bodyType.jsp");
				rs.forward(request, response);
			}
		}
	}

	
	private BodyTypeBean getSessionBean(HttpServletRequest request) 
	{
		BodyTypeBean sessionBean = null;
		if (null != request.getSession().getAttribute("bodyTypeBean") && !"".equals(request.getSession().getAttribute("bodyTypeBean"))) 
		{
			sessionBean = (BodyTypeBean) request.getSession().getAttribute("bodyTypeBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new BodyTypeBean();
			request.getSession().setAttribute("bodyTypeBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(BodyTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(BodyTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "getLanguageList()", e);
		}
	}
	
	private void getBodyTypeList(BodyTypeBean sessionBean)
	{
		try
		{
			sessionBean.setBodyTypeList(new ArrayList<BodyTypeDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<BodyTypeDetails> list = new ArrayList<BodyTypeDetails>();
				list = BodyTypeDAO.getAllBodyTypesList(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						BodyTypeDetails bDetails = (BodyTypeDetails)list.get(i);
						if(null!=bDetails.getFlag() && !"".equals(bDetails.getFlag()))
						{
							// set Label
							if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								bDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								bDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								bDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								bDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(bDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								bDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						bDetails=  null;
					}
					sessionBean.setBodyTypeList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "getBodyTypeList()", e);
		}
	}
	
	private void getFlagList(BodyTypeBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
	
	private void readParamsFromRequest(BodyTypeBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new BodyTypeDetails());
			sessionBean.setManualLanguageId(null);
			sessionBean.setCountryLocaleId(null);
			
			if(null!=request.getParameter("BDT_CountryLocale_Code") && !"".equals(request.getParameter("BDT_CountryLocale_Code")))
			{
				sessionBean.setCountryLocaleId((String)request.getParameter("BDT_CountryLocale_Code").trim());
			}
			if(null!=request.getParameter("BDT_Lang_Code") && !"".equals(request.getParameter("BDT_Lang_Code")))
			{
				sessionBean.setManualLanguageId((String)request.getParameter("BDT_Lang_Code").trim());
			}
			if(null!=request.getParameter("BDT_Type_Code") && !"".equals(request.getParameter("BDT_Type_Code")))
			{
				sessionBean.getFieldDetails().setBodyCode((String)request.getParameter("BDT_Type_Code").trim());
			}
			
			if(null!=request.getParameter("BDT_Description") && !"".equals(request.getParameter("BDT_Description")))
			{
				sessionBean.getFieldDetails().setBodyCodeDescription((String)request.getParameter("BDT_Description").trim());
			}
			
			if(null!=request.getParameter("BDT_Description_Reg") && !"".equals(request.getParameter("BDT_Description_Reg")))
			{
				sessionBean.getFieldDetails().setBodyCodeDescriptionRegional((String)request.getParameter("BDT_Description_Reg").trim());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean validate(BodyTypeDetails fieldDetails, BodyTypeBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
				null==fieldDetails.getBodyCode() || "".equals(fieldDetails.getBodyCode()) || 
				null==fieldDetails.getBodyCodeDescription() || "".equals(fieldDetails.getBodyCodeDescription()) || 
				null==fieldDetails.getBodyCodeDescriptionRegional() || "".equals(fieldDetails.getBodyCodeDescriptionRegional()))  
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		
		if(null!=fieldDetails.getBodyCode() && !"".equals(fieldDetails.getBodyCode()))
		{
			if(fieldDetails.getBodyCode().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.bodytypecode"),"10"));
			}
		}
		
		if(null!=fieldDetails.getBodyCodeDescription() && !"".equals(fieldDetails.getBodyCodeDescription()))
		{
			if(fieldDetails.getBodyCodeDescription().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.bodytypedesc"),"200"));
			}
		}
		
		if(null!=fieldDetails.getBodyCodeDescriptionRegional() && !"".equals(fieldDetails.getBodyCodeDescriptionRegional()))
		{
			if(fieldDetails.getBodyCodeDescriptionRegional().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.bodytypedesc.reg"),"200"));
			}
		}
		
		if(null!=fieldDetails.getBodyCode() && !"".equals(fieldDetails.getBodyCode()))
		{
			if(null!=sessionBean.getBodyTypeList() && sessionBean.getBodyTypeList().size()>0)
			{
				for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
				{
					BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
					if(bDetails.getBodyCode().trim().toLowerCase().equals(fieldDetails.getBodyCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.bodytypecode")));
						break;
					}
					bDetails=  null;
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
	
	private boolean validateUpdate(BodyTypeDetails fieldDetails, BodyTypeBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getBodyCode() || "".equals(fieldDetails.getBodyCode()) ||
				null==fieldDetails.getBodyCodeDescription() || "".equals(fieldDetails.getBodyCodeDescription()) ||
						null==fieldDetails.getBodyCodeDescriptionRegional() || "".equals(fieldDetails.getBodyCodeDescriptionRegional()) ||
				null==fieldDetails.getBodyTypeId() || "".equals(fieldDetails.getBodyTypeId())) 
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}
		
		if(null!=fieldDetails.getBodyCode() && !"".equals(fieldDetails.getBodyCode()))
		{
			if(fieldDetails.getBodyCode().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.bodytypecode")+",10,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getBodyCodeDescription() && !"".equals(fieldDetails.getBodyCodeDescription()))
		{
			if(fieldDetails.getBodyCodeDescription().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.bodytypedesc")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getBodyCodeDescriptionRegional() && !"".equals(fieldDetails.getBodyCodeDescriptionRegional()))
		{
			if(fieldDetails.getBodyCodeDescriptionRegional().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.bodytypedesc.reg")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getBodyCode() && !"".equals(fieldDetails.getBodyCode()))
		{
			if(null!=sessionBean.getBodyTypeList() && sessionBean.getBodyTypeList().size()>0)
			{
				for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
				{
					BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
					if(bDetails.getBodyTypeId()!=fieldDetails.getBodyTypeId() && 
							bDetails.getBodyCode().trim().toLowerCase().equals(fieldDetails.getBodyCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.bodytypecode")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
					bDetails=  null;
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
	
	private void saveBodyTypeDetails(BodyTypeBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */
				sessionBean.getFieldDetails().setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
				sessionBean.getFieldDetails().setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = BodyTypeDAO.saveBodyTypeDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveBodyTypeDetails :: Body Type Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.bodytype")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setBodyTypeList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getBodyTypeList
					 */
					getBodyTypeList(sessionBean);
				}
				else
				{
					logger.info("saveBodyTypeDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.bodytype")));
				}
			}
			else
			{
				logger.info("saveBodyTypeDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "saveBodyTypeDetails()", e);
		}
	}
	
	private void editBodyTypeDetails(HttpServletRequest request, BodyTypeBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			sessionBean.setSelectedRows(null);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getBodyTypeList() && !"".equals(sessionBean.getBodyTypeList().size()>0))
			{
				for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
				{
					BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
					bDetails.setEditableFlag(false);
				}
			}
			if(null!=request.getParameter("BDT_SelectedRows") && !"".equals(request.getParameter("BDT_SelectedRows")))
			{
				sessionBean.setSelectedRows(String.valueOf(request.getParameter("BDT_SelectedRows")));
				
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getBodyTypeList() && !"".equals(sessionBean.getBodyTypeList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
							{
								BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
								if(rowId.equals(String.valueOf(bDetails.getBodyTypeId())))
								{
									logger.info("editBodyTypeDetails :: Making Row No {"+bDetails.getSrNo()+"} Editable.");
									bDetails.setEditableFlag(true);
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
				logger.info("editBodyTypeDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "editBodyTypeDetails()", e);
		}
	}

	private void deleteBodyTypeDetails(HttpServletRequest request, BodyTypeBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("BDT_SelectedRows") && !"".equals(request.getParameter("BDT_SelectedRows")))
			{
				String deleteIds=String.valueOf(request.getParameter("BDT_SelectedRows"));
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = BodyTypeDAO.deleteBodyDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.bodytype")));
						/*
						 * call getCarList
						 */
						getBodyTypeList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.bodytype")));
					}
				}
			}
			else
			{
				logger.info("deleteBodyTypeDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "deleteBodyTypeDetails()", e);
		}
	}

	private void updateBodyTypeDetails(HttpServletRequest request, BodyTypeBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("BDT_UpdatedRows") && !"".equals(request.getParameter("BDT_UpdatedRows")))
			{
				ArrayList<BodyTypeDetails> updateDataList = new ArrayList<BodyTypeDetails>();
				String updatedRows=(String)request.getParameter("BDT_UpdatedRows");
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getBodyTypeList() && sessionBean.getBodyTypeList().size()>0)
					{

						for(int i=0;i<sessionBean.getBodyTypeList().size();i++)
						{
							BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(i);
							if(bDetails.isEditableFlag()==true)
							{
								// set fields empty 
								bDetails.setBodyCode("");
								bDetails.setFlag("");
								bDetails.setBodyCodeDescription("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String bodyTypeCodeId="BDT_BodyList_Code_"+String.valueOf(bDetails.getBodyTypeId());
								String descId="BDT_BodyList_Desc_"+String.valueOf(bDetails.getBodyTypeId());
								String descRegId="BDT_BodyList_Desc_Reg_"+String.valueOf(bDetails.getBodyTypeId());
								String flag="BDT_BodyList_Flag_"+String.valueOf(bDetails.getBodyTypeId());

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										String value=token.substring(token.indexOf("<MDM_TS>")+8, token.length());
										if(key.equals(bodyTypeCodeId))
										{
											bDetails.setBodyCode(value);
										}
										else if(key.equals(descId))
										{
											bDetails.setBodyCodeDescription(value);
										}
										else if(key.equals(descRegId))
										{
											bDetails.setBodyCodeDescriptionRegional(value);
										}
										else if(key.equals(flag))
										{
											bDetails.setFlag(value);	
										}
										key = null;
										value= null;
										token = null;
									}
								}

								// set all request params ids to null
								bodyTypeCodeId = null;
								descId = null;
								/*
								cdromId=  null;
								ewInducingId = null;
								contentLang = null;
								*/
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getBodyTypeList().size();i++)
						{
							BodyTypeDetails bDetails = (BodyTypeDetails)sessionBean.getBodyTypeList().get(i);
							if(bDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate bDetails Object
								 */
								if(validateUpdate(bDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(bDetails);
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
							logger.info("updateBodyTypeDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";

							for(int a=0;a<updateDataList.size();a++)
							{
								BodyTypeDetails bDetails = (BodyTypeDetails)updateDataList.get(a);
								boolean updateFlag = BodyTypeDAO.updateBodyTypeDetails(bDetails);
								if(updateFlag==true)
								{
									logger.info("updateBodyTypeDetails :: Body Type Details updated successfully for Row No :: > " + bDetails.getSrNo());
									successMessage = successMessage+String.valueOf(bDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateBodyTypeDetails :: Failed to Update Axle Type Details for Row No :: >  "+ bDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(bDetails.getSrNo())+",";	
								}
								bDetails=  null;
							}

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.bodytype"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.bodytype"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateBodyTypeDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setBodyTypeList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getBodyTypeList
								 */
								getBodyTypeList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateBodyTypeDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getBodyTypeList() && sessionBean.getBodyTypeList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
										{
											BodyTypeDetails alD = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
											alD.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(alD.getSrNo())))
												{
													alD.setEditableFlag(true);
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
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "updateBodyTypeDetails()", e);
		}
	}
	
	private void activeBodyTypeDetails(HttpServletRequest request, BodyTypeBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("BDT_SelectedRows") && !"".equals(request.getParameter("BDT_SelectedRows")))
			{
				String activeIds=String.valueOf(request.getParameter("BDT_SelectedRows"));
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
					BodyTypeDetails cDetails = null;
					Map<String,String> dataMap = null;
					if(null!=tok && tok.length>0)
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=sessionBean.getBodyTypeList() && sessionBean.getBodyTypeList().size()>0)
							{
								cDetails  =null;
								for(int b=0;b<sessionBean.getBodyTypeList().size();b++)
								{
									cDetails=  (BodyTypeDetails)sessionBean.getBodyTypeList().get(b);
									if(String.valueOf(cDetails.getBodyTypeId()).equals(tok[a]))
									{
										dataMap= new HashMap<String, String>();
										dataMap.put("ID", tok[a]);
										dataMap.put("FLAG", cDetails.getOldFlag());
										activeIdsList.add(dataMap);
										dataMap=  null;
									}
									cDetails = null;
								}
							}
						}
					}
					tok = null;
					dataMap=  null;
					boolean bool = BodyTypeDAO.activeBodyDetails(activeIdsList);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.bodytype")));
						/*
						 * call getCarList
						 */
						getBodyTypeList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.bodytype")));
					}
					activeIdsList = null;
				}
				activeIds = null;
			}
			else
			{
				logger.info("activeBodyTypeDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "activeBodyTypeDetails()", e);
		}
	}

	
	private  void exportBodyTypeDetails(HttpServletRequest request, BodyTypeBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		sessionBean.setSelectedRows(null);
		try
		{
			if(null!=request.getParameter("BDT_SelectedRows") && !"".equals(request.getParameter("BDT_SelectedRows")))
			{
				sessionBean.setSelectedRows(String.valueOf(request.getParameter("BDT_SelectedRows")));
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<BodyTypeDetails> exportDataList = new ArrayList<BodyTypeDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getBodyTypeList() && !"".equals(sessionBean.getBodyTypeList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getBodyTypeList().size();a++)
							{
								BodyTypeDetails details = (BodyTypeDetails)sessionBean.getBodyTypeList().get(a);
								if(rowId.equals(String.valueOf(details.getBodyTypeId())))
								{
									// add to export List
									exportDataList.add(details);
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
					writeBodyTypeExcel(exportDataList, sessionBean);
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
					logger.info("exportBodyTypeDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportBodyTypeDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "exportBodyTypeDetails()", e);
		}
	}

	private void writeBodyTypeExcel(ArrayList<BodyTypeDetails> bodyTypeList, BodyTypeBean sessionBean)
	{
		try
		{
			String path = ApplicationProperties.getProperty("EXPORT_DATA_PHYSICAL_PATH");
			if(null!=path && !"".equals(path))
			{
				if(!path.endsWith("\\"))
				{
					path = path+"\\";
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_BODY_TYPE_NAME");
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
				carlineCodeCell.setCellValue("BODY TYPE CODE");
				Cell carNameEngCell = headerRow.createCell(1);
				carNameEngCell.setCellValue("BODY TYPE NAME REGIONAL LANG");
				Cell carNameRegCell = headerRow.createCell(2);
				carNameRegCell.setCellValue("BODY TYPE NAME ENG LANG");
				
				int rowCount=0;
				if(null!=bodyTypeList && bodyTypeList.size()>0)
				{
					for(int i=0;i<bodyTypeList.size();i++)
					{
						BodyTypeDetails details = (BodyTypeDetails)bodyTypeList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						
						if(null!=details.getBodyCode() && !"".equals(details.getBodyCode()))
						{
							cell0.setCellValue(details.getBodyCode());
						}
						if(null!=details.getBodyCodeDescriptionRegional() && !"".equals(details.getBodyCodeDescriptionRegional()))
						{
							cell1.setCellValue(details.getBodyCodeDescriptionRegional());
						}
						if(null!=details.getBodyCodeDescription() && !"".equals(details.getBodyCodeDescription()))
						{
							cell2.setCellValue(details.getBodyCodeDescription());
						}
						
						cell0 = null;
						cell1 = null;
						cell2 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					carlineCodeCell = null;
					carNameEngCell = null;
					carNameRegCell = null;
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
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "writeBodyTypeExcel()", e);
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
	
	private void performAccessCheck(BodyTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(BodyType.class.getName(), "performAccessCheck()", e);
		}
	}

}
