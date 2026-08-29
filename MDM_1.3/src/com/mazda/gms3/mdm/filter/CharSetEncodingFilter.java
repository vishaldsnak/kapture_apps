package com.mazda.gms3.mdm.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet Filter implementation class CharSetEncodingFilter
 */
public class CharSetEncodingFilter implements Filter {

	/**
	 * Extensions served straight from the web application as STATIC files. This filter is
	 * mapped on /*, so it also sees these requests - and it must NOT stamp "text/html" on
	 * them, otherwise the container's own MIME type (text/css, application/javascript,
	 * image/png ...) is replaced and the browser refuses the resource. A stylesheet
	 * delivered as text/html is ignored by every browser in standards mode, which shows up
	 * as "the CSS is not loading" even though the file is returned with HTTP 200.
	 */
	private static final String[] STATIC_EXTENSIONS = new String[] { ".css", ".js", ".map",
			".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg", ".webp", ".woff",
			".woff2", ".ttf", ".otf", ".eot", ".pdf", ".zip", ".csv", ".txt", ".json",
			".xml", ".html", ".htm",
			// office documents - excel, word and powerpoint, legacy and OOXML
			".xls", ".xlsx", ".xlsm", ".xlsb", ".doc", ".docx", ".docm", ".ppt", ".pptx",
			".pptm", ".rtf" };

    /**
     * Default constructor.
     */
    public CharSetEncodingFilter() {
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		/*
		 * Only DYNAMIC responses (JSP / servlet output) are declared as HTML. Static
		 * resources keep the content type the container derives from their extension.
		 */
		if (isStaticResourceRequest(request) == false) {
			response.setContentType("text/html; charset=UTF-8");
		}
		chain.doFilter(request, response);
	}

	/**
	 * @return true when the requested URI ends with one of the static file extensions.
	 */
	private boolean isStaticResourceRequest(ServletRequest request) {
		if (!(request instanceof HttpServletRequest)) {
			return false;
		}
		String uri = ((HttpServletRequest) request).getRequestURI();
		if (null == uri || "".equals(uri)) {
			return false;
		}
		uri = uri.toLowerCase();
		for (int i = 0; i < STATIC_EXTENSIONS.length; i++) {
			if (uri.endsWith(STATIC_EXTENSIONS[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

}
