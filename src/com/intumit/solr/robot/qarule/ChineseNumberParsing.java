package com.intumit.solr.robot.qarule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseNumberParsing {
    // 通過中文數字字符，在該字符串中查找其對應的數值或其表示的數位
    // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14
    // 零 一 二 三 四 五 六 七 八 九 十 百 千 萬 億
    private static final String dic = "零一二三四五六七八九十百千萬億";
    public static final Pattern CHINESE_NUMBER_PATTERN = Pattern.compile("[0-9" + dic + "兩]+");
    static final Pattern ASCII_NUMBER_PATTERN = Pattern.compile("[0-9]+");

    // 反向的數字模板
    private static final String model = "個十百千萬十百千億十百千";

    /**
     * 解析中文數字字符串，獲取它所表示的數值
     * 
     * @param numberZhString
     *            中文數字字符串
     * @return 中文數字字符串所代表的數值
     */
    public static Number parseLong(String numberZhString) {
        if (numberZhString == "") {
            return 0;
        }
        
        Matcher asciiMatcher = ASCII_NUMBER_PATTERN.matcher(numberZhString);
        
        // 這裡是把中文數字當中摻雜的阿拉伯數字直接硬轉中文，等整串文字都是中文之後才做繼續處理
        while (asciiMatcher.find()) {
        	String asciiPart = asciiMatcher.group();
        	numberZhString = numberZhString.replace(asciiPart, toString(Long.parseLong(asciiPart)));
        	asciiMatcher = ASCII_NUMBER_PATTERN.matcher(numberZhString);
        }
        
        // Hack for "兩" == "二"
        // 這裡之後應該可以 enhance 成「中文大寫數字如壹貳叁肆」這些都一起轉一轉
        numberZhString = numberZhString.replaceAll("兩", "二");
        
        /**
         * 開始正式處理中文數字 to java long
         * 此時 numberZhString 全部應該都是中文字（且只有 dic 中的字）
         */
        // 將傳入的中文數字字符串倒置 eg.五千萬零九百零三 -> 三零百九零萬千五
        numberZhString = new StringBuffer(numberZhString).reverse().toString();

        // 聲明一個數組，與反向的數字模板對應
        char[] resultChars = new char[12];
        // 將結果字符數組使用字符0初始化
        for (int i = 0; i < resultChars.length; i++) {
            resultChars[i] = '0';
        }

        // i -- 用來遍歷numberZhString的數組下標
        // j -- 用來訪問resultChars的數組下標
        for (int i = 0, j = 0; i < numberZhString.length(); i++) {
            // 依次取出numberZhString中的字符，如三，零，百，千
            char c = numberZhString.charAt(i);

            // 查找字符c所對應的數值或表示的數位
            // 如：三對應3，百對應11代表百位或百萬位或百億位
            int t = dic.indexOf(c);
            // 如果字符c代表一個數值，將該數值轉化為數字字符，並存入resultChars.
            if (t >= 1 && t <= 9) {
                resultChars[j] = (char) (t + '0');
            }
            // 如果字符c代表一個數位，移動resultChars的數組下標j
            else if (t >= 10 && t <= 14) {
                j = model.indexOf(c, j);
                
                if (t == 10) {
                	resultChars[j] = (char) (1 + '0');
                }
            }
            // 如果字符c代表0，什麼也不做
            else if (t == 0) {

            }
            // 否則，拋出運行期異常
            else {
                throw new RuntimeException("中文數字字符串包含非法字符！只能包含以下字符："
                        + "零、一、二、三、四、五、六、七、八、九、十、百、千、萬、億");
            }
        }

        // 獲取resultChars的字符串形式
        String resultString = String.copyValueOf(resultChars);
        // 將該字符串倒置
        resultString = new StringBuffer(resultString).reverse().toString();

        // 將該字符串解析為long返回
        return new Long(resultString);
    }

    /**
     * 返回長整數l的中文表示形式
     */
    public static String toString(long l) {
        String numberString = Long.toString(l);
        numberString = new StringBuffer(numberString).reverse().toString();

        char[] resultChars = new char[12];
        for (int i = 0; i < resultChars.length; i++) {
            resultChars[i] = '零';
        }

        for (int i = 0; i < numberString.length(); i++) {
            resultChars[i] = dic.charAt(numberString.charAt(i) - '0');
        }

        StringBuffer resultStringBuffer = new StringBuffer(20);
        for (int i = 0; i < numberString.length(); i++) {
            if (i == 4) {
                resultStringBuffer.append('萬');
            }
            if (i == 8) {
                resultStringBuffer.append('億');
            }
            if (resultChars[i] != '零') {
                if (i == 4 || i == 8) {
                    resultStringBuffer.append(resultChars[i]);
                } else {
                    resultStringBuffer.append("" + model.charAt(i)
                            + resultChars[i]);
                }
            } 
            else if (resultStringBuffer.length() == 0) {
            	resultStringBuffer.append('十');
            }
            else {
                if (resultStringBuffer.charAt(resultStringBuffer.length() - 1) != '零'
                        && resultStringBuffer.charAt(resultStringBuffer
                                .length() - 1) != '萬'
                        && resultStringBuffer.charAt(resultStringBuffer
                                .length() - 1) != '億') {
                    resultStringBuffer.append('零');
                }
            }
        }

        resultStringBuffer.deleteCharAt(0);
        resultStringBuffer.reverse();
        return resultStringBuffer.toString();
    }

    public static void main(String[] args) {
    	System.out.println(ChineseNumberParsing.toString(50L));
    	System.out.println(ChineseNumberParsing.toString(53L));
    	System.out.println(ChineseNumberParsing.toString(153L));
    	System.out.println(ChineseNumberParsing.toString(1153L));
    	System.out.println(ChineseNumberParsing.toString(11153L));
    	System.out.println(ChineseNumberParsing.toString(111153L));
        System.out.println(ChineseNumberParsing.parseLong("九百零三萬"));
        System.out.println(ChineseNumberParsing.parseLong("十一萬"));
        System.out.println(ChineseNumberParsing.parseLong("九百三"));
        System.out.println(ChineseNumberParsing.parseLong("50萬"));
        System.out.println(ChineseNumberParsing.parseLong("53萬"));
        System.out.println(ChineseNumberParsing.parseLong("53萬兩千300"));
        System.out.println(ChineseNumberParsing.parseLong("5千萬"));
        System.out.println(ChineseNumberParsing.parseLong("兩萬"));
        System.out.println(ChineseNumberParsing.toString(907300250302L));
    }
}