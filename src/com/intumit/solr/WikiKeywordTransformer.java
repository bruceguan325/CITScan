package com.intumit.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.servlet.WikiUtilAll;

/**
 * 在 Import 資料的時後，把字串利用 Wiki Service 做有效的斷詞，以求建的索引 比較漂亮，使用時只要把所需斷詞的欄位加上
 * wiki="true"，並且在 schema.xml 儲存的索引欄位加上 multiValued="true"。可設定 limit="5" 來代表最多的關鍵字數量，預設為 10
 * 
 * @author Sbos Chang
 * 
 */
public class WikiKeywordTransformer extends Transformer {

	private static final String WIKI = "wiki";
	private static final String LIMIT = "limit";
	private static final String LOCALE = "locale";

	private Integer limit;

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		for (Map<String, String> map : context.getAllEntityFields()) {
			String doWIKI = map.get(WIKI);
			String localeStr = map.get(LOCALE);

			if (doWIKI == null)
				continue;
			
			HitHotLocale locale = HitHotLocale.valueOf(localeStr);

			if (new Boolean(doWIKI)) {

				String key = map.get(DataImporter.COLUMN);
				String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
				Object value = row.get(key);
				
				if (srcCol != null) {
					String[] srcCols = srcCol.split(",");
					
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
					List<String> list = new ArrayList<String>();
					
					for (int i=0; i < srcCols.length; i++) {
						Object v2 = row.get(srcCols[i]);
						if (v2 == null)
							continue;
						
						if (v2 instanceof String) {
							list.add((String)v2);
						}
						else if (v2 instanceof List) {
							list.addAll((List)v2);
						}
					}
					value = list;
				}

				// System.out.println("DO WIKI");
				String limitStr = map.get(LIMIT);

				if (limitStr != null) {
					limit = new Integer(limitStr);
				} else {
					limit = 10;
				}

				if (value instanceof String) {
					String str = (String) value;
					try {
						if (str != null) {
							List<String> cleaned = wiki(str, locale, limit);

							if (cleaned.size() > 0)
								row.put(key, cleaned);
							else
								row.remove(key);

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (value instanceof List) {
					List<String> list = (List<String>) value;
					String str = StringUtils.join(list, "\n");

					try {
						if (str != null) {
							List<String> cleaned = wiki(str, locale, limit);

							if (cleaned.size() > 0)
								row.put(key, cleaned);
							else
								row.remove(key);

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Not string, not list.... "
							+ value.getClass().toString());
				}
			}
		}

		return row;
	}

	private List<String> wiki(String str, HitHotLocale locale, int wikiCount) {

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		List<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(StringUtils.split(WikiUtilAll.wiki(str, locale, wikiCount),",")));
		return list;
	}
}
