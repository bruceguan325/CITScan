package com.intumit.solr.robot.connector.web;

import java.awt.image.BufferedImage;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.RobotImageFile;
import com.intumit.solr.robot.RobotImageFilePath;
import com.intumit.solr.robot.connector.line.NaverLineActionType;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.util.XssStringFilter;
import com.intumit.syslog.OperationLogEntity;
import com.intumit.systemconfig.RobotImageFileConfig;

import flexjson.JSONSerializer;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Restful CRUD
 * 
 * @author herb
 *
 */
@WebServlet(urlPatterns = { "/wiseadm/webRM/richMessages", "/wiseadm/webRM/richMessages/*" })
public class RichMessageServlet extends HttpServlet {

	public static Logger log = Logger.getLogger(RichMessageServlet.class.getName());

	static XssStringFilter xssStr = new XssStringFilter();

	private static final long serialVersionUID = 1L;
	
	private static final CharSequence COLLECTION_URL_PATTERN = "/wiseadm/webRM/richMessages";

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

	private static final Integer[] BASE_URL_SIZE_COLLECTION = { BASE_URL_240, BASE_URL_300, BASE_URL_460, BASE_URL_700, BASE_URL_1040 };
	private static final Pattern templatePattern = Pattern.compile("\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)(:.+?)?\\}\\}");
	
	@SuppressWarnings("deprecation")
	public JSONObject pruneInvalidData(Tenant tenant, String contextPath, String msgType, JSONObject tpl, String mkey) {
		try {
			switch (msgType) {
			case "textwithbutton":
			case "textwithoutbutton":
			case "threegrid":
			case "twogrid":
			case "onegrid":
			case "carousel":
				JSONObject t = tpl.optJSONObject("template");
				JSONArray cols = t.optJSONArray("columns");
				JSONArray nc = new JSONArray();
				List<String> thumbnailImageUrls = new ArrayList<String>();

				for (int i = 0; i < cols.length(); i++) {
					JSONObject c = cols.getJSONObject(i);

					if (c.has("thumbnailImageUrl")) {
						if (StringUtils.trimToNull(c.getString("thumbnailImageUrl")) == null)
							c.remove("thumbnailImageUrl");
						else {
							c.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
									c.getString("thumbnailImageUrl"), null));
							thumbnailImageUrls.add(c.optString("thumbnailImageUrl"));
						}
					}
					
					// 取代特定語法 {{L:按鈕文字:網址}}需改成html語法<a href="網址">文字</a>
					if("onegrid".equals(msgType) && StringUtils.trimToNull(c.optString("text1")) != null) {
                        String text = c.optString("text1");
                        if (text != null && text.contains("{{L:")) {                    
                            Matcher matcher = templatePattern.matcher(text);
                            int diff = 0;
                            while (matcher.find()) {
                                int start = matcher.start();
                                int end = matcher.end();
                                String originalStr = matcher.group();
                                String name = StringUtils.trim(matcher.group(1));
                                if (name.equalsIgnoreCase("L")) {
                                    String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
                                    String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");                       
                                    String link = StringUtils.substringAfter(val, ":");
                                    String res = "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">" + linkName
                                            + "</a>";
                                    text = new StringBuilder(text.substring(0, start+diff))
                                            .append(res)
                                            .append(text.substring(end+diff)).toString();
                                    diff = res.length() - originalStr.length();
                                }                       
                            }                   
                            c.put("text1", text);
                        } else {
                        	c.put("text1", text);
                        	}
                    }

					// 有按鈕類的版型type都放入column array
					if (c.has("actions") && StringUtils.trimToNull(c.optString("title")) != null) { 
						c.put("actions", pruneInvalidActions(c.optJSONArray("actions", new JSONArray())));
						c.put("actions", prunePostbackActions(c.optJSONArray("actions", new JSONArray())));
						nc.put(c);
					}
					
					if ("twogrid".equals(msgType) && StringUtils.trimToNull(c.optString("title")) != null) { 
						nc.put(c);
					}
					
					if ("textwithoutbutton".equals(msgType) && StringUtils.trimToNull(c.optString("title")) != null) { 
						nc.put(c);
					}
				}

				String homePath = new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
						.append(RichMessage.imgPath).toString();
				
				try {
					for (String thumbnailImageUrl : thumbnailImageUrls) {
						String encodedName = StringUtils.substringAfterLast(thumbnailImageUrl, "/");
						RobotImageFile imgFile = RobotImageFile.getBy(tenant.getId(), encodedName,
								RichMessage.class.getName(), "img/webRM", mkey);
						if (imgFile == null) {
							Path path = Paths.get(new StringBuilder().append(homePath).append(File.separator).append(encodedName).toString());
							if (Files.exists(path)) {
								InputStream imageInFile = Files.newInputStream(path);
								Blob blob = Hibernate.createBlob(imageInFile);
								new RobotImageFile(tenant.getId(), "img/webRM", RichMessage.class.getName(),
										encodedName, mkey, blob);
								imageInFile.close();
							}
						}
					}

					List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(tenant.getId(),
							RichMessage.class.getName(), mkey, false);
					String[] baseUrlSizeCollection = getBaseUrlSizeCollection();
					boolean deleteFolder = false;
					String folder = "";
					for (RobotImageFile imgFile : imgFiles) {
						if (StringUtils.equalsAny(imgFile.getMkey(), baseUrlSizeCollection) && !deleteFolder) {
							deleteFolder = true;
							folder = imgFile.getNamespace();
						}
						if (!thumbnailImageUrls
								.contains(contextPath + "/" + imgFile.getNamespace() + "/" + imgFile.getMkey())) {
							imgFile.delete();
						}
					}
					if (deleteFolder) {
						RobotImageFile.deleteFolderPath(folder);
					}

				} catch (IOException e) {
					log.log(Level.ERROR, "ERROR in thumbnailImageUrl to RobotImageFile : " + e.toString());
				}

				t.put("columns", nc);
				break;
			case "imagemap":
				if (!tpl.has("actions")) {
					return new JSONObject();
				}

				if (tpl.has("baseUrl")) {
					if (StringUtils.trimToNull(tpl.getString("baseUrl")) != null) {
						processBaseUrlAndFixRatio(tenant.getId(), contextPath, tpl, mkey, null);
					}
				} else {
					RobotImageFile.listDeleteBy(tenant.getId(), RichMessage.class.getName(), mkey, true);
				}

				tpl.put("actions", pruneInvalidActions(tpl.getJSONArray("actions")));
				break;
//			case "quickReplies":
//				JSONObject quickReply = tpl.optJSONObject("quickReply");
//				if (!quickReply.has("items")) {
//					return new JSONObject();
//				}
//				RobotImageFile.listDeleteBy(tenant.getId(), RichMessage.class.getName(), mkey, true);
//				quickReply.put("items", pruneInvalidActions(quickReply.getJSONArray("items")));
//				quickReply.put("items", prunePostbackActions(quickReply.getJSONArray("items")));
//				break;
			case "imageCarousel":
				JSONObject imageT = tpl.optJSONObject("template");
				JSONArray imageCols = imageT.optJSONArray("columns");
				JSONArray jsonArray = new JSONArray();
				List<String> icThumbnailImageUrls = new ArrayList<String>();

				for (int i = 0; i < imageCols.length(); i++) {
					JSONObject c = imageCols.getJSONObject(i);

					if (!c.has("imgAltText") || c.optString("imgAltText").isEmpty()) {
						continue;
					}
					if (c.has("thumbnailImageUrl")) {
						if (StringUtils.trimToNull(c.getString("thumbnailImageUrl")) == null)
							c.remove("thumbnailImageUrl");
						else {
							c.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
									c.getString("thumbnailImageUrl"), null));
							icThumbnailImageUrls.add(c.optString("thumbnailImageUrl"));
						}
					}

