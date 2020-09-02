package com.intumit.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.model.WikiWordtable;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.recommend.renew.AhoCorasick;
import com.intumit.smartwiki.recommend.renew.AhoCorasickFactory;
import com.intumit.smartwiki.recommend.renew.RecommendUtil;
import com.intumit.smartwiki.util.Constants;
import com.intumit.smartwiki.util.TextUtil;
import com.intumit.solr.servlet.WikiUtilAll;

/**
 * 在 Import 資料的時後，把字串利用 Wiki Service 做有效的斷詞，以求建的索引 比較漂亮，使用時只要把所需斷詞的欄位加上
 * wiki="true"，並且在 schema.xml 儲存的索引欄位加上 multiValued="true"。可設定 limit="5" 來代表最多的關鍵字數量，預設為 10
 * 
 * @author Sbos Chang
 * 
 */
public class LocalWikiKeywordTransformer extends Transformer {
	private Set<String> EMPTY_SET = new HashSet<String>(0);

	private static final String WIKI = "wiki";
	private static final String LIMIT = "limit";
	private static final String LOCALE = "locale";
	private static final String ALL_KEYWORDS = "all";

	private Integer limit;

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		for (Map<String, String> map : context.getAllEntityFields()) {
			String doWIKI = map.get(WIKI);
			String localeStr = map.get(LOCALE);
			Boolean allKw = new Boolean(StringUtils.defaultString(map.get(ALL_KEYWORDS), "false"));

			if (doWIKI == null)
				continue;
			
			HitHotLocale locale = HitHotLocale.valueOf(localeStr);

			if (new Boolean(doWIKI)) {

				String key = map.get(DataImporter.COLUMN);
				String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
				Object value = row.get(key);
				
				if (srcCol != null) {
					String[] srcCols = srcCol.split(",");
					
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
							List<String> cleaned = wiki(str, locale, EMPTY_SET, limit, allKw);

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
							List<String> cleaned = wiki(str, locale, EMPTY_SET, limit, allKw);

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

	public static List<String> wiki(String str, HitHotLocale locale, Set<String> blackList, int wikiCount, boolean isAll) {
		List<String> highlightWordList = getNewKeywords(locale, str, blackList, false);
		List<String> keywords = null;
		if (highlightWordList.size() > wikiCount) {
			keywords = highlightWordList.subList(0, wikiCount);
		}
		else {
			keywords = highlightWordList;
		}
		return keywords;
	}
	

	static List<String> getNewKeywords(HitHotLocale locale, String content, Set<String> blackList, boolean isAll) {
		String text = TextUtil.preprocessText(content);
		
		TreeSet<WikiWord> sorted = new TreeSet<WikiWord>(wikiSort); 
		sorted.addAll(AhoCorasickFactory.getInstance(locale).getWikiWord(text));
		

		List<String> keywords = new ArrayList<String>();
		for (WikiWord wiki : sorted) {
			if (blackList.contains(wiki.getPageTitle()))
				continue;
			//System.out.println(isAll + ":result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			if (isAll && wiki.getInitScore() > 0) {
				keywords.add(wiki.getPageTitle());
			}
		}

		WikiWordtable matchedPatterns = new WikiWordtable(locale);
		for (WikiWord wiki : sorted) {
			if (blackList.contains(wiki.getPageTitle()))
				continue;
			matchedPatterns.put(wiki.getPageTitle(), wiki);
		}

		matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字

		List<WikiWord> wikiWordList = RecommendUtil.calculateScore(locale, matchedPatterns);
		//StringBuilder newKeywords = new StringBuilder("");
		for (WikiWord wiki : wikiWordList) {
			if (isAll || Constants.RECOMMEND.equals(wiki.getRecommand())) {
				//if (newKeywords.length() > 0) newKeywords.append(",");
				//newKeywords.append(wiki.getPageTitle());
				//System.out.println("result2:" + wiki.getPageTitle() + " / " + wiki.getTotalScore());

				if (!keywords.contains(wiki.getPageTitle()))
					keywords.add(wiki.getPageTitle());
			}
		}

		return keywords;
	}

	static Comparator wikiSort = new Comparator(){

	public int compare(Object o1, Object o2) {
		WikiWord w1 = (WikiWord)o1; 
		WikiWord w2 = (WikiWord)o2;
		// TODO Auto-generated method stub
		return w1.getInitScore() >= w2.getInitScore() ? -1 : 1;
	}};
}