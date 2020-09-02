package com.intumit.smartwiki.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.intumit.smartwiki.util.Constants;

/**
 * @author Neil
 */
public class NetUtil {

    private static Log log = LogFactory.getLog(NetUtil.class);

    public static void main(String[] args) {
        Constants.PROXY_HOST = "proxy.intumit.com";
        Constants.PROXY_PORT = 3128;
        System.out.println(NetUtil.getHTMLWithGet(
            "http://zh.wikipedia.org/w/index.php?title=MediaWiki:Conversiontable/zh-tw&variant=zh-tw",
            "UTF8"));
    }

    /**
     * 以GTE方式取得網頁的HTML內容，有問題時回傳NULL
     *
     * @throws NetConnectException
     */
    public static final String getHTMLWithGet(String url, String encoding) {
        // 準備連線工具並設定PROXY（如果有的話）
        HttpClient client = new HttpClient();
        if (StringUtils.isNotBlank(Constants.PROXY_HOST)) {
            HostConfiguration conf = new HostConfiguration();
            conf.setProxy(Constants.PROXY_HOST, Constants.PROXY_PORT);
            client.setHostConfiguration(conf);
        }
        HttpMethod method = null;
        InputStream is = null;
        String html = null;
        try {
            // 使用GET METHOD
            method = new GetMethod(url);
            method.setRequestHeader(
                "user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; Alexa Toolbar)");
            // 連線
            client.executeMethod(method);
            // 判斷連線結果
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                // 讀串流資料
                is = method.getResponseBodyAsStream();
                // 轉串流資料為字串
                html = IOUtils.toString(is, encoding);
            }
            else {
                log.error("method.getStatusCode()" + method.getStatusCode());
                log.error("Error Message:"
                    + NetUtil.getNetConnectMessage(method.getStatusCode()));
            }
        }
        catch (HttpException e) {
            log.error(e);
            log.error("Error Message:"
                + NetUtil.getNetConnectMessage(method.getStatusCode()));
        }
        catch (IOException e) {
            log.error(e);
            log.error("Error Message:"
                + NetUtil.getNetConnectMessage(method.getStatusCode()));
        }
        finally {
            IOUtils.closeQuietly(is);
            if (method != null) {
                method.releaseConnection();
            }
        }
        return html;
    }

    /**
     * 將HTML錯誤編號轉成文字敘述
     *
     * @param errorCode
     * @return
     */
    public static String getNetConnectMessage(int errorCode) {
        StringBuffer sb = new StringBuffer();
        sb.append("網路連線失敗：");
        switch (errorCode) {
            case -1:
                sb.append("中斷");
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                sb.append("伺服器內部錯誤（");
                sb.append(errorCode);
                sb.append("）");
                break;
            case HttpStatus.SC_FORBIDDEN:
                sb.append("被拒（");
                sb.append(errorCode);
                sb.append("）");
                break;
            case HttpStatus.SC_NOT_FOUND:
                sb.append("網頁不存在（");
                sb.append(errorCode);
                sb.append("）");
                break;
            default:
                sb.append("未定義錯誤代碼（");
                sb.append(errorCode);
                sb.append("）而無法顯示正確訊息");
                break;
        }
        return sb.toString();
    }
}
