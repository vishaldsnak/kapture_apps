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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_ESI_CATEGORY);
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
<jsp:useBean id="categoryBean" class="com.mazda.gms3.mdm.bean.CategoryBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.category"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.esicategories"/> &raquo;</a>  <!-- <a href="javascript:void(0);"><fmt:message key="label.category"/> &rsaquo;</a>  -->   
            </div>
            <form id="CAT_Form" name="CAT_Form" action="<%=request.getContextPath() %>/category" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="CAT_SelectedRows" id="CAT_SelectedRows" value="<c:out value="${categoryBean.selectedRows }"/>" />
			<input type="hidden" name="CAT_UpdatedRows" id="CAT_UpdatedRows" value="" />
			<input type="hidden" name="CAT_ActionClicked" id="CAT_ActionClicked" value="<c:out value="${categoryBean.actionClicked }"/>" />
			<input type="hidden" name="CAT_DataTabel_displayPageNo" id="CAT_DataTabel_displayPageNo" value="<c:out value="${categoryBean.displayPageNo }"/>" />
			<input type="hidden" name="CAT_DataTabel_displayPageLen" id="CAT_DataTabel_displayPageLen" value="<c:out value="${categoryBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_CAT_Error_Message" colspan="3">
											<c:if test="${!empty categoryBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=categoryBean.getErrorMessage() && !"".equals(categoryBean.getErrorMessage()))
														{
															String[] msgs = categoryBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty categoryBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${categoryBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty categoryBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${categoryBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="CAT_CountryLocale_Code" name="CAT_CountryLocale_Code" onchange="CAT_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty categoryBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${categoryBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty categoryBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq categoryBean.countryLocaleId}">
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
												<select id="CAT_Lang_Code" name="CAT_Lang_Code" onchange="CAT_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty categoryBean.languageList }">
														<c:forEach var="languageList" items="${categoryBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq categoryBean.manualLanguageId}">
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
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<c:if test="${categoryBean.showButtons eq true }">
										<tr>
											<td colspan="3" align="right">
												<c:if test="${categoryBean.showReadControls eq true }">
													<input type="button" value="<fmt:message key="label.view" />" class="gear_button cursorPointer" onclick="CAT_viewBlock();">
												</c:if>
												<c:if test="${categoryBean.showWriteControls eq true }">
													<input type="button" value="<fmt:message key="label.addnew" />" class="bluebutton cursorPointer" onclick="CAT_addNewBlock();">
												</c:if>
											</td>
										</tr>
										<tr>
											<td colspan="3">&nbsp;</td>
										</tr>
									</c:if>
									
									<c:if test="${categoryBean.showView eq true }">
										<tr>
											<td colspan="3">
												<h3>
													<fmt:message key="label.search"/> <fmt:message key="label.master"/> <fmt:message key="label.details"/>
												</h3>
												<br />
											</td>
										</tr>
										<tr>
											<td colspan="3">
												<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
													<tr>
														<td><fmt:message key="label.categorycode"/></td>
														<td>
															<select id="CAT_SearchLevel1Code" name="CAT_SearchLevel1Code" 
																onchange="CAT_SearchLevel1('SEARCH_LEVEL1');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty categoryBean.searchCatLevel1List }">
																	<c:forEach var="searchCatLevel1List" items="${categoryBean.searchCatLevel1List }">
																		<c:set var="searchCatLevel1ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel1List.categoryCode}">
																			<c:if test="${searchCatLevel1List.categoryCode eq categoryBean.searchCatLevel1Code}">
																				<c:set var="searchCatLevel1ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchCatLevel1ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchCatLevel1List.categoryCode }"/>"><c:out value="${searchCatLevel1List.categoryNameEng }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchCatLevel1List.categoryCode }"/>"><c:out value="${searchCatLevel1List.categoryNameEng }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.subcategorycode"/></td>	
														<td>
															<select id="CAT_SearchLevel2Code" name="CAT_SearchLevel2Code" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty categoryBean.searchCatLevel2List }">
																	<c:forEach var="searchCatLevel2List" items="${categoryBean.searchCatLevel2List }">
																		<c:set var="searchCatLevel2ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel2List.subCategoryCode}">
																			<c:if test="${searchCatLevel2List.subCategoryCode eq categoryBean.searchCatLevel2Code}">
																				<c:set var="searchCatLevel2ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchCatLevel2ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchCatLevel2List.subCategoryCode }"/>"><c:out value="${searchCatLevel2List.subCategoryNameEng }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchCatLevel2List.subCategoryCode }"/>"><c:out value="${searchCatLevel2List.subCategoryNameEng }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													<tr>
														<td colspan="4" style="text-align:right;">
															<input type="button" onclick="CAT_search();" class="bluebutton cursorPointer" value="<fmt:message key="label.search" />"> 
															<input type="button" onclick="CAT_clear();" class="gear_button cursorPointer" value="<fmt:message key="label.clear" />">
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</c:if>
									
									<c:if test="${categoryBean.showAdd eq true }">
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
														<td><fmt:message key="label.categorycode"/><span class="mandatory">*</span></td>
														<td>
															<select id="CAT_AddLevel1Code" name="CAT_AddLevel1Code" 
																onchange="CAT_SearchLevel1('ADD_LEVEL1');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty categoryBean.addCatLevel1List }">
																	<c:forEach var="addCatLevel1List" items="${categoryBean.addCatLevel1List }">
																		<c:set var="addCatLevel1ListFlag" value="" />
																		<c:if test="${!empty addCatLevel1List.categoryCode}">
																			<c:if test="${addCatLevel1List.categoryCode eq categoryBean.addCatLevel1Code}">
																				<c:set var="addCatLevel1ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addCatLevel1ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addCatLevel1List.categoryCode }"/>"><c:out value="${addCatLevel1List.categoryNameEng }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addCatLevel1List.categoryCode }"/>"><c:out value="${addCatLevel1List.categoryNameEng }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.subcategorycode"/><span class="mandatory">*</span></td>	
														<td>
															<select id="CAT_AddLevel2Code" name="CAT_AddLevel2Code" 
																onchange="CAT_addComboSelection();" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty categoryBean.addCatLevel2List }">
																	<c:forEach var="addCatLevel2List" items="${categoryBean.addCatLevel2List }">
																		<c:set var="addCatLevel2ListFlag" value="" />
																		<c:if test="${!empty addCatLevel2List.subCategoryCode}">
																			<c:if test="${addCatLevel2List.subCategoryCode eq categoryBean.addCatLevel2Code}">
																				<c:set var="addCatLevel2ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${addCatLevel2ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${addCatLevel2List.subCategoryCode }"/>"><c:out value="${addCatLevel2List.subCategoryNameEng }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${addCatLevel2List.subCategoryCode }"/>"><c:out value="${addCatLevel2List.subCategoryNameEng }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													
													<c:if test="${categoryBean.addCatLevel1Code eq 'ADDNEW' }">
														<tr>
															<td><fmt:message key="label.categorycode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" id="CAT_NewCatLevel1Code" 
															 	name="CAT_NewCatLevel1Code" value="<c:out value="${categoryBean.newCatLevel1Code }" />" /></td>
															<td><fmt:message key="label.categoryname.eng"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" id="CAT_NewCatLevel1Name" 
																name="CAT_NewCatLevel1Name" value="<c:out value="${categoryBean.newCatLevel1Name }" />"/></td>
														</tr>
													</c:if>
													
													<c:if test="${categoryBean.addCatLevel2Code eq 'ADDNEW' }">
														<tr>
															<td><fmt:message key="label.subcategorycode"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" id="CAT_NewCatLevel2Code" 
																name="CAT_NewCatLevel2Code" value="<c:out value="${categoryBean.newCatLevel2Code }" />" /></td>
															<td><fmt:message key="label.subcategoryname.eng"/><span class="mandatory">*</span></td>
															<td><input type="text" style="width: 200px;" id="CAT_NewCatLevel2Name" 
																name="CAT_NewCatLevel2Name" value="<c:out value="${categoryBean.newCatLevel2Name }" />"/></td>
														</tr>
													</c:if>
													
													<tr>
														<td><fmt:message key="label.subsubcategorycode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CAT_SubSubCategory_Code" name="CAT_SubSubCategory_Code" value="<c:out value="${categoryBean.fieldDetails.subSubCategoryCode }" />" /></td>
														<td><fmt:message key="label.subsubcategoryname.eng"/></td>
														<td><input type="text" style="width: 200px;" id="CAT_SubSubCategory_Name_En" name="CAT_SubSubCategory_Name_En" value="<c:out value="${categoryBean.fieldDetails.subSubCategoryNameEng }" />"/></td>
													</tr>
													<tr>
														<td style="text-align: right;" colspan="4">
															<input type="button" name="CAT_Entry" id="CAT_Entry" onclick="CAT_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</c:if>
								</table>
								<c:if test="${categoryBean.showAdd eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.category"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 180px;"><fmt:message key="label.upload" /> <fmt:message key="label.category"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="CAT_File" name="CAT_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="CAT_file_upload();" id="CAT_Upload" name="CAT_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="CAT_DownloadTemplate" onclick="CAT_download_template();"
										title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								
								<c:if test="${categoryBean.showView eq true || categoryBean.showAdd eq true }">
								<p style="height: 45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty categoryBean.categoryList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty categoryBean.categoryList }">
											<c:forEach var="categoryList" items="${categoryBean.categoryList }">
												<c:if test="${categoryList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty categoryBean.categoryList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${categoryBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="8%" ><fmt:message key="label.categorycode"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.categoryname.eng"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="11%" ><fmt:message key="label.subcategorycode"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.subcategoryname.eng"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="11%" ><fmt:message key="label.subsubcategorycode"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="13%"><fmt:message key="label.subsubcategoryname.eng"/></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${categoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.imsyncstatus"/></th>
											<th scope="col" width="6%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="7%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty categoryBean.categoryList }">
											<c:forEach var="categoryList" items="${categoryBean.categoryList }">
												<tr <c:if test="${categoryList.editableFlag eq true  }">class="selected"</c:if>>
													<td> 
														<input type="checkbox" name="CAT_Selection" onchange="CAT_checkBoxManagement();" 
															value="<c:out value="${categoryList.categoryId }" />" 
															id="CAT_Selection_<c:out value="${categoryList.categoryId }"  />"
															<c:if test="${categoryList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${categoryBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${categoryList.srNo }" /> </td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.categoryCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.categoryCode }" />" size="10" 
																name="CAT_CategoryList_Code_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_Code_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.categoryNameEng }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:105px !important;" type="text" value="<c:out value="${categoryList.categoryNameEng }" />" size="10" 
																name="CAT_CategoryList_Name_En_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_Name_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subCategoryCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.subCategoryCode }" />" size="10" 
																name="CAT_CategoryList_SubCatCode_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_SubCatCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subCategoryNameEng }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:105px !important;" type="text" value="<c:out value="${categoryList.subCategoryNameEng }" />" size="10" 
																name="CAT_CategoryList_SubCatName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_SubCatName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subSubCategoryCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.subSubCategoryCode }" />" size="10" 
																name="CAT_CategoryList_SubSubCatCode_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_SubSubCatCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subSubCategoryNameEng }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:105px !important;" type="text" value="<c:out value="${categoryList.subSubCategoryNameEng }" />" size="10" 
																name="CAT_CategoryList_SubSubCatName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CAT_CategoryList_SubSubCatName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.flagLabel }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<select name="CAT_LangList_Flag_<c:out value="${categoryList.categoryId }" />" id="CAT_LangList_Flag_<c:out value="${categoryList.categoryId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${categoryBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty categoryList.flag}">
																		<c:if test="${categoryList.flag eq flagList.value}">
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
														<c:if test="${!empty categoryList.syncStatus }">
															<c:out value="${categoryList.syncStatus }"/>
														</c:if>
														<c:if test="${empty categoryList.syncStatus }">-</c:if>
													</td>
													<td>
														<fmt:formatDate value="${categoryList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${categoryList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty categoryBean.categoryList }">
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


