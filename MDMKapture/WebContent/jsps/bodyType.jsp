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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_BODY_TYPE);
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
<jsp:useBean id="bodyTypeBean" class="com.mazda.gms3.mdm.bean.BodyTypeBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.bodytype"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.vehicletype" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.bodytype"/> &rsaquo;</a> 
            </div>
            
            <form id="BDT_Form" name="BDT_Form" action="<%=request.getContextPath() %>/bodytype" method="post">
            <input type="hidden" name="BDT_SelectedRows" id="BDT_SelectedRows" value="<c:out value="${bodyTypeBean.selectedRows }"/>" />
			<input type="hidden" name="BDT_UpdatedRows" id="BDT_UpdatedRows" value="" />
			<input type="hidden" name="BDT_ResetAction" id="BDT_ResetAction" value="" />
			<input type="hidden" name="BDT_DeleteAction" id="BDT_DeleteAction" value="" />
			<input type="hidden" name="BDT_EditAction" id="BDT_EditAction" value="" />
			<input type="hidden" name="BDT_ExportAction" id="BDT_ExportAction" value="" />
			<input type="hidden" name="BDT_ActiveAction" id="BDT_ActiveAction" value="" />
			<input type="hidden" name="BDT_DataTabel_displayPageNo" id="BDT_DataTabel_displayPageNo" value="<c:out value="${bodyTypeBean.displayPageNo }"/>" />
			<input type="hidden" name="BDT_DataTabel_displayPageLen" id="BDT_DataTabel_displayPageLen" value="<c:out value="${bodyTypeBean.displayPageLength }"/>" />
			<input type="hidden" name="BDT_LanguageSelection" id="BDT_LanguageSelection" value="" />
           	<input type="hidden" name="BDT_CountrySelection" id="BDT_CountrySelection" value="" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_BDT_Error_Message" colspan="3">
											<c:if test="${!empty bodyTypeBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=bodyTypeBean.getErrorMessage() && !"".equals(bodyTypeBean.getErrorMessage()))
														{
															String[] msgs = bodyTypeBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty bodyTypeBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${bodyTypeBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty bodyTypeBean.infoMessage }">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${bodyTypeBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="BDT_CountryLocale_Code" name="BDT_CountryLocale_Code" onchange="bdt_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty bodyTypeBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${bodyTypeBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty bodyTypeBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq bodyTypeBean.countryLocaleId}">
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
												<select id="BDT_Lang_Code" name="BDT_Lang_Code" onchange="bdt_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty bodyTypeBean.languageList }">
														<c:forEach var="languageList" items="${bodyTypeBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq bodyTypeBean.manualLanguageId}">
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
									<c:if test="${bodyTypeBean.showWriteControls eq true }">
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
													<td><fmt:message key="label.bodytypecode"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="BDT_Type_Code" name="BDT_Type_Code" value="<c:out value="${bodyTypeBean.fieldDetails.bodyCode }" />"/></td>
													<td><fmt:message key="label.bodytypedesc.reg"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="BDT_Description_Reg" name="BDT_Description_Reg" value="<c:out value="${bodyTypeBean.fieldDetails.bodyCodeDescriptionRegional }" />"/></td>
													<td><fmt:message key="label.bodytypedesc"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="BDT_Description" name="BDT_Description" value="<c:out value="${bodyTypeBean.fieldDetails.bodyCodeDescription }" />"/></td>
													<td style="text-align: center !important;">
														<input type="submit" name="BDT_Entry" id="BDT_Entry" onclick="bdt_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${bodyTypeBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.bodytype"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.bodytype"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label>
												<input id="BDT_uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="BDT_File" name="BDT_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="bdt_file_upload();" id="BDT_Upload" name="BDT_Upload"
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
												<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
												<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
													id="BDT_DownloadTemplate" onclick="bdt_download_template();"
													title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty bodyTypeBean.bodyTypeList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty bodyTypeBean.bodyTypeList }">
											<c:forEach var="bodyTypeList" items="${bodyTypeBean.bodyTypeList }">
												<c:if test="${bodyTypeList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>	
										<c:if test="${empty bodyTypeBean.bodyTypeList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="bdt_selectallrows(this);"
												<c:if test="${bodyTypeBean.showUpdate eq true }"> disabled="disabled" </c:if> 
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="6%">#.</th>
											<th scope="col" width="13%"><fmt:message key="label.bodytypecode"/> <c:if test="${bodyTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.bodytypedesc.reg"/> <c:if test="${bodyTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.bodytypedesc"/> <c:if test="${bodyTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${bodyTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.imsyncstatus"/></th>
											<th scope="col" width="11%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="11%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty bodyTypeBean.bodyTypeList }">
											<c:forEach var="bodyTypeList" items="${bodyTypeBean.bodyTypeList }">
												<tr <c:if test="${bodyTypeList.editableFlag eq true  }">class="selected"</c:if> >
													<td> 
														<input type="checkbox" name="MDM_BDT_Selection" onchange="bdt_checkBoxManagement();" 
															value="<c:out value="${bodyTypeList.bodyTypeId }" />" 
															id="MDM_BDT_Selection_<c:out value="${bodyTypeList.bodyTypeId }"  />"
															<c:if test="${bodyTypeList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${bodyTypeBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${bodyTypeList.srNo }" /> </td>
													<td>
														<c:if test="${bodyTypeList.editableFlag eq false }">
															<label style="text-align: left;"><c:out value="${bodyTypeList.bodyCode }" /></label>
														</c:if>
														<c:if test="${bodyTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bodyTypeList.bodyCode }" />" name="BDT_BodyList_Code_<c:out value="${bodyTypeList.bodyTypeId }" />" size="10" id="BDT_BodyList_Code_<c:out value="${bodyTypeList.bodyTypeId }" />" style="width: 150px;" />
														</c:if>
													</td>
													<td>
														<c:if test="${bodyTypeList.editableFlag eq false }">
															<label  style="text-align: left;"><c:out value="${bodyTypeList.bodyCodeDescriptionRegional }" /></label>
														</c:if>
														<c:if test="${bodyTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bodyTypeList.bodyCodeDescriptionRegional }" />" size="10" name="BDT_BodyList_Desc_Reg_<c:out value="${bodyTypeList.bodyTypeId }" />" id="BDT_BodyList_Desc_Reg_<c:out value="${bodyTypeList.bodyTypeId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${bodyTypeList.editableFlag eq false }">
															<label  style="text-align: left;"><c:out value="${bodyTypeList.bodyCodeDescription }" /></label>
														</c:if>
														<c:if test="${bodyTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${bodyTypeList.bodyCodeDescription }" />" size="10" name="BDT_BodyList_Desc_<c:out value="${bodyTypeList.bodyTypeId }" />" id="BDT_BodyList_Desc_<c:out value="${bodyTypeList.bodyTypeId }" />"/>
														</c:if>
													</td>
													<td style="text-align: left;">
														<c:if test="${bodyTypeList.editableFlag eq false }">
															<label ><c:out value="${bodyTypeList.flagLabel }" /></label>
														</c:if>
														<c:if test="${bodyTypeList.editableFlag eq true }">
															<select name="BDT_BodyList_Flag_<c:out value="${bodyTypeList.bodyTypeId }" />" id="BDT_BodyList_Flag_<c:out value="${bodyTypeList.bodyTypeId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${bodyTypeBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty bodyTypeList.flag}">
																		<c:if test="${bodyTypeList.flag eq flagList.value}">
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
														<c:if test="${!empty bodyTypeList.syncStatus }">
															<c:out value="${bodyTypeList.syncStatus }"/>
														</c:if>
														<c:if test="${empty bodyTypeList.syncStatus }">-</c:if>
													</td>
													<td>
														<fmt:formatDate value="${bodyTypeList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${bodyTypeList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty bodyTypeBean.bodyTypeList }">
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


<c:if test="${!empty bodyTypeBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${bodyTypeBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>

<c:if test="${!empty bodyTypeBean.bodyTypeList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty bodyTypeBean.bodyTypeList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${bodyTypeBean.showUpdate eq false && bodyTypeBean.showWriteControls eq true}">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel="<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"bdt_edit();\" name=\"BDT_Edit\" id=\"BDT_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"bdt_delete();\" name=\"BDT_Delete\" id=\"BDT_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"bdt_active();\" name=\"BDT_Active\" id=\"BDT_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${bodyTypeBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"bdt_export();\"  name=\"BDT_Export\" id=\"BDT_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>

<c:if test="${bodyTypeBean.showUpdate eq true && bodyTypeBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"submit\" onclick=\"bdt_update();\" style=\"float:right\" name=\"BDT_Update\" id=\"BDT_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"BDT_Reset\" id=\"BDT_Reset\" onclick=\"bdt_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
</script>
</c:if>
	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


function lengthValidation(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^a-zA-Z]/g,'');
	if(null!=thisObj.value)
	{
		var value = thisObj.value;
		if(null!=value && value.length>2)
		{
			value=value.substring(0,2);	
		}
		thisObj.value = value;
	}
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
                        { aTargets: [ 7 ], bSortable: true },
                        { aTargets: [ 8 ], bSortable: true }
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
	var pageNo=$("#BDT_DataTabel_displayPageNo").val();
	var pageLength=$("#BDT_DataTabel_displayPageLen").val();
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
		bdt_checkBoxManagement();
	});
}

