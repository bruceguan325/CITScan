package com.intumit.solr.robot.dictionary;

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
public class CustomDataDictionary extends BaseChineseDictionary {
	static Map<Integer, AhoCorasickDoubleArrayTrie<Set<CustomData>>> trieMap = new HashMap<Integer, AhoCorasickDoubleArrayTrie<Set<CustomData>>>();

	public static void clear(Integer tenantId) {
		trieMap.remove(tenantId);
	}
	
	public static void clearCache(Integer tenantId) {
		File cacheDir = new File(WiSeEnv.getHomePath() + "/dict/customdata/");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		String cachePath = WiSeEnv.getHomePath() + "/dict/customdata/cache." + tenantId + ".forest";
		String cacheValuePath = WiSeEnv.getHomePath() + "/dict/customdata/cache." + tenantId + ".value";
		File cacheFile = new File(cachePath);
		File cacheValueFile = new File(cacheValuePath);
		
		if (cacheFile.exists()) {
			cacheFile.delete();
		}
		if (cacheValueFile.exists()) {
			cacheValueFile.delete();
		}
	}

	public static AhoCorasickDoubleArrayTrie<Set<CustomData>> loadIfNull(Integer tenantId) {
		if (!trieMap.containsKey(tenantId)) {
			AhoCorasickDoubleArrayTrie<Set<CustomData>> trie = new AhoCorasickDoubleArrayTrie<Set<CustomData>>();

			try {
				File cacheDir = new File(WiSeEnv.getHomePath() + "/dict/customdata/");
				if (!cacheDir.exists()) {
					cacheDir.mkdirs();
				}
				String cachePath = WiSeEnv.getHomePath() + "/dict/customdata/cache." + tenantId + ".forest";
				String cacheValuePath = WiSeEnv.getHomePath() + "/dict/customdata/cache." + tenantId + ".value";
				File cacheFile = new File(cachePath);
				File cacheValueFile = new File(cacheValuePath);

				if (cacheFile.exists()) {
					long begin = System.currentTimeMillis();
					trie.load(new ObjectInputStream(new FileInputStream(cacheFile)), new ObjectInputStream(new FileInputStream(cacheValueFile)));
					System.out.println("CustomData trie.load() from cache = " + (System.currentTimeMillis() - begin) + "ms");
				}
				else {
					Tenant t = Tenant.get(tenantId);

					/* 這段 Deprecated, 改用 QAEntity 設定哪些欄位應該要被 load
					Set<String> allowedFns = null; 
					if (StringUtils.isNoneEmpty(t.getAllowedCustomDataFieldNames())) {
						allowedFns = new HashSet<String>(Arrays.asList(StringUtils.split(t.getAllowedCustomDataFieldNames(), ",")));
					}
					*/
					SolrServer server = t.getCoreServer4Write(); // 避免 clean cache 要重 reload 時索引還沒同步
					Set<CustomData> cdSet = CustomData.getAll(t, new HashSet<CustomData>(), server); //, allowedFns);

					if (t.getOpendataCoreUrl() != null) {
						String openUrl = "";
						if(t.getOpendataCoreUrl().contains("wiseadm")){
							openUrl = t.getOpendataCoreUrl().split("wiseadm")[0] + "fieldList?q=*:*";
						}
						if(openUrl.equals("")){
							System.out.println("***");
							System.out.println("OpenData URL Problem!!!");
							System.out.println("***");
						}
						cdSet = CustomData.getAllJson(cdSet, openUrl, null);
					}

					if (cdSet.size() == 0) return null;

					String robotName = RobotFormalAnswers.giveMeRandomAnswer(tenantId, "ROBOT_NAME", MessageUtil.getMessage(Locale.getDefault(), "robot.name"));
					cdSet.add(new CustomData("", "ROBOTNAME", robotName));
					TreeMap<String, Set<CustomData>> map = new TreeMap<String, Set<CustomData>>();
					for (CustomData cd : cdSet) {
						if (map.containsKey(cd.getValue())) map.get(cd.getValue()).add(cd);
						else {
							Set<CustomData> set = new HashSet<CustomData>();
							set.add(cd);
							map.put(cd.getValue(), set);
						}
					}

					long begin = System.currentTimeMillis();
					trie.build(map);
					trie.save(new ObjectOutputStream(new FileOutputStream(cacheFile)));
					trie.saveValue(new ObjectOutputStream(new FileOutputStream(cacheValueFile)));

					System.out.println("CustomData trie.build(map) = " + (System.currentTimeMillis() - begin) + "ms");
				}

				trieMap.put(tenantId, trie);
			}
			catch (Exception e) {
				System.out.println("Error loading from custom data dictionary cache, clear it!");
				e.printStackTrace();
				clearCache(tenantId);
				clear(tenantId);
			}
		}
		return trieMap.get(tenantId);
	}
	
