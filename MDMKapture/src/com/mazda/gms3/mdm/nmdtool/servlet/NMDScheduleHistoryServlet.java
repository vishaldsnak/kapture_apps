package com.mazda.gms3.mdm.nmdtool.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dataexttool.servlet.DataExportToolHistoryServlet;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.nmdtool.bean.NMDScheduleHistorySessionBean;
import com.mazda.gms3.mdm.nmdtool.dao.NMDScheduleDAO;
import com.mazda.gms3.mdm.nmdtool.vo.NMDJobDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

/**
 * Servlet implementation class NMDScheduleHistoryServlet
 */
public class NMDScheduleHistoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger logger = LogManager.getLogger(NMDScheduleHistoryServlet.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL_HISTORY;
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NMDScheduleHistoryServlet() {
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
			
			
			NMDScheduleHistorySessionBean sessionBean = getSessionBean(request);
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
			sessionBean.setActionClicked(null);
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
			 * Call function to load - History List
			 */
			getHistoryList(sessionBean);
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleHistoryServlet.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/nmdtool/history.jsp");
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
			
			
			NMDScheduleHistorySessionBean sessionBean = getSessionBean(request);
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
			sessionBean.setActionClicked(null);
			
			if(null!=request.getParameter("NMDSCHHISTORY_displayPageNo") && !"".equals(request.getParameter("NMDSCHHISTORY_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("NMDSCHHISTORY_displayPageNo"));
			}
			if(null!=request.getParameter("NMDSCHHISTORY_displayPageLen") && !"".equals(request.getParameter("NMDSCHHISTORY_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("NMDSCHHISTORY_displayPageLen"));
			}
			if(null!=request.getParameter("NMDSCHHISTORY_ActionClicked") && !"".equals(request.getParameter("NMDSCHHISTORY_ActionClicked")))
			{
				sessionBean.setActionClicked((String)request.getParameter("NMDSCHHISTORY_ActionClicked"));
			}
			if(null!=request.getParameter("fromDateField") && !"".equals(request.getParameter("fromDateField")))
			{
				sessionBean.setFromDate((String)request.getParameter("fromDateField"));
			}
			if(null!=request.getParameter("toDateField") && !"".equals(request.getParameter("toDateField")))
			{
				sessionBean.setToDate((String)request.getParameter("toDateField"));
			}
			
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("RESET"))
				{
					getHistoryList(sessionBean);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/nmdtool/history.jsp");
				rs.forward(request, response);
			}
		}
	}

	private NMDScheduleHistorySessionBean getSessionBean(HttpServletRequest request) 
	{
		NMDScheduleHistorySessionBean sessionBean = null;
		if (null != request.getSession().getAttribute("nmdScheduleHistorySessionBean") && !"".equals(request.getSession().getAttribute("nmdScheduleHistorySessionBean"))) 
		{
			sessionBean = (NMDScheduleHistorySessionBean) request.getSession().getAttribute("nmdScheduleHistorySessionBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new NMDScheduleHistorySessionBean();
			request.getSession().setAttribute("nmdScheduleHistorySessionBean", sessionBean);
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
	
	private void performAccessCheck(NMDScheduleHistorySessionBean sessionBean, HttpServletRequest request)
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
						if(modDetails.getModuleRefkey().trim().toLowerCase().equals(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL.trim().toLowerCase()))
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
			Utilities.printStackTraceToLogs(NMDScheduleHistoryServlet.class.getName(), "performAccessCheck()", e);
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
			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL);
			moduleDetails.setModulePath("/nmdschedule");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;
			
			// ADD RMI TOOL HISTORY PAGE
			moduleDetails = new ModuleDetails();
			moduleDetails.setModuleRefkey(moduleRefKey);
			moduleDetails.setModulePath("/nmdschedulehistory");
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
			Utilities.printStackTraceToLogs(NMDScheduleHistoryServlet.class.getName(), "performNavigationOperation()", e);
		}
	}
	
	private void getHistoryList(NMDScheduleHistorySessionBean sessionBean)
	{
		sessionBean.setRunReloadScript(false);
		sessionBean.setTransationsList(new ArrayList<NMDJobDetails>());
		try
		{
			if(null!=wslId && !"".equals(wslId))
			{
				List<NMDJobDetails> list = NMDScheduleDAO.getJobsList(sessionBean.getFromDate() , sessionBean.getToDate());
				if(null!=list && list.size()>0)
				{
					NMDJobDetails schDetails = null;
					for(int i=0;i<list.size();i++)
					{
						schDetails = (NMDJobDetails)list.get(i);
						if(null!=schDetails.getRunningStatus() && !"".equals(schDetails.getRunningStatus()) && 
								(schDetails.getRunningStatus().equals(ScheduleConstants.STATUS_PENDING) || 
										schDetails.getRunningStatus().equals(ScheduleConstants.STATUS_PROCESSING)))
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
			Utilities.printStackTraceToLogs(NMDScheduleHistoryServlet.class.getName(), "getHistoryList()", e);
		}
	}
	
}
