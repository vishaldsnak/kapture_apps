<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Content Provision Guide shown under the network path on the Settings page (English only). --%>
<div class="sg-wrap" id="sgGuide">
	<div class="sg-head">
		<a class="sg-pdf" href="<%=request.getContextPath()%>/docs/DMT_Content_Provision_Guide.pdf" download="DMT_Content_Provision_Guide.pdf" title="Download the whole guide as PDF"><i class="sg-pdf-icon"></i>Download PDF</a>
		<h3>Content Provision Guide</h3>
		<p>Rules and naming conventions to follow when placing master data, conversion content and facelift content for DMT.</p>
	</div>
	<div class="sg-body">
		<div class="sg-nav">
			<a data-sg="overview" class="sg-active"><span class="sg-num">1</span>Overview</a>
			<a data-sg="sequence"><span class="sg-num">2</span>Processing Sequence</a>
			<a data-sg="folders"><span class="sg-num">3</span>Folder Structure</a>
			<a data-sg="mdmfiles"><span class="sg-num">4</span>Master Data Files</a>
			<a data-sg="contentfiles"><span class="sg-num">5</span>Content Text Files</a>
			<a data-sg="mdauto"><span class="sg-num">6</span>Master Data Automation</a>
			<a data-sg="facelift"><span class="sg-num">7</span>Facelift Automation</a>
			<a data-sg="mdsync"><span class="sg-num">8</span>Master Data Syncing</a>
			<a data-sg="rules"><span class="sg-num">9</span>Scheduling Rules</a>
			<a data-sg="names"><span class="sg-num">10</span>Jobs, Reports, Validation</a>
		</div>
		<div class="sg-panel-wrap">

			<!-- 1. OVERVIEW -->
			<div class="sg-panel sg-show" id="sg-overview">
				<h4>Overview</h4>
				<p class="sg-lead">DMT loads master data and manual content from a network location into Kapture for the MC, MME and MNAO markets.</p>
				<div class="sg-grid">
					<div><div class="sg-card"><h6>Settings</h6><p>Your source network path. It is saved per user and cannot be changed while any job is Pending or Processing.</p></div></div>
					<div><div class="sg-card"><h6>Schedule</h6><p><b>Folder Based Content Load</b> (conversion) and <b>IPM Facelift VIN Update</b> (facelift automation).</p></div></div>
					<div><div class="sg-card"><h6>History</h6><p>Job status, item details, report download, abort, and the Preview link of a conversion job.</p></div></div>
				</div>
				<div style="height:12px"></div>
				<div class="sg-grid">
					<div><div class="sg-card"><h6>Preview / Publish</h6><p>Conversion loads documents as <b>Draft</b>. Review them on the Preview page, then publish from there.</p></div></div>
					<div><div class="sg-card"><h6>Meta Data Validation</h6><p>Upload VIN, ESI category, display order and CD text files to check them against MDM and Kapture before a load.</p></div></div>
					<div><div class="sg-card"><h6>Master Data Syncing</h6><p>Sends MDM master data that is waiting for sync to the Kapture categories, per market and item type.</p></div></div>
				</div>
				<div class="sg-note"><b>Network path:</b> mandatory, at most 300 characters. It can point at the folder that holds <code>MDM</code>, <code>GMS3_Content</code> and <code>Facelift_Content</code>, or directly at the <code>GMS3_Content</code> folder (then <code>MDM</code> and <code>Facelift_Content</code> must sit beside it).</div>
			</div>

			<!-- 2. SEQUENCE -->
			<div class="sg-panel" id="sg-sequence">
				<h4>Processing Sequence</h4>
				<p class="sg-lead">Process in this order. Each step depends on the one before it.</p>
				<ol class="sg-steps">
					<li><b>Place master data (MDM folder)</b><span>MDM Excel exports for every locale you will process (see Master Data Files).</span></li>
					<li><b>Sync master data to Kapture</b><span>Run <b>Master Data Syncing</b> for the market, or choose the load type <b>Master Data with Content / Master Data with Facelift</b> so the job loads it first.</span></li>
					<li><b>Facelift - IPM Facelift VIN Update</b><span>If Excel files are in <code>Facelift_Content</code>: Validate Data, Identify, View, Schedule. Old VINs are replaced by new VINs on existing documents.</span></li>
					<li><b>Conversion - Folder Based Content Load</b><span>Market, Locale, Model, Manual Type, Go, select rows, choose the load type, Schedule. Documents are created / updated in Kapture as Draft.</span></li>
					<li><b>Review the reports and the Preview</b><span>Download the reports zip from History and check the Failure / Missing Categories reports.</span></li>
					<li><b>Publish</b><span>Publish from the job's Preview page. Follow the publish job on the History page.</span></li>
				</ol>
				<div class="sg-warn"><b>Facelift before conversion:</b> when Excel files are present in <code>Facelift_Content</code>, the conversion Go asks <span class="sg-msg">"The IPM Facelift VIN Update ... must be executed before the Conversion. Have you executed it?"</span> Answer Yes only after the facelift job has finished.</div>
				<div class="sg-note"><b>One job at a time:</b> a conversion, facelift job, publish job or Master Data Syncing job cannot be scheduled while another one is Pending or Processing.</div>
			</div>

			<!-- 3. FOLDERS -->
			<div class="sg-panel" id="sg-folders">
				<h4>Folder Structure</h4>
				<p class="sg-lead">Folder names are used as they are. Keep the exact names and nesting below.</p>
