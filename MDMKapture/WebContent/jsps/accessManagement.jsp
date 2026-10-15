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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_ACCESS_MANAGEMENT);
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
<jsp:useBean id="accessManagementBean" class="com.mazda.gms3.mdm.bean.AccessManagementBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.accessmanagement"/> </title>
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.accessmanagement"/> &rsaquo;</a>
           	</div>
            <form id="ACM_Form" name="ACM_Form" action="<%=request.getContextPath() %>/accessmanagement" method="post">
			<input type="hidden" name="ACM_ActionClicked" id="ACM_ActionClicked" value="" />
			<input type="hidden" name="ACM_SelectedModules" id="ACM_SelectedModules" value="" />
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<table width="100%" cellspacing="1" cellpadding="3" >
									<tr>
										<td id="MDM_ACM_Error_Message" colspan="3">
											<c:if test="${!empty accessManagementBean.errorMessage }">
												<div class="errorMessage" id="MS3_ERROR_MESSAGE">
													<%
														if(null!=accessManagementBean.getErrorMessage() && !"".equals(accessManagementBean.getErrorMessage()))
														{
															String[] msgs = accessManagementBean.getErrorMessage().split("<MSG_TOKEN>");
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
											<c:if test="${!empty accessManagementBean.successMessage }">
												<div class="successMessage" id="MS3_ERROR_MESSAGE">
													<c:out value="${accessManagementBean.successMessage }" />
												</div>
											</c:if>
											<div class="cb"></div>
										</td>
									</tr>
									<tr>
										<td colspan="3">
											<div class="formElement_row leftpadding_none">
												<label><fmt:message key="label.role"/>: <span class="mandatory">*</span></label> 
												<select id="ACM_RoleId" name="ACM_RoleId" onchange="acm_roleSelection();">
													<option value=""><fmt:message key="label.selectOne"/></option>
													<c:if test="${!empty accessManagementBean.rolesList }">
														<c:forEach var="rolesList" items="${accessManagementBean.rolesList }">
															<c:set var="selectedRoleFlag" value="" />
															<c:if test="${!empty accessManagementBean.roleId}">
																<c:if test="${rolesList.roleId eq accessManagementBean.roleId}">
																	<c:set var="selectedRoleFlag" value="1" />
																</c:if>
															</c:if>
															<c:choose>
																<c:when test="${selectedRoleFlag eq 1}">
																	<option selected="selected" value="<c:out value="${rolesList.roleId }"/>"><c:out value="${rolesList.roleName }"/></option>
																</c:when>
																<c:otherwise>
																	<option value="<c:out value="${rolesList.roleId }"/>"><c:out value="${rolesList.roleName }"/></option>
																</c:otherwise>
															</c:choose>
														</c:forEach>
													</c:if>
												</select>
												<div class="cb"></div>
											</div>
										</td>
									</tr>
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>	
									<tr>
										<td colspan="3">
											<h3>
												<fmt:message key="label.modules"/>
											</h3>
											<br />
										</td>
									</tr>
									<c:if test="${!empty accessManagementBean. modulesList}">
										<tr>
											<td colspan="3" style="border-bottom:1px solid #dedede;">
												<div class="modulesListDisplay">
													<ul class="menu">
														<c:forEach var="modulesList" items="${accessManagementBean. modulesList }">
															<li>
																<div class="label">
																	<a href="javascript:void(0);">
																		<c:out value="${modulesList.moduleDisplayName }"/>
																	</a>
																</div>	
																<div class="chkBox">
																	<input type="checkbox" name="ACM_ModChkBox" onclick="acm_modcheckboxselection(this);"
																		id="ACM_ModChkBox_<c:out value="${modulesList.moduleId }" />" 
																		value="<c:out value="${modulesList.moduleId }" />" 
																		<c:if test="${modulesList.itemSelected eq true }"> checked="checked" </c:if> />
																</div>
																<c:if test="${!empty modulesList.itemDetails }">
																	<ul class="submenu">
																		<c:forEach var="itemDetails" items="${modulesList.itemDetails  }">
																			<li>
																				<div class="label"><a href="javascript:void(0);"><c:out value="${itemDetails.itemName }"/></a></div>
																				<div class="chkBox">
																					<input type="checkbox" onclick="acm_moditmcheckboxselection(this);" 
																						name="ACM_ModChkBox_<c:out value="${modulesList.moduleId }" />" 
																						id="ACM_ModChkBox_<c:out value="${modulesList.moduleId }" />_<c:out value="${itemDetails.itemCode }" />" 
																						value="<c:out value="${itemDetails.itemCode }" />" <c:if test="${itemDetails.itemSelected eq true }"> checked="checked" </c:if> />
																				</div>
																			</li>
																		</c:forEach>
																	</ul>
																</c:if>
															</li>
														</c:forEach>
													</ul>	
												</div>
											</td>
										</tr>
									</c:if>
									
									<tr>
										<td colspan="3">&nbsp;</td>
									</tr>
									<tr>
										<td colspan="3" align="right">
											<c:if test="${accessManagementBean.showWriteControls eq true }">
												<input type="button" name="ACM_Save" id="ACM_Save" onclick="acm_saveData();" class="bluebutton cursorPointer"  value="<fmt:message key="label.save" />"/>
											</c:if> 
											<input type="button" name="ACM_Reset" id="ACM_Reset" class="gear_button cursorPointer"  value="<fmt:message key="label.reset" />" onclick="acm_reset_withDialog();"/>
										</td>
									</tr>
								</table>
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

</body>
<!-- FOOTER STARTS -->
	<jsp:include page="footer.jsp" flush="true" />
<!-- FOOTER ENDS -->

	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});


