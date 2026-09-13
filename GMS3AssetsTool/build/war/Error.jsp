<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Mazda Service Support Center</title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.structure.min.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.css" type="text/css" rel="stylesheet" />
<link href="js/jquery-ui.theme.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="css/dataTables.jqueryui.min.css" />

<style type="text/css">
	.cursorPointer
	{
		cursor: pointer;
	}
	
	.formElement_row label
	{
		margin-left: 0px !important;
	}
	
	.ui-dialog{width:500px !important; height: auto !important;}
	.ui-dialog .ui-dialog-title{color:#fff !important;}
	
</style>

</head>


<body>
	<form action="<%=request.getContextPath() %>/error" method="post">
		<div id="mainWrapper">
			<div class="header"></div>
			<div class="padder">
				<div class="contentBox">
						<div class="convertInfobox">
						
							<div class="managerInfo">
								<div class="managerSettings">
									<table width="100%" border="0">
										<tr>
											<td>
												Your session has expired, Please click on 
												<input type="submit" id="MS3_LOGIN_CONTINUE" class="bluebutton cursorPointer" name="MS3_LOGIN_CONTINUE" value="Sign In"/>
												 to continue. 
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

<script type="text/javascript">

var serverUrl = "<%=request.getContextPath() %>/error"; 

function ms3_Login()
{
	// Make Ajax Call
	$.ajax({
		url:serverUrl,
		type:"post",
		data:{
			MS3_LOGIN_CONTINUE:"MS3_LOGIN_CONTINUE"
		},
		dataType:"text",
		success:function(responseData){
			if(null != responseData)
			{
				// Do nothing
			}
		},
		error:function(xmlhttp, status, error){
			alert("error :: "+status);
		}
	});	
}

</script>

</html>

