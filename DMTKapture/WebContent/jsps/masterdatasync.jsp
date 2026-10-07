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
	String icContext=ApplicationProperties.getProperty("GMS3_INFOCENTER_WEB_CONTEXT");
	String locale=ApplicationProperties.getProperty("locales.values.english");
	Object localeObj=request.getSession().getAttribute("MD_SYNC_LS_Locale");
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
<jsp:useBean id="masterDataSyncBean" class="com.mazda.gms3.dmt.autosync.bean.MasterDataSyncBean" scope="session"></jsp:useBean>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><fmt:message key="label.title" /> - <fmt:message key="label.masterdatasynching"/> </title>
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

.categoryData tr:first-child td {
    border-top:0px !important;
}

.categoryData tr td:first-child
{
	border-left:0px !important;
}

.ui-dialog
{
	width:900px !important; 
	height:auto !important; 
	overflow-x:hidden !important;
	overflow-y:auto !important;
}

.ui-dialog .ui-dialog-title{color:#fff !important;}

#mdsync_pending_dialog-confirm
{
	height:auto !important;
}
#mdsync_completed_dialog-confirm
{
	height: auto !important;
}
#mdsync_view_item_details-dialog
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
					<li><a href="<%=request.getContextPath() %>/metadatavalidation"><fmt:message key="label.metadatavalidation"/></a></li>
					<li class="activeLink"><a href="<%=request.getContextPath() %>/mdsync"><fmt:message key="label.masterdatasynching" /> </a></li>
				</ul>
				<div class="cb"></div>
			</div>
            <!-- TOP MENU ENDS -->
            <form action="<%=request.getContextPath() %>/mdsync" id="MD_SYNC_Form" name="MD_SYNC_Form" method="post" accept-charset="UTF-8">
            	<input type="hidden" name="MD_SYNC_ActionClicked" id="MD_SYNC_ActionClicked" value="">
				<input type="hidden" name="MD_SYNC_DisplayPageNo" id="MD_SYNC_DisplayPageNo" value="<c:out value="${masterDataSyncBean.displayPageNo }"/>" />
				<input type="hidden" name="MD_SYNC_DisplayPageLength" id="MD_SYNC_DisplayPageLength" value="<c:out value="${masterDataSyncBean.displayPageLength }"/>" />
				<input type="hidden" name="MD_SYNC_DisplayPageTempNo" id="MD_SYNC_DisplayPageTempNo" value="<c:out value="${masterDataSyncBean.displayPageTempNo }"/>" />
				<input type="hidden" name="MD_SYNC_DisplayPageTempLength" id="MD_SYNC_DisplayPageTempLength" value="<c:out value="${masterDataSyncBean.displayPageTempLength }"/>" />
            	<input type="hidden" name="MD_SYNC_SelectedCategories" id="MD_SYNC_SelectedCategories" value="<c:out value="${masterDataSyncBean.selectedCategories }"/>">
            	<input type="hidden" name="MD_SYNC_ItemToBeDeleted" id="MD_SYNC_ItemToBeDeleted" value="" >
            	<input type="hidden" name="MD_SYNC_AbortThreadId" id="MD_SYNC_AbortThreadId" value="" >
            	<input type="hidden" name="MD_SYNC_AbortScheduleId" id="MD_SYNC_AbortScheduleId" value="">
            	<input type="hidden" name="MD_SYNC_Sch_SelectedRows" id="MD_SYNC_Sch_SelectedRows" value="">
            	<div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        		<table width="100%" cellspacing="1" cellpadding="3">
									<tr>
										<td id="MD_SYNC_TD_Error_Message" colspan="2">
											<c:if test="${!empty masterDataSyncBean.errorMessage }">
												<div class="errorMessage" id="MD_SYNC_ERROR_MESSAGE">
													<%
														if(null!=masterDataSyncBean.getErrorMessage() && !"".equals(masterDataSyncBean.getErrorMessage()))
														{
															String[] msgs = masterDataSyncBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty masterDataSyncBean.successMessage }">
												<div class="successMessage" id="MD_SYNC_ERROR_MESSAGE">
													<c:out value="${masterDataSyncBean.successMessage }" />
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
												<label style="margin-left:0px !important;"><fmt:message key="label.market"/>: <span class="mandatory">*</span></label> 
												<select id="MD_SYNC_MarketId" name="MD_SYNC_MarketId" style="width:300px;" onchange="mdsync_marketSelection();">
													<option value=""><fmt:message key="label.selectone" /> </option>
													<c:if test="${!empty masterDataSyncBean.marketList }">
														<c:forEach var="marketList" items="${ masterDataSyncBean.marketList }">
															<c:set var="selectedMarket" value=""/>
															<c:if test="${!empty masterDataSyncBean.marketId }">
																<c:if test="${masterDataSyncBean.marketId eq marketList.value}">
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
												<div class="cb"></div>
											</div>
										</td>
										<td width="50%">
											&nbsp;
										</td>
									</tr>
								</table>
								
								<c:if test="${!empty masterDataSyncBean.categoryTypesList }">
								<br />
								<h4 style="font-size:16px;">
									<fmt:message key="label.selectmasterdata" />
								</h4>
								<br />
								<p class="warningMessage">
									<fmt:message key="label.note" />:* <br/> 1.) <fmt:message key="label.masterdata.synching.info" /><br/>
									2.) <fmt:message key="label.masterdata.count.info" />
								</p>
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable categoryData">
									<c:if test="${!empty masterDataSyncBean.categoryTypesList }">
										<% int count=1; %>
										<tr style="background-color:#dadada;" >
											<td width="33%" style="text-align:left;padding-left:80px !important;border-left:1px solid #fff;border-top:1px solid #fff;border-bottom:1px solid #fff;">
										<c:forEach var="categoryTypesList" items="${masterDataSyncBean.categoryTypesList }" varStatus="types">
											
											<input type="checkbox" name="MD_Sync_CatTypes" id="MD_Sync_CatTypes_<c:out value="${types.index }"/>" 
												value="<c:out value="${categoryTypesList.value }"/>" 
												<c:if test="${categoryTypesList.value == 'ALL' }"> onchange="mdsync_allcatTypeCheckBoxOperation(this);" </c:if> 
												<c:if test="${categoryTypesList.value != 'ALL' }"> onchange="mdsync_catTypeCheckBoxOperation('<c:out value="${fn:length(masterDataSyncBean.categoryTypesList)}" />',this);" </c:if> 
												 /> <c:out value="${categoryTypesList.label }"></c:out> 
											<c:if test="${(types.index +1) % 3 ==0 }">
											<% // increment count - a new row is going to get added
												count++;
											%>
													</td>
												</tr>
												<tr <% if(count%2!=0) { %> style="background-color:#dadada;"  <% } %>>
													<td width="33%" style="text-align:left;padding-left:80px !important;border-left:1px solid #fff;border-top:1px solid #fff;border-bottom:1px solid #fff;">
											</c:if>
											<c:if test="${(types.index +1) % 3 !=0 }">
												</td><td width="33%" style="text-align:left;padding-left:80px !important;border-left:1px solid #fff;border-top:1px solid #fff;border-bottom:1px solid #fff;">
											</c:if>
										</c:forEach>
									</c:if>
									
									<tr>
										<td colspan="4" align="right" style="text-align:right">
											<input type="button" id="MD_SYNC_Add" class="bluebutton cursorPointer"  onclick="mdsync_add();" value="<fmt:message key="label.go" />"/>
										</td>
									</tr>
									<c:if test="${masterDataSyncBean.showItemsBlock eq true}">
									<tr>
										<td colspan="4">
											<p class="warningMessage">
												<fmt:message key="label.note" />: <fmt:message key="label.masterdata.count.info" />
											</p>
											<table id="example" class="display historyTable" cellspacing="0"
												width="100%">
												<thead>
													<tr>
														<th scope="col" width="10%">#.</th>
														<th scope="col" width="20%"><fmt:message key="label.market"/></th>
														<th scope="col" width="20%"><fmt:message key="label.masterdatatype"/></th>
														<th scope="col" width="30%"><fmt:message key="label.total"/> <fmt:message key="label.categories"/></th>
														<th scope="col" width="20%"><fmt:message key="label.delete"/></th>
													</tr>
												</thead>
												<tbody>
													<c:forEach var="itemsList" items="${masterDataSyncBean.itemsList }">
														<tr>
															<td><c:out value="${itemsList.srNo }"></c:out> </td>
															<td><c:out value="${itemsList.locale }"></c:out> </td>
															<td><c:out value="${itemsList.itemName }"></c:out> </td>
															<td><c:out value="${itemsList.totalCount }"></c:out> </td>
															<td>
																<a href="javascript:void(0);" onclick="mdsync_deleteItem('<c:out value="${itemsList.srNo }"/>');"><i class="deleteIcon"></i></a>
															</td>
														</tr>
													</c:forEach>
												</tbody>
											</table>
										</td>
									</tr>
									<tr>
										<td colspan="4" align="right" style="text-align:right">
											<input type="button" id="MD_SYNC_Reset" class="gear_button cursorPointer"  onclick="mdsync_reset();" value="<fmt:message key="label.reset" />"/>
											<c:if test="${!empty masterDataSyncBean.itemsList }">
												<input type="button" id="MD_SYNC_Schedule" class="bluebutton cursorPointer"  onclick="mdsync_conversion();" value="<fmt:message key="label.sync.master.data"  />"/>
											</c:if>
										</td>
									</tr>
									</c:if>
								</table>
								</c:if>
								
								
								<br/>
                       			<p style="height: 5px;"></p>
                       			<h4 style="font-size:16px;"><fmt:message key="label.masterdatasynching" /> <fmt:message key="label.schedule"/> </h4><br />
                       			<p class="warningMessage">
                       				<fmt:message key="label.show.viewall.validation.schedule.note" />
                       			</p>
                        		<table id="example1" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MD_SYNC_HeaderCheckBox" name="MD_SYNC_HeaderCheckBox" onclick="mdsync_sch_selectallrows(this);"/>
											</th>
											<th scope="col" width="5%">#.</th>
											<th scope="col" width="10%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="10%"><fmt:message key="label.scheduledby"/></th>
											<th scope="col" width="10%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.market"/></th>
											<th scope="col" width="15%"><fmt:message key="label.categories"/> (<fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/>)</th>
											<th scope="col" width="10%"><fmt:message key="label.job"/> <fmt:message key="label.status"/></th>
											<th scope="col" width="10%"><fmt:message key="label.schedule"/> <fmt:message key="label.status"/></th>
											<th scope="col" width="7%"><fmt:message key="label.reports"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty masterDataSyncBean.transactionsList }">
											<c:forEach var="transactionList" items="${masterDataSyncBean.transactionsList }">
												<tr <c:if test="${transactionList.rowSelected eq true  }">class="selected"</c:if>>
													<td>
														<c:if test="${!empty transactionList.jobStatus && (transactionList.jobStatus!='Pending' && transactionList.jobStatus!='Processing'  )}">
														
														<input type="checkbox" name="MD_SYNC_Selection" onchange="mdsync_sch_checkBoxManagement();" 
															value="<c:out value="${transactionList.scheduleId }" />" 
															id="MD_SYNC_Selection_<c:out value="${transactionList.scheduleId }"  />"
															<c:if test="${transactionList.rowSelected eq true  }">checked="checked"</c:if> />
														</c:if>	
													</td>
													<td><c:out value="${transactionList.srNo }"/> </td>
													<td><a href="javascript:void(0);" onclick="mdsync_viewItemDetails('<c:out value="${transactionList.scheduleId }"/>');"><c:out value="${transactionList.scheduleName }"/></a></td>
													<td><c:out value="${transactionList.userId }"/> </td>
													<td><fmt:formatDate value="${transactionList.scheduleTime }" pattern="dd MMM yyyy HH:mm:ss" /> </td>
													<td><fmt:formatDate value="${transactionList.finishTime }" pattern="dd MMM yyyy HH:mm:ss" /> </td>
													<td><c:out value="${transactionList.market }"/> </td>
													<td><c:out value="${transactionList.totalCount }"/> / <c:out value="${transactionList.successCount }"/> / <c:out value="${transactionList.failureCount }"/> </td>
													<td>
														<c:if test="${!empty transactionList.jobStatus }">
															<c:choose>
																<c:when test="${transactionList.jobStatus == 'Pending' || transactionList.jobStatus == 'Processing' }">
																	<a href="javascript:void(0);" onclick="mdsync_abortSchedule('<c:out value="${transactionList.threadId }"/>','<c:out value="${transactionList.scheduleId }"/>');"><c:out value="${transactionList.jobStatus }"/></a>
																</c:when>
																<c:otherwise>
																	<c:out value="${transactionList.jobStatus }"></c:out>
																</c:otherwise>
															</c:choose>
														</c:if>
													</td>
													<td><c:out value="${transactionList.scheduleStatus }"></c:out> </td>
													<td>
														<a href="javascript:void(0);" onclick="mdsync_downloadReports('<c:out value="${transactionList.jobStatus }"/>','<c:out value="${transactionList.scheduleId }"/>');" style="text-decoration:none;"><i class="downloadReports"></i></a>
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
		<fmt:message key="label.reset.masterdatasync.description"/>
	</p>
