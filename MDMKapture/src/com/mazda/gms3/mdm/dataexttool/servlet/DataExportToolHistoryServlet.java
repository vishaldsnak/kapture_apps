package com.mazda.gms3.mdm.dataexttool.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.dataexttool.bean.DataExportToolHistorySessionBean;
import com.mazda.gms3.mdm.dataexttool.dao.DataExportToolDAO;
import com.mazda.gms3.mdm.dataexttool.vo.DataExportToolScheduleDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

/**
 * Servlet implementation class DataExportToolHistoryServlet
 */
public class DataExportToolHistoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private Logger logger = LogManager.getLogger(DataExportToolHistoryServlet.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL_HISTORY;
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DataExportToolHistoryServlet() {
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
			
			
			DataExportToolHistorySessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setTransationsList(null);
			
			/*
			 * Call function to load - History List
			 */
			getHistoryList(sessionBean);
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			// PLOT TOP MENU AND UPDATE USER SESSION BEAN
			performNavigationOperation(userSessionBean, request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/dataexttool/history.jsp");
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
			
			
			DataExportToolHistorySessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			
			if(null!=request.getParameter("DATAEXPTOOLHISTORY_displayPageNo") && !"".equals(request.getParameter("DATAEXPTOOLHISTORY_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("DATAEXPTOOLHISTORY_displayPageNo"));
			}
			if(null!=request.getParameter("DATAEXPTOOLHISTORY_displayPageLen") && !"".equals(request.getParameter("DATAEXPTOOLHISTORY_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("DATAEXPTOOLHISTORY_displayPageLen"));
			}
			
			if(null!=request.getParameter("DATAEXPTOOLHISTORY_ActionClicked") && !"".equals(request.getParameter("DATAEXPTOOLHISTORY_ActionClicked")))
			{
				if(((String)request.getParameter("DATAEXPTOOLHISTORY_ActionClicked")).equals("ABORT_SCHEDULE"))
				{
					abortConversionOperation(request, sessionBean);
				}
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "doPost()", e);
		}
		
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			// PLOT TOP MENU AND UPDATE USER SESSION BEAN
			performNavigationOperation(userSessionBean, request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/dataexttool/history.jsp");
				rs.forward(request, response);
			}
		}
	}


	private DataExportToolHistorySessionBean getSessionBean(HttpServletRequest request) 
	{
		DataExportToolHistorySessionBean sessionBean = null;
		if (null != request.getSession().getAttribute("dataExportToolHistorySessionBean") && !"".equals(request.getSession().getAttribute("dataExportToolHistorySessionBean"))) 
		{
			sessionBean = (DataExportToolHistorySessionBean) request.getSession().getAttribute("dataExportToolHistorySessionBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new DataExportToolHistorySessionBean();
			request.getSession().setAttribute("dataExportToolHistorySessionBean", sessionBean);
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
	
	private void performAccessCheck(DataExportToolHistorySessionBean sessionBean, HttpServletRequest request)
	{
		/*
		 * Check User Has access to MME SI VIN Functionality or Not.
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
						// CHECK FOR RMI TOOL ACCESS
						if(modDetails.getModuleRefkey().trim().toLowerCase().equals(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL.trim().toLowerCase()))
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
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "performAccessCheck()", e);
		}
	}
	
	private void performNavigationOperation(UserAccessBean userSessionBean, HttpServletRequest request)
	{
		try
		{
			/*
			 * CHECK FOR THE ENGINE TYPE LIST IN USER SESSION BEAN
			 * REDIRECT TO THE ONE AT 0 INDEX
			 * SET ALL THE ITEMS TO TOP MENU LIST IN USER SESSION BEAN
			 * UPDATE USER SESSION BEAN OBJECT IN HTTP SESSION
			 */
			userSessionBean.setTopMenuList(new ArrayList<ModuleDetails>());
			// ADD DASHBOARD
			ModuleDetails moduleDetails = new ModuleDetails();
			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_DASHBOARD);
			moduleDetails.setModulePath("/mypage");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;
			
			// ADD RMI TOOL
			moduleDetails = new ModuleDetails();
			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL);
			moduleDetails.setModulePath("/mnaodataexport");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;
			
			// ADD RMI TOOL HISTORY PAGE
			moduleDetails = new ModuleDetails();
			moduleDetails.setModuleRefkey(moduleRefKey);
			moduleDetails.setModulePath("/mnaodataexporthistory");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;
			
			/*
			 * 	UPDATE USER BEAN IN SESSION OBJECT
			 */
			request.getSession().removeAttribute("userAccessBean");
			request.getSession().setAttribute("userAccessBean", userSessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "performNavigationOperation()", e);
		}
	}
	
	private void getHistoryList(DataExportToolHistorySessionBean sessionBean)
	{
		sessionBean.setRunReloadScript(false);
		sessionBean.setTransationsList(new ArrayList<DataExportToolScheduleDetails>());
		try
		{
			if(null!=wslId && !"".equals(wslId))
			{
				ArrayList<DataExportToolScheduleDetails> list = DataExportToolDAO.getHistoryTransactionList();
				if(null!=list && list.size()>0)
				{
					DataExportToolScheduleDetails schDetails = null;
					for(int i=0;i<list.size();i++)
					{
						schDetails = (DataExportToolScheduleDetails)list.get(i);
						if(null!=schDetails.getProcessingStatus() && !"".equals(schDetails.getProcessingStatus()) && 
								(schDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_PENDING) || 
										schDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_PROCESSING)))
						{
							sessionBean.setRunReloadScript(true);
							break;
						}
						schDetails = null;
					}
					
					sessionBean.setTransationsList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "getHistoryList()", e);
		}
	}
	
	private void abortConversionOperation(HttpServletRequest request, DataExportToolHistorySessionBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("DATAEXPTOOLHISTORY_AbortScheduleId") && !"".equals(request.getParameter("DATAEXPTOOLHISTORY_AbortScheduleId")))
			{
				// abort conversion
				String scheduleId=(String)request.getParameter("DATAEXPTOOLHISTORY_AbortScheduleId");
//				String threadId="MGSS_MNAO_EXPORT_"+scheduleId;
				String threadId="";
			
				/*
				 * identify Thread Id from History List
				 */
				if(null!=sessionBean.getTransationsList() && sessionBean.getTransationsList().size()>0)
				{
					DataExportToolScheduleDetails schDetails = null;
					for(int a=0;a<sessionBean.getTransationsList().size();a++)
					{
						schDetails = (DataExportToolScheduleDetails)sessionBean.getTransationsList().get(a);
						if(String.valueOf(schDetails.getScheduleId()).equals(scheduleId))
						{
							threadId = schDetails.getThreadId();
							break;
						}
						schDetails = null;
					}
					schDetails = null;
				}
				
				logger.info("abortConversionOperation :: Killing Thread Id {"+ threadId+"} for Schedule ::> " + scheduleId);
				
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
					DataExportToolDAO.updateAbortStatus(scheduleId);
					
//					NotificationEmailHelper.generateNotificationEmail(scheduleId);
					
					sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
					
					/*
					 * re-call Transaction List
					 */
					getHistoryList(sessionBean);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "abortConversionOperation()", e);
					logger.info("abortConversionOperation :: Error while updating status for Schedule Id {"+scheduleId+"} in DATABASE.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
				}
			}
			else
			{
				logger.info("abortConversionOperation :: Schedule id & Thread id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DataExportToolHistoryServlet.class.getName(), "abortConversionOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
		}
	}


}
