package com.mazda.gms3.mdm.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import com.mazda.gms3.mdm.bean.DisplayOrderBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CarlineDAO;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.DisplayOrderDAO;
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
import com.mazda.gms3.mdm.vo.DisplayOrderDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class DisplayOrder
 */

public class DisplayOrder extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private Logger logger = LogManager.getLogger(DisplayOrder.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;
	
	String moduleRefKey=AccessManagementInterface.REF_KEY_DISPLAY_ORDER;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DisplayOrder() {
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
			DisplayOrderBean sessionBean = getSessionBean(request);
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
			sessionBean.setDisplayOrderList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setDisplayOrderListToImport(new ArrayList<DisplayOrderDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setModelTypeList(null);
			sessionBean.setFilterModelType(null);
			sessionBean.setInfoMessage(null);
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			/*
			 * call function to load modelType List
			 */
			getModelTypeList(sessionBean);
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/displayorder.jsp");
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
			DisplayOrderBean sessionBean = getSessionBean(request);
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
					
					// set Filter Model Type null as well
					sessionBean.setFilterModelType(null);
					/*
					 * call function to load abbreviationList
					 */
					getDisplayOrderList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * and DisplayOrder Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getDisplayOrderList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("FILTER_ON_DISPLAYORDER"))
				{
					/*
					 * and DisplayOrder Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getDisplayOrderList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_DISPLAYORDER"))
				{
					/*
					 * Save Operation called
					 */
					saveDisplayOrderDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_DISPLAYORDER"))
				{
					/*
					 * Edit Operation called
					 */
					editDisplayOrderDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_DISPLAYORDER"))
				{
					/*
					 * Delete Operation called
					 */
					deleteDisplayOrderDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_DISPLAYORDER"))
				{
					/*
					 * Active Operation called
					 */
					activeDisplayOrderDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_DISPLAYORDER"))
				{
					/*
					 * Update Operation called
					 */
					updateDisplayOrderDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_DISPLAYORDER"))
				{
					// reset some fields
					resetDisplayOrderOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_DISPLAYORDER"))
				{
					/*
					 * Export DisplayOrder Operation
					 */
					exportDisplayOrderDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/displayorder.jsp");
				rs.forward(request, response);
			}
		}
	}

	private DisplayOrderBean getSessionBean(HttpServletRequest request) 
	{
		DisplayOrderBean sessionBean = null;
		if (null != request.getSession().getAttribute("displayOrderBean") && !"".equals(request.getSession().getAttribute("displayOrderBean"))) 
		{
			sessionBean = (DisplayOrderBean) request.getSession().getAttribute("displayOrderBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new DisplayOrderBean();
			request.getSession().setAttribute("displayOrderBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList_OLD(DisplayOrderBean sessionBean, HttpServletRequest request)
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
					sessionBean.setCountryLocaleList(list);
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private  void getCountryLocaleList(DisplayOrderBean sessionBean, HttpServletRequest request)
	{
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> tempList = new ArrayList<CountryLocaleDetails>();
			tempList = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			if(null!=tempList && tempList.size()>0)
			{
				/*
				 * ITERATE LIST AND ALLOW ONLY WRITABLE COUNTRIES
				 */
				String nonWritableMMECountries=ApplicationProperties.getProperty("writable.esi.countries");
				String[] tokens = nonWritableMMECountries.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						String ccCode = tokens[a];
						if(null!=tempList && tempList.size()>0)
						{
							for(int b=0;b<tempList.size();b++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)tempList.get(b);
								if(clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(ccCode.trim().toLowerCase()))
								{
									list.add(clDetails);
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
			tempList=  null;
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	
	private void getLanguageList(DisplayOrderBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> tempList = new ArrayList<ManualLanguageDetails>();
				tempList = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId());
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				if(null!=tempList && tempList.size()>0)
				{
					/*
					 * ITERATE LIST AND ALLOW WRITABLE LANGUAGES
					 */
					String nonWritableMMELocales=ApplicationProperties.getProperty("writable.esi.locales");
					nonWritableMMELocales = nonWritableMMELocales.replace("_", "-");
					String[] tokens = nonWritableMMELocales.split(",");
					if(null!=tokens && tokens.length>0)
					{
						for(int a=0;a<tokens.length;a++)
						{
							String lcCode = tokens[a];
							if(null!=tempList && tempList.size()>0)
							{
								for(int b=0;b<tempList.size();b++)
								{
									ManualLanguageDetails mlDetails = (ManualLanguageDetails)tempList.get(b);
									if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(lcCode.trim().toLowerCase()))
									{
										list.add(mlDetails);
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
				tempList=  null;
				
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
							
							// PROCEED FOR IDENTIFYING LANGUAGES FOR COMBO
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "getLanguageList()", e);
		}
	}
	
	private  void getModelTypeList(DisplayOrderBean sessionBean)
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "getModelTypeList()", e);
		}
	}
	
	private void getFlagList(DisplayOrderBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
		
	private void readParamsFromRequest(DisplayOrderBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new DisplayOrderDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setDisplayOrderListToImport(null);
			sessionBean.setFilterModelType(null);
			
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
						if (fieldName.equals("DISP_UpdatedRows")) 
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
						
						
						if(fieldName.equals("DISP_SelectedRows"))
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
						if (fieldName.equals("DISP_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("DISP_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("DISP_SelectedRows")) 
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
						
						if (fieldName.equals("DISP_CountryLocale_Code")) 
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
						
						if (fieldName.equals("DISP_Lang_Code")) 
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
						
						if (fieldName.equals("DISP_Filter_Model_Type")) 
						{
							// set the value in sessionBean.setFilterModelType
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setFilterModelType(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("DISP_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setDisplayOrderCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setDisplayOrderCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("DISP_Name")) 
						{
							// set the value in sessionBean.getFieldDetails().setDisplayOrderName
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setDisplayOrderName(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("DISP_ModelType")) 
						{
							// set the value in sessionBean.getFieldDetails().setModelType
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setModelType(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("DISP_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private boolean validateFileUpload(DisplayOrderBean sessionBean)
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

	private boolean validate(DisplayOrderDetails fieldDetails, DisplayOrderBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==fieldDetails.getDisplayOrderCode() || "".equals(fieldDetails.getDisplayOrderCode()) ||
				null==fieldDetails.getDisplayOrderName() || "".equals(fieldDetails.getDisplayOrderName()) || 
				null==fieldDetails.getModelType() || "".equals(fieldDetails.getModelType()))
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getDisplayOrderCode() && !"".equals(fieldDetails.getDisplayOrderCode()))
		{
			if(fieldDetails.getDisplayOrderCode().length()>50)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.displayordercode"),"50"));
			}
		}
		
		if(null!=fieldDetails.getDisplayOrderName() && !"".equals(fieldDetails.getDisplayOrderName()))
		{
			if(fieldDetails.getDisplayOrderName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.displayordername"),"200"));
			}
		}
		
		if(null!=fieldDetails.getDisplayOrderCode() && !"".equals(fieldDetails.getDisplayOrderCode()) &&
				null!=fieldDetails.getDisplayOrderName() && !"".equals(fieldDetails.getDisplayOrderName()) && 
				null!=fieldDetails.getModelType() && !"".equals(fieldDetails.getModelType()))
		{
			if(null!=sessionBean.getDisplayOrderList() && sessionBean.getDisplayOrderList().size()>0)
			{
				for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
				{
					DisplayOrderDetails details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
					if(details.getModelType().trim().toLowerCase().equals(fieldDetails.getModelType().trim().toLowerCase()) && 
							details.getDisplayOrderCode().trim().toLowerCase().equals(fieldDetails.getDisplayOrderCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.displayordercode")));
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
	
	private boolean validateUpdate(DisplayOrderDetails fieldDetails, DisplayOrderBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getDisplayOrderCode() || "".equals(fieldDetails.getDisplayOrderCode()) ||
				null==fieldDetails.getDisplayOrderName() || "".equals(fieldDetails.getDisplayOrderName()) || 
				null==fieldDetails.getModelType() || "".equals(fieldDetails.getModelType()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getDisplayOrderCode() && !"".equals(fieldDetails.getDisplayOrderCode()))
		{
			if(fieldDetails.getDisplayOrderCode().length()>50)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.displayordercode")+",50,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getDisplayOrderName() && !"".equals(fieldDetails.getDisplayOrderName()))
		{
			if(fieldDetails.getDisplayOrderName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.displayordername")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getDisplayOrderId() && fieldDetails.getDisplayOrderId()>0 && 
				null!=fieldDetails.getDisplayOrderCode() && !"".equals(fieldDetails.getDisplayOrderCode()) &&
				null!=fieldDetails.getDisplayOrderName() && !"".equals(fieldDetails.getDisplayOrderName()) &&
				null!=fieldDetails.getModelType() && !"".equals(fieldDetails.getModelType()) &&
				null!=sessionBean.getDisplayOrderList() && sessionBean.getDisplayOrderList().size()>0)
		{
			for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
			{
				DisplayOrderDetails details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
				if(fieldDetails.getDisplayOrderId()!=details.getDisplayOrderId()  
						&& details.getDisplayOrderCode().trim().toLowerCase().equals(fieldDetails.getDisplayOrderCode().trim().toLowerCase())  
						&& details.getModelType().trim().toLowerCase().equals(fieldDetails.getModelType().trim().toLowerCase()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.displayordercode")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
					data = null;
					id = null;
					break;
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
	
	private boolean validateExcelRowData(DisplayOrderBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getDisplayOrderListToImport() && sessionBean.getDisplayOrderListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getDisplayOrderListToImport().size();i++)
			{
				DisplayOrderDetails fieldDetails = (DisplayOrderDetails) sessionBean.getDisplayOrderListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;
				
				if(null==fieldDetails.getModelType() || "".equals(fieldDetails.getModelType()) || 
						null==fieldDetails.getDisplayOrderCode() || "".equals(fieldDetails.getDisplayOrderCode()) ||
						null==fieldDetails.getDisplayOrderName() || "".equals(fieldDetails.getDisplayOrderName()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.displayorder")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getDisplayOrderCode() && !"".equals(fieldDetails.getDisplayOrderCode()))
				{
					if(fieldDetails.getDisplayOrderCode().length()>50)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.displayordercode")+",50,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getDisplayOrderName() && !"".equals(fieldDetails.getDisplayOrderName()))
				{
					if(fieldDetails.getDisplayOrderName().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.displayordername")+",200,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getModelType() && !"".equals(fieldDetails.getModelType()))
				{
					boolean matchFound = false;
					if(null!=sessionBean.getModelTypeList() && sessionBean.getModelTypeList().size()>0)
					{
						for(int a=0;a<sessionBean.getModelTypeList().size();a++)
						{
							SelectItemDetails si = (SelectItemDetails)sessionBean.getModelTypeList().get(a);
							if(fieldDetails.getModelType().trim().toLowerCase().equals(si.getLabel().trim().toLowerCase()))
							{
								matchFound =true;
								fieldDetails.setModelType(si.getValue());
								break;
							}
							si  =null;
						}
					}
					
					if(matchFound==false)
					{

						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.modeltype")+","+String.valueOf(rowNo);
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.displayorder")));
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

	private void decideErrorDisplay(DisplayOrderBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_DISPLAYORDER_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "validateExcelRowData()", e);
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
		
	private void saveDisplayOrderDetails(DisplayOrderBean sessionBean)
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
				boolean bool = DisplayOrderDAO.saveDisplayOrderDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveDisplayOrderDetails :: DisplayOrder Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.displayorder")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setDisplayOrderList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getDisplayOrderList(sessionBean);
				}
				else
				{
					logger.info("saveDisplayOrderDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.displayorder")));
				}
			}
			else
			{
				logger.info("saveDisplayOrderDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "saveDisplayOrderDetails()", e);
		}
	}
	
	private void editDisplayOrderDetails(HttpServletRequest request, DisplayOrderBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getDisplayOrderList() && !"".equals(sessionBean.getDisplayOrderList().size()>0))
			{
				for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
				{
					DisplayOrderDetails details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getDisplayOrderList() && !"".equals(sessionBean.getDisplayOrderList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
							{
								DisplayOrderDetails details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
								if(rowId.equals(String.valueOf(details.getDisplayOrderId())))
								{
									logger.info("editDisplayOrderDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
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
				logger.info("editDisplayOrderDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "editDisplayOrderDetails()", e);
		}
	}

	private void deleteDisplayOrderDetails(HttpServletRequest request, DisplayOrderBean sessionBean)
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
					boolean bool = DisplayOrderDAO.deleteDisplayOrderDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.displayorder")));
						/*
						 * call getDisplayOrderList
						 */
						getDisplayOrderList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.displayorder")));
					}
				}
			}
			else
			{
				logger.info("deleteDisplayOrderDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "deleteDisplayOrderDetails()", e);
		}
	}

	private void activeDisplayOrderDetails(HttpServletRequest request, DisplayOrderBean sessionBean)
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
					boolean bool = DisplayOrderDAO.activeDisplayOrderDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.displayorder")));
						/*
						 * call getMissionBookList
						 */
						getDisplayOrderList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.displayorder")));
					}
				}
			}
			else
			{
				logger.info("activeDisplayOrderDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "activeDisplayOrderDetails()", e);
		}
	}
	
	private void updateDisplayOrderDetails(HttpServletRequest request, DisplayOrderBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<DisplayOrderDetails> updateDataList = new ArrayList<DisplayOrderDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getDisplayOrderList() && sessionBean.getDisplayOrderList().size()>0)
					{
						DisplayOrderDetails details = new DisplayOrderDetails();
						for(int i=0;i<sessionBean.getDisplayOrderList().size();i++)
						{
							details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setDisplayOrderCode("");
								details.setDisplayOrderName("");
								details.setModelType("");
								details.setFlag("");

								/*
								 * fetch the values from request
								 * and set in vinList
								 */
								String code="DISP_List_Code"+String.valueOf(details.getDisplayOrderId());
								String name="DISP_List_Name_Eng"+String.valueOf(details.getDisplayOrderId());
								String modelType="DISP_List_ModelType"+String.valueOf(details.getDisplayOrderId());
								String flag="DISP_List_Flag_"+String.valueOf(details.getDisplayOrderId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(code))
										{
											details.setDisplayOrderCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getDisplayOrderCode() && !"".equals(details.getDisplayOrderCode()))
											{
												details.setDisplayOrderCode(details.getDisplayOrderCode().trim());
											}
										}
										else if(key.equals(name))
										{
											details.setDisplayOrderName(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getDisplayOrderName() && !"".equals(details.getDisplayOrderName()))
											{
												details.setDisplayOrderName(details.getDisplayOrderName().trim());
											}
										}
										else if(key.equals(modelType))
										{
											details.setModelType(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getModelType() && !"".equals(details.getModelType()))
											{
												details.setModelType(details.getModelType().trim());
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
								code = null;
								name=null;
								flag= null;
								modelType = null;
							}
							details = null;
						}

						details = new DisplayOrderDetails();
						for(int i=0;i<sessionBean.getDisplayOrderList().size();i++)
						{
							details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(i);
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
							details = null;
						}

						if(null!=updateDataList && updateDataList.size()>0)
						{
							logger.info("updateDisplayOrderDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							
							updateDataList = DisplayOrderDAO.updateDisplayOrderDetails(updateDataList);
							
							for(int a=0;a<updateDataList.size();a++)
							{
								details = (DisplayOrderDetails)updateDataList.get(a);
								if(details.isSaveStatusWhileImport()==true)
								{
									logger.info("updateDisplayOrderDetails :: DisplayOrder Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateDisplayOrderDetails :: Failed to Update DisplayOrder Details for Row No :: >  "+ details.getSrNo());
									errorMessage = errorMessage+String.valueOf(details.getSrNo())+",";	
								}
								details=  null;
							}

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.displayorder"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.displayorder"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateDisplayOrderDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setDisplayOrderList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getDisplayOrderList
								 */
								getDisplayOrderList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateDisplayOrderDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getDisplayOrderList() && sessionBean.getDisplayOrderList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										details = new DisplayOrderDetails();
										for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
										{
											details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "updateDisplayOrderDetails()", e);
		}
	}
	
	private void exportDisplayOrderDetails(HttpServletRequest request, DisplayOrderBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<DisplayOrderDetails> exportDataList = new ArrayList<DisplayOrderDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getDisplayOrderList() && !"".equals(sessionBean.getDisplayOrderList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getDisplayOrderList().size();a++)
							{
								DisplayOrderDetails details = (DisplayOrderDetails)sessionBean.getDisplayOrderList().get(a);
								if(rowId.equals(String.valueOf(details.getDisplayOrderId())))
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
					writeDisplayOrderExcel(exportDataList, sessionBean);
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
					logger.info("exportDisplayOrderDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportDisplayOrderDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "exportDisplayOrderDetails()", e);
		}
	}

	
	private void readExcelData(byte[] data, DisplayOrderBean sessionBean,String extension)
	{
		sessionBean.setDisplayOrderListToImport(new ArrayList<DisplayOrderDetails>());
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
				 * MODEL TYPE
				 * DISPLAY ORDER CODE
				 * DISPLAY ORDER NAME
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						DisplayOrderDetails details = new DisplayOrderDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setModelType(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setDisplayOrderCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setDisplayOrderName(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						/*
						 * add details to vinList for import
						 */
						if(null==sessionBean.getDisplayOrderListToImport() || sessionBean.getDisplayOrderListToImport().size()<=0)
						{
							sessionBean.setDisplayOrderListToImport(new ArrayList<DisplayOrderDetails>());
						}

						sessionBean.getDisplayOrderListToImport().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsWorkBook=  null;
				xlsSheet = null;
				rowIterator  = null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "readExcelData()", e);
		}
	}

	private void writeDisplayOrderExcel(ArrayList<DisplayOrderDetails> vinList, DisplayOrderBean sessionBean)
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
				 * So the final Name will be - US_EN-US_ND_DISPLAYORDER_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_DISPLAYORDER_NAME");
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
				
				Cell modelTypeCell = headerRow.createCell(0);
				modelTypeCell.setCellValue("MODEL TYPE");
				Cell codeCell = headerRow.createCell(1);
				codeCell.setCellValue("DISPLAY ORDER CODE");
				Cell nameCell = headerRow.createCell(2);
				nameCell.setCellValue("DISPLAY ORDER NAME");
				
				int rowCount=0;
				if(null!=vinList && vinList.size()>0)
				{
					for(int i=0;i<vinList.size();i++)
					{
						DisplayOrderDetails details = (DisplayOrderDetails)vinList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						
						if(null!=details.getModelType() && !"".equals(details.getModelType()))
						{
							cell0.setCellValue(details.getModelType());
						}
						if(null!=details.getDisplayOrderCode() && !"".equals(details.getDisplayOrderCode()))
						{
							cell1.setCellValue(details.getDisplayOrderCode());
						}
						if(null!=details.getDisplayOrderName() && !"".equals(details.getDisplayOrderName()))
						{
							cell2.setCellValue(details.getDisplayOrderName());
						}
						
						
						cell2 = null;
						cell0 = null;
						cell1 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					codeCell = null;
					nameCell=  null;
					modelTypeCell= null;
					
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "writeDisplayOrderExcel()", e);
		}
	}

	private void executeExcelOperation(DisplayOrderBean sessionBean, byte[] data, String extension)
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
				ArrayList<DisplayOrderDetails> listToSave = new ArrayList<DisplayOrderDetails>();
				DisplayOrderDetails fieldDetails = new DisplayOrderDetails();
				DisplayOrderDetails existingDetails = new DisplayOrderDetails();
				for(int i=0;i<sessionBean.getDisplayOrderListToImport().size();i++)
				{
					fieldDetails = new DisplayOrderDetails();
					fieldDetails = (DisplayOrderDetails)sessionBean.getDisplayOrderListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							existingDetails = new DisplayOrderDetails();
							existingDetails = (DisplayOrderDetails)listToSave.get(j);
							/*
							 * IDENTIY DUPLICATE ON THE BASIS OF 
							 *  ABBREVIATION CODE
							 */
							if(fieldDetails.getModelType().equals(existingDetails.getModelType()) && 
									fieldDetails.getDisplayOrderCode().equals(existingDetails.getDisplayOrderCode()) && 
									fieldDetails.getDisplayOrderName().equals(existingDetails.getDisplayOrderName()))
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
							existingDetails = null;
						}
					}
					if(addToList==true)
					{
						listToSave.add(fieldDetails);
					}
					fieldDetails = null;
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.displayorder"), String.valueOf(rows[a])));
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
					fieldDetails = new DisplayOrderDetails();
					ArrayList<DisplayOrderDetails> tempList = listToSave;
					listToSave = new ArrayList<DisplayOrderDetails>();
					for(int i=0;i<tempList.size();i++)
					{
						fieldDetails = new DisplayOrderDetails();  
						fieldDetails = (DisplayOrderDetails)tempList.get(i);
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						listToSave.add(fieldDetails);
						fieldDetails = null;
					}
					
					
					listToSave = DisplayOrderDAO.importDisplayOrderDetails(listToSave);
					fieldDetails = new DisplayOrderDetails();
					for(int i=0;i<listToSave.size();i++)
					{
						fieldDetails= new DisplayOrderDetails();
						fieldDetails = (DisplayOrderDetails)listToSave.get(i);
						if(fieldDetails.isSaveStatusWhileImport() == true)
						{
							logger.info("readParamsFromRequest() :: DisplayOrder Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("readParamsFromRequest() :: Failed to Import DisplayOrder Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails= null;
					}

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
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.displayorder"), successLineNo ));
						
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated vin list
						 */
						getDisplayOrderList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.displayorder"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.displayorder"), errorLineNo));
					}
					
					successLineNo = null;
					errorLineNo = null;		
				}
				else
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.displayorder")));
				}
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "executeExcelOperation()", e);
		}
	}

	private void getDisplayOrderList(DisplayOrderBean sessionBean)
	{
		try
		{
			sessionBean.setDisplayOrderList(new ArrayList<DisplayOrderDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) && 
					null!=sessionBean.getFilterModelType() && !"".equals(sessionBean.getFilterModelType()))
			{
				String filterModelType="";
				if(null!=sessionBean.getFilterModelType() && !"".equals(sessionBean.getFilterModelType()))
				{
					filterModelType=  sessionBean.getFilterModelType();
				}
				ArrayList<DisplayOrderDetails> list = new ArrayList<DisplayOrderDetails>();
				list = DisplayOrderDAO.getDisplayOrderDetailsList(String.valueOf(sessionBean.getManualLanguageId()), filterModelType);
				filterModelType = null;
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						DisplayOrderDetails details = (DisplayOrderDetails)list.get(i);
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
						details=  null;
					}
					// SET ALL DISPLAY LIST
					sessionBean.setDisplayOrderList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "getDisplayOrderList()", e);
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
	
	private void performAccessCheck(DisplayOrderBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(DisplayOrder.class.getName(), "performAccessCheck()", e);
		}
	}

	/**
	 * Function will perform reset operation
	 * @param sessionBean
	 */
	private void resetDisplayOrderOperation(DisplayOrderBean sessionBean)
	{
		sessionBean.setDisplayOrderList(null);
		sessionBean.setFlagList(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSuccessMessage(null);
		sessionBean.setSelectedRows(null);
		sessionBean.setShowUpdate(false);
		sessionBean.setDisplayPageLength(null);
		sessionBean.setDisplayPageNo(null);
		sessionBean.setUpdatedRows(null);
		/*
		 * call getDisplayOrderList
		 */
		getDisplayOrderList(sessionBean);	
	}
}
