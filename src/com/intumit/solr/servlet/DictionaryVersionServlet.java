package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryDatabaseVersion;
import com.intumit.solr.robot.dictionary.DictionaryVersionDAO;
import com.intumit.solr.robot.dictionary.DictionaryVersionService;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

@WebServlet("/wiseadm/DictionaryVersionServlet/*")
public class DictionaryVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(DictionaryVersionServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tenant t = Tenant.getFromSession(req.getSession());
		DictionaryVersionService service = DictionaryVersionService.getInstance();
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
		}  else if (uri.contains("check")) {
			doCheck(req, resp);
		} else {
			XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
			Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
			OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
			if (log == null) {
				resp.setStatus(500);
				return;
			}
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
			Tenant t = Tenant.getFromSession(req.getSession());
			String action = req.getParameter("action");
			JSONObject result = new JSONObject();
			DictionaryVersionService service = DictionaryVersionService.getInstance();
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
						DictionaryDatabase word = DictionaryDatabase.get(t.getId(), keyword);
						DictionaryDatabaseVersion entity = DictionaryVersionDAO.getInstance().get(id);
						DictionaryDatabaseVersion entityInAudit = DictionaryVersionDAO.getInstance().findByKeywordAndStatus(t.getId(), keyword, AuditStatus.AUDIT);
						if (word != null && !word.getId().equals(entity.getPublicId())) {
							result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
						} else if (entityInAudit != null && !entityInAudit.getId().equals(entity.getId())) {
							result.put(MessageUtil.KEY_MSG, "該關鍵字已在審核中!");
						} else {
							String purposes = String.join(",", req.getParameterValues("purpose"));
							boolean enabled = "1".equals(req.getParameter("enabled")) ? Boolean.TRUE : Boolean.FALSE;
							String category = req.getParameter("category");
							boolean enableQaScopeRestriction = "1".equals(req.getParameter("enableQaScopeRestriction"))
									? Boolean.TRUE
									: Boolean.FALSE;
							result.put(MessageUtil.KEY_MSG, service.updateVersion(id, user, keyword, purposes,
									category, enabled, enableQaScopeRestriction));
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
		Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		if (log == null) {
			resp.setStatus(500);
			return;
		}
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		DictionaryVersionService service = DictionaryVersionService.getInstance();
		Tenant t = Tenant.getFromSession(req.getSession());
		JSONObject result = new JSONObject();
		Set<Integer> tIdSet = user.getTenantIdSet();
		String action = req.getParameter("action");
		try {
			if (tIdSet.contains(t.getId())) {
				String keyword = req.getParameter("keyword").toLowerCase().replace("　", " ").trim();
				String id = req.getParameter("id");
				if ((StringUtils.isBlank(id) || !service.publicIdInAudit(t.getId(), Long.parseLong(id))) && !service.keywordInAudit(t.getId(), keyword)) {
					String purposes = req.getParameterValues("purpose") != null
							? String.join(",", req.getParameterValues("purpose"))
							: "";
					boolean enabled = "1".equals(req.getParameter("enabled")) ? Boolean.TRUE : Boolean.FALSE;
					String category = req.getParameter("category");
					boolean enableQaScopeRestriction = "1".equals(req.getParameter("enableQaScopeRestriction"))
							? Boolean.TRUE
							: Boolean.FALSE;
					DictionaryDatabaseVersion entity = new DictionaryDatabaseVersion(keyword, purposes, category,
							enabled, enableQaScopeRestriction);
					entity.setEditorId(user.getId());
					entity.setEditorName(user.getName());
					entity.setTenantId(t.getId());

					switch (action) {
					case "save":
						{
							DictionaryDatabase word = DictionaryDatabase.get(t.getId(), keyword);
							if (word != null) {
								result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
							} else {
								result.append(MessageUtil.KEY_MSG, service.addNewVesion(entity));
							}
						}
						break;
					case "update":
						{
							entity.setPublicId(Long.parseLong(id));
							DictionaryDatabase word = DictionaryDatabase.get(t.getId(), keyword);
							if (word != null && !word.getId().equals(entity.getPublicId())) {
								result.put(MessageUtil.KEY_MSG, "該關鍵字已存在!");
							} else {
								result.append(MessageUtil.KEY_MSG, service.update(entity));
							}
						}
						break;
					case "delete":
						result.append(MessageUtil.KEY_MSG, service.delete(t.getId(), user.getId(), Long.parseLong(id)));
						break;
					default:
						break;
					}
					log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
					result.put(MessageUtil.KEY_MSG, "該關鍵字已在審核中!");
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
		DictionaryVersionService service = DictionaryVersionService.getInstance();
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
		DictionaryVersionService service = DictionaryVersionService.getInstance();
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
