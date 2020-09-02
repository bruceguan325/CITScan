package com.intumit.solr;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.intumit.solr.servlet.SolrDispatchFilter;

public class DumpEverythingFilter implements Filter {
	private static final Logger log = Logger.getLogger(DumpEverythingFilter.class);

	private static HashSet<String> skipThosePaths = null;

	public void init(FilterConfig conf) throws ServletException {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
		if (needDump(request)) {
			String method = request.getMethod();
			
			if ("put".equalsIgnoreCase(method)) {
				System.out.println("Got you!");
			}
			System.out.println(String.format("[%s - %s] [%s] %s", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()), SolrDispatchFilter.getClientIpAddr(request), request.getMethod(), request.getRequestURI()));
		}
		chain.doFilter(request, response);
	}

	public boolean needDump(HttpServletRequest request) {
		if (skipThosePaths == null) {
			initPathsToBeSkip(request.getContextPath());
		}
		return !dontDumpPlz(request.getRequestURL());
	}

	private boolean dontDumpPlz(StringBuffer requestURL) {
		if (skipThosePaths != null) {
			return skipThosePaths.contains(StringUtils.lowerCase(requestURL.toString()));
		}
		return false;
	}

	private synchronized void initPathsToBeSkip(String contextPath) {
		if (skipThosePaths == null) {
			contextPath = contextPath.toLowerCase() + "/";
			skipThosePaths = new HashSet<String>();
			skipThosePaths.add(contextPath + "ErrorPage.jsp".toLowerCase());
			skipThosePaths.add(contextPath + "ErrorPage403.jsp".toLowerCase());
			skipThosePaths.add(contextPath + "ErrorPage404.jsp".toLowerCase());
			skipThosePaths.add(contextPath + "ErrorPage503.jsp".toLowerCase());
			skipThosePaths.add(contextPath + "ShowMessage.jsp".toLowerCase());
		}
	}

	private static final String getMessage(String resourceKey) {
		return MessageUtil.getMessage(Locale.TAIWAN, resourceKey);
	}
}
