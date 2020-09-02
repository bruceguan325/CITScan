package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import com.intumit.smartwiki.util.NameValuePair;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.util.WiSeUtils;

public abstract class QAOutputTemplate {
	
	private static final String DEFAULT_PACKAGE = "com.intumit.solr.robot.";
	static Map<String, Class> registeredClass = new HashMap<String, Class>();

	static {
		register(GeneralTextOutput.class);
	}

	public static Class findRegisteredClass(String type) {
		if (type.startsWith(DEFAULT_PACKAGE)) {
			type = StringUtils.substringAfter(type, DEFAULT_PACKAGE);
		}
		return registeredClass.get(type);
	}
	
	public static Collection<String> listTypes() {
		return registeredClass.keySet();
	}
	
	public static void register(Class clazz) {
		String clazzName = clazz.getName();
		
		if (clazzName.startsWith(DEFAULT_PACKAGE)) {
			clazzName = StringUtils.substringAfter(clazzName, DEFAULT_PACKAGE);
		}
		registeredClass.put(clazzName, clazz);
	}

	abstract public QAOutputResult output(QA customQa, QAContext qaContenxt, QAPattern qp, List<CustomData> nvPairs, QADataAggregator aggregator);
}
