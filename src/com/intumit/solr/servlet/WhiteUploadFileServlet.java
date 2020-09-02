package com.intumit.solr.servlet;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

public class WhiteUploadFileServlet extends HttpServlet
{
	private String base;

	public void init(ServletConfig sc) throws ServletException {
		ServletContext sco = sc.getServletContext();
		base = sco.getRealPath("/") + "csv_file/";
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		try {
			if (isMultipart) {
				DiskFileItemFactory factory = new DiskFileItemFactory();
				factory.setSizeThreshold(4096);
				factory.setRepository(new File(base));
				ServletFileUpload upload = new ServletFileUpload(factory);
				upload.setSizeMax(10000000);
				List items = upload.parseRequest(req);
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					if (item.isFormField()) {
						String name = item.getFieldName();
						String value = item.getString();
						System.out.println("name:" + name + " value:" + value);
					} else {
						String fieldName = item.getFieldName();
						String fileName = item.getName();
						String contentType = item.getContentType();
						boolean isInMemory = item.isInMemory();
						long sizeInBytes = item.getSize();
						System.out.println("fieldName:" + fieldName
								+ " fileName:" + fileName);
						File to = new File(base, fileName);
						item.write(to);
						System.out.println(getTransPath());
						List<String> lines = FileUtils.readLines(new File(
								getTransPath() + "/"+fileName));

						for (String eachLine : lines) {

							String[] eachToken = eachLine.split(",");

							if (eachToken != null) {
								String keyword = eachToken[2];
									if (!keyword.equals("")) {
										WhiteWikiWordFacade.getInstance().save(keyword, "#userDefined");
									}
								
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		res.sendRedirect("wiseadm/white");
	}

	private String getTransPath() {

		SAXBuilder sb = new SAXBuilder();
		String urlStr="";
		try {
			
			int str_length = String.valueOf(
					SynUploadFileServlet.class.getResource("")).length();
			String str = String.valueOf(WhiteUploadFileServlet.class.getResource(""))
					.substring(5, str_length);
			File file = new File(str + "Path.xml");
			Document doc = sb.build(file);
			Element root = doc.getRootElement();
			urlStr = root.getChild("path1").getTextTrim();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return urlStr;
	}

}
