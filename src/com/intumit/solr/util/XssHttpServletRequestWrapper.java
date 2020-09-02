package com.intumit.solr.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS過濾處理
 * 
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    /**
     * 沒被包裝過的HttpServletRequest（特殊場景，需要自己過濾）
     */
    private HttpServletRequest orgRequest;
    /**
     * html過濾
     */
    private final static HTMLFilter HTML_FILTER = new HTMLFilter();
    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request request
     * @throws IllegalArgumentException if the request is null
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IllegalArgumentException {
        super(request);
        orgRequest = request;
    }

	@Override
    public ServletInputStream getInputStream() throws IOException {
        //非json類型，直接返回
        if(!super.getHeader(HttpHeaders.CONTENT_TYPE).equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)){
            return super.getInputStream();
        }

        //為空，直接返回
        String json = IOUtils.toString(super.getInputStream(), "utf-8");
        if (StringUtils.trimToNull(json) == null) {
            return super.getInputStream();
        }

        //xss過濾
        json = xssEncode(json);
        final ByteArrayInputStream bis = new ByteArrayInputStream(json.getBytes("utf-8"));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bis.read();
            }
        };
    }
	
	public ServletInputStream getFakeInputStream() throws IOException {
		//為空，直接返回
        String text = IOUtils.toString(super.getInputStream(), "utf-8");
        if (StringUtils.trimToNull(text) == null) {
            return super.getInputStream();
        }

        //xss過濾
        fakeXssEncode(text);
        final ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes("utf-8"));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bis.read();
            }
        };
    }

    /**
     * 過濾參數
     * @param name 參數名
     * @return String value
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (StringUtils.trimToNull(value) != null) {
            value = xssEncode(value);
        }
        return value;
    }
    
    public String getFakeParameter(String name) {
        String value = super.getParameter(name);
        if (StringUtils.trimToNull(value) != null) {
            fakeXssEncode(value);
        }
        return value;
    }
    
    public Object getFakeAttribute(String name) {
    	Object value = super.getAttribute(name);
        if (value != null) {
            fakeXssEncode(value.toString());
        }
        return value;
    }
    
    public String getFakeQueryString() {
        String value = super.getQueryString();
        if (StringUtils.trimToNull(value) != null) {
            fakeXssEncode(value);
        }
        return value;
    }
    
    public String getFaksePathInfo() {
    	String value = super.getPathInfo();
        if (StringUtils.trimToNull(value) != null) {
            fakeXssEncode(value);
        }
        return value;
    }
    
    public StringBuffer getFakeRequestURL(){
    	String value = super.getRequestURL().toString();
    	if (StringUtils.trimToNull(value) != null) {
            fakeXssEncode(value);
        }
    	return new StringBuffer().append(value);
    }

    /**
     * 過濾參數，值為數組
     * @param name 參數名
     * @return String[]
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] parameters = super.getParameterValues(name);
        if (parameters == null || parameters.length == 0) {
            return null;
        }

        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = xssEncode(parameters[i]);
        }
        return parameters;
    }
    
    public String[] getFakeParameterValues(String name) {
        String[] parameters = super.getParameterValues(name);
        if (parameters == null || parameters.length == 0) {
            return null;
        }

        for (int i = 0; i < parameters.length; i++) {
            fakeXssEncode(parameters[i]);
        }
        return parameters;
    }

    /**
     * 過濾參數，返回鍵值對形式的參數類型
     * @return Map
     */
    @Override
    public Map<String,String[]> getParameterMap() {
        Map<String,String[]> map = new LinkedHashMap<>();
        Map<String,String[]> parameters = super.getParameterMap();
        for (String key : parameters.keySet()) {
            String[] values = parameters.get(key);
            for (int i = 0; i < values.length; i++) {
                values[i] = xssEncode(values[i]);
            }
            map.put(key, values);
        }
        return map;
    }
    
    public Map<String,String[]> getFakeParameterMap() {
        Map<String,String[]> map = new LinkedHashMap<>();
        Map<String,String[]> parameters = super.getParameterMap();
        for (String key : parameters.keySet()) {
            String[] values = parameters.get(key);
            for (int i = 0; i < values.length; i++) {
                fakeXssEncode(values[i]);
            }
            map.put(key, values);
        }
        return map;
    }

    /**
     * 獲取request的頭屬性，並且進行xss過濾，返回它的值
     * @param name 屬性名
     * @return String 值
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (StringUtils.trimToNull(value) != null) {
            value = xssEncode(value);
        }
        return value;
    }
    
    public String getFakeHeader(String name) {
        String value = super.getHeader(name);
        if (StringUtils.trimToNull(value) != null) {
            fakeXssEncode(value);
        }
        return value;
    }

    private String xssEncode(String input) {
        return HTML_FILTER.filter(input);
    }
    
    private String fakeXssEncode(String input) {
		return HTML_FILTER.fakeFilter(input);
	}

    /**
     * 獲取最原始的request
     * @return HttpServletRequest 原始的request
     */
    public HttpServletRequest getOrgRequest() {
        return orgRequest;
    }

    /**
     * 獲取最原始的request，明確不進行xss過濾的
     * @param request request
     * @return HttpServletRequest 原始request
     */
    public static HttpServletRequest getOrgRequest(HttpServletRequest request) {
        if (request instanceof XssHttpServletRequestWrapper) {
            return ((XssHttpServletRequestWrapper) request).getOrgRequest();
        }
        return request;
    }
}