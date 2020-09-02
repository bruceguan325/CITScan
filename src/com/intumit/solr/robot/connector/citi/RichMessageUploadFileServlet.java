package com.intumit.solr.robot.connector.citi;

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
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.citi.CitiDeep;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.connector.citi.RichMessage;
import com.intumit.solr.tenant.Tenant;

@WebServlet(urlPatterns = { "/wiseadm/citi/qaRichMessageUploadFile" })
public class RichMessageUploadFileServlet extends HttpServlet {
	
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
				items.add((FileItem) req.getSession().getAttribute("QARichMessageImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("QARichMessageImportCsvTempFile", item);

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
								    int priority = (StringUtils.isNumeric(cells.get(0))?Integer.parseInt(cells.get(0)):0);
									String logo = cells.get(1).replace("\n", ";");
									String title = cells.get(2);
									String reward = cells.get(3);
									String imageUrl = cells.get(4);
									String offerName1 = cells.get(5);
									String offerText1 = cells.get(6);
									String offerName2 = cells.get(7);
									String offerText2 = cells.get(8);
									String offerName3 = cells.get(9);
									String offerText3 = cells.get(10);
									String knowMore = cells.get(11);
									String applyNow = cells.get(12);
									int id = (StringUtils.isNumeric(cells.get(13))?Integer.parseInt(cells.get(13)):0);

                                    CitiDeep richMessage = RichMessage.get(id);

									if (StringUtils.trimToNull(logo) != null) {
										if (richMessage == null) {
											if (!dryRun) {
												RichMessage.save(logo, title, imageUrl, offerName1, offerText1, offerName2, 
												      offerText2, offerName3, offerText3, applyNow, knowMore, reward, priority);
											}
											String addRichMessage = MessageUtil.getMessage(locale, "global.add");
											out.println(addRichMessage + "「" + title + "：" + reward + "(" + logo + ")」：" + priority);
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun) {
											    RichMessage.save(id, logo, title, imageUrl, offerName1, offerText1, offerName2, 
	                                                      offerText2, offerName3, offerText3, applyNow, knowMore, reward, priority);
											}
											String updateRichMessage = MessageUtil.getMessage(locale, "global.update");
											out.println(updateRichMessage +"「" + title + "：" + reward + "(" + logo + ")」：" + priority);
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

		String dataName = MessageUtil.getMessage(locale, "qa.citi.card.right");
		String backTo = MessageUtil.getMessage(locale, "qa.citi.card.right");
		
		String update = MessageUtil.getMessage(locale, "global.update");
		String expectAddTemplate = MessageUtil.getMessage(locale, "expect.add");
		String totalAdd = MessageUtil.getMessage(locale, "total.add");
		
		if (dryRun) {
			out.println("<h2>");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			out.println(String.format("%s%d%s，%s%d%s<br>", expectAddTemplate, addCount, dataName, update, updateCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/citi/qaRichMessageUploadFile?dryRun=false'>"+sureImport+"</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/citi/qaRichMessageList.jsp'>"+backTo+"</a>");
			out.println("</h2>");
		}
		else {
			out.println("<h2>");
			out.println(String.format("%s%d%s，%s%d%s<br>", totalAdd, addCount, dataName, update, updateCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/citi/qaRichMessageList.jsp'>"+backTo+"</a>");
			out.println("</h2>");
		}
	
	}
}
