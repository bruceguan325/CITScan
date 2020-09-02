/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intumit.solr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * 將 Date 轉換成指定 format 的 String
 * 跟 solr.DateFormatTransformer 剛好相反， solr 原有的是將 String parse 成 Date 物件
 *  solr 的名稱感覺取錯了，應該叫做 DateParseTransformer
 * </p>
 */
public class DateFormatTransformer extends Transformer {
  private Map<String, SimpleDateFormat> fmtCache = new HashMap<String, SimpleDateFormat>();
  private static final Logger LOG = LoggerFactory
          .getLogger(DateFormatTransformer.class);

  @SuppressWarnings("unchecked")
  public Object transformRow(Map<String, Object> aRow, Context context) {
    for (Map<String, String> map : context.getAllEntityFields()) {
      String fmt = map.get(DATE_TIME_FMT);
      if (fmt == null)
        continue;
      String column = map.get(DataImporter.COLUMN);
      String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
      if (srcCol == null)
        srcCol = column;
      try {
        Object o = aRow.get(srcCol);
        if (o instanceof List) {
          List inputs = (List) o;
          List<String> results = new ArrayList<String>();
          for (Object input : inputs) {
            results.add(process(input, fmt));
          }
          aRow.put(column, results);
        } else {
          if (o != null) {
            aRow.put(column, process(o, fmt));
          }
        }
      } catch (ParseException e) {
        LOG.warn("Could not format a Date field ", e);
      }
    }
    return aRow;
  }

  private String process(Object value, String format) throws ParseException {
    if (value == null) return null;
    
    if (value instanceof Date) {
	    Date dateVal = (Date)value;
	    
	    SimpleDateFormat fmt = fmtCache.get(format);
	    if (fmt == null) {
	      fmt = new SimpleDateFormat(format);
	      fmtCache.put(format, fmt);
	    }
	    return fmt.format(dateVal);
    }
    return null;
  }

  public static final String DATE_TIME_FMT = "dateTimeFormat";
}
