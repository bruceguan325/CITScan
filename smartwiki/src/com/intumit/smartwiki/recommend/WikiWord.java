package com.intumit.smartwiki.recommend;

import java.util.Set;

import com.intumit.hithot.HitHotLocale;

public class WikiWord {

	private String pageTitle; // page title

	private int firstIndex; // 文章中出現位置

	private int frequency; // 文章中出現的次數

	private double relationWordScore; // 關聯詞數

	private double devoteScore; // 貢獻分數

	private double longTermScore; // 長詞分數

	private double totalScore; // 總分

	private Set<Integer> allChildSet; // 所有關聯詞

	private Set<Integer> relationChildSet; // 出現在文章中的關聯詞

	private int pageDF; // TF

	private int links; // 在 Wikipeida 出現的連結數

	private double tfidfValue; // TF-IDF 總分

	private String recommand; // 建議

	private int priority;

	private int keywordId, redirectId;

	private int ambigId; // 同義字列表頁的 Page id

	private String synset;
	
	
	
	private double initScore;	// 從索引中取出的分數
	private Boolean isBasicLatin = null; // 這個 WikiWord 是拉丁語系還是 CJK？

	public double getInitScore() {
		return initScore;
	}

	public void setInitScore(double initScore) {
		this.initScore = initScore;
	}

	public WikiWord() {
	}

	public WikiWord(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public Set<Integer> getAllChildSet() {
		return allChildSet;
	}

	public void setAllChildSet(Set<Integer> allChildSet) {
		this.allChildSet = allChildSet;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getFirstIndex() {
		return this.firstIndex;
	}

	public void setFirstIndex(int index) {
		this.firstIndex = index;
	}

	public int getPageDF() {
		return pageDF;
	}

	public void setPageDF(int pageDF) {
		this.pageDF = pageDF;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getRecommand() {
		return recommand;
	}

	public void setRecommand(String recommand) {
		this.recommand = recommand;
	}

	public Set<Integer> getRelationChildSet() {
		return relationChildSet;
	}

	public void setRelationChildSet(Set<Integer> relationChildSet) {
		this.relationChildSet = relationChildSet;
	}

	public double getRelationWordScore() {
		return relationWordScore;
	}

	public void setRelationWordScore(double relationWordScore) {
		this.relationWordScore = relationWordScore;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}

	public int getKeywordId() {
		return keywordId;
	}

	public void setKeywordId(int keywordId) {
		this.keywordId = keywordId;
	}

	public boolean isMoreImportant(WikiWord wiki) {
		int len = this.getPageTitle().split("_").length;
		int wikiLen = wiki.getPageTitle().split("_").length;
		if (wiki.getPageTitle().split("_").length == 1)
			return true;
		else if (this.getFirstIndex() == wiki.getFirstIndex()
				&& this.getPageTitle().length() > wiki.getPageTitle().length())
			return true; // new wiki word is the prefix
		else if (this.getFirstIndex() + len == wiki.getFirstIndex() + wikiLen
				&& this.getFirstIndex() < wiki.getFirstIndex())
			return true; // new wiki word is the suffix
		else if (len > wikiLen)
			return true;
		else if (len == wikiLen
				&& (this.getPageTitle().startsWith("the_") || this
						.getPageTitle().startsWith("The_")))
			return false;
		else if (len == wikiLen
				&& (wiki.getPageTitle().startsWith("the_") || wiki
						.getPageTitle().startsWith("The_")))
			return true;
		else
			return false;
	}

	public int getWordLen(HitHotLocale locale) {
		int result = 0;
		if (!locale.isCjk())
			result = this.getPageTitle().split("_").length;
		else
			result = this.getPageTitle().length();

		return result;
	}

	/*
	 * TODO fix error, or delete this method
	public static int[] childIds(long fromId) {
		return null;
		String lang = Config.getInstance().getLang();
		DataSourceManager manager = null;
		if ("zh".equals(lang))
			manager = (DataSourceManager) Constants.APPLICATION_CONTEXT
					.getBean("dataSourceManagerZH");
		else
			manager = (DataSourceManager) Constants.APPLICATION_CONTEXT
					.getBean("dataSourceManagerEN");

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		List<Integer> childList = new ArrayList<Integer>();
		try {
			conn = manager.getDataSource().getConnection();
			String query = "SELECT pl_to FROM pagelinks l WHERE pl_from = "
					+ fromId + " and pl_to is not null ";

			if (conn != null && !conn.isClosed()) {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(query);

				while (rs.next()) {
					childList.add(rs.getInt("pl_to"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			log.error(e);
		} finally {
			try {
				rs.close();
				stmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		int[] result = new int[childList.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = childList.get(i).intValue();
		}
		// Runtime.getRuntime().gc();
		return result;
	}
		*/

	/**
	 * 取出所有同義字
	 * 
	 * @return
	 */
	/*
	 * TODO fix error, or delete this method
	public WikiWordList getSynons() {
		WikiWordList result = new WikiWordList();
		DataSourceManager manager = WikiFactory.getInstance()
				.getDataSourceManager();

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = manager.getDataSource().getConnection();
			String query = "SELECT * FROM disamblinks l WHERE synon_id is not null and page_from = "
					+ this.ambigId;

			if (conn != null && !conn.isClosed()) {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(query);

				while (rs.next()) {
					WikiWord word = new WikiWord();
					word.setKeywordId(rs.getInt("synon_id"));
					word.setPageTitle(rs.getString("synon"));
					result.add(word);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			log.error(e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return result;
	}
		*/

	public int getRedirectId() {
		return redirectId;
	}

	public void setRedirectId(int redirectId) {
		this.redirectId = redirectId;
	}

	public int getAmbigId() {
		return ambigId;
	}

	public void setAmbigId(int ambigId) {
		this.ambigId = ambigId;
	}

	public String getSynset() {
		return synset;
	}

	public void setSynset(String synset) {
		this.synset = synset;
	}

	public int getLinks() {
		return links;
	}

	public void setLinks(int links) {
		this.links = links;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Boolean getIsBasicLatin() {
		if (isBasicLatin == null) {
			boolean isTrue = true;
			for (char c : getPageTitle().toCharArray()) {
				Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
				
				isTrue = (ub == Character.UnicodeBlock.BASIC_LATIN);
				
				if (!isTrue)
					break;
			}
			isBasicLatin = isTrue ? Boolean.TRUE : Boolean.FALSE;
		}
		
		return isBasicLatin;
	}

	public void setIsBasicLatin(Boolean isBasicLatin) {
		this.isBasicLatin = isBasicLatin;
	}
}
