package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import com.mariten.kanatools.KanaConverter;

public class KuromojiUtil {
	final int conv_op_kata_and_hira_to_zen_flags = KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA;
	final int conv_op_ascii_han2zen = KanaConverter.OP_HAN_ASCII_TO_ZEN_ASCII | KanaConverter.OP_HAN_KATA_TO_ZEN_KATA;
	
	public String convertAsciiHanToZen(String mixed) {
		return KanaConverter.convertKana(mixed, conv_op_ascii_han2zen);
	}
	public String convert(String mixed, int op) {
		return KanaConverter.convertKana(mixed, op);
	}

	public String segAndToHiragana(String mixed) {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}
	
	public List<String> segAndToHiragana(List<String> mixed) {
		List<String> newList = new ArrayList<>();
		mixed.forEach( str -> newList.add(segAndToHiragana(str)) );
		
		return newList;
	}
	
	protected static String kuromojiTagMapping(String[] allFeatures) {
		if (allFeatures != null && allFeatures.length > 0) {
			String allFeaturesStr = StringUtils.join(allFeatures, ",");
			List<String> l = Arrays.asList(allFeatures);
			
			if (l.contains("数")) {
				return "m#jp#" + allFeaturesStr;
			}
			else if (StringUtils.startsWith(allFeaturesStr, "名詞")) {
				return "n#jp#" + allFeaturesStr;
			}
			else if (StringUtils.startsWith(allFeaturesStr, "動詞")) {
				return "v#jp#" + allFeaturesStr;
			}
			else if (StringUtils.startsWithAny(allFeaturesStr, new String[] {"助詞", "助動詞"})) {
				return "u#jp#" + allFeaturesStr;
			}
			else if (StringUtils.startsWithAny(allFeaturesStr, new String[] {"記号"})) {
				return "w#jp#" + allFeaturesStr;
			}
			else if (StringUtils.contains(allFeaturesStr, "UserDefined")) {
				return "#UserDefined#jp#" + allFeaturesStr;
			}
			
			return allFeaturesStr;
		}
		return "??";
	}
}
