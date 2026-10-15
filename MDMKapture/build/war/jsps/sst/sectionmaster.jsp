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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SECTION_MASTER);
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
<jsp:useBean id="sectionBean" class="com.mazda.gms3.sst.bean.SectionMasterBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.section"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.sstmaintenance" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.section"/> &rsaquo;</a> 
            </div>			
            <form id="SEC_Form" name="SEC_Form" action="<%=request.getContextPath() %>/sectionmaster" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="SEC_SelectedRows" id="SEC_SelectedRows" value="<c:out value="${sectionBean.selectedRows }"/>" />
			<input type="hidden" name="SEC_ActionClicked" id="SEC_ActionClicked" value="<c:out value="${sectionBean.actionClicked }"/>" />
			<input type="hidden" name="SEC_UpdatedRows" id="SEC_UpdatedRows" value="" />
			<input type="hidden" name="SEC_DataTabel_displayPageNo" id="SEC_DataTabel_displayPageNo" value="<c:out value="${sectionBean.displayPageNo }"/>" />
			<input type="hidden" name="SEC_DataTabel_displayPageLen" id="SEC_DataTabel_displayPageLen" value="<c:out value="${sectionBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_SEC_Error_Message" colspan="3">
											<c:if test="${!empty sectionBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=sectionBean.getErrorMessage() && !"".equals(sectionBean.getErrorMessage()))
														{
															String[] msgs = sectionBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty sectionBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sectionBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty sectionBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sectionBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="SEC_CountryLocale_Code" name="SEC_CountryLocale_Code" onchange="SEC_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sectionBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${sectionBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty sectionBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq sectionBean.countryLocaleId}">
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
												<select id="SEC_Lang_Code" name="SEC_Lang_Code" onchange="SEC_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sectionBean.languageList }">
														<c:forEach var="languageList" items="${sectionBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq sectionBean.manualLanguageId}">
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
									<c:if test="${sectionBean.showWriteControls eq true }">
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
													<td><fmt:message key="label.division"/><span class="mandatory">*</span></td>
													<td>
														<select id="SEC_Div_Code" name="SEC_Div_Code" style="width: 200px !important">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty sectionBean.divisionList }">
																<c:forEach var="divisionList" items="${sectionBean.divisionList }">
																	<c:set var="selectedFlagDiv" value="" />
																	<c:if test="${!empty divisionList.divisionId}">
																		<c:if test="${divisionList.divisionId eq sectionBean.divisionId}">
																			<c:set var="selectedFlagDiv" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagDiv eq 1}">
																			<option selected="selected" value="<c:out value="${divisionList.divisionId }"/>"><c:out value="${divisionList.divisionName }"/> (<c:out value="${divisionList.divisionCode }"/>)</option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${divisionList.divisionId }"/>"><c:out value="${divisionList.divisionName }"/> (<c:out value="${divisionList.divisionCode }"/>)</option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td><fmt:message key="label.sectioncode"/><span class="mandatory">*</span></td>
													<td><input type="text" id="SEC_Code" style="width: 50px;" name="SEC_Code" 
														value="<c:out value="${sectionBean.fieldDetails.sectionCode }" />" size="10" maxlength="4" 
														onchange="numbersOnly(this);" onkeyup="numbersOnly(this);" /></td>
													<td><fmt:message key="label.sectionname"/> <span class="mandatory">*</span></td>
													<td><input type="text" id="SEC_Name" style="width: 200px;" name="SEC_Name" value="<c:out value="${sectionBean.fieldDetails.sectionName }" />" /></td>
													<td>&nbsp;</td>
												</tr>
												<tr>
													<td><fmt:message key="label.sectionindex"/><span class="mandatory">*</span></td>
													<td style="padding-left: 40px;float: left;"><input type="text" style="width:50px;" id="SEC_Index" name="SEC_Index" value="<c:out value="${sectionBean.fieldDetails.sectionIndex }" />" 
														size="10" maxlength="4" onchange="numbersOnly(this);" onkeyup="numbersOnly(this);"/></td>
													<td><fmt:message key="label.sortid"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width:50px;" id="SEC_SortId" name="SEC_SortId" 
														onchange="numbersOnly(this);" onkeyup="numbersOnly(this);" size="10" maxlength="4" 
														value="<c:out value="${sectionBean.fieldDetails.sortId }" />" /></td>
													<td colspan="2">&nbsp;</td>	
													<td style="text-align: right; !important;">
														<input type="button" name="SEC_Entry" id="SEC_Entry" onclick="SEC_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
													
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${sectionBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.section"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.section"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="SEC_File" name="SEC_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SEC_file_upload();" id="SEC_Upload" name="SEC_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="SEC_DownloadTemplate" onclick="SEC_download_template();"
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
								<c:if test="${!empty sectionBean.sectionList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty sectionBean.sectionList }">
											<c:forEach var="sectionList" items="${sectionBean.sectionList }">
												<c:if test="${sectionList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty sectionBean.sectionList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${sectionBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="5%">#.</th>
											<th scope="col" width="15%"><fmt:message key="label.division"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.sectioncode"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="13%"><fmt:message key="label.sectionindex"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="15%"><fmt:message key="label.sectionname"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.sortid"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="12%"><fmt:message key="label.flag"/> <c:if test="${sectionBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="7%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="8%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty sectionBean.sectionList }">
											<c:forEach var="sectionList" items="${sectionBean.sectionList }">
												<tr <c:if test="${sectionList.editableFlag eq true }"> class="selected" </c:if>>
													<td> 
														<input type="checkbox" name="SEC_Selection" onchange="SEC_checkBoxManagement();" 
															value="<c:out value="${sectionList.sectionId }" />" 
															id="SEC_Selection_<c:out value="${sectionList.sectionId }"  />"
															<c:if test="${sectionList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${sectionBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${sectionList.srNo }" /> </td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.divisionName }" /> (<c:out value="${sectionList.divisionCode }"/>)</label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<select name="SEC_List_DivId<c:out value="${sectionList.sectionId }" />" id="SEC_List_DivId<c:out value="${sectionList.sectionId }" />" style="width: 110px;">
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:forEach var="divisionList" items="${sectionBean.divisionList }">
																	<c:set var="selectedDiv" value="" />
																	<c:if test="${!empty divisionList.divisionId}">
																		<c:if test="${divisionList.divisionId eq sectionList.divisionId}">
																			<c:set var="selectedDiv" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedDiv eq 1}">
																			<option value="<c:out value="${divisionList.divisionId }" />" selected="selected"><c:out value="${divisionList.divisionName }" /> (<c:out value="${divisionList.divisionCode }"/>)</option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${divisionList.divisionId }" />"><c:out value="${divisionList.divisionName }" /> (<c:out value="${divisionList.divisionCode }"/>)</option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</select>
														</c:if>
													</td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.sectionCode }" /></label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<input style="width: 30px !important;" type="text" value="<c:out value="${sectionList.sectionCode }" />" size="10" 
																maxlength="4" onchange="numbersOnly(this);" onkeyup="numbersOnly(this);" 
																name="SEC_List_Code<c:out value="${sectionList.sectionId }" />" 
																id="SEC_List_Code<c:out value="${sectionList.sectionId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.sectionIndex }" /></label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<input style="width: 30px !important;" type="text" value="<c:out value="${sectionList.sectionIndex }" />" size="10" 
																name="SEC_List_Index<c:out value="${sectionList.sectionId }" />"  
																id="SEC_List_Index<c:out value="${sectionList.sectionId }" />" maxlength="4" 
																onchange="numbersOnly(this);" onkeyup="numbersOnly(this);"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.sectionName }" /></label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${sectionList.sectionName }" />"  
																name="SEC_List_Name<c:out value="${sectionList.sectionId }" />" 
																id="SEC_List_Name<c:out value="${sectionList.sectionId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.sortId }" /></label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<input style="width: 30px !important;" type="text" value="<c:out value="${sectionList.sortId }" />" size="10" 
																name="SEC_List_SortId<c:out value="${sectionList.sectionId }" />" 
																id="SEC_List_SortId<c:out value="${sectionList.sectionId }" />" maxlength="4" 
																onchange="numbersOnly(this);" onkeyup="numbersOnly(this);"/>
														</c:if>
													</td>
													<td>
														<c:if test="${sectionList.editableFlag eq false }">
															<label><c:out value="${sectionList.flagLabel }" /></label>
														</c:if>
														<c:if test="${sectionList.editableFlag eq true }">
															<select name="SEC_List_Flag_<c:out value="${sectionList.sectionId }" />" id="SEC_List_Flag_<c:out value="${sectionList.sectionId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${sectionBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty sectionList.flag}">
																		<c:if test="${sectionList.flag eq flagList.value}">
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
														<fmt:formatDate value="${sectionList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${sectionList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty sectionBean.sectionList }">
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
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<c:if test="${!empty sectionBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${sectionBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("SEC_File"))
{
document.getElementById("SEC_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty sectionBean.sectionList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty sectionBean.sectionList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${sectionBean.showUpdate eq false }">
<c:if test="${sectionBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SEC_edit();\" name=\"SEC_Edit\" id=\"SEC_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SEC_delete();\" name=\"SEC_Delete\" id=\"SEC_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"SEC_active();\" name=\"SEC_Active\" id=\"SEC_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
	
</script>
</c:if>
<c:if test="${sectionBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"SEC_export();\"  name=\"SEC_Export\" id=\"SEC_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>

</c:if>
<c:if test="${sectionBean.showUpdate eq true && sectionBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"SEC_update();\" style=\"float:right\" name=\"SEC_Update\" id=\"SEC_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"SEC_Reset\" id=\"SEC_Reset\" onclick=\"SEC_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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

function numbersOnly(thisObj)
{
	thisObj.value=thisObj.value.replace(/[^0-9]/g,'');
}



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
	var pageNo=$("#SEC_DataTabel_displayPageNo").val();
	var pageLength=$("#SEC_DataTabel_displayPageLen").val();
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
		SEC_checkBoxManagement();
	});
}

