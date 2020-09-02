package com.intumit.solr.robot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ansj.domain.Term;
import org.apache.commons.lang.StringUtils;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class EnglishUtil {
	TokenizerModel tModel = null;
	POSModel pModel = null;
	Lemmatizer lemmatizer = null;
	
	void loadModelIfNull() {
		if (tModel == null) {
			try (InputStream modelIn = this.getClass().getResourceAsStream("/model/en-token.bin")) {
				tModel = new TokenizerModel(modelIn);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (pModel == null) {
			try (InputStream modelIn = this.getClass().getResourceAsStream("/model/en-pos-maxent.bin")) {
				pModel = new POSModel(modelIn);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (lemmatizer == null) {
			try {
				lemmatizer = new DictionaryLemmatizer(this.getClass().getResourceAsStream("/model/en-lemmatizer.dict"));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Term> tokenize(String sentence) {
		loadModelIfNull();
		
		List<Term> terms = new ArrayList<>();
		
		try {
			String tokens[] = null;
			String posTag[] = null;
			
			Tokenizer tokenizer = new TokenizerME(tModel);
			tokens = tokenizer.tokenize(sentence);
			System.out.println(Arrays.asList(tokens));
			
			for (int i=0; i < tokens.length; i++) {
				if (rewriteMap.containsKey(tokens[i])) {
					tokens[i] = rewriteMap.get(tokens[i]);
				}
			}
			System.out.println(Arrays.asList(tokens));

			POSTagger pos = new POSTaggerME(pModel);
			posTag = pos.tag(tokens);
			
			String[] lemmed = lemmatizer.lemmatize(tokens, posTag);
			System.out.println(Arrays.asList(lemmed));

			
			for (int i=0; i < tokens.length; i++) {
				String kw = lemmed[i].equals("O") ? tokens[i] : lemmed[i];
				
				Term t = new Term(kw, i, posTagMapping(posTag[i]), 1000);
				
				terms.add(t);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(Arrays.asList(terms));
		
		return terms;
	}

	public String posTagMapping(String openNlpTag) {
		if (StringUtils.equalsIgnoreCase(openNlpTag, "CD")) {
			return "m#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "VBP") || StringUtils.startsWithIgnoreCase(openNlpTag, "VBZ")) {
			return "r#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "VB")) {
			return "v#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "NN")) {
			return "n#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "RB")) {
			return "d#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "PRP")) {
			return "r#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "JJ")) {
			return "a#en#" + openNlpTag;
		}
		else if (StringUtils.startsWithIgnoreCase(openNlpTag, "CC") 
				|| StringUtils.startsWithIgnoreCase(openNlpTag, "IN")
				|| StringUtils.startsWithIgnoreCase(openNlpTag, "TO")) {
			return "c#en#" + openNlpTag;
		}
		else if (StringUtils.contains(openNlpTag, "UserDefined")) {
			return "#UserDefined#en#" + openNlpTag;
		}
		return "??#" + openNlpTag;
	}

	static Map<String, String> rewriteMap = new HashMap<>();
	static {
		rewriteMap.put("n't", "not");
		rewriteMap.put("'d", "would");
		rewriteMap.put("'ll", "will");
		rewriteMap.put("'s", "is");
		rewriteMap.put("'m", "am");
		rewriteMap.put("'ve", "have");
		rewriteMap.put("'re", " are");
	}
}
