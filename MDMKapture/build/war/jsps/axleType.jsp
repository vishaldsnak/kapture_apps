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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_DRIVEAXLE_TYPE);
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
<jsp:useBean id="axleTypeBean" class="com.mazda.gms3.mdm.bean.AxleTypeBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.axletype"/> </title>
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.vehicletype" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.axletype"/> &rsaquo;</a> 
           	</div>
           	
            <form id="AXL_Form" name="AXL_Form" action="<%=request.getContextPath() %>/axletype" method="post">
            <input type="hidden" name="AXL_SelectedRows" id="AXL_SelectedRows" value="<c:out value="${axleTypeBean.selectedRows }"/>" />
			<input type="hidden" name="AXL_UpdatedRows" id="AXL_UpdatedRows" value="" />
			<input type="hidden" name="AXL_ResetAction" id="AXL_ResetAction" value="" />
			<input type="hidden" name="AXL_DeleteAction" id="AXL_DeleteAction" value="" />
			<input type="hidden" name="AXL_EditAction" id="AXL_EditAction" value="" />
			<input type="hidden" name="AXL_ActiveAction" id="AXL_ActiveAction" value="" />
			<input type="hidden" name="AXL_ExportAction" id="AXL_ExportAction" value="" />
			
			<input type="hidden" name="AXL_DataTabel_displayPageNo" id="AXL_DataTabel_displayPageNo" value="<c:out value="${axleTypeBean.displayPageNo }"/>" />
			<input type="hidden" name="AXL_DataTabel_displayPageLen" id="AXL_DataTabel_displayPageLen" value="<c:out value="${axleTypeBean.displayPageLength }"/>" />
			<input type="hidden" name="AXL_LanguageSelection" id="AXL_LanguageSelection" value="" />
           	<input type="hidden" name="AXL_CountrySelection" id="AXL_CountrySelection" value="" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_AXL_Error_Message" colspan="3">
											<c:if test="${!empty axleTypeBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=axleTypeBean.getErrorMessage() && !"".equals(axleTypeBean.getErrorMessage()))
														{
															String[] msgs = axleTypeBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty axleTypeBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${axleTypeBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty axleTypeBean.infoMessage }">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${axleTypeBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="AXL_CountryLocale_Code" name="AXL_CountryLocale_Code" onchange="axl_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty axleTypeBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${axleTypeBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty axleTypeBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq axleTypeBean.countryLocaleId}">
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
												<select id="AXL_Lang_Code" name="AXL_Lang_Code" onchange="axl_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty axleTypeBean.languageList }">
														<c:forEach var="languageList" items="${axleTypeBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq axleTypeBean.manualLanguageId}">
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
									<c:if test="${axleTypeBean.showWriteControls eq true }">
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
													<td><fmt:message key="label.axletypecode"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="AXL_Type_Code" name="AXL_Type_Code" value="<c:out value="${axleTypeBean.fieldDetails.axleCode }" />"/></td>
													<td><fmt:message key="label.axletypedesc.reg"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="AXL_Description_Reg" name="AXL_Description_Reg" value="<c:out value="${axleTypeBean.fieldDetails.axleCodeDescriptionRegional }" />"/></td>
													<td><fmt:message key="label.axletypedesc"/><span class="mandatory">*</span></td>
													<td><input style="width: 200px;" type="text" id="AXL_Description" name="AXL_Description" value="<c:out value="${axleTypeBean.fieldDetails.axleCodeDescription }" />"/></td>
													<td style="text-align: center !important;">
														<input type="submit" name="AXL_Entry" id="AXL_Entry" onclick="axl_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${axleTypeBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.axletype"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.axletype"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label>
												<input id="AXL_uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="AXL_File" name="AXL_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="axl_file_upload();" id="AXL_Upload" name="AXL_Upload"
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
												<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
												<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
													id="AXL_DownloadTemplate" onclick="axl_download_template();"
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
								<c:if test="${!empty axleTypeBean.axleTypeList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty axleTypeBean.axleTypeList }">
											<c:forEach var="axleTypeList" items="${axleTypeBean.axleTypeList }">
												<c:if test="${axleTypeList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>	
										<c:if test="${empty axleTypeBean.axleTypeList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="axl_selectallrows(this);"
												<c:if test="${axleTypeBean.showUpdate eq true }"> disabled="disabled" </c:if> 
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="6%">#.</th>
											<th scope="col" width="13%"><fmt:message key="label.axletypecode"/> <c:if test="${axleTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.axletypedesc.reg"/> <c:if test="${axleTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="18%"><fmt:message key="label.axletypedesc"/> <c:if test="${axleTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${axleTypeBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.imsyncstatus"/></th>
											<th scope="col" width="11%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="11%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty axleTypeBean.axleTypeList }">
											<c:forEach var="axleTypeList" items="${axleTypeBean.axleTypeList }">
												<tr <c:if test="${axleTypeList.editableFlag eq true  }">class="selected"</c:if> >
													<td>
														<!-- CONDITION COMMENTED FOR DEPRECATED ROWS  axleTypeList.showCheckBox --> 
														<input type="checkbox" name="MDM_AXL_Selection" onchange="axl_checkBoxManagement();" 
															value="<c:out value="${axleTypeList.axleTypeId }" />" 
															id="MDM_AXL_Selection_<c:out value="${axleTypeList.axleTypeId }"  />"
															<c:if test="${axleTypeList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${axleTypeBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${axleTypeList.srNo }" /> </td>
													<td>
														<c:if test="${axleTypeList.editableFlag eq false }">
															<label style="text-align: left;"><c:out value="${axleTypeList.axleCode }" /></label>
														</c:if>
														<c:if test="${axleTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${axleTypeList.axleCode }" />" name="AXL_AxleList_Code_<c:out value="${axleTypeList.axleTypeId }" />" size="10" id="AXL_AxleList_Code_<c:out value="${axleTypeList.axleTypeId }" />" style="width: 150px;" />
														</c:if>
													</td>
													<td>
														<c:if test="${axleTypeList.editableFlag eq false }">
															<label  style="text-align: left;"><c:out value="${axleTypeList.axleCodeDescriptionRegional }" /></label>
														</c:if>
														<c:if test="${axleTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${axleTypeList.axleCodeDescriptionRegional }" />" size="10" name="AXL_AxleList_Desc_Reg_<c:out value="${axleTypeList.axleTypeId }" />" id="AXL_AxleList_Desc_Reg_<c:out value="${axleTypeList.axleTypeId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${axleTypeList.editableFlag eq false }">
															<label  style="text-align: left;"><c:out value="${axleTypeList.axleCodeDescription }" /></label>
														</c:if>
														<c:if test="${axleTypeList.editableFlag eq true }">
															<input type="text" value="<c:out value="${axleTypeList.axleCodeDescription }" />" size="10" name="AXL_AxleList_Desc_<c:out value="${axleTypeList.axleTypeId }" />" id="AXL_AxleList_Desc_<c:out value="${axleTypeList.axleTypeId }" />"/>
														</c:if>
													</td>
													<td style="text-align: left;">
														<c:if test="${axleTypeList.editableFlag eq false }">
															<label ><c:out value="${axleTypeList.flagLabel }" /></label>
														</c:if>
														<c:if test="${axleTypeList.editableFlag eq true }">
															<select name="AXL_AxleList_Flag_<c:out value="${axleTypeList.axleTypeId }" />" id="AXL_AxleList_Flag_<c:out value="${axleTypeList.axleTypeId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${axleTypeBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty axleTypeList.flag}">
																		<c:if test="${axleTypeList.flag eq flagList.value}">
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
														<c:if test="${!empty axleTypeList.syncStatus }">
															<c:out value="${axleTypeList.syncStatus }"/>
														</c:if>
														<c:if test="${empty axleTypeList.syncStatus }">-</c:if>
													</td>
													<td>
														<fmt:formatDate value="${axleTypeList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${axleTypeList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty axleTypeBean.axleTypeList }">
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


<c:if test="${!empty axleTypeBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${axleTypeBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>

<c:if test="${!empty axleTypeBean.axleTypeList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty axleTypeBean.axleTypeList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${axleTypeBean.showUpdate eq false && axleTypeBean.showWriteControls eq true}">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel="<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"axl_edit();\" name=\"AXL_Edit\" id=\"AXL_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"axl_delete();\" name=\"AXL_Delete\" id=\"AXL_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"axl_active();\" name=\"AXL_Active\" id=\"AXL_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>
<c:if test="${axleTypeBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"axl_export();\"  name=\"AXL_Export\" id=\"AXL_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>

<c:if test="${axleTypeBean.showUpdate eq true && axleTypeBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"submit\" onclick=\"axl_update();\" style=\"float:right\" name=\"AXL_Update\" id=\"AXL_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"AXL_Reset\" id=\"AXL_Reset\" onclick=\"axl_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
	var pageNo=$("#AXL_DataTabel_displayPageNo").val();
	var pageLength=$("#AXL_DataTabel_displayPageLen").val();
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
		axl_checkBoxManagement();
	});
}

