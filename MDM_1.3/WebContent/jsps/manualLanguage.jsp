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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_MANUAL_LANGUAGE);
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
<jsp:useBean id="manualLanguageBean" class="com.mazda.gms3.mdm.bean.ManualLanguageBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.manuallanguage"/> </title>
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
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.manuallanguage"/> &rsaquo;</a>   
            </div>
            <form id="ML_Form" name="ML_Form" action="<%=request.getContextPath() %>/manuallanguage" method="post">
            <input type="hidden" name="ML_SelectedRows" id="ML_SelectedRows" value="<c:out value="${manualLanguageBean.selectedRows }"/>" />
			<input type="hidden" name="ML_UpdatedRows" id="ML_UpdatedRows" value="" />
			<input type="hidden" name="ML_ResetAction" id="ML_ResetAction" value="" />
			<input type="hidden" name="ML_DeleteAction" id="ML_DeleteAction" value="" />
			<input type="hidden" name="ML_EditAction" id="ML_EditAction" value="" />
			<input type="hidden" name="ML_PermanentDeleteAction" id="ML_PermanentDeleteAction" value="" />
			<input type="hidden" name="ML_CountrylocaleSelection" id="ML_CountrylocaleSelection" value="" />
			<input type="hidden" name="ML_DataTabel_displayPageNo" id="ML_DataTabel_displayPageNo" value="<c:out value="${manualLanguageBean.displayPageNo }"/>" />
			<input type="hidden" name="ML_DataTabel_displayPageLen" id="ML_DataTabel_displayPageLen" value="<c:out value="${manualLanguageBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.manuallanguage"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_ML_Error_Message">
											<c:if test="${!empty manualLanguageBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=manualLanguageBean.getErrorMessage() && !"".equals(manualLanguageBean.getErrorMessage()))
														{
															String[] msgs = manualLanguageBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty manualLanguageBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${manualLanguageBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td>
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="ML_CountryLocale_Code" name="ML_CountryLocale_Code" onchange="mdm_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty manualLanguageBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${manualLanguageBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty manualLanguageBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq manualLanguageBean.countryLocaleId}">
																	<c:set var="selectedFlagLang" value="1" />
																</c:if>
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
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${manualLanguageBean.showWriteControls eq true }">
									<tr>
										<td>&nbsp;</td>
									</tr>
									<tr>
										<td>
											<h3>
												<fmt:message key="label.insertnewdata"/>
											</h3>
											<br />
										</td>
									</tr>
									<tr>
										<td>
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td><fmt:message key="label.languagecode"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" maxlength="10" onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" id="ML_Lang_Code" name="ML_Lang_Code" value="<c:out value="${manualLanguageBean.fieldDetails.manualLanguageCode }" />" /></td>
													<td><fmt:message key="label.languagename"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="ML_Lang_Name" name="ML_Lang_Name" value="<c:out value="${manualLanguageBean.fieldDetails.manualLanguageName }" />"/></td>
													<td style="text-align: center !important;">
														<input type="submit" name="ML_Entry" id="ML_Entry" onclick="mdm_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty manualLanguageBean.languageList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty manualLanguageBean.languageList }">
											<c:forEach var="languageList" items="${manualLanguageBean.languageList }">
												<c:if test="${languageList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>	
										<c:if test="${empty manualLanguageBean.languageList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${manualLanguageBean.showUpdate eq true }"> disabled="disabled" </c:if> 
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="18%"><fmt:message key="label.languagecode"/> <c:if test="${manualLanguageBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.languagename"/> <c:if test="${manualLanguageBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.flag"/> <c:if test="${manualLanguageBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="18%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty manualLanguageBean.languageList }">
											<c:forEach var="languageList" items="${manualLanguageBean.languageList }">
												<tr <c:if test="${languageList.editableFlag eq true  }">class="selected"</c:if> >
													<td> 
														<input type="checkbox" name="MDM_ML_Selection" onchange="mdm_ml_checkBoxManagement();" 
															value="<c:out value="${languageList.manualLanguageId }" />" 
															id="MDM_ML_Selection_<c:out value="${languageList.manualLanguageId }"  />"
															<c:if test="${languageList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${manualLanguageBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${languageList.srNo }" /> </td>
													<td>
														<c:if test="${languageList.editableFlag eq false }">
															<label style="text-align: left;"><c:out value="${languageList.manualLanguageCode }" /></label>
														</c:if>
														<c:if test="${languageList.editableFlag eq true }">
															<input type="text" value="<c:out value="${languageList.manualLanguageCode }" />" size="10" maxlength="10" onchange="lengthValidation(this);" onkeyup="lengthValidation(this);" name="ML_LangList_Code_<c:out value="${languageList.manualLanguageId }" />" id="ML_LangList_Code_<c:out value="${languageList.manualLanguageId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${languageList.editableFlag eq false }">
															<label  style="text-align: left;"><c:out value="${languageList.manualLanguageName }" /></label>
														</c:if>
														<c:if test="${languageList.editableFlag eq true }">
															<input type="text" value="<c:out value="${languageList.manualLanguageName }" />" size="10" name="ML_LangList_Name_<c:out value="${languageList.manualLanguageId }" />" id="ML_LangList_Name_<c:out value="${languageList.manualLanguageId }" />"/>
														</c:if>
													</td>
													<td style="text-align: left;">
														<c:if test="${languageList.editableFlag eq false }">
															<label ><c:out value="${languageList.flagLabel }" /></label>
														</c:if>
														<c:if test="${languageList.editableFlag eq true }">
															<select name="ML_LangList_Flag_<c:out value="${languageList.manualLanguageId }" />" id="ML_LangList_Flag_<c:out value="${languageList.manualLanguageId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${manualLanguageBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty languageList.flag}">
																		<c:if test="${languageList.flag eq flagList.value}">
																			<c:set var="selectedFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlag eq 1}">
																			<option value="<c:out value="${flagList.value }" />" selected="selected"><c:out value="${flagList.label }" /></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${flagList.value }" />"><c:out value="${flagList.label }" /></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</select>
														</c:if>
													</td>
													<td>
														<fmt:formatDate value="${languageList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${languageList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty manualLanguageBean.languageList }">
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
<div id="reset_dialog-confirm" title="<fmt:message key="label.resetform"/>" class="hide">
	<p>
		<strong><fmt:message key="label.resetdescription"/></strong>
	</p>
</div>
<div id="delete_dialog-confirm" title="<fmt:message key="label.delete"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.deletedescription"/>
	</p>
</div>

<div id="permanentdelete_dialog-confirm" title="<fmt:message key="label.permanentdelete"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.permanentdeletedescription"/>
	</p>
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

<c:if test="${!empty manualLanguageBean.languageList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty manualLanguageBean.languageList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${manualLanguageBean.showUpdate eq false && manualLanguageBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel= "<fmt:message key="label.delete"/>";
	var permanentDeleteLabel = "<fmt:message key="label.permanentdelete"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"mdm_edit();\" name=\"ML_Edit\" id=\"ML_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"mdm_delete();\" name=\"ML_Delete\" id=\"ML_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"mdm_permanentdelete();\" name=\"ML_PermanentDelete\" id=\"ML_PermanentDelete\" class=\"bluebutton cursorPointer\" value=\""+permanentDeleteLabel+"\"/>";
	edit=null;
</script>
</c:if>
<c:if test="${manualLanguageBean.showUpdate eq true && manualLanguageBean.showWriteControls eq true }">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"submit\" onclick=\"mdm_update();\" style=\"float:right\" name=\"ML_Update\" id=\"ML_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"ML_Reset\" id=\"ML_Reset\" onclick=\"mdm_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
</script>
</c:if>
	
<script type="text/javascript">

function lengthValidation(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^a-zA-Z-]/g,'');
	//thisObj.value=thisObj.value.toLowerCase();
}

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
                        { aTargets: [ 0 ], bSortable: false },
                        { aTargets: [ 1 ], bSortable: true },
                        { aTargets: [ 2 ], bSortable: true },
                        { aTargets: [ 3 ], bSortable: true },
                        { aTargets: [ 4 ], bSortable: true },
                        { aTargets: [ 5 ], bSortable: true },
                        { aTargets: [ 6 ], bSortable: true }
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
	var pageNo=$("#ML_DataTabel_displayPageNo").val();
	var pageLength=$("#ML_DataTabel_displayPageLen").val();
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
		mdm_ml_checkBoxManagement();
	});
}

