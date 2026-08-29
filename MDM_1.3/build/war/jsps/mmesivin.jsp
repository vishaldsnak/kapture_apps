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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_MME_SIVIN_RANGE);
%>


<%
	String applicationContext = ApplicationProperties.getProperty("application.environment.context");
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
<jsp:useBean id="mmeSiVinBean" class="com.mazda.gms3.mdm.bean.MMESIVinBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.MME_SIVIN_RANGE"/> </title>
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

.listBox {
    min-height: 70px !important;
}


.previewTable
{
	border-left: 1px solid #DADADA;
	border-right: 1px solid #DADADA;
	border-bottom: 1px solid #DADADA;
	margin:auto !important;
}


.previewTable th
{
    border-top: 1px solid #DADADA;
    color: #BABABA;
    font-size: 10px;
    font-weight: normal;
    padding: 20px !important;
}

.previewTable td
{
	background-color: #FAFAFA !important;
	border-top: 1px solid #DADADA;
	font-size: 12.2px;
	font-weight: normal;
}

.footerRow
{
	background-color: #fff !important;
	text-align: right !important;
}

#MMESIVIN_INFO_TOOLtip
{
	text-align: left;
	width: 350px;
}

.infoIcon-font:before
{
	font-size: 13.5px !important;
	font-family: "FontAwesome" !important;
    float: left !important;
    color: #34495E !important;
}

.ui-tooltip {
	font-size: 10.5px !important;
	font-weight: normal !important;
	text-align: left !important;
	max-width:500px !important;
	font-family:"InterstateMazda-Regular",Arial;
	color:#5C5B65;
	width:500px !important;
}