</div>
<div id="delete_dialog-confirm" title="<fmt:message key="label.delete"/> <fmt:message key="label.item"/>" class="hide">
	<p>
		<fmt:message key="label.delete.masterdatasync.description"/>
	</p>
</div>

<div id="delete_schedule_dialog-confirm" title="<fmt:message key="label.delete"/> <fmt:message key="label.schedule"/>" class="hide">
	<p>
		<fmt:message key="label.delete.masterdatasync.schedule.description"/>
	</p>
</div>


<div id="mdsync_pending_dialog-confirm" title="<fmt:message key="label.view" /> <fmt:message key="label.reports" />" class="hide">
	<p>
		<strong><fmt:message key="label.pending.dialog.info.start" /> <u style="color:#D0021B" id="MD_SYNC_STATUS_LABEL"></u>. <fmt:message key="label.masterdatasync.pending.dialog.info.end" /> </strong>
	</p>
	<br/>
	<br/>
	<!-- 
	<p style="text-align: justify;font-size: 11px; !important">
		<b><u><fmt:message key="label.note" /> :</u></b> <fmt:message key="label.note.info"/>
	</p> -->
</div>


<div id="mdsync_completed_dialog-confirm" title="<fmt:message key="label.reports" />" class="hide">

</div>

<div id="mdsync_view_item_details-dialog" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="abort_dialog-confirm" title="<fmt:message key="label.abortschedule"/>" class="hide">
	<p>
		<fmt:message key="label.abort.masterdata.schedule"/>
	</p>
