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
	String icContext=ApplicationProperties.getProperty("GMS3_INFOCENTER_WEB_CONTEXT");	
	String locale=ApplicationProperties.getProperty("locales.values.english");
	Object localeObj=request.getSession().getAttribute("MDVAL_LS_Locale");
	if(null!=localeObj && !"".equals(localeObj))
	{
		locale = (String)localeObj;
	}
	localeObj = null;
	String mnaoLocales = ApplicationProperties.getProperty("mnao.locales");
	mnaoLocales=mnaoLocales.replace("_", "-");
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
<jsp:useBean id="metaDataValidationBean" class="com.mazda.gms3.dmt.metadataval.bean.MetaDataValidationBean" scope="session"></jsp:useBean>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><fmt:message key="label.title" /> - <fmt:message key="label.metadatavalidation"/> </title>
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

.inputTable td{
	padding:10px !important;
}

.ui-dialog
{
	width:900px !important; 
	height:auto !important; 
	overflow-x:hidden !important;
	overflow-y:auto !important;
}

.ui-dialog .ui-dialog-title{color:#fff !important;}

#mdval_pending_dialog-confirm
{
	height:auto !important;
}
#mdval_completed_dialog-confirm
{
	height: auto !important;
}
.deleteScheduleIcon:before {
    content: "\f1f8";
    color: #fff;
    font-size: 15px;
    font-family: "FontAwesome";
    margin-right: 10px;
    font-weight: normal;
    font-style: normal;
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
					<li><a href="<%=request.getContextPath() %>/schedule"><fmt:message key="label.schedule"/></a></li>
					<li><a href="<%=request.getContextPath() %>/history"><fmt:message key="label.history"/></a></li>
					<li class="activeLink"><a href="<%=request.getContextPath() %>/metadatavalidation"><fmt:message key="label.metadatavalidation"/></a></li>
					<li><a href="<%=request.getContextPath() %>/mdsync"><fmt:message key="label.masterdatasynching" /> </a></li>	
				</ul>
				<div class="cb"></div>
			</div>
            <!-- TOP MENU ENDS -->
            <form action="<%=request.getContextPath() %>/metadatavalidation" id="MDVAL_Form" name="MDVAL_Form" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            	<input type="hidden" name="MDVAL_ActionClicked" id="MDVAL_ActionClicked" value="">
				<input type="hidden" name="MDVAL_displayPageNo" id="MDVAL_displayPageNo" value="<c:out value="${metaDataValidationBean.displayPageNo }"/>" />
				<input type="hidden" name="MDVAL_displayPageLen" id="MDVAL_displayPageLen" value="<c:out value="${metaDataValidationBean.displayPageLength }"/>" />
				<input type="hidden" name="MDVAL_Schedule_displayPageNo" id="MDVAL_Schedule_displayPageNo" value="<c:out value="${metaDataValidationBean.scheduleDisplayPageNo }"/>" />
				<input type="hidden" name="MDVAL_Schedule_displayPageLen" id="MDVAL_Schedule_displayPageLen" value="<c:out value="${metaDataValidationBean.scheduleDisplayPageLength }"/>" />
            	<input type="hidden" name="MDVAL_ItemToBeDeleted" id="MDVAL_ItemToBeDeleted" value="">
            	<input type="hidden" name="MDVAL_Sch_SelectedRows" id="MDVAL_Sch_SelectedRows" value="">
            	<div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        		<table width="100%" cellspacing="1" cellpadding="3">
									<tr>
										<td id="MDVAL_TD_Error_Message" colspan="2">
											<c:if test="${!empty metaDataValidationBean.errorMessage }">
												<div class="errorMessage" id="MDVAL_ERROR_MESSAGE">
													<%
														if(null!=metaDataValidationBean.getErrorMessage() && !"".equals(metaDataValidationBean.getErrorMessage()))
														{
															String[] msgs = metaDataValidationBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty metaDataValidationBean.successMessage }">
												<div class="successMessage" id="MDVAL_ERROR_MESSAGE">
													<c:out value="${metaDataValidationBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									
									<tr>
										<td colspan="2">&nbsp;</td>
									</tr>
									<tr>
										<td width="50%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.country"/>: <span class="mandatory">*</span></label> 
												<select id="MDVAL_CountryLocale_Code" name="MDVAL_CountryLocale_Code" onchange="mdval_countrySelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty metaDataValidationBean.countryList }">
														<c:forEach var="countryList" items="${metaDataValidationBean.countryList }">
															<c:set var="selectedFlagCountry" value="" />
															<c:if test="${!empty metaDataValidationBean.countryId}">
																<c:if test="${countryList.value eq metaDataValidationBean.countryId}">
																	<c:set var="selectedFlagCountry" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagCountry eq 1}">
																	<option selected="selected" value="<c:out value="${countryList.value }"/>"><c:out value="${countryList.label }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${countryList.value }"/>"><c:out value="${countryList.label }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
										<td width="50%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.language"/>: <span class="mandatory">*</span></label> 
												<select id="MDVAL_Lang_Code" name="MDVAL_Lang_Code" onchange="mdval_languageSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty metaDataValidationBean.languageList }">
														<c:forEach var="languageList" items="${metaDataValidationBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty metaDataValidationBean.languageId}">
																<c:if test="${languageList.value eq metaDataValidationBean.languageId}">
																	<c:set var="selectedFlagLang" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
																	<option selected="selected" value="<c:out value="${languageList.value }"/>"><c:out value="${languageList.label }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${languageList.value }"/>"><c:out value="${languageList.label }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								
								<br />
								<h4 style="font-size:16px;">
									<fmt:message key="label.add.metadatafiles" />
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td width="25%">
											<label><fmt:message key="label.metadata"/> <fmt:message key="label.type"/>: <span class="mandatory">*</span></label>
										</td>
										<td width="75%" style="text-align: left;">
											<select id="MDVAL_MetaDataType" name="MDVAL_MetaDataType" style="width: 360px; height: 30px;" onchange="mdval_helpDisplay(this);">
												<option value=""><fmt:message key="label.selectOne"/></option>
												<c:if test="${!empty metaDataValidationBean.metaDataTypeList }">
													<c:forEach var="metaDataTypeList" items="${metaDataValidationBean.metaDataTypeList }">
														<c:set var="selectedFlagMetaData" value="" />
														<c:if test="${!empty metaDataValidationBean.metaDataType}">
															<c:if test="${metaDataTypeList.value eq metaDataValidationBean.metaDataType}">
																<c:set var="selectedFlagMetaData" value="1" />
															</c:if>
														</c:if>
														<c:choose>
															<c:when test="${selectedFlagMetaData eq 1}">
																<option selected="selected" value="<c:out value="${metaDataTypeList.value }"/>"><c:out value="${metaDataTypeList.label }"/></option>
															</c:when>
															<c:otherwise>
																<option value="<c:out value="${metaDataTypeList.value }"/>"><c:out value="${metaDataTypeList.label }"/></option>
															</c:otherwise>
														</c:choose>
													</c:forEach>
												</c:if>
											</select>
											<b id="MDVAL_HelpDisplay" style="font-weight: normal;padding-left: 20px;font-size: 12px;color: #D0021B;"></b>
										</td>
									</tr>
									<tr>
										<td><label><fmt:message key="label.upload" /> <fmt:message key="label.metadata"/> <fmt:message key="label.file"/> <span class="mandatory">*</span></label></td>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="MDVAL_File" name="MDVAL_File" type="file" class="upload" multiple="multiple" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="mdval_file_upload();" id="MDVAL_Upload" name="MDVAL_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.upload"/></button>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${!empty metaDataValidationBean.itemsList }">
	                        		<tr>
										<td colspan="2">
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th scope="col" width="10%">#.</th>
														<th scope="col" width="20%"><fmt:message key="label.locale"/></th>
														<th scope="col" width="20%"><fmt:message key="label.metadata"/> <fmt:message key="label.type"/></th>
														<th scope="col" width="40%"><fmt:message key="label.file"/></th>
														<th scope="col" width="10%"><fmt:message key="label.delete"/></th>
													</tr>
												</thead>
												<tbody>
													<c:if test="${!empty metaDataValidationBean.itemsList }">
														<c:forEach var="itemsList" items="${metaDataValidationBean.itemsList }">
															<tr>
																<td><c:out value="${itemsList.srNo }"/></td>
																<td><c:out value="${itemsList.locale }"></c:out></td>
																<td><c:out value="${itemsList.metaDataTypeLabel }"></c:out></td>
																<td>
																	<c:if test="${!empty itemsList.webFilePath }">
																		<a href="javascript:void(0);" onclick="mdval_showReports('<c:out value="${itemsList.webFilePath }"/>');"><c:out value="${itemsList.fileName }"/></a>
																	</c:if>
																	<c:if test="${empty itemsList.webFilePath }">
																		<c:out value="${itemsList.fileName }"/>
																	</c:if>
																</td>
																<td>
																	<a href="javascript:void(0);" onclick="mdval_deleteItem('<c:out value="${itemsList.srNo }"/>');"><i class="deleteIcon"></i></a>
																</td>
															</tr>
														</c:forEach>
													</c:if>
												</tbody>
											</table>
										</td>
									</tr>
									</c:if>
									<tr>
										<td colspan="2" align="right" style="text-align:right;padding-top:0px !important;">
											<input type="button" id="MDVAL_Reset" class="gear_button cursorPointer"  onclick="mdval_reset();" value="<fmt:message key="label.reset" />"/>
											<input type="button" id="MDVAL_Schedule" class="bluebutton cursorPointer"  onclick="mdval_conversion();" value="<fmt:message key="label.validate" />"/>
										</td>
									</tr>
								</table>
								<br/>
                       			<p style="height: 5px;"></p>
                       			<h4 style="font-size:16px;"><fmt:message key="label.validation"/> <fmt:message key="label.schedule"/> </h4><br />
                       			<p class="warningMessage">
                       				<fmt:message key="label.show.viewall.validation.schedule.note" />
                       			</p>
                        		<table id="example1" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDVAL_HeaderCheckBox" name="MDVAL_HeaderCheckBox" onclick="mdval_sch_selectallrows(this);"/>
											</th>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="10%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="10%"><fmt:message key="label.scheduledby"/></th>
											<th scope="col" width="10%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="7%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="10%"><fmt:message key="label.metadata"/> <fmt:message key="label.type"/></th>
											<th scope="col" width="11%"><fmt:message key="label.lines"/> (<fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/>)</th>
											<th scope="col" width="12%"><fmt:message key="label.file"/></th>
											<th scope="col" width="7%"><fmt:message key="label.status"/></th>
											<th scope="col" width="7%"><fmt:message key="label.reports"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty metaDataValidationBean.scheduleList }">
											<c:forEach var="scheduleList" items="${metaDataValidationBean.scheduleList }">
												<tr <c:if test="${scheduleList.rowSelected eq true  }">class="selected"</c:if>>
													<td>
														<c:if test="${!empty scheduleList.status && (scheduleList.status!='Pending' && scheduleList.status!='Processing'  )}">
														<input type="checkbox" name="MDVAL_Selection" onchange="mdval_sch_checkBoxManagement();" 
															value="<c:out value="${scheduleList.scheduleId }" />" 
															id="MDVAL_Selection_<c:out value="${scheduleList.scheduleId }"  />"
															<c:if test="${scheduleList.rowSelected eq true  }">checked="checked"</c:if> />
														</c:if>	
													</td>
													<td><c:out value="${scheduleList.srNo }"/></td>
													<td><c:out value="${scheduleList.scheduleName }"></c:out></td>
													<td><c:out value="${scheduleList.wslId }"></c:out></td>
													<td><fmt:formatDate value="${scheduleList.startTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${scheduleList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${scheduleList.locale }"></c:out></td>
													<td><c:out value="${scheduleList.metaDataTypeLabel }"></c:out></td>
													<td><c:out value="${scheduleList.totalCount }"/> / <c:out value="${scheduleList.successCount }"/> / <c:out value="${scheduleList.failureCount }"/></td>
													<td>
														<c:if test="${!empty scheduleList.webFilePath }">
															<a href="javascript:void(0);" onclick="mdval_showReports('<c:out value="${scheduleList.webFilePath }"/>');"><c:out value="${scheduleList.fileName }"/></a>
														</c:if>
														<c:if test="${empty scheduleList.webFilePath }">
															<c:out value="${scheduleList.fileName }"/>
														</c:if>
													</td>
													<td>
														<c:if test="${scheduleList.showAbort eq true}">
															<a href="javascript:void(0);" onclick="mdval_abortConversion('<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.threadId }"/>');"><c:out value="${scheduleList.status }"/></a>		
														</c:if>
														<c:if test="${scheduleList.showAbort eq false}">
															<c:out value="${scheduleList.status }"/>
														</c:if>
													</td>
													<td>
														<c:if test="${scheduleList.status != 'Success'}">
															<a href="javascript:void(0);" onclick="mdval_viewReports('<c:out value="${scheduleList.status }"/>','<c:out value="${scheduleList.scheduleId }"/>');" style="text-decoration:none;"><i class="downloadReports"></i></a>
															<p style="display: none;" id="MDVAL_Remarks_<c:out value="${scheduleList.scheduleId }"/>"><c:out value="${scheduleList.schdeuleRemarks }"></c:out></p>
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
<div id="reset_dialog-confirm" title="<fmt:message key="label.resetform"/>" class="hide">
	<p>
		<fmt:message key="label.reset.metadataval.description"/>
	</p>