.refreshIcon:before
{
	content:"\f021";
	font-family:"FontAwesome";
	font-size:80px;
	font-style:normal;
	color:#AAAAAA;
}


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
	            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.MME_SIVIN_RANGE" /> &rsaquo;</a>
	            </div>
	            <div class="rightPannel" style="margin-right: 30px !important;">
	            	<a href="javascript:void(0);" id="MMESIVIN_INFO_TOOLtip" style="text-decoration: none;" title="<fmt:message key="label.mnao.sivinrange.info.text" />" ><i class="infoIcon infoIcon-font"></i></a>
	            </div>  
            </div>
            <form id="MMESIVIN_Form" name="MMESIVIN_Form" action="<%=request.getContextPath() %>/mmesivin" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="MMESIVIN_SelectedRows" id="MMESIVIN_SelectedRows" value="<c:out value="${mmeSiVinBean.selectedRows }"/>" />
			<input type="hidden" name="MMESIVIN_UpdatedRows" id="MMESIVIN_UpdatedRows" value="" />
			<input type="hidden" name="MMESIVIN_DataTabel_displayPageNo" id="MMESIVIN_DataTabel_displayPageNo" value="<c:out value="${mmeSiVinBean.displayPageNo }"/>" />
			<input type="hidden" name="MMESIVIN_DataTabel_displayPageLen" id="MMESIVIN_DataTabel_displayPageLen" value="<c:out value="${mmeSiVinBean.displayPageLength }"/>" />
			
			<input type="hidden" name="MMESIVIN_VDSSelectedValues" id="MMESIVIN_VDSSelectedValues" value="<c:out value="${mmeSiVinBean.hiddenVDSId }"/>" />
			<input type="hidden" name="MMESIVIN_EngineTypeSelectedValues" id="MMESIVIN_EngineTypeSelectedValues" value="<c:out value="${mmeSiVinBean.hiddenEngineTypeId }"/>" />
			<input type="hidden" name="MMESIVIN_MissionTypeSelectedValues" id="MMESIVIN_MissionTypeSelectedValues" value="<c:out value="${mmeSiVinBean.hiddenMissionTypeId }"/>" />
			<input type="hidden" name="MMESIVIN_Preview_DataTabel_displayPageNo" id="MMESIVIN_Preview_DataTabel_displayPageNo" value="<c:out value="${mmeSiVinBean.displayPreviewPageNo }"/>" />
			<input type="hidden" name="MMESIVIN_Preview_DataTabel_displayPageLen" id="MMESIVIN_Preview_DataTabel_displayPageLen" value="<c:out value="${mmeSiVinBean.displayPreviewPageLength }"/>" />
			<input type="hidden" name="MMESIVIN_Preview_SelectedRows" id="MMESIVIN_Preview_SelectedRows" value="<c:out value="${mmeSiVinBean.selectedPreviewRows }"/>" />
			
			
			<input type="hidden" name="MMESIVIN_ActionClicked" id="MMESIVIN_ActionClicked" value="<c:out value="${mmeSiVinBean.actionClicked }"/>" />
            <input type="hidden" name="MMESIVIN_MapVinSrNo" id="MMESIVIN_MapVinSrNo" value="" />
            
            <input type="hidden" name="MMESIVIN_TranslationUpdate" id="MMESIVIN_TranslationUpdate" value="<c:out value="${mmeSiVinBean.translationsUpdate }"/>" />
            
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_MMESIVIN_Error_Message" colspan="3">
											<c:if test="${!empty mmeSiVinBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=mmeSiVinBean.getErrorMessage() && !"".equals(mmeSiVinBean.getErrorMessage()))
														{
															String[] msgs = mmeSiVinBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty mmeSiVinBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${mmeSiVinBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty mmeSiVinBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${mmeSiVinBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="34%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="MMESIVIN_CountryLocale_Code" name="MMESIVIN_CountryLocale_Code" onchange="mmesivin_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty mmeSiVinBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${mmeSiVinBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty mmeSiVinBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq mmeSiVinBean.countryLocaleId}">
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
												<select id="MMESIVIN_Lang_Code" name="MMESIVIN_Lang_Code" onchange="mmesivin_langSelection();" >
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty mmeSiVinBean.languageList }">
														<c:forEach var="languageList" items="${mmeSiVinBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq mmeSiVinBean.manualLanguageId}">
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
										<td>
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.okmdocid"/>: <span class="mandatory">*</span></label> 
												<input type="text" style="width: 190px;" id="MMESIVIN_SI_Number" name="MMESIVIN_SI_Number"  value="<c:out value="${mmeSiVinBean.siNumber }" />"  />
												<div class="cb"></div>
											</div>	
										</td>
										<td>
											<c:if test="${mmeSiVinBean.showTranslationOperationsControl eq true}">
												<div class="">
													<label style="width:auto;padding-left:50px;"><fmt:message key="label.updatetranslations"/>:</label> 
													<c:if test="${!empty mmeSiVinBean.yesNoList }">
														<c:forEach var="yesNoList" items="${mmeSiVinBean.yesNoList }">
															<c:set value="0" var="operationTypeFlag"/>
															<c:if test="${!empty mmeSiVinBean.translationsUpdate }">
																<c:if test="${mmeSiVinBean.translationsUpdate eq yesNoList.value }">
																	<c:set value="1" var="operationTypeFlag"/>
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${operationTypeFlag eq 1}">
																	<input style="vertical-align:top;margin-left:10px;" type="radio" checked="checked" onchange="mmesivin_optype(this);" name="MMESIVIN_OperationType" id="MMESIVIN_OperationType_<c:out value="${yesNoList.value }"/>"  value="<c:out value="${yesNoList.value }"/>" /> <span style="vertical-align:top;"><c:out value="${yesNoList.label }"/></span>
																</c:when>
																<c:otherwise>
																	<input style="vertical-align:top;margin-left:10px;" type="radio" name="MMESIVIN_OperationType" onchange="mmesivin_optype(this);" id="MMESIVIN_OperationType_<c:out value="${yesNoList.value }"/>"  value="<c:out value="${yesNoList.value }"/>" /> <span style="vertical-align:top;"><c:out value="${yesNoList.label }"/></span>
																</c:otherwise>
															</c:choose>	
														</c:forEach>
													</c:if>
													<div class="cb"></div>
												</div>	
											</c:if>
										</td>
										<td></td>
									</tr>
									<tr>
										<td>
											<div class="formElement_row" style="float: right;">
												<input style="width: 70px;" type="button" name="MMESIVIN_Search" id="MMESIVIN_Search"  onclick="mmesivin_search();" class="bluebutton cursorPointer"  value="<fmt:message key="label.search"/>" /> 
												<input style="margin-left: -15px;width: 70px;margin-right: 60px;" type="button" name="MMESIVIN_SearchClear" id="MMESIVIN_SearchClear" onclick="mmesivin_clearsearch();" class="gear_button cursorPointer"  value="<fmt:message key="label.clear"/>" />
												<div class="cb"></div>
											</div>
										</td>
										<td colspan="2">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<tr>
										<td colspan="3">
											<h3>
												<fmt:message key="label.addvin"></fmt:message>
											</h3>
											<br />
										</td>
									</tr>
									<tr>
										<td colspan="3">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.wmicode"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px">
														<select id="MMESIVIN_WMICode" name="MMESIVIN_WMICode" style="width: 310px;" onchange="mmesivin_wmiSelection();" >
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty mmeSiVinBean.wmiList }">
																<c:forEach var="wmiList" items="${mmeSiVinBean.wmiList }">
																	<c:set var="wmiFlag" value="" />
																	<c:if test="${!empty wmiList.value}">
																		<c:if test="${wmiList.value eq mmeSiVinBean.wmiId}">
																			<c:set var="wmiFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${wmiFlag eq 1}">
																			<option selected="selected" value="<c:out value="${wmiList.value }"/>"><c:out value="${wmiList.label }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${wmiList.value }"/>"><c:out value="${wmiList.label }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.model"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px">
														<select id="MMESIVIN_Model" name="MMESIVIN_Model" style="width: 310px;" onchange="mmesivin_modelSelection();" >
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty mmeSiVinBean.modelsList }">
																<c:forEach var="modelsList" items="${mmeSiVinBean.modelsList }">
																	<c:set var="selectedModelFlag" value="" />
																	<c:if test="${!empty modelsList.carlineIdForCombo}">
																		<c:if test="${modelsList.carlineIdForCombo eq mmeSiVinBean.modelId}">
																			<c:set var="selectedModelFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedModelFlag eq 1}">
																			<option selected="selected" value="<c:out value="${modelsList.carlineIdForCombo }"/>"><c:out value="${modelsList.carlineNameReg }"/> <c:out value="${modelsList.carlineCode }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${modelsList.carlineIdForCombo }"/>"><c:out value="${modelsList.carlineNameReg }"/> <c:out value="${modelsList.carlineCode }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
												</tr>
												<tr>	
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.enginetype"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px">
														<select id="MMESIVIN_EngineType" multiple="multiple" style="width: 310px;" class="listBox" name="MMESIVIN_EngineType" onchange="mmesivin_engTypeSelection();" >
															<c:if test="${!empty mmeSiVinBean.engineTypeList }">
																<c:forEach var="engineTypeList" items="${mmeSiVinBean.engineTypeList }">
																	<c:set var="selectedEngType" value="" />
																	<c:if test="${!empty mmeSiVinBean.engineTypeIds }">
																		<c:forEach var="selEngineTypes" items="${mmeSiVinBean.engineTypeIds }">
																			<c:if test="${selEngineTypes eq engineTypeList.value}">
																				<c:set var="selectedEngType" value="1" />
																			</c:if>
																		</c:forEach>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedEngType eq 1}">
																			<option value="<c:out value="${engineTypeList.value }" />" selected="selected"><c:out value="${engineTypeList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${engineTypeList.value }" />"><c:out value="${engineTypeList.label }" /></option>
																		</c:otherwise>
																	</c:choose>	
																</c:forEach>
															</c:if>
															<c:if test="${empty mmeSiVinBean.engineTypeList }">
																<option value=""><fmt:message key="label.selectOne" /> </option>
															</c:if>
														</select>
													</td>
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.transmissiontype"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px">
														<select multiple="multiple" id="MMESIVIN_MissionType" style="width: 310px;" class="listBox" name="MMESIVIN_MissionType" onchange="mmesivin_missionTypeSelection();" >
															<c:if test="${!empty mmeSiVinBean.missionTypeList }">
																<c:forEach var="missionTypeList" items="${mmeSiVinBean.missionTypeList }">
																	<c:set var="selectedMissionType" value="" />
																	<c:if test="${!empty mmeSiVinBean.missionTypeIds }">
																		<c:forEach var="selMissionTypes" items="${mmeSiVinBean.missionTypeIds }">
																			<c:if test="${selMissionTypes eq missionTypeList.value}">
																				<c:set var="selectedMissionType" value="1" />
																			</c:if>
																		</c:forEach>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedMissionType eq 1}">
																			<option value="<c:out value="${missionTypeList.value }" />" selected="selected"><c:out value="${missionTypeList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${missionTypeList.value }" />"><c:out value="${missionTypeList.label }" /></option>
																		</c:otherwise>
																	</c:choose>	
																</c:forEach>
															</c:if>
															<c:if test="${empty mmeSiVinBean.missionTypeList }">
																<option value=""><fmt:message key="label.selectOne" /> </option>
															</c:if>
														</select>
													</td>
												</tr>
												<tr>	
													<td style="vertical-align: top;text-align: left;" rowspan="3"><fmt:message key="label.vdscode"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px" rowspan="3" >
														<select multiple="multiple" id="MMESIVIN_VDS" style="width: 310px;min-height: 100px !important;" class="listBox" name="MMESIVIN_VDS" onchange="mmesivin_vdsselection();" >
															<c:if test="${!empty mmeSiVinBean.vdsList }">
																<c:forEach var="vdsList" items="${mmeSiVinBean.vdsList }">
																	<c:set var="selectedVDS" value="" />
																	<c:if test="${!empty mmeSiVinBean.vdsId }">
																		<c:forEach var="selVDS" items="${mmeSiVinBean.vdsId }">
																			<c:if test="${selVDS eq vdsList.value}">
																				<c:set var="selectedVDS" value="1" />
																			</c:if>
																		</c:forEach>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedVDS eq 1}">
																			<option value="<c:out value="${vdsList.value }" />" selected="selected"><c:out value="${vdsList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${vdsList.value }" />"><c:out value="${vdsList.label }" /></option>
																		</c:otherwise>
																	</c:choose>	
																</c:forEach>
															</c:if>
															<c:if test="${empty mmeSiVinBean.vdsList }">
																<option value=""><fmt:message key="label.selectOne" /> </option>
															</c:if>
														</select>
													</td>
												</tr>
												<tr>	
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.visstartrange"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px"><input style="width: 300px;" type="text" id="MMESIVIN_VISStartRange" name="MMESIVIN_VISStartRange" value="<c:out value="${mmeSiVinBean.vinStartRange }" />" /></td>
												</tr>
												<tr>	
													<td style="vertical-align: top;text-align: left;"><fmt:message key="label.visendrange"/><span class="mandatory">*</span></td>
													<td style="vertical-align: top;text-align: left;padding-left:27px"><input style="width: 300px;" type="text" id="MMESIVIN_VISEndRange" name="MMESIVIN_VISEndRange" value="<c:out value="${mmeSiVinBean.vinEndRange }" />" /></td>
												</tr>
												<c:if test="${mmeSiVinBean.showAddNewVDSField eq true }">
													<tr>	
														<td style="vertical-align: top;text-align: left;"><fmt:message key="label.enter"/> <fmt:message key="label.vdscode"/><span class="mandatory">*</span></td>
														<td style="vertical-align: top;text-align: left;padding-left:27px"><input style="width: 300px;" type="text" id="MMESIVIN_NEWVDSCode" name="MMESIVIN_NEWVDSCode" value="<c:out value="${mmeSiVinBean.addNewVDSId }" />" /></td>
														<td colspan="2"></td>
													</tr>
												</c:if>
												<tr>
													<td colspan="4" style="text-align: right !important;">
														<input type="button" name="MMESIVIN_Entry" id="MMESIVIN_Entry" class="bluebutton cursorPointer"  onclick="mmesivin_entry();" value="<fmt:message key="label.preview" />" />
													</td>	
												</tr>
												
												<c:if test="${!empty mmeSiVinBean.tempVinsList }">
													<tr>
														<td colspan="4" align="center">
															<h4 style="text-align: left;">
																<fmt:message key="label.preview"/> <fmt:message key="label.vin"/>
															</h4>
															
															<br />
															<table id="example1" class="display historyTable" cellspacing="0" width="100%">
																<thead>
																	<c:set var="showChkBoxPreviewSelected" value="1" />
																	<c:if test="${!empty mmeSiVinBean.tempVinsList }">
																		<c:forEach var="tempVinsList" items="${mmeSiVinBean.tempVinsList }">
																			<c:if test="${tempVinsList.selected eq false }">
																				<c:set var="showChkBoxPreviewSelected" value="0" />
																			</c:if>
																		</c:forEach>
																	</c:if>	
																	<c:if test="${empty mmeSiVinBean.tempVinsList }">
																		<c:set var="showChkBoxPreviewSelected" value="0" />
																	</c:if>
																
																	<tr>
																		<th width="3%">
																			<input type="checkbox" name="MDM_MMESIVIN_AddCheckBoxHeader"  id="MDM_MMESIVIN_AddCheckBoxHeader"   
																			 onclick="mmesivin_preview_selectallrows(this);" <c:choose><c:when test="${showChkBoxPreviewSelected eq 1}"> checked="checked" </c:when></c:choose> />
																		</th>
																		<th width="7%">#.</th>
																		<th width="15%"><fmt:message key="label.model"/></th>
																		<th width="15%"><fmt:message key="label.carcode"/></th>
																		<th width="15%"><fmt:message key="label.wmicode"></fmt:message> </th>
																		<th width="15%"><fmt:message key="label.vdscode"></fmt:message> </th>
																		<th width="15%"><fmt:message key="label.visstartrange"></fmt:message> </th>
																		<th width="15%"><fmt:message key="label.visendrange"></fmt:message> </th>
																	</tr>
																</thead>
																<tbody>	
																	<c:forEach var="tempVinsList" items="${mmeSiVinBean.tempVinsList }">
																		<tr> 
																			<td>
																				<input type="checkbox" name="MDM_MMESIVIN_AddCheckBox" 
																				value="<c:out value="${tempVinsList.srNo }"/>" 
																				 id="MDM_MMESIVIN_AddCheckBoxRow_<c:out value="${tempVinsList.srNo }"/>"  
																				 <c:if test="${tempVinsList.selected eq true}"> checked="checked" </c:if> 
																				  onchange="mmesivin_preview_checkBoxManagement();" />
																			</td>
																			<td><c:out value="${tempVinsList.srNo }"/> </td>
																			<td>
																				<c:if test="${!empty tempVinsList.modelRegionalName }">
																					<c:out value="${tempVinsList.modelRegionalName }" />	
																				</c:if>
																				<c:if test="${empty tempVinsList.modelRegionalName }">
																					<c:out value="${tempVinsList.model }" />
																				</c:if>
																			 </td>
																			 <td><c:out value="${tempVinsList.carlineCode }" /></td>
																			<td><c:out value="${tempVinsList.wmiCode }" /> </td>
																			<td><c:out value="${tempVinsList.vdsCode }" /> </td>
																			<td><c:out value="${tempVinsList.vinStartRange }" /> </td>
																			<td><c:out value="${tempVinsList.vinEndRange }" /> </td>
																		</tr>
																	</c:forEach>
																</tbody>
															</table>
														</td>
													</tr>
													<tr>
														<td align="right" class="footerRow" colspan="4">
															<c:if test="${mmeSiVinBean.showWriteControls eq true }">
																<input type="button" value="<fmt:message key="label.add" /> <fmt:message key="label.vin" />" class="bluebutton  cursorPointer" onclick="mmesivin_permanentsave();" />
															</c:if>
															<input style="margin:-4px;" type="button" name="MMESIVIN_Reset" id="MMESIVIN_Reset" class="gear_button cursorPointer"  onclick="mmesivin_reset_withDialog();" value="<fmt:message key="label.reset"/>" />	
														</td>
													</tr>
												</c:if>
											</table>
										</td>
									</tr>
								</table>
								
								<c:if test="${mmeSiVinBean.showWriteControls eq true}">
								<!-- IMPORT FUNCTIONALITY HERE -->
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.vin"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 180px;"><fmt:message key="label.upload" /> <fmt:message key="label.vin"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span> <a href="<%=request.getContextPath()%>/templates/MME_SIVIN_Import_Template.zip" target="_blank" class="warningMessage" style="padding-left: 30px;padding-top: 4px;"><fmt:message key="label.download" /> <fmt:message key="label.template" /></a></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="MMESIVIN_File" name="MMESIVIN_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="MMESIVIN_file_upload();" id="MMESIVIN_Upload" name="MMESIVIN_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>				
								</c:if>
								
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.document"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty mmeSiVinBean.documentsList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty mmeSiVinBean.documentsList }">
											<c:forEach var="documentsList" items="${mmeSiVinBean.documentsList }">
												<c:if test="${documentsList.selectedRow eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>	
										<c:if test="${empty mmeSiVinBean.documentsList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mmesivin_selectallrows(this);" 
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="6%">#.</th>
											<th scope="col" width="13%"><fmt:message key="label.documentid"/></th>
											<th scope="col" width="13%"><fmt:message key="label.model"/></th>
											<th scope="col" width="13%"><fmt:message key="label.carcode"/></th>
											<th scope="col" width="13%"><fmt:message key="label.wmicode"/></th>
											<th scope="col" width="13%"><fmt:message key="label.vdscode"/></th>
											<th scope="col" width="13%"><fmt:message key="label.visstartrange"/></th>
											<th scope="col" width="13%"><fmt:message key="label.visendrange"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty mmeSiVinBean.documentsList }">
											<c:forEach var="documentsList" items="${mmeSiVinBean.documentsList }">
												<tr <c:if test="${documentsList.selectedRow eq true  }">class="selected"</c:if> >
													<td>
														<!-- CONDITION COMMENTED FOR DEPRECATED ROWS  documentsList.showCheckBox --> 
														<input type="checkbox" name="MDM_MMESIVIN_Selection" onchange="mmesivin_checkBoxManagement();" 
															value="<c:out value="${documentsList.srNo }" />" 
															id="MDM_MMESIVIN_Selection_<c:out value="${documentsList.srNo }"  />"
															<c:if test="${documentsList.selectedRow eq true  }">checked="checked"</c:if>/>
													</td>
													<td><c:out value="${documentsList.srNo }" /></td>
													<td><c:out value="${documentsList.documentId }" /> </td>
													<td>
														<c:if test="${!empty documentsList.modelRegionalName }">
															<c:out value="${documentsList.modelRegionalName }"></c:out>
														</c:if>
														<c:if test="${empty documentsList.modelRegionalName }">
															<c:out value="${documentsList.model }" /> 	
														</c:if>
													</td>
													<td><c:out value="${documentsList.carlineCode }" /></td>
													<td><c:out value="${documentsList.wmiCode }" /> </td>
													<td><c:out value="${documentsList.vdsCode }" /> </td>
													<td><c:out value="${documentsList.vinStartRange }" /> </td>
													<td><c:out value="${documentsList.vinEndRange }" /> </td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty mmeSiVinBean.documentsList }">
									<table cellspacing="0"
											width="100%" class="noRecordTable">
											<tr>
												<td width="15%">&nbsp;</td>
												<td width="70%" class="noRecordText">
													<fmt:message key="label.novin.table.message" />
												</td>
												<td width="15%" style="text-align:right;">
													<i class="noRecordIcon"><i>
												</td>
											</tr>
									</table>	
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

