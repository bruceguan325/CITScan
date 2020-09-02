package com.intumit.android.search.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Stopwatch;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hazelcast.util.collection.ArrayUtils;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;

public class TaiwanAddressNormalizeUtil implements Serializable {
	public static enum AddrType {
		ADMINISTRATIVE_AREA,
		LOCALITY,
		SUBLOCALITY,
	}
	public static class AddrPart {
		public AddrPart(AddrType type, String text) {
			this.type = type;
			this.text = text;
		}
		public AddrType type;
		public String text;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((text == null) ? 0 : text.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			AddrPart other = (AddrPart) obj;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			if (type != other.type)
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "AddrPart [type=" + type + ", text=" + text + "]";
		}
		
	}
	
	private AddressFormatConfig formatCfg;
	private String address=null;
	private String postal_code="";
	private String country="台灣";
	private String administrative_area=null;
	private String locality=null;
	private String sublocality=null;
	private String route=null;
	private String street_number=null;
	private String floor=null;
	private Double latitude=null,longitude=null;
	private Integer checkLevel=0,errorValue=99;
	private Integer weight=0;
	private Date mydate;
	private ArrayList<String> data=new ArrayList<String>();
	
	final private List<String> separators;
	final private Map<String, String> synonyms;
	
	final private static Map<String, String> DEFAULT_SYNONYMS = new HashMap<String, String>(){{
				put("臺","台");	put("巿","市");
				put("廍","部");	put("双","雙");	put("峯","峰");	put("响","響");	put("脚","腳");
				put("舘","館");	put("豊","豐");	put("磘","瑤");	put("濓","濂");	put("猴","侯");
				put("菓","果");	put("槺","康");	put("嵵","蒔");	put("梁","樑");	put("岐","歧");
				put("坂","板");	put("晋","晉");	put("洲","州");
			}};
	final private static List<String> DEFAULT_SEPARATOR = new ArrayList<String>(){{
				add("：");add("位於");add("鄰近");
				add("臺灣");
				add("縣");add("市"); //administrative_area
				add("區");add("鄉");add("鎮");add("台"); //locality
				add("里里");add("里");add("村"); //sublocality
				add("鄰");			//忽略
				add("弄");add("巷");add("段");add("街");add("路");add("道");//route
				add("對面");add("出口");add("號");//street_number
				add("樓");add("室");add("F");add("f"); //忽略
			}};
			


	static AhoCorasickDoubleArrayTrie<AddrPart[]> addrPartTrie = new AhoCorasickDoubleArrayTrie<AddrPart[]>();
	static AhoCorasickDoubleArrayTrie<String[]> adminAreaTrie = new AhoCorasickDoubleArrayTrie<String[]>();
	static AhoCorasickDoubleArrayTrie<String[]> localityTrie = new AhoCorasickDoubleArrayTrie<String[]>();
	private static Map<String, Set<String>> adminToLocalityMap = new HashMap<>();
	public static Set<String> getLocalitiesByAdministrativeArea(String adminArea) {
		return adminToLocalityMap.get(adminArea);
	}
	public static Set<String> searchAdministrativeAreaByLocality(String locality) {
		Set<String> s = new HashSet<>();
		for (String admin: adminToLocalityMap.keySet()) {
			if (adminToLocalityMap.get(admin).contains(locality)) {
				s.add(admin);
			}
		}
		return s;
	}
	public static Set<String> searchAdministrativeArea(String adminArea) {
		Set<String> s = new HashSet<>();
		for (String admin: adminToLocalityMap.keySet()) {
			if (adminToLocalityMap.get(admin).contains(adminArea)) {
				s.add(admin);
			}
		}
		return s;
	}
	public static List<AddrPart> searchAddrPart(String text) {
		char[] charArray = text.toCharArray();
		final List<AddrPart> results = new ArrayList<AddrPart>();
    	
        final AddrPart[][] wordNet = new AddrPart[charArray.length][];
        final int[] lengthNet = new int[charArray.length];
        
        addrPartTrie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<AddrPart[]>()
        {
            @Override
            public void hit(int begin, int end, AddrPart[] value)
            {
                int length = end - begin;
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	results.addAll(Arrays.asList(wordNet[offset]));
            	offset += lengthNet[offset];
            }
        }
        return results;
	}
	public static AhoCorasickDoubleArrayTrie<AddrPart[]> getAddrTrie() {
        return addrPartTrie;
	}

