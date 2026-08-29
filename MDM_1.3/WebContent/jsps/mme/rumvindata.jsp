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
	request.setAttribute("PAGE_NAME", AccessManagementInterface.REF_KEY_RUM_VIN_DATA);
%>


<%
	String applicationContext=ApplicationProperties.getProperty("application.environment.context");
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
<jsp:useBean id="rumVinDataBean" class="com.mazda.gms3.mdm.bean.RumVinDataBean" scope="session"></jsp:useBean>

<title><fmt:message key="mdm.title" /> - <fmt:message key="label.dash.RUM_VIN_DATA"/> </title>
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
.ui-dialog{width:500px !important; height: auto !important;top: 100px !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

#mdm_reports_dialog-confirm
{
	
	height: auto !important;
}
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
           		<a href="javascript:void(0);"><i class="homeIcon"></i> <fmt:message key="label.dash.DASHBOARD" /> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.rumvin"/> &raquo;</a>  <a href="javascript:void(0);"><fmt:message key="label.dash.RUM_VIN_DATA"/> &rsaquo;</a> 
           	</div>
           	
            <form id="RUMVINDATA_Form" name="RUMVINDATA_Form" action="<%=request.getContextPath() %>/rumvindata" method="post">
			<input type="hidden" name="RUMVINDATA_ActionClicked" id="RUMVINDATA_ActionClicked" value="">
            <div class="padder">
            	<div class="contentBox">
                	<div class="convertInfobox">
                    	<div class="managerInfo">
                        	<!--<h3><fmt:message key="label.countrylocale"/> <fmt:message key="label.details"/>:</h3>-->
							<div class="managerSettings">
								<h4>
									<fmt:message key="label.dash.RUM_VIN_DATA" /> <fmt:message key="label.details"/>
								</h4>
								<br />
								
								<table  class="historyTable" cellspacing="0"
									width="100%" style="table-layout:fixed !important;">
									<tr>
						        		<td width="100%" style="text-align:center;border-top:2px solid #DADADA;border-left: 1px solid #DADADA">
					        				<a href="javascript:void(0);" class="pagButtons" <c:if test="${rumVinDataBean.firstDisbled eq false }"> onclick="rumvin_Navigation('FIRST');" </c:if>><i class="firstNav"></i></a>
					        				<a href="javascript:void(0);" class="pagButtons" <c:if test="${rumVinDataBean.previousDisabled eq false }"> onclick="rumvin_Navigation('PREVIOUS');" </c:if>><i class="prevNav"></i></a>
											<span class="paginate_page"><fmt:message key="label.page" /></span> 
											<input type="text" readonly="readonly" value="<c:out value="${rumVinDataBean.currentPageNo }" />" 
											class="paginate_input" />
											<span class="paginate_of"><fmt:message key="label.of" /> 
						        			<c:out value="${rumVinDataBean.totalPages }"></c:out>
											</span> 
											<a href="javascript:void(0);" class="pagButtons" <c:if test="${rumVinDataBean.nextDisabled eq false }"> onclick="rumvin_Navigation('NEXT');" </c:if>><i class="nextNav"></i></a>
					        				<a href="javascript:void(0);" class="pagButtons" <c:if test="${rumVinDataBean.lastDisabled eq false }"> onclick="rumvin_Navigation('LAST');" </c:if>><i class="lastNav"></i></a>
						        		</td>
						        	</tr>
								</table>	
								
								<table class="historyTable" cellspacing="0"
									width="100%" style="table-layout:fixed !important;">
									<thead>
										<tr>
											<th scope="col" style="border-left: 1px solid #DADADA;text-align: center;">#.</th>
											<th scope="col" style="text-align: center;"><fmt:message key="label.russiavin"/></th>
											<th scope="col" style="text-align: center;"><fmt:message key="label.mazdavin" /></th>
											<th scope="col" style="text-align: center;"><fmt:message key="label.entrytime" /></th>
											<th scope="col" style="text-align: center;"><fmt:message key="label.updatedtime" /></th>
										</tr>
									</thead>
									<tbody>
										<% if(null!=rumVinDataBean.getJspData() && !"".equals(rumVinDataBean.getJspData())) { %>
										<%=rumVinDataBean.getJspData() %>
										
										<% } else {  %>
											<tr>
												<td colspan="5" style="text-align: center;"><fmt:message key="label.norecordsfound" /> </td>
											</tr>
										<%  } %>
									</tbody>
								</table>	
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


	
<script type="text/javascript">

$(window).load(function() {
	$("#loader").fadeOut("slow");
});

function rumvin_Navigation(value)
{
	// show Loader
	$("#loader").show();
	$("#RUMVINDATA_ActionClicked").val(value);
	$("#RUMVINDATA_Form").submit();
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
						rumvinsch_readDataTableValues();
						 $("#RUMVINDATA_ResetAction").val("Reset");
						 $("#RUMVINDATA_Form").submit();
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