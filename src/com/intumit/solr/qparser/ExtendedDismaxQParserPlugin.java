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

package com.intumit.solr.qparser;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 * An advanced multi-field query parser based on the DisMax parser. See Wiki
 * page http://wiki.apache.org/solr/ExtendedDisMax
 */
public class ExtendedDismaxQParserPlugin extends QParserPlugin {
	public static final String NAME = "edismax";

	public static final String SYNONYM = "syn";
	public static final String SEGMENT = "seg";
	public static final String TENANT_ID = "tenantId";
	public static final String NO_AUTO_PHRASE = "fuzzy";

	public static final String BIGRAM_MODE = "bigramMode";

	@Override
	public void init(NamedList args) {
	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		return new ExtendedDismaxQParser(qstr, localParams, params, req);
	}
}
