package com.intumit.solr.robot;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.UserClueTag;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.tenant.Tenant;

import junit.framework.TestCase;

public class FunctionUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		HibernateUtil.init();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		HibernateUtil.shutdown();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		int tid = 999999;
		QAContext ctx = QAContextManager.create("XXXX");
		UserClue uc = new UserClue();
		uc.setTenantId(tid);
		uc.setLineUserId("ABC");
		UserClue.saveOrUpdate(uc);
		System.out.println("Temp UC:" + uc.getId());
		
		Tenant t = new Tenant();
		t.setId(tid);
		ctx.setTenant(t);
		ctx.setUserClue(uc);
		UserClueTag.addTagToUser(uc, "SC_LOAN_APPLY_SENT");
		List<UserClueTag> tags = UserClueTag.findUserClueTags(uc, "SC_LOAN_APPLY_SENT");
		System.out.println(tags.get(0).getId() + "/" + tags.get(0).getTag());
		
		String[][] testCases = new String[][] {
				{"{{CONST:ABCDEFG}}{{PIPE}}{{CONST:HIJKLMNOP}}{{PIPE}}{{PRINT}}", "HIJKLMNOP"},
				{"{{CONST:ABCDEFG}} {{CONST:HIJKLMNOP}}{{PIPE}}{{PRINT}}", "ABCDEFG HIJKLMNOP"},
				{"{{CONST:ABCDEFG}} {{CONST:HIJKLMNOP}}", "ABCDEFG HIJKLMNOP"},
				{"{{CONST:我要查123456789這個號碼}}{{PIPE}}{{REGEX:::([0-9]+)}}", "123456789"},
				{"{{CONST:我要查123456789這個號碼}}{{PIPE}}{{REGEX:::(?:^|[^0-9])([0-9]{8})(?:$|[^0-9])}}", ""},
				{"{{CONST:我要查123456789這個號碼}}{{PIPE}}{{REGEX:::(?:^|[^0-9])([0-9]{9})(?:$|[^0-9])}}", "123456789"},
				{"{{CONST:我要查123456789}}{{PIPE}}{{REGEX:::(?:^|[^0-9])([0-9]{9})(?:$|[^0-9])}}", "123456789"},
				{"{{REGEX:[[CONST:我要查123456789]]::(?:^|[^0-9])([0-9]{9})(?:$|[^0-9])}}", "123456789"},
				{"{{CONST:ABCDEFG}}{{PIPE}}{{CONST:OOO}} {{CONST:HIJKLMNOP}}{{PIPE}}{{EMPTY}}", "OOO "},
				{"{{JSONPATH:$.store[?(@.name=='INTUMIT')].tel::NO::{\"store\":[{\"name\":\"INTUMIT\",\"tel\":\"02-29912100\"}]} }}", "02-29912100"},
				{"{{JSONPATH:$.store[0].name::NO::{\"store\":[{\"name\":\"INTUMIT\",\"tel\":\"02-29912100\"}]} }}", "INTUMIT"},
				{"{{JSONPATH:$.store[0].tel::NO::{\"store\":[{\"name\":\"INTUMIT\",\"tel\":\"02-29912100\"}]} }}", "02-29912100"},
				{"{{JSONPATH:$.store[0].phone::NO::{\"store\":[{\"name\":\"INTUMIT\",\"tel\":\"02-29912100\"}]} }}", "NO"},
				{"{{EXPR:(2+3)*5}}", "25.0"},
				{"{{NCMP:25 > 20}}", "true"},
				{"{{NCMP:(2+3)*5 = 25}}", "true"},
				{"{{OPTION:OOO:XXX}}", "{{OPTION:OOO:XXX}}"},
				{"{{USERTAG:has::SC_LOAN_APPLY_SENT}}", "true"},
		};
		
		for (String[] testCase: testCases) {
			String r = FunctionUtil.collectExecAndReplace(testCase[0], ctx);
			
			System.out.print("[" + StringUtils.equals(testCase[1], r) + "]");
			System.out.println(" *****[" + r + "]******");
			TestCase.assertEquals(testCase[1], r);
		}
		UserClueTag.clearTagOnUser(uc, "SC_LOAN_APPLY_SENT");
		TestCase.assertEquals("false", FunctionUtil.collectExecAndReplace("{{USERTAG:has::SC_LOAN_APPLY_SENT}}", ctx));
	}

}
