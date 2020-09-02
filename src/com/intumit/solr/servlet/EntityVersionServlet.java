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
import com.intumit.solr.robot.entity.EntityDatabaseVersion;
import com.intumit.solr.robot.entity.EntityVersionDAO;
import com.intumit.solr.robot.entity.EntityVersionService;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.entity.QAEntityType;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

@WebServlet("/wiseadm/EntityVersionServlet/*")
public class EntityVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(EntityVersionServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tenant t = Tenant.getFromSession(req.getSession());
		EntityVersionService service = EntityVersionService.getInstance();
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
			EntityVersionService service = EntityVersionService.getInstance();
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
						String category = req.getParameter("category").replace("　", " ").trim();
						String code = req.getParameter("code").replace("　", " ").trim();
						String name = req.getParameter("name");
						String subEntities = req.getParameter("subEntities");
						String entityValues = req.getParameter("entityValues");
						QAEntityType entityType = QAEntityType.valueOf(req.getParameter("entityType"));
						QAEntity word = QAEntity.get(t.getId(), code);
						EntityDatabaseVersion entity = EntityVersionDAO.getInstance().get(id);
						EntityDatabaseVersion entityInAudit = EntityVersionDAO.getInstance().findByCodeAndStatus(t.getId(), code, AuditStatus.AUDIT);
						if (word != null && !word.getId().equals(entity.getPublicId())) {
							result.put(MessageUtil.KEY_MSG, "該實體代號已存在!");
						} else if (entityInAudit != null && !entityInAudit.getId().equals(entity.getId())) {
							result.put(MessageUtil.KEY_MSG, "該實體已在審核中!");
						} else {
							boolean fromIndex = "1".equals(req.getParameter("fromIndex")) ? Boolean.TRUE : Boolean.FALSE;
							boolean enabled = "1".equals(req.getParameter("enabled")) ? Boolean.TRUE : Boolean.FALSE;
							String refKP = req.getParameter("refKP");
							result.put(MessageUtil.KEY_MSG, service.updateVersion(id, user, category, code, name,
									subEntities, entityValues, entityType, fromIndex, enabled, refKP));
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
		EntityVersionService service = EntityVersionService.getInstance();
		Tenant t = Tenant.getFromSession(req.getSession());
		JSONObject result = new JSONObject();
		Set<Integer> tIdSet = user.getTenantIdSet();
		String action = req.getParameter("action");
		try {
			if (tIdSet.contains(t.getId())) {
				String category = req.getParameter("category").replace("　", " ").trim();
				String name = req.getParameter("name");
				String code = req.getParameter("code").replace("　", " ").trim();
				String id = req.getParameter("id");
				if ((StringUtils.isBlank(id) || !service.publicIdInAudit(t.getId(), Long.parseLong(id))) && !service.CodeInAudit(t.getId(), code)) {
					EntityDatabaseVersion entity = null;
					if (!action.equals("delete")) {
						String subEntities = req.getParameter("subEntities");
						String entityValues = req.getParameter("entityValues");
						QAEntityType entityType = QAEntityType.valueOf(req.getParameter("entityType"));
						boolean fromIndex = "1".equals(req.getParameter("fromIndex")) ? Boolean.TRUE : Boolean.FALSE;
						boolean enabled = "1".equals(req.getParameter("enabled")) ? Boolean.TRUE : Boolean.FALSE;
						String refKP = req.getParameter("refKP");
						entity = new EntityDatabaseVersion(category, code, name, subEntities, entityValues,
								entityType, enabled, fromIndex, refKP);
						entity.setEditorId(user.getId());
						entity.setEditorName(user.getName());
						entity.setTenantId(t.getId());
					}

					switch (action) {
					case "save":
						{
							QAEntity word = QAEntity.get(t.getId(), code);
							if (word != null) {
								result.put(MessageUtil.KEY_MSG, "該實體代號已存在!");
							} else {
								result.append(MessageUtil.KEY_MSG, service.addNewVesion(entity));
							}
						}
						break;
					case "update":
						{
							entity.setPublicId(Long.parseLong(id));
							QAEntity word = QAEntity.get(t.getId(), code);
							if (word != null && !word.getId().equals(entity.getPublicId())) {
								result.put(MessageUtil.KEY_MSG, "該實體代號已存在!");
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
					result.put(MessageUtil.KEY_MSG, "該實體已在審核中!");
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
		EntityVersionService service = EntityVersionService.getInstance();
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
		EntityVersionService service = EntityVersionService.getInstance();
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
