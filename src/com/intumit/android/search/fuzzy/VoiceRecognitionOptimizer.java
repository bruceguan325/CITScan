package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TCustomHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.util.IOUtils;

import com.intumit.android.search.CJKSplitter;
import com.intumit.android.search.Constants;
import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.FuzzyQueryOption;
import com.intumit.android.search.util.ChineseHelper;
import com.intumit.android.search.util.Statistics;
import com.intumit.hithot.HitHotLocale;

public class VoiceRecognitionOptimizer {
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
	MODE mode;
	FuzzyLevel fuzzyLevel = FuzzyLevel.DEFAULT;
	PhoneticSimilarity phSim = PhoneticSimilarity.NORMAL;
	
	VoiceScorer defaultScorer = new VoiceScorer(new DefaultPhoneticMap());
	VoiceScorer strictScorer = new VoiceScorer(new StrictPhoneticMap());
	Scorer numberScorer = new Scorer();
	
	int maxLocalNGram = MAX_NGRAM;
	
	public VoiceRecognitionOptimizer(File baseDir) {
		super();
		this.baseDir = baseDir;
	}

	public void createDictionary(Collection<String> coll, String dictName, MODE mode) {
		createDictionary(new CollectionImportIterator(coll), dictName, mode);
	}
	public void createDictionary(BufferedReader reader, String dictName, MODE mode) {
		createDictionary(new BufferedReaderImportIterator(reader), dictName, mode);
	}
	private void createDictionary(ImportIterator iitr, String dictName, MODE mode) {
		
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		try {
			this.mode = mode;
			this.name = dictName;
			config = new HashMap<String, Object>();
			config.put("name", dictName);
			config.put("mode", mode);
			
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
				
				if (mode == MODE.PINYIN) {
					Set<String> set = ChineseHelper.getPinyin(line);
					
					for (String pinyin: set) {
						aho.addString(pinyin, new Output(currentLineNo, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
					}
				}
				else if (mode == MODE.MPS2) {
					Set<String> set = ChineseHelper.getMPS2(line, 1000);
					//System.out.println(line + ":" + set);

					for (String pinyin: set) {
						aho.addString(pinyin, new Output(currentLineNo, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
					}
				}
				else if (mode == MODE.MPS2_NO_TONE) {
					Set<String> set = ChineseHelper.getNoToneMPS2(line, 1000);
					//System.out.println(line + ":" + set);

					for (String mps: set) {
						aho.addString(mps, new Output(currentLineNo, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
					}
				}
				else {
					aho.addString(line, new Output(currentLineNo, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
				}
			}
			iitr.close();
			
			aho.prepare();
			aho.setIncludePathInScoreDoc(true);
			if (mode == MODE.PINYIN || mode == MODE.MPS2) aho.setEnablePhoneticMode(true); 
			
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
			if (mode == MODE.PINYIN || mode == MODE.MPS2) aho.setEnablePhoneticMode(true); 
			//aho.printTrie();
			long ahoSaveTC = System.currentTimeMillis() - ahoSaveBegin;

			if (Constants.DEBUG)
				System.out.println("Aho TC:" + Statistics.AHOCORASIC_TOTAL_TC + "ms / Add TC: " + Statistics.AHOCORASIC_ADD_TC + "ms / TC of put: " + Statistics.AHOCORASIC_BUILDING_TC + "ms / Save TC: " + ahoSaveTC + "ms / Size: " + (ahoFile.length() / 1024) + "kb");
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
	
	public void setPhoneticSimilarity(PhoneticSimilarity phSim) {
		this.phSim = phSim;
	}

	public String getName() {
		return name;
	}

	public MODE getMode() {
		return mode;
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
						this.mode = (MODE)config.get("mode");
						
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
				aho = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.en_US);
			}
		}
		else {
			aho = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.en_US);
		}
		aho.setIncludePathInScoreDoc(true);
		if (mode == MODE.PINYIN || mode == MODE.MPS2 || mode == MODE.MPS2_NO_TONE) aho.setEnablePhoneticMode(true); 
	}

	public VROResult suggest(String sentence) {
		VROResult result = new VROResult();
		
		if (StringUtils.length(sentence) < 2) 
			return result;
		
		if (mode == MODE.PINYIN)
			pinyinSuggest(result, sentence, false);
		else if (mode == MODE.MPS2) {
			mps2Suggest(result, sentence, false, false);
		}
		else if (mode == MODE.MPS2_NO_TONE) {
			mps2Suggest(result, sentence, true, false);
		}
		else if (mode == MODE.NUMBER) {
			numberSuggest(result, sentence, false);
		}
		
		return result;
	}
	
	public VROResult search(String keyword) {
		return search(keyword, 10);
	}

	public VROResult search(String keyword, int limit) {
		VROResult result = new VROResult();
		
		if (StringUtils.length(keyword) < 2) 
			return result;
		
		Set<String> set = null;
		if (mode == MODE.PINYIN)
			set = ChineseHelper.getPinyin(keyword);
		else if (mode == MODE.MPS2) {
			set = ChineseHelper.getMPS2(keyword, 1000);
		}
		else if (mode == MODE.MPS2_NO_TONE) {
			set = ChineseHelper.getNoToneMPS2(keyword, 1000);
		}
		else if (mode == MODE.NUMBER) {
			String converted = tryConvertNumber(keyword, numberFuzzy);
			List<String> expanded = expandTheConvertedNumber(converted);
			set = new HashSet<String>(expanded);
		}

		int distance = 2;
		int ngram = keyword.length();
		
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
			if (ngram <= 3) {
				distance = 1;
			}
		}
		else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
			distance = 0;
		}
		
		DocSet finalSet = null;
		
		try {
			for (String pinyin: set) {
				DocSet thisSet = null;
				
				FuzzyQueryOption option = new FuzzyQueryOption(distance, true, limit, true, false, Output.NOT_SYNONYM);
				thisSet = aho.fuzzySearch(pinyin.toCharArray(), option);
				
				if (finalSet == null) {
					finalSet = (DocSet)thisSet.clone();
				}
				else {
					finalSet.orOperation(thisSet);
				}
			}
		} catch (CloneNotSupportedException ignore) {
			// should not be here
		}
		
		
		DocSetIterator itr = finalSet.iterator();
		ScoredDoc doc = null;
		
		while ((doc = itr.nextDoc()) != null) {
			String answer = idMap.get(doc.doc);
	
			DetailScoredDoc dsc = (DetailScoredDoc)doc;
			String matchedBy = "";
			for (char[] path: dsc.getPaths()) {
				matchedBy += new String(path) + ", ";
			}
			matchedBy = StringUtils.substringBeforeLast(matchedBy, ", ");
			Piece piece = new Piece(answer, 0, keyword.length());
			piece.setMatchedBy(answer);
			piece.setPinyin(matchedBy);
			piece.setScore(dsc.getScore());
			
			result.put(keyword, piece);
		}
		
		return result;
	}
	
	void pinyinSuggest(VROResult vr, String sentence, boolean debug) {
		List<Piece> foundPiece = new ArrayList<Piece>();
		
		for (int ngram = MAX_NGRAM; ngram >= 2; ngram--) {
			List<Piece> pieces = splitToNGram(sentence, ngram, ngram - 1, foundPiece);
			
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
				if (ngram <= 3) {
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
				distance = 0;
			}
			
			//System.out.println("" + ngram + "gram:" + pieces + ", distance:" + distance);
			
			for (Piece piece: pieces) {
				Set<String> set = ChineseHelper.getPinyin(piece.text);
				boolean foundAtThisPiece = false;
				
				for (String pinyin: set) {
					DocSet result = null;
					

					FuzzyQueryOption option = new FuzzyQueryOption(distance, false, -1, false, false, Output.NOT_SYNONYM);
					result = aho.fuzzySearch(pinyin.toCharArray(), option);
					List<String> matched = debug ? new ArrayList<String>() : null;
					
					if (result.size() == 1) {
						DocSetIterator itr = result.iterator();
						ScoredDoc doc = itr.nextDoc();
						String answer = idMap.get(doc.doc);

						DetailScoredDoc dsc = (DetailScoredDoc)doc;
						String matchedBy = "";
						for (char[] path: dsc.getPaths()) {
							matchedBy += new String(path) + ", ";
						}
						matchedBy = StringUtils.substringBeforeLast(matchedBy, ", ");
						piece.setMatchedBy(matchedBy);
						piece.setPinyin(pinyin);
						
						if (vr.put(answer, piece)) {
							foundAtThisPiece = true;
						}
						
					} else if (result.size() == 0) {
					} else {
						TreeSet<ScoredDoc> sortedIds = new TreeSet<ScoredDoc>(sortByScore);

						if (result != null) {
							ScoredDoc doc = null;
							DocSetIterator docItr = result.iterator();

							while ((doc = docItr.nextDoc()) != null) {
								sortedIds.add(doc);
							}
						}


						float highestScore = sortedIds.first().getScore();
						
						for (ScoredDoc doc: sortedIds) {
							if (doc.getScore() != highestScore)  {
								break;
							}
							Piece thisPiece = new Piece(piece.text, piece.startPos, piece.endPos);
							String answer = idMap.get(doc.doc);
	
							DetailScoredDoc dsc = (DetailScoredDoc)doc;
							String matchedBy = "";
							for (char[] path: dsc.getPaths()) {
								matchedBy += new String(path) + ", ";
							}
							matchedBy = StringUtils.substringBeforeLast(matchedBy, ", ");
							thisPiece.setMatchedBy(matchedBy);
							thisPiece.setPinyin(pinyin);
							
							if (vr.put(answer, thisPiece)) {
								foundAtThisPiece = true;
							}
						}
					}
				}
				
				if (foundAtThisPiece) {
					foundPiece.add(piece);
				}
			}
		}
	}
	
	void mps2Suggest(VROResult vr, String sentence, boolean useNoToneMPS2, boolean debug) {
		List<Piece> foundPiece = new ArrayList<Piece>();
		//aho.printTrie();
		
		for (int ngram = MAX_NGRAM; ngram >= 2; ngram--) {
			List<Piece> pieces = splitToNGram(sentence, ngram, ngram - 1, foundPiece);
			if (ngram > 2) {
				aho.setPhoneticMap(phSim.getDefaultMap());
			}
			else {
				aho.setPhoneticMap(phSim.getBigramMap());
			}
			aho.setScorer(defaultScorer);
			
			int distance = 2;
			float scoreThreshold = 0.8f;
			
			if (fuzzyLevel == FuzzyLevel.DEFAULT) {
				scoreThreshold = 0.8f;
				if (ngram >= 5) {
					aho.setScorer(defaultScorer);
					distance = 3;
					
				}
				else if (ngram <= 2) {
					aho.setScorer(strictScorer);
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.HIGH) {
				scoreThreshold = 0.4f;
				if (ngram >= 3) {
					aho.setScorer(defaultScorer);
					distance = 3;
				}
				else if (ngram <= 2) {
					aho.setScorer(defaultScorer);
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.LOW) {
				scoreThreshold = 1.2f;
				aho.setScorer(strictScorer);
				if (ngram <= 3) {
					distance = 1;
				}
			}
			else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
				scoreThreshold = 0;
				aho.setScorer(strictScorer);
				distance = 0;
			}
			
			for (Piece piece: pieces) {
				Set<String> set = useNoToneMPS2 
									? ChineseHelper.getNoToneMPS2(piece.text, 1000)
									: ChineseHelper.getMPS2(piece.text, 1000);
				//if (ngram == 2) System.out.println("" + ngram + "gram:" + piece + ", distance:" + distance + ", [" + set + "]");
				boolean foundAtThisPiece = false;
				
				for (String pinyin: set) {
					DocSet result = null;
					
					FuzzyQueryOption option = new FuzzyQueryOption(distance, false, -1, false, false, Output.NOT_SYNONYM);
					result = aho.fuzzySearch(pinyin.toCharArray(), option);
					//System.out.println("Result of [" + pinyin + "]:" + result.size());
					List<String> matched = debug ? new ArrayList<String>() : null;
					
					if (result.size() == 1) {
						DocSetIterator itr = result.iterator();
						ScoredDoc doc = itr.nextDoc();
						String answer = idMap.get(doc.doc);
						
						DetailScoredDoc dsc = (DetailScoredDoc)doc;
						String matchedBy = "";
						for (char[] path: dsc.getPaths()) {
							matchedBy += new String(path) + ", ";
						}
						matchedBy = StringUtils.substringBeforeLast(matchedBy, ", ");
						piece.setMatchedBy(matchedBy);
						piece.setPinyin(pinyin);

						if (StringUtils.equalsIgnoreCase(answer, piece.text)) {
							piece.setScore(100f);
						}
						else {
							piece.setScore(dsc.getScore());
						}
						
						if (answer.length() != ngram) {
							piece.setScore(piece.getScore() * 0.75f);
						}
						
						if (piece.getScore() >= scoreThreshold && vr.put(answer, piece)) {
							foundAtThisPiece = true;
						}
					} else if (result.size() == 0) {
					} else {
						TreeSet<ScoredDoc> sortedIds = new TreeSet<ScoredDoc>(sortByScore);

						if (result != null) {
							ScoredDoc doc = null;
							DocSetIterator docItr = result.iterator();

							while ((doc = docItr.nextDoc()) != null) {
								sortedIds.add(doc);
							}
						}
						
						float highestScore = sortedIds.first().getScore();
						
						for (ScoredDoc doc: sortedIds) {
							if (doc.getScore() != highestScore && (ngram < 3 || doc.getScore() < 0.2)) {
								String answer = idMap.get(doc.doc);
								
								if (!StringUtils.equalsIgnoreCase(answer, piece.text)) {
									continue;
								}
								else {
									System.out.println("*******************" + answer);
								}
							}
							
							Piece thisPiece = new Piece(piece.text, piece.startPos, piece.endPos);
							String answer = idMap.get(doc.doc);
							
							DetailScoredDoc dsc = (DetailScoredDoc)doc;
							String matchedBy = "";
							for (char[] path: dsc.getPaths()) {
								matchedBy += new String(path) + ", ";
							}
							matchedBy = StringUtils.substringBeforeLast(matchedBy, ", ");
							thisPiece.setMatchedBy(matchedBy);
							thisPiece.setPinyin(pinyin);

							if (StringUtils.equalsIgnoreCase(answer, piece.text)) {
								thisPiece.setScore(100f);
							}
							else {
								thisPiece.setScore(dsc.getScore());
							}
							
							if (answer.length() != ngram) {
								thisPiece.setScore(thisPiece.getScore() * 0.75f);
							}
							
							if (thisPiece.getScore() >= scoreThreshold && vr.put(answer, thisPiece)) {
								foundAtThisPiece = true;
							}
						}
					}
					
					if (debug) {
						System.out.println("Pinyin of " + piece + ": [" + pinyin + "]: " +  matched + "(" + result.size() + ")");
					}
				}
				
				
				if (foundAtThisPiece) {
					foundPiece.add(piece);
				}
			}
		}
	}
	
	
	void numberSuggest(VROResult vr, String sentence, boolean debug) {
		String converted = tryConvertNumber(sentence, numberFuzzy);
		List<String> expanded = expandTheConvertedNumber(converted);
		
		if (Constants.DEBUG) {
			System.out.println("Fuzzy search phone number: " 
				+ "[" + sentence + " / " + converted + " / " + expanded + "]");
		}

		DocSet result = null;
		if (expanded.size() > 0) {
			int tries = 0;
			int minDistance = 2;
			if (fuzzyLevel == FuzzyLevel.HIGH) {
				minDistance = 3;
			}
			else if (fuzzyLevel == FuzzyLevel.LOW) {
				minDistance = 1;
			}
			else if (fuzzyLevel == FuzzyLevel.NOFUZZY) {
				minDistance = 0;
			}
			aho.setPhoneticMap(phSim.getDefaultMap());

			while (result == null && tries < 3) {
				for (String s : expanded) {
					int d = (int) Math.min(minDistance, tries + (s.indexOf("?") == -1 ? 0 : 1));

					FuzzyQueryOption option = new FuzzyQueryOption(d, true, -1, false, false, Output.NOT_SYNONYM);
					DocSet tmpRes = aho.fuzzySearch(s.toCharArray(), option);

					if (tmpRes.size() > 0) {
						if (Constants.DEBUG)
							System.out.println("Use this fuzzy: " + s);
						
						result = tmpRes;
						break;
					}
				}

				tries++;
			}
		} else {
			result = aho.fuzzySearch(converted.toCharArray(), FuzzyQueryOption.DEFAULT_D2_P);
		}
		
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
	

	static String IMPOSSIBLE_TOKEN = "\uFFFC\uFFFC";
	
	static class Token {
		String token;
		boolean isCJK;
		int startPos;
		int endPos;
		boolean masked = false;
		
		public Token(String token, boolean isCJK, int startPos, int endPos) {
			super();
			this.token = token;
			this.isCJK = isCJK;
			this.startPos = startPos;
			this.endPos = endPos;
		}

		@Override
		public String toString() {
			return "Token [" + (token != null ? "token=" + token + ", " : "") + "isCJK=" + isCJK + ", startPos=" + startPos + ", endPos=" + endPos + ", masked="
					+ masked + "]";
		}
		
	}
	
	List<Piece> splitToNGram(String sentence, int ngramOfCJK, int ngramOfLatin, List<Piece> foundPiece) {
		
		//sentence = sentence.replaceAll("[我的是]", IMPOSSIBLE_TOKEN);
		//sentence = sentence.replaceAll("到底", IMPOSSIBLE_TOKEN);
		//sentence = sentence.replaceAll("要?不要", IMPOSSIBLE_TOKEN);
		//sentence = sentence.replaceAll(" ", IMPOSSIBLE_TOKEN);
		
		List<Piece> l = new ArrayList<Piece>();
		try {
			CJKSplitter cjkSpt = new CJKSplitter(sentence, false);
			List<Token> tokens = new ArrayList<Token>();
			
			while (cjkSpt.next()) {
				Token t = new Token(cjkSpt.getToken(), cjkSpt.isCJK(), cjkSpt.getStartPos(), cjkSpt.getEndPos());
				//System.out.println(t);
				tokens.add(t);
			}
			for (Piece p: foundPiece) {
				for (int i=p.startPos; i < p.endPos; i++) {
					
					// 如果有某個 piece 分數大於 1, 那麼他代表的那幾個字就不再用更小的 ngram 去 fuzzy search 了
					if (p.score > 1)
						tokens.get(i).masked = true;
				}
			}
			
			List<List<String>> listOfTokens = new ArrayList<List<String>>();
			Token lastToken = null;
			List<String> currentTokens = new ArrayList<String>();
			listOfTokens.add(currentTokens);
			
			int pos = -1;
			for (Token token: tokens) {
				pos++;
				
				if (currentTokens.size() == 0) {
				}
				else {
					if (token.masked) {
						currentTokens = new ArrayList<String>();
						listOfTokens.add(currentTokens);
					}
					else if (lastToken != null) {
						if ( (lastToken.isCJK != token.isCJK) 
								|| (token.isCJK && (token.startPos != lastToken.endPos))
							) {
							currentTokens = new ArrayList<String>();
							listOfTokens.add(currentTokens);
						}
					}
					
				}
				
				if (token.masked) continue;
				currentTokens.add(token.token);
				
				if (token.isCJK) {
					if (currentTokens.size() >= ngramOfCJK) {
						String text = StringUtils.join(currentTokens.subList(currentTokens.size() - ngramOfCJK, currentTokens.size()), "");
						Piece p = new Piece(text, pos - ngramOfCJK + 1, pos + 1);
						l.add(p);
					}
				}
				else {
					if (currentTokens.size() >= ngramOfLatin) {
						String text = StringUtils.join(currentTokens.subList(currentTokens.size() - ngramOfLatin, currentTokens.size()), " ");
						Piece p = new Piece(text, pos - ngramOfLatin + 1, pos + 1);
						//Piece p = new Piece(token.token, pos, pos + 1);
						l.add(p);
					}
				}
				lastToken = token;
				//tokens.add(new Token(cjkSpt.getToken(), cjkSpt.isCJK(), cjkSpt.getStartPos(), cjkSpt.getEndPos()));
			}
			
/*			for (List<String> lt: listOfTokens) {
				for (int i=0; i+ngram <= lt.size(); i++) {
					String text = StringUtils.join(lt.subList(i, i+ngram), "");
					Piece p = new Piece(text, i, i+ngram);
					l.add(p);
				}
			}
*/		} catch (IOException e) {
			e.printStackTrace();
		}
		return l;
	}

	static AhoCorasick numberFuzzy = AhoCorasickFactory.newStateAhoCorasick(HitHotLocale.en_US);

	static String numberMap = "零壹貳参肆伍陸柒捌玖";
	
	static {
		for (int i=0; i < numberMap.length(); i++) {
			String str = numberMap.substring(i, i+1);
			Set<String> set = ChineseHelper.getPinyin(str);
			//System.out.println("Pinyin for [" + i + "]:" + set);
			
			for (String pinyin: set) {
				numberFuzzy.addString(pinyin, new Output(i, DEFAULT_FIELD_CODE, Output.NOT_SYNONYM));
			}
		}
		
		numberFuzzy.prepare();
	}

	static Comparator sortByScore = new Comparator() {
		@Override
		public int compare(Object lhs, Object rhs) {
			ScoredDoc f1 = (ScoredDoc)lhs;
			ScoredDoc f2 = (ScoredDoc)rhs;
			return f1.score > f2.score ? -1 : 1;
		}};
		
	String tryConvertNumber(String org, AhoCorasick numberFuzzy) {
		StringBuilder result = new StringBuilder();
		
		if (Constants.DEBUG)
			System.out.print("Start convert:" + org);
		
		for (int i=0; i < org.length(); i++) {
			String testChar = org.substring(i, i+1);
			if (testChar.matches("[0-9]")) {
				result.append(testChar);
				continue;
			}
			Set<String> pinyins = ChineseHelper.getPinyin(testChar);
			
			StringBuilder pinyinRegex = new StringBuilder();
			
			for (String py: pinyins) {
				int d = py.length();
				DocSet ds = numberFuzzy.fuzzySearch(py.toCharArray(), FuzzyQueryOption.DEFAULT_D1_NP);
				
				if (ds.size() == 0) {
					continue;
				}
				DocSetIterator itr = ds.iterator();
				ScoredDoc doc = null;
				while ( (doc = itr.nextDoc()) != null) {
					if (pinyinRegex.indexOf("" + doc.doc) != -1)
						continue;
					pinyinRegex.append(doc.doc);
				}
			}
			
			if (pinyinRegex.length() == 0) {
				result.append("");
			}
			else if (pinyinRegex.length() == 1) {
				result.append(pinyinRegex);
			}
			else {
				result.append("[" + pinyinRegex.toString() + "]");
			}
		}
		
		if (Constants.DEBUG)
			System.out.println(" ==> " + result.toString());
		return result.toString();
	}
	
	List<String> expandTheConvertedNumber(String converted) {
		List<String> l = new ArrayList<String>();
		String thePattern = ("\\[([0-9]+?)\\]");
		Pattern p = Pattern.compile(thePattern);
		Matcher m = p.matcher(converted);
		
		while (m.find()) {
			String allStr = m.group();
			String allPossible = m.group(1);
			
			for (int i=0; i < allPossible.length(); i++) {
				String num = allPossible.substring(i, i+1);
				String tmp = StringUtils.replace(converted, allStr, num);
				
				if (tmp.indexOf("[") != -1) 
					expandTheConvertedNumberR(l, tmp);
				else if (!l.contains(tmp))
					l.add(tmp);
			}
		}
		
		return l;
	}
	
	void expandTheConvertedNumberR(List<String> l, String converted) {
		String thePattern = ("\\[([0-9]+?)\\]");
		Pattern p = Pattern.compile(thePattern);
		Matcher m = p.matcher(converted);
		
		while (m.find()) {
			String allStr = m.group();
			String allPossible = m.group(1);
			
			for (int i=0; i < allPossible.length(); i++) {
				String num = allPossible.substring(i, i+1);
				String tmp = StringUtils.replace(converted, allStr, num);
				
				if (tmp.matches(thePattern)) 
					expandTheConvertedNumberR(l, tmp);
				else if (!l.contains(tmp))
					l.add(tmp);
			}
		}
		
	}
}
