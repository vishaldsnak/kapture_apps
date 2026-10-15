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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_VIN);
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
<jsp:useBean id="vinBean" class="com.mazda.gms3.mdm.bean.VinBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.vin"/> </title>
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
            
             <div class="breadcumbs">
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.vin"/> &rsaquo;</a> 
            </div>			
            <form id="VIN_Form" name="VIN_Form" action="<%=request.getContextPath() %>/vin" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="VIN_SelectedRows" id="VIN_SelectedRows" value="<c:out value="${vinBean.selectedRows }"/>" />
			<input type="hidden" name="VIN_ActionClicked" id="VIN_ActionClicked" value="<c:out value="${vinBean.actionClicked }"/>" />
			<input type="hidden" name="VIN_UpdatedRows" id="VIN_UpdatedRows" value="" />
			<input type="hidden" name="VIN_DataTabel_displayPageNo" id="VIN_DataTabel_displayPageNo" value="<c:out value="${vinBean.displayPageNo }"/>" />
			<input type="hidden" name="VIN_DataTabel_displayPageLen" id="VIN_DataTabel_displayPageLen" value="<c:out value="${vinBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_VIN_Error_Message" colspan="3">
											<c:if test="${!empty vinBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=vinBean.getErrorMessage() && !"".equals(vinBean.getErrorMessage()))
														{
															String[] msgs = vinBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty vinBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${vinBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty vinBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${vinBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="VIN_CountryLocale_Code" name="VIN_CountryLocale_Code" onchange="VIN_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty vinBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${vinBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty vinBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq vinBean.countryLocaleId}">
																	<c:set var="selectedFlagLocale" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLocale eq 1}">
																	<option selected="selected" value="<c:out value="${countryLocaleList.countryLocaleId }"/>"><c:out value="${countryLocaleList.countryLocaleCode }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${countryLocaleList.countryLocaleId }"/>"><c:out value="${countryLocaleList.countryLocaleCode }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
												</div>
											</td>
											<td width="34%">
												<div class="formElement_row leftpadding_none">	
												<label><fmt:message key="label.language"/>: <span class="mandatory">*</span></label> 
												<select id="VIN_Lang_Code" name="VIN_Lang_Code" onchange="VIN_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty vinBean.languageList }">
														<c:forEach var="languageList" items="${vinBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq vinBean.manualLanguageId}">
																	<c:set var="selectedFlagLang" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
																	<option selected="selected" value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
												</div>
											</td>
											<td width="33%">
												<div class="formElement_row leftpadding_none">	
												<label><fmt:message key="label.model"/>: <span class="mandatory">*</span></label> 
												<select id="VIN_CarId" name="VIN_CarId" onchange="VIN_carSelection();">
													<option value="ALL"><fmt:message key="label.all"/></option>
													<c:if test="${!empty vinBean.carlineList }">
														<c:forEach var="carlineList" items="${vinBean.carlineList }">
															<c:set var="selectedFlagCar" value="" />
															<c:if test="${!empty carlineList.carlineIdForCombo}">
																<c:if test="${carlineList.carlineIdForCombo eq vinBean.carlineId}">
																	<c:set var="selectedFlagCar" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagCar eq 1}">
																	<option selected="selected" value="<c:out value="${carlineList.carlineIdForCombo }"/>"><c:out value="${carlineList.carlineNameEng }"/> (<c:out value="${carlineList.carlineCode }"/>)</option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${carlineList.carlineIdForCombo }"/>"><c:out value="${carlineList.carlineNameEng }"/> (<c:out value="${carlineList.carlineCode }"/>)</option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="2" align="left" style="padding-left: 50px;">
											<p class="warningMessage"><fmt:message key="label.note" />: <fmt:message key="label.info.vin.creation.removal" /> </p>
										</td>
										<td></td>
									</tr>
									
									<c:if test="${vinBean.showButtons eq true }">
										<tr>
											<td colspan="3" align="right">
												<c:if test="${vinBean.showReadControls eq true }">
													<input type="button" value="<fmt:message key="label.view" />" class="gear_button cursorPointer" onclick="VIN_viewBlock();">
												</c:if>
												<c:if test="${vinBean.showWriteControls eq true }">
													<input type="button" value="<fmt:message key="label.addnew" />" class="bluebutton cursorPointer" onclick="VIN_addNewBlock();">
												</c:if>
											</td>
										</tr>
										<tr>
											<td colspan="3">&nbsp;</td>
										</tr>
									</c:if>
									
									<c:if test="${vinBean.showView eq true }">
										<tr>
											<td colspan="3">
												<h3>
													<fmt:message key="label.search"/> <fmt:message key="label.master"/> <fmt:message key="label.details"/> 
												</h3>
												<br />
											</td>
										</tr>
										<tr>
											<td colspan="3">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tr>
														<td><fmt:message key="label.wmicode"/></td>
														<td>
															<select id="VIN_SearchWmiId" name="VIN_SearchWmiId" onchange="VIN_SearchComboSel('SEARCH_WMI');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.searchWMIList }">
																	<c:forEach var="searchWMIList" items="${vinBean.searchWMIList }">
																		<c:set var="searchWMIListFlag" value="" />
																		<c:if test="${!empty searchWMIList.value}">
																			<c:if test="${searchWMIList.value eq vinBean.searchWMICode}">
																				<c:set var="searchWMIListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchWMIListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchWMIList.value }"/>"><c:out value="${searchWMIList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchWMIList.value }"/>"><c:out value="${searchWMIList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.groupcode"/></td>
														<td>
															<select id="VIN_SearchGroupId" name="VIN_SearchGroupId" onchange="VIN_SearchComboSel('SEARCH_GROUP');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.searchGroupList }">
																	<c:forEach var="searchGroupList" items="${vinBean.searchGroupList }">
																		<c:set var="searchGroupListFlag" value="" />
																		<c:if test="${!empty searchGroupList.groupCode}">
																			<c:if test="${searchGroupList.groupCode eq vinBean.searchGroupId}">
																				<c:set var="searchGroupListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchGroupListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchGroupList.groupCode }"/>"><c:out value="${searchGroupList.groupCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchGroupList.groupCode }"/>"><c:out value="${searchGroupList.groupCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>	
													</tr>
													<tr>	
														<td><fmt:message key="label.vdscode"/></td>
														<td>
															<select id="VIN_SearchVDSId" name="VIN_SearchVDSId" onchange="VIN_SearchComboSel('SEARCH_VDS');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.searchVDSList }">
																	<c:forEach var="searchVDSList" items="${vinBean.searchVDSList }">
																		<c:set var="searchVDSListFlag" value="" />
																		<c:if test="${!empty searchVDSList.vdsCode}">
																			<c:if test="${searchVDSList.vdsCode eq vinBean.searchVDSCode}">
																				<c:set var="searchVDSListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchVDSListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchVDSList.vdsCode }"/>"><c:out value="${searchVDSList.vdsCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchVDSList.vdsCode }"/>"><c:out value="${searchVDSList.vdsCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.visrangestartend"/></td>
														<td>
															<select id="VIN_SearchVISStartId" name="VIN_SearchVISStartId" onchange="VIN_SearchComboSel('SEARCH_VISSTART');" style="width: 105px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.searchVISStartList }">
																	<c:forEach var="searchVISStartList" items="${vinBean.searchVISStartList }">
																		<c:set var="searchVISStartListFlag" value="" />
																		<c:if test="${!empty searchVISStartList.visStartRange}">
																			<c:if test="${searchVISStartList.visStartRange eq vinBean.searchVISStartRange}">
																				<c:set var="searchVISStartListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchVISStartListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchVISStartList.visStartRange }"/>"><c:out value="${searchVISStartList.visStartRange }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchVISStartList.visStartRange }"/>"><c:out value="${searchVISStartList.visStartRange }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
															<select id="VIN_SearchVISEndId" name="VIN_SearchVISEndId" style="width: 105px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.searchVISEndList }">
																	<c:forEach var="searchVISEndList" items="${vinBean.searchVISEndList }">
																		<c:set var="searchVISEndListFlag" value="" />
																		<c:if test="${!empty searchVISEndList.visEndRange}">
																			<c:if test="${searchVISEndList.visEndRange eq vinBean.searchVISEndRange}">
																				<c:set var="searchVISEndListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchVISEndListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchVISEndList.visEndRange }"/>"><c:out value="${searchVISEndList.visEndRange }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchVISEndList.visEndRange }"/>"><c:out value="${searchVISEndList.visEndRange }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													<tr>
														<td colspan="4" style="text-align:right;">
															<input type="button" onclick="VIN_search();" class="bluebutton cursorPointer" value="<fmt:message key="label.search" />"> 
															<input type="button" onclick="VIN_clear();" class="gear_button cursorPointer" value="<fmt:message key="label.clear" />">
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</c:if>
									<c:if test="${vinBean.showAdd eq true }">
										<c:set var="makeFieldsDisabled" value="" />
										<c:if test="${empty vinBean.carlineId || vinBean.carlineId eq '' || vinBean.carlineId eq 'ALL'}">
											<c:set var="makeFieldsDisabled" value="TRUE" />
										</c:if>
										<tr>
											<td colspan="3">
												<h3>
													<fmt:message key="label.insertnewdata"/>
												</h3>
												<br />
											</td>
										</tr>
										<tr>
											<td colspan="3">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tr>
														<td><fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
														<td>
															<select id="VIN_AddWmiId" name="VIN_AddWmiId" onchange="VIN_SearchComboSel('ADD_WMI');" 
																style="width: 210px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if>>
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.addWMIList }">
																	<c:forEach var="addWMIList" items="${vinBean.addWMIList }">
																		<c:set var="addWMIListFlag" value="" />
																		<c:if test="${!empty addWMIList.value}">
																			<c:if test="${addWMIList.value eq vinBean.addWmiCode}">
																				<c:set var="addWMIListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addWMIListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addWMIList.value }"/>"><c:out value="${addWMIList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addWMIList.value }"/>"><c:out value="${addWMIList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.groupcode"/><span class="mandatory">*</span></td>
														<td>
															<select id="VIN_AddGroupId" name="VIN_AddGroupId" onchange="VIN_SearchComboSel('ADD_GROUP');" 
																style="width: 210px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if>>
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.addGroupList }">
																	<c:forEach var="addGroupList" items="${vinBean.addGroupList }">
																		<c:set var="addGroupListFlag" value="" />
																		<c:if test="${!empty addGroupList.groupCode}">
																			<c:if test="${addGroupList.groupCode eq vinBean.addGroupId}">
																				<c:set var="addGroupListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addGroupListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addGroupList.groupCode }"/>"><c:out value="${addGroupList.groupCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addGroupList.groupCode }"/>"><c:out value="${addGroupList.groupCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>	
													</tr>
													<c:if test="${vinBean.addGroupId eq  'ADDNEW'}">
														<tr>
															<c:if test="${vinBean.addGroupId eq  'ADDNEW'  }">
															<td><fmt:message key="label.groupcode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
																id="VIN_NewGroupCode" name="VIN_NewGroupCode" 
															 	value="<c:out value="${vinBean.newGroupCode }" />"/></td>
															</c:if>
															<td></td>
															<td></td>	
														</tr>
													</c:if>
													<tr>	
														<td><fmt:message key="label.vdscode"/><span class="mandatory">*</span></td>
														<td>
															<select id="VIN_AddVDSId" name="VIN_AddVDSId" onchange="VIN_SearchComboSel('ADD_VDS');" 
																style="width: 210px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if>>
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.addVDSList }">
																	<c:forEach var="addVDSList" items="${vinBean.addVDSList }">
																		<c:set var="addVDSListFlag" value="" />
																		<c:if test="${!empty addVDSList.vdsCode}">
																			<c:if test="${addVDSList.vdsCode eq vinBean.addVDSCode}">
																				<c:set var="addVDSListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addVDSListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addVDSList.vdsCode }"/>"><c:out value="${addVDSList.vdsCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addVDSList.vdsCode }"/>"><c:out value="${addVDSList.vdsCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.visstartrange"/><span class="mandatory">*</span></td>
														<td>
															<select id="VIN_AddVISStartId" name="VIN_AddVISStartId" onchange="VIN_SearchComboSel('ADD_VISSTART');" 
																style="width: 210px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if>>
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.addVISStartList }">
																	<c:forEach var="addVISStartList" items="${vinBean.addVISStartList }">
																		<c:set var="addVISStartListFlag" value="" />
																		<c:if test="${!empty addVISStartList.visStartRange}">
																			<c:if test="${addVISStartList.visStartRange eq vinBean.addVISStartRange}">
																				<c:set var="addVISStartListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addVISStartListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addVISStartList.visStartRange }"/>"><c:out value="${addVISStartList.visStartRange }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addVISStartList.visStartRange }"/>"><c:out value="${addVISStartList.visStartRange }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													<c:if test="${vinBean.addVDSCode eq  'ADDNEW' || vinBean.addVISStartRange eq 'ADDNEW' }">
														<tr>
															<c:if test="${vinBean.addVDSCode eq  'ADDNEW'  }">
															<td><fmt:message key="label.vdscode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
																id="VIN_NewVDSCode" name="VIN_NewVDSCode" 
															 	value="<c:out value="${vinBean.newVDSCode }" />"/></td>
															</c:if>
															<c:if test="${vinBean.addVISStartRange eq 'ADDNEW' }">
															<td><fmt:message key="label.visstartrange"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
																id="VIN_NewVISStartRange" name="VIN_NewVISStartRange" 
															 	value="<c:out value="${vinBean.newVISStartRange }" />"/></td>
															</c:if> 	
														</tr>
													</c:if>
													
													<tr>	
														<td><fmt:message key="label.visendrange"/><span class="mandatory">*</span></td>
														<td>
															<select id="VIN_AddVISEndId" name="VIN_AddVISEndId" onchange="VIN_addComboSelection();" 
																style="width: 210px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if>>
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty vinBean.addVISEndList }">
																	<c:forEach var="addVISEndList" items="${vinBean.addVISEndList }">
																		<c:set var="addVISEndListFlag" value="" />
																		<c:if test="${!empty addVISEndList.visEndRange}">
																			<c:if test="${addVISEndList.visEndRange eq vinBean.addVISEndRange}">
																				<c:set var="addVISEndListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addVISEndListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addVISEndList.visEndRange }"/>"><c:out value="${addVISEndList.visEndRange }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addVISEndList.visEndRange }"/>"><c:out value="${addVISEndList.visEndRange }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.enginecode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
															id="VIN_Engine_Code" name="VIN_Engine_Code" value="<c:out value="${vinBean.fieldDetails.engineCode }" />"/></td>
													</tr>
													
													<c:if test="${vinBean.addVISEndRange eq  'ADDNEW' }">
														<tr>
															<td><fmt:message key="label.visendrange"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
																id="VIN_NewVISEndRange" name="VIN_NewVISEndRange" 
															 	value="<c:out value="${vinBean.newVISEndRange }" />"/></td>
														</tr>
													</c:if>
													
													<tr>
														<td><fmt:message key="label.missioncode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
															id="VIN_Mission_Code" name="VIN_Mission_Code" value="<c:out value="${vinBean.fieldDetails.missionCode }" />"/></td>
														<td style="text-align: right; !important;" colspan="2">
															<input type="button" name="VIN_Entry" id="VIN_Entry" <c:if test="${makeFieldsDisabled eq 'TRUE' }">disabled="disabled"</c:if> 
																 onclick="VIN_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</c:if>
								</table>
								
								<c:if test="${vinBean.showAdd eq true }">
									<p style="height:45px;">&nbsp;</p>
									<h4>
										<fmt:message key="label.import"/> <fmt:message key="label.vin"/> <fmt:message key="label.details"/>
									</h4>
									<br />
									<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
										<tr>
											<td>
												<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
													<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.vin"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
													<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
													<div class="fileUpload btn">
														<span><fmt:message key="label.browse"/></span>
														<input id="VIN_File" name="VIN_File" type="file" class="upload" />
													</div>
													<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="VIN_file_upload();" id="VIN_Upload" name="VIN_Upload" 
													class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="VIN_DownloadTemplate" onclick="VIN_download_template();"
										title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
													<div class="cb"></div>
												</div>
											</td>
										</tr>
									</table>
								</c:if>
								
								<c:if test="${vinBean.showView eq true || vinBean.showAdd eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<c:if test="${!empty vinBean.vinList }">
								<p class="warningMessage">
									<fmt:message key="label.vin"/> <fmt:message key="label.details"/> {<fmt:message key="label.combinationof"/> <fmt:message key="label.groupcode"/>, <fmt:message key="label.wmicode"/>, <fmt:message key="label.vdscode"/>, <fmt:message key="label.visstartrange"/>, <fmt:message key="label.visendrange"/>} <fmt:message key="label.unique.warning"/> <fmt:message key="label.model"/>.
								</p>
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty vinBean.vinList }">
											<c:forEach var="vinList" items="${vinBean.vinList }">
												<c:if test="${vinList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty vinBean.vinList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${vinBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="7%"><fmt:message key="label.wmicode"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.groupcode"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.vdscode"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.visstartrange"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.visendrange"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="11%"><fmt:message key="label.enginecode"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="11%"><fmt:message key="label.missioncode"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.flag"/> <c:if test="${vinBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.imsyncstatus"/></th>
											<th scope="col" width="7%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="7%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty vinBean.vinList }">
											<c:forEach var="vinList" items="${vinBean.vinList }">
												<tr <c:if test="${vinList.editableFlag eq true }"> class="selected" </c:if>>
													<td> 
														<input type="checkbox" name="VIN_Selection" onchange="VIN_checkBoxManagement();" 
															value="<c:out value="${vinList.vinId }" />" 
															id="VIN_Selection_<c:out value="${vinList.vinId }"  />"
															<c:if test="${vinList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${vinBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${vinList.srNo }" /> </td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.wmiCode }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<select name="VIN_VinList_Wmi_Code<c:out value="${vinList.vinId }" />" id="VIN_VinList_Wmi_Code<c:out value="${vinList.vinId }" />" style="width: 90px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:forEach var="wmiCodesList" items="${vinList.wmiCodeList }">
																	<c:set var="selectedWmiFlag" value="" />
																	<c:if test="${!empty vinList.wmiCode}">
																		<c:if test="${vinList.wmiCode eq wmiCodesList.value}">
																			<c:set var="selectedWmiFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedWmiFlag eq 1}">
																			<option value="<c:out value="${wmiCodesList.value }" />" selected="selected"><c:out value="${wmiCodesList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${wmiCodesList.value }" />"><c:out value="${wmiCodesList.label }" /></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</select>	
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.groupCode }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.groupCode }" />" size="10" 
																name="VIN_VinList_Group_Code<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Group_Code<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.vdsCode }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.vdsCode }" />" size="10" 
																name="VIN_VinList_Vds_Code<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Vds_Code<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.visStartRange }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.visStartRange }" />" size="10" 
																name="VIN_VinList_Vis_Start_Range<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Vis_Start_Range<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.visEndRange }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.visEndRange }" />" size="10" 
																name="VIN_VinList_Vis_End_Range<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Vis_End_Range<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.engineCode }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.engineCode }" />" size="10" 
																name="VIN_VinList_Engine_Code<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Engine_Code<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.missionCode }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${vinList.missionCode }" />" size="10" 
																name="VIN_VinList_Mission_Code<c:out value="${vinList.vinId }" />" 
																id="VIN_VinList_Mission_Code<c:out value="${vinList.vinId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${vinList.editableFlag eq false }">
															<label><c:out value="${vinList.flagLabel }" /></label>
														</c:if>
														<c:if test="${vinList.editableFlag eq true }">
															<select name="VIN_LangList_Flag_<c:out value="${vinList.vinId }" />" id="VIN_LangList_Flag_<c:out value="${vinList.vinId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${vinBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty vinList.flag}">
																		<c:if test="${vinList.flag eq flagList.value}">
																			<c:set var="selectedFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlag eq 1}">
																			<option value="<c:out value="${flagList.value }" />" selected="selected"><c:out value="${flagList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${flagList.value }" />"><c:out value="${flagList.label }" /></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</select>
														</c:if>
													</td>
													<td>
															<c:if test="${!empty vinList.syncStatus }">
																<c:out value="${vinList.syncStatus }"/>
															</c:if>
															<c:if test="${empty vinList.syncStatus }">-</c:if>
														</td>
													<td>
														<fmt:formatDate value="${vinList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${vinList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty vinBean.vinList }">
									<table cellspacing="0"
											width="100%" class="noRecordTable">
											<tr>
												<td width="15%">&nbsp;</td>
												<td width="70%" class="noRecordText">
													<fmt:message key="label.norecord.table.message" />
												</td>
												<td width="15%" style="text-align:right;">
													<i class="noRecordIcon"><i>
												</td>
											</tr>
									</table>	
								</c:if>
								</c:if>
							</div>
                        </div>
                    </div>
                </div>
            </div>
       </form>     
</div>
<div id="reset_dialog-confirm" title="<fmt:message key="label.resetform"/>" class="hide">
	<p>
		<strong><fmt:message key="label.resetdescription"/></strong>
	</p>
</div>
<div id="delete_dialog-confirm" title="<fmt:message key="label.delete"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.deletedescription"/>
	</p>
</div>

<div id="active_dialog-confirm" title="<fmt:message key="flag.label.active"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.activedescription"/>
	</p>
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<c:if test="${!empty vinBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${vinBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("VIN_File"))
{
document.getElementById("VIN_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty vinBean.vinList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty vinBean.vinList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${vinBean.showUpdate eq false }">
<c:if test="${vinBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"VIN_edit();\" name=\"VIN_Edit\" id=\"VIN_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"VIN_delete();\" name=\"VIN_Delete\" id=\"VIN_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"VIN_active();\" name=\"VIN_Active\" id=\"VIN_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
	
</script>
</c:if>

<c:if test="${vinBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"VIN_export();\"  name=\"VIN_Export\" id=\"VIN_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${vinBean.showUpdate eq true && vinBean.showWriteControls eq true }">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"VIN_update();\" style=\"float:right\" name=\"VIN_Update\" id=\"VIN_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"VIN_Reset\" id=\"VIN_Reset\" onclick=\"VIN_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
</script>
</c:if>

<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});


$(document).ready(function() {
	loadDataTable();
	//$("body").unmask();
} );


function loadDataTable()
{
	// data table labels
	var show="<fmt:message key="label.show"/>";
	var entries="<fmt:message key="label.entries"/>";
	var zeroRec =  "<fmt:message key="label.norecordsfound"/>"; 
	//var showing = "<fmt:message key="label.showing"/>";
	//var to="<fmt:message key="label.to"/>";
	var of="<fmt:message key="label.of"/>";
	//var entriesLowercase="<fmt:message key="label.entries.lowercase"/>";
	//var noRecAvailable="<fmt:message key="label.norecordsfound"/>";
	//var filtered="<fmt:message key="label.filtered"/>";
	//var from="<fmt:message key="label.filtered"/>";
	//var total="<fmt:message key="label.total"/>";
	var search="<fmt:message key="label.search"/>";
	
	var page = "<fmt:message key="label.page"/>";
	var first="<i class=\"firstNav\"></i>";
	var last="<i class=\"lastNav\"></i>";
	var previous="<i class=\"prevNav\"></i>";
	var next="<i class=\"nextNav\"></i>";
	var space=" ";
	var domData;
	if(version=="6" || version=="7")
	{
		// FOR IE
		domData="<\"top\"fip><\"toolbar\">rt<\"bottom\"p><\"clear\">";
	}
	else
	{
		domData="<\"top\"<\"toolbar\">fip>rt<\"bottom\"p><\"clear\">";
	}
	show=show+" ";
	$('#example').dataTable({
		"bDestroy":true,
		"dom": domData,
		"pagingType": "input",
        /* Disable initial sort */
         "bSort" : true,
         aoColumnDefs: [
                        { aTargets: [ 0 ], bSortable: false },
                        { aTargets: [ 1 ], bSortable: true },
                        { aTargets: [ 2 ], bSortable: true },
                        { aTargets: [ 3 ], bSortable: true },
                        { aTargets: [ 4 ], bSortable: true },
                        { aTargets: [ 5 ], bSortable: true },
                        { aTargets: [ 6 ], bSortable: true },
                        { aTargets: [ 7 ], bSortable: true },
                        { aTargets: [ 8 ], bSortable: true },
                        { aTargets: [ 9 ], bSortable: true },
                        { aTargets: [ 10 ], bSortable: true },
                        { aTargets: [ 11 ], bSortable: true },
                        { aTargets: [ 12 ], bSortable: true }
                     ],
        "order": [[1, 'asc']],
		 //Customize Language
		 "language": {
          	"lengthMenu": ""+show+"_MENU_ "+entries+" ",
          	"zeroRecords": ""+zeroRec+"",
			//"info": ""+showing+" _START_ "+to+" _END_ "+of+" _TOTAL_ "+entriesLowercase+"",
			"info": "_START_ - _END_ "+of+" _TOTAL_",
            //"infoEmpty": ""+noRecAvailable+"",
			"infoEmpty":""+space+"",
            //"infoFiltered": "("+filtered+" "+from+" _MAX_ "+total+" "+entriesLowercase+")",
			"infoFiltered": ""+space+"",
			"search": ""+space+"",
			"searchPlaceholder": ""+search+"",
			"paginate": {
				"next": ""+next+"",
				"previous": ""+previous+"",
				"first": ""+first+"",
				"last": ""+last+""	
			}
        }
    });
	
	
	$("div.toolbar").html(htmlContent);
	// change pagination text
	$('.paginate_page').text(page);
	var pageOfVal=$('.paginate_of:first').text();
	if(null!=pageOfVal && pageOfVal!="")
	{
		pageOfVal = $.trim(pageOfVal);
		if(pageOfVal.indexOf(" ")!=-1)
		{
			var afterValue=pageOfVal.substring(pageOfVal.indexOf(" ")+1, pageOfVal.length);
			afterValue = $.trim(afterValue);
			pageOfVal = of+" "+afterValue;
			$('.paginate_of').text(pageOfVal);
		}
	}	
	
	var table = $('#example').DataTable();
	var pageNo=$("#VIN_DataTabel_displayPageNo").val();
	var pageLength=$("#VIN_DataTabel_displayPageLen").val();
	// first set pageLength
	if(null!=pageLength && pageLength!="")
	{
		table.page.len(pageLength);
		table.draw();
	}
	// then pageNo
	if(null!=pageNo && pageNo!="")
	{
		var act = parseInt(pageNo);
		table.page(act).draw(false);
	}
	
	// Call on Search to control top level check box selection
	table.on( 'search.dt', function () {
		VIN_checkBoxManagement();
	});
}

function VIN_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#VIN_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#VIN_DataTabel_displayPageLen").val(length);
	}	
	
}

