package com.intumit.solr.robot.dictionary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

public class DictionaryVersionDAO {
	private static DictionaryVersionDAO instatnce = null;
	private Logger logger = Logger.getLogger(DictionaryVersionDAO.class);

	public static DictionaryVersionDAO getInstance() {
		if (instatnce == null) {
			synchronized (DictionaryVersionDAO.class) {
				instatnce = new DictionaryVersionDAO();
			}
		}
		return instatnce;
	}

	public DictionaryDatabaseVersion get(long id) {
		Session session = null;
		DictionaryDatabaseVersion result = null;
		try {
			session = HibernateUtil.getSession();
			result = (DictionaryDatabaseVersion) session.get(DictionaryDatabaseVersion.class, id);
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public synchronized void delete(DictionaryDatabaseVersion entity) {
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

	public synchronized void saveOrUpdate(DictionaryDatabaseVersion entity) {
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

	public DictionaryDatabaseVersion findByKeywordAndStatus(int tenantId, String keyword, AuditStatus status) {
		DictionaryDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("keyword", keyword));
			criteria.add(Restrictions.eq("status", status));
			result = (DictionaryDatabaseVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public DictionaryDatabaseVersion findByPublicIdAndStatus(int tenantId, Long publicId, AuditStatus status) {
		DictionaryDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.eq("status", status));
			result = (DictionaryDatabaseVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}
	
	public List<DictionaryDatabaseVersion> listByPublicIdsAndStatus(int tenantId, Collection<Long> publicIds, AuditStatus status) {
	    Session session = null;
	    List<DictionaryDatabaseVersion> all = new ArrayList<>();
        try {
            session = HibernateUtil.getSession();
            AtomicInteger counter = new AtomicInteger(0);
            Collection<List<Long>> publicIdGroupItr = publicIds.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 1200)).values();
            for(List<Long> currentQueryIds : publicIdGroupItr) {
                Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class);
                criteria.add(Restrictions.eq("tenantId", tenantId));
                criteria.add(Restrictions.in("publicId", currentQueryIds));
                criteria.add(Restrictions.eq("status", status));
                all.addAll(criteria.list());
            }
        } catch (HibernateException e) {
            logger.error(e, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return all;
	}
	
	public Date getLastPassTime(int tenantId, Long publicId) {
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class).addOrder(Order.desc("passTime"));
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.or(Restrictions.eq("status", AuditStatus.HISTORY),
					Restrictions.eq("status", AuditStatus.PUBLISH)));
			criteria.setFirstResult(0).setMaxResults(1);
			try {
				return ((DictionaryDatabaseVersion) criteria.uniqueResult()).getPassTime();
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
	
	public List<DictionaryDatabaseVersion> listAllPassedByPublicIds(int tenantId, Collection<Long> publicIds) {
	    List<DictionaryDatabaseVersion> all = new ArrayList<>();
	    Session session = null;
        try {
            session = HibernateUtil.getSession();
            AtomicInteger counter = new AtomicInteger(0);
            Collection<List<Long>> publicIdGroupItr = publicIds.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 1200)).values();
            for(List<Long> currentQueryIds : publicIdGroupItr) {
                Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class).addOrder(Order.desc("passTime"));
                criteria.add(Restrictions.eq("tenantId", tenantId));
                criteria.add(Restrictions.in("publicId", currentQueryIds));
                criteria.add(Restrictions.or(Restrictions.eq("status", AuditStatus.HISTORY),
                        Restrictions.eq("status", AuditStatus.PUBLISH)));
                 all.addAll(criteria.list());
            }
        } catch (HibernateException e) {
            logger.error(e, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return all;
	}

	public int getPublicVesionNum(int tenantId, Long publicId) {
		int versionNum = 1;
		if (publicId != null) {
			Session session = null;
			try {
				session = HibernateUtil.getSession();
				Query query = session.createQuery(
						"select count(*) from  DictionaryDatabaseVersion where publicId=:publicId and tenantId=:tenantId");
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

	public List<DictionaryDatabaseVersion> listAll(int tenantId) {
		return list(tenantId, false);
	}

	public List<DictionaryDatabaseVersion> list(int tenantId, boolean exceptPublish) {
		final List<DictionaryDatabaseVersion> entities = new ArrayList<>();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					String sql = "select id,category,enableQaScopeRestriction,enabled,keyword,purposes,editorId,(select name From AdminUser u where d.editorId=u.id) as editorName,auditorId,(select name From AdminUser u where d.auditorId=u.id) as auditorName,message,status,action,createTime,updateTime,updateLog,passTime,publicId from DictionaryDatabaseVersion d where d.tenantId = ?";
					if (exceptPublish) {
						sql += " AND d.status != 0";
					}
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, tenantId);
						rs = pstmt.executeQuery();
						while (rs.next()) {
							DictionaryDatabaseVersion ddv = new DictionaryDatabaseVersion();
							ddv.setId(rs.getLong("id"));
							ddv.setKeyword(rs.getString("keyword"));
							ddv.setCategory(rs.getString("category"));
							ddv.setPurposes(rs.getString("purposes"));
							ddv.setEnabled(rs.getBoolean("enabled"));
							ddv.setEnableQaScopeRestriction(rs.getBoolean("enableQaScopeRestriction"));
							ddv.setEditorId(rs.getInt("editorId"));
							ddv.setEditorName(rs.getString("editorName"));
							ddv.setAuditorId(rs.getInt("auditorId"));
							ddv.setAuditorName(rs.getString("auditorName"));
							ddv.setAction(AuditAction.values()[rs.getInt("action")]);
							ddv.setStatus(AuditStatus.values()[rs.getInt("status")]);
							ddv.setCreateTime(rs.getTimestamp("createTime"));
							ddv.setUpdateTime(rs.getTimestamp("updateTime"));
							ddv.setUpdateLog(rs.getString("updateLog"));
							ddv.setPassTime(rs.getTimestamp("passTime"));
							ddv.setMessage(rs.getString("message"));
							ddv.setPublicId(rs.getLong("publicId"));
							entities.add(ddv);
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

	public DictionaryDatabaseVersion getByPublicId(int tenantId, Long id) {
		DictionaryDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(DictionaryDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", id));
			result = (DictionaryDatabaseVersion) criteria.uniqueResult();
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
