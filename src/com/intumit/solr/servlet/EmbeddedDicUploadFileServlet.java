package com.intumit.solr.servlet;

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
import com.intumit.solr.robot.dictionary.EmbeddedFuzzyDictionaryDatabase;
import com.intumit.solr.tenant.Tenant;

public class EmbeddedDicUploadFileServlet extends HttpServlet {
	
	private static final long serialVersionUID = 8736246184294928639L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		res.setContentType("text/html; charset=UTF8");
		res.setCharacterEncoding("UTF-8");
		Locale locale = req.getLocale();
		PrintWriter out = res.getWriter();
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
				items.add((FileItem) req.getSession().getAttribute("EmbeddedDicImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("EmbeddedDicImportCsvTempFile", item);

					InputStream input = item.getInputStream();
					String fileName = item.getName();
					System.out.println(fileName + ": " + StringUtils.substringBeforeLast(fileName, "."));
					String type = StringUtils.upperCase(StringUtils.substringBeforeLast(fileName, "."));

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

							String[] headers = null;

							List<String> cells = null;
							try {
								while ((cells = listReader.read()) != null) {
									Long id = null;
									String keyword = null;
									if(cells.size() > 1) {
										id = Long.parseLong(cells.get(0));
										keyword = cells.get(1);
									}
									else {
										keyword = cells.get(0);
									}
									EmbeddedFuzzyDictionaryDatabase origDic = EmbeddedFuzzyDictionaryDatabase.get(t.getId(), keyword, type);
									if (StringUtils.trimToNull(keyword) != null) {
										if (origDic == null) {
											if (!dryRun) {
												EmbeddedFuzzyDictionaryDatabase.save(t.getId(), id, keyword, type);
											}
											String addDicWords = MessageUtil.getMessage(locale, "embedded.dictionary.add");
											out.println(addDicWords + "「" + keyword + "」 / " + type) ;
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun) {
												EmbeddedFuzzyDictionaryDatabase.update(t.getId(), origDic.getId(), keyword, type);
											}
											String updateDicWords = MessageUtil.getMessage(locale, "embedded.dictionary.update");
											out.println(updateDicWords + "「" + keyword + "」 / " + type);
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

		if (dryRun) {
			out.println("<h2>");
			String expectAddSynWords = MessageUtil.getMessage(locale, "expect.add.dic.words");
			String groupSynWords = MessageUtil.getMessage(locale, "group.dic.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			String backSynManger = MessageUtil.getMessage(locale, "back.dic.manger");
			out.println(expectAddSynWords + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/embeddedUploadFile?dryRun=false'>"+sureImport+"</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/embedded'>"+backSynManger+"</a>");
			out.println("</h2>");
		}
		else {
			String totalAdd = MessageUtil.getMessage(locale, "total.add");
			String groupSynWords = MessageUtil.getMessage(locale, "group.dic.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String backSynManger = MessageUtil.getMessage(locale, "back.dic.manger");
			out.println("<h2>");
			out.println(totalAdd + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/embedded'>"+backSynManger+"</a>");
			out.println("</h2>");
		}
	}


}
