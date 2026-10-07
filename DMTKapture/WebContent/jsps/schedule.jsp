<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<jsp:useBean id="scheduleBean" class="com.mazda.gms3.dmt.bean.ScheduleBean" scope="session"></jsp:useBean>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><fmt:message key="label.title" /> - <fmt:message key="label.schedule"/> </title>
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="css/style.css" type="text/css" rel="stylesheet" />
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
.ui-dialog{width:350px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

.searchIcon:before
{
	content: "\f002";
    color: #fff;
    font-size: 15px;
    font-family: "FontAwesome";
    font-style: normal;
	margin-right:10px;
}

</style>
<jsp:include page="headeragent.jsp" flush="true" />
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
					<li class="margin-left-20"><a
						href="<%=request.getContextPath() %>/settings"><fmt:message key="label.settings"/></a></li>
					<li class="activeLink"><a href="<%=request.getContextPath() %>/schedule"><fmt:message key="label.schedule"/></a></li>
					<li><a href="<%=request.getContextPath() %>/history"><fmt:message key="label.history"/></a></li>
					<li><a href="<%=request.getContextPath() %>/metadatavalidation"><fmt:message key="label.metadatavalidation"/></a></li>	
					<li><a href="<%=request.getContextPath() %>/mdsync"><fmt:message key="label.masterdatasynching" /> </a></li>
				</ul>
				<div class="cb"></div>
			</div>
            <!-- TOP MENU ENDS -->
            
            <form action="<%=request.getContextPath() %>/schedule" id="DMT_SCH_Form" name="DMT_SCH_Form" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
           
           
            
            <input type="hidden" name="DMT_SCH_ActionClicked" id="DMT_SCH_ActionClicked" value="">
             <input type="hidden" name="DMT_SCH_SelectedLocales" id="DMT_SCH_SelectedLocales" value="<c:out value="${scheduleBean.selectedLocalesString }"/>">
              <input type="hidden" name="DMT_SCH_SelectedModels" id="DMT_SCH_SelectedModels" value="<c:out value="${scheduleBean.selectedModelsString }"/>">
               <input type="hidden" name="DMT_SCH_SelectedManualTypes" id="DMT_SCH_SelectedManualTypes" value="<c:out value="${scheduleBean.selectedManualTypesString }"/>">
			<input type="hidden" name="DMT_SCH_displayPageNo" id="DMT_SCH_displayPageNo" value="<c:out value="${scheduleBean.displayPageNo }"/>" />
			<input type="hidden" name="DMT_SCH_displayPageLen" id="DMT_SCH_displayPageLen" value="<c:out value="${scheduleBean.displayPageLength }"/>" />
			<input type="hidden" name="DMT_SCHAT_displayPageNo" id="DMT_SCHAT_displayPageNo" value="<c:out value="${scheduleBean.displayATPageNo }"/>" />
			<input type="hidden" name="DMT_SCHAT_displayPageLen" id="DMT_SCHAT_displayPageLen" value="<c:out value="${scheduleBean.displayATPageLength }"/>" />
            <input type="hidden" name="DMT_SCH_SelectedRows" id="DMT_SCH_SelectedRows" value="<c:out value="${scheduleBean.selectedRows }"/>" />
             <input type="hidden" name="DMT_SCHAT_SelectedRows" id="DMT_SCHAT_SelectedRows" value="<c:out value="${scheduleBean.selectedATRows }"/>" />
            <input type="hidden" name="DMT_SCH_OperationId" id="DMT_SCH_OperationId" value="<c:out value="${scheduleBean.operationId }"/>" />
            
            <input type="hidden" name="DMT_SCHATDCID_displayPageNo" id="DMT_SCHATDCID_displayPageNo" value="<c:out value="${scheduleBean.displayATDCIDPageNo }"/>" />
			<input type="hidden" name="DMT_SCHATDCID_displayPageLen" id="DMT_SCHATDCID_displayPageLen" value="<c:out value="${scheduleBean.displayATDCIDPageLength }"/>" />
            <input type="hidden" name="DMT_SCHAT_JobIdForATOperation" id="DMT_SCHAT_JobIdForATOperation" value="" />
            
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="DMT_TD_Error_Message">
											<c:if test="${!empty scheduleBean.errorMessage }">
												<div class="errorMessage" id="DMT_ERROR_MESSAGE">
													<%
														if(null!=scheduleBean.getErrorMessage() && !"".equals(scheduleBean.getErrorMessage()))
														{
															String[] msgs = scheduleBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty scheduleBean.successMessage }">
												<div class="successMessage" id="DMT_ERROR_MESSAGE">
													<c:out value="${scheduleBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									
									<!--  ADD RADIO SELECTION BUTTON HERE  -->
									<tr style="height:35px;">
										<td align="center" style="font-size:15px;">
											<label><fmt:message key="label.operationtype"/>:</label>
											<c:if test="${!empty scheduleBean.operationList }">
												<c:forEach var="operationList" items="${scheduleBean.operationList }">
													<c:set value="0" var="operationTypeFlag"/>
													<c:if test="${!empty scheduleBean.operationId }">
														<c:if test="${scheduleBean.operationId eq operationList.value }">
															<c:set value="1" var="operationTypeFlag"/>
														</c:if>
													</c:if>
													<c:choose>
														<c:when test="${operationTypeFlag eq 1}">
															<input type="radio" checked="checked" onchange="dmt_sch_optype(this);" name="DMT_SCH_OperationType" id="DMT_SCH_OperationType_<c:out value="${operationList.value }"/>"  value="<c:out value="${operationList.value }"/>" /> <span style="margin-right:15px;"><c:out value="${operationList.label }"/></span>
														</c:when>
														<c:otherwise>
															<input type="radio" name="DMT_SCH_OperationType" onchange="dmt_sch_optype(this);" id="DMT_SCH_OperationType_<c:out value="${operationList.value }"/>"  value="<c:out value="${operationList.value }"/>" /> <span style="margin-right:15px;"><c:out value="${operationList.label }"/></span>
														</c:otherwise>
													</c:choose>	
												</c:forEach>
											</c:if>
										 </td>
									</tr>
								</table>
								
								
								
								<c:if test="${!empty scheduleBean.operationId && scheduleBean.operationId==2 }">
								<!-- ----------------------------------------------------------------------------- -->
								<!-- -------------------------- AUTOMATION PAGE STARTS --------------------------- -->
								<!-- ----------------------------------------------------------------------------- -->
								<table width="100%" cellspacing="1" cellpadding="3" >	
									<tr>
										<td>
											<h3><fmt:message key="label.upload" /> <fmt:message key="label.vinmapping" /></h3>
											<br />
										</td>
									</tr>
									<tr>
										<td>
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td>
														<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
															<label style="width: 250px;"><fmt:message key="label.upload" /> <fmt:message key="label.vinmapping"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
															<input id="uploadFile" class="fileInput" style="width:300px !important;" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
															<div class="fileUpload btn">
																<span><fmt:message key="label.browse"/></span>
																<input id="DMT_SCHAT_File" name="DMT_SCHAT_File" type="file" class="upload" />
															</div>
															<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="dmt_schat_file_upload();" id="DST_SCHAT_Upload" name="DMT_SCHAT_Upload" 
															class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.validatedata"/></button>
															<div class="cb"></div>
														</div>
													</td>
												</tr>
												<c:if test="${scheduleBean.showIdenitfyButton eq true }">
													<tr>
														<td>
															<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
																<label style="width: auto;margin-left: 90px;">
																	<fmt:message key="label.info.identify.impacted.documents.part1" />
																</label>
																<input style="margin-left: 5px; margin-right: 5px;width: 100px;" type="button" id="DMT_SCHAT_SubmitButton" name="DMT_SCHAT_SubmitButton" 
																class="bluebutton cursorPointer" value="<fmt:message key="label.submit"/>"  onclick="dmt_schat_identifyimpacteddocs();" />
																<label style="width: auto;margin-left: 0px !important;">
																	<fmt:message key="label.info.identify.impacted.documents.part2" />
																</label>
																<div class="cb"></div>
															</div>
														</td>
													</tr>
												</c:if>
											</table>
										</td>
									</tr>
								</table>
								
								<table width="100%">	
									<!-- <tr>
										<td>
											<div style="margin: auto;width: 80%;padding-bottom: 10px;">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tbody>
														<tr>
														<td style="text-align:right;">From Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DMT_SCHAT_FromDate" name="DMT_SCHAT_FromDate" value="<c:out value="${scheduleBean.fromDate }" />"></td>
														<td style="text-align:right;">To Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DMT_SCHAT_ToDate" name="DMT_SCHAT_ToDate" value="<c:out value="${scheduleBean.toDate }" />"></td>
														<td style="text-align: center !important;">
															<button type="button" name="search" id="search" onclick="DMT_SCAT_Search();" 
																class="bluebutton cursorPointer"><i class="searchIcon"></i> Search</button>
														</td>
													</tr>
													</tbody>
												</table>
											</div>
										</td>
									</tr> -->
									<tr>
										<td>
											<p style="height: 5px;">&nbsp;</p>
											<h3><fmt:message key="label.job" /> <fmt:message key="label.history" /></h3>
											<br />
											<p class="warningMessage">
												Note: Only records of last 30 days will be shown by default. Inorder to view previous Job executions, please use date filter criteria.
											</p>
										</td>
									</tr>
									<tr>
										<td>
											<div style="padding-bottom: 5px;">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tbody>
														<tr>
														<td style="text-align:right;">From Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DMT_SCHAT_FromDate" name="DMT_SCHAT_FromDate" value="<c:out value="${scheduleBean.fromDate }" />"></td>
														<td style="text-align:right;">To Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DMT_SCHAT_ToDate" name="DMT_SCHAT_ToDate" value="<c:out value="${scheduleBean.toDate }" />"></td>
														<td style="text-align: center !important;">
															<button type="button" name="search" id="search" onclick="DMT_SCAT_Search();" 
																class="bluebutton cursorPointer"><i class="searchIcon"></i> Search</button>
														</td>
													</tr>
													</tbody>
												</table>
											</div>
											<table id="example1" class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th scope="col" width="3%">#.</th>
														<th scope="col" width="13%"><fmt:message key="label.schedulename"/></th>
														<th scope="col" width="12%"><fmt:message key="label.scheduledby"/></th>
														<th scope="col" width="8%"><fmt:message key="label.scheduletime"/></th>
														<th scope="col" width="8%"><fmt:message key="label.finishtime"/></th>
														<th scope="col" width="10%"><fmt:message key="label.total" /> <fmt:message key="label.documents" /></th>
														<th scope="col" width="11%"><fmt:message key="label.schedule" /> <fmt:message key="label.status" /></th>
														<th scope="col" width="10%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
														<th scope="col" width="19%"><fmt:message key="label.remarks"/></th>
														<th scope="col" width="6%"><fmt:message key="label.view"/></th>
													</tr>
												</thead>
												<tbody>
													<c:if test="${!empty scheduleBean.atDocsIdentificationJobList }">
														<c:forEach var="atDocsIdentificationJobList" items="${scheduleBean.atDocsIdentificationJobList }">
															<tr>
																<td><c:out value="${atDocsIdentificationJobList.srNo }"/></td>
																<td><c:out value="${atDocsIdentificationJobList.scheduleName }"></c:out></td>
																<td><c:out value="${atDocsIdentificationJobList.userId }"></c:out></td>
																<td><fmt:formatDate value="${atDocsIdentificationJobList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><fmt:formatDate value="${atDocsIdentificationJobList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><c:out value="${atDocsIdentificationJobList.totalDocsForProcessing }"></c:out></td>
																<td>
																	<c:if test="${atDocsIdentificationJobList.showAbort eq true}">
																		<a href="javascript:void(0);" onclick="dmt_schatdcid_abortConversion('<c:out value="${atDocsIdentificationJobList.scheduleId }"/>');"><c:out value="${atDocsIdentificationJobList.scheduleStatusLabel }"/></a>		
																	</c:if>
																	<c:if test="${atDocsIdentificationJobList.showAbort eq false}">
																		<c:out value="${atDocsIdentificationJobList.scheduleStatusLabel }"/>
																	</c:if>
																</td>
																<td><c:out value="${atDocsIdentificationJobList.jobStatusLabel }"></c:out></td>
																<c:if test="${!empty atDocsIdentificationJobList.remarks }">
																	<td><c:out value="${atDocsIdentificationJobList.remarks }"></c:out></td>
																</c:if>
																<c:if test="${empty atDocsIdentificationJobList.remarks }">
																	<td>-</td>
																</c:if>
																<td>
																	<c:if test="${atDocsIdentificationJobList.showViewButton eq true }">
																		<a href="javascript:void(0);" onclick="dmt_schatdcid_viewItems('<c:out value="${atDocsIdentificationJobList.scheduleId }"/>');"><fmt:message key="label.view" /> </a>
																	</c:if>
																</td>
															</tr>
														</c:forEach>
													</c:if>
												</tbody>
											</table>
										</td>
									</tr>
								</table>
									
								<c:if test="${scheduleBean.showAutomationGridBlock eq true}">
									<c:if test="${!empty scheduleBean.automationItemsList }">	
									<p style="height: 5px;">&nbsp;</p>
									<h4>
										<fmt:message key="label.select" /> <fmt:message key="label.items" />
									</h4>
									<br />
									<table id="example" class="display historyTable" cellspacing="0"
											width="100%">
											<thead>
												<tr>
													<th scope="col" width="3%">
														<input type="checkbox" name="DMT_SCHAT_SelectAll"  id="DMT_SCHAT_SelectAll" onclick="dmt_schat_selectAllOperation(this);" 
															<c:if test="${scheduleBean.selectATAll eq true }"> checked="checked" </c:if> 
														<c:if test="${empty scheduleBean.automationItemsList }">disabled="disabled" </c:if> />
													</th>
													<th scope="col" width="3%">#.</th>
													<th scope="col" width="10%"><fmt:message key="label.locale"/></th>
													<th scope="col" width="20%"><fmt:message key="label.channel"/></th>
													<th scope="col" width="15%"><fmt:message key="label.documenttype"/></th>
													<th scope="col" width="15%"><fmt:message key="label.carlinedetails"/></th>
													<th scope="col" width="14%"><fmt:message key="label.totaldocsforprocessing"/></th>
													<th scope="col" width="20%"><fmt:message key="label.remarks"/></th>
												</tr>
											</thead>
											<tbody>
												<c:if test="${!empty scheduleBean.automationItemsList }">
													<c:forEach var="automationItemsList" items="${scheduleBean.automationItemsList }">
														<tr <c:if test="${automationItemsList.rowSelected eq true }"> class="selected"  </c:if>>
															<td>
																<c:if test="${automationItemsList.showCheckBox eq true }">
																	<input type="checkbox" name="DMT_SCHAT_ItemSelect"  onclick="dmt_schat_checkBoxManagement();"
																		id="DMT_SCHAT_ItemSelect_<c:out value="${automationItemsList.srNo }"/>" 
																		value="<c:out value="${automationItemsList.srNo }"/>" 
																		<c:if test="${automationItemsList.rowSelected eq true }"> checked="checked"  </c:if> />
																</c:if>	
																<c:if test="${automationItemsList.showCheckBox eq false }">
																	<a href="<%=request.getContextPath() %>/downloadipmfaceliftotherchannelreports?item=<c:out value="${automationItemsList.srNo }"/>" target="_blank" style="text-decoration:none;"><i class="downloadReports"></i></a>
																</c:if>
															</td>
															<td><c:out value="${automationItemsList.srNo }"/></td>
															<td><c:out value="${automationItemsList.locale }"></c:out></td>
															<td><c:out value="${automationItemsList.channelLabel }"></c:out></td>
															<td>
																<c:if test="${!empty automationItemsList.documentTypeLabel }">
																	<c:out value="${automationItemsList.documentTypeLabel }"></c:out>	
																</c:if>
																<c:if test="${empty automationItemsList.documentTypeLabel }">
																-
																</c:if>
															</td>
															<td><c:out value="${automationItemsList.displayCarlineInfo }"></c:out></td>
															<td><c:out value="${automationItemsList.totalDocumentsCount }"></c:out></td>
															<c:if test="${!empty automationItemsList.remarks }">
																<td><c:out value="${automationItemsList.remarks }"></c:out></td>
															</c:if>
															<c:if test="${empty automationItemsList.remarks }">
																<td>-</td>
															</c:if>
															
														</tr>
													</c:forEach>
												</c:if>
											</tbody>
										</table>
										
										</c:if>
										
									<c:if test="${empty scheduleBean.automationItemsList}">
											<!-- <p style="height: 45px;">&nbsp;</p>
											<br />
											
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<tr>
													<td style="border-left: 1px solid #DADADA;"><fmt:message key="label.norecordsfound" /> </td>
												</tr>
											</table>
											 -->	
										 	<p style="height: 45px;">&nbsp;</p>
											<h4><fmt:message key="label.vin.manual.type.mapping" /> </h4>
											<br />
											<table class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th style="border-left: 1px solid #DADADA;text-align: center;" scope="col">#.</th>
														<th style="border-left: 1px solid #DADADA;text-align: center;" scope="col"><fmt:message key="VIN.metadata.supported.filetypes.label" /> <fmt:message key="label.count" /> </th>
														<th style="border-left: 1px solid #DADADA;text-align: center;" scope="col"><fmt:message key="label.remarks"/> </th>
													</tr>
												</thead>
												<tr>
													<td style="border-left: 1px solid #DADADA;text-align: center;" width="10%">1</td>
													<td style="border-left: 1px solid #DADADA;text-align: center;" width="30%"><c:out value="${fn:length(scheduleBean.inputDataList)}"/> </td>
													<td style="border-left: 1px solid #DADADA;text-align: center;" width="60%"><fmt:message key="label.automation.vin.manual.type.mapping.remarks" /></td>
												</tr>
											</table>
									</c:if>
									<br />
										<table width="100%" cellspacing="1" cellpadding="3" >
											<tr>
												<td align="right">
													<input type="button" id="DMT_SCHAT_Reset" class="gear_button cursorPointer"  onclick="dmt_schat_reset();" value="<fmt:message key="label.reset" />"/>
													<input type="button" id="DMT_SCHAT_Schedule" class="bluebutton cursorPointer"  onclick="dmt_schat_conversion();" value="<fmt:message key="label.schedule" />"/>
												</td>
											</tr>
										</table>
								</c:if>					
								
								<!-- ----------------------------------------------------------------------------- -->
								<!-- -------------------------- AUTOMATION PAGE STARTS --------------------------- -->
								<!-- ----------------------------------------------------------------------------- -->
								</c:if>
								
								
								
								
								<c:if test="${!empty scheduleBean.operationId && scheduleBean.operationId==1 }">
								<!-- ----------------------------------------------------------------------------- -->
								<!-- --------------------------- DATA LOAD PAGE STARTS --------------------------- -->
								<!-- ----------------------------------------------------------------------------- -->
								<c:if test="${scheduleBean.showJSPData eq true }">
									<table width="100%" cellspacing="1" cellpadding="3" >	
										<tr>
											<td>
												<h3><fmt:message key="label.search" /> <fmt:message key="label.criteria" /></h3>
												<br />
											</td>
										</tr>
										<tr>
											<td>
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tr>
														<td><fmt:message key="label.market"/><span class="mandatory">*</span></td>
														<td>
															<select id="DMT_SCH_MarketId" name="DMT_SCH_MarketId" onchange="dmt_marketSelection();" style="width: 110px;">
																<option value=""><fmt:message key="label.selectone" /> </option>
																<c:if test="${!empty scheduleBean.marketList }">
																	<c:forEach var="marketList" items="${scheduleBean.marketList }">
																		<c:set var="selectedMarket" value="" />
																		<c:if test="${!empty scheduleBean.marketId}">
																			<c:if test="${scheduleBean.marketId eq marketList.value}">
																				<c:set var="selectedMarket" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedMarket eq 1}">
																				<option value="<c:out value="${marketList.value }" />" selected="selected"><c:out value="${marketList.label }" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${marketList.value }" />"><c:out value="${marketList.label }" /></option>
																			</c:otherwise>
																		</c:choose>		
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.locale"/></td>
														<td>
															<select multiple="multiple" id="DMT_SCH_Locales" class="listBox" name="DMT_SCH_Locales" onchange="dmt_localeSelection();" style="width: 110px;">
																<c:if test="${!empty scheduleBean.localesList }">
																	<c:forEach var="localesList" items="${scheduleBean.localesList }">
																		<c:set var="selectedLocale" value="" />
																		<c:if test="${!empty scheduleBean.selectedLocales }">
																			<c:forEach var="selLocales" items="${scheduleBean.selectedLocales }">
																				<c:if test="${selLocales eq localesList.value}">
																					<c:set var="selectedLocale" value="1" />
																				</c:if>
																			</c:forEach>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedLocale eq 1}">
																				<option value="<c:out value="${localesList.value }" />" selected="selected"><c:out value="${localesList.label }" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${localesList.value }" />"><c:out value="${localesList.label }" /></option>
																			</c:otherwise>
																		</c:choose>	
																	</c:forEach>
																</c:if>
																<c:if test="${empty scheduleBean.localesList }">
																	<option value=""><fmt:message key="label.selectone" /> </option>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.models"/></td>
														<td>
															<select multiple="multiple" id="DMT_SCH_Models" class="listBox" name="DMT_SCH_Models" onchange="dmt_modelSelection();">
																<c:if test="${!empty scheduleBean.modelsList }">
																	<c:forEach var="modelsList" items="${scheduleBean.modelsList }">
																		<c:set var="selectedModel" value="" />
																		<c:if test="${!empty scheduleBean.selectedModels }">
																			<c:forEach var="selModels" items="${scheduleBean.selectedModels }">
																				<c:if test="${selModels eq modelsList.value}">
																					<c:set var="selectedModel" value="1" />
																				</c:if>
																			</c:forEach>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedModel eq 1}">
																				<option value="<c:out value="${modelsList.value }" />" selected="selected"><c:out value="${modelsList.label }" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${modelsList.value }" />"><c:out value="${modelsList.label }" /></option>
																			</c:otherwise>
																		</c:choose>	
																	</c:forEach>
																</c:if>
																<c:if test="${empty scheduleBean.modelsList }">
																	<option value=""><fmt:message key="label.selectone" /> </option>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.manualtype"/></td>
														<td>
															<select multiple="multiple" id="DMT_SCH_ManualTypes" class="listBox" name="DMT_SCH_ManualTypes" onchange="dmt_manualTypeSelection();" style="width: 110px;">
																<c:if test="${!empty scheduleBean.manualTypesList }">
																	<c:forEach var="manualTypesList" items="${scheduleBean.manualTypesList }">
																		<c:set var="selectedManualTypes" value="" />
																		<c:if test="${!empty scheduleBean.selectedModels }">
																			<c:forEach var="selManualTypes" items="${scheduleBean.selectedManualTypes }">
																				<c:if test="${selManualTypes eq manualTypesList.value}">
																					<c:set var="selectedManualTypes" value="1" />
																				</c:if>
																			</c:forEach>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedManualTypes eq 1}">
																				<option value="<c:out value="${manualTypesList.value }" />" selected="selected"><c:out value="${manualTypesList.label }" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${manualTypesList.value }" />"><c:out value="${manualTypesList.label }" /></option>
																			</c:otherwise>
																		</c:choose>	
																	</c:forEach>
																</c:if>
																<c:if test="${empty scheduleBean.manualTypesList }">
																	<option value=""><fmt:message key="label.selectone" /> </option>
																</c:if>
															</select>
														</td>
														<td>
															<input type="button" value="<fmt:message key="label.go"/>" style="float: right;" onclick="dmt_filterRecords();" class="bluebutton cursorPointer"/>
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</table>
									
									<c:if test="${!empty scheduleBean.scheduleItemsList }">
										<p style="height: 45px;">&nbsp;</p>
										<h4>
											<fmt:message key="label.select" /> <fmt:message key="label.items" />
										</h4>
											
										<br />
										<table id="example" class="display historyTable" cellspacing="0"
											width="100%">
											<thead>
												<tr>
													<th scope="col" width="3%">
														<input type="checkbox" name="DMT_SCH_SelectAll"  id="DMT_SCH_SelectAll" onclick="dmt_selectAllOperation(this);" 
															<c:if test="${scheduleBean.selectAll eq true }"> checked="checked" </c:if> 
														<c:if test="${empty scheduleBean.scheduleItemsList }">disabled="disabled" </c:if> />
													</th>
													<th scope="col" width="3%">#.</th>
													<th scope="col" width="5%"><fmt:message key="label.market"/></th>
													<th scope="col" width="5%"><fmt:message key="label.locale"/></th>
													<th scope="col" width="6%"><fmt:message key="label.model"/></th>
													<th scope="col" width="8%"><fmt:message key="label.manualtype"/></th>
													<th scope="col" width="7%"><fmt:message key="label.materialname"/></th>
													<th scope="col" width="8%"><fmt:message key="label.totaldocsforprocessing"/></th>
													<th scope="col" width="8%"><fmt:message key="label.esicat"/> / <fmt:message key="label.leftmenu"/> <fmt:message key="label.docs"/> <fmt:message key="label.count"/></th>
													<th scope="col" width="7%"><fmt:message key="label.folderfiles"/> <fmt:message key="label.count"/></th>
													<th scope="col" width="8%"><fmt:message key="label.okassetscount"/></th>
													<th scope="col" width="8%"><fmt:message key="label.totaldocsfordeletion"/></th>
													<th scope="col" width="8%"><fmt:message key="label.totaldisplayordercount"/></th>
													<th scope="col" width="8%"><fmt:message key="label.totalcdprocessingcount"/></th>
													<th scope="col" width="8%"><fmt:message key="label.totalscmvinmappingcount"/></th>
												</tr>
											</thead>
											<tbody>
												<c:if test="${!empty scheduleBean.scheduleItemsList }">
													<c:forEach var="scheduleItemsList" items="${scheduleBean.scheduleItemsList }">
														<tr <c:if test="${scheduleItemsList.rowSelected eq true }"> class="selected"  </c:if>>
															<td>
																<input type="checkbox" name="DMT_SCH_ItemSelect"  onclick="dmt_sch_checkBoxManagement();"
																	id="DMT_SCH_ItemSelect_<c:out value="${scheduleItemsList.srNo }"/>" 
																	value="<c:out value="${scheduleItemsList.srNo }"/>" 
																	<c:if test="${scheduleItemsList.rowSelected eq true }"> checked="checked"  </c:if> />
															</td>
															<td><c:out value="${scheduleItemsList.srNo }"/></td>
															<td><c:out value="${scheduleItemsList.market }"></c:out></td>
															<td><c:out value="${scheduleItemsList.locale }"></c:out></td>
															<td><c:out value="${scheduleItemsList.modelFolderName }"></c:out></td>
															<td><c:out value="${scheduleItemsList.manualTypeLabel }"></c:out></td>
															<td>
																<c:if test="${!empty scheduleItemsList.materialFolderName }">
																	<c:out value="${scheduleItemsList.materialFolderName }"></c:out>	
																</c:if>
															</td>
															<td><c:out value="${scheduleItemsList.totalDocsForProcessing }"></c:out></td>
															<td><c:out value="${scheduleItemsList.esiCatLeftMenuCount }"></c:out></td>
															<td><c:out value="${scheduleItemsList.totalFilesCount }"></c:out></td>
															<td><c:out value="${scheduleItemsList.okAssetsCount }"></c:out></td>
															<td><c:out value="${scheduleItemsList.totalDocsForDeletion }"></c:out></td>
															<td><c:out value="${scheduleItemsList.totalDisplayOrderCount }"></c:out></td>
															<td><c:out value="${scheduleItemsList.totalCDProcessingCount }"></c:out></td>
															<td><c:out value="${scheduleItemsList.totalSCMVinCount }"></c:out></td>
														</tr>
													</c:forEach>
												</c:if>
											</tbody>
										</table>
										<br />
										<table width="100%" cellspacing="1" cellpadding="3" >
											<tr>
												<td align="right">
													<input type="button" id="DMT_SCH_Reset" class="gear_button cursorPointer"  onclick="dmt_sch_reset();" value="<fmt:message key="label.reset" />"/>
													<input type="button" id="DMT_SCH_Schedule" class="bluebutton cursorPointer"  onclick="dmt_sch_conversion();" value="<fmt:message key="label.schedule" />"/>
												</td>
											</tr>
										</table>
									</c:if>
									<c:if test="${scheduleBean.showNoRecordTable eq true }">
											<p style="height: 45px;">&nbsp;</p>
											<br />
											
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<tr>
													<td style="border-left: 1px solid #DADADA;"><fmt:message key="label.norecordsfound" /> </td>
												</tr>
											</table>	
									</c:if>
								</c:if>
								<c:if test="${scheduleBean.showJSPData eq false}">
									<table width="100%" cellspacing="1" cellpadding="3" >
										<tr>
											<td>&nbsp;</td>
										</tr>
										<tr>
											<td>
												<fmt:message key="label.networkpath.define.info" /> 
													<fmt:message key="label.please.click.on" /> 
													<a href="<%=request.getContextPath() %>/settings" style="text-decoration: none;" class="bluebutton cursorPointer"><fmt:message key="label.settings" /></a> <fmt:message key="label.define.network.location.remaining.text" />
											</td>
										</tr>
									</table>
								</c:if>
								<!-- ----------------------------------------------------------------------------- -->
								<!-- --------------------------- DATA LOAD PAGE ENDS ----------------------------- -->
								<!-- ----------------------------------------------------------------------------- -->
								</c:if>
							</div>
							<div class="cb"></div>
                        </div>
                    </div>
                </div>
            </div>
      </form>
