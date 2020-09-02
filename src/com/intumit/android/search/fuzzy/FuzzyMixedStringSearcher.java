package com.intumit.android.search.fuzzy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.util.IOUtils;

import com.intumit.android.search.CJKSplitter;
import com.intumit.android.search.Constants;
import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.FuzzyQueryOption;
import com.intumit.android.search.fuzzy.VoiceRecognitionOptimizer.Token;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.util.WiSeEnv;

import gnu.trove.map.hash.TCustomHashMap;

public class FuzzyMixedStringSearcher {
	static final int DEFAULT_FIELD_CODE = 9999;
	static final int MAX_NGRAM = 10;
	static final String FN_AHO = "_a";
	static final String FN_CONFIG = "_c";
	static final String FN_MAPPING_CODE = "_im";
	
	File baseDir;
	AhoCorasick aho = null;
	Map<Integer, String> idMap;
	Map<String, Object> config;
	String name;
	FuzzyLevel fuzzyLevel = FuzzyLevel.DEFAULT;
	
	Scorer numberScorer = new Scorer();
	
	int maxLocalNGram = MAX_NGRAM;
	
	public FuzzyMixedStringSearcher(File baseDir) {
		super();
		this.baseDir = baseDir;
	}

	public void createDictionary(Collection<String> coll, String dictName) {
		createDictionary(new CollectionImportIterator(coll), dictName);
	}
	public void createDictionary(BufferedReader reader, String dictName) {
		createDictionary(new BufferedReaderImportIterator(reader), dictName);
	}
	private void createDictionary(ImportIterator iitr, String dictName) {
		
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		try {
			this.name = dictName;
			config = new HashMap<String, Object>();
			config.put("name", dictName);
			
			aho = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.en_US);
			idMap = new HashMap<Integer, String>();
			maxLocalNGram = 4;
			String line;
			int lineNo = 1;
			while ((line = StringUtils.trimToNull(iitr.next())) != null) {
				int currentLineNo = lineNo++;
				idMap.put(currentLineNo, line);
				line = line.toLowerCase();
				
				maxLocalNGram = Math.min(Math.max(maxLocalNGram, line.length()), MAX_NGRAM);
				aho.addString(line, new Output(currentLineNo, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
			}
			iitr.close();
			
			aho.prepare();
			aho.setIncludePathInScoreDoc(true);
			
			File ahoFile = new File(baseDir, FN_AHO);
			if (ahoFile.exists()) {
				ahoFile.delete();
			}
			File ahoMappingFile = new File(baseDir, FN_MAPPING_CODE);
			if (ahoMappingFile.exists()) {
				ahoMappingFile.delete();
			}
			File ahoConfigFile = new File(baseDir, FN_CONFIG);
			if (ahoConfigFile.exists()) {
				ahoConfigFile.delete();
			}
			/*
			long ahoSaveBegin = System.currentTimeMillis();

			FileOutputStream fo = null;
			ObjectOutputStream oo = null;
			BufferedOutputStream bos = null;
			try {
				fo = new FileOutputStream(ahoMappingFile);
				bos = new BufferedOutputStream(fo);
				oo = new ObjectOutputStream(bos);
				oo.writeObject(idMap);
				oo.flush();
				oo.reset();
				if (Constants.DEBUG) {
					System.out.println("Mapping code size: " + idMap.size() + " / file size: " + (ahoMappingFile.length() / 1024f) + "k");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				IOUtils.closeWhileHandlingException(oo);
				IOUtils.closeWhileHandlingException(bos);
				IOUtils.closeWhileHandlingException(fo);
			}
			
			fo = null;
			oo = null;
			bos = null;
			try {
				fo = new FileOutputStream(ahoConfigFile);
				bos = new BufferedOutputStream(fo);
				oo = new ObjectOutputStream(bos);
				oo.writeObject(config);
				oo.flush();
				oo.reset();
				if (Constants.DEBUG) {
					System.out.println("Config: " + config);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				IOUtils.closeWhileHandlingException(oo);
				IOUtils.closeWhileHandlingException(bos);
				IOUtils.closeWhileHandlingException(fo);
			}
			
			
			AhoCorasickFactory.saveTreeToFileBB(ahoFile, aho);
			aho = AhoCorasickFactory.newByteBufferAhoCorasick(HitHotLocale.en_US);
			AhoCorasickFactory.loadTreeFromFileIntoByteBufferAhoCorasick(ahoFile, (ByteBufferAhoCorasick)aho);
			aho.prepare();
			aho.setIncludePathInScoreDoc(true);
			aho.printTrie();
			long ahoSaveTC = System.currentTimeMillis() - ahoSaveBegin;

			//if (Constants.DEBUG) System.out.println("Aho TC:" + Statistics.AHOCORASIC_TOTAL_TC + "ms / Add TC: " + Statistics.AHOCORASIC_ADD_TC + "ms / TC of put: " + Statistics.AHOCORASIC_BUILDING_TC + "ms / Save TC: " + ahoSaveTC + "ms / Size: " + (ahoFile.length() / 1024) + "kb");
			 */
		} catch (Exception e) {
			if (Constants.DEBUG) {
				e.printStackTrace();
			}
		}
	}
	
	public FuzzyLevel getFuzzyLevel() {
		return fuzzyLevel;
	}

	public void setFuzzyLevel(FuzzyLevel fuzzyLevel) {
		this.fuzzyLevel = fuzzyLevel;
	}

	public String getName() {
		return name;
	}
	
	String join(List<Token> tokens) {
		StringBuilder sb = new StringBuilder();
		
		Token lastToken = null;
		
		for (Token t: tokens) {
			if (lastToken == null || t.isCJK) {
				sb.append(t.token);
			}
			else {
				if (!lastToken.isCJK && !t.isCJK)
					sb.append(' ');
				sb.append(t.token);
			}
			
			lastToken = t;
		}
		
		return sb.toString();
	}
	
	List<Piece> splitToNGram(String sentence, int ngramOfCJK, int ngramOfLatin, List<Piece> foundPiece) {
		List<Piece> l = new ArrayList<Piece>();
		try {
			CJKSplitter cjkSpt = new CJKSplitter(sentence, false);
			List<Token> tokens = new ArrayList<Token>();
			
			Token lastT = null;
			while (cjkSpt.next()) {
				Token t = new Token(cjkSpt.getToken(), cjkSpt.isCJK(), cjkSpt.getStartPos(), cjkSpt.getEndPos());
				
				if (lastT != null && !lastT.isCJK && !t.isCJK && lastT.startPos <= t.startPos && lastT.endPos >= t.endPos) {
					// System.out.println("[" + t + "] is not CJK and already included in last token [" + lastT + "], skip it.");
				}
				else {
					tokens.add(t);
				}
				lastT = t;
			}
			for (Piece p: foundPiece) {
				for (int i=p.startPos; i < p.endPos; i++) {
					
					// 如果有某個 piece 分數大於 1, 那麼他代表的那幾個字就不再用更小的 ngram 去 fuzzy search 了
					if (p.score > 1)
						tokens.get(i).masked = true;
				}
			}
			
			List<List<Token>> listOfTokens = new ArrayList<List<Token>>();
			Token lastToken = null;
			List<Token> currentTokens = new ArrayList<Token>();
			listOfTokens.add(currentTokens);
			
			int pos = -1;
			for (Token token: tokens) {
				pos++;
				
				if (currentTokens.size() == 0) {
				}
				else if (token.masked) {
					currentTokens = new ArrayList<Token>();
					listOfTokens.add(currentTokens);
				}
				
				if (token.masked) continue;
				currentTokens.add(token);
				
				if (token.isCJK) {
					if (currentTokens.size() >= ngramOfCJK) {
						String text = join(currentTokens.subList(currentTokens.size() - ngramOfCJK, currentTokens.size()));
						Piece p = new Piece(text, pos - ngramOfCJK + 1, pos + 1);
						l.add(p);
					}
				}
				else {
					if (currentTokens.size() >= ngramOfLatin) {
						String text = join(currentTokens.subList(currentTokens.size() - ngramOfLatin, currentTokens.size()));
						Piece p = new Piece(text, pos - ngramOfLatin + 1, pos + 1);
						l.add(p);
					}
				}
				lastToken = token;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return l;
	}
	
	public static void main(String[] args) {

		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		int[] testCasesNGram = new int[] {
			2,
			5,
			4,
		};
		
		String[] testCases = new String[] {
			"GTX 1080i",
			"固態硬碟ssd",
			"固態硬碟ssd",
			};
		
		for (int i=0; i < testCasesNGram.length; i++) {
			int tcn = testCasesNGram[i];
			String tc = testCases[i];
			List<Piece> pieces = fuzzy.splitToNGram(tc, tcn, tcn, new ArrayList<>());
			System.out.println(pieces);
		}
	}

	public void loadDictionary() {
		if (baseDir.exists() && baseDir.isDirectory()) {
			File ahoFile = new File(baseDir, FN_AHO);
			File ahoMappingFile = new File(baseDir, FN_MAPPING_CODE);
			File ahoConfigFile = new File(baseDir, FN_CONFIG);

			if (ahoFile.exists()){ 
				FileInputStream fin = null; 
				BufferedInputStream bis = null;
				ObjectInputStream oin = null;
				try {
					long begin = System.currentTimeMillis();
					aho = AhoCorasickFactory.newByteBufferAhoCorasick(HitHotLocale.en_US);
					if (ahoFile.exists() && ahoMappingFile.exists()) {
						
						// Load Mapping Code
						fin = new FileInputStream(ahoMappingFile);
						bis = new BufferedInputStream(fin);
						oin =new ObjectInputStream(bis);
						
						idMap = (Map<Integer, String>) oin.readObject();
						
						// Load AhoCorasick
						AhoCorasickFactory.loadTreeFromFileIntoByteBufferAhoCorasick(ahoFile, (ByteBufferAhoCorasick)aho);
						
						if (Constants.DEBUG) {
							System.out.println("Aho index load TC (" + (ahoFile.length() / 1024) + "kb): " + (System.currentTimeMillis() - begin) + "ms");
							System.out.println(AhoCorasickFactory.statistics(aho).toString());
						}
					}
				}
				catch (Exception e) {
					if (Constants.DEBUG) e.printStackTrace();
				} 
				finally {
					IOUtils.closeWhileHandlingException(oin);
					IOUtils.closeWhileHandlingException(bis);
					IOUtils.closeWhileHandlingException(fin);
				}
				
				fin = null; 
				bis = null;
				oin = null;
				try {
					if (ahoFile.exists() && ahoConfigFile.exists()) {
						// Load Mapping Code
						fin = new FileInputStream(ahoConfigFile);
						bis = new BufferedInputStream(fin);
						oin =new ObjectInputStream(bis);
						
						config = (Map<String, Object>) oin.readObject();
						this.name = (String)config.get("name");
						
						if (Constants.DEBUG) {
							System.out.println("Load config:" + config);
						}
					}
				}
				catch (Exception e) {
					if (Constants.DEBUG) e.printStackTrace();
				} 
				finally {
					IOUtils.closeWhileHandlingException(oin);
					IOUtils.closeWhileHandlingException(bis);
					IOUtils.closeWhileHandlingException(fin);
				}
			}
			else {
				aho = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.zh_TW);
			}
		}
		else {
			aho = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.zh_TW);
		}
		aho.setIncludePathInScoreDoc(true);
	}

	public VROResult suggest(String sentence) {
		VROResult result = new VROResult();
		
		if (StringUtils.length(sentence) < 2) 
			return result;

		stringSuggest(result, sentence, false);
		
		return result;
	}
	
	public VROResult search(String sentence) {
		return search(sentence, 10);
	}

	public VROResult search(String sentence, int limit) {
		VROResult result = new VROResult();
		
		if (StringUtils.length(sentence) < 2) 
			return result;
		
		Set<String> set = new HashSet<String>();
		set.addAll(Arrays.asList(StringUtils.split(sentence)));

		List<Piece> foundPiece = new ArrayList<Piece>();
		//aho.printTrie();
		
		for (int ngram = maxLocalNGram; ngram >= 2; ngram--) {
			List<Piece> pieces = splitToNGram(sentence, ngram, ngram, foundPiece);
			//System.out.println("NGRAM:" + ngram + " => PIECES:" + pieces.size());
			//System.out.println(pieces);
			int distance = 2;
			
			if (fuzzyLevel == FuzzyLevel.DEFAULT) {
				if (ngram >= 5) {
					distance = 3;
				}
				else if (ngram <= 2) {
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.HIGH) {
				if (ngram >= 4) {
					distance = 3;
				}
				else if (ngram <= 2) {
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.LOW) {
				if (ngram <= 2) {
					distance = 0;
				}
				else if (ngram <= 3) {
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
				distance = 0;
			}
    		
    		try {
    			for (Piece piece: pieces) {
    				DocSet thisSet = null;
    				FuzzyQueryOption option = new FuzzyQueryOption(distance, false, false, limit, false, false, Output.NOT_SYNONYM);
    				thisSet = aho.fuzzySearch(piece.getText().toCharArray(), option);
    
    				DocSetIterator itr = thisSet.iterator();
    				ScoredDoc doc = null;
    				
    				while ((doc = itr.nextDoc()) != null) {
    					String answer = idMap.get(doc.doc);
    			
    					DetailScoredDoc dsc = (DetailScoredDoc)doc;
    					//System.out.println(answer + ":" + doc.getScore());
    					String matchedBy = "";
    					for (char[] path: dsc.getPaths()) {
    						matchedBy += new String(path) + ", ";
    					}
						
    					Piece cloned = new Piece(matchedBy, piece.startPos, piece.endPos);

						if (StringUtils.equalsIgnoreCase(answer, piece.text)) {
							cloned.setScore(100f);
						}
						else {
							cloned.setScore(dsc.getScore());
						}

						result.put(answer, cloned);
    				}
    			}
    		} catch (Exception ignore) {
    			// should not be here
    		}
		}
		
//		result.resultMap.forEach((k,v) -> {
//			System.out.println("Answer[" + k + "] => " + v);
//		});
		return result;
	}
	
	void stringSuggest(VROResult vr, String sentence, boolean debug) {
		
		if (Constants.DEBUG) {
			System.out.println("Fuzzy search string: [" + sentence + "]");
		}

		int distance = 2;
		int sentenceLength = sentence.length();
		
		if (fuzzyLevel == FuzzyLevel.DEFAULT) {
			if (sentenceLength >= 5) {
				distance = 3;
			}
			else if (sentenceLength <= 2) {
				distance = 1;
			}
		}
		else if (fuzzyLevel == FuzzyLevel.HIGH) {
			if (sentenceLength >= 4) {
				distance = 3;
			}
			else if (sentenceLength <= 2) {
				distance = 1;
			}
		}
		else if (fuzzyLevel == FuzzyLevel.LOW) {
			if (sentenceLength <= 2) {
				distance = 0;
			}
			else if (sentenceLength <= 3) {
				distance = 1;
			}
		}
		else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
			distance = 0;
		}
		DocSet result = aho.fuzzySearch(sentence.toCharArray(), new FuzzyQueryOption(distance, true, false, -1, false, false, Output.NOT_SYNONYM));
		
		if (result == null || result.size() == 0) {
			if (Constants.DEBUG)
				System.out.println("No result for " + sentence);
		}
		else if (result.size() == 1) {
			DocSetIterator itr = result.iterator();
			DetailScoredDoc docWP = (DetailScoredDoc) itr
					.nextDoc();

			Map<Integer, TCustomHashMap<char[], Byte>> pathAndHP = docWP.getPathAndHighlightPoints();

			String answer = new String(docWP.getPaths().iterator().next());
			vr.put(answer, new Piece(sentence, 0, sentence.length()));
		} else {
			if (Constants.DEBUG)
				System.out.println(" --> Got ambigous results: " + result.size() + " entries.");
			TreeSet<ScoredDoc> sortedIds = new TreeSet<ScoredDoc>(sortByScore);

			if (result != null) {
				ScoredDoc doc = null;
				DocSetIterator docItr = result.iterator();

				while ((doc = docItr.nextDoc()) != null) {
					sortedIds.add(doc);
				}
			}

			DetailScoredDoc docWP = (DetailScoredDoc) sortedIds.first();
			
			String answer = new String(docWP.getPaths().iterator().next());
			vr.put(answer, new Piece(sentence, 0, sentence.length()));
		}
	}
	
	static Comparator sortByScore = new Comparator() {
		@Override
		public int compare(Object lhs, Object rhs) {
			ScoredDoc f1 = (ScoredDoc)lhs;
			ScoredDoc f2 = (ScoredDoc)rhs;
			return f1.score > f2.score ? -1 : 1;
		}};
}
