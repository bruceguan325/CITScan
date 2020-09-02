package com.intumit.solr.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.admin.AdminLocation;
import com.intumit.solr.admin.AdminLocationFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.push.PushData.ContentType;
import com.intumit.systemconfig.WiseSystemConfig;
import com.runawaybits.html2markdown.DateUtil;

/**
 * 
 * 簡單的log檢視
 */
@WebServlet("/wiseadm/logViewer")
public class LogViewerServlet extends HttpServlet {

	private static final long serialVersionUID = 396332967564833025L;
	private static final Logger logger = Logger.getLogger(LogViewerServlet.class);
	private static String jettyPath = "";

	/**
	 * get log list
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		jettyPath = WiseSystemConfig.get().getvLog();
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		String remoteIp = req.getRemoteAddr();
		Set<AdminLocation> admLocs = AdminLocationFacade.getInstance().findAll(remoteIp,
				user == null ? null : user.getLoginName());
		if (user != null && user.isSuperAdmin() && !admLocs.isEmpty()) {
			try (PrintWriter out = resp.getWriter()) {
				JSONArray fileList = getFileNames();
				resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
				resp.setContentType(ContentType.JSON_ARRAY.getContentType());
				JSONObject jobj = new JSONObject();
				jobj.put("data", fileList);
				out.write(jobj.toString());
			} catch (Exception e) {
				logger.error(e, e);
			}

		} else {
			logger.warn("permisson denied");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		jettyPath = WiseSystemConfig.get().getvLog();
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		String remoteIp = req.getRemoteAddr();
		Set<AdminLocation> admLocs = AdminLocationFacade.getInstance().findAll(remoteIp,
				user == null ? null : user.getLoginName());
		if (user != null && user.isSuperAdmin() && !admLocs.isEmpty()) {
			String fileName = req.getParameter("fileName");
			String startStr = req.getParameter("start");
			String endStr = req.getParameter("end");
			StringBuilder sb = new StringBuilder();
			int start = 0;
			int end = 1000;
			if (StringUtils.isNotBlank(startStr) && StringUtils.isNotBlank(endStr)) {
				try {
					start = Integer.parseInt(startStr);
					end = Integer.parseInt(endStr);
				} catch (NumberFormatException e) {
					logger.info("parse fail");
				}
			}
			if (StringUtils.isNotBlank(fileName)) {
				File file = new File(String.format("%s/%s", jettyPath, fileName));
				try (ServletOutputStream out = resp.getOutputStream()) {
					List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
					int total = lines.size();
					if (start <= total && end <= total) {
						lines = lines.subList(start, end);
					} else if (end >= total) {
						lines = lines.subList(start, lines.size());
					}
					lines.stream().forEach(line -> {
						sb.append(line);
						sb.append("\n");
					});

					JSONObject jobj = new JSONObject();
					jobj.put("log", sb.toString());
					jobj.put("size", total);

					resp.setContentType(ContentType.JSON_OBJECT.getContentType());
					resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

					out.write(jobj.toString().getBytes(StandardCharsets.UTF_8.name()));
				} catch (Exception e) {
					logger.error(e, e);
				}
			}
		} else {
			logger.warn("permisson denied");
			try {
				req.getRequestDispatcher("login.jsp").forward(req, resp);
			} catch (ServletException | IOException e) {
				logger.error(e, e);
			}
		}
	}

	private JSONArray getFileNames() throws IOException {
		jettyPath = WiseSystemConfig.get().getvLog();
		JSONArray fileList = new JSONArray();
		if (StringUtils.isNotBlank(jettyPath)) {
			File folder = new File(jettyPath);			
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					JSONObject jobj = new JSONObject();
					File file = listOfFiles[i];
					String size = FileUtils.byteCountToDisplaySize(file.length());
					try {
						jobj.put("fileName", file.getName());
						jobj.put("lastModified", DateUtil.dateToString(new Date(file.lastModified())));
						jobj.put("size", size);
						fileList.add(jobj);
					} catch (JSONException e) {
						logger.error(e, e);
					}
				}
			}
		}
		return fileList;
	}
}
