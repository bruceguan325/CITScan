package com.intumit.solr.util;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class VelocityManager {

	private static final String TEMPLATE_ENCODING = "UTF-8";

	private static VelocityManager instance = new VelocityManager();

	private VelocityEngine ve = new VelocityEngine();

	public static VelocityManager getInstance() {
		return instance;
	}

	private VelocityManager() {
		super();
		init();
	}

	private void init() {
		Properties p = new Properties();
		p.setProperty("input.encoding", TEMPLATE_ENCODING);
		try {
			ve.init(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getTemplateString(String templateName, VelocityContext context) {
		StringWriter writer = new StringWriter();
		try {
			String s = IOUtils.toString(getClass().getResourceAsStream(
					templateName), TEMPLATE_ENCODING);
			ve.evaluate(context, writer, null, s);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return writer.toString();
	}



}
