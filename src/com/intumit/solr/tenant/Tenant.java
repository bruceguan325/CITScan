package com.intumit.solr.tenant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.ClusterMembershipListener;
import com.intumit.solr.SearchManager;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.QAUserType;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.RobotFormalAnswers;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.systemconfig.WiseSystemConfig;

import flexjson.JSONSerializer;

@Entity
public class Tenant implements Serializable {
	public static final String SESSION_KEY = "_INTUMITTENANT_";
	public static String SIMPLE_AES_SECRET_KEY = "Intumit168168168";
	public static String SIMPLE_AES_INIT_VECTOR = "IntumitRandomInV";

	static final int DEFAULT_OTHER_MLT_QA_NUM = 5;
	static final Boolean DEFAULT_ENABLE_TEXT_CRM = Boolean.TRUE;
	static final String DEFAULT_CRM_API_BASE_URL = "http://localhost/SmartHelpDesk/lhc_web/";
	static final Boolean DEFAULT_DEBUG = Boolean.TRUE;
	static final Boolean DEFAULT_QA_EXPLAIN = Boolean.FALSE;
	static final Boolean DEFAULT_CONTEXT_MATCH = Boolean.FALSE;
	static final Boolean DEFAULT_RESTRICT_TO_QA_CATEGORY = Boolean.FALSE;
	static final Boolean DEFAULT_RESTRICT_TO_KNOWLEDGE_POINT = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_PHONETIC_HOMONYM = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_ESSENTIAL_KEYWORD_MODE = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_MULTI_LOCALE = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_SCENARIO = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_AUTO_LEARNING = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_RATE_LIMIT_BY_IP_ADDR = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_RATE_LIMIT_BY_QA_CONTEXT = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_QA_AUDIT = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_USER_FRONTEND = Boolean.FALSE;
	static final int DEFAULT_RATE_LIMIT_BY_IP_PER_SECOND = 3;
	static final int DEFAULT_RATE_LIMIT_BY_QA_CONTEXT_PER_SECOND = 1;
	
	//用情境做滿意度匯出報表功能
	static final Boolean DEFAULT_ENABLE_SCENARIO_CRM = Boolean.FALSE;
	
	static final int DEFAULT_THRESHOLD_1 = 100; // used in qa-ajax.jsp
	static final int DEFAULT_THRESHOLD_2 = 40;	// used in qa-ajax.jsp
	static final int DEFAULT_THRESHOLD_3 = 20;	// used in qa-ajax.jsp
	static final int DEFAULT_TOO_MANY_REPEAT_THRESHOLD = 3;	// used in qa-ajax.jsp
	
	public static final int DEFAULT_QACONTEXT_SESSION_TIMEOUT_IN_SECOND = 30*60;	// 30min

	static final int DEFAULT_BOOST_1 = 2;	// 知識主題加權
	static final int DEFAULT_BOOST_2 = 5;	// 上下文加權
	static final int DEFAULT_BOOST_3 = 1;	// 詞庫加權
	
	static final int DEFAULT_MAX_BATCH_UPLOAD_SIZE = 300;

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	private String uuid;

	@Column(length = 64)
	private String name;

	private String email; // Also account name, used for login

	private String pwd;

	private String tel;

	@Column(length = 512)
	private String address;

	@Column(length = 512)
	private String coreName;

	@Enumerated(EnumType.STRING)
	private HitHotLocale locale;

	@Column(length = 512)
	private String qaCategory;

	@Column(length = 128)
	private String specificEservice;

	@Deprecated
	@Index(name="apikeyIdx")
	@Column(length = 32)
	private String apikey;

	@Lob
	private String notes;

	@Lob
	private String faqJson;

	@Lob
	private String qaMatchCtrlFlow;

	@Deprecated
	@Index(name="lineBotChannelIdIdx")
	@Column(length = 32)
	private String lineBotChannelId;

	/**
	 * 這個將來統一作為各種 Bot Config 用，不然每次都要開欄位有點瞎
	 */
	@Lob
	private String lineBotConfigJson;

	@Column(length = 256)
	private String forceIgnoreCharacters;

