package com.intumit.solr.robot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.TemplateUtil.Collector;
import com.intumit.solr.robot.dictionary.CategorizedKeyValuePair;
import com.intumit.solr.robot.dictionary.CategorizedValueParser;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qadialog.GroovyUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.robot.qaplugin.QAPlugin;
import com.intumit.solr.robot.qarule.ForwardToCrmRule;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.RateLimiter;
import com.intumit.solr.util.WiSeUtils;

import flexjson.JSONSerializer;
import groovy.lang.Binding;

public class QAContext implements Serializable {

	static final long serialVersionUID = 6871924323597721880L;

	public static final String QUESTION_CHANGE_LISTENER = "_QCL";

	public static final String REQ_ATTR_RECONSTRUCTED_QUESTION = "_RQ";
	public static final String REQ_ATTR_NLP_RESULT = "_NLPR";
	private static final String CTX_ATTRKEY_USER_CLUE = "_USER_CLUE";

	public static final String EVENT_SOURCE_INPUTBOX = "InputBox";
	public static final String EVENT_SOURCE_UNKNOWN = "Unknown";
	public static final String EVENT_SOURCE_TYPE_KEYIN = "keyin";
	public static final String EVENT_SOURCE_TYPE_BUTTON = "button";
	public static final String EVENT_SOURCE_TYPE_SCHEDULE = "schedule";
	public static final String EVENT_SOURCE_TYPE_UNKNOWN = "unknown";

	public static enum QUESTION_TYPE {
		PROFESSIONAL,
		INTERCEPTED,
		ANSWER_OPTION,
		CASUAL,
		PREPARE_FORWARD,
		ANSWER_FORWARD,
		ANSWER_FORWARD_OPTION,
		BAD_QUESTION,
		REPEAT_TOO_MANY_TIMES,
		UNKNOWN,
		WELCOME,
	}

	public static enum ANSWER_TYPE {
		PROFESSIONAL,
		INTERCEPTED,
		SEARCH,
		MLT_ONLY,
		PROFESSIONAL_PLUGIN, // This is for customized plugin
		QA_PATTERN,
		CASUAL,
		NO_ANSWER,
		FORWARD,
		STAY_WITH_ROBOT,
		UNKNOWN,
		DIALOG,
		DIALOG_SWITCH,
		WELCOME,
		REPEAT_TOO_MANY_TIMES,
		PREPARE_FORWARD, 
		USER_BINDING, // 綁定流程
	}

	public static enum NO_ANSWER_OPTION {
		CONTINUE("forward.option.stayWithRobot"),
		WEB_CHAT("forward.option.webChat"),
		VOICE_CALL("forward.option.voiceCall"),
		VIDEO_CALL("forward.option.videoCall"),
		MESSAGE_BOARD("forward.option.messageBoard"),
		;

		String title;
		NO_ANSWER_OPTION(String title) {
			this.title = title;
		}
		public String getTitle() {
			return title;
		}
	}

	String contextId = null; // qaId
	long qastamp = 0;
	RateLimiter rateLimiter = null;
	String conversations = null;
	EventType eventType = null;
	EventType lastEventType = null;
	
	// 存放來源標籤
	String eventSource = null;
	String lastEventSource = null;
	
	// 用來保存本次事件（或者是問題）的來源類型，default 為 keyin。代表是由使用者打字輸入的
	// 可能的值還有 button，schedule...（代表由點選按鈕，或者由排程觸發）
	String eventSourceType = null;
	String lastEventSourceType = null;
	
	QUESTION_TYPE questionType = QUESTION_TYPE.UNKNOWN;
	QUESTION_TYPE lastQuestionType = QUESTION_TYPE.UNKNOWN;
	ANSWER_TYPE answerType = ANSWER_TYPE.UNKNOWN;
	ANSWER_TYPE lastAnswerType = ANSWER_TYPE.UNKNOWN;

	HitHotLocale locale = null;
	Tenant tenant = null;
	String lastQuestion = null;
	QA lastQA = null;
	DictionaryDatabase[] lastKPs = null;

	String originalQuestionTime = null;
	String originalQuestion = null;
	String currentQuestion = null;
	QA currentQA = null;
	DictionaryDatabase[] currentKPs = null;

	String restrictToQaCategory = null;
	String lastQaCategory = null;
	String eservice = null;
	String qaChannel = null;
	String userType = QAUserType.DEFAULT_USERTYPE_CODE;
	String firstBankSpecialNext = "";
	String lastFirstBankSpecialNext = "";

	Boolean forceToggleContextThisTime = null;
	boolean hasDirectAnswer = false;
	boolean clientSupportHtml = true;
	boolean fromKeep = false;
	Map<String, Object> response = new HashMap<String, Object>();
	StringBuilder answerText = null;
	Map<String, Object> lastResponse = new HashMap<String, Object>();
	String lastAnswerText = null;

	String icspid;

	int noResultCount = 0;
	int repeatSameQuestionCount = 0;
	int forwardDep = 0;

	JSONObject lineUserProfile = null;
	JSONObject interceptedPlugins = null;
	JSONObject interceptedPluginsKeep = null;
	
	List<Option> showedOptionsInLastAnswer = new ArrayList<>();

	JSONObject interceptedPluginsForNextQuestion = null;

	// 存放哪些 option menu 是本次 Answer 要給使用者的（只放 key，實際 Menu 在 optionMenuMap 中）
	List<String> showedOptionMenuKeys = new ArrayList<>();
	List<String> showedOptionMenuKeysInLastAnswer = new ArrayList<>();

	// 用來儲存 OptionMenu(s)，不隨 shift 清除，但應該要在被 offerMenu 時決定要不要 clear
	Map<String, OptionMenu> optionMenuMap = new HashMap<>();
	
	// 回合結束時要清掉的選單
	Set<String> optionMenuKeysToBeRemove = new HashSet<>();
	
	Stack<List<String>> showedMenuHistories = new Stack<List<String>>();

	/**
	 * A map for storing attributes, scope is restricted to single request. It
	 * will be move to lastReqAttr and cleared at begin of next request.
	 */
	Map<String, Object> reqAttr = new HashMap<String, Object>();

	/**
	 * A map for storing attributes during last request.
	 */
	Map<String, Object> lastReqAttr = new HashMap<String, Object>();

	/**
	 * A map for storing context level attributes, scope is restricted to single
	 * context. It will be cleared when this qa context be recycled.
	 */
	Map<String, Object> ctxAttr = new HashMap<String, Object>();

	long accessTimestamp = -1;

	/**
	 * For Debug
	 */
	List<Object[]> explain = null;

	public void shift() {
		synchronized (this) {
			qastamp++;
			lastQuestion = currentQuestion;
			lastAnswerText = answerText != null ? answerText.toString() : null;
			lastQA = currentQA;
			lastKPs = currentKPs;
			
			originalQuestion = null;
			currentQuestion = null;
			currentQA = null;
			currentKPs = null;
			explain = null;

			lastEventSource = eventSource;
			lastEventSourceType = eventSourceType;
			lastEventType = eventType;
			eventType = null;
			eventSource = null;
			eventSourceType = null;
			// for performance, just swap two pointer
			lastReqAttr.clear();
			Map<String, Object> tmp = lastReqAttr;
			lastReqAttr = reqAttr;
			reqAttr = tmp;
			reqAttr.put("_PATH_KEY", "__shifted__");

			accessTimestamp = System.currentTimeMillis();

			interceptedPlugins = interceptedPluginsForNextQuestion;

			while (showedMenuHistories.size() > 10) {
				synchronized (showedMenuHistories) {
					showedMenuHistories.remove(showedMenuHistories.size() - 1);
				}
			}
			if (showedOptionMenuKeys.size() > 0) {
				showedMenuHistories.push(showedOptionMenuKeys);
				
				for (String showedKey: showedOptionMenuKeys) {
					OptionMenu m = optionMenuMap.get(showedKey);
					
					// 正常 Lifetime.REQ 會自己被調動狀態，ONETIME 就強制要移除
					if (m != null && m.getLifetime() == Lifetime.ONETIME) {
						optionMenuKeysToBeRemove.add(showedKey);
					}
				}
			}
			showedOptionMenuKeysInLastAnswer = showedOptionMenuKeys;
			showedOptionMenuKeys = new ArrayList<>();
			/*showedOptionsInLastAnswer = null;// optionsForShowInAnswer;
			if (showedOptionsInLastAnswer != null && interceptedPlugins != null) {
				showedOptionsInLastAnswerKeep = showedOptionsInLastAnswer;
				interceptedPluginsKeep = interceptedPlugins;
			}*/
			fromKeep = false;

			lastFirstBankSpecialNext = firstBankSpecialNext;
			firstBankSpecialNext = "";

			interceptedPluginsForNextQuestion = null;
			// optionsForShowInAnswer = null;

			lastQuestionType = questionType;
			lastAnswerType = answerType;
			questionType = QUESTION_TYPE.UNKNOWN;
			answerType = ANSWER_TYPE.UNKNOWN;
			answerText = null;
			hasDirectAnswer = false;

			lastResponse = response;
			response = new HashMap<String, Object>();
		}
	}

	Stack<QAContext> stateStack = new Stack<QAContext>();

