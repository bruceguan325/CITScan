package com.intumit.solr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

/**
 * 把原本 hierarchy tree 欄位切割做成 multiValued 以便 filter 用，如 /a/b/c => /a、/a/b、/a/b/c
 */
public class HierarchySplitTransformer extends Transformer {

    private static final String COL = "hierarchy";

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
                            List<String> cleaned = split(str);

                            if (cleaned.size() > 0) {
                                row.put(key, cleaned);
                            }
                            else {
                                row.remove(key);
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return row;
    }

    private List<String> split(String str) {

        List<String> hierarchy = new ArrayList<String>();
        if(StringUtils.isBlank(str)) return hierarchy;

        try {
            String path = "";

            String[] values = str.split("/");
            for(String s : values) {
                path = path + s +"/";
                hierarchy.add(path);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return hierarchy;
    }


}
