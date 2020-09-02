package com.intumit.solr.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.tika.detect.AutoDetectReader;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.velocity.app.VelocityEngine;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import com.google.common.collect.HashBiMap;
import com.intumit.smartwiki.WikiWord;
import com.intumit.solr.dataset.DataSet;
import com.intumit.solr.dataset.DataSetFacade;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.systemconfig.WiseSystemConfigFacade;
import com.runawaybits.html2markdown.HTML2Md;
import com.thoughtworks.xstream.XStream;

public class WiSeUtils {
	private static final Logger LOG = LoggerFactory.getLogger(WiSeUtils.class);
	
	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36";
	private static final String VELOCITY_ENGINE = "WISE-VELOCITY-ENGINE";

	public static String nl2br(String str) {
		return str != null ? str.replaceAll("\\r?\\n", "<BR/>") : null;
	}
	
	public static String br2nl(String str) {
		return str != null ? str.replaceAll("(?is)</?(BR|P)/?>", "\n") : null;
	}
	
	public static String html2md(String html) {
		try {
			html = HTML2Md.convertHtml(html, "UTF-8");
			html = html.replaceAll("\n", "\n\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return html;
	}

	public static String output(Object str, String prefix, String postfix,
			String defaultStr) {
		if (str == null || StringUtils.trimToNull(str.toString()) == null)
			return defaultStr;

		return prefix + str.toString() + postfix;
	}

	public static String outputQ(String str) {
		if (str == null || StringUtils.trimToNull(str.toString()) == null
				|| str.contains(":"))
			return "";

		return str;
	}

	public static void logKeyword(String str) {
		if (str == null || StringUtils.trimToNull(str.toString()) == null
				|| str.contains(":"))
			return;
		SearchKeywordLogFacade.getInstance().log(str);
	}

	private static zhcode zhTool;

	static TrustManager[] trustAllCerts = new TrustManager[] { 
		new X509TrustManager() {

			public java.security.cert.X509Certificate[] getAcceptedIssuers()

			{
	
				return null;
	
			}
	
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType)
	
			{
	
			}
	
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType)
	
			{
	
			}
		}
	};
	
	 /**
	 * Host name verifier that does not perform nay checks.
	 */
	static class NullHostnameVerifier implements HostnameVerifier {
	    public boolean verify(String hostname, SSLSession session) {
	        return true;
	    }
	}
	
	static {
		// 無視 SSL 憑證，也不管 Hostname DNS 對應
		// 一切都是為了能夠抓 https:// 的網頁
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
	}

