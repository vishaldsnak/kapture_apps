package com.mazda.gms3.cdrom.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;

import com.mazda.gms3.cdrom.bean.ApplicableVINList;
import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.bean.CDRomSearchBean;
import com.mazda.gms3.cdrom.bean.CDRomUtil;
import com.mazda.gms3.cdrom.bean.CDRomZipUtils;
import com.mazda.gms3.cdrom.bean.DocumentFailureReportBean;
import com.mazda.gms3.cdrom.bean.InnerLinkReportBean;
import com.mazda.gms3.cdrom.bean.KeyWordSearchBean;
import com.mazda.gms3.cdrom.bean.LabelBean;
import com.mazda.gms3.cdrom.bean.OKAssetReportBean;
import com.mazda.gms3.cdrom.bean.TransactionReportBean;
import com.mazda.gms3.cdrom.bean.ViewContentBean;
import com.mazda.gms3.cdrom.bean.impl.CDRomStartConversionImpl;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.utils.AbortCheck;

/**
 * Servlet implementation class CDRomDownloadServlet
 */

public class CDRomScheduleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomScheduleServlet.class);
	String wslId = "";
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;
	MessageProperties msgProps = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomScheduleServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		try
		{
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
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
			// INITIALIZE SESSION BEAN
			CDRomSearchBean sessionBean = getSessionBean(request);
			// RESET ERROR MESASGE
			sessionBean.setErrorMessage(null);
			// RESET ACTION CLICKED
			sessionBean.setActionClicked(null);
			
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false 	&& sessionBean.isShowWriteControls() == false) 
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomScheduleServlet.class.getName(), "doGet()", e);
		}

		if (useReqDis == true) 
		{
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
				// REDIRECT TO CDROM RESULT JSP
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/cdrom/cdromresult.jsp");
				rs.forward(request, response);
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

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		String jspOutCome="/jsps/cdrom/cdromresult.jsp";
		try
		{
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
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
			
			CDRomSearchBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage(null);
			
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false && sessionBean.isShowWriteControls() == false) 
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			/*
			 * CALL Function to read Params from Request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("BACK_TO_DETAIL_PAGE"))
				{
					jspOutCome ="REDIRECT_TO_DETAIL_PAGE";
				}
				else if(sessionBean.getActionClicked().equals("SCHEDULE_JOB"))
				{
					// PROCEED FOR SCHEDULING JOB
					boolean bool = scheduleJobOperation(sessionBean, request);
					if(bool==true)
					{
						jspOutCome = "REDIRECT_TO_HISTORY";
					}
				}
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(CDRomScheduleServlet.class.getName(), "doPost()", e);
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
				if(null!=jspOutCome && jspOutCome.equals("REDIRECT_TO_DETAIL_PAGE"))
				{
					response.sendRedirect(request.getContextPath()+"/cdromdetail?cromsbk=true");
				}
				else if(null!=jspOutCome && jspOutCome.equals("REDIRECT_TO_HISTORY"))
				{
					response.sendRedirect(request.getContextPath() + "/history");
				}
				else 
				{
					// REDIRECT TO CDROM RESULT JSP
					RequestDispatcher rs = request.getRequestDispatcher(jspOutCome);
					rs.forward(request, response);
				}
			}
		}
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
			Utilities.printStackTraceToLogs(
					CDRomScheduleServlet.class.getName(),
					"performAccessCheck()", e);
		}
	}

	private void readParamsFromRequest(CDRomSearchBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setActionClicked(null);
			if(null!=request.getParameter("CD_Rom_ActionClicked") && !"".equals(request.getParameter("CD_Rom_ActionClicked")))
			{
				sessionBean.setActionClicked(request.getParameter("CD_Rom_ActionClicked"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomScheduleServlet.class.getName(), "readParamsFromRequest()", e);
		}
	}

	private boolean scheduleJobOperation(CDRomSearchBean sessionBean , HttpServletRequest request)
	{
		boolean bool = false;
		try
		{
			// IDENTIFY MODEL TYPE & CARLINE REGIONAL NAME
//			String modelTypeForSchedule="";
			String carlineNameRegional="";
			
			if(null==sessionBean.getModels() || sessionBean.getModels().size()<=0)
			{
				CDRomUtil.manageModelsList(sessionBean);
			}
			if(null!=sessionBean.getModels())
			{
				for(LabelBean clDetails : sessionBean.getModels())
				{
					if(clDetails.getKey().trim().toLowerCase().equals(sessionBean.getSelectedModel().trim().toLowerCase()))
					{
						carlineNameRegional = clDetails.getExtraAttribute().trim();
						/*
						 * Issue RE: MGSS-MC: Bongo(SK) of CD creation data has problem Date 25 Oct 2020
						 * REPLACE / BY SPACE IN THE REGIONAL NAME AS IT IS CREATING EXTRA FOLDER IN THE EXTRACTED STRUCTURE
						 */
						if(null!=carlineNameRegional && !"".equals(carlineNameRegional))
						{
							carlineNameRegional = carlineNameRegional.replace("/", " ");
						}
//						modelTypeForSchedule = clDetails.getModelTypeForCarline().trim();
						break;
					}
					clDetails= null;
				}
			}
			
			// START PREPARING SCHEDULE DATA
			ArrayList<CDRomScheduleItemDetails> finalItemsList = new ArrayList<CDRomScheduleItemDetails>();
			long totalDocsCount = 0;
			
			if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
			{
				for(CDRomScheduleItemDetails itemDetails : sessionBean.getItemsList())
				{
					if(itemDetails.getTotalDocsForProcessing()>0)
					{
						totalDocsCount =totalDocsCount+itemDetails.getTotalDocsForProcessing();
						finalItemsList.add(itemDetails);
					}
					itemDetails = null;
				}
			}
			
			if(totalDocsCount>0)
			{
				logger.info("scheduleJobOperation :: Total Docs found for Extraction are :: > "+ totalDocsCount);
				String timeStamp = String.valueOf(System.currentTimeMillis());
				CDRomScheduleDetails schDetails = new CDRomScheduleDetails();
				schDetails.setScheduleName(ApplicationProperties.getProperty("schedule.name.key") + timeStamp + "_");
				schDetails.setThreadId(ApplicationProperties.getProperty("schedule.name.key"));
				schDetails.setUserId(wslId);
				schDetails.setScheduleStatus(ApplicationProperties.getProperty("schedule.status.pending.value"));
				schDetails.setTotalDocsForProcessing(totalDocsCount);
				schDetails.setOkAssetsCount(0);
				
				schDetails.setCountryLocalId(new Long(sessionBean.getSelectedCountry()).longValue());
				schDetails.setLocaleCode(sessionBean.getSelectedLanguage());
				if(null!=sessionBean.getLanguageList())
				{
					for(ManualLanguageDetails mlDetails : sessionBean.getLanguageList())
					{
						if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(sessionBean.getSelectedLanguage().trim().toLowerCase()))
						{
							schDetails.setLanguageLocalId(mlDetails.getManualLanguageId());
							break;
						}
						mlDetails = null;
					}
				}
				
				String carlineCode="";
				String modelNameEnglish="";
				if(null!=sessionBean.getSelectedModel() && !"".equals(sessionBean.getSelectedModel()))
				{
					try
					{
						carlineCode = sessionBean.getSelectedModel().substring(0, sessionBean.getSelectedModel().indexOf("_"));
						modelNameEnglish = sessionBean.getSelectedModel().substring(sessionBean.getSelectedModel().indexOf("_")+1, sessionBean.getSelectedModel().length());
					}
					catch(Exception e){}
				}
