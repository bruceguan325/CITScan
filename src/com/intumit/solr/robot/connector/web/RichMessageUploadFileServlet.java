package com.intumit.solr.robot.connector.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Hibernate;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.RobotImageFile;
import com.intumit.solr.robot.RobotImageFilePath;
import com.intumit.solr.tenant.Tenant;

@WebServlet(urlPatterns = { "/wiseadm/webRM/qaRichMessageUploadFile" })
public class RichMessageUploadFileServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html; charset=UTF8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		Locale locale = HitHotLocale.determineLocale(req, false, false);
		int addCount = 0, updateCount = 0;
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));
		String action = req.getParameter("action");
		String type = req.getParameter("type");
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(4096);
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setSizeMax(10000000);
			List<FileItem> items = new ArrayList<FileItem>();

			if (dryRun) {
				if (isMultipart) {
					items.addAll(upload.parseRequest(req));
				}
			}
			else {
				items.add((FileItem) req.getSession().getAttribute("QARichMessageImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println(new StringBuilder().append("name:").append(name).append(" value:").append(value)
							.toString());
				}
				else {
					req.getSession().setAttribute("QARichMessageImportCsvTempFile", item);

					InputStream input = item.getInputStream();

					if (input.available() == 0) {
						System.out.println("File content expired, please upload it again.");
					}
					else {
						ICsvListReader listReader = null;

						try {
							String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
							String contextPath = req.getContextPath();
							System.out.println(
									new StringBuilder().append("Detected charset:").append(charset).toString());
							input = item.getInputStream();

							listReader = new CsvListReader(new InputStreamReader(input, charset),
									CsvPreference.EXCEL_PREFERENCE);

							List<String> cells = null;
							try {
								while ((cells = listReader.read()) != null) {
									String mkey = cells.get(0);
									String msgName = cells.get(1);
									String msgDesc = cells.get(2);
									String msgType = cells.get(3);
									
									if (!"default".equals(type)) {
										if (!type.equals(msgType))
											continue;
										// 理應重構
										String msgTemplate = ""; 
										switch (type) {
										case "buttons":
											String altText = cells.get(4);
											String text = cells.get(5);
											String[] btns = new String[10];
											btns[0] = cells.size() > 6 ? cells.get(6) : null;
											btns[1] = cells.size() > 7 ? cells.get(7) : null;
											btns[2] = cells.size() > 8 ? cells.get(8) : null;
											btns[3] = cells.size() > 9 ? cells.get(9) : null;
											btns[4] = cells.size() > 10 ? cells.get(10) : null;
											btns[5] = cells.size() > 11 ? cells.get(11) : null;
											btns[6] = cells.size() > 12 ? cells.get(12) : null;
											btns[7] = cells.size() > 13 ? cells.get(13) : null;
											btns[8] = cells.size() > 14 ? cells.get(14) : null;
											btns[9] = cells.size() > 15 ? cells.get(15) : null;
											msgTemplate = generateButtonsJson(altText, text, btns);
											break;
										case "quickReplies":
											altText = cells.get(4);
											text = cells.get(5);
											btns = new String[15];
											btns[0] = cells.size() > 6 ? cells.get(6) : null;
											btns[1] = cells.size() > 7 ? cells.get(7) : null;
											btns[2] = cells.size() > 8 ? cells.get(8) : null;
											btns[3] = cells.size() > 9 ? cells.get(9) : null;
											btns[4] = cells.size() > 10 ? cells.get(10) : null;
											btns[5] = cells.size() > 11 ? cells.get(11) : null;
											btns[6] = cells.size() > 12 ? cells.get(12) : null;
											btns[7] = cells.size() > 13 ? cells.get(13) : null;
											btns[8] = cells.size() > 14 ? cells.get(14) : null;
											btns[9] = cells.size() > 15 ? cells.get(15) : null;
											btns[10] = cells.size() > 16 ? cells.get(16) : null;
											btns[11] = cells.size() > 17 ? cells.get(17) : null;
											btns[12] = cells.size() > 18 ? cells.get(18) : null;
											btns[13] = cells.size() > 19 ? cells.get(19) : null;
											btns[14] = cells.size() > 20 ? cells.get(20) : null;
											msgTemplate = generateQuickRepliesJson(altText, text, btns);
											break;
										case "text":
											altText = cells.get(4);
											text = cells.get(5);
											msgTemplate = generateTextJson(altText, text);
											break;
										case "carousel":
											altText = cells.get(4);
											String fixedTitle = cells.get(5);
											String[][] cols = new String[15][3];
											String[][] colsOfBtns = new String[15][5];
											
											int indexOfCell;
											for (int i = 0; i < cols.length; i++) {
												for (int j = 0; j < cols[i].length; j++) {
													indexOfCell = i * 8 + j + 6;
													cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
												
												for (int j = 0; j < colsOfBtns[i].length; j++) {
													indexOfCell = i * 8 + j + 9;
													colsOfBtns[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
											}
											
											msgTemplate = generateCarouselJson(altText, fixedTitle, cols, colsOfBtns);
											break;
										case "stretch":
											altText = cells.get(4);
											text = cells.get(5);
											String stretchTitle = cells.get(6);
											String stretchText = cells.get(7);
											btns = new String[10];
											btns[0] = cells.size() > 8 ? cells.get(8) : null;
											btns[1] = cells.size() > 9 ? cells.get(9) : null;
											btns[2] = cells.size() > 10 ? cells.get(10) : null;
											btns[3] = cells.size() > 11 ? cells.get(11) : null;
											btns[4] = cells.size() > 12 ? cells.get(12) : null;
											btns[5] = cells.size() > 13 ? cells.get(13) : null;
											btns[6] = cells.size() > 14 ? cells.get(14) : null;
											btns[7] = cells.size() > 15 ? cells.get(15) : null;
											btns[8] = cells.size() > 16 ? cells.get(16) : null;
											btns[9] = cells.size() > 17 ? cells.get(17) : null;
											msgTemplate = generateStretchJson(altText, text, stretchTitle, stretchText, btns);
											break;
										case "threegrid":
											altText = cells.get(4);
											fixedTitle = cells.get(5);
											cols = new String[15][9];
											btns = new String[15];
											
											for (int i = 0; i < cols.length; i++) {
												for (int j = 0; j < cols[i].length; j++) {
													if (j == 8) {
														indexOfCell = i * 10 + j + 7;
														cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
													} else {
														indexOfCell = i * 10 + j + 6;
														cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
													}
												}
												indexOfCell = i * 10 + 14;
												btns[i] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
											}
											
											msgTemplate = generateThreeGridJson(altText, fixedTitle, cols, btns);
											break;
										case "twogrid":
											altText = cells.get(4);
											fixedTitle = cells.get(5);
											cols = new String[15][4];
											
											for (int i = 0; i < cols.length; i++) {
												for (int j = 0; j < cols[i].length; j++) {
													indexOfCell = i * 4 + j + 6;
													cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
											}
											
											msgTemplate = generateTwoGridJson(altText, fixedTitle, cols);
											break;
										case "onegrid":
										    altText = cells.get(4);
                                            fixedTitle = cells.get(5);
                                            // 15張圖卡 每張圖卡6個欄(5 text,1 btn Action)
                                            cols = new String[15][6];
                                            btns = new String[15];
                                            for(int i = 0 ; i < cols.length ; i++) {
                                                for(int j = 0 ; j < cols[i].length ; j++) {
                                                    // 6-11 12-17 ...
                                                    indexOfCell = (i+1) * 6 + j;
                                                    if(indexOfCell == (i+1) * 6 + 4) { // 10 16 ..
                                                        btns[i] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
                                                    }else {
                                                        cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
                                                    }
                                                }
                                            }
                                            msgTemplate = generateOneGridJson(altText, fixedTitle, cols, btns);
                                            break;
										case "textwithoutbutton":
											altText = cells.get(4);
											fixedTitle = cells.get(5);
											cols = new String[15][3];
											
											for (int i = 0; i < cols.length; i++) {
												for (int j = 0; j < cols[i].length; j++) {
													indexOfCell = i * 3 + j + 6;
													cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
											}
											
											msgTemplate = generateTextWithoutButtonJson(altText, fixedTitle, cols);
											break;
										case "textwithbutton":
											altText = cells.get(4);
											fixedTitle = cells.get(5);
											cols = new String[15][3];
											colsOfBtns = new String[15][5];
											
											for (int i = 0; i < cols.length; i++) {
												for (int j = 0; j < cols[i].length; j++) {
													indexOfCell = i * 8 + j + 6;
													cols[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
												
												for (int j = 0; j < colsOfBtns[i].length; j++) {
													indexOfCell = i * 8 + j + 9;
													colsOfBtns[i][j] = cells.size() > indexOfCell ? cells.get(indexOfCell) : null;
												}
											}
											
											msgTemplate = generateTextWithButtonJson(altText, fixedTitle, cols, colsOfBtns);
											break;
										}
										
										RichMessage richMessage = RichMessage.getByMKey(t.getId(), mkey);

										if (StringUtils.trimToNull(msgTemplate) != null) {
											if (richMessage == null) {
												if (!dryRun && "import".equalsIgnoreCase(action)) {
													richMessage = RichMessage.save(t.getId(), mkey, msgName, msgDesc, msgType, msgTemplate);
												}
												String addRichMessage = MessageUtil.getMessage(locale, "global.add");
												out.append(new StringBuilder().append(addRichMessage).append("「")
														.append(mkey).append("：").append(msgName).append("(")
														.append(msgDesc).append(")」：").append(msgType).toString());
												out.append("<BR>");
												out.flush();
												addCount++;
											}
											else {
												if (!dryRun && "import".equalsIgnoreCase(action)) {
													richMessage.setMkey(mkey);
													richMessage.setMsgName(msgName);
													richMessage.setMsgDesc(msgDesc);
													richMessage.setMsgType(msgType);
													richMessage.setMsgTemplate(msgTemplate);
													RichMessage.saveOrUpdate(richMessage);
												}
												String updateRichMessage = MessageUtil.getMessage(locale, "global.update");
												out.append(new StringBuilder().append(updateRichMessage).append("「")
														.append(mkey).append("：").append(msgName).append("(")
														.append(msgDesc).append(")」：").append(msgType).toString());
												out.append("<BR>");
												out.flush();
												updateCount++;
											}
										}
									} else {
										String msgTemplate = cells.get(4);
										
										String[] imgs = new String[10];
										imgs[0] = cells.size() > 5 ? cells.get(5) : null;
										imgs[1] = cells.size() > 6 ? cells.get(6) : null;
										imgs[2] = cells.size() > 7 ? cells.get(7) : null;
										imgs[3] = cells.size() > 8 ? cells.get(8) : null;
										imgs[4] = cells.size() > 9 ? cells.get(9) : null;
										imgs[5] = cells.size() > 10 ? cells.get(10) : null;
										imgs[6] = cells.size() > 11 ? cells.get(11) : null;
										imgs[7] = cells.size() > 12 ? cells.get(12) : null;
										imgs[8] = cells.size() > 13 ? cells.get(13) : null;
										imgs[9] = cells.size() > 14 ? cells.get(14) : null;
										
										RichMessage richMessage = RichMessage.getByMKey(t.getId(), mkey);

										if (StringUtils.trimToNull(msgTemplate) != null) {
											if (richMessage == null) {
												if (!dryRun && "import".equalsIgnoreCase(action)) {
													richMessage = RichMessage.save(t.getId(), mkey, msgName, msgDesc, msgType, msgTemplate);
												}
												if (richMessage != null) {
													processBase64ToImgFile(contextPath, richMessage, imgs, t);
												}
												String addRichMessage = MessageUtil.getMessage(locale, "global.add");
												out.append(new StringBuilder().append(addRichMessage).append("「")
														.append(mkey).append("：").append(msgName).append("(")
														.append(msgDesc).append(")」：").append(msgType).toString());
												out.append("<BR>");
												out.flush();
												addCount++;
											}
											else {
												if (!dryRun && "import".equalsIgnoreCase(action)) {
													richMessage.setMkey(mkey);
													richMessage.setMsgName(msgName);
													richMessage.setMsgDesc(msgDesc);
													richMessage.setMsgType(msgType);
													richMessage.setMsgTemplate(msgTemplate);
													RichMessage.saveOrUpdate(richMessage);
													
													processBase64ToImgFile(contextPath, richMessage, imgs, t);
												}
												String updateRichMessage = MessageUtil.getMessage(locale, "global.update");
												out.append(new StringBuilder().append(updateRichMessage).append("「")
														.append(mkey).append("：").append(msgName).append("(")
														.append(msgDesc).append(")」：").append(msgType).toString());
												out.append("<BR>");
												out.flush();
												updateCount++;
											}
										}
									}
								}
							}
							catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						finally {
							if (listReader != null) {
								listReader.close();
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		String dataName = MessageUtil.getMessage(locale, "qa.richmessage.navbar.web");
		String backTo = new StringBuilder().append(MessageUtil.getMessage(locale, "back.to")).append(dataName)
				.append(MessageUtil.getMessage(locale, "global.manage")).toString();

		String update = MessageUtil.getMessage(locale, "global.update");
		String expectAdd = MessageUtil.getMessage(locale, "expect.add");
		String totalAdd = MessageUtil.getMessage(locale, "total.add");
		
		if (dryRun && "import".equalsIgnoreCase(action)) {
			out.append("<h2>");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			out.append(String.format("%s%d%s，%s%d%s<br>", expectAdd, addCount, dataName, update, updateCount, dataName));
			out.append(new StringBuilder().append("<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMessageUploadFile?action=import&type=" + type)
					.append("&dryRun=false'>").append(sureImport).append("</a>").toString());
			out.append(new StringBuilder().append("&nbsp;<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMessageList.jsp'>").append(backTo).append("</a>").toString());
			out.append("</h2>");
			out.flush();
		}
		else {
			out.append("<h2>");
			out.append(String.format("%s%d%s，%s%d%s<br>", totalAdd, addCount, dataName, update, updateCount, dataName));
			out.append(new StringBuilder().append("<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMessageList.jsp'>").append(backTo).append("</a>").toString());
			out.append("</h2>");
			out.flush();
		}
	
	}
	
	@SuppressWarnings("deprecation")
	private void processBase64ToImgFile(String contextPath, RichMessage richMessage, String[] imgs, Tenant t) {
		try {
			JSONObject jo = new JSONObject(richMessage.getMsgTemplate());
			// key:fileName, value:oldUrl, newUrl
			LinkedHashMap<String, String[]> imageUrlReturns = new LinkedHashMap<String, String[]>();
			boolean processImgUrl = false;
			switch (richMessage.getMsgType()) {
			case "imagemap":
				String baseUrl = jo.optString("baseUrl", "");
				String fileName = StringUtils.substringAfterLast(baseUrl, "/");
				if (fileName.isEmpty())
					break;
				RichMessageServlet.processBaseUrlAndFixRatio(t.getId(), contextPath,
						imgs[0] == null ? jo : jo.put("baseUrl", imgs[0]), richMessage.getMkey(), fileName);
				
				String returnUrl = jo.optString("baseUrl", "");
				imageUrlReturns.put(fileName, new String[] { baseUrl, returnUrl });
				break;
			case "carousel":
				JSONObject tpl = jo.optJSONObject("template", new JSONObject());
				JSONArray columns = tpl.optJSONArray("columns", new JSONArray());
				for (int i = 0; i < columns.size(); i++) {
					JSONObject column = (JSONObject) columns.get(i);
					baseUrl = column.optString("thumbnailImageUrl", "");
					fileName = StringUtils.substringAfterLast(baseUrl, "/");
					imageUrlReturns.put(fileName, new String[] { baseUrl, null });
				}
				processImgUrl = true;
				break;
			}
			if (processImgUrl) {
				int i = 0;
				for (String fileName : imageUrlReturns.keySet()) {
					if (fileName.isEmpty())
						continue;
					String[] oldNewUrl = imageUrlReturns.get(fileName);
					String returnUrl = RichMessageServlet.processImageAndReturnUrl(t, contextPath,
							imgs[i] == null ? oldNewUrl[0] : imgs[i], fileName);
					oldNewUrl[1] = returnUrl;
					Path path = Paths
							.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
									.append(RichMessage.imgPath).append(File.separator).append(fileName).toString());
					if (imgs[i] != null && Files.exists(path)) {
						RobotImageFile imgFile = RobotImageFile.getBy(t.getId(), fileName, RichMessage.class.getName(),
								"img/webRM", richMessage.getMkey());
						InputStream imageInFile = Files.newInputStream(path);
						Blob blob = Hibernate.createBlob(imageInFile);
						if (imgFile == null) {
							new RobotImageFile(t.getId(), "img/webRM", RichMessage.class.getName(), fileName,
									richMessage.getMkey(), blob);
						} else {
							imgFile.setFileBody(blob);
							imgFile.setMarkForClean(null);
							RobotImageFile.save(imgFile);
						}
					}
					i++;
				}

				List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(t.getId(), RichMessage.class.getName(),
						richMessage.getMkey(), false);
				String[] baseUrlSizeCollection = RichMessageServlet.getBaseUrlSizeCollection();
				boolean deleteFolder = false;
				String folder = "";
				for (RobotImageFile imgFile : imgFiles) {
					if (StringUtils.equalsAny(imgFile.getMkey(), baseUrlSizeCollection) && !deleteFolder) {
						deleteFolder = true;
						folder = imgFile.getNamespace();
					}
					if (!imageUrlReturns.keySet().contains(imgFile.getMkey())) {
						imgFile.delete();
					}
				}
				if (deleteFolder) {
					RobotImageFile.deleteFolderPath(folder);
				}
			}
			boolean updateRm = false;
			for (String keyUrl : imageUrlReturns.keySet()) {
				if (keyUrl.isEmpty()) continue;
				String[] oldNewUrl = imageUrlReturns.get(keyUrl);
				if (!oldNewUrl[0].equals(oldNewUrl[1])) {
					richMessage.setMsgTemplate(
							StringUtils.replace(richMessage.getMsgTemplate(), oldNewUrl[0], oldNewUrl[1]));
					String oldStr = oldNewUrl[0].replace("/", "\\/");
					String newStr = oldNewUrl[1].replace("/", "\\/");
					richMessage.setMsgTemplate(StringUtils.replace(richMessage.getMsgTemplate(), oldStr, newStr));
					updateRm = true;
				}
			}
			if (updateRm)
				RichMessage.saveOrUpdate(richMessage);
		} catch (Exception e) {
			System.out.println("Error in processBase64ToImgFile : " + e);
		}
	}
	
	private JSONObject generateMsgTemplate(String altText, JSONObject template) {
		try {
			JSONObject message = new JSONObject();
			message.put("type", "template");
			message.put("altText", altText);
			message.put("template", template);
			return message;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	
	private JSONArray generateActionsJson(String... btns) {
		try {
			JSONArray actions = new JSONArray();
			for (String btn : btns) {
				JSONObject action;
				btn = StringUtils.substringBetween(btn, "{{", "}}");
				if (StringUtils.startsWith(btn, "L")) {
					String label = StringUtils.substringBetween(btn, ":");
					String uri = StringUtils.substringAfter(btn, label + ":");
					action = new JSONObject();
					action.put("label", label);
					action.put("uri", uri);
					action.put("type", "uri");
					actions.add(action);
				} else if (StringUtils.startsWith(btn, "Q")) {
					String label = StringUtils.substringBetween(btn, ":");
					String data = StringUtils.substringAfter(btn, label + ":");
					action = new JSONObject();
					action.put("label", label);
					action.put("text", data);
					action.put("type", "message");
					actions.add(action);
				} else if (StringUtils.startsWith(btn, "T")) {
					String label = StringUtils.substringBetween(btn, ":");
					String phone = StringUtils.substringAfter(btn, label + ":");
					action = new JSONObject();
					action.put("label", label);
					action.put("phone", phone);
					action.put("type", "call");
					actions.add(action);
				}
			}
			return actions;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String generateButtonsJson(String altText, String text, String[] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("text", text);
			template.put("type", "buttons");
			template.put("actions", generateActionsJson(btns));
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateQuickRepliesJson(String altText, String text, String[] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("text", text);
			template.put("type", "quickReplies");
			template.put("actions", generateActionsJson(btns));
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateTextJson(String altText, String text) {
		try {
			JSONObject message = new JSONObject();
			message.put("altText", altText);
			message.put("text", text);
			message.put("type", "text");
			
			return message.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	
	private String generateCarouselJson(String altText, String fixedTitle, String[][] cols, String[][] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("fixedTitle", fixedTitle);
			template.put("type", "carousel");
			
			JSONArray colums = new JSONArray();
			for (int i = 0; i < cols.length; i++) {
				String[] col = cols[i];
				JSONObject column;
				
				if (StringUtils.isNotBlank(col[1])) {
					column = new JSONObject();
					column.put("thumbnailImageUrl", col[0]);
					column.put("title", col[1]);
					column.put("text", col[2]);
					column.put("actions", generateActionsJson(btns[i]));
					colums.add(column);
				}
			}
			template.put("columns", colums);
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateStretchJson(String altText, String text, String stretchTitle, String stretchText, String[] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("text", text);
			template.put("type", "stretch");
			template.put("stretchTitle", stretchTitle);
			template.put("stretchText", stretchText);
			template.put("actions", generateActionsJson(btns));
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateThreeGridJson(String altText, String fixedTitle, String[][] cols, String[] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("fixedTitle", fixedTitle);
			template.put("type", "threegrid");
			
			JSONArray colums = new JSONArray();
			for (int i = 0; i < cols.length; i++) {
				String[] col = cols[i];
				JSONObject column;
				
				if (StringUtils.isNotBlank(col[1])) {
					column = new JSONObject();
					column.put("thumbnailImageUrl", col[0]);
					column.put("title", col[1]);
					column.put("text1", col[2]);
					column.put("text2", col[3]);
					column.put("text3", col[4]);
					column.put("text4", col[5]);
					column.put("text5", col[6]);
					column.put("text6", col[7]);
					column.put("text7", col[8]);
					column.put("actions", generateActionsJson(btns[i]));
					colums.add(column);
				}
			}
			template.put("columns", colums);
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateOneGridJson(String altText, String fixedTitle, String[][] cols, String[] btns) {
        try {
            JSONObject template = new JSONObject();
            template.put("fixedTitle", fixedTitle);
            template.put("type", "onegrid");
            
            JSONArray colums = new JSONArray();
            for (int i = 0; i < cols.length; i++) {
                String[] col = cols[i];
                JSONObject column;
                
                if (StringUtils.isNotBlank(col[1])) {
                    column = new JSONObject();
                    column.put("thumbnailImageUrl", col[0]);
                    column.put("picTitle", col[1]);
                    column.put("title", col[2]);
                    column.put("text1", col[3]);
                    column.put("text2", col[5]);
                    column.put("actions", generateActionsJson(btns[i]));
                    colums.add(column);
                }
            }
            template.put("columns", colums);
            
            return generateMsgTemplate(altText, template).toString();
        }catch(Exception e) {
            e.printStackTrace();
        }
        return "{}";
	}
	
	private String generateTwoGridJson(String altText, String fixedTitle, String[][] cols) {
		try {
			JSONObject template = new JSONObject();
			template.put("fixedTitle", fixedTitle);
			template.put("type", "twogrid");
			
			JSONArray colums = new JSONArray();
			for (int i = 0; i < cols.length; i++) {
				String[] col = cols[i];
				JSONObject column;
				
				if (StringUtils.isNotBlank(col[1])) {
					column = new JSONObject();
					column.put("thumbnailImageUrl", col[0]);
					column.put("title", col[1]);
					column.put("text1", col[2]);
					column.put("text2", col[3]);
					colums.add(column);
				}
			}
			template.put("columns", colums);
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateTextWithoutButtonJson(String altText, String fixedTitle, String[][] cols) {
		try {
			JSONObject template = new JSONObject();
			template.put("fixedTitle", fixedTitle);
			template.put("type", "textwithoutbutton");
			
			JSONArray colums = new JSONArray();
			for (int i = 0; i < cols.length; i++) {
				String[] col = cols[i];
				JSONObject column;
				
				if (StringUtils.isNotBlank(col[1])) {
					column = new JSONObject();
					column.put("thumbnailImageUrl", col[0]);
					column.put("title", col[1]);
					column.put("text", col[2]);
					colums.add(column);
				}
			}
			template.put("columns", colums);
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String generateTextWithButtonJson(String altText, String fixedTitle, String[][] cols, String[][] btns) {
		try {
			JSONObject template = new JSONObject();
			template.put("fixedTitle", fixedTitle);
			template.put("type", "textwithbutton");
			
			JSONArray colums = new JSONArray();
			for (int i = 0; i < cols.length; i++) {
				String[] col = cols[i];
				JSONObject column;
				
				if (StringUtils.isNotBlank(col[1])) {
					column = new JSONObject();
					column.put("thumbnailImageUrl", col[0]);
					column.put("title", col[1]);
					column.put("text", col[2]);
					column.put("actions", generateActionsJson(btns[i]));
					colums.add(column);
				}
			}
			template.put("columns", colums);
			
			return generateMsgTemplate(altText, template).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
}
