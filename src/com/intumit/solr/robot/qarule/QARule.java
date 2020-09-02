package com.intumit.solr.robot.qarule;

import java.io.Serializable;
import java.util.Map;

import com.intumit.solr.robot.QAContext;

/**
 * 每一個與回答有關的規則
 * 每個 Rule 都必需是 state-less
 * 一個 Tenant instance 當中，只會有一個 QAMatchRuleController instance
 * 而每個 rule 在其下也只會有一個 instance，所以請當作 Servlet 來用
 * 
 * @author herb
 *
 */
public interface QARule extends Serializable {
	
	/**
	 * 傳入初始設定
	 * @param configs
	 */
	public void init(Map<String, Object> configs);
	

	/**
	 * 在邏輯判斷前的檢查，這裡強調的是檢查 precondition 是否滿足
	 * 意思是這裡通常是在檢查 ctx 是否已經存在需要的變數
	 * 
	 * @param ctx
	 * @return 不可為 null，至少要回傳 PreRuleCheckResult.DEFAULT_NORMAL_RESULT
	 */
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx);
	
	/**
	 * 實際上本 rule 的邏輯判定區，請儘量集中在這裡，而非 pre / post check
	 * @param ctx
	 * @return 不可為 null，至少要回傳 PostRuleCheckResult.DEFAULT_CONTINUE_RESULT
	 */
	public PostRuleCheckResult checkRule(QAContext ctx);
	
	/**
	 * 這是輸出檢查，意思是通常此 rule 判定後，會改變一些 ctx 內的狀態
	 * 這裡要檢查是否有正常改變
	 * 
	 * @param ctx
	 * @return 不可為 null，至少要回傳 PostRuleCheckResult.DEFAULT_CONTINUE_RESULT
	 */
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result);
}
