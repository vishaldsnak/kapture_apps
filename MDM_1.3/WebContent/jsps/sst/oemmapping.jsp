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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SST_OEM_MAPPING);
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
<jsp:useBean id="oemMappingBean" class="com.mazda.gms3.sst.bean.OEMMappingBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.oemmapping.header"/> </title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="css/chosen.css" type="text/css" rel="stylesheet" />
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.sstmaintenance" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.oemmapping.header"/> &rsaquo;</a> 
            </div>			
            <form id="OEMMAP_Form" name="OEMMAP_Form" action="<%=request.getContextPath() %>/oemmapping" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="OEMMAP_SelectedRows" id="OEMMAP_SelectedRows" value="<c:out value="${oemMappingBean.selectedRows }"/>" />
			<input type="hidden" name="OEMMAP_ActionClicked" id="OEMMAP_ActionClicked" value="<c:out value="${oemMappingBean.actionClicked }"/>" />
			<input type="hidden" name="OEMMAP_UpdatedRows" id="OEMMAP_UpdatedRows" value="" />
			<input type="hidden" name="OEMMAP_DataTabel_displayPageNo" id="OEMMAP_DataTabel_displayPageNo" value="<c:out value="${oemMappingBean.displayPageNo }"/>" />
			<input type="hidden" name="OEMMAP_DataTabel_displayPageLen" id="OEMMAP_DataTabel_displayPageLen" value="<c:out value="${oemMappingBean.displayPageLength }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_OEMMAP_Error_Message" colspan="3">
											<c:if test="${!empty oemMappingBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=oemMappingBean.getErrorMessage() && !"".equals(oemMappingBean.getErrorMessage()))
														{
															String[] msgs = oemMappingBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty oemMappingBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${oemMappingBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty oemMappingBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${oemMappingBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="OEMMAP_CountryLocale_Code" name="OEMMAP_CountryLocale_Code" onchange="OEMMAP_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty oemMappingBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${oemMappingBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty oemMappingBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq oemMappingBean.countryLocaleId}">
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
												<select id="OEMMAP_Lang_Code" name="OEMMAP_Lang_Code" onchange="OEMMAP_langSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty oemMappingBean.languageList }">
														<c:forEach var="languageList" items="${oemMappingBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq oemMappingBean.manualLanguageId}">
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
									<c:if test="${oemMappingBean.showWriteControls eq true }">
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
													<td>
														<select id="OEMMAP_SSTId" name="OEMMAP_SSTId" style="width: 210px;" class="chosen-select">
															<option value=""><fmt:message key="label.selectOne"/></option>
															<c:if test="${!empty oemMappingBean.sstList }">
																<c:forEach var="sstList" items="${oemMappingBean.sstList }">
																	<c:set var="selectedFlagSST" value="" />
																	<c:if test="${!empty sstList.sstId}">
																		<c:if test="${sstList.sstId eq oemMappingBean.fieldDetails.sstId}">
																			<c:set var="selectedFlagSST" value="1" />
																		</c:if>
																	</c:if>
																	<c:choose>
																		<c:when test="${selectedFlagSST eq 1}">
																			<option selected="selected" value="<c:out value="${sstList.sstId }"/>"><c:out value="${sstList.sstNumber }"/></option>
																		</c:when>
																		<c:otherwise>
																			<option value="<c:out value="${sstList.sstId }"/>"><c:out value="${sstList.sstNumber }"/></option>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</c:if>
														</select>
													</td>
													<td><fmt:message key="label.fordnumber"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="OEMMAP_FordNo" name="OEMMAP_FordNo" value="<c:out value="${oemMappingBean.fieldDetails.fordNumber }" />" /></td>
													<td><fmt:message key="label.nissannumber"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="OEMMAP_NissanNo" name="OEMMAP_NissanNo" value="<c:out value="${oemMappingBean.fieldDetails.nissanNumber }" />" /></td>
												</tr>
												<tr>
													<td><fmt:message key="label.isuzunumber"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="OEMMAP_IsuzuNo" name="OEMMAP_IsuzuNo" value="<c:out value="${oemMappingBean.fieldDetails.isuzuNumber }" />" /></td>
													<td><fmt:message key="label.suzukinumber"/><span class="mandatory">*</span></td>
													<td><input type="text" style="width: 200px;" id="OEMMAP_SuzukiNo" name="OEMMAP_SuzukiNo" value="<c:out value="${oemMappingBean.fieldDetails.suzukiNumber }" />" /></td>
													<td style="text-align: right; !important;" colspan="2">
														<input type="button" name="OEMMAP_Entry" id="OEMMAP_Entry" onclick="OEMMAP_entry();" class="bluebutton cursorPointer"  value="<fmt:message key="label.entry"/>"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									</c:if>
								</table>
								<c:if test="${oemMappingBean.showWriteControls eq true }">
								<p style="height:45px;">&nbsp;</p>
								<h4>
									<fmt:message key="label.import"/> <fmt:message key="label.oemmapping"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.oemmapping"/> <fmt:message key="label.excel"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="OEMMAP_File" name="OEMMAP_File" type="file" class="upload" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="OEMMAP_file_upload();" id="OEMMAP_Upload" name="OEMMAP_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.import"/></button>
									<!-- DOWNLOAD THE BLANK IMPORT TEMPLATE FOR THIS SCREEN -->
									<a style="margin-left:25px;float: left;line-height:30px;" href="javascript:void(0);"
										id="OEMMAP_DownloadTemplate" onclick="OEMMAP_download_template();"
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
								<c:if test="${!empty oemMappingBean.oemMappingList }">
								<table id="example" class="display historyTable" cellspacing="0"
									width="100%">
									<thead>
										<c:set var="showChkBoxSelected" value="1" />
										<c:if test="${!empty oemMappingBean.oemMappingList }">
											<c:forEach var="oemMappingList" items="${oemMappingBean.oemMappingList }">
												<c:if test="${oemMappingList.editableFlag eq false }">
													<c:set var="showChkBoxSelected" value="0" />
												</c:if>
											</c:forEach>
										</c:if>
										<c:if test="${empty oemMappingBean.oemMappingList }">
											<c:set var="showChkBoxSelected" value="0" />
										</c:if>
										<tr>
											<th scope="col" width="3%">
												<input type="checkbox" id="MDM_HeaderCheckBox" name="MDM_HeaderCheckBox" onclick="mdm_selectallrows(this);"
												<c:if test="${oemMappingBean.showUpdate eq true }"> disabled="disabled" </c:if>
												<c:choose><c:when test="${showChkBoxSelected eq 1}"> checked="checked" </c:when></c:choose> />
											</th>
											<th scope="col" width="7%">#.</th>
											<th scope="col" width="20%"><fmt:message key="label.sstnumber"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.fordnumber"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.nissannumber"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.isuzunumber"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.suzukinumber"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.flag"/> <c:if test="${oemMappingBean.showUpdate eq true }"><span style="color: red;">*</span></c:if></th>
											<th scope="col" width="10%"><fmt:message key="label.entrytime"/></th>
											<th scope="col" width="10%"><fmt:message key="label.updatedtime"/></th>
										</tr>
									</thead>
									<tbody>
										<c:if test="${!empty oemMappingBean.oemMappingList }">
											<c:forEach var="oemMappingList" items="${oemMappingBean.oemMappingList }">
												<tr <c:if test="${oemMappingList.editableFlag eq true }"> class="selected" </c:if>>
													<td> 
														<input type="checkbox" name="OEMMAP_Selection" onchange="OEMMAP_checkBoxManagement();" 
															value="<c:out value="${oemMappingList.oemMappingId }" />" 
															id="OEMMAP_Selection_<c:out value="${oemMappingList.oemMappingId }"  />"
															<c:if test="${oemMappingList.editableFlag eq true  }">checked="checked"</c:if> 
															<c:if test="${oemMappingBean.showUpdate eq true }"> disabled="disabled" </c:if>/>
													</td>
													<td><c:out value="${oemMappingList.srNo }" /> </td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.sstNumber }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<select style="width: 200px !important; " name="OEMMAP_List_SSTId<c:out value="${oemMappingList.oemMappingId }" />" id="OEMMAP_List_SSTId<c:out value="${oemMappingList.oemMappingId }" />" class="chosen-select" >
																<option value=""><fmt:message key="label.selectOne"/></option>
																<c:if test="${!empty oemMappingBean.sstList }">
																	<c:forEach var="sstList" items="${oemMappingBean.sstList }">
																		<c:set var="selectedFlagSST" value="" />
																		<c:if test="${!empty sstList.sstId}">
																			<c:if test="${sstList.sstId eq oemMappingList.sstId}">
																				<c:set var="selectedFlagSST" value="1" />
																			</c:if>
																		</c:if>
																		<c:choose>
																			<c:when test="${selectedFlagSST eq 1}">
																				<option selected="selected" value="<c:out value="${sstList.sstId }"/>"><c:out value="${sstList.sstNumber }"/></option>
																			</c:when>
																			<c:otherwise>
																				<option value="<c:out value="${sstList.sstId }"/>"><c:out value="${sstList.sstNumber }"/></option>
																			</c:otherwise>
																		</c:choose>
																	</c:forEach>
																</c:if>
															</select>
														</c:if>
													</td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.fordNumber }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${oemMappingList.fordNumber }" />" size="10" 
																name="OEMMAP_List_FordNo<c:out value="${oemMappingList.oemMappingId }" />" 
																id="OEMMAP_List_FordNo<c:out value="${oemMappingList.oemMappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.nissanNumber }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${oemMappingList.nissanNumber }" />" size="10" 
																name="OEMMAP_List_NissanNo<c:out value="${oemMappingList.oemMappingId }" />" 
																id="OEMMAP_List_NissanNo<c:out value="${oemMappingList.oemMappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.isuzuNumber }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${oemMappingList.isuzuNumber }" />" size="10" 
																name="OEMMAP_List_IsuzuNo<c:out value="${oemMappingList.oemMappingId }" />" 
																id="OEMMAP_List_IsuzuNo<c:out value="${oemMappingList.oemMappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.suzukiNumber }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<input style="width: auto !important;" type="text" value="<c:out value="${oemMappingList.suzukiNumber }" />" size="10" 
																name="OEMMAP_List_SuzukiNo<c:out value="${oemMappingList.oemMappingId }" />" 
																id="OEMMAP_List_SuzukiNo<c:out value="${oemMappingList.oemMappingId }" />"/>
														</c:if>
													</td>
													<td>
														<c:if test="${oemMappingList.editableFlag eq false }">
															<label><c:out value="${oemMappingList.flagLabel }" /></label>
														</c:if>
														<c:if test="${oemMappingList.editableFlag eq true }">
															<select name="OEMMAP_List_Flag_<c:out value="${oemMappingList.oemMappingId }" />" id="OEMMAP_List_Flag_<c:out value="${oemMappingList.oemMappingId }" />" style="width: 90px;">
																<c:forEach var="flagList" items="${oemMappingBean.flagList }">
																	<c:set var="selectedFlag" value="" />
																	<c:if test="${!empty oemMappingList.flag}">
																		<c:if test="${oemMappingList.flag eq flagList.value}">
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
														<fmt:formatDate value="${oemMappingList.entryTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
													<td>
														<fmt:formatDate value="${oemMappingList.updatedTime}"  pattern="yyyy/MM/dd HH:mm:ss"/>
													</td>
												</tr>
											</c:forEach>
										</c:if>
									</tbody>
								</table>	
								</c:if>
								<c:if test="${empty oemMappingBean.oemMappingList }">
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


<!-- JS Files for AUTO COMPLETE COMBO -->
<script src="js/chosen.jquery.js" type="text/javascript"></script>
<script src="js/init.js" type="text/javascript" charset="utf-8"></script>

<c:if test="${!empty oemMappingBean.reportViewPath  }">
<script>
	var reportURL = "<c:out value="${oemMappingBean.reportViewPath  }"/>";
	window.open(reportURL, "_BLANK");
</script>	
</c:if>


<script>
if(null!=document.getElementById("OEMMAP_File"))
{
document.getElementById("OEMMAP_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<c:if test="${!empty oemMappingBean.oemMappingList }">
<script type="text/javascript">
var htmlContent="<div class=\"separator\"></div>";
</script>
</c:if>
<c:if test="${empty oemMappingBean.oemMappingList }">
<script type="text/javascript">
var htmlContent="";
</script>
</c:if>
<c:if test="${oemMappingBean.showUpdate eq false }">
<c:if test="${oemMappingBean.showWriteControls eq true }">
<script type="text/javascript">
	var edit = "<fmt:message key="label.edit"/>";
	var deleteLabel = "<fmt:message key="label.delete"/>";
	var activeLabel="<fmt:message key="flag.label.active" />";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"OEMMAP_edit();\" name=\"OEMMAP_Edit\" id=\"OEMMAP_Edit\" class=\"bluebutton cursorPointer\" value=\""+edit+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"OEMMAP_delete();\" name=\"OEMMAP_Delete\" id=\"OEMMAP_Delete\" class=\"bluebutton cursorPointer\" value=\""+deleteLabel+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" onclick=\"OEMMAP_active();\" name=\"OEMMAP_Active\" id=\"OEMMAP_Active\" class=\"bluebutton cursorPointer\" value=\""+activeLabel+"\"/>";
	edit=null;
</script>
</c:if>

<c:if test="${oemMappingBean.showReadControls eq true }">
<script type="text/javascript">
	var exportLabel = "<fmt:message key="label.export"/>";
	htmlContent=htmlContent+"<button type=\"button\" style=\"float:right\" onclick=\"OEMMAP_export();\"  name=\"OEMMAP_Export\" id=\"OEMMAP_Export\" class=\"bluebutton cursorPointer\"><i class=\"downloadIcon\"></i>"+exportLabel+"</button>";
</script>
</c:if>
</c:if>
<c:if test="${oemMappingBean.showUpdate eq true && oemMappingBean.showWriteControls eq true}">
<script type="text/javascript">
	var update="<fmt:message key="label.save"/>";
	var cancel="<fmt:message key="label.cancel"/>";
	htmlContent=htmlContent+"<input type=\"button\" onclick=\"OEMMAP_update();\" style=\"float:right\" name=\"OEMMAP_Update\" id=\"OEMMAP_Update\"  class=\"bluebutton cursorPointer\" value=\""+update+"\"/>";
	htmlContent=htmlContent+"<input type=\"button\" style=\"float:right\" name=\"OEMMAP_Reset\" id=\"OEMMAP_Reset\" onclick=\"OEMMAP_reset();\" class=\"gear_button cursorPointer\" value=\""+cancel+"\"/>";
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
	var pageNo=$("#OEMMAP_DataTabel_displayPageNo").val();
	var pageLength=$("#OEMMAP_DataTabel_displayPageLen").val();
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
		OEMMAP_checkBoxManagement();
	});
}

