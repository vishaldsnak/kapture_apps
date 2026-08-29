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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_CVC_CATEGORY);
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
<jsp:useBean id="cvcCategoryBean" class="com.mazda.gms3.mdm.bean.CVCCategoryBean" scope="session"></jsp:useBean>

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
.ui-dialog{width:500px !important; height:auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

#report_dialog-confirm
{
	height: auto !important;
}

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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.cvccategories"/> &raquo;</a>  
            </div>
            <form id="CVC_CAT_Form" name="CVC_CAT_Form" action="<%=request.getContextPath() %>/cvccategory" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="CVC_CAT_SelectedRows" id="CVC_CAT_SelectedRows" value="<c:out value="${cvcCategoryBean.selectedRows }"/>" />
			<input type="hidden" name="CVC_CAT_UpdatedRows" id="CVC_CAT_UpdatedRows" value="" />
			<input type="hidden" name="CVC_CAT_ActionClicked" id="CVC_CAT_ActionClicked" value="<c:out value="${cvcCategoryBean.actionClicked }"/>" />
			<input type="hidden" name="CVC_CAT_DataTabel_displayPageNo" id="CVC_CAT_DataTabel_displayPageNo" value="<c:out value="${cvcCategoryBean.displayPageNo }"/>" />
			<input type="hidden" name="CVC_CAT_DataTabel_displayPageLen" id="CVC_CAT_DataTabel_displayPageLen" value="<c:out value="${cvcCategoryBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_CVC_CAT_Error_Message" colspan="3">
											<c:if test="${!empty cvcCategoryBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=cvcCategoryBean.getErrorMessage() && !"".equals(cvcCategoryBean.getErrorMessage()))
														{
															String[] msgs = cvcCategoryBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty cvcCategoryBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${cvcCategoryBean.successMessage }" />
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
												<select id="CVC_CAT_CountryLocale_Code" name="CVC_CAT_CountryLocale_Code" onchange="CVC_CAT_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty cvcCategoryBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${cvcCategoryBean.countryLocaleList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty cvcCategoryBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq cvcCategoryBean.countryLocaleId}">
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
												<select id="CVC_CAT_Lang_Code" name="CVC_CAT_Lang_Code" onchange="CVC_CAT_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty cvcCategoryBean.languageList }">
														<c:forEach var="languageList" items="${cvcCategoryBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq cvcCategoryBean.manualLanguageId}">
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
									<c:if test="${cvcCategoryBean.showButtons eq true }">
										<tr>
											<td colspan="3" align="right">
												<c:if test="${cvcCategoryBean.showReadControls eq true }">
													<input type="button" value="<fmt:message key="label.view" />" class="gear_button cursorPointer" onclick="CVC_CAT_viewBlock();">
												</c:if>
												<c:if test="${cvcCategoryBean.showWriteControls eq true }">
													<input type="button" value="<fmt:message key="label.addnew" />" class="bluebutton cursorPointer" onclick="CVC_CAT_addNewBlock();">
												</c:if>
											</td>
										</tr>
										<tr>
											<td colspan="3">&nbsp;</td>
										</tr>
									</c:if>
									
									<c:if test="${cvcCategoryBean.showView eq true }">
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
															<select id="CVC_CAT_SearchLevel1Code" name="CVC_CAT_SearchLevel1Code" 
																onchange="CVC_CAT_SearchLevel1('SEARCH_LEVEL1');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty cvcCategoryBean.searchCatLevel1List }">
																	<c:forEach var="searchCatLevel1List" items="${cvcCategoryBean.searchCatLevel1List }">
																		<c:set var="searchCatLevel1ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel1List.categoryCode}">
																			<c:if test="${searchCatLevel1List.categoryCode eq cvcCategoryBean.searchCatLevel1Code}">
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
															<select id="CVC_CAT_SearchLevel2Code" name="CVC_CAT_SearchLevel2Code" 
																onchange="CVC_CAT_SearchLevel1('SEARCH_LEVEL2');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty cvcCategoryBean.searchCatLevel2List }">
																	<c:forEach var="searchCatLevel2List" items="${cvcCategoryBean.searchCatLevel2List }">
																		<c:set var="searchCatLevel2ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel2List.subCategoryCode}">
																			<c:if test="${searchCatLevel2List.subCategoryCode eq cvcCategoryBean.searchCatLevel2Code}">
																				<c:set var="searchCatLevel2ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchCatLevel2ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchCatLevel2List.subCategoryCode }"/>"><c:out value="${searchCatLevel2List.subCategoryName }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchCatLevel2List.subCategoryCode }"/>"><c:out value="${searchCatLevel2List.subCategoryName }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													
													<tr>
														<td><fmt:message key="label.symptomcode"/></td>
														<td>
															<select id="CVC_CAT_SearchLevel3Code" name="CVC_CAT_SearchLevel3Code" 
																onchange="CVC_CAT_SearchLevel1('SEARCH_LEVEL3');" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty cvcCategoryBean.searchCatLevel3List }">
																	<c:forEach var="searchCatLevel3List" items="${cvcCategoryBean.searchCatLevel3List }">
																		<c:set var="searchCatLevel3ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel3List.symptomCode}">
																			<c:if test="${searchCatLevel3List.symptomCode eq cvcCategoryBean.searchCatLevel3Code}">
																				<c:set var="searchCatLevel3ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchCatLevel3ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchCatLevel3List.symptomCode }"/>"><c:out value="${searchCatLevel3List.symptomName }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchCatLevel3List.symptomCode }"/>"><c:out value="${searchCatLevel3List.symptomName }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
														<td><fmt:message key="label.subsymptomcode"/></td>	
														<td>
															<select id="CVC_CAT_SearchLevel4Code" name="CVC_CAT_SearchLevel4Code" style="width: 210px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty cvcCategoryBean.searchCatLevel4List }">
																	<c:forEach var="searchCatLevel4List" items="${cvcCategoryBean.searchCatLevel4List }">
																		<c:set var="searchCatLevel4ListFlag" value="" />
																		<c:if test="${!empty searchCatLevel4List.subSymptomCode}">
																			<c:if test="${searchCatLevel4List.subSymptomCode eq cvcCategoryBean.searchCatLevel4Code}">
																				<c:set var="searchCatLevel4ListFlag" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${searchCatLevel4ListFlag eq 1}">
																				<option selected="selected" value="<c:out value="${searchCatLevel4List.subSymptomCode }"/>"><c:out value="${searchCatLevel4List.subSymptomName }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${searchCatLevel4List.subSymptomCode }"/>"><c:out value="${searchCatLevel4List.subSymptomName }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</td>
													</tr>
													
													<tr>
														<td colspan="4" style="text-align:right;">
															<input type="button" onclick="CVC_CAT_search();" class="bluebutton cursorPointer" value="<fmt:message key="label.search" />"> 
															<input type="button" onclick="CVC_CAT_clear();" class="gear_button cursorPointer" value="<fmt:message key="label.clear" />">
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</c:if>
									
									
									
									
									<c:if test="${cvcCategoryBean.showAdd eq true }">
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
														<select id="CVC_CAT_AddLevel1Code" name="CVC_CAT_AddLevel1Code" 
																onchange="CVC_CAT_SearchLevel1('ADD_LEVEL1');" style="width: 210px;">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty cvcCategoryBean.addCatLevel1List }">
																<c:forEach var="addCatLevel1List" items="${cvcCategoryBean.addCatLevel1List }">
																	<c:set var="addCatLevel1ListFlag" value="" />
																	<c:if test="${!empty addCatLevel1List.categoryCode}">
																		<c:if test="${addCatLevel1List.categoryCode eq cvcCategoryBean.addCatLevel1Code}">
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
														<select id="CVC_CAT_AddLevel2Code" name="CVC_CAT_AddLevel2Code" 
															onchange="CVC_CAT_SearchLevel1('ADD_LEVEL2');"  style="width: 210px;">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty cvcCategoryBean.addCatLevel2List }">
																<c:forEach var="addCatLevel2List" items="${cvcCategoryBean.addCatLevel2List }">
																	<c:set var="addCatLevel2ListFlag" value="" />
																	<c:if test="${!empty addCatLevel2List.subCategoryCode}">
																		<c:if test="${addCatLevel2List.subCategoryCode eq cvcCategoryBean.addCatLevel2Code}">
																			<c:set var="addCatLevel2ListFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${addCatLevel2ListFlag eq 1}">
																			<option selected="selected" value="<c:out value="${addCatLevel2List.subCategoryCode }"/>"><c:out value="${addCatLevel2List.subCategoryName }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${addCatLevel2List.subCategoryCode }"/>"><c:out value="${addCatLevel2List.subCategoryName }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
												</tr>
												<c:if test="${cvcCategoryBean.addCatLevel1Code eq 'ADDNEW' }">
													<tr>
														<td><fmt:message key="label.categorycode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel1Code" 
														 	name="CVC_CAT_NewCatLevel1Code" value="<c:out value="${cvcCategoryBean.newCatLevel1Code }" />" /></td>
														<td><fmt:message key="label.categoryname.eng"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel1Name" 
															name="CVC_CAT_NewCatLevel1Name" value="<c:out value="${cvcCategoryBean.newCatLevel1Name }" />"/></td>
													</tr>
												</c:if>
													
												<c:if test="${cvcCategoryBean.addCatLevel2Code eq 'ADDNEW' }">
													<tr>
														<td><fmt:message key="label.subcategorycode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel2Code" 
															name="CVC_CAT_NewCatLevel2Code" value="<c:out value="${cvcCategoryBean.newCatLevel2Code }" />" /></td>
														<td><fmt:message key="label.subcategoryname.eng"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel2Name" 
															name="CVC_CAT_NewCatLevel2Name" value="<c:out value="${cvcCategoryBean.newCatLevel2Name }" />"/></td>
													</tr>
												</c:if>
												<tr>
													<td><fmt:message key="label.symptomcode"/><span class="mandatory">*</span></td>
													<td>
														<select id="CVC_CAT_AddLevel3Code" name="CVC_CAT_AddLevel3Code" 
																onchange="CVC_CAT_SearchLevel1('ADD_LEVEL3');" style="width: 210px;">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty cvcCategoryBean.addCatLevel3List }">
																<c:forEach var="addCatLevel3List" items="${cvcCategoryBean.addCatLevel3List }">
																	<c:set var="addCatLevel3ListFlag" value="" />
																	<c:if test="${!empty addCatLevel3List.symptomCode}">
																		<c:if test="${addCatLevel3List.symptomCode eq cvcCategoryBean.addCatLevel3Code}">
																			<c:set var="addCatLevel3ListFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${addCatLevel3ListFlag eq 1}">
																			<option selected="selected" value="<c:out value="${addCatLevel3List.symptomCode }"/>"><c:out value="${addCatLevel3List.symptomName }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${addCatLevel3List.symptomCode }"/>"><c:out value="${addCatLevel3List.symptomName }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td><fmt:message key="label.subsymptomcode"/><span class="mandatory">*</span></td>	
													<td>
														<select id="CVC_CAT_AddLevel4Code" name="CVC_CAT_AddLevel4Code" 
															onchange="CVC_CAT_addComboSelection();"  style="width: 210px;">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty cvcCategoryBean.addCatLevel4List }">
																<c:forEach var="addCatLevel4List" items="${cvcCategoryBean.addCatLevel4List }">
																	<c:set var="addCatLevel4ListFlag" value="" />
																	<c:if test="${!empty addCatLevel4List.subSymptomCode}">
																		<c:if test="${addCatLevel4List.subSymptomCode eq cvcCategoryBean.addCatLevel4Code}">
																			<c:set var="addCatLevel4ListFlag" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${addCatLevel4ListFlag eq 1}">
																			<option selected="selected" value="<c:out value="${addCatLevel4List.subSymptomCode }"/>"><c:out value="${addCatLevel4List.subSymptomName }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${addCatLevel4List.subSymptomCode }"/>"><c:out value="${addCatLevel4List.subSymptomName }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
												</tr>
												
												<c:if test="${cvcCategoryBean.addCatLevel3Code eq 'ADDNEW' }">
													<tr>
														<td><fmt:message key="label.symptomcode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel3Code" 
														 	name="CVC_CAT_NewCatLevel3Code" value="<c:out value="${cvcCategoryBean.newCatLevel3Code }" />" /></td>
														<td><fmt:message key="label.symptomname.eng"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel3Name" 
															name="CVC_CAT_NewCatLevel3Name" value="<c:out value="${cvcCategoryBean.newCatLevel3Name }" />"/></td>
													</tr>
												</c:if>
													
												<c:if test="${cvcCategoryBean.addCatLevel4Code eq 'ADDNEW' }">
													<tr>
														<td><fmt:message key="label.subsymptomcode"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel4Code" 
															name="CVC_CAT_NewCatLevel4Code" value="<c:out value="${cvcCategoryBean.newCatLevel4Code }" />" /></td>
														<td><fmt:message key="label.subsymptomname.eng"/><span class="mandatory">*</span></td>
														<td><input type="text" style="width: 200px;" id="CVC_CAT_NewCatLevel4Name" 
															name="CVC_CAT_NewCatLevel4Name" value="<c:out value="${cvcCategoryBean.newCatLevel4Name }" />"/></td>
													</tr>
												</c:if>
												
												<tr>
													<td><fmt:message key="label.conditioncode"/><span class="mandatory">*</span></td>
													<td><input type="text" id="CVC_CAT_Condition_Code" style="width: 200px;" name="CVC_CAT_Condition_Code" value="<c:out value="${cvcCategoryBean.fieldDetails.conditionCode }" />" /></td>
													<td><fmt:message key="label.conditionname.eng"/><span class="mandatory">*</span></td>
													<td><input type="text" id="CVC_CAT_Condition_Name" style="width: 200px;" name="CVC_CAT_Condition_Name" value="<c:out value="${cvcCategoryBean.fieldDetails.conditionName }" />"/></td>
												</tr>
												<tr>
													<td><fmt:message key="label.rank"/><span class="mandatory">*</span></td>
													<td><input type="text" id="CVC_CAT_Rank" style="width: 200px;" name="CVC_CAT_Rank" value="<c:out value="${cvcCategoryBean.fieldDetails.rank }" />" /></td>
													<td style="text-align: right;" colspan="2">
														<input type="button" name="CVC_CAT_Entry" id="CVC_CAT_Entry" onclick="CVC_CAT_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${cvcCategoryBean.showAdd eq true }">
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
													<input id="CVC_CAT_File" name="CVC_CAT_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="CVC_CAT_file_upload();" id="CVC_CAT_Upload" name="CVC_CAT_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="CVC_CAT_DownloadTemplate" onclick="CVC_CAT_download_template();"
										title="<fmt:message key="label.download.template"/>"><fmt:message key="label.download.template"/></a>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
								</table>
								</c:if>
								
								<c:if test="${cvcCategoryBean.showView eq true || cvcCategoryBean.showAdd eq true }">
								<p style="height: 45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty cvcCategoryBean.categoryList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty cvcCategoryBean.categoryList }">
											<c:forEach var="categoryList" items="${cvcCategoryBean.categoryList }">
												<c:if test="${categoryList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty cvcCategoryBean.categoryList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${cvcCategoryBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="3%">#.</th>
											<th scope="col" width="5%" ><fmt:message key="label.categorycode"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.categoryname.eng"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="6%" ><fmt:message key="label.subcategorycode"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.subcategoryname.eng"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="5%" ><fmt:message key="label.symptomcode"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.symptomname.eng"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="6%" ><fmt:message key="label.subsymptomcode"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.subsymptomname.eng"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="5%" ><fmt:message key="label.conditioncode"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.conditionname.eng"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="4%" ><fmt:message key="label.rank"/> <c:if test="${cvcCategoryBean.showUpdate eq true }"><span class="mandatory">*</span></c:if></th>
											<th scope="col" width="8%"><fmt:message key="label.flag"/> </th>
											<th scope="col" width="5%"><fmt:message key="label.imsyncstatus"/></th>
											<th scope="col" width="5%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="5%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty cvcCategoryBean.categoryList }">
											<c:forEach var="categoryList" items="${cvcCategoryBean.categoryList }">
												<tr <c:if test="${categoryList.editableFlag eq true  }">class="selected"</c:if>>
													<td> 
														<input type="checkbox" name="CVC_CAT_Selection" onchange="CVC_CAT_checkBoxManagement();" 
															value="<c:out value="${categoryList.categoryId }" />" 
															id="CVC_CAT_Selection_<c:out value="${categoryList.categoryId }"  />"
															<c:if test="${categoryList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${cvcCategoryBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${categoryList.srNo }" /> </td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.categoryCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.categoryCode }" />" size="10" 
																name="CVC_CAT_CategoryList_Code_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_Code_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.categoryNameEng }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.categoryNameEng }" />" size="10" 
																name="CVC_CAT_CategoryList_Name_En_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_Name_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subCategoryCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.subCategoryCode }" />" size="10" 
																name="CVC_CAT_CategoryList_SubCatCode_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SubCatCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subCategoryName }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.subCategoryName }" />" size="10" 
																name="CVC_CAT_CategoryList_SubCatName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SubCatName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.symptomCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.symptomCode }" />" size="10" 
																name="CVC_CAT_CategoryList_SymCode_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SymCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.symptomName }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.symptomName }" />" size="10" 
																name="CVC_CAT_CategoryList_SymName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SymName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subSymptomCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.subSymptomCode }" />" size="10" 
																name="CVC_CAT_CategoryList_SubSymCode_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SubSymCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.subSymptomName }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.subSymptomName }" />" size="10" 
																name="CVC_CAT_CategoryList_SubSymName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_SubSymName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.conditionCode }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.conditionCode }" />" size="10" 
																name="CVC_CAT_CategoryList_ConCode_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_ConCode_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.conditionName }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:55px !important;" type="text" value="<c:out value="${categoryList.conditionName }" />" size="10" 
																name="CVC_CAT_CategoryList_ConName_En_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_ConName_En_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.rank }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<input style="width:20px !important;" type="text" value="<c:out value="${categoryList.rank }" />" size="10" 
																name="CVC_CAT_CategoryList_Rank_<c:out value="${categoryList.categoryId }" />" 
																id="CVC_CAT_CategoryList_Rank_<c:out value="${categoryList.categoryId }" />"/>
														</c:if>
													</td>
													
													<td>
														<c:if test="${categoryList.editableFlag eq false }">
															<label><c:out value="${categoryList.flagLabel }" /></label>
														</c:if>
														<c:if test="${categoryList.editableFlag eq true }">
															<select name="CVC_CAT_LangList_Flag_<c:out value="${categoryList.categoryId }" />" id="CVC_CAT_LangList_Flag_<c:out value="${categoryList.categoryId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${cvcCategoryBean.flagList }">
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
								<c:if test="${empty cvcCategoryBean.categoryList }">
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


</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->




<div id="report_dialog-confirm" title="<fmt:message key="label.export" />" class="hide" style="height: auto !important;">
<table width="100%" class="display historyTable" cellpadding="0" cellspacing="0" id="reportDetailsBlock">
	
</table>
</div>


<c:if test="${!empty cvcCategoryBean.reportViewPath  }">
<script>
var reportURL = "<c:out value="${cvcCategoryBean.reportViewPath  }"/>";
window.open(reportURL, "_BLANK");	
</script>	
</c:if>

<script>
if(null!=document.getElementById("CVC_CAT_File"))
{
document.getElementById("CVC_CAT_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>


<c:if test="${!empty cvcCategoryBean.categoryList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty cvcCategoryBean.categoryList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${cvcCategoryBean.showUpdate eq false }">
<c:if test="${cvcCategoryBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel ="<fmt:message key="label.delete"/>";
	var activeLabel = "<fmt:message key="flag.label.active"  />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CVC_CAT_edit();\" name=\"CVC_CAT_Edit\" id=\"CVC_CAT_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CVC_CAT_delete();\" name=\"CVC_CAT_Delete\" id=\"CVC_CAT_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"CVC_CAT_active();\" name=\"CVC_CAT_Active\" id=\"CVC_CAT_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${cvcCategoryBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"CVC_CAT_export();\"  name=\"CVC_CAT_Export\" id=\"CVC_CAT_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>

</c:if>
<c:if test="${cvcCategoryBean.showUpdate eq true && cvcCategoryBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"CVC_CAT_update();\" style=\"float:right\" name=\"CVC_CAT_Update\" id=\"CVC_CAT_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"CVC_CAT_Reset\" id=\"CVC_CAT_Reset\" onclick=\"CVC_CAT_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
                        { aTargets: [ 7 ], bSortable: true },
                        { aTargets: [ 8 ], bSortable: true },
                        { aTargets: [ 9 ], bSortable: true },
                        { aTargets: [ 10 ], bSortable: true },
                        { aTargets: [ 11 ], bSortable: true },
                        { aTargets: [ 12 ], bSortable: true },
                        { aTargets: [ 13 ], bSortable: true },
                        { aTargets: [ 14 ], bSortable: true },
                        { aTargets: [ 15 ], bSortable: true },
                        { aTargets: [ 16 ], bSortable: true }
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
	var pageNo=$("#CVC_CAT_DataTabel_displayPageNo").val();
	var pageLength=$("#CVC_CAT_DataTabel_displayPageLen").val();
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
		CVC_CAT_checkBoxManagement();
	});
}

