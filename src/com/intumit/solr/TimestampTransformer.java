package com.intumit.solr;

import java.util.Date;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;

/**
 * Used for convert unix timestamp to java Date
 * Unix timestamp is seconds (not millisecond) from "1970/01/01 00:00:00"
 * Use attribute format="millisecond" TO force transformer change to millisecond mode
 * @author Herb
 */
public class TimestampTransformer extends Transformer {

	private static final String TIMESTAMP = "timestamp";
	private static final String FORMAT = "format";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			if (!"true".equals(map.get(TIMESTAMP)))
				continue;

			toTRIM = true;
			String key = map.get(DataImporter.COLUMN);
			String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
			Object value = row.get(srcCol == null ? key : srcCol);
			
			if (value != null) {
				if (value instanceof Number) {
					Number lTimestamp = (Number) value;
					try {
						if (lTimestamp != null) {
							Date datee = new Date();
							datee.setTime("millisecond".equals(map.get(FORMAT)) ? lTimestamp.longValue() : (lTimestamp.longValue() * 1000));
							row.put(key, datee);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("*****" + value == null ? "Null Object" : value.getClass().getName());
				}
			}
		}

		if (!toTRIM) {
			System.out
					.println("There is no timestamp field, please set \"timestamp\" attribute (TRUE/FALSE) of String FIELD for converting");
		}
		return row;
	}
}
