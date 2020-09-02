package com.intumit.solr.robot.qaplugin;

import com.intumit.solr.robot.qaplugin.QAPlugins.Registry;

public class QAPluginsTestUtil {

	public static void insert(Class clazz, String id, String name) {
		QAPlugins.registries.put(id, new Registry(clazz, id, name));
	}
	
	public static void remove(String id) {
		QAPlugins.registries.remove(id);
	}
}
