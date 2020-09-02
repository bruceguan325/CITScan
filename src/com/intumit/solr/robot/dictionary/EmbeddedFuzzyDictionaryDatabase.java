package com.intumit.solr.robot.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

import org.elasticsearch.common.lang3.StringUtils;

import com.hazelcast.core.ITopic;
import com.intumit.solr.robot.dictionary.EmbeddedFuzzyDictionaryDatabaseChangeEvent.EventType;
import com.intumit.solr.servlet.HazelcastUtil;

@Entity
public class EmbeddedFuzzyDictionaryDatabase implements Serializable {
	
	private static final long serialVersionUID = -5024115874542677876L;

	static List<EmbeddedFuzzyDictionaryDatabaseChangeListener> listeners = new ArrayList<EmbeddedFuzzyDictionaryDatabaseChangeListener>();
	
	private Long id;

	private Integer tenantId;

	private String keyword;
	
	private String type;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public static synchronized void init() {
		ITopic topic = HazelcastUtil.getTopic( "embedded-fuzzy-dictionary-database" );
		topic.addMessageListener(new EmbeddedFuzzyDictionaryDatabaseMessageListener());
		
		addListener(new EmbeddedFuzzyDictionaryDatabaseChange4CacheReloadListener());
		addListener(new EmbeddedFuzzyDictionaryDatabaseChange4DictionarySearcherListener());
	}
	
	public static void addListener(EmbeddedFuzzyDictionaryDatabaseChangeListener l) {
		listeners.add(l);
	}
	
	public static void removeListener(EmbeddedFuzzyDictionaryDatabaseChangeListener l) {
		listeners.remove(l);
	}

	public static Map<String, Set<EmbeddedFuzzyDictionaryDatabase>> getEmbeddedFuzzyDictionary(Integer tenantId) {
		return getFromCache(tenantId);
	}
	
	public static synchronized EmbeddedFuzzyDictionaryDatabase get(Integer tenantId, long id) {
		return FuzzyCoreUtils.getInstance(tenantId).get(id);
	}
	
	public static synchronized EmbeddedFuzzyDictionaryDatabase get(Integer tenantId, String keyword, String type) {
		return FuzzyCoreUtils.getInstance(tenantId).get(keyword, type);
	}

	public static synchronized void save(Integer tenantId, Long id, String keyword, String type) {
		if(StringUtils.isBlank(keyword) || StringUtils.isBlank(type)) return;
		EmbeddedFuzzyDictionaryDatabase oldEfd = get(tenantId, keyword, type);
		if(oldEfd != null) {
			update(tenantId, oldEfd.getId(), keyword, type);
			return;
		}
		try {
			EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
			if(id == null) {
				id = FuzzyCoreUtils.getInstance(tenantId).getMaxId() + 1;
			}
			efd.setId(id);
			efd.setTenantId(tenantId);
			efd.setKeyword(keyword.toLowerCase());
			efd.setType(type.toUpperCase());
			FuzzyCoreUtils.getInstance(tenantId).add(efd);
			
			try {
				ITopic topic = HazelcastUtil.getTopic( "embedded-fuzzy-dictionary-database" );
				topic.publish(new EmbeddedFuzzyDictionaryDatabaseChangeEvent(EmbeddedFuzzyDictionaryDatabase.class.getName(), EventType.SAVE, efd, null));
			}
			catch (Exception e) {
				HazelcastUtil.log().error("Cannot publish embedded-fuzzy-dictionary-database save message", e);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static synchronized void delete(Integer tenantId, String id) throws Exception {
		try {
			long lid = Long.parseLong(id);
			EmbeddedFuzzyDictionaryDatabase efd = get(tenantId, lid);
			if (efd != null) {
				FuzzyCoreUtils.getInstance(tenantId).delete(id);
				try {
					ITopic topic = HazelcastUtil.getTopic( "embedded-fuzzy-dictionary-database" );
					topic.publish(new EmbeddedFuzzyDictionaryDatabaseChangeEvent(EmbeddedFuzzyDictionaryDatabase.class.getName(), EventType.DELETE, efd, null));
				}
				catch (Exception e) {
					HazelcastUtil.log().error("Cannot publish embedded-fuzzy-dictionary-database delete message", e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static Map<Integer, Map<String, Set<EmbeddedFuzzyDictionaryDatabase>>> cache = new HashMap<Integer, Map<String, Set<EmbeddedFuzzyDictionaryDatabase>>>();

	protected static void clearCache(Integer tenantId) {
		synchronized (cache) {
			if (cache.containsKey(tenantId)) {
				cache.remove(tenantId);
			}
		}
	}
	
	private static Map<String, Set<EmbeddedFuzzyDictionaryDatabase>> getFromCache(Integer tenantId) {
		if (!cache.containsKey(tenantId)) {
			initCache(tenantId);
		}
		return cache.get(tenantId);
	}
	
	private synchronized static void initCache(Integer tenantId) {
		
		if (cache.containsKey(tenantId)) { return; }

		Map<String, Set<EmbeddedFuzzyDictionaryDatabase>> tenantEfdMap = new HashMap<String, Set<EmbeddedFuzzyDictionaryDatabase>>();

		List<EmbeddedFuzzyDictionaryDatabase> efdList = listByTenantId(tenantId);
		for (EmbeddedFuzzyDictionaryDatabase efd: efdList) {
			if(!tenantEfdMap.containsKey(efd.getType())) {
				tenantEfdMap.put(efd.getType(), new HashSet<EmbeddedFuzzyDictionaryDatabase>());
			}
			tenantEfdMap.get(efd.getType()).add(efd);
		}

		cache.put(tenantId, tenantEfdMap);
	}

	public static List<EmbeddedFuzzyDictionaryDatabase> listByTenantId(Integer tenantId) {
		return FuzzyCoreUtils.getInstance(tenantId).listAll();
	}

	public static List<EmbeddedFuzzyDictionaryDatabase> search(Integer tenantId, String searchKeyword, String searchType) {
		return FuzzyCoreUtils.getInstance(tenantId).search(searchKeyword, searchType);
	}

	public static void update(Integer tenantId, Long id, String keyword, String type) {
		if(StringUtils.isBlank(keyword) || StringUtils.isBlank(type)) return;
		try {
			EmbeddedFuzzyDictionaryDatabase oldEfd = get(tenantId, id);
			EmbeddedFuzzyDictionaryDatabase newEfd = get(tenantId, id);
			newEfd.setId(id);
			newEfd.setKeyword(keyword);
			newEfd.setType(type);
			FuzzyCoreUtils.getInstance(tenantId).update(oldEfd, newEfd);
			try {
				ITopic topic = HazelcastUtil.getTopic( "embedded-fuzzy-dictionary-database" );
				topic.publish(new EmbeddedFuzzyDictionaryDatabaseChangeEvent(EmbeddedFuzzyDictionaryDatabase.class.getName(), EventType.UPDATE, newEfd, oldEfd));
			}
			catch (Exception e) {
				HazelcastUtil.log().error("Cannot publish dictionary-database update message", e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
