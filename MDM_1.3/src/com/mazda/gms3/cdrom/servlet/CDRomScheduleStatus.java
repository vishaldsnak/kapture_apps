package com.mazda.gms3.cdrom.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.cdrom.bean.CDRomScheduleDetails;
import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.bean.CDRomSearchBean;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;

public class CDRomScheduleStatus extends HttpServlet {

	static Logger logger = LogManager.getLogger(CDRomScheduleStatus.class);
	String moduleRefKey = AccessManagementInterface.REF_KEY_CD_CREATION;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String wslId = "";
		boolean useReqDis = true;

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
		CDRomSearchBean searchBean = getSessionBean(request);
		performAccessCheck(searchBean, request);
		if (searchBean.isShowReadControls() == false
				&& searchBean.isShowWriteControls() == false) {
			// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
			response.sendRedirect(request.getContextPath() + "/noaccess");
			useReqDis = false;
		}

		ArrayList<CDRomScheduleDetails> scDetails = new ArrayList<CDRomScheduleDetails>();
		ArrayList<CDRomScheduleItemDetails> scItemDetails = new ArrayList<CDRomScheduleItemDetails>();

		try {
			if (searchBean.getScheduleID() != null) {
				scDetails = CDRomDAO.getScheduleDetails(searchBean
						.getScheduleID(),null,null);
				scItemDetails = CDRomDAO.getScheduleItemDetails(searchBean
						.getScheduleID());
				CDRomScheduleDetails scDetail = null;
				if (scDetails != null && scDetails.size() > 0) {
					for (int i = 0; i < scDetails.size(); i++) {
						scDetail = scDetails.get(i);
						scDetail.setItemsList(scItemDetails);
					}
				}
				request.setAttribute("scDetails", scDetail);
			}
		} catch (SQLException e) {
			logger.info("doGet :: ################ Exception ################");
			Utilities.printStackTraceToLogs(
					CDRomScheduleStatus.class.getName(), "doGet()", e);
			logger.info("doGet :: ################ Exception ################");
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
						.getRequestDispatcher("/jsps/cdrom/cdromschedulestatus.jsp");
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
					CDRomScheduleStatus.class.getName(),
					"performAccessCheck()", e);
		}
	}

}
