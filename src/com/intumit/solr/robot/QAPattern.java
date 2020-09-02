package com.intumit.solr.robot;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.robot.dictionary.CustomData;

@Entity
public class QAPattern implements Serializable {
	public enum DataSource {
		OPENDATA,
		LOCAL,
	}

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="tenantIdIdx")
	Integer tenantId;
	
	@Column(length = 8)
	@Index(name="mkeyIdIdx")
	String mkey;

	@Column(length = 512)
	String questionTemplate;
	
	@Column(length = 128)
	String dataAggregator;
	
	DataSource dataSource;
	
	@Lob
	String answerTemplate;
	
	@Lob
	String answerVoice;
	
	int maxMatched;

	@Lob
	String specialRestriction;
	
	@Lob
	String previewParameters;
	
	@Lob
	String staticAnswer;
	
	@Column(length = 128)
	String answerPeriod;
	
	public QAPattern() {
		super();
	}
	
	public QAPattern(String questionTemplate, String dataAggregator, String answerTemplate, String answerVoice,
			int maxMatched,	String specialRestriction, String staticAnswer) {
		super();
		this.questionTemplate = questionTemplate;
		this.answerTemplate = answerTemplate;
		this.answerVoice = answerVoice;
		this.dataAggregator = dataAggregator;
		this.maxMatched = maxMatched;
		this.specialRestriction = specialRestriction;
		this.staticAnswer = staticAnswer;
	}
	
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	public String getMkey() {
		return mkey;
	}
	public void setMkey(String mkey) {
		this.mkey = mkey;
	}

	public DataSource getDataSource() {
		return dataSource != null ? dataSource : DataSource.LOCAL;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getQuestionTemplate() {
		return questionTemplate;
	}
	public void setQuestionTemplate(String questionTemplate) {
		this.questionTemplate = questionTemplate;
	}
	public String getDataAggregator() {
		return dataAggregator;
	}
	public void setDataAggregator(String dataAggregator) {
		this.dataAggregator = dataAggregator;
	}
	public String getAnswerTemplate() {
		return answerTemplate;
	}
	public void setAnswerTemplate(String answerTemplate) {
		this.answerTemplate = answerTemplate;
	}
	public String getAnswerVoice() {
		return answerVoice;
	}
	public void setAnswerVoice(String answerVoice) {
		this.answerVoice = answerVoice;
	}
	public int getMaxMatched() {
		return maxMatched;
	}
	public void setMaxMatched(int maxMatched) {
		this.maxMatched = maxMatched;
	}
	public String getSpecialRestriction() {
		return specialRestriction;
	}
	public void setSpecialRestriction(String specialRestriction) {
		this.specialRestriction = StringUtils.trimToNull(specialRestriction);
	}
	public boolean hasSpecialRestriction() {
		return StringUtils.isNotEmpty(specialRestriction);
	}
	public String getAnswerPeriod() {
		return answerPeriod;
	}
	public void setAnswerPeriod(String answerPeriod) {
		this.answerPeriod = answerPeriod;
	}
	public boolean addSpecialRestriction(QA customQa, QAContext qaCtx, List<CustomData> nvPairs, SolrQuery query) {
		boolean dirty = false;
		
		if (hasSpecialRestriction()) {
			String lines[] = applySpecialRestrictionTemplate(customQa, qaCtx, nvPairs).split("\\r?\\n");
			
			if (lines.length == 1 && lines[0].indexOf("=") == -1) {
				query.addFilterQuery(lines[0]);
				dirty = true;
			}
			else {
				for (String line: lines) {
					line = StringUtils.trimToNull(line);
					
					if (line == null) continue;
					
					if (line.startsWith("fq=")) {
						query.addFilterQuery(StringUtils.substringAfter(line, "fq="));
						dirty = true;
					}
					else if (line.startsWith("sort=")) {
						String sortStr = StringUtils.substringAfter(line, "sort=");
						if (sortStr.indexOf(" ") != -1 && StringUtils.endsWithAny(sortStr, new String[] {"asc", "desc"})) {
							query.addSort(
									SortClause.create(
											StringUtils.substringBeforeLast(sortStr, " "), 
											StringUtils.substringAfterLast(sortStr, " ")));
						}
						else {
							query.addSort(SortClause.asc(sortStr));
						}
						dirty = true;
					}
				}
			}
		}
		return dirty;
	}

	public String applySpecialRestrictionTemplate(QA customQa, QAContext qaCtx, List<CustomData> nvPairs) {
		StringWriter buf = new StringWriter();
		
		try {
			//取得velocity的上下文context
			VelocityContext context = new VelocityContext();
			
			//把數據填入上下文
			context.put("question", qaCtx.getCurrentQuestion());
			context.put("qaCtx", qaCtx);
			context.put("math", new MathTool());
			context.put("num", new NumberTool());
			context.put("colMapper", ColumnNameMappingFacade.getInstance());

			//轉換輸出
			VelocityEngine ve = new VelocityEngine();

			// Make Velocity log to Log4J
			ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
			"org.apache.velocity.runtime.log.SimpleLog4JLogSystem");

			ve.setProperty("runtime.log.logsystem.log4j.category",
					this.getClass().getName());

			ve.init();
			ve.evaluate(context, buf, this.getClass().getName(), getSpecialRestriction());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buf.toString();
	}
	
	
	public boolean isStatic() {
		return StringUtils.isNotEmpty(staticAnswer);
	}
	public String getStaticAnswer() {
		return staticAnswer;
	}
	public void setStaticAnswer(String staticAnswer) {
		this.staticAnswer = staticAnswer;
	}

	public List<String> getMltFieldNamesInQuestion() {
		Pattern p = Pattern.compile("%\\{\\+?(.*?)\\}");
		Matcher m = p.matcher(questionTemplate);
		
		List<String> fns = new ArrayList<String>();
		while (m.find()) {
			String fn = m.group(1);
			fns.add(fn + "_t");
			fns.add(fn + "_mt");
		}
		return fns;
	}

	public String getPreviewParameters() {
		return previewParameters;
	}

	public void setPreviewParameters(String previewParamters) {
		this.previewParameters = previewParamters;
	}
	
	public Map<String, List<String>> getPreviewParameterMap() {
		Map<String, List<String>> map = new HashMap<>();
		String[] clauses = StringUtils.trimToEmpty(previewParameters).split("\\r?\\n");
		String query = "";
		for (int ccc = 0; ccc < clauses.length; ccc++) {
			String c = clauses[ccc].trim();
			if (c.length() == 0)
				continue;
			String key = c.substring(0, c.indexOf("="));
			String val = c.substring(c.indexOf("=") + 1);

			if (!map.containsKey(key)) {
				map.put(key, new ArrayList<String>());
			}
			map.get(key).add(val);
		}
		return map;
	}

	@Override
	public String toString() {
		return "QAPattern [tenantId="+tenantId+", id=" + id + ", questionTemplate=" + questionTemplate + ", dataAggregator=" + dataAggregator
				+ ", maxMatched=" + maxMatched + ", specialRestriction=" + specialRestriction + "]";
	}

	// DAO
	public static synchronized void save(QAPattern p) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
			QAUtil.cleanInstance(p.getTenantId());
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void saveOrUpdate(QAPattern p) {
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

	public static synchronized void delete(Integer tenantId, int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			QAPattern p = get(id);
			if (p.getTenantId() != tenantId) return;
			
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + QAPattern.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
			QAUtil.cleanInstance(p.getTenantId());
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized List<QAPattern> list(Integer tenantId) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAPattern.class)
					.add(Restrictions.eq("tenantId", tenantId)).addOrder(Order.asc("id"));
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
	public static synchronized QAPattern get(int id) {
		try {
			return (QAPattern)HibernateUtil.getSession().get(QAPattern.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	public static QAPattern getByKey(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (QAPattern)ses.createCriteria(QAPattern.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("mkey", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
}
