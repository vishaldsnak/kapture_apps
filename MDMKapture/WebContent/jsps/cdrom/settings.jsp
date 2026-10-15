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
	//request.setAttribute("PAGE_NAME", "SETTINGS");
	request.setAttribute("PAGE_NAME",AccessManagementInterface.REF_KEY_CD_CREATION_SETTINGS);
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
<jsp:useBean id="settingsBean" class="com.mazda.gms3.cdrom.bean.CDRomSettingsBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.settings"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.settings"/> &rsaquo;</a> 
            </div>

	   <form action="<%=request.getContextPath() %>/settings" id="MDM_SET_Form" name="MDM_SET_Form" method="post">
            <input type="hidden" name="MDM_SET_ActionClicked" id="MDM_SET_ActionClicked" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
								<table width="100%">
									<tr>
										<td>
											 <c:if test="${!empty settingsBean.errorMessage }">
												<div class="errorMessage" id="MDM_ERROR_MESSAGE">
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
												<div class="successMessage" id="MDM_ERROR_MESSAGE">
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
											<h3><fmt:message key="label.define" /> <fmt:message key="label.settings.network" /> </h3>
											<br />
										</td>
									</tr>
									<tr>
										<td>
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td style="width: 250px; vertical-align: top;"><fmt:message key="label.settings.network" /><span class="mandatory">*</span> </td>
													<td style="width: 500px; vertical-align: top;text-align: left !important">
														<input type="text" id="MDM_SET_NetworkPath" name="MDM_SET_NetworkPath" value="<c:out value="${settingsBean.fieldDetails.networkPath }"/>" style="width:480px !important;text-align:left !important;"><br/>
														<i class="infoIcon"></i> <b class="settingsText"><fmt:message key="label.settings.help.text" /></b>
													</td>
													<td style="vertical-align: top;">
														<c:if test="${settingsBean.showSave eq true }">
															<input type="button" name="MDM_SET_Save" id="MDM_SET_Save" class="bluebutton cursorpointer" value="<fmt:message key="label.save"/>" onclick="mdm_set_save();" />
														</c:if>
														
														<c:if test="${settingsBean.showUpdate eq true }">
															<input type="button" name="MDM_SET_Update" id="MDM_SET_Update" class="bluebutton cursorpointer" value="<fmt:message key="label.update"/>" onclick="mdm_set_save();" />
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
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function mdm_set_save()
{
	$("#MDM_SET_ActionClicked").val("SAVE_MAPPING");
	$("#MDM_SET_Form").submit();
}


</script>

</html>