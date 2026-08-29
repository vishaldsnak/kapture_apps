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
 %>	
<html>
<head>
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<!-- 
	SET PAGE NAME
 -->
<%
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_DASHBOARD);
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

<jsp:useBean id="userAccessBean" class="com.mazda.gms3.mdm.bean.UserAccessBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.DASHBOARD"/> </title>
<link rel="stylesheet" href="css/font-awesome/css/font-awesome.min.css" />
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
<jsp:include page="headeragent.jsp" flush="true" />
</head>
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
	<jsp:include page="languageSelector.jsp" flush="true" />
	<!-- CHANGE LANGUAGE MENU ENDS -->
	
          <!-- TOP MENU STARTS -->
	 <jsp:include page="topMenu.jsp" flush="true" />
	<!-- TOP MENU ENDS -->
    <div class="quicklinks">
		<div class="breadcumbs">
			<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &rsaquo;</a>
		</div>
		<c:if test="${userAccessBean.superAdminUser eq true }">
			<div class="rightPannel">
				<a href="javascript:void(0);" onclick="mdm_navigationOperation('SETTINGS');"><i class="settingsIcon"></i> <fmt:message key="label.dash.settings" /> </a>
			</div>
		</c:if>
	</div>	
	<form id="MDM_DashboardForm" name="MDM_DashboardForm" action="<%=request.getContextPath() %>/mypage" method="post">
	<input type="hidden" name="DASH_TILE_CLICKED_VAL" id="DASH_TILE_CLICKED_VAL" value="" />
	<input type="hidden" name="DASH_TILE_CLICKED_REFKEY_VAL" id="DASH_TILE_CLICKED_REFKEY_VAL" value="" />
	<!--  CONTENT BLOCK STARTS HERE -->
	<div class="padder">
    	<div class="contentBox">
           	<div class="convertInfobox">
               	<div class="managerInfo">
                   	<div class="managerSettings">
						<hr style="border:1px solid #34495e;" />
						<div class="dash-items-container">
							<!--  items display starts here -->
							<c:if test="${!empty userAccessBean.defaultModulesList }">
								<c:forEach var="defaultModulesList" items="${userAccessBean.defaultModulesList }">
									<div class="dash-items" onclick="mdm_navigateSomeWhere('DEFAULT', '<c:out value="${defaultModulesList.moduleRefkey }"/>');">
										<p>
											<c:out value="${defaultModulesList.moduleDisplayName }" />
										</p>
									</div>
								</c:forEach>
							</c:if>
							
							<c:if test="${!empty userAccessBean.normalModulesList }">
								<c:forEach var="normalModulesList" items="${userAccessBean.normalModulesList }">
									<div class="dash-items" onclick="mdm_navigateSomeWhere('NORMAL', '<c:out value="${normalModulesList.moduleRefkey }"/>');">
										<p><c:out value="${normalModulesList.moduleDisplayName }" /></p>
									</div>
								</c:forEach>
							</c:if>
							
							<c:if test="${!empty userAccessBean.engineModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('ENGINE');">
									<p><fmt:message key="label.engine"/></p>	
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.missionModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('MISSION');">
									<p><fmt:message key="label.mission"/></p>
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.vehilceTypeModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('VEHICLE_TYPE');">
									<p><fmt:message key="label.vehicletype"/> </p>
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.sstMaintenanceModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('SST_MAINTENANCE');">
									<p><fmt:message key="label.dash.sstmaintenance"/></p>
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.sstVehicleTypeModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('SST_VEHICLE_MAINTENANCE');">
									<p><fmt:message key="label.dash.sstvehicletypemanagement"/></p>
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.cdCreationModulesList }">
								<div class="dash-items" onclick="mdm_navigationOperation('CD_CREATION');">
									<p><fmt:message key="label.dash.CD_CREATION"/></p>
								</div>
							</c:if>
							
							<c:if test="${!empty userAccessBean.rumVinMappingList }">
								<div class="dash-items" onclick="mdm_navigationOperation('RUMVIN');">
									<p><fmt:message key="label.rumvin"/></p>	
								</div>
							</c:if>
							<!--  items display ends here -->
							
							
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>			
	</form>								
	<!-- CONTENT BLOCK ENDS HERE -->
            
</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<script>
$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function mdm_navigateSomeWhere(navType, refKey)
{
	// show Loader
	$("#loader").show();
	document.getElementById("DASH_TILE_CLICKED_VAL").value=navType;
	document.getElementById("DASH_TILE_CLICKED_REFKEY_VAL").value=refKey;
	document.forms['MDM_DashboardForm'].submit();
}

function mdm_navigationOperation(navType)
{
	// show Loader
	$("#loader").show();
	document.getElementById("DASH_TILE_CLICKED_VAL").value=navType;
	document.forms['MDM_DashboardForm'].submit();
}

</script>

<script type="text/javascript">
var serverUrl="<%=request.getContextPath()%>/localeselector";

function mdm_localeData()
{
	var loc = $("#LS_Locale").val();
	// Make Ajax Call
	$.ajax({
		url:serverUrl,
		type:"post",
		data:{
			MDM_LOCALE_GO:"MDM_LOCALE_GO",
			MDM_LOCALE_SEL_VALUE:loc
		},
		dataType:"text",
		success:function(responseData){
			if(null != responseData)
			{
				var index = responseData.indexOf("<SCRIPT");
				if(index!=-1)
				{
					responseData = responseData.substring(0,index);
				}
				try
				{
					// remove extra /n/r and other spaces from the JSON String
					responseData = responseData.replace(/(\r\n|\n|\r)/gm,""); 
					var jsonData = $.parseJSON(responseData);
					var token = jsonData.TOKEN;
					//$("body").unmask();
					if(token=="SUCCESS")
					{
						 $("#MDM_DashboardForm").submit();
					}
					else
					{
						alert("Unable to change Preferred Language at the moment.");
					}	
				}
				catch(err)
				{
					console.log(err);
				}
			}
		},
		error:function(xmlhttp, status, error){
			//$("body").unmask();
			alert("error :: "+status);
		}
	});
}
</script>

</html>            