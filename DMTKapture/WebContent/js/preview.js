/*
 * Content Preview page (jsps/preview.jsp).
 * Everything comes from the job's preview.json (written by the job while it ran) and the job's
 * preview folder - no database and no Kapture call.
 *
 * preview.json:
 *   documents[] : id, title, version, channel, type, manualType, url, attachments[{name,url}],
 *                 placements[[sequence, level1, level2, ...]], vins[index into vins],
 *                 published {job, date, version, from, here} once a Publish Content job published it
 *                 (here: scheduled from THIS job - the page is gone; otherwise url is a notice page)
 *   vins[]      : carlineCode, carlineName, wmi, vds, visStart, visEnd (one filter: "start-end")
 *                 MNAO job ("mnao": true): model, year too - filters Model, Year, WMI, VDS, VIS Range (no Carline)
 * A url without a leading / is relative to the job's preview folder.
 */
(function () {
	"use strict";

	var data = null;
	var byId = {};
	var selectedId = null;
	// multi select filters, cascading top-down; nothing ticked in a filter = no restriction
	var CARLINE_FILTER = { id: "pvCarline", text: function (v) { return v.carlineName ? v.carlineName + " (" + v.carlineCode + ")" : v.carlineCode; }, value: function (v) { return v.carlineCode; } };
	// MNAO: model and year in place of the carline
	var MODEL_FILTERS = [
		{ id: "pvModel", text: function (v) { return v.model || ""; }, value: function (v) { return v.model || ""; } },
		{ id: "pvYear", text: function (v) { return v.year || ""; }, value: function (v) { return v.year || ""; } }
	];
	var FILTERS = [
		CARLINE_FILTER,
		{ id: "pvWmi", text: function (v) { return v.wmi; }, value: function (v) { return v.wmi; } },
		{ id: "pvVds", text: function (v) { return v.vds; }, value: function (v) { return v.vds; } },
		// VIS: start-end ranges, in VIS order
		{ id: "pvVis", text: visRange, value: visRange }
	];

	/** Numbers inside a text compare as numbers: 99999-... before 100001-... */
	function naturalCompare(a, b) {
		var re = /(\d+)|(\D+)/g;
		var x = String(a).match(re) || [], y = String(b).match(re) || [];
		for (var i = 0; i < x.length && i < y.length; i++) {
			if (x[i] === y[i]) { continue; }
			var nx = /^\d/.test(x[i]), ny = /^\d/.test(y[i]);
			if (nx && ny && Number(x[i]) !== Number(y[i])) { return Number(x[i]) - Number(y[i]); }
			return x[i] < y[i] ? -1 : 1;
		}
		return x.length - y.length;
	}

	function $(id) { return document.getElementById(id); }

	/** VIS start and end of a VIN as ONE value: start-end */
	function visRange(v) { return v.visStart || v.visEnd ? v.visStart + "-" + v.visEnd : ""; }

	function el(tag, cls, text) {
		var e = document.createElement(tag);
		if (cls) { e.className = cls; }
		if (null != text) { e.appendChild(document.createTextNode(text)); }
		return e;
	}

	function fullUrl(url) {
		if (!url) { return null; }
		return url.charAt(0) === "/" ? url : DMT_PREVIEW.webFolder + url;
	}

	// ---- filters ------------------------------------------------------------------------------

	/** The filters before index upTo that have something ticked. */
	function activeFilters(upTo) {
		var active = [];
		for (var i = 0; i < upTo; i++) {
			if (count(FILTERS[i].selected)) { active.push(FILTERS[i]); }
		}
		return active;
	}

	function count(set) {
		var n = 0;
		for (var k in set) { if (set.hasOwnProperty(k)) { n++; } }
		return n;
	}

	function vinMatches(v, active) {
		for (var i = 0; i < active.length; i++) {
			if (!active[i].selected.hasOwnProperty(active[i].value(v))) { return false; }
		}
		return true;
	}

	/**
	 * Fills filter i with the values of the VINs matching the filters before it, keeps the ticks
	 * that are still offered, then does the same for the filters after it.
	 */
	function fillFilter(i) {
		var f = FILTERS[i];
		var active = activeFilters(i);
		var seen = {};
		var options = [];
		for (var n = 0; n < data.vins.length; n++) {
			var v = data.vins[n];
			if (!vinMatches(v, active)) { continue; }
			var value = f.value(v);
			if (value === "" || seen.hasOwnProperty(value)) { continue; }
			seen[value] = true;
			options.push({ value: value, text: f.text(v) });
		}
		options.sort(function (a, b) { return naturalCompare(a.text, b.text); });
		var kept = {};
		for (var k in f.selected) {
			if (f.selected.hasOwnProperty(k) && seen.hasOwnProperty(k)) { kept[k] = true; }
		}
		f.selected = kept;
		f.options = options;
		renderMulti(f);
		if (i + 1 < FILTERS.length) { fillFilter(i + 1); }
	}

	function documentMatches(d, active) {
		if (!active.length) { return true; }
		for (var n = 0; n < d.vins.length; n++) {
			if (vinMatches(data.vins[d.vins[n]], active)) { return true; }
		}
		return false;
	}

	function filterChanged(i) {
		if (i + 1 < FILTERS.length) { fillFilter(i + 1); }
		renderTree();
	}

	// ---- multi select dropdown ----------------------------------------------------------------

	function closeMultis(except) {
		for (var i = 0; i < FILTERS.length; i++) {
			var box = $(FILTERS[i].id);
			if (box !== except) { box.className = "pvMulti"; }
		}
	}

	function buildMulti(f, i) {
		var box = $(f.id);
		box.innerHTML = "";
		f.button = el("button", "pvMultiButton");
		f.button.type = "button";
		f.panel = el("div", "pvMultiPanel");
		box.appendChild(f.button);
		box.appendChild(f.panel);
		f.selected = {};
		f.options = [];
		f.button.onclick = function () {
			var open = box.className.indexOf("pvMultiOpen") >= 0;
			closeMultis(box);
			box.className = open ? "pvMulti" : "pvMulti pvMultiOpen";
		};
		f.panel.onchange = function (ev) {
			var cb = ev.target;
			if (!cb || cb.type !== "checkbox") { return; }
			if (cb.checked) { f.selected[cb.value] = true; } else { delete f.selected[cb.value]; }
			updateButton(f);
			filterChanged(i);
		};
		f.panel.onclick = function (ev) {
			var a = ev.target;
			if (!a || a.nodeName !== "A") { return; }
			// the panel is rebuilt below: the dropdown must not take this click for an outside click
			ev.stopPropagation();
			f.selected = {};
			if (a.getAttribute("data-act") === "all") {
				for (var k = 0; k < f.options.length; k++) { f.selected[f.options[k].value] = true; }
			}
			renderMulti(f);
			filterChanged(i);
		};
	}

	function renderMulti(f) {
		f.panel.innerHTML = "";
		if (f.options.length) {
			var actions = el("div", "pvMultiActions");
			var all = el("a", null, "Select all");
			all.setAttribute("data-act", "all");
			var none = el("a", null, "Clear");
			none.setAttribute("data-act", "none");
			actions.appendChild(all);
			actions.appendChild(none);
			f.panel.appendChild(actions);
		}
		for (var k = 0; k < f.options.length; k++) {
			var o = f.options[k];
			var label = el("label", "pvMultiOption");
			var cb = document.createElement("input");
			cb.type = "checkbox";
			cb.value = o.value;
			cb.checked = f.selected.hasOwnProperty(o.value);
			label.appendChild(cb);
			label.appendChild(document.createTextNode(o.text));
			f.panel.appendChild(label);
		}
		updateButton(f);
	}

	/** "All" when nothing is ticked, the value when one is, "n selected" when more are. */
	function updateButton(f) {
		var texts = [];
		for (var k = 0; k < f.options.length; k++) {
			if (f.selected.hasOwnProperty(f.options[k].value)) { texts.push(f.options[k].text); }
		}
		f.button.disabled = f.options.length === 0;
		f.button.innerHTML = "";
		f.button.appendChild(document.createTextNode(!f.options.length ? "-" : (!texts.length ? "All" : (texts.length === 1 ? texts[0] : texts.length + " selected"))));
		f.button.title = texts.length ? texts.join("\n") : "All";
	}

	// ---- tree ---------------------------------------------------------------------------------

	function newFolder(name) { return { name: name, folders: {}, order: [], docs: [], seq: null }; }

	function minSeq(a, b) { return null == a ? b : (null == b ? a : (b < a ? b : a)); }

	function buildModel(docs) {
		var roots = {};
		var rootOrder = [];
		for (var i = 0; i < docs.length; i++) {
			var d = docs[i];
			var rootName = d.manualType || "-";
			if (!roots.hasOwnProperty(rootName)) {
				roots[rootName] = newFolder(rootName);
				rootOrder.push(rootName);
			}
			var root = roots[rootName];
			if (!d.placements.length) {
				// no display order: directly under the manual type, beside the display order tree
				root.docs.push({ doc: d, seq: null });
				continue;
			}
			for (var p = 0; p < d.placements.length; p++) {
				var placement = d.placements[p];
				var seq = placement[0] || null;
				var node = root;
				node.seq = minSeq(node.seq, seq);
				for (var l = 1; l < placement.length; l++) {
					var name = placement[l];
					if (!node.folders.hasOwnProperty(name)) {
						node.folders[name] = newFolder(name);
						node.order.push(name);
					}
					node = node.folders[name];
					node.seq = minSeq(node.seq, seq);
				}
				node.docs.push({ doc: d, seq: seq });
			}
		}
		rootOrder.sort();
		return { roots: roots, order: rootOrder };
	}

	function renderFolder(folder, cls) {
		var li = el("li", "pvFolder " + cls);
		var node = el("div", "pvNode");
		node.appendChild(el("span", "pvCaret"));
		node.appendChild(el("span", null, folder.name));
		node.onclick = function () { li.className = li.className.indexOf("pvOpen") >= 0 ? li.className.replace(" pvOpen", "") : li.className + " pvOpen"; };
		li.appendChild(node);
		var ul = el("ul");
		var names = folder.order.slice(0);
		names.sort(function (a, b) {
			var sa = folder.folders[a].seq, sb = folder.folders[b].seq;
			if (sa !== sb) { return null == sa ? 1 : (null == sb ? -1 : (sa < sb ? -1 : 1)); }
			return a < b ? -1 : (a > b ? 1 : 0);
		});
		for (var i = 0; i < names.length; i++) {
			ul.appendChild(renderFolder(folder.folders[names[i]], ""));
		}
		var docs = folder.docs.slice(0);
		docs.sort(function (a, b) {
			if (a.seq !== b.seq) { return null == a.seq ? 1 : (null == b.seq ? -1 : (a.seq < b.seq ? -1 : 1)); }
			return a.doc.id < b.doc.id ? -1 : 1;
		});
		for (var k = 0; k < docs.length; k++) {
			ul.appendChild(renderDocument(docs[k].doc));
		}
		li.appendChild(ul);
		return li;
	}

	/** "pvDoc", plus pvPublishedDoc for a published document (greyed out) */
	function docClass(d) { return d && d.published ? "pvDoc pvPublishedDoc" : "pvDoc"; }

	function publishedText(p) {
		return "Published by " + p.job + " on " + p.date + (p.version ? ", version " + p.version : "")
			+ (p.here ? "" : " (scheduled from job " + p.from + ")");
	}

	function renderDocument(d) {
		var li = el("li", docClass(d));
		li.setAttribute("data-doc", d.id);
		var node = el("div", "pvNode");
		node.appendChild(el("span", "pvLeafCaret"));
		node.appendChild(el("span", "pvId", d.id));
		node.appendChild(el("span", null, d.title));
		if (d.published) {
			node.appendChild(el("span", "pvPublishedLabel", "Published"));
			node.title = publishedText(d.published);
		} else {
			node.title = d.title;
		}
		node.onclick = function () { openDocument(d.id, ""); };
		li.appendChild(node);
		if (d.id === selectedId) { li.className += " pvSelected"; }
		return li;
	}

	function renderTree() {
		var active = activeFilters(FILTERS.length);
		var docs = [];
		for (var i = 0; i < data.documents.length; i++) {
			if (documentMatches(data.documents[i], active)) { docs.push(data.documents[i]); }
		}
		var tree = $("pvTree");
		tree.innerHTML = "";
		if (!docs.length) {
			tree.appendChild(el("div", "pvInfo", "No document of this job matches the filters."));
			return;
		}
		var model = buildModel(docs);
		var ul = el("ul");
		for (var r = 0; r < model.order.length; r++) {
			ul.appendChild(renderFolder(model.roots[model.order[r]], "pvRoot pvOpen"));
		}
		tree.appendChild(ul);
		markSelected();
	}

	function markSelected() {
		var items = $("pvTree").getElementsByTagName("li");
		for (var i = 0; i < items.length; i++) {
			var li = items[i];
			if (li.className.indexOf("pvDoc") < 0) { continue; }
			var on = li.getAttribute("data-doc") === selectedId;
			li.className = docClass(byId[li.getAttribute("data-doc")]) + (on ? " pvSelected" : "");
			if (on) {
				// open the folders above it
				for (var p = li.parentNode; p && p.id !== "pvTree"; p = p.parentNode) {
					if (p.nodeName === "LI" && p.className.indexOf("pvOpen") < 0) { p.className += " pvOpen"; }
				}
			}
		}
	}

	// ---- document pane ------------------------------------------------------------------------

	function openDocument(id, hash) {
		var d = byId[id];
		if (!d) { return; }
		if (d.published && d.published.here) {
			// published from this job: its page is gone
			showMessage(d.id + " - " + publishedText(d.published) + ".");
			return;
		}
		selectedId = id;
		var header = $("pvDocHeader");
		header.innerHTML = "";
		header.appendChild(el("span", "pvDocId", d.id));
		header.appendChild(el("span", "pvDocTitle", d.title));
		var meta = el("div", "pvDocMeta");
		meta.appendChild(el("span", d.published ? "pvBadge pvBadgePublished" : "pvBadge", d.published ? "Status: Published" : "Status: Draft"));
		if (d.published) { meta.appendChild(el("span", null, publishedText(d.published))); }
		if (d.version) { meta.appendChild(el("span", null, "Version " + d.version)); }
		meta.appendChild(el("span", null, d.manualType));
		var url = fullUrl(d.url);
		if (url) {
			var open = el("a", null, "Open in new tab");
			open.href = url;
			open.target = "_blank";
			meta.appendChild(open);
		}
		for (var i = 0; i < d.attachments.length; i++) {
			meta.appendChild(document.createTextNode("  "));
			var a = el("a", null, d.attachments[i].name);
			a.href = fullUrl(d.attachments[i].url);
			a.target = "_blank";
			a.title = "Open or download";
			meta.appendChild(a);
		}
		header.appendChild(meta);
		$("pvFrame").src = url ? url + (hash || "") : "about:blank";
		markSelected();
	}

	/**
	 * onConfirm given: the modal also shows the confirm button, which calls it.
	 * busy given: the items other jobs are processing, shown as a table under the text.
	 */
	function showMessage(text, onConfirm, busy, title) {
		var box = $("pvModalText");
		box.innerHTML = "";
		box.appendChild(el("div", "pvModalLead", text));
		var wide = busy && busy.length;
		if (wide) {
			var table = el("table", "pvBusyTable");
			var head = el("tr");
			var cols = ["Model", "Manual type", "Running job", "Job type"];
			for (var c = 0; c < cols.length; c++) { head.appendChild(el("th", null, cols[c])); }
			table.appendChild(head);
			for (var i = 0; i < busy.length; i++) {
				var tr = el("tr");
				tr.appendChild(el("td", null, busy[i].model));
				tr.appendChild(el("td", null, busy[i].manualType));
				tr.appendChild(el("td", "pvBusyJob", busy[i].job));
				tr.appendChild(el("td", null, busy[i].jobType));
				table.appendChild(tr);
			}
			box.appendChild(table);
		}
		$("pvModalTitle").innerHTML = "";
		$("pvModalTitle").appendChild(document.createTextNode(title || "Content Preview"));
		$("pvModalBox").className = wide ? "pvModal pvModalWide" : "pvModal";
		var confirm = $("pvModalConfirm");
		confirm.className = onConfirm ? "bluebutton" : "bluebutton pvHidden";
		confirm.onclick = onConfirm ? function () { $("pvModal").className = "pvModalBack"; onConfirm(); } : null;
		$("pvModal").className = "pvModalBack pvShow";
	}

	// ---- publish ------------------------------------------------------------------------------

	function unpublishedCount() {
		var n = 0;
		for (var i = 0; i < data.documents.length; i++) { if (!data.documents[i].published) { n++; } }
		return n;
	}

	var publishing = false;

	function publish() {
		// one request at a time: the overlay blocks the page until the answer is shown
		if (publishing) { return; }
		publishing = true;
		var button = $("pvPublish");
		button.disabled = true;
		$("pvBusy").className = "pvModalBack pvBusyBack pvShow";
		var xhr = new XMLHttpRequest();
		xhr.open("POST", DMT_PREVIEW.publishUrl, true);
		xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		xhr.onreadystatechange = function () {
			if (xhr.readyState !== 4) { return; }
			publishing = false;
			$("pvBusy").className = "pvModalBack pvBusyBack";
			var answer = null;
			try { answer = JSON.parse(xhr.responseText); } catch (err) { answer = null; }
			if (answer && answer.ok) {
				button.title = answer.message;
				showMessage(answer.message, null, null, "Publish job scheduled");
				return;
			}
			button.disabled = false;
			if (answer && answer.busy && answer.busy.length) {
				showMessage(answer.message, null, answer.busy, "Cannot publish yet");
				return;
			}
			showMessage(answer && answer.message ? answer.message : "The publish job could not be scheduled (HTTP " + xhr.status + ").", null, null,
				"Cannot publish");
		};
		xhr.send("action=publish&schedule=" + encodeURIComponent(DMT_PREVIEW.scheduleId));
	}

	// links inside a preview page arrive here (_assets/preview_page.js)
	window.addEventListener("message", function (e) {
		if (!e.data || !e.data.dmtPreview || e.source !== $("pvFrame").contentWindow) { return; }
		if (e.data.dmtPreview === "open") {
			if (byId.hasOwnProperty(e.data.id)) {
				openDocument(e.data.id, e.data.hash);
			} else {
				showMessage("Document " + e.data.id + " is not part of this job.");
			}
		} else if (e.data.dmtPreview === "other") {
			showMessage("This is an other manual link: " + e.data.link);
		} else if (e.data.dmtPreview === "shown" &&byId.hasOwnProperty(e.data.id) && e.data.id !== selectedId) {
			selectedId = e.data.id;
			markSelected();
		}
	});

	// ---- start --------------------------------------------------------------------------------

	function start(json) {
		data = json;
		for (var i = 0; i < data.documents.length; i++) { byId[data.documents[i].id] = data.documents[i]; }
		var unpublished = unpublishedCount();
		$("pvDocCount").innerHTML = String(unpublished);
		$("pvPubCount").innerHTML = String(data.documents.length - unpublished);
		var button = $("pvPublish");
		button.disabled = unpublished === 0;
		button.onclick = function () {
			showMessage("Publish the " + unpublished + " unpublished document(s) of this job in Kapture? The latest version of each document is published,"
				+ " whichever job wrote it. Documents deleted by a later job are not published.", publish);
		};
		if (data.mnao === true) {
			FILTERS.splice.apply(FILTERS, [0, 1].concat(MODEL_FILTERS));
			$("pvCarlineFilter").style.display = "none";
			$("pvModelFilter").style.display = "";
			$("pvYearFilter").style.display = "";
		}
		for (var f = 0; f < FILTERS.length; f++) { buildMulti(FILTERS[f], f); }
		// a click outside an open dropdown, or Escape, closes it
		document.addEventListener("click", function (ev) {
			for (var t = ev.target; t; t = t.parentNode) {
				if (t.className && String(t.className).indexOf("pvMulti") === 0) { return; }
			}
			closeMultis(null);
		});
		document.addEventListener("keydown", function (ev) { if (ev.key === "Escape") { closeMultis(null); } });
		// a click in the document pane (an iframe) never reaches this page - the page loses the focus instead
		window.addEventListener("blur", function () { closeMultis(null); });
		$("pvReset").onclick = function () {
			for (var r = 0; r < FILTERS.length; r++) { FILTERS[r].selected = {}; }
			closeMultis(null);
			fillFilter(0);
			renderTree();
		};
		fillFilter(0);
		renderTree();
	}

	$("pvModalClose").onclick = function () { $("pvModal").className = "pvModalBack"; };

	var xhr = new XMLHttpRequest();
	xhr.open("GET", DMT_PREVIEW.dataUrl, true);
	xhr.onreadystatechange = function () {
		if (xhr.readyState !== 4) { return; }
		if (xhr.status === 200) {
			try {
				start(JSON.parse(xhr.responseText));
				return;
			} catch (err) {
				if (window.console) { console.log(err); }
			}
		}
		$("pvTree").innerHTML = "";
		$("pvTree").appendChild(el("div", "pvInfo", "The preview of this job could not be loaded."));
	};
	xhr.send();
})();
