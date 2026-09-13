package com.webapp.mazda.assetstool.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webapp.mazda.assetstool.util.ApplicationPropertiesUtil;

/**
 * Servlet implementation class Error
 */
public class Error extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Error() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		RequestDispatcher rs = request.getRequestDispatcher("Error.jsp");
		rs.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			if (null != request.getParameter("MS3_LOGIN_CONTINUE")
					&& !"".equals(request.getParameter("MS3_LOGIN_CONTINUE"))) {
				String redirectPath = ApplicationPropertiesUtil
						.getProperty("ERROR_REDIRECT_PATH");
				response.sendRedirect(redirectPath);
			} else {
				RequestDispatcher rs = request
						.getRequestDispatcher("Error.jsp");
				rs.forward(request, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			RequestDispatcher rs = request.getRequestDispatcher("Error.jsp");
			rs.forward(request, response);
		}
	}

}