</div>

<div id="abort_schat_dialog-confirm" title="<fmt:message key="label.abort"/>" class="hide">
	<p>
		<fmt:message key="label.abort.schedule"/>
	</p>
</div>
<div id="reset_dialog-confirm" title="<fmt:message key="label.resetform"/>" class="hide">
	<p>
		<fmt:message key="label.resetdescription"/>
	</p>
</div>

<div id="scheduleconfirmation_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.scheduleconversiondescription"/>
	</p>
</div>

<div id="scheduleautomation_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.scheduleautomationdescription"/>
	</p>
</div>

<div id="schedule_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>&nbsp;</p>
	<p align="center">
		<strong id="DMT_SCH_ConversionMessage"></strong>
	</p>
</div>
</body>

<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true"/>
<!-- FOOTER ENDS -->

<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});

//performs Operation Type Change Submit
function dmt_sch_optype(thisObj)
{
	$("#loader").show();
	$("#DMT_SCH_OperationId").val($(thisObj).val());
	$("#DMT_SCH_ActionClicked").val("OPERATION_CHANGE");
	$("#DMT_SCH_Form").submit();
}
</script>

<c:if test="${!empty scheduleBean.operationId && scheduleBean.operationId==2 }">

<c:if test="${!empty scheduleBean.scheduleATName }">
	<script type="text/javascript">
		var msg = "<c:out value="${scheduleBean.successMessage }"/>";
		dmt_schat_showConfirmation(msg);
		
		function dmt_schat_showConfirmation(message)
		{
			// open dialog
			$("#DMT_SCH_ConversionMessage").html(message);
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
				          url =url+"<%=request.getContextPath()%>/history";
				          window.location.href=url;
				        }  
				  	}
			    });
		}
	</script>	
