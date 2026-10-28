package com.mazda.gms3.dmt.servlet;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.mazda.gms3.dmt.automation.dao.AutomationDAO;
import com.mazda.gms3.dmt.automation.dao.AutomationIdentificationDAO;
import com.mazda.gms3.dmt.automation.impl.StartAutomationProcessingImpl;
import com.mazda.gms3.dmt.automation.impl.StartDocumentsIdentificationImpl;
import com.mazda.gms3.dmt.automation.utils.AutomationUtils;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.bean.ScheduleBean;
import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.conversion.dao.EntParsing;
import com.mazda.gms3.dmt.conversion.impl.StartConversionMNAOImpl;
import com.mazda.gms3.dmt.conversion.impl.StartMCConversionImpl;
import com.mazda.gms3.dmt.conversion.impl.StartMMEConversionImpl;
import com.mazda.gms3.dmt.conversion.utils.ConversionUtils;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.dao.SettingsDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.utils.CDProcessingUtils;
import com.mazda.gms3.dmt.mc.vo.CDProcessingDetails;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DateFormatter;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.SendMailUsingAuthentication;
import com.mazda.gms3.dmt.utils.SourceContentPaths;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.DeleteFileDetails;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.ManualTypeDetails;
import com.mazda.gms3.dmt.vo.ScheduleDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.SettingDetails;

/**
 * Servlet implementation class Schedule
 */
