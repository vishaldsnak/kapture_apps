# Builds WebContent/docs/DMT_Content_Provision_Guide.pdf from the Settings page guide
# (WebContent/jsps/settingsGuide.jsp + WebContent/css/settings_guide.css), so the PDF and the page
# never drift apart. Re-run after every change to the guide, then rebuild dmtgt.war.
#
#   python docs/build_settings_guide_pdf.py
#
# Needs Microsoft Edge (or Chrome) for the rendering and PyMuPDF (pip install pymupdf) for the
# page numbers, the contents page numbers, the contents links and the PDF bookmarks.
#
# Pass 1 renders the guide to find the page each section starts on; pass 2 renders it again with
# those numbers in the contents; then the footer, links and bookmarks are added.
import datetime
import io
import os
import re
import subprocess
import sys
import tempfile
import time

try:
    import pymupdf
except ImportError:
    sys.exit("PyMuPDF is required: pip install pymupdf")

HERE = os.path.dirname(os.path.abspath(__file__))
APP = os.path.dirname(HERE)
JSP = os.path.join(APP, "WebContent", "jsps", "settingsGuide.jsp")
CSS = os.path.join(APP, "WebContent", "css", "settings_guide.css")
OUT_DIR = os.path.join(APP, "WebContent", "docs")
OUT = os.path.join(OUT_DIR, "DMT_Content_Provision_Guide.pdf")
TITLE = "DMT Content Provision Guide"
BROWSERS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
]

PRINT_CSS = """
@page { size: A4; margin: 14mm 12mm 16mm 12mm; }
body { margin: 0; font-family: Arial, sans-serif; color: #4A4A4A; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
.sg-wrap { border: none; margin: 0; }
.sg-head, .sg-nav, .sg-pdf { display: none !important; }
.sg-body, .sg-panel-wrap { display: block; padding: 0; }
.sg-panel { display: block !important; page-break-before: always; padding-top: 2mm; }
.sg-panel h4 { font-size: 18px; border-bottom: 2px solid #D0021B; padding-bottom: 6px; margin-bottom: 6px; }
.sg-table, .sg-card, .sg-note, .sg-warn, .sg-steps li, .sg-tree { page-break-inside: avoid; }
.sg-tree { font-size: 10px; white-space: pre; overflow: visible; }
.sg-panel h5 { margin: 13px 0 5px; }
.sg-panel code { font-size: 10.5px; padding: 0 4px; }
.sg-panel ul { margin: 4px 0 4px 16px; }
.sg-panel .sg-lead { margin-bottom: 12px; }
.sg-ex-block { page-break-before: always; border-top: none; margin-top: 0; }
.sg-ex { page-break-inside: avoid; }
.sg-panel p, .sg-panel li { font-size: 12px; line-height: 1.5; }
.sg-table { font-size: 11.5px; }
.sg-table td, .sg-table th { padding: 5px 8px; }
.sg-note, .sg-warn { margin: 10px 0 4px; padding: 8px 12px; font-size: 12px; }
.cover { padding-top: 6mm; }
.cover .bar { width: 60px; height: 6px; background: #D0021B; margin-bottom: 16px; }
.cover h1 { font-size: 30px; color: #2B2B2B; margin: 0 0 6px; }
.cover h2 { font-size: 15px; color: #777; font-weight: normal; margin: 0 0 8px; }
.cover .meta { font-size: 11.5px; color: #999; margin-bottom: 34px; }
.toc h3 { font-size: 16px; color: #2B2B2B; margin: 0 0 10px; padding-bottom: 6px; border-bottom: 2px solid #D0021B; }
.toc .row { display: flex; align-items: baseline; font-size: 13.5px; color: #2B2B2B; padding: 7px 0; }
.toc .num { width: 30px; color: #D0021B; font-weight: bold; }
.toc .dots { flex: 1; border-bottom: 1px dotted #AAA; margin: 0 8px; position: relative; top: -3px; }
.toc .pg { min-width: 22px; text-align: right; font-weight: bold; }
.cover .intro { margin-top: 30px; font-size: 12px; line-height: 1.6; color: #666; background: #FAFAFA; border: 1px solid #E6E6E6; border-radius: 4px; padding: 12px 14px; }
"""


def find_browser():
    browser = next((b for b in BROWSERS if os.path.exists(b)), None)
    if browser is None:
        sys.exit("Edge / Chrome not found - looked in: " + ", ".join(BROWSERS))
    return browser


def render(browser, html, pdf, tmp_dir):
    src = os.path.join(tmp_dir, "guide.html")
    io.open(src, "w", encoding="utf-8").write(html)
    if os.path.exists(pdf):
        os.remove(pdf)
    subprocess.call([browser, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                     "--user-data-dir=" + os.path.join(tmp_dir, "profile"),
                     "--print-to-pdf=" + pdf, "file:///" + src.replace("\\", "/")])
    # msedge.exe can hand the job to a child process and return first - wait for the file to settle
    last = -1
    for _ in range(120):
        size = os.path.getsize(pdf) if os.path.exists(pdf) else -1
        if size > 0 and size == last:
            break
        last = size
        time.sleep(0.5)
    if not os.path.exists(pdf) or os.path.getsize(pdf) == 0:
        sys.exit("PDF was not written: " + pdf)


