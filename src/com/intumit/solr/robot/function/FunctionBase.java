package com.intumit.solr.robot.function;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public abstract class FunctionBase implements RobotFunction {
	String data;
	String originalText;

	public FunctionBase(String originalText, String data) {
		this.originalText = originalText;
		this.data = data;
	}
	
	public String getOriginalText() {
		return originalText;
	}
	
	/**
	 * 取得參數們，遇到 :: 分開，不限數量
	 * @return
	 */
	List<String> splitData() {
		return Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(data, "::"));
	}
	
	/**
	 * 取得參數們，遇到 :: 分開，最多 max 個，到達 max 個之後，後續所有字元都歸在最後一個參數當中（包含後續如果有出現 :: 也一樣）
	 * @param max
	 * @return
	 */
	List<String> splitData(int max) {
		return Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(data, "::", max));
	}
	
}
