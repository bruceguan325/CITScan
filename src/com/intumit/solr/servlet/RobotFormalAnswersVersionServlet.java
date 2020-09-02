package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intumit.message.MessageUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.RobotFormalAnswers;
import com.intumit.solr.robot.RobotFormalAnswersVersionService;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

@WebServlet("/wiseadm/RobotFormalAnswersVersionServlet/*")
public class RobotFormalAnswersVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(RobotFormalAnswersVersionServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tenant t = Tenant.getFromSession(req.getSession());
		RobotFormalAnswersVersionService service = RobotFormalAnswersVersionService.getInstance();
		JsonArray result = service.listAll(t.getId());
		for (JsonElement element : result) {
			JsonObject object = element.getAsJsonObject();
			if ("AUDIT".equals(object.get("status").getAsString())
					&& "UPDATE".equals(object.get("action").getAsString())) {
				object.addProperty("oldAnswers", RobotFormalAnswers
						.get(object.get("tenantId").getAsInt(), object.get("publicId").getAsLong()).getAnswers());
			}
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
			RobotFormalAnswersVersionService service = RobotFormalAnswersVersionService.getInstance();
			Long id = null;
			try {
				id = Long.parseLong(req.getParameter("id"));
			} catch (NumberFormatException e) {
				logger.error(e, e);
			}
			String updateTime = req.getParameter("updateTime");
			RobotFormalAnswersVersionService.Result res = null;
			try {
				if (t != null) {
					switch (action) {
					case "pass":
						res = service.pass(t.getId(), id, updateTime, user);
						result.put(MessageUtil.KEY_MSG, res.getMessage());
						log.setStatusMessage(res.getStatus());
						break;
					case "reject":
						String message = req.getParameter("message");
						res = service.reject(id, updateTime, user, message);
						result.put(MessageUtil.KEY_MSG, res.getMessage());
						log.setStatusMessage(res.getStatus());
						break;
					case "update":
						String keyName = req.getParameter("keyName");
						String value = req.getParameter("answers");
						List<String> values = Arrays.asList(value.split("\\r?\\n"));
						res = service.updateVersion(id, user, keyName, values);
						result.put(MessageUtil.KEY_MSG, res.getMessage());
						log.setStatusMessage(res.getStatus());
						break;
					default:
						break;
					}
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
	
	protected void doCheck(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RobotFormalAnswersVersionService service = RobotFormalAnswersVersionService.getInstance();
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		JSONObject result = new JSONObject();
		RobotFormalAnswersVersionService.Result res = null;
		try {
			String updateTime = req.getParameter("updateTime");
			Long id = Long.parseLong(req.getParameter("id"));
			res = service.check(id, updateTime, user);
			result.put(MessageUtil.KEY_MSG, res.getMessage());
		} catch (NumberFormatException | JSONException e) {
			logger.error(e, e);
		}
		printJsonResult(resp, result.toString());
	}
}