<div id="report_dialog-confirm" title="<fmt:message key="label.export" />" class="hide" style="height: auto !important;">
<table width="100%" class="display historyTable" cellpadding="0" cellspacing="0" id="reportDetailsBlock">
	
</table>
</div>



</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->



<c:if test="${!empty categoryBean.reportViewPath  }">
<script>
var reportURL = "<c:out value="${categoryBean.reportViewPath  }"/>";
window.open(reportURL, "_BLANK");	
</script>	
</c:if>

<script>
if(null!=document.getElementById("CAT_File"))
{
document.getElementById("CAT_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>



<c:if test="${!empty categoryBean.categoryList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty categoryBean.categoryList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${categoryBean.showUpdate eq false }">
<c:if test="${categoryBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel ="<fmt:message key="label.delete"/>";
	var activeLabel = "<fmt:message key="flag.label.active"  />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CAT_edit();\" name=\"CAT_Edit\" id=\"CAT_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CAT_delete();\" name=\"CAT_Delete\" id=\"CAT_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CAT_active();\" name=\"CAT_Active\" id=\"CAT_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${categoryBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"CAT_export();\"  name=\"CAT_Export\" id=\"CAT_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${categoryBean.showUpdate eq true && categoryBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"submit\" onclick=\"CAT_update();\" style=\"float:right\" name=\"CAT_Update\" id=\"CAT_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"CAT_Reset\" id=\"CAT_Reset\" onclick=\"CAT_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
	// unmask body content
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
		"deferRender": true,
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
                        { aTargets: [ 9 ], bSortable: true },
                        { aTargets: [ 10 ], bSortable: true },
                        { aTargets: [ 11 ], bSortable: true }
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
	var pageNo=$("#CAT_DataTabel_displayPageNo").val();
	var pageLength=$("#CAT_DataTabel_displayPageLen").val();
	// first set pageLength
	if(null!=pageLength && pageLength!="")
	{
		table.page.len(pageLength);
		//table.draw();
	}
	// then pageNo
	if(null!=pageNo && pageNo!="")
	{
		var act = parseInt(pageNo);
		table.page(act).draw(false);
	}
	
	// Call on Search to control top level check box selection
	table.on( 'search.dt', function () {
		CAT_checkBoxManagement();
	});
}

function CAT_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#CAT_DataTabel_displayPageNo").val(info.page);	
	}	
	if(null!=length)
	{
		$("#CAT_DataTabel_displayPageLen").val(length);
	}	
	
}