public class Schedule extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger logger = LogManager.getLogger(Schedule.class);

	/*
	 * PER REQUEST, NOT PER SERVLET (2026-10-11). These were fields of the one servlet instance every user shares, so a
	 * request could read another user's id, messages or content path. They are set at the start of doGet / doPost; a job
	 * thread gets the user id captured before it starts.
	 */
	private static final ThreadLocal<MessageProperties> MSGPROPS = new ThreadLocal<MessageProperties>();
	private static final ThreadLocal<String> SERVERPATH = new ThreadLocal<String>();
	private static final ThreadLocal<String> WSLID = new ThreadLocal<String>();

	private static MessageProperties msgProps() { return MSGPROPS.get(); }
	private static String serverPath() { return SERVERPATH.get(); }
	private static String wslId() { return WSLID.get(); }


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Schedule() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		WSLID.set(""); SERVERPATH.set(""); MSGPROPS.remove();
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				WSLID.set(request.getHeader("iv-user"));
				if (null == wslId() || "".equals(wslId())) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
			}
			MSGPROPS.set(new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale")));


			/*
			 * CHECK WHETHER USER HAS ACCESS TO THE APPLICATION OR NOT
			 */
			UserAccessBean userBean = getUserSessionBean(request);
			if(null!=userBean && userBean.isShowNoAccess()==true)
			{
				/*
				 * NAVIGATE USER TO NO ACCESS PAGE, USER DOES NOT HAVE ACCESS TO DMT 
				 */
				response.sendRedirect(request.getContextPath()+"/noaccess");
				useReqDis = false;
			}


			ScheduleBean sessionBean = getSessionBean(request);
			sessionBean.setShowJSPData(false);

			/*
			 * call function to set serverPath from the settings dao
			 */
			SettingDetails sDetails = SettingsDAO.getSettingsData(wslId());
			if(null!=sDetails && null!=sDetails.getNetworkPath() && !"".equals(sDetails.getNetworkPath()))
			{
				SERVERPATH.set(com.mazda.gms3.dmt.utils.SourceContentPaths.contentRoot(sDetails.getNetworkPath()));
				sessionBean.setShowJSPData(true);
			}
			sDetails = null;
			//serverPath=ApplicationProperties.getProperty("GMS3_SOURCE_CONTENT_PATH");
			if(null!=serverPath() && !"".equals(serverPath()))
			{
				SERVERPATH.set(serverPath().replace("/", "\\"));
			}


			sessionBean.setSuccessMessage(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setScheduleName(null);
			sessionBean.setActionClicked(null);
			sessionBean.setMarketList(null);
			sessionBean.setMarketId(null);
			sessionBean.setLocalesList(null);
			sessionBean.setModelsList(null);
			sessionBean.setScheduleItemsList(null);
			sessionBean.setSelectedLocales(null);
			sessionBean.setSelectedLocalesString(null);
			sessionBean.setSelectedModels(null);
			sessionBean.setSelectedModelsString(null);
			sessionBean.setSelectedManualTypes(null);
			sessionBean.setSelectedManualTypesString(null);
			sessionBean.setManualTypesList(null);
			// set default Value for the Document Status
			sessionBean.setDocumentStatusId(ApplicationProperties.getProperty("flag.value.publish")); sessionBean.setLoadTypeId(ScheduleDAO.LOAD_TYPE_ONLY_CONTENT);
			sessionBean.setDocumentStatusList(null);
			sessionBean.setSelectAll(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayATPageLength(null);
			sessionBean.setDisplayATPageNo(null);
			sessionBean.setShowNoRecordTable(false);
			sessionBean.setSelectedRows(null);
			sessionBean.setScheduleATName(null);

			sessionBean.setAutomationItemsList(null);
			sessionBean.setShowAutomationGridBlock(false);
			sessionBean.setSelectedATRows(null);
			sessionBean.setSelectATAll(false);

			sessionBean.setShowIdenitfyButton(false);
			sessionBean.setInputDataList(null);

			// for operationId - set Default Value - Data Load
			sessionBean.setOperationId(ApplicationProperties.getProperty("operation.type.dataload"));
			sessionBean.setOperationList(null);

			sessionBean.setFromDate(null);
			sessionBean.setToDate(null);
			sessionBean.setAtDocsIdentificationJobList(null);
			sessionBean.setJobIdForATOperation(null);
			sessionBean.setDisplayATDCIDPageLength(null);
			sessionBean.setDisplayATDCIDPageNo(null);
			
			/*
			 * call function to read Market List
			 */
			getMarketList(sessionBean);

			/*
			 * call function to get the Document Status List
			 */
			getDocumentStatusList(sessionBean);

			/*
			 * call function to get Operation Type List
			 */
			getOperationList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/schedule.jsp");
			rs.forward(request, response);
		}
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		WSLID.set(""); SERVERPATH.set(""); MSGPROPS.remove();
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				WSLID.set(request.getHeader("iv-user"));
				if (null == wslId() || "".equals(wslId())) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
			}
			MSGPROPS.set(new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale")));

			/*
			 * CHECK WHETHER USER HAS ACCESS TO THE APPLICATION OR NOT
			 */
			UserAccessBean userBean = getUserSessionBean(request);
			if(null!=userBean && userBean.isShowNoAccess()==true)
			{
				/*
				 * NAVIGATE USER TO NO ACCESS PAGE, USER DOES NOT HAVE ACCESS TO DMT 
				 */
				response.sendRedirect(request.getContextPath()+"/noaccess");
				useReqDis = false;
			}
			/*
			 * call function to set serverPath from the settings dao
			 */
			ScheduleBean sessionBean = getSessionBean(request);
			sessionBean.setShowJSPData(false);
			sessionBean.setShowNoRecordTable(false);

			/*
			 * call function to set serverPath from the settings dao
			 */
			SettingDetails sDetails = SettingsDAO.getSettingsData(wslId());
			if(null!=sDetails && null!=sDetails.getNetworkPath() && !"".equals(sDetails.getNetworkPath()))
			{
				SERVERPATH.set(com.mazda.gms3.dmt.utils.SourceContentPaths.contentRoot(sDetails.getNetworkPath()));
				sessionBean.setShowJSPData(true);
			}
			sDetails = null;

			sessionBean.setSuccessMessage(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setScheduleName(null);
			sessionBean.setScheduleATName(null);

			/*
			 * read parameters from request
			 */
			readParametersFromRequest(sessionBean, request);


			/*
			 * DO THIS CHECKBOX MANAGEMENT AT THIS POSITION BEFORE CLICKED ON SCHEDULING OR ANY FORM SUBMIT ACTION.
			 * TO PERSIST THE CURRENT VALUES OF DATA TABLE FROM JSP
			 */
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				checkBoxOperation(request, sessionBean);
			}

			if(null!=sessionBean.getSelectedATRows() && !"".equals(sessionBean.getSelectedATRows()))
			{
				checkBoxATOperation(request, sessionBean);
			}

			sessionBean.setFaceliftConfirmPending(false);
			sessionBean.setScrollToSelectItems(false);
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("MARKET_SELECTION"))
				{
					marketSelectionOperation(sessionBean);
				}

				else if(sessionBean.getActionClicked().equals("LOCALE_SELECTION"))
				{
					localeSelectionOperation(sessionBean);
				}

				else if(sessionBean.getActionClicked().equals("MODEL_SELECTION"))
				{
					modelSelectionOperation(sessionBean);
				}

				else if(sessionBean.getActionClicked().equals("FILTER_RECORDS"))
				{
					// facelift Excel files present: the IPM Facelift VIN Update must have run before the conversion
					if(!sessionBean.isFaceliftRunConfirmed() && faceliftFilesPresent())
					{
						sessionBean.setScheduleItemsList(new ArrayList<ScheduleItemDetails>());
						sessionBean.setShowNoRecordTable(false);
						sessionBean.setSelectAll(false);
						sessionBean.setFaceliftConfirmPending(true);
					}
					else
					{
						filterOperation(sessionBean);
					}
				}

				else if(sessionBean.getActionClicked().equals("SCHEDULE_CONVERSION"))
				{
					// the job rules are checked and the schedule created under one lock (two clicks at once)
					synchronized(com.mazda.gms3.dmt.dao.JobRules.SCHEDULE_LOCK)
					{
						scheduleConversion(response, sessionBean);
					}
				}

				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					resetOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ABORT_JOB"))
				{
					abortConversionOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_JOBS") || sessionBean.getActionClicked().equals("REFRESH_LIST"))
				{
					getAtDocsIdentificationScheduleList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VIEW_IDENTIFIED_DOCS_DETAILS"))
				{
					viewImpactedDocumentsDetails(sessionBean);
					// the page opens at the listed items - every other action (Schedule, errors) stays at the top
					sessionBean.setScrollToSelectItems(sessionBean.isShowAutomationGridBlock() && null!=sessionBean.getAutomationItemsList()
							&& !sessionBean.getAutomationItemsList().isEmpty() && (null==sessionBean.getErrorMessage() || "".equals(sessionBean.getErrorMessage())));
				}
				else if(sessionBean.getActionClicked().equals("OPERATION_CHANGE"))
				{
					if(null!=sessionBean.getOperationId() && sessionBean.getOperationId().equals(ApplicationProperties.getProperty("operation.type.automation")))
					{
						// VALUE CHANGED TO AUTOMATION - RESET DATA LOAD VARIABLES
						resetOperation(sessionBean);
						
						// LOAD DEFAULT VALUES
						// set today's date as ToDate
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						sessionBean.setToDate(sdf.format(new Date()));
						// get 10 days before today's date
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.DATE, -30);
						Date dateBefore10Days = cal.getTime();
						sessionBean.setFromDate(sdf.format(dateBefore10Days));
						cal  =null;
						dateBefore10Days = null;

						/*
						 * call function to get AT Docs Identification Jobs List
						 */
						getAtDocsIdentificationScheduleList(sessionBean);
					}
					else if(null!=sessionBean.getOperationId() && sessionBean.getOperationId().equals(ApplicationProperties.getProperty("operation.type.dataload")))
					{
						// VALUE CHANGED TO DATALOAD - RESET AUTOMATION VARIABLES
						resetATOperation(sessionBean);
					}

				}
				else if(sessionBean.getActionClicked().equals("RESET_AUTOMATION"))
				{
					resetATOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SCHEDULE_AUTOMATION"))
				{
					// the job rules are checked and the schedule created under one lock (two clicks at once)
					synchronized(com.mazda.gms3.dmt.dao.JobRules.SCHEDULE_LOCK)
					{
						scheduleAutomation(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("IDENTIFY_IMPACTED_DOCUMENTS"))
				{
					// the job rules are checked and the schedule created under one lock (two clicks at once)
					synchronized(com.mazda.gms3.dmt.dao.JobRules.SCHEDULE_LOCK)
					{
						identifyImpactedDocumentsDataForSchedule(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("VALIDATE_FACELIFT"))
				{
					validateFaceliftFolder(sessionBean);
				}
			}

			// IPM FACELIFT VIN UPDATE: the Excel files of the facelift folder (or the "no Excel found" message)
			if(null!=sessionBean.getOperationId() && sessionBean.getOperationId().equals(ApplicationProperties.getProperty("operation.type.automation")))
			{
				refreshFaceliftFiles(sessionBean);
			}

			// reset actionClicked
			sessionBean.setActionClicked(null);
			sessionBean.setFaceliftRunConfirmed(false);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "doPost()", e);
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/schedule.jsp");
			rs.forward(request, response);
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

	private ScheduleBean getSessionBean(HttpServletRequest request) 
	{
		ScheduleBean sessionBean = null;
		if (null != request.getSession().getAttribute("scheduleBean") && 
				!"".equals(request.getSession().getAttribute("scheduleBean"))) 
		{
			sessionBean = (ScheduleBean) request.getSession().getAttribute("scheduleBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new ScheduleBean();
			request.getSession().setAttribute("scheduleBean", sessionBean);
		}
		return sessionBean;
	}

	/**
	 * Function will read all the parameters from HTTPServlet Request
	 * @param sessionBean
	 * @param request
	 */
	@SuppressWarnings("unchecked")
	private void readParametersFromRequest(ScheduleBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setMarketId(null);
			sessionBean.setSelectedLocales(null);
			sessionBean.setSelectedLocalesString(null);
			sessionBean.setSelectedModelsString(null);
			sessionBean.setSelectedModels(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setSelectedATRows(null);
			sessionBean.setSelectedManualTypesString(null);
			sessionBean.setSelectedManualTypes(null);
			sessionBean.setOperationId(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayATPageLength(null);
			sessionBean.setDisplayATPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setFromDate(null);
			sessionBean.setToDate(null);
			sessionBean.setJobIdForATOperation(null);
			sessionBean.setDisplayATDCIDPageLength(null);
			sessionBean.setDisplayATDCIDPageNo(null);

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
					if (fieldName.equals("DMT_SCH_MarketId")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setMarketId(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_SelectedLocales")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							if(value.endsWith(","))
							{
								value = value.substring(0, value.length()-1);
							}
							sessionBean.setSelectedLocalesString(value);
							sessionBean.setSelectedLocales(value.split(","));
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_SelectedModels")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							if(value.endsWith(","))
							{
								value = value.substring(0, value.length()-1);
							}
							sessionBean.setSelectedModelsString(value);
							sessionBean.setSelectedModels(value.split(","));
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_SelectedManualTypes")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							if(value.endsWith(","))
							{
								value = value.substring(0, value.length()-1);
							}
							sessionBean.setSelectedManualTypesString(value);
							sessionBean.setSelectedManualTypes(value.split(","));
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_LoadType"))
					{
						String value = fileItem.getString("UTF-8");
						if (ScheduleDAO.LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(value) || ScheduleDAO.LOAD_TYPE_ONLY_CONTENT.equals(value))
						{
							sessionBean.setLoadTypeId(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCHAT_LoadType"))
					{
						String value = fileItem.getString("UTF-8");
						if (ScheduleDAO.LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(value) || ScheduleDAO.LOAD_TYPE_ONLY_CONTENT.equals(value))
						{
							sessionBean.setAtLoadTypeId(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_DocumentStatus")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDocumentStatusId(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_SelectedRows")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setSelectedRows(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCHAT_SelectedRows")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setSelectedATRows(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_OperationId")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setOperationId(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_displayPageNo")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayPageNo(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_displayPageLen")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayPageLength(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCHAT_displayPageNo")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayATPageNo(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCHAT_displayPageLen")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayATPageLength(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCH_FaceliftConfirmed"))
					{
						sessionBean.setFaceliftRunConfirmed("Y".equals(fileItem.getString("UTF-8")));
					}

					if (fieldName.equals("DMT_SCH_ActionClicked")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setActionClicked(value);
						}
						value=null;
					}
					
					if (fieldName.equals("DMT_SCHAT_FromDate")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setFromDate(value);
						}
						value=null;
					}
					
					if (fieldName.equals("DMT_SCHAT_ToDate")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setToDate(value);
						}
						value=null;
					}
					
					if (fieldName.equals("DMT_SCHAT_JobIdForATOperation")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setJobIdForATOperation(value);
						}
						value=null;
					}
					
					if (fieldName.equals("DMT_SCHATDCID_displayPageNo")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayATDCIDPageNo(value);
						}
						value=null;
					}

					if (fieldName.equals("DMT_SCHATDCID_displayPageLen")) 
					{
						String value = fileItem.getString("UTF-8");
						if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
						{
							sessionBean.setDisplayATDCIDPageLength(value);
						}
						value=null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "readParametersFromRequest()", e);
		}
	}

	/**
	 * Function will read all the market folders from the server path; e.g. source content location
	 * @param sessionBean
	 */
	private void getMarketList(ScheduleBean sessionBean)
	{
		try
		{
			sessionBean.setMarketId(null);
			sessionBean.setMarketList(new ArrayList<SelectItemDetails>());
			if(null!=serverPath() && !"".equals(serverPath()))
			{
				File sourceContentFolder = PathUtil.file(serverPath());
				if(sourceContentFolder.exists() && sourceContentFolder.isDirectory())
				{
					File[] marketFolders = sourceContentFolder.listFiles();
					if(null!=marketFolders && marketFolders.length>0)
					{
						for(int i=0;i<marketFolders.length;i++)
						{
							File market = marketFolders[i];
							if(market.isDirectory())
							{
								SelectItemDetails si = new SelectItemDetails();
								si.setValue(market.getName());
								si.setLabel(String.valueOf(market.getName()).toUpperCase());
								sessionBean.getMarketList().add(si);
								si = null;
							}
							market=  null;
						}
					}
					marketFolders= null;
				}
				else
				{
					logger.info("getMarketList :: Either Source Content Directory at ServerPath does not Exists or No Market Folder Found at Path :: > " + serverPath());
				}
				sourceContentFolder = null;
			}
			else
			{
				logger.info("getMarketList :: Server Path is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "getMarketList()", e);
		}
	}

	/**
	 * Function will read all the locale folders inside the selected market folder
	 * @param sessionBean
	 */
	private void getLocalesList(ScheduleBean sessionBean)
	{
		try
		{
			sessionBean.setSelectedLocales(null);
			sessionBean.setSelectedLocalesString(null);
			sessionBean.setLocalesList(new ArrayList<SelectItemDetails>());
			if(null!=serverPath() && !"".equals(serverPath()) && null!=sessionBean.getMarketId() && !"".equals(sessionBean.getMarketId()))
			{
				String marketDirPath = serverPath();
				if(!marketDirPath.endsWith("\\"))
				{
					marketDirPath = marketDirPath+"\\";
				}
				marketDirPath = marketDirPath+sessionBean.getMarketId();
				File marketDirectory = PathUtil.file(marketDirPath);
				if(marketDirectory.exists() && marketDirectory.isDirectory())
				{
					File[] localeFoldersList = marketDirectory.listFiles();
					if(null!=localeFoldersList && localeFoldersList.length>0)
					{
						ArrayList<SelectItemDetails> tempList = new ArrayList<SelectItemDetails>();
						for(int i=0;i<localeFoldersList.length;i++)
						{
							File localeDir = localeFoldersList[i];
							SelectItemDetails si = new SelectItemDetails();
							si.setLabel(localeDir.getName());
							si.setValue(localeDir.getName());
							tempList.add(si);
							si = null;
							localeDir = null;
						}
						if(null!=tempList && tempList.size()>0)
						{
							/*
							 * add All option to the List
							 */
							SelectItemDetails si = new SelectItemDetails();
							si.setLabel(msgProps().getProperty("label.all"));
							si.setValue(ApplicationProperties.getProperty("check.all.label"));
							sessionBean.getLocalesList().add(si);
							si = null;
							sessionBean.getLocalesList().addAll(tempList);
						}
						tempList = null;
					}
					localeFoldersList = null;
				}
				else
				{
					logger.info("getLocalesList :: Market Directory at Path {"+marketDirPath+"} does not exists.");
				}
			}
			else
			{
				logger.info("getLocalesList :: Either Server Path or Market Id is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "getLocalesList()", e);
		}
	}

	/**
	 * Function will read all the models folder inside the selected market / selected locales.
	 * @param sessionBean
	 */
	private void getModelsList(ScheduleBean sessionBean)
	{
		try
		{
			sessionBean.setSelectedModels(null);
			sessionBean.setSelectedModelsString(null);
			sessionBean.setModelsList(new ArrayList<SelectItemDetails>());
			if(null!=serverPath() && !"".equals(serverPath()) && null!=sessionBean.getMarketId() && !"".equals(sessionBean.getMarketId()))
			{
				String marketDirPath = serverPath();
				if(!marketDirPath.endsWith("\\"))
				{
					marketDirPath = marketDirPath+"\\";
				}
				marketDirPath = marketDirPath+sessionBean.getMarketId();
				File marketDirectory = PathUtil.file(marketDirPath);
				if(marketDirectory.exists() && marketDirectory.isDirectory())
				{
					ArrayList<SelectItemDetails> tempList = new ArrayList<SelectItemDetails>();
					/*
					 * check here if sessionBean.getSelectedLocales is not null
					 * then fetch the models of the selected Locale only
					 * Else read all the Models Values
					 */
					if(null!=sessionBean.getSelectedLocales() && sessionBean.getSelectedLocales().length>0)
					{
						for(int i=0;i<sessionBean.getSelectedLocales().length;i++)
						{
							String locale = String.valueOf(sessionBean.getSelectedLocales()[i]);
							String localeDirPath = marketDirPath+"\\"+locale;
							File localeDirectory = PathUtil.file(localeDirPath);
							if(localeDirectory.exists() && localeDirectory.isDirectory())
							{
								File[] modelsList = localeDirectory.listFiles();
								if(null!=modelsList && modelsList.length>0)
								{
									for(int j=0;j<modelsList.length;j++)
									{
										File model = modelsList[j];
										if(model.exists() && model.isDirectory())
										{
											// ADD TO MODEL LIST
											SelectItemDetails si = new  SelectItemDetails();
											si.setLabel(model.getName()+" ("+ localeDirectory.getName()+")");
											si.setValue(model.getName()+" ("+ localeDirectory.getName()+")");
											tempList.add(si);
											si = null;
										}
										model=null;
									}
								}
								modelsList = null;
							}
							localeDirectory = null;
							localeDirPath = null;
							locale=null;
						}
					}
					else
					{
						/*
						 * Read Models of all the locales Found
						 */
						File[] localesDir = marketDirectory.listFiles();
						if(null!=localesDir && localesDir.length>0)
						{
							for(int i=0;i<localesDir.length;i++)
							{
								File localeFolder = localesDir[i];
								if(localeFolder.exists() && localeFolder.isDirectory())
								{
									File[] modelsList = localeFolder.listFiles();
									if(null!=modelsList && modelsList.length>0)
									{
										for(int j=0;j<modelsList.length;j++)
										{
											File modelDir = modelsList[j];
											if(modelDir.exists() && modelDir.isDirectory())
											{
												SelectItemDetails si = new SelectItemDetails();
												si.setLabel(modelDir.getName()+" ("+localeFolder.getName()+")");
												si.setValue(modelDir.getName()+" ("+ localeFolder.getName()+")");
												tempList.add(si);
												si = null;
											}
											modelDir=  null;
										}
									}
									modelsList=null;
								}
								localeFolder=null;
							}
						}
						localesDir = null;
					}

					if(null!=tempList && tempList.size()>0)
					{
						/*
						 * add All option to the List
						 */
						SelectItemDetails si = new SelectItemDetails();
						si.setLabel(msgProps().getProperty("label.all"));
						si.setValue(ApplicationProperties.getProperty("check.all.label"));
						sessionBean.getModelsList().add(si);
						si = null;
						sessionBean.getModelsList().addAll(tempList);
					}
					tempList = null;
				}
				else
				{
					logger.info("getModelsList :: Market Directory at Path {"+marketDirPath+"} does not exists.");
				}
			}
			else
			{
				logger.info("getModelsList :: Either Server Path or Market Id is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "getModelsList()", e);
		}
	}

	/**
	 * Function will read the documents status from properties files
	 * @param sessionBean
	 */
	private void getDocumentStatusList(ScheduleBean sessionBean)
	{
		sessionBean.setDocumentStatusList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps().getProperty("flag.label.publish"));
		si.setValue(ApplicationProperties.getProperty("flag.value.publish"));
		sessionBean.getDocumentStatusList().add(si);
		si = null;

		si = new SelectItemDetails();
		si.setLabel(msgProps().getProperty("flag.label.draft"));
		si.setValue(ApplicationProperties.getProperty("flag.value.draft"));
		sessionBean.getDocumentStatusList().add(si);
		si = null;
	}

	private void getOperationList(ScheduleBean sessionBean)
	{
		sessionBean.setOperationList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps().getProperty("operation.type.dataload"));
		si.setValue(ApplicationProperties.getProperty("operation.type.dataload"));
		sessionBean.getOperationList().add(si);
		si = null;

		si = new SelectItemDetails();
		si.setLabel(msgProps().getProperty("operation.type.automation"));
		si.setValue(ApplicationProperties.getProperty("operation.type.automation"));
		sessionBean.getOperationList().add(si);
		si = null;
	}



	/**
	 * Function will readLocales and Models information for the selected Market
	 * @param sessionBean
	 */
	private void marketSelectionOperation(ScheduleBean sessionBean)
	{
		try
		{
			/*
			 * call function to get Locale List
			 */
			getLocalesList(sessionBean);

			// set SELECTED MODELS AND MODELS LIST TO NULL
			sessionBean.setSelectedModels(null);
			sessionBean.setSelectedModelsString(null);
			sessionBean.setModelsList(new ArrayList<SelectItemDetails>());

			// set SELECTED MANUAL TYPES AND MANUAL TYPES LIST TO NULL
			sessionBean.setSelectedManualTypes(null);
			sessionBean.setSelectedManualTypesString(null);
			sessionBean.setManualTypesList(new ArrayList<SelectItemDetails>());

			/*
			 * call function to get Models List
			 */
			//			getModelsList(sessionBean);

			/*
			 * also reset the following to null
			 */
			sessionBean.setSelectedRows(null);
			sessionBean.setSelectAll(false);
			sessionBean.setScheduleItemsList(new ArrayList<ScheduleItemDetails>());
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "marketSelectionOperation()", e);
		}
	}
	/**
	 * Function will read Models for the selected Locales
	 * @param sessionBean
	 */
	private void localeSelectionOperation(ScheduleBean sessionBean)
	{
		try
		{
			// set SELECTED MANUAL TYPES AND MANUAL TYPES LIST TO NULL
			sessionBean.setSelectedManualTypes(null);
			sessionBean.setSelectedManualTypesString(null);
			sessionBean.setManualTypesList(new ArrayList<SelectItemDetails>());

			if(null!=sessionBean.getSelectedLocales() && sessionBean.getSelectedLocales().length>0)
			{
				boolean allFound=false;
				for(int a=0;a<sessionBean.getSelectedLocales().length;a++)
				{
					String token=sessionBean.getSelectedLocales()[a];
					if(null!=token && !"".equals(token))
					{
						if(token.equals(ApplicationProperties.getProperty("check.all.label")))
						{
							allFound= true;
							break;
						}
					}
					token=null;
				}

				if(allFound==true)
				{
					/*
					 * Iterate localesList and add all the localeCodes to selectedLocales
					 */
					if(null!=sessionBean.getLocalesList() && sessionBean.getLocalesList().size()>0)
					{
						String selLocaleValues="";
						for(int i=0;i<sessionBean.getLocalesList().size();i++)
						{
							SelectItemDetails si = (SelectItemDetails)sessionBean.getLocalesList().get(i);
							selLocaleValues = selLocaleValues+si.getValue();
							if(i!=sessionBean.getLocalesList().size()-1)
							{
								selLocaleValues=  selLocaleValues+",";
							}
							si = null;
						}
						if(null!=selLocaleValues && !"".equals(selLocaleValues))
						{
							if(selLocaleValues.endsWith(","))
							{
								selLocaleValues = selLocaleValues.substring(0,selLocaleValues.length()-1);
							}
							String[] array = selLocaleValues.split(",");
							// update selLocalesValue
							sessionBean.setSelectedLocales(array);
							sessionBean.setSelectedLocalesString(selLocaleValues);
							array= null;
						}
						selLocaleValues = null;
					}
				}
			}
			/*
			 * call function to get Models List
			 */
			getModelsList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "localeSelectionOperation()", e);
		}
	}
	/**
	 * Function will update the Selected Market in the SessionBean
	 * @param sessionBean
	 */
	private void modelSelectionOperation(ScheduleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedModels() && sessionBean.getSelectedModels().length>0)
			{
				boolean allFound=false;
				for(int a=0;a<sessionBean.getSelectedModels().length;a++)
				{
					String token=sessionBean.getSelectedModels()[a];
					if(null!=token && !"".equals(token))
					{
						if(token.equals(ApplicationProperties.getProperty("check.all.label")))
						{
							allFound= true;
							break;
						}
					}
					token=null;
				}

				if(allFound==true)
				{
					/*
					 * Iterate modelsList and add all the modelCodes to selectedModels
					 */
					if(null!=sessionBean.getModelsList() && sessionBean.getModelsList().size()>0)
					{
						String selModelValues="";
						for(int i=0;i<sessionBean.getModelsList().size();i++)
						{
							SelectItemDetails si = (SelectItemDetails)sessionBean.getModelsList().get(i);
							selModelValues = selModelValues+si.getValue();
							if(i!=sessionBean.getModelsList().size()-1)
							{
								selModelValues=  selModelValues+",";
							}
							si = null;
						}
						if(null!=selModelValues && !"".equals(selModelValues))
						{
							if(selModelValues.endsWith(","))
							{
								selModelValues = selModelValues.substring(0,selModelValues.length()-1);
							}
							String[] array = selModelValues.split(",");
							// update selModelValues
							sessionBean.setSelectedModels(array);
							sessionBean.setSelectedModelsString(selModelValues);
							array= null;
						}
						selModelValues = null;
					}
				}
			}
			/*
			 * call function to get Manual Types
			 */
			getManualTypesList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "modelSelectionOperation()", e);
		}
	}


	private void getManualTypesList(ScheduleBean sessionBean)
	{
		try
		{
			sessionBean.setSelectedManualTypes(null);
			sessionBean.setSelectedManualTypesString(null);
			sessionBean.setManualTypesList(new ArrayList<SelectItemDetails>());
			if(null!=serverPath() && !"".equals(serverPath()) && null!=sessionBean.getMarketId() && !"".equals(sessionBean.getMarketId()))
			{
				String marketDirPath = serverPath();
				if(!marketDirPath.endsWith("\\"))
				{
					marketDirPath = marketDirPath+"\\";
				}
				marketDirPath = marketDirPath+sessionBean.getMarketId();
				File marketDirectory = PathUtil.file(marketDirPath);
				if(marketDirectory.exists() && marketDirectory.isDirectory())
				{
					ArrayList<SelectItemDetails> tempList = new ArrayList<SelectItemDetails>();
					/*
					 * check here if sessionBean.getSelectedLocales is not null
					 * then fetch the models of the selected Locale only
					 * Else read all the Models Values
					 */
					if(null!=sessionBean.getSelectedLocales() && sessionBean.getSelectedLocales().length>0)
					{
						for(int i=0;i<sessionBean.getSelectedLocales().length;i++)
						{
							String locale = String.valueOf(sessionBean.getSelectedLocales()[i]);
							String localeDirPath = marketDirPath+"\\"+locale;
							File localeDirectory = PathUtil.file(localeDirPath);
							if(localeDirectory.exists() && localeDirectory.isDirectory())
							{
								if(null!=sessionBean.getSelectedModels() && sessionBean.getSelectedModels().length>0)
								{
									for(int j=0;j<sessionBean.getSelectedModels().length;j++)
									{
										String modelKey = String.valueOf(sessionBean.getSelectedModels()[j]);
										String model="";
										String localeModelValue ="";
										// remove locale from Model
										if(modelKey.lastIndexOf("(")!=-1)
										{
											model = modelKey.substring(0, modelKey.lastIndexOf("(")).trim();
											localeModelValue = modelKey.substring(modelKey.lastIndexOf("(")+1, modelKey.length());
											if(null!=localeModelValue)
											{
												localeModelValue = localeModelValue.replace(")", "");
												localeModelValue = localeModelValue.trim();
											}
										}

										if(locale.trim().toLowerCase().equals(localeModelValue.trim().toLowerCase()))
										{
											/*
											 * MODEL is of the SELECTED LOCALE
											 */
											String modelDirPath = localeDirPath+"\\"+model;
											File modelDir =PathUtil.file(modelDirPath);
											if(modelDir.exists() && modelDir.isDirectory())
											{
												tempList = modelDirOperationForManualTypeCombo(modelDir, tempList);
											}
											modelDir = null;
											modelDirPath=  null;
										}
										model = null;
									}
								}
								else
								{
									/*
									 * NO MODEL IS SELECTED - ITERATE FOR EACH MODEL AND ADD MANUAL TYPES
									 */
									File[] modelsList = localeDirectory.listFiles();
									if(null!=modelsList && modelsList.length>0)
									{
										for(int j=0;j<modelsList.length;j++)
										{
											File model = modelsList[j];
											if(model.exists() && model.isDirectory())
											{
												tempList = modelDirOperationForManualTypeCombo(model, tempList);
											}
											model=null;
										}
									}
									modelsList = null;
								}
							}
							localeDirectory = null;
							localeDirPath = null;
							locale=null;
						}
					}
					else
					{
						/*
						 * Read Models of all the locales Found
						 */
						File[] localesDir = marketDirectory.listFiles();
						if(null!=localesDir && localesDir.length>0)
						{
							for(int i=0;i<localesDir.length;i++)
							{
								File localeFolder = localesDir[i];
								if(localeFolder.exists() && localeFolder.isDirectory())
								{
									File[] modelsList = localeFolder.listFiles();
									if(null!=modelsList && modelsList.length>0)
									{
										for(int j=0;j<modelsList.length;j++)
										{
											File modelDir = modelsList[j];
											if(modelDir.exists() && modelDir.isDirectory())
											{
												tempList= modelDirOperationForManualTypeCombo(modelDir, tempList);
											}
											modelDir=  null;
										}
									}
									modelsList=null;
								}
								localeFolder=null;
							}
						}
						localesDir = null;
					}

					if(null!=tempList && tempList.size()>0)
					{
						/*
						 * add All option to the List
						 */
						SelectItemDetails si = new SelectItemDetails();
						si.setLabel(msgProps().getProperty("label.all"));
						si.setValue(ApplicationProperties.getProperty("check.all.label"));
						sessionBean.getManualTypesList().add(si);
						si = null;
						sessionBean.getManualTypesList().addAll(tempList);
					}
					tempList = null;
				}
				else
				{
					logger.info("getManualTypesList :: Market Directory at Path {"+marketDirPath+"} does not exists.");
				}
			}
			else
			{
				logger.info("getManualTypesList :: Either Server Path or Market Id is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "getManualTypesList()", e);
		}
	}

	private ArrayList<SelectItemDetails> modelDirOperationForManualTypeCombo(File modelDir, ArrayList<SelectItemDetails> tempList)
	{
		try
		{
			File[] manualTypesFolder = modelDir.listFiles();
			if(null!=manualTypesFolder && manualTypesFolder.length>0)
			{
				for(int k=0;k<manualTypesFolder.length;k++)
				{
					File manualTypeDir = manualTypesFolder[k];
					if(null!=manualTypeDir.getName() && !"".equals(manualTypeDir.getName()))
					{
						boolean addToList = true;
						if(null!=tempList && tempList.size()>0)
						{
							for(int y=0;y<tempList.size();y++)
							{
								SelectItemDetails existDetails = (SelectItemDetails)tempList.get(y);

								if(existDetails.getValue().trim().toLowerCase().equals(manualTypeDir.getName().toLowerCase()))
								{
									// already Added
									addToList= false;
									break;
								}
							}
						}

						if(addToList == true)
						{
							/*
							 * GET MANUAL TYPE ON THE BASIS OF MANUAL CODE
							 * 
							 * NO NEED OF GETTTIGN MANUAL TYPE - JUST SET THE FOLDER NAME
							 * DATE 18 MAY 2017
							 */ 
							/*
							String searchKey=manualTypeDir.getName();
							String modelFolderName = modelDir.getName();
							if(null!=modelFolderName && !"".equals(modelFolderName))
							{
								String afterFirstIndex="";
								if(modelFolderName.indexOf("_")!=-1)
								{
									// SET MODEL TYPE HERE
									afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
								}
								// NOW IDENTIFY MODEL 
								if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
								{
									if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) ||
										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) ||
										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
									{
										// OVER HAUL MANUALS. - SEARCH KEY WILL BE MODEL
										searchKey = afterFirstIndex;
									}
								}
								afterFirstIndex = null;
							}
							modelFolderName=  null;

							ManualTypeDetails mtDetails = ScheduleDAO.getManualTypeDetailsOnManualCode(searchKey);
							if(null!=mtDetails && null!=mtDetails.getManualTypeName() && 
									!"".equals(mtDetails.getManualTypeName()))
							{
								// ADD TO MODEL LIST
								SelectItemDetails si = new  SelectItemDetails();
								si.setLabel(mtDetails.getManualTypeName());
								si.setValue(manualTypeDir.getName());
								tempList.add(si);
								si = null;
							}
							mtDetails = null;
							searchKey = null;
							 */

							// ADD TO MODEL LIST
							SelectItemDetails si = new  SelectItemDetails();
							si.setLabel(manualTypeDir.getName().toUpperCase());
							si.setValue(manualTypeDir.getName());
							tempList.add(si);
							si = null;
						}
					}
					manualTypeDir = null;
				}
			}
			manualTypesFolder= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "modelDirOperationForManualTypeCombo()", e);
		}
		return tempList;
	}


	/**
	 * Function will perform reset Operation
	 * @param sessionBean
	 */
	private void resetOperation(ScheduleBean sessionBean)
	{
		sessionBean.setSuccessMessage(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setScheduleName(null);
		sessionBean.setMarketList(null);
		sessionBean.setMarketId(null);
		sessionBean.setLocalesList(null);
		sessionBean.setModelsList(null);
		sessionBean.setScheduleItemsList(null);
		sessionBean.setSelectedLocales(null);
		sessionBean.setSelectedLocalesString(null);
		sessionBean.setSelectedModels(null);
		sessionBean.setSelectedModelsString(null);
		sessionBean.setSelectedManualTypes(null);
		sessionBean.setSelectedManualTypesString(null);
		sessionBean.setManualTypesList(null);
		// set default Value for the Document Status
		sessionBean.setDocumentStatusId(ApplicationProperties.getProperty("flag.value.publish")); sessionBean.setLoadTypeId(ScheduleDAO.LOAD_TYPE_ONLY_CONTENT);
		sessionBean.setDocumentStatusList(null);
		sessionBean.setDisplayPageLength(null);
		sessionBean.setDisplayPageNo(null);
		sessionBean.setSelectedRows(null);
		sessionBean.setActionClicked(null);

		/*
		 * call function to read Market List
		 */
		getMarketList(sessionBean);

		/*
		 * call function to get the Document Status List
		 */
		getDocumentStatusList(sessionBean);
	}

	private void resetATOperation(ScheduleBean sessionBean)
	{
		sessionBean.setAutomationItemsList(null);
		sessionBean.setDisplayATPageLength(null);
		sessionBean.setDisplayATPageNo(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSuccessMessage(null);
		sessionBean.setSelectATAll(false);
		sessionBean.setSelectedATRows(null);
		sessionBean.setScheduleATName(null);
		sessionBean.setShowAutomationGridBlock(false);
		sessionBean.setInputDataList(null);
		sessionBean.setShowIdenitfyButton(false);
		sessionBean.setJobIdForATOperation(null);
		sessionBean.setAtLoadTypeId(ScheduleDAO.LOAD_TYPE_ONLY_CONTENT);
		
		// set today's date as ToDate
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sessionBean.setToDate(sdf.format(new Date()));
		// get 10 days before today's date
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, -30);
		Date dateBefore10Days = cal.getTime();
		sessionBean.setFromDate(sdf.format(dateBefore10Days));
		cal  =null;
		dateBefore10Days = null;

		/*
		 * call function to get AT Docs Identification Jobs List
		 */
		getAtDocsIdentificationScheduleList(sessionBean);
	}
	
	private void getAtDocsIdentificationScheduleList(ScheduleBean sessionBean)
	{
		sessionBean.setAtDocsIdentificationJobList(new ArrayList<ScheduleDetails>());
		try
		{
			if(null!=sessionBean.getFromDate() && !"".equals(sessionBean.getFromDate()) && null!=sessionBean.getToDate()  && !"".equals(sessionBean.getToDate()))
			{
				List<ScheduleDetails> tempList = AutomationIdentificationDAO.getScheduleList(sessionBean.getFromDate(), sessionBean.getToDate());
				if(null!=tempList && tempList.size()>0)
				{
					ScheduleDetails details = null;
					for(int a=0;a<tempList.size();a++)
					{
						details = (ScheduleDetails)tempList.get(a);
						/*
						 * CHECK IF THE WSL ID IS FROM GMS3_INTERNAL_TEAM_USERS
						 * THEN SHOW OKADMIN
						 */
						if(null!=details.getUserId() && !"".equals(details.getUserId()))
						{
							// INTERNAL USERS
							StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS"),",");
							while(str.hasMoreTokens())
							{
								String tok = str.nextToken();
								if(tok.trim().toLowerCase().equals(details.getUserId().trim().toLowerCase()))
								{
									details.setUserId(ApplicationProperties.getProperty("USERNAME"));
								}
								tok = null;
							}
							str = null;
						}
						
						details.setShowAbort(false);
						details.setShowViewButton(false);
						if(null!=details.getScheduleStatus() && !"".equals(details.getScheduleStatus()))
						{
							if(details.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value")))
							{
								details.setScheduleStatusLabel(msgProps().getProperty("schedule.status.pending.label"));
							}
							else if(details.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
							{
								details.setScheduleStatusLabel(msgProps().getProperty("schedule.status.processing.label"));
								// set showAbortLink to true
								details.setShowAbort(true);
							}
							else if(details.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.completed.value")))
							{
								details.setScheduleStatusLabel(msgProps().getProperty("schedule.status.completed.label"));
								// set showViewButton to true - only if Documents Count > 0 and an item is left to schedule
								// (the count stays after an update job took the items; the remark says by which job)
								if(details.getTotalDocsForProcessing()>0 && 0!=details.getRemainingItemsCount())
								{
									details.setShowViewButton(true);
								}
							}
							else if(details.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.aborted.value")))
							{
								details.setScheduleStatusLabel(msgProps().getProperty("schedule.status.aborted.label"));
							}
						}
						
						// Job Status Label
						if(null!=details.getJobStatus() && !"".equals(details.getJobStatus()))
						{
							if(details.getJobStatus().equals(ApplicationProperties.getProperty("schedule.status.success.value")))
							{
								details.setJobStatusLabel(msgProps().getProperty("schedule.status.success.label"));
							}
							else if(details.getJobStatus().equals(ApplicationProperties.getProperty("schedule.status.failure.value")))
							{
								details.setJobStatusLabel(msgProps().getProperty("schedule.status.failure.label"));
							}
						}
						
						// add to scheduleList
						sessionBean.getAtDocsIdentificationJobList().add(details);
						details = null;
					}
					details = null;
				}
				tempList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "getAtDocsIdentificationScheduleList()", e);
		}
	}

	@SuppressWarnings("deprecation")
	private void abortConversionOperation(ScheduleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getJobIdForATOperation() && !"".equals(sessionBean.getJobIdForATOperation()))
			{
				String scheduleId=sessionBean.getJobIdForATOperation();
				String threadId=null;
				String scheduleName=null;
				if(null!=sessionBean.getAtDocsIdentificationJobList() && sessionBean.getAtDocsIdentificationJobList().size()>0)
				{
					ScheduleDetails dt = null;
					for(int a=0;a<sessionBean.getAtDocsIdentificationJobList().size();a++)
					{
						dt = (ScheduleDetails) sessionBean.getAtDocsIdentificationJobList().get(a);
						if(String.valueOf(dt.getScheduleId()).equals(scheduleId))
						{
							// set threadId
							threadId = dt.getThreadId();
							// set scheduleName
							scheduleName = dt.getScheduleName();
							break;
						}
						dt = null;
					}
					dt = null;
				}
				
				if(null!=threadId && !"".equals(threadId))
				{
					logger.info("abortConversionOperation :: Killing Thread Id {"+ threadId+"} for Schedule ::> " + scheduleId);
					
					/*
					 * Get all the Active Threads and match the thread on the
					 * basis of NAME
					 */
					Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
					Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
					if (null != threadArray && threadArray.length > 0) 
					{
						boolean threadFound = false;
						for (int i = 0; i < threadArray.length; i++) 
						{
							Thread at = threadArray[i];
							if (null != at.getName() && !"".equals(at.getName())) {
								if (at.getName().equals(threadId)) 
								{
									threadFound = true;
									try 
									{
										if (at.isAlive()) 
										{
											com.mazda.gms3.dmt.utils.ThreadAbortUtil.abort(at);
										}
									} 
									catch (Exception e) 
									{
										Utilities.printStackTraceToLogs(Schedule.class.getName(), "abortConversion()", e);
										// no need of showing any error
										logger.info(msgProps().getProperty("error.message.impacted.docs.already.finish")+" :: Proceed for updaitng Job Status in DATABASE.");
									}
									break;
								}
							}
						}
						
						if (threadFound == false) 
						{
							logger.info(msgProps().getProperty("error.message.impacted.docs.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
						}
					} 
					else
					{
						logger.info(msgProps().getProperty("error.message.impacted.docs.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
					}
					threadArray = null;
					threadSet = null;
					
					/*
					 * call function to update ABort Status in DATABASE - IRRELEVANT OF WHETHEER THREAD WAS ACTIVE IN CONTIANER OR NOT
					 * THIS WILL HELP TO AVOID UPDATING STATUS MANUALLY IN DAATBASE.
					 */
					try
					{
						AutomationIdentificationDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.aborted.value"), 
								ApplicationProperties.getProperty("schedule.status.failure.value"), true, null);
						
						/*
						 * PROCEED FOR SENDING EMAILS
						 */
						ArrayList<String> toList = new ArrayList<String>();
						String userId = ApplicationProperties.getProperty("NOTIF_EMAIL_TO_ID");
						if(null!=userId && !"".equals(userId))
						{
							String[] tok = userId.split(",");
							if(null!=tok && tok.length>0)
							{
								for(int c=0;c<tok.length;c++)
								{
									toList.add(tok[c].trim().toString());
								}
							}
							tok = null;
						}
						String fromEmailId=ApplicationProperties.getProperty("NOTIF_EMAIL_FROM_ID");
						String subject="FACELIFT - DOCUMENTS IDETIFICATION JOB - "+ scheduleName+" - ABORTED";
						SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
						StringBuilder str = new StringBuilder();
						str.append("<p>Dear User,</p>");
						str.append("<p>FaceLift Documents Identification Job <b>"+scheduleName+"</b> has been Aborted Successfully at "+sdf.format(new Date())+".</p>");
						str.append("<p>For more details please login to DMT Application.</p>");
						str.append("<p>Regards,<br />MGSS Application.<br />Note - This is an auto generated email. Please do not respond to this email.</p>");
						
						SendMailUsingAuthentication.newPostHTMLMail(toList, subject, str.toString(), fromEmailId);
						
						toList = null;
						fromEmailId = null;
						subject = null;
						str = null;
						sdf = null;
						userId = null;
						
						// show success Message - independent of whether thread was active or not
						sessionBean.setSuccessMessage(msgProps().getProperty("job.abort.success.message"));
						/*
						 * re-call Schedule List
						 */
						getAtDocsIdentificationScheduleList(sessionBean);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Schedule.class.getName(), "abortConversionOperation()", e);
						logger.info("abortConversionOperation :: Error while updating status for Schedule Id {"+scheduleId+"} in DATABASE.");
						sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
					}
				}
				else
				{
					logger.info("abortConversionOperation :: Thread Id from request as param are null. Throw Message");
					sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
				}
				threadId = null;
				scheduleId = null;
			}
			else
			{
				logger.info("abortConversionOperation :: Schedule Id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "abortConversionOperation()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}
	
	
	/**
	 * Function will validate Search Processing Files GO Operation
	 * @param sessionBean
	 * @return
	 */
	private boolean validate(ScheduleBean sessionBean)
	{
		if(null==sessionBean.getMarketId() || "".equals(sessionBean.getMarketId()))
		{
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.fields"));
			return false;
		}
		return true;
	}
	/**
	 * Function will initiate File identification Operation
	 * @param sessionBean
	 */
	private void filterOperation(ScheduleBean sessionBean)
	{
		sessionBean.setShowNoRecordTable(false);
		sessionBean.setSelectAll(false);
		try
		{
			if(validate(sessionBean))
			{
				// MNAO: same facelift folder layout as MC / MME since the new content structure
				if(sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()) || sessionBean.getMarketId().trim().toLowerCase().equals
						(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()) || sessionBean.getMarketId().trim().toLowerCase().equals
						(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
				{
					/*
					 * WHEN MC, MME OR MNAO MARKET
					 */
					identifyFileItemsOperationForMC(sessionBean);
				}

				if(null!=sessionBean.getScheduleItemsList() && sessionBean.getScheduleItemsList().size()>0)
				{
					// set all rows as Selected to false
					sessionBean.setSelectAll(false);
					/*
					 * iterate list and set rowSelectedFlag to true for all rows
					 */
					for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
					{
						ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
						// set all rows on load as selected to False
						schItemDetails.setRowSelected(false);
					}
				}
				else
				{
					sessionBean.setShowNoRecordTable(true);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "filterOperation()", e);
		}
	}

	/**
	 * Function will identify all the Processing files count, okAssets count, Deletion Files Count and OkAssets Delete Count.
	 * @param sessionBean
	 */
	private void identifyFileItemsOperationForMC(ScheduleBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		try
		{
			sessionBean.setScheduleItemsList(null);
			ArrayList<ScheduleItemDetails> tempItemsList = new ArrayList<ScheduleItemDetails>();
			if(null!=sessionBean.getMarketId() && !"".equals(sessionBean.getMarketId()) 
					&& null!=serverPath() && !"".equals(serverPath()))
			{
				String[] localesToProcess=null;
				String[] modelsToProcess=null;
				String[] manualTypesToProcess = null;
				/*
				 * check and remove All from both the locales & models
				 */
				if(null!=sessionBean.getSelectedLocales() && sessionBean.getSelectedLocales().length>0)
				{
					String localeData = "";
					for(int i=0;i<sessionBean.getSelectedLocales().length;i++)
					{
						String localeValue = String.valueOf(sessionBean.getSelectedLocales()[i]);
						if(null!=localeValue && !"".equals(localeValue) && !ApplicationProperties.getProperty("check.all.label").equals(localeValue))
						{
							localeData = localeData+localeValue+",";
						}
						localeValue = null;
					}
					if(null!=localeData && !"".equals(localeData))
					{
						if(localeData.endsWith(","))
						{
							localeData= localeData.substring(0,localeData.length()-1);
						}
						localesToProcess = localeData.split(",");
					}
				}

				if(null!=sessionBean.getSelectedModels() && sessionBean.getSelectedModels().length>0)
				{
					String modelData = "";
					for(int i=0;i<sessionBean.getSelectedModels().length;i++)
					{
						String modelValue = String.valueOf(sessionBean.getSelectedModels()[i]);
						if(null!=modelValue && !"".equals(modelValue) && !ApplicationProperties.getProperty("check.all.label").equals(modelValue))
						{
							modelData = modelData+modelValue+",";
						}
						modelValue = null;
					}
					if(null!=modelData && !"".equals(modelData))
					{
						if(modelData.endsWith(","))
						{
							modelData= modelData.substring(0,modelData.length()-1);
						}
						modelsToProcess = modelData.split(",");
					}
					modelData = null;
				}

				if(null!=sessionBean.getSelectedManualTypes() && sessionBean.getSelectedManualTypes().length>0)
				{
					String manualTypeData = "";
					for(int i=0;i<sessionBean.getSelectedManualTypes().length;i++)
					{
						String manualTypeValue = String.valueOf(sessionBean.getSelectedManualTypes()[i]);
						if(null!=manualTypeValue && !"".equals(manualTypeValue) && !ApplicationProperties.getProperty("check.all.label").equals(manualTypeValue))
						{
							manualTypeData = manualTypeData+manualTypeValue+",";
						}
						manualTypeValue = null;
					}
					if(null!=manualTypeData && !"".equals(manualTypeData))
					{
						if(manualTypeData.endsWith(","))
						{
							manualTypeData= manualTypeData.substring(0,manualTypeData.length()-1);
						}
						manualTypesToProcess = manualTypeData.split(",");
					}
					manualTypeData = null;
				}

				/*
				 * Now Create path for the MarKET DIRECTORY 
				 */
				String marketDirPath="";
				SERVERPATH.set(serverPath().replace("/", "\\"));
				if(serverPath().endsWith("\\"))
				{
					marketDirPath = serverPath()+sessionBean.getMarketId();
				}
				else
				{
					marketDirPath = serverPath()+"\\"+sessionBean.getMarketId();
				}
				logger.info("identifyFileItemsOperationForMC :: Iterating Market Directory at Path :: > " + marketDirPath);
				File marketFolder = PathUtil.file(marketDirPath);
				if(marketFolder.exists() && marketFolder.isDirectory())
				{
					// Get all the Locale directory
					File[] localesFoldersList = marketFolder.listFiles();
					if(null!=localesFoldersList && localesFoldersList.length>0)
					{
						/*
						 * check here if localesToProcess is not null
						 * 	then only allow the Ones selected for processing
						 * else process all Locales
						 */
						for(int i=0;i<localesFoldersList.length;i++)
						{
							File localeFolder = localesFoldersList[i];
							boolean processLocale=true;
							if(null!=localesToProcess && localesToProcess.length>0)
							{
								boolean localeSelected=false;
								for(int a=0;a<localesToProcess.length;a++)
								{
									String selLocale=localesToProcess[a];
									if(selLocale.trim().toLowerCase().equals(localeFolder.getName().trim().toLowerCase()))
									{
										// localeSelected to true
										localeSelected = true;
										break;
									}
									selLocale= null;
								}
								if(localeSelected==false)
								{
									// processLocale = false
									processLocale = false;
								}
							}

							if(processLocale==true)
							{
								// Get all the Models Directory
								File[] modelsFoldersList = localeFolder.listFiles();
								if(null!=modelsFoldersList && modelsFoldersList.length>0)
								{
									/*
									 * check here if modelsToProcess is not null
									 * 	then only allow the Ones selected for processing
									 * else process all Models
									 */
									for(int j=0;j<modelsFoldersList.length;j++)
									{
										File modelFolder = modelsFoldersList[j];
										boolean processModel=true;
										if(null!=modelsToProcess && modelsToProcess.length>0)
										{
											boolean modelSelected=false;
											for(int b=0;b<modelsToProcess.length;b++)
											{
												String selModel = modelsToProcess[b];
												/*
												 * FROM THE SEL MODEL, remove (localeName)
												 * Identify the localeName from selModel Name
												 */
												String modelLocale = "";
												if(null!=selModel && !"".equals(selModel))
												{
													if(selModel.lastIndexOf("(")!=-1)
													{
														// FIRST EXTRACT MODEL LOCALE ELSE, IT WILL ALWAYS BE EMPTY
														modelLocale = selModel.substring(selModel.lastIndexOf("(")+1, selModel.length());
														selModel = selModel.substring(0,selModel.lastIndexOf("(")).trim();
														if(null!=modelLocale && !"".equals(modelLocale))
														{
															if(modelLocale.lastIndexOf(")")!=-1)
															{
																modelLocale = modelLocale.substring(0, modelLocale.lastIndexOf(")"));
															}
														}
													}

													if(null!=selModel && !"".equals(selModel) 
															&& null!=modelLocale && !"".equals(modelLocale))
													{
														if(selModel.trim().toLowerCase().equals(modelFolder.getName().trim().toLowerCase()) && 
																modelLocale.trim().toLowerCase().equals(localeFolder.getName().trim().toLowerCase()))
														{
															// model selected
															modelSelected =true;
															break;
														}
													}
												}
												selModel=  null;
												modelLocale=  null;
											}

											if(modelSelected==false)
											{
												// set processModel to false
												processModel= false;
											}
										}

										if(processModel==true)
										{
											/*
											 * Start Counting Files for the Model for each Manual Type
											 */
											File[] manualTypeFoldersList = modelFolder.listFiles();
											if(null!=manualTypeFoldersList && manualTypeFoldersList.length>0)
											{
												for(int m=0;m<manualTypeFoldersList.length;m++)
												{
													File manualFolder = manualTypeFoldersList[m];
													// e-WD / WM / WD / BSM / SH / A01 / A02 etc.
													if(manualFolder.exists() && manualFolder.isDirectory())
													{
														boolean processManualType=true;
														if(null!=manualTypesToProcess && manualTypesToProcess.length>0)
														{
															boolean manualTypeSelected=false;
															for(int a=0;a<manualTypesToProcess.length;a++)
															{
																String selManualType=manualTypesToProcess[a];
																if(selManualType.trim().toLowerCase().equals(manualFolder.getName().trim().toLowerCase()))
																{
																	// manualTypeSelected to true
																	manualTypeSelected = true;
																	break;
																}
																selManualType= null;
															}
															if(manualTypeSelected==false)
															{
																// processManualType = false
																processManualType = false;
															}
														}

														if(processManualType == true)
														{
															/*
															 * Here, start with Face-lift Folders.
															 * identify whether Face-lift Folder Contains any Folders & Files
															 * 	if contains Folders= then it is Material Folder
															 * 	if contains Files only = then second Flow
															 */
															File[] faceliftFoldersList = manualFolder.listFiles();
															if(null!=faceliftFoldersList && faceliftFoldersList.length>0)
															{
																/*
																 * verify for each processingFaceLift Folder
																 * either all material folder must exist - mentioned in VIN.TEXT Files
																 * or no material folder must exist
																 */
																for(int w=0;w<faceliftFoldersList.length;w++)
																{
																	File faceLiftFolder = faceliftFoldersList[w];
																	if(faceLiftFolder.exists() && faceLiftFolder.isDirectory())
																	{
																		File[] dummyFoldersList = faceLiftFolder.listFiles();
																		if(null!=dummyFoldersList && dummyFoldersList.length>0)
																		{
																			ArrayList<DisplayOrderDetails> allDisplayOrderList = new ArrayList<DisplayOrderDetails>();
																			ArrayList<CDProcessingDetails> allCDProcessingDetailsList = new ArrayList<CDProcessingDetails>();
																			ArrayList<VinDetails> allVINList = new ArrayList<VinDetails>();
																			ArrayList<String> uniqueMaterialFoldersListinVinFile = new ArrayList<String>();
																			/*
																			 * SCM VIN APPLICABLE ONLY FOR MME MATKET AND WM MANUAL TYPE
																			 */
																			ArrayList<VinDetails> allSCMVinList = new ArrayList<VinDetails>();
																			
																			boolean allMaterialFoldersExists = false;

																			/*
																			 * BEFORE MOVING FURTHER JUST VERIFY IF FACELIFT FOLDER CONTAINS ANY VIN.TXT FILE OR NOT
																			 * DATE 25 JULY 2019 (NOT APPLICABLE FOR ENGINE / AT / MT) 
																			 * ALSO VERIFY IF CONTAINS DISPLAY ORDER FILE OR NOT
																			 */
																			boolean vinFileFound =false;
																			boolean displayOrderFileFound = false;
																			for(int a=0;a<dummyFoldersList.length;a++)
																			{
																				File dummyFile = dummyFoldersList[a];
																				if(dummyFile.exists())
																				{
																					// d01.txt or CD00.txt or vin.txt
																					if(dummyFile.isFile())
																					{
																						if(com.mazda.gms3.dmt.utils.MetadataFiles.isVinFile(dummyFile.getName()))
																						{
																							vinFileFound = true;
																							break;
																						}
																					}
																				}
																				dummyFile = null;
																			}
																			// for displayOrder
																			for(int a=0;a<dummyFoldersList.length;a++)
																			{
																				File dummyFile = dummyFoldersList[a];
																				if(dummyFile.exists())
																				{
																					// d01.txt or CD00.txt or vin.txt
																					if(dummyFile.isFile())
																					{
																						if(dummyFile.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.displayorder")) 
																								&& !dummyFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("file.name.delete") + ApplicationProperties.getProperty("extension.txt")) &&  
																								dummyFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
																						{
																							displayOrderFileFound = true;
																							break;
																						}
																					}
																				}
																				dummyFile = null;
																			}

																			/*
																			 * ITERATE FACELIFT FOLDER AND READ DATA FOR ALL THE 
																			 * DISPLAY ORDER FILES
																			 * CD PROCESSING FILES
																			 * SCM VIN FILE
																			 */
																			for(int a=0;a<dummyFoldersList.length;a++)
																			{
																				File dummyFile = dummyFoldersList[a];
																				if(dummyFile.exists())
																				{
																					// d01.txt or CD00.txt
																					if(dummyFile.isFile())
																					{
																						/*
																						 * IDENTIFY MODEL & 
																						 * CHECK IF MODEL IS ENGINE / AT / MT
																						 */
																						String model="";
																						String modelType="";
																						String modelFolderName = com.mazda.gms3.dmt.mc.utils.ConversionUtils.modelFolderForParsing(modelFolder.getName(), sessionBean.getMarketId());
																						if(null!=modelFolderName && !"".equals(modelFolderName))
																						{
																							String afterFirstIndex="";
																							if(modelFolderName.indexOf("_")!=-1)
																							{
																								// SET MODEL TYPE HERE
																								modelType= modelFolderName.substring(0, modelFolderName.indexOf("_"));
																								afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
																							}

																							// NOW IDENTIFY MODEL 
																							if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
																							{
																								if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
																								{
																									// OVER HAUL MANUALS.
																									model = afterFirstIndex;
																								}
																								else
																								{
																									// NORMAL MODEL -> cx-3_ab. Check for Last Index, because model could be Nissan_familia_van_bv
																									if(afterFirstIndex.lastIndexOf("_")!=-1)
																									{
																										model = afterFirstIndex.substring(0, afterFirstIndex.lastIndexOf("_"));
																									}
																									else
																									{
																										// set all as Model Name
																										model = afterFirstIndex;
																									}
																								}
																							}
																							afterFirstIndex = null;
																						}
																						modelFolderName = null;

																						if(com.mazda.gms3.dmt.utils.MetadataFiles.isVinFile(dummyFile.getName()))
																						{
																							ArrayList<VinDetails> tempList = new ArrayList<VinDetails>();
																							if(sessionBean.getMarketId().trim().toLowerCase().equals
																									(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()))
																							{
																								// MC Market
																								tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readVINTextFile(dummyFile,"N");
																							}
																							else if(sessionBean.getMarketId().trim().toLowerCase().equals
																									(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
																							{
																								// MME Market
																								tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readVINTextFileForMME(dummyFile,"N"); 
																							}
																							else if(sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
																							{
																								// MNAO Market: 11 fields, the model and year included
																								tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readVINTextFileForMNAO(dummyFile,"N");
																							}
																							if(null!=tempList && tempList.size()>0)
																							{
																								allVINList.addAll(tempList);
																							}
																							tempList= null;
																						}
																						
																						/*
																						 * SCM TXT FILE FOR SCHM.txt FILE
																						 * DATE 21 12 2022
																						 */
																						if(dummyFile.getName().trim().toLowerCase().contains(ApplicationProperties.getProperty("file.scm.vin.ent")) && 
																								dummyFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
																						{
																							/*
																							 * only for WM Channel
																							 */
																							ArrayList<VinDetails> tempList = new ArrayList<VinDetails>();
																							if(sessionBean.getMarketId().trim().toLowerCase().equals
																									(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()) && 
																									manualFolder.getName().trim().toLowerCase().equals("wm"))
																							{
																								// MME Market
																								tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readSCMVINTextFileForMME(dummyFile,"N"); 
																							}
																							if(null!=tempList && tempList.size()>0)
																							{
																								allSCMVinList.addAll(tempList);
																							}
																							tempList= null;
																						}

																						if(dummyFile.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.displayorder")) 
																								&& !dummyFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("file.name.delete") + ApplicationProperties.getProperty("extension.txt")) &&  
																								dummyFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
																						{
																							// DISPLAY ORDER TEXT FILE FOUND.
																							/*
																							 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
																							 * ALSO, FOR THESE MODELS, FETCH THE MASTER VIN LIST AND ADD TO EACH DOCUMENT OF THE MATERIAL FOLDER.
																							 * FETCH ENGINE TYPE & MISSION TYPES FOR THE BOOK TO BE APPLIED AS CATEGORY WITH EACH OF THE DOCUMENT
																							 */
																							if(null!=model && (model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) 
																									|| model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
																									|| model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
																							{
																								/*
																								 * NO CHECK REQUIRED HERE ON THE BASIS OF VIN TEXT FILE FOUND HERE
																								 * DATE 25 JULY 2019
																								 */
																								ArrayList<DisplayOrderDetails> tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readDisplayOrderTextFileForEngineATMT(dummyFile, modelType,"N");
																								if(null!=tempList && tempList.size()>0)
																								{
																									allDisplayOrderList.addAll(tempList);
																								}
																								tempList= null;
																							}
																							else
																							{
																								ScheduleItemDetails itemDetails = new ScheduleItemDetails();
																								itemDetails.setModelType(modelType);
																								ArrayList<DisplayOrderDetails> tempList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readDisplayOrderTextFile(dummyFile, itemDetails, "N");
																								if(null!=tempList && tempList.size()>0)
																								{
																									allDisplayOrderList.addAll(tempList);
																								}
																								tempList= null;
																								itemDetails= null;
																							}
																						}
																						else if(dummyFile.getName().trim().toLowerCase().startsWith(ApplicationProperties.getProperty("file.name.cdprocessing"))  &&   
																								dummyFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.txt")))
																						{
																							// CD PROCESSING TEXT FILE FOUND.
																							/*
																							 * DO THIS ONLY WHEN MODEL IS ENGINE / AT / MT
																							 */
																							if(null!=model && (model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																									model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) 
																									|| model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
																									|| model.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase())))
																							{
																								ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFileForEngineATMT(dummyFile, modelType, "N");
																								if(null!=tempList && tempList.size()>0)
																								{
																									allCDProcessingDetailsList.addAll(tempList);
																								}
																								tempList= null;
																							}
																							else
																							{
																								ScheduleItemDetails itemDetails = new ScheduleItemDetails();
																								itemDetails.setModelType(modelType);
																								ArrayList<CDProcessingDetails> tempList = CDProcessingUtils.readCDProcessingTextFile(dummyFile, modelType, "N");
																								if(null!=tempList && tempList.size()>0)
																								{
																									allCDProcessingDetailsList.addAll(tempList);
																								}
																								tempList= null;
																								itemDetails= null;
																							}
																						}

																						model = null;
																						modelType= null;
																					}
																				}
																			}

																			/*
																			 * identify uniqueMaterialFoldersList from allVINList of the processingFaceLiftFolder 
																			 */
																			if(null!=allVINList && allVINList.size()>0)
																			{
																				VinDetails vCheck = null;
																				boolean addToUniqueList= true;
																				for(int rte=0;rte<allVINList.size();rte++)
																				{
																					vCheck = (VinDetails)allVINList.get(rte);
																					if(null!=vCheck.getMaterialFolder() && !"".equals(vCheck.getMaterialFolder()))
																					{
																						addToUniqueList = true;
																						if(null!=uniqueMaterialFoldersListinVinFile && uniqueMaterialFoldersListinVinFile.size()>0)
																						{
																							for(int yut = 0;yut<uniqueMaterialFoldersListinVinFile.size();yut++)
																							{
																								if(vCheck.getMaterialFolder().trim().toLowerCase().equals(uniqueMaterialFoldersListinVinFile.get(yut).trim().toLowerCase()))
																								{
																									// already added
																									addToUniqueList=false;
																									break;
																								}
																							}
																						}
																						if(addToUniqueList==true)
																						{
																							// add to uniqueList
																							uniqueMaterialFoldersListinVinFile.add(vCheck.getMaterialFolder());
																						}
																					}
																					vCheck=  null;
																				}
																				vCheck = null;
																			}

																			/*
																			 * IDENTIFY IF ALL MATERIAL FOLDER IDENTIFIED FROM ALL VIN.TXT FILE
																			 * EXISTS PHYSICALLY OR NOT
																			 * IF YES - THEN PROCEED
																			 * 
																			 * IF EXTRA MATERIAL FOLDERS FOUND - DONT BOTHER, LET THEM PROCESS
																			 */
																			if(null!=dummyFoldersList && dummyFoldersList.length>0 && null!=uniqueMaterialFoldersListinVinFile
																					&& uniqueMaterialFoldersListinVinFile.size()>0)
																			{
																				int foundMaterialFoldersCount=0;
																				for(int tyt=0;tyt<uniqueMaterialFoldersListinVinFile.size();tyt++)
																				{
																					for(int a=0;a<dummyFoldersList.length;a++)
																					{
																						File dummyFile = dummyFoldersList[a];
																						if(dummyFile.exists() && dummyFile.isDirectory())
																						{
																							if(uniqueMaterialFoldersListinVinFile.get(tyt).toString().trim().toLowerCase().equals(dummyFile.getName().trim().toLowerCase()))
																							{
																								// material Folder exists physically.
																								foundMaterialFoldersCount++;
																							}
																						}
																						dummyFile = null;
																					}
																				}

																				if(foundMaterialFoldersCount==uniqueMaterialFoldersListinVinFile.size())
																				{
																					// allMaterialFolders exists
																					allMaterialFoldersExists = true;
																				}
																				foundMaterialFoldersCount = 0;
																			}
																			/*
																			 * CHECK IF MATERIAL FOLDER EXISTS OR NOT FOR THE PROCESSING
																			 * FACELIFT FOLDER
																			 */
																			boolean anyMaterialDirectoryExists = false;
																			for(int a=0;a<dummyFoldersList.length;a++)
																			{
																				File dummyFile = dummyFoldersList[a];
																				if(dummyFile.exists())
																				{
																					// 1A91-3U-15G_XML
																					if(dummyFile.isDirectory())
																					{
																						anyMaterialDirectoryExists = true;
																						/*
																						 * Start adding all the itemDetails to a VO 
																						 * and then to the List
																						 */
																						boolean isEngATMT=false;
																						ScheduleItemDetails schItemDetails = new ScheduleItemDetails();
																						schItemDetails.setSrNo(tempItemsList.size()+1);
																						schItemDetails.setMarket(marketFolder.getName());
																						schItemDetails.setLocale(localeFolder.getName());
																						
																						if(null!=allVINList && allVINList.size()>0)
																						{
																							VinDetails vCheck = null;
																							List<String> issueFiles = null;
																							boolean addToIssueList = true;
																							for(int are=0;are<allVINList.size();are++)
																							{
																								vCheck = (VinDetails)allVINList.get(are);
																								if(vCheck.isAllTokensExists()==false)
																								{
																									addToIssueList = true;
																									// add this VIN File Name to issueNames
																									if(null!=issueFiles && issueFiles.size()>0)
																									{
																										for(int you=0;you<issueFiles.size();you++)
																										{
																											if(String.valueOf(issueFiles.get(you)).trim().toLowerCase().equals(vCheck.getVinSourceFileName().trim().toLowerCase()))
																											{
																												// vin fileName already added
																												addToIssueList = false;
																												break;
																											}
																										}
																									}
																									
																									if(addToIssueList == true)
																									{
																										if(null==issueFiles || issueFiles.size()<=0)
																										{
																											issueFiles = new ArrayList<String>();
																										}
																										issueFiles.add(vCheck.getVinSourceFileName());
																									}
																								}
																								vCheck = null;
																							}
																							vCheck = null;
																							
																							if(null!=issueFiles && issueFiles.size()>0)
																							{
																								for(int you=0;you<issueFiles.size();you++)
																								{
																									if(null==schItemDetails.getVinFileNamesWithIssues())
																									{
																										schItemDetails.setVinFileNamesWithIssues("");
																									}
																									schItemDetails.setVinFileNamesWithIssues(schItemDetails.getVinFileNamesWithIssues()+String.valueOf(issueFiles.get(you)));
																									if(you!=issueFiles.size()-1)
																									{
																										schItemDetails.setVinFileNamesWithIssues(schItemDetails.getVinFileNamesWithIssues()+", ");
																									}
																								}
																							}
																							issueFiles = null;
																						}
																						
																						/*
																						 * Here MODEL FOLDER NAME WILL BE IN FOLLOWING FORMAT.
																						 * NewM_AT / NewM_MT / NewM_Eng
																						 * Isuzu_Titan_zw / OldM_rx-7_fd
																						 * 
																						 * <MODEL_TYPE>_<EITHER OVERHAUL / OR MODEL>_<CARLINE CODE, ONLY FOR MODELS>
																						 */

																						String modelFolderName = com.mazda.gms3.dmt.mc.utils.ConversionUtils.modelFolderForParsing(modelFolder.getName(), sessionBean.getMarketId());
																						if(null!=modelFolderName && !"".equals(modelFolderName))
																						{
																							String afterFirstIndex="";
																							if(modelFolderName.indexOf("_")!=-1)
																							{
																								// SET MODEL TYPE HERE
																								schItemDetails.setModelType(modelFolderName.substring(0, modelFolderName.indexOf("_")));
																								afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
																							}

																							// NOW IDENTIFY MODEL 
																							if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
																							{
																								if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
																										afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
																								{
																									// OVER HAUL MANUALS.
																									schItemDetails.setModel(afterFirstIndex);
																									// SET ENG AT MT FLAG TO TRUE - 25TH JULY 2019
																									isEngATMT=true;
																								}
																								else
																								{
																									// NORMAL MODEL -> cx-3_ab. Check for Last Index, because model could be Nissan_familia_van_bv
																									if(afterFirstIndex.lastIndexOf("_")!=-1)
																									{
																										schItemDetails.setModel(afterFirstIndex.substring(0, afterFirstIndex.lastIndexOf("_")));
																										schItemDetails.setCarlineCode(afterFirstIndex.substring(afterFirstIndex.lastIndexOf("_")+1, afterFirstIndex.length()));
																									}
																									else
																									{
																										// set all as Model Name
																										schItemDetails.setModel(afterFirstIndex);
																									}
																								}
																							}
																							afterFirstIndex = null;
																						}

																						schItemDetails.setModelFolderName(modelFolder.getName());
																						modelFolderName = null;
																						schItemDetails.setManualType(manualFolder.getName());
																						// getManualTypeName if not Engine and Mission as Model - Overhaul Manuals
																						if(null!=schItemDetails.getModel() && !"".equals(schItemDetails.getModel()) && 
																								null!=schItemDetails.getManualType() && !"".equals(schItemDetails.getManualType()))
																						{

																							// CALL FUNCTION TO SET MANUAL TYPE DETAILS
																							ManualTypeDetails mtDetails = new ManualTypeDetails();
																							if(schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																									schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																									schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																									schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																									schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																									schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
																									|| schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
																							{
																								// get on Model
																								/*
																								 * here check if Market is MC - THEN GO ON THE WAY IT IS
																								 * ELSE IF MME - THEN ALWAYS FETCH FOR - EN_UK
																								 */
																								if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
																								{
																									String loc = ApplicationProperties.getProperty("en-uk");
																									loc = loc.replace("_", "-");
																									mtDetails = ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getModel(), loc);
																									loc = null;
																								}
																								else
																								{
																									// MC Market
																									mtDetails = ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getModel(), schItemDetails.getLocale());
																								}
																							}
																							else
																							{
																								/*
																								 * here check if Market is MC - THEN GO ON THE WAY IT IS
																								 * ELSE IF MME - THEN ALWAYS FETCH FOR - EN_UK
																								 */
																								if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
																								{
																									String loc = ApplicationProperties.getProperty("en-uk");
																									loc = loc.replace("_", "-");
																									mtDetails= ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getManualType(), loc);
																									loc = null;
																								}
																								else
																								{
																									// MC Market
																									mtDetails= ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getManualType(), schItemDetails.getLocale());
																								}
																							}
																							if(null!=mtDetails && null!=mtDetails.getManualTypeName() && !"".equals(mtDetails.getManualTypeName()))
																							{
																								schItemDetails.setManualTypeLabel(mtDetails.getManualTypeName());
																							}
																							mtDetails = null;
																						}

																						// SET FACELIFT FOLDER NAME
																						schItemDetails.setFaceLiftFolderName(faceLiftFolder.getName());
																						// SET MATERIAL NAME
																						schItemDetails.setMaterialFolderName(dummyFile.getName());

																						/*
																						 *  set display order found & vin found flags
																						 *  25th JULY 2019
																						 */
																						schItemDetails.setDisplayOrderFileFound(displayOrderFileFound);
																						if(isEngATMT==true)
																						{
																							// explicitly set VIN Found to true - as VIN Data is to be read from MDM
																							schItemDetails.setVinFileFound(true);
																							/*
																							 * set allMaterialFolders found flag for the processing faceLift Folder
																							 * as TRUE because no VIN Data is to be read from MDM
																							 */
																							schItemDetails.setAllowProcessingMaterialFolderCheck(true);
																						}
																						else
																						{
																							schItemDetails.setVinFileFound(vinFileFound);
																							/*
																							 * set allMaterialFolders found flag for the processing faceLift Folder
																							 */
																							schItemDetails.setAllowProcessingMaterialFolderCheck(allMaterialFoldersExists);
																						}

																						logger.info("identifyFileItemsOperationForMC :: Proceed for Iterating Material Folder DIrectory :: > " + dummyFile.getName());
																						/*
																						 * Start Getting Material Folder Counts
																						 */
																						schItemDetails = readMaterialFolderFilesCountForMC(dummyFile, schItemDetails);

																						// COUNT EVERY FILE THEY MATTERS

																						// set processingDocsCount
																						if(null!=schItemDetails.getProcessingFileList() && schItemDetails.getProcessingFileList().size()>0)
																						{
																							schItemDetails.setTotalDocsForProcessing(new Long(schItemDetails.getProcessingFileList().size()).longValue());
																							/*
																							 * IF MANUAL TYPE IS BSM, THEN META DATA DOCS COUNT WILL BE SAME
																							 * AS TOTAL PROCESSING COUNT
																							 */
																							if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.bsm.label").trim().toLowerCase()))
																							{
																								schItemDetails.setMetaDocsCount(schItemDetails.getTotalDocsForProcessing());
																							}
																						}
																						// set okAssets Count
																						if(null!=schItemDetails.getOkAssetsFileList() && schItemDetails.getOkAssetsFileList().size()>0)
																						{
																							schItemDetails.setOkAssetsCount(new Long(schItemDetails.getOkAssetsFileList().size()).longValue());
																						}


																						String displayOrderFileNamesForMaterialFolder="";
																						// identifyDisplayOrderCount
																						if(null!=allDisplayOrderList && allDisplayOrderList.size()>0)
																						{
																							for(DisplayOrderDetails details : allDisplayOrderList)
																							{
																								if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																								{
																									if(details.getModelFolderName().trim().toLowerCase().equals(schItemDetails.getModelFolderName().trim().toLowerCase()) && 
																											details.getManualType().trim().toLowerCase().equals(schItemDetails.getManualType().trim().toLowerCase()) &&
																											details.getFaceLiftFolderName().trim().toLowerCase().equals(schItemDetails.getFaceLiftFolderName().trim().toLowerCase()) &&
																											details.getMaterialFolderName().trim().toLowerCase().equals(schItemDetails.getMaterialFolderName().trim().toLowerCase()))
																									{
																										schItemDetails.setTotalDisplayOrderCount(schItemDetails.getTotalDisplayOrderCount()+1);
																										if(isEngATMT==false)
																										{
																											// do this only when SM / WD Docs Processing
																											if(null!=details.getDisplayOrderSourceFileName() && !"".equals(details.getDisplayOrderSourceFileName()))
																											{
																												if(!displayOrderFileNamesForMaterialFolder.trim().toLowerCase().contains(details.getDisplayOrderSourceFileName().trim().toLowerCase()))
																												{
																													// ADD 
																													displayOrderFileNamesForMaterialFolder+=details.getDisplayOrderSourceFileName().trim().toLowerCase()+",";
																												}
																											}
																										}

																									}
																								}
																								details = null;
																							}
																						}

																						if(isEngATMT==false)
																						{
																							if(null!=displayOrderFileNamesForMaterialFolder && !"".equals(displayOrderFileNamesForMaterialFolder))
																							{
																								if(displayOrderFileNamesForMaterialFolder.endsWith(","))
																								{
																									displayOrderFileNamesForMaterialFolder = displayOrderFileNamesForMaterialFolder.substring(0, displayOrderFileNamesForMaterialFolder.length()-1);
																								}
																							}
																							boolean allRequiredDisplayOrderExists = true;
																							// identify Applicable VIN for this Material Folder and check all DisplayOrderFound
																							if(null!=allVINList && allVINList.size()>0)
																							{
																								VinDetails vd = null;
																								for(int z=0;z<allVINList.size();z++)
																								{
																									vd = (VinDetails)allVINList.get(z);
																									if(null!=vd.getLineType() && vd.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																									{
																										if(vd.getManualType().trim().toLowerCase().equals(schItemDetails.getManualType().trim().toLowerCase()) && 
																												vd.getFaceLiftFolder().trim().toLowerCase().equals(schItemDetails.getFaceLiftFolderName().trim().toLowerCase()) && 
																												vd.getMaterialFolder().trim().toLowerCase().equals(schItemDetails.getMaterialFolderName().trim().toLowerCase()))
																										{
																											// this VIN is for current Material Folder
																											if(null!=vd.getDisplayOrderTextFileName() && null!=displayOrderFileNamesForMaterialFolder)
																											{
																												if(!displayOrderFileNamesForMaterialFolder.trim().toLowerCase().contains(vd.getDisplayOrderTextFileName().toLowerCase().trim()))
																												{
																													allRequiredDisplayOrderExists = false;
																													break;
																												}
																											}
																										}
																									}
																									vd = null;
																								}
																							}
																							// set in schItemDetails
																							schItemDetails.setAllRequiredDisplayOrderExists(allRequiredDisplayOrderExists);
																						}
																						else
																						{
																							// case for ENG AT MT AS VIN.TXT IS NOT PROVIDED.
																							schItemDetails.setAllRequiredDisplayOrderExists(true);
																						}

																						displayOrderFileNamesForMaterialFolder = null;
																						// identifyCDProcessingCount
																						if(null!=allCDProcessingDetailsList && allCDProcessingDetailsList.size()>0)
																						{
																							for(CDProcessingDetails details : allCDProcessingDetailsList)
																							{
																								if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																								{
																									if(details.getModelFolderName().trim().toLowerCase().equals(schItemDetails.getModelFolderName().trim().toLowerCase()) && 
																											details.getManualType().trim().toLowerCase().equals(schItemDetails.getManualType().trim().toLowerCase()) &&
																											details.getFaceLiftFolderName().trim().toLowerCase().equals(schItemDetails.getFaceLiftFolderName().trim().toLowerCase()) &&
																											details.getMaterialFolderName().trim().toLowerCase().equals(schItemDetails.getMaterialFolderName().trim().toLowerCase()))
																									{
																										schItemDetails.setTotalCDProcessingCount(schItemDetails.getTotalCDProcessingCount()+1);
																									}
																								}
																								details = null;
																							}
																						}
																						
																						// identifySCMVINProcessingCount
																						if(null!=allSCMVinList && allSCMVinList.size()>0)
																						{
																							for(VinDetails details : allSCMVinList)
																							{
																								if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																								{
																									if(details.getManualType().trim().toLowerCase().equals(schItemDetails.getManualType().trim().toLowerCase()) &&
																											details.getFaceLiftFolder().trim().toLowerCase().equals(schItemDetails.getFaceLiftFolderName().trim().toLowerCase()) &&
																											details.getMaterialFolder().trim().toLowerCase().equals(schItemDetails.getMaterialFolderName().trim().toLowerCase()))
																									{
																										schItemDetails.setTotalSCMVinCount(schItemDetails.getTotalSCMVinCount()+1);
																									}
																								}
																								details = null;
																							}
																						}
																						/*
																						 * add schItemDetails to tempList
																						 */
																						tempItemsList.add(schItemDetails);
																						schItemDetails= null;
																					}
																				}
																				dummyFile = null;
																			}

																			if(anyMaterialDirectoryExists==false)
																			{
																				/*
																				 * NO MATERIAL FOLDER EXISTS FOR THE FACELIFT FOLDER
																				 * CHECK IF DISPLAY ORDER FILE & CD PROCESSING FILE EXISTS OR NOT
																				 * ADD CHECK FOR SCM VIN FILE AS WELL
																				 */
																				if((null!=allCDProcessingDetailsList && allCDProcessingDetailsList.size()>0) || 
																						((null!=allDisplayOrderList && allDisplayOrderList.size()>0)) || 
																						((null!=allSCMVinList && allSCMVinList.size()>0)))
																				{
																					/*
																					 * CREATE A ITEM DETAILS HERE AND ADD ONLY THE DISPLAY ORDER COUNT
																					 * AS WELL AS CD PROCESSING COUNT, ALL VALID LINES WILL BE COUNTED
																					 */
																					/*
																					 * Start adding all the itemDetails to a VO 
																					 * and then to the List
																					 */
																					boolean isEngATMT=false;
																					ScheduleItemDetails schItemDetails = new ScheduleItemDetails();
																					schItemDetails.setSrNo(tempItemsList.size()+1);
																					schItemDetails.setMarket(marketFolder.getName());
																					schItemDetails.setLocale(localeFolder.getName());
																					/*
																					 * set this Flag to true, as No Material Folder exists
																					 * for the processing FaceLift Folder
																					 */
																					schItemDetails.setAllowProcessingMaterialFolderCheck(true);

																					/*
																					 * Here MODEL FOLDER NAME WILL BE IN FOLLOWING FORMAT.
																					 * NewM_AT / NewM_MT / NewM_Eng
																					 * Isuzu_Titan_zw / OldM_rx-7_fd
																					 * 
																					 * <MODEL_TYPE>_<EITHER OVERHAUL / OR MODEL>_<CARLINE CODE, ONLY FOR MODELS>
																					 */

																					String modelFolderName = com.mazda.gms3.dmt.mc.utils.ConversionUtils.modelFolderForParsing(modelFolder.getName(), sessionBean.getMarketId());
																					if(null!=modelFolderName && !"".equals(modelFolderName))
																					{
																						String afterFirstIndex="";
																						if(modelFolderName.indexOf("_")!=-1)
																						{
																							// SET MODEL TYPE HERE
																							schItemDetails.setModelType(modelFolderName.substring(0, modelFolderName.indexOf("_")));
																							afterFirstIndex= modelFolderName.substring(modelFolderName.indexOf("_")+1, modelFolderName.length());
																						}

																						// NOW IDENTIFY MODEL 
																						if(null!=afterFirstIndex && !"".equals(afterFirstIndex))
																						{
																							if(afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase()) ||
																									afterFirstIndex.trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
																							{
																								// OVER HAUL MANUALS.
																								schItemDetails.setModel(afterFirstIndex);
																								// SET ENG AT MT FLAG TO TRUE - 25TH JULY 2019
																								isEngATMT=true;
																							}
																							else
																							{
																								// NORMAL MODEL -> cx-3_ab. Check for Last Index, because model could be Nissan_familia_van_bv
																								if(afterFirstIndex.lastIndexOf("_")!=-1)
																								{
																									schItemDetails.setModel(afterFirstIndex.substring(0, afterFirstIndex.lastIndexOf("_")));
																									schItemDetails.setCarlineCode(afterFirstIndex.substring(afterFirstIndex.lastIndexOf("_")+1, afterFirstIndex.length()));
																								}
																								else
																								{
																									// set all as Model Name
																									schItemDetails.setModel(afterFirstIndex);
																								}
																							}
																						}
																						afterFirstIndex = null;
																					}

																					schItemDetails.setModelFolderName(modelFolder.getName());
																					modelFolderName = null;
																					schItemDetails.setManualType(manualFolder.getName());
																					// getManualTypeName if not Engine and Mission as Model - Overhaul Manuals
																					if(null!=schItemDetails.getModel() && !"".equals(schItemDetails.getModel()) && 
																							null!=schItemDetails.getManualType() && !"".equals(schItemDetails.getManualType()))
																					{

																						// CALL FUNCTION TO SET MANUAL TYPE DETAILS
																						ManualTypeDetails mtDetails = new ManualTypeDetails();
																						if(schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.label").trim().toLowerCase()) || 
																								schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.engine.mc.label").trim().toLowerCase()) || 
																								schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.rq.label").trim().toLowerCase()) || 
																								schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.mcm.label").trim().toLowerCase()) || 
																								schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.dm.label").trim().toLowerCase()) || 
																								schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.auto.mission.label").trim().toLowerCase())
																								|| schItemDetails.getModel().trim().toLowerCase().equals(ApplicationProperties.getProperty("check.manual.mission.label").trim().toLowerCase()))
																						{
																							// get on Model
																							/*
																							 * here check if Market is MC - THEN GO ON THE WAY IT IS
																							 * ELSE IF MME - THEN ALWAYS FETCH FOR - EN_UK
																							 */
																							if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
																							{
																								String loc = ApplicationProperties.getProperty("en-uk");
																								loc = loc.replace("_", "-");
																								mtDetails = ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getModel(), loc);
																								loc= null;
																							}
																							else
																							{
																								// MC Market
																								mtDetails = ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getModel(), schItemDetails.getLocale());
																							}
																						}
																						else
																						{
																							/*
																							 * here check if Market is MC - THEN GO ON THE WAY IT IS
																							 * ELSE IF MME - THEN ALWAYS FETCH FOR - EN_UK
																							 */
																							if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
																							{
																								String loc = ApplicationProperties.getProperty("en-uk");
																								loc = loc.replace("_", "-");
																								mtDetails= ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getManualType(), loc);
																								loc = null;
																							}
																							else
																							{
																								// MC Market
																								mtDetails= ScheduleDAO.getManualTypeDetailsOnManualCode(schItemDetails.getManualType(), schItemDetails.getLocale());
																							}
																						}
																						if(null!=mtDetails &&  null!=mtDetails.getManualTypeName() && !"".equals(mtDetails.getManualTypeName()))
																						{
																							schItemDetails.setManualTypeLabel(mtDetails.getManualTypeName());
																						}
																						mtDetails = null;
																					}

																					// SET FACELIFT FOLDER NAME
																					schItemDetails.setFaceLiftFolderName(faceLiftFolder.getName());
																					/*
																					 *  set display order found & vin found flags
																					 *  25th JULY 2019
																					 */
																					schItemDetails.setDisplayOrderFileFound(displayOrderFileFound);
																					if(isEngATMT==true)
																					{
																						// explicitly set VIN Found to true - as VIN Data is to be read from MDM
																						schItemDetails.setVinFileFound(true);
																					}
																					else
																					{
																						schItemDetails.setVinFileFound(vinFileFound);
																					}


																					// set DisplayOrderFileCount
																					String displayOrderFileNamesForMaterialFolder="";
																					if(null!=allDisplayOrderList && allDisplayOrderList.size()>0)
																					{
																						for(DisplayOrderDetails details : allDisplayOrderList)
																						{
																							if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																							{
																								schItemDetails.setTotalDisplayOrderCount(schItemDetails.getTotalDisplayOrderCount()+1);
																								if(isEngATMT==false)
																								{
																									// do this only when SM / WD Docs Processing
																									if(null!=details.getDisplayOrderSourceFileName() && !"".equals(details.getDisplayOrderSourceFileName()))
																									{
																										if(!displayOrderFileNamesForMaterialFolder.trim().toLowerCase().contains(details.getDisplayOrderSourceFileName().trim().toLowerCase()))
																										{
																											// ADD 
																											displayOrderFileNamesForMaterialFolder+=details.getDisplayOrderSourceFileName().trim().toLowerCase()+",";
																										}
																									}
																								}
																							}
																							details = null;
																						}
																					}


																					if(isEngATMT==false)
																					{
																						if(null!=displayOrderFileNamesForMaterialFolder && !"".equals(displayOrderFileNamesForMaterialFolder))
																						{
																							if(displayOrderFileNamesForMaterialFolder.endsWith(","))
																							{
																								displayOrderFileNamesForMaterialFolder = displayOrderFileNamesForMaterialFolder.substring(0, displayOrderFileNamesForMaterialFolder.length()-1);
																							}
																						}

																						boolean allRequiredDisplayOrderExists = true;
																						// identify Applicable VIN for this Material Folder and check all DisplayOrderFound
																						if(null!=allVINList && allVINList.size()>0)
																						{
																							VinDetails vd = null;
																							for(int z=0;z<allVINList.size();z++)
																							{
																								vd = (VinDetails)allVINList.get(z);
																								if(null!=vd.getLineType() && vd.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																								{
																									// this VIN is for current Material Folder
																									if(null!=vd.getDisplayOrderTextFileName() && null!=displayOrderFileNamesForMaterialFolder)
																									{
																										if(!displayOrderFileNamesForMaterialFolder.trim().toLowerCase().contains(vd.getDisplayOrderTextFileName().toLowerCase().trim()))
																										{
																											allRequiredDisplayOrderExists = false;
																											break;
																										}
																									}
																								}
																								vd = null;
																							}
																						}

																						// set in schItemDetails
																						schItemDetails.setAllRequiredDisplayOrderExists(allRequiredDisplayOrderExists);
																					}
																					else
																					{
																						// case for ENG AT MT AS VIN.TXT IS NOT PROVIDED.
																						schItemDetails.setAllRequiredDisplayOrderExists(true);
																					}

																					// set CDProcessingFileCount
																					if(null!=allCDProcessingDetailsList && allCDProcessingDetailsList.size()>0)
																					{
																						for(CDProcessingDetails details : allCDProcessingDetailsList)
																						{
																							if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																							{
																								schItemDetails.setTotalCDProcessingCount(schItemDetails.getTotalCDProcessingCount()+1);
																							}
																							details = null;
																						}
																					}
																					
																					// set scmVIN File Count
																					if(null!=allSCMVinList && allSCMVinList.size()>0)
																					{
																						for(VinDetails details : allSCMVinList)
																						{
																							if(null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID))
																							{
																								schItemDetails.setTotalSCMVinCount(schItemDetails.getTotalSCMVinCount()+1);
																							}
																							details = null;
																						}
																					}
																					/*
																					 * add schItemDetails to tempList
																					 */
																					tempItemsList.add(schItemDetails);
																					schItemDetails = null;
																				}
																			}

																			allDisplayOrderList = null;
																			allCDProcessingDetailsList = null;
																			allSCMVinList = null;
																			uniqueMaterialFoldersListinVinFile=null;
																			allMaterialFoldersExists=false;
																			//																			virtualMaterialFoldersList = null;
																		}
																		dummyFoldersList = null;
																	}
																	faceLiftFolder =null;
																}
															}
															faceliftFoldersList = null;
														}
													}
													manualFolder = null;
												}
											}
											manualTypeFoldersList = null;
										}
										modelFolder = null;
									}
								}
								modelsFoldersList = null;
							}
							localeFolder = null;
						}
					}
					localesFoldersList =null;
				}
				marketFolder = null;
				marketDirPath = null;
				localesToProcess = null;
				modelsToProcess = null;
				manualTypesToProcess = null;
			}

			if(null!=tempItemsList && tempItemsList.size()>0)
			{
				sessionBean.setScheduleItemsList(new ArrayList<ScheduleItemDetails>());
				sessionBean.setScheduleItemsList(tempItemsList);
			}
			tempItemsList = null;

			/*
			 * set error message
			 */
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				sessionBean.setErrorMessage(errorMessage.toString());
			}
			errorMessage = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "identifyFileItemsOperationForMC()", e);
		}
	}


	private ScheduleItemDetails readMaterialFolderFilesCountForMC(File materialFolder, ScheduleItemDetails schItemDetails)
	{
		try
		{
			// 1A91-3U-15G_XML
			// Here Identify the last String after _ in the Material Folder Name
			if(materialFolder.exists() && materialFolder.isDirectory())
			{
				File[] childFilesList = materialFolder.listFiles();
				if(null!=childFilesList && childFilesList.length>0)
				{
					ArrayList<ESICategoryDetails> esiCategoryList = new ArrayList<ESICategoryDetails>();

					/*
					 * START IDENTIFIYING FILES, ONLY ESI CAT TEXT FILE
					 */
					for(int e=0;e<childFilesList.length;e++)
					{
						File childFile = childFilesList[e];

						if(childFile.exists() && childFile.isFile())
						{
							if(com.mazda.gms3.dmt.utils.MetadataFiles.isEsiCatFile(childFile.getName()))
							{
								/*
								 * HERE CHECK IF MARKET IS MC - THEN READ THE ESI CAT FILE AS PER EXISTING FORMAT
								 * CHECK IF MARKET IS MME AND PROCESSING MANUAL TYPE IS WIRING DIAGRAM
								 * 	THEN AN EXTRA INDEX WILL HAVE TO BE READ AT 4TH POSITION (STEERING TYPE) IN ESI CAT TEXT FILE.S
								 */
								boolean readMMEWDESICatText=false;
								if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
								{
									// MARKET IS MME - CHECK FOR MANUAL TYPE AS WD OR E-WD
									if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
											schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
									{
										readMMEWDESICatText = true;
									}
								}

								if(readMMEWDESICatText==false)
								{
									// MC MARKET (ALL CHANNELS) & MME MARKET (SM CHANNEL)
									esiCategoryList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readESICategoryTextFile(childFile);
								}
								else if(readMMEWDESICatText==true)
								{
									// read ESI CAT FILE FOR MME MARKET - CHANNEL > WIRING DIAGRAM
									esiCategoryList = com.mazda.gms3.dmt.mc.utils.ConversionUtils.readESICategoryTextFileForMME_WD(childFile);
								}
							}
						}
						childFile = null;
					}

					// SET ESI CAT COUNT
					if(null!=esiCategoryList && esiCategoryList.size()>0)
					{
						int c = esiCategoryList.size();
						schItemDetails.setEsiCatLeftMenuCount(new Long(c).longValue());
					}


					/*
					 * OEM CONTENT - MC MARKET, MODEL TYPE NEITHER NEW NOR OLD, MANUAL TYPE NOT A WIRING DIAGRAM
					 */
					boolean oemContent = false;
					java.util.Set<String> oemListedPaths = new java.util.HashSet<String>();
					if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase())
							&& null!=schItemDetails.getModelType() && !"".equals(schItemDetails.getModelType().trim())
							&& !schItemDetails.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new").trim().toLowerCase())
							&& !schItemDetails.getModelType().trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.old").trim().toLowerCase())
							&& !schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder"))
							&& !schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
					{
						oemContent = true;
						if(null!=esiCategoryList)
						{
							for(int r=0;r<esiCategoryList.size();r++)
							{
								ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
								if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID)
										&& null!=details.getFilePath() && !"".equals(details.getFilePath()))
								{
									oemListedPaths.add(details.getFilePath().trim().toLowerCase());
								}
							}
						}
					}

					/*
					 * START IDENTIFIYING FILES, ONLY FOLDERS
					 * DO NOTHING WITH ESICAT.TXT, VIN.TXT AND DISPLAYORDER.TXT FILES
					 */
					for(int e=0;e<childFilesList.length;e++)
					{
						File childFile = childFilesList[e];
						/*
						 * HERE COUNT ONLY THE FOLDERS, SKIP LEFT MENU TEXT, VIN ENT , VIN ATTRIBUTE ENT AND DELETE.TXT 
						 * GO FOR FOLDERS LIKE - HTML / PDF / ENT.DIR / IMG.DIR / DJVU / XML
						 * 
						 */
						if(childFile.exists() && childFile.isDirectory() && oemContent==true)
						{
							/*
							 * OEM CONTENT OF SM / OSM (MC MARKET) - EVERY FILE OF EVERY FOLDER IS COPIED TO OKASSETS AS IT IS,
							 * AND THE HTML FILES LYING DIRECTLY IN THE HTML / HTML5 FOLDER THAT THE ESI CATEGORY TEXT FILE
							 * LISTS ARE THE DOCUMENTS.
							 */
							boolean documentsFolder = childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html"))
									|| childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5"));
							schItemDetails = countOemFolder(childFile, schItemDetails, oemListedPaths, documentsFolder);
						}
						else if(childFile.exists() && childFile.isDirectory())
						{
							/*
							 * 
							 * Now, here processing Logic for child Folders, which are html/pdf/djvu/ent.gms3/img.gm3
							 * For ENT.GMS3 - COUNT ALL .ENT FILES AS DOCUMENTS
							 * IMG.GMS3 - COUNT ALL AS OKASSETS
							 * 
							 * DJVU - COUNT ALL DJVU FILES AS DOCUMENTS
							 * 
							 * IF PROCESSING WIRING DIAGRAM - 
							 * 	CHECK FOR PARENT FOLDER ENDING WITH - 
							 * 		IF HTML ONLY - 
							 * 			COUNT ALL THE FILES INSIDE HTML AS OKASSETS
							 * 			LOOK UP FOR ESI CAT. TXT - IF FOUND - THEN COUNT AS DOCUMENTS (ALL ENTRIES)
							 * 		ELSE IF PDF ONLY - 
							 * 			COUNT ALL THE PDF FILES AS DOCUMENTS
							 * 		ELSE IF HTML & PDF
							 * 			PDFS WILL BE COUNTED AS DOCUMENTS & OKASSETS
							 * 			HTMLS WILL BE COUNTED AS OKASSETS
							 */
							if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")))
							{
								boolean countPDFAsOkAssets = false;
								if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
										schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
								{
									String tok="";
									String mtName = materialFolder.getName();
									if(mtName.lastIndexOf("_")!=-1)
									{
										tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
									}
									if(null==tok)
									{
										tok="";
									}
									// DO NOT CHECK FOR NULL
									if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
									{
										countPDFAsOkAssets = true;
									}
									tok = null;
									mtName=null;
								}

								if(countPDFAsOkAssets==true)
								{
									// COUNT PDF AS OKASSETS AS WELL
									schItemDetails = countPDFFilesForOKAssets(childFile, schItemDetails);
								}

								// COUNT AS DOCUMENTS NO MATTER WD OR ANY OTHER MANUAL TYPE
								schItemDetails = countPDFFilesMC(childFile, schItemDetails, esiCategoryList);
							}
							else if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
							{
								schItemDetails = countDJVUFiles(childFile, schItemDetails, esiCategoryList);
							}
							else if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")))
							{
								if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
										schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
								{
									// WIRING DIAGRAM
									schItemDetails = countHTMLFilesMC(childFile, schItemDetails, esiCategoryList);
								}
								else
								{
									// SM OR OSM
									schItemDetails  =countHTMLFilesForOtherChannelsMC(childFile, schItemDetails, esiCategoryList);
								}
							}
							else if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5")))
							{
								// HTML5 WIRING DIAGRAMS
								schItemDetails = countHTML5FilesMC(childFile, schItemDetails, esiCategoryList);
							}
							else if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.ent.dir")))
							{
								schItemDetails = countENTFilesMC(childFile, schItemDetails, esiCategoryList);
							}
							else if(childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.img.dir")) || 
									childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.figures")) || 
									childFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.figure")))
							{
								/*
								 * call imagesCountOperation
								 */
								schItemDetails = imagesCountOperation(childFile, schItemDetails);
							}
						}
						childFile = null;
					}

					/*
					 * DOCUMENTS FOR DELETION (MC AND MME MARKETS) - there is no delete text file. A document the database
					 * still has as ACTIVE for this material folder and that the ESI category text file no longer
					 * lists is deleted by the job; the same identification is used here for the count.
					 */
					if(schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase())
							|| schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase())
							|| schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
					{
						int withdrawnCount = com.mazda.gms3.dmt.mc.utils.WithdrawnDocumentsFinder.find(esiCategoryList, schItemDetails.getLocale(), schItemDetails.getModelFolderName(),
								schItemDetails.getManualType(), schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), schItemDetails.getModelType(), schItemDetails.getMarket()).size();
						schItemDetails.setTotalDocsForDeletion(schItemDetails.getTotalDocsForDeletion() + withdrawnCount);
					}
					esiCategoryList = null;
				}
				childFilesList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "readMaterialFolderFilesCountForMC()", e);
		}
		return schItemDetails;
	}


	/**
	 * Function will identify whether the file has to be skipped or not
	 * @param fileName
	 * @return
	 */
	private boolean skipAttributesFile(String fileName)
	{
		/*
		 * METADATA / ATTRIBUTE FILES ONLY, BY WHOLE NAME + EXTENSION (MetadataFiles): d / cd / esicat / vin
		 * text files, delete.txt, leftmenu.txt, vin / vin_attribute ent files, window.js.
		 * WAS skip.items.pattern MATCHED ANYWHERE IN THE NAME - A DOCUMENT LIKE RM100000001GVIN.html WAS
		 * LEFT OUT OF THE COUNT ALTHOUGH THE CONVERSION PROCESSES IT.
		 */
		return com.mazda.gms3.dmt.utils.MetadataFiles.isMetadataFile(fileName);
	}



	private ScheduleItemDetails countPDFFilesMC(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList)
	{
		try
		{
			File[] pdfFilesList = parentFolder.listFiles();
			if(null!=pdfFilesList && pdfFilesList.length>0)
			{
				for(int i=0;i<pdfFilesList.length;i++)
				{
					File pdfFile = pdfFilesList[i];
					if(pdfFile.isFile())
					{
						if(pdfFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf").trim().toLowerCase()))
						{
							boolean proccedFurther = false;
							// identify File Path
							String filePath =PathUtil.winPath(pdfFile);

							/*
							 * THIS VARIABLE FOR COVERING 
							 * THE WD CHANNEL - (MATERIAL FOLDER HAVING BOTH HTML & PDF, 
							 * 	BUT ESI CAT HAVING VALUES FOR PFDS instead of HTML)
							 */
							String anotherFilePath="";

							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
							{
								if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
								{
									filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
								}

								/*
								 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
								 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
								 * THEN REMOVE EXTENSION FROM THE FILE PATH
								 */
								if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
										schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
								{
									String tok="";
									String mtName = schItemDetails.getMaterialFolderName();
									if(mtName.lastIndexOf("_")!=-1)
									{
										tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
									}
									if(null==tok)
									{
										tok="";
									}

									// DO NOT CHECK FOR NULL
									if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
									{
										// REMOVE EXTENSION
										if(filePath.lastIndexOf(".")!=-1)
										{
											filePath = filePath.substring(0, filePath.lastIndexOf("."));
										}
										// SET ORIGINAL PATH IN ANOTHER FILE PATH
										anotherFilePath = filePath;
										// ALSO REPLACE HERE \PDF\ TO \HTML\, BECAUSE ESI CAT ALWAYS CONTAINS ENTRIES FOR HTML ONLY
										filePath = filePath.replace("\\pdf\\", "\\html\\");
									}
									tok = null;
								}

								if(null!=filePath && !"".equals(filePath))
								{
									filePath= filePath.trim();
								}
								if(null!=anotherFilePath && !"".equals(anotherFilePath))
								{
									anotherFilePath = anotherFilePath.trim();
								}
								if(null!=esiCategoryList && esiCategoryList.size()>0)
								{
									for(int r=0;r<esiCategoryList.size();r++)
									{
										ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
										if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
												null!=details.getFilePath() && !"".equals(details.getFilePath()))
										{
											if((filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())) || 
													(null!=anotherFilePath && anotherFilePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())))
											{
												// PROCEED FOR DOCUMENT CREATION OF THE FILE
												proccedFurther = true;
												break;
											}
										}
										details= null;
									}
								}
							}




							if(proccedFurther==true)
							{
								// COUNT THE FILE
								schItemDetails = addToProcessingFileList(pdfFile, schItemDetails);

								/*
								 * DO NOT ADD COUNT OF PDFS WHICH WILL BE PROCESSED AS DOCUMENTS TO ESI CAT COUNT
								 * INCASE OF WD WITH HTML & PDF
								 * DATE = 20TH SEPT 2017 
								 *

								if(null!=schItemDetails.getManualType() && (schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
										schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder"))))
								{
									if(null!=schItemDetails.getMaterialFolderName())
									{
										String tok="";
										String mtName = schItemDetails.getMaterialFolderName();
										if(mtName.lastIndexOf("_")!=-1)
										{
											tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
										}
										if(null==tok)
										{
											tok="";
										}
										// DO NOT CHECK FOR NULL
										if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
												!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
										{
											// INCREMENT ESI CAT COUNT BY 1
											schItemDetails.setEsiCatLeftMenuCount(schItemDetails.getEsiCatLeftMenuCount()+1);
										}
										tok = null;
										mtName=null;
									}
								}
								 */
							}

							// INCREMDENT THE TOTAL FILES COUNT by 1
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);

							filePath = null;
							anotherFilePath = null;
						}
					}
					pdfFile= null;
				}
			}
			pdfFilesList=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countPDFFilesMC()", e);
		}
		return schItemDetails;
	}


	/**
	 * Function will count the DJVU Files for the Directory
	 * @param parentFolder
	 * @param schItemDetails
	 * @return
	 */
	private ScheduleItemDetails countDJVUFiles(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList)
	{
		try
		{
			File[] djvuFilesList = parentFolder.listFiles();
			if(null!=djvuFilesList && djvuFilesList.length>0)
			{
				String djvuFileName=null;
				String djvuAbsoluePath=null;
				File djvuFile = null;
				for(int i=0;i<djvuFilesList.length;i++)
				{
					djvuFile = djvuFilesList[i];
					/*
		    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
		    		 * DO THIS ONLY FOR MODEL 
		    		 */
					djvuFileName = Utilities.replaceJunkCharacterToFullbyte(djvuFile.getName(), schItemDetails.getModelFolderName(), schItemDetails.getManualType(), 
							schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), schItemDetails.getLocale());
					djvuAbsoluePath = Utilities.replaceJunkCharacterToFullbyte(PathUtil.winPath(djvuFile), schItemDetails.getModelFolderName(), schItemDetails.getManualType(), 
							schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), schItemDetails.getLocale());
					if(!skipAttributesFile(djvuFileName))
					{
						if(djvuFileName.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.djvu").trim().toLowerCase()))
						{
							/*
							 * CHECK THE ENTRY OF THE DJVU FILE IN ESI CATEGORY FILE, IF EXISTS - THEN COUNT FILE.
							 */
							boolean proccedFurther = false;
							// identify File Path
							String filePath =djvuAbsoluePath;
							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
							{
								if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
								{
									filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
								}

								filePath= filePath.trim();
								if(null!=esiCategoryList && esiCategoryList.size()>0)
								{
									for(int r=0;r<esiCategoryList.size();r++)
									{
										ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
										if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
												null!=details.getFilePath() && !"".equals(details.getFilePath()))
										{
											if(filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase()))
											{
												// PROCEED FOR DOCUMENT CREATION OF THE FILE
												proccedFurther = true;
												break;
											}
										}
										details= null;
									}
								}
							}
							if(proccedFurther==true)
							{
								// COUNT THE FILE
								schItemDetails = addToProcessingFileList(djvuFile, schItemDetails);
							}

							// INCREMDENT THE TOTAL FILES COUNT by 1
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);

							filePath = null;
						}
					}
					djvuFileName = null;
					djvuAbsoluePath = null;
					djvuFile = null;
				}
			}
			djvuFilesList=  null;

			/*
			 * call function to count all the files as OKAssets Files for the COMPLETE DJVU FOLDER
			 */
			schItemDetails = countHTMLFilesForOKAssets(parentFolder, schItemDetails);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countDJVUFiles()", e);
		}
		return schItemDetails;
	}


	private ScheduleItemDetails countENTFilesMC(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList)
	{
		try
		{
			File[] entFilesList = parentFolder.listFiles();
			if(null!=entFilesList && entFilesList.length>0)
			{
				for(int i=0;i<entFilesList.length;i++)
				{
					File entFile = entFilesList[i];
					if(!skipAttributesFile(entFile.getName()))
					{
						/**
						 * TO DO THIS FOR ENT 0R XML FILE
						 * A NEW XML FILE (RDF FILE INTRODUCED FOR MME MARKET)
						 */
						boolean countProcessingFile=false;
						boolean countOKAssetProcessing=false;
						if(entFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent").trim().toLowerCase()))
						{
							countProcessingFile=true;
						}

						if(entFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.xml").trim().toLowerCase()))
						{
							countProcessingFile=true;
							// COUNT THIS XML FILE AS OK ASSETS AS WELL
							countOKAssetProcessing = true;
						}

						if(entFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf").trim().toLowerCase()))
						{
							countOKAssetProcessing=true;
						}

						//						if(entFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent").trim().toLowerCase()))
						if(countProcessingFile==true)
						{
							/*
							 * CHECK THE ENTRY OF THE ENT FILE IN ESI CATEGORY FILE, IF EXISTS - THEN COUNT FILE.
							 */
							boolean proccedFurther = false;
							// identify File Path
							String filePath =PathUtil.winPath(entFile);
							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
							{
								if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
								{
									filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
								}

								filePath= filePath.trim();
								if(null!=esiCategoryList && esiCategoryList.size()>0)
								{
									for(int r=0;r<esiCategoryList.size();r++)
									{
										ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
										if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
												null!=details.getFilePath() && !"".equals(details.getFilePath()))
										{
											if(filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase()))
											{
												// PROCEED FOR DOCUMENT CREATION OF THE FILE
												proccedFurther = true;
												break;
											}
										}
										details= null;
									}
								}
							}
							if(proccedFurther==true)
							{
								// COUNT THE FILE
								schItemDetails = addToProcessingFileList(entFile, schItemDetails);
							}
							// INCREMDENT THE TOTAL FILES COUNT by 1
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);
							filePath = null;
						}


						//						else if(entFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf").trim().toLowerCase()))
						if(countOKAssetProcessing==true)
						{
							// add to okAssetsList
							schItemDetails.setSourceFileName(entFile.getName());
							schItemDetails.setSourceFilePath(PathUtil.winPath(entFile));
							if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
							{
								if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
								{
									schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
								}
							}
							if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
							{
								schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
							}
							schItemDetails.getOkAssetsFileList().add(schItemDetails);
						}
						entFile= null;
					}
				}
			}
			entFilesList=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countENTFiles()", e);
		}
		return schItemDetails;
	}

	/**
	 * Function will Count HTML Files for SM / OSM FOR MC MARKET
	 * @param parentFolder
	 * @param schItemDetails
	 * @return
	 */
	/**
	 * OEM content: every file of the folder (and of the folders below it) counts as an OKAssets
	 * file; a html file lying directly in a documents folder and listed in the ESI category
	 * text file also counts as a document.
	 */
	private ScheduleItemDetails countOemFolder(File folder, ScheduleItemDetails schItemDetails, java.util.Set<String> listedPaths, boolean documentsFolder)
	{
		try
		{
			File[] children = folder.listFiles();
			if(null!=children)
			{
				for(int i=0;i<children.length;i++)
				{
					File child = children[i];
					if(child.isDirectory())
					{
						schItemDetails = countOemFolder(child, schItemDetails, listedPaths, false);
					}
					else if(child.isFile() && !child.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
					{
						if(documentsFolder==true && child.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html").trim().toLowerCase()))
						{
							String filePath = PathUtil.winPath(child);
							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()) && filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
							{
								filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
							}
							if(listedPaths.contains(filePath.trim().toLowerCase()))
							{
								schItemDetails = addToProcessingFileList(child, schItemDetails);
							}
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);
						}

						// add to okAssetsList
						schItemDetails.setSourceFileName(child.getName());
						schItemDetails.setSourceFilePath(PathUtil.winPath(child));
						if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
						{
							schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
						}
						if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
						{
							schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
						}
						schItemDetails.getOkAssetsFileList().add(schItemDetails);
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countOemFolder()", e);
		}
		return schItemDetails;
	}

	private ScheduleItemDetails countHTMLFilesForOtherChannelsMC(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList) 
	{
		try
		{
			File[] htmlFilesList = parentFolder.listFiles();
			if(null!=htmlFilesList && htmlFilesList.length>0)
			{
				for(int i=0;i<htmlFilesList.length;i++)
				{
					File htmlFile = htmlFilesList[i];
					if(htmlFile.isFile())
					{
						if(!skipAttributesFile(htmlFile.getName()))
						{
							if(htmlFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html").trim().toLowerCase()))
							{
								/*
								 * CHECK THE ENTRY OF THE DJVU FILE IN ESI CATEGORY FILE, IF EXISTS - THEN COUNT FILE.
								 */
								boolean proccedFurther = false;
								// identify File Path
								String filePath =PathUtil.winPath(htmlFile);
								if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
								{
									if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
									{
										filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
									}

									filePath= filePath.trim();
									if(null!=esiCategoryList && esiCategoryList.size()>0)
									{
										for(int r=0;r<esiCategoryList.size();r++)
										{
											ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
											if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
													null!=details.getFilePath() && !"".equals(details.getFilePath()))
											{
												if(filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase()))
												{
													// PROCEED FOR DOCUMENT CREATION OF THE FILE
													proccedFurther = true;
													break;
												}
											}
											details= null;
										}
									}
								}
								if(proccedFurther==true)
								{
									// COUNT THE FILE
									schItemDetails = addToProcessingFileList(htmlFile, schItemDetails);
								}

								// INCREMDENT THE TOTAL FILES COUNT by 1
								schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);


								filePath = null;
							}
							else if(htmlFile.getName().trim().toLowerCase().endsWith("."+ApplicationProperties.getProperty("directory.css").trim().toLowerCase()))
							{
								// DO IT FOR MC & MME MARKET NO - 21 MAY 20201
								if(null!=schItemDetails.getMarket() && (schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()) || 
										schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()) || schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase())))
								{
									/*
									 * DO THIS ONLY FOR MC MARKET NOT MME - DATE 12 JUNE 2018
									 */
									// COUNT THIS CSS FILE AS OKASSETS COUNT - DATE 12 JUNE 2018
									// add to okAssetsList
									schItemDetails.setSourceFileName(htmlFile.getName());
									schItemDetails.setSourceFilePath(PathUtil.winPath(htmlFile));
									if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
									{
										if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
										{
											schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
										}
									}
									if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
									{
										schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
									}
									schItemDetails.getOkAssetsFileList().add(schItemDetails);
								}
							}
						}
						else
						{
							/*
							 * DO THIS ONLY FOR MC MARKET NOT MME - DATE 12 JUNE 2018
							 */
							// DO APPLY THE COUNT FOR CSS FILE HERE AS WELL, AS THE NAME MAY CONTAIN SKIP ATTRIBUTES E.G. left.css, so those files needs to be 
							// counted as well
							if(htmlFile.getName().trim().toLowerCase().endsWith("."+ApplicationProperties.getProperty("directory.css").trim().toLowerCase()))
							{
								if(null!=schItemDetails.getMarket() && schItemDetails.getMarket().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()))
								{
									// COUNT THIS CSS FILE AS OKASSETS COUNT - DATE 12 JUNE 2018
									// add to okAssetsList
									schItemDetails.setSourceFileName(htmlFile.getName());
									schItemDetails.setSourceFilePath(PathUtil.winPath(htmlFile));
									if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
									{
										if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
										{
											schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
										}
									}
									if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
									{
										schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
									}
									schItemDetails.getOkAssetsFileList().add(schItemDetails);
								}
							}
						}
					}
					else if(htmlFile.isDirectory())
					{
						if(htmlFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.image")) ||
								htmlFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.images")))
						{
							File[] imgFilesList = htmlFile.listFiles();
							if(null!=imgFilesList && imgFilesList.length>0)
							{
								for(int e=0;e<imgFilesList.length;e++)
								{
									File imgFile = imgFilesList[e];
									if(imgFile.exists() && imgFile.isFile())
									{
										if(!imgFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
										{
											// add to okAssetsList
											schItemDetails.setSourceFileName(imgFile.getName());
											schItemDetails.setSourceFilePath(PathUtil.winPath(imgFile));
											if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
											{
												if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
												{
													schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
												}
											}
											if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
											{
												schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
											}
											schItemDetails.getOkAssetsFileList().add(schItemDetails);
										}
									}
									imgFile= null;
								}
							}
							imgFilesList = null;
						}
					}
					htmlFile = null;
				}
			}
			htmlFilesList=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countHTMLFilesForOtherChannelsMC()", e);
		}
		return schItemDetails;
	}




	private ScheduleItemDetails countHTMLFilesMC(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList)
	{
		try
		{

			File[] htmlFilesList = parentFolder.listFiles();
			if(null!=htmlFilesList && htmlFilesList.length>0)
			{
				for(int i=0;i<htmlFilesList.length;i++)
				{
					File htmlFile = htmlFilesList[i];
					if(!skipAttributesFile(htmlFile.getName()))
					{
						if(htmlFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html").trim().toLowerCase()) || 
								htmlFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm").trim().toLowerCase()))
						{
							/*
							 * look for the entry of the HTML file in ESI CATEGORY TEXT FILE, IF FOUND THEN ONLY COUNT THE FILES.
							 */
							boolean proccedFurther = false;

							/*
							 * THIS VARIABLE FOR COVERING 
							 * THE WD CHANNEL - (MATERIAL FOLDER HAVING BOTH HTML & PDF, 
							 * 	BUT ESI CAT HAVING VALUES FOR PFDS instead of HTML)
							 */
							String anotherFilePath = "";
							// identify File Path
							String filePath =PathUtil.winPath(htmlFile);
							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
							{
								if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
								{
									filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
								}
								/*
								 * ALSO CHECK IF PROCESSING CHANNEL IS WIRING DIAGRAMS
								 * AND MATEIAL FOLDER DOESN'T ENDS WITH HTML/PDF/DJVU/XML
								 * THEN REMOVE EXTENSION FROM THE FILE PATH
								 */
								if(schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
										schItemDetails.getManualType().trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
								{
									String tok="";
									String mtName = schItemDetails.getMaterialFolderName();
									if(mtName.lastIndexOf("_")!=-1)
									{
										tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
									}
									if(null==tok)
									{
										tok="";
									}

									// DO NOT CHECK FOR NULL
									if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
											!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
									{
										// REMOVE EXTENSION
										if(filePath.lastIndexOf(".")!=-1)
										{
											filePath = filePath.substring(0, filePath.lastIndexOf("."));
										}
										// REPLACE HTML FOLDER BY PDF AND SET IN ANOTHER PATH TO CHECK
										anotherFilePath = filePath.replace("\\html\\", "\\pdf\\");
									}
									tok = null;
								}

								if(null!=filePath && !"".equals(filePath))
								{
									filePath= filePath.trim();
								}
								if(null!=anotherFilePath && !"".equals(anotherFilePath))
								{
									anotherFilePath = anotherFilePath.trim();
								}
								if(null!=esiCategoryList && esiCategoryList.size()>0)
								{
									for(int r=0;r<esiCategoryList.size();r++)
									{
										ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
										if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
												null!=details.getFilePath() && !"".equals(details.getFilePath()))
										{
											if((filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())) || 
													(null!=anotherFilePath && anotherFilePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())))
											{
												// PROCEED FOR DOCUMENT CREATION OF THE FILE
												proccedFurther = true;
												break;
											}
										}
										details= null;
									}
								}
							}
							if(proccedFurther==true)
							{
								// COUNT THE FILE
								schItemDetails = addToProcessingFileList(htmlFile, schItemDetails);
							}
							filePath = null;
							anotherFilePath = null;

							// INCREMDENT THE TOTAL FILES COUNT by 1
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);
						}
					}
					htmlFile=  null;
				}
			}
			htmlFilesList=  null;
			/*
			 * call function to count all the files as OKAssets Files for the COMPLETE HTML FOLDER
			 */
			schItemDetails = countHTMLFilesForOKAssets(parentFolder, schItemDetails);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countHTMLFilesMC()", e);
		}
		return schItemDetails;
	}

	private ScheduleItemDetails countHTML5FilesMC(File parentFolder, ScheduleItemDetails schItemDetails, ArrayList<ESICategoryDetails> esiCategoryList)
	{
		try
		{

			File[] htmlFilesList = parentFolder.listFiles();
			if(null!=htmlFilesList && htmlFilesList.length>0)
			{
				for(int i=0;i<htmlFilesList.length;i++)
				{
					File htmlFile = htmlFilesList[i];
					if(!skipAttributesFile(htmlFile.getName()))
					{
						if(htmlFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html").trim().toLowerCase()) || 
								htmlFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm").trim().toLowerCase()))
						{
							/*
							 * look for the entry of the HTML file in ESI CATEGORY TEXT FILE, IF FOUND THEN ONLY COUNT THE FILES.
							 */
							boolean proccedFurther = false;

							// identify File Path
							String filePath =PathUtil.winPath(htmlFile);
							if(null!=schItemDetails.getLocale() && !"".equals(schItemDetails.getLocale()))
							{
								if(filePath.lastIndexOf(schItemDetails.getLocale())!=-1)
								{
									filePath = filePath.substring(filePath.lastIndexOf(schItemDetails.getLocale()), filePath.length());
								}

								if(null!=filePath && !"".equals(filePath))
								{
									filePath= filePath.trim();
								}
								if(null!=esiCategoryList && esiCategoryList.size()>0)
								{
									for(int r=0;r<esiCategoryList.size();r++)
									{
										ESICategoryDetails details = (ESICategoryDetails)esiCategoryList.get(r);
										if(null!=details && null!=details.getLineType() && details.getLineType().equals(com.mazda.gms3.dmt.mc.utils.ConversionUtils.LINE_TYPE_VALID) && 
												null!=details.getFilePath() && !"".equals(details.getFilePath()))
										{
											if((filePath.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())))
											{
												// PROCEED FOR DOCUMENT CREATION OF THE FILE
												proccedFurther = true;
												break;
											}
										}
										details= null;
									}
								}
							}
							if(proccedFurther==true)
							{
								// COUNT THE FILE
								schItemDetails = addToProcessingFileList(htmlFile, schItemDetails);
							}
							filePath = null;

							// INCREMDENT THE TOTAL FILES COUNT by 1
							schItemDetails.setTotalFilesCount(schItemDetails.getTotalFilesCount()+1);
						}
					}
					htmlFile=  null;
				}
			}
			htmlFilesList=  null;
			/*
			 * call function to count all the files as OKAssets Files for the COMPLETE HTML5 FOLDER
			 */
			schItemDetails = countHTMLFilesForOKAssets(parentFolder, schItemDetails);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countHTML5FilesMC()", e);
		}
		return schItemDetails;
	}


	/**
	 * Function will count the PDF Files of CONN FOLDER AS OK ASSETS MOVEMENT
	 * @param parentFolder
	 * @param schItemDetails
	 * @return
	 */
	private ScheduleItemDetails countPDFFilesForOKAssets(File parentFolder, ScheduleItemDetails schItemDetails )
	{
		try
		{
			File[] processingFilesList = parentFolder.listFiles();
			if(null!=processingFilesList && processingFilesList.length>0)
			{
				for(int c=0;c<processingFilesList.length;c++)
				{
					File processingFile = processingFilesList[c];
					if(processingFile.exists() && processingFile.isFile())
					{
						if(!processingFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
						{
							// add to okAssetsList
							schItemDetails.setSourceFileName(processingFile.getName());
							schItemDetails.setSourceFilePath(PathUtil.winPath(processingFile));
							if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
							{
								if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
								{
									schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
								}
							}
							if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
							{
								schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
							}
							schItemDetails.getOkAssetsFileList().add(schItemDetails);
						}
					}
					else if(processingFile.exists() && processingFile.isDirectory())
					{
						// call recursive function
						schItemDetails = countPDFFilesForOKAssets(processingFile, schItemDetails);
					}
					processingFile = null;
				}
			}
			processingFilesList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countPDFFilesForOKAssets()", e);
		}
		return schItemDetails;
	}


	/**
	 * Function will count HTML Folder Files for OKAssets movement
	 * @param parentFolder
	 * @param schItemDetails
	 * @return
	 */
	private ScheduleItemDetails countHTMLFilesForOKAssets(File parentFolder, ScheduleItemDetails schItemDetails )
	{
		try
		{
			File[] processingFilesList = parentFolder.listFiles();
			if(null!=processingFilesList && processingFilesList.length>0)
			{
				for(int c=0;c<processingFilesList.length;c++)
				{
					File processingFile = processingFilesList[c];
					if(processingFile.exists() && processingFile.isFile())
					{
						if(!processingFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
						{
							// add to okAssetsList
							schItemDetails.setSourceFileName(processingFile.getName());
							schItemDetails.setSourceFilePath(PathUtil.winPath(processingFile));
							if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
							{
								if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
								{
									schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
								}
							}
							if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
							{
								schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
							}
							schItemDetails.getOkAssetsFileList().add(schItemDetails);
						}
					}
					else if(processingFile.exists() && processingFile.isDirectory())
					{
						// call recursive function
						schItemDetails = countHTMLFilesForOKAssets(processingFile, schItemDetails);
					}
					processingFile = null;
				}
			}
			processingFilesList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "countHTMLFilesForOKAssets()", e);
		}
		return schItemDetails;
	}

	/**
	 * FUNCTION WILL BE USED FOR COUNTING FILES IN IMAGES / CONN DIRECTORY FOLDER.
	 * @param parentFolder
	 * @param schItemDetails
	 * @return
	 */

	private ScheduleItemDetails imagesCountOperation(File parentFolder, ScheduleItemDetails schItemDetails )
	{
		try
		{
			File[] processingFilesList = parentFolder.listFiles();
			if(null!=processingFilesList && processingFilesList.length>0)
			{
				for(int c=0;c<processingFilesList.length;c++)
				{
					File processingFile = processingFilesList[c];
					if(processingFile.exists() && processingFile.isFile())
					{
						if(!processingFile.getName().trim().toLowerCase().equals(ApplicationProperties.getProperty("thumbs.db.file")))
						{
							// add to okAssetsList
							schItemDetails.setSourceFileName(processingFile.getName());
							schItemDetails.setSourceFilePath(PathUtil.winPath(processingFile));
							if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
							{
								if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
								{
									schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
								}
							}
							if(null==schItemDetails.getOkAssetsFileList() || schItemDetails.getOkAssetsFileList().size()<=0)
							{
								schItemDetails.setOkAssetsFileList(new ArrayList<ScheduleItemDetails>());
							}
							schItemDetails.getOkAssetsFileList().add(schItemDetails);

						}
					}
					processingFile = null;
				}
			}
			processingFilesList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "imagesCountOperation()", e);
		}
		return schItemDetails;
	}

	/**
	 * Function will add processingFile details to processingFileList
	 * @param processingFile
	 * @param schItemDetails
	 * @return
	 */
	private ScheduleItemDetails addToProcessingFileList(File processingFile, ScheduleItemDetails schItemDetails)
	{
		try
		{
			/*
			 * if file is .ent, then read the fileName attribute from entFile
			 */
			schItemDetails.setSourceFileName(processingFile.getName());
			schItemDetails.setSourceFilePath(PathUtil.winPath(processingFile));
			if(null!=schItemDetails.getSourceFileName() && !"".equals(schItemDetails.getSourceFileName()))
			{
				if(schItemDetails.getSourceFileName().lastIndexOf(".")!=-1)
				{
					schItemDetails.setSourceFileExtension(schItemDetails.getSourceFileName().substring(schItemDetails.getSourceFileName().lastIndexOf("."),schItemDetails.getSourceFileName().length()));
				}
			}
			if(processingFile.getName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent").trim().toLowerCase()))
			{
				/*
				 * READ ENT FILE NAME ATTRIBUTE
				 */
				String entContent=ConversionUtils.getStringFromXML(processingFile);
				if(null!=entContent && !"".equals(entContent))
				{
					String fileNameAttribute = EntParsing.getFileNameAttributeFromEnt(entContent);
					if(null!=fileNameAttribute && !"".equals(fileNameAttribute))
					{
						schItemDetails.setFileNameAttribute(fileNameAttribute);
					}
				}
				entContent= null;
			}
			if(null==schItemDetails.getProcessingFileList() || schItemDetails.getProcessingFileList().size()<=0)
			{
				schItemDetails.setProcessingFileList(new ArrayList<ScheduleItemDetails>());
			}
			schItemDetails.getProcessingFileList().add(schItemDetails);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "addToProcessingFileList()", e);
		}
		return schItemDetails;
	}

	/**
	 * Function will perform checkBox operation Select All / Unselect All
	 * @param request
	 * @param sessionBean
	 */
	private void checkBoxOperation(HttpServletRequest request, ScheduleBean sessionBean)
	{
		try
		{
			/*
			 * set selectAll flag to false in sessionBean
			 * iterate scheduleList and set all the rowSelected to false
			 */
			sessionBean.setSelectAll(false);
			if(null!=sessionBean.getScheduleItemsList() && sessionBean.getScheduleItemsList().size()>0)
			{
				/*
				 * iterate list and set rowSelectedFlag to true for all rows
				 */
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					schItemDetails.setRowSelected(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String selRows = sessionBean.getSelectedRows();
				String[] selRowsList = selRows.split(",");
				if(null!=selRowsList && selRowsList.length>0)
				{
					for(int i=0;i<selRowsList.length;i++)
					{
						String selSrNo=selRowsList[i];
						if(null!=sessionBean.getScheduleItemsList() && sessionBean.getScheduleItemsList().size()>0)
						{
							/*
							 * iterate list and set rowSelectedFlag to true for all rows
							 */
							for(int j=0;j<sessionBean.getScheduleItemsList().size();j++)
							{
								ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(j);
								if(selSrNo.equals(String.valueOf(schItemDetails.getSrNo())))
								{
									// set schItemDetails.rowSelected to true
									schItemDetails.setRowSelected(true);
									break;
								}
							}
						}
						selSrNo = null;
					}
				}
			}

			/*
			 * now again check here, if all the rows are selected
			 * then set selectAll to true
			 * else false
			 */
			sessionBean.setSelectAll(true);
			if(null!=sessionBean.getScheduleItemsList() && sessionBean.getScheduleItemsList().size()>0)
			{
				/*
				 * iterate list and set rowSelectedFlag to true for all rows
				 */
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==false)
					{
						sessionBean.setSelectAll(false);
						break;
					}
				}
			}
			else
			{
				// now rows found.
				sessionBean.setSelectAll(false);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "checkBoxOperation()", e);
		}
	}

	private void checkBoxATOperation(HttpServletRequest request, ScheduleBean sessionBean)
	{
		try
		{
			/*
			 * set selectAll flag to false in sessionBean
			 * iterate scheduleList and set all the rowSelected to false
			 */
			sessionBean.setSelectATAll(false);
			if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
			{
				/*
				 * iterate list and set rowSelectedFlag to true for all rows
				 */
				for(int i=0;i<sessionBean.getAutomationItemsList().size();i++)
				{
					ItemDetails schItemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(i);
					schItemDetails.setRowSelected(false);
				}
			}
			if(null!=sessionBean.getSelectedATRows() && !"".equals(sessionBean.getSelectedATRows()))
			{
				String selRows = sessionBean.getSelectedATRows();
				String[] selRowsList = selRows.split(",");
				if(null!=selRowsList && selRowsList.length>0)
				{
					for(int i=0;i<selRowsList.length;i++)
					{
						String selSrNo=selRowsList[i];
						if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
						{
							/*
							 * iterate list and set rowSelectedFlag to true for all rows
							 */
							for(int j=0;j<sessionBean.getAutomationItemsList().size();j++)
							{
								ItemDetails schItemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(j);
								if(selSrNo.equals(String.valueOf(schItemDetails.getSrNo())))
								{
									// set schItemDetails.rowSelected to true
									schItemDetails.setRowSelected(true);
									break;
								}
							}
						}
						selSrNo = null;
					}
				}
			}

			/*
			 * now again check here, if all the rows are selected
			 * then set selectAll to true
			 * else false
			 */
			sessionBean.setSelectATAll(true);
			if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
			{
				/*
				 * iterate list and set rowSelectedFlag to true for all rows
				 */
				for(int i=0;i<sessionBean.getAutomationItemsList().size();i++)
				{
					ItemDetails schItemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(i);
					if(schItemDetails.isRowSelected()==false)
					{
						sessionBean.setSelectATAll(false);
						break;
					}
				}
			}
			else
			{
				// now rows found.
				sessionBean.setSelectATAll(false);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "checkBoxATOperation()", e);
		}
	}



	/**
	 * Function will validate the mandatory fields for scheduling conversion
	 * @param sessionBean
	 * @return
	 */
	private boolean validateConversion(ScheduleBean sessionBean)
	{
		if(null==sessionBean.getScheduleItemsList() || sessionBean.getScheduleItemsList().size()<=0)
		{
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.select.one.item.conversion"));
			return false;
		}

		if(null!=sessionBean.getScheduleItemsList() && sessionBean.getScheduleItemsList().size()>0)
		{
			boolean anyRowSelected = false;
			for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
			{
				ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
				if(schItemDetails.isRowSelected()==true)
				{
					anyRowSelected = true;
				}
			}

			if(anyRowSelected==false)
			{
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.select.one.item.conversion"));
				return false;
			}

			/*
			 * VERIFY FOR THE SELECTED ITEMS Each ROW
			 * IF DISPLAY ORDER FOUND IS TRUE BUT VIN FOUND IS FALSE - DO NOT ALLOW
			 * 25TH JULY 2019
			 * PERFORM THE BELOW VALIDATIONS ONLY FOR MC & MME MARKET
			 */
			if(sessionBean.getMarketId().trim().toLowerCase().equals
					(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()) || sessionBean.getMarketId().trim().toLowerCase().equals
					(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()) || sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
			{
				String rowNos="";
				// CHECK FOR NO DISPLAY ORDER & NO VIN - DO NOT ALLOW
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						// check this only when docs count =0, so that alone CD Processing / OKAssets / Delete can be done
						if(schItemDetails.getTotalDocsForProcessing() > 0)
						{
							if(schItemDetails.isDisplayOrderFileFound()==false && schItemDetails.isVinFileFound()==false)
							{
								// add this row NO
								rowNos+=String.valueOf(schItemDetails.getSrNo())+" ,";
							}
						}
					}
				}

				if(null!=rowNos && !"".equals(rowNos))
				{
					if(rowNos.endsWith(","))
					{
						rowNos = rowNos.substring(0, rowNos.length()-1);
					}
					// add ERROR MESSAGE
					sessionBean.setErrorMessage("Row No. "+rowNos.trim()+" cannot be selected for Content Load, as they do not contain any Display Order & VIN Text File.");
					return false;
				}
				
				
				// CHECK FOR VIN FILES, IF ANY WITH ISSUES FOUND
				String errors="";
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						if(null!=schItemDetails.getVinFileNamesWithIssues() && !"".equals(schItemDetails.getVinFileNamesWithIssues()))
						{
							// add this row NO
							errors+="Row No. "+String.valueOf(schItemDetails.getSrNo())+" cannot be selected for Content Load, VIN File(s) - "+schItemDetails.getVinFileNamesWithIssues()+" does not have proper VINs defined.";
							errors+="<MSG_TOKEN>";
						}
					}
					schItemDetails = null;
				}
				
				if(null!=errors && !"".equals(errors))
				{
					// add ERROR MESSAGE
					sessionBean.setErrorMessage(errors);
					errors = null;
					return false;
				}
				
				
				// CHECK FOR DISPLAY ORDER FOUND & VIN NOT FOUND
				rowNos = "";
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						if(schItemDetails.isDisplayOrderFileFound()==true)
						{
							if(schItemDetails.isVinFileFound()==false)
							{
								// add this row NO
								rowNos+=String.valueOf(schItemDetails.getSrNo())+" ,";
							}
						}
					}
				}

				if(null!=rowNos && !"".equals(rowNos))
				{
					if(rowNos.endsWith(","))
					{
						rowNos = rowNos.substring(0, rowNos.length()-1);
					}
					// add ERROR MESSAGE
					sessionBean.setErrorMessage("Row No. "+rowNos.trim()+" cannot be selected for Content Load, as they contain only Display Order Text File and No VIN Text File.");
					return false;
				}

				// CHECK FOR DISPLAY ORDER NOT FOUND & VIN FOUND
				rowNos = "";
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						if(schItemDetails.isDisplayOrderFileFound()==false)
						{
							// ONLY WHEN DOCS FOUND - BECAUSE ONLY VIN FILE IS NOT PERFORMING ANY PROCESSING
							if(schItemDetails.getTotalDocsForProcessing()>0 && schItemDetails.isVinFileFound()==true)
							{
								// add this row NO
								rowNos+=String.valueOf(schItemDetails.getSrNo())+" ,";
							}
						}
					}
				}

				if(null!=rowNos && !"".equals(rowNos))
				{
					if(rowNos.endsWith(","))
					{
						rowNos = rowNos.substring(0, rowNos.length()-1);
					}
					// add ERROR MESSAGE
					sessionBean.setErrorMessage("Row No. "+rowNos.trim()+" cannot be selected for Content Load, as they contain only VIN Text File and No Display Order Text File.");
					return false;
				}

				// CHECK FOR DISPLAY ORDER FOUND, VIN FOUND BUT ALL REQUIRED DISPLAY ORDER DOES NOT EXIST
				rowNos = "";
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						if(schItemDetails.isDisplayOrderFileFound()==true && schItemDetails.isVinFileFound()==true)
						{
							if(schItemDetails.isAllRequiredDisplayOrderExists()==false)
							{
								// add this row NO
								rowNos+=String.valueOf(schItemDetails.getSrNo())+" ,";
							}
						}
					}
				}

				if(null!=rowNos && !"".equals(rowNos))
				{
					if(rowNos.endsWith(","))
					{
						rowNos = rowNos.substring(0, rowNos.length()-1);
					}
					// add ERROR MESSAGE
					sessionBean.setErrorMessage("Row No. "+rowNos.trim()+" cannot be selected for Content Load, as the required Text file for Display Order provided in VIN.Txt is missing.");
					return false;
				}

				// CHECK FOR ALL MATERIAL FOLDER IDENTIFIED FROM VIN EXISTS PHYSICALLY
				rowNos = "";
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						// ONLY WHEN VIN FILE FOUND
						if(schItemDetails.isVinFileFound()==true && schItemDetails.isAllowProcessingMaterialFolderCheck()==false)
						{
							// add this row NO
							rowNos+=String.valueOf(schItemDetails.getSrNo())+" ,";
						}
					}
				}

				if(null!=rowNos && !"".equals(rowNos))
				{
					if(rowNos.endsWith(","))
					{
						rowNos = rowNos.substring(0, rowNos.length()-1);
					}
					// add ERROR MESSAGE
					sessionBean.setErrorMessage("Row No. "+rowNos.trim()+" cannot be selected for Content Load, as all the Material Folder identified from the VIN Text file(s) does not exist under Parent FaceLift folder.");
					return false;
				}
				rowNos = null;
			}

			long totalDocsCount=0;
			long totalDocsDeleteCount=0;
			long okAssetsCount=0;
			long okAssetsDeleteCount=0;
			long totalDisplayOrderCount=0;
			long totalCDProcessingCount=0;
			long totalSCMCount=0;
			for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
			{
				ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
				if(schItemDetails.isRowSelected()==true)
				{
					totalDocsCount= totalDocsCount+schItemDetails.getTotalDocsForProcessing();
					totalDocsDeleteCount = totalDocsDeleteCount+schItemDetails.getTotalDocsForDeletion();
					okAssetsCount = okAssetsCount + schItemDetails.getOkAssetsCount();
					okAssetsDeleteCount = okAssetsDeleteCount+ schItemDetails.getOkAssetsDeleteCount();
					totalDisplayOrderCount=  totalDisplayOrderCount+schItemDetails.getTotalDisplayOrderCount();
					totalCDProcessingCount = totalCDProcessingCount+schItemDetails.getTotalCDProcessingCount();
					totalSCMCount = totalSCMCount+ schItemDetails.getTotalSCMVinCount();
				}
			}

			if(totalDocsCount==0 && totalDocsDeleteCount==0 && okAssetsCount==0 && okAssetsDeleteCount==0 && totalDisplayOrderCount==0 && totalCDProcessingCount ==0 && totalSCMCount==0)
			{
				if(sessionBean.getMarketId().trim().toLowerCase().equals("mc") || sessionBean.getMarketId().trim().toLowerCase().equals("mme"))
				{
					sessionBean.setErrorMessage(msgProps().getProperty("error.message.no.docs.found.mc.conversion"));
					return false;
				}
				else if(sessionBean.getMarketId().trim().toLowerCase().equals("mnao"))
				{
					sessionBean.setErrorMessage(msgProps().getProperty("error.message.no.docs.found.conversion"));
					return false;
				}
			}
		
		}

		/*
		 * JOB RULES (2026-10-16, JobRules): conversions run in parallel - refused only while an IPM Facelift VIN Update /
		 * Document Identification / Master Data Syncing job is Pending or Processing, or when the parallel jobs cap is
		 * reached. The items of the selected rows are checked against the running conversion / publish jobs later.
		 */
		try 
		{
			com.mazda.gms3.dmt.dao.JobRules.Refusal refusal = com.mazda.gms3.dmt.dao.JobRules.contentRefusal(com.mazda.gms3.dmt.dao.JobRules.active());
			if(null!=refusal)
			{
				sessionBean.setErrorMessage(msgProps().getMessage(refusal.args, refusal.key));
				return false;
			}
		}
		catch (Exception e) 
		{
			// the rules cannot be checked - nothing is scheduled
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "validateConversion", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
			return false;
		}
		return true;
	}

	/**
	 * Function will schedule a conversion , start the details in DB and start documents Processing
	 * @param response
	 * @param sessionBean
	 */
	private void scheduleConversion(HttpServletResponse response, ScheduleBean sessionBean)
	{
		try
		{
			if(validateConversion(sessionBean))
			{
				// IDENTIFY IF ANY MASTER DATA LEFT FOR SYNCHING
				MasterDataSyncDAO masterDataDAO = new MasterDataSyncDAO();
				AutoSyncScheduleItemDetails details = null;
				String[] selCats = AutoSyncConstants.ITEMS_KEYS.split(",");
				// ONLY THE LOCALES OF THIS CONVERSION: "Master Data with Content" syncs the rows of its own locale, so
				// rows still waiting in another locale of the market are no reason to refuse a content load here
				ArrayList<String> conversionLocales = new ArrayList<String>();
				if(null!=sessionBean.getSelectedLocales())
				{
					for(String l : sessionBean.getSelectedLocales())
					{
						if(null!=l && !"".equals(l.trim()) && !l.trim().equalsIgnoreCase(ApplicationProperties.getProperty("check.all.label").trim()))
						{
							conversionLocales.add(l.trim());
						}
					}
				}
				// ... and their companion locales (en-US -> en-CA): "Master Data with Content" syncs those too
				conversionLocales = new ArrayList<String>(com.mazda.gms3.dmt.masterdata.MasterDataLoad.withCompanions(conversionLocales));
				ArrayList<AutoSyncScheduleItemDetails> itemsList = masterDataDAO.getItemsCountForProcessing(selCats, sessionBean.getMarketId().trim(),
						conversionLocales.isEmpty() ? null : conversionLocales.toArray(new String[0]));
				int totalCount = 0;
				if(null!=itemsList && itemsList.size()>0)
				{
					for(int a=0;a<itemsList.size();a++)
					{
						details = (AutoSyncScheduleItemDetails)itemsList.get(a);
						if(details.getTotalCount()>0)
						{
							totalCount = totalCount+details.getTotalCount();
						}
						details = null;
					}
				}
				details = null;
				masterDataDAO =  null;
				itemsList =null;
				selCats  = null;

				/*
				 * "MASTER DATA WITH CONTENT" (MC AND MME MARKETS) - THE JOB ITSELF LOADS THE MASTER DATA AND SENDS IT TO
				 * KAPTURE BEFORE THE CONTENT, SO MASTER DATA STILL WAITING IS NOT A REASON TO REFUSE THE SCHEDULE.
				 */
				boolean masterDataWithContent = ScheduleDAO.LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(sessionBean.getLoadTypeId())
						&& (sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase())
								|| sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase())
								|| sessionBean.getMarketId().trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()));
				if(totalCount>0 && masterDataWithContent==false)
				{
					String[] id = new String[] { String.valueOf(totalCount), sessionBean.getMarketId(), conversionLocales.isEmpty() ? ApplicationProperties.getProperty("check.all.label").trim() : String.join(", ", conversionLocales) };
					sessionBean.setErrorMessage(msgProps().getMessage(id, "error.message.masterdata.remains.for.syncing"));
					id = null;
				}
				else
				{
					if(sessionBean.getMarketId().trim().toLowerCase().equals
							(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
					{
						/*
						 * WHEN MNAO MARKET
						 */
						scheduleMNAOOperation(response, sessionBean);
					}
					else if(sessionBean.getMarketId().trim().toLowerCase().equals
							(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()))
					{
						/*
						 * WHEN MC MARKET
						 */
						scheduleMCOperation(response, sessionBean);
					}
					else if(sessionBean.getMarketId().trim().toLowerCase().equals
							(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
					{
						/*
						 * WHEN MME MARKET
						 */
						scheduleMMEOperation(response, sessionBean);
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "scheduleConversion()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}

	private void scheduleMNAOOperation(HttpServletResponse response, ScheduleBean sessionBean)
	{
		try
		{

			/*
			 * CHECK HERE, 
			 * IF NO OF PENDING / PROCESSING JOBS ARE 5, DO NOT ALLOW TO SCHEDULE A JOB 
			 * 
			 * 
			 * IF FOR ANY OF THE SELECTED ITEMS
			 * MARKET
			 * LOCALE
			 * MODEL
			 * MANUAL TYPE
			 * MATERIAL NAME 
			 * IS IN PROCESSING STATE - STOP DO NOT ALLOW USER TO MOVE FORWARD
			 */
			// JOB COUNT CHECK.
			//			int pendingProcessingJobsCount= ScheduleDAO.getPendingProcessingJobsCount();
			//			if(pendingProcessingJobsCount<5)
			{
				/*
				 * A SELECTED ITEM (LOCALE + MODEL FOLDER + MANUAL TYPE + FACELIFT FOLDER + MATERIAL FOLDER) THAT A PENDING / RUNNING
				 * CONVERSION OR PUBLISH CONTENT JOB (ANY MARKET) IS PROCESSING CANNOT BE SCHEDULED UNTIL THAT JOB FINISHES (JobRules)
				 */
				boolean proceedFurther= true;
				StringBuilder errorMessage = new StringBuilder();
				java.util.List<com.mazda.gms3.dmt.dao.JobRules.Item> runningItems = com.mazda.gms3.dmt.dao.JobRules.runningContentItems();
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						com.mazda.gms3.dmt.dao.JobRules.Item busy = com.mazda.gms3.dmt.dao.JobRules.busy(runningItems, new com.mazda.gms3.dmt.dao.JobRules.Item(
								schItemDetails.getLocale(), schItemDetails.getModelFolderName(), schItemDetails.getManualType(),
								schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), null));
						if(null!=busy)
						{
							// Row No. {0} is already Processing under - {1}
							errorMessage.append(msgProps().getMessage(new String[] { String.valueOf(schItemDetails.getSrNo()), busy.scheduleName },
									"error.message.item.already.processing"));
							errorMessage.append("\n");
							proceedFurther=false;
						}
					}
				}
				runningItems = null;

				if(proceedFurther==false)
				{
					StringBuilder msgBuilder = new StringBuilder();
					// cannot proceed further
					String message = msgProps().getProperty("label.cannot.proceed.further");
					msgBuilder.append(message);
					msgBuilder.append("\n");
					if(null!=errorMessage)
					{
						msgBuilder.append(errorMessage.toString());
					}
					sessionBean.setErrorMessage(msgBuilder.toString());
					message=  null;
					msgBuilder=  null;
				}
				else
				{
					/*
					 * Prepare Data for the Storing the data in Schedule & Criteria Table
					 */
					ScheduleDetails schDetails = new ScheduleDetails();
					schDetails.setScheduleName(ApplicationProperties.getProperty("schedule.name.key")+DateFormatter.parseDateToString_YYYY_MM_DD(new Date())+"_");
					schDetails.setThreadId(ApplicationProperties.getProperty("schedule.name.key"));
					schDetails.setUserId(wslId());
					schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
					// a Kapture load is always Draft - the Publish Content job publishes (as MC)
					schDetails.setImDocsProcessingStatus(ApplicationProperties.getProperty("flag.value.draft"));
					long totalDocsCount=0;
					long totalDocsDeleteCount=0;
					long okAssetsCount=0;
					long okAssetsDeleteCount=0;
					long totalMetaDataDocsCount=0;
					long totalDisplayOrderCount=0;
					long totalCDProcessingCount=0;
					long totalSCMCount=0;
					ArrayList<ScheduleItemDetails> itemsList = new ArrayList<ScheduleItemDetails>();
					for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
					{
						ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
						if(schItemDetails.isRowSelected()==true)
						{
							totalDocsCount= totalDocsCount+schItemDetails.getTotalDocsForProcessing();
							totalDocsDeleteCount = totalDocsDeleteCount+schItemDetails.getTotalDocsForDeletion();
							okAssetsCount = okAssetsCount + schItemDetails.getOkAssetsCount();
							okAssetsDeleteCount = okAssetsDeleteCount+ schItemDetails.getOkAssetsDeleteCount();
							totalMetaDataDocsCount = totalMetaDataDocsCount + schItemDetails.getMetaDocsCount();
							totalDisplayOrderCount=  totalDisplayOrderCount+ schItemDetails.getTotalDisplayOrderCount();
							totalCDProcessingCount  = totalCDProcessingCount+ schItemDetails.getTotalCDProcessingCount();
							totalSCMCount = totalSCMCount + schItemDetails.getTotalSCMVinCount();
							itemsList.add(schItemDetails);
						}
					}
					schDetails.setTotalDocsForProcessing(totalDocsCount);
					schDetails.setTotalDocsForDeletion(totalDocsDeleteCount);
					schDetails.setOkAssetsCount(okAssetsCount);
					schDetails.setOkAssetsDeleteCount(okAssetsDeleteCount);
					schDetails.setTotalMetaDataDocsCount(totalMetaDataDocsCount);
					schDetails.setTotalCDProcessingCount(totalCDProcessingCount);
					schDetails.setTotalDisplayOrderCount(totalDisplayOrderCount);
					schDetails.setTotalSCMVinCount(totalSCMCount);

					schDetails.setItemsList(new ArrayList<ScheduleItemDetails>());
					if(null!=itemsList && itemsList.size()>0)
					{
						schDetails.setItemsList(itemsList);
					}
					/*
					 * call function to store data
					 */

					Long scheduleId= ScheduleDAO.createSchedule(schDetails);
					if(null!=scheduleId && scheduleId>0)
					{
						ScheduleDAO.saveScheduleLoadType(scheduleId, sessionBean.getLoadTypeId());
						sessionBean.setScheduleName(schDetails.getScheduleName());
						String message = msgProps().addMessage("conversion.schedule.success", schDetails.getScheduleName());
						sessionBean.setSuccessMessage(message);
						message= null;
						/*
						 * Update Thread and start with customName
						 */
						String threadId = schDetails.getThreadId();
						final String code = String
								.valueOf(scheduleId);
						final String jobUser = wslId();
						final StartConversionMNAOImpl startConvImpl = new StartConversionMNAOImpl();
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {

								synchronized (startConvImpl) {
									try {
										startConvImpl.startConversion(String.valueOf(code), jobUser);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(Schedule.class.getName(), "run()", e);
									}
								}
							}
						};

						Thread th = new Thread(runn, threadId);
						th.start();

						threadId = null;

					}
					else
					{
						sessionBean.setErrorMessage(msgProps().getProperty("error.message.failure.schedule.conversion"));
					}
					scheduleId = null;
					schDetails = null;
					itemsList = null;
				}
			}
			//			else
			//			{
			//				// Maximum 5 conversion jobs can be scheduled in parallel. Please wait till the Processing jobs gets finished.
			//				sessionBean.setErrorMessage(msgProps().getProperty("error.message.max.jobs.scheduled"));
			//			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "scheduleMNAOOperation()", e);
		}
	}

	private void scheduleMCOperation(HttpServletResponse response, ScheduleBean sessionBean)
	{
		try
		{

			/*
			 * CHECK HERE, 
			 * IF NO OF PENDING / PROCESSING JOBS ARE 5, DO NOT ALLOW TO SCHEDULE A JOB 
			 * 
			 * 
			 * IF FOR ANY OF THE SELECTED ITEMS
			 * MARKET
			 * LOCALE
			 * MODEL
			 * MANUAL TYPE
			 * MATERIAL NAME 
			 * IS IN PROCESSING STATE - STOP DO NOT ALLOW USER TO MOVE FORWARD
			 */
			// JOB COUNT CHECK.
			//			int pendingProcessingJobsCount= ScheduleDAO.getPendingProcessingJobsCount();
			//			if(pendingProcessingJobsCount<5)
			{
				/*
				 * A SELECTED ITEM (LOCALE + MODEL FOLDER + MANUAL TYPE + FACELIFT FOLDER + MATERIAL FOLDER) THAT A PENDING / RUNNING
				 * CONVERSION OR PUBLISH CONTENT JOB (ANY MARKET) IS PROCESSING CANNOT BE SCHEDULED UNTIL THAT JOB FINISHES (JobRules)
				 */
				boolean proceedFurther= true;
				StringBuilder errorMessage = new StringBuilder();
				java.util.List<com.mazda.gms3.dmt.dao.JobRules.Item> runningItems = com.mazda.gms3.dmt.dao.JobRules.runningContentItems();
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						com.mazda.gms3.dmt.dao.JobRules.Item busy = com.mazda.gms3.dmt.dao.JobRules.busy(runningItems, new com.mazda.gms3.dmt.dao.JobRules.Item(
								schItemDetails.getLocale(), schItemDetails.getModelFolderName(), schItemDetails.getManualType(),
								schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), null));
						if(null!=busy)
						{
							// Row No. {0} is already Processing under - {1}
							errorMessage.append(msgProps().getMessage(new String[] { String.valueOf(schItemDetails.getSrNo()), busy.scheduleName },
									"error.message.item.already.processing"));
							errorMessage.append("\n");
							proceedFurther=false;
						}
					}
				}
				runningItems = null;

				if(proceedFurther==false)
				{
					StringBuilder msgBuilder = new StringBuilder();
					// cannot proceed further
					String message = msgProps().getProperty("label.cannot.proceed.further");
					msgBuilder.append(message);
					msgBuilder.append("\n");
					if(null!=errorMessage)
					{
						msgBuilder.append(errorMessage.toString());
					}
					sessionBean.setErrorMessage(msgBuilder.toString());
					message=  null;
					msgBuilder=  null;
				}
				else
				{
					/*
					 * Prepare Data for the Storing the data in Schedule & Criteria Table
					 */
					ScheduleDetails schDetails = new ScheduleDetails();
					schDetails.setScheduleName(ApplicationProperties.getProperty("schedule.name.mc.key")+DateFormatter.parseDateToString_YYYY_MM_DD(new Date())+"_");
					schDetails.setThreadId(ApplicationProperties.getProperty("schedule.name.mc.key"));
					schDetails.setUserId(wslId());
					schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
					// a Kapture load is always Draft - the Publish Content job publishes
					schDetails.setImDocsProcessingStatus(ApplicationProperties.getProperty("flag.value.draft"));
					long totalDocsCount=0;
					long totalDocsDeleteCount=0;
					long okAssetsCount=0;
					long okAssetsDeleteCount=0;
					long totalMetaDataDocsCount=0;
					long totalDisplayOrderCount=0;
					long totalCDProcessingCount=0;
					ArrayList<ScheduleItemDetails> itemsList = new ArrayList<ScheduleItemDetails>();
					for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
					{
						ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
						if(schItemDetails.isRowSelected()==true)
						{
							totalDocsCount= totalDocsCount+schItemDetails.getTotalDocsForProcessing();
							totalDocsDeleteCount = totalDocsDeleteCount+schItemDetails.getTotalDocsForDeletion();
							okAssetsCount = okAssetsCount + schItemDetails.getOkAssetsCount();
							okAssetsDeleteCount = okAssetsDeleteCount+ schItemDetails.getOkAssetsDeleteCount();
							totalMetaDataDocsCount = totalMetaDataDocsCount + schItemDetails.getMetaDocsCount();
							totalDisplayOrderCount=  totalDisplayOrderCount+ schItemDetails.getTotalDisplayOrderCount();
							totalCDProcessingCount  = totalCDProcessingCount+ schItemDetails.getTotalCDProcessingCount();
							itemsList.add(schItemDetails);
						}
					}
					schDetails.setTotalDocsForProcessing(totalDocsCount);
					schDetails.setTotalDocsForDeletion(totalDocsDeleteCount);
					schDetails.setOkAssetsCount(okAssetsCount);
					schDetails.setOkAssetsDeleteCount(okAssetsDeleteCount);
					schDetails.setTotalMetaDataDocsCount(totalMetaDataDocsCount);
					schDetails.setTotalCDProcessingCount(totalCDProcessingCount);
					schDetails.setTotalDisplayOrderCount(totalDisplayOrderCount);

					schDetails.setItemsList(new ArrayList<ScheduleItemDetails>());
					if(null!=itemsList && itemsList.size()>0)
					{
						schDetails.setItemsList(itemsList);
					}
					/*
					 * call function to store data
					 */

					Long scheduleId= ScheduleDAO.createSchedule(schDetails);
					if(null!=scheduleId && scheduleId>0)
					{
						ScheduleDAO.saveScheduleLoadType(scheduleId, sessionBean.getLoadTypeId());
						sessionBean.setScheduleName(schDetails.getScheduleName());
						String message = msgProps().addMessage("conversion.schedule.success", schDetails.getScheduleName());
						sessionBean.setSuccessMessage(message);
						message= null;
						/*
						 * Update Thread and start with customName
						 */
						String threadId = schDetails.getThreadId();
						final String code = String
								.valueOf(scheduleId);
						final String jobUser = wslId();
						final StartMCConversionImpl startConvImpl = new StartMCConversionImpl();
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {

								synchronized (startConvImpl) {
									try {
										startConvImpl.startConversion(String.valueOf(code), jobUser);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(Schedule.class.getName(), "run()", e);
									}
								}
							}
						};

						Thread th = new Thread(runn, threadId);
						th.start();

						threadId = null;

					}
					else
					{
						sessionBean.setErrorMessage(msgProps().getProperty("error.message.failure.schedule.conversion"));
					}
					scheduleId = null;
					schDetails = null;
					itemsList = null;
				}
			}
			//			else
			//			{
			//				// Maximum 5 conversion jobs can be scheduled in parallel. Please wait till the Processing jobs gets finished.
			//				sessionBean.setErrorMessage(msgProps().getProperty("error.message.max.jobs.scheduled"));
			//			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "scheduleMCOperation()", e);
		}
	}

	private void scheduleMMEOperation(HttpServletResponse response, ScheduleBean sessionBean)
	{
		try
		{

			/*
			 * CHECK HERE, 
			 * IF NO OF PENDING / PROCESSING JOBS ARE 5, DO NOT ALLOW TO SCHEDULE A JOB 
			 * 
			 * 
			 * IF FOR ANY OF THE SELECTED ITEMS
			 * MARKET
			 * LOCALE
			 * MODEL
			 * MANUAL TYPE
			 * MATERIAL NAME 
			 * IS IN PROCESSING STATE - STOP DO NOT ALLOW USER TO MOVE FORWARD
			 */
			// JOB COUNT CHECK.
			//			int pendingProcessingJobsCount= ScheduleDAO.getPendingProcessingJobsCount();
			//			if(pendingProcessingJobsCount<5)
			{
				/*
				 * A SELECTED ITEM (LOCALE + MODEL FOLDER + MANUAL TYPE + FACELIFT FOLDER + MATERIAL FOLDER) THAT A PENDING / RUNNING
				 * CONVERSION OR PUBLISH CONTENT JOB (ANY MARKET) IS PROCESSING CANNOT BE SCHEDULED UNTIL THAT JOB FINISHES (JobRules)
				 */
				boolean proceedFurther= true;
				StringBuilder errorMessage = new StringBuilder();
				java.util.List<com.mazda.gms3.dmt.dao.JobRules.Item> runningItems = com.mazda.gms3.dmt.dao.JobRules.runningContentItems();
				for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
				{
					ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
					if(schItemDetails.isRowSelected()==true)
					{
						com.mazda.gms3.dmt.dao.JobRules.Item busy = com.mazda.gms3.dmt.dao.JobRules.busy(runningItems, new com.mazda.gms3.dmt.dao.JobRules.Item(
								schItemDetails.getLocale(), schItemDetails.getModelFolderName(), schItemDetails.getManualType(),
								schItemDetails.getFaceLiftFolderName(), schItemDetails.getMaterialFolderName(), null));
						if(null!=busy)
						{
							// Row No. {0} is already Processing under - {1}
							errorMessage.append(msgProps().getMessage(new String[] { String.valueOf(schItemDetails.getSrNo()), busy.scheduleName },
									"error.message.item.already.processing"));
							errorMessage.append("\n");
							proceedFurther=false;
						}
					}
				}
				runningItems = null;

				if(proceedFurther==false)
				{
					StringBuilder msgBuilder = new StringBuilder();
					// cannot proceed further
					String message = msgProps().getProperty("label.cannot.proceed.further");
					msgBuilder.append(message);
					msgBuilder.append("\n");
					if(null!=errorMessage)
					{
						msgBuilder.append(errorMessage.toString());
					}
					sessionBean.setErrorMessage(msgBuilder.toString());
					message=  null;
					msgBuilder=  null;
				}
				else
				{
					/*
					 * Prepare Data for the Storing the data in Schedule & Criteria Table
					 */
					ScheduleDetails schDetails = new ScheduleDetails();
					schDetails.setScheduleName(ApplicationProperties.getProperty("schedule.name.mme.key")+DateFormatter.parseDateToString_YYYY_MM_DD(new Date())+"_");
					schDetails.setThreadId(ApplicationProperties.getProperty("schedule.name.mme.key"));
					schDetails.setUserId(wslId());
					schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
					// a Kapture load is always Draft - the Publish Content job publishes (as MC)
					schDetails.setImDocsProcessingStatus(ApplicationProperties.getProperty("flag.value.draft"));
					long totalDocsCount=0;
					long totalDocsDeleteCount=0;
					long okAssetsCount=0;
					long okAssetsDeleteCount=0;
					long totalMetaDataDocsCount=0;
					long totalDisplayOrderCount=0;
					long totalCDProcessingCount=0;
					long totalSCMCount=0;
					ArrayList<ScheduleItemDetails> itemsList = new ArrayList<ScheduleItemDetails>();
					for(int i=0;i<sessionBean.getScheduleItemsList().size();i++)
					{
						ScheduleItemDetails schItemDetails = (ScheduleItemDetails)sessionBean.getScheduleItemsList().get(i);
						if(schItemDetails.isRowSelected()==true)
						{
							totalDocsCount= totalDocsCount+schItemDetails.getTotalDocsForProcessing();
							totalDocsDeleteCount = totalDocsDeleteCount+schItemDetails.getTotalDocsForDeletion();
							okAssetsCount = okAssetsCount + schItemDetails.getOkAssetsCount();
							okAssetsDeleteCount = okAssetsDeleteCount+ schItemDetails.getOkAssetsDeleteCount();
							totalMetaDataDocsCount = totalMetaDataDocsCount + schItemDetails.getMetaDocsCount();
							totalDisplayOrderCount=  totalDisplayOrderCount+ schItemDetails.getTotalDisplayOrderCount();
							totalCDProcessingCount  = totalCDProcessingCount+ schItemDetails.getTotalCDProcessingCount();
							totalSCMCount = totalSCMCount + schItemDetails.getTotalSCMVinCount();
							itemsList.add(schItemDetails);
						}
					}
					schDetails.setTotalDocsForProcessing(totalDocsCount);
					schDetails.setTotalDocsForDeletion(totalDocsDeleteCount);
					schDetails.setOkAssetsCount(okAssetsCount);
					schDetails.setOkAssetsDeleteCount(okAssetsDeleteCount);
					schDetails.setTotalMetaDataDocsCount(totalMetaDataDocsCount);
					schDetails.setTotalCDProcessingCount(totalCDProcessingCount);
					schDetails.setTotalDisplayOrderCount(totalDisplayOrderCount);
					schDetails.setTotalSCMVinCount(totalSCMCount);

					schDetails.setItemsList(new ArrayList<ScheduleItemDetails>());
					if(null!=itemsList && itemsList.size()>0)
					{
						schDetails.setItemsList(itemsList);
					}
					/*
					 * call function to store data
					 */

					Long scheduleId= ScheduleDAO.createSchedule(schDetails);
					if(null!=scheduleId && scheduleId>0)
					{
						ScheduleDAO.saveScheduleLoadType(scheduleId, sessionBean.getLoadTypeId());
						sessionBean.setScheduleName(schDetails.getScheduleName());
						String message = msgProps().addMessage("conversion.schedule.success", schDetails.getScheduleName());
						sessionBean.setSuccessMessage(message);
						message= null;
						/*
						 * Update Thread and start with customName
						 */
						String threadId = schDetails.getThreadId();
						final String code = String
								.valueOf(scheduleId);
						final String jobUser = wslId();
						final StartMMEConversionImpl startConvImpl = new StartMMEConversionImpl();
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {

								synchronized (startConvImpl) {
									try {
										startConvImpl.startConversion(String.valueOf(code), jobUser);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(Schedule.class.getName(), "run()", e);
									}
								}
							}
						};

						Thread th = new Thread(runn, threadId);
						th.start();

						threadId = null;

					}
					else
					{
						sessionBean.setErrorMessage(msgProps().getProperty("error.message.failure.schedule.conversion"));
					}
					scheduleId = null;
					schDetails = null;
					itemsList = null;
				}
			}
			//			else
			//			{
			//				// Maximum 5 conversion jobs can be scheduled in parallel. Please wait till the Processing jobs gets finished.
			//				sessionBean.setErrorMessage(msgProps().getProperty("error.message.max.jobs.scheduled"));
			//			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "scheduleMMEOperation()", e);
		}
	}
	/*
	 * =====================================================================================================
	 * IPM FACELIFT VIN UPDATE - THE FACELIFT FOLDER (2026-10-11)
	 * No upload any more: every Excel of the folder source.facelift.folder.name (beside MDM, under the source content
	 * location of the Settings page) is read - same 11 columns, any mix of locales. "Validate Data" reads and checks them;
	 * the Identify button appears when every row is valid.
	 * =====================================================================================================
	 */

	/** The Excel files of the facelift folder, for the page (names only; empty = the "no Excel found" message). */
	private void refreshFaceliftFiles(ScheduleBean sessionBean)
	{
		List<String> names = new ArrayList<String>();
		try
		{
			for(File f : AutomationUtils.listFaceliftFiles(SourceContentPaths.faceliftFolder(serverPath())))
			{
				names.add(f.getName());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "refreshFaceliftFiles()", e);
		}
		sessionBean.setFaceliftFileNames(names);
		if(null==sessionBean.getAtLoadTypeId())
		{
			sessionBean.setAtLoadTypeId(ScheduleDAO.LOAD_TYPE_ONLY_CONTENT);
		}
	}

	/**
	 * Conversion "Go": true when the facelift folder holds Excel files. Order with all three folders = MDM (Master Data
	 * Syncing), then Facelift_Content (IPM Facelift VIN Update), then GMS3_Content (conversion) - so the user confirms the
	 * facelift job ran before the conversion records are listed. A folder that cannot be read counts as present (ask).
	 */
	private boolean faceliftFilesPresent()
	{
		try
		{
			return !AutomationUtils.listFaceliftFiles(SourceContentPaths.faceliftFolder(serverPath())).isEmpty();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "faceliftFilesPresent()", e);
			return true;
		}
	}

	/** "Validate Data": read every Excel of the facelift folder and check the rows. */
	private void validateFaceliftFolder(ScheduleBean sessionBean)
	{
		sessionBean.setAutomationItemsList(new ArrayList<ItemDetails>());
		sessionBean.setInputDataList(new ArrayList<ExcelRowDetails>());
		sessionBean.setShowIdenitfyButton(false);
		sessionBean.setShowAutomationGridBlock(false);
		try
		{
			File folder = SourceContentPaths.faceliftFolder(serverPath());
			List<File> files = AutomationUtils.listFaceliftFiles(folder);
			refreshFaceliftFiles(sessionBean);
			if(files.isEmpty())
			{
				logger.info("validateFaceliftFolder :: no Excel in "+PathUtil.winPath(folder));
				sessionBean.setErrorMessage(msgProps().addMessage("error.facelift.no.excel", PathUtil.winPath(folder)));
				return;
			}
			List<String> readErrors = new ArrayList<String>();
			ArrayList<ExcelRowDetails> inputDataList = AutomationUtils.readFaceliftFolder(files, readErrors);
			if(!readErrors.isEmpty())
			{
				StringBuilder errorMessage = new StringBuilder();
				for(String m : readErrors)
				{
					errorMessage.append(errorMessage.length()>0 ? "<MSG_TOKEN>" : "").append(m);
				}
				decideErrorDisplay(sessionBean, errorMessage, readErrors.size());
				return;
			}
			if(inputDataList.isEmpty())
			{
				logger.info("validateFaceliftFolder :: No Data Read from the facelift Excel files.");
				sessionBean.setErrorMessage(msgProps().getProperty("error.no.data.excel"));
				return;
			}
			logger.info("validateFaceliftFolder :: rows read from "+files.size()+" file(s) :: > "+inputDataList.size()+". Proceed to Validate them.");
			if(validateExcel(inputDataList, sessionBean))
			{
				sessionBean.setInputDataList(inputDataList);
				sessionBean.setShowIdenitfyButton(true);
				sessionBean.setSuccessMessage(msgProps().getMessage(new String[] { String.valueOf(inputDataList.size()), String.valueOf(files.size()) },
						"facelift.validate.success"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "validateFaceliftFolder()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}

	/** "Row No.<n> (<file>)" - commas taken out, the messages are split on them. */
	private static String rowRef(ExcelRowDetails data)
	{
		return String.valueOf(data.getRowNo())+(null==data.getFileName() ? "" : " ("+data.getFileName().replace(",", " ")+")");
	}

	/** MNAO: WMI+VDS+VISstart+VISend; MC / MME: CARLINE+WMI+VDS+VISstart+VISend with a "-" WMI as "___" - upper case. */
	private static String vinRefKey(String locale, String carline, String wmi, String vds, String visStart, String visEnd)
	{
		String key;
		if(AutomationDAO.isLocaleOf("mnao.locales", locale))
		{
			key = wmi+vds+visStart+visEnd;
		}
		else
		{
			key = carline+("-".equals(wmi.trim()) ? "___" : wmi)+vds+visStart+visEnd;
		}
		return Utilities.replaceCharsForRefKeys(key.trim().toUpperCase()).trim().toUpperCase();
	}

	/** The VIN as the messages show it: MNAO WMI+VDS+VIS; MC / MME "<model> <carline> <wmi>-<vds>-<start>-<end>". */
	private static String vinLabel(ExcelRowDetails data, boolean old)
	{
		String model = old ? data.getOldModelName() : data.getNewModelName();
		String carline = old ? data.getOldCarlineCode() : data.getNewCarlineCode();
		String wmi = old ? data.getOldWmiCode() : data.getNewWmiCode();
		String vds = old ? data.getOldVdsCode() : data.getNewVdsCode();
		String start = old ? data.getOldVisStartRange() : data.getNewVisStartRange();
		String end = old ? data.getOldVisEndRange() : data.getNewVisEndRange();
		if(AutomationDAO.isLocaleOf("mnao.locales", data.getLocale()))
		{
			return (wmi+vds+start+end).trim().toUpperCase();
		}
		String label = null!=model && !"".equals(model) ? model.replace(",", " ")+" " : "";
		return label+carline+" "+("-".equals(wmi.trim()) ? "" : wmi+"-")+vds+"-"+start+"-"+end;
	}

	/**
	 * The rows of the facelift Excel files:
	 *  - all 11 cells given, reference keys built (as before)
	 *  - locale in MDM and Kapture; OLD VIN range in MDM and Kapture (the documents carry it)
	 *  - NEW VIN range in MDM and Kapture - OR in the ALL_VIN master data file of the locale (loaded by "Master Data with
	 *    Facelift" at the start of the update job; "Only Facelift" cannot be scheduled while it is missing in Kapture)
	 * More than 10 errors go to a text file with a link (decideErrorDisplay). A check that cannot be made fails the
	 * validation - it never lets the rows through.
	 */
	private boolean validateExcel(ArrayList<ExcelRowDetails> inputDataList, ScheduleBean sessionBean)
	{
		StringBuilder errorMessage=new StringBuilder();
		int errorCount=0;
		try
		{
			for(ExcelRowDetails data : inputDataList)
			{
				// all mandatory columns are provided or not
				if(blank(data.getLocale()) || blank(data.getOldCarlineCode()) || blank(data.getOldWmiCode()) || blank(data.getOldVdsCode())
						|| blank(data.getOldVisStartRange()) || blank(data.getOldVisEndRange()) || blank(data.getNewCarlineCode())
						|| blank(data.getNewWmiCode()) || blank(data.getNewVdsCode()) || blank(data.getNewVisStartRange()) || blank(data.getNewVisEndRange()))
				{
					append(errorMessage, msgProps().addMessage("error.excel.row.data.invalid", rowRef(data)));
					errorCount++;
					continue;
				}
				data.setOldRefKey(vinRefKey(data.getLocale(), data.getOldCarlineCode(), data.getOldWmiCode(), data.getOldVdsCode(),
						data.getOldVisStartRange(), data.getOldVisEndRange()));
				data.setNewRefKey(vinRefKey(data.getLocale(), data.getNewCarlineCode(), data.getNewWmiCode(), data.getNewVdsCode(),
						data.getNewVisStartRange(), data.getNewVisEndRange()));
				if("".equals(data.getOldRefKey()))
				{
					append(errorMessage, msgProps().getMessage(new String[] { "Old", rowRef(data) }, "error.execel.row.data.fail.generate.refkey"));
					errorCount++;
				}
				if("".equals(data.getNewRefKey()))
				{
					append(errorMessage, msgProps().getMessage(new String[] { "New", rowRef(data) }, "error.execel.row.data.fail.generate.refkey"));
					errorCount++;
				}
			}

			// NO ERRORS FOUND - CHECK THE ROWS IN MDM, KAPTURE AND THE MASTER DATA FILES
			if(errorCount==0)
			{
				AutomationDAO.validateExcelData(inputDataList, SourceContentPaths.masterDataFolder(serverPath()));
				for(ExcelRowDetails data : inputDataList)
				{
					if(!data.isLocaleExistsInIM())
					{
						append(errorMessage, msgProps().getMessage(new String[] { data.getLocale(), rowRef(data) }, "error.execel.row.data.fail.locale.exists.im"));
						errorCount++;
					}
					if(!data.isLocaleExistsInMDM())
					{
						append(errorMessage, msgProps().getMessage(new String[] { data.getLocale(), rowRef(data) }, "error.execel.row.data.fail.locale.exists.mdm"));
						errorCount++;
					}
					if(!data.isOldVinExistsinIM())
					{
						append(errorMessage, msgProps().getMessage(new String[] { vinLabel(data, true), "Old", rowRef(data) }, "error.execel.row.data.fail.vin.exists.im"));
						errorCount++;
					}
					if(!data.isOldVinExistsinMDM())
					{
						append(errorMessage, msgProps().getMessage(new String[] { vinLabel(data, true), "Old", rowRef(data) }, "error.execel.row.data.fail.vin.exists.mdm"));
						errorCount++;
					}
					// a new range the master data file carries is loaded by "Master Data with Facelift"
					if(!data.isNewVinInMasterDataFile())
					{
						if(!data.isNewVinExistsinIM())
						{
							append(errorMessage, msgProps().getMessage(new String[] { vinLabel(data, false), "New", rowRef(data) }, "error.execel.row.data.fail.vin.exists.im"));
							errorCount++;
						}
						if(!data.isNewVinExistsinMDM())
						{
							append(errorMessage, msgProps().getMessage(new String[] { vinLabel(data, false), "New", rowRef(data) }, "error.execel.row.data.fail.vin.exists.mdm"));
							errorCount++;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "validateExcel()", e);
			// a check that could not be made is not a passed check
			append(errorMessage, msgProps().addMessage("error.facelift.validation.failed", String.valueOf(e.getMessage())));
			errorCount++;
		}
		if(errorCount>0)
		{
			decideErrorDisplay(sessionBean, errorMessage, errorCount);
			return false;
		}
		return true;
	}

	private static boolean blank(String s)
	{
		return null==s || "".equals(s.trim());
	}

	private static void append(StringBuilder errorMessage, String message)
	{
		if(errorMessage.length()>0)
		{
			errorMessage.append("<MSG_TOKEN>");
		}
		errorMessage.append(message);
	}


	private  void decideErrorDisplay(ScheduleBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_AUTOMATION_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
			File errorFile = PathUtil.file(eFPath+eFName);
			try {
				String data = errorMessage.toString();
				data = data.replace("<MSG_TOKEN>", "\n");

				FileOutputStream fos = PathUtil.fileOutputStream(errorFile);
				fos.write(data.getBytes());
				fos.flush();
				fos.close();
				fos = null;
				data = null;
			} catch (FileNotFoundException e) {
				Utilities.printStackTraceToLogs(ScheduleBean.class.getName(), "decideErrorDisplay()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(ScheduleBean.class.getName(), "decideErrorDisplay()", e);
			}
			errorFile=null;
			String webPath = com.mazda.gms3.dmt.utils.OkAssetsWeb.url(ApplicationProperties.getProperty("EXPORT_ERROR_WB_PATH")+eFName);

			// label.error.screen.help.text.start
			// label.here
			// label.view.the.details
			String message=msgProps().addMessage("error.import.invalid.custom.message", String.valueOf(errorCount));
			message = message + " "+msgProps().getProperty("label.error.screen.help.text.start");
			message = message+ " <a href=\""+webPath+"\" target=\"_blank\">" +msgProps().getProperty("label.here")+"</a>";
			message = message +" " +msgProps().getProperty("label.view.the.details");

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

	private boolean validateAutomationConversion(ScheduleBean sessionBean)
	{
		//		if(null==sessionBean.getAutomationItemsList() || sessionBean.getAutomationItemsList().size()<=0)
		//		{
		//			sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.select.one.item"));
		//			return false;
		//		}

		if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
		{
			boolean anyRowSelected = false;
			for(int i=0;i<sessionBean.getAutomationItemsList().size();i++)
			{
				ItemDetails schItemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(i);
				if(schItemDetails.isRowSelected()==true)
				{
					anyRowSelected = true;
				}
			}

			if(anyRowSelected==false)
			{
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.select.one.item"));
				return false;
			}
			long totalDocsCount=0;
			for(int i=0;i<sessionBean.getAutomationItemsList().size();i++)
			{
				ItemDetails schItemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(i);
				if(schItemDetails.isRowSelected()==true)
				{
					totalDocsCount= totalDocsCount+schItemDetails.getTotalDocumentsCount();
				}
			}

			if(totalDocsCount==0)
			{
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.no.docs.found.ipm.facelist.vin.update"));
				return false;
			}
		}
		else
		{
			/*
			 * IN INPUT LIST IS NOT NULL - THEN ALLOW TO SCHEDULE JOB
			 */
			if(null==sessionBean.getInputDataList() || sessionBean.getInputDataList().size()<=0)
			{
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.mandatory.select.one.item"));
				return false;
			}
		}
		/*
		 * JOB RULES (2026-10-16, JobRules): refused while a Conversion / Publish job is Pending or Processing, or when the
		 * parallel jobs cap is reached. Other Facelift / Document Identification / Master Data Syncing jobs may run.
		 */
		return masterJobAllowed(sessionBean, "validateAutomationConversion()");
	}
	/** JobRules for an IPM Facelift VIN Update / Document Identification job; false (and the message set) when refused. */
	private boolean masterJobAllowed(ScheduleBean sessionBean, String caller)
	{
		try
		{
			com.mazda.gms3.dmt.dao.JobRules.Refusal refusal = com.mazda.gms3.dmt.dao.JobRules.masterRefusal(com.mazda.gms3.dmt.dao.JobRules.active());
			if(null!=refusal)
			{
				sessionBean.setErrorMessage(msgProps().getMessage(refusal.args, refusal.key));
				return false;
			}
			return true;
		}
		catch (Exception e)
		{
			// the rules cannot be checked - nothing is scheduled
			Utilities.printStackTraceToLogs(Schedule.class.getName(), caller, e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
			return false;
		}
	}
	/**
	 * Schedules the update job (job B) for the selected items of the viewed identification job. The load type comes from
	 * the radios beside the Schedule button ("Master Data with Facelift" / "Only Facelift", gms3_dmt_conv_schedule.dc_load_type).
	 * "Only Facelift" is refused while a NEW VIN category of the selected items is still missing in Kapture (a range that
	 * was accepted because the master data file carries it) - checked against Kapture now, not as it was at validation.
	 */
	private void scheduleAutomation(ScheduleBean sessionBean)
	{
		try
		{
			if(!validateAutomationConversion(sessionBean))
			{
				return;
			}
			ScheduleDetails schDetails = new ScheduleDetails();
			schDetails.setScheduleName(ApplicationProperties.getProperty("automation.sch.key")+DateFormatter.parseDateToString_YYYY_MM_DD(new Date())+"_");
			schDetails.setThreadId(ApplicationProperties.getProperty("automation.sch.key"));
			schDetails.setUserId(wslId());
			schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
			long totalDocsCount=0;
			ArrayList<ItemDetails> itemsList = new ArrayList<ItemDetails>();
			List<Long> itemIds = new ArrayList<Long>();
			if(null!=sessionBean.getAutomationItemsList())
			{
				for(ItemDetails schItemDetails : sessionBean.getAutomationItemsList())
				{
					if(schItemDetails.isRowSelected())
					{
						totalDocsCount += null==schItemDetails.getTotalDocumentsCount() ? 0 : schItemDetails.getTotalDocumentsCount().longValue();
						if(null!=schItemDetails.getDocIdnItemId())
						{
							itemIds.add(schItemDetails.getDocIdnItemId());
						}
						itemsList.add(schItemDetails);
					}
				}
			}
			schDetails.setTotalDocsForProcessing(totalDocsCount);
			schDetails.setAutomationItemsList(itemsList);

			/*
			 * THE UNIQUE VINS OF THE SELECTED ITEMS AS INPUT LIST - RATHER THAN PICKING UP FROM SESSION
			 * UNIQUE KEY = LOCALE + OLD / NEW CARLINE, WMI, VDS, VIS START, VIS END, MODEL NAMES
			 */
			ArrayList<ExcelRowDetails> uniqueVinsInputList = AutomationIdentificationDAO.getUniqueVINsListForSelectedItems(itemIds);
			if(null==uniqueVinsInputList)
			{
				uniqueVinsInputList = new ArrayList<ExcelRowDetails>();
			}

			String loadType = ScheduleDAO.LOAD_TYPE_MASTER_DATA_WITH_CONTENT.equals(sessionBean.getAtLoadTypeId())
					? ScheduleDAO.LOAD_TYPE_MASTER_DATA_WITH_CONTENT : ScheduleDAO.LOAD_TYPE_ONLY_CONTENT;
			if(ScheduleDAO.LOAD_TYPE_ONLY_CONTENT.equals(loadType))
			{
				// "ONLY FACELIFT" - EVERY NEW VIN CATEGORY MUST BE IN KAPTURE NOW
				List<ExcelRowDetails> notThen = new ArrayList<ExcelRowDetails>();
				for(ExcelRowDetails v : uniqueVinsInputList)
				{
					if(!v.isNewVinExistsinIM())
					{
						notThen.add(v);
					}
				}
				if(!notThen.isEmpty())
				{
					Set<String> now = AutomationDAO.newVinsInKapture(notThen);
					Set<String> missing = new java.util.TreeSet<String>();
					for(ExcelRowDetails v : notThen)
					{
						if(!now.contains(v.getLocale().trim().replace("-", "_").toUpperCase()+"|"+v.getNewRefKey().trim().toUpperCase()))
						{
							missing.add(v.getLocale()+" "+v.getNewRefKey());
						}
					}
					if(!missing.isEmpty())
					{
						logger.info("scheduleAutomation :: Only Facelift refused - new VIN categories not in Kapture :: > "+missing);
						sessionBean.setErrorMessage(msgProps().getMessage(new String[] { String.valueOf(missing.size()),
								String.join(" / ", missing).replace(",", " ") }, "error.facelift.only.new.vin.missing"));
						return;
					}
				}
			}

			schDetails= ScheduleDAO.createAutomationSchedule(schDetails);
			if(null==schDetails || schDetails.getScheduleId()<=0)
			{
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.failure.ipm.facelift.vin.update.conversion"));
				return;
			}
			// what the job loads - read by the job itself (ScheduleDAO.isMasterDataWithContent)
			ScheduleDAO.saveScheduleLoadType(Long.valueOf(schDetails.getScheduleId()), loadType);
			sessionBean.setScheduleATName(schDetails.getScheduleName());
			sessionBean.setSuccessMessage(msgProps().addMessage("conversion.schedule.success", schDetails.getScheduleName()));

			String threadId = schDetails.getThreadId();
			final ScheduleDetails scheduleDetails = schDetails;
			final StartAutomationProcessingImpl startAutoImpl = new StartAutomationProcessingImpl();
			final ArrayList<ExcelRowDetails> inputDataList = uniqueVinsInputList;
			// captured here - the job thread has no request
			final String settingsPath = serverPath();
			Runnable runn = new Runnable()
			{
				@Override
				public void run() {

					synchronized (startAutoImpl) {
						try {
							startAutoImpl.startAutomation(scheduleDetails, inputDataList, settingsPath);
						} catch (Exception e) {
							Utilities.printStackTraceToLogs(Schedule.class.getName(), "run()", e);
						}
					}
				}
			};
			Thread th = new Thread(runn, threadId);
			th.start();
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "scheduleAutomation()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}


	private void identifyImpactedDocumentsDataForSchedule(ScheduleBean sessionBean)
	{
		try
		{
			// JOB RULES (2026-10-16): an impacted document can be in a running Conversion / Publish job
			if(!masterJobAllowed(sessionBean, "identifyImpactedDocumentsData()"))
			{
				return;
			}
			if(null!=sessionBean.getInputDataList() && sessionBean.getInputDataList().size()>0)
			{
				/*
				 * Prepare Data for the Storing the data in Schedule & Criteria Table
				 */
				ScheduleDetails schDetails = new ScheduleDetails();
				schDetails.setScheduleName(ApplicationProperties.getProperty("automation.identification.sch.key")+DateFormatter.parseDateToString_YYYY_MM_DD(new Date())+"_");
				schDetails.setThreadId(ApplicationProperties.getProperty("automation.identification.sch.key"));
				schDetails.setUserId(wslId());
				schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
				/*
				 * call function to store data
				 */
				Long scheduleId= AutomationIdentificationDAO.createSchedule(schDetails);
				if(null!=scheduleId && scheduleId>0)
				{
					sessionBean.setScheduleName(schDetails.getScheduleName());
					String message = msgProps().addMessage("conversion.schedule.success", schDetails.getScheduleName());
					sessionBean.setSuccessMessage(message);
					message= null;
					/*
					 * Update Thread and start with customName
					 */
					String threadId = schDetails.getThreadId();
					final String code = String.valueOf(scheduleId);
					final StartDocumentsIdentificationImpl docImpl = new StartDocumentsIdentificationImpl();
					final String scheduleName = sessionBean.getScheduleName();
					final ArrayList<ExcelRowDetails> inputDataList = sessionBean.getInputDataList();
					Runnable runn = new Runnable() 
					{
						@Override
						public void run() {

							synchronized (docImpl) {
								try {
									docImpl.startDocumentsIdentification(inputDataList, code, scheduleName);
								} catch (Exception e) {
									Utilities.printStackTraceToLogs(Schedule.class.getName(), "run()", e);
								}
							}
						}
					};

					Thread th = new Thread(runn, threadId);
					th.start();

					threadId = null;
					
					/*
					 * call function to update documentsIdenticiationJobsList
					 */
					getAtDocsIdentificationScheduleList(sessionBean);
					// reset Input List in sessionBean
					sessionBean.setInputDataList(null);
				}
				else
				{
					sessionBean.setErrorMessage(msgProps().getProperty("error.message.failure.schedule.conversion"));
				}
				scheduleId = null;
				schDetails = null;
			}
			else
			{
				logger.info("identifyImpactedDocumentsData :: Input Data List is null. Failed to read Excel File or Not data found in the uploaded Excel for Processing.");
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "identifyImpactedDocumentsData()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}

	private void viewImpactedDocumentsDetails(ScheduleBean sessionBean)
	{
		sessionBean.setShowAutomationGridBlock(false);
		sessionBean.setAutomationItemsList(new ArrayList<ItemDetails>());
		try
		{
			if(null!=sessionBean.getJobIdForATOperation() && !"".equals(sessionBean.getJobIdForATOperation()))
			{
				sessionBean.setShowAutomationGridBlock(true);
				//PROCEED FOR IDENTIFYING IMPACTED DOCUMENT DETAILS
				sessionBean.setAutomationItemsList(AutomationIdentificationDAO.getScheduleItemsList(sessionBean.getJobIdForATOperation()));

				// remove Items with docsCount 0
				if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
				{
					ItemDetails itemDetails = new ItemDetails();
					for(int a=0;a<sessionBean.getAutomationItemsList().size();a++)
					{
						itemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(a);
						if(null==itemDetails.getTotalDocumentsCount() || itemDetails.getTotalDocumentsCount()==0)
						{
							sessionBean.getAutomationItemsList().remove(a);
							a--;
						}
					}

					if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
					{
						// reset srNo
						itemDetails = new ItemDetails();
						for(int a=0;a<sessionBean.getAutomationItemsList().size();a++)
						{
							itemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(a);
							// UPDATE SR NO
							itemDetails.setSrNo(a+1);
							itemDetails.setDisplayCarlineInfo(itemDetails.getCarlineInfo());

							/*
							 * SET REMAKRS FOR SI, ACC, TR, VI
							 */
							if(null!=itemDetails.getChannelRefKey() && (itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES) || 
									itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE) || 
									itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_TRAINING) || 
									itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS) ))
							{
								// showCheckBox = false;
								itemDetails.setShowCheckBox(false);
								// set remarks
								itemDetails.setRemarks(msgProps().getProperty("label.document.noupdate.info"));
							}
						}
					}
				}
				// an update job takes its items out of the identification job (legacy) - say so instead of a blank page
				if(null==sessionBean.getAutomationItemsList() || sessionBean.getAutomationItemsList().isEmpty())
				{
					logger.info("viewImpactedDocumentsDetails :: no item left to schedule for identification job {"+sessionBean.getJobIdForATOperation()+"}.");
					sessionBean.setShowAutomationGridBlock(false);
					sessionBean.setErrorMessage(msgProps().getProperty("error.facelift.view.no.items"));
				}
			}
			else
			{
				logger.info("viewImpactedDocumentsDetails :: Schedule Id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Schedule.class.getName(), "viewImpactedDocumentsDetails()", e);
			sessionBean.setErrorMessage(msgProps().getProperty("error.message.faiure"));
		}
	}
}
