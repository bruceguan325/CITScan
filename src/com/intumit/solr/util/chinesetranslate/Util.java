package com.intumit.solr.util.chinesetranslate;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class Util {

    private static Log log = LogFactory.getLog(Util.class);

    /**
     * 將傳?��??? text �?, ??��?��?? original head, tail 轉�?? new head, tail <br />
     * ex : wrapper("==BOLD==","==","==","--","||") ??�被轉�?? --BOLD||
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
     * 將�?��?��?? orgHead ??? orgTail 中�?�內容移?�� ( ??�括 orgHead ??? orgTail ) <br />
     * Wiki word 中�?��?? tag ??�迴??��?��?��?�在, ??��?�可?��??? tag 被�?��?�在
     * Constants.NON_NECESSARY_INNER_TAGS <br />
     * ?��??��?��?? orgTail ??��?��?�相??��??, ??��?�除??? <br />
     * 如�?�未來�?�發?�� remover 移除?��容�?��?��?��??, ?��檢查?��?��??�未??��?��?? INNER tag ???要�?��??
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
     * 將�?��?��?? head ?�� text ??��?�頭移除, 移除??�次?��?�� maxTimes ??��??, 0 表示?��移除
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
     * 將�?��?��?? tail ?�� text ??��?�尾移除, 移除??�次?��?�� maxTimes ??��??, 0 表示?��移除
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
     * 將簡體中??��?��?��?��?�中???
     *
     * @param text
     * @return
     */
    public static String translatorS2T(String text) {
        String newText = processChineseDisplaySyntax(
            text,
            Constants.ZH_TW_VALUE);

        // 將�?��?��?��?��?��?��?�起�?
        newText = wrapNoChange(newText, Constants.DICTIONARY_NO_CHANGE_TW);

        // ?�� -{ ??? }- ??�起來�?��?��?�是不�?��?��?��??
        String[] all = splitByStartEndTag(
            newText,
            Constants.WIKI_ZH_TAG_START,
            Constants.WIKI_ZH_TAG_END);

        for (int i = 0; i < all.length; i++) {
            // -{ ??�頭??��?�串?��不�?��?��?��?��?�串
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
     * 將�?��?�中??��?��?�簡體中???
     *
     * @param text
     * @return
     */
    public static String translatorT2S(String text) {
        String newText = processChineseDisplaySyntax(
            text,
            Constants.ZH_CN_VALUE);

        // 將�?��?��?��?��?��?��?�起�?
        newText = wrapNoChange(newText, Constants.DICTIONARY_NO_CHANGE_CN);

        // ?�� -{ ??? }- ??�起來�?��?��?�是不�?��?��?��??
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

        // ?�� -{ ??? }- ??�起來�?��?��?�是不�?��?��?��??
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
        String toLang) {
        StringBuffer wikiUrl = new StringBuffer();

        try {
            wiki = URLEncoder.encode(wiki, "UTF-8");
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }

        wikiUrl.append("<a href=\"").append(Constants.WIKI_LOOKUP).append(wiki);
        wikiUrl.append("&toLang=").append(toLang);
        wikiUrl.append("\" target=\"_self\">").append(wikiDisplay).append("</a>");

        return wikiUrl.toString();
    }

    public static String processChineseDisplaySyntax(
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
            // 如�?��?�到 -{A| ??��?��??, 表示之�?��?�都必�?��??併�?��??
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
                // 如�?��?? replace, ??�新?�� end, 以�?��?? replace 導致?��度�?��??
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

        // -{zh:不�?��??; zh-cn:簡�??; zh-tw:繁�??; zh-hk:港澳繁�??; zh-sg:馬新簡�?�}-
        // ?��??��?��?��?? zh-hk ??? zh-sg

        // 如�?��?�解??��?�內??��?��?��?��?��?? ZH tag
        if (!newText.contains(displayTag)) {
            // 如�?��?��?��?? ZH tag ?�� "zh", ?��第�????�可?��??? tag ??�傳
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
                return parseChineseDisplayWord(newText, Constants.ZH_HK_VALUE);
            }
            else if (chineseDisplayType.equals(Constants.ZH_CN_VALUE)) {
                return parseChineseDisplayWord(newText, Constants.ZH_SG_VALUE);
            }
            else {
                // 如�?��?��?��?? ZH tag 不是 "zh", ?�� "zh" 來解???
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
     * @param pageTitle 要�?��?��?? page title
     * @return String[] 如�?? page title ?��??��????��??, 將�?��?��?��?��?��?�大寫�?�傳???. 如�?��?��?��?��??, ?��??��?�陣??��?��??.
     *         第�????�只??�第�???��?��?��?��?�大�?, ?��餘�?��??. 第�?��?�只??�第�???��?��?��?��?��?�大�?, ?��餘�?�寫. 第�?��?��????��?��?��?��?�都轉大�?
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
            result[0] = StringUtils.join(tmp, " "); // 第�????��?��?��?��?�大�?, ?��餘�?��??

            for (int i = 0; i < tmp.length; i++)
                tmp[i] = StringUtils.lowerCase(tmp[i]);
            tmp[0] = StringUtils.capitalize(tmp[0]);
            result[1] = StringUtils.join(tmp, " "); // 第�????��?��?��?��?�大�?, ?��餘�?�寫

            result[2] = WordUtils.capitalizeFully(result[0]); // ?????��?��?��?��?�大�?,
            // ?��餘�?�寫
            // 將空?��轉�?��?��??
            for (int i = 0; i < result.length; i++)
                result[i] = StringUtils.replace(result[i], " ", "_");
        }

        return result;
    }

    /**
     * ?��?��字�?�是?��?��??��?�符???
     *
     * @param T
     * @return
     */
    public static boolean isSeparator(char T) {
        return (T == ' ' || T == '_' || T == '.' || T == ',' || T == '\n');
    }

    /**
     * ?��?��?��?��?��?��???
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
}
