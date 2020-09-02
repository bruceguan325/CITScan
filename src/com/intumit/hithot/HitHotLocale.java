package com.intumit.hithot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 這個應該都以 HitHot 官網的 CVS 為主，其他的都應該是 copy & paste 過去的
 * 以後這些共用的 source 可能要獨立成為 share project
 */
public enum HitHotLocale {
    en_US("U.S.", "en"), //
    en_UK("U.K.", "enUK"), //2011.04
    en_CA("Canada", "enCA"), //2011.04-2
    en_AU("Australia", "enAU"), //2011.04-2
    en_IN("India (English)", "enIN"), //2011.04
    en_ZA("South Africa","enZA"), //2011.04-2
    zh_CN("简体中文", "zhCN"), //
    zh_TW("繁體中文", "zhTW"), //
    zh_HK("香港中文", "zhHK"), //
    ja_JP("日本語", "ja"), //
    ru_RU("Русский", "ru"), //
    de_DE("Deutsch", "de"), //
    fr_FR("Français", "fr"), //
    es_ES("Español", "es"), //
	es_MX("México", "esMX"), // 2011.04-2
	es_AR("Argentina", "esAR"), //2011.04-2
    pt_PT("Português", "pt"), //
    pt_BR("Brasil", "ptBR"), //2011.04-2
    it_IT("Italiano", "it"), //
    ko_KR("한국의", "ko"), //
    ar_SA("العربية", "ar"),
    id_ID("Bahasa Indonesia", "idID"), //2012.09
    en_ID("Indonesia (English)", "enID"), //2011.04
    en_SG("Singapore", "enSG"), //
    tr_TR("Türkçe", "tr"), //
    pl_PL("Polski", "pl"), //
    hu_HU("Magyar", "hu"), //
    cs_CZ("Čeština", "cs"), //
    nl_NL("Nederlands", "nl"), //
    da_DK("Dansk", "da"), //
    sv_SE("Svenska", "sv"), //
    no_NO("Norsk", "no"), //
    fi_FI("Suomi", "fi"), //
    el_GR("Ελληνικά", "el"), //
    es_PE("Perú","esPE") // 2011.04-2
    ;

	static HashMap<HitHotLocale, String> google_langs = new HashMap<HitHotLocale, String>();
    static HashMap<HitHotLocale, String> wiki_langs = new HashMap<HitHotLocale, String>();
    static HashMap<HitHotLocale, Locale> map_to_java_locales = new HashMap<HitHotLocale, Locale>();
    static HashMap<HitHotLocale, Pattern> month_pattern = new HashMap<HitHotLocale, Pattern>();

