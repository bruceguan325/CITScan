package com.intumit.solr;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeSource.State;

import com.intumit.solr.util.chinesetranslate.ChineseTranslator;

/**
 * Only for Taiwan Phonetic Symbols
 * It use synonym concept to add homonyms
 */
public final class ChineseTranslateFilter extends TokenFilter {
	State bufferedState;
	Stack<String> termStack = null;
	
	CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
	OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	ChineseTranslateFilter(TokenStream input) {
		super(input);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (termStack != null && termStack.size() > 0) {
			restoreState(bufferedState);
			posIncAtt.setPositionIncrement(0);
			termAtt.setEmpty().append(termStack.pop());
			
			return true;
		}
		else if (input.incrementToken()) {
			try {
				StringBuffer buf = new StringBuffer();
				
				for (int i=0; i < termAtt.length(); i++) {
					Character theOtherChar = ChineseTranslator.getInstance().translateToTheOther(termAtt.charAt(i));
					
					if (theOtherChar != null)
						buf.append(theOtherChar);
					else 
						buf.append(termAtt.charAt(i));
					
					String orig = termAtt.toString();
					String translated = buf.toString();
					
					if (!StringUtils.equals(orig, translated)) {
						if (termStack == null)
							termStack = new Stack<String>();
						else 
							termStack.clear();
						
						bufferedState = captureState();
						termStack.add(translated);
					}
				}
			} catch (Exception ignored) {
			}
			return true;
		}
		return false;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		if (termStack != null)
			termStack.clear();
		bufferedState = null;
	}
}
