package com.intumit.solr.robot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

public class RobotFormalAnswersVersionDAO {

	private static RobotFormalAnswersVersionDAO instatnce = null;
	private Logger logger = Logger.getLogger(RobotFormalAnswersVersionDAO.class);

	public static RobotFormalAnswersVersionDAO getInstance() {
		if (instatnce == null) {
			synchronized (RobotFormalAnswersVersionDAO.class) {
				instatnce = new RobotFormalAnswersVersionDAO();
			}
		}
		return instatnce;
	}

	public RobotFormalAnswersVersion get(long id) {
		Session session = null;
		RobotFormalAnswersVersion result = null;
		try {
			session = HibernateUtil.getSession();
			result = (RobotFormalAnswersVersion) session.get(RobotFormalAnswersVersion.class, id);
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public synchronized void delete(RobotFormalAnswersVersion entity) {
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

	public synchronized void saveOrUpdate(RobotFormalAnswersVersion entity) {
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

	public RobotFormalAnswersVersion findByKeyAndStatus(int tenantId, String key, AuditStatus status) {
		RobotFormalAnswersVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(RobotFormalAnswersVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("keyName", key));
			criteria.add(Restrictions.eq("status", status));
			result = (RobotFormalAnswersVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public RobotFormalAnswersVersion findByPublicIdAndStatus(int tenantId, Long publicId, AuditStatus status) {
		RobotFormalAnswersVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(RobotFormalAnswersVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.eq("status", status));
			result = (RobotFormalAnswersVersion) criteria.uniqueResult();
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
			Criteria criteria = session.createCriteria(RobotFormalAnswersVersion.class)
					.addOrder(Order.desc("passTime"));
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.eq("status", AuditStatus.HISTORY));
			criteria.setFirstResult(0).setMaxResults(1);
			try {
				return ((RobotFormalAnswersVersion) criteria.uniqueResult()).getPassTime();
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

	public int getPublicVesionNum(int tenantId, Long publicId) {
		int versionNum = 1;
		if (publicId != null) {
			Session session = null;
			try {
				session = HibernateUtil.getSession();
				Query query = session.createQuery(
						"SELECT COUNT(*) FROM RobotFormalAnswersVersion WHERE publicId=:publicId AND tenantId=:tenantId");
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

	public List<RobotFormalAnswersVersion> listAll(int tenantId) {
		final List<RobotFormalAnswersVersion> entities = new ArrayList<>();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					String sql = "SELECT *, (SELECT name FROM AdminUser u WHERE d.editorId=u.id) as editorName, (SELECT name FROM AdminUser u WHERE d.auditorId=u.id) as auditorName FROM RobotFormalAnswersVersion d where d.tenantId = ?";
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, tenantId);
						rs = pstmt.executeQuery();
						while (rs.next()) {
							RobotFormalAnswersVersion version = new RobotFormalAnswersVersion();
							version.setKeyName(rs.getString("keyName"));
							version.setAnswers(rs.getString("answers"));
							version.setId(rs.getLong("id"));
							version.setTenantId(rs.getInt("tenantId"));
							version.setEditorId(rs.getInt("editorId"));
							version.setEditorName(rs.getString("editorName"));
							version.setAuditorId(rs.getInt("auditorId"));
							version.setAuditorName(rs.getString("auditorName"));
							version.setAction(AuditAction.values()[rs.getInt("action")]);
							version.setStatus(AuditStatus.values()[rs.getInt("status")]);
							version.setCreateTime(rs.getTimestamp("createTime"));
							version.setUpdateTime(rs.getTimestamp("updateTime"));
							version.setUpdateLog(rs.getString("updateLog"));
							version.setPassTime(rs.getTimestamp("passTime"));
							version.setMessage(rs.getString("message"));
							version.setPublicId(rs.getLong("publicId"));
							entities.add(version);
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

	public RobotFormalAnswersVersion getByPublicId(int tenantId, Long id) {
		RobotFormalAnswersVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(RobotFormalAnswersVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", id));
			result = (RobotFormalAnswersVersion) criteria.uniqueResult();
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
