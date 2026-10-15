<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="com.webapp.mazda.assetstool.util.ApplicationPropertiesUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="ISO-8859-1"%>
<%
response.setHeader("Cache-Control","no-cache"); //Forces caches to obtain a new copy of the page from the origin server
response.setHeader("Cache-Control","no-store"); //Directs caches not to store the page under any circumstance
response.setDateHeader("Expires", 0); //Causes the proxy cache to see the page as "stale"
response.setHeader("Pragma","no-cache"); //HTTP 1.0 backward compatibility
Object obj = request.getHeader("iv-user");
if(null==obj || "".equals(obj))
{
	response.sendRedirect(request.getContextPath()+"/error");
}

 %>		
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Mazda Service Support Center</title>
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
.ui-dialog{width:650px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

</style>
<jsp:include page="headeragent.jsp" flush="true" />


<jsp:useBean id="manageAssetsBean"
	class="com.webapp.mazda.assetstool.bean.ManageAssetsBean" scope="session"></jsp:useBean>
	
	
</head>
<body>
<!-- loader Starts here  -->
<div class="ui-overlay" style="z-index:9999;position:absolute" id="loader">
	<div class="ui-widget-overlay"></div>
	<div class="ui-widget-shadow ui-corner-all" style="position: absolute; top: 310px; left: 504.5px; width: 300px; height: 37px;">
	</div>
	<div class="loadmask-msg ui-widget ui-widget-content ui-corner-all" style="text-align:center; position: absolute; padding: 10px; top: 310px; left: 504.5px; width:278px;"><div class="ui-overlay-loading">Please wait, content is loading...</div></div>
</div>
<!--- Loader Ends here -->
<form action="<%=request.getContextPath() %>/manageassets" method="post" enctype="multipart/form-data" name="MS3_Upload_Form" id="MS3_Upload_Form">
<input type="hidden" name="MS3_Action_Clicked" id="MS3_Action_Clicked" value="<c:out value="${manageAssetsBean.actionClicked }"/>">
<input type="hidden" name="MS3_SelectedRows" id="MS3_SelectedRows" value="<c:out value="${manageAssetsBean.selectedRows }"/>" />
<input type="hidden" name="MS3_DataTabel_displayPageNo" id="MS3_DataTabel_displayPageNo" value="<c:out value="${manageAssetsBean.displayPageNo }"/>" />
<input type="hidden" name="MS3_DataTabel_displayPageLen" id="MS3_DataTabel_displayPageLen" value="<c:out value="${manageAssetsBean.displayPageLength }"/>" />
			
<div id="mainWrapper">
	<!-- HEADER STARTS HERE -->
	<jsp:include page="languageSelector.jsp" flush="true" />
	<!-- HEADER ENDS HERE -->
		
	<!-- TOP MENU STARTS -->
		<jsp:include page="topMenu.jsp" flush="true" />
	<!-- TOP MENU ENDS -->
            
    <div class="breadcumbs">
    	<a href="javascript:void(0);">Manage Assets &rsaquo;</a> 
    </div>
    <!-- TOP MENU Ends -->     
	
	<!-- Content Block Starts Here -->
	<div class="padder">
		<div class="contentBox">
			<div class="convertInfobox">
				<div class="managerInfo">
					<div class="managerSettings">
						<table width="100%" cellspacing="1" cellpadding="3" >
							<tr>
								<td id="MS3_ERROR_MESSAGE">
									<c:if test="${!empty manageAssetsBean.infoMessage }">
										<div class="successMessage">
											<!-- <c:out value="${ manageAssetsBean.infoMessage }"/> -->
											<%=manageAssetsBean.getInfoMessage() %>
										</div>
									</c:if>
									<c:if test="${!empty manageAssetsBean.errorMessage }">
										<div class="errorMessage">
										<!-- 	<c:out escapeXml="true" value="${ manageAssetsBean.errorMessage }"/> -->
											<%=manageAssetsBean.getErrorMessage() %>
										</div>
									</c:if>
									
								</td>
							</tr>
							<tr>
								<td>&nbsp;</td>
							</tr>	
							<tr>
								<td>
									<h3>
										Upload Assets
									</h3>
									<br />
								</td>
							</tr>
							<tr>
								<td>
									<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
							
										<tr>
											<td width="25%">
												<div class="formElement_row leftpadding_none">
													<label>Channel: <span class="mandatory">*</span></label> 
												</div>	
												<div class="cb"></div>
											</td>
											<td  width="25%">
												<div class="formElement_row leftpadding_none">
													<select id="MS3_Channel_Name" name="MS3_Channel_Name" onchange="ms3_getLocales();" style="width:200px !important;">
														<option value="">Select One</option>
														<c:if test="${!empty manageAssetsBean.channelsList }">
															<c:forEach var="channelsList" items="${manageAssetsBean.channelsList }">
																<c:set var="selectedChannel" value="" />
																<c:if test="${!empty manageAssetsBean.channelName}">
																	<c:if
																		test="${manageAssetsBean.channelName eq channelsList.value}">
																		<c:set var="selectedChannel" value="1" />
																	</c:if>
																</c:if>
																<c:choose>
																	<c:when test="${selectedChannel eq 1}">
																		<c:set var="selectedChannel" value="0" />
																		<option value="<c:out value="${channelsList.value}"/>" selected="selected">
																			<c:out value="${channelsList.label}"></c:out>
																		</option>
																	</c:when>
																	<c:otherwise>
																		<option value="<c:out value="${channelsList.value}"/>">
																			<c:out value="${channelsList.label}"></c:out>
																		</option>
																	</c:otherwise>
																</c:choose>
															</c:forEach>
														</c:if>
													</select>
												</div>	
												<div class="cb"></div>
											</td>
											<td  width="25%">
												<c:if test="${ manageAssetsBean.showLocale eq true}">
													<div class="formElement_row leftpadding_none">
														<label>Locale: <span class="mandatory">*</span></label> 
													</div>	
													<div class="cb"></div>
												</c:if>
											</td>
											<td  width="25%">
												<c:if test="${ manageAssetsBean.showLocale eq true}">
													<div class="formElement_row leftpadding_none">
														<select id="MS3_Locale_Name" name="MS3_Locale_Name" style="width:200px !important;">
															<option value="">Select One</option>
															<c:if test="${!empty manageAssetsBean.localeList }">
																<c:forEach var="localeList" items="${manageAssetsBean.localeList }">
																	<c:set var="selectedLocale" value="" />
																	<c:if test="${!empty manageAssetsBean.localeName}">
																		<c:if
																			test="${manageAssetsBean.localeName eq localeList.value}">
																			<c:set var="selectedLocale" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedLocale eq 1}">
																			<c:set var="selectedLocale" value="0" />
																			<option value="<c:out value="${localeList.value}"/>" selected="selected">
																				<c:out value="${localeList.label}"></c:out>
																			</option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${localeList.value}"/>">
																				<c:out value="${localeList.label}"></c:out>
																			</option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</div>	
													<div class="cb"></div>
												</c:if>
											</td>
										</tr>
										<c:if test="${!empty manageAssetsBean.acceptableFilesTypesForSelectedChannel }">
										<tr>
											<td>
												<div class="formElement_row leftpadding_none">
													<label><b>Acceptable File Types: <span class="mandatory">*</span></b></label> 
												</div>	
												<div class="cb"></div>
											</td>
											<td colspan="3">
												<div class="formElement_row leftpadding_none">
													<label style="width: auto !important; margin-left: 20px !important;"><b><c:out value="${manageAssetsBean.acceptableFilesTypesForSelectedChannel }"></c:out></b> </label> 
												</div>	
												<div class="cb"></div>
											</td>
										</tr>
										</c:if>
										
										<tr>
											<td>
												<div class="formElement_row leftpadding_none">
													<label>Upload File(s):<span class="mandatory">*</span></label> 
												</div>	
												<div class="cb"></div>
											</td>
											<td colspan="3">
												<div class="formElement_row leftpadding_none">
													<input type="file" name="MS3_File" id="MS3_File" multiple style="width: 250px !important;" />&nbsp; 
												</div>	
												<div class="cb"></div>
												<label class="errorMessage" style="margin-left:20px;">* Click <a href="javascript:void(0);" onclick="ms3_showAcceptablesFilesDialog();">here</a> to refer acceptable file types for all the available channels.</label>	
												<div class="cb"></div>
											</td>
										</tr>
										<tr>
											<td colspan="4" align="right" valign="top" style="padding: 15px !important;">
												<input type="button" id="MS3_XREF_RESET" class="gear_button cursorPointer" onclick="ms3_xref_reset();" value="Reset"/>
												<input type="button" class="bluebutton cursorPointer" value="Upload" name="MS3_Upload_Button" id="MS3_Upload_Button" onclick="ms3_upload();" />
											</td>
										</tr>
									</table>
								</td>
							</tr>	
							</table>	
							
							<% 
							boolean showCheckBoxes = false;
							if(null!=manageAssetsBean.getUploadedFileList() && manageAssetsBean.getUploadedFileList().size()>0)
							{
							%>
							
							<p style="height:45px;">&nbsp;</p>
							<h4>
								Results Summary
							</h4>
								
							<br />
									<% 	
										for(int i=0;i<manageAssetsBean.getUploadedFileList().size();i++)
										{
											Map<Object, Object> dataMap = (HashMap<Object, Object>)manageAssetsBean.getUploadedFileList().get(i);
											String colorClass = dataMap.get("MESSAGE_COLOR").toString();
											if(null!=colorClass && !"".equals(colorClass) && colorClass.equals("orange"))
											{
												showCheckBoxes = true;
											}
										}
									%>
							<table border="0" cellspacing="0" cellpadding="0" id="example" class="display historyTable">
								<thead>
									<tr>
										<th width="5%" scope="col">
											<% 
												if(showCheckBoxes==true)
												{	
											%>
											<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="ms3_selectallrows(this);" />
											<% } else { %>
												#.	
											<% } %>
										</th>
										<th width="25%" scope="col">File Name</th>
										<th width="55%" scope="col">Uploaded File Path</th>
										<th width="15%" scope="col">Status</th>
									</tr>
								</thead>
								<tbody>
									<% int count=0; %>
									
									<!--  Add First Files to be Over Written -->
									<% 	
										
										for(int i=0;i<manageAssetsBean.getUploadedFileList().size();i++)
										{
											Map<Object, Object> dataMap = (HashMap<Object, Object>)manageAssetsBean.getUploadedFileList().get(i);
											String colorClass = dataMap.get("MESSAGE_COLOR").toString();
											
									%>
											<%
												if(null!=colorClass && !"".equals(colorClass) && colorClass.equals("orange"))
												{
													count++;
											%>
											<tr>
												<td>
													<input type="checkbox" name="MDM_AXL_Selection" onchange="ms3_checkBoxManagement();" 
															value="<%=dataMap.get("FILE_NAME") %>" 
															id="MDM_AXL_Selection_<%=count %>"
															/>
												</td>
												<td><%=dataMap.get("FILE_NAME") %></td>
												<td><%=dataMap.get("UPLOADED_PATH") %></td>
												<td><label class="label-orange">Already Exists</label></td>
											</tr>	
											<% } %>
									<% } %>
									
									<% 	
										
										for(int i=0;i<manageAssetsBean.getUploadedFileList().size();i++)
										{
											
											Map<Object, Object> dataMap = (HashMap<Object, Object>)manageAssetsBean.getUploadedFileList().get(i);
											String colorClass = dataMap.get("MESSAGE_COLOR").toString();
									%>
									<%
												if(null!=colorClass && !"".equals(colorClass) && !colorClass.equals("orange"))
												{
													count++;
											%>
									
									<tr>
										<td><%=count %> </td>
										<td><%=dataMap.get("FILE_NAME") %></td>
										<td><%=dataMap.get("UPLOADED_PATH") %></td>
										<td>
											<%
												if(null!=colorClass && !"".equals(colorClass) && colorClass.equals("green"))
												{
											%>
												<label class="label-green">Success</label>
											<% 
												} else if(null!=colorClass && !"".equals(colorClass) && colorClass.equals("red")) {
											%>
												<label class="label-red">Failed</label>
											<% 
												} else if(null!=colorClass && !"".equals(colorClass) && colorClass.equals("orange")) {
											%>
												<label class="label-orange">Already Exists</label>
											<% } %>
										</td>
									</tr>
									<% } %>
									<% 
										} 
									%>
								</tbody>
							</table>
					
							<% 
							} 
							%>
						
					</div>
				</div>
			</div>
		</div>
	</div>				
			
	<!-- Content Block Ends Here -->
