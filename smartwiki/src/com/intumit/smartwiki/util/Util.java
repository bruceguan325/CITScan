package com.intumit.smartwiki.util;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Util {

    private static Log log = LogFactory.getLog(Util.class);

    /**
     * 將傳入的 text 中, 指定的 original head, tail 轉成 new head, tail <br />
     * ex : wrapper("==BOLD==","==","==","--","||") 會被轉成 --BOLD||
     *
     * @param text
     * @param orgHead
     * @param orgTail
     * @param newHead
     * @param newTail
     * @return
     */
    public static String wrapper(
        String text,
        String orgHead,
        String orgTail,
        String newHead,
        String newTail) {
        StringBuffer newText = new StringBuffer();
        String tmpText = text;
        int counter = 0;

        int start = StringUtils.indexOf(tmpText, orgHead);
        int end = StringUtils.indexOf(tmpText, orgTail, start
            + orgHead.length());
        while ((start != -1) && (end != -1)) {
            if (counter++ > Constants.LOOP_LIMIT) {
                log.error("Infinate loop?");
                break;
            }
            if (newText.length() > 0)
                newText.append(newTail);
            newText.append(tmpText.substring(0, start)).append(newHead);
            newText.append(tmpText.substring(start + orgHead.length(), end));
            tmpText = tmpText.substring(
                end + orgTail.length(),
                tmpText.length());
            start = StringUtils.indexOf(tmpText, orgHead);
            end = StringUtils.indexOf(tmpText, orgTail, start
                + orgHead.length());
        }
        if (newText.length() > 0)
            newText.append(newTail);
        newText.append(tmpText);

        return newText.toString();
    }

    /**
     * 將指定的 orgHead 和 orgTail 中的內容移除 ( 包括 orgHead 和 orgTail ) <br />
     * Wiki word 中有些 tag 會迴圈式的存在, 這些可能的 tag 被記錄在
     * Constants.NON_NECESSARY_INNER_TAGS <br />
     * 當指定的 orgTail 與這些相同時, 會排除掉 <br />
     * 如果未來有發現 remover 移除內容有問題時, 可檢查是否有未處理的 INNER tag 需要增加
     *
     * @param text
     * @param orgHead
     * @param orgTail
     * @return
     */
    public static String remover(String text, String orgHead, String orgTail) {
        StringBuffer newText = new StringBuffer();
        String tmpText = text;
        int counter = 0;

        int start = StringUtils.indexOf(tmpText, orgHead);
        int end = StringUtils.indexOf(tmpText, orgTail, start
            + orgHead.length());

        if ((start != -1) && (end != -1)) {
            for (int i = 0; i < Constants.NON_NECESSARY_INNER_TAGS.length; i++) {
                if (orgTail.equals(Constants.NON_NECESSARY_INNER_TAGS[i][1])) {
                    end = getRealEnd(
                        tmpText,
                        start + orgHead.length(),
                        end,
                        Constants.NON_NECESSARY_INNER_TAGS[i][0],
                        Constants.NON_NECESSARY_INNER_TAGS[i][1]);
                }
            }
        }

        while ((start != -1) && (end != -1)) {
            if (counter++ > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
            newText.append(tmpText.substring(0, start));
            tmpText = removeStart(tmpText.substring(
                end + orgTail.length(),
                tmpText.length()), Constants.WIKI_NEW_LINE2);
            start = StringUtils.indexOf(tmpText, orgHead);
            end = StringUtils.indexOf(tmpText, orgTail, start
                + orgHead.length());

            if ((start != -1) && (end != -1)) {
                for (int i = 0; i < Constants.NON_NECESSARY_INNER_TAGS.length; i++) {
                    if (orgTail.equals(Constants.NON_NECESSARY_INNER_TAGS[i][1])) {
                        end = getRealEnd(
                            tmpText,
                            start + orgHead.length(),
                            end,
                            Constants.NON_NECESSARY_INNER_TAGS[i][0],
                            Constants.NON_NECESSARY_INNER_TAGS[i][1]);
                    }
                }
            }
        }
        newText.append(tmpText);

        return newText.toString();
    }

    public static String removeStart(String text, String head) {
        return removeStart(text, head, 0);
    }

    /**
     * 將指定的 head 自 text 的開頭移除, 移除的次數由 maxTimes 指定, 0 表示全移除
     * @param text
     * @param head
     * @param maxTimes
     * @return
     */
    public static String removeStart(String text, String head, int maxTimes) {
        String newText = StringUtils.trim(text);
        if((newText == null)||(newText.length()==0))
            return newText;

        int counter = 0;
        while (StringUtils.indexOf(newText, head) == 0) {
            if ((maxTimes > 0) && (++counter > maxTimes))
                break;
            if (counter > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
            newText = StringUtils.trim(StringUtils.substringAfter(newText, head));
        }
        return newText;
    }

    public static String removeTail(String text, String tail) {
        return removeTail(text, tail, 0);
    }

    /**
     * 將指定的 tail 自 text 的結尾移除, 移除的次數由 maxTimes 指定, 0 表示全移除
     * @param text
     * @param tail
     * @param maxTimes
     * @return
     */
    public static String removeTail(String text, String tail, int maxTimes) {
        String newText = StringUtils.trim(text);
        if((newText == null)||(newText.length()==0))
            return newText;

        int counter = 0;
        while ((StringUtils.lastIndexOf(newText, tail)>-1)&&(StringUtils.lastIndexOf(newText, tail) == (newText.length() - tail.length()))) {
            if ((maxTimes > 0) && (++counter > maxTimes))
                break;
            if (counter > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
            newText = StringUtils.substringBeforeLast(newText, tail);
        }
        return newText;
    }

    /**
     * 將簡體中文轉成繁體中文
     *
     * @param text
     * @return
     */
    public static String translatorS2T(String text) {
        String newText = processChineseDisplaySyntax(
            text,
            Constants.ZH_TW_VALUE);

        // 將不轉換的字詞包起來
        newText = wrapNoChange(newText, Constants.DICTIONARY_NO_CHANGE_TW);

        // 用 -{ 和 }- 包起來的字詞是不須轉換的
        String[] all = splitByStartEndTag(
            newText,
            Constants.WIKI_ZH_TAG_START,
            Constants.WIKI_ZH_TAG_END);

        for (int i = 0; i < all.length; i++) {
            // -{ 開頭的字串是不須轉換的字串
            if (StringUtils.indexOf(all[i], Constants.WIKI_ZH_TAG_START) == 0) {
                all[i] = Util.removeStart(all[i], Constants.WIKI_ZH_TAG_START);
                continue;
            }

            for (NameValuePair pair : Constants.DICTIONARY_S2T_PHRASE) {
                all[i] = StringUtils.replace(
                    all[i],
                    pair.getName(),
                    pair.getValue());
            }

            for (NameValuePair pair : Constants.DICTIONARY_S2T_WORD) {
                all[i] = StringUtils.replace(
                    all[i],
                    pair.getName(),
                    pair.getValue());
            }
        }

        return StringUtils.join(all);
    }

    /**
     * 將繁體中文轉成簡體中文
     *
     * @param text
     * @return
     */
    public static String translatorT2S(String text) {
        String newText = processChineseDisplaySyntax(
            text,
            Constants.ZH_CN_VALUE);

        // 將不轉換的字詞包起來
        newText = wrapNoChange(newText, Constants.DICTIONARY_NO_CHANGE_CN);

        // 用 -{ 和 }- 包起來的字詞是不須轉換的
        String[] all = splitByStartEndTag(
            newText,
            Constants.WIKI_ZH_TAG_START,
            Constants.WIKI_ZH_TAG_END);

        for (int i = 0; i < all.length; i++) {
            if (StringUtils.indexOf(all[i], Constants.WIKI_ZH_TAG_START) == 0) {
                all[i] = Util.removeStart(all[i], Constants.WIKI_ZH_TAG_START);
                continue;
            }
            for (NameValuePair pair : Constants.DICTIONARY_T2S_PHRASE) {
                all[i] = StringUtils.replace(
                    all[i],
                    pair.getName(),
                    pair.getValue());
            }

            for (NameValuePair pair : Constants.DICTIONARY_T2S_WORD) {
                all[i] = StringUtils.replace(
                    all[i],
                    pair.getName(),
                    pair.getValue());
            }
        }

        return StringUtils.join(all);
    }

    public static String translatorNone(String text) {
        String newText = processChineseDisplaySyntax(
            text,
            Constants.ZH_NO_VALUE);

        // 用 -{ 和 }- 包起來的字詞是不須轉換的
        String[] all = splitByStartEndTag(
            newText,
            Constants.WIKI_ZH_TAG_START,
            Constants.WIKI_ZH_TAG_END);

        for (int i = 0; i < all.length; i++) {
            if (StringUtils.indexOf(all[i], Constants.WIKI_ZH_TAG_START) == 0) {
                all[i] = Util.removeStart(all[i], Constants.WIKI_ZH_TAG_START);
                continue;
            }
        }

        return StringUtils.join(all);
    }

    public static String generateWikiUrl(
        String wiki,
        String wikiDisplay,
        String preferLang) {
        StringBuffer wikiUrl = new StringBuffer();

        try {
            wiki = URLEncoder.encode(wiki, "UTF-8");
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        String uuid = UUID.getUUID();
        wikiUrl.append("<a class=\"swiki\" href=\"").append(Constants.WIKI_LOOKUP).append(wiki);
        
        
        wikiUrl.append("&preferLang=").append(兩碼轉四碼(preferLang));
        wikiUrl.append("&uuid=").append(StringUtils.defaultString(uuid));
        if (uuid == null) {
    		wikiUrl.append("\" target=\"_self\">").append(wikiDisplay).append("</a>");
        } else {
            if (uuid.equals("8bfceffb-3126-11dc-a600-9b4ef35943ed")) 
        	wikiUrl.append("\" >").append(wikiDisplay).append("</a>");
            else if (uuid.equals("34e6792a-3126-11dc-a600-9b4ef35943ed"))
        	wikiUrl.append("\" onclick=\"parent.openLinkAtFlyout(this.href); return false;\" >").append(wikiDisplay).append("</a>");
	    else
	    	wikiUrl.append("\" target=\"_self\">").append(wikiDisplay).append("</a>");
        }

        return wikiUrl.toString();
    }
    
    private static String 兩碼轉四碼(String preferLang) {
		for (LanguageCoutry lc : LanguageCoutry.values()) {
			if(lc.getLangCode().equals(preferLang))
				return lc.name();
		}
		return LanguageCoutry.en_US.name();
	}

	public static String generateExtUrl(
            String link,
            String linkName) {
            String linkUrl = null;

            try {
                link = URLEncoder.encode(link.substring(7), "UTF-8");
            }
            catch (Exception e) {
                log.error(e.getMessage());
            }
            linkUrl = "<a href=\"http://" + link + "\" target=\"_self\">" + linkName + "</a>";
            return linkUrl;
        }

    private static String processChineseDisplaySyntax(
        String text,
        String chineseDisplayType) {
        StringBuffer newText = new StringBuffer();
        String tmpText = text;
        String tmpWord = null;
        String[] replaceAllTerm = null;
        int counter = 0;

        int start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
        int end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END, start
            + Constants.WIKI_ZH_TAG_START.length());

        while ((start > -1) && (end > -1) && (start < end)) {
            if (counter++ > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
            newText.append(tmpText.substring(0, start));
            tmpWord = StringUtils.substring(tmpText, start, end);
            newText.append("-{").append(
                parseChineseDisplayWord(tmpWord, chineseDisplayType)).append(
                "}-");
            // 如果遇到 -{A| 的語法, 表示之後的都必須一併轉換
            if ((StringUtils.indexOf(tmpWord, "-{A|") == 0)) {
                replaceAllTerm = parseReplaceTerm(tmpWord, chineseDisplayType);
                if (replaceAllTerm.length > 1) {
                    for (int i = 1; i < replaceAllTerm.length; i++) {
                        tmpText = StringUtils.replace(
                            tmpText,
                            replaceAllTerm[i],
                            replaceAllTerm[0]);
                    }
                }
                // 如果有 replace, 重新找 end, 以免因 replace 導致長度有變
                end = StringUtils.indexOf(
                    tmpText,
                    Constants.WIKI_ZH_TAG_END,
                    start + Constants.WIKI_ZH_TAG_START.length());
            }

            tmpText = StringUtils.substring(tmpText, end
                + Constants.WIKI_ZH_TAG_END.length(), tmpText.length());
            start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
            end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END, start
                + Constants.WIKI_ZH_TAG_START.length());
        }
        newText.append(tmpText);

        return newText.toString();
    }

    private static String parseChineseDisplayWord(
        String text,
        String chineseDisplayType) {
        String newText = text;
        String displayTag = chineseDisplayType + ":";

        // -{zh:不轉換; zh_CN:簡體; zh_TW:繁體; zh_HK:港澳繁體; zh_SG:馬新簡體}-
        // 目前不處理 zh_HK 和 zh_SG

        //2009/10/13 因為wiki用以下來表示 繁體:zh-hant, 簡體:zh-hans
        boolean exist = newText.contains(displayTag);
        if((!exist) && (chineseDisplayType.equals(Constants.ZH_TW_VALUE) || chineseDisplayType.equals(Constants.ZH_HK_VALUE)) )
        {	if (newText.contains(Constants.LANGUAGE_WIKI_ZHTW))
	        {	exist = true;
		    	displayTag = Constants.LANGUAGE_WIKI_ZHTW + ":";
		    }
        }
        else if((!exist) && (chineseDisplayType.equals(Constants.ZH_CN_VALUE) || chineseDisplayType.equals(Constants.ZH_SG_VALUE)) )
        {	if (newText.contains(Constants.LANGUAGE_WIKI_ZHCN))
	        {	exist = true;
	        	displayTag = Constants.LANGUAGE_WIKI_ZHCN + ":";
	        }
        }
        
        // 如果要解析的內文沒有指定的 ZH tag
        if (!exist) {
            // 如果指定的 ZH tag 是 "zh", 找第一個可能的 tag 回傳
            if (chineseDisplayType.equals(Constants.ZH_NO_VALUE)) {
                if (newText.contains(Constants.ZH_TW_VALUE)) {
                    return parseChineseDisplayWord(
                        newText,
                        Constants.ZH_TW_VALUE);
                }
                else if (newText.contains(Constants.ZH_CN_VALUE)) {
                    return parseChineseDisplayWord(
                        newText,
                        Constants.ZH_CN_VALUE);
                }
                else if (newText.contains(Constants.ZH_HK_VALUE)) {
                    return parseChineseDisplayWord(
                        newText,
                        Constants.ZH_HK_VALUE);
                }
                else if (newText.contains(Constants.ZH_SG_VALUE)) {
                    return parseChineseDisplayWord(
                        newText,
                        Constants.ZH_SG_VALUE);
                }
                else {
                    return StringUtils.substringBeforeLast(
                        StringUtils.substringAfter(
                            newText,
                            Constants.WIKI_ZH_TAG_START),
                        Constants.WIKI_ZH_TAG_END);
                }
            }
            else if (chineseDisplayType.equals(Constants.ZH_TW_VALUE)) {
                //要解析的內文沒有指定的 ZH tag，且指定的是zh_TW：試試看zh_HK
            	return parseChineseDisplayWord(newText, Constants.ZH_HK_VALUE);
            }
            else if (chineseDisplayType.equals(Constants.ZH_CN_VALUE)) {
                //要解析的內文沒有指定的 ZH tag，且指定的是zh_CN：試試看zh_SG
                return parseChineseDisplayWord(newText, Constants.ZH_SG_VALUE);
            }
            else {
                // 如果指定的 ZH tag 不是 "zh", 用 "zh" 來解析
                return StringUtils.replace(
                    newText,
                    Constants.WIKI_ZH_TAG_START,
                    "",
                    1);
            }
        }
        int start = StringUtils.indexOf(newText, displayTag)
            + displayTag.length();
        int end = StringUtils.indexOf(newText, ";", start);
        if (end == -1)
            end = newText.length();

        return StringUtils.substring(newText, start, end);
    }

    private static String[] parseReplaceTerm(
        String text,
        String chineseDisplayType) {
        String[] result = StringUtils.split(StringUtils.substringAfter(
            text,
            "-{A|"), ";");
        String tmp = null;

        for (int i = 0; i < result.length; i++) {
            result[i] = StringUtils.trim(result[i]);
            if (result[i].startsWith(chineseDisplayType)) {
                result[i] = StringUtils.trim(StringUtils.substringAfter(
                    result[i],
                    ":"));
                if (i != 0) {
                    tmp = result[i];
                    result[i] = result[0];
                    result[0] = tmp;
                }
            }
            else
                result[i] = StringUtils.trim(StringUtils.substringAfter(
                    result[i],
                    ":"));
        }
        return result;
    }

    public static int getHeadCharLength(String text, String head) {
        int length = 0;
        String tmpText = text;
        while (StringUtils.indexOf(tmpText, head) == 0) {
            length++;
            tmpText = Util.removeStart(tmpText, head, 1);
        }

        return length;
    }

    private static int getRealEnd(
        String text,
        int oriStart,
        int oriEnd,
        String innerStart,
        String innerEnd) {
        int finalEnd = oriEnd;
        int tmpStart = oriStart;
        int tmpEnd = oriEnd;
        int counter = 0;

        while (StringUtils.indexOf(text.substring(tmpStart, tmpEnd), innerStart) > -1) {
            if (counter++ > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
            String tmp = text.substring(tmpStart, text.length());

            finalEnd = StringUtils.indexOf(tmp, innerEnd, tmpEnd + innerEnd.length() - tmpStart)
                + tmpStart;
            tmpStart = tmpEnd;
            tmpEnd = finalEnd;
            if ((tmpStart < 0) || (tmpEnd < 0) || (tmpEnd < tmpStart))
                break;
        }

        return finalEnd;
    }

    private static String[] splitByStartEndTag(
        String text,
        String startTag,
        String endTag) {
        String tmpText = text;
        String[] result = new String[StringUtils.countMatches(tmpText, endTag) * 2 + 1];
        int counter = 0;

        int start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
        int end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END, start
            + Constants.WIKI_ZH_TAG_START.length());

        while ((start > -1) && (end > -1) && (start < end)) {
            result[counter] = tmpText.substring(0, start);
            result[counter + 1] = StringUtils.substring(tmpText, start, end);

            tmpText = StringUtils.substring(tmpText, end
                + Constants.WIKI_ZH_TAG_END.length(), tmpText.length());
            start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
            end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END, start
                + Constants.WIKI_ZH_TAG_START.length());
            counter += 2;
            if (counter > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
        }
        result[counter] = tmpText;

        return result;
    }

    public static String[] splitBetween(

    String text, String startTag, String endTag) {
        String tmpText = text;
        String[] result = new String[StringUtils.countMatches(tmpText, endTag)];
        int counter = 0;

        int start = StringUtils.indexOf(tmpText, startTag);
        int end = StringUtils.indexOf(tmpText, endTag, start
            + startTag.length());

        while ((start > -1) && (end > -1) && (start < end)) {
            result[counter++] = tmpText.substring(start + startTag.length(), end);

            tmpText = StringUtils.substring(
                tmpText,
                end + endTag.length(),
                tmpText.length());
            start = StringUtils.indexOf(tmpText, startTag);
            end = StringUtils.indexOf(tmpText, endTag, start
                + startTag.length());
            if (counter > Constants.LOOP_LIMIT) {
                log.error("Infinite loop?");
                break;
            }
        }

        return result;
    }

    private static String wrapNoChange(
        String text,
        ArrayList<NameValuePair> list) {
        String tmpText = text;
        String tmpWord = null;
        StringBuffer newText = new StringBuffer();

        for (NameValuePair pair : list) {
            tmpText = StringUtils.replace(
                tmpText,
                pair.getName(),
                Constants.WIKI_ZH_TAG_START
                    + pair.getValue()
                    + Constants.WIKI_ZH_TAG_END);
        }

        int start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
        int end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END);

        while ((start > -1) && (end > -1) && (start < end)) {
            newText.append(StringUtils.substring(tmpText, 0, start
                + Constants.WIKI_ZH_TAG_START.length()));

            tmpWord = StringUtils.substring(tmpText, start
                + Constants.WIKI_ZH_TAG_START.length(), end);

            if (StringUtils.contains(tmpWord, Constants.WIKI_ZH_TAG_START)) {
                end = StringUtils.indexOf(
                    tmpText,
                    Constants.WIKI_ZH_TAG_END,
                    end + Constants.WIKI_ZH_TAG_END.length());
                tmpWord = StringUtils.substring(tmpText, start
                    + Constants.WIKI_ZH_TAG_START.length(), end);
                tmpWord = StringUtils.replace(StringUtils.replace(
                    tmpWord,
                    Constants.WIKI_ZH_TAG_START,
                    ""), Constants.WIKI_ZH_TAG_END, "");
            }
            newText.append(tmpWord).append(Constants.WIKI_ZH_TAG_END);

            tmpText = StringUtils.substring(tmpText, end
                + Constants.WIKI_ZH_TAG_END.length(), tmpText.length());
            start = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_START);
            end = StringUtils.indexOf(tmpText, Constants.WIKI_ZH_TAG_END, start
                + Constants.WIKI_ZH_TAG_START.length());
        }

        newText.append(tmpText);

        return newText.toString();
    }

    /**
     * @param pageTitle 要分析的 page title
     * @return String[] 如果 page title 只有一個字, 將這個字的字首大寫後傳回. 如果有多個字, 傳回的陣列有三.
     *         第一個只有第一個的字首轉大寫, 其餘不變. 第二個只有第一個字的字首轉大寫, 其餘小寫. 第三個所有字的字首都轉大寫
     */
    public static String[] getpossiblePageTitle(String pageTitle) {
        String[] tmp = StringUtils.split(StringUtils.replace(
            pageTitle,
            " ",
            "_"), "_");
        String[] result = null;

        if (tmp.length == 0) {
            return result;
        }
        else {
            result = new String[3];
            tmp[0] = StringUtils.capitalize(tmp[0]);
            result[0] = StringUtils.join(tmp, " "); // 第一個字的字首大寫, 其餘不變

            for (int i = 0; i < tmp.length; i++)
                tmp[i] = StringUtils.lowerCase(tmp[i]);
            tmp[0] = StringUtils.capitalize(tmp[0]);
            result[1] = StringUtils.join(tmp, " "); // 第一個字的字首大寫, 其餘小寫

            result[2] = WordUtils.capitalizeFully(result[0]); // 所有字的字首大寫,
            // 其餘小寫
            // 將空白轉回底線
            for (int i = 0; i < result.length; i++)
                result[i] = StringUtils.replace(result[i], " ", "_");
        }

        return result;
    }

    /**
     * 判斷字元是否為分隔符號
     *
     * @param T
     * @return
     */
    public static boolean isSeparator(char T) {
        return (T == ' ' || T == '_' || T == '.' || T == ',' || T == '\n');
    }

    /**
     * 判斷是否為英文
     *
     * @param text
     * @return
     */
    public static boolean isEnglish(String text) {
        if (text == null)
            return false;
        if (text.length() == 0)
            return false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 255)
                return false;
        }
        return true;
    }
    

    /**
     * 在'\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '"', '{', '}', '~', '*', '?', '|', '&'
     * 前面加上'\\'
     *
     * @param text
     * @return
     */
    public static String escape(String s) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