<div id="schedule_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>&nbsp;</p>
	<p align="center">
		<strong id="MDM_SCH_ConversionMessage"></strong>
	</p>
</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<script>
if(null!=document.getElementById("MMESIVIN_File"))
{
document.getElementById("MMESIVIN_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty mmeSiVinBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${mmeSiVinBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>

<c:if test="${!empty mmeSiVinBean.scheduleName }">
	<script type="text/javascript">
		var msg = "<c:out value="${mmeSiVinBean.successMessage }"/>";
		mdm_sch_showConfirmation(msg);
		
		function mdm_sch_showConfirmation(message)
		{
			// open dialog
			$("#MDM_SCH_ConversionMessage").html(message);
			$( "#schedule_dialog-confirm" ).removeClass('hide').dialog({
				  closeOnEscape: false,
				  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			      resizable: false,
			      height:140,
			      modal: true,
			      buttons: {
			    	  "<fmt:message key="label.ok"/>": function() {
				          $( this ).dialog( "close" );
				          // NAVIGATE TO HISTORY PAGE
				          var appCon="<%=applicationContext %>";
				          var url="";
						  if(null!=appCon && appCon!="" && appCon!="null")
						  {
						  	url=appCon;
						  }	
				          url =url+"<%=request.getContextPath()%>/mmesivinbatchtransactions";
				          window.location.href=url;
				        }  
				  	}
			    });
		}
	</script>	
</c:if>


<c:if test="${mmeSiVinBean.showTranslationOperationsControl eq true }">
<script type="text/javascript">
//performs Operation Type Change Value handling
function mmesivin_optype(thisObj)
{
	$("#MMESIVIN_TranslationUpdate").val($(thisObj).val());
}
</script>
</c:if>

<c:if test="${!empty mmeSiVinBean.documentsList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty mmeSiVinBean.documentsList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>

<c:if test="${!empty mmeSiVinBean.tempVinsList }">
<script type="text/javascript">
var htmlPreviewContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty mmeSiVinBean.tempVinsList }">
<script type="text/javascript">
var htmlPreviewContent="";
</script>
</c:if>


<c:if test="${mmeSiVinBean.showWriteControls eq true }">
<script type="text/javascript">
	var deleteLabel="<fmt:message key="label.actual.delete" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"mmesivin_delete();\" name=\"MMESIVIN_Delete\" id=\"MMESIVIN_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
</script>


<c:if test="${mmeSiVinBean.showTranslationOperationsControl eq true }">
<script type="text/javascript">
	var deleteTransLabel="<fmt:message key="label.actual.delete.from.translations" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"mmesivin_deleteTranslations();\" name=\"MMESIVIN_DeleteTranslations\" id=\"MMESIVIN_DeleteTranslations\" class=\"bluebutton cursorPointer\" value=\""+deleteTransLabel+"\"/>";
</script>
</c:if>
</c:if>

<c:if test="${mmeSiVinBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"mmesivin_export();\"  name=\"MMESIVIN_Export\" id=\"MMESIVIN_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


$("#MMESIVIN_INFO_TOOLtip").tooltip();


function lengthValidation(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^a-zA-Z]/g,'');
	if(null!=thisObj.value)
	{
		var value = thisObj.value;
		if(null!=value && value.length>2)
		{
			value=value.substring(0,2);	
		}
		thisObj.value = value;
	}
}


