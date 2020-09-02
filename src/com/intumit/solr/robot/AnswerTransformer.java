package com.intumit.solr.robot;

/**
 * 理論上 AnswerTransformer 應該單純是為了處理
 * 階層 / 選單 / 連結 / 影像等相關內容要輸出到不同 channel 的時候所需的特殊格式
 * 但不應該處理整體「API」結構。整體API輸出結構最終目前都還是交由 qa-ajax.jsp / conn-line.jsp / ms-bfc.jsp 等程式來處理
 * 
 * @TODO 未來應該要定義更清楚的訊息轉換規則及責任分工，不然程式會越改越亂
 * 
 * @author herb
 *
 */
public interface AnswerTransformer {
	public QAContext transform(QAChannel ch, QAContext ctx);
}