</div>
<div id="scheduleconfirmation_dialog-confirm" title="<fmt:message key="label.schedule"/>" class="hide">
	<p>
		<strong><fmt:message key="label.confirmation.masterdata.schedule"/></strong>
	</p>
</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true"/>
<!-- FOOTER ENDS -->


<c:if test="${!empty masterDataSyncBean.successMessage }">
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

var runReloadScript="<c:out value="${masterDataSyncBean.reloadJSP }" />";

if(runReloadScript=="true")
{
	// reload page after every 2 minutes  
	setTimeout(function()
	{
		mdsync_readDataTableValues();
		mdsync_readScheduleDataTableValues();
		$("#MD_SYNC_ActionClicked").val("REFRESH_ITEMS");
	    $("#MD_SYNC_Form").submit();
	}, 120000);
}
</script>

<script type="text/javascript">
var htmlContent="";	
var htmlScheduleContent="";
</script>

<c:if test="${!empty masterDataSyncBean.itemsList }">
<script type="text/javascript">
htmlContent="<div class=\"separator\"></div>";	
</script>
</c:if>


<c:if test="${!empty masterDataSyncBean.transactionsList}">
<script type="text/javascript">
var refreshLabel="<fmt:message key="label.refresh"/>";
var deleteLabel="<fmt:message key="label.delete"/>";
// add separator when transactionsList is not empty
htmlScheduleContent="<div class=\"separator\"></div>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"mdsync_refresh();\"  name=\"MD_SYNC_Refresh\" id=\"MD_SYNC_Refresh\" class=\"bluebutton cursorPointer\"><i class=\"refreshIcon\"></i> "+refreshLabel+"</button>";
htmlScheduleContent=htmlScheduleContent+"<button type=\"button\" style=\"float:right\" onclick=\"mdsync_deleteSchedule();\" name=\"MD_SYNC_Sch_Delete\" id=\"MD_SYNC_Sch_Delete\" class=\"bluebutton cursorPointer\"><i class=\"deleteScheduleIcon\"></i> "+deleteLabel+"</button>";
refreshLabel = null;
deleteLabel = null;
</script>
</c:if>

