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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SI_CHANNEL_DATA_LOAD);
%>

<%
	String applicationContext=ApplicationProperties.getProperty("application.environment.context");	
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
<jsp:useBean id="sichannelDataLoadBean" class="com.mazda.gms3.mdm.sidataload.bean.SIChannelDataLoadBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.SI_CHANNEL_DATA_LOAD"/> </title>
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

#SICH_pending_dialog-confirm
{
	height:auto !important;
}

#SICH_aborted_reports_dialog-confirm
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

.downloadWordIcon:before
{
	content:"\f1c2";
	color:#34495E;
	font-size:20px;
	font-family:"FontAwesome";
	margin-right:10px;
	font-weight:normal;
	font-style:normal;
	cursor:pointer;
}

/* Icon for Help pdf guide */
.pdfIcon:before
{
	content: "\f1c1";
	color:#D0021B;
	font-size: 12px;
	font-family: "FontAwesome";
	font-style: normal;
}

.wordIcon:before
{
	content: "\f1c2";
	color:#D0021B;
	font-size: 12px;
	font-family: "FontAwesome";
	font-style: normal;
}

/* Help icon links */
.helpGuideLinks
{
	text-decoration:none !important;
	font-size:11px !important;
	color:#D0021B !important;
	cursor:pointer !important;
	font-family:"InterstateMazda-Regular",Arial;
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.SI_CHANNEL_DATA_LOAD"/> &rsaquo;</a> 
            </div>			
            <form id="SICH_Form" name="SICH_Form" action="<%=request.getContextPath() %>/sichanneldataload" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
			<input type="hidden" name="SICH_ActionClicked" id="SICH_ActionClicked" value="<c:out value="${sichannelDataLoadBean.actionClicked }"/>" />
			<input type="hidden" name="SICH_OperationType" id="SICH_OperationType" value="<c:out value="${sichannelDataLoadBean.selectedOperationType }"/>" />
            <input type="hidden" name="SICH_Schedule_displayPageNo" id="SICH_Schedule_displayPageNo" value="<c:out value="${categoryBean.displayPageNo }"/>" />
			<input type="hidden" name="SICH_Schedule_displayPageLen" id="SICH_Schedule_displayPageLen" value="<c:out value="${categoryBean.displayPageLength }"/>" />
            <input type="hidden" name="SICH_AbortSchId" id="SICH_AbortSchId" value="">
            
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="SICH_TD_Error_Message" colspan="3">
											<c:if test="${!empty sichannelDataLoadBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=sichannelDataLoadBean.getErrorMessage() && !"".equals(sichannelDataLoadBean.getErrorMessage()))
														{
															String[] msgs = sichannelDataLoadBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty sichannelDataLoadBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sichannelDataLoadBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty sichannelDataLoadBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sichannelDataLoadBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="SICH_CountryLocale_Code" name="SICH_CountryLocale_Code" onchange="SICH_CountrySelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sichannelDataLoadBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${sichannelDataLoadBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty sichannelDataLoadBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq sichannelDataLoadBean.countryLocaleId}">
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
												<select id="SICH_Lang_Code" name="SICH_Lang_Code">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sichannelDataLoadBean.languageList }">
														<c:forEach var="languageList" items="${sichannelDataLoadBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq sichannelDataLoadBean.manualLanguageId}">
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
									<c:if test="${sichannelDataLoadBean.showWriteControls eq true }">
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<tr>
										<td colspan="3">
											<h3>
												<fmt:message key="label.dash.SI_CHANNEL_DATA_LOAD"/>
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
														<c:if test="${!empty sichannelDataLoadBean.operationTypeList }">
															<c:forEach var="operationTypeList" items="${sichannelDataLoadBean.operationTypeList }">
																<c:set var="selectedOperationFlag" value="" />
																<c:if test="${!empty sichannelDataLoadBean.selectedOperationType}">
																	<c:if test="${operationTypeList.value eq sichannelDataLoadBean.selectedOperationType}">
																		<c:set var="selectedOperationFlag" value="1" />
																	</c:if>
																</c:if>
																<c:choose>
																	<c:when test="${selectedOperationFlag eq 1}">
																		<input type="radio" onchange="SICH_OpType(this);" name="SICH_OperationType" id="SICH_OperationType_<c:out value="${operationTypeList.value }"/>" value="<c:out value="${operationTypeList.value }"/>" checked="checked"> <span style="margin-right:15px;"><c:out value="${operationTypeList.label }"/></span>
																	</c:when>
																	<c:otherwise>
																		<input type="radio" onchange="SICH_OpType(this);" name="SICH_OperationType" id="SICH_OperationType_<c:out value="${operationTypeList.value }"/>" value="<c:out value="${operationTypeList.value }"/>"> <span style="margin-right:15px;"><c:out value="${operationTypeList.label }"/></span>
																	</c:otherwise>
																</c:choose>
															</c:forEach>
														</c:if>
													</td>
												</tr>
												<c:if test="${sichannelDataLoadBean.showOldDocumentIdField eq true }">
													<tr>	
														<td>
															<fmt:message key="label.enter"/> <fmt:message key="label.documentid"/> <span class="mandatory">*</span>
														</td>
														<td style="text-align: left;">
															<input type="text" id="SICH_OldDocumentId" name="SICH_OldDocumentId" style="width:240px;" onchange="allowedCharsValidation(this);" onkeyup="allowedCharsValidation(this);" value="<c:out value="${sichannelDataLoadBean.oldDocumentId }"/>" />
														</td>
													</tr>
												</c:if>
												<tr>
													<td>
															<fmt:message key="label.upload" /> <fmt:message key="label.word"/> <fmt:message key="label.file"/><span class="mandatory">*</span><br>
															<a class="helpGuideLinks" href="templates/SI_Import_Guidelines.pdf" target="_blank"><i class="pdfIcon"></i> Import Guidelines</a> <span class="helpGuideLinks">|<span> <a class="helpGuideLinks" href="templates/SI-Sample_Word_Template.docx" target="_blank"><i class="wordIcon"></i> Sample Document</a>
													</td>
													<td>
														<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
															<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
															<div class="fileUpload btn">
																<span><fmt:message key="label.browse"/></span>
																<input id="SICH_File" name="SICH_File" type="file" class="upload" />
															</div>
															<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SICH_file_upload();" id="SICH_Upload" name="SICH_Upload" 
															class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.upload"/></button>
															<div class="cb"></div>
														</div>
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
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="8%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="7%"><fmt:message key="label.scheduledby"/></th>
											<th scope="col" width="8%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="6%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="7%"><fmt:message key="label.documentid"/></th>
											<th scope="col" width="7%"><fmt:message key="label.fetchedversion"/></th>
											<th scope="col" width="7%"><fmt:message key="label.modifiedversion"/></th>
											<th scope="col" width="5%"><fmt:message key="label.publishstatus"/></th>
											<th scope="col" width="8%"><fmt:message key="label.okassets"/> (<fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/>)</th>
											<th scope="col" width="7%"><fmt:message key="label.sch.status"/></th>
											<th scope="col" width="6%"><fmt:message key="label.reports"/></th>
											<th scope="col" width="5%"><fmt:message key="label.word"/></th>
											<th scope="col" width="8%"><fmt:message key="label.remarks"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty sichannelDataLoadBean.scheduleList }">
											<c:forEach var="scheduleList" items="${sichannelDataLoadBean.scheduleList }">
												<tr>
													<td><c:out value="${scheduleList.srNo }"/></td>
													<td><c:out value="${scheduleList.scheduleName }"></c:out></td>
													<td><c:out value="${scheduleList.wslId }"></c:out></td>
													<td><fmt:formatDate value="${scheduleList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${scheduleList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${scheduleList.locale }"></c:out></td>
													<td><c:out value="${scheduleList.documentId }"/></td>
													<td><c:out value="${scheduleList.fetchedVersion }"/></td>
													<td><c:out value="${scheduleList.modifiedVersion }"/></td>
													<td><c:out value="${scheduleList.documentStatus }"/></td>
													<td><c:out value="${scheduleList.okAssetsTotalCount }"/> / <c:out value="${scheduleList.okAssetsSuccessCount }"/> / <c:out value="${scheduleList.okAssetsFailureCount }"/></td>
													<td>
														<c:choose>
															<c:when test="${!empty scheduleList.scheduleStatus && (scheduleList.scheduleStatus=='Uploading File' || scheduleList.scheduleStatus=='Processing'  ) }">
																<a href="javascript:void(0);" onclick="SICH_abortSchedule('<c:out value="${scheduleList.scheduleId }"/>');"><c:out value="${scheduleList.scheduleStatus }"/></a>
															</c:when>
															<c:otherwise>
																<c:out value="${scheduleList.scheduleStatus }"/>
															</c:otherwise>
														</c:choose>
													</td>
													<td>
														<c:if test="${!empty scheduleList.reportsPath }">
														<a href="javascript:void(0);" onclick="SICH_viewReports('<c:out value="${scheduleList.reportsPath }"/>');" style="text-decoration:none;"><i class="downloadReports"></i></a>
														</c:if>
													</td>
													<td>
														<c:if test="${!empty scheduleList.zipFilePath }">
															<a href="javascript:void(0);" onclick="SICH_viewReports('<c:out value="${scheduleList.zipFilePath }"/>');" style="text-decoration:none;"><i class="downloadWordIcon"></i></a>
														</c:if>
													</td>
													<td>
														<c:if test="${!empty scheduleList.errorsComplete }">
															<div class="commentIcon cus_tooltip">
																<span class="cus_tooltiptext"><c:out value="${scheduleList.errorsComplete }"/></span>
															</div>
														</c:if>
														<c:if test="${empty scheduleList.errorsComplete }">
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