function VIN_edit()
{
	VIN_readDataTableValues();
	var value=$("#VIN_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#VIN_ActionClicked").val("EDIT_VIN");
		$("#VIN_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}  
}

function VIN_viewBlock()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("VIEW_BUTTON");
	$("#VIN_Form").submit();
}

function VIN_addNewBlock()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("ADD_NEW_BUTTON");
	$("#VIN_Form").submit();	
}

function VIN_delete_action()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("DELETE_VIN");
	$("#VIN_Form").submit();
}

function VIN_active_action()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("ACTIVE_VIN");
	$("#VIN_Form").submit();
}

function VIN_search()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("SEARCH_VIN");
	$("#VIN_Form").submit();
}

function VIN_clear()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("CLEAR_SEARCH");
	$("#VIN_Form").submit();
}

function VIN_SearchComboSel(operationType)
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val(operationType);
	$("#VIN_Form").submit();
}

function VIN_addComboSelection()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("");
	$("#VIN_Form").submit();
}


function VIN_delete()
{
	$("#MDM_VIN_Error_Message").html("");
	var value=$("#VIN_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#delete_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: false,
			  height:140,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.yes"/>": function() {
					  $( this ).dialog( "close" );
					  VIN_delete_action();
					}  
				,
				"<fmt:message key="label.no"/>": function() {
				  $( this ).dialog( "close" );
				}
			  }
			});
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.delete" />";
		mdm_show_errorMessage(delMessage);	
	}
}