function mdm_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#ML_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#ML_DataTabel_displayPageLen").val(length);	
	}	
	
}

function mdm_edit()
{
	mdm_readDataTableValues();
	var value=$("#ML_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#ML_EditAction").val("EDIT");
		$("#ML_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}

function ML_delete_action()
{
	// show Loader
	$("#loader").show();
	mdm_readDataTableValues();
	$("#ML_DeleteAction").val("DELETE");
	$("#ML_Form").submit();
}

function mdm_delete()
{
	$("#MDM_ML_Error_Message").html("");
	var value=$("#ML_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#delete_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: false,
			  height:140,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.yes"/>": function() {
					  $( this ).dialog( "close" );
					  ML_delete_action();
					}  
				,
				"<fmt:message key="label.no"/>": function() {
				  $( this ).dialog( "close" );
				}
			  }
			});
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
		$("#delete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.delete" />";
		mdm_show_errorMessage(delMessage);	
	}
}

function ML_permanentdelete_action()
{
	// show Loader
	$("#loader").show();
	mdm_readDataTableValues();
	$("#ML_PermanentDeleteAction").val("PERMANENT_DELETE");
	$("#ML_Form").submit();
}

function mdm_permanentdelete()
{
	$("#MDM_ML_Error_Message").html("");
	var value=$("#ML_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#permanentdelete_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: false,
			  height:140,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.yes"/>": function() {
					  $( this ).dialog( "close" );
					  ML_permanentdelete_action();
					}  
				,
				"<fmt:message key="label.no"/>": function() {
				  $( this ).dialog( "close" );
				}
			  }
			});
		$("#permanentdelete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
		$("#permanentdelete_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.permanent.delete" />";
		mdm_show_errorMessage(delMessage);	
	}
}