<div id="SICH_pending_dialog-confirm" title="<fmt:message key="download" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="SICH_STATUS_LABEL"></u>. <fmt:message key="label.pending.dialog.operation.end" /> </strong>
	</p>
	<br/>
	<br/>
</div>

<div id="SICH_aborted_reports_dialog-confirm" title="<fmt:message key="label.download" />" class="hide">
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
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<script>
if(null!=document.getElementById("SICH_File")){
document.getElementById("SICH_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty sichannelDataLoadBean.successMessage }">
<script type="text/javascript">
setTimeout(function(){ 
	$("div.successMessage").fadeOut("slow");
}, 10000);
</script>
</c:if>

<script type="text/javascript">
var htmlScheduleContent="";
</script>


<c:if test="${!empty sichannelDataLoadBean.scheduleList}">
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
var deleteLabel="<fmt:message key="label.actual.delete"/>";
// add separator when transactionsList is not empty
htmlScheduleContent="<div class=\"separator\"></div>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"SICH_refresh();\"  name=\"SICH_Refresh\" id=\"SICH_Refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
//htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"SICH_deleteSchedule();\" name=\"SICH_Sch_Delete\" id=\"SICH_Sch_Delete\" class=\"bluebutton cursorPointer\"><i class=\"deleteScheduleIcon\"></i> "+deleteLabel+"</button>";
refreshLabel = null;
deleteLabel = null;
</script>
</c:if>



<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});


