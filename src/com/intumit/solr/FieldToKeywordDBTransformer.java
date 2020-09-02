package com.intumit.solr;

import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;

/**
 * 把某欄位的值轉成 keyword
 *
 * @author sboschang
 *
 */
public class FieldToKeywordDBTransformer extends Transformer {

    private static final String COL = "toKeyword";

    @SuppressWarnings("unchecked")
    public Object transformRow(Map<String, Object> row, Context context) {

        for (Map<String, String> map : context.getAllEntityFields()) {
            String doIt = map.get(COL);
            if (doIt == null)
                continue;

            if (new Boolean(doIt)) {
                String key = map.get(DataImporter.COLUMN);
                Object value = row.get(key);

                if (value instanceof String) {
                    String str = (String) value;
                    try {
                        if (str != null) {
                            SearchKeywordLogFacade.getInstance().log(str);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } // for

        return row;
    }
}