</div>
<div id="delete_dialog-confirm" title="<fmt:message key="label.delete"/> <fmt:message key="label.item"/>" class="hide">
	<p>
		<fmt:message key="label.delete.metadataval.description"/>
	</p>
</div>

<div id="delete_schedule_dialog-confirm" title="<fmt:message key="label.delete"/> <fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.delete.metadata.schedule.description"/>
	</p>
</div>

<div id="mdval_pending_dialog-confirm" title="<fmt:message key="label.view" /> <fmt:message key="label.reports" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="MDVAL_STATUS_LABEL"></u>. <fmt:message key="label.metadataval.pending.dialog.info.end" /> </strong>
	</p>
	<br/>
	<br/>
	<!-- 
	<p style="text-align: justify;font-size: 11px; !important">
		<b><u><fmt:message key="label.note" /> :</u></b> <fmt:message key="label.note.info"/>
	</p> -->
</div>


<div id="mdval_completed_dialog-confirm" title="<fmt:message key="label.reports" />" class="hide">

</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true"/>
<!-- FOOTER ENDS -->


<script type="text/javascript">
if(null!=document.getElementById("MDVAL_File"))
{
	document.getElementById("MDVAL_File").onchange = function () {
    	document.getElementById("uploadFile").value = this.value;
	};
}
</script>