	public static String getDataFromUrl(CookieManager cm, String query, String referer, String forceEncoding) throws IOException {
		StringBuilder builder = new StringBuilder();
		try {
			URL url = new URL(query);

			Proxy proxy = null;
			try {
				proxy = WiseSystemConfigFacade.getInstance().get().getProxy();
			} catch (Exception e) {
				LOG.info("Cannot get proxy setting form WiSeSystemConfig:" + e.getMessage());
			}
			URLConnection connection = proxy == null ? url.openConnection() : url.openConnection(proxy);
			
			connection.setRequestProperty("User-Agent", USER_AGENT);
			if (referer != null) connection.setRequestProperty("Referer", referer);
			connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8,zh-TW;q=0.6,zh;q=0.4");
			//connection.setRequestProperty("Accept-Charset", "utf-8");
			connection.setRequestProperty("Progma", "no-cache");
			if (cm != null) cm.setCookies(connection);

			String body = null;
			InputStream in = null;
			try {
				//connection.setConnectTimeout(6000);
				//connection.setReadTimeout(6000);
				in = connection.getInputStream();
				String encoding = connection.getContentEncoding();
				//System.out.println(connection.getHeaderFields());
				encoding = forceEncoding == null ? (encoding == null ? "UTF-8" : encoding) : forceEncoding;
				body = IOUtils.toString(in, encoding);
				if (cm != null) cm.storeCookies(connection);
			}
			catch (Exception ex) {
				in = ((HttpURLConnection)connection).getErrorStream();
				int code = ((HttpURLConnection)connection).getResponseCode();

				String encoding = connection.getContentEncoding();
				//System.out.println(connection.getHeaderFields());
				encoding = forceEncoding == null ? (encoding == null ? "UTF-8" : encoding) : forceEncoding;
				body = IOUtils.toString(in, encoding);
				
				GetUrlContentException guce = new GetUrlContentException(ex.getMessage());
				guce.setErrorStreamContent(body);
				guce.setErrorCode(code);
				throw guce;
			}
			return body;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return StringUtils.trimToNull(builder.toString());
	}

	public static String getDataFromUrl(String query, String forceEncoding) throws IOException {
		return getDataFromUrl(null, query, null, forceEncoding);
		
	}
	public static String getDataFromUrl(String query) throws IOException {
		return getDataFromUrl(query, null);
	}

	public static String getDataFromUrlIgnoringStatusCode(String query) {
		StringBuilder builder = new StringBuilder();
		try {
			URL url = new URL(query);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection
					.setRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-TW; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
			connection.setRequestProperty("Content-Type",
					"text/xml; charset:UTF-8;");
			connection.setRequestProperty("Accept-Language",
					"zh-tw,en-us;q=0.7,en;q=0.3");
			connection.setRequestProperty("Accept-Charset", "utf-8");

			connection.getResponseCode();
			InputStream stream = null;
			try{
			    stream = connection.getErrorStream();
			    if (stream == null) {
			        stream = connection.getInputStream();
			    }

				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			}finally{
				IOUtils.closeQuietly(stream);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return StringUtils.trimToNull(builder.toString());
	}

	public static String autoDetectCharset(InputStream is) throws IOException, TikaException {
        AutoDetectReader reader = new AutoDetectReader(is);
        Charset charSet = reader.getCharset();
        return charSet.toString();
    }
	
	public static String autoDetectContentType(InputStream is) {
		ContentHandler contenthandler = new BodyContentHandler();
		Metadata metadata = new Metadata();
		Parser parser = new AutoDetectParser();
		try {
			parser.parse(is, contenthandler, metadata, null);
		}
		catch (Exception e) {
			LOG.info("autoDetectContentType has some error:" + e.getMessage());
		}
		return metadata.get(Metadata.CONTENT_TYPE);
	}

	public static String post(String url, NamedList params) {
		return post(url, params, "UTF-8");
	}

	public static String post(String url, NamedList params, String charset) {
		return postWithPayload(url, params, null, charset);
	}
	
	public static String postJson(String query, JSONObject body, String charset) {
	    StringBuilder builder = new StringBuilder();
        try {
            URL url = new URL(query);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection
                    .setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-TW; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Language",
                    "zh-tw,en-us;q=0.7,en;q=0.3");
            connection.setRequestProperty("Accept-Charset", "utf-8");
            OutputStream dataOutputStream = connection.getOutputStream();
            dataOutputStream.write(body.toString().getBytes());
            dataOutputStream.flush();
            dataOutputStream.close();

            int code = connection.getResponseCode();
            InputStream stream = null;
            try{
                stream = connection.getErrorStream();
                if (stream == null) {
                    stream = connection.getInputStream();
                }
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, charset));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                if(code != 200) {
                    // TODO 錯誤處理
                    System.out.print(builder.toString());
                    builder.setLength(0);
                    builder = new StringBuilder();
                }
            }finally{
                IOUtils.closeQuietly(stream);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return StringUtils.trimToNull(builder.toString());
	}
	
	public static String postWithPayload(String url, NamedList params, byte[] payload, String charset) {
		HttpClient hc = new HttpClient();
		PostMethod method = new PostMethod(url);
		String result = null;

		if (params != null) {
			for (Iterator<Map.Entry> itr = params.iterator(); itr.hasNext();) {
				Map.Entry entry = itr.next();
	
				method.addParameter(entry.getKey().toString(), entry.getValue()
						.toString());
			}
		}
		
		if (payload != null)
			method.setRequestEntity(new ByteArrayRequestEntity(payload));

		try {
			method.getParams().setParameter(
					HttpMethodParams.HTTP_CONTENT_CHARSET, charset);
			int returnCode = hc.executeMethod(method);
			result = method.getResponseBodyAsString();
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (method != null) {
                try {
             	   method.releaseConnection();
                } catch (Exception e) {
                    //logger.error("-------> Release HTTP connection exception:", e);
                }
         }

         if (hc != null) {
                try {
                    ((SimpleHttpConnectionManager) hc.getHttpConnectionManager()).shutdown();
                } catch (Exception e) {
                    //logger.error("-------> Close HTTP connection exception:", e);
                }
                hc = null;
          }
		}

		return result;
	}
	
	public static Map<String, Object> getParameterMap(HttpServletRequest request) {
		Map<String, Object> payloadData = new HashMap<String, Object>();
		java.util.Enumeration enu = request.getParameterNames();
		while(enu.hasMoreElements()){
			String paramName = (String)enu.nextElement();
			//System.out.println( "paramName : " + paramName );
			payloadData.put( paramName, request.getParameter(paramName));
		}
		
		return payloadData;
	}
	
	public static Map<String, List<String>> splitUrlQueryParams(String queryPairStr) throws UnsupportedEncodingException {
		final Map<String, List<String>> queryPairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = queryPairStr.split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!queryPairs.containsKey(key)) {
				queryPairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			queryPairs.get(key).add(value);
		}
		return queryPairs;
	}
	
	public static String getPayload(HttpServletRequest request) {
		try {
			String payload = IOUtils.toString(request.getInputStream(), "UTF-8");
			LOG.debug("Got payload: " + payload);
			
			return payload;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String getParameterJson(Map<String,String[]> map) {
		JSONObject jsonObject = new JSONObject();
		try {
			for(String key:map.keySet()) {
				String[] datas = map.get(key);
				JSONArray jsonArray = new JSONArray();
				for(String data:datas)
					jsonArray.put(data);
				jsonObject.put(key, jsonArray);
			}
			return StringEscapeUtils.unescapeJava(jsonObject.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static NamedList getDataAndDateFromUrl(String query) {
		NamedList namedList = new NamedList();
		StringBuilder builder = new StringBuilder();
		long date = 0L;
		try {
			URL url = new URL(query);
			URLConnection connection = url.openConnection();
			connection
					.setRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-TW; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
			connection.setRequestProperty("Content-Type",
					"text/xml; charset:UTF-8;");
			connection.setRequestProperty("Accept-Language",
					"zh-tw,en-us;q=0.7,en;q=0.3");
			connection.setRequestProperty("Accept-Charset", "utf-8");
			date = connection.getDate();

			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream(), "UTF-8"));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			namedList.add("content", builder.toString());
			namedList.add("date", new Long(date));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return namedList;
	}

	private static String getParam(Map<String, String[]> params, String pn) {
		if (params.containsKey(pn)) {
			String[] values = params.get(pn);
			
			if (values == null || values.length == 0) {
				return null;
			}
			
			return values[0];
		}
		
		return null;
	}

	public static SolrQuery parseUrlSearchParameters(DataSet ds, boolean isTargetDataSet, Map<String, String[]> params, String additionalFilterQuery) {
		String qqq = getParam(params, "q");
		if (StringUtils.isEmpty(qqq))
			qqq = "*:*";
		
		String qt = "/browse";
		
		//判斷參數是否非數字
		int start=0;
		String preStart=getParam(params, "start")==null?"0":getParam(params, "start");
		if(StringUtils.isNumeric(preStart)){
			start= Integer.parseInt(preStart);
		}
		if(start<0){
			start=0;
		}
		
		int facetLimit = getParam(params, "facet.limit") == null ? 10
				: Integer.parseInt(getParam(params, "facet.limit"));
		String sortStr = getParam(params, "sort") == null ? "Date_dt desc"
				: getParam(params, "sort");
		
		//為了判斷使用者輸入是否隨意輸入排序條件，若有新的排序請加入判斷
		//if(sort.indexOf("Date_dt")!=-1){
	    //	sort="Date_dt desc";
	    //}else{
	    //	sort="score desc";
	    //}
		
		String[] filterQuery = params.get("fq");
		
		
		SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;
		String qf = "Body_mt";
		String highlightField = "Body_mt";
		
		String[] facetField = (ds == null || StringUtils.isEmpty(ds.getFacets())) ? new String[0] : ds.getFacets().split(",");
		List<String> drillDownFacetField = Arrays.asList(new String[0]);
		for (int i=0; i < facetField.length; i++) {
			String ff = facetField[i];
			if (drillDownFacetField.contains(ff)) {
				facetField[i] = "{!ex=" + facetField[i] + "}" + facetField[i];
			}
		}
		
		String[] dsFilters = (ds == null || StringUtils.isEmpty(ds.getFilters())) ? new String[0] : ds.getFilters().split(",");
		if (ds != null && StringUtils.isNotEmpty(ds.getFieldWeight()))
			qf = ds.getFieldWeight();
		if (ds != null && StringUtils.isNotEmpty(ds.getFieldHighlight()))
			highlightField = ds.getFieldHighlight();
		
		String[] facetQuery = new String[0];
		SolrQuery.SortClause sortClause = SolrQuery.SortClause.desc("Date_dt");

		if (sortStr.indexOf(" ") != -1) {
			sortClause = SolrQuery.SortClause.create(StringUtils.substringBefore(sortStr, " "), StringUtils.substringAfter(sortStr, " "));
		}
		
		//使用者可以在前端自行輸入一頁顯示筆數，這邊判斷如果跟系統預設不一樣就以使用者輸入為主
		int rowSize=getParam(params, "rowSize")==null?25:Integer.parseInt(getParam(params, "rowSize"));
		SolrQuery solrQ = new SolrQuery();
		solrQ.setQuery(qqq)
				.setRequestHandler(qt)
				.setParam("qf", qf)
				.setFacet(true)
				.setFacetMinCount(1)
				.setHighlight(true)
				.setSort(sortClause)
				.setRows(rowSize)
				.setStart(start);
		
		if (additionalFilterQuery != null)
			solrQ.addFilterQuery(additionalFilterQuery);
		
		if (isTargetDataSet) {
			// 在找價錢的時候要先把 filter query 加上 Filter Query
			Set<String> set = new LinkedHashSet<String>();
			for (int jjj = 0; filterQuery != null
					&& jjj < filterQuery.length; jjj++) {
				solrQ.addFilterQuery(filterQuery[jjj].trim());
			}
			
			for (int jjj = 0; dsFilters != null
					&& jjj < dsFilters.length; jjj++) {
				solrQ.addFilterQuery("{!tag=DF}" + dsFilters[jjj].trim());
			}
            
			com.intumit.solr.dataset.DataSet.FacetQuery[] dsFQs = ds.getFQ();//ds != null ? ds.getFQ() : null;
            for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
            	List<String>fqs = dsFQs[jjj].facetQuries();
            	
            	for (String currFQ : fqs) {
            		solrQ.addFacetQuery(currFQ);
            	}
            }
			
			// 只有目前主要的 core 才要查詢維度等資訊
			for (int jjj = 0; jjj < facetField.length; jjj++) {
				solrQ.addFacetField(facetField[jjj]);
			}
			
			for (int jjj = 0; jjj < facetQuery.length; jjj++) {
				if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(facetQuery[jjj])))
					solrQ.addFacetQuery(StringUtils.trimToEmpty(facetQuery[jjj].trim()));
			}
			
			solrQ
					.setSort(sortClause)
					.setParam("hl.fl", highlightField)
					.setParam("hl.usePhraseHighlighter", true)
					.setParam("hl.fragsize",System.getProperty("searchresult_descmax"))
					.setFacetLimit(facetLimit);
		}
		else {
			solrQ.setRows(1);

			for (int jjj = 0; dsFilters != null
					&& jjj < dsFilters.length; jjj++) {
				solrQ.addFilterQuery("{!tag=DF}" + dsFilters[jjj].trim());
			}
		}
		
		return solrQ;
	}
	
	public static TreeSet<Map.Entry<String, Integer>> getSortedTreeSet() {
		TreeSet<Map.Entry<String, Integer>> sorted = new TreeSet<Map.Entry<String, Integer>>(
				new Comparator() {
					@Override
					public int compare(Object o1, Object o2) {
						Map.Entry<String, Integer> e1 = (Map.Entry<String, Integer>) o1;
						Map.Entry<String, Integer> e2 = (Map.Entry<String, Integer>) o2;
						int comp = e2.getValue().compareTo(e1.getValue());

						return comp == 0 ? 1 : comp;
					}
				});

		return sorted;
	}

	static DateTimeFormatter sdf2 = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.000'Z'").withZone(DateTimeZone.forID("Asia/Taipei"));

	public static String toSolrDateStr(DateTime dateTime) {
		return dateTime.toString(sdf2);
	}
	public static String toSolrDateStr(Calendar cal) {
		return new DateTime(cal).toString(sdf2);
	}

	public static DateTime fromSolrDateStr(String dateStr)
			throws ParseException {
		DateTime dateTime = sdf2.parseDateTime(dateStr);
		return dateTime;
	}

	public static String cn2tw(String cnStr) {
		if (zhTool == null)
			zhTool = new zhcode();

		return zhTool.convertString(cnStr, zhcode.GBK, zhcode.UTF8T);
	}

	public static String tw2cn(String twStr) {
		if (zhTool == null)
			zhTool = new zhcode();

		return zhTool.convertString(twStr, zhcode.UTF8T, zhcode.UTF8S);
	}

	public synchronized static String listWikiWordXml(List<WikiWord> result) {
		return out().toXML(result);
	}

	private static XStream out() {
		XStream out = new XStream();
		out.alias("wikiWord", WikiWord.class);
		return out;
	}

	public static String dblQuote(String str) {
		if (StringUtils.isNotEmpty(str))
			return '"' + StringUtils.strip(str, "\"") + '"';
		return str;
	}

	public static VelocityEngine getVelocityEngine(HttpServletRequest req) {

		ServletContext servletContext = req.getServletContext();


		synchronized (servletContext) {
			if (servletContext.getAttribute(VELOCITY_ENGINE) == null) {
				servletContext.setAttribute(VELOCITY_ENGINE,
						createVelocityEngine(servletContext));
			}
			return (VelocityEngine) servletContext
					.getAttribute(VELOCITY_ENGINE);
		}
	}

	private static VelocityEngine createVelocityEngine(ServletContext servletContext) {
		try {
			VelocityEngine result = new VelocityEngine();

			try {
				Properties velocityProperty = new Properties();
				InputStream in = WiSeUtils.class.getResourceAsStream("/velocity.properties");
				velocityProperty.load(in);
				in.close();
				result.setApplicationAttribute("javax.servlet.ServletContext", servletContext);
				result.init(velocityProperty);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String getFromCookie(String name, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	public static String setCookie(String name, String value, int maxAge,
			String path, HttpServletResponse response) {
		if (name == null)
			return null;
		if (maxAge <= 0)
			maxAge = 3600;
		if (path == null || path.length() == 0)
			path = "/";
		if (response == null)
			return null;

		Cookie nameCookie = new Cookie(name, value);
		nameCookie.setMaxAge(maxAge);
		nameCookie.setPath(path);
		response.addCookie(nameCookie);

		return value;
	}

	public static void deleteCookie(String name, String path, HttpServletResponse response) {
		if (name == null)
			return;
		if (path == null || path.length() == 0)
			path = "/";
		if (response == null)
			return;

		Cookie nameCookie = new Cookie(name, "");
		nameCookie.setMaxAge(0);
		nameCookie.setPath(path);
		response.addCookie(nameCookie);

		return;
	}

	public static String sha256(String toBeEncrypt) {
		MessageDigest sha = null;

		try {
			sha = MessageDigest.getInstance("SHA-256"); // 要用 SHA-1 就改成 SHA-1
			sha.update(toBeEncrypt.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		return byte2hex(sha.digest());
	}

	public static String sha1(String toBeEncrypt) {
		MessageDigest sha = null;

		try {
			sha = MessageDigest.getInstance("SHA-1"); // 要用 SHA-1 就改成 SHA-1
			sha.update(toBeEncrypt.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		return byte2hex(sha.digest());
	}

	private static String byte2hex(byte[] b) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
			if (stmp.length() == 1) hs = hs + "0" + stmp;
			else hs = hs + stmp;
		}
		return hs.toUpperCase();
	}


	public static final List<String> availableTimeRanges = Arrays.asList( new String[] {"today", "yesterday", "past24hr", "thisWeek", "lastWeek", "thisMonth", "lastMonath"} );
	public static final List<String> availableTimeRangeNames = Arrays.asList( new String[] {"今日", "昨日", "過去24小時", "本週", "上週", "本月", "上個月"} );

	public static Date[] getTimeRangePair(String timeRange) {
		Date afterDate = null;
		Date beforeDate = null;

		if ("today".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
			beforeDate = first.getTime();
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
			afterDate = first.getTime();
		}
		else if ("yesterday".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
			first.add(Calendar.DATE, -1);
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
			afterDate = first.getTime();
			first.add(Calendar.DATE, 1);
			first.add(Calendar.SECOND, -1);
			beforeDate = first.getTime();
		}
		else if ("past24hr".equals(timeRange)) {
			Calendar first = Calendar.getInstance();
			beforeDate = first.getTime();
			first.add(Calendar.DATE, -1);
			afterDate = first.getTime();
		}
		else if ("thisWeek".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
			beforeDate = first.getTime();
		    first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
		    afterDate = first.getTime();
		}
		else if ("lastWeek".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
		    first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
		    beforeDate = first.getTime();
		    first.add(Calendar.DATE, -7);
		    afterDate = first.getTime();
		}
		else if ("thisMonth".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
			beforeDate = first.getTime();
		    first.set(Calendar.DAY_OF_MONTH, 1);
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
		    afterDate = first.getTime();
		}
		else if ("lastMonth".equals(timeRange)) {
		    Calendar first = Calendar.getInstance();
		    first.set(Calendar.DAY_OF_MONTH, 1);
		    first.set(Calendar.HOUR_OF_DAY, 0);
		    first.set(Calendar.MINUTE, 0);
		    first.set(Calendar.SECOND, 0);
		    first.add(Calendar.MONTH, -1);
		    afterDate = first.getTime();
		    first.add(Calendar.MONTH, 1);
		    first.add(Calendar.SECOND, -1);
		    beforeDate = first.getTime();
		}

		return new Date[] {afterDate, beforeDate};
	}

	public static boolean containsAny(String alt, Collection<String> matchs) {
		if(matchs != null && !matchs.isEmpty()) {
			for(String match : matchs) {
				if(alt.contains(match)) return true;
			}
		}
		return false;
	}
	
	/**
	 * 查詢距離某個指定的中心點（centerPoint）方圓幾公里（distance）內的物件
	 * 
	 * @param server SolrServer
	 * @param baseQuery 可以提供預先設定好的 query，或者給 null 也沒關係
	 * @param latLonFieldName 索引中 location 欄位名稱
	 * @param centerPoint 欲查詢的「中心點」經緯度，是字串，如 "25.054373,121.557672" 這樣的格式（不含雙引號）
	 * @param distance 查詢距 centerPoint 方圓多少「公里」範圍內的資料，可含小數點
	 * @param start 分頁機制，從哪一筆開始（由零開始計算）
	 * @param rows 分頁開始，回傳多少筆資料
	 * @return
	 * @throws SolrServerException
	 */
	public static SolrDocumentList geoSearch(SolrServer server, SolrQuery baseQuery, String latLonFieldName, String centerPoint, float distance, int start, int rows) throws SolrServerException {
		if (baseQuery == null) {
			baseQuery = new SolrQuery();
		}

		baseQuery.setRequestHandler("/browse");
		baseQuery.setStart(start);
		baseQuery.setRows(rows);
		
		baseQuery.addFilterQuery("{!geofilt}");
		baseQuery.setParam("sfield", latLonFieldName);
		baseQuery.setParam("pt", centerPoint);
		baseQuery.setParam("d", "" + distance);
		baseQuery.setSort("geodist()", SolrQuery.ORDER.asc);  // 照距離 近 到 遠 排序
		
		LOG.debug("geoSearch query:" + baseQuery);
		
		return server.query(baseQuery).getResults();
	}
	

    public static String aesEncrypt(String key, String initVector, String value, Boolean... useUrlEncode) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            String base64 = Base64.encodeBase64String(encrypted);
    		
    		LOG.debug("aesEncrypt:" + base64);
    		
    		if (useUrlEncode != null && useUrlEncode.length > 0 && useUrlEncode[0]) {
    			return URLEncoder.encode(base64, "UTF-8");
    		}

            return base64;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String aesDecrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

	public static class SimplePagination {
		int rows = 10;
		int pageGap = 5;
		long total = 0;
		String baseUrl = "#";
		
		public SimplePagination(int rows, long total, int pageGap, String baseUrl) {
			this.rows = rows;
			this.total = total;
			this.pageGap = pageGap;
			this.baseUrl = baseUrl;
		}
		
		public String makeUrl(int start, int rows) {
			return baseUrl + "&start=" + start + "&rows=" + rows;
		}

		public String toHtml(int start) {
			
			return toHtml(start, false);
		}
		public String toHtml(int start, boolean printBrief) {
			StringBuffer buf = new StringBuffer();
			buf.append("<div><div class='btn-group'>");
			int JUMP_N_PAGE = pageGap;

			int currPage = (int) Math.floor(start / rows) + 1;
			if (currPage > 1) {
				buf.append("<A class='btn btn-default'  HREF='" + makeUrl(0, rows) + "'>");
				buf.append("&nbsp;<span class='glyphicon glyphicon-fast-backward'>");
				buf.append("</span>");
				buf.append("</A>");
			}
			int prevNPageStart = start - rows * JUMP_N_PAGE;

			if (prevNPageStart >= 0 && prevNPageStart < total) {
				buf.append("<A class='btn btn-default' HREF='" + makeUrl(prevNPageStart, rows) + "'>");
				buf.append("<span class='glyphicon glyphicon-step-backward'>");
				buf.append("</span>");
				buf.append("&nbsp;</A>");
			}
			for (int po = -4; po <= 4; po++) {
				int nStart = start + rows * po;
				if (nStart < 0 || nStart >= total) continue;

				if (po == 0) {
					buf.append("<a class='btn btn-default active' href='#'>" + (currPage + po) + "</a>");
					continue;
				}
				buf.append("<A class='btn btn-default'  HREF='" + makeUrl(nStart, rows) + "'>" + (currPage + po)
						+ "</A>");
			}
			int nextNPageStart = start + rows * JUMP_N_PAGE;

			if (nextNPageStart >= 0 && nextNPageStart < total) {
				buf.append("<A class='btn btn-default' HREF='" + makeUrl(nextNPageStart, rows) + "'>");
				buf.append("<span class='glyphicon glyphicon-step-forward'>");
				buf.append("</span>");
				buf.append("&nbsp;</A>");
			}
			int lastPage = (int) Math.floor((total - 1) / rows) + 1;
			if (lastPage != currPage) {
				buf.append("<A class='btn btn-default' HREF='" + makeUrl((lastPage - 1) * rows, rows) + "'>");
				buf.append("<span class='glyphicon glyphicon-fast-forward'>");
				buf.append("</span>");
				buf.append("&nbsp;</A>");
			}
			buf.append("</div>");
			
			if (printBrief) {
				buf.append("&nbsp;&nbsp; " + (start+1) + "-" + Math.min(total, start+rows) + " of " + total + " records");
			}
			buf.append("</div>");
			return buf.toString();
		}
	}
	
	public static List<String> getNamedGroupCandidates(String regex) {
        List<String> namedGroups = new ArrayList<String>();
        Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regex);

        while (m.find()) {
            namedGroups.add(m.group(1));
        }

        return namedGroups;
    }
	
	public static Map<String, List<String>> splitQuery(String dataStr) {
		if (StringUtils.indexOfAny(dataStr, "&=") == -1) return null;
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = dataStr.split("&");
		try {
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
				query_pairs.get(key).add(value);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return query_pairs;
	}
	
	public static ArrayList<String> whiteList(ArrayList<String> whiteList) throws IOException {
		String whiteIp = null;
		
		// 本機用
		//FileReader ipFile = new FileReader("D://Intumit/whiteList.txt");
		// 本機(啟動參數需新增-DwhiteList)、測試機及花旗環境用(WebSphere JVM需新增-DwhiteList)
		FileReader ipFile = new FileReader(Paths.get(System.getProperty("whiteList")).toFile());
		
		BufferedReader br = new BufferedReader(ipFile);
		
		while (br.ready()) {
			whiteIp = br.readLine();
			}
		
		String[] split_line = whiteIp.split(",");
		
		for (String a: split_line) {
			whiteList.add(a);
			}
		
		ipFile.close();
		return whiteList;
	}
	
	public static String stGuest(String remoteGuest) {
		String[] guest = remoteGuest.split("\\.");
		StringBuilder stGuest = new StringBuilder();
		
		for(int i = 0; i < guest.length ; i++  ){
			if(i == 0 ){
				stGuest.append(guest[i].toString() + ".");
			}else if (i == 1){
				stGuest.append(guest[i].toString());
			}
		}
		
		return stGuest.toString();
	}

	public static Map<String, Integer> getAdvStat(int dsID) {
		Map<String, Integer> result = new HashMap<String, Integer> ();
		DataSet ds = DataSetFacade.getInstance().get(dsID);  
		String[] df = {"matchedQuestion"};
		String[] num = {"1000000"};
		Map<String, String[]> params = new HashMap<>();
		params.put("df", df);
		params.put("num", num);
		
		try {
			SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, params, null);
			Set<String> blackFF = new HashSet<String>();
			HashBiMap<String, String> ffToFq = HashBiMap.create(); 
			String ffName = "MatchedQuestion_s";
			String tagPrefix = null;
			int dataNum = 1000000;
			SolrServer mServer = Tenant.get(1).getStatisticsSolrServer();
			
			SolrQuery secondLevelQueryBase = multiCoreQ.getCopy();

			multiCoreQ.addFacetField(ffName);
			multiCoreQ.setRows(0);
			multiCoreQ
					.setFacetLimit(dataNum).setFacetMinCount(1)
					.setParam("f.QuestionType_s.facet.limit", "" + dataNum)
					.setParam("f.AnswerType_s.facet.limit", "" + dataNum)
					.setParam("f.QuestionKeyword_ms.facet.limit", "" + dataNum)
					.setParam("f.AnswerCategory_s.facet.limit", "" + dataNum);
			
			String mainKey = multiCoreQ.getQuery();
			blackFF.add(mainKey);
			
			//System.out.println("*****" + multiCoreQ);
			QueryResponse mainRsp = mServer.query(multiCoreQ);
			JSONArray yAxisFqDataArr = new JSONArray();
			Map<String, Integer> dataMap = new HashMap<String, Integer>();

			int id = 1;
			FacetField ff = mainRsp.getFacetField(ffName);
			List<FacetField.Count> ffVal = ff.getValues();
			
			for (FacetField.Count c: ffVal) {
				dataMap.put(c.getName(), (int)c.getCount());
				ffToFq.put(c.getName(), c.getAsFilterQuery());
			}
			
			SortedSet<Map.Entry<String, Integer>> sortedset = new TreeSet<Map.Entry<String, Integer>>(
		            new Comparator<Map.Entry<String, Integer>>() {
		                public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
		                		int result = e2.getValue().compareTo(e1.getValue());
		                    return result == 0 ? 1 : result;
		                }
		            });

		  	sortedset.addAll(dataMap.entrySet());
			
			for (Map.Entry<String, Integer> c: sortedset) {
				String tagName = c.getKey();
				System.out.println(tagName);
				
				if (blackFF.contains(tagName))
					continue; 
				
				JSONObject obj = new JSONObject();
				int tagCount = (int)c.getValue();
				obj.put("name", tagName);
				result.put(tagName, tagCount);
				
				if (yAxisFqDataArr.length() == dataNum) {
					break;
				}
			}

			return result;
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
}
