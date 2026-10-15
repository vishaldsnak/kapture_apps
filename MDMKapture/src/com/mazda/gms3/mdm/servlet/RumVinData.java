package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.RumVinDataBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.RumVinScheduleDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.RumVinDetails;

/**
 * Servlet implementation class RumVinData
 */
public class RumVinData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger logger = LogManager.getLogger(RumVinData.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;

	String moduleRefKey=AccessManagementInterface.REF_KEY_RUM_VIN_DATA;
       
	private int maxEntriesPerPage=50;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RumVinData() {
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
			RumVinDataBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			

			/*
			 * REQUEST HAS BEEN MADE FOR LOADING THE FRESH DATA
			 */
			sessionBean.setDataList(null);
			sessionBean.setTotalPages(0);
			sessionBean.setOffset(0);
			sessionBean.setCurrentPageNo(0);
			sessionBean.setNavigationBlock(false);
			sessionBean.setFirstDisbled(false);
			sessionBean.setLastDisabled(false);
			sessionBean.setPreviousDisabled(false);
			sessionBean.setNextDisabled(false);
			sessionBean.setDisplayDataList(null);
			/*
			 * Call function to load - History List
			 */
			getRumVinDataList(sessionBean);

			/*
			 * SINCE HERE, THE PAGE IS GETTING LOADED FROM ALL INDEX PAGE
			 * RELOAD EVERYTHING.
			 */
			// SET CURRENT PAGE NO TO 0
			sessionBean.setCurrentPageNo(0);
			// SET OFFSET TO 0
			sessionBean.setOffset(0);
			if(null!=sessionBean.getDataList() && sessionBean.getDataList().size()>0)
			{
				// IDENTIFY TOTAL PAGES
				getTotalPages(sessionBean);
				// SET CURRENT PAGE NO AS BY DEFAULT 1
				sessionBean.setCurrentPageNo(1);
				// CALCULATE OFFSET - BY DEFAULT PAGE HAS TO BE 1
				calculateOffSet(sessionBean, sessionBean.getCurrentPageNo());
				// POPULATE DISAPLAY INDEX SST LIST
				populateDisplayList(sessionBean);
				// PERFORM NAVIGATION BLOCK OPERATION
				navigationBlockOperation(sessionBean);
				// PREPARE JSP DATA
				prepareJSPData(sessionBean);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mme/rumvindata.jsp");
				rs.forward(request, response);
			}
		}
			
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			RumVinDataBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			String actionClicked="";
			if(null!=request.getParameter("RUMVINDATA_ActionClicked") && !"".equals(request.getParameter("RUMVINDATA_ActionClicked")))
			{
				actionClicked= (String)request.getParameter("RUMVINDATA_ActionClicked");
				
				if(actionClicked.equals("NEXT"))
				{
					// add 1 from currentPage No, ensure it's not more than totalPages
					int no = sessionBean.getCurrentPageNo()  + 1;
					if(no> sessionBean.getTotalPages())
					{
						sessionBean.setCurrentPageNo(sessionBean.getTotalPages());
					}
					else
					{
						sessionBean.setCurrentPageNo(no);
					}
					// CALCULATE OFFSET - 
					calculateOffSet(sessionBean, sessionBean.getCurrentPageNo());
					// POPULATE DISAPLAY INDEX SST LIST
					populateDisplayList(sessionBean);
					// PERFORM NAVIGATION BLOCK OPERATION
					navigationBlockOperation(sessionBean);
					// PREPARE JSP DATA
					prepareJSPData(sessionBean);
					
				}
				else if(actionClicked.equals("PREVIOUS"))
				{
					// subTract 1 from currentPage No, ensure it's not zero
					int no = sessionBean.getCurrentPageNo() -1;
					if(no<=0)
					{
						sessionBean.setCurrentPageNo(1);
					}
					else
					{
						sessionBean.setCurrentPageNo(no);
					}
					// CALCULATE OFFSET - 
					calculateOffSet(sessionBean, sessionBean.getCurrentPageNo());
					// POPULATE DISAPLAY INDEX SST LIST
					populateDisplayList(sessionBean);
					// PERFORM NAVIGATION BLOCK OPERATION
					navigationBlockOperation(sessionBean);
					// PREPARE JSP DATA
					prepareJSPData(sessionBean);
					
				}
				else if(actionClicked.equals("FIRST"))
				{
					// set Current Page equals to Page 1
					sessionBean.setCurrentPageNo(1);
					// CALCULATE OFFSET - 
					calculateOffSet(sessionBean, sessionBean.getCurrentPageNo());
					// POPULATE DISAPLAY INDEX SST LIST
					populateDisplayList(sessionBean);
					// PERFORM NAVIGATION BLOCK OPERATION
					navigationBlockOperation(sessionBean);
					// PREPARE JSP DATA
					prepareJSPData(sessionBean);
					
				}
				else if(actionClicked.equals("LAST"))
				{
					// set Current Page equals to Total Pages No.
					sessionBean.setCurrentPageNo(sessionBean.getTotalPages());
					// CALCULATE OFFSET - 
					calculateOffSet(sessionBean, sessionBean.getCurrentPageNo());
					// POPULATE DISAPLAY INDEX SST LIST
					populateDisplayList(sessionBean);
					// PERFORM NAVIGATION BLOCK OPERATION
					navigationBlockOperation(sessionBean);
					// PREPARE JSP DATA
					prepareJSPData(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mme/rumvindata.jsp");
				rs.forward(request, response);
			}
		}
	}


	private void performAccessCheck(RumVinDataBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "performAccessCheck()", e);
		}
	}
	
	private RumVinDataBean getSessionBean(HttpServletRequest request) 
	{
		RumVinDataBean sessionBean = null;
		if (null != request.getSession().getAttribute("rumVinDataBean") && !"".equals(request.getSession().getAttribute("rumVinDataBean"))) 
		{
			sessionBean = (RumVinDataBean) request.getSession().getAttribute("rumVinDataBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new RumVinDataBean();
			request.getSession().setAttribute("rumVinDataBean", sessionBean);
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

	private static void getRumVinDataList(RumVinDataBean sessionBean)
	{
		sessionBean.setDataList(new ArrayList<RumVinDetails>());
		sessionBean.setDisplayDataList(new ArrayList<RumVinDetails>());
		try
		{
			ArrayList<RumVinDetails> list = RumVinScheduleDAO.getRumVinDataList();
			if(null!=list && list.size()>0)
			{
				sessionBean.setDataList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "getRumVinDataList()", e);
		}
		
	}
	
	
	/**
	 * Function will Identify Total No of Pages.
	 * @param sessionBean
	 */
	private void getTotalPages(RumVinDataBean sessionBean)
	{
		sessionBean.setTotalPages(0);
		try
		{
			if(null!=sessionBean.getDataList() && sessionBean.getDataList().size()>0)
			{
				int pages = sessionBean.getDataList().size() / maxEntriesPerPage;
				if (sessionBean.getDataList().size() % maxEntriesPerPage != 0) 
				{
					pages = pages + 1;
				}
				sessionBean.setTotalPages(pages);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(),"getTotalPages()", e);
		}
	}
	
	/**
	 * Fuction will calculate the Offset for the Display List
	 * @param sessionBean
	 * @param page
	 */
	private void calculateOffSet(RumVinDataBean sessionBean, int page)
	{
		try
		{
			if(page>0)
			{
				int offset = maxEntriesPerPage * (page - 1);
				sessionBean.setOffset(offset);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(),"calculateOffSet()", e);
		}
	}
	
	/**
	 * Function will Populate SST Display Index List on the basis of OffSet & Length
	 * @param sessionBean
	 */
	private void populateDisplayList(RumVinDataBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getDataList() && sessionBean.getDataList().size()>0)
			{
				sessionBean.setDisplayDataList(new ArrayList<RumVinDetails>());
				int to = sessionBean.getOffset() + maxEntriesPerPage;
				
				if (sessionBean.getOffset() > sessionBean.getDataList().size())
				{
					sessionBean.setOffset(sessionBean.getDataList().size());
				}
				
				if (to > sessionBean.getDataList().size())
				{
					to = sessionBean.getDataList().size();
				}
				
				for (int i = sessionBean.getOffset(); i < to; i++) 
				{
					sessionBean.getDisplayDataList().add(sessionBean.getDataList().get(i));
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "populateDisplayList()", e);
		}
	}
	
	/**
	 * Function will Perform Navigation Buttons Enable Disable Operation
	 * @param sessionBean
	 */
	private void navigationBlockOperation(RumVinDataBean sessionBean)
	{
		sessionBean.setNavigationBlock(false);
		sessionBean.setFirstDisbled(false);
		sessionBean.setLastDisabled(false);
		sessionBean.setPreviousDisabled(false);
		sessionBean.setNextDisabled(false);
		try
		{
			/*
			 * IT TOTAL PAGES IS 1
			 * 	DO NOT SHOW ANY NAVIGATION BLOCL
			 * IF MORE THAN 1
			 * 	SHOW NAVIGATION BLOCK
			 * 	
			 */
			if(sessionBean.getTotalPages()>1)
			{
				// SET SHOW NAVIGATION BLOCK TRUE
				sessionBean.setNavigationBlock(true);
				// if CURRENT PAGE IS 1
				if(sessionBean.getCurrentPageNo()==1)
				{
					// DISABLE FIRST & PREVIOUS BUTTONS
					sessionBean.setFirstDisbled(true);
					sessionBean.setPreviousDisabled(true);
					
					sessionBean.setNextDisabled(false);
					sessionBean.setLastDisabled(false);
				}
				// if CURRENT PAGE IS LAST PAGE
				else if(sessionBean.getCurrentPageNo()==sessionBean.getTotalPages())
				{
					// DISABLE NEXT & LAST BUTTONS
					
					sessionBean.setFirstDisbled(false);
					sessionBean.setPreviousDisabled(false);
					
					sessionBean.setNextDisabled(true);
					sessionBean.setLastDisabled(true);
				}
				else
				{
					// ENABLE ALL BUTTONS
					sessionBean.setFirstDisbled(false);
					sessionBean.setPreviousDisabled(false);
					
					sessionBean.setNextDisabled(false);
					sessionBean.setLastDisabled(false);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "navigationBlockOperation()", e);
		}
	}
	
	private void prepareJSPData(RumVinDataBean sessionBean)
	{
		sessionBean.setJspData("");
		try
		{
			if(null!=sessionBean.getDisplayDataList() && sessionBean.getDisplayDataList().size()>0)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
				StringBuilder str = new StringBuilder();
				for(int i=0; i<sessionBean.getDisplayDataList().size();i++)
				{
					RumVinDetails details = (RumVinDetails)sessionBean.getDisplayDataList().get(i);
					if(i%2==0)
					{
						str.append("<tr>");
					}
					else
					{
						str.append("<tr class=\"selected\">");
					}
					// column 1
					str.append("<td style=\"border-left: 1px solid #DADADA;border-right: 1px solid #ddd;text-align:center;\">");
					str.append(details.getSrNo());
					str.append("</td>");
					// column 2
					str.append("<td style=\"border-right: 1px solid #ddd;text-align:center;\">"+details.getRussiaVin()+" </td>");
					str.append("<td style=\"border-right: 1px solid #ddd;text-align:center;\">"+details.getMazdaVin()+" </td>");
					str.append("<td style=\"border-right: 1px solid #ddd;text-align:center;\">");
					if(null!=details.getCreatedTime())
					{
						str.append(sdf.format(details.getCreatedTime()));
					}
					str.append("</td>");
					str.append("<td style=\"text-align:center;\">");
					if(null!=details.getModifiedTime())
					{
						str.append(sdf.format(details.getModifiedTime()));
					}
					str.append("</td>");					
					
					str.append("</tr>");
					details = null;
				}
				sdf = null;
				
				if(null!=str)
				{
					sessionBean.setJspData(str.toString());
				}
				str = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RumVinData.class.getName(), "prepareData()", e);
		}
	}


}