function OEMMAP_readDataTableValues()
{
	var table = $('#example').DataTable();
	var info = table.page.info();
	var length = table.page.len();
	// update data table display page & length
	if(null!=info)
	{
		$("#OEMMAP_DataTabel_displayPageNo").val(info.page);
	}	
	if(null!=length)
	{
		$("#OEMMAP_DataTabel_displayPageLen").val(length);
	}	
	
}

function OEMMAP_edit()
{
	OEMMAP_readDataTableValues();
	var value=$("#OEMMAP_SelectedRows").val();
	if(null!=value && value!="")
	{	
		// show loader
		$("#loader").show();
		$("#OEMMAP_ActionClicked").val("EDIT_OEMMAPPING");
		$("#OEMMAP_Form").submit();
	}
	else
	{
		var message="<fmt:message key="error.select.onerow.edit" />";
		mdm_show_errorMessage(message);
	}  
}

function OEMMAP_delete_action()
{
	// show loader
	$("#loader").show();
	OEMMAP_readDataTableValues();
	$("#OEMMAP_ActionClicked").val("DELETE_OEMMAPPING");
	$("#OEMMAP_Form").submit();
}

function OEMMAP_active_action()
{
	// show loader
	$("#loader").show();
	OEMMAP_readDataTableValues();
	$("#OEMMAP_ActionClicked").val("ACTIVE_OEMMAPPING");
	$("#OEMMAP_Form").submit();
}