	public static boolean isLetterOrDigit(char c) {
		return Character.isDigit(c) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '/') || (c == '-') || (c == '_');
	}

	public static Set<CustomData>[] search(Integer tenantId, final char[] charArray) {
		final List<Set<CustomData>> results = new ArrayList<Set<CustomData>>();
		AhoCorasickDoubleArrayTrie<Set<CustomData>> trie = loadIfNull(tenantId);
		if (trie == null) return (Set<CustomData>[]) results.toArray(new Set<?>[0]);

		final Object[] wordNet = new Object[charArray.length];
		final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CustomData>>() {
			@Override
			public void hit(int begin, int end, Set<CustomData> value) {
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
				results.add((Set<CustomData>) wordNet[offset]);
				offset += lengthNet[offset];
			}
		}
		return (Set<CustomData>[]) results.toArray(new Set<?>[0]);
	}
	
	public static Set<CustomData>[] search(Integer tenantId, final char[] charArray, final String dataType, final String restrictToField) {
		final List<Set<CustomData>> results = new ArrayList<Set<CustomData>>();
		AhoCorasickDoubleArrayTrie<Set<CustomData>> trie = loadIfNull(tenantId);
		if (trie == null) return (Set<CustomData>[]) results.toArray(new Set<?>[0]);

		final Object[] wordNet = new Object[charArray.length];
		final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CustomData>>() {
			@Override
			public void hit(int begin, int end, Set<CustomData> value) {
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
					Iterator<CustomData> itr = value.iterator();
					
					while (itr.hasNext()) {
						CustomData c = itr.next();
						
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
				results.add((Set<CustomData>) wordNet[offset]);
				offset += lengthNet[offset];
			}
		}
		return (Set<CustomData>[]) results.toArray(new Set<?>[0]);
	}

	public static Set<String> expand(Integer tenantId, final char[] charArray, List<CustomData> nvPairs) {
		final Set<String> results = new HashSet<String>();
		AhoCorasickDoubleArrayTrie<Set<CustomData>> trie = loadIfNull(tenantId);
		if (trie == null) return results;

		final Set<?>[] wordNet = new Set<?>[charArray.length];
		final int[] lengthNet = new int[charArray.length];
		System.out.println("Full charArray:" + new String(charArray));

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CustomData>>() {
			@Override
			public void hit(int begin, int end, Set<CustomData> value) {
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
				
				Set<CustomData> cdset = (Set<CustomData>) wordNet[offset];

				for (CustomData cd : cdset) {
					nvPairs.add(cd);
				}
				offset += lengthNet[offset];
			}
		}

		List<Integer> newList = new ArrayList<Integer>(offsets);

		String tmp = new String(charArray);
		recursiveExpand(tmp, wordNet, lengthNet, newList, results);
		if(results.size() > 50){
			System.out.println("results:" + new ArrayList<String>(results).subList(0, 50) + "..., total size : " + results.size());
		}else{
			System.out.println("results:" + results);
		}

		return results;
	}

	static void recursiveExpand(String orig, Set<?>[] wordNet, int[] lengthNet, List<Integer> offsets,
			Set<String> allAlts) {
		List<String> baseList = new ArrayList<String>();
		baseList.add(orig);

		for (int i = offsets.size() - 1; i >= 0; i--) {
			int offset = offsets.get(i);
			CustomData[] cdset = ((Set<CustomData>) wordNet[offset]).toArray(new CustomData[0]);

			List<String> tmpList = new ArrayList(baseList);

			for (int k = 0; k < cdset.length; k++) {
				CustomData cd = cdset[k];
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
