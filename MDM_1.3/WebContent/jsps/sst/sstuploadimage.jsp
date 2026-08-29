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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_SST_UPLOAD_IMAGES);
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
<jsp:useBean id="sstUploadImageBean" class="com.mazda.gms3.sst.bean.SSTUploadImageBean" scope="session"></jsp:useBean>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.SST_UPLOAD_IMAGES"/> </title>
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
            	<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.sstmaintenance" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.SST_UPLOAD_IMAGES"/> &rsaquo;</a> 
            </div>			
            <form id="SSTUIMG_Form" name="SSTUIMG_Form" action="<%=request.getContextPath() %>/sstuploadimages" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
			<input type="hidden" name="SSTUIMG_ActionClicked" id="SSTUIMG_ActionClicked" value="<c:out value="${sstUploadImageBean.actionClicked }"/>" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_SSTUIMG_Error_Message" colspan="3">
											<c:if test="${!empty sstUploadImageBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=sstUploadImageBean.getErrorMessage() && !"".equals(sstUploadImageBean.getErrorMessage()))
														{
															String[] msgs = sstUploadImageBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty sstUploadImageBean.successMessage}">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sstUploadImageBean.successMessage }" />
												</div>
											</c:if>
											<c:if test="${!empty sstUploadImageBean.infoMessage}">
												<div class="errorWarningMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${sstUploadImageBean.infoMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td width="33%">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.countrylocale"/>: <span class="mandatory">*</span></label> 
												<select id="SSTUIMG_CountryLocale_Code" name="SSTUIMG_CountryLocale_Code" onchange="SSTUIMG_localeSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sstUploadImageBean.countryLocaleList }">
														<c:forEach var="countryLocaleList" items="${sstUploadImageBean.countryLocaleList }">
															<c:set var="selectedFlagLocale" value="" />
															<c:if test="${!empty sstUploadImageBean.countryLocaleId}">
																<c:if test="${countryLocaleList.countryLocaleId eq sstUploadImageBean.countryLocaleId}">
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
												<select id="SSTUIMG_Lang_Code" name="SSTUIMG_Lang_Code">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sstUploadImageBean.languageList }">
														<c:forEach var="languageList" items="${sstUploadImageBean.languageList }">
															<c:set var="selectedFlagLang" value="" />
															<c:if test="${!empty languageList.manualLanguageId}">
																<c:if test="${languageList.manualLanguageId eq sstUploadImageBean.manualLanguageId}">
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
								</table>
								
								<c:if test="${sstUploadImageBean.showWriteControls eq true }">
								<br />
								<h4>
									<fmt:message key="label.upload"/> <fmt:message key="label.sstimage"/> / <fmt:message key="label.useimage"/> <fmt:message key="label.details"/>
								</h4>
								<br />
								<table width="100%" border="0" cellspacing="0" cellpadding="0" class="inputTable">
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.imagetype" /> <span class="mandatory">*</span></label> 
												<select id="SSTUIMG_ImageType" name="SSTUIMG_ImageType" style="width:200px;height:30px;margin:0px;">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty sstUploadImageBean.imageTypeList }">
														<c:forEach var="imageTypeList" items="${sstUploadImageBean.imageTypeList }">
															<c:set var="selectedImageTypeFlag" value="" />
															<c:if test="${!empty imageTypeList.value}">
																<c:if test="${imageTypeList.value eq sstUploadImageBean.imageType}">
																	<c:set var="selectedImageTypeFlag" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedImageTypeFlag eq 1}">
																	<option selected="selected" value="<c:out value="${imageTypeList.value }"/>"><c:out value="${imageTypeList.label }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${imageTypeList.value }"/>"><c:out value="${imageTypeList.label }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
											</div>
										</td>
									</tr>
									<tr>
										<td>
											<div class="formElement_row leftpadding_none" style="margin: 0px !important;">
												<label style="width: 150px;"><fmt:message key="label.upload" /> <fmt:message key="label.sstimage"/> / <fmt:message key="label.useimage"/> <span class="mandatory">*</span></label> 
												<input id="uploadFile" class="fileInput" placeholder="<fmt:message key="label.choose.file"/>" disabled="disabled" />
												<div class="fileUpload btn">
													<span><fmt:message key="label.browse"/></span>
													<input id="SSTUIMG_File" name="SSTUIMG_File" type="file" class="upload" multiple="multiple" />
												</div>
												<button style="margin-left:25px;padding:6px 10px;float: left;" type="button" onclick="SSTUIMG_file_upload();" id="SSTUIMG_Upload" name="SSTUIMG_Upload" 
												class="bluebutton cursorPointer"><i class="uploadIcon"></i><fmt:message key="label.upload"/></button>
												<div class="cb"></div>
												<div class="warningMessage" style="padding-left: 60px;padding-top: 10px;">
													<i class="infoIcon" style="color: #D0021B !important;"></i> <fmt:message key="label.sstuploadimage.disclaimer.text" /> 
												</div>
											</div>
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

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="../footer.jsp" flush="true" />
<!-- FOOTER ENDS -->


<script>
if(null!=document.getElementById("SSTUIMG_File"))
{
document.getElementById("SSTUIMG_File").onchange = function () {
    document.getElementById("uploadFile").value = this.value;
};
}
</script>

<script type="text/javascript">
$(window).load(function() {
	$("#loader").fadeOut("slow");
});


function SSTUIMG_file_upload()
{
	// show loader
	$("#loader").show();
 	$("#SSTUIMG_ActionClicked").val("FILE_UPLOAD");
	$("#SSTUIMG_Form").submit();
}

function SSTUIMG_localeSelection()
{
	// show loader
	$("#loader").show();
	$("#SSTUIMG_ActionClicked").val("COUNTRY_LOCALE_SELECTION");
	$("#SSTUIMG_Form").submit();
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
						SSTUIMG_readDataTableValues();
						 $("#SSTUIMG_ActionClicked").val("RESET_SSTUIMGIMAGE");
						 $("#SSTUIMG_Form").submit();
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