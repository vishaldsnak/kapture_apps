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
<%--
	MY PAGE (DASHBOARD).

	THE TILES ARE NO LONGER DRAWN AS ONE FLAT GRID. THEY ARE GROUPED INTO CATEGORY TABS BY
	com.mazda.gms3.mdm.utils.DashboardCategories, WHICH THE Dashboard SERVLET PUTS ON THE REQUEST
	AS DASHBOARD_CATEGORIES.

	WHICH TILES A USER GETS IS STILL DECIDED ENTIRELY BY AccessManagementFilter - THIS PAGE ONLY
	ARRANGES WHAT IT IS GIVEN, SO NO ACCESS RULE CHANGED. THE POST CONTRACT IS ALSO UNCHANGED:
	EVERY TILE STILL SUBMITS DASH_TILE_CLICKED_VAL AND DASH_TILE_CLICKED_REFKEY_VAL TO /mypage.
--%>
<%
	// KEEPS THE LOCALE SELECTOR BEAN POPULATED EXACTLY AS languageSelector.jsp USED TO
%>
<jsp:include page="/localeselector" />
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
<!-- MY PAGE LAYOUT - LOADED LAST SO IT WINS OVER style.css -->
<link href="css/mdm-dashboard.css" type="text/css" rel="stylesheet" />
<style type="text/css">
.cursorPointer {
	cursor: pointer;
}
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

/* MY PAGE HAS ITS OWN FULL WIDTH SHELL - THE APPLICATION FOOTER IS RENDERED INSIDE IT INSTEAD */
#footerData{display:none;}
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

	<!-- APPLICATION TOP BAR STARTS - REPLACES languageSelector.jsp + topMenu.jsp ON THIS PAGE -->
	<jsp:include page="appTopBar.jsp" flush="true" />
	<!-- APPLICATION TOP BAR ENDS -->

	<form id="MDM_DashboardForm" name="MDM_DashboardForm" action="<%=request.getContextPath() %>/mypage" method="post">
	<input type="hidden" name="DASH_TILE_CLICKED_VAL" id="DASH_TILE_CLICKED_VAL" value="" />
	<input type="hidden" name="DASH_TILE_CLICKED_REFKEY_VAL" id="DASH_TILE_CLICKED_REFKEY_VAL" value="" />
	<%-- CATEGORY OF THE CLICKED TILE - THE SERVLET REMEMBERS IT SO THE SAME TAB RE OPENS LATER --%>
	<input type="hidden" name="DASH_TAB_SELECTED" id="DASH_TAB_SELECTED" value="" />
	<!--  CONTENT BLOCK STARTS HERE -->
	<div class="mdmPage">

		<div class="mdmWelcome">
			<h1>
				<fmt:message key="label.dash.welcome" />,
				<span><c:out value="${userAccessBean.userDisplayName}" /></span>
			</h1>
			<p><fmt:message key="label.dash.mypage.subtitle" /></p>
		</div>

		<c:choose>
			<c:when test="${!empty DASHBOARD_CATEGORIES}">
				<!-- CATEGORY TABS -->
				<div class="mdmTabs" id="mdmTabs">
					<c:forEach var="category" items="${DASHBOARD_CATEGORIES}" varStatus="catStatus">
						<div class="mdmTab <c:if test="${catStatus.index == DASHBOARD_ACTIVE_INDEX}">mdmTabOn</c:if>"
							id="mdmTab_<c:out value="${catStatus.index}"/>"
							onclick="mdm_selectDashboardTab(<c:out value="${catStatus.index}"/>);">
							<span class="mdmTabTop"></span>
							<%-- INLINE SVG - PRINTED RAW ON PURPOSE. <c:out> WOULD ESCAPE IT AND SHOW THE MARKUP. --%>
							<span class="mdmTabIcon">${category.icon}</span>
							<span class="mdmTabLabel"><c:out value="${category.title}" /></span>
							<span class="mdmTabCount">(<c:out value="${category.tileCount}" />)</span>
						</div>
					</c:forEach>
				</div>

				<!-- TILES, ONE PANEL PER CATEGORY -->
				<div class="mdmPanels" id="mdmPanels">
					<c:forEach var="category" items="${DASHBOARD_CATEGORIES}" varStatus="catStatus">
						<div class="mdmPanel <c:if test="${catStatus.index == DASHBOARD_ACTIVE_INDEX}">mdmPanelOn</c:if>"
							id="mdmPanel_<c:out value="${catStatus.index}"/>">
							<div class="mdmPanelHead">
								<h2><c:out value="${category.title}" /></h2>
								<c:if test="${!empty category.description}">
									<p><c:out value="${category.description}" /></p>
								</c:if>
							</div>
							<div class="mdmTiles">
								<c:forEach var="tile" items="${category.tilesList}">
									<a class="mdmTile" href="javascript:void(0);"
										onclick="mdm_navigateSomeWhere('<c:out value="${tile.navType}"/>', '<c:out value="${tile.refKey}"/>', '<c:out value="${category.categoryKey}"/>');">
										<%-- SVG CHEVRON, AS IN THE MOCKUP - THE OLD &rsaquo; RENDERED AT A DIFFERENT WEIGHT --%>
										<span class="mdmTileArrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
										<%-- INLINE SVG - PRINTED RAW ON PURPOSE, SEE DashboardTileDetails.icon --%>
										<span class="mdmTileIcon">${tile.icon}</span>
										<h3><c:out value="${tile.title}" /></h3>
										<c:if test="${!empty tile.description}">
											<p><c:out value="${tile.description}" /></p>
										</c:if>
									</a>
								</c:forEach>
								<div class="cb"></div>
							</div>
						</div>
					</c:forEach>
				</div>
			</c:when>
			<c:otherwise>
				<div class="mdmPanels">
					<div class="mdmNoTiles"><fmt:message key="label.dash.notiles" /></div>
				</div>
			</c:otherwise>
		</c:choose>

		<div class="mdmFooter" id="mdmFooterData"></div>
	</div>
	<!-- CONTENT BLOCK ENDS HERE -->
	</form>

