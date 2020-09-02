package com.intumit.solr.robot.connector.citi;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wink.json4j.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfigFacade;

import flexjson.JSONSerializer;
import net.coobird.thumbnailator.Thumbnails;
import com.intumit.citi.CitiDeep;
/**
 * Restful CRUD
 * 
 * @author herb
 *
 */
@WebServlet(urlPatterns = { "/wiseadm/citi/richMessages", "/wiseadm/citi/richMessages/*" })
public class RichMessageServlet extends HttpServlet {
	private static final CharSequence COLLECTION_URL_PATTERN = "/wiseadm/citi/richMessages";

	static JSONArray pruneInvalidActions(JSONArray actions) throws JSONException {
		JSONArray na = new JSONArray();

		for (int i = 0; i < actions.length(); i++) {
			JSONObject a = actions.getJSONObject(i);

			if (a != null && StringUtils.trimToNull(a.optString("label")) != null) {
				for (String name: JSONObject.getNames(a)) {
					// 除掉完全空白的 property
					if (a.has(name) && a.get(name) instanceof String && StringUtils.isEmpty(a.getString(name))) {
						a.remove(name);
					}
				}
				na.put(a);
			}
		}

		return na;
	}
	
	static JSONArray prunePostbackActions(JSONArray actions) throws JSONException {
		JSONArray na = new JSONArray();
		
		for (int i = 0; i < actions.length(); i++) {
			JSONObject a = actions.getJSONObject(i);
			if (a != null && StringUtils.trimToNull(a.optString("type")) != null &&
				a.getString("type").equals("postback")) {
				a.put("displayText", a.optString("label"));
			}
			na.put(a);
		}
		
		return na;
	}

	public JSONObject pruneInvalidData(Tenant tenant, String contextPath, String msgType, JSONObject tpl) {
		try {
			if ("carousel".equals(msgType)) {
				JSONObject t = tpl.optJSONObject("template");
				JSONArray cols = t.optJSONArray("columns");
				JSONArray nc = new JSONArray();

				for (int i = 0; i < cols.length(); i++) {
					JSONObject c = cols.getJSONObject(i);

					if (c.has("thumbnailImageUrl")) {
						if (StringUtils.trimToNull(c.getString("thumbnailImageUrl")) == null)
							c.remove("thumbnailImageUrl");
						else {
							c.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath, c.getString("thumbnailImageUrl")));
						}
					}

					if (c.has("actions") && StringUtils.trimToNull(c.optString("text")) != null) { 
						c.put("actions", pruneInvalidActions(c.optJSONArray("actions", new JSONArray())));
						t.put("actions", prunePostbackActions(t.optJSONArray("actions", new JSONArray())));
						nc.put(c);
					}
				}

				t.put("columns", nc);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tpl;
	}

	private static final Pattern DATA_URL_PATTERN = Pattern.compile("(?i)data:image/(png|jpeg|jpg);base64,(.*+)");

	private static InputStream getFileFromDataURI(String base64) {
		byte[] img = Base64.getDecoder().decode(base64);
		return new ByteArrayInputStream(img);
	}

	private static String processImageAndReturnUrl(Tenant t, String contextPath, String dataUrl) {
		Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
		if (m.find()) {
			try {
				String encodedName = getName(t.getId());
				File storeDir = new File(WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise"
						+ File.separator + "img" + File.separator + "citi");
				
				if (!storeDir.exists()) storeDir.mkdirs();
				
				OutputStream os = new FileOutputStream(new File(storeDir, encodedName));

				InputStream imageIs = getFileFromDataURI(m.group(2));
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = imageIs.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				imageIs.close();
				os.flush();
				os.close();
				return contextPath + "/img/citi/" + encodedName;
			} catch (Exception e) {
				e.printStackTrace();
				return dataUrl;
			}
		} else {
			return dataUrl;
		}
	}

	private static synchronized String getName(Integer tid) {
		String name = tid + "_" + System.currentTimeMillis();
		String encodedName = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
		return encodedName;
	}

