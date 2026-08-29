package com.mazda.gms3.sst.servlet;

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
import java.util.Iterator;
import java.util.List;

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

import com.mazda.gms3.mdm.bean.UserAccessBean;
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
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.bean.SectionMasterBean;
import com.mazda.gms3.sst.dao.DivisionDAO;
import com.mazda.gms3.sst.dao.SectionDAO;
import com.mazda.gms3.sst.utils.SSTUtils;
import com.mazda.gms3.sst.vo.DivisionDetails;
import com.mazda.gms3.sst.vo.SectionDetails;

/**
 * Servlet implementation class SectionMaster
 */

public class SectionMaster extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = LogManager.getLogger(SectionMaster.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	static String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_SECTION_MASTER;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SectionMaster() {
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
			SectionMasterBean sessionBean = getSessionBean(request);
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
			sessionBean.setDivisionId(null);
			sessionBean.setDivisionList(null);
			sessionBean.setSectionList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setSectionListToImport(new ArrayList<SectionDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);
			
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sectionmaster.jsp");
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
			SectionMasterBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setInfoMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setReportViewPath(null);
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_LOCALE_SELECTION"))
				{
					/*
					 * call function to load all the Manual language data
					 */
					sessionBean.setManualLanguageId(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getLanguageList(sessionBean, request);
					sessionBean.setSelectedRows(null);
					
					/*
					 * call function to load sectionList
					 */
					getSectionList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * and Section Details List in case of language change
					 */
					sessionBean.setDivisionId(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getDivisionList(sessionBean);
					getSectionList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_SECTION"))
				{
					/*
					 * Save Operation called
					 */
					saveSectionDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_SECTION"))
				{
					/*
					 * Edit Operation called
					 */
					editSectionDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SECTION"))
				{
					/*
					 * Delete Operation called
					 */
					deleteSectionDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_SECTION"))
				{
					/*
					 * Active Operation called
					 */
					activeSectionDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_SECTION"))
				{
					/*
					 * Update Operation called
					 */
					updateSectionDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_SECTION"))
				{
					// reset some fields
					sessionBean.setSectionList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					sessionBean.setUpdatedRows(null);
					/*
					 * call getSectionList
					 */
					getSectionList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_SECTION"))
				{
					/*
					 * Export Section Operation
					 */
					exportSectionDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sectionmaster.jsp");
				rs.forward(request, response);
			}
		}
	}

	private SectionMasterBean getSessionBean(HttpServletRequest request) 
	{
		SectionMasterBean sessionBean = null;
		if (null != request.getSession().getAttribute("sectionBean") && !"".equals(request.getSession().getAttribute("sectionBean"))) 
		{
			sessionBean = (SectionMasterBean) request.getSession().getAttribute("sectionBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SectionMasterBean();
			request.getSession().setAttribute("sectionBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(SectionMasterBean sessionBean, HttpServletRequest request)
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
				 * + MNAO COUNTRIES = mnao.countries.codes
				 */
				String nonWritableMMECountries=ApplicationProperties.getProperty("mme.not.writable.countries").trim()+","+ApplicationProperties.getProperty("mnao.countries.codes").trim();
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(SectionMasterBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "getLanguageList()", e);
		}
	}
	
	private static void getDivisionList(SectionMasterBean sessionBean)
	{
		try
		{
			sessionBean.setDivisionList(new ArrayList<DivisionDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<DivisionDetails> list = new ArrayList<DivisionDetails>();
				list = DivisionDAO.getDivisionDetailsListForCombo(sessionBean.getManualLanguageId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setDivisionList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "getDivisionList()", e);
		}
	}
	
	private static void getFlagList(SectionMasterBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
		
	private static void readParamsFromRequest(SectionMasterBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new SectionDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setDivisionId(null);
			sessionBean.setSectionListToImport(null);
			
			
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
						if (fieldName.equals("SEC_UpdatedRows")) 
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
						
						
						if(fieldName.equals("SEC_SelectedRows"))
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
						if (fieldName.equals("SEC_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("SEC_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("SEC_SelectedRows")) 
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
						
						if (fieldName.equals("SEC_CountryLocale_Code")) 
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
						
						if (fieldName.equals("SEC_Lang_Code")) 
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
						
						if (fieldName.equals("SEC_Div_Code")) 
						{
							// set the value in sessionBean.setDivisionId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDivisionId(value);
								// set in Field Details as Well
								sessionBean.getFieldDetails().setDivisionId(new Long(value).longValue());
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_Index")) 
						{
							// set the value in sessionBean.getFieldDetails().setSectionIndex
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSectionIndex(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_Name")) 
						{
							// set the value in sessionBean.getFieldDetails().setSectionName
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSectionName(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setSectionName
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								Integer v = new Integer(value).intValue();
								sessionBean.getFieldDetails().setSectionCode(v);
								v = null;
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_SortId")) 
						{
							// set the value in sessionBean.getFieldDetails().setSortId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSortId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_HTMLPath")) 
						{
							// set the value in sessionBean.getFieldDetails().setHtmlPath
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
//								sessionBean.getFieldDetails().setHtmlPath(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SEC_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private static boolean validateFileUpload(SectionMasterBean sessionBean)
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

	private static boolean validate(SectionDetails fieldDetails, SectionMasterBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==fieldDetails.getDivisionId() || fieldDetails.getDivisionId()==0 ||
				null==fieldDetails.getSectionName() || "".equals(fieldDetails.getSectionName()) || 
				null==fieldDetails.getSortId() || "".equals(fieldDetails.getSortId()) || 
				null==fieldDetails.getSectionIndex() || "".equals(fieldDetails.getSectionIndex()) || 
				null==fieldDetails.getSectionCode())
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getSectionName() && !"".equals(fieldDetails.getSectionName()))
		{
			if(fieldDetails.getSectionName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.sectionname"),"200"));
			}
		}
		
		if(null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()))
		{
			// SECTION INDEX CAN BE 0
			if(fieldDetails.getSectionIndex().length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.sectionindex"),"4"));
			}
		}
		
//		if(null!=fieldDetails.getHtmlPath() && !"".equals(fieldDetails.getHtmlPath()))
//		{
//			if(fieldDetails.getHtmlPath().length()>256)
//			{
//				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//				{
//					errorMessage.append("<MSG_TOKEN>");
//				}
//				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.htmlpath"),"256"));
//			}
//		}
		if(null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()))
		{
			// SORT ID CAN BE 0
			if(fieldDetails.getSortId().length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.sortid"),"4"));
			}
		}
		
		if(null!=fieldDetails.getSectionCode())
		{
			String secCode = String.valueOf(fieldDetails.getSectionCode());
			if(secCode.length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.sectioncode"),"4"));
			}
			secCode = null;
		}
		
		/*
		 * SECTION CODE HAS TO BE UNIQUE ACROSS ALL DIVISIONS
		 * FOR A DIVISION, SORT ID CANNOT BE REPEATED
		 *
		 * NOTE: SECTION INDEX IS ONLY REQUIRED TO BE PRESENT BELOW - IT IS NOT CHECKED FOR
		 * UNIQUENESS. AN EARLIER COMMENT HERE CLAIMED IT WAS, WHICH NEVER MATCHED THE CODE.
		 */
		if(null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()) &&
				null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()) 
				&& null!=fieldDetails.getDivisionId() && fieldDetails.getDivisionId()>0 && null!=fieldDetails.getSectionCode())
		{
			if(null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
			{
				// sectionCode has to be unique across all Divisions
				for(int a=0;a<sessionBean.getSectionList().size();a++)
				{
					SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
					if(null!=details.getSectionCode())
					{
						if(fieldDetails.getSectionCode()==details.getSectionCode())
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.sectioncode")));
						
						}
					}
					details=  null;
				}
				
				// sort Id
				for(int a=0;a<sessionBean.getSectionList().size();a++)
				{
					SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
					if(fieldDetails.getDivisionId()==details.getDivisionId())
					{
						if(details.getSortId().trim().toLowerCase().equals(fieldDetails.getSortId().trim().toLowerCase()))
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String key=msgProps.getProperty("label.sortid")+" "+ msgProps.getProperty("label.for.lowercase")+" "+msgProps.getProperty("label.division");
							errorMessage.append(msgProps.addMessage("error.unique", key));
							key =null;
							break;
						}
					}
					details=  null;
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
	
	private static boolean validateUpdate(SectionDetails fieldDetails, SectionMasterBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getDivisionId() || fieldDetails.getDivisionId()==0 ||
				null==fieldDetails.getSectionName() || "".equals(fieldDetails.getSectionName()) || 
				null==fieldDetails.getSortId() || "".equals(fieldDetails.getSortId()) || 
				null==fieldDetails.getSectionIndex() || "".equals(fieldDetails.getSectionIndex()) || 
				null==fieldDetails.getSectionCode())
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getSectionName() && !"".equals(fieldDetails.getSectionName()))
		{
			if(fieldDetails.getSectionName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.sectionname")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()))
		{
			if(fieldDetails.getSectionIndex().length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.sectionindex")+",4,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
//		if(null!=fieldDetails.getHtmlPath() && !"".equals(fieldDetails.getHtmlPath()))
//		{
//			if(fieldDetails.getHtmlPath().length()>256)
//			{
//				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//				{
//					errorMessage.append("<MSG_TOKEN>");
//				}
//				String data = msgProps.getProperty("label.htmlpath")+",256,"+String.valueOf(fieldDetails.getSrNo());
//				String[] id = data.split(",");
//				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
//				data = null;
//				id = null;
//			}
//		}
		
		if(null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()))
		{
			if(fieldDetails.getSortId().length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.sortid")+",4,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getSectionCode())
		{
			String secCode = String.valueOf(fieldDetails.getSectionCode());
			if(secCode.length()>4)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.sectioncode")+",4,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		

		/*
		 * FOR A DIVISION, SORT ID AND SEECTION INDEX ID HAS TO BE UNIQUE
		 */
		if(null!=fieldDetails.getSectionId() && fieldDetails.getSectionId()>0 && 
				null!=fieldDetails.getDivisionId() && fieldDetails.getDivisionId()>0 &&
				null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()) &&
						null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()) && null!=fieldDetails.getSectionCode() &&
				null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
		{
			for(int a=0;a<sessionBean.getSectionList().size();a++)
			{
				SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
				if(null!=details.getSectionCode())
				{
					// SECTION CODE HAS TO BE UNIQUE ACROSS ALL DIVISIONS
					if(fieldDetails.getSectionId()!=details.getSectionId()  
							&& details.getSectionCode()== fieldDetails.getSectionCode())
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sectioncode")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
				}
				details=  null;
			}
			
			for(int a=0;a<sessionBean.getSectionList().size();a++)
			{
				SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
				
				// SORT ID HAS TO BE UNIQUE IN A DIVISION
				if(fieldDetails.getSectionId()!=details.getSectionId()  
						&& details.getDivisionId()== fieldDetails.getDivisionId())
				{
					if(fieldDetails.getSortId().trim().toLowerCase().equals(details.getSortId().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String key=msgProps.getProperty("label.sortid")+" "+ msgProps.getProperty("label.for.lowercase")+" "+msgProps.getProperty("label.division");
						String data = key+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						key = null;
						break;
					}
				}
				details=  null;
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
	
	private static boolean validateExcelRowData(SectionMasterBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getSectionListToImport() && sessionBean.getSectionListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getSectionListToImport().size();i++)
			{
				SectionDetails fieldDetails = (SectionDetails) sessionBean.getSectionListToImport().get(i);
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
				
				if(null==fieldDetails.getDivisionId() || fieldDetails.getDivisionId()==0 || 
						null==fieldDetails.getSectionIndex() || "".equals(fieldDetails.getSectionIndex()) ||
						null==fieldDetails.getSectionName() || "".equals(fieldDetails.getSectionName()) || 
						null==fieldDetails.getSortId() || "".equals(fieldDetails.getSortId()) || 
						null==fieldDetails.getSectionCodeFromExcel() || "".equals(fieldDetails.getSectionCodeFromExcel()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.section")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getSectionName() && !"".equals(fieldDetails.getSectionName()))
				{
					if(fieldDetails.getSectionName().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sectionname")+",200,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()))
				{
					if(fieldDetails.getSectionIndex().length()>4)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sectionindex")+",4,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					
					if(SSTUtils.validateNumbersOnly(fieldDetails.getSectionIndex())==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.sectionindex")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
//				if(null!=fieldDetails.getHtmlPath() && !"".equals(fieldDetails.getHtmlPath()))
//				{
//					if(fieldDetails.getHtmlPath().length()>256)
//					{
//						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
//						{
//							errorMessage.append("<MSG_TOKEN>");
//						}
//						String data = msgProps.getProperty("label.htmlpath")+",256,"+String.valueOf(rowNo);
//						String[] id = data.split(",");
//						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
//						data = null;
//						id = null;
//						// increment errorCount by 1
//						errorCount++;
//					}
//				}
				
				if(null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()))
				{
					if(fieldDetails.getSortId().length()>4)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sortid")+",4,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					if(SSTUtils.validateNumbersOnly(fieldDetails.getSortId())==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.sortid")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getSectionCodeFromExcel() && !"".equals(fieldDetails.getSectionCodeFromExcel()))
				{
					if(fieldDetails.getSectionCodeFromExcel().length()>4)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sectioncode")+",4,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					
					if(SSTUtils.validateNumbersOnly(fieldDetails.getSectionIndex())==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.sectioncode")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
			}
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.section")));
			// increment errorCount by 1
			errorCount++;
		}
		
		
		/*
		 * SORT ID MUST BE UNIQUE WITHIN A DIVISION.
		 *
		 * The same rule the on-screen add (validate) and edit (validateUpdate) already enforce -
		 * it was missing from the import. Checked here, before any database work, so the whole
		 * file is rejected rather than half applied.
		 *
		 * Two comparisons are needed:
		 *   1) against the rows already stored for that division, EXCLUDING the row this line
		 *      will itself update (matched on section code, the screen's unique key) - the same
		 *      exclusion validateUpdate makes with its own section id;
		 *   2) against the earlier rows of this same file, which the screen never has to
		 *      consider but an import does.
		 * ACTION=D rows are skipped - a delete does not carry a sort id.
		 */
		if(null!=sessionBean.getSectionListToImport() && sessionBean.getSectionListToImport().size()>0)
		{
			java.util.HashMap<String, Integer> sortIdsInFile = new java.util.HashMap<String, Integer>();
			for(int i=0;i<sessionBean.getSectionListToImport().size();i++)
			{
				SectionDetails fieldDetails = (SectionDetails) sessionBean.getSectionListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;

				if(ImportActionUtils.isDeleteAction(fieldDetails.getImportAction()))
				{
					continue;
				}
				if(null==fieldDetails.getSortId() || "".equals(fieldDetails.getSortId().trim())
						|| null==fieldDetails.getDivisionId() || fieldDetails.getDivisionId()<=0)
				{
					// missing values are already reported by the checks above
					continue;
				}

				String sortId = fieldDetails.getSortId().trim().toLowerCase();
				long divisionId = fieldDetails.getDivisionId().longValue();
				String sectionCode = (null==fieldDetails.getSectionCodeFromExcel()) ? ""
						: fieldDetails.getSectionCodeFromExcel().trim().toLowerCase();

				/*
				 * A row can clash with a stored row AND with an earlier row of this file at
				 * the same time. That is still ONE problem on ONE row - report it once.
				 */
				boolean sortIdReported = false;

				/*
				 * 1) AGAINST WHAT IS ALREADY STORED
				 */
				if(null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
				{
					for(int a=0;a<sessionBean.getSectionList().size();a++)
					{
						SectionDetails existing = (SectionDetails)sessionBean.getSectionList().get(a);
						if(null==existing.getDivisionId() || existing.getDivisionId().longValue()!=divisionId)
						{
							continue;
						}
						/*
						 * SKIP THE ROW THIS LINE WILL UPDATE - otherwise re-importing a row
						 * unchanged would report its own sort id as a duplicate.
						 */
						String existingCode = (null==existing.getSectionCode()) ? ""
								: String.valueOf(existing.getSectionCode()).trim().toLowerCase();
						if(!"".equals(sectionCode) && sectionCode.equals(existingCode))
						{
							continue;
						}
						if(null!=existing.getSortId()
								&& existing.getSortId().trim().toLowerCase().equals(sortId))
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String key = msgProps.getProperty("label.sortid")+" "+msgProps.getProperty("label.for.lowercase")+" "+msgProps.getProperty("label.division");
							errorMessage.append(msgProps.addMessage("error.unique.update", key, String.valueOf(rowNo)));
							key = null;
							errorCount++;
							sortIdReported = true;
							break;
						}
						existing = null;
					}
				}

				/*
				 * 2) AGAINST THE EARLIER ROWS OF THIS FILE
				 */
				String fileKey = String.valueOf(divisionId)+"|"+sortId;
				if(sortIdsInFile.containsKey(fileKey))
				{
					if(sortIdReported==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String key = msgProps.getProperty("label.sortid")+" "+msgProps.getProperty("label.for.lowercase")+" "+msgProps.getProperty("label.division");
						errorMessage.append(msgProps.addMessage("error.unique.update", key, String.valueOf(rowNo)));
						key = null;
						errorCount++;
					}
				}
				else
				{
					sortIdsInFile.put(fileKey, new Integer(rowNo));
				}
				fieldDetails = null;
			}
			sortIdsInFile = null;
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

	private static void decideErrorDisplay(SectionMasterBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_SECTION_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "validateExcelRowData()", e);
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
		
	private static void saveSectionDetails(SectionMasterBean sessionBean)
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
				boolean bool = SectionDAO.saveSectionDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveSectionDetails :: Section Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.section")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setSectionList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDivisionId(null);
					/*
					 * call getLanguageList
					 */
					getSectionList(sessionBean);
				}
				else
				{
					logger.info("saveSectionDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.section")));
				}
			}
			else
			{
				logger.info("saveSectionDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "saveSectionDetails()", e);
		}
	}
	
	private static void editSectionDetails(HttpServletRequest request, SectionMasterBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getSectionList() && !"".equals(sessionBean.getSectionList().size()>0))
			{
				for(int a=0;a<sessionBean.getSectionList().size();a++)
				{
					SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getSectionList() && !"".equals(sessionBean.getSectionList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getSectionList().size();a++)
							{
								SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
								if(rowId.equals(String.valueOf(details.getSectionId())))
								{
									logger.info("editSectionDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
									details.setEditableFlag(true);
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
				logger.info("editSectionDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "editSectionDetails()", e);
		}
	}

	private static void deleteSectionDetails(HttpServletRequest request, SectionMasterBean sessionBean)
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
					boolean bool = SectionDAO.deleteSectionDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.section")));
						/*
						 * call getSectionList
						 */
						getSectionList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.section")));
					}
				}
			}
			else
			{
				logger.info("deleteSectionDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "deleteSectionDetails()", e);
		}
	}

	private static void activeSectionDetails(HttpServletRequest request, SectionMasterBean sessionBean)
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
					boolean bool = SectionDAO.activeSectionDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.section")));
						/*
						 * call getMissionBookList
						 */
						getSectionList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.section")));
					}
				}
			}
			else
			{
				logger.info("activeSectionDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "activeSectionDetails()", e);
		}
	}
	
	private static void updateSectionDetails(HttpServletRequest request, SectionMasterBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<SectionDetails> updateDataList = new ArrayList<SectionDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
					{
						for(int i=0;i<sessionBean.getSectionList().size();i++)
						{
							SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setSectionIndex("");
								details.setSortId("");
//								details.setHtmlPath("");
								details.setDivisionId(null);
								details.setSectionName("");
								details.setFlag("");
								details.setSectionCode(null);
								/*
								 * fetch the values from request
								 * and set in sectionList
								 */
								String secIndex="SEC_List_Index"+String.valueOf(details.getSectionId());
								String name="SEC_List_Name"+String.valueOf(details.getSectionId());
								String flag="SEC_List_Flag_"+String.valueOf(details.getSectionId());
								String sortId="SEC_List_SortId"+String.valueOf(details.getSectionId());
								String divId="SEC_List_DivId"+String.valueOf(details.getSectionId());
//								String htmlPath="SEC_List_HTMLPath"+String.valueOf(details.getSectionId());
								String secCode = "SEC_List_Code"+String.valueOf(details.getSectionId());

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(secIndex))
										{
											details.setSectionIndex(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getSectionIndex() && !"".equals(details.getSectionIndex()))
											{
												details.setSectionIndex(details.getSectionIndex().trim());
											}
										}
										else if(key.equals(name))
										{
											details.setSectionName(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getSectionName() && !"".equals(details.getSectionName()))
											{
												details.setSectionName(details.getSectionName().trim());
											}
										}
										else if(key.equals(sortId))
										{
											details.setSortId(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getSortId() && !"".equals(details.getSortId()))
											{
												details.setSortId(details.getSortId().trim());
											}
										}
										else if(key.equals(secCode))
										{
											String val = token.substring(token.indexOf("<MDM_TS>")+8, token.length());
											if(null!=val && !"".equals(val))
											{
												details.setSectionCode(new Integer(val).intValue());
											}
											val = null;
										}
//										else if(key.equals(htmlPath))
//										{
//											details.setHtmlPath(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
//											if(null!=details.getHtmlPath() && !"".equals(details.getHtmlPath()))
//											{
//												details.setHtmlPath(details.getHtmlPath().trim());
//											}
//										}
										else if(key.equals(divId))
										{
											String val=token.substring(token.indexOf("<MDM_TS>")+8, token.length());
											if(null!=val && !"".equals(val) && !"null".equals(val.trim().toLowerCase()))
											{
												details.setDivisionId(new Long(val.trim()).longValue());
											}
											val = null;
										}
										else if(key.equals(flag))
										{
											details.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key = null;
										token= null;
									}
								}

								// set all request params ids to null
								secIndex = null;
								name=null;
								flag= null;
								sortId= null;
								divId = null;
//								htmlPath= null;
							}
						}

						for(int i=0;i<sessionBean.getSectionList().size();i++)
						{
							SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(i);
							if(details.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate details Object
								 */
								if(validateUpdate(details, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(details);
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
							logger.info("updateSectionDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							String closeConnection="N";
							Connection conn = null;
							try
							{
								conn = DBConnectionHelper.getConnection();
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "updateSectionDetails()", e);
							}
							for(int a=0;a<updateDataList.size();a++)
							{
								SectionDetails details = (SectionDetails)updateDataList.get(a);
								boolean updateFlag = SectionDAO.updateSectionDetails(details, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateSectionDetails :: Section Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateSectionDetails :: Failed to Update Section Details for Row No :: >  "+ details.getSrNo());
									errorMessage = errorMessage+String.valueOf(details.getSrNo())+",";	
								}
								details=  null;
							}
							closeConnection = null;
							try
							{
								if(null!=conn)
								{
									conn.close();
								}
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "updateSectionDetails()", e);
							}
							conn = null;

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.section"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.section"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateSectionDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setSectionList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getSectionList
								 */
								getSectionList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateSectionDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getSectionList().size();a++)
										{
											SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
											details.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(details.getSrNo())))
												{
													details.setEditableFlag(true);
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "updateSectionDetails()", e);
		}
	}
	
	private static void exportSectionDetails(HttpServletRequest request, SectionMasterBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<SectionDetails> exportDataList = new ArrayList<SectionDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getSectionList() && !"".equals(sessionBean.getSectionList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getSectionList().size();a++)
							{
								SectionDetails details = (SectionDetails)sessionBean.getSectionList().get(a);
								if(rowId.equals(String.valueOf(details.getSectionId())))
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
					writeSectionExcel(exportDataList, sessionBean);
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
					logger.info("exportSectionDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportSectionDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "exportSectionDetails()", e);
		}
	}

	private static void readExcelData(byte[] data, SectionMasterBean sessionBean, String extension)
	{
		sessionBean.setSectionListToImport(new ArrayList<SectionDetails>());
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
				 * ROW COLUMNS HAS TO BE IN FOLLOWING SEQUENCE
				 * GROUP CODE
				 * WMI CODE
				 * VDS CODE
				 * VIS START RANGE
				 * VIS END RANGE
				 * ENGINE CODE
				 * MISSION CODE
				 * 
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						SectionDetails details = new SectionDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setDivisionCode(String.valueOf(dataCell).trim());
							/*
							 * IDENTIFY DIVISION ID ON THE BASIS OF CODE
							 */
							if(null!=sessionBean.getDivisionList() && sessionBean.getDivisionList().size()>0)
							{
								for(int a=0;a<sessionBean.getDivisionList().size();a++)
								{
									DivisionDetails div = (DivisionDetails)sessionBean.getDivisionList().get(a);
									if(div.getDivisionCode().trim().toLowerCase().equals(details.getDivisionCode().trim().toLowerCase()))
									{
										details.setDivisionId(div.getDivisionId());
										break;
									}
								}
							}
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSectionCodeFromExcel(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSectionIndex(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSectionName(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSortId(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
//						dataCell = SSTUtils.readCellValue(row.getCell(5));
//						if(null!=dataCell && !"".equals(dataCell))
//						{
//							details.setHtmlPath(String.valueOf(dataCell).trim());
//						}
//						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						/*
						 * add details to sectionList for import
						 */
						if(null==sessionBean.getSectionListToImport() || sessionBean.getSectionListToImport().size()<=0)
						{
							sessionBean.setSectionListToImport(new ArrayList<SectionDetails>());
						}

						sessionBean.getSectionListToImport().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsSheet= null;
				xlsWorkBook =null;
				rowIterator=  null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "readExcelData()", e);
		}
	}

	private static void writeSectionExcel(ArrayList<SectionDetails> sectionList, SectionMasterBean sessionBean)
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
				 * So the final Name will be - US_EN-US_ND_SECTION_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_SECTION_NAME");
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
				
				Cell divisionCell = headerRow.createCell(0);
				divisionCell.setCellValue("DIVISION CODE");
				Cell secCodeCell = headerRow.createCell(1);
				secCodeCell.setCellValue("SECTION CODE");
				Cell secIndexCell = headerRow.createCell(2);
				secIndexCell.setCellValue("SECTION INDEX");
				Cell nameCell = headerRow.createCell(3);
				nameCell.setCellValue("SECTION NAME");
				Cell sortIdCell = headerRow.createCell(4);
				sortIdCell.setCellValue("SORT ID");
				Cell htmlPathCell = headerRow.createCell(5);
				htmlPathCell.setCellValue("HTML PATH");
				
				int rowCount=0;
				if(null!=sectionList && sectionList.size()>0)
				{
					for(int i=0;i<sectionList.size();i++)
					{
						SectionDetails details = (SectionDetails)sectionList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						if(null!=details.getDivisionCode() && !"".equals(details.getDivisionCode()))
						{
							cell0.setCellValue(details.getDivisionCode());
						}
						if(null!=details.getSectionCode())
						{
							cell1.setCellValue(String.valueOf(details.getSectionCode()));
						}
						if(null!=details.getSectionIndex() && !"".equals(details.getSectionIndex()))
						{
							cell2.setCellValue(details.getSectionIndex());
						}
						if(null!=details.getSectionName() && !"".equals(details.getSectionName()))
						{
							cell3.setCellValue(details.getSectionName());
						}
						if(null!=details.getSortId() && !"".equals(details.getSortId()))
						{
							cell4.setCellValue(details.getSortId());
						}
//						if(null!=details.getHtmlPath() && !"".equals(details.getHtmlPath()))
//						{
//							cell5.setCellValue(details.getHtmlPath());
//						}
						cell0 = null;
						cell1 = null;
						cell2=null;
						cell3=null;
						cell4=null;
						cell5 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					secIndexCell = null;
					nameCell=  null;
					sortIdCell= null;
					divisionCell= null;
					htmlPathCell=  null;
					secCodeCell=  null;
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "writeSectionExcel()", e);
		}
	}

	private static void executeExcelOperation(SectionMasterBean sessionBean, byte[] data, String extension)
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
				ArrayList<SectionDetails> listToSave = new ArrayList<SectionDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<SectionDetails> listToDelete = new ArrayList<SectionDetails>();
				for(int i=0;i<sessionBean.getSectionListToImport().size();i++)
				{
					SectionDetails fieldDetails = (SectionDetails)sessionBean.getSectionListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));

					/*
					 * SINCE ALL THE ROWS ARE VALIDATED - UPDATE SECTION CODE IN FIELD DETAILS
					 */
					if(null!=fieldDetails.getSectionCodeFromExcel() && !"".equals(fieldDetails.getSectionCodeFromExcel()))
					{
						fieldDetails.setSectionCode(new Integer(fieldDetails.getSectionCodeFromExcel()).intValue());
					}

					/*
					 * NOTE: the conversion above MUST stay ahead of the ACTION=D routing -
					 * sectionCode is the Integer the unique combination keys on, and a
					 * delete row that skipped it would reach the DAO with null.
					 */
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
					
					
					String tok="";
					if(null!=fieldDetails.getSectionIndex() && !"".equals(fieldDetails.getSectionIndex()))
					{
						tok = fieldDetails.getSectionIndex()+"<TOK>";
					}
					if(null!=fieldDetails.getSectionName() && !"".equals(fieldDetails.getSectionName()))
					{
						tok = tok+fieldDetails.getSectionName()+"<TOK>";
					}
					if(null!=fieldDetails.getDivisionName() && !"".equals(fieldDetails.getDivisionName()))
					{
						tok = tok+fieldDetails.getDivisionName()+"<TOK>";
					}
					if(null!=fieldDetails.getSortId() && !"".equals(fieldDetails.getSortId()))
					{
						tok = tok+fieldDetails.getSortId()+"<TOK>";
					}
//					if(null!=fieldDetails.getHtmlPath() && !"".equals(fieldDetails.getHtmlPath()))
//					{
//						tok = tok+fieldDetails.getHtmlPath()+"<TOK>";
//					}
					if(null!=fieldDetails.getSectionCode())
					{
						String secCode = String.valueOf(fieldDetails.getSectionCode());
						tok = tok+secCode+"<TOK>";
						secCode = null;
					}
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							/*
							 * IDENTIFY DUPLICATE ROW ON - 
							 * SECTION INDEX
							 * SECTION NAME
							 * SORT ID
							 * DIVISION NAME
							 */
							SectionDetails existingDetails = (SectionDetails)listToSave.get(j);
							
							String matchKey="";
							if(null!=existingDetails.getSectionIndex() && !"".equals(existingDetails.getSectionIndex()))
							{
								matchKey = existingDetails.getSectionIndex()+"<TOK>";
							}
							if(null!=existingDetails.getSectionName() && !"".equals(existingDetails.getSectionName()))
							{
								matchKey = matchKey+existingDetails.getSectionName()+"<TOK>";
							}
							if(null!=existingDetails.getDivisionName() && !"".equals(existingDetails.getDivisionName()))
							{
								matchKey = matchKey+existingDetails.getDivisionName()+"<TOK>";
							}
							if(null!=existingDetails.getSortId() && !"".equals(existingDetails.getSortId()))
							{
								matchKey = matchKey+existingDetails.getSortId()+"<TOK>";
							}
//							if(null!=existingDetails.getHtmlPath() && !"".equals(existingDetails.getHtmlPath()))
//							{
//								matchKey = matchKey+existingDetails.getHtmlPath()+"<TOK>";
//							}
							if(null!=existingDetails.getSectionCode())
							{
								String secCode = String.valueOf(existingDetails.getSectionCode());
								matchKey = matchKey+secCode+"<TOK>";
								secCode = null;
							}
							
							if(tok.trim().toLowerCase().equals(matchKey.trim().toLowerCase()))
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
							matchKey = null;
						}
					}
					if(addToList==true)
					{
						listToSave.add(fieldDetails);
					}
					tok  = null;
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.section"), String.valueOf(rows[a])));
						}
					}
					rows = null;
				}
				duplicateRowNo = null;
				
				
				if(null!=listToSave && listToSave.size()>0)
				{
					/*
					 * ITERATE AND START SAVING EACH ROW
					 */
					String successLineNo="";
					String errorLineNo="";
					int successCount=0;
					String closeConnection="N";
					Connection conn = null;
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
					}
					for(int i=0;i<listToSave.size();i++)
					{
						SectionDetails fieldDetails = (SectionDetails)listToSave.get(i);
						
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						boolean saveSectionData = SectionDAO.importSectionDetails(fieldDetails, conn, closeConnection);
						
						if(saveSectionData == true)
						{
							logger.info("executeExcelOperation() :: Section Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("executeExcelOperation() :: Failed to Import Section Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails= null;
					}
					
					closeConnection = null;
					try
					{
						if(null!=conn)
						{
							conn.close();
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
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
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.section"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated vin list
						 */
						getSectionList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.section"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.section"), errorLineNo));
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.section")));
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
							SectionDetails deleteDetails = (SectionDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = SectionDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
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
						Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
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
							deleted = SectionDAO.deleteSectionDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getSectionList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.section"), String.valueOf(deleteErrorRows[a])));
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "executeExcelOperation()", e);
		}
	}

	private static void getSectionList(SectionMasterBean sessionBean)
	{
		try
		{
			sessionBean.setSectionList(new ArrayList<SectionDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<SectionDetails> list = new ArrayList<SectionDetails>();
				list = SectionDAO.getSectionDetailsList(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						SectionDetails details = (SectionDetails)list.get(i);
						if(null!=details.getFlag() && !"".equals(details.getFlag()))
						{
							// set Label
							if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								details.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								details.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								details.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								details.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								details.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						details = null;
					}
					sessionBean.setSectionList(list);
				}
				list = null;
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "getSectionList()", e);
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
	
	private void performAccessCheck(SectionMasterBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SectionMaster.class.getName(), "performAccessCheck()", e);
		}
	}

}
