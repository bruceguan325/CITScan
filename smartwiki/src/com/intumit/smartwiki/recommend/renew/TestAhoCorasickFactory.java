package com.intumit.smartwiki.recommend.renew;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.model.WikiWordtable;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.util.Constants;
import com.intumit.smartwiki.util.TextUtil;

public class TestAhoCorasickFactory {
	
	static String[] zhTW_testdata = new String[] {
		"中華民國及碩網資訊萬歲，我們有了 nbsp; 不少收穫，爆竹，爆炸以及燒金紙，萬歲，萬萬歲！",
		"一九四三，１９４３，1943 巴菲特和工作人員交談，無線wifi的環境，或者是3G網路",
		"圖文abc巴菲特和工作人員交談",
		"圖文：巴菲特和工作人員交談",
		"【4月11日 MODE PRESS】メアリー・ケイト・オルセン（Mary-Kate Olsen）とアシュレー・オルセンRC造（Ashley Olsen）姉妹が、無線LAN新たにネットショッピング・サイトを日本HP立ち上げる。…… ≫続きを読む",
		"Новости Блоги Изображения Видео Медведев на Русском Зато разнос устроил сам Дмитрий Медведев. С одной стороны, объекты саммита, в которые вбуханы гигантские (в основном федеральные) средства, растут. Строятся мосты, гостиницы, дороги, очистные сооружения, новый кампус Дальневосточного федерального университета на острове Русском, где состоятся ...",
		"«Strengt hemmelig» er en film om Karl A. Marthinsen. Filmen er tillitvekkende, og derfor er den sjokkerende.",
	};

	@Before
	public void setUp() throws Exception {
		HibernateUtil.init();
	}

	@After
	public void tearDown() throws Exception {}
	
	//@Test
	public void testTextProcess() {
		for (String text: zhTW_testdata) {
			System.out.println(TextUtil.preprocessText(text));
		}
	}

	//@Test
	public void testZHTW() {

		boolean debug = false;
		
		// "[碩網資訊, 金紙, 爆竹, 中華民國]"
		TestCase.assertEquals("[碩網資訊, 金紙, 爆竹, 中華民國]", quickTest(HitHotLocale.zh_TW, "中華民國及碩網資訊萬歲，我們有了 nbsp; 不少收穫，爆竹，爆炸以及燒金紙，萬歲，萬萬歲！", debug).toString());
		
		// "[巴菲特]"
		TestCase.assertEquals("[巴菲特]", quickTest(HitHotLocale.zh_TW, "圖文：巴菲特和工作人員交談", debug).toString());
		
	}

