<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.mazda.gms3.dmt.utils.ApplicationProperties"%>
<!DOCTYPE html>
<%
	/*
	 * INTERNAL UTILITY PAGE.
	 *
	 * NOTE - the wsl.check / iv-user header block that every other DMT page carries is
	 * DELIBERATELY ABSENT here. This screen is reached directly by URL and is not linked
	 * from the application menu. Do not add the WSL check without being asked.
	 *
	 * No database access happens on this page or in its servlet.
	 */
	response.setHeader("Cache-Control", "no-cache, no-store");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);

	String relativePath = (String) request.getAttribute("uploadRelativePath");
	if (null == relativePath) {
		relativePath = "";
	}
	String physicalPath = (String) request.getAttribute("uploadPhysicalPath");
	if (null == physicalPath) {
		physicalPath = "";
	}
	String ctx = request.getContextPath();
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta http-equiv="x-ua-compatible" content="IE=edge" />
<title>File Upload Manager</title>
<link href="<%=ctx%>/css/style.css" type="text/css" rel="stylesheet" />
<style type="text/css">
body {
	font-family: Arial, Helvetica, sans-serif;
	font-size: 13px;
	color: #333;
	margin: 0;
	padding: 0;
	background: #f4f4f4;
}
.fum-wrap {
	max-width: 1100px;
	margin: 20px auto;
	background: #fff;
	border: 1px solid #d5d5d5;
	padding: 20px 24px 28px 24px;
}
.fum-wrap h1 {
	font-size: 20px;
	margin: 0 0 4px 0;
	color: #101010;
}
.fum-sub {
	color: #777;
	font-size: 12px;
	margin-bottom: 18px;
	word-break: break-all;
}
.fum-sub code {
	background: #f0f0f0;
	padding: 1px 4px;
}
.fum-drop {
	border: 2px dashed #b8b8b8;
	background: #fafafa;
	padding: 26px 16px;
	text-align: center;
	color: #666;
	margin-bottom: 14px;
}
.fum-drop.fum-over {
	border-color: #1c6fb8;
	background: #eaf3fb;
	color: #1c6fb8;
}
.fum-btn {
	display: inline-block;
	background: #1c6fb8;
	color: #fff;
	border: none;
	padding: 7px 16px;
	font-size: 13px;
	cursor: pointer;
}
.fum-btn:hover {
	background: #17598f;
}
.fum-btn[disabled] {
	background: #9bb8d1;
	cursor: default;
}
.fum-btn-sec {
	background: #6c757d;
}
.fum-btn-sec:hover {
	background: #565e64;
}
.fum-btn-del {
	background: #b83a3a;
}
.fum-btn-del:hover {
	background: #8f2d2d;
}
table.fum-tbl {
	width: 100%;
	border-collapse: collapse;
	margin-top: 6px;
}
table.fum-tbl th, table.fum-tbl td {
	border: 1px solid #e2e2e2;
	padding: 7px 9px;
	text-align: left;
	vertical-align: middle;
	font-size: 12.5px;
}
table.fum-tbl th {
	background: #f0f0f0;
	font-weight: bold;
}
table.fum-tbl tr:hover td {
	background: #fafcfe;
}
.fum-name {
	word-break: break-all;
}
.fum-bar-outer {
	background: #e6e6e6;
	height: 15px;
	width: 100%;
	position: relative;
	min-width: 140px;
}
.fum-bar-inner {
	background: #1c6fb8;
	height: 15px;
	width: 0%;
}
.fum-bar-inner.fum-ok {
	background: #2e8b46;
}
.fum-bar-inner.fum-err {
	background: #b83a3a;
}
.fum-bar-text {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	line-height: 15px;
	text-align: center;
	font-size: 11px;
	color: #222;
}
.fum-sec {
	margin-top: 26px;
}
.fum-sec h2 {
	font-size: 15px;
	margin: 0 0 8px 0;
	border-bottom: 1px solid #e2e2e2;
	padding-bottom: 6px;
}
.fum-msg {
	padding: 8px 10px;
	margin-bottom: 12px;
	display: none;
}
.fum-msg-ok {
	background: #e5f3e9;
	border: 1px solid #b7dcc3;
	color: #226b38;
}
.fum-msg-err {
	background: #fbeaea;
	border: 1px solid #e5bcbc;
	color: #8f2d2d;
}
.fum-empty {
	color: #888;
	padding: 14px 4px;
	font-style: italic;
}
.fum-right {
	text-align: right;
}
.fum-actions a {
	margin-right: 10px;
}
</style>
</head>
<body>

