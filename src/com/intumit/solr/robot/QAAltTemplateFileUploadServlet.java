package com.intumit.solr.robot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.message.MessageUtil;
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.tenant.Tenant;

public class QAAltTemplateFileUploadServlet extends HttpServlet {
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html; charset=UTF8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		if ((AdminGroupFacade.getInstance().getFromSession(req.getSession()).getSystemAdminCURD() & AdminGroup.O4) == 0) {
			out.println("Permission Denied");
			return ; // 沒有批次匯入權限
		}
		
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		Locale locale = req.getLocale();
		int addCount = 0, updateCount = 0;
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));
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
				items.add((FileItem) req.getSession().getAttribute("QAAleTemplateImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("QAAleTemplateImportCsvTempFile", item);

					InputStream input = item.getInputStream();

					if (input.available() == 0) {
						System.out.println("File content expired, please upload it again.");
					}
					else {
						final CellProcessor[] allProcessors = null;
						ICsvListReader listReader = null;

						try {
							String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
							System.out.println("Detected charset:" + charset);
							input = item.getInputStream();

							listReader = new CsvListReader(new InputStreamReader(input, charset),
									CsvPreference.EXCEL_PREFERENCE);

							List<String> cells = null;
							try {
								while ((cells = listReader.read()) != null) {
									String mkey = cells.get(0);
									String name = cells.get(1);
									String description = cells.get(2);
									String template = cells.get(3);
									String suggestPatterns = cells.get(4);
									Integer priority = Integer.parseInt(cells.get(5));
									
									QAAltTemplate altTemplate = QAAltTemplate.getByKey(t.getId(), mkey);

									if (StringUtils.trimToNull(template) != null) {
										if (altTemplate == null) {
											if (!dryRun) {QAAltTemplate.save(t.getId(), mkey, name, description, template, suggestPatterns, priority);
											}
											String addTemplate = MessageUtil.getMessage(locale, "global.add");
											out.println(addTemplate + "「" + mkey + "：" + name + "(" + template + ")」：" + suggestPatterns);
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun) {
												altTemplate.setMkey(mkey);
												altTemplate.setName(name);
												altTemplate.setDescription(description);
												altTemplate.setTemplate(template);
												altTemplate.setSuggestPatterns(suggestPatterns);
												altTemplate.setPriority(priority);
												QAAltTemplate.saveOrUpdate(altTemplate);
											}
											String updateTemplate = MessageUtil.getMessage(locale, "global.update");
											out.println(updateTemplate +"「" + mkey + "：" + name + "(" + template + ")」：" + suggestPatterns);
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

		String dataName = MessageUtil.getMessage(locale, "top.question.method");
		String backTo = MessageUtil.getMessage(locale, "alt.template.management");
		
		String update = MessageUtil.getMessage(locale, "global.update");
		String expectAddTemplate = MessageUtil.getMessage(locale, "expect.add");
		String totalAdd = MessageUtil.getMessage(locale, "total.add");
		
		if (dryRun) {
			out.println("<h2>");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			out.println(String.format("%s%d%s，%s%d%s<br>", expectAddTemplate, addCount, dataName, update, updateCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/qaAltTemplateFileUpload?dryRun=false'>"+sureImport+"</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/qaAltTemplateList.jsp'>"+backTo+"</a>");
			out.println("</h2>");
		}
		else {
			out.println("<h2>");
			out.println(String.format("%s%d%s，%s%d%s<br>", totalAdd, addCount, dataName, update, updateCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/qaAltTemplateList.jsp'>"+backTo+"</a>");
			out.println("</h2>");
		}
	}
}
