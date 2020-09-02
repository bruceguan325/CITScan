package com.intumit.solr.util;

import groovy.lang.Script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Groovy Script Tool
 * @author herb
 *
 */
public class GroovyScriptTool  extends Script {

	@Override
	public Object run() {
		return null;
	}

	public Object  ls(){
		return  this.getBinding().getVariables().keySet();
	}
	
	public String toString(Object o){
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(o);
		return json;
	}
}