</div>	
			

<div id="ms3_reset_dialog-confirm" title="Reset Form" class="hide">
	<p>
		<strong>Are you sure, you wish to reset the complete form ?</strong>
	</p>
</div>

<div id="ms3_discard_dialog-confirm" title="Discard" class="hide">
	<p>
		<strong>Are you sure, you wish to discard the selected files ?</strong>
	</p>
</div>

<div id="ms3_upload_dialog-confirm" title="Processing" class="hide">
	<br>
	<br>
	<p>
		<strong id="MS3_UPLOAD_STATUS"></strong>
	</p>
</div>

<div id="ms3_acceptable_file_dialog" title="Allowed File Types Matrix" class="hide">
	<div class="historyInfobox">
		<table border="0" cellspacing="0" cellpadding="0" class="historyTable" style="border-left: 1px solid #dfdfdf;">
			<thead>
				<tr>
					<th width="35%" style="text-align:left; font-size:12px;" scope="col">Channels</th>
					<th width="65%" style="text-align:left; font-size:12px;" scope="col">File Types</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Accessory") %></td>
					<td style="text-align:left;font-size:12px; "><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_ACCESSORY") %></td>
				</tr>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Group_Message") %></td>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_GROUP_MESSAGE") %></td>
				</tr>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Service_Information") %></td>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_SERVICE_INFORMATION") %></td>
				</tr>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Service_Manuals") %></td>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_SERVICE_MANUALS") %></td>
				</tr>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Static_Content") %></td>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_STATIC_CONTENT") %></td>
				</tr>
				<tr>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("Videos") %></td>
					<td style="text-align:left;font-size:12px;"><%=ApplicationPropertiesUtil.getProperty("CHANNEL_FILE_TYPES_VIDEOS") %></td>
				</tr>
			</tbody>
		</table>
	</div>
	<div class="cb"></div>
	<p>&nbsp;</p>
