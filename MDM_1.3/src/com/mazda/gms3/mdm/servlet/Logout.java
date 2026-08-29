package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * Servlet implementation class Logout
 */
public class Logout extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static Logger logger = LogManager.getLogger(Logout.class);
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Logout() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try
		{
			Enumeration<String> e =request.getSession().getAttributeNames();
			if(null!=e && e.hasMoreElements()==true)
			{
				logger.info("############### doPost :: Proceed for removing All Session Attributes.");
				while(e.hasMoreElements())
				{
					String ele = e.nextElement();
					if(null!=ele && !"".equals(ele))
					{
						request.getSession().removeAttribute(ele);
					}
					ele = null;
				}
			}
			e = null;
			request.getSession(false).setMaxInactiveInterval(0);
			request.getSession(false).invalidate();
			if(null!=request.getCookies())
			{
				for (Cookie cookie : request.getCookies()) {
					cookie.setValue("");
					cookie.setMaxAge(0);
					cookie.setPath("/");
					response.addCookie(cookie);
				}
			}
			
			
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Logout.class.getName(), "doPost()", e);
		}
	}
}
