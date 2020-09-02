package com.intumit.solr.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class WordSegFor1111 {
	WordSeg cmSeg = new WordSeg();
	WordSeg custSeg = new WordSeg(WiSeEnv.getHomePath() + "/dict/1111");
	//WordSeg custAllSeg = new WordSeg("./dict/1111_all");
	

	public List<String> segWords(String txt) throws IOException {
		List<String> list1 = custSeg.segWords(txt);
		boolean failed = true;
		for (String w: list1) {
			if (w.length() > 1) {
				failed = false;
				break;
			}
		}
		
		if (failed) {
			list1 = cmSeg.segWords(txt);
		}
		
		return list1;
	}
	
	public static void main(String[] args) {
		try {
			WordSegFor1111 seg = new WordSegFor1111();
			System.out.println(seg.segWords("這行文字是要被中文斷詞處理的文章，可以從執行結果看斷詞是否成功 莊圓大師"));
			System.out.println(seg.segWords("新光三越南西店"));
			System.out.println(seg.segWords("房屋銷售業務"));
			System.out.println(seg.segWords("悲劇悲劇"));
			
			StringWriter sw = new StringWriter();
			JSONObject j = new JSONObject();
			JSONArray a = new JSONArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
