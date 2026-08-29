package com.mazda.gms3.cdrom.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.cdrom.bean.CDRomSettingDetails;
import com.mazda.gms3.cdrom.bean.CDRomSettingsBean;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;

public class CDRomSettingsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomSettingsServlet.class);

	static MessageProperties msgProps = null;
	static String wslId = "";
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomSettingsServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		try {
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
			msgProps = new MessageProperties(request.getSession().getAttribute(
					"MDM_LS_Locale"));

			CDRomSettingsBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false
					&& sessionBean.isShowWriteControls() == false) {
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setShowSave(false);
			sessionBean.setShowUpdate(false);
			sessionBean.setFieldDetails(new CDRomSettingDetails());
			sessionBean.getFieldDetails().setUserId(wslId);
			/*
			 * call function to get Network Path on the basis of User Id
			 */
			getSettingDetails(sessionBean);

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomSettingsServlet.class.getName(), "doGet()", e);
		}
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList()
					|| userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean
							.getTopMenuList().size() == 1)) {
				response.sendRedirect(request.getContextPath() + "/mypage");
			} else {
				RequestDispatcher rs = request
						.getRequestDispatcher("/jsps/cdrom/settings.jsp");
				rs.forward(request, response);
			}
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		try {
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
			msgProps = new MessageProperties(request.getSession().getAttribute(
					"MDM_LS_Locale"));

			CDRomSettingsBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if (sessionBean.isShowReadControls() == false
					&& sessionBean.isShowWriteControls() == false) {
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			if (null == sessionBean.getFieldDetails()) {
				sessionBean.setFieldDetails(new CDRomSettingDetails());
			}

			sessionBean.getFieldDetails().setUserId(wslId);

			if (null != request.getParameter("MDM_SET_ActionClicked")
					&& !"".equals(request.getParameter("MDM_SET_ActionClicked"))) {
				if (request.getParameter("MDM_SET_ActionClicked").equals(
						"SAVE_MAPPING")) {
					sessionBean.getFieldDetails().setNetworkPath(null);
					if (null != request.getParameter("MDM_SET_NetworkPath")
							&& !"".equals(request
									.getParameter("MDM_SET_NetworkPath"))) {
						sessionBean.getFieldDetails().setNetworkPath(
								(String) request
										.getParameter("MDM_SET_NetworkPath"));
					}

					/*
					 * validate path & userId
					 */
					if (validate(sessionBean)) {
						int count = CDRomDAO.getProcessingScheduleJobCount();
						if (count <= 0) {
							/*
							 * call function to save / update networkPath
							 */
							boolean flag = CDRomDAO.saveNetworkPath(sessionBean
									.getFieldDetails());
							if (flag == true) {
								if (sessionBean.isShowSave()) {
									sessionBean
											.setSuccessMessage(msgProps
													.addMessage(
															"settings.save.success.message",
															msgProps.getProperty("label.settings.network")));
								} else if (sessionBean.isShowUpdate()) {
									sessionBean
											.setSuccessMessage(msgProps
													.addMessage(
															"settings.update.success.message",
															msgProps.getProperty("label.settings.network")));
								}

								/*
								 * call function to getNetwork Path again
								 */
								getSettingDetails(sessionBean);
							} else {
								sessionBean.setErrorMessage(msgProps
										.getProperty("error.message.faiure"));
							}
						} else {
							sessionBean
									.setErrorMessage(msgProps
											.getProperty("error.message.fail.settings.sourcepath.update"));
						}
					}
				} else if (request.getParameter("MDM_SET_ActionClicked")
						.equals("RESET_MAPPING")) {
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setShowSave(false);
					sessionBean.setShowUpdate(false);

					/*
					 * call function to getNetworkPath
					 */
					getSettingDetails(sessionBean);
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(
					CDRomSettingsServlet.class.getName(), "doPost()", e);
		}
		if (useReqDis == true) {
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS
			 * NULL REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if (null == userSessionBean.getTopMenuList()
					|| userSessionBean.getTopMenuList().size() <= 0
					|| (null != userSessionBean.getTopMenuList() && userSessionBean
							.getTopMenuList().size() == 1)) {
				response.sendRedirect(request.getContextPath() + "/mypage");
			} else {
				RequestDispatcher rs = request
						.getRequestDispatcher("/jsps/cdrom/settings.jsp");
				rs.forward(request, response);
			}
		}
	}

	private CDRomSettingsBean getSessionBean(HttpServletRequest request) {
		CDRomSettingsBean sessionBean = null;
		if (null != request.getSession().getAttribute("settingsBean")
				&& !"".equals(request.getSession().getAttribute("settingsBean"))) {
			sessionBean = (CDRomSettingsBean) request.getSession()
					.getAttribute("settingsBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CDRomSettingsBean();
			request.getSession().setAttribute("settingsBean", sessionBean);
		}
		return sessionBean;
	}

	private static void getSettingDetails(CDRomSettingsBean sessionBean) {
		try {
			sessionBean.getFieldDetails().setNetworkPath(null);
			sessionBean.setShowSave(true);
			sessionBean.setShowUpdate(false);
			if (null != sessionBean.getFieldDetails()
					&& null != sessionBean.getFieldDetails().getUserId()
					&& !"".equals(sessionBean.getFieldDetails().getUserId())) {
				CDRomSettingDetails sDetails = CDRomDAO
						.getSettingsData(sessionBean.getFieldDetails()
								.getUserId());
				if (null != sDetails && !"".equals(sDetails)
						&& null != sDetails.getNetworkPath()
						&& !"".equals(sDetails.getNetworkPath())) {
					sessionBean.getFieldDetails().setNetworkPath(
							sDetails.getNetworkPath());
					// set showUpdate to true && showSave to false
					sessionBean.setShowSave(false);
					sessionBean.setShowUpdate(true);
				}
				sDetails = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean validate(CDRomSettingsBean sessionBean) {
		if (null == sessionBean.getFieldDetails()
				|| null == sessionBean.getFieldDetails().getUserId()
				|| "".equals(sessionBean.getFieldDetails().getUserId())
				|| null == sessionBean.getFieldDetails().getNetworkPath()
				|| "".equals(sessionBean.getFieldDetails().getNetworkPath())) {
			sessionBean.setErrorMessage(msgProps
					.getProperty("error.message.mandatory.fields"));
			return false;
		}

		if (null != sessionBean.getFieldDetails()
				&& null != sessionBean.getFieldDetails().getUserId()
				&& !"".equals(sessionBean.getFieldDetails().getUserId())) {
			if (sessionBean.getFieldDetails().getUserId().trim().length() > 100) {
				String data = msgProps.getProperty("label.userid") + ",100";
				String[] id = data.split(",");
				sessionBean.setErrorMessage(msgProps.getMessage(id,
						"error.length.greater.characters"));
				data = null;
				id = null;
				return false;
			}
		}

		if (null != sessionBean.getFieldDetails()
				&& null != sessionBean.getFieldDetails().getNetworkPath()
				&& !"".equals(sessionBean.getFieldDetails().getNetworkPath())) {
			if (sessionBean.getFieldDetails().getNetworkPath().trim().length() > 300) {
				String data = msgProps.getProperty("label.networkpath")
						+ ",300";
				String[] id = data.split(",");
				sessionBean.setErrorMessage(msgProps.getMessage(id,
						"error.length.greater.characters"));
				data = null;
				id = null;
				return false;
			}
		}

		return true;
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

	private void performAccessCheck(CDRomSettingsBean sessionBean,
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
					CDRomSettingsServlet.class.getName(),
					"performAccessCheck()", e);
		}
	}

}
