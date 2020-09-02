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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.model.WikiWordtable;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.util.Constants;
import com.intumit.smartwiki.util.TextUtil;
import com.intumit.smartwiki.util.Util;
import com.intumit.solr.util.WiSeEnv;

public class AhoCorasickFactory {
	static Hashtable<HitHotLocale, AhoCorasick> 
		instances = new Hashtable<HitHotLocale, AhoCorasick>();
	
	static Hashtable<HitHotLocale, InstanceInfo> 
		instanceInfos = new Hashtable<HitHotLocale, InstanceInfo>();
	
	static Logger log = Logger.getLogger(AhoCorasickFactory.class.getName());
	
	static File baseDir = new File(".");
	static {
		String solrHome = System.getProperty("solr.solr.home");
		if (solrHome != null) {
			baseDir = new File(solrHome);
		}
	}
	
	public static class InstanceInfo {
		long instanceCreatedAt;
		long loadTimeCost;
		long counter;
		public InstanceInfo(long instanceCreatedAt, long loadTimeCost,
				long counter) {
			super();
			this.instanceCreatedAt = instanceCreatedAt;
			this.loadTimeCost = loadTimeCost;
			this.counter = counter;
		}
		public long getInstanceCreatedAt() {
			return instanceCreatedAt;
		}
		public long getLoadTimeCost() {
			return loadTimeCost;
		}
		public long getCounter() {
			return counter;
		}
		
	}
	
	
	public static Collection<HitHotLocale> listAvailableInstances() {
		return instances.keySet();
	}
	
	public static InstanceInfo getInstanceInfo(HitHotLocale locale) {
		return (instanceInfos.get(locale));
	}
	
	/**
	 * 每個語言只會有一個 AhoCorasick instance
	 * 如果傳入的語言不存在，或者沒有設定 WikiRankIndex（如果沒 forest cache），就會丟出 Exception
	 * 
	 * TODO 這裡並不 thread safe，目前是靠 InitServlet確保都啟用完成才進入 Server
	 * 
	 * @param locale
	 * @return
	 */
	public static AhoCorasick getInstance(HitHotLocale locale) {
		/* 本來是打算在 wiki server這一端判斷
		   後來發現其實是 rss crawling server那一端判斷比較合理一點點
		   
		if (locale == HitHotLocale.zh_HK)
			locale = HitHotLocale.zh_TW;
		if (locale == HitHotLocale.en_SG)
			locale = HitHotLocale.en_US;
		*/
		if (instances.containsKey(locale))  {
			instanceInfos.get(locale).counter++;
			return instances.get(locale);
		}
		
		AhoCorasick newOne = new AhoCorasick(locale);
		InstanceInfo info = new InstanceInfo(System.currentTimeMillis(), -1, 1);
		instances.put(locale, newOne);
		instanceInfos.put(locale, info);
		info.loadTimeCost = loadTreeFromFile(locale, newOne);
		newOne.prepare();
		
		return newOne;
	}
	
	public static AhoCorasick forceReloadInstance(HitHotLocale locale) {
		if (instances.containsKey(locale)) {
			AhoCorasick newOne = new AhoCorasick(locale);
			InstanceInfo info = instanceInfos.get(locale);
			info.loadTimeCost = loadTreeFromFile(locale, newOne);
			info.instanceCreatedAt = System.currentTimeMillis();
			info.counter = 1;
			//AhoCorasick oldOne = instances.get(locale);
			newOne.prepare();
			instances.put(locale, newOne);
			
			return newOne;
		}
		else {
			return getInstance(locale);
		}
	}
	
	public static boolean removeAhoCorasick(HitHotLocale locale) {
		if (instances.containsKey(locale)) {
			instances.remove(locale);
			instanceInfos.remove(locale);
			return true;
		}
		return false;
	}
	
	public static AhoCorasick forceNewInstance(HitHotLocale locale) {
		AhoCorasick newOne = new AhoCorasick(locale);
		return newOne;
	}

	/**
	 * 從 kernel/forest 當中找尋 cache 檔案（就是把 object serialized）
	 * 
	 * @param locale
	 * @param aho
	 * @return true if there is at least one cache file.
	 */
	protected static long loadTreeFromFile(HitHotLocale locale, AhoCorasick aho) {
		File dir = new File(WiSeEnv.getHomePath(), "dict/forest");
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
			for(File f: dir.listFiles()) {
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
		
		long tc = System.currentTimeMillis() - start;
		
		if (loadFilesCnt > 0){
			log.info("Loading "+locale.getKeyInSolr()+" tree from file successful in " + tc + " ms." );
		} else {
			log.info("No file to load, please re-load from index");
		}
		
		return tc;
	}

	protected static void flushFile(File f, HashMap<String, Integer> data, boolean clearMap) {
		FileOutputStream fo = null;
		ObjectOutputStream oo = null;
		BufferedOutputStream bos = null;

		try {
			log.info("Tring to saving tree to file:" + f.getAbsolutePath());
			fo = new FileOutputStream(f);
			bos = new BufferedOutputStream(fo);
			oo = new ObjectOutputStream(bos);
			oo.writeObject(data);
			oo.flush();
			oo.reset();
			log.info("Saving tree to " + f.getAbsolutePath() + " OK.");
		} catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(oo);
			IOUtils.closeQuietly(bos);
			IOUtils.closeQuietly(fo);
		}

		if (clearMap)
			clearMap(data);
	}

	private static void flushTree(AhoCorasick aho, HashMap<String, Integer> data) {
		for (Map.Entry<String, Integer> entry : data.entrySet()) {
			aho.addString(entry.getKey(), aho.getEntryCount(), entry.getValue()
					.doubleValue());
		}
	}

	private static void clearMap(HashMap<String, Integer> data) {
		data.clear();
		System.gc();
	}
	
	
	static final int FOREST_FILE_SPLIT_SIZE = 400000;
	
	public static StringBuffer statistics() {
		StringBuffer sb = new StringBuffer();
		for (HitHotLocale locale: instances.keySet()) {
			AhoCorasick aho = instances.get(locale);
			sb.append(locale.name() + ":" + aho.getEntryCount());
			sb.append("\n");
		}
		
		return sb;
	}
	 
}
