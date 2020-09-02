package com.intumit.solr.robot.dictionary;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DictionaryVersionService {
	private static DictionaryVersionService instance = null;
	private static final Logger logger = Logger.getLogger(DictionaryVersionService.class);
	private static final String DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("yyyy/MM/dd");
	private boolean formReset = false;

	public static DictionaryVersionService getInstance() {
		if (instance == null) {
			synchronized (DictionaryVersionService.class) {
				instance = new DictionaryVersionService();
			}
		}
		instance.setFormReset(false);
		return instance;
	}

	// 等權限弄好~再寫邏輯
	public boolean isAuditor(AdminUser user) {
		return true;
	}

	public synchronized String pass(int tenantId, long id, String updateTime, AdminUser auditor) {
		StringBuilder msg = new StringBuilder();
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = dao.get(id);
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
						DictionaryDatabase word = DictionaryDatabase.save(tenantId, entity.getKeyword(), entity.getPurposes(), entity.isEnabled(),
								entity.getCategory(), entity.getEnableQaScopeRestriction());
						entity.setPublicId(word.getId());
						break;
					case UPDATE:
						DictionaryDatabase.update(tenantId, entity.getPublicId(), entity.getKeyword(), entity.getPurposes(),
								entity.isEnabled(), entity.getCategory(), entity.getEnableQaScopeRestriction());
						break;
					case DELETE:
						DictionaryDatabase.delete(tenantId, entity.getPublicId());
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
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = dao.get(id);
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
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = dao.get(id);
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

	public synchronized String addNewVesion(DictionaryDatabaseVersion entity) {
		StringBuilder msg = new StringBuilder();
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
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

	public synchronized String update(DictionaryDatabaseVersion entity) {
		StringBuilder msg = new StringBuilder();
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
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
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabase dic = DictionaryDatabase.get(id);
		if (dic != null) {
			DictionaryDatabaseVersion entity = new DictionaryDatabaseVersion();
			entity.setKeyword(dic.getKeyword());
			entity.setCategory(dic.getCategory());
			entity.setPurposes(dic.getPurposes());
			entity.setEnabled(dic.isEnabled());
			entity.setEnableQaScopeRestriction(dic.getEnableQaScopeRestriction());
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
			logger.warn(String.format("SynonymKeyword[id:%d] not found", id));
			msg.append("同義詞不存在，刪除失敗!");
		}
		return msg.toString();
	}

	public synchronized String cancelAudit(long id, int userId) {
		StringBuilder msg = new StringBuilder();
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = dao.get(id);
		if (userId == entity.getEditorId()) {
			dao.delete(entity);
			msg.append("已取消申請!");
		} else {
			msg.append("權限不足!只有送審者才能取消送審!");
			logger.warn(String.format("Permisson denied,\t userId[%d] -> editorId:[%d]", userId, entity.getEditorId()));
		}
		return msg.toString();
	}

	public synchronized boolean keywordInAudit(int tenantId, String keyword) {
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		return dao.findByKeywordAndStatus(tenantId, keyword, AuditStatus.AUDIT) != null;
	}

	public synchronized boolean publicIdInAudit(int tenantId, Long publicId) {
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		return dao.findByPublicIdAndStatus(tenantId, publicId, AuditStatus.AUDIT) != null;
	}
	
	public Map<Long, DictionaryDatabaseVersionStatusDto> listStatusOfIds(int tenantId, Collection<Long> publicIds) {
	    Map<Long, DictionaryDatabaseVersionStatusDto>  result = new HashMap<>();
	    DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
	    List<DictionaryDatabaseVersion> dicVers = dao.listByPublicIdsAndStatus(tenantId, publicIds, AuditStatus.AUDIT);
	    dicVers.forEach(dicVer -> {
	        if(!result.containsKey(dicVer.getPublicId())) {
	            DictionaryDatabaseVersionStatusDto dto = new DictionaryDatabaseVersionStatusDto();
	            dto.setAudit(true);
	            result.put(dicVer.getPublicId(), dto);
	        }
	    });
	    dicVers = dao.listAllPassedByPublicIds(tenantId, publicIds);
	    dicVers.forEach(dicVer -> {
	        if(dicVer.getPassTime() != null) {
    	        if(!result.containsKey(dicVer.getPublicId())) {
    	            DictionaryDatabaseVersionStatusDto dto = new DictionaryDatabaseVersionStatusDto();
    	            dto.setLastPassTime(SHORT_SDF.format(dicVer.getPassTime()));
    	            result.put(dicVer.getPublicId(), dto);
    	        }
    	        else if(result.get(dicVer.getPublicId()).getLastPassTime() == null) {
    	            result.get(dicVer.getPublicId()).setLastPassTime(SHORT_SDF.format(dicVer.getPassTime()));
    	        }
	        }
	    });
	    return result;
	}

	public String getLastPassDate(int tenantId, Long publicId) {
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		Date lastPassTime = dao.getLastPassTime(tenantId, publicId);
		if (lastPassTime!= null) {
			return SHORT_SDF.format(dao.getLastPassTime(tenantId, publicId));
		}
		return "";
	}

	public synchronized String updateVersion(long id, AdminUser user, String keyword, String purposes, String category,
			boolean enabled, boolean enableQaScopeRestriction) {
		StringBuilder msg = new StringBuilder();
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = dao.get(id);
		if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
			msg.append("此資料已被核可");
		} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
			msg.append("此資料已被駁回");
		} else {
			entity.setKeyword(keyword);
			entity.setPurposes(purposes);
			entity.setCategory(category);
			entity.setEnabled(enabled);
			entity.setEnableQaScopeRestriction(enableQaScopeRestriction);
			Date date = Calendar.getInstance().getTime();
			entity.setUpdateTime(date);
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
	
	public void saveByUpload(int tenantId, AdminUser user, AuditAction action, DictionaryDatabase word) {
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		DictionaryDatabaseVersion entity = new DictionaryDatabaseVersion(word.getKeyword(), word.getPurposes(), word.getCategory(),
				word.isEnabled(), word.getEnableQaScopeRestriction());
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
		DictionaryVersionDAO dao = DictionaryVersionDAO.getInstance();
		List<DictionaryDatabaseVersion> list = dao.list(tenantId, exceptPublish);
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT_PATTERN).create();
		JsonElement element = gson.toJsonTree(list, new TypeToken<List<DictionaryDatabaseVersion>>() {
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
