package com.intumit.solr.robot.function;

import java.io.Serializable;

import com.intumit.solr.robot.QAContext;

/**
 * 這是用來作為 inline function call 用的
 * 任何可作為 inline function 都需實作此介面
 * inline function call 可用在「答案文字的任何地方」，格式為 {{FUNCTION_NAME:PARAMETERS}}
 * 其中 PARAMETERS 又以雙冒號做為分隔（ex. {{SETREQ:KEY::VALUE}}）
 *
 * 答案在輸出前會先經過 FunctionUtil 的處理
 * 
 * @author herb
 *
 */
public interface RobotFunction extends Serializable {
	Object exec(QAContext ctx, UserInput in);
	String getOriginalText();
}
