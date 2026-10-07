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
	String applicationContext=ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
	String icContext=ApplicationProperties.getProperty("GMS3_INFOCENTER_WEB_CONTEXT");

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
<jsp:useBean id="historyBean" class="com.mazda.gms3.dmt.bean.HistoryBean" scope="session"></jsp:useBean>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><fmt:message key="label.title" /> - <fmt:message key="label.history"/> </title>
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
	width:961px !important; 
	height:auto !important; 
	overflow-x:hidden !important;
	overflow-y:auto !important;
}

.ui-dialog .ui-dialog-title{color:#fff !important;}

#dmt_pending_dialog-confirm, #dmt_schat_pending_dialog-confirm
{
	height:auto !important;
}
#dmt_completed_dialog-confirm
{
	height: auto !important;
}

#dmt_view_item_details-dialog
{
	height: auto !important;
}
#dmt_view_report_summary-dialog
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

.inputTable td{
	padding:10px !important;
}

/* same style as the reports (downloadReports) and job summary (summaryIcon) icons */
.previewIcon:before {
	content: "\f06e";
	color: #34495E;
	font-size: 20px;
	font-family: "FontAwesome";
	font-style: normal;
}

/* the execution history table fits the page: fixed layout on the header widths, long values wrap */
#example.historyTable {
	table-layout: fixed;
	width: 100% !important;
}
#example.historyTable th, #example.historyTable td {
	padding: 8px 4px;
	overflow-wrap: anywhere;
	word-break: break-word;
}

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
					<li><a href="<%=request.getContextPath() %>/schedule"><fmt:message key="label.schedule"/></a></li>
					<li class="activeLink"><a href="<%=request.getContextPath() %>/history"><fmt:message key="label.history"/></a></li>
					<li><a href="<%=request.getContextPath() %>/metadatavalidation"><fmt:message key="label.metadatavalidation"/></a></li>	
					<li><a href="<%=request.getContextPath() %>/mdsync"><fmt:message key="label.masterdatasynching" /> </a></li>
				</ul>
				<div class="cb"></div>
			</div>
            <!-- TOP MENU ENDS -->
            <form action="<%=request.getContextPath() %>/history" id="DMT_HIS_Form" name="DMT_HIS_Form" method="post">
           	<input type="hidden" name="DMT_HIS_displayPageNo" id="DMT_HIS_displayPageNo" value="<c:out value="${historyBean.displayPageNo }"/>" />
			<input type="hidden" name="DMT_HIS_displayPageLen" id="DMT_HIS_displayPageLen" value="<c:out value="${historyBean.displayPageLength }"/>" /> 
            <input type="hidden" name="DMT_HIS_ActionClicked" id="DMT_HIS_ActionClicked" value="">
            <input type="hidden" name="DMT_HIS_ScheduleId" id="DMT_HIS_ScheduleId" value="">
            <input type="hidden" name="DMT_HIS_ThreadId" id="DMT_HIS_ThreadId" value="">
            <input type="hidden" name="DMT_HIS_ScheduleType" id="DMT_HIS_ScheduleType" value="">
            <input type="hidden" name="DMT_HIS_Sch_SelectedRows" id="DMT_HIS_Sch_SelectedRows" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
								<table width="100%">
									<tr>
										<td id="DMT_HIS_TD_Error_Message">
											<c:if test="${!empty historyBean.errorMessage }">
												<div class="errorMessage" id="DMT_ERROR_MESSAGE">
													<%
														if(null!=historyBean.getErrorMessage() && !"".equals(historyBean.getErrorMessage()))
														{
															String[] msgs = historyBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty historyBean.successMessage }">
												<div class="successMessage" id="DMT_ERROR_MESSAGE">
													<c:out value="${historyBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td>
											<div style="margin: auto;width: 80%;padding-bottom: 10px;">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tbody>
														<tr>
														<td style="text-align:right;">From Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="fromDateField" name="fromDateField" value="<c:out value="${historyBean.fromDate }" />"></td>
														<td style="text-align:right;">To Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="toDateField" name="toDateField" value="<c:out value="${historyBean.toDate }" />"></td>
														<td style="text-align: center !important;">
															<button type="button" name="search" id="search" onclick="searchSubmit();" 
																class="bluebutton cursorPointer"><i class="searchIcon"></i> Search</button>
														</td>
													</tr>
													</tbody>
												</table>
											</div>
										</td>
									</tr>
									<tr>
										<td>
											<h3><fmt:message key="label.executionhistory" /></h3>
											<br />
											<p class="warningMessage">
												Note: Only records of last 10 days will be shown by default. Inorder to view previous Job executions, please use date filter criteria.
											</p>
										</td>
									</tr>
									<c:if test="${! empty historyBean.scheduleList }">
									<tr>
										<td>
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th scope="col" width="2%">
															<input type="checkbox" id="DMT_HIS_HeaderCheckBox" name="DMT_HIS_HeaderCheckBox" onclick="dmt_sch_selectallrows(this);"/>
														</th>
														<th scope="col" width="2%">#.</th>
														<th scope="col" width="12%"><fmt:message key="label.schedulename" /> </th>
														<th scope="col" width="5%"><fmt:message key="label.scheduledby" /></th>
														<th scope="col" width="6%"><fmt:message key="label.scheduletime" /></th>
														<th scope="col" width="6%"><fmt:message key="label.finishtime" /></th>
														<th scope="col" width="6%"><fmt:message key="label.documents" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="6%"><fmt:message key="label.okassetscount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="6%"><fmt:message key="label.innerlinkscount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="5%"><fmt:message key="label.deletecount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="6%"><fmt:message key="label.displayordercount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="6%"><fmt:message key="label.cdprocessingcount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="6%"><fmt:message key="label.scmvinmappingcount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="8%"><fmt:message key="label.contentstatus" /></th> 
														<th scope="col" width="6%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
														<th scope="col" width="4%"><fmt:message key="label.view" /> <fmt:message key="label.reports" /></th>
														<th scope="col" width="4%"><fmt:message key="label.job" /> <fmt:message key="label.summary" /></th>
														<th scope="col" width="4%"><fmt:message key="label.preview" /></th>
													</tr>
												</thead>
												<tbody>
													<c:if test="${! empty historyBean.scheduleList }">
														<c:forEach var="scheduleList" items="${historyBean.scheduleList }">
															<tr <c:if test="${scheduleList.rowSelected eq true  }">class="selected"</c:if>>
																<td>
																	<c:if test="${!empty scheduleList.scheduleStatus && (scheduleList.scheduleStatus!='1' && scheduleList.scheduleStatus!='2') }">
																	<input type="checkbox" name="DMT_HIS_Selection" onchange="dmt_sch_checkBoxManagement();" 
																		value="<c:out value="${scheduleList.scheduleId }" />" 
																		id="DMT_HIS_Selection_<c:out value="${scheduleList.scheduleId }"  />"
																		<c:if test="${scheduleList.rowSelected eq true  }">checked="checked"</c:if> />
																	</c:if>	
																</td>
																<td><c:out value="${scheduleList.srNo }"/> </td>
																<td><a href="javascript:void(0);" onclick="dmt_viewItemDetails('<c:out value="${scheduleList.scheduleId }"/>');"><c:out value="${scheduleList.scheduleName }"/></a></td>
																<td><c:out value="${scheduleList.userId }"/></td>
																<td><fmt:formatDate value="${scheduleList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><fmt:formatDate value="${scheduleList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><c:out value="${scheduleList.totalDocsForProcessing }"/> / <c:out value="${scheduleList.processedDocsCount }"/> / <c:out value="${scheduleList.failedProcessedDocsCount }"/></td>
																<td><c:out value="${scheduleList.okAssetsCount }"/> / <c:out value="${scheduleList.processedOkAssetsCount }"/> / <c:out value="${scheduleList.failedOkAssetsCount }"/></td>
																<td>
																	<c:choose>
																		<c:when test="${!empty scheduleList.innerLinksProcessingStatus }">
																			<c:out value="${scheduleList.innerLinksProcessingStatus }" />
																		</c:when>
																		<c:otherwise>
																			<c:out value="${scheduleList.totalInnerLinksCount }"/> / <c:out value="${scheduleList.processedInnerLinksCount }"/> / <c:out value="${scheduleList.failedInnerLinksCount }"/>
																		</c:otherwise>
																	</c:choose>
																</td>
																<td><c:out value="${scheduleList.totalDocsForDeletion }"/> / <c:out value="${scheduleList.deletedDocsCount }"/> / <c:out value="${scheduleList.failedDeletedDocsCount }"/></td>
																<td><c:out value="${scheduleList.totalDisplayOrderCount }"/> / <c:out value="${scheduleList.processedDisplayOrderCount }"/> / <c:out value="${scheduleList.failedDisplayOrderCount }"/></td>
																<td><c:out value="${scheduleList.totalCDProcessingCount }"/> / <c:out value="${scheduleList.processedCDProcessingCount }"/> / <c:out value="${scheduleList.failedCDProcessingCount }"/></td>
																<td><c:out value="${scheduleList.totalSCMVinCount }"/> / <c:out value="${scheduleList.processedSCMVinCount }"/> / <c:out value="${scheduleList.failedSCMVinCount }"/></td>
																<td><c:out value="${scheduleList.currentProcessingStatus }"/> </td>
																<td>
																	<c:if test="${scheduleList.showAbort eq true}">
																		<a href="javascript:void(0);" onclick="dmt_his_abortConversion('<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.threadId }"/>','<c:out value="${scheduleList.scheduleType }"/>');"><c:out value="${scheduleList.scheduleStatusLabel }"/></a>		
																	</c:if>
																	<c:if test="${scheduleList.showAbort eq false}">
																		<c:out value="${scheduleList.scheduleStatusLabel }"/>
																	</c:if>
																</td>
																<td>
																	<a href="javascript:void(0);" onclick="dmt_viewReports('<c:out value="${scheduleList.scheduleStatus }"/>','<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.scheduleStatusLabel }"/>','<c:out value="${scheduleList.scheduleType }"/>');" style="text-decoration:none;"><i class="downloadReports"></i></a>
																</td>
																<td>
																	<c:if test="${!empty scheduleList.scheduleType && scheduleList.scheduleType eq 'DATALOAD' }">
																		<a href="javascript:void(0);" onclick="dmt_viewReportsSummary('<c:out value="${scheduleList.scheduleId }"/>');" style="text-decoration:none;"><i class="summaryIcon"></i></a>
																	</c:if>
																</td>
																<td>
																	<c:if test="${scheduleList.showPreview eq true}">
																		<a href="<c:url value="/preview"><c:param name="schedule" value="${scheduleList.scheduleId }"/><c:param name="name" value="${scheduleList.scheduleName }"/></c:url>"
																			target="_blank" rel="noopener" title="<fmt:message key="label.preview" />" style="text-decoration:none;"><i class="previewIcon"></i></a>
																	</c:if>
																</td>
															</tr>	
														</c:forEach>
													</c:if>
													<c:if test="${empty historyBean.scheduleList }">
														<tr>
															<td colspan="14"><fmt:message key="label.norecordsfound" /> </td>
														</tr>
													</c:if>
												</tbody>
											</table>
										</td>
									</tr>	
									</c:if>
									<c:if test="${empty historyBean.scheduleList }">
									<tr>
										<td>
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<tr>
													<td><fmt:message key="label.norecordsfound" /> </td>
												</tr>
											</table>	
										</td>
									</tr>
									</c:if>
								</table>
							</div>
                        </div>
                    </div>
                </div>
            </div>
     </form>   
</div>
<div id="abort_dialog-confirm" title="<fmt:message key="label.abortconversion"/>" class="hide">
	<p>
		<fmt:message key="label.abortdescription"/>
	</p>
</div>

<div id="abort_schat_dialog-confirm" title="<fmt:message key="label.abort.ipm.facelift.vin.update"/>" class="hide">
	<p>
		<fmt:message key="label.abort.ipm.facelift.vin.update.description"/>
	</p>
</div>

<div id="dmt_pending_dialog-confirm" title="<fmt:message key="label.view" /> <fmt:message key="label.reports" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="DMT_STATUS_LABEL"></u>. <fmt:message key="label.pending.dialog.info.end" /> </strong>
	</p>
	<br/>
	<br/>
	
	<p style="text-align: justify;font-size: 11px; !important">
		<b><u><fmt:message key="label.note" /> :</u></b> <fmt:message key="label.note.info"/>
	</p>
</div>

<div id="dmt_schat_pending_dialog-confirm" title="<fmt:message key="label.view" /> <fmt:message key="label.reports" />" class="hide">
	<p>
		<strong><fmt:message key="label.ipm.facelift.pending.dialog.info.start" /> <u style="color:#D0021B" id="DMT_SCHAT_STATUS_LABEL"></u>. <fmt:message key="label.ipm.facelift.pending.dialog.info.end" /> </strong>
	</p>
	<br/>
	<br/>
</div>

<div id="delete_schedule_dialog-confirm" title="<fmt:message key="label.delete"/> <fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.delete.contentload.schedule.description"/>
	</p>
</div>



<div id="dmt_completed_dialog-confirm" title="<fmt:message key="label.reports" />" class="hide">

</div>


<div id="dmt_view_item_details-dialog" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="dmt_view_report_summary-dialog" title="<fmt:message key="label.reports"/> <fmt:message key="label.summary"/>" class="hide">

</div>


</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true"/>
<!-- FOOTER ENDS -->

<script type="text/javascript">
var htmlContent="";
var calldtfunc = false;
</script>


<c:if test="${!empty historyBean.scheduleList}">
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
var deleteLabel="<fmt:message key="label.delete"/>";
// add separator when transactionsList is not empty
htmlContent="<div class=\"separator\" style=\"padding-right:20px;\"></div>&nbsp;";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"searchSubmit();\"  name=\"refresh\" id=\"refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"dmt_sch_deleteSchedule();\" name=\"DMT_Sch_Delete\" id=\"DMT_Sch_Delete\" class=\"bluebutton cursorPointer\"><i class=\"deleteScheduleIcon\"></i> "+deleteLabel+"</button>";
deleteLabel = null;
</script>
</c:if>


<script type="text/javascript">

function searchSubmit()
{
	$("#DMT_HIS_TD_Error_Message").html("");
	var fromDate=$("#fromDateField").val();
	var toDate = $("#toDateField").val();
	if(null!=fromDate && fromDate!='' && null!=toDate && toDate!='')
	{
		var fDate = new Date(fromDate);
		var tDate = new Date(toDate);
		if(fromDate >= toDate)
		{
			var err='<div class="errorMessage">From Date must be less than To Date.</div><div class="cb"></div>';
			$("#DMT_HIS_TD_Error_Message").html(err);
		}
		else
		{
			// show Loader
			 $("#loader").show();
			 dmt_his_readDataTableValues();
			 $("#DMT_HIS_ActionClicked").val("RESET");
			 $("#DMT_HIS_Form").submit();	
		}	
	}
	else
	{
		var err='<div class="errorMessage">From Date & To Date are mandatory.</div><div class="cb"></div>';
		$("#DMT_HIS_TD_Error_Message").html(err);
	}	
}

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

var runReloadScript="<c:out value="${historyBean.runReloadScript }" />";

if(runReloadScript=="true")
{
	// reload page after every 30 seconds
	setTimeout(function()
	{
		//window.location.reload(1);
		searchSubmit();
	}, 120000);
}


//var htmlContent="<div class=\"separator\"></div>";
$(document).ready(function() {
	<c:if test="${!empty historyBean.scheduleList }">
		loadDataTable();
		calldtfunc=true;
	</c:if>
	<c:if test="${historyBean.showDetailsPopUp eq true}">
		dmt_his_OpenViewDetailsPopup();
	</c:if>
		// date pickers initialization
		$( "#fromDateField" ).datepicker({
			dateFormat: 'yy-mm-dd',
			changeYear:true,
			changeMonth:true,
			showButtonPanel: true
		});
		$( "#toDateField" ).datepicker({
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
		// keep the header widths (they add up to 100%) - no horizontal scroll
		"autoWidth": false,
		"pagingType": "input",
		/*"pageLength":5,*/
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
                        { aTargets: [ 14 ], bSortable: true },
                        { aTargets: [ 15 ], bSortable: true },
                        { aTargets: [ 16 ], bSortable: false }

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
	var pageNo=$("#DMT_HIS_displayPageNo").val();
	var pageLength=$("#DMT_HIS_displayPageLen").val();
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

function dmt_his_readDataTableValues()
{
	if(calldtfunc==true)
	{
		var table = $('#example').DataTable();
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		$("#DMT_HIS_displayPageNo").val(info.page);
		$("#DMT_HIS_displayPageLen").val(length);
	}
}


function dmt_his_abortConversionSendReq(scheduleId, threadId, scheduleType)
{
	dmt_his_readDataTableValues();
	$("#DMT_HIS_ActionClicked").val("ABORT_CONVERSION");
	$("#DMT_HIS_ScheduleId").val(scheduleId);
	$("#DMT_HIS_ThreadId").val(threadId);
	$("#DMT_HIS_ScheduleType").val(scheduleType);
	$("#DMT_HIS_Form").submit();
}
	
function dmt_his_closeShowDetails()
{
	dmt_his_readDataTableValues();
	$("#DMT_HIS_ActionClicked").val("CLOSE_ITEM_DETAILS");
	$("#DMT_HIS_Form").submit();
}

function dmt_his_abortConversion(scheduleId, threadId, scheduleType)
{
	if(scheduleType =='DATALOAD')
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
			          dmt_his_abortConversionSendReq(scheduleId, threadId, scheduleType);
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
	else
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
			          dmt_his_abortConversionSendReq(scheduleId, threadId, scheduleType);
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
			
}


function dmt_his_OpenViewDetailsPopup()
{
	// open dialog
	$( "#schDetails_dialog-confirm" ).removeClass('hide').dialog({
		  draggable:false,
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      modal: true,
		  height:500,
	      buttons: {
	    	"<fmt:message key="label.close" />": function() {
	    		$( this ).dialog( "close" );
				dmt_his_closeShowDetails();
	    	}
	      }
	    });
		
		//$("#schDetails_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#schDetails_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
}


function dmt_viewItemDetails(scheduleCode)
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
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			DMT_HISTORY_VIEW_ITEM_DETAILS:"DMT_HISTORY_VIEW_ITEM_DETAILS",
			DMT_SCHEDULE_CODE:scheduleCode
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
						$( "#dmt_view_item_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#dmt_view_item_details-dialog" ).removeClass('hide').dialog({
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
						//$("#dmt_view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#dmt_view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
					}
					else if(token=="NAVIGATE")
					{
						// submit the form
						document.forms[0].submit();
					}	
					else if(token=="NAVIGATE_TO_ERROR")
					{
						// redirect to Error
						var url="";
						if(null!=appContext && appContext!="" && appContext!="null")
						{
							url=appContext;
						}
						url =url+"<%=request.getContextPath() %>/error";
						window.location.href=url;
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


function dmt_viewReports(status, scheduleCode, statusLabel, scheduleType)
{
	if(status=="1" || status=="2")
	{
		if(scheduleType == 'DATALOAD')
		{
			// set status Label
			$('#DMT_STATUS_LABEL').html(statusLabel);
			
			// open dialog
			$( "#dmt_pending_dialog-confirm" ).removeClass('hide').dialog({
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
			//$("#dmt_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
			$("#dmt_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
		}
		else
		{
			// set status Label
			$('#DMT_SCHAT_STATUS_LABEL').html(statusLabel);
			
			// open dialog
			$( "#dmt_schat_pending_dialog-confirm" ).removeClass('hide').dialog({
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
			//$("#dmt_schat_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
			$("#dmt_schat_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
		}	
		
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
		
		// Make Ajax Call
		$.ajax({
			url:schReports,
			type:"post",
			data:{
				DMT_HISTORY_VIEW_REPORTS:"DMT_HISTORY_VIEW_REPORTS",
				DMT_SCHEDULE_CODE:scheduleCode,
				DMT_SCHEDULE_STATUS:status,
				DMT_SCHEDULE_TYPE:scheduleType
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
							$( "#dmt_completed_dialog-confirm" ).html(responseString);	
							
							// open dialog
							$( "#dmt_completed_dialog-confirm" ).removeClass('hide').dialog({
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
							//$("#dmt_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
							$("#dmt_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
						}
						else if(token=="NAVIGATE")
						{
							// submit the form
							document.forms[0].submit();
						}	
						else if(token=="NAVIGATE_TO_ERROR")
						{
							// redirect to Error
							var url="";
							if(null!=appContext && appContext!=""  && appContext!="null")
							{
								url = appContext;
							}
							url =url+"<%=request.getContextPath() %>/error";
							window.location.href=url;
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



function dmt_viewReportsSummary(scheduleCode)
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
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			DMT_HISTORY_VIEW_REPORTS_SUMMARY:"DMT_HISTORY_VIEW_REPORTS_SUMMARY",
			DMT_SCHEDULE_CODE:scheduleCode
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
						$( "#dmt_view_report_summary-dialog" ).html(responseString);	
						
						// open dialog
						$( "#dmt_view_report_summary-dialog" ).removeClass('hide').dialog({
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
						//$("#dmt_view_report_summary-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#dmt_view_report_summary-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
					}
					else if(token=="NAVIGATE")
					{
						// submit the form
						document.forms[0].submit();
					}	
					else if(token=="NAVIGATE_TO_ERROR")
					{
						// redirect to Error
						var url="";
						if(null!=appContext && appContext!="" && appContext!="null")
						{
							url=appContext;
						}
						url =url+"<%=request.getContextPath() %>/error";
						window.location.href=url;
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

function dmt_sch_openReports(url)
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


function dmt_sch_checkBoxManagement()
{
	$("#DMT_HIS_Sch_SelectedRows").val("");
	var value=$("#DMT_HIS_Sch_SelectedRows").val();
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
		$("#DMT_HIS_Sch_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - DMT_HIS_HeaderCheckBox
	*/
	$("#DMT_HIS_HeaderCheckBox").prop("checked",false);
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
			$("#DMT_HIS_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function dmt_sch_selectallrows(thisObj)
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
		$("#DMT_HIS_Sch_SelectedRows").val("");
	}
	dmt_sch_checkBoxManagement();
}

function dmt_sch_deleteSchedule()
{
	$("#DMT_HIS_TD_Error_Message").html("");
	var value=$("#DMT_HIS_Sch_SelectedRows").val();
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
					 	// DO NOT READ DATA TABLE VALUES AS TABLE NEEDS TO BE REFRESHED
						$("#DMT_HIS_ActionClicked").val("DELETE_SCHEDULE");
						$("#DMT_HIS_Form").submit();
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
		dmt_showErrorMessage("<fmt:message key="error.select.onerow.delete" />");
	}
}

function dmt_showErrorMessage(errorMessage)
{
	$("#DMT_HIS_TD_Error_Message").html("");
	var message="<div class=\"errorMessage\" id=\"DMT_ERROR_MESSAGE\">";
	message = message+errorMessage;
	message = message+"</div>";
	message = message+"<div class=\"cb\"></div>";
	$("#DMT_HIS_TD_Error_Message").html(message);
	window.scrollTo(0,0);
	message=null;
}

</script>


<script type="text/javascript">
var appCon="<%=applicationContext %>";
var serverUrl="";
if(null!=appCon && appCon!=""  && appCon!="null") 
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
					// $("body").unmask();
					if(token=="SUCCESS")
					{
						 dmt_his_readDataTableValues();
						 $("#DMT_HIS_ActionClicked").val("RESET");
						 $("#DMT_HIS_Form").submit();
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
			// $("body").unmask();
			alert("error :: "+status);
		}
	});
}
</script>

</html>