function bdt_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#BDT_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#BDT_DataTabel_displayPageLen").val(length);
	}
	
}

function bdt_edit()
{
	bdt_readDataTableValues();
	var value=$("#BDT_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#BDT_EditAction").val("EDIT");
		$("#BDT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}


function BDT_active_action()
{
	// show Loader
	$("#loader").show();
	bdt_readDataTableValues();
	$("#BDT_ActiveAction").val("ACTIVE");
	$("#BDT_Form").submit();
}

function bdt_active()
{
	$("#MDM_BDT_Error_Message").html("");
	var value=$("#BDT_SelectedRows").val();
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
					  BDT_active_action();
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



function BDT_delete_action()
{
	// show Loader
	$("#loader").show();
	bdt_readDataTableValues();
	$("#BDT_DeleteAction").val("DELETE");
	$("#BDT_Form").submit();
}

function bdt_delete()
{
	$("#MDM_BDT_Error_Message").html("");
	var value=$("#BDT_SelectedRows").val();
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
					  BDT_delete_action();
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


function bdt_export()
{
	$("#MDM_BDT_Error_Message").html("");
	var value=$("#BDT_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		bdt_readDataTableValues();
		$("#BDT_ExportAction").val("EXPORT");
		  $("#BDT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
}


function mdm_show_errorMessage(delMessage)
{
	
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_BDT_Error_Message").html(html);
	window.scrollTo(0,0);
}

function bdt_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#BDT_CountrySelection").val("COUNTRY_LOCALE_SELECTION");
	 $("#BDT_Form").submit();
}

function bdt_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#BDT_LanguageSelection").val("LANG_SELECTION");
	 $("#BDT_Form").submit();
}


function bdt_entry()
{
	bdt_readDataTableValues();
}

function bdt_update()
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
	$("#BDT_UpdatedRows").val(newData);	
 	bdt_readDataTableValues();
}

function bdt_checkBoxManagement()
{
	$("#BDT_SelectedRows").val("");
	var value=$("#BDT_SelectedRows").val();
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
		$("#BDT_SelectedRows").val(result);
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

function bdt_reset_withDialog()
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
				 $("#BDT_ResetAction").val("Reset");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#BDT_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function bdt_reset()
{
	// show Loader
	$("#loader").show();
	 $("#BDT_ResetAction").val("Reset");
     $("#BDT_Form").submit();
}


function bdt_selectallrows(thisObj)
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
		$("#BDT_SelectedRows").val("");
	}
	bdt_checkBoxManagement();
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
						bdt_readDataTableValues();
						 $("#BDT_ResetAction").val("Reset");
						 $("#BDT_Form").submit();
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


<script type="text/javascript">
/*
 * EXCEL IMPORT
 */
if(null!=document.getElementById("BDT_File")) {
	document.getElementById("BDT_File").onchange = function () {
		document.getElementById("BDT_uploadFile").value = this.value;
	};
}

/*
 * The import is the ONLY multipart submit of this screen: the form is switched to multipart for
 * this one submit. The servlet sends a multipart request to the import, every other action keeps
 * posting exactly as before.
 */
function bdt_file_upload()
{
	$("#loader").show();
	var importForm = document.getElementById("BDT_Form");
	importForm.enctype = "multipart/form-data";
	importForm.encoding = "multipart/form-data";
	importForm.submit();
}

/*
 * A page restored from the browser's back / forward cache keeps the form as it was - put it back
 * to a normal post so the next action is not taken for an import.
 */
window.addEventListener("pageshow", function () {
	var mainForm = document.getElementById("BDT_Form");
	if(null!=mainForm) {
		mainForm.enctype = "application/x-www-form-urlencoded";
		mainForm.encoding = "application/x-www-form-urlencoded";
	}
});

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT submitted,
 * so nothing already keyed in is lost.
 */
function bdt_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=BODY_TYPE";
}
</script>

</html>