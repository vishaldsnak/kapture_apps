package com.mazda.gms3.dmt.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.Logger;

import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.dao.UserProfileDAO;
import com.mazda.gms3.dmt.vo.UserProfileDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * Servlet Filter implementation class AccessManagementFilter
 */
public class AccessManagementFilter implements Filter {

	private Logger logger = Logger.getLogger(AccessManagementFilter.class);
	
	private ArrayList<String> rolesList = new ArrayList<String>();
	MessageProperties msgProps= null;
    /**
     * Default constructor. 
     */
    public AccessManagementFilter() {
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String wslId="";
		boolean reDirectToError = false;
		UserAccessBean sessionBean = getSessionBean((HttpServletRequest)request);

		/*
		 * If the container does not put an "iv-user" header on the request (no WebSEAL in
		 * front of the application), inject the user configured as wsl.default.user by
		 * wrapping the request. Everything downstream - this filter, every servlet's
		 * wsl.check block and every JSP - keeps reading the header exactly as before.
		 * The PROFILE of that user is still read from Kapture. Leave the property BLANK
		 * to use the real iv-user header only.
		 */
		String defaultUser = ApplicationProperties.getProperty("wsl.default.user");
		if(null!=defaultUser && !"".equals(defaultUser.trim())
				&& null==((HttpServletRequest)request).getHeader("iv-user"))
		{
			request = new DefaultUserRequestWrapper((HttpServletRequest)request, defaultUser.trim());
		}
		defaultUser = null;

		try
		{
			if(null!=((HttpServletRequest)request).getHeader("iv-user"))
			{
				wslId = (String)((HttpServletRequest)request).getHeader("iv-user");
			}
			
			// EXPLICITY MAKE WSLID TO LOWERCASE
			if(null!=wslId && !"".equals(wslId))
			{
				wslId = wslId.trim().toLowerCase();
			}
			msgProps = new MessageProperties(((HttpServletRequest)request).getSession().getAttribute("DMT_LS_Locale"));
			
			
			if(null!=wslId && !"".equals(wslId))
			{
//				logger.info("doFilter :: Logged In user is :: > " + wslId);
				
				// call functionTo Populate Roles List
				getRolesList();
				
				/*
				 * FETCH THE USER ROLE MAPPED WITH THE USER, ALONG WITH THE CONTENT LOCALES
				 * AND DEFAULT LOCALES.
				 * IF USER IS SUPER ADMIN, THEN POPULATE USER ACCESS LIST WITH ALL THE MODULES HAVING READ & WRITE ACCESS
				 * IF USER IS NOT SUPER ADMIN, THEN FETCH ROLE BASED ACCESS FOR MODULES FOR THE USER AND SET IN USER ACCESS LIST 
				 */
				boolean rePopulateBean = true;
				if(null!=sessionBean.getLoggedInUserId() && !"".equals(sessionBean.getLoggedInUserId()))
				{
					if(sessionBean.getLoggedInUserId().trim().toLowerCase().equals(wslId.trim().toLowerCase()))
					{
						rePopulateBean = false;
					}
				}
				
				if(rePopulateBean==true)
				{
					// RESET BEAN
					resetBean(sessionBean);
					
					// update WSL ID in sessionBean
					sessionBean.setLoggedInUserId(wslId);
					// FETCH USER PROFILE DETAILS FROM KAPTURE.
					UserProfileDetails userDetails = null;
					try
					{
						userDetails = UserProfileDAO.getUserProfileFromCMS(wslId);
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(AccessManagementFilter.class.getName(), "doFilter()", e);
					}
					
					if(null!=userDetails && null!=userDetails.getDefaultLocale() && !"".equals(userDetails.getDefaultLocale())
							&& null!=userDetails.getRoleRefKeys())
					{
						addUserProfileDetailsToSessionBean(sessionBean, userDetails);
					}
					else
					{
						logger.info("doFilter :: Failed to Fetch User Profile Details from Kapture for {"+wslId+"}. Return to Error.");
						// RESET USER BEAN
						resetBean(sessionBean);
						/*
						 * REDIRECT TO ERROR.
						 */
						reDirectToError = true;
					}
					userDetails = null;
				}
			}
			else
			{
//				logger.info("doFilter :: WSL ID from Request is null. Return to Error.");
				
				// RESET USER BEAN
				resetBean(sessionBean);
				/*
				 * REDIRECT TO ERROR.
				 */
				reDirectToError = true;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagementFilter.class.getName(), "doFilter()", e);
		}
		
		if(reDirectToError == true)
		{
//			((HttpServletResponse) response).sendRedirect(((HttpServletRequest)request).getContextPath()+"/error");
		}
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

	private UserAccessBean getSessionBean(HttpServletRequest request) 
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
	
	private void resetBean(UserAccessBean sessionBean)
	{
		sessionBean.setUserDisplayName(null);
		sessionBean.setLoggedInUserId(null);
		// SET DEFAULT VALUE
		sessionBean.setShowNoAccess(true);
		sessionBean.setMappedRolesList(null);
	}
	
	
	private void addUserProfileDetailsToSessionBean(UserAccessBean sessionBean, UserProfileDetails userDetails)
	{
		try
		{
			// SET USER DISPLAY NAME
			if(null!=userDetails && null!=userDetails.getFirstName() && !"".equals(userDetails.getFirstName()))
			{
				sessionBean.setUserDisplayName(userDetails.getFirstName());
			}
			else
			{
				sessionBean.setUserDisplayName("");
			}
			
			// ADD MAPPED ROLES
			sessionBean.setMappedRolesList(new ArrayList<String>());
			List<String> mappedRolesList = userDetails.getRoleRefKeys();
			if(null!=mappedRolesList && mappedRolesList.size()>0)
			{
				for(int i=0;i<mappedRolesList.size();i++)
				{
					String mappedRoleKey = (String)mappedRolesList.get(i);
					if(null!=mappedRoleKey && !"".equals(mappedRoleKey))
					{
						sessionBean.getMappedRolesList().add(mappedRoleKey);
					}
					mappedRoleKey = null;
				}
			}
			mappedRolesList = null;
			
			/*
			 * IDENTIFY WHETHER USER IS SUPER ADMIN OR NOT
			 * CHECK FOR THE TOP MOST PRIORITY OF THE ROLES MAPPED WITH  LOGGED IN USER
			 * IF PRIORITY = 1, THEN SUPER ADMIN ELSE FETCH ROLE BASED ACCESS WITH TOP PRIORITY 
			 * 
			 * NOTE - HERE TOP PRIORITY IS THE ONE WITH LEAST VALUE
			 */
			if(null!=sessionBean.getMappedRolesList() && sessionBean.getMappedRolesList().size()>0 
					&& null!=rolesList && rolesList.size()>0)
			{
				for(int a=0;a<sessionBean.getMappedRolesList().size();a++)
				{
					for(int b=0;b<rolesList.size();b++)
					{
						String roleRefKey = (String)rolesList.get(b);
						if(((String)sessionBean.getMappedRolesList().get(a).trim().toLowerCase()).equals(roleRefKey.trim().toLowerCase()))
						{
							// USER HAS ACCESS TO DMT APPLICATION.
							sessionBean.setShowNoAccess(false);
							break;
						}
						roleRefKey = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagementFilter.class.getName(), "addUserProfileDetailsToSessionBean()", e);
		}
	}
	
	private void getRolesList()
	{
		try
		{
			if(null==rolesList || rolesList.size()<=0)
			{
				rolesList = new ArrayList<String>();
				StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("DMT_ALLOWED_ACCESS_ROLES"),",");
				while(str.hasMoreTokens())
				{
					String token = str.nextToken();
					rolesList.add(token);
					token = null;
				}
				str = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AccessManagementFilter.class.getName(), "getRolesList()", e);
		}
	}

	/**
	 * Wraps the request so that getHeader("iv-user") returns the configured default user
	 * when the container did not supply that header. Only the iv-user header is overridden;
	 * every other header behaves exactly as before.
	 */
	private static class DefaultUserRequestWrapper extends HttpServletRequestWrapper {

		private static final String IV_USER = "iv-user";
		private String defaultUser = null;

		public DefaultUserRequestWrapper(HttpServletRequest request, String defaultUser) {
			super(request);
			this.defaultUser = defaultUser;
		}

		public String getHeader(String name) {
			if (null != name && IV_USER.equalsIgnoreCase(name)) {
				String value = super.getHeader(name);
				if (null == value || "".equals(value)) {
					return defaultUser;
				}
				return value;
			}
			return super.getHeader(name);
		}

		public java.util.Enumeration<String> getHeaders(String name) {
			if (null != name && IV_USER.equalsIgnoreCase(name)) {
				java.util.Enumeration<String> values = super.getHeaders(name);
				if (null == values || !values.hasMoreElements()) {
					java.util.List<String> injected = new ArrayList<String>();
					injected.add(defaultUser);
					return java.util.Collections.enumeration(injected);
				}
				return values;
			}
			return super.getHeaders(name);
		}

		public java.util.Enumeration<String> getHeaderNames() {
			java.util.List<String> names = new ArrayList<String>();
			java.util.Enumeration<String> existing = super.getHeaderNames();
			boolean hasIvUser = false;
			if (null != existing) {
				while (existing.hasMoreElements()) {
					String n = existing.nextElement();
					names.add(n);
					if (IV_USER.equalsIgnoreCase(n)) {
						hasIvUser = true;
					}
					n = null;
				}
			}
			if (hasIvUser == false) {
				names.add(IV_USER);
			}
			return java.util.Collections.enumeration(names);
		}
	}
}
