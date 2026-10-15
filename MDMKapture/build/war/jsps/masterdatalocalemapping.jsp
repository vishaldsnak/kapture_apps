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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_MASTERDATA_LOCALE_MAPPING);
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
<jsp:useBean id="masterDataLocaleMappingBean" class="com.mazda.gms3.mdm.bean.MasterDataLocaleMappingBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.masterdatalocalemapping"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.masterdatalocalemapping"/> &rsaquo;</a> 
            </div>			
            <form id="MDLM_Form" name="MDLM_Form" action="<%=request.getContextPath() %>/masterdatalocalemapping" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="MDLM_SelectedRows" id="MDLM_SelectedRows" value="<c:out value="${masterDataLocaleMappingBean.selectedRows }"/>" />
			<input type="hidden" name="MDLM_ActionClicked" id="MDLM_ActionClicked" value="<c:out value="${masterDataLocaleMappingBean.actionClicked }"/>" />
			<input type="hidden" name="MDLM_UpdatedRows" id="MDLM_UpdatedRows" value="" />
			<input type="hidden" name="MDLM_SelectedMappingIdForMasterDataType" id="MDLM_SelectedMappingIdForMasterDataType" value="" />
			<input type="hidden" name="MDLM_DataTabel_displayPageNo" id="MDLM_DataTabel_displayPageNo" value="<c:out value="${masterDataLocaleMappingBean.displayPageNo }"/>" />
			<input type="hidden" name="MDLM_DataTabel_displayPageLen" id="MDLM_DataTabel_displayPageLen" value="<c:out value="${masterDataLocaleMappingBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_MDLM_Error_Message" colspan="3">
											<c:if test="${!empty masterDataLocaleMappingBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=masterDataLocaleMappingBean.getErrorMessage() && !"".equals(masterDataLocaleMappingBean.getErrorMessage()))
														{
															String[] msgs = masterDataLocaleMappingBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty masterDataLocaleMappingBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${masterDataLocaleMappingBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty masterDataLocaleMappingBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${masterDataLocaleMappingBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									
									<c:if test="${masterDataLocaleMappingBean.showWriteControls eq true }">
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
													<td><fmt:message key="label.market"/><span class="mandatory">*</span></td>
													<td>
														<select id="MDLM_Market_Code" name="MDLM_Market_Code" style="width:300px;" onchange="MDLM_marketSelection();">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty masterDataLocaleMappingBean.marketList }">
																<c:forEach var="marketList" items="${masterDataLocaleMappingBean.marketList }">
																	<c:set var="selectedFlagMarket" value="" />
																	<c:if test="${!empty marketList.value}">
																		<c:if test="${marketList.value eq masterDataLocaleMappingBean.fieldDetails.market}">
																			<c:set var="selectedFlagMarket" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagMarket eq 1}">
																			<option selected="selected" value="<c:out value="${marketList.value }"/>"><c:out value="${marketList.label }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${marketList.value }"/>"><c:out value="${marketList.label }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td><fmt:message key="label.processinglocale"/><span class="mandatory">*</span></td>
													<td><input type="text" id="MDLM_Locale" name="MDLM_Locale" style="width: 285px;" value="<c:out value="${masterDataLocaleMappingBean.fieldDetails.locale }" />" /></td>
												</tr>
												<tr>
													<td><fmt:message key="label.masterdatatype"/><span class="mandatory">*</span></td>
													<td>
														<select id="MDLM_MasterDataType" name="MDLM_MasterDataType" style="width:300px;">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty masterDataLocaleMappingBean.masterDataTypesList }">
																<c:forEach var="masterDataTypesList" items="${masterDataLocaleMappingBean.masterDataTypesList }">
																	<c:set var="selectedFlagMasterDataType" value="" />
																	<c:if test="${!empty masterDataTypesList.value}">
																		<c:if test="${masterDataTypesList.value eq masterDataLocaleMappingBean.fieldDetails.masterDataType}">
																			<c:set var="selectedFlagMasterDataType" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagMasterDataType eq 1}">
																			<option selected="selected" value="<c:out value="${masterDataTypesList.value }"/>"><c:out value="${masterDataTypesList.label }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${masterDataTypesList.value }"/>"><c:out value="${masterDataTypesList.label }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td><fmt:message key="label.alternatelocale"/><span class="mandatory">*</span></td>
													<td><input type="text" id="MDLM_AlternateLocale" name="MDLM_AlternateLocale" style="width: 285px;" value="<c:out value="${masterDataLocaleMappingBean.fieldDetails.alternateLocale }" />" /></td>
												</tr>
												<tr>
													<td colspan="4" style="text-align: right; !important;">
														<input type="button" name="MDLM_Entry" id="MDLM_Entry" onclick="MDLM_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${masterDataLocaleMappingBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.masterdatalocalemapping"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 200px;"><fmt:message key="label.upload" /> <fmt:message key="label.masterdatalocalemapping"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="MDLM_File" name="MDLM_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="MDLM_file_upload();" id="MDLM_Upload" name="MDLM_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="MDLM_DownloadTemplate" onclick="MDLM_download_template();"
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
								<c:if test="${!empty masterDataLocaleMappingBean.mappingList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty masterDataLocaleMappingBean.mappingList }">
											<c:forEach var="mappingList" items="${masterDataLocaleMappingBean.mappingList }">
												<c:if test="${mappingList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty masterDataLocaleMappingBean.mappingList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="5%">#.</th>
											<th scope="col" width="15%"><fmt:message key="label.market"/> <c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="11%"><fmt:message key="label.processinglocale"/> <c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="22%"><fmt:message key="label.masterdatatype"/> <c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.alternatelocale"/> <c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.flag"/> <c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty masterDataLocaleMappingBean.mappingList }">
											<c:forEach var="mappingList" items="${masterDataLocaleMappingBean.mappingList }">
												<tr <c:if test="${mappingList.editableFlag eq true }"> class="selected" </c:if>>
													<td> 
														<input type="checkbox" name="MDLM_Selection" onchange="MDLM_checkBoxManagement();" 
															value="<c:out value="${mappingList.mappingId }" />" 
															id="MDLM_Selection_<c:out value="${mappingList.mappingId }"  />"
															<c:if test="${mappingList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${masterDataLocaleMappingBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${mappingList.srNo }" /> </td>
													<td>
														<c:if test="${mappingList.editableFlag eq false }">
															<label><c:out value="${mappingList.market }" /></label>
														</c:if>
														<c:if test="${mappingList.editableFlag eq true }">
															<select name="MDLM_List_Market<c:out value="${mappingList.mappingId }" />" 
																id="MDLM_List_Market<c:out value="${mappingList.mappingId }" />" style="width:110px;" 
																onchange="MDLM_marketSelectionDataGrid('<c:out value="${mappingList.mappingId }" />');">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty masterDataLocaleMappingBean.marketList }">
																	<c:forEach var="marketList" items="${masterDataLocaleMappingBean.marketList }">
																		<c:set var="selectedFlagModelType" value="" />
																		<c:if test="${!empty marketList.value}">
																			<c:if test="${marketList.value eq mappingList.market}">
																				<c:set var="selectedFlagModelType" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedFlagModelType eq 1}">
																				<option selected="selected" value="<c:out value="${marketList.value }"/>"><c:out value="${marketList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${marketList.value }"/>"><c:out value="${marketList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>	
														</c:if>
													</td>
													<td>
														<c:if test="${mappingList.editableFlag eq false }">
															<label><c:out value="${mappingList.locale }" /></label>
														</c:if>
														<c:if test="${mappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${mappingList.locale }" />" size="10" 
																name="MDLM_List_Locale<c:out value="${mappingList.mappingId }" />" 
																id="MDLM_List_Locale<c:out value="${mappingList.mappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${mappingList.editableFlag eq false }">
															<label><c:out value="${mappingList.masterDataTypeLabel }" /> (<c:out value="${mappingList.masterDataType }" />)</label>
														</c:if>
														<c:if test="${mappingList.editableFlag eq true }">
															<select name="MDLM_List_MasterDataType<c:out value="${mappingList.mappingId }" />" 
																id="MDLM_List_MasterDataType<c:out value="${mappingList.mappingId }" />" style="width:260px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty mappingList.masterDataTypeList }">
																	<c:forEach var="masterDataTypeList" items="${mappingList.masterDataTypeList }">
																		<c:set var="selectedFlagMasterDataType" value="" />
																		<c:if test="${!empty masterDataTypeList.value}">
																			<c:if test="${masterDataTypeList.value eq mappingList.masterDataType}">
																				<c:set var="selectedFlagMasterDataType" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedFlagMasterDataType eq 1}">
																				<option selected="selected" value="<c:out value="${masterDataTypeList.value }"/>"><c:out value="${masterDataTypeList.label }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${masterDataTypeList.value }"/>"><c:out value="${masterDataTypeList.label }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>	
														</c:if>
													</td>
													<td>
														<c:if test="${mappingList.editableFlag eq false }">
															<label><c:out value="${mappingList.alternateLocale }" /></label>
														</c:if>
														<c:if test="${mappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${mappingList.alternateLocale }" />" size="10" 
																name="MDLM_List_AlternateLocale<c:out value="${mappingList.mappingId }" />" 
																id="MDLM_List_AlternateLocale<c:out value="${mappingList.mappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${mappingList.editableFlag eq false }">
															<label><c:out value="${mappingList.flagLabel }" /></label>
														</c:if>
														<c:if test="${mappingList.editableFlag eq true }">
															<select name="MDLM_List_Flag_<c:out value="${mappingList.mappingId }" />" id="MDLM_List_Flag_<c:out value="${mappingList.mappingId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${masterDataLocaleMappingBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty mappingList.flag}">
																		<c:if test="${mappingList.flag eq flagList.value}">
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
														<fmt:formatDate value="${mappingList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${mappingList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty masterDataLocaleMappingBean.mappingList }">
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


<c:if test="${!empty masterDataLocaleMappingBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${masterDataLocaleMappingBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("MDLM_File"))
{
document.getElementById("MDLM_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty masterDataLocaleMappingBean.mappingList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty masterDataLocaleMappingBean.mappingList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${masterDataLocaleMappingBean.showUpdate eq false }">
<c:if test="${masterDataLocaleMappingBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"MDLM_edit();\" name=\"MDLM_Edit\" id=\"MDLM_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"MDLM_delete();\" name=\"MDLM_Delete\" id=\"MDLM_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"MDLM_active();\" name=\"MDLM_Active\" id=\"MDLM_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
	
</script>
</c:if>

<c:if test="${masterDataLocaleMappingBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"MDLM_export();\"  name=\"MDLM_Export\" id=\"MDLM_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>

</c:if>
<c:if test="${masterDataLocaleMappingBean.showUpdate eq true && masterDataLocaleMappingBean.showWriteControls eq true }">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"MDLM_update();\" style=\"float:right\" name=\"MDLM_Update\" id=\"MDLM_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"MDLM_Reset\" id=\"MDLM_Reset\" onclick=\"MDLM_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
	//$("body").unmask();
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
	var pageNo=$("#MDLM_DataTabel_displayPageNo").val();
	var pageLength=$("#MDLM_DataTabel_displayPageLen").val();
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
		MDLM_checkBoxManagement();
	});
}

function MDLM_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#MDLM_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#MDLM_DataTabel_displayPageLen").val(length);
	}	
	
}

