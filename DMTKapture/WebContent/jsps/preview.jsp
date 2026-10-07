<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%!
	/** Last-modified time of a web file - appended to its URL so a new deploy is never served from the browser cache. */
	private static String pvVersion(javax.servlet.ServletContext ctx, String path) {
		try {
			java.net.URL u = ctx.getResource(path);
			if (null != u) {
				return String.valueOf(u.openConnection().getLastModified());
			}
		} catch (Exception e) {
			// no version: the plain URL
		}
		return "0";
	}
%>
<%
	response.setHeader("Cache-Control", "no-store");
	response.setDateHeader("Expires", 0);
	response.setHeader("Pragma", "no-cache");
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Content Preview - Job <c:out value="${previewScheduleId}" /></title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<link href="css/preview.css?v=<%=pvVersion(application, "/css/preview.css") %>" type="text/css" rel="stylesheet" />
</head>
<body class="pvBody">
	<div class="header pvHeader">
		<img src="images/u7.png" />
		<div class="pvHeaderTitle">Content Preview</div>
	</div>

	<div class="pvContext">
		<div class="pvContextLeft">
			<strong>Job</strong>&nbsp;<span id="pvJobName"><c:out value="${previewScheduleName}" /></span>
			<span class="pvSep">|</span>
			<strong>Schedule Id</strong>&nbsp;<c:out value="${previewScheduleId}" />
			<span class="pvSep">|</span>
			<strong>Unpublished documents</strong>&nbsp;<span id="pvDocCount">-</span>
			<span class="pvSep">|</span>
			<strong>Published</strong>&nbsp;<span id="pvPubCount">-</span>
		</div>
		<div class="pvContextRight">
			<button type="button" class="bluebutton pvPublish" id="pvPublish" disabled="disabled"
				title="Publishes every unpublished document of this job in Kapture">Publish Content</button>
		</div>
	</div>

	<c:choose>
		<c:when test="${previewAvailable}">
			<div class="pvFilters">
				<div class="pvFilter" id="pvCarlineFilter"><span class="pvFilterLabel">Carline</span><div class="pvMulti" id="pvCarline"></div></div>
				<%-- MNAO jobs (preview.json "mnao": true): Model and Year in place of Carline - preview.js shows them --%>
				<div class="pvFilter" id="pvModelFilter" style="display:none"><span class="pvFilterLabel">Model</span><div class="pvMulti" id="pvModel"></div></div>
				<div class="pvFilter" id="pvYearFilter" style="display:none"><span class="pvFilterLabel">Year</span><div class="pvMulti" id="pvYear"></div></div>
				<div class="pvFilter"><span class="pvFilterLabel">WMI</span><div class="pvMulti" id="pvWmi"></div></div>
				<div class="pvFilter"><span class="pvFilterLabel">VDS</span><div class="pvMulti" id="pvVds"></div></div>
				<div class="pvFilter"><span class="pvFilterLabel">VIS Range</span><div class="pvMulti" id="pvVis"></div></div>
				<button type="button" class="bluebutton" id="pvReset">Reset</button>
			</div>

			<div class="pvMain">
				<div class="pvTree" id="pvTree"><div class="pvInfo">Loading...</div></div>
				<div class="pvContent">
					<div class="pvDocHeader" id="pvDocHeader">
						<div class="pvInfo">Select a document in the tree.</div>
					</div>
					<iframe class="pvFrame" id="pvFrame" title="Document"></iframe>
				</div>
			</div>
		</c:when>
		<c:otherwise>
			<div class="pvMain">
				<div class="pvInfo pvNone">There is nothing to preview for this job: it has no unpublished document.</div>
			</div>
		</c:otherwise>
	</c:choose>

	<div class="pvModalBack pvBusyBack" id="pvBusy">
		<div class="pvBusyBox"><div class="pvSpinner"></div><div>Scheduling the publish job, please wait...</div></div>
	</div>

	<div class="pvModalBack" id="pvModal">
		<div class="pvModal" id="pvModalBox">
			<div class="pvModalTitle" id="pvModalTitle">Content Preview</div>
			<div class="pvModalText" id="pvModalText"></div>
			<div class="pvModalButtons">
				<button type="button" class="bluebutton pvHidden" id="pvModalConfirm">Publish</button>
				<button type="button" class="bluebutton" id="pvModalClose">Close</button>
			</div>
		</div>
	</div>

	<c:if test="${previewAvailable}">
		<script type="text/javascript">
			var DMT_PREVIEW = {
				dataUrl : "<%=request.getContextPath()%>/preview?data=Y&schedule=<c:out value="${previewScheduleId}" />",
				publishUrl : "<%=request.getContextPath()%>/preview",
				scheduleId : "<c:out value="${previewScheduleId}" />",
				webFolder : "<c:out value="${previewWebFolder}" />"
			};
		</script>
		<script type="text/javascript" src="js/preview.js?v=<%=pvVersion(application, "/js/preview.js") %>"></script>
	</c:if>
</body>
</html>
