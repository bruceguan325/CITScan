package com.intumit.solr.robot.wivo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.QADialogConfig;

@Entity
public class WiVoEntry implements Serializable, Cloneable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="channel")
	private String channel;
	
	@Index(name="keyword")
	private String keyword;

	/**
	 * excludes 欄位用逗號分隔關鍵字，關鍵字用來保護特定的詞組不被同音取代，因此這裡通常列出的是會被 WiVo 同音判讀跟 keyword 欄位同音而你又不希望他被替換的那些詞。
	 */
	@Index(name="excludes")
	@Lob
	private String excludes;

	/**
	 * includes 欄位用逗號分隔關鍵字，這裡的關鍵字僅用來做字面上的取代（取代為 keyword），並不會用來做同音的判斷
	 */
	@Index(name="includes")
	@Lob
	private String includes;

	@Index(name="enabled")
	private boolean enabled;

	@Transient
	private Set<String> excludeSet = null;

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
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channelCode) {
		this.channel = StringUtils.trim(channelCode);
	}
	public String getExcludes() {
		return excludes;
	}
	public void setExcludes(String excludes) {
		this.excludes = StringUtils.trim(excludes);
		excludeSet = null;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = StringUtils.trim(keyword);
	}
	public String getIncludes() {
		return includes;
	}
	public void setIncludes(String includes) {
		this.includes = StringUtils.trim(includes);
	}
	public Set<String> getExcludeSet() {
		if (excludeSet == null) {
			excludeSet = new HashSet<String>();
			excludeSet.addAll(Arrays.asList(StringUtils.split(excludes, ",")));
		}
		
		return excludeSet;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	
	@Override
	public String toString() {
		return "WiVoEntry [id=" + id + ", tenantId=" + tenantId
				+ ", channel=" + channel 
				+ ", keyword=" + keyword 
				+ ", excludes=" + excludes 
				+ ", includes=" + includes 
				+ ", enabled=" + enabled + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WiVoEntry other = (WiVoEntry) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	public WiVoEntry getCopy() {
		return getCopy(new WiVoEntry());
	}
	public WiVoEntry getCopy(WiVoEntry copyTo) {
		copyTo.id = id;
		copyTo.channel = channel;
		copyTo.keyword = keyword;
		copyTo.excludes = excludes;
		copyTo.includes = includes;
		copyTo.enabled = enabled;
		return copyTo;
	}
	
	public static synchronized WiVoEntry get(long id) {
		try {
			return (WiVoEntry)HibernateUtil.getSession().get(WiVoEntry.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized WiVoEntry save(Integer tenantId, String channelCode, String keyword, String excludes,String includes, boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WiVoEntry word = new WiVoEntry();
			word.setTenantId(tenantId);
			word.setChannel(channelCode);
			word.setKeyword(keyword);
			word.setExcludes(excludes.toLowerCase());
			word.setIncludes(includes.toLowerCase());
			word.setEnabled(enabled);
			ses.saveOrUpdate(word);
			tx.commit();

			EventCenter.fireEvent(WiVoEntry.class.getName(), tenantId, "reload", null);
			
			return word;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
		
	}
	public static synchronized void update(Integer tenantId, String id, String channelCode, String keyword, String excludes, String includes,
			boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WiVoEntry oldWord = get(lid);
			WiVoEntry newWord = get(lid);

			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(lid);
				newWord.setChannel(channelCode);
				newWord.setKeyword(keyword);
				newWord.setEnabled(enabled);
				newWord.setExcludes(excludes.toLowerCase());
				newWord.setIncludes(includes.toLowerCase());

				ses.update(newWord);
				tx.commit();
				
				EventCenter.fireEvent(WiVoEntry.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void saveOrUpdate(WiVoEntry e) {
		Session ses = null;
		Transaction tx = null;
		try {
			if (e.getId() != null) {
				WiVoEntry oldWord = get(e.getId());

				if (!e.getTenantId().equals(oldWord.getTenantId())) {
					System.out.println("Intrusion detected: tenant[" + e.getTenantId() + "] trying to access wivo data of tenant[" + oldWord.getTenantId() + "]");
					return;
				}
			}
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(e);
			tx.commit();
			
			EventCenter.fireEvent(WiVoEntry.class.getName(), e.getTenantId(), "reload", null);
		} catch (Exception ex) {
			ex.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(Integer tenantId, String id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			long lid = Long.parseLong(id);
			WiVoEntry word = get(lid);

			if (word != null && word.getTenantId().equals(tenantId)) {
				ses.delete(word);
				tx.commit();
				
				EventCenter.fireEvent(WiVoEntry.class.getName(), tenantId, "reload", null);
			}
			else {
				System.out.println("Intrusion detected: tenant[" + tenantId + "] trying to access wivo data of tenant[" + word.getTenantId() + "]");
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized WiVoEntry get(Integer tenantId, String channel, String keyword) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(WiVoEntry.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("keyword"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (channel != null) {
				ct.add(Restrictions.eq("channel", channel));
			}
			
			if (keyword != null) {
				ct.add(Restrictions.eq("keyword", keyword));
			}

			return (WiVoEntry)ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static synchronized WiVoEntry get(Integer tenantId, String sentence) {
		List<WiVoEntry> l = list(tenantId, sentence, true, null);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<WiVoEntry> listAll() {
		return list(null, null, false, null);
	}

	public static List<WiVoEntry> listByTenantId(Integer tenantId) {
		return list(tenantId, null, false, null);
	}

	public static List<WiVoEntry> listByTenantIdAndChannel(Integer tenantId, String channel, boolean includeEmptyChannel) {
		return listByChannel(tenantId, channel, includeEmptyChannel, null);
	}

	public static synchronized List<WiVoEntry> search(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, true, null);
	}

	private static List listByChannel(Integer tenantId, String channel, boolean includeEmptyChannel, Boolean enabled) {
		String searchCategory = StringUtils.lowerCase(channel);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WiVoEntry.class)
					.addOrder(Order.asc("keyword"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (channel != null) {
				if (includeEmptyChannel) {
					ct.add(
						Restrictions.or(
							Restrictions.or(Restrictions.eq("channel", searchCategory), Restrictions.eq("channel", "")),
							Restrictions.isNull("channel"))
						);
				}
				else {
					ct.add(Restrictions.eq("channel", searchCategory));
				}
			}

			if (enabled != null) {
				ct.add(Restrictions.eq("enabled", enabled.booleanValue()));
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

	private static List list(Integer tenantId, String query, boolean fuzzyMatchAllFieldIncludesAndExcludes, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WiVoEntry.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("keyword"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				if (fuzzyMatchAllFieldIncludesAndExcludes) {
					ct.add(Restrictions.or(
							Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE), 
							Restrictions.or(Restrictions.like("includes", searchKeyword, MatchMode.ANYWHERE), Restrictions.like("excludes", searchKeyword, MatchMode.ANYWHERE))
							));
				}
				else {
					ct.add(Restrictions.eq("keyword", query));
				}
			}

			if (enabled != null) {
				ct.add(Restrictions.eq("enabled", enabled.booleanValue()));
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
	
	public static Map<String, WiVoEntry> collToMap(Collection<WiVoEntry> l, List<String> retainThoseChannels) {
		Map<String, WiVoEntry> m = new HashMap<>();

		if (l != null)
		for (WiVoEntry i: l) {
			if (retainThoseChannels != null) {
				if (retainThoseChannels.contains(i.getChannel())) {
					m.put(i.getChannel(), i);
				}
			}
			else {
				m.put(i.getChannel(), i);
			}
		}
		
		return m;
	}

	public static void delete(WiVoEntry entity) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(entity);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
}