def build_html(css, body, sections, pages):
    rows = "".join(
        '<div class="row"><span class="num">%d</span><span>%s</span><span class="dots"></span>'
        '<span class="pg">%s</span></div>' % (i + 1, title, pages[i] if pages else "")
        for i, (title, _) in enumerate(sections))
    cover = ('<div class="cover"><div class="bar"></div><h1>' + TITLE + '</h1>'
             '<h2>Rules and naming conventions for master data, conversion content and facelift content</h2>'
             '<div class="meta">Generated %s from the DMT Settings page</div>'
             '<div class="toc"><h3>Contents</h3>%s</div>'
             '<div class="intro">This guide is also available on the DMT <b>Settings</b> page. Follow the folder and file '
             'naming conventions exactly - DMT discovers markets, locales, models, manual types and files by their names.'
             '</div></div>') % (datetime.date.today().strftime("%d %b %Y"), rows)
    return ('<!DOCTYPE html><html><head><meta charset="utf-8"><title>' + TITLE + '</title>'
            '<style>' + css + PRINT_CSS + '</style></head><body>' + cover + body + '</body></html>')


def section_pages(pdf, sections):
    """First page (1-based) whose top area carries the section heading."""
    doc = pymupdf.open(pdf)
    pages, start = [], 1                       # page index 0 = cover / contents
    for _, heading in sections:
        needle = heading[:40]
        found = None
        for i in range(start, doc.page_count):
            hits = [r for r in doc[i].search_for(needle) if r.y0 < doc[i].rect.height * 0.25]
            if hits:
                found = i
                break
        if found is None:
            sys.exit("Section heading not found in the PDF: " + heading)
        pages.append(found + 1)
        start = found
    doc.close()
    return pages


def finish(pdf, out, sections, pages):
    doc = pymupdf.open(pdf)
    total = doc.page_count
    grey = (0.45, 0.45, 0.45)
    for i in range(1, total):                  # no footer on the cover
        page = doc[i]
        w, h = page.rect.width, page.rect.height
        y = h - 9 * 72 / 25.4                  # 9 mm from the bottom edge
        page.draw_line((34, y - 10), (w - 34, y - 10), color=(0.85, 0.85, 0.85), width=0.6)
        page.insert_text((34, y), TITLE, fontsize=8, fontname="helv", color=grey)
        label = "Page %d of %d" % (i + 1, total)
        page.insert_text((w - 34 - pymupdf.get_text_length(label, "helv", 8), y), label,
                         fontsize=8, fontname="helv", color=grey)
    # contents rows -> links to their page
    cover = doc[0]
    for (title, _), pg in zip(sections, pages):
        for r in cover.search_for(title):
            cover.insert_link({"kind": pymupdf.LINK_GOTO, "from": pymupdf.Rect(30, r.y0 - 3, cover.rect.width - 30, r.y1 + 3),
                               "page": pg - 1})
            break
    doc.set_toc([[1, "Contents", 1]] + [[1, "%d. %s" % (i + 1, t), p] for i, ((t, _), p) in enumerate(zip(sections, pages))])
    doc.set_metadata({"title": TITLE, "subject": "DMT content provision rules", "creator": "DMT"})
    if os.path.exists(out):
        os.remove(out)
    doc.save(out, garbage=3, deflate=True)
    doc.close()
    return total


def main():
    jsp = io.open(JSP, encoding="utf-8").read()
    css = io.open(CSS, encoding="utf-8").read()
    body = re.sub(r"<%[@-].*?%>", "", jsp, flags=re.S)          # JSP directives / comments
    body = re.sub(r"<%=.*?%>", "", body, flags=re.S)             # JSP expressions (download link)
    body = re.sub(r"<script.*?</script>", "", body, flags=re.S)  # tab switching
    titles = re.findall(r'<a data-sg="[^"]+"[^>]*><span class="sg-num">\d+</span>([^<]+)</a>', body)
    headings = [re.sub(r"<[^>]+>", "", h).strip() for h in re.findall(r'<div class="sg-panel[^"]*"[^>]*>\s*<h4>(.*?)</h4>', body, flags=re.S)]
    if not titles or len(titles) != len(headings):
        sys.exit("Guide sections not found or tabs (%d) != panels (%d) in %s" % (len(titles), len(headings), JSP))
    sections = list(zip(titles, headings))

    browser = find_browser()
    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)
    tmp_dir = tempfile.mkdtemp(prefix="sgpdf_")
    draft = os.path.join(tmp_dir, "draft.pdf")
    render(browser, build_html(css, body, sections, None), draft, tmp_dir)       # pass 1: where sections land
    pages = section_pages(draft, sections)
    render(browser, build_html(css, body, sections, pages), draft, tmp_dir)      # pass 2: numbers in the contents
    if section_pages(draft, sections) != pages:
        sys.exit("Section pages moved between passes - check the contents page layout.")
    total = finish(draft, OUT, sections, pages)
    print("Written %s (%d bytes, %d pages, %d sections)" % (OUT, os.path.getsize(OUT), total, len(sections)))
    for (t, _), p in zip(sections, pages):
        print("  %-28s page %d" % (t, p))


if __name__ == "__main__":
    main()
