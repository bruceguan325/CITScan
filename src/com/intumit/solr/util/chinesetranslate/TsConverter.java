package com.intumit.solr.util.chinesetranslate;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.XStream;

public class TsConverter {

	private static final TsConverter instance = new TsConverter();
	private StringLengthOrderMap zhHans;
	private StringLengthOrderMap zhHant;

	private TsConverter() {
		super();
	}

	public static TsConverter getInstance() {
		return instance;
	}

	public static void main(String[] args) throws Exception {
		String origin = FileUtils.readFileToString(new File("text"));

		TsConverter cv = new TsConverter();
		// for (int i = 0; i < 10; i++) {
		String result = cv.t2S(origin);
		System.out.println(result);
		System.out.println(System.currentTimeMillis());
		// }
	}

	public String s2T(String text) {
		if (StringUtils.isEmpty(text))
			return text;

		if (zhHant == null)
			try {
				zhHant = (StringLengthOrderMap) new XStream().fromXML(FileUtils
						.readFileToString(new File(Config.hantFile), "utf8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		return replaceWithOrder(text, zhHant);
	}

	public String t2S(String text) {
		if (StringUtils.isEmpty(text))
			return text;

		if (zhHans == null)
			try {
				zhHans = (StringLengthOrderMap) new XStream().fromXML(FileUtils
						.readFileToString(new File(Config.hansFile), "utf8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		return replaceWithOrder(text, zhHans);
	}

	// TODO The fast way is building a tree to map all phrases.
	private String replaceWithOrder(String template,
			StringLengthOrderMap stringLengthOrderMap) {

		Map<Integer, Entry<String, String>> resaultmap = new TreeMap<Integer, Entry<String, String>>();
		for (Entry<String, String> e : stringLengthOrderMap.entrySet()) {
			if (template.length() < e.getKey().length())
				continue;

			int position = -1;
			while ((position = template.indexOf(e.getKey(), position + 1)) > -1) {
				if (!resaultmap.containsKey(position)) {
					resaultmap.put(position, e);
				}
			}
		}

		int idx = 0;
		StringBuffer sb = new StringBuffer();
		for (Entry<Integer, Entry<String, String>> e : resaultmap.entrySet()) {
			// Some positions of terms may conflict, for example, the "崑崙=>昆仑"
			// and "崑=>昆".
			if (e.getKey() < idx)
				continue;

			sb.append(template.substring(idx, e.getKey()));
			sb.append(e.getValue().getValue());
			idx = e.getKey() + e.getValue().getKey().length();
		}
		sb.append(template.substring(idx));
		return sb.toString();
	}

}
