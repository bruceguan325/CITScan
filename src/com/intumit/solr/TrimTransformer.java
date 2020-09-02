package com.intumit.solr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class TrimTransformer  extends Transformer {

	private static final String TRIM = "trim";
	private static final String TRIMTO = "trimTo";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
		    if (!"true".equals(map.get(TRIM))) continue;
			toTRIM = true;
			
			String trimTo = map.get(TRIMTO);
			
			String key = map.get(DataImporter.COLUMN);
			Object value = row.get(key);
			if (value instanceof String) {
				String str = (String) value;
				try {
					if (str != null) {
						String cleaned = trim(str);
						
						if (cleaned.trim().length() != 0)
							row.put(key, cleaned);
						else {
							if (trimTo == null)
								row.remove(key);
							else
								row.put(key, trimTo);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (value instanceof List) {
				List lVal = (List<String>)value;
				List nVal = new ArrayList<String>();
				
				for (Iterator<String> itr=lVal.iterator(); itr.hasNext(); ) {
					String str = itr.next();

					try {
						if (str != null) {
							String cleaned = trim(str);
							
							if (cleaned.trim().length() != 0)
								nVal.add(cleaned);
							else if (trimTo != null) {
								nVal.add(trimTo);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				row.put(key, nVal);
			}
		}

		if (!toTRIM) {
			System.out
					.println("There is no trim field, please set \"trim\" attribute (TRUE/FALSE) of String FIELD for converting");
		}
		return row;
	}

	private String trim(String text) {
		return text.trim();
	}
}
