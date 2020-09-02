package com.intumit.android.search;

import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.CATENATE_ALL;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.GENERATE_WORD_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.PRESERVE_ORIGINAL;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.SPLIT_ON_CASE_CHANGE;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.SPLIT_ON_NUMERICS;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

class MySimpleAnalyzer extends Analyzer {

	private final Version matchVersion;
	private boolean enableLowerCase;
	private static int flags = PRESERVE_ORIGINAL | GENERATE_WORD_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS;//  GENERATE_NUMBER_PARTS | STEM_ENGLISH_POSSESSIVE;

	public MySimpleAnalyzer(Version matchVersion) {
		this(matchVersion, true);
	}
	public MySimpleAnalyzer(Version matchVersion, boolean enableLowerCase) {
		this.matchVersion = matchVersion;
		this.enableLowerCase = enableLowerCase;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		Tokenizer tokenizer = new StandardTokenizer(matchVersion, reader);
		/*TokenStream stream = new StandardFilter(matchVersion, tokenizer);
		stream = new LowerCaseFilter(matchVersion, stream);*/
		TokenStream stream = new WordDelimiterFilter(tokenizer, WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, flags, null);
		if (enableLowerCase) stream = new LowerCaseFilter(matchVersion, stream);
		stream = new CJKWidthFilter(stream);
		return new TokenStreamComponents(tokenizer, stream);
	}
}
