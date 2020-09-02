package com.intumit.solr;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

public abstract class TransformerBase extends Transformer {

	public Object transformRow(Map<String, Object> row, Context context) {

		boolean haveHTML = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			String doit = map.get(attr());
			if (doit == null)
				continue;
			haveHTML = new Boolean(doit);

			if (haveHTML) {
				String key = map.get(DataImporter.COLUMN);
				Object value = row.get(key);
				if (value instanceof String) {
					String str = (String) value;
					try {
						if (str != null) {
							String cleaned = transform(StringUtils.stripToEmpty(str));

							if (cleaned.trim().length() != 0)
								row.put(key, cleaned);
							else
								row.remove(key);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		return row;
	}


	protected abstract String transform(String str); 
	protected abstract String attr(); 
		
}
