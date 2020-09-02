
package com.intumit.solr.robot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.util.Calendar;
import com.intumit.solr.AuditAction;
import com.intumit.solr.admin.AdminUser;
import com.intumit.syslog.OperationLogEntity;

public class RobotFormalAnswersVersionService {
	private static RobotFormalAnswersVersionService instance = null;
	private static final Logger logger = Logger.getLogger(RobotFormalAnswersVersionService.class);
	private static final String DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("yyyy/MM/dd");
	private boolean formReset = false;

	public static RobotFormalAnswersVersionService getInstance() {
		if (instance == null) {
			synchronized (RobotFormalAnswersVersionService.class) {
				instance = new RobotFormalAnswersVersionService();
			}
		}
		instance.setFormReset(false);
		return instance;
	}

	// TODO
	public boolean isAuditor(AdminUser user) {
		return true;
	}

	public synchronized Result pass(int tenantId, long id, String updateTime, AdminUser auditor) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		RobotFormalAnswersVersion entity = dao.get(id);
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus())) {
				res.setMessage("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				res.setMessage("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				res.setMessage("此資料已被修改請重新審核");
			} else {
				try {
					switch (entity.getAction()) {
					case ADD:
						RobotFormalAnswers fa = RobotFormalAnswers.add(tenantId, entity.getKeyName(),
								entity.getAnswers());
						entity.setPublicId(fa.getId());
						break;
					case UPDATE:
						RobotFormalAnswers.update(tenantId, entity.getPublicId(), entity.getAnswers());
						break;
					case DELETE:
						RobotFormalAnswers.delete(tenantId, entity.getPublicId());
						break;
					default:
						break;
					}
				} catch (Exception e) {
					logger.error(e, e);
				}
				entity.setAuditorId(auditor.getId());
				entity.setStatus(AuditStatus.HISTORY);
				entity.setPassTime(Calendar.getInstance().getTime());
				dao.saveOrUpdate(entity);
				res.setMessage("已成功核可!");
				res.setStatus(OperationLogEntity.Status.SUCCESS);
			}
		} else {
			res.setMessage("帳號未有審核權限!");
		}
		return res;
	}

	public synchronized Result reject(long id, String updateTime, AdminUser auditor, String message) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		RobotFormalAnswersVersion entity = dao.get(id);
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus())) {
				res.setMessage("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				res.setMessage("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				res.setMessage("此資料已被修改請重新審核");
			} else {
				entity.setStatus(AuditStatus.REJECT);
				entity.setMessage(message);
				entity.setPassTime(Calendar.getInstance().getTime());
				entity.setAuditorId(auditor.getId());
				dao.saveOrUpdate(entity);
				res.setMessage("已成功駁回!");
				res.setStatus(OperationLogEntity.Status.SUCCESS);
			}
		} else {
			res.setMessage("帳號未有審核權限!");
		}
		return res;
	}
	
	public synchronized Result check(long id, String updateTime, AdminUser auditor) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		RobotFormalAnswersVersion entity = dao.get(id);
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus())) {
				res.setMessage("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				res.setMessage("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				res.setMessage("此資料已被修改請重新確認");
			}
		} else {
			res.setMessage("帳號未有審核權限!");
		}
		return res;
	}

	public synchronized Result updateVersion(long id, AdminUser user, String keyName, List<String> answersList) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		RobotFormalAnswersVersion entity = dao.get(id);
		RobotFormalAnswers fa = RobotFormalAnswers.get(entity.getTenantId(), keyName);
		RobotFormalAnswersVersion entityInAudit = RobotFormalAnswersVersionDAO.getInstance()
				.findByKeyAndStatus(entity.getTenantId(), keyName, AuditStatus.AUDIT);
		if (fa != null && (entity.getPublicId() == null || !fa.getId().equals(entity.getPublicId()))) {
			res.setMessage("該制式文案已存在!");
		} else if (entityInAudit != null && !entityInAudit.getId().equals(entity.getId())) {
			res.setMessage("該制式文案已在審核中!");
		} else if (AuditStatus.HISTORY.equals(entity.getStatus())) {
			res.setMessage("此資料已被核可");
		} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
			res.setMessage("此資料已被駁回");
		} else {
			entity.setKeyName(keyName);
			entity.setAnswers(answersList);
			Date date = Calendar.getInstance().getTime();
			entity.setUpdateTime(date);
			entity.setEditorId(user.getId());
			entity.setEditorName(user.getName());
			entity.addUpdateLog();
			entity.setStatus(AuditStatus.AUDIT);
			dao.saveOrUpdate(entity);
			res.setMessage("已成功送審!");
			res.setStatus(OperationLogEntity.Status.SUCCESS);
			formReset = true;
		}
		return res;
	}

	public synchronized Result addNewVesion(RobotFormalAnswersVersion entity) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswers fa = RobotFormalAnswers.get(entity.getTenantId(), entity.getKeyName());
		if (fa != null) {
			res.setMessage("該制式文案已存在!");
		} else if (keyInAudit(entity.getTenantId(), entity.getKeyName())) {
			res.setMessage("該制式文案已在審核中!");
		} else {
			RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
			entity.setAction(AuditAction.ADD);
			Date date = Calendar.getInstance().getTime();
			entity.setCreateTime(date);
			entity.setUpdateTime(date);
			entity.setStatus(AuditStatus.AUDIT);
			entity.setVersion(dao.getPublicVesionNum(entity.getTenantId(), null));
			entity.addUpdateLog();
			dao.saveOrUpdate(entity);
			res.setMessage("已成功送審!");
			res.setStatus(OperationLogEntity.Status.SUCCESS);
		}
		return res;
	}

	public synchronized Result update(RobotFormalAnswersVersion entity) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswers fa = RobotFormalAnswers.get(entity.getTenantId(), entity.getPublicId());
		if (fa != null) {
			RobotFormalAnswers faCheck = RobotFormalAnswers.get(entity.getTenantId(), entity.getKeyName());
			if (faCheck != null && !faCheck.getId().equals(entity.getPublicId())) {
				res.setMessage("該制式文案已存在!");
			} else if (publicIdInAudit(entity.getTenantId(), entity.getPublicId())
					|| keyInAudit(entity.getTenantId(), entity.getKeyName())) {
				res.setMessage("該制式文案已在審核中!");
			} else {
				RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
				entity.setAction(AuditAction.UPDATE);
				Date date = Calendar.getInstance().getTime();
				entity.setCreateTime(date);
				entity.setUpdateTime(date);
				entity.setStatus(AuditStatus.AUDIT);
				entity.setVersion(dao.getPublicVesionNum(entity.getTenantId(), entity.getPublicId()));
				entity.addUpdateLog();
				dao.saveOrUpdate(entity);
				res.setMessage("已成功送審!");
				res.setStatus(OperationLogEntity.Status.SUCCESS);
			}
		} else {
			logger.warn(String.format("RobotFormalAnswers[id:%d] not found", entity.getPublicId()));
			res.setMessage("制式文案不存在，編輯失敗!");
		}
		return res;
	}

	public synchronized Result delete(int tenantId, int editorId, Long id) {
		Result res = new Result();
		res.setStatus(OperationLogEntity.Status.FAILED);
		RobotFormalAnswers fa = RobotFormalAnswers.get(tenantId, id);
		if (fa != null) {
			if (publicIdInAudit(tenantId, id)) {
				res.setMessage("該制式文案已在審核中!");
			} else if (fa.isSystemDefault()) {
				res.setMessage("系統制式文案無法刪除!");
			} else {
				RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
				RobotFormalAnswersVersion entity = new RobotFormalAnswersVersion();
				entity.setKeyName(fa.getKeyName());
				entity.setAnswers(fa.getAnswers());
				entity.setAction(AuditAction.DELETE);
				Date date = Calendar.getInstance().getTime();
				entity.setCreateTime(date);
				entity.setUpdateTime(date);
				entity.setStatus(AuditStatus.AUDIT);
				entity.setEditorId(editorId);
				entity.setPublicId(id);
				entity.setTenantId(tenantId);
				dao.saveOrUpdate(entity);
				res.setMessage("已成功送審!");
				res.setStatus(OperationLogEntity.Status.SUCCESS);
			}
		} else {
			logger.warn(String.format("RobotFormalAnswers[id:%d] not found", id));
			res.setMessage("制式文案不存在，刪除失敗!");
		}
		return res;
	}

	public synchronized boolean keyInAudit(int tenantId, String keyName) {
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		return dao.findByKeyAndStatus(tenantId, keyName, AuditStatus.AUDIT) != null;
	}

	public synchronized boolean publicIdInAudit(int tenantId, Long publicId) {
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		return dao.findByPublicIdAndStatus(tenantId, publicId, AuditStatus.AUDIT) != null;
	}

	public String getLastPassDate(int tenantId, Long publicId) {
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		Date lastPassTime = dao.getLastPassTime(tenantId, publicId);
		if (lastPassTime != null) {
			return SHORT_SDF.format(dao.getLastPassTime(tenantId, publicId));
		}
		return "";
	}

	public class Result {
		private String message;
		private OperationLogEntity.Status status;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public OperationLogEntity.Status getStatus() {
			return status;
		}

		public void setStatus(OperationLogEntity.Status status) {
			this.status = status;
		}
	}

	public synchronized JsonArray listAll(int tenantId) {
		RobotFormalAnswersVersionDAO dao = RobotFormalAnswersVersionDAO.getInstance();
		List<RobotFormalAnswersVersion> list = dao.listAll(tenantId);
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT_PATTERN).create();
		JsonElement element = gson.toJsonTree(list, new TypeToken<List<RobotFormalAnswersVersion>>() {
		}.getType());
		return element.getAsJsonArray();
	}
	
	public boolean isFormReset() {
		return formReset;
	}

	public void setFormReset(boolean formReset) {
		this.formReset = formReset;
	}
}