<div class="fum-wrap">

	<h1>File Upload Manager</h1>
	<div class="fum-sub">
		Internal utility. Files are stored at <code><%=physicalPath%></code>
		and served from <code><%=relativePath%></code>
	</div>

	<div id="fumMsg" class="fum-msg"></div>

	<!-- ================= UPLOAD ================= -->
	<div class="fum-sec">
		<h2>Upload</h2>

		<div id="fumDrop" class="fum-drop">
			Drag and drop files here, or
			<button type="button" class="fum-btn" id="fumBrowse">Choose files</button>
			<div style="margin-top:8px;font-size:11.5px;color:#999;">
				Any file type. Any size. Multiple files supported - each uploads separately with its own progress.
			</div>
		</div>

		<!-- kept out of a form on purpose: uploads are sent one file per XHR -->
		<input type="file" id="fumFiles" multiple="multiple" style="display:none;" />

		<div>
			<button type="button" class="fum-btn" id="fumUpload" disabled="disabled">Start upload</button>
			<button type="button" class="fum-btn fum-btn-sec" id="fumClear">Clear list</button>
		</div>

		<table class="fum-tbl" id="fumQueueTbl" style="display:none;">
			<thead>
				<tr>
					<th style="width:38%;">File</th>
					<th style="width:12%;">Size</th>
					<th style="width:32%;">Progress</th>
					<th style="width:18%;">Status</th>
				</tr>
			</thead>
			<tbody id="fumQueueBody"></tbody>
		</table>
	</div>

	<!-- ================= EXISTING FILES ================= -->
	<div class="fum-sec">
		<h2>
			Files in folder
			<button type="button" class="fum-btn fum-btn-sec" id="fumRefresh" style="float:right;margin-top:-4px;">Refresh</button>
		</h2>
		<div id="fumListWrap">
			<div class="fum-empty">Loading...</div>
		</div>
	</div>

</div>

