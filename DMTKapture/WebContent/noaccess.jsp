<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
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
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<%
	String locale=ApplicationProperties.getProperty("locales.values.english");
	Object localeObj=request.getSession().getAttribute("DMT_LS_Locale");
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
<fmt:setBundle basename="com.mazda.gms3.dmt.properties.nl.dmtresource_en" />
<% 		
	} else if(locale.equals(ApplicationProperties.getProperty("locales.values.japanese")))
	{
%>
<fmt:setBundle basename="com.mazda.gms3.dmt.properties.nl.dmtresource_jp" />
<% 		
	}
%>
<jsp:useBean id="userAccessBean" class="com.mazda.gms3.dmt.bean.UserAccessBean" scope="session"></jsp:useBean>
<title><fmt:message key="label.title" /> - <fmt:message key="label.errorcode.403" /> - <fmt:message key="label.forbidden" />  </title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
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

</head>
<jsp:include page="jsps/headeragent.jsp" flush="true" />
<body>
<!-- loader Starts here  -->
<div class="ui-overlay" style="z-index:9999;position:absolute" id="loader">
	<div class="ui-widget-overlay"></div>
	<div class="ui-widget-shadow ui-corner-all" style="position: absolute; top: 310px; left: 504.5px; width: 300px; height: 37px;">
	</div>
	<div class="loadmask-msg ui-widget ui-widget-content ui-corner-all" style="text-align:center; position: absolute; padding: 10px; top: 310px; left: 504.5px; width:278px;"><div class="ui-overlay-loading"><fmt:message key="label.window.loding.message" /></div></div>
</div>
<!--- Loader Ends here -->
<div id="mainWrapper">
	<!-- CHANGE LANGUAGE MENU STARTS -->
	<jsp:include page="jsps/languageSelector.jsp" flush="true" />
	<!-- CHANGE LANGUAGE MENU ENDS -->
	 <!-- TOP MENU STARTS -->
	<div class="topMenu">
		<div class="logout" onclick="dmt_logout();"><span><i class="logoutIcon"></i><fmt:message key="label.logout" /> </span></div>
		<ul><li></li></ul>
		<div class="cb"></div>
	</div>
          <!-- TOP MENU ENDS -->
	 <div class="padder">
         <div class="contentBox">
           	<div class="convertInfobox">
               	<div class="managerInfo">
				<div class="managerSettings">
					<table width="100%" cellspacing="1" cellpadding="3"  align="center">
						<tr>
							<td style="text-align: center;">
								<div style="font-size: 100px">&nbsp;</div>
								<!-- <div style="font-size: 100px"><fmt:message key="label.errorcode.403" /></div>
								<div style="font-size: 60px;"><fmt:message key="label.forbidden" /> <fmt:message key="label.access" /></div> -->
								<div style="font-size: 24px;margin: 30px;"><fmt:message key="label.no.access.application.info" /></div>
							</td>
						</tr>
					</table>
				</div>
			</div>
		</div>
	</div>					
</div>			

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="jsps/footer.jsp" flush="true" />
<!-- FOOTER ENDS -->
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});
</script>

</html>