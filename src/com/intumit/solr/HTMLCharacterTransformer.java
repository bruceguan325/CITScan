package com.intumit.solr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;
import org.apache.solr.handler.dataimport.VariableResolver;

import com.intumit.solr.util.HtmlUnicodeToJava;

/**
 * 把 html unicode 內碼轉成 java unicode，&#nnnn; or &#xnnnnn; &#(\\d{1,5});?|&#x(\\w{1,5});?
 * 10 進位 16 進位都可以吃，另外沒有加  ; 的話跟瀏覽器一樣，容錯。
 * @author sboschang
 *
 */
public class HTMLCharacterTransformer extends Transformer {

    public static final String TRANS_UNICODE = "htmlUni2Java";
    public static final String TRUE = "true";

    @Override
    @SuppressWarnings("unchecked")
    public Object transformRow(Map<String, Object> row, Context context) {
        VariableResolver resolver = context.getVariableResolver();
        List<Map<String, String>> fields = context.getAllEntityFields();
        for (Map<String, String> field : fields) {
            String col = field.get(DataImporter.COLUMN);
            String srcCol = field.get(RegexTransformer.SRC_COL_NAME);
            String splitHTML = resolver.replaceTokens(field.get(TRANS_UNICODE));
            if (!TRUE.equals(splitHTML))
                continue;
            Object tmpVal = row.get(srcCol == null ? col : srcCol);
            if (tmpVal == null)
                continue;

            if (tmpVal instanceof List) {
                List<String> inputs = (List<String>) tmpVal;
                List results = new ArrayList();
                for (String input : inputs) {
                    if (input == null)
                        continue;
                    Object o = null;
                    try {
                        o = HtmlUnicodeToJava.convertHtml2Java(input);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(input);
                    }
                    if (o != null)
                        results.add(o);
                }
                row.put(col, results);
            }
            else {
                String value = tmpVal.toString();
                Object o = null;
                try {
                    o = HtmlUnicodeToJava.convertHtml2Java(value);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(value);
                }
                if (o != null)
                    row.put(col, o);
            }
        }
        return row;
    }
}
