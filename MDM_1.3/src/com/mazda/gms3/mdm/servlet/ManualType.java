package com.mazda.gms3.mdm.servlet;

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

import com.mazda.gms3.mdm.bean.ManualTypeBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualTypeDAO;
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
import com.mazda.gms3.mdm.vo.ImportActionResult;
import com.mazda.gms3.mdm.vo.ManualTypeDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class ManualType
 */

public class ManualType extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static Logger logger = LogManager.getLogger(ManualType.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	static String reportName=null;
	
	String moduleRefKey=AccessManagementInterface.REF_KEY_MANUAL_TYPE;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ManualType() {
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
			ManualTypeBean sessionBean = getSessionBean(request);
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
			sessionBean.setManualTypeList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setManualTypeListToImport(new ArrayList<ManualTypeDetails>());
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/manualType.jsp");
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
			ManualTypeBean sessionBean = getSessionBean(request);
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
					 * call function to load abbreviationList
					 */
					getManualTypeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * and ManualType Details List in case of language change
					 */
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getManualTypeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_MANUALTYPE"))
				{
					/*
					 * Save Operation called
					 */
					saveManualTypeDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_MANUALTYPE"))
				{
					/*
					 * Edit Operation called
					 */
					editManualTypeDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_MANUALTYPE"))
				{
					/*
					 * Delete Operation called
					 */
					deleteManualTypeDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_MANUALTYPE"))
				{
					/*
					 * Active Operation called
					 */
					activeManualTypeDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_MANUALTYPE"))
				{
					/*
					 * Update Operation called
					 */
					updateManualTypeDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_MANUALTYPE"))
				{
					// reset some fields
					sessionBean.setManualTypeList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					sessionBean.setUpdatedRows(null);
					/*
					 * call getManualTypeList
					 */
					getManualTypeList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_MANUALTYPE"))
				{
					/*
					 * Export ManualType Operation
					 */
					exportManualTypeDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/manualType.jsp");
				rs.forward(request, response);
			}
		}
	}

	private ManualTypeBean getSessionBean(HttpServletRequest request) 
	{
		ManualTypeBean sessionBean = null;
		if (null != request.getSession().getAttribute("manualTypeBean") && !"".equals(request.getSession().getAttribute("manualTypeBean"))) 
		{
			sessionBean = (ManualTypeBean) request.getSession().getAttribute("manualTypeBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new ManualTypeBean();
			request.getSession().setAttribute("manualTypeBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(ManualTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(ManualTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "getLanguageList()", e);
		}
	}
	
	private static void getFlagList(ManualTypeBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}
		
	private static void readParamsFromRequest(ManualTypeBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new ManualTypeDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setManualTypeListToImport(null);
			
			
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
						if (fieldName.equals("MAN_TYPE_UpdatedRows")) 
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
						
						
						if(fieldName.equals("MAN_TYPE_SelectedRows"))
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
						if (fieldName.equals("MAN_TYPE_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("MAN_TYPE_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("MAN_TYPE_SelectedRows")) 
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
						
						if (fieldName.equals("MAN_TYPE_CountryLocale_Code")) 
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
						
						if (fieldName.equals("MAN_TYPE_Lang_Code")) 
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
						
						if (fieldName.equals("MAN_TYPE_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setManualTypeCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setManualTypeCode(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MAN_TYPE_Name")) 
						{
							// set the value in sessionBean.getFieldDetails().setManualTypeName
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setManualTypeName(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MAN_TYPE_RefKey")) 
						{
							// set the value in sessionBean.getFieldDetails().setManualTypeRefKey
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setManualTypeRefKey(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MAN_TYPE_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private static boolean validateFileUpload(ManualTypeBean sessionBean)
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

	private static boolean validate(ManualTypeDetails fieldDetails, ManualTypeBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==fieldDetails.getManualTypeCode() || "".equals(fieldDetails.getManualTypeCode()) ||
				null==fieldDetails.getManualTypeName() || "".equals(fieldDetails.getManualTypeName()) || 
				null==fieldDetails.getManualTypeRefKey() || "".equals(fieldDetails.getManualTypeRefKey()))
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()))
		{
			if(fieldDetails.getManualTypeCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.manualtypecode"),"20"));
			}
		}
		
		if(null!=fieldDetails.getManualTypeName() && !"".equals(fieldDetails.getManualTypeName()))
		{
			if(fieldDetails.getManualTypeName().length()>150)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.manualtypename"),"150"));
			}
		}
		
		if(null!=fieldDetails.getManualTypeRefKey() && !"".equals(fieldDetails.getManualTypeRefKey()))
		{
			if(fieldDetails.getManualTypeRefKey().length()>150)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.manualtyperefkey"),"150"));
			}
		}
		
		if(null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()) &&
				null!=fieldDetails.getManualTypeName() && !"".equals(fieldDetails.getManualTypeName()) && 
				null!=fieldDetails.getManualTypeRefKey() && !"".equals(fieldDetails.getManualTypeRefKey()))
		{
			if(null!=sessionBean.getManualTypeList() && sessionBean.getManualTypeList().size()>0)
			{
				for(int a=0;a<sessionBean.getManualTypeList().size();a++)
				{
					ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
					if(details.getManualTypeCode().trim().toLowerCase().equals(fieldDetails.getManualTypeCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.manualtypecode")));
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
	
	private static boolean validateUpdate(ManualTypeDetails fieldDetails, ManualTypeBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getManualTypeCode() || "".equals(fieldDetails.getManualTypeCode()) ||
				null==fieldDetails.getManualTypeName() || "".equals(fieldDetails.getManualTypeName()) || 
				null==fieldDetails.getManualTypeRefKey() || "".equals(fieldDetails.getManualTypeRefKey()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()))
		{
			if(fieldDetails.getManualTypeCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.manualtypecode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getManualTypeName() && !"".equals(fieldDetails.getManualTypeName()))
		{
			if(fieldDetails.getManualTypeName().length()>150)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.manualtypename")+",150,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getManualTypeRefKey() && !"".equals(fieldDetails.getManualTypeRefKey()))
		{
			if(fieldDetails.getManualTypeRefKey().length()>150)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.manualtyperefkey")+",150,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getManualTypeId() && fieldDetails.getManualTypeId()>0 && 
				null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()) &&
				null!=fieldDetails.getManualTypeName() && !"".equals(fieldDetails.getManualTypeName()) &&
				null!=fieldDetails.getManualTypeRefKey() && !"".equals(fieldDetails.getManualTypeRefKey()) &&
				null!=sessionBean.getManualTypeList() && sessionBean.getManualTypeList().size()>0)
		{
			for(int a=0;a<sessionBean.getManualTypeList().size();a++)
			{
				ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
				if(fieldDetails.getManualTypeId()!=details.getManualTypeId()  
						&& details.getManualTypeCode().trim().toLowerCase().equals(fieldDetails.getManualTypeCode().trim().toLowerCase()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.manualtypecode")+","+String.valueOf(fieldDetails.getSrNo());
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
	
	private static boolean validateExcelRowData(ManualTypeBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		// tracks the in-file unique combination (locale + language are fixed per import, so the
		// discriminator is the manual type code) to reject duplicate rows before any DB work.
		java.util.Set<String> seenUniqueKeys = new java.util.HashSet<String>();
		if(null!=sessionBean.getManualTypeListToImport() && sessionBean.getManualTypeListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getManualTypeListToImport().size();i++)
			{
				ManualTypeDetails fieldDetails = (ManualTypeDetails) sessionBean.getManualTypeListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;

				/*
				 * NEW: reject an unknown value in the Action / Marker column BEFORE any DB work.
				 * Allowed: blank, A/ADD, U/UPDATE, D/DELETE (case-insensitive).
				 */
				if(!isValidAction(fieldDetails.getImportAction()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.excel.unknown.action", String.valueOf(rowNo)));
					errorCount++;
				}

				/*
				 * NEW: reject duplicate rows (same unique combination) within the file BEFORE any DB work.
				 */
				if(null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()))
				{
					String uniqueKey = fieldDetails.getManualTypeCode().trim().toLowerCase();
					if(seenUniqueKeys.contains(uniqueKey))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.manualtype"), String.valueOf(rowNo)));
						errorCount++;
					}
					else
					{
						seenUniqueKeys.add(uniqueKey);
					}
					uniqueKey = null;
				}

				if(null==fieldDetails.getManualTypeRefKey() || "".equals(fieldDetails.getManualTypeRefKey()) ||
						null==fieldDetails.getManualTypeCode() || "".equals(fieldDetails.getManualTypeCode()) ||
						null==fieldDetails.getManualTypeName() || "".equals(fieldDetails.getManualTypeName()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.manualtype")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getManualTypeCode() && !"".equals(fieldDetails.getManualTypeCode()))
				{
					if(fieldDetails.getManualTypeCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.manualtypecode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getManualTypeName() && !"".equals(fieldDetails.getManualTypeName()))
				{
					if(fieldDetails.getManualTypeName().length()>150)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.manualtypename")+",150,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getManualTypeRefKey() && !"".equals(fieldDetails.getManualTypeRefKey()))
				{
					if(fieldDetails.getManualTypeRefKey().length()>150)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.manualtyperefkey")+",150,"+String.valueOf(rowNo);
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.manualtype")));
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

	private static void decideErrorDisplay(ManualTypeBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_MANUALTYPE_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(ManualType.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(ManualType.class.getName(), "validateExcelRowData()", e);
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
		
	private static void saveManualTypeDetails(ManualTypeBean sessionBean)
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
				boolean bool = ManualTypeDAO.saveManualTypeDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveManualTypeDetails :: ManualType Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.manualtype")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setManualTypeList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getManualTypeList(sessionBean);
				}
				else
				{
					logger.info("saveManualTypeDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.manualtype")));
				}
			}
			else
			{
				logger.info("saveManualTypeDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "saveManualTypeDetails()", e);
		}
	}
	
	private static void editManualTypeDetails(HttpServletRequest request, ManualTypeBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getManualTypeList() && !"".equals(sessionBean.getManualTypeList().size()>0))
			{
				for(int a=0;a<sessionBean.getManualTypeList().size();a++)
				{
					ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getManualTypeList() && !"".equals(sessionBean.getManualTypeList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getManualTypeList().size();a++)
							{
								ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
								if(rowId.equals(String.valueOf(details.getManualTypeId())))
								{
									logger.info("editManualTypeDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
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
				logger.info("editManualTypeDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "editManualTypeDetails()", e);
		}
	}

	private static void deleteManualTypeDetails(HttpServletRequest request, ManualTypeBean sessionBean)
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
					boolean bool = ManualTypeDAO.deleteManualTypeDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.manualtype")));
						/*
						 * call getManualTypeList
						 */
						getManualTypeList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.manualtype")));
					}
				}
			}
			else
			{
				logger.info("deleteManualTypeDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "deleteManualTypeDetails()", e);
		}
	}

	private static void activeManualTypeDetails(HttpServletRequest request, ManualTypeBean sessionBean)
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
					boolean bool = ManualTypeDAO.activeManualTypeDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.manualtype")));
						/*
						 * call getMissionBookList
						 */
						getManualTypeList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.manualtype")));
					}
				}
			}
			else
			{
				logger.info("activeManualTypeDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "activeManualTypeDetails()", e);
		}
	}
	
	private static void updateManualTypeDetails(HttpServletRequest request, ManualTypeBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<ManualTypeDetails> updateDataList = new ArrayList<ManualTypeDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getManualTypeList() && sessionBean.getManualTypeList().size()>0)
					{
						for(int i=0;i<sessionBean.getManualTypeList().size();i++)
						{
							ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setManualTypeCode("");
								details.setManualTypeName("");
								details.setManualTypeRefKey("");
								details.setFlag("");

								/*
								 * fetch the values from request
								 * and set in manualTypeList
								 */
								String code="MAN_TYPE_List_Code"+String.valueOf(details.getManualTypeId());
								String name="MAN_TYPE_List_Name_Eng"+String.valueOf(details.getManualTypeId());
								String refKey="MAN_TYPE_List_RefKey"+String.valueOf(details.getManualTypeId());
								String flag="MAN_TYPE_List_Flag_"+String.valueOf(details.getManualTypeId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(code))
										{
											details.setManualTypeCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getManualTypeCode() && !"".equals(details.getManualTypeCode()))
											{
												details.setManualTypeCode(details.getManualTypeCode().trim());
											}
										}
										else if(key.equals(name))
										{
											details.setManualTypeName(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getManualTypeName() && !"".equals(details.getManualTypeName()))
											{
												details.setManualTypeName(details.getManualTypeName().trim());
											}
										}
										else if(key.equals(refKey))
										{
											details.setManualTypeRefKey(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getManualTypeRefKey() && !"".equals(details.getManualTypeRefKey()))
											{
												details.setManualTypeRefKey(details.getManualTypeRefKey().trim());
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
								refKey = null;
							}
						}

						for(int i=0;i<sessionBean.getManualTypeList().size();i++)
						{
							ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(i);
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
							logger.info("updateManualTypeDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							Connection conn = null;
							String closeConnection="N";
							try
							{
								conn  = DBConnectionHelper.getConnection();
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(ManualType.class.getName(), "updateManualTypeDetails()", e);
							}
							
							for(int a=0;a<updateDataList.size();a++)
							{
								ManualTypeDetails details = (ManualTypeDetails)updateDataList.get(a);
								boolean updateFlag = ManualTypeDAO.updateManualTypeDetails(details, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateManualTypeDetails :: ManualType Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateManualTypeDetails :: Failed to Update ManualType Details for Row No :: >  "+ details.getSrNo());
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
								Utilities.printStackTraceToLogs(ManualType.class.getName(), "updateManualTypeDetails()", e);
							}
							conn = null;
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.manualtype"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.manualtype"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateManualTypeDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setManualTypeList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getManualTypeList
								 */
								getManualTypeList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateManualTypeDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getManualTypeList() && sessionBean.getManualTypeList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getManualTypeList().size();a++)
										{
											ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "updateManualTypeDetails()", e);
		}
	}
	
	private static void exportManualTypeDetails(HttpServletRequest request, ManualTypeBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<ManualTypeDetails> exportDataList = new ArrayList<ManualTypeDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getManualTypeList() && !"".equals(sessionBean.getManualTypeList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getManualTypeList().size();a++)
							{
								ManualTypeDetails details = (ManualTypeDetails)sessionBean.getManualTypeList().get(a);
								if(rowId.equals(String.valueOf(details.getManualTypeId())))
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
					writeManualTypeExcel(exportDataList, sessionBean);
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
					logger.info("exportManualTypeDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportManualTypeDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "exportManualTypeDetails()", e);
		}
	}

	private static void readExcelData(byte[] data, ManualTypeBean sessionBean, String extension)
	{
		sessionBean.setManualTypeListToImport(new ArrayList<ManualTypeDetails>());
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
						ManualTypeDetails details = new ManualTypeDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setManualTypeCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setManualTypeName(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setManualTypeRefKey(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * Values: A/ADD/Add (create), U/update/Update (update), D/delete/Delete (soft delete).
						 * Blank / any non-delete value falls through to the existing create-or-update path.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * add details to manualTypeList for import
						 */
						if(null==sessionBean.getManualTypeListToImport() || sessionBean.getManualTypeListToImport().size()<=0)
						{
							sessionBean.setManualTypeListToImport(new ArrayList<ManualTypeDetails>());
						}

						sessionBean.getManualTypeListToImport().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsWorkBook = null;
				xlsSheet= null;
				rowIterator=  null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "readExcelData()", e);
		}
	}

	private static void writeManualTypeExcel(ArrayList<ManualTypeDetails> manualTypeList, ManualTypeBean sessionBean)
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
				 * So the final Name will be - US_EN-US_ND_MANUALTYPE_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_MANUALTYPE_NAME");
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
				codeCell.setCellValue("MANUAL TYPE CODE");
				Cell nameCell = headerRow.createCell(1);
				nameCell.setCellValue("MANUAL TYPE NAME");
				Cell refKeyCell = headerRow.createCell(2);
				refKeyCell.setCellValue("MANUAL TYPE REF KEY");
				
				int rowCount=0;
				if(null!=manualTypeList && manualTypeList.size()>0)
				{
					for(int i=0;i<manualTypeList.size();i++)
					{
						ManualTypeDetails details = (ManualTypeDetails)manualTypeList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						
						if(null!=details.getManualTypeCode() && !"".equals(details.getManualTypeCode()))
						{
							cell0.setCellValue(details.getManualTypeCode());
						}
						if(null!=details.getManualTypeName() && !"".equals(details.getManualTypeName()))
						{
							cell1.setCellValue(details.getManualTypeName());
						}
						if(null!=details.getManualTypeRefKey() && !"".equals(details.getManualTypeRefKey()))
						{
							cell2.setCellValue(details.getManualTypeRefKey());
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
					refKeyCell= null;
					
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "writeManualTypeExcel()", e);
		}
	}

	private static void executeExcelOperation(ManualTypeBean sessionBean, byte[] data, String extension)
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
				ArrayList<ManualTypeDetails> listToSave = new ArrayList<ManualTypeDetails>();
				for(int i=0;i<sessionBean.getManualTypeListToImport().size();i++)
				{
					ManualTypeDetails fieldDetails = (ManualTypeDetails)sessionBean.getManualTypeListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							ManualTypeDetails existingDetails = (ManualTypeDetails)listToSave.get(j);
							/*
							 * IDENTIY DUPLICATE ON THE BASIS OF 
							 *  ABBREVIATION CODE
							 */
							if(fieldDetails.getManualTypeRefKey().equals(existingDetails.getManualTypeRefKey()) && 
									fieldDetails.getManualTypeCode().equals(existingDetails.getManualTypeCode()) && 
									fieldDetails.getManualTypeName().equals(existingDetails.getManualTypeName()))
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.manualtype"), String.valueOf(rows[a])));
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
					 * CLASSIFY EACH DEDUPED ROW BY THE ACTION / MARKER COLUMN AND STAMP LOCALE / LANGUAGE IDENTITY.
					 * A / ADD / U / UPDATE / blank -> create-or-update on the unique combination.
					 * D / DELETE                    -> soft-delete the row matched by the same unique combination.
					 */
					ArrayList<ManualTypeDetails> saveList = new ArrayList<ManualTypeDetails>();
					ArrayList<ManualTypeDetails> deleteList = new ArrayList<ManualTypeDetails>();
					for(int i=0;i<listToSave.size();i++)
					{
						ManualTypeDetails fieldDetails = (ManualTypeDetails)listToSave.get(i);
						fieldDetails.setCountryLocaleCode(countryLocaleCode);
						fieldDetails.setManualLanguageCode(languageCode);
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						if(isDeleteAction(fieldDetails.getImportAction()))
						{
							deleteList.add(fieldDetails);
						}
						else
						{
							saveList.add(fieldDetails);
						}
						fieldDetails= null;
					}

					/*
					 * ONE connection / ONE transaction for the whole file (see ManualTypeDAO.importManualTypeDetails).
					 */
					Connection conn = null;
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(ManualType.class.getName(), "executeExcelOperation()", e);
					}

					ImportActionResult importResult = ManualTypeDAO.importManualTypeDetails(saveList, deleteList, conn);

					try
					{
						if(null!=conn)
						{
							conn.close();
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(ManualType.class.getName(), "executeExcelOperation()", e);
					}
					conn = null;

					boolean reloadList = false;
					String successMessage = "";

					/*
					 * CREATE / UPDATE SUCCESS (A / U / blank rows)
					 */
					int successCount = importResult.getSaveSuccessRows().size();
					if(successCount>0)
					{
						successMessage = msgProps.addMessage("import.success.count.message", String.valueOf(successCount));
						reloadList = true;
					}

					/*
					 * DELETE SUCCESS (D rows) - shown alongside the create/update success, space separated.
					 * (The success message div renders a single escaped string, so no <MSG_TOKEN> here.)
					 */
					int deleteSuccessCount = importResult.getDeleteSuccessRows().size();
					if(deleteSuccessCount>0)
					{
						String delSuccess = msgProps.addMessage("import.delete.success.count.message", String.valueOf(deleteSuccessCount));
						if(null!=successMessage && !"".equals(successMessage))
						{
							successMessage = successMessage+" "+delSuccess;
						}
						else
						{
							successMessage = delSuccess;
						}
						delSuccess = null;
						reloadList = true;
					}

					if(null!=successMessage && !"".equals(successMessage))
					{
						sessionBean.setSuccessMessage(successMessage);
					}
					successMessage = null;

					/*
					 * CREATE / UPDATE FAILURES
					 */
					if(null!=importResult.getSaveFailureRows() && importResult.getSaveFailureRows().size()>0)
					{
						failureCount = failureCount + importResult.getSaveFailureRows().size();
						for(int a=0;a<importResult.getSaveFailureRows().size();a++)
						{
							errorCount++;
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.manualtype"), String.valueOf(importResult.getSaveFailureRows().get(a))));
						}
					}

					/*
					 * DELETE FAILURES (no active row for the unique combination, or a DB error)
					 */
					if(null!=importResult.getDeleteFailureRows() && importResult.getDeleteFailureRows().size()>0)
					{
						failureCount = failureCount + importResult.getDeleteFailureRows().size();
						for(int a=0;a<importResult.getDeleteFailureRows().size();a++)
						{
							errorCount++;
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.manualtype"), String.valueOf(importResult.getDeleteFailureRows().get(a))));
						}
					}

					if(reloadList==true)
					{
						/*
						 * reload the grid with the updated data
						 */
						getManualTypeList(sessionBean);
					}

					countryLocaleCode=  null;
					languageCode= null;
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.manualtype")));
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "executeExcelOperation()", e);
		}
	}

	/*
	 * Resolve the Action / Marker column value. Only an explicit delete marker
	 * (D / DELETE / Delete, case-insensitive) routes a row to soft-delete; blank or any
	 * other value keeps the existing create-or-update behavior.
	 */
	private static boolean isDeleteAction(String action)
	{
		if(null!=action && !"".equals(action.trim()))
		{
			String a = action.trim().toLowerCase();
			if(a.equals("d") || a.equals("delete"))
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * Valid Action / Marker column values: blank (create-or-update), A/ADD (create),
	 * U/UPDATE (update), D/DELETE (soft delete). Any other value is rejected during Excel
	 * validation, before any DB operation. Comparison is case-insensitive.
	 */
	private static boolean isValidAction(String action)
	{
		if(null==action || "".equals(action.trim()))
		{
			// blank is allowed - falls through to create-or-update
			return true;
		}
		String a = action.trim().toLowerCase();
		if(a.equals("a") || a.equals("add") || a.equals("u") || a.equals("update")
				|| a.equals("d") || a.equals("delete"))
		{
			return true;
		}
		return false;
	}

	private static void getManualTypeList(ManualTypeBean sessionBean)
	{
		try
		{
			sessionBean.setManualTypeList(new ArrayList<ManualTypeDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<ManualTypeDetails> list = new ArrayList<ManualTypeDetails>();
				list = ManualTypeDAO.getManualTypeDetailsList(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						ManualTypeDetails details = (ManualTypeDetails)list.get(i);
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
					sessionBean.setManualTypeList(list);
				}
				list = null;
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "getManualTypeList()", e);
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
	
	private void performAccessCheck(ManualTypeBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(ManualType.class.getName(), "performAccessCheck()", e);
		}
	}

}
