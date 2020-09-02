package com.intumit.solr.robot.connector.line;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

/**
 * Restful CRUD
 * 
 * @author herb
 *
 */
@WebServlet(urlPatterns = { "/wiseadm/line/richMessages", "/wiseadm/line/richMessages/*" })
public class RichMessageServlet extends HttpServlet {
	private static final CharSequence COLLECTION_URL_PATTERN = "/wiseadm/line/richMessages";

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

	private static final Integer BASE_URL_240 = 240;
	private static final Integer BASE_URL_300 = 300;
	private static final Integer BASE_URL_460 = 460;
	private static final Integer BASE_URL_700 = 700;
	private static final Integer BASE_URL_1040 = 1040;
	private static final Integer BASE_SIZE = BASE_URL_1040;

	private static final Integer[] BASE_URL_SIZE_COLLECTION = { BASE_URL_240, BASE_URL_300, BASE_URL_460, BASE_URL_700,
			BASE_URL_1040 };

	public JSONObject pruneInvalidData(Tenant tenant, String msgType, JSONObject tpl) {
		try {
			JSONObject botCfg = new JSONObject(StringUtils.defaultString(tenant.getLineBotConfigJson(), "{}"));
			JSONObject lineCfg = new JSONObject();
			if (botCfg.has("line")) {
				lineCfg = botCfg.optJSONObject("line");
			} else {
				lineCfg = botCfg;
			}
			if ("buttons".equals(msgType)) {
				JSONObject t = tpl.optJSONObject("template");

				if (!t.has("actions")) {
					return new JSONObject();
				}

				if (t.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(t.getString("thumbnailImageUrl")) == null)
						t.remove("thumbnailImageUrl");
					else {
						t.put("thumbnailImageUrl", processImageAndReturnUrl(tenant.getId(), lineCfg, t.optString("thumbnailImageUrl")));
					}
				}

				t.put("actions", pruneInvalidActions(t.getJSONArray("actions")));
				t.put("actions", prunePostbackActions(t.getJSONArray("actions")));
			} else if ("carousel".equals(msgType)) {
				JSONObject t = tpl.optJSONObject("template");
				JSONArray cols = t.optJSONArray("columns");
				JSONArray nc = new JSONArray();

				for (int i = 0; i < cols.length(); i++) {
					JSONObject c = cols.getJSONObject(i);

					if (c.has("thumbnailImageUrl")) {
						if (StringUtils.trimToNull(c.getString("thumbnailImageUrl")) == null)
							c.remove("thumbnailImageUrl");
						else {
							c.put("thumbnailImageUrl", processImageAndReturnUrl(tenant.getId(), lineCfg, c.getString("thumbnailImageUrl")));
						}
					}

					if (c.has("actions") && StringUtils.trimToNull(c.optString("text")) != null) { // LINE 的規則
						c.put("actions", pruneInvalidActions(c.getJSONArray("actions")));
						t.put("actions", prunePostbackActions(t.getJSONArray("actions")));
						nc.put(c);
					}
				}

				t.put("columns", nc);
			} else if ("imagemap".equals(msgType)) {

				if (!tpl.has("actions")) {
					return new JSONObject();
				}

				if (tpl.has("baseUrl")) {
					if (StringUtils.trimToNull(tpl.getString("baseUrl")) != null) {
						processBaseUrlAndFixRatio(tenant.getId(), lineCfg, tpl);
						//tpl.put("baseUrl", processBaseUrlAndFixRatio(tenant.getId(), lineCfg, tpl.getString("baseUrl")));
					}
				}

				tpl.put("actions", pruneInvalidActions(tpl.getJSONArray("actions")));
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

	private void processBaseUrlAndFixRatio(Integer tid, JSONObject lineCfg, JSONObject tpl) {
		try {
			String dataUrl = tpl.getString("baseUrl");
			Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
			
			if (m.find()) {
				File original = null;
				try {
					String encodedName = getName(tid);
					InputStream imageIs = getFileFromDataURI(m.group(2));
					String filePath = WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise"
							+ File.separator + "img" + File.separator + "line" + File.separator + encodedName;
					
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

					File baseUrlDir = new File(filePath);
					FileUtils.forceMkdir(baseUrlDir);

					for (final Integer size : BASE_URL_SIZE_COLLECTION) {
						File temp = new File(baseUrlDir, size.toString() + ".PNG");
						File dest = new File(baseUrlDir, size.toString());
						Thumbnails.of(original).width(size).outputFormat("PNG").toFile(temp);
						temp.renameTo(dest);
						
						// 若是 1040 的，來開始修正各種比例問題
						if (size == BASE_SIZE) {
							BufferedImage bi = Thumbnails.of(dest).width(size).asBufferedImage();
							int w = bi.getWidth();
							int h = bi.getHeight();
							System.out.println("w=" + w + ",h=" +h);
							
							float wm = 1f;
							float hm = 1f;
							
							JSONObject baseSize = tpl.getJSONObject("baseSize");
							int tplBaseW = baseSize.getInt("width");
							int tplBaseH = baseSize.getInt("height");
							
							if (tplBaseW != w) {
								wm = (float)w / (float)tplBaseW;
							}
							if (tplBaseH != h) {
								hm = (float)h / (float)tplBaseH;
							}
							
							if (wm != 1f) {
								System.out.println("Imagemap's baseSize.width need to be adjust by ratio [" + wm + "]");
								baseSize.put("width", w);
							}
							if (hm != 1f) {
								System.out.println("Imagemap's baseSize.height need to be adjust by ratio [" + hm + "]");
								baseSize.put("height", h);
							}
							
							JSONArray actions = tpl.optJSONArray("actions", new JSONArray());
							
							for (int i=0; i < actions.length(); i++) {
								JSONObject a = actions.getJSONObject(i);
								
								if (a.has("area")) {
									JSONObject area = a.getJSONObject("area");
									
									if (wm != 1f) {
										area.put("x", (int)Math.round(area.getInt("x") * wm));
										area.put("width", (int)Math.round(area.getInt("width") * wm));
									}
									if (hm != 1f) {
										area.put("y", (int)Math.round(area.getInt("y") * hm));
										area.put("height", (int)Math.round(area.getInt("height") * hm));
									}
								}
							}
						}
					}

					tpl.put("baseUrl", getUrlPrefix(lineCfg) + "/img/line/" + encodedName);
				} catch (Exception e) {
					e.printStackTrace();
					tpl.put("baseUrl", dataUrl);
				}
				finally {
					if (original != null) FileUtils.deleteQuietly(original);
				}
			} else {
				tpl.put("baseUrl", dataUrl);
				
				// 每次儲存都會讓 area 跑掉，所以都要重新 check 一次。
				final Integer size = BASE_SIZE;
				String filePath = WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise"
						+ File.separator + "img" + File.separator + "line" + File.separator + StringUtils.substringAfterLast(dataUrl, "/");

				File baseUrlDir = new File(filePath);
				File dest = new File(baseUrlDir, size.toString());
				
				// 若是 1040 的，來開始修正各種比例問題
				BufferedImage bi = Thumbnails.of(dest).width(size).asBufferedImage();
				int w = bi.getWidth();
				int h = bi.getHeight();
				System.out.println("w=" + w + ",h=" +h);
				
				float wm = 1f;
				float hm = 1f;
				
				JSONObject baseSize = tpl.getJSONObject("baseSize");
				int tplBaseW = baseSize.getInt("width");
				int tplBaseH = baseSize.getInt("height");
				
				if (tplBaseW != w) {
					wm = (float)w / (float)tplBaseW;
				}
				if (tplBaseH != h) {
					hm = (float)h / (float)tplBaseH;
				}
				
				if (wm != 1f) {
					System.out.println("Imagemap's baseSize.width need to be adjust by ratio [" + wm + "]");
					baseSize.put("width", w);
				}
				if (hm != 1f) {
					System.out.println("Imagemap's baseSize.height need to be adjust by ratio [" + hm + "]");
					baseSize.put("height", h);
				}
				
				JSONArray actions = tpl.optJSONArray("actions", new JSONArray());
				
				for (int i=0; i < actions.length(); i++) {
					JSONObject a = actions.getJSONObject(i);
					
					if (a.has("area")) {
						JSONObject area = a.getJSONObject("area");
						
						if (wm != 1f) {
							area.put("x", (int)Math.round(area.getInt("x") * wm));
							area.put("width", (int)Math.round(area.getInt("width") * wm));
						}
						if (hm != 1f) {
							area.put("y", (int)Math.round(area.getInt("y") * hm));
							area.put("height", (int)Math.round(area.getInt("height") * hm));
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String processImageAndReturnUrl(Integer tid, JSONObject lineCfg, String dataUrl) {
		Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
		if (m.find()) {
			try {
				String encodedName = getName(tid);
				OutputStream os = new FileOutputStream(
						WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise"
								+ File.separator + "img" + File.separator + "line" + File.separator + encodedName);

				InputStream imageIs = getFileFromDataURI(m.group(2));
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = imageIs.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				imageIs.close();
				os.flush();
				os.close();
				return getUrlPrefix(lineCfg) + "/img/line/" + encodedName;
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

	private static String getUrlPrefix(JSONObject lineCfg) throws JSONException {
		if (lineCfg.has("domainUrl") && StringUtils.isNotBlank(lineCfg.getString("domainUrl"))) {
			return lineCfg.getString("domainUrl") + WiseSystemConfigFacade.getInstance().get().getContextPath();
		} else {
			return WiseSystemConfigFacade.getInstance().get().getFullUrlBase();
		}
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
				obj.put("msgTemplate", pruneInvalidData(t, msgType, msgT).toString());
			} else {
				obj.put("msgTemplate", "{}");
			}
			// System.out.println(obj.toString(2));

			RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);
			msg.setId(null);
			msg.setTenantId(t.getId());

			RichMessage.saveOrUpdate(msg);
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
			Tenant t = Tenant.getFromSession(req.getSession());
			String uri = req.getRequestURI();

			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				JSONSerializer js = new JSONSerializer();
				String s = js.serialize(RichMessage.list(t.getId(), null));
				resp.getWriter().println(s);
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMessage msg = RichMessage.get(id);

				if (msg.getTenantId().intValue() == t.getId()) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);
					resp.getWriter().println(s);
				} else {
					resp.getWriter().println("{}");
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
					obj.put("msgTemplate", pruneInvalidData(t, msgType, msgT).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
				// System.out.println(obj.toString(2));

				RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);

				// 安全檢核
				if (!msg.getTenantId().equals(t.getId())) {
					System.out.println("Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
							+ msg.getId() + " (t=" + msg.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]");
					return;
				}

				RichMessage.saveOrUpdate(msg);
				resp.getWriter().println("{}"); // not support update whole
												// collection
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMessage origMsg = RichMessage.get(id);

				System.out.println("Update rich message :[" + origMsg.getId() + "]");

				String payload = WiSeUtils.getPayload(req);
				JSONObject obj = new JSONObject(payload);
				String msgType = obj.optString("msgType");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, msgType, msgT).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
				// System.out.println(obj.toString(2));
				RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);

				// 安全檢核
				if (!msg.getTenantId().equals(t.getId()) || !msg.getId().equals(origMsg.getId())) {
					System.out.println("Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
							+ msg.getId() + " (t=" + msg.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]");
					return;
				}

				RichMessage.saveOrUpdate(msg);
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
				RichMessage msg = RichMessage.get(id);

				if (msg != null && msg.getTenantId().equals(t.getId())) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);
					resp.getWriter().println(s);
					RichMessage.delete(msg);
				} else {
					resp.getWriter().println("{}");
					System.out.println("Intrusion detected: tenant[" + t.getId() + "] trying to access data of tenant["
							+ msg.getTenantId() + "]");
				}
			}
			// resp.getWriter().println(new JSONObject().put("status",
			// this.getClass().getName() + "::DELETE").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String[] getBaseUrlSizeCollection() {
		String str = Arrays.toString(BASE_URL_SIZE_COLLECTION).replaceAll("\\s+", ""); // [1, 2, 3, 4, 5] => [1,2,3,4,5]
		String[] baseUrlSizeCollection = str.substring(1, str.length() - 1).split(",");
		return baseUrlSizeCollection;
	}

}
