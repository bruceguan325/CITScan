package com.intumit.solr.util.fileupload;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

@MultipartConfig
public class QAFileUploadServlet extends HttpServlet {
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("QAFileUploadServlet doPost");
		
		//RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/wiseadm/qaImport.jsp");
		//requestDispatcher.forward(request, response);
		List<FileMeta> files = new LinkedList<FileMeta>();
		files.addAll(MultipartRequestHandler.uploadByJavaServletAPI(request));
		
		request.getSession().setAttribute("qaFileUploaded", files);

		response.setContentType("application/json");

		// 4. Convert List<FileMeta> into JSON format
		ObjectMapper mapper = new ObjectMapper();

		// 5. Send resutl to client
		mapper.writeValue(response.getOutputStream(), files);
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("QAFileUploadServlet doGet");
		
		doPost(request, response);
	}
}
