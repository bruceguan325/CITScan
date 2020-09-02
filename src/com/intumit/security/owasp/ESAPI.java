package com.intumit.security.owasp;

import org.apache.commons.lang.StringEscapeUtils;

public class ESAPI {

	public static class Encoder {
		Encoder() {}
		
		public String encodeForJavaScript(String str) {
			return StringEscapeUtils.escapeJavaScript(str);
		}
		
		public String encodeForHTML(String str) {
			return StringEscapeUtils.escapeHtml(str);
		}
		
		public String encodeForHTMLAttribute(String str) {
			return StringEscapeUtils.escapeHtml(str);
		}
	}

	private static Encoder encoder;
	
	static {
		encoder = new Encoder();
	}
	
	public static Encoder encoder() {
		return encoder;
	}
}
