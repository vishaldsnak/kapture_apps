<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
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
<%
	String applicationContext = ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
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
<jsp:useBean id="settingsBean" class="com.mazda.gms3.dmt.bean.SettingsBean" scope="session"></jsp:useBean>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><fmt:message key="label.title" /> - <fmt:message key="label.settings"/> </title>
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />
<style type="text/css">
.cursorPointer {
	cursor: pointer;
}

.ui-dialog
{
	width:655px !important; 
	height:auto !important; 
	overflow-x:hidden !important;
	overflow-y:auto !important;
}

.ui-dialog .ui-dialog-title{color:#fff !important;}


</style>
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
			<div class="topMenu">
				<div class="logout" onclick="dmt_logout();"><span><i class="logoutIcon"></i><fmt:message key="label.logout" /> </span></div>
				<ul>
					<li class="activeLink margin-left-20"><a
						href="<%=request.getContextPath() %>/settings"><fmt:message key="label.settings"/></a></li>
					<li><a href="<%=request.getContextPath() %>/schedule"><fmt:message key="label.schedule"/></a></li>
					<li><a href="<%=request.getContextPath() %>/history"><fmt:message key="label.history"/></a></li>
					<li><a href="<%=request.getContextPath() %>/metadatavalidation"><fmt:message key="label.metadatavalidation"/></a></li>	
					<li><a href="<%=request.getContextPath() %>/mdsync"><fmt:message key="label.masterdatasynching" /> </a></li>
				</ul>
				<div class="cb"></div>
			</div>
            <!-- TOP MENU ENDS -->
            <form action="<%=request.getContextPath() %>/settings" id="DMT_SET_Form" name="DMT_SET_Form" method="post">
            <input type="hidden" name="DMT_SET_ActionClicked" id="DMT_SET_ActionClicked" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
								<table width="100%">
									<tr>
										<td>
											<c:if test="${!empty settingsBean.errorMessage }">
												<div class="errorMessage" id="DMT_ERROR_MESSAGE">
													<%
														if(null!=settingsBean.getErrorMessage() && !"".equals(settingsBean.getErrorMessage()))
														{
															String[] msgs = settingsBean.getErrorMessage().split("<MSG_TOKEN>");
															if(null!=msgs && msgs.length>0)
															{
																for(int i=0;i<msgs.length;i++)
																{
																	%>
																		<%=msgs[i].toString() %><br/>
																	<% 					
																}		
															}
														}
													%>
												</div>
											</c:if>
											<c:if test="${!empty settingsBean.successMessage }">
												<div class="successMessage" id="DMT_ERROR_MESSAGE">
													<c:out value="${settingsBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td>&nbsp;</td>
									</tr>
									<tr>
										<td>
											<h3><fmt:message key="label.define" /> <fmt:message key="label.networkpath" /> </h3>
											<br />
										</td>
									</tr>
									<tr>
										<td>
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td style="width: 250px; vertical-align: top;"><fmt:message key="label.networkpath" /><span class="mandatory">*</span> </td>
													<td style="width: 500px; vertical-align: top;text-align: left !important">
														<input type="text" id="DMT_SET_NetworkPath" name="DMT_SET_NetworkPath" value="<c:out value="${settingsBean.fieldDetails.networkPath }"/>" style="width:480px !important;text-align:left !important;"><br/>
														<i class="infoIcon"></i> <b class="settingsText"><fmt:message key="label.settings.help.text" /></b>
													</td>
													<td style="vertical-align: top;">
														<c:if test="${settingsBean.showSave eq true }">
															<input type="button" name="DMT_SET_Save" id="DMT_SET_Save" class="bluebutton cursorpointer" value="<fmt:message key="label.save"/>" onclick="dmt_set_save();" />
														</c:if>
														
														<c:if test="${settingsBean.showUpdate eq true }">
															<input type="button" name="DMT_SET_Update" id="DMT_SET_Update" class="bluebutton cursorpointer" value="<fmt:message key="label.update"/>" onclick="dmt_set_save();" />
														</c:if>
													</td>
												</tr>
												
											</table>
										</td>
									</tr>	
								</table>
							</div>
                        </div>
                    </div>
                </div>
            </div>
     </form>   
</div>
</body>
<!-- FOOTER STARTS -->
<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function dmt_set_save()
{
	$("#DMT_SET_ActionClicked").val("SAVE_MAPPING");
	$("#DMT_SET_Form").submit();
}


</script>

<script type="text/javascript">
var appCon="<%=applicationContext %>";
var serverUrl="";
if(null!=appCon && appCon!="" && appCon!="null")
{
	serverUrl=appCon;
}	
serverUrl=serverUrl+"<%=request.getContextPath()%>/localeselector";


function dmt_localeData()
{
	var loc = $("#LS_Locale").val();
	// Make Ajax Call
	$.ajax({
		url:serverUrl,
		type:"post",
		data:{
			DMT_LOCALE_GO:"DMT_LOCALE_GO",
			DMT_LOCALE_SEL_VALUE:loc
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
					$("body").unmask();
					if(token=="SUCCESS")
					{
						 $("#DMT_SET_ActionClicked").val("RESET");
						 $("#DMT_SET_Form").submit();
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
			$("body").unmask();
			alert("error :: "+status);
		}
	});
}
</script>
</html>