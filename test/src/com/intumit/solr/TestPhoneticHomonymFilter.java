package com.intumit.solr;

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

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class TestPhoneticHomonymFilter extends BaseTokenStreamTestCase {
  Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer t = new StandardTokenizer(TEST_VERSION_CURRENT, reader);
      return new TokenStreamComponents(t, new PhoneticHomonymFilter(new CJKBigramFilter(t, CJKBigramFilter.HAN)));
    }
  };
  
/*  Analyzer unibiAnalyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer t = new StandardTokenizer(TEST_VERSION_CURRENT, reader);
      return new TokenStreamComponents(t, new PhoneticHomonymFilter(new CJKBigramFilter(t, CJKBigramFilter.HAN)));
    }
  };*/
  
  public void testAllScripts() throws Exception {
	 /* ArrayList<String> phonetics = new ArrayList<String>();
		
		try {
			CJKPhoneticsUtil.findAllPhonetics("同音詞", phonetics);
		} catch (Exception ignored) {
		}
	System.out.println(
			phonetics
			);*/
	
    assertAnalyzesTo(analyzer, "同音詞是指語",
        new String[] { "同音", "侗喑", "音詞", "喑茲", "詞是", "茲事", "是指", "事只", "指語", "只予", "只儥" },
        new int[] 	 { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0 }
    	);
  }
  
  
}
