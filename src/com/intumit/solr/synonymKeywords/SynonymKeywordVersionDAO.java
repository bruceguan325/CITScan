package com.intumit.solr.synonymKeywords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.AuditAction;
import com.intumit.solr.robot.AuditStatus;

public class SynonymKeywordVersionDAO {
	private static SynonymKeywordVersionDAO instance = null;
	private Logger logger = Logger.getLogger(SynonymKeywordVersionDAO.class);

	private SynonymKeywordVersionDAO() {
	}

	public static SynonymKeywordVersionDAO getInstance() {
		if (instance == null) {
			synchronized (SynonymKeywordVersionDAO.class) {
				instance = new SynonymKeywordVersionDAO();
			}
		}
		return instance;
	}

	public synchronized void save(SynonymKeyword synonymKeyword, int editorId) {
		Session session = HibernateUtil.getSession();
		SynonymKeywordVersion entity = new SynonymKeywordVersion(synonymKeyword);
		entity.setEditorId(editorId);
		entity.setStatus(AuditStatus.AUDIT);
		entity.setCreateTime(new Date());
		entity.setAction(AuditAction.ADD);
		Transaction tx = session.beginTransaction();
		try {
			session.save(entity);
		} catch (HibernateException e) {
			tx.rollback();
			logger.error(e, e);
		} finally {
			session.close();
		}
	}

	public synchronized void saveOrUpdate(SynonymKeywordVersion entity) {
		Session session = HibernateUtil.getSession();
		Transaction tx = session.beginTransaction();
		try {
			session.saveOrUpdate(entity);
			tx.commit();
		} catch (HibernateException e) {
			tx.rollback();
			logger.error(e, e);
		} finally {
			session.close();
		}
	}

	public SynonymKeywordVersion get(long id) {
		Session session = null;
		SynonymKeywordVersion result = null;
		try {
			session = HibernateUtil.getSession();
			result = (SynonymKeywordVersion) session.get(SynonymKeywordVersion.class, id);
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public SynonymKeywordVersion findByKeywordAndStatus(int tenantId, String keyword, AuditStatus status) {
		SynonymKeywordVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(SynonymKeywordVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.or(Restrictions.eq("keyword", keyword),
										 Restrictions.like("synonymKeyword", "%," + keyword + ",%")));
			criteria.add(Restrictions.eq("status", status));
			result = (SynonymKeywordVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}
	
	public List<SynonymKeywordVersion> findBySynonymKeywordAndStatus(int tenantId, String keyword, AuditStatus status, String nowSynkeyword) {
		List<SynonymKeywordVersion> allResult = new ArrayList<SynonymKeywordVersion>();
		String searchKeyword = keyword.toLowerCase();
		List<String> nowKeywords = null;
		String[] keywords = searchKeyword.split(",");
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			
			if(StringUtils.strip(nowSynkeyword, ",").equals(""))
				nowKeywords = new ArrayList<String>();
			else
				nowKeywords = Arrays.asList(StringUtils.strip(nowSynkeyword, ",").split(","));

			for(String word:keywords) { // synonymKeyword loop
				
				if(nowKeywords.indexOf(word) != -1) // 原本關鍵字已存在跳過不判斷
					continue;
				
				Criteria criteria = session.createCriteria(SynonymKeywordVersion.class);
				criteria.add(Restrictions.eq("tenantId", tenantId));
				criteria.add(Restrictions.or(Restrictions.like("synonymKeyword", "%," + word + ",%"),
         			   						 Restrictions.eq("keyword", "" + word + "")));
				criteria.add(Restrictions.eq("status", status));
				List<SynonymKeywordVersion> result = criteria.list();
				
				if(result != null) {
					allResult.addAll(result);
				}
			}
			
			if(allResult.size() == 0)
				return null;
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return allResult;
	}
	
	public SynonymKeywordVersion findByPublicIdAndStatus(int tenantId, Long publicId, AuditStatus status) {
		SynonymKeywordVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(SynonymKeywordVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.eq("status", status));
			result = (SynonymKeywordVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}
	
	public Date getLastPassTime(int tenantId, Long publicId) {
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(SynonymKeywordVersion.class).addOrder(Order.desc("passTime"));
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.or(Restrictions.eq("status", AuditStatus.HISTORY),
					Restrictions.eq("status", AuditStatus.PUBLISH)));
			criteria.setFirstResult(0).setMaxResults(1);
			try {
				return ((SynonymKeywordVersion) criteria.uniqueResult()).getPassTime();
			} catch (Exception e) {
			}
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return null;
		
	}

	public synchronized void delete(SynonymKeywordVersion entity) {
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Transaction tx = session.beginTransaction();
			session.delete(entity);
			tx.commit();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	public int getPublicVesionNum(int tenantId, Long publicId) {
		int versionNum = 1;
		if (publicId != null) {
			Session session = null;
			try {
				session = HibernateUtil.getSession();
				Query query = session.createQuery(
						"select count(*) from  SynonymKeywordVersion where publicId=:publicId and tenantId=:tenantId");
				query.setInteger("tenantId", tenantId);
				query.setLong("publicId", publicId);
				Long count = (Long) query.uniqueResult();
				if (count != null) {
					versionNum += count;
				}
			} catch (HibernateException e) {
				logger.error(e, e);
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
		return versionNum;
	}

	public List<SynonymKeywordVersion> listAll(int tenantId) {
		return list(tenantId, false);
	}

	public List<SynonymKeywordVersion> list(int tenantId, boolean exceptPublish) {
		final List<SynonymKeywordVersion> entities = new ArrayList<>();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					sql.append(" select id,keyword,synonymKeyword,nature,reverse,editorId, ");
					sql.append(" 		(select name From AdminUser u where s.editorId=u.id ) as editorName,auditorId, ");
					sql.append(" 		(select name From AdminUser u where s.auditorId=u.id) as auditorName, ");
					sql.append(" 		message,status,action,createTime,updateTime,updateLog,passTime,publicId ");
					sql.append(" from SynonymKeywordVersion s where s.tenantId = ? ");
					if (exceptPublish) {
						sql.append(" AND s.status != 0 ");
					}
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql.toString());
						pstmt.setInt(1, tenantId);
						rs = pstmt.executeQuery();
						while (rs.next()) {
							SynonymKeywordVersion skv = new SynonymKeywordVersion();
							skv.setId(rs.getLong("id"));
							skv.setKeyword(rs.getString("keyword"));
							skv.setSynonymKeyword(StringUtils.strip(rs.getString("synonymKeyword"), ","));
							skv.setNature(rs.getString("nature"));
							skv.setReverse(rs.getBoolean("reverse"));
							skv.setEditorId(rs.getInt("editorId"));
							skv.setEditorName(rs.getString("editorName"));
							skv.setAuditorId(rs.getInt("auditorId"));
							skv.setAuditorName(rs.getString("auditorName"));
							skv.setAction(AuditAction.values()[rs.getInt("action")]);
							skv.setStatus(AuditStatus.values()[rs.getInt("status")]);
							skv.setCreateTime(rs.getTimestamp("createTime"));
							skv.setUpdateTime(rs.getTimestamp("updateTime"));
							skv.setUpdateLog(rs.getString("updateLog"));
							skv.setPassTime(rs.getTimestamp("passTime"));
							skv.setMessage(rs.getString("message"));
							skv.setPublicId(rs.getLong("publicId"));
							entities.add(skv);
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
		return entities;
	}

	public SynonymKeywordVersion getByPublicId(int tenantId, Long id) {
		SynonymKeywordVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(SynonymKeywordVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", id));
			result = (SynonymKeywordVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}
}
