/**
 Copyright (c) Microsoft Corporation
 All rights reserved.
 MIT License
 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the ""Software""), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.microsoft.ttshttpoxford.sample;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.microsoft.util.HttpsConnection;

public class TTSService {

	private static String CACHE_DIR = null;

	private static String WEB_CACHE_DIR = null;

	private static String ttsServiceUri = "https://speech.platform.bing.com/synthesize";

	private static OxfordAuthentication auth = null; // It will auto renew
													 // expired token, so just
													 // need one instance.

	static Map<HitHotLocale, String[]> ttsSettings = new HashMap<>();
	static {
		ttsSettings.put(HitHotLocale.zh_TW, new String[] {"zh-TW", Gender.Female, "Microsoft Server Speech Text to Speech Voice (zh-TW, Yating, Apollo)"});
		ttsSettings.put(HitHotLocale.ja_JP, new String[] {"ja-JP", Gender.Female, "Microsoft Server Speech Text to Speech Voice (ja-JP, Ayumi, Apollo)"});
		ttsSettings.put(HitHotLocale.en_US, new String[] {"en-US", Gender.Female, "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)"});
	}

	public static String[] getMicrosoftTtsSetting(HitHotLocale locale) {
		return ttsSettings.get(locale);
	}
	
	public static String getCacheFileName(String textToSynthesize, String lang, String genderName) {
		String key = WiSeUtils.sha256(textToSynthesize);
		return key + "-" + lang + "-" + genderName;
	}
	
	/**
	 * Synthesize the voice through the specified parameters.
	 */
	public static byte[] Synthesize(String textToSynthesize, String outputFormat, String lang, String genderName,
			String voiceName) throws Exception {
		if (CACHE_DIR == null) {
			CACHE_DIR = WiSeEnv.getHomePath() + "/cache/tts/";

			File cacheDir = new File(CACHE_DIR);

			if (!cacheDir.exists()) cacheDir.mkdirs();
		}

		String key = getCacheFileName(textToSynthesize, lang, genderName);
		byte[] audioBuffer = getFromCache(key);

		if (audioBuffer == null) {

			// Note: Sign up at http://www.projectoxford.ai for the client
			// credentials.
			if (auth == null) {
				String microsoftSttKey = System.getProperty("microsoft.tts.key");

				if (microsoftSttKey == null) {
					System.err
							.println("Cannot find Microsoft TTS apikey, please set java jvm args 'microsoft.tts.key'");
				}
				else {
					auth = new OxfordAuthentication("intumit_tts", microsoftSttKey);
				}
			}

			String token = auth.GetAccessToken();
			HttpsURLConnection webRequest = HttpsConnection.getHttpsConnection(ttsServiceUri);
			webRequest.setDoInput(true);
			webRequest.setDoOutput(true);
			webRequest.setConnectTimeout(5000);
			webRequest.setReadTimeout(15000);
			webRequest.setRequestMethod("POST");

			webRequest.setRequestProperty("Content-Type", "application/ssml+xml");
			webRequest.setRequestProperty("X-Microsoft-OutputFormat", outputFormat);
			webRequest.setRequestProperty("Authorization", "Bearer " + token);
			webRequest.setRequestProperty("X-Search-AppId", "07D3234E49CE426DAA29772419F436CA");
			webRequest.setRequestProperty("X-Search-ClientID", "1ECFAE91408841A480F00935DC390960");
			webRequest.setRequestProperty("User-Agent", "TTSAndroid");
			webRequest.setRequestProperty("Accept", "*/*");

			String SsmlTemplate = "<speak version='1.0' xml:lang='en-us'><voice xml:lang='%s' xml:gender='%s' name='%s'>%s</voice></speak>";
			String body = String.format(SsmlTemplate, lang, genderName, voiceName,
					StringEscapeUtils.escapeXml(textToSynthesize));
			System.out.println(body);

			byte[] bytes = body.getBytes();
			webRequest.setRequestProperty("content-length", String.valueOf(bytes.length));
			webRequest.connect();

			DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
			dop.write(bytes);
			dop.flush();
			dop.close();

			if (webRequest.getResponseCode() == 200) {
				InputStream inSt = webRequest.getInputStream();
				ByteArray ba = new ByteArray();

				int rn2 = 0;
				int bufferLength = 4096;
				byte[] buf2 = new byte[bufferLength];

				while ((rn2 = inSt.read(buf2, 0, bufferLength)) > 0) {
					ba.cat(buf2, 0, rn2);
				}

				inSt.close();
				webRequest.disconnect();
				audioBuffer = ba.getArray();
			}
			else {
				System.out.println("Error code (" + webRequest.getResponseCode() + ") :"
						+ webRequest.getResponseMessage());
			}

			writeToCache(key, audioBuffer);
		}

		if (WEB_CACHE_DIR == null) {
			WEB_CACHE_DIR = WiSeEnv.getHomePath().replace("kernel", "webapps") + File.separator + "wise"
					+ File.separator + "commons" + File.separator;
		}
		File cacheFile = new File(WEB_CACHE_DIR + key + ".wav");
		if (!cacheFile.exists()) {
			writeToWebCache(cacheFile, audioBuffer);
		}

		return audioBuffer;
	}

	private static byte[] getFromCache(String key) {
		File cacheFile = new File(CACHE_DIR + key + ".pcm");
		if (cacheFile.exists()) {
			try {
				return FileUtils.readFileToByteArray(cacheFile);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private static boolean writeToCache(String key, byte[] audioBuffer) {
		try {
			// write the pcm data to the file
			File cacheFile = new File(CACHE_DIR + key + ".pcm");
			FileUtils.writeByteArrayToFile(cacheFile, audioBuffer);

			return true;
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	private static boolean writeToWebCache(File cacheFile, byte[] audioBuffer) {
		try {
			// write the pcm data to the file
			FileUtils.writeByteArrayToFile(cacheFile, audioBuffer);

			return true;
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

}
