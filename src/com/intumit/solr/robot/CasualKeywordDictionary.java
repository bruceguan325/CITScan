package com.intumit.solr.robot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.elasticsearch.common.lang3.StringUtils;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.message.MessageUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.RobotFormalAnswers;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

/**
 * 簡易的自訂辭典斷詞工具 快速、高效
 *
 * @author herb
 */
public class CasualKeywordDictionary extends BaseChineseDictionary {
	static AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> trie = null;

	public static void clear() {
		trie = null;
	}
	
	public static void clearCache() {
		File cacheDir = new File(WiSeEnv.getHomePath() + "/dict/customdata/");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		String cachePath = WiSeEnv.getHomePath() + "/dict/customdata/casualKeyword.cache.forest";
		String cacheValuePath = WiSeEnv.getHomePath() + "/dict/customdata/casualKeyword.cache.value";
		File cacheFile = new File(cachePath);
		File cacheValueFile = new File(cacheValuePath);
		
		if (cacheFile.exists()) {
			cacheFile.delete();
		}
		if (cacheValueFile.exists()) {
			cacheValueFile.delete();
		}
	}

	public static AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> loadIfNull(Integer tenantId) {
		if (trie == null) {
			AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> tmptrie = new AhoCorasickDoubleArrayTrie<Set<CasualKeyword>>();

			try {
				File cacheDir = new File(WiSeEnv.getHomePath() + "/dict/customdata/");
				if (!cacheDir.exists()) {
					cacheDir.mkdirs();
				}
				String cachePath = WiSeEnv.getHomePath() + "/dict/customdata/casualKeyword.forest";
				String cacheValuePath = WiSeEnv.getHomePath() + "/dict/customdata/casualKeyword.value";
				File cacheFile = new File(cachePath);
				File cacheValueFile = new File(cacheValuePath);

				if (cacheFile.exists()) {
					long begin = System.currentTimeMillis();
					tmptrie.load(new ObjectInputStream(new FileInputStream(cacheFile)), new ObjectInputStream(new FileInputStream(cacheValueFile)));
					System.out.println("CasualKeyword trie.load() from cache = " + (System.currentTimeMillis() - begin) + "ms");
				}
				else {
					Tenant t = Tenant.get(tenantId);
					SolrServer server = QAUtil.getCasualServer(t, false);
					Set<CasualKeyword> cdSet = CasualKeyword.getAll(new HashSet<CasualKeyword>(), server);

					if (cdSet.size() == 0) return null;

					cdSet.add(new CasualKeyword("", "ROBOTNAME", "你"));
					TreeMap<String, Set<CasualKeyword>> map = new TreeMap<String, Set<CasualKeyword>>();
					for (CasualKeyword cd : cdSet) {
						if (map.containsKey(cd.getValue())) map.get(cd.getValue()).add(cd);
						else {
							Set<CasualKeyword> set = new HashSet<CasualKeyword>();
							set.add(cd);
							map.put(cd.getValue(), set);
						}
					}

					long begin = System.currentTimeMillis();
					tmptrie.build(map);
					tmptrie.save(new ObjectOutputStream(new FileOutputStream(cacheFile)));
					tmptrie.saveValue(new ObjectOutputStream(new FileOutputStream(cacheValueFile)));

					System.out.println("CasualKeyword trie.build(map) = " + (System.currentTimeMillis() - begin) + "ms");
				}

				trie = tmptrie;
			}
			catch (Exception e) {
				System.out.println("Error loading from custom data dictionary cache, clear it!");
				e.printStackTrace();
				clearCache();
				clear();
			}
		}
		return trie;
	}
	
