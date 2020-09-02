package com.intumit.solr.util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.intumit.android.search.CJKSplitter;
import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.recommend.renew.AhoCorasick;
import com.intumit.smartwiki.recommend.renew.AhoCorasickFactory;
import com.intumit.solr.qparser.SynAndSegQueryUtils;

public class WordSeg {
	public static String NAMESPACE_DEFAULT = "default";
	public static String NAMESPACE_DEFAULT_DIRPATH = "/dict/common";
	public static String NAMESPACE_CUSTOMIZE = "custom";
	public static String NAMESPACE_CUSTOMIZE_DIRPATH = "/dict/customize";
	static Hashtable<String, WordSeg> instances = new Hashtable<String, WordSeg>();
	
	public static WordSeg getInstance(String namespace) {
		WordSeg ws = instances.get(namespace);
		
		if (ws == null) {
			if (namespace.equals(NAMESPACE_CUSTOMIZE)) {
				ws = new WordSeg(WiSeEnv.getHomePath() + NAMESPACE_CUSTOMIZE_DIRPATH);
				instances.put(namespace, ws);
			}
			else if (namespace.equals(NAMESPACE_DEFAULT)) {
				ws = new WordSeg(WiSeEnv.getHomePath() + NAMESPACE_DEFAULT_DIRPATH);
				instances.put(namespace, ws);
			}
		}
		if (ws.dic.wordsFileIsChange()) {
			ws.dic.reload();
		}
		
		return ws;
	}
	protected Dictionary dic;
	private Hashtable<Integer, String> outputMap = new Hashtable<Integer, String>();
	private AhoCorasick aho;
	
	public WordSeg(String dicPath) {
		System.setProperty("mmseg.dic.path", dicPath);	//這裡可以指定自訂詞庫
		dic = Dictionary.getInstance(new File(dicPath));
		
		AhoCorasick aho = AhoCorasickFactory.forceNewInstance(HitHotLocale.zh_TW);
		
		try {
			List<String> lines = FileUtils.readLines(new File(dicPath + "/words.dic"), "UTF-8");
			int i=0;
			for (String line: lines) {
				int id = ++i;
				outputMap.put(id, line);
				aho.addString(line, id);
			}
			
			aho.prepare();
			this.aho = aho;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public WordSeg() {
		this(WiSeEnv.getHomePath() + "/dict/common");
	}

	protected Seg getSeg(boolean maxWord) {
		if (!maxWord) {
			return new ComplexSeg(dic);
		}
		else {
			return new MaxWordSeg(dic);
		}
	}
	
	public List<String> segWords(String txt) throws IOException {
		return segWords(txt, false, false);
	}
	
	public List<String> segWords(String txt, boolean mergeSingleWord, boolean quotePhrase) throws IOException {
		List<String> phraseList = new ArrayList<String>();

		try {
			List<WikiWord> wwlist = aho.getWikiWord(txt);
			Iterator<WikiWord> itr = wwlist.iterator();
			while (itr.hasNext()) {
				WikiWord wword = itr.next();
				phraseList.add(wword.getPageTitle().replaceAll("_", " "));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<String> list = new ArrayList<String>();
		String mytxt = txt;
		for (String phrase: phraseList) {
			int pos = -1;
			while ((pos = StringUtils.indexOfIgnoreCase(mytxt, phrase)) != -1) {
				mytxt = mytxt.substring(0, pos) + "   " + mytxt.substring(pos + phrase.length());
			}
			if (quotePhrase) {
				list.add("\"" + phrase + "\"");
			}
			else {
				list.add(phrase);
			}
		}
		

		CJKSplitter splitter = null;
		
		try {
		  splitter = new CJKSplitter();
			splitter.setConcateCJK(mergeSingleWord);
			splitter.setQuery(mytxt);
			
			while (splitter.next()) {
				list.add(splitter.getToken());
			}
		}
		finally {
		  splitter.close();
		}
		
		
		/*while((word=mmSeg.next())!=null) {
			String w = word.getString();
			
			if (mergeSingleWord && w.length() == 1) {
				if (mergeStart) {
					mergeBuf.append(w);
				}
				else {
					mergeStart = true;
					mergeBuf = new StringBuffer(w);
				}
			}
			else {
				if (mergeStart) {
					mergeStart = false;
					list.add(mergeBuf.toString());
				}
				if (quotePhrase) {
					list.add("\"" + w + "\"");
				}
				else {
					list.add(w);
				}
			}
		}
		if (mergeStart) {
			mergeStart = false;
			list.add(mergeBuf.toString());
		}
		
		if (list.size() <= 0) {
			list.clear();
			seg = getSeg(true);
			input.reset();
			mmSeg = new MMSeg(input, seg);
			word = null;
			while((word=mmSeg.next())!=null) {
				String w = word.getString();
				
				if (mergeSingleWord && w.length() == 1) {
					if (mergeStart) {
						mergeBuf.append(w);
					}
					else {
						mergeStart = true;
						mergeBuf = new StringBuffer(w);
					}
				}
				else {
					if (mergeStart) {
						mergeStart = false;
						list.add(mergeBuf.toString());
					}
					list.add(w);
				}
			}
		}
		if (mergeStart) {
			mergeStart = false;
			list.add(mergeBuf.toString());
		}*/
		return list;
	}
	

	public String segWords(String txt, String wordSpilt) throws IOException {
		return segWords(txt, wordSpilt, false, false);
	}
	public String segWords(String txt, String wordSpilt, boolean mergeSingleWord, boolean quotePhrase) throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		List<String> list = segWords(txt, mergeSingleWord, quotePhrase);
		for (String w: list) {
			if(!first) {
				sb.append(wordSpilt);
			}
			sb.append(w);
			first = false;		
		}
		return sb.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
/*			WordSeg cmSeg = new WordSeg();
			System.out.println(cmSeg.segWords("這行文字是要被中文斷詞處理的文章，可以從執行結果看斷詞是否成功 莊圓大師", " | ", true));
			System.out.println(cmSeg.segWords("新光三越南西店", " | ", true));
			System.out.println(cmSeg.segWords("房屋銷售業務", " | ", true));
			System.out.println(cmSeg.segWords("悲劇悲劇", " | ", true));
			System.out.println(cmSeg.segWords("處方藥劑", " | ", true));
*/			
			WordSeg custSeg = new WordSeg("kernel/dict/customize");
/*			System.out.println(custSeg.segWords("這行文字是要被中文斷詞處理的文章，可以從執行結果看斷詞是否成功 莊圓大師", " | ", true));
			System.out.println(custSeg.segWords("新光三越南西店銷售專員", " | ", true));
			System.out.println(custSeg.segWords("房屋銷售業務", " | ", true));
			System.out.println(custSeg.segWords("悲劇悲劇", " | ", true));
*/			
			System.out.println(custSeg.segWords("中芯國際", " | ", false, true));
			System.out.println(custSeg.segWords("中芯國際半導體台嘰嘰宏碁CEO是個好人蘭奇是混蛋", " | ", true, true));
			System.out.println(custSeg.segWords("steve ballmer is a good man", " | ", false, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
