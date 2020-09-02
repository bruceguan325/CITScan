package com.intumit.solr.robot.connector.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Hibernate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.RobotImageFile;
import com.intumit.solr.robot.RobotImageFilePath;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.util.XssStringFilter;
import com.intumit.systemconfig.RobotImageFileConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

import flexjson.JSONSerializer;

/**
 * Restful CRUD
 * 
 * @author dudamel
 *
 */
@WebServlet(urlPatterns = { "/wiseadm/webRM/richMenu", "/wiseadm/webRM/richMenu/*" })
public class RichMenuServlet extends HttpServlet {
	
	public static Logger log = Logger.getLogger(RichMenuServlet.class.getName());
	
	static XssStringFilter xssStr = new XssStringFilter();

	private static final long serialVersionUID = 1L;
	
	private static final CharSequence COLLECTION_URL_PATTERN = "/wiseadm/webRM/richMenu";

	static JSONArray pruneInvalidActions(JSONArray actions, Tenant t) throws JSONException {
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
				JSONObject bounds = a.optJSONObject("bounds", new JSONObject());
				a.remove("bounds");
				if (!bounds.isEmpty()) {
					na.put(new JSONObject().put("bounds", bounds).put("action", a));
				}
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

	public JSONObject pruneInvalidData(Tenant tenant, String msgType, JSONObject tpl, String msgName, String menuMkey) {
		try {
			if ("richmenu".equals(msgType)) {

				if (!tpl.has("areas")) {
					return new JSONObject();
				}

				if (tpl.has("baseUrl")) {
					if (StringUtils.trimToNull(tpl.getString("baseUrl")) != null) {
						processBaseUrlAndFixRatio(tenant.getId(), tpl, menuMkey, null);
					}
				}

				tpl.put("areas", pruneInvalidActions(tpl.getJSONArray("areas"), tenant));
				tpl.put("name", msgName);
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
	
	@SuppressWarnings("deprecation")
	protected static void processBaseUrlAndFixRatio(Integer tid, JSONObject tpl, String menuMkey, String assignName)  {
		try {
			String dataUrl = tpl.getString("baseUrl");
			Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
			
			if (m.find()) {
				File original = null;
				try {
					String encodedName = assignName == null ? getName(tid) : assignName;
					InputStream imageIs = getFileFromDataURI(m.group(2));
					String fileName = encodedName + "." + (dataUrl.contains("png") ? "png" : "jpeg");
					Path path = Paths
							.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
									.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
					String filePath = path.toFile().getCanonicalPath();

					original = new File(filePath + "O");

					OutputStream os = new FileOutputStream(original);

					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = imageIs.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					imageIs.close();
					os.flush();
					os.close();

					RobotImageFile.listDeleteBy(tid, RichMenu.class.getName(), menuMkey, true);
					File baseUrlDir = new File(filePath);
					FileUtils.copyFile(original, baseUrlDir);

					InputStream imageInFile = new FileInputStream(baseUrlDir);
					if (imageInFile != null) {
						Blob blob = Hibernate.createBlob(imageInFile);
						new RobotImageFile(tid, "img/webRM", RichMenu.class.getName(), fileName, menuMkey, blob);
						imageInFile.close();
					}

					String newPath = "/" + RobotImageFileConfig.getImageFileConfig()[0];
					tpl.put("baseUrl", WiseSystemConfigFacade.getInstance().get().getFullUrlBase() + newPath + "/img/webRM/" + fileName);
				} catch (Exception e) {
					e.printStackTrace();
					tpl.put("baseUrl", dataUrl);
				}
				finally {
					if (original != null) FileUtils.deleteQuietly(original);
				}
			} else {
				tpl.put("baseUrl", dataUrl);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
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

		String payload = WiSeUtils.getPayload(req);
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			// Permission check must be move to gateway someday.
			ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			JSONObject obj = new JSONObject(payload);
			String mkey = obj.optString("mkey");
			RichMenu rm = RichMenu.getByMKey(t.getId(), mkey);
			if (rm == null) {
				String msgType = obj.optString("msgType");
				String msgName = obj.optString("msgName");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, msgType, msgT, msgName, obj.optString("mkey")).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}

				
				RichMenu msg = om.readValue(obj.toString(2), RichMenu.class);
			
				msg.setId(null);
				msg.setTenantId(t.getId());
	
				RichMenu.saveOrUpdate(msg);
				resp.getWriter().append(new JSONObject()
						.put("id", msg.getId())
						.put("name", msg.getMsgName())
						.put("mkey", msg.getMkey())
						.put("status", this.getClass().getName() + "::POST")
						.toString());
				resp.getWriter().flush();
				log.info("global.success!");
			} else {
				resp.getWriter().append("{\"result\":\"Mkey Exist\"}");
				resp.getWriter().flush();
				resp.setStatus(500);
				log.info("global.failed, Mkey Exist!");
			}
		} catch (JSONException e) {
			e.printStackTrace();
			log.info("global.failed, " + e.toString());
		}
	}
	
	public static void checkRobotImageFile(Tenant t, String baseUrl, String contextPath, String rmMkey) throws IOException {
		if (baseUrl.isEmpty())
			return;
		String fileName = StringUtils.substringAfterLast(baseUrl, "/");
		RobotImageFile imgFile = RobotImageFile.getBy(t.getId(), fileName, RichMenu.class.getName(), "img/webRM",
				rmMkey);
		if (imgFile != null)
			return;
		Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getOldPath()).append(File.separator)
				.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
		Path newPath = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
				.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
		if (imgFile == null && Files.exists(path)) {
			createRobotImageFile(t, path, fileName, rmMkey);
		} else if (imgFile == null && Files.exists(newPath)) {
			createRobotImageFile(t, newPath, fileName, rmMkey);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void createRobotImageFile(Tenant t, Path path, String fileName, String rmMkey) throws IOException {
		InputStream imageInFile = Files.newInputStream(path);
		Blob blob = Hibernate.createBlob(imageInFile);
		new RobotImageFile(t.getId(), "img/webRM", RichMenu.class.getName(), fileName, rmMkey, blob);
	}

	/**
	 * R
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			String uri = xssStr.getCheck(req.getRequestURI());
			String contextPath = req.getContextPath().replace("/", "");
			String idStr = StringUtils.substringAfterLast(uri, "/");
			
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				JSONSerializer js = new JSONSerializer();
				List<RichMenu> richMenus = RichMenu.list(t.getId());
				for (RichMenu richMenu: richMenus) {
					JSONObject tpl = new JSONObject(richMenu.getMsgTemplate());
					JSONObject size = tpl.optJSONObject("size", new JSONObject());
					if (!size.isEmpty() && size.containsKey("height")) {
						richMenu.setFixHeight(size.optString("height"));
					}
				}
				try {
					for (RichMenu richMenu : richMenus) {
						JSONObject tpl = new JSONObject(richMenu.getMsgTemplate());
						String baseUrl = tpl.optString("baseUrl", "");
						checkRobotImageFile(t, baseUrl, contextPath, richMenu.getMkey());
					}
				} catch (Exception e) {
					log.log(Level.ERROR, "ERROR in checkRobotImageFile : " + e.toString());
				}
				String s = js.serialize(richMenus);
				resp.getWriter().append(xssStr.getFakeCheck(s));
				resp.getWriter().flush();
			} else {
				RichMenu msg = null;
				try {
					int id = Integer.parseInt(idStr);
					msg = RichMenu.get(id);
				}
				catch (Exception ex) {
					// it's Mkey
					msg = RichMenu.getByMKey(t.getId(), idStr);
				}

				if (msg.getTenantId().intValue() == t.getId()) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);
					
					resp.getWriter().append(xssStr.getFakeCheck(s));
					resp.getWriter().flush();
				} else {
					resp.getWriter().append("{}");
					resp.getWriter().flush();
					System.out.println("Intrusion detected: tenant[" + t.getId() + "] trying to access data of tenant["
							+ msg.getTenantId() + "]");
				}
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

		String payload = WiSeUtils.getPayload(req);
		JSONObject errorResult = new JSONObject();
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			// Permission check must be move to gateway someday.
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
			String uri = xssStr.getCheck(req.getRequestURI());
			ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				resp.getWriter().append("{}"); // not support update whole
				resp.getWriter().flush();
				log.info("global.failed, not support delete whole!");
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMenu origMsg = RichMenu.get(id);
				log.info("Update rich message :[" + origMsg.getId() + "]");
				
				JSONObject obj = new JSONObject(payload);
				String mkey = obj.optString("mkey");
				RichMenu rm = RichMenu.getByMKey(t.getId(), mkey);
				if (rm == null || (rm != null && origMsg.getMkey().equals(mkey))) {
					String msgType = obj.optString("msgType");
					String msgName = obj.optString("msgName");
					JSONObject msgT = obj.optJSONObject("msgTemplate");
					if (msgT != null) {
						if (rm == null) {
							RobotImageFile.updateMkeyInfo(origMsg.getMkey(), mkey, RichMenu.class.getName(),
									"img/webRM", t.getId());
						}
						obj.put("msgTemplate", pruneInvalidData(t, msgType, msgT, msgName, obj.optString("mkey")).toString());
					} else {
						obj.put("msgTemplate", "{}");
					}
					
					RichMenu menu = om.readValue(obj.toString(2), RichMenu.class);
					
					// 安全檢核
					if (!menu.getTenantId().equals(t.getId()) || !menu.getId().equals(origMsg.getId())) {
						String errorStr = "Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
								+ menu.getId() + " (t=" + menu.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]";
						errorResult.put("error", errorStr).put("status", this.getClass().getName() + "::PUT");
						throw new Exception(errorStr);
					}
				
					RichMenu.saveOrUpdate(menu);
					resp.getWriter().append(new JSONObject()
							.put("id", menu.getId())
							.put("name", menu.getMsgName())
							.put("mkey", menu.getMkey())
							.put("status", this.getClass().getName() + "::PUT")
							.toString());		// not support update whole
					resp.getWriter().flush();	// collection
					log.info("global.success!");
				} else {
					resp.getWriter().append("{\"result\":\"Mkey Exist\"}");
					resp.getWriter().flush();
					resp.setStatus(500);
					log.info("global.failed, Mkey Exist!");
				}
			}
		} catch (Exception e) {			
			resp.getWriter().append(xssStr.getFakeCheck(errorResult.toString()));
			resp.getWriter().flush();
			resp.setStatus(500);
			log.info("global.failed, " + e.toString());
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
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			String uri = xssStr.getCheck(req.getRequestURI());
			
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				resp.getWriter().append("{}");	// not support delete whole
				resp.getWriter().flush();		// collection
				log.info("global.failed, not support delete whole!");
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMenu msg = RichMenu.get(id);
				if (msg != null && msg.getTenantId().equals(t.getId())) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);
					resp.getWriter().append(xssStr.getFakeCheck(s));
					resp.getWriter().flush();
					RichMenu.delete(msg);
					
					RobotImageFile.listDeleteBy(t.getId(), RichMenu.class.getName(), msg.getMkey(), true);
					log.info("global.success!");
				} else {
					resp.getWriter().append("{}");
					resp.getWriter().flush();
					String error = "Intrusion detected: tenant[" + t.getId() + "] trying to access data of tenant["
							+ msg.getTenantId() + "]";
					log.info("global.failed, " + error);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("global.failed, " + e.toString());
		}
	}

}
