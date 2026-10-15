package com.mazda.gms3.mdm.nmdtool.servlet;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.nmdtool.bean.NMDScheduleSessionBean;
import com.mazda.gms3.mdm.nmdtool.dao.NMDScheduleDAO;
import com.mazda.gms3.mdm.nmdtool.vo.NMDScheduleDetails;
import com.mazda.gms3.mdm.rmitool.servlet.RMIToolServlet;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class NMDScheduleTransactions
 */
public class NMDScheduleTransactions extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger logger = LogManager.getLogger(NMDScheduleTransactions.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL;

       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NMDScheduleTransactions() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			NMDScheduleSessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setActionClicked(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDayOfMonth(null);
			sessionBean.setTimeOfMonth(null);
			sessionBean.setDaysList(null);
			sessionBean.setHoursList(null);
			sessionBean.setSchedulesList(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDeltaLastExecTime(null);
			sessionBean.setConvDeltaLastExecTime(null);
			
			/*
			 * call function to load all the Month Days Data
			 */
			getDaysList(sessionBean);
			
			/*
			 * call function to load all the Hours Data
			 */
			getHoursList(sessionBean);
			
			/*
			 * call function to load Models List
			 */
			getScheduleList(sessionBean);

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIToolServlet.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/nmdtool/schedule.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		String jspPath = "/jsps/nmdtool/schedule.jsp";
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
			NMDScheduleSessionBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageLength(null);

			/*
			 * call function to read parametrs from request
			 */
			readParametersFromRequest(sessionBean, request);

			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("SUBMIT"))
				{
					createSchedule(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("RUN_ONDEMAND"))
				{
					runOnDemandOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("RESET_FULL_EXTRACT_LAST_EXEC_TIME"))
				{
					resetLastExectionTimeOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_DELTA_EXTRACT_LAST_EXEC_TIME"))
				{
					updateDeltaLastExectionTimeOperation(sessionBean);
				}
			}
			sessionBean.setActionClicked(null);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIToolServlet.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher(jspPath);
				rs.forward(request, response);
			}
		}
	}
	
	private NMDScheduleSessionBean getSessionBean(HttpServletRequest request) 
	{
		NMDScheduleSessionBean sessionBean = null;
		if (null != request.getSession().getAttribute("nmdScheduleBean") && !"".equals(request.getSession().getAttribute("nmdScheduleBean"))) 
		{
			sessionBean = (NMDScheduleSessionBean) request.getSession().getAttribute("nmdScheduleBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new NMDScheduleSessionBean();
			request.getSession().setAttribute("nmdScheduleBean", sessionBean);
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

	private void performAccessCheck(NMDScheduleSessionBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "performAccessCheck()", e);
		}
	}

	private static void getDaysList(NMDScheduleSessionBean sessionBean)
	{
		sessionBean.setDaysList(new ArrayList<SelectItemDetails>());
		try
		{
			for(int a=1;a<=31;a++)
			{
				SelectItemDetails si  =new SelectItemDetails();
				si.setLabel(String.valueOf(a));
				si.setValue(String.valueOf(a));
				sessionBean.getDaysList().add(si);
				si = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "getDaysList()", e);
		}
	}
	
	private static void getHoursList(NMDScheduleSessionBean sessionBean)
	{
		sessionBean.setHoursList(new ArrayList<SelectItemDetails>());
		try
		{
			SelectItemDetails si  =null;
			for(int a=0;a<=23;a++)
			{
				if(a<10)
				{
					si  =new SelectItemDetails();
					si.setLabel("0"+a+":00");
					si.setValue(String.valueOf(a));
					sessionBean.getHoursList().add(si);
					si = null;
				}
				else
				{
					si  =new SelectItemDetails();
					si.setLabel(a+":00");
					si.setValue(String.valueOf(a));
					sessionBean.getHoursList().add(si);
					si = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "getHoursList()", e);
		}
	}
	
	private static void getScheduleList(NMDScheduleSessionBean sessionBean)
	{
		sessionBean.setSchedulesList(new ArrayList<NMDScheduleDetails>());
		try
		{
			String locale = ApplicationProperties.getProperty("en_us");
			// REPLACE _ BY -
			locale = locale.replace("_", "-");
			List<NMDScheduleDetails> tempList = NMDScheduleDAO.getModelDetails(locale, wslId);
			if(null!=tempList && tempList.size()>0)
			{
				NMDScheduleDetails cd = null;
				int count=0;
				for(int a=0;a<tempList.size();a++)
				{
					cd= (NMDScheduleDetails)tempList.get(a);
					// increment count
					count++;
					cd.setSrNo(count);
					// add to sessionBean
					sessionBean.getSchedulesList().add(cd);
					cd = null;
				}
				count=0;
			}
			tempList = null;
			locale = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "getScheduleList()", e);
		}
	}
	
	private void readParametersFromRequest(NMDScheduleSessionBean sessionBean, HttpServletRequest request) 
	{
		try
		{
			sessionBean.setActionClicked(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDayOfMonth(null);
			sessionBean.setTimeOfMonth(null);
			sessionBean.setDeltaLastExecTime(null);
			sessionBean.setConvDeltaLastExecTime(null);

			if (null!=request.getParameter("NMDTOOL_DataTabel_displayPageNo") && !"".equals(request.getParameter("NMDTOOL_DataTabel_displayPageNo"))) 
			{
				// set the value in sessionBean.setDisplayPageNo
				sessionBean.setDisplayPageNo((String)request.getParameter("NMDTOOL_DataTabel_displayPageNo"));
			}

			if(null!=request.getParameter("NMDTOOL_DataTabel_displayPageLen") && !"".equals(request.getParameter("NMDTOOL_DataTabel_displayPageLen")))
			{
				// set the value in sessionBean.setDisplayPageLength
				sessionBean.setDisplayPageLength((String)request.getParameter("NMDTOOL_DataTabel_displayPageLen"));
			}

			if(null!=request.getParameterValues("NMDTOOL_DayOfMonth") && !"".equals(request.getParameter("NMDTOOL_DayOfMonth")))
			{
				sessionBean.setDayOfMonth((String)request.getParameter("NMDTOOL_DayOfMonth"));
			}
			
			if(null!=request.getParameterValues("NMDTOOL_HourOfMonth") && !"".equals(request.getParameter("NMDTOOL_HourOfMonth")))
			{
				sessionBean.setTimeOfMonth((String)request.getParameter("NMDTOOL_HourOfMonth"));
			}
			if(null!=request.getParameter("NMDTOOL_ActionClicked") && !"".equals(request.getParameter("NMDTOOL_ActionClicked")))
			{
				sessionBean.setActionClicked((String)request.getParameter("NMDTOOL_ActionClicked"));
			}
			if(null!=request.getParameter("NMDTOOL_SelectedRows") && !"".equals(request.getParameter("NMDTOOL_SelectedRows")))
			{
				sessionBean.setSelectedRows((String)request.getParameter("NMDTOOL_SelectedRows"));
				if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
				{
					if(sessionBean.getSelectedRows().endsWith(","))
					{
						sessionBean.setSelectedRows(sessionBean.getSelectedRows().substring(0, sessionBean.getSelectedRows().length()-1));
					}
				}
			}
			
			// show Selected Rows in Data Table
			if(null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
			{
				NMDScheduleDetails schDetails =null;
				for(int a=0;a<sessionBean.getSchedulesList().size();a++)
				{
					schDetails= (NMDScheduleDetails)sessionBean.getSchedulesList().get(a);
					// set selected to false default value
					schDetails.setSelected(false);
					if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
					{
						String[] tok = sessionBean.getSelectedRows().split(",");
						if(null!=tok && tok.length>0)
						{
							for(int b=0;b<tok.length;b++)
							{
								if(String.valueOf(schDetails.getSrNo()).equals(tok[b].toString()))
								{
									// set selected to true
									schDetails.setSelected(true);
									break;
								}
							}
						}
						tok = null;
					}
					
					schDetails = null;
				}
				schDetails = null;
			}
			
			
			if(null!=request.getParameter("NMDTOOL_DeltaLastExecTime") && !"".equals(request.getParameter("NMDTOOL_DeltaLastExecTime")))
			{
				sessionBean.setDeltaLastExecTime((String)request.getParameter("NMDTOOL_DeltaLastExecTime"));
				try
				{
					// 2026/02/16 14:39
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm");
					Date convDate = sdf.parse(sessionBean.getDeltaLastExecTime());
					sessionBean.setConvDeltaLastExecTime(new Timestamp(convDate.getTime()));
					convDate = null;
					sdf = null;
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "readParametersFromRequest()", e);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "readParametersFromRequest()", e);
		}
	}
	
	private boolean validate(NMDScheduleSessionBean sessionBean)
	{
		if(null==sessionBean.getDayOfMonth() || "".equals(sessionBean.getDayOfMonth()) || null==sessionBean.getTimeOfMonth() || "".equals(sessionBean.getTimeOfMonth()))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			return false;
		}
		// check all running Jobs - if any running then do not allow operation
		List<NMDScheduleDetails> runningJobs = NMDScheduleDAO.getRunningScheduleList(null);
		if(null!=runningJobs && runningJobs.size()>0)
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.data.ext.jobs.running.auto.schedule"));
			return false;
		}
		runningJobs = null;
		return true;
	}
	
	private void createSchedule(NMDScheduleSessionBean sessionBean)
	{
		try
		{
			if(validate(sessionBean))
			{
				if(null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
				{
					NMDScheduleDetails cd = null;
					// update List with Day & Time of Month
					for(int a=0;a<sessionBean.getSchedulesList().size();a++)
					{
						cd = (NMDScheduleDetails)sessionBean.getSchedulesList().get(a);
						cd.setDayOfMonth(sessionBean.getDayOfMonth());
						cd.setTimeofMonth(sessionBean.getTimeOfMonth());
						cd.setWslId(wslId);
						cd = null;
					}
					
					/*
					 * proceed for saving
					 */
					boolean bool = NMDScheduleDAO.createModelsSchedule(sessionBean.getSchedulesList());
					if(bool==true)
					{
						logger.info("createSchedule() :: Models Schedule Created / Updated Successfully.");
						sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.schedule")));
						/*
						 * refresh scheduleList
						 */
						getScheduleList(sessionBean);
						// reset Input fields
						sessionBean.setDayOfMonth(null);
						sessionBean.setTimeOfMonth(null);
						sessionBean.setSelectedRows(null);
						sessionBean.setDeltaLastExecTime(null);
						sessionBean.setConvDeltaLastExecTime(null);
					}
					else
					{
						logger.info("createSchedule() :: Failed to Create / Update Models Schedule."); 
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "createSchedule()", e);
		}
	}
	
	private void runOnDemandOperation(NMDScheduleSessionBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()) 
					&& null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
			{
				String[] tok = sessionBean.getSelectedRows().split(",");
				if(null!=tok && tok.length>0)
				{
					NMDScheduleDetails details = null;
					List<String> schIdList=null;
					String keyToCheck=null;
					for(int b=0;b<tok.length;b++)
					{
						details = null;
						for(int a=0;a<sessionBean.getSchedulesList().size();a++)
						{
							details = (NMDScheduleDetails)sessionBean.getSchedulesList().get(a);
							if(details.getScheduleId()>0)
							{
								if(tok[b].toString().equals(String.valueOf(details.getSrNo())))
								{
									if(null==keyToCheck)
									{
										keyToCheck="";
									}
									keyToCheck+=String.valueOf(details.getScheduleId())+",";
									if(null==schIdList || schIdList.size()<=0)
									{
										schIdList = new ArrayList<String>();
									}
									schIdList.add(String.valueOf(details.getScheduleId()));
									break;
								}
							}
							details= null;
						}
					}
					
					if(null!=keyToCheck && !"".equals(keyToCheck))
					{
						if(keyToCheck.endsWith(","))
						{
							keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
						}
						/*
						 * CHECK IF ANY DATA EXTRACTION JOBS RUNNING FOR THE SELECTED MODELS
						 */
						List<NMDScheduleDetails> runningList = NMDScheduleDAO.getRunningScheduleList(keyToCheck);
						boolean proceedFurther=true;
						String runningModels=null;
						if(null!=runningList && runningList.size()>0)
						{
							NMDScheduleDetails cd = null;
							for(int c=0;c<runningList.size();c++)
							{
								cd = (NMDScheduleDetails)runningList.get(c);
								proceedFurther=false;
								if(null==runningModels)
								{
									runningModels="";
								}
								runningModels+=cd.getModel();
								if(c!=runningList.size()-1)
								{
									runningModels+=",";
								}
								cd = null;
							}
							cd = null;
						}
						
						if(proceedFurther==false)
						{
							if(null!=runningModels && !"".equals(runningModels))
							{
								if(runningModels.endsWith(","))
								{
									runningModels = runningModels.substring(0, runningModels.length()-1);
								}
							}
							// error.data.ext.jobs.running.other.ops.schedule
							sessionBean.setErrorMessage(msgProps.addMessage("error.data.ext.jobs.running.other.ops.schedule", runningModels, msgProps.getProperty("label.runondemand")));
						
						}
						else if(proceedFurther==true)
						{
							if(null!=schIdList && schIdList.size()>0)
							{
								boolean bool = NMDScheduleDAO.createOnDemandSchedule(schIdList);
								if(bool==true)
								{
									sessionBean.setSelectedRows(null);
									sessionBean.setSuccessMessage(msgProps.getProperty("extraction.start.success"));
								}
								else
								{
									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
									logger.info("runOnDemandOperation :: Failed to Create Run On Demand Schedules for Selected Schedules.");
								}
							}
							else
							{
								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
								logger.info("runOnDemandOperation :: Failed to Identify Schedule Ids for Run On Demand Operation.");
							}
						}
						runningList =null;
						runningModels = null;
					}
					schIdList = null;
					keyToCheck  =null;
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
				}
				tok = null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "runOnDemandOperation()", e);
		}
	}
	
	private void resetLastExectionTimeOperation(NMDScheduleSessionBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()) 
					&& null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
			{
				String[] tok = sessionBean.getSelectedRows().split(",");
				if(null!=tok && tok.length>0)
				{
					NMDScheduleDetails details = null;
					List<String> schIdList=null;
					String keyToCheck=null;
					for(int b=0;b<tok.length;b++)
					{
						details = null;
						for(int a=0;a<sessionBean.getSchedulesList().size();a++)
						{
							details = (NMDScheduleDetails)sessionBean.getSchedulesList().get(a);
							if(details.getScheduleId()>0)
							{
								if(tok[b].toString().equals(String.valueOf(details.getSrNo())))
								{
									if(null==keyToCheck)
									{
										keyToCheck="";
									}
									keyToCheck+=String.valueOf(details.getScheduleId())+",";
									if(null==schIdList || schIdList.size()<=0)
									{
										schIdList = new ArrayList<String>();
									}
									schIdList.add(String.valueOf(details.getScheduleId()));
									break;
								}
							}
							details= null;
						}
					}
					
					if(null!=keyToCheck && !"".equals(keyToCheck))
					{
						if(keyToCheck.endsWith(","))
						{
							keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
						}
						/*
						 * CHECK IF ANY DATA EXTRACTION JOBS RUNNING FOR THE SELECTED MODELS
						 */
						List<NMDScheduleDetails> runningList = NMDScheduleDAO.getRunningScheduleList(keyToCheck);
						boolean proceedFurther=true;
						String runningModels=null;
						if(null!=runningList && runningList.size()>0)
						{
							NMDScheduleDetails cd = null;
							for(int c=0;c<runningList.size();c++)
							{
								cd = (NMDScheduleDetails)runningList.get(c);
								proceedFurther=false;
								if(null==runningModels)
								{
									runningModels="";
								}
								runningModels+=cd.getModel();
								if(c!=runningList.size()-1)
								{
									runningModels+=",";
								}
								cd = null;
							}
							cd = null;
						}
						if(proceedFurther==false)
						{
							if(null!=runningModels && !"".equals(runningModels))
							{
								if(runningModels.endsWith(","))
								{
									runningModels = runningModels.substring(0, runningModels.length()-1);
								}
							}
							// error.data.ext.jobs.running.other.ops.schedule
							sessionBean.setErrorMessage(msgProps.addMessage("error.data.ext.jobs.running.other.ops.schedule", runningModels, msgProps.getProperty("label.reset.for.full.extract")));
						}
						else if(proceedFurther==true)
						{
							if(null!=schIdList && schIdList.size()>0)
							{
								boolean bool = NMDScheduleDAO.resetLastExecTime(schIdList, wslId);
								if(bool==true)
								{
									sessionBean.setSelectedRows(null);
									sessionBean.setSuccessMessage(msgProps.getProperty("reset.last.exec.time.success"));
									/*
									 * call function to get Models List
									 */
									getScheduleList(sessionBean);
								}
								else
								{
									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
									logger.info("resetLastExectionTimeOperation :: Failed to Reset Last Exeuction Time for Selected Schedules.");
								}
							}
							else
							{
								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
								logger.info("resetLastExectionTimeOperation :: Failed to Identify Schedule Ids for Run On Reset Last Exeuction Time Operation.");
							}
						}
						runningList =null;
						runningModels = null;
					}
					schIdList = null;
					keyToCheck = null;
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
				}
				tok = null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "resetLastExectionTimeOperation()", e);
		}
	}
	
	private boolean validateDeltaDateTime(NMDScheduleSessionBean sessionBean)
	{
		if(null==sessionBean.getDeltaLastExecTime() || "".equals(sessionBean.getDeltaLastExecTime()) && 
				null==sessionBean.getConvDeltaLastExecTime())
		{ 
			sessionBean.setErrorMessage(msgProps.getProperty("error.delta.date.time.mandatory"));
			return false;
		}
		
		if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()) && null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
		{
			String[] tok = sessionBean.getSelectedRows().split(",");
			NMDScheduleDetails schDetails = null;
			if(null!=tok && tok.length>0)
			{
				for(int a=0;a<tok.length;a++)
				{
					schDetails = null;
					for(int b=0;b<sessionBean.getSchedulesList().size();b++)
					{
						schDetails = (NMDScheduleDetails)sessionBean.getSchedulesList().get(b);
						if(String.valueOf(schDetails.getSrNo()).equals(tok[a].toString()))
						{
							/*
							 * check if Last Execution Time is Null - Not allowed
							 */
							if(null==schDetails.getLastExecutionTime())
							{
								String errorMessage = msgProps.addMessage("error.last.exec.time.null.model", schDetails.getModel(), msgProps.getProperty("label.update.for.delta.extract"));
								sessionBean.setErrorMessage(errorMessage);
								errorMessage  =null;
								return false;
							}
							
							/*
							 * check if Last Execution Time is Not null - then Delta Date Time must be less than Last Exection Time
							 */
							if(null!=schDetails.getLastExecutionTime() && null!=sessionBean.getConvDeltaLastExecTime())
							{
								if(sessionBean.getConvDeltaLastExecTime().getTime() > schDetails.getLastExecutionTime().getTime())
								{
									String errorMessage = msgProps.addMessage("error.data.less", msgProps.getProperty("label.delta.datetime"), msgProps.getProperty("label.lastexecutiontime"));
									sessionBean.setErrorMessage(errorMessage);
									return false;
								}
							}
							break;
						}
						schDetails=  null;
					}
				}
			}
			tok  =null;
		}
		
		