	/**
	 * Copy all fields without stateStack & explain
	 * 
	 * @param octx copy from octx
	 * @param nctx copy to nctx
	 * @return nctx
	 */
	QAContext copyWithoutStateStackAndExplain(QAContext octx, QAContext nctx) {
		nctx.contextId = octx.contextId;
		nctx.qastamp = octx.qastamp;
		nctx.conversations = octx.conversations;
		nctx.questionType = octx.questionType;
		nctx.lastQuestionType = octx.lastQuestionType;
		nctx.answerType = octx.answerType;
		nctx.lastAnswerType = octx.lastAnswerType;
		nctx.locale = octx.locale;
		nctx.lastQuestion = octx.lastQuestion;
		nctx.originalQuestion = octx.originalQuestion;
		nctx.currentQuestion = octx.currentQuestion;
		nctx.restrictToQaCategory = octx.restrictToQaCategory;
		nctx.lastQaCategory = octx.lastQaCategory;
		nctx.eservice = octx.eservice;
		nctx.qaChannel = octx.qaChannel;
		nctx.userType = octx.userType;
		nctx.forceToggleContextThisTime = octx.forceToggleContextThisTime;
		nctx.hasDirectAnswer = octx.hasDirectAnswer;
		nctx.clientSupportHtml = octx.clientSupportHtml;
		nctx.fromKeep = octx.fromKeep;
		nctx.lastAnswerText = octx.lastAnswerText;
		nctx.icspid = octx.icspid;
		nctx.noResultCount = octx.noResultCount;
		nctx.repeatSameQuestionCount = octx.repeatSameQuestionCount;
		nctx.forwardDep = octx.forwardDep;
		nctx.accessTimestamp = octx.accessTimestamp;
		nctx.eventType = octx.eventType;
		nctx.lastEventType = octx.lastEventType;
		nctx.eventSource = octx.eventSource;
		nctx.lastEventSource = octx.lastEventSource;
		nctx.eventSourceType = octx.eventSourceType;
		nctx.lastEventSourceType = octx.lastEventSourceType;
		
		// 這幾個可假設不會變動內容 
		nctx.tenant = octx.tenant;
		nctx.currentQA = octx.currentQA;
		nctx.lastQA = octx.lastQA;
		nctx.lastKPs = octx.lastKPs;
		nctx.currentKPs = octx.currentKPs;
		nctx.rateLimiter = octx.rateLimiter;
		
		// 皆下來這些有 Bug，List / JSONObject / Map 等非 primitive type 都是 reference 應該要 clone 才對
		nctx.answerText = octx.answerText != null ? new StringBuilder(octx.answerText) : octx.answerText;
		nctx.response.putAll(octx.response);
		nctx.lastResponse.putAll(octx.lastResponse);
		nctx.lineUserProfile = octx.lineUserProfile != null ? new JSONObject(octx.lineUserProfile) : octx.lineUserProfile;
		nctx.interceptedPlugins = octx.interceptedPlugins != null ? new JSONObject(octx.interceptedPlugins) : octx.interceptedPlugins;
		nctx.interceptedPluginsKeep = octx.interceptedPluginsKeep != null ? new JSONObject(octx.interceptedPluginsKeep) : octx.interceptedPluginsKeep;
		nctx.showedOptionsInLastAnswer.addAll(octx.showedOptionsInLastAnswer);
		nctx.showedMenuHistories.addAll(octx.showedMenuHistories);
		nctx.interceptedPluginsForNextQuestion = octx.interceptedPluginsForNextQuestion != null ? new JSONObject(octx.interceptedPluginsForNextQuestion) : octx.interceptedPluginsForNextQuestion;
		nctx.showedOptionMenuKeys.addAll(octx.showedOptionMenuKeys);
		nctx.showedOptionMenuKeysInLastAnswer.addAll(octx.showedOptionMenuKeysInLastAnswer);
		nctx.optionMenuKeysToBeRemove.addAll(octx.optionMenuKeysToBeRemove);
		nctx.optionMenuMap.putAll(octx.optionMenuMap);
		nctx.reqAttr.putAll(octx.reqAttr);
		nctx.lastReqAttr.putAll(octx.lastReqAttr);
		nctx.ctxAttr.putAll(octx.ctxAttr);
		/* 刻意不 clone 這兩個
		nctx.explain = octx.explain;
		nctx.stateStack = octx.stateStack;
		 */

		return nctx;
	}

	public void snapshot() {
		synchronized (this) {
			QAContext snapshoted = copyWithoutStateStackAndExplain(this, new QAContext());
			stateStack.add(snapshoted);
		}
	}

	public void rewind() {
		synchronized (this) {
			QAContext popup = stateStack.pop();
			copyWithoutStateStackAndExplain(popup, this);
		}
	}
	
	public QAContext cloneWithoutStateStackAndExplain() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			
			oos.writeObject(this);
			byte[] b = baos.toByteArray();
			oos.close();
			baos.close();
			
			ByteArrayInputStream bais = new ByteArrayInputStream(b);
			ObjectInputStream ois = new ObjectInputStream(bais);
			QAContext nctx = (QAContext)ois.readObject();
			
