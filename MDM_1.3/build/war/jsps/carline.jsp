<?xml version="1.0" encoding="UTF-8"?>

<%@page import="com.mazda.gms3.mdm.vo.AccessManagementInterface"%>
<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<meta http-equiv="Content-Type"  content="text/html; charset=UTF-8">
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<head>
<!-- 
	SET PAGE NAME
 -->
<%
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_CARLINE);
%>

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
<jsp:useBean id="carlineBean" class="com.mazda.gms3.mdm.bean.CarlineBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.car"/> </title>
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
            
             <div class="breadcumbs">
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.car"/> &rsaquo;</a> 
            </div>
            <form id="CAR_Form" name="CAR_Form" action="<%=request.getContextPath() %>/carline" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="CAR_SelectedRows" id="CAR_SelectedRows" value="<c:out value="${carlineBean.selectedRows }"/>" />
			<input type="hidden" name="CAR_ActionClicked" id="CAR_ActionClicked" value="<c:out value="${carlineBean.actionClicked }"/>" />
			<input type="hidden" name="CAR_UpdatedRows" id="CAR_UpdatedRows" value="" />
			<input type="hidden" name="CAR_DataTabel_displayPageNo" id="CAR_DataTabel_displayPageNo" value="<c:out value="${carlineBean.displayPageNo }"/>" />
			<input type="hidden" name="CAR_DataTabel_displayPageLen" id="CAR_DataTabel_displayPageLen" value="<c:out value="${carlineBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_CAR_Error_Message" colspan="3">
											<c:if test="${!empty carlineBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=carlineBean.getErrorMessage() && !"".equals(carlineBean.getErrorMessage()))
														{
															String[] msgs = carlineBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty carlineBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${carlineBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty carlineBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${carlineBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="CAR_CountryLocale_Code" name="CAR_CountryLocale_Code" onchange="cl_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty carlineBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${carlineBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty carlineBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq carlineBean.countryLocaleId}">
																	<c:set var="selectedFlagLang" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
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
												<select id="CAR_Lang_Code" name="CAR_Lang_Code" onchange="cl_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty carlineBean.languageList }">
														<c:forEach var="languageList" items="${carlineBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq carlineBean.manualLanguageId}">
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
										&nbsp;
										</td>
									</tr>
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>
									<c:if test="${carlineBean.showButtons eq true }">
										<tr>
											<td colspan="3" align="right">
												<c:if test="${carlineBean.showReadControls eq true }">
													<input type="button" value="<fmt:message key="label.view" />" class="gear_button cursorPointer" onclick="cl_viewBlock();"/>
												</c:if>
												<c:if test="${carlineBean.showWriteControls eq true }">
													<input type="button" value="<fmt:message key="label.addnew" />" class="bluebutton cursorPointer" onclick="cl_addNewBlock();"/>
												</c:if>	
											</td>
										</tr>
										<tr>
											<td colspan="3">&nbsp;</td>
										</tr>
									</c:if>
									<c:if test="${carlineBean.showView eq true }">
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
													<c:if test="${carlineBean.mnaoContent eq true }">
														<tr>
															<td><fmt:message key="label.wmicode"/></td>
															<td>
																<select id="CAR_SearchWmiCode" name="CAR_SearchWmiCode" 
																	style="width: 200px;" onchange="cl_wmisearchselection();">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewWmiList }">
																		<c:forEach var="searchWmiList" items="${carlineBean.searchWmiList }">
																			<c:set var="selectedFlagSearchWMI" value="" />
																			<c:if test="${!empty searchWmiList.wmiCode}">
																				<c:if test="${searchWmiList.wmiCode eq carlineBean.searchWmiId}">
																					<c:set var="selectedFlagSearchWMI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagSearchWMI eq 1}">
																					<option selected="selected" value="<c:out value="${searchWmiList.wmiCode }"/>"><c:out value="${searchWmiList.wmiCode }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${searchWmiList.wmiCode }"/>"><c:out value="${searchWmiList.wmiCode }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.car"/></td>
															<td>
																<select id="CAR_SearchCarlineName" name="CAR_SearchCarlineName" 
																 	style="width: 200px;" onchange="cl_carnamesearchselection();"> 
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.searchCarlineNameList }">
																		<c:forEach var="searchCarlineNameList" items="${carlineBean.searchCarlineNameList }">
																			<c:set var="selectedFlagSearchCarlineName" value="" />
																			<c:if test="${!empty searchCarlineNameList.carlineNameEng}">
																				<c:if test="${searchCarlineNameList.carlineNameEng eq carlineBean.searchCarlineNameId}">
																					<c:set var="selectedFlagSearchCarlineName" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagSearchCarlineName eq 1}">
																					<option selected="selected" value="<c:out value="${searchCarlineNameList.carlineNameEng }"/>"><c:out value="${searchCarlineNameList.carlineNameReg }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${searchCarlineNameList.carlineNameEng }"/>"><c:out value="${searchCarlineNameList.carlineNameReg }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															</tr>
															<tr>
															<td><fmt:message key="label.carcode"/></td>
															<td>
																<select id="CAR_SearchCarlineCode" name="CAR_SearchCarlineCode" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.searchCarlineCodeList }">
																		<c:forEach var="searchCarlineCodeList" items="${carlineBean.searchCarlineCodeList }">
																			<c:set var="selectedFlagSearchCarlineCode" value="" />
																			<c:if test="${!empty searchCarlineCodeList.carlineCode}">
																				<c:if test="${searchCarlineCodeList.carlineCode eq carlineBean.searchCarlineCodeId}">
																					<c:set var="selectedFlagSearchCarlineCode" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagSearchCarlineCode eq 1}">
																					<option selected="selected" value="<c:out value="${searchCarlineCodeList.carlineCode }"/>"><c:out value="${searchCarlineCodeList.carlineCode }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${searchCarlineCodeList.carlineCode }"/>"><c:out value="${searchCarlineCodeList.carlineCode }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
														</tr>
														<tr>
															<td colspan="4" style="text-align:right;">
																<input type="button" onclick="cl_search();" class="bluebutton cursorPointer" value="<fmt:message key="label.search" />"> 
																<input type="button" onclick="cl_clear();" class="gear_button cursorPointer" value="<fmt:message key="label.clear" />">
															</td>
														</tr>
													</c:if>
													
													<c:if test="${carlineBean.mnaoContent eq false }">
														<tr>
															<td><fmt:message key="label.modeltype"/></td>
															<td>
																<select id="CAR_SearchModelType" name="CAR_SearchModelType" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.modelTypeList }">
																		<c:forEach var="modelTypeList" items="${carlineBean.modelTypeList }">
																			<c:set var="selectedFlagMod" value="" />
																			<c:if test="${!empty modelTypeList.value}">
																				<c:if test="${modelTypeList.value eq carlineBean.searchModelTypeId}">
																					<c:set var="selectedFlagMod" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagMod eq 1}">
																					<option selected="selected" value="<c:out value="${modelTypeList.value }"/>"><c:out value="${modelTypeList.label }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${modelTypeList.value }"/>"><c:out value="${modelTypeList.label }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.esicategoryflag"/></td>
															<td>
																<select id="CAR_SearchESICategoryFlag" name="CAR_SearchESICategoryFlag" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.esiCategoryFlagList }">
																		<c:forEach var="esiCategoryFlagList" items="${carlineBean.esiCategoryFlagList }">
																			<c:set var="selectedFlagESI" value="" />
																			<c:if test="${!empty esiCategoryFlagList.value}">
																				<c:if test="${esiCategoryFlagList.value eq carlineBean.searchESICategoryFlagId}">
																					<c:set var="selectedFlagESI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagESI eq 1}">
																					<option selected="selected" value="<c:out value="${esiCategoryFlagList.value }"/>"><c:out value="${esiCategoryFlagList.label }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${esiCategoryFlagList.value }"/>"><c:out value="${esiCategoryFlagList.label }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															</tr>
															<tr>
															<td><fmt:message key="label.wmicode"/></td>
															<td>
																<select id="CAR_SearchWmiCode" name="CAR_SearchWmiCode" 
																	style="width: 200px;" onchange="cl_wmisearchselection();">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewWmiList }">
																		<c:forEach var="searchWmiList" items="${carlineBean.searchWmiList }">
																			<c:set var="selectedFlagSearchWMI" value="" />
																			<c:if test="${!empty searchWmiList.wmiCode}">
																				<c:if test="${searchWmiList.wmiCode eq carlineBean.searchWmiId}">
																					<c:set var="selectedFlagSearchWMI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagSearchWMI eq 1}">
																					<option selected="selected" value="<c:out value="${searchWmiList.wmiCode }"/>"><c:out value="${searchWmiList.wmiCode }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${searchWmiList.wmiCode }"/>"><c:out value="${searchWmiList.wmiCode }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.car"/></td>
															<td>
																<select id="CAR_SearchCarlineName" name="CAR_SearchCarlineName" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.searchCarlineNameList }">
																		<c:forEach var="searchCarlineNameList" items="${carlineBean.searchCarlineNameList }">
																			<c:set var="selectedFlagSearchCarlineName" value="" />
																			<c:if test="${!empty searchCarlineNameList.carlineNameEng}">
																				<c:if test="${searchCarlineNameList.carlineNameEng eq carlineBean.searchCarlineNameId}">
																					<c:set var="selectedFlagSearchCarlineName" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagSearchCarlineName eq 1}">
																					<option selected="selected" value="<c:out value="${searchCarlineNameList.carlineNameEng }"/>"><c:out value="${searchCarlineNameList.carlineNameReg }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${searchCarlineNameList.carlineNameEng }"/>"><c:out value="${searchCarlineNameList.carlineNameReg }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
														</tr>
														<tr>
															<td colspan="4" style="text-align:right;">
																<input type="button" onclick="cl_search();" class="bluebutton cursorPointer" value="<fmt:message key="label.search" />"> 
																<input type="button" onclick="cl_clear();" class="gear_button cursorPointer" value="<fmt:message key="label.clear" />">
															</td>
														</tr>
													</c:if>
												</table>
											</td>
										</tr>
									</c:if>
									
									
									<c:if test="${carlineBean.showAdd eq true }">	
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
													<c:if test="${carlineBean.mnaoContent eq true }">
														<tr>
															<td><fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
															<td>
																<select id="CAR_AddNewWmiCode" name="CAR_AddNewWmiCode" 
																	onchange="cl_wmiSelection();" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewWmiList }">
																		<c:forEach var="addNewWmiList" items="${carlineBean.addNewWmiList }">
																			<c:set var="selectedFlagWMI" value="" />
																			<c:if test="${!empty addNewWmiList.wmiCode}">
																				<c:if test="${addNewWmiList.wmiCode eq carlineBean.addNewWmiId}">
																					<c:set var="selectedFlagWMI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagWMI eq 1}">
																					<option selected="selected" value="<c:out value="${addNewWmiList.wmiCode }"/>"><c:out value="${addNewWmiList.wmiCode }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${addNewWmiList.wmiCode }"/>"><c:out value="${addNewWmiList.wmiCode }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.car"/><span class="mandatory">*</span></td>
															<td>
																<select id="CAR_AddNewCarlineNameId" name="CAR_AddNewCarlineNameId" 
																	onchange="cl_carlineNameSelection();" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewCarlineNameList }">
																		<c:forEach var="addNewCarlineNameList" items="${carlineBean.addNewCarlineNameList }">
																			<c:set var="selectedFlagCarlineName" value="" />
																			<c:if test="${!empty addNewCarlineNameList.carlineNameEng}">
																				<c:if test="${addNewCarlineNameList.carlineNameEng eq carlineBean.addNewCarlineNameId}">
																					<c:set var="selectedFlagCarlineName" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagCarlineName eq 1}">
																					<option selected="selected" value="<c:out value="${addNewCarlineNameList.carlineNameEng }"/>"><c:out value="${addNewCarlineNameList.carlineNameReg }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${addNewCarlineNameList.carlineNameEng }"/>"><c:out value="${addNewCarlineNameList.carlineNameReg }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.carcode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 185px;" id="CAR_CarCode" name="CAR_CarCode"  value="<c:out value="${carlineBean.fieldDetails.carlineCode }" />" /></td>
														</tr>
														
														<c:if test="${!empty carlineBean.addNewWmiId && carlineBean.addNewWmiId=='ADDNEW' }">
															<tr>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" maxlength="5" id="CAR_NewWmiCode" name="CAR_NewWmiCode" value="<c:out value="${carlineBean.newWmiFieldValue }" />"/></td>	
															</tr>
														</c:if>
														
														<c:if test="${!empty carlineBean.addNewCarlineNameId && carlineBean.addNewCarlineNameId=='ADDNEW' }">
															<tr>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.carname.reg"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" id="CAR_NewCarlineNameReg" name="CAR_NewCarlineNameReg" 
																	value="<c:out value="${carlineBean.newCarlineNameRegFieldValue }" />"/></td>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.carname.eng"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" id="CAR_NewCarlineNameEng" name="CAR_NewCarlineNameEng" 
																 	value="<c:out value="${carlineBean.newCarlineNameEngFieldValue }" />"/></td>
															</tr>
														</c:if>
														
														<tr>
															<td><fmt:message key="label.yearstart"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 185px;" maxlength="4" onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" id="CAR_YearStart" name="CAR_YearStart" value="<c:out value="${carlineBean.fieldDetails.yearStart }" />"/></td>
															<td><fmt:message key="label.yearend"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 185px;" id="CAR_YearEnd" maxlength="4" onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" name="CAR_YearEnd" value="<c:out value="${carlineBean.fieldDetails.yearEnd }" />"/></td>
															<td><fmt:message key="label.icdisplaycode"/></td>
															<td><input type="text" style="width: 185px;" id="CAR_ICDisplayCarCode" name="CAR_ICDisplayCarCode"  value="<c:out value="${carlineBean.fieldDetails.icDisplayCarlineCode }" />" /></td>
														</tr>
														<tr>
															<td style="text-align: right;" colspan="6">
																<input type="submit" name="CAR_Entry" id="CAR_Entry" onclick="cl_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
															</td>
														</tr>
													</c:if>
													
													<c:if test="${carlineBean.mnaoContent eq false }">
														<tr>
															<td><fmt:message key="label.modeltype"/></td>
															<td>
																<select id="CAR_AddNewModelType" name="CAR_AddNewModelType" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.modelTypeList }">
																		<c:forEach var="modelTypeList" items="${carlineBean.modelTypeList }">
																			<c:set var="selectedFlagMod" value="" />
																			<c:if test="${!empty modelTypeList.value}">
																				<c:if test="${modelTypeList.value eq carlineBean.addNewModelTypeId}">
																					<c:set var="selectedFlagMod" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagMod eq 1}">
																					<option selected="selected" value="<c:out value="${modelTypeList.value }"/>"><c:out value="${modelTypeList.label }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${modelTypeList.value }"/>"><c:out value="${modelTypeList.label }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.esicategoryflag"/></td>
															<td>
																<select id="CAR_AddNewESICategoryFlag" name="CAR_AddNewESICategoryFlag" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.esiCategoryFlagList }">
																		<c:forEach var="esiCategoryFlagList" items="${carlineBean.esiCategoryFlagList }">
																			<c:set var="selectedFlagESI" value="" />
																			<c:if test="${!empty esiCategoryFlagList.value}">
																				<c:if test="${esiCategoryFlagList.value eq carlineBean.addNewESICategoryFlagId}">
																					<c:set var="selectedFlagESI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagESI eq 1}">
																					<option selected="selected" value="<c:out value="${esiCategoryFlagList.value }"/>"><c:out value="${esiCategoryFlagList.label }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${esiCategoryFlagList.value }"/>"><c:out value="${esiCategoryFlagList.label }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
															<td>
																<select id="CAR_AddNewWmiCode" name="CAR_AddNewWmiCode" onchange="cl_wmiSelection();" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewWmiList }">
																		<c:forEach var="addNewWmiList" items="${carlineBean.addNewWmiList }">
																			<c:set var="selectedFlagWMI" value="" />
																			<c:if test="${!empty addNewWmiList.wmiCode}">
																				<c:if test="${addNewWmiList.wmiCode eq carlineBean.addNewWmiId}">
																					<c:set var="selectedFlagWMI" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagWMI eq 1}">
																					<option selected="selected" value="<c:out value="${addNewWmiList.wmiCode }"/>"><c:out value="${addNewWmiList.wmiCode }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${addNewWmiList.wmiCode }"/>"><c:out value="${addNewWmiList.wmiCode }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															</tr>
															<tr>
															<td><fmt:message key="label.car"/><span class="mandatory">*</span></td>
															<td>
																<select id="CAR_AddNewCarlineNameId" name="CAR_AddNewCarlineNameId" onchange="cl_carlineNameSelection();" style="width: 200px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty carlineBean.addNewCarlineNameList }">
																		<c:forEach var="addNewCarlineNameList" items="${carlineBean.addNewCarlineNameList }">
																			<c:set var="selectedFlagCarlineName" value="" />
																			<c:if test="${!empty addNewCarlineNameList.carlineNameEng}">
																				<c:if test="${addNewCarlineNameList.carlineNameEng eq carlineBean.addNewCarlineNameId}">
																					<c:set var="selectedFlagCarlineName" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedFlagCarlineName eq 1}">
																					<option selected="selected" value="<c:out value="${addNewCarlineNameList.carlineNameEng }"/>"><c:out value="${addNewCarlineNameList.carlineNameReg }"/></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${addNewCarlineNameList.carlineNameEng }"/>"><c:out value="${addNewCarlineNameList.carlineNameReg }"/></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</c:if>
																</select>
															</td>
															<td><fmt:message key="label.carcode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 185px;" id="CAR_CarCode" name="CAR_CarCode"  value="<c:out value="${carlineBean.fieldDetails.carlineCode }" />" /></td>
															<td><fmt:message key="label.icdisplaycode"/></td>
															<td><input type="text" style="width: 185px;" id="CAR_ICDisplayCarCode" name="CAR_ICDisplayCarCode"  value="<c:out value="${carlineBean.fieldDetails.icDisplayCarlineCode }" />" /></td>
														</tr>
														
														<c:if test="${!empty carlineBean.addNewWmiId && carlineBean.addNewWmiId=='ADDNEW' }">
															<tr>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" maxlength="5" id="CAR_NewWmiCode" name="CAR_NewWmiCode" value="<c:out value="${carlineBean.newWmiFieldValue }" />"/></td>	
															</tr>
														</c:if>
														
														<c:if test="${!empty carlineBean.addNewCarlineNameId && carlineBean.addNewCarlineNameId=='ADDNEW' }">
															<tr>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.carname.reg"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" id="CAR_NewCarlineNameReg" name="CAR_NewCarlineNameReg" 
																	value="<c:out value="${carlineBean.newCarlineNameRegFieldValue }" />"/></td>
																<td><fmt:message key="label.enter"/> <fmt:message key="label.carname.eng"/><span class="mandatory">*</span></td>
																<td><input type="text" style="width: 185px;" id="CAR_NewCarlineNameEng" name="CAR_NewCarlineNameEng" 
																 	value="<c:out value="${carlineBean.newCarlineNameEngFieldValue }" />"/></td>
															</tr>
														</c:if>
														<tr>
															<td style="text-align: right;" colspan="6">
																<input type="submit" name="CAR_Entry" id="CAR_Entry" onclick="cl_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
															</td>
														</tr>
														
													</c:if>
												</table>
											</td>
										</tr>
									</c:if>
								</table>
								
								<c:if test="${ carlineBean.showAdd eq true }"> 
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.car"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.car"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="CAR_File" name="CAR_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="cl_file_upload();" id="CAR_Upload" name="CAR_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="cl_DownloadTemplate" onclick="cl_download_template();"
										title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								
								<c:if test="${carlineBean.showView eq true || carlineBean.showAdd eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								</c:if>
								<c:if test="${carlineBean.showView eq true || carlineBean.showAdd eq true }">
								<c:if test="${!empty carlineBean.carList}">
									<table id="example" class="display historyTable" cellspacing="0"
										width="100%">
										<thead>
											<c:set var="showChkBoxSelected" value="1" />
											<c:if test="${!empty carlineBean.carList }">
												<c:forEach var="carList" items="${carlineBean.carList }">
													<c:if test="${carList.editableFlag eq false }">
														<c:set var="showChkBoxSelected" value="0" />
													</c:if>
												</c:forEach>
											</c:if>
											<c:if test="${empty carlineBean.carList }">
												<c:set var="showChkBoxSelected" value="0" />
											</c:if>
											<tr>
												<th scope="col" width="3%">
													<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
													<c:if test="${carlineBean.showUpdate eq true }"> disabled="disabled" </c:if>
													<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
												</th>
												<th scope="col" width="5%">#.</th>
												<th scope="col" width="8%" ><fmt:message key="label.carcode"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												<th scope="col" width="11%"><fmt:message key="label.carname.reg"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												<th scope="col" width="11%"><fmt:message key="label.carname.eng"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												<th scope="col" width="7%"><fmt:message key="label.wmicode"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												<c:if test="${carlineBean.mnaoContent eq true }">
													<th scope="col" width="8%"><fmt:message key="label.yearstart"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
													<th scope="col" width="8%"><fmt:message key="label.yearend"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												</c:if>
												<c:if test="${carlineBean.mnaoContent eq false }">
													<th scope="col" width="8%"><fmt:message key="label.modeltype"/> <c:if test="${carlineBean.showUpdate eq true }">*</span></c:if></th>
													<th scope="col" width="8%"><fmt:message key="label.esicategoryflag"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												</c:if>
												
												<th scope="col" width="8%"><fmt:message key="label.icdisplaycode"/></th>
												<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${carlineBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
												<th scope="col" width="7%"><fmt:message key="label.imsyncstatus"/></th>
												<th scope="col" width="7%"><fmt:message key="label.entrytime"/></th>
												<th scope="col" width="7%"><fmt:message key="label.updatedtime"/></th>
											</tr>
										</thead>
										<tbody>
											<c:if test="${!empty carlineBean.carList }">
												<c:forEach var="carList" items="${carlineBean.carList }">
													<tr <c:if test="${carList.editableFlag eq true  }">class="selected"</c:if>>
														<td> 
															<input type="checkbox" name="CAR_Selection" onchange="cl_checkBoxManagement();" 
																value="<c:out value="${carList.carlineCodeId }" />" 
																id="CAR_Selection_<c:out value="${carList.carlineCodeId }"  />"
																<c:if test="${carList.editableFlag eq true  }">checked="checked" </c:if> 
																<c:if test="${carlineBean.showUpdate eq true }"> disabled="disabled" </c:if> />
														</td>
														<td><c:out value="${carList.srNo }" /> </td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.carlineCode }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.carlineCode }" />" size="10" 
																	name="CAR_CarList_Code_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_Code_<c:out value="${carList.carlineCodeId }" />"/>
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.carlineNameReg }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.carlineNameReg }" />" size="10" 
																	name="CAR_CarList_Name_Reg_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_Name_Reg_<c:out value="${carList.carlineCodeId }" />"/>
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.carlineNameEng }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.carlineNameEng }" />" size="10" 
																	name="CAR_CarList_Name_En_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_Name_En_<c:out value="${carList.carlineCodeId }" />"/>
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.wmiCode }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.wmiCode }" />" size="10"  maxlength="5"
																	name="CAR_CarList_Wmi_Code_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_Wmi_Code_<c:out value="${carList.carlineCodeId }" />"/>
															</c:if>
														</td>
														<c:if test="${carlineBean.mnaoContent eq true }">
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.yearStart }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.yearStart }" />" size="10" maxlength="4" 
																	onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" 
																	name="CAR_CarList_YearStart_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_YearStart_<c:out value="${carList.carlineCodeId }" />" />
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.yearEnd }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.yearEnd }" />" size="10"  maxlength="4"
																	onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" 
																	name="CAR_CarList_YearEnd_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_YearEnd_<c:out value="${carList.carlineCodeId }" />" />
															</c:if>
														</td>
														</c:if>
														<c:if test="${carlineBean.mnaoContent eq false }">
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.modelType }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<select name="CAR_CarList_ModelType_<c:out value="${carList.carlineCodeId }" />" 
																 id="CAR_CarList_ModelType_<c:out value="${carList.carlineCodeId }" />" style="width: 90px;">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:forEach var="modelTypeList" items="${carlineBean.modelTypeList }">
																		<c:set var="selectedModelType" value="" />
																		<c:if test="${!empty modelTypeList.value}">
																			<c:if test="${carList.modelType eq modelTypeList.value}">
																				<c:set var="selectedModelType" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedModelType eq 1}">
																				<option value="<c:out value="${modelTypeList.value }" />" selected="selected"><c:out value="${modelTypeList.label }" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${modelTypeList.value }" />"><c:out value="${modelTypeList.label }" /></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</select>
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.esiCategoryFlagLabel }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<select name="CAR_CarList_ESICatFlag_<c:out value="${carList.carlineCodeId }" />" 
																	 id="CAR_CarList_ESICatFlag_<c:out value="${carList.carlineCodeId }" />" style="width: 90px;">
																		<option value=""><fmt:message key="label.selectOne"/></option>
																		<c:forEach var="esiCategoryFlagList" items="${carlineBean.esiCategoryFlagList }">
																			<c:set var="selectedESICatFlag" value="" />
																			<c:if test="${!empty esiCategoryFlagList.value}">
																				<c:if test="${carList.esiCategoryFlag eq esiCategoryFlagList.value}">
																					<c:set var="selectedESICatFlag" value="1" />
																				</c:if>
																			</c:if>
																			<c:choose>
																				<c:when test="${selectedESICatFlag eq 1}">
																					<option value="<c:out value="${esiCategoryFlagList.value }" />" selected="selected"><c:out value="${esiCategoryFlagList.label }" /></option>
																				</c:when>
																				<c:otherwise>
																					<option value="<c:out value="${esiCategoryFlagList.value }" />"><c:out value="${esiCategoryFlagList.label }" /></option>
																				</c:otherwise>
																			</c:choose>
																		</c:forEach>
																	</select>
															</c:if>
														</td>
														</c:if>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.icDisplayCarlineCode }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<input style="width: auto !important;" type="text" value="<c:out value="${carList.icDisplayCarlineCode }" />" size="10" 
																	name="CAR_CarList_ICDisplayCode_<c:out value="${carList.carlineCodeId }" />" 
																	id="CAR_CarList_ICDisplayCode_<c:out value="${carList.carlineCodeId }" />"/>
															</c:if>
														</td>
														<td>
															<c:if test="${carList.editableFlag eq false }">
																<label><c:out value="${carList.flagLabel }" /></label>
															</c:if>
															<c:if test="${carList.editableFlag eq true }">
																<select name="CAR_LangList_Flag_<c:out value="${carList.carlineCodeId }" />" id="CAR_LangList_Flag_<c:out value="${carList.carlineCodeId }" />" style="width: 90px;">
																	<c:forEach var="flagList" items="${carlineBean.flagList }">
																		<c:set var="selectedFlag" value="" />
																		<c:if test="${!empty carList.flag}">
																			<c:if test="${carList.flag eq flagList.value}">
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
															<c:if test="${!empty carList.syncStatus }">
																<c:out value="${carList.syncStatus }"/>
															</c:if>
															<c:if test="${empty carList.syncStatus }">-</c:if>
														</td>
														<td>
															<fmt:formatDate value="${carList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
														</td>
														<td>
															<fmt:formatDate value="${carList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
														</td>
													</tr>
												</c:forEach>
											</c:if>
										</tbody>
									</table>
								</c:if>	
								<c:if test="${empty carlineBean.carList}">
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


