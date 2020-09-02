package com.intumit.solr.robot.wivo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.fuzzy.PhoneticSimilarity;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.util.Assert;

public class WiVOEntryDictionaryTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("solr.solr.home", "./kernel");
		HibernateUtil.init();
		MessageUtil.initialize();
		HazelcastUtil.init();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		HibernateUtil.shutdown();
		HazelcastUtil.shutdown();
	}

	private Tenant tmpTenant;
	private List<WiVoEntry> entries = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		tmpTenant = new Tenant();
		tmpTenant.setId(99999999);
		tmpTenant.setName("Tmp tenant " + System.currentTimeMillis());
		tmpTenant.setNotes("" + System.currentTimeMillis());
		//Tenant.saveOrUpdate(tmpTenant);
	}

	@After
	public void tearDown() throws Exception {
		//Tenant.delete(tmpTenant.getId());
		for (WiVoEntry w: entries) {
			WiVoEntry.delete(w);
		}
	}

	@Test
	public void testSearch() {
		WiVoEntry w = null;
		try {
		w = WiVoEntry.save(tmpTenant.getId(), "web", "匹匹", "痞痞", "", true); entries.add(w);
		w = WiVoEntry.save(tmpTenant.getId(), "web", "草原", "", "", true); entries.add(w);
		w = WiVoEntry.save(tmpTenant.getId(), "web", "macro", "", "micro", true); entries.add(w);
		w = WiVoEntry.save(tmpTenant.getId(), "web", "小麥克", "", "小mic", true); entries.add(w);
		w = WiVoEntry.save(tmpTenant.getId(), "web", "hard", "", "soft", true); entries.add(w);
		w = WiVoEntry.save(tmpTenant.getId(), "web", "macos", "", "windows", true); entries.add(w);
		
		// 故意不同 channel
		w = WiVoEntry.save(tmpTenant.getId(), "app", "飛過", "", "跑過", true); entries.add(w);
		
		/*Set<WiVoEntryMatched>[] result = WiVoEntryDictionary.search(tmpTenant.getId(), "我看著一皮皮駿馬跑過草猿，而我正在使用microsoft的windows 10。那個人叫做小mic，是個好人。", "web");
		
		for (Set<WiVoEntryMatched> r: result) {
			System.out.println(r);
		}*/
		
		WiVoUtil wu = WiVoUtil.createNewInstance(tmpTenant.getId(), FuzzyLevel.DEFAULT, PhoneticSimilarity.NORMAL, true);
		String t = wu.suggestFullQuestion("我看著一皮皮駿馬跑過草猿，還有痞痞的馴馬師在樹下休息。", "web");
		System.out.println(t);
		Assert.assertEquals("我看著一匹匹駿馬跑過草原，還有痞痞的馴馬師在樹下休息。", t);
		
		t = wu.suggestFullQuestion("我看著一皮皮駿馬跑過草猿，而我正在使用microsoft的windows 10。那個人叫做小mic，是個好人。", "web");
		System.out.println(t);
		Assert.assertEquals("我看著一匹匹駿馬跑過草原，而我正在使用microsoft的macos 10。那個人叫做小麥克，是個好人。", t);
		
		} catch (Exception e) {
		}
	}

}