$(document).ready(function() {
	loadDataTable();
	loadPreviewDataTable();
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
                        { aTargets: [ 8 ], bSortable: true }
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
	
	$('div.toolbar').html(htmlContent);
	// change pagination text
	$('#example .paginate_page').text(page);
	var pageOfVal=$('#example .paginate_of:first').text();
	if(null!=pageOfVal && pageOfVal!="")
	{
		pageOfVal = $.trim(pageOfVal);
		if(pageOfVal.indexOf(" ")!=-1)
		{
			var afterValue=pageOfVal.substring(pageOfVal.indexOf(" ")+1, pageOfVal.length);
			afterValue = $.trim(afterValue);
			pageOfVal = of+" "+afterValue;
			$('#example .paginate_of').text(pageOfVal);
		}
	}	
	
	var table = $('#example').DataTable();
	var pageNo=$("#MMESIVIN_DataTabel_displayPageNo").val();
	var pageLength=$("#MMESIVIN_DataTabel_displayPageLen").val();
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
		mmesivin_checkBoxManagement();
	});
}

function loadPreviewDataTable()
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
	$('#example1').dataTable({
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
                        { aTargets: [ 7 ], bSortable: true }
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
	
	$("#example1 .toolbar").html(htmlPreviewContent);
	// change pagination text
	$('#example1 .paginate_page').text(page);
	var pageOfVal=$('#example1 .paginate_of:first').text();
	if(null!=pageOfVal && pageOfVal!="")
	{
		pageOfVal = $.trim(pageOfVal);
		if(pageOfVal.indexOf(" ")!=-1)
		{
			var afterValue=pageOfVal.substring(pageOfVal.indexOf(" ")+1, pageOfVal.length);
			afterValue = $.trim(afterValue);
			pageOfVal = of+" "+afterValue;
			$('#example1 .paginate_of').text(pageOfVal);
		}
	}	
	
	var table = $('#example1').DataTable();
	var pageNo=$("#MMESIVIN_Preview_DataTabel_displayPageNo").val();
	var pageLength=$("#MMESIVIN_Preview_DataTabel_displayPageLen").val();
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
		mmesivin_preview_checkBoxManagement();
	});
}