</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<script type="text/javascript">
/*
 * TAB SWITCHING IS PLAIN JAVASCRIPT ON PURPOSE - IT RUNS BEFORE footer.jsp'S jQuery HAS TO BE
 * RELIED ON, AND THE PAGE HAS NO OTHER REASON TO NEED IT.
 */
function mdm_selectDashboardTab(index)
{
	var tabs = document.getElementById("mdmTabs");
	var panels = document.getElementById("mdmPanels");
	if(null==tabs || null==panels)
	{
		return;
	}
	var i=0;
	for(i=0; i<tabs.childNodes.length; i++)
	{
		var tab = tabs.childNodes[i];
		if(null!=tab && tab.className && tab.className.indexOf("mdmTab")>=0)
		{
			tab.className = (tab.id == ("mdmTab_"+index)) ? "mdmTab mdmTabOn" : "mdmTab";
		}
	}
	for(i=0; i<panels.childNodes.length; i++)
	{
		var panel = panels.childNodes[i];
		if(null!=panel && panel.className && panel.className.indexOf("mdmPanel")>=0)
		{
			panel.className = (panel.id == ("mdmPanel_"+index)) ? "mdmPanel mdmPanelOn" : "mdmPanel";
		}
	}
}

// PAGE FOOTER - footer.jsp WRITES THE SAME TEXT INTO ITS OWN ELEMENT, WHICH THIS PAGE HIDES
(function(){
	var el = document.getElementById("mdmFooterData");
	if(null!=el)
	{
		el.innerHTML = "&copy; " + new Date().getFullYear() + " Mazda Motor Corporation";
	}
})();
</script>

<script>
$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function mdm_navigateSomeWhere(navType, refKey, tabKey)
{
	// show Loader
	$("#loader").show();
	document.getElementById("DASH_TILE_CLICKED_VAL").value=navType;
	document.getElementById("DASH_TILE_CLICKED_REFKEY_VAL").value=refKey;
	// REMEMBER THE TAB THIS TILE SITS ON, SO COMING BACK TO MY PAGE RE OPENS IT
	document.getElementById("DASH_TAB_SELECTED").value=tabKey;
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