function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_ML_Error_Message").html(html);
	window.scrollTo(0,0);
}


function mdm_entry()
{
	// show Loader
	$("#loader").show();
	mdm_readDataTableValues();
}

function mdm_update()
{
	var newData="";
	var table = $('#example').DataTable();
	var data = table.$('input , select').serializeArray();
	$.each(data, function(i, field){
		if(null!=newData && newData!="")
		{
			newData=newData+"<MDM_FS>";
		}
		var v = "";
		if(null!=field.value && field.value!="")
		{
			v = $.trim(field.value);
		}	
		newData=newData+field.name+"<MDM_TS>"+v;
		v = null;
	});
	// show Loader
	$("#loader").show();
	$("#ML_UpdatedRows").val(newData);	
 	mdm_readDataTableValues();
}

function mdm_ml_checkBoxManagement()
{
	$("#ML_SelectedRows").val("");
	var value=$("#ML_SelectedRows").val();
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
		$("#ML_SelectedRows").val(result);
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

function mdm_reset_withDialog()
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
				 $("#ML_ResetAction").val("Reset");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
				  $("#ML_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function mdm_reset()
{
	// show Loader
	$("#loader").show();
	 $("#ML_ResetAction").val("Reset");
     $("#ML_Form").submit();
}

function mdm_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#ML_CountrylocaleSelection").val("COUNTRY_LOCALE_SELECTION");
	 $("#ML_Form").submit();
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
		$("#ML_SelectedRows").val("");
	}
	mdm_ml_checkBoxManagement();
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
						mdm_readDataTableValues();
						 $("#ML_ResetAction").val("Reset");
						 $("#ML_Form").submit();
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