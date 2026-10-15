package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.SIInnerLinksIdentificationHistoryBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.SIInnerLinksIdentificationHistoryDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.GenerateFinalResponse;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SIInnerLinksIdentificationTransactionDetails;

/**
 * Servlet implementation class SIInnerLinksIdentificationHistory
 */
public class SIInnerLinksIdentificationHistory extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	Logger logger = LogManager.getLogger(SIInnerLinksIdentificationHistory.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;

	String moduleRefKey=AccessManagementInterface.REF_KEY_SI_INNERLINKS_IDENTIFICATION_HISTORY;

	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SIInnerLinksIdentificationHistory() {
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
			
			/*
			 * Initialize bean
			 */
			SIInnerLinksIdentificationHistoryBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setHistoryList(null);
			
			/*
			 * Call function to load - History List
			 */
			getHistoryList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistory.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/siInnerLinksIdentification.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try
		{
			if(null!=request.getParameter("SISM_GetReports") && !"".equals(request.getParameter("SISM_GetReports")))
			{
				String finalOutPut="";
				if(null!=request.getParameter("SISM_ScheduleId") && !"".equals(request.getParameter("SISM_ScheduleId")))
				{
					String scheduleId = (String)request.getParameter("SISM_ScheduleId");
					UserAccessBean userAccessBean = getUserSessionBean(request);
					/*
					 * GET REPORTS LIST ON THE BASIS OF SCHJEDULE ID
					 * ALSO GET USER'S LOCALES LIST
					 * IF USER IS SUPER ADMIN - THEN SHOW ALL LOCALES REPORTS
					 * ELSE IDENTIFY THE LOCALES MAPPED WITH LOGGED IN USER AND SHOW ONLY APPLICABLE LOCALE REPORTS
					 */
					ArrayList<SIInnerLinksIdentificationTransactionDetails> reportsList = SIInnerLinksIdentificationHistoryDAO.getReportsList(scheduleId);
					if(null!=reportsList && reportsList.size()>0)
					{
						ArrayList<SIInnerLinksIdentificationTransactionDetails> applicableReportsList = new ArrayList<SIInnerLinksIdentificationTransactionDetails>();
						if(userAccessBean.isSuperAdminUser()==true)
						{
							// LOGGED IN USER IS SUPER ADMIN - SET ALL REPORTS
							applicableReportsList  = reportsList;
						}
						else 
						{
							if(null!=userAccessBean.getUserLocalesList() && userAccessBean.getUserLocalesList().size()>0)
							{
								for(String userLocale : userAccessBean.getUserLocalesList())
								{
									userLocale = userLocale.replace("_", "-");
									for(SIInnerLinksIdentificationTransactionDetails data : reportsList)
									{
										String keyToCheck = data.getReportLocale();
										keyToCheck = keyToCheck.replace("_", "-");
										if(userLocale.trim().toLowerCase().equals(keyToCheck.trim().toLowerCase()))
										{
											// add to Applicable List
											applicableReportsList.add(data);
											break;
										}
										keyToCheck = null;
										data = null;
									}
									userLocale= null;
								}
							}
						}
						
						if(null!=applicableReportsList && applicableReportsList.size()>0)
						{
							/*
							 * GENERATE FINAL OUTPUT
							 */
							StringBuilder str = new StringBuilder();
							for(SIInnerLinksIdentificationTransactionDetails data : applicableReportsList)
							{
								str.append("<tr>");
								str.append("<td>"+data.getSrNo()+"</td>");
								str.append("<td>"+data.getReportLocale()+"</td>");
								if(null!=data.getReportsPath() && !"".equals(data.getReportsPath()) && !"null".equals(data.getReportsPath()))
								{
									str.append("<td style=\\\"text-align: center;\\\"><a href=\\\""+data.getReportsPath()+"\\\" style=\\\"text-decoration:none;\\\" target=\\\"_blank\\\"><i class=\\\"downloadReports\\\"></i></a></td>");
								}
								else
								{
									str.append("<td style=\\\"text-align: center;\\\"> - </td>");
								}
								str.append("</tr>");
								data = null;
							}
							
							if(null!=str)
							{
								finalOutPut = str.toString();
							}
							str = null;
						}
						else
						{
							logger.info("doPost :: No Applicable Reports Data found for Schedule Id :: > " + scheduleId);
						}
						
						applicableReportsList = null;
					}
					else
					{
						logger.info("doPost :: No Reports Data found in Database for Schedule id :: > " + scheduleId);
					}
					reportsList = null;
					scheduleId = null;
				}
				else
				{
					logger.info("doPost :: Schedule id as Parameter is null in Request.");
				}
				
				/*
				 * SEND RESPONSE BACK TO SERVER
				 */
				GenerateFinalResponse.generateFinalResponse(response, "DATA", finalOutPut);
				finalOutPut=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistory.class.getName(), "doPost()", e);
		}
	}

	private SIInnerLinksIdentificationHistoryBean getSessionBean(HttpServletRequest request) 
	{
		SIInnerLinksIdentificationHistoryBean sessionBean = null;
		if (null != request.getSession().getAttribute("siInnerLinksIdentificationHistoryBean") && !"".equals(request.getSession().getAttribute("siInnerLinksIdentificationHistoryBean"))) 
		{
			sessionBean = (SIInnerLinksIdentificationHistoryBean) request.getSession().getAttribute("siInnerLinksIdentificationHistoryBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SIInnerLinksIdentificationHistoryBean();
			request.getSession().setAttribute("siInnerLinksIdentificationHistoryBean", sessionBean);
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
	
	private void performAccessCheck(SIInnerLinksIdentificationHistoryBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistory.class.getName(), "performAccessCheck()", e);
		}
	}

	
	private static void getHistoryList(SIInnerLinksIdentificationHistoryBean sessionBean)
	{
		sessionBean.setRunReloadScript(false);
		sessionBean.setHistoryList(new ArrayList<SIInnerLinksIdentificationTransactionDetails>());
		try
		{
			ArrayList<SIInnerLinksIdentificationTransactionDetails> list = SIInnerLinksIdentificationHistoryDAO.getHistoryDataList();
			if(null!=list && list.size()>0)
			{
				for(int i=0;i<list.size();i++)
				{
					SIInnerLinksIdentificationTransactionDetails schDetails = (SIInnerLinksIdentificationTransactionDetails)list.get(i);
					if(null!=schDetails.getJobStatus() && !"".equals(schDetails.getJobStatus()) && schDetails.getJobStatus().trim().toLowerCase().equals("processing"))
					{
						sessionBean.setRunReloadScript(true);
						break;
					}
				}
				
				sessionBean.setHistoryList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SIInnerLinksIdentificationHistory.class.getName(), "getHistoryList()", e);
		}
		
	}
}