<c:if test="${!empty metaDataValidationBean.successMessage }">
<script type="text/javascript">
setTimeout(function(){ 
	$("div.successMessage").fadeOut("slow");
}, 10000);
</script>
</c:if>

<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});

var runReloadScript="<c:out value="${metaDataValidationBean.reloadJSP }" />";

if(runReloadScript=="true")
{
	// reload page after every 2 minutes  
	setTimeout(function()
	{
		mdval_readDataTableValues();
		mdval_readScheduleDataTableValues();
		$("#MDVAL_ActionClicked").val("REFRESH");
	    $("#MDVAL_Form").submit();
	}, 120000);
}
</script>

<script type="text/javascript">
var htmlContent="";	
var htmlScheduleContent="";
</script>

<c:if test="${!empty metaDataValidationBean.itemsList }">
<script type="text/javascript">
htmlContent="<div class=\"separator\"></div>";	
</script>
</c:if>


<c:if test="${!empty metaDataValidationBean.scheduleList}">
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
var deleteLabel="<fmt:message key="label.delete"/>";
// add separator when transactionsList is not empty
htmlScheduleContent="<div class=\"separator\"></div>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"mdval_refresh();\"  name=\"MDVAL_Refresh\" id=\"MDVAL_Refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"mdval_deleteSchedule();\" name=\"MDVAL_Sch_Delete\" id=\"MDVAL_Sch_Delete\" class=\"bluebutton cursorPointer\"><i class=\"deleteScheduleIcon\"></i> "+deleteLabel+"</button>";
refreshLabel = null;
deleteLabel = null;
</script>
</c:if>

