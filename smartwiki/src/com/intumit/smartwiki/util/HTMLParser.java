package com.intumit.smartwiki.util;

public class HTMLParser {

	public static String removeHTMLTag(String str) {
		String tagPattern = "<{1}[^>]{1,}>{1}";
		return str.replaceAll(tagPattern, "");
	}
}
