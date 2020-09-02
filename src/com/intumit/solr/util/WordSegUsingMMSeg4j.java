package com.intumit.solr.util;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.SimpleSeg;
import com.chenlb.mmseg4j.Word;

public class WordSegUsingMMSeg4j {
	public static String NAMESPACE_DEFAULT = "default";
	public static String NAMESPACE_DEFAULT_DIRPATH = "/dict/common";
	public static String NAMESPACE_CUSTOMIZE = "custom";
	public static String NAMESPACE_CUSTOMIZE_DIRPATH = "/dict/customize";
	static Hashtable<String, WordSegUsingMMSeg4j> instances = new Hashtable<String, WordSegUsingMMSeg4j>();
	
	public static WordSegUsingMMSeg4j getInstance(String namespace) {
		WordSegUsingMMSeg4j ws = instances.get(namespace);
		
		if (ws == null) {
			if (namespace.equals(NAMESPACE_CUSTOMIZE)) {
				ws = new WordSegUsingMMSeg4j(WiSeEnv.getHomePath() + NAMESPACE_CUSTOMIZE_DIRPATH);
				instances.put(namespace, ws);
			}
			else if (namespace.equals(NAMESPACE_DEFAULT)) {
				ws = new WordSegUsingMMSeg4j(WiSeEnv.getHomePath() + NAMESPACE_DEFAULT_DIRPATH);
				instances.put(namespace, ws);
			}
		}
		if (ws.dic.wordsFileIsChange()) {
			ws.dic.reload();
		}
		
		return ws;
	}
	protected Dictionary dic;
	
	public WordSegUsingMMSeg4j(String dicPath) {
		System.setProperty("mmseg.dic.path", dicPath);	//這裡可以指定自訂詞庫
		dic = Dictionary.getInstance(new File(dicPath));
	}
	
	public WordSegUsingMMSeg4j() {
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
		List<String> list = new ArrayList<String>();
		Reader input = new StringReader(txt);
		Seg seg = getSeg(false);
		MMSeg mmSeg = new MMSeg(input, seg);
		Word word = null;
		boolean mergeStart = false;
		StringBuffer mergeBuf = null;
		
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
		}
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
			WordSegUsingMMSeg4j custSeg = new WordSegUsingMMSeg4j("kernel/dict/customize");
/*			System.out.println(custSeg.segWords("這行文字是要被中文斷詞處理的文章，可以從執行結果看斷詞是否成功 莊圓大師", " | ", true));
			System.out.println(custSeg.segWords("新光三越南西店銷售專員", " | ", true));
			System.out.println(custSeg.segWords("房屋銷售業務", " | ", true));
			System.out.println(custSeg.segWords("悲劇悲劇", " | ", true));
*/			
			System.out.println(custSeg.segWords("中芯國際", " | ", false, true));
			System.out.println(custSeg.segWords("中芯國際半導體台嘰嘰", " | ", true, true));
			System.out.println(custSeg.segWords("Steve_Ballmer", " | ", true, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
