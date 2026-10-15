package com.mazda.gms3.cdrom.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.cdrom.bean.CDRomHistoryBean;
import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;

public class CDRomHistoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomSettingsServlet.class);

	static MessageProperties msgProps = null;

	static String wslId = "";
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomHistoryServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		try {
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
			}
			msgProps = new MessageProperties(request.getSession().getAttribute(
					"MDM_LS_Locale"));

			CDRomHistoryBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false
					&& sessionBean.isShowWriteControls() == false) {
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setScheduleList(null);
			sessionBean.setItemsList(null);
			sessionBean.setDisplayScheduleDetails(null);
			sessionBean.setShowDetailsPopUp(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			// set today's date as ToDate
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sessionBean.setToDate(sdf.format(new Date()));
			// get 30 days before today's date
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.DATE, -30);
			Date dateBefore30Days = cal.getTime();
			sessionBean.setFromDate(sdf.format(dateBefore30Days));
			cal  =null;
			dateBefore30Days = null;
			/*
			 * call function to load Schedule List
			 */
			getScheduleList(sessionBean);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomHistoryServlet.class.getName(), "doGet()", e);
		}
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList()
					|| userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean
							.getTopMenuList().size() == 1)) {
				response.sendRedirect(request.getContextPath() + "/mypage");
			} else {
				RequestDispatcher rs = request
						.getRequestDispatcher("/jsps/cdrom/history.jsp");
				rs.forward(request, response);
			}
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		try {
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
			}
			msgProps = new MessageProperties(request.getSession().getAttribute(
					"MDM_LS_Locale"));

			CDRomHistoryBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false
					&& sessionBean.isShowWriteControls() == false) {
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			/*
			 * set displayPageNo and displayPageLenght
			 */
			if (null != request.getParameter("MDM_HIS_displayPageNo")
					&& !"".equals(request.getParameter("MDM_HIS_displayPageNo"))) {
				sessionBean.setDisplayPageNo((String) request
						.getParameter("MDM_HIS_displayPageNo"));
			}
			if (null != request.getParameter("MDM_HIS_displayPageLen")
					&& !"".equals(request
							.getParameter("MDM_HIS_displayPageLen"))) {
				sessionBean.setDisplayPageLength((String) request
						.getParameter("MDM_HIS_displayPageLen"));
			}
			
			if(null!=request.getParameter("fromDateField") && !"".equals(request.getParameter("fromDateField")))
			{
				sessionBean.setFromDate((String)request.getParameter("fromDateField"));
			}
			if(null!=request.getParameter("toDateField") && !"".equals(request.getParameter("toDateField")))
			{
				sessionBean.setToDate((String)request.getParameter("toDateField"));
			}

			if (null != request.getParameter("MDM_HIS_ActionClicked")
					&& !"".equals(request.getParameter("MDM_HIS_ActionClicked"))) {
				if (request.getParameter("MDM_HIS_ActionClicked")
						.equals("CLOSE_ITEM_DETAILS")) {
					/*
					 * close the opened pop up
					 */
					sessionBean.setItemsList(null);
					sessionBean.setShowDetailsPopUp(false);
				} else if (request.getParameter("MDM_HIS_ActionClicked")
						.equals("ABORT_CONVERSION")) {
					/*
					 * call function to abort the conversion
					 */
					abortConversionOperation(request, sessionBean);
				} else if (request.getParameter("MDM_HIS_ActionClicked")
						.equals("RESET")) {
					/*
					 * call function to reload historyList
					 */
					/*
					 * call function to load Schedule List
					 */
					getScheduleList(sessionBean);
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomHistoryServlet.class.getName(), "doPost()", e);
		}
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList()
					|| userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean
							.getTopMenuList().size() == 1)) {
				response.sendRedirect(request.getContextPath() + "/mypage");
			} else {
				RequestDispatcher rs = request
						.getRequestDispatcher("/jsps/cdrom/history.jsp");
				rs.forward(request, response);
			}
		}
	}

	private CDRomHistoryBean getSessionBean(HttpServletRequest request) {
		CDRomHistoryBean sessionBean = null;
		if (null != request.getSession().getAttribute("historyBean")
				&& !"".equals(request.getSession().getAttribute("historyBean"))) {
			sessionBean = (CDRomHistoryBean) request.getSession().getAttribute(
					"historyBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CDRomHistoryBean();
			request.getSession().setAttribute("historyBean", sessionBean);
		}
		return sessionBean;
	}

	private static void getScheduleList(CDRomHistoryBean sessionBean) {
		try {
			sessionBean.setRunReloadScript(false);
			sessionBean.setScheduleList(new ArrayList<CDRomScheduleDetails>());
			
			if(null!=sessionBean.getFromDate() && !"".equals(sessionBean.getFromDate()) && null!=sessionBean.getToDate()  && !"".equals(sessionBean.getToDate()))
			{
				ArrayList<CDRomScheduleDetails> tempList = CDRomDAO
						.getScheduleDetails("",sessionBean.getFromDate(),sessionBean.getToDate());
				if (null != tempList && tempList.size() > 0) {
					for (int i = 0; i < tempList.size(); i++) {
						CDRomScheduleDetails schDetails = (CDRomScheduleDetails) tempList
								.get(i);
						if (schDetails
								.getScheduleStatus()
								.equals(ApplicationProperties
										.getProperty("schedule.status.processing.value"))) {
							sessionBean.setRunReloadScript(true);
							break;
						}
					}

					String webServerPath = ApplicationProperties.getProperty("cdrom.extracted.webserver.path");
					/*
					 * update schedule status and show Abort Flag
					 */
					for (int i = 0; i < tempList.size(); i++) {
						CDRomScheduleDetails schDetails = (CDRomScheduleDetails) tempList
								.get(i);
						schDetails.setShowAbort(false);
						schDetails.setShowViewLink(false);
						if (null != schDetails.getScheduleStatus()
								&& !"".equals(schDetails.getScheduleStatus())) {
							if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.pending.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.pending.label"));
								// set showAbortLink to true
								if (wslId != null
										&& wslId.equals(schDetails.getUserId())) {
									schDetails.setShowAbort(true);
								}
							} else if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.processing.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.processing.label"));
								// set showAbortLink to true
								if (wslId != null
										&& wslId.equals(schDetails.getUserId())) {
									schDetails.setShowAbort(true);
								}
							} else if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.success.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.success.label"));
							} else if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.failure.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.failure.label"));
							} else if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.aborted.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.aborted.label"));
							} else if (schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.no.record.value"))) {
								schDetails
								.setScheduleStatusLabel(msgProps
										.getProperty("schedule.status.no.record.label"));
							}

							/*
							 * if schedule status is success / failure - then  show View Link
							 * set the view Path for the CD
							 */
							if(schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.success.value")) || schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.failure.value")))
							{
								schDetails.setShowViewLink(true);
								if(null!=schDetails.getZipFileName() && !"".equals(schDetails.getZipFileName()))
								{
									String zipFolderName="";
									if(schDetails.getZipFileName().lastIndexOf("/")!=-1)
									{
										zipFolderName = schDetails.getZipFileName().substring(schDetails.getZipFileName().lastIndexOf("/")+1, schDetails.getZipFileName().length());
										if(null!=zipFolderName && !"".equals(zipFolderName))
										{
											// remove extension
											if(zipFolderName.lastIndexOf(".")!=-1)
											{
												zipFolderName = zipFolderName.substring(0, zipFolderName.lastIndexOf("."));
											}
											if(null!=zipFolderName && !"".equals(zipFolderName))
											{
												zipFolderName= zipFolderName.trim();
												// add Context & web server Path
												schDetails.setViewLinkPath(webServerPath+zipFolderName+"/index.html");
											}
										}
									}
									zipFolderName = null;
								}
							}


							/*
							 * UPDATE FAILURE DOCS COUNT. DO THIS ONLY WHEN MARKET
							 * VALUE IS MNAO, FOR MC SHOW THE VALUES AS IT IS
							 * FETCHED FROM DATABASE
							 */
							if (!schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.pending.value"))
									&& !schDetails
									.getScheduleStatus()
									.equals(ApplicationProperties
											.getProperty("schedule.status.processing.value"))) {
								/*
								 * DO THIS ONLY FOR MNAO MARKET
								 */
								if (null != schDetails.getMarketName()
										&& schDetails
										.getMarketName()
										.trim()
										.toLowerCase()
										.equals(ApplicationProperties
												.getProperty("market.mnao")
												.trim().toLowerCase())) {
									schDetails
									.setFailedProcessedDocsCount(schDetails
											.getTotalDocsForProcessing()
											- schDetails
											.getProcessedDocsCount());
									schDetails.setFailedOkAssetsCount(schDetails
											.getOkAssetsCount()
											- schDetails
											.getProcessedOkAssetsCount());
									// set Failed Inner Links Count.
									schDetails.setFailedInnerLinksCount(schDetails
											.getTotalInnerLinksCount()
											- schDetails
											.getProcessedInnerLinksCount());
								}
							}
						}
						sessionBean.getScheduleList().add(schDetails);
						schDetails = null;
					}
					webServerPath = null;
				}
				tempList = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			sessionBean.setErrorMessage(msgProps
					.getProperty("error.message.faiure"));
		}
	}


	@SuppressWarnings("deprecation")
	private static void abortConversionOperation(HttpServletRequest request,
			CDRomHistoryBean sessionBean) {
		try {
			if (null != request.getParameter("MDM_HIS_ScheduleId")
					&& !"".equals(request.getParameter("MDM_HIS_ScheduleId"))
					&& null != request.getParameter("MDM_HIS_ThreadId")
					&& !"".equals(request.getParameter("MDM_HIS_ThreadId"))) {
				String scheduleId = (String) request
						.getParameter("MDM_HIS_ScheduleId");
				String threadId = (String) request
						.getParameter("MDM_HIS_ThreadId");
				logger.info("abortConversionOperation :: Killing Thread Id {"
						+ threadId + "} for Schedule ::> " + scheduleId);

				/*
				 * Get all the Active Threads and match the thread on the basis
				 * of NAME
				 */
				Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
				Thread[] threadArray = threadSet.toArray(new Thread[threadSet
						.size()]);
				if (null != threadArray && threadArray.length > 0) {
					boolean threadFound = false;
					for (int i = 0; i < threadArray.length; i++) {
						Thread at = threadArray[i];
						if (null != at.getName() && !"".equals(at.getName())) {
							if (at.getName().equals(threadId)) {
								threadFound = true;
								/*
								 * THE Thread.stop() CALL THAT WAS HERE IS GONE. It was removed
								 * from Java in 20 and threw UnsupportedOperationException straight
								 * into the catch below, so it never stopped anything - the
								 * schedule was marked Aborted while the worker ran on.
								 *
								 * The scan itself STAYS, unlike the other abort screens: here the
								 * updateAbortStatus() call below sits INSIDE this loop and only
								 * runs when the thread is found, so removing the scan would delete
								 * the abort write itself. The worker now stops itself by reading
								 * that status - see CDRomDAO.isAborted().
								 */
								if (at.isAlive()) {
									logger.info("abortConversionOperation :: worker thread {"
											+ threadId + "} is alive - it will stop itself at the"
											+ " next check once the Aborted status is written.");
								}
								/*
								 * call function to update ABort Status
								 * update all the items status to completed  as well
								 */
								try
								{
									boolean flag = CDRomDAO.updateAbortStatus(scheduleId,ApplicationProperties.getProperty("schedule.status.aborted.value"));
									if(flag==true)
									{
										sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
									}
									else
									{
										sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
									}
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(CDRomHistoryServlet.class.getName(), "abortConversion()", e);
									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
								}
								/*
								 * re-call Schedule List
								 */
								getScheduleList(sessionBean);
								break;
							}
						}
					}

					if (threadFound == false) 
					{
//						sessionBean.setErrorMessage(msgProps.getProperty("error.message.conversion.not.active"));
						/*
						 * Do not show any error message - simple abort the conversion
						 * call function to update ABort Status
						 */
						try
						{
							boolean flag = CDRomDAO.updateAbortStatus(scheduleId,ApplicationProperties.getProperty("schedule.status.aborted.value"));
							if(flag==true)
							{
								sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
							}
							else
							{
								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
							}
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(CDRomHistoryServlet.class.getName(), "abortConversion()", e);
							sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
						}
						
						/*
						 * re-call Schedule List
						 */
						getScheduleList(sessionBean);
					}
				} else {
//					sessionBean.setErrorMessage(msgProps.getProperty("error.message.conversion.not.active"));
					/*
					 * Do not show any error message - simple abort the conversion
					 * call function to update ABort Status
					 */
					try
					{
						boolean flag = CDRomDAO.updateAbortStatus(scheduleId,ApplicationProperties.getProperty("schedule.status.aborted.value"));
						if(flag==true)
						{
							sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
						}
						else
						{
							sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(CDRomHistoryServlet.class.getName(), "abortConversion()", e);
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					}
					/*
					 * re-call Schedule List
					 */
					getScheduleList(sessionBean);
				}
				threadArray = null;
				threadSet = null;
			} else {
				logger.info("abortConversionOperation :: Schedule id & Thread id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(CDRomHistoryServlet.class.getName(), "abortConversionOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}

	private UserAccessBean getUserSessionBean(HttpServletRequest request) {
		UserAccessBean sessionBean = null;
		if (null != request.getSession().getAttribute("userAccessBean")
				&& !"".equals(request.getSession().getAttribute(
						"userAccessBean"))) {
			sessionBean = (UserAccessBean) request.getSession().getAttribute(
					"userAccessBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new UserAccessBean();
			request.getSession().setAttribute("userAccessBean", sessionBean);
		}
		return sessionBean;
	}

	private void performAccessCheck(CDRomHistoryBean sessionBean,
			HttpServletRequest request) {
		/*
		 * Check User Has access to this Functionality or Not. Identify if User
		 * is Super Admin - then enable Read / Write Access on this page else -
		 * check if user has access to this functionality check for the
		 * accessType if READ ACCESS - SHOW READ CONTROLS if WRITE ACCESS - SHOW
		 * WRITE CONTROLS if READ & WRITE ACCESS - SHOW READ & WRITE CONTROLS
		 */
		try {
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setShowReadControls(false);
			sessionBean.setShowWriteControls(false);
			if (userSessionBean.isSuperAdminUser() == true) {
				sessionBean.setShowReadControls(true);
				sessionBean.setShowWriteControls(true);
			} else if (userSessionBean.isSuperAdminUser() == false) {
				if (null != userSessionBean.getUserAllModulesList()
						&& userSessionBean.getUserAllModulesList().size() > 0) {
					for (int a = 0; a < userSessionBean.getUserAllModulesList()
							.size(); a++) {
						ModuleDetails modDetails = (ModuleDetails) userSessionBean
								.getUserAllModulesList().get(a);
						if (modDetails.getModuleRefkey().trim().toLowerCase()
								.equals(moduleRefKey.trim().toLowerCase())) {
							// USER HAS ACCESS TO THIS SCREEN. CHECK FOR ACCESS
							// TYPE
							if (modDetails.getAccessType() == AccessManagementInterface.ONLY_READ_ACCESS) {
								sessionBean.setShowReadControls(true);
							} else if (modDetails.getAccessType() == AccessManagementInterface.ONLY_WRITE_ACCESS) {
								sessionBean.setShowWriteControls(true);
							} else if (modDetails.getAccessType() == AccessManagementInterface.READ_AND_WRITE_ACCESS) {
								sessionBean.setShowReadControls(true);
								sessionBean.setShowWriteControls(true);
							} else {
								sessionBean.setShowReadControls(false);
								sessionBean.setShowWriteControls(false);
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomHistoryServlet.class.getName(),
					"performAccessCheck()", e);
		}
	}

}