//		if(null!=sessionBean.getConvDeltaLastExecTime())
//		{
//			try
//			{
//				// 2026/02/16 14:39
//				Date systemDate = new Date();
//				if(sessionBean.getConvDeltaLastExecTime().getTime() > systemDate.getTime())
//				{
//					sessionBean.setErrorMessage(msgProps.getProperty("error.delta.date.time.greater.than.systemdate"));
//					return false;
//				}
//				systemDate = null;
//			}
//			catch(Exception e)
//			{
//				Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "validateDeltaDateTime()", e);
//				sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
//				return false;
//			}
//		}
		return true;
	}
	
	private void updateDeltaLastExectionTimeOperation(NMDScheduleSessionBean sessionBean)
	{
		try
		{
			if(validateDeltaDateTime(sessionBean))
			{
				if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()) 
						&& null!=sessionBean.getSchedulesList() && sessionBean.getSchedulesList().size()>0)
				{
					String[] tok = sessionBean.getSelectedRows().split(",");
					if(null!=tok && tok.length>0)
					{
						NMDScheduleDetails details = null;
						List<String> schIdList=null;
						String keyToCheck=null;
						for(int b=0;b<tok.length;b++)
						{
							details = null;
							for(int a=0;a<sessionBean.getSchedulesList().size();a++)
							{
								details = (NMDScheduleDetails)sessionBean.getSchedulesList().get(a);
								if(details.getScheduleId()>0)
								{
									if(tok[b].toString().equals(String.valueOf(details.getSrNo())))
									{
										if(null==keyToCheck)
										{
											keyToCheck="";
										}
										keyToCheck+=String.valueOf(details.getScheduleId())+",";
										if(null==schIdList || schIdList.size()<=0)
										{
											schIdList = new ArrayList<String>();
										}
										schIdList.add(String.valueOf(details.getScheduleId()));
										break;
									}
								}
								details= null;
							}
						}
						
						if(null!=keyToCheck && !"".equals(keyToCheck))
						{
							if(keyToCheck.endsWith(","))
							{
								keyToCheck = keyToCheck.substring(0, keyToCheck.length()-1);
							}
							/*
							 * CHECK IF ANY DATA EXTRACTION JOBS RUNNING FOR THE SELECTED MODELS
							 */
							List<NMDScheduleDetails> runningList = NMDScheduleDAO.getRunningScheduleList(keyToCheck);
							boolean proceedFurther=true;
							String runningModels=null;
							if(null!=runningList && runningList.size()>0)
							{
								NMDScheduleDetails cd = null;
								for(int c=0;c<runningList.size();c++)
								{
									cd = (NMDScheduleDetails)runningList.get(c);
									proceedFurther=false;
									if(null==runningModels)
									{
										runningModels="";
									}
									runningModels+=cd.getModel();
									if(c!=runningList.size()-1)
									{
										runningModels+=",";
									}
									cd = null;
								}
								cd = null;
							}
							if(proceedFurther==false)
							{
								if(null!=runningModels && !"".equals(runningModels))
								{
									if(runningModels.endsWith(","))
									{
										runningModels = runningModels.substring(0, runningModels.length()-1);
									}
								}
								// error.data.ext.jobs.running.other.ops.schedule
								sessionBean.setErrorMessage(msgProps.addMessage("error.data.ext.jobs.running.other.ops.schedule", runningModels, msgProps.getProperty("label.update.for.delta.extract")));
							}
							else if(proceedFurther==true)
							{
								if(null!=schIdList && schIdList.size()>0)
								{
									boolean bool = NMDScheduleDAO.updateLastExecTime(schIdList, sessionBean.getConvDeltaLastExecTime(), wslId);
									if(bool==true)
									{
										sessionBean.setSuccessMessage(msgProps.getProperty("update.last.exec.time.success"));
										sessionBean.setConvDeltaLastExecTime(null);
										sessionBean.setDeltaLastExecTime(null);
										sessionBean.setSelectedRows(null);
										/*
										 * call function to get Models List
										 */
										getScheduleList(sessionBean);
									}
									else
									{
										sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
										logger.info("updateDeltaLastExectionTimeOperation :: Failed to Reset Last Exeuction Time for Selected Schedules.");
									}
								}
								else
								{
									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
									logger.info("updateDeltaLastExectionTimeOperation :: Failed to Identify Schedule Ids for Delta Update Last Time Exec Operation.");
								}
							}
							runningList =null;
							runningModels = null;
						}
						schIdList = null;
						keyToCheck = null;
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
					}
					tok = null;
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow"));
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(NMDScheduleTransactions.class.getName(), "updateDeltaLastExectionTimeOperation()", e);
		}
	}
	
}