package com.intumit.android.search.fuzzy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MostStrictPhoneticMap implements PhoneticMap {

	static Map<Character, Collection<Character>> map = new HashMap<Character, Collection<Character>>();
	static Map<Character, Collection<Character>> sameMap = new HashMap<Character, Collection<Character>>();
	static Set<Character> allowIns = new HashSet<Character>();
	static Set<Character> allowDel = new HashSet<Character>();
	
	static {
	}
	
	public boolean isPhoneticSimilar(Character c1, Character c2) {
		if (map.containsKey(c1)) {
			return map.get(c1).contains(c2);
		}
		
		return false;
	}

	@Override
	public boolean isPhoneticAllowDeletion(Character c) {
		return allowDel.contains(c);
	}

	@Override
	public boolean isPhoneticAllowInsertion(Character c) {
		return allowIns.contains(c);
	}

	@Override
	public boolean isPhoneticTheSame(Character c1, Character c2) {
		if (sameMap.containsKey(c1)) {
			return sameMap.get(c1).contains(c2);
		}
		
		return false;
	}
}
