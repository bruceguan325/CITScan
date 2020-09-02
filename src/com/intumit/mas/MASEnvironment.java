package com.intumit.mas;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.systemconfig.WiseSystemConfig;

/**
 * 元富環境
 * @author johnli
 */
public class MASEnvironment {
	public static Logger logger = Logger.getLogger(MASEnvironment.class.getName());
	public static boolean isImageUrlStartWithContextPath = true; //圖片網址改成ContextPath開頭,Ex./wise/img/sticker/MV9TS0ktMTY=.png
	public static boolean needImageUrlStartWithContextPath() {
		if(MASEnvironment.isImageUrlStartWithContextPath) {
			logger.info("圖片網址改成ContextPath開頭,ContextPath:"+WiseSystemConfig.get().getContextPath());
		}
		return MASEnvironment.isImageUrlStartWithContextPath;
	}
	public static boolean isSkipRuleWhenHaveCurrentDialog = true;//情境中SkipRule 
	public static boolean needSkipRuleWhenHaveCurrentDialog(QAContext ctx) {
		if(MASEnvironment.isSkipRuleWhenHaveCurrentDialog) {
			logger.info("需確認情境中SkipRule");
			boolean haveCurrentDialog = false;
			try {
				QADialog currentDialog = (QADialog) ctx.getCtxAttr("_currentDialog");
				if(currentDialog != null) {
					logger.info("情境中SkipRule,currentDialog.getMkey():"+currentDialog.getMkey());
					haveCurrentDialog = true;
				}
			}catch(Exception e) {
				logger.error("needSkipRuleWhenHaveCurrentDialog have error",e);
			}
			return haveCurrentDialog?true:false;
		}
		return MASEnvironment.isSkipRuleWhenHaveCurrentDialog;
	}
	
	/**
	 * 因為元富知識點有例句「1元」,所以在ChooseQAOptionRule不能將CurrentQuestion從"1元"改成"1"
	 * <br>SkipCode的部分交由ChooseQAOptionRule實作,MASEnvironment只負責檢查是否要SkipCode
	 * <br>可透過qa-ajax.jsp設定是否檢查與要檢查的問句
	 * @相關資料 gmail主旨 [元富] 上線問題_1元已學入例句但無法完全比對 (英數中混雜似乎沒辦法完全比對)
	 */
	public static boolean isSkipCodeInChooseQAOptionRuleWhenMatchCurrentQuestion = true;
	
	/**
	 * 在ChooseQAOptionRule中需要SkipCode的CurrentQuestion
	 */
	public static String[] currentQuestionInChooseQAOptionRuleNeedSkipCode = {"1元"};
	
	public static boolean needSkipCodeInChooseQAOptionRuleWhenMatchCurrentQuestion(QAContext ctx) {
		if(MASEnvironment.isSkipCodeInChooseQAOptionRuleWhenMatchCurrentQuestion) {
			logger.info("元富環境內,需檢查在ChooseQAOptionRule時的CurrentQuestion");
			for(String q:currentQuestionInChooseQAOptionRuleNeedSkipCode) {
				if(StringUtils.equals(q, ctx.getCurrentQuestion())) {
					return true;
				}
			}
		}
		return false;
	}
	
}
