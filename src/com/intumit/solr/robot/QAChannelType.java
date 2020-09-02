package com.intumit.solr.robot;

/**
 * QAChannelType 主要用來定義該種 Channel 的格式（格式是指技術上溝通的格式）
 * 格式也會影響「編輯畫面」
 * 
 * @author herb
 *
 */
public enum QAChannelType {
	/**
	 * 純文字，包含文字跟語音的文字
	 */
	PLAIN_TEXT,
	/**
	 * 網頁格式
	 */
	RICH_TEXT,
	/**
	 * Apache velocity template engine
	 */
	VELOCITY_TEMPLATE,
	/**
	 * Markdown 格式 
	 */
	MARKDOWN_TEXT,
	/**
	 * FB MESSENGER 格式
	 */
	FACEBOOK_MESSENGER,
	/**
	 * LINE Message API 格式
	 */
	LINE,
	/**
	 * Skype 格式
	 */
	SKYPE,
	/**
	 * Slack 格式
	 */
	SLACK,
	/**
	 * Microsoft Teams 格式
	 */
	MICROSOFT_TEAMS,
	/**
	 * 實體機器人格式，目前應該包含跟 NAOqi 溝通跟「觸控螢幕溝通」的格式(output、outputVoice、outputMood)
	 */
	ROBOT,
	/**
	 * 包含文字跟語音的答案(output、outputVoice)
	 */
	PLAIN_TEXT_WITH_VOICE,
	/**
	 * Actions on Google 格式
	 */
	ACTIONS_ON_GOOGLE,
}
