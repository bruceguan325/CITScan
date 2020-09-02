package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intumit.checkmarx.CheckMarxUtils;
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.QAEvaluationLog;
import com.intumit.solr.robot.QAEvaluationLogQueue;
import com.intumit.solr.util.XssHttpServletRequestWrapper;

public class QAEvaluationLogServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
			QAEvaluationLog eva = QAEvaluationLog.get(Long.valueOf(xssReq.getParameter("evaLogId")));
			String type = xssReq.getParameter("evaLogType");

			response.setContentType("application/vnd.ms-excel");
			response.setCharacterEncoding("UTF-8");
			// 不設的話 fileDownload的dialog不會關掉
			response.setHeader("Set-Cookie", "fileDownload=true; path=/; HttpOnly");

			// Assume file name is retrieved from database
			// This should send the file to browser
			OutputStream out = response.getOutputStream();
			InputStream in = null;
			if (type.equals("report")) {
				in = eva.getReportFile().getBinaryStream();
				response.setHeader("Content-disposition", "attachment;filename=" + eva.getId() + "_"
						+ System.currentTimeMillis() + "_Evaluation_Report.xls");
			} else if (type.equals("detail") || type.equals("hidden") || type.equals("delete")) {
				in = eva.getDetailFile() != null ? eva.getDetailFile().getBinaryStream() : null;
				response.setHeader("Content-disposition", "attachment;filename=" + eva.getId() + "_"
						+ System.currentTimeMillis() + "_Evaluation_Detail.xlsx");
			}
			if (type.equals("hidden") || type.equals("delete")) {
				Boolean auth = ((AdminGroupFacade.getInstance().getFromSession(xssReq.getSession()).getSystemAdminCURD()
						& AdminGroup.O4) == 0
						&& !("admin".equals(
								AdminUserFacade.getInstance().getFromSession(xssReq.getSession()).getLoginName())
								&& 1 == AdminUserFacade.getInstance().getFromSession(xssReq.getSession()).getId()));
				if (!auth && type.equals("hidden")) {
					eva.setShowInList(false);
					eva.update();
				}
				if (!auth && type.equals("delete")) {
                    QAEvaluationLogQueue.cancelBuild(eva.getId());
					eva.delete();
				}
			}

			byte[] buffer = new byte[4096];
			int length;
			while (in != null && (length = in.read(buffer)) > 0) {
                out.write(CheckMarxUtils.escapeHtml(buffer), 0, length);
			}
			if (in != null)
				in.close();
			out.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}