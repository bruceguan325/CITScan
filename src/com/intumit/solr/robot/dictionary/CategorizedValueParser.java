package com.intumit.solr.robot.dictionary;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import org.elasticsearch.common.lang3.StringUtils;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.util.WiSeUtils;

public class CategorizedValueParser {
	//static ULocale locale = ULocale.TAIWAN;
	static ULocale locale = ULocale.getDefault();
	static NumberFormat nf = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);

	public static String toDateRangeQuery(CategorizedKeyValuePair kv) {
		try {
			String val = (String)kv.getValue();
			//"最近"
			if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.lately").equals(val)) {
				return "[NOW/DAY-7DAY TO NOW]";
			}
			//"今天"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.nowadays").equals(val)) {
				return "[NOW/DAY TO NOW]";
			}
			//"昨天"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.yesterdat").equals(val)) {
				return "[NOW/DAY-1DAY TO NOW/DAY]";
			}
			//"前天"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.before.yesterday").equals(val)) {
				return "[NOW/DAY-2DAY TO NOW/DAY-1DAY]";
			}
			//"上週"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.last.week").equals(val)) {
				return "[NOW/DAY-14DAY TO NOW/DAY-7DAY]";
			}
			//"下週"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.next.week").equals(val)) {
				return "[NOW/DAY TO NOW/DAY+7DAY]";
			}
			//"這個月"
			else if (MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.this.month").equals(val)) {
				return "[NOW/DAY-1DAY TO NOW/DAY]";
			}
			//"月"
			else if (StringUtils.endsWith(val, MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.month"))) {
				try {
					Calendar cal = Calendar.getInstance(Locale.ENGLISH);
					Number num = nf.parse(StringUtils.substringBeforeLast(val, MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.month")));
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MONTH, num.intValue() - 1);
					cal.set(Calendar.DATE, 1);
					
					Calendar endCal = (Calendar)cal.clone();
					endCal.add(Calendar.MONTH, 1);
					endCal.add(Calendar.SECOND, -1);
					
					return "[" + WiSeUtils.toSolrDateStr(cal) + " TO " + WiSeUtils.toSolrDateStr(endCal) + "]";
				}
				catch (ParseException e) {
					System.out.println("Unparseable date [" + val + "]: " + e.getMessage());
				}
			}
			//"年"
			else if (StringUtils.endsWith(val, MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.year"))) {
				try {
					Calendar cal = Calendar.getInstance(Locale.ENGLISH);
					Number num = nf.parse(StringUtils.substringBeforeLast(val, MessageUtil.getMessage(Locale.TRADITIONAL_CHINESE, "global.year")));
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MONTH, 0);
					cal.set(Calendar.DATE, 1);
					cal.set(Calendar.YEAR, num.intValue());
					
					Calendar endCal = (Calendar)cal.clone();
					endCal.add(Calendar.YEAR, 1);
					endCal.add(Calendar.SECOND, -1);
					
					return "[" + WiSeUtils.toSolrDateStr(cal) + " TO " + WiSeUtils.toSolrDateStr(endCal) + "]";
				}
				catch (ParseException e) {
					System.out.println("Unparseable date [" + val + "]: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String toNumberRangeQuery(CategorizedKeyValuePair kv) {
		String val = (String)kv.getValue();
		try {
			Number num = nf.parse(val);
			return "[" + num + " TO " + num + "]";
		}
		catch (ParseException e) {
			System.out.println("Unparseable number [" + val + "]: " + e.getMessage());
		}
		return null;
	}
	
	public static Number toNumber(CategorizedKeyValuePair kv) {
		String val = (String)kv.getValue();
		try {
			Number num = nf.parse(val);
			return num;
		}
		catch (ParseException e) {
			System.out.println("Unparseable number [" + val + "]: " + e.getMessage());
		}
		return null;
	}
	
	public static void main(String[] args) {
		MessageUtil.initialize();
		String tmp = CategorizedValueParser.toNumberRangeQuery(new CategorizedKeyValuePair("A", "万二", CategorizedKeyValuePair.Type.NUMBER));
		System.out.println(tmp);
		String dateRangeQuery = CategorizedValueParser.toDateRangeQuery(new CategorizedKeyValuePair("A", "昨天", CategorizedKeyValuePair.Type.CUSTOM));
		System.out.println(dateRangeQuery);
		
	}
}
