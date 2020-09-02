package com.intumit.solr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.solr.util.WiSeUtils;

public class TranslateTransformer extends Transformer {

	private static final String CONVERT = "translate";
	private static final String TO_ENCODING = "to";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			String doTRIM = map.get(CONVERT);
			String to = map.get(TO_ENCODING);
			if (doTRIM == null)
				continue;

			if (new Boolean(doTRIM)) {
				toTRIM = true;
				String key = map.get(DataImporter.COLUMN);
				String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
				Object value = srcCol == null ? row.get(key) : row.get(srcCol);
				
				if (value instanceof String) {
					String str = (String) value;
					try {
						if (str != null) {
							String translated = null;

					    	if ("t".equalsIgnoreCase(to)) {
					    		translated = WiSeUtils.cn2tw(str);
					    	}
					    	else if ("s".equalsIgnoreCase(to)) {
					    		translated = WiSeUtils.tw2cn(str);
					    	}
					    	
							if (translated != null) {
								row.put(key, translated);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if (value instanceof List) {
					List<String> listV = (List<String>)value;
					List<String> translatedList = new ArrayList<String>();
					
					for (String str: listV) {
						try {
							if (str != null) {
								String translated = null;
	
						    	if ("t".equalsIgnoreCase(to)) {
						    		translated = WiSeUtils.cn2tw(str);
						    	}
						    	else if ("s".equalsIgnoreCase(to)) {
						    		translated = WiSeUtils.tw2cn(str);
						    	}
						    	
								if (translated != null) {
									translatedList.add(translated);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					row.put(key, translatedList);
				}
				else {
					System.out.println("NOT STRING Object:" + value);//value != null ? value.getClass().getName() : value);
				}
			}
		}

		if (!toTRIM) {
			System.out
					.println("There is no 'translate' field, please set \"translate\" attribute (TRUE/FALSE) AND to s/t of String FIELD for converting");
		}
		return row;
	}
}
