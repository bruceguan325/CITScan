package com.intumit.syslog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.tenant.Tenant;

@Entity
public class OperationLogEntity {
	
	// action for save
	public static final String SAVE = "save";
	public static final String CREATE = "create";
	public static final String COPY = "copy";
	public static final String POST = "post";
	// action for update
	public static final String UPDATE = "update";
	public static final String EDIT = "edit";
	public static final String PUT = "put";
	public static final String UPDATE_SELECT = "updateselected";
	// action for delete
	public static final String DELETE = "delete";
	public static final String CLEAR = "clear";
	// action for test
	public static final String TEST = "test";
	// action for sudo
	public static final String SUDO = "sudo";
	// action for in/out
	public static final String EXPORT = "export";
	public static final String IMPORT = "import";
	// action for in/out
	public static final String LOAD = "load";
	public static final String KEY_OPERATION_LOG_ID= "opLogId";
	private static final HashMap<String, String> paths = new HashMap<>();
	private static final List<String> actions = new ArrayList<String>();
	
    private static final HashMap<String, String> restPaths = new HashMap<>();
    private static final List<String> restActions = new ArrayList<String>();

	static {
		paths.put("/syn", "global.synonyms");
		paths.put("/dict", "global.dictionary");
		paths.put("/intent", "intent.management");
		paths.put("/entity", "entity.management");
		//paths.put("/wivoEntry", "wivoEntry.management");
		paths.put("/ambiguity", "global.compulsory.break.word");
		//paths.put("/eventType", "eventType.management");
		
		paths.put("/qaDataSave.jsp", "top.qa");
		paths.put("/qaAdmin.jsp", "top.qa");
		paths.put("/qaDataSave2.jsp", "top.life.langue");
		paths.put("/qaAdmin2.jsp", "top.life.langue");
		//paths.put("/qaChannelHotSave.jsp", "global.hot.problems");
		paths.put("/qaFormalAnswer.jsp", "qa.formal.answer");
		paths.put("/qaFormalAnswerSticker.jsp", "qa.formal.answer.sticker");
		paths.put("/qaPatternSave.jsp", "global.special.answer");
		//paths.put("/qaPatternDel.jsp", "global.special.answer");
		paths.put("/qaSegBatchList.jsp", "global.batch.segment.change");
		//paths.put("/qaAltTemplateSave.jsp", "alt.template");
		//paths.put("/line/qaLiffApp-ajax.jsp", "qa.liffapp.navbar.line");
		
		paths.put("/processData", "qa.import");
        paths.put("/SynonymVersionServlet", "synonym.audit");
		paths.put("/SynonymVersionServlet/add", "synonym.audit");
		paths.put("/SynonymVersionServlet/delete", "synonym.audit");
		paths.put("/synUploadFile", "synonym.import");
        paths.put("/DictionaryVersionServlet", "dictionary.audit");
		paths.put("/DictionaryVersionServlet/add", "dictionary.audit");
		paths.put("/DictionaryVersionServlet/delete", "dictionary.audit");
		paths.put("/dicUploadFile", "dictionary.import");
        paths.put("/RobotFormalAnswersVersionServlet", "formal.audit");
		paths.put("/RobotFormalAnswersVersionServlet/add", "formal.audit");
		paths.put("/RobotFormalAnswersVersionServlet/delete", "formal.audit");
        paths.put("/EntityVersionServlet", "entity.audit");
		paths.put("/EntityVersionServlet/add", "entity.audit");
		paths.put("/EntityVersionServlet/delete", "entity.audit");
		paths.put("/qaEntityUploadFile", "entity.import");
		paths.put("/webLine/qaRichMessageUploadFile", "qa.richmessage.import.webline");
		//restPaths.put("/webRM/richMessages", "qa.richmessage.navbar.web");
		restPaths.put("/webLine/richMessages", "qa.richmessage.navbar.webline");
		
		actions.add("save");
		actions.add("update");
		actions.add("copy");
		actions.add("delete");
		actions.add("test");
		actions.add("reject");
		actions.add("pass");
		actions.add("cancel");
		actions.add("check");
		actions.add("import");
		
        restActions.add("POST");
        restActions.add("PUT");
        restActions.add("DELETE");
	}

	public enum Status {
		FAILED("global.failed"), SUCCESS("global.success");
		String title;

