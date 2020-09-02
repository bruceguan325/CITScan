package com.intumit.solr.robot.entity;

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
import com.intumit.solr.robot.AuditStatus;

public class EntityVersionDAO {
	private static EntityVersionDAO instatnce = null;
	private Logger logger = Logger.getLogger(EntityVersionDAO.class);

	public static EntityVersionDAO getInstance() {
		if (instatnce == null) {
			synchronized (EntityVersionDAO.class) {
				instatnce = new EntityVersionDAO();
			}
		}
		return instatnce;
	}

	public EntityDatabaseVersion get(long id) {
		Session session = null;
		EntityDatabaseVersion result = null;
		try {
			session = HibernateUtil.getSession();
			result = (EntityDatabaseVersion) session.get(EntityDatabaseVersion.class, id);
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}

	public synchronized void delete(EntityDatabaseVersion entity) {
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

	public synchronized void saveOrUpdate(EntityDatabaseVersion entity) {
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

	public EntityDatabaseVersion findByCodeAndStatus(int tenantId, String code, AuditStatus status) {
		EntityDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(EntityDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("code", code));
			criteria.add(Restrictions.eq("status", status));
			result = (EntityDatabaseVersion) criteria.uniqueResult();
		} catch (HibernateException e) {
			logger.error(e, e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return result;
	}
	
	public EntityDatabaseVersion findByPublicIdAndStatus(int tenantId, Long publicId, AuditStatus status) {
		EntityDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(EntityDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.eq("status", status));
			result = (EntityDatabaseVersion) criteria.uniqueResult();
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
			Criteria criteria = session.createCriteria(EntityDatabaseVersion.class).addOrder(Order.desc("passTime"));
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", publicId));
			criteria.add(Restrictions.or(Restrictions.eq("status", AuditStatus.HISTORY),
					  Restrictions.eq("status", AuditStatus.PUBLISH)));
			criteria.setFirstResult(0).setMaxResults(1);
			try {
				return ((EntityDatabaseVersion) criteria.uniqueResult()).getPassTime();
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
						"select count(*) from  EntityDatabaseVersion where publicId=:publicId and tenantId=:tenantId");
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

	public List<EntityDatabaseVersion> listAll(int tenantId) {
		return list(tenantId, false);
	}

	public List<EntityDatabaseVersion> list(int tenantId, boolean exceptPublish) {
		final List<EntityDatabaseVersion> entities = new ArrayList<>();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					sql.append(" select id,category,code,name,enabled,refKP,subEntities,entityValues,entityType,fromIndex,enabled,editorId, ");
					sql.append(" (select name From AdminUser u where d.editorId=u.id )as editorName,auditorId, ");
					sql.append(" (select name From AdminUser u where d.auditorId=u.id) as auditorName, ");
					sql.append(" message,status,action,createTime,updateTime,updateLog,passTime,publicId ");
					sql.append(" from EntityDatabaseVersion d where d.tenantId = ? ");
					if (exceptPublish) {
						sql.append("  AND d.status != 0 ");
					}
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						pstmt = conn.prepareStatement(sql.toString());
						pstmt.setInt(1, tenantId);
						rs = pstmt.executeQuery();
						while (rs.next()) {
							EntityDatabaseVersion edv = new EntityDatabaseVersion();
							edv.setId(rs.getLong("id"));
							edv.setCategory(rs.getString("category"));
							edv.setCode(rs.getString("code"));
							edv.setName(rs.getString("name"));
							edv.setRefKP(rs.getString("refKP"));
							edv.setSubEntities(rs.getString("subEntities"));
							QAEntityType entityType = QAEntityType.valueOf(getEntityKey(rs.getString("entityType")));
							edv.setEntityType(entityType);
							edv.setEntityValues(rs.getString("entityValues"));
							edv.setFromIndex(rs.getBoolean("fromIndex"));
							edv.setEnabled(rs.getBoolean("enabled"));		
							edv.setEditorId(rs.getInt("editorId"));
							edv.setEditorName(rs.getString("editorName"));
							edv.setAuditorId(rs.getInt("auditorId"));
							edv.setAuditorName(rs.getString("auditorName"));
							edv.setAction(AuditAction.values()[rs.getInt("action")]);
							edv.setStatus(AuditStatus.values()[rs.getInt("status")]);
							edv.setCreateTime(rs.getTimestamp("createTime"));
							edv.setUpdateTime(rs.getTimestamp("updateTime"));
							edv.setUpdateLog(rs.getString("updateLog"));
							edv.setPassTime(rs.getTimestamp("passTime"));
							edv.setMessage(rs.getString("message"));
							edv.setPublicId(rs.getLong("publicId"));
							entities.add(edv);
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

				private String getEntityKey(String entityType) {
					String key = "";
					switch (entityType) {
					case "0":
						key = "DATETIME";
						break;
					case "1":
						key = "NUMBER";
						break;
					case "2":
						key = "LOCATION";
						break;
					case "3":
						key = "STRING";
						break;
					case "4":
						key = "REGEXP";
						break;
					case "5":
						key = "CUSTOM";
						break;
					}
					return key;
				}
			});
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return entities;
	}

	public EntityDatabaseVersion getByPublicId(int tenantId, Long id) {
		EntityDatabaseVersion result = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			Criteria criteria = session.createCriteria(EntityDatabaseVersion.class);
			criteria.add(Restrictions.eq("tenantId", tenantId));
			criteria.add(Restrictions.eq("publicId", id));
			result = (EntityDatabaseVersion) criteria.uniqueResult();
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
