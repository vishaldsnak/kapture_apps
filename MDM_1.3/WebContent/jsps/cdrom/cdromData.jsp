<%@page import="com.mazda.gms3.mdm.vo.AccessManagementInterface"%>
<%@ page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_CD_CREATION);
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
<jsp:useBean id="CDRomSearchBean" class="com.mazda.gms3.cdrom.bean.CDRomSearchBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.cdcreation"/> </title>
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
			<jsp:include page="../languageSelector.jsp" flush="true" />
			<!-- CHANGE LANGUAGE MENU ENDS -->
			
            <!-- TOP MENU STARTS -->
             <jsp:include page="../topMenu.jsp" flush="true" />
			<!-- TOP MENU ENDS -->
            
           <div class="breadcumbs">
            	<c:choose>
            		<c:when test="${!empty CDRomSearchBean.selectedLanguage && (CDRomSearchBean.selectedLanguage eq 'ja-JP' || CDRomSearchBean.selectedLanguage eq 'en-JP' )}">
            			<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.cdcreation"/> &raquo;</a> ${CDRomSearchBean.selectedModel} &raquo; ${CDRomSearchBean.selectedVds} &rsaquo; ${CDRomSearchBean.selectedVinRange}	
            		</c:when>
            		<c:otherwise><
            			<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.cdcreation"/> &raquo;</a> ${CDRomSearchBean.selectedModel} &raquo; ${CDRomSearchBean.selectedWmi} &raquo; ${CDRomSearchBean.selectedVds} &rsaquo; ${CDRomSearchBean.selectedVinRange}
            		</c:otherwise>
            	</c:choose>
            </div>
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        	<table width="100%">
									<tr>
										<td>
											<c:if test="${!empty CDRomSearchBean.errorMessage }">
												<div class="errorMessage">
													<%
														if (null != CDRomSearchBean.getErrorMessage() && !"".equals(CDRomSearchBean.getErrorMessage())) {
																String[] msgs = CDRomSearchBean.getErrorMessage().split(",");
																if (null != msgs && msgs.length > 0) {
																	for (int i = 0; i < msgs.length; i++) {
													%>
													<%=msgs[i].toString()%><br />
													<%
														}
																}
															}
													%>
												</div>
											</c:if>
										</td>
									</tr>
									<tr>
										<td>
											<form id="CD_Rom_DataForm"  class="second" name="CD_Rom_DataForm" action="<%=request.getContextPath() %>/cdromdetail" method="post">
												<center><h3 class="heading"><fmt:message key="label.cdrom.cdromdata.heading"/></h3></center>
												
												<label><fmt:message key="label.cdrom.servicecontent"/></label></br>
													<table width="100%">
														<tr><td>
														<c:if test="${not empty CDRomSearchBean.serviceContents}">
														<c:forEach var="serviceContent" items="${CDRomSearchBean.serviceContents}" varStatus="status">
														
															<c:choose>
															<c:when test="${CDRomSearchBean.selectedServiceContents.contains(serviceContent.key)}">
																<input type="checkbox" name="serviceContent" value="${serviceContent.key}" checked="true">${serviceContent.value}
															</c:when>
															<c:otherwise>
																
																<input type="checkbox" name="serviceContent" value="${serviceContent.key}" >${serviceContent.value}
															</c:otherwise>
															</c:choose>
															<c:if test="${status.index % 2 ==0}"></td></tr><tr><td></c:if>
															<c:if test="${status.index % 2 !=0}"></td><td></c:if>
														</c:forEach>
														</c:if>
														<c:if test="${empty CDRomSearchBean.serviceContents}">
															<tr><td><fmt:message key="label.norecordsfound"/></td></tr>
														</c:if>
								 						</table>
								 						</br><label><fmt:message key="label.cdrom.engineworkshop"/></label></br>
														<table class="second" width="100%">
														<tr><td>
														<c:if test="${not empty CDRomSearchBean.engineWorkshopManuals}">
														<c:forEach var="engineWorkshopManual" items="${CDRomSearchBean.engineWorkshopManuals}" varStatus="status">
															<c:choose>
															<c:when test="${CDRomSearchBean.selectedEngineWorkshopManuals.contains(engineWorkshopManual.key)}">
																<input type="checkbox" name="engineWorkshopManual" value="${engineWorkshopManual.key}" checked="true">${engineWorkshopManual.value}
															</c:when>
															<c:otherwise>
																<input type="checkbox" name="engineWorkshopManual" value="${engineWorkshopManual.key}" >${engineWorkshopManual.value}
															</c:otherwise>
															</c:choose>
															<c:if test="${status.index % 2 ==0}"></td></tr><tr><td></c:if>
															<c:if test="${status.index % 2 !=0}"></td><td></c:if>
														</c:forEach>
														</c:if>
														<c:if test="${empty CDRomSearchBean.engineWorkshopManuals}">
															<tr><td><fmt:message key="label.norecordsfound"/></td></tr>
														</c:if>
														</table>
									
								 						</br><label><fmt:message key="label.cdrom.transmissionworkshop"/></label></br>
								 						<table class="second" width="100%">
								 						<tr><td>
								 						<c:if test="${not empty CDRomSearchBean.transmissionWorkshopManual}">
														<c:forEach var="transmissionWorkshopManual" items="${CDRomSearchBean.transmissionWorkshopManual}" varStatus="status">
															<c:choose>
															<c:when test="${CDRomSearchBean.selectedTransmissionWorkshopManual.contains(transmissionWorkshopManual.key)}">
																<input type="checkbox" name="transmissionWorkshopManual" value="${transmissionWorkshopManual.key}" checked="true">${transmissionWorkshopManual.value}
															</c:when>
															<c:otherwise>
																<input type="checkbox" name="transmissionWorkshopManual" value="${transmissionWorkshopManual.key}">${transmissionWorkshopManual.value}
															</c:otherwise>
															</c:choose>
															<c:if test="${status.index % 2 ==0}"></td></tr><tr><td></c:if>
															<c:if test="${status.index % 2 !=0}"></td><td></c:if>
														</c:forEach>
														</c:if>
														<c:if test="${empty CDRomSearchBean.transmissionWorkshopManual}">
															<tr><td><fmt:message key="label.norecordsfound"/></td></tr>
														</c:if>
														</table>
												 
											<input type="hidden" name="CD_Rom_ActionClicked" id="CD_Rom_ActionClicked" value="<c:out value="${CDRomSearchBean.actionClicked }"/>">
											<input type="hidden" id="serviceContentValue" name="serviceContentValue">
											<input type="hidden" id="engineWorkshopManualValue" name="engineWorkshopManualValue">
											<input type="hidden" id="transmissionWorkshopManualValue" name="transmissionWorkshopManualValue">
											<br/><center>
											<input type="button" class="gear_button cursorPointer" value="<fmt:message key="label.back"/>" name="back" id="back" onclick="cdrom_goback();">
												<c:choose>
												<c:when test="${empty CDRomSearchBean.transmissionWorkshopManual && empty CDRomSearchBean.engineWorkshopManuals && empty CDRomSearchBean.serviceContents}">
													<input type="button" class="bluebutton cursorPointer button-disabled" disabled="disabled" value="<fmt:message key="label.next"/>" name="formSubmit" id="formSubmit" onclick="crom_next();">
												</c:when>
												<c:otherwise>
													<input type="button" class="bluebutton cursorPointer" value="<fmt:message key="label.next"/>" name="formSubmit" id="formSubmit" onclick="crom_next();">
												</c:otherwise>
											</c:choose>
											<!-- ADDING REMOVE ALL CHECKBOX BUTTON -->
											<c:choose>
												<c:when test="${empty CDRomSearchBean.transmissionWorkshopManual && empty CDRomSearchBean.engineWorkshopManuals && empty CDRomSearchBean.serviceContents}">
													<input type="button" class="bluebutton cursorPointer button-disabled" style="width:100px !important;" disabled="disabled" value="<fmt:message key="label.cdrom.removeall"/>" name="formSubmitRmoveAll" id="formSubmitRmoveAll" onclick="crom_removeAllCheckBox();">
												</c:when>
												<c:otherwise>
													<input type="button" class="bluebutton cursorPointer" style="width:100px !important;" value="<fmt:message key="label.cdrom.removeall"/>" name="formSubmitRmoveAll" id="formSubmitRmoveAll" onclick="crom_removeAllCheckBox();">
												</c:otherwise>
											</c:choose>											
											</center>
										</form>  
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


