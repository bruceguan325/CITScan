package com.intumit.smartwiki.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intumit.smartwiki.recommend.WikiWord;

public class BlackUtil {

    //CNA
    public static final String[] cnaBlack = { 
	"賭博", "色情", "抗議", "遊行", "政黨", 
	"政治", "活動", "暴動", "走私", "毒品", 
	"街頭運動", "馬英九", "馬總統"
    };
    public static List<WikiWord> 過濾黑名單(List<WikiWord> highlightWordList,
	    String uuid) {
	List<WikiWord> newList = new ArrayList<WikiWord>();
	//CNA
	if (uuid.equals("e3e91837-3124-11dc-a600-9b4ef35943ed")) {
	    List<String> resList = Arrays.asList(cnaBlack);
	    for (WikiWord word : highlightWordList) {
		if (!resList.contains(word.getPageTitle())) {
		    newList.add(word);
		}
	    }
	    return newList;
	} else {
	    return highlightWordList;
	}

    }
}