<script type="text/javascript" src="<%=ctx%>/js/external/jquery/jquery.js"></script>
<script type="text/javascript">
/* global jQuery */
(function ($) {
	"use strict";

	var SERVLET = "<%=ctx%>/fileuploadmanager";

	var queue = [];      // {file, row, state}
	var uploading = false;

	/* ---------------- helpers ---------------- */

	function esc(s) {
		if (s === null || typeof s === "undefined") { return ""; }
		return String(s)
			.replace(/&/g, "&amp;")
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;")
			.replace(/'/g, "&#39;");
	}

	function fmtSize(bytes) {
		if (bytes < 1024) { return bytes + " B"; }
		if (bytes < 1024 * 1024) { return Math.round(bytes / 1024) + " KB"; }
		if (bytes < 1024 * 1024 * 1024) { return (bytes / (1024 * 1024)).toFixed(1) + " MB"; }
		return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
	}

	function showMsg(text, isError) {
		var $m = $("#fumMsg");
		$m.removeClass("fum-msg-ok fum-msg-err")
		  .addClass(isError ? "fum-msg-err" : "fum-msg-ok")
		  .text(text)
		  .show();
	}

	function hideMsg() {
		$("#fumMsg").hide();
	}

	/* ---------------- queue ---------------- */

	function addFiles(fileList) {
		var i;
		for (i = 0; i < fileList.length; i++) {
			addOne(fileList[i]);
		}
		if (queue.length > 0) {
			$("#fumQueueTbl").show();
			$("#fumUpload").prop("disabled", uploading);
		}
	}

	function addOne(file) {
		var $row = $(
			"<tr>" +
				"<td class='fum-name'>" + esc(file.name) + "</td>" +
				"<td>" + fmtSize(file.size) + "</td>" +
				"<td>" +
					"<div class='fum-bar-outer'>" +
						"<div class='fum-bar-inner'></div>" +
						"<div class='fum-bar-text'>0%</div>" +
					"</div>" +
				"</td>" +
				"<td class='fum-status'>Queued</td>" +
			"</tr>"
		);
		$("#fumQueueBody").append($row);
		queue.push({ file: file, $row: $row, state: "QUEUED" });
	}

	function setProgress(item, pct) {
		item.$row.find(".fum-bar-inner").css("width", pct + "%");
		item.$row.find(".fum-bar-text").text(pct + "%");
	}

	function setDone(item, ok, statusText) {
		item.$row.find(".fum-bar-inner")
			.css("width", "100%")
			.addClass(ok ? "fum-ok" : "fum-err");
		item.$row.find(".fum-bar-text").text(ok ? "100%" : "");
		item.$row.find(".fum-status").text(statusText);
		item.state = ok ? "DONE" : "FAILED";
	}

	/* ---------------- upload ---------------- */

	function startUpload() {
		if (uploading) { return; }
		hideMsg();
		uploading = true;
		$("#fumUpload").prop("disabled", true);
		uploadNext(0);
	}

	/*
	 * One file per request, sequentially. Sequential rather than parallel so a batch of
	 * large files cannot saturate the connection or the server's thread pool, and so the
	 * per-file progress readings stay meaningful.
	 */
	function uploadNext(index) {
		if (index >= queue.length) {
			uploading = false;
			$("#fumUpload").prop("disabled", queue.length === 0);
			loadList();
			return;
		}

		var item = queue[index];

		if (item.state === "DONE") {
			uploadNext(index + 1);
			return;
		}

		item.state = "UPLOADING";
		item.$row.find(".fum-status").text("Uploading...");

		var fd = new FormData();
		fd.append("file", item.file, item.file.name);

		var xhr = new XMLHttpRequest();
		xhr.open("POST", SERVLET, true);

		// native per-file progress
		if (xhr.upload) {
			xhr.upload.onprogress = function (e) {
				if (e.lengthComputable) {
					setProgress(item, Math.round((e.loaded / e.total) * 100));
				}
			};
		}

		xhr.onload = function () {
			var res = null;
			try {
				res = JSON.parse(xhr.responseText);
			} catch (err) {
				res = null;
			}
			if (xhr.status === 200 && res && res.status === "SUCCESS") {
				setDone(item, true, res.renamed ? "Saved as " + res.savedName : "Uploaded");
			} else {
				setDone(item, false, (res && res.message) ? res.message : ("Failed (HTTP " + xhr.status + ")"));
			}
			uploadNext(index + 1);
		};

		xhr.onerror = function () {
			setDone(item, false, "Network error");
			uploadNext(index + 1);
		};

		xhr.onabort = function () {
			setDone(item, false, "Aborted");
			uploadNext(index + 1);
		};

		xhr.send(fd);
	}

	/* ---------------- listing ---------------- */

	function loadList() {
		$.ajax({
			url: SERVLET,
			type: "GET",
			data: { action: "list", ts: new Date().getTime() },
			dataType: "json"
		}).done(function (res) {
			renderList(res);
		}).fail(function () {
			$("#fumListWrap").html("<div class='fum-empty'>Could not load the file list.</div>");
		});
	}

	function renderList(res) {
		if (!res || res.status !== "SUCCESS") {
			$("#fumListWrap").html("<div class='fum-empty'>" +
				esc((res && res.message) ? res.message : "Could not read the folder.") + "</div>");
			return;
		}
		if (!res.files || res.files.length === 0) {
			$("#fumListWrap").html("<div class='fum-empty'>No files in the folder.</div>");
			return;
		}

		var html = "<table class='fum-tbl'><thead><tr>" +
			"<th style='width:46%;'>File name</th>" +
			"<th style='width:12%;'>Size</th>" +
			"<th style='width:22%;'>Last modified</th>" +
			"<th style='width:20%;'>Actions</th>" +
			"</tr></thead><tbody>";

		var i, f;
		for (i = 0; i < res.files.length; i++) {
			f = res.files[i];
			html += "<tr>" +
				"<td class='fum-name'>" + esc(f.name) + "</td>" +
				"<td>" + esc(f.sizeLabel) + "</td>" +
				"<td>" + esc(f.modified) + "</td>" +
				"<td class='fum-actions'>" +
					"<a href='" + esc(f.url) + "' target='_blank' rel='noopener'>Download</a>" +
					"<button type='button' class='fum-btn fum-btn-del fum-del' data-file='" + esc(f.name) + "'>Delete</button>" +
				"</td>" +
			"</tr>";
		}
		html += "</tbody></table>";
		$("#fumListWrap").html(html);
	}

	function deleteFile(name) {
		if (!window.confirm("Delete '" + name + "' ?\n\nThis cannot be undone.")) {
			return;
		}
		hideMsg();
		$.ajax({
			url: SERVLET,
			type: "POST",
			data: { action: "delete", file: name },
			dataType: "json"
		}).done(function (res) {
			if (res && res.status === "SUCCESS") {
				showMsg(res.message, false);
			} else {
				showMsg((res && res.message) ? res.message : "Delete failed.", true);
			}
			loadList();
		}).fail(function () {
			showMsg("Delete request failed.", true);
			loadList();
		});
	}

	/* ---------------- wiring ---------------- */

	$(document).ready(function () {

		$("#fumBrowse").on("click", function () {
			$("#fumFiles").click();
		});

		$("#fumFiles").on("change", function () {
			if (this.files && this.files.length > 0) {
				addFiles(this.files);
			}
			// reset so selecting the same file again still fires change
			$(this).val("");
		});

		$("#fumUpload").on("click", startUpload);

		$("#fumClear").on("click", function () {
			if (uploading) { return; }
			queue = [];
			$("#fumQueueBody").empty();
			$("#fumQueueTbl").hide();
			$("#fumUpload").prop("disabled", true);
			hideMsg();
		});

		$("#fumRefresh").on("click", loadList);

		$("#fumListWrap").on("click", ".fum-del", function () {
			deleteFile($(this).attr("data-file"));
		});

		// drag and drop
		var $drop = $("#fumDrop");
		$drop.on("dragover dragenter", function (e) {
			e.preventDefault();
			e.stopPropagation();
			$drop.addClass("fum-over");
		});
		$drop.on("dragleave dragend drop", function (e) {
			e.preventDefault();
			e.stopPropagation();
			$drop.removeClass("fum-over");
		});
		$drop.on("drop", function (e) {
			var dt = e.originalEvent.dataTransfer;
			if (dt && dt.files && dt.files.length > 0) {
				addFiles(dt.files);
			}
		});
		// stop the browser from navigating when a file is dropped outside the zone
		$(document).on("dragover drop", function (e) {
			e.preventDefault();
		});

		loadList();
	});

}(jQuery));
</script>

</body>
</html>