			return nctx;
		}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public String getContextId() {
		return contextId;
	}

	protected void setContextId(String contextId) {
		this.contextId = contextId;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	/**
	 * 是否目前允許問問題（限流機制）
	 * 若 Tenant 當中已經啟用 QAContext 限流，則會取得 RateLimiter 來判定，而且會直接等到 acquire 為止
	 * 如果 RateLimiter 是 null 則直接放行
	 * 
	 * @return
	 */
	public boolean acquireRequestPermit() {
		RateLimiter r = getRateLimiter();

		if (r != null) {
			return r.acquire();
		}
		return true;
	}

	/**
	 * 取得限流器
	 * 需要 Tenant.getEnableRateLimitByQAContext() == true 才會做動
	 * 否則會回傳 null
	 * 
	 * @return
	 */
	protected RateLimiter getRateLimiter() {
		if (tenant != null && tenant.getEnableRateLimitByQAContext()) {
			if (rateLimiter == null) {
				rateLimiter = RateLimiter.create(tenant.getRateLimitByQAContextPerSec());
			}
		}
		else {
			rateLimiter = null;
		}
		return rateLimiter;
	}

	protected void setRateLimiter(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	public HitHotLocale getLocale() {
		return locale != null ? locale : HitHotLocale.zh_TW;
	}

	public void setLocale(HitHotLocale locale) {
		this.locale = locale;
	}

	public QAUtil getUtil() {
		return QAUtil.getInstance(tenant.getId());
	}
	
	public String getOriginalQuestionTime() {
		return originalQuestionTime;
	}

	public void setOriginalQuestionTime(String originalQuestionTime) {
		this.originalQuestionTime = originalQuestionTime;
	}
	
	public String getCurrentQuestion() {
		return currentQuestion;
	}

	public void setCurrentQuestion(String currentQuestion) {
		String orig = this.currentQuestion;
		this.currentQuestion = currentQuestion;

		List<QuestionChangeListener> qcls = (List<QuestionChangeListener>) reqAttr
				.get(QUESTION_CHANGE_LISTENER);

		if (qcls != null) {
			for (QuestionChangeListener qcl : qcls) {
				qcl.changed(this, orig, currentQuestion);
			}
		}
	}

	public String getOriginalQuestion() {
		return originalQuestion;
	}

	public void setOriginalQuestion(String originalQuestion) {
		this.originalQuestion = originalQuestion;
	}

	public long getAccessTimestamp() {
		return accessTimestamp;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public EventType getLastEventType() {
		return lastEventType;
	}

	public void setLastEventType(EventType lastEventType) {
		this.lastEventType = lastEventType;
	}

	public String getEventSource() {
		return eventSource != null ? eventSource : QAContext.EVENT_SOURCE_INPUTBOX;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}

	public String getLastEventSource() {
		return lastEventSource != null ? lastEventSource : QAContext.EVENT_SOURCE_INPUTBOX;
	}

	public void setLastEventSource(String lastEventSource) {
		this.lastEventSource = lastEventSource;
	}

	/**
	 * 用來保存本次事件（或者是問題）的來源類型，default 為 keyin。代表是由使用者打字輸入的 
	 * 可能的值還有 button，schedule...（代表由點選按鈕，或者由排程觸發）
	 * 
	 * 來源如果是另一個題目產生的選單，就把題目編號存放在 eventSource 當中
	 * 
	 * 如果還有更進一步的資料，就建議存放在 reqAttr 當中，使用 eventSourceData 作為 key
	 * 資料型態為 JSONObject
	 * 
	 * 例如 eventSourceType = button
	 * 而 eventSource = COMMON_SENSE-12345
	 * 代表來源為按鈕，而該按鈕為 kid = 12345 編號的問答產生的
	 * 
	 * @return
	 */
	public String getEventSourceType() {
		return eventSourceType != null ? eventSourceType : QAContext.EVENT_SOURCE_TYPE_KEYIN;
	}

	public void setEventSourceType(String eventSourceType) {
		this.eventSourceType = eventSourceType;
	}

	public String getLastEventSourceType() {
		return lastEventSourceType != null ? lastEventSourceType : QAContext.EVENT_SOURCE_TYPE_KEYIN;
	}

	public void setLastEventSourceType(String lastEventSourceType) {
		this.lastEventSourceType = lastEventSourceType;
	}

	public QA getCurrentQA() {
		return currentQA;
	}

	public void setCurrentQA(QA currentQA) {
		this.currentQA = currentQA;
		
		// 設定標準答案的同時順便存入知識主題
		if (currentQA != null) {
			try {
				String standardQ = currentQA.getQuestion();
				
				if (standardQ != null) {
					DictionaryDatabase[] kps = KnowledgePointDictionary.search(tenant.getId(), standardQ.toCharArray(), getRestrictToQaCategory());
					
					if (kps != null && kps.length > 0) {
						setCurrentKPs(kps);
					}
				}
			}
			catch (Exception ignoreIt) {
				ignoreIt.printStackTrace();
			}
		}
	}

	public DictionaryDatabase[] getCurrentKPs() {
		return currentKPs;
	}

	public void setCurrentKPs(DictionaryDatabase[] currentKPs) {
		this.currentKPs = currentKPs;
	}

	public QUESTION_TYPE getQuestionType() {
		return questionType;
	}

	public void setQuestionType(QUESTION_TYPE questionType) {
		this.questionType = questionType;
	}

	public ANSWER_TYPE getAnswerType() {
		return answerType;
	}

	public void setAnswerType(ANSWER_TYPE answerType) {
		this.answerType = answerType;
	}

	public QUESTION_TYPE getLastQuestionType() {
		return lastQuestionType;
	}

	public void setLastQuestionType(QUESTION_TYPE lastQuestionType) {
		this.lastQuestionType = lastQuestionType;
	}

	public ANSWER_TYPE getLastAnswerType() {
		return lastAnswerType;
	}

	public void setLastAnswerType(ANSWER_TYPE lastAnswerType) {
		this.lastAnswerType = lastAnswerType;
	}

	public String getLastAnswerText() {
		return lastAnswerText;
	}

	public void setLastAnswerText(String lastAnswerText) {
		this.lastAnswerText = lastAnswerText;
	}

	/**
	 * this.hasDirectAnswer == true || currentQA != null
	 * 
	 * @return
	 */
	public boolean isHasDirectAnswer() {
		return hasDirectAnswer || currentQA != null;
	}

	public void setHasDirectAnswer(boolean hasDirectAnswer,
			ANSWER_TYPE answerType) {
		this.hasDirectAnswer = hasDirectAnswer;
		this.setAnswerType(answerType);
		this.noResultCount = 0;
	}

	public String getLastFirstBankSpecialNext() {
		return lastFirstBankSpecialNext;
	}

	public void setLastFirstBankSpecialNext(String lastFirstBankSpecialNext) {
		this.lastFirstBankSpecialNext = lastFirstBankSpecialNext;
	}

	public String getFirstBankSpecialNext() {
		return firstBankSpecialNext;
	}

	public void setFirstBankSpecialNext(String firstBankSpecialNext) {
		this.firstBankSpecialNext = firstBankSpecialNext;
	}

	public boolean isClientSupportHtml() {
		return clientSupportHtml;
	}

	public void setClientSupportHtml(boolean clientSupportHtml) {
		this.clientSupportHtml = clientSupportHtml;
	}

	public boolean isFromKeep() {
		return fromKeep;
	}

	public void setFromKeep(boolean fromKeep) {
		this.fromKeep = fromKeep;
	}

	public String getRestrictToQaCategory() {
		return restrictToQaCategory;
	}

	public void setRestrictToQaCategory(String lastQaCategory) {
		this.restrictToQaCategory = lastQaCategory;
	}

	public String getLastQaCategory() {
		return lastQaCategory;
	}

	public void setLastQaCategory(String lastQaCategory) {
		this.lastQaCategory = lastQaCategory;
	}

	public int getForwardDep() {
		return forwardDep;
	}

	public void setForwardDep(int forwardDep) {
		this.forwardDep = forwardDep;
	}

	public String getEservice() {
		return eservice;
	}

	public void setEservice(String eservice) {
		this.eservice = eservice;
	}

	public String getLastQuestion() {
		return lastQuestion;
	}

	public void setLastQuestion(String lastQuestion) {
		this.lastQuestion = lastQuestion;
	}

	public QA getLastQA() {
		return lastQA;
	}

	public void setLastQA(QA lastQA) {
		this.lastQA = lastQA;
	}

	public DictionaryDatabase[] getLastKPs() {
		return lastKPs;
	}

	public void setLastKPs(DictionaryDatabase[] lastKPs) {
		this.lastKPs = lastKPs;
	}

	public void addQuestionChangeListener(QuestionChangeListener qcl) {
		List<QuestionChangeListener> qcls = (List<QuestionChangeListener>) reqAttr
				.get(QUESTION_CHANGE_LISTENER);

		if (qcls == null) {
			qcls = new ArrayList<>();
			reqAttr.put(QUESTION_CHANGE_LISTENER, qcls);
		}

		qcls.add(qcl);
		qcl.listenerAdded(this, this.getCurrentQuestion());
	}

	public String getConversations() {
		return conversations;
	}

	public void setConversations(String lastConversation) {
		this.conversations = lastConversation;
	}

	public JSONObject getConversationsJson() {
		JSONObject cjson = null;
		try {
			List<ServiceLogEntity> logs = new ArrayList<ServiceLogEntity>();
			if (tenant != null && contextId != null) {
				logs = ServiceLogEntity.getFromQaId(tenant.getId(), contextId, tenant.getSessionExpirationSecond(), 1);
			}
			if (!logs.isEmpty()) {
				ServiceLogEntity log = logs.get(0);
				String conversations = log.getConversations();
				if (conversations != null) {
					cjson = new JSONObject(conversations);
				}
			} else {
				cjson = new JSONObject();
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		if (cjson == null) {
			cjson = new JSONObject();
		}

		if (!cjson.has("messages")) {
			try {
				cjson.put("messages", new JSONArray());
			}
			catch (JSONException ignore) {
				ignore.printStackTrace();
			}
		}

		return cjson;
	}

	public void setRequestAttribute(String key, Object val) {
		reqAttr.put(key, val);
	}

	public Object getRequestAttribute(String key) {
		return reqAttr.get(key);
	}

	public Map<String, Object> getRequestAttributes() {
		return reqAttr;
	}

	public void setIntents(Set<QAIntent> intents) {
		setRequestAttribute("intents", intents);
	}

	public void setIntents(QAIntent intent) {
		Set<QAIntent> intents = new HashSet<>();

		if (intent != null) intents.add(intent);
		setRequestAttribute("intents", intents);
	}
	
	public Set<QAIntent> getIntents() {
		Set<QAIntent> intents = (Set<QAIntent>)getRequestAttribute("intents");
		if (intents == null) {
			return new HashSet<>();
		}
		return intents;
	}

	public void setEntities(Set<QAEntity> entities) {
		setRequestAttribute("entities", entities);
	}

	public void setEntities(QAEntity entity) {
		Set<QAEntity> entities = new HashSet<>();

		if (entity != null) entities.add(entity);
		setRequestAttribute("entities", entities);
	}
	
	public Set<QAEntity> getEntities() {
		Set<QAEntity> entities = (Set<QAEntity>)getRequestAttribute("entities");
		if (entities == null) {
			return new HashSet<>();
		}
		return entities;
	}

	public Object getRequestAttribute(String key, Object defaultVal) {
		Object val = reqAttr.get(key);

		if (val == null) return defaultVal;

		return val;
	}

	public Object getLastRequestAttribute(String key) {
		return lastReqAttr.get(key);
	}

	public Object getLastRequestAttribute(String key, Object defaultVal) {
		Object val = lastReqAttr.get(key);

		if (val == null) return defaultVal;

		return val;
	}

	public Object getLastResponseAttribute(String key) {
		return lastResponse.get(key);
	}

	public Object getLastResponseAttribute(String key, Object defaultVal) {
		Object val = lastResponse != null ? lastResponse.get(key) : null;

		if (val == null) return defaultVal;

		return val;
	}
	
	public Object getResponseAttribute(String key) {
		return response.get(key);
	}

	public Object getCtxAttr(String key) {
		return ctxAttr.get(key);
	}

	public Map<String, Object> getCtxAttr() {
		return ctxAttr;
	}

	public void setCtxAttr(String key, Object val) {
		this.ctxAttr.put(key, val);
	}

	public int getNoResultCount() {
		return noResultCount;
	}

	public int getThenIncNoResultCount() {
		return this.noResultCount++;
	}

	public void setNoResultCount(int noResultCount) {
		this.noResultCount = noResultCount;
	}

	public int getRepeatSameQuestionCount() {
		return repeatSameQuestionCount;
	}

	public int incThenGetRepeatSameQuestionCount() {
		return ++this.repeatSameQuestionCount;
	}

	public void setRepeatSameQuestionCount(int repeatSameQuestionCount) {
		this.repeatSameQuestionCount = repeatSameQuestionCount;
	}

	public void setAnswerText(String text) {
		answerText = new StringBuilder(StringUtils.trimToEmpty(text));
	}

	public void prependAnswerText(String text) {
		if (answerText == null) {
			answerText = new StringBuilder();
		}
		answerText.insert(0, text);
	}

	public void appendAnswerText(String text) {
		if (answerText == null) {
			answerText = new StringBuilder();
		}
		answerText.append(text);
	}

	
	public QAContext appendNL() {
		return appendNL(1);
	}
	public QAContext appendNL(int times) {
		String NL = getQAChannelInstance().getUseHtmlNewline() ? "<br>" : "\n";
		
		appendAnswerText(StringUtils.repeat(NL, times));
		return this;
	}

	public StringBuilder getAnswerText() {
		return answerText;
	}

	public boolean hasAnswerText() {
		return answerText != null && answerText.length() > 0;
	}

	public void appendIcspidIfEnterTesterMode() {
		if (getTenant() != null && getTenant().getEnableDebug()) {
			String icspid = StringUtils.trim(getIcspid());
			appendAnswerText((isClientSupportHtml() ? "<br/>" : "\n")
					+ "（<bean:message key='global.binding'/>DEVICE ID: "
					+ (StringUtils.isBlank(icspid) ? "<bean:message key='global.without'/>"
							: icspid) + "）");
		}
	}

	/*public List<Option> getShowedOptionsInLastAnswer() {
		return showedOptionsInLastAnswer;
	}

	public void setShowedOptionsInLastAnswer(
			List<Option> showedOptionsInLastAnswer) {
		this.showedOptionsInLastAnswer = showedOptionsInLastAnswer;
	}

	public List<Option> getShowedOptionsInLastAnswerKeep() {
		return showedOptionsInLastAnswerKeep;
	}

	public void setShowedOptionsInLastAnswerKeep(
			List<Option> showedOptionsInLastAnswerKeep) {
		this.showedOptionsInLastAnswerKeep = showedOptionsInLastAnswerKeep;
	}*/

	public Boolean getForceToggleContextThisTime() {
		return forceToggleContextThisTime;
	}

	public void setForceToggleContextThisTime(Boolean forceToggleContextThisTime) {
		this.forceToggleContextThisTime = forceToggleContextThisTime;
	}

	public String getMessage(String key) {
		return MessageUtil.getMessage(HitHotLocale.toJavaLocale(getLocale()), key);
		}

	public String getMessage(String key, String[] args) {
		return MessageUtil.getMessageResources().getMessage(HitHotLocale.toJavaLocale(getLocale()), key, args);
	}

	public String getIcspid() {
		return icspid;
	}

	public void setIcspid(String icspid) {
		this.icspid = icspid;
	}
	
	public UserClue getUserClue() {
		return (UserClue)getCtxAttr(CTX_ATTRKEY_USER_CLUE);
	}
	
	public void setUserClue(UserClue clue) {
		setCtxAttr(CTX_ATTRKEY_USER_CLUE, clue);
		
		if (clue != null && clue.getNickname() != null) {
			setCtxAttr("NICKNAME", clue.getNickname());
		}
	}
	
	public JSONObject getLineUserProfile() {
		return lineUserProfile;
	}

	public void setLineUserProfile(JSONObject lineUserProfile) {
		this.lineUserProfile = lineUserProfile;
	}

	public JSONObject getInterceptedPlugins() {
		return interceptedPlugins;
	}

	public void setInterceptedPlugins(JSONObject interceptorPlugins) {
		this.interceptedPlugins = interceptorPlugins;
	}

	public JSONObject getInterceptedPluginData(String pId) {
		if (interceptedPlugins != null) {
			return interceptedPlugins.optJSONObject(pId);
		}

		return null;
	}

	public Set<String> getInterceptedPluginIds() {
		HashSet<String> ids = new HashSet<>();
		if (interceptedPlugins != null) {
			JSONArray names = interceptedPlugins.names();
			if (names != null) {
				for (int i = 0; i < names.length(); i++) {
					ids.add(names.optString(i));
				}
			}
		}

		return ids;
	}

	public JSONObject getInterceptedPluginsKeep() {
		return interceptedPluginsKeep;
	}

	public void setInterceptedPluginsKeep(JSONObject interceptorPluginsKeep) {
		this.interceptedPluginsKeep = interceptorPluginsKeep;
	}

	public JSONObject getInterceptedPluginDataKeep(String pId) {
		if (interceptedPluginsKeep != null) {
			return interceptedPluginsKeep.optJSONObject(pId);
		}

		return null;
	}

	public Set<String> getInterceptedPluginIdsKeep() {
		HashSet<String> ids = new HashSet<>();
		if (interceptedPluginsKeep != null) {
			JSONArray names = interceptedPluginsKeep.names();
			if (names != null) {
				for (int i = 0; i < names.length(); i++) {
					ids.add(names.optString(i));
				}
			}
		}

		return ids;
	}

	public void interceptNextQuestion(QAPlugin p, JSONObject pluginData) {
		if (interceptedPluginsForNextQuestion == null) {
			interceptedPluginsForNextQuestion = new JSONObject();
		}

		try {
			interceptedPluginsForNextQuestion.put(p.getId(),
					p.prepareInterceptorData(this, pluginData));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public QAUtil getQAUtil() {
		return QAUtil.getInstance(getTenant());
	}

	public CategorizedKeyValuePair addCategorizedKeyValuePair(String key,
			Object value, CategorizedKeyValuePair.Type type) {
		return addCategorizedKeyValuePair(new CategorizedKeyValuePair(key,
				value, type));
	}

	public CategorizedKeyValuePair addCategorizedKeyValuePair(
			CategorizedKeyValuePair kv) {
		List<CategorizedKeyValuePair> pairList = null;
		if (reqAttr.containsKey("ckvPair")) {
			pairList = (List<CategorizedKeyValuePair>) reqAttr.get("ckvPair");
		}
		else {
			pairList = new ArrayList<>();
			reqAttr.put("ckvPair", pairList);
		}

		pairList.add(kv);
		return kv;
	}

	public void setCategorizedKeyValuePair(
			List<CategorizedKeyValuePair> pairList) {

		reqAttr.put("ckvPair", pairList != null ? pairList
				: new ArrayList<CategorizedKeyValuePair>());
	}

	public List<CategorizedKeyValuePair> getCategorizedKeyValuePair() {
		if (reqAttr.containsKey("ckvPair")) {
			return (List<CategorizedKeyValuePair>) reqAttr.get("ckvPair");
		}
		return new ArrayList<>();
	}

	public boolean hasNumberKeyValuePair() {
		for (CategorizedKeyValuePair p : getCategorizedKeyValuePair()) {
			if (p.getType() == CategorizedKeyValuePair.Type.NUMBER) {
				return true;
			}
		}

		return false;
	}

	public String getNumberRangeQuery(int idx) {
		for (CategorizedKeyValuePair p : getCategorizedKeyValuePair()) {
			if (p.getType() == CategorizedKeyValuePair.Type.NUMBER) {
				if (idx-- == 0) return CategorizedValueParser.toNumberRangeQuery(p);
			}
		}

		return null;
	}

	public Number getNumber(int idx) {
		for (CategorizedKeyValuePair p : getCategorizedKeyValuePair()) {
			if (p.getType() == CategorizedKeyValuePair.Type.NUMBER) {
				if (idx-- == 0) return CategorizedValueParser.toNumber(p);
			}
		}

		return null;
	}

	public boolean hasTemporalKeyValuePair() {
		for (CategorizedKeyValuePair p : getCategorizedKeyValuePair()) {
			if (p.getType() == CategorizedKeyValuePair.Type.TEMPORAL) {
				return true;
			}
		}

		return false;
	}

	public String getTemporalRangeQuery(int idx) {
		for (CategorizedKeyValuePair p : getCategorizedKeyValuePair()) {
			if (p.getType() == CategorizedKeyValuePair.Type.TEMPORAL) {
				if (idx-- == 0) return CategorizedValueParser.toDateRangeQuery(p);
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return "QAContext [conversations=" + "{{TOO LONG TO PRINT}}"
				+ ",\n\t>> channel=" + qaChannel
				+ ",\n\t>> questionType=" + questionType
				+ ",\n\t>> answerType=" + answerType
				+ ",\n\t>> lastQuestion=" + lastQuestion
				+ ", lastQA=" + lastQA
				+ ",\n\t>> currentQuestion=" + currentQuestion
				+ ", currentQA=" + currentQA
				+ ",\n\t>> hasDirectAnswer=" + hasDirectAnswer
				+ ", clientSupportHtml=" + clientSupportHtml
				+ ",\n\t>> icspid=" + icspid
				+ ", noResultCount=" + noResultCount
				+ ", repeatSameQuestionCount=" + repeatSameQuestionCount
				+ ", interceptedPlugins=" + interceptedPlugins
				+ ",\n\t>> response=" + response
				+ ",\n\t>> answerText=" + answerText
				+ ",\n\t>> showedOptionsInLastAnswer=" + showedOptionsInLastAnswer
				+ ",\n\t>> interceptedPluginsForNextQuestion=" + interceptedPluginsForNextQuestion
				+ ",\n\t>> restrictToQaCategory=" + restrictToQaCategory
				+ ",\n\t>> reqAttr=" + reqAttr + "]";
	}

	public void appendExplain(String key, Object val) {
		if (explain == null) explain = new ArrayList<Object[]>();

		String pathKey = (String) getRequestAttribute("_PATH_KEY");
		if (val == null || val instanceof Serializable) explain.add(new Object[] { pathKey, key, val,
				System.currentTimeMillis() - accessTimestamp });
		else explain.add(new Object[] { pathKey, key, val.toString(), System.currentTimeMillis() - accessTimestamp });
	}

	public List<Object[]> getExplain() {
		return explain;
	}

	public boolean setResponseAttribute(String key, Object value) {
		response.put(key, value);
		return true;
	}
	
	public ArrayList<Map<String, Object>> getJSONArrayIMG() {
		if(answerText == null)
			return new ArrayList<Map<String, Object>> ();
		String output = answerText.toString();

		ArrayList<Map<String, Object>> linkDataList = TemplateUtil
				.collectByTagName(output, "G", new Collector() {
					@Override
					public Map<String, Object> call(String name, String val) {
						Map<String, Object> linkData = new HashMap<String, Object>();
						if (StringUtils.equals(name, "G")) {
			    				String stickerKey = StringUtils.substringBefore(val, ":");
							String link = stickerKey;
							
							RobotFormalAnswersSticker sticker = RobotFormalAnswersSticker.giveMeAnswer(tenant.getId(), stickerKey);
							if (sticker != null) {
								link = QAUtil.getFullUrlBase() + "/img/sticker/" + sticker.getAnswers();
							}

							linkData.put("link", link);
							linkData.put("title", stickerKey);
							linkData.put("type", "uri");
						}
						
						if (linkData.isEmpty()) linkData = null;
						return linkData;
					}
				});
		return linkDataList;
	}

	public ArrayList<Map<String, Object>> getJSONArrayLink() {
		if(answerText == null)
			return new ArrayList<Map<String, Object>> ();
		String output = answerText.toString();

		ArrayList<Map<String, Object>> linkDataList = TemplateUtil
				.collectByTagName(output, "L", new Collector() {
					@Override
					public Map<String, Object> call(String name, String val) {
						Map<String, Object> linkData = new HashMap<String, Object>();
						if (StringUtils.equals(name, "L")) {
							String linkName = StringUtils.substringBefore(val,
									":");
							String link = StringUtils.substringAfter(val, ":");

							linkData.put("link", link);
							linkData.put("title", linkName);
							linkData.put("type", "uri");
						}
						if (linkData.isEmpty())
							linkData = null;
						return linkData;
					}
				});
		return linkDataList;
	}
	
	public ArrayList<Map<String, Object>> getJSONArrayQ() {
		if(answerText == null)
			return new ArrayList<Map<String, Object>> ();
		String output = answerText.toString();

		ArrayList<Map<String, Object>> qDataList = TemplateUtil
				.collectByTagName(output, "Q", new Collector() {
					@Override
					public Map<String, Object> call(String name, String val) {
						Map<String, Object> qData = new HashMap<String, Object>();
						if (StringUtils.equals(name, "Q")) {
							String qName = StringUtils.substringBefore(val,
									":");
							String q = StringUtils.substringAfter(val, ":");

							qData.put("link", q);
							qData.put("title", qName);
							qData.put("type", "message");
						}
						if (qData.isEmpty())
							qData = null;
						return qData;
					}
				});
		return qDataList;
	}

	public JSONObject genResponseJSON() {
		try {
			// 應該只有 Transformer 們能產生最終 answerText
			if (answerText != null || isHasDirectAnswer()) {
				if (answerText == null) {
					answerText = new StringBuilder();
				}
				else {
					answerText = new StringBuilder(FunctionUtil.collectExecAndReplace(answerText.toString(), this));
				}
				
				QAChannel ch = QAChannel.get(getTenant().getId(), getQaChannel());
				
				if (ch != null) {
					switch (ch.getType()) {
						case LINE:
							new NaverLineAnswerTransformer().transform(ch, this);
							break;
						case RICH_TEXT:
							new RichTextAnswerTransformer().transform(ch, this);
							break;
						case FACEBOOK_MESSENGER:
							new FacebookAnswerTransformer().transform(ch, this);
							break;
						case SKYPE:
						case SLACK:
						case MICROSOFT_TEAMS:
							new BotframeworkGeneralAnswerTransformer().transform(ch, this);
							break;
						case ROBOT:
							new RobotAnswerTransformer().transform(ch, this);
							break;
						case PLAIN_TEXT_WITH_VOICE:
							new PlainTextVoinceAnswerTransformer().transform(ch, this);
							break;
						case ACTIONS_ON_GOOGLE:
							new ActionsOnGoogleAnswerTransformer().transform(ch, this);
							break;
						default: 
							new PlainTextAnswerTransformer().transform(ch, this);
							break;
					}
				}
				else {
					new PlainTextAnswerTransformer().transform(ch, this);
				}

				String output = getAnswerText().toString();
				response.put("output", output);
				ArrayList<Map<String, Object>> link = getJSONArrayLink();
				response.put("link", link);
				if (response.containsKey("outputVoice") && (answerType == ANSWER_TYPE.NO_ANSWER || answerType == ANSWER_TYPE.PROFESSIONAL_PLUGIN)) {
					response.put("outputVoice", getResponseAttribute("outputVoice").toString().replaceAll("\\<.*?>",""));
				}
			}
			else {
				answerText = new StringBuilder(StringUtils.trimToEmpty("{{F:NO_ANSWER}}"));
				answerType = QAContext.ANSWER_TYPE.NO_ANSWER;
				String output = getAnswerText().toString();
				response.put("output", output);
			}
			
			// 
			// 理論上從這裡之後，都不應該有任何程式去動到 output
			//
			
			if (answerType == ANSWER_TYPE.NO_ANSWER) {
				ArrayList<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>();
				NO_ANSWER_OPTION[] allOption = NO_ANSWER_OPTION.values();
				for (NO_ANSWER_OPTION option : allOption) {
					Map<String, Object> data = new HashMap<String, Object>();
					data.put("option", option.name());
					data.put("title", getMessage(option.getTitle()));
					datalist.add(data);
				}
				try {
					response.put("forward", new JSONArray(datalist));
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			if (getQAChannelInstance() != null) {
				response.put("channelType", getQAChannelInstance().getType().name());
			}

			if (getOriginalQuestion() != null) {
				response.put("originalQuestion", getOriginalQuestion());
				
				if (originalQuestionTime != null) {
					response.put("originalQuestionTime", originalQuestionTime);
				}
			}

			if (getCurrentQuestion() != null) {
				response.put("currentQuestion", getCurrentQuestion());
			}

			if (getLocale() != null) {
				response.put("locale", getLocale().name());
			}

			if (getRestrictToQaCategory() != null) {
				response.put("qaCategory", getRestrictToQaCategory());
			}
			
			if (getCurrentKPs() != null) {
				try {
					response.put("kps", new JSONArray(new JSONSerializer()
						.exclude("*.id", "*.class", "*.tenantId", "*.purposes", "*.purposesJson", "*.purposeSet", "*.enabled")
						.deepSerialize(getCurrentKPs())));
        			}
        			catch (JSONException e1) {
    					e1.printStackTrace();
    				}
			}
			
			if (getLastKPs() != null) {
				try {
					response.put("lastKps", new JSONArray(new JSONSerializer()
						.exclude("*.id", "*.class", "*.tenantId", "*.purposes", "*.purposesJson", "*.purposeSet", "*.enabled")
						.deepSerialize(getLastKPs())));
        			}
        			catch (JSONException e1) {
    					e1.printStackTrace();
    				}
			}

			if (eservice != null) {
				response.put("eservice", eservice);
			}

			if (userType != null) {
				response.put("userType", userType);
			}

			if (reqAttr.containsKey("entities")) {
				try {
					response.put("entities", new JSONArray(new JSONSerializer()
						.exclude("*.copy").exclude("*.class")
						.exclude("*.enabled").exclude("*.fromIndex")
						.exclude("*.subEntities").exclude("*.clazz")
						.exclude("*.id").exclude("*.tenantId")
						.deepSerialize(reqAttr.get("entities"))));
        			}
        			catch (JSONException e1) {
    					e1.printStackTrace();
    				}
    			}
			if (reqAttr.containsKey("intents")) {
    				try {
    					response.put("intents", new JSONArray(new JSONSerializer()
    							.exclude("*.copy").exclude("*.class")
    							.exclude("*.id").exclude("*.tenantId")
    							.exclude("*.enabled").exclude("*.clazz")
    							.deepSerialize(reqAttr.get("intents"))));
    				}
    				catch (JSONException e1) {
    						e1.printStackTrace();
    				}
			}

			if (getCurrentQA() != null) {
				Number score = (Number) currentQA.getFieldValue("score");
				if (score != null) {
					float fscore = score.floatValue();
					double confidenceFactor = QAUtil.AnswerConfidenceLevel
							.calConfidenceFactor(fscore);

					response.put("score", fscore);
					response.put("confidence", QAUtil.AnswerConfidenceLevel
							.findConfidenceLevel(fscore).getName());
					response.put("confidenceFactor", confidenceFactor);
				}

				Long kid = (Long) currentQA.getFieldValue("kid_l");
				MultiChannelAnswer mca = null;

				if (kid != null) {
					response.put("kid", kid);
					mca = MultiChannelAnswer.get(getTenant().getId(), currentQA.getId(), getQaChannel(), getUserType());
				}

				String qaCategory = (String) currentQA.getFieldValue("QA_CATEGORY_s");
				if (qaCategory != null) {
					response.put("currentQaCategory", qaCategory);
					lastQaCategory = qaCategory;
				}
				if (currentQA.getFieldValues("CATEGORY_ms") != null) {
					String category = Arrays.toString(currentQA.getFieldValues("CATEGORY_ms").toArray());
					category = category.replace("[", "").replace("]", "");
					if (category != null)
						response.put("currentCategory", category);
				}

				JSONObject extraParams = currentQA.getExtraParams();
				if (extraParams != null) {
					try {
						for (Object key : extraParams.keySet()) {
							response.put(key.toString(), extraParams.get(key));
						}
					}
					catch (Exception ignoreIt) {}
				}

				// mca 在這裡只有判斷 extraParameter 的用途，因此沒有用 MultiChannelAnswer.findNonEmptyAnswer。
				if (mca != null && StringUtils.isNotEmpty(mca.getExtraParameters())) {
					try {
						JSONObject ep = new JSONObject(mca.getExtraParameters());
						
						for (Object key : ep.keySet()) {
							response.put(key.toString(), ep.get(key));
						}
					}
					catch (Exception ignoreIt) {}
				}
			}

			// 處理 forward api object
			if ((Boolean) getRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, Boolean.FALSE)) {
				ArrayList<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>();
				NO_ANSWER_OPTION[] allOption = NO_ANSWER_OPTION.values();
				for (NO_ANSWER_OPTION option : allOption) {
					Map<String, Object> data = new HashMap<String, Object>();
					data.put("option", option.name());
					data.put("title", MessageUtil.getMessage(getLocale().getLocale(), option.getTitle()));
					datalist.add(data);
				}

				try {
					response.put("forward", new JSONArray(datalist));
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
			}

			if (eventType != null) {
				response.put("eventTypeChannel", eventType.getChannel());
				response.put("eventTypeCode", eventType.getCode());
				
				JSONObject eventData = (JSONObject)getRequestAttribute("eventData");
				if (eventData != null) 
					response.put("eventTypeData", eventData);
			}
			response.put("eventSource", getEventSource());
			response.put("eventSourceType", getEventSourceType());
			response.put("lastQaCategory", lastQaCategory);
			response.put("datetime", QAUtil.formatDatetime(Calendar.getInstance().getTime()));
			response.put("questionType", getQuestionType().name());
			response.put("answerType", getAnswerType().name());
			response.put("originalQuestionTime", originalQuestionTime);
			response.put("feedback", "0");
			response.put("feedbackId", new Date().getTime());
			if (forwardDep > 0) {
				response.put("forwardDep", forwardDep);
			}

			return new JSONObject(response);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return new JSONObject();
	}

	private static String escapeNL(String in) {
		String out = null;
		if (in != null) {
			out = in.replaceAll("\n", "\\\\n");
		}
		return out;
	}

	public String explainPath() {
		List<Object[]> logs = getExplain();

		List<QAPattern> patterns = QAPattern.list(getTenant().getId());
		int no = 1;

		StringBuffer path = new StringBuffer();

		path.append("Path of Q[" + currentQuestion + "] ");
		int counter = 1;
		Long lastElasped = 0l;

		for (Object[] log : logs) {
			Object val = log[2];
			Long elasped = (Long) log[3];
			path.append(" [TC" + (elasped - lastElasped));
			path.append("] ");

			// if (counter % 3 == 0)
			path.append("\n\t");

			path.append(" --> ");
			path.append("(" + counter++ + ") ");
			path.append(log[0] + "/" + log[1]);
			lastElasped = elasped;
		}

		path.append("\nEND");

		return path.toString();
	}

	/**
	 * 選單，由 Options 組成，一個 Menu 會有特定的屬性，如 header / footer，用來顯示使用 header / footer
	 * 最前面跟最後有沒有換行也會影響整體呈現
	 * 
	 * 另外也會透過 MenuView 來定義選單的進階特性。例如選項的呈現方式（是有序清單，還是隱藏不顯示） 如此來達到如下的一些功能
	 * 例如（ORDERED_LIST） [答案] [HEADER] [IndentOrSplitStr][1. 選項1 Title]
	 * [IndentOrSplitStr][2. 選項2 Title] [FOOTER]
	 * 
	 * 或者（INLINE_LIST) [答案] [HEADER][選項1 Title][IndentOrSplitStr][選項2 Title]
	 * [FOOTER]
	 * 
	 * 或者（HIDDEN，不告訴使用者選擇，讓使用者自由發揮） [HEADER][FOOTER]
	 * 
	 * @author herb
	 */
	public class OptionMenu implements Serializable {
		long qastamp;
		String key; // Must be unique in this QAContext
		MenuView view;
		MenuSelectionBehavior behavior;
		Lifetime lifetime;
		String menuHeader;
		String menuFooter;
		String indentOrSplitString;
		String createFrom;
		List<Option> options = null;
		boolean doPersonalize;

		public OptionMenu(String key, MenuView view,
				Lifetime lifetime, String createFrom,
				MenuSelectionBehavior behavior, String menuHeader,
				String menuFooter, boolean doPersonalize,
				String indentOrSplitString) {
			super();
			this.key = key;
			this.qastamp = QAContext.this.qastamp; // 標上目前的 qastamp
			this.view = view;
			this.createFrom = createFrom;
			this.lifetime = lifetime;
			this.behavior = behavior;
			this.menuHeader = menuHeader;
			this.menuFooter = menuFooter;
			this.doPersonalize = doPersonalize;
			this.indentOrSplitString = indentOrSplitString;
		}

		public String getKey() {
			return key;
		}

		public MenuSelectionBehavior getBehavior() {
			return behavior;
		}

		public long getQastamp() {
			return qastamp;
		}

		public void setQastamp(long qastamp) {
			this.qastamp = qastamp;
		}

		public String getIndentOrSplitString() {
			return indentOrSplitString;
		}

		public void setIndentOrSplitString(String indentOrSplitString) {
			this.indentOrSplitString = indentOrSplitString;
		}

		public MenuView getView() {
			return view;
		}

		public String getCreateFrom() {
			return createFrom;
		}

		public Lifetime getLifetime() {
			return lifetime != null ? lifetime : Lifetime.REQ;
		}

		public boolean isDoPersonalize() {
			return doPersonalize;
		}

		public int addOption(String pluginId, String subMenuKey,
				OptionAction action, String name, ParsedOption value, Long refToKid) {
			if (options == null) {
				options = new ArrayList<Option>();
			}
			FormalAnswerReplacer far = QAUtil.getInstance(tenant).getFormalAnswerReplacer(QAContext.this);
			name = TemplateUtil.process(name, far);
			options.add(new Option(pluginId, this, subMenuKey, action, name, value, refToKid));
			return options.size();
		}

		public int appendOptions(List<Option> newOptions) {
			if (options == null) {
				options = new ArrayList<Option>();
			}
			options.addAll(newOptions);

			return options.size();
		}

		public boolean duplicateOption(String name, Object value) {
			if (options == null) return false;

			for (Option o : options) {
				boolean valueEqual = false;
				if(value instanceof String) {
					valueEqual = StringUtils.equalsIgnoreCase(o.getValue().question, (String)value);
				}
				else {
					valueEqual = o.getValue().equals(value);
				}
				if (StringUtils.equals(o.name, name) && valueEqual) {
					return true;
				}
			}

			return false;
		}

		public int optionsSize() {
			if (options == null) {
				options = new ArrayList<Option>();
			}
			return options.size();
		}

		public List<Option> getOptions() {
			if (options == null) {
				options = new ArrayList<Option>();
			}
			return options;
		}

		@Override
		public String toString() {
			return "OptionMenu [qastamp=" + qastamp + ", key=" + key
					+ ", view=" + view + ", behavior=" + behavior
					+ ", menuHeader=" + menuHeader + ", menuFooter="
					+ menuFooter + ", indentOrSplitString="
					+ indentOrSplitString + ", doPersonalize=" + doPersonalize
					+ ", options=" + options + "]";
		}
	}

	public OptionMenu createOptionMenu(String key, MenuView view,
			String createFrom, MenuSelectionBehavior behavior, String menuHeader,
			String menuFooter, boolean doPersonalize, String indentOrSplitString) {
		return new OptionMenu(key, view, Lifetime.REQ, createFrom, behavior, menuHeader, menuFooter,
				doPersonalize, indentOrSplitString);
	}

	public OptionMenu createOptionMenu(String key, MenuView view, Lifetime lt,
			String createFrom, MenuSelectionBehavior behavior, String menuHeader,
			String menuFooter, boolean doPersonalize, String indentOrSplitString) {
		return new OptionMenu(key, view, lt, createFrom, behavior, menuHeader, menuFooter,
				doPersonalize, indentOrSplitString);
	}

	public void addAndShowOptionMenu(OptionMenu menu) {
		addAndShowOptionMenu(menu, -1);
	}

	public void addAndShowOptionMenu(OptionMenu menu, int index) {
		optionMenuMap.put(menu.key, menu);
		
		if (index == -1 || index >= showedOptionMenuKeys.size()) {
			showedOptionMenuKeys.add(menu.key);
		}
		else {
			showedOptionMenuKeys.add(index, menu.key);
		}
	}

	/**
	 * 清除一次性選單，通常在 ChooseQAOptionRule 最後使用
	 */
	public void removeOnetimeOptionMenu() {
		for (String toBeRemoveKey: optionMenuKeysToBeRemove) {
			if (optionMenuMap.containsKey(toBeRemoveKey)) {
				optionMenuMap.remove(toBeRemoveKey);
			}
		}
		optionMenuKeysToBeRemove.clear();	
	}

	/*
	 * 好像沒用到 public OptionMenu findOptionMenu(String key) { return
	 * optionMenuMap.get(key); }
	 */

	public boolean isDuplicateShowedOption(String name, Object value) {
		for (String key : showedOptionMenuKeys) {
			OptionMenu m = optionMenuMap.get(key);

			if (m != null) {
				for (Option o : m.getOptions()) {
					boolean valueEqual = false;
					if(value instanceof String) {
						valueEqual = StringUtils.equalsIgnoreCase(o.getValue().question, (String)value);
					}
					else {
						valueEqual = o.getValue().equals(value);
					}
					if (StringUtils.equals(o.name, name) && valueEqual) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * 跟 showOptionMenu 的差別在於這個不會 show
	 * 
	 * @param menu
	 */
	public void addOptionMenu(OptionMenu menu) {
		optionMenuMap.put(menu.key, menu);
	}

	public OptionMenu findOptionMenu(String key) {
		return optionMenuMap.get(key);
	}

	/**
	 * Warning: It would take time to search a option
	 * @param key
	 * @return
	 */
	public Option findOption(String pipeStr) {
		
		Set<String> checkedKeys = new HashSet<>();
		String keyScope = null;
		String optionId = pipeStr;
		Option found = null;
		
		// 若有 "::" 代表有限定 scope，用來避免選項在不同問答中有重複的 id，會造成找 option 錯亂。
		if (StringUtils.indexOf(pipeStr, "::") != -1) {
			keyScope = StringUtils.substringBefore(pipeStr, "::");
			optionId = StringUtils.substringAfter(pipeStr, "::");
		}
		
		for (OptionMenu m: optionMenuMap.values()) {
			checkedKeys.add(m.getKey());
			System.out.println(m.getKey() + "-" + m.getQastamp());
			
			for (Option o: m.getOptions()) {
				System.out.println("  -> " + o.getName() + "(" + o.getValue() + ")");

				if (keyScope == null || StringUtils.startsWith(m.getKey(), keyScope)) {
					if (o.getValue() != null 
							&& (optionId.equals("" + o.getValue().id)) ) {// || pipeStr.equals(o.getValue().pipe)) ) {
						return o;
					}
				}
				
				if (o.getSubMenuKey() != null && !checkedKeys.contains(o.getSubMenuKey())) {
					found = findOptionRecursive(o.getSubMenuKey(), pipeStr, checkedKeys);
					
					if (found != null) 
						return found;
				}
			}
		}
		
		return null;
	}

	/**
	 * Warning: It would take time to search a option
	 * @param key
	 * @return
	 */
	Option findOptionRecursive(String menuKey, String pipeStr, Set<String>checkedKeys) {
		checkedKeys.add(menuKey);
		
		OptionMenu m = findOptionMenu(menuKey);
		System.out.println(m.getKey() + "-" + m.getQastamp());
		String keyScope = null;
		String optionId = pipeStr;
		
		// 若有 "::" 代表有限定 scope，用來避免選項在不同問答中有重複的 id，會造成找 option 錯亂。
		if (StringUtils.indexOf(pipeStr, "::") != -1) {
			keyScope = StringUtils.substringBefore(pipeStr, "::");
			optionId = StringUtils.substringAfter(pipeStr, "::");
		}
		
		for (Option o: m.getOptions()) {
			System.out.println("  -> " + o.getName() + "(" + o.getValue() + ")");

			if (keyScope == null || StringUtils.startsWith(m.getKey(), keyScope)) {
				if (o.getValue() != null 
						&& (optionId.equals("" + o.getValue().id)) ) {// || pipeStr.equals(o.getValue().pipe)) ) {
					return o;
				}
			}
			
			if (o.getSubMenuKey() != null && !checkedKeys.contains(o.getSubMenuKey())) {
				Option found = findOptionRecursive(o.getSubMenuKey(), pipeStr, checkedKeys);
				if (found != null)
					return found;
			}
		}
		return null;
	}
	
	public boolean showOptionMenu(String key) {
		return showOptionMenu(key, -1);
	}

	public boolean showOptionMenu(String key, int index) {
		if (optionMenuMap.containsKey(key) && !showedOptionMenuKeys.contains(key)) {
			if (index == -1 || index >= showedOptionMenuKeys.size()) {
				showedOptionMenuKeys.add(key);
			}
			else {
				showedOptionMenuKeys.add(index, key);
			}
			return true;
		}

		return false;
	}
	
	public Option getShowedOption(List<OptionMenu> showedMenus,int choosed) {
		int offset = 0;
		for (OptionMenu m : showedMenus) {
			if (m.view == MenuView.HIDDEN) continue;

			if (choosed < offset) {
				break;
			}

			if (choosed - offset >= m.optionsSize()) {
				offset += m.optionsSize();
				continue;
			}

			return m.getOptions().get(choosed - offset);
		}

		return null;
	}

	public Option getShowedOptionInLastAnswer(int choosed) {
		return getShowedOption(getAllShowedOptionMenuInLastAnswer(), choosed);
	}

	public int getShowedOptionsSizeInLastAnswer() {
		int counter = 0;
		for (String key : showedOptionMenuKeysInLastAnswer) {
			OptionMenu m = optionMenuMap.get(key);

			if (m.view == MenuView.HIDDEN) continue;
			if (Lifetime.ONETIME == m.getLifetime()) continue;

			counter += m.optionsSize();
		}

		return counter;
	}

	public List<OptionMenu> getAllShowedOptionMenuInLastAnswer() {
		List<OptionMenu> list = new ArrayList<OptionMenu>();

		for (String key : showedOptionMenuKeysInLastAnswer) {
			OptionMenu m = optionMenuMap.get(key);
			
			if (m != null) list.add(m);
		}

		return list;
	}

	/**
	 * 這個跟 getAllShowedOptionMenuInLastAnswer() 不一樣的地方是
	 * getAllShowedOptionMenuInLastAnswer() 是拿到前一題的選單
	 * 這個是拿到「最後曾經顯示的選單」，意思是不管是不是最近被問，反正最後曾經被顯示的選單們（可能一次剛好顯示不只一個選單）
	 * 
	 * @return
	 */
	public List<OptionMenu> getLastShowedOptionMenus() {
		List<OptionMenu> list = new ArrayList<OptionMenu>();

		if (showedMenuHistories.size() > 0) {
			List<String> keys = showedMenuHistories.peek();
			
			for (String key: keys) {
				if (optionMenuMap.containsKey(key)) {
					OptionMenu m = optionMenuMap.get(key);
					
					if (Lifetime.ONETIME != m.getLifetime())
						list.add(m);
				}
			}
		}

		return list;
	}
	
	public static enum Lifetime {
		//CTX,  //尚未implement
		//DLG,  //尚未implement
		REQ, 
		ONETIME;// ONETIME 會強制在下一個對話回合被移除
	}

	public static enum MenuView {
		ORDERED_LIST("有序選單", "menu.order.option"), INLINE_LIST("行內文字", "menu.inline.text"), HIDDEN("隱藏選單", "menu.hidden.menu"), ;

		String title;
		String messageKey;

		MenuView(String title, String messageKey) {
			this.title = title;
			this.messageKey = messageKey;
		}

		public String getTitle() {
			return title;
		}
		
		public String getMessage(Locale locale) {
			return MessageUtil.getMessage(locale, messageKey);
		}
	}

	public static enum MenuSelectionBehavior {
		NUMBER_ONLY("接受輸入數字", "menu.accept.input.number"),
		NUMBER_OR_FULL_MATCH_TITLE("接受數字及完整標題", "menu.accept.input.number.and.fullTitle"),
		NUMBER_OR_FUZZY_TITLE("接受數字及模糊標題", "menu.accept.input.number.and.partialTitle"),
		FULL_MATCH_TITLE("接受完整標題", "menu.accept.input.fullTitle"), // 允許輸入數字或者選項名稱（語音互動會用到）
		FUZZY_TITLE("接受模糊標題", "menu.accept.input.partialTitle"), // 允許 Fuzzy 選項比對
		FUNCTION_CALL("執行函式", "menu.run.script"), ;

		String title;
		String messageKey;

		MenuSelectionBehavior(String title, String messageKey) {
			this.title = title;
			this.messageKey = messageKey;
		}

		public String getTitle() {
			return title;
		}
		
		public String getMessage(Locale locale) {
			return MessageUtil.getMessage(locale, messageKey);
		}
	}

	public static enum OptionAction {
		INPUT_NUMBER("送出選項數字", "hierarchical.send.option.num"), // 選擇後輸入數字， value must be Long
		INPUT_TEXT("送出文字", "hierarchical.send.text"), // 選擇後輸入文字， value must be String
		DIRECT_ANSWER("直接回答", "hierarchical.directly.answer"), // 選擇後直接給特定文字， value must be String
		SUB_MENU_ONLY("提供子選項", "hierarchical.provide.suboptions"), // 選擇後給予別的選項， value 不重要，不會拿來用
		REDIRECT_TO_QUESTION("轉送到別題", "hierarchical.redirect.other.question"), // 選擇後回答特定問題， value must be Long (Question ID)
		REDIRECT_TO_OPTION("轉送到別的選項", "hierarchical.redirect.other.option"), // 選擇後視為跳到別的選項， value must be Long (h_qa_id，the ID of option)
		// INSERT_OPTIONS, // [特殊機制] 這裡直接引用另一個 OptionMenu 內的選項進來， value must be String (key of OptionMenu)
		PROCESS_BY_QAPLUGIN("交給外掛處理", "hierarchical.redirect.plugin"), // 選擇後交給對應的 QAPlugin 自行處理
		;

		String title;
		String messageKey;
		
		OptionAction(String title, String messageKey) {
			this.title = title;
			this.messageKey = messageKey;
		}

		public String getTitle() {
			return title;
		}
		
		public String getMessageKey() {
			return messageKey;
		}
		
		public String getMessage(Locale locale) {
			return MessageUtil.getMessage(locale, messageKey);
		}
	}

	/**
	 * 答案中提供選擇題，集中由 context 控管方便計算選項數目
	 *
	 * @author herb
	 */
	public static class Option implements Serializable {
		String pluginId;
		OptionMenu menu;
		String subMenuKey;
		OptionAction action;
		String name;
		ParsedOption value;
		Long refToKid; // pipe
		String script;

		public Option(String pluginId, OptionMenu menu, String subMenuKey,
				OptionAction action, String name, ParsedOption value, Long refToKid) {
			super();
			this.pluginId = pluginId;
			this.menu = menu;
			this.subMenuKey = subMenuKey;
			this.action = action;
			this.name = name;
			this.value = value;
			this.refToKid = refToKid;
		}

		public String getPluginId() {
			return pluginId;
		}

		public OptionMenu getMenu() {
			return menu;
		}

		public String getSubMenuKey() {
			return subMenuKey;
		}

		public void setSubMenuKey(String subMenuKey) {
			this.subMenuKey = subMenuKey;
		}

		public OptionAction getAction() {
			return action;
		}

		public String getName() {
			return name;
		}

		public ParsedOption getValue() {
			return value;
		}

		public Long getRefToKid() {
			return refToKid;
		}

		public String getScript() {
			return script;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((action == null) ? 0 : action.hashCode());
			result = prime * result + ((menu == null) ? 0 : menu.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((pluginId == null) ? 0 : pluginId.hashCode());
			result = prime * result + ((refToKid == null) ? 0 : refToKid.hashCode());
			result = prime * result + ((script == null) ? 0 : script.hashCode());
			result = prime * result + ((subMenuKey == null) ? 0 : subMenuKey.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Option other = (Option) obj;
			if (action != other.action) return false;
			if (menu == null) {
				if (other.menu != null) return false;
			}
			else if (!menu.equals(other.menu)) return false;
			if (name == null) {
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (pluginId == null) {
				if (other.pluginId != null) return false;
			}
			else if (!pluginId.equals(other.pluginId)) return false;
			if (refToKid == null) {
				if (other.refToKid != null) return false;
			}
			else if (!refToKid.equals(other.refToKid)) return false;
			if (script == null) {
				if (other.script != null) return false;
			}
			else if (!script.equals(other.script)) return false;
			if (subMenuKey == null) {
				if (other.subMenuKey != null) return false;
			}
			else if (!subMenuKey.equals(other.subMenuKey)) return false;
			if (value == null) {
				if (other.value != null) return false;
			}
			else if (!value.equals(other.value)) return false;
			return true;
		}

		@Override
		public String toString() {
			return "Option [pluginId=" + pluginId + ", menu=" + menu.getKey()
					+ ", action=" + action + ", name=" + name + ", value="
					+ value + ", refToKid=" + refToKid + ", has Script?" + StringUtils.isNotEmpty(script) + "]";
	}
	}

	public int getShowedOptionMenuSize() {
		if (showedOptionMenuKeys == null) {
			return 0;
	}
		return showedOptionMenuKeys.size();
	}

	public int getTotalShowedOptions() {
		if (showedOptionMenuKeys == null) {
			return 0;
		}
		int total = 0;

		for (String key : showedOptionMenuKeys) {
			OptionMenu m = optionMenuMap.get(key);

			if (m != null) {
				total += m.optionsSize();
			}
		}

		return total;
	}

	/**
	 * Actually this is the "QAChannel"
	 * 
	 * @return
	 */
	public String getQaChannel() {
		return qaChannel != null ? qaChannel : QAChannel.DEFAULT_CHANNEL_CODE;
	}

	public void setQaChannel(String channelCode) {
		this.qaChannel = channelCode;
	}
	
	public QAChannel getQAChannelInstance() {
		QAChannel ch = QAChannel.get(getTenant().getId(), getQaChannel());
		return ch;
	}

	public String getUserType() {
		return userType != null ? userType : QAUserType.DEFAULT_USERTYPE_CODE;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public void setQaAsAnswer(QA directAnswer) {
		String NL = getQAChannelInstance().getUseHtmlNewline() ? "<br>" : "\n";
		// 使 context 記住本次回答的主體
		setCurrentQA(directAnswer);
		boolean explainQA = getTenant().getEnableQAExplain();

		String answerToAppendOrSet = "";
		String customScript = (String) directAnswer.getFieldValue("CUSTOM_SCRIPT_s");
		if (StringUtils.isNotEmpty(customScript)) {
			Binding binding = new Binding();
			binding.setProperty("ctx", this);
			binding.setProperty("currentQA", directAnswer);
			
			answerToAppendOrSet = (String)GroovyUtil.runScript(binding, customScript);
		}

		if (StringUtils.isNotEmpty(answerToAppendOrSet) && StringUtils.endsWith(answerToAppendOrSet, "{{...}}}")) {
			setAnswerText(StringUtils.substringBeforeLast(answerToAppendOrSet, "{{{...}}}"));
			answerToAppendOrSet = "";
		}

		if (StringUtils.isEmpty(answerToAppendOrSet)) {
			if (StringUtils.trimToNull(getQaChannel()) != null) {
				answerToAppendOrSet = MultiChannelAnswer.qaRuleGetAnswer(this, (String) directAnswer.getFieldValue("id"), getQAChannelInstance(), getUserType());
			}
			
			if (StringUtils.isEmpty(answerToAppendOrSet)) {
				answerToAppendOrSet = (String) directAnswer.getFieldValue("ANSWER_s");
			}
		}

		if (hasAnswerText()) {
			appendAnswerText(NL);
			appendAnswerText(answerToAppendOrSet);
		} else {
			setAnswerText(answerToAppendOrSet);
		}
		
		if (StringUtils.isNotEmpty(answerToAppendOrSet)) {
			setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
		}
		setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
		// end


		// 準備 OtherPossibleQuestions
		SolrDocumentList mlts = getQAUtil().mltCommonSense(getCurrentQuestion(), this, getTenant().getOtherMltQANum());
		if (explainQA) appendExplain("We have direct answer, now trying to find otherPossibleQuestions...",  mlts != null ? mlts.clone() : mlts);
		JSONArray otherPossibleQuestions = new JSONArray();
		
		for (SolrDocument doc : mlts) {
			if (((Float) doc.getFirstValue("score")) > getTenant().getThreshold3()/2) {
				Object id = doc.getFirstValue("id");
	
				if (directAnswer != null && id.equals(directAnswer.getFieldValue("id"))) {
					appendExplain(String.format("score > %d, this is possible other question, but is skipped bcz equals to firstCommonSense", getTenant().getThreshold3()), doc);
					continue;
				}
	
				if (Boolean.TRUE.equals(doc.getFirstValue(QA.FN_IS_NOT_FOR_MLT))) {
					appendExplain(String.format("This is possible other question, but is skipped bcz equals to not for MLT==true"), doc);
					continue;
				}
	
				if (explainQA) {
					appendExplain(String.format("score > %d, this is possible other question", getTenant().getThreshold3()/2), doc);
				}
		
				try {
					JSONObject otherQ = new JSONObject();
					otherQ.put("question", doc.getFirstValue("QUESTION_s"));
                    otherQ.put("briefly_question", doc.getFirstValue("Briefly_QUESTION_s"));
					otherQ.put("score", doc.getFirstValue("score"));
					otherQ.put("kid", doc.getFirstValue("kid_l"));
	
					otherPossibleQuestions.put(otherQ);
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (otherPossibleQuestions.length() > 0) {
			setRequestAttribute("otherPossibleQuestions", otherPossibleQuestions);
		}

		setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
	}

	public Object removeCtxAttr(String key) {
		if (ctxAttr.containsKey(key)) {
			return ctxAttr.remove(key);
		}
		
		return null;
	}

	public Object removeRequestAttribute(String key) {
		if (reqAttr.containsKey(key)) {
			return reqAttr.remove(key);
		}
		
		return null;
	}
}
