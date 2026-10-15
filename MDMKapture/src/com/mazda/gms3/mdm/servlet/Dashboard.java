package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DashboardCategories;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.DashboardCategoryDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;

/**
 * Servlet implementation class Dashboard
 */

public class Dashboard extends HttpServlet {
	private static final long serialVersionUID = 1L;

	String wslId=null;
	MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_DASHBOARD;

	/*
	 * THE CATEGORY TAB THE USER WAS ON WHEN THEY LEFT MY PAGE. HELD IN THE SESSION SO THE TAB
	 * SURVIVES THE ROUND TRIP THROUGH WHATEVER SCREEN THEY OPENED.
	 */
	private static final String SESSION_KEY_DASH_TAB = "MDM_DASH_TAB";
	private static final String PARAM_DASH_TAB = "DASH_TAB_SELECTED";
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Dashboard() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		boolean showNoAccess=false;
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
			 * RESET TOP MENU LIST
			 * ADD ONLY MY PAGE OPTION
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			performNavigationOperation(null, userSessionBean, request);
			if(useReqDis==true)
			{
				if(null!=userSessionBean)
				{
					if(null==userSessionBean.getUserAllModulesList() || userSessionBean.getUserAllModulesList().size()<=0)
					{
						showNoAccess = true;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Dashboard.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * CHECK HERE USER MODULES LIST IS NULL -THEN REDIRECT TO NO ACCESS PAGE
			 * ELSE DASHBOARD JSP
			 */
			if(showNoAccess==true)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
			}
			else
			{
				setDashboardCategories(request);
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/dashboard.jsp");
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
		String redirectToMDMPages="";
		boolean showNoAccess=false;
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
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
			 * REMEMBER WHICH CATEGORY TAB THE CLICKED TILE BELONGED TO, BEFORE ANY REDIRECT.
			 * COMING BACK TO MY PAGE LATER RE OPENS IT - SEE setDashboardCategories().
			 */
			if(null!=request.getParameter(PARAM_DASH_TAB) && !"".equals(request.getParameter(PARAM_DASH_TAB)))
			{
				request.getSession().setAttribute(SESSION_KEY_DASH_TAB, request.getParameter(PARAM_DASH_TAB).trim());
			}

			if(useReqDis==true)
			{
				if(null!=userSessionBean)
				{
					// user does not have any module mapped.
					if(null==userSessionBean.getUserAllModulesList() || userSessionBean.getUserAllModulesList().size()<=0)
					{
						showNoAccess=true;
					}
				}
			}
			
			if(null!=request.getParameter("DASH_TILE_CLICKED_VAL") && !"".equals(request.getParameter("DASH_TILE_CLICKED_VAL")))
			{
				if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("DEFAULT"))
				{
					if(null!=request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL") && !"".equals(request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL")))
					{
						/*
						 * ADD TOP MENU 
						 * AND THEN CLICKED MODULE
						 */
						performNavigationOperation(null, userSessionBean, request);
						if(null!=userSessionBean.getDefaultModulesList() && userSessionBean.getDefaultModulesList().size()>0)
						{
							for(ModuleDetails modDetails : userSessionBean.getDefaultModulesList())
							{
								if(modDetails.getModuleRefkey().trim().toLowerCase().equals(String.valueOf(request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL")).trim().toLowerCase()))
								{
									// ADD TO TOP MENU & SET REDIRECT PATH
									redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
									userSessionBean.getTopMenuList().add(modDetails);
									break;
								}
								modDetails = null;
							}
						}
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("NORMAL"))
				{
					if(null!=request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL") && !"".equals(request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL")))
					{
						/*
						 * ADD TOP MENU 
						 * AND THEN CLICKED MODULE
						 */
						performNavigationOperation(null, userSessionBean, request);
						if(null!=userSessionBean.getNormalModulesList() && userSessionBean.getNormalModulesList().size()>0)
						{
							for(ModuleDetails modDetails : userSessionBean.getNormalModulesList())
							{
								if(modDetails.getModuleRefkey().trim().toLowerCase().equals(String.valueOf(request.getParameter("DASH_TILE_CLICKED_REFKEY_VAL")).trim().toLowerCase()))
								{
									// ADD TO TOP MENU & SET REDIRECT PATH
									redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
									userSessionBean.getTopMenuList().add(modDetails);
									/*
									 * put a check here - if MME SIVIN RANGE / MC SIVIN RANGE / MNAO SIVIN RANGE / RMI TOOL
									 * History page for each market has to go and added to Top Menu List
									 */
									if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_MME_SIVIN_RANGE))
									{
										// ADD SI VIN BATCH TRANSACTIONS PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_MME_SIVIN_BATCH_TRANSACTIONS);
										moduleDetails.setModulePath("/mmesivinbatchtransactions");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									else if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_MC_SIVIN_RANGE))
									{
										// ADD SI VIN BATCH TRANSACTIONS PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_MC_SIVIN_BATCH_TRANSACTIONS);
										moduleDetails.setModulePath("/mcsivinbatchtransactions");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									else if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_MNAO_SIVIN_RANGE))
									{
										// ADD SI VIN BATCH TRANSACTIONS PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_MNAO_SIVIN_BATCH_TRANSACTIONS);
										moduleDetails.setModulePath("/mnaosivinbatchtransactions");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									else if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_RMI_TOOL))
									{
										// ADD RMI TOOL HISTORY PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_RMI_TOOL_HISTORY);
										moduleDetails.setModulePath("/rmitoolhistory");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									else if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL))
									{
										// ADD RMI TOOL HISTORY PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL_HISTORY);
										moduleDetails.setModulePath("/mnaodataexporthistory");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									else if(modDetails.getModuleRefkey().equals(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL))
									{
										// ADD RMI TOOL HISTORY PAGE
										ModuleDetails moduleDetails = new ModuleDetails();
										moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL_HISTORY);
										moduleDetails.setModulePath("/nmdschedulehistory");
										moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
										userSessionBean.getTopMenuList().add(moduleDetails);
										moduleDetails=  null;
									}
									break;
								}
								modDetails = null;
							}
						}
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("ENGINE"))
				{
					performNavigationOperation(userSessionBean.getEngineModulesList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getEngineModulesList() && userSessionBean.getEngineModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getEngineModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}

				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("MISSION"))
				{
					performNavigationOperation(userSessionBean.getMissionModulesList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getMissionModulesList() && userSessionBean.getMissionModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getMissionModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("VEHICLE_TYPE"))
				{
					performNavigationOperation(userSessionBean.getVehilceTypeModulesList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getVehilceTypeModulesList() && userSessionBean.getVehilceTypeModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getVehilceTypeModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("SST_MAINTENANCE"))
				{
					performNavigationOperation(userSessionBean.getSstMaintenanceModulesList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getSstMaintenanceModulesList() && userSessionBean.getSstMaintenanceModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getSstMaintenanceModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("SST_VEHICLE_MAINTENANCE"))
				{
					performNavigationOperation(userSessionBean.getSstVehicleTypeModulesList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getSstVehicleTypeModulesList() && userSessionBean.getSstVehicleTypeModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getSstVehicleTypeModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("CD_CREATION"))
				{
					performNavigationOperationForCDCreation(userSessionBean.getCdCreationModulesList(), userSessionBean, request);
					/*
					 * REDIRECT TO CD ROM PAGE
					 */
					if(null!=userSessionBean.getCdCreationModulesList() && userSessionBean.getCdCreationModulesList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getCdCreationModulesList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}
				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("RUMVIN"))
				{
					performNavigationOperation(userSessionBean.getRumVinMappingList(), userSessionBean, request);
					/*
					 * GET THE URL AT 0 INDEX
					 */
					if(null!=userSessionBean.getRumVinMappingList() && userSessionBean.getRumVinMappingList().size()>0)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getRumVinMappingList().get(0);
						redirectToMDMPages =request.getContextPath()+modDetails.getModulePath();
						modDetails=  null;
					}

				}
				else if(request.getParameter("DASH_TILE_CLICKED_VAL").equals("SETTINGS"))
				{
					/*
					 * ADD TOP MENU 
					 * AND THEN ADD SETTINGS
					 */
					performNavigationOperation(null, userSessionBean, request);
					// ADD ACCESS MANAGEMENT
					ModuleDetails moduleDetails = new ModuleDetails();
					moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT);
					moduleDetails.setModulePath("/accessmanagement");
					moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
					userSessionBean.getTopMenuList().add(moduleDetails);
					/*
					 * 	UPDATE USER BEAN IN SESSION OBJECT
					 */
					request.getSession().removeAttribute("userAccessBean");
					request.getSession().setAttribute("userAccessBean", userSessionBean);

					redirectToMDMPages =request.getContextPath()+moduleDetails.getModulePath();
					moduleDetails = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Dashboard.class.getName(), "doPost()", e);
		}

		if(null!=redirectToMDMPages && !"".equals(redirectToMDMPages))
		{
			response.sendRedirect(redirectToMDMPages);
		}
		else
		{
			if(useReqDis==true)
			{
				/*
				 * CHECK HERE IF USER ALL MODULES LIST IS NULL - THEN USER DOES NOT HAVE ACCESS TO MDM APPLICATION
				 */
				if(showNoAccess==true)
				{
					// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
					response.sendRedirect(request.getContextPath() + "/noaccess");
				}
				else
				{
					setDashboardCategories(request);
					RequestDispatcher rs = request.getRequestDispatcher("/jsps/dashboard.jsp");
					rs.forward(request, response);
				}
			}
		}
		redirectToMDMPages = null;
	}

	/**
	 * GROUPS THE TILES THE USER MAY SEE INTO THE CATEGORY TABS RENDERED BY dashboard.jsp AND PUTS
	 * THEM ON THE REQUEST.
	 *
	 * THE TILES THEMSELVES ARE STILL DECIDED BY AccessManagementFilter - THIS ONLY ARRANGES THEM,
	 * SO NO ACCESS RULE IS AFFECTED. IT IS CALLED IMMEDIATELY BEFORE EACH FORWARD SO THE PAGE
	 * ALWAYS SEES THE TILE LIST AS IT STANDS AFTER THE NAVIGATION WORK ABOVE.
	 */
	private void setDashboardCategories(HttpServletRequest request)
	{
		try
		{
			ArrayList<ModuleDetails> displayTilesList = null;
			boolean superAdminUser = false;
			Object beanObj = request.getSession().getAttribute("userAccessBean");
			if(null!=beanObj && beanObj instanceof UserAccessBean)
			{
				displayTilesList = ((UserAccessBean)beanObj).getDisplayTilesList();
				superAdminUser = ((UserAccessBean)beanObj).isSuperAdminUser();
			}
			beanObj = null;
			if(null==msgProps)
			{
				msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			}

			ArrayList<DashboardCategoryDetails> categoriesList =
					DashboardCategories.groupTiles(displayTilesList, msgProps, superAdminUser);
			request.setAttribute("DASHBOARD_CATEGORIES", categoriesList);

			/*
			 * RE OPEN THE TAB THE USER LEFT FROM.
			 *
			 * THE CATEGORY OF THE CLICKED TILE IS REMEMBERED IN THE SESSION WHEN THE TILE IS
			 * POSTED, SO COMING BACK TO MY PAGE - BY THE TOP BAR LINK OR ANY OTHER ROUTE - LANDS
			 * ON THE SAME TAB INSTEAD OF THE FIRST ONE. AN UNKNOWN OR MISSING VALUE FALLS BACK TO
			 * THE FIRST TAB, WHICH IS ALSO WHAT A FRESH LOGIN GETS.
			 */
			int activeIndex = 0;
			Object rememberedTab = request.getSession().getAttribute(SESSION_KEY_DASH_TAB);
			if(null!=rememberedTab && !"".equals(rememberedTab) && null!=categoriesList)
			{
				for(int i=0; i<categoriesList.size(); i++)
				{
					if(String.valueOf(rememberedTab).equals(categoriesList.get(i).getCategoryKey()))
					{
						activeIndex = i;
						break;
					}
				}
			}
			rememberedTab = null;
			request.setAttribute("DASHBOARD_ACTIVE_INDEX", new Integer(activeIndex));

			categoriesList = null;
			displayTilesList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Dashboard.class.getName(), "setDashboardCategories()", e);
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

	private void performNavigationOperation(ArrayList<ModuleDetails> userModulesList, UserAccessBean userSessionBean, HttpServletRequest request)
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
			moduleDetails.setModuleRefkey(moduleRefKey);
			moduleDetails.setModulePath("/mypage");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;
			if(null!=userModulesList && userModulesList.size()>0)
			{
				for(ModuleDetails modDetails : userModulesList)
				{
					userSessionBean.getTopMenuList().add(modDetails);
					modDetails = null;
				}
			}
			/*
			 * 	UPDATE USER BEAN IN SESSION OBJECT
			 */
			request.getSession().removeAttribute("userAccessBean");
			request.getSession().setAttribute("userAccessBean", userSessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Dashboard.class.getName(), "performNavigationOperation()", e);
		}
	}

	private void performNavigationOperationForCDCreation(ArrayList<ModuleDetails> userModulesList, UserAccessBean userSessionBean, HttpServletRequest request)
	{
		try
		{

			/*
			 * PREPARE TOP MENU FOR CD CREATION, SINCE USER HAS ACCESS TO IT, ALL THE TABS OF IT MUST BE ACCESSIBLE.
			 */
			userSessionBean.setTopMenuList(new ArrayList<ModuleDetails>());

			// ADD DASHBOARD
			ModuleDetails moduleDetails = new ModuleDetails();
			moduleDetails.setModuleRefkey(moduleRefKey);
			moduleDetails.setModulePath("/mypage");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash."+ moduleDetails.getModuleRefkey()));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails=  null;

			// ADD SETTINGS PAGE OF CD CREATION - DO NOT ADD SETTINGS
//			moduleDetails= new ModuleDetails();
//			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_CD_CREATION_SETTINGS);
//			moduleDetails.setModulePath("/settings");
//			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash.settings"));
//			userSessionBean.getTopMenuList().add(moduleDetails);
//			moduleDetails= null;

			// ADD CD ROM CREATION PAGE OF CD CREATION
			moduleDetails= new ModuleDetails();
			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_CD_CREATION);
			moduleDetails.setModulePath("/cdrom");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.dash.cdcreation"));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails= null;

			// ADD HISTORY PAGE OF CD CREATION
			moduleDetails= new ModuleDetails();
			moduleDetails.setModuleRefkey(AccessManagementInterface.REF_KEY_CD_CREATION_HISTORY);
			moduleDetails.setModulePath("/history");
			moduleDetails.setModuleDisplayName(msgProps.getProperty("label.history"));
			userSessionBean.getTopMenuList().add(moduleDetails);
			moduleDetails= null;

			/*
			 * 	UPDATE USER BEAN IN SESSION OBJECT
			 */
			request.getSession().removeAttribute("userAccessBean");
			request.getSession().setAttribute("userAccessBean", userSessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Dashboard.class.getName(), "performNavigationOperationForCDCreation()", e);
		}
	}

}