	class WikiComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			WikiWord w1 = (WikiWord)o1; 
			WikiWord w2 = (WikiWord)o2;
			// TODO Auto-generated method stub
			return w1.getInitScore() >= w2.getInitScore() ? -1 : 1;
		}};
	
	//@Test
	public void testLangNO() {
		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.no_NO);
		try {
			String text = TextUtil.preprocessText("«Strengt hemmelig» er en film om Karl A. Marthinsen. Filmen er tillitvekkende, og derfor er den sjokkerende.");
//			String text = TextUtil.preprocessText("Vegvesenet tok i bruk sitt nye superkamera.");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.no_NO);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.no_NO, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static List<String> quickTest(HitHotLocale locale, String content, boolean debug) {
		AhoCorasick aho = AhoCorasickFactory.getInstance(locale);
		List<String> recommends = new ArrayList<String>();
		try {
			//String text = TextUtil.preprocessText("中華民國及碩網資訊萬歲，我們有了 nbsp; 不少收穫，爆竹，爆炸以及燒金紙，萬歲，萬萬歲！");
			String text = TextUtil.preprocessText(content);
			if (debug) System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
				if (debug) 	System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(locale);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.zh_TW, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand())) {
					recommends.add(wiki.getPageTitle());
					//System.out.println("result2:" + wiki.getPageTitle());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return recommends;
	}
	

	//@Test
	public void testLangJP() {
		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.ja_JP);
		try {
			String text = TextUtil.preprocessText("【4月11日 MODE PRESS】メアリー・ケイト・オルセン（Mary-Kate Olsen）とアシュレー・オルセンRC造（Ashley Olsen）姉妹が、無線LAN新たにネットショッピング・サイトを日本HP立ち上げる。…… ≫続きを読む");
			//String text = TextUtil.preprocessText("経済産業省は、東日本大震災で工場が全壊した中小企業などを対象に、国の補助金を活用して実質的に無利子で融資が受けられる新たな制度を設ける方針を固めました。");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.ja_JP);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.ja_JP, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testLangZHCN() {
		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.zh_CN);
		try {
			String text = TextUtil.preprocessText("据美国媒体报道，当地时间4月18日，阿富汗民众在该国东部帕尔万省举行示威，抗议警方17日晚逮捕当地一名宗教领袖。随后，示威民众与警方发生暴力冲突，造成1人死亡、13人受伤。 帕尔万省警察总监谢尔·艾哈迈德·马拉达尼表示，17日晚一名宗教领袖在省府恰里卡尔被捕致使民众18日举行抗议示威。示威过程中，一名持枪男子突然开枪引发混乱，随后警方无力控制局势。 医院方面透露，一名男子在暴力冲突中丧生，另有13人受伤，其中大部分都是枪杀。 医院一名负责人称，目前他仍能听见医院外的枪击声，并且直升机正在空中巡逻。 （来源：中国日报网 谌融 编辑：杨洁）");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.zh_CN);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.zh_CN, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//@Test
	public void testLangRU() {
		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.ru_RU);
		try {
			String text = TextUtil.preprocessText(
					"Новости Блоги Изображения Видео Медведев на Русском Зато разнос устроил сам Дмитрий Медведев. С одной стороны, объекты саммита, в которые вбуханы гигантские (в основном федеральные) средства, растут. Строятся мосты, гостиницы, дороги, очистные сооружения, новый кампус Дальневосточного федерального университета на острове Русском, где состоятся ...");
			//String text = TextUtil.preprocessText("経済産業省は、東日本大震災で工場が全壊した中小企業などを対象に、国の補助金を活用して実質的に無利子で融資が受けられる新たな制度を設ける方針を固めました。");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.ru_RU);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.ru_RU, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//@Test
	public void testLangIDID() {
		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.id_ID);
		try {
			String text = TextUtil.preprocessText(
					"Gaius Julius Caesar Augustus (23 September 63 SM–19 Agustus 14), yang bergelar Kaisar Oktavianus Augustus atau Kaisar Agustus (bahasa Latin: Imperator Caesar Divi Filivs Avgvtvs), adalah Kaisar Romawi pertama dan salah satu yang paling berpengaruh. Ia mengakhiri perang saudara berkepanjangan dan menciptakan kedamaian, kesejahteraan, dan kemegahan di Kekaisaran Romawi, yang dikenal dengan sebutan Pax Romana atau kedamaian Romawi. Memerintah sebagai penguasa tunggal mulai tahun 27 SM sampai matinya tahun 14 M. Ia menikah dengan Livia Drusilla dan langgeng hingga lebih dari 51 tahun. Setelah mati, Tiberius menggantikannya sebagai kaisar Romawi. Pada zaman pemerintahannya, Yesus Kristus dilahirkan di Betlehem, Provinsi Iudaea[1] dan kemudian bertumbuh di Nazaret, Provinsi Galilea.");
			//String text = TextUtil.preprocessText("経済産業省は、東日本大震災で工場が全壊した中小企業などを対象に、国の補助金を活用して実質的に無利子で融資が受けられる新たな制度を設ける方針を固めました。");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.id_ID);
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            /*for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }*/
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.id_ID, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
