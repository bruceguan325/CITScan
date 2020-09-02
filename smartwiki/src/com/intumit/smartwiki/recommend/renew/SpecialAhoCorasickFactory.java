package com.intumit.smartwiki.recommend.renew;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.cjk.CJKTokenizer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.config.WikiRankIndex;
import com.intumit.smartwiki.config.WikiRankIndexFacade;
import com.intumit.smartwiki.model.WikiWordtable;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.util.Constants;
import com.intumit.smartwiki.util.TextUtil;
import com.intumit.smartwiki.util.Util;
import com.intumit.solr.SearchManager;

public class SpecialAhoCorasickFactory {
	static Hashtable<HitHotLocale, AhoCorasick> 
		instances = new Hashtable<HitHotLocale, AhoCorasick>();
	
	static Logger log = Logger.getLogger(SpecialAhoCorasickFactory.class.getName());
	
	/**
	 * 每個語言只會有一個 AhoCorasick instance
	 * 如果傳入的語言不存在，或者沒有設定 WikiRankIndex（如果沒 forest cache），就會丟出 Exception
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	 */
	public static AhoCorasick getInstance(HitHotLocale locale, SolrServer server) {
		if (instances.containsKey(locale))  {
			return instances.get(locale);
		}
		
		AhoCorasick newOne = new AhoCorasick(locale);
		instances.put(locale, newOne);
		importData(locale, server, newOne);
		newOne.prepare();
		
		return newOne;
	}

	/**
	 * 從 kernel/forest 當中找尋 cache 檔案（就是把 object serialized）
	 * 
	 * @param locale
	 * @param aho
	 * @return true if there is at least one cache file.
	 */
	protected static boolean loadTreeFromFile(HitHotLocale locale, AhoCorasick aho) {
		File dir = new File("forest_special");
		if(!dir.exists())
			dir.mkdir();
		FileInputStream fin = null; 
		BufferedInputStream bis = null;
		ObjectInputStream oin = null;
		long  start = System.currentTimeMillis();
		HashMap<String, Integer> tmp = null;
		
		log.info("Tring to loading tree from file");
		int loadFilesCnt = 0;
		if(dir.exists() && dir.isDirectory()){
			for(File f: dir.listFiles()){
				if(f.getName().startsWith(locale.getKeyInSolr() + "-")){
					try{
						fin = new FileInputStream(f);
						bis = new BufferedInputStream(fin);
						oin =new ObjectInputStream(bis);
						tmp = (HashMap<String, Integer>) oin.readObject();
						log.info("Loading from "+ f.getName() +" ("+tmp.keySet().size() +")");
						flushTree(aho, tmp);
						clearMap(tmp);
						loadFilesCnt++;
					}catch (IOException e){
						log.log(Level.WARNING, "Error on loading:" + f.getName(),e);
					}catch (Exception e){
						log.log(Level.WARNING, e.getMessage());
					}finally{
						IOUtils.closeQuietly(oin);
						IOUtils.closeQuietly(bis);
						IOUtils.closeQuietly(fin);
					}
				}
			}
		}
		
		if (loadFilesCnt > 0){
			log.info("Loading "+locale.getKeyInSolr()+" tree from file successful in " + (System.currentTimeMillis() - start) + " ms." );
			return true;
		} else {
			log.info("No file to load, re-load from index");
			return false;
		}
	}

	protected static void flushFile(HitHotLocale locale, AhoCorasick aho, int cnt, HashMap<String, Integer>data) {
		flushTree(aho, data);
		File dir = new File("forest_special");
		if(!dir.exists())
			dir.mkdir();
		
		String filename = dir.getPath() + "/" + locale.getKeyInSolr() + "-" + new DecimalFormat("000").format(cnt) + ".tree";
		File f = new File( filename );
		FileOutputStream fo = null;
		ObjectOutputStream oo = null;
		BufferedOutputStream bos = null;
		
		try{
			log.info("Tring to saving tree to file:" + filename );
			fo = new FileOutputStream(f);
			bos = new BufferedOutputStream(fo);
			oo = new ObjectOutputStream(bos);
			oo.writeObject(data);
			oo.flush();
			oo.reset();
			log.info("Saving tree to " + filename + " OK.");
		}catch(Throwable e){
			log.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(oo);
			IOUtils.closeQuietly(bos);
			IOUtils.closeQuietly(fo);
		}
		
		clearMap(data);
	}

