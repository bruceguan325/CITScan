package com.intumit.smartwiki.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import com.intumit.android.search.CJKSplitter;
import com.intumit.hithot.HitHotLocale;

public class TextUtil {
	private static boolean blacklist;

	private static String dirBlacklist;

	public static String preprocessText(String text) {
		StringBuffer result = new StringBuffer("");
		
		CJKSplitter splitter = new CJKSplitter();
		
		try {
			splitter.setConcateCJK(true);
			splitter.setQuery(text);
			
			
			
			while (splitter.next()) {
				if (result.length() > 0) {
					result.append("_");
				}
				result.append(splitter.getToken());
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			splitter.close();
		}

		return result.toString();
	}

	public static String toPathFormat(String page_title) {
		StringBuffer str = new StringBuffer();
		str.append("_");
		for (int i = 0; i < page_title.length(); i++) {
			if (i - 1 > 0 && page_title.charAt(i - 1) == '_') // 底線後轉大寫
			str.append(Character.toUpperCase(page_title.charAt(i)));
			else str.append(page_title.charAt(i));
		}
		str.append("_");
		return str.toString();
	}

	private static String blackStrs[] = {};

	private static String zhTWBlackWords[] = { "公斤", "前天", "m67", "啟東", "老中", "方太", "塵埃", "流媒體", "潮来", "昭平", "冷血",
			"接招", "none", "打得火熱", "禮拜二", "台北時間", "台灣時間", "蘇威", "mts", "六度", "月雲", "望都", "胡嘉", "華師", "韓燕", "黃柏", "順其自然",
			"放鴿子", "苦力", "高目", "王郁", "大豐", "永明", "文康", "氣團", "公克", "天全", "天會", "氣團", "玉樹", "立方公尺", "日活", "六處", "前天",
			"說真的", "天正", "張也", "受質", "文應", "Cyc", "費信", "光化", "張榮", "1角", "十架", "十二天", "周一", "周二", "周三", "周四", "周五",
			"周六", "10美分", "5美分", "1美分", "上癮", "星期六", "日營", "凌晨", "13點", "21點", "24點", "天天", "張姓", "許姓", "謝姓", "吳育",
			"天應", "文選", "公頃", "天領", "角都" };

	private static String zhCNBlackWords[] = { "凤凰网", "网易", "东方网", "体坛网", "泡泡网", "中新网", "中华网" };

	private static String jaBlackWords[] = { "afp", "ロイター", "正法", "パーセント", "mhs", "条例", "前日比", "懲役", "日本時間", "パーセント",
			"ため息", "日本海新聞", "マンツーマン", "14歲", "イノベーション", "キロワット時", "市区町村", "情報誌", "はがき", "中国本土", "不作為", "泣きぼくろ", "群像",
			"ア行", "日本海新聞", "さがり", "山形新聞", "山陰中央新報", "NHK", "河北新報", "分数", "城氏", "フジテレビ系", "フジテレビ", "テレビ朝日", "刷る", "8ミリ",
			"秋田魁新報", "今だから", "テレビ東京", "岐阜新聞", "岩手日報", "中国新聞", "毎日放送", "建中", "日テレ", "午前3時", "平方メートル", "ありがとう", "いただきます",
			"凛々", "上方", "キール", "ルノー", "たたき", "インフォシーク", "徳島新聞", "佐賀新聞", "北海道新聞", "業10", "（笑）"

	};

	private static String enBlackWords[] = { "there_is", "there_are", "to_be", "it's", "it_was", "of_the", "that's",
			"not_to", "There_Is", "There_Are", "To_be", "It's", "It_Was", "Of_The", "That's", "Not_To", "CNN",
			"New_York_Daily_News", "Boston_Globe", "Haven’t_Found", "On_It", "Bloomberg", "Don’t_Tell", "91st",
			"As_is", "Look_No_Further", "37th", "Los_Angeles_Times", "This_Is", "IGN", "With_the", "More_than",
			"Associated", "AFP", "Reuters_UK", "CBS", "China_Daily", "AZN", "Financial_Post", "Here’s_How", "And_then",
			"Money_Times", "Brisbane_Times", "Neither_Here_nor_There", "Before_Vanishing", "Leading_the_way",
			"Oneindia", "With_It", "Grand_Forks_Herald", "136th", "NBC_Sports" };

	private static String deBlackWords[] = { "Getty_Images", "Kleinbus", "B2B", "Kanzlerin", "Kleine_Zeitung" };

	private static String frBlackWords[] = { "TF1", "Le_Petit_garçon", "Journal_du_dimanche", "Vingt_ans_aprés",
			"Plongeon", "Serie_A", "Les_cent_pas", "C'est_la_guerre", "Lundi_matin", "Bureau_de_vote",
			"Debut_de_soiree", "France_2", "PDG", "C'est_la_guerre", "Journal_du_dimanche", "Top_14", "Le_Parisien",
			"1er_novembre_1954", "AFP", "M6", "RTL", "En_pleine_tempete", "Weekend", "Petite_robe_noire" };

	private static String itBlackWords[] = { "Adnkronos", "La_Stampa", "Leggo", "Mercoledì", "La_Repubblica",
			"Quotidiano_nazionale", "Agenzia_di_stampa", "800_metri", "41_bis", "ANSA", "Da_A_ad_A", "500_euro" };

	private static String esBlackWords[] = { "EFE", "Arrancada", "Cara_a_cara", "Chispita", "Iguala", "Entrena",
			"Césped", "RTVE", "Antena_3", "GMT", "Anoche", "El_Universal", "El_mejor_momento", "El_Séptimo",
			"Igualada", "Anoche", "Delantero", "Calla", "Cómodo", "Dos_Minutos", "El_Almuerzo", "Esta_mañana",
			"Hasta_mañana", "Buenas_noticias", "Cadena_Ser", "Ansiedad", "Apología", "Caen", "CNN", "Igual_que_ayer",
			"El_engaño", "En_el_día_de_hoy", "RNE", "Euronews", "Frágiles", "Descarado", "El_Jueves", "Euforia",
			"Sonara", "Frente_a_frente" };

	private static String ruBlackWords[] = { "Киеве", "Быстрова", "Балашихи", "Грузии", "Буша", "ВГТРК" };

	private static String arBlackWords[] = { "الجمعة", "السبت", "الخريف", "مايو", "الاثنين", "الثلاثاء", "مارس",
			"سبتمبر", "مايو", "الأهلي", "أول_مايو", "الحديد", "الماء", "وكالة_أنباء", "وكالة_أنباء_الألمانية",
			"العريضة", "الكويتية", "اللبنانية", "الخليجي", "التشاؤم", "وكالة_أنباء", "العراقية", "الاسبانية", "أين",
			"إفريقية", "سورية", "بي_بي_سي", "السلمية", "أنقرة", "الثاني_تشرين", "برس_فرانس", "كويتية", "سوري",
			"نقاط_ثلاث" };

	private static String koBlackWords[] = { "KBS_2TV", "SBS", "MBC", "Mnet", "일요일", "말과", "KBS", "뉴욕 _타임스", "고르고",
			"EBS", "NHK", "연합뉴스", "CBS", "조선중앙방송", "뉴욕_타임스", "CBS_라디오", "HNK", "MBC_라디오", "사람과" };

	private static String fiBlackWords[] = { "MTV3", "STT", "AFP", "Huomenna", "Huuto", "Tunnit", "Elohopea ", "Ihme",
			"Aftonbladet", "Tähän_saakka", "TV1", "Iltalehti", "CNN", "Financial_Times", "Rikki", "Yleisradio",
			"Viikonloppu", "Konna", "Minuutti", "Noita", "TV2", "SAK" };

	private static String nlBlackWords[] = { "ANP", "Oostenrijkse", "Deze_Week", "Eindhovens_Dagblad", "Crunch_Time",
			"Daily_Mail", "El_Pais", "Beho", "TV_Oost", "Vecht", "Nederland_2", "TNT_Post", "CNN", "NBC", "Deze_Week",
			"Belga", "De_Gelderlander", "DPA", "Berlingske_Tidende", "De_Volkskrant", "NRC_Handelsblad",
			"De_Twentsche_Courant_Tubantia", "Radio_1", "ANSA", "Il_Giornal", "AFN", "Nederland_3", "Novum", "Anatolia"

	};

	private static String daBlackWords[] = { "Formiddag", "DR1", "Gaven", "Tæt_på", "Hotellet", "Læk", "24timer",
			"Det_Passer", "Hule", "Beder", "Bunke", "Tomme", "TV_2", "Årtier", "Kontant", "100_meter", "Kasse",
			"Kryds", "Terror", "Gram" };

	private static String ptBlackWords[] = { "AFP", "200_metros", "G1", "Camisa_9", "El_Mundo" };

	private static String svBlackWords[] = { "Onsdag", "SVT", "Vännerna", "Expressen", "TV_4_Plus", "Okej", "SVT1",
			"Pryda", "TV4", "Aktuellt", "Thailändska", "VG", "45_minuter", "Lille", "Dokument_inifrån", "Isländska",
			"Blond", "TV4_Sport", "Canal_Plus", "Lördag", "Förmiddag", "TV6", "Kosta", "Skiter", "Sydsvenskan",
			"På_djupt_vatten", "Under_ytan", "Expressen", "Råg_i_ryggen", "Biljon", "Tall" };

	private static String trBlackWords[] = { "Bu_Akşam", "İl", "Cuma", "Haber_Türk", "Yeni_Şafak", "Hürriyet", "Sabah",
			"Unuttum", "Çarçamba", "Duble", "Zaman_gazetesi", "Saatlet", "CNN", "FX", "Kilogram", "Washington_Post",
			"Korku", "Kutup", "Devler", "Annem", "Pazartesi", "Laciver", "NTV" };

	private static String notImportantWord[] = { "年", "月", "日", "世紀", "월", "일", "أبريل", "년" };

	private static String chineseNumber[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ㄧ", "二", "三",
			"四", "五", "六", "七", "八", "九", "十", "廿" };

	static final String[] months = { "January", "February", "March", "April", "May", "June", "July", "August",
			"September", "October", "November", "December", "gennaio", "febbraio", "marzo", "aprile", "maggio",
			"giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre", "de_enero", "de_febrero",
			"de_marzo", "de_abril", "de_mayo", "de_junio", "de_julio", "de_agosto", "de_septiembre", "de_octubre",
			"de_noviembre", "de_diciembre", "Avril", "Mai", "janvier", "février", "mars", "avril", "mai", "juin",
			"juni", "april", "mei", "december", "maart", "augustus", "juillet", "août", "septembre", "octobre",
			"novembre", "décembre", "мая", "مايو", "أبريل", "اذار", "جمادى_الأول", "آذار", "ابريل", "de_junho",
			"de_maio", "de_abril", "de_março", "de_janeiro", "de_fevereiro", "de_julho", "maj", "september", "juli",
			"oktober", "Eylül", "Mayıs", "Temmuz", "Eylül", "Ağustos", "Ocak" };

	public static boolean filter(String word) {
		// 過濾黑名單
		if (TextUtil.blacklist) {
			if (isBlacklist(word)) return false;
		}

		if (word.equals("……") || word.equals("——")) return false;

		// 過濾空白
		if (word.equals("")) return false;
		// 過濾長度為1
		if (word.length() == 1) return false;
		// 過濾數字3000以下
		if (StringUtils.isNumeric(word)) {
			if (Double.parseDouble(word) <= 3000) return false;
		}

		// 過濾含有月份長度為二的關鍵字,
		for (int i = 0; i < months.length; i++) {
			if (word.equalsIgnoreCase(months[i])) return false;
			if (word.startsWith(months[i] + "_")) return false;
			if (word.endsWith("_" + months[i])) return false;
		}

		// 過濾不重要字
		if (!isImportantWikiWord(word)) return false;
		return true;
	}

	public static boolean filterNew(HitHotLocale locale, String word) {
		// 過濾黑名單
		// if (TextUtil.blacklist) {
		// if (isBlacklist(word))
		// return false;
		// }

		try {
			if (isBlackWords(locale, word)) return false;

			if (word.equals("……") || word.equals("——")) return false;

			// 過濾空白
			if (word.equals("")) return false;
			// 過濾長度為1
			if (word.length() == 1) return false;
			// 過濾數字3000以下
			if (StringUtils.isNumeric(word)) {
				if (Double.parseDouble(word) <= 10000) return false;
			}

			// 過濾含有月份長度為二的關鍵字,
			for (int i = 0; i < months.length; i++) {
				if (word.equalsIgnoreCase(months[i])) return false;
				if (word.startsWith(months[i] + "_")) return false;
				if (word.endsWith("_" + months[i])) return false;
			}

			// 過濾不重要字
			if (!isImportantWikiWord(word)) return false;
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isBlacklist(String word) {
		try {
			List strList = FileUtils.readLines(new File(dirBlacklist), "UTF-8");
			Iterator iterator = strList.iterator();

			while (iterator.hasNext()) {
				String s = (String) iterator.next();
				System.out.println("Blacklist:" + s);
				if (word.equals(s)) {
					return true;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isImportantWikiWord(String word) {
		if (containNotImportantWord(word) && containChineseNumber(word)) return false;
		else return true;
	}

	public static boolean isBlackWords(HitHotLocale locale, String word) {
		switch (locale) {
		case zh_TW:
			blackStrs = zhTWBlackWords;
			break;
		case zh_CN:
			blackStrs = zhCNBlackWords;
			break;
		case ja_JP:
			blackStrs = jaBlackWords;
			break;
		case en_US:
		case en_SG:
			blackStrs = enBlackWords;
			break;
		case de_DE:
			blackStrs = deBlackWords;
			break;
		case es_ES:
			blackStrs = esBlackWords;
			break;
		case fr_FR:
			blackStrs = frBlackWords;
			break;
		case it_IT:
			blackStrs = itBlackWords;
			break;
		case ar_SA:
			blackStrs = arBlackWords;
			break;
		case ru_RU:
			blackStrs = ruBlackWords;
			break;
		case ko_KR:
			blackStrs = koBlackWords;
			break;
		case nl_NL:
			blackStrs = nlBlackWords;
			break;
		case fi_FI:
			blackStrs = fiBlackWords;
			break;
		case pt_PT:
			blackStrs = ptBlackWords;
			break;
		case da_DK:
			blackStrs = daBlackWords;
			break;
		case sv_SE:
			blackStrs = svBlackWords;
			break;
		case tr_TR:
			blackStrs = trBlackWords;
			break;
		default:
			blackStrs = enBlackWords;
			break;
		}
		for (String s : blackStrs) {
			if (StringUtils.equalsIgnoreCase(word, s)) return true;
		}
		return false;
	}

	public static boolean containNotImportantWord(String word) {
		for (String s : notImportantWord) {
			if (StringUtils.contains(word, s)) return true;
		}
		return false;
	}

	public static boolean containChineseNumber(String word) {
		for (String s : chineseNumber) {
			if (StringUtils.contains(word, s)) return true;
		}
		return false;
	}

	public void setDirBlacklist(String dirBlacklist) {
		this.dirBlacklist = dirBlacklist;
	}

	public void setBlacklist(boolean blacklist) {
		this.blacklist = blacklist;
	}

	public static void main(String[] args) {
		System.out.println(preprocessText("Dow Jones"));
		System.out.println(preprocessText("中華民國"));
		System.out
				.println(preprocessText("【4月11日 MODE PRESS】メアリー・ケイト・オルセン（Mary-Kate Olsen）とアシュレー・オルセン（Ashley Olsen）姉妹が、新たにネットショッピング・サイトを立ち上げる。…… ≫続きを読む"));
		System.out
				.println(preprocessText("中華民國萬歲！！！萬萬歲！Currents １２３４５| ５４３２１　９９９ａｂｃGoods: A Floating Coat RackMost coat stands sit on the floor, but the Pox coat rack by Laz Ojalde, a Miami product designer, hangs from the ceiling."));
	}
}
