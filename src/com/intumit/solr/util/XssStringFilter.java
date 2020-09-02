package com.intumit.solr.util;

import org.apache.commons.lang.StringUtils;

public class XssStringFilter {

	private final static HTMLFilter HTML_FILTER = new HTMLFilter();
	private final static String EMPTY = new String();
	
	public String escapeHtml2(String val) {
		if (StringUtils.trimToNull(val) != null) {
			String value = new String(val);
			value = fakeXssEncode(value);
			return value;
		}
		return null;
	}

	public String getFakeCheck(String val) {
		if (StringUtils.trimToNull(val) != null) {
			String value = new String(val);
			value = fakeXssEncode(value);
			return value;
		}
		return null;
	}
	
	public String escapeHtml(String val) {
		if (StringUtils.trimToNull(val) != null) {
			String value = new String(val);
			value = xssEncode(value);
			return value;
		}
		return null;
	}

	public String getCheck(String val) {
		if (StringUtils.trimToNull(val) != null) {
			String value = new String(val);
			value = xssEncode(value);
			return value;
		}
		return null;
	}

	// HTTP Response Splitting : detect and remove any existent \r\n == %0D%0A == CRLF to prevent
	public String resoveSplitting(String url) {
		if (StringUtils.trimToNull(url) != null) {
			String clean = new String(url);
			clean = clean.replace("\n", EMPTY).replace("\r", EMPTY).replace("%0d", EMPTY).replace("%0D", EMPTY)
					.replace("%0a", EMPTY).replace("%0A", EMPTY);
			return clean;
		}
		return null;
	}

	private String fakeXssEncode(String input) {
		return HTML_FILTER.fakeFilter(input);
	}

	private String xssEncode(String input) {
		return HTML_FILTER.filter(input);
	}

}
