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

import com.microsoft.util.HttpsConnection;

public class STTSample {

	public static void main(String[] args) {

		String fileFullPath = "your wave file path";
		String sttServiceUri = "https://speech.platform.bing.com/recognize/query";

        try{
            // Note: Sign up at http://www.projectoxford.ai for the client credentials.
        	OxfordAuthentication auth = new OxfordAuthentication("your client id", "your subscription key");
            String token = auth.GetAccessToken();
            sttServiceUri += "?scenarios=smd";                                  // websearch is the other main option.
            sttServiceUri += "&appid=D4D52672-91D7-4C74-8AD8-42B1D98141A5";     // You must use this ID.
            sttServiceUri += "&locale=en-US";                                   // We support several other languages.  Refer to README file.
            sttServiceUri += "&device.os=wp7";
            sttServiceUri += "&version=3.0";
            sttServiceUri += "&format=json";
            sttServiceUri += "&instanceid=565D69FF-E928-4B7E-87DA-9A750B96D9E3";
            UUID uuid = UUID.randomUUID();
            sttServiceUri += "&requestid=" + uuid.toString();
            HttpsURLConnection webRequest = HttpsConnection.getHttpsConnection(sttServiceUri);
            webRequest.setDoInput(true);
            webRequest.setDoOutput(true);
            webRequest.setConnectTimeout(5000);
            webRequest.setReadTimeout(15000);
            webRequest.setRequestMethod("POST");
            webRequest.setRequestProperty("Content-Type", "audio/wav; samplerate=16000");
            webRequest.setRequestProperty("Authorization", "Bearer " + token);


            byte[] bytes = ToByteArray(fileFullPath);
            webRequest.setRequestProperty("content-length", String.valueOf(bytes.length));
            webRequest.connect();

            DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
            dop.write(bytes);
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
            System.out.println(stringBuffer.toString());
        }catch(Exception e){
        	e.printStackTrace();
        }
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

}