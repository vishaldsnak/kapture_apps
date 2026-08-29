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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_MNAO_DATA_EXPORT_TOOL);
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
<jsp:useBean id="dataExportToolBean" class="com.mazda.gms3.mdm.dataexttool.bean.DataExportToolSessionBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.dataexporttool"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.dataexporttool"/> &rsaquo;</a>   
            </div>
            <form id="DATAEXPTOOL_Form" name="DATAEXPTOOL_Form" action="<%=request.getContextPath() %>/mnaodataexport" method="post"  accept-charset="UTF-8">
            <input type="hidden" name="DATAEXPTOOL_SelectedRows" id="DATAEXPTOOL_SelectedRows" value="<c:out value="${dataExportToolBean.selectedRows }"/>" />
            <input type="hidden" name="DATAEXPTOOL_ActionClicked" id="DATAEXPTOOL_ActionClicked" value="<c:out value="${dataExportToolBean.actionClicked }"/>" />
			<input type="hidden" name="DATAEXPTOOL_DataTabel_displayPageNo" id="DATAEXPTOOL_DataTabel_displayPageNo" value="<c:out value="${dataExportToolBean.displayPageNo }"/>" />
			<input type="hidden" name="DATAEXPTOOL_DataTabel_displayPageLen" id="DATAEXPTOOL_DataTabel_displayPageLen" value="<c:out value="${dataExportToolBean.displayPageLength }"/>" />
            <input type="hidden" name="DATAEXPTOOL_DataExtractionTypeId" id="DATAEXPTOOL_DataExtractionTypeId" value="<c:out value="${dataExportToolBean.selectedDataExtractionType }"/>" />
            
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td  id="MDM_DATAEXPTOOL_Error_Message" colspan="4">
											<c:if test="${!empty dataExportToolBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=dataExportToolBean.getErrorMessage() && !"".equals(dataExportToolBean.getErrorMessage()))
														{
															String[] msgs = dataExportToolBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty dataExportToolBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${dataExportToolBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty dataExportToolBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${dataExportToolBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td colspan="4">
											<div class="errorMessage">
												Note: Maximum 1 Model can be selected from the below criteria for export at a time.
											</div>
											<div class="cb"></div>
										</td>
									</tr>
									<!--  ADD RADIO SELECTION BUTTON HERE  -->
									<tr style="height:35px;">
										<td style="font-size:15px;" colspan="4">
											<label style="margin-right:200px !important;"><fmt:message key="label.dataextractiontype"/>:</label>
											<c:if test="${!empty dataExportToolBean.dataExtractionTypeList }">
												<c:forEach var="dataExtractionTypeList" items="${dataExportToolBean.dataExtractionTypeList }">
													<c:set value="0" var="operationTypeFlag"/>
													<c:if test="${!empty dataExportToolBean.selectedDataExtractionType }">
														<c:if test="${dataExportToolBean.selectedDataExtractionType eq dataExtractionTypeList.value }">
															<c:set value="1" var="operationTypeFlag"/>
														</c:if>
													</c:if>
													<c:choose>
														<c:when test="${operationTypeFlag eq 1}">
															<input type="radio" checked="checked" onchange="dataexptool_optype(this);" name="DATAEXPTOOL_DataExtractionType" id="DATAEXPTOOL_DataExtractionType_<c:out value="${dataExtractionTypeList.value }"/>"  value="<c:out value="${dataExtractionTypeList.value }"/>" /> <span style="margin-right:100px;"><c:out value="${dataExtractionTypeList.label }"/></span>
														</c:when>
														<c:otherwise>
															<input type="radio" name="DATAEXPTOOL_DataExtractionType" onchange="dataexptool_optype(this);" id="DATAEXPTOOL_DataExtractionType_<c:out value="${dataExtractionTypeList.value }"/>"  value="<c:out value="${dataExtractionTypeList.value }"/>" /> <span style="margin-right:100px;"><c:out value="${dataExtractionTypeList.label }"/></span>
														</c:otherwise>
													</c:choose>	
												</c:forEach>
											</c:if>
										 </td>
									</tr>
									
									<c:if test="${dataExportToolBean.showDatesBlock eq true }">
										<tr>
											<td colspan="4">
												<div style="margin: auto;width: 100%;padding-bottom: 10px;">
													<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
														<tbody>
															<tr>
																<td style="text-align:right;"><fmt:message key="label.pubdoc.fromdate" /> <span class="mandatory">*</span></td>
																<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DATAEXPTOOL_FromDate" name="DATAEXPTOOL_FromDate" value="<c:out value="${dataExportToolBean.fromDate }" />"></td>
																<td style="text-align:right;"><fmt:message key="label.pubdoc.todate" /> <span class="mandatory">*</span></td>
																<td><input style="width: 220px;" placeholder="YYYY-MM-DD" type="text" id="DATAEXPTOOL_ToDate" name="DATAEXPTOOL_ToDate" value="<c:out value="${dataExportToolBean.toDate }" />"></td>
															</tr>
														</tbody>
													</table>
												</div>
											</td>
										</tr>
									</c:if>
									
									
									<tr>
										<td width="30%">
											<div class="formElement_row leftpadding_none">
												<label style="width:50px !important;"><fmt:message key="label.country"/><span class="mandatory">*</span></label>
												<select multiple="multiple" name="DATAEXPTOOL_CountryId" id="DATAEXPTOOL_CountryId" style="width: 220px !important;height: 160px !important;">
														<c:if test="${empty dataExportToolBean.countryLocaleList }">
														<option value=""><fmt:message key="label.selectOne"/></option>
														</c:if>
														<c:if test="${!empty dataExportToolBean.countryLocaleList }">
															<c:forEach var="countryLocaleList" items="${dataExportToolBean.countryLocaleList }">
																<c:set var="selectedFlagLang" value="" />
																<c:if test="${!empty dataExportToolBean.countryLocaleId}">
																	<c:forEach var="selCountries" items="${dataExportToolBean.countryLocaleId }">
																		<c:if test="${selCountries eq countryLocaleList.countryLocaleId}">
																			<c:set var="selectedFlagLang" value="1" />
																		</c:if>
																	</c:forEach>
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
											</div>
											<div class="cb"></div>
										</td>
										<td width="30%">
											<div class="formElement_row leftpadding_none">
											<label style="width:50px !important;"><fmt:message key="label.language"/><span class="mandatory">*</span></label>
											<select multiple="multiple" name="DATAEXPTOOL_LanguageId" id="DATAEXPTOOL_LanguageId" style="width: 220px !important;height: 160px !important;" 
												onchange="dataexptool_languageSelection();">
													<c:if test="${empty dataExportToolBean.languageList }">
													<option value=""><fmt:message key="label.selectOne"/></option>
													</c:if>
													<c:if test="${!empty dataExportToolBean.languageList }">
														<c:forEach var="languageList" items="${dataExportToolBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:forEach var="selLanguages" items="${dataExportToolBean.manualLanguageId }">
																	<c:if test="${selLanguages eq languageList.manualLanguageId}">
																		<c:set var="selectedFlagLang" value="1" />
																	</c:if>
																</c:forEach>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
																	<option selected="selected" value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/> <c:if test="${!empty languageList.manualLanguageName }">(<c:out value="${languageList.manualLanguageName }"/>) </c:if></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/> <c:if test="${!empty languageList.manualLanguageName }">(<c:out value="${languageList.manualLanguageName }"/>)</c:if></option>
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
											<label style="width:50px !important;"><fmt:message key="label.model"/><span class="mandatory">*</span></label>
											<select size="4" name="DATAEXPTOOL_ModelId" id="DATAEXPTOOL_ModelId" style="width: 220px !important;height: 160px !important;">
												<c:if test="${empty dataExportToolBean.modelsList }">
												<option value=""><fmt:message key="label.selectOne"/></option>
												</c:if>
													<c:if test="${!empty dataExportToolBean.modelsList }">
														<c:forEach var="modelsList" items="${dataExportToolBean.modelsList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty modelsList.manualLanguageId}">
																<c:forEach var="selModels" items="${dataExportToolBean.modelId }">
																	<c:if test="${selModels eq modelsList.carlineIdForCombo}">
																		<c:set var="selectedFlagLang" value="1" />
																	</c:if>
																</c:forEach>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
																	<option selected="selected" value="<c:out value="${modelsList.carlineIdForCombo }"/>"><c:out value="${modelsList.carlineIdForCombo }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${modelsList.carlineIdForCombo }"/>"><c:out value="${modelsList.carlineIdForCombo }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>	
											</select>
											</div>
											<div class="cb"></div>
										</td>
										<td>
											<input class="bluebutton cursorPointer" type="button" id="DATAEXPTOOL_Next" onclick="dataexptool_Go();" name="DATAEXPTOOL_Next" value="<fmt:message key="label.go"/>">
										</td>
									</tr>		
								</table>
								
								<p style="height:45px;">&nbsp;</p>
								<h4 style="font-size:14px !important;">
									<fmt:message key="label.items"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty dataExportToolBean.itemsList }">
											<c:forEach var="itemsList" items="${dataExportToolBean.itemsList }">
												<c:if test="${itemsList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty dataExportToolBean.itemsList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="13%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="22%"><fmt:message key="label.model"/></th>
											<th scope="col" width="22%"><fmt:message key="label.channel"/></th>
											<th scope="col" width="23%"><fmt:message key="label.documenttype"/></th>
											<th scope="col" width="18%"><fmt:message key="label.documentscount"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty dataExportToolBean.itemsList }">
											<c:forEach var="itemsList" items="${dataExportToolBean.itemsList }">
												<tr <c:if test="${itemsList.editableFlag eq true  }"> class="selected"</c:if>>
													<td> 
														<input type="checkbox" name="DATAEXPTOOL_Selection" onchange="DATAEXPTOOL_checkBoxManagement();" 
															value="<c:out value="${itemsList.srNo }" />" 
															id="DATAEXPTOOL_Selection_<c:out value="${itemsList.srNo }"  />"
															<c:if test="${itemsList.editableFlag eq true  }">checked="checked"</c:if> />
													</td>
													<td><c:out value="${itemsList.srNo }" /> </td>
													<td>
														<label><c:out value="${itemsList.locale }" /></label>
													</td>
													<td>
														<label><c:out value="${itemsList.model }" /></label>
													</td>
													<td>
														<label><c:out value="${itemsList.channelName }" /></label>
													</td>
													<td>
														<c:if test="${!empty itemsList.documentType}">
															<label><c:out value="${itemsList.documentType }" /></label>
														</c:if>
														<c:if test="${ empty itemsList.documentType}">
															<label>-</label>
														</c:if>
														
													</td>
													<td>
														<label><c:out value="${itemsList.documentsCounts }" /></label>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								
								<table width="100%" border="0" cellspacing="0" cellpadding="0">
									<tr>
										<td>
											<div class="formElement_row" style="float: right;">
												<c:if test="${!empty dataExportToolBean.itemsList }">
													<input style="width: 70px;" type="button" name="DATAEXPTOOL_Schedule" id="DATAEXPTOOL_Schedule"   class="bluebutton cursorPointer" onclick="dataexptool_Submit();" value="<fmt:message key="label.schedule"/>" /> 
												</c:if>
												<input style="margin-left: -15px;width: 70px;margin-right: 60px;" type="button" name="DATAEXPTOOL_Reset" id="DATAEXPTOOL_Reset" onclick="dataexptool_reset();" class="gear_button cursorPointer"  value="<fmt:message key="label.reset"/>" />
												<div class="cb"></div>
											</div>
										</td>
									</tr>
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
		<strong><fmt:message key="label.rmitool.reset"/></strong>
	</p>
</div>
<div id="schedule_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>&nbsp;</p>
	<p align="center">
		<strong id="DATAEXPTOOL_SCH_ConversionMessage"></strong>
	</p>
</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<c:if test="${!empty dataExportToolBean.scheduleName }">
	<script type="text/javascript">
		var msg = "<c:out value="${dataExportToolBean.successMessage }"/>";
		dataexptool_showConfirmation(msg);
		
		function dataexptool_showConfirmation(message)
		{
			// open dialog
			$("#DATAEXPTOOL_SCH_ConversionMessage").html(message);
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
				          url =url+"<%=request.getContextPath()%>/mnaodataexporthistory";
				          window.location.href=url;
				        }  
				  	}
			    });
		}
	</script>	
