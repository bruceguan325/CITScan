package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.recommendKeywords.RecommendKeyword;
import com.intumit.solr.searchKeywords.SearchKeywordLog;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.thoughtworks.xstream.XStream;

import flexjson.JSONSerializer;

public class GetHotKeywordServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		
		String wt = StringUtils.defaultString(req.getParameter("format"), "json");

		//欲顯示的熱門關鍵字數量
		Integer rows = 10;
		//幾天內的熱門關鍵字
		Integer day = 15;
		//指定關鍵字頻道
		String d = null;

		try {
			if (req.getParameter("rows") != null)
				rows = Integer.valueOf((String)req.getParameter("rows"));

			if (req.getParameter("day") != null)
				day = Integer.valueOf((String)req.getParameter("day"));

			if (req.getParameter("d") != null)
				d = req.getParameter("d");


		} catch (Exception e) {
			log(e.getMessage(), e);
		}

		List<RecommendKeyword> list = SearchKeywordLogFacade.getInstance().getHotKeywords(d, rows, day);

		try {
			if ("xml".equalsIgnoreCase(wt)) {
				resp.setContentType("application/xml; charset=UTF-8");
				//JSONSerializer serializer = new JSONSerializer();
				resp.getWriter().write(out().toXML(list));
			}
			else if ("json".equalsIgnoreCase(wt)) {
				resp.setContentType("application/json; charset=UTF-8");
				JSONSerializer serializer = new JSONSerializer();
				serializer.exclude("class", "id", "saveTime", "sort");
				resp.getWriter().write(serializer.serialize(list));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private XStream out() {
		XStream out = new XStream();
		out.alias("keyword", SearchKeywordLog.class);
		out.alias("keyword", RecommendKeyword.class);
		return out;
	}
}
