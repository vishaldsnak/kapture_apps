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
import com.mazda.gms3.mdm.dao.MCSIVinDAO;
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
import com.mazda.gms3.sst.bean.SSTMDMVINMappingBean;
import com.mazda.gms3.sst.dao.SSTMDMVINMappingDAO;
import com.mazda.gms3.sst.utils.SSTUtils;
import com.mazda.gms3.sst.vo.SSTMDMVinDetails;

/**
 * Servlet implementation class DivisionMaster
 */
public class SSTMDMVinMapping extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static Logger logger = LogManager.getLogger(SSTMDMVinMapping.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	static String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_SST_MDM_VIN_MAPPING;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SSTMDMVinMapping() {
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
		SSTMDMVINMappingBean sessionBean = getSessionBean(request);
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
			 * perform access check
			 */
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
			sessionBean.setMappingListToImport(new ArrayList<SSTMDMVinDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setWmiCodeList(null);
			sessionBean.setCarlineCodeList(null);
			sessionBean.setVdsCodeList(null);
			sessionBean.setVisStartList(null);
			sessionBean.setVisEndList(null);
			
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sstmdmvinmapping.jsp");
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
		SSTMDMVINMappingBean sessionBean = getSessionBean(request);
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
			 * perform access check
			 */
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
					
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setCarlineComboId(null);
						sessionBean.getFieldDetails().setCarlineCode(null);
						sessionBean.getFieldDetails().setCarlineNameEng(null);
						sessionBean.getFieldDetails().setCarlineNameReg(null);
						sessionBean.getFieldDetails().setWmiCode(null);
						sessionBean.getFieldDetails().setVdsCode(null);
						sessionBean.getFieldDetails().setVisStart(null);
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					
					getCarlineCodeList(sessionBean);
					getWmiCodeList(sessionBean);
					getVDSCodeList(sessionBean);
					getVISStartList(sessionBean);
					getVISEndList(sessionBean);
					
					/*
					 * call function to load abbreviationList
					 */
					getMappingList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * and Division Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setCarlineComboId(null);
						sessionBean.getFieldDetails().setCarlineCode(null);
						sessionBean.getFieldDetails().setCarlineNameEng(null);
						sessionBean.getFieldDetails().setCarlineNameReg(null);
						sessionBean.getFieldDetails().setWmiCode(null);
						sessionBean.getFieldDetails().setVdsCode(null);
						sessionBean.getFieldDetails().setVisStart(null);
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					
					getCarlineCodeList(sessionBean);
					getWmiCodeList(sessionBean);
					getVDSCodeList(sessionBean);
					getVISStartList(sessionBean);
					getVISEndList(sessionBean);
					
					
					getMappingList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("WMI_SELECTION"))
				{
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setCarlineComboId(null);
						sessionBean.getFieldDetails().setCarlineCode(null);
						sessionBean.getFieldDetails().setCarlineNameEng(null);
						sessionBean.getFieldDetails().setCarlineNameReg(null);
						sessionBean.getFieldDetails().setVdsCode(null);
						sessionBean.getFieldDetails().setVisStart(null);
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					getCarlineCodeList(sessionBean);
					getVDSCodeList(sessionBean);
					getVISStartList(sessionBean);
					getVISEndList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("CARLINE_SELECTION"))
				{
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setVdsCode(null);
						sessionBean.getFieldDetails().setVisStart(null);
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					getVDSCodeList(sessionBean);
					getVISStartList(sessionBean);
					getVISEndList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VDS_SELECTION"))
				{
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setVisStart(null);
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					getVISStartList(sessionBean);
					getVISEndList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VISSTART_SELECTION"))
				{
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setVisEnd(null);
					}
					
					getVISEndList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_SSTMDMVINMAPPING"))
				{
					/*
					 * Save Operation called
					 */
					saveSSTMDMVinDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_SSTMDMVINMAPPING"))
				{
					/*
					 * Edit Operation called
					 */
					editSSTMDMVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SSTMDMVINMAPPING"))
				{
					/*
					 * Delete Operation called
					 */
					deleteSSTMDMVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_SSTMDMVINMAPPING"))
				{
					/*
					 * Active Operation called
					 */
					activeSSTMDMVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_SSTMDMVINMAPPING"))
				{
					/*
					 * Update Operation called
					 */
					updateSSTMDMVinDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_SSTMDMVINMAPPING"))
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
					 * call getMappingList
					 */
					getMappingList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_SSTMDMVINMAPPING"))
				{
					/*
					 * Export Division Operation
					 */
					exportSSTMDMVinDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sst/sstmdmvinmapping.jsp");
				rs.forward(request, response);
			}
		}
	}

	private SSTMDMVINMappingBean getSessionBean(HttpServletRequest request) 
	{
		SSTMDMVINMappingBean sessionBean = null;
		if (null != request.getSession().getAttribute("sstMdmVinMappingBean") && !"".equals(request.getSession().getAttribute("sstMdmVinMappingBean"))) 
		{
			sessionBean = (SSTMDMVINMappingBean) request.getSession().getAttribute("sstMdmVinMappingBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SSTMDMVINMappingBean();
			request.getSession().setAttribute("sstMdmVinMappingBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(SSTMDMVINMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(SSTMDMVINMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getLanguageList()", e);
		}
	}
	
	private static void getFlagList(SSTMDMVINMappingBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
		
	private static void getWmiCodeList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setWmiCodeList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && null!=sessionBean.getManualLanguageId() 
					&& !"".equals(sessionBean.getManualLanguageId()))
			{
				// NO CARLINE ENG NAME
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getWMIList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setWmiCodeList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getWmiCodeList()", e);
		}
	}
	
	private static void getCarlineCodeList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setCarlineCodeList(new ArrayList<CarlineDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) 
					&& null!=sessionBean.getFieldDetails() && null!=sessionBean.getFieldDetails().getWmiCode() 
					&& !"".equals(sessionBean.getFieldDetails().getWmiCode()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				/*
				 * IDENTIFY & FETCH ON LOCALE CODE
				 */
//				list = CarlineDAO.getCarlineDetailsListForComboFORVIN(sessionBean.getManualLanguageId());
				list = MCSIVinDAO.getModelsList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), sessionBean.getFieldDetails().getWmiCode(), null, null);
				if(null!=list && list.size()>0)
				{
					sessionBean.setCarlineCodeList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getCarlineCodeList()", e);
		}
	}
	
	private static ArrayList<CarlineDetails> getCarlineCodeListForValidations(String wmiCode, SSTMDMVINMappingBean sessionBean, Connection conn, String closeConnection)
	{
		ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
		try
		{
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) 
					&& null!=sessionBean.getManualLanguageId()  && !"".equals(sessionBean.getManualLanguageId()) && null!=wmiCode && !"".equals(wmiCode))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				list = MCSIVinDAO.getModelsList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), wmiCode, conn, closeConnection);
				if(null!=list && list.size()>0)
				{
					carlineList=  list;
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getCarlineCodeListForValidations()", e);
		}
		return carlineList;
	}
	
	private static ArrayList<SelectItemDetails> getVDSCodeListForValidation(String carlineCode, String carlineNameEng, String wmiCode, SSTMDMVINMappingBean sessionBean, Connection conn, String closeConnection)
	{
		ArrayList<SelectItemDetails> vdsList = new ArrayList<SelectItemDetails>();
		try
		{
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && 
					null!=sessionBean.getManualLanguageId()  && !"".equals(sessionBean.getManualLanguageId())  && null!=carlineCode  
					&& !"".equals(carlineCode) && null!=carlineNameEng && !"".equals(carlineNameEng) && null!=wmiCode && !"".equals(wmiCode))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVDSList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), wmiCode, carlineCode, carlineNameEng, conn, closeConnection);
				if(null!=list && list.size()>0)
				{
					vdsList=  list;
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVDSCodeListForValidation()", e);
		}
		return vdsList;
	}
	
	private static ArrayList<SelectItemDetails> getVISStartListForValidation(String carlineCode, String carlineNameEng, String wmiCode,String vdsCode, SSTMDMVINMappingBean sessionBean, Connection conn, String closeConnection)
	{
		ArrayList<SelectItemDetails> visStartList = new ArrayList<SelectItemDetails>();
		try
		{
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && 
					null!=sessionBean.getManualLanguageId()  && !"".equals(sessionBean.getManualLanguageId())  && null!=carlineCode  
					&& !"".equals(carlineCode) && null!=carlineNameEng && !"".equals(carlineNameEng) && null!=wmiCode && !"".equals(wmiCode)
					&& null!=vdsCode && !"".equals(vdsCode))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVISStartRangeList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), wmiCode, carlineCode, carlineNameEng, vdsCode, conn, closeConnection);
				if(null!=list && list.size()>0)
				{
					visStartList=  list;
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVISStartListForValidation()", e);
		}
		return visStartList;
	}
	
	private static ArrayList<SelectItemDetails> getVISEndRangeListForValidation(String carlineCode, String carlineNameEng, String wmiCode,String vdsCode, String visStartRange,SSTMDMVINMappingBean sessionBean, Connection conn, String closeConnection)
	{
		ArrayList<SelectItemDetails> visEndList = new ArrayList<SelectItemDetails>();
		try
		{
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && 
					null!=sessionBean.getManualLanguageId()  && !"".equals(sessionBean.getManualLanguageId())  && null!=carlineCode  
					&& !"".equals(carlineCode) && null!=carlineNameEng && !"".equals(carlineNameEng) && null!=wmiCode && !"".equals(wmiCode)
					&& null!=vdsCode && !"".equals(vdsCode) && null!=visStartRange && !"".equals(visStartRange))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVISEndRangeList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), wmiCode, carlineCode, carlineNameEng, vdsCode, visStartRange, conn, closeConnection);
				if(null!=list && list.size()>0)
				{
					visEndList=  list;
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVISEndRangeListForValidation()", e);
		}
		return visEndList;
	}
	
	private static void getVDSCodeList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setVdsCodeList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && null!=sessionBean.getManualLanguageId() 
					&& !"".equals(sessionBean.getManualLanguageId()) && null!=sessionBean.getFieldDetails() && null!=sessionBean.getFieldDetails().getCarlineCode() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineCode()) && null!=sessionBean.getFieldDetails().getCarlineNameEng() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineNameEng()) && null!=sessionBean.getFieldDetails().getWmiCode() 
					&& !"".equals(sessionBean.getFieldDetails().getWmiCode()))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVDSList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), sessionBean.getFieldDetails().getWmiCode(), 
						sessionBean.getFieldDetails().getCarlineCode(), sessionBean.getFieldDetails().getCarlineNameEng(), null,null);
				if(null!=list && list.size()>0)
				{
					sessionBean.setVdsCodeList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVDSCodeList()", e);
		}
	}
	
	private static void getVISStartList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setVisStartList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && null!=sessionBean.getManualLanguageId() 
					&& !"".equals(sessionBean.getManualLanguageId()) && null!=sessionBean.getFieldDetails() && null!=sessionBean.getFieldDetails().getCarlineCode() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineCode()) && null!=sessionBean.getFieldDetails().getCarlineNameEng() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineNameEng()) && null!=sessionBean.getFieldDetails().getWmiCode() && 
					!"".equals(sessionBean.getFieldDetails().getWmiCode()) && null!=sessionBean.getFieldDetails().getVdsCode() 
					&& !"".equals(sessionBean.getFieldDetails().getVdsCode()))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVISStartRangeList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), sessionBean.getFieldDetails().getWmiCode(), 
						sessionBean.getFieldDetails().getCarlineCode(), sessionBean.getFieldDetails().getCarlineNameEng(), sessionBean.getFieldDetails().getVdsCode(), null,null);
				
				if(null!=list && list.size()>0)
				{
					sessionBean.setVisStartList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVISStartList()", e);
		}
	}
	
	private static void getVISEndList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setVisEndList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && null!=sessionBean.getManualLanguageId() 
					&& !"".equals(sessionBean.getManualLanguageId()) && null!=sessionBean.getFieldDetails() && null!=sessionBean.getFieldDetails().getCarlineCode() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineCode()) && null!=sessionBean.getFieldDetails().getCarlineNameEng() 
					&& !"".equals(sessionBean.getFieldDetails().getCarlineNameEng()) && null!=sessionBean.getFieldDetails().getWmiCode() && 
					!"".equals(sessionBean.getFieldDetails().getWmiCode()) && null!=sessionBean.getFieldDetails().getVdsCode() && !"".equals(sessionBean.getFieldDetails().getVdsCode()) 
					&& null!=sessionBean.getFieldDetails().getVisStart() && !"".equals(sessionBean.getFieldDetails().getVisStart()))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				list = MCSIVinDAO.getVISEndRangeList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), sessionBean.getFieldDetails().getWmiCode(), 
						sessionBean.getFieldDetails().getCarlineCode(), sessionBean.getFieldDetails().getCarlineNameEng(),sessionBean.getFieldDetails().getVdsCode(),
						sessionBean.getFieldDetails().getVisStart(), null,null);
				if(null!=list && list.size()>0)
				{
					sessionBean.setVisEndList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getVISEndList()", e);
		}
	}
	
	private static void readParamsFromRequest(SSTMDMVINMappingBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new SSTMDMVinDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setMappingListToImport(null);
			
			
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
			while (iterator.hasNext()) 
			{
				FileItem fileItem = iterator.next();
				if (fileItem.isFormField())
				{
					String fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						if (fieldName.equals("SSTMDMVIN_UpdatedRows")) 
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
						
						
						if(fieldName.equals("SSTMDMVIN_SelectedRows"))
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
						if (fieldName.equals("SSTMDMVIN_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("SSTMDMVIN_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("SSTMDMVIN_SelectedRows")) 
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
						
						if (fieldName.equals("SSTMDMVIN_CountryLocale_Code")) 
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
						
						if (fieldName.equals("SSTMDMVIN_Lang_Code")) 
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
						
						if (fieldName.equals("SSTMDMVIN_CarlineCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setCarlineCode / setCarlineNameEng / setCarlineNameReg
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setCarlineComboId(value);
								if(null!=sessionBean.getCarlineCodeList() && sessionBean.getCarlineCodeList().size()>0)
								{
									for(CarlineDetails cDetails: sessionBean.getCarlineCodeList())
									{
										if(cDetails.getCarlineIdForCombo().trim().toLowerCase().equals(value.trim().toLowerCase()))
										{
											sessionBean.getFieldDetails().setCarlineCode(cDetails.getCarlineCode());
											sessionBean.getFieldDetails().setCarlineNameEng(cDetails.getCarlineNameEng());
											sessionBean.getFieldDetails().setCarlineNameReg(cDetails.getCarlineNameReg());
											break;
										}
										cDetails= null;
									}
								}
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_WmiCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setWmiCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setWmiCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_VdsCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setVdsCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setVdsCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_VisStartRange")) 
						{
							// set the value in sessionBean.getFieldDetails().setVisStart
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setVisStart(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_VisEndRange")) 
						{
							// set the value in sessionBean.getFieldDetails().setVisEnd
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setVisEnd(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_SSTCarlineCode")) 
						{
							// set the value in sessionBean.getFieldDetails().setSstCarlineCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSstCarlineCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SSTMDMVIN_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private static boolean validateFileUpload(SSTMDMVINMappingBean sessionBean)
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

	private static boolean validate(SSTMDMVinDetails fieldDetails, SSTMDMVINMappingBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
				null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
				null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
				null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) || 
				null==fieldDetails.getVisStart() || "".equals(fieldDetails.getVisStart()) || 
				null==fieldDetails.getVisEnd() || "".equals(fieldDetails.getVisEnd()) || 
				null==fieldDetails.getSstCarlineCode() || "".equals(fieldDetails.getSstCarlineCode()))
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getSstCarlineCode() && !"".equals(fieldDetails.getSstCarlineCode()))
		{
			if(fieldDetails.getSstCarlineCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.sstcarlinecode"),"20"));
			}
		}
		
		
		if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) &&
				null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
				null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
				null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) &&
				null!=fieldDetails.getVisStart() && !"".equals(fieldDetails.getVisStart()) &&
				null!=fieldDetails.getVisEnd() && !"".equals(fieldDetails.getVisEnd()) &&
				null!=fieldDetails.getSstCarlineCode() && !"".equals(fieldDetails.getSstCarlineCode()))
		{
			if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
					if(details.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase()) 
						&& details.getCarlineNameEng().trim().toLowerCase().equals(fieldDetails.getCarlineNameEng().trim().toLowerCase())
						&& details.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase())
						&& details.getVdsCode().trim().toLowerCase().equals(fieldDetails.getVdsCode().trim().toLowerCase())
						&& details.getVisStart().trim().toLowerCase().equals(fieldDetails.getVisStart().trim().toLowerCase())
						&& details.getVisEnd().trim().toLowerCase().equals(fieldDetails.getVisEnd().trim().toLowerCase())
						&& details.getSstCarlineCode().trim().toLowerCase().equals(fieldDetails.getSstCarlineCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.sstmdmvinmapping")));
						break;
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
	
	private static boolean validateUpdate(SSTMDMVinDetails fieldDetails, SSTMDMVINMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
				null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) ||
				null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
				null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) || 
				null==fieldDetails.getVisStart() || "".equals(fieldDetails.getVisStart()) || 
				null==fieldDetails.getVisEnd() || "".equals(fieldDetails.getVisEnd()) || 
				null==fieldDetails.getSstCarlineCode() || "".equals(fieldDetails.getSstCarlineCode()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getSstCarlineCode() && !"".equals(fieldDetails.getSstCarlineCode()))
		{
			if(fieldDetails.getSstCarlineCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.sstcarlinecode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) &&
				null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
				null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
				null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) &&
				null!=fieldDetails.getVisStart() && !"".equals(fieldDetails.getVisStart()) &&
				null!=fieldDetails.getVisEnd() && !"".equals(fieldDetails.getVisEnd()) &&
				null!=fieldDetails.getSstCarlineCode() && !"".equals(fieldDetails.getSstCarlineCode()))
		{
			if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
					if(details.getSstMdmVinMappingId()!=fieldDetails.getSstMdmVinMappingId() && 
						details.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase()) 
						&& details.getCarlineNameEng().trim().toLowerCase().equals(fieldDetails.getCarlineNameEng().trim().toLowerCase())
						&& details.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase())
						&& details.getVdsCode().trim().toLowerCase().equals(fieldDetails.getVdsCode().trim().toLowerCase())
						&& details.getVisStart().trim().toLowerCase().equals(fieldDetails.getVisStart().trim().toLowerCase())
						&& details.getVisEnd().trim().toLowerCase().equals(fieldDetails.getVisEnd().trim().toLowerCase())
						&& details.getSstCarlineCode().trim().toLowerCase().equals(fieldDetails.getSstCarlineCode().trim().toLowerCase()))
					{

						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sstmdmvinmapping")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
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
	
	private static boolean validateExcelRowData(SSTMDMVINMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getMappingListToImport() && sessionBean.getMappingListToImport().size()>0)
		{
			Connection conn = null;
			String closeConnection="N";
			try
			{
				conn = DBConnectionHelper.getConnection();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "validateExcelRowData()", e);
			}
			
			for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
			{
				SSTMDMVinDetails fieldDetails = (SSTMDMVinDetails) sessionBean.getMappingListToImport().get(i);
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
				
				if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
						null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) ||
						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
						null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) || 
						null==fieldDetails.getVisStart() || "".equals(fieldDetails.getVisStart()) || 
						null==fieldDetails.getVisEnd() || "".equals(fieldDetails.getVisEnd()) || 
						null==fieldDetails.getSstCarlineCode() || "".equals(fieldDetails.getSstCarlineCode()) )
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.sstmdmvinmapping")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}
				
				if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
				{
					boolean matchFound=false;
					// IDENTIFY WMI CODE - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=sessionBean.getWmiCodeList() && sessionBean.getWmiCodeList().size()>0)
					{
						for(int a=0;a<sessionBean.getWmiCodeList().size();a++)
						{
							SelectItemDetails de = (SelectItemDetails)sessionBean.getWmiCodeList().get(a);
							if(de.getValue().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase()))
							{
								fieldDetails.setWmiCode(de.getValue());
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
						String data = msgProps.getProperty("label.wmicode")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
						&& null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
						null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
				{
					// call function to getModelsList 
					ArrayList<CarlineDetails> carlineList =  getCarlineCodeListForValidations(fieldDetails.getWmiCode(), sessionBean, conn, closeConnection);
					String keyToCheck = fieldDetails.getCarlineNameEng().trim().toUpperCase()+"_"+fieldDetails.getCarlineCode().trim().toUpperCase();
					boolean matchFound=false;
					// IDENTIFY CARLINE CODE - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=carlineList && carlineList.size()>0)
					{
						for(int a=0;a<carlineList.size();a++)
						{
							CarlineDetails de = (CarlineDetails)carlineList.get(a);
							if(de.getCarlineIdForCombo().trim().toLowerCase().equals(keyToCheck.trim().toLowerCase()))
							{
								fieldDetails.setCarlineCode(de.getCarlineCode());
								fieldDetails.setCarlineNameEng(de.getCarlineNameEng());
								fieldDetails.setCarlineNameReg(de.getCarlineNameReg());
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
						String data = msgProps.getProperty("label.dash.CARLINE")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					carlineList  = null;
				}
				
				if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
						&& null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
						null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) && 
						null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()))
				{
					// call function to getVDS CODE
					ArrayList<SelectItemDetails> vdsList =  getVDSCodeListForValidation(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), fieldDetails.getWmiCode(), sessionBean, conn, closeConnection);
					
					boolean matchFound=false;
					// IDENTIFY VDS CODE - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=vdsList && vdsList.size()>0)
					{
						for(int a=0;a<vdsList.size();a++)
						{
							SelectItemDetails de = (SelectItemDetails)vdsList.get(a);
							if(de.getValue().trim().toLowerCase().equals(fieldDetails.getVdsCode().trim().toLowerCase()))
							{
								fieldDetails.setVdsCode(de.getValue());
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
						String data = msgProps.getProperty("label.vdscode")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					vdsList = null;
				}
				
				if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
						&& null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
						null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) && 
						null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) && 
						null!=fieldDetails.getVisStart() && !"".equals(fieldDetails.getVisStart()))
				{
					// call function to getVISStart CODE
					ArrayList<SelectItemDetails> visStartList =  getVISStartListForValidation(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), fieldDetails.getWmiCode(), fieldDetails.getVdsCode(),sessionBean, conn,closeConnection);
					
					boolean matchFound=false;
					// IDENTIFY VISSTART - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=visStartList && visStartList.size()>0)
					{
						for(int a=0;a<visStartList.size();a++)
						{
							SelectItemDetails de = (SelectItemDetails)visStartList.get(a);
							if(de.getValue().trim().toLowerCase().equals(fieldDetails.getVisStart().trim().toLowerCase()))
							{
								fieldDetails.setVisStart(de.getValue());
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
						String data = msgProps.getProperty("label.visstartrange")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					visStartList = null;
				}
				
				if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
						&& null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
						null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) && 
						null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) && 
						null!=fieldDetails.getVisStart() && !"".equals(fieldDetails.getVisStart()) && 
						null!=fieldDetails.getVisEnd() && !"".equals(fieldDetails.getVisEnd()))
				{
					// call function to getVisEnd CODE
					ArrayList<SelectItemDetails> visEndList =  getVISEndRangeListForValidation(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), fieldDetails.getWmiCode(), fieldDetails.getVdsCode(), fieldDetails.getVisStart(),sessionBean, conn, closeConnection);
					
					boolean matchFound=false;
					// IDENTIFY VISEND - IF NOT FOUND - THEN IMPROPER LINE.
					if(null!=visEndList && visEndList.size()>0)
					{
						for(int a=0;a<visEndList.size();a++)
						{
							SelectItemDetails de = (SelectItemDetails)visEndList.get(a);
							if(de.getValue().trim().toLowerCase().equals(fieldDetails.getVisEnd().trim().toLowerCase()))
							{
								fieldDetails.setVisEnd(de.getValue());
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
						String data = msgProps.getProperty("label.visendrange")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					visEndList = null;
				}
				
				if(null!=fieldDetails.getSstCarlineCode() && !"".equals(fieldDetails.getSstCarlineCode()))
				{
					if(fieldDetails.getSstCarlineCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.sstcarlinecode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
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
				Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "validateExcelRowData()", e);
			}
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sstmdmvinmapping")));
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

	private static void decideErrorDisplay(SSTMDMVINMappingBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_SSTMDMVINMAPPING_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "validateExcelRowData()", e);
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
		
	private static void saveSSTMDMVinDetails(SSTMDMVINMappingBean sessionBean)
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
				boolean bool = SSTMDMVINMappingDAO.saveSSTMDMVinDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveSSTMDMVinDetails :: Division Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.sstmdmvinmapping")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setMappingList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getMappingList(sessionBean);
				}
				else
				{
					logger.info("saveSSTMDMVinDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.sstmdmvinmapping")));
				}
			}
			else
			{
				logger.info("saveSSTMDMVinDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "saveSSTMDMVinDetails()", e);
		}
	}
	
	private static void editSSTMDMVinDetails(HttpServletRequest request, SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
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
								SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getSstMdmVinMappingId())))
								{
									logger.info("editSSTMDMVinDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
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
				logger.info("editSSTMDMVinDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "editSSTMDMVinDetails()", e);
		}
	}

	private static void deleteSSTMDMVinDetails(HttpServletRequest request, SSTMDMVINMappingBean sessionBean)
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
					boolean bool = SSTMDMVINMappingDAO.deleteSSTMDMVinDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.sstmdmvinmapping")));
						/*
						 * call getMappingList
						 */
						getMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.sstmdmvinmapping")));
					}
				}
			}
			else
			{
				logger.info("deleteSSTMDMVinDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "deleteSSTMDMVinDetails()", e);
		}
	}

	private static void activeSSTMDMVinDetails(HttpServletRequest request, SSTMDMVINMappingBean sessionBean)
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
					boolean bool = SSTMDMVINMappingDAO.activeSSTMDMVinDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.sstmdmvinmapping")));
						/*
						 * call getMissionBookList
						 */
						getMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.sstmdmvinmapping")));
					}
				}
			}
			else
			{
				logger.info("activeSSTMDMVinDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "activeSSTMDMVinDetails()", e);
		}
	}
	
	private static void updateSSTMDMVinDetails(HttpServletRequest request, SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<SSTMDMVinDetails> updateDataList = new ArrayList<SSTMDMVinDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
					{
						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setSstCarlineCode("");
								details.setFlag("");

								/*
								 * fetch the values from request
								 * and set in vinList
								 */
								String sstCarlineCode="SSTMDMVIN_List_SSTCarlineCode"+String.valueOf(details.getSstMdmVinMappingId());
								String flag="SSTMDMVIN_List_Flag_"+String.valueOf(details.getSstMdmVinMappingId());
								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(sstCarlineCode))
										{
											details.setSstCarlineCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getSstCarlineCode() && !"".equals(details.getSstCarlineCode()))
											{
												details.setSstCarlineCode(details.getSstCarlineCode().trim());
											}
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
								sstCarlineCode=null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(i);
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
							logger.info("updateSSTMDMVinDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
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
								Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "updateSSTMDMVinDetails()", e);
							}
							for(int a=0;a<updateDataList.size();a++)
							{
								SSTMDMVinDetails details = (SSTMDMVinDetails)updateDataList.get(a);
								boolean updateFlag = SSTMDMVINMappingDAO.updateSSTMDMVinDetails(details, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateSSTMDMVinDetails :: Division Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateSSTMDMVinDetails :: Failed to Update Division Details for Row No :: >  "+ details.getSrNo());
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
								Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "updateSSTMDMVinDetails()", e);
							}
							
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.sstmdmvinmapping"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.sstmdmvinmapping"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateSSTMDMVinDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setMappingList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getMappingList
								 */
								getMappingList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateSSTMDMVinDetails :: Error found in rows :: > " + errorMessage);
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
											SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "updateSSTMDMVinDetails()", e);
		}
	}
	
	private static void exportSSTMDMVinDetails(HttpServletRequest request, SSTMDMVINMappingBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<SSTMDMVinDetails> exportDataList = new ArrayList<SSTMDMVinDetails>();
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
								SSTMDMVinDetails details = (SSTMDMVinDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getSstMdmVinMappingId())))
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
					writeDivisionExcel(exportDataList, sessionBean);
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
					logger.info("exportSSTMDMVinDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportSSTMDMVinDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "exportSSTMDMVinDetails()", e);
		}
	}

	private static void readExcelData(byte[] data, SSTMDMVINMappingBean sessionBean, String extension)
	{
		sessionBean.setMappingListToImport(new ArrayList<SSTMDMVinDetails>());
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
				 * CARLINE CODE
				 * WMI CODE
				 * VDS CODE
				 * VIS START RANGE
				 * VIS END RANGE
				 * SST CARLINE CODE
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						SSTMDMVinDetails details = new SSTMDMVinDetails();

						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setCarlineNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVisStart(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(5));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVisEnd(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSstCarlineCode(String.valueOf(dataCell).trim());
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
							details.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						/*
						 * add details to vinList for import
						 */
						if(null==sessionBean.getMappingListToImport() || sessionBean.getMappingListToImport().size()<=0)
						{
							sessionBean.setMappingListToImport(new ArrayList<SSTMDMVinDetails>());
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
				xlsSheet=null;
				xlsWorkBook=null;
				rowIterator = null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "readExcelData()", e);
		}
	}

	private static void writeDivisionExcel(ArrayList<SSTMDMVinDetails> vinList, SSTMDMVINMappingBean sessionBean)
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
				 * So the final Name will be - US_EN-US_ND_SSTMDMVINMAPPING_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_SSTMDMVINMAPPING_NAME");
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
				Cell carlineNameCell = headerRow.createCell(1);
				carlineNameCell.setCellValue("CARRLINE NAME");
				Cell wmiCodeCell = headerRow.createCell(1);
				wmiCodeCell.setCellValue("WMI CODE");
				Cell vdsCodeCell = headerRow.createCell(2);
				vdsCodeCell.setCellValue("VDS CODE");
				Cell visStartCell = headerRow.createCell(3);
				visStartCell.setCellValue("VIS START RANGE");
				Cell visEndCell= headerRow.createCell(4);
				visEndCell.setCellValue("VIS END RANGE");
				Cell sstCarlineCodeCell = headerRow.createCell(5);
				sstCarlineCodeCell.setCellValue("SST CALINE CODE");
				
				int rowCount=0;
				if(null!=vinList && vinList.size()>0)
				{
					for(int i=0;i<vinList.size();i++)
					{
						SSTMDMVinDetails details = (SSTMDMVinDetails)vinList.get(i);
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
						
						if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
						{
							cell0.setCellValue(details.getCarlineCode());
						}
						if(null!=details.getCarlineNameEng() && !"".equals(details.getCarlineNameEng()))
						{
							cell1.setCellValue(details.getCarlineNameEng());
						}
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							cell2.setCellValue(details.getWmiCode());
						}
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
						{
							cell3.setCellValue(details.getVdsCode());
						}
						if(null!=details.getVisStart() && !"".equals(details.getVisStart()))
						{
							cell4.setCellValue(details.getVisStart());
						}
						if(null!=details.getVisEnd() && !"".equals(details.getVisEnd()))
						{
							cell5.setCellValue(details.getVisEnd());
						}
						if(null!=details.getSstCarlineCode() && !"".equals(details.getSstCarlineCode()))
						{
							cell6.setCellValue(details.getSstCarlineCode());
						}
						
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					carlineCodeCell=  null;
					wmiCodeCell=null;
					vdsCodeCell = null;
					visStartCell= null;
					visEndCell=  null;
					sstCarlineCodeCell=  null;
					carlineNameCell = null;
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "writeDivisionExcel()", e);
		}
	}

	private static void executeExcelOperation(SSTMDMVINMappingBean sessionBean, byte[] data, String extension)
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
				ArrayList<SSTMDMVinDetails> listToSave = new ArrayList<SSTMDMVinDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<SSTMDMVinDetails> listToDelete = new ArrayList<SSTMDMVinDetails>();
				for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
				{
					SSTMDMVinDetails fieldDetails = (SSTMDMVinDetails)sessionBean.getMappingListToImport().get(i);
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
							SSTMDMVinDetails existingDetails = (SSTMDMVinDetails)listToSave.get(j);
							/*
							 * IDENTIFY ON THE BASIS OF 
							 * SORT ID
							 * DIVISION NAME
							 */
							if(fieldDetails.getCarlineCode().equals(existingDetails.getCarlineCode()) && 
								fieldDetails.getCarlineNameEng().equals(existingDetails.getCarlineNameEng())  
							&& fieldDetails.getWmiCode().equals(existingDetails.getWmiCode()) && 
							fieldDetails.getVdsCode().equals(existingDetails.getVdsCode()) 
							&& fieldDetails.getVisStart().equals(existingDetails.getVisStart()) && 
							fieldDetails.getVisEnd().equals(existingDetails.getVisEnd()) 
							&& fieldDetails.getSstCarlineCode().equals(existingDetails.getSstCarlineCode()))
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.sstmdmvinmapping"), String.valueOf(rows[a])));
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
					Connection conn =null;
					String closeConnection="N";
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
					}
					for(int i=0;i<listToSave.size();i++)
					{
						SSTMDMVinDetails fieldDetails = (SSTMDMVinDetails)listToSave.get(i);
						
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						boolean saveDivisionData = SSTMDMVINMappingDAO.importSSTMDMVinDetails(fieldDetails, conn, closeConnection);
						
						if(saveDivisionData == true)
						{
							logger.info("executeExcelOperation() :: Division Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("executeExcelOperation() :: Failed to Import SST MDM VIN Mapping Data for Line No {"+(i+1+1)+"}.");
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
						Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
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
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.sstmdmvinmapping"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated vin list
						 */
						getMappingList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.sstmdmvinmapping"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.sstmdmvinmapping"), errorLineNo));
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sstmdmvinmapping")));
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
							SSTMDMVinDetails deleteDetails = (SSTMDMVinDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = SSTMDMVINMappingDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
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
						Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
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
							deleted = SSTMDMVINMappingDAO.deleteSSTMDMVinDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getMappingList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.sstmdmvinmapping"), String.valueOf(deleteErrorRows[a])));
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "executeExcelOperation()", e);
		}
	}

	private static void getMappingList(SSTMDMVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setMappingList(new ArrayList<SSTMDMVinDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<SSTMDMVinDetails> list = new ArrayList<SSTMDMVinDetails>();
				list = SSTMDMVINMappingDAO.getSSTMDMVinDetails(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						SSTMDMVinDetails details = (SSTMDMVinDetails)list.get(i);
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "getMappingList()", e);
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
	
	private void performAccessCheck(SSTMDMVINMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SSTMDMVinMapping.class.getName(), "performAccessCheck()", e);
		}
	}
}