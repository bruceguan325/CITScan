package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atilika.kuromoji.ipadic.neologd.Token;
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
		List<Token> tokens = QAUtil.nativeJpTokenizer.tokenize(mixed);
		StringBuilder sb = new StringBuilder();
		
		for (Token t: tokens) {
			String kata = t.getReading();
			String converted = kata;

    		if (StringUtils.trimToNull(kata) == null || "*".equals(kata)) {
    			converted = t.getSurface();
    		}
    		else {
    			converted = KanaConverter.convertKana(kata, conv_op_kata_and_hira_to_zen_flags);
    		}
			
			System.out.println(String.format("%s at %d / converted %s ---------------", t.getSurface(), t.getPosition(), converted));
			System.out.println(String.format("[reading]%s, [pronunciation]%s", t.getReading(), t.getPronunciation()));
			System.out.println(String.format("[base]%s,[reading]%s,[conj]%s,[conjType]%s", t.getBaseForm(), t.getReading(), t.getConjugationForm(), t.getConjugationType()));
			System.out.println(String.format("[1]%s,[2]%s,[3]%s,[4]%s,[5]%s,[6]%s,[7]%s,[8]%s,[9]%s", t.getAllFeaturesArray()));
			System.out.println("---------------------------------");
			
			sb.append(converted);
		}
		
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
