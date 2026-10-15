<%@page import="com.mazda.gms3.mdm.vo.AccessManagementInterface"%>
<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
 <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SI_TRANSLATION_UPDATE);
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
<jsp:useBean id="siTranslationUpdateBean" class="com.mazda.gms3.mdm.bean.SITranslationUpdateBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.SI_TRANSLATION_UPDATE"/> </title>
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

#SITU_pending_dialog-confirm
{
	height:auto !important;
}

#SITU_aborted_reports_dialog-confirm
{
	height:auto !important;
}

.inputTable td
{
	padding: 10px !important;
}

.validateIcon:before
{
	content:"\f00c";
	color:#fff;
	font-size:15px;
	font-family:"FontAwesome";
	margin-right:10px;
	font-weight:normal;
	font-style:normal;
}

.commentIcon:before
{
	content:"\f086";
	color:#34495E;
	font-size:20px;
	font-family:"FontAwesome";
	margin-right:10px;
	font-weight:normal;
	font-style:normal;
	cursor:pointer;
}

/* XML Zip Icon download\f1c6 */

.downloadXMLIcon:before
{
	content:"\f1c6";
	color:#34495E;
	font-size:20px;
	font-family:"FontAwesome";
	margin-right:10px;
	font-weight:normal;
	font-style:normal;
	cursor:pointer;
}

/* Tooltip container */
.cus_tooltip {
  position: relative;
  display: inline-block;
  /*border-bottom: 1px dotted black;  If you want dots under the hoverable text */
}

/* Tooltip text */
.cus_tooltip .cus_tooltiptext {
  visibility: hidden;
  width: 150px;
  background-color: #dedede;
  color: #555555;
  text-align: left;
  padding: 5px 5px;;
  border-radius: 6px;
	top: -5px;
  right: 105%; 
  /* Position the tooltip text - see examples below! */
  position: absolute;
  z-index: 1;
}

/* Show the tooltip text when you mouse over the tooltip container */
.cus_tooltip:hover .cus_tooltiptext {
  visibility: visible;
}

