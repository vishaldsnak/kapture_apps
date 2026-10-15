<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">


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
	
<div class="header">
	<img src="images/u7.png" />
	<ul>
		<li><span>Assets Management Tool</span></li>
	</ul>
</div>