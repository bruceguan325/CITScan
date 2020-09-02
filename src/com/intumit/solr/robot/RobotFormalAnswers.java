package com.intumit.solr.robot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import com.hazelcast.core.ITopic;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.servlet.HazelcastUtil;
import com.vdurmont.emoji.EmojiParser;

import flexjson.JSONSerializer;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name="tenantUniqueKeyName", columnNames = {"keyName", "tenantId"}))
public class RobotFormalAnswers {

	public static final String SEPARATOR = "///";

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="keyNameIdx")
	private String keyName;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Lob
	private String answers;
	
	private boolean systemDefault;
	
	@Transient
	private List<String> answersList;

	public RobotFormalAnswers(Integer tenantId, String key, String answers) {
		this.tenantId = tenantId;
		this.keyName = key;
		this.answers = answers;
		this.answersList = trimList(Arrays.asList(StringUtils.splitByWholeSeparator(answers, SEPARATOR)));
	}
	public RobotFormalAnswers(Integer tenantId, String key, List<String> answersList) {
		this.tenantId = tenantId;
		this.keyName = key;
		this.answers = StringUtils.join(trimList(answersList), SEPARATOR);
		this.answersList = answersList;
	}
	public RobotFormalAnswers() {
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
	public void setKeyName(String column) {
		this.keyName = column;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setAnswers(String answers) {
		this.answers = answers;
		this.answersList = trimList(Arrays.asList(StringUtils.splitByWholeSeparator(answers, SEPARATOR)));
	}

	public void setAnswers(List<String> answersList) {
		this.answers = StringUtils.join(trimList(answersList), SEPARATOR);
		this.answersList = answersList;
	}

	public String getAnswers() {
		return answers;
	}

	public List<String> getAnswersList() {
		return answersList;
	}

	public boolean isSystemDefault() {
		return systemDefault;
	}

	public void setSystemDefault(boolean systemDefault) {
		this.systemDefault = systemDefault;
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}

	public static String giveMeRandomAnswer(Integer tenantId, String key, String defaultAnswer) {
//		List<String> formalAnswers = RobotFormalAnswers.getAnswers(tenantId, key);
		List<String> formalAnswers = RobotFormalAnswers.listDateFormalAnswer(tenantId, key);

		String ans = defaultAnswer;
		
		if (formalAnswers != null) {
			if (formalAnswers.size() > 0) {
				ans = formalAnswers.get(RandomUtils.nextInt(formalAnswers.size()));
			} else {
				ans = "";
			}
		}

		if (ans == null) {
			ans = "";
		}

		if (StringUtils.startsWith(ans, "__")) {
			String localeStr = StringUtils.substringBetween(ans, "__");

			ans = StringUtils.substringAfter(ans, "__" + localeStr + "__");
		}

		return EmojiParser.parseToUnicode(ans);
	}

	public static String giveMeRandomAnswer(QAContext qaCtx, String key, String defaultAnswer) {
//		List<String> allFormalAnswers = RobotFormalAnswers.getAnswers(qaCtx.getTenant().getId(), key);
		List<String> allFormalAnswers = RobotFormalAnswers.listDateFormalAnswer(qaCtx.getTenant().getId(), key);
		List<String> formalAnswers = null;

		if (qaCtx.getTenant().getEnableMultiLocale() && qaCtx.getLocale() != null) {
			formalAnswers = new ArrayList<>();
			String prefix = "__" + qaCtx.getLocale().name() + "__";

			for (String fa: allFormalAnswers) {
				if (StringUtils.startsWith(fa, prefix)) {
					formalAnswers.add(StringUtils.substringAfter(fa, prefix));
				}
			}
		}
		else {
			formalAnswers = allFormalAnswers;
		}


		String ans = defaultAnswer;
		
		if (formalAnswers != null) {
			if (formalAnswers.size() > 0) {
				ans = formalAnswers.get(RandomUtils.nextInt(formalAnswers.size()));
			}
			else {
				ans = "";
			}
		}

		if (ans == null) {
			ans = "";
		}

		if (StringUtils.startsWith(ans, "__")) {
			String localeStr = StringUtils.substringBetween(ans, "__");

			ans = StringUtils.substringAfter(ans, "__" + localeStr + "__");
		}

		return EmojiParser.parseToUnicode(ans);

	}

	private static final Pattern timePattern = Pattern.compile("\\[(\\d{4})(~(\\d{4}))?\\]");
	private static final DateFormat MMdd = new SimpleDateFormat("MMdd");

	private static List<String> listDateFormalAnswer(Integer tenantId, String key) {
		List<String> result = new ArrayList<String>();
		List<String> exactly = new ArrayList<String>();
		List<String> all = RobotFormalAnswers.getAnswers(tenantId, key);
		Integer today = Integer.parseInt(MMdd.format(new Date(System.currentTimeMillis())));

		if (all != null && all.size() > 0) {
			for(String answer : all) {
				Matcher answerWithDate = timePattern.matcher(answer);
				if(answerWithDate.find()) {
					Integer start = Integer.parseInt(answerWithDate.group(1));
					Integer end = null;
					if(answerWithDate.group(3) != null) {
						end = Integer.parseInt(answerWithDate.group(3));
						if(today >= start && today <= end) {
							result.add(EmojiParser.parseToUnicode(answerWithDate.replaceAll("")));
						}
					}
					else if(today.equals(start)){
						exactly.add(EmojiParser.parseToUnicode(answerWithDate.replaceAll("")));
					}
				}
				else {
					result.add(EmojiParser.parseToUnicode(answer));
				}
			}
		}
		if(exactly.size() > 0) return exactly;
		return result;
	}

	static Map<Integer, Map<String, RobotFormalAnswers>> keyVal = new HashMap<Integer, Map<String, RobotFormalAnswers>>();
	//static List<String> EMPTY_LIST = Arrays.asList(new String[0]);
	public static Map<String, RobotFormalAnswers> getKeyValueMap(Integer tenantId){
		if (!keyVal.containsKey(tenantId)) {
			reload(tenantId);
		}
		return keyVal.get(tenantId);
	}
	public static List<String> getAnswers(Integer tenantId, String key){
		if (!keyVal.containsKey(tenantId)) {
			reload(tenantId);
		}
		if(StringUtils.isEmpty(key)){
			return null;
		}
		return keyVal.get(tenantId).get(key).getAnswersList();
	}
	
	public static Map<String, RobotFormalAnswers> fullSearchBySQL(int tenantId, String query, AuditStatus status) {
		String searchKeyword = query.toLowerCase();
		Map<String, RobotFormalAnswers> map = new HashMap<String, RobotFormalAnswers>();
		RobotFormalAnswersVersionService service = RobotFormalAnswersVersionService.getInstance();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					StringBuilder sql = new StringBuilder();
					
					if (status != null) {					
						sql.append(" SELECT s.id, s.answers, s.keyName, s.systemDefault, s.tenantId ");
						sql.append(" FROM RobotFormalAnswers AS s ");
						sql.append(" WHERE s.tenantId = ? ");
					} else {
						sql.append(" SELECT s.id, s.answers, s.keyName, s.systemDefault, s.tenantId, v.status, v.passTime ");
						sql.append(" FROM RobotFormalAnswers AS s ");
						sql.append(" LEFT JOIN RobotFormalAnswersVersion AS v ");
						sql.append(" ON s.tenantId = v.tenantId and s.id = v.publicId ");
						sql.append(" WHERE s.tenantId = ? AND (v.version is null OR v.version = ");
						
						// 取最新版本，不包含審核中
						sql.append(" (SELECT TOP 1 v.version FROM RobotFormalAnswersVersion AS v ");
						sql.append(" WHERE s.tenantId = v.tenantId AND s.id = v.publicId AND v.publicId IS NOT null order by v.updateTime desc )) ");

						sql.append(" AND (CAST(s.id AS CHAR) = ? ");
						sql.append("   	  OR  s.keyName LIKE ? ");
						sql.append("   	  OR  s.answers LIKE ? ");
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
							RobotFormalAnswers ans = new RobotFormalAnswers();
							ans.setId(rs.getLong("id"));
							ans.setKeyName(rs.getString("keyname"));
							ans.setAnswers(rs.getString("answers"));
							ans.setTenantId(rs.getInt("tenantId"));
							
							if (status == AuditStatus.AUDIT) {
								if(service.publicIdInAudit(ans.getTenantId(), ans.getId()))
									map.put(ans.getKeyName(), ans);
							} else if(status == AuditStatus.HISTORY) {
								if(!service.publicIdInAudit(ans.getTenantId(), ans.getId()))
									map.put(ans.getKeyName(), ans);
							} else {
								map.put(ans.getKeyName(), ans);
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
		return map;
	}

	public static void saveMap(Integer tenantId, Map<String, List<String>> map){
		for(String key: map.keySet()){
			if(StringUtils.isEmpty(key)){
				continue;
			}
			List<String> value = map.get(key);
			value = trimList(value);

			if (value == null) {
				delete(tenantId,key);
			} else {
				save(tenantId, key, value);
			}
		}

		try {
            ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
            topic.publish(new RobotFormalAnswersEvent(tenantId));
        }
        catch (Exception e) {
            HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
        }
	}
	
	public static void saveMapForInit(Integer tenantId, Map<String, List<String>> map){
		for(String key: map.keySet()){
			if(StringUtils.isEmpty(key)){
				continue;
			}
			List<String> value = map.get(key);
			value = trimList(value);

			if (value == null || value.size() == 0) {
				delete(tenantId,key);
			} else {
				saveIfNotExist(tenantId, key, value, true);
			}
		}

		try {
            ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
            topic.publish(new RobotFormalAnswersEvent(tenantId));
        }
        catch (Exception e) {
            HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
        }
	}

	public static RobotFormalAnswers get(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (RobotFormalAnswers)ses.createCriteria(RobotFormalAnswers.class).add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("keyName", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}

	public static RobotFormalAnswers get(Integer tenantId, Long id){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (RobotFormalAnswers)ses.createCriteria(RobotFormalAnswers.class).add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("id", id)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	public static void delete(Integer tenantId, String key) {
		Session ses = null;
		Transaction tx = null;
		RobotFormalAnswers cn = get(tenantId, key);
		if(cn==null || cn.isSystemDefault()) return;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			try {
                ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
                topic.publish(new RobotFormalAnswersEvent(tenantId));
            }
            catch (Exception e) {
                HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
            }
		}
	}
	synchronized static public void reload(Integer tenantId) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			List result = ses.createCriteria(RobotFormalAnswers.class).add(Restrictions.eq("tenantId", tenantId)).list();
			Map<String, RobotFormalAnswers> map = new LinkedHashMap<String, RobotFormalAnswers>();
			for(Object o : result){
				RobotFormalAnswers cn = (RobotFormalAnswers)o;
				cn.setAnswers(cn.getAnswers());
				map.put(cn.getKeyName(), cn);
			}
			keyVal.put(tenantId, map);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	static void saveIfNotExist(Integer tenantId, String key, List<String> values) {
		saveIfNotExist(tenantId, key, values, false);
	}
	
	static void saveIfNotExist(Integer tenantId, String key, List<String> values, boolean systemDefault) {
		RobotFormalAnswers cn = get(tenantId, key);
		
		if(cn != null) return;
		
		cn = new RobotFormalAnswers(tenantId, key, values);
		cn.setSystemDefault(systemDefault);

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	static void save(Integer tenantId, String key, List<String> values, boolean reload){
		RobotFormalAnswers cn = get(tenantId, key);
		if (cn==null) {
			cn = new RobotFormalAnswers(tenantId, key, values);
		}
		else {
			cn.setAnswers(values);
		}

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			if(reload) {
                try {
                    ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
                    topic.publish(new RobotFormalAnswersEvent(tenantId));
                }
                catch (Exception e) {
                    HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
                }
            }
		}
	}
	
	public static RobotFormalAnswers add(Integer tenantId, String key, List<String> values) {
		return add(new RobotFormalAnswers(tenantId, key, values));
	}
	
	public static RobotFormalAnswers add(Integer tenantId, String key, String answers) {
		return add(new RobotFormalAnswers(tenantId, key, answers));
	}
	
	private static RobotFormalAnswers add(RobotFormalAnswers fa) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(fa);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			try {
				ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
				topic.publish(new RobotFormalAnswersEvent(fa.getTenantId()));
			} catch (Exception e) {
				HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
			}
		}
		return fa;
	}
	
	public static void update(Integer tenantId, Long id, List<String> values) {
		RobotFormalAnswers fa = get(tenantId, id);
		if (fa == null) {
			return;
		}
		fa.setAnswers(values);
		update(fa);
	}
	
	public static void update(Integer tenantId, Long id, String answers) {
		RobotFormalAnswers fa = get(tenantId, id);
		if (fa == null) {
			return;
		}
		fa.setAnswers(answers);
		update(fa);
	}
	
	private static void update(RobotFormalAnswers fa) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(fa);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			try {
				ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
				topic.publish(new RobotFormalAnswersEvent(fa.getTenantId()));
			} catch (Exception e) {
				HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
			}
		}
	}

	public static void delete(Integer tenantId, Long id) {
		Session ses = null;
		Transaction tx = null;
		RobotFormalAnswers cn = get(tenantId, id);
		if(cn==null || cn.isSystemDefault()) return;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			try {
                ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
                topic.publish(new RobotFormalAnswersEvent(tenantId));
            }
            catch (Exception e) {
                HazelcastUtil.log().error("Cannot publish robot-formal-answer reload message", e);
            }
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<RobotFormalAnswers> list(int tenantId) {
		List<RobotFormalAnswers> result = new ArrayList();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			result = ses
					.createCriteria(RobotFormalAnswers.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.addOrder(Order.asc("id")).list();
			return result;
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	public static synchronized void saveOrUpdate(RobotFormalAnswers p) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	static void save(Integer tenantId, String key, List<String> values){
		save(tenantId, key, values, true);
	}

	public static List<String> trimList(List<String> list) {
		List<String> newList = new ArrayList<String>();

		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			String s = iterator.next();
			if (StringUtils.trimToNull(s) != null) {
				newList.add(StringUtils.trim(s));
			}
		}
		return newList;
	}

	public synchronized static void init() {
        ITopic topic = HazelcastUtil.getTopic("robot-formal-answers");
        topic.addMessageListener(new RobotFormalAnswersMessageListener());
    }
}
