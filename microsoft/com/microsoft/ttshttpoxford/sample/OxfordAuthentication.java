/**
 Copyright (c) Microsoft Corporation
 All rights reserved.
 MIT License
 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the ""Software""), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.microsoft.ttshttpoxford.sample;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import com.microsoft.util.HttpsConnection;

/*
     * This class demonstrates how to get a valid O-auth token from
     * Azure Data Market.
     */
public class OxfordAuthentication
{
    public static final String AccessTokenUri = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";

    private String clientSecret;
    private String token;
    private Timer accessTokenRenewer;

    //Access token expires every 10 minutes. Renew it every 9 minutes only.
    private final int RefreshTokenDuration = 9 * 60 * 1000;
    private TimerTask nineMinitesTask = null;

    public OxfordAuthentication(String clientId, String clientSecret)
    {
        this.clientSecret = clientSecret;

            /*
             * If clientid or client secret has special characters, encode before sending request
             */
        this.token = HttpPost(AccessTokenUri, clientSecret);

        // renew the token every specified minutes
        accessTokenRenewer = new Timer();
        nineMinitesTask = new TimerTask(){
            public void run(){
                RenewAccessToken();
            }
        };

        accessTokenRenewer.schedule(nineMinitesTask, 0, RefreshTokenDuration);
    }

    public String GetAccessToken()
    {
        return this.token;
    }

    private void RenewAccessToken()
    {
        String newAccessToken = HttpPost(AccessTokenUri, clientSecret);
        //swap the new token with old one
        //Note: the swap is thread unsafe
        System.out.println("new access token: " + newAccessToken);
        this.token = newAccessToken;
    }

    private String HttpPost(String AccessTokenUri, String requestDetails)
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
            webRequest.setRequestProperty("Ocp-Apim-Subscription-Key", requestDetails);

            webRequest.setRequestProperty("content-length", "0");
            webRequest.connect();

            DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
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
            e.printStackTrace();
        }

        return null;
    }
}