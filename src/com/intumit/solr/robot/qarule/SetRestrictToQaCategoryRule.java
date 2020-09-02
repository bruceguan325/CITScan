package com.intumit.solr.robot.qarule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;

/**
 * 判斷是否限制業務類別
 * 
 * @先決條件 必需先斷過知識主題
 * 
 * @author herb
 */
public class SetRestrictToQaCategoryRule implements PreQAMatchRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.getTenant().getEnableRestrictToQaCategory()) {
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		}
		
		// 沒啟用限制業務類別就直接跳過
		return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		//偵測此問題的知識點位在哪個類別(for是否跨區)
		DictionaryDatabase[] currentKPs = ctx.getCurrentKPs();
		Set<String> relatedQaCategories = new HashSet<String>();
		String qaCategorySelect = (String)ctx.getRequestAttribute("qaCategorySelect");
		
		if (currentKPs != null && currentKPs.length > 0) {
			// 把知識點分類塞到一個 HashSet 當中（且排除「通用」）
			for (DictionaryDatabase currentKP: currentKPs) {
				if (!QAUtil.GENERAL_CATEGORIES.contains(currentKP.getCategory())) {
					relatedQaCategories.add(currentKP.getCategory());
				}
			}
		}
	
		if (qaCategorySelect != null && !qaCategorySelect.equals("")) {
			// qaCatgegorySelect not null 時，才根據 qaCategorySelect 切換
			// 這有點像是預選，算是模擬進線預設業務類別的機制
			ctx.setRestrictToQaCategory(qaCategorySelect);
		}
		
		if (ctx.getRestrictToQaCategory() != null
			&& relatedQaCategories.size() > 0 && !relatedQaCategories.contains(ctx.getRestrictToQaCategory())
			) {
			// 知識點分類們若不包含目前的業務類別，則切換到 Set 當中的第一個
			ctx.setRestrictToQaCategory(relatedQaCategories.iterator().next());
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
