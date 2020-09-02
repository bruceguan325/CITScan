package com.intumit.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

public class ClobTransformer extends Transformer {
	private static final Object CLOB = "clob";
	private static final Object ENCODING = "encoding";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean haveClob = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			if (!"true".equals(map.get(CLOB)))
				continue;

			haveClob = true;
			String key = map.get(DataImporter.COLUMN);
			String encoding = map.get(ENCODING) == null ? "UTF-8" : map.get(ENCODING);
			Object value = row.get(key);
			if (value instanceof Clob) {
				Clob clob = (Clob) value;
				try {
					if (clob != null) {
						String orig = asString(clob.getCharacterStream());
						row.put(key, orig);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (value instanceof Blob) {
				Blob clob = (Blob) value;
				try {
					if (clob != null) {
						String orig = new String(clob.getBytes(0, (int) clob.length()), encoding);
						row.put(key, orig);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (value instanceof byte[]) {
				byte[] clob = (byte[]) value;
				try {
					if (clob != null) {
						String orig = new String(clob, encoding);
						row.put(key, orig);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (!haveClob) {
			System.out
					.println("There is no clob field, please set \"clob\" attribute (TRUE/FALSE) of CLOB FIELD for converting");
		}
		return row;
	}

	public static String asString(Reader reader) throws IOException {
		if (reader == null) {
			return null;
		}

		BufferedReader br = new BufferedReader(reader);
		StringWriter sw = new StringWriter();
		char[] buf = new char[4096];
		int l;
		while ((l = br.read(buf)) != -1) {
			sw.write(buf, 0, l);
		}
		return sw.toString();
	}
}