</c:if>




<script type="text/javascript">
//FILE COMPONENT VALUE SET
if(null!=document.getElementById("DMT_SCHAT_File"))
{
	document.getElementById("DMT_SCHAT_File").onchange = function () {
    	document.getElementById("uploadFile").value = this.value;
	};
}


function dmt_schat_file_upload()
{
	// show Loader
	$("#loader").show();
	dmt_schatDocsIden_readDataTableValues();
 	$("#DMT_SCH_ActionClicked").val("FILE_UPLOAD");
	$("#DMT_SCH_Form").submit();
}

function dmt_schat_identifyimpacteddocs()
{
	// show Loader
	$("#loader").show();
	dmt_schatDocsIden_readDataTableValues();
 	$("#DMT_SCH_ActionClicked").val("IDENTIFY_IMPACTED_DOCUMENTS");
	$("#DMT_SCH_Form").submit();
}

function dmatdcim_refresh()
{
	// show Loader
	$("#loader").show();
	dmt_schatDocsIden_readDataTableValues();
	dmt_schat_readDataTableValues();
 	$("#DMT_SCH_ActionClicked").val("REFRESH_LIST");
	$("#DMT_SCH_Form").submit();
}

function DMT_SCAT_Search()
{
	$("#DMT_TD_Error_Message").html("");
	var fromDate=$("#DMT_SCHAT_FromDate").val();
	var toDate = $("#DMT_SCHAT_ToDate").val();
	if(null!=fromDate && fromDate!='' && null!=toDate && toDate!='')
	{
		var fDate = new Date(fromDate);
		var tDate = new Date(toDate);
		if(fromDate >= toDate)
		{
			var err='<div class="errorMessage">From Date must be less than To Date.</div><div class="cb"></div>';
			$("#DMT_TD_Error_Message").html(err);
		}
		else
		{
			// show Loader
			 $("#loader").show();
			 dmt_schatDocsIden_readDataTableValues();
			 $("#DMT_SCH_ActionClicked").val("SEARCH_JOBS");
			 $("#DMT_SCH_Form").submit();	
		}	
	}
	else
	{
		var err='<div class="errorMessage">From Date & To Date are mandatory.</div><div class="cb"></div>';
		$("#DMT_TD_Error_Message").html(err);
	}
}

