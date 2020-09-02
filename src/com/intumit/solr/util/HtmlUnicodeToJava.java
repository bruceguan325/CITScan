package com.intumit.solr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 html unicode 內碼轉成 java unicode，&#nnnn; or &#xnnnnn; &#(\\d{1,5});?|&#x(\\w{1,5});?
 * 10 進位 16 進位都可以吃，另外沒有加  ; 的話跟瀏覽器一樣，容錯。
 * @author sboschang
 *
 */
public class HtmlUnicodeToJava {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

         String str ="<br><br>&#xFF3B;&#x7279;&#x96C6;&#x60C5;&#x5831;&#xFF3D;<br>&#x8868;&#x7D19;&#xFF1A;&#x5CB8;&#x672C;&#x30BB;&#x30B7;&#x30EB;&#x3000;&#x7279;&#x5225;&#x4ED8;&#x9332;&#xFF1A;&#xFF34;&#xFF4F;&#x3000;&#xFF42;&#xFF45;&#x3000;&#xFF42;&#xFF59;&#x30A2;&#x30CB;&#x30A8;&#x30B9;&#xFF42;&#xFF0E;&#x30D3;&#x30C3;&#x30B0;&#x30B5;&#x30A4;&#x30BA;&#x30C8;&#x30FC;&#x30C8;&#x30D0;&#x30C3;&#x30B0;<br><br>&#x2605;&#x7279;&#x5225;&#x4ED8;";
        // String temps[] = StringUtils.splitPreserveAllTokens(str, "&#");
        //String str = "&#8243&#8243;&#xFF3B&#xFF3B;";
        System.out.println(convertHtml2Java(str));
    }

    public static String convertHtml2Java(String source) {

        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile(
            "&#(\\d{1,5});?|&#x(\\w{1,5});?").matcher(
            source);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                matcher.appendReplacement(
                    sb,
                    String.valueOf((char) (Integer.valueOf(matcher.group(1)).intValue())));
            }
            else if (matcher.group(2) != null) {
                matcher.appendReplacement(
                    sb,
                    String.valueOf((char) (Integer.valueOf(matcher.group(2), 16).intValue())));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String replaceHtmlCharacterToUnicode(String s) {
        StringBuffer source = new StringBuffer(s);
        StringBuffer result = new StringBuffer();

        int iIndex = 0;
        do {
            int iSign = iIndex;
            iIndex = source.indexOf("&#", iIndex);

            if (iIndex < 0) {
                result.append(source.substring(iSign));
                break;
            }

            int iIndex2 = source.indexOf(";", iIndex + 2);
            if (iIndex2 < 0) {
                result.append(source.substring(iSign));
                break;
            }

            if (iIndex > iSign) {
                result.append(source.substring(iSign, iIndex));
            }

            try {
                int iWord = Integer.parseInt(source.substring(
                    (iIndex + 2),
                    iIndex2));
                char c = (char) iWord;
                result.append(c);
                iIndex = iIndex2 + 1;
            }
            catch (Exception e) {
                int iTemp = source.indexOf("&#", iIndex + 2);
                if (iIndex2 < iTemp) {
                    result.append(source.substring(iIndex, iIndex2 + 1));
                    iIndex = iIndex2 + 1;
                }
                else {
                    result.append(source.substring(iIndex, iTemp));
                    iIndex = iTemp;
                }
            }
        }
        while (true);

        return result.toString();
    }

}
