package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.solr.SearchManager;
import com.intumit.solr.recommendKeywords.RecommendKeyword;
import com.intumit.solr.searchKeywords.SearchKeywordLog;
import com.thoughtworks.xstream.XStream;

import flexjson.JSONSerializer;

public class GetSuggestKeywordServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 */
	private static List<String> querySuggest(String keyword) {
		List<String> suggests = new ArrayList<String>();
		try {
			SolrQuery query = new SolrQuery(keyword);
			query.setStart(0).setRows(10)
					.setParam("qf", "Name_t^1 Name_p^0.01")
					.setRequestHandler("/browse").setFacet(true);
			query.addFacetField("Name_s");
			SolrServer mServer = SearchManager.getServer("core-keyword");
			QueryResponse rsp = mServer.query(query);

			SolrDocumentList docList = rsp.getResults();

			if (docList.getNumFound() > 0) {
				FacetField ff = rsp.getFacetField("Name_s");
				for (FacetField.Count count : ff.getValues()) {
					if (count.getCount() > 10) {
						String kw = count.getName();

						if (!kw.equalsIgnoreCase(keyword))
							suggests.add(kw);
					}

					if (suggests.size() > 2)
						break;
				}
			} else {
				query = new SolrQuery();
				query.setStart(0).setRows(0)
						.setQuery(keyword)
						.setRequestHandler("/select")
						.setParam("mlt", true)
						.setParam("mlt.mintf", "1").setParam("mlt.mindf", "1")
						.setParam("mlt.fl", "Name_t").setFacet(true);
				query.addFacetField("Name_s");

				mServer = SearchManager.getServer("core-keyword");
				rsp = mServer.query(query);

				FacetField ff = rsp.getFacetField("Name_s");
				for (FacetField.Count count : ff.getValues()) {
					if (count.getCount() > 400) {
						String kw = count.getName();

						if (!kw.equalsIgnoreCase(keyword)) {
							if (StringUtils.getLevenshteinDistance(kw, keyword) <= (keyword
									.length() / 2))
								suggests.add(kw);
						}
					}

					if (suggests.size() > 2)
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return suggests;
	}

	private static List<String> queryFromCoreSuggest(String keyword) {
		List<String> suggests = new ArrayList<String>();

		SolrQuery query = new SolrQuery();
		String fl = "id";
		String sort = "length asc";

		// 用 BODY_s 搜尋
		query.setQuery("BODY_s:" + keyword).setParam("fl", fl)
				.setParam("sort", sort);

		QueryResponse rsp = null;
		SolrDocumentList docs = null;
		SolrServer mServer = SearchManager.getServer("core-suggest");
		try {
			rsp = mServer.query(query);

			if (rsp.getResults().getNumFound() == 0) {
				// 用 unigram_u 來搜尋（default 欄位）
				if (keyword.endsWith("*")) {
					query.setQuery(keyword.substring(0, keyword.length() - 1));
				} else
					query.setQuery(keyword);

				rsp = mServer.query(query);
			}
			docs = rsp.getResults();

			for (SolrDocument doc : docs) {
				String k = (String) doc.getFieldValue("Name_t");
				if (!suggests.contains(k)) {
					suggests.add(k);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return suggests;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {

		String wt = StringUtils.defaultString(req.getParameter("format"), "json");
		String q = req.getParameter("q");

		try {
			List<String> suggests = this.querySuggest(q);
			if ("xml".equalsIgnoreCase(wt)) {
				resp.setContentType("application/xml; charset=UTF-8");
				//JSONSerializer serializer = new JSONSerializer();
				resp.getWriter().write(out().toXML(suggests));
			}
			else if ("json".equalsIgnoreCase(wt)) {
				resp.setContentType("application/json;charset=UTF-8");
				JSONSerializer serializer = new JSONSerializer();
				resp.getWriter().write(serializer.serialize(suggests));
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private XStream out() {
		XStream out = new XStream();
		return out;
	}
}
