/**
 Copyright (c) Microsoft Corporation
 All rights reserved.
 MIT License
 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the ""Software""), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.microsoft.ttshttpoxford.sample;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microsoft.util.HttpsConnection;


public class STTService {

    private static String sttServiceUri = "https://speech.platform.bing.com/recognize";

    private static OxfordAuthentication auth = null;  // It will auto renew expired token, so just need one instance.


    /**
     *
     */
    public static String Recognize(byte[] audioBytes, String resultFormat, int sampleing, String locale) throws Exception {
        // Note: Sign up at http://www.projectoxford.ai for the client credentials.
    	if (auth == null) {
    		String microsoftSttKey = System.getProperty("microsoft.stt.key");
    		
    		if (microsoftSttKey == null) {
    			System.err.println("Cannot find Microsoft STT apikey, please set java jvm args 'microsoft.stt.key'");
    		}
    		else {
    			auth = new OxfordAuthentication("robot_stt", microsoftSttKey);
    		}
    	}

        String token = auth.GetAccessToken();


        String apiUrl = sttServiceUri + "?Version=3.0";
        apiUrl += "&scenarios=ulm";                               // websearch is the other main option.
        apiUrl += "&appID=D4D52672-91D7-4C74-8AD8-42B1D98141A5";  // You must use this ID.
        apiUrl += "&locale=" + locale;                            // We support several other languages.  Refer to README file.
        apiUrl += "&device.os=Android";
        apiUrl += "&format=" + resultFormat;
        apiUrl += "&instanceid=a5d778c2-c926-4c98-b488-27274def891f";  // 隨意，但同一個應用系統不要改
        UUID uuid = UUID.randomUUID();
        apiUrl += "&requestid=" + uuid.toString();

        HttpsURLConnection webRequest = HttpsConnection.getHttpsConnection(apiUrl);
        webRequest.setDoInput(true);
        webRequest.setDoOutput(true);
        webRequest.setConnectTimeout(5000);
        webRequest.setReadTimeout(15000);
        webRequest.setRequestMethod("POST");
//        webRequest.setRequestProperty("Content-Type", "audio/wav; codec=\"audio/wav\"; samplerate=" + sampleing + "; sourcerate=8000; trustsourcerate=false");

        webRequest.setRequestProperty("Content-Type", "audio/wav; samplerate=" + sampleing);
        webRequest.setRequestProperty("Authorization", "Bearer " + token);
        webRequest.setRequestProperty("Content-Length", String.valueOf(audioBytes.length));

        //webRequest.getOutputStream().write(audioBytes);
        DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
        dop.write(audioBytes);
        dop.flush();
        dop.close();


        // Get the response
        final BufferedReader rd = new BufferedReader(new InputStreamReader(webRequest.getInputStream()));
        final StringBuffer stringBuffer = new StringBuffer();
        String line;
        while ((line = rd.readLine()) != null) {
        	stringBuffer.append(line);
        }
        rd.close();
        webRequest.disconnect();

        String result = stringBuffer.toString();
        //System.out.println(result);

        return result;
    }



    /**
     * the traditional io way
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static byte[] ToByteArray(String filename) throws IOException {

        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }

    public static String getBestResult(byte[] audioBytes, String resultFormat, int sampleing, String locale) throws Exception {
    	JSONObject result = new JSONObject(Recognize(audioBytes, resultFormat, sampleing, locale));

    	if (result.has("results")) {
    		JSONArray results = result.getJSONArray("results");

    		JSONObject best = null;
    		double bestConf = -1;

    		for (int i=0; i < results.length(); i++) {
    			JSONObject j = results.getJSONObject(i);

    			if (j.has("confidence")) {
    				double jc = Double.parseDouble(j.getString("confidence"));

    				if (jc > bestConf) {
    					best = j;
    				}
    			}
    		}

    		if (best != null) {
    			return best.getString("lexical");
    		}
    	}

    	return null;
    }


    public static void main(String[] args) throws Exception {
    	byte[] audioBytes = ToByteArray("/Volumes/Data/Users/herb/git/WiSe_based/Robot/kernel/cache/line_audio/14824583197425390122022066.wav");
    	System.out.println(getBestResult(audioBytes, "json", 16000, "zh-TW"));
    }
}