<script type="text/javascript">
$(document).ready(function() {
	loadDataTable();
	loadScheduleDataTable();
	mdsync_populateCategoryTypesOnLoad();
});

function loadDataTable()
{
	<c:if test="${masterDataSyncBean.showItemsBlock eq true }">
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
	var pageNo=$("#MD_SYNC_DisplayPageTempNo").val();
	var pageLength=$("#MD_SYNC_DisplayPageTempLength").val();
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

function mdsync_readDataTableValues()
{
	<c:if test="${masterDataSyncBean.showItemsBlock eq true }">
	var table = $('#example').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#MD_SYNC_DisplayPageTempNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#MD_SYNC_DisplayPageTempLength").val(length);	
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
                        { aTargets: [ 10 ], bSortable: false }
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
	var pageNo=$("#MD_SYNC_DisplayPageNo").val();
	var pageLength=$("#MD_SYNC_Schedule_displayPageLen").val();
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
		mdsync_sch_checkBoxManagement();
	});
}


function mdsync_readScheduleDataTableValues()
{
	var table = $('#example1').DataTable();
	if(null!=table && table!="undefined")
	{	
		var info = table.page.info();
		var length = table.page.len();
		// update data table display page & length
		if(null!=info)
		{
			$("#MD_SYNC_DisplayPageNo").val(info.page);	
		}	
		if(null!=length)
		{
			$("#MD_SYNC_DisplayPageLength").val(length);	
		}
	}
}