function CAT_edit()
{
	CAT_readDataTableValues();
	var value=$("#CAT_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#CAT_ActionClicked").val("EDIT");
		$("#CAT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}

function CAT_delete_action()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("DELETE");
	$("#CAT_Form").submit();
}

function CAT_delete()
{
	$("#MDM_CAT_Error_Message").html("");
	var value=$("#CAT_SelectedRows").val();
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
					  CAT_delete_action();
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

function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_CAT_Error_Message").html(html);
	window.scrollTo(0,0);
}

function CAT_entry()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("SAVE");
	  $("#CAT_Form").submit();
}

function CAT_update()
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
  	$("#CAT_UpdatedRows").val(newData);
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("UPDATE");
	$("#CAT_Form").submit();
}

function CAT_checkBoxManagement()
{
	$("#CAT_SelectedRows").val("");
	var value=$("#CAT_SelectedRows").val();
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
		$("#CAT_SelectedRows").val(result);
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

function CAT_reset_withDialog()
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
				 $("#CAT_ActionClicked").val("RESET");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#CAT_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function CAT_reset()
{
	// show Loader
	$("#loader").show();
	$("#CAT_ActionClicked").val("RESET");
	  $("#CAT_Form").submit();
 }
 

function CAT_active_action()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("ACTIVE");
	$("#CAT_Form").submit();
}

