package com.mazda.gms3.mdm.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mazda.gms3.mdm.bean.SITranslationUpdateBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.dao.TranslationUpdateDAO;
import com.mazda.gms3.mdm.im.impl.TranslationExportProcessingImpl;
import com.mazda.gms3.mdm.im.impl.TranslationImportProcessingImpl;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SITranslationScheduleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class SITranslationUpdate
 */
public class SITranslationUpdate extends HttpServlet {
	private static final long serialVersionUID = 1L;


	Logger logger = LogManager.getLogger(SITranslationUpdate.class);

	MessageProperties msgProps= null;
	String wslId="";
	String moduleRefKey=AccessManagementInterface.REF_KEY_SI_TRANSLATION_UPDATE;
	String selectedIds=null;
	String abortSchId=null;
	boolean invalidXMLsInZip=false;
	int fileCount=0;
	String folderPath=null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SITranslationUpdate() {
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
			SITranslationUpdateBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}


			sessionBean.setCountryLocaleId(null);
			sessionBean.setCountryLocaleList(null);
			sessionBean.setActionClicked(null);
			sessionBean.setDocumentIdInput(null);
			sessionBean.setDocumentIdList(null);
			sessionBean.setFailedDocumentIdList(null);
			sessionBean.setTotalDocumentIdList(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setLanguageList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setOperationTypeList(null);
			sessionBean.setOperationTypeSelected(AccessManagementInterface.OPERATION_TYPE_EXPORT);
			sessionBean.setShowExportBlock(false);
			sessionBean.setShowImportBlock(false);
			sessionBean.setScheduleList(null);
			sessionBean.setDocumentIdListToImport(null);
			sessionBean.setImportFilesCount(null);
			/*
			 * call function to get CountryLocaleList
			 */
			getCountryLocaleList(sessionBean, request);

			/*
			 * call function to get OperationTypeList
			 */
			getOperationTypeList(sessionBean);

			/*
			 * call function to show hide Import / Export Block
			 */
			showHideImportExportBlock(sessionBean);

			/*
			 * get scheduleList
			 */
			getScheduleList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mme/sitranslationupdate.jsp");
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
			SITranslationUpdateBean sessionBean = getSessionBean(request);
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

			/*
			 * read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);

			/*
			 * call function to show hide Import / Export Block
			 */
			showHideImportExportBlock(sessionBean);


			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_LOCALE_SELECTION"))
				{
					sessionBean.setManualLanguageId(null);
					/*
					 * call function to get Language List
					 */
					getLanguageList(sessionBean, request);
				}
				else if(sessionBean.getActionClicked().equals("VALIDATE_EXPORT_IDS"))
				{
					validateDocumentIdInputOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SCHEDULE_NOW"))
				{
					if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
					{
						scheduleJobExportOperation(sessionBean);
					}
					else if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_IMPORT))
					{
						scheduleJobImportOperation(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("REFRESH"))
				{
					getScheduleList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SCHEDULE"))
				{
					deleteScheduleTransactions(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ABORT_SCHEDULE"))
				{
					abortSchedule(sessionBean);
				}
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mme/sitranslationupdate.jsp");
				rs.forward(request, response);
			}
		}
	}

	private SITranslationUpdateBean getSessionBean(HttpServletRequest request) 
	{
		SITranslationUpdateBean sessionBean = null;
		if (null != request.getSession().getAttribute("siTranslationUpdateBean") && !"".equals(request.getSession().getAttribute("siTranslationUpdateBean"))) 
		{
			sessionBean = (SITranslationUpdateBean) request.getSession().getAttribute("siTranslationUpdateBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SITranslationUpdateBean();
			request.getSession().setAttribute("siTranslationUpdateBean", sessionBean);
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

	private void performAccessCheck(SITranslationUpdateBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "performAccessCheck()", e);
		}
	}

	private void getOperationTypeList(SITranslationUpdateBean sessionBean)
	{
		sessionBean.setOperationTypeList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(AccessManagementInterface.OPERATION_TYPE_EXPORT);
		si.setValue(AccessManagementInterface.OPERATION_TYPE_EXPORT);
		sessionBean.getOperationTypeList().add(si);
		si = null;

		si = new SelectItemDetails();
		si.setLabel(AccessManagementInterface.OPERATION_TYPE_IMPORT);
		si.setValue(AccessManagementInterface.OPERATION_TYPE_IMPORT);
		sessionBean.getOperationTypeList().add(si);
		si = null;
	}

	private void showHideImportExportBlock(SITranslationUpdateBean sessionBean)
	{	
		sessionBean.setShowImportBlock(false);
		sessionBean.setShowExportBlock(false);
		if(null!=sessionBean.getOperationTypeSelected() && !"".equals(sessionBean.getOperationTypeSelected()))
		{
			if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
			{
				sessionBean.setShowExportBlock(true);
			}
			else if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_IMPORT))
			{
				sessionBean.setShowImportBlock(true);
			}
		}
	}

	private void getCountryLocaleList(SITranslationUpdateBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private void getLanguageList(SITranslationUpdateBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> list = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId());
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
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "getLanguageList()", e);
		}
	}

	private void readParamsFromRequest(SITranslationUpdateBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setActionClicked(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setOperationTypeSelected(null);
			sessionBean.setDocumentIdInput(null);
			sessionBean.setScheduleDisplayPageLength(null);
			sessionBean.setScheduleDisplayPageNo(null);
			selectedIds=  null;
			abortSchId = null;

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
						if (fieldName.equals("SITU_Schedule_displayPageNo")) 
						{
							// set the value in sessionBean.setScheduleDisplayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setScheduleDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SITU_Schedule_displayPageLen")) 
						{
							// set the value in sessionBean.setScheduleDisplayPageLength
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setScheduleDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}

						if(fieldName.equals("SITU_Sch_SelectedRows"))
						{
							// set the value in selectedIds
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								selectedIds = value;
							}
							// set value to null
							value = null;
						}

						if(fieldName.equals("SITU_AbortSchId"))
						{
							// set the value in abortSchId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								abortSchId = value;
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SITU_CountryLocale_Code")) 
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

						if (fieldName.equals("SITU_Lang_Code")) 
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

						if (fieldName.equals("SITU_OperationType")) 
						{
							// set the value in sessionBean.setOperationTypeSelected
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setOperationTypeSelected(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SITU_ActionClicked")) 
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

						if (fieldName.equals("SITU_DocumentIds")) 
						{
							// set the value in sessionBean.setDocumentIdInput
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDocumentIdInput(value);
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
						if(null!=sessionBean.getOperationTypeSelected() && sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
						{
							if(validateFileUpload(sessionBean))
							{
								String fileName = fileItem.getName();
								byte[] data = fileItem.get();
								if(null!=fileName && !"".equals(fileName) && null!=data && data.length>0)
								{
									/*
									 * check for Excel
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
						else if(null!=sessionBean.getOperationTypeSelected() && sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_IMPORT))
						{
							if(validateFileUpload(sessionBean))
							{
								String fileName = fileItem.getName();
								byte[] data = fileItem.get();
								if(null!=fileName && !"".equals(fileName) && null!=data && data.length>0)
								{
									/*
									 * check for ZIP
									 */
									String extension="";
									if(fileName.lastIndexOf(".")!=-1)
									{
										extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
										if(null!=extension && !"".equals(extension))
										{
											if(extension.trim().toLowerCase().equals("zip"))
											{
												// CHECK FOR SIZE NOT MORE THAN 5 MB
												if(data.length <= (5*1024*1024))
												{
													/*
													 * call function to upload zip to a temp directory 
													 * unzip it and identify all the XMLs
													 */
													executeZIPOperation(sessionBean, data);
												}
												else
												{
													logger.info("readParamsFromRequest() :: File Size of Zip file is higher. Max size supported is 5 MB. Throw Error."); 
													sessionBean.setErrorMessage(msgProps.getProperty("error.zip.file.length"));
												}
											}
											else if(extension.trim().toLowerCase().equals("xml"))
											{
//												if(fileName.trim().toLowerCase().startsWith("si") || fileName.trim().toLowerCase().startsWith("sm"))
												{
													/*
													 * execute XML Operation
													 */
													executeXMLOperation(sessionBean, data, fileName);
												}
//												else
//												{
//													logger.info("readParamsFromRequest :: Uploaded XML is invalid. Translations Update for SI & SM channels are only allowed.");
//													sessionBean.setErrorMessage(msgProps.getProperty("error.upload.xml.file"));
//												}
											}
											else
											{
												logger.info("readParamsFromRequest() :: Extension is not ZIP or XML. Throw Message - Uploaded file not supported."); 
												sessionBean.setErrorMessage(msgProps.getProperty("error.valid.zip.or.xml"));
											}
										}
										else
										{
											logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid Zip / XML File.");
											sessionBean.setErrorMessage(msgProps.getProperty("error.valid.zip.or.xml"));
										}
									}
									else
									{
										logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid Zip / XML File.");
										sessionBean.setErrorMessage(msgProps.getProperty("error.valid.zip.or.xml"));
									}
									extension=null;
								}
								else
								{
									logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid Zip / XML File.");
									sessionBean.setErrorMessage(msgProps.getProperty("error.valid.zip.or.xml"));
								}
								fileName=  null;
								data = null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "readParamsFromRequest()", e);
		}
	}

	private boolean validateFileUpload(SITranslationUpdateBean sessionBean)
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

	private void readExcelData(byte[] data, SITranslationUpdateBean sessionBean,String extension)
	{
		sessionBean.setDocumentIdListToImport(new ArrayList<String>());
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
				 * DOCUMENT ID
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							if(null==sessionBean.getDocumentIdListToImport() || sessionBean.getDocumentIdListToImport().size()<=0)
							{
								sessionBean.setDocumentIdListToImport(new ArrayList<String>());
							}
							sessionBean.getDocumentIdListToImport().add(String.valueOf(dataCell).trim());
						}
						dataCell = null;
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
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "readExcelData()", e);
		}
	}

	private boolean validateExcelRowData(SITranslationUpdateBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getDocumentIdListToImport() && sessionBean.getDocumentIdListToImport().size()>0)
		{
			String documentId=null;
			for(int i=0;i<sessionBean.getDocumentIdListToImport().size();i++)
			{
				documentId = "";
				documentId= String.valueOf(sessionBean.getDocumentIdListToImport().get(i));
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;

				if(null==documentId || "".equals(documentId))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR DOCUMENT AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.document")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=documentId && !"".equals(documentId))
				{
					if(!documentId.trim().toLowerCase().startsWith("si") && !documentId.trim().toLowerCase().startsWith("sm"))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.document")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines.translation"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				documentId = null;
			}
			documentId = null;
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.document")));
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

	private void decideErrorDisplay(SITranslationUpdateBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_SITRANSLATION_UPDATE")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "validateExcelRowData()", e);
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

	private void executeExcelOperation(SITranslationUpdateBean sessionBean, byte[] data, String extension)
	{
		sessionBean.setDocumentIdList(new ArrayList<String>());
		sessionBean.setFailedDocumentIdList(new ArrayList<String>());
		sessionBean.setTotalDocumentIdList(new ArrayList<String>());
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
				ArrayList<String> listToSave = new ArrayList<String>();
				String documentId="";
				String existingDocumentId="";
				int rowNo=0;
				for(int i=0;i<sessionBean.getDocumentIdListToImport().size();i++)
				{
					documentId = "";
					documentId = String.valueOf(sessionBean.getDocumentIdListToImport().get(i));
					rowNo= (i+1+1);
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						existingDocumentId=  null;
						for(int j=0;j<listToSave.size();j++)
						{
							existingDocumentId = "";
							existingDocumentId = String.valueOf(listToSave.get(j));
							/*
							 * IDENTIY DUPLICATE ON THE BASIS OF 
							 *  ABBREVIATION CODE
							 */
							if(existingDocumentId.trim().toLowerCase().equals(documentId.trim().toLowerCase()))
							{
								if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
								{
									duplicateRowNo = duplicateRowNo+",";
								}
								duplicateRowNo = duplicateRowNo+String.valueOf(rowNo);
								// already Added = SKIP IT
								addToList = false;
								break;
							}
							existingDocumentId = null;
						}
					}
					if(addToList==true)
					{
						// before adding if document starts with si & sm or not
						if(documentId.trim().toLowerCase().startsWith("si") || documentId.trim().toLowerCase().startsWith("sm"))
						{
							listToSave.add(documentId);
						}
					}
					documentId = null;
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.document"), String.valueOf(rows[a])));
						}
					}
					rows = null;
				}
				duplicateRowNo = null;

				if(null!=listToSave && listToSave.size()>0)
				{
					
					String localeCode="";
					if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
					{
						if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
						{
							ManualLanguageDetails mlDetails = null;
							for(int r=0;r<sessionBean.getLanguageList().size();r++)
							{
								mlDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
								if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
								{
									localeCode= mlDetails.getManualLanguageName();
									break;
								}
								mlDetails = null;
							}
						}
					}
					/*
					 * SET THIS LIST DOCUMENT ID LIST
					 * VERIFY WHETHER EACH DOCUMENT EXISTS FOR THE SELECTED LOCALE OR NOT
					 */
					if(null!=localeCode && !"".equals(localeCode))
					{
						localeCode= localeCode.replace("-", "_");
					}
					List<Map<String, String>> docsList = new ArrayList<Map<String,String>>();
					Map<String, String> docMap = null;
					for (int a=0;a<listToSave.size();a++)
					{
						docMap=  new HashMap<String, String>();
						docMap.put("DOC_ID", String.valueOf(listToSave.get(a)));
						docMap.put("LOCALE", localeCode);
						// Set default status as N
						docMap.put("STATUS", "N");
						docsList.add(docMap);
						docMap=  null;
						
						// add to totalDocsList as well
						if(null==sessionBean.getTotalDocumentIdList() || sessionBean.getTotalDocumentIdList().size()<=0)
						{
							sessionBean.setTotalDocumentIdList(new ArrayList<String>());
						}
						sessionBean.getTotalDocumentIdList().add(String.valueOf(listToSave.get(a)));
					}
					
					if(null!=docsList && docsList.size()>0)
					{
						docsList = TranslationUpdateDAO.verifyDocuments(docsList);
						/*
						 * ITERATE LIST & CHECK IF ALL DOCUMENT EXISTS IN THE SELECTED LOCALE OR NOT
						 */
						docMap = null;
						if(null!=docsList && docsList.size()>0)
						{
							for(int b=0;b<docsList.size();b++)
							{
								docMap = (Map<String, String>)docsList.get(b);
								if(null!=docMap && null!=docMap.get("DOC_ID") && !"".equals(docMap.get("DOC_ID")) && 
										null!=docMap.get("LOCALE") && !"".equals(docMap.get("LOCALE")))
								{
									if(null!=docMap.get("STATUS") && docMap.get("STATUS").toString().equals("Y"))
									{
										// INCREMENT SUCCESS COUNT
										if(null==sessionBean.getDocumentIdList() || sessionBean.getDocumentIdList().size()<=0)
										{
											sessionBean.setDocumentIdList(new ArrayList<String>());
										}
										sessionBean.getDocumentIdList().add(docMap.get("DOC_ID").toString());
									}
									else
									{
										// ADD ERROR MESSAGE AS WELL
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										// increment erroCount by 1
										errorCount++;
										errorMessage.append(docMap.get("DOC_ID").toString()+" does not exist for "+docMap.get("LOCALE").toString()+" Locale in Kapture.");
										
										if(null==sessionBean.getFailedDocumentIdList() || sessionBean.getFailedDocumentIdList().size()<=0)
										{
											sessionBean.setFailedDocumentIdList(new ArrayList<String>());
										}
										sessionBean.getFailedDocumentIdList().add(docMap.get("DOC_ID").toString());
									}
								}
								docMap=  null;
							}
						}
						
					}
					docsList = null;
//					sessionBean.setDocumentIdList(listToSave);
					// set docmentIdListToImport to null
					sessionBean.setDocumentIdListToImport(null);
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.document")));
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
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "executeExcelOperation()", e);
		}
	}

	private boolean validateExportDocumentInputIds(SITranslationUpdateBean sessionBean)
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
		
		if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
		{
			if((null==sessionBean.getDocumentIdInput() || "".equals(sessionBean.getDocumentIdInput())))
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.mandatory.documentids", msgProps.getProperty("label.documentid")));
			}
			
			if(null!=sessionBean.getDocumentIdInput() && !"".equals(sessionBean.getDocumentIdInput()))
			{
				int count=0;
				// MAX 5 DOCUMENT ID'S ALLOWED
				String[] tok = sessionBean.getDocumentIdInput().split(",");
				if(null!=tok && tok.length>0)
				{
					for(int a=0;a<tok.length;a++)
					{
						if(null!=tok[a] && !"".equals(tok[a]) && !"null".equals(tok[a].trim().toLowerCase()))
						{
							count++;
						}
					}
					if(count > 5)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.getProperty("error.max.allowed.documents.translation"));
					}
					else
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=tok[a] && !"".equals(tok[a]) && !"null".equals(tok[a].trim().toLowerCase()))
							{
								// CHECK, THERE SHOULD BE ONLY SI & SM DOCUMENT ID
								if(!tok[a].trim().toLowerCase().startsWith("si") && !tok[a].trim().toLowerCase().startsWith("sm"))
								{
									// 
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append(msgProps.addMessage("error.invalid.docid.translation", tok[a].trim().toUpperCase()));
								}
							}
						}
					}
				}
				tok = null;
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
	
	private boolean validateSchedule(SITranslationUpdateBean sessionBean)
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

		if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
		{
			if((null==sessionBean.getDocumentIdList() || sessionBean.getDocumentIdList().size()<=0))
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
//				errorMessage.append(msgProps.addMessage("error.mandatory.documentids", msgProps.getProperty("label.documentid")));
				errorMessage.append(msgProps.getProperty("error.export.translation.schedule.job"));
			}
		}
		else if(sessionBean.getOperationTypeSelected().equals(AccessManagementInterface.OPERATION_TYPE_IMPORT))
		{
			if(fileCount==0)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific",msgProps.getProperty("label.zip")+" / "+ msgProps.getProperty("label.xmls")));
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

	private void validateDocumentIdInputOperation(SITranslationUpdateBean sessionBean)
	{
		sessionBean.setTotalDocumentIdList(new ArrayList<String>());
		sessionBean.setDocumentIdList(new ArrayList<String>());
		sessionBean.setFailedDocumentIdList(new ArrayList<String>());
		try
		{
			if(validateExportDocumentInputIds(sessionBean))
			{
				if(null!=sessionBean.getDocumentIdInput() && !"".equals(sessionBean.getDocumentIdInput()))
				{
					ArrayList<String> listToSave = new ArrayList<String>();
					String[]  tok = sessionBean.getDocumentIdInput().split(",");
					if(null!=tok && tok.length>0)
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=tok[a] && !"".equals(tok[a]) && !"null".equals(tok[a].trim().toLowerCase()))
							{
								boolean add = true;
								if(null!=listToSave && listToSave.size()>0)
								{
									for(int b=0;b<listToSave.size();b++)
									{
										if(listToSave.get(b).toString().trim().toLowerCase().equals(tok[a].trim().toLowerCase()))
										{
											add = false;
											break;
										}
									}
								}

								if(add==true)
								{
									listToSave.add(tok[a].trim().toUpperCase());
								}
							}
						}
						
						if(null!=listToSave && listToSave.size()>0)
						{
							/*
							 * CHECK FOR EACH DOCUMENT WHETHER EXISTS IN IM FOR THE SELECTED LOCALE
							 */
							String localeCode="";
							if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
							{
								if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
								{
									ManualLanguageDetails mlDetails = null;
									for(int r=0;r<sessionBean.getLanguageList().size();r++)
									{
										mlDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
										if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
										{
											localeCode= mlDetails.getManualLanguageName();
											break;
										}
										mlDetails = null;
									}
								}
							}
							/*
							 * SET THIS LIST DOCUMENT ID LIST
							 * VERIFY WHETHER EACH DOCUMENT EXISTS FOR THE SELECTED LOCALE OR NOT
							 */
							if(null!=localeCode && !"".equals(localeCode))
							{
								localeCode= localeCode.replace("-", "_");
							}
							List<Map<String, String>> docsList = new ArrayList<Map<String,String>>();
							Map<String, String> docMap = null;
							for (int a=0;a<listToSave.size();a++)
							{
								docMap=  new HashMap<String, String>();
								docMap.put("DOC_ID", String.valueOf(listToSave.get(a)));
								docMap.put("LOCALE", localeCode);
								// Set default status as N
								docMap.put("STATUS", "N");
								docsList.add(docMap);
								docMap=  null;
								
								// add to totalDocsList as well
								if(null==sessionBean.getTotalDocumentIdList() || sessionBean.getTotalDocumentIdList().size()<=0)
								{
									sessionBean.setTotalDocumentIdList(new ArrayList<String>());
								}
								sessionBean.getTotalDocumentIdList().add(String.valueOf(listToSave.get(a)));
							}
							
							if(null!=docsList && docsList.size()>0)
							{
								docsList = TranslationUpdateDAO.verifyDocuments(docsList);
								/*
								 * ITERATE LIST & CHECK IF ALL DOCUMENT EXISTS IN THE SELECTED LOCALE OR NOT
								 */
								int errorCount=0;
								StringBuilder errorMessage = new StringBuilder();
								docMap = null;
								if(null!=docsList && docsList.size()>0)
								{
									for(int b=0;b<docsList.size();b++)
									{
										docMap = (Map<String, String>)docsList.get(b);
										if(null!=docMap && null!=docMap.get("DOC_ID") && !"".equals(docMap.get("DOC_ID")) && 
												null!=docMap.get("LOCALE") && !"".equals(docMap.get("LOCALE")))
										{
											if(null!=docMap.get("STATUS") && docMap.get("STATUS").toString().equals("Y"))
											{
												// INCREMENT SUCCESS COUNT
												if(null==sessionBean.getDocumentIdList() || sessionBean.getDocumentIdList().size()<=0)
												{
													sessionBean.setDocumentIdList(new ArrayList<String>());
												}
												sessionBean.getDocumentIdList().add(docMap.get("DOC_ID").toString());
											}
											else
											{
												// ADD ERROR MESSAGE AS WELL
												if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
												{
													errorMessage.append("<MSG_TOKEN>");
												}
												// increment erroCount by 1
												errorCount++;
												errorMessage.append(docMap.get("DOC_ID").toString()+" does not exist for "+docMap.get("LOCALE").toString()+" Locale in Kapture.");
												
												if(null==sessionBean.getFailedDocumentIdList() || sessionBean.getFailedDocumentIdList().size()<=0)
												{
													sessionBean.setFailedDocumentIdList(new ArrayList<String>());
												}
												sessionBean.getFailedDocumentIdList().add(docMap.get("DOC_ID").toString());
											}
										}
										docMap=  null;
									}
								}
								if(errorCount > 0)
								{
									decideErrorDisplay(sessionBean, errorMessage, errorCount);
								}
							}
							docsList = null;
							localeCode=  null;
						}
						listToSave = null;
					}
					tok = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "validateDocumentIdInputOperation()", e);
		}
	}

	private void scheduleJobExportOperation(SITranslationUpdateBean sessionBean)
	{
		try
		{
			if(validateSchedule(sessionBean))
			{
				/*
				 * check here, if documentId List from sessionBean is null
				 * 	look for documentId Input text and add to final docs List
				 */
				ArrayList<String> docsList = new ArrayList<String>();
				if(null!=sessionBean.getDocumentIdList() && !"".equals(sessionBean.getDocumentIdList()))
				{
					docsList = sessionBean.getDocumentIdList();
				}

				// PROCEED ONLY WHEN DOC IDS ARE NOT NULL
				if(null!=docsList && docsList.size()>0)
				{
					// CRETAE SCHEDULE
					SITranslationScheduleDetails schDetails = new SITranslationScheduleDetails();
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						ManualLanguageDetails mlDetails = null;
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(sessionBean.getManualLanguageId().trim().equals(String.valueOf(mlDetails.getManualLanguageId())))
							{
								schDetails.setLocaleCode(mlDetails.getManualLanguageName());
								break;
							}
							mlDetails=  null;
						}
						mlDetails=  null;
					}

					schDetails.setTotalCount(docsList.size());
					schDetails.setScheduleName(ApplicationProperties.getProperty("translation.update.job.key"));
					schDetails.setThreadId(ApplicationProperties.getProperty("translation.update.job.key"));
					// set as Pending
					schDetails.setProcessingStatus(ScheduleConstants.STATUS_PENDING);
					schDetails.setOperationType(sessionBean.getOperationTypeSelected());
					schDetails.setUserId(wslId);

					/*
					 * CREATE SCHEDULE
					 */
					schDetails.setScheduleId(TranslationUpdateDAO.createSchedule(schDetails));
					if(schDetails.getScheduleId() > 0)
					{
						schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(schDetails.getScheduleId()));
						schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(schDetails.getScheduleId()));
						// set successMessage
						sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));

						/*
						 * invoke a parallel thread and start execution
						 */

						final TranslationExportProcessingImpl jobImpl = new TranslationExportProcessingImpl();
						final SITranslationScheduleDetails scheduleDetails = schDetails;
						final ArrayList<String> documentsList = docsList;
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {

								synchronized (jobImpl) {
									try {
										jobImpl.startProcess(scheduleDetails, documentsList);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "run()", e);
									}
								}
							}
						};

						Thread th = new Thread(runn, schDetails.getThreadId());
						th.start();
					
						/*
						 * CALL FUNCTION TO RETRIEVE SCHEDULE LIST
						 */
						getScheduleList(sessionBean);

						// reset documentsList & documentId input field from sessionBean
						sessionBean.setDocumentIdInput(null);
						sessionBean.setDocumentIdList(null);
						sessionBean.setFailedDocumentIdList(null);
						sessionBean.setTotalDocumentIdList(null);
					}
					else
					{
						// unable to schedule translation import / export job. Please contact administrator or try after some time.
						sessionBean.setErrorMessage(msgProps.addMessage("error.failure.schedule.translation", sessionBean.getOperationTypeSelected()));
					}
					schDetails=  null;
					docsList = null;
				}
				else
				{
					// set error message - document id is required.
//					sessionBean.setErrorMessage(msgProps.addMessage("error.mandatory.documentids", msgProps.getProperty("label.documentid")));
					sessionBean.setErrorMessage(msgProps.getProperty("error.export.translation.schedule.job"));
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "scheduleJobExportOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}

	private void scheduleJobImportOperation(SITranslationUpdateBean sessionBean)
	{
		try
		{
			if(validateSchedule(sessionBean))
			{
				// PROCEED ONLY WHEN DOC IDS ARE NOT NULL
				if(null!=folderPath && !"".equals(folderPath))
				{
					// CRETAE SCHEDULE
					SITranslationScheduleDetails schDetails = new SITranslationScheduleDetails();
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						ManualLanguageDetails mlDetails = null;
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(sessionBean.getManualLanguageId().trim().equals(String.valueOf(mlDetails.getManualLanguageId())))
							{
								schDetails.setLocaleCode(mlDetails.getManualLanguageName());
								break;
							}
							mlDetails=  null;
						}
						mlDetails=  null;
					}

					schDetails.setTotalCount(fileCount);
					schDetails.setScheduleName(ApplicationProperties.getProperty("translation.update.job.key"));
					schDetails.setThreadId(ApplicationProperties.getProperty("translation.update.job.key"));
					// set as Pending
					schDetails.setProcessingStatus(ScheduleConstants.STATUS_PENDING);
					schDetails.setOperationType(sessionBean.getOperationTypeSelected());
					schDetails.setUserId(wslId);

					/*
					 * CREATE SCHEDULE
					 */
					schDetails.setScheduleId(TranslationUpdateDAO.createSchedule(schDetails));
					if(schDetails.getScheduleId() > 0)
					{
						schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(schDetails.getScheduleId()));
						schDetails.setThreadId(schDetails.getThreadId()+String.valueOf(schDetails.getScheduleId()));
						// set successMessage
						sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));

						/*
						 * invoke a parallel thread and start execution
						 */

						final TranslationImportProcessingImpl jobImpl = new TranslationImportProcessingImpl();
						final SITranslationScheduleDetails scheduleDetails = schDetails;
						final String dirPath = folderPath;
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {

								synchronized (jobImpl) {
									try {
										jobImpl.startProcess(scheduleDetails, dirPath);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "run()", e);
									}
								}
							}
						};

						Thread th = new Thread(runn, schDetails.getThreadId());
						th.start();
					
						
						/*
						 * CALL FUNCTION TO RETRIEVE SCHEDULE LIST
						 */
						getScheduleList(sessionBean);
						fileCount=  0;
						invalidXMLsInZip=false;
						folderPath=  null;
						sessionBean.setImportFilesCount(null);
					}
					else
					{
						// unable to schedule translation import / export job. Please contact administrator or try after some time.
						sessionBean.setErrorMessage(msgProps.addMessage("error.failure.schedule.translation", sessionBean.getOperationTypeSelected()));
					}
					schDetails=  null;
				}
				else
				{
					// set error message - zip / xml file is required.
					sessionBean.setErrorMessage(msgProps.addMessage("error.mandatory.fields.specific",msgProps.getProperty("label.zip")+" / "+ msgProps.getProperty("label.xml")));
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "scheduleJobImportOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}

	
	private void getScheduleList(SITranslationUpdateBean sessionBean)
	{
		sessionBean.setReloadJSP(false);
		sessionBean.setScheduleList(new ArrayList<SITranslationScheduleDetails>());
		try
		{
			sessionBean.setScheduleList(TranslationUpdateDAO.getScheduleList());
			if(null!=sessionBean.getScheduleList() && sessionBean.getScheduleList().size()>0)
			{
				SITranslationScheduleDetails details = null;
				for(int a=0;a<sessionBean.getScheduleList().size();a++)
				{
					details = (SITranslationScheduleDetails)sessionBean.getScheduleList().get(a);
					if(null!=details.getProcessingStatus() && (details.getProcessingStatus().equals(ScheduleConstants.STATUS_PENDING) || 
							details.getProcessingStatus().equals(ScheduleConstants.STATUS_PROCESSING)))
					{
						sessionBean.setReloadJSP(true);
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "getScheduleList()", e);
		}
	}

	private void deleteScheduleTransactions(SITranslationUpdateBean sessionBean)
	{
		try
		{
			if(null!=selectedIds && !"".equals(selectedIds))
			{
				String selRows = (String)selectedIds;
				if(null!=selRows && !"".equals(selRows))
				{
					if(selRows.endsWith(","))
					{
						selRows = selRows.substring(0,selRows.length()-1);
					}
					boolean bool = TranslationUpdateDAO.deleteScheduleDetails(selectedIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("translation.schedule.delete.success"));
						/*
						 * call getScheduleList
						 */
						getScheduleList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.message.translation.schedule.delete"));
					}
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.schedule.delete"));
				}
				selRows=  null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.schedule.delete"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "deleteScheduleTransactions()", e);
		}
	}

	private void abortSchedule(SITranslationUpdateBean sessionBean)
	{
		try
		{
			if(null!=abortSchId && !"".equals(abortSchId))
			{
				// abort conversion
				String scheduleId=(String)abortSchId;
				String threadId=ApplicationProperties.getProperty("translation.update.job.key")+scheduleId;

				logger.info("abortSchedule :: Killing Thread Id {"+ threadId+"} for Schedule ::> " + scheduleId);

				/*
				 * NO THREAD SCAN HERE ANY MORE. This used to find the worker by name and
				 * call Thread.stop() on it - removed in Java 20, and the resulting
				 * UnsupportedOperationException was swallowed by the catch below, so the
				 * schedule was marked Aborted while the worker ran on to completion.
				 * The worker now stops ITSELF by reading the Aborted status this screen
				 * writes just below - see the isAborted() method on this family's DAO.
				 */

				/*
				 * call function to update ABort Status in DATABASE - IRRELEVANT OF WHETHEER THREAD WAS ACTIVE IN CONTIANER OR NOT
				 * THIS WILL HELP TO AVOID UPDATING STATUS MANUALLY IN DAATBASE.
				 */
				try
				{
					TranslationUpdateDAO.updateAbortStatus(scheduleId);

					//					NotificationEmailHelper.generateNotificationEmail(scheduleId);

					sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));

					/*
					 * re-call Schedule List
					 */
					getScheduleList(sessionBean);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "abortSchedule()", e);
					logger.info("abortSchedule :: Error while updating status for Schedule Id {"+scheduleId+"} in DATABASE.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
				}
			}
			else
			{
				logger.info("abortSchedule :: Schedule id & Thread id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "abortSchedule()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}


	private void executeZIPOperation(SITranslationUpdateBean sessionBean, byte[] data)
	{
		invalidXMLsInZip= false;
		fileCount=0;
		folderPath=  null;
		sessionBean.setImportFilesCount(null);
		try
		{
			if(null!=data)
			{
				// Write zip File to temp dir
				String path = ApplicationProperties.getProperty("translation.update.import.physical.path");
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				File file = new File(path);
				if(!file.exists() || !file.isDirectory())
				{
					file.mkdir();
				}
				file = null;
				// add temp dir
				path+=ApplicationProperties.getProperty("translation.update.import.temp.directory");
				file=new File(path);
				if(!file.exists() || !file.isDirectory())
				{
					file.mkdir();
				}
				file = null;
				/// append timestamp.zip
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				long val = new Date().getTime();
				boolean bool = FileReadWriteUtil.writeFile(data, path+String.valueOf(val)+ApplicationProperties.getProperty("ZIP_SUFFIX"));
				if(bool==true)
				{
					logger.info("executeZIPOperation :: Zip file Written successfully in Temp Directory");
					// unzip the file in temp dir.
					boolean unzipFlag = unzip(sessionBean,path+String.valueOf(val)+ApplicationProperties.getProperty("ZIP_SUFFIX"), path+String.valueOf(val));
					if(unzipFlag==true)
					{
						if(invalidXMLsInZip==false)
						{
							// no errors in ZIP File - PROCEED FOR SCHEDULING A JOB - USE TEMP FOLDER PATH FOR READING ALL XMLS
							// FOLDER PATH = path+String.valueOf(val)
							folderPath = path+String.valueOf(val);
							if(fileCount>0)
							{
								/*
								 * IDENTIFY SELECTED LOCALE FROM COMBO
								 */
								String selectedLocaleCode="";
								if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
								{
									if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
									{
										ManualLanguageDetails mlDetails = null;
										for(int r=0;r<sessionBean.getLanguageList().size();r++)
										{
											mlDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
											if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
											{
												selectedLocaleCode= mlDetails.getManualLanguageName();
												if(null!=selectedLocaleCode && !"".equals(selectedLocaleCode))
												{
													selectedLocaleCode = selectedLocaleCode.replace("-", "_");
												}
												break;
											}
											mlDetails = null;
										}
									}
								}
								
								/*
								 * verify all the XMLs belongs to SI & SM Channel Only
								 * also verify the locale selected from Combo should match the Locale added in XML.
								 */
								int errorCount=0;
								StringBuilder errorMessage = new StringBuilder();
								List<Map<String, String>> docsList = new ArrayList<Map<String,String>>();
								
								try
								{
									File zipDir  = new File(folderPath) ;
									if(zipDir.exists() && zipDir.isDirectory())
									{
										if(null!=zipDir.listFiles() && zipDir.listFiles().length>0)
										{
											byte[] xmlData = null;
											String xmlString=null;
											String docValues=null;
											String documentId=null;
											String locale= null;
											Map<String, String> docMap = null;
											for(int c=0;c<zipDir.listFiles().length;c++)
											{
												if(zipDir.listFiles()[c].exists() && zipDir.listFiles()[c].isFile())
												{
													if(!zipDir.listFiles()[c].getName().trim().toLowerCase().equals("thumbs.db") && 
															zipDir.listFiles()[c].getName().trim().toLowerCase().endsWith(".xml"))
													{
														try
														{
															/*
															 * parse XML File and get the document id & locale
															 */
															xmlData= FileReadWriteUtil.readFile(zipDir.listFiles()[c].getAbsolutePath());
															if(null!=xmlData && xmlData.length>0)
															{
																xmlString =new String (xmlData);
																if(null!=xmlString && !"".equals(xmlString))
																{
																	docValues=  getDocumentDetails(xmlString);
																	if(null!=docValues && !"".equals(docValues))
																	{
																		if(docValues.indexOf("+")!=-1)
																		{
																			documentId  =docValues.substring(0, docValues.indexOf("+"));
																			locale= docValues.substring(docValues.indexOf("+")+1, docValues.length());
																		}
																		
																		if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
																		{
																			locale = locale.replace("-", "_");
																			/*
																			 * NOW CHECK DOCUMENT ID MUST START WITH SI OR SM
																			 * AND LOCALE SHOULD MATCH TO SELECTED LOCALE
																			 */
																			boolean proceedFur = true;
																			if(!documentId.trim().toLowerCase().startsWith("si") && !documentId.trim().toLowerCase().startsWith("sm"))
																			{
																				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																				{
																					errorMessage.append("<MSG_TOKEN>");
																				}
																				errorMessage.append("Document Id ("+documentId+") in "+zipDir.listFiles()[c].getName()+ " is not allowed for Translation update, only SI & SM Channel documents can be processed. ");
																				// increment errorCount
																				errorCount++;
																				// do not proceed further
																				proceedFur = false;
																			}
																			if(!locale.trim().toLowerCase().equals(selectedLocaleCode.trim().toLowerCase()))
																			{
																				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																				{
																					errorMessage.append("<MSG_TOKEN>");
																				}
																				errorMessage.append("Locale ("+locale+") in "+zipDir.listFiles()[c].getName()+" does not match with Selected Locale ("+selectedLocaleCode+") on Screen.");
																				// increment errorCount
																				errorCount++;
																				// do not proceed further
																				proceedFur = false;
																			}
																			
																			if(proceedFur==true)
																			{
																				docMap=  new HashMap<String, String>();
																				docMap.put("DOC_ID", documentId);
																				docMap.put("LOCALE", locale);
																				docMap.put("FILE_NAME", zipDir.listFiles()[c].getName());
																				docsList.add(docMap);
																				docMap=  null;
																			}
																		}
																		else
																		{
																			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																			{
																				errorMessage.append("<MSG_TOKEN>");
																			}
																			errorMessage.append(msgProps.addMessage("error.failure.read.document.details", zipDir.listFiles()[c].getName()));
																			// increment errorCount
																			errorCount++;
																		}
																		documentId = null;
																		locale= null;
																	}
																	else
																	{
																		if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																		{
																			errorMessage.append("<MSG_TOKEN>");
																		}
																		errorMessage.append(msgProps.addMessage("error.failure.read.document.details", zipDir.listFiles()[c].getName()));
																		// increment errorCount
																		errorCount++;
																	}
																	docValues=  null;
																}
															}
															else
															{
																if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																{
																	errorMessage.append("<MSG_TOKEN>");
																}
																errorMessage.append("Failed to read data from "+zipDir.listFiles()[c].getName()+".");
																// increment errorCount
																errorCount++;
															}
															xmlData= null;
														}
														catch(Exception e)
														{
															Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName()	, "executeZIPOperation()", e);
															if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
															{
																errorMessage.append("<MSG_TOKEN>");
															}
															errorMessage.append(msgProps.getProperty("error.generic"));
															// increment errorCount
															errorCount++;
														}
													}
												}
											}
											// proceed for checking what all documents exists in IM for the locale - do this when error count is 0
											if(errorCount==0 && null!=docsList && docsList.size()>0)
											{
												docsList = TranslationUpdateDAO.verifyDocuments(docsList);
												if(null!=docsList && docsList.size()>0)
												{
													docMap= null;
													for(int c=0;c<docsList.size();c++)
													{
														docMap=  (Map<String, String>)docsList.get(c);
														if(null!=docMap && null!=docMap.get("DOC_ID") && !"".equals(docMap.get("DOC_ID")) && 
																null!=docMap.get("LOCALE") && !"".equals(docMap.get("LOCALE")))
														{
															if(null!=docMap.get("STATUS") && docMap.get("STATUS").toString().equals("Y"))
															{
																// do nothing
															}
															else
															{
																if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
																{
																	errorMessage.append("<MSG_TOKEN>");
																}
																errorMessage.append(docMap.get("DOC_ID").toString()+ " for "+docMap.get("LOCALE").toString()+" does not exists in Kapture for "+docMap.get("FILE_NAME").toString()+".");
																// increment errorCount
																errorCount++;
															}
														}
														docMap=  null;
													}
												}
											}
											docsList = null;
										}
									}
									else
									{
										logger.info("executeZIPOperation ::  No Zip file temp directory exists at location :: > "+ folderPath);
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										errorMessage.append(msgProps.getProperty("error.generic"));
										// increment errorCount
										errorCount++;
									}
									zipDir=  null;
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "executeZIPOperation()", e);
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append(msgProps.getProperty("error.generic"));
									// increment errorCount
									errorCount++;
								}
								
								if(errorCount > 0)
								{
									decideErrorDisplay(sessionBean, errorMessage, errorCount);
								}
								else
								{
									// no errors - allow for scheduling of Job
									sessionBean.setImportFilesCount(String.valueOf(fileCount));
								}
								selectedLocaleCode = null;
								errorMessage  =null;
							}
						}
						else
						{
							sessionBean.setErrorMessage(msgProps.getProperty("error.zip.invalid.files"));
						}
					}
					else
					{
						logger.info("executeZIPOperation :: Failed to Unzip Zip file in Temp Directory. Throw Generic Error");
						sessionBean.setErrorMessage(msgProps.getProperty("error.extract.upload.zip.file"));
					}
				}
				else
				{
					logger.info("executeZIPOperation :: Failed to Write Zip file in Temp Directory.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.failure.upload.zip.file"));
				}
				path = null;
				val =0;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "executeZIPOperation()", e);
		}
	}

	private boolean unzip(SITranslationUpdateBean sessionBean, String zipFilePath, String destDir) 
	{
		boolean bool=true;
		try
		{
			File dir = new File(destDir);
			// create output directory if it doesn't exist
			if(!dir.exists() || !dir.isDirectory())
			{
				dir.mkdirs();
			}
			FileInputStream fis;
			//buffer for read and write data to file
			byte[] buffer = new byte[1024];
			try 
			{
				fis = new FileInputStream(zipFilePath);
				ZipInputStream zis = new ZipInputStream(fis);
				ZipEntry ze = zis.getNextEntry();
				String fileName="";
				File newFile = null;
				while(ze != null)
				{
					fileName = ze.getName();
					if(!fileName.trim().toLowerCase().equals("thumbs.db"))
					{
//						if(fileName.trim().toLowerCase().endsWith(".xml") && (fileName.trim().toLowerCase().startsWith("si") 
//								|| fileName.trim().toLowerCase().startsWith("sm")))
						if(fileName.trim().toLowerCase().endsWith(".xml"))
						{
							newFile = new File(destDir + File.separator + fileName);
							logger.info("unzip :: Unzipping to "+newFile.getAbsolutePath());
							//create directories for sub directories in zip
							new File(newFile.getParent()).mkdirs();
							FileOutputStream fos = new FileOutputStream(newFile);
							int len;
							while ((len = zis.read(buffer)) > 0) {
								fos.write(buffer, 0, len);
							}
							fos.close();
							newFile =  null;
							// increment fileCount by 1
							fileCount++;
						}
						else
						{
							invalidXMLsInZip = true;
						}
						//close this ZipEntry
						zis.closeEntry();
						ze = zis.getNextEntry();
						fileName=  null;
						
						if(invalidXMLsInZip==true)
						{
							// stop unzipping and throw error
							break;
						}
					}
				}
				//close last ZipEntry
				zis.closeEntry();
				zis.close();
				fis.close();
			} 
			catch (IOException e) 
			{
				Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "unzip", e);
				bool=false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "unzip", e);
			bool=false;
		}
		return bool;

	}

	private void executeXMLOperation(SITranslationUpdateBean sessionBean, byte[] data,String fileName)
	{
		invalidXMLsInZip=false;
		fileCount=0;
		folderPath=  null;
		sessionBean.setImportFilesCount(null);
		try
		{
			if(null!=data)
			{
				String path = ApplicationProperties.getProperty("translation.update.import.physical.path");
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				File file = new File(path);
				if(!file.exists() || !file.isDirectory())
				{
					file.mkdir();
				}
				file = null;
				// add temp dir
				path+=ApplicationProperties.getProperty("translation.update.import.temp.directory");
				file=new File(path);
				if(!file.exists() || !file.isDirectory())
				{
					file.mkdir();
				}
				file = null;
				/// append timestamp
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				
				// create folder of current time stamp
				long val = new Date().getTime();
				file = new File(path+String.valueOf(val));
				if(!file.exists() || !file.isDirectory())
				{
					file.mkdir();
				}
				file = null;
				
				boolean bool = FileReadWriteUtil.writeFile(data, path+String.valueOf(val)+"/"+fileName);
				if(bool==true)
				{
					logger.info("executeXMLOperation :: XML file Written successfully in Temp Directory");
					// FOLDER PATH = path+String.valueOf(val)
					folderPath = path+String.valueOf(val);
					
					StringBuilder errorMessage=new StringBuilder();
					int errorCount=0;
					List<Map<String, String>> docsList = new ArrayList<Map<String,String>>();
					Map<String, String> docMap = null;
					
					try
					{
						String xml =new String(data);
						if(null!=xml && !"".equals(xml))
						{
							/*
							 * IDENTIFY SELECTED LOCALE FROM COMBO
							 */
							String selectedLocaleCode="";
							if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
							{
								if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
								{
									ManualLanguageDetails mlDetails = null;
									for(int r=0;r<sessionBean.getLanguageList().size();r++)
									{
										mlDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
										if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
										{
											selectedLocaleCode= mlDetails.getManualLanguageName();
											if(null!=selectedLocaleCode && !"".equals(selectedLocaleCode))
											{
												selectedLocaleCode = selectedLocaleCode.replace("-", "_");
											}
											break;
										}
										mlDetails = null;
									}
								}
							}
							
							
							String docValues=  getDocumentDetails(xml);
							String documentId="";
							String locale="";
							if(null!=docValues && !"".equals(docValues))
							{
								if(docValues.indexOf("+")!=-1)
								{
									documentId  =docValues.substring(0, docValues.indexOf("+"));
									locale= docValues.substring(docValues.indexOf("+")+1, docValues.length());
								}
								
								if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
								{
									locale = locale.replace("-", "_");
									/*
									 * NOW CHECK DOCUMENT ID MUST START WITH SI OR SM
									 * AND LOCALE SHOULD MATCH TO SELECTED LOCALE
									 */
									boolean proceedFur = true;
									if(!documentId.trim().toLowerCase().startsWith("si") && !documentId.trim().toLowerCase().startsWith("sm"))
									{
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										errorMessage.append("Document Id ("+documentId+") is not allowed for Translation update in the uploaded XML File. Only SI & SM Channel documents can be processed.");
										// increment errorCount
										errorCount++;
										// do not proceed further
										proceedFur = false;
									}
									if(!locale.trim().toLowerCase().equals(selectedLocaleCode.trim().toLowerCase()))
									{
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										errorMessage.append("Locale ("+locale+") in the uploaded XML File does not match with Selected Locale ("+selectedLocaleCode+") on Screen.");
										// increment errorCount
										errorCount++;
										// do not proceed further
										proceedFur = false;
									}
									
									if(proceedFur==true)
									{
										docMap=  new HashMap<String, String>();
										docMap.put("DOC_ID", documentId);
										docMap.put("LOCALE", locale);
										docsList.add(docMap);
										docMap=  null;
									}
								}
								else
								{
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append("Failed to read document & locale details from uploaded XML File.");
									// increment errorCount
									errorCount++;
								}
								documentId = null;
								locale= null;
							}
							else
							{
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append("Failed to read document & locale details from uploaded XML File.");
								// increment errorCount
								errorCount++;
							}
							docValues=  null;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "executeXMLOperation()", e);

						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.getProperty("error.genric"));
						// increment errorCount
						errorCount++;
					}
					
					if(errorCount==0 && null!=docsList && docsList.size()>0)
					{
						docsList = TranslationUpdateDAO.verifyDocuments(docsList);
						if(null!=docsList && docsList.size()>0)
						{
							docMap= null;
							for(int c=0;c<docsList.size();c++)
							{
								docMap=  (Map<String, String>)docsList.get(c);
								if(null!=docMap && null!=docMap.get("DOC_ID") && !"".equals(docMap.get("DOC_ID")) && 
										null!=docMap.get("LOCALE") && !"".equals(docMap.get("LOCALE")))
								{
									if(null!=docMap.get("STATUS") && docMap.get("STATUS").toString().equals("Y"))
									{
										// do nothing
									}
									else
									{
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										errorMessage.append(docMap.get("DOC_ID").toString()+ " for "+docMap.get("LOCALE").toString()+" does not exists in Kapture.");
										// increment errorCount
										errorCount++;
									}
								}
								docMap=  null;
							}
						}
					}
					
					
					if(errorCount> 0 )
					{
						decideErrorDisplay(sessionBean, errorMessage, errorCount);
					}
					else
					{
						// increment file Count by 1
						fileCount++;
						sessionBean.setImportFilesCount(String.valueOf(fileCount));
					}
				}
				else
				{
					logger.info("executeXMLOperation :: Failed to Write XML file in Temp Directory.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.failure.upload.xml.file"));
				}
				val = 0;
				path  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "executeXMLOperation()", e);
		}
	}

	private String getDocumentDetails(String xml)
	{
		String returnData = "";
		try
		{
			String documentId="";
			String locale="";
			if(null!=xml && !"".equalsIgnoreCase(xml))
			{
				// convert SOURCE XML TO DOCUMENT
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xml));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					Node node=  null;
					NodeList nodesList = doc.getElementsByTagName("DOCUMENTID");
					if(null!=nodesList && nodesList.getLength()>0)
					{
						node = nodesList.item(0);
						documentId  = getCharacterDataFromElement((Element)node);
						node= null;
					}
					nodesList=  null;
					node= null;
					
					nodesList= doc.getElementsByTagName("LOCALE");
					if(null!=nodesList && nodesList.getLength()>0)
					{
						node = nodesList.item(0);
						locale  = getCharacterDataFromElement((Element)node);
						node= null;
					}
					nodesList=  null;
					node= null;
				}
				doc = null;
				is =null;
				db=  null;
			}
			
			if(null!=documentId && !"".equals(documentId))
			{
				returnData = documentId;
			}
			returnData+="+";
			if(null!=locale && !"".equals(locale))
			{
				returnData+=locale;
			}
			documentId= null;
			locale = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationUpdate.class.getName(), "getDocumentDetails()", e);
			returnData = null;
		}
		return returnData;
	}
	
	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}
}