<c:if test="${!empty carlineBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${carlineBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("CAR_File"))
{
document.getElementById("CAR_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>


<c:if test="${!empty carlineBean.carList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty carlineBean.carList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>

<c:if test="${carlineBean.showUpdate eq false }">
<c:if test="${carlineBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"cl_edit();\" name=\"CAR_Edit\" id=\"CAR_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"cl_delete();\" name=\"CAR_Delete\" id=\"CAR_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"cl_active();\" name=\"CAR_Active\" id=\"CAR_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
	deleteLabel = null;
</script>
</c:if>

<c:if test="${carlineBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"cl_export();\"  name=\"CAR_Export\" id=\"CAR_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${carlineBean.showUpdate eq true && carlineBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"cl_update();\" style=\"float:right\" name=\"CAR_Update\" id=\"CAR_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"CAR_Reset\" id=\"CAR_Reset\" onclick=\"cl_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
</script>
</c:if>


<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


function lengthValidation(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^0-9]/g,'');
}

$(document).ready(function() {
	loadDataTable();
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
	var pageNo=$("#CAR_DataTabel_displayPageNo").val();
	var pageLength=$("#CAR_DataTabel_displayPageLen").val();
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
		cl_checkBoxManagement();
	});
}

function cl_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#CAR_DataTabel_displayPageNo").val(info.page);
	}
	if(null!=length)
	{
		$("#CAR_DataTabel_displayPageLen").val(length);	
	}
	
}

