package com.intumit.solr.robot.qarule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.QuestionChangeListener;
import com.intumit.solr.robot.ReconstructQuestionParameter;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.entity.QAEntityDictionary;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.intent.QAIntentDictionary;
import com.intumit.solr.robot.qadialog.QADialog;

public class Reconstruct4QuestionChangeListener implements
		QuestionChangeListener {

	@Override
	public void changed(QAContext ctx, String orig, String newQuestion) {
		String lastInputType = (String)ctx.getLastResponseAttribute("inputType", "string");
		
		if (!StringUtils.equals(lastInputType, "password") 
				&& !StringUtils.equals(lastInputType, "stringMask") 
				&& (orig == null || !orig.equals(newQuestion))) {
			
			String question = QAUtil.removeSymbols(newQuestion, ctx.getTenant().getLocale());
			List<String> alts = QAUtil.reconstructQuestion(question, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, ctx, QAUtil.getInstance(ctx.getTenant()).getToAnalysis());
			if (!alts.contains(question))
				alts.add(0, question);
			
			ctx.setRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, alts);
			System.out.println("ReconstructedQuestion of [" + question + "]" + alts);

			if (ctx.getTenant().getEnableScenario() && !QADialog.findTrainedUtterance(ctx, null)) {
				Set<QAIntent> intents = new HashSet<>();
				Set<QAEntity> entities = new HashSet<>();
				
				for (String alt: alts) {
					Set<QAIntent>[] intentsRes = QAIntentDictionary.search(ctx.getTenant().getId(), alt.toCharArray(), null);
					Set<QAEntity>[] entitiesRes = QAEntityDictionary.search(ctx.getTenant().getId(), alt, null);
					
					for (Set<QAIntent> d: intentsRes) {
						intents.addAll(d);
					}
					for (Set<QAEntity> d: entitiesRes) {
						entities.addAll(d);
					}
				}
				
				Map<String, QAEntity> map = QAEntity.collToMap(entities, null);
				Set<QAEntity> toBeRemove = new HashSet<>();
				
				for (QAEntity ed: entities) {
					if (StringUtils.isNotEmpty(ed.getSubEntities())) {
						for (String subTag: StringUtils.split(ed.getSubEntities(), ",")) {
							if (map.containsKey(subTag)) {
								toBeRemove.add(ed);
							}
						}
					}
				}
				
				entities.removeAll(toBeRemove);
		
				ctx.setEntities(entities);
				ctx.setIntents(intents);
			}
		}
		else {
			// 如果是 password / stringMask
			// 理論上資料都是特殊格式，不應該將資料特殊處理過
			List<String> l = new ArrayList<>();
			l.add(newQuestion);
			ctx.setRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, l);
		}
	}

	@Override
	public void listenerAdded(QAContext ctx, String currQuestion) {
		changed(ctx, null, currQuestion); // 加入的時候也跑一下
	}

}