</style>
<jsp:include page="../headeragent.jsp" flush="true" />
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
			<jsp:include page="../languageSelector.jsp" flush="true" />
			<!-- CHANGE LANGUAGE MENU ENDS -->
 			<!-- TOP MENU STARTS -->
			 <jsp:include page="../topMenu.jsp" flush="true" />
			<!-- TOP MENU ENDS -->
            
             <div class="breadcumbs">
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.SI_TRANSLATION_UPDATE"/> &rsaquo;</a> 
            </div>			
            <form id="SITU_Form" name="SITU_Form" action="<%=request.getContextPath() %>/translationupdate" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
			<input type="hidden" name="SITU_ActionClicked" id="SITU_ActionClicked" value="<c:out value="${siTranslationUpdateBean.actionClicked }"/>" />
			<input type="hidden" name="SITU_OperationType" id="SITU_OperationType" value="<c:out value="${siTranslationUpdateBean.operationTypeSelected }"/>" />
            <input type="hidden" name="SITU_Schedule_displayPageNo" id="SITU_Schedule_displayPageNo" value="<c:out value="${siTranslationUpdateBean.scheduleDisplayPageNo }"/>" />
			<input type="hidden" name="SITU_Schedule_displayPageLen" id="SITU_Schedule_displayPageLen" value="<c:out value="${siTranslationUpdateBean.scheduleDisplayPageLength }"/>" />
            <input type="hidden" name="SITU_Sch_SelectedRows" id="SITU_Sch_SelectedRows" value="">
            <input type="hidden" name="SITU_AbortSchId" id="SITU_AbortSchId" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="SITU_TD_Error_Message" colspan="3">
											<c:if test="${!empty siTranslationUpdateBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=siTranslationUpdateBean.getErrorMessage() && !"".equals(siTranslationUpdateBean.getErrorMessage()))
														{
															String[] msgs = siTranslationUpdateBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty siTranslationUpdateBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${siTranslationUpdateBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty siTranslationUpdateBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${siTranslationUpdateBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="SITU_CountryLocale_Code" name="SITU_CountryLocale_Code" onchange="SITU_CountrySelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty siTranslationUpdateBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${siTranslationUpdateBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty siTranslationUpdateBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq siTranslationUpdateBean.countryLocaleId}">
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
												<select id="SITU_Lang_Code" name="SITU_Lang_Code">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty siTranslationUpdateBean.languageList }">
														<c:forEach var="languageList" items="${siTranslationUpdateBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq siTranslationUpdateBean.manualLanguageId}">
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
									<c:if test="${siTranslationUpdateBean.showWriteControls eq true }">
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<tr>
										<td colspan="3">
											<h3>
												<fmt:message key="label.si.translation.update"/>
											</h3>
											<br />
										</td>
									</tr>
									<tr>
										<td colspan="3">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td width="30%"><fmt:message key="label.select"/> <fmt:message key="label.operationtype"/><span class="mandatory">*</span></td>
													<td width="70%" style="text-align: left;">
														<c:if test="${!empty siTranslationUpdateBean.operationTypeList }">
															<c:forEach var="operationTypeList" items="${siTranslationUpdateBean.operationTypeList }">
																<c:set var="selectedOperationFlag" value="" />
																<c:if test="${!empty siTranslationUpdateBean.operationTypeSelected}">
																	<c:if test="${operationTypeList.value eq siTranslationUpdateBean.operationTypeSelected}">
																		<c:set var="selectedOperationFlag" value="1" />
																	</c:if>
																</c:if>
																<c:choose>
																	<c:when test="${selectedOperationFlag eq 1}">
																		<input type="radio" onchange="SITU_OpType(this);" name="SITU_OperationType" id="SITU_OperationType_<c:out value="${operationTypeList.value }"/>" value="<c:out value="${operationTypeList.value }"/>" checked="checked"> <span style="margin-right:15px;"><c:out value="${operationTypeList.label }"/></span>
																	</c:when>
																	<c:otherwise>
																		<input type="radio" onchange="SITU_OpType(this);" name="SITU_OperationType" id="SITU_OperationType_<c:out value="${operationTypeList.value }"/>" value="<c:out value="${operationTypeList.value }"/>"> <span style="margin-right:15px;"><c:out value="${operationTypeList.label }"/></span>
																	</c:otherwise>
																</c:choose>
															</c:forEach>
														</c:if>
													</td>
												</tr>
												<c:if test="${siTranslationUpdateBean.showExportBlock eq true }">
													<tr>	
														<td style="vertical-align: top">
															<fmt:message key="label.enter.document.ids"/> <span class="mandatory">*</span>
															<br/><br/><p style="color:#D0021B !important;margin-top: 12px;"><b><fmt:message key="label.or.uppercase" /></b></p>	
														</td>
														<td style="text-align: left;">
															<textarea rows="3" cols="32" id="SITU_DocumentIds" name="SITU_DocumentIds" onchange="allowedCharsValidation(this);" onkeyup="allowedCharsValidation(this);"><c:out value="${siTranslationUpdateBean.documentIdInput }"/></textarea>
															<button style="margin-left:25px;vertical-align: top;" type="button" onclick="SITU_doc_validate();" id="SITU_ValidateDocIds" name="SITU_ValidateDocIds" 
															class="bluebutton cursorPointer"><i class="validateIcon"></i><fmt:message key="label.validate"/></button>
															<br/>
															<p class="warningMessage" style="margin-top:1px !important;font-size:11px;">
																<i class="infoIcon"></i> <fmt:message key="label.si.trans.documentids.info"/>
															</p>
														</td>
													</tr>
												</c:if>
												<tr>
													<td>
														<c:if test="${siTranslationUpdateBean.showImportBlock eq true }">
															<fmt:message key="label.upload" /> <fmt:message key="label.zip"/> / <fmt:message key="label.xmls" /> <fmt:message key="label.file"/><span class="mandatory">*</span>
															<br/>
															<a href="templates/SIXXXXX.xml" target="_blank" class="warningMessage" style="float:none;padding-top: 4px;">SIXXXXX.xml</a> | 
															<a href="templates/SMXXXXX.xml" target="_blank" class="warningMessage" style="float:none;padding-top: 4px;">SMXXXXX.xml</a> 
														</c:if>
														<c:if test="${siTranslationUpdateBean.showExportBlock eq true }">	
															<fmt:message key="label.upload" /> <fmt:message key="label.excel"/> <fmt:message key="label.file"/>
															<br/>
															<a href="javascript:void(0);" onclick="SITU_downloadExportTemplate();" target="_blank" class="warningMessage" style="float:none;padding-top: 4px;">Download Template</a>
														</c:if>	
													</td>
													<td>
														<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
															<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
															<div class="fileUpload btn">
																<span><fmt:message key="label.browse"/></span>
																<input id="SITU_File" name="SITU_File" type="file" class="upload" />
															</div>
															<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SITU_file_upload();" id="SITU_Upload" name="SITU_Upload" 
															class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.upload"/></button>
															<div class="cb"></div>
															<c:if test="${siTranslationUpdateBean.showImportBlock eq true }">
																<p class="warningMessage" style="margin-top:1px !important;font-size:11px;">
																	<i class="infoIcon"></i> <fmt:message key="label.warning.zip.file.import"></fmt:message>
																</p>
															
															</c:if>
														</div>
													</td>
												</tr>
												<c:if test="${siTranslationUpdateBean.showExportBlock eq true }">
													<c:if test="${!empty siTranslationUpdateBean.totalDocumentIdList}">
														<tr>
															<td colspan="2">
																<table width="100%" border="0" cellspacing="0" cellpadding="0" class="historyTable">
																	<thead>
																		<tr>
																			<th style="border-left: 1px solid #DADADA; width: 15%; text-align: center;" rowspan="2">#.</th>
																			<th style="border-left: 1px solid #DADADA;text-align: center;" colspan="3"><fmt:message key="label.documents.toexport"></fmt:message> ( <fmt:message key="label.total.uppercase" /> / <fmt:message key="label.success" /> / <fmt:message key="label.failure" />)</th>
																		</tr>
																		<tr>
																			<th style="border-left: 1px solid #DADADA;text-align: center;"><fmt:message key="label.total.uppercase" /></th>
																			<th style="border-left: 1px solid #DADADA;text-align: center;"><fmt:message key="label.success" /></th>
																			<th style="border-left: 1px solid #DADADA;text-align: center;"><fmt:message key="label.failure" /></th>
																		</tr>
																	</thead>
																	<tbody>
																		<tr>
																			<td style="border-left: 1px solid #DADADA;text-align: center;">1</td>
																			<td style="border-left: 1px solid #DADADA; text-align: center;">
																				<c:if test="${!empty siTranslationUpdateBean.totalDocumentIdList}">
																					<c:out value="${fn:length(siTranslationUpdateBean.totalDocumentIdList)} "></c:out>
																				</c:if>
																				<c:if test="${empty siTranslationUpdateBean.totalDocumentIdList}">
																					0
																				</c:if> 
																			</td>
																			<td style="border-left: 1px solid #DADADA;text-align: center;">
																				<c:if test="${!empty siTranslationUpdateBean.documentIdList}">
																					<c:out value="${fn:length(siTranslationUpdateBean.documentIdList)} "></c:out>
																				</c:if>
																				<c:if test="${empty siTranslationUpdateBean.documentIdList}">
																					0
																				</c:if>
																			</td>
																			<td style="border-left: 1px solid #DADADA;text-align: center;">
																				<c:if test="${!empty siTranslationUpdateBean.failedDocumentIdList}">
																					<c:out value="${fn:length(siTranslationUpdateBean.failedDocumentIdList)} "></c:out>
																				</c:if>
																				<c:if test="${empty siTranslationUpdateBean.failedDocumentIdList}">
																					0
																				</c:if>
																			</td>
																		</tr>
																	</tbody>
																</table>
															</td>
														</tr>
													</c:if>
												</c:if>
												<c:if test="${siTranslationUpdateBean.showImportBlock eq true }">
													<c:if test="${!empty siTranslationUpdateBean.importFilesCount }">
														<tr>
															<td colspan="2">
																<table width="100%" border="0" cellspacing="0" cellpadding="0" class="historyTable">
																	<thead>
																		<tr>
																			<th style="border-left: 1px solid #DADADA;">#.</th>
																			<th style="border-left: 1px solid #DADADA;"><fmt:message key="label.documents.toimport"/> </th>
																		</tr>
																	</thead>
																	<tbody>
																		<tr>
																			<td style="border-left: 1px solid #DADADA;">1</td>
																			<td style="border-left: 1px solid #DADADA;">
																				<c:out value="${siTranslationUpdateBean.importFilesCount} "></c:out>
																			</td>
																		</tr>
																	</tbody>
																</table>
															</td>
														</tr>
													</c:if>
												</c:if>
												<tr>	
													<td colspan="2" style="text-align: right; !important;">
														<input type="button" name="SITU_Entry" id="SITU_Entry" onclick="SITU_schedule();" class="bluebutton cursorPointer"  value="<fmt:message key="label.schedule"/> <fmt:message key="label.now"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								
								<br/>
                       			<p style="height: 5px;"></p>
                       			<h4 style="font-size:16px;"><fmt:message key="label.schedule"/> <fmt:message key="label.details"/> </h4><br />
                       			<p class="warningMessage">
                       				<fmt:message key="label.show.viewall.schedule.note" />
                       			</p>
                       			<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="SITU_HeaderCheckBox" name="SITU_HeaderCheckBox" onclick="SITU_selectallrows(this);"/>
											</th>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="8%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="8%"><fmt:message key="label.scheduledby"/></th>
											<th scope="col" width="8%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="9%"><fmt:message key="label.operationtype"/></th>
											<th scope="col" width="10%"><fmt:message key="label.documents"/> (<fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/>)</th>
											<th scope="col" width="8%"><fmt:message key="label.sch.status"/></th>
											<th scope="col" width="8%"><fmt:message key="label.processing.status"/></th>
											<th scope="col" width="6%"><fmt:message key="label.reports"/></th>
											<th scope="col" width="6%"><fmt:message key="label.xmls"/></th>
											<th scope="col" width="7%"><fmt:message key="label.remarks"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty siTranslationUpdateBean.scheduleList }">
											<c:forEach var="scheduleList" items="${siTranslationUpdateBean.scheduleList }">
												<tr>
													<td>
														<c:if test="${!empty scheduleList.processingStatus && (scheduleList.processingStatus!='Pending' && scheduleList.processingStatus!='Processing'  )}">
														<input type="checkbox" name="SITU_Selection" onchange="SITU_checkBoxManagement();" 
															value="<c:out value="${scheduleList.scheduleId }" />" 
															id="SITU_Selection_<c:out value="${scheduleList.scheduleId }"  />"/>
														</c:if>	
													</td>
													<td><c:out value="${scheduleList.srNo }"/></td>
													<td><c:out value="${scheduleList.scheduleName }"></c:out></td>
													<td><c:out value="${scheduleList.userId }"></c:out></td>
													<td><fmt:formatDate value="${scheduleList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${scheduleList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${scheduleList.localeCode }"></c:out></td>
													<td><c:out value="${scheduleList.operationType }"/></td>
													<td><c:out value="${scheduleList.totalCount }"/> / <c:out value="${scheduleList.successCount }"/> / <c:out value="${scheduleList.failureCount }"/></td>
													<td><c:out value="${scheduleList.jobStatus }"></c:out> </td>
													<td>
														<c:choose>
															<c:when test="${!empty scheduleList.processingStatus && (scheduleList.processingStatus=='Pending' || scheduleList.processingStatus=='Processing'  ) }">
																<a href="javascript:void(0);" onclick="SITU_abortSchedule('<c:out value="${scheduleList.scheduleId }"/>');"><c:out value="${scheduleList.processingStatus }"/></a>
															</c:when>
															<c:otherwise>
																<c:out value="${scheduleList.processingStatus }"/>
															</c:otherwise>
														</c:choose>
													</td>
													<td>
														<a href="javascript:void(0);" onclick="SITU_viewReports('<c:out value="${scheduleList.processingStatus }"/>','<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.operationType }"/>','VR');" style="text-decoration:none;"><i class="downloadReports"></i></a>
													</td>
													<td>
														<c:if test="${!empty scheduleList.operationType && scheduleList.operationType == 'Export' }">
															<a href="javascript:void(0);" onclick="SITU_viewReports('<c:out value="${scheduleList.processingStatus }"/>','<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.operationType }"/>','VX');" style="text-decoration:none;"><i class="downloadXMLIcon"></i></a>
														</c:if>
													</td>
													<td>
														<c:if test="${scheduleList.processingStatus!='Aborted' }">
															<c:if test="${!empty scheduleList.remarks }">
																<div class="commentIcon cus_tooltip">
																	<span class="cus_tooltiptext"><c:out value="${scheduleList.remarks }"/></span>
																</div>
															</c:if>
															<c:if test="${empty scheduleList.remarks }">
															-
															</c:if>
														</c:if>
														<c:if test="${scheduleList.processingStatus=='Aborted' }">
															- 
														</c:if>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>
                        		
							</div>
                        </div>
                    </div>
                </div>
            </div>
       </form>     
</div>

<div id="SITU_pending_dialog-confirm" title="<fmt:message key="download" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="SITU_STATUS_LABEL"></u>. <fmt:message key="label.pending.dialog.operation.end" /> </strong>
	</p>
	<br/>
	<br/>
</div>

<div id="SITU_aborted_reports_dialog-confirm" title="<fmt:message key="label.download" />" class="hide">
	<p>
		<strong><fmt:message key="label.norecordsfound" /></strong>
	</p>
	<br/>
	<br/>
</div>

<div id="delete_schedule_dialog-confirm" title="<fmt:message key="label.actual.delete"/> <fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.delete.translation.schedule.description"/>
	</p>
</div>

<div id="abort_dialog-confirm" title="<fmt:message key="label.abortschedule"/>" class="hide">
	<p>
		<fmt:message key="label.abortscheduledescription"/>
	</p>
</div> 

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<script>
if(null!=document.getElementById("SITU_File")){
document.getElementById("SITU_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty siTranslationUpdateBean.successMessage }">
<script type="text/javascript">
setTimeout(function(){ 
	$("div.successMessage").fadeOut("slow");
}, 10000);
</script>
</c:if>

<script type="text/javascript">
var htmlScheduleContent="";
</script>


<c:if test="${!empty siTranslationUpdateBean.scheduleList}">
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
var deleteLabel="<fmt:message key="label.actual.delete"/>";
// add separator when transactionsList is not empty
htmlScheduleContent="<div class=\"separator\"></div>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"SITU_refresh();\"  name=\"SITU_Refresh\" id=\"SITU_Refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"SITU_deleteSchedule();\" name=\"SITU_Sch_Delete\" id=\"SITU_Sch_Delete\" class=\"bluebutton cursorPointer\"><i class=\"deleteScheduleIcon\"></i> "+deleteLabel+"</button>";
refreshLabel = null;
deleteLabel = null;
</script>
</c:if>



<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});


var runReloadScript="<c:out value="${siTranslationUpdateBean.reloadJSP }" />";

if(runReloadScript=="true")
{
	// reload page after every 2 minutes  
	setTimeout(function()
	{
		SITU_readScheduleDataTableValues();
		$("#SITU_ActionClicked").val("REFRESH");
	    $("#SITU_Form").submit();
	}, 120000);
}


$(document).ready(function() {
	loadScheduleDataTable();
});

function loadScheduleDataTable()
{
	// data table labels
	var show="<fmt:message key="label.show"/>";
	var entries="<fmt:message key="label.entries"/>";
	var zeroRec =  "<fmt:message key="label.norecordsfound"/>"; 
	var of="<fmt:message key="label.of"/>";
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
                        { aTargets: [ 12 ], bSortable: true },
                        { aTargets: [ 13 ], bSortable: true }
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
	
	$('#example_wrapper .top .toolbar').html(htmlScheduleContent);
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
	var pageNo=$("#SITU_Schedule_displayPageNo").val();
	var pageLength=$("#SITU_Schedule_displayPageLen").val();
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
		SITU_checkBoxManagement();
	});
}


