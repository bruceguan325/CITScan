package org.apache.solr.handler.dataimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataSource;
import org.apache.solr.handler.dataimport.EntityProcessorBase;

import com.intumit.message.MessageUtil;
import com.intumit.solr.util.WiSeUtils;

/**
 * 直接透過 HTTP request 向 Google 要求 result（本應透過 Google AJAX API，但筆數有限制... 連百筆都不給，故放棄）
 * 指定 site，便會抓取該 site 的 URL
 * 指定 contentExtractionSite，透過該 site 的 url 參數，來作內容擷取
 * 可參考 conf/data-config-google-web-search.xml 的設定
 * @author Herb
 */
public class GoogleWebSearchEntityProcessor extends EntityProcessorBase {
	public static final String URL = "URL";
	public static final String TITLE = "TITLE";
	public static final String HTMLCONTENT = "HTMLContent";
	public static final String EXTRACT_CONTENT = "EXTRACTED";
	public static final String UNIXTIME = "UnixTime";
	public static final String DEFAULT_CONTENT_EXTRACTOR_URL = "http://219.84.175.196:7036/cgi-bin/doExtract.pl";

	private DataSource dataSource;
	private String site;
	private String contentExtractionSite;
	private int limit = 100;

	@SuppressWarnings("unchecked")
	public void init(Context context) {
		super.init(context);
		site = context.getEntityAttribute("site");
		contentExtractionSite = context.getEntityAttribute("contentExtractor");
		String limitStr = context.getEntityAttribute("limit");
		if (limitStr != null) {
			limit = Integer.parseInt(limitStr);
		}
		dataSource = context.getDataSource();
		rowIterator = null;

	}

	public Map<String, Object> nextRow() {
		if (rowIterator != null)
			return getAndApplyTrans();

		List<Map<String, Object>> fileDetails = new ArrayList<Map<String, Object>>();

		try {
			String urlStr = "http://www.google.com.tw/search?hl=zh-TW&q="
					+ URLEncoder.encode("site:" + site, "UTF-8");
			getURLDetail(urlStr, fileDetails);
			rowIterator = fileDetails.iterator();
			System.out.println("Got " + fileDetails.size()
					+ " urls to be [getAndApplyTrans].");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return getAndApplyTrans();

	}

	void getURLDetail(String urlStr, List<Map<String, Object>> fileDetails) {

		String googleStr = "(?s)<li [^>]*>.*?<a href=\"(.*?)\".*?>(.*?)</a>.*?(?=(<li)|(</ol>))";
		Pattern googlePattern = Pattern.compile(googleStr);
		String googleTotalStr = "(?s)"+MessageUtil.getMessage(Locale.getDefault(), "total.about")+" <b>(.*?)</b> "+MessageUtil.getMessage(Locale.getDefault(), "number");
		Pattern googleTotalPattern = Pattern.compile(googleTotalStr);

		int start = 0;
		int total = 0;
		int countOfPrevPage = 0;
		int numOfSameCountOccur = 0;
		do {
			try {
				String content = WiSeUtils.getDataFromUrl(urlStr + "&start=" + start);

				System.out.println(start + "*****");
				Matcher mTotal = googleTotalPattern.matcher(content);

				if (mTotal.find()) {
					String totalStr = mTotal.group(1);
					System.out.println(totalStr);
					totalStr = totalStr.replaceAll(",", "");
					total = Integer.parseInt(totalStr);
				}

				Matcher mList = googlePattern.matcher(content);

				while (mList.find()) {
					start++;
					Map<String, Object> details = new HashMap<String, Object>();
					String url = mList.group(1);
					details.put(URL, url);
					details.put(TITLE, mList.group(2));
					if (context.getEntityAttribute("pk") != null)
						details.put(context.getEntityAttribute("pk"), url);
					
					fileDetails.add(details);
				}

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (start == countOfPrevPage)
				numOfSameCountOccur++;
			else
				numOfSameCountOccur = 0;
				
			countOfPrevPage = start;
		} while (start < total && start < limit && numOfSameCountOccur <= 5);
	}

	private Map<String, Object> getAndApplyTrans() {
		while (true) {
			Map<String, Object> r = getNext();
			if (r == null)
				return null;

			Map<String, Object> newRow = cloneMap(r);

			if (context.currentProcess() != Context.FIND_DELTA) {
				try {
					String url = (String) newRow.get(URL);
					NamedList objs = WiSeUtils.getDataAndDateFromUrl(url);
					String data = (String)objs.get("content");
					Long date = (Long)objs.get("date");
					
					if (data != null && date != null) {
						newRow.put(HTMLCONTENT, data);
						newRow.put(UNIXTIME, date);
					}
					
					if (contentExtractionSite != null) {
						String q = contentExtractionSite + "?url=" + URLEncoder.encode(url, "UTF-8");
						
						String ec = WiSeUtils.getDataFromUrl(q);
						
						if (ec != null) {
							newRow.put(EXTRACT_CONTENT, ec);
						}
					}

				} catch (RuntimeException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (newRow != null)
				return newRow;
		}
	}

	private Map<String, Object> cloneMap(Map<String, Object> row) {
		HashMap<String, Object> hm = new HashMap<String, Object>();

		for (Iterator<Map.Entry<String, Object>> itr = row.entrySet()
				.iterator(); itr.hasNext();) {
			Map.Entry<String, Object> entry = itr.next();
			hm.put(entry.getKey(), entry.getValue());
		}

		return hm;
	}
}
