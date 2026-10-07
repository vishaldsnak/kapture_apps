<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="x-ua-compatible" content="IE=11,10,9" >
<%
	String locale=ApplicationProperties.getProperty("locales.values.english");
	Object localeObj=request.getSession().getAttribute("DMT_LS_Locale");
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
<fmt:setBundle basename="com.mazda.gms3.dmt.properties.nl.dmtresource_en" />
<% 		
	} else if(locale.equals(ApplicationProperties.getProperty("locales.values.japanese")))
	{
%>
<fmt:setBundle basename="com.mazda.gms3.dmt.properties.nl.dmtresource_jp" />
<% 		
	}
%>
<title><fmt:message key="label.title" /> - <fmt:message key="label.error" />  </title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />

<style>
	
	.header ul
	{
		float:right;
		list-style: none;
	}
	.header ul li
	{
		float: left;
		list-style: none;
		margin:15px;
		text-align: center;
		vertical-align: middle;
		position:relative;
	}
	.header ul li span
	{
		font-size: 23px;
		/*text-transform: uppercase;*/
		color:#555555;
	}
	
	.langIcon:after
	{
		cursor: pointer;
	}
	.header ul li div
	{
		margin:2px;
	}
	.header ul li div p
	{
		float: left;
		padding-top: 8px;
		padding-left: 5px;
		color: #5C5B65;
	    font-size: 9px;
	    border: none;
	    cursor: pointer;
	}
	
	.header ul li ul li {
		float:none;
		margin:5px 0px 15px 10px !important;
	}
	
	.header ul li ul
	{
		margin:0px 0px 15px 0px !important;
		float:right;
		position:absolute;
		left:-115px;
		top:100%;
		background-color:#fff;
		width:150px;
		box-shadow:0px 2px 4px;
		z-index:99;
	}
	
	.header ul li:hover ul
	{
		box-shadow:0px 2px 4px;
	} 
	.header ul li ul li
	{
		text-align:left;
	}
	
	.header ul li ul li select{
		height: 30px;margin-top: 2px; width:100px; font-family:"InterstateMazda-Regular",Arial;
		border:1px solid #DADADA;
		border-radius:2px;
		
	}
	
</style>
<!--[if IE]> 
<style type="text/css">
.header ul li ul
{
	border:1px solid #DADADA;
	left:-137px !important;
}
.header ul li:hover ul
{
	border:1px solid #DADADA;
}
.header ul
{
	margin-top:-59px !important;
}
.header ul li div p
{
	padding-top:5px !important;
}

</style>
 <![endif]-->
<style type="text/css">
.cursorPointer {
	cursor: pointer;
}
.ui-dialog{width:500px !important; height: auto !important;}
.ui-dialog .ui-dialog-title{color:#fff !important;}

</style>
</head>


<body>
	<form action="<%=request.getContextPath() %>/error" method="post">
		<div id="mainWrapper">
			<div class="header">
				<img src="images/u7.png" />
				<ul>
					<li><span><fmt:message key="dmt.title.desc"/></span></li>
				</ul>	
			</div>
			<div class="padder">
				<div class="contentBox">
						<div class="convertInfobox">
						
							<div class="managerInfo">
								<div class="managerSettings">
									<table width="100%" border="0">
										<tr>
											<td>
												<fmt:message key="label.error.screen.text"/>
											</td>
										</tr>
										<tr>
											<td>
												&nbsp;
											</td>
										</tr>
										<tr>
											<td>
												<fmt:message key="label.error.screen.help.text.start"/> <a href="<%=request.getContextPath() %>/schedule"><fmt:message key="label.here"/></a> <fmt:message key="label.error.screen.help.text.end"/>
											</td>
										</tr>
									</table>
								</div>
							</div>
								
						</div>
				</div>
			</div>
		</div>
	</form>
</body>
<script type="text/javascript" src="js/external/jquery/jquery.js"></script>
<script type="text/javascript" src="js/jquery-ui.js"></script>
<script type="text/javascript" src="js/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>

<script type="text/javascript" src="js/jquery.loadmask.js"></script>
</html>