    protected static List<String[]> searchPossibleStringInTrie(AhoCorasickDoubleArrayTrie<String[]> trie, char[] charArray) {
    	final List<String[]> results = new ArrayList<String[]>();
    	
        final String[][] wordNet = new String[charArray.length][];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<String[]>()
        {
            @Override
            public void hit(int begin, int end, String[] value)
            {
                int length = end - begin;
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	results.add(wordNet[offset]);
            	offset += lengthNet[offset];
            }
        }
        return results;
    }
	private static TreeSet<String> geocodedataList = new TreeSet<String>();
	static {
		InputStreamReader fr;
		BufferedReader br;
		try {
			TreeMap<String, String[]> adminAreaShortToFormal = new TreeMap<String, String[]>();
			TreeMap<String, String[]> localityShortToFormal = new TreeMap<String, String[]>();
			TreeMap<String, AddrPart[]> addrPartMap = new TreeMap<String, AddrPart[]>();
			
			fr = new InputStreamReader(
					TaiwanAddressNormalizeUtil.class
							.getResourceAsStream("geocodedata.txt"));
			br = new BufferedReader(fr);
			String str;
			while ((str = br.readLine()) != null) {
				String[] datas = str.split("\t");

				String admin = datas[1];
				String locality = datas[3];
				String sublocality = datas[5];
				geocodedataList.add(admin);
				geocodedataList.add(locality);
				geocodedataList.add(sublocality);

				if (!adminToLocalityMap.containsKey(admin)) {
					adminToLocalityMap.put(admin, new HashSet<String>());
				}
				AddrPart adminAP = new AddrPart(AddrType.ADMINISTRATIVE_AREA, admin);
				AddrPart localityAP = new AddrPart(AddrType.LOCALITY, locality);
				addrPartMap.put(admin, new AddrPart[] {adminAP});
				if (admin.indexOf("臺") != -1) {
					addrPartMap.put(admin.replaceAll("臺", "台"), new AddrPart[] {adminAP});
				}
				
				addrPartMap.put(locality, new AddrPart[] {localityAP});

				String shortAdminArea = admin.substring(0, admin.length() - 1);
				if (shortAdminArea.length() > 1 && !adminAreaShortToFormal.containsKey(shortAdminArea)) {
					adminAreaShortToFormal.put(shortAdminArea, new String[] { shortAdminArea, admin });
					addrPartMap.put(shortAdminArea, new AddrPart[] {adminAP});
					
					if (shortAdminArea.indexOf("臺") != -1) {
						addrPartMap.put(shortAdminArea.replaceAll("臺", "台"), new AddrPart[] {adminAP});
					}
				}
				
				String shortLocality = locality.substring(0, locality.length() - 1);
				if (shortLocality.length() > 1 && !localityShortToFormal.containsKey(shortLocality)) {
					localityShortToFormal.put(shortLocality, new String[] { shortLocality, locality });
					addrPartMap.put(shortLocality, new AddrPart[] {localityAP});
				}

				adminToLocalityMap.get(admin).add(locality);
			}

			adminAreaTrie.build(adminAreaShortToFormal);
			localityTrie.build(localityShortToFormal);
			addrPartTrie.build(addrPartMap);
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String decodeHtmlEntity(String source){
		int indexnum = 0;
		int beginIndex = source.indexOf("&#x", indexnum);
		while (beginIndex > -1) {
			int endIndex = source.indexOf(";", indexnum);
			// System.out.println(beginIndex+":"+endIndex);
			if (endIndex > beginIndex) {
				String strStr = source.substring(beginIndex, endIndex + 1);
				String dataStr = source.substring(beginIndex + 3, endIndex);
				// System.out.print(dataStr+" ");
				Integer i = Integer.parseInt(dataStr, 16);
				// System.out.println(i);
				String s = new String(Character.toChars(i));
				source = source.replaceAll(strStr, s);
				// System.out.println(source);
				// System.out.println(Character.toChars(i));
			} else {
				break;
			}
			beginIndex = source.indexOf("&#x", indexnum);
		}
		beginIndex = source.indexOf("&#", indexnum);
		// System.out.println(source);
		while (beginIndex > -1) {
			int endIndex = source.indexOf(";", indexnum);
			// System.out.println(beginIndex+":"+endIndex);
			if (endIndex > beginIndex) {
				String strStr = source.substring(beginIndex, endIndex + 1);
				String dataStr = source.substring(beginIndex + 2, endIndex);
				// System.out.print(dataStr+" ");
				Integer i = Integer.parseInt(dataStr, 10);
				// System.out.println(i);
				String s = new String(Character.toChars(i));
				source = source.replaceAll(strStr, s);
				// System.out.println(source);
			} else {
				break;
			}
			beginIndex = source.indexOf("&#", indexnum);
		}

		return source;
	}
	
	public TaiwanAddressNormalizeUtil(String address,
			AddressFormatConfig cfg, ArrayList<String> separators,
			HashMap<String, String> synonyms) {

		this.formatCfg = cfg;
		this.mydate = Calendar.getInstance().getTime();
		address = decodeHtmlEntity(address.replaceAll("臺", "台")
				.replaceAll(" ", "").replaceAll("　", "").trim());
		this.address = address;
		// address=java.net.URLDecoder.decode(address);
		this.synonyms = new HashMap<String, String>(DEFAULT_SYNONYMS);
		if (synonyms != null)
			this.synonyms.putAll(synonyms);

		for (String k : this.synonyms.keySet()) {
			address = address.replaceAll(k, this.synonyms.get(k));
		}
		if (separators == null) {
			this.separators = DEFAULT_SEPARATOR;
		} else {
			this.separators = separators;
		}
		address = address.replaceAll("（", "(").replaceAll("）", ")")
				.replaceAll(";", "； ").replaceAll("。", "； ");
		if (address.indexOf("(") > -1) {
			int beginIndex = address.indexOf("(");
			int endIndex = address.indexOf(")");
			if (endIndex - beginIndex > 1) {
				String s = address.substring(beginIndex, endIndex + 1);
				address = address.replace(s, "");
				s = s.replaceAll("\\(", "").replaceAll("\\)", "");
				address = address + "；" + s;
			}
		}
		String[] addresslist = address.split("；");
		/*ArrayList<TaiwanAddressNormalizeUtil> AUList = new ArrayList<TaiwanAddressNormalizeUtil>();
		TaiwanAddressNormalizeUtil bestAU = null;
		if (addresslist.length > 1) {
			// WebAddressUtil bestAU=null;
			int checkLevel = -1;
			for (String address_i : addresslist) {
				TaiwanAddressNormalizeUtil au = new TaiwanAddressNormalizeUtil(address_i);
				AUList.add(au);
				if (au.getCheckLevel() > checkLevel) {
					bestAU = au;
					checkLevel = au.getCheckLevel();
				}
				// System.out.println(au);
			}

		} else {
			splitALL(address);
		}*/
		splitALL(address);

		if (this.getLocality() == null) {
			List<String[]> candidates = this.searchPossibleStringInTrie(localityTrie, address.toCharArray());
			
			if (candidates.size() == 1) {
				String shortLocality = candidates.get(0)[0];
				if (!address.endsWith(shortLocality)) {
					// 如果猜測的短「區」名後面接著「路」「巷」之類的名字，代表猜錯了
					String nextWord = address.substring( address.indexOf(shortLocality) + shortLocality.length(), 
							address.indexOf(shortLocality) + shortLocality.length() + 1
							);
					if ("路巷街道".indexOf(nextWord) == -1) {
						address = address.replace(candidates.get(0)[0], candidates.get(0)[1]);
						this.setLocality(candidates.get(0)[1]);
					}
				}
				else {
					if (this.getAdministrative_area() != null) {
						Set<String> possibleAdminAreas = this.searchAdministrativeAreaByLocality(candidates.get(0)[1]);
						
						if (possibleAdminAreas.contains(this.getAdministrative_area())) {
							address = address.replace(candidates.get(0)[0], candidates.get(0)[1]);
							this.setLocality(candidates.get(0)[1]);
						}
					}
					else {
						address = address.replace(candidates.get(0)[0], candidates.get(0)[1]);
						this.setLocality(candidates.get(0)[1]);
					}
				}
			}
		}
		if (this.getAdministrative_area() == null && this.getLocality() != null) {
			Set<String> aas = searchAdministrativeAreaByLocality(this.getLocality());
			if (aas != null && aas.size() == 1) {
				this.setAdministrative_area(aas.iterator().next());
			}
		}
		splitALL(address);
		initSet();
		//aCompare(bestAU);
	}

	public TaiwanAddressNormalizeUtil(String address,
			AddressFormatConfig cfg, ArrayList<String> separators) {
		this(address, cfg, separators, null);
	}

	public TaiwanAddressNormalizeUtil(String address, AddressFormatConfig cfg) {
		this(address, cfg, null);
	}

	public TaiwanAddressNormalizeUtil(String address) {
		this(address, new AddressFormatConfig(false, true, true, true, true));
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPostal_code() {
		return postal_code;
	}
	public void setPostal_code(String postal_code) {
		this.postal_code = postal_code;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getAdministrative_area() {
		return administrative_area;
	}
	public void setAdministrative_area(String administrative_area) {
		this.administrative_area = administrative_area;
	}
	public String getLocality() {
		return locality;
	}
	public void setLocality(String locality) {
		this.locality = locality;
	}
	public String getSublocality() {
		return sublocality;
	}
	public void setSublocality(String sublocality) {
		this.sublocality = sublocality;
	}
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}
	public String getStreet_number() {
		return street_number;
	}
	public void setStreet_number(String street_number) {
		this.street_number = street_number;
	}
	public String getFloor() {
		return floor;
	}
	public void setFloor(String floor) {
		this.floor = floor;
	}
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	public Integer getCheckLevel() {
		return checkLevel;
	}
	public void setCheckLevel(Integer checkLevel) {
		this.checkLevel = checkLevel;
	}
	public Integer getErrorValue() {
		return errorValue;
	}
	public void setErrorValue(Integer errorValue) {
		this.errorValue = errorValue;
	}
	public Date getMydate() {
		return mydate;
	}
	public void setMydate(Date mydate) {
		this.mydate = mydate;
	}
	public ArrayList<String> getData() {
		return data;
	}
	private String detachStr(String src){
		if(src==null)return src;
		String[] detachCodes={",",".","，","~","、","．"};
		for(String detach:detachCodes){
			int detachIndex=src.lastIndexOf(detach);
			if(detachIndex>-1) src=src.substring(detachIndex+detach.length());
		}
		return src;
	}

	private void initSet() {
		adjustStreetNumber();
		adjustRoute();
		//adjustSublocality();
		//adjustAdministrative();
		//adjustCheckLevel();
		//adjustWeight();
	}
	
	/*private void splitALLOld(String address){
		int indexnum=0;
		for(int i=0;i<address.length();i++){
			if(address.charAt(i)<127){
				postal_code=postal_code+address.charAt(i);
			}else{
				indexnum=i;
				break;
			}
		}
		for(String splitCode:separators){
			int index=address.indexOf(splitCode, indexnum);
			
			if(index<0) continue;
			int dataLen=(index+splitCode.length()-indexnum);
			if(splitCode.equals("台灣")&&dataLen>3) continue;
			if(splitCode.equals("縣")&&(dataLen >3||dataLen<3)) continue;
			if(splitCode.equals("市")&&(dataLen >5||dataLen<2)) continue;
			if(splitCode.equals("台")&&(dataLen >5||dataLen<2))	continue;
			if(splitCode.equals("區")&&(dataLen >5||dataLen<2))	continue;
			if(splitCode.equals("里")&&(dataLen >5||dataLen<2)) 
				index=address.indexOf(splitCode, index+splitCode.length());
			if(splitCode.equals("村")&&(dataLen >5||dataLen<2))
				index=address.indexOf(splitCode, index+splitCode.length());
			if(index<0) continue;
			
			data.add(address.substring(indexnum, index+splitCode.length()));
			if(locality!=null)
				if(splitCode.equals("區")||splitCode.equals("鄉")||splitCode.equals("鎮")||splitCode.equals("台"))
					continue;
			if(sublocality!=null)
				if(splitCode.equals("里里")||splitCode.equals("里")||splitCode.equals("村"))
					continue;
			if(route!=null)
				if(splitCode.equals("弄")||splitCode.equals("巷")||splitCode.equals("段")||splitCode.equals("街")||splitCode.equals("路")||splitCode.equals("道"))
					continue;
			if(street_number!=null)
				if(splitCode.equals("對面")||splitCode.equals("出口")||splitCode.equals("號"))
					continue;
			indexnum=index+splitCode.length();
			//SaveField
			if(splitCode.equals("台灣")) country=data.get(data.size()-1);
			else if(splitCode.equals("縣")) administrative_area=data.get(data.size()-1);
			else if(splitCode.equals("市")){
				if(administrative_area==null){
					administrative_area=data.get(data.size()-1);
					continue;
				}
				locality=data.get(data.size()-1);
				continue;
			}else if(locality==null && splitCode.equals("區")) locality=data.get(data.size()-1);
			else if(locality==null && splitCode.equals("鄉")) locality=data.get(data.size()-1);
			else if(locality==null && splitCode.equals("鎮")) locality=data.get(data.size()-1);
			else if(locality==null && splitCode.equals("台")) locality=data.get(data.size()-1);
			else if(sublocality==null && splitCode.equals("里里")) sublocality=data.get(data.size()-1);
			else if(sublocality==null && splitCode.equals("村")) sublocality=data.get(data.size()-1);
			else if(sublocality==null && splitCode.equals("里")) sublocality=data.get(data.size()-1);
			else if(route==null && splitCode.equals("弄")) route=data.get(data.size()-1);
			else if(route==null && splitCode.equals("巷")) route=data.get(data.size()-1);
			else if(route==null && splitCode.equals("段")) route=data.get(data.size()-1);
			else if(route==null && splitCode.equals("街")) route=data.get(data.size()-1);
			else if(route==null && splitCode.equals("路")) route=data.get(data.size()-1);
			else if(route==null && splitCode.equals("道")) route=data.get(data.size()-1);
			else if(street_number==null && splitCode.equals("對面")) street_number=data.get(data.size()-1);
			else if(street_number==null && splitCode.equals("出口")) street_number=data.get(data.size()-1);
			else if(street_number==null && splitCode.equals("號")) street_number=data.get(data.size()-1);
		}
	}*/
	
	private void splitALL(String address){
		TreeSet<Integer> splitIndexSet=new TreeSet<Integer>();
		ArrayList<String> datas=new ArrayList<String>();
		//splitIndexSet.add(0);
		//String address=this.address;
		
		int indexTop=0;
		
		for(int i=0;i<address.length();i++){
			if(address.charAt(i)<127){
				postal_code=postal_code+address.charAt(i);
			}else{
				indexTop=i;
				break;
			}
		}
		splitIndexSet.add(address.length());
		for (String separator : separators) {
			int indexnum = indexTop;
			int index = address.indexOf(separator, indexnum);
			while (index > 0) {
				splitIndexSet.add(index + separator.length());
				indexnum = index + separator.length();
				index = address.indexOf(separator, indexnum);
			}
		}
		int beginIndex=indexTop;
		for (Integer i : splitIndexSet) {
			int len = i - beginIndex;
			String s = address.substring(beginIndex, i);
			if (!s.equals("市") && !s.equals("縣") && !s.equals("鎮")
					&& !s.equals("鄰") && !s.equals("台") && !s.equals("巷")) {
				if (len < 2 && datas.size() > 0) {
					datas.add(datas.size() - 1, datas.get(datas.size() - 1) + s);
					datas.remove(datas.size() - 1);
				} else
					datas.add(address.substring(beginIndex, i));
				beginIndex = i;
			}
		}
		route="";
		for(String splitCode:separators){
			for(String data:datas){
				if(data.trim().replaceAll(" ", "").equals(""))continue;
				if(data.indexOf(splitCode)!=(data.length()-splitCode.length()))continue;
				if(splitCode.equals("里")&&(data.length() >5||data.length()<2))continue;
				if(splitCode.equals("村")&&(data.length() >5||data.length()<2))continue;
				if(data.length()==1)continue;
				if(splitCode.equals("臺灣")) country=data;
				else if(administrative_area==null && splitCode.equals("縣")) administrative_area=data;
				else if(splitCode.equals("市")){
					if(administrative_area==null){
						administrative_area=data;
						continue;
					}
					if(administrative_area.indexOf("市")<0)
						locality=data;
					continue;
				}else if(locality==null && splitCode.equals("區")) locality=data;
				else if(locality==null && splitCode.equals("鄉")) locality=data;
				else if(locality==null && splitCode.equals("鎮")) locality=data;
				else if(locality==null && splitCode.equals("台")) locality=data;
				else if(sublocality==null && splitCode.equals("里里")) sublocality=data;
				else if(sublocality==null && splitCode.equals("村")) sublocality=data;
				else if(sublocality==null && splitCode.equals("里")) sublocality=data;
				else if(splitCode.equals("弄")) route=data+route;
				else if(splitCode.equals("巷")) route=data+route;
				else if(splitCode.equals("段") && route.indexOf("段")<0) route=data+route;
				else if(splitCode.equals("街") && route.indexOf("街")<0) route=data+route;
				else if(splitCode.equals("路") && route.indexOf("路")<0) route=data+route;
				else if(splitCode.equals("道") && route.indexOf("道")<0) route=data+route;
				else if(street_number==null && splitCode.equals("對面")) street_number=data;
				else if(street_number==null && splitCode.equals("出口")) street_number=data;
				else if(street_number==null && splitCode.equals("號")) street_number=data;
				else if(floor==null && splitCode.equals("樓")) floor=data;
				else if(floor==null && splitCode.equals("F")) floor=data;
			}
		}
		if(route.equals(""))route=null;
		this.data=datas;
		//System.out.print(datas);
		//System.out.println(splitIndexSet);
	}
	
	private void aCompare(TaiwanAddressNormalizeUtil target) {
		if(target==null)return;
		if(target.weight>this.weight){
			this.postal_code=target.postal_code;
			this.country=target.country;
			this.administrative_area=target.administrative_area;
			this.locality=target.locality;
			this.sublocality=target.sublocality;
			this.route=target.route;
			this.street_number=target.street_number;
			this.weight=target.weight;
			this.data=target.data;
			this.checkLevel=target.checkLevel;
			return;
		}
	}
	
	public String getFormatAddress(){
		String str="";
		if(formatCfg.withCountry && country!=null)str+=country;
		if(formatCfg.withAdministrativeArea && administrative_area!=null)str+=administrative_area;
		if(formatCfg.withLocality && locality!=null)str+=locality;
		if(formatCfg.withSublocality && sublocality!=null&&route==null&&street_number==null)str+=sublocality;
		if(route!=null)str+=route;
		if(street_number!=null)str+=street_number;
		if(formatCfg.withFloor && floor!=null)str+=floor;
		return str;
	}
	public String getFormatSublocalityAddress(){
		String str="";
		if(administrative_area!=null)str+=administrative_area;
		if(locality!=null)str+=locality;
		if(sublocality!=null)str+=sublocality;
		return str;
	}
	public String getFormatlocalityAddress(){
		String str="";
		if(administrative_area!=null)str+=administrative_area;
		if(locality!=null)str+=locality;
		return str;
	}
	
	public int getErrorValue(TaiwanAddressNormalizeUtil target){
		Integer errorValue=Math.max(this.weight, target.weight);
		if(target == null) return errorValue;
		//System.out.println("searcher:"+this.getFormatAddress());
		//System.out.println("resulter:"+target.getFormatAddress());
		//System.out.println("searcherErrorValue:"+(this.weight));
		//System.out.println("resulterErrorValue:"+(target.weight));
		//System.out.println("MAXerrorValue:"+(this.weight | target.weight));
		
		if(getFormatAddress().equals(target.getFormatAddress())){
			errorValue=0;
			return errorValue;
		}
		if(target.street_number!=null && this.street_number!=null){
			if(street_number.equals(target.street_number)){
				errorValue-=1;
				//System.out.print(" street_number:"+"equals "+errorValue);
			}else{
				checkLevel=5;
			}
			//street_number.compareTo(arg0)
		}
		if(target.route!=null && this.route!=null){
			if(route.equals(target.route)){
				errorValue-=2;
				//System.out.print(" route:"+"equals "+errorValue);
			}else{
				checkLevel=4;
			}
			//street_number.compareTo(arg0)
		}
		if(target.sublocality!=null && this.sublocality!=null){
			if(sublocality.equals(target.sublocality)){
				errorValue-=4;
				//System.out.print(" sublocality:"+"equals "+errorValue);
			}else if(sublocality.equals(target.sublocality.replace("村","里"))){
				errorValue-=4;
				//System.out.print(" sublocality:"+"equals "+errorValue);
			}else{
				checkLevel=3;
			}
			//street_number.compareTo(arg0)
		}else if(sublocality==null && target.sublocality!=null){
			errorValue-=4;
			//System.out.print(" sublocality:"+"equals "+errorValue);
		}
		if(target.locality!=null && this.locality!=null){
			if(locality.equals(target.locality)){
				errorValue-=8;
				//System.out.print(" locality:"+"equals "+errorValue);
			}else if(locality.equals(target.locality.replace("鄉","市"))){
				errorValue-=8;
				//System.out.print(" locality:"+"equals "+errorValue);
			}else{
				checkLevel=2;
			}
			//street_number.compareTo(arg0)
		}else if(locality==null && target.locality!=null){
			errorValue-=8;
			//System.out.print(" locality:"+"equals "+errorValue);
		}
		if(target.administrative_area!=null && this.administrative_area!=null){
			if(administrative_area.equals(target.administrative_area)){
				errorValue-=16;
				//System.out.print(" administrative_area:"+"equals "+errorValue);
			}else if(administrative_area.equals(target.administrative_area.replace("縣","市"))){
				errorValue-=16;
				//System.out.print(" administrative_area:"+"equals "+errorValue);
			}else{
				checkLevel=1;
			}
			//street_number.compareTo(arg0)
		}
		if(target.country!=null && this.country!=null){
			if(country.equals(target.country)){
				errorValue-=32;
				//System.out.print(" country:"+"equals "+errorValue);
			}else{
				checkLevel=0;
			}
			//street_number.compareTo(arg0)
		}
		//System.out.println();
		//errorValue=99;
		return errorValue;
	}
	
	private void adjustRoute() {
		if(route==null)return;
		int index=route.indexOf("段");
		if(index<0)	index=route.indexOf("路");
		if(index<0)	index=route.indexOf("道");
		if(index<0)	index=route.indexOf("街");
		if(index<0){
			route=adjustNumber(route);
			return;
		}
		String ns=route.substring(index+1,route.length());
		ns=adjustNumber(ns);
		
		String ps=route.substring(0,index+1);
		ps=ps.replaceAll("１","一");
		ps=ps.replaceAll("２","二");
		ps=ps.replaceAll("３","三");
		ps=ps.replaceAll("４","四");
		ps=ps.replaceAll("５","五");
		ps=ps.replaceAll("６","六");
		ps=ps.replaceAll("７","七");
		ps=ps.replaceAll("８","八");
		ps=ps.replaceAll("９","九");
		
		ps=ps.replaceAll("1","一");
		ps=ps.replaceAll("2","二");
		ps=ps.replaceAll("3","三");
		ps=ps.replaceAll("4","四");
		ps=ps.replaceAll("5","五");
		ps=ps.replaceAll("6","六");
		ps=ps.replaceAll("7","七");
		ps=ps.replaceAll("8","八");
		ps=ps.replaceAll("9","九");
		
		route=ps+ns;
	}
	private void adjustAdministrative() {
		if(administrative_area==null) return;
		
		administrative_area=administrative_area.trim().replaceAll(" ", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\(", "").replaceAll("\\)", "");
		for(int i=administrative_area.length()-1;i>=0;i--){
			if(administrative_area.charAt(i)<127){
				postal_code=""+administrative_area.charAt(i)+postal_code;
			}
		}
		administrative_area=administrative_area.replace(postal_code, "");
	}
	private void adjustSublocality() {
		if(sublocality==null) return;
		for(int i=0;i<sublocality.length()-1;i++){
			if(geocodedataList.contains(sublocality.substring(i))){
				if(route==null)route="";
				route=sublocality.substring(0,i)+route;
				sublocality=sublocality.substring(i);
				break;
			}
		}
	}
	private void adjustStreetNumber() {
		street_number=adjustNumber(street_number);
		street_number=detachStr(street_number);
	}
	
	private String adjustNumber(String str) {
		if(str==null)return null;
		//符號
		str=str.replaceAll("之", "-");
		str=str.replaceAll("－", "-");
		//全形數字
		str=str.replaceAll("０", "0");
		str=str.replaceAll("１", "1");
		str=str.replaceAll("２", "2");
		str=str.replaceAll("３", "3");
		str=str.replaceAll("４", "4");
		str=str.replaceAll("５", "5");
		str=str.replaceAll("６", "6");
		str=str.replaceAll("７", "7");
		str=str.replaceAll("８", "8");
		str=str.replaceAll("９", "9");
		//國字大寫
		str=str.replaceAll("零", "0");
		str=str.replaceAll("壹", "1");
		str=str.replaceAll("貳", "2");
		str=str.replaceAll("參", "3");
		str=str.replaceAll("肆", "4");
		str=str.replaceAll("伍", "5");
		str=str.replaceAll("陸", "6");
		str=str.replaceAll("柒", "7");
		str=str.replaceAll("捌", "8");
		str=str.replaceAll("玖", "9");
		
		str=str.replaceAll("○", "0");
		str=str.replaceAll("一", "1");
		str=str.replaceAll("二", "2");
		str=str.replaceAll("三", "3");
		str=str.replaceAll("四", "4");
		str=str.replaceAll("五", "5");
		str=str.replaceAll("六", "6");
		str=str.replaceAll("七", "7");
		str=str.replaceAll("八", "8");
		str=str.replaceAll("九", "9");
		//單位符號 (十,百)
		String[] strs={"仟","千","佰","百","拾","十"};
		int i=0;
		while(i<strs.length){
			String s=strs[i];
			int index=str.indexOf(s);
			if(index<0){
				i++;
				continue;
			}
			char pc = '無';
			if(index-1>-1){
				pc = str.charAt(index-1);
			}
			char nc ='無';
			if(str.length()>index+1){
				nc = str.charAt(index+1);
			}
			if((pc<='9' && pc>='0') && (nc<='9' && nc>='0')){
				str=str.replace(s, "");
				continue;
			}
			if(!(pc<='9' && pc>='0')&&!(nc<='9' && nc>='0')){
				if(s.equals("十")||s.equals("拾"))
					str=str.replace(s, "10");
				else if(s.equals("百")||s.equals("佰"))
					str=str.replace(s, "100");
				else if(s.equals("千")||s.equals("仟"))
					str=str.replace(s, "1000");
				else if(s.equals("廿"))
					str=str.replace(s, "20");
				continue;
			}
			if(!(pc<='9' && pc>='0')){
				str=str.replace(s, "1");
				if(s.equals("廿"))
					str=str.replace(s, "2");
				continue;
			}
			if(!(nc<='9' && nc>='0')){
				if(s.equals("十")||s.equals("拾"))
					str=str.replace(s, "0");
				else if(s.equals("百")||s.equals("佰"))
					str=str.replace(s, "00");
				else if(s.equals("千")||s.equals("仟"))
					str=str.replace(s, "000");
				continue;
			}
		}
		return str;
	}
	
	private void adjustWeight() {
		if(country!=null)weight+=32;
		if(administrative_area!=null)weight+=16;
		if(locality!=null)weight+=8;
		if(sublocality!=null)weight+=4;
		if(route!=null)weight+=2;
		if(street_number!=null)weight++;
		if(weight==32 && data.size()>0) route=data.get(0);
	}
	
	
	private void adjustCheckLevel() {
		checkLevel=0;
		if(administrative_area!=null)checkLevel=1;
		if(locality!=null)checkLevel=2;
		if(sublocality!=null)checkLevel=3;
		if(route!=null && checkLevel!=null && checkLevel>=2)checkLevel=4;
		if(street_number!=null && checkLevel!=null && checkLevel>=2)checkLevel=5;
	}
	
	@Override
	public String toString() {
		return getFormatAddress();
	}
	
	public Integer getIntFloor() {
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(floor);
		m.find();
		String numPart = m.group();
		if (floor.startsWith("B")) {
			numPart = "-" + numPart;
		}
		return Integer.parseInt(numPart);
	}
	
	public static void main(String[] args) throws IOException {
		//decodeHtmlEntity("嘉義縣中埔鄉和美村二九鄰中山新&#x90A8;四七六之一號");
		//String testAddress="臺中市南屯區位於鎮平里及豐樂里，鄰近永順路、黎明路與環中路。";
		//String testAddress="台北市仁愛路四段296號";
		//String testAddress="南投縣竹山鎮&#30808;&#30936;30936;里仁德段237號";
		System.out.println(TaiwanAddressNormalizeUtil.searchAddrPart("臺南市 / 台南市"));
		String testAddress="台北市蛋黃區仁愛路四段296號";
		Stopwatch sw = Stopwatch.createStarted();
		TaiwanAddressNormalizeUtil au=new TaiwanAddressNormalizeUtil(testAddress);
		System.out.println(au.getAdministrative_area());
		System.out.println(au.getLocality());
		System.out.println(au.getAddress());
		System.out.println(sw.stop().elapsed(TimeUnit.MILLISECONDS) + ":" + au.getFormatAddress());
	}
	
	public static class AddressFormatConfig implements Serializable {
		boolean withCountry;
		boolean withAdministrativeArea;
		boolean withLocality;
		boolean withSublocality;
		boolean withFloor;
		
		public AddressFormatConfig(boolean withCountry,
				boolean withAdministrativeArea, boolean withLocality,
				boolean withSublocality, boolean withFloor) {
			super();
			this.withCountry = withCountry;
			this.withAdministrativeArea = withAdministrativeArea;
			this.withLocality = withLocality;
			this.withSublocality = withSublocality;
			this.withFloor = withFloor;
		}
		
	}
}
