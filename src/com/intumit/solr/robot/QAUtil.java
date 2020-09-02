package com.intumit.solr.robot;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.ansj.domain.Nature;
import org.ansj.domain.Result;
import org.ansj.library.AmbiguityLibrary;
import org.ansj.recognition.impl.UserDicNatureRecognition;
import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.ansj.util.MyStaticValue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.nlpcn.commons.lang.tire.GetWord;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;

import com.atilika.kuromoji.TokenizerBase.Mode;
import com.atilika.kuromoji.ipadic.neologd.Token;
import com.atilika.kuromoji.ipadic.neologd.Tokenizer;
import com.google.common.collect.BiMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.log4j.Logger;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.WikiWord;
import com.intumit.smartwiki.recommend.renew.AhoCorasick;
import com.intumit.smartwiki.recommend.renew.SearchResult;
import com.intumit.smartwiki.util.NameValuePair;
import com.intumit.smartwiki.util.TextUtil;
import com.intumit.solr.ClusterMembershipListener;
import com.intumit.solr.SearchManager;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.qparser.ExtendedDismaxQParserPlugin;
import com.intumit.solr.robot.TemplateUtil.Cleaner;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.ambiguity.AmbiguityDatabase;
import com.intumit.solr.robot.dictionary.CategorizedKeyValuePair;
import com.intumit.solr.robot.dictionary.CategorizedValueParser;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.robot.dictionary.EmbeddedFuzzyDictionaryDatabase;
import com.intumit.solr.robot.dictionary.EssentialKeywordDictionary;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.solr.robot.qaplugin.CustomQA;
import com.intumit.solr.robot.qaplugin.HierarchicalQA;
import com.intumit.solr.robot.qaplugin.QADialogPlugin;
import com.intumit.solr.robot.qarule.ForwardToCrmRule.Forward;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.robot.qarule.WiVoSuggestRule;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.RequestRateLimiter;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class QAUtil {
	public static Logger infoLog = Logger.getLogger(QAUtil.class.getName());
	private static final int QUERY_TIME_ALLOWED = 15000;

	/**
	 * qa-ajax.jsp的session timeout（分鐘）
	 */
	public static final long QA_SESSION_TIMEOUT_MINS = 15;

	public static final String TAG_NAME_FORMAL_ANSWER = "F";
	public static final String TAG_NAME_ANSWER_SHORTCUT = "SHORTCUT";

	public enum CrmRedirectTag {

		LHCSPCODE3501(Forward.PERSONNEL), LHCSPCODE3502(Forward.MESSAGE), LHCSPCODE3503(Forward.SURVEY);

		public final Forward forward;

		CrmRedirectTag(Forward forward) {
			this.forward = forward;
		}

		public static CrmRedirectTag match(String answer) {
			CrmRedirectTag tag = null;
			for (CrmRedirectTag t : values()) {
				if (TemplateUtil.matchWholeByTagName(answer, t.name())) {
					tag = t;
					break;
				}
			}
			return tag;
		}

	}

	public static final String ANSWER_LINK_TARGET = "_blank"; //

	public static final Set<String> GENERAL_CATEGORIES = new HashSet<String>(
			Arrays.asList(new String[] { "全部", "通用", "" }));
	public static final String[] NLP_INCLUDED_NATURES = new String[] { "a", "b", "c", "d", "f", "gi", "gm", "i", "j",
			"l", "m", "n", "q", "ry", "s", "t", "v", "z", "#" };
	public static final String[] CONFIRM_STRING_ARRAY = { "好", "行", "可", "要", "需要", "對", "隊", "兌", // "對" 的常見注音不選字
			"是", "事", "式", "市", "視", "室", "試", // "是" 的常見注音不選字
			"厚", "後", "ㄟ賽", "素", // 台語
			"同意", "願意", "可以", "沒錯", "繼續", "沒問題", "請繼續", "應該是", "應該對", "應該沒錯", "大概是", "大概對", "大概沒錯", // 較為懷疑的口氣
			"y", "yes", "ok", "okey", "good", "fine", // 英文
	};
	public static final String[] CONFIRM_NO_STRING_ARRAY = { "不", "不好", "不行", "不對", "不是", "不可", "不要", "不想", "拒絕", "等等",
			"不用", "不需要", "不願意", "鼻要", "n", "no", "不可以", "否", "非", "母厚", "某後", "每賽", "美賽", "某愛", "錯", "沒有", "沒" };

	public static final Pattern CASUAL_AUXILIARY_STRING_BEGINING_PATTERN = Pattern
			.compile("(?msi)^(感覺|感謝|剛剛|剛才|剛去|剛|真|真的|我想|也許|覺得|我覺得|哈|呵|是喔)+");

	public static final String[] AUXILIARY_STRING_ENDING_ARRAY = { "喔", "耶", "的", "唷", "吧", "啊", "滴", "哩", "了", "呀",
			"唉", "哎", "咦", "噫", "呢", "呵", "哦", "嘿", "噢", "喲", "呵", "壓", "吔", "ㄟ", "ㄚ", };

	// CHANGABLE
	public static String CURRENT_EVALUATION_LOG_NS = "qa:cathaybk:test:week1:";
	public static float MLT_COMMON_SENSE_FACTOR = 15;

	public static boolean ENABLE_NLP = true;
	public static boolean USE_NLPANALYSIS = false; // Use ToAnalysis if false
	// public static String CASUAL_CORE = "core1";

	public static final int MAX_CLAUSES = 50;
	public static final boolean QA_SAVER_DEBUG = false;
	public static final float MLT_COMMON_SENSE_Q_MIN_SCORE = 5;
	public static final String DATATYPE_COMMON_SENSE = "COMMON_SENSE";
	public static final String DATATYPE_CASUAL = "CASUAL";
	public static final String DATATYPE_QA_AUDIT = "QA_AUDIT";
	public static final int ALT_LIMIT_PER_DOC = 100000;
	public static final int ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER = 2100000;
	public static final int ALT_LIMIT_PER_QUESTION = 2000000; // 500,000,000 // 五億
	// public static final String QUERY_BOOST_STRING_MATCHING = "QUESTION_s^100000
	// QUESTION_ALT_ms^100000";
	public static final String QUERY_BOOST_FULLTEXT_FIELDS = "QUESTION_t^100.0 QUESTION_ALT_mt^100.0";
	public static final String QUERY_BOOST = "QUESTION_t^1.0 QUESTION_s^100000 QUESTION_ALT_mt^1.0 QUESTION_ALT_ms^100000";
	public static final String QUERY_BOOST_WITH_PHONETIC_HOMONYM = "QUESTION_t^1.5 QUESTION_s^100000 QUESTION_ALT_mt^0.8 QUESTION_ALT_ms^100000 QUESTION_p^0.8 QUESTION_ALT_mp^0.2";
	public static final String QUERY_BOOST_CASUAL = "QUESTION_t^1.5 QUESTION_s^100000 QUESTION_ALT_mt^0.8 QUESTION_ALT_ms^100";

	public static final String QA_RESPONSE_FIELDS = "id,kid_l,score,QUESTION_s,Briefly_QUESTION_s,CATEGORY_ms,Status_s,Reject_s,updatedAdminUserId_i,"
			+ QA.FN_QA_CATEGORY + "," + QA.FN_QUESTION_ALT_TPL + "," + QA.FN_EXTRA_PARAMS + "," + QA.FN_IS_NOT_FOR_MLT
			+ "," + QA.FN_IS_NOT_FOR_SEARCH + "," + QA.FN_NOT_APPEND_MLT + "," + QA.FN_IS_KMS_RELATE_EXPIRED_MEMO + ","
			+ "ANSWERPHONE_s,ANSWER_s,ANSWER_ALT_ms,ANSWER_LINK_s,ANSWER_RICHTEXT_s" + "," + "ANSWER_PLUGIN_ID_s,"
			+ HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME + "," + "CUSTOM_SCRIPT_s,ENTITY_VALUES_JSON_s,"
			+ QADialogPlugin.QA_KEY_INDEX_FIELD + "," + QADialogPlugin.QA_USE_DRAFT_INDEX_FIELD + ","
			+ CustomQA.QA_PATTERN_KEY_INDEX_FIELD + "," + CustomQA.SUGGEST_QUESTIONS_INDEX_FIELD + ","
			+ CustomQA.SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD + ","
			+ "DialogName_s,INTENT_ms,ENTITY_ms,BINDER_ID_ml";

	private String qaTagFilter = null;
	private static Comparator<? super SolrDocument> docScoreDescComparator = null;

	public static final String[] TEMPORAL_STR_ENDING = new String[] { "年", "月", "分鐘" };
	public static final Set<String> NOT_A_NUMBER_STR_SET = new HashSet<String>(
			Arrays.asList(new String[] { "多", "好幾" }));

	static final Pattern expandBlock = Pattern.compile("\\(([^\\)]+)\\)");
	static final Pattern intentBlock = Pattern.compile("\\((@[^\\)]+)\\)");
	static final Pattern entityBlock = Pattern.compile("\\((#[^\\)]+)\\)");
	static final Pattern essentialBlock = Pattern.compile("\\[([^\\]]+)\\]");

	static Tokenizer nativeJpTokenizer = null;

	static {
		try {
			nativeJpTokenizer = new Tokenizer.Builder().mode(Mode.NORMAL).build();
			docScoreDescComparator = new Comparator<SolrDocument>() {

				@Override
				public int compare(SolrDocument d1, SolrDocument d2) {
					Number s1 = (Number) d1.getFirstValue("score");
					Number s2 = (Number) d2.getFirstValue("score");

					return s2.doubleValue() > s1.doubleValue() ? 1 : -1;
				}

			};

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isConfirmWithYes(String answer) {
		String tmp = StringUtils.lowerCase(answer);
		if (StringUtils.endsWithAny(tmp, AUXILIARY_STRING_ENDING_ARRAY)) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}

		if (tmp.length() > 1 && StringUtils.countMatches(tmp, tmp.substring(0, 1)) == tmp.length()) {
			tmp = tmp.substring(0, 1);
		}
		return ArrayUtils.contains(CONFIRM_STRING_ARRAY, tmp);
	}

	public static boolean isConfirmWithNo(String answer) {
		String tmp = StringUtils.lowerCase(answer);
		if (StringUtils.endsWithAny(tmp, AUXILIARY_STRING_ENDING_ARRAY)) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}

		if (tmp.length() > 1 && StringUtils.countMatches(tmp, tmp.substring(0, 1)) == tmp.length()) {
			tmp = tmp.substring(0, 1);
		}
		return ArrayUtils.contains(CONFIRM_NO_STRING_ARRAY, tmp);
	}

	private static HashMap<Integer, QAUtil> instanceMap = null;

	public static QAUtil getInstance(Tenant tenant) {
		if (instanceMap == null) {
			instanceMap = new HashMap<Integer, QAUtil>();
		}
		if (!instanceMap.containsKey(tenant.getId())) {
			instanceMap.put(tenant.getId(), new QAUtil(tenant));
		}

		return instanceMap.get(tenant.getId());
	}

	public static QAUtil getInstance(int tenantId) {
		if (instanceMap == null) {
			instanceMap = new HashMap<Integer, QAUtil>();
		}
		if (!instanceMap.containsKey(tenantId)) {
			Tenant t = Tenant.get(tenantId);
			instanceMap.put(tenantId, new QAUtil(t));
		}

		return instanceMap.get(tenantId);
	}

	public static void cleanInstance(Integer tenantId) {
		if (instanceMap != null && instanceMap.containsKey(tenantId))
			instanceMap.remove(tenantId);
		CustomDataDictionary.clear(tenantId);
	}

	public static SolrServer getCasualServer(Tenant t, boolean writeMode) {
		String url = t == null ? null : StringUtils.trimToNull(t.getCasualCoreUrl());

		if (url == null)
			return null;

		if (!StringUtils.startsWith(url, "http")) {
			if (writeMode && WiseSystemConfig.get().getLbModeEnable()
					&& !ClusterMembershipListener.getInstance().isLeaderNow()) {
				String masterUrl = SearchManager.getCoreUrl4Write(url);

				if (masterUrl != null) {
					return SearchManager.getRemoteServer(masterUrl);
				}
			}
			return SearchManager.getServer(url);
		}

		return SearchManager.getRemoteServer(url);
	}

	public static SolrDocument getCasualQASolrDocument(Tenant t, long kid) {
		SolrServer server = getCasualServer(t, false);

		if (server != null) {
			SolrQuery queryBase = new SolrQuery();
			queryBase.setQuery("kid_l:" + kid);
			queryBase.addFilterQuery("-isPart_i:[2 TO *]");
			queryBase.addFilterQuery("dataType_s:" + DATATYPE_CASUAL);
			queryBase.setRows(1);
			try {
				SolrDocumentList docList = server.query(queryBase).getResults();
				if (docList.size() > 0) {
					return docList.get(0);
				}
			} catch (SolrServerException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static long id2Kid(String id) {
		long kid = Long
				.parseLong(StringUtils.trim(StringUtils.substringBefore(StringUtils.substringAfter(id, "-"), "-")));
		return kid;
	}
	
	public static String kid2Id(long kid) {
		String id = String.format("%s-%d", DATATYPE_COMMON_SENSE, kid);
		return id;
	}
	
	public static List<String> getAllPossibleFacetTerms(SolrServer server, String fieldNamePrefix,
			String... additionalFilterQuery) {
		SolrQuery queryBase = new SolrQuery();
		queryBase.setQuery("*:*");
		queryBase.setFacet(true);
		queryBase.setFacetLimit(20000);
		queryBase.setFacetMinCount(1);

		if (additionalFilterQuery != null)
			queryBase.addFilterQuery(additionalFilterQuery);

		List<String> list = new ArrayList<String>();
		String[] suffixes = { "_s", "_ms" };

		if (StringUtils.endsWithAny(fieldNamePrefix, suffixes)) {
			suffixes = new String[] { "" }; // 本身就有自帶 _s / _ms 就不需再 append
		}

		for (String suffix : suffixes) {
			queryBase.addFacetField(fieldNamePrefix + suffix);
		}

		try {
			QueryResponse res = server.query(queryBase);
			for (String suffix : suffixes) {
				FacetField ff = res.getFacetField(fieldNamePrefix + suffix);

				for (FacetField.Count c : ff.getValues()) {
					if (!list.contains(c.getName()))
						list.add(StringUtils.trimToEmpty(c.getName()));
				}
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return list;
	}

	private BiMap<Integer, NameValuePair> outputMap = null;
	private AhoCorasick aho;
	private boolean initialized = false;
	private Tenant tenant;
	Map<Long, String> kid2Qmap = new HashMap<>();
	Forest ambiguityForest = null;
	Forest userDefinedForest = null;
	private Map<String, String> kuromojiUserDictionary = new ConcurrentHashMap<>();
	private Tokenizer jpTokenizer = null;
	private EnglishUtil englishUtil = null;

	public QAUtil(Tenant tenant) {
		this.tenant = tenant;
	}

	synchronized void init() {
		if (!initialized) {
			synchronized (this) {
				if (!initialized) { // Double lock to avoid performance issue
					initialized = buildUserDefinedDictionary();
				}
			}
		}

		// Static! 過往 solrconfig.xml 的 maxClauseCount 現在會有問題（static 無法每個 core 有各自的設定）
		// 另外也尚無法確認這是 client 端還是 server 端，如果是 server 端代表也無法影響 remote solr server
		org.apache.lucene.search.BooleanQuery.setMaxClauseCount(40960);
	}

	public void forceRebuildUserDefiniedDictionary() {
		initialized = false;
	}

	public String getStandardQ(long kid) {
		if (kid2Qmap.size() == 0) {
			try {
				SolrServer server = tenant.getCoreServer();
				SolrQuery queryBase = new SolrQuery();
				queryBase.setQuery("*:*");
				queryBase.setFields("id,kid_l,QUESTION_s");
				queryBase.addFilterQuery("-isPart_i:[2 TO *]");
				queryBase.addFilterQuery("dataType_s:" + DATATYPE_COMMON_SENSE);
				queryBase.setRows(50000);
				SolrDocumentList docList = server.query(queryBase).getResults();
				for (SolrDocument doc : docList) {
					Long k = (Long) doc.getFirstValue("kid_l");
					String q = (String) doc.getFirstValue("QUESTION_s");

					kid2Qmap.put(k, q);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return kid2Qmap.get(kid);
	}

	public JSONObject getStandardA(long kid, QAChannel ch, QAContext toBeCopy) {
		QAMatchRuleController ctrl = QAMatchRuleController.getTempInstanceForMakeAnswer(tenant.getId());
		String standardQ = getStandardQ(kid);
		QAContext tmpCtx = new QAContext();
		tmpCtx.setTenant(tenant);

		if (toBeCopy != null) {
			toBeCopy.copyWithoutStateStackAndExplain(toBeCopy, tmpCtx);
		}

		toBeCopy.shift();

		if (ch != null) {
			tmpCtx.setQaChannel(ch.getCode());
		}

		tmpCtx.setOriginalQuestion(standardQ);
		tmpCtx.setOriginalQuestionTime(QAUtil.formatDatetime(Calendar.getInstance().getTime()));
		tmpCtx.setCurrentQuestion(standardQ);

		ctrl.check(tmpCtx);
		JSONObject resp = tmpCtx.genResponseJSON();
		return resp;
	}

	public SolrDocument getMainQASolrDocument(long kid) {
		return getMainQASolrDocument(kid, null);
	}

	/**
	 * 如果 fromMaster == true，強制從 master 取得最新的 Doc 避免在還沒 sync 之前從 slave 取得舊版的資料
	 *
	 * WiseSystemConfig 的 LB mode != true 的時候會直接呼叫 getMainQASolrDocument(long kid)
	 *
	 * @param kid
	 * @param fromMaster
	 * @return
	 */
	public SolrDocument getMainQASolrDocument(long kid, Boolean fromMaster) {
		SolrServer server = null;
		if (fromMaster != null && fromMaster && WiseSystemConfig.get().getLbModeEnable()) {
			server = tenant.getCoreServer4Write();
		} else {
			server = tenant.getCoreServer();
		}
		SolrQuery queryBase = new SolrQuery();
		queryBase.setQuery("kid_l:" + kid);
		queryBase.addFilterQuery("-isPart_i:[2 TO *]");
		queryBase.addFilterQuery("dataType_s:" + DATATYPE_COMMON_SENSE);
		if (qaTagFilter != null) {
			queryBase.addFilterQuery("CATEGORY_ms:*" + qaTagFilter + "*");
		}
		queryBase.setRows(1);
		try {
			SolrDocumentList docList = server.query(queryBase).getResults();
			if (docList.size() > 0) {
				return docList.get(0);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public SolrDocument getFilterQASolrDocument(long kid, Boolean fromMaster, String datatype, Boolean isBanned) {
		SolrServer server = null;
		if (fromMaster != null && fromMaster && WiseSystemConfig.get().getLbModeEnable()) {
			server = tenant.getCoreServer4Write();
		} else {
			server = tenant.getCoreServer();
		}
		SolrQuery queryBase = new SolrQuery();
		queryBase.setQuery("kid_l:" + kid);
		queryBase.addFilterQuery("-isPart_i:[2 TO *]");
		queryBase.addFilterQuery("dataType_s:" + datatype);
		if (qaTagFilter != null) {
			queryBase.addFilterQuery("CATEGORY_ms:*" + qaTagFilter + "*");
		}
		if (isBanned) {
			queryBase.addFilterQuery("-" + QA.FN_BANNED + ":true");
		}
		queryBase.setRows(1);
		try {
			SolrDocumentList docList = server.query(queryBase).getResults();
			if (docList.size() > 0) {
				return docList.get(0);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String[] guessNature(String keyword) {
		try {
			String[] params = this.ambiguityForest.getBranch(keyword).getParam();

			if (params == null)
				params = this.userDefinedForest.getBranch(keyword).getParam();

			if (params != null && params.length > 0) {
				if (StringUtils.isNotEmpty(params[0]) && !StringUtils.equalsIgnoreCase("null", params[0])) {
					return params;
				}
			}
		} catch (Exception ignore) {
			// NatureRecognition may cause NullPointer Exception if cannot recognize Term
		}

		return null;
	}

	public String getUserDefinedNature(String keyword) {
		try {
			String[] params = this.userDefinedForest.getBranch(keyword).getParam();

			if (params != null && params.length > 0) {
				if (StringUtils.isNotEmpty(params[0]) && !StringUtils.equalsIgnoreCase("null", params[0])) {
					return params[0];
				}
			}
		} catch (Exception ignore) {
			// NatureRecognition may cause NullPointer Exception if cannot recognize Term
		}

		return null;
	}

	/**
	 *
	 * @param from    0: WhiteWiki / 1: AMBIGUITY DB / 2: KNOWLEDGE_POINT / 3:
	 *                SYNONYM
	 * @param action  0: NEW / 1: UPDATE / 2: DELETE
	 * @param keyword
	 * @param value   public void updateForest(int from, int action, String keyword,
	 *                String[] params) { String[] oldParams =
	 *                UserDefineLibrary.getParams(this.ambiguityForest, keyword);
	 * 
	 *                if (from <= 2) { if (oldParams == null) {
	 *                Library.insertWord(this.ambiguityForest, new Value(keyword,
	 *                params)); } else { String nature = params[1]; int oldFrom =
	 *                StringUtils.indexOf(nature, "#") != -1 ?
	 *                Integer.parseInt(StringUtils.substringAfter(nature,
	 *                "#").substring(0, 1)) : 99;
	 * 
	 *                if (oldFrom >= from) {
	 *                Library.insertWord(this.ambiguityForest, new Value(keyword,
	 *                params)); } } } else { if (oldParams == null) { oldParams =
	 *                UserDefineLibrary.getParams(this.userDefinedForest, keyword);
	 * 
	 *                if (oldParams == null) {
	 *                Library.insertWord(this.userDefinedForest, new Value(keyword,
	 *                params)); } else { String nature = params[1]; int oldFrom =
	 *                StringUtils.indexOf(nature, "#") != -1 ?
	 *                Integer.parseInt(StringUtils.substringAfter(nature,
	 *                "#").substring(0, 1)) : 99;
	 * 
	 *                if (oldFrom >= from) {
	 *                Library.insertWord(this.userDefinedForest, new Value(keyword,
	 *                params)); } } } } }
	 */

	public static String[] toUserDefinedValue(String kw, String nature) {
		return toUserDefinedValue(kw, nature, 150000);
	}

	public static String[] toUserDefinedValue(String kw, String nature, int baseScore) {
		int score = baseScore;

		if (kw.length() > 2) {
			score *= (kw.length() - 1);
		}

		return new String[] { nature, "" + score };
	}

	public void addToUserDefinedForest(String kw, String forceNature, String natureSuffix) {
		String[] params = guessNature(kw);
		infoLog.info(kw + " (" + (params != null ? Arrays.asList(params) : null) + "), ");

		switch (tenant.getLocale()) {
		case ja_JP:
			if (forceNature != null) {
				userDefinedForest.addBranch(kw, toUserDefinedValue(kw, forceNature + "#" + natureSuffix));
				addKuromojiUserDictionary(kw, forceNature + natureSuffix);
			} else {
				if (params == null) {
					userDefinedForest.addBranch(kw, toUserDefinedValue(kw, "#" + natureSuffix));
				} else {
					try {
						if (StringUtils.startsWith(params[0], "#")) {
							addKuromojiUserDictionary(kw, natureSuffix);
						} else {
							addKuromojiUserDictionary(kw, forceNature + natureSuffix);
						}
					} catch (Exception ignore) {
					}
				}
			}
			break;
		case zh_TW:
		case en_US:
		default:
			if (forceNature != null) {
				userDefinedForest.addBranch(kw, toUserDefinedValue(kw, forceNature + "#" + natureSuffix));
			} else {
				if (params == null) {
					userDefinedForest.addBranch(kw, toUserDefinedValue(kw, "#" + natureSuffix));
				} else {
					try {
						if (StringUtils.startsWith(params[0], "#")) {
							params = toUserDefinedValue(kw, params[0]);
						} else {
							params = toUserDefinedValue(kw, "#" + natureSuffix);
						}
					} catch (Exception ignore) {
					}
					userDefinedForest.addBranch(kw, params);
				}
			}
			break;
		}
	}

	private boolean buildUserDefinedDictionary() {
		ambiguityForest = new Forest();
		userDefinedForest = new Forest();
		kuromojiUserDictionary.clear();
		englishUtil = new EnglishUtil();

		try {
			boolean doJP = (tenant != null && tenant.getLocale() == HitHotLocale.ja_JP);
			// boolean doEN = (tenant != null && tenant.getLocale() == HitHotLocale.en_US);

			String ambiguityLibrary = "library" + File.separator + "ambiguity.dic";
			if (StringUtils.isNotBlank(ambiguityLibrary)) {
				Path path = Paths.get(WiSeEnv.getWebServerHome() + File.separator + ambiguityLibrary);
				if (Files.exists(path)) {
					File file = new File(path.toFile().getCanonicalPath());
					if (file.isFile() && file.canRead()) {
						try {
							AmbiguityLibrary.remove("" + tenant.getId());
							AmbiguityLibrary.putIfAbsent("" + tenant.getId(), file.getAbsolutePath());
							ambiguityForest = AmbiguityLibrary.get("" + tenant.getId());
						} catch (Exception e) {
							System.out.println("init ambiguity  error :" + new File(path.toString())
									+ " because : not find that file or can not to read !");
							e.printStackTrace();
						}
						System.out.println("init ambiguityLibrary ok!");
					} else {
						System.out.println("init ambiguity  warning :" + new File(path.toString())
								+ " because : failed to read !");
					}
					System.out.println("init ambiguityLibrary ok!");
				} else {
					System.out.println("init ambiguity  warning :"
							+ new File(WiSeEnv.getWebServerHome() + File.separator + ambiguityLibrary).getAbsolutePath()
							+ " because : file not found or failed to read !");
				}
			}

			// 加载用户自定义词典
			String userLibrary = "library" + File.separator + "default.dic";
			userDefinedForest = Library
					.makeForest(new File(WiSeEnv.getWebServerHome() + File.separator + userLibrary).getAbsolutePath());

			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			List<WikiWord> wikis = WhiteWikiWordFacade.getInstance().listSynonymKeywords();

			for (WikiWord wiki : wikis) {
				ambiguityForest.addBranch(wiki.getPageTitle(), new String[] { wiki.getPageTitle(), wiki.getNature() });
			}

			Set<String> kps = DictionaryDatabase.getDictionary(tenant.getId(), Purpose.KNOWLEDGE_POINT);

			for (String kp : kps) {
				userDefinedForest.addBranch(kp,
						toUserDefinedValue(kp, "#UserDefined_KP", KnowledgePointDictionary.DEFAULT_SEG_SCORE));
				if (doJP)
					addKuromojiUserDictionary(kp, "UserDefined_KP");

			}
			whiteSet.addAll(kps);

			List<AmbiguityDatabase> adbList = AmbiguityDatabase.listAll(tenant.getId());
			for (AmbiguityDatabase adb : adbList) {
				if (adb.isEnabled() && !whiteSet.contains(adb.getSentence()))
					ambiguityForest.addBranch(adb.getSentence(), adb.getDisambiuationParts());
			}

			Set<String> eks = EssentialKeywordDictionary.eksSet(tenant.getId());
			System.out.println("Loading EssentialKeywordDictionary: ");
			for (String ek : eks) {
				if (!whiteSet.contains(ek)) {
					whiteSet.add(ek);
					addToUserDefinedForest(ek, null, "UserDefined_EK");
				}
			}
			System.out.println();

			List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listAll(tenant.getId());

			for (SynonymKeyword syn : syns) {
				System.out.print("Loading Synonym (" + syn.getKeyword() + ") :");
				if (!whiteSet.contains(syn.getKeyword())) {
					whiteSet.add(syn.getKeyword());
					addToUserDefinedForest(syn.getKeyword(), syn.getNature(), "UserDefined_SSK");
				}
				System.out.println();
				System.out.print("Loading UserDefined Synonym of (" + syn.getKeyword() + ") :");

				for (String synStr : syn.getSynonymList()) {
					if (!whiteSet.contains(synStr)) {
						whiteSet.add(synStr);
						addToUserDefinedForest(synStr, syn.getNature(), "UserDefined_SS");
					}
				}
				System.out.println();
			}

			Map<String, Set<EmbeddedFuzzyDictionaryDatabase>> efds = EmbeddedFuzzyDictionaryDatabase
					.getEmbeddedFuzzyDictionary(tenant.getId());
			for (Map.Entry<String, Set<EmbeddedFuzzyDictionaryDatabase>> efdMap : efds.entrySet()) {
				System.out.print("Loading EmbeddedFuzzyDictionary (" + efdMap.getKey() + ") :");
				for (EmbeddedFuzzyDictionaryDatabase efd : efdMap.getValue()) {
					if (StringUtils.isBlank(efd.getKeyword()))
						continue;
					addToUserDefinedForest(StringUtils.trim(efd.getKeyword()), null, "UserDefined_EFD");
				}
				System.out.println();
			}

			CustomDataDictionary.loadIfNull(tenant.getId());
			reloadKuromojiUserDictionary();
			return true;
		} catch (Exception e) {
			System.out.println("Cannot buildUserDefinedDictionary...");
			e.printStackTrace();

			return false;
		}
	}

	/**
	 * 合併 QA 的程式碼，基本只有這裡就全部搞完
	 */
	public void mergeQA(final QA survived, final QA toBeMerged, boolean dryRun, AdminUser mergedBy) {
		try {
			List<String> qAlts = survived.getQuestionAltTemplates();
			List<String> ihAltTplMkeys = (List) survived.getFieldValues(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS);
			List<String> ihAltTpls = (List) survived.getFieldValues(QA.FN_INHERITANT_ALT_TEMPLATES);
			List<String> altTplsToBeMerged = toBeMerged.getQuestionAltTemplates();
			List<String> mergeDuplicateAltTpls = new ArrayList<String>();
			Set<String> dups = new HashSet<String>();

			// 先準備一個斷詞清單作為檢查重複用
			if (qAlts != null) {
				for (String alt : qAlts) {
					Map<String, String> altData = QA.parseQAAlt(alt);
					String segPart = altData.get("alt");

					if (!dups.contains(segPart)) {
						dups.add(segPart);
					}
				}
			}

			// 從被消滅的QA取得所有斷句並開始依序檢測
			for (String alt : altTplsToBeMerged) {
				Map<String, String> altData = QA.parseQAAlt(alt);
				String segPart = altData.get("alt");

				// 沒有重複就加入（重複的意思是指斷句本身，不是例句）
				if (!dups.contains(segPart)) {
					dups.add(segPart);

					if (qAlts == null)
						qAlts = new ArrayList<String>();
					qAlts.add(alt);
				} else {
					// 有重複的就跳過，後面這個 mergeDuplicateAltTpls 也許可以存在索引內以備不時之需？
					System.out.println("Duplicated alt templates to be merged:" + alt);
					mergeDuplicateAltTpls.add(alt);
				}

				qAlts.add(QAUtil.removeSymbols(toBeMerged.getQuestion(), tenant.getLocale()));
			}

			survived.setQuestionAltTemplates(qAlts);

			// 處理繼承範本
			Collection<Object> ihMkeys2 = (Collection<Object>) toBeMerged
					.getFieldValues(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS);

			// 被消滅知識點若有繼承就要處理
			if (ihMkeys2 != null) {
				Set<Object> toBeMergeMkeys = new HashSet<Object>();
				toBeMergeMkeys.addAll(ihMkeys2);
				// 移除重複的，有重複的不從被消滅方合併過來，就讓他消失
				if (ihAltTplMkeys != null)
					toBeMergeMkeys.removeAll(ihAltTplMkeys);

				Collection<Object> combined2 = (Collection<Object>) toBeMerged
						.getFieldValues(QA.FN_INHERITANT_ALT_TEMPLATES);

				boolean dirty = false;

				for (Object c : combined2) {
					for (Object m : toBeMergeMkeys) {
						if (StringUtils.startsWithIgnoreCase((String) c, (String) m + ":")) {
							if (ihAltTplMkeys == null) {
								ihAltTplMkeys = new ArrayList<>();
								ihAltTpls = new ArrayList<>();
							}
							ihAltTplMkeys.add((String) m);
							ihAltTpls.add((String) c);
							dirty = true;
						}
					}
				}

				if (dirty) {
					survived.setOrUpdateField(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS, ihAltTplMkeys);
					survived.setOrUpdateField(QA.FN_INHERITANT_ALT_TEMPLATES, ihAltTpls);
				}

			}

			// 處理排除句
			if (toBeMerged.getExcludeQuestionAltTemplates() != null
					&& toBeMerged.getExcludeQuestionAltTemplates().size() > 0) {
				List<String> l = survived.getExcludeQuestionAltTemplates();
				if (l == null) {
					l = new ArrayList<String>();
				}
				// 這裡應該要判斷重複但還沒判斷
				l.addAll(toBeMerged.getExcludeQuestionAltTemplates());

				survived.setExcludeQuestionAltTemplates(l);
			}

			// 處理測試紀錄 （最多處理十萬筆）
			List<EvaluationLogEntity> ele = EvaluationLogEntity.listBy(tenant.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS,
					null, toBeMerged.getKid(), null, null, null, null, null, false, null, null, null, null, 0, 100000);
			if (ele != null) {
				for (EvaluationLogEntity e : ele) {
					if (e.getCorrectAnswerId() != null && !e.getCorrectAnswerId().equals(survived.getKid())) {
						e.setCorrectAnswerId(survived.getKid());
						e.setDescription("Merged from kid[" + toBeMerged.getKid() + "] by admId [" + mergedBy.getId()
								+ "] at [" + java.util.Calendar.getInstance().getTime() + "]");
						EvaluationLogEntity.save(e);
					}
				}
			}

			SolrServer server = tenant.getCoreServer4Write();
			// 將被消滅的 QA 停用
			toBeMerged.setIsBanned(true);
			// 塞一些之後可能需要拿來查詢的欄位
			toBeMerged.setField("MERGED_TO_l", survived.getKid());
			toBeMerged.setField("MERGED_BY_i", mergedBy.getId());
			toBeMerged.setField("MERGED_AT_dt", Calendar.getInstance().getTime());

			if (!dryRun) {
				server.add(toBeMerged);
				server.add(survived);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SolrDocumentList mltCommonSense(String question, QAContext qaCtx) {
		return mltCommonSense(question, qaCtx, qaCtx.getTenant().getOtherMltQANum());
	}

	public SolrDocumentList triggeredByEventType(EventType et, QAContext qaCtx, int rows) {
		if (!initialized)
			init();
		SolrQuery sq = null;// new SolrQuery();

		try {
			MLT_COMMON_SENSE_FACTOR = 15;

			TreeSet<SolrDocument> sorted = new TreeSet<SolrDocument>(docScoreDescComparator);
			boolean first = true;

			String cleaned = "*:*";

			String theEventTypeFq = null;
			if (StringUtils.trimToNull(et.getChannel()) == null) {
				theEventTypeFq = QA.FN_TRIGGERED_BY_EVENT_TYPES + ":" + WiSeUtils.dblQuote(et.getCode()) + "";
			} else {
				theEventTypeFq = QA.FN_TRIGGERED_BY_EVENT_TYPES + ":(" + WiSeUtils.dblQuote(et.getCode()) + " "
						+ WiSeUtils.dblQuote(et.getChannel() + "::" + et.getCode()) + ")";
			}

			float factor = 1; // ENABLE_NLP ? MLT_COMMON_SENSE_FACTOR/(float)cleaned.length() : 1;
			if (!first) {
				factor *= .7f;
			} else
				first = false;

			sq = new SolrQuery();
			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:COMMON_SENSE").addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery(theEventTypeFq) // 就是這個光
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					// .addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID) // 不管是不是特殊問答
					.setParam("qf", QUERY_BOOST).setParam("d", "1").setParam("mm", "0")
					.setParam("enableElevation", true).setParam("forceElevation", true).setParam("fuzzy", true)
					.setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setRows(rows * 100);

			if (qaCtx != null) {
				if (qaCtx.getTenant().getEnableRestrictToQaCategory() && qaCtx.getRestrictToQaCategory() != null
						&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
					// 僅搜問題本身的類別&類別All(如常見問題)
					sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
							+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
				}

				if (qaCtx.getTenant().getEnableMultiLocale() && qaCtx.getLocale() != null) {
					sq.addFilterQuery("QA_LOCALE_ms:" + qaCtx.getLocale().name());
				}

				if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
					sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
				}
			}

			DictionaryDatabase[] currentKPs = KnowledgePointDictionary.search(tenant.getId(), cleaned.toCharArray(),
					qaCtx.getRestrictToQaCategory());

			if (currentKPs != null && currentKPs.length > 0 && qaCtx.getTenant().getBoost1() > 0) {
				// sq.setParam("bf", "max(product(query($qq),1000),1)");

				String qq = "QUESTION_t:(";
				for (DictionaryDatabase kp : currentKPs) {
					qq += WiSeUtils.dblQuote(kp.getKeyword());
				}

				sq.setParam("bq", qq + ")^" + (0.5 * qaCtx.getTenant().getBoost1()));
			}

			QueryResponse qr = tenant.getCoreServer().query(sq);
			SolrDocumentList currDocs = qr.getResults();

			for (int i = currDocs.size() - 1; i >= 0; i--) {
				SolrDocument thisDoc = currDocs.get(i);
				String thisQ = (String) thisDoc.getFirstValue("QUESTION_s");
				Float newScore = 870f; // Event trigger 固定score ;//(Float)thisDoc.getFirstValue("score");

				if (thisQ != null && qaCtx != null && qaCtx.getLastQA() != null
						&& (qaCtx.getForceToggleContextThisTime() != null ? qaCtx.getForceToggleContextThisTime()
								: qaCtx.getTenant().getEnableContextMatch())) {

					if (currentKPs.length == 0) {

						DictionaryDatabase[] thisKPs = KnowledgePointDictionary.search(tenant.getId(),
								thisQ.toCharArray(), qaCtx.getRestrictToQaCategory());
						boolean matchedKP = false;
						DictionaryDatabase[] lastKPs = qaCtx.getLastKPs();

						if (lastKPs != null && lastKPs.length > 0 && thisKPs.length > 0) {
							for (DictionaryDatabase kp : thisKPs) {
								if (ArrayUtils.contains(lastKPs, kp)) {
									newScore *= 1f + (0.5f * qaCtx.getTenant().getBoost2());
									matchedKP = true;
									break;
								}
							}
						}

						if (!matchedKP) {
							List<String> tags = (List<String>) thisDoc.getFieldValue("CATEGORY_ms");

							if (tags != null && tags.size() > 0) {
								List<String> ctxTags = qaCtx.getLastQA().getTags();

								if (ctxTags != null && ctxTags.size() > 0) {
									tags.retainAll(ctxTags);

									if (tags.size() > 2) {
										newScore *= (int) Math.pow(tags.size(), 2);
									}
								}
							}
						}
					}
				}
				// System.out.println(String.format("(%d). kid[%d]. score(%f=>%f)", i, thisKid,
				// newScore, newScore * factor));
				newScore *= factor;
				thisDoc.setField("score", newScore);
			}

			if (currDocs.size() > rows) {
				currDocs.removeAll(currDocs.subList(rows, currDocs.size()));
			}

			sorted.addAll(currDocs);

			SolrDocumentList docs = new SolrDocumentList();
			Set<Long> exist = new HashSet<Long>();
			while (docs.size() < rows) {
				if (sorted.size() > 0) {
					SolrDocument thisDoc = sorted.pollFirst();
					Long thisKid = (Long) thisDoc.getFirstValue("kid_l");

					if (exist.contains(thisKid))
						continue;
					else
						exist.add(thisKid);

					docs.add(thisDoc);
				} else {
					break;
				}
			}

			// AtomicInteger idx = new AtomicInteger();
			// docs.forEach(doc -> { System.out.println(String.format("*(%d). kid[%d].
			// score(%f)", idx.incrementAndGet(), doc.getFirstValue("kid_l"),
			// doc.getFirstValue("score"))); });

			return docs;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocumentList searchStandardQuestion(Tenant t, String question) {
		if (!initialized)
			init();
		SolrQuery sqry = null;
		SolrDocumentList docList = null;
		try {
			if (question == null) {
				return null;
			} else {
				SolrServer server = t.getCoreServer4Write();
				sqry = new SolrQuery();
				question = "q=QUESTION_s:*" + question + "*";
				String select = question.trim();
				String[] clauses = select.split("\n");
				for (int i = 0; i < clauses.length; i++) {
					String c = clauses[i].trim();
					if (c.length() == 0)
						continue;
					String key = c.substring(0, c.indexOf("="));
					String val = c.substring(c.indexOf("=") + 1);
					// System.out.println(String.format("%s, %s", key, val));

					if ("q".equalsIgnoreCase(key)) {
						sqry.setQuery(val);
					} else {
						sqry.add(key, val);
					}
				}
				QueryResponse resp = server.query(sqry);
				docList = resp.getResults();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return docList;
	}

	public SolrDocumentList mltCommonSense(String question, QAContext qaCtx, int rows) {
		if (!initialized)
			init();
		SolrQuery sq = null;// new SolrQuery();

		try {
			MLT_COMMON_SENSE_FACTOR = 15;
			if (question == null) {
				return null;
			} else {
				question = removeSymbols(question, qaCtx.getTenant().getLocale());
			}
			List<String> cleanAndNormalized = QAUtil.reconstructQuestion(question,
					ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, getToAnalysis());
			TreeSet<SolrDocument> sorted = new TreeSet<SolrDocument>(docScoreDescComparator);
			boolean first = true;

			String cleaned = "\"" + StringUtils.join(cleanAndNormalized, "\" \"") + "\"";

			float factor = 1;// ENABLE_NLP ? MLT_COMMON_SENSE_FACTOR/(float)cleaned.length() : 1;

			if (!first) {
				factor *= .7f;
			} else
				first = false;

			sq = new SolrQuery();
			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:COMMON_SENSE").addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					.addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID).setParam("qf", QUERY_BOOST).setParam("d", "1")
					.setParam("mm", "0").setParam("enableElevation", true).setParam("forceElevation", true)
					.setParam("fuzzy", true).setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setRows(rows * 100);
			
			boolean solrDebugExplain = (qaCtx != null
					&& qaCtx.getRequestAttribute("DEBUG", Boolean.FALSE).equals(Boolean.TRUE));

			if (solrDebugExplain) {
				sq.setParam("debug", true);
				sq.setParam("debug.explain.structured", true);
			}

			if (qaCtx.getTenant().getEnableRestrictToQaCategory() && qaCtx.getRestrictToQaCategory() != null
					&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
				// 僅搜問題本身的類別&類別All(如常見問題)
				sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
						+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
			}

			if (qaCtx.getTenant().getEnableMultiLocale() && qaCtx.getLocale() != null) {
				sq.addFilterQuery("QA_LOCALE_ms:" + qaCtx.getLocale().name());
			}

			if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
				sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
			}

			DictionaryDatabase[] currentKPs = KnowledgePointDictionary.search(tenant.getId(), cleaned.toCharArray(),
					qaCtx.getRestrictToQaCategory());

			if (currentKPs != null && currentKPs.length > 0 && qaCtx.getTenant().getBoost1() > 0) {
				String qq = "QUESTION_t:(";
				boolean isEnableRestriction = qaCtx.getTenant().getEnableRestrictToKnowledgePoint();
				StringBuilder qqForRestriction = isEnableRestriction ? new StringBuilder() : null;

				for (DictionaryDatabase kp : currentKPs) {
					String dbqKw = WiSeUtils.dblQuote(kp.getKeyword());
					qq += dbqKw;

					if (isEnableRestriction && kp.getEnableQaScopeRestriction()) {
						qqForRestriction.append(" +QUESTION_t:" + dbqKw);
					}
				}

				if (qaCtx.getTenant().getBoost1() > 0) {
					sq.setParam("bq", qq + ")^" + (0.5 * qaCtx.getTenant().getBoost1()));
				}

				if (isEnableRestriction && qqForRestriction.length() > 0) {
					sq.addFilterQuery(qqForRestriction.toString().trim());
				}
			}

			String forExp = StringUtils.left(cleaned, 200) + (cleaned.length() > 200 ? "..." : "");
			qaCtx.appendExplain(
					"MltCommonSense query of (" + cleanAndNormalized.size() + " normalized queries) [" + forExp + "]",
					sq);

			QueryResponse qr = tenant.getCoreServer().query(sq);

			SolrDocumentList currDocs = qr.getResults();
			SimpleOrderedMap<Object> explainMap = null;
			
			qaCtx.appendExplain(
					"MltCommonSense result of (" + cleanAndNormalized.size() + " normalized queries) [" + forExp + "]",
					currDocs);

			for (int i = currDocs.size() - 1; i >= 0; i--) {
				SolrDocument thisDoc = currDocs.get(i);
				String thisId = (String)thisDoc.getFirstValue("id");
				String thisQ = (String) thisDoc.getFirstValue("QUESTION_s");
				Float newScore = (Float) thisDoc.getFirstValue("score");

				if (thisQ != null && qaCtx != null && qaCtx.getLastQA() != null
						&& (qaCtx.getForceToggleContextThisTime() != null ? qaCtx.getForceToggleContextThisTime()
								: qaCtx.getTenant().getEnableContextMatch())) {

					if (currentKPs.length == 0) {
						DictionaryDatabase[] lastKPs = qaCtx.getLastKPs();
						DictionaryDatabase[] thisKPs = KnowledgePointDictionary.search(tenant.getId(),
								thisQ.toCharArray(), qaCtx.getRestrictToQaCategory());
						boolean matchedKP = false;

						if (lastKPs != null && lastKPs.length > 0 && thisKPs.length > 0) {
							for (DictionaryDatabase kp : thisKPs) {
								if (ArrayUtils.contains(lastKPs, kp)) {
									newScore *= 1f + (0.5f * qaCtx.getTenant().getBoost2());
									matchedKP = true;
									break;
								}
							}
						}

						if (!matchedKP) {
							List<String> tags = (List<String>) thisDoc.getFieldValue("CATEGORY_ms");

							if (tags != null && tags.size() > 0) {
								List<String> ctxTags = qaCtx.getLastQA().getTags();

								if (ctxTags != null && ctxTags.size() > 0) {
									tags.retainAll(ctxTags);

									if (tags.size() > 2) {
										newScore *= (int) Math.pow(tags.size(), 2);
									}
								}
							}
						}
					}
				}
				// System.out.println(String.format("(%d). kid[%d]. score(%f=>%f)", i, thisKid,
				// newScore, newScore * factor));
				newScore *= factor;
				thisDoc.setField("score", newScore);

				if (solrDebugExplain && explainMap != null) {
					SimpleOrderedMap explain = (SimpleOrderedMap) explainMap.get(thisId);
					thisDoc.setField("_explainMap", explain);
					// System.out.println("Explain of [" + thisId + "]:" + explain);
					// StringBuilder explainResult = explainer(new StringBuilder(), explain);
					// System.out.println("Result ==> " + explainResult.toString());
				}
			}

			if (currDocs.size() > rows) {
				currDocs.removeAll(currDocs.subList(rows, currDocs.size()));
			}

			sorted.addAll(currDocs);

			SolrDocumentList docs = new SolrDocumentList();
			Set<Long> exist = new HashSet<Long>();
			while (docs.size() < rows) {
				if (sorted.size() > 0) {
					SolrDocument thisDoc = sorted.pollFirst();
					Long thisKid = (Long) thisDoc.getFirstValue("kid_l");

					if (exist.contains(thisKid))
						continue;
					else
						exist.add(thisKid);

					docs.add(thisDoc);
				} else {
					break;
				}
			}

			// AtomicInteger idx = new AtomicInteger();
			// docs.forEach(doc -> { System.out.println(String.format("*(%d). kid[%d].
			// score(%f)", idx.incrementAndGet(), doc.getFirstValue("kid_l"),
			// doc.getFirstValue("score"))); });

			return docs;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public StringBuilder explainer(final StringBuilder sb, final SimpleOrderedMap som) {
		Float score = (Float) som.get("value");
		String desc = (String) som.get("description");
		boolean isMaxScoreOnly = "max of:".equals(desc);
		boolean skipChild = false;

		if (StringUtils.startsWith(desc, "weight(")) {
			String kw = parseReadableExplainDesc(desc);
			sb.append("K[" + kw + "/" + score + "]");
			skipChild = true;
		}

		List details = (List) som.get("details");

		if (!skipChild) {
			if (isMaxScoreOnly) {
				explainer(sb, getMaxScoreExplain(details));
			} else {
				sb.append(" (");
				for (int i = 0; i < details.size(); i++) {
					Object d = details.get(i);

					if (d instanceof SimpleOrderedMap) {
						if (i > 0)
							sb.append("+");
						explainer(sb, (SimpleOrderedMap) d);
					} else {
						System.out.println("Unknown detail:" + d.getClass().getName());
					}
				}
				sb.append(") ");
			}
		}
//		details.forEach(d -> System.out.println(d.getClass().getName()));

		return sb;
	}

	public Map<String, Float> explainKeywords(final Map<String, Float> map, final SimpleOrderedMap som) {
		Float score = (Float) som.get("value");
		String desc = (String) som.get("description");
		boolean isMaxScoreOnly = "max of:".equals(desc);
		boolean skipChild = false;

		if (StringUtils.startsWith(desc, "weight(")) {
			String kw = parseReadableExplainDesc(desc);

			if (map.containsKey(kw)) {
				map.put(kw, map.get(kw) + score);
			} else {
				map.put(kw, score);
			}
			skipChild = true;
		}

		List details = (List) som.get("details");

		if (!skipChild && details != null) {
			if (isMaxScoreOnly) {
				explainKeywords(map, getMaxScoreExplain(details));
			} else {
				for (int i = 0; i < details.size(); i++) {
					Object d = details.get(i);

					if (d instanceof SimpleOrderedMap) {
						explainKeywords(map, (SimpleOrderedMap) d);
					} else {
						System.out.println("Unknown detail:" + d.getClass().getName());
					}
				}
			}
		}
//		details.forEach(d -> System.out.println(d.getClass().getName()));
		return map;
	}

	private String parseReadableExplainDesc(String desc) {
		String temp = StringUtils.substringBetween(desc, ":", " in ");
		temp = StringUtils.substringBefore(temp, "^");
		temp = StringUtils.strip(temp, " \"");
		StringBuilder sb = new StringBuilder();

		String[] pieces = StringUtils.split(temp);
		char lastChar = 0;
		boolean appendSpace = false;

		for (String p : pieces) {
			if (appendSpace) {
				sb.append(' ');
				sb.append(p);
			} else {
				if (lastChar != 0 && p.charAt(0) == lastChar && p.length() > 1) {
					sb.append(p.subSequence(1, p.length()));
				} else {
					sb.append(p);
				}
			}

			lastChar = p.charAt(p.length() - 1);

			if (CharUtils.isAsciiAlpha(lastChar)) {
				appendSpace = true;
			} else {
				appendSpace = false;
			}
		}

		return sb.toString();
	}

	private SimpleOrderedMap getMaxScoreExplain(List details) {
		SimpleOrderedMap max = null;
		float maxScore = 0;

		for (Object o : details) {
			SimpleOrderedMap d = (SimpleOrderedMap) o;
			float df = ((Float) d.get("value")).floatValue();

			if (max == null || df > maxScore) {
				max = d;
				maxScore = df;
			}
		}
		return max;
	}

	/*
	 * public SolrDocumentList mltCommonSenseQuestion(String question, int rows) {
	 * SolrQuery sq = null;
	 * 
	 * try { SolrDocumentList docs = null; sq = new SolrQuery()
	 * .addFilterQuery("dataType_s:" + DATATYPE_COMMON_SENSE) .setParam("fl",
	 * "id,QUESTION_s,score") .setRequestHandler("/mlt") .setParam("stream.body",
	 * question) .setParam("mlt.fl", "QUESTION_t,QUESTION_ALT_mt")
	 * .setParam("mlt.mindf", "0") .setParam("mlt.mintf", "1") .setRows(rows);
	 * 
	 * QueryResponse qr =
	 * com.intumit.solr.SearchManager.getServer(USED_CORE).query(sq);
	 * 
	 * docs = qr.getResults();
	 * 
	 * return docs; } catch (SolrServerException e) { e.printStackTrace(); }
	 * 
	 * return null; }
	 * 
	 * public SolrDocumentList searchCommonSense(String question, QAContext qaCtx) {
	 * return searchCommonSense(question, qaCtx, false, false, false, 0, 5); }
	 * 
	 * public SolrDocumentList searchCommonSense(String question, QAContext qaCtx,
	 * boolean noReconstruct, boolean enableHomonym) { return
	 * searchCommonSense(question, qaCtx, noReconstruct, enableHomonym, false, 0,
	 * 5); }
	 */

	/**
	 * This do solr search on the common sense data, filter out the isPart_i:[2 TO
	 * *] Please don't try to use this method to search generated alts
	 * (QUESTION_ALT_ms and QUESTION_ALT_mt) bcz we don't search all the parts and
	 * you will get unexpected results.
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @param start
	 * @param rows
	 * @return
	 */
	public SolrDocumentList lowLevelSearchCommonSense(String fieldName, String fieldValue, int start, int rows) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			sq.setRequestHandler("/browse").setQuery("*:*").setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:COMMON_SENSE").addFilterQuery("-isPart_i:[2 TO *]")
					.setParam("qf", QUERY_BOOST).setParam("d", "1").setParam("mm", "0")
					.setParam("enableElevation", true).setParam("forceElevation", true).setParam("fuzzy", false)
					.setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, false).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(start).setRows(rows);

			sq.addFilterQuery(fieldName + ":" + fieldValue);

			QueryResponse qr = tenant.getCoreServer().query(sq);
			docs = qr.getResults();

			return docs;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocumentList searchCommonSense(String question, QAContext qaCtx, boolean noReconstruct,
			boolean enableHomonym, boolean allowCustomQA, int start, int rows) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			question = removeSymbols(question, qaCtx.getTenant().getLocale());
			List<String> alts = noReconstruct ? new ArrayList<String>()
					: QAUtil.reconstructQuestion(question, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx,
							getToAnalysis());
			String cleaned = noReconstruct ? question : ('"' + StringUtils.join(alts, "\" \"") + '"');
			// String cleaned = noReconstruct ? question : StringUtils.join(alts, " ");
			System.out.println("search[" + cleaned + "]");
			System.out.println(alts.size() + " alts => " + StringUtils.countMatches(cleaned, " ") + " clauses.");
			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:COMMON_SENSE").addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					.setParam("qf", enableHomonym ? QUERY_BOOST_WITH_PHONETIC_HOMONYM : QUERY_BOOST).setParam("d", "1")
					.setParam("mm", "0").setParam("enableElevation", true).setParam("forceElevation", true)
					.setParam("fuzzy", false).setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, false).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(start).setRows(rows * 100);

			if (qaCtx.getTenant().getEnableRestrictToQaCategory() && qaCtx.getRestrictToQaCategory() != null
					&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
				// 僅搜問題本身的類別&類別All(如常見問題)
				sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
						+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
			}

			if (qaCtx.getTenant().getEnableMultiLocale() && qaCtx.getLocale() != null) {
				sq.addFilterQuery("QA_LOCALE_ms:" + qaCtx.getLocale().name());
			}

			if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
				sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
			}

			if (!allowCustomQA)
				sq.addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID);

			DictionaryDatabase[] currentKPs = KnowledgePointDictionary.search(tenant.getId(), question.toCharArray(),
					qaCtx.getRestrictToQaCategory());

			if (qaCtx != null && (qaCtx.getForceToggleContextThisTime() != null ? qaCtx.getForceToggleContextThisTime()
					: qaCtx.getTenant().getEnableContextMatch()) && qaCtx.getLastQA() != null) {
				if (currentKPs.length == 0) {
					DictionaryDatabase[] lastKPs = qaCtx.getLastKPs();

					if (lastKPs != null && lastKPs.length > 0) {
						StringBuilder highWeight = new StringBuilder();

						for (DictionaryDatabase kp : lastKPs) {
							if (highWeight.length() > 0) {
								highWeight.append(" ");
							}
							highWeight.append(WiSeUtils.dblQuote(kp.getKeyword()));
						}
						sq.setParam("bf", "max(product(query($qq),10000),1)");
						sq.setParam("qq", "{!myedismax bf=}QUESTION_t:(" + highWeight.toString() + ")");
					} else {
						List<String> ctxTags = qaCtx.getLastQA().getTags();

						if (ctxTags != null & ctxTags.size() == 3) {
							sq.setParam("bf", "max(product(query($qq),10000),1)");
							sq.setParam("qq", "{!myedismax bf=}CATEGORY_ms:" + WiSeUtils.dblQuote(ctxTags.get(2))
									+ " QUESTION_s:" + WiSeUtils.dblQuote(preReconstructClean(question)));
						}
					}
				}
			} else {
				sq.setParam("boost", "max(query($qq),1)");
				sq.setParam("qq", "QUESTION_s:" + WiSeUtils.dblQuote(preReconstructClean(question)));
			}

			if (currentKPs != null && currentKPs.length > 0 && qaCtx.getTenant().getBoost1() > 0) {
				String qq = "QUESTION_t:(";
				boolean isEnableRestriction = qaCtx.getTenant().getEnableRestrictToKnowledgePoint();
				StringBuilder qqForRestriction = isEnableRestriction ? new StringBuilder() : null;

				for (DictionaryDatabase kp : currentKPs) {
					String dbqKw = WiSeUtils.dblQuote(kp.getKeyword());
					qq += dbqKw;

					if (isEnableRestriction && kp.getEnableQaScopeRestriction()) {
						qqForRestriction.append(" +QUESTION_t:" + dbqKw);
					}
				}

				if (qaCtx.getTenant().getBoost1() > 0) {
					sq.setParam("bq", qq + ")^" + (0.5 * qaCtx.getTenant().getBoost1()));
				}

				if (isEnableRestriction && qqForRestriction.length() > 0) {
					sq.addFilterQuery(qqForRestriction.toString().trim());
				}
			}

			QueryResponse qr = tenant.getCoreServer().query(sq);
			docs = qr.getResults();
			Set<Long> exist = new HashSet<Long>(docs.size());

			for (int i = docs.size() - 1; i > 0; i--) {
				SolrDocument thisDoc = docs.get(i);
				Long thisKid = (Long) thisDoc.getFirstValue("kid_l");
				if (exist.contains(thisKid))
					docs.remove(i);
				else
					exist.add(thisKid);
			}

			if (docs.size() > rows) {
				docs.removeAll(docs.subList(rows, docs.size()));
			}

			return docs;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocumentList lowLevelSearchDirectAnswer(String question, List<String> alts, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			String cleaned = '"' + StringUtils.join(alts, "\" \"") + '"';

			System.out.println("search[" + cleaned + "]");
			sq.setParam("mm", "1");

			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE)
					.addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					.addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID)
					.setParam("qf",
							qaCtx.getTenant().getEnablePhoneticHomonym()
									? "QUESTION_s^10000 QUESTION_ALT_ms^10000 QUESTION_ps^10000 QUESTION_ALT_mps^10000"
									: "QUESTION_s^10000 QUESTION_ALT_ms^10000")
					.setParam("d", "1").setParam("enableElevation", true).setParam("forceElevation", true)
					.setParam("fuzzy", false).setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

			sq.setParam("boost", "max(query($qq),1)");

			String preCQ = WiSeUtils.dblQuote(preReconstructClean(question));
			sq.setParam("qq", "QUESTION_s:" + preCQ + " " + "QUESTION_ALT_ms:" + preCQ);

			QueryResponse qr = tenant.getCoreServer().query(sq);
			docs = qr.getResults();

			return docs;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocument searchDirectAnswer(String question, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			/*
			 * String cleaned = removeSymbols(question); if (cleaned.equals(" ")){ cleaned =
			 * question; }
			 */

			List<String> alts = (List<String>) qaCtx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			String cleaned = ('"' + StringUtils.join(alts, "\" \"") + '"').replaceAll("\\.", ""); // Phrase query must
																									// remove the dot
																									// character
			System.out.println("search[" + cleaned + "]");
			sq.setParam("mm", "1");

			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE)
					.addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					.addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID)
					.setParam("qf",
							qaCtx.getTenant().getEnablePhoneticHomonym()
									? "QUESTION_s^10000 QUESTION_ALT_ms^10000 QUESTION_ps^10000 QUESTION_ALT_mps^10000"
									: "QUESTION_s^10000 QUESTION_ALT_ms^10000")
					.setParam("d", "1").setParam("enableElevation", true).setParam("forceElevation", true)
					.setParam("fuzzy", false).setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

			sq.setParam("boost", "max(query($qq),1)");

			String preCQ = WiSeUtils.dblQuote(preReconstructClean(question));
			sq.setParam("qq", "QUESTION_s:" + preCQ + " " + "QUESTION_ALT_ms:" + preCQ);

			if (qaCtx != null) {
				if (qaCtx.getTenant().getEnableRestrictToQaCategory() && qaCtx.getRestrictToQaCategory() != null
						&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
					// 僅搜問題本身的類別&類別All(如常見問題)
					sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
							+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
				}

				if (qaCtx.getTenant().getEnableMultiLocale() && qaCtx.getLocale() != null) {
					sq.addFilterQuery("QA_LOCALE_ms:" + qaCtx.getLocale().name());
				}

				if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
					sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
				}
			}

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("try DirectAnswer search", sq);
			}

			boolean solrDebugExplain = (qaCtx != null && qaCtx.getRequestAttribute("DEBUG", Boolean.FALSE).equals(Boolean.TRUE));
			QueryResponse qr = tenant.getCoreServer().query(sq);

			docs = qr.getResults();

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("DirectAnswer search result", docs);
			}

			if (docs.size() > 0) {
				SolrDocument thisDoc = docs.get(0);

				if (solrDebugExplain) {
					String thisId = (String) thisDoc.getFirstValue("id");
					Long thisKid = (Long) thisDoc.getFirstValue("kid_l");
					SimpleOrderedMap explainMap = qr.getDebugMap() != null
							? (SimpleOrderedMap) qr.getDebugMap().get("explain")
							: null;
					SimpleOrderedMap explain = (SimpleOrderedMap) explainMap.get(thisId);
					thisDoc.setField("_explainMap", explainKeywords(new HashMap(), explain));
				}
				return thisDoc;
			}

		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*public SolrDocument searchDirectAnswerByEssentialKeyword(String question, QAContext qaCtx) {
		if (!initialized) init();

		try {
			List<String> alts = (List<String>)qaCtx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			Collection<String> eks = EssentialKeywordDictionary.expand(qaCtx.getTenant().getId(), alts);
	
			return searchDirectAnswerByEssentialKeyword(eks, qaCtx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return null;
	}*/

	public SolrDocument searchDirectAnswerByEssentialKeyword(QAContext qaCtx) {
		if (!initialized)
			init();

		try {
			NlpResult nlpRes = (NlpResult) qaCtx.getRequestAttribute(QAContext.REQ_ATTR_NLP_RESULT);
			Set<String> eSentences = new HashSet<>();

			for (TreeSet<String> ts : nlpRes.eks) {
				eSentences.add(StringUtils.join(ts.iterator(), ""));
			}

			return searchDirectAnswerByEssentialKeyword(eSentences, qaCtx);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocument searchDirectAnswerByEssentialKeyword(Collection<String> eks, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			sq.setParam("mm", "1");

			if (eks.size() > 0) {
				sq.setRequestHandler("/browse").setQuery('"' + StringUtils.join(eks, "\" \"") + '"')
						.setFields(QA_RESPONSE_FIELDS).setParam("qf", QA.FN_ESSENTIAL_SENTENCES + "^10000")
						.setParam("mm", "1").setParam("fuzzy", false)
						.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
						.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
						.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).setStart(0).setRows(5);

				if (qaCtx != null && qaCtx.getTenant().getEnableRestrictToQaCategory()
						&& qaCtx.getRestrictToQaCategory() != null
						&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
					// 僅搜問題本身的類別&類別All(如常見問題)
					sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
							+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
				}

				if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
					sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
				}

				QueryResponse qr = tenant.getCoreServer().query(sq);
				docs = qr.getResults();

				if (docs.size() > 0) {
					System.out.println("Matched by Essential Sentence:" + eks);
					return docs.get(0);
				}
			}

			return null;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocument searchDirectCustomQA(String question, QAOutputTemplate outputTpl, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();
		/* 代碼會有問題 question = replaceCustumeText(question); */
		if (!qaCtx.getLastFirstBankSpecialNext().equals("")) {
			question = qaCtx.getLastQuestion();
		}
		try {
			SolrDocumentList docs = null;
			List<CustomData> completePairs = new ArrayList<CustomData>();
			List<String> reconstructed = QAUtil.reconstructQuestion(question,
					ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, getToAnalysis());
			reconstructed.add(0, question);
			List<String> alts = new ArrayList<String>(reconstructed);

			for (String tmp : reconstructed) {
				alts.addAll(CustomDataDictionary.expand(tenant.getId(), tmp.toCharArray(), completePairs));
			}

			DictionaryDatabase[] currentKPs = KnowledgePointDictionary.search(tenant.getId(), question.toCharArray(),
					qaCtx.getRestrictToQaCategory());
			qaCtx.setCurrentKPs(currentKPs);

			if (completePairs.size() == 0 && qaCtx != null && qaCtx.getLastKPs() != null
					&& (qaCtx.getForceToggleContextThisTime() != null ? qaCtx.getForceToggleContextThisTime()
							: qaCtx.getTenant().getEnableContextMatch())) {
				if (currentKPs.length == 0) {
					DictionaryDatabase[] lastKPs = qaCtx.getLastKPs();

					if (lastKPs != null && lastKPs.length > 0) {
						String newQuestion = lastKPs[0].getKeyword() + question;

						reconstructed = QAUtil.reconstructQuestion(newQuestion,
								ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, getToAnalysis());
						List<String> newAlts = new ArrayList<String>();

						for (String tmp : reconstructed) {
							newAlts.addAll(
									CustomDataDictionary.expand(tenant.getId(), tmp.toCharArray(), completePairs));
						}

						if (completePairs.size() > 0) {
							qaCtx.setCurrentKPs(lastKPs);
							alts = newAlts;
						}
					}
				}
			}

			String cleaned = alts.size() == 1 ? alts.get(0) : ('"' + StringUtils.join(alts, "\" \"") + '"');
			qaCtx.setRequestAttribute("nvPairs", completePairs);
			// String cleaned = noReconstruct ? question : StringUtils.join(alts, " ");
			if (alts.size() > 50) {
				System.out.println("****** search[" + alts.subList(0, 50) + "...], total alts : " + alts.size());
			} else {
				System.out.println("****** search[" + cleaned + "]");
			}
			System.out.println(alts.size() + " alts => " + StringUtils.countMatches(cleaned, " ") + " clauses.");

			/*
			 * if (alts.size() > QAUtil.MAX_CLAUSES) { alts =
			 * QAUtil.reconstructQuestion(question, ENABLE_NLP, true, true, true, true);
			 * cleaned = StringUtils.join(alts, " "); System.out.println("search[" + cleaned
			 * + "]"); System.out.println(alts.size() + " alts => " +
			 * StringUtils.countMatches(cleaned, " ") + " clauses."); }
			 */
			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE)
					.addFilterQuery("-" + QA.FN_BANNED + ":true")
					.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
					.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
					.addFilterQuery("ANSWER_PLUGIN_ID_s:" + CustomQA.ID)
					.setParam("qf", "QUESTION_s^10000 QUESTION_ALT_ms^10000").setParam("d", "1").setParam("mm", "1")
					.setParam("enableElevation", true).setParam("forceElevation", true).setParam("fuzzy", false)
					.setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

			sq.setParam("boost", "max(query($qq),1)");
			sq.setParam("qq", "QUESTION_s:" + WiSeUtils.dblQuote(question) + " " + "QUESTION_ALT_ms:"
					+ WiSeUtils.dblQuote(question));

			if (qaCtx != null) {
				if (tenant.getEnableRestrictToQaCategory() && qaCtx.getRestrictToQaCategory() != null
						&& !GENERAL_CATEGORIES.contains(qaCtx.getRestrictToQaCategory())) {
					// 僅搜問題本身的類別&類別All(如常見問題)
					sq.addFilterQuery("QA_CATEGORY_s:(" + qaCtx.getRestrictToQaCategory()
							+ " 全部 通用) QA_CATEGORY_s:(*:* -[\"\" TO *])");
				}

				if (tenant.getEnableMultiLocale() && qaCtx.getLocale() != null) {
					sq.addFilterQuery("QA_LOCALE_ms:" + qaCtx.getLocale().name());
				}

				if (qaCtx.getRequestAttribute("qaTagFilter") != null) {
					sq.addFilterQuery("CATEGORY_ms:*" + qaCtx.getRequestAttribute("qaTagFilter") + "*");
				}

				if (tenant.getEnableQAExplain()) {
					qaCtx.appendExplain("CustomQA search result", sq);
				}
			}

			QueryResponse qr = tenant.getCoreServer().query(sq);
			docs = qr.getResults();

			QAOutputResult output = null;
			long begin = System.currentTimeMillis();

			if (docs.size() > 0) {
				for (SolrDocument currDoc : docs) {
					if (System.currentTimeMillis() - begin > 1500) {
						if (qaCtx.getTenant().getEnableQAExplain())
							qaCtx.appendExplain("Take too long to find best QAPattern", currDoc);

						break;
					}
					String qaPatternMkey = (String) currDoc.getFirstValue(CustomQA.QA_PATTERN_KEY_INDEX_FIELD);
					QAPattern pattern = QAPattern.getByKey(qaCtx.getTenant().getId(), qaPatternMkey);
					if (pattern != null) {
						if (qaCtx.getTenant().getEnableQAExplain())
							qaCtx.appendExplain("Checking QAPattern", pattern);
						output = getOutput(qaCtx, outputTpl, completePairs, currDoc, pattern);

						if (output != null && output.isHasResult() || output.getOutput().length() > 0) {
							qaCtx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.QA_PATTERN);
							qaCtx.setCurrentQA(new QA(currDoc));

							if (pattern != null) {
								qaCtx.setRequestAttribute("bestQp", pattern);
							}
							if (qaCtx.isClientSupportHtml()) {
								output.setOutput(WiSeUtils.nl2br(output.getOutput()));
							}
							qaCtx.setAnswerText(output.getOutput());

							if (qaCtx.getTenant().getEnableQAExplain())
								qaCtx.appendExplain("Got result from this QAPattern", output);

							return currDoc;
						}
					}
				}
			} else {
				try {
					String simpleNlp = StringUtils.replaceChars(simpleNlp(this, question, true), "()", "");
					String esCoreName = QASaver.solrCoreNameToESCoreName(tenant);
					List<PercolationResult> results = searchPercolators(esCoreName, simpleNlp);
					if (qaCtx.getTenant().getEnableQAExplain())
						qaCtx.appendExplain("There is no custom qa complete result, try search percolators:", results);
					if (qaCtx.getTenant().getEnableQAExplain()) {
						qaCtx.appendExplain("Use SimpleNlp result to find uncompletePaire", simpleNlp);
					}
					System.out.println(String.format("Simple Nlp Result [%s] -> [%s]", question, simpleNlp));
					boolean autoCompleted = false;
					for (int i = 0; i < results.size(); i++) {
						PercolationResult result = results.get(i);

						if (System.currentTimeMillis() - begin > 1500) {
							break;
						}
						boolean matchAll = StringUtils.startsWith(simpleNlp, result.getPrefix())
								&& StringUtils.endsWith(simpleNlp, result.getPostfix());
						try {
							List<String> tpls = new ArrayList<String>(
									Arrays.asList(new String[] { result.getOriginalAlt() }));
							// List<NameValuePair> uncompleteNVPairs =
							// QADataAggregator.findUncompleteNVPairs(tpls,
							// reconstructed.size() > 1 &&
							// !reconstructed.get(1).contains("srbt") ?
							// reconstructed.get(1) : question, completePairs);
							List<NameValuePair> uncompleteNVPairs = QADataAggregator.findUncompleteNVPairs(tpls,
									simpleNlp, completePairs);

							if (uncompleteNVPairs.size() > 0)
								qaCtx.setRequestAttribute("uncompleteNVPairs", uncompleteNVPairs);
							else if (completePairs.isEmpty() && uncompleteNVPairs.isEmpty())
								continue;

							if (result == null || result.getKid() == null)
								continue;

							SolrDocument currDoc = getMainQASolrDocument(result.getKid());

							if (currDoc == null)
								continue;

							if (currDoc.getFieldValues("CATEGORY_ms") != null) {
								String categoryMS = Arrays.toString(currDoc.getFieldValues("CATEGORY_ms").toArray());
								qaCtx.appendExplain("QA category is " + categoryMS + ", your tag is : ",
										qaCtx.getRequestAttribute("qaTagFilter"));
								if (qaCtx.getRequestAttribute("qaTagFilter") != null && categoryMS != null
										&& !categoryMS.contains(qaCtx.getRequestAttribute("qaTagFilter").toString())) {
									continue;
								}
							} else if (currDoc.getFieldValues("CATEGORY_ms") == null
									&& qaCtx.getRequestAttribute("qaTagFilter") != null) {
								qaCtx.appendExplain("QA category is null, your tag is : ",
										qaCtx.getRequestAttribute("qaTagFilter"));
								continue;
							}

							currDoc.setField(QA.FN_QUESTION_ALT_TPL,
									Arrays.asList(new String[] { result.getOriginalAlt() }));
							String qaPatternMkey = (String) currDoc.getFirstValue(CustomQA.QA_PATTERN_KEY_INDEX_FIELD);
							QAPattern pattern = QAPattern.getByKey(qaCtx.getTenant().getId(), qaPatternMkey);

							if (pattern == null) {
								if (qaCtx.getTenant().getEnableQAExplain()) {
									qaCtx.appendExplain("No QAPattern for mkey[" + qaPatternMkey + "]", currDoc);
								}
								continue;
							} else {
								if (qaCtx.getTenant().getEnableQAExplain())
									qaCtx.appendExplain("Checking QAPattern", pattern);
							}
							output = getOutput(qaCtx, outputTpl, completePairs, currDoc, pattern);
							autoCompleted |= output.isAutoCompleted();

							// 這邊得判斷還需要一些優化，待有想到再說
							System.out.println(String.format("PercolationResult : %s", result.toString()));
							System.out.println(
									String.format("current [%d], total [%d], autoCompleted [%b], matchAll [%b]", i + 1,
											results.size(), autoCompleted, matchAll));
							if ((output != null && output.isHasResult())
									|| (i == results.size() - 1 && (autoCompleted || matchAll))) {
								qaCtx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.QA_PATTERN);
								qaCtx.setCurrentQA(new QA(currDoc));

								if (pattern != null) {
									qaCtx.setRequestAttribute("bestQp", pattern);
								}
								if (qaCtx.isClientSupportHtml()) {
									output.setOutput(WiSeUtils.nl2br(output.getOutput()));
								}
								qaCtx.setAnswerText(output.getOutput());

								if (qaCtx.getTenant().getEnableQAExplain())
									qaCtx.appendExplain("Got result from this QAPattern", output);

								return currDoc;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (IndexMissingException ignoreIt) {
				}
			}

			return null;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public SolrDocument searchDialogUtterance(String question, String dialogName, QAContext qaCtx) {
		if (!initialized) init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			List<String> alts = (List<String>)qaCtx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			String cleaned = ('"' + StringUtils.join(alts, "\" \"") + '"'); // Phrase query must remove the dot character
			cleaned = cleaned + " " + cleaned.replaceAll("\\.", "");
			System.out.println("search utterance[" + cleaned + "]");
			sq.setParam("mm", "1");

			sq
				.setRequestHandler("/browse")
				.setQuery(cleaned)
				.setFields(QA_RESPONSE_FIELDS)
				.addFilterQuery("dataType_s:" + QADialog.UTTERANCE_INDEX_DATATYPE)
				.setParam("qf", qaCtx.getTenant().getEnablePhoneticHomonym() 
								? "QUESTION_s^1000 QUESTION_ALT_ms^1000 QUESTION_ps^100 QUESTION_ALT_mps^100 QUESTION_t^0.2 QUESTION_ALT_mt^0.1"
								: "QUESTION_s^1000 QUESTION_ALT_ms^1000 QUESTION_t^0.2 QUESTION_ALT_mt^0.1"
								)
                .setParam("d", "1")
				.setParam("enableElevation", true)
				.setParam("forceElevation", true)
				.setParam("fuzzy", true)
				.setTimeAllowed(QUERY_TIME_ALLOWED)
				.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
				.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
				.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true)
				.addSort("score", SolrQuery.ORDER.desc)
				.setStart(0)
				.setRows(5);

			sq.setParam("boost", "max(query($qq),1)");

			String preCQ = WiSeUtils.dblQuote(preReconstructClean(question));
			sq.setParam("qq", "QUESTION_s:" + preCQ + " " + "QUESTION_ALT_ms:" + preCQ + (dialogName != null ? " DialogName_s:" + dialogName : ""));

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("try DirectDialogUtterance search", sq);
			}

			QueryResponse qr = tenant.getCoreServer().query(sq);

			docs = qr.getResults();

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("Scoring DialogUtterance search result", docs);
			}

			if (docs.size() > 0 && docs.getMaxScore() > 1000) {
				return docs.get(0);
			}

			return null;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public SolrDocument searchDirectDialogUtterance(String question, String dialogName, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrQuery sq = new SolrQuery();

		try {
			SolrDocumentList docs = null;
			List<String> alts = (List<String>) qaCtx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			String cleaned = '"' + StringUtils.join(alts, "\" \"") + '"';
			System.out.println("search utterance[" + cleaned + "]");
			sq.setParam("mm", "1");

			sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
					.addFilterQuery("dataType_s:" + QADialog.UTTERANCE_INDEX_DATATYPE)
					.setParam("qf",
							qaCtx.getTenant().getEnablePhoneticHomonym()
									? "QUESTION_s^10000 QUESTION_ALT_ms^10000 QUESTION_ps^10000 QUESTION_ALT_mps^10000"
									: "QUESTION_s^10000 QUESTION_ALT_ms^10000")
					.setParam("d", "1").setParam("enableElevation", true).setParam("forceElevation", true)
					.setParam("fuzzy", false).setTimeAllowed(QUERY_TIME_ALLOWED)
					.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
					.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
					.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
					.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

			sq.setParam("boost", "max(query($qq),1)");

			String preCQ = WiSeUtils.dblQuote(preReconstructClean(question));
			sq.setParam("qq", "QUESTION_s:" + preCQ + " " + "QUESTION_ALT_ms:" + preCQ
					+ (dialogName != null ? " DialogName_s:" + dialogName : ""));

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("try DirectDialogUtterance search", sq);
			}

			QueryResponse qr = tenant.getCoreServer().query(sq);

			docs = qr.getResults();

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain("DirectDialogUtterance search result", docs);
			}

			if (docs.size() > 0) {
				return docs.get(0);
			}

			return null;
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}

	private QAOutputResult getOutput(QAContext qaCtx, QAOutputTemplate outputTpl, List<CustomData> nvPairs,
			SolrDocument customDoc, QAPattern qp) {
		try {
			Class aggregatorClazz = QADataAggregator.findRegisteredClass(qp.getDataAggregator());
			QADataAggregator aggregator = (QADataAggregator) aggregatorClazz.newInstance();

			// out.println("Query Debug:" + java.net.URLDecoder.decode(myQuery.toString(),
			// "UTF-8"));
			// out.println("<HR>");

			QA customQA = new QA(customDoc);

			QAOutputResult output = outputTpl.output(customQA, qaCtx, qp, nvPairs, aggregator);

			return output;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SolrDocument searchDirectCasual(String question, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrServer server = getCasualServer(qaCtx.getTenant(), false);

		if (server != null) {
			try {
				SolrQuery sq = new SolrQuery();
				SolrDocumentList docs = null;
				List<String> alts = QAUtil.reconstructForCasual(qaCtx, question);
				/*
				 * String robotName = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(),
				 * "ROBOT_NAME", qaCtx.getMessage("robot.name")); question =
				 * StringUtils.replace(question, robotName,
				 * CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.ROBOTNAME));
				 * question = StringUtils.lowerCase(removeSymbols(question));
				 * 
				 * // 拿掉語助詞 if (StringUtils.endsWithAny(question,
				 * AUXILIARY_STRING_ENDING_ARRAY)) { String c1 =
				 * ClientUtils.escapeQueryChars(question.substring(0, question.length() - 1));
				 * String c2 = replaceCasualText(c1); if (!StringUtils.equals(question, c1))
				 * alts.add(c1);
				 * 
				 * if (!StringUtils.equals(c1, c2)) alts.add(c2); } alts.add(0,
				 * ClientUtils.escapeQueryChars(question));
				 */

				String cleaned = '"' + StringUtils.join(alts, "\" \"") + '"';
				// String cleaned = noReconstruct ? question : StringUtils.join(alts, " ");
				System.out.println("casual search[" + cleaned + "]");
				System.out.println(alts.size() + " alts => " + StringUtils.countMatches(cleaned, " ") + " clauses.");

				sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
						.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_CASUAL)
						.addFilterQuery("-" + QA.FN_BANNED + ":true")
						.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
						.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
						.setParam("qf", "QUESTION_s^10000 QUESTION_ALT_TPL_ms^10000 QUESTION_ALT_ms^10000")
						.setParam("d", "1").setParam("mm", "1").setParam("enableElevation", true)
						.setParam("forceElevation", true).setParam("fuzzy", false).setTimeAllowed(QUERY_TIME_ALLOWED)
						.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
						.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
						.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
						.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

				sq.setParam("boost", "max(query($qq),1)");
				sq.setParam("qq", "QUESTION_s:" + WiSeUtils.dblQuote(question) + " " + "QUESTION_ALT_ms:"
						+ WiSeUtils.dblQuote(question));

				QueryResponse qr = server.query(sq);

				docs = qr.getResults();

				if (docs.size() > 0) {
					return docs.get(0);
				}

				return null;
			} catch (SolrServerException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public boolean shouldTryingToFindAnswer(String question, String[][] postTags, SolrDocumentList searched) {

		Set<String> dontSearchSet = DictionaryDatabase.getDictionary(tenant.getId(),
				DictionaryDatabase.Purpose.DONT_SEARCH);
		Set<String> forceSearchSet = DictionaryDatabase.getDictionary(tenant.getId(),
				DictionaryDatabase.Purpose.SEARCH);

		if (dontSearchSet.contains(question))
			return true;
		if (question.length() <= 1)
			return true;

		if (!forceSearchSet.contains(question)) {
			if (searched != null && searched.size() == 0)
				return true;
			if (postTags != null && postTags[0].length != 1)
				return true;
		}
		return false;
	}

	public SolrDocumentList mltCasual(String question, QAContext qaCtx) {
		if (!initialized)
			init();
		SolrServer server = getCasualServer(qaCtx.getTenant(), false);

		if (server != null) {
			try {
				SolrQuery sq = null;// new SolrQuery();
				SolrDocumentList docs = null;
				// String cleaned = QAUtil.simpleClean(question);
				List<String> alts = QAUtil.reconstructQuestion(question,
						ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, getToAnalysis());
				String cleaned = StringUtils.join(alts, " ");
				sq = new SolrQuery();
				sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
						.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_CASUAL)
						.addFilterQuery("-" + QA.FN_BANNED + ":true")
						.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
						.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])").setParam("qf", QUERY_BOOST)
						.setParam("d", "1").setParam("mm", "100%").setParam("enableElevation", true)
						.setParam("forceElevation", true).setParam("fuzzy", true).setTimeAllowed(QUERY_TIME_ALLOWED)
						.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
						.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
						.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
						.addSort("kid_l", SolrQuery.ORDER.asc).setStart(0).setRows(5);

				QueryResponse qr = server.query(sq);

				docs = qr.getResults();

				return docs;
			} catch (SolrServerException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public SolrDocument searchCasual(String question, QAContext qaCtx, Float threshold) {
		if (!initialized)
			init();
		SolrServer server = getCasualServer(qaCtx.getTenant(), false);

		if (server != null) {
			try {
				SolrQuery sq = new SolrQuery();
				SolrDocumentList docs = null;
				// String cleaned = QAUtil.simpleClean(question);
				List<String> alts = QAUtil.reconstructQuestion(question,
						ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, getToAnalysis());
				// String cleaned = StringUtils.join(alts, " ");
				String cleaned = '"' + StringUtils.join(alts, "\" \"") + '"';

				sq.setRequestHandler("/browse").setQuery(cleaned).setFields(QA_RESPONSE_FIELDS)
						.setParam("qf", QUERY_BOOST_CASUAL).addFilterQuery("dataType_s:" + QAUtil.DATATYPE_CASUAL)
						.addFilterQuery("-" + QA.FN_BANNED + ":true")
						.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
						.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])").setParam("mm", "0")
						.setParam("fuzzy", true).setTimeAllowed(QUERY_TIME_ALLOWED)
						.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + tenant.getId())
						.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
						.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true).addSort("score", SolrQuery.ORDER.desc)
						.addSort("kid_l", SolrQuery.ORDER.asc).setRows(5);
				{
					// Boost if question exactly match QUESTION_s
					sq.setParam("boost", "query($qq)");
					sq.setParam("qq", "QUESTION_s:" + WiSeUtils.dblQuote(question) + " QUESTION_ALT_ms:"
							+ WiSeUtils.dblQuote(question));
				}

				QueryResponse qr = server.query(sq);

				docs = qr.getResults();

				if (docs.getNumFound() > 0 && (threshold == null || docs.getMaxScore() >= threshold)) {
					return docs.get(0);
				}
			} catch (SolrServerException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static List<PercolationResult> searchPercolators(String esCoreName, String question) {
		List<PercolationResult> results = new ArrayList<PercolationResult>();
		Client esCli = SearchManager.getESClient();

		try {
			// Percolate a doc first to ensure Elasticsearch add field mapping automatically
			// Build a document to check against the percolator
			XContentBuilder docBuilder = jsonBuilder().startObject();
			docBuilder.field("doc").startObject(); // This is needed to designate the document
			docBuilder.field("terms", question);
			docBuilder.endObject(); // End of the doc field
			docBuilder.endObject(); // End of the JSON root object
			// Percolate
			PercolateResponse response = esCli.preparePercolate().setIndices(esCoreName).setDocumentType("alts")
					.setSource(docBuilder).execute().actionGet();

			// Iterate over the results
			for (PercolateResponse.Match match : response) {
				// Handle the result which is the name of
				// the query in the percolator
				// System.out.println(match.getId());
				GetResponse getRsp = esCli.prepareGet(esCoreName, ".percolator", match.getId().toString())
						.setFields("originalAlt", "kid", "prefix", "postfix").execute().actionGet();

				if (getRsp == null || getRsp.getFields() == null || getRsp.getFields().isEmpty())
					continue; // Cause NPE

				getRsp.getFields().forEach((key, val) -> {
					System.out
							.print("Elasticsearched: (" + key + "," + val.getName() + "::" + val.getValues() + ") / ");
				});
				System.out.println();
				String alt = getRsp.getField("originalAlt") != null ? (String) getRsp.getField("originalAlt").getValue()
						: null;
				Long kid = getRsp.getField("kid") != null ? (Long) getRsp.getField("kid").getValue() : null;
				String prefix = getRsp.getField("prefix") != null ? (String) getRsp.getField("prefix").getValue() : "";
				String postfix = getRsp.getField("postfix") != null ? (String) getRsp.getField("postfix").getValue()
						: "";

				PercolationResult r = new PercolationResult(match.getId().toString(), alt, kid, prefix, postfix);

				if (alt != null && !results.contains(r)) {
					results.add(r);
				}
			}
		} catch (IndexMissingException ignoreIt) {
			System.out.println("core [" + esCoreName + "] has no ES index");
		} catch (NullPointerException e1) {
			System.out.println("NPE of getRsp.getField: " + e1.getMessage());
		} catch (ElasticsearchException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return results;
	}

	public Analysis getToAnalysis() {
		if (!initialized)
			init();
		Analysis a = new ToAnalysis();
		a.setForests(userDefinedForest);
		a.setAmbiguityForest(ambiguityForest);
		return a;
	}

	public Tokenizer getJpTokenizer() {
		if (!initialized)
			init();
		return jpTokenizer;
	}

	public Forest getAmbiguityForest() {
		return ambiguityForest;
	}

	public Forest getUserDefinedForest() {
		if(!initialized) {
			init();
		}
		return userDefinedForest;
	}

	public List<NameValuePair> search(String test) {
		if (!initialized)
			init();
		String preparedText = TextUtil.preprocessText(test);
		Iterator<SearchResult> itr = aho.search(preparedText.toCharArray());
		List<NameValuePair> res = new ArrayList<NameValuePair>();

		while (itr.hasNext()) {
			SearchResult r = itr.next();

			int[] outputs = r.getOutputs();

			for (int output : outputs) {
				NameValuePair nv = outputMap.get(output);
				res.add(nv);
			}
		}
		return res;
	}

	public static List<String> parseMultiValue(String str) {
		List<String> vals = new ArrayList<String>();

		if (StringUtils.isNotBlank(str)) {
			String[] toks = str.split("\n");
			for (String t : toks) {
				String val = StringUtils.trim(t);
				if (StringUtils.isNotBlank(val)) {
					vals.add(val);
				}
			}
		}
		return vals.isEmpty() ? null : vals;
	}

	/**
	 * @param question
	 * @param param    TODO
	 * @return
	 */
	public static List<String> reconstructQuestion(String question, ReconstructQuestionParameter param, QAContext qaCtx,
			Analysis analysis) {
		List<String> results = new ArrayList<String>();
		MyStaticValue.isRealName = true;

		if (param != null && param == ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM) {
			System.out.println("Before reconstructQuestion:[" + question + "]");
		}
		if (qaCtx == null || qaCtx.getTenant() == null) {
			results.add(question);
			return results;
		}

		Tenant t = qaCtx.getTenant();
		NlpResult nlpResult = QAUtil.nlp(t, QAUtil.getInstance(qaCtx.getTenant()), question, qaCtx.getLocale().getLocale(), ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_SEGMENT_2);
		long estimated = QASaver.estimateAltCount(qaCtx.getTenant().getId(), Arrays.asList(new String[] {nlpResult.getSegResult()}), false);

		if (param != null && param == ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM) {
			System.out.println("Estimated reconstructed question alt count:[" + estimated + "] <= [" + nlpResult + "]");
		}
		if (estimated > MAX_CLAUSES) {
			nlpResult = QAUtil.nlp(t, QAUtil.getInstance(qaCtx.getTenant()), question, qaCtx.getLocale().getLocale(),
					ReconstructQuestionParameter.LONG_LENGTH_USER_INPUT_PARAM,
					TemperalAndNumberStrategy.PREBUILD_STRATEGY_SEGMENT_1);
			estimated = QASaver.estimateAltCount(qaCtx.getTenant().getId(), Arrays.asList(new String[] {nlpResult.getSegResult()}), false);
//			System.out.println("Estimated long length question reconstructed alt count:[" + estimated + "] <= [" + nlpResult + "]");
		}

		qaCtx.setRequestAttribute(QAContext.REQ_ATTR_NLP_RESULT, nlpResult);
		BloomFilter<CharSequence> dupCheck = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 1000,
				0.00001);

		String alt = TemplateUtil.process(nlpResult.getSegResult(param.insertStopwordBetweenKeywords), TemplateUtil.CUSTOM_QA_REPLACER);
		if (StringUtils.indexOf(alt, "[") == -1 && StringUtils.indexOf(alt, "]") == -1) {
			recursiveExpand(t, qaCtx, alt, param, 1, results, dupCheck, alt);
		}

		String insertQuestion = (param.simpleClean ? simpleClean(question, qaCtx != null ? qaCtx.getTenant() : null)
				: question);
		if (param.checkDup && dupCheck.put(insertQuestion)) {
			results.remove(insertQuestion);
		}
		results.add(0, insertQuestion);
		return results;
	}

	static void recursiveExpand(Tenant tenant, QAContext ctx, String alt, ReconstructQuestionParameter param, int level,
			List<String> allSet, BloomFilter<CharSequence> dupCheck, String firstLvAlt) {
		if (alt == null)
			return;

		Matcher m = QAUtil.expandBlock.matcher(alt);

		if (m.find()) {
			int start = m.start();
			int end = m.end();

			String before = StringUtils.substring(alt, 0, start);
			String after = StringUtils.substring(alt, end);
			String inners = m.group(1);

			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			List<String> newInnerList = new ArrayList<>();
			boolean dirty = false;

			for (String inner : innerArr) {
				if (StringUtils.startsWith(inner, "#")) {
					System.out.print("Try to find entity [" + inner + "]... ");
					dirty = true;
					QAEntity entity = QAEntity.get(tenant.getId(), inner.substring(1), null, null);

					if (entity != null) {
						System.out.println("Found entity, now expand innerArr...");
						String[] allValues = StringUtils.split(entity.getEntityValues(), "|");
						newInnerList.addAll(Arrays.asList(allValues));
					} else {
						System.out.println("Cannot found the entity [" + innerArr + "] skip it.");
					}
				} else if (StringUtils.startsWith(inner, "@")) {
					System.out.print("Try to find intent [" + inner + "]... ");
					dirty = true;
					QAIntent intent = QAIntent.get(tenant.getId(), inner.substring(1), null);

					if (intent != null) {
						System.out.println("Found intent, now expand innerArr...");
						String[] allValues = StringUtils.split(intent.getKeywords(), ",");
						newInnerList.addAll(Arrays.asList(allValues));
					} else {
						System.out.println("Cannot found the intent [" + innerArr + "] skip it.");
					}
				} else {
					newInnerList.add(inner);
				}

				// 超過上限就不再繼續加入其他同一個 block 內的其他詞，但還是要繼續執行後面的 recursiveExpand
				if (allSet.size() > MAX_CLAUSES) {
					break;
				}
			}

			if (dirty) {
				innerArr = newInnerList.toArray(new String[0]);
			}

			for (String inner : innerArr) {
				String newAlt = QASaver.appendSpaceIfAppendLetterToLetter(before, inner, after);
				if (QAUtil.expandBlock.matcher(newAlt).find()) {
					recursiveExpand(tenant, ctx, StringUtils.trimToEmpty(newAlt), param, level + 1, allSet, dupCheck,
							firstLvAlt);
				} else {
					if (!param.checkDup || dupCheck.put(newAlt)) {
						allSet.add(newAlt);
					}
				}
			}
		} else {
			if (!param.checkDup || dupCheck.put(alt)) {
				allSet.add(alt);
			}
		}
	}

	/**
	 * @param question
	 * @param param    TODO
	 * @return
	 */
	public static List<String> reconstructQuestion_OLD(String question, ReconstructQuestionParameter param,
			QAContext qaCtx, Analysis analysis) {
		boolean checkDup = param.checkDup;
		List<String> results = new ArrayList<String>();
		MyStaticValue.isRealName = true;

		System.out.println("Before reconstructQuestion:[" + question + "]");
		boolean skipNlp = false;
		if (qaCtx != null && qaCtx.getTenant().getEnableMultiLocale()) {
			if (qaCtx.getLocale() != null && HitHotLocale.zh_TW != qaCtx.getLocale()) {
				skipNlp = true;
			}
		}

		if (param.nlp) { // 目前暫時先不 skipNlp，因為先用翻譯處理，等不用翻譯的時候就要這段 && !skipNlp) {

			List<org.ansj.domain.Term> parse = null;
			Tenant tenant = qaCtx != null ? qaCtx.getTenant() : null;

			// 為了 ja_JP 各種 hack... 理想上應該每一種語言有一個專門 nlp class 這樣比較好...
			if (tenant != null && tenant.getLocale() == HitHotLocale.ja_JP) {
				Tokenizer tokenizer = new Tokenizer.Builder().build();
				parse = new ArrayList<org.ansj.domain.Term>();
				int offe = 0;

				for (Token token : tokenizer.tokenize(question)) {
					String kw = token.getBaseForm();
					if (StringUtils.trimToNull(kw) == null || "*".equals(kw)) {
						kw = token.getSurface();
					}
					System.out.println(kw + "\t" + token.getAllFeatures());
					org.ansj.domain.Term t = new org.ansj.domain.Term(kw, offe++,
							KuromojiUtil.kuromojiTagMapping(token.getAllFeaturesArray()), 1000);
					parse.add(t);
				}
			} else {

				if (analysis != null) {
					try {
						parse = analysis.parseStr(preReconstructClean(" " + question + " ")).getTerms();
					} catch (Exception e) {
						System.out.println("Cannot analysis.doParse(" + question + ")... preRC("
								+ preReconstructClean(question) + ")");
						e.printStackTrace();
					}
				} else {
					if (USE_NLPANALYSIS) {
						parse = org.ansj.splitWord.analysis.NlpAnalysis.parse(preReconstructClean(" " + question + " "))
								.getTerms();
					} else {
						parse = org.ansj.splitWord.analysis.ToAnalysis.parse(preReconstructClean(" " + question + " "))
								.getTerms();
					}
				}
			}

			// parse = FilterModifWord.modifResult(parse);
			boolean chaosMode = param.pChaosMode;
			StringBuilder notNM = new StringBuilder(); // 沒有正規化的句子
			StringBuilder NM = new StringBuilder(); // 正規化的句子
			List<StringBuilder> alts = new ArrayList<StringBuilder>();

			if (chaosMode) {
				alts.add(new StringBuilder());
			}

			boolean allEnglish = true;
			boolean inEnMode = false;
			String englishPhraseStack = "";
			List<String> queue = new ArrayList<String>();
			// List<String> fixedSynonyms = new ArrayList<String>();

			for (int i = 0; i < parse.size(); i++) {
				org.ansj.domain.Term t = parse.get(i);

				if ((i == 0 || i + 1 == parse.size()) && StringUtils.isBlank(t.getName()) && inEnMode == false)
					continue;

				String nature = t.getNatureStr();

				if (StringUtils.startsWithAny(nature, new String[] { "en" })) {
					if (inEnMode) {
						englishPhraseStack += " " + t.getRealName();
					} else {
						inEnMode = true;
						englishPhraseStack = t.getRealName();
					}
					if (i < parse.size() - 1)
						continue;
				} else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
					// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
					if (i < parse.size() - 1)
						continue;
				} else {
					allEnglish = false;

					if (inEnMode) {
						queue.add(englishPhraseStack);
						inEnMode = false;
					}
				}

				if (!inEnMode
						&& (nature != null && !"null".equals(nature)
								&& StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))
						&& (tenant == null || !tenant.isForceIgnoreCharacter(t.getRealName()))) {
					queue.add(t.getRealName());
				}
				if (inEnMode && !(i < parse.size() - 1)) {
					queue.add(englishPhraseStack);
				}

				while (queue.size() > 0) {
					String kw = queue.remove(0);
					appendSpaceIfAppendLetterToLetter(notNM, kw);

					if (param.normalizeSynonym) { // 進行同義詞正規化動作
						List<String> synKws = new ArrayList<String>();
						boolean exactlyMatch = false;

						if (StringUtils.startsWithAny(nature, new String[] { "m", "t" })) {
							if (StringUtils.startsWith(nature, "m")
									&& StringUtils.endsWithAny(t.getRealName(), TEMPORAL_STR_ENDING)) {
								t.setNature(new Nature("t"));
								nature = "t";
							}

							if (qaCtx != null) {
								if (StringUtils.startsWith(nature, "mt")) {
									CategorizedKeyValuePair kvNum = new CategorizedKeyValuePair(
											CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER),
											t.getRealName(), CategorizedKeyValuePair.Type.NUMBER);
									synKws.add(kvNum.getKey());
									CategorizedKeyValuePair kvDate = new CategorizedKeyValuePair(
											CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL),
											t.getRealName(), CategorizedKeyValuePair.Type.TEMPORAL);
									synKws.add(kvDate.getKey());

									String convertable = CategorizedValueParser.toNumberRangeQuery(kvNum);

									if (convertable != null) {
										qaCtx.addCategorizedKeyValuePair(kvNum);
									} else {
										convertable = CategorizedValueParser.toDateRangeQuery(kvDate);

										if (convertable != null) {
											qaCtx.addCategorizedKeyValuePair(kvDate);
										} else {
											nature = "mt?";
										}
									}
								} else if (StringUtils.startsWith(nature, "t") || (StringUtils.startsWith(nature, "m")
										&& StringUtils.endsWithAny(t.getRealName(), QAUtil.TEMPORAL_STR_ENDING))) {
									CategorizedKeyValuePair kv = new CategorizedKeyValuePair(
											CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL),
											t.getRealName(), CategorizedKeyValuePair.Type.TEMPORAL);
									synKws.add(kv.getKey());

									String convertable = CategorizedValueParser.toDateRangeQuery(kv);

									if (convertable != null) {
										qaCtx.addCategorizedKeyValuePair(kv);
									} else {
										nature = "t?";
									}
								} else if (StringUtils.startsWith(nature, "m")
										&& !NOT_A_NUMBER_STR_SET.contains(t.getRealName())) {
									CategorizedKeyValuePair kv = new CategorizedKeyValuePair(
											CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER),
											t.getRealName(), CategorizedKeyValuePair.Type.NUMBER);
									synKws.add(kv.getKey());

									String convertable = CategorizedValueParser.toNumberRangeQuery(kv);

									if (convertable != null) {
										qaCtx.addCategorizedKeyValuePair(kv);
									} else {
										nature = "m?";
									}
								}
							}
						} else {
							if (qaCtx != null) {
								List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance()
										.listByQuery(qaCtx.getTenant().getId(), kw);

								if (param.normalizeSynonymWithHighestPriority) {
									// 若本身就是同義詞的頭，就用本身，若不是則只挑 priority 最高的加入，若平手找第一個。
									String toBeAdd = null;
									exactlyMatch = true;
									int highestPriority = 0;

									for (SynonymKeyword sk : synonymsAll) {
										if (kw.equalsIgnoreCase(sk.getKeyword())) {
											toBeAdd = sk.getKeyword();
											break;
										} else {
											if (sk.getPriority() > highestPriority) {
												toBeAdd = sk.getKeyword();
												highestPriority = sk.getPriority();
											}
										}
									}

									if (toBeAdd != null) {
										synKws.add(toBeAdd);
									}
								} else {
									for (SynonymKeyword sk : synonymsAll) {
										if (kw.equalsIgnoreCase(sk.getKeyword())) {
											exactlyMatch = true;
										} else {
											synKws.add(sk.getKeyword());
										}
									}
								}
							}
						}

						System.out.println(kw + "=>" + synKws);
						List<StringBuilder> newAlts = new ArrayList<StringBuilder>(); // 用來存放「目前這個詞」所產生的新句子（不直接加入 alts
																						// 是因為我們本來就是要 loop alts 來
																						// concate new keyword）

						if (!chaosMode) {
							// 沒有同義詞，或者目前斷出來的詞只有一組同義詞，且不是同義詞的 keyword 那一欄 (所以需要正規化)
							if (synKws.size() == 0) {
								appendSpaceIfAppendLetterToLetter(NM, kw);
							} else if (exactlyMatch && synKws.size() == 1) {
								appendSpaceIfAppendLetterToLetter(NM, synKws.get(0));
							} else {
								// 不只一個同義詞，進入渾沌模式
								chaosMode = true;
								alts.add(new StringBuilder(NM.toString()));
							}
						}

						if (chaosMode) {
							for (StringBuilder currQ : alts) {
								String first = kw; // 只是單純的為了下面這個 for 能夠第一次先以 kw 來跑

								if (alts.size() <= MAX_CLAUSES) { // 太多就放棄
									for (String synKw : synKws) {
										if (first == null) { // 這有可能是 null 嗎？失憶中...
											first = synKw;
										} else {
											StringBuilder newQ = new StringBuilder(currQ.toString());
											appendSpaceIfAppendLetterToLetter(newQ, synKw);
											newAlts.add(newQ);
										}
									}
								}

								// currQ 已經被 clone 用來串同義詞了，最後用來串 kw 本身
								appendSpaceIfAppendLetterToLetter(currQ, first);
							}

							alts.addAll(newAlts);
						}
					}
				}
			}
			if (!chaosMode) {
				alts.add(0, NM);
			}
			alts.add(0, notNM);

			if (param.debug) {
				System.out.println("reconstructQuestionByNlp:" + alts.size() + " alts.");
				System.out.println("ANSJ:" + parse);
				System.out.println("new Question:" + alts);
			}

			if (alts.size() > MAX_CLAUSES) {
				if (param.pChaosMode && param.allowAutoDisableChaosMode) {
					// analysis.resetContent();
					return reconstructQuestion(question,
							new ReconstructQuestionParameter(param.nlp, param.normalizeSynonym, checkDup,
									param.simpleClean, false, param.allowAutoDisableChaosMode, param.debug, true),
							qaCtx, analysis);
				}
				checkDup = true;
			}

			for (StringBuilder alt : alts) {
				String cleaned = param.simpleClean
						? simpleClean(alt.toString(), qaCtx != null ? qaCtx.getTenant() : null)
						: alt.toString();

				if (StringUtils.isNotEmpty(cleaned) && (!checkDup || !results.contains(cleaned))) {
					results.add(cleaned);

					if (results.size() >= MAX_CLAUSES)
						break;
				}
			}
		}

		String insertQuestion = (param.simpleClean ? simpleClean(question, qaCtx != null ? qaCtx.getTenant() : null)
				: question);
		if (checkDup && results.contains(insertQuestion)) {
			results.remove(insertQuestion);
		}
		results.add(0, insertQuestion);

		return results;
	}

	public static List<String> reconstructForCasual(QAContext ctx, String question) {
		String orig = question;
		List<String> candidates = new ArrayList<String>();
		candidates.add(orig);

		if (ctx != null) {
			// 直接把機器人名稱從問句當中換成「你」，例如「小智是誰」直接替換為「你是誰」。這樣簡化生活問法維護
			Tenant tenant = ctx.getTenant();
			String robotName = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(), "ROBOT_NAME",
					ctx.getMessage("robot.name"));

			if (ctx.getLocale() != null) {
				if (ctx.getLocale() == HitHotLocale.zh_TW) {
					question = StringUtils.replace(question, robotName, "你");
				} else if (ctx.getLocale() == HitHotLocale.en_US) {
					question = StringUtils.replace(question, robotName, " you ");
				} else {
					question = StringUtils.replace(question, robotName, "你");
				}
			} else {
				question = StringUtils.replace(question, robotName, "你");
			}

			if (!StringUtils.equalsIgnoreCase(orig, question)) {
				candidates.add(question);
			}
		}
		question = StringUtils.lowerCase(removeSymbols(question, ctx.getTenant().getLocale()));

		// 拿掉語助詞
		Matcher bp = CASUAL_AUXILIARY_STRING_BEGINING_PATTERN.matcher(question);
		if (bp.find()) {
			String c1 = ClientUtils.escapeQueryChars(bp.replaceAll(""));

			if (c1.length() > 0) {
				String c2 = replaceCasualText(c1);

				if (!StringUtils.equals(question, c1))
					candidates.add(c1);

				if (!StringUtils.equals(c1, c2))
					candidates.add(c2);
			}
		} else {
			if (!StringUtils.equals(orig, question))
				candidates.add(question);
		}

		if (question.length() > 1 && StringUtils.endsWithAny(question, AUXILIARY_STRING_ENDING_ARRAY)) {
			String c1 = ClientUtils.escapeQueryChars(question.substring(0, question.length() - 1));

			while (c1.length() > 1 && StringUtils.endsWithAny(c1, AUXILIARY_STRING_ENDING_ARRAY)) {
				c1 = ClientUtils.escapeQueryChars(c1.substring(0, c1.length() - 1));
			}

			String c2 = replaceCasualText(c1);

			if (!StringUtils.equals(question, c1))
				candidates.add(c1);

			if (!StringUtils.equals(c1, c2))
				candidates.add(c2);
		} else {
			if (!StringUtils.equals(orig, question))
				candidates.add(question);
		}

		List<String> alts = new ArrayList<String>();

		for (String c : candidates) {
			String cc = ClientUtils.escapeQueryChars(c);

			if (!alts.contains(cc)) {
				alts.add(cc);
			}
		}

		return alts;
	}

	public String reconstructToSolrQuery(String question) {
		StringBuilder notNM = new StringBuilder();

		List<org.ansj.domain.Term> parse = QAUtil.USE_NLPANALYSIS
				? org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + question + " "))
						.getTerms()
				: getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + question + " ")).getTerms();
		// parse = org.ansj.util.FilterModifWord.modifResult(parse) ;
		boolean allEnglish = true;
		boolean inEnMode = false;
		String englishPhraseStack = "";
		List<String> queue = new ArrayList<String>();

		for (int i = 0; i < parse.size(); i++) {
			org.ansj.domain.Term t = parse.get(i);

			if ((i == 0 || i + 1 == parse.size()) && StringUtils.isBlank(t.getName()))
				continue;

			String nature = t.getNatureStr();

			/*
			 * try { String newNature = qautil.getUserDefinedNature(t.getRealName()); if
			 * (newNature != null && !StringUtils.equalsIgnoreCase(nature, newNature)) {
			 * nature = newNature; t.setNature(new org.ansj.domain.Nature(newNature)); } }
			 * catch (Exception ignore) {}
			 */

			if (StringUtils.equalsIgnoreCase(nature, "en")) {
				if (inEnMode) {
					englishPhraseStack += " " + t.getRealName();
				} else {
					inEnMode = true;
					englishPhraseStack = t.getRealName();
				}
				if (i < parse.size() - 1)
					continue;
			}
			/*
			 * else if (StringUtils.startsWithAny(nature, new String[] {"m"})) {
			 * t.setRealName("{{$NUM}}|"); } else if (StringUtils.startsWithAny(nature, new
			 * String[] {"t"})) { t.setRealName("{{$DATE}}|"); }
			 */
			else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
				// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
				if (i < parse.size() - 1)
					continue;
			} else {
				allEnglish = false;

				if (inEnMode) {
					queue.add(englishPhraseStack);
					inEnMode = false;
				}
			}

			if (!inEnMode && (nature != null && !"null".equals(nature)
					&& StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
				String kw = t.getRealName();

				try {
					if (tenant.isForceIgnoreCharacter(kw)) {
						kw += "**";
					}
				} catch (Exception ignore) {
				}

				queue.add(kw);
			} else {
				String kw = t.getRealName();
				queue.add(kw + "**");
			}
			if (inEnMode && !(i < parse.size() - 1)) {
				queue.add(englishPhraseStack);
			}

			while (queue.size() > 0) {
				String kw = queue.remove(0);
				if ("nw".equals(nature)) {
					continue;
				}

				if (kw.indexOf("**") != -1) {
					continue;
				}

				List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance()
						.listByQueryWhereReverseIsTrue(tenant.getId(), kw);

				if (synonymsAll.size() > 0) {
					String newKw = StringUtils.length(kw) > 1 ? WiSeUtils.dblQuote(kw) : "";
					int c = newKw.length() > 0 ? 1 : 0;

					for (SynonymKeyword syn : synonymsAll) {
						// if (StringUtils.length(syn.getKeyword()) == 1 || StringUtils.equals(kw,
						// syn.getKeyword()))
						if (StringUtils.equals(kw, syn.getKeyword()))
							continue;

						if (newKw.length() > 0) {
							newKw += " ";
						}

						c++;
						newKw += WiSeUtils.dblQuote(syn.getKeyword());
					}

					if (c > 1)
						kw = "(" + newKw + ")";
					else
						kw = newKw;
				} else {
					if (StringUtils.length(kw) == 1)
						continue;
					kw = WiSeUtils.dblQuote(kw);
				}

				if (notNM.length() > 0) {
					notNM.append(" ");
				}
				notNM.append("");

				if (kw.length() > 0) {
					notNM.append(kw);// + ":[" + nature + "]");
				}
				notNM.append("");
			}
		}

		return notNM.toString();
	}

	/**
	 * KOKO + i刷 = "KOKO i刷" KOKO + 信用卡 = "KOKO信用卡" 信用卡 + 餘額 = "信用卡餘額"
	 *
	 * @param b
	 * @param toBeAppend
	 */
	private static void appendSpaceIfAppendLetterToLetter(StringBuilder b, String toBeAppend) {
		if (b.length() > 0 && b.substring(b.length() - 1, b.length()).matches("^\\w$") && toBeAppend.length() > 0
				&& toBeAppend.substring(0, 1).matches("^\\w$")) {
			b.append(' ');
		}
		b.append(toBeAppend);
	}

	public static String preReconstructClean(String r) {
		String tmp = r;
		tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), ".&|-{}\"'\\/－＝＋＆", "");
		if (tmp.equals(""))
			tmp = " ";
		return tmp;
	}
	
    public static String removeSymbols(String r) {
        String tmp = r;
        tmp = StringUtils.replaceChars( StringUtils.lowerCase(tmp), "^,.?!$&|-()[]{}\"'\\/，。－＝＋？！＄＆／＼｜（）「」『』", "");
        return tmp;
    }

	public static String removeSymbols(String r, HitHotLocale locale) {
		String tmp = r;
		switch (locale) {
		case zh_TW:
		case ja_JP:
			tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), "~～!！,.?$&|-()[]{}\"'\\/，。－＝＋？＄＆／＼｜（）「」『』", "");
			break;
		case en_US:
			tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), "~～!！,.?$&|()[]{}\"\\/，。－＝＋？＄＆／＼｜（）「」『』", " ");
		}
		if (tmp.equals(""))
			tmp = " ";
		return tmp;
	}

	public static String simpleClean(String r, Tenant t) {
		String tmp = r;
		switch (t.getLocale()) {
		case zh_TW:
		case ja_JP:
			tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), ",.?！$&|-()[]{}\\/\"'，。－＝＋？!＄＆／＼｜（）「」『』", "");// +
																														// (t
																														// !=
																														// null
																														// ?
																														// StringUtils.trimToEmpty(t.getForceIgnoreCharacters())
																														// :
																														// ""),
																														// "");
			break;
		case en_US:
			tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), ",.?！$&|-()[]{}\\/\"，。－＝＋？!＄＆／＼｜（）「」『』", " ");// +
																														// (t
																														// !=
																														// null
																														// ?
																														// StringUtils.trimToEmpty(t.getForceIgnoreCharacters())
																														// :
																														// ""),
																														// "");
		}
		// tmp = tmp.replaceAll("^(查詢?|(抱歉)?請?(問|說明)(一下)?)", "");
		// tmp = tmp.replaceAll("(嗎|ㄇ|嘛|吗|了)$", "");
		// System.out.println("Cleaned Alt:[" + tmp + " (" + r + ")]");
		return tmp;
	}

	// replace泥、妳、您to你in searchDirectCasual
	public static String replaceCasualText(String r) {
		String tmp = r;
		tmp = StringUtils.replaceChars(tmp, "妳您泥妮", "你你你你");
		return tmp;
	}

	/*
	 * public static String replaceCustumeText(String r) { String tmp = r; tmp =
	 * StringUtils.replaceChars( tmp, "123456789", "一二三四五六七八九"); return tmp; }
	 * 
	 * private static Collection<String> cleanUpAltString(Collection<String> r) {
	 * Set<String> newList = new HashSet<String>(); Iterator<String> itr =
	 * r.iterator(); while (itr.hasNext()) { String cleaned =
	 * cleanUpAltString(itr.next()); newList.add(cleaned); } return newList; }
	 */

	public static String autoLink(String input) {
		return new AutoLink().link(StringUtils.trimToEmpty(input));
	}

	public static void main(String[] args) {
		HibernateUtil.init();
		QAUtil qa = new QAUtil(WiseSystemConfigFacade.getInstance().get().getDefaultTenant());
		List<NameValuePair> s = qa.search("請問館前分行電話");
		System.out.println(s);

		/*
		 * try { String[][] tags = qa.tag2Array("好市多信用卡申辦的手續?"); for (String[] tag:
		 * tags) { System.out.println(Arrays.asList(tag)); }
		 * 
		 * JointParser parser = new JointParser(WiSeEnv.getHomePath() +
		 * "/models/dep.m"); DependencyTree tree = parser.parse2T(tags[0],tags[1]);
		 * System.out.println(tree.toString()); } catch (LoadModelException e) {
		 * e.printStackTrace(); }
		 * 
		 * List<String> alts = QAUtil.reconstructQuestion("雙幣卡在國外刷卡,交易手續費怎收",
		 * ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM);
		 * System.out.println(alts.size()); TreeSet<String> ts = new TreeSet<String>();
		 * ts.addAll(alts); System.out.println(StringUtils.join(ts, '\n'));
		 */
		/*
		 * String[] tests = new String[] { "(本期|)信用卡帳單最低(應繳|)(多少|)" };
		 * 
		 * for (String test: tests) { System.out.println("Current Test:[" + test + "]");
		 * List<String> l = new ArrayList<String>(); l.add(test);
		 * 
		 * QAAltBuild build = new QAAltBuild();
		 * build.setCreatedTime(Calendar.getInstance().getTime()); build.setDocId(null);
		 * build.setQAltTpls(StringUtils.join(l, "\n")); Collection<String> expanded =
		 * new QASaver(build, new QA()).expandAlt(null);
		 * 
		 * System.out.println("Got (" + expanded.size() + ") alts"); for (String e:
		 * expanded) { System.out.print("  --> "); System.out.println(e); } }
		 */
	}

	public static enum AnswerConfidenceLevel {
		HIGH("高", 800d, null), MEDIUM("普通", 40d, 800d), LOW("低", 25d, 40d),
		NO_ANSWER("smart.robot.qa.confidence.noAnswer", null, 30d),;

		private String name;
		private Double hBoundary; // Not include
		private Double lBoundary; // Include

		AnswerConfidenceLevel(String name, Double lBoundary, Double hBoundary) {
			this.name = name;
			this.lBoundary = lBoundary;
			this.hBoundary = hBoundary;
		}

		public String getName() {
			return name;
		}

		public static AnswerConfidenceLevel findConfidenceLevel(double score) {
			for (AnswerConfidenceLevel acl : values()) {
				if ((acl.lBoundary == null || score >= acl.lBoundary)
						&& (acl.hBoundary == null || score < acl.hBoundary))
					return acl;
			}

			return NO_ANSWER;
		}

		public static double calConfidenceFactor(double score) {
			double cf = Math.min(200, score) / 20;
			return Math.round(cf * 100) / 100;
		}
	}

	public static enum QuestionType {
		WHO, WHERE, WHEN, WHY, UNKNOWN
	}

	public static class SVOParser {
		private String[][] tagArray;
		private int subjectPosition = -1;
		private boolean isQuestion = false;
		private QuestionType questionType = QuestionType.UNKNOWN;
		private String subject;
		private String verb;
		private String object;
		private static final int WORD = 0;
		private static final int TARGET = 1;

		private static final List<String> SUBJECT_TARGET_LIST = Arrays
				.asList(new String[] { "人稱代詞", "專有名", "人名", "地名", "機構名", "時間短語" });
		private static final List<String> PREDICATE_TARGET_LIST = Arrays
				.asList(new String[] { "動詞", "能願動詞", "形謂詞", "行為動詞" });
		private static final List<String> OBJECT_TARGET_LIST = Arrays
				.asList(new String[] { "人稱代詞", "專有名", "人名", "名詞", "地名", "機構名", "時間短語", "疑問代詞" });
		private static final List<String> QUESTION_MARK_LIST = Arrays.asList(new String[] { "？", "?" });

		public SVOParser(String[][] tagArray) {
			this.tagArray = tagArray;
			subject = parseSubject();
			verb = parsePredicate();
			object = parseObject();
		}

		protected String parseSubject() {
			int i;
			String[] zhuyu = new String[20];
			int k = 0;
			for (i = 0; i < tagArray[WORD].length; i++) {
				String word = tagArray[WORD][i];
				String target = tagArray[TARGET][i];
				if (SUBJECT_TARGET_LIST.contains(target)) {
					zhuyu[k] = word;
					subjectPosition = i;
					k++;
					break;
				} else if (StringUtils.equals(target, "名詞") || StringUtils.equals(target, "實體名")) {
					if (i >= 1 && (tagArray[TARGET][i - 1].equals("動詞") || tagArray[TARGET][i - 1].equals("能願動詞")
							|| tagArray[TARGET][i - 1].equals("把動詞"))) {
						// 當做不是？
					} else {
						zhuyu[k] = word;
						subjectPosition = i;
						k++;
						break;
					}
				} else {
				}
			}
			return zhuyu[0];
		}

		protected String parsePredicate() {
			int i;
			String[] weiyu = new String[20];
			int k = 0;
			for (i = 0; i < tagArray[WORD].length; i++) {
				String word = tagArray[WORD][i];
				String target = tagArray[TARGET][i];
				if (PREDICATE_TARGET_LIST.contains(target)) {
					if (StringUtils.equals(target, "能願動詞")) {
						weiyu[k] = word;
						int findNextVerb = i + 1;
						while (findNextVerb < tagArray[WORD].length && (tagArray[TARGET][findNextVerb].equals("動詞"))) {
							weiyu[k] += tagArray[WORD][findNextVerb];
							findNextVerb++;
						}
						k++;
						break;
					} else {
						weiyu[k] = word;
						k++;
					}

					break;
				}
			}
			return weiyu[0];
		}

		protected String parseObject() {
			int i;
			String[] binyu = new String[20];
			int k = 0;

			// 取出賓語
			for (i = 0; i < tagArray[WORD].length && k < binyu.length; i++) {
				if (i == subjectPosition)
					continue;

				String word = tagArray[WORD][i];
				String target = tagArray[TARGET][i];

				if (OBJECT_TARGET_LIST.contains(target)) {
					binyu[k] = word;
					k++;

					if (target.equals("疑問代詞")) {
						isQuestion = true;
						questionType = parseQuestionType(word);
					}
				}

				if ("標點".equals(target) && QUESTION_MARK_LIST.contains(word)) {
					isQuestion = true;
				}
			}

			System.out.println("SPOS=" + subjectPosition);
			return binyu[0];
		}

		private QuestionType parseQuestionType(String word) {
			QuestionType qt = QuestionType.UNKNOWN;
			if (StringUtils.contains(word, "哪")) {
				qt = QuestionType.WHERE;
			} else if (StringUtils.contains(word, "誰")) {
				qt = QuestionType.WHO;
			}
			return qt;
		}

		public boolean isQuestion() {
			return isQuestion;
		}

		public QuestionType getQuestionType() {
			return questionType;
		}

		public String getSubject() {
			return subject;
		}

		public String getVerb() {
			return verb;
		}

		public String getObject() {
			return object;
		}

		public boolean isSVO() {
			return subject != null && verb != null && object != null;
		}

		@Override
		public String toString() {
			return "SVOParser [tagArray=" + Arrays.toString(tagArray) + ", subjectPosition=" + subjectPosition
					+ ", isQuestion=" + isQuestion + ", questionType=" + questionType + ", subject=" + subject
					+ ", verb=" + verb + ", object=" + object + "]";
		}
	}

	public static final class AutoLink {
		static final String URL = "(((ht|f)tp(s?)\\:\\/\\/|~\\/)|www.)" + "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov"
				+ "|mil|biz|info|mobi|name|aero|jobs|museum" + "|travel|[a-z]{2}))(:[\\d]{1,5})?"
				+ "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" + "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?"
				+ "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" + "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?"
				+ "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" + "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?";
		final String linkBefore = "<a href=\"";
		final String linkMiddleOne = "\" title=\"";
		final String linkMiddleTwo = "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">";
		final String linkAfter = "</a>";
		static final Pattern patternUrl = Pattern.compile(URL);

		String output = null, protocol;
		Matcher matchers = null;

		public String link(String input) {
			try {
				matchers = patternUrl.matcher(input);
				output = matchers
						.replaceAll(linkBefore + "$0" + linkMiddleOne + "$0" + linkMiddleTwo + "連結" + linkAfter);
			} catch (PatternSyntaxException e) {
				System.out.println(e.getMessage() + e.getDescription() + e.getIndex());
			}
			return StringUtils.defaultString(StringUtils.trimToNull(output), input);
		}
	}

	public static String toClickableHtml(String cmd, String text, boolean supportHtml) {
		String out;
		if (supportHtml) {
			out = "<span class='question-clickable' data-opnum='" + cmd + "'>" + text + "</span>";
		} else {
			out = text;
		}
		return out;
	}

	public static final String DATE_FORMAT = "yyyy/MM/dd";
	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

	public static boolean NO_MORE_EXPENSIVE_SYN_EXPAND = true;

	public static Date parseDate(String dateStr) throws ParseException {
		return new SimpleDateFormat(DATE_FORMAT).parse(dateStr);
	}

	public static String formatDate(Date date) {
		return new SimpleDateFormat(DATE_FORMAT).format(date);
	}

	public static Date parseDatetime(String datetimeStr) throws ParseException {
		return new SimpleDateFormat(DATETIME_FORMAT).parse(datetimeStr);
	}

	public static String formatDatetime(Date datetime) {
		return new SimpleDateFormat(DATETIME_FORMAT).format(datetime);
	}

	public static boolean isDateExpired(String date) {
		boolean expired = false;
		if (StringUtils.isNotBlank(date)) {
			long expiryMillis = Long.MAX_VALUE;
			try {
				expiryMillis = parseDate(date).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			expired = System.currentTimeMillis() >= expiryMillis;
		}
		return expired;
	}

	public static String getFullUrlBase() {
		return WiseSystemConfigFacade.getInstance().get().getFullUrlBase();
	}

	public static String getContextPath() {
		return WiseSystemConfigFacade.getInstance().get().getContextPath();
	}

	public static String appendIcspidIfEnterTesterMode(QAContext qaCtx, String in) {
		String out = in;
		if (qaCtx != null && qaCtx.getTenant().getEnableDebug()) {
			String icspid = StringUtils.trim(qaCtx.getIcspid());
			out += (qaCtx.isClientSupportHtml() ? "<br/>" : "\n") + "（<bean:message key='global.binding'/>DEVICE ID: "
					+ (StringUtils.isBlank(icspid) ? "<bean:message key='global.without'/>" : icspid) + "）";
		}
		return out;
	}

	public class FormalAnswerReplacer implements Replacer {
		private Map<String, String> defaultAnswer = new LinkedHashMap<String, String>();
		private String bindToTag;

		public FormalAnswerReplacer(String bindToTag, QAContext ctx) {
			this.bindToTag = bindToTag;
			defaultAnswer.put("ENTER", "global.enter");
			defaultAnswer.put("MANY_REPEAT_SAME_QUESTION", "default.answer");
			defaultAnswer.put("NO_ANSWER", "no.answer");
			defaultAnswer.put("NO_ANSWER_BUT_HAS_RECOMMENDATION", "search.some.recommendation");
			defaultAnswer.put("OR_YOU_CAN", "or.you.can");
			defaultAnswer.put("PLEASE", "global.please");
			defaultAnswer.put("ROBOT_NAME", "robot.name");
			defaultAnswer.put("SEARCH_TEXT_1", "search.text.1");
			defaultAnswer.put("SEARCH_TEXT_2_PLAIN", "search.text.2.plain");
			defaultAnswer.put("SEARCH_TEXT_2_HTML", "search.text.2.html");
			defaultAnswer.put("YOU_MAY_WANT_TO_KNOW", "you.may.want.to.know");
//			defaultAnswer.put("WAITING_AND_CHOOSE_TEXT1", "waiting.and.choose.text1");
//			defaultAnswer.put("OFFLINE_FORM_TEXT1", "offline.form.text1");
//			defaultAnswer.put("THE_END_TEXT1", "the.end.text1");
//			defaultAnswer.put("OFFLINE_FORM_TEXT2", "offline.form.text1");
//			defaultAnswer.put("THE_END_TEXT2", "the.end.text1");

			defaultAnswer.put("FORWARD_HEADER_GENERAL", "forward.header.general");
			defaultAnswer.put("FORWARD_HEADER_MARKETING", "forward.header.marketing");
			defaultAnswer.put("FORWARD_HEADER_NEGATIVE", "forward.header.negative");
			defaultAnswer.put("FORWARD_HEADER_WEBCHAT", "forward.header.webChat");
			defaultAnswer.put("FORWARD_HEADER_WEBCHAT_BUSY", "forward.header.webChatBusy");
			defaultAnswer.put("FORWARD_HEADER_WEBCHAT_OFFLINE", "forward.header.webChatOffline");

			defaultAnswer.put("FORWARD_FOOTER_GENERAL", "forward.footer.general");

			defaultAnswer.put("FORWARD_OPTION_STAY_WITH_ROBOT", "forward.option.stayWithRobot");
			defaultAnswer.put("FORWARD_OPTION_KEEP_WAITING_WEBCHAT", "forward.option.keepWaitingWebchat");

			defaultAnswer.put("CHOOSED_FORWARD_OPTION_STAY_WITH_ROBOT", "choosed.forward.option.stayWithRobot");
			defaultAnswer.put("CHOOSED_FORWARD_OPTION_MESSAGE_BOARD", "choosed.forward.option.messageBoard");

			defaultAnswer.put("FORWARD_OPTION_WEBCHAT", "forward.option.webChat");
			defaultAnswer.put("FORWARD_OPTION_MESSAGE_BOARD", "forward.option.messageBoard");
			defaultAnswer.put("FORWARD_OPTION_VOICE_CALL", "forward.option.voiceCall");
			defaultAnswer.put("FORWARD_OPTION_VIDEO_CALL", "forward.option.videoCall");
			defaultAnswer.put("FORWARD_OPTION_ONLINE_BANKING", "forward.option.onlineBanking");
			defaultAnswer.put("DEFAULT_ROBOT_VOICE_ANSWER", "robot.default.voice.answer");
			
			//與迪普串接需要
			defaultAnswer.put("alreadyCancel", "alreadyCancel");
			defaultAnswer.put("applyNowCustomerService", "applyNowCustomerService");
			defaultAnswer.put("applyNowForLoan", "applyNowForLoan");
			defaultAnswer.put("autoTransfer", "autoTransfer");
			defaultAnswer.put("availableCredit", "availableCredit");
			defaultAnswer.put("billCheckoutDate", "billCheckoutDate");
			defaultAnswer.put("BillInfoLink", "BillInfoLink");
			defaultAnswer.put("clickCustomerService", "clickCustomerService");
			defaultAnswer.put("checkoutBill", "checkoutBill");
			defaultAnswer.put("currBillTranDetail", "currBillTranDetail");
			defaultAnswer.put("currentOverpayAmout", "currentOverpayAmout");
			defaultAnswer.put("eLoveEarthTogether", "eLoveEarthTogether");
			defaultAnswer.put("emailBox", "emailBox");
			defaultAnswer.put("emailInOneHour", "emailInOneHour");
			defaultAnswer.put("goldAmount", "goldAmount");
			defaultAnswer.put("IAgreeEmail", "IAgreeEmail");
			defaultAnswer.put("incomeDate", "incomeDate");
			defaultAnswer.put("miniAmountPayment", "miniAmountPayment");
			defaultAnswer.put("noCardInfo", "noCardInfo");
			defaultAnswer.put("noNewPymRecord", "noNewPymRecord");
			defaultAnswer.put("noPayment", "noPayment");
			defaultAnswer.put("noStateMent", "noStateMent");
			defaultAnswer.put("notWorkingHourNow", "notWorkingHourNow");
			defaultAnswer.put("nowYouHaveNoEmail", "nowYouHaveNoEmail");
			defaultAnswer.put("paymentDeadline", "paymentDeadline");
			defaultAnswer.put("poleBear", "poleBear");
			defaultAnswer.put("sevenDays", "sevenDays");
			defaultAnswer.put("sharingQuota", "sharingQuota");
			defaultAnswer.put("singleQuota", "singleQuota");
			defaultAnswer.put("smartPay", "smartPay");
			defaultAnswer.put("totalAmountofCurrentBill", "totalAmountofCurrentBill");
			defaultAnswer.put("totalCredit", "totalCredit");
			defaultAnswer.put("transDate", "transDate");
			defaultAnswer.put("usedQuata", "usedQuata");
			defaultAnswer.put("waitForTwoDays", "waitForTwoDays");
			defaultAnswer.put("youMayWannaKnow", "youMayWannaKnow");
			
			//webline 
			defaultAnswer.put("LADDER_GUIDE", "global.pleaseSelect");
			defaultAnswer.put("LADDER_GUIDE_BY_WEBLINE", "global.pleaseSelect");
			defaultAnswer.put("YOU_MAY_WANT_TO_KNOW_BY_WEBLINE", "you.may.want.to.know.by.webline");
			
		}

		@Override
		public String call(String name, String val) {
			if (StringUtils.equals(name, bindToTag)) {
				String ans = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(), val, val);

				return StringUtils.trimToNull(ans);
			}
			// backward compatibility
			else if (StringUtils.equals(name, "ROBOT_NAME")) {
				String ans = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(), name, val); // 這裡要用 name, 不是 val，因為
																								// val 是 null
				return StringUtils.trimToNull(ans);
			} else if (StringUtils.equals(name, "L")) {
				String linkName = StringUtils
						.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");
				String link = StringUtils.substringAfter(val, ":");

				return "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\""
						+ QAUtil.ANSWER_LINK_TARGET + "\">" + linkName + "</a>";
			}

			return null;
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			QAChannel ch = QAChannel.get(ctx.getTenant().getId(), ctx.getQaChannel());
			if (((StringUtils.equals(name, "ROBOT_NAME") || StringUtils.equals(val, "ROBOT_NAME"))
					&& ctx.getRequestAttribute("replaceName") != null
					&& ctx.getRequestAttribute("replaceName").equals("false"))) {
				return null;
			}
			if (StringUtils.equals(name, bindToTag)) {
				String ans = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(), val,
						getDefaultFormalAnswer(ctx, val));

				return StringUtils.trimToNull(ans);
			}
			// backward compatibility
			else if (StringUtils.equals(name, "ROBOT_NAME")) {
				String ans = RobotFormalAnswers.giveMeRandomAnswer(tenant.getId(), name,
						getDefaultFormalAnswer(ctx, name)); // 這裡要用 name, 不是 val，因為 val 是 null
				return StringUtils.trimToNull(ans);
			} else if (StringUtils.equals(name, "L")) {
				// 只有 Channel 選用「附加選項到答案文字」時才把連結弄成答案內的文字
				if (ctx.getQAChannelInstance().getAppendOptionToOutput()) {
					String linkName = StringUtils
							.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");
					if (ctx.isClientSupportHtml()) {
						String link = StringUtils.substringAfter(val, ":");

						return "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\""
								+ QAUtil.ANSWER_LINK_TARGET + "\">" + linkName + "</a>";
					} else {
						return linkName;
					}
				} else {
					return "";
				}
			} else if (StringUtils.equals(name, "Q")) {
				// 只有 Channel 選用「附加選項到答案文字」時才把選項弄成答案內的文字
				if (ctx.getQAChannelInstance().getAppendOptionToOutput()) {
					String linkName = StringUtils
							.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "點我");

					if (ctx.isClientSupportHtml()) {
						String link = StringUtils.substringAfter(val, ":");
						return "<span class='question-clickable' data-opnum='" + link + "'>" + linkName + "</span>";
					} else {
						return linkName;
					}
				} else {
					return "";
				}
			} else if (StringUtils.equals(name, "TIME")) {
				String format = "HH:mm:ss";
				if (ctx.getLocale() == HitHotLocale.zh_TW || ctx.getLocale() == HitHotLocale.zh_CN) {
					format = "HH%smm%sss%s";
					format = String.format(format, ctx.getMessage("global.hour"), ctx.getMessage("global.mintues"),
							ctx.getMessage("global.seconds"));
				}
				SimpleDateFormat sdFormat = new SimpleDateFormat(format);
				return sdFormat.format(new Date());
			} else if (StringUtils.equals(name, "DATE")) {
				String format = "yyyy/MM/dd";
				if (ctx.getLocale() == HitHotLocale.zh_TW || ctx.getLocale() == HitHotLocale.zh_CN) {
					format = "yyyy%sMM%sdd%s";
					format = String.format(format, ctx.getMessage("global.year"), ctx.getMessage("global.month"),
							ctx.getMessage("global.day"));
				}
				SimpleDateFormat sdFormat = new SimpleDateFormat(format);
				return sdFormat.format(new Date());
			} else if (StringUtils.equals(name, "WEEK")) {
				SimpleDateFormat sdFormat = new SimpleDateFormat("E", Locale.CHINESE);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				switch (val) {
				case "BEFORE_YESTERDAY":
					calendar.add(Calendar.DATE, -2);
					break;
				case "YESTERDAY":
					calendar.add(Calendar.DATE, -1);
					break;
				case "TOMORROW":
					calendar.add(Calendar.DATE, 1);
					break;
				case "AFTER_TOMORROW":
					calendar.add(Calendar.DATE, 2);
					break;
				}
				return sdFormat.format(calendar.getTime());
			}
			/*
			 * else if (StringUtils.equals(name, "INQUIRY")) { ctx.setCurrentQuestion(val);
			 * ctx.setOriginalQuestion(val); ctx.setAnswerText(""); QAMatchRuleController c
			 * = QAMatchRuleController.getInstance(ctx.getTenant().getId()); c.check(ctx);
			 * String answer = ""; if (ctx.getAnswerText() != null) { answer =
			 * ctx.getAnswerText().toString(); answer = TemplateUtil.process(ctx, answer,
			 * QAUtil.getCleaner()); } return answer; }
			 */

			return null;
		}

		private String getDefaultFormalAnswer(QAContext ctx, String val) {
			if (defaultAnswer.containsKey(val)) {
				String[] args = { "{{ROBOT_NAME}}" };
				return ctx.getMessage(defaultAnswer.get(val), args);
			}
			return "";
		}

		public Map<String, List<String>> getDefaultRobotFormalAnswerMap(QAContext ctx) {
			Map<String, List<String>> defaultRobotFormalAnswer = new LinkedHashMap<String, List<String>>();
			String[] args = { "{{ROBOT_NAME}}" };
			for (Map.Entry<String, String> defaultAnswerEntry : defaultAnswer.entrySet()) {
				List<String> answer = new ArrayList<String>();
				try {
					answer.add(ctx.getMessage(defaultAnswerEntry.getValue(), args));
					defaultRobotFormalAnswer.put(defaultAnswerEntry.getKey(), answer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return defaultRobotFormalAnswer;
		}
	}

	FormalAnswerReplacer far = null;

	public FormalAnswerReplacer getFormalAnswerReplacer(QAContext ctx) {
		if (far == null) {
			far = new FormalAnswerReplacer("F", ctx);
		}
		return far;
	}

	static Cleaner cleaner = new Cleaner();

	/**
	 * 清空所有 {{XXXX}} 之類的東西
	 * 
	 * @return
	 */
	public static Cleaner getCleaner() {
		return cleaner;
	}

	/**
	 * {{G:XXXX:width=100&height=100&enclosedByCenterTag=false}}
	 * 
	 * @author herb
	 *
	 */
	public class FormalAnswerStickerReplacer implements Replacer {
		private Map<String, String> defaultAnswer = new HashMap<String, String>();
		private String bindToTag;

		public FormalAnswerStickerReplacer(String bindToTag, QAContext ctx) {
			this.bindToTag = bindToTag;
		}

		@Override
		public String call(String name, String val) {
			return call(null, name, val);
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			if (ctx == null || ctx.getQAChannelInstance().getUseHtmlNewline()) {
				if (StringUtils.equals(name, bindToTag)) {
					String stickerKey = StringUtils.substringBefore(val, ":");
					String stickerAttr = StringUtils.substringAfter(val, ":");

					RobotFormalAnswersSticker sticker = RobotFormalAnswersSticker.giveMeAnswer(tenant.getId(),
							stickerKey);

					if (sticker != null) {
						int h = sticker.getHeight();
						int w = sticker.getWidth();
						boolean enclosedByCenterTag = false;

						if (StringUtils.isNotEmpty(stickerAttr)) {
							try {
								Map<String, List<String>> attrs = WiSeUtils.splitUrlQueryParams(stickerAttr);
								if (attrs.containsKey("width")) {
									w = Integer.parseInt(attrs.get("width").get(0));
								}
								if (attrs.containsKey("height")) {
									h = Integer.parseInt(attrs.get("height").get(0));
								}
								if (attrs.containsKey("enclosedByCenterTag")) {
									enclosedByCenterTag = Boolean.parseBoolean(attrs.get("enclosedByCenterTag").get(0));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						return (enclosedByCenterTag ? "<center>" : "")
								+ String.format("<img height='%d' width='%d' src='%s'>", h, w,
										QAUtil.getFullUrlBase() + "/img/sticker/" + sticker.getAnswers())
								+ (enclosedByCenterTag ? "</center>" : "");
					}
					return "<img height='100' width='100' src='" + QAUtil.getFullUrlBase() + "/img/sticker/"
							+ stickerKey + "'>";
				}
			}

			return "";
		}

	}

	FormalAnswerStickerReplacer farSticker = null;

	public FormalAnswerStickerReplacer getFormalAnswerStickerReplacer(QAContext ctx) {
		if (farSticker == null) {
			farSticker = new FormalAnswerStickerReplacer("G", ctx);
		}
		return farSticker;
	}

	/**
	 * @deprecated Use
	 *             {@link #nlp(Tenant,QAUtil,String,Locale,boolean,TemperalAndNumberStrategy)}
	 *             instead
	 */
	public static NlpResult nlp(Tenant tenant, QAUtil qautil, String question, Locale locale, boolean replaceSyn, boolean reserveTemperalAndNumber) {
		return nlp(tenant, qautil, question, locale, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, 
				reserveTemperalAndNumber 
				? TemperalAndNumberStrategy.PREBUILD_STRATEGY_SEGMENT_2
				: TemperalAndNumberStrategy.PREBUILD_STRATEGY_SEGMENT_1);
	}

	public static NlpResult nlp(Tenant tenant, QAUtil qautil, String question, Locale locale, ReconstructQuestionParameter param, TemperalAndNumberStrategy strategy) {
        return qautil.nlp(question, qautil, param, strategy,false);
    }
	
	public static NlpResult nlp(Tenant tenant, QAUtil qautil, String question, Locale locale, ReconstructQuestionParameter param, TemperalAndNumberStrategy strategy,boolean replaceSyn) {
        return qautil.nlp(question, qautil, param, strategy,replaceSyn);
    }
	
    public NlpResult nlp(String question, QAUtil qautil, ReconstructQuestionParameter param, TemperalAndNumberStrategy strategy, boolean replaceSyn) {
		StringBuilder notNM = new StringBuilder();
		NlpResult res = new NlpResult(question);
		Set<String> eksSet = null;
		List<TreeSet<String>> eks = null;
		boolean ekMode = tenant.getEnableEssentialKeywordMode();

		if (ekMode) {
			eksSet = EssentialKeywordDictionary.eksSet(tenant.getId());
			eks = new ArrayList<>();
			res.eks = eks;
		}
		List<org.ansj.domain.Term> terms = null;
		if (tenant != null && tenant.getLocale() == HitHotLocale.ja_JP) {
			terms = new ArrayList<org.ansj.domain.Term>();
			int offe = 0;
			
		    for (Token token : qautil.getJpTokenizer().tokenize(question)) {
		    		String kw = token.getBaseForm();
		    		if (StringUtils.trimToNull(kw) == null || "*".equals(kw)) {
		    			kw = token.getSurface();
		    		}
			    	/*System.out.println(kw + "\t" + token.getAllFeatures());
			    	System.out.println(kw + "\t ---> " + token.getReading());
			    	System.out.println(kw + "\t ---> " + new KuromojiUtil().segAndToHiragana(token.getReading()));*/
			    	org.ansj.domain.Term t = new org.ansj.domain.Term(kw, offe++, KuromojiUtil.kuromojiTagMapping(token.getAllFeaturesArray()), 1000);
			    	terms.add(t);
			}
		}
		else if (tenant != null && tenant.getLocale() == HitHotLocale.en_US) {
			terms = qautil.englishUtil.tokenize(question);
		}
		else {
			terms = qautil.parseChineseTerms(question);
		}
		boolean allEnglish = true;
		boolean inEnMode = false;
		boolean isLastTemporalTerm = false;
		boolean isLastNumberTerm = false;
		String englishPhraseStack = "";
		List<String> queue = new ArrayList<String>();
		
		while (terms.size() > 0) {
			org.ansj.domain.Term t = terms.get(terms.size() - 1);

			if (StringUtils.isBlank(t.getName())) {
				terms.remove(terms.size() - 1);
			}
			else {
				break;
			}
		}
		
		while (terms.size() > 0) {
			org.ansj.domain.Term t = terms.get(0);

			if (StringUtils.isBlank(t.getName())) {
				terms.remove(0);
			}
			else {
				break;
			}
		}
	
		StringBuilder originSeg = new StringBuilder();
		for (org.ansj.domain.Term t: terms) {
			originSeg.append("(" + t.getRealName() + ")");
		}
		res.setOriginSentenceSeg(originSeg.toString());
	
		for (int i=0; i < terms.size(); i++) {
			org.ansj.domain.Term t = terms.get(i);
//			System.out.println(t.getName() + ":" + t.getNatureStr());
			
			if ((i == 0 || i+1 == terms.size()) && StringUtils.isBlank(t.getName()) && inEnMode == false) {
				isLastTemporalTerm = isLastNumberTerm = false;
				continue;
			}
			
			String nature = t.getNatureStr();
	
			if (StringUtils.startsWith(nature, "mt")) {
                if(replaceSyn)t.setRealName("{{$NUM}}|{{$DATE}}|");    
				// 避免被下一個 elseif 到
			}
			else if (StringUtils.startsWith(nature, "m") && StringUtils.endsWithAny(t.getRealName(), QAUtil.TEMPORAL_STR_ENDING)) {
				t.setNature(new org.ansj.domain.Nature("t"));
				nature = "t";
			}
			
			if (StringUtils.startsWith(nature, "m") && !QAUtil.NOT_A_NUMBER_STR_SET.contains(t.getRealName())) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER), t.getRealName(), CategorizedKeyValuePair.Type.NUMBER);
	
				String convertable = CategorizedValueParser.toNumberRangeQuery(kv);
	
				if (convertable == null) {
					nature = "m?";
				}
                if(replaceSyn)t.setRealName("{{$NUM}}|");
			}
			else if (StringUtils.startsWith(nature, "t")) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL), t.getRealName(), CategorizedKeyValuePair.Type.TEMPORAL);
				String convertable = CategorizedValueParser.toDateRangeQuery(kv);
	
				if (convertable == null) {
					nature = "t?";
				}
                if(replaceSyn)t.setRealName("{{$DATE}}|");
			}
	
			if (StringUtils.equalsIgnoreCase(nature, "en")) {
				if (inEnMode) {
					englishPhraseStack += " " + t.getRealName();
				}
				else {
					inEnMode = true;
					englishPhraseStack = t.getRealName();
				}
				if (i < terms.size() - 1) continue;
			}
			else if (param.normalizeSynonym && StringUtils.startsWithAny(nature, new String[] {"mt"})) {
				boolean markIgnorable = strategy.markAllTemperalStrIgnorable || strategy.markAllNumericStrIgnorable;
				if (isLastTemporalTerm && isLastNumberTerm) {
					if (strategy.keepOriginalTemperalStr || strategy.keepOriginalNumericStr) {
						String lastKw = queue.get(queue.size()-1);
						lastKw = StringUtils.substring(lastKw, 0, markIgnorable ? lastKw.length() - 1 : lastKw.length()) 
								+ t.getRealName()
								+ (markIgnorable ? "|" : "")
									;
						//System.out.println(lastKw);
						queue.set(queue.size()-1, lastKw);
					}
				}
				else {
    				isLastTemporalTerm = isLastNumberTerm = true;
    				
    				StringBuilder sb = new StringBuilder();
    				if (strategy.addSymbolToMatchAllTemperal) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append("{{$DATE}}");
    				}
    				if (strategy.addSymbolToMatchAllNumber) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append("{{$NUM}}");
    				}
    				if (strategy.keepOriginalTemperalStr || strategy.keepOriginalNumericStr) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append(t.getRealName());
    				}
    				if (markIgnorable) {
    					if (sb.length() > 0) sb.append('|');
    				}
    				
    				if (sb.length() > 0)
    					t.setRealName(sb.toString());
    				
					queue.add(t.getRealName());
				}
				if (i < terms.size() - 1) continue;
			}
			else if (param.normalizeSynonym && StringUtils.startsWithAny(nature, new String[] {"m"})) {
				if (isLastNumberTerm && !isLastTemporalTerm) {
					if (strategy.keepOriginalNumericStr) {
						String lastKw = queue.get(queue.size()-1);
						lastKw = StringUtils.substring(lastKw, 0, strategy.markAllNumericStrIgnorable ? lastKw.length() - 1 : lastKw.length()) 
								+ t.getRealName()
								+ (strategy.markAllNumericStrIgnorable ? "|" : "")
									;
						//System.out.println(lastKw);
						queue.set(queue.size()-1, lastKw);
					}
				}
				else {
    				isLastTemporalTerm = false; isLastNumberTerm = true;
    				StringBuilder sb = new StringBuilder();
    				if (strategy.addSymbolToMatchAllNumber) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append("{{$NUM}}");
    				}
    				if (strategy.keepOriginalNumericStr) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append(t.getRealName());
    				}
    				if (strategy.markAllNumericStrIgnorable) {
    					if (sb.length() > 0) sb.append('|');
    				}
    				
    				if (sb.length() > 0)
    					t.setRealName(sb.toString());
    				
					queue.add(t.getRealName());
				}

				if (i < terms.size() - 1) continue;
			}
			else if (param.normalizeSynonym && StringUtils.startsWithAny(nature, new String[] {"t"})) {
				if (!isLastNumberTerm && isLastTemporalTerm) {
					if (strategy.keepOriginalTemperalStr) {
						String lastKw = queue.get(queue.size()-1);
						lastKw = StringUtils.substring(lastKw, 0, strategy.markAllTemperalStrIgnorable ? lastKw.length() - 1 : lastKw.length()) 
								+ t.getRealName()
								+ (strategy.markAllTemperalStrIgnorable ? "|" : "")
									;
						//System.out.println(lastKw);
						queue.set(queue.size()-1, lastKw);
					}
				}
				else {
    				isLastTemporalTerm = true; isLastNumberTerm = false;
    				StringBuilder sb = new StringBuilder();
    				if (strategy.addSymbolToMatchAllTemperal) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append("{{$DATE}}");
    				}
    				if (strategy.keepOriginalTemperalStr) {
    					if (sb.length() > 0) sb.append('|');
    					sb.append(t.getRealName());
    				}
    				if (strategy.markAllTemperalStrIgnorable) {
    					if (sb.length() > 0) sb.append('|');
    				}
    				
    				if (sb.length() > 0)
    					t.setRealName(sb.toString());
    				
					queue.add(t.getRealName());
				}
				if (i < terms.size() - 1) continue;
			}
			else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
				isLastTemporalTerm = false; isLastNumberTerm = false;
				// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
				if (i < terms.size() - 1) continue;
			}
			else {
				allEnglish = false;
				isLastTemporalTerm = false; isLastNumberTerm = false;
	
				if (inEnMode) {
					queue.add(englishPhraseStack);
					inEnMode = false;
				}
			}

			if (!inEnMode && (nature != null && !"null".equals(nature) && StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
				String kw = t.getRealName();
		
				try {
					if (tenant.isForceIgnoreCharacter(kw)) {
						kw += "**";
					}
				}
				catch (Exception ignore) {}
				
				if (!isLastTemporalTerm && !isLastNumberTerm)
					queue.add(kw);
			}
			else if (inEnMode && !(i < terms.size() - 1)) {
				queue.add(englishPhraseStack);
			}
			else {
				String kw = t.getRealName();
				if (!((i+1 == terms.size()) && StringUtils.isBlank(t.getName())))
					queue.add(kw + "**");
			}
	
			System.out.println("NLP QUEUE:" + queue);
			List<String> eksThisLv = new ArrayList<>();
	
			while (queue.size() > 0) {
				String kw = queue.remove(0);
				if ("nw".equals(nature)) {
					continue;
				}
	
				boolean replacedBySyn = false;
				if (param.normalizeSynonym) {
					if (kw.indexOf("**") != -1) {
						continue;
					}
					
					if (ekMode && eksSet.contains(kw)) {
						// 確認此詞是否為 ez keyword
						eksThisLv.add(kw);
					}
					
					List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenant.getId(), kw);
					
					if (synonymsAll.size() > 0) {
						if (param.normalizeSynonymWithHighestPriority) {
							int highestP = -2;
							String theSurvivedKw = null;

							for (SynonymKeyword syn: synonymsAll) {
								if (syn.getPriority() > highestP) {
									theSurvivedKw = syn.getKeyword();
									highestP = syn.getPriority();
								}
							}
							
							if (theSurvivedKw != null) {
								replacedBySyn = true;
								kw = theSurvivedKw;

								if (ekMode && eksSet.contains(kw)) {
									// 確認此同義詞的頭(hightest priority)是否為 ez keyword
									eksThisLv.add(kw);
							}
						}
						}
						else {
							replacedBySyn = true;
							String newKw = "";
							
							for (SynonymKeyword syn: synonymsAll) {
								if (newKw.length() > 0) {
									newKw += "|";
								}
								
								newKw += syn.getKeyword();

								if (ekMode && eksSet.contains(syn.getKeyword())) {
									// 確認此同義詞的頭是否為 ez keyword
									eksThisLv.add(syn.getKeyword());
							}
							}
							
							kw = newKw;
						}
					}
				}
				else {
					if (ekMode && eksSet.contains(kw)) {
						// 確認此詞是否為 ez keyword
						eksThisLv.add(kw);
					}
				}
	
				if (ekMode && eksThisLv.size() > 0) {
					res.addEssentialKeyword(eksThisLv.toArray(new String[0]));
				}
	
				if (notNM.length() > 0) {
					//notNM.append(" / ");
				}
	
				if (kw.length() > 0) {
					if (param.normalizeSynonym) {
						if (kw.length() > 0 || replacedBySyn || StringUtils.startsWith(nature, "#")) {
							notNM.append("(").append(kw).append(")");
						}
						else {
							notNM.append(kw);
						}
					}
					else {
						notNM.append("(").append(kw).append(")");
					}
				}
			}
		}
	
		if (notNM.length() == 0) {
			res.setSegResult(question);
		}
		else {
			res.setSegResult(notNM.toString());
	}
		return res;
	}

	List<org.ansj.domain.Term> parseChineseTerms(String sentence) {
		Result res = null;

		if (QAUtil.USE_NLPANALYSIS) {
			res = org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + sentence + " "));
		} else {
			res = getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + sentence + " "));
		}

		new UserDicNatureRecognition(this.getAmbiguityForest(), this.getUserDefinedForest()).recognition(res);
		return res.getTerms();
	}

	public static String nlpWithColor(QAUtil qautil, String question, Locale locale) {
		StringBuilder notNM = new StringBuilder();

		List<org.ansj.domain.Term> terms = null;
		if (qautil.tenant != null && qautil.tenant.getLocale() == HitHotLocale.ja_JP) {
			terms = new ArrayList<org.ansj.domain.Term>();
			int offe = 0;

			System.out.println("  ---------> " + new KuromojiUtil().segAndToHiragana(question));

			for (Token token : qautil.nativeJpTokenizer.tokenize(question)) {
				String kw = token.getBaseForm();
				if (StringUtils.trimToNull(kw) == null || "*".equals(kw)) {
					kw = token.getSurface();
				}
				/*
				 * System.out.print(kw + "\t" + token.getAllFeatures()); System.out.println();
				 */

				org.ansj.domain.Term t = new org.ansj.domain.Term(kw, offe++,
						KuromojiUtil.kuromojiTagMapping(token.getAllFeaturesArray()), 1000);
				terms.add(t);
			}
		} else if (qautil.tenant != null && qautil.tenant.getLocale() == HitHotLocale.en_US) {
			terms = qautil.englishUtil.tokenize(question);
		} else {
			terms = QAUtil.USE_NLPANALYSIS
					? org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + question + " "))
							.getTerms()
					: qautil.getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + question + " ")).getTerms();
		}
		// parse = org.ansj.util.FilterModifWord.modifResult(parse) ;
		boolean allEnglish = true;
		boolean inEnMode = false;
		String englishPhraseStack = "";
		List<String> queue = new ArrayList<String>();

		//
		for (int i = 0; i < terms.size(); i++) {
			org.ansj.domain.Term t = terms.get(i);
			String nature = t.getNatureStr();

			System.out.println(t.getName() + "\t" + t.getNatureStr());

			if (StringUtils.equalsIgnoreCase(nature, "en")) {
				if (inEnMode) {
					englishPhraseStack += " " + t.getRealName();
				} else {
					inEnMode = true;
					englishPhraseStack = t.getRealName();
				}
				if (i < terms.size() - 1)
					continue;
			} else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
				// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
				if (i < terms.size() - 1)
					continue;
			} else {
				allEnglish = false;

				if (inEnMode) {
					queue.add(englishPhraseStack);
					inEnMode = false;
				}
			}

			if (!inEnMode && (nature != null && !"null".equals(nature)
					&& StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
				String kw = t.getRealName();

				if (i == 0 && kw.matches("^(查詢?|(抱歉)?請?(問|說明)(一下)?)")) {
					kw += "**";
				}

				queue.add(kw);
			} else {
				String kw = t.getRealName();
				queue.add(kw + "**");
			}
			if (inEnMode && !(i < terms.size() - 1)) {
				queue.add(englishPhraseStack);
			}

			while (queue.size() > 0) {
				String kw = queue.remove(0);
				if ("nw".equals(nature) || " **".equals(kw)) {
					continue;
				}
				
				boolean replacedBySyn = false;
					
					
				List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance()
						.listByQueryWhereReverseIsTrue(qautil.tenant.getId(), kw);

				if (synonymsAll.size() > 0) {
					replacedBySyn = true;
					String newKw = "";

					for (SynonymKeyword syn : synonymsAll) {
						if (newKw.length() > 0) {
							newKw += "|";
						}

						newKw += syn.getKeyword();
					}

					kw = newKw;
				}
	
				
				kw = kw.replace("**", "");
				if (notNM.length() > 0) {
					notNM.append("\n");
				}
				//notNM.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");

				if (kw.length() > 0) {
					notNM.append(kw);
				}

				if (nature != null) {
					if (nature.startsWith("n")) {
						notNM.append("(名詞)");
					} else if (nature.startsWith("t")) {
						notNM.append("(時間詞)");
					} else if (nature.startsWith("s")) {
						notNM.append("(處所詞)");
					} else if (nature.startsWith("f")) {
						notNM.append("(方位詞)");
					} else if (nature.startsWith("v")) {
						notNM.append("(動詞)");
					} else if (nature.startsWith("a")) {
						notNM.append("(形容詞)");
					} else if (nature.startsWith("b")) {
						notNM.append("(區別詞)");
					} else if (nature.startsWith("z")) {
						notNM.append("(狀態詞)");
					} else if (nature.startsWith("r")) {
						notNM.append("(代詞)");
					} else if (nature.startsWith("m")) {
						notNM.append("(數詞)");
					} else if (nature.startsWith("q")) {
						notNM.append("(量詞)");
					} else if (nature.startsWith("d")) {
						notNM.append("(副詞)");
					} else if (nature.startsWith("p")) {
						notNM.append("(介詞)");
					} else if (nature.startsWith("c")) {
						notNM.append("(連詞)");
					} else if (nature.startsWith("u")) {
						notNM.append("(助詞)");
					} else if (nature.startsWith("e")) {
						notNM.append("(嘆詞)");
					} else if (nature.startsWith("y")) {
						notNM.append("(語氣詞)");
					} else if (nature.startsWith("o")) {
						notNM.append("(擬聲詞)");
					} else if (nature.startsWith("h")) {
						notNM.append("(前綴詞)");
					} else if (nature.startsWith("k")) {
						notNM.append("(後綴詞)");
					} else if (nature.startsWith("x")) {
						notNM.append("(字符串)");
					} else if (nature.startsWith("w")) {
						notNM.append("(標點符號)");
					} else if (nature.startsWith("#UserDefined_KP")) {
						notNM.append("(知識主題)");
					} else if (nature.startsWith("#UserDefined_SSK")) {
						notNM.append("(自訂詞)");
					} else if (nature.startsWith("#UserDefined_SS")) {
						notNM.append("(同義詞)");
					} else {
						notNM.append("(" + nature + ")");
					}
				} else {
					notNM.append("(" + nature + ")");
				}
			}
		}

		//notNM.append("\n");
		return notNM.toString();
	}

	public static String simpleNlp(QAUtil qautil, String question, boolean replaceSyn) {
		StringBuilder notNM = new StringBuilder();

		List<org.ansj.domain.Term> parse = QAUtil.USE_NLPANALYSIS
				? org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + question + " "))
						.getTerms()
				: qautil.getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + question + " ")).getTerms();
		// parse = org.ansj.util.FilterModifWord.modifResult(parse) ;
		boolean allEnglish = true;
		boolean inEnMode = false;
		String englishPhraseStack = "";
		List<String> queue = new ArrayList<String>();

		for (int i = 0; i < parse.size(); i++) {
			org.ansj.domain.Term t = parse.get(i);

			System.out.println(t.getName() + ":" + t.getNatureStr());

			if ((i == 0 || i + 1 == parse.size()) && StringUtils.isBlank(t.getName()) && inEnMode == false)
				continue;

			String nature = t.getNatureStr();

			if (StringUtils.startsWith(nature, "mt")) {
				// 避免被下一個 elseif 到
			} else if (StringUtils.startsWith(nature, "m")
					&& StringUtils.endsWithAny(t.getRealName(), QAUtil.TEMPORAL_STR_ENDING)) {
				t.setNature(new org.ansj.domain.Nature("t"));
				nature = "t";
			}

			if (StringUtils.startsWith(nature, "m") && !QAUtil.NOT_A_NUMBER_STR_SET.contains(t.getRealName())) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(
						CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER), t.getRealName(),
						CategorizedKeyValuePair.Type.NUMBER);

				String convertable = CategorizedValueParser.toNumberRangeQuery(kv);

				if (convertable == null) {
					nature = "m?";
				}
			} else if (StringUtils.startsWith(nature, "t")) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(
						CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL), t.getRealName(),
						CategorizedKeyValuePair.Type.TEMPORAL);
				String convertable = CategorizedValueParser.toDateRangeQuery(kv);

				if (convertable == null) {
					nature = "t?";
				}
			}

			if (StringUtils.equalsIgnoreCase(nature, "en")) {
				if (inEnMode) {
					englishPhraseStack += " " + t.getRealName();
				} else {
					inEnMode = true;
					englishPhraseStack = t.getRealName();
				}
				if (i < parse.size() - 1)
					continue;
			} else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
				// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
				if (i < parse.size() - 1)
					continue;
			} else {
				allEnglish = false;

				if (inEnMode) {
					queue.add(englishPhraseStack);
					inEnMode = false;
				}
			}

			if (!inEnMode && (nature != null && !"null".equals(nature)
					&& StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
				String kw = t.getRealName();
				try {
					if (qautil.tenant.isForceIgnoreCharacter(kw)) {
						kw += "**";
					}
				} catch (Exception ignore) {
				}
				queue.add(kw);
			} else {
				String kw = t.getRealName();
				if (!((i + 1 == parse.size()) && StringUtils.isBlank(t.getName())))
					queue.add(kw + "**");
			}
			if (inEnMode && !(i < parse.size() - 1)) {
				queue.add(englishPhraseStack);
			}

			while (queue.size() > 0) {
				String kw = queue.remove(0);
				if ("nw".equals(nature)) {
					continue;
				}

				if (kw.indexOf("**") != -1)
					continue;

				if (replaceSyn) {
					List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance()
							.listByQueryWhereReverseIsTrue(qautil.tenant.getId(), kw);

					if (synonymsAll.size() > 0) {
						String newKw = "";
						for (SynonymKeyword syn : synonymsAll) {
							if (newKw.length() > 0) {
								newKw += "|";
							}
							newKw += syn.getKeyword();
						}
						kw = newKw;
					}
				}

				if (notNM.length() > 0) {
					// notNM.append(" / ");
				}

				if (kw.length() > 0) {
					notNM.append("(");
					notNM.append(kw);// + ":[" + nature + "]");
					notNM.append(")");
				}
			}
		}

		return notNM.toString();
	}

	public String getQaTagFilter() {
		return qaTagFilter;
	}

	public void setQaTagFilter(String qaTagFilter) {
		this.qaTagFilter = qaTagFilter;
	}

	public String doWiVoByRuleConfig(String question, String channelCode) {
		String wivo = null;
		try {
			if (tenant.getLocale() == com.intumit.hithot.HitHotLocale.zh_TW) {
				WiVoSuggestRule r = (WiVoSuggestRule) QAMatchRuleController.getInstance("tenant:" + tenant.getId())
						.getRuleByClassName(QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH,
								WiVoSuggestRule.class.getName());
				if (r != null) {
					wivo = r.suggest(tenant.getId(), channelCode, question);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return wivo;
	}

	public static void parseRequest(QAContext qaCtx, HttpServletRequest request, boolean testMode, boolean doShift) {
		Locale locale = (Locale) request.getSession().getAttribute("org.apache.struts.action.LOCALE");

		if (request.getParameter("locale") != null) {
			String localeStr = request.getParameter("locale");
			HitHotLocale hhl = HitHotLocale.valueOf(localeStr);
			locale = HitHotLocale.guessJavaLocale(hhl);

			qaCtx.setLocale(hhl);
		} else {
			qaCtx.setLocale(com.intumit.hithot.HitHotLocale.guessHitHotLocaleByJavaLocale(locale));
		}

		String baseUrl = StringUtils.trimToEmpty(WiseSystemConfig.get().getHostname()) + request.getContextPath();
		qaCtx.setCtxAttr("baseUrl", baseUrl);

		boolean noRecommand = getBooleanParameter(request, "noRecommand", false);
		String qaChannel = getStringParameterDefaultIfEmpty(request, "ch", "web");
		String userType = getStringParameterDefaultIfEmpty(request, "userType", "unknown");
		String etStr = getStringParameterDefaultIfEmpty(request, "et", "message");
		String eventSource = getStringParameterDefaultIfEmpty(request, "es", null);
		String eventSourceType = getStringParameterDefaultIfEmpty(request, "est", null);
		Boolean forceToggleContextThisTime = getBooleanParameter(request, "ftc", null);
		boolean htmlFormat = getBooleanParameter(request, "html", true);
		String icspid = getStringParameterDefaultIfEmpty(request, "ICSPID", null);
		String qaCategorySelect = getStringParameterDefaultIfEmpty(request, "category", null);
		String qaTagFilter = getStringParameterDefaultIfEmpty(request, "qaTagFilter", null);
		String replaceName = getStringParameterDefaultIfEmpty(request, "replaceName", null);

		Tenant t = Tenant.getFromRequest(request, testMode);

		if (t != null) {
			qaCtx.setTenant(t);
		}

		// 如果還是沒有 tenant，則失敗
		if (t == null) {
			JSONObject errorOutput = new JSONObject();
			try {
				errorOutput.put("errorCode", 400);
				errorOutput.put("errorMessage", "Cann't determine skill type.");
			} catch (JSONException ignore) {
			}

			throw new RobotException(errorOutput);
		} else if (!t.getEnableTenant()) {
			JSONObject errorOutput = new JSONObject();
			try {
				errorOutput.put("errorCode", 400);
				errorOutput.put("errorMessage", "Tenant disable.");
			} catch (JSONException ignore) {
			}

			throw new RobotException(errorOutput);
		}
		// 測試模式不計算 request 量，不然目前排程測試或者相關的自動測試可能都會卡住
		else if (!testMode) {
			if (!qaCtx.acquireRequestPermit() || !RequestRateLimiter.acquireRequestPermition(
					com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request), qaCtx.getTenant(),
					"/qa-ajax.jsp")) {
				JSONObject errorOutput = new JSONObject();
				try {
					errorOutput.put("errorCode", 600);
					errorOutput.put("errorMessage",
							"Cannot acquire request permission, you should not flooding API request.");
				} catch (JSONException ignore) {
				}

				throw new RobotException(errorOutput);
			}
		}

		if (doShift)
			qaCtx.shift(); // 清除 request scope attributes

		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(request.getParameter("q")));
		qaCtx.setOriginalQuestion(tmpQ);
		qaCtx.setOriginalQuestionTime(QAUtil.formatDatetime(Calendar.getInstance().getTime()));
		qaCtx.setCurrentQuestion(tmpQ);
		qaCtx.setQaChannel(qaChannel);
		qaCtx.setUserType(userType);
		qaCtx.setClientSupportHtml(htmlFormat);
		qaCtx.setForceToggleContextThisTime(forceToggleContextThisTime);
		qaCtx.setIcspid(icspid);
		if (qaCtx.getLastQaCategory() == null && qaCategorySelect != null) {
			qaCtx.setLastQaCategory(qaCategorySelect);
		}

		qaCtx.setEservice(qaChannel);

		EventType et = EventType.matchBestEventType(t.getId(), qaChannel, etStr);
		if (et == null) {
			System.out.println("Unknown EventType [" + etStr + "]...");
		}
		qaCtx.setEventType(et);

		// 是 message 的 default eventSource 是 InputBox，其餘是 Unknown
		if (et != null && "message".equals(et.getCode())) {
			if (eventSource == null) {
				eventSource = QAContext.EVENT_SOURCE_INPUTBOX;
			}
			if (eventSourceType == null) {
				eventSourceType = QAContext.EVENT_SOURCE_TYPE_KEYIN;
			}
		} else {
			if (eventSource == null) {
				eventSource = QAContext.EVENT_SOURCE_UNKNOWN;
			}
			if (eventSourceType == null) {
				eventSourceType = QAContext.EVENT_SOURCE_TYPE_UNKNOWN;
			}
		}
		qaCtx.setEventSource(eventSource);
		qaCtx.setEventSourceType(eventSourceType);

		qaCtx.setRequestAttribute("noRecommand", noRecommand);
		qaCtx.setRequestAttribute("qaCategorySelect", qaCategorySelect);
		if (qaTagFilter != null) {
			qaCtx.setRequestAttribute("qaTagFilter", qaTagFilter);
		}
		if (replaceName != null) {
			qaCtx.setRequestAttribute("replaceName", replaceName);
		}

		qaCtx.setCtxAttr("_bundle", new JSONObject(WiSeUtils.getParameterMap(request)));
	}

	public static Boolean getBooleanParameter(HttpServletRequest request, String paramName, Boolean defaultVal) {
		Boolean val = null;
		if (request.getParameter(paramName) != null) {
			if (Boolean.parseBoolean(request.getParameter(paramName))) {
				val = true;
			} else {
				val = false;
			}
		} else {
			val = defaultVal;
		}

		return val;
	}

	public static String getStringParameterDefaultIfEmpty(HttpServletRequest request, String paramName,
			String defaultVal) {
		String val = request.getParameter(paramName);
		if (StringUtils.isEmpty(val)) {
			val = defaultVal;
		}

		return val;
	}

	public static StringBuilder replaceDoubleWraps(String text, String target, String replacement) {
		return new StringBuilder(text.replace(target, replacement));
	}

	public void addKuromojiUserDictionary(String keyword, String nature) {
		if (StringUtils.isBlank(keyword))
			return;
		String cleanKeyword = StringUtils.trim(keyword);
		if (cleanKeyword.contains(" "))
			return;
		if (!kuromojiUserDictionary.containsKey(cleanKeyword)) {
			kuromojiUserDictionary.put(cleanKeyword, String.format("%s,%s,*,%s", cleanKeyword, cleanKeyword, nature));
		}
	}

	public void deleteKuromojiUserDictionary(String keyword) {
		if (StringUtils.isBlank(keyword))
			return;
		String cleanKeyword = StringUtils.trim(keyword);
		if (kuromojiUserDictionary.containsKey(cleanKeyword)) {
			kuromojiUserDictionary.remove(cleanKeyword);
		}
	}

	public void reloadKuromojiUserDictionary() {
		try {
			jpTokenizer = new Tokenizer.Builder().mode(Mode.NORMAL)
					.userDictionary(getKuromojiUserDictionaryInputStream()).build();
		} catch (Exception ignore) {
			jpTokenizer = new Tokenizer.Builder().mode(Mode.NORMAL).build();
		}
	}

	public InputStream getKuromojiUserDictionaryInputStream() {
		if (kuromojiUserDictionary.isEmpty())
			return new ByteArrayInputStream("".getBytes());

		return new ByteArrayInputStream(StringUtils.join(kuromojiUserDictionary.values(), "\r\n").getBytes());
	}
}
