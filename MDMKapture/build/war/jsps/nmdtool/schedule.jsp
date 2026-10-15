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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_NEW_MNAO_DATA_EXPORT_TOOL);
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
<jsp:useBean id="nmdScheduleBean" class="com.mazda.gms3.mdm.nmdtool.bean.NMDScheduleSessionBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.newdataexporttool"/> </title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery.datetimepicker.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />
<style type="text/css">
.cursorPointer {
	cursor: pointer;
}
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

.calendarField{
	width:180px;
	font-family: "InterstateMazda-Regular", Arial;
    border: 1px solid #DADADA;
    height: 25px;
    color: #5C5B65;
    border-radius: 2px;
    padding-left: 10px;
}
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.newdataexporttool"/> &rsaquo;</a>   
            </div>
            <form id="NMDTOOL_Form" name="NMDTOOL_Form" action="<%=request.getContextPath() %>/nmdschedule" method="post"  accept-charset="UTF-8">
            <input type="hidden" name="NMDTOOL_SelectedRows" id="NMDTOOL_SelectedRows" value="<c:out value="${nmdScheduleBean.selectedRows }"/>" />
            <input type="hidden" name="NMDTOOL_ActionClicked" id="NMDTOOL_ActionClicked" value="" />
			<input type="hidden" name="NMDTOOL_DataTabel_displayPageNo" id="NMDTOOL_DataTabel_displayPageNo" value="<c:out value="${nmdScheduleBean.displayPageNo }"/>" />
			<input type="hidden" name="NMDTOOL_DataTabel_displayPageLen" id="NMDTOOL_DataTabel_displayPageLen" value="<c:out value="${nmdScheduleBean.displayPageLength }"/>" />
            
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td  id="MDM_NMDTOOL_Error_Message" style="padding-bottom:5px !important;">
											<c:if test="${!empty nmdScheduleBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE" style="width:100% !important;">
													<%
														if(null!=nmdScheduleBean.getErrorMessage() && !"".equals(nmdScheduleBean.getErrorMessage()))
														{
															String[] msgs = nmdScheduleBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty nmdScheduleBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE" style="width:100% !important;">
													<c:out value="${nmdScheduleBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td>
											<h4 style="font-size:14px !important;">
												<fmt:message key="label.auto" /> <fmt:message key="label.scheduler.settings" />
											</h4>
											<p class="mandatory" style="font-size: 11px !important;padding-top:5px !important;padding-bottom:5px !important;"><i class="infoIcon"></i> <fmt:message key="label.note" />: <fmt:message key="label.auto.schedule.settings.info" /> </p>
										</td>
									</tr>
									<tr>
										<td>
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td width="30%">
														<div class="formElement_row leftpadding_none">
															<label style="width:50px !important;"><fmt:message key="label.day"/><span class="mandatory">*</span></label>
															<select name="NMDTOOL_DayOfMonth" id="NMDTOOL_DayOfMonth">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty nmdScheduleBean.daysList }">
																	<c:forEach var="daysList" items="${nmdScheduleBean.daysList }">
																		<c:set var="selectedDayFlag" value="" />
																		<c:if test="${!empty nmdScheduleBean.dayOfMonth}">
																			<c:if test="${daysList.value eq nmdScheduleBean.dayOfMonth}">
																				<c:set var="selectedDayFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedDayFlag eq 1}">
																				<option selected="selected" value="<c:out value="${daysList.value }"/>"><c:out value="${daysList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${daysList.value }"/>"><c:out value="${daysList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</div>
														<div class="cb"></div>
													</td>
													<td width="30%">
														<div class="formElement_row leftpadding_none">
														<label style="width:50px !important;"><fmt:message key="label.hour"/><span class="mandatory">*</span></label>
														<select name="NMDTOOL_HourOfMonth" id="NMDTOOL_HourOfMonth">
																	<option value=""><fmt:message key="label.selectOne"/></option>
																	<c:if test="${!empty nmdScheduleBean.hoursList }">
																	<c:forEach var="hoursList" items="${nmdScheduleBean.hoursList }">
																		<c:set var="selectedHourFlag" value="" />
																		<c:if test="${!empty nmdScheduleBean.timeOfMonth}">
																			<c:if test="${hoursList.value eq nmdScheduleBean.timeOfMonth}">
																				<c:set var="selectedHourFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedHourFlag eq 1}">
																				<option selected="selected" value="<c:out value="${hoursList.value }"/>"><c:out value="${hoursList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${hoursList.value }"/>"><c:out value="${hoursList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select> 
														</div>
														<div class="cb"></div>
													</td>
													<td width="30%">
														<div class="formElement_row leftpadding_none">
															<input class="bluebutton cursorPointer" type="button" id="NMDTOOL_Submit" style="width: 90px !important;padding:7px !important;" 
																onclick="NMDTOOL_submit();" name="NMDTOOL_Submit" value="<fmt:message key="label.schedule"/>">
														</div>
														<div class="cb"></div>
													</td>
												</tr>
											</table>
										</td>
									</tr>		
								</table>
								
								<p style="height:15px;">&nbsp;</p>
								<h4 style="font-size:14px !important;">
									<fmt:message key="label.model"/> <fmt:message key="label.details"/>
								</h4>
								<p class="mandatory" style="font-size: 11px !important;padding-top:5px !important;padding-bottom:5px !important;"><i class="infoIcon"></i> <fmt:message key="label.note" />: <fmt:message key="label.checkbox.selection.bulk.ext.screen" /> </p>
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"/>
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="18%"><fmt:message key="label.en_us"/> <fmt:message key="label.models"/></th>
											<th scope="col" width="20%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="17%"><fmt:message key="label.lastexecutiontime"/></th>
											<th scope="col" width="15%"><fmt:message key="label.modifiedby"/></th>
											<th scope="col" width="10%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty nmdScheduleBean.schedulesList }">
											<c:forEach var="schedulesList" items="${nmdScheduleBean.schedulesList }">
												<tr>
													<td> 
														<input type="checkbox" <c:if test="${schedulesList.selected eq true }"> checked="checked" </c:if>  name="NMDTOOL_Selection" onchange="NMDTOOL_checkBoxManagement();" 
															value="<c:out value="${schedulesList.srNo }" />" 
															id="NMDTOOL_Selection_<c:out value="${schedulesList.srNo }"  />" />
													</td>
													<td><c:out value="${schedulesList.srNo }" /> </td>
													<td>
														<label><c:out value="${schedulesList.model }" /></label>
													</td>
													<td>
														<c:if test="${!empty schedulesList.dayOfMonth }">
															<label><fmt:message key="label.scheduledon"/> <b><c:out value="${schedulesList.dayOfMonth }" /> <fmt:message key="label.day"/></b> <fmt:message key="label.ofeverymonht"/> <fmt:message key="label.at"/> <b><c:out value="${schedulesList.timeofMonth }" /> <fmt:message key="label.hours"/></b>.</label>
														</c:if>
														<c:if test="${empty schedulesList.dayOfMonth }">
															<label>-</label>
														</c:if>
													</td>
													<td>
														<fmt:formatDate value="${schedulesList.lastExecutionTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<label><c:out value="${schedulesList.wslId }" /></label>
													</td>
													<td>
														<fmt:formatDate value="${schedulesList.createdTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${schedulesList.modifiedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
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

<div id="warning_dialog-confirm" title="<fmt:message key="label.confirm"/>" class="hide">
	<p>
		<strong><fmt:message key="label.warning.deltatime.reset.description"/></strong>
	</p>
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<c:if test="${!empty nmdScheduleBean.schedulesList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
var runOnDemand = "<fmt:message key="label.runondemand"/>";
var resetForFullExtract="<fmt:message key="label.reset.for.full.extract"/>";
var updateForDeltaExtract="<fmt:message key="label.update.for.delta.extract"/>";
var deltaDateAndTime="<fmt:message key="label.delta.datetime"/>";

htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"NMDTOOL_runondemand();\"  name=\"NMDTOOL_RunOnDemand\" id=\"NMDTOOL_RunOnDemand\" class=\"bluebutton cursorPointer\">"+runOnDemand+"</button>";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"NMDTOOL_resetfullextractime();\"  name=\"NMDTOOL_ResetFullExtract\" id=\"NMDTOOL_ResetFullExtract\" class=\"bluebutton cursorPointer\">"+resetForFullExtract+"</button>";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"NMDTOOL_updatedeltaextractime();\"  name=\"NMDTOOL_UpdateDeltaExtract\" id=\"NMDTOOL_UpdateDeltaExtract\" class=\"bluebutton cursorPointer\">"+updateForDeltaExtract+"</button>";
// add datetimepicker field
var dateTimeValue="<c:out value="${nmdScheduleBean.deltaLastExecTime }" />";
htmlContent=htmlContent+"<input class=\"calendarField\" placeholder=\""+deltaDateAndTime+"\" type=\"text\" id=\"NMDTOOL_DeltaLastExecTime\" name=\"NMDTOOL_DeltaLastExecTime\" value=\""+dateTimeValue+"\">";
resetForFullExtract = null;
runOnDemand = null;
</script>
</c:if>
<c:if test="${empty nmdScheduleBean.schedulesList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


$(document).ready(function() {
	loadDataTable();
	
	// date pickers initialization
	$( "#NMDTOOL_DeltaLastExecTime" ).datetimepicker({
		format:'d-m-y H:i',
		step:15,
		maxDate: new Date(),
		changeYear:true,
		changeMonth:true,
		showButtonPanel: true
	});
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
	var pageNo=$("#NMDTOOL_DataTabel_displayPageNo").val();
	var pageLength=$("#NMDTOOL_DataTabel_displayPageLen").val();
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
		NMDTOOL_checkBoxManagement();
	});
}

function NMDTOOL_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#NMDTOOL_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#NMDTOOL_DataTabel_displayPageLen").val(length);	
	}	
	
}


