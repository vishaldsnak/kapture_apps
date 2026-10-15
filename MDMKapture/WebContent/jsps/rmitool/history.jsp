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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_RMI_TOOL_HISTORY);
%>


<%
	String applicationContext=ApplicationProperties.getProperty("application.environment.context");	
//	String icWebContext = ApplicationProperties.getProperty("application.infocenter.web.context");
	String reportsWebPath = ApplicationProperties.getProperty("rmitool.reports.web.path");
	String rmiWorkingDirWebPath = ApplicationProperties.getProperty("rmitool.working.dir.web.path");
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
<jsp:useBean id="rmiToolHistorySessionBean" class="com.mazda.gms3.mdm.rmitool.bean.RMIExportToolHistorySessionBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.RMI_TOOL_HISTORY"/> </title>
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
#view_item_details-dialog
{
	height: auto !important;
}
#view_summary_details-dialog
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.RMI_TOOL_HISTORY"/> &rsaquo;</a> 
           	</div>
           	
            <form id="RMITOOLHISTORY_Form" name="RMITOOLHISTORY_Form" action="<%=request.getContextPath() %>/rmitoolhistory" method="post">
			<input type="hidden" name="RMITOOLHISTORY_ActionClicked" id="RMITOOLHISTORY_ActionClicked" value="" />
			<input type="hidden" name="RMITOOLHISTORY_AbortScheduleId" id="RMITOOLHISTORY_AbortScheduleId" value="" />
			<input type="hidden" name="RMITOOLHISTORY_displayPageNo" id="RMITOOLHISTORY_displayPageNo" value="<c:out value="${rmiToolHistorySessionBean.displayPageNo }"/>" />
			<input type="hidden" name="RMITOOLHISTORY_displayPageLen" id="RMITOOLHISTORY_displayPageLen" value="<c:out value="${rmiToolHistorySessionBean.displayPageLength }"/>" /> 
            
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
											<th scope="col" width="9%"><fmt:message key="label.schedulename"/></th>
											<th scope="col" width="9%"><fmt:message key="label.scheduledby"/></th>
											<th scope="col" width="8%"><fmt:message key="label.scheduletime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.finishtime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.documents" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="10%"><fmt:message key="label.okassets" /> - ( <fmt:message key="label.total"/> / <fmt:message key="label.processed"/> / <fmt:message key="label.failed"/> ) </th>
											<th scope="col" width="8%"><fmt:message key="label.content.export.progress"/></th>
											<th scope="col" width="8%"><fmt:message key="label.job" /> <fmt:message key="label.status" /></th>
											<th scope="col" width="6%"><fmt:message key="label.download.xmls" /></th>
											<th scope="col" width="6%"><fmt:message key="label.download" /> <fmt:message key="label.reports" /></th>
											<th scope="col" width="6%"><fmt:message key="label.job" /> <fmt:message key="label.summary" /></th>
											<th scope="col" width="9%"><fmt:message key="label.remarks" /></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty rmiToolHistorySessionBean.transationsList }">
											<c:forEach var="transationsList" items="${rmiToolHistorySessionBean.transationsList }">
												<tr>
													<td><c:out value="${transationsList.srNo }"/> </td>
													<td><a href="javascript:void(0);" onclick="rmitoolhistory_viewItemDetails('<c:out value="${transationsList.scheduleId }"/>');"><c:out value="${transationsList.scheduleName }"/></a></td>
													<td><c:out value="${transationsList.wslId }"/></td>
													<td><fmt:formatDate value="${transationsList.startTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><fmt:formatDate value="${transationsList.finishTime}"  pattern="dd MMM yyyy HH:mm:ss"/></td>
													<td><c:out value="${transationsList.totalDocsCount }"/> / <c:out value="${transationsList.successDocsCount }"/> / <c:out value="${transationsList.failureDocsCount }"/></td>
													<td><c:out value="${transationsList.totalOkAssetsCount }"/> / <c:out value="${transationsList.successOkAssetsCount }"/> / <c:out value="${transationsList.failureOkAssetsCount }"/></td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.processingStatus && (transationsList.processingStatus eq 'Pending' || transationsList.processingStatus eq 'Processing') }">
																<a href="javascript:void(0);" onclick="rmitoolhistory_abortSchedule('<c:out value="${transationsList.scheduleId }"/>');"><c:out value="${transationsList.processingStatus }"/></a>
															</c:when>
															<c:otherwise><c:out value="${transationsList.processingStatus }"/></c:otherwise>
														</c:choose>
													 </td>
													<td><c:out value="${transationsList.completionStatus }"/></td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.processingStatus && transationsList.processingStatus eq 'Completed' }">
																<a href="javascript:void(0);" target="_blank" style="text-decoration:none;" onclick="rmitoolhistory_downloadXMLS('<c:out value="${transationsList.scheduleId }"/>');"><i class="downloadReports"></i></a>
															</c:when>
														</c:choose>
													</td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.processingStatus && transationsList.processingStatus eq 'Completed' }">
																<a href="javascript:void(0);" target="_blank" style="text-decoration:none;" onclick="rmitoolhistory_downloadReports('<c:out value="${transationsList.scheduleId }"/>');"><i class="downloadReports"></i></a>
															</c:when>
														</c:choose>
													</td>
													<td>
														<c:choose>
															<c:when test="${!empty transationsList.processingStatus && transationsList.processingStatus eq 'Completed' }">
																<a href="javascript:void(0);" style="text-decoration:none;" onclick="rmitoolhistory_viewSummary('<c:out value="${transationsList.scheduleId }"/>');"><i class="summaryIcon"></i></a>
															</c:when>
														</c:choose>
													</td>
													<td><c:out value="${transationsList.reportsPath }"/></td>
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
       
