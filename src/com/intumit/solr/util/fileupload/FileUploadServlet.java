package com.intumit.solr.util.fileupload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.ProcessQADataServlet;

//this to be used with Java Servlet 3.0 API
@MultipartConfig
public class FileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/***************************************************
	 * URL: /upload doPost(): upload the files and other parameters
	 ****************************************************/
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// Get current session
		List<FileMeta> files = (List<FileMeta>) request.getSession().getAttribute("uploaded");
		String parameter = request.getParameter("tm");
		if (StringUtils.equalsIgnoreCase("true", request.getParameter("tm"))) {
			ByteArrayInputStream is = new ByteArrayInputStream(request.getParameter("textContent").getBytes());
			// 2. Get Post textarea as file content
			FileMeta temp = new FileMeta();
			String fileName = request.getParameter("fileName"); 
			temp.setFileName(fileName);
			temp.setQaFile(ProcessQADataServlet.isQaFile(fileName));
			temp.setContent(is);
			temp.setFileType("text/plain");
			temp.setFileSize(is.available()/1024+ "Kb");

			AdminUser user = AdminUserFacade.getInstance().getFromSession(request.getSession());
			temp.setTwitter(user.getName());

			if (files == null) {
				files = new ArrayList<FileMeta>();
				files.add(temp);
				request.getSession().setAttribute("uploaded", files);
			} else {
				files.add(temp);
			}
		}
		else {
			// 2. Get Upload File Using Apache FileUpload
			List<FileMeta> thisUpload = MultipartRequestHandler
					.uploadByJavaServletAPI(request);

			if (files == null) {
				files = thisUpload;
				request.getSession().setAttribute("uploaded", thisUpload);
			} else {
				files.addAll(thisUpload);
			}
		}

		// 3. Set response type to json
		response.setContentType("application/json");

		// 4. Convert List<FileMeta> into JSON format
		ObjectMapper mapper = new ObjectMapper();

		// 5. Send resutl to client
		mapper.writeValue(response.getOutputStream(), files);

	}

	/***************************************************
	 * URL: /upload?f=value doGet(): get file of index "f" from List<FileMeta>
	 * as an attachment
	 ****************************************************/
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//Locale locale = request.getLocale();
		Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
		String command = request.getParameter("c");
		List<FileMeta> files = (List<FileMeta>) request.getSession()
				.getAttribute("uploaded");

		if ("l".equalsIgnoreCase(command)) {
			response.setContentType("application/json");
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(response.getOutputStream(), files);
		} else if ("d".equalsIgnoreCase(command)) {
			// 1. Get f from URL upload?f="?"
			String value = request.getParameter("f");

			// 2. Get the file of index "f" from the list "files"
			FileMeta getFile = files.get(Integer.parseInt(value));

			if (getFile != null) {
				files.remove(getFile);
			}

			response.sendRedirect(request.getContextPath() + "/wiseadm/fileUpload.jsp");

		} else if ("i".equalsIgnoreCase(command)) {
			String value = request.getParameter("f");
			FileMeta getFile = files == null?null:files.get(Integer.parseInt(value));
			String dataType = getFile == null?null:StringUtils.substringBeforeLast(getFile.getFileName(), ".");
			if (StringUtils.startsWithIgnoreCase(dataType, ProcessQADataServlet.QA_DATA_FILE_NAME)
					|| StringUtils.startsWithIgnoreCase(dataType, ProcessQADataServlet.QA_DATA_MULTI_CHANNEL_ANSWER_FILE_NAME)) {
				RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/wiseadm/fileImport.jsp");
				requestDispatcher.forward(request, response);
			}
			/*else if (StringUtils.startsWithIgnoreCase(dataType, MessageUtil.getMessage(Locale.TAIWAN, "smart.robot.qa.hierarchical"))
					|| StringUtils.startsWithIgnoreCase(dataType, MessageUtil.getMessage(Locale.CHINA, "smart.robot.qa.hierarchical"))) {
				RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/wiseadm/fileImportHierarchical.jsp");
				requestDispatcher.forward(request, response);
			}*/
			else {
				response.sendRedirect(request.getContextPath() + "/wiseadm/processData?f=" + value);
			}
		} else {
			// 1. Get f from URL upload?f="?"
			String value = request.getParameter("f");

			// 2. Get the file of index "f" from the list "files"
			FileMeta getFile = files.get(Integer.parseInt(value));

			try {
				// 3. Set the response content type = file content type
				response.setContentType(getFile.getFileType());

				// 4. Set header Content-disposition
				response.setHeader("Content-disposition",
						"attachment; filename=\"" + getFile.getFileName()
								+ "\"");

				// 5. Copy file inputstream to response outputstream
				InputStream input = getFile.getContent();
				OutputStream output = response.getOutputStream();
				byte[] buffer = new byte[1024 * 10];

				for (int length = 0; (length = input.read(buffer)) > 0;) {
					output.write(buffer, 0, length);
				}

				output.close();
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
