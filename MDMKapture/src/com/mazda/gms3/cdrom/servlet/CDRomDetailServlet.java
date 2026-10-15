package com.mazda.gms3.cdrom.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.bean.CDRomSearchBean;
import com.mazda.gms3.cdrom.bean.CDRomUtil;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;

/**
 * Servlet implementation class CDRomServlet
 */
public class CDRomDetailServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomDetailServlet.class);
	MessageProperties msgProps = null;
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomDetailServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String wslId = "";
		boolean useReqDis = true;
		try
		{
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) 
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			
			// CREATE SESSION BEAN OBJECT
			CDRomSearchBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false
					&& sessionBean.isShowWriteControls() == false) {
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			// RESET ERROR MESSAGE
			sessionBean.setErrorMessage(null);
			// RESET ACTION CLICKED
			sessionBean.setActionClicked(null);
			// RESET ITEMS LIST
			sessionBean.setItemsList(null);
			
			// DO NOT RESET WHEN NAVIGATED BACK FROM THE 3RD PAGE.
			if(null==request.getParameter("cromsbk") || "".equals(request.getParameter("cromsbk")))
			{
				/*
				 * RESET PAGE 2 VARIABLES
				 */
				sessionBean.setServiceContents(null);
				sessionBean.setEngineWorkshopManuals(null);
				sessionBean.setTransmissionWorkshopManual(null);

				sessionBean.setSelectedServiceContents(null);
				sessionBean.setSelectedEngineWorkshopManuals(null);
				sessionBean.setSelectedTransmissionWorkshopManual(null);

				sessionBean.setApplicableVINList(null);

				/*
				 * IDENTIFY ALL THE APPLICABLE VINS LIST
				 * LOAD SERVICE CONTENTS
				 * LOAD ENGINE TYPE LIST ON THE BASIS OF ENGINE BOOK
				 * LOAD TRANSMISSION TYPE LIST ON THE BASIS OF TRANSMISSION BOOK
				 * CHECK FOR PARAM cdsch IN REQUEST TO LOAD THE CLICKED SEARCH CRITERIA JOB DETAILS
				 */
				String scheduleId=null;
				if(null!=request.getParameter("cdsch") && !"".equals(request.getParameter("cdsch")))
				{
					scheduleId = (String)request.getParameter("cdsch");
					if(null!=scheduleId && !"".equals(scheduleId) && !"0".equals(scheduleId))
					{
						pageOnLoadOperation(sessionBean, scheduleId);
					}
				}
				scheduleId = null;
					
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "doGet()", e);
		}
		
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size() == 1)) 
			{
				response.sendRedirect(request.getContextPath() + "/mypage");
			} 
			else 
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/cdrom/cdromData.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException 
	{
		String wslId = "";
		boolean useReqDis = true;
		String jspOutCome="/jsps/cdrom/cdromData.jsp";
		try
		{
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) 
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			
			// CREATE SESSION BEAN OBJECT
			CDRomSearchBean sessionBean = getSessionBean(request);
			// RESET ERROR MESSAGE
			sessionBean.setErrorMessage(null);
			
			performAccessCheck(sessionBean, request);
			
			if (sessionBean.isShowReadControls() == false && sessionBean.isShowWriteControls() == false) 
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			/*
			 * CALL FUNCTION TO READ PARAMETERS FROM REQUEST
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("BACK_TO_INDEX_CLICKED"))
				{
					jspOutCome = "REDIRECT_BACK_TO_INDEX";
				}
				else if(sessionBean.getActionClicked().equals("NEXT_CLICKED"))
				{
					/*
					 * first validate at Least 1 check Box must be Selected.
					 */
					if(validate(sessionBean))
					{
						/*
						 * PREPARE ITEMS DATA - 
						 * AS WELL PREPARE FINAL RESULTS DATA
						 * SELECTED MANUAL TYPES WITH DOCUMENTS COUNT.
						 */
						prepareDataForResultsPage(sessionBean);
						
						jspOutCome = "REDIRECT_TO_RESULT";
					}
				}
				else if(sessionBean.getActionClicked().equals("REMOVEL_ALL_CLICKED"))
				{
					/*
					 * remove all selected 
					 * service content
					 * engine books
					 * mission books
					 */
					sessionBean.setSelectedServiceContents(new ArrayList<String>());
					sessionBean.setSelectedEngineWorkshopManuals(new ArrayList<String>());
					sessionBean.setSelectedTransmissionWorkshopManual(new ArrayList<String>());
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "doPost()", e);
		}
		
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size() == 1)) 
			{
				response.sendRedirect(request.getContextPath() + "/mypage");
			} 
			else 
			{
				if(null!=jspOutCome && jspOutCome.equals("REDIRECT_BACK_TO_INDEX"))
				{
					response.sendRedirect(request.getContextPath()+"/cdrom?cromibk=true");
				}
				else if(null!=jspOutCome && jspOutCome.equals("REDIRECT_TO_RESULT"))
				{
					response.sendRedirect(request.getContextPath()+"/cdromschedule");
				}
				else
				{
					RequestDispatcher rs = request.getRequestDispatcher(jspOutCome);
					rs.forward(request, response);
				}
			}
		}
	}

	private CDRomSearchBean getSessionBean(HttpServletRequest request) {
		CDRomSearchBean sessionBean = null;
		if (null != request.getSession().getAttribute("CDRomSearchBean")
				&& !"".equals(request.getSession().getAttribute(
						"CDRomSearchBean"))) {
			sessionBean = (CDRomSearchBean) request.getSession().getAttribute(
					"CDRomSearchBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CDRomSearchBean();
			request.getSession().setAttribute("CDRomSearchBean", sessionBean);
		}
		return sessionBean;
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

	private void performAccessCheck(CDRomSearchBean sessionBean,
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
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(),
					"performAccessCheck()", e);
		}
	}

	
	private void readParamsFromRequest(CDRomSearchBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setActionClicked(null);
			sessionBean.setSelectedServiceContents(null);
			sessionBean.setSelectedTransmissionWorkshopManual(null);
			sessionBean.setSelectedEngineWorkshopManuals(null);
			if(null!=request.getParameter("CD_Rom_ActionClicked") && !"".equals(request.getParameter("CD_Rom_ActionClicked")))
			{
				sessionBean.setActionClicked(request.getParameter("CD_Rom_ActionClicked"));
			}
			if(null!=request.getParameter("serviceContentValue") && !"".equals(request.getParameter("serviceContentValue")))
			{
				String sel = (String)request.getParameter("serviceContentValue");
				if(null!=sel && !"".equals(sel))
				{
					sessionBean.setSelectedServiceContents(CDRomUtil.getAsList(sel));
				}
				sel = null;
			}
			if(null!=request.getParameter("engineWorkshopManualValue") && !"".equals(request.getParameter("engineWorkshopManualValue")))
			{
				String sel = (String)request.getParameter("engineWorkshopManualValue");
				if(null!=sel && !"".equals(sel))
				{
					sessionBean.setSelectedEngineWorkshopManuals(CDRomUtil.getAsList(sel));
				}
				sel = null;
			}
			if(null!=request.getParameter("transmissionWorkshopManualValue") && !"".equals(request.getParameter("transmissionWorkshopManualValue")))
			{
				String sel = (String)request.getParameter("transmissionWorkshopManualValue");
				if(null!=sel && !"".equals(sel))
				{
					sessionBean.setSelectedTransmissionWorkshopManual(CDRomUtil.getAsList(sel));
				}
				sel = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "readParamsFromRequest()", e);
		}
	}

	private boolean validate(CDRomSearchBean sessionBean)
	{
		if((null==sessionBean.getSelectedServiceContents() || sessionBean.getSelectedServiceContents().size()<=0)  && 
			(null==sessionBean.getSelectedEngineWorkshopManuals() || sessionBean.getSelectedEngineWorkshopManuals().size()<=0) &&  
			(null==sessionBean.getSelectedTransmissionWorkshopManual() || sessionBean.getSelectedTransmissionWorkshopManual().size()<=0))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.select.checkbox"));
			return false;
		}
		return true;
	}
	
	private void pageOnLoadOperation(CDRomSearchBean sessionBean, String scheduleId)
	{
		try
		{
			ArrayList<CDRomScheduleDetails> schList = CDRomDAO.getSearchCriteriaScheduleDetails(scheduleId,null,null);
			if(null!=schList && schList.size()>0)
			{
				// get FIRST ITEM
				CDRomScheduleDetails schDetails = (CDRomScheduleDetails)schList.get(0);
				// SET REQUIRED VARIABLES IN SESSION BEAN
				sessionBean.setSelectedCountry(String.valueOf(schDetails.getCountryLocalId()));
				sessionBean.setSelectedLanguage(schDetails.getLocaleCode());
				sessionBean.setSelectedModel(schDetails.getCarlineCode());
				sessionBean.setSelectedWmi(schDetails.getVinWmiCode());
				sessionBean.setSelectedVds(schDetails.getVinVdsCode());
				sessionBean.setSelectedVinRange(schDetails.getVinStartRange());
				
				/*
				 * IDENTIFY SELECTED BOOK CODES
				 */
				List<String> selEngBookCodesList = new ArrayList<String>();
				List<String> selMissionBookCodesList = new ArrayList<String>();
				
				ArrayList<CDRomScheduleItemDetails> itemsList = CDRomDAO.getSearchCriteriaScheduleItemDetails(scheduleId);
				if(null!=itemsList && itemsList.size()>0)
				{
					CDRomScheduleItemDetails itemDetails = null;
					for(int a=0;a<itemsList.size();a++)
					{
						itemDetails= (CDRomScheduleItemDetails)itemsList.get(a);
						if(null!=itemDetails.getManualTypeCode() && !"".equals(itemDetails.getManualTypeCode()))
						{
							if(null!=itemDetails.getManualType() && itemDetails.getManualType().toLowerCase().trim().equals("EngineManuals".toLowerCase()))
							{
								// add to engine books list
								selEngBookCodesList.add(itemDetails.getManualTypeCode());
							}
							else 
							{
								// add to mission books list
								selMissionBookCodesList.add(itemDetails.getManualTypeCode());
							}
						}
						itemDetails = null;
					}
				}
				itemsList=  null;
				schDetails = null;
				
				
				// IDENTIFY APPLICABLE VIN LIST
//				identifyApplicableVINList(sessionBean);
				
				// IDENTIFY ALL THE MANUAL TYPES
				identifyServiceManualTypes(sessionBean);
				
				// IDENTIFY ALL THE ENGINE BOOKS ON THE BASIS OF ENGINE BOOKS
				identifyEngineBookManualTypes(sessionBean, selEngBookCodesList);
				
				// IDENITFY ALL THE TRANSMISSION BOOKS ON THE BASIS OF ENGINE BOOKS
				identifyTransmissionBookManualTypes(sessionBean, selMissionBookCodesList);
				
				selEngBookCodesList=null;
				selMissionBookCodesList=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "pageOnLoadOperation()", e);
		}
	}

	private void identifyServiceManualTypes(CDRomSearchBean sessionBean)
	{
		try
		{
			sessionBean.setServiceContents(new ArrayList<LabelBean>());
			/*
			 * IDENTIFY ALL THE SERVICE CONTENT TYPES ON THE BASIS OF SELECTED LOCALE & ONLY DEFAULT VIN. 
			 * SEGREGATE CARLINE CODE AND MODEL NAME FROM SELECTED MODEL
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
				{
					carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
					modelName = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
				}
			}
			catch(Exception e){}
			
			List<LabelBean> list = CDRomDAO.getServiceContentList(sessionBean.getSelectedLanguage(), modelName,carlineCode,
					sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange());
			if(null!=list && list.size()>0)
			{
				sessionBean.setServiceContents(list);
				/*
				 * set all service content as selected on load
				 * DATE CHANGE - 26 - MARCH 2022
				 * NEW REQUEST FROM MC
				 */
				sessionBean.setSelectedServiceContents(new ArrayList<String>());
				LabelBean lb = null;
				for(int a=0;a<sessionBean.getServiceContents().size();a++)
				{
					lb = (LabelBean)sessionBean.getServiceContents().get(a);
					if(null!=lb.getKey() && !"".equals(lb.getKey()))
					{
						sessionBean.getSelectedServiceContents().add(lb.getKey());
					}
					lb = null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "identifyServiceManualTypes()", e);
		}
	}

	private void identifyEngineBookManualTypes(CDRomSearchBean sessionBean, List<String> selBookCodes)
	{
		try
		{
			sessionBean.setEngineWorkshopManuals(new ArrayList<LabelBean>());

			/*
			 * IDENTIFY ALL THE SERVICE CONTENT TYPES ON THE BASIS OF SELECTED LOCALE & SELECTED VIN
			 * SEGREGATE CARLINE CODE AND MODEL NAME FROM SELECTED MODEL
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
				{
					carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
					modelName = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
				}
			}
			catch(Exception e){}

//			List<LabelBean> list = CDRomDAO.getEngineWorkshopManualList(sessionBean.getSelectedLanguage(), sessionBean.getApplicableVINList());
			List<LabelBean> list = CDRomDAO.getEngineWorkshopManualList(sessionBean.getSelectedLanguage(), modelName,carlineCode,
					sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange());
			if(null!=list && list.size()>0)
			{
				sessionBean.setEngineWorkshopManuals(list);
				/*
				 * set all engine books as selected on load
				 * DATE CHANGE - 26 - MARCH 2022
				 * NEW REQUEST FROM MC
				 */
				if(null!=selBookCodes && selBookCodes.size()>0)
				{
					/*
					 * CHECK IF THE BOOK EXISTS IN MASTER LIST
					 * IF YES - THEN ADD TO SELECTED BOOK LIST
					 */
					LabelBean lb = null;
					for(int a=0;a<list.size();a++)
					{
						lb = (LabelBean)list.get(a);
						for(int b=0;b<selBookCodes.size();b++)
						{
							if(lb.getKey().toLowerCase().equals(selBookCodes.get(b).toLowerCase()))
							{
								// exists - proceed for adding
								boolean add = true;
								if(null!=sessionBean.getSelectedEngineWorkshopManuals() && sessionBean.getSelectedEngineWorkshopManuals().size()>0)
								{
									for(int c=0;c<sessionBean.getSelectedEngineWorkshopManuals().size();c++)
									{
										if(sessionBean.getSelectedEngineWorkshopManuals().get(c).toLowerCase().equals(selBookCodes.get(b).toLowerCase()))
										{
											// already added
											add = false;
											break;
										}
									}
								}
								
								if(add==true)
								{
									if(null==sessionBean.getSelectedEngineWorkshopManuals() || sessionBean.getSelectedEngineWorkshopManuals().size()<=0)
									{
										sessionBean.setSelectedEngineWorkshopManuals(new ArrayList<String>());
									}
									sessionBean.getSelectedEngineWorkshopManuals().add(selBookCodes.get(b));
								}
								break;
							}
						}
						lb = null;
					}
				}
				selBookCodes = null;
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "identifyEngineBookManualTypes()", e);
		}
	}
	
	private void identifyTransmissionBookManualTypes(CDRomSearchBean sessionBean, List<String> selBookCodes)
	{
		try
		{
			sessionBean.setTransmissionWorkshopManual(new ArrayList<LabelBean>());
			
			/*
			 * IDENTIFY ALL THE SERVICE CONTENT TYPES ON THE BASIS OF SELECTED LOCALE & APPLICABLE VINS 
			 * SEGREGATE CARLINE CODE AND MODEL NAME FROM SELECTED MODEL
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
				{
					carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
					modelName = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
				}
			}
			catch(Exception e){}
			
//			List<LabelBean> list = CDRomDAO.getTransmissionWorkshopManual(sessionBean.getSelectedLanguage(), sessionBean.getApplicableVINList());
			List<LabelBean> list = CDRomDAO.getTransmissionWorkshopManual(sessionBean.getSelectedLanguage(), modelName,carlineCode,
					sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange());
			if(null!=list && list.size()>0)
			{
				sessionBean.setTransmissionWorkshopManual(list);
				/*
				 * identify selected books for the selected vin
				 * DATE CHANGE - 26 - MARCH 2022
				 * NEW REQUEST FROM MC
				 */
				if(null!=selBookCodes && selBookCodes.size()>0)
				{
					/*
					 * CHECK IF THE BOOK EXISTS IN MASTER LIST
					 * IF YES - THEN ADD TO SELECTED BOOK LIST
					 */
					LabelBean lb = null;
					for(int a=0;a<list.size();a++)
					{
						lb = (LabelBean)list.get(a);
						for(int b=0;b<selBookCodes.size();b++)
						{
							if(lb.getKey().toLowerCase().equals(selBookCodes.get(b).toLowerCase()))
							{
								// exists - proceed for adding
								boolean add = true;
								if(null!=sessionBean.getSelectedTransmissionWorkshopManual() && sessionBean.getSelectedTransmissionWorkshopManual().size()>0)
								{
									for(int c=0;c<sessionBean.getSelectedTransmissionWorkshopManual().size();c++)
									{
										if(sessionBean.getSelectedTransmissionWorkshopManual().get(c).toLowerCase().equals(selBookCodes.get(b).toLowerCase()))
										{
											// already added
											add = false;
											break;
										}
									}
								}
								
								if(add==true)
								{
									if(null==sessionBean.getSelectedTransmissionWorkshopManual() || sessionBean.getSelectedTransmissionWorkshopManual().size()<=0)
									{
										sessionBean.setSelectedTransmissionWorkshopManual(new ArrayList<String>());
									}
									sessionBean.getSelectedTransmissionWorkshopManual().add(selBookCodes.get(b));
								}
								break;
							}
						}
						lb = null;
					}
				}
				selBookCodes = null;				
				
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "identifyTransmissionBookManualTypes()", e);
		}
	}
	
	private void prepareDataForResultsPage(CDRomSearchBean sessionBean)
	{
		sessionBean.setItemsList(new ArrayList<CDRomScheduleItemDetails>());
		try
		{
			ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
			/*
			 * IDENTIFY DISTINCT DOCUMENTS COUNT FOR EACH SELECTED MANUAL TYPE ON THE BASIS OF APPLICABLE LIST
			 */
			String carlineCode="";
			String modelName="";
			try
			{
				if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
				{
					carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
					modelName = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
				}
			}
			catch(Exception e){}
			
			// SERVICE MANUAL TYPES
			if(null!=sessionBean.getSelectedServiceContents() && sessionBean.getSelectedServiceContents().size()>0)
			{
				String refKey="";
				CDRomScheduleItemDetails itemDetails = null;
				for(String serviceContent : sessionBean.getSelectedServiceContents())
				{
					itemDetails = new CDRomScheduleItemDetails();
					itemDetails.setManualTypeCode(serviceContent);
					itemDetails.setManualType("ServiceManuals");
					itemDetails.setManualTypeDisplayLabel("Service Content");
					if(null!=sessionBean.getServiceContents())
					{
						for(LabelBean lb : sessionBean.getServiceContents())
						{
							if(lb.getKey().trim().toLowerCase().equals(serviceContent.trim().toLowerCase()))
							{
								itemDetails.setManualTypeName(lb.getValue());
								// USE EXTRA ATTRIBUTE TO IDENITFY MANUAL TYPE REF KEY
								refKey=  lb.getExtraAttribute();
								break;
							}
							lb = null;
						}
					}
					
					long itemDocsCount=0;
					ArrayList<String> docListForManualType = new ArrayList<String>();
					if(null!=itemDetails.getManualTypeCode() && !"".equals(itemDetails.getManualTypeCode()) && 
							null!=itemDetails.getManualTypeName() && !"".equals(itemDetails.getManualTypeName())
							&& null!=refKey && !"".equals(refKey))
					{
						/*
						 * FETCH ALL CD ROM DOCUMENT DATA ON THE BASIS OF FACELIFT FOLDER.
						 */
						ArrayList<String> tempDocsList = CDRomDAO.fetchCDRomDataOnFaceLift(refKey, itemDetails.getManualTypeName(), modelName, carlineCode, sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange(),sessionBean.getSelectedLanguage());
						if(null!=tempDocsList && tempDocsList.size()>0)
						{
							for(String tempDocId : tempDocsList)
							{
								boolean addToList = true;
								if(null!=docListForManualType && docListForManualType.size()>0)
								{
									for(String exist : docListForManualType)
									{
										if(exist.trim().toLowerCase().equals(tempDocId.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToList = false;
											break;
										}
										exist = null;
									}
								}
								
								if(addToList == true)
								{
									// INCREMENT ITEM DOCS COUNT
									itemDocsCount++;
									docListForManualType.add(tempDocId);
								}
								tempDocId = null;
							}
						}
						tempDocsList = null;
					}
					
					if(null!=docListForManualType && docListForManualType.size()>0)
					{
						// SET DOCUMENT IDS LIST IN ITEM DETAILS AS WELL.
						itemDetails.setDocumentIdsList(docListForManualType);
					}
					docListForManualType = null;
					
					if(itemDocsCount > 0 )
					{
						itemDetails.setTotalDocsForProcessing(itemDocsCount);
						logger.info("prepareDataForResultsPage :: Total Documents Found for {"+itemDetails.getManualTypeCode()+"} :: are :: > " + itemDocsCount);
					}
					else
					{
						itemDetails.setTotalDocsForProcessing(0);
						logger.info("prepareDataForResultsPage :: No Documents Found for {"+itemDetails.getManualTypeCode()+"}.");
					}
					
					// add itemDetails to itemsList
					itemsList.add(itemDetails);
					
					itemDetails = null;
					serviceContent = null;
				}
			}
			
			// ENGINE MANUAL TYPES
			if(null!=sessionBean.getSelectedEngineWorkshopManuals() && sessionBean.getSelectedEngineWorkshopManuals().size()>0)
			{
				for(String engineManualContent : sessionBean.getSelectedEngineWorkshopManuals())
				{
					CDRomScheduleItemDetails itemDetails = new CDRomScheduleItemDetails();
					itemDetails.setManualTypeCode(engineManualContent);
					itemDetails.setManualType("EngineManuals");
					itemDetails.setManualTypeDisplayLabel("Engine Manuals");
					if(null!=sessionBean.getEngineWorkshopManuals())
					{
						for(LabelBean lb : sessionBean.getEngineWorkshopManuals())
						{
							if(lb.getKey().trim().toLowerCase().equals(engineManualContent.trim().toLowerCase()))
							{
								itemDetails.setManualTypeName(lb.getValue());
								break;
							}
							lb = null;
						}
					}
					
					
					long itemDocsCount=0;
					ArrayList<String> docListForManualType = new ArrayList<String>();
					if(null!=engineManualContent && !"".equals(engineManualContent))
					{
						/*
						 * FETCH ALL CD ROM DOCUMENT DATA ON THE BASIS OF BOOK CODE & LOCALE
						 * IN-191220-2968
						 * FETCH ALL DOCUMENTS FROM VIEW CONTENT TABLE BASED ON BOOK CODE, MODEL, CARLINE CODE
						 */
//						ArrayList<String> tempDocsList = CDRomDAO.fetchCDRomDataOnBookCodes(engineManualContent, "ENGINE", modelName, carlineCode, sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange());
						ArrayList<String> tempDocsList = CDRomDAO.fetchCDRomDataOnBookCodes(engineManualContent, "ENGINE", modelName, carlineCode,sessionBean.getSelectedLanguage());
						if(null!=tempDocsList && tempDocsList.size()>0)
						{
							for(String tempDocId : tempDocsList)
							{
								boolean addToList = true;
								if(null!=docListForManualType && docListForManualType.size()>0)
								{
									for(String exist : docListForManualType)
									{
										if(exist.trim().toLowerCase().equals(tempDocId.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToList = false;
											break;
										}
										exist = null;
									}
								}
								
								if(addToList == true)
								{
									// INCREMENT ITEM DOCS COUNT
									itemDocsCount++;
									docListForManualType.add(tempDocId);
								}
								tempDocId = null;
							}
						}
						tempDocsList = null;
					}
					
					if(null!=docListForManualType && docListForManualType.size()>0)
					{
						// SET DOCUMENT IDS LIST IN ITEM DETAILS AS WELL.
						itemDetails.setDocumentIdsList(docListForManualType);
					}
					docListForManualType = null;
					
					if(itemDocsCount > 0 )
					{
						itemDetails.setTotalDocsForProcessing(itemDocsCount);
						logger.info("prepareDataForResultsPage :: Total Documents Found for {"+itemDetails.getManualTypeCode()+"} :: are :: > " + itemDocsCount);
					}
					else
					{
						itemDetails.setTotalDocsForProcessing(0);
						logger.info("prepareDataForResultsPage :: No Documents Found for {"+itemDetails.getManualTypeCode()+"}.");
					}
					// add itemDetails to itemsList
					itemsList.add(itemDetails);
					itemDetails = null;
					engineManualContent = null;
				}
			}
			
			

			// FOR TRANSMISSION MANUAL TYPES
			if(null!=sessionBean.getSelectedTransmissionWorkshopManual() && sessionBean.getSelectedTransmissionWorkshopManual().size()>0)
			{
				for(String transmissionManualContent : sessionBean.getSelectedTransmissionWorkshopManual())
				{
					CDRomScheduleItemDetails itemDetails = new CDRomScheduleItemDetails();
					itemDetails.setManualTypeCode(transmissionManualContent);
					itemDetails.setManualType("TransmissionManuals");
					itemDetails.setManualTypeDisplayLabel("Transmission Manuals");
					if(null!=sessionBean.getTransmissionWorkshopManual())
					{
						for(LabelBean lb : sessionBean.getTransmissionWorkshopManual())
						{
							if(lb.getKey().trim().toLowerCase().equals(transmissionManualContent.trim().toLowerCase()))
							{
								itemDetails.setManualTypeName(lb.getValue());
								break;
							}
							lb = null;
						}
					}
					
					long itemDocsCount=0;
					ArrayList<String> docListForManualType = new ArrayList<String>();
					if(null!=transmissionManualContent && !"".equals(transmissionManualContent))
					{
						/*
						 * FETCH ALL CD ROM DOCUMENT DATA ON THE BASIS OF BOOK CODE & LOCALE
						 * IN-191220-2968
						 * FETCH ALL DOCUMENTS FROM VIEW CONTENT TABLE BASED ON BOOK CODE, MODEL, CARLINE CODE
						 */
//						ArrayList<String> tempDocsList = CDRomDAO.fetchCDRomDataOnBookCodes(transmissionManualContent, "MISSION", modelName, carlineCode, sessionBean.getSelectedWmi(), sessionBean.getSelectedVds(), sessionBean.getSelectedVinRange());
						ArrayList<String> tempDocsList = CDRomDAO.fetchCDRomDataOnBookCodes(transmissionManualContent, "MISSION", modelName, carlineCode,sessionBean.getSelectedLanguage());
						if(null!=tempDocsList && tempDocsList.size()>0)
						{
							for(String tempDocId : tempDocsList)
							{
								boolean addToList = true;
								if(null!=docListForManualType && docListForManualType.size()>0)
								{
									for(String exist : docListForManualType)
									{
										if(exist.trim().toLowerCase().equals(tempDocId.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToList = false;
											break;
										}
										exist = null;
									}
								}
								
								if(addToList == true)
								{
									// INCREMENT ITEM DOCS COUNT
									itemDocsCount++;
									docListForManualType.add(tempDocId);
								}
								tempDocId = null;
							}
						}
						tempDocsList = null;
					}
					
					if(null!=docListForManualType && docListForManualType.size()>0)
					{
						// SET DOCUMENT IDS LIST IN ITEM DETAILS AS WELL.
						itemDetails.setDocumentIdsList(docListForManualType);
					}
					docListForManualType = null;
					
					if(itemDocsCount > 0 )
					{
						itemDetails.setTotalDocsForProcessing(itemDocsCount);
						logger.info("prepareDataForResultsPage :: Total Documents Found for {"+itemDetails.getManualTypeCode()+"} :: are :: > " + itemDocsCount);
					}
					else
					{
						itemDetails.setTotalDocsForProcessing(0);
						logger.info("prepareDataForResultsPage :: No Documents Found for {"+itemDetails.getManualTypeCode()+"}.");
					}
					
					// add itemDetails to itemsList
					itemsList.add(itemDetails);
					
					itemDetails = null;
					transmissionManualContent = null;
				}
			}
			
			if(null!=itemsList && itemsList.size()>0)
			{
				sessionBean.setItemsList(itemsList);
			}
			itemsList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomDetailServlet.class.getName(), "prepareDataForResultsPage()", e);
		}
		
	}

}