<div id="abort_dialog-confirm" title="<fmt:message key="label.abortschedule"/>" class="hide">
	<p>
		<fmt:message key="label.abortscheduledescription"/>
	</p>
</div>        
</div>

<div id="view_item_details-dialog" title="<fmt:message key="label.item"/> <fmt:message key="label.details"/>" class="hide">

</div>

<div id="view_summary_details-dialog" title="<fmt:message key="label.reports"/> <fmt:message key="label.summary"/> <fmt:message key="label.details"/>" class="hide">

</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


var runReloadScript="<c:out value="${rmiToolHistorySessionBean.runReloadScript }" />";

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
                        { aTargets: [ 9 ], bSortable: false },
                        { aTargets: [ 10 ], bSortable: false },
                        { aTargets: [ 11 ], bSortable: false },
                        { aTargets: [ 12 ], bSortable: true }
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
	
	var table = $('#example').DataTable();
	var pageNo=$("#RMITOOLHISTORY_displayPageNo").val();
	var pageLength=$("#RMITOOLHISTORY_displayPageLen").val();
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

function rmitoolhistory_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	$("#RMITOOLHISTORY_displayPageNo").val(info.page);
	$("#RMITOOLHISTORY_displayPageLen").val(length);
}

function rmitoolhistory_downloadReports(scheduleCode)
{
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	var reportsWebpath = "<%=reportsWebPath %>";
	var zipExt = "<%=ApplicationProperties.getProperty("REPORTS_ZIP_SUFFIX") %>";
	schReports =schReports+reportsWebpath+scheduleCode+"/"+scheduleCode+"_"+zipExt; 
	
	window.open(schReports);
}

function rmitoolhistory_downloadXMLS(scheduleCode)
{
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	var rmiWorkingDirWebPath = "<%=rmiWorkingDirWebPath %>";
	schReports =schReports+rmiWorkingDirWebPath+scheduleCode+"/"+scheduleCode+"_XMLS.zip"; 
	
	window.open(schReports);
}



function rmitoolhistory_abortScheduleAction(scheduleId)
{
	rmitoolhistory_readDataTableValues();
	$("#RMITOOLHISTORY_ActionClicked").val("ABORT_SCHEDULE");
	$("#RMITOOLHISTORY_AbortScheduleId").val(scheduleId);
	$("#RMITOOLHISTORY_Form").submit();
}

function rmitoolhistory_abortSchedule(scheduleId)
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
	          rmitoolhistory_abortScheduleAction(scheduleId);
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



function rmitoolhistory_viewItemDetails(scheduleCode)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/rmitoolviewdetails"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			RMITOOLHIST_VIEW_ITEM_DETAILS:"RMITOOLHIST_VIEW_ITEM_DETAILS",
			RMITOOLHIST_SCHEDULE_CODE:scheduleCode
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
						$( "#view_item_details-dialog" ).html(responseString);	
						
						// open dialog
						$( "#view_item_details-dialog" ).removeClass('hide').dialog({
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
						//$("#view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
						$("#view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button	
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

function rmitoolhistory_viewSummary(scheduleCode)
{
	// call function to check whether the reports exists for the Schedule Code or not
	
	// $("body").mask("Please wait...");
	var schReports="";
	var appContext = "<%=applicationContext %>";
	if(null!=appContext && appContext!="" && appContext!="null")
	{
		schReports=appContext;
	}	
	schReports =schReports+"<%=request.getContextPath() %>/rmitoolviewdetails"; 
	// Make Ajax Call
	$.ajax({
		url:schReports,
		type:"post",
		data:{
			RMITOOLHIST_VIEW_SUMMARY_DETAILS:"RMITOOLHIST_VIEW_SUMMARY_DETAILS",
			RMITOOLHIST_SCHEDULE_CODE:scheduleCode
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
						//$("#view_item_details-dialog").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
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
						rmitoolhistory_readDataTableValues();
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