    static {
        
        map_to_java_locales.put(zh_CN, Locale.SIMPLIFIED_CHINESE);
        map_to_java_locales.put(zh_TW, Locale.TRADITIONAL_CHINESE);
        map_to_java_locales.put(ja_JP, Locale.JAPANESE);
        map_to_java_locales.put(ko_KR, Locale.KOREAN);
        map_to_java_locales.put(fr_FR, Locale.FRENCH);
        map_to_java_locales.put(it_IT, Locale.ITALIAN);
        map_to_java_locales.put(en_US, Locale.US);
        map_to_java_locales.put(en_UK, Locale.UK); //2011.04
        map_to_java_locales.put(en_CA, Locale.CANADA); //2011.04-2

        
		google_langs.put(ar_SA, "ar");
		google_langs.put(zh_CN, "zh-CN");
		google_langs.put(zh_TW, "zh-TW");
		google_langs.put(cs_CZ, "cs");
		google_langs.put(da_DK, "da");
		google_langs.put(fi_FI, "fi");
		google_langs.put(fr_FR, "fr");
		google_langs.put(el_GR, "el");
		google_langs.put(de_DE, "de");
		google_langs.put(hu_HU, "hu");
		google_langs.put(it_IT, "it");
		google_langs.put(ja_JP, "ja");
		google_langs.put(ko_KR, "ko");
		google_langs.put(nl_NL, "nl");
		google_langs.put(no_NO, "no");
		google_langs.put(pl_PL, "pl");
		google_langs.put(pt_PT, "pt");
		google_langs.put(pt_BR, "pt"); //2011.04-2
		google_langs.put(ru_RU, "ru");
		google_langs.put(es_ES, "es");
		google_langs.put(es_MX, "es"); //2011.04-2
		google_langs.put(es_AR, "es"); //2011.04-2
		google_langs.put(sv_SE, "sv");
		google_langs.put(tr_TR, "tr");
		google_langs.put(en_US, "en");
		google_langs.put(id_ID, "id"); //2011.04
		google_langs.put(en_ID, "en"); //2011.04
		google_langs.put(en_IN, "en"); //2011.04
		google_langs.put(en_UK, "en"); //2011.04
		google_langs.put(en_CA, "en"); //2011.04-2
		google_langs.put(en_AU, "en"); //2011.04-2
		google_langs.put(en_ZA, "en"); //2011.04-2
		google_langs.put(es_PE, "es"); //2011.04-2
		
		wiki_langs.put(ar_SA, "ar");
		wiki_langs.put(zh_CN, "zhCN");
		wiki_langs.put(zh_TW, "zhTW");
		wiki_langs.put(cs_CZ, "cs");
		wiki_langs.put(da_DK, "da");
		wiki_langs.put(fi_FI, "fi");
		wiki_langs.put(fr_FR, "fr");
		wiki_langs.put(el_GR, "el");
		wiki_langs.put(de_DE, "de");
		wiki_langs.put(hu_HU, "hu");
		wiki_langs.put(it_IT, "it");
		wiki_langs.put(ja_JP, "ja");
		wiki_langs.put(ko_KR, "ko");
		wiki_langs.put(nl_NL, "nl");
		wiki_langs.put(no_NO, "no");
		wiki_langs.put(pl_PL, "pl");
		wiki_langs.put(pt_PT, "pt");
		wiki_langs.put(pt_BR, "pt"); //2011.04-2
		wiki_langs.put(ru_RU, "ru");
		wiki_langs.put(es_ES, "es");
		wiki_langs.put(es_MX, "es"); //2011.04-2
		wiki_langs.put(es_AR, "es"); //2011.04-2
		wiki_langs.put(sv_SE, "sv");
		wiki_langs.put(tr_TR, "tr");
		wiki_langs.put(en_US, "en");
		wiki_langs.put(id_ID, "id"); //2012.09
		wiki_langs.put(en_ID, "en"); //2011.04
		wiki_langs.put(en_IN, "en"); //2011.04
		wiki_langs.put(en_UK, "en"); //2011.04
		wiki_langs.put(en_CA, "en"); //2011.04-2
		wiki_langs.put(en_AU, "en"); //2011.04-2
		wiki_langs.put(en_ZA, "en"); //2011.04-2
		wiki_langs.put(es_PE, "es"); //2011.04-2
		
		month_pattern.put(ru_RU, Pattern.compile("(?i)^[0-9]{1,2}\\s(Января|Февраля|Марта|Апреля|мая|Июня|Июля|Августа|Сентября|Октября|октября|Ноября|Декабря)$"));
		month_pattern.put(id_ID, Pattern.compile("(?im)^[0-9]{1,2}\\s(Januari|Februari|Maret|April|Mei|Juni|Juli|Agustus|September|Oktober|November|Desember)$"));
		month_pattern.put(en_US, Pattern.compile("(?i)^[0-9]{1,2}\\s(January|February|March|April|May|June|July|August|September|October|November|December)$"));
    }

    private String text;
    private String keyInSolr;

    private HitHotLocale(String text) {
        this.text = text;
        this.keyInSolr = name();
    }

    private HitHotLocale(String text, String keyInSolr) {
        this.text = text;
        this.keyInSolr = keyInSolr;
    }

    public Locale getLocale() {
        String[] ss = StringUtils.split(name(), '_');
        return new Locale(ss[0], ss[1]);
    }

    /**
     * 用途以及原因都不知道
     * 似乎也沒太多地方用到
     * 造成混淆，以後儘量少用這個 method
     * 
     * @deprecated
     * @return
     */
    public String getUnderlineCode() {
        if (this.equals(zh_CN)) return name();
        if (this.equals(zh_TW)) return name();
        if (this.equals(zh_HK)) return name();
        if (this.equals(en_SG)) return name();

        return keyInSolr;
    }

    public String getText() {
        return text;
    }

    public String getKeyInSolr() {
        return keyInSolr;
    }

    public boolean isCjk() {
        switch (this) {
            case zh_TW :
            case zh_CN :
            case zh_HK :
            case ja_JP :
            case ko_KR :
                return true;
            default :
                return false;
        }
    }
    
