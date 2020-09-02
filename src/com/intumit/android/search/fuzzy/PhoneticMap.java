package com.intumit.android.search.fuzzy;

public interface PhoneticMap {
	public boolean isPhoneticTheSame(Character c1, Character c2);
	public boolean isPhoneticSimilar(Character c1, Character c2);
	public boolean isPhoneticAllowDeletion(Character c);
	public boolean isPhoneticAllowInsertion(Character c);
}