<div class="sg-tree"><span class="f">&lt;Network path&gt;</span>
 |-- <span class="f">MDM</span>                                                  <span class="c">master data Excel files (all locales, flat)</span>
 |-- <span class="f">Facelift_Content</span>                                     <span class="c">facelift VIN Excel files (flat, any locale mix)</span>
 `-- <span class="f">GMS3_Content</span>
      `-- <span class="f">&lt;MARKET&gt;</span>                                        <span class="c">MC | MME | MNAO</span>
           `-- <span class="f">&lt;locale&gt;</span>                                   <span class="c">ja-JP, en-UK, en-US, fr-CA ...</span>
                `-- <span class="f">&lt;model folder&gt;</span>                        <span class="c">NewM_familia_van_ncp | CX-5_KM</span>
                     `-- <span class="f">&lt;manual type&gt;</span>                    <span class="c">WM, SH, WD, e-WD, BSM ...</span>
                          `-- <span class="f">&lt;facelift folder&gt;</span>           <span class="c">FL0000, FL0001 ...</span>
                               |-- <span class="x">vin.txt</span>                <span class="c">VIN ranges (vin1.txt ... allowed)</span>
                               |-- <span class="x">d01.txt ...</span>            <span class="c">display order, named in vin.txt</span>
                               |-- <span class="x">CD00.txt ...</span>           <span class="c">CD processing (MC / MME only)</span>
                               |-- <span class="x">scmvin.txt</span>             <span class="c">MME, WM only</span>
                               `-- <span class="f">&lt;material folder&gt;</span>      <span class="c">E-W1J-7EC7_HTML, ..._PDF, ..._XML</span>
                                    |-- <span class="x">esicat.txt</span>        <span class="c">document list + ESI codes</span>
                                    `-- <span class="f">html | html5 | pdf | djvu | ent.gms3 | img.gms3 | figure(s)</span></div>

				<h5>Naming per level</h5>
				<table class="sg-table">
					<tr><th style="width:150px">Level</th><th>Convention</th><th style="width:230px">Example</th></tr>
					<tr><td>Market</td><td>Exactly <code>MC</code>, <code>MME</code> or <code>MNAO</code>. Other folders are listed but not processed.</td><td><code>MC</code></td></tr>
					<tr><td>Locale</td><td>Language-country with a hyphen. It must match the locale in the MDM Excel file names and be a locale configured in DMT.</td><td><code>ja-JP</code>, <code>en-UK</code>, <code>en-US</code></td></tr>
					<tr><td>Model (MC / MME)</td><td><code>NewM_&lt;model&gt;_&lt;code&gt;</code> or <code>OldM_&lt;model&gt;_&lt;code&gt;</code>. In MC, any other prefix is handled as OEM content.</td><td><code>NewM_BT-50_tf</code>, <code>Isuzu_titan_lh</code></td></tr>
					<tr><td>Model (MNAO)</td><td><code>&lt;model&gt;_&lt;code&gt;</code>, no NewM / OldM prefix. The model is the part before the last <code>_</code>.</td><td><code>CX-5_KM</code>, <code>BT-50_TF</code></td></tr>
					<tr><td>Manual type</td><td>Shown as the folder name. <code>WD</code> and <code>e-WD</code> switch on wiring diagram handling.</td><td><code>WM</code>, <code>e-WD</code></td></tr>
					<tr><td>Facelift folder</td><td>One folder per facelift. A new facelift folder creates new articles.</td><td><code>FL0012</code></td></tr>
					<tr><td>Material folder</td><td>The suffix after the last <code>_</code> gives the format (<code>_HTML</code>, <code>_XML</code>, <code>_PDF</code>).</td><td><code>E-WZE-8FC2_HTML</code></td></tr>
				</table>
				<div class="sg-note"><b>Facelift-only location:</b> <code>GMS3_Content</code> is not needed when only <code>MDM</code> and <code>Facelift_Content</code> are provided.</div>
			</div>

			<!-- 4. MDM FILES -->
			<div class="sg-panel" id="sg-mdmfiles">
				<h4>Master Data Files (MDM folder)</h4>
				<p class="sg-lead">MDM export files, one per locale and master data type, placed directly in the <code>MDM</code> folder.</p>
				<div class="sg-grid">
					<div style="width:55%"><div class="sg-card"><h6>File name</h6><p><code>&lt;CC&gt;_&lt;LOCALE&gt;_&lt;TYPE&gt;.xlsx</code><br/>CC = 2-letter country code, LOCALE = <code>xx-XX</code>, TYPE = one of the types below. <code>.xlsx</code> or <code>.xls</code> only.</p></div></div>
					<div><div class="sg-card"><h6>Examples</h6><p><code>JP_JA-JP_CARLINE.xlsx</code><br/><code>UK_EN-UK_ALL_VIN.xlsx</code><br/><code>US_EN-US_DRIVELINE_AXLE_TYPE.xlsx</code></p></div></div>
				</div>
				<h5>Types (loaded in this order)</h5>
				<table class="sg-table">
					<tr><th style="width:30px">#</th><th style="width:190px">TYPE</th><th>Data columns</th></tr>
					<tr><td>1</td><td><code>CARLINE</code></td><td>CARLINE CODE, CAR NAME ENG LANG, CAR NAME REGIONAL LANG, WMI CODE, MODEL TYPE, ESI CATEGORY FLAG, IC DISPLAY CARLINE CODE. <b>MNAO:</b> columns 5-6 are YEAR START, YEAR END (4 digits, end not before start).</td></tr>
					<tr><td>2</td><td><code>CAT</code></td><td>LEVEL 1 CODE, LEVEL 1, LEVEL 2 CODE, LEVEL 2, LEVEL 3 CODE, LEVEL 3</td></tr>
					<tr><td>3</td><td><code>ENGINE_BOOK</code></td><td>3 columns</td></tr>
					<tr><td>4</td><td><code>ENGINE_TYPE</code></td><td>ENGINE BOOK CODE, ENGINE TYPE CODE, NAME, GROUP TYPE (the engine book must exist)</td></tr>
					<tr><td>5</td><td><code>TRANSMISSION_BOOK</code></td><td>3 columns</td></tr>
					<tr><td>6</td><td><code>TRANSMISSION_TYPE</code></td><td>3 columns</td></tr>
					<tr><td>7</td><td><code>DRIVELINE_AXLE_TYPE</code></td><td>3 columns (code, regional name, English name)</td></tr>
					<tr><td>8</td><td><code>BODY_TYPE</code></td><td>3 columns (code, regional name, English name)</td></tr>
					<tr><td>9</td><td><code>ALL_VIN</code></td><td>MODEL, CARLINE CODE, GROUP, WMI, VDS, VIS START, VIS END, ENGINE CODE, TRANSMISSION_CODE (the carline must exist)</td></tr>
				</table>
				<h5>Sheet layout</h5>
				<ul>
					<li>Sheet name <code>EXPORTED DATA</code>, header in row 1, data from row 2.</li>
					<li>The column right after the data columns must be headed <code>Indicator</code>.</li>
					<li>Indicator: <span class="sg-pill">A / ADD</span><span class="sg-pill">U / UPDATE</span><span class="sg-pill">blank</span> = add or update; <span class="sg-pill r">D / DELETE</span> = mark the row deleted. Any other value fails the row.</li>
					<li>Mandatory fields that are empty fail the row (<span class="sg-msg">"X is empty."</span>).</li>
				</ul>
				<div class="sg-note"><b>en-CA (MNAO):</b> content never comes for en-CA, but an en-US load also loads <code>CA_EN-CA_*.xlsx</code> when present and syncs its waiting categories. A missing en-CA file is not an error.</div>
				<div class="sg-warn"><b>Avoid:</b> two files with the same locale and type (only one is used), other file types in the folder, and a country / language that is not set up in MDM (the load stops for that locale).</div>
			</div>

			<!-- 5. CONTENT FILES -->
			<div class="sg-panel" id="sg-contentfiles">
				<h4>Content Text Files</h4>
				<p class="sg-lead">Fields are separated by <code>||</code>, one entry per line, each line ending with <code>;</code>.</p>

				<h5>VIN file - facelift folder (<code>vin.txt</code>, <code>vin1.txt</code> ...)</h5>
				<table class="sg-table">
					<tr><th style="width:80px">Market</th><th style="width:60px">Fields</th><th>Layout</th></tr>
					<tr><td>MC</td><td>8</td><td><code>MANUAL TYPE||FL FOLDER||MATERIAL||CARLINE||VDS||VIS START||VIS END||DISPLAY ORDER FILE</code></td></tr>
					<tr><td>MME</td><td>9</td><td><code>MANUAL TYPE||FL FOLDER||MATERIAL||CARLINE||WMI||VDS||VIS START||VIS END||DISPLAY ORDER FILE</code></td></tr>
					<tr><td>MNAO</td><td>11</td><td><code>MANUAL TYPE||FL FOLDER||MATERIAL||CARLINE||WMI||VDS||VIS START||VIS END||MODEL||YEAR||DISPLAY ORDER FILE</code><br/>All 11 fields are required, e.g. <code>WM||FL0012||E-WZE-8FC2_MY2026_HTML||TF||AMP2||TFR**JT*||600001||649999||CX90||2026MY||d01.txt;</code></td></tr>
				</table>

				<ul>
					<li><b>SCM VIN file</b> (MME, manual type WM only): <code>scmvin.txt</code>, 9 fields ending with the source ENT file name.</li>
					<li><b>Overhaul books</b> (Engine / AT / MT): VIN data comes from MDM, no VIN file is needed.</li>
				</ul>

				<h5>Display order file - facelift folder (<code>d01.txt</code>, <code>d02.txt</code> ...)</h5>
				<p>The name starts with <code>d</code>, ends with <code>.txt</code> and must be exactly the name given in the last field of the VIN file. A <code>delete.txt</code> file is not used any more and is ignored.</p>
				<p><code>MODEL FOLDER||MANUAL TYPE||FL FOLDER||MATERIAL||PROCESSING FOLDER||FILE NAME||TITLE||ENGINE TYPE||TRANSMISSION TYPE||BODY TYPE||DRIVE AXLE TYPE||LEVEL 1 .. LEVEL 6||SEQUENCE</code></p>
				<ul>
					<li>Levels are <b>names</b> (1 to 6 levels); the sequence is always the last field.</li>
					<li>Engine / AT / MT overhaul books leave out TITLE and the four type fields.</li>
				</ul>

				<h5>CD file - facelift folder (<code>CD00.txt</code> ..., MC / MME only)</h5>
				<p>Name starts with <code>CD</code>; same layout as the display order file. MNAO has no CD file.</p>
				<div class="sg-warn"><b>Display order and CD files are complete lists:</b> for every material folder named in vin.txt, a document that is not listed - or whose line is invalid (an empty folder / file name field) or carries a wrong path - <b>loses its view content</b> (display order) and <b>its CD data</b> (CD file, MC / MME) on the next load. A material folder without any line loses all of it. The document itself stays in Kapture and, when converted, still shows in that job's Preview without a place in the tree. Removed rows are reported as <code>DELETE</code> in DISPLAY_ORDER.xlsx / CD_PROCESSING_DATA.xlsx. See <b>Examples</b> at the end of this tab.</div>

				<h5>ESI category file - material folder (<code>esicat.txt</code>)</h5>
				<table class="sg-table">
					<tr><th style="width:160px">Case</th><th>Layout</th></tr>
					<tr><td>MC / MME / MNAO</td><td><code>FL FOLDER||MATERIAL||PROCESSING FOLDER||FILE NAME||TITLE||ESI CODE 1||ESI CODE 2||ESI CODE 3</code><br/>e.g. <code>FL0012||E-WZE-8FC2_MY2026_HTML||html||si_1049129.html||removal||Y1||Y10001||01;</code></td></tr>
					<tr><td>MME wiring diagrams</td><td>9 fields: <code>STEERING TYPE(S)</code> after TITLE (e.g. <code>LEFT_HAND_DRIVE,RIGHT_HAND_DRIVE</code>).</td></tr>
				</table>
				<div class="sg-warn"><b>esicat.txt is the document list:</b> a document already loaded for the material folder but no longer listed in esicat.txt is <b>deleted (unpublished)</b> by the next load. To remove a document cleanly, remove it from esicat.txt, the display order / CD files and the folder.</div>
				<div class="sg-ex-block">
				<h5>Examples - changing the display order (MC familia van, folder <code>E-W1J-7EC7_PDF</code>)</h5>
				<div class="sg-card sg-ex">
					<h6>Example 1 - a document is dropped from the display order</h6>
					<p><b>Change:</b> the line of <code>J_DTSNAMELIST_7.pdf</code> is removed from <code>d01.txt</code>. The PDF stays in the folder and in <code>esicat.txt</code>.</p>
					<div class="sg-snap">
						<div class="sg-snap-bar">d01.txt (facelift folder FL0001)</div>