</c:if>




<c:if test="${!empty dataExportToolBean.itemsList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty dataExportToolBean.itemsList }">
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
	$( "#DATAEXPTOOL_FromDate" ).datepicker({
		dateFormat: 'yy-mm-dd',
		changeYear:true,
		changeMonth:true,
		showButtonPanel: true
	});
	$( "#DATAEXPTOOL_ToDate" ).datepicker({
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

$("#DATAEXPTOOL_CountryId").on('click', 'option', function() {
	if ($("#DATAEXPTOOL_CountryId option:selected").length > 5) {
		$(this).removeAttr("selected");
		 alert('You can select upto 5 options only.');
	}
	else
	{
		$("#loader").show();
		$("#DATAEXPTOOL_ActionClicked").val("COUNTRY_SELECTION");
		DATAEXPTOOL_checkBoxManagement();
		DATAEXPTOOL_readDataTableValues();
		$("#DATAEXPTOOL_Form").submit();	
	}
});

/*
$("#DATAEXPTOOL_ModelId").on('click', 'option', function() {
	if ($("#DATAEXPTOOL_ModelId option:selected").length > 1) {
		$(this).removeAttr("selected");
	}
});
*/

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
                        { aTargets: [ 6 ], bSortable: false }
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
	var pageNo=$("#DATAEXPTOOL_DataTabel_displayPageNo").val();
	var pageLength=$("#DATAEXPTOOL_DataTabel_displayPageLen").val();
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
		DATAEXPTOOL_checkBoxManagement();
	});
}