<script type="text/javascript">
$(document).ready(function() {
	loadDataTable();
	loadScheduleDataTable();
	mdval_helpDisplayOnLoad();
});

function loadDataTable()
{
	<c:if test="${!empty metaDataValidationBean.itemsList }">
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
                       { aTargets: [ 4 ], bSortable: false }
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
	var pageNo=$("#MDVAL_displayPageNo").val();
	var pageLength=$("#MDVAL_displayPageLen").val();
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
	</c:if>
}

function mdval_readDataTableValues()
{
	<c:if test="${!empty metaDataValidationBean.itemsList }">
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#MDVAL_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#MDVAL_displayPageLen").val(length);	
		}
	}
	</c:if>
}


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
                        { aTargets: [ 7 ], bSortable: true },
                        { aTargets: [ 8 ], bSortable: true },
                        { aTargets: [ 9 ], bSortable: true },
                        { aTargets: [ 10 ], bSortable: true },
                        { aTargets: [ 11 ], bSortable: true }
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
	
	$('#example1_wrapper .top .toolbar').html(htmlScheduleContent);
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
	var pageNo=$("#MDVAL_Schedule_displayPageNo").val();
	var pageLength=$("#MDVAL_Schedule_displayPageLen").val();
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
		mdval_sch_checkBoxManagement();
	});
}


