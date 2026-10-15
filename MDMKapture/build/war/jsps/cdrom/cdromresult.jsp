<%@page import="com.mazda.gms3.mdm.vo.AccessManagementInterface"%>
<%@ page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.CD_CREATION" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.cdcreation"/> &rsaquo;</a> 
            </div>
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<div class="managerSettings">
                        	<table width="100%">
                        		<tr>
										<td>
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
											<form id="CD_Rom_SchForm" name="CD_Rom_SchForm"  method="post" action="<%=request.getContextPath() %>/cdromschedule">
							              	<input type="hidden" name="CD_Rom_ActionClicked" id="CD_Rom_ActionClicked" value="<c:out value="${CDRomSearchBean.actionClicked }"/>">
											<div>
												<h3>
													<fmt:message key="label.dash.cdcreation" />
												</h3>
												<p>
													<fmt:message key="label.cdrom.cdromresult.heading1" />
													<fmt:message key="label.cdrom.cdromresult.heading2" />
												</p>
											</div>
											<p style="height:45px;">&nbsp;</p>
											<c:if test="${!empty CDRomSearchBean.itemsList }">
											<p class="warningMessage">
												<fmt:message key="label.cdrom.result.page.warning" />
											</p>
											</c:if>
											<table id="example" class="display historyTable" cellspacing="0" width="100%">
												<thead>
													<tr>
														<th scope="col" width="5%">#.</th>
														<th scope="col" width="7%"><fmt:message key="label.language"/></th>
														<th scope="col" width="10%"><fmt:message key="label.model"/></th>
														<th scope="col" width="8%"><fmt:message key="label.wmi"/></th>
														<th scope="col" width="10%"><fmt:message key="label.vds"/></th>
														<th scope="col" width="10%"><fmt:message key="label.visstartrange"/></th>
														<th scope="col" width="15%"><fmt:message key="label.contenttype"/></th>
														<th scope="col" width="15%"><fmt:message key="label.manualtypename"/></th>
														<th scope="col" width="20%"><fmt:message key="label.totaldocsforprocessing"/></th>
													</tr>
												</thead>
												<tbody>
													<% int count=0; %>
													<c:if test="${!empty CDRomSearchBean.itemsList }">
														<c:forEach var="itemsList" items="${CDRomSearchBean.itemsList }">
															<% count++; %>
															
															<tr>
																<td><%=count %></td>
																<td><c:out value="${CDRomSearchBean.selectedLanguage}"></c:out> </td>
																<td>
																	<c:forEach var="modelList" items="${CDRomSearchBean.models}">
																		<c:if test="${modelList.key == CDRomSearchBean.selectedModel}">
																			<c:out value="${modelList.value}" />
																		</c:if>
																	</c:forEach>
																</td>
																<td><c:out value="${CDRomSearchBean.selectedWmi}"/></td>
																<td><c:out value="${CDRomSearchBean.selectedVds}"/></td>
																<td><c:out value="${CDRomSearchBean.selectedVinRange}"/> </td>
																<td><c:out value="${itemsList.manualTypeDisplayLabel }"/> </td>
																<td><c:out value="${itemsList.manualTypeName }"/> </td>
																<td><c:out value="${itemsList.totalDocsForProcessing }"/> </td>
															</tr>
														</c:forEach>
													</c:if>
												</tbody>
											</table>
											<div style="text-align: right;">
												<br>
												<input type="button" class="gear_button cursorPointer" value="<fmt:message key="label.back"/>" name="back" id="back" onclick="crom_schBack();">
												<input type="button" class="bluebutton cursorPointer" value="<fmt:message key="label.submit"/>" name="next" id="next">
											</div> 
										</form>
										</td>
									</tr>
							</table>
							</div>
						</div>
					</div>
				</div>						
</div>
</div>
<div id="dialog" style="display: none">
    <fmt:message key="label.popup.cdrom.message"/><br><br><br>
    <hr><br>
    <input type="button" style="float:right;" class="bluebutton cursorPointer" value="<fmt:message key="label.popup.cdrom.button.schedule"/>" name="yes" id="yes">
    <input type="button" style="float:right;" class="gear_button cursorPointer" value="<fmt:message key="label.popup.cdrom.button.cancel"/>" name="no" id="no">
     
</div>
</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<script type="text/javascript">
var htmlContent="";
</script>

<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});

$(document).ready(function() {
	loadDataTable();
	//$("body").unmask();
} );

function crom_schBack()
{
	$("#CD_Rom_ActionClicked").val("BACK_TO_DETAIL_PAGE");
	$("#CD_Rom_SchForm").submit();	
}


function loadDataTable()
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
                        { aTargets: [ 7 ], bSortable: true }
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
}




</script>
<script type="text/javascript">
    $(function () {
        $("#dialog").dialog({
            modal: true,
            autoOpen: false,
            title: '<fmt:message key="label.popup.cdrom.button.schedule"/>',
            width: 300,
            height: 150
        });
        $("#next").click(function () {
            $('#dialog').dialog('open');
        });
    });
    
    $('#no').click(function() {
    	$('#dialog').dialog('close');
    });

    $('#yes').click(function() {
    	$('#yes').prop('disabled', true);
    	$('#dialog').dialog('close');
    	// show Loader before submit
		$("#loader").show();
    	//location.href=applicationContext+"<request.getContextPath() %>/cdromschedule";
		$("#CD_Rom_ActionClicked").val("SCHEDULE_JOB");
		$("#CD_Rom_SchForm").submit();	
    });

</script>

</html>