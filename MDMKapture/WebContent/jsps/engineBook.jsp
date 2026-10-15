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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_ENGINE_BOOK);
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
<jsp:useBean id="engineBookBean" class="com.mazda.gms3.mdm.bean.EngineBookBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.enginebook"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.engine"/> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.enginebook"/> &rsaquo;</a>   
            </div>
            <form id="EB_Form" name="EB_Form" action="<%=request.getContextPath() %>/enginebook" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="EB_SelectedRows" id="EB_SelectedRows" value="<c:out value="${engineBookBean.selectedRows }"/>" />
            <input type="hidden" name="EB_ActionClicked" id="EB_ActionClicked" value="<c:out value="${engineBookBean.actionClicked }"/>" />
			<input type="hidden" name="EB_UpdatedRows" id="EB_UpdatedRows" value="" />
			<input type="hidden" name="EB_DataTabel_displayPageNo" id="EB_DataTabel_displayPageNo" value="<c:out value="${engineBookBean.displayPageNo }"/>" />
			<input type="hidden" name="EB_DataTabel_displayPageLen" id="EB_DataTabel_displayPageLen" value="<c:out value="${engineBookBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td  id="MDM_EB_Error_Message" colspan="3">
											<c:if test="${!empty engineBookBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=engineBookBean.getErrorMessage() && !"".equals(engineBookBean.getErrorMessage()))
														{
															String[] msgs = engineBookBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty engineBookBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${engineBookBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty engineBookBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${engineBookBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="EB_CountryLocale_Code" name="EB_CountryLocale_Code" onchange="EB_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty engineBookBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${engineBookBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty engineBookBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq engineBookBean.countryLocaleId}">
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
										<td width="34%">
											<div class="formElement_row leftpadding_none">		
												<label><fmt:message key="label.language"/>: <span class="mandatory">*</span></label> 
												<select id="EB_Lang_Code" name="EB_Lang_Code" onchange="EB_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty engineBookBean.languageList }">
														<c:forEach var="languageList" items="${engineBookBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq engineBookBean.manualLanguageId}">
																	<c:set var="selectedFlagLang" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLang eq 1}">
																	<option selected="selected" value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${languageList.manualLanguageId }"/>"><c:out value="${languageList.manualLanguageCode }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
										<td width="33%">&nbsp;</td>
									</tr>
									<c:if test="${engineBookBean.showWriteControls eq true }">
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<tr>
										<td colspan="3">
											<h3>
												<fmt:message key="label.insertnewdata"/>
											</h3>
											<br />
										</td>
									</tr>
									<tr>
										<td colspan="3">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td><fmt:message key="label.enginebookcode"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="EB_Book_Code" name="EB_Book_Code" value="<c:out value="${engineBookBean.fieldDetails.bookCode }" />" /></td>
													<td><fmt:message key="label.enginebookname.reg"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="EB_Book_Name_Reg" name="EB_Book_Name_Reg" value="<c:out value="${engineBookBean.fieldDetails.bookNameReg }" />"/></td>
													<td><fmt:message key="label.enginebookname.eng"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="EB_Book_Name_En" name="EB_Book_Name_En" value="<c:out value="${engineBookBean.fieldDetails.bookNameEng }" />"/></td>
													<td style="text-align: center;">
														<input type="button" name="EB_Entry" id="EB_Entry" onclick="EB_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								
								<c:if test="${engineBookBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.enginebook"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.enginebook"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="EB_File" name="EB_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="eb_file_upload();" id="EB_Upload" name="EB_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="eb_DownloadTemplate" onclick="eb_download_template();"
										title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								
								
								
								<p style="height: 45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty engineBookBean.bookList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty engineBookBean.bookList }">
											<c:forEach var="bookList" items="${engineBookBean.bookList }">
												<c:if test="${bookList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty engineBookBean.bookList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${engineBookBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="20%"><fmt:message key="label.enginebookcode"/> <c:if test="${engineBookBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="20%"><fmt:message key="label.enginebookname.reg"/> <c:if test="${engineBookBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="20%"><fmt:message key="label.enginebookname.eng"/> <c:if test="${engineBookBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${engineBookBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty engineBookBean.bookList }">
											<c:forEach var="bookList" items="${engineBookBean.bookList }">
												<tr <c:if test="${bookList.editableFlag eq true  }"> class="selected"</c:if>>
													<td> 
														<input type="checkbox" name="EB_Selection" onchange="EB_checkBoxManagement();" 
															value="<c:out value="${bookList.bookId }" />" 
															id="EB_Selection_<c:out value="${bookList.bookId }"  />"
															<c:if test="${bookList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${engineBookBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${bookList.srNo }" /> </td>
													<td>
														<c:if test="${bookList.editableFlag eq false }">
															<label><c:out value="${bookList.bookCode }" /></label>
														</c:if>
														<c:if test="${bookList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bookList.bookCode }" />" size="10" 
																name="EB_BookList_Code_<c:out value="${bookList.bookId }" />" 
																id="EB_BookList_Code_<c:out value="${bookList.bookId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${bookList.editableFlag eq false }">
															<label><c:out value="${bookList.bookNameReg }" /></label>
														</c:if>
														<c:if test="${bookList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bookList.bookNameReg }" />" size="10" 
																name="EB_BookList_Name_Reg_<c:out value="${bookList.bookId }" />" 
																id="EB_BookList_Name_Reg_<c:out value="${bookList.bookId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${bookList.editableFlag eq false }">
															<label><c:out value="${bookList.bookNameEng }" /></label>
														</c:if>
														<c:if test="${bookList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bookList.bookNameEng }" />" size="10" 
																name="EB_BookList_Name_En_<c:out value="${bookList.bookId }" />" 
																id="EB_BookList_Name_En_<c:out value="${bookList.bookId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${bookList.editableFlag eq false }">
															<label><c:out value="${bookList.flagLabel }" /></label>
														</c:if>
														<c:if test="${bookList.editableFlag eq true }">
															<select name="EB_LangList_Flag_<c:out value="${bookList.bookId }" />" id="EB_LangList_Flag_<c:out value="${bookList.bookId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${engineBookBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty bookList.flag}">
																		<c:if test="${bookList.flag eq flagList.value}">
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
														<fmt:formatDate value="${bookList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${bookList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty engineBookBean.bookList }">
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

<div id="active_dialog-confirm" title="<fmt:message key="flag.label.active"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.activedescription"/>
	</p>
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<c:if test="${!empty engineBookBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${engineBookBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>

<script>
if(null!=document.getElementById("EB_File"))
{
	document.getElementById("EB_File").onchange = function () {
    	document.getElementById("uploadFile").value = this.value;
	};
}
</script>

<c:if test="${!empty engineBookBean.bookList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty engineBookBean.bookList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>

<c:if test="${engineBookBean.showUpdate eq false }">
<c:if test="${engineBookBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var activeLabel="<fmt:message key="flag.label.active" />"; 
	var deleteLabel = "<fmt:message key="label.delete"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"EB_edit();\" name=\"EB_Edit\" id=\"EB_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"EB_delete();\" name=\"EB_Delete\" id=\"EB_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"EB_active();\" name=\"EB_Active\" id=\"EB_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${engineBookBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"EB_export();\"  name=\"EB_Export\" id=\"EB_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${engineBookBean.showUpdate eq true && engineBookBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"EB_update();\" style=\"float:right\" name=\"EB_Update\" id=\"EB_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"EB_Reset\" id=\"EB_Reset\" onclick=\"EB_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
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
	var pageNo=$("#EB_DataTabel_displayPageNo").val();
	var pageLength=$("#EB_DataTabel_displayPageLen").val();
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
		EB_checkBoxManagement();
	});
}

function EB_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#EB_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#EB_DataTabel_displayPageLen").val(length);	
	}	
	
}

function eb_active_action()
{
	// show Loader
	$("#loader").show();
	EB_readDataTableValues();
	$("#EB_ActionClicked").val("ACTIVE");
	$("#EB_Form").submit();
}

function EB_export()
{
	$("#MDM_EB_Error_Message").html("");
	EB_readDataTableValues();
	var value=$("#EB_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#EB_ActionClicked").val("EXPORT");
		$("#EB_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}
}


function EB_edit()
{
	$("#MDM_EB_Error_Message").html("");
	EB_readDataTableValues();
	var value=$("#EB_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#EB_ActionClicked").val("EDIT");
		$("#EB_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}


function eb_file_upload()
{
	// show Loader
	$("#loader").show();
	EB_readDataTableValues();
 	$("#EB_ActionClicked").val("FILE_UPLOAD");
	$("#EB_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function eb_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=ENGINE_BOOK";
}

function EB_delete_action()
{
	// show Loader
	$("#loader").show();
	EB_readDataTableValues();
	$("#EB_ActionClicked").val("DELETE");
	$("#EB_Form").submit();
}

function EB_delete()
{
	$("#MDM_EB_Error_Message").html("");
	var value=$("#EB_SelectedRows").val();
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
					  EB_delete_action();
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

function EB_active()
{
	$("#MDM_EB_Error_Message").html("");
	var value=$("#EB_SelectedRows").val();
	if(null!=value && value!="")
	{
		// open dialog
		$( "#active_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: false,
			  height:140,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.yes"/>": function() {
					  $( this ).dialog( "close" );
					  eb_active_action();
					}  
				,
				"<fmt:message key="label.no"/>": function() {
				  $( this ).dialog( "close" );
				}
			  }
			});
		$("#active_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
		$("#active_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
	}
	else
	{
		var delMessage="<fmt:message key="error.select.onerow.active" />";
		mdm_show_errorMessage(delMessage);	
	}
}

function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_EB_Error_Message").html(html);
	window.scrollTo(0,0);
}

function EB_entry()
{
	// show Loader
	$("#loader").show();
	EB_readDataTableValues();
	$("#EB_ActionClicked").val("SAVE");
	  $("#EB_Form").submit();
}

function EB_update()
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
   	$("#EB_UpdatedRows").val(newData);
	EB_readDataTableValues();
	$("#EB_ActionClicked").val("UPDATE");
	  $("#EB_Form").submit();
}

function EB_checkBoxManagement()
{
	$("#EB_SelectedRows").val("");
	var value=$("#EB_SelectedRows").val();
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
		$("#EB_SelectedRows").val(result);
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

function EB_reset()
{
	// show Loader
	$("#loader").show();
	$("#EB_ActionClicked").val("RESET");
	  $("#EB_Form").submit();
}
function EB_reset_withDialog()
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
				 $("#EB_ActionClicked").val("RESET");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#EB_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function EB_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#EB_ActionClicked").val("COUNTRY_SELECTION");
	 $("#EB_Form").submit();
}

function EB_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#EB_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#EB_Form").submit();
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
		$("#EB_SelectedRows").val("");
	}
	EB_checkBoxManagement();
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
						 EB_readDataTableValues();
						 $("#EB_ActionClicked").val("RESET");
						 $("#EB_Form").submit();
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