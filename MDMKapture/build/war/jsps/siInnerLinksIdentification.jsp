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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SI_INNERLINKS_IDENTIFICATION_HISTORY);
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
<jsp:useBean id="siInnerLinksIdentificationHistoryBean" class="com.mazda.gms3.mdm.bean.SIInnerLinksIdentificationHistoryBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.SI_INNERLINKS_IDENTIFICATION_HISTORY"/> </title>
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
.ui-dialog{width:500px !important; height: auto !important;top: 100px !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

#mdm_reports_dialog-confirm
{
	
	height: auto !important;
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.SI_INNERLINKS_IDENTIFICATION_HISTORY"/> &rsaquo;</a> 
           	</div>
           	
            <form id="AXL_Form" name="AXL_Form" action="<%=request.getContextPath() %>/siinnerlinksidentificationhistory" method="get">
			
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<h4>
									<fmt:message key="label.executionhistory"/>
								</h4>
								<br />
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="15%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="11%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="11%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="15%"><fmt:message key="label.documents"/> - ( <fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="15%"><fmt:message key="label.sm"/> <fmt:message key="label.innerlinkscount"/> - ( <fmt:message key="label.total"/> / <fmt:message key="label.active"/> / <fmt:message key="label.inactive"/> ) </th>
											<th scope="col" width="12%"><fmt:message key="label.schedule" /> <fmt:message key="label.status" /></th>
											<th scope="col" width="13%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
											<th scope="col" width="5%"><fmt:message key="label.view" /> <fmt:message key="label.reports" /></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty siInnerLinksIdentificationHistoryBean.historyList }">
											<c:forEach var="historyList" items="${siInnerLinksIdentificationHistoryBean.historyList }">
												<tr>
													<td><c:out value="${historyList.srNo }"/> </td>
													<td><c:out value="${historyList.jobName }"/></td>
													<td><fmt:formatDate value="${historyList.startTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${historyList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${historyList.totalDocumentsCount }"/> / <c:out value="${historyList.processedDocumentsCount }"/> / <c:out value="${historyList.failureDocumentsCount }"/></td>
													<td>
														<c:out value="${historyList.totalInnerLinksCount }"/> / <c:out value="${historyList.activeInnerLinksCount }"/> / <c:out value="${historyList.inactiveInnerLinksCount }"/>
													</td>
													<td><c:out value="${historyList.processingStatus }"/> </td>
													<td><c:out value="${historyList.jobStatus }"/> </td>
													<td>
														<a href="javascript:void(0);" style="text-decoration:none;" onclick="sism_downloadReports('<c:out value="${historyList.jobId }"/>');"><i class="downloadReports"></i></a>
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

<div id="mdm_reports_dialog-confirm" title="<fmt:message key="label.reports" />" class="hide">
	<table width="100%" cellspacing="0" cellpadding="0" >
		<tr>
			<td>
				<table class="historyTable" cellspacing="0" width="100%" style="border-left:1px solid #DADADA;">
					<thead>
						<tr>
							<th scope="col" width="10%">#.</th>
							<th scope="col" width="65%" class="codeClass"><fmt:message key="label.locale"/></th>
							<th scope="col" width="25%" style="text-align: center;"><fmt:message key="label.reports" /></th>
						</tr>
					</thead>
					<tbody id="SISM_ReportsData">
						
					</tbody>
				</table>
			</td>
		</tr>
	</table>				
</div>


</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


var runReloadScript="<c:out value="${siInnerLinksIdentificationHistoryBean.runReloadScript }" />";

if(runReloadScript=="true")
{
	// reload page after every 30 seconds
	setTimeout(function()
	{
		window.location.reload(1);
	}, 120000);
}

$(document).ready(function() {
	loadDataTable();
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
                        { aTargets: [ 8 ], bSortable: false }
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

function sism_downloadReports(scheduleCode)
{
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/siinnerlinksidentificationhistory"; 
	
	var showNoRecord= true;
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			SISM_GetReports:"SISM_GetReports",
			SISM_ScheduleId:scheduleCode
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
						if(null!=responseString && responseString!="")
						{
							showNoRecord = false;
							$("#SISM_ReportsData").html(responseString);
						}
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
			//alert("error :: "+status);
		}
	});	
	
	if(showNoRecord==true)
	{
		$("#SISM_ReportsData").html("<tr><td colspan=\"3\"><fmt:message key="label.norecordsfound"/></td></tr>");
	}
	
	// open dialog
	$( "#mdm_reports_dialog-confirm" ).removeClass('hide').dialog({
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
	//$("#mdm_reports_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
	$("#mdm_reports_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
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
						axl_readDataTableValues();
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