function VIN_active()
{
	$("#MDM_VIN_Error_Message").html("");
	var value=$("#VIN_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#active_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: false,
			  height:140,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.yes"/>": function() {
					  $( this ).dialog( "close" );
					  VIN_active_action();
					}  
				,
				"<fmt:message key="label.no"/>": function() {
				  $( this ).dialog( "close" );
				}
			  }
			});
		$("#active_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
		$("#active_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.active" />";
		mdm_show_errorMessage(delMessage);	
	}
}


function VIN_export()
{
	$("#MDM_VIN_Error_Message").html("");
	var value=$("#VIN_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		VIN_readDataTableValues();
		$("#VIN_ActionClicked").val("EXPORT_VIN");
		  $("#VIN_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
}

function mdm_show_errorMessage(message)
{
	
	var html="<div class=\"errorMessage\">"+message+"</div>";
	$("#MDM_VIN_Error_Message").html(html);
	window.scrollTo(0,0);
}


function VIN_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#VIN_ActionClicked").val("FILE_UPLOAD");
	$("#VIN_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function VIN_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=VIN";
}


function VIN_entry()
{
	// show Loader
	$("#loader").show();
	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("SAVE_VIN");
	  $("#VIN_Form").submit();
}



function VIN_update()
{
	// show Loader
	$("#loader").show();
	var newData="";
	var table = $('#example').DataTable();
	var data = table.$('input , select').serializeArray();
	$.each(data, function(i, field){
		if(null!=newData && newData!="")
		{
			newData=newData+"<MDM_FS>";
		}
		var v = "";
		if(null!=field.value && field.value!="")
		{
			v = $.trim(field.value);
		}	
		newData=newData+field.name+"<MDM_TS>"+v;
		v = null;
	});
   	$("#VIN_UpdatedRows").val(newData);
   	VIN_readDataTableValues();
	$("#VIN_ActionClicked").val("UPDATE_VIN");
	  $("#VIN_Form").submit();
}

function VIN_checkBoxManagement()
{
	$("#VIN_SelectedRows").val("");
	var value=$("#VIN_SelectedRows").val();
	if(null!=value && value!="")
	{
		value = value+",";
	}
	// Get Selected Checkboxes from data table
	var table = $('#example').DataTable();
	var rows = table.rows({ 'search': 'applied' }).nodes();
	// set all Rows as unselected
	$('input[type="checkbox"]', rows).each(function() {
		// remove class selected
		$(this).parents('tr').removeClass("selected");
	});
	
	$('input[type="checkbox"]', rows).each(function() {
	if ($(this).is(':checked')) {
			value = value+$(this).attr('value')+",";
			// add class selected
			$(this).parents('tr').addClass("selected");
		}
	});
		
	if(null!=value && value!="")
	{
		var lastChar = value.substring(value.length-1,value.length);
		if(lastChar==",")
		{
			value=value.substring(0,value.length-1);
		}
		/*
			remove duplicate values
		*/
		var tokens = value.split(",");
		var selIds=[];
		var result=[];
		if(null!=tokens && tokens.length>0)
		{
			for(var n=0;n<tokens.length;n++)
			{
				selIds.push(tokens[n]);
			}
			var result = [];
			$.each(selIds, function(i, e) {
				if ($.inArray(e, result) == -1) result.push(e);
			});
		}
		$("#VIN_SelectedRows").val(result);
	}	
	/*
	check here if all rows are selected, then by default
	check - MDM_HeaderCheckBox
	*/
	$("#MDM_HeaderCheckBox").prop("checked",false);
	var allSelected=true;
	$('input[type="checkbox"]', rows).each(function() {
		if (!$(this).is(':checked')) {
			allSelected = false;
			}
		});
	if(allSelected==true)
	{
		if($('input[type="checkbox"]', rows).length>0)
		{
			$("#MDM_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function VIN_reset()
{
	// show Loader
	$("#loader").show();
	$("#VIN_ActionClicked").val("RESET_VIN");
	  $("#VIN_Form").submit();
}

function VIN_reset_withDialog()
{
	// open dialog
	$( "#reset_dialog-confirm" ).removeClass('hide').dialog({
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	"<fmt:message key="label.reset"/>": function() {
				 $("#VIN_ActionClicked").val("RESET_VIN");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
				  $("#VIN_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function VIN_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#VIN_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#VIN_Form").submit();
}

function VIN_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#VIN_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#VIN_Form").submit();
}

function VIN_carSelection()
{
	// show Loader
	$("#loader").show();
	$("#VIN_ActionClicked").val("CAR_SELECTION");
	 $("#VIN_Form").submit();
}

function VIN_carCodeSelection()
{
	$("#VIN_ActionClicked").val("CARCODE_SELECTION");
	 $("#VIN_Form").submit();
}

function VIN_filterSelection()
{
	// show Loader
	$("#loader").show();
	$("#VIN_ActionClicked").val("FILTER_SELECTION");
	 $("#VIN_Form").submit();
}

function mdm_selectallrows(thisObj)
{
	if(thisObj.checked==true)
	{
		var table = $('#example').DataTable();
		var rows = table.rows({ 'search': 'applied' }).nodes();
		$('input[type="checkbox"]', rows).prop('checked', true);
	}
	else
	{
		var table = $('#example').DataTable();
		var rows = table.rows({ 'search': 'applied' }).nodes();
		$('input[type="checkbox"]', rows).prop('checked', false);
		// set hidden chkboxes selection value to null
		$("#VIN_SelectedRows").val("");
	}
	VIN_checkBoxManagement();
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
						VIN_readDataTableValues();
						 $("#VIN_ActionClicked").val("RESET_VIN");
						 $("#VIN_Form").submit();
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