function mmesivin_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#MMESIVIN_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#MMESIVIN_DataTabel_displayPageLen").val(length);
	}
	
}

function mmesivin_preview_readDataTableValues()
{
	var table = $('#example1').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#MMESIVIN_Preview_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#MMESIVIN_Preview_DataTabel_displayPageLen").val(length);
	}
}

function mmesivin_export()
{
	$("#MDM_MMESIVIN_Error_Message").html("");
	
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	var value=$("#MMESIVIN_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show LOADED
		$("#loader").show();
		$("#MMESIVIN_ActionClicked").val("EXPORT");
		$("#MMESIVIN_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}
}

function MMESIVIN_delete_action()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("REMOVE_VIN");
	$("#MMESIVIN_Form").submit();
}

function MMESIVIN_delete_translation_action()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("REMOVE_VIN_FROM_TRANSLATIONS_TOO");
	$("#MMESIVIN_Form").submit();
}

function mmesivin_delete()
{
	$("#MDM_MMESIVIN_Error_Message").html("");
	var value=$("#MMESIVIN_SelectedRows").val();
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
					  MMESIVIN_delete_action();
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
		var delMessage="<fmt:message key="error.select.onerow.actual.delete" />";
		mdm_show_errorMessage(delMessage);	
	}
}

function mmesivin_deleteTranslations()
{
	$("#MDM_MMESIVIN_Error_Message").html("");
	var value=$("#MMESIVIN_SelectedRows").val();
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
					  MMESIVIN_delete_translation_action();
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
		var delMessage="<fmt:message key="error.select.onerow.actual.delete" />";
		mdm_show_errorMessage(delMessage);	
	}	
}