function mdsync_marketSelection()
{
	// show Loader
	$("#loader").show();
	mdsync_readDataTableValues();
	mdsync_readScheduleDataTableValues();
	$("#MD_SYNC_ActionClicked").val("MARKET_SELECTION");
	$("#MD_SYNC_Form").submit();
}

function mdsync_populateCategoryTypesOnLoad()
{
	// first set all checkBoxes selected to false
	$('input[name=MD_Sync_CatTypes]').each(function() {
		$(this).prop('checked', false);	
	});
	// Now set selected Ones as checked
	var selectedCheckBoxes="<c:out value="${masterDataSyncBean.selectedCategories }"/>";
	if(null!=selectedCheckBoxes && selectedCheckBoxes!="")
	{
		var tok = selectedCheckBoxes.split(",");
		if(null!=tok && tok.length>0)
		{
			for(var a=0;a<tok.length;a++)
			{
				$('input[name=MD_Sync_CatTypes]').each(function() {
					if($(this).val()== tok[a])
					{
						$(this).prop('checked', true);
					}	
				});
			}
		}
	}
}

function mdsync_allcatTypeCheckBoxOperation(thisObj)
{
	var selectedCheckBoxes="";
	$('input[name=MD_Sync_CatTypes]').each(function() {
		if($(thisObj).prop('checked')==true)
		{	
			$(this).prop('checked',true);
			// select all checkBoxes
			selectedCheckBoxes+=$(this).val()+",";
		}
		else
		{
			// un select all checkBoxes
			$(this).prop('checked',false);
		}	
	});
	
	// before setting check, if value ends with , then remove it from String
	var last = selectedCheckBoxes.substring(selectedCheckBoxes.length-1,selectedCheckBoxes.length);
	if(null!=last && last==",")
	{
		selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
	}
	/*
	if(selectedCheckBoxes.endsWith(","))
	{
		selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
	}*/
	$("#MD_SYNC_SelectedCategories").val(selectedCheckBoxes);
	
}