</div>


</form>
</body>


<script type="text/javascript" src="js/external/jquery/jquery.js"></script>
<script type="text/javascript" src="js/jquery-ui.js"></script>
<script type="text/javascript" src="js/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>

<script type="text/javascript" src="js/jquery.loadmask.js"></script>
<script type="text/javascript" src="js/input.js"></script>

<% 
if(null!=manageAssetsBean.getUploadedFileList() && manageAssetsBean.getUploadedFileList().size()>0)
{
%>

<script type="text/javascript">
var htmlContent="";
</script>

<%
	if(showCheckBoxes==true)
	{
%>
<script type="text/javascript">
htmlContent=htmlContent+"<input type=\"button\" onclick=\"ms3_overwrite();\" style=\"float:right\" name=\"MS3_Overwrite\" id=\"MS3_Overwrite\"  class=\"bluebutton cursorPointer\" value=\"Overwrite\"/>";
htmlContent=htmlContent+"<input type=\"button\" onclick=\"ms3_discard();\" style=\"float:right\" name=\"MS3_Discard\" id=\"MS3_Discard\"  class=\"gear_button cursorPointer\" value=\"Discard\"/>";
</script>

<% } %>

<script type="text/javascript">
function loadDataTable()
{
	
	//data table labels
	var show="Show";
	var entries="Entries";
	var zeroRec =  "No records found."; 
	var of="of";
	var search="Search";
	var page = "Page";
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
<%
if(showCheckBoxes==true)
{
%>
	                    { aTargets: [ 0 ], bSortable: false },
	                    { aTargets: [ 1 ], bSortable: true },
	                    { aTargets: [ 2 ], bSortable: true },
	                    { aTargets: [ 3 ], bSortable: true }
<% } else { %>
						{ aTargets: [ 0 ], bSortable: true },
						{ aTargets: [ 1 ], bSortable: true },
	                    { aTargets: [ 2 ], bSortable: true },
	                    { aTargets: [ 3 ], bSortable: true }
<% } %>

	                    
	                 ],
	                 <%
	                 if(showCheckBoxes==false)
	                 {
	                 %>             
	    "order": [[1, 'asc']],
	    <% } %>
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
	var pageNo=$("#MS3_DataTabel_displayPageNo").val();
	var pageLength=$("#MS3_DataTabel_displayPageLen").val();
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
		ms3_checkBoxManagement();
	});
}	


