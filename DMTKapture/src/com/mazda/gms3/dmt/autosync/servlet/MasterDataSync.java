package com.mazda.gms3.dmt.autosync.servlet;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.autosync.bean.MasterDataSyncBean;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncDAO;
import com.mazda.gms3.dmt.autosync.dao.MasterDataSyncTransactionDAO;
import com.mazda.gms3.dmt.autosync.impl.MasterDataSynchingImpl;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncConstants;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

/**
 * Servlet implementation class MasterDataSync
 */
public class MasterDataSync extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private Logger logger = LogManager.getLogger(MasterDataSync.class);
	private MessageProperties msgProps= null;
	
	private String wslId=""; 
	private MasterDataSyncDAO masterDataDAO = new MasterDataSyncDAO();
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MasterDataSync() {
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
			
			MasterDataSyncBean sessionBean = getSessionBean(request);
			sessionBean.setActionClicked(null);
			sessionBean.setCategoryTypesList(null);
			sessionBean.setCategoryTypesList(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setItemsList(null);
			sessionBean.setSelectedCategories(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setTransactionsList(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageTempLength(null);
			sessionBean.setDisplayPageTempNo(null);
			sessionBean.setMarketId(null);
			sessionBean.setMarketList(null);
			sessionBean.setShowItemsBlock(false);
			/*
			 * call function to get marketList
			 */
			getMarketList(sessionBean);
			
			onLoadMarketComboOperation(sessionBean);
			
			getTransactionsList(sessionBean);
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/masterdatasync.jsp");
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
		
			MasterDataSyncBean sessionBean = getSessionBean(request);
			
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageTempLength(null);
			sessionBean.setDisplayPageTempNo(null);
			
			/*
			 * call function to read parameters from Request
			 */
			readParametersFromRequest(request, sessionBean);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("MARKET_SELECTION"))
				{
					sessionBean.setSelectedCategories(null);
					// set itemsList as null
					sessionBean.setItemsList(null);
					sessionBean.setShowItemsBlock(false);
					/*
					 * call getCategoryTypeList
					 */
					getCategoryTypesList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ADD_ITEMS"))
				{
					/*
					 * call add Items Function
					 */
					addItems(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_ITEM"))
				{
					/*
					 * call delete Items Function
					 */
					deleteItem(sessionBean, request);
				}
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					/*
					 * call reset Function
					 */
					reset(sessionBean);
				}
				else if (sessionBean.getActionClicked().equals("SYNC_ITEMS"))
				{
					/*
					 * call SYNC Operation
					 */
					syncOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("REFRESH_ITEMS"))
				{
					/*
					 * call getTransactionsList
					 */
					getTransactionsList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ABORT_SCHEDULE"))
				{
					/*
					 * call abort function
					 */
					abortConversionOperation(request, sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SCHEDULE"))
				{
					/*
					 * call delete schedule function
					 */
					deleteScheduleTransactions(request, sessionBean);
				}
			}
			
			sessionBean.setActionClicked(null);
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "doPost()", e);
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/masterdatasync.jsp");
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
	
	private MasterDataSyncBean getSessionBean(HttpServletRequest request) 
	{
		MasterDataSyncBean sessionBean = null;
		if (null != request.getSession().getAttribute("masterDataSyncBean") && 
				!"".equals(request.getSession().getAttribute("masterDataSyncBean"))) 
		{
			sessionBean = (MasterDataSyncBean) request.getSession().getAttribute("masterDataSyncBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new MasterDataSyncBean();
			request.getSession().setAttribute("masterDataSyncBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void readParametersFromRequest(HttpServletRequest request, MasterDataSyncBean sessionBean)
	{
		try
		{
			sessionBean.setMarketId(null);
			sessionBean.setSelectedCategories(null);
			sessionBean.setActionClicked(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageTempLength(null);
			sessionBean.setDisplayPageTempNo(null);
			
			if(null!=request.getParameter("MD_SYNC_DisplayPageLength") && !"".equals(request.getParameter("MD_SYNC_DisplayPageLength")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("MD_SYNC_DisplayPageLength"));
			}
			if(null!=request.getParameter("MD_SYNC_DisplayPageTempLength") && !"".equals(request.getParameter("MD_SYNC_DisplayPageTempLength")))
			{
				sessionBean.setDisplayPageTempLength((String)request.getParameter("MD_SYNC_DisplayPageTempLength"));
			}
			if(null!=request.getParameter("MD_SYNC_DisplayPageNo") && !"".equals(request.getParameter("MD_SYNC_DisplayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("MD_SYNC_DisplayPageNo"));
			}
			if(null!=request.getParameter("MD_SYNC_DisplayPageTempNo") && !"".equals(request.getParameter("MD_SYNC_DisplayPageTempNo")))
			{
				sessionBean.setDisplayPageTempNo((String)request.getParameter("MD_SYNC_DisplayPageTempNo"));
			}
			if(null!=request.getParameter("MD_SYNC_ActionClicked") && !"".equals(request.getParameter("MD_SYNC_ActionClicked")))
			{
				sessionBean.setActionClicked((String)request.getParameter("MD_SYNC_ActionClicked"));
			}
			if(null!=request.getParameter("MD_SYNC_MarketId") && !"".equals(request.getParameterValues("MD_SYNC_MarketId")))
			{
				sessionBean.setMarketId((String)request.getParameter("MD_SYNC_MarketId"));
			}
			if(null!=request.getParameter("MD_SYNC_SelectedCategories") && !"".equals(request.getParameter("MD_SYNC_SelectedCategories")))
			{
				sessionBean.setSelectedCategories((String)request.getParameter("MD_SYNC_SelectedCategories"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "readParametersFromRequest()", e);
		}
	}
	
	private void getMarketList(MasterDataSyncBean sessionBean)
	{

		sessionBean.setMarketList(new ArrayList<SelectItemDetails>());
		try
		{
			SelectItemDetails si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mnao"));
			si.setValue(ApplicationProperties.getProperty("market.mnao"));
			sessionBean.getMarketList().add(si);
			si  =null;
			
			si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mc"));
			si.setValue(ApplicationProperties.getProperty("market.mc"));
			sessionBean.getMarketList().add(si);
			si  =null;
			
			si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mme"));
			si.setValue(ApplicationProperties.getProperty("market.mme"));
			sessionBean.getMarketList().add(si);
			si  =null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "getMarketList()", e);
		}
	}
	
	private void onLoadMarketComboOperation(MasterDataSyncBean sessionBean)
	{
		sessionBean.setMarketId(null);
		sessionBean.setSelectedCategories(null);
		sessionBean.setItemsList(null);
		sessionBean.setShowItemsBlock(false);
		try
		{
			if(null!=sessionBean.getMarketList() && sessionBean.getMarketList().size()>0)
			{
				SelectItemDetails si = (SelectItemDetails)sessionBean.getMarketList().get(0);
				if(null!=si && null!=si.getValue() && !"".equals(si.getValue()))
				{
					sessionBean.setMarketId(si.getValue());
					/*
					 * call getCategoryTypes operations
					 */
					getCategoryTypesList(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "onLoadMarketComboOperation()", e);
		}
	}
	
	private void getCategoryTypesList(MasterDataSyncBean sessionBean)
	{
		sessionBean.setCategoryTypesList(new ArrayList<SelectItemDetails>());
		try
		{
			if(null!=sessionBean.getMarketId() && !"".equals(sessionBean.getMarketId()))
			{
				ArrayList<SelectItemDetails> tempList = new ArrayList<SelectItemDetails>();
				SelectItemDetails si = new SelectItemDetails();
				StringTokenizer strLabel = new StringTokenizer(AutoSyncConstants.ITEMS_LABELS,",");
				StringTokenizer strKeys = new StringTokenizer(AutoSyncConstants.ITEMS_KEYS,",");
				while(strLabel.hasMoreTokens() && strKeys.hasMoreTokens())
				{
					si = new SelectItemDetails();
					si.setLabel(strLabel.nextToken());
					si.setValue(strKeys.nextToken());
					
					if(sessionBean.getMarketId().equals(ApplicationProperties.getProperty("market.mnao")))
					{
						// do no add CARLINE in this case
						if(!si.getValue().equals(AutoSyncConstants.ITEM_KEY_CARLINE))
						{
							tempList.add(si);
						}
					}
					else
					{
						// MC / MME MARKET = DO NOT ADD MODEL_YEAR & VIN RANGE
						if((!si.getValue().equals(AutoSyncConstants.ITEM_KEY_MODEL_YEAR) && (!si.getValue().equals(AutoSyncConstants.ITEM_KEY_VIN_RANGE))))
						{
							tempList.add(si);
						}
					}
					si = null;
				}
				strLabel = null;
				strKeys=  null;

				if(null!=tempList && tempList.size()>0)
				{
					sessionBean.setCategoryTypesList(tempList);
				}
				tempList=  null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "getCategoryTypesList()", e);
		}
	}
	
	private void getTransactionsList(MasterDataSyncBean sessionBean)
	{
		sessionBean.setReloadJSP(false);
		sessionBean.setTransactionsList(new ArrayList<AutoSyncScheduleDetails>());
		try
		{
			sessionBean.setTransactionsList(masterDataDAO.getScheduleDetailsList());
			if(null!=sessionBean.getTransactionsList() && sessionBean.getTransactionsList().size()>0)
			{
				AutoSyncScheduleDetails schDetails = null;
//				File reportFile = null;
//				String physicalPath=ApplicationProperties.getProperty("masterdata.synching.reports.physicalpath");
//				if(!physicalPath.endsWith("/"))
//				{
//					physicalPath=physicalPath+"/";
//				}
//				String relativePath=ApplicationProperties.getProperty("masterdata.synching.reports.relativepath");	
//				if(!relativePath.endsWith("/"))
//				{
//					relativePath = relativePath+"/";
//				}
//				String reportsPath="";
				for(int a=0;a<sessionBean.getTransactionsList().size();a++)
				{
					schDetails =  (AutoSyncScheduleDetails)sessionBean.getTransactionsList().get(a);
					if(null!=schDetails.getJobStatus() && (schDetails.getJobStatus().equals(AutoSyncConstants.STATUS_PENDING) 
							|| schDetails.getJobStatus().equals(AutoSyncConstants.STATUS_PROCESSING)))
					{
						sessionBean.setReloadJSP(true);
						break;
					}
					schDetails = null;
//					// perform for reportsPath
//					try
//					{
//						reportFile= PathUtil.file(physicalPath+String.valueOf(schDetails.getScheduleId())+"/"+String.valueOf(schDetails.getScheduleId())+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX"));
//						if(reportFile.exists() && reportFile.isFile())
//						{
//							reportsPath = relativePath+String.valueOf(schDetails.getScheduleId())+"_"+ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX");
//							if(null!=reportsPath && !"".equals(reportsPath))
//							{
//								schDetails.setReportsPath(reportsPath);
//							}
//						}
//						reportFile = null;
//						reportsPath = null;
//					}
//					catch(Exception  e)
//					{
//						// DO NOTHING HERE
//						logger.info("getTransactionsList :: Exception While reading the Report File for Schedule Id {"+schDetails.getScheduleId()+"}. Either the reports does not exist or corrupted.");
//					}
//					reportFile = null;
//					reportsPath = null;
				}
//				reportFile = null;
//				reportsPath = null;
//				physicalPath= null;
//				relativePath = null;
				schDetails=  null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "getTransactionsList()", e);
		}
	}
	
	private void addItems(MasterDataSyncBean sessionBean)
	{
		try
		{
			if(validateAddItems(sessionBean))
			{
				sessionBean.setItemsList(new ArrayList<AutoSyncScheduleItemDetails>());
				// show ItemsBlock to true
				sessionBean.setShowItemsBlock(true);
				ArrayList<AutoSyncScheduleItemDetails> tempList =masterDataDAO.getItemsCountForProcessing(sessionBean.getSelectedCategories().split(","), sessionBean.getMarketId());
				if(null!=tempList && tempList.size()>0)
				{
					// show only those items where count is more than 0 & categoriesList is not null
					AutoSyncScheduleItemDetails items = null;
					for (int r=0;r<tempList.size();r++)
					{
						items = (AutoSyncScheduleItemDetails)tempList.get(r);
						if(items.getTotalCount()>0)
						{
							sessionBean.getItemsList().add(items);
						}
						items = null;
					}
					items = null;
					
					if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
					{
						// set SRNO
						for(int b=0;b<sessionBean.getItemsList().size();b++)
						{
							items = (AutoSyncScheduleItemDetails)sessionBean.getItemsList().get(b);
							items.setSrNo(b+1);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "addItems()", e);
		}
	}
	
	private boolean validateAddItems(MasterDataSyncBean sessionBean)
	{
		if(null==sessionBean.getMarketId() || "".equals(sessionBean.getMarketId()))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.mandatory.fields"));
			return false;
		}
		if(null==sessionBean.getSelectedCategories() || "".equals(sessionBean.getSelectedCategories()))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.mandatory.select.one.item"));
			return false;
		}
		return true;
	}
	
	private void deleteItem(MasterDataSyncBean sessionBean, HttpServletRequest request)
	{
		try
		{
			if(null!=request.getParameter("MD_SYNC_ItemToBeDeleted") && !"".equals(request.getParameter("MD_SYNC_ItemToBeDeleted")))
			{
				if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
				{
					for(int a=0;a<sessionBean.getItemsList().size();a++)
					{
						AutoSyncScheduleItemDetails details = (AutoSyncScheduleItemDetails)sessionBean.getItemsList().get(a);
						if(String.valueOf(details.getSrNo()).equals((String)request.getParameter("MD_SYNC_ItemToBeDeleted")))
						{
							// remove item from List
							sessionBean.getItemsList().remove(a);
							a--;
							break;
						}
					}
				}

				// re-arrange srNo
				if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
				{
					for(int a=0;a<sessionBean.getItemsList().size();a++)
					{
						AutoSyncScheduleItemDetails details = (AutoSyncScheduleItemDetails)sessionBean.getItemsList().get(a);
						details.setSrNo(a+1);
					}
				}
			}
			else
			{
				logger.info("deleteItem() :: Sr. No. for the item to be deleted is null. Failed to delete item.");
				sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "deleteItem()", e);
		}
	}
	
	private void reset(MasterDataSyncBean sessionBean)
	{
		sessionBean.setActionClicked(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSelectedCategories(null);
		sessionBean.setSuccessMessage(null);
		
		onLoadMarketComboOperation(sessionBean);
		getTransactionsList(sessionBean);
	}
	
	private void syncOperation(MasterDataSyncBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
			{
				if(validateConversion(sessionBean))
				{
					/*
					 * CREATE A SCHEDULE FOR MASTER DAYA SYNCHING
					 */
					AutoSyncScheduleDetails details = new AutoSyncScheduleDetails();
					details.setScheduleName(ApplicationProperties.getProperty("automation.sch.key")+sessionBean.getMarketId().trim().toUpperCase()+"_");
					details.setUserId(wslId);
					details.setScheduleTime(new Timestamp(new Date().getTime()));
					details.setJobStatus(AutoSyncConstants.STATUS_PENDING);
					details.setThreadId(ApplicationProperties.getProperty("automation.sch.key")+sessionBean.getMarketId().trim().toUpperCase()+"_");
					// SET MARKET
					details.setMarket(sessionBean.getMarketId());
					details.setItemsList(sessionBean.getItemsList());
					
					/*
					 * IDENTIFY TOTAL COUNT
					 */
					int totalCount=0;
					if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
					{
						AutoSyncScheduleItemDetails items = null;
						for(int a=0;a<sessionBean.getItemsList().size();a++)
						{
							items = (AutoSyncScheduleItemDetails)sessionBean.getItemsList().get(a);
							totalCount = totalCount+items.getTotalCount();
						}
						items = null;
					}
					
					details.setTotalCount(totalCount);
					totalCount=  0;
					
					details=masterDataDAO.createSchedule(details);
					
					if(null!=details && details.getScheduleId()>0)
					{
						/*
						 * PROCEED FOR EXECUTING THREAD
						 */
						final AutoSyncScheduleDetails scheduleDetails = new AutoSyncScheduleDetails();
						scheduleDetails.setScheduleId(details.getScheduleId());
						scheduleDetails.setScheduleName(details.getScheduleName());
						scheduleDetails.setTotalCount(details.getTotalCount());
						scheduleDetails.setThreadId(details.getThreadId());
						scheduleDetails.setMarket(details.getMarket());
						scheduleDetails.setItemsList(details.getItemsList());
						scheduleDetails.setUserId(details.getUserId());
						final MasterDataSynchingImpl mastDataSyncImpl = new MasterDataSynchingImpl();
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {
								
								synchronized (mastDataSyncImpl) {
									try {
										mastDataSyncImpl.startSynching(scheduleDetails);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "run()", e);
									}
								}
							}
						};
						
						Thread th = new Thread(runn, details.getThreadId());
						th.start();
						
						// reset itemsList Only
						sessionBean.setItemsList(null);
						sessionBean.setShowItemsBlock(false);
						
						sessionBean.setSuccessMessage(msgProps.addMessage("conversion.schedule.success",details.getScheduleName()));
						
						details = null;
						/*
						 * call function to get transactions:List
						 */
						getTransactionsList(sessionBean);
					}
					else
					{
						logger.info("syncOperation :: Failed to create schedule for {"+sessionBean.getMarketId()+"} Market. Throwing Generic Error.");
						// throw generic error
						sessionBean.setErrorMessage(msgProps.getProperty("error.message.failure.master.data.synching.conversion"));
					}
					details=null;
				}
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.message.mandatory.select.one.item"));
			}
		}
		catch(Exception e)
		{
			logger.info("syncOperation :: Some Exception Occured while creating Schedule for {"+sessionBean.getMarketId()+"} Market. Throwing Generic Error.");
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "syncOperation()", e);
			// throw generic error
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
		}
	}
	
	@SuppressWarnings("deprecation")
	private void abortConversionOperation(HttpServletRequest request, MasterDataSyncBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("MD_SYNC_AbortThreadId") && !"".equals(request.getParameter("MD_SYNC_AbortThreadId"))
					&& null!=request.getParameter("MD_SYNC_AbortScheduleId") && !"".equals(request.getParameter("MD_SYNC_AbortScheduleId")))
			{
				String scheduleId=(String)request.getParameter("MD_SYNC_AbortScheduleId");
				String threadId=(String)request.getParameter("MD_SYNC_AbortThreadId");
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
										 * STOP THREAD
										 */
										com.mazda.gms3.dmt.utils.ThreadAbortUtil.abort(at);
									}
								} 
								catch (Exception e) 
								{
									Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "abortConversion()", e);
									// no need of showing any error
									logger.info(msgProps.getProperty("error.message.masterdatasync.already.finish")+" :: Proceed for updaitng Job Status in DATABASE.");
								}
								break;
							}
						}
					}
					
					if (threadFound == false) 
					{
						logger.info(msgProps.getProperty("error.message.masterdatasync.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
					}
				} 
				else
				{
					logger.info(msgProps.getProperty("error.message.masterdatasync.not.active")+" :: Proceed for updaitng Job Status in DATABASE.");
				}
				threadArray = null;
				threadSet = null;
				
				/*
				 * call function to update ABort Status in DATABASE - IRRELEVANT OF WHETHEER THREAD WAS ACTIVE IN CONTIANER OR NOT
				 * THIS WILL HELP TO AVOID UPDATING STATUS MANUALLY IN DAATBASE.
				 */
				try
				{
					MasterDataSyncTransactionDAO transDao = new MasterDataSyncTransactionDAO();
					transDao.updateJobStatus(scheduleId, AutoSyncConstants.STATUS_ABORTED, AutoSyncConstants.STATUS_FAILURE);
					transDao = null;
					sessionBean.setSuccessMessage(msgProps.getProperty("abort.masterdatasync.success.message"));
					/*
					 * re-call Transaction List
					 */
					getTransactionsList(sessionBean);
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "abortConversionOperation()", e);
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
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "abortConversionOperation()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
		}
	}
	
	private void deleteScheduleTransactions(HttpServletRequest request, MasterDataSyncBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("MD_SYNC_Sch_SelectedRows") && !"".equals(request.getParameter("MD_SYNC_Sch_SelectedRows")))
			{
				String selRows = (String)request.getParameter("MD_SYNC_Sch_SelectedRows");
				if(null!=selRows && !"".equals(selRows))
				{
					if(selRows.endsWith(","))
					{
						selRows = selRows.substring(0,selRows.length()-1);
					}
					boolean bool = masterDataDAO.deleteScheduleDetails(selRows);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("schedule.delete.success"));
						/*
						 * call getTransactionList
						 */
						getTransactionsList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.message.schedule.delete"));
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
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "deleteScheduleTransactions()", e);
		}
	}

	private boolean validateConversion(MasterDataSyncBean sessionBean)
	{
		/*
		 * check if there's any Pending / Processing Job - then do not allow to Schedule Conversion.
		 * Date 19 November 2016
		 */
		int count=0;
		try 
		{
			count = ScheduleDAO.getPendingProcessingJobsCount();
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(MasterDataSync.class.getName(), "validateConversion()", e);
		}
		if(count>0)
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.fail.master.data.synching"));
			return false;
		}
		return true;
	}
}
