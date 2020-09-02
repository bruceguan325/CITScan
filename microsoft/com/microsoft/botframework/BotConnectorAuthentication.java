/**
 Copyright (c) Microsoft Corporation
 All rights reserved.
 MIT License
 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the ""Software""), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.microsoft.botframework;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import com.intumit.solr.util.WiSeUtils;
import com.microsoft.util.HttpsConnection;

import flexjson.JSONDeserializer;

/*
     * This class demonstrates how to get a valid O-auth token from
     * Azure Data Market.
     */
public class BotConnectorAuthentication implements Serializable
{
    public static final String AccessTokenUri = "https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token";

    private String clientId;
    private String clientSecret;
    private BotConnectorAccessToken token;
    private Timer accessTokenRenewer;

    //Access token expires every 60 minutes. Renew it every 50 minutes only.
    private final int RefreshTokenDuration = 50 * 60 * 1000;
    private TimerTask fiftyMinitesTask = null;

    public BotConnectorAuthentication(String clientId, String clientSecret)
    {
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        /*
         * If clientid or client secret has special characters, encode before sending request
         */
        RenewAccessToken();

        // renew the token every specified minutes
        accessTokenRenewer = new Timer();
        fiftyMinitesTask = new TimerTask(){
            public void run(){
                RenewAccessToken();
            }
        };

        accessTokenRenewer.schedule(fiftyMinitesTask, RefreshTokenDuration, RefreshTokenDuration);
    }

    public BotConnectorAccessToken GetAccessToken()
    {
        return this.token;
    }

    private void RenewAccessToken()
    {
        String newAccessToken = HttpPost(AccessTokenUri, clientId, clientSecret);
        //swap the new token with old one
        //Note: the swap is thread unsafe
        this.token = (BotConnectorAccessToken)new JSONDeserializer().deserialize(newAccessToken, BotConnectorAccessToken.class);
        System.out.println("new botframework access token: " + token);
    }

    private String HttpPost(String AccessTokenUri, String clientId, String clientSecret)
    {
        InputStream inSt = null;
        HttpsURLConnection webRequest = null;

        //Prepare OAuth request
        try{
            webRequest = HttpsConnection.getHttpsConnection(AccessTokenUri);
            webRequest.setDoInput(true);
            webRequest.setDoOutput(true);
            webRequest.setConnectTimeout(5000);
            webRequest.setReadTimeout(5000);
            webRequest.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            webRequest.setRequestMethod("POST");
            
            String content = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret + "&scope=https%3A%2F%2Fapi.botframework.com%2F.default";

            webRequest.setRequestProperty("content-length", "" + content.length());
            
            System.out.println(webRequest.getRequestProperties().toString());
            webRequest.connect();

            DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
            dop.write(content.getBytes());
            dop.flush();
            dop.close();

            inSt = webRequest.getInputStream();
            InputStreamReader in = new InputStreamReader(inSt);
            BufferedReader bufferedReader = new BufferedReader(in);
            StringBuffer strBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                strBuffer.append(line);
            }

            bufferedReader.close();
            in.close();
            inSt.close();
            
            webRequest.disconnect();

            return strBuffer.toString();
        }catch (Exception e){
            try {
				System.out.println(webRequest.getResponseMessage());
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            e.printStackTrace();
        }

        return null;
    }
    
    public static void main(String[] args) {
    		BotConnectorAuthentication bca = new BotConnectorAuthentication("xxxxx", "xxxxx");
    }
}