//				schDetails.setCarlineCode(sessionBean.getSelectedModel());
				schDetails.setCarlineCode(carlineCode);
				schDetails.setCarlineNameRegional(carlineNameRegional.trim());
				
				// NO NEED TO SET MODEL TYPE VALUE FOR SCHEDULE - NOT USED ANYWHERE
//				schDetails.setModelType(modelTypeForSchedule.trim());
				schDetails.setVinWmiCode(sessionBean.getSelectedWmi());
				schDetails.setVinVdsCode(sessionBean.getSelectedVds());
				schDetails.setVinStartRange(sessionBean.getSelectedVinRange());
				
				schDetails.setItemsList(finalItemsList);
				
				Long scheduleId = CDRomDAO.createSchedule(schDetails);
				if(null!=scheduleId && scheduleId>0)
				{
					logger.info("scheduleJobOperation :: Job {"+schDetails.getScheduleName()+"} Scheduled Successfully.");
					bool = true;
					
					/*
					 * INITIATE JOB 
					 */
					final Long scheduleIdForJob = scheduleId;
					final String locale = sessionBean.getSelectedLanguage();
					final String vinStartRange = sessionBean.getSelectedVinRange().trim();
					final String vdsCode = sessionBean.getSelectedVds().trim();
					final String wmiCode = sessionBean.getSelectedWmi().trim();
					final String modelName = carlineNameRegional.trim();
					final String modelNameEng = modelNameEnglish.trim();
//					final String modelType = modelTypeForSchedule;
//					final String carLineCode = sessionBean.getSelectedModel().trim();
					final String carLineCode = carlineCode.trim();
					final String scheduleTimeStamp = String.valueOf(System.currentTimeMillis());
					final List<KeyWordSearchBean> keywordSearchList = new ArrayList<KeyWordSearchBean>();
					final List<ViewContentBean> viewContentList = new ArrayList<ViewContentBean>();
					final List<OKAssetReportBean> okAssetReportBean = new ArrayList<OKAssetReportBean>();
					final List<InnerLinkReportBean> innerLinkReportBean = new ArrayList<InnerLinkReportBean>();
					final List<TransactionReportBean> transactionReportBean = new ArrayList<TransactionReportBean>();
					final List<DocumentFailureReportBean> documentFailureReportBean = new ArrayList<DocumentFailureReportBean>();
					final String networkPath = ApplicationProperties.getProperty("cdrom.schedule.path");
					final Set<String> firstLevelCatList = new HashSet<String>();
					final ArrayList<CDRomScheduleItemDetails> itemsListForSchedule = finalItemsList;
					
					String threadId = ApplicationProperties.getProperty("schedule.name.key") + String.valueOf(scheduleId);
					final CDRomSearchBean searchBean = getSessionBean(request);
					final CDRomStartConversionImpl startConvImpl = new CDRomStartConversionImpl();
					final String scheduleName = ApplicationProperties.getProperty("schedule.name.key")+ timeStamp+ "_"+ String.valueOf(scheduleId);
					
					final CDRomUtil cdromUtil = new CDRomUtil();
					final CDRomDAO cdromDAO = new CDRomDAO();
					final CDRomZipUtils cdromZip = new CDRomZipUtils();
					final CDRomZipUtils cdromReportZip =new CDRomZipUtils();
					/*
					 * THE ZIP STEPS RUN INSIDE THIS SCHEDULE'S OWN THREAD and are the longest part of the
					 * data extract, so they need their own way to notice an abort. CDRom keeps NUMERIC
					 * status codes, which CDRomDAO.isAborted() resolves.
					 */
					final String abortScheduleId = String.valueOf(scheduleIdForJob);
					AbortCheck cdAbortCheck = new AbortCheck()
					{
						public boolean isAborted()
						{
							return CDRomDAO.isAborted(abortScheduleId);
						}
					};
					cdromUtil.setAbortCheck(cdAbortCheck);
					cdromZip.setAbortCheck(cdAbortCheck);
					cdromReportZip.setAbortCheck(cdAbortCheck);
					Runnable runn = new Runnable() {
						public void run() {
							synchronized (startConvImpl) {
								logger.info("Thread starts here for schedule id : "
										+ String.valueOf(scheduleIdForJob));

								cdromUtil.getDocumentData(locale, carLineCode,
										vdsCode, vinStartRange, wmiCode,scheduleTimeStamp,
										modelName, wslId, startConvImpl, networkPath,
										keywordSearchList, viewContentList,
										firstLevelCatList, searchBean, okAssetReportBean,
										innerLinkReportBean, transactionReportBean,
										documentFailureReportBean, scheduleIdForJob, itemsListForSchedule,cdromDAO);

								try {
									if (documentFailureReportBean.size() > 0
											|| transactionReportBean.size() > 0) {

										/*
										 * IDENTIFY APPLICABLE VIN LIST - prepare ALL IDENTIFIED DOCUMENTS LIST
										 */
										ArrayList<String> docList = new ArrayList<String>();
										CDRomScheduleItemDetails docSchDetails = null;
										if(null!=itemsListForSchedule && itemsListForSchedule.size()>0)
										{
											for(int r=0;r<itemsListForSchedule.size();r++)
											{
												docSchDetails = (CDRomScheduleItemDetails)itemsListForSchedule.get(r);
												if(null!=docSchDetails.getDocumentIdsList() && docSchDetails.getDocumentIdsList().size()>0)
												{
													docList.addAll(docSchDetails.getDocumentIdsList());
												}
												docSchDetails = null;
											}
										}
										docSchDetails = null;
										ArrayList<ApplicableVINList> applicableVINList = new ArrayList<ApplicableVINList>();
										if(null!=docList && docList.size()>0)
										{
											// updtae processing status to Applicable VIN Processing
											cdromDAO.updateCurrentProcessingStatus(
													String.valueOf(scheduleIdForJob),
													ApplicationProperties
													.getProperty("schedule.current.status.applicable.vin.processing"),
													"");

											// call function to identify applicable vin list
											applicableVINList = cdromDAO.getApplicableVINList(docList, modelNameEng, carLineCode,wmiCode,locale);
										}
										docList=  null;


										//	CDRomUtil.copyHTML(scheduleTimeStamp, networkPath, searchBean.getApplicableVINList());
										/*
										 * ABORTED FROM THE SCREEN - do not copy, json or zip. Everything from here to the end
										 * of this thread is post-conversion packaging; on Java 8 Thread.stop() killed the
										 * thread at whatever point the user clicked and none of it ran.
										 */
										if(CDRomDAO.isAborted(abortScheduleId)) { return; }
										cdromUtil.copyHTML(scheduleTimeStamp, networkPath, applicableVINList,locale);
										//	CDRomUtil.createJson(keywordSearchList, viewContentList,
										//	scheduleTimeStamp, firstLevelCatList, networkPath,
										//	modelName, carLineCode, vdsCode, vinStartRange,
										//	selectedManualTypesAll, searchBean);

										cdromUtil.createJson(keywordSearchList, viewContentList,
												scheduleTimeStamp, firstLevelCatList, networkPath,
												modelName, carLineCode, vdsCode, vinStartRange,
												searchBean, itemsListForSchedule,locale,wmiCode);
										cdromUtil.createReports(networkPath, scheduleTimeStamp,
												okAssetReportBean, innerLinkReportBean,
												transactionReportBean, wslId, scheduleName,
												locale, modelName, carLineCode, wmiCode,
												vdsCode, vinStartRange,
												documentFailureReportBean);
										cdromDAO.updateCurrentProcessingStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.current.status.zip.processing"),
												"");

//										cdromZip = new CDRomZipUtils(networkPath+ "/CD_ROM_" + scheduleTimeStamp + ".zip", networkPath	+ "/CD_ROM_" + scheduleTimeStamp);
										/*
										 * ABORTED FROM THE SCREEN - do not zip. On Java 8 Thread.stop() killed this thread
										 * here and no CD image was ever produced.
										 */
										if(CDRomDAO.isAborted(abortScheduleId)) { return; }
										cdromZip.initialize(networkPath+ "/CD_ROM_" + scheduleTimeStamp + ".zip", networkPath	+ "/CD_ROM_" + scheduleTimeStamp);
										cdromZip.generateFileList(new File(networkPath	+ "/CD_ROM_" + scheduleTimeStamp));
										boolean zipFlag = cdromZip.zipIt(networkPath + "/CD_ROM_" + scheduleTimeStamp
												+ ".zip");
										String zipFileName = networkPath + "/CD_ROM_"
												+ scheduleTimeStamp + ".zip";
										
										if(zipFlag==false)
										{
											/*
											 * generateZipFailureReport
											 */
											cdromUtil.generateZipFileErrorReport(networkPath, scheduleTimeStamp,cdromZip.getErrorsList());
										}
										
										// set errorsList to null
										cdromZip.setErrorsList(null);
										
										cdromDAO.updateZipFileName(zipFileName,
												String.valueOf(scheduleIdForJob));
										cdromDAO.updateCurrentProcessingStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.current.status.report.processing"),
												"");
										
										zipFileName = null;

										cdromReportZip.initialize(
												networkPath + "/CD_ROM_" + scheduleTimeStamp
												+ "_Reports.zip", networkPath
												+ "/CD_ROM_" + scheduleTimeStamp + "_Reports");
										
//										CDRomZipUtils cdromReportZip = new CDRomZipUtils(
//												networkPath + "/CD_ROM_" + scheduleTimeStamp
//												+ "_Reports.zip", networkPath
//												+ "/CD_ROM_" + scheduleTimeStamp + "_Reports");
										cdromReportZip.generateFileList(new File(networkPath
												+ "/CD_ROM_" + scheduleTimeStamp + "_Reports"));
										cdromReportZip.zipIt(networkPath + "/CD_ROM_"
												+ scheduleTimeStamp + "_Reports.zip");
										
										cdromDAO.updateCurrentProcessingStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.current.status.complete.value"),
												"");
										int failureCount = cdromDAO.getFailureCount(scheduleIdForJob);
										if (failureCount == 0) {
											
											/*
											 * CHECK HERE IF ZIP FLAG IS FALSE
											 * E.G. SOME ERROR WHILE GENERATING ZIP FILE SET JOB STATUS TO FAILURE
											 */
											if(zipFlag==true)
											{
												cdromDAO.updateScheduleStatus(
														String.valueOf(scheduleIdForJob),
														ApplicationProperties
														.getProperty("schedule.status.success.value"));
											}
											else
											{
												cdromDAO.updateScheduleStatus(
														String.valueOf(scheduleIdForJob),
														ApplicationProperties
														.getProperty("schedule.status.failure.value"));
											}
										} else if (failureCount > 0) {
											cdromDAO.updateScheduleStatus(
													String.valueOf(scheduleIdForJob),
													ApplicationProperties
													.getProperty("schedule.status.failure.value"));
										}
									} else {
										cdromDAO.updateCurrentProcessingStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.current.status.complete.value"),
												"");
										cdromDAO.updateScheduleStatus(String
												.valueOf(scheduleIdForJob), ApplicationProperties
												.getProperty("schedule.status.no.record.value"));
									}
									// DO NOT REMOVE THE CD-ROM FILE, AS IT NEEDS TO BE VIEWED ONLINE AS WELL
									//								FileUtils.forceDelete(new File(networkPath + "/CD_ROM_"
									//										+ scheduleTimeStamp));
									FileUtils.deleteDirectory(new File(networkPath + "/CD_ROM_"
											+ scheduleTimeStamp + "_Reports"));
								} catch (Exception e) {
									logger.info("scheduleJob :: ################ Exception ################");
									Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
											"scheduleJob()", e);
									/*
									 * Complete Job & Mark Status as Failure
									 */
									try
									{
										cdromDAO.updateCurrentProcessingStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.current.status.complete.value"),
												"");
										
										cdromDAO.updateScheduleStatus(
												String.valueOf(scheduleIdForJob),
												ApplicationProperties
												.getProperty("schedule.status.failure.value"));
									}
									catch(Exception eq)
									{
										Utilities.printStackTraceToLogs(CDRomDAO.class.getName(),
												"scheduleJob()", eq);
									}
									
									logger.info("scheduleJob :: ################ Exception ################");
								}
							}
						}
					};
					Thread th = new Thread(runn, threadId);
					th.start();
					threadId = null;
				}
				else
				{
					logger.info("scheduleJobOperation :: Failed to Create Schedule Id. Throw Generic Error.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.job.schedule.failure"));
				}
				schDetails =null;
				carlineNameRegional = null;
				timeStamp = null;
				scheduleId = null;
			}
			else
			{
				/*
				 * CANNOT PROCEED FURTHER AS EITHER NO ITEMS ARE SELECTED OR NO DOCUMENTS FOUND FOR 
				 * PROCESSING FOR THE SELECTED MANUAL TYPES
				 */
				logger.info("scheduleJobOperation :: As No Documents Found for Extraction for the Selected Manual Types.");
				// set errorMessage
				sessionBean.setErrorMessage(msgProps.getProperty("error.job.schedule.nodocs.found"));
			}
			finalItemsList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomScheduleServlet.class.getName(), "scheduleJobOperation()", e);
		}
		return bool;
	}

}