function mdval_readScheduleDataTableValues()
{
	var table = $('#example1').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#MDVAL_Schedule_displayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#MDVAL_Schedule_displayPageLen").val(length);	
		}
	}
}

function mdval_countrySelection()
{
	// show Loader
	$("#loader").show();
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
	$("#MDVAL_ActionClicked").val("COUNTRY_SELECTION");
	$("#MDVAL_Form").submit();
}

function mdval_languageSelection()
{
	// show Loader
	$("#loader").show();
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
	$("#MDVAL_ActionClicked").val("LANGUAGE_SELECTION");
	$("#MDVAL_Form").submit();
}

function mdval_file_upload()
{
	// show Loader
	$("#loader").show();
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
 	$("#MDVAL_ActionClicked").val("FILE_UPLOAD");
	$("#MDVAL_Form").submit();
}

function mdval_deleteItem(srNo)
{
	// open dialog
	$( "#delete_dialog-confirm" ).removeClass('hide').dialog({
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
				  	mdval_readDataTableValues();
					mdval_readScheduleDataTableValues();
					$("#MDVAL_ItemToBeDeleted").val(srNo);
					$("#MDVAL_ActionClicked").val("DELETE_ITEM");
					$("#MDVAL_Form").submit();
		        }  
		  	,
		  	"<fmt:message key="label.no"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function mdval_reset()
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
	    		  mdval_readDataTableValues();
	    		  mdval_readScheduleDataTableValues();
				  $( this ).dialog( "close" );
		          $("#MDVAL_ActionClicked").val("RESET");
		          $("#MDVAL_Form").submit();
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

function mdval_conversion()
{
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
	$("#MDVAL_ActionClicked").val("VALIDATE");
    $("#MDVAL_Form").submit();
}

function mdval_addNewSchedule()
{
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
	$("#MDVAL_ActionClicked").val("ADD_NEW");
    $("#MDVAL_Form").submit();
}

function mdval_refresh()
{
	mdval_readDataTableValues();
	mdval_readScheduleDataTableValues();
	$("#MDVAL_ActionClicked").val("REFRESH");
    $("#MDVAL_Form").submit();
}


function mdval_viewReports(status, scheduleCode)
{
	if(status=="Pending" || status=="Processing")
	{
		// set status Label
		$('#MDVAL_STATUS_LABEL').html(status);
		
		// open dialog
		$( "#mdval_pending_dialog-confirm" ).removeClass('hide').dialog({
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
		//$("#mdval_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#mdval_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
	}
	else
	{
		// call function to check whether the reports exists for the Schedule Code or not
		
		// $("body").mask("Please wait...");
		
		var schReports="";
		var appContext = "<%=applicationContext %>";
		if(null!=appContext && appContext!="" && appContext!="null")
		{
			schReports=appContext;
		}	
		schReports =schReports+"<%=request.getContextPath() %>/schedulereports"; 
		var key = "#MDVAL_Remarks_"+scheduleCode;
		var remarks=$(key).html();
		// Make Ajax Call
		$.ajax({
			url:schReports,
			type:"post",
			data:{
				MDVAL_VIEW_REPORTS:"MDVAL_VIEW_REPORTS",
				MDVAL_SCHEDULE_CODE:scheduleCode,
				MDVAL_SCHEDULE_STATUS:status,
				MDVAL_SCHEDULE_REMARKS:remarks
			},
			dataType:"text",
			success:function(responseData){
				if(null != responseData)
				{
					var index=responseData.indexOf("<SCRIPT");
					if(index!=-1)
					{
						responseData= responseData.substring(0,index);
					}
					try
					{
						// remove extra /n/r and other spaces from the JSON String
						responseData = responseData.replace(/(\r\n|\n|\r)/gm,""); 
						var jsonData = $.parseJSON(responseData);
						var token = jsonData.TOKEN;
						var responseString = jsonData.RESPONSESTRING;
						
						// $("body").unmask();
						if(token=="DATA")
						{
							// set the dialog HTML AND Open it
							$( "#mdval_completed_dialog-confirm" ).html(responseString);	
							
							// open dialog
							$( "#mdval_completed_dialog-confirm" ).removeClass('hide').dialog({
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
							//$("#mdval_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
							$("#mdval_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
						}
						else
						{
							// show error Message
							alert(responseString);
						}	
					}
					catch(err)
					{
						//console.log(err);
						// $("body").unmask();
					}
				}
			},
			error:function(xmlhttp, status, error){
				// $("body").unmask();
				alert("error :: "+status);
			}
		});
	}	
}

function mdval_showReports(url)
{
	var appContext="<%=applicationContext %>";
	var icContext = "<%=icContext %>";
	if(null!=url && url!="")
	{
		if(null!=appContext && appContext!="" && url.indexOf(appContext)!=-1)
		{
			url = url.substring(url.indexOf(appContext)+1, url.length);
		}	
		// append icCOntext - do not append
		//url = icContext+url;
		// append appContext to ensure, it is always there
		url = appContext+url;
		window.open(url, "_blank");
	}	
}


var mnaoLocales="<%=mnaoLocales %>";
function mdval_helpDisplay(thisObj)
{
	$("#MDVAL_HelpDisplay").html("");
	var value=$(thisObj).val();
	var locale = "<c:out value="${metaDataValidationBean.languageCode }"/>";
	if(null!=value && value!="")
	{
		if(value.indexOf("ESICATEGORY")>-1)
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="esicategory.help.message" />");
		}
		else if(value=='VIN')
		{
			if(null!=locale && locale!="" && mnaoLocales.indexOf(locale)>-1)
			{
				$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vin.ent.help.message" />");
			}
			else
			{
				$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vin.text.help.message" />");	
			}	
		}	
		else if(value=='DISPLAYORDER')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="displayorder.help.message" />");
		}
		else if(value=='CDPROCESSING')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="cdprocessing.help.message" />");
		}
		else if(value=='LEFTMENU')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="leftmenu.help.message" />");
		}
		else if(value=='VINATTRIBUTE')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vinattribute.help.message" />");
		}
	}
}



