package com.intumit.checkmarx;

public class SealObject<T> {

	private T item;
	
	public void seal(T item) {
		this.item = item;
	}
	
	public T get() {
		return item;
	}
}