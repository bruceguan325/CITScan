package com.intumit.solr.robot;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;

import com.hazelcast.core.ITopic;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;
import com.intumit.solr.robot.dictionary.DictionaryDatabaseChangeEvent.EventType;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

@Entity
public class SegmentBatchTask implements Serializable {
	
	public static enum Status {
		WAIT, REJECT, DONE
	}

	public static enum Purpose {
		SYNONYM_KEYWORD_CHANGED("synonym.keyword.changed"),
		MANUAL_ADD("manual.new.add"),
		;

		String desc;
		Purpose(String desc) {
			this.desc = desc;
		}
		public String getDesc() {
			return desc;
		}
	}

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="keyword")
	private String keyword;

	@Lob
	private String toBeMerged;

	@Index(name="renameTo")
	private String renameTo;

	@Enumerated(EnumType.STRING)
	@Index(name="purpose")
	private Purpose purpose;
	
	@Enumerated(EnumType.STRING)
	@Index(name="status")
	private Status status;

	@Index(name="source")
	private String source;

	@Lob
	private String processedKids;
	
	@Lob
	private String log;

	@Index(name="tsIdx")
	Date entryTimestamp;

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
	public String getSource() {
		return source;
	}
	public void setSource(String category) {
		this.source = category;
	}
	public String getToBeMerged() {
		return toBeMerged;
	}
	public void setToBeMerged(String toBeMerged) {
		this.toBeMerged = toBeMerged;
	}
	public String getRenameTo() {
		return renameTo;
	}
	public void setRenameTo(String renameTo) {
		this.renameTo = renameTo;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public Purpose getPurpose() {
		return purpose;
	}
	public void setPurpose(Purpose purpose) {
		this.purpose = purpose;
	}
	public Date getEntryTimestamp() {
		return entryTimestamp;
	}
	public void setEntryTimestamp(Date entryTimestamp) {
		this.entryTimestamp = entryTimestamp;
	}
	public String getProcessedKids() {
		return processedKids != null ? processedKids : "";
	}
	public void setProcessedKids(String processedKids) {
		this.processedKids = processedKids;
	}
	public void addProcessedKid(Long kid) {
		String s = getProcessedKids();
		if (s.length() > 0) {
			s += ",";
		}
		s += "" + kid;
		setProcessedKids(s);
	}
	public String getLog() {
		return log != null ? log : "";
	}
	public void appendLog(AdminUser user, String toBeAppend) {
		log = getLog();
		if (log.length() > 0) {
			log += "\n";
		}
		
		String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS").format(Calendar.getInstance().getTime());
		log += "[" + ts + "] ";
		if (user != null) {
			log += user.getName() + "(" + user.getId() + "):";
		}
		log += toBeAppend;
	}
	@Override
	public String toString() {
		return "SegmentBatchTask [id=" + id + ", tenantId=" + tenantId + ", keyword=" + keyword + ", toBeMerged="
				+ toBeMerged + ", renameTo=" + renameTo + ", purpose=" + purpose + ", status=" + status + ", source="
				+ source + ", entryTimestamp=" + entryTimestamp + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryTimestamp == null) ? 0 : entryTimestamp.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
		result = prime * result + ((purpose == null) ? 0 : purpose.hashCode());
		result = prime * result + ((renameTo == null) ? 0 : renameTo.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
		result = prime * result + ((toBeMerged == null) ? 0 : toBeMerged.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SegmentBatchTask other = (SegmentBatchTask) obj;
		if (entryTimestamp == null) {
			if (other.entryTimestamp != null) return false;
		}
		else if (!entryTimestamp.equals(other.entryTimestamp)) return false;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (keyword == null) {
			if (other.keyword != null) return false;
		}
		else if (!keyword.equals(other.keyword)) return false;
		if (purpose != other.purpose) return false;
		if (renameTo == null) {
			if (other.renameTo != null) return false;
		}
		else if (!renameTo.equals(other.renameTo)) return false;
		if (source == null) {
			if (other.source != null) return false;
		}
		else if (!source.equals(other.source)) return false;
		if (status != other.status) return false;
		if (tenantId == null) {
			if (other.tenantId != null) return false;
		}
		else if (!tenantId.equals(other.tenantId)) return false;
		if (toBeMerged == null) {
			if (other.toBeMerged != null) return false;
		}
		else if (!toBeMerged.equals(other.toBeMerged)) return false;
		return true;
	}
	
	@Transient
	public boolean isRename() {
        return StringUtils.isNotEmpty(getRenameTo());
	}
	
	@Transient
	public boolean isMerge() {
		boolean doMerge = false;

        if (StringUtils.isNotEmpty(getToBeMerged())) {
            	for (String kw: StringUtils.split(getToBeMerged(), ",")) {
            		if (StringUtils.isNotEmpty(kw)) {
            			doMerge = true;
            		}
            	}
        }
        return doMerge;
	}
	
	public String toQuery() {
        StringBuilder buf = new StringBuilder();
        buf.append(WiSeUtils.dblQuote(getKeyword()));
        
        if (isRename()) {
	        	if (buf.length() > 0) buf.append(" ");
	        	buf.append(WiSeUtils.dblQuote(getRenameTo()));
        }
        
        if (isMerge()) {
	        	for (String kw: StringUtils.split(getToBeMerged(), ",")) {
	        		if (StringUtils.isNotEmpty(kw)) {
	        			if (buf.length() > 0) buf.append(" ");
	        			buf.append("*" + kw.replaceAll(" ", "_") + "*");
	        		}
	        	}
        }
        
        return buf.toString();
	}
	
	public Set<String> toKeywordSet() {
		Set<String> tkws = new HashSet<>();
		tkws.add(getKeyword());
		
        if (isRename()) {
            	tkws.add(getRenameTo());
        }
        
        if (isMerge()) {
            	for (String kw: StringUtils.split(getToBeMerged(), ",")) {
            		if (StringUtils.isNotEmpty(kw)) {
            			tkws.add(kw);
            		}
            	}
        }
        
        return tkws;
	}
	/**
	 * 預先掃過所有可能的問答跟所有範本（這裡在範本部分可能會因為範本非常多有潛在的 performance issue，因為他要一個一個掃過所有的範本）
	 * 看看有沒有可能異動的（Aggressive, 而且一旦有就直接 return true，不會等到全部掃過）
	 * @return
	 */
	public boolean hasPossibleChange() {
		try {
			Tenant tenant = Tenant.get(tenantId);
			QAUtil qautil = QAUtil.getInstance(tenant);
			// 這裡只撈前 10000 個可能的問句（如果跟本同義詞相關的超過10000個問答就有可能處理不了）
	        SolrDocumentList docs = qautil.lowLevelSearchCommonSense("QUESTION_ALT_TPL_ms", "(" + toQuery() + ")", 1, 10000);
	        Set<String> tkws = toKeywordSet();
	        
	        for (SolrDocument doc: docs) {
				QA qa = new QA(doc);
				List<String> altTpls = qa.getQuestionAltTemplates();
				boolean suspicion = false;
				boolean dirty = false;
				
				for (String altTplRow: altTpls) {
					String altTpl = QA.parseAndGetQAAltOnly(altTplRow);
					Sentence s = new Sentence(altTpl);
					boolean toBeCheck = false;
					
					for (Sentence.Block b: s.getBlocks()) {
						if (b.isInParentheses()) {
							if (CollectionUtils.containsAny(b.getTerms(), tkws)) {
								toBeCheck = true;
							}
						}
					}
					
					if (toBeCheck) {
						suspicion = true;
						String newAltTpl = processMergeAndReplace(altTpl, tkws, true);
						
						// 只是為了呈現
						if (!StringUtils.equals(altTpl, newAltTpl)) {
							dirty = true;
						}
					}
					
					if (dirty)
						return true;
				}
	        }
	
	        List<QAAltTemplate> tpls = QAAltTemplate.list(tenant.getId());
			
	        for (QAAltTemplate tpl: tpls) {
				List<String> altTpls = QAUtil.parseMultiValue(tpl.getTemplate());
				boolean suspicion = false;
				boolean dirty = false;
			
				if (altTpls != null)
				for (String altTplRow: altTpls) {
					String altTpl = QA.parseAndGetQAAltOnly(altTplRow);
					Sentence s = new Sentence(altTpl);
					boolean toBeCheck = false;
					
					for (Sentence.Block b: s.getBlocks()) {
						if (b.isInParentheses()) {
							if (CollectionUtils.containsAny(b.getTerms(), tkws)) {
								toBeCheck = true;
							}
						}
					}
					
					if (toBeCheck) {
						suspicion = true;
						String newAltTpl = processMergeAndReplace(altTpl, tkws, true);
						
						// 只是為了呈現
						if (!StringUtils.equals(altTpl, newAltTpl)) {
							dirty = true;
						}
					}
					
					if (dirty)
						return true;
				}
	        }
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
        
        return false;
	}
	
	public String processMergeAndReplace(String altTpl, Set<String> toBeCheckKeywords, boolean maxMerge) {
		Sentence s = new Sentence(altTpl);
		
		boolean doRename = isRename();
		boolean doMerge = isMerge();
		boolean toBeCheck = false;
		
		for (Sentence.Block b: s.getBlocks()) {
			if (b.isInParentheses()) {
				if (CollectionUtils.containsAny(b.getTerms(), toBeCheckKeywords)) {
					toBeCheck = true;
				}
			}
		}
		
		if (toBeCheck) {
			if (doRename) {
				s.replaceTerm(this.getKeyword(), this.getRenameTo());
			}
			
			if (doMerge) {
				if (maxMerge) {
					s.replaceTerms( doRename ? this.getRenameTo() : this.getKeyword(), new HashSet<String>(Arrays.asList(StringUtils.split(this.getToBeMerged(), ","))) );
				}
				else {
					s.removeTermBIfTermAExist( doRename ? this.getRenameTo() : this.getKeyword(), StringUtils.split(this.getToBeMerged(), ",") );
				}
			}
			
			String newAlt = s.concateToString();
			return newAlt;
		}
		else {
			return altTpl;
		}
	}
	
	
	
	
	
	
	// DAO
	public static synchronized SegmentBatchTask get(long id) {
		try {
			return (SegmentBatchTask)HibernateUtil.getSession().get(SegmentBatchTask.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized SegmentBatchTask save(Integer tenantId, String keyword, String toBeMerged, String renameTo, Purpose purpose, Status status, String source) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			SegmentBatchTask word = new SegmentBatchTask();
			word.setTenantId(tenantId);
			word.setKeyword(keyword.toLowerCase());
			word.setToBeMerged(toBeMerged != null ? toBeMerged.toLowerCase() : null);
			word.setRenameTo(renameTo != null ? renameTo.toLowerCase() : null);
			word.setPurpose(purpose);
			word.setSource(source);
			word.setStatus(status);
			word.setEntryTimestamp(Calendar.getInstance().getTime());
			word.appendLog(null, "Created [" + source + "]");
			ses.saveOrUpdate(word);
			tx.commit();
			
			return word;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		
		return null;
	}
	public static synchronized long saveOrUpdate(SegmentBatchTask sbt) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(sbt);
			tx.commit();

		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return sbt.getId();
	}


	public static synchronized void delete(Integer tenantId, String id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			long lid = Long.parseLong(id);
			SegmentBatchTask word = get(lid);

			if (word != null) {
				ses.delete(word);
				tx.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized SegmentBatchTask get(Integer tenantId, String sentence) {
		List<SegmentBatchTask> l = list(tenantId, sentence);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<SegmentBatchTask> listAll() {
		return list(null, null);
	}
/*
	private static List listByQaCategory(Integer tenantId, String qaCategory, boolean fullMatch, Boolean enabled) {
		String searchCategory = StringUtils.lowerCase(qaCategory);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SegmentBatchTask.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (qaCategory != null) {
				ct.add(fullMatch
								? Restrictions.eq("source", searchCategory)
								: Restrictions.like("source", searchCategory, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("source", searchCategory)
									: Restrictions.like("source", searchCategory, MatchMode.ANYWHERE)
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
	}*/

	public static List list(Integer tenantId, String query) {
		return list(tenantId, query, null);
	}

	public static List list(Integer tenantId, String query, Status status) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SegmentBatchTask.class).addOrder(Order.desc("entryTimestamp"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(Restrictions.eq("keyword", searchKeyword));
			}

			if (status != null) {
				ct.add(Restrictions.eq("status", status));
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

	public static Number count(Integer tenantId, String query, Status status) {
		String searchKeyword = StringUtils.lowerCase(query);

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(SegmentBatchTask.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(Restrictions.eq("keyword", searchKeyword));
			}

			if (status != null) {
				ct.add(Restrictions.eq("status", status));
			}

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
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

		List<SegmentBatchTask> l = list(tenantId, null);
		for (SegmentBatchTask dd: l) {
			ccc.get(dd.getPurpose()).add(dd.getKeyword());
		}

		cache.put(tenantId, ccc);
	}
	
	
	
}
