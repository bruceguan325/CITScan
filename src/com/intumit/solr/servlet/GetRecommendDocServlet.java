package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

import com.intumit.solr.SearchManager;
import com.intumit.solr.dataset.DataSet;
import com.intumit.solr.dataset.DataSetFacade;
import com.intumit.solr.recommendDocuments.RecommendDocument;
import com.thoughtworks.xstream.XStream;

/**
 * Originally wrote for TTV project, modified to fit standard WiSe.
 * 
 * Three parameters
 * id : the Doc ID for MLT
 * dsId : DataSet ID
 * num : number of mlt docs (default use mlt count in dataset setting)
 * 
 * @author Herb and Unknown.
 */
public class GetRecommendDocServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {

		String id = "";
		String mainCore = "";
		// int total = 10;

		Integer dsId = null;
		Long num = null;

		try {

			if (req.getParameter("id") != null)
				id = req.getParameter("id");

			if (req.getParameter("d") != null)
				dsId = new Integer(req.getParameter("d"));

			if (req.getParameter("rows") != null)
				num = new Long(req.getParameter("rows"));

			// if (req.getParameter("total") != null)
			// total = Integer.parseInt(req.getParameter("total"));

		} catch (Exception e) {
			log(e.getMessage(), e);
		}

		// SolrDocumentList docs = null;
		List<RecommendDocument> docs = new ArrayList<RecommendDocument>();
		
		try {
			DataSet ds = DataSetFacade.getInstance().get(dsId);
			docs = getMoreLikeThis(id, ds, num == null ? ds.getFieldMltCount()
					: num);
			String body = listRecommendDocsXml(docs);
			resp.setContentType("application/xml;charset=UTF-8");
			// JSONSerializer serializer = new JSONSerializer();
			resp.getWriter().write(body);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private XStream out() {
		XStream out = new XStream();
		out.alias("doc", RecommendDocument.class);
		// out.alias("doc", SolrDocument.class);
		// out.alias("list", SolrDocumentList.class);
		return out;
	}

	public synchronized String listRecommendDocsXml(List<RecommendDocument> docs) {
		return out().toXML(docs);
	}

	private List<RecommendDocument> getMoreLikeThis(String id, DataSet ds,
			long num) throws Exception {
		SolrServer server = SearchManager.getServer(ds.getCoreName());
		SolrQuery query = new SolrQuery();

		// String sort = "Date_dt";
		// SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;

		query.setQuery("id:" + id);
		query.setParam("mlt", true);
		query.setParam("mlt.mindf", "1");
		query.setParam("mlt.mintf", "1");
		query.setParam("mlt.count", String.valueOf(num));
		query.setParam("mlt.fl", ds.getFieldMlt());

		QueryResponse mainRsp = null;
		mainRsp = server.query(query);

		NamedList<Object> nList = mainRsp.getResponse();
		NamedList<Object> resList = null;

		// Look for known things
		for (int i = 0; i < nList.size(); i++) {
			String n = nList.getName(i);
			if ("moreLikeThis".equals(n)) {
				resList = (NamedList<Object>) nList.getVal(i);
			}
		}

		List<RecommendDocument> results = new ArrayList<RecommendDocument>();
		for (int i = 0; i < resList.size(); i++) {

			String n = resList.getName(i);

			if (resList.getVal(i) instanceof SolrDocumentList) {
				SolrDocumentList mltDocList = (SolrDocumentList) resList
						.getVal(i);

				for (Iterator<SolrDocument> itr = mltDocList.iterator(); itr
						.hasNext();) {

					SolrDocument doc = itr.next();

					RecommendDocument recommendDocument = new RecommendDocument();
					recommendDocument.setCore(String.valueOf(ds.getCoreName()));

					recommendDocument.setId((String) doc.getFieldValue("id"));
					recommendDocument.setTitle((String) doc
							.getFieldValue("Name_t"));
					recommendDocument.setDescription((String) doc
							.getFieldValue("Description_mt"));
					recommendDocument.setDate((Date) doc
							.getFieldValue("Date_dt"));
					results.add(recommendDocument);
				}
			}
		}
		Collections.sort(results, sort);
		return results;
	}

	static final Comparator<RecommendDocument> sort = new Comparator<RecommendDocument>() {
		public int compare(RecommendDocument o1, RecommendDocument o2) {
			if (o1.getDate() == o2.getDate())
				return 0;
			else if (o1.getDate().after(o2.getDate()))
				return -1;
			else
				return 1;
		}
	};
}
