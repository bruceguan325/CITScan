package com.intumit.android.search.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.intumit.android.search.CJKSplitter;
import com.intumit.android.search.Constants;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class ChineseHelper {
	// 漢語拼音格式輸出類
	static HanyuPinyinOutputFormat hanYuPinOutputFormat = new HanyuPinyinOutputFormat();

	static {
		// 輸出設置，大小寫，音標方式等
		hanYuPinOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		hanYuPinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		hanYuPinOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
	}
	
	static Map<CharSequence, List<Phome>> mps2Map = new HashMap<CharSequence, List<Phome>>();
	
	static Map<CharSequence, List<Phome>> mpsNoTone2Map = new HashMap<CharSequence, List<Phome>>();
	
	static {
		/*Map<CharSequence, CharSequence> phoneMap = readMap("%keyname", false);
		phoneMap.put("3", "");
		phoneMap.put("4", "");
		phoneMap.put("6", "");
		phoneMap.put("7", "");
		mps2Map = readMultiValueMap("%chardef", null, phoneMap, true);*/
		try {
			mpsNoTone2Map = readMultiValueMapFromTsi("oldTsi.src");
			mps2Map = readMultiValueMapFromTsi("newTsi.src");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println(mps2Map);
	}

	/**
	 * All pinyin, and all first letter of pinyin
	 * Mixed in one method bcz we care about performance
	 * 
	 * @author herb
	 * @param src
	 * @return Set<String>
	static String[][] getPinyinStuffsForIndex(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				char[] srcChar;
				srcChar = src.toCharArray();

				String[][] temp = new String[src.length()][];
				String[][] temp2 = new String[src.length()][];
				for (int i = 0; i < srcChar.length; i++) {
					char c = srcChar[i];
					// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
					if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
						try {
							temp[i] = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
							
							if (temp[i] != null) {
								int length = temp[i].length;
								temp2[i] = new String[length];
								for (int j=0; j < length; j++) {
									temp2[i][j] = temp[i][j].substring(0, 1);
								}
							}
						}
						catch (BadHanyuPinyinOutputFormatCombination e) {
							e.printStackTrace();
						}
					}
					else if (((int) c >= 65 && (int) c <= 90) || ((int) c >= 97 && (int) c <= 122)) {
						temp[i] = new String[] { String.valueOf(srcChar[i]) };
					}
					else {
						temp[i] = new String[] { "" };
					}
				}
				
				String[] pingyinArray = Exchange(temp);
				String[] pingyinArrayOfFirstLetter = Exchange(temp2);
				return new String[][] {pingyinArray, pingyinArrayOfFirstLetter};
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	 */


	/**
	 * All pinyin, and all first letter of pinyin
	 * Mixed in one method bcz we care about performance
	 * 
	 * @author Herb
	 * @param src
	 * @return Set<String>
	 */
	public static Map<String, Set<String>> getPinyinSynonyms(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				Map<String, Set<String>> map = new HashMap<String, Set<String>>();
				
				CJKSplitter splitter = new CJKSplitter(src, true);

				while (splitter.next()) {
					String token = splitter.getToken();
					if (!splitter.isCJK()) continue;
					
					char[] srcChar = token.toCharArray();
					
					boolean doFirstLetter = srcChar.length > 2;
					String[][] temp2 = doFirstLetter ? new String[token.length()][] : null;
					
					
					
					for (int i = 0; i < srcChar.length; i++) {
						char c = srcChar[i];
						// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
						if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
							try {
								String[] pinyins = PinyinHelper
										.toHanyuPinyinStringArray(srcChar[i],
												hanYuPinOutputFormat);

								if (pinyins != null) {
									Set<String> pinyinSet = new HashSet<String>();
									pinyinSet.addAll(Arrays.asList(pinyins));
									map.put(src.substring(i, i+1),
											pinyinSet);

									if (doFirstLetter) {
										int length = pinyins.length;
										temp2[i] = new String[length];
	
										for (int j = 0; j < length; j++) {
											temp2[i][j] = pinyins[j]
													.substring(0, 1);
										}
									}
								}
							} catch (BadHanyuPinyinOutputFormatCombination e) {
								e.printStackTrace();
							}
						}
					}

					if (doFirstLetter) {
						CharSequence[] pingyinArrayOfFirstLetter = Exchange(temp2);
						Set<String> pinyinSet4FirstLetter = new HashSet<String>();
						for (CharSequence str : pingyinArrayOfFirstLetter) {
							pinyinSet4FirstLetter.add(str.toString());
						}
	
						map.put(token, pinyinSet4FirstLetter);
					}
				}
				
				//System.out.println(map);
				return map;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}
	
	/**
	 * All pinyin, and all first letter of pinyin
	 * Mixed in one method bcz we care about performance
	 * 
	 * @author herb
	 * @param src
	 * @return Set<String>
	 */
	public static Map<String, Set<String>> getPinyinSynonymsForLongText(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				Map<String, Set<String>> map = new HashMap<String, Set<String>>();
				CJKSplitter splitter = new CJKSplitter(src, true);

				while (splitter.next()) {
					String token = splitter.getToken();
					if (!splitter.isCJK()) continue;
					
					char[] srcChar;
					srcChar = token.toCharArray();
					for (int i = 0; i < srcChar.length; i++) {
						char c = srcChar[i];
						// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
						if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
							try {
								String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
								
								if (pinyins != null) {
									Set<String> pinyinSet = new HashSet<String>();
									pinyinSet.addAll(Arrays.asList(pinyins));
									map.put(new String(srcChar, i, 1), pinyinSet);
								}
							}
							catch (BadHanyuPinyinOutputFormatCombination e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				return map;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * All pinyin, and all first letter of pinyin
	 * Mixed in one method bcz we care about performance
	 * 
	 * @author herb
	 * @param src
	 * @return Set<String>
	 */
	static Set<String>[] getUnigramPinyinStuffsForIndex(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {

				Set<String> pinyinSet = new HashSet<String>();
				char[] srcChar;
				srcChar = src.toCharArray();
				String[][] temp2 = new String[src.length()][];
				for (int i = 0; i < srcChar.length; i++) {
					char c = srcChar[i];
					// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
					if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
						try {
							String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
							
							if (pinyins != null) {
								pinyinSet.addAll(Arrays.asList(pinyins));
								
								int length = pinyins.length;
								temp2[i] = new String[length];
								
								for (int j=0; j < length; j++) {
									temp2[i][j] = pinyins[j].substring(0, 1);
								}
							}
						}
						catch (BadHanyuPinyinOutputFormatCombination e) {
							e.printStackTrace();
						}
					}
				}
				
				CharSequence[] pingyinArrayOfFirstLetter = Exchange(temp2);
				Set<CharSequence> pinyinSet4FirstLetter = new HashSet<CharSequence>();
				for (CharSequence str: pingyinArrayOfFirstLetter) {
					pinyinSet4FirstLetter.add(str);
				}
				
				return new Set[] {pinyinSet, pinyinSet4FirstLetter};
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Get set of all pinyin
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getPinyin(char c) {
		try {
			// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
			if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
				try {
					String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, hanYuPinOutputFormat);
					
					return new HashSet<String>(Arrays.asList(arr));
				}
				catch (BadHanyuPinyinOutputFormatCombination e) {
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get set of all pinyin
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getPinyin(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				char[] srcChar;
				srcChar = src.toCharArray();

				String[][] temp = new String[src.length()][];
				for (int i = 0; i < srcChar.length; i++) {
					char c = srcChar[i];
					// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
					if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
						try {
							temp[i] = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
						}
						catch (BadHanyuPinyinOutputFormatCombination e) {
							e.printStackTrace();
						}
					}
					else if (((int) c >= 65 && (int) c <= 90) || ((int) c >= 97 && (int) c <= 122)) {
						temp[i] = new String[] { String.valueOf(srcChar[i]) };
					}
					else {
						temp[i] = new String[] { "" };
					}
				}
				
				CharSequence[] pingyinArray = Exchange(temp);
				Set<String> pinyinSet = new HashSet<String>();
				for (int i = 0; i < pingyinArray.length; i++) {
					pinyinSet.add(pingyinArray[i].toString());
				}
				return pinyinSet;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		System.out.println(getMPS2("信用卡掛失", 1));
	}
	

	/**
	 * 所有可能的注音
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getMPS2(String src, int threshold) {
		return getMPS2(src, threshold, mps2Map);
	}

	/**
	 * 所有可能的注音（不管聲調）
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getNoToneMPS2(String src, int threshold) {
		return getMPS2(src, threshold, mpsNoTone2Map);
	}

	/**
	 * 取得所有可能的注音
	 * 
	 * @author wyh
	 * @param src
	 * @param threshold
	 * @param mps2Map 自訂注音表
	 * @return Set<String>
	 */
	static Set<String> getMPS2(String src, int threshold, Map<CharSequence, List<Phome>> mps2Map) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {

				CharSequence[][] temp = new CharSequence[src.length()][];
				for (int i = 0; i < src.length(); i++) {
					CharSequence c = src.subSequence(i,  i+1);

					if (mps2Map.containsKey(c)) {
						//System.out.println(c + ":" + mps2Map.get(c));
						List<CharSequence> list = new ArrayList<CharSequence>();
						
						for (Phome p: mps2Map.get(c)) {
							if (p.score >= threshold) {
								list.add(p.pinyin);
							}
						}
						
						if (list.size() > 0) {
							temp[i] = list.toArray(new String[0]);
						}
						else {
							for (Phome p: mps2Map.get(c)) {
								list.add(p.pinyin);
							}
							temp[i] = list.toArray(new String[0]);
						}
					}
					else {
						temp[i] = new CharSequence[] { c };
					}
				}
				
				CharSequence[] pingyinArray = Exchange(temp);
				Set<String> pinyinSet = new HashSet<String>();
				for (int i = 0; i < pingyinArray.length; i++) {
					pinyinSet.add(pingyinArray[i].toString());
				}
				return pinyinSet;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Get set of all pinyin
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getPinyinForQuery(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				Set<String> finalSet = new HashSet<String>();
				CJKSplitter splitter = new CJKSplitter(src, true);

				while (splitter.next()) {
					String token = splitter.getToken();
					if (!splitter.isCJK()) continue;
					
					char[] srcChar;
					srcChar = token.toCharArray();
					for (int i = 0; i < srcChar.length; i++) {
						char c = srcChar[i];
						// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
						if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
							try {
								String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
								
								if (pinyins != null) {
									finalSet.addAll(Arrays.asList(pinyins));
								}
							}
							catch (BadHanyuPinyinOutputFormatCombination e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				return finalSet;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}


	/**
	 * Get set of all the first letter of pinyin
	 * 
	 * @author wyh
	 * @param src
	 * @return Set<String>
	 */
	public static Set<String> getFirstLetterCombinationOfPinyin(String src) {
		try {
			if (src != null && !src.trim().equalsIgnoreCase("")) {
				char[] srcChar;
				srcChar = src.toCharArray();

				CharSequence[][] temp = new CharSequence[src.length()][];
				for (int i = 0; i < srcChar.length; i++) {
					char c = srcChar[i];
					// 是中文或者a-z或者A-Z轉換拼音(我的需求，是保留中文或者a-z或者A-Z)
					if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
						try {
							temp[i] = PinyinHelper.toHanyuPinyinStringArray(srcChar[i], hanYuPinOutputFormat);
							
							for (int j=0; j < temp[i].length; j++) {
								temp[i][j] = temp[i][j].subSequence(0, 1);
							}
						}
						catch (BadHanyuPinyinOutputFormatCombination e) {
							e.printStackTrace();
						}
					}
					else {
						temp[i] = new String[] { "" };
					}
				}
				CharSequence[] pingyinArray = Exchange(temp);
				Set<String> pinyinSet = new HashSet<String>();
				for (int i = 0; i < pingyinArray.length; i++) {
					pinyinSet.add(pingyinArray[i].toString());
				}
				return pinyinSet;
			}
		}
		catch (Exception e) {
			if (Constants.DEBUG) {
				System.out.println("Cannot get pinyin of [" + src + "]:" + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Recursive
	 * 
	 * @author wyh
	 * @param strJaggedArray
	 * @return
	 */
	static CharSequence[] Exchange(CharSequence[][] strJaggedArray) {
		CharSequence[][] temp = DoExchange(strJaggedArray);
		return temp[0];
	}

	/**
	 * Recursive
	 * 
	 * @author wyh
	 * @param strJaggedArray
	 * @return
	 */
	static CharSequence[][] DoExchange(CharSequence[][] strJaggedArray) {
		int len = strJaggedArray.length;
		if (len >= 2) {
			int len1 = strJaggedArray[0].length;
			int len2 = strJaggedArray[1].length;
			int newlen = len1 * len2;
			CharSequence[] temp = new CharSequence[newlen];
			int Index = 0;
			for (int i = 0; i < len1; i++) {
				for (int j = 0; j < len2; j++) {
					temp[Index] = new StringBuilder().append(strJaggedArray[0][i]).append(strJaggedArray[1][j]).toString();
					Index++;
				}
			}
			CharSequence[][] newArray = new CharSequence[len - 1][];
			for (int i = 2; i < len; i++) {
				newArray[i - 1] = strJaggedArray[i];
			}
			newArray[0] = temp;
			return DoExchange(newArray);
		}
		else {
			return strJaggedArray;
		}
	}
	
	
	static public Map<CharSequence, CharSequence> readMap(String area, boolean reverse) {
		InputStream is = ChineseHelper.class
				.getResourceAsStream("phone.cin");

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		Map<CharSequence, CharSequence> data = new HashMap<CharSequence, CharSequence>();
		boolean foundBegin = false;
		String line = null;

		try {
			while ( (line = reader.readLine()) != null) {
				if (!foundBegin) {
					// Got Begin
					if (line.startsWith(area)) {
						foundBegin = true;
					}
					continue;
				}
				else {
					// Got End
					if (line.startsWith(area)) {
						break;
					}
				}
				
				String key = StringUtils.trimToNull(StringUtils.substringBefore(line, " "));
				String value = StringUtils.trimToNull(StringUtils.substringAfter(line, " "));
			
				if (reverse) {
					String tmp = value;
					value = key;
					key = tmp;
				}
				
				if (key == null || value == null) {
					System.out.println("Bad line:[" + line + "]");
					continue;
				}
	
				data.put(key, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	
	static String convertViaMap(String text, Map<CharSequence, CharSequence> map) {

		String newKey = "";
		
		for (int i=0; i < text.length(); i++) {
			CharSequence charAtI = text.subSequence(i, i+1);
			
			if (map.containsKey(charAtI)) {
				newKey += map.get(charAtI);
			}
			else {
				newKey += charAtI;
			}
		}
		
		return newKey;
	}
	
	

	static public Map<CharSequence, Set<CharSequence>> readMultiValueMap(String area, Map<CharSequence, CharSequence> keyMap, Map<CharSequence, CharSequence> valueMap, boolean reverse) throws UnsupportedEncodingException {
		InputStream is = ChineseHelper.class
				.getResourceAsStream("phone.cin");

		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF8"));
		
		Map<CharSequence, Set<CharSequence>> data = new HashMap<CharSequence, Set<CharSequence>>();
		boolean foundBegin = false;
		String line = null;

		try {
			while ( (line = reader.readLine()) != null) {
				if (!foundBegin) {
					// Got Begin
					if (line.startsWith(area)) {
						foundBegin = true;
					}
					continue;
				}
				else {
					// Got End
					if (line.startsWith(area)) {
						break;
					}
				}
				
				String key = StringUtils.trimToNull(StringUtils.substringBefore(line, " "));
				String value = StringUtils.trimToNull(StringUtils.substringAfter(line, " "));
			
				if (reverse) {
					String tmp = value;
					value = key;
					key = tmp;
				}
				
				if (key == null || value == null) {
					System.out.println("Bad line:[" + line + "]");
					continue;
				}
				
				if (keyMap != null) key = convertViaMap(key, keyMap);
				if (valueMap != null) value = convertViaMap(value, valueMap);
				
				if (data.containsKey(key)) {
					data.get(key).add(value);
				}
				else {
					HashSet<CharSequence> set = new HashSet<CharSequence>();
					set.add(value);
					data.put(key, set);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}

	static public Map<CharSequence, List<Phome>> readMultiValueMapFromTsi(String tsiFilename) throws UnsupportedEncodingException {
		InputStream is = ChineseHelper.class
				.getResourceAsStream(tsiFilename);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF8"));
		
		Map<CharSequence, List<Phome>> data = new HashMap<CharSequence, List<Phome>>();
		String line = null;
		int lc = 0;

		try {
			while ( (line = reader.readLine()) != null) {
				
				lc++;
				String[] splited = line.split(" ");
				
				if (splited.length < 3) {
					System.out.println("Bad line " + lc + ":[" + line + "]");
					continue;
				}
				
				String key = StringUtils.trimToNull(splited[0]);
				String value = StringUtils.trimToNull(splited[2]);
				String scoreStr = StringUtils.trimToNull(splited[1]);
				
				if (key == null || value == null || scoreStr == null) {
					System.out.println("Bad line " + lc + ":[" + line + "]");
					continue;
				}

				try {
					int score = Integer.parseInt(scoreStr);
					Phome p = new Phome(value, score);
					
					if (data.containsKey(key)) {
						List<Phome> set = data.get(key);
						int idx = set.indexOf(p);
						
						if (idx != -1) {
							Phome exist = set.get(idx);
							
							if (exist.score < p.score) {
								set.set(idx, p);
							}
						}
						else {
							set.add(p);
						}
					}
					else {
						List<Phome> set = new ArrayList<Phome>();
						set.add(p);
						data.put(key, set);
					}
				} catch (Exception e) {
					System.out.println("Bad line " + lc + ":[" + line + "]:" + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}
	

	
	static class Phome {
		CharSequence pinyin;
		int score;
		
		public Phome(CharSequence pinyin) {
			this(pinyin, 0);
		}
		
		public Phome(CharSequence pinyin, int score) {
			super();
			this.pinyin = pinyin;
			this.score = score;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((pinyin == null) ? 0 : pinyin.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Phome other = (Phome) obj;
			if (pinyin == null) {
				if (other.pinyin != null)
					return false;
			} else if (!pinyin.equals(other.pinyin))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Phome [pinyin=" + pinyin + ", score=" + score + "]";
		}
		
	}
}