// call function to load Data Table
loadDataTable();
</script>

<% } %>
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});



function ms3_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#MS3_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#MS3_DataTabel_displayPageLen").val(length);
	}
	
}

function ms3_xref_reset()
{
	
	// open dialog
	$( "#ms3_reset_dialog-confirm" ).removeClass('hide').dialog({
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	"Reset": function() {
					$( this ).dialog( "close" );
					$("#MS3_ERROR_MESSAGE").html("");
					// set channelName & localeName to "" as well
					$( "#MS3_Channel_Name" ).val("");
					$( "#MS3_Locale_Name" ).val("");
					// set explicitly the form files to null before submitting form
					//document.getElementById("MS3_File").value=null;
					var $el = $('#MS3_File');
					$el.wrap('<form>').closest('form').get(0).reset();
					$el.unwrap();
					document.getElementById("MS3_Action_Clicked").value="RESET";
					document.forms['MS3_Upload_Form'].submit();
		        }  
		  	,
	        Cancel: function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		event.preventDefault();
		
		$("#ms3_reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
		$("#ms3_reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
		
}


function ms3_showAcceptablesFilesDialog()
{
	// open dialog
	$( "#ms3_acceptable_file_dialog" ).removeClass('hide').dialog({
		  //closeOnEscape: false,
		 // open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      modal: true
	    });
		event.preventDefault();	
}

function ms3_show_processingDialog()
{
	var classValue = $( "#ms3_upload_dialog-confirm" ).attr("class");
	if(classValue=="hide")
	{
		// open dialog
		$( "#ms3_upload_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true
		    });
			event.preventDefault();	
			
		// call ms3_getUploadStatus
		ms3_getUploadStatus();	
	}
}


function ms3_getUploadStatus()
{
	var classValue = $( "#ms3_upload_dialog-confirm" ).attr("class");
	if(classValue!="hide")
	{
		var serverUrl = "<%=request.getContextPath() %>/Progress";
		// Make Ajax Call
		$.ajax({
			url:serverUrl,
			type:"post",
			data:{
				MS3_GET_PROGRESS:"MS3_GET_PROGRESS"
			},
			dataType:"text",
			success:function(responseData){
				if(null != responseData)
				{
					var message = "Please wait.... ";
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
						var responseString = jsonData.RESPONSESTRING;
						
						if(token=="DATA")
						{
							// set in STRONG TAG OF UPLOAD DIALOG
							
							if(null!=responseString && responseString!="" && responseString!="0")
							{
								message = message+responseString+" % Done.";
							}
							
							
						}
					}
					catch(err)
					{
						console.log(err);
					}
					$('#MS3_UPLOAD_STATUS').html(message);
				}
			},
			error:function(xmlhttp, status, error){
				//alert("error :: "+status);
				console.log(err);
			}
		});	
		
		// call function after every 2 second
		setTimeout(ms3_getUploadStatus, 2000);
	}
}

function ms3_getLocales()
{
	ms3_readDataTableValues();
	$("#MS3_ERROR_MESSAGE").html("");
	// set explicitly the form files to null before submitting form
	//document.getElementById("MS3_File").value=null;
	var $el = $('#MS3_File');
	$el.wrap('<form>').closest('form').get(0).reset();
	$el.unwrap();
	document.getElementById("MS3_Action_Clicked").value="GET_LOCALES";
    document.forms['MS3_Upload_Form'].submit();
}

function ms3_upload()
{
	ms3_readDataTableValues();
	$("#MS3_ERROR_MESSAGE").html("");
	if(ms3_validateFields())
	{
		document.getElementById("MS3_Action_Clicked").value="UPLOAD";
	    document.forms['MS3_Upload_Form'].submit();
	    ms3_show_processingDialog();	
	}
}


function ms3_validateFields()
{
	var channelName = $("#MS3_Channel_Name").val();
	var localeName = $("#MS3_Locale_Name").val();
	var fileData = $('#MS3_File').val();
	if(null==channelName || channelName=="")
	{
		// show errorMessage
		ms3_showErrorMessage();
		return false;
	}
	if(null!=channelName && channelName!="")
	{
		if(channelName!="Group Message")
		{
			if(null==localeName || localeName=="")
			{
				// show erroMessage
				ms3_showErrorMessage();
				return false;
			}	
		}	
	}
	if(null==fileData || fileData=="")
	{
		// show errorMessage
		ms3_showErrorMessage();
		return false;
	}	
	return true;
}

function ms3_showErrorMessage()
{
	var message="<div class=\"errorMessage\">";
	message = message+"Please fill all the fields marked as mandatory (*).";
	message = message +"</div>";
	
	$("#MS3_ERROR_MESSAGE").html(message);
	window.scrollTo(0,0);
	
}


function ms3_selectallrows(thisObj)
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
		$("#MS3_SelectedRows").val("");
	}
	ms3_checkBoxManagement();
	
}

