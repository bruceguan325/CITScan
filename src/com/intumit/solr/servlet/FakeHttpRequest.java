package com.intumit.solr.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

/**
 * Currently only fake non-array parameter
 * And can only fake in getParameter()
 * 
 * @author Herb
 *
 */
public class FakeHttpRequest implements HttpServletRequest {
	
	private HttpServletRequest origReq;
	private Hashtable<String, String> fakeMap = new Hashtable<String, String>();
	private HashSet<String> fakeRemovedMap = new HashSet<String>();
	
	public void setFakeParameter(String name, String value) {
		fakeMap.put(name, value);
	}
	
	public void removeParameter(String name) {
		fakeRemovedMap.add(name);
	}

	public Object getAttribute(String arg0) {
		return origReq.getAttribute(arg0);
	}

	public Enumeration getAttributeNames() {
		return origReq.getAttributeNames();
	}

	public String getAuthType() {
		return origReq.getAuthType();
	}

	public String getCharacterEncoding() {
		return origReq.getCharacterEncoding();
	}

	public int getContentLength() {
		return origReq.getContentLength();
	}

	public String getContentType() {
		return origReq.getContentType();
	}

	public String getContextPath() {
		return origReq.getContextPath();
	}

	public Cookie[] getCookies() {
		return origReq.getCookies();
	}

	public long getDateHeader(String arg0) {
		return origReq.getDateHeader(arg0);
	}

	public String getHeader(String arg0) {
		return origReq.getHeader(arg0);
	}

	public Enumeration getHeaderNames() {
		return origReq.getHeaderNames();
	}

	public Enumeration getHeaders(String arg0) {
		return origReq.getHeaders(arg0);
	}

	public ServletInputStream getInputStream() throws IOException {
		return origReq.getInputStream();
	}

	public int getIntHeader(String arg0) {
		return origReq.getIntHeader(arg0);
	}

	public String getLocalAddr() {
		return origReq.getLocalAddr();
	}

	public String getLocalName() {
		return origReq.getLocalName();
	}

	public int getLocalPort() {
		return origReq.getLocalPort();
	}

	public Locale getLocale() {
		return origReq.getLocale();
	}

	public Enumeration getLocales() {
		return origReq.getLocales();
	}

	public String getMethod() {
		return origReq.getMethod();
	}

	public String getParameter(String arg0) {
		if (fakeMap.containsKey(arg0))
			return fakeMap.get(arg0);
		if (fakeRemovedMap.contains(arg0) || origReq == null)
			return null;
		
		return origReq.getParameter(arg0);
	}

	public Map getParameterMap() {
		return origReq.getParameterMap();
	}

	public Enumeration getParameterNames() {
		return origReq.getParameterNames();
	}

	public String[] getParameterValues(String arg0) {
		return origReq.getParameterValues(arg0);
	}

	public String getPathInfo() {
		return origReq.getPathInfo();
	}

	public String getPathTranslated() {
		return origReq.getPathTranslated();
	}

	public String getProtocol() {
		return origReq.getProtocol();
	}

	public String getQueryString() {
		return origReq.getQueryString();
	}

	public BufferedReader getReader() throws IOException {
		return origReq.getReader();
	}

	public String getRealPath(String arg0) {
		return origReq.getRealPath(arg0);
	}

	public String getRemoteAddr() {
		if (origReq != null)
			return origReq.getRemoteAddr();
		else
			return null;
	}

	public String getRemoteHost() {
		return origReq.getRemoteHost();
	}

	public int getRemotePort() {
		return origReq.getRemotePort();
	}

	public String getRemoteUser() {
		return origReq.getRemoteUser();
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return origReq.getRequestDispatcher(arg0);
	}

	public String getRequestURI() {
		return origReq.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return origReq.getRequestURL();
	}

	public String getRequestedSessionId() {
		return origReq.getRequestedSessionId();
	}

	public String getScheme() {
		return origReq.getScheme();
	}

	public String getServerName() {
		return origReq.getServerName();
	}

	public int getServerPort() {
		return origReq.getServerPort();
	}

	public String getServletPath() {
		return origReq.getServletPath();
	}

	public HttpSession getSession() {
		return origReq.getSession();
	}

	public HttpSession getSession(boolean arg0) {
		return origReq.getSession(arg0);
	}

	public Principal getUserPrincipal() {
		return origReq.getUserPrincipal();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return origReq.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return origReq.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return origReq.isRequestedSessionIdFromUrl();
	}

	public boolean isRequestedSessionIdValid() {
		return origReq.isRequestedSessionIdValid();
	}

	public boolean isSecure() {
		return origReq.isSecure();
	}

	public boolean isUserInRole(String arg0) {
		return origReq.isUserInRole(arg0);
	}

	public void removeAttribute(String arg0) {
		origReq.removeAttribute(arg0);
	}

	public void setAttribute(String arg0, Object arg1) {
		origReq.setAttribute(arg0, arg1);
	}

	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		origReq.setCharacterEncoding(arg0);
	}

	public FakeHttpRequest(HttpServletRequest origReq) {
		this.origReq = origReq;
	}
	
	public FakeHttpRequest() {
		this.origReq = null;
	}

	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		return origReq.authenticate(arg0);
	}

	public String changeSessionId() {
		return origReq.changeSessionId();
	}

	public AsyncContext getAsyncContext() {
		return origReq.getAsyncContext();
	}

	public long getContentLengthLong() {
		return origReq.getContentLengthLong();
	}

	public DispatcherType getDispatcherType() {
		return origReq.getDispatcherType();
	}

	public Part getPart(String arg0) throws IOException, ServletException {
		return origReq.getPart(arg0);
	}

	public Collection<Part> getParts() throws IOException, ServletException {
		return origReq.getParts();
	}

	public ServletContext getServletContext() {
		return origReq.getServletContext();
	}

	public boolean isAsyncStarted() {
		return origReq.isAsyncStarted();
	}

	public boolean isAsyncSupported() {
		return origReq.isAsyncSupported();
	}

	public void login(String arg0, String arg1) throws ServletException {
		origReq.login(arg0, arg1);
	}

	public void logout() throws ServletException {
		origReq.logout();
	}

	public AsyncContext startAsync() throws IllegalStateException {
		return origReq.startAsync();
	}

	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		return origReq.startAsync(arg0, arg1);
	}

	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		return origReq.upgrade(arg0);
	}

}