function CAT_viewBlock()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("VIEW_BUTTON");
	$("#CAT_Form").submit();
}

function CAT_addNewBlock()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("ADD_NEW_BUTTON");
	$("#CAT_Form").submit();	
}

function CAT_search()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("SEARCH_CATEGORY");
	$("#CAT_Form").submit();
}

function CAT_clear()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("CLEAR_SEARCH");
	$("#CAT_Form").submit();
}

function CAT_SearchLevel1(operationType)
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val(operationType);
	$("#CAT_Form").submit();
}

function CAT_addComboSelection()
{
	// show Loader
	$("#loader").show();
	CAT_readDataTableValues();
	$("#CAT_ActionClicked").val("");
	$("#CAT_Form").submit();
}


function CAT_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#CAT_ActionClicked").val("FILE_UPLOAD");
	$("#CAT_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function CAT_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=ESI_CATEGORY";
}
 

function CAT_active()
{
	$("#MDM_CAT_Error_Message").html("");
	var value=$("#CAT_SelectedRows").val();
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
					  CAT_active_action();
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

 
function CAT_export()
{
	$("#MDM_CAT_Error_Message").html("");
	var value=$("#CAT_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		CAT_readDataTableValues();
		$("#CAT_ActionClicked").val("EXPORT_CATEGORY");
		  $("#CAT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
} 

function CAT_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#CAT_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#CAT_Form").submit();
}

function CAT_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#CAT_ActionClicked").val("LANG_SELECTION");
	 $("#CAT_Form").submit();
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
		$("#CAT_SelectedRows").val("");
	}
	CAT_checkBoxManagement();
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
						 CAT_readDataTableValues();
						 $("#CAT_ActionClicked").val("RESET");
						 $("#CAT_Form").submit();
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