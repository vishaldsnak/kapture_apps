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
String applicationContext=ApplicationProperties.getProperty("application.environment.context");
 %>	
<html>
<head>
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<!-- 
	SET PAGE NAME
 -->
<%
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_CD_CREATION);
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
<jsp:useBean id="CDRomSearchBean" class="com.mazda.gms3.cdrom.bean.CDRomSearchBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.cdcreation"/> </title>
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
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

#dialog{
	height:auto !important;
}
</style>

</head>
<jsp:include page="../headeragent.jsp" flush="true" />
<body>
<!-- loader Starts here  -->
<div class="ui-overlay" style="z-index:9999;position:absolute" id="loader">
	<div class="ui-widget-overlay"></div>
	<div class="ui-widget-shadow ui-corner-all" style="position: absolute; top: 250px; left: 504.5px; width: 300px; height: 37px;">
	</div>
	<div class="loadmask-msg ui-widget ui-widget-content ui-corner-all" style="text-align:center; position: absolute; padding: 10px; top: 250px; left: 504.5px; width:278px;"><div class="ui-overlay-loading"><fmt:message key="label.window.loding.message" /></div></div>
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.cdcreation"/> &rsaquo;</a> 
           	</div>
           	
           	<div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        	<form id="CD_Rom_Form" name="CD_Rom_Form" action="<%=request.getContextPath() %>/cdrom" method="post">
                        	<input type="hidden" id="contextPath" value="<%=request.getContextPath() %>">
							<input type="hidden" name="MDM_HIS_displayPageNo" id="MDM_HIS_displayPageNo" value="<c:out value="${CDRomSearchBean.displayPageNo }"/>" />
							<input type="hidden" name="MDM_HIS_displayPageLen" id="MDM_HIS_displayPageLen" value="<c:out value="${CDRomSearchBean.displayPageLength }"/>" /> 
            
							<input type="hidden" name="CD_Rom_SearchSchId" id="CD_Rom_SearchSchId" value="" />
							<input type="hidden" name="CD_Rom_ActionClicked" id="CD_Rom_ActionClicked" value="<c:out value="${CDRomSearchBean.actionClicked }"/>">
                        	<table width="100%">
									<tr>
										<td id="MDM_HIS_TD_Error_Message">
											<c:if test="${!empty CDRomSearchBean.errorMessage }">
											<div class="errorMessage">
												<%
													if (null != CDRomSearchBean.getErrorMessage() && !"".equals(CDRomSearchBean.getErrorMessage())) {
															String[] msgs = CDRomSearchBean.getErrorMessage().split(",");
															if (null != msgs && msgs.length > 0) {
																for (int i = 0; i < msgs.length; i++) {
												%>
												<%=msgs[i].toString()%><br />
												<%
													}
															}
														}
												%>
											</div>
										</c:if>
										</td>
                        			</tr>
                        			<tr>
                        				<td>
                        					<div class="help"> 
									         	<h3 style="font-size: 14px;"><fmt:message key="label.dash.cdcreation"/> </h3><span><div class="helpicon">?</div><a href="javascript:void(0);" onclick="cdrom_loadHelpPop();" style="text-decoration: none;"><fmt:message key="label.cdrom.help"/> </a></span>
									          </div>
									          <div id="dialog" style="display: none; line-height: 1.3">
												   <div><fmt:message key="label.cdrom.help.popup.1"/></div>
												   
												   <ul style="margin-left: 50px;">
												   		<li><fmt:message key="label.cdrom.help.popup.message1"/></li>
												   		<li><fmt:message key="label.cdrom.help.popup.message2"/></li>
												   		<!-- <li><fmt:message key="label.cdrom.help.popup.message4"/></li> -->
												   		<li><fmt:message key="label.cdrom.help.popup.message3"/></li>
												   </ul>
												   <div><fmt:message key="label.cdrom.help.popup.2"/></div>
												   <hr>
									    		<input type="button" class="gear_button cursorPointer" style="margin-left: 200px; margin-top: 5px" value="<fmt:message key="label.close"/>" name="close" id="close">
									   		  </div>
												<div class="first">
															<label><fmt:message key="label.country"/><span class="mandatory">*</span></label>
															
															<select name="CD_Rom_Country_List" id="CD_Rom_Country_List" style="width: 159px !important;" 
																onchange="cdrom_countrySelection();">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:if test="${!empty CDRomSearchBean.countryLocaleList}"></c:if>
																		<c:forEach var="countryList" items="${CDRomSearchBean.countryLocaleList}">
																			<c:choose>
																			<c:when test="${countryList.countryLocaleId == CDRomSearchBean.selectedCountry }">
																				<option value="<c:out value="${countryList.countryLocaleId}" />" selected="selected"><c:out value="${countryList.countryLocaleCode}" /></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${countryList.countryLocaleId}" />"><c:out value="${countryList.countryLocaleCode}" /></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
															</select>
														
														<span><fmt:message key="label.language"/><span class="mandatory">*</span></span>
													
															<select name="CD_Rom_Language_List" id="CD_Rom_Language_List" style="width: 158px !important;" 
																onchange="cdrom_languageSelection();">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:forEach var="languageList" items="${CDRomSearchBean.languageList}">
																	<c:choose>
																	<c:when test="${languageList.manualLanguageName == CDRomSearchBean.selectedLanguage }">
																		<option value="<c:out value="${languageList.manualLanguageName}" />" selected="selected"><c:out value="${languageList.manualLanguageCode}" /></option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${languageList.manualLanguageName}" />"><c:out value="${languageList.manualLanguageCode}" /></option>
																	</c:otherwise>
																	</c:choose>
																	</c:forEach>
															</select> 
													
															
														<br><br><label><fmt:message key="label.model"/><span class="mandatory">*</span></label>
													
															<select name="CD_Rom_Models_List" id="CD_Rom_Models_List" style="width: 406px !important;" 
																onchange="cdrom_modelSelection();">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:forEach var="modelList" items="${CDRomSearchBean.models}">
																	<c:choose>
																	<c:when test="${modelList.key == CDRomSearchBean.selectedModel }">
																		<option value="<c:out value="${modelList.key}" />" selected="selected"><c:out value="${modelList.value}" /></option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${modelList.key}" />"><c:out value="${modelList.value}" /></option>
																	</c:otherwise>
																	</c:choose>
																	</c:forEach>
															</select>
												
													
														<br><br><label><fmt:message key="label.vin"/><span class="mandatory">*</span></label>
												
															<select name="CD_Rom_Wmi_List" id="CD_Rom_Wmi_List" style="width: 190px;" onchange="cdrom_wmiSelection();">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:forEach var="wmiList" items="${CDRomSearchBean.wmi}">
																	<c:choose>
																	<c:when test="${wmiList.key == CDRomSearchBean.selectedWmi }">
																		<option value="<c:out value="${wmiList.key}" />" selected="selected"><c:out value="${wmiList.value}" /></option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${wmiList.key}" />"><c:out value="${wmiList.value}" /></option>
																	</c:otherwise>
																	</c:choose>
																	</c:forEach>
															</select>
														
															<select name="CD_Rom_vds_List" id="CD_Rom_vds_List" style="width: 190px;" onchange="cdrom_vdsSelection();">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:forEach var="vdsList" items="${CDRomSearchBean.vds}">
																	<c:choose>
																	<c:when test="${vdsList.key == CDRomSearchBean.selectedVds }">
																		<option value="<c:out value="${vdsList.key}" />" selected="selected"><c:out value="${vdsList.value}" /></option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${vdsList.key}" />"><c:out value="${vdsList.value}" /></option>
																	</c:otherwise>
																	</c:choose>
																	</c:forEach>
															</select>
															
															<select name="CD_Rom_vinRange_List" id="CD_Rom_vinRange_List" style="width: 190px;">
																	<option value=""><fmt:message key="label.cdrom.default.select.option"/></option>
																	<c:forEach var="vinList" items="${CDRomSearchBean.vinRange}">
																	<c:choose>
																	<c:when test="${vinList.key == CDRomSearchBean.selectedVinRange }">
																		<option value="<c:out value="${vinList.key}" />" selected="selected"><c:out value="${vinList.value}" /></option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${vinList.key}" />"><c:out value="${vinList.value}" /></option>
																	</c:otherwise>
																	</c:choose>
																	</c:forEach>
															</select>
										
														
														<input class="bluebutton cursorPointer" type="button" id="CDRom_Next" onclick="cdrom_Go();" name="CDRom_Next" value="<fmt:message key="label.go"/>">
													    
													
											</div>
                        				</td>
                        			</tr>
                        			<tr>
                        				<td style="height: 5px;">&nbsp;</td>
                        			</tr>
                        			<tr>
										<td>
											<div style="margin: auto;width: 80%;padding-bottom: 10px;">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tbody>
														<tr>
														<td style="text-align:right;">From Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="fromDateField" name="fromDateField" value="<c:out value="${CDRomSearchBean.fromDate }" />"></td>
														<td style="text-align:right;">To Date<span class="mandatory">*</span></td>
														<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="toDateField" name="toDateField" value="<c:out value="${CDRomSearchBean.toDate }" />"></td>
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
                        			<c:if test="${! empty CDRomSearchBean.searchCritriaJobList }">
                        			<tr>
                        				<td>
                        					<table id="example" class="display historyTable" cellspacing="0" width="100%">
												<thead>
													<tr>
														<th scope="col" width="3%">#.</th>
														<th scope="col" width="9%"><fmt:message key="label.schedulename" /> </th>
														<th scope="col" width="9%"><fmt:message key="label.scheduledby" /></th>
														<th scope="col" width="9%"><fmt:message key="label.scheduletime" /></th>
														<th scope="col" width="9%"><fmt:message key="label.finishtime" /></th>
														<th scope="col" width="9%"><fmt:message key="label.locale" /></th>
														<th scope="col" width="11%"><fmt:message key="label.model" />
														<th scope="col" width="8%"><fmt:message key="label.wmi" /></th>
														<th scope="col" width="8%"><fmt:message key="label.vds" /></th> 
														<th scope="col" width="8%"><fmt:message key="label.vis" /></th>
														<th scope="col" width="8%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
														<th scope="col" width="9%">Action</th>
													</tr>
												</thead>
												<tbody>
													<c:if test="${! empty CDRomSearchBean.searchCritriaJobList }">
														<c:forEach var="searchCritriaJobList" items="${CDRomSearchBean.searchCritriaJobList }">
															<tr>
																<td><c:out value="${searchCritriaJobList.srNo }"/> </td>
																<td><c:out value="${searchCritriaJobList.scheduleName }"/></td>
																<td><c:out value="${searchCritriaJobList.userId }"/></td>
																<td><fmt:formatDate value="${searchCritriaJobList.scheduleTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><fmt:formatDate value="${searchCritriaJobList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
																<td><c:out value="${searchCritriaJobList.localeCode }"/></td>
																<td><c:out value="${searchCritriaJobList.carlineNameRegional }"/></td>
																<td><c:out value="${searchCritriaJobList.vinWmiCode }"/></td>
																<td><c:out value="${searchCritriaJobList.vinVdsCode }"/></td>
																<td><c:out value="${searchCritriaJobList.vinStartRange }"/></td>
																<td>
																	<c:out value="${searchCritriaJobList.scheduleStatusLabel }"/>
																</td>
																<td> 
																	<c:if test="${searchCritriaJobList.showViewLink eq true }">
																		<a href="javascript:void(0);" onclick="cdrom_movetodetails('<c:out value="${searchCritriaJobList.scheduleId }"/>');">Click Next</a>
																	</c:if>
																</td>
															</tr>	
														</c:forEach>
													</c:if>
													<c:if test="${empty CDRomSearchBean.searchCritriaJobList }">
														<tr>
															<td colspan="12"><fmt:message key="label.norecordsfound" /> </td>
														</tr>
													</c:if>
												</tbody>
											</table>
                        				</td>
                        			</tr>
                        			</c:if>
                        			<c:if test="${empty CDRomSearchBean.searchCritriaJobList }">
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
                        	</form>
                        	</div>
                        </div>
                      </div>
                    </div>
                </div>      	
          
		
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<script type="text/javascript">
var calldtfunc=false;
</script>
<c:if test="${! empty CDRomSearchBean.searchCritriaJobList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
// add separator when transactionsList is not empty
htmlContent="<div class=\"separator\" style=\"padding-right:20px;\"></div>&nbsp;";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"cdrom_Refresh();\"  name=\"refresh\" id=\"refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
refreshLabel = null;
</script>
</c:if>



<script type="text/javascript">

$(document).ready(function() {
	<c:if test="${!empty CDRomSearchBean.searchCritriaJobList }">
		loadDataTable();
		calldtfunc = true;
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
	
});

</script>


<script type="text/javascript">
/*
var runReloadScript="<c:out value="${CDRomSearchBean.runReloadScript }" />";

if(runReloadScript=="true")
{
	// reload page after every 30 seconds
	setTimeout(function()
	{
		//window.location.reload(1);
		cdrom_Refresh();
	}, 120000);
}
*/


$(window).load(function() {
	$("#loader").fadeOut("slow");
});

$("#dialog").dialog({
    modal: true,
    autoOpen: false,
    title: '<fmt:message key="label.cdrom.help"/>',
    width: 300,
    height: 180
});


$('#close').click(function() {
	$('#dialog').dialog('close');
});

function cdrom_loadHelpPop(){
	$('#dialog').dialog('open');
}


function cdrom_countrySelection()
{
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("COUNTRY_SELECTION");
	$("#CD_Rom_Form").submit();	
}

function cdrom_languageSelection()
{
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("LANGUAGE_SELECTION");
	$("#CD_Rom_Form").submit();	
}

function cdrom_modelSelection()
{
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("MODEL_SELECTION");
	$("#CD_Rom_Form").submit();	
}

function cdrom_wmiSelection()
{
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("WMI_SELECTION");
	$("#CD_Rom_Form").submit();	
}

function cdrom_vdsSelection()
{
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("VDS_SELECTION");
	$("#CD_Rom_Form").submit();	
}

function cdrom_Go()
{
	$("#loader").show();
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("GO_CLICKED");
	$("#CD_Rom_Form").submit();
}

function cdrom_Refresh()
{
	$("#loader").show();
	$("#CD_Rom_ActionClicked").val("REFRESH");
	$("#CD_Rom_Form").submit();
}

function cdrom_movetodetails(schId)
{
	$("#loader").show();
	dmt_his_readDataTableValues();
	$("#CD_Rom_ActionClicked").val("REDIRECT_TO_DETAIL");
	$("#CD_Rom_SearchSchId").val(schId);
	$("#CD_Rom_Form").submit();	
}

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
		"bDestroy":true,
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
                        { aTargets: [ 11 ], bSortable: false }
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
			 $("#CD_Rom_ActionClicked").val("REFRESH");
			 $("#CD_Rom_Form").submit();	
		}	
	}
	else
	{
		var err='<div class="errorMessage">From Date & To Date are mandatory.</div><div class="cb"></div>';
		$("#MDM_HIS_TD_Error_Message").html(err);
	}	
}

</script>

</html>