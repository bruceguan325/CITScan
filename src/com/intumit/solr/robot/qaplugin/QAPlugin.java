package com.intumit.solr.robot.qaplugin;

import java.lang.reflect.Constructor;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;

/**
 * 
 * QAPlugin create serveral hooks during QA api call or administration process.
 * 
 * A. on QA API call
 *  Before doing normal matching process: -> onApiChoosedOption -> onApiQuestionIntercept -> onApiAnswerIntercept -> shouldInterceptNextApi 
 *  After normal matching process (before search casual db): -> onApiAfterProfessionalIndexMatching -> shouldDoNLP + onApiNLPAnswer
 * 
 */
public abstract class QAPlugin {
	
	private String id;
	
	private String name;
	
	protected QAPlugin(String id, String name){
		this.id = id;
		this.name = name;
	}
	
	static <T extends QAPlugin> T newInstance(Class<T> clazz, String id, String name) 
			throws Exception {
		Constructor<T> constructor = clazz.getDeclaredConstructor(String.class, String.class);
		return constructor.newInstance(id, name);
	}
	
	public final String getId(){
		return id;
	}
	
	public final String getName(){
		return name;
	}
	
	/** 是否綁定後台QA編輯介面。
	 * 	API會執行一個後台綁定的plugin，以及全部無綁定的plugin。 */
	public boolean hasEditorPage(){
		return false;
	}
	
	/** 回傳JSP forward路徑 */
	public String onEditorPageCreate(SolrDocument doc, HttpServletRequest req){
		return null;
	}
	
	/** selected參數表示該plugin被選取與否 */
	public void onEditorPageSave(SolrInputDocument doc, HttpServletRequest req, 
			boolean selected){
	}
	
	/** 回傳JSP forward路徑；
	 * 	null則無後台預覽；
	 * 	origAnswer參數是目前的答案 */
	public String onAnswerPreview(String origAnswer, HttpServletRequest req){
		return null;
	}
	
	/** 是否攔截下一次API呼叫 */
	public boolean shouldInterceptNextApi(QAContext ctx, JSONObject curData){
		return false;
	}
	
	/** 傳遞參數給下一次的攔截 */
	public JSONObject prepareInterceptorData(QAContext ctx, JSONObject curData){
		return null;
	}
	
	/**
	 * 當使用者選了某個選項 
	 * 
	 * @param ctx
	 * @param option
	 * @return
	 */
	public PostRuleCheckResult onApiChoosedOption(QAContext ctx, QAContext.Option option) {
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	/** 攔截並自訂問題 */
	public String onApiQuestionIntercept(QAContext ctx, String question, JSONObject data){
		return question;
	}
	
	/** 攔截並自訂答案 */
	public String onApiAnswerIntercept(QAContext ctx, JSONObject data){
		return null;
	}
	
	/** 做完專業知識庫比對後呼叫此 hook */
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, QAContext ctx){
		return null;
	}
	
	/** 是否進行NLP */
	public boolean shouldDoNLP(){
		return false;
	}
	
	/** 提供NLP答案；API最終連接每個plugin的答案 */
	public String onApiNLPAnswer(String[][] postTags, QAContext ctx){
		return null;
	}
	
	/** 是否支援答案template處理 */
	public boolean supportAnswerTemplate(){
		return false;
	}

	@Override
	public String toString() {
		return "QAPlugin [id=" + id + ", name=" + name
				+ ", hasEditorPage()=" + hasEditorPage() + ", shouldDoNLP()=" + shouldDoNLP()
				+ ", supportAnswerTemplate()=" + supportAnswerTemplate() + "]";
	}
	
}
