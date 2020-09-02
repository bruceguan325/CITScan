package com.intumit.solr.synonymKeywords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.StrUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.hazelcast.core.ITopic;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.KuromojiUtil;
import com.intumit.solr.robot.SegmentBatchTask;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;

public class SynonymKeywordFacade {
	public static final String DEFAULT_FUNC = "default";
	static SynonymKeywordFacade instance = null;
	public static final int MAX_KEYWORD_LENGTH = 128;
	protected static Map<String, Long> keywordPriorityMap = new HashMap<String, Long>();

	List<SynonymKeywordChangeListener> listeners = new ArrayList<SynonymKeywordChangeListener>();

	/**
	 * 改成 Singleton
	 * 
	 * @return
	 */
	public static SynonymKeywordFacade getInstance() {
		if (instance == null) {
			synchronized (SynonymKeywordChangeListener.class) {
				if (instance == null) {
					instance = new SynonymKeywordFacade();
				}
			}
		}
		return instance;
	}

	public void reset() {
		instance = new SynonymKeywordFacade();
	}

	public void addListener(SynonymKeywordChangeListener l) {
		listeners.add(l);
	}

	public void removeListener(SynonymKeywordChangeListener l) {
		listeners.remove(l);
	}

	private SynonymKeywordFacade() {
		ITopic topic = HazelcastUtil.getTopic("synonym");
		topic.addMessageListener(new SynonymKeywordMessageListener());

		addListener(new SynonymChange4CacheReloadListener());
		addListener(new SynonymChange4UserDefinedDictionary());
		addListener(new SynonymChange4DictionarySearcher());
		addListener(new SynonymChange4IntentAndEntityDictionaryListener());
	}

