package com.intumit.solr.facet.tree;

import java.util.Iterator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

public class TestTreeFacetingUI {
	// @Test
	public void fatchxml() throws Exception {
		String fieldNames = "MainCategoryName,SecondCategoryName";
		HttpClient client = new HttpClient();

		client.getState().setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials("root", "intumitdemo"));

		String query = "q=*:*&rows=0&facet=on&facet.tree=" + fieldNames;
		GetMethod get = new GetMethod(
				"http://localhost:8080/wise/wiseadm/core0/select?" + query);
		client.executeMethod(get);
		String xml = get.getResponseBodyAsString();
		System.out.println(xml);
	}

	@Test
	public void parseXml() throws Exception {

		String xml = IOUtils.toString(getClass().getResourceAsStream(
				"select.xml"));

		String fieldNames = "MainCategoryName,SecondCategoryName";
		try {
			Document doc = DocumentHelper.parseText(xml);
			// 這個xpath的意思是,獲取text='系統管理'的一個Item下的所有Item的節點
			String xpath = "//lst[@name='" + fieldNames + "']/child::*";

			for (Iterator i = doc.selectNodes(xpath).iterator(); i.hasNext();) {
				Element elt = (Element) i.next();

				Attribute attr = elt.attribute("name");
				StringBuilder sb = new StringBuilder();
				int parentcount = 0;
				for (Iterator i2 = elt.elementIterator(); i2.hasNext();) {
					Element child = (Element) i2.next();
					String ctext = child.getText();
					if (!"0".equals(ctext)) {
						parentcount += Integer.valueOf(ctext);
						sb.append("<li><a href=\"#\">");
						sb.append(child.attributeValue("name") + ctext);
						sb.append("</a></li>");
					}
				}
				System.out.println("<li><a href=\"#\">" + attr.getValue()
						+ parentcount + "</a>");
				System.out.println("<ul>");
				System.out.println(sb.toString());
				System.out.println("</ul></li>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
