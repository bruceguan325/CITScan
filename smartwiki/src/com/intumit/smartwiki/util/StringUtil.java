package com.intumit.smartwiki.util;

import java.util.List;

import com.intumit.util.Assert;

public class StringUtil {

    /**
     * 把全形的數字和英文字轉成半形
     * 
     * @param s
     * @return
     */
    public static String convertFromFull(String s) {
        if (s == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // uff21 - uff3a : 大寫英文字
            // uff41 - uff5a : 小寫英文字
            // uff10 - uff19 : 數字
            if ((c >= '\uff21' && c <= '\uff3a')
                || (c >= '\uff41' && c <= '\uff5a')
                || (c >= '\uff10' && c <= '\uff19')) {
                c = (char) (c - '\ufee0');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String toSqlInList(List list) {
        Assert.assertNotNull(list);
        return toSqlInList(list.toArray());
    }

    public static String toSqlInList(Object[] values) {
        Assert.assertNotNull(values);
        Assert.assertTrue(values.length != 0);
        StringBuffer sb = new StringBuffer();
        sb.append(" (");
        sb.append(getSqlValue(values[0]));
        for (int i = 1; i < values.length; i++) {
            sb.append("," + getSqlValue(values[i]));
        }
        sb.append(") ");
        return sb.toString();
    }

    public static String toSqlInList(long[] values) {
        Assert.assertNotNull(values);
        Assert.assertTrue(values.length != 0);
        StringBuffer sb = new StringBuffer();
        sb.append(" (");
        sb.append(values[0]);
        for (int i = 1; i < values.length; i++) {
            sb.append("," + values[i]);
        }
        sb.append(") ");
        return sb.toString();
    }

    public static String toSqlInList(int[] values) {
        Assert.assertNotNull(values);
        Assert.assertTrue(values.length != 0);
        StringBuffer sb = new StringBuffer();
        sb.append(" (");
        sb.append(values[0]);
        for (int i = 1; i < values.length; i++) {
            sb.append("," + values[i]);
        }
        sb.append(") ");
        return sb.toString();
    }

    private static String getSqlValue(Object value) {
        if (value instanceof String) {
            return "'" + (String) value + "'";
        }
        else {
            return value.toString();
        }
    }

    public static boolean isEmpty(String keyword) {
        return (keyword == null) || keyword.trim().equals("");
    }

}
