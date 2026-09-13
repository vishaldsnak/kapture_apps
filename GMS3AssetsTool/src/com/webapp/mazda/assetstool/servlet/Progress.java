package com.webapp.mazda.assetstool.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webapp.mazda.assetstool.util.GenerateFinalResponse;

/**
 * Servlet implementation class Progress
 */
public class Progress extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Progress() {
		super();
		// TODO Auto-generated constructor stub
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
			if (null != request.getParameter("MS3_GET_PROGRESS")
					&& !"".equals(request.getParameter("MS3_GET_PROGRESS"))) {
				String token = "";
				String responseString = "";
				Object obj = request.getSession().getAttribute(
						"testProgressListener");
				if (null != obj && !"".equals(obj)) {
					token = "DATA";
					TestProgressListener test = (TestProgressListener) obj;
					responseString = String.valueOf(test.getPercentDone());
					test = null;
				} else {
					token = "ERROR";
					responseString = "";
				}
				obj = null;

				/*
				 * Call function to generate finalResponse
				 */
				GenerateFinalResponse.generateFinalResponse(response, token,
						responseString);
				token = null;
				responseString = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
