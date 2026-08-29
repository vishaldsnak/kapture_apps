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
	//request.setAttribute("PAGE_NAME", "HISTORY");
	request.setAttribute("PAGE_NAME",AccessManagementInterface.REF_KEY_CD_CREATION_HISTORY);
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
<jsp:useBean id="historyBean" class="com.mazda.gms3.cdrom.bean.CDRomHistoryBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.history"/> </title>
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
.ui-dialog{width:auto; height: auto !important; margin-left: 150px;margin-right: 150px;}
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.history"/> &rsaquo;</a> 
            </div>
	<form action="<%=request.getContextPath() %>/history" id="MDM_HIS_Form" name="MDM_HIS_Form" method="post">
           <input type="hidden" name="MDM_HIS_displayPageNo" id="MDM_HIS_displayPageNo" value="<c:out value="${historyBean.displayPageNo }"/>" />
			<input type="hidden" name="MDM_HIS_displayPageLen" id="MDM_HIS_displayPageLen" value="<c:out value="${historyBean.displayPageLength }"/>" /> 
            <input type="hidden" name="MDM_HIS_ActionClicked" id="MDM_HIS_ActionClicked" value="">
            <input type="hidden" name="MDM_HIS_ScheduleId" id="MDM_HIS_ScheduleId" value="">
            <input type="hidden" name="MDM_HIS_ThreadId" id="MDM_HIS_ThreadId" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
								<table width="100%">
									<tr>
										<td id="MDM_HIS_TD_Error_Message">
											<c:if test="${!empty historyBean.errorMessage }">
												<div class="errorMessage" id="MDM_ERROR_MESSAGE">
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
												<div class="successMessage" id="MDM_ERROR_MESSAGE">
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
										</td>
									</tr>
									<c:if test="${! empty historyBean.scheduleList }">
									<tr>
										<td>
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th scope="col" width="3%">#.</th>
														<th scope="col" width="8%"><fmt:message key="label.schedulename" /> </th>
														<th scope="col" width="7%"><fmt:message key="label.scheduledby" /></th>
														<th scope="col" width="7%"><fmt:message key="label.scheduletime" /></th>
														<th scope="col" width="7%"><fmt:message key="label.finishtime" /></th>
														<th scope="col" width="9%"><fmt:message key="label.documents" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="9%"><fmt:message key="label.okassetscount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="9%"><fmt:message key="label.innerlinkscount" /> - (<fmt:message key="label.total" /> / <fmt:message key="label.processed" /> / <fmt:message key="label.failed" />)</th>
														<th scope="col" width="8%"><fmt:message key="label.contentstatus" /></th> 
														<th scope="col" width="6%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
														<th scope="col" width="5%"><fmt:message key="label.download" /> <fmt:message key="label.content" /></th>
														<th scope="col" width="5%"><fmt:message key="label.download" /> <fmt:message key="label.reports" /></th>
														<th scope="col" width="5%"><fmt:message key="label.viewcdrom" /></th>
													</tr>
												</thead>
												<tbody>
													<c:if test="${! empty historyBean.scheduleList }">
														<c:forEach var="scheduleList" items="${historyBean.scheduleList }">
															<tr>
																<td><c:out value="${scheduleList.srNo }"/> </td>
																<td><a href="javascript:void(0);" onclick="dmt_viewItemDetails('<c:out value="${scheduleList.scheduleId }"/>');"><c:out value="${scheduleList.scheduleName }"/></a></td>
																<td><c:out value="${scheduleList.userId }"/></td>
																<td><fmt:formatDate value="${scheduleList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><fmt:formatDate value="${scheduleList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><c:out value="${scheduleList.totalDocsForProcessing }"/> / <c:out value="${scheduleList.processedDocsCount }"/> / <c:out value="${scheduleList.failedProcessedDocsCount }"/></td>
																<c:choose>
																		<c:when test="${scheduleList.okAssetsCount eq 0 && scheduleList.currentProcessingStatus ne 'COMPLETED' && scheduleList.currentProcessingStatus ne 'CD Zip File Processing' && scheduleList.currentProcessingStatus ne 'Reports Processing'}">
																			<td><fmt:message key="label.counting" /></td>
																		</c:when>
																		<c:otherwise>
																			<td><c:out value="${scheduleList.okAssetsCount }"/> / <c:out value="${scheduleList.processedOkAssetsCount }"/> / <c:out value="${scheduleList.failedOkAssetsCount }"/></td>
																		</c:otherwise>
																</c:choose>
																<td>
																	<c:choose>
																		<c:when test="${scheduleList.okAssetsCount eq 0 && scheduleList.currentProcessingStatus ne 'COMPLETED' && scheduleList.currentProcessingStatus ne 'CD Zip File Processing' && scheduleList.currentProcessingStatus ne 'Reports Processing'}">
																			<fmt:message key="label.counting" />
																		</c:when>
																		<c:otherwise>
																			<c:out value="${scheduleList.totalInnerLinksCount }"/> / <c:out value="${scheduleList.processedInnerLinksCount }"/> / <c:out value="${scheduleList.failedInnerLinksCount }"/>
																		</c:otherwise>
																	</c:choose>
																</td>
																<td><c:out value="${scheduleList.currentProcessingStatus }"/> </td>
																<td>
																	<c:if test="${scheduleList.showAbort eq true}">
																		<a href="javascript:void(0);" onclick="dmt_his_abortConversion('<c:out value="${scheduleList.scheduleId }"/>','<c:out value="${scheduleList.threadId }"/>');"><c:out value="${scheduleList.scheduleStatusLabel }"/></a>		
																	</c:if>
																	<c:if test="${scheduleList.showAbort eq false}">
																		<c:out value="${scheduleList.scheduleStatusLabel }"/>
																	</c:if>
																</td>
																<td>
																<c:if test="${scheduleList.currentProcessingStatus eq 'COMPLETED' && not empty scheduleList.zipFileName}">
																	<a href="javascript:void(0);" onclick="dmt_viewReports1('<c:out value="${scheduleList.scheduleId }"/>','downloadContent');" style="text-decoration:none;"><i class="downloadReports"></i></a>
																</c:if>	
																</td>
																<td>
																<c:if test="${scheduleList.currentProcessingStatus eq 'COMPLETED' && not empty scheduleList.zipFileName}">
																	<a href="javascript:void(0);" onclick="dmt_viewReports1('<c:out value="${scheduleList.scheduleId }"/>','downloadReport');" style="text-decoration:none;"><i class="downloadReports"></i></a>
																</c:if>	
																</td>
																<td> 
																	<c:if test="${scheduleList.showViewLink eq true }">
																		<c:if test="${!empty scheduleList.viewLinkPath }">
																			<a href="<c:out value="${scheduleList.viewLinkPath }"/>" target="_blank"><i class="viewCDROMIcon"></i></a>
																		</c:if>
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

<c:if test="${historyBean.showDetailsPopUp eq true }">
<div id="schDetails_dialog-confirm" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">
	<table width="100%" cellspacing="0" cellpadding="0" >
		<tr>
			<td>
				<table class="historyTable" cellspacing="0" width="100%" style="border-left:1px solid #DADADA;">
					<thead>
						<tr>
							<th scope="col" width="5%">#.</th>
							<th scope="col" width="11%" class="codeClass"><fmt:message key="label.market"/></th>
							<th scope="col" width="11%"><fmt:message key="label.locale"/></th>
							<th scope="col" width="10%"><fmt:message key="label.model"/></th>
							<th scope="col" width="14%"><fmt:message key="label.manualtype"/></th>
							<th scope="col" width="13%"><fmt:message key="label.totaldocsforprocessing"/></th>
							<th scope="col" width="12%"><fmt:message key="label.totaldocsfordeletion"/></th>
							<th scope="col" width="12%"><fmt:message key="label.okassetscount"/></th>
							<th scope="col" width="12%"><fmt:message key="label.okassetsdeletecount"/></th>
						</tr>
					</thead>
					<tbody>
						<c:if test="${!empty historyBean.itemsList }">
							<c:forEach var="itemsList" items="${historyBean.itemsList }">
								<tr>
									<td><c:out value="${itemsList.srNo }"/></td>
									<td><c:out value="${itemsList.market }"/></td>
									<td>
										<c:out value="${itemsList.locale }"/>
									</td>
									<td>
										<c:out value="${itemsList.model }"/>
									</td>
									<td>
										<c:out value="${itemsList.manualType }"/>
									</td>
									<td><c:out value="${itemsList.totalDocsForProcessing }"/></td>
									<td><c:out value="${itemsList.totalDocsForDeletion }"/></td>
									<td><c:out value="${itemsList.okAssetsCount }"/></td>
									<td><c:out value="${itemsList.okAssetsDeleteCount }"/></td>
								</tr>
							</c:forEach>
						</c:if>
						<c:if test="${empty historyBean.itemsList }">
							<tr>
								<td colspan="9"><fmt:message key="label.norecordsfound" /> </td>
							</tr>
						</c:if>
					</tbody>
				</table>	
			</td>
		</tr>
	</table>
</div>
</c:if>


<div id="dmt_pending_dialog-confirm" title="<fmt:message key="label.view" /> <fmt:message key="label.reports" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="MDM_STATUS_LABEL"></u>. <fmt:message key="label.pending.dialog.info.end" /> </strong>
	</p>
	<br/>
	<br/>
	
	<p style="text-align: justify;font-size: 11px; !important">
		<b><u><fmt:message key="label.note" /> :</u></b> <fmt:message key="label.note.info"/>
	</p>
</div>


<div id="dmt_completed_dialog-confirm" class="hide">

</div>


<div id="dmt_view_item_details-dialog" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">

</div>

  

</body>  

<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<script type="text/javascript">
var calldtfunc=false;
</script>
<script type="text/javascript">


$(window).load(function() {
	$("#loader").fadeOut("slow");
});

var runReloadScript="<c:out value="${historyBean.runReloadScript }" />";

if(runReloadScript=="true")
{
	// reload page after every 30 seconds
	setTimeout(function()
	{
		window.location.reload(1);
	}, 120000);
}


//var htmlContent="<div class=\"separator\"></div>";
$(document).ready(function() {
	<c:if test="${!empty historyBean.scheduleList }">
		loadDataTable();
		calldtfunc = true;
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
	
	$('#example').dataTable({
		"dom": '<"top"fip>rt<"bottom"p><"clear">',
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
                        { aTargets: [ 10 ], bSortable: false },
                        { aTargets: [ 11 ], bSortable: false },
                        { aTargets: [ 12 ], bSortable: false }
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
	
//	$("div.toolbar").html(htmlContent);
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
	var pageNo=$("#MDM_HIS_displayPageNo").val();
	var pageLength=$("#MDM_HIS_displayPageLen").val();
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

function dmt_his_readDataTableValues()
{
	if(calldtfunc==true)
	{
		var table = $('#example').DataTable();
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		$("#MDM_HIS_displayPageNo").val(info.page);
		$("#MDM_HIS_displayPageLen").val(length);	
	}
}


function dmt_his_viewDetails(scheduleId)
{
	dmt_his_readDataTableValues();
	$("#MDM_HIS_ActionClicked").val("SHOW_ITEM_DETAILS");
	$("#MDM_HIS_ScheduleId").val(scheduleId);
	$("#MDM_HIS_Form").submit();
}

function dmt_his_abortConversionSendReq(scheduleId, threadId)
{
	dmt_his_readDataTableValues();
	$("#MDM_HIS_ActionClicked").val("ABORT_CONVERSION");
	$("#MDM_HIS_ScheduleId").val(scheduleId);
	$("#MDM_HIS_ThreadId").val(threadId);
	$("#MDM_HIS_Form").submit();
}
	
function dmt_his_closeShowDetails()
{
	dmt_his_readDataTableValues();
	$("#MDM_HIS_ActionClicked").val("CLOSE_ITEM_DETAILS");
	$("#MDM_HIS_Form").submit();
}

function dmt_his_abortConversion(scheduleId, threadId)
{
	// open dialog
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
		          dmt_his_abortConversionSendReq(scheduleId, threadId);
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
			MDM_HISTORY_VIEW_ITEM_DETAILS:"MDM_HISTORY_VIEW_ITEM_DETAILS",
			MDM_SCHEDULE_CODE:scheduleCode
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
						      modal: true,
						      width:900,
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

function dmt_viewReports(scheduleId, type){
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/schedulereports"; 
	var method ="post"; // Set method to post by default if not specified.

    // The rest of this code assumes you are not using a library.
    // It can be made less wordy if you use one.
    var form = document.createElement("form");
    form.setAttribute("method", method);
    form.setAttribute("action", schReports);

   
        
            var hiddenField = document.createElement("input");
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", "scheduleId");
            hiddenField.setAttribute("value", scheduleId);

            form.appendChild(hiddenField);
            
            var hiddenField1 = document.createElement("input");
            hiddenField1.setAttribute("type", "hidden");
            hiddenField1.setAttribute("name", "type");
            hiddenField1.setAttribute("value", type);

            form.appendChild(hiddenField1);
   
    document.body.appendChild(form);
    form.submit();
	

	// Make Ajax Call
	/* $.ajax({
		url:schReports,
		type:"post",
		data:{
			MDM_SCHEDULE_CODE:scheduleId,
			MDM_SCHEDULE_DOWNLOAD:"MDM_SCHEDULE_DOWNLOAD"
		},
		dataType:"text",
		success:function(responseData){
			alert("File Successfully Downloaded");
		},
		error:function(xmlhttp, status, error){
			// $("body").unmask();
			alert("error :: "+status);
		}
	}); */
}


function searchSubmit()
{
	$("#MDM_HIS_TD_Error_Message").html("");
	var fromDate=$("#fromDateField").val();
	var toDate = $("#toDateField").val();
	if(null!=fromDate && fromDate!='' && null!=toDate && toDate!='')
	{
		var fDate = new Date(fromDate);
		var tDate = new Date(toDate);
		if(fromDate >= toDate)
		{
			var err='<div class="errorMessage">From Date must be less than To Date.</div><div class="cb"></div>';
			$("#MDM_HIS_TD_Error_Message").html(err);
		}
		else
		{
			// show Loader
			 $("#loader").show();
			 dmt_his_readDataTableValues();
			 $("#MDM_HIS_ActionClicked").val("RESET");
			 $("#MDM_HIS_Form").submit();	
		}	
	}
	else
	{
		var err='<div class="errorMessage">From Date & To Date are mandatory.</div><div class="cb"></div>';
		$("#MDM_HIS_TD_Error_Message").html(err);
	}	
}

function dmt_viewReports1( scheduleCode, type){

		// call function to check whether the reports exists for the Schedule Code or not
		
		// $("body").mask("Please wait...");
		var title;
		if(type === 'downloadContent'){
			title = '<fmt:message key="label.content"/>'
		}else{
			title = '<fmt:message key="label.reports"/>';
		}
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
				MDM_HISTORY_VIEW_REPORTS:"MDM_HISTORY_VIEW_REPORTS",
				MDM_SCHEDULE_CODE:scheduleCode,
				MDM_SCHEDULE_STATUS:type
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
							      height:180,
							      width:600,
							      modal: true,
							      title: title,
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
</script>
        
</html>