	@Column(length = 256)
	private String allowedLocales;

	private Boolean emailVerified;
	private String emailVerifyCode;

	private long permission;

	private Boolean enableTenant = Boolean.FALSE;
	private String crmApiBaseUrl;
	private String casualCoreUrl;
	private String statisticsCoreUrl;
	private String opendataCoreUrl;
	private String outerSourceUrl;
	/**
	 * 被實體的 fromIndex 取代
	 * @deprecated
	 */
	private String allowedCustomDataFieldNames;
	
	/**
	 * @TODO need explanation 
	 */
	private String allowedOptionsJson;

	private Integer otherMltQANum = null;
	private Integer defaultTextCrmDepartmentId = 1;
	private Boolean enableDebug = DEFAULT_DEBUG;
	private Boolean enableQAExplain = DEFAULT_QA_EXPLAIN;
	private Boolean enableEssentialKeywordMode = DEFAULT_ENABLE_ESSENTIAL_KEYWORD_MODE;
	private Boolean enableContextMatch = DEFAULT_CONTEXT_MATCH;
	private Boolean enableRestrictToQaCategory = DEFAULT_RESTRICT_TO_QA_CATEGORY;
	private Boolean enableRestrictToKnowledgePoint = DEFAULT_RESTRICT_TO_KNOWLEDGE_POINT;
	private Boolean enablePhoneticHomonym = DEFAULT_ENABLE_PHONETIC_HOMONYM;
	private Boolean enableTextCrm = DEFAULT_ENABLE_TEXT_CRM;
	private Boolean enableMultiLocale = DEFAULT_ENABLE_MULTI_LOCALE;
	private Boolean enableScenario = DEFAULT_ENABLE_SCENARIO;
	private Boolean enableAutoLearning = DEFAULT_ENABLE_AUTO_LEARNING;
	private Boolean enableQaAudit = DEFAULT_ENABLE_QA_AUDIT;
	private Boolean enableUserFrontEnd = DEFAULT_ENABLE_USER_FRONTEND;
	
	private Boolean enableRateLimitByIP = null;
	private Boolean enableRateLimitByQAContext = null;
	
	private Integer threshold1 = DEFAULT_THRESHOLD_1;	// 完全比對的正確答案門檻
	private Integer threshold2 = DEFAULT_THRESHOLD_2;	// 模糊比對的正確答案門檻
	private Integer threshold3 = DEFAULT_THRESHOLD_3;	// 相關推薦的門檻
	private Integer tooManyRepeatThreshold = DEFAULT_TOO_MANY_REPEAT_THRESHOLD;	// 鍵盤壞惹的次數

	private Integer boost1 = DEFAULT_BOOST_1;	// 知識點關鍵字加權
	private Integer boost2 = DEFAULT_BOOST_2;	// 上下文加權
	private Integer boost3 = DEFAULT_BOOST_3;	// 詞庫加權
	
	private Integer rateLimitByIpPerSec;
	private Integer rateLimitByQAContextPerSec;
	private Integer sessionExpirationSecond;
	
	private Integer maxBatchUploadSize = DEFAULT_MAX_BATCH_UPLOAD_SIZE;

