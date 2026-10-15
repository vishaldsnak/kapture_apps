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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL_HISTORY);
%>


<%
	String applicationContext=ApplicationProperties.getProperty("application.environment.context");	
//	String icWebContext = ApplicationProperties.getProperty("application.infocenter.web.context");
	String reportsWebPath = ApplicationProperties.getProperty("dataexttool.reports.web.path");
	String dataExpToolWorkingDirWebPath = ApplicationProperties.getProperty("dataexttool.working.dir.web.path");
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
<jsp:useBean id="nmdScheduleHistorySessionBean" class="com.mazda.gms3.mdm.nmdtool.bean.NMDScheduleHistorySessionBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.NEW_MNAO_DATA_EXPORT_TOOL_HISTORY"/> </title>
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
.ui-dialog{width:900px !important; height: auto !important;top: 100px !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

#abort_dialog-confirm
{
	height: auto !important;
}
#view_attempt_details-dialog
{
	height: auto !important;
}
#view_summary_details-dialog
{
	height: auto !important;
}
#view_transfer_summary_details-dialog
{
	height: auto !important;
}
#view_systemerror-dialog
{
	height: auto !important;
}

#view_reports-dialog
{
	height: auto !important;
}


.summaryIcon:before
{
	content: "\f0f6";
	color:#34495E;
	font-size: 20px;
	font-family: "FontAwesome";
	font-style: normal;
}

.commentsIcon:before
{
	content: "\f086";
	color:#34495E;
	font-size: 20px;
	font-family: "FontAwesome";
	font-style: normal;
}

.downloadReports:before
{
	font-size: 15px !important;
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.NEW_MNAO_DATA_EXPORT_TOOL_HISTORY"/> &rsaquo;</a> 
           	</div>
           	
            <form id="NMDSCHHISTORY_Form" name="NMDSCHHISTORY_Form" action="<%=request.getContextPath() %>/nmdschedulehistory" method="post">
			<input type="hidden" name="NMDSCHHISTORY_displayPageNo" id="NMDSCHHISTORY_displayPageNo" value="<c:out value="${nmdScheduleHistorySessionBean.displayPageNo }"/>" />
			<input type="hidden" name="NMDSCHHISTORY_displayPageLen" id="NMDSCHHISTORY_displayPageLen" value="<c:out value="${nmdScheduleHistorySessionBean.displayPageLength }"/>" /> 
    		<input type="hidden" name="NMDSCHHISTORY_ActionClicked" id="NMDSCHHISTORY_ActionClicked" value="" />        
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%">
									<tr>
										<td  id="MDM_NMDTOOL_Error_Message">
											<c:if test="${!empty nmdScheduleHistorySessionBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=nmdScheduleHistorySessionBean.getErrorMessage() && !"".equals(nmdScheduleHistorySessionBean.getErrorMessage()))
														{
															String[] msgs = nmdScheduleHistorySessionBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty nmdScheduleHistorySessionBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${nmdScheduleHistorySessionBean.successMessage }" />
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
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="fromDateField" name="fromDateField" value="<c:out value="${nmdScheduleHistorySessionBean.fromDate }" />"></td>
														<td style="text-align:right;">To Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="toDateField" name="toDateField" value="<c:out value="${nmdScheduleHistorySessionBean.toDate }" />"></td>
														<td style="text-align: center !important;">
															<button type="button" name="search" id="search" onclick="NMDTOOL_refresh();" 
																class="bluebutton cursorPointer"><i class="searchIcon"></i> Search</button>
														</td>
													</tr>
													</tbody>
												</table>
											</div>
										</td>
									</tr>
								</table>
								
								<h4>
									<fmt:message key="label.executionhistory"/>
								</h4>
								<br />
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="16%"><fmt:message key="label.data.jobname"/></th>
											<!--  <th scope="col" width="4%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="6%"><fmt:message key="label.model"/></th>-->
											<th scope="col" width="6%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="6%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="6%"><fmt:message key="label.documents" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.success"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="6%"><fmt:message key="label.innerlinks" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.success"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="6%"><fmt:message key="label.okassets" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.success"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="6%"><fmt:message key="label.actual.delete" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.success"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="10%"><fmt:message key="label.transfer" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.success"/> / <fmt:message key="label.skipped"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="6%"><fmt:message key="label.job" /><fmt:message key="label.running.progress"/></th>
											<th scope="col" width="6%"><fmt:message key="label.data" /> <fmt:message key="label.status" /></th>
											<th scope="col" width="6%"><fmt:message key="label.failurereason" /></th>
											<th scope="col" width="4%"><fmt:message key="label.attempt" /></th>
											<th scope="col" width="4%"><fmt:message key="label.download" /> <fmt:message key="label.reports" /></th>
											<th scope="col" width="4%"><fmt:message key="label.job" /> <fmt:message key="label.summary" /></th>
											<th scope="col" width="4%"><fmt:message key="label.remarks" /></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty nmdScheduleHistorySessionBean.transationsList }">
											<c:forEach var="transationsList" items="${nmdScheduleHistorySessionBean.transationsList }">
												<tr>
													<td><c:out value="${transationsList.srNo }"/> </td>
													<td style="width:100px !important;word-break:break-all !important;"><c:out value="${transationsList.jobName }"/></td>
													<!-- <td><c:out value="${transationsList.locale }"/></td>
													<td><c:out value="${transationsList.model }"/></td>  -->
													<td><fmt:formatDate value="${transationsList.startTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${transationsList.endTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${transationsList.totalDocs }"/> / <c:out value="${transationsList.successDoc }"/> / <c:out value="${transationsList.failureDocs }"/></td>
													<td><c:out value="${transationsList.totalInnerLinks }"/> / <c:out value="${transationsList.successInnerLinks }"/> / <c:out value="${transationsList.failureInnnerLinks }"/></td>
													<td><c:out value="${transationsList.totalOkAssets }"/> / <c:out value="${transationsList.successOkAssets }"/> / <c:out value="${transationsList.failureOkAssets }"/></td>
													<td><c:out value="${transationsList.totalDelDocs }"/> / <c:out value="${transationsList.successDelDoc }"/> / <c:out value="${transationsList.failureDelDocs }"/></td>
													<td><a href="javascript:void(0);" onclick="nmdschhistory_viewTransferSummary('<c:out value="${transationsList.jobId }"/>','<c:out value="${transationsList.attemptId }"/>');"><c:out value="${transationsList.totalTransferDocs }"/> / <c:out value="${transationsList.successTransferDoc }"/> / <c:out value="${transationsList.skippedTranferDocs }"/> / <c:out value="${transationsList.failureTransferDocs }"/></a></td>
													<td>
														<c:out value="${transationsList.runningStatus }"/>
													</td>
													<td><c:out value="${transationsList.jobStatus }"/></td>
													<td><c:out value="${transationsList.failureReason }"/></td>
													<td style="text-align:center !important;"><a href="javascript:void(0);" onclick="nmdschhistory_viewAttemptDetails('<c:out value="${transationsList.jobId }"/>');"><c:out value="${transationsList.attemptsCount }"/></a></td>
													<td style="text-align:center !important;">
														<c:choose>
															<c:when test="${!empty transationsList.reportsPath }">
																<a href="javascript:void(0);" onclick="nmdschhistory_viewReport('<c:out value="${transationsList.jobId }"/>','<c:out value="${transationsList.attemptId }"/>');" style="text-decoration:none;">
																	<i class="downloadReports"></i>
																</a>
															</c:when>
															<c:otherwise> - </c:otherwise>
														</c:choose>
													</td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.runningStatus && (transationsList.runningStatus eq 'Completed' || transationsList.runningStatus eq 'Aborted') }">
																<a href="javascript:void(0);" style="text-decoration:none;" onclick="nmdschhistory_viewSummary('<c:out value="${transationsList.jobId }"/>','<c:out value="${transationsList.attemptId }"/>');"><i class="summaryIcon"></i></a>
															</c:when>
															<c:otherwise> - </c:otherwise>
														</c:choose>
													</td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.runningStatus && transationsList.failureReason eq 'System Issue' }">
																<a href="javascript:void(0);" style="text-decoration:none;" onclick="nmdschhistory_viewSystemError('<c:out value="${transationsList.jobId }"/>','<c:out value="${transationsList.attemptId }"/>');"><i class="commentsIcon"></i></a>
															</c:when>
															<c:otherwise> - </c:otherwise>
														</c:choose>
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

