package com.intumit.license;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.intumit.message.MessageUtil;

public class CheckInitialFilter implements Filter {
	private static final Logger log = Logger.getLogger(CheckInitialFilter.class);

	private static HashSet<String> nonCheckPath = null;

	public void init(FilterConfig conf) throws ServletException {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		log.debug(request.getRequestURI() + " enter CheckInitialFilter!");
		
		if (needCheck(request)) {
			if (InitialReport.hasError()) {
				String message = InitialReport.getErrorMessage() + "\n\n" + getMessage("initialError.inform");
				ErrorPage.toDisplayMessage(message, (HttpServletRequest)request, (HttpServletResponse)response);
				return;
			}
		}
		chain.doFilter(request, response);
	}

	public boolean needCheck(HttpServletRequest request) {
		if (nonCheckPath == null) {
			initNonCheck(request.getContextPath());
		}
		return StringUtils.endsWithAny(StringUtils.lowerCase(request.getRequestURI()), new String[] { ".jsp", ".html" }) && !nonCheck(request.getRequestURL());
	}

	private boolean nonCheck(StringBuffer requestURL) {
		if (nonCheckPath != null) {
			return nonCheckPath.contains(StringUtils.lowerCase(requestURL.toString()));
		}
		return false;
	}

	private synchronized void initNonCheck(String contextPath) {
		if (nonCheckPath == null) {
			contextPath = contextPath.toLowerCase() + "/";
			nonCheckPath = new HashSet<String>();
			nonCheckPath.add(contextPath + "ErrorPage.jsp".toLowerCase());
			nonCheckPath.add(contextPath + "ErrorPage403.jsp".toLowerCase());
			nonCheckPath.add(contextPath + "ErrorPage404.jsp".toLowerCase());
			nonCheckPath.add(contextPath + "ShowMessage.jsp".toLowerCase());
		}
	}

	private static final String getMessage(String resourceKey) {
		return MessageUtil.getMessage(Locale.TAIWAN, resourceKey);
	}
}
