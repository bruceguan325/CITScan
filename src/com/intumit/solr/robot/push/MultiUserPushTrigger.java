package com.intumit.solr.robot.push;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
 * #TODO: 還沒實作，只是將 code 貼過來
 * 
 * 存放要推播給使用者訊息的地方
 * 1. 有條件（條件類型目前暫訂支援「時間」、「SQL」跟「Script」）
 * 2. PUSH 的內容可以有「直接文字」跟「指定QA（Kid）」
 * 3. 可以指定頻道（單一、多選、全部或者 by 用戶設定）
 * 4. 可指定全部用戶或者看欄位內設定（逗號分隔）
 * 4. 可指定通知自刪條件（通知幾次後自刪 或 幾月幾號之後自刪）
 * 5. 有此 Queue 是否已完成的狀態紀錄
 * 
 * @author herb
 */
public class MultiUserPushTrigger extends UserPushTrigger implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	public enum Status { inqueue, suspended, done, }
	public enum CriteriaType { datetime, sql, script, }
	public enum TargetUsersType { all, byConfig }
	public enum StopCriteriaType { datetime, maxTimes }

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Enumerated(EnumType.STRING)
	@Index(name = "targetUsersType")
	private TargetUsersType targetUsersType;
	@Lob
	private String targetUsers;
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public static synchronized MultiUserPushTrigger get(long id) {
		try {
			return (MultiUserPushTrigger)HibernateUtil.getSession().get(MultiUserPushTrigger.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static List<MultiUserPushTrigger> getByStatus(int tenantId, Status pushStatus) {
		List<MultiUserPushTrigger> list = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SingleUserPushTrigger.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("pushStatus", pushStatus));
						
			list = (List<MultiUserPushTrigger>) ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return list;
	}
	
	public static boolean saveOrUpdate(MultiUserPushTrigger clue) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			clue.updateTime = now;
			
			if (clue.id == null || clue.createdTime == null) {
				clue.createdTime = now;
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

	public TargetUsersType getTargetUsersType() {
		return targetUsersType;
	}

	public void setTargetUsersType(TargetUsersType targetUsersType) {
		this.targetUsersType = targetUsersType;
	}

	public String getTargetUsers() {
		return targetUsers;
	}

	public void setTargetUsers(String targetUsers) {
		this.targetUsers = targetUsers;
	}

	@Override
	public String toString() {
		return "MultiUserPushTrigger [" + (id != null ? "id=" + id + ", " : "") + (tenantId != null ? "tenantId=" + tenantId + ", " : "")
				+ (pushStatus != null ? "pushStatus=" + pushStatus + ", " : "") + (pushDataId != null ? "pushDataId=" + pushDataId + ", " : "")
				+ (criteriaType != null ? "criteriaType=" + criteriaType + ", " : "") + (pushCriteria != null ? "pushCriteria=" + pushCriteria + ", " : "")
				+ (targetUsersType != null ? "targetUsersType=" + targetUsersType + ", " : "")
				+ (targetUsers != null ? "targetUsers=" + targetUsers + ", " : "")
				+ (stopCriteriaType != null ? "stopCriteriaType=" + stopCriteriaType + ", " : "")
				+ (stopCriteria != null ? "stopCriteria=" + stopCriteria + ", " : "") + (createdTime != null ? "createdTime=" + createdTime + ", " : "")
				+ (updateTime != null ? "updateTime=" + updateTime : "") + "]";
	}

}
