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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SCH_MAIN_VIN_MAPPING);
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
<jsp:useBean id="schMaintenanceVINMappingBean" class="com.mazda.gms3.mdm.bean.SchMaintenanceVINMappingBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.sch.maintenance.vin.mapping"/> </title>
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
             <jsp:include page="topMenu.jsp" flush="true" />
			<!-- TOP MENU ENDS -->
            
            <div class="breadcumbs">
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.sch.maintenance.vin.mapping"/> &rsaquo;</a>   
            </div>
            <form id="SCHVIN_Form" name="SCHVIN_Form" action="<%=request.getContextPath() %>/schMainVinMapping" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="SCHVIN_SelectedRows" id="SCHVIN_SelectedRows" value="<c:out value="${schMaintenanceVINMappingBean.selectedRows }"/>" />
            <input type="hidden" name="SCHVIN_ActionClicked" id="SCHVIN_ActionClicked" value="<c:out value="${schMaintenanceVINMappingBean.actionClicked }"/>" />
			<input type="hidden" name="SCHVIN_UpdatedRows" id="SCHVIN_UpdatedRows" value="" />
			<input type="hidden" name="SCHVIN_DataTabel_displayPageNo" id="SCHVIN_DataTabel_displayPageNo" value="<c:out value="${schMaintenanceVINMappingBean.displayPageNo }"/>" />
			<input type="hidden" name="SCHVIN_DataTabel_displayPageLen" id="SCHVIN_DataTabel_displayPageLen" value="<c:out value="${schMaintenanceVINMappingBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td  id="MDM_SCHVIN_Error_Message" colspan="3">
											<c:if test="${!empty schMaintenanceVINMappingBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=schMaintenanceVINMappingBean.getErrorMessage() && !"".equals(schMaintenanceVINMappingBean.getErrorMessage()))
														{
															String[] msgs = schMaintenanceVINMappingBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty schMaintenanceVINMappingBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${schMaintenanceVINMappingBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty schMaintenanceVINMappingBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${schMaintenanceVINMappingBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
								</table>
								
								<c:if test="${schMaintenanceVINMappingBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.sch.maintenance.vin.mapping"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 350px;"><fmt:message key="label.upload" /> <fmt:message key="label.sch.maintenance.vin.mapping"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span> <a href="<%=request.getContextPath()%>/templates/SCH_MAIN_VIN_Mapping_Import_Template.zip" target="_blank" class="warningMessage" style="padding-left: 30px;padding-top: 4px;"><fmt:message key="label.download" /> <fmt:message key="label.template" /></a></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="SCHVIN_File" name="SCHVIN_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SCHVIN_file_upload();" id="SCHVIN_Upload" name="SCHVIN_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								
								<p style="height: 45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.sch.maintenance.vin.mapping"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty schMaintenanceVINMappingBean.schVinMappingList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<tr>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="6%"><fmt:message key="label.locale"/></th>
											<th scope="col" width="7%"><fmt:message key="label.model"/></th>
											<th scope="col" width="7%"><fmt:message key="label.carcode"/></th>
											<th scope="col" width="7%"><fmt:message key="label.wmicode"/></th>
											<th scope="col" width="7%"><fmt:message key="label.vdscode"/></th>
											<th scope="col" width="7%"><fmt:message key="label.visstartrange"/></th>
											<th scope="col" width="7%"><fmt:message key="label.visendrange"/></th>
											<th scope="col" width="7%"><fmt:message key="label.documentid"/></th>
											<th scope="col" width="12%"><fmt:message key="label.documenttitle"/></th>
											<th scope="col" width="7%"><fmt:message key="label.sourcefilename"/></th>
											<th scope="col" width="11%"><fmt:message key="label.sourcefilepath"/></th>
											<th scope="col" width="6%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="6%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty schMaintenanceVINMappingBean.schVinMappingList }">
											<c:forEach var="schVinMappingList" items="${schMaintenanceVINMappingBean.schVinMappingList }">
												<tr <c:if test="${schVinMappingList.editableFlag eq true  }"> class="selected"</c:if>>
													<td><c:out value="${schVinMappingList.srNo }" /> </td>
													<td>
														<label><c:out value="${schVinMappingList.locale }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.modelName }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.carlineCode }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.wmiCode }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.vdsCode }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.visStartRange }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.visEndRange }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.documentId }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.title }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.sourceFileName }" /></label>
													</td>
													<td>
														<label><c:out value="${schVinMappingList.sourceFilePath }" /></label>
													</td>
													<td>
														<fmt:formatDate value="${schVinMappingList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${schVinMappingList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty schMaintenanceVINMappingBean.schVinMappingList }">
									<table cellspacing="0"
											width="100%" class="noRecordTable">
											<tr>
												<td width="15%">&nbsp;</td>
												<td width="70%" class="noRecordText">
													<fmt:message key="label.norecord.table.message" />
												</td>
												<td width="15%" style="text-align:right;">
													<i class="noRecordIcon"><i>
												</td>
											</tr>
									</table>	
								</c:if>
							</div>
                        </div>
                    </div>
                </div>
            </div>
       </form>     
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<c:if test="${!empty schMaintenanceVINMappingBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${schMaintenanceVINMappingBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>

<script>
if(null!=document.getElementById("SCHVIN_File"))
{
	document.getElementById("SCHVIN_File").onchange = function () {
    	document.getElementById("uploadFile").value = this.value;
	};
}
</script>

<c:if test="${!empty schMaintenanceVINMappingBean.schVinMappingList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
var exportLabel = "<fmt:message key="label.export"/>";
htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"SCHVIN_export();\"  name=\"SCHVIN_Export\" id=\"SCHVIN_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
<c:if test="${empty schMaintenanceVINMappingBean.schVinMappingList }">
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
                        { aTargets: [ 8 ], bSortable: true },
                        { aTargets: [ 9 ], bSortable: true },
                        { aTargets: [ 10 ], bSortable: true },
                        { aTargets: [ 11 ], bSortable: true },
                        { aTargets: [ 12 ], bSortable: true },
                        { aTargets: [ 13 ], bSortable: true }
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
	var pageNo=$("#SCHVIN_DataTabel_displayPageNo").val();
	var pageLength=$("#SCHVIN_DataTabel_displayPageLen").val();
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

function SCHVIN_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#SCHVIN_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#SCHVIN_DataTabel_displayPageLen").val(length);	
	}	
	
}

function SCHVIN_export()
{
	$("#MDM_SCHVIN_Error_Message").html("");
	SCHVIN_readDataTableValues();
	var value=$("#SCHVIN_SelectedRows").val();
	// show Loader
	$("#loader").show();
	$("#SCHVIN_ActionClicked").val("EXPORT");
	$("#SCHVIN_Form").submit();
}


function SCHVIN_file_upload()
{
	// show Loader
	$("#loader").show();
	SCHVIN_readDataTableValues();
 	$("#SCHVIN_ActionClicked").val("FILE_UPLOAD");
	$("#SCHVIN_Form").submit();
}

function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_SCHVIN_Error_Message").html(html);
	window.scrollTo(0,0);
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
						 SCHVIN_readDataTableValues();
						 $("#SCHVIN_ActionClicked").val("RESET");
						 $("#SCHVIN_Form").submit();
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