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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.RegexTransformer;
import org.apache.solr.handler.dataimport.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * 系統預設是使用格林威治標準時間來作處理，所有時間都會轉成格林威治 這導致後續查詢時有時會比較麻煩，必須將時間自行換算才能作查詢
 * 
 * 雖不建議使用此 Transformer，但若非不得以的情況下，仍可使用此 Transformer 預先將日期調整 例如，如果以台灣的時區，是
 * +8小時，於是可以設定 timezone="+8"，Transformer 會將時間先行 +8小時
 * 如此換算的格林威治時間將會與台灣時間是相同（後續就可以把格林威治時間當作台灣時間直接查詢）
 * </p>
 */
public class TimeZoneTransformer extends Transformer {
	private static final Logger LOG = LoggerFactory
			.getLogger(TimeZoneTransformer.class);

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> aRow, Context context) {
		for (Map<String, String> map : context.getAllEntityFields()) {
			String tzOffsetStr = map.get(TIMEZONE);
			if (tzOffsetStr == null)
				continue;
			String column = map.get(DataImporter.COLUMN);
			String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
			
			if (srcCol == null)
				srcCol = column;
			
			if (tzOffsetStr.startsWith("+"))
				tzOffsetStr = tzOffsetStr.substring(1);
			
			int tzOffset = Integer.parseInt(tzOffsetStr);
			Object o = aRow.get(srcCol);
			
			if (o instanceof List) {
				List inputs = (List) o;
				List<Date> results = new ArrayList<Date>();
				
				for (Object input : inputs) {
					results.add(process(input, tzOffset));
				}
				aRow.put(column, results);
			} else {
				if (o != null) {
					aRow.put(column, process(o, tzOffset));
				}
			}
		}
		return aRow;
	}

	private Date process(Object value, int tzOffset) {
		if (value == null)
			return null;

		if (value instanceof Date) {
			Date dateVal = (Date) value;
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateVal);
			cal.add(Calendar.HOUR, tzOffset);

			return cal.getTime();
		}
		return null;
	}

	public static final String TIMEZONE = "timezone";
}
