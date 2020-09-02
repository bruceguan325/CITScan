package com.intumit.solr.robot.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.intumit.solr.qparser.ExtendedDismaxQParserPlugin;
import com.intumit.solr.tenant.Tenant;

public class FuzzyCoreUtils implements Serializable {
	
	private static final long serialVersionUID = 6361723537495559702L;
	
	private static final Map<Integer, FuzzyCoreUtils> tenantUtilMap = new HashMap<Integer, FuzzyCoreUtils>();
	
	private static final String FUZZY_DATATYPE = "EMBEDDED_FUZZY";
	
	private Tenant tenant;
	private SolrServer fuzzyCoreServer;
	private SolrServer fuzzyCoreServer4Write;
	
	public synchronized static FuzzyCoreUtils getInstance(Integer tenantId) {
		if(!tenantUtilMap.containsKey(tenantId) || tenantUtilMap.get(tenantId) == null) {
			Tenant t = Tenant.get(tenantId);
			tenantUtilMap.put(tenantId, new FuzzyCoreUtils(t));
		}
		return tenantUtilMap.get(tenantId);
	}
	
	private FuzzyCoreUtils(Tenant t) {
		this.tenant = t;
		this.fuzzyCoreServer = t.getCoreServer();
		this.fuzzyCoreServer4Write = t.getCoreServer4Write();
	}
	
	public synchronized void add(EmbeddedFuzzyDictionaryDatabase efd) throws Exception {
		try {
			SolrInputDocument efdDoc = new SolrInputDocument();
			efdDoc.addField("id", String.format("%s-%s", FUZZY_DATATYPE, efd.getId().toString()));
			efdDoc.addField("efdId_l", efd.getId());
			efdDoc.addField("dataType_s", FUZZY_DATATYPE);
			efdDoc.addField("KEYWORD_s", efd.getKeyword());
			efdDoc.addField("KEYWORD_t", efd.getKeyword());
			efdDoc.addField("TYPE_s", efd.getType());
			efdDoc.addField("createTime_dt", new Date(System.currentTimeMillis()));
			fuzzyCoreServer4Write.add(efdDoc);
			fuzzyCoreServer4Write.commit(true, true, false);
		}
		catch(Exception ignore) {
			ignore.printStackTrace();
			throw ignore;
		}
	}
	
	public synchronized void add(List<EmbeddedFuzzyDictionaryDatabase> efds) throws Exception {
		try {
			List<SolrInputDocument> efdDocs = new ArrayList<SolrInputDocument>();
			for(EmbeddedFuzzyDictionaryDatabase efd : efds) {
				SolrInputDocument efdDoc = new SolrInputDocument();
				efdDoc.addField("id", String.format("%s-%s", FUZZY_DATATYPE, efd.getId().toString()));
				efdDoc.addField("efdId_l", efd.getId());
				efdDoc.addField("dataType_s", FUZZY_DATATYPE);
				efdDoc.addField("KEYWORD_s", efd.getKeyword());
				efdDoc.addField("KEYWORD_t", efd.getKeyword());
				efdDoc.addField("TYPE_s", efd.getType());
				efdDoc.addField("createTime_dt", new Date(System.currentTimeMillis()));
				efdDocs.add(efdDoc);
			}
			fuzzyCoreServer4Write.add(efdDocs);
			fuzzyCoreServer4Write.commit(true, true, false);
		}
		catch(Exception ignore) {
			ignore.printStackTrace();
			throw ignore;
		}
	}
	
