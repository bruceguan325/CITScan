package com.intumit.solr.robot.qaplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class TypeAndTagsMatcher {
	
	Map<String, Set<String>> typeTagsMap = new HashMap<>();
	
	TypeAndTagsMatcher(String[][] postTags){
		typeTagsMap = toTypeTagsMap(postTags);
		try {
			System.out.println("TypeAndTagsMatcher " + new JSONObject(typeTagsMap).toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	boolean all(String type, String... tags){
		boolean match = false;
		Set<String> typeTags = typeTagsMap.get(type);
		if(typeTags != null){
			match = typeTags.containsAll(Arrays.asList(tags));
		}
		return match;
	}
	
	boolean any(String type, String... tags){
		boolean match = false;
		Set<String> typeTags = typeTagsMap.get(type);
		if(typeTags != null){
			for(String t:tags){
				if(typeTags.contains(t)){
					match = true;
					break;
				}
			}
		}
		return match;
	}
	
	static Map<String, Set<String>> toTypeTagsMap(String[][] postTags) {
		Map<String, Set<String>> typeTagsMap = new HashMap<>();
		String[] tags = postTags[0];
		String[] types = postTags[1];
		for(int i=0; i<tags.length; i++){
			String type = types[i];
			Set<String> wordSet = typeTagsMap.get(type);
			if(wordSet == null){
				wordSet = new HashSet<>();
				typeTagsMap.put(type, wordSet);
			}
			wordSet.add(tags[i]);
		}
		return typeTagsMap;
	}
	
	static String[][] parsePostTags(String json) throws JSONException{
		JSONArray jArray = new JSONArray(json);
		String[][] postTags = new String[2][];
		for(int i=0; i<postTags.length; i++){
			List<String> list = new ArrayList<String>();
			JSONArray vals = jArray.optJSONArray(i);
			for(int j=0; j<vals.length(); j++){
				list.add(vals.optString(j));
			}
			postTags[i] = list.toArray(new String[0]);
		}
		return postTags;
	}
	
}