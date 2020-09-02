package com.intumit.solr.servlet;

import com.intumit.solr.keywords.WikiKeyword;

public class WikiUtil1 {
	//zhTW
	private static String serviceUrl = "http://79.125.121.167/SmartWiki/services/KeywordService?wsdl";
	private static String uuid = "5ae3c525-3130-11dc-a600-9b4ef35943ed";
	//zhCN
	private static String serviceUrl2 = "http://79.125.121.167/SmartWiki/services/KeywordService?wsdl";
	private static String uuid2 = "6ed44106-2f52-11dc-b356-ed0feb83f71a";
	//en
	private static String serviceUrl3 = "http://swiki-en.intumit.com/SmartWiki/services/KeywordService?wsdl";
	private static String uuid3 = "0578f046-4190-11dc-8f68-0fbb0edc7ba2";
	//ja
	private static String serviceUrl4 = "http://79.125.121.167/SmartWiki/services/KeywordService?wsdl";
	private static String uuid4 = "60dd53d4-3129-11dc-a600-9b4ef35943ed";
	//es
	private static String serviceUrl5 = "http://79.125.13.13/SmartWiki/services/KeywordService?wsdl";
	private static String uuid5 = "459a77be-9083-11de-9e09-adffa9ec8c6b";
	//de
	private static String serviceUrl6 = "http://79.125.13.13:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid6 = "3d24694c-2467-11dc-a550-4353765b2850";
	//ko
	private static String serviceUrl7 = "http://79.125.120.215:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid7 = "362d74df-3122-11dc-a600-9b4ef35943ed";
	//it
	private static String serviceUrl8 = "http://79.125.120.215:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid8 = "d201bac1-3122-11dc-a600-9b4ef35943ed";
	//fr
	private static String serviceUrl9 = "http://79.125.13.13:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid9 = "72a67473-3123-11dc-a600-9b4ef35943ed";
	//ru
	private static String serviceUrl10 = "http://79.125.120.215:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid10 = "ab4d79e4-3123-11dc-a600-9b4ef35943ed";
	//ar
	private static String serviceUrl11 = "http://79.125.120.215:80/SmartWiki/services/KeywordService?wsdl";
	private static String uuid11 = "ae849396-3124-11dc-a600-9b4ef35943ed";
//	//
//	private static String serviceUrl12 = "http://79.125.121.167:80/SmartWiki/services/KeywordService?wsdl";
//	private static String uuid12 = "6ed44106-2f52-11dc-b356-ed0feb83f71a";
	
	
	static {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void addKeyword(WikiKeyword wikiKeyword, String lang) {
        try {

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
	
}