	public EmbeddedFuzzyDictionaryDatabase get(String keyword, String type) {
		SolrQuery sq = new SolrQuery();
		sq.setRows(1);
		sq.addFilterQuery("KEYWORD_s:\"" + keyword + "\"");
		sq.addFilterQuery("TYPE_s:" + type);
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false);
		try {
			QueryResponse resp = fuzzyCoreServer.query(sq);
			SolrDocumentList results = resp.getResults();
			if(!results.isEmpty()) {
				EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
				SolrDocument doc = results.get(0);
				efd.setId((Long)doc.getFieldValue("efdId_l"));
				efd.setKeyword((String)doc.getFieldValue("KEYWORD_s"));
				efd.setType((String)doc.getFieldValue("TYPE_s"));
				efd.setTenantId(tenant.getId());
				return efd;
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public EmbeddedFuzzyDictionaryDatabase get(Long id) {
		SolrQuery sq = new SolrQuery();
		sq.setRows(1);
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.addFilterQuery("efdId_l:" + id);
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false);
		try {
			QueryResponse resp = fuzzyCoreServer.query(sq);
			SolrDocumentList results = resp.getResults();
			if(!results.isEmpty()) {
				EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
				SolrDocument doc = results.get(0);
				efd.setId((Long)doc.getFieldValue("efdId_l"));
				efd.setKeyword((String)doc.getFieldValue("KEYWORD_s"));
				efd.setType((String)doc.getFieldValue("TYPE_s"));
				efd.setTenantId(tenant.getId());
				return efd;
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public synchronized void delete(String id) throws Exception {
		try {
			fuzzyCoreServer4Write.deleteById(StringUtils.startsWith(id, FUZZY_DATATYPE) ? id : FUZZY_DATATYPE + "-" + id);
			fuzzyCoreServer4Write.commit(true, true, false);
		}
		catch(Exception ignore) {
			throw ignore;
		}
	}
	
	public synchronized void update(EmbeddedFuzzyDictionaryDatabase efd, EmbeddedFuzzyDictionaryDatabase newEfd) throws Exception{
		try {
			delete(efd.getId().toString());
			add(newEfd);
		}
		catch(Exception ignore) {
			ignore.printStackTrace();
		}
	}
	
	public String autoComplete(String query, String embeddedType, AtomicBoolean autoCompleted) {
		String autoComplete = query;
		SolrQuery sq = new SolrQuery();
		sq.setRows(1);
		sq.setQuery(query);
		sq.addFilterQuery("TYPE_s:" + embeddedType);
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.setParam("qf", "KEYWORD_s^5 KEYWORD_t^0.1");
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false)
			.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
			.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true)
			.setParam("mm", "90%");
		try {
			QueryResponse resp = fuzzyCoreServer.query(sq);
			SolrDocumentList results = resp.getResults();
			if(!results.isEmpty()) {
				autoComplete = (String) results.get(0).getFieldValue("KEYWORD_s");
				System.out.println(String.format("[%s] AUTOCOMPLETE TO [%s]", query, autoComplete));
				autoCompleted.set(true);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return autoComplete;
	}
	
	private static final Integer ROW_SIZE = Integer.valueOf(1000);
	
	public List<EmbeddedFuzzyDictionaryDatabase> listAll() {
		List<EmbeddedFuzzyDictionaryDatabase> all = new ArrayList<EmbeddedFuzzyDictionaryDatabase>();
		int start = 0;
		long limit = Long.MAX_VALUE;
		SolrQuery sq = new SolrQuery();
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false);
		try {
			while(start < limit - 1) {
				sq.setStart(start);
				sq.setRows(ROW_SIZE);
				QueryResponse resp = fuzzyCoreServer.query(sq);
				limit = resp.getResults().getNumFound();
				SolrDocumentList results = resp.getResults();
				for(SolrDocument doc : results) {
					EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
					efd.setId((Long)doc.getFieldValue("efdId_l"));
					efd.setKeyword((String)doc.getFieldValue("KEYWORD_s"));
					efd.setType((String)doc.getFieldValue("TYPE_s"));
					efd.setTenantId(tenant.getId());
					all.add(efd);
					start++;
				}
			}
		}
		catch(Exception ignore) {
			ignore.printStackTrace();
		}
		return all;
	}
	
	public List<EmbeddedFuzzyDictionaryDatabase> search(String keyword, String type) {
		List<EmbeddedFuzzyDictionaryDatabase> searchResult = new ArrayList<EmbeddedFuzzyDictionaryDatabase>();
		SolrQuery sq = new SolrQuery();
		sq.setRows(Integer.MAX_VALUE);
		if(StringUtils.isNotBlank(type)) {
			sq.addFilterQuery("TYPE_s:" + type);
		}
		if(StringUtils.isNotBlank(keyword)) {
			sq.addFilterQuery("KEYWORD_t:*" + keyword + "*");
		}
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false);
		try {
			QueryResponse resp = fuzzyCoreServer.query(sq);
			SolrDocumentList results = resp.getResults();
			for(SolrDocument doc : results) {
				EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
				efd.setId((Long)doc.getFieldValue("efdId_l"));
				efd.setKeyword((String)doc.getFieldValue("KEYWORD_s"));
				efd.setType((String)doc.getFieldValue("TYPE_s"));
				efd.setTenantId(tenant.getId());
				searchResult.add(efd);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return searchResult;
	}
	
	public Long getMaxId() {
		SolrQuery sq = new SolrQuery();
		sq.setRows(1);
		sq.addFilterQuery("dataType_s:" + FUZZY_DATATYPE);
		sq.setRequestHandler("/browse")
			.setParam("enableElevation", true)
			.setParam("forceElevation", true)
			.setParam("fuzzy", false)
			.setSort("efdId_l", ORDER.desc);
		try {
			QueryResponse resp = fuzzyCoreServer.query(sq);
			SolrDocumentList results = resp.getResults();
			if(!results.isEmpty()) {
				EmbeddedFuzzyDictionaryDatabase efd = new EmbeddedFuzzyDictionaryDatabase();
				SolrDocument doc = results.get(0);
				efd.setId((Long)doc.getFieldValue("efdId_l"));
				efd.setKeyword((String)doc.getFieldValue("KEYWORD_s"));
				efd.setType((String)doc.getFieldValue("TYPE_s"));
				efd.setTenantId(tenant.getId());
				return (Long)doc.getFieldValue("efdId_l");
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return Long.valueOf(0);
	}
}