	static boolean isLetterOrDigit(char c) {
		return Character.isDigit(c) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '/') || (c == '-') || (c == '_');
	}

	public static Set<CasualKeyword> search(Integer tenantId, final char[] charArray) {
		final Set<CasualKeyword> results = new HashSet<>();
		AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> trie = loadIfNull(tenantId);
		if (trie == null) return results;

		final Object[] wordNet = new Object[charArray.length];
		final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CasualKeyword>>() {
			@Override
			public void hit(int begin, int end, Set<CasualKeyword> value) {
				int length = end - begin;
				
				if (length > charArray.length || begin >= charArray.length) return;
				
				boolean allAscii = true;
				for (int i=begin; i < end; i++) {
					if (!isLetterOrDigit(charArray[i])) {
						//System.out.println("[" + charArray[i] + "] is not isLetterOrDigit");
						allAscii = false;
						break;
					}
				}
				
				if (allAscii) {
					
					if (begin > 0 && isLetterOrDigit(charArray[begin-1])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin-1, length+1) + "]");
						return;
					}
					if (end < charArray.length && isLetterOrDigit(charArray[end])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin, length+1) + "]");
						return;
					}
						
					//System.out.println(new String(charArray, begin, length) + " survived.");
				}
				else {
					//System.out.println(new String(charArray, begin, length) + " survived.(NOT all isLetterOrDigit)");
				}

				if (length > lengthNet[begin]) {
					wordNet[begin] = value;
					lengthNet[begin] = length;
				}
			}
		});

		for (int offset = 0; offset < wordNet.length;) {
			if (wordNet[offset] == null) {
				++offset;
				continue;
			}
			else {
				results.addAll((Set<CasualKeyword>) wordNet[offset]);
				offset += lengthNet[offset];
			}
		}
		return results;
	}
	
	public static Set<CasualKeyword>[] search(Integer tenantId, final char[] charArray, final String dataType, final String restrictToField) {
		final List<Set<CasualKeyword>> results = new ArrayList<Set<CasualKeyword>>();
		AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> trie = loadIfNull(tenantId);
		if (trie == null) return (Set<CasualKeyword>[]) results.toArray(new Set<?>[0]);

		final Object[] wordNet = new Object[charArray.length];
		final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CasualKeyword>>() {
			@Override
			public void hit(int begin, int end, Set<CasualKeyword> value) {
				int length = end - begin;
				
				if (length > charArray.length || begin >= charArray.length) return;
				
				boolean allAscii = true;
				for (int i=begin; i < end; i++) {
					if (!isLetterOrDigit(charArray[i])) {
						//System.out.println("[" + charArray[i] + "] is not isLetterOrDigit");
						allAscii = false;
						break;
					}
				}
				
				if (allAscii) {
					
					if (begin > 0 && isLetterOrDigit(charArray[begin-1])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin-1, length+1) + "]");
						return;
					}
					if (end < charArray.length && isLetterOrDigit(charArray[end])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin, length+1) + "]");
						return;
					}
						
					//System.out.println(new String(charArray, begin, length) + " survived.");
				}
				else {
					//System.out.println(new String(charArray, begin, length) + " survived.(NOT all isLetterOrDigit)");
				}
				
				if (dataType != null) {
					Iterator<CasualKeyword> itr = value.iterator();
					
					while (itr.hasNext()) {
						CasualKeyword c = itr.next();
						
						if (!StringUtils.equalsIgnoreCase(dataType, c.getDataType())) {
							itr.remove();
							continue;
						}
						
						if (restrictToField != null) {
							if (!StringUtils.equalsIgnoreCase(restrictToField, c.getName())) {
								itr.remove();
								continue;
							}
						}
					}
				}
				
				if (value.size() > 0 && length > lengthNet[begin]) {
					wordNet[begin] = value;
					lengthNet[begin] = length;
				}
			}
		});

		for (int offset = 0; offset < wordNet.length;) {
			if (wordNet[offset] == null) {
				++offset;
				continue;
			}
			else {
				results.add((Set<CasualKeyword>) wordNet[offset]);
				offset += lengthNet[offset];
			}
		}
		return (Set<CasualKeyword>[]) results.toArray(new Set<?>[0]);
	}

	public static Set<String> expand(Integer tenantId, final char[] charArray, List<CasualKeyword> nvPairs) {
		final Set<String> results = new HashSet<String>();
		AhoCorasickDoubleArrayTrie<Set<CasualKeyword>> trie = loadIfNull(tenantId);
		if (trie == null) return results;

		final Set<?>[] wordNet = new Set<?>[charArray.length];
		final int[] lengthNet = new int[charArray.length];
		System.out.println("Full charArray:" + new String(charArray));

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CasualKeyword>>() {
			@Override
			public void hit(int begin, int end, Set<CasualKeyword> value) {
				int length = end - begin;
				
				if (length > charArray.length || begin >= charArray.length) return;
				
				boolean allAscii = true;
				for (int i=begin; i < end; i++) {
					if (!isLetterOrDigit(charArray[i])) {
						//System.out.println("[" + charArray[i] + "] is not isLetterOrDigit");
						allAscii = false;
						break;
					}
				}
				
				if (allAscii) {
					
					if (begin > 0 && isLetterOrDigit(charArray[begin-1])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin-1, length+1) + "]");
						return;
					}
					if (end < charArray.length && isLetterOrDigit(charArray[end])) {
						//System.out.println(new String(charArray, begin, length) + " is in [" + new String(charArray, begin, length+1) + "]");
						return;
					}
						
					//System.out.println(new String(charArray, begin, length) + " survived.");
				}
				else {
					//System.out.println(new String(charArray, begin, length) + " survived.(NOT all isLetterOrDigit)");
				}

				if (length > lengthNet[begin]) {
					wordNet[begin] = value;
					lengthNet[begin] = length;
				}
			}
		});

		List<Integer> offsets = new ArrayList<Integer>();

		for (int offset = 0; offset < wordNet.length;) {
			if (wordNet[offset] == null) {
				++offset;
				continue;
			}
			else {
				offsets.add(offset);
				System.out.println(String.format("%d: %s", offset, new String(charArray, offset, lengthNet[offset])));//, wordNet[offset].toString()));
				
				Set<CasualKeyword> cdset = (Set<CasualKeyword>) wordNet[offset];

				for (CasualKeyword cd : cdset) {
					nvPairs.add(cd);
				}
				offset += lengthNet[offset];
			}
		}

		List<Integer> newList = new ArrayList<Integer>(offsets);

		String tmp = new String(charArray);
		recursiveExpand(tmp, wordNet, lengthNet, newList, results);
		System.out.println("results:" + results);

		return results;
	}

	static void recursiveExpand(String orig, Set<?>[] wordNet, int[] lengthNet, List<Integer> offsets,
			Set<String> allAlts) {
		List<String> baseList = new ArrayList<String>();
		baseList.add(orig);

		for (int i = offsets.size() - 1; i >= 0; i--) {
			int offset = offsets.get(i);
			CasualKeyword[] cdset = ((Set<CasualKeyword>) wordNet[offset]).toArray(new CasualKeyword[0]);

			List<String> tmpList = new ArrayList(baseList);

			for (int k = 0; k < cdset.length; k++) {
				CasualKeyword cd = cdset[k];
				String theKey = cd.toInlineKey();

				for (int j = 0; j < tmpList.size(); j++) {
					String tmp = tmpList.get(j);
					tmp = tmp.substring(0, offset) + theKey + tmp.substring(offset + lengthNet[offset]);

					if (k == 0) baseList.set(j, tmp);
					else baseList.add(tmp);
				}

				String thePartialKey = cd.toPartialInlineKey();
				for (int j = 0; j < tmpList.size(); j++) {
					String tmp = tmpList.get(j);
					tmp = tmp.substring(0, offset) + thePartialKey + tmp.substring(offset + lengthNet[offset]);
					baseList.add(tmp);
				}
			}

			if (offsets.size() > 1) {
				List<Integer> newList = new ArrayList<Integer>(offsets);
				newList.remove(i);

				recursiveExpand(orig, wordNet, lengthNet, newList, allAlts);
			}
		}
		allAlts.addAll(baseList);
	}

	/*
	 * public static void main(String[] args) { HibernateUtil.init(); Set<CustomData>[] results =
	 * CustomDataDictionary.search("金山分局的地址在哪".toCharArray());
	 *
	 * for (Set<CustomData> cdset: results) { for (CustomData result: cdset) System.out.println("(" + result.getDataType() +
	 * "). " + result.getName() + ":" + result.getValue()); }
	 *
	 * Set<String> alts = CustomDataDictionary.expand("金山分局的地址在哪".toCharArray(), new ArrayList<CustomData>());
	 *
	 * for (String alt: alts) { System.out.println(alt); } }
	 */

}