function CVC_CAT_readDataTableValues()
{
	var table = $('#example').DataTable();
	if(null!=table && table!='undefined')
	{
		if(null!=table.page && table.page!='undefined')
		{
			var info = table.page.info();
			var length = table.page.len();
			// update data table display page & length
			if(null!=info && info!="undefined")
			{
				$("#CVC_CAT_DataTabel_displayPageNo").val(info.page);	
			}
			if(null!=length && length!="undefined")
			{
				$("#CVC_CAT_DataTabel_displayPageLen").val(length);
			}	
		}	
	}
}

function CVC_CAT_edit()
{
	CVC_CAT_readDataTableValues();
	var value=$("#CVC_CAT_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show Loader
		$("#loader").show();
		$("#CVC_CAT_ActionClicked").val("EDIT");
		$("#CVC_CAT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}

function CVC_CAT_delete_action()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("DELETE");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_active_action()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("ACTIVE");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_file_upload()
{
	// show Loader
	$("#loader").show();
 	$("#CVC_CAT_ActionClicked").val("FILE_UPLOAD");
	$("#CVC_CAT_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function CVC_CAT_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=CVC_CATEGORY";
}

function CVC_CAT_delete()
{
	$("#MDM_CVC_CAT_Error_Message").html("");
	var value=$("#CVC_CAT_SelectedRows").val();
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
					  CVC_CAT_delete_action();
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


function CVC_CAT_active()
{
	$("#MDM_CVC_CAT_Error_Message").html("");
	var value=$("#CVC_CAT_SelectedRows").val();
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
					  CVC_CAT_active_action();
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


function mdm_show_errorMessage(message)
{
	var html="<div class=\"errorMessage\">"+message+"</div>";
	$("#MDM_CVC_CAT_Error_Message").html(html);
	window.scrollTo(0,0);
}

function CVC_CAT_entry()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("SAVE");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_update()
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
  	$("#CVC_CAT_UpdatedRows").val(newData);
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("UPDATE");
	$("#CVC_CAT_Form").submit();
}


function CVC_CAT_export()
{
	$("#MDM_CVC_CAT_Error_Message").html("");
	var value=$("#CVC_CAT_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show Loader
		$("#loader").show();
		CVC_CAT_readDataTableValues();
		$("#CVC_CAT_ActionClicked").val("EXPORT_CATEGORY");
		  $("#CVC_CAT_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.export" />";
		mdm_show_errorMessage(message);
	}	
}


function CVC_CAT_checkBoxManagement()
{
	$("#CVC_CAT_SelectedRows").val("");
	var value=$("#CVC_CAT_SelectedRows").val();
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
		$("#CVC_CAT_SelectedRows").val(result);
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

function CVC_CAT_reset_withDialog()
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
				 $("#CVC_CAT_ActionClicked").val("RESET");
		          $( this ).dialog( "close" );
		       // show Loader
					$("#loader").show();
		          $("#CVC_CAT_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function CVC_CAT_reset()
{
	// show Loader
	$("#loader").show();
	$("#CVC_CAT_ActionClicked").val("RESET");
	  $("#CVC_CAT_Form").submit();
 }

function CVC_CAT_localeSelection()
{
	// show Loader
	$("#loader").show();
	$("#CVC_CAT_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#CVC_CAT_Form").submit();
}

function CVC_CAT_langSelection()
{
	// show Loader
	$("#loader").show();
	$("#CVC_CAT_ActionClicked").val("LANG_SELECTION");
	 $("#CVC_CAT_Form").submit();
}

function CVC_CAT_viewBlock()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("VIEW_BUTTON");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_addNewBlock()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("ADD_NEW_BUTTON");
	$("#CVC_CAT_Form").submit();	
}

function CVC_CAT_search()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("SEARCH_CATEGORY");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_clear()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("CLEAR_SEARCH");
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_SearchLevel1(operationType)
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val(operationType);
	$("#CVC_CAT_Form").submit();
}

function CVC_CAT_addComboSelection()
{
	// show Loader
	$("#loader").show();
	CVC_CAT_readDataTableValues();
	$("#CVC_CAT_ActionClicked").val("");
	$("#CVC_CAT_Form").submit();
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
		$("#CVC_CAT_SelectedRows").val("");
	}
	CVC_CAT_checkBoxManagement();
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
						 CVC_CAT_readDataTableValues();
						 $("#CVC_CAT_ActionClicked").val("RESET");
						 $("#CVC_CAT_Form").submit();
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