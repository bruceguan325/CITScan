package com.intumit.solr.robot.qarule;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.DummyTenant;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.QUESTION_TYPE;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.qaplugin.DummyOptionsQAPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugins;
import com.intumit.solr.robot.qaplugin.QAPluginsTestUtil;
import com.intumit.solr.servlet.HazelcastUtil;

public class QAMatchRuleControllerTest {

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

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	static Map<String, String> qaPair = new HashMap<String, String>();
	
	static {
		qaPair.put("嗨嗨", "哈囉！");
		qaPair.put("你好", "你好喔！");
	}
	
	QAContext createContext() {
		String qaId = java.util.UUID.randomUUID().toString();
		QAContext ctx = QAContextManager.create(qaId);
		ctx.setTenant(new DummyTenant());
		
		return ctx;
	}
	
	QARule prepareDummyAnswers() {
		Map<String, Object> cfg = new HashMap<String, Object>();
		cfg.put("qaPair", qaPair);
		
		QAMatchRuleController c = new QAMatchRuleController();
		QARule rule = new DummyAnswerRule();
		rule.init(cfg);
		
		return rule;
	}

	@Test
	public void testDummyAnswer() {
		QAContext ctx = createContext();
		QAMatchRuleController c = new QAMatchRuleController();
		QARule rule = prepareDummyAnswers();
		c.addRule(rule, QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		
		String testQuesiton = "嗨嗨";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals(qaPair.get(testQuesiton), ctx.getAnswerText().toString());
		
		ctx.shift();
		testQuesiton = "你好";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals(qaPair.get(testQuesiton), ctx.getAnswerText().toString());
		
		ctx.shift();
		testQuesiton = "科科";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertFalse(ctx.hasAnswerText());
	}

	@Test
	public void testTooManyRepeat() {
		QAContext ctx = createContext();
		
		Map<String, Object> cfg = new HashMap<String, Object>();
		cfg.put("limit", 3);
		
		QAMatchRuleController c = new QAMatchRuleController();
		QARule rule = new TooManyRepeatQuestionRule();
		rule.init(cfg);
		c.addRule(rule, QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		
		QARule dummy = prepareDummyAnswers();
		c.addRule(dummy, QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		
		String testQuesiton = "嗨嗨";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals(qaPair.get(testQuesiton), ctx.getAnswerText().toString());
		
		ctx.shift();

		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals(qaPair.get(testQuesiton), ctx.getAnswerText().toString());
		
		ctx.shift();

		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals("default.answer", ctx.getAnswerText().toString());
		Assert.assertEquals(QUESTION_TYPE.REPEAT_TOO_MANY_TIMES, ctx.getQuestionType());
	}

	@Test
	public void testForwardToCrm() {
		QAContext ctx = createContext();
		
		Map<String, Object> cfg = new HashMap<String, Object>();
		
		QAMatchRuleController c = new QAMatchRuleController();
		QARule rule = new ForwardToCrmRule();
		rule.init(cfg);
		c.addRule(rule, QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		c.addRule(prepareDummyAnswers(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		
		ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
		ctx.shift();
		
		// 先測回答 Yes （理想上應該可以多測幾種不同的回答好的方式）
		// 因為 DummyTenant 沒有 department id，理論上回答一定是 OutOfService
		String testQuesiton = "Y";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals("{{F:FORWARD_HEADER_WEBCHAT_OFFLINE}}<br/>{{F:ENTER}}1. <span class='question-clickable' data-opnum='1'>{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}</span><br/>{{F:ENTER}}2. <span class='question-clickable' data-opnum='2'>{{F:FORWARD_OPTION_MESSAGE_BOARD}}</span><br/>{{F:FORWARD_FOOTER_GENERAL}}", ctx.getAnswerText().toString());
		
		// 接著回答 3，測是否能測到 State2（參見 ForwardToCrmRule）
		ctx.shift();
		testQuesiton = "3";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals("{{F:REDIRECT_MESSAGE_BOARD}}", ctx.getAnswerText().toString());
		
		// 測轉真人回答 No 的狀態 （理想上應該可以多測幾種不同的回答不好的方式）
		ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
		ctx.shift();
		testQuesiton = "N";
		ctx.setCurrentQuestion(testQuesiton);
		c.check(ctx);
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals("{{F:CONTINUE_TALKING_WITH_ROBOT}}", ctx.getAnswerText().toString());
	}

	@Test
	public void testChooseQAOption() {
		QAContext ctx = createContext();
		
		Map<String, Object> cfg = new HashMap<String, Object>();
		
		// 準備 Controller & chain
		QAMatchRuleController c = new QAMatchRuleController();
		QARule rule = new ForwardToCrmRule(cfg);
		c.addRule(rule, QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		
		rule = new ChooseQAOptionRule();
		c.addRule(rule, QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		
		c.addRule(prepareDummyAnswers(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		
		// 開始測試
		QAPluginsTestUtil.insert(DummyOptionsQAPlugin.class, DummyOptionsQAPlugin.ID, "DummyOptionsQAPlugin");
		QAPlugin dop = QAPlugins.get(DummyOptionsQAPlugin.ID); 
		String testQuestion = "嗨嗨";
		ctx.setCurrentQuestion(testQuestion);
		c.check(ctx);
		// 偷加選項
		dop.onApiAfterProfessionalIndexMatching(testQuestion, ctx);

		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertEquals("哈囉！<br>{{F:ENTER}}1." + testQuestion, ctx.getAnswerText().toString());

		// 回答選項1
		ctx.shift();
		ctx.setCurrentQuestion("1");
		c.check(ctx);
		
		// 代表問句被正確 rewrite 為「嗨嗨」，且能正確跑完後面的 chain 拿到「哈囉！」為答案
		Assert.assertEquals(testQuestion, ctx.getCurrentQuestion());
		Assert.assertEquals(QUESTION_TYPE.PROFESSIONAL, ctx.getQuestionType());
		Assert.assertTrue(ctx.hasAnswerText());
		Assert.assertTrue(StringUtils.startsWith(ctx.getAnswerText().toString(), "哈囉"));  
		
		// 清除
		QAPluginsTestUtil.remove(DummyOptionsQAPlugin.ID);
	}
}