$(document).ready(function() {
} );

function acm_modcheckboxselection(thisObj)
{
	var value = thisObj.value;
	// itemsName
	var name="ACM_ModChkBox_"+value;
	if($(thisObj).prop("checked")==true)
	{
		// CHECK IT ALSO CHECK ALL ITS CHILD ITEMS
		$(thisObj).prop("checked",true);
		$("input[name='"+name+"']").each( function () {
			$(this).prop("checked",true);
		});
	}
	else if($(thisObj).prop("checked")==false)
	{
		// UNCHECK IT AND ALSO UNCHECK ALL ITS CHILD ITEMS
		$(thisObj).prop("checked",false);
		$("input[name='"+name+"']").each( function () {
			$(this).prop("checked",false);
		});
	}	
}

function acm_moditmcheckboxselection(thisObj)
{
	// IF EITHER OF THE CHILD IS SELECTED, SELECT ITS PARENT TOO
	var name=thisObj.name;
	// CHECK THE CURRENT VALUE IF TRUE, SET PARENT AS TRUE
	if($(thisObj).prop("checked")==true)
	{
		$("#"+name).prop("checked",true);
	}
	else if($(thisObj).prop("checked")==false)
	{
		// CHECK FOR ALL THE NEXT CHILDS OF SAME MODULE, IF ANY IS TRUE, LET PARENT BE TRUE
		// ELSE SET PARENT FALSE
		var parentStatus=false;
		$("input[name='"+name+"']").each( function () {
			if($(this).prop("checked")==true)
			{
				parentStatus = true;
				return false;
			}	
		});
		
		if(parentStatus==true)
		{
			$("#"+name).prop("checked",true);
		}	
		else
		{
			$("#"+name).prop("checked",false);
		}	
	}	
}

function acm_saveData()
{
	// resetErrorMessage
	$("#MDM_ACM_Error_Message").html("");
	// resetSelectedValue
	$("#ACM_SelectedModules").val("");
	var roleId=$("#ACM_RoleId").val();
	if(null!=roleId && roleId!="" && roleId!="null")
	{	
		var dataSelected="";
		var name="ACM_ModChkBox";
		var parTotal=0;
		
		$("input[name='"+name+"']").each( function () {
			if($(this).prop("checked")==true)
			{
				parTotal++;
			}
		});
		
		var parIndex=0;
		$("input[name='"+name+"']").each( function () {
			if($(this).prop("checked")==true)
			{
				parIndex++;
				// CHECK FOR ITS CHILDS
				var childValues="";
				var childName = $(this).attr("id");
				var total = 0;
				$("input[name='"+childName+"']").each( function () {
					if($(this).prop("checked")==true)
					{
						total++;
					}
				});
				var index=0;
				$("input[name='"+childName+"']").each( function () {
					if($(this).prop("checked")==true)
					{
						index++;
						childValues=childValues+$(this).val();
						if (index !== total) 
						{
							childValues = childValues+"||";
						}	
					}	
				});
				if(null!=childValues && childValues!="")
				{
					dataSelected = dataSelected+$(this).val()+":"+childValues;
					if(parIndex!== parTotal)
					{
						dataSelected = dataSelected+"#";
					}
				}	
			}	
		});
		
		if(null!=dataSelected && dataSelected!="")
		{	
			// show Loader
			$("#loader").show();
			$("#ACM_SelectedModules").val(dataSelected);
			$("#ACM_ActionClicked").val("SAVE");
			 $("#ACM_Form").submit();
		}	
		else
		{
			// show errorMessage
			var message="<fmt:message key="error.select.onemodule.to.map" />";
			mdm_show_errorMessage(message);
		}
	}
	else
	{
		// show errorMessage
		var message="<fmt:message key="error.mandatory.fields" />";
		mdm_show_errorMessage(message);
	}
}



function mdm_show_errorMessage(delMessage)
{
	var html="<div class=\"errorMessage\">"+delMessage+"</div>";
	$("#MDM_ACM_Error_Message").html(html);
	window.scrollTo(0,0);
}

function acm_roleSelection()
{
	// show Loader
	$("#loader").show();
	$("#ACM_ActionClicked").val("ROLE_CHANGE");
	 $("#ACM_Form").submit();
}

function acm_reset_withDialog()
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
	    		$("#ACM_ActionClicked").val("RESET");
		         $( this ).dialog( "close" );
		      		// show Loader
					$("#loader").show();
		         $("#ACM_Form").submit();
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
						 $("#ACM_ActionClicked").val("RESET");
						 $("#ACM_Form").submit();
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