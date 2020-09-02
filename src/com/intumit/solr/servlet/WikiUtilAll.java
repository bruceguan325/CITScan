package com.intumit.solr.servlet;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.keywords.WikiKeyword;

public class WikiUtilAll {
	
	private static String HIGHLIGHT_API_URL = "http://highlight.hithot.cc";

	public static HitHotLocale getMappedLocale(HitHotLocale locale) {
		switch (locale) {
			case zh_TW:
			case zh_HK:
				return HitHotLocale.zh_TW;
			case en_US:
			case en_SG:
			case en_IN:
			case en_UK:
			case en_ID:
			case en_AU: //2011.04-2
			case en_CA: //2011.04-2
			case en_ZA: //2011.04-2
				return HitHotLocale.en_US;
			case es_ES:
			case es_AR: //2011.04-2
			case es_MX: //2011.04-2
			case es_PE: //2011.04-2
			    return HitHotLocale.es_ES;
			case pt_PT: //2011.05-2
			case pt_BR: //2011.05-2
				return HitHotLocale.pt_PT;
			case zh_CN:
			case ja_JP:
			case no_NO:
			case nl_NL:
			case pl_PL:
			case fi_FI:
			case tr_TR:
			case hu_HU:
			case cs_CZ:
			case de_DE:
			case fr_FR:
			case ko_KR:
			case ru_RU:
			case it_IT:
			// 新增 06/20
			case ar_SA:
			case da_DK:
			case sv_SE:
			case el_GR:
				return locale;
			default:
				return locale;
		}
	}
	
	public static String wiki(String str, HitHotLocale locale, int nums) {
		return getWikiByJson(str, getMappedLocale(locale), nums);
	}
	
	public static boolean isWikiWord(HitHotLocale locale, String keyword) {
		try {
			/*String url = "http://219.84.175.208:8082/wise/isWikiKeywords.jsp?locale=" + getMappedLocale(locale).name() 
				+ "&content=" + URLEncoder.encode(keyword, "UTF-8");*/
			
			if (HIGHLIGHT_API_URL == null) {
				String host = System.getProperty("highlight.host", "highlight.hithot.cc");
				String port = System.getProperty("highlight.port", "80");
				HIGHLIGHT_API_URL = "http://" + host + ":" + port + "";
			}

	        HttpClient c = new HttpClient();
	        PostMethod post = new UTF8PostMethod(HIGHLIGHT_API_URL + "/is");
	        
	        post.setParameter("locale", getMappedLocale(locale).name());
	        post.setParameter("content", keyword);

	        try {
	            if (c.executeMethod(post) == 200){
	            	JSONObject json = new JSONObject(IOUtils.toString(post.getResponseBodyAsStream(), "UTF-8"));
	                JSONArray arr = json.getJSONArray("keywords");

	    			for (int i=0; i < arr.length(); i++) {
	    				JSONObject obj = arr.getJSONObject(i);
	    				String str = obj.getString("keyword");
	    				
	    				if (str != null && str.replaceAll("_", " ").equalsIgnoreCase(keyword)) {
	    					return true;
	    				}
	    			}
	            }
	        }
	        finally {
	        	if (post != null) {
	        		post.releaseConnection();
	        	}
	        }
		} catch (Exception e) {
			System.err.println("WikiUtilAll.isWikiWord() Error:" + e.getMessage());
		}
		
		return false;
		
	}
	
	
	public static void addKeyword(WikiKeyword wikiKeyword) {
	    String locale = wikiKeyword.getLang();
	    String lang = HitHotLocale.valueOf(locale).getKeyInSolr();
        WikiUtil1.addKeyword(wikiKeyword, lang);
    }
	
	private static String getWikiByJson(String str, HitHotLocale locale, int nums) {
		if (HIGHLIGHT_API_URL == null) {
			String host = System.getProperty("highlight.host", "highlight.hithot.cc");
			String port = System.getProperty("highlight.port", "80");
			HIGHLIGHT_API_URL = "http://" + host + ":" + port + "";
		}
		
        HttpClient c = new HttpClient();
        PostMethod post = new UTF8PostMethod(HIGHLIGHT_API_URL + "/hl");
        List<String> wikiKeyword = new ArrayList<String>();
        
        post.setParameter("count", String.valueOf(nums));
        post.setParameter("locale", locale.name());
        // post.setParameter("info", "details");
        post.setParameter("content", str);
        
        try {
            if (c.executeMethod(post) == 200){
                String json = IOUtils.toString(post.getResponseBodyAsStream(), "UTF-8");
                //System.out.println(json);
                JSONArray array = new JSONArray(new JSONObject(json).getString("keywords"));
                //System.out.println(array);
                
                for (int i = 0; i < array.length(); i++) {
                    if (array.getString(i).startsWith(".")) continue;
                    wikiKeyword.add(array.getString(i));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	if (post != null) {
        		post.releaseConnection();
        	}
        }

	    return StringUtils.join(wikiKeyword,",");
	    
	}
}
class UTF8PostMethod extends PostMethod {
    public UTF8PostMethod(String url) {
        super(url);
    }

    @Override
    public String getRequestCharSet() {
        // return super.getRequestCharSet(); 
        return "UTF-8";
    }
}
