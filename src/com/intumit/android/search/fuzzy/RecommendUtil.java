package com.intumit.android.search.fuzzy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.intumit.hithot.HitHotLocale;

public class RecommendUtil {
	// 排序
	/*static final Comparator<WikiWord> sort = new Comparator<WikiWord>() {
		public int compare(WikiWord o1, WikiWord o2) {
			if (o1.getTotalScore() == o2.getTotalScore())
				return 0;
			else if ((o1.getTotalScore() > o2.getTotalScore()))
				return -1;
			else
				return 1;
		}
	};

	private static String notImportantWord[] = { "年", "月", "日" };

	private static String chineseNumber[] = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "0", "ㄧ", "二", "三", "四", "五", "六", "七", "八",
			"九", "十", "廿" };

	public static List<WikiWord> calculateScore(HitHotLocale locale, Hashtable matchedPatterns) {
		WikiWordList wikiWordList = ((WikiWordtable) matchedPatterns).toList();

		WikiWord me;
		double averageScore = 0.0;
		int scoreSize = 0;
		for (int i = 0; i < wikiWordList.size(); i++) {
			BigDecimal bd;

			me = wikiWordList.get(i);
			int frequency = ((WikiWord) matchedPatterns.get(me.getPageTitle()))
					.getFrequency();
			int index = ((WikiWord) matchedPatterns.get(me.getPageTitle()))
					.getFirstIndex();
			me.setFrequency(frequency);
			me.setFirstIndex(index);
			// double totalScore = getAdjustScroe(me);

			double totalScore = ((WikiWord) matchedPatterns.get(me
					.getPageTitle())).getInitScore();
			try {
				// bd = new BigDecimal(String.valueOf(totalScore));
				// me.setTotalScore(Double.parseDouble(bd.setScale(5,
				// BigDecimal.ROUND_HALF_UP).toString()));
				me.setTotalScore(totalScore);
			} catch (Exception e) {
				me.setTotalScore(0.0);
			}
			if (me.getTotalScore() > 0.0) {
				averageScore += totalScore;
				scoreSize++;
			}
		}

		// 取得建議
		wikiWordList = getRecommend(locale, wikiWordList, averageScore, scoreSize);
		return wikiWordList;
	}

	private static double getIDFValue(int pageDF, int pageCount) {
		// 若 pageDF 為 0 ，則預設 idf 為 0.5
		if (pageDF == 0)
			return 3.5;
		else
			return Math.log10((double) pageCount / pageDF);
	}

	private static WikiWordList getRecommend(HitHotLocale locale, WikiWordList wikiWordList,
			double averageScore, int size) {

		Collections.sort(wikiWordList, sort);
		double percentage = (double) Percent.getHighLightPercent() / 100;
		if (percentage != 0.0) {
			int i = 0;
			for (WikiWord wiki : wikiWordList) {
				if (i <= Math.ceil(wikiWordList.size() * percentage)) {
					wiki.setRecommand(Constants.RECOMMEND);
				} else {
					wiki.setRecommand(" ");
				}
				i++;
			}
		} else {
			//double avg = averageScore / size;
			int threshold = 10;
			switch (locale) {
				case zh_TW:
					threshold = 30;
					break;
				case zh_CN:
					threshold = 30;
					break;
				case en_US:
					threshold = 1;
					break;
				default:
					threshold = 10;
			}
			
			for (WikiWord wiki : wikiWordList) {
				if (wiki.getTotalScore() >= threshold) {
					wiki.setRecommand(Constants.RECOMMEND);
				} else {
					wiki.setRecommand(" ");
				}
			}
		}

		// 根據總分排序
		return wikiWordList;
	}

	// 非數字、非符號、非英文字，才算長詞
	private static double addLongTermWeight(String word, double longTermWeight) {
		int count = 0;
		String s;
		if (word.length() >= 3 && isImportantWikiWord(word)) {
			char charArray[] = word.toCharArray();
			for (Character c : charArray) {
				s = c.toString();
				if (!StringUtils.isAsciiPrintable(s))
					count++;
				if (Character.isUpperCase(c))
					count++;
			}
		}

		if (count >= 3)
			return longTermWeight + (count - 3) * 0.5; // 每超過 1， 加0.5
		else
			return 0;
	}

	public static boolean isImportantWikiWord(String word) {
		if (containNotImportantWord(word) && containChineseNumber(word))
			return false;
		else
			return true;
	}

	public static boolean containNotImportantWord(String word) {
		for (String s : notImportantWord) {
			if (StringUtils.contains(word, s))
				return true;
		}
		return false;
	}

	public static boolean containChineseNumber(String word) {
		for (String s : chineseNumber) {
			if (StringUtils.contains(word, s))
				return true;
		}
		return false;
	}

	private static double getAdjustScroe(WikiWord word) {
		if (word.getPriority() > 0) {
			return word.getPriority() * 10000;
		} else if (word.getPriority() < 0) {
			return word.getPriority() * 10000;
		} else {
			double f = word.getFrequency();
			double l = word.getLinks();
			double s = word.getPageDF();
			double numerator = (l + f) * Math.sqrt(f);
			double denominator = Math.pow(s, 1.5) + f;
			double adjustScore = numerator / denominator;
			return adjustScore;
		}
	}*/
}
