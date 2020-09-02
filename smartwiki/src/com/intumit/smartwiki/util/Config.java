package com.intumit.smartwiki.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class Config {

	private String lang = "zh";

	public static String DEFAULT_FILE_CHARSET = "MS950";

	private static Config cfg = new Config();

	private String configPath = "./Webapps/WEB-INF/cfg"; // default config
															// path
	private Element configElement;

	public static Config getInstance() {
		return cfg;
	}

	public Element getServerConfigElement() {
		File serverConfigFile = new File(configPath+"/Server.xml");

		try {
			this.configElement = XMLUtil.readXml(serverConfigFile)
					.getRootElement();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.configElement;
	}

	private Config() {
	}

	public void setConfPath(String path) {
		this.configPath = path;
	}

	public void init() {
		initDictionary();
		this.setLang(getServerConfigElement().getChildText("Lang"));
	}

	private void initDictionary() {
		String path = null;
		List words = null;
		Element word = null;

		// �c�餣�ഫ�r
		try {
			path = configPath + "/dictionaryNoTransferTW.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_NO_CHANGE_TW.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(Constants.DICTIONARY_NO_CHANGE_TW);

		// ²�餣�ഫ�r
		try {
			path = configPath + "/dictionaryNoTransferCN.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_NO_CHANGE_CN.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
		}
		java.util.Collections.sort(Constants.DICTIONARY_NO_CHANGE_CN);

		// ²�� to �c���J
		try {
			path = configPath + "/dictionaryS2TPhrase.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_S2T_PHRASE.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
		}
		java.util.Collections.sort(Constants.DICTIONARY_S2T_PHRASE);

		// ²�� to �c���r
		try {
			path = configPath + "/dictionaryS2TWord.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_S2T_WORD.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
		}

		// �c�� to ²���J
		try {
			path = configPath + "/dictionaryT2SPhrase.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_T2S_PHRASE.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
		}
		java.util.Collections.sort(Constants.DICTIONARY_T2S_PHRASE);

		// �c�� to ²���r
		try {
			path = configPath + "/dictionaryT2SWord.xml";
			words = (new SAXBuilder().build(new FileInputStream(path)))
					.getRootElement().getChild("words").getChildren();

			for (int i = 0; i < words.size(); i++) {
				word = (Element) words.get(i);
				Constants.DICTIONARY_T2S_WORD.add(new NameValuePair(word
						.getChildText(Constants.DICTIONARY_FROM_TAG), word
						.getChildText(Constants.DICTIONARY_TO_TAG)));
			}
		} catch (Exception e) {
		}
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
}
