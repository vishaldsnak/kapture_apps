<%@page import="com.mazda.gms3.cdrom.bean.CDRomScheduleDetails"%>
<%@page import="com.mazda.gms3.mdm.vo.AccessManagementInterface"%>
<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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
CDRomScheduleDetails sessionBean = null;
if (null != request.getAttribute("scDetails") && !"".equals(request.getAttribute("scDetails"))) 
{
	sessionBean = (CDRomScheduleDetails) request.getSession().getAttribute("scDetails");
} 
else 
{
	// initialize the sessionBean and set it in HTTP Session
	sessionBean = new CDRomScheduleDetails();
	request.setAttribute("CDRomScheduleDetails", sessionBean);
}
 %>	
<html>
<head>
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<!-- 
	SET PAGE NAME
 -->
<%
	//request.setAttribute("PAGE_NAME", "STATUS");
	request.setAttribute("PAGE_NAME",AccessManagementInterface.REF_KEY_CD_CREATION);
%>

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

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.cdrom"/> </title>
<link rel="stylesheet" href="css/font-awesome/css/font-awesome.min.css" />
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="css/cdrom_style.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />
<style type="text/css">
.cursorPointer {
	cursor: pointer;
}
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

</style>
<meta http-equiv="refresh" content="15">
</head>
<jsp:include page="../headeragent.jsp" flush="true" />
<body>
<div id="mainWrapper">

			<!-- CHANGE LANGUAGE MENU STARTS -->
			<jsp:include page="../languageSelector.jsp" flush="true" />
			<!-- CHANGE LANGUAGE MENU ENDS -->
			
            <!-- TOP MENU STARTS -->
             <jsp:include page="../topMenu.jsp" flush="true" />
			<!-- TOP MENU ENDS -->
            
            <div class="breadcumbs">
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.cdcreation"/> &rsaquo;</a> 
            </div>
        
        <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        	<table width="100%">
									<tr>
										<td>
											<div class="status">
										        <table>
										        <c:if test="${empty requestScope.scDetails }">
										        	<tr><td><fmt:message key="label.schedule.message1"/> </td></tr>
										        </c:if>
										        <c:if test="${not empty requestScope.scDetails }">
										        	<tr><td>
										        	<label>
										        	<fmt:message key="label.jobname"/> ${requestScope.scDetails.scheduleName}
										        	</label></td>
										        	<tr><td>
										        	<label>
										        	<c:set var="scheduleStatus" value="Processing"/>
										        	<c:if test="${requestScope.scDetails.scheduleStatus eq '3'}">
										        		<c:set var="scheduleStatus" value="Completed"/>
										        	</c:if>
										        	<fmt:message key="label.job.status"/> ${scheduleStatus}
										        	</label></td></tr>
										        	<c:choose>
										        		<c:when test="${empty requestScope.scDetails.itemsList and requestScope.scDetails.scheduleStatus eq '3'}">
										        		        	<tr><td colspan="9"><fmt:message key="label.schedule.message2"/></td></tr>
										        		</c:when>
										        		<c:when test="${empty requestScope.scDetails.itemsList}">
										        		        	<tr><td colspan="9"><fmt:message key="label.schedule.message"/></td></tr>
										        		</c:when>
										        	</c:choose>
										        </table>
											        <c:if test="${not empty requestScope.scDetails.itemsList}">
												        <table class="historyTable">
													        <tr>
														        <td><fmt:message key="label.channelname"/></td>
														        <td><fmt:message key="label.totaldocs"/></td>
														        <td><fmt:message key="label.processeddocs"/></td>
														        <td><fmt:message key="label.faileddocs"/></td>
														        <td><fmt:message key="label.okasset.totaldocs"/></td>
														        <td><fmt:message key="label.okasset.processeddocs"/></td>
														        <td><fmt:message key="label.okaseet.faileddocs"/></td>
														        <td><fmt:message key="label.innerLink.total"/></td>
														        <td><fmt:message key="label.currentstatus"/></td>
													        </tr>
												        	<c:forEach var="item" items="${requestScope.scDetails.itemsList}">
												        		<tr>
												        		<td><label><c:out value="${item.manualTypeName }" /></label></td>
												        	
												        		<td><label><c:out value="${item.totalDocsForProcessing }" /></label></td>
												        		
												        		<td><label><c:out value="${item.processedDocsCount }" /></label></td>
												        	
												        		<td><label><c:out value="${item.failedProcessedDocsCount }" /></label></td>
												        		
												        		<td><label><c:out value="${item.okAssetsCount }" /></label></td>
												        		
												        		<td><label><c:out value="${item.processedOkAssetsCount }" /></label></td>
												        		
												        		<td><label><c:out value="${item.failedOkAssetsCount }" /></label></td>
												        		
												        		<td><label><c:out value="${item.totalInnerLinksCount }" /></label></td>
												        		
												        		<td><label><c:out value="${item.currentProcessingStatus }" /></label></td>
												        		</tr>
												        			
												        	</c:forEach>
												        </table>
											        </c:if>
										        </c:if>
												</div>
										</td>
									</tr>
							</table>
							</div>	
						</div>
					</div>
				</div>
			</div>					
        

 </div>   
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

</html>