<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function cdrom_goback()
{
	$("#CD_Rom_ActionClicked").val("BACK_TO_INDEX_CLICKED");
	$("#CD_Rom_DataForm").submit();		
}

function crom_next(){
	var selectedCheckBoxesValue1 = '';
	$("input:checkbox[name=serviceContent]:checked").each(function(){
		if (selectedCheckBoxesValue1.length == 0) {
			selectedCheckBoxesValue1 += $(this).val();
        }
        else {
        	selectedCheckBoxesValue1 += ',' + $(this).val();
        }
		$("#serviceContentValue").val(selectedCheckBoxesValue1);
	});
	var selectedCheckBoxesValue2 = '';
	$("input:checkbox[name=engineWorkshopManual]:checked").each(function(){
		if (selectedCheckBoxesValue2.length == 0) {
			selectedCheckBoxesValue2 += $(this).val();
        }
        else {
        	selectedCheckBoxesValue2 += ',' + $(this).val();
        }
		$("#engineWorkshopManualValue").val(selectedCheckBoxesValue2);
	});
	var selectedCheckBoxesValue3 = '';
	$("input:checkbox[name=transmissionWorkshopManual]:checked").each(function(){
		if (selectedCheckBoxesValue3.length == 0) {
			selectedCheckBoxesValue3 += $(this).val();
        }
        else {
        	selectedCheckBoxesValue3 += ',' + $(this).val();
        }
		$("#transmissionWorkshopManualValue").val(selectedCheckBoxesValue3);
	});
	
	// show Loader before submit
	$("#loader").show();
	$("#CD_Rom_ActionClicked").val("NEXT_CLICKED");
	$("#CD_Rom_DataForm").submit();	

}

function crom_removeAllCheckBox()
{
	// show Loader before submit
	$("#loader").show();
	$("#CD_Rom_ActionClicked").val("REMOVEL_ALL_CLICKED");
	$("#CD_Rom_DataForm").submit();	
}

</script>

</html>