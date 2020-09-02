/*
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
package org.apache.solr.handler.dataimport;

import static org.apache.solr.handler.dataimport.DataImportHandlerException.SEVERE;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.wrapAndThrow;
import static org.apache.solr.handler.dataimport.XPathEntityProcessor.URL;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author herb
 */
public class JsonEntityProcessor extends EntityProcessorBase {
  private static final Logger LOG = LoggerFactory.getLogger(JsonEntityProcessor.class);
  
  private JSONArray jArr = null;
  private boolean ended = false;
  private int idx = 0;

  @Override
  public void init(Context context) {
    super.init(context);
    ended = false;
    jArr = null;
    idx = 0;
  }

  @Override
  public Map<String, Object> nextRow() {
    if (ended) return null;
    
    if (jArr == null) {
	    DataSource<Reader> ds = context.getDataSource();
	    String url = context.replaceTokens(context.getEntityAttribute("dataField"));
	    String path = context.replaceTokens(context.getEntityAttribute("path"));
	    Reader r = null;
	    try {
	      r = ds.getData(url);
	    } catch (Exception e) {
	      wrapAndThrow(SEVERE, e, "Exception reading url : " + url);
	    }
	    StringWriter sw = new StringWriter();
	    char[] buf = new char[1024];
	    while (true) {
	      int len = 0;
	      try {
	        len = r.read(buf);
	      } catch (IOException e) {
	        IOUtils.closeQuietly(r);
	        wrapAndThrow(SEVERE, e, "Exception reading url : " + url);
	      }
	      if (len <= 0) break;
	      sw.append(new String(buf, 0, len));
	    }
	    
	    String jsonStr = sw.toString();
	    
	    try {
			if (path.equals(".")) {
				try {
					jArr = new JSONArray(jsonStr);
				}
				catch (Exception e) {
					jArr = new JSONArray();
					JSONObject jobj = new JSONObject(jsonStr);
					jArr.put(jobj);
				}
			}
			else {
				String[] pathes = path.split("\\.");
				JSONObject jobj = new JSONObject(jsonStr);
				Object currObj = jobj;
				
				for (String currPath: pathes) {
					if (currObj instanceof JSONObject && ((JSONObject)currObj).has(currPath)) {
						currObj = ((JSONObject)currObj).get(currPath);
					}
					else {
						ended = true;
					}
				}
				
				if (currObj instanceof JSONArray) {
					jArr = (JSONArray)currObj;
				}
				else {
					ended = true;
				}
			}
		}
		catch (JSONException e) {
	        wrapAndThrow(SEVERE, e, "Exception finding json path [" + path + "]");
		}
	    IOUtils.closeQuietly(r);
    }
    
    if (jArr != null && idx < jArr.length()) {
	    try {
			Map<String, Object> row = new HashMap<>();
			row.put("arrIndex_i", idx);
			row.put(PLAIN_TEXT, jArr.get(idx++).toString());
			return row;
		}
		catch (JSONException e) {
	        wrapAndThrow(SEVERE, e, "Cannot get data from json array (" + (idx-1) + ")");
		}
    }
    else {
	    ended = true;
    }
    return null;
  }

  public static final String PLAIN_TEXT = "plainText";
}
