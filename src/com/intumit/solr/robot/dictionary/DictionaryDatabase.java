package com.intumit.solr.robot.dictionary;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import com.hazelcast.core.ITopic;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.robot.dictionary.DictionaryDatabaseChangeEvent.EventType;
import com.intumit.solr.servlet.HazelcastUtil;

@Entity
public class DictionaryDatabase implements Serializable {

	static List<DictionaryDatabaseChangeListener> listeners = new ArrayList<DictionaryDatabaseChangeListener>();
	
	public enum Purpose {
		KNOWLEDGE_POINT("knowledge.point"),
		SEARCH("search.words"),
		DONT_SEARCH("avoid.search"),
		BLACKLIST("blacklist.keyword"),
		MARKETING("marketing.keyword"),
		;

		String desc;
		Purpose(String desc) {
			this.desc = desc;
		}
		public String getDesc() {
			return desc;
		}
		
		public static Purpose getPurpose(String value) {
			String val = value.replace("　", " ").trim();
	        if (val == null || val.length() < 1) {
	            return null;
	        }
	        
	        for (Purpose t : values()) {
	            if (t.name().equalsIgnoreCase(val)) {
	                return t;
	            }
	        }
	        return null;
		}
	}

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="keyword")
	private String keyword;

	@Index(name="purposes")
	private String purposes;

	@Index(name="enabled")
	private boolean enabled;

	@Index(name="category")
	private String category;

	@Index(name="enableQaScopeRestriction")
	private Boolean enableQaScopeRestriction;
	
	@Transient
	private boolean inAudit;
	
	@Transient
	private String passDate;

	public boolean isInAudit() {
		return inAudit;
	}
	
	public void setInAudit(boolean inAudit) {
		this.inAudit = inAudit;
	}
	
	public String getPassDate() {
		return passDate;
	}
	
	public void setPassDate(String passDate) {
		this.passDate = passDate;
	}


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
		this.keyword = StringUtils.trim(keyword);
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = StringUtils.trim(category);
	}
	public String getPurposes() {
		return StringUtils.strip(purposes, ",");
	}
	public JSONArray getPurposesJson() {
		try {
			return new JSONArray(new ArrayList<String>(Arrays.asList(StringUtils.split(purposes, ","))));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public void setPurposes(String purposes) {
		this.purposes = purposes;
	}
	public Set<Purpose> getPurposeSet() {
		Set<Purpose> set = new HashSet<Purpose>();
		for (String pstr: StringUtils.split(purposes, ",")) {
			try {
				Purpose p = Purpose.valueOf(pstr);
				set.add(p);
			}
			catch (Exception ignored) {}
		}
		return set;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	public Boolean getEnableQaScopeRestriction() {
		return enableQaScopeRestriction != null ? enableQaScopeRestriction : Boolean.FALSE;
	}
	public void setEnableQaScopeRestriction(Boolean enableQaScopeRestriction) {
		this.enableQaScopeRestriction = enableQaScopeRestriction;
	}
	
	@Override
	public String toString() {
		return "DictionaryDatabase [" + (id != null ? "id=" + id + ", " : "") + (tenantId != null ? "tenantId=" + tenantId + ", " : "")
				+ (keyword != null ? "keyword=" + keyword + ", " : "") + (purposes != null ? "purposes=" + purposes + ", " : "") + "enabled=" + enabled + ", "
				+ (category != null ? "category=" + category + ", " : "")
				+ (enableQaScopeRestriction != null ? "enableQaScopeRestriction=" + enableQaScopeRestriction : "") + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DictionaryDatabase other = (DictionaryDatabase) obj;
		if (enabled != other.enabled) return false;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (keyword == null) {
			if (other.keyword != null) return false;
		}
		else if (!keyword.equals(other.keyword)) return false;
		if (category == null) {
			if (other.category != null) return false;
		}
		else if (!category.equals(other.category)) return false;
		return true;
	}
	
	public static synchronized void init() {
		ITopic topic = HazelcastUtil.getTopic( "dictionary-database" );
		topic.addMessageListener(new DictionaryDatabaseMessageListener());
		
		addListener(new DictionaryDatabaseChange4CacheReloadListener());
		addListener(new DictionaryDatabaseChange4DictionarySearcherListener());
	}
	
	public static void addListener(DictionaryDatabaseChangeListener l) {
		listeners.add(l);
	}
	
	public static void removeListener(DictionaryDatabaseChangeListener l) {
		listeners.remove(l);
	}
	
	public static synchronized DictionaryDatabase get(long id) {
		Session session = null;
		try {
			session = HibernateUtil.getSession() ;
			return (DictionaryDatabase)session.get(DictionaryDatabase.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(session!=null) {
				session.close();
			}
		}

		return null;
	}

	public static synchronized DictionaryDatabase save(Integer tenantId, String sentence, String purposes,
			boolean enabled, String category, Boolean enableQaScopeRestriction) throws Exception {
		Session ses = null;
		Transaction tx = null;
		DictionaryDatabase word = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			word = new DictionaryDatabase();
			word.setTenantId(tenantId);
			word.setKeyword(sentence.toLowerCase());
			word.setPurposes(purposes);
			word.setEnabled(enabled);
			word.setCategory(category);
			word.setEnableQaScopeRestriction(enableQaScopeRestriction);
			ses.saveOrUpdate(word);
			tx.commit();
			
			try {
				ITopic topic = HazelcastUtil.getTopic( "dictionary-database" );
				topic.publish(new DictionaryDatabaseChangeEvent(DictionaryDatabase.class.getName(), null, word, EventType.SAVE));
			}
			catch (Exception e) {
				HazelcastUtil.log().error("Cannot publish dictionary-database save message", e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
		return word;
	}
	public static synchronized DictionaryDatabase update(Integer tenantId, Long id, String sentence,
			String purposes, boolean enabled, String category, Boolean enableQaScopeRestriction) throws Exception {
		Session ses = null;
		Transaction tx = null;
		DictionaryDatabase word = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DictionaryDatabase oldWord = get(id);
			DictionaryDatabase newWord = get(id);
			
			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(id);
				newWord.setKeyword(sentence.toLowerCase());
				newWord.setPurposes(purposes);
				newWord.setEnabled(enabled);
				newWord.setCategory(category);
				newWord.setEnableQaScopeRestriction(enableQaScopeRestriction);

				ses.update(newWord);
				tx.commit();
				word = newWord;
				try {
					ITopic topic = HazelcastUtil.getTopic( "dictionary-database" );
					topic.publish(new DictionaryDatabaseChangeEvent(DictionaryDatabase.class.getName(), oldWord, newWord, EventType.UPDATE));
				}
				catch (Exception e) {
					HazelcastUtil.log().error("Cannot publish dictionary-database update message", e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
		return word;
	}

	public static synchronized void delete(Integer tenantId, Long id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DictionaryDatabase word = get(id);

			if (word != null) {
				ses.delete(word);
				tx.commit();
				
				try {
					ITopic topic = HazelcastUtil.getTopic( "dictionary-database" );
					topic.publish(new DictionaryDatabaseChangeEvent(DictionaryDatabase.class.getName(), word, null, EventType.DELETE));
				}
				catch (Exception e) {
					HazelcastUtil.log().error("Cannot publish dictionary-database delete message", e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}

	public static synchronized DictionaryDatabase get(Integer tenantId, String sentence) {
		List<DictionaryDatabase> l = list(tenantId, sentence, true, null);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<DictionaryDatabase> listAll() {
		return list(null, null, false, null);
	}

	public static List<DictionaryDatabase> listByTenantId(Integer tenantId) {
		return list(tenantId, null, false, null);
	}

	public static List<DictionaryDatabase> listByTenantIdQaCategory(Integer tenantId, String qaCategory) {
		return listByQaCategory(tenantId, qaCategory, false, true);
	}

	public static synchronized List<DictionaryDatabase> search(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, null);
	}

	private static List listByQaCategory(Integer tenantId, String qaCategory, boolean fullMatch, Boolean enabled) {
		String searchCategory = StringUtils.lowerCase(qaCategory);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DictionaryDatabase.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (qaCategory != null) {
				ct.add(fullMatch
								? Restrictions.eq("category", searchCategory)
								: Restrictions.like("category", searchCategory, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("category", searchCategory)
									: Restrictions.like("category", searchCategory, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
			}
			else {
			}

			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	private static List list(Integer tenantId, String query, boolean fullMatch, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DictionaryDatabase.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(fullMatch
								? Restrictions.eq("keyword", searchKeyword)
								: Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("keyword", searchKeyword)
									: Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
			}
			else {
			}

			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}
	
	public static List<DictionaryDatabase> fullSearchBySQL(int tenantId, String query, AuditStatus status, String purposes) {
		String searchKeyword = query.toLowerCase();
		List result = new ArrayList();
		DictionaryVersionService service = DictionaryVersionService.getInstance();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					
					if (status != null) {
						sql.append(" SELECT s.id, s.keyword, s.purposes, s.category, s.enabled, s.tenantId ");
						sql.append(" FROM DictionaryDatabase AS s ");
						sql.append(" WHERE s.tenantId = ? ");
					} else if (purposes != null) {
						sql.append(" SELECT s.id, s.keyword, s.purposes, s.category, s.enabled, s.tenantId ");
						sql.append(" FROM DictionaryDatabase AS s ");
						sql.append(" WHERE s.tenantId = ? ");
						sql.append(" AND (',' + s.purposes + ',') LIKE ? ");
					} else {
						sql.append(" SELECT s.id, s.keyword, s.purposes, s.category, s.enabled, s.tenantId, v.status, v.passTime ");
						sql.append(" FROM DictionaryDatabase AS s ");
						sql.append(" LEFT JOIN DictionaryDatabaseVersion AS v ");
						sql.append(" ON s.tenantId = v.tenantId and s.id = v.publicId ");
						sql.append(" WHERE s.tenantId = ? AND (v.version is null OR v.version = ");
						
						// 取最新版本，不包含審核中
						sql.append(" (SELECT TOP 1 v.version FROM DictionaryDatabaseVersion AS v ");
						sql.append(" WHERE s.tenantId = v.tenantId AND s.id = v.publicId AND v.passTime IS NOT null order by v.updateTime desc )) ");
						
						sql.append(" AND (CAST(s.id AS CHAR) = ? ");
						sql.append("   	  OR  s.keyword LIKE ? ");
						sql.append("   	  OR  CAST(convert(char(10), v.passTime, 111) AS char) = ? ) ");
					}
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql.toString());
						
						if (status != null) {
							pstmt.setInt(1, tenantId);
						} else if (purposes != null) {
							pstmt.setInt(1, tenantId);
							pstmt.setString(2, "%," + purposes + ",%");
						} else {
							pstmt.setInt(1, tenantId);
							pstmt.setString(2, searchKeyword);
							pstmt.setString(3, "%" + searchKeyword + "%");
							pstmt.setString(4, searchKeyword);	
						}
						
						rs = pstmt.executeQuery();
						while (rs.next()) {
							DictionaryDatabase dic = new DictionaryDatabase();
							dic.setId(rs.getLong("id"));
							dic.setKeyword(rs.getString("keyword"));
							dic.setPurposes(rs.getString("purposes"));
							dic.setCategory(rs.getString("category"));
							dic.setEnabled(rs.getBoolean("enabled"));
							dic.setTenantId(rs.getInt("tenantId"));
							
							if (status == AuditStatus.AUDIT) {
								if(service.publicIdInAudit(dic.getTenantId(), dic.getId()))
									result.add(dic);
							} else if(status == AuditStatus.HISTORY) {
								if(!service.publicIdInAudit(dic.getTenantId(), dic.getId()))
									result.add(dic);
							} else {
								result.add(dic);
							}	
						}
					} finally {
						if (rs != null) {
							rs.close();
						}
						if (pstmt != null) {
							pstmt.close();
						}
					}
				}
			});
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public static Set<String> getDictionary(Integer tenantId, Purpose p) {
		return getFromCache(tenantId, p);
	}

	static Map<Integer, Map<Purpose, Set<String>>> cache = new HashMap<Integer, Map<Purpose, Set<String>>>();

	protected static void clearCache(Integer tenantId) {
		synchronized (cache) {
			if (cache.containsKey(tenantId)) {
				cache.remove(tenantId);
			}
		}
	}
	private static Set<String> getFromCache(Integer tenantId, Purpose p) {
		if (!cache.containsKey(tenantId)) {
			initCache(tenantId);
		}

		return cache.get(tenantId).get(p);
	}
	private synchronized static void initCache(Integer tenantId) {
		if (cache.containsKey(tenantId)) { return; }

		Map<Purpose, Set<String>> ccc = new HashMap<Purpose, Set<String>>();
		for (Purpose p: Purpose.values()) {
			ccc.put(p, new HashSet<String>());
		}

		List<DictionaryDatabase> l = listByTenantId(tenantId);
		for (DictionaryDatabase dd: l) {
			if (dd.isEnabled()) {
				for (Purpose p: dd.getPurposeSet()) {
					ccc.get(p).add(dd.getKeyword());
				}
			}
		}

		cache.put(tenantId, ccc);
	}
}
