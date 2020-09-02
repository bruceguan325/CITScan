package com.intumit.smartwiki.util;

public enum LanguageCoutry {

	en_US("English", "en"), zh_TW("繁體中文", "zh_TW"), zh_CN("简体中文", "zh_CN"), ja_JP(
			"日本語", "ja"), de_DE("Deutsch", "de"), it_IT("Italiano", "it"), ru_RU(
			"Русский", "ru"), fr_FR("Français", "fr"), ko_KR("한국의", "ko"), es_ES(
			"Español", "es"), pt_PT("Português", "pt"), nl_NL("Nederlands",
			"nl"), sv_SE("Svenska", "sv"), no_NO("Norsk", "no"), fi_FI("Suomi",
			"fi"), pl_PL("Polski", "pl"), cs_CZ("Čeština", "cs"), hu_HU(
			"Magyar", "hu"), tr_TR("Türkçe", "tr"), el_GR("Ελληνικά", "el"), da_DK(
			"Dansk", "da"), ar_SA("العربية", "ar");
	
	String lang;
	String langCode;

	public String getLang() {
		return lang;
	}

	public String getLangCode() {
		return langCode;
	}

	private LanguageCoutry(String lang, String langCode) {
		this.lang = lang;
		this.langCode = langCode;
	}
	public static void main(String[] args) {
		System.out.println(LanguageCoutry.en_US);
	}

}