function SEC_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#SEC_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#SEC_DataTabel_displayPageLen").val(length);
	}	
	
}

function SEC_edit()
{
	SEC_readDataTableValues();
	var value=$("#SEC_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show loader
		$("#loader").show();
		$("#SEC_ActionClicked").val("EDIT_SECTION");
		$("#SEC_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}  
}

function SEC_delete_action()
{
	// show loader
	$("#loader").show();
	SEC_readDataTableValues();
	$("#SEC_ActionClicked").val("DELETE_SECTION");
	$("#SEC_Form").submit();
}

function SEC_active_action()
{
	// show loader
	$("#loader").show();
	SEC_readDataTableValues();
	$("#SEC_ActionClicked").val("ACTIVE_SECTION");
	$("#SEC_Form").submit();
}

function SEC_delete()
{
	$("#MDM_SEC_Error_Message").html("");
	var value=$("#SEC_SelectedRows").val();
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
					  SEC_delete_action();
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


function SEC_active()
{
	$("#MDM_SEC_Error_Message").html("");
	var value=$("#SEC_SelectedRows").val();
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
					  SEC_active_action();
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


function SEC_export()
{
	$("#MDM_SEC_Error_Message").html("");
	var value=$("#SEC_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show loader
		$("#loader").show();
		SEC_readDataTableValues();
		$("#SEC_ActionClicked").val("EXPORT_SECTION");
		  $("#SEC_Form").submit();
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
	$("#MDM_SEC_Error_Message").html(html);
	window.scrollTo(0,0);
}


function SEC_file_upload()
{
	// show loader
	$("#loader").show();
 	$("#SEC_ActionClicked").val("FILE_UPLOAD");
	$("#SEC_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function SEC_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=SECTION_MASTER";
}


function SEC_entry()
{
	// show loader
	$("#loader").show();
	SEC_readDataTableValues();
	$("#SEC_ActionClicked").val("SAVE_SECTION");
	  $("#SEC_Form").submit();
}



function SEC_update()
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
   	$("#SEC_UpdatedRows").val(newData);
   	SEC_readDataTableValues();
	$("#SEC_ActionClicked").val("UPDATE_SECTION");
	  $("#SEC_Form").submit();
}

function SEC_checkBoxManagement()
{
	$("#SEC_SelectedRows").val("");
	var value=$("#SEC_SelectedRows").val();
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
			$.each(selIds, function(i, e) {
				if ($.inArray(e, result) == -1) result.push(e);
			});
		}
		$("#SEC_SelectedRows").val(result);
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

function SEC_reset()
{
	// show loader
	$("#loader").show();
	$("#SEC_ActionClicked").val("RESET_SECTION");
	  $("#SEC_Form").submit();
}

function SEC_reset_withDialog()
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
				 $("#SEC_ActionClicked").val("RESET_SECTION");
		          $( this ).dialog( "close" );
		       // show loader
		  		$("#loader").show();
				  $("#SEC_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function SEC_localeSelection()
{
	// show loader
	$("#loader").show();
	$("#SEC_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#SEC_Form").submit();
}

function SEC_langSelection()
{
	// show loader
	$("#loader").show();
	$("#SEC_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#SEC_Form").submit();
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
		$("#SEC_SelectedRows").val("");
	}
	SEC_checkBoxManagement();
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
						SEC_readDataTableValues();
						 $("#SEC_ActionClicked").val("RESET_SECTION");
						 $("#SEC_Form").submit();
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