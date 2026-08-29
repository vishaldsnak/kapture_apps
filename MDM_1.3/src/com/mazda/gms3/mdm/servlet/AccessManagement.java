package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.AccessManagementBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.AccessManagementDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.ModuleComparator;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.RoleDetails;

/**
 * Servlet implementation class AccessManagement
 */
public class AccessManagement extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String wslId=null;
	private MessageProperties msgProps=null;
	private Logger logger = LogManager.getLogger(AccessManagement.class);
	
	String moduleRefKey= AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AccessManagement() {
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
			AccessManagementBean sessionBean = getSessionBean(request);
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
			sessionBean.setRoleId(null);
			sessionBean.setRolesList(null);
			sessionBean.setModulesList(null);
			sessionBean.setSelectedModules(null);
			sessionBean.setItemsList(null);
			
			/*
			 * CALL FUNCTION TO GET ROLES LIST
			 */
			getRolesList(sessionBean);
			/*
			 * CALL FUNCTION TO GET ITEMS LIST
			 */
			getItemsList(sessionBean);
			/*
			 * CALL FUNCTION TO GET MODULES LIST
			 */
			getModulesList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/accessManagement.jsp");
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
			AccessManagementBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			
			/*
			 * read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=request.getParameter("ACM_ActionClicked") && !"".equals(request.getParameter("ACM_ActionClicked")))
			{
				if(request.getParameter("ACM_ActionClicked").equals("SAVE"))
				{
					saveAccessManagementDetails(sessionBean);
				}
				else if(request.getParameter("ACM_ActionClicked").equals("ROLE_CHANGE"))
				{
					roleChangeOperation(sessionBean);
				}
				else if(request.getParameter("ACM_ActionClicked").equals("RESET"))
				{
					resetOperation(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/accessManagement.jsp");
				rs.forward(request, response);
			}
		}
	}
	
	private AccessManagementBean getSessionBean(HttpServletRequest request) 
	{
		AccessManagementBean sessionBean = null;
		if (null != request.getSession().getAttribute("accessManagementBean") && !"".equals(request.getSession().getAttribute("accessManagementBean"))) 
		{
			sessionBean = (AccessManagementBean) request.getSession().getAttribute("accessManagementBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new AccessManagementBean();
			request.getSession().setAttribute("accessManagementBean", sessionBean);
		}
		return sessionBean;
	}
	
	
	private void getRolesList(AccessManagementBean sessionBean)
	{
		try
		{
			sessionBean.setRolesList(new ArrayList<RoleDetails>());
			ArrayList<RoleDetails> list = new ArrayList<RoleDetails>();
			list = AccessManagementDAO.getRoleDetailsList();
			if(null!=list && list.size()>0)
			{
				sessionBean.setRolesList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "getRolesList()", e);
		}
	}
	
	private void getModulesList(AccessManagementBean sessionBean)
	{
		try
		{
			sessionBean.setModulesList(new ArrayList<ModuleDetails>());
			ArrayList<ModuleDetails> list = new ArrayList<ModuleDetails>();
			list = AccessManagementDAO.getModuleDetailsList();
			if(null!=list && list.size()>0)
			{
				for(int a=0;a<list.size();a++)
				{
					ModuleDetails moduleDetails = (ModuleDetails) list.get(a);
					// SET MODULE NAME
					moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+moduleDetails.getModuleRefkey()));
					// DO NOT ADD DEFAULT MODULES LIST
					if(moduleDetails.getModuleType()!=AccessManagementInterface.MODULE_TYPE_DEFAULT)
					{
						// add ITEM TO EACH OF THE MODULE 
						moduleDetails.setItemDetails(new ArrayList<ModuleDetails>());
						if(null!=sessionBean.getItemsList())
						{
							moduleDetails.setItemDetails(sessionBean.getItemsList());
						}
						sessionBean.getModulesList().add(moduleDetails);
					}
				}
			}
			list = null;
			
			
			/*
			 * IF MODULE LIST IS NOT NULL = SORT IT ON THE BASIS OF DISPLAY ORDER
			 * DATE - 16 JULY 2018
			 */
			if(null!=sessionBean.getModulesList() && sessionBean.getModulesList().size()>0)
			{
				Collections.sort(sessionBean.getModulesList(), new ModuleComparator());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "getModulesList()", e);
		}
	}
	
	private void getItemsList(AccessManagementBean sessionBean)
	{
		sessionBean.setItemsList(new ArrayList<ModuleDetails>());
		
		ModuleDetails si = new ModuleDetails();
		si.setItemCode(com.mazda.gms3.mdm.vo.AccessManagementInterface.ONLY_READ_ACCESS);
		si.setItemName(msgProps.getProperty("label.read.access"));
		sessionBean.getItemsList().add(si);
		si = null;
		
		si = new ModuleDetails();
		si.setItemCode(com.mazda.gms3.mdm.vo.AccessManagementInterface.ONLY_WRITE_ACCESS);
		si.setItemName(msgProps.getProperty("label.write.access"));
		sessionBean.getItemsList().add(si);
		si = null;
		
	}
	
	private void readParamsFromRequest(AccessManagementBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setRoleId(null);
			sessionBean.setSelectedModules(null);
			// resetModuleList
			resetModuleList(sessionBean);
			
			if(null!=request.getParameter("ACM_RoleId") && !"".equals(request.getParameter("ACM_RoleId")))
			{
				sessionBean.setRoleId((String)request.getParameter("ACM_RoleId").trim());
			}
			if(null!=request.getParameter("ACM_SelectedModules") && !"".equals(request.getParameter("ACM_SelectedModules")))
			{
				sessionBean.setSelectedModules((String)request.getParameter("ACM_SelectedModules").trim());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void saveAccessManagementDetails(AccessManagementBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getRoleId() && !"".equals(sessionBean.getRoleId()))
			{
				ArrayList<ModuleDetails> selectedModuleData = new ArrayList<ModuleDetails>();
				if(null!=sessionBean.getSelectedModules() && !"".equals(sessionBean.getSelectedModules()))
				{
					// HERE TOKEN WILL BE IN FOMRAT - 1:1||2#2:1||2# AND SO ON.
					String[] moduleCodes=sessionBean.getSelectedModules().split("#");
					if(null!=moduleCodes && moduleCodes.length>0)
					{
						for(int a=0;a<moduleCodes.length;a++)
						{
							String moduleToken = moduleCodes[a];
							if(null!=moduleToken && !"".equals(moduleToken))
							{
								// CHECK FOR THE ACCESS TYPES AND REAL CODE
								// TOKEN WILL BE IN FORMAT - 1:1||2
								String moduleCode="";
								String itemsToken="";
								String[] itemsSelected=null;
								if(moduleToken.indexOf(":")!=-1)
								{
									moduleCode = moduleToken.substring(0, moduleToken.indexOf(":"));
									itemsToken = moduleToken.substring(moduleToken.indexOf(":")+1, moduleToken.length());
									if(null!=itemsToken && !"".equals(itemsToken))
									{
										itemsSelected = itemsToken.split("\\|\\|");
									}
									itemsToken = null;
								}
								
								/*
								 * IF Module selected is not null & Items Selected Length is more than 0, proceed 
								 */
								if(null!=moduleCode && !"".equals(moduleCode) && null!=itemsSelected && itemsSelected.length>0)
								{
									ModuleDetails modDetails = new ModuleDetails();
									modDetails.setRoleId(new Long(sessionBean.getRoleId()).longValue());
									modDetails.setModuleId(new Long(moduleCode).longValue());
									int accessType=0;
									if(itemsSelected.length==2)
									{
										accessType= AccessManagementInterface.READ_AND_WRITE_ACCESS;
									}
									else
									{
										// GET THE VALUE AT INDEX 0 AND SET AS ACCESS
										accessType = new Integer(String.valueOf(itemsSelected[0])).intValue();
									}
									
									if(accessType>0)
									{
										modDetails.setAccessType(accessType);
										selectedModuleData.add(modDetails);
									}
									modDetails = null;
								}
								moduleCode=null;
								itemsSelected=null;
								itemsToken = null;
							}
							moduleToken = null;
						}
					}
				}
				
				if(null!=selectedModuleData && selectedModuleData.size()>0)
				{
					/*
					 * PROCEED FOR SAVING DATA
					 */
					boolean flag = AccessManagementDAO.saveAccessManagementData(selectedModuleData, sessionBean.getRoleId());
					if(flag==true)
					{
						logger.info("saveAccessManagementDetails :: Access Management Data Inserted Successfully.");
						// show Success Message
						sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.accessmanagement")));
						// reset fields
						sessionBean.setErrorMessage(null);
						// call functionToUpdateModuleList
						roleChangeOperation(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.accessmanagement")));
						logger.info("saveAccessManagementDetails :: Failed to Save Access Management Data.");
					}
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onemodule.to.map"));
				}
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.role")));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "saveAccessManagementDetails()", e);
		}
	}
	
	private void resetOperation(AccessManagementBean sessionBean)
	{

		sessionBean.setRoleId(null);
		// resetModuleList
		resetModuleList(sessionBean);
	}
	
	private void resetModuleList(AccessManagementBean sessionBean)
	{
		if(null!=sessionBean.getModulesList() && sessionBean.getModulesList().size()>0)
		{
			for(int b=0;b<sessionBean.getModulesList().size();b++)
			{
				ModuleDetails moduleDetails=(ModuleDetails)sessionBean.getModulesList().get(b);
				moduleDetails.setItemSelected(false);
				if(null!=moduleDetails.getItemDetails() && moduleDetails.getItemDetails().size()>0)
				{
					for(int r=0;r<moduleDetails.getItemDetails().size();r++)
					{
						ModuleDetails itemDetails = (ModuleDetails)moduleDetails.getItemDetails().get(r);
						itemDetails.setItemSelected(false);
					}
				}
			}
		}
	}
	
	private void roleChangeOperation(AccessManagementBean sessionBean)
	{
		try
		{
			// resetModuleList
			resetModuleList(sessionBean);
			
			/*
			 * CHECK IF ROLE ID IS NOT NULL
			 * 	THEN FETCH ALL THE MAPPED MODULES FOR THE LOGGED IN USER
			 * AND SET THE SELECTED VALUES ACCORDINGLY IN MASTER LIST
			 * ELSE LOAD THE LIST AS IT IS.
			 */
			if(null!=sessionBean.getRoleId() && !"".equals(sessionBean.getRoleId()))
			{
				ArrayList<ModuleDetails> roleBasedList = AccessManagementDAO.getExistingAccessDetails(sessionBean.getRoleId(), "");
				if(null!=roleBasedList && roleBasedList.size()>0)
				{
					if(null!=sessionBean.getModulesList() && sessionBean.getModulesList().size()>0)
					{
						for(int i=0;i<sessionBean.getModulesList().size();i++)
						{
							ModuleDetails moduleDetails=(ModuleDetails)sessionBean.getModulesList().get(i);
							for(int a=0;a<roleBasedList.size();a++)
							{
								ModuleDetails existingDetails = (ModuleDetails)roleBasedList.get(a);
								if(null!=existingDetails.getModuleId() && null!=moduleDetails.getModuleId() && 
										existingDetails.getModuleId().longValue()==moduleDetails.getModuleId().longValue())
								{
									if(null!=existingDetails.getAccessType() && existingDetails.getAccessType()>0)
									{
										moduleDetails.setItemSelected(true);
										ArrayList<ModuleDetails> tempItemsList = new ArrayList<ModuleDetails>();
										/*
										 * CHECK IF ACCESS TYPE IS 3
										 * THEN SET OTH ITEM SELECTED
										 * ELSE SET THE DEFINED ITEM SELECTED
										 */
										if(existingDetails.getAccessType()==AccessManagementInterface.READ_AND_WRITE_ACCESS)
										{
											if(null!=moduleDetails.getItemDetails() && moduleDetails.getItemDetails().size()>0)
											{
												for(int r=0;r<moduleDetails.getItemDetails().size();r++)
												{
													ModuleDetails itemDetails = (ModuleDetails)moduleDetails.getItemDetails().get(r);
													ModuleDetails newItemDetails = new ModuleDetails();
													newItemDetails.setItemCode(itemDetails.getItemCode());
													newItemDetails.setItemName(itemDetails.getItemName());
													newItemDetails.setItemSelected(true);
													tempItemsList.add(newItemDetails);
													newItemDetails= null;
												}
											}
										}
										else
										{
											if(null!=moduleDetails.getItemDetails() && moduleDetails.getItemDetails().size()>0)
											{
												for(int r=0;r<moduleDetails.getItemDetails().size();r++)
												{
													ModuleDetails itemDetails = (ModuleDetails)moduleDetails.getItemDetails().get(r);
													ModuleDetails newItemDetails = new ModuleDetails();
													newItemDetails.setItemCode(itemDetails.getItemCode());
													newItemDetails.setItemName(itemDetails.getItemName());
													newItemDetails.setItemSelected(false);
													if(itemDetails.getItemCode().equals(existingDetails.getAccessType()))
													{
														newItemDetails.setItemSelected(true);
													}
													tempItemsList.add(newItemDetails);
													newItemDetails= null;
												}
											}
										}
										
										if(null!=tempItemsList && tempItemsList.size()>0)
										{
											moduleDetails.setItemDetails(tempItemsList);
										}
										tempItemsList = null;
									}
									break;
								}
							}
						}
					}
				}
				roleBasedList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "roleChangeOperation()", e);
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
	
	private void performAccessCheck(AccessManagementBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(AccessManagement.class.getName(), "performAccessCheck()", e);
		}
	}

	
}