function mdsync_catTypeCheckBoxOperation(itemsLength, thisObj)
{
	// by default set all All checkBox SELECTED as false
	$("#MD_Sync_CatTypes_0").prop("checked",false);
	
	// set clicked checkBox value to true / false based on checkbox current event
	if($(thisObj).prop('checked')==true)
	{
		$(thisObj).prop('checked',true);
	}
	else
	{
		$(thisObj).prop('checked',false);
	}
	var selectedCheckBoxes="";
	$('input[name=MD_Sync_CatTypes]').each(function() {
		if($(this).prop('checked')==true)
		{
			selectedCheckBoxes+=$(this).val()+",";
		}	
	});
	
	// before setting check, if value ends with , then remove it from String
	var last = selectedCheckBoxes.substring(selectedCheckBoxes.length-1,selectedCheckBoxes.length);
	if(null!=last && last==",")
	{
		selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
	}
	/*
	if(selectedCheckBoxes.endsWith(","))
	{
		selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
	}*/
	
	// check here, if selectedCheckBoxes array Length == itemsLength -1, then set All as checked, else make it unchecked
	var tok = selectedCheckBoxes.split(",");
	if(null!=tok && tok.length>0)
	{
		if(tok.length== (itemsLength-1))
		{
			// update selectedCheckBoxes value
			$('input[name=MD_Sync_CatTypes]').each(function() {
				$(this).prop('checked',true);
				selectedCheckBoxes+=$(this).val()+",";	
			});
			
			// before setting check, if value ends with , then remove it from String
			var last1 = selectedCheckBoxes.substring(selectedCheckBoxes.length-1,selectedCheckBoxes.length);
			if(null!=last1 && last1==",")
			{
				selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
			}	
			/*			
			if(selectedCheckBoxes.endsWith(","))
			{
				selectedCheckBoxes = selectedCheckBoxes.substring(0, selectedCheckBoxes.length-1);
			}*/
		}
	}	
	$("#MD_SYNC_SelectedCategories").val(selectedCheckBoxes);
}

function mdsync_add()
{
	if(mdsync_add_Validate())
	{
		$("#loader").show();
	  	mdsync_readDataTableValues();
		mdsync_readScheduleDataTableValues();
		$("#MD_SYNC_ActionClicked").val("ADD_ITEMS");
		$("#MD_SYNC_Form").submit();
	}
}

function mdsync_add_Validate()
{
	var market = $("#MD_SYNC_MarketId").val();
	var selectedCheckBoxes=$("#MD_SYNC_SelectedCategories").val();
	if(null==market || market=="")
	{
		// SHOW ERROR MESSAGE
		mdsync_showErrorMessage("<fmt:message key="error.message.mandatory.fields" />");
		return false;
	}	
	if(null==selectedCheckBoxes || selectedCheckBoxes=="")
	{
		// SHOW ERROR MESSAGE
		mdsync_showErrorMessage("<fmt:message key="error.message.mandatory.select.one.item" />");
		return false;
	}
	return true;
	
}


function mdsync_showErrorMessage(errorMessage)
{
	$("#MD_SYNC_TD_Error_Message").html("");
	var message="<div class=\"errorMessage\" id=\"MD_SYNC_ERROR_MESSAGE\">";
	message = message+errorMessage;
	message = message+"</div>";
	message = message+"<div class=\"cb\"></div>";
	$("#MD_SYNC_TD_Error_Message").html(message);
	window.scrollTo(0,0);
	message=null;
}

function mdsync_deleteItem(srNo)
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
				  	mdsync_readDataTableValues();
					mdsync_readScheduleDataTableValues();
					$("#MD_SYNC_ItemToBeDeleted").val(srNo);
					$("#MD_SYNC_ActionClicked").val("DELETE_ITEM");
					$("#MD_SYNC_Form").submit();
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

