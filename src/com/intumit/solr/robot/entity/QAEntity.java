package com.intumit.solr.robot.entity;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.robot.EventCenter;

@Entity
@Table(name = "EntityDatabase")
public class QAEntity implements Serializable, Cloneable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="category")
	private String category;
	
	@Index(name="name")
	private String name;

	@Index(name="code")
	private String code;
	
	@Index(name="refKP")
	@Column(length=128)
	private String refKP;

	@Index(name="subEntities")
	@Lob
	private String subEntities;
	
	private QAEntityType entityType;

	@Index(name="entityValues")
	@Lob
	private String entityValues;

	@Index(name="fromIndex")
	private Boolean fromIndex;

	@Index(name="enabled")
	private boolean enabled;
	
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
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = StringUtils.trim(category);
	}
	public String getCode() {
		return code;
	}
	public void setCode(String tag) {
		this.code = StringUtils.trim(tag);
	}
	
	public String getRefKP() {
		return StringUtils.trimToEmpty(refKP);
	}
	
	public void setRefKP(String refKP) {
		this.refKP = StringUtils.trimToEmpty(refKP);
	}
	
	public String getSubEntities() {
		return subEntities;
	}
	public void setSubEntities(String subTags) {
		this.subEntities = StringUtils.trim(subTags);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = StringUtils.trim(name);
	}
	public QAEntityType getEntityType() {
		return entityType;
	}
	public void setEntityType(QAEntityType valueType) {
		this.entityType = valueType;
	}
	public String getEntityValues() {
		return entityValues;
	}
	public void setEntityValues(String entityValues) {
		this.entityValues = StringUtils.trim(entityValues);
	}
	public Boolean getFromIndex() {
		return fromIndex != null ? fromIndex : false;
	}
	public void setFromIndex(Boolean fromIndex) {
		this.fromIndex = fromIndex;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	
	@Override
	public String toString() {
		return "QAEntity [id=" + id + ", tenantId=" + tenantId
				+ ", category=" + category + ", name=" + name + ", code="
				+ code + ", subEntities=" + subEntities + ", entityType="
				+ entityType + ", entityValues=" + entityValues + ", enabled=" + enabled
				+ ", fromIndex=" + fromIndex
				 + "]";
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
		QAEntity other = (QAEntity) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	public QAEntity getCopy() {
		return getCopy(new QAEntity());
	}
	public QAEntity getCopy(QAEntity copyTo) {
		copyTo.id = id;
		copyTo.category = category;
		copyTo.code = code;
		copyTo.name = name;
		copyTo.subEntities = subEntities;
		copyTo.entityType = entityType;
		copyTo.entityValues = entityValues;
		copyTo.fromIndex = fromIndex;
		copyTo.enabled = enabled;
		return copyTo;
	}
	
	public static boolean isValidCodeOrCategory(String str) {
		return str.matches("^[a-zA-Z]+$");
	}
	public static synchronized QAEntity get(long id) {
		try {
			return (QAEntity)HibernateUtil.getSession().get(QAEntity.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized QAEntity save(Integer tenantId, String category, String tag, String name, String subEntities, QAEntityType entityType, String values, boolean fromIndex, boolean enabled) {
		Session ses = null;
		Transaction tx = null;
		QAEntity word = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			word = new QAEntity();
			word.setTenantId(tenantId);
			word.setCategory(category);
			word.setCode(tag);
			word.setName(name);
			word.setFromIndex(fromIndex);
			word.setSubEntities(subEntities);
			word.setEntityType(entityType);
			word.setEntityValues(values.toLowerCase());
			word.setEnabled(enabled);
			ses.saveOrUpdate(word);
			tx.commit();
			
			EventCenter.fireEvent(QAEntity.class.getName(), tenantId, "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return word;
	}
	
	public static synchronized void save(Integer tenantId, String category, String tag, String name, String refKP, String subEntities, QAEntityType entityType, String values, boolean fromIndex, boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAEntity word = new QAEntity();
			word.setTenantId(tenantId);
			word.setCategory(category);
			word.setCode(tag);
			word.setName(name);
			word.setRefKP(refKP);
			word.setFromIndex(fromIndex);
			word.setSubEntities(subEntities);
			word.setEntityType(entityType);
			word.setEntityValues(values.toLowerCase());
			word.setEnabled(enabled);
			ses.saveOrUpdate(word);
			tx.commit();
			EventCenter.fireEvent(QAEntity.class.getName(), tenantId, "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void update(Integer tenantId, String id, String category, String code, String name, String subEntities, QAEntityType entityType, String values,
			boolean fromIndex, boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAEntity oldWord = get(lid);
			QAEntity newWord = get(lid);

			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(lid);
				newWord.setCategory(category);
				newWord.setCode(code);
				newWord.setName(name);
				newWord.setFromIndex(fromIndex);
				newWord.setEnabled(enabled);
				newWord.setSubEntities(subEntities);
				newWord.setEntityType(entityType);
				newWord.setEntityValues(values.toLowerCase());

				ses.update(newWord);
				tx.commit();
				
				EventCenter.fireEvent(QAEntity.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void update(Integer tenantId, String id, String category, String code, String name, String refKP, String subEntities, QAEntityType entityType, String values,
			boolean fromIndex, boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAEntity newWord = get(lid);

			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(lid);
				newWord.setCategory(category);
				newWord.setCode(code);
				newWord.setName(name);
				newWord.setRefKP(refKP);
				newWord.setFromIndex(fromIndex);
				newWord.setEnabled(enabled);
				newWord.setSubEntities(subEntities);
				newWord.setEntityType(entityType);
				newWord.setEntityValues(values.toLowerCase());

				ses.update(newWord);
				tx.commit();
				EventCenter.fireEvent(QAEntity.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void saveOrUpdate(QAEntity e) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(e);
			tx.commit();
			
			EventCenter.fireEvent(QAEntity.class.getName(), e.getTenantId(), "reload", null);
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
			QAEntity word = get(lid);

			if (word != null) {
				ses.delete(word);
				tx.commit();
				
				EventCenter.fireEvent(QAEntity.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized QAEntity get(Integer tenantId, String code, String category, String name) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(QAEntity.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("code"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (code != null) {
				ct.add(Restrictions.eq("code", code));
			}

			if (category != null) {
				ct.add(Restrictions.eq("category", category));
			}
			
			if (name != null) {
				ct.add(Restrictions.eq("name", name));
			}

			return (QAEntity)ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static synchronized QAEntity get(Integer tenantId, String sentence) {
		List<QAEntity> l = list(tenantId, sentence, null, true, null);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<QAEntity> listAll() {
		return list(null, null, null, false, null);
	}

	public static List<QAEntity> listByTenantId(Integer tenantId) {
		return list(tenantId, null, null, false, null);
	}

	public static List<QAEntity> listByTenantIdAndCategory(Integer tenantId, String qaCategory) {
		return listByCategory(tenantId, qaCategory, false, true);
	}

	public static synchronized List<QAEntity> search(Integer tenantId, String searchKeyword) {
		return fullSearch(tenantId, searchKeyword);
	}

	private static List listByCategory(Integer tenantId, String qaCategory, boolean fullMatch, Boolean enabled) {
		String searchCategory = StringUtils.lowerCase(qaCategory);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEntity.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("code"));

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
	
	public static List fullSearch(Integer tenantId, String query) {
		String searchKeyword = query.toLowerCase();
		List result = null;
		
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			Criterion c1 = (Restrictions.like("entityValues", searchKeyword, MatchMode.ANYWHERE));
			Criterion c2 = (Restrictions.like("name", searchKeyword, MatchMode.ANYWHERE));
			Criterion c3 = (Restrictions.like("code", searchKeyword, MatchMode.ANYWHERE));
			LogicalExpression orExp = Restrictions.or(c1, c2);
			LogicalExpression secondExp = Restrictions.or(orExp, c3);
			ct.add(secondExp);
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
	
	public static List<QAEntity> fullSearchBySQL(int tenantId, String query, AuditStatus status) {
		String searchKeyword = query.toLowerCase();
		List result = new ArrayList();
		EntityVersionService service = EntityVersionService.getInstance();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					
					if (status != null) {
						sql.append(" SELECT s.id, s.category, s.code, s.name, s.subEntities, s.entityType, s.entityValues, s.tenantId ");
						sql.append(" FROM EntityDatabase AS s ");
						sql.append(" WHERE s.tenantId = ? ");
					}else {
						sql.append(" SELECT s.id, s.category, s.code, s.name, s.subEntities, s.entityType, s.entityValues, s.tenantId, v.status, v.passTime ");
						sql.append(" FROM EntityDatabase AS s ");
						sql.append(" LEFT JOIN EntityDatabaseVersion AS v ");
						sql.append(" ON s.tenantId = v.tenantId and s.id = v.publicId ");
						sql.append(" WHERE s.tenantId = ? AND (v.version is null OR v.version = ");
						
						// 取最新版本，不包含審核中
						sql.append(" (SELECT TOP 1 v.version FROM EntityDatabaseVersion AS v ");
						sql.append(" WHERE s.tenantId = v.tenantId AND s.id = v.publicId AND v.passTime IS NOT null order by v.updateTime desc )) ");
						
						sql.append(" AND (CAST(s.id AS CHAR) = ? ");
						sql.append("   	  OR  s.category LIKE ? ");
						sql.append("   	  OR  s.code LIKE ? ");
						sql.append("   	  OR  s.name LIKE ? ");
						sql.append("   	  OR  s.entityValues LIKE ? ");
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
							pstmt.setString(5, "%" + searchKeyword + "%");
							pstmt.setString(6, "%" + searchKeyword + "%");
							pstmt.setString(7, searchKeyword);	
						}
						
						rs = pstmt.executeQuery();
						while (rs.next()) {
							QAEntity en = new QAEntity();
							en.setId(rs.getLong("id"));
							en.setCategory(rs.getString("category"));
							en.setCode(rs.getString("code"));
							en.setName(rs.getString("name"));
							en.setSubEntities(rs.getString("subEntities"));
							QAEntityType entityType = QAEntityType.valueOf(getEntityKey(rs.getString("entityType")));
							en.setEntityType(entityType);
							en.setEntityValues(rs.getString("entityValues"));
							en.setTenantId(rs.getInt("tenantId"));
							
							if (status == AuditStatus.AUDIT) {
								if(service.publicIdInAudit(en.getTenantId(), en.getId()))
									result.add(en);
							} else if(status == AuditStatus.HISTORY) {
								if(!service.publicIdInAudit(en.getTenantId(), en.getId()))
									result.add(en);
							} else {
								result.add(en);
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
		return result;
	}

	private static List list(Integer tenantId, String query, Boolean fromIndex, boolean fullMatch, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEntity.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("code"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(fullMatch
								? Restrictions.eq("code", searchKeyword)
								: Restrictions.like("code", searchKeyword, MatchMode.ANYWHERE)
							);
			}
			
			if (fromIndex != null) {
				if (fromIndex) {
					ct.add(Restrictions.eq("fromIndex", fromIndex));
				}
				else {
					ct.add(Restrictions.or(
							Restrictions.isNull("fromIndex"),
							Restrictions.eq("fromIndex", fromIndex)
							));
				}
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("code", searchKeyword)
									: Restrictions.like("code", searchKeyword, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
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
	
	public static Map<String, QAEntity> collToMap(Collection<QAEntity> l, List<String> retainThoseCategories) {
		Map<String, QAEntity> m = new HashMap<>();

		if (l != null)
		for (QAEntity i: l) {
			if (retainThoseCategories != null) {
				if (retainThoseCategories.contains(i.getCategory())) {
					m.put(i.getCode(), i);
				}
			}
			else {
				m.put(i.getCode(), i);
			}
		}
		
		return m;
	}

	public static void delete(QAEntity entity) {
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