					if (StringUtils.trimToNull(c.optString("uri")) != null) { 
						c.put("uri", c.optString("uri"));
						jsonArray.put(c);
					}
				}

				String icHomePath = new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
						.append(RichMessage.imgPath).toString();
				
				try {
					for (String thumbnailImageUrl : icThumbnailImageUrls) {
						String encodedName = StringUtils.substringAfterLast(thumbnailImageUrl, "/");
						RobotImageFile imgFile = RobotImageFile.getBy(tenant.getId(), encodedName,
								RichMessage.class.getName(), "img/webRM", mkey);
						if (imgFile == null) {
							Path path = Paths.get(new StringBuilder().append(icHomePath).append(File.separator).append(encodedName).toString());
							if (Files.exists(path)) {
								InputStream imageInFile = Files.newInputStream(path);
								Blob blob = Hibernate.createBlob(imageInFile);
								new RobotImageFile(tenant.getId(), "img/webRM", RichMessage.class.getName(),
										encodedName, mkey, blob);
								imageInFile.close();
							}
						}
					}

					List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(tenant.getId(),
							RichMessage.class.getName(), mkey, false);
					String[] baseUrlSizeCollection = getBaseUrlSizeCollection();
					boolean deleteFolder = false;
					String folder = "";
					for (RobotImageFile imgFile : imgFiles) {
						if (StringUtils.equalsAny(imgFile.getMkey(), baseUrlSizeCollection) && !deleteFolder) {
							deleteFolder = true;
							folder = imgFile.getNamespace();
						}
						if (!icThumbnailImageUrls
								.contains(contextPath + "/" + imgFile.getNamespace() + "/" + imgFile.getMkey())) {
							imgFile.delete();
						}
					}
					if (deleteFolder) {
						RobotImageFile.deleteFolderPath(folder);
					}

				} catch (IOException e) {
					log.log(Level.ERROR, "ERROR in thumbnailImageUrl to RobotImageFile : " + e.toString());
				}

				imageT.put("columns", jsonArray);
				break;
			case "optionsWithIconText":
				JSONObject template = tpl.optJSONObject("template");
				String thumbnailImageUrl = null;
				if (!template.has("columns")) {
					return new JSONObject();
				}
				String extendContent = template.optString("extendContent");
				if (extendContent != null && extendContent.contains("{{L:")) {					
					Matcher matcher = templatePattern.matcher(extendContent);
					int diff = 0;
					while (matcher.find()) {
						int start = matcher.start();
						int end = matcher.end();
						String originalStr = matcher.group();
						String name = StringUtils.trim(matcher.group(1));
						if (name.equalsIgnoreCase("L")) {
							String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
							String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");						
							String link = StringUtils.substringAfter(val, ":");
							String res = "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">" + linkName
									+ "</a>";
							extendContent = new StringBuilder(extendContent.substring(0, start+diff))
									.append(res)
									.append(extendContent.substring(end+diff)).toString();
							diff = res.length() - originalStr.length();
						}						
					}					
					template.put("extendContent", extendContent);
				}
				JSONArray columns = template.optJSONArray("columns");
				columns = pruneInvalidActions(columns);
				columns = prunePostbackActions(columns);
				template.put("columns", columns);
				
				if (template.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(template.getString("thumbnailImageUrl")) == null)
						template.remove("thumbnailImageUrl");
					else {
						template.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
								template.getString("thumbnailImageUrl"), null));	
						thumbnailImageUrl = template.optString("thumbnailImageUrl");
					}
				}
				processImage(tenant, thumbnailImageUrl, mkey, contextPath);				
				break;
			case "optionsWithImageTitle":
				JSONObject iTTemplate = tpl.optJSONObject("template");
				String iTThumbnailImageUrl = null;
				if (!iTTemplate.has("columns")) {
					return new JSONObject();
				}
				
				JSONArray iTColumns = iTTemplate.optJSONArray("columns");
				iTColumns = pruneInvalidActions(iTColumns);
				iTColumns = prunePostbackActions(iTColumns);
				iTTemplate.put("columns", iTColumns);				
				
				if (iTTemplate.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(iTTemplate.getString("thumbnailImageUrl")) == null)
						iTTemplate.remove("thumbnailImageUrl");
					else {
						iTTemplate.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
								iTTemplate.getString("thumbnailImageUrl"), null));	
						iTThumbnailImageUrl = iTTemplate.optString("thumbnailImageUrl");
					}
				}
				processImage(tenant, iTThumbnailImageUrl, mkey, contextPath);			
				break;
			case "imageModal":
				JSONObject iMTemplate = tpl.optJSONObject("template");
				String iMThumbnailImageUrl = null;
				if (!iMTemplate.has("columns")) {
					return new JSONObject();
				}
				
				JSONArray iMColumns = iMTemplate.optJSONArray("columns");
				iMColumns = pruneInvalidActions(iMColumns);
				iMColumns = prunePostbackActions(iMColumns);
				iMTemplate.put("columns", iMColumns);				
				
				if (iMTemplate.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(iMTemplate.getString("thumbnailImageUrl")) == null)
						iMTemplate.remove("thumbnailImageUrl");
					else {
						iMTemplate.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
								iMTemplate.getString("thumbnailImageUrl"), null));	
						iMThumbnailImageUrl = iMTemplate.optString("thumbnailImageUrl");
					}
				}
				processImage(tenant, iMThumbnailImageUrl, mkey, contextPath);			
				break;
			case "textModal":
				JSONObject tMTemplate = tpl.optJSONObject("template");
				if (!tMTemplate.has("columns")) {
					return new JSONObject();
				}
				String tMExtendContent = tMTemplate.optString("extendContent");
				if (tMExtendContent != null && tMExtendContent.contains("{{L:")) {					
					Matcher matcher = templatePattern.matcher(tMExtendContent);
					int diff = 0;
					while (matcher.find()) {
						int start = matcher.start();
						int end = matcher.end();
						String originalStr = matcher.group();
						String name = StringUtils.trim(matcher.group(1));
						if (name.equalsIgnoreCase("L")) {
							String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
							String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");						
							String link = StringUtils.substringAfter(val, ":");
							String res = "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">" + linkName
									+ "</a>";
							tMExtendContent = new StringBuilder(tMExtendContent.substring(0, start+diff))
									.append(res)
									.append(tMExtendContent.substring(end+diff)).toString();
							diff = res.length() - originalStr.length();
						}						
					}					
				}		
				tMTemplate.put("extendContent", tMExtendContent);
				JSONArray tMColumns = tMTemplate.optJSONArray("columns");
				tMColumns = pruneInvalidActions(tMColumns);
				tMColumns = prunePostbackActions(tMColumns);
				tMTemplate.put("columns", tMColumns);
				break;
			case "alert":
				JSONObject alertTemplate = tpl.optJSONObject("template");
				String alertThumbnailImageUrl = null;
				if (!alertTemplate.has("columns")) {
					return new JSONObject();
				}
				
				JSONArray alertColumns = alertTemplate.optJSONArray("columns");
				alertColumns = pruneInvalidActions(alertColumns);
				alertColumns = prunePostbackActions(alertColumns);
				alertTemplate.put("columns", alertColumns);				
				
				if (alertTemplate.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(alertTemplate.getString("thumbnailImageUrl")) == null)
						alertTemplate.remove("thumbnailImageUrl");
					else {
						alertTemplate.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
								alertTemplate.getString("thumbnailImageUrl"), null));	
						alertThumbnailImageUrl = alertTemplate.optString("thumbnailImageUrl");
					}
				}
				processImage(tenant, alertThumbnailImageUrl, mkey, contextPath);
				break;
			case "buttons":
			case "quickReplies":
			case "stretch":
				template = tpl.optJSONObject("template");
				thumbnailImageUrl = null;
				if (!template.has("actions")) {
					return new JSONObject();
				}
				String text = template.optString("text");
				if (text != null && text.contains("{{L:")) {					
					Matcher matcher = templatePattern.matcher(text);
					int diff = 0;
					while (matcher.find()) {
						int start = matcher.start();
						int end = matcher.end();
						String originalStr = matcher.group();
						String name = StringUtils.trim(matcher.group(1));
						if (name.equalsIgnoreCase("L")) {
							String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
							String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");						
							String link = StringUtils.substringAfter(val, ":");
							String res = "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">" + linkName
									+ "</a>";
							text = new StringBuilder(text.substring(0, start+diff))
									.append(res)
									.append(text.substring(end+diff)).toString();
							diff = res.length() - originalStr.length();
						}						
					}					
					template.put("text", text);
				}
				JSONArray actions = template.optJSONArray("actions");
				actions = pruneInvalidActions(actions);
				actions = prunePostbackActions(actions);
				template.put("actions", actions);
				
				if (template.has("thumbnailImageUrl")) {
					if (StringUtils.trimToNull(template.getString("thumbnailImageUrl")) == null)
						template.remove("thumbnailImageUrl");
					else {
						template.put("thumbnailImageUrl", processImageAndReturnUrl(tenant, contextPath,
								template.getString("thumbnailImageUrl"), null));	
						thumbnailImageUrl = template.optString("thumbnailImageUrl");
					}
				}
				processImage(tenant, thumbnailImageUrl, mkey, contextPath);				
				break;
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
	protected static void processBaseUrlAndFixRatio(Integer tid, String contextPath, JSONObject tpl, String mkey, String assignName) {
		try {
			String dataUrl = tpl.getString("baseUrl");
			Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
			String homePath = new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
					.append(RichMessage.imgPath).toString();
			if (m.find()) {
				File original = null;
				try {
					String encodedName = assignName == null ? getName(tid) : assignName;
					InputStream imageIs = getFileFromDataURI(m.group(2));
					Path path = Paths.get(new StringBuilder().append(homePath).append(File.separator).append(encodedName).toString());
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

					RobotImageFile.listDeleteBy(tid, RichMessage.class.getName(), mkey, true);
					File baseUrlDir = new File(filePath);
					FileUtils.forceMkdir(baseUrlDir);

					for (final Integer size : BASE_URL_SIZE_COLLECTION) {
						File temp = new File(baseUrlDir, size.toString() + ".PNG");
						File dest = new File(baseUrlDir, size.toString());
						Thumbnails.of(original).width(size).outputFormat("PNG").toFile(temp);
						temp.renameTo(dest);
						
						InputStream imageInFile = new FileInputStream(dest);
						if (imageInFile != null) {
							Blob blob = Hibernate.createBlob(imageInFile);
							new RobotImageFile(tid, "img/webRM/" + encodedName, RichMessage.class.getName(), "" + size, mkey, blob);
							imageInFile.close();
						}
						
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
					String newPath = "/" + RobotImageFileConfig.getImageFileConfig()[0];
					tpl.put("baseUrl", contextPath + newPath + "/img/webRM/" + encodedName);
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
				String filePath = new StringBuilder().append(homePath)
						.append(StringUtils.substringAfterLast(dataUrl, "/")).toString();

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

	protected static String processImageAndReturnUrl(Tenant t, String contextPath, String dataUrl, String assignName) {
		Matcher m = DATA_URL_PATTERN.matcher(dataUrl);
		if (m.find()) {
			try {
				String encodedName = assignName == null ? getName(t.getId()) : assignName;

				Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
						.append(RichMessage.imgPath).toString());
				File storeDir = new File(path.toString());
				
				if (!storeDir.exists()) Files.createDirectories(Paths.get(path.toFile().getCanonicalPath()));
				
				path = Paths
						.get(new StringBuilder().append(path).append(File.separator).append(encodedName).toString());

				if (Files.exists(path)) {
					Files.delete(path);
				}
				
				OutputStream os = Files.newOutputStream(path);

				InputStream imageIs = getFileFromDataURI(m.group(2));
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = imageIs.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				imageIs.close();
				os.flush();
				os.close();
				
				return contextPath + "/img/webRM/" + encodedName;
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
	
	@SuppressWarnings("deprecation")
	private static void processImage(Tenant tenant, String thumbnailImageUrl, String mkey, String contextPath) {		
		if (thumbnailImageUrl != null) {
		    String homePath = new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
		            .append(RichMessage.imgPath).toString();
		    try {
		        String encodedName = StringUtils.substringAfterLast(thumbnailImageUrl, "/");
		        RobotImageFile imgFile = RobotImageFile.getBy(tenant.getId(), encodedName,
		                RichMessage.class.getName(), "img/webRM", mkey);
		        if (imgFile == null) {
		            Path path = Paths.get(new StringBuilder().append(homePath).append(File.separator).append(encodedName).toString());
		            if (Files.exists(path)) {
		                InputStream imageInFile = Files.newInputStream(path);
		                Blob blob = Hibernate.createBlob(imageInFile);
		                new RobotImageFile(tenant.getId(), "img/webRM", RichMessage.class.getName(),
		                        encodedName, mkey, blob);
		                imageInFile.close();
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		    
		}

		List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(tenant.getId(),
		        RichMessage.class.getName(), mkey, false);
		String[] baseUrlSizeCollection = getBaseUrlSizeCollection();
		boolean deleteFolder = false;
		String folder = "";
		for (RobotImageFile imgFile : imgFiles) {
		    if (StringUtils.equalsAny(imgFile.getMkey(), baseUrlSizeCollection) && !deleteFolder) {
		        deleteFolder = true;
		        folder = imgFile.getNamespace();
		    }
		    if (!thumbnailImageUrl.equals(contextPath + "/" + imgFile.getNamespace() + "/" + imgFile.getMkey())) {
		        imgFile.delete();
		    }
		}
		if (deleteFolder) {
		    RobotImageFile.deleteFolderPath(folder);
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
        Integer opLogId = (Integer) req.getAttribute("opLogId");
        OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
        if (log == null) {
            resp.setStatus(500);
            return;
        }
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			// Permission check must be move to gateway someday.
			ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			String payload = WiSeUtils.getPayload(req);
			JSONObject obj = new JSONObject(payload);
			String mkey = obj.optString("mkey");
			RichMessage rm = RichMessage.getByMKey(t.getId(), mkey);
			if (rm == null) {
				String msgType = obj.optString("msgType");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT, obj.optString("mkey")).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
	
				RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);
				msg.setId(null);
				msg.setTenantId(t.getId());
	
				RichMessage.saveOrUpdate(msg);
				resp.getWriter().append(xssStr.escapeHtml2(
						new JSONObject()
							.put("status", this.getClass().getName() + "::POST")
							.put("id", msg.getId())
							.put("name", msg.getMsgName())
							.put("mkey", msg.getMkey())
						.toString()));
				resp.getWriter().flush();
	            log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} else {
				resp.getWriter().println("{\"result\":\"Mkey Exist\"}");
				resp.setStatus(500);
				//log.info("global.failed, Mkey Exist!");
			}
		} catch (JSONException e) {
			e.printStackTrace();
            log.setStatusMessage(OperationLogEntity.Status.FAILED);
            log.appendToMoreDetails(e.toString());
		}
        log.update();
	}

	public static void checkRobotImageFile(Tenant t, String baseUrl, String contextPath, String rmMkey)
			throws IOException {
		if (baseUrl.isEmpty())
			return;
		String fileName = StringUtils.substringAfterLast(baseUrl, "/");
		RobotImageFile imgFile = RobotImageFile.getBy(t.getId(), fileName, RichMessage.class.getName(), "img/webRM",
				rmMkey);
		if (imgFile != null)
			return;
		List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(t.getId(), RichMessage.class.getName(), rmMkey,
				false);
		Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getOldPath()).append(File.separator)
				.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
		Path newPath = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
				.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
		List<String> sizes = Arrays.asList(getBaseUrlSizeCollection());
		boolean objectExist = false;
		if (imgFile == null && Files.isDirectory(path)) {
			for (String size : sizes) {
				for (RobotImageFile imgF : imgFiles) {
					if (imgF.getMkey().equals(size)) {
						objectExist = true;
						break;
					}
				}
				if (!objectExist) {
					Path p = Paths.get(new StringBuilder().append(path).append(File.separator).append(size).toString());
					createRobotImageFile(t, p, fileName, size, rmMkey);
				}
				objectExist = false;
			}
		} else if (imgFile == null && Files.isDirectory(newPath)) {
			for (String size : sizes) {
				for (RobotImageFile imgF : imgFiles) {
					if (imgF.getMkey().equals(size)) {
						objectExist = true;
						break;
					}
				}
				if (!objectExist) {
					Path np = Paths
							.get(new StringBuilder().append(newPath).append(File.separator).append(size).toString());
					createRobotImageFile(t, np, fileName, size, rmMkey);
				}
				objectExist = false;
			}
		} else if (imgFile == null && Files.exists(path)) {
			createRobotImageFile(t, path, fileName, "", rmMkey);
		} else if (imgFile == null && Files.exists(newPath)) {
			createRobotImageFile(t, newPath, fileName, "", rmMkey);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void createRobotImageFile(Tenant t, Path path, String fileName, String size, String rmMkey)
			throws IOException {
		if (Files.exists(path)) {
			InputStream imageInFile = Files.newInputStream(path);
			Blob blob = Hibernate.createBlob(imageInFile);
			if (!size.isEmpty()) {
				new RobotImageFile(t.getId(), "img/webRM/" + fileName, RichMessage.class.getName(), size, rmMkey, blob);
			} else {
				new RobotImageFile(t.getId(), "img/webRM", RichMessage.class.getName(), fileName, rmMkey, blob);
			}
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
			String uri = xssStr.getCheck(req.getRequestURI());
			String contextPath = req.getContextPath().replace("/", "");
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				JSONSerializer js = new JSONSerializer();
				List<RichMessage> richMessages = RichMessage.list(t.getId(), null);
				try {
					String baseUrl = null;
					for (RichMessage richMessage : richMessages) {
						JSONObject jo = new JSONObject(richMessage.getMsgTemplate());
						switch(richMessage.getMsgType()) {
						case "imagemap":
							baseUrl = jo.optString("baseUrl", "");
							checkRobotImageFile(t, baseUrl, contextPath, richMessage.getMkey());
							break;
						case "carousel":
							JSONObject tpl = jo.optJSONObject("template", new JSONObject());
							JSONArray columns = tpl.optJSONArray("columns", new JSONArray());
							for (int i = 0;i < columns.size(); i++) {
								JSONObject column = (JSONObject) columns.get(i);
								baseUrl = column.optString("thumbnailImageUrl", "");
								checkRobotImageFile(t, baseUrl, contextPath, richMessage.getMkey());
								baseUrl = "";
							}
							break;
						}
						baseUrl = "";
					}
				} catch (Exception e) {
					log.log(Level.ERROR, "ERROR in checkRobotImageFile : " + e.toString());
				}
				String s = js.serialize(richMessages);
				resp.getWriter().append(xssStr.getFakeCheck(s));
				resp.getWriter().flush();
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				RichMessage msg = null;
				try {
					int id = Integer.parseInt(idStr);
					msg = RichMessage.get(id);
				}
				catch (Exception ignoreIt) {}
				
				if (msg == null) {
					// maybe it's Mkey
					try {
						msg = RichMessage.getByMKey(t.getId(), idStr);
					}
					catch (Exception ex) {
					}
				}

				if (msg.getTenantId().intValue() == t.getId()) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);

					// 為了紀錄QuickReply設定的template mkey是什麼
					if (msg.getMsgType().equals("quikcReplies")) {
						JSONObject tmpl = new JSONObject(msg.getMsgTemplate());
						if (tmpl.getString("type").equals("template")) {
							req.setAttribute("template_mkey", tmpl.getString("template"));
						}
					}
					
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
        Integer opLogId = (Integer) req.getAttribute("opLogId");
        OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
        if (log == null) {
            resp.setStatus(500);
            return;
        }
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			// Permission check must be move to gateway someday.
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());

			String uri = xssStr.getCheck(req.getRequestURI());
			ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				String payload = WiSeUtils.getPayload(req);
				JSONObject obj = new JSONObject(payload);
				String msgType = obj.optString("msgType");
				JSONObject msgT = obj.optJSONObject("msgTemplate");
				if (msgT != null) {
					obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT, obj.optString("mkey")).toString());
				} else {
					obj.put("msgTemplate", "{}");
				}
				// System.out.println(obj.toString(2));

				RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);

				// 安全檢核
				if (!msg.getTenantId().equals(t.getId())) {
                    String result = "Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
                            + msg.getId() + " (t=" + msg.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]";
                    System.out.println(result);
                    log.setStatusMessage(OperationLogEntity.Status.FAILED);
                    log.appendToMoreDetails(result);
                    log.update();
					return;
				}

				RichMessage.saveOrUpdate(msg);
				resp.getWriter().append(xssStr.escapeHtml2(
						new JSONObject()
							.put("status", this.getClass().getName() + "::PUT")
							.put("id", msg.getId())
							.put("name", msg.getMsgName())
							.put("mkey", msg.getMkey())
						.toString())); // not support update whole collection
			    resp.getWriter().flush();
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMessage origMsg = RichMessage.get(id);
                log.setNamespace(log.getNamespace().replace("/" + id, ""));
                log.appendToMoreDetails("ID:" + id + ", MKEY:" + origMsg.getMkey());
				System.out.println("Update rich message :[" + origMsg.getId() + "]");

				String payload = WiSeUtils.getPayload(req);
				JSONObject obj = new JSONObject(payload);
				String mkey = obj.optString("mkey");
				RichMessage rm = RichMessage.getByMKey(t.getId(), mkey);
				if (rm == null || (rm != null && origMsg.getMkey().equals(mkey))) {
					String msgType = obj.optString("msgType");
					JSONObject msgT = obj.optJSONObject("msgTemplate");
					if (msgT != null) {
						if (rm == null) {
							RobotImageFile.updateMkeyInfo(origMsg.getMkey(), mkey, RichMessage.class.getName(),
									"img/webRM", t.getId());
						}
						obj.put("msgTemplate", pruneInvalidData(t, req.getContextPath(), msgType, msgT, obj.optString("mkey")).toString());
					} else {
						obj.put("msgTemplate", "{}");
					}
					// System.out.println(obj.toString(2));
					RichMessage msg = om.readValue(obj.toString(2), RichMessage.class);
	
					// 安全檢核
					if (!msg.getTenantId().equals(t.getId()) || !msg.getId().equals(origMsg.getId())) {
	                    String result = "Intrusion detected: user [" + user.getId() + "] trying update a RichMessage["
	                            + msg.getId() + " (t=" + msg.getTenantId() + ")] which is not matching current tenant id [" + t.getId() + "]";
	                    System.out.println(result);
	                    log.setStatusMessage(OperationLogEntity.Status.FAILED);
	                    log.appendToMoreDetails(result);
	                    log.update();
						return;
					}
	
					RichMessage.saveOrUpdate(msg);
					resp.getWriter().append(xssStr.escapeHtml2(
						new JSONObject()
							.put("status", this.getClass().getName() + "::PUT")
							.put("id", msg.getId())
							.put("name", msg.getMsgName())
							.put("mkey", msg.getMkey())
						.toString())); // not support update whole collection
				    resp.getWriter().flush();
		            log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					resp.getWriter().println("{\"result\":\"Mkey Exist\"}");
					resp.setStatus(500);
					//log.info("global.failed, Mkey Exist!");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
            log.setStatusMessage(OperationLogEntity.Status.FAILED);
            log.appendToMoreDetails(e.toString());
		}
		log.update();
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
        OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
        if (log == null) {
            resp.setStatus(500);
            return;
        }
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			String uri = xssStr.getCheck(req.getRequestURI());
			if (StringUtils.endsWith(uri, COLLECTION_URL_PATTERN)) {
				resp.getWriter().append("{}");	// not support delete whole
				resp.getWriter().flush();		// collection
                log.setStatusMessage(OperationLogEntity.Status.FAILED);
                log.appendToMoreDetails("not support delete whole");
			} else {
				String idStr = StringUtils.substringAfterLast(uri, "/");
				int id = Integer.parseInt(idStr);
				RichMessage msg = RichMessage.get(id);
                log.setNamespace(log.getNamespace().replace("/" + id, ""));
                log.appendToMoreDetails("ID:" + id + ", MKEY:" + msg.getMkey());
				if (msg != null && msg.getTenantId().equals(t.getId())) {
					JSONSerializer js = new JSONSerializer();
					String s = js.serialize(msg);
					RichMessage.delete(msg);
					
					RobotImageFile.listDeleteBy(t.getId(), RichMessage.class.getName(), msg.getMkey(), true);
					resp.getWriter().append(xssStr.getFakeCheck(s));
					resp.getWriter().flush();
                    log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					resp.getWriter().append("{}");
					resp.getWriter().flush();
                    String result = "Intrusion detected: tenant[" + t.getId() + "] trying to access data of tenant["
                            + msg.getTenantId() + "]";
                    System.out.println(result);
                    log.setStatusMessage(OperationLogEntity.Status.FAILED);
                    log.appendToMoreDetails(result);
				}
			}
			// resp.getWriter().println(new JSONObject().put("status",
			// this.getClass().getName() + "::DELETE").toString());
		} catch (Exception e) {
			e.printStackTrace();
            log.setStatusMessage(OperationLogEntity.Status.FAILED);
            log.appendToMoreDetails(e.toString());
		}
        log.update();
	}

	/**
	 * Traverse actions and append event source / event source type information to the postback.data
	 * @param rmj
	 * @param ctx
	 */
	public static void addEventSourceAndEncryptPostback(JSONObject rmj, String eventSource, String eventSourceType) {
		try {
			if (rmj.has("actions")) {
				addEventSourceAndEncryptPostback(rmj.getJSONArray("actions"), eventSource, eventSourceType);
			}
			if (rmj.has("template")) {
				JSONObject tpl = rmj.optJSONObject("template");
				addEventSourceAndEncryptPostback(tpl, eventSource, eventSourceType);
			}
			if (rmj.has("columns")) {
				JSONArray cols = rmj.optJSONArray("columns");
				// 新增的版型沒有塞在actions內
				if (rmj.optString("type") != null && !rmj.optString("type").equals("carousel")) {
					addEventSourceAndEncryptPostback(cols, eventSource, eventSourceType);					
				} else {
					int colCount = cols.length();
					for (int i = 0; i < colCount; i++) {
						JSONObject col = cols.getJSONObject(i);
						addEventSourceAndEncryptPostback(col, eventSource, eventSourceType);
					}
				}
			}
			if (rmj.has("quickReply")) {
				JSONArray items = rmj.getJSONObject("quickReply").getJSONArray("items");
				addEventSourceForQuickReplies(items, eventSource, eventSourceType);
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
	public static void addEventSourceAndEncryptPostback(JSONArray actions, String eventSource, String eventSourceType) {
		int actionCount = actions.length();
		for (int i = 0; i < actionCount; i++) {
			try {
				JSONObject a = actions.getJSONObject(i);
				
				// 這是 for quick reply 的結構是 items[i].["aciton"] （判斷依據是 items[i] 發現 type == "action"，就再 getJSONObject("action") 才做後面比對
				if ("action".equals(a.optString("type"))) {
					a = a.optJSONObject("action");
				}
				
				if (eventSource != null) a.put("es", eventSource);
				if (eventSourceType != null) a.put("est", eventSourceType);
				NaverLineActionType type = NaverLineActionType.valueOf(a.optString("type", ""));
				switch (type) {
					case postback:
						if (a.has(type.getContent())) {
							StringBuilder payload = new StringBuilder();
							String data = a.optString(type.getContent(), "");
							Map<String, List<String>> nvPair = WiSeUtils.splitQuery(data);
							if (nvPair == null) {
								payload.append("action=_message&message=").append(data);
							} else {
								payload.append(data);
							}
							String encrypted = WiSeUtils.aesEncrypt(Tenant.SIMPLE_AES_SECRET_KEY,
								Tenant.SIMPLE_AES_INIT_VECTOR, payload.toString(), true);
							a.put(type.getContent(), encrypted);
						}
						break;
					default:
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void addEventSourceForQuickReplies(JSONArray items, String eventSource, String eventSourceType, String... parameters) {
		if (items != null) {
			try {
				int replyCount = items.length();
				NaverLineActionType type = NaverLineActionType.postback;
				for (int i = 0; i < replyCount; i++) {
					JSONObject item = items.getJSONObject(i);
					JSONObject r = item.getJSONObject("action");
					if (eventSource != null) r.put("es", eventSource);
					if (eventSourceType != null) r.put("est", eventSourceType);
					if (r.has(type.getContent())) {
						StringBuilder payload = new StringBuilder();
						String data = r.optString(type.getContent(), "");
						Map<String, List<String>> nvPair = WiSeUtils.splitQuery(data);
						if (nvPair == null) { payload.append("action=_message&message=").append(data); } 
						else { payload.append(data); }
						
						if (parameters != null && parameters.length > 0) {
							payload.append("&").append(parameters[0]);
						}
						
						String encrypted = WiSeUtils.aesEncrypt(Tenant.SIMPLE_AES_SECRET_KEY,
							Tenant.SIMPLE_AES_INIT_VECTOR, payload.toString(), true);
						r.put(type.getContent(), encrypted);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String[] getBaseUrlSizeCollection() {
		String str = Arrays.toString(BASE_URL_SIZE_COLLECTION).replaceAll("\\s+", ""); // [1, 2, 3, 4, 5] => [1,2,3,4,5]
		String[] baseUrlSizeCollection = str.substring(1, str.length() - 1).split(",");
		return baseUrlSizeCollection;
	}

}