function mdsync_reset()
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
	    		  mdsync_readDataTableValues();
	    		  mdsync_readScheduleDataTableValues();
				  $( this ).dialog( "close" );
		          $("#MD_SYNC_ActionClicked").val("RESET");
		          $("#MD_SYNC_Form").submit();
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

function mdsync_conversion()
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
	    		  	mdsync_readDataTableValues();
	    			mdsync_readScheduleDataTableValues();
	    			$("#MD_SYNC_ActionClicked").val("SYNC_ITEMS");
	    		    $("#MD_SYNC_Form").submit();	
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


function mdsync_refresh()
{
	mdsync_readDataTableValues();
	mdsync_readScheduleDataTableValues();
	$("#MD_SYNC_ActionClicked").val("REFRESH_ITEMS");
    $("#MD_SYNC_Form").submit();
}


function mdsync_downloadReports(status, scheduleCode)
{
	if(status=="Pending" || status=="Processing")
	{
		// set status Label
		$('#MD_SYNC_STATUS_LABEL').html(status);
		
		// open dialog
		$( "#mdsync_pending_dialog-confirm" ).removeClass('hide').dialog({
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
		//$("#mdsync_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#mdsync_pending_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
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
				MD_SYNC_VIEW_REPORTS:"MD_SYNC_VIEW_REPORTS",
				MD_SYNC_SCHEDULE_CODE:scheduleCode,
				MD_SYNC_SCHEDULE_STATUS:status
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
						if(token=="DATA_WITH_URL")
						{
							// call open reports URL
							mdsync_showReports(responseString);
						}	
						else if(token=="DATA")
						{
							// set the dialog HTML AND Open it
							$( "#mdsync_completed_dialog-confirm" ).html(responseString);	
							
							// open dialog
							$( "#mdsync_completed_dialog-confirm" ).removeClass('hide').dialog({
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
							//$("#mdsync_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
							$("#mdsync_completed_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
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

function mdsync_viewItemDetails(scheduleCode)
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
			MD_SYNC_VIEW_ITEM_DETAILS:"MD_SYNC_VIEW_ITEM_DETAILS",
			MD_SYNC_SCHEDULE_CODE:scheduleCode
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
						$( "#mdsync_view_item_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#mdsync_view_item_details-dialog" ).removeClass('hide').dialog({
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
						$("#mdsync_view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
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

function mdsync_abortSchedule(threadId, scheduleId)
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
		          $("#MD_SYNC_AbortThreadId").val(threadId);
		          $("#MD_SYNC_AbortScheduleId").val(scheduleId);
		          mdsync_readDataTableValues();
		          mdsync_readScheduleDataTableValues();
		      	  $("#MD_SYNC_ActionClicked").val("ABORT_SCHEDULE");
		          $("#MD_SYNC_Form").submit();
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

function mdsync_showReports(url)
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

function mdsync_sch_checkBoxManagement()
{
	$("#MD_SYNC_Sch_SelectedRows").val("");
	var value=$("#MDSYNC_Sch_SelectedRows").val();
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
		$("#MD_SYNC_Sch_SelectedRows").val(result);
	}
	/*
	check here if all rows are selected, then by default
	check - MD_SYNC_HeaderCheckBox
	*/
	$("#MD_SYNC_HeaderCheckBox").prop("checked",false);
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
			$("#MD_SYNC_HeaderCheckBox").prop("checked",true);
		}
	}	
}

function mdsync_sch_selectallrows(thisObj)
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
		$("#MD_SYNC_Sch_SelectedRows").val("");
	}
	mdsync_sch_checkBoxManagement();
}

function mdsync_deleteSchedule()
{
	$("#MD_SYNC_TD_Error_Message").html("");
	var value=$("#MD_SYNC_Sch_SelectedRows").val();
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
					  	mdsync_readDataTableValues();
					  	// do not read for schedule as table nees to be refreshed
						$("#MD_SYNC_ActionClicked").val("DELETE_SCHEDULE");
						$("#MD_SYNC_Form").submit();
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
		mdsync_showErrorMessage("<fmt:message key="error.select.onerow.delete" />");
	}
}


</script>
</html>