/*
 * Documents Identification Table 
 */
var htmlATDocsIdenContent="";
<c:if test="${!empty scheduleBean.atDocsIdentificationJobList }">

var refreshLabel="<fmt:message key="label.refresh"/>";
htmlATDocsIdenContent="<div class=\"separator\"></div>";
htmlATDocsIdenContent=htmlATDocsIdenContent+"<button type=\"button\" style=\"float:right\" onclick=\"dmatdcim_refresh();\"  name=\"DMATDCIM_SCH_Refresh\" id=\"DMATDCIM_SCH_Refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";

</c:if>

function loadATDocsIdenDataTable()
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
	show=show+" ";
	var domDataDM;
	if(version=="6" || version=="7")
	{
		// FOR IE
		domDataDM="<\"top\"fip><\"toolbar\">rt<\"bottom\"p><\"clear\">";
	}
	else
	{
		domDataDM="<\"top\"<\"toolbar\">fip>rt<\"bottom\"p><\"clear\">";
	}
	$('#example1').dataTable({
		"bDestroy":true,
		"dom": domDataDM,
		"pagingType": "input",
		"pageLength":5,
		 /*
		 	"pageLength":5,
		 Disable initial sort */
        "bSort" : true,
        aoColumnDefs: [
                       { aTargets: [ 0 ], bSortable: true },
                       { aTargets: [ 1 ], bSortable: true },
                       { aTargets: [ 2 ], bSortable: true },
                       { aTargets: [ 3 ], bSortable: true },
                       { aTargets: [ 4 ], bSortable: true },
                       { aTargets: [ 5 ], bSortable: true },
                       { aTargets: [ 6 ], bSortable: true },
                       { aTargets: [ 7 ], bSortable: true },
                       { aTargets: [ 8 ], bSortable: true },
                       { aTargets: [ 9 ], bSortable: false }
                    ],
       "order": [[0, 'asc']],
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
	
	$("#example1_wrapper .top .toolbar").html(htmlATDocsIdenContent);
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
	var pageNo=$("#DMT_SCHATDCID_displayPageNo").val();
	var pageLength=$("#DMT_SCHATDCID_displayPageLen").val();
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
}

function dmt_schatDocsIden_readDataTableValues()
{
	var table = $('#example1').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#DMT_SCHATDCID_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#DMT_SCHATDCID_displayPageLen").val(length);	
		}
	}
}

