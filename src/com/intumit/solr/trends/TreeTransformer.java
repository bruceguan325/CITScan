package com.intumit.solr.trends;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

/**
 * A handy tool for trim string (list<string>) data
 * Use trim="true" to enable field for trimming
 * And use trimTo="XXXX" when we got empty string after trimming.
 * If not set trimTo attribute, empty string will be dropped.
 * If use trimTo="XXX" XXX can be any string, empty string will be replace by "XXX"
 * @author Herb
 *
 */
public class TreeTransformer  extends Transformer {

	private static final String TREE = "tree";
	private static final String TYPE = "type";
	public static Map<String, String> classMap = new HashMap();

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTREE = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
		    if (!"true".equals(map.get(TREE))) continue;
		    toTREE = true;
			
			String type = map.get(TYPE);
			
			String key = map.get(DataImporter.COLUMN);
			Object value = row.get(key);
			if (value instanceof String) {
				String path = (String) value;
				try {
					if (path != null) {
						
						String[] a = path.split(",");
						path = "";
						for (int i = 0; i < a.length; i++) {
							if("class".equals(type))
							    path += getClassName(a[i], context) + "/";
						}
						
						
						if (path.trim().length() != 0)
							row.put(key, path);
						else {
							row.remove(key);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

		if (!toTREE) {
			System.out
					.println("There is no tree field, please set \"tree\" attribute (TRUE/FALSE) of String FIELD for converting");
		}
		return row;
	}

	private String getClassName(String str, Context context) {
	 if(classMap.size() != 0)	
	      str = classMap.get(str);	
	 return str;
	}

}