		Status(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	private Integer tenantId;

	@Index(name = "statusMessageIdx")
	private Status statusMessage;

	@Column(length = 64)
	@Index(name = "namespaceIdx")
	private String namespace;

	@Column(length = 64)
	@Index(name = "eventIdx")
	private String event;

	@Column(length = 64)
	@Index(name = "clientIpIdx")
	private String clientIp;

	@Column(length = 64, name = "logIdentity")
	@Index(name = "identityIdx")
	private String identity;

	@Lob
	private String parameters;

	@Lob
	private String moreDetails;

	@Index(name = "timestampIdx")
	private Date timestamp;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getMoreDetails() {
		return moreDetails;
	}

	public void setMoreDetails(String moreDetails) {
		this.moreDetails = moreDetails;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public OperationLogEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Status getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(Status statusMessage) {
		this.statusMessage = statusMessage;
	}

	public static synchronized List<OperationLogEntity> listBy(Integer tenantId, int start, int rows, String targetUser,
			String targetPath, String targetEvent, String targetStatus, String targetStart, String targetEnd) {
		List<OperationLogEntity> result = new ArrayList<OperationLogEntity>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(OperationLogEntity.class).addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (targetUser != null) {
				ct.add(Restrictions.eq("identity", "AdminUser:" + targetUser));
			}
			if (targetPath != null) {
				ct.add(Restrictions.eq("namespace", targetPath));
			}
			if (targetEvent != null) {
				ct.add(Restrictions.eq("event", targetEvent));
			}
			if (targetStatus != null) {
				if(targetStatus.equals("NONE")) {
					ct.add(Restrictions.isNull("statusMessage"));
				} else {
					ct.add(Restrictions.eq("statusMessage", Status.valueOf(targetStatus)));
				}
			}
			if (targetStart != null && targetEnd == null) {
				ct.add(Restrictions.ge("timestamp", sdf.parse(targetStart)));
			} else if (targetStart == null && targetEnd != null) {
				ct.add(Restrictions.le("timestamp", sdf.parse(targetEnd)));
			} else if (targetStart != null && targetEnd != null) {
				ct.add(Restrictions.and(Restrictions.ge("timestamp", sdf.parse(targetStart)),
						Restrictions.lt("timestamp", sdf.parse(targetEnd))));
			}

			result = ct.setFirstResult(start).setMaxResults(rows).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized Number countBy(Integer tenantId, String targetUser,
			String targetPath, String targetEvent, String targetStatus, String targetStart, String targetEnd) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(OperationLogEntity.class);
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (targetUser != null) {
				ct.add(Restrictions.eq("identity", "AdminUser:" + targetUser));
			}
			if (targetPath != null) {
				ct.add(Restrictions.eq("namespace", targetPath));
			}
			if (targetEvent != null) {
				ct.add(Restrictions.eq("event", targetEvent));
			}
			if (targetStatus != null) {
				ct.add(Restrictions.eq("statusMessage", Status.valueOf(targetStatus)));
			}
			if (targetStart != null && targetEnd == null) {
				ct.add(Restrictions.ge("timestamp", sdf.parse(targetStart)));
			} else if (targetStart == null && targetEnd != null) {
				ct.add(Restrictions.le("timestamp", sdf.parse(targetEnd)));
			} else if (targetStart != null && targetEnd != null) {
				ct.add(Restrictions.and(Restrictions.ge("timestamp", sdf.parse(targetStart)),
						Restrictions.lt("timestamp", sdf.parse(targetEnd))));
			}
			tx.commit();
			return (Number) ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return 0;
	}

	public static synchronized void save(OperationLogEntity log) {
		Session ses = null;
		Transaction tx = null;
		try {
			log.setTimestamp(Calendar.getInstance().getTime());

			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(log);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static void setIdentitesAndSave(HttpServletRequest req, OperationLogEntity log) {
		String ip = SolrDispatchFilter.getClientIpAddr(req);
		String identity = null;

		String reqUri = req.getRequestURI();
		String admPath = req.getContextPath() + "/wiseadm";

		if (StringUtils.startsWithIgnoreCase(reqUri, admPath)) {
			AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(req.getSession());
			if (user == null) {
				identity = null;
			} else {
				identity = "AdminUser:" + user.getName();
			}
		}
		Tenant tenant = Tenant.getFromSession(req.getSession());
		if (tenant != null) {
			log.setTenantId(tenant.getId());
		}

		log.setClientIp(ip);
		if (identity != null)
			log.setIdentity(identity);
		log.appendToMoreDetails(req.getHeader("User-Agent"));

		save(log);
	}

	public void appendToMoreDetails(String more) {
		if (StringUtils.isEmpty(moreDetails)) {
			moreDetails = "";
		}
		moreDetails += (moreDetails.isEmpty() ? "" : "\n") + more;
	}

	public static OperationLogEntity log(HttpServletRequest req, String namespace, String event, String param,
			Status statusMessage) {
		OperationLogEntity log = new OperationLogEntity();
		log.setNamespace(namespace);
		log.setEvent(event);
		log.setParameters(param);
		log.setStatusMessage(statusMessage);
		setIdentitesAndSave(req, log);
		return log;
	}
	
	public static List<String> getActions() {
		return actions;
	}
	
    public static List<String> getRestActions() {
        return restActions;
    }

	public static String getPath(String path) {
		if (paths.containsKey(path)) {
			return paths.get(path);
		}
		return null;
	}
	
    public static String getRestPath(String restPath) {
        if (restPaths.containsKey(restPath)) {
            return restPaths.get(restPath);
        }
        return null;
    }

	public static Boolean isPathExist(String path) {
		if (paths.containsKey(path)) {
			return true;
		}
		return false;
	}
	
    public static Boolean isRestPathExist(String restPath) {
        for (String key : restPaths.keySet()) {
            if (restPath.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

	public static HashMap<String, String> getPaths() {
		return paths;
	}
	
    
    public static HashMap<String, String> getRestPaths() {
        return restPaths;
    }

	public static synchronized OperationLogEntity get(int id) {
		try {
			return (OperationLogEntity) HibernateUtil.getSession().get(OperationLogEntity.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public void update() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.update(this);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

}