function SITU_readScheduleDataTableValues()
{
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#SITU_Schedule_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#SITU_Schedule_displayPageLen").val(length);	
		}
	}
}

function SITU_checkBoxManagement()
{
	$("#SITU_Sch_SelectedRows").val("");
	var value=$("#SITU_Sch_SelectedRows").val();
	if(value==null || value=="undefined" || value=="null")
	{
		value="";
	}
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
		$("#SITU_Sch_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - SITU_HeaderCheckBox
	*/
	$("#SITU_HeaderCheckBox").prop("checked",false);
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
			$("#SITU_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function SITU_selectallrows(thisObj)
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
		$("#SITU_Sch_SelectedRows").val("");
	}
	SITU_checkBoxManagement();
}



function SITU_deleteSchedule()
{
	$("#SITU_TD_Error_Message").html("");
	var value=$("#SITU_Sch_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#delete_schedule_dialog-confirm" ).removeClass('hide').dialog({
			  draggable:false,
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		    	  "<fmt:message key="label.yes"/>": function() {
					  	$( this ).dialog( "close" );
					 	// show Loader
					  	$("#loader").show();
						SITU_readScheduleDataTableValues();
						$("#SITU_ActionClicked").val("DELETE_SCHEDULE");
					    $("#SITU_Form").submit();
			        }  
			  	,
			  	"<fmt:message key="label.no"/>": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
		
		$("#delete_schedule_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#delete_schedule_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		// SHOW ERROR MESSAGE
		mdm_show_errorMessage("<fmt:message key="error.select.onerow.schedule.delete" />");
	}
}

function allowedCharsValidation(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^a-zA-Z0-9,]/g,'');
	//thisObj.value=thisObj.value.toLowerCase();
}

function mdm_show_errorMessage(message)
{
	
	var html="<div class=\"errorMessage\">"+message+"</div>";
	$("#SITU_TD_Error_Message").html(html);
	window.scrollTo(0,0);
}

function SITU_doc_validate()
{
	// show Loader
	$("#loader").show();
	SITU_readScheduleDataTableValues();
 	$("#SITU_ActionClicked").val("VALIDATE_EXPORT_IDS");
	$("#SITU_Form").submit();	
}

function SITU_file_upload()
{
	// show Loader
	$("#loader").show();
	SITU_readScheduleDataTableValues();
 	$("#SITU_ActionClicked").val("FILE_UPLOAD");
	$("#SITU_Form").submit();
}

function SITU_CountrySelection()
{
	// show Loader
	$("#loader").show();
	SITU_readScheduleDataTableValues();
	$("#SITU_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#SITU_Form").submit();
}

//performs Operation Type Change Submit
function SITU_OpType(thisObj)
{
	$("#loader").show();
	SITU_readScheduleDataTableValues();
	$("#SITU_OperationType").val($(thisObj).val());
	// set actionClicked as Blank
	$("#SITU_ActionClicked").val("");
	$("#SITU_Form").submit();
}

function SITU_schedule()
{
	// show Loader
	$("#loader").show();
	SITU_readScheduleDataTableValues();
	$("#SITU_ActionClicked").val("SCHEDULE_NOW");
	 $("#SITU_Form").submit();
}

function SITU_refresh()
{
	// show Loader
	$("#loader").show();
	SITU_readScheduleDataTableValues();
	$("#SITU_ActionClicked").val("REFRESH");
    $("#SITU_Form").submit();
}


function SITU_viewReports(status, scheduleCode, operationType, downloadType)
{
	if(status=="Pending" || status=="Processing")
	{
		// set status Label
		$('#SITU_STATUS_LABEL').html(status);
		
		// open dialog
		$( "#SITU_pending_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		        "<fmt:message key="label.close" />": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
		//$("#SITU_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#SITU_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
	}
	else if(status=="Aborted")
	{
		// open dialog - SITU_aborted_reports_dialog-confirm
		$( "#SITU_aborted_reports_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		        "<fmt:message key="label.close" />": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
		//$("#SITU_aborted_reports_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#SITU_aborted_reports_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
	}
	else
	{
		$("#SITU_OtherOps_SchId").val(scheduleCode);
		$("#SITU_OtherOps_OpType").val(operationType);
		if(downloadType =='VR')
		{
			$("#SITU_OtherOps_ActionClicked").val("VIEW_REPORT");	
		}
		else if(downloadType =='VX')
		{
			$("#SITU_OtherOps_ActionClicked").val("VIEW_XMLS");
		}
		$("#SITranslationsOtherOpsForm").submit();
	}
}

function SITU_downloadExportTemplate()
{
	// DOWNLOAD_EXPORT_TEMPLATE
	$("#SITU_OtherOps_ActionClicked").val("DOWNLOAD_EXPORT_TEMPLATE");
	$("#SITranslationsOtherOpsForm").submit();
}

function SITU_abortScheduleAction(scheduleId)
{
	SITU_readScheduleDataTableValues();
	$("#SITU_ActionClicked").val("ABORT_SCHEDULE");
	$("#SITU_AbortSchId").val(scheduleId);
	$("#SITU_Form").submit();
}

function SITU_abortSchedule(scheduleId)
{
	// open dialog - abort_schat_dialog-confirm
	$( "#abort_dialog-confirm" ).removeClass('hide').dialog({
		draggable:false,
	   closeOnEscape: false,
	  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
      resizable: false,
      height:140,
      modal: true,
      buttons: {
    	"<fmt:message key="label.abort"/>": function() {
	          $( this ).dialog( "close" );
	          SITU_abortScheduleAction(scheduleId);
	        }  
	  	,
        "<fmt:message key="label.cancel"/>": function() {
          $( this ).dialog( "close" );
        }
      }
    });
	$("#abort_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
	$("#abort_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
			
}


</script>

<form method="post" name="SITranslationsOtherOpsForm" id="SITranslationsOtherOpsForm" action="<%=request.getContextPath() %>/translationotherops" target="_blank">
	<input type="hidden" name="SITU_OtherOps_SchId" id="SITU_OtherOps_SchId" value="" />
	<input type="hidden" name="SITU_OtherOps_OpType" id="SITU_OtherOps_OpType" value="" />
	<input type="hidden" name="SITU_OtherOps_ActionClicked" id="SITU_OtherOps_ActionClicked" value="" />
</form>


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
						SITU_readDataTableValues();
						 $("#SITU_ActionClicked").val("RESET");
						 $("#SITU_Form").submit();
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