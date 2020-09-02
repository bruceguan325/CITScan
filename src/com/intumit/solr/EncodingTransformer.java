package com.intumit.solr;

import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;

public class EncodingTransformer extends Transformer {

	private static final String CONVERT = "encoding";
	private static final String FROM_ENCODING = "from";
	private static final String TO_ENCODING = "to";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			String doTRIM = map.get(CONVERT);
			String from = map.get(FROM_ENCODING);
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
							String utf8 = null;
							
							if (from == null)
								utf8 = new String( str.getBytes(), to );
							else if (to == null)
								utf8 = new String( str.getBytes(from) );
							else 
								utf8 = new String( str.getBytes(from), to );
							
							if (utf8 != null) {
								row.put(key, utf8);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("NOT STRING Object:" + value);//value != null ? value.getClass().getName() : value);
				}
			}
		}

		if (!toTRIM) {
			System.out
					.println("There is no 'encoding' field, please set \"encoding\" attribute (TRUE/FALSE) AND from-to charset of String FIELD for converting");
		}
		return row;
	}
}
