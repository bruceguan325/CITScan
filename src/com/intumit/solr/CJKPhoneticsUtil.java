package com.intumit.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;

public class CJKPhoneticsUtil {

    static Hashtable<Character, Character> mapping = null;//new Hashtable<String, String>();
    static Hashtable<Character, List<Character>> sameWordDiffPhoneticsMapping = null;//new Hashtable<String, String>();

	synchronized private static void loadMap() {
		if (mapping != null)
			return;
		
		InputStream inputStream = null;
		BufferedReader reader = null;
    	int sameWordDiffPhoneticsMoreThanOneCount = 0;

        try {
        	inputStream = CJKPhoneticsUtil.class.getResourceAsStream("phone2.csv");
        	reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	        StringBuilder sb = new StringBuilder();
	        
	        mapping = new Hashtable<Character, Character>();
	        sameWordDiffPhoneticsMapping = new Hashtable<Character, List<Character>>();
	        Hashtable<String, Character> allPh = new Hashtable<String, Character>();
	        
	        String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
                String[] split = line.split(",");
                
                String keystring = split[0]; 
                String val = split[1];
                
                keystring = keystring;//keystring.replaceAll("＊.*", ""); 
                String keys[] = keystring.split("([0-9]+\\.)|＊");

                for (int iii=0; iii < keys.length; iii++) {
                	String key = keys[iii].trim();
                	
	                if (key == null || key.trim().length() == 0
		                || val == null || val.trim().length() == 0) {
	                	continue;
	                }
	                
	                char word = val.charAt(0); 
	                char mappedWord = word;
	                Character.UnicodeBlock ubWord = Character.UnicodeBlock.of(word);
	                
	                if ((ubWord == Character.UnicodeBlock.BASIC_LATIN)
	                        || (ubWord == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)) {
	                	System.out.println("Not a valid CJK UnicodeBlock:[" + word + "]:" + keystring);
	                	continue;
	                }
	                
	                if (allPh.containsKey(key)) {
	                	mappedWord = allPh.get(key);
	                }
	                else {
	                	allPh.put(key, word);
	                }
	
	            	mapping.put(word, mappedWord);
	            	
	                if (sameWordDiffPhoneticsMapping.containsKey(word)) {
	                	if (sameWordDiffPhoneticsMapping.get(word).size() == 1) {
	                		sameWordDiffPhoneticsMoreThanOneCount++;
//	                		System.out.println("[" + word + "]:" + sameWordDiffPhoneticsMapping.get(word));
	                	}
	                	
	                	sameWordDiffPhoneticsMapping.get(word).add(mappedWord);
	                }
	                else {
	                	ArrayList<Character> l = new ArrayList<Character>();
	                	l.add(mappedWord);
	                	sameWordDiffPhoneticsMapping.put(word, l);
	                }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	if (reader != null)
            		reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("load cjk phonetics mapping:" + mapping.size());
        System.out.println("same word w/ diff phonetics:" + sameWordDiffPhoneticsMoreThanOneCount);
	}
	
	/* �o�ӬOŪ�u�s�ŭ��v���`����Ӫ� "phone.cin"�A��]���}���r�S���Щ�A�Ӧh�F����B�z
	 * 
	   synchronized private static void loadMap() {
		if (mapping != null)
			return;
		
		InputStream inputStream = null;
		BufferedReader reader = null;
    	int sameWordDiffPhoneticsMoreThanOneCount = 0;

        try {
        	inputStream = CJKPhoneticsUtil.class.getResourceAsStream("phone.cin");
        	reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	        StringBuilder sb = new StringBuilder();
	        
	        mapping = new Hashtable<Character, Character>();
	        sameWordDiffPhoneticsMapping = new Hashtable<Character, List<Character>>();
	        Hashtable<String, Character> allPh = new Hashtable<String, Character>();
	        
	        String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
                String[] split = line.split(" ");
                String key = split[0];  // �`��
                String val = split[1];  // �r

                if (key == null || key.trim().length() == 0
	                || val == null || val.trim().length() == 0) {
                	continue;
                }
                
                char word = val.charAt(0); // �ഫ�� char ���A
                char mappedWord = word;    // �w�]�����r
                Character.UnicodeBlock ubWord = Character.UnicodeBlock.of(word);
                
                if ((ubWord == Character.UnicodeBlock.BASIC_LATIN)
                        || (ubWord == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)) {
                	System.out.println("Not a valid CJK UnicodeBlock:[" + word + "]");
                	continue;
                }
                
                if (allPh.containsKey(key)) {
                	// �Y�ӵo���w�g�������r�]�ӵo�����Ĥ@�Ӧr�^�A���N�������r�ӥ�
                	mappedWord = allPh.get(key);
                }
                else {
                	// �p�G�Y�ӵo���٨S�������r�A���N�������r�������Y
                	allPh.put(key, word);
                }

            	mapping.put(word, mappedWord);
            	
            	// �}���r��Ӫ�            	
                if (sameWordDiffPhoneticsMapping.containsKey(word)) {
                	if (sameWordDiffPhoneticsMapping.get(word).size() == 1)
                		sameWordDiffPhoneticsMoreThanOneCount++;
                	
                	sameWordDiffPhoneticsMapping.get(word).add(mappedWord);
//                	System.out.print("[" + word + "]:");
//                	System.out.println(sameWordDiffPhoneticsMapping.get(word));
                }
                else {
                	ArrayList<Character> l = new ArrayList<Character>();
                	l.add(mappedWord);
                	sameWordDiffPhoneticsMapping.put(word, l);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("load cjk phonetics mapping:" + mapping.size());
        System.out.println("same word w/ diff phonetics:" + sameWordDiffPhoneticsMoreThanOneCount);
	}*/
	
	public static char getMappedWord(char orig) {
		loadMap();
		
		if (mapping.containsKey(orig))
			return mapping.get(orig);
		
		return orig;
	}
	
	
	public static boolean hasDiffPhonetics(char orig) {
		loadMap();
		
		//System.out.println("[" + orig + "]:" + sameWordDiffPhoneticsMapping.get(orig));
		if (sameWordDiffPhoneticsMapping.containsKey(orig)) {
			return sameWordDiffPhoneticsMapping.get(orig).size() > 1;
		}
		
		return false;
	}
	
	public static List<Character> getDiffPhonetics(char orig) {
		loadMap();
		
		return sameWordDiffPhoneticsMapping.get(orig);
	}

	public static void findAllPhonetics(String value,
			List<String> phonetics) {
		loadMap();
		
		int estimateSentences = 1;
		
		char[] vChars = value.toCharArray();
		ArrayList<String> sba = new ArrayList<String>();
		
		if (hasDiffPhonetics(vChars[0])) {
			List<Character> diffP = getDiffPhonetics(vChars[0]);
			estimateSentences *= diffP.size();
			
			for (int jjj=0; jjj < diffP.size(); jjj++) {
				sba.add(diffP.get(jjj).toString());
			}
		}
		else {
			sba.add(new String(new char[] {getMappedWord(vChars[0])}));
		}
		
		for (int iii=1; iii < vChars.length; iii++) {
			
			if (hasDiffPhonetics(vChars[iii]) && estimateSentences < 1024) {
				List<Character> diffP = getDiffPhonetics(vChars[iii]);
				estimateSentences *= diffP.size();
					
				ArrayList<String> tmpAll = new ArrayList<String>();
				
				for (int kkk = 0; kkk < diffP.size(); kkk++) {
					ArrayList<String> newSba = (ArrayList<String>)sba.clone();
					
					for (int mmm = 0; mmm < newSba.size(); mmm++) {
						newSba.set(mmm, newSba.get(mmm) + diffP.get(kkk));
					}
					
					tmpAll.addAll(newSba);
				
					if (estimateSentences >= 1024) // 最多能忍受的句子排列組合數量，太多很可怕
						break;
				}
				
				sba = tmpAll;
			}
			else {
				for (int mmm = 0; mmm < sba.size(); mmm++) {
					sba.set(mmm, sba.get(mmm) + getMappedWord(vChars[iii]));
				}
			}
		}
		
		for (int i=0; i < sba.size(); i++) {
			String word = sba.get(i);
			
			if (!phonetics.contains(word) && !StringUtils.equals(value, word))
				phonetics.add(word);
		}
	}
	
	public static void main(String[] args) {
		
		String term = "姐姐";
		Stack<String> tmpAll = new Stack<String>();
		
		findAllPhonetics(term, tmpAll);
		
		System.out.println(tmpAll);
	}
}
