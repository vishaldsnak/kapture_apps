package com.mazda.gms3.dmt.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.utils.Utilities;

/**
 * Servlet implementation class Logout
 */
public class Logout extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Logout() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			@SuppressWarnings("unchecked")
			Enumeration<String> e = request.getSession().getAttributeNames();
			if (null != e && e.hasMoreElements() == true) {
				while (e.hasMoreElements()) {
					String ele = e.nextElement();
					if (null != ele && !"".equals(ele)) {
						request.getSession().removeAttribute(ele);
					}
					ele = null;
				}
			}
			e = null;
			request.getSession(false).setMaxInactiveInterval(0);
			request.getSession(false).invalidate();
			for (Cookie cookie : request.getCookies()) {
				cookie.setValue("");
				cookie.setMaxAge(0);
				cookie.setPath("/");
				response.addCookie(cookie);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(Logout.class.getName(), "doPost()",
					e);
		}
	}

}
