package com.intumit.viewRecord;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.solr.SearchManager;

public class ViewStatisticsEntity {

	String idRecord;
	int viewTime;
	String name;
	String orgUrl;
	String target=null;
	

	public String getIdRecord() {
		return idRecord;
	}
	public void setIdRecord(String idRecord) {
		this.idRecord = idRecord;
		
		SolrServer server = SearchManager.getServer("core0");
		SolrQuery sqry = new SolrQuery();
		
		sqry.setQuery("id:"+idRecord);
		
		try {
			QueryResponse resp = server.query(sqry);
			SolrDocumentList docList = resp.getResults();
			
			if (docList == null) docList = new SolrDocumentList();
			
			for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); ) {
				SolrDocument doc = docItr.next();
				Collection values = doc.getFieldValues("Name_t");
				String valStr = (values != null) ? StringUtils.left(values.toString(), 30) : "";
				if(values != null && values.size() > 30) valStr += "...";
				setName(valStr);
				
				String icuitemId = (String)doc.getFieldValue("IcuItem_s");
				Integer showType;
				try {
						showType = new Integer((String)doc.getFieldValue("ShowType_s"));
				}
				catch(java.lang.NumberFormatException e) {
						showType = 0;
				}

				String rXdmpId = (String)doc.getFieldValue("XdmpId_s");
				String domainName = (String)doc.getFieldValue("DomainName_s");
				domainName = domainName.trim();
				String nodeId = (String)doc.getFieldValue("CtNodeId_s");
				if (nodeId == null) nodeId = "0";
				String xurl = (String)doc.getFieldValue("Xurl_s");
				if (xurl != null && !xurl.startsWith("http")) {
					xurl = "http://" + domainName + xurl;
				}
				String fileDownload = (String)doc.getFieldValue("FileDownload_s");
				
				if ("100001".equals(rXdmpId)) {
					domainName = "www.taipei.gov.tw";
				}

				switch (showType) {
					case 1:
					orgUrl = "http://" + domainName + "/ct.asp?xitem=" + icuitemId + "&CtNode=" + nodeId + "&mp=" + rXdmpId;
					break;
					case 2:
					orgUrl = xurl;
					target = "_new";
					break;
					case 3:
					orgUrl = "http://" + domainName + "/public/Data/" + fileDownload;
					target = "_new";
					break;
					default:
					orgUrl = "http://" + domainName + "/ct.asp?xitem=" + icuitemId + "&CtNode=" + nodeId + "&mp=" + rXdmpId;
					break;
				}
				return;
			}
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setName("");
		
	}
	public int getViewTime() {
		return viewTime;
	}
	public void setViewTime(int viewTime) {
		this.viewTime = viewTime;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOrgUrl() {
		return orgUrl;
	}
	public void setOrgUrl(String orgUrl) {
		this.orgUrl = orgUrl;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
}
