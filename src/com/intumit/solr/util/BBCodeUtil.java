/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.intumit.solr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="BBCodeUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Alexander Chow
 *
 */
public class BBCodeUtil {

	static Map<Integer, String> fontSizes = new HashMap<Integer, String>();

	static Map<String, String> listStyles = new HashMap<String, String>();

	static String[][] emoticons = {
		{"angry.gif", ":angry:"},
		{"bashful.gif", ":bashful:"},
		{"big_grin.gif", ":grin:"},
		{"blink.gif", ":blink:"},
		{"blush.gif", ":*)"},
		{"bored.gif", ":bored:"},
		{"closed_eyes.gif", "-_-"},
		{"cold.gif", ":cold:"},
		{"cool.gif", "B)"},
		{"darth_vader.gif", ":vader:"},
		{"dry.gif", "<_<"},
		{"exclamation.gif", ":what:"},
		{"girl.gif", ":girl:"},
		{"glare.gif", ">_>"},
		{"happy.gif", ":)"},
		{"huh.gif", ":huh:"},
		{"in_love.gif", "<3"},
		{"karate_kid.gif", ":kid:"},
		{"kiss.gif", ":#"},
		{"laugh.gif", ":lol:"},
		{"mad.gif", ":mad:"},
		{"mellow.gif", ":mellow:"},
		{"ninja.gif", ":ph34r:"},
		{"oh_my.gif", ":O"},
		{"pac_man.gif", ":V"},
		{"roll_eyes.gif", ":rolleyes:"},
		{"sad.gif", ":("},
		{"sleep.gif", ":sleep:"},
		{"smile.gif", ":D"},
		{"smug.gif", ":smug:"},
		{"suspicious.gif", "8o"},
		{"tongue.gif", ":P"},
		{"unsure.gif", ":unsure:"},
		{"wacko.gif", ":wacko:"},
		{"wink.gif", ":wink:"},
		{"wub.gif", ":wub:"}
	};

	static {
		fontSizes.put(new Integer(1), "<span style='font-size: 0.7em';>");
		fontSizes.put(new Integer(2), "<span style='font-size: 0.8em';>");
		fontSizes.put(new Integer(3), "<span style='font-size: 0.9em';>");
		fontSizes.put(new Integer(4), "<span style='font-size: 1.0em';>");
		fontSizes.put(new Integer(5), "<span style='font-size: 1.1em';>");
		fontSizes.put(new Integer(6), "<span style='font-size: 1.3em';>");
		fontSizes.put(new Integer(7), "<span style='font-size: 1.5em';>");

		listStyles.put("1", "<ol style='list-style-type: decimal';>");
		listStyles.put("i", "<ol style='list-style-type: lower-roman';>");
		listStyles.put("I", "<ol style='list-style-type: upper-roman';>");
		listStyles.put("a", "<ol style='list-style-type: lower-alpha';>");
		listStyles.put("A", "<ol style='list-style-type: upper-alpha';>");

		for (int i = 0; i < emoticons.length; i++) {
			String[] emoticon = emoticons[i];

			String image = emoticon[0];
			String code = emoticon[1];

			emoticon[0] =
				"<img alt='emoticon' src='@theme_images_path@/emoticons/" +
					image + "' />";
			emoticon[1] = code;//HtmlUtil.escape(code);
		}
	}

	public static final String[][] EMOTICONS = emoticons;

