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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SST_MASTER);
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
<jsp:useBean id="sstBean" class="com.mazda.gms3.sst.bean.SSTMasterBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.sst"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.sstmaintenance" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.sst"/> &rsaquo;</a> 
            </div>			
            <form id="SST_Form" name="SST_Form" action="<%=request.getContextPath() %>/sstmaster" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="SST_SelectedRows" id="SST_SelectedRows" value="<c:out value="${sstBean.selectedRows }"/>" />
			<input type="hidden" name="SST_ActionClicked" id="SST_ActionClicked" value="<c:out value="${sstBean.actionClicked }"/>" />
			<input type="hidden" name="SST_UpdatedRows" id="SST_UpdatedRows" value="" />
			<input type="hidden" name="SST_DataTabel_displayPageNo" id="SST_DataTabel_displayPageNo" value="<c:out value="${sstBean.displayPageNo }"/>" />
			<input type="hidden" name="SST_DataTabel_displayPageLen" id="SST_DataTabel_displayPageLen" value="<c:out value="${sstBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_SST_Error_Message" colspan="3">
											<c:if test="${!empty sstBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=sstBean.getErrorMessage() && !"".equals(sstBean.getErrorMessage()))
														{
															String[] msgs = sstBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty sstBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sstBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty sstBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sstBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="SST_CountryLocale_Code" name="SST_CountryLocale_Code" onchange="SST_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sstBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${sstBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty sstBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq sstBean.countryLocaleId}">
																	<c:set var="selectedFlagLocale" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedFlagLocale eq 1}">
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
												<select id="SST_Lang_Code" name="SST_Lang_Code" onchange="SST_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sstBean.languageList }">
														<c:forEach var="languageList" items="${sstBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq sstBean.manualLanguageId}">
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
											<td width="33%">
												&nbsp;	
											</td>
									</tr>
									<c:if test="${sstBean.showWriteControls eq true }">
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
													<td><fmt:message key="label.sstnumber"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width:200px;" id="SST_Number" name="SST_Number" value="<c:out value="${sstBean.fieldDetails.sstNumber }" />" /></td>
													<td><fmt:message key="label.sstname"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width:200px;" id="SST_Name" name="SST_Name" value="<c:out value="${sstBean.fieldDetails.sstName }" />" /></td>
													<td><fmt:message key="label.sstrevision"/></td>
													<td><input type="text" style="width:200px;" id="SST_Revision" name="SST_Revision" value="<c:out value="${sstBean.fieldDetails.sstRevision }" />" /></td>
												</tr>
												<tr>
													<td><fmt:message key="label.sstimagecode"/></td>
													<td>
														<input type="text" style="width:200px;" id="SST_SSTImageId" name="SST_SSTImageId" value="<c:out value="${sstBean.fieldDetails.sstImageCode }" />" />
														<!--  <select style="width:200px;"  id="SST_SSTImageId" name="SST_SSTImageId">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty sstBean.sstImagesList }">
																<c:forEach var="sstImagesList" items="${sstBean.sstImagesList }">
																	<c:set var="selectedFlagSSTImage" value="" />
																	<c:if test="${!empty sstImagesList.sstImageId}">
																		<c:if test="${sstImagesList.sstImageId eq sstBean.fieldDetails.sstImageId}">
																			<c:set var="selectedFlagSSTImage" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagSSTImage eq 1}">
																			<option selected="selected" value="<c:out value="${sstImagesList.sstImageId }"/>"><c:out value="${sstImagesList.sstImageCode }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${sstImagesList.sstImageId }"/>"><c:out value="${sstImagesList.sstImageCode }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
														-->
														
													</td>
													<td><fmt:message key="label.useimagecode"/></td>
													<td>
														<input type="text" style="width:200px;" id="SST_USEImageId" name="SST_USEImageId" value="<c:out value="${sstBean.fieldDetails.useImageCode }" />" />
														
														<!-- 
														<select style="width:200px;"  id="SST_USEImageId" name="SST_USEImageId">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty sstBean.useImagesList }">
																<c:forEach var="useImagesList" items="${sstBean.useImagesList }">
																	<c:set var="selectedFlagUSEImage" value="" />
																	<c:if test="${!empty useImagesList.useImageId}">
																		<c:if test="${useImagesList.useImageId eq sstBean.fieldDetails.useImageId}">
																			<c:set var="selectedFlagUSEImage" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagUSEImage eq 1}">
																			<option selected="selected" value="<c:out value="${useImagesList.useImageId }"/>"><c:out value="${useImagesList.useImageCode }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${useImagesList.useImageId }"/>"><c:out value="${useImagesList.useImageCode }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
														 -->
													</td>
													<td style="text-align: right; !important;" colspan="2">
														<input type="button" name="SST_Entry" id="SST_Entry" onclick="SST_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								
								<c:if test="${sstBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.sst"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.sst"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="SST_File" name="SST_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SST_file_upload();" id="SST_Upload" name="SST_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="SST_DownloadTemplate" onclick="SST_download_template();"
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
								<c:if test="${!empty sstBean.sstList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty sstBean.sstList }">
											<c:forEach var="sstList" items="${sstBean.sstList }">
												<c:if test="${sstList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty sstBean.sstList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${sstBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="6%">#.</th>
											<th scope="col" width="13%"><fmt:message key="label.sstnumber"/> <c:if test="${sstBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="13%"><fmt:message key="label.sstname"/> <c:if test="${sstBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="13%"><fmt:message key="label.sstrevision"/></th>
											<th scope="col" width="13%"><fmt:message key="label.sstimagecode"/></th>
											<th scope="col" width="13%"><fmt:message key="label.useimagecode"/></th>
											<th scope="col" width="11%"><fmt:message key="label.flag"/> <c:if test="${sstBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty sstBean.sstList }">
											<c:forEach var="sstList" items="${sstBean.sstList }">
												<tr <c:if test="${sstList.editableFlag eq true }"> class="selected" </c:if>>
													<td> 
														<input type="checkbox" name="SST_Selection" onchange="SST_checkBoxManagement();" 
															value="<c:out value="${sstList.sstId }" />" 
															id="SST_Selection_<c:out value="${sstList.sstId }"  />"
															<c:if test="${sstList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${sstBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${sstList.srNo }" /> </td>
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><a href="javascript:void(0);" onclick="SST_viewsstnoimage('<c:out value="${sstList.sstImagePreviewPath }"/>','<c:out value="${sstList.useImagePreviewPath }"/>','<c:out value="${sstList.sstNumber }"/>','<c:out value="${sstList.sstName }"/>');"><c:out value="${sstList.sstNumber }" /></a></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sstList.sstNumber }" />" size="10" 
																name="SST_List_Number<c:out value="${sstList.sstId }" />" 
																id="SST_List_Number<c:out value="${sstList.sstId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><c:out value="${sstList.sstName }" /></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sstList.sstName }" />" size="10" 
																name="SST_List_Name<c:out value="${sstList.sstId }" />" 
																id="SST_List_Name<c:out value="${sstList.sstId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><c:out value="${sstList.sstRevision }" /></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sstList.sstRevision }" />" size="10" 
																name="SST_List_Revision<c:out value="${sstList.sstId }" />" 
																id="SST_List_Revision<c:out value="${sstList.sstId }" />"/>
														</c:if>
													</td>
													
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><c:out value="${sstList.sstImageCode }" /></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sstList.sstImageCode }" />" size="10" 
																name="SST_List_SSTImageId<c:out value="${sstList.sstId }" />" 
																id="SST_List_SSTImageId<c:out value="${sstList.sstId }" />"/>
														</c:if>
														
														<!--  
														<c:if test="${sstList.editableFlag eq false }">
															<c:if test="${!empty sstList.sstImagePreviewPath }">
																<label><a href="javascript:void(0);" onclick="SST_viewimage('<c:out value="${ sstList.sstImagePreviewPath }"/>');"><c:out value="${sstList.sstImageCode }" /></a></label>
															</c:if>
															<c:if test="${empty sstList.sstImagePreviewPath }">
																<label><c:out value="${sstList.sstImageCode }" /></label>
															</c:if>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<select name="SST_List_SSTImageId<c:out value="${sstList.sstId }" />" 
															   id="SST_List_SSTImageId<c:out value="${sstList.sstId }" />" style="width: 110px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty sstBean.sstImagesList }">
																	<c:forEach var="sstImagesList" items="${sstBean.sstImagesList }">
																		<c:set var="selectedFlagSSTImage" value="" />
																		<c:if test="${!empty sstImagesList.sstImageId}">
																			<c:if test="${sstImagesList.sstImageId eq sstList.sstImageId}">
																				<c:set var="selectedFlagSSTImage" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedFlagSSTImage eq 1}">
																				<option selected="selected" value="<c:out value="${sstImagesList.sstImageId }"/>"><c:out value="${sstImagesList.sstImageCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${sstImagesList.sstImageId }"/>"><c:out value="${sstImagesList.sstImageCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</c:if>
														-->
													</td>
													
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><c:out value="${sstList.useImageCode }" /></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sstList.useImageCode }" />" size="10" 
																name="SST_List_USEImageId<c:out value="${sstList.sstId }" />" 
																id="SST_List_USEImageId<c:out value="${sstList.sstId }" />"/>
														</c:if>
													<!-- 
														<c:if test="${sstList.editableFlag eq false }">
															<c:if test="${!empty sstList.useImagePreviewPath }">
																<label><a href="javascript:void(0);" onclick="SST_viewimage('<c:out value="${ sstList.useImagePreviewPath }"/>');"><c:out value="${sstList.useImageCode }" /></a></label>
															</c:if>
															<c:if test="${empty sstList.useImagePreviewPath }">
																<label><c:out value="${sstList.useImageCode }" /></label>
															</c:if>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<select name="SST_List_USEImageId<c:out value="${sstList.sstId }" />" 
															   id="SST_List_USEImageId<c:out value="${sstList.sstId }" />" style="width: 110px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty sstBean.useImagesList }">
																	<c:forEach var="useImagesList" items="${sstBean.useImagesList }">
																		<c:set var="selectedFlagUSEImage" value="" />
																		<c:if test="${!empty useImagesList.useImageId}">
																			<c:if test="${useImagesList.useImageId eq sstList.useImageId}">
																				<c:set var="selectedFlagUSEImage" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedFlagUSEImage eq 1}">
																				<option selected="selected" value="<c:out value="${useImagesList.useImageId }"/>"><c:out value="${useImagesList.useImageCode }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${useImagesList.useImageId }"/>"><c:out value="${useImagesList.useImageCode }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</c:if>
														-->
													</td>
													
													<td>
														<c:if test="${sstList.editableFlag eq false }">
															<label><c:out value="${sstList.flagLabel }" /></label>
														</c:if>
														<c:if test="${sstList.editableFlag eq true }">
															<select name="SST_List_Flag_<c:out value="${sstList.sstId }" />" id="SST_List_Flag_<c:out value="${sstList.sstId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${sstBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty sstList.flag}">
																		<c:if test="${sstList.flag eq flagList.value}">
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
														<fmt:formatDate value="${sstList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${sstList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty sstBean.sstList }">
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

<div id="viewimage_dialog-confirm" title="<fmt:message key="label.preview"/>" class="hide">
	<p style="margin-top: 5px;">
		<img alt="" src="" id="SST_ImagePath" name="SST_ImagePath">
	</p>
</div>

<div id="viewsstnoimage_dialog-confirm" title="<fmt:message key="label.preview"/>" class="hide">
	<p style="margin-top: 5px;" id="SST_SSTNo">
		
	</p>
	<table width="100%" align="center" cellpadding="3" cellspacing="1" border="1">
		<tbody>
			<tr>
				<td colspan="2" style="background:#dadada; padding-left:10px;" id="SST_SSTName"></td>
			</tr>
			<tr>
				<td width="50%"><img src="" id="SST_SSTImagePath"> </td>
				<td width="50%"><img src="" id="SST_USEImagePath"> </td>
			</tr>
		</tbody>
	</table>
</div>

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<c:if test="${!empty sstBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${sstBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("SST_File"))
{
document.getElementById("SST_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty sstBean.sstList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty sstBean.sstList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${sstBean.showUpdate eq false }">
<c:if test="${sstBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SST_edit();\" name=\"SST_Edit\" id=\"SST_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SST_delete();\" name=\"SST_Delete\" id=\"SST_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SST_active();\" name=\"SST_Active\" id=\"SST_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${sstBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"SST_export();\"  name=\"SST_Export\" id=\"SST_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${sstBean.showUpdate eq true }">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"SST_update();\" style=\"float:right\" name=\"SST_Update\" id=\"SST_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"SST_Reset\" id=\"SST_Reset\" onclick=\"SST_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
                        { aTargets: [ 8 ], bSortable: true },
                        { aTargets: [ 9 ], bSortable: true }
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
	var pageNo=$("#SST_DataTabel_displayPageNo").val();
	var pageLength=$("#SST_DataTabel_displayPageLen").val();
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
		SST_checkBoxManagement();
	});
}