<pre>NewM_familia_van_ncp||WM||FL0001||E-W1J-7EC7_PDF||pdf||J_DTSNAMELIST_6.pdf||...||0010;
<span class="sg-del">NewM_familia_van_ncp||WM||FL0001||E-W1J-7EC7_PDF||pdf||J_DTSNAMELIST_7.pdf||...||0011;</span>   <span class="sg-tag">line removed</span>
NewM_familia_van_ncp||WM||FL0001||E-W1J-7EC7_PDF||pdf||J_DTSNAMELIST_8.pdf||...||0012;</pre>
					</div>
					<div class="sg-snap">
						<div class="sg-snap-bar">DISPLAY_ORDER.xlsx (reports zip of the job)</div>
						<table class="sg-snap-table">
							<tr><th>SOURCE FILE</th><th>DOCUMENT ID</th><th>OPERATION</th><th>STATUS</th><th>ERROR MESSAGE</th></tr>
							<tr><td>...\E-W1J-7EC7_PDF\pdf\J_DTSNAMELIST_7.pdf</td><td>SM231910</td><td><b>DELETE</b></td><td>SUCCESS</td><td>VIEW CONTENT DELETED - DOCUMENT IS NOT IN THE DISPLAY ORDER FILE.</td></tr>
						</table>
					</div>
					<p class="sg-ex-result"><b>Result:</b> SM231910 is converted and stays in Kapture, but its <b>view content and CD data are removed</b> (it no longer appears under any display order position). In the job's Preview it is shown <b>without a place in the tree</b>.</p>
				</div>
				<div class="sg-card sg-ex">
					<h6>Example 2 - a document is listed, but its file is not delivered</h6>
					<p><b>Change:</b> <code>J_DTSNAMELIST_2.pdf</code> was loaded and <b>published</b> earlier. This time <code>d01.txt</code> still lists it, but the PDF is not in the folder, so the job does not convert it.</p>
					<div class="sg-snap sg-snap-2">
						<div><div class="sg-snap-bar">d01.txt</div>
