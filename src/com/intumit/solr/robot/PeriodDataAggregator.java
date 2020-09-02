package com.intumit.solr.robot;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.VelocityContext;
import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.dictionary.CustomData;

public class PeriodDataAggregator extends QADataAggregator {
	
	private static String[] rangeArr = {"以上", "以下", "超過", "未滿"};
	private static String[] numArr = {"一", "壹", "二", "貳", "兩", "三", "參", "四", "肆", "五", "伍", "六", "陸", "七", "柒", "八", "捌", "九", "玖" };
	private static String[] periodArr = {"年", "個月", "天"};
	private static String[] tenArr = {"十", "拾"};

	@Override
	public void aggregate(QA customQa, QAContext qaCtx, QAPattern qp, List<CustomData> nvPairs, VelocityContext context) {
		
		try {
			AtomicBoolean autoComplete = new AtomicBoolean(false);
			SolrQuery thisQuery = this.generateQuery(customQa, qp, qaCtx, nvPairs, autoComplete);
			String question = qaCtx.getCurrentQuestion();
			String[] numPeriod = findPeriodString(question).split(",");
			String numS = "";
			String periodS = "";
			if (numPeriod.length == 2) {
				numS = numPeriod[0];
				periodS = numPeriod[1];
			}
			numS = numS.replaceAll("[^0-9]+", "");
			if(numS.equals("05")){
				numS = "0.5";
			} else if (numS.equals("")) {
				numS = "0";
			}
			String range = "";
			for (String c : rangeArr) {
				if (question.contains(c)) {
					range = c;
					break;
				}
			}
			int period = 0;
			Double num = Double.valueOf(numS);
			if (periodS.equals(periodArr[0])) {
				period = (int) (num * 360);
			} else if (periodS.equals(periodArr[1])) {
				period = (int) (num * 30);
			} else if (periodS.equals(periodArr[2])) {
				period = (int) (num * 1);
			}
			if (period > 0) {
				if (!range.equals("")) {
					if (range.equals(rangeArr[0])) {
						thisQuery.addFilterQuery("day_i:[" + period + " TO *]");
					} else if (range.equals(rangeArr[1])) {
						thisQuery.addFilterQuery("day_i:[* TO " + period + "]");
					} else if (range.equals(rangeArr[2])) {
						thisQuery.addFilterQuery("day_i:[" + (period + 1) + " TO *]");
					} else if (range.equals(rangeArr[3])) {
						thisQuery.addFilterQuery("day_i:[* TO " + (period - 1) + "]");
					}
				} else {
					String[] qpPeriods = qp.getAnswerPeriod().split(",");
					int i = 0;
					for (String qpPeriod : qpPeriods) {
						if (period <= Integer.valueOf(qpPeriod)) {
							break;
						}
						i++;
					}
					if(i >= qpPeriods.length){
						thisQuery.addFilterQuery("day_i:[" + period + " TO *]");
					} else {
						if (period == Integer.valueOf(qpPeriods[i]) && (qpPeriods.length - 1 != i)) {
							thisQuery.addFilterQuery("day_i:" + qpPeriods[i]);
						} else if (period < Integer.valueOf(qpPeriods[i]) && (i > 0)) {
							thisQuery.addFilterQuery("day_i:[" + qpPeriods[i - 1] + " TO " + qpPeriods[i] + "]");
						} else {
							thisQuery.addFilterQuery("day_i:[" + qpPeriods[i] + " TO *]");
						}
					}
				}
			}
			QueryResponse res = getDataSourceServer(qp.getDataSource(), qaCtx.getTenant()).query(thisQuery);
			SolrDocumentList docs = res.getResults();

			context.put("docs", docs);
			context.put("autoComplete", autoComplete);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}
	
	public static String replaceRegularText(String r) {
		String tmp = r;
		tmp = StringUtils.replaceChars(StringUtils.lowerCase(tmp), "一壹二貳兩三參四肆五伍六陸七柒八捌九玖十拾", "112223344556677889911");
		return tmp;
	}
	
	private static String[] replaceArr = {"今天"};
	
	public static String findPeriodString(String question) {
		for (String r : replaceArr) {
			question = question.replace(r, "");
		}
		Pattern templatePattern = Pattern
				.compile(".*?([0-9]+)(" + periodArr[0] + "|" + periodArr[1] + "|" + periodArr[2] + ").*?");
		Matcher matcher = templatePattern.matcher((String) question);
		String numS = "";
		String periodS = "";
		while (matcher.find()) {
			numS = matcher.group(1);
			periodS = matcher.group(2);
			break;
		}
		if (numS.equals("")) {
			templatePattern = Pattern
					.compile(".*?(.*?)(" + periodArr[0] + "|" + periodArr[1] + "|" + periodArr[2] + ").*?");
			matcher = templatePattern.matcher(question);
			while (matcher.find()) {
				numS = matcher.group(1);
				periodS = matcher.group(2);
				break;
			}
			// 找到十在問問答中的位置
			if (numS.contains("十") || numS.contains("拾")) {
				char[] qArray = numS.toCharArray();
				int n = -1;
				for (int i = 0; i < qArray.length; i++) {
					if (Arrays.asList(tenArr).contains(String.valueOf(qArray[i]))) {
						n = i;
						break;
					}
				}
				if (n != -1) {
					numS = "";
					if (n > 0 && Arrays.asList(numArr).contains(String.valueOf(qArray[n - 1]))) {
						numS += String.valueOf(qArray[n - 1]);
					}
					numS += String.valueOf(qArray[n]);
					if (n != (qArray.length - 1) && Arrays.asList(numArr).contains(String.valueOf(qArray[n + 1]))) {
						numS += String.valueOf(qArray[n + 1]);
					}
				}
				boolean replaceTen = false;
				String replaceString = "";
				// 二十一=>二一
				if (numS.length() == 3) {
					replaceTen = true;
					replaceString = "";
				// 三十=>三0
				} else if (numS.length() == 2 && (numS.substring(1).contains("十") || numS.substring(1).contains("拾"))) {
					replaceTen = true;
					replaceString = "0";
				// 十=>10
				} else if (numS.length() == 1 && Arrays.asList(tenArr).contains(numS)) {
					replaceTen = true;
					replaceString = "10";
				}
				if(replaceTen){
					numS = numS.replace("十", replaceString);
					numS = numS.replace("拾", replaceString);
				}
			}
			numS = replaceRegularText(numS);
			if(numS.contains("半")){
				numS = numS.replace("半", "05");
			}
		}
		return numS + "," + periodS;
	}

}