    public String getUnicodeRangeRegex() {
        switch (this) {
            case zh_TW :
            case zh_CN :
            case zh_HK :
            case ja_JP :
            case ko_KR :
                return "\\w\\u4e00-\\u9fa5|\\w\\u0800-\\u4e00";
            default :
                return "a-zA-Z_\\-\\s";
        }
    }
    
    public static boolean isLocaleString(String str) {
        try {
            HitHotLocale.valueOf(str);
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }
    
    public static Pattern getMonthPattern(HitHotLocale locale) {
    	if (month_pattern.containsKey(locale))
    		return month_pattern.get(locale);
    	return month_pattern.get(en_US);
    }
    
    /**
     * Deprecated! Only used in WkiUtil1, that is retired class....
     * 
     * Wikipedia language code mapping.
     * 
     * @deprecated
     * @param locale
     * @return
     */
    public static String toWikiLang(HitHotLocale locale) {
    	return wiki_langs.get(locale);
    }
    
    /**
     * Java locales
     * 
     * @param locale
     * @return
     */
    public static Locale toJavaLocale(HitHotLocale locale) {
        return map_to_java_locales.get(locale);
    }
    
    public static HitHotLocale guessHitHotLocaleByJavaLocale(Locale javaLocale) {
    	for (Map.Entry<HitHotLocale, Locale> entry: map_to_java_locales.entrySet()) {
    		if (entry.getValue().equals(javaLocale)) {
    			return entry.getKey();
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Deprecated! Use GoogleNewsUtil.getGoogleNewsCode() instead
     * 
     * The Google News language code mapping.
     * 
     * @param locale
     * @return
     */
    public static String toGoogleLang(HitHotLocale locale) {
    	return google_langs.get(locale);
    }
    
    public static Locale guessJavaLocale(HitHotLocale locale) {
    	String ln = locale.name();
    	if (ln.indexOf("_") > 0) {
    		String firstPart = StringUtils.substringBefore(ln, "_");
    		String secondPart = StringUtils.substringAfter(ln, "_");
    		
    		return new Locale(firstPart, secondPart);
    	}
    	
    	return new Locale(ln);
    }

	public static HitHotLocale guessBestMatchLocale(String language) {
		if (HitHotLocale.isLocaleString(language)) {
			return HitHotLocale.valueOf(language);
		}
		
		String lowerLang = StringUtils.lowerCase(language);
		for (HitHotLocale locale : HitHotLocale.values()) {
			if (StringUtils.lowerCase(locale.name()).startsWith(lowerLang)) {
				return locale;
			}
		}
		
		if (lowerLang.indexOf("_") != -1) {
			String trimLang = StringUtils.substringBefore(lowerLang, "_");

			for (HitHotLocale locale : HitHotLocale.values()) {
				if (StringUtils.lowerCase(locale.name()).startsWith(trimLang)) {
					return locale;
				}
			}
		}
		
		return null;
	}
	
	public static HitHotLocale getByKeyInSolr(String keyInSolr){
		HitHotLocale locale = null;
		for(HitHotLocale l : HitHotLocale.values()){
			if(l.getKeyInSolr().equals(keyInSolr)){
				locale = l;
				break;
			}
		}
		return locale;
	}

	
	public static Locale determineLocale(HttpServletRequest request, boolean setSession, boolean setDefault) {
		String selectedHitHotLocaleStr = StringEscapeUtils.escapeHtml(request.getParameter("selectedLocale"));
		Locale browserLocale = request.getLocale();
		Locale sessionLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
		
		Locale finalLocale = null;

		if (selectedHitHotLocaleStr != null) {
			HitHotLocale hithotLocale = HitHotLocale.valueOf(selectedHitHotLocaleStr);

			if (hithotLocale != null) {
			    finalLocale = hithotLocale.getLocale();
			}
		}
		else if (sessionLocale != null) {
			finalLocale = sessionLocale;
		}
		else if (browserLocale != null) {
			finalLocale = browserLocale;
		}
		else if (Locale.getDefault() != null) {
			finalLocale = Locale.getDefault();
		}
		if (finalLocale == null) {
			finalLocale = Locale.TAIWAN;
		}
		
		if (setSession) {
		    request.getSession().setAttribute("org.apache.struts.action.LOCALE", finalLocale);
		}
		if (setDefault) {
		    Locale.setDefault(finalLocale);
		}
		
		return finalLocale;
	}
}