var htmlATContent="";
</script>

<script type="text/javascript">
function datePicketInitialization()
{
	// date pickers initialization
	$( "#DMT_SCHAT_FromDate" ).datepicker({
		dateFormat: 'yy-mm-dd',
		changeYear:true,
		changeMonth:true,
		showButtonPanel: true
	});
	$( "#DMT_SCHAT_ToDate" ).datepicker({
		dateFormat: 'yy-mm-dd',
		changeYear:true,
		changeMonth:true,
		showButtonPanel: true
	});
	// code for moving to today's date when clicked on Today Button
	// code for moving to today's date when clicked on Today Button
	var _gotoToday = jQuery.datepicker._gotoToday;
	jQuery.datepicker._gotoToday = function(a){
    var target = jQuery(a);
    var inst = this._getInst(target[0]);
    _gotoToday.call(this, a);
		jQuery.datepicker._selectDate(a, jQuery.datepicker._formatDate(inst,inst.selectedDay, inst.selectedMonth, inst.selectedYear));
	};	
}


<c:if test="${empty scheduleBean.automationItemsList }">
$(document).ready(function() {
	loadATDocsIdenDataTable();
	
	datePicketInitialization();
	
});
</c:if>


<c:if test="${!empty scheduleBean.automationItemsList }">
$(document).ready(function() {
	loadATDocsIdenDataTable();
	loadATDataTable();
	datePicketInitialization();
	
});


