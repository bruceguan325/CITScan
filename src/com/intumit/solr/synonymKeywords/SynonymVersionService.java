package com.intumit.solr.synonymKeywords;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class SynonymVersionService {

	private static SynonymVersionService instance = null;
	private static final Logger logger = Logger.getLogger(SynonymVersionService.class);
	private static final String DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("yyyy/MM/dd");
	private boolean formReset = false;

	public static SynonymVersionService getInstance() {
		if (instance == null) {
			synchronized (SynonymVersionService.class) {
				instance = new SynonymVersionService();
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
		SynonymKeywordFacade skf = SynonymKeywordFacade.getInstance();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = dao.get(id);
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
						SynonymKeyword word = skf.save(tenantId, entity.getKeyword(), entity.getSynonymKeyword(), entity.isReverse(),
								entity.getNature());
						entity.setPublicId(word.getId());
						break;
					case UPDATE:
						skf.update(entity.getPublicId(), entity.getKeyword(), entity.getSynonymKeyword(),
								entity.isReverse(), entity.getNature());
						break;
					case DELETE:
						skf.delete(tenantId, entity.getPublicId());
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
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = dao.get(id);
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
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = dao.get(id);
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

	public synchronized String addNewVesion(SynonymKeywordVersion entity) {
		StringBuilder msg = new StringBuilder();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
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

	public synchronized boolean keywordInAudit(int tenantId, String keyword) {
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		return dao.findByKeywordAndStatus(tenantId, keyword, AuditStatus.AUDIT) != null;
	}
	
	public synchronized boolean publicIdInAudit(int tenantId, Long publicId) {
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		return dao.findByPublicIdAndStatus(tenantId, publicId, AuditStatus.AUDIT) != null;
	}
	
	public boolean synonymKeywordRepeat(String synonymKeyword) {
		boolean isRepeat = false;
		List<String> wordList = new ArrayList<String>();
		for(String s : synonymKeyword.split(",")) {
			if(wordList.indexOf(s) != -1) {
				isRepeat = true;
				break;
			} else {
				wordList.add(s);
			}
		}
		return isRepeat;
	}
	
	public String getLastPassDate(int tenantId, Long publicId) {
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		Date lastPassTime = dao.getLastPassTime(tenantId, publicId);
		if (lastPassTime!= null) {
			return SHORT_SDF.format(dao.getLastPassTime(tenantId, publicId));
		}
		return "";
	}

	public synchronized String cancelAudit(long id, int userId) {
		StringBuilder msg = new StringBuilder();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = dao.get(id);
		if (userId == entity.getEditorId()) {
			dao.delete(entity);
			msg.append("已取消申請!");
		} else {
			msg.append("權限不足!只有送審者才能取消送審!");
			logger.warn(String.format("Permisson denied,\t userId[%d] -> editorId:[%d]", userId, entity.getEditorId()));
		}
		return msg.toString();
	}

	public synchronized String updateVersion(long id, AdminUser user, String keyword, String synonymKeyword,
			boolean reverse, String nature) {
		StringBuilder msg = new StringBuilder();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = dao.get(id);
		if (AuditStatus.HISTORY.equals(entity.getStatus()) || AuditStatus.PUBLISH.equals(entity.getStatus())) {
			msg.append("此資料已被核可");
		} else if (AuditStatus.REJECT.equals(entity.getStatus())) {
			msg.append("此資料已被駁回");
		} else {
			entity.setKeyword(keyword);
			entity.setSynonymKeyword("," + synonymKeyword + ",");
			entity.setNature(nature);
			entity.setReverse(reverse);
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

	public synchronized String update(SynonymKeywordVersion entity) {
		StringBuilder msg = new StringBuilder();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
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
		SynonymKeywordFacade skf = SynonymKeywordFacade.getInstance();
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeyword synonymKeyword = skf.get(id);
		if (synonymKeyword != null) {
			SynonymKeywordVersion entity = new SynonymKeywordVersion();
			entity.setKeyword(synonymKeyword.getKeyword());
			entity.setSynonymKeyword(synonymKeyword.getSynonymKeyword());
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

	public void saveByUpload(int tenantId, AdminUser user, AuditAction action, SynonymKeyword word) {
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		SynonymKeywordVersion entity = new SynonymKeywordVersion(word.getKeyword(), word.getNature(),
				word.getSynonymKeyword(), word.isReverse());
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
		SynonymKeywordVersionDAO dao = SynonymKeywordVersionDAO.getInstance();
		List<SynonymKeywordVersion> list = dao.list(tenantId, exceptPublish);
		Gson gson = new GsonBuilder().setDateFormat("yyyy/MM/dd HH:mm:ss").create();
		JsonElement element = gson.toJsonTree(list, new TypeToken<List<SynonymKeywordVersion>>() {
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
