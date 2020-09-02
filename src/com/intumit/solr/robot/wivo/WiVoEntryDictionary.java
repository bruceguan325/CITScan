package com.intumit.solr.robot.wivo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.lang3.StringUtils;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.android.search.fuzzy.Piece;
import com.intumit.android.search.fuzzy.VROResult;
import com.intumit.android.search.fuzzy.VoiceRecognitionOptimizer;
import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;
import com.intumit.android.search.util.TaiwanAddressNormalizeUtil.AddrPart;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.WiVoUtil;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;

/**
 * 簡易的自訂辭典斷詞工具 快速、高效
 * 
 * @author herb
 */
public class WiVoEntryDictionary extends BaseChineseDictionary implements Serializable
{
	static Map<Integer, AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>> trieMap = new HashMap<Integer, AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>>();
	static Map<String, AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>> trieChannelMap = new HashMap<String, AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>>();

	// 暫時有個小地方可以解決特定詞一直被轉換的問題
	static Set<String> DONT_TOUCH_ME = new HashSet<String>(Arrays.asList(new String[] { "" }));
	
	public static void clear(Integer tenantId) {
		trieMap.remove(tenantId);
		
		Iterator<String> itr = trieChannelMap.keySet().iterator();
		
		while (itr.hasNext()) {
			String key = itr.next();
			
			if (StringUtils.startsWith(key, "" + tenantId + "_")) {
				itr.remove();
			}
		}
	}

	public static AhoCorasickDoubleArrayTrie<Set<WiVoEntry>> loadIfNull(Integer tenantId, String channel) {
		if (channel == null) {
			if (!trieMap.containsKey(tenantId)) {
				//Tenant t = Tenant.get(companyId);
				List<WiVoEntry> ddList = WiVoEntry.listByTenantId(tenantId);
				if (ddList.size() == 0) return null;
				AhoCorasickDoubleArrayTrie<Set<WiVoEntry>> trie = new AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>();
	
				TreeMap<String, Set<WiVoEntry>> map = new TreeMap<String, Set<WiVoEntry>>();
	
				for (WiVoEntry dd : ddList) {
					if (dd.isEnabled() 
							&& StringUtils.isEmpty(dd.getChannel()) // channel is empty 代表是「全部」
							&& StringUtils.isNotEmpty(dd.getIncludes())) {
						String[] includeKws = StringUtils.split(dd.getIncludes(), ",");
	
						for (String keyword : includeKws) {
							if (map.containsKey(keyword)) map.get(keyword).add(dd);
							else {
								Set<WiVoEntry> set = new HashSet<WiVoEntry>();
								set.add(dd);
								map.put(keyword, set);
							}
						}
					}
				}
				
				System.out.println("WiVoEntry map for (全部):" + map);
	
				if (map.size() == 0) {
					trieMap.put(tenantId, null);
					return null;
				}
				
				trie.build(map);
				trieMap.put(tenantId, trie);
			}
			return trieMap.get(tenantId);
		}
		else {
			// 新增trieCategoryMap，for租戶-分類的知識點
			if (!trieChannelMap.containsKey(tenantId + "_" + channel)) {
				List<WiVoEntry> ddList = WiVoEntry.listByTenantIdAndChannel(tenantId, channel, true);
				if (ddList.size() == 0) {
					trieChannelMap.put(tenantId + "_" + channel, trieMap.get(tenantId));
					return trieChannelMap.get(tenantId + "_" + channel);
				}
				AhoCorasickDoubleArrayTrie<Set<WiVoEntry>> trie = new AhoCorasickDoubleArrayTrie<Set<WiVoEntry>>();
				TreeMap<String, Set<WiVoEntry>> map = new TreeMap<String, Set<WiVoEntry>>();

				for (WiVoEntry dd : ddList) {
					if (StringUtils.isEmpty(dd.getChannel()) || dd.getChannel().equals(channel)) {
						String[] includeKws = StringUtils.split(dd.getIncludes(), ",");

						for (String keyword : includeKws) {
							if (map.containsKey(keyword)) map.get(keyword).add(dd);
							else {
								Set<WiVoEntry> set = new HashSet<WiVoEntry>();
								set.add(dd);
								map.put(keyword, set);
							}
						}
					}
				}

				if (map.size() == 0) return null;

				trie.build(map);
				trieChannelMap.put(tenantId + "_" + channel, trie);
			}
			return trieChannelMap.get(tenantId + "_" + channel);
		}
	}
	
