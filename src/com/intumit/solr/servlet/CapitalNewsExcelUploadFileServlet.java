package com.intumit.solr.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.message.MessageUtil;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.CapitalExcelNewsParser;
import com.intumit.solr.util.WiSeEnv;

public class CapitalNewsExcelUploadFileServlet extends HttpServlet {

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
				items.add((FileItem) req.getSession().getAttribute("cneUploadFileTmp"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("cneUploadFileTmp", item);

					InputStream input = item.getInputStream();

					if (input.available() == 0) {
						System.out.println("File content expired, please upload it again.");
					}
					else {
						String outputFn = StringUtils.substringBeforeLast(item.getName(), ".") + "-modified." 
								+ StringUtils.substringAfterLast(item.getName(), ".");
						String outputPath = WiSeEnv.getHomePath() + "/../webapps/wise/ckeditor-upload/" + outputFn;
								
						System.out.println(outputPath);
						CapitalExcelNewsParser.stockSynonymsCsvPath = WiSeEnv.getHomePath() + "/dict/capital/StockSynonyms.csv";
						CapitalExcelNewsParser.parseAndSave(input, new FileOutputStream(outputPath) );
						
						out.println("<h2>");
						out.println("處理完畢，請下載此檔<a href='" + req.getContextPath() + "/ckeditor-upload/" + URLEncoder.encode(outputFn, "UTF-8") + "'>" + outputFn + "</a>");
						out.println("</h2>");
					}
				}
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			
			out.println("未知的錯誤，請聯繫客服.");
		}
	}

}
