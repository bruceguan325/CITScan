package com.intumit.solr;

import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

public class EncodingInspectorTransformer extends Transformer {

	private static final String INSPECT = "inspect";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		boolean toTRIM = false;
		for (Map<String, String> map : context.getAllEntityFields()) {
			String doTRIM = map.get(INSPECT);
			if (doTRIM == null)
				continue;

			if (new Boolean(doTRIM)) {
				toTRIM = true;
				String key = map.get(DataImporter.COLUMN);
				Object value = row.get(key);
				if (value instanceof String) {
					String str = (String) value;
					try {
						if (str != null) {
							byte[] bArr = str.getBytes();
							System.out.println("****[" + str + "]****************");
							for (int i = 0; i < bArr.length; i++) {
								System.out.print(Integer.toHexString((int)bArr[i]));
							}
							System.out.println("*************************************");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return row;
	}
}
