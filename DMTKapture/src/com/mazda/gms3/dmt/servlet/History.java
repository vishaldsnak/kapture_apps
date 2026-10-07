package com.mazda.gms3.dmt.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.bean.HistoryBean;
import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.dao.UserProfileDAO;
import com.mazda.gms3.dmt.conversion.impl.StartConversionMNAOImpl;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.email.generator.NotificationEmailHelper;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ScheduleDetails;

/**
 * Servlet implementation class History
 */
public class History extends HttpServlet {
	private static final long serialVersionUID = 1L;
    static Logger logger = LogManager.getLogger(History.class);
    
	static MessageProperties msgProps= null;
	
	static String wslId="";
    /**
     * @see HttpServlet#HttpServlet()
     */
    public History() {
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
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
			HistoryBean sessionBean = getSessionBean(request);
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
			// get 10 days before today's date
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.DATE, -30);
			Date dateBefore10Days = cal.getTime();
			sessionBean.setFromDate(sdf.format(dateBefore10Days));
			cal  =null;
			dateBefore10Days = null;

			/*
			 * call function to load Schedule List
			 */
			getScheduleList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(History.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/history.jsp");
			rs.forward(request, response);
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
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
			HistoryBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			/*
			 * set displayPageNo and displayPageLenght
			 */
			if(null!=request.getParameter("DMT_HIS_displayPageNo") && !"".equals(request.getParameter("DMT_HIS_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("DMT_HIS_displayPageNo"));
			}
			if(null!=request.getParameter("DMT_HIS_displayPageLen") && !"".equals(request.getParameter("DMT_HIS_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("DMT_HIS_displayPageLen"));
			}
			if(null!=request.getParameter("fromDateField") && !"".equals(request.getParameter("fromDateField")))
			{
				sessionBean.setFromDate((String)request.getParameter("fromDateField"));
			}
			if(null!=request.getParameter("toDateField") && !"".equals(request.getParameter("toDateField")))
			{
				sessionBean.setToDate((String)request.getParameter("toDateField"));
			}
			
			
			if(null!=request.getParameter("DMT_HIS_ActionClicked") && 
					!"".equals(request.getParameter("DMT_HIS_ActionClicked")))
			{
				if(request.getParameter("DMT_HIS_ActionClicked").equals("CLOSE_ITEM_DETAILS"))
				{
					/*
					 * close the opened pop up
					 */
					sessionBean.setItemsList(null);
					sessionBean.setShowDetailsPopUp(false);
				}
				else if(request.getParameter("DMT_HIS_ActionClicked").equals("ABORT_CONVERSION"))
				{
					/*
					 * call function to abort the conversion
					 */
					abortConversionOperation(request, sessionBean);
				}
				else if(request.getParameter("DMT_HIS_ActionClicked").equals("RESET"))
				{
					/*
					 * call function to reload historyList
					 */
					/*
					 * call function to load Schedule List
					 */
					getScheduleList(sessionBean);
				}
				else if(request.getParameter("DMT_HIS_ActionClicked").equals("DELETE_SCHEDULE"))
				{
					/*
					 * call delete schedule function
					 */
					deleteScheduleTransactions(request, sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(History.class.getName(), "doPost()", e);
		}
		if(useReqDis==true)
		{	
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/history.jsp");
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
	
	private HistoryBean getSessionBean(HttpServletRequest request) 
	{
		HistoryBean sessionBean = null;
		if (null != request.getSession().getAttribute("historyBean") && 
				!"".equals(request.getSession().getAttribute("historyBean"))) 
		{
			sessionBean = (HistoryBean) request.getSession().getAttribute("historyBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new HistoryBean();
			request.getSession().setAttribute("historyBean", sessionBean);
		}
		return sessionBean;
	}

	
	private static void getScheduleList(HistoryBean sessionBean)
	{
		try
		{
			sessionBean.setRunReloadScript(false);
			sessionBean.setScheduleList(new ArrayList<ScheduleDetails>());
			if(null!=sessionBean.getFromDate() && !"".equals(sessionBean.getFromDate()) && null!=sessionBean.getToDate()  && !"".equals(sessionBean.getToDate()))
			{
				ArrayList<ScheduleDetails> tempList = ScheduleDAO.getScheduleDetails(sessionBean.getFromDate(), sessionBean.getToDate());
				if(null!=tempList && tempList.size()>0)
				{
					for(int i=0;i<tempList.size();i++)
					{
						ScheduleDetails schDetails = (ScheduleDetails)tempList.get(i);
						if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
						{
							sessionBean.setRunReloadScript(true);
							break;
						}
					}
					/*
					 * PREVIEW - the finished MC / MME content jobs that still have an unpublished document (one query
					 * for the page; the database decides, a preview folder on the S3 mount can come back
					 * after it was deleted)
					 */
					java.util.List<String> previewCandidates = new java.util.ArrayList<String>();
					for(int i=0;i<tempList.size();i++)
					{
						ScheduleDetails schDetails = (ScheduleDetails)tempList.get(i);
						if(null!=schDetails.getMarketName() && (schDetails.getMarketName().trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mc").trim())
									|| schDetails.getMarketName().trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mme").trim())
									|| schDetails.getMarketName().trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mnao").trim()))
								&& AutomationConstants.SCHEDULE_TYPE_DATALOAD.equals(schDetails.getScheduleType())
								&& null!=schDetails.getScheduleStatus()
								&& !schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value"))
								&& !schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
						{
							previewCandidates.add(String.valueOf(schDetails.getScheduleId()));
						}
					}
					java.util.Set<String> previewJobs = new java.util.HashSet<String>();
					try
					{
						previewJobs = com.mazda.gms3.dmt.preview.PreviewDAO.jobsWithUnpublished(previewCandidates);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(History.class.getName(), "getScheduleList()", e);
					}
					/*
					 * update schedule status and show Abort Flag
					 */
					for(int i=0;i<tempList.size();i++)
					{
						ScheduleDetails schDetails = (ScheduleDetails)tempList.get(i);

						/*
						 * CHECK IF THE WSL ID IS FROM GMS3_INTERNAL_TEAM_USERS
						 * THEN SHOW OKADMIN
						 */
						if(null!=schDetails.getUserId() && !"".equals(schDetails.getUserId()))
						{
							// INTERNAL USERS
							StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS"),",");
							while(str.hasMoreTokens())
							{
								String tok = str.nextToken();
								if(tok.trim().toLowerCase().equals(schDetails.getUserId().trim().toLowerCase()))
								{
									schDetails.setUserId(ApplicationProperties.getProperty("USERNAME"));
								}
								tok = null;
							}
							str = null;
						}

						schDetails.setShowAbort(false);
						// PREVIEW - an unpublished document in the database AND the job's preview files
						String scheduleKey = String.valueOf(schDetails.getScheduleId());
						schDetails.setShowPreview(previewJobs.contains(scheduleKey)
								&& com.mazda.gms3.dmt.preview.PreviewBuilder.hasPreview(scheduleKey));
						if(null!=schDetails.getScheduleStatus() && !"".equals(schDetails.getScheduleStatus()))
						{
							if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value")))
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.pending.label"));
							}
							else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.processing.label"));
								// set showAbortLink to true
								schDetails.setShowAbort(true);

								/*
								 * DO THIS ONLY FOR MNAO MARKET
								 */
								if(null!=schDetails.getMarketName() && schDetails.getMarketName().trim().toLowerCase().
										equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
								{
									// also check here, if innerLinksCount is 0, then show innerLinksProcessingStatus as Counting
									if(schDetails.getTotalInnerLinksCount()==0)
									{
										schDetails.setInnerLinksProcessingStatus(msgProps.getProperty("label.counting"));
									}
								}
							}
							else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.success.value")))
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.success.label"));
							}
							else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.failure.value")))
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.failure.label"));
							}
							else if(schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.aborted.value")))
							{
								schDetails.setScheduleStatusLabel(msgProps.getProperty("schedule.status.aborted.label"));
							}

							/*
							 * UPDATE FAILURE DOCS COUNT.
							 * DO THIS ONLY WHEN MARKET VALUE IS MNAO, FOR MC SHOW THE VALUES AS IT IS FETCHED FROM DATABASE
							 */
						//						if(!schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.pending.value")) && 
						//								!schDetails.getScheduleStatus().equals(ApplicationProperties.getProperty("schedule.status.processing.value")))
						//						{
						//							/*
						//							 * DO THIS ONLY FOR MNAO MARKET
						//							 */
						//							if(null!=schDetails.getMarketName() && schDetails.getMarketName().trim().toLowerCase().
						//									equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
						//							{
						//								schDetails.setFailedProcessedDocsCount(schDetails.getTotalDocsForProcessing() - schDetails.getProcessedDocsCount());
						//								schDetails.setFailedDeletedDocsCount(schDetails.getTotalDocsForDeletion() - schDetails.getDeletedDocsCount());
						//
						//								schDetails.setFailedMetaDataDocsCount(schDetails.getTotalMetaDataDocsCount() - schDetails.getProcessedMetaDataDocsCount());
						//								schDetails.setFailedOkAssetsCount(schDetails.getOkAssetsCount() - schDetails.getProcessedOkAssetsCount());
						//								schDetails.setFailedOkAssetsDeleteCount(schDetails.getOkAssetsDeleteCount() - schDetails.getProcessedOkAssetsDeleteCount());
						//
						//								// set Failed Inner Links Count.
						//								schDetails.setFailedInnerLinksCount(schDetails.getTotalInnerLinksCount() - schDetails.getProcessedInnerLinksCount());
						//							}
						//						}
						}
						sessionBean.getScheduleList().add(schDetails);
						schDetails= null;
					}
				}
				tempList=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(History.class.getName(), "getScheduleList()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
		}
	}
	
	@SuppressWarnings("deprecation")
	private static void abortConversionOperation(HttpServletRequest request, HistoryBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("DMT_HIS_ScheduleId") && !"".equals(request.getParameter("DMT_HIS_ScheduleId"))
					&& null!=request.getParameter("DMT_HIS_ThreadId") && !"".equals(request.getParameter("DMT_HIS_ThreadId")) && 
					null!=request.getParameter("DMT_HIS_ScheduleType") && !"".equals(request.getParameter("DMT_HIS_ScheduleType")))
			{
				// delete from display order temp table
				ScheduleDAO.deleteDisplayOrderTemp();
				
				String scheduleId=(String)request.getParameter("DMT_HIS_ScheduleId");
				String threadId=(String)request.getParameter("DMT_HIS_ThreadId");
				String scheduleType=(String)request.getParameter("DMT_HIS_ScheduleType");
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
										/*
										 * CALL FUNCTION TO GENERATE REPORTS
										 * 
										 * to do for MNAO / MC THREAD
										 */
//										StartConversionImpl.printReports(scheduleId);
//										StartConversionImpl.removeCurrentScheduleCodeDataFromLists(scheduleId);
										com.mazda.gms3.dmt.utils.ThreadAbortUtil.abort(at);
									}
									
									// remove success message from here - do it downwards
//									if(scheduleType.trim().equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
//									{
//										sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
//									}
//									else
//									{
//										sessionBean.setSuccessMessage(msgProps.getProperty("abort.ipm.facelift.vin.update.success.message"));
//									}
								} 
								catch (Exception e) 
								{
									Utilities.printStackTraceToLogs(History.class.getName(), "abortConversion()", e);
									// no need of showing any error
									if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
									{
										logger.info(msgProps.getProperty("error.message.conversion.already.finish")+" :: Proceed for updaitng Job Status in DATABASE.");
//										sessionBean.setErrorMessage(msgProps.getProperty("error.message.conversion.already.finish"));
									}
									else
									{
										logger.info(msgProps.getProperty("error.message.ipm.facelift.vin.update.already.finish")+" :: Proceed for updaitng Job Status in DATABASE.");
//										sessionBean.setErrorMessage(msgProps.getProperty("error.message.ipm.facelift.vin.update.already.finish"));
									}
								}
								break;
							}
						}
					}
					
					if (threadFound == false) 
					{
						if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
						{
							logger.info(msgProps.getProperty("error.message.conversion.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
//							sessionBean.setErrorMessage(msgProps.getProperty("error.message.conversion.not.active"));
						}
						else
						{
							logger.info(msgProps.getProperty("error.message.ipm.facelift.vin.update.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
//							sessionBean.setErrorMessage(msgProps.getProperty("error.message.ipm.facelift.vin.update.not.active"));
						}
					}
				} 
				else
				{
					if(scheduleType.equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
					{
						logger.info(msgProps.getProperty("error.message.conversion.not.active")+" :: Proceed for updating Job Status in DATABASE.");
//						sessionBean.setErrorMessage(msgProps.getProperty("error.message.conversion.not.active"));
					}
					else
					{
						logger.info(msgProps.getProperty("error.message.ipm.facelift.vin.update.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
//						sessionBean.setErrorMessage(msgProps.getProperty("error.message.ipm.facelift.vin.update.not.active"));
					}
				}
				threadArray = null;
				threadSet = null;
				
				/*
				 * call function to update ABort Status in DATABASE - IRRELEVANT OF WHETHEER THREAD WAS ACTIVE IN CONTIANER OR NOT
				 * THIS WILL HELP TO AVOID UPDATING STATUS MANUALLY IN DAATBASE.
				 */
				try
				{
					ScheduleDAO.updateScheduleStatus(scheduleId, ApplicationProperties.getProperty("schedule.status.aborted.value"));
					
					
					/*
					 * FETCH USER PROFILE ON THE BASIS OF WSL ID
					 * TO GET EMAIL ID
					 */
					String emailUserId=null;
					try
					{
						emailUserId = UserProfileDAO.getUserEmail(wslId);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(History.class.getName(), "abortConversionOperation()", e);
					}
					
					logger.info("startConversion :: Email Id read from Kapture for WSL ID >> " + wslId+" >> is >> "+ emailUserId);
					
					
					NotificationEmailHelper.generateNotificationEmail(scheduleId, emailUserId);
					
					emailUserId  =null;
					
					// show success Message - independent of whether thread was active or not
					if(scheduleType.trim().equals(AutomationConstants.SCHEDULE_TYPE_DATALOAD))
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("abort.success.message"));
					}
					else
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("abort.ipm.facelift.vin.update.success.message"));
					}
					
					/*
					 * re-call Schedule List
					 */
					getScheduleList(sessionBean);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(History.class.getName(), "abortConversionOperation()", e);
					logger.info("abortConversionOperation :: Error while updating status for Schedule Id {"+scheduleId+"} in DATABASE.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
				}
			}
			else
			{
				logger.info("abortConversionOperation :: Schedule Id & Thread Id from request as param are null. Throw Message");
				sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(History.class.getName(), "abortConversionOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
		}
	}
	
	private void deleteScheduleTransactions(HttpServletRequest request, HistoryBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("DMT_HIS_Sch_SelectedRows") && !"".equals(request.getParameter("DMT_HIS_Sch_SelectedRows")))
			{
				String selRows = (String)request.getParameter("DMT_HIS_Sch_SelectedRows");
				if(null!=selRows && !"".equals(selRows))
				{
					if(selRows.endsWith(","))
					{
						selRows = selRows.substring(0,selRows.length()-1);
					}
					boolean bool = ScheduleDAO.deleteScheduleDetails(selRows);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("contentload.schedule.delete.success"));
						/*
						 * call getScheduleList
						 */
						getScheduleList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.message.contentload.schedule.delete"));
					}
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.delete"));
				}
				selRows=  null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.delete"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(History.class.getName(), "deleteScheduleTransactions()", e);
		}
	}

}
