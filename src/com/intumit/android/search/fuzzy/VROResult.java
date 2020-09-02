package com.intumit.android.search.fuzzy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class VROResult {
	private static final List<Piece> EMPTY_PIECES = Arrays.asList(new Piece[0]);
	HashMap<String, TreeSet<Piece>> resultMap = new HashMap<String, TreeSet<Piece>>();
	TreeMap<Piece, String> sortedByScoreCache = null; 
	
	protected boolean put(String answer, Piece piece) {
		if (resultMap.containsKey(answer)) {
			Set<Piece> pieces = resultMap.get(answer); 
			if (!pieces.contains(piece)) {
				pieces.add(piece);
			}
			else {
				return false;
			}
		}
		else {
			TreeSet<Piece> pieces = new TreeSet<Piece>();
			pieces.add(piece);
			resultMap.put(answer, pieces);
		}
		
		sortedByScoreCache = null;
		return true;
	}
	
	public boolean hasExactMatch() {
		if (sortedByScoreCache == null) {
			getSuggests();
		}
		
		if (sortedByScoreCache.size() > 0) {
			Map.Entry<Piece, String> entry = sortedByScoreCache.firstEntry();
			if (entry.getKey().getScore() > 99f) {
				return true;
			}
		}
		return false;
	}
	
	public String getExactMatch() {
		if (sortedByScoreCache == null) {
			getSuggests();
		}
		Map.Entry<Piece, String> entry = sortedByScoreCache.firstEntry();
		if (entry.getKey().getScore() > 99f) {
			return entry.getValue();
		}
		return null;
	}
	
	public List<String> getSuggests() {
		if (sortedByScoreCache == null) {
			TreeMap<Piece, String> sortedByScore = new TreeMap<Piece, String>();
			
			for (String key: resultMap.keySet()) {
				Piece highestScore = resultMap.get(key).first();
				sortedByScore.put(highestScore, key);
			}
			sortedByScoreCache = sortedByScore;
		}
		
		return new ArrayList<String>(sortedByScoreCache.values());
	}
	
	public List<Piece> getMatchedPieces(String suggest) {
		if (resultMap.containsKey(suggest))
			return new ArrayList<Piece>(resultMap.get(suggest));
		return EMPTY_PIECES;
	}
}
