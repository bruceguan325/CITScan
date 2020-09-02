package com.intumit.solr.wikiindex;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.solr.util.chinesetranslate.ChineseTranslator;

public class WikiTextTransformer extends Transformer {
	final Pattern WIKIWORDS = Pattern.compile("(?s)\\[\\[([^\\]\\[:]+)\\]\\]");
	final Pattern REDIRECT = Pattern
			.compile("(?is)#(redirect|REDIRECTION)[ :]*\\[\\[([^]]+)\\]\\]");
	final Pattern DISAMBIGUATION = Pattern.compile("(.*) \\(.*\\)");
	final Pattern DATE = Pattern
			.compile("(\\d{1,4}年)?(\\d{1,2}月)?(\\d{1,2}日)?");

	@Override
	public Object transformRow(Map<String, Object> row, Context context) {

		String title = (String) row.get("Name_s");

		if (DATE.matcher(title).matches())
			return null;

		row.put("PageTitle_s", title);

		title = title.replaceAll("_", " ");
		// Replace parentheses in the title.
		if (title.contains("(")) {
			Matcher m = DISAMBIGUATION.matcher(title);
			if (m.find()) {
				title = m.group(1);
				row.put("Disambiguation_b", true);
			}
		}

		if (StringUtils.isBlank(title) || title.length() == 1
				|| title.matches("\\d+")
				// Replace lucene query special characters.
				|| title.matches(".*[%$+-/&|!{}\\[\\]\\^\"~*?:\\\\].*")
				//
				|| title.matches("\\p{Punct}+"))
			return null;

		row.put("Name_s", title);

		boolean toCN = false;
		String text = null;
		String key = null;
		// Process the wiki content.
		for (Map<String, String> map : context.getAllEntityFields()) {
			if (BooleanUtils.toBoolean(map.get("wikitext"))) {
				toCN = BooleanUtils.toBoolean(map.get("to_zh_CN"));
				key = map.get(DataImporter.COLUMN);
				text = (String) row.get(key);
			}
		}

		if (key == null || text == null)
			return null;

		Matcher m;
		// Is it has redirection?
		m = REDIRECT.matcher(text);
		if (m.find()) {
			String word = m.group(2);

			if (DATE.matcher(word).matches())
				return null;

			row.put("Redirect_s", word);
			row.put("Redirect_b", true);
		}
		// END

		// Clean URLs.
		text = text.replaceAll("http://[[a-zA-Z_0-9=&]*./?-]*", "");
		// END

		if (toCN)
			text = ChineseTranslator.getInstance().translateT2S(text);

		// Get all links from here.
		Set<String> linkms = new HashSet<String>();
		m = WIKIWORDS.matcher(text);
		while (m.find()) {
			String words = m.group(1);
			for (String s : words.split("\\|")) {
				if (StringUtils.isEmpty(s) || s.length() == 1)
					continue;
				// Too slow
				// if (DATE.matcher(s).matches())
				// continue;
				linkms.add(s);
			}
		}

		if (!linkms.isEmpty())
			row.put("LinkTo_ms", linkms);
		// END

		row.put(key, text);

		if (toCN)
			title = ChineseTranslator.getInstance().translateT2S(title);
		row.put("Name_s", title);

		return row;
	}
}
