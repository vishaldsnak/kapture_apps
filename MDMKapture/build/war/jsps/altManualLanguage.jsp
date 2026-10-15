<%@page import="com.mazda.gms3.mdm.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
	request.setAttribute("PAGE_NAME", "ALT_MAN_LANG");
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
<jsp:useBean id="ruleBean" class="com.mazda.gms3.mdm.bean.RuleBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.altmanuallanguage"/> </title>
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />
<link href="css/style.css" type="text/css" rel="stylesheet" />
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
            	<a href="javascript:void(0);"><fmt:message key="label.language"/> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.altmanuallanguage"/> &rsaquo;</a>   
            </div>
            <form id="ALT_RULE_Form" name="ALT_RULE_Form" action="<%=request.getContextPath() %>/altmanuallanguage" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="ALT_RULE_SelectedRows" id="ALT_RULE_SelectedRows" value="<c:out value="${ruleBean.selectedRows }"/>" />
			<input type="hidden" name="ALT_RULE_UpdatedRows" id="ALT_RULE_UpdatedRows" value="" />
			<input type="hidden" name="ALT_RULE_ActionClicked" id="ALT_RULE_ActionClicked" value="<c:out value="${ruleBean.actionClicked }"/>" />
			<input type="hidden" name="ALT_RULE_DataTabel_displayPageNo" id="ALT_RULE_DataTabel_displayPageNo" value="<c:out value="${ruleBean.displayPageNo }"/>" />
			<input type="hidden" name="ALT_RULE_DataTabel_displayPageLen" id="ALT_RULE_DataTabel_displayPageLen" value="<c:out value="${ruleBean.displayPageLength }"/>" />
            <input type="hidden" name="ALT_RULE_Language_Delete" id="ALT_RULE_Language_Delete" value="<c:out value="${ruleBean.languageSrNo }"/>">
            <input type="hidden" name="ALT_RULE_Language_DeleteType" id="ALT_RULE_Language_DeleteType" value="<c:out value="${ruleBean.languageDelType }"/>">
            <input type="hidden" name="ALT_RULE_Model_Delete" id="ALT_RULE_Model_Delete" value="<c:out value="${ruleBean.modelSrNo }"/>">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--  <h3><fmt:message key="label.altmanuallanguage"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_ALT_RULE_Error_Message">
											<c:if test="${!empty ruleBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=ruleBean.getErrorMessage() && !"".equals(ruleBean.getErrorMessage()))
														{
															String[] msgs = ruleBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty ruleBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=ruleBean.getSuccessMessage() && !"".equals(ruleBean.getSuccessMessage()))
														{
															String[] msgs = ruleBean.getSuccessMessage().split("<MSG_TOKEN>");
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
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td>&nbsp;</td>
									</tr>
									<tr>
										<td>
											<h3>
												<fmt:message key="label.insertnewruledata"/>
											</h3>
											<br />
										</td>
									</tr>
									<tr>
										<td colspan="2">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
												<tr>
													<td><fmt:message key="label.rulename"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width:200px" id="ALT_RULE_Name" name="ALT_RULE_Name" value="<c:out value="${ruleBean.fieldDetails.ruleName }" />" /></td>
													<td><fmt:message key="label.ruledesc"/></td>
													<td>
														<textarea rows="3" id="ALT_RULE_Desc" name="ALT_RULE_Desc"  
															 style="width:340px"><c:out value="${ruleBean.fieldDetails.ruleDesc }" /></textarea>
													</td>		 
													<td style="text-align: center;">
														<input type="button" name="ALT_RULE_Entry" id="ALT_RULE_Entry" onclick="alt_rule_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
								</table>
								
								<p style="height: 45px;">&nbsp;</p>
								<c:if test="${!empty ruleBean.ruleListForCombo }">
								<h4>
									<fmt:message key="label.definemodellangmapping"/>
								</h4>
									
								<br />
								<table width="100%" cellspacing="1" cellpadding="3" style="border: 1px solid #E0E0E0;">
									<tr>
										<td colspan="4">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.select"/> <fmt:message key="label.rule"/>: <span class="mandatory">*</span></label> 
												<select id="ALT_RULE_RuleId" name="ALT_RULE_RuleId" onchange="alt_rule_submit();" style="margin:0px 50px !important;width:256px !important;">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty ruleBean.ruleListForCombo }">
														<c:forEach var="ruleListCombo" items="${ruleBean.ruleListForCombo }">
															<c:set var="ruleFlag" value="" />
															<c:if test="${!empty ruleBean.ruleId}">
																<c:if test="${ruleListCombo.ruleId eq ruleBean.ruleId}">
																	<c:set var="ruleFlag" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${ruleFlag eq 1}">
																	<option value="<c:out value="${ruleListCombo.ruleId }" />" selected="selected"><c:out value="${ruleListCombo.ruleName }" /></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${ruleListCombo.ruleId }" />"><c:out value="${ruleListCombo.ruleName }" /></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${ruleBean.defaultRule eq false }">
									<tr>
										<td colspan="4">
											<div class="formElement_row leftpadding_none">
												<label style="width: 150px;"><fmt:message key="label.model" /> / <fmt:message key="label.language"/> <fmt:message key="label.csv"/>: <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="ALT_Model_File" name="ALT_Model_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;" type="button" onclick="alt_file_upload();" id="ALT_Rule_ModelUpload" name="ALT_Rule_ModelUpload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.upload"/></button>
												<!-- <button style="margin-left:17px;padding:6px 10px;" type="button" id="AML_RULE_ToolTip" title="<fmt:message key="label.help.text.sample.csv.download"/>" 
													onclick= "window.open('SampleCSVs.zip')" class="bluebutton cursorPointer" ><i class="downloadIcon"></i><fmt:message key="label.download"/></button>
													 -->
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<tr>
										<td colspan="4">&nbsp;</td>
									</tr>
									</c:if>
									<tr>
										<td colspan="4">
											<div class="formElement_row leftpadding_none">
												<label><strong><fmt:message key="label.modelmapping"></fmt:message></strong></label> 
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${ruleBean.defaultRule eq false }">
									<c:if test="${!empty ruleBean.ruleModelsList }">
									<tr>
										<td colspan="4" style="padding: 5px 20px;">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="historyTable" style="border-left: 1px solid #E0E0E0;border-bottom: 1px solid #E0E0E0;">
												<tr>
													<th width="10%">#.</th>
													<th width="16%" style="text-transform: uppercase;"><fmt:message key="label.model"/></th>
													<th width="16%" style="text-transform: uppercase;"><fmt:message key="label.wmi"/></th>
													<th width="16%" style="text-transform: uppercase;"><fmt:message key="label.vds"/></th>
													<th width="16%" style="text-transform: uppercase;"><fmt:message key="label.visstartrange"/></th>
													<th width="16%" style="text-transform: uppercase;"><fmt:message key="label.visendrange"/></th>
													<th width="10%"></th>
												</tr>
												<c:if test="${!empty ruleBean.ruleModelsList }">
													<c:forEach var="modelsList" items="${ruleBean.ruleModelsList }">
														<tr>
															<td><c:out value="${modelsList.srNo }"/></td>
															<td><c:out value="${modelsList.carCode }"/></td>
															<td><c:out value="${modelsList.wmiCode }"/></td>
															<td><c:out value="${modelsList.vdsCode }"/></td>
															<td><c:out value="${modelsList.visStartRange }"/></td>
															<td><c:out value="${modelsList.visEndRange }"/></td>
															<td><a href="javascript:void(0);" style="text-decoration: none;" onclick="alt_rule_model_mapping_delete('<c:out value="${modelsList.srNo }"/>');"><img src="images/delete.jpg" height="20px"/></a></td>
														</tr>	
													</c:forEach>
												</c:if>
												<c:if test="${empty ruleBean.ruleModelsList }">
													<tr>
														<td colspan="6" align="center" style="text-align: center !important;"><fmt:message key="label.norecordsfound"/> </td>
													</tr>
												</c:if>
											</table>
										</td>
									</tr>
									</c:if>
									<c:if test="${empty ruleBean.ruleModelsList }">
									<tr>
										<td colspan="4" style="padding: 5px 20px;">
											<table cellspacing="0"
													width="100%" class="noRecordTable">
													<tr>
														<td width="15%">&nbsp;</td>
														<td width="70%" class="noRecordText">
															<fmt:message key="label.norecord.table.message.model.mapping" />
														</td>
														<td width="15%" style="text-align:right;">
															<i class="noRecordIcon"><i>
														</td>
													</tr>
											</table>
										</td>
									</tr>
									</c:if>
									</c:if>
									<c:if test="${ruleBean.defaultRule eq true }">
									<tr>
										<td colspan="4" style="padding: 5px 20px;">
											<table cellspacing="0"
													width="100%" class="noRecordTable">
													<tr>
														<td width="15%">&nbsp;</td>
														<td width="70%" class="noRecordText">
															<fmt:message key="label.applicable.forall.models"></fmt:message>
														</td>
														<td width="15%" style="text-align:right;">
															&nbsp;
														</td>
													</tr>
											</table>
										</td>
									</tr>
									</c:if>
									<tr>
										<td colspan="4">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="4">
											<div class="formElement_row leftpadding_none">
												<label style="width: 200px;"><strong><fmt:message key="label.languagemapping"></fmt:message></strong></label> 
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${!empty ruleBean.ruleLanguageList }">
									<tr>
										<td colspan="4" style="padding:5px 20px;">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="historyTable" style="border-left: 1px solid #E0E0E0;border-bottom: 1px solid #E0E0E0;">
												<tr>
													<th width="10%">#.</th>
													<th width="40%" ><fmt:message key="label.originallanguage"/></th>
													<th width="40%"><fmt:message key="label.alternatelanguage"/></th>
													<th width="10%"></th>
												</tr>
												<c:if test="${!empty ruleBean.ruleLanguageList }">
													<c:forEach var="ruleLanguageList" items="${ruleBean.ruleLanguageList }">
														<tr>
															<td><c:out value="${ruleLanguageList.srNo }"/></td>
															<td><c:out value="${ruleLanguageList.fromLangCode }"/></td>
															<td><c:out value="${ruleLanguageList.toLangCode }"/></td>
															<td>
																<c:if test="${ruleBean.defaultRule eq false }">
																	<a href="javascript:void(0);" style="text-decoration: none;" onclick="alt_rule_language_mapping_delete('<c:out value="${ruleLanguageList.srNo }"/>','C');"><img src="images/delete.jpg" height="20px"/></a>
																</c:if>
															</td>
														</tr>
													</c:forEach>
												</c:if>
												<c:if test="${empty ruleBean.ruleLanguageList }">
													<tr>
														<td colspan="4" align="center" style="text-align: center !important;"><fmt:message key="label.norecordsfound"/> </td>
													</tr>
												</c:if>
											</table>
										</td>
									</tr>
									</c:if>
									<c:if test="${empty ruleBean.ruleLanguageList }">
									<tr>
										<td colspan="4" style="padding: 5px 20px;">
											<table cellspacing="0"
													width="100%" class="noRecordTable">
													<tr>
														<td width="15%">&nbsp;</td>
														<td width="70%" class="noRecordText">
															<fmt:message key="label.norecord.table.message.language.mapping" />
														</td>
														<td width="15%" style="text-align:right;">
															<i class="noRecordIcon"><i>
														</td>
													</tr>
											</table>
										</td>
									</tr>
									</c:if>
									<tr>
										<td colspan="4">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="4">
											<div class="formElement_row leftpadding_none">
												<label style="width: 300px;"><strong><fmt:message key="label.wdlanguagemapping"></fmt:message></strong></label> 
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<c:if test="${!empty ruleBean.wdRuleLanguageList }">
									<tr>
										<td colspan="4" style="padding:5px 20px;">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" class="historyTable" style="border-left: 1px solid #E0E0E0;border-bottom: 1px solid #E0E0E0;">
												<tr>
													<th width="10%">#.</th>
													<th width="40%" ><fmt:message key="label.originallanguage"/></th>
													<th width="40%"><fmt:message key="label.alternatelanguage"/></th>
													<th width="10%"></th>
												</tr>
												<c:if test="${!empty ruleBean.wdRuleLanguageList }">
													<c:forEach var="wdRuleLanguageList" items="${ruleBean.wdRuleLanguageList }">
														<tr>
															<td><c:out value="${wdRuleLanguageList.srNo }"/></td>
															<td><c:out value="${wdRuleLanguageList.fromLangCode }"/></td>
															<td><c:out value="${wdRuleLanguageList.toLangCode }"/></td>
															<td>
																<c:if test="${ruleBean.defaultRule eq false }">
																	<a href="javascript:void(0);" style="text-decoration: none;" onclick="alt_rule_language_mapping_delete('<c:out value="${wdRuleLanguageList.srNo }"/>','WD');"><img src="images/delete.jpg" height="20px"/></a>
																</c:if>
															</td>
														</tr>
													</c:forEach>
												</c:if>
												<c:if test="${empty ruleBean.wdRuleLanguageList }">
													<tr>
														<td colspan="4" align="center" style="text-align: center !important;"><fmt:message key="label.norecordsfound"/> </td>
													</tr>
												</c:if>
											</table>
										</td>
									</tr>
									</c:if>
									<c:if test="${empty ruleBean.wdRuleLanguageList }">
									<tr>
										<td colspan="4" style="padding: 5px 20px;">
											<table cellspacing="0"
													width="100%" class="noRecordTable">
													<tr>
														<td width="15%">&nbsp;</td>
														<td width="70%" class="noRecordText">
															<fmt:message key="label.norecord.table.message.wd.language.mapping" />
														</td>
														<td width="15%" style="text-align:right;">
															<i class="noRecordIcon"><i>
														</td>
													</tr>
											</table>
										</td>
									</tr>
									</c:if>
									<c:if test="${!empty ruleBean.wdRuleLanguageList || !empty ruleBean.ruleLanguageList || !empty ruleBean.ruleModelsList}">
									<tr>
										<td colspan="4">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="4" style="padding-right: 20px;">
											<input type="button" style="float:right" name="UPDATE1"  onclick="alt_rule_saveMapping();" class="bluebutton cursorPointer" value="<fmt:message key="label.createupdatemapping" />"/>
										</td>
									</tr>
									</c:if>
									<tr>
										<td colspan="4">&nbsp;</td>
									</tr>
								</table>
								<p style="height: 45px;">&nbsp;</p>
								</c:if>
								<h4>
									<fmt:message key="label.master"/> <fmt:message key="label.rule"/> <fmt:message key="label.details"/>
								</h4>
								
								<br />
								<c:if test="${!empty ruleBean.ruleList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty ruleBean.ruleList }">
											<c:forEach var="ruleList" items="${ruleBean.ruleList }">
												<c:if test="${ruleList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty ruleBean.ruleList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${ruleBean.showUpdate eq true }"> disabled="disabled" </c:if> 
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="25%" ><fmt:message key="label.rulename"/> <c:if test="${ruleBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="35%"><fmt:message key="label.ruledesc"/></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${ruleBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty ruleBean.ruleList }">
											<c:forEach var="ruleList" items="${ruleBean.ruleList }">
												<tr <c:if test="${ruleList.editableFlag eq true  }">class="selected"</c:if>>
													<td> 
														<!-- CONDITION COMMENTED FOR DEPRECATED FLAG && ruleList.showCheckBox eq true-  -->
														<c:if test="${ruleList.defaultRule eq false}">
														<input type="checkbox" name="ALT_RULE_Selection" onchange="alt_rule_checkBoxManagement();" 
															value="<c:out value="${ruleList.ruleId }" />" 
															id="ALT_RULE_Selection_<c:out value="${ruleList.ruleId }"  />"
															<c:if test="${ruleList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${ruleBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
														</c:if> 	
													</td>
													<td><c:out value="${ruleList.srNo }" /> </td>
													<td style="text-align: justify !important;">
														<c:if test="${ruleList.editableFlag eq false }">
															<label><c:out value="${ruleList.ruleName }" /></label>
														</c:if>
														<c:if test="${ruleList.editableFlag eq true }">
															<input type="text" value="<c:out value="${ruleList.ruleName }" />" size="10" 
																name="ALT_RULE_RuleList_Name_<c:out value="${ruleList.ruleId }" />" 
																id="ALT_RULE_RuleList_Name_<c:out value="${ruleList.ruleId }" />"/>
														</c:if>
													</td>
													<td style="text-align: justify !important;">
														<c:if test="${ruleList.editableFlag eq false }">
															<label><c:out value="${ruleList.ruleDesc }" /></label>
														</c:if>
														<c:if test="${ruleList.editableFlag eq true }">
															<textarea rows="3" id="ALT_RULE_RuleList_Desc_<c:out value="${ruleList.ruleId }" />" 
															 name="ALT_RULE_RuleList_Desc_<c:out value="${ruleList.ruleId }" />" 
															 style="width:340px"><c:out value="${ruleList.ruleDesc }" /></textarea>
														</c:if>
													</td>
													<td>
														<c:if test="${ruleList.editableFlag eq false }">
															<label><c:out value="${ruleList.flagLabel }" /></label>
														</c:if>
														<c:if test="${ruleList.editableFlag eq true }">
															<select name="ALT_RULE_LangList_Flag_<c:out value="${ruleList.ruleId }" />" id="ALT_RULE_LangList_Flag_<c:out value="${ruleList.ruleId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${ruleBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty ruleList.flag}">
																		<c:if test="${ruleList.flag eq flagList.value}">
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
														<fmt:formatDate value="${ruleList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${ruleList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty ruleBean.ruleList }">
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
	<p style="margin-top: 5px;">
		<fmt:message key="label.resetdescription"/>
	</p>
</div>

<div id="delete_dialog-confirm" title="<fmt:message key="label.delete"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.deletedescription"/>
	</p>
</div>

<div id="model_dialog-confirm" title="<fmt:message key="label.delete.model"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.deletemodeldescription"/>
	</p>
</div>

<div id="language_dialog-confirm" title="<fmt:message key="label.delete.language"/>" class="hide">
	<p style="margin-top: 5px;">
		<fmt:message key="label.deletelanguagedescription"/>
	</p>
</div>

</body>
<script type="text/javascript" src="js/external/jquery/jquery.js"></script>
<script type="text/javascript" src="js/jquery-ui.js"></script>
<script type="text/javascript" src="js/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>

<script type="text/javascript" src="js/jquery.loadmask.js"></script>
<script type="text/javascript" src="js/input.js"></script>

<c:if test="${!empty ruleBean.ruleList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";

document.getElementById("ALT_Model_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};

</script>

</c:if>
<c:if test="${empty ruleBean.ruleList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>

<c:if test="${ruleBean.showUpdate eq false }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel="<fmt:message key="label.delete"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"alt_rule_edit();\" name=\"ALT_RULE_Edit\" id=\"ALT_RULE_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"alt_rule_delete_withDialog();\" name=\"ALT_RULE_Delete\" id=\"ALT_RULE_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	edit=null;
	deleteLabel=null;
</script>
</c:if>
<c:if test="${ruleBean.showUpdate eq true }">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"submit\" onclick=\"alt_rule_update();\" style=\"float:right\" name=\"ALT_RULE_Update\" id=\"ALT_RULE_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"ALT_RULE_Reset\" id=\"ALT_RULE_Reset\" onclick=\"alt_rule_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
	update=null;
	cancel=null;
</script>
</c:if>

<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


$(function() {
    $( "#AML_RULE_ToolTip" ).tooltip({
      position: {
        my: "center bottom-20",
        at: "center top",
        using: function( position, feedback ) {
          $( this ).css( position );
		  $( "<div>" )
            .addClass( "arrow" )
            .addClass( feedback.vertical )
            .addClass( feedback.horizontal )
			.appendTo( this );
        }
      }
    });
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
	if(version=="6" || version=="7" )
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
	var pageNo=$("#ALT_RULE_DataTabel_displayPageNo").val();
	var pageLength=$("#ALT_RULE_DataTabel_displayPageLen").val();
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
		alt_rule_checkBoxManagement();
	});
}

function alt_rule_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#ALT_RULE_DataTabel_displayPageNo").val(info.page);	
	}
	if(null!=length)
	{
		$("#ALT_RULE_DataTabel_displayPageLen").val(length);	
	}
}