	public synchronized SynonymKeyword get(long id) {
		try {
			return (SynonymKeyword) HibernateUtil.getSession().get(SynonymKeyword.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public synchronized SynonymKeyword save(SynonymKeyword synonymKeyword) {
		return save(synonymKeyword.getTenantId(), synonymKeyword.getKeyword(), synonymKeyword.getSynonymKeyword(),
				synonymKeyword.isReverse(), synonymKeyword.getNature());
	}

	public synchronized SynonymKeyword save(Integer tenantId, String keyword, String synonymKeyword, boolean reverse,
			String nature) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			SynonymKeyword syn = new SynonymKeyword();
			syn.setTenantId(tenantId);
			syn.setKeyword(keyword.toLowerCase());
			syn.setSynonymKeyword("," + synonymKeyword.toLowerCase() + ",");
			syn.setNature(nature);

			// 這段在判斷日文自動產生平假名的同義詞
			Tenant t = Tenant.get(syn.getTenantId());
			if (t != null && t.getLocale() == HitHotLocale.ja_JP) {
				String synStr = syn.getSynonymKeywordForEditingOrReading();
				List<String> autoGen = new KuromojiUtil()
						.segAndToHiragana(Arrays.asList(StringUtils.split(syn.getKeyword() + synStr, ",")));

				if (autoGen != null && autoGen.size() > 0) {
					synStr += ",,,," + StringUtils.join(autoGen, ",");
				}
				syn.setSynonymKeyword("," + synStr + ",");
			}
			syn.setReverse(reverse);
			ses.saveOrUpdate(syn);
			tx.commit();

			try {
				SegmentBatchTask sbt = SegmentBatchTask.save(tenantId, keyword,
						syn.getSynonymKeywordForEditingOrReading(), null,
						SegmentBatchTask.Purpose.SYNONYM_KEYWORD_CHANGED, SegmentBatchTask.Status.WAIT,
						" auto create when Synonym saved.");

				if (sbt != null && !sbt.hasPossibleChange()) {
					sbt.setStatus(SegmentBatchTask.Status.REJECT);
					SegmentBatchTask.saveOrUpdate(sbt);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			EventCenter.fireEvent(getClass().getName(), tenantId, "reload", "同義詞新增成功!");
			return syn;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return null;
	}

	public boolean isNew(int TenantId, String keyWord) {
		boolean isNew = true;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(SynonymKeyword.class);
			criteria.add(Restrictions.eq("tenantId", TenantId));
			criteria.add(Restrictions.eq("keyWord", keyWord));
			isNew = CollectionUtils.isEmpty(criteria.list());
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return isNew;
	}

	public synchronized SynonymKeyword update(Long id, String keyword, String synonymKeyword, boolean reverse, String nature)
			throws Exception {
		Session ses = null;
		Transaction tx = null;
		SynonymKeyword word = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			SynonymKeyword oldSyn = get(id);
			SynonymKeyword newSyn = get(id);

			newSyn.setId(id);
			newSyn.setKeyword(keyword.toLowerCase());
			newSyn.setSynonymKeyword("," + synonymKeyword.toLowerCase() + ",");
			newSyn.setNature(nature);

			// 這段在判斷日文自動產生平假名的同義詞
			Tenant t = Tenant.get(newSyn.getTenantId());
			if (t != null && t.getLocale() == HitHotLocale.ja_JP) {
				String synStr = newSyn.getSynonymKeywordForEditingOrReading();
				List<String> autoGen = new KuromojiUtil()
						.segAndToHiragana(Arrays.asList(StringUtils.split(newSyn.getKeyword() + synStr, ",")));

				if (autoGen != null && autoGen.size() > 0) {
					synStr += ",,,," + StringUtils.join(autoGen, ",");
				}
				newSyn.setSynonymKeyword("," + synStr + ",");
			}
			newSyn.setReverse(reverse);
			ses.update(newSyn);
			tx.commit();
			word = newSyn;
			try {
				String renameTo = StringUtils.equalsIgnoreCase(oldSyn.getKeyword(), newSyn.getKeyword()) ? null
						: newSyn.getKeyword();
				String toBeMerged = StringUtils.equalsIgnoreCase(oldSyn.getSynonymKeyword(), newSyn.getSynonymKeyword())
						? null
						: StringUtils.strip(newSyn.getSynonymKeywordForEditingOrReading(), ",");

				if (toBeMerged != null || renameTo != null) {
					SegmentBatchTask sbt = SegmentBatchTask.save(oldSyn.getTenantId(), oldSyn.getKeyword(), toBeMerged,
							renameTo, SegmentBatchTask.Purpose.SYNONYM_KEYWORD_CHANGED, SegmentBatchTask.Status.WAIT,
							" auto create when Synonym update.");

					if (sbt != null && !sbt.hasPossibleChange()) {
						sbt.setStatus(SegmentBatchTask.Status.REJECT);
						SegmentBatchTask.saveOrUpdate(sbt);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			EventCenter.fireEvent(getClass().getName(), t.getId(), "reload", "同義詞更新成功!");
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return word;
	}

	public synchronized void delete(Integer tenantId, Long id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			SynonymKeyword syn = get(id);

			if (syn != null) {
				ses.createQuery("delete from " + SynonymKeyword.class.getName() + " Where id=" + id).executeUpdate();
				tx.commit();
				
				EventCenter.fireEvent(getClass().getName(), tenantId, "reload", "同義詞刪除成功!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized SynonymKeyword get(Integer tenantId, String keyword) {
		List<SynonymKeyword> l = list(tenantId, keyword, false, false);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public List<String> getSynonyms(Integer tenantId, String query, boolean keywordOnly) {
		String searchKeyword = query.toLowerCase();
		List<SynonymKeyword> listSyn = list(tenantId, searchKeyword, keywordOnly, false);
		List<String> listKw = new ArrayList<String>();

		for (SynonymKeyword syn : listSyn) {

			if (searchKeyword.equals(syn.getKeyword())) {
				if (syn.isReverse()) {
					listKw.addAll(Arrays.asList(StringUtils.split(syn.getKeyword(), ",")));
					listKw.addAll(Arrays.asList(StringUtils.split(syn.getSynonymKeyword(), ",")));
				} else {
					listKw.addAll(Arrays.asList(StringUtils.split(syn.getSynonymKeyword(), ",")));
				}
			} else {
				if (syn.isReverse()) {
					List<String> tmpSyns = new ArrayList<>(
							Arrays.asList(StringUtils.split(syn.getSynonymKeyword(), ",")));
					if (tmpSyns.contains(searchKeyword)) {
						listKw.addAll(Arrays.asList(StringUtils.split(syn.getKeyword(), ",")));
						listKw.addAll(tmpSyns);
					}
				}
			}

			if (listKw.size() > 10000) {
				System.out.println("Memory leak detected! ");
				break;
			}
		}

		return listKw;
	}

	public synchronized List<SynonymKeyword> listAll(Integer tenantId) {
		List<SynonymKeyword> result = new ArrayList<>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SynonymKeyword.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
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

	public synchronized List<SynonymKeyword> listByQueryWhereReverseIsTrue(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, false);
	}

	/**
	 * 
	 * @param searchKeyword
	 * @return
	 */
	public synchronized List<SynonymKeyword> listByQuery(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, true);
	}

	private Map<Integer, Table<String, Boolean, List<SynonymKeyword>>> caches = new HashMap<Integer, Table<String, Boolean, List<SynonymKeyword>>>();

	private void clearCache(Integer tenantId) {
		Table<String, Boolean, List<SynonymKeyword>> cache = caches.get(tenantId);

		if (cache != null) {
			cache.clear();
		}
	}

	synchronized Table<String, Boolean, List<SynonymKeyword>> reloadCache(Integer tenantId) {
		Table<String, Boolean, List<SynonymKeyword>> cache = caches.get(tenantId);

		if (cache != null) {
			cache.clear();
		} else {
			cache = HashBasedTable.create();
			caches.put(tenantId, cache);
		}

		List<SynonymKeyword> allSyns = listAll(tenantId);

		for (SynonymKeyword syn : allSyns) {
			{
				String lk = StringUtils.lowerCase(syn.getKeyword());
				List<SynonymKeyword> lsk = cache.get(lk, true);

				if (lsk == null) {
					lsk = new ArrayList<SynonymKeyword>();
					cache.put(lk, true, lsk);
				}

				if (!lsk.contains(syn))
					lsk.add(syn);

				// reverse = false
				List<SynonymKeyword> lskf = cache.get(lk, false);

				if (lskf == null) {
					lskf = new ArrayList<SynonymKeyword>();
					cache.put(lk, false, lsk);
				}

				if (!lskf.contains(syn))
					lskf.add(syn);
			}

			for (String syn2 : syn.getSynonymList()) {
				String lk = StringUtils.lowerCase(syn2);
				List<SynonymKeyword> lsk = cache.get(lk, true);

				if (lsk == null) {
					lsk = new ArrayList<SynonymKeyword>();
					cache.put(lk, true, lsk);
				}

				if (!lsk.contains(syn))
					lsk.add(syn);

				if (syn.isReverse()) {
					// reverse = false
					List<SynonymKeyword> lskf = cache.get(lk, false);

					if (lskf == null) {
						lskf = new ArrayList<SynonymKeyword>();
						cache.put(lk, false, lsk);
					}

					if (!lskf.contains(syn))
						lskf.add(syn);
				}
			}
		}

		// Cache 建完了，全部改成 unmodifiable，避免後面出現奇怪的事情
		for (Cell<String, Boolean, List<SynonymKeyword>> cell : cache.cellSet()) {
			cache.put(cell.getRowKey(), cell.getColumnKey(), Collections.unmodifiableList(cell.getValue()));
		}

		return cache;
	}

	/*
	 */
	private List list(Integer tenantId, String query, boolean keywordOnly, boolean includeUnreversible) {
		List result = new ArrayList();
		String searchKeyword = query.toLowerCase();
		Table<String, Boolean, List<SynonymKeyword>> cache = caches.get(tenantId);

		if (!keywordOnly) {
			if (cache == null) {
				cache = reloadCache(tenantId);
			}
			List<SynonymKeyword> fromCache = cache.get(searchKeyword, includeUnreversible);

			if (fromCache != null)
				return fromCache;
			else
				return result;
		}

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(SynonymKeyword.class);
			Criterion c1 = (Restrictions.eq("keyword", "" + searchKeyword + ""));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (keywordOnly) {
				ct.add(c1);
			} else {
				Criterion c2 = null;
				if (includeUnreversible) {
					c2 = Restrictions.like("synonymKeyword", "%," + searchKeyword + ",%");
				} else {
					c2 = Restrictions.and(Restrictions.like("synonymKeyword", "%," + searchKeyword + ",%"),
							Restrictions.ne("reverse", false)); // 不包含 reverse == false 的，因此應該就是 reverse == true 的...
				}
				LogicalExpression orExp = Restrictions.or(c1, c2);
				ct.add(orExp);
			}
			result = ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	public SynonymKeyword findSynonymKeywordById(Integer tenantId, Long id) {
		SynonymKeyword result = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			
			if (id==null)
				return null;
			
			Criteria ct = ses.createCriteria(SynonymKeyword.class);
			
			ct.add(Restrictions.eq("tenantId", tenantId));
			ct.add(Restrictions.eq("id", id));
			
			result = (SynonymKeyword) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}
	
	public SynonymKeyword findSynonymKeywordByKeyword(Integer tenantId, String keyword) {
		SynonymKeyword result = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			
			Criteria ct = ses.createCriteria(SynonymKeyword.class);
			
			ct.add(Restrictions.eq("tenantId", tenantId));
			ct.add(Restrictions.eq("keyword", keyword));
			
			result = (SynonymKeyword) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}
	
	public List<SynonymKeyword> findBySynonymKeyword(Integer tenantId, String query, String nowSynkeyword) {
		List<SynonymKeyword> allResult = new ArrayList<SynonymKeyword>();
		String searchKeyword = query.toLowerCase();
		List<String> nowKeywords = null;
		String[] keywords = searchKeyword.split(",");
		Session ses = null;
		try {		
			ses = HibernateUtil.getSession();
			
			if(StringUtils.strip(nowSynkeyword, ",").equals(""))
				nowKeywords = new ArrayList<String>();
			else
				nowKeywords = Arrays.asList(StringUtils.strip(nowSynkeyword, ",").split(","));
			
			for(String word:keywords) { // synonymKeyword loop
				
				if(nowKeywords.indexOf(word) != -1) // 原本關鍵字已存在跳過不判斷
					continue;
				
				Criteria ct = ses.createCriteria(SynonymKeyword.class);
				
				if (tenantId != null) {
					ct.add(Restrictions.eq("tenantId", tenantId));
				}
                ct.add(Restrictions.or(Restrictions.like("synonymKeyword", "%," + word + ",%"),
                        			   Restrictions.eq("keyword", "" + word + "")));
				ct.add(Restrictions.ne("reverse", false));
				List<SynonymKeyword> result = ct.list();
				
				if(result != null) {
					allResult.addAll(result);
				}
			}
			
			if(allResult.size() == 0)
				return null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return allResult;
	}

	public List fullSearch(Integer tenantId, String query) {
		String searchKeyword = query.toLowerCase();
		List result = null;

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SynonymKeyword.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			// ct.add(Restrictions.like("keyword",searchKeyword));
			Criterion c1 = (Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE));
			Criterion c2 = (Restrictions.like("synonymKeyword", searchKeyword, MatchMode.ANYWHERE));
			LogicalExpression orExp = Restrictions.or(c1, c2);
			ct.add(orExp);
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
	
	public List<SynonymKeyword> fullSearchBySQL(int tenantId, String query, AuditStatus status) {
		String searchKeyword = query.toLowerCase();
		List result = new ArrayList();
		SynonymVersionService service = SynonymVersionService.getInstance();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					
					if (status != null) {
						sql.append(" SELECT s.id, s.keyword, s.synonymKeyword, s.nature, s.reverse, s.tenantId ");
						sql.append(" FROM SynonymKeyword AS s ");
						sql.append(" WHERE s.tenantId = ? ");
					}else {
						sql.append(" SELECT s.id, s.keyword, s.synonymKeyword, s.nature, s.reverse, s.tenantId, v.status, v.passTime ");
						sql.append(" FROM SynonymKeyword AS s ");
						sql.append(" LEFT JOIN SynonymKeywordVersion AS v ");
						sql.append(" ON s.tenantId = v.tenantId and s.id = v.publicId ");
						sql.append(" WHERE s.tenantId = ? AND (v.version is null OR v.version = ");
						
						// 取最新版本，不包含審核中
						sql.append(" (SELECT TOP 1 v.version FROM SynonymKeywordVersion AS v ");
						sql.append(" WHERE s.tenantId = v.tenantId AND s.id = v.publicId AND v.passTime IS NOT null order by v.updateTime desc )) ");
						
						sql.append(" AND (CAST(s.id AS CHAR) = ? ");
						sql.append("   	  OR  s.keyword LIKE ? ");
						sql.append("   	  OR  s.synonymKeyword LIKE ? ");
						sql.append("   	  OR  CAST(convert(char(10), v.passTime, 111) AS char) = ? ) ");
					}
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql.toString());
						
						if (status != null) {
							pstmt.setInt(1, tenantId);
						} else {
							pstmt.setInt(1, tenantId);
							pstmt.setString(2, searchKeyword);
							pstmt.setString(3, "%" + searchKeyword + "%");
							pstmt.setString(4, "%" + searchKeyword + "%");
							pstmt.setString(5, searchKeyword);	
						}
						
						rs = pstmt.executeQuery();
						while (rs.next()) {
							SynonymKeyword sk = new SynonymKeyword();
							sk.setId(rs.getLong("id"));
							sk.setKeyword(rs.getString("keyword"));
							sk.setSynonymKeyword(StringUtils.strip(rs.getString("synonymKeyword"), ","));
							sk.setNature(rs.getString("nature"));
							sk.setReverse(rs.getBoolean("reverse"));
							sk.setTenantId(rs.getInt("tenantId"));
							
							if (status == AuditStatus.AUDIT) {
								if(service.publicIdInAudit(sk.getTenantId(), sk.getId()))
									result.add(sk);
							} else if(status == AuditStatus.HISTORY) {
								if(!service.publicIdInAudit(sk.getTenantId(), sk.getId()))
									result.add(sk);
							} else {
								result.add(sk);
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

	// a , b c , d e f => [[a],[b,c],[d,e,f]]
	private static List<List<String>> getSynList(String str, String separator) {
		List<String> strList = new ArrayList<>(Arrays.asList(StringUtils.split(str, separator)));
		// now split on whitespace to get a list of token strings
		List<List<String>> synList = new ArrayList<List<String>>();
		for (String toks : strList) {
			List<String> tokList = StrUtils.splitWS(toks, true);
			synList.add(tokList);
		}
		return synList;
	}

	public synchronized String querySynonymKeywordXmlPartial(Integer tenantId, String keyword) {
		List<SynonymKeyword> result = listByQueryWhereReverseIsTrue(tenantId, keyword);
		StringBuilder sb = new StringBuilder();
		sb.append("<list>");
		for (SynonymKeyword sk : result) {
			sb.append(toXML(sk));
		}
		sb.append("</list>");
		return sb.toString();
	}

	public synchronized String querySynonymKeywordXmlFull(Integer tenantId, String keyword) {
		List<SynonymKeyword> result = fullSearch(tenantId, keyword);
		StringBuilder sb = new StringBuilder();
		sb.append("<list>");
		for (SynonymKeyword sk : result) {
			sb.append(toXML(sk, keyword));
		}
		sb.append("</list>");
		return sb.toString();
	}

	private String toXML(SynonymKeyword sk) {
		StringBuilder sb = new StringBuilder();
		sb.append("<result>");
		sb.append("<keyword>");
		sb.append(sk.getKeyword());
		sb.append("</keyword>");
		for (String synonym : StringUtils.split(sk.getSynonymKeywordForEditingOrReading(), ",")) {
			sb.append("<synonym>");
			sb.append(synonym);
			sb.append("</synonym>");
		}
		sb.append("</result>");
		return sb.toString();

	}

	private String toXML(SynonymKeyword sk, String query) {
		String keyword = query.toLowerCase();
		StringBuilder sb = new StringBuilder();
		boolean flag = false;
		for (String synonym : StringUtils.split(sk.getSynonymKeywordForEditingOrReading(), ",")) {
			if (synonym.equals(keyword)) {
				flag = true;
				break;
			}
		}

		if (sk.getKeyword().equals(keyword) || flag) {
			sb.append("<result>");
			sb.append("<keyword>");
			sb.append(sk.getKeyword());
			sb.append("</keyword>");
			for (String synonym : StringUtils.split(sk.getSynonymKeywordForEditingOrReading(), ",")) {
				sb.append("<synonym>");
				sb.append(synonym);
				sb.append("</synonym>");
			}
			sb.append("</result>");
		}
		return sb.toString();

	}

}