function loadATDataTable()
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
	show=show+" ";
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
	$('#example').dataTable({
		"bDestroy":true,
		"dom": domData,
		"pagingType": "input",
		 /*
		 	"pageLength":5,
		 Disable initial sort */
        "bSort" : true,
        aoColumnDefs: [
                       { aTargets: [ 0 ], bSortable: false },
                       { aTargets: [ 1 ], bSortable: true },
                       { aTargets: [ 2 ], bSortable: true },
                       { aTargets: [ 3 ], bSortable: true },
                       { aTargets: [ 4 ], bSortable: true },
                       { aTargets: [ 5 ], bSortable: true },
                       { aTargets: [ 6 ], bSortable: true }
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
	
	$("div.toolbar").html(htmlATContent);
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
	var pageNo=$("#DMT_SCHAT_displayPageNo").val();
	var pageLength=$("#DMT_SCHAT_displayPageLen").val();
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
		dmt_schat_checkBoxManagement();
	});
}

</c:if>

function dmt_schat_readDataTableValues()
{
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#DMT_SCHAT_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#DMT_SCHAT_displayPageLen").val(length);	
		}
	}
}

function dmt_schat_reset()
{
	// open dialog
	$( "#reset_dialog-confirm" ).removeClass('hide').dialog({
		  draggable:false,
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	  "<fmt:message key="label.reset"/>": function() {
	    		  $( this ).dialog( "close" );
	    		  $("#loader").show();
	    		  dmt_schat_readDataTableValues();
	    		  dmt_schatDocsIden_readDataTableValues();
		          $("#DMT_SCH_ActionClicked").val("RESET_AUTOMATION");
		          $("#DMT_SCH_Form").submit();
		        }  
		  	,
		  	"<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
		$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function dmt_schat_selectAllOperation(thisObj)
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
		$("#DMT_SCHAT_SelectedRows").val("");
	}
	
	dmt_schat_checkBoxManagement();
}

function dmt_schat_checkBoxManagement()
{
	$("#DMT_SCHAT_SelectedRows").val("");
	var value=$("#DMT_SCHAT_SelectedRows").val();
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
		$("#DMT_SCHAT_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - MDM_HeaderCheckBox
	*/
	$("#DMT_SCHAT_SelectAll").prop("checked",false);
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
			$("#DMT_SCHAT_SelectAll").prop("checked",true);
		}
	}	
}


