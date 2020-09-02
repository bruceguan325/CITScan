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

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImportHandlerException;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.DataSource;
import org.apache.solr.handler.dataimport.EntityProcessor;
import org.apache.solr.handler.dataimport.EntityProcessorBase;
import org.apache.solr.handler.dataimport.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An {@link EntityProcessor} instance which provides support for reading from
 * databases. It is used in conjunction with {@link JdbcDataSource}. This is the default
 * {@link EntityProcessor} if none is specified explicitly in data-config.xml
 * </p>
 * <p/>
 * <p>
 * Refer to <a
 * href="http://wiki.apache.org/solr/DataImportHandler">http://wiki.apache.org/solr/DataImportHandler</a>
 * for more details.
 * </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 *
 *
 * @since solr 1.3
 */
public class MySqlBatchEntityProcessor extends EntityProcessorBase {
	private static final Logger LOG = LoggerFactory
			.getLogger(MySqlBatchEntityProcessor.class);

  protected DataSource<Iterator<Map<String, Object>>> dataSource;

	private int batchSize;
	private int offset = 0;

  @Override
  @SuppressWarnings("unchecked")
  public void init(Context context) {
    super.init(context);
    dataSource = context.getDataSource();
		batchSize = Integer.parseInt(context.getEntityAttribute("batchSize"));
  }

  protected void initQuery(String q) {
    try {
      DataImporter.QUERY_COUNT.get().incrementAndGet();
			rowIterator = dataSource.getData(q + " LIMIT " + offset + "," + batchSize);
      this.query = q;
    } catch (DataImportHandlerException e) {
      throw e;
    } catch (Exception e) {
      LOG.error( "The query failed '" + q + "'", e);
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE, e);
    }
  }

  @Override
  public Map<String, Object> nextRow() {    
    if (rowIterator == null) {
      String q = getQuery();
      initQuery(context.replaceTokens(q));
    }
    return getNext();
  }

  @Override
  public Map<String, Object> nextModifiedRowKey() {
    if (rowIterator == null) {
      String deltaQuery = context.getEntityAttribute(DELTA_QUERY);
      if (deltaQuery == null)
        return null;
      initQuery(context.replaceTokens(deltaQuery));
    }
    return getNext();
  }

  @Override
  public Map<String, Object> nextDeletedRowKey() {
    if (rowIterator == null) {
      String deletedPkQuery = context.getEntityAttribute(DEL_PK_QUERY);
      if (deletedPkQuery == null)
        return null;
      initQuery(context.replaceTokens(deletedPkQuery));
    }
    return getNext();
  }

  @Override
  public Map<String, Object> nextModifiedParentRowKey() {
    if (rowIterator == null) {
      String parentDeltaQuery = context.getEntityAttribute(PARENT_DELTA_QUERY);
      if (parentDeltaQuery == null)
        return null;
      LOG.info("Running parentDeltaQuery for Entity: "
              + context.getEntityAttribute("name"));
      initQuery(context.replaceTokens(parentDeltaQuery));
    }
    return getNext();
  }

  public String getQuery() {
    String queryString = context.getEntityAttribute(QUERY);
    if (Context.FULL_DUMP.equals(context.currentProcess())) {
      return queryString;
    }
    if (Context.DELTA_DUMP.equals(context.currentProcess())) {
      String deltaImportQuery = context.getEntityAttribute(DELTA_IMPORT_QUERY);
      if(deltaImportQuery != null) return deltaImportQuery;
    }
    LOG.warn("'deltaImportQuery' attribute is not specified for entity : "+ entityName);
    return getDeltaImportQuery(queryString);
  }

  public String getDeltaImportQuery(String queryString) {    
    StringBuilder sb = new StringBuilder(queryString);
    if (SELECT_WHERE_PATTERN.matcher(queryString).find()) {
      sb.append(" and ");
    } else {
      sb.append(" where ");
    }
    boolean first = true;
    String[] primaryKeys = context.getEntityAttribute("pk").split(",");
    for (String primaryKey : primaryKeys) {
      if (!first) {
        sb.append(" and ");
      }
      first = false;
      Object val = context.resolve("dataimporter.delta." + primaryKey);
      if (val == null) {
        Matcher m = DOT_PATTERN.matcher(primaryKey);
        if (m.find()) {
          val = context.resolve("dataimporter.delta." + m.group(1));
        }
      }
      sb.append(primaryKey).append(" = ");
      if (val instanceof Number) {
        sb.append(val.toString());
      } else {
        sb.append("'").append(val.toString()).append("'");
      }
    }
    return sb.toString();
  }

	protected Map<String, Object> getNext() {
		try {
			if (rowIterator == null)
				return null;
			if (rowIterator.hasNext()) {
				offset++;
				return rowIterator.next();
			}
			else if (offset >= (batchSize)) {
				DataImporter.QUERY_COUNT.get().incrementAndGet();
				rowIterator = dataSource.getData(query + " LIMIT " + offset + "," + batchSize);

				if (rowIterator.hasNext()) {
					offset++;
					return rowIterator.next();
				}
			}
			
			offset = 0;
			query = null;
			rowIterator = null;
			return null;
		} catch (Exception e) {
			LOG.error("getNext() failed for query '" + query + "' LIMIT(" + offset + "," + batchSize + ")", e);
			query = null;
			rowIterator = null;
			DataImportHandlerException.wrapAndThrow(DataImportHandlerException.WARN, e);
			return null;
		}
	}

  private static Pattern SELECT_WHERE_PATTERN = Pattern.compile(
          "^\\s*(select\\b.*?\\b)(where).*", Pattern.CASE_INSENSITIVE);

  public static final String QUERY = "query";

  public static final String DELTA_QUERY = "deltaQuery";

  public static final String DELTA_IMPORT_QUERY = "deltaImportQuery";

  public static final String PARENT_DELTA_QUERY = "parentDeltaQuery";

  public static final String DEL_PK_QUERY = "deletedPkQuery";

  public static final Pattern DOT_PATTERN = Pattern.compile(".*?\\.(.*)$");
}
