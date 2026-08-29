<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
	String locale=ApplicationProperties.getProperty("locales.values.english");
	Object localeObj=request.getSession().getAttribute("MDM_LS_Locale");
	if(null!=localeObj && !"".equals(localeObj))
	{
		locale = (String)localeObj;
	}
	localeObj = null;
%>
<%
	if(locale.equals(ApplicationProperties.getProperty("locales.values.english")))
	{
%>
<fmt:setBundle basename="com.mazda.gms3.mdm.properties.nl.mdmresource_en" />
<% 		
	} else if(locale.equals(ApplicationProperties.getProperty("locales.values.japanese")))
	{
%>
<fmt:setBundle basename="com.mazda.gms3.mdm.properties.nl.mdmresource_jp" />
<% 		
	}
%>


<jsp:useBean id="userAccessBean" class="com.mazda.gms3.mdm.bean.UserAccessBean" scope="session"></jsp:useBean>
 <!-- TOP MENU STARTS -->
			<div class="topMenu">
				<div class="logout" onclick="mdm_logout();"><span><i class="logoutIcon"></i><fmt:message key="label.logout" /> </span></div>
				<ul>
					<% int opCount=0; %>
					<c:if test="${!empty userAccessBean.topMenuList }">
						<c:forEach items="${userAccessBean.topMenuList }" var="topMenuList">
							<li class="<% if(opCount==0) { %> margin-left-20 <% } %> <c:if test="${PAGE_NAME eq topMenuList.moduleRefkey }">activeLink</c:if>"><a href="<%=request.getContextPath() %><c:out value="${topMenuList.modulePath }"/>"><c:out value="${topMenuList.moduleDisplayName }" /> </a></li>
							<% opCount++; %>
						</c:forEach>
					</c:if>	
					
					<!-- ONLY WHEN VIEWING MY PAGE -->
					<c:if test="${PAGE_NAME eq userAccessBean.dashboardRefKey }">
						<c:if test="${!empty userAccessBean.userDisplayName }">
							<li class="welcome"><div><fmt:message key="label.dash.welcome" /> <c:out value="${userAccessBean.userDisplayName}" /> !!!</div></li>
						</c:if>
					</c:if>
				</ul>
				<div class="cb"></div>
			</div>    
            <!-- TOP MENU ENDS -->