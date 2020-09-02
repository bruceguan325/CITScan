package com.intumit.solr.robot.entity;

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
import com.intumit.solr.robot.AuditStatus;

public class EntityVersionService {
	private static EntityVersionService instance = null;
	private static final Logger logger = Logger.getLogger(EntityVersionService.class);
	private static final String DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("yyyy/MM/dd");
	private boolean formReset = false;
	
	public static EntityVersionService getInstance() {
		if (instance == null) {
			synchronized (EntityVersionService.class) {
				instance = new EntityVersionService();
			}
		}
		instance.setFormReset(false);
		return instance;
	}

	// 等權限弄好~再寫邏輯
	public boolean isAuditor(AdminUser user) {
		return true;
	}

	public synchronized String pass(Integer tenantId, long id, String updateTime, AdminUser auditor) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = dao.get(id);
		// 權限判斷 使用者有審核權限
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
				msg.append("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				msg.append("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				msg.append("此資料已被修改請重新審核");
			} else {
				try {
					switch (entity.getAction()) {
					case ADD:
						QAEntity word = QAEntity.save(tenantId, entity.getCategory(), entity.getCode(), entity.getName(),entity.getSubEntities(),
								entity.getEntityType(), entity.getEntityValues(), entity.getFromIndex(), entity.isEnabled());
						entity.setPublicId(word.getId());
						break;
					case UPDATE:
						QAEntity.update(tenantId, entity.getPublicId()+"", entity.getCategory(), entity.getCode(), entity.getName(),entity.getSubEntities(),
								entity.getEntityType(), entity.getEntityValues(), entity.getFromIndex(), entity.isEnabled());
						break;
					case DELETE:
						QAEntity.delete(tenantId, entity.getPublicId()+"");
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
				msg.append("已成功核可!");
			}
		} else {
			msg.append("帳號未有審核權限!");
		}
		return msg.toString();
	}

