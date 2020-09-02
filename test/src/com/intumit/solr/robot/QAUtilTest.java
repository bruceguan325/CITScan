package com.intumit.solr.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ansj.domain.Term;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.servlet.HazelcastUtil;

import junit.framework.TestCase;

public class QAUtilTest {
	QAUtil qu = null;
	BulkRequestBuilder bulkRequest = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Locale.setDefault(Locale.TAIWAN);
		System.setProperty("solr.solr.home", "./kernel");
		HibernateUtil.init();
		SearchManager.initESNode();
	}

	@Before
	public void setUp() throws Exception {
		/*qu = new QAUtil(WiseSystemConfig.get().getDefaultTenant());
		qu.init();*/
	}

	@After
	public void tearDown() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		SearchManager.closeESNode();
		HibernateUtil.shutdown();
		HazelcastUtil.shutdown();
	}

	//@Test
	public void testElasticsearchPercolator() {
		String esCoreName = "core1111";
		QA qa = new QA();
		qa.setKid(1111L);
		qa.setQuestionAltTemplates(
				Arrays.asList(new String[] {

				}));

		String[] alts = new String[] {
				"請問 分行在哪",
				"請問 分行怎麼去",
		};

		try {
			Client esCli = SearchManager.getESClient();
			PercolateResponse response = QASaver.esInitCore(esCli, esCoreName);
			QASaver.esDeletePercolatorsByKid(esCli, esCoreName, qa.getKid());
			response = QASaver.esInitCore(esCli, esCoreName);
			bulkRequest = esCli.prepareBulk();
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		Client esCli = SearchManager.getESClient();

		for (String alt : alts) {
			String[] tokens = StringUtils.split(alt, " ");
			BoolQueryBuilder qb = QueryBuilders.boolQuery();

			for (String token : tokens) {
				qb.must(QueryBuilders.matchPhraseQuery("terms", token));
			}

			try {
				IndexRequestBuilder indexReq = QASaver.esAddPercolator(esCli, esCoreName, qa.getKid(), alt, qb);
				if (bulkRequest != null) {
					bulkRequest.add(indexReq);
				}
				else {
					indexReq.execute().actionGet();
				}
			}
			catch (ElasticsearchException e) {
				e.printStackTrace();
			}
			System.out.println(qb);
		}

		try {
			if (bulkRequest != null && bulkRequest.numberOfActions() > 0) {
				BulkResponse bulkResponse = bulkRequest.execute().actionGet();
				if (bulkResponse.hasFailures()) {
					// process failures by iterating through each bulk response item
				}
			}
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}

		String[][] testCases = new String[][] {
				new String[] { "請問松江分行在哪", alts[0] },
				new String[] { "請問松江分行在哪裡", alts[0] },
				new String[] { "我想請問松江分行在哪裡", alts[0] },
				new String[] { "松江分行在哪", null },
				new String[] { "松江分行在哪裡，請問", alts[0] },
				new String[] { "請問南港分行怎麼去", alts[1] },
				new String[] { "南港分行怎麼去", null },
				new String[] { "請問南港分行在哪，分行怎麼去", null },
		};
		// 現在開始搜尋 Percolator
		try {
			for (String[] testCase : testCases) {
				List<PercolationResult> results = QAUtil.searchPercolators(esCoreName, testCase[0]);

				if (results.size() > 0) {
					for (PercolationResult result : results) {
						List<String> tpls = new ArrayList<String>(Arrays.asList(new String[] { result.getOriginalAlt() }));
						System.out.println("Q: [" + testCase[0] + "] => KID:" + result.getKid() + " / ALT:" + result.getOriginalAlt() + " / EXPECT: "
								+ testCase[1] + " / " + StringUtils.equals(result.getOriginalAlt(), testCase[1]));
					}
				}
				else {
					System.out.println("Q: [" + testCase[0] + "] => EXPECT: " + testCase[1] + " / " + (testCase[1] == null));
				}
			}
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	//@Test
	/* 以前不需要 ctx + tenant
	 * 但現在這邊要補上 Mock Tenant + 同義詞，才會真的做動
	 */
	public void testReconstructQuestion() {
		List<String> l = null;

		l = qu.reconstructQuestion("LINE Pay如何申請", ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, null, null);
		for (String tmp : l) {
			System.out.println(tmp);
		}
		TestCase.assertTrue(l.contains("line pay如何申請"));
		TestCase.assertTrue(l.contains("line pay怎麼申請"));

		l = qu.reconstructQuestion("KOKO i刷", ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, null, null);
		for (String tmp : l) {
			System.out.println(tmp);
		}
		TestCase.assertTrue(l.contains("koko i刷"));
		TestCase.assertTrue(l.contains("koko i刷金融卡"));

		l = qu.reconstructQuestion("PLAY(COMBO)悠遊聯名卡的專屬優惠", ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, null, null);
		for (String tmp : l) {
			System.out.println(tmp);
		}
		TestCase.assertTrue(l.contains("play combo悠遊聯名卡的專屬優惠"));
		TestCase.assertTrue(l.contains("play combo悠遊卡專屬優惠"));
	}

	@Test
	public void testEnglishAnalyzer() {
		List<Term> terms = new EnglishUtil().tokenize("I'd like to buy two great books. I'd bought two expensive book. Now I didn't have ten books. I'm handsome yesterday morning. You're bitch at 2pm. We fucked up. Please don't laugh at me. We have been here for a long time.");
		System.out.println(terms);
		terms = new EnglishUtil().tokenize("Barack Hussein Obama II  is the 44th and current President of the United States, and the first African American to hold the office.");
		System.out.println(terms);
	}

	public static void main(String[] args) throws IOException {
	}
}
