package com.intumit.android.search.fuzzy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NumberPhoneticMap implements PhoneticMap {

	static Map<Character, Collection<Character>> map = new HashMap<Character, Collection<Character>>();
	static Set<Character> allowIns = new HashSet<Character>();
	static Set<Character> allowDel = new HashSet<Character>();
	
	static {
		map.put('a', Arrays.asList(new Character [] {'e', 'i', 'y'}));
		map.put('b', Arrays.asList(new Character [] {'f', 'p'}));
		map.put('c', Arrays.asList(new Character [] {'g', 'k'}));
		map.put('d', Arrays.asList(new Character [] {'j', 't'}));
		map.put('e', Arrays.asList(new Character [] {'a', 'i', 'y'}));
		map.put('f', Arrays.asList(new Character [] {'v'}));
		map.put('g', Arrays.asList(new Character [] {'c', 'k', 'l'}));
//		map.put('h', Arrays.asList(new Character [] {''}));
		map.put('i', Arrays.asList(new Character [] {'a', 'e', 'u', 'y'}));
		map.put('j', Arrays.asList(new Character [] {'d', 'g', 'q'}));
		map.put('k', Arrays.asList(new Character [] {'c'}));
		map.put('l', Arrays.asList(new Character [] {'r'}));
		map.put('m', Arrays.asList(new Character [] {'f', 'n'}));
		map.put('n', Arrays.asList(new Character [] {'l', 'm'}));
		map.put('o', Arrays.asList(new Character [] {'u'}));
		map.put('p', Arrays.asList(new Character [] {'b', 'f'}));
		map.put('q', Arrays.asList(new Character [] {'c', 'j', 'g', 'l'}));
		map.put('r', Arrays.asList(new Character [] {'l'}));
		map.put('s', Arrays.asList(new Character [] {'x', 'z'}));
		map.put('t', Arrays.asList(new Character [] {'d'}));
		map.put('u', Arrays.asList(new Character [] {'e', 'i', 'o', 'y'}));
		map.put('v', Arrays.asList(new Character [] {'f', 'u', 'w'}));
		map.put('w', Arrays.asList(new Character [] {'f', 'u', 'v'}));
		map.put('x', Arrays.asList(new Character [] {'c', 's', 'z'}));
		map.put('y', Arrays.asList(new Character [] {'i', 'e', 'j'}));
		map.put('z', Arrays.asList(new Character [] {'c', 's', 'x'}));
		
		map.put('ㄅ', Arrays.asList(new Character [] {'ㄆ'}));
		map.put('ㄆ', Arrays.asList(new Character [] {'ㄅ'}));
		map.put('ㄉ', Arrays.asList(new Character [] {'ㄊ'}));
		map.put('ㄊ', Arrays.asList(new Character [] {'ㄎ', 'ㄉ'}));
		map.put('ㄍ', Arrays.asList(new Character [] {'ㄌ', 'ㄎ'}));
		map.put('ㄎ', Arrays.asList(new Character [] {'ㄊ', 'ㄍ', 'ㄔ'}));
		map.put('ㄌ', Arrays.asList(new Character [] {'ㄋ', 'ㄖ', 'ㄍ'}));
		map.put('ㄋ', Arrays.asList(new Character [] {'ㄌ'}));
		map.put('ㄐ', Arrays.asList(new Character [] {'ㄑ', 'ㄉ'}));
		map.put('ㄑ', Arrays.asList(new Character [] {'ㄐ', 'ㄒ'}));
		map.put('ㄥ', Arrays.asList(new Character [] {'ㄣ', 'ㄢ'}));
		map.put('ㄣ', Arrays.asList(new Character [] {'ㄥ', 'ㄢ'}));
		map.put('ㄢ', Arrays.asList(new Character [] {'ㄤ', 'ㄠ', 'ㄥ'}));
		map.put('ㄤ', Arrays.asList(new Character [] {'ㄢ', 'ㄠ', 'ㄚ'}));
		map.put('ㄚ', Arrays.asList(new Character [] {'ㄠ', 'ㄤ'}));
		//map.put('ㄠ', Arrays.asList(new Character [] {'ㄢ', 'ㄤ'}));
		map.put('ㄛ', Arrays.asList(new Character [] {'ㄜ'}));
		map.put('ㄜ', Arrays.asList(new Character [] {'ㄛ'}));
		map.put('ㄓ', Arrays.asList(new Character [] {'ㄗ', 'ㄔ'}));
		map.put('ㄔ', Arrays.asList(new Character [] {'ㄘ', 'ㄓ', 'ㄎ'}));
		map.put('ㄕ', Arrays.asList(new Character [] {'ㄗ', 'ㄙ', 'ㄒ', 'ㄓ'}));
		map.put('ㄖ', Arrays.asList(new Character [] {'ㄌ', 'ㄋ'}));
		map.put('ㄗ', Arrays.asList(new Character [] {'ㄓ', 'ㄔ', 'ㄕ'}));
		map.put('ㄘ', Arrays.asList(new Character [] {'ㄔ', 'ㄓ'}));
		map.put('ㄙ', Arrays.asList(new Character [] {'ㄕ', 'ㄒ'}));
		map.put('ㄒ', Arrays.asList(new Character [] {'ㄑ'}));
		map.put('ㄩ', Arrays.asList(new Character [] {'ㄧ'}));
		map.put('ㄧ', Arrays.asList(new Character [] {'ㄩ'}));
		
		
		allowIns.addAll(Arrays.asList(new Character[] {'一', 'ㄣ'}));
		allowDel.addAll(Arrays.asList(new Character[] {'ㄢ', 'ㄣ', 'ㄠ'}));
	}
	
	public boolean isPhoneticSimilar(Character c1, Character c2) {
		if (map.containsKey(c1)) {
			return map.get(c1).contains(c2);
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
		return false;
	}
}