	/**
	 * C
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	    resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        System.out.println(this.getClass().getName() + "::POST");
        try {
            Tenant t = Tenant.getFromSession(req.getSession());
            // Permission check must be move to gateway someday.
            AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
            ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String payload = WiSeUtils.getPayload(req);
            JSONObject obj = new JSONObject(payload);
            String msgType = obj.optString("msgType");
            JSONObject msgT = obj.optJSONObject("msgTemplate");
            if (msgT != null) {
                obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT).toString());
            } else {
                obj.put("msgTemplate", "{}");
            }
            // System.out.println(obj.toString(2));

            CitiDeep msg = om.readValue(obj.toString(2), CitiDeep.class);
            msg.tranMsg();
            msg.setId(null);
            //msg.setTenantId(t.getId());

            CitiDeep.saveOrUpdate(msg);
            resp.getWriter().println(new JSONObject().put("status", this.getClass().getName() + "::POST").toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
	}

	/**
	 * R
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try {
			String uri = req.getRequestURI();
			Tenant t = Tenant.getFromSession(req.getSession());
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				JSONSerializer js = new JSONSerializer();
				String s = js.serialize(RichMessage.list(t.getId()));
				resp.getWriter().println(s);
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				CitiDeep msg = RichMessage.get(id);

				JSONSerializer js = new JSONSerializer();
				String s = js.serialize(msg);
				resp.getWriter().println(s);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * U
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		System.out.println(this.getClass().getName() + "::PUT");
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			// Permission check must be move to gateway someday.
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());

			String uri = req.getRequestURI();
			ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				String payload = WiSeUtils.getPayload(req);
				JSONObject obj = new JSONObject(payload);
				String msgType = obj.optString("msgType");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
				// System.out.println(obj.toString(2));

				RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);
				/*
				// 安全檢核
				if (!msg.getTenantId().equals(t.getId())) {
					System.out.println("Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
							+ msg.getId() + " (t=" + msg.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]");
					return;
				}
                */
				RichMessage.saveOrUpdate(msg);
				resp.getWriter().println("{}"); // not support update whole
												// collection
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				CitiDeep origMsg = RichMessage.get(id);

				System.out.println("Update rich message :[" + origMsg.getId() + "]");

				String payload = WiSeUtils.getPayload(req);
				JSONObject obj = new JSONObject(payload);
				String msgType = obj.optString("msgType");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
				// System.out.println(obj.toString(2));
				CitiDeep msg = om.readValue(obj.toString(2), CitiDeep.class);
				msg.tranMsg();
				// 安全檢核
				if (!msg.getId().equals(origMsg.getId())) {
					System.out.println("Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
							+ msg.getId() );
					return;
				}

				CitiDeep.saveOrUpdate(msg);
				resp.getWriter().println("{}"); // not support update whole
												// collection
			}
			// resp.getWriter().println(new JSONObject().put("status",
			// this.getClass().getName() + "::PUT").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * D
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		System.out.println(this.getClass().getName() + "::DELETE");
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			String uri = req.getRequestURI();
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				resp.getWriter().println("{}"); // not support delete whole
												// collection
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				CitiDeep msg = RichMessage.get(id);

				JSONSerializer js = new JSONSerializer();
				String s = js.serialize(msg);
				resp.getWriter().println(s);
				RichMessage.delete(msg);
				
			}
			// resp.getWriter().println(new JSONObject().put("status",
			// this.getClass().getName() + "::DELETE").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Traverse actions and append event source / event source type information to the postback.data
	 * @param rmj
	 * @param ctx
	 */
	public static void addEventSource(JSONObject rmj, String eventSource, String eventSourceType) {
		try {
			if (rmj.has("actions")) {
				addEventSource(rmj.getJSONArray("actions"), eventSource, eventSourceType);
			}
			if (rmj.has("template")) {
				JSONObject tpl = rmj.optJSONObject("template");
				addEventSource(tpl, eventSource, eventSourceType);
			}
			if (rmj.has("columns")) {
				JSONArray cols = rmj.optJSONArray("columns");
				
				for (int i=0; i < cols.length(); i++) {
					JSONObject col = cols.getJSONObject(i);
					addEventSource(col, eventSource, eventSourceType);
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Traverse actions and append event source / event source type information to the postback.data
	 * @param rmj
	 * @param ctx
	 */
	public static void addEventSource(JSONArray actions, String eventSource, String eventSourceType) {
		for (int i=0; i < actions.length(); i++) {
			try {
				JSONObject a = actions.getJSONObject(i);
				
				// 這是 for quick reply 的結構是 items[i].["aciton"] （判斷依據是 items[i] 發現 type == "action"，就再 getJSONObject("action") 才做後面比對
				if ("action".equals(a.optString("type"))) {
					a = a.optJSONObject("action");
				}
				
				if (eventSource != null) a.put("es", eventSource);
				if (eventSourceType != null) a.put("est", eventSourceType);
				
				switch (a.optString("type", "")) {
					case "postback":
						if (a.has("data")) {
							String data = a.optString("data", "");
							String encrypted = WiSeUtils.aesEncrypt(Tenant.SIMPLE_AES_SECRET_KEY, Tenant.SIMPLE_AES_INIT_VECTOR, data);
							a.put("data", encrypted);
						}
						
						break;
					default:
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

}