function dmt_schat_conversion()
{
	dmt_schat_checkBoxManagement();
	var selRows = $("#DMT_SCHAT_SelectedRows").val();
	//if(null!=selRows && selRows!="")
	{
		$( "#scheduleautomation_dialog-confirm" ).removeClass('hide').dialog({
			  draggable:false,
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		    	  "<fmt:message key="label.schedule"/>": function() {
		    		  // CLOSE THE DIALOG
		    		  $( this ).dialog( "close" );
		    		 	dmt_schat_checkBoxManagement();
		    		  	dmt_schat_readDataTableValues();
		    		  	dmt_schatDocsIden_readDataTableValues();
		    			$("#loader").show();
		    			$("#DMT_SCH_ActionClicked").val("SCHEDULE_AUTOMATION");
		    		    $("#DMT_SCH_Form").submit();	
			        }  
			  	,
			  	"<fmt:message key="label.cancel"/>": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
			
			$("#scheduleautomation_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
			$("#scheduleautomation_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	/*
		COMMENT ELSE CONDITION BECAUSE VIN MANUAL TYPE UPDATE CAN ALSO HAPPEN AS ALONE
	else
	{
		$("#DMT_TD_Error_Message").html("");
		var message="<div class=\"errorMessage\" id=\"DMT_ERROR_MESSAGE\">";
		message = message+"<fmt:message key="error.message.mandatory.select.one.item" />";
		message = message+"</div>";
		message = message+"<div class=\"cb\"></div>";
		$("#DMT_TD_Error_Message").html(message);
		window.scrollTo(0,0);
		message=null;
	}	*/
}

function dmt_schatdcid_abortConversion(scheduleId)
{
	// open dialog - abort_schat_dialog-confirm
	$( "#abort_schat_dialog-confirm" ).removeClass('hide').dialog({
			draggable:false,
		   closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	"<fmt:message key="label.abort"/>": function() {
		          $( this ).dialog( "close" );
		          dmt_schatdcid_abortConversionSendReq(scheduleId);
		        }  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		$("#abort_schat_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#abort_schat_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function dmt_schatdcid_abortConversionSendReq(scheduleId)
{
	// show Loader
	$("#loader").show();
	dmt_schatDocsIden_readDataTableValues();
	dmt_schat_readDataTableValues();
	$("#DMT_SCH_ActionClicked").val("ABORT_JOB");
	$("#DMT_SCHAT_JobIdForATOperation").val(scheduleId);
	$("#DMT_SCH_Form").submit();	
}

function dmt_schatdcid_viewItems(scheduleId)
{
	// show Loader
	$("#loader").show();
	dmt_schatDocsIden_readDataTableValues();
	dmt_schat_readDataTableValues();
	$("#DMT_SCH_ActionClicked").val("VIEW_IDENTIFIED_DOCS_DETAILS");
	$("#DMT_SCHAT_JobIdForATOperation").val(scheduleId);
	$("#DMT_SCH_Form").submit();	
}
</script>
</c:if>



<c:if test="${!empty scheduleBean.operationId && scheduleBean.operationId==1 }">
<c:if test="${scheduleBean.showJSPData eq true}">

<c:if test="${empty scheduleBean.scheduleItemsList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>

<c:if test="${!empty scheduleBean.scheduleItemsList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
<%-- MC, MME and MNAO markets: a Kapture load is always Draft, the Publish Content job publishes - no choice offered --%>
<c:if test="${!empty scheduleBean.documentStatusList && scheduleBean.marketId ne 'MC' && scheduleBean.marketId ne 'MME' && scheduleBean.marketId ne 'MNAO'}">
	<c:forEach var="documentStatusList" items="${scheduleBean.documentStatusList }">
		<c:set var="selectedStatus" value="" />
		<c:if test="${!empty scheduleBean.documentStatusId}">
			<c:if test="${scheduleBean.documentStatusId eq documentStatusList.value}">
				<c:set var="selectedStatus" value="1" />
			</c:if>
		</c:if>
		
		<c:choose>
			<c:when test="${selectedStatus eq 1}">
				<script type="text/javascript">
					var value="<c:out value="${documentStatusList.value }" />";
					var label="<c:out value="${documentStatusList.label }" />";
					var id="DMT_SCH_DocumentStatus_<c:out value="${documentStatusList.value }" />";
					htmlContent=htmlContent+"<input type=\"radio\" name=\"DMT_SCH_DocumentStatus\" checked=\"checked\"";
					htmlContent=htmlContent+"value=\""+value+"\"";
					htmlContent=htmlContent+"id=\""+id+"\">";
					htmlContent=htmlContent+"<label>"+label+"</label>";
				</script>	
			</c:when>
			<c:otherwise>
				<script type="text/javascript">
					var value="<c:out value="${documentStatusList.value }" />";
					var label="<c:out value="${documentStatusList.label }" />";
					var id="DMT_SCH_DocumentStatus_<c:out value="${documentStatusList.value }" />";
					htmlContent=htmlContent+"<input type=\"radio\" name=\"DMT_SCH_DocumentStatus\" ";
					htmlContent=htmlContent+"value=\""+value+"\"";
					htmlContent=htmlContent+"id=\""+id+"\">";
					htmlContent=htmlContent+"<label>"+label+"</label>";
				</script>
			</c:otherwise>
		</c:choose>
	</c:forEach>	
</c:if>

<%-- MC, MME and MNAO markets: what the schedule loads --%>
<c:if test="${scheduleBean.marketId eq 'MC' || scheduleBean.marketId eq 'MME' || scheduleBean.marketId eq 'MNAO'}">
	<script type="text/javascript">
		var masterDataChecked="<c:if test="${scheduleBean.loadTypeId eq 'MASTER_DATA_WITH_CONTENT'}">checked=\"checked\"</c:if>";
		var onlyContentChecked="<c:if test="${scheduleBean.loadTypeId ne 'MASTER_DATA_WITH_CONTENT'}">checked=\"checked\"</c:if>";
		htmlContent=htmlContent+"<span style=\"margin-left:30px;\"></span>";
		htmlContent=htmlContent+"<input type=\"radio\" name=\"DMT_SCH_LoadType\" value=\"MASTER_DATA_WITH_CONTENT\" id=\"DMT_SCH_LoadType_MasterData\" "+masterDataChecked+">";
		htmlContent=htmlContent+"<label>Master Data with Content</label>";
		htmlContent=htmlContent+"<input type=\"radio\" name=\"DMT_SCH_LoadType\" value=\"ONLY_CONTENT\" id=\"DMT_SCH_LoadType_OnlyContent\" "+onlyContentChecked+">";
		htmlContent=htmlContent+"<label>Only Content</label>";
	</script>
</c:if>

</c:if>
<c:if test="${!empty scheduleBean.scheduleName }">
	<script type="text/javascript">
		var msg = "<c:out value="${scheduleBean.successMessage }"/>";
		dmt_sch_showConfirmation(msg);
		
		function dmt_sch_showConfirmation(message)
		{
			// open dialog
			$("#DMT_SCH_ConversionMessage").html(message);
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
				          url =url+"<%=request.getContextPath()%>/history";
				          window.location.href=url;
				        }  
				  	}
			    });
		}
	</script>	
</c:if>


<script type="text/javascript">
<c:if test="${empty scheduleBean.scheduleItemsList }">
$(document).ready(function() {
	//$("body").unmask();
	
});
</c:if>


<c:if test="${!empty scheduleBean.scheduleItemsList }">
$(document).ready(function() {
	//$("body").unmask();
	loadDataTable();
});

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
	show=show+" ";
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
	$('#example').dataTable({
		"dom": domData,
		"pagingType": "input",
		 /*
		 	"pageLength":5,
		 Disable initial sort */
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
                       { aTargets: [ 13 ], bSortable: true },
                       { aTargets: [ 14 ], bSortable: true }
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
	var pageNo=$("#DMT_SCH_displayPageNo").val();
	var pageLength=$("#DMT_SCH_displayPageLen").val();
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
		dmt_sch_checkBoxManagement();
	});
}

</c:if>

function dmt_sch_readDataTableValues()
{
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#DMT_SCH_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#DMT_SCH_displayPageLen").val(length);	
		}
	}
}


function dmt_marketSelection()
{
	$("#loader").show();
	dmt_sch_readDataTableValues();
	dmt_sch_checkBoxManagement();
	  $("#DMT_SCH_ActionClicked").val("MARKET_SELECTION");
	 $("#DMT_SCH_Form").submit();
}

function dmt_localeSelection()
{
	$("#loader").show();
	/*
		check here, if All is selected, then select all options in the List box
	*/
	
	var selLocales=$("#DMT_SCH_Locales").val();
	if(null!=selLocales && selLocales!="")
	{
		var allString = "All";
		if(selLocales.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#DMT_SCH_Locales").find("option").prop("selected", true);
		}
	}
	
	// DMT_SCH_SelectedLocales
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#DMT_SCH_Locales").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#DMT_SCH_SelectedLocales').val(value);
	
	dmt_sch_checkBoxManagement();
	dmt_sch_readDataTableValues();
	$("#DMT_SCH_ActionClicked").val("LOCALE_SELECTION");
	$("#DMT_SCH_Form").submit();
}

function dmt_modelSelection()
{
	$("#loader").show();
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selModels=$("#DMT_SCH_Models").val();
	var modelValues="";
	if(null!=selModels && selModels!="")
	{
		var allString = "All";
		if(selModels.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#DMT_SCH_Models").find("option").prop("selected", true);
		}
	}
	
	// DMT_SCH_SelectedModels
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#DMT_SCH_Models").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#DMT_SCH_SelectedModels').val(value);
	
	dmt_sch_checkBoxManagement();
	dmt_sch_readDataTableValues();
	$("#DMT_SCH_ActionClicked").val("MODEL_SELECTION");
	$("#DMT_SCH_Form").submit();
}

function dmt_manualTypeSelection()
{
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selManualTypes=$("#DMT_SCH_ManualTypes").val();
	if(null!=selManualTypes && selManualTypes!="")
	{
		var allString = "All";
		if(selManualTypes.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#DMT_SCH_ManualTypes").find("option").prop("selected", true);
		}
	}
	
	// DMT_SCH_SelectedManualTypes
	// identify All SELECTED VALUES AND SET IN HIDDEN FIELD AS COMMA SEPARATE VALUES
	
	var value="";
	$("#DMT_SCH_ManualTypes").find("option").each(function() {
	if ($(this).is(':selected')) {
			value = value+$(this).attr('value')+",";
		}
	});
	
	$('#DMT_SCH_SelectedManualTypes').val(value);
}

function dmt_filterRecords()
{
	// CHECK FOR MARKET ID, IF NULL - THEN SHOW ERROR
	var marketId=$("#DMT_SCH_MarketId").val();
	if(null!=marketId && marketId!="")
	{
		$("#loader").show();
		dmt_sch_readDataTableValues();
		dmt_sch_checkBoxManagement();
		$("#DMT_SCH_ActionClicked").val("FILTER_RECORDS");
	    $("#DMT_SCH_Form").submit();		
	}
	else
	{
		$("#DMT_TD_Error_Message").html("");
		var message="<div class=\"errorMessage\" id=\"DMT_ERROR_MESSAGE\">";
		message = message+"<fmt:message key="error.message.mandatory.fields" />";
		message = message+"</div>";
		message = message+"<div class=\"cb\"></div>";
		$("#DMT_TD_Error_Message").html(message);
		window.scrollTo(0,0);
		message=null;
	}	
}

function dmt_sch_reset()
{
	// open dialog
	$( "#reset_dialog-confirm" ).removeClass('hide').dialog({
		  draggable:false,
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	  "<fmt:message key="label.reset"/>": function() {
	    		  $( this ).dialog( "close" );
	    		  $("#loader").show();
	    		  dmt_sch_readDataTableValues();
		          $("#DMT_SCH_ActionClicked").val("RESET");
		          $("#DMT_SCH_Form").submit();
		        }  
		  	,
		  	"<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
		$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function dmt_selectAllOperation(thisObj)
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
		$("#DMT_SCH_SelectedRows").val("");
	}
	
	dmt_sch_checkBoxManagement();
}

function dmt_sch_checkBoxManagement()
{
	$("#DMT_SCH_SelectedRows").val("");
	var value=$("#DMT_SCH_SelectedRows").val();
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
		$("#DMT_SCH_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - MDM_HeaderCheckBox
	*/
	$("#DMT_SCH_SelectAll").prop("checked",false);
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
			$("#DMT_SCH_SelectAll").prop("checked",true);
		}
	}	
}


function dmt_sch_conversion()
{
	dmt_sch_checkBoxManagement();
	var selRows = $("#DMT_SCH_SelectedRows").val();
	if(null!=selRows && selRows!="")
	{
		$( "#scheduleconfirmation_dialog-confirm" ).removeClass('hide').dialog({
			  draggable:false,
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		    	  "<fmt:message key="label.schedule"/>": function() {
		    		  // CLOSE THE DIALOG
		    		  $( this ).dialog( "close" );
		    		 	dmt_sch_checkBoxManagement();
		    		  	dmt_sch_readDataTableValues();
		    			$("#loader").show();
		    			$("#DMT_SCH_ActionClicked").val("SCHEDULE_CONVERSION");
		    		    $("#DMT_SCH_Form").submit();	
			        }  
			  	,
			  	"<fmt:message key="label.cancel"/>": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
			
			$("#scheduleconfirmation_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
			$("#scheduleconfirmation_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		$("#DMT_TD_Error_Message").html("");
		var message="<div class=\"errorMessage\" id=\"DMT_ERROR_MESSAGE\">";
		message = message+"<fmt:message key="error.message.mandatory.select.one.item.conversion" />";
		message = message+"</div>";
		message = message+"<div class=\"cb\"></div>";
		$("#DMT_TD_Error_Message").html(message);
		window.scrollTo(0,0);
		message=null;
	}	
}

</script>
</c:if>
</c:if>
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
					//$("body").unmask();
					if(token=="SUCCESS")
					{
						 dmt_sch_readDataTableValues();
						 $("#DMT_SCH_ActionClicked").val("RESET");
						 $("#DMT_SCH_Form").submit();
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