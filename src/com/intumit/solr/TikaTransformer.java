package com.intumit.solr;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;
import org.apache.tika.metadata.Metadata;

import com.intumit.solr.util.FileParser;

/**
 * A handy tool for trim string (list<string>) data
 * Use trim="true" to enable field for trimming
 * And use trimTo="XXXX" when we got empty string after trimming.
 * If not set trimTo attribute, empty string will be dropped.
 * If use trimTo="XXX" XXX can be any string, empty string will be replace by "XXX"
 * @author Herb
 *
 */
public class TikaTransformer  extends Transformer {

	private static final String TIKA = "tika";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
		    if (!"true".equals(map.get(TIKA))) continue;
			toTRIM = true;
		    String srCol = map.get(DataImporter.COLUMN);
		      
		    if (map.containsKey(RegexTransformer.SRC_COL_NAME)) {
		          srCol = map.get(RegexTransformer.SRC_COL_NAME);
		    }
			
			String key = map.get(DataImporter.COLUMN);
			Object value = row.get(srCol);
			Metadata meta = new Metadata();
			
			if (value instanceof String) {
				String str = (String) value;
				try {
					if (str != null) {
						String cleaned = parse(str, meta);
                        row.put(key, cleaned);
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
							String cleaned = parse(str, meta);
							nVal.add(cleaned);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				row.put(key, nVal);
			}
			
			if (meta.get("title") != null) {
			    row.put("Name_t", meta.get("title"));
			}
		}

		if (!toTRIM) {
			System.out
					.println("There is no tika field, please set \"tika\" attribute (TRUE/FALSE) of String FIELD for converting");
		}
		return row;
	}

	private String parse(String filePath, Metadata meta) {
	    return FileParser.getInstance().autoParse(new File(filePath), meta);
	}
}
