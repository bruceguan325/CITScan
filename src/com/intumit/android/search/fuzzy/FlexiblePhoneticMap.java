package com.intumit.android.search.fuzzy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FlexiblePhoneticMap implements PhoneticMap {

	static Map<Character, Collection<Character>> sameMap = new HashMap<Character, Collection<Character>>();
	static Set<Character> vowelSet = new HashSet<Character>();
	static Set<Character> consonantSet = new HashSet<Character>();
	
	static {
		sameMap.put('ㄥ', Arrays.asList(new Character [] {'ㄣ'}));
		sameMap.put('ㄣ', Arrays.asList(new Character [] {'ㄥ'}));
		sameMap.put('ㄧ', Arrays.asList(new Character [] {'ㄩ'}));
		sameMap.put('ㄩ', Arrays.asList(new Character [] {'ㄧ'}));
		sameMap.put('ㄋ', Arrays.asList(new Character [] {'ㄌ'}));
		sameMap.put('ㄈ', Arrays.asList(new Character [] {'ㄏ'}));
		sameMap.put('ㄏ', Arrays.asList(new Character [] {'ㄈ'}));
		sameMap.put('ㄌ', Arrays.asList(new Character [] {'ㄋ', 'ㄖ'}));
		sameMap.put('ㄖ', Arrays.asList(new Character [] {'ㄌ'}));
		sameMap.put('ㄓ', Arrays.asList(new Character [] {'ㄗ'}));
		sameMap.put('ㄗ', Arrays.asList(new Character [] {'ㄓ'}));
		
		vowelSet.addAll(Arrays.asList(new Character[] {'ㄚ','ㄛ','ㄜ','ㄝ','ㄞ','ㄟ','ㄠ','ㄡ','ㄢ','ㄣ','ㄤ','ㄥ','ㄦ','ㄧ','ㄨ','ㄩ','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z', }));
		consonantSet.addAll(Arrays.asList(new Character[] {'ㄅ','ㄆ','ㄇ','ㄉ','ㄊ','ㄋ','ㄍ','ㄎ','ㄏ','ㄐ','ㄑ','ㄒ','ㄗ','ㄘ','ㄙ','ㄓ','ㄔ','ㄕ','ㄖ','ㄈ','ㄌ',}));
		
	}
	
	public boolean isPhoneticSimilar(Character c1, Character c2) {
		if (vowelSet.contains(c1) && vowelSet.contains(c2)
			|| consonantSet.contains(c1) && consonantSet.contains(c2)) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isPhoneticAllowDeletion(Character c) {
		return true;
	}

	@Override
	public boolean isPhoneticAllowInsertion(Character c) {
		return true;
	}

	@Override
	public boolean isPhoneticTheSame(Character c1, Character c2) {
		if (sameMap.containsKey(c1)) {
			return sameMap.get(c1).contains(c2);
		}
		
		return false;
	}
}