function axl_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#AXL_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#AXL_DataTabel_displayPageLen").val(length);
	}
	
}

function axl_edit()
{
	axl_readDataTableValues();
	var value=$("#AXL_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#AXL_EditAction").val("EDIT");
		$("#AXL_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}

function AXL_active_action()
{
	// show Loader
	$("#loader").show();
	axl_readDataTableValues();
	$("#AXL_ActiveAction").val("ACTIVE");
	$("#AXL_Form").submit();
}

function axl_active()
{
	$("#MDM_AXL_Error_Message").html("");
	var value=$("#AXL_SelectedRows").val();
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
					  AXL_active_action();
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



function AXL_delete_action()
{
	// show Loader
	$("#loader").show();
	axl_readDataTableValues();
	$("#AXL_DeleteAction").val("DELETE");
	$("#AXL_Form").submit();
}

function axl_delete()
{
	$("#MDM_AXL_Error_Message").html("");
	var value=$("#AXL_SelectedRows").val();
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
					  AXL_delete_action();
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

function axl_export()
{
	$("#MDM_AXL_Error_Message").html("");
	var value=$("#AXL_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		axl_readDataTableValues();
		$("#AXL_ExportAction").val("EXPORT");
		  $("#AXL_Form").submit();
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
	$("#MDM_AXL_Error_Message").html(html);
	window.scrollTo(0,0);
}

function axl_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#AXL_CountrySelection").val("COUNTRY_LOCALE_SELECTION");
	 $("#AXL_Form").submit();
}

function axl_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#AXL_LanguageSelection").val("LANG_SELECTION");
	 $("#AXL_Form").submit();
}

function axl_entry()
{
	axl_readDataTableValues();
}

function axl_update()
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
	$("#AXL_UpdatedRows").val(newData);	
 	axl_readDataTableValues();
}

function axl_checkBoxManagement()
{
	$("#AXL_SelectedRows").val("");
	var value=$("#AXL_SelectedRows").val();
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
		$("#AXL_SelectedRows").val(result);
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

function axl_reset_withDialog()
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
				 $("#AXL_ResetAction").val("Reset");
		          $( this ).dialog( "close" );
		       		// show Loader
					$("#loader").show();
		          $("#AXL_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
	$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
	$("#reset_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function axl_reset()
{
	// show Loader
	$("#loader").show();
	 $("#AXL_ResetAction").val("Reset");
     $("#AXL_Form").submit();
}


function axl_selectallrows(thisObj)
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
		$("#AXL_SelectedRows").val("");
	}
	axl_checkBoxManagement();
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


<script type="text/javascript">
/*
 * EXCEL IMPORT
 */
if(null!=document.getElementById("AXL_File")) {
	document.getElementById("AXL_File").onchange = function () {
		document.getElementById("AXL_uploadFile").value = this.value;
	};
}

/*
 * The import is the ONLY multipart submit of this screen: the form is switched to multipart for
 * this one submit. The servlet sends a multipart request to the import, every other action keeps
 * posting exactly as before.
 */
function axl_file_upload()
{
	$("#loader").show();
	var importForm = document.getElementById("AXL_Form");
	importForm.enctype = "multipart/form-data";
	importForm.encoding = "multipart/form-data";
	importForm.submit();
}

/*
 * A page restored from the browser's back / forward cache keeps the form as it was - put it back
 * to a normal post so the next action is not taken for an import.
 */
window.addEventListener("pageshow", function () {
	var mainForm = document.getElementById("AXL_Form");
	if(null!=mainForm) {
		mainForm.enctype = "application/x-www-form-urlencoded";
		mainForm.encoding = "application/x-www-form-urlencoded";
	}
});

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT submitted,
 * so nothing already keyed in is lost.
 */
function axl_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=DRIVE_AXLE_TYPE";
}
</script>

</html>