function cl_wmisearchselection()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("SEARCH_WMI_SELECTION");
	$("#CAR_Form").submit();
}

function cl_carnamesearchselection()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("SEARCH_CARLINENAME_SELECTION");
	$("#CAR_Form").submit();
}

function cl_wmiSelection()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("ADD_WMI_SELECTION");
	$("#CAR_Form").submit();
}

function cl_carlineNameSelection()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("");
	$("#CAR_Form").submit();
}
function cl_edit()
{
	cl_readDataTableValues();
	var value=$("#CAR_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#CAR_ActionClicked").val("EDIT_CAR");
		$("#CAR_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}

function cl_delete_action()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("DELETE_CAR");
	$("#CAR_Form").submit();
}

function cl_viewBlock()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("VIEW_BUTTON");
	$("#CAR_Form").submit();
}

function cl_addNewBlock()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("ADD_NEW_BUTTON");
	$("#CAR_Form").submit();	
}

function cl_active_action()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("ACTIVE_CAR");
	$("#CAR_Form").submit();
}

function cl_search()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("SEARCH_CAR");
	$("#CAR_Form").submit();
}

function cl_clear()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("CLEAR_SEARCH");
	$("#CAR_Form").submit();
}


function cl_delete()
{
	$("#MDM_CAR_Error_Message").html("");
	var value=$("#CAR_SelectedRows").val();
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
				  cl_delete_action();
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


function cl_active()
{
	$("#MDM_CAR_Error_Message").html("");
	var value=$("#CAR_SelectedRows").val();
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
					  cl_active_action();
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

function cl_export()
{
	$("#MDM_CAR_Error_Message").html("");
	var value=$("#CAR_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		cl_readDataTableValues();
		$("#CAR_ActionClicked").val("EXPORT_CAR");
		  $("#CAR_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
}

function mdm_show_errorMessage(delMessage)
{
	
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_CAR_Error_Message").html(html);
	window.scrollTo(0,0);
}

function cl_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#CAR_ActionClicked").val("FILE_UPLOAD");
	$("#CAR_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost.
 *
 * This screen's COLUMNS depend on the selected country / language (MNAO markets get
 * YEAR START / YEAR END, the others MODEL TYPE / ESI CATEGORY FLAG), so the selected
 * language is passed along and the download is refused until one is chosen - handing
 * over the wrong layout would be worse than asking.
 */
function cl_download_template()
{
	var mlId = $("#CAR_Lang_Code").val();
	if(null==mlId || mlId=="")
	{
		mdm_show_errorMessage("<fmt:message key="error.template.select.locale.language"/>");
		return;
	}
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=CARLINE&ml="+encodeURIComponent(mlId);
}


function cl_entry()
{
	// show Loader
	$("#loader").show();
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("SAVE_CAR");
	  $("#CAR_Form").submit();
}

function cl_update()
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
	
   	$("#CAR_UpdatedRows").val(newData);
	cl_readDataTableValues();
	$("#CAR_ActionClicked").val("UPDATE_CAR");
	  $("#CAR_Form").submit();
}

function cl_checkBoxManagement()
{
	$("#CAR_SelectedRows").val("");
	var value=$("#CAR_SelectedRows").val();
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
		$("#CAR_SelectedRows").val(result);
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

function cl_reset_with_dialog()
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
				 // show Loader
				$("#loader").show();
				 $("#CAR_ResetAction").val("Reset");
		          $( this ).dialog( "close" );
				  $("#CAR_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		//
}

function cl_reset()
{
	// show Loader
	$("#loader").show();
	$("#CAR_ActionClicked").val("RESET_CAR");
	  $("#CAR_Form").submit();
}

function cl_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#CAR_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#CAR_Form").submit();
}

function cl_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#CAR_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#CAR_Form").submit();
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
		$("#CAR_SelectedRows").val("");
	}
	cl_checkBoxManagement();
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
						 cl_readDataTableValues();
						 $("#CAR_ActionClicked").val("RESET_CAR");
						 $("#CAR_Form").submit();
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