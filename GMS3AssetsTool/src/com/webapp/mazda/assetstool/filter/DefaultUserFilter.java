package com.webapp.mazda.assetstool.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.webapp.mazda.assetstool.logging.LogManager;
import com.webapp.mazda.assetstool.logging.Logger;
import com.webapp.mazda.assetstool.util.ApplicationPropertiesUtil;

/**
 * Supplies the WebSEAL "iv-user" header when the request carries none.
 *
 * Every screen of this application (ManageAssets, OKAssetsUpload.jsp) redirects to
 * /error unless the request carries the iv-user header that WebSEAL adds in front of
 * the production servers. On a plain Tomcat there is no WebSEAL, so the application
 * cannot be exercised at all.
 *
 * This is the same mechanism MDM uses (AccessManagementFilter.DefaultUserRequestWrapper):
 * when application.properties carries a non-blank "wsl.default.user" AND the incoming
 * request has no iv-user header, the request is wrapped so that getHeader("iv-user")
 * returns the configured id. A real incoming header ALWAYS wins - the wrapper is only
 * installed when the header is absent.
 *
 * LEAVE wsl.default.user BLANK ON VDI / MC DEV. Blank means the filter does nothing and
 * the real WebSEAL header is required, exactly as before the migration.
 *
 * Nothing else in the application had to change: servlets and JSPs keep reading the
 * header the way they always did, the wrapped request simply flows down the chain.
 */
public class DefaultUserFilter implements Filter {

	private static Logger logger = LogManager.getLogger(DefaultUserFilter.class);

	private static final String IV_USER = "iv-user";
	private static final String PROP_DEFAULT_USER = "wsl.default.user";

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String header = httpRequest.getHeader(IV_USER);
			if (null == header || "".equals(header.trim())) {
				String defaultUser = ApplicationPropertiesUtil
						.getProperty(PROP_DEFAULT_USER);
				if (null != defaultUser && !"".equals(defaultUser.trim())) {
					logger.info("doFilter :: no " + IV_USER
							+ " header on the request - injecting " + PROP_DEFAULT_USER
							+ " = " + defaultUser.trim());
					request = new DefaultUserRequestWrapper(httpRequest,
							defaultUser.trim());
				}
			}
		}
		chain.doFilter(request, response);
	}

	/**
	 * Answers the configured user for the iv-user header and delegates everything else
	 * to the real request.
	 */
	private static class DefaultUserRequestWrapper extends HttpServletRequestWrapper {

		private String defaultUser = null;

		public DefaultUserRequestWrapper(HttpServletRequest request, String defaultUser) {
			super(request);
			this.defaultUser = defaultUser;
		}

		public String getHeader(String name) {
			if (null != name && IV_USER.equalsIgnoreCase(name)) {
				String value = super.getHeader(name);
				if (null == value || "".equals(value)) {
					return defaultUser;
				}
				return value;
			}
			return super.getHeader(name);
		}

		public Enumeration<String> getHeaders(String name) {
			if (null != name && IV_USER.equalsIgnoreCase(name)) {
				Enumeration<String> values = super.getHeaders(name);
				if (null == values || !values.hasMoreElements()) {
					List<String> injected = new ArrayList<String>();
					injected.add(defaultUser);
					return Collections.enumeration(injected);
				}
				return values;
			}
			return super.getHeaders(name);
		}

		public Enumeration<String> getHeaderNames() {
			List<String> names = new ArrayList<String>();
			Enumeration<String> existing = super.getHeaderNames();
			boolean hasIvUser = false;
			if (null != existing) {
				while (existing.hasMoreElements()) {
					String n = existing.nextElement();
					names.add(n);
					if (IV_USER.equalsIgnoreCase(n)) {
						hasIvUser = true;
					}
				}
			}
			if (!hasIvUser) {
				names.add(IV_USER);
			}
			return Collections.enumeration(names);
		}
	}
}
