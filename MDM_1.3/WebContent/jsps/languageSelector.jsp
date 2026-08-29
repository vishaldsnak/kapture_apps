<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
{
	response.setHeader("Cache-Control","no-cache"); //Forces caches to obtain a new copy of the page from the origin server
	response.setHeader("Cache-Control","no-store"); //Directs caches not to store the page under any circumstance
	response.setDateHeader("Expires", 0); //Causes the proxy cache to see the page as "stale"
	response.setHeader("Pragma","no-cache"); //HTTP 1.0 backward compatibility
	Object obj = request.getHeader("iv-user");
	if(null==obj || "".equals(obj))
	{
		response.sendRedirect(request.getContextPath()+"/error");
	}
}
 %>	
<jsp:include page="/localeselector"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
request.setCharacterEncoding("UTF-8");
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

<style>
	
	.header ul
	{
		float:right;
		list-style: none;
	}
	.header ul li
	{
		float: left;
		list-style: none;
		margin:15px;
		text-align: center;
		vertical-align: middle;
		position:relative;
	}
	.header ul li span
	{
		font-size: 23px;
		/*text-transform: uppercase;*/
		color:#555555;
	}
	
	.langIcon:after
	{
		cursor: pointer;
	}
	.header ul li div
	{
		margin:2px;
	}
	.header ul li div p
	{
		float: left;
		padding-top: 8px;
		padding-left: 5px;
		color: #5C5B65;
	    font-size: 9px;
	    border: none;
	    cursor: pointer;
	}
	
	.header ul li ul li {
		float:none;
		margin:5px 0px 15px 10px !important;
	}
	
	.header ul li ul
	{
		margin:0px 0px 15px 0px !important;
		float:right;
		position:absolute;
		left:-115px;
		top:100%;
		background-color:#fff;
		width:150px;
		box-shadow:0px 2px 4px;
		z-index:99;
	}
	
	.header ul li:hover ul
	{
		box-shadow:0px 2px 4px;
	} 
	.header ul li ul li
	{
		text-align:left;
	}
	
	.header ul li ul li select{
		height: 30px;margin-top: 2px; width:100px; font-family:"InterstateMazda-Regular",Arial;
		border:1px solid #DADADA;
		border-radius:2px;
		
	}
	
</style>
<!--[if IE]> 
<style type="text/css">
.header ul li ul
{
	border:1px solid #DADADA;
	left:-137px !important;
}
.header ul li:hover ul
{
	border:1px solid #DADADA;
}
.header ul
{
	margin-top:-59px !important;
}
.header ul li div p
{
	padding-top:5px !important;
}

</style>
 <![endif]-->
<jsp:useBean id="localeSelectorBean"
	class="com.mazda.gms3.mdm.bean.LocaleSelectorBean" scope="session"></jsp:useBean>
<script type="text/javascript">
	function mdm_showLangOptions(thisObj)
	{
		
		if($("#MDM_Locale_DropDown").hasClass("show"))
		{
			//$(thisObj).closest("div").removeClass("activeHeaderLi");
			$("#MDM_Locale_DropDown").removeClass("show");
			$("#MDM_Locale_DropDown").addClass("hide");
		}
		else
		{
		//	$(thisObj).closest("div").addClass("activeHeaderLi");
			$("#MDM_Locale_DropDown").addClass("show");
			$("#MDM_Locale_DropDown").removeClass("hide");
		}
	}
</script>	
<div class="header">
	<img src="images/u7.png" />
	<ul>
		<li><span><fmt:message key="mdm.title.desc"/></span></li>
		<!-- 
		<li>
		<div>
			<i class="langIcon"></i><p onclick="mdm_showLangOptions(this);">&#x25BC;</p>
			<ul id="MDM_Locale_DropDown" class="hide">
				<li>
					<select
								id="LS_Locale" name="LS_Locale">
								<c:if test="${!empty localeSelectorBean.localeList }">
									<c:forEach var="localeList" items="${localeSelectorBean.localeList }">
										<c:set var="selecetedLocaleFlag" value="" />
										<c:if test="${!empty localeSelectorBean.selectedLocale}">
											<c:if test="${localeSelectorBean.selectedLocale eq localeList.value}">
												<c:set var="selecetedLocaleFlag" value="1" />
											</c:if>
										</c:if>
										<c:choose>
											<c:when test="${selecetedLocaleFlag eq 1}">
												<option value="<c:out value="${localeList.value }" />"
													selected="selected"><c:out value="${localeList.label }" /></option>
											</c:when>
											<c:otherwise>
												<option value="<c:out value="${localeList.value }" />"><c:out
														value="${localeList.label }" /></option>
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</c:if>
							</select>
				</li>
				<li>
					 <input type="button" id="LS_Go" name="LS_Go" onclick="mdm_localeData();"
							value="<fmt:message key="label.apply"/>" class="bluebutton cursorPointer">
				<li>
			</ul>
			</div>
		</li>
		-->
	</ul>
</div>