function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_NMDTOOL_Error_Message").html(html);
	window.scrollTo(0,0);
}

function NMDTOOL_checkBoxManagement()
{
	$("#NMDTOOL_SelectedRows").val("");
	var value=$("#NMDTOOL_SelectedRows").val();
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
		$("#NMDTOOL_SelectedRows").val(result);
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
		$("#NMDTOOL_SelectedRows").val("");
	}
	NMDTOOL_checkBoxManagement();
}

//performs Operation Type Change Submit
function NMDTOOL_optype(thisObj)
{
	$("#loader").show();
	$("#NMDTOOL_DataExtractionTypeId").val($(thisObj).val());
	NMDTOOL_checkBoxManagement();
	NMDTOOL_readDataTableValues();
	$("#NMDTOOL_Form").submit();
}


function NMDTOOL_submit()
{
	$("#loader").show();
	$("#NMDTOOL_ActionClicked").val("SUBMIT");
	NMDTOOL_checkBoxManagement();
	NMDTOOL_readDataTableValues();
	$("#NMDTOOL_Form").submit();
}

function NMDTOOL_runondemand()
{
	var selRows=$("#NMDTOOL_SelectedRows").val();
	if(null!=selRows && selRows!='')
	{
		$("#loader").show();
		$("#NMDTOOL_ActionClicked").val("RUN_ONDEMAND");
		NMDTOOL_checkBoxManagement();
		NMDTOOL_readDataTableValues();
		$("#NMDTOOL_Form").submit();
	}	
	else
	{
		var delMessage="<fmt:message key="error.select.onerow" />";
		mdm_show_errorMessage(delMessage);	
	}	
}

