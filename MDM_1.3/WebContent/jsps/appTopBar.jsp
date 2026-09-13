<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
	APPLICATION TOP BAR - THE MERGED HEADER + NAVIGATION BAR FROM THE FINALIZED MOCKUP.

	IT REPLACES languageSelector.jsp (BRAND HEADER) + topMenu.jsp (NAVIGATION) ON MY PAGE.
	IT IS WRITTEN AS A STANDALONE INCLUDE SO THE REMAINING SCREENS CAN ADOPT IT LATER WITHOUT
	ANY CHANGE HERE - THEY ONLY HAVE TO SWAP THEIR TWO INCLUDES FOR THIS ONE AND LINK
	css/mdm-dashboard.css.

	THE NAVIGATION ROW IS DRIVEN BY THE SAME userAccessBean.topMenuList topMenu.jsp USES, AND IT
	IS SUPPRESSED WHEN THE ONLY ENTRY IS MY PAGE ITSELF - WHICH IS THE CASE ON THE DASHBOARD, SO
	THE BAR THERE LOOKS EXACTLY LIKE THE MOCKUP.

	REQUIRES - THE INCLUDING PAGE MUST HAVE ALREADY SET THE PAGE_NAME REQUEST ATTRIBUTE AND MUST
	LINK css/mdm-dashboard.css. mdm_logout() COMES FROM footer.jsp.
--%>
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

<%--
	AVATAR INITIALS - FIRST LETTER OF THE FIRST TWO WORDS OF THE DISPLAY NAME, UPPERCASED.
	FALLS BACK TO A SINGLE DASH SO THE CIRCLE IS NEVER EMPTY.
--%>
<%
	String displayName = "";
	Object uaBeanObj = request.getSession().getAttribute("userAccessBean");
	if(null!=uaBeanObj && uaBeanObj instanceof com.mazda.gms3.mdm.bean.UserAccessBean)
	{
		String nameVal = ((com.mazda.gms3.mdm.bean.UserAccessBean)uaBeanObj).getUserDisplayName();
		if(null!=nameVal)
		{
			displayName = nameVal.trim();
		}
		nameVal = null;
	}
	uaBeanObj = null;

	String initials = "-";
	if(!"".equals(displayName))
	{
		StringBuffer sb = new StringBuffer();
		String[] parts = displayName.split("[\\s._@-]+");
		for(int i=0; i<parts.length && sb.length()<2; i++)
		{
			if(null!=parts[i] && parts[i].length()>0)
			{
				sb.append(parts[i].substring(0,1));
			}
		}
		if(sb.length()>0)
		{
			initials = sb.toString().toUpperCase();
		}
		sb = null;
		parts = null;
	}
%>

<!-- APPLICATION TOP BAR STARTS -->
<div class="mdmTopBar">
	<div class="tbInner">
		<div class="tbLeft">
			<span class="tbLogo"><img src="images/u7.png" alt="Mazda" /></span>
			<span class="tbDivider"></span>
			<span class="tbTitle"><fmt:message key="mdm.title.desc" /></span>
		</div>
		<div class="tbNav">
			<%-- ONLY WORTH A NAV ROW WHEN THERE IS SOMEWHERE OTHER THAN MY PAGE TO GO --%>
			<c:if test="${!empty userAccessBean.topMenuList && fn:length(userAccessBean.topMenuList) > 1}">
				<ul>
					<c:forEach items="${userAccessBean.topMenuList}" var="topMenuItem">
						<li class="<c:if test="${PAGE_NAME eq topMenuItem.moduleRefkey}">activeLink</c:if>"><a
							href="<%=request.getContextPath()%><c:out value="${topMenuItem.modulePath}"/>"><c:out
									value="${topMenuItem.moduleDisplayName}" /></a></li>
					</c:forEach>
				</ul>
			</c:if>
		</div>
		<div class="tbRight">
			<a class="tbAction" href="javascript:void(0);" onclick="mdm_logout();"><fmt:message
					key="label.logout" /></a>
			<%--
				ACCESS MANAGEMENT - super admins only. It is no longer a My Page tile; it sits here as
				a gear icon between Logout and the profile. It opens the SAME screen the old SETTINGS
				tile did, THROUGH THE SAME NAVIGATION: mdm_navigateSomeWhere('SETTINGS', ...) submits
				MDM_DashboardForm, and the Dashboard servlet's SETTINGS branch builds the top menu and
				redirects to /accessmanagement. A bare link to /accessmanagement CANNOT be used -
				AccessManagement.doGet bounces to /mypage when the top menu holds only My Page, which
				is the dashboard's state. So this depends on mdm_navigateSomeWhere + MDM_DashboardForm
				being present, exactly as the tile it replaces did (both live on My Page/dashboard.jsp).
				The fa-cog glyph needs font-awesome, already linked by the including page; the title is
				the text fallback for accessibility.
			--%>
			<c:if test="${userAccessBean.superAdminUser eq true}">
				<a class="tbAction tbActionIcon" href="javascript:void(0);"
						onclick="mdm_navigateSomeWhere('SETTINGS','','');"
						title="<fmt:message key="label.dash.ACCESS_MANAGEMENT" />"
						aria-label="<fmt:message key="label.dash.ACCESS_MANAGEMENT" />"><i
						class="fa fa-cog"></i></a>
			</c:if>
			<span class="tbUser">
				<span class="tbAvatar"><%=initials%></span>
				<span class="tbUserMeta">
					<b><c:out value="${userAccessBean.userDisplayName}" /></b>
					<c:choose>
						<c:when test="${userAccessBean.superAdminUser eq true}">
							<span><fmt:message key="label.dash.role.superadmin" /></span>
						</c:when>
						<c:otherwise>
							<span><fmt:message key="label.dash.role.user" /></span>
						</c:otherwise>
					</c:choose>
				</span>
			</span>
		</div>
	</div>
</div>
<!-- APPLICATION TOP BAR ENDS -->
