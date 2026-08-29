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
import org.apache.poi.ss.format.CellNumberFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.bean.VinCrossReferenceBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.VINCrossReferenceDAO;
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
import com.mazda.gms3.mdm.vo.VinCrossReferenceDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class VINCrossReference
 */
public class VINCrossReference extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	Logger logger = LogManager.getLogger(VINCrossReference.class);

	MessageProperties msgProps= null;
	String wslId=null;
	String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_VIN_CROSS_REFERENCE;

       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public VINCrossReference() {
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
			VinCrossReferenceBean sessionBean = getSessionBean(request);
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
			sessionBean.setCrossReferenceList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setInfoMessage(null);

			sessionBean.setReportViewPath(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setCrossReferenceListToImport(null);

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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/vincrossreference.jsp");
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
			VinCrossReferenceBean sessionBean = getSessionBean(request);
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
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);

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
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setManualLanguageId(null);
					/*
					 * call function to load all the Manual language data
					 */
					getLanguageList(sessionBean, request);
					/*
					 * call function to load vincrossReferenceDetails
					 */
					getCrossReferenceList(sessionBean);

				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					/*
					 * call function to load vincrossReferenceDetails
					 */
					getCrossReferenceList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE"))
				{
					/*
					 * Save Operation called
					 */
					saveVinCrossReferenceDetails(sessionBean);
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("EDIT"))
				{
					/*
					 * Edit Operation called
					 */
					editVinCrossReferenceDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE"))
				{
					/*
					 * Delete Operation called
					 */
					deleteVinCrossReferenceDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE"))
				{
					/*
					 * Update Operation called
					 */
					updateVinCrossReferenceDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE"))
				{
					/*
					 * Active Operation called
					 */
					activeVinCrossReferenceDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					// reset some fields
					sessionBean.setCrossReferenceList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					/*
					 * call getCrossReferenceList()
					 */
					getCrossReferenceList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT"))
				{
					/*
					 * Export VIN Operation
					 */
					exportVinCrossReferenceDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/vincrossreference.jsp");
				rs.forward(request, response);
			}
		}
	}
	
	private VinCrossReferenceBean getSessionBean(HttpServletRequest request) 
	{
		VinCrossReferenceBean sessionBean = null;
		if (null != request.getSession().getAttribute("vinCrossReferenceBean") && !"".equals(request.getSession().getAttribute("vinCrossReferenceBean"))) 
		{
			sessionBean = (VinCrossReferenceBean) request.getSession().getAttribute("vinCrossReferenceBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new VinCrossReferenceBean();
			request.getSession().setAttribute("vinCrossReferenceBean", sessionBean);
		}
		return sessionBean;
	}
	

	private void getCountryLocaleList(VinCrossReferenceBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private void getLanguageList(VinCrossReferenceBean sessionBean, HttpServletRequest request)
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
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "getLanguageList()", e);
		}
	}

	private  void getFlagList(VinCrossReferenceBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());

		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
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

	private void performAccessCheck(VinCrossReferenceBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "performAccessCheck()", e);
		}
	}

	
	private  void getCrossReferenceList(VinCrossReferenceBean sessionBean)
	{
		try
		{
			sessionBean.setCrossReferenceList(new ArrayList<VinCrossReferenceDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<VinCrossReferenceDetails> list = new ArrayList<VinCrossReferenceDetails>();
				list = VINCrossReferenceDAO.getVinCrossReferenceDetailsList(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						VinCrossReferenceDetails details  = (VinCrossReferenceDetails)list.get(i);
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
					sessionBean.setCrossReferenceList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "getCrossReferenceList()()", e);
		}
	}


	private  void readParamsFromRequest(VinCrossReferenceBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new VinCrossReferenceDetails());
			sessionBean.setManualLanguageId(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setCrossReferenceListToImport(null);

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
						if (fieldName.equals("VIN_XREF_UpdatedRows")) 
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


						if(fieldName.equals("VIN_XREF_SelectedRows"))
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
						if (fieldName.equals("VIN_XREF_DataTabel_displayPageNo")) 
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

						if (fieldName.equals("VIN_XREF_DataTabel_displayPageLen")) 
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

						if (fieldName.equals("VIN_XREF_CountryLocale_Code")) 
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

						if (fieldName.equals("VIN_XREF_Lang_Code")) 
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

						if (fieldName.equals("VIN_XREF_CrossRef_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setVinCrossReferenceCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setVinCrossReferenceCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_XREF_CrossRef_Year")) 
						{
							// set the value in sessionBean.getFieldDetails().setVinCrossReferenceYear
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setVinCrossReferenceYear(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_XREF_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "readParamsFromRequest(", e);
		}
	}

	private  boolean validate(VinCrossReferenceDetails fieldDetails, VinCrossReferenceBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		boolean addMandatoryMessage = false;
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==fieldDetails.getVinCrossReferenceCode() || "".equals(fieldDetails.getVinCrossReferenceCode()) || 
				null==fieldDetails.getVinCrossReferenceYear() || "".equals(fieldDetails.getVinCrossReferenceYear()))
		{
			addMandatoryMessage = true;
		}

		if(addMandatoryMessage==true)
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}

		if(null!=fieldDetails.getVinCrossReferenceYear() && !"".equals(fieldDetails.getVinCrossReferenceYear()))
		{
			if(fieldDetails.getVinCrossReferenceYear().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.vincrossrefyear"),"20"));
			}
		}
		
		if(null!=fieldDetails.getVinCrossReferenceCode() && !"".equals(fieldDetails.getVinCrossReferenceCode()))
		{
			if(fieldDetails.getVinCrossReferenceCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.vincrossrefcode"),"20"));
			}
		}

		if(null!=fieldDetails.getVinCrossReferenceCode() && !"".equals(fieldDetails.getVinCrossReferenceCode()) && 
				null!=fieldDetails.getVinCrossReferenceYear() && !"".equals(fieldDetails.getVinCrossReferenceYear()) && 
				null!=sessionBean.getCrossReferenceList() && sessionBean.getCrossReferenceList().size()>0)
		{
			for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
			{
				VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
				/*
				 * CHECK FOR THE MANDATORY FIELDS
				 */
				if(null!=details.getVinCrossReferenceYear() && !"".equals(details.getVinCrossReferenceYear()) && 
						null!=details.getVinCrossReferenceCode() && !"".equals(details.getVinCrossReferenceCode()))
				{
					if(details.getVinCrossReferenceYear().trim().toLowerCase().equals(fieldDetails.getVinCrossReferenceYear().trim().toLowerCase()) && 
							details.getVinCrossReferenceCode().trim().toLowerCase().equals(fieldDetails.getVinCrossReferenceCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.vincrossref")));
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

	private  boolean validateUpdate(VinCrossReferenceDetails fieldDetails, VinCrossReferenceBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getVinCrossReferenceCode() || "".equals(fieldDetails.getVinCrossReferenceCode()) || 
				null==fieldDetails.getVinCrossReferenceYear() || "".equals(fieldDetails.getVinCrossReferenceYear()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getVinCrossReferenceYear() && !"".equals(fieldDetails.getVinCrossReferenceYear()))
		{
			if(fieldDetails.getVinCrossReferenceYear().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.vincrossrefyear")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getVinCrossReferenceCode() && !"".equals(fieldDetails.getVinCrossReferenceCode()))
		{
			if(fieldDetails.getVinCrossReferenceCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.vincrossrefcode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}


		if(null!=fieldDetails.getVinCrossReferenceCode() && !"".equals(fieldDetails.getVinCrossReferenceCode()) && 
				null!=fieldDetails.getVinCrossReferenceYear() && !"".equals(fieldDetails.getVinCrossReferenceYear()) && 
				null!=sessionBean.getCrossReferenceList() && sessionBean.getCrossReferenceList().size()>0)
		{
			for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
			{
				VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
				/*
				 * CHECL FOR THE MANDATORY FIELDS
				 */
				if(null!=details.getVinCrossReferenceYear() && !"".equals(details.getVinCrossReferenceYear()) && 
						null!=details.getVinCrossReferenceCode() && !"".equals(details.getVinCrossReferenceCode()))
				{
					if(details.getVinCrossReferenceId() != fieldDetails.getVinCrossReferenceId() && 
							details.getVinCrossReferenceYear().trim().toLowerCase().equals(fieldDetails.getVinCrossReferenceYear().trim().toLowerCase()) && 
							details.getVinCrossReferenceCode().trim().toLowerCase().equals(fieldDetails.getVinCrossReferenceCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.vincrossref")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
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

	private  boolean validateFileUpload(VinCrossReferenceBean sessionBean)
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

	private  void decideErrorDisplay(VinCrossReferenceBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_VIN_XREF_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "validateExcelRowData()", e);
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

	private  void saveVinCrossReferenceDetails(VinCrossReferenceBean sessionBean)
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
				sessionBean.getFieldDetails().setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
				sessionBean.getFieldDetails().setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = VINCrossReferenceDAO.saveVinCrossReferenceDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveVinCrossReferenceDetails :: VINCrossReference Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.vincrossref")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setCrossReferenceList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getVINCrossReferenceList
					 */
					getCrossReferenceList(sessionBean);
				}
				else
				{
					logger.info("saveVinCrossReferenceDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.vincrossref")));
				}
			}
			else
			{
				logger.info("saveVinCrossReferenceDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "saveVinCrossReferenceDetails()", e);
		}
	}

	private  void editVinCrossReferenceDetails(HttpServletRequest request, VinCrossReferenceBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getCrossReferenceList() && !"".equals(sessionBean.getCrossReferenceList().size()>0))
			{
				for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
				{
					VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCrossReferenceList() && !"".equals(sessionBean.getCrossReferenceList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
							{
								VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
								if(rowId.equals(String.valueOf(details.getVinCrossReferenceId())))
								{
									logger.info("editVinCrossReferenceDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
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
				logger.info("editVinCrossReferenceDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "editVinCrossReferenceDetails()", e);
		}
	}

	private  void deleteVinCrossReferenceDetails(HttpServletRequest request, VinCrossReferenceBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String deleteIds=String.valueOf(sessionBean.getSelectedRows());
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = VINCrossReferenceDAO.deleteVinCrossReferenceDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.vincrossref")));
						/*
						 * call getVINCrossReferenceList
						 */
						getCrossReferenceList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.vincrossref")));
					}
				}
			}
			else
			{
				logger.info("deleteVinCrossReferenceDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "deleteVinCrossReferenceDetails()", e);
		}
	}

	private  void activeVinCrossReferenceDetails(HttpServletRequest request, VinCrossReferenceBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String activeIds=String.valueOf(sessionBean.getSelectedRows());
				if(null!=activeIds && !"".equals(activeIds))
				{
					if(activeIds.endsWith(","))
					{
						activeIds = activeIds.substring(0,activeIds.length()-1);
					}
					boolean bool = VINCrossReferenceDAO.activeVinCrossReferenceDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.vincrossref")));
						/*
						 * call getVINCrossReferenceList
						 */
						getCrossReferenceList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.vincrossref")));
					}
				}
				activeIds  =null;
			}
			else
			{
				logger.info("activeVinCrossReferenceDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "activeVinCrossReferenceDetails()", e);
		}
	}

	private  void exportVinCrossReferenceDetails(HttpServletRequest request, VinCrossReferenceBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<VinCrossReferenceDetails> exportDataList = new ArrayList<VinCrossReferenceDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCrossReferenceList() && !"".equals(sessionBean.getCrossReferenceList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
							{
								VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
								if(rowId.equals(String.valueOf(details.getVinCrossReferenceId())))
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
					writeXREFExcel(exportDataList, sessionBean);
					/*
					 * PREARE CVC REORT PATH AND MAKE IT DOWNLOAD
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
					logger.info("exportVinCrossReferenceDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportVinCrossReferenceDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "exportVinCrossReferenceDetails()", e);
		}
	}

	private  void updateVinCrossReferenceDetails(HttpServletRequest request, VinCrossReferenceBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<VinCrossReferenceDetails> updateDataList = new ArrayList<VinCrossReferenceDetails>();
				String updatedRows=(String)sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getCrossReferenceList() && sessionBean.getCrossReferenceList().size()>0)
					{
						for(int i=0;i<sessionBean.getCrossReferenceList().size();i++)
						{
							VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setVinCrossReferenceCode("");
								details.setVinCrossReferenceYear("");
								details.setFlag("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String codeId="VIN_XREF_VINCrossReferenceList_Code_"+String.valueOf(details.getVinCrossReferenceId());
								String yearId="VIN_XREF_VINCrossReferenceList_Year_"+String.valueOf(details.getVinCrossReferenceId());

								String flag="VIN_XREF_List_Flag_"+String.valueOf(details.getVinCrossReferenceId());

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(codeId))
										{
											details.setVinCrossReferenceCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(yearId))
										{
											details.setVinCrossReferenceYear(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(flag))
										{
											details.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key = null;
										token = null;
									}
								}

								// set all request params ids to null
								codeId = null;
								yearId = null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getCrossReferenceList().size();i++)
						{
							VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(i);
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
							logger.info("updateVinCrossReferenceDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
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
								Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "updateVinCrossReferenceDetails()", e);
							}
							
							
							for(int a=0;a<updateDataList.size();a++)
							{
								VinCrossReferenceDetails details = (VinCrossReferenceDetails)updateDataList.get(a);
								boolean updateFlag = VINCrossReferenceDAO.updateVinCrossReferenceDetails(details, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateVinCrossReferenceDetails :: VINCrossReference Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateVinCrossReferenceDetails :: Failed to Update Book Details for Row No :: >  "+ details.getSrNo());
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
								Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "updateVinCrossReferenceDetails()", e);
							}
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.vincrossref"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.vincrossref"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateVinCrossReferenceDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setCrossReferenceList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getVINCrossReferenceList
								 */
								getCrossReferenceList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateVinCrossReferenceDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getCrossReferenceList() && sessionBean.getCrossReferenceList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getCrossReferenceList().size();a++)
										{
											VinCrossReferenceDetails details = (VinCrossReferenceDetails)sessionBean.getCrossReferenceList().get(a);
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "updateVinCrossReferenceDetails()", e);
		}
	}

	private  boolean validateExcelRowData(VinCrossReferenceBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getCrossReferenceListToImport() && sessionBean.getCrossReferenceListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getCrossReferenceListToImport().size();i++)
			{
				VinCrossReferenceDetails fieldDetails = (VinCrossReferenceDetails) sessionBean.getCrossReferenceListToImport().get(i);
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


				if(null==fieldDetails.getVinCrossReferenceCode() || "".equals(fieldDetails.getVinCrossReferenceCode()) ||
						null==fieldDetails.getVinCrossReferenceYear() || "".equals(fieldDetails.getVinCrossReferenceYear()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.vincrossref")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getVinCrossReferenceCode() && !"".equals(fieldDetails.getVinCrossReferenceCode()))
				{
					if(fieldDetails.getVinCrossReferenceCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.vincrossrefcode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getVinCrossReferenceYear() && !"".equals(fieldDetails.getVinCrossReferenceYear()))
				{
					if(fieldDetails.getVinCrossReferenceYear().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.vincrossrefyear")+",20,"+String.valueOf(rowNo);
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vincrossref")));
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

	private  void executeExcelOperation(VinCrossReferenceBean sessionBean, byte[] data, String extension)
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

				String duplicateRowNo="";
				int errorCount=0;
				int duplicateRowsCount=0;
				int failureCount=0;
				ArrayList<VinCrossReferenceDetails> listToSave = new ArrayList<VinCrossReferenceDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<VinCrossReferenceDetails> listToDelete = new ArrayList<VinCrossReferenceDetails>();
				for(int i=0;i<sessionBean.getCrossReferenceListToImport().size();i++)
				{
					VinCrossReferenceDetails fieldDetails = (VinCrossReferenceDetails)sessionBean.getCrossReferenceListToImport().get(i);
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
							VinCrossReferenceDetails existingDetails = (VinCrossReferenceDetails)listToSave.get(j);
							if(fieldDetails.getVinCrossReferenceCode().equals(existingDetails.getVinCrossReferenceCode()) 
									&& fieldDetails.getVinCrossReferenceYear().equals(existingDetails.getVinCrossReferenceYear()))
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.vincrossref"), String.valueOf(rows[a])));
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
						Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
					}
					for(int i=0;i<listToSave.size();i++)
					{
						VinCrossReferenceDetails fieldDetails = (VinCrossReferenceDetails)listToSave.get(i);
						
						fieldDetails.setCountryLocaleCode(countryLocaleCode);
						fieldDetails.setManualLanguageCode(languageCode);
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						boolean saveVINCrossReferenceData = VINCrossReferenceDAO.importVinCrossReferenceDetails(fieldDetails, conn, closeConnection);

						if(saveVINCrossReferenceData == true)
						{
							logger.info("readParamsFromRequest() :: VINCrossReference Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("readParamsFromRequest() :: Failed to Import VINCrossReference Data for Line No {"+(i+1+1)+"}.");
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
						Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
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
						successRows = null;

						//						successLineNo = "( "+successLineNo+" )";
						//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.vincrossref"), successLineNo ));

						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated crossRef list
						 */
						getCrossReferenceList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.vincrossref"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
						//						errorLineNo= "( "+errorLineNo+ " )";
						//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.vincrossref"), errorLineNo));
					}

					successLineNo = null;
					errorLineNo = null;		
					countryLocaleCode = null;
					languageCode = null;
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vincrossref")));
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
							VinCrossReferenceDetails deleteDetails = (VinCrossReferenceDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = VINCrossReferenceDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
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
						Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
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
							deleted = VINCrossReferenceDAO.deleteVinCrossReferenceDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getCrossReferenceList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.vincrossref"), String.valueOf(deleteErrorRows[a])));
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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "executeExcelOperation()", e);
		}
	}

	/**
	 * Function will identify the Cell Value Type
	 * and accordingly will read their values and will return
	 * @param cell
	 * @return
	 */
	private  Object readCellValue(Cell cell)
	{
		Object cellValue = null;
		try {
			if (null != cell) {
				/*
				 * check for Cell Type and format accordingly
				 */
				if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
					cellValue = cell.getNumericCellValue();

					/*
					 * Check here, if callValue is 0; THEN return "" as object
					 * else, check for decimal value in cell
					 * if decimal value is .0 or .00, then remove it
					 * if more than 0, then pass the value as it is
					 */

					String val = String.valueOf(cellValue);
					if(null!=val && !"".equals(val))
					{
						/*
						 *  check if val has any value in point to decimal and that value is 0 then remove the decimal Value
						 */
						if(val.lastIndexOf(".")!=-1)
						{
							String decimal = val.substring(val.lastIndexOf(".")+1,val.length());
							if(null!=decimal && !"".equals(decimal))
							{
								Double dec = new Double(decimal).doubleValue();
								if(dec==0)
								{
									// remove decimal from the val and value after decimal
									val = val.substring(0,val.lastIndexOf("."));
								}
								else
								{
									// GET THE NUMERIC VALUE IN THE FORMAT ########################## AND PASS IT
									CellNumberFormatter cn = new CellNumberFormatter("################################");
									val = cn.format(cell.getNumericCellValue());
									cn = null;
								}
								// set dec to null
								dec = null;
							}
							// set decimal to null
							decimal = null;
						}
					}

					// set cellValue as val
					cellValue = val;
					// set val to null
					val  = null;
				}
				else if(cell.getCellType() == Cell.CELL_TYPE_FORMULA)
				{
					cellValue = cell.getCellFormula();
					if(null!=cellValue)
					{
						// append a = with this formaula
						cellValue = "="+cellValue;
					}
				}
				else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					cellValue = cell.getStringCellValue();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "readCellValue()", e);
			// set cellValue to null
			cellValue = null;
		}
		return cellValue;
	}

	private  void writeXREFExcel(ArrayList<VinCrossReferenceDetails> crossRefList, VinCrossReferenceBean sessionBean)
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
				// add CVC DATA NAME
				String name = "";
				/*
				 * add SELECTED COUNTRY LOCALE CODE, ADD MANUAL LANGUAGE CODE, 
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - US_EN-US_CVC_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_VIN_XREF_NAME");
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

				Cell codeCell = headerRow.createCell(0);
				codeCell.setCellValue("VIN XREF CODE");
				Cell yearCell = headerRow.createCell(1);
				yearCell.setCellValue("VIN XREF YEAR");

				int rowCount=0;
				if(null!=crossRefList && crossRefList.size()>0)
				{
					for(int i=0;i<crossRefList.size();i++)
					{
						VinCrossReferenceDetails cDetails = (VinCrossReferenceDetails)crossRefList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);

						cell0.setCellValue("");
						cell1.setCellValue("");

						if(null!=cDetails.getVinCrossReferenceCode() && !"".equals(cDetails.getVinCrossReferenceCode()))
						{
							cell0.setCellValue(cDetails.getVinCrossReferenceCode());
						}
						if(null!=cDetails.getVinCrossReferenceYear() && !"".equals(cDetails.getVinCrossReferenceYear()))
						{
							cell1.setCellValue(cDetails.getVinCrossReferenceYear());
						}

						cell0 = null;
						cell1 = null;
						row = null;
						cDetails = null;
					}

					headerRow =  null;
					codeCell=null;
					yearCell=null;

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
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "writeXREFExcel()", e);
		}
	}

	private  void readExcelData(byte[] data, VinCrossReferenceBean sessionBean, String extension)
	{
		sessionBean.setCrossReferenceListToImport(new ArrayList<VinCrossReferenceDetails>());
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
				 * RANK
				 * VIN XREF CODE
				 * VIN XREF YEAR
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						VinCrossReferenceDetails cDetails = new VinCrossReferenceDetails();
						Object dataCell = readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setVinCrossReferenceCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setVinCrossReferenceYear(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * add details to crossRefList for import
						 */
						if(null==sessionBean.getCrossReferenceListToImport() || sessionBean.getCrossReferenceListToImport().size()<=0)
						{
							sessionBean.setCrossReferenceListToImport(new ArrayList<VinCrossReferenceDetails>());
						}

						sessionBean.getCrossReferenceListToImport().add(cDetails);
						cDetails= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsSheet = null;
				xlsWorkBook = null;
				rowIterator=  null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(VINCrossReference.class.getName(), "readExcelData()", e);
		}
	}
}