function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_MMESIVIN_Error_Message").html(html);
	window.scrollTo(0,0);
}

function MMESIVIN_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#MMESIVIN_ActionClicked").val("FILE_UPLOAD");
	$("#MMESIVIN_Form").submit();
}


function mmesivin_localeSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("COUNTRY_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_langSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_wmiSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("WMI_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_modelSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("MODEL_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_yearSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("YEAR_SELCTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_engTypeSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selEng=$("#MMESIVIN_EngineType").val();
	if(null!=selEng && selEng!="")
	{
		var allString = "All";
		if(selEng.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#MMESIVIN_EngineType").find("option").prop("selected", true);
		}
	}
	
	// MMESIVIN_EngineTypeSelectedValues
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#MMESIVIN_EngineType").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#MMESIVIN_EngineTypeSelectedValues').val(value);
	$("#MMESIVIN_ActionClicked").val("ENGINETYPE_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_missionTypeSelection()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	
	
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selMission=$("#MMESIVIN_MissionType").val();
	if(null!=selMission && selMission!="")
	{
		var allString = "All";
		if(selMission.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#MMESIVIN_MissionType").find("option").prop("selected", true);
		}
	}
	// MMESIVIN_MissionTypeSelectedValues
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#MMESIVIN_MissionType").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#MMESIVIN_MissionTypeSelectedValues').val(value);
	
	$("#MMESIVIN_ActionClicked").val("MISSSIONTYPE_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_vdsselection()
{
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selVds=$("#MMESIVIN_VDS").val();
	if(null!=selVds && selVds!="")
	{
		var allString = "All";
		if(selVds.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#MMESIVIN_VDS").find("option").prop("selected", true);
		}
	}
	
	// MMESIVIN_VDSSelectedValues
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#MMESIVIN_VDS").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#MMESIVIN_VDSSelectedValues').val(value);
	$("#MMESIVIN_ActionClicked").val("VDS_SELECTION");
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_entry()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("ADD_VIN");
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_reset()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("RESET_VIN");
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_permanentsave()
{
	
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MDM_MMESIVIN_Error_Message").html("");
	var value=$("#MMESIVIN_Preview_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show LOADED
		$("#loader").show();
		$("#MMESIVIN_ActionClicked").val("PERMANENT_ADD_VIN");
		mmesivin_readDataTableValues();
		mmesivin_preview_readDataTableValues();
		 $("#MMESIVIN_Form").submit();
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.add.vin" />";
		mdm_show_errorMessage(delMessage);	
	}
}

function mmesivin_reset_withDialog()
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
				  $( this ).dialog( "close" );
				// show Loader
					$("#loader").show();
				  mmesivin_reset();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
	
	// add Scripts for coloring the buttons of Dialog box
	$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
	$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}


function mmesivin_search()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	$("#MMESIVIN_ActionClicked").val("SEARCH");
	$("#MMESIVIN_Form").submit();
}

function mmesivin_clearsearch()
{
	// show LOADED
	$("#loader").show();
	mmesivin_preview_checkBoxManagement();
	$("#MMESIVIN_ActionClicked").val("CLEAR_SEARCH");
	mmesivin_readDataTableValues();
	mmesivin_preview_readDataTableValues();
	 $("#MMESIVIN_Form").submit();
}

function mmesivin_checkBoxManagement()
{
	$("#MMESIVIN_SelectedRows").val("");
	var value=$("#MMESIVIN_SelectedRows").val();
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
		$("#MMESIVIN_SelectedRows").val(result);
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


function mmesivin_preview_checkBoxManagement()
{
	$("#MMESIVIN_Preview_SelectedRows").val("");
	var value=$("#MMESIVIN_Preview_SelectedRows").val();
	if(null!=value && value!="")
	{
		value = value+",";
	}
	// Get Selected Checkboxes from data table
	var table = $('#example1').DataTable();
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
		$("#MMESIVIN_Preview_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - MDM_MMESIVIN_AddCheckBoxHeader
	*/
	$("#MDM_MMESIVIN_AddCheckBoxHeader").prop("checked",false);
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
			$("#MDM_MMESIVIN_AddCheckBoxHeader").prop("checked",true);
		}
	}	
}


function mmesivin_selectallrows(thisObj)
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
		$("#MMESIVIN_SelectedRows").val("");
	}
	mmesivin_checkBoxManagement();
}


function mmesivin_preview_selectallrows(thisObj)
{
	if(thisObj.checked==true)
	{
		var table = $('#example1').DataTable();
		var rows = table.rows({ 'search': 'applied' }).nodes();
		$('input[type="checkbox"]', rows).prop('checked', true);
	}
	else
	{
		var table = $('#example1').DataTable();
		var rows = table.rows({ 'search': 'applied' }).nodes();
		$('input[type="checkbox"]', rows).prop('checked', false);
		// set hidden chkboxes selection value to null
		$("#MMESIVIN_Preview_SelectedRows").val("");
	}
	mmesivin_preview_checkBoxManagement();
}

function mmesivin_downloadSampleTemplate()
{
	var appContext = "<%=ApplicationProperties.getProperty("application.environment.context") %>";
	var templateURL = appContext+"/"+"<%=request.getContextPath()%>/templates/MME_SIVIN_Import_Template.zip";
	window.open(templateURL, "_blank");
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
						mmesivin_preview_checkBoxManagement();
						mmesivin_readDataTableValues();
						mmesivin_preview_readDataTableValues();
						 $("#MMESIVIN_ActionClicked").val("RESET");
						 $("#MMESIVIN_Form").submit();
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