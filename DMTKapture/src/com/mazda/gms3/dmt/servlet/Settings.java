package com.mazda.gms3.dmt.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.bean.SettingsBean;
import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.dao.SettingsDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.SettingDetails;

/**
 * Servlet implementation class Settings
 */
public class Settings extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(Settings.class);
	static MessageProperties msgProps = null;
	static String wslId = "";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Settings() {
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
					"DMT_LS_Locale"));
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
			SettingsBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setShowSave(false);
			sessionBean.setShowUpdate(false);

			sessionBean.setFieldDetails(new SettingDetails());
			sessionBean.getFieldDetails().setUserId(wslId);
			/*
			 * call function to get Network Path on the basis of User Id
			 */
			getSettingDetails(sessionBean);

		} catch (Exception e) {
			Utilities.printStackTraceToLogs(Settings.class.getName(),
					"doGet()", e);
		}
		if (useReqDis == true) {
			RequestDispatcher rs = request
					.getRequestDispatcher("/jsps/settings.jsp");
			rs.forward(request, response);
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
					"DMT_LS_Locale"));
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
			SettingsBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			if (null == sessionBean.getFieldDetails()) {
				sessionBean.setFieldDetails(new SettingDetails());
			}
			// set userId
			sessionBean.getFieldDetails().setUserId(wslId);

			if (null != request.getParameter("DMT_SET_ActionClicked")
					&& !"".equals(request.getParameter("DMT_SET_ActionClicked"))) {
				if (request.getParameter("DMT_SET_ActionClicked").equals(
						"SAVE_MAPPING")) {
					sessionBean.getFieldDetails().setNetworkPath(null);
					if (null != request.getParameter("DMT_SET_NetworkPath")
							&& !"".equals(request
									.getParameter("DMT_SET_NetworkPath"))) {
						sessionBean.getFieldDetails().setNetworkPath(
								(String) request
										.getParameter("DMT_SET_NetworkPath"));
					}

					/*
					 * validate path & userId
					 */
					if (validate(sessionBean)) {
						/*
						 * CHECK IF THERE'S ANY JOB UNDER PROCESSING STATE DO
						 * NOT ALLOW TO UPDATE THE PATH. DATE - 14TH NOVEMBER
						 * 2016
						 */
						int count = ScheduleDAO.getPendingProcessingJobsCount();
						if (count <= 0) {
							/*
							 * call function to save / update networkPath
							 */
							boolean flag = SettingsDAO
									.saveNetworkPath(sessionBean
											.getFieldDetails());
							if (flag == true) {
								if (sessionBean.isShowSave()) {
									sessionBean
											.setSuccessMessage(msgProps
													.addMessage(
															"settings.save.success.message",
															msgProps.getProperty("label.networkpath")));
								} else if (sessionBean.isShowUpdate()) {
									sessionBean
											.setSuccessMessage(msgProps
													.addMessage(
															"settings.update.success.message",
															msgProps.getProperty("label.networkpath")));
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
				} else if (request.getParameter("DMT_SET_ActionClicked")
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
			Utilities.printStackTraceToLogs(Settings.class.getName(),
					"doPost()", e);
		}
		if (useReqDis == true) {
			RequestDispatcher rs = request
					.getRequestDispatcher("/jsps/settings.jsp");
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
	
	private SettingsBean getSessionBean(HttpServletRequest request) {
		SettingsBean sessionBean = null;
		if (null != request.getSession().getAttribute("settingsBean")
				&& !"".equals(request.getSession().getAttribute("settingsBean"))) {
			sessionBean = (SettingsBean) request.getSession().getAttribute(
					"settingsBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SettingsBean();
			request.getSession().setAttribute("settingsBean", sessionBean);
		}
		return sessionBean;
	}

	private static void getSettingDetails(SettingsBean sessionBean) {
		try {
			sessionBean.getFieldDetails().setNetworkPath(null);
			sessionBean.setShowSave(true);
			sessionBean.setShowUpdate(false);
			if (null != sessionBean.getFieldDetails()
					&& null != sessionBean.getFieldDetails().getUserId()
					&& !"".equals(sessionBean.getFieldDetails().getUserId())) {
				SettingDetails sDetails = SettingsDAO
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
			Utilities.printStackTraceToLogs(Settings.class.getName(),
					"getSettingDetails()", e);
		}
	}

	private static boolean validate(SettingsBean sessionBean) {
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
}
