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

/**
 * Only for Taiwan Phonetic Symbols
 * It use synonym concept to add homonyms
 */
public final class PhoneticHomonymFilter extends TokenFilter {
	State bufferedState;
	Stack<String> currentListOfHomonyms = null;
	
	CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
	OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	PhoneticHomonymFilter(TokenStream input) {
		super(input);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (currentListOfHomonyms != null && currentListOfHomonyms.size() > 0) {
			restoreState(bufferedState);
			posIncAtt.setPositionIncrement(0);
			termAtt.setEmpty().append(currentListOfHomonyms.pop());
			
			return true;
		}
		else if (input.incrementToken()) {
			if (currentListOfHomonyms == null)
				currentListOfHomonyms = new Stack<String>();
			else 
				currentListOfHomonyms.clear();
			
			try {
				String str = termAtt.toString();
				CJKPhoneticsUtil.findAllPhonetics(str, currentListOfHomonyms);
				if (currentListOfHomonyms.size() > 0) {
					bufferedState = captureState();
				}
			} catch (Exception ignored) {
				//ignored.printStackTrace();
			}
			return true;
		}
		return false;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		if (currentListOfHomonyms != null)
			currentListOfHomonyms.clear();
		bufferedState = null;
	}
}