	private static void flushTree(AhoCorasick aho, HashMap<String, Integer> data) {
		for (Map.Entry<String, Integer> entry: data.entrySet()){
			aho.addString(entry.getKey(), aho.getEntryCount(), entry.getValue().doubleValue());
		}
	}

	private static void clearMap(HashMap<String, Integer> data) {
		data.clear();
		System.gc();
	}
	
	/*
	 * 在Server一開始的時候，執行此函數 從索引讀資料(全部)，用以建 AhoCorasick Tree
	 */
	public static void importData(HitHotLocale locale, SolrServer server, AhoCorasick aho) {
		if (loadTreeFromFile(locale, aho)) {
			return;
		}
		
		HashMap<String, Integer>tmp = new HashMap<String, Integer>();
		long start = System.currentTimeMillis();
		System.out.println("Build " + locale + " Trie");
		
		try {
			SolrQuery query = new SolrQuery();
			
			int cursor = 0;
			int fileSuffix = 0;
			query.setQuery("*:*");
			query.setRows(50000);
			query.setFields("Name_s");
			
			do {
				query.setStart(cursor);
				QueryResponse response = server.query(query);
				
				SolrDocumentList docList = response.getResults();
				
				for (SolrDocument sdoc: docList) {
					cursor++;
					String fName = (String)sdoc.getFieldValue("Name_s");
					fName = fName.replaceAll(" ", "_");
					
					String newName = (locale == HitHotLocale.zh_TW || locale == HitHotLocale.zh_HK) 
										? Util.translatorS2T(fName)
										: fName;
										
					Integer fScore = 1;//(Integer)sdoc.getFieldValue("Score_i");

					if (TextUtil.filterNew(locale, newName)) {
						tmp.put(newName, fScore);
						
						if (cursor % 50000 == 0) {
							System.out.println(cursor + ":" + newName);
						}
					}
					
				}
				if (docList != null && docList.size() > 0) {
					flushFile(locale, aho, ++fileSuffix, tmp);
				}
				if (docList == null || cursor >= docList.getNumFound() || docList.size() == 0) {
						break;
				}
			} while (true);
			
			log.info("Loading tree from index completed in " + (System.currentTimeMillis() - start) + " ms." );
			// TODO
			// 匯入額外資料庫
			// importNewKeyword(lang);
			
		} 
		catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}

		System.out.println("Approxmate memory usage: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024) + "MB");
		System.out.println("Available memory: " + Runtime.getRuntime().freeMemory() / (1024*1024) + "MB");
		//ready = true;
	}
	
	/*
	private static void testLangNO() {
		AhoCorasick aho = SpecialAhoCorasickFactory.getInstance(HitHotLocale.no_NO);
		try {
			String text = TextUtil.preprocessText("«Strengt hemmelig» er en film om Karl A. Marthinsen. Filmen er tillitvekkende, og derfor er den sjokkerende.");
//			String text = TextUtil.preprocessText("Vegvesenet tok i bruk sitt nye superkamera.");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text, false);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable();
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.no_NO, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}*/

