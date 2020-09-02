package com.intumit.solr.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.data.Stat;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;


public abstract class ColumnNameMappingFacade {
	static ColumnNameMappingFacade instance = null;
	static Map<String, String> columnMapping = null;
	
	public static ColumnNameMappingFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkColumnNameMappingFacade();
			}
			else {
				instance = new DatabaseColumnNameMappingFacade();
			}
			columnMapping = new HashMap<String, String>();
			instance.reload();
		}
		
		return instance;
	}
	
	public abstract void reload();
	public abstract Map<String, String> getColumnMappingMap();
	public abstract String getMappingName(String fieldName);
	public abstract void saveMap(Map<String, String> map);
	public abstract ColumnNameMapping get(String fieldName);
	public abstract void delete(String fieldName);

	public static class ZkColumnNameMappingFacade extends ColumnNameMappingFacade {
		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/columnnamemapping";
		
		boolean cacheDirty = true;
		NodeCache cache = null;

		public ZkColumnNameMappingFacade() {
			CuratorFramework client = SearchManager.getCuratorClient();
			
			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH, "[]".getBytes());
				}
				
				cache = new NodeCache(client, MYPATH);
				cache.start();
				
				addListener(cache);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	    private void addListener(NodeCache pCache)
	    {
	        // a PathChildrenCacheListener is optional. Here, it's used just to log changes
	    	NodeCacheListener listener = new NodeCacheListener()
	        {
				@Override
				public void nodeChanged() throws Exception {
                    System.out.println("ColumnNameMapping Node dirty, going to reload cache.");
                    cacheDirty = true;
				}
	        };
	        pCache.getListenable().addListener(listener);
	    }
	    
		public void saveJson(String jsonStr) {
			try {
				String dsZkPath = MYPATH;
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				client.setData().forPath(dsZkPath, jsonStr.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void saveMap(Map<String, String> map) {
			try {
				JSONArray jArr = new JSONArray();
				
				for (Map.Entry<String, String> entry: map.entrySet()) {
					JSONObject obj = new JSONObject();
					obj.put("name", entry.getKey());
					obj.put("value", entry.getValue());
					
					jArr.put(obj);
				}
				String dsZkPath = MYPATH;
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				client.setData().forPath(dsZkPath, jArr.toString().getBytes());
				
				cacheDirty = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		


		@Override
		public void reload() {
			try {
				if (columnMapping == null) {
					columnMapping = new HashMap<String, String>();
				}
				cache.rebuild();
				String jsonStr = new String(cache.getCurrentData().getData());
				JSONArray jArr = new JSONArray(jsonStr);
				
				for (int i=0; i < jArr.length(); i++) {
					JSONObject obj = jArr.getJSONObject(i);
					columnMapping.put(obj.getString("name"), obj.getString("value"));
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		@Override
		public void delete(String fieldName) {
			if (columnMapping.containsKey(fieldName)) {
				columnMapping.remove(fieldName);
				saveMap(columnMapping);
			}
		}

		@Override
		public Map<String, String> getColumnMappingMap() {
			try {
				if (cacheDirty) {
					reload();
					cacheDirty = false;
				}
				return columnMapping;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}


		@Override
		public String getMappingName(String fieldName) {
			try {
				if (cacheDirty) {
					reload();
					cacheDirty = false;
				}
				return columnMapping.get(fieldName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		public ColumnNameMapping get(String fieldName) {
			try {
				if (cacheDirty) {
					reload();
					cacheDirty = false;
				}
				return new ColumnNameMapping(fieldName, columnMapping.get(fieldName));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}


		
	}
	
	public static class DatabaseColumnNameMappingFacade extends ColumnNameMappingFacade {

		public Map<String, String> getColumnMappingMap(){
			return columnMapping;
		}
		public String getMappingName(String fieldName){
			if(StringUtils.isEmpty(fieldName)){
				return null;
			}
			return columnMapping.get(fieldName);
		}
		public void saveMap(Map<String, String> map){
			for(String key: map.keySet()){
				if(StringUtils.isEmpty(key)){
					continue;
				}
				String value = map.get(key);
				if(StringUtils.isEmpty(value)){
					delete(key);
				} else {
					save(key,value,false);
				}
			}
			reload();
		}
	
		public ColumnNameMapping get(String fieldName){
			Session ses = null;
			try {
				ses = HibernateUtil.getSession();
				return (ColumnNameMapping)ses.createCriteria(ColumnNameMapping.class).add(Restrictions.eq("columnName", fieldName)).uniqueResult();
			} catch (HibernateException e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
			return null;
		}
		public void delete(String fieldName) {
			Session ses = null;
			Transaction tx = null;
			ColumnNameMapping cn = get(fieldName);
			if(cn==null) return;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.delete(cn);
				tx.commit();
			} catch (HibernateException e) {
				e.printStackTrace();
			} finally {
				ses.close();
				reload();
			}
		}
		public void reload() {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				List result = ses.createCriteria(ColumnNameMapping.class).list();
				columnMapping.clear();
				for(Object o : result){
					ColumnNameMapping cn = (ColumnNameMapping)o;
					columnMapping.put(cn.getColumnName(),cn.getMappingName());
				}
				
				if (columnMapping.size() == 0) {
					columnMapping.put("CATEGORY_ms", "分類");
					columnMapping.put("Eservice_s", "來源");
					columnMapping.put("AnswerType_s", "答案類型");
					columnMapping.put("AnswerCategory_s", "知識分類");
					columnMapping.put("ANSWER_s", "標準答案");
					columnMapping.put("Identity_s", "使用者代號");
					columnMapping.put("KNOWLEDGE_POINT_ms", "知識主題");
					columnMapping.put("MatchedQuestion_s", "問題");
					columnMapping.put("QUESTION_s", "標準問題");
					columnMapping.put("QuestionCategory_ms", "業務類別");
					columnMapping.put("QuestionKeyword_ms", "關鍵字");
					columnMapping.put("KnowledgePoint_ms", "知識主題");
					columnMapping.put("QuestionTag_ms", "問題標籤");
					columnMapping.put("QuestionType_s", "問題歸納");
					columnMapping.put("UserType_s", "用戶類型");
					columnMapping.put("kid_l", "知識編號");
					columnMapping.put("updated_dt", "更新時間");
					columnMapping.put("Status_s", "審核狀態/說明");
					columnMapping.put("Confidence_s", "信度");
					columnMapping.put("Date_dt", "日期");
				}
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
		}
		void save(String fieldName,String mapping, boolean reload){
			ColumnNameMapping cn = new ColumnNameMapping(fieldName,mapping);
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(cn);
				tx.commit();
			} catch (HibernateException e) {
				e.printStackTrace();
			} finally {
				ses.close();
				if(reload)
					reload();
			}
		}
		
		void save(String fieldName,String mapping){
			save(fieldName,mapping,true);
		}
	}

}