function DATAEXPTOOL_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#DATAEXPTOOL_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#DATAEXPTOOL_DataTabel_displayPageLen").val(length);	
	}	
	
}


function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_DATAEXPTOOL_Error_Message").html(html);
	window.scrollTo(0,0);
}

function DATAEXPTOOL_checkBoxManagement()
{
	$("#DATAEXPTOOL_SelectedRows").val("");
	var value=$("#DATAEXPTOOL_SelectedRows").val();
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
		$("#DATAEXPTOOL_SelectedRows").val(result);
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
		$("#DATAEXPTOOL_SelectedRows").val("");
	}
	DATAEXPTOOL_checkBoxManagement();
}

function dataexptool_languageSelection()
{
	$("#loader").show();
	$("#DATAEXPTOOL_ActionClicked").val("LANGUAGE_SELECTION");
	DATAEXPTOOL_checkBoxManagement();
	DATAEXPTOOL_readDataTableValues();
	
	/*
		check here, if All is selected, then select all options in the List box
	*/
	var selEng=$("#DATAEXPTOOL_LanguageId").val();
	if(null!=selEng && selEng!="")
	{
		var allString = "1000000";
		if(selEng.indexOf(allString)!=-1)
		{
			// contains All - set all Items as Selected
			$("#DATAEXPTOOL_LanguageId").find("option").prop("selected", true);
		}
	}
	$("#DATAEXPTOOL_Form").submit();	
}

