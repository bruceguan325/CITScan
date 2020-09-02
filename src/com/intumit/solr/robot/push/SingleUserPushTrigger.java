package com.intumit.solr.robot.push;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
/**
 * 存放要推播給單一使用者訊息的地方
 * 1. 有條件（條件類型目前暫訂支援「時間」、「SQL」跟「Script」）
 * 2. PUSH 的內容可以有「直接文字」跟「指定QA（Kid）」
 * 3. 可以指定頻道（單一、多選、全部或者 by 用戶設定）
 * 4. 可指定通知自刪條件（通知幾次後自刪 或 幾月幾號之後自刪）
 * 5. 有此 Queue 是否已完成的狀態紀錄
 * 
 * @author herb
 */
public class SingleUserPushTrigger extends UserPushTrigger implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Index(name = "userClueId")
	private Long userClueId;

	public static synchronized SingleUserPushTrigger get(long id) {
		try {
			return (SingleUserPushTrigger)HibernateUtil.getSession().get(SingleUserPushTrigger.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static List<SingleUserPushTrigger> getByStatus(int tenantId, Status pushStatus) {
		return getByStatus(tenantId, null, pushStatus);
	}
	
	public static List<SingleUserPushTrigger> getByStatus(int tenantId, String namespace, Status pushStatus) {
		List<SingleUserPushTrigger> list = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SingleUserPushTrigger.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("pushStatus", pushStatus));
			
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
			}
						
			list = (List<SingleUserPushTrigger>) ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return list;
	}
	
	public static boolean saveOrUpdate(SingleUserPushTrigger clue) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			clue.setUpdateTime(now);
			
			if (clue.id == null || clue.getCreatedTime() == null) {
				clue.setCreatedTime(now);
			}
			ses.saveOrUpdate(clue);
			tx.commit();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return success;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserClueId() {
		return userClueId;
	}

	public void setUserClueId(Long userClueId) {
		this.userClueId = userClueId;
	}

	@Override
	public String toString() {
		return "SingleUserPushTrigger [" + (id != null ? "id=" + id + ", " : "") + (userClueId != null ? "userClueId=" + userClueId + ", " : "")
				+ (pushStatus != null ? "pushStatus=" + pushStatus + ", " : "") + (criteriaType != null ? "criteriaType=" + criteriaType + ", " : "")
				+ (pushCriteria != null ? "pushCriteria=" + pushCriteria + ", " : "")
				+ (stopCriteriaType != null ? "stopCriteriaType=" + stopCriteriaType + ", " : "")
				+ (stopCriteria != null ? "stopCriteria=" + stopCriteria + ", " : "") + (triggeredTimes != null ? "triggeredTimes=" + triggeredTimes : "")
				+ "]";
	}

}