function mdval_helpDisplayOnLoad()
{
	$("#MDVAL_HelpDisplay").html("");
	var value=$("#MDVAL_MetaDataType").val();
	var locale = "<c:out value="${metaDataValidationBean.languageCode }"/>";
	if(null!=value && value!="")
	{
		if(value.indexOf("ESICATEGORY")>-1)
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="esicategory.help.message" />");
		}
		else if(value=='VIN')
		{
			if(null!=locale && locale!="" && mnaoLocales.indexOf(locale)>-1)
			{
				$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vin.ent.help.message" />");
			}
			else
			{
				$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vin.text.help.message" />");	
			}
		}	
		else if(value=='DISPLAYORDER')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="displayorder.help.message" />");
		}
		else if(value=='CDPROCESSING')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="cdprocessing.help.message" />");
		}
		else if(value=='LEFTMENU')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="leftmenu.help.message" />");
		}
		else if(value=='VINATTRIBUTE')
		{
			$("#MDVAL_HelpDisplay").html("<i class=\"infoIcon\"></i>&nbsp;<fmt:message key="vinattribute.help.message" />");
		}
	}
}

function mdval_sch_checkBoxManagement()
{
	$("#MDVAL_Sch_SelectedRows").val("");
	var value=$("#MDVAL_Sch_SelectedRows").val();
	if(value==null || value=="undefined" || value=="null")
	{
		value="";
	}
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
		$("#MDVAL_Sch_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - MDVAL_HeaderCheckBox
	*/
	$("#MDVAL_HeaderCheckBox").prop("checked",false);
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
			$("#MDVAL_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function mdval_sch_selectallrows(thisObj)
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
		$("#MDVAL_Sch_SelectedRows").val("");
	}
	mdval_sch_checkBoxManagement();
}



function mdval_deleteSchedule()
{
	$("#MDVAL_TD_Error_Message").html("");
	var value=$("#MDVAL_Sch_SelectedRows").val();
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
					  	mdval_readDataTableValues();
						mdval_readScheduleDataTableValues();
						$("#MDVAL_ActionClicked").val("DELETE_SCHEDULE");
					    $("#MDVAL_Form").submit();
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
		mdval_showErrorMessage("<fmt:message key="error.select.onerow.delete" />");
	}
}

function mdval_showErrorMessage(errorMessage)
{
	$("#MDVAL_TD_Error_Message").html("");
	var message="<div class=\"errorMessage\" id=\"MDVAL_ERROR_MESSAGE\">";
	message = message+errorMessage;
	message = message+"</div>";
	message = message+"<div class=\"cb\"></div>";
	$("#MDVAL_TD_Error_Message").html(message);
	window.scrollTo(0,0);
	message=null;
}

</script>
</html>