	public static String getHTML(String bbcode) {
		//String html = HtmlUtil.escape(bbcode);

		String html = StringUtil.replace(bbcode, _BBCODE_TAGS, _HTML_TAGS);

		for (int i = 0; i < emoticons.length; i++) {
			String[] emoticon = emoticons[i];

			html = StringUtil.replace(html, emoticon[1], emoticon[0]);
		}

		BBCodeTag tag = null;

		StringBuilder sb = null;

		while ((tag = getFirstTag(html, "code")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			String code = tag.getElement().replaceAll("\t", StringPool.FOUR_SPACES);
			String[] lines = code.split("\\n");
			int digits = String.valueOf(lines.length + 1).length();

			sb = new StringBuilder(preTag);

			sb.append("<div class='code'>");

			for (int i = 0; i < lines.length; i++) {
				String index = String.valueOf(i + 1);
				int ld = index.length();

				sb.append("<span class='code-lines'>");

				for (int j = 0; j < digits - ld; j++) {
					sb.append("&nbsp;");
				}

				lines[i] = StringUtil.replace(lines[i], "   ",
					StringPool.NBSP + StringPool.SPACE + StringPool.NBSP);
				lines[i] = StringUtil.replace(lines[i], "  ",
					StringPool.NBSP + StringPool.SPACE);

				sb.append(index + "</span>");
				sb.append(lines[i]);

				if (index.length() < lines.length) {
					sb.append("<br />");
				}
			}

			sb.append("</div>");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "color")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			if (tag.hasParameter()) {
				sb.append("<span style='color: ");
				sb.append(tag.getParameter() + ";'>");
				sb.append(tag.getElement() + "</span>");
			}
			else {
				sb.append(tag.getElement());
			}

			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "email")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			String mailto = WiSeUtils.output(
				tag.getParameter(), "", "", tag.getElement().trim());

			sb = new StringBuilder(preTag);

			sb.append("<a href='mailto: " + mailto + "'>");
			sb.append(tag.getElement() + "</a>");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "font")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			if (tag.hasParameter()) {
				sb.append("<span style='font-family: ");
				sb.append(tag.getParameter() + "';>");
				sb.append(tag.getElement() + "</span>");
			}
			else {
				sb.append(tag.getElement());
			}

			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "img")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			sb.append("<img alt='' src='" + tag.getElement().trim() + "' />");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "IMG")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			sb.append("<img alt='' src='" + tag.getElement().trim() + "' />");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "list")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			String[] items = _getListItems(tag.getElement());

			sb = new StringBuilder(preTag);

			if (tag.hasParameter() &&
				listStyles.containsKey(tag.getParameter())) {

				sb.append(listStyles.get(tag.getParameter()));

				for (int i = 0; i < items.length; i++) {
					if (items[i].trim().length() > 0) {
						sb.append("<li>" + items[i].trim() + "</li>");
					}
				}

				sb.append("</ol>");
			}
			else {
				sb.append("<ul style='list-style-type: disc';>");

				for (int i = 0; i < items.length; i++) {
					if (items[i].trim().length() > 0) {
						sb.append("<li>" + items[i].trim() + "</li>");
					}
				}

				sb.append("</ul>");
			}

			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "quote")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			if (tag.hasParameter()) {
				sb.append("<div class='quote-title'>");
				sb.append(tag.getParameter() + ":</div>");
			}

			sb.append("<div class='quote'>");
			sb.append("<div class='quote-content'>");
			sb.append(tag.getElement());
			sb.append("</div></div>");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "size")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			sb = new StringBuilder(preTag);

			if (tag.hasParameter()) {
				Integer size = new Integer(
					WiSeUtils.output(tag.getParameter(), "", "", "0").replaceAll("[^0-9]", ""));

				if (size.intValue() > 7) {
					size = new Integer(7);
				}

				if (fontSizes.containsKey(size)) {
					sb.append(fontSizes.get(size));
					sb.append(tag.getElement() + "</span>");
				}
				else {
					sb.append(tag.getElement());
				}
			}
			else {
				sb.append(tag.getElement());
			}

			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "url")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());

			String url = WiSeUtils.output(
				tag.getParameter(), "", "", tag.getElement().trim());

			sb = new StringBuilder(preTag);

			sb.append("<a href='" + url + "'>");
			sb.append(tag.getElement() + "</a>");
			sb.append(postTag);

			html = sb.toString();
		}

		while ((tag = getFirstTag(html, "b")) != null) {
			String preTag = html.substring(0, tag.getStartPos());
			String postTag = html.substring(tag.getEndPos());
			sb = new StringBuilder(preTag);
			sb.append("<b>" + tag.getElement() + "</b>");
			sb.append(postTag);

			html = sb.toString();
		}

		html = StringUtil.replace(html, "\n", "<br />");

		return html;
	}

	public static BBCodeTag getFirstTag(String bbcode, String name) {
		BBCodeTag tag = new BBCodeTag();

		String begTag = "[" + name;
		String endTag = "[/" + name + "]";

		String preTag = StringUtil.extractFirst(bbcode, begTag);

		if (preTag == null) {
			return null;
		}

		if (preTag.length() != bbcode.length()) {
			tag.setStartPos(preTag.length());

			String remainder = bbcode.substring(
				preTag.length() + begTag.length());

			int cb = remainder.indexOf("]");
			int end = _getEndTagPos(remainder, begTag, endTag);

			if (end < 0 || cb >= remainder.length() || end >= remainder.length() || cb >= (end-1))
				return null;
			
			if (cb > 0 && remainder.startsWith("=")) {
				tag.setParameter(remainder.substring(1, cb));
				tag.setElement(remainder.substring(cb + 1, end));
			}
			else if (cb == 0) {
				try {
					tag.setElement(remainder.substring(1, end));
				}
				catch (StringIndexOutOfBoundsException sioobe) {
					_log.error(bbcode);

					throw sioobe;
				}
			}
		}

		if (tag.hasElement()) {
			int length =
				begTag.length() + 1 + tag.getElement().length() +
					endTag.length();

			if (tag.hasParameter()) {
				length += 1 + tag.getParameter().length();
			}

			tag.setEndPos(tag.getStartPos() + length);

			return tag;
		}

		return null;
	}

	private static int _getEndTagPos(
		String remainder, String begTag, String endTag) {

		int nextBegTagPos = remainder.indexOf(begTag);
		int nextEndTagPos = remainder.indexOf(endTag);

		while ((nextBegTagPos < nextEndTagPos) && (nextBegTagPos >= 0)) {
			nextBegTagPos = remainder.indexOf(
				begTag, nextBegTagPos + begTag.length());
			nextEndTagPos = remainder.indexOf(
				endTag, nextEndTagPos + endTag.length());
		}

		return nextEndTagPos;
	}

	private static String[] _getListItems(String tagElement) {
		List<String> items = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();

		int nestLevel = 0;

		for (String item : StringUtil.split(tagElement, "[*]")) {
			item = item.trim();

			if (item.length() == 0) {
				continue;
			}

			int begTagCount = StringUtil.count(item, "[list");

			if (begTagCount > 0) {
				nestLevel += begTagCount;
			}

			int endTagCount = StringUtil.count(item, "[/list]");

			if (endTagCount > 0) {
				nestLevel -= endTagCount;
			}

			if (nestLevel == 0) {
				if ((begTagCount == 0) && (endTagCount == 0)) {
					items.add(item);
				}
				else if (endTagCount > 0) {
					if (sb.length() > 0) {
						sb.append("[*]");
					}

					sb.append(item);

					items.add(sb.toString());

					sb.delete(0, sb.length());
				}
			}
			else {
				if (sb.length() > 0) {
					sb.append("[*]");
				}

				sb.append(item);
			}
		}

		return items.toArray(new String[items.size()]);
	}
	
	public static void main(String[] args) {
		String content = "这是瓶子和CHICHY送给大家新年香香的礼物~~~*^_^* (注：此贴是我俩用了一天时间亲手编辑而成，另外感谢我朋友提供的完美翻译!!!如转载必告知!!!) Guerlain，中文叫娇兰。 娇兰是纯以香水成名的公司。香水世家娇兰公司的创办人JACQUES GUERLAIN是近代调香师鼻祖，在同行中被称为[名鼻]，他也是拿破仑的御用调香师。 在早期香料种类有限的年代,他以近100种香料调制出50种包括[L\'HEURE BLEUE][蝴蝶夫人][一千零一夜]的经典名香。 娇兰世家历经三代，从皮耶娇兰到杰克娇兰，再到保罗娇兰，青出于蓝而胜于蓝。 [IMG]http://www.fzpp.net/non-cgi/usr/35/35_216.jpg[/IMG] 〈午夜飞行>是<小王子>的作者圣修伯利的另一部作品，叙述的是在南美洲飞行的故事。 1933年，娇兰为〈午夜飞行>创作了同名香水，出世迷离。 Vol de Nuit（夜间飞行） 神秘东方香水系列，香水代表的是兴奋和冒险的精神，香水瓶身的装饰标志是法国的空军军旗。 前味：以柑橘味为主，包括橘花、佛手柑、柠檬和橙花油 中味：花梨木、茉莉、伊兰花 后味：龙涎-波莎迷可香系列，以香草、安息香、白檀香、龙涎香、皮革香为主 [IMG]http://photo.gznet.com/photos/1199650/1199650-mn6hK2PEQs.JPG[/IMG] Mitsouko（蝴蝶夫人） 这瓶香水据说是为了纪念一位因日俄战争而和身为海军军官的情人分离的日本女孩。每当这位军官遥望东方或闻到这种神秘的东方之香，就会想起那逝去的爱。1921年推出，是第一个使用合成香精的香水，流行至今。 前味：以佛手柑为主，还有少量柠檬、橘皮、橙花油、和现代水果-桃香 中味：芬芳花香，包括玫瑰、茉莉、伊兰花 后味：香草、安息香、白檀香、麝香、橡苔香草及石玫瑰香胶 [IMG]http://www.fzpp.net/non-cgi/usr/35/35_216_2.jpg[/IMG] Champs-Elysees （香榭里舍） 以法国巴黎著名的香榭里舍大道为名，最大的特色是以含羞草为主味的现代花香系列香水。它的瓶身由香榭里舍大道上的著名建筑巧妙组合而成，如卢浮宫的玻璃墙 金字塔、协和广场、凯旋门等，仿佛在告诉你， 这里是巴黎是花之首都、浪漫之王国。 前味：银合欢叶、玫瑰、杏花、蓝莓 中味：银合欢花、铃兰 后味：杏树、龙涎香 [IMG]http://www.fzpp.net/non-cgi/usr/35/35_216_3.jpg[/IMG] Shalimar （一千零一夜） 芬芳花香-神秘东方之香系列香水，1925年出。Shalimar是梵语中\"爱的神殿\"的意思。它原是印度王沙杰罕为他钟爱的妃子泰姬所建的一座美丽的花园的名字。 前味：以佛手柑、橘皮和花梨木香为主，辅以柑橘香 中味：芬芳花香系列为主。包括玫瑰、茉莉和鸢尾、并衬以木香 后味：以香草、苏合香为主 [IMG]http://www.fzpp.net/non-cgi/usr/35/35_216_4.jpg[/IMG] Jicky 是香水大师皮耶 娇兰在1889年推出的古典名作之一，也是世界上第一瓶所谓的现代香水。以其子杰克命名。香味属东方-神秘香和清新草绿香的结合。 前味：以柠檬为主，辅之以橘香、佛手柑与花梨木香 中味：以茉莉、爪哇薄荷为主，玫瑰、鸢尾为辅 后味：香草、波莎迷克香 [已被 蓝色香水瓶 编辑过, 在 2004-01-16 14:45]";
		
		String str = getHTML(content);
		System.out.println(str);
	}

	private static final String[] _BBCODE_TAGS = {
		"[b]", "[/b]", "[i]", "[/i]", "[u]", "[/u]", "[s]", "[/s]",
		"[img]", "[/img]",
		"[IMG]", "[/IMG]",
		"[left]", "[center]", "[right]", "[indent]",
		"[/left]", "[/center]", "[/right]", "[/indent]", "[tt]", "[/tt]"
	};

	private static final String[] _HTML_TAGS = {
		"<b>", "</b>", "<i>", "</i>", "<u>", "</u>", "<strike>", "</strike>",
		"<img alt='' src='", "' />",
		"<div style='text-align: left'>", "<div style='text-align: center'>",
		"<div style='text-align: right'>", "<div style='margin-left: 15px'>",
		"</div>", "</div>", "</div>", "</div>", "<tt>", "</tt>"
	};

	private static Log _log = LogFactory.getLog(BBCodeUtil.class);

}