function MDLM_edit()
{
	MDLM_readDataTableValues();
	var value=$("#MDLM_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#MDLM_ActionClicked").val("EDIT_MAPPING");
		$("#MDLM_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}  
}


function MDLM_delete_action()
{
	// show Loader
	$("#loader").show();
	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("DELETE_MAPPING");
	$("#MDLM_Form").submit();
}

function MDLM_active_action()
{
	// show Loader
	$("#loader").show();
	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("ACTIVE_MAPPING");
	$("#MDLM_Form").submit();
}

function MDLM_delete()
{
	$("#MDM_MDLM_Error_Message").html("");
	var value=$("#MDLM_SelectedRows").val();
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
					  MDLM_delete_action();
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


function MDLM_active()
{
	$("#MDM_MDLM_Error_Message").html("");
	var value=$("#MDLM_SelectedRows").val();
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
					  MDLM_active_action();
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


function MDLM_export()
{
	$("#MDM_MDLM_Error_Message").html("");
	var value=$("#MDLM_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		MDLM_readDataTableValues();
		$("#MDLM_ActionClicked").val("EXPORT_MAPPING");
		  $("#MDLM_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
}

function mdm_show_errorMessage(message)
{
	
	var html="<div class=\"errorMessage\">"+message+"</div>";
	$("#MDM_MDLM_Error_Message").html(html);
	window.scrollTo(0,0);
}


function MDLM_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#MDLM_ActionClicked").val("FILE_UPLOAD");
	$("#MDLM_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function MDLM_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=MASTERDATA_LOCALE_MAPPING";
}


function MDLM_entry()
{
	// show Loader
	$("#loader").show();
	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("SAVE_MAPPING");
	  $("#MDLM_Form").submit();
}

function MDLM_marketSelection()
{
	// show Loader
	$("#loader").show();
	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("MARKET_SELECTION");
	  $("#MDLM_Form").submit();
}

function MDLM_marketSelectionDataGrid(mappingId)
{
	// show Loader
	$("#loader").show();
	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("MARKET_DATATABLE_SELECTION");
	$("#MDLM_SelectedMappingIdForMasterDataType").val(mappingId);
	$("#MDLM_Form").submit();
}

function MDLM_update()
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
   	$("#MDLM_UpdatedRows").val(newData);
   	MDLM_readDataTableValues();
	$("#MDLM_ActionClicked").val("UPDATE_MAPPING");
	  $("#MDLM_Form").submit();
}

function MDLM_checkBoxManagement()
{
	$("#MDLM_SelectedRows").val("");
	var value=$("#MDLM_SelectedRows").val();
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
		$("#MDLM_SelectedRows").val(result);
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

function MDLM_reset()
{
	// show Loader
	$("#loader").show();
	$("#MDLM_ActionClicked").val("RESET_MAPPING");
	  $("#MDLM_Form").submit();
}

function MDLM_reset_withDialog()
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
				 $("#MDLM_ActionClicked").val("RESET_MAPPING");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#MDLM_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
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
		$("#MDLM_SelectedRows").val("");
	}
	MDLM_checkBoxManagement();
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
						MDLM_readDataTableValues();
						 $("#MDLM_ActionClicked").val("RESET_MAPPING");
						 $("#MDLM_Form").submit();
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