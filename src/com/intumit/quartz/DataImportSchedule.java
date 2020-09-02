package com.intumit.quartz;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.intumit.solr.util.WiSeEnv;

public class DataImportSchedule implements Job {

    static Logger logger = Logger.getLogger(DataImportSchedule.class.getName());

    public void execute(JobExecutionContext context)
        throws JobExecutionException {
        JobDataMap map = context.getJobDetail().getJobDataMap();
        String name = map.getString("name");
        logger.info("Start " + name + " - " + new java.util.Date());
        try {
            getResponseBody(
                map.getString("deltaImport"),
                map.getString("account"),
                map.getString("password"));

            Thread.sleep(map.getInt("commitInterval") * 1000);

            getResponseBody(
                map.getString("commit"),
                map.getString("account"),
                map.getString("password"));

            Thread.sleep(map.getInt("commitInterval") * 1000);

            getResponseBody(
                map.getString("sms"),
                map.getString("account"),
                map.getString("password"));

            logger.info("End  " + name + " - " + new java.util.Date());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getResponseBody(String url, String account, String password)
        throws UnsupportedEncodingException {

        logger.info("run url:" + url);

        HttpClient httpClient = new HttpClient();

        // 認證
        httpClient.getState().setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(account, password));

        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter(
            HttpMethodParams.RETRY_HANDLER,
            new DefaultHttpMethodRetryHandler());
        
        getMethod.setRequestHeader("Connection", "close"); 

        try {

            int statusCode = httpClient.executeMethod(getMethod);
            byte[] responseBody;

            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: "
                    + getMethod.getStatusLine());
            }
            else {
                responseBody = getMethod.getResponseBody();
                System.out.println(new String(responseBody));
                return new String(responseBody);
            }

        }
        catch (HttpException e) {
            System.out.println("Please check your provided http address!");
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (getMethod != null) {
                try {
             	   getMethod.releaseConnection();
                } catch (Exception e) {
                    logger.error("-------> Release HTTP connection exception:", e);
                }
         }
         
         if (httpClient != null) {
                try {
                    ((SimpleHttpConnectionManager) httpClient.getHttpConnectionManager()).shutdown();
                } catch (Exception e) {
                    logger.error("-------> Close HTTP connection exception:", e);
                }
                httpClient = null;
          }
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        // System.out.println("start");
        // System.getProperties()
        // .put(
        // "org.quartz.properties",
        // "D:/Java/Project/wise/wise_20090417/WiSe/tomcat/webapps/wise/WEB-INF/quartz.properties");
        try {
            // StdSchedulerFactory.getDefaultScheduler().start();
            String url = "http://localhost:8080/wise" + WiSeEnv.getAdminContextPath() + "/core0/dataimport?command=delta-import&commit=false&optimize=false";
            // getResponseBody(url, "root", "intumitdemo");
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("end");
    }

}
