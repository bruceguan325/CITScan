package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.google.gson.JsonArray;
import com.intumit.message.MessageUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.synonymKeywords.SynonymKeywordVersion;
import com.intumit.solr.synonymKeywords.SynonymKeywordVersionDAO;
import com.intumit.solr.synonymKeywords.SynonymVersionService;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

@WebServlet("/wiseadm/SynonymVersionServlet/*")
public class SynonymVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SynonymVersionServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tenant t = Tenant.getFromSession(req.getSession());
		SynonymVersionService service = SynonymVersionService.getInstance();
		JsonArray result = service.list(t.getId(), true);
		printJsonResult(resp, result.toString());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uri = req.getRequestURI();
		if (uri.contains("add")) {
			doPut(req, resp);
		} else if (uri.contains("delete")) {
			doDelete(req, resp);
		} else if (uri.contains("check")) {
			doCheck(req, resp);
		} else {
			XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
			Integer opLogId = (Integer) xssReq.getFakeAttribute(OperationLogEntity.KEY_OPERATION_LOG_ID);
			OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
			if (log == null) {
				resp.setStatus(500);
				return;
			}
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
			Tenant t = Tenant.getFromSession(req.getSession());
			String action = req.getParameter("action");
			JSONObject result = new JSONObject();
			SynonymVersionService service = SynonymVersionService.getInstance();
			Long id = null;
			try {
				id = Long.parseLong(req.getParameter("id"));
			} catch (NumberFormatException e) {
				logger.error(e, e);
			}
			String updateTime = req.getParameter("updateTime");
			try {
				if (t != null) {
					switch (action) {
					case "pass":
						result.put(MessageUtil.KEY_MSG, service.pass(t.getId(), id, updateTime, user));
						break;
					case "reject":
						String message = req.getParameter("message");
						result.put(MessageUtil.KEY_MSG, service.reject(id, updateTime, user, message));
						break;
					case "update":
						String keyword = req.getParameter("keyword").toLowerCase().replace("　", " ").trim();
						String synonymKeyword = req.getParameter("synonymKeyword").toLowerCase().replace("　", " ").trim();
						boolean reverse = "1".equals(req.getParameter("reverse")) ? Boolean.TRUE : Boolean.FALSE;
						String nature = StringUtils.trimToNull(req.getParameter("nature"));
						SynonymKeyword word = SynonymKeywordFacade.getInstance().get(t.getId(), keyword);
						SynonymKeywordVersion entity = SynonymKeywordVersionDAO.getInstance().get(id);
						String nowSynkeyword = entity == null ? "" : entity.getSynonymKeyword();
						List<SynonymKeyword> synonymInword = SynonymKeywordFacade.getInstance().findBySynonymKeyword(t.getId(), synonymKeyword, nowSynkeyword);
						SynonymKeywordVersion entityInAudit = SynonymKeywordVersionDAO.getInstance().findByKeywordAndStatus(t.getId(), keyword, AuditStatus.AUDIT);
						List<SynonymKeywordVersion> synonymInAudit = SynonymKeywordVersionDAO.getInstance().findBySynonymKeywordAndStatus(t.getId(), synonymKeyword, AuditStatus.AUDIT, nowSynkeyword);
						boolean noPass = false;
						if(SynonymVersionService.getInstance().synonymKeywordRepeat(synonymKeyword)) {
							result.put(MessageUtil.KEY_MSG, "同義詞重複!");
							noPass = true;
						} else if (word != null && !word.getId().equals(entity.getPublicId())) {
							result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
							noPass = true;
						} else if (entityInAudit != null && !entityInAudit.getId().equals(entity.getId())) {
							result.put(MessageUtil.KEY_MSG, "該關鍵字已在審核中!");
							noPass = true;
						} 
						
						if (synonymInword != null) {
							for(SynonymKeyword s:synonymInword) {
								if(!s.getId().equals(entity.getPublicId())) {
									result.put(MessageUtil.KEY_MSG, "該同義詞已存在!");
									noPass = true;
									break;
								}
							}
						}
						
						if (synonymInAudit != null) {
							for(SynonymKeywordVersion s:synonymInAudit) {
								if(!s.getId().equals(entity.getId())) {
									result.put(MessageUtil.KEY_MSG, "該同義詞已在審核中!");
									noPass = true;
									break;
								}
							}
						} 
						
						if(noPass == false) {
							result.put(MessageUtil.KEY_MSG,
									service.updateVersion(id, user, keyword, synonymKeyword, reverse, nature));
						}
						break;
					default:
						break;
					}
					log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
					result.put(MessageUtil.KEY_MSG, "所屬公司別不同，操作失敗!");
				}
				result.put(MessageUtil.KEY_RESET, service.isFormReset());
			} catch (JSONException e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				logger.error(e, e);
			}
			log.update();
			printJsonResult(resp, result.toString());
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Integer opLogId = (Integer) xssReq.getFakeAttribute(OperationLogEntity.KEY_OPERATION_LOG_ID);
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		if (log == null) {
			resp.setStatus(500);
			return;
		}
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		SynonymVersionService service = SynonymVersionService.getInstance();
		Tenant t = Tenant.getFromSession(req.getSession());
		JSONObject result = new JSONObject();
		Set<Integer> tIdSet = user.getTenantIdSet();
		String action = req.getParameter("action");
		try {
			if (tIdSet.contains(t.getId())) {
				String keyword = req.getParameter("keyword").toLowerCase().replace("　", " ").trim();
				String id = req.getParameter("id");
				Long id_long = null;
				try {
					if (id.equals(""))
						id_long = null;
					else
						id_long = Long.parseLong(req.getParameter("id"));
				} catch (NumberFormatException e) {
					logger.error(e, e);
				}
				if ((StringUtils.isBlank(id) || !service.publicIdInAudit(t.getId(), id_long)) && !service.keywordInAudit(t.getId(), keyword)) {
					if(action.equals("save")||action.equals("update")) {
						String synonymKeyword = req.getParameter("synonymKeyword").toLowerCase().replace("　", " ").trim();
						boolean reverse = "1".equals(req.getParameter("reverse")) ? Boolean.TRUE : Boolean.FALSE;
						String nature = StringUtils.trimToNull(req.getParameter("nature"));
						
						SynonymKeyword nowSynEntity = SynonymKeywordFacade.getInstance().findSynonymKeywordById(t.getId(), id_long);
						String nowSynkeyword = nowSynEntity == null ? "" : nowSynEntity.getSynonymKeyword();
						List<SynonymKeyword> synonymInword = SynonymKeywordFacade.getInstance().findBySynonymKeyword(t.getId(), synonymKeyword, nowSynkeyword);
						List<SynonymKeywordVersion> synonymInAudit = SynonymKeywordVersionDAO.getInstance().findBySynonymKeywordAndStatus(t.getId(), synonymKeyword, AuditStatus.AUDIT, nowSynkeyword);
						
						// 同義詞列表狀態為審核中不可以編輯及刪除，因此不用判斷是否為同一筆
						if(SynonymVersionService.getInstance().synonymKeywordRepeat(synonymKeyword)) {
							result.put(MessageUtil.KEY_MSG, "同義詞重複!");
							log.setStatusMessage(OperationLogEntity.Status.FAILED);
						} else if(synonymInword != null) {
							result.put(MessageUtil.KEY_MSG, "該同義詞已存在!");
							log.setStatusMessage(OperationLogEntity.Status.FAILED);
						} else if(synonymInAudit != null) {
							result.put(MessageUtil.KEY_MSG, "該同義詞已在審核中!");
							log.setStatusMessage(OperationLogEntity.Status.FAILED);
						} else {
							SynonymKeywordVersion entity = new SynonymKeywordVersion(keyword, nature, synonymKeyword, reverse);
							entity.setEditorId(user.getId());
							entity.setEditorName(user.getName());
							entity.setTenantId(t.getId());
							entity.setSynonymKeyword("," + synonymKeyword+ ",");
							
							switch (action) {
							case "save":
								{
									SynonymKeyword word = SynonymKeywordFacade.getInstance().get(t.getId(), keyword);
									if (word != null) {
										result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
									} else {
									result.append(MessageUtil.KEY_MSG, service.addNewVesion(entity));
									}
								}
								break;
							case "update":
								{
									entity.setPublicId(id_long);
									SynonymKeyword word = SynonymKeywordFacade.getInstance().get(t.getId(), keyword);
									if (word != null && !word.getId().equals(entity.getPublicId())) {
										result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
									}  else {
										result.append(MessageUtil.KEY_MSG, service.update(entity));
									}
								}
								break;
							default:
								break;
							}
						}
					} else if(action.equals("delete")) {
						result.append(MessageUtil.KEY_MSG, service.delete(t.getId(), user.getId(), id_long));
					}
					log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					result.put(MessageUtil.KEY_MSG, "該關鍵字已在審核中!");
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
				}
			} else {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				result.put(MessageUtil.KEY_MSG, "權限不足!");
			}
			result.put(MessageUtil.KEY_RESET, service.isFormReset());
		} catch (NumberFormatException | JSONException e) {
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
			logger.error(e, e);
		}
		log.update();
		printJsonResult(resp, result.toString());
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		if (log == null) {
			resp.setStatus(500);
			return;
		}
		SynonymVersionService service = SynonymVersionService.getInstance();
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		String idStr = req.getParameter("id");
		JSONObject result = new JSONObject();
		Long id = null;
		try {
			id = Long.parseLong(idStr);
			result.put(MessageUtil.KEY_MSG, service.cancelAudit(id, user.getId()));
			log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
		} catch (NumberFormatException | JSONException e) {
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
			logger.error(e, e);
		}
		log.update();
		printJsonResult(resp, result.toString());
	}
	
	protected void doCheck(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		SynonymVersionService service = SynonymVersionService.getInstance();
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		JSONObject result = new JSONObject();
		try {
			String updateTime = req.getParameter("updateTime");
			Long id = Long.parseLong(req.getParameter("id"));
			result.put(MessageUtil.KEY_MSG, service.check(id, updateTime, user));
		} catch (NumberFormatException | JSONException e) {
			logger.error(e, e);
		}
		printJsonResult(resp, result.toString());
	}

	protected void printJsonResult(HttpServletResponse resp, String message) {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try (PrintWriter writer = resp.getWriter()) {
			writer.write(message);
		} catch (Exception e) {
			logger.error(e, e);
		}
	}
}