<pre>NewM_familia_van_ncp||WM||FL0001||E-W1J-7EC7_PDF||pdf||J_DTSNAMELIST_1.pdf||...||0003;
<span class="sg-keep">NewM_familia_van_ncp||WM||FL0001||E-W1J-7EC7_PDF||pdf||J_DTSNAMELIST_2.pdf||...||0004;</span>   <span class="sg-tag sg-tag-k">still listed</span></pre></div>
						<div><div class="sg-snap-bar">E-W1J-7EC7_PDF\pdf (folder)</div>
<pre>J_DTSNAMELIST_1.pdf
<span class="sg-del">J_DTSNAMELIST_2.pdf</span>   <span class="sg-tag">not delivered</span></pre></div>
					</div>
					<p class="sg-ex-result"><b>Result:</b> its <b>view content is still written</b> from the display order line and <b>keeps its status</b> - Published stays Published (a draft stays Draft). It is <b>not in this job's Preview</b>, because the job did not convert it.</p>
				</div>
				</div>
			</div>

			<!-- 6. MASTER DATA AUTOMATION -->
			<div class="sg-panel" id="sg-mdauto">
				<h4>Master Data Automation</h4>
				<p class="sg-lead">The load type decides whether the job first loads master data from the MDM folder and syncs it to Kapture.</p>
				<div class="sg-grid">
					<div style="width:50%"><div class="sg-card">
						<h6>Conversion - "Master Data with Content"</h6>
						<ol class="sg-steps" style="margin-top:10px !important">
							<li><b>Load MDM Excel</b><span>For each locale of the selected rows (en-US also brings en-CA), its MDM files are loaded into MDM.</span></li>
							<li><b>Sync categories to Kapture</b><span>Every waiting row of the locale is written to the Kapture categories.</span></li>
							<li><b>Convert documents</b><span>Content is processed with the categories in place.</span></li>
							<li><b>Reports</b><span>MASTER_DATA_LOAD_REPORT + MASTER_DATA_CATEGORY_REPORT in the zip.</span></li>
						</ol>
					</div></div>
					<div><div class="sg-card">
						<h6>Facelift - "Master Data with Facelift"</h6>
						<ol class="sg-steps sg-grey" style="margin-top:10px !important">
							<li><b>Load MDM Excel</b><span>Per market, for the locales of the facelift job (new VIN ranges come from ALL_VIN).</span></li>
							<li><b>Sync categories to Kapture</b><span>New VIN categories are created before any document.</span></li>
							<li><b>Swap VINs on documents</b><span>Old VIN categories are replaced by the new ones.</span></li>
							<li><b>Reports</b><span>Master data reports added to the job zip.</span></li>
						</ol>
					</div></div>
				</div>
				<h5>Rules</h5>
				<ul>
					<li>The same Excel rules apply as in <b>Master Data Files</b>.</li>
					<li>Label rule: when several rows share a reference key, the <b>last row with a name</b> gives the label; a blank name keeps the existing label.</li>
					<li>A master data failure never stops the job: content is still processed, and the job ends as FAILURE so the report is reviewed.</li>
					<li>Reports list only added, updated, deleted and failed rows; no report is written when there is nothing to report.</li>
				</ul>
				<div class="sg-note"><b>"Only Content" / "Only Facelift":</b> no master data is loaded. Only Content is refused while categories of the selected locales are still waiting for sync. Only Facelift is refused when a new VIN category is not yet in Kapture.</div>
			</div>

			<!-- 7. FACELIFT -->
			<div class="sg-panel" id="sg-facelift">
				<h4>Facelift Automation - IPM Facelift VIN Update</h4>
				<p class="sg-lead">Replaces old VIN ranges by new VIN ranges on documents that are already in Kapture. No upload: DMT reads the <code>Facelift_Content</code> folder.</p>
				<h5>Excel file</h5>
				<ul>
					<li>Any name, <code>.xlsx</code> / <code>.xls</code> / <code>.xlsm</code>; first sheet; header in row 1. Several files and mixed locales are allowed. Open-file locks (<code>~$...</code>) are skipped.</li>
					<li>All 11 columns are required on every row:</li>
				</ul>
				<table class="sg-table">
					<tr><th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th><th>7</th><th>8</th><th>9</th><th>10</th><th>11</th></tr>
					<tr><td>LOCALE</td><td>OLD CARLINE CODE</td><td>OLD WMI</td><td>OLD VDS</td><td>OLD VIS START</td><td>OLD VIS END</td><td>NEW CARLINE CODE</td><td>NEW WMI</td><td>NEW VDS</td><td>NEW VIS START</td><td>NEW VIS END</td></tr>
				</table>
				<h5>Flow</h5>
				<ol class="sg-steps">
					<li><b>Validate Data</b><span>Locale in MDM and Kapture; old VIN in MDM and Kapture; new VIN in MDM and Kapture, or in the locale's ALL_VIN MDM file. More than 10 errors are given as a downloadable error file.</span></li>
					<li><b>Identify</b><span>Creates the identification job that finds the impacted SM / OSM / WD documents.</span></li>
					<li><b>View and select items</b><span>Choose "Master Data with Facelift" or "Only Facelift", then Schedule.</span></li>
					<li><b>Facelift job</b><span>VIN categories are swapped and bulk updated; documents that were published are published again.</span></li>
					<li><b>Old VIN deactivation</b><span>Only when the whole job succeeds, the old VIN ranges are made inactive in MDM (OLD_VIN_DEACTIVATION_REPORT). A range that is also a new range of the job is kept.</span></li>
				</ol>
				<div class="sg-warn"><b>After the facelift:</b> remove or archive the processed Excel files from <code>Facelift_Content</code>. While they are there, every conversion Go asks whether the facelift has been executed.</div>
			</div>

			<!-- 8. MASTER DATA SYNCING -->
			<div class="sg-panel" id="sg-mdsync">
				<h4>Master Data Syncing</h4>
				<p class="sg-lead">Sends MDM rows that are waiting for sync to the Kapture categories, for every locale mapped to the market.</p>
				<h5>Items per market</h5>
				<table class="sg-table">
					<tr><th style="width:110px">Market</th><th>Items offered</th></tr>
					<tr><td>MNAO</td><td><span class="sg-pill">Model and Year</span><span class="sg-pill">VIN Range</span><span class="sg-pill">ESI Category</span><span class="sg-pill">Engine Type</span><span class="sg-pill">Transmission Type</span><span class="sg-pill">Driveline/Axle Type</span><span class="sg-pill">Body Type</span></td></tr>
					<tr><td>MC / MME</td><td><span class="sg-pill">Carline + VIN</span><span class="sg-pill">ESI Category</span><span class="sg-pill">Engine Type</span><span class="sg-pill">Transmission Type</span><span class="sg-pill">Driveline/Axle Type</span><span class="sg-pill">Body Type</span></td></tr>
				</table>
				<p>Only items with waiting rows are listed. Select the market and at least one item (or All).</p>
				<h5>Rules</h5>
				<ul>
					<li>Only active MDM rows waiting for sync are taken. A locale without its own rows uses the rows of its alternate locale (for example en-UK for MME, en-US for en-CA).</li>
					<li>Every level of a row is attempted. A missing category is created under its parent; a <b>missing parent fails that level</b>. The top category is never created here.</li>
					<li>A row is marked synced only when all its levels succeeded in every locale it was processed for. Failed rows stay waiting and are retried by the next run.</li>
					<li>Label rule: last row with a name wins; a blank name keeps the existing label. Each category is also completed in all Kapture default locales.</li>
					<li>Reports: CATEGORY_TRANSACTIONS and CATEGORY_FAILURES in the job zip.</li>
				</ul>
				<div class="sg-warn"><b>Gate on the Schedule screen:</b> while categories of the selected locales are waiting for sync, an "Only Content" conversion is refused with <span class="sg-msg">"&lt;n&gt; Categories are still remaining for Syncing in Kapture for &lt;market&gt; Market ..."</span>. Run Master Data Syncing first or choose "Master Data with Content".</div>
			</div>

			<!-- 9. SCHEDULING RULES -->
			<div class="sg-panel" id="sg-rules">
				<h4>Scheduling Rules</h4>
				<p class="sg-lead">Checks made on the Schedule screen before a conversion can be scheduled.</p>
				<table class="sg-table">
					<tr><th style="width:200px">Check</th><th>Rule</th></tr>
					<tr><td>Selection</td><td>Market is mandatory for Go; at least one row must be selected to Schedule.</td></tr>
					<tr><td>One job at a time</td><td>Refused while another Conversion / IPM Facelift VIN Update / Master Data Syncing / Publish job is Pending or Processing.</td></tr>
					<tr><td>Item already processing</td><td>A row (locale, model, manual type, facelift, material) already in a running job cannot be scheduled again.</td></tr>
					<tr><td>Item being published</td><td>A row whose locale + model folder + manual type is in a running publish job is refused until that job finishes.</td></tr>
					<tr><td>VIN + display order</td><td>A row with documents needs a VIN file and its display order file. Refused when: no VIN or display order file, only one of the two, VIN lines without proper VINs, or the display order file named in vin.txt is missing.</td></tr>
					<tr><td>Material folders</td><td>Refused when none of the material folders named in the VIN file exists under the facelift folder.</td></tr>
					<tr><td>Master data waiting</td><td>"Only Content" refused while categories of the selected locales wait for sync (see Master Data Syncing).</td></tr>
					<tr><td>Facelift pending</td><td>Facelift Excel present: Go asks for confirmation that the facelift has been executed.</td></tr>
					<tr><td>Publish</td><td>Refused while the conversion job is still running, when nothing is left to publish, or when its items are being processed by other jobs.</td></tr>
				</table>
				<div class="sg-note"><b>Network path changes</b> are refused while any job is Pending or Processing.</div>
			</div>

			<!-- 10. NAMES / REPORTS / VALIDATION -->
			<div class="sg-panel" id="sg-names">
				<h4>Jobs, Reports and Meta Data Validation</h4>
				<p class="sg-lead">How DMT names its jobs and reports, and the file naming expected by Meta Data Validation.</p>
				<h5>Job names</h5>
				<table class="sg-table">
					<tr><th style="width:200px">Job</th><th>Name</th></tr>
					<tr><td>Conversion</td><td><code>MGSS_CONV_&lt;MC|MME|MNAO&gt;_yyyyMMdd_&lt;id&gt;</code></td></tr>
					<tr><td>Publish</td><td><code>MGSS_PUB_&lt;market&gt;_yyyyMMdd_&lt;conversion job id&gt;</code></td></tr>
					<tr><td>Facelift identification</td><td><code>MGSS_DI_SYNCyyyyMMdd_&lt;id&gt;</code></td></tr>
					<tr><td>Facelift job</td><td><code>MGSS_MDSYNC_yyyyMMdd_&lt;id&gt;</code></td></tr>
					<tr><td>Master Data Syncing</td><td><code>MGSS_MDSYNC_&lt;MARKET&gt;_&lt;id&gt;</code></td></tr>
				</table>
				<h5>Reports (in the job zip)</h5>
				<table class="sg-table">
					<tr><th style="width:200px">Job</th><th>Reports</th></tr>
					<tr><td>Conversion</td><td>TRANSACTION_REPORT, FAILURE_REPORT, MISSING_CATEGORIES, DISPLAY_ORDER, CD_PROCESSING_DATA (MC / MME), VIEW_CONTENT_REPORT (MNAO: VIEW_CONTENT_MODEL_YEAR_REPORT + VIEW_CONTENT_VIN_REPORT), INNER_LINKS_REPORT, OTHER_MANUAL_LINKS_REPORT, VOLTAGE_*_JS_REPORT (WD)</td></tr>
					<tr><td>Master data load</td><td>MASTER_DATA_LOAD_REPORT, MASTER_DATA_CATEGORY_REPORT</td></tr>
					<tr><td>Facelift</td><td>OLD_VIN_DEACTIVATION_REPORT, VIN_MANUAL_TYPE_MAPPING_REPORT (MME)</td></tr>
					<tr><td>Master Data Syncing</td><td>CATEGORY_TRANSACTIONS, CATEGORY_FAILURES</td></tr>
				</table>
				<div class="sg-note"><b>DISPLAY_ORDER / CD_PROCESSING_DATA:</b> the <code>OPERATION</code> column shows <code>INSERT</code> for listed documents and <code>DELETE</code> for documents whose view content / CD data was removed because they are no longer listed.</div>
				<h5>Meta Data Validation uploads</h5>
				<table class="sg-table">
					<tr><th style="width:200px">Market</th><th>File types</th></tr>
					<tr><td>MC</td><td>VIN, ESI Category New / Old, Display Order, CD Processing</td></tr>
					<tr><td>MME</td><td>VIN, ESI Category Manuals New / Old, ESI Category WD New / Old, Display Order, CD Processing</td></tr>
					<tr><td>MNAO</td><td>VIN (11 fields), ESI Category, Display Order</td></tr>
				</table>
				<p><code>.txt</code> files only; the name must start with <code>vin</code> (VIN), <code>esi</code> (ESI Category), <code>d</code> (Display Order) or <code>cd</code> (CD Processing). Formats are the same as in <b>Content Text Files</b>.</p>
			</div>

		</div>
	</div>
</div>
<script type="text/javascript">
(function(){
	var guide = document.getElementById("sgGuide");
	if(!guide) return;
	var links = guide.querySelectorAll(".sg-nav a");
	function show(key){
		for(var i=0;i<links.length;i++){
			var on = links[i].getAttribute("data-sg")===key;
			links[i].className = on ? "sg-active" : "";
			document.getElementById("sg-"+links[i].getAttribute("data-sg")).className = on ? "sg-panel sg-show" : "sg-panel";
		}
		try { sessionStorage.setItem("dmtSettingsGuideTab", key); } catch(e) {}
	}
	for(var i=0;i<links.length;i++){
		links[i].onclick = function(){ show(this.getAttribute("data-sg")); };
	}
	try { var last = sessionStorage.getItem("dmtSettingsGuideTab"); if(last && document.getElementById("sg-"+last)) show(last); } catch(e) {}
})();
</script>
