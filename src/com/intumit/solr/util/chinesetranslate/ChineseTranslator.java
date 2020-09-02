package com.intumit.solr.util.chinesetranslate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class ChineseTranslator {

    private static ChineseTranslator instance = new ChineseTranslator();

    private ChineseTranslator() {
        initDictionary();
    }

    public String translateT2S(String s) {
        return Util.translatorT2S(s);
    }

    public String translateS2T(String s) {
        return Util.translatorS2T(s);
    }
    
    public Character translateToTheOther(Character tORs) {
    	if (Constants.CHINESE_T2S_S2T_CHAR_MAP.containsKey(tORs)) {
    		return Constants.CHINESE_T2S_S2T_CHAR_MAP.get(tORs);
    	}
    	return null;
    }

    public static void main(String[] args) throws IOException {
        String s = ChineseTranslator.getInstance().translateT2S(
            "將指定的 tail 自 text 的硬碟結尾移除, 移除的次數由 maxTimes 指定, 0 表示全移除");
        System.out.println(s);
        s = ChineseTranslator.getInstance().translateS2T(s);
        System.out.println(s);
    }

    private static void initDictionary() {
        List words = null;
        Element word = null;

        // 繁體不轉換字
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryNoTransferTW.xml");
            Reader r = new InputStreamReader(is, "UTF8");
            words = (new SAXBuilder().build(r)).getRootElement().getChild(
                "words").getChildren();
            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_NO_CHANGE_TW.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        java.util.Collections.sort(Constants.DICTIONARY_NO_CHANGE_TW);

        // 簡體不轉換字
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryNoTransferCN.xml");
            Reader r = new InputStreamReader(is, "UTF8");
            words = (new SAXBuilder().build(r)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_NO_CHANGE_CN.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
        }
        java.util.Collections.sort(Constants.DICTIONARY_NO_CHANGE_CN);

        // 繁體 to 簡體詞彙
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryT2SPhrase.xml");
            Reader r = new InputStreamReader(is, "UTF8");
            words = (new SAXBuilder().build(r)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_T2S_PHRASE.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
        }
        java.util.Collections.sort(Constants.DICTIONARY_T2S_PHRASE);

        // 簡體 to 繁體詞彙
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryS2TPhrase.xml");
            Reader r = new InputStreamReader(is, "UTF8");
            words = (new SAXBuilder().build(r)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_S2T_PHRASE.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
        }
        java.util.Collections.sort(Constants.DICTIONARY_S2T_PHRASE);

        // 繁體 to 簡體單字
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryT2SWord.xml");
            words = (new SAXBuilder().build(is)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_T2S_WORD.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
        }

        // 簡體 to 繁體單字
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryS2TWord.xml");
            words = (new SAXBuilder().build(is)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                Constants.DICTIONARY_S2T_WORD.add(new NameValuePair(
                    word.getChildText(Constants.DICTIONARY_FROM_TAG),
                    word.getChildText(Constants.DICTIONARY_TO_TAG)));
            }
        }
        catch (Exception e) {
        }

        
        // 給 TokenizerFilter 用 (繁體 to 簡體單字)
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryT2SWord.xml");
            words = (new SAXBuilder().build(is)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                String ft = word.getChildText(Constants.DICTIONARY_FROM_TAG);
                String tt = word.getChildText(Constants.DICTIONARY_TO_TAG);
                
                if (ft.length() > 0 && tt.length() > 0) {
	                Character fc = ft.charAt(0);
	                Character tc = tt.charAt(0);
	                
	                if (!Constants.CHINESE_T2S_S2T_CHAR_MAP.containsKey(fc)) {
	                	Constants.CHINESE_T2S_S2T_CHAR_MAP.put(fc,  tc);
	                }
                }
            }
        }
        catch (Exception e) {
        }
        // 給 TokenizerFilter 用 (簡體 to 繁體單字)
        try {
            InputStream is = ChineseTranslator.class.getResourceAsStream("dictionaryS2TWord.xml");
            words = (new SAXBuilder().build(is)).getRootElement().getChild(
                "words").getChildren();

            for (int i = 0; i < words.size(); i++) {
                word = (Element) words.get(i);
                String ft = word.getChildText(Constants.DICTIONARY_FROM_TAG);
                String tt = word.getChildText(Constants.DICTIONARY_TO_TAG);
                
                if (ft.length() > 0 && tt.length() > 0) {
	                Character fc = ft.charAt(0);
	                Character tc = tt.charAt(0);
	                
	                if (!Constants.CHINESE_T2S_S2T_CHAR_MAP.containsKey(fc)) {
	                	Constants.CHINESE_T2S_S2T_CHAR_MAP.put(fc,  tc);
	                }
                }
            }
        }
        catch (Exception e) {
        }
    }

    public static ChineseTranslator getInstance() {
        return instance;
    }
}
