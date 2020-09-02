package com.intumit.solr.robot;


public class UrlTransformerTest {

	public static void main(String[] args) {
		String url = "http://www.intumit.com";
		String text = "'這邊" + url + "，請瀏覽。";
		System.out.println("url in: " + url);
		System.out.println("text: " + text);
		System.out.println("text out: " + UrlTransformer.base64UrlEncodeAndPrepend(text, "http://www.me.com?value="));
	}
	
}
