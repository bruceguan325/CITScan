package com.intumit.solr.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * 交通部（MoTC）所建置的「公共運輸整合資訊流通服務平臺」(Public Transport Data eXchange，PTX)
 * 裡頭可以抓取許多交通的數據，但需要申請 API KEY 並且透過加密方式呼叫
 * 所以將範例程式建立成工具來用
 * 有哪些 API 可以用，請參考 https://ptx.transportdata.tw/PTX
 * 
 * @author herb
 */
public class PTXMotcUtil {
	public static void main(String[] args) {
		String data = call("http://ptx.transportdata.tw/MOTC/v2/Rail/THSR/Station?$top=30&$format=json",
				"PUT-YOUR-APP-ID-HERE",
				"PUT-YOUR-APP-SECRET-HERE"
				);
		
		System.out.println(data);
	}

	public static String call(String apiUri, String appId, String appKey) {
		HttpURLConnection connection = null;
		String APIUrl = apiUri;//"http://ptx.transportdata.tw/MOTC/v2/Rail/THSR/Station?$top=30&$format=json";
		//申請的APPID
		String APPID = appId;//"FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
		//申請的APPKey
		String APPKey = appKey;//"FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";

		//取得當下的UTC時間，Java8有提供時間格式DateTimeFormatter.RFC_1123_DATE_TIME
		//但是格式與C#有一點不同，所以只能自行定義
		String xdate = getServerTime();
		String SignDate = "x-date: " + xdate;

		String Signature = "";
		try {
			//取得加密簽章
			Signature = hmacSha1Signature(SignDate, APPKey);
		}
		catch (SignatureException e1) {
			e1.printStackTrace();
		}

		//System.out.println("Signature :" + Signature);
		String sAuth = "hmac username=\"" + APPID + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"" + Signature + "\"";
		//System.out.println(sAuth);
		try {
			URL url = new URL(APIUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", sAuth);
			connection.setRequestProperty("x-date", xdate);
			connection.setRequestProperty("Accept-Encoding", "gzip");
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//將InputStream轉換為Byte
			InputStream inputStream = connection.getInputStream();
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int bytesRead = 0;
			while ((bytesRead = inputStream.read(buff)) != -1) {
				bao.write(buff, 0, bytesRead);
			}

			//解開GZIP
			ByteArrayInputStream bais = new ByteArrayInputStream(bao.toByteArray());
			GZIPInputStream gzis = new GZIPInputStream(bais);
			InputStreamReader reader = new InputStreamReader(gzis);
			BufferedReader in = new BufferedReader(reader);

			//讀取回傳資料
			String line, response = "";
			while ((line = in.readLine()) != null) {
				response += (line + "\n");
			}

			//System.out.println(response);
			return response;
		}
		catch (ProtocolException e) {
			e.printStackTrace();
		}

		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	static String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}
	
	static String hmacSha1Signature(String xData, String AppKey) throws java.security.SignatureException {
		try {
			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(AppKey.getBytes("UTF-8"),"HmacSHA1");

			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(xData.getBytes("UTF-8"));
			String result = Base64.encodeBase64String(rawHmac);
			return result;

		} catch (Exception e) {
			throw new SignatureException("Failed to generate HMAC : "+ e.getMessage());
		}
	}
}