function NMDTOOL_resetfullextractime()
{
	var selRows=$("#NMDTOOL_SelectedRows").val();
	if(null!=selRows && selRows!='')
	{
		if(selRows.endsWith(','))
		{
			selRows =selRows.substring(0,selRows.length-1);	
		}
		var tok = selRows.split(',');
		if(null!=tok && tok.length==1)
		{
			$( "#warning_dialog-confirm" ).removeClass('hide').dialog({
				  closeOnEscape: false,
				  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			      resizable: false,
			      height:140,
			      modal: true,
			      buttons: {
			    	"<fmt:message key="label.yes"/>": function() {
			    			// show Loader	
			    			$("#loader").show();
							$("#NMDTOOL_ActionClicked").val("RESET_FULL_EXTRACT_LAST_EXEC_TIME");
							NMDTOOL_checkBoxManagement();
							NMDTOOL_readDataTableValues();
							$("#NMDTOOL_Form").submit();	
						}  
				  	,
			        "<fmt:message key="label.no"/>": function() {
			          $( this ).dialog( "close" );
			        }
				}
			});
		}	
		else
		{
			var errorMessage="<fmt:message key="error.message.one.model.selection" />";
			mdm_show_errorMessage(errorMessage);
		}
	}	
	else
	{
		var delMessage="<fmt:message key="error.select.onerow" />";
		mdm_show_errorMessage(delMessage);	
	}	
}

function NMDTOOL_updatedeltaextractime()
{
	var selRows=$("#NMDTOOL_SelectedRows").val();
	if(null!=selRows && selRows!='')
	{
		if(selRows.endsWith(','))
		{
			selRows =selRows.substring(0,selRows.length-1);	
		}
		var tok = selRows.split(',');
		if(null!=tok && tok.length==1)
		{
			$( "#warning_dialog-confirm" ).removeClass('hide').dialog({
				  closeOnEscape: false,
				  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			      resizable: false,
			      height:140,
			      modal: true,
			      buttons: {
			    	"<fmt:message key="label.yes"/>": function() {
			    			// show Loader	
				    		$("#loader").show();
							$("#NMDTOOL_ActionClicked").val("UPDATE_DELTA_EXTRACT_LAST_EXEC_TIME");
							NMDTOOL_checkBoxManagement();
							NMDTOOL_readDataTableValues();
							$("#NMDTOOL_Form").submit();	
						}  
				  	,
			        "<fmt:message key="label.no"/>": function() {
			          $( this ).dialog( "close" );
			        }
				}
			});
		}
		else
		{
			var errorMessage="<fmt:message key="error.message.one.model.selection" />";
			mdm_show_errorMessage(errorMessage);	
		}
	}	
	else
	{
		var delMessage="<fmt:message key="error.select.onerow" />";
		mdm_show_errorMessage(delMessage);	
	}	
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
						 NMDTOOL_readDataTableValues();
						 $("#NMDTOOL_ActionClicked").val("RESET");
						 $("#NMDTOOL_Form").submit();
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