var runReloadScript="<c:out value="${sichannelDataLoadBean.reloadJSP }" />";

if(runReloadScript=="true")
{
	// reload page after every 2 minutes  
	setTimeout(function()
	{
		SICH_readScheduleDataTableValues();
		$("#SICH_ActionClicked").val("REFRESH");
	    $("#SICH_Form").submit();
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
                        { aTargets: [ 0 ], bSortable: true },
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
	var pageNo=$("#SICH_Schedule_displayPageNo").val();
	var pageLength=$("#SICH_Schedule_displayPageLen").val();
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
		//SICH_checkBoxManagement();
	});
}


function SICH_readScheduleDataTableValues()
{
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#SICH_Schedule_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#SICH_Schedule_displayPageLen").val(length);	
		}
	}
}

function SICH_checkBoxManagement()
{
	$("#SICH_Sch_SelectedRows").val("");
	var value=$("#SICH_Sch_SelectedRows").val();
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
		$("#SICH_Sch_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - SICH_HeaderCheckBox
	*/
	$("#SICH_HeaderCheckBox").prop("checked",false);
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
			$("#SICH_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function SICH_selectallrows(thisObj)
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
		$("#SICH_Sch_SelectedRows").val("");
	}
	//SICH_checkBoxManagement();
}



function SICH_deleteSchedule()
{
	$("#SICH_TD_Error_Message").html("");
	var value=$("#SICH_Sch_SelectedRows").val();
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
						SICH_readScheduleDataTableValues();
						$("#SICH_ActionClicked").val("DELETE_SCHEDULE");
					    $("#SICH_Form").submit();
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
	$("#SICH_TD_Error_Message").html(html);
	window.scrollTo(0,0);
}


function SICH_file_upload()
{
	// show Loader
	$("#loader").show();
	SICH_readScheduleDataTableValues();
 	$("#SICH_ActionClicked").val("FILE_UPLOAD");
	$("#SICH_Form").submit();
}

function SICH_CountrySelection()
{
	// show Loader
	$("#loader").show();
	SICH_readScheduleDataTableValues();
	$("#SICH_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	$("#SICH_Form").submit();
}

//performs Operation Type Change Submit
function SICH_OpType(thisObj)
{
	$("#loader").show();
	SICH_readScheduleDataTableValues();
	$("#SICH_OperationType").val($(thisObj).val());
	// set actionClicked as Blank
	$("#SICH_ActionClicked").val("");
	$("#SICH_Form").submit();
}


function SICH_refresh()
{
	// show Loader
	$("#loader").show();
	SICH_readScheduleDataTableValues();
	$("#SICH_ActionClicked").val("REFRESH");
    $("#SICH_Form").submit();
}


function SICH_viewReports(url)
{
	var appContext = "<%=applicationContext %>";
	url = appContext+url;
	window.open(url);
}

function SICH_downloadExportTemplate()
{
	// DOWNLOAD_EXPORT_TEMPLATE
	$("#SICH_OtherOps_ActionClicked").val("DOWNLOAD_EXPORT_TEMPLATE");
	$("#SITranslationsOtherOpsForm").submit();
}

function SICH_abortScheduleAction(scheduleId)
{
	SICH_readScheduleDataTableValues();
	$("#SICH_ActionClicked").val("ABORT_SCHEDULE");
	$("#SICH_AbortSchId").val(scheduleId);
	$("#SICH_Form").submit();
}

function SICH_abortSchedule(scheduleId)
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
	          SICH_abortScheduleAction(scheduleId);
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
	<input type="hidden" name="SICH_OtherOps_SchId" id="SICH_OtherOps_SchId" value="" />
	<input type="hidden" name="SICH_OtherOps_OpType" id="SICH_OtherOps_OpType" value="" />
	<input type="hidden" name="SICH_OtherOps_ActionClicked" id="SICH_OtherOps_ActionClicked" value="" />
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
						SICH_readDataTableValues();
						 $("#SICH_ActionClicked").val("RESET");
						 $("#SICH_Form").submit();
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