function ms3_checkBoxManagement()
{
	$("#MS3_SelectedRows").val("");
	var value=$("#MS3_SelectedRows").val();
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
		$("#MS3_SelectedRows").val(result);
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

function ms3_overwrite()
{
	ms3_readDataTableValues();
	
	var values = $("#MS3_SelectedRows").val();
	if(null!=values && values!="")
	{
		document.getElementById("MS3_Action_Clicked").value="OVERWRITE";
	    document.forms['MS3_Upload_Form'].submit();
	}
	else
	{
		var message="<div class=\"errorMessage\">";
		message = message+"Please select atleast 1 row to overwrite.";
		message = message +"</div>";
		
		$("#MS3_ERROR_MESSAGE").html(message);
		window.scrollTo(0,0);
	}	
}

function ms3_discard()
{
	ms3_readDataTableValues();
	var values = $("#MS3_SelectedRows").val();
	if(null!=values && values!="")
	{
		// open dialog
		$( "#ms3_discard_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		      resizable: false,
		      height:140,
		      modal: true,
		      buttons: {
		    	"Yes": function() {
						$( this ).dialog( "close" );
						document.getElementById("MS3_Action_Clicked").value="DISCARD";
					    document.forms['MS3_Upload_Form'].submit();
			        }  
			  	,
		        "No": function() {
		          $( this ).dialog( "close" );
		        }
		      }
		    });
			event.preventDefault();
			
			$("#ms3_discard_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // first button
			$("#ms3_discard_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var message="<div class=\"errorMessage\">";
		message = message+"Please select atleast 1 row to discard.";
		message = message +"</div>";
		
		$("#MS3_ERROR_MESSAGE").html(message);
		window.scrollTo(0,0);
	}	
}


function ms3_discardConfirmation()
{
	
			
}

</script>

</html>