<div id="view_attempt_details-dialog" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="view_summary_details-dialog" title="<fmt:message key="label.reports"/> <fmt:message key="label.summary"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="view_transfer_summary_details-dialog" title="<fmt:message key="label.transfer"/> <fmt:message key="label.summary"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="view_systemerror-dialog" title="<fmt:message key="label.systemissue"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="view_reports-dialog" title="<fmt:message key="label.reports"/> <fmt:message key="label.details"/>" class="hide">

</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<c:if test="${!empty nmdScheduleHistorySessionBean.transationsList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
var refresh = "<fmt:message key="label.refresh"/>";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"NMDTOOL_refresh();\"  name=\"NMDSCHHISTORY_Refresh\" id=\"NMDSCHHISTORY_Refresh\" class=\"bluebutton cursorPointer\">"+refresh+"</button>";
</script>
</c:if>
<c:if test="${empty nmdScheduleHistorySessionBean.transationsList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>


	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

var runReloadScript="<c:out value="${nmdScheduleHistorySessionBean.runReloadScript }" />";

if(runReloadScript=="true")
{
	// reload page after every 60 seconds
	setTimeout(function()
	{
		window.location.reload(1);
	}, 60000);
}

$(document).ready(function() {
	loadDataTable();
	
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
						{ aTargets: [ 15 ], bSortable: true }
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
	var pageNo=$("#NMDSCHHISTORY_displayPageNo").val();
	var pageLength=$("#NMDSCHHISTORY_displayPageLen").val();
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

function NMDTOOL_refresh()
{
	$("#MDM_NMDTOOL_Error_Message").html("");
	var fromDate=$("#fromDateField").val();
	var toDate = $("#toDateField").val();
	if(null!=fromDate && fromDate!='' && null!=toDate && toDate!='')
	{
		var fDate = new Date(fromDate);
		var tDate = new Date(toDate);
		if(fromDate >= toDate)
		{
			var err='<div class="errorMessage">From Date must be less than To Date.</div><div class="cb"></div>';
			$("#MDM_NMDTOOL_Error_Message").html(err);
		}
		else
		{
			// show Loader
			 $("#loader").show();
			 dataexptoolhistory_readDataTableValues();
			 $("#NMDSCHHISTORY_ActionClicked").val("RESET");
			 $("#NMDSCHHISTORY_Form").submit();	
		}	
	}
	else
	{
		var err='<div class="errorMessage">From Date & To Date are mandatory.</div><div class="cb"></div>';
		$("#MDM_NMDTOOL_Error_Message").html(err);
	}
}

function dataexptoolhistory_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	$("#NMDSCHHISTORY_displayPageNo").val(info.page);
	$("#NMDSCHHISTORY_displayPageLen").val(length);
}

function nmdschhistory_viewAttemptDetails(jobId)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/nmdtoolview"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			NMDSCHHISTORY_VIEW_ATTEMPT_DETAILS:"NMDSCHHISTORY_VIEW_ATTEMPT_DETAILS",
			NMDSCHHISTORY_SCHEDULE_CODE:jobId
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
						$( "#view_attempt_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#view_attempt_details-dialog" ).removeClass('hide').dialog({
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
						//$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
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

function nmdschhistory_viewSummary(jobId, attemptId)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/nmdtoolview"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			NMDSCHHISTORY_VIEW_SUMMARY_DETAILS:"NMDSCHHISTORY_VIEW_SUMMARY_DETAILS",
			NMDSCHHISTORY_SCHEDULE_CODE:jobId,
			NMDSCHHISTORY_ATTEMPT_ID:attemptId
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
						$( "#view_summary_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#view_summary_details-dialog" ).removeClass('hide').dialog({
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
						//$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#view_summary_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
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

function nmdschhistory_viewTransferSummary(jobId, attemptId)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/nmdtoolview"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			NMDSCHHISTORY_VIEW_TRANSFER_SUMMARY_DETAILS:"NMDSCHHISTORY_VIEW_TRANSFER_SUMMARY_DETAILS",
			NMDSCHHISTORY_SCHEDULE_CODE:jobId,
			NMDSCHHISTORY_ATTEMPT_ID:attemptId
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
						$( "#view_transfer_summary_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#view_transfer_summary_details-dialog" ).removeClass('hide').dialog({
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
						//$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#view_transfer_summary_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
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


function nmdschhistory_viewSystemError(jobId, attemptId)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/nmdtoolview"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			NMDSCHHISTORY_VIEW_SYSTEM_ERROR:"NMDSCHHISTORY_VIEW_SYSTEM_ERROR",
			NMDSCHHISTORY_SCHEDULE_CODE:jobId,
			NMDSCHHISTORY_ATTEMPT_ID:attemptId
		},
		dataType:"text",
		success:function(response){
			var responseData = response;
			if(null != responseData)
			{
				try
				{
					// set the dialog HTML AND Open it
					$( "#view_systemerror-dialog" ).html(responseData);	
					// open dialog
					$( "#view_systemerror-dialog" ).removeClass('hide').dialog({
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
					//$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
					$("#view_systemerror-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
				}
				catch(err)
				{
					console.log(err);
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

function nmdschhistory_viewReport(jobId, attemptId)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/nmdtoolview"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			NMDSCHHISTORY_VIEW_REPORTS:"NMDSCHHISTORY_VIEW_REPORTS",
			NMDSCHHISTORY_SCHEDULE_CODE:jobId,
			NMDSCHHISTORY_ATTEMPT_ID:attemptId
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
						$( "#view_reports-dialog" ).html(responseString);	
						
						// open dialog
						$( "#view_reports-dialog" ).removeClass('hide').dialog({
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
						//$("#view_attempt_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#view_reports-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
					}
					else if(token=="PATH")
					{
						var url=responseString;
						window.open(url, '_blank');
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
						dataexptoolhistory_readDataTableValues();
						 $("#AXL_ResetAction").val("Reset");
						 $("#AXL_Form").submit();
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