function alt_rule_edit()
{
	alt_rule_readDataTableValues();
	var value=$("#ALT_RULE_SelectedRows").val();
	if(null!=value && value!="")
	{	
		$("#ALT_RULE_ActionClicked").val("EDIT_RULE");
		$("#ALT_RULE_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}
}


function alt_rule_delete()
{
	alt_rule_readDataTableValues();
	$("#ALT_RULE_ActionClicked").val("DELETE_RULE");
	$("#ALT_RULE_Form").submit();
}

function alt_rule_entry()
{
	alt_rule_readDataTableValues();
	$("#ALT_RULE_ActionClicked").val("SAVE_RULE");
	$("#ALT_RULE_Form").submit();
	
}

function alt_rule_update()
{
	var newData="";
	var table = $('#example').DataTable();
	var data = table.$('input , textarea, select').serializeArray();
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
	$("#ALT_RULE_UpdatedRows").val(newData);	
	alt_rule_readDataTableValues();
	$("#ALT_RULE_ActionClicked").val("UPDATE_RULE");
	$("#ALT_RULE_Form").submit();
}

function alt_rule_checkBoxManagement()
{
	$("#ALT_RULE_SelectedRows").val("");
	var value=$("#ALT_RULE_SelectedRows").val();
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
		$("#ALT_RULE_SelectedRows").val(result);
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

function mdm_show_errorMessage(message)
{
	var html="<div class=\"errorMessage\">"+message+"</div>";
	$("#MDM_ALT_RULE_Error_Message").html(html);
	window.scrollTo(0,0);
}

function alt_rule_delete_withDialog()
{
	$("#MDM_ALT_RULE_Error_Message").html("");
	var value=$("#ALT_RULE_SelectedRows").val();
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
				  alt_rule_delete();
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

function alt_rule_reset_withDialog()
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
				 $("#ALT_RULE_ActionClicked").val("RESET_ACTION");
		          $( this ).dialog( "close" );
				  $("#ALT_RULE_Form").submit();
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

function alt_rule_reset()
{
	$("#ALT_RULE_ActionClicked").val("RESET_ACTION");
	  $("#ALT_RULE_Form").submit();
}

function alt_rule_submit()
{
	$("#ALT_RULE_ActionClicked").val("RULE_SELECTION");
	$("#ALT_RULE_Form").submit();
}

function alt_file_upload()
{
 	$("#ALT_RULE_ActionClicked").val("FILE_UPLOAD");
	$("#ALT_RULE_Form").submit();
}


function alt_rule_model_mapping_delete(srNo)
{
	// open dialog
	$( "#model_dialog-confirm" ).removeClass('hide').dialog({
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	"<fmt:message key="label.yes"/>": function() {
	    		$("#ALT_RULE_ActionClicked").val("DELETE_MODEL_MAPPING");
	    		$("#ALT_RULE_Model_Delete").val(srNo);
		          $( this ).dialog( "close" );
				  $("#ALT_RULE_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.no"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
	$("#model_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
	$("#model_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button	
}

function alt_rule_language_mapping_delete(srNo, delType)
{
	// open dialog
	$( "#language_dialog-confirm" ).removeClass('hide').dialog({
		  closeOnEscape: false,
		  open: function(event, ui) { $(".ui-dialog-titlebar-close", ui.dialog | ui).hide(); },
	      resizable: false,
	      height:140,
	      modal: true,
	      buttons: {
	    	"<fmt:message key="label.yes"/>": function() {
	    		$("#ALT_RULE_ActionClicked").val("DELETE_LANGUAGE_MAPPING");
	    		$("#ALT_RULE_Language_Delete").val(srNo);
	    		$("#ALT_RULE_Language_DeleteType").val(delType);
		          $( this ).dialog( "close" );
				  $("#ALT_RULE_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.no"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
	$("#language_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(1).addClass("bluebutton_dialog"); // second button
	$("#language_dialog-confirm").closest(".ui-dialog").find(".ui-button").eq(2).addClass("gear_button_dialog"); // second button
}

function alt_rule_saveMapping()
{
	$("#ALT_RULE_ActionClicked").val("SAVE_MAPPING");
	 $("#ALT_RULE_Form").submit();
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
		$("#ALT_RULE_SelectedRows").val("");
	}
	alt_rule_checkBoxManagement();
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
						alt_rule_readDataTableValues();
						$("#ALT_RULE_ActionClicked").val("RESET_ACTION");
						$("#ALT_RULE_Form").submit();
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