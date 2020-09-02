package com.intumit.solr.robot;

import java.io.File;

import org.ansj.domain.Result;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.library.Library;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

import junit.framework.TestCase;

public class AnsjTest {
	private static KuromojiUtil util = null;
	private static Tenant tenant = null;
	private static QAUtil qautil = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("jetty.home", "./jetty9");
		System.setProperty("solr.solr.home", "./kernel");
		HibernateUtil.init();
		RobotTestHelper.initMessageUtil();
		HazelcastUtil.init();
		tenant = RobotTestHelper.getAndSaveTenantForTest(HitHotLocale.zh_TW);
		qautil = QAUtil.getInstance(tenant);
		qautil.init();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		RobotTestHelper.tearDown(tenant);
		//HazelcastUtil.shutdown();
		HibernateUtil.shutdown();
	}

	@Before
	public void setUp() throws Exception {
		tenant.setLocale(HitHotLocale.zh_TW);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		Forest f1 = new Forest();
		String userLibrary = "library" + File.separator + "default.dic";
		f1 = Library.makeForest(new File(WiSeEnv.getWebServerHome() + File.separator + userLibrary).getAbsolutePath());

		f1.addBranch("國旅卡", new String[] { "f1", "10000" });
		f1.addBranch("特約商", new String[] { "f2", "10000" });
		f1.addBranch("個人專戶", new String[] { "f2", "10000" });
		f1.addBranch("退休金", new String[] { "f2", "10000" });

		String content = "國旅卡特約商真是大到不能倒！";
		Result result = ToAnalysis.parse(content, f1);
		System.out.println(result);
		TestCase.assertEquals("國旅卡", result.getTerms().get(0).getName());
		TestCase.assertEquals("f1", result.getTerms().get(0).getNatureStr());
		TestCase.assertEquals("特約商", result.getTerms().get(1).getName());
		TestCase.assertEquals("f2", result.getTerms().get(1).getNatureStr());

		content = "想要瞭解個人專戶當中的退休金請領時間規定";
		result = ToAnalysis.parse(content, f1);
		System.out.println(result);
		TestCase.assertEquals("國旅卡", result.getTerms().get(0).getName());
		TestCase.assertEquals("f1", result.getTerms().get(0).getNatureStr());
		TestCase.assertEquals("特約商", result.getTerms().get(1).getName());
		TestCase.assertEquals("f2", result.getTerms().get(1).getNatureStr());
	}


}
