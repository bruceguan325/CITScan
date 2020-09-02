package com.intumit.solr.robot.connector.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONObject;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.tenant.Tenant;

@WebServlet(urlPatterns = { "/wiseadm/webRM/qaRichMenuUploadFile" })
public class RichMenuUploadFileServlet extends HttpServlet {
	
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
		Locale locale1 =  (Locale)req.getSession().getAttribute("org.apache.struts.action.LOCALE");//判斷語系
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		Locale locale = HitHotLocale.determineLocale(req, false, false);
		int addCount = 0, updateCount = 0;
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));
		String action = req.getParameter("action");
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
				items.add((FileItem) req.getSession().getAttribute("QARichMenuImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println(new StringBuilder().append("name:").append(name).append(" value:").append(value)
							.toString());
				}
				else {
					req.getSession().setAttribute("QARichMenuImportCsvTempFile", item);

					InputStream input = item.getInputStream();

					if (input.available() == 0) {
						System.out.println("File content expired, please upload it again.");
					}
					else {
						ICsvListReader listReader = null;

						try {
							String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
							System.out.println(
									new StringBuilder().append("Detected charset:").append(charset).toString());
							input = item.getInputStream();

							listReader = new CsvListReader(new InputStreamReader(input, charset),
									CsvPreference.EXCEL_PREFERENCE);

							List<String> cells = null;
							try {
								while ((cells = listReader.read()) != null) {
									String mkey = cells.get(0);
									String channelCode = cells.get(1);
									String menuSeq = cells.get(2);
									String msgName = cells.get(3);
									String msgDesc = cells.get(4);
									String msgType = cells.get(5);
									String msgTemplate = cells.get(6);
									
									String img = cells.size() > 7 ? cells.get(7) : null;
									
									RichMenu richMenu = RichMenu.getByMKey(t.getId(), mkey);
									String fileName = null;
									if (StringUtils.trimToNull(msgTemplate) != null) {
										if (richMenu == null) {
											if (!dryRun && "import".equalsIgnoreCase(action)) {
												richMenu = new RichMenu();
												richMenu.setMsgTemplate(msgTemplate);
												fileName = processBase64ToImgFile(richMenu, img, t);
											}
											if (fileName != null) {
												richMenu = RichMenu.save(t.getId(), mkey, msgName, msgDesc, msgType, richMenu.getMsgTemplate(), channelCode, menuSeq);
											}
											String addRichMenu = MessageUtil.getMessage(locale1, "global.add");
											out.println(new StringBuilder().append(addRichMenu).append("「").append(mkey)
													.append("：").append(msgName).append("(").append(msgDesc)
													.append(")」：").append(msgType).toString());
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun && "import".equalsIgnoreCase(action)) {
												richMenu.setMkey(mkey);
												richMenu.setMsgName(msgName);
												richMenu.setMsgDesc(msgDesc);
												richMenu.setMsgType(msgType);
												richMenu.setMsgTemplate(msgTemplate);
												richMenu.setChannelCode(channelCode);
												richMenu.setMenuSeq(menuSeq != null && !menuSeq.isEmpty() ? Integer.valueOf(menuSeq) : 1);
												fileName = processBase64ToImgFile(richMenu, img, t);
												if (fileName != null)
													RichMenu.saveOrUpdate(richMenu);
											}
											String updateRichMenu = MessageUtil.getMessage(locale1, "global.update");
											out.println(new StringBuilder().append(updateRichMenu).append("「")
													.append(mkey).append("：").append(msgName).append("(")
													.append(msgDesc).append(")」：").append(msgType).toString());
											out.println("<BR>");
											updateCount++;
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

		String dataName = MessageUtil.getMessage(locale, "qa.richmenu.navbar.web");
		String backTo = MessageUtil.getMessage(locale, "qa.richmenu.navbar.web");
		
		String update = MessageUtil.getMessage(locale1, "global.update");
		String expectAddTemplate = MessageUtil.getMessage(locale1, "expect.add");
		String totalAdd = MessageUtil.getMessage(locale1, "total.add");
		
		if (dryRun && "import".equalsIgnoreCase(action)) {
			out.println("<h2>");
			String sureImport = MessageUtil.getMessage(locale1, "sure.import");
			out.println(String.format("%s%d%s，%s%d%s<br>", expectAddTemplate, addCount, dataName, update, updateCount, dataName));
			out.println(new StringBuilder().append("<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMenuUploadFile?action=").append("import")
					.append("&dryRun=false'>").append(sureImport).append("</a>").toString());
			out.println(new StringBuilder().append("&nbsp;<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMenuList.jsp'>").append(backTo).append("</a>").toString());
			out.println("</h2>");
		}
		else {
			out.println("<h2>");
			out.println(String.format("%s%d%s，%s%d%s<br>", totalAdd, addCount, dataName, update, updateCount, dataName));
			out.println(new StringBuilder().append("<a href='").append(req.getContextPath())
					.append("/wiseadm/webRM/qaRichMenuList.jsp'>").append(backTo).append("</a>").toString());
			out.println("</h2>");
		}
	
	}
	
	private String processBase64ToImgFile(RichMenu richMenu, String img, Tenant t) {
		try {
			String baseUrl = null;
			String fileName = null;
			JSONObject jo = new JSONObject(richMenu.getMsgTemplate());

			baseUrl = jo.optString("baseUrl", "");
			if (baseUrl.isEmpty())
				return "";
			fileName = StringUtils.substringAfterLast(baseUrl, "/");
			fileName = StringUtils.substringBeforeLast(fileName, ".");
			RichMenuServlet.processBaseUrlAndFixRatio(t.getId(), img == null ? jo : jo.put("baseUrl", img),
					richMenu.getMkey(), fileName);
			richMenu.setMsgTemplate(jo.toString());
			return fileName;
		} catch (Exception e) {
			System.out.println("Error in processBase64ToImgFile : " + e);
		}
		return null;
	}
}