function OEMMAP_delete()
{
	$("#MDM_OEMMAP_Error_Message").html("");
	var value=$("#OEMMAP_SelectedRows").val();
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
					  OEMMAP_delete_action();
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


function OEMMAP_active()
{
	$("#MDM_OEMMAP_Error_Message").html("");
	var value=$("#OEMMAP_SelectedRows").val();
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
					  OEMMAP_active_action();
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


function OEMMAP_export()
{
	$("#MDM_OEMMAP_Error_Message").html("");
	var value=$("#OEMMAP_SelectedRows").val();
	if(null!=value && value!="")
	{
		// show loader
		$("#loader").show();
		OEMMAP_readDataTableValues();
		$("#OEMMAP_ActionClicked").val("EXPORT_OEMMAPPING");
		  $("#OEMMAP_Form").submit();
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
	$("#MDM_OEMMAP_Error_Message").html(html);
	window.scrollTo(0,0);
}


function OEMMAP_file_upload()
{
	// show loader
	$("#loader").show();
 	$("#OEMMAP_ActionClicked").val("FILE_UPLOAD");
	$("#OEMMAP_Form").submit();
}

/*
 * Download the blank import template for this screen. Plain GET - the page is NOT
 * submitted, so nothing already keyed in is lost, and no loader is shown (the
 * browser handles the download itself).
 */
function OEMMAP_download_template()
{
	window.location.href = "<c:out value="${pageContext.request.contextPath}"/>/importtemplate?screen=SST_OEM_MAPPING";
}


function OEMMAP_entry()
{
	// show loader
	$("#loader").show();
	OEMMAP_readDataTableValues();
	$("#OEMMAP_ActionClicked").val("SAVE_OEMMAPPING");
	  $("#OEMMAP_Form").submit();
}


function OEMMAP_update()
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
   	$("#OEMMAP_UpdatedRows").val(newData);
   	OEMMAP_readDataTableValues();
	$("#OEMMAP_ActionClicked").val("UPDATE_OEMMAPPING");
	  $("#OEMMAP_Form").submit();
}

function OEMMAP_checkBoxManagement()
{
	$("#OEMMAP_SelectedRows").val("");
	var value=$("#OEMMAP_SelectedRows").val();
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
		$("#OEMMAP_SelectedRows").val(result);
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

function OEMMAP_reset()
{
	// show loader
	$("#loader").show();
	$("#OEMMAP_ActionClicked").val("RESET_OEMMAPPING");
	  $("#OEMMAP_Form").submit();
}

function OEMMAP_reset_withDialog()
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
				 $("#OEMMAP_ActionClicked").val("RESET_OEMMAPPING");
		          $( this ).dialog( "close" );
		       // show loader
		  		$("#loader").show();
				  $("#OEMMAP_Form").submit();
				}  
		  	,
	        "<fmt:message key="label.cancel"/>": function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
		
}

function OEMMAP_localeSelection()
{
	// show loader
	$("#loader").show();
	$("#OEMMAP_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	 $("#OEMMAP_Form").submit();
}

function OEMMAP_langSelection()
{
	// show loader
	$("#loader").show();
	$("#OEMMAP_ActionClicked").val("LANGUAGE_SELECTION");
	 $("#OEMMAP_Form").submit();
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
		$("#OEMMAP_SelectedRows").val("");
	}
	OEMMAP_checkBoxManagement();
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
						OEMMAP_readDataTableValues();
						 $("#OEMMAP_ActionClicked").val("RESET_OEMMAPPING");
						 $("#OEMMAP_Form").submit();
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