	public synchronized String reject(long id, String updateTime, AdminUser auditor, String message) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = dao.get(id);
		// 權限判斷 使用者有審核權限
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
				msg.append("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				msg.append("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				msg.append("此資料已被修改請重新審核");
			} else {
				entity.setStatus(AuditStatus.REJECT);
				entity.setMessage(message);
				entity.setPassTime(Calendar.getInstance().getTime());
				entity.setAuditorId(auditor.getId());
				dao.saveOrUpdate(entity);
				msg.append("已成功駁回!");
			}
		} else {
			msg.append("帳號未有審核權限!");
		}
		return msg.toString();
	}
	
	public synchronized String check(long id, String updateTime, AdminUser auditor) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = dao.get(id);
		// 權限判斷 使用者有審核權限
		if (isAuditor(auditor) && entity.checkNotEditor(auditor.getId())) {
			if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
				msg.append("此資料已被核可");
			} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
				msg.append("此資料已被駁回");
			} else if (!updateTime.equals(SDF.format(entity.getUpdateTime()))) {
				msg.append("此資料已被修改請重新確認");
			} 
		} else {
			msg.append("帳號未有審核權限!");
		}
		return msg.toString();
	}

	public synchronized String addNewVesion(EntityDatabaseVersion entity) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		entity.setAction(AuditAction.ADD);
		Date date = Calendar.getInstance().getTime();
		entity.setCreateTime(date);
		entity.setUpdateTime(date);
		entity.setStatus(AuditStatus.AUDIT);
		entity.setVersion(dao.getPublicVesionNum(entity.getTenantId(), null));
		entity.addUpdateLog();
		dao.saveOrUpdate(entity);
		msg.append("已成功送審!");
		formReset = true;
		return msg.toString();
	}

	public synchronized String update(EntityDatabaseVersion entity) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		entity.setAction(AuditAction.UPDATE);
		Date date = Calendar.getInstance().getTime();
		entity.setCreateTime(date);
		entity.setUpdateTime(date);
		entity.setStatus(AuditStatus.AUDIT);
		entity.setVersion(dao.getPublicVesionNum(entity.getTenantId(), entity.getPublicId()));
		entity.addUpdateLog();
		dao.saveOrUpdate(entity);
		msg.append("已成功送審!");
		formReset = true;
		return msg.toString();
	}

	public synchronized String delete(int tenantId, int editorId, Long id) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		QAEntity ent = QAEntity.get(id);
		if (ent != null) {
			EntityDatabaseVersion entity = new EntityDatabaseVersion();
			entity.setCategory(ent.getCategory());
			entity.setCode(ent.getCode());
			entity.setName(ent.getName());
			entity.setRefKP(ent.getRefKP());
			entity.setEntityType(ent.getEntityType());
			entity.setEntityValues(ent.getEntityValues());
			entity.setFromIndex(ent.getFromIndex());
			entity.setEnabled(ent.isEnabled());
			entity.setAction(AuditAction.DELETE);
			Date date = Calendar.getInstance().getTime();
			entity.setCreateTime(date);
			entity.setUpdateTime(date);
			entity.setStatus(AuditStatus.AUDIT);
			entity.setEditorId(editorId);
			entity.setPublicId(id);
			entity.setTenantId(tenantId);
			dao.saveOrUpdate(entity);
			msg.append("已成功送審!");
		} else {
			logger.warn(String.format("Entity[id:%d] not found", id));
			msg.append("實體不存在，刪除失敗!");
		}
		return msg.toString();
	}

	public synchronized String cancelAudit(long id, int userId) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = dao.get(id);
		if (userId == entity.getEditorId()) {
			dao.delete(entity);
			msg.append("已取消申請!");
		} else {
			msg.append("權限不足!只有送審者才能取消送審!");
			logger.warn(String.format("Permisson denied,\t userId[%d] -> editorId:[%d]", userId, entity.getEditorId()));
		}
		return msg.toString();
	}

	public synchronized boolean CodeInAudit(int tenantId, String code) {
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		return dao.findByCodeAndStatus(tenantId, code, AuditStatus.AUDIT) != null;
	}
	
	public synchronized boolean publicIdInAudit(int tenantId, Long publicId) {
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		return dao.findByPublicIdAndStatus(tenantId, publicId, AuditStatus.AUDIT) != null;
	}
	
	public String getLastPassDate(int tenantId, Long publicId) {
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		Date lastPassTime = dao.getLastPassTime(tenantId, publicId);
		if (lastPassTime!= null) {
			return SHORT_SDF.format(dao.getLastPassTime(tenantId, publicId));
		}
		return "";
	}

	public synchronized String updateVersion(long id, AdminUser user, String category, String code, String name, String subEntities,
			String entityValues, QAEntityType entityType, boolean fromIndex,boolean enabled, String refKP) {
		StringBuilder msg = new StringBuilder();
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = dao.get(id);
		if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
			msg.append("此資料已被核可");
		} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
			msg.append("此資料已被駁回");
		} else {
			entity.setCategory(category);
			entity.setCode(code);
			entity.setName(name);
			entity.setSubEntities(subEntities);
			entity.setEntityValues(entityValues);
			entity.setEntityType(entityType);
			entity.setFromIndex(fromIndex);
			entity.setEnabled(enabled);
			entity.setRefKP(refKP);
			entity.setUpdateTime(Calendar.getInstance().getTime());
			entity.setEditorId(user.getId());
			entity.setEditorName(user.getName());
			entity.addUpdateLog();
			entity.setStatus(AuditStatus.AUDIT);
			dao.saveOrUpdate(entity);
			msg.append("已成功送審!");
			formReset = true;
		}
		return msg.toString();
	}

	public void saveByUpload(int tenantId, AdminUser user, AuditAction action, QAEntity word) {
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		EntityDatabaseVersion entity = new EntityDatabaseVersion(word.getCategory(), word.getCode(), word.getName(),
				word.getSubEntities(), word.getEntityValues(), word.getEntityType(), word.isEnabled(),
				word.getFromIndex(), word.getRefKP());
		entity.setEditorId(user.getId());
		entity.setTenantId(tenantId);
		entity.setAction(action);
		Date date = Calendar.getInstance().getTime();
		entity.setCreateTime(date);
		entity.setUpdateTime(date);
		entity.setStatus(AuditStatus.PUBLISH);
		if (AuditAction.ADD.equals(action)) {
			entity.setVersion(dao.getPublicVesionNum(tenantId, null));
		} else if (AuditAction.UPDATE.equals(action)) {
			entity.setVersion(dao.getPublicVesionNum(tenantId, word.getId()));
		}
		entity.setPublicId(word.getId());
		entity.setAuditorId(user.getId());
		entity.setPassTime(date);
		dao.saveOrUpdate(entity);
	}

	public synchronized JsonArray listAll(int tenantId) {
		return list(tenantId, false);
	}

	public synchronized JsonArray list(int tenantId, boolean exceptPublish) {
		EntityVersionDAO dao = EntityVersionDAO.getInstance();
		List<EntityDatabaseVersion> list = dao.list(tenantId, exceptPublish);
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT_PATTERN).create();
		JsonElement element = gson.toJsonTree(list, new TypeToken<List<EntityDatabaseVersion>>() {
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
