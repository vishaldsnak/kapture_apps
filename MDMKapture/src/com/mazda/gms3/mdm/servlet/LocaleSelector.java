package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;




import com.mazda.gms3.mdm.bean.LocaleSelectorBean;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.GenerateFinalResponse;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class LocaleSelector
 */
public class LocaleSelector extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LocaleSelector() {
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
			LocaleSelectorBean sessionBean = getSessionBean(request);
			if(null!=request.getSession().getAttribute("MDM_LS_Locale") && !"".equals(request.getSession().getAttribute("MDM_LS_Locale")))
			{
				sessionBean.setSelectedLocale((String)request.getSession().getAttribute("MDM_LS_Locale"));
			}
			
			request.getSession().setAttribute("data", request.getParameter("test"));
			
			/*
			 * call function to load localesList
			 */
			getLocalesList(sessionBean);
			
			if(null!=request.getParameter("MDM_LOCALE_GO") && !"".equals(request.getParameter("MDM_LOCALE_GO")))
			{
				String token = "";
				String responseString = "";
				
				/*
				 * change the selected value in Locale
				 */
				sessionBean.setSelectedLocale(null);
				request.getSession().removeAttribute("MDM_LS_Locale");
				if(null!=request.getParameter("MDM_LOCALE_SEL_VALUE") && !"".equals(request.getParameter("MDM_LOCALE_SEL_VALUE")))
				{
					token = "SUCCESS";
					responseString = "FORM_RELOAD";
					/*
					 * set selectedLocale in sessionBean
					 * and also in HTTP Session / Update the selected locale value
					 */
					sessionBean.setSelectedLocale((String)request.getParameter("MDM_LOCALE_SEL_VALUE"));
					request.getSession().setAttribute("MDM_LS_Locale", sessionBean.getSelectedLocale());
				}
				else
				{
					token = "ERROR";
					responseString = "";
				}
				GenerateFinalResponse.generateFinalResponse(response,token, responseString);
				token = null;
				responseString = null;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private LocaleSelectorBean getSessionBean(HttpServletRequest request) 
	{
		LocaleSelectorBean sessionBean = null;
		if (null != request.getSession().getAttribute("localeSelectorBean") && 
				!"".equals(request.getSession().getAttribute("localeSelectorBean"))) 
		{
			sessionBean = (LocaleSelectorBean) request.getSession().getAttribute("localeSelectorBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new LocaleSelectorBean();
			request.getSession().setAttribute("localeSelectorBean", sessionBean);
		}
		return sessionBean;
	}
	
	private static void getLocalesList(LocaleSelectorBean sessionBean)
	{
		sessionBean.setLocaleList(new ArrayList<SelectItemDetails>());
		
		StringTokenizer labels = new StringTokenizer(ApplicationProperties.getProperty("locales.labels"), ",");
		StringTokenizer values = new StringTokenizer(ApplicationProperties.getProperty("locales.values"), ",");
		while(labels.hasMoreTokens() && values.hasMoreTokens())
		{
			SelectItemDetails si = new SelectItemDetails();
			si.setLabel(labels.nextToken());
			si.setValue(values.nextToken());
			sessionBean.getLocaleList().add(si);
			si = null;
		}
		labels=null;
		values= null;
	}
}