	// 情境滿意度問卷匯出
	private Boolean enableScenarioCrm = DEFAULT_ENABLE_SCENARIO_CRM;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Tenant other = (Tenant) obj;
		if (id != other.id) return false;
		return true;
	}
	
	public Boolean getEnableScenarioCrm() {
		return enableScenarioCrm != null ? enableScenarioCrm : DEFAULT_ENABLE_SCENARIO_CRM;
	}

	public void setEnableScenarioCrm(Boolean enableScenarioCrm) {
		this.enableScenarioCrm = enableScenarioCrm;
	}

	/*
	// 新壽 POC 新增欄位 (BEGIN)
	private Boolean enableUserLogin = null;
	private Boolean enableUploadFileInRichText = null;
	
	public Boolean getEnableUserLogin() {
		return enableUserLogin != null ? enableUserLogin : false;
	}
	public void setEnableUserLogin(Boolean enableUserLogin) {
		this.enableUserLogin = enableUserLogin;
	}
	public Boolean getEnableUploadFileInRichText() {
		return enableUploadFileInRichText != null ? enableUploadFileInRichText : false;
	}
	public void setEnableUploadFileInRichText(Boolean enableUploadFileInRichText) {
		this.enableUploadFileInRichText = enableUploadFileInRichText;
	}
	// 新壽 POC 新增欄位 (END)
	*/

	private String kmsUrl;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HitHotLocale getLocale() {
		return locale != null ? locale : HitHotLocale.zh_TW;
	}

	public void setLocale(HitHotLocale locale) {
		this.locale = locale;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmailVerifyCode() {
		return emailVerifyCode;
	}

	public void setEmailVerifyCode(String emailVerifyCode) {
		this.emailVerifyCode = emailVerifyCode;
	}

	public Boolean getEmailVerified() {
		return emailVerified == null ? false : emailVerified;
	}

	public void setEmailVerified(Boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCoreName() {
		return coreName;
	}

	public void setCoreName(String coreName) {
		this.coreName = coreName;
	}

	public SolrServer getCoreServer() {
		if (StringUtils.startsWith(coreName, "http:")) {
			return SearchManager.getRemoteServer(coreName);
		}

		return SearchManager.getServer(coreName);
	}

	public SolrServer getCoreServer4Write() {
		if (StringUtils.startsWith(coreName, "http:")) {
			return SearchManager.getRemoteServer(coreName);
		}
		else if (WiseSystemConfig.get().getLbModeEnable() && !ClusterMembershipListener.getInstance().isLeaderNow()) {
			String masterUrl = SearchManager.getCoreUrl4Write(coreName);

			if (masterUrl != null) {
				return SearchManager.getRemoteServer(masterUrl);
			}
		}

		return SearchManager.getServer(coreName);
	}

	/**
	 * 被實體的 fromIndex 取代
	 * @deprecated
	 * @return
	 */
	public String getAllowedCustomDataFieldNames() {
		return allowedCustomDataFieldNames;
	}

	/**
	 * 
	 * 被實體的 fromIndex 取代
	 * @deprecated
	 * @param allowedCustomDataFieldNames
	 */
	public void setAllowedCustomDataFieldNames(String allowedCustomDataFieldNames) {
		this.allowedCustomDataFieldNames = allowedCustomDataFieldNames;
	}

	public String getQaCategory() {
		return qaCategory;
	}

	public void setQaCategory(String qaCategory) {
		this.qaCategory = qaCategory;
	}

	public String getSpecificEservice() {
		return specificEservice == null ? "" : specificEservice;
	}

	public void setSpecificEservice(String specificEservice) {
		this.specificEservice = specificEservice;
	}
	
	public Integer getSessionExpirationSecond() {
		return sessionExpirationSecond != null ? sessionExpirationSecond : DEFAULT_QACONTEXT_SESSION_TIMEOUT_IN_SECOND;
	}

	public void setSessionExpirationSecond(Integer sessionExpirationSecond) {
		this.sessionExpirationSecond = sessionExpirationSecond;
	}

	@Override
	public String toString() {
		return "Tenant [id=" + id + ", uuid=" + uuid + ", name=" + name
				+ ", email=" + email + ", tel=" + tel + ", coreName="
				+ coreName + ", notes=" + notes + "]";
	}

	private static final String QACATEGORY_SPLITTER = ",";
	private static final Integer FIRST_QACATEGORY_INDEX = 0;

	/**
	 * qaCategory加上index，方便頁面處理
	 * @return
	 */
	public Map<Integer, String> getQaCategoryMap() {
		Map<Integer, String> qaCategoryMap = new LinkedHashMap<Integer, String>();
		if (StringUtils.isNotBlank(qaCategory)) {
			String[] qaCategorys = qaCategory.split(QACATEGORY_SPLITTER);
			Integer idx = new Integer(FIRST_QACATEGORY_INDEX);
			for (String qaCate : qaCategorys) {
				qaCategoryMap.put(idx++, qaCate);
			}
		}
		return qaCategoryMap;
	}

	public String getDefaultQaCategory() {
		Map<Integer, String> qaCategoryMap = getQaCategoryMap();
		if (!qaCategoryMap.isEmpty()) {
			return qaCategoryMap.get(FIRST_QACATEGORY_INDEX);
		}
		return "";
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Boolean getEnableTenant() {
		return enableTenant != null ? enableTenant : true;
	}

	public void setEnableTenant(Boolean enableTenant) {
		this.enableTenant = enableTenant;
	}

	public String getCrmApiBaseUrl() {
		return crmApiBaseUrl != null ? crmApiBaseUrl : DEFAULT_CRM_API_BASE_URL;
	}

	public void setCrmApiBaseUrl(String crmApiBaseUrl) {
		this.crmApiBaseUrl = crmApiBaseUrl;
	}

	public String getCasualCoreUrl() {
		return casualCoreUrl;
	}

	public void setCasualCoreUrl(String casualCoreUrl) {
		this.casualCoreUrl = casualCoreUrl;
	}

	public SolrServer getStatisticsSolrServer() {
		String url = getStatisticsCoreUrl();
		
		if (StringUtils.startsWith(url, "http")) {
			return SearchManager.getRemoteServer(url);
		}
		
		return SearchManager.getLocalServer(url);
	}

	public String getStatisticsCoreUrl() {
		return StringUtils.defaultString(statisticsCoreUrl, "core-log");
	}

	public void setStatisticsCoreUrl(String statisticsCoreUrl) {
		this.statisticsCoreUrl = statisticsCoreUrl;
	}

	public String getOpendataCoreUrl() {
		return opendataCoreUrl;
	}

	public void setOpendataCoreUrl(String opendataCoreUrl) {
		this.opendataCoreUrl = opendataCoreUrl;
	}

	public String getOuterSourceUrl() {
		return outerSourceUrl;
	}

	public void setOuterSourceUrl(String outerSourceUrl) {
		this.outerSourceUrl = outerSourceUrl;
	}

	public Integer getOtherMltQANum() {
		return otherMltQANum != null ? otherMltQANum : DEFAULT_OTHER_MLT_QA_NUM;
	}

	public void setOtherMltQANum(Integer otherMltQANum) {
		this.otherMltQANum = otherMltQANum;
	}

	public Integer getDefaultTextCrmDepartmentId() {
		return defaultTextCrmDepartmentId != null ? defaultTextCrmDepartmentId : 1;
	}

	public void setDefaultTextCrmDepartmentId(Integer defaultTextCrmDepartmentId) {
		this.defaultTextCrmDepartmentId = defaultTextCrmDepartmentId;
	}

	public Boolean getEnableDebug() {
		return enableDebug != null ? enableDebug : DEFAULT_DEBUG;
	}

	public void setEnableDebug(Boolean enableDebug) {
		this.enableDebug = enableDebug;
	}

	public Boolean getEnableQAExplain() {
		return enableQAExplain != null ? enableQAExplain : DEFAULT_QA_EXPLAIN;
	}

	public void setEnableQAExplain(Boolean enableQAExplain) {
		this.enableQAExplain = enableQAExplain;
	}

	public Boolean getEnableContextMatch() {
		return enableContextMatch != null ? enableContextMatch : DEFAULT_CONTEXT_MATCH;
	}

	public void setEnableContextMatch(Boolean enableContextMatch) {
		this.enableContextMatch = enableContextMatch;
	}

	public Boolean getEnableRestrictToQaCategory() {
		return enableRestrictToQaCategory != null ? enableRestrictToQaCategory : DEFAULT_RESTRICT_TO_QA_CATEGORY;
	}

	public void setEnableRestrictToQaCategory(Boolean enableRestrictToQaCategory) {
		this.enableRestrictToQaCategory = enableRestrictToQaCategory;
	}

	public Boolean getEnableRestrictToKnowledgePoint() {
		return enableRestrictToKnowledgePoint != null ? enableRestrictToKnowledgePoint : DEFAULT_RESTRICT_TO_KNOWLEDGE_POINT;
	}

	public void setEnableRestrictToKnowledgePoint(Boolean enableRestrictToKnowledgePoint) {
		this.enableRestrictToKnowledgePoint = enableRestrictToKnowledgePoint;
	}

	public Boolean getEnablePhoneticHomonym() {
		return enablePhoneticHomonym != null ? enablePhoneticHomonym : DEFAULT_ENABLE_PHONETIC_HOMONYM;
	}

	public void setEnablePhoneticHomonym(Boolean enablePhoneticHomonym) {
		this.enablePhoneticHomonym = enablePhoneticHomonym;
	}

	public Boolean getEnableEssentialKeywordMode() {
		return enableEssentialKeywordMode != null ? enableEssentialKeywordMode : DEFAULT_ENABLE_ESSENTIAL_KEYWORD_MODE;
	}

	public void setEnableEssentialKeywordMode(Boolean enableEssentialKeywordMode) {
		this.enableEssentialKeywordMode = enableEssentialKeywordMode;
	}

	public Boolean getEnableTextCrm() {
		return enableTextCrm != null ? enableTextCrm : DEFAULT_ENABLE_TEXT_CRM;
	}

	public void setEnableTextCrm(Boolean enableTextCrm) {
		this.enableTextCrm = enableTextCrm;
	}

	public Boolean getEnableMultiLocale() {
		return enableMultiLocale != null ? enableMultiLocale : DEFAULT_ENABLE_MULTI_LOCALE;
	}

	public void setEnableMultiLocale(Boolean enableMultiLocale) {
		this.enableMultiLocale = enableMultiLocale;
	}

	public Boolean getEnableScenario() {
		return enableScenario != null ? enableScenario : DEFAULT_ENABLE_SCENARIO;
	}

	public void setEnableScenario(Boolean enableScenario) {
		this.enableScenario = enableScenario;
	}

	public Boolean getEnableAutoLearning() {
		return enableAutoLearning != null ? enableAutoLearning : DEFAULT_ENABLE_AUTO_LEARNING;
	}

	public void setEnableAutoLearning(Boolean enableAutoLearning) {
		this.enableAutoLearning = enableAutoLearning;
	}

	public Boolean getEnableRateLimitByIP() {
		return enableRateLimitByIP != null ? enableRateLimitByIP : DEFAULT_ENABLE_RATE_LIMIT_BY_IP_ADDR;
	}

	public void setEnableRateLimitByIP(Boolean enableRateLimitByIP) {
		this.enableRateLimitByIP = enableRateLimitByIP;
	}

	public Boolean getEnableRateLimitByQAContext() {
		return enableRateLimitByQAContext != null ? enableRateLimitByQAContext : DEFAULT_ENABLE_RATE_LIMIT_BY_QA_CONTEXT;
	}

	public void setEnableRateLimitByQAContext(Boolean enableRateLimitByQAContext) {
		this.enableRateLimitByQAContext = enableRateLimitByQAContext;
	}

	public String getAllowedLocales() {
		return allowedLocales;
	}

	public List<HitHotLocale> getAllowedLocalesList() {
		List<HitHotLocale> l = new ArrayList<>();

		if (StringUtils.isNotEmpty(allowedLocales)) {
			for (String ln: StringUtils.split(allowedLocales, ",")) {
				try {
					l.add(HitHotLocale.valueOf(ln));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return l;
	}

	public void setAllowedLocales(String allowedLocales) {
		this.allowedLocales = allowedLocales;
	}

	public Integer getThreshold1() {
		return threshold1 != null ? threshold1 : DEFAULT_THRESHOLD_1;
	}

	public void setThreshold1(Integer threshold1) {
		this.threshold1 = threshold1;
	}

	public Integer getThreshold2() {
		return threshold2 != null ? threshold2 : DEFAULT_THRESHOLD_2;
	}

	public void setThreshold2(Integer threshold2) {
		this.threshold2 = threshold2;
	}

	public Integer getThreshold3() {
		return threshold3 != null ? threshold3 : DEFAULT_THRESHOLD_3;
	}

	public void setThreshold3(Integer threshold3) {
		this.threshold3 = threshold3;
	}

	public Integer getRateLimitByIpPerSec() {
		return rateLimitByIpPerSec != null ? rateLimitByIpPerSec : DEFAULT_RATE_LIMIT_BY_IP_PER_SECOND;
	}

	public void setRateLimitByIpPerSec(Integer rateLimitByIpPerSec) {
		this.rateLimitByIpPerSec = rateLimitByIpPerSec;
	}

	public Integer getRateLimitByQAContextPerSec() {
		return rateLimitByQAContextPerSec != null ? rateLimitByQAContextPerSec : DEFAULT_RATE_LIMIT_BY_QA_CONTEXT_PER_SECOND;
	}

	public void setRateLimitByQAContextPerSec(Integer rateLimitByQAContextPerSec) {
		this.rateLimitByQAContextPerSec = rateLimitByQAContextPerSec;
	}

	public Integer getTooManyRepeatThreshold() {
		return tooManyRepeatThreshold != null ? tooManyRepeatThreshold : DEFAULT_TOO_MANY_REPEAT_THRESHOLD;
	}

	public void setTooManyRepeatThreshold(Integer tooManyRepeatThreshold) {
		this.tooManyRepeatThreshold = tooManyRepeatThreshold;
	}

	public Integer getBoost1() {
		return boost1 != null ? boost1 : DEFAULT_BOOST_1;
	}

	public void setBoost1(Integer boost1) {
		this.boost1 = boost1;
	}

	public Integer getBoost2() {
		return boost2 != null ? boost2 : DEFAULT_BOOST_2;
	}

	public void setBoost2(Integer boost2) {
		this.boost2 = boost2;
	}

	public Integer getBoost3() {
		return boost3 != null ? boost3 : DEFAULT_BOOST_3;
	}

	public void setBoost3(Integer boost3) {
		this.boost3 = boost3;
	}

	public String getForceIgnoreCharacters() {
		return forceIgnoreCharacters;
	}

	public void setForceIgnoreCharacters(String forceIgnoreCharacters) {
		this.forceIgnoreCharacters = forceIgnoreCharacters;
		foreceIgnoreCharacterSetCache = null;
	}

	@Transient
	Set<String> foreceIgnoreCharacterSetCache = null;

	public boolean isForceIgnoreCharacter(String toBeFind) {
		if (foreceIgnoreCharacterSetCache == null) {
			foreceIgnoreCharacterSetCache = new HashSet<String>();

			if (StringUtils.isNotEmpty(forceIgnoreCharacters)) {
				for (char c: forceIgnoreCharacters.toCharArray()) {
					foreceIgnoreCharacterSetCache.add(new String(new char[] {c}));
				}
			}
		}

		return foreceIgnoreCharacterSetCache.contains(toBeFind);
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}

	public long getPermission() {
		return permission;
	}

	public void setPermission(long permission) {
		this.permission = permission;
	}

	@Deprecated
	public String getApikey() {
		return apikey;
	}

	@Deprecated
	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	@Deprecated
	public String getLineBotChannelId() {
		return lineBotChannelId;
	}

	@Deprecated
	public void setLineBotChannelId(String lineBotChannelId) {
		this.lineBotChannelId = lineBotChannelId;
	}

	public String getLineBotConfigJson() {
		return lineBotConfigJson;
	}

	public void setLineBotConfigJson(String lineBotConfigJson) {
		this.lineBotConfigJson = lineBotConfigJson;
	}

	public String getFaqJson() {
		return faqJson == null ? "[]" : faqJson;
	}

	public void setFaqJson(String faqJson) {
		this.faqJson = faqJson;
	}

	public String getQaMatchCtrlFlow() {
		return qaMatchCtrlFlow;
	}

	public void setQaMatchCtrlFlow(String qaMatchCtrlFlow) {
		this.qaMatchCtrlFlow = qaMatchCtrlFlow;
	}
	
	public String getKmsUrl() {
		return kmsUrl;
	}
	public void setKmsUrl(String kmsUrl) {
		this.kmsUrl = kmsUrl;
	}

	public String getAllowedOptionsJson() {
		return allowedOptionsJson;
	}

	public void setAllowedOptionsJson(String allowedOptionsJson) {
		this.allowedOptionsJson = allowedOptionsJson;
	}
	
	public Boolean getEnableQaAudit() {
		return enableQaAudit == null ? DEFAULT_ENABLE_QA_AUDIT : enableQaAudit;
	}

	public void setEnableQaAudit(Boolean enableQaAudit) {
		this.enableQaAudit = enableQaAudit;
	}

	public Boolean getEnableUserFrontEnd() {
		return enableUserFrontEnd != null ? enableUserFrontEnd : DEFAULT_ENABLE_USER_FRONTEND;
	}

	public void setEnableUserFrontEnd(Boolean enableUserFrontEnd) {
		this.enableUserFrontEnd = enableUserFrontEnd;
	}

	public Integer getMaxBatchUploadSize() {
		return maxBatchUploadSize == null ? DEFAULT_MAX_BATCH_UPLOAD_SIZE : maxBatchUploadSize;
	}

	public void setMaxBatchUploadSize(Integer maxBatchUploadSize) {
		this.maxBatchUploadSize = maxBatchUploadSize;
	}

	// Static methods
	public static boolean isStatisticOnly(Tenant t) {
		return (t.getPermission() == 1024);
	}

	public static synchronized int saveOrUpdate(Tenant setting) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Tenant old = get(setting.getId());
			ses.saveOrUpdate(setting);
			tx.commit();

			if (old == null) {
				initDefaultRobotFormalAnswer(setting);
				QAChannel.checkData(setting.getId());
				QAUserType.checkData(setting.getId());
			}
			
			if (old != null) {
				if (!StringUtils.equals(old.getAllowedCustomDataFieldNames(), setting.getAllowedCustomDataFieldNames())) {
					CustomDataDictionary.clearCache(setting.getId());
					CustomDataDictionary.clear(setting.getId());
				}

				EventCenter.fireEvent(Tenant.class.getName(), setting.getId(), "reload", null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return setting.getId();
	}

	public static synchronized Tenant get(Integer id) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (Tenant)ses.get(Tenant.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}


	public static synchronized void delete(Integer id) throws Exception {
		try {
			Tenant phts = get(id);
			delete(phts);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	public static synchronized void delete(Tenant t) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(t);
			tx.commit();
			
			EventCenter.fireEvent(Tenant.class.getName(), t.getId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Tenant> list() {
		List<Tenant> result = new ArrayList<Tenant>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(Tenant.class).addOrder(Order.asc("id"));
			ct.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static Tenant getByLineBotChannelId(String cid) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(Tenant.class);
			ct.add( Restrictions.eq("lineBotChannelId", cid ) );

			Tenant phts = (Tenant)ct.uniqueResult();
			return phts;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static Tenant login(String loginName, String password) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(Tenant.class).addOrder(Order.asc("id"));
			ct.add( Restrictions.eq("email", loginName ) );
			ct.add( Restrictions.eq("pwd", password ) );
			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		if (result.size() > 0) {
			return (Tenant)result.get(0);
		}
		return null;
	}

	public static String genEmailVerifyCode() {
		int length = 8;
		String hs = "";

		while (length > 0) {
			hs += StringUtils.leftPad(Integer.toHexString((int)(Math.random() * 65536)), 4, '0');
			length -= 4;
		}

		return hs;
	}

	public static String genApikey() {
		int length = 16;
		String hs = "";

		while (length > 0) {
			hs += StringUtils.leftPad(Integer.toHexString((int)(Math.random() * 65536)), 4, '0');
			length -= 4;
		}

		System.out.println("hs:" + hs + ":" + hs.length());

		return getVerifyCode(hs) + hs;
	}

	static String getVerifyCode(String apikeyBase) {
		long verifyCode = 0;

		for (char c: apikeyBase.toCharArray()) {
			if (verifyCode % 2 != 0) {
				verifyCode *= c;
			}
			else {
				verifyCode += c;
			}
		}

		return Long.toHexString(verifyCode).substring(0, 4);
	}

	public static boolean isValidApikey(String apikey) {
		if (apikey == null || apikey.length() != 20) {
			return false;
		}

		String apikeyLowered = apikey.toLowerCase();
		String verifyCode = apikeyLowered.substring(0, 4);
		String apikeyBase = apikeyLowered.substring(4);

		return verifyCode.equals(getVerifyCode(apikeyBase));
	}

	public static Tenant getFromSession(HttpSession sess) {
		Tenant tenant = (Tenant)sess.getAttribute(Tenant.SESSION_KEY);
		return tenant;
	}

	public static void setSession(HttpSession sess, Tenant tenant) {
		sess.setAttribute(Tenant.SESSION_KEY, tenant);
		
		/**
		 * 當要把 Tenant 設定到 session 時，判斷目前用戶是否有專屬於該 Tenant 的角色，有的話，一併設定到 session 當中
		 */
		TenantAdminGroup.checkTenantAndAdminThenSetSession(sess);
	}
	
	public synchronized static void initDefaultRobotFormalAnswer(Tenant target) {
		List<Tenant> targetTenants = new ArrayList<Tenant>();
		if(target == null) {
			targetTenants = list();
		}
		else {
			targetTenants.add(target);
		}
		
		Map<String, List<String>> defaultAnswerMap = null;
		for(Tenant t : targetTenants) {
			try {
				if(defaultAnswerMap == null) {
					QAContext fakeCtx = QAContextManager.create(String.format("test_%s", "" + System.currentTimeMillis()));
					fakeCtx.setLocale(HitHotLocale.zh_TW);
					defaultAnswerMap = QAUtil.getInstance(t).getFormalAnswerReplacer(fakeCtx).getDefaultRobotFormalAnswerMap(fakeCtx);
				}
				RobotFormalAnswers.saveMapForInit(t.getId(), defaultAnswerMap);
			}
			catch(Exception ignore) {
				ignore.printStackTrace();
			}
		}
	}
	
	/**
	 * 從判斷用戶是否具備 Tenant access 權限
	 * 
	 * 若 request 中有 apikey，會判斷 apikey 的公司別
	 * 若沒有，則會看是否為 testByAdminMode，如果是 testByAdminMode 會
	 * 需要 session 內有 AdminUser 帳號，且 request 的 tid 參數是該用戶可管理的公司。
	 * 
	 * @param request
	 * @param testByAdminMode
	 * @return
	 */
	public static Tenant getFromRequest(HttpServletRequest request, boolean testByAdminMode) {
		String apikey = QAUtil.getStringParameterDefaultIfEmpty(request, "apikey", null);

		Tenant t = null;
		if (apikey != null) {
			Apikey key = Apikey.getByApiKey(apikey);

			if (key != null && key.isValid()) {
				t = key.getTenant();

				if (t != null) {
					key.incCallCounter();
					Apikey.saveOrUpdate(key);
				}
			}
		}
		else if (testByAdminMode && request.getParameter("tid") != null) {
			AdminUser user = AdminUserFacade.getInstance().getFromSession(request.getSession());
			int tid = Integer.parseInt(request.getParameter("tid"));

			if (user != null) {
				user.getTenantIdSet().contains(tid);
				t = Tenant.get(tid);
			}
		}
		
		return t;
	}
	
	/**
	 * Baseurl also contains contextPath
	 **/
	public String getBaseUrl(String channelCode) {
		try {
			JSONObject botCfg = new JSONObject(StringUtils.defaultString(getLineBotConfigJson(), "{}"));
			JSONObject targetCfg = new JSONObject();
			
			if (channelCode != null && botCfg.has(channelCode)) {
				targetCfg = botCfg.optJSONObject(channelCode);
				
				if (!targetCfg.has("domainUrl")) {
					targetCfg = botCfg;
				}
			} else {
				targetCfg = botCfg;
			}
			
			if (targetCfg.has("domainUrl") && StringUtils.isNotBlank(targetCfg.getString("domainUrl"))) {
				return targetCfg.getString("domainUrl") + WiseSystemConfig.get().getContextPath();
			} else {
				return WiseSystemConfig.get().getFullUrlBase();
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
			return WiseSystemConfig.get().getFullUrlBase();
		}
	}
}