	private static void testLangZH() {
		AhoCorasick aho = SpecialAhoCorasickFactory.getInstance(HitHotLocale.zh_TW, SearchManager.getServer("core0"));
		try {
			String text = TextUtil.preprocessText("92式手枪, 92式军用手枪, 92式除雷車");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable(HitHotLocale.zh_TW);
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
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

/*
	private static void testLangJP() {
		AhoCorasick aho = SpecialAhoCorasickFactory.getInstance(HitHotLocale.ja_JP);
		try {
			String text = TextUtil.preprocessText("【4月11日 MODE PRESS】メアリー・ケイト・オルセン（Mary-Kate Olsen）とアシュレー・オルセンRC造（Ashley Olsen）姉妹が、無線LAN新たにネットショッピング・サイトを日本HP立ち上げる。…… ≫続きを読む");
			//String text = TextUtil.preprocessText("経済産業省は、東日本大震災で工場が全壊した中小企業などを対象に、国の補助金を活用して実質的に無利子で融資が受けられる新たな制度を設ける方針を固めました。");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text, true);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable();
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.ja_JP, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}*/
	
	/*
	private static void testLangZHCN() {
		AhoCorasick aho = SpecialAhoCorasickFactory.getInstance(HitHotLocale.zh_CN);
		try {
			String text = TextUtil.preprocessText("据美国媒体报道，当地时间4月18日，阿富汗民众在该国东部帕尔万省举行示威，抗议警方17日晚逮捕当地一名宗教领袖。随后，示威民众与警方发生暴力冲突，造成1人死亡、13人受伤。 帕尔万省警察总监谢尔·艾哈迈德·马拉达尼表示，17日晚一名宗教领袖在省府恰里卡尔被捕致使民众18日举行抗议示威。示威过程中，一名持枪男子突然开枪引发混乱，随后警方无力控制局势。 医院方面透露，一名男子在暴力冲突中丧生，另有13人受伤，其中大部分都是枪杀。 医院一名负责人称，目前他仍能听见医院外的枪击声，并且直升机正在空中巡逻。 （来源：中国日报网 谌融 编辑：杨洁）");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text, true);
			for (WikiWord wiki : list) {
//				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable();
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }
            
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
	*/


/*
	private static void testLangRU() {
		AhoCorasick aho = SpecialAhoCorasickFactory.getInstance(HitHotLocale.ru_RU);
		try {
			String text = TextUtil.preprocessText(
					"Новости Блоги Изображения Видео Медведев на Русском Зато разнос устроил сам Дмитрий Медведев. С одной стороны, объекты саммита, в которые вбуханы гигантские (в основном федеральные) средства, растут. Строятся мосты, гостиницы, дороги, очистные сооружения, новый кампус Дальневосточного федерального университета на острове Русском, где состоятся ...");
			//String text = TextUtil.preprocessText("経済産業省は、東日本大震災で工場が全壊した中小企業などを対象に、国の補助金を活用して実質的に無利子で融資が受けられる新たな制度を設ける方針を固めました。");
			System.out.println(text);
			List<WikiWord> list = aho.getWikiWord(text, false);
			for (WikiWord wiki : list) {
				System.out.println("result:" + wiki.getPageTitle() + " / " + wiki.getInitScore());
			}
			

            WikiWordtable matchedPatterns = new WikiWordtable();
            for (int i = 0; i < list.size(); i++) {
                matchedPatterns.put(list.get(i).getPageTitle(), list.get(i));
            }

            matchedPatterns = matchedPatterns.filterStopWord(); // 過濾停用字
            
            for (Object pt: matchedPatterns.keySet()) {
            	System.out.println(matchedPatterns.get(pt));
            }
            
            List<WikiWord> wikiWordList = RecommendUtil.calculateScore(
            		HitHotLocale.ru_RU, matchedPatterns);

			for (WikiWord wiki : wikiWordList) {
				if (Constants.RECOMMEND.equals(wiki.getRecommand()))
					System.out.println("result2:" + wiki.getPageTitle());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}*/
	
	public static void main(String args[]) {
//		testLangJP();
		testLangZH();
//		testLangZHCN();
//		testLangNO();
//		testLangRU();

//		AhoCorasick aho = AhoCorasickFactory.getInstance(HitHotLocale.cs_CZ);
	}
	 
}
