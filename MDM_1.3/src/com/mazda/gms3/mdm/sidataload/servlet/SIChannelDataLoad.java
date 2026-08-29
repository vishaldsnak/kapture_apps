package com.mazda.gms3.mdm.sidataload.servlet;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.bean.SIChannelDataLoadBean;
import com.mazda.gms3.mdm.sidataload.dao.SIChannelDataLoadDAO;
import com.mazda.gms3.mdm.sidataload.impl.SIChannelDataLoadProcessingImpl;
import com.mazda.gms3.mdm.sidataload.kapture.SIChannelKaptureService;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelScheduleDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIKaptureDocumentDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class SIChannelDataLoad
 */
public class SIChannelDataLoad extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	Logger logger = LogManager.getLogger(SIChannelDataLoad.class);

	MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_SI_CHANNEL_DATA_LOAD;
	String wslId="";
	static final String OP_TYPE_NEW_DOCUMENT="New Document";
	static final String OP_TYPE_OLD_DOCUMENT="Old Document";
	private SIChannelDataLoadBean sessionBean=null;
	String abortSchId=null;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SIChannelDataLoad() {
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
			sessionBean = getSessionBean(request);
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
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setOperationTypeList(null);
			sessionBean.setSelectedOperationType(OP_TYPE_NEW_DOCUMENT);
			sessionBean.setActionClicked(null);
			sessionBean.setOldDocumentId(null);
			sessionBean.setShowOldDocumentIdField(false);
			sessionBean.setScheduleList(null);

			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(request);

			/*
			 * call function to load operation Type List
			 */
			getOperationTypeList();
			
			/*
			 * call function to get scheduleList
			 */
			getScheduleList(sessionBean);
			
			/*
			 * RESOLVE THE SERVICE INFORMATION CHANNEL ONCE, HERE, WHILE THE PAGE LOADS.
			 *
			 * Every Kapture call this screen makes - read the existing document, create a new one,
			 * update an existing one - has to name the channel's contentid. It is the same value for
			 * every SI document and it never changes, so it is resolved when the screen opens and
			 * cached in the DAO for the rest of the JVM's life; the schedule threads started from
			 * doPost() read it straight from that cache without touching the CMS again.
			 *
			 * NOT FATAL HERE ON PURPOSE. A user with read access may only be looking at the schedule
			 * list, which needs nothing from Kapture. The upload path validates it before scheduling
			 * anything, so a failure is reported when it actually matters instead of blocking a page
			 * that would otherwise work.
			 */
			FetchKaptureDataDAO.getSIChannelContentId();

			/*
			 * call function to show hide new document Id field
			 */
			performShowHideNewDocIdField();
			sessionBean =  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sichanneldataload.jsp");
				rs.forward(request, response);
			}
			userSessionBean = null;
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
			sessionBean = getSessionBean(request);
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
			 * call function to read parameters from request
			 */
			readParamsFromRequest(request);

			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_LOCALE_SELECTION"))
				{
					sessionBean.setManualLanguageId(null);
					/*
					 * call function to load all the Manual language data
					 */
					getLanguageList(request);
				}
				else if(sessionBean.getActionClicked().equals("REFRESH"))
				{
					/*
					 * call function to get scheduleList
					 */
					getScheduleList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ABORT_SCHEDULE"))
				{
					/*
					 * call function to abort schedule
					 */
					abortSchedule();
				}
			}
			
			/*
			 * call function to show hide new document Id field
			 */
			performShowHideNewDocIdField();
			sessionBean=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/sichanneldataload.jsp");
				rs.forward(request, response);
			}
			userSessionBean = null;
		}
	}

	private SIChannelDataLoadBean getSessionBean(HttpServletRequest request) 
	{
		sessionBean = null;
		if (null != request.getSession().getAttribute("sichannelDataLoadBean") && !"".equals(request.getSession().getAttribute("sichannelDataLoadBean"))) 
		{
			sessionBean = (SIChannelDataLoadBean) request.getSession().getAttribute("sichannelDataLoadBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SIChannelDataLoadBean();
			request.getSession().setAttribute("sichannelDataLoadBean", sessionBean);
		}
		return sessionBean;
	}
	
	private  void getCountryLocaleList(HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private  void getLanguageList(HttpServletRequest request)
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
				list=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "getLanguageList()", e);
		}
	}
	
	private  void getOperationTypeList()
	{
		sessionBean.setOperationTypeList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(OP_TYPE_NEW_DOCUMENT);
		si.setValue(OP_TYPE_NEW_DOCUMENT);
		sessionBean.getOperationTypeList().add(si);
		si = null;
		
		si = new SelectItemDetails();
		si.setLabel(OP_TYPE_OLD_DOCUMENT);
		si.setValue(OP_TYPE_OLD_DOCUMENT);
		
		sessionBean.getOperationTypeList().add(si);
		si = null;
	}
	
	private  void readParamsFromRequest(HttpServletRequest request)
	{
		try
		{
			sessionBean.setManualLanguageId(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setOldDocumentId(null);
			sessionBean.setActionClicked(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			abortSchId=null; 
			
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
			String value = null;
			String fieldName=null;
			FileItem fileItem = null;
			while (iterator.hasNext()) 
			{
				fileItem = iterator.next();
				if (fileItem.isFormField())
				{
					fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						if(fieldName.equals("SICH_AbortSchId"))
						{
							// set the value in abortSchId
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								abortSchId = value;
							}
							// set value to null
							value = null;
						}
						
						
						if (fieldName.equals("SICH_CountryLocale_Code")) 
						{
							// set the value in sessionBean.setCountryLocaleId
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setCountryLocaleId(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SICH_Lang_Code")) 
						{
							// set the value in sessionBean.setManualLanguageId
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setManualLanguageId(value);
							}
							// set value to null
							value = null;
						}


						if (fieldName.equals("SICH_OperationType")) 
						{
							// set the value in sessionBean.setSelectedOperationType
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedOperationType(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SICH_OldDocumentId")) 
						{
							// set the value in sessionBean.setOldDocumentId
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setOldDocumentId(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SICH_ActionClicked")) 
						{
							// set the value in sessionBean.actionClicked
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setActionClicked(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SICH_Schedule_displayPageNo")) 
						{
							// set the value in sessionBean.displayPageNo
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SICH_Schedule_displayPageLen")) 
						{
							// set the value in sessionBean.displayPageLenght
							value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}
					}
					fieldName = null;
					value=null;
				}
				else if (!fileItem.isFormField()) 
				{
					if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked())
							&& sessionBean.getActionClicked().equals("FILE_UPLOAD"))
					{
						// FIRST VALIDATE ALL MANDATORY FIELDS
						if(validateFileUpload())
						{
							/*
							 * NOW CHECK FOR FILE NAME , SIZE & EXTENSION
							 */
							String fileName = fileItem.getName();
							byte[] data = fileItem.get();
							if(null!=fileName && !"".equals(fileName) && null!=data)
							{
								// CHECK FOR ZIP FILE
								String extension="";
								if(fileName.lastIndexOf(".")!=-1)
								{
									extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
									if(null!=extension && !"".equals(extension))
									{
										if(extension.trim().toLowerCase().equals("doc") || extension.trim().toLowerCase().equals("docx"))
										{
											/*
											 * PERFORM IM VALIDATION OF DOCUMENT IF OLD DOCUMENT IS SELECTED 
											 * FOR PROCESSING
											 */
											if(performIMValidations())
											{
												/*
												 * 	SCHEDULE A JOB
												 *  INITIATE A NEW THREAD - UPLOAD FILE TO A TEMP DIRECTORY,
												 *  	CONVERT IT TO HTML AND THEN START DOCUMENT CREATION / MODIFICATION 
												 */
												SIChannelScheduleDetails schDetails = new SIChannelScheduleDetails();
												schDetails.setScheduleName("SI_DL_SCH_");
												if(sessionBean.getSelectedOperationType().equals(OP_TYPE_OLD_DOCUMENT))
												{
													schDetails.setDocumentId(sessionBean.getOldDocumentId());
												}
												schDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
												schDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
												ManualLanguageDetails mlDetails =null;
												if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
												{
													for(int a=0;a<sessionBean.getLanguageList().size();a++)
													{
														mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
														if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
														{
															schDetails.setLocale(mlDetails.getManualLanguageName());
															break;
														}
														mlDetails  =null;
													}
												}
												mlDetails  = null;
												
												schDetails.setWslId(wslId);
												schDetails.setScheduleStatus(ScheduleConstants.STATUS_PROCESSING);
												schDetails.setScheduleTime(new Timestamp(new Date().getTime()));
												/*
												 * PROCEED FOR CREATING SCHEDULE IN DATABASE
												 */
												long scheduleId=SIChannelDataLoadDAO.createSchedule(schDetails);
												if(scheduleId>0)
												{
													// set scheduleId
													schDetails.setScheduleId(scheduleId);
													// update scheduleName
													schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
													// show successMessage
													sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));
													
													/*
													 * CREATE A NEW TRHEAD WITH NAME AS SCHEDULE NAME AND START PROCESS
													 * 		1.) UPLOAD FILE TO TEMP DIRECTORY
													 * 		2.) CONVERT UPLOADED FILE IN HTML IN TEMP DIRECTORY
													 * 		3.) START PROCESSING HTML FILE, E.G. READIING THE HTML FILE INSIDE IT
													 * 				A.) IDENTIFYING SCHEMA FIELDS CONTENT
													 * 				B.) IDENTIFYING OKASSETS AND LOOKUP OF THE FILES.
													 */
													final SIChannelScheduleDetails details = new SIChannelScheduleDetails();
													details.setScheduleId(scheduleId);
													details.setScheduleName(schDetails.getScheduleName());
													details.setLocale(schDetails.getLocale());
													details.setDocumentId(schDetails.getDocumentId());
													details.setWslId(schDetails.getWslId());
													if(null!=schDetails.getDocumentId() && !"".equals(schDetails.getDocumentId()))
													{
														details.setDocumentType(OP_TYPE_OLD_DOCUMENT);
													}
													else
													{
														details.setDocumentType(OP_TYPE_NEW_DOCUMENT);
													}
													final byte[] fileDate = data;
													final SIChannelDataLoadProcessingImpl startConvImpl = new SIChannelDataLoadProcessingImpl();
													final String ext = extension;
													Runnable runn = new Runnable() 
													{
														@Override
														public void run() {
															
															synchronized (startConvImpl) {
																try {
//																	startConvImpl.startProcessing(details, fileDate);
																	startConvImpl.startProcessing_WithWordFile(details, fileDate, ext);
																} catch (Exception e) {
																	Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "run()", e);
																}
															}
														}
													};
													
													// start thread
													Thread th = new Thread(runn, schDetails.getScheduleName());
													th.start();
													
													/*
													 * call function to get scheduleList
													 */
													getScheduleList(sessionBean);
												}
												else
												{
													logger.info("readParamsFromRequest() :: Failed to create Schedule in Database. Throw Generic Error.");
													sessionBean.setErrorMessage(msgProps.getProperty("error.job.schedule.failure"));
												}
												schDetails = null;
											}
										}
										else
										{
//											logger.info("readParamsFromRequest() :: Extension is not ZIP. Throw Message - Uploaded file not supported."); 
//											sessionBean.setErrorMessage("Please upload a valid ZIP file.");
											logger.info("readParamsFromRequest() :: Extension is not Doc / Docx. Throw Message - Uploaded file not supported."); 
											sessionBean.setErrorMessage("Please upload a valid Doc file.");
										}
									}
									else
									{
//										logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid Zip File.");
//										sessionBean.setErrorMessage("Please upload a valid ZIP file.");
										logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid Doc File.");
										sessionBean.setErrorMessage("Please upload a valid Doc file.");
									}
								}
								else
								{
//									logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid Zip File.");
//									sessionBean.setErrorMessage("Please upload a valid ZIP file.");
									logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid Doc File.");
									sessionBean.setErrorMessage("Please upload a valid Doc file.");
								}
								extension = null;
							}
							else
							{
//								logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid Zip File.");
//								sessionBean.setErrorMessage("Please upload a valid ZIP file.");
								logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid Doc File.");
								sessionBean.setErrorMessage("Please upload a valid Doc file.");
							}
							fileName=  null;
							data = null;
						}
					}
				}
				fileItem = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "readParamsFromRequest(", e);
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

	private void performAccessCheck(SIChannelDataLoadBean sessionBean, HttpServletRequest request)
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
			userSessionBean = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "performAccessCheck()", e);
		}
	}

	private void performShowHideNewDocIdField()
	{
		sessionBean.setShowOldDocumentIdField(false);
		if(null!=sessionBean.getSelectedOperationType() && sessionBean.getSelectedOperationType().equals(OP_TYPE_OLD_DOCUMENT))
		{
			// show Old Document Id field
			sessionBean.setShowOldDocumentIdField(true);
		}
		else
		{
			// set Old Document Id field as blank
			sessionBean.setOldDocumentId(null);
		}
	}
	
	private  boolean validateFileUpload()
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
		
		if(null!=sessionBean.getSelectedOperationType() && sessionBean.getSelectedOperationType().equals(OP_TYPE_OLD_DOCUMENT))
		{
			if(null==sessionBean.getOldDocumentId() || "".equals(sessionBean.getOldDocumentId()))
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.documentid")));
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
	
	private boolean performIMValidations()
	{
		if(null!=sessionBean.getSelectedOperationType() && sessionBean.getSelectedOperationType().equals(OP_TYPE_OLD_DOCUMENT))
		{
			if(null!=sessionBean.getOldDocumentId() && !"".equals(sessionBean.getOldDocumentId()) && 
					null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				// IDENTIFY LOCALE
				try
				{
					String locale=null;
					ManualLanguageDetails mlDetails =null;
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
							{
								locale= mlDetails.getManualLanguageName();
								break;
							}
							mlDetails  =null;
						}
					}
					
					if(null!=locale && !"".equals(locale))
					{
						locale = locale.replace("-", "_").trim();
						/*
						 * FETCH THE DOCUMENT FROM KAPTURE FOR THE IDENTIFIED LOCALE.
						 *
						 * VALIDATED HERE, BEFORE A SCHEDULE ROW EXISTS. Everything past this point
						 * runs on a background thread whose only visible outcome is a schedule
						 * status, so a document the user cannot write to is far more useful to
						 * report on the screen than in a report they have to go and open.
						 */
						SIKaptureDocumentDetails kaptureDoc = SIChannelKaptureService.getDocument(
								sessionBean.getOldDocumentId(), locale,
								"SI Data Load - validating the selected document before scheduling");

						/*
						 * "COULD NOT ASK" IS NOT "NOT THERE" - see SIKaptureDocumentDetails. If the
						 * API was unreachable the document may well exist, so the upload is refused
						 * rather than allowed to run against an unknown state.
						 */
						if(kaptureDoc.isReadFailed()==true)
						{
							logger.info("performIMValidations :: Could not read "+ sessionBean.getOldDocumentId()+" for Locale "+locale+" from Kapture :: >"+ kaptureDoc.getErrorMessage());
							sessionBean.setErrorMessage("Failed to Fetch "+ sessionBean.getOldDocumentId()+" for Locale "+locale+" from Kapture.");
							return false;
						}

						if(kaptureDoc.isFound()==false)
						{
							logger.info("performIMValidations :: "+ sessionBean.getOldDocumentId()+" has no version in Locale "+locale+" in Kapture.");
							sessionBean.setErrorMessage("Failed to Fetch "+ sessionBean.getOldDocumentId()+" for Locale "+locale+" from Kapture.");
							return false;
						}

						/*
						 * MASTER OR TRANSLATION - RECORDED, NOT REFUSED. Both can be loaded into;
						 * which one it is decides the write endpoint later, and having it in the log
						 * from the outset makes a surprising result traceable to the read.
						 */
						logger.info("performIMValidations :: "+ kaptureDoc.describe());

						/*
						 * CHECKED OUT - STILL A REAL CONDITION UNDER KAPTURE. The channel has
						 * checkout enabled and k_article_checkout carries checked-out SI documents;
						 * only the table being read has changed.
						 */
						if(FetchKaptureDataDAO.isDocumentCheckedOut(sessionBean.getOldDocumentId())==true)
						{
							// DOCUMENT IS CHECKED OUT BY SOMEONE
							logger.info("performIMValidations :: Document Id "+ sessionBean.getOldDocumentId()+" for Locale "+locale+" is Checked Out in Kapture.");
							sessionBean.setErrorMessage(msgProps.addMessage("error.document.checked.out",sessionBean.getOldDocumentId()));
							return false;
						}
						kaptureDoc = null;
					}
					else
					{
						logger.info("performIMValidations :: Failed to identify Locale Code for the selected Manual Language id in Combo.");
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
						return false;
					}
					locale = null;
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "performIMValidations()", e);
					sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					return false;
				}
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
				return false;
			}
		}
		return true;
	}
	
	
	private void abortSchedule()
	{
		try
		{
			if(null!=abortSchId && !"".equals(abortSchId))
			{
				// abort conversion
				String scheduleId=(String)abortSchId;
				String threadId="SI_DL_SCH_"+scheduleId;

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
					SIChannelDataLoadDAO.updateAbortStatus(scheduleId);
					sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
					/*
					 * re-call Schedule List
					 */
					getScheduleList(sessionBean);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "abortSchedule()", e);
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
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "abortSchedule()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}

	private void getScheduleList(SIChannelDataLoadBean sessionBean)
	{
		sessionBean.setReloadJSP(false);
		sessionBean.setScheduleList(new ArrayList<SIChannelScheduleDetails>());
		try
		{
			sessionBean.setScheduleList(SIChannelDataLoadDAO.getScheduleList(wslId));
			if(null!=sessionBean.getScheduleList() && sessionBean.getScheduleList().size()>0)
			{
				SIChannelScheduleDetails details = null;
				for(int a=0;a<sessionBean.getScheduleList().size();a++)
				{
					details = (SIChannelScheduleDetails)sessionBean.getScheduleList().get(a);
					if(null!=details.getScheduleStatus() && (details.getScheduleStatus().equals(ScheduleConstants.STATUS_PROCESSING) ||   
							details.getScheduleStatus().equals("Uploading File")))
					{
						sessionBean.setReloadJSP(true);
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIChannelDataLoad.class.getName(), "getScheduleList()", e);
		}
	}

	
}
