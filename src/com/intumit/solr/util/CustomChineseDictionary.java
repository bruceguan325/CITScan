package com.intumit.solr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;

/**
 * 簡易的自訂辭典斷詞工具
 * 快速、高效
 * 
 * @author herb
 */
public class CustomChineseDictionary extends BaseChineseDictionary
{
    static AhoCorasickDoubleArrayTrie<Object[]> trie = new AhoCorasickDoubleArrayTrie<Object[]>();
    
    static String[] kws = new String[] {
    	"悠遊卡",
    	"捷運悠遊卡",
    	"信用卡",
    	"信用",
    };
    
    public static void load() {
        TreeMap<String, Object[]> map = new TreeMap<String, Object[]>();
        for (int idx=0; idx < kws.length; idx++) {
            map.put(kws[idx], new Object[] {idx, kws[idx]});
        }
        trie.build(map);
    }

    protected static Object[][] search(char[] charArray) {
    	final List<Object[]> results = new ArrayList<Object[]>();
    	
        final Object[][] wordNet = new Object[charArray.length][];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Object[]>()
        {
            @Override
            public void hit(int begin, int end, Object[] value)
            {
                int length = end - begin;
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	results.add(wordNet[offset]);
            	offset += lengthNet[offset];
            }
        }
        return results.toArray(new Object[0][0]);
    }
    
    public static void main(String[] args) {
    	CustomChineseDictionary.load();
    	Object[][] results = CustomChineseDictionary.search("我今天早上用了捷運悠遊卡搭車到新店市公所站".toCharArray());
    	
    	for (Object[] result: results) {
    		System.out.println("(" + result[0] + "). " + result[1]); 
    	}
    }
}
