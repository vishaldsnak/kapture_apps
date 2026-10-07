<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@page import="java.util.Date"%>
<%
	String useFederation="0";
	String appContext=ApplicationProperties.getProperty("GMS3_APPLICATION_CONTEXT");
	Object userObj = request.getHeader("iv-user");
	boolean mmeUser=false;
	if(null!=userObj && !"".equals(userObj))
	{
		String val = String.valueOf(userObj);
		if(val.trim().toLowerCase().endsWith("@mazda.co.jp") || val.trim().toLowerCase().endsWith("@mazdaeur.com"))
		{
			useFederation = "1";
			if(val.trim().toLowerCase().endsWith("@mazdaeur.com"))
			{
				mmeUser = true;
			}
		}
	}
	String federationUrl=ApplicationProperties.getProperty("FEDERATION_URL");
	String host=ApplicationProperties.getProperty("GMS3_EMAIL_NOTIF_HOSTNAME");
	String mcHost=ApplicationProperties.getProperty("application.mc.hostname");
	String mcReturnUrl=ApplicationProperties.getProperty("application.mc.redirecturl");
 %>
<footer style="text-align:center;padding-bottom: 20px;" id="footerData">
</footer>

<script type="text/javascript">
	var date = new Date();
	var year = date.getFullYear();
	document.getElementById("footerData").innerHTML="© "+year+" Mazda Motor Corporation";
</script>

<script type="text/javascript" src="js/external/jquery/jquery.js"></script>
<script type="text/javascript" src="js/jquery-ui.js"></script>
<script type="text/javascript" src="js/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>

<script type="text/javascript" src="js/jquery.loadmask.js"></script>
<script type="text/javascript" src="js/input.js"></script>

<!-- JS For Headers -->
<script type="text/javascript">
function setCookie(cname, cvalue, exdays) {
	var d = new Date();
	d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
	var expires = "expires="+d.toUTCString();
	document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function dmt_logout()
{
	var appCon = "<%=appContext%>";
	var logoutUrl="";
	var fed = "<%=useFederation%>";
	var mmeUser = "<%=mmeUser%>";
	if(null!=appCon && appCon!="")
	{
		logoutUrl = logoutUrl+appCon;
	}	
	logoutUrl=logoutUrl+"<%=request.getContextPath()%>/logout";
	// Make Ajax Call
	$.ajax({
		url:logoutUrl,
		type:"post",
		dataType:"text",
		success:function(responseData){
			// set COOKIE
			setCookie('PD-S-SESSION-ID','', 1);
			setCookie('PD-ID','', 1);
			setCookie('IV_JCT','', 1);
			// CLEAR ALL COOKIES
			clearListCookies();
			document.execCommand("ClearAuthenticationCache");
			// redirect to schedule
			var mp="";
			if(null!=appCon && appCon!="")
			{
				mp = mp+appCon;
			}	
			mp=mp+"<%=request.getContextPath()%>";
			if(fed=="1")
			{
				// add Federation Url to the Path & hostDetails with context
				//mp="<%=federationUrl+mcHost%>"+mp;
				//mp="<%=federationUrl+mcReturnUrl %>";
				if(mmeUser=="true")
				{
					mp="<%=mcHost%>/pkmslogout";
				}
				else
				{
					mp="<%=mcHost%>"+mp+"/history";	
				}	
			}
			location.href=mp;
		},
		error:function(xmlhttp, status, error){
			//$("body").unmask();
			alert("error :: "+status+" Failed to Invalidate Session");
		}
	});
}

function clearListCookies()
{   
	var cookies = document.cookie.split(";");
	for (var i = 0; i < cookies.length; i++)
	{   
		var spcook =  cookies[i].split("=");
		deleteCookie(spcook[0].trim());
	}
}
function deleteCookie(cookiename)
{
	var d = new Date();
	d.setDate(d.getDate() - 1);
	var expires = ";expires="+d;
	var name=cookiename;
	setCookie(name,'', 1);
}
</script>