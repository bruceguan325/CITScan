package com.intumit.smartwiki.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.smartwiki.config.WikiRankIndex;
import com.intumit.smartwiki.config.WikiRankIndexFacade;
import com.intumit.smartwiki.recommend.WikiWord;

public class WikiWordList extends ArrayList<WikiWord> {

	public void fill() {
		if (this.size() == 0)
			return;

		String[] idsStr = new String[this.size()];
		int count = 0;
		for (WikiWord word : this) {
			idsStr[count] = Integer.toString(word.getKeywordId());
			count++;
		}
		
		WikiRankIndex index = WikiRankIndexFacade.getInstance().get("zh_TW");
		SolrServer server = index.getServer();
		SolrQuery query = new SolrQuery();
		query.setRows(100);
		query.setStart(0);
		
		String queryStr = "";
		for (String id: idsStr) {
			queryStr += (queryStr.length() != 0 ? " AND " : "") + "id:" + idsStr;   
		}
		query.setQuery(queryStr);
		
		QueryResponse response;
		try {
			response = server.query(query);
			SolrDocumentList docList = response.getResults();
			
			for (SolrDocument doc: docList) {
				Integer id = new Integer((String)doc.getFieldValue("id"));
				Integer iSum = (Integer)doc.getFieldValue("Sum_i");
	
				if (iSum != null)
					this.setDF(id, iSum);
				
				Integer iRedir = (Integer)doc.getFieldValue("pRedirect_i");
				if (iRedir != null)
					this.setRedirectId(id, iRedir);
				
				Integer iDisID = (Integer)doc.getFieldValue("pDisID_i");
				if (iDisID != null)
					this.setDisambId(id, iDisID);
				
				String sSyn = (String)doc.getFieldValue("pSyn_s");
				if (sSyn != null) {
					this.setSynset(id, sSyn);
				}
				else {
					String sTemp = (String)doc.getFieldValue("pTemp_s");
					if (sTemp != null)
						this.setSynset(id, sTemp);
				}
					
	
				Integer iLink = (Integer)doc.getFieldValue("Link_i");
				if (iLink != null)
					this.setLinks(id, iLink);
				
				Integer iPri = (Integer)doc.getFieldValue("pPri_i");
				if (iPri != null)
					this.setPriority(id, iPri);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		sumFrequency();
		sumPageDF();
	}

	public void fillByLang() {
		if (this.size() == 0)
			return;

		String[] idsStr = new String[this.size()];
		int count = 0;
		for (WikiWord word : this) {
			idsStr[count] = Integer.toString(word.getKeywordId());
			count++;
		}
		
		WikiRankIndex index = WikiRankIndexFacade.getInstance().get("zh_TW");
		SolrServer server = index.getServer();
		SolrQuery query = new SolrQuery();
		query.setRows(100);
		query.setStart(0);
		
		String queryStr = "";
		for (String id: idsStr) {
			queryStr += (queryStr.length() != 0 ? " AND " : "") + "id:" + idsStr;   
		}
		query.setQuery(queryStr);
		
		QueryResponse response;
		try {
			response = server.query(query);
			SolrDocumentList docList = response.getResults();
			
			for (SolrDocument doc: docList) {
				Integer id = new Integer((String)doc.getFieldValue("id"));
				Integer iPage = (Integer)doc.getFieldValue("Pages_i");
	
				if (iPage != null)
					this.setDF(id, iPage);
				
				Integer iRedir = (Integer)doc.getFieldValue("pRedirect_i");
				if (iRedir != null)
					this.setRedirectId(id, iRedir);
				
				Integer iDisID = (Integer)doc.getFieldValue("pDisID_i");
				if (iDisID != null)
					this.setDisambId(id, iDisID);
				
				String sSyn = (String)doc.getFieldValue("pSyn_s");
				if (sSyn != null) {
					this.setSynset(id, sSyn);
				}
				else {
					String sTemp = (String)doc.getFieldValue("pTemp_s");
					if (sTemp != null)
						this.setSynset(id, sTemp);
				}
					
	
				Integer iLink = (Integer)doc.getFieldValue("LinkFrom_i");
				if (iLink != null)
					this.setLinks(id, iLink);
				
				Integer iPri = (Integer)doc.getFieldValue("pPri_i");
				if (iPri != null)
					this.setPriority(id, iPri);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		sumPageDF();
	}

	private void setDisambId(int keywordId, int disambId) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId)
				word.setAmbigId(disambId);
		}
	}

	public void setDF(int keywordId, int df) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId)
				word.setPageDF(df);
		}
	}

	public void setLinks(int keywordId, int links) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId)
				word.setLinks(links);
		}
	}

	public void setRedirectId(int keywordId, int redirectId) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId) {
				if (redirectId == 0)
					word.setRedirectId(keywordId);
				else
					word.setRedirectId(redirectId);
			}
		}
	}

	private void setSynset(int keywordId, String synset) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId)
				word.setSynset(synset);
		}
	}

	/**
	 * 把相同 redirectId 的 keyword 頻率加總
	 */
	private void sumFrequency() {
		List<Integer> frequencys = new ArrayList(this.size());
		for (WikiWord word1 : this) {
			int totalFrequency = word1.getFrequency();
			for (WikiWord word2 : this) {
				if (word1 != word2
						&& word1.getRedirectId() == word2.getRedirectId())
					totalFrequency += word2.getFrequency();
			}

			frequencys.add(totalFrequency);

		}

		for (int i = 0; i < this.size(); i++) {
			this.get(i).setFrequency(frequencys.get(i));
		}
	}

	/**
	 * 把相同 redirectId 的 keyword PageDF 值加總
	 * 
	 */
	private void sumPageDF() {
		List<Integer> pageDFs = new ArrayList(this.size());
		for (WikiWord word1 : this) {
			int totalPageDF = word1.getPageDF();
			// for (WikiWord word2 : this) {
			// if (word1 != word2
			// && word1.getRedirectId() == word2.getRedirectId())
			// totalPageDF += word2.getPageDF();
			// }

			// if (word1 != word2)
			// totalPageDF += word2.getPageDF();
			// }

			pageDFs.add(totalPageDF);
		}
		for (int i = 0; i < this.size(); i++) {
			this.get(i).setPageDF(pageDFs.get(i));
		}
	}

	public static WikiWordList toWikiWordList(List<WikiWord> list) {
		WikiWordList result = new WikiWordList();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			WikiWord element = (WikiWord) iter.next();
			result.add(element);
		}
		return result;
	}

	/*
	 * 取得同義字
	 * TODO fix error, or delete this method
	public void getCorrectSynon() {
		for (int i = 0; i < this.size(); i++) {
			WikiWord word = this.get(i);
			if (word.getAmbigId() != 0) {
				WikiWordList synons = word.getSynons();
				synons.fill();
				for (int j = 0; j < synons.size(); j++) {
					WikiWord synon = synons.get(j);
					Set<Integer> childSet = null;
					WikiWord element;
					Set<Integer> children;

					childSet = new HashSet<Integer>();
					children = synon.getAllChildSet();

					// 找出關聯到最多其他關鍵字的 多義詞 
					for (Integer child : children) {
						for (Iterator iter = this.iterator(); iter.hasNext();) {
							element = (WikiWord) iter.next();
							if (child.intValue() == element.getKeywordId()) {
								childSet.add(child);
								break;
							}
						}
					}
					synon.setRelationChildSet(childSet);
				}
				WikiWord correctOne = null;
				for (int j = 0; j < synons.size(); j++) {
					if (j == 0)
						correctOne = synons.get(j);
					else {
						if (correctOne.getRelationChildSet().size() < synons
								.get(j).getRelationChildSet().size())
							correctOne = synons.get(j);
					}
				}

				if (correctOne != null)
					this.set(i, correctOne);
			}
		}
	}
	 */

	/*
	private void fillChildSet(Connection conn, String idsStr) throws Exception {
		Statement stmt = null;
		ResultSet result = null;

		String query = "SELECT l.pl_from, l.pl_to FROM pagelinks l WHERE l.pl_to is not null and l.pl_from in "
				+ idsStr;

		List<String> childList = new ArrayList<String>();
		if (conn != null && !conn.isClosed()) {
			stmt = conn.createStatement();
			result = stmt.executeQuery(query);
			String pl_from;

			while (result.next()) {
				int pl_to = result.getInt(2);
				pl_from = result.getString(1);
				childList.add(pl_from + "," + pl_to);
			}
		}

		String child;
		Integer from, to;
		for (WikiWord word : this) {
			Set<Integer> allChildSet = new HashSet<Integer>();
			for (int j = 0; j < childList.size(); j++) {
				child = childList.get(j);
				from = Integer.parseInt(child.substring(0, child.indexOf(",")));
				to = Integer.valueOf(child.substring(child.indexOf(",") + 1));
				if (word.getKeywordId() == from) {
					allChildSet.add(to);
				}
			}
			word.setAllChildSet(allChildSet);
			// word.setAllChildSet(((WikiWord)this.get(word)).getAllChildSet());
		}

	}
	*/

	public void setPriority(int keywordId, int priority) {
		for (WikiWord word : this) {
			if (word.getKeywordId() == keywordId)
				word.setPriority(priority);
		}
	}

}