function dataexptool_modelSelection(thisObj)
{
	var val = $(thisObj).val();
	$("#DATAEXPTOOL_ModelId").find("option").each(function() {
		// disable all options first - 
		$(this).prop("selected", false);
	});
	
	$("#DATAEXPTOOL_ModelId").find("option").each(function() {
		if ($(this).attr('value')==val) {
			// enable current selected option
			$(this).prop("selected", true);
		}
	});	
}

//performs Operation Type Change Submit
function dataexptool_optype(thisObj)
{
	$("#loader").show();
	$("#DATAEXPTOOL_DataExtractionTypeId").val($(thisObj).val());
	DATAEXPTOOL_checkBoxManagement();
	DATAEXPTOOL_readDataTableValues();
	$("#DATAEXPTOOL_Form").submit();
}


function dataexptool_Go()
{
	$("#loader").show();
	$("#DATAEXPTOOL_ActionClicked").val("SUBMIT");
	DATAEXPTOOL_checkBoxManagement();
	DATAEXPTOOL_readDataTableValues();
	$("#DATAEXPTOOL_Form").submit();
}

function dataexptool_reset()
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
				 $("#DATAEXPTOOL_ActionClicked").val("RESET");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#DATAEXPTOOL_Form").submit();
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

function dataexptool_Submit()
{
	DATAEXPTOOL_checkBoxManagement();
	var selRows = $("#DATAEXPTOOL_SelectedRows").val();
	if(null!=selRows && selRows!="")
	{
		$("#loader").show();
		$("#DATAEXPTOOL_ActionClicked").val("SCHEDULE");
		DATAEXPTOOL_checkBoxManagement();
		DATAEXPTOOL_readDataTableValues();
		$("#DATAEXPTOOL_Form").submit();
	}
	else
	{
		$("#MDM_DATAEXPTOOL_Error_Message").html("");
		var message="<div class=\"errorMessage\" id=\"MDM_ERROR_MESSAGE\">";
		message = message+"<fmt:message key="error.select.checkbox" />";
		message = message+"</div>";
		message = message+"<div class=\"cb\"></div>";
		$("#MDM_DATAEXPTOOL_Error_Message").html(message);
		window.scrollTo(0,0);
		message=null;
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
						 DATAEXPTOOL_readDataTableValues();
						 $("#DATAEXPTOOL_ActionClicked").val("RESET");
						 $("#DATAEXPTOOL_Form").submit();
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

