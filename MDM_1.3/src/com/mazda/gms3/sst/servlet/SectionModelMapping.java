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
import com.mazda.gms3.sst.bean.SectionModelMappingBean;
import com.mazda.gms3.sst.dao.DivisionDAO;
import com.mazda.gms3.sst.dao.SSTMasterDAO;
import com.mazda.gms3.sst.dao.SectionDAO;
import com.mazda.gms3.sst.dao.SectionModelMappingDAO;
import com.mazda.gms3.sst.utils.SSTUtils;
import com.mazda.gms3.sst.vo.DivisionDetails;
import com.mazda.gms3.sst.vo.SSTDetails;
import com.mazda.gms3.sst.vo.SectionDetails;
import com.mazda.gms3.sst.vo.SectionModelMappingDetails;

public class SectionModelMapping extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static Logger logger = LogManager.getLogger(SectionModelMapping.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	static String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_SST_SECTION_MODEL_MAPPING;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SectionModelMapping() {
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
			SectionModelMappingBean sessionBean = getSessionBean(request);
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
			sessionBean.setModelList(null);
			sessionBean.setSectionList(null);
			sessionBean.setMappingList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setMappingListToImport(new ArrayList<SectionModelMappingDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setSstList(null);
			sessionBean.setDivisionList(null);
			sessionBean.setDivisionId(null);
			
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sectionmodelmapping.jsp");
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
			SectionModelMappingBean sessionBean = getSessionBean(request);
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
					sessionBean.setDivisionId(null);
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setSectionId(null);
						sessionBean.getFieldDetails().setModelCode(null);
						sessionBean.getFieldDetails().setSstId(null);
					}
					getDivisionList(sessionBean);
					getSectionList(sessionBean);
					getSSTList(sessionBean);
					getModelList(sessionBean);
					/*
					 * call function to load abbreviationList
					 */
					getSectionModelMappingList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * and Division Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setDivisionId(null);
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setSectionId(null);
						sessionBean.getFieldDetails().setModelCode(null);
						sessionBean.getFieldDetails().setSstId(null);
					}
					getDivisionList(sessionBean);
					getModelList(sessionBean);
					getSSTList(sessionBean);
					getSectionList(sessionBean);
					getSectionModelMappingList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DIVISION_SELECTION"))
				{
					/*
					 * and Division Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setSectionId(null);
					}
					getSectionList(sessionBean);
					getSectionModelMappingList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_SECTIONMODELMAP"))
				{
					/*
					 * Save Operation called
					 */
					saveSectionModelMappingDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_SECTIONMODELMAP"))
				{
					/*
					 * Edit Operation called
					 */
					editSectionModelMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SECTIONMODELMAP"))
				{
					/*
					 * Delete Operation called
					 */
					deleteSectionModelMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_SECTIONMODELMAP"))
				{
					/*
					 * Active Operation called
					 */
					activeSectionModelMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_SECTIONMODELMAP"))
				{
					/*
					 * Update Operation called
					 */
					updateSectionModelMappingDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_SECTIONMODELMAP"))
				{
					// reset some fields
					sessionBean.setMappingList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					sessionBean.setUpdatedRows(null);
					/*
					 * call getSectionModelMappingList
					 */
					getSectionModelMappingList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_SECTIONMODELMAP"))
				{
					/*
					 * Export Division Operation
					 */
					exportSectionModelMappingDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sectionmodelmapping.jsp");
				rs.forward(request, response);
			}
		}
	}

	private SectionModelMappingBean getSessionBean(HttpServletRequest request) 
	{
		SectionModelMappingBean sessionBean = null;
		if (null != request.getSession().getAttribute("sectionMappingBean") && !"".equals(request.getSession().getAttribute("sectionMappingBean"))) 
		{
			sessionBean = (SectionModelMappingBean) request.getSession().getAttribute("sectionMappingBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SectionModelMappingBean();
			request.getSession().setAttribute("sectionMappingBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(SectionModelMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(SectionModelMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getLanguageList()", e);
		}
	}
	
	
	private static void getDivisionList(SectionModelMappingBean sessionBean)
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
	
	/**
	 * Here GET MODLES ON THE BASIS OF SELECTED DIVISION
	 * @param sessionBean
	 */
	private static void getModelList(SectionModelMappingBean sessionBean)
	{
		try
		{
			sessionBean.setModelList(new ArrayList<CarlineDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				list = CarlineDAO.getModelDetailsListForSSTCombo(sessionBean.getManualLanguageId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setModelList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getModelList()", e);
		}
	}
	
	private static void getSectionList(SectionModelMappingBean sessionBean)
	{
		try
		{
			sessionBean.setSectionList(new ArrayList<SectionDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) && 
					null!=sessionBean.getDivisionId() && !"".equals(sessionBean.getDivisionId()))
			{
				ArrayList<SectionDetails> list = new ArrayList<SectionDetails>();
				list = SectionDAO.getSectionDetailsListForCombo(sessionBean.getManualLanguageId(), sessionBean.getDivisionId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setSectionList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getSectionList()", e);
		}
	}
	
	private static void getSSTList(SectionModelMappingBean sessionBean)
	{
		try
		{
			sessionBean.setSstList(new ArrayList<SSTDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<SSTDetails> list = new ArrayList<SSTDetails>();
				list = SSTMasterDAO.getSSTDetailsListForCombo(sessionBean.getManualLanguageId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setSstList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getSSTList()", e);
		}
	}
	
	
	private static void getFlagList(SectionModelMappingBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
		
	private static void readParamsFromRequest(SectionModelMappingBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new SectionModelMappingDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setMappingListToImport(null);
			sessionBean.setDivisionId(null);
			
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
						if (fieldName.equals("SECMODELMAP_UpdatedRows")) 
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
						
						
						if(fieldName.equals("SECMODELMAP_SelectedRows"))
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
						if (fieldName.equals("SECMODELMAP_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("SECMODELMAP_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("SECMODELMAP_SelectedRows")) 
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
						
						if (fieldName.equals("SECMODELMAP_CountryLocale_Code")) 
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
						
						if (fieldName.equals("SECMODELMAP_Lang_Code")) 
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
						
						if (fieldName.equals("SECMODELMAP_Division_Code")) 
						{
							// set the value in sessionBean.setDivisionId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDivisionId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SECMODELMAP_ModelId")) 
						{
							// set the value in sessionBean.getFieldDetails().setModelCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setModelCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SECMODELMAP_SectionId")) 
						{
							// set the value in sessionBean.getFieldDetails().setSectionId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								Long v= new Long(value).longValue();
								sessionBean.getFieldDetails().setSectionId(v);
								v = null;
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SECMODELMAP_SSTId")) 
						{
							// set the value in sessionBean.getFieldDetails().setSstId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								Long v = new Long(value).longValue();
								sessionBean.getFieldDetails().setSstId(v);
								v = null;
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SECMODELMAP_Remarks")) 
						{
							// set the value in sessionBean.getFieldDetails().setRemarks
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setRemarks(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SECMODELMAP_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private static boolean validateFileUpload(SectionModelMappingBean sessionBean)
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
		
		if(null==sessionBean.getDivisionId() || "".equals(sessionBean.getDivisionId()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.division")));
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

	private static boolean validate(SectionModelMappingDetails fieldDetails, SectionModelMappingBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
				null==fieldDetails.getSectionId() || fieldDetails.getSectionId()<=0 || 
				null==fieldDetails.getSstId() || fieldDetails.getSstId()<=0 || 
				null==fieldDetails.getModelCode() || "".equals(fieldDetails.getModelCode()) )
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getRemarks() && !"".equals(fieldDetails.getRemarks()))
		{
			if(fieldDetails.getRemarks().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.remarks"),"200"));
			}
		}
				
		if(null!=fieldDetails.getSectionId() && fieldDetails.getSectionId()>0 && 
				 null!=fieldDetails.getModelCode() && !"".equals(fieldDetails.getModelCode()) && 
				 null!=fieldDetails.getSstId() && fieldDetails.getSstId()>0)
		{
			String token = String.valueOf(fieldDetails.getSectionId())+"<TOK>"+String.valueOf(fieldDetails.getModelCode())+"<TOK>"+String.valueOf(fieldDetails.getSstId());
			if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
					
					String matchKey = String.valueOf(details.getSectionId())+"<TOK>"+String.valueOf(details.getModelCode())+"<TOK>"+String.valueOf(details.getSstId());
					
					if(token.equals(matchKey))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.sectionmodelmapping")));
						break;
					}
					details=  null;
					matchKey= null;
				}
				token= null;
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
	
	private static boolean validateUpdate(SectionModelMappingDetails fieldDetails, SectionModelMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getSectionId() || fieldDetails.getSectionId()<=0 || 
				null==fieldDetails.getModelCode() || "".equals(fieldDetails.getModelCode()) || 
				null==fieldDetails.getSstId() || fieldDetails.getSstId()<=0 )
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getRemarks() && !"".equals(fieldDetails.getRemarks()))
		{
			if(fieldDetails.getRemarks().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.remarks")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getSectionModelMappingId() && fieldDetails.getSectionModelMappingId()>0 && 
				 null!=fieldDetails.getSectionId() && fieldDetails.getSectionId()>0 && 
				 null!=fieldDetails.getModelCode() && !"".equals(fieldDetails.getModelCode()) && 
						 null!=fieldDetails.getSstId() && fieldDetails.getSstId()>0 && 
				null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
		{
			String token = String.valueOf(fieldDetails.getSectionId())+"<TOK>"+String.valueOf(fieldDetails.getModelCode())+"<TOK>"+String.valueOf(fieldDetails.getSstId());
			for(int a=0;a<sessionBean.getMappingList().size();a++)
			{
				SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
				
				String matchKey = String.valueOf(details.getSectionId())+"<TOK>"+String.valueOf(details.getModelCode())+"<TOK>"+String.valueOf(details.getSstId());
				
				if(fieldDetails.getSectionModelMappingId()!=details.getSectionModelMappingId() && 
						token.equals(matchKey))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.sectionmodelmapping")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
					data = null;
					id = null;
					break;
				}
				matchKey=  null;
				details=  null;
			}
			token=  null;
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
	
	private static boolean validateExcelRowData(SectionModelMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getMappingListToImport() && sessionBean.getMappingListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
			{
				SectionModelMappingDetails fieldDetails = (SectionModelMappingDetails) sessionBean.getMappingListToImport().get(i);
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
				
				if(null==fieldDetails.getSectionCode() || "".equals(fieldDetails.getSectionCode()) || 
						null==fieldDetails.getModelCode() || "".equals(fieldDetails.getModelCode()) || 
						null==fieldDetails.getSstNumber() || "".equals(fieldDetails.getSstNumber()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.sectionmodelmapping")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}
				
				
				if(null!=fieldDetails.getSectionCode() && !"".equals(fieldDetails.getSectionCode()))
				{
					boolean matchFound=false;
					// IDENTIFY SST ID - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=sessionBean.getSectionList() && sessionBean.getSectionList().size()>0)
					{
						for(int a=0;a<sessionBean.getSectionList().size();a++)
						{
							SectionDetails se = (SectionDetails)sessionBean.getSectionList().get(a);
							if(String.valueOf(se.getSectionCode()).trim().toLowerCase().equals(fieldDetails.getSectionCode().trim().toLowerCase()))
							{
								fieldDetails.setSectionId(se.getSectionId());
								matchFound = true;
								break;
							}
							se = null;
						}
					}
					if(matchFound==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.section")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getModelCode() && !"".equals(fieldDetails.getModelCode()))
				{
					boolean matchFound=false;
					// IDENTIFY SST ID - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=sessionBean.getModelList() && sessionBean.getModelList().size()>0)
					{
						for(int a=0;a<sessionBean.getModelList().size();a++)
						{
							CarlineDetails de = (CarlineDetails)sessionBean.getModelList().get(a);
							if(de.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getModelCode().trim().toLowerCase()))
							{
								/*
								 * UPDATE CARLINE CODE FOR ACTUAL VALUE FROM LIST, SINCE MATCHED WITH EQUALS IGNORE CASE
								 * ALSO ADD MODEL NAME
								 */
								fieldDetails.setModelCode(de.getCarlineCode());
								fieldDetails.setModelName(de.getCarlineNameEng());
								matchFound = true;
								break;
							}
							de = null;
						}
					}
					if(matchFound==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.vehicletype")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getSstNumber() && !"".equals(fieldDetails.getSstNumber()))
				{
					boolean matchFound=false;
					// IDENTIFY SST ID - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=sessionBean.getSstList() && sessionBean.getSstList().size()>0)
					{
						for(int a=0;a<sessionBean.getSstList().size();a++)
						{
							SSTDetails de = (SSTDetails)sessionBean.getSstList().get(a);
							if(de.getSstNumber().trim().toLowerCase().equals(fieldDetails.getSstNumber().trim().toLowerCase()))
							{
								fieldDetails.setSstId(de.getSstId());
								matchFound = true;
								break;
							}
							de = null;
						}
					}
					if(matchFound==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.sstnumber")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getRemarks() && !"".equals(fieldDetails.getRemarks()))
				{
					if(fieldDetails.getRemarks().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.remarks")+",200,"+String.valueOf(rowNo);
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
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sectionmodelmapping")));
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

	private static void decideErrorDisplay(SectionModelMappingBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_SECTIONMODELMAP_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "validateExcelRowData()", e);
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
		
	private static void saveSectionModelMappingDetails(SectionModelMappingBean sessionBean)
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
				if(null!=sessionBean.getModelList() && sessionBean.getModelList().size()>0)
				{
					for(int a=0;a<sessionBean.getModelList().size();a++)
					{
						CarlineDetails de = (CarlineDetails)sessionBean.getModelList().get(a);
						if(de.getCarlineCode().trim().toLowerCase().equals(sessionBean.getFieldDetails().getModelCode().trim().toLowerCase()))
						{
							sessionBean.getFieldDetails().setModelName(de.getCarlineNameEng());
							break;
						}
						de = null;
					}
				}
				boolean bool = SectionModelMappingDAO.saveSectionModelMappingDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveSectionModelMappingDetails :: Section Model Mapping Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.sectionmodelmapping")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setMappingList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getSectionModelMappingList(sessionBean);
				}
				else
				{
					logger.info("saveSectionModelMappingDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.sectionmodelmapping")));
				}
			}
			else
			{
				logger.info("saveSectionModelMappingDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "saveSectionModelMappingDetails()", e);
		}
	}
	
	private static void editSectionModelMappingDetails(HttpServletRequest request, SectionModelMappingBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getMappingList().size();a++)
							{
								SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getSectionModelMappingId())))
								{
									logger.info("editSectionModelMappingDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
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
				logger.info("editSectionModelMappingDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "editSectionModelMappingDetails()", e);
		}
	}

	private static void deleteSectionModelMappingDetails(HttpServletRequest request, SectionModelMappingBean sessionBean)
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
					boolean bool = SectionModelMappingDAO.deleteSectionModelMappingDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.sectionmodelmapping")));
						/*
						 * call getSectionModelMappingList
						 */
						getSectionModelMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.sectionmodelmapping")));
					}
				}
			}
			else
			{
				logger.info("deleteSectionModelMappingDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "deleteSectionModelMappingDetails()", e);
		}
	}

	private static void activeSectionModelMappingDetails(HttpServletRequest request, SectionModelMappingBean sessionBean)
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
					boolean bool = SectionModelMappingDAO.activeSectionModelMappingDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.sectionmodelmapping")));
						/*
						 * call getMissionBookList
						 */
						getSectionModelMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.sectionmodelmapping")));
					}
				}
			}
			else
			{
				logger.info("activeSectionModelMappingDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "activeSectionModelMappingDetails()", e);
		}
	}
	
	private static void updateSectionModelMappingDetails(HttpServletRequest request, SectionModelMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<SectionModelMappingDetails> updateDataList = new ArrayList<SectionModelMappingDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
					{

						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setRemarks("");
								details.setSectionId(null);
								details.setModelCode("");
								details.setFlag("");
								details.setSstId(null);

								/*
								 * fetch the values from request
								 * and set in mappingList
								 */
								String sectionId="SECMODELMAP_List_SectionId"+String.valueOf(details.getSectionModelMappingId());
								String modelId="SECMODELMAP_List_ModelId"+String.valueOf(details.getSectionModelMappingId());
								String remarks="SECMODELMAP_List_Remarks"+String.valueOf(details.getSectionModelMappingId());
								String flag="SECMODELMAP_List_Flag_"+String.valueOf(details.getSectionModelMappingId());
								String sstId="SECMODELMAP_List_SSTId"+String.valueOf(details.getSectionModelMappingId());
								

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(remarks))
										{
											details.setRemarks(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
											{
												details.setRemarks(details.getRemarks().trim());
											}
										}
										else if(key.equals(sectionId))
										{
											String tok = token.substring(token.indexOf("<MDM_TS>")+8, token.length());
											if(null!=tok && !"".equals(tok))
											{
												details.setSectionId(new Long(tok).longValue());
											}
											tok = null;
										}
										else if(key.equals(modelId))
										{
											String tok = token.substring(token.indexOf("<MDM_TS>")+8, token.length());
											if(null!=tok && !"".equals(tok))
											{
												details.setModelCode(tok);
											}
											tok = null;
										}
										else if(key.equals(sstId))
										{
											String val = token.substring(token.indexOf("<MDM_TS>")+8, token.length());
											if(null!=val && !"".equals(val) && !"null".equals(val.trim().toLowerCase()))
											{
												details.setSstId(new Long(val).longValue());
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
								sectionId=null;
								modelId  =null;
								remarks = null;
								flag= null;
								sstId= null;
							}
						}

						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(i);
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
							logger.info("updateSectionModelMappingDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
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
								Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "updateSectionModelMappingDetails()", e);
							}
							for(int a=0;a<updateDataList.size();a++)
							{
								SectionModelMappingDetails details = (SectionModelMappingDetails)updateDataList.get(a);
								if(null!=sessionBean.getModelList() && sessionBean.getModelList().size()>0)
								{
									for(int b=0;b<sessionBean.getModelList().size();b++)
									{
										CarlineDetails de = (CarlineDetails)sessionBean.getModelList().get(b);
										if(de.getCarlineCode().trim().toLowerCase().equals(details.getModelCode().trim().toLowerCase()))
										{
											details.setModelName(de.getCarlineNameEng());
											break;
										}
										de = null;
									}
								}
								boolean updateFlag = SectionModelMappingDAO.updateSectionModelMappingDetails(details, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateSectionModelMappingDetails :: Division Model Mapping Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateSectionModelMappingDetails :: Failed to Update Section Model Mapping Details for Row No :: >  "+ details.getSrNo());
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
								Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "updateSectionModelMappingDetails()", e);
							}
							conn = null;

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.sectionmodelmapping"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.sectionmodelmapping"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateSectionModelMappingDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setMappingList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getSectionModelMappingList
								 */
								getSectionModelMappingList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateSectionModelMappingDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getMappingList().size();a++)
										{
											SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "updateSectionModelMappingDetails()", e);
		}
	}
	
	private static void exportSectionModelMappingDetails(HttpServletRequest request, SectionModelMappingBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<SectionModelMappingDetails> exportDataList = new ArrayList<SectionModelMappingDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getMappingList().size();a++)
							{
								SectionModelMappingDetails details = (SectionModelMappingDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getSectionModelMappingId())))
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
					writeSectionModelMappingExcel(exportDataList, sessionBean);
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
					logger.info("exportSectionModelMappingDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportSectionModelMappingDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "exportSectionModelMappingDetails()", e);
		}
	}

	private static void readExcelData(byte[] data, SectionModelMappingBean sessionBean, String extension)
	{
		sessionBean.setMappingListToImport(new ArrayList<SectionModelMappingDetails>());
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
						SectionModelMappingDetails details = new SectionModelMappingDetails();

						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setModelCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSectionCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSstNumber(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setRemarks(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						/*
						 * add details to mappingList for import
						 */
						if(null==sessionBean.getMappingListToImport() || sessionBean.getMappingListToImport().size()<=0)
						{
							sessionBean.setMappingListToImport(new ArrayList<SectionModelMappingDetails>());
						}

						sessionBean.getMappingListToImport().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsWorkBook=  null;
				xlsSheet  = null;
				rowIterator = null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "readExcelData()", e);
		}
	}

	private static void writeSectionModelMappingExcel(ArrayList<SectionModelMappingDetails> mappingList, SectionModelMappingBean sessionBean)
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
				 * So the final Name will be - US_EN-US_ND_SECTIONMODELMAP_DDMMYYYY_HHMMSS.XSLX
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
				// add DIVISON ID
				if(null!=sessionBean.getDivisionId() && !"".equals(sessionBean.getDivisionId()))
				{
					if(null!=name && !"".equals(name))
					{
						name = name.trim()+"_";
					}
					name = name.trim()+sessionBean.getDivisionId().trim().toUpperCase();
				}
				
				if(null!=name && !"".equals(name))
				{
					name = name.trim()+"_";
				}
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_SECTIONMODELMAP_NAME");
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
				carlineCodeCell.setCellValue("VEHICLE TYPE CODE");
				Cell sectionCell = headerRow.createCell(1);
				sectionCell.setCellValue("SECTION CODE");
				Cell sstNumberCell = headerRow.createCell(2);
				sstNumberCell.setCellValue("SST NUMBER");
				Cell remarksCell = headerRow.createCell(3);
				remarksCell.setCellValue("REMARKS");
				
				int rowCount=0;
				if(null!=mappingList && mappingList.size()>0)
				{
					for(int i=0;i<mappingList.size();i++)
					{
						SectionModelMappingDetails details = (SectionModelMappingDetails)mappingList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						
						if(null!=details.getModelCode() && !"".equals(details.getModelCode()))
						{
							cell0.setCellValue(details.getModelCode());
						}
						if(null!=details.getSectionCode() && !"".equals(details.getSectionCode()))
						{
							cell1.setCellValue(details.getSectionCode());
						}
						if(null!=details.getSstNumberDetails() &&  null!=details.getSstNumberDetails().getSstNumber() && !"".equals(details.getSstNumberDetails().getSstNumber()))
						{
							cell2.setCellValue(details.getSstNumberDetails().getSstNumber());
						}
						if(null!=details.getRemarks() && !"".equals(details.getRemarks()))
						{
							cell3.setCellValue(details.getRemarks());
						}
						
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					sectionCell =  null;
					remarksCell= null;
					carlineCodeCell = null;
					sstNumberCell = null;
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "writeSectionModelMappingExcel()", e);
		}
	}

	private static void executeExcelOperation(SectionModelMappingBean sessionBean, byte[] data, String extension)
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
				ArrayList<SectionModelMappingDetails> listToSave = new ArrayList<SectionModelMappingDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<SectionModelMappingDetails> listToDelete = new ArrayList<SectionModelMappingDetails>();
				for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
				{
					SectionModelMappingDetails fieldDetails = (SectionModelMappingDetails)sessionBean.getMappingListToImport().get(i);
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
							SectionModelMappingDetails existingDetails = (SectionModelMappingDetails)listToSave.get(j);
							/*
							 * IDENTIFY ON THE BASIS OF 
							 * SORT ID
							 * DIVISION NAME
							 * MODEL NAME
							 * ESI MODEL NAME
							 */
							String tok = String.valueOf(fieldDetails.getSectionId())+"<TOK>"+String.valueOf(fieldDetails.getModelCode())+"<TOK>"+String.valueOf(fieldDetails.getSstId());
							if(null!=fieldDetails.getRemarks() && !"".equals(fieldDetails.getRemarks()))
							{
								tok = tok +"<TOK>"+fieldDetails.getRemarks();
							}
							
							String matchKey = String.valueOf(existingDetails.getSectionId())+"<TOK>"+String.valueOf(existingDetails.getModelCode())+"<TOK>"+String.valueOf(existingDetails.getSstId());
							if(null!=existingDetails.getRemarks() && !"".equals(existingDetails.getRemarks()))
							{
								matchKey = matchKey +"<TOK>"+existingDetails.getRemarks();
							}
							
							if(tok.equals(matchKey))
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
							tok  =null;
							matchKey= null;
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.sectionmodelmapping"), String.valueOf(rows[a])));
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
						Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
					}
					for(int i=0;i<listToSave.size();i++)
					{
						SectionModelMappingDetails fieldDetails = (SectionModelMappingDetails)listToSave.get(i);
						
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						boolean saveDivisionData = SectionModelMappingDAO.importSectionModelMappingDetails(fieldDetails, conn, closeConnection);
						
						if(saveDivisionData == true)
						{
							logger.info("executeExcelOperation() :: Section Model Mapping Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("executeExcelOperation() :: Failed to Import Section Model Mapping Data for Line No {"+(i+1+1)+"}.");
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
						Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
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
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.division"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated vin list
						 */
						getSectionModelMappingList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.sectionmodelmapping"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.division"), errorLineNo));
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sectionmodelmapping")));
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
							SectionModelMappingDetails deleteDetails = (SectionModelMappingDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = SectionModelMappingDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
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
						Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
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
							deleted = SectionModelMappingDAO.deleteSectionModelMappingDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getSectionModelMappingList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.sectionmodelmapping"), String.valueOf(deleteErrorRows[a])));
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "executeExcelOperation()", e);
		}
	}

	private static void getSectionModelMappingList(SectionModelMappingBean sessionBean)
	{
		try
		{
			sessionBean.setMappingList(new ArrayList<SectionModelMappingDetails>());
			if(null!=sessionBean.getDivisionId() && !"".equals(sessionBean.getDivisionId()))
			{
				ArrayList<SectionModelMappingDetails> list = new ArrayList<SectionModelMappingDetails>();
				list = SectionModelMappingDAO.getSectionModelMappingDetailsList(String.valueOf(sessionBean.getDivisionId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						SectionModelMappingDetails details = (SectionModelMappingDetails)list.get(i);
						/*
						 * update Model Name, in case if Original Name updated in DIVISION MODEL MAPPING TABLE
						 */
						if(null!=details.getModelCode() && !"".equals(details.getModelCode()) && 
								null!=sessionBean.getModelList() && sessionBean.getModelList().size()>0)
						{
							for(int a=0;a<sessionBean.getModelList().size();a++)
							{
								CarlineDetails cDetails = (CarlineDetails)sessionBean.getModelList().get(a);
								if(cDetails.getCarlineCode().trim().toLowerCase().equals(details.getModelCode().trim().toLowerCase()))
								{
									details.setModelName(cDetails.getCarlineNameEng());
									break;
								}
								cDetails=  null;
							}
						}
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
					sessionBean.setMappingList(list);
				}
				list = null;
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "getSectionModelMappingList()", e);
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
	
	private void performAccessCheck(SectionModelMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SectionModelMapping.class.getName(), "performAccessCheck()", e);
		}
	}

}