	public static Set<WiVoEntryMatched>[] search(Integer tenantId, String text, String channel) {
		final char[] charArray = text.toCharArray();
		final List<Set<WiVoEntryMatched>> results = new ArrayList<Set<WiVoEntryMatched>>();
		AhoCorasickDoubleArrayTrie<Set<WiVoEntry>> trie = loadIfNull(tenantId, channel);
		if (trie == null) return (Set<WiVoEntryMatched>[]) results.toArray(new Set<?>[0]);

		final Object[] wordNet = new Object[charArray.length];
		final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<WiVoEntry>>()
		{
			@Override
			public void hit(int begin, int end, Set<WiVoEntry> value)
			{
				int length = end - begin;
				if (length > lengthNet[begin])
				{
					wordNet[begin] = value;
					lengthNet[begin] = length;
				}
			}
		});

		for (int offset = 0; offset < wordNet.length;)
		{
			if (wordNet[offset] == null) {
				++offset;
				continue;
			}
			else {
				Object obj = wordNet[offset];

				Set<WiVoEntry> set = (Set<WiVoEntry>) obj;
				Set<WiVoEntryMatched> clonedSet = new HashSet<WiVoEntryMatched>();

				for (WiVoEntry ed : set) {
					// newOne.setId((long)(Math.random() * 10000L));
					if (offset + lengthNet[offset] < charArray.length 
							&& isEnglishAlphabet(charArray[offset + lengthNet[offset] - 1]) 
							&& isEnglishAlphabet(charArray[offset + lengthNet[offset]])) {
						// 比對出來的字串最後一個字元是英文字，且下一個字元也是英文字。這樣的情形則跳過不替換
						//System.out.println("Skip " + new String(charArray, offset, lengthNet[offset]) + ", more letters after it." + charArray[offset + lengthNet[offset]]);
						continue;
					}

					if (offset > 0 
							&& isEnglishAlphabet(charArray[offset]) 
							&& isEnglishAlphabet(charArray[offset - 1])) {
						// 比對出來的字串第一個字元是英文字，且前一個字元也是英文字。這樣的情形則跳過不替換
						//System.out.println("Skip " + new String(charArray, offset, lengthNet[offset]) + ", more letters before it." + charArray[offset - 1]);
						continue;
					}
					String val = new String(charArray, offset, lengthNet[offset]);
					
					WiVoEntryMatched newOne = (WiVoEntryMatched) ed.getCopy(new WiVoEntryMatched());
					newOne.setMatchedPosition(offset);
					newOne.setMatchedValue(val);

					clonedSet.add(newOne);
				}
				
				if (clonedSet.size() > 0) {
					results.add(clonedSet);
					offset += lengthNet[offset];
				}
				else {
					++offset;
					continue;
				}
			}
		}

		return (Set<WiVoEntryMatched>[]) results.toArray(new Set<?>[0]);
	}
	
	static boolean isEnglishAlphabet(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	public static void main(String[] args) {
		HibernateUtil.init();
		Set<WiVoEntryMatched>[] results = WiVoEntryDictionary.search(7, "我要開卡，你這個混蛋", null);

		for (Set<WiVoEntryMatched> resultSet : results) {
			for (WiVoEntry result : resultSet) {
				System.out.println("(" + result.getId() + "). " + result.getChannel() + "." + result.getKeyword()
						+ ":" + result.getIncludes() + "(" + result.getExcludes() + ")");
			}
		}
	}
}