function SST_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#SST_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#SST_DataTabel_displayPageLen").val(length);
	}	
	
}

function SST_edit()
{
	SST_readDataTableValues();
	var value=$("#SST_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show loader
		$("#loader").show();
		$("#SST_ActionClicked").val("EDIT_SST");
		$("#SST_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}  
}

function SST_delete_action()
{
	// show loader
	$("#loader").show();	
	SST_readDataTableValues();
	$("#SST_ActionClicked").val("DELETE_SST");
	$("#SST_Form").submit();
}

function SST_active_action()
{
	// show loader
	$("#loader").show();
	SST_readDataTableValues();
	$("#SST_ActionClicked").val("ACTIVE_SST");
	$("#SST_Form").submit();
}

function SST_delete()
{
	$("#MDM_SST_Error_Message").html("");
	var value=$("#SST_SelectedRows").val();
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
					  SST_delete_action();
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


function SST_active()
{
	$("#MDM_SST_Error_Message").html("");
	var value=$("#SST_SelectedRows").val();
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
					  SST_active_action();
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


function SST_export()
{
	$("#MDM_SST_Error_Message").html("");
	var value=$("#SST_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show loader
		$("#loader").show();
		SST_readDataTableValues();
		$("#SST_ActionClicked").val("EXPORT_SST");
		  $("#SST_Form").submit();
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
	$("#MDM_SST_Error_Message").html(html);
	window.scrollTo(0,0);
}


function SST_file_upload()
{
	// show loader
	$("#loader").show();
 	$("#SST_ActionClicked").val("FILE_UPLOAD");
	$("#SST_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function SST_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=SST_MASTER";
}


function SST_entry()
{
	// show loader
	$("#loader").show();
	SST_readDataTableValues();
	$("#SST_ActionClicked").val("SAVE_SST");
	  $("#SST_Form").submit();
}



function SST_update()
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
	// show loader
	$("#loader").show();
   	$("#SST_UpdatedRows").val(newData);
   	SST_readDataTableValues();
	$("#SST_ActionClicked").val("UPDATE_SST");
	  $("#SST_Form").submit();
}

function SST_checkBoxManagement()
{
	$("#SST_SelectedRows").val("");
	var value=$("#SST_SelectedRows").val();
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
		$("#SST_SelectedRows").val(result);
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

function SST_reset()
{
	// show loader
	$("#loader").show();
	$("#SST_ActionClicked").val("RESET_SST");
	  $("#SST_Form").submit();
}

function SST_reset_withDialog()
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
				 $("#SST_ActionClicked").val("RESET_SST");
		          $( this ).dialog( "close" );
		       // show loader
		  		$("#loader").show();
				  $("#SST_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function SST_localeSelection()
{
	// show loader
	$("#loader").show();
	$("#SST_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#SST_Form").submit();
}

function SST_langSelection()
{
	// show loader
	$("#loader").show();
	$("#SST_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#SST_Form").submit();
}


function SST_viewimage(imageURL)
{
	if(null!=imageURL && imageURL!="")
	{
		// set IMAGE URL
		$("#SST_ImagePath").attr("src",imageURL);
		// open dialog
		$( "#viewimage_dialog-confirm" ).removeClass('hide').dialog({
			  closeOnEscape: false,
			  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
			  resizable: true,
			  modal: true,
			  buttons: {
				"<fmt:message key="label.close"/>": function() {
				  // RESET IMAGE URL
				  $("#SST_ImagePath").attr("src","");
					$( this ).dialog( "close" );
				  
				}
			  }
			});
		$("#viewimage_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
	}
}

function SST_viewsstnoimage(sstImageURL, useImageURL, sstNo, sstName)
{
	var noPreviewURL="<%=request.getContextPath() %>/images/no_preview_available.png";
	var appContext = "<%=ApplicationProperties.getProperty("application.environment.context") %>";
	noPreviewURL = appContext+"/"+noPreviewURL;
	// SET BY DEFAULT NO PREVIEW URL
	$("#SST_SSTImagePath").attr("src",noPreviewURL);
	if(null!=sstImageURL && sstImageURL!="")
	{
		// set IMAGE URL - CHECK Image Exists or not, if not then set no preview available
		$.ajax({
			url:sstImageURL+"?t=" + new Date().getTime(),
			cache : false,
			success:function(responseData){
				$("#SST_SSTImagePath").attr("src",sstImageURL);
			}
		});
	}	
	// SET BY DEFAULT NO PREVIEW URL
	$("#SST_USEImagePath").attr("src",noPreviewURL);
	if(null!=useImageURL && useImageURL!="")
	{
		// set IMAGE URL - CHECK Image Exists or not, if not then set no preview available
		$.ajax({
			url:useImageURL+"?t=" + new Date().getTime(),
			cache : false,
			success:function(responseData){
				$("#SST_USEImagePath").attr("src",useImageURL);
			}
		});
	}
	$("#SST_SSTNo").html(sstNo);
	$("#SST_SSTName").html(sstName);

	
	// open dialog
	$( "#viewsstnoimage_dialog-confirm" ).removeClass('hide').dialog({
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
		  resizable: true,
		  modal: true,
		  buttons: {
			"<fmt:message key="label.close"/>": function() {
			  // RESET IMAGE URL
			  $("#SST_SSTImagePath").attr("src","");
			  $("#SST_USEImagePath").attr("src","");
			  $("#SST_SSTNo").html("");
			  $("#SST_SSTName").html("");
				$( this ).dialog( "close" );
			  
			}
		  }
		});
	$("#viewsstnoimage_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("gear_button_dialog"); // second button
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
		$("#SST_SelectedRows").val("");
	}
	SST_checkBoxManagement();
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
						SST_readDataTableValues();
						 $("#SST_ActionClicked").val("RESET_SST");